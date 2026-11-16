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
			+ "    function dlViewerShow() { \n"
			+ "        var xml = document.getElementById('dl-xml').value; \n"
			+ "        $('#dl-msg').html(''); $('#dl-details').html(''); \n"
			+ "        if (!xml.trim()) { $('#dl-pic').html('<i>Paste a deadlock report first.</i>'); return; } \n"
			+ "        sqlServerDeadlockRender($('#dl-pic'), xml, document.getElementById('dl-eventTime').value, { isDark: false, onParsed: function(d) { \n"
			+ "            if (d.error) return; \n"
			+ "            if (d.deadlockCount > 1) $('#dl-msg').html('<span class=\"text-warning-emphasis\">The report has <b>' + d.deadlockCount + '</b> deadlocks, this is the first one.</span>'); \n"
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
