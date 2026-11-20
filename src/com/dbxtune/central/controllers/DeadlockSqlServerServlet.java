/*******************************************************************************
 * Copyright (C) 2010-2027 Goran Schwarz
 *
 * This file is part of DbxTune
 * DbxTune is a family of sub-products *Tune, hence the Dbx
 * Here are some of the tools: AseTune, IqTune, RsTune, RaxTune, HanaTune,
 *          SqlServerTune, PostgresTune, MySqlTune, MariaDbTune, Db2Tune, ...
 *
 * DbxTune is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.dbxtune.central.controllers;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.text.StringEscapeUtils;

import com.dbxtune.central.controllers.HtmlStatic.PageSection;
import com.dbxtune.utils.HtmlUtils;
import com.dbxtune.utils.StringUtil;

/**
 * SQL Server Deadlock Viewer (Tools -> Deadlock -> SQL Server):
 * <pre>GET  /deadlock/sqlserver                      a form to paste (or load/drop) a deadlock report
 * POST /deadlock/sqlserver  (xml, eventTime)     the same page, filled in and shown (the "view" link in the Daily Summary Report)</pre>
 * Shows the same as the Counter Details "Row Detail" of a CmDeadlocks row: the picture (tooltips, Play), the SQL to look up
 * the wait resources, the column values and the XML. Drawing: dbxDeadlockSqlServer.js, parsing: {@link SqlServerDeadlockParseServlet}.
 */
public class DeadlockSqlServerServlet
extends HttpServlet
{
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		writePage(resp, null, null);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		writePage(resp, req.getParameter("xml"), req.getParameter("eventTime"));
	}

	private static void writePage(HttpServletResponse resp, String xml, String eventTime) throws IOException
	{
		// "How to get a deadlock report": SQL to run (SSMS) on any SQL Server. They return the whole XE event: it has the time
		// of the deadlock (UTC), which a bare <deadlock> does not. {title, when to use it, SQL}
		String[][] howTo = {
			{ "1. From the <code>system_health</code> files"
			, "SQL Server 2012+ and Azure SQL Managed Instance (DbxTune's 'Deadlocks' reads the same). How far back it goes depends on the "
			  + "size of the files and how busy the server is (example 5 shows the files)."
			, "-- The last 20 deadlocks from the 'system_health' Extended Events files (newest first)\n"
			  + "SELECT TOP (20)\n"
			  + "       event_time_utc = x.ev.value('(event/@timestamp)[1]', 'datetime2(3)'),\n"
			  + "       deadlock_event = x.ev\n"
			  + "FROM sys.fn_xe_file_target_read_file(N'system_health*.xel', NULL, NULL, NULL) AS f\n"
			  + "CROSS APPLY (SELECT CAST(f.event_data AS xml)) AS x(ev)\n"
			  + "WHERE f.object_name = N'xml_deadlock_report'\n"
			  + "ORDER BY event_time_utc DESC;"
			},
			{ "1b. The same, for a time period (SQL Server 2017+)"
			, "SQL Server 2017+ has the column <code>timestamp_utc</code>: no XML is needed to filter on the time."
			, "-- Deadlocks from the last 24 hours (newest first)\n"
			  + "SELECT event_time_utc = f.timestamp_utc,\n"
			  + "       deadlock_event = CAST(f.event_data AS xml)\n"
			  + "FROM sys.fn_xe_file_target_read_file(N'system_health*.xel', NULL, NULL, NULL) AS f\n"
			  + "WHERE f.object_name = N'xml_deadlock_report'\n"
			  + "  -- The CAST is needed: without it, SQL Server 2019 and 2022 return no rows (2025 is OK)\n"
			  + "  AND CAST(f.timestamp_utc AS datetime2(7)) >= DATEADD(hour, -24, SYSUTCDATETIME())\n"
			  + "ORDER BY f.timestamp_utc DESC;"
			},
			{ "2. From the <code>system_health</code> ring buffer"
			, "In memory: fast, but only the most recent events (and nothing from before the last restart)."
			, "SELECT event_time_utc = e.n.value('@timestamp', 'datetime2(3)'),\n"
			  + "       deadlock_event = e.n.query('.')\n"
			  + "FROM (SELECT target_data = CAST(t.target_data AS xml)\n"
			  + "      FROM sys.dm_xe_session_targets t\n"
			  + "      JOIN sys.dm_xe_sessions s ON s.address = t.event_session_address\n"
			  + "      WHERE s.name = N'system_health' AND t.target_name = N'ring_buffer') AS rb\n"
			  + "CROSS APPLY rb.target_data.nodes('RingBufferTarget/event[@name=\"xml_deadlock_report\"]') AS e(n)\n"
			  + "ORDER BY event_time_utc DESC;"
			},
			{ "3. Azure SQL Database"
			, "Has no <code>system_health</code>. Run it in the <code>master</code> database of the logical server: deadlocks of all its "
			  + "databases, kept only a short time (for longer: a session like example 4, <code>ON DATABASE</code> with a <code>ring_buffer</code> or an Azure Storage <code>event_file</code>)."
			, "-- In the 'master' database\n"
			  + "SELECT event_time_utc = x.ev.value('(event/@timestamp)[1]', 'datetime2(3)'),\n"
			  + "       database_name  = x.ev.value('(event/data[@name=\"database_name\"]/value)[1]', 'sysname'),\n"
			  + "       deadlock_event = x.ev\n"
			  + "FROM sys.fn_xe_telemetry_blob_target_read_file('dl', NULL, NULL, NULL) AS f\n"
			  + "CROSS APPLY (SELECT CAST(f.event_data AS xml)) AS x(ev)\n"
			  + "ORDER BY event_time_utc DESC;"
			},
			{ "4. Your own session, only for deadlocks"
			, "When <code>system_health</code> rolls over too fast (busy server), or is stopped. Read it like example 1, with <code>N'deadlocks*.xel'</code>."
			, "CREATE EVENT SESSION deadlocks ON SERVER\n"
			  + "    ADD EVENT sqlserver.xml_deadlock_report\n"
			  + "    ADD TARGET package0.event_file (SET filename = N'deadlocks', max_file_size = 10, max_rollover_files = 10)\n"
			  + "    WITH (STARTUP_STATE = ON);\n"
			  + "ALTER EVENT SESSION deadlocks ON SERVER STATE = START;\n"
			  + "\n"
			  + "-- Remove it:\n"
			  + "-- DROP EVENT SESSION deadlocks ON SERVER;"
			},
			{ "5. Is <code>system_health</code> running, and where are its files?"
			, "No rows from example 1? Check that the session is running. The file name has no path when it is the default: the errorlog directory."
			, "SELECT s.name,\n"
			  + "       is_running = CASE WHEN r.name IS NULL THEN 'NO' ELSE 'yes' END,\n"
			  + "       file_name  = (SELECT CAST(f.value AS nvarchar(260)) FROM sys.server_event_session_fields f\n"
			  + "                     WHERE f.event_session_id = s.event_session_id AND f.name = 'filename'),\n"
			  + "       max_file_size_mb = (SELECT CAST(f.value AS int) FROM sys.server_event_session_fields f\n"
			  + "                     WHERE f.event_session_id = s.event_session_id AND f.name = 'max_file_size'),\n"
			  + "       max_rollover_files = (SELECT CAST(f.value AS int) FROM sys.server_event_session_fields f\n"
			  + "                     WHERE f.event_session_id = s.event_session_id AND f.name = 'max_rollover_files')\n"
			  + "FROM sys.server_event_sessions s\n"
			  + "LEFT JOIN sys.dm_xe_sessions r ON r.name = s.name\n"
			  + "WHERE s.name = N'system_health';"
			},
		};
		StringBuilder howToHtml = new StringBuilder();
		for (String[] h : howTo)
		{
			// Prism's toolbar adds a "Copy" button to the code block
			howToHtml.append("    <div class='mt-3'><b>").append(h[0]).append("</b> \n")
				.append("      <div class='text-muted' style='font-size:0.9em;'>").append(h[1]).append("</div> \n")
				.append("      <pre class='language-sql' style='margin:4px 0 0;font-size:0.8em;'><code class='language-sql'>")
				.append(StringEscapeUtils.escapeHtml4(h[2])).append("</code></pre></div> \n");
		}

		resp.setContentType("text/html; charset=UTF-8");
		resp.setCharacterEncoding("UTF-8");
		PrintWriter out = resp.getWriter();
		out.print(""
			+ "<!DOCTYPE html> \n"
			+ "<html lang='en'> \n"
			+ "<head> \n"
			+ "    <meta charset='UTF-8'> \n"
			+ "    <meta name='viewport' content='width=device-width, initial-scale=1'> \n"
			+ "    <title>DbxTune - SQL Server Deadlock Viewer</title> \n"
			+ HtmlStatic.getUserDefinedContentHead()
			+ HtmlUtils.createCssLinkTag("/scripts/prism/prism-1.30.0.css")
			+ HtmlUtils.createJsScriptTag("/scripts/prism/prism-1.30.0.js")
			+ HtmlUtils.createJsScriptTag("/scripts/dbxtune/js/dbxDeadlockSqlServer.js")
			+ "</head> \n"
			+ "<body> \n"
			+ HtmlStatic.getHtmlNavbar(PageSection.Tools, "", true)
			+ "<div class='container-fluid px-4 py-3' style='max-width:1100px;'> \n"
			+ "  <h2>SQL Server Deadlock Viewer</h2> \n"
			+ "  <p class='text-muted'>Paste a deadlock report and press Show: a <code>&lt;deadlock&gt;</code> (a .xdl file from SSMS or the Daily Summary Report), "
			+ "     a <code>&lt;deadlock-list&gt;</code>, or a <code>xml_deadlock_report</code> event from <code>system_health</code>. "
			+ "     You get the same as when clicking a row in Counter Details 'Deadlocks': the timeline picture (hover for details, Play), "
			+ "     the SQL to look up what the sessions waited for, and the details.</p> \n"
			// How to get a deadlock report, from any SQL Server (closed by default)
			+ "  <details id='dl-howto' class='card shadow-sm mb-3'><summary class='card-header' style='cursor:pointer;'><b>How to get a deadlock report</b> (from any SQL Server, with SQL)</summary> \n"
			+ "  <div class='card-body' style='font-size:0.95em;'> \n"
			+ "    Run one of these in SSMS (or any SQL tool) on the server. Each row is one deadlock: copy the <code>deadlock_event</code> "
			+ "    value and paste it below. Several rows copied at once: you pick the deadlock from a list. "
			+ "    Permissions: <code>VIEW SERVER STATE</code> (SQL Server 2022+: <code>VIEW SERVER PERFORMANCE STATE</code>); "
			+ "    example 4 needs <code>ALTER ANY EVENT SESSION</code>. \n"
			+ howToHtml
			+ "    <div class='mt-3'><b>Tips</b><ul style='font-size:0.9em;margin-bottom:0;'> \n"
			+ "      <li>SSMS: click the <code>deadlock_event</code> cell, it opens as XML: Ctrl+A, Ctrl+C, and paste it here.</li> \n"
			+ "      <li>Do not use 'Results to Text': it cuts long values (256 characters by default, at most 8192).</li> \n"
			+ "      <li><code>sqlcmd</code>: add <code>-I</code> (QUOTED_IDENTIFIER ON, needed by the XML functions) and <code>-y 0</code> (do not cut the XML).</li> \n"
			+ "      <li>A deadlock is in the ring buffer (example 2) right away, but in the files (examples 1, 4) only after up to a few minutes.</li> \n"
			+ "      <li>A bare <code>&lt;deadlock&gt;</code> (a .xdl file) has no time: it is estimated from the sessions' transaction start + wait time. "
			+ "          The <code>deadlock_event</code> above has the real time (UTC).</li> \n"
			+ "      <li>Without SQL, in SSMS: Management &rarr; Extended Events &rarr; Sessions &rarr; system_health &rarr; package0.event_file &rarr; View Target Data, "
			+ "          a <code>xml_deadlock_report</code> row &rarr; the 'Deadlock' tab &rarr; right-click &rarr; Export to .xdl, and 'Load from File' here.</li> \n"
			+ "      <li>Trace flag 1222 / 1204 (deadlocks in the errorlog) is text, not XML: it cannot be shown here.</li> \n"
			+ "    </ul></div> \n"
			+ "  </div></details> \n"
			+ "  <div class='card shadow-sm mb-3'><div class='card-body'> \n"
			+ "    <div class='mb-2'> \n"
			+ "      <button type='button' class='btn btn-outline-secondary btn-sm' onclick=\"document.getElementById('dl-file').click();\">&#128194; Load from File</button> \n"
			+ "      <input type='file' id='dl-file' accept='.xdl,.xml,.txt' style='display:none' onchange='dlViewerReadFile(this.files[0]); this.value=\"\";'> \n"
			+ "      <span style='font-size:0.8em;color:#888;margin-left:6px;'>or drag &amp; drop a file onto the text area below</span> \n"
			+ "    </div> \n"
			+ "    <textarea class='form-control mb-2' id='dl-xml' rows='10' style='font-family:monospace;font-size:0.85em;' placeholder='Paste the deadlock XML here...' \n"
			+ "              ondragover='event.preventDefault(); this.style.borderColor=\"#4a90d9\";' ondragleave='this.style.borderColor=\"\";' \n"
			+ "              ondrop='event.preventDefault(); this.style.borderColor=\"\"; dlViewerReadFile(event.dataTransfer.files && event.dataTransfer.files[0]);'></textarea> \n"
			+ "    <input type='hidden' id='dl-eventTime'> \n"
			+ "    <button type='button' class='btn btn-primary' onclick='dlViewerShow();'>Show</button> \n"
			+ "  </div></div> \n"
			+ "  <div id='dl-msg' class='mb-2' style='font-size:0.9em;'></div> \n"
			+ "  <div id='dl-pic' class='mb-3' style='overflow:auto;'></div> \n"
			+ "  <div id='dl-details'></div> \n"
			+ "</div> \n"
			+ "<script> \n"
			+ "    function dlViewerReadFile(file) { \n"
			+ "        if (!file) return; \n"
			+ "        var reader = new FileReader(); \n"
			+ "        reader.onload = function(e) { document.getElementById('dl-xml').value = e.target.result; document.getElementById('dl-eventTime').value = ''; dlViewerShow(); }; \n"
			+ "        reader.readAsText(file); \n"
			+ "    } \n"
			+ "    $(function() { if (typeof Prism !== 'undefined') $('#dl-howto code.language-sql').each(function() { Prism.highlightElement(this); }); }); \n"
			// index: which deadlock, when the paste has several (the drop-down)
			+ "    function dlViewerShow(index) { \n"
			+ "        var xml = document.getElementById('dl-xml').value; \n"
			+ "        $('#dl-msg').html(''); $('#dl-details').html(''); \n"
			+ "        if (!xml.trim()) { $('#dl-pic').html('<i>Paste a deadlock report first.</i>'); return; } \n"
			+ "        sqlServerDeadlockRender($('#dl-pic'), xml, document.getElementById('dl-eventTime').value, { isDark: false, index: index || 0, onParsed: function(d) { \n"
			+ "            if (d.error) return; \n"
			+ "            if (d.deadlockCount > 1) { \n"
			+ "                var s = '<label for=\"dl-pick\" class=\"me-2\">The report has <b>' + d.deadlockCount + '</b> deadlocks:</label>' \n"
			+ "                      + '<select id=\"dl-pick\" class=\"form-select form-select-sm d-inline-block w-auto\" style=\"max-width:100%;\" onchange=\"dlViewerShow(+this.value);\">'; \n"
			+ "                d.list.forEach(function(e) { \n"
			+ "                    var txt = '#' + (e.index + 1) + ' - ' + (e.error ? 'could not parse it' \n"
			+ "                        : (e.eventTime || '(no time)') + (e.eventTimeNote ? ' (' + e.eventTimeNote + ')' : '') \n"
			+ "                          + (e.dbName ? ' - ' + e.dbName : '') + ' - victim spid ' + e.victimSpid + ' - ' + e.processCount + ' sessions' \n"
			+ "                          + (e.objects ? ' - ' + (e.objects.length > 80 ? e.objects.substring(0, 80) + '...' : e.objects) : '')); \n"
			+ "                    s += '<option value=\"' + e.index + '\"' + (e.index === d.index ? ' selected' : '') + (e.error ? ' disabled' : '') + '>' + escHtml(txt) + '</option>'; \n"
			+ "                }); \n"
			+ "                $('#dl-msg').html(s + '</select>'); \n"
			+ "            } \n"
			// The column values, like the Row Detail of a CmDeadlocks row, then the XML (Prism) with a .xdl download (opens in SSMS)
			+ "            var h = '<table class=\"table table-sm table-bordered\" style=\"font-size:0.85em;word-break:break-word;\"><thead class=\"table-light\"><tr><th style=\"white-space:nowrap\">Column</th><th>Value</th></tr></thead><tbody>'; \n"
			+ "            Object.keys(d.columns).forEach(function(c) { \n"
			+ "                var v = d.columns[c]; \n"
			+ "                h += '<tr><td style=\"white-space:nowrap;font-weight:bold;\">' + escHtml(c) + '</td><td>' + (v === null || v === undefined ? '<span style=\"color:#aaa\">null</span>' \n"
			+ "                   : '<pre style=\"white-space:pre-wrap;margin:0;font-size:0.9em;\">' + escHtml(String(v)) + '</pre>') + '</td></tr>'; \n"
			+ "            }); \n"
			+ "            h += '</tbody></table>'; \n"
			+ "            var xdl = URL.createObjectURL(new Blob([d.xml], { type: 'application/xml' })); \n"
			+ "            h += '<details open><summary><b>Deadlock XML</b> (without the native call stacks) &nbsp;<a download=\"deadlock.xdl\" href=\"' + xdl + '\">Download .xdl</a> (opens in SSMS)</summary>' \n"
			+ "               + '<pre class=\"language-xml\" style=\"white-space:pre-wrap;max-height:500px;overflow:auto;font-size:0.8em;\"><code class=\"language-xml\" style=\"white-space:pre-wrap;word-break:break-all;\">' + escHtml(d.xml) + '</code></pre></details>'; \n"
			+ "            $('#dl-details').html(h); \n"
			+ "            if (typeof Prism !== 'undefined') $('#dl-details code.language-xml').each(function() { Prism.highlightElement(this); }); \n"
			+ "        }}); \n"
			+ "    } \n"
			// POST (the "view" link in the Daily Summary Report): fill in and show
			+ (xml == null ? "" : ""
			+ "    document.getElementById('dl-xml').value       = '" + StringEscapeUtils.escapeEcmaScript(xml) + "'; \n"
			+ "    document.getElementById('dl-eventTime').value = '" + StringEscapeUtils.escapeEcmaScript(StringUtil.nullToValue(eventTime, "")) + "'; \n"
			+ "    $(function() { dlViewerShow(); }); \n")
			+ "</script> \n"
			+ HtmlStatic.getJavaScriptAtEnd(true)
			+ "</body> \n"
			+ "</html> \n");
		out.flush();
	}
}
