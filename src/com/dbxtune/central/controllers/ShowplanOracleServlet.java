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
import java.lang.invoke.MethodHandles;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.central.controllers.HtmlStatic.PageSection;
import com.dbxtune.utils.HtmlUtils;
import com.dbxtune.utils.StringUtil;


/**
 * The Oracle Execution Plan Viewer page: <code>/showplan/oracle</code>
 * <p>
 * The plan is the TEXT that DBMS_XPLAN prints (DISPLAY_CURSOR, DISPLAY). It is parsed and drawn in
 * the browser by <code>dbxShowplanOracle.js</code>, in the dialog that <code>dbxShowplan.js</code> holds: the same
 * dialog the 'Active Statements' panel of an Oracle server opens.
 * <ul>
 *   <li>GET: a form to paste a plan into. The button opens the dialog with what was pasted, without a round trip to the server
 *       (the plan only leaves the browser when the user opens one of the LLM sections of the dialog).</li>
 *   <li>POST (plan, sql): the same page, with the dialog opened on the plan that was posted. This is how the
 *       "Open in External Window" button of the dialog hands its plan over.</li>
 * </ul>
 */
public class ShowplanOracleServlet
extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		resp.setContentType("text/html; charset=UTF-8");
		resp.setCharacterEncoding("UTF-8");
		PrintWriter out = resp.getWriter();
		out.print(createPage(null, null));
		out.flush();
		out.close();
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		String plan = request.getParameter("plan");
		String sql  = request.getParameter("sql");

		if (_logger.isDebugEnabled())
			_logger.debug("/showplan/oracle: received request from: remoteHost='" + request.getRemoteHost() + "', remoteAddr='" + request.getRemoteAddr() + "', remoteUser='" + request.getRemoteUser() + "'.");

		if (StringUtil.isNullOrBlank(plan))
		{
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Expecting an Oracle execution plan (the text DBMS_XPLAN prints) in the parameter 'plan', but it was empty.");
			return;
		}

		response.setContentType("text/html; charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
		PrintWriter out = response.getWriter();
		out.print(createPage(plan, sql));
		out.flush();
		out.close();
	}

	/**
	 * @param plan  the text of DBMS_XPLAN to open the dialog with, or null: just the form
	 * @param sql   the SQL statement of the plan, or null (DBMS_XPLAN.DISPLAY_CURSOR prints it above the plan, and that is used then)
	 */
	public static String createPage(String plan, String sql)
	{
		String str = "" +
				"<!DOCTYPE html> \n" +
				"<html lang='en'> \n" +
				" \n" +
				"<head> \n" +
				"    <meta charset='UTF-8'> \n" +
				"    <meta name='viewport' content='width=device-width, initial-scale=1'> \n" +
				"    <title>DbxTune - Oracle Execution Plan</title> \n" +
				"    <meta http-equiv='Cache-Control' content='no-cache, no-store, must-revalidate' /> \n" +
				"    <meta http-equiv='Pragma' content='no-cache' /> \n" +
				"    <meta http-equiv='Expires' content='0' /> \n" +
				" \n" +
				// jQuery, Bootstrap, dbxcentral.css, dbxcentral.utils.js and the login wiring: as every other DbxCentral page
				HtmlStatic.getUserDefinedContentHead() +

				// jQuery UI: the dialog is draggable and resizable
				HtmlUtils.createCssLinkTag("/scripts/jquery/ui/1.14.2/themes/smoothness/jquery-ui.css") +
				HtmlUtils.createJsScriptTag("/scripts/jquery/ui/1.14.2/jquery-ui.min.js") +
				HtmlUtils.createJsScriptTag("/scripts/jquery/ui/1.14.2/jquery.ui.touch-punch.min.js") +

				// The renderer. dbxShowplanGraph.js is the layout code it shares with the ASE and SQL Server renderers, so it comes first
				HtmlUtils.createJsScriptTag("/scripts/dbxtune/js/dbxShowplanGraph.js") +
				HtmlUtils.createJsScriptTag("/scripts/dbxtune/js/dbxShowplanOracle.js") +
				HtmlUtils.createJsScriptTag("/scripts/panzoom/4.6.2/panzoom.min.js") +

				// The LLM Optimization Advice / LLM Prompt Preview sections of the dialog
				"    <script src='/scripts/marked/18.0.14/marked.min.js'></script> \n" +
				"    <script src='/scripts/dompurify/3.4.16/purify.min.js'></script> \n" +
				"    <script src='/scripts/dbxtune/js/dbxLlmAdvice.js'></script> \n" +

				// The SQL Text section of the dialog: syntax colours and "Format SQL"
				HtmlUtils.createCssLinkTag("/scripts/prism/prism-1.30.0.css") +
				HtmlUtils.createJsScriptTag("/scripts/prism/prism-1.30.0.js") +
				HtmlUtils.createJsScriptTag("/scripts/sql-formatter/15.9.0/sql-formatter.min.js") +
				HtmlUtils.createJsScriptTag("/scripts/dbxtune/js/dbxSqlFormat.js") +

				// The dialog itself: showOracleShowplanDialog()
				HtmlUtils.createJsScriptTag("/scripts/dbxtune/js/dbxShowplan.js") +
				"</head> \n" +
				" \n" +
				"<body> \n" +
				HtmlStatic.getHtmlNavbar(PageSection.Tools, "<li class='nav-item'><a class='nav-link' href='/showplan/'>All Showplan Viewers</a></li>", true) +

				"    <div class='container-fluid px-4 py-3' style='max-width: 1100px;'> \n" +
				"      <h2>Oracle Execution Plan Viewer</h2> \n" +
				"      <p class='text-muted'>Paste the text that <code>DBMS_XPLAN</code> prints and press <b>Show Plan</b>. The plan is drawn in your browser: it is only sent to the server if you open one of the LLM sections of the dialog.</p> \n" +
				"      <div class='card shadow-sm'> \n" +
				"        <div class='card-body'> \n" +
				"          <div class='mb-2'> \n" +
				"            <button type='button' class='btn btn-outline-secondary btn-sm' onclick=\"document.getElementById('ora-showplan-file-input').click();\">&#128194; Load from File</button> \n" +
				"            <input type='file' id='ora-showplan-file-input' accept='.txt,.log,.lst,.out' style='display:none' onchange='oraShowplanPasteLoadFile(event)'> \n" +
				"            <span style='font-size:0.8em;color:#888;margin-left:6px;'>or drag &amp; drop a file onto the text area below</span> \n" +
				"          </div> \n" +
				"          <textarea class='form-control mb-3' id='plan' rows='22' wrap='off' spellcheck='false' style='font-family:monospace;font-size:0.8rem;' placeholder='Paste the output of DBMS_XPLAN here...' ondragover='oraShowplanPasteDragOver(event)' ondragleave='oraShowplanPasteDragLeave(event)' ondrop='oraShowplanPasteDrop(event)'></textarea> \n" +
				"          <details class='mb-3'> \n" +
				"            <summary style='cursor:pointer;font-size:0.85em;color:#555;'>SQL Text (optional)</summary> \n" +
				"            <p class='text-muted' style='font-size:0.8em;margin-top:6px;'>Only needed when the plan text does not hold the statement: <code>DISPLAY_CURSOR</code> prints it above the plan, <code>EXPLAIN PLAN</code> + <code>DISPLAY</code> does not.</p> \n" +
				"            <textarea class='form-control' id='sql' rows='6' spellcheck='false' style='font-family:monospace;font-size:0.8rem;' placeholder='Paste the SQL statement here (optional)...'></textarea> \n" +
				"          </details> \n" +
				"          <button type='button' class='btn btn-primary' onclick='oraShowplanPasteShow()'>Show Plan</button> \n" +
				"          <span id='ora-showplan-paste-msg' style='margin-left:10px;font-size:0.85em;color:#b45309;'></span> \n" +
				"        </div> \n" +
				"      </div> \n" +

				// How to get a plan text: the question of everyone who is not an Oracle DBA
				"      <div class='card shadow-sm mt-3'> \n" +
				"        <div class='card-body' style='font-size:0.85em;'> \n" +
				"          <h6>How to get the plan text</h6> \n" +
				"          <p class='mb-1'>The plan of a statement that <b>is in the shared pool</b> (it has run, or is running). The SQL_ID is in <code>V$SESSION</code> and <code>V$SQL</code>, and in the Active Statements and SQL Statistics of OracleTune:</p> \n" +
				"          <pre class='mb-2' style='background:#f4f4f4;padding:6px 8px;border-radius:3px;'>select * from table(DBMS_XPLAN.DISPLAY_CURSOR('&lt;sql_id&gt;', null, 'TYPICAL'));</pre> \n" +
				"          <p class='mb-1'>The plan of the statement <b>you just ran</b> in this session, with what it really did (rows, time, buffers per operation). The statement needs the hint <code>/*+ gather_plan_statistics */</code>, or the session <code>STATISTICS_LEVEL = ALL</code>; in SQL*Plus also <code>set serveroutput off</code>:</p> \n" +
				"          <pre class='mb-2' style='background:#f4f4f4;padding:6px 8px;border-radius:3px;'>select * from table(DBMS_XPLAN.DISPLAY_CURSOR(null, null, 'ALLSTATS LAST'));</pre> \n" +
				"          <p class='mb-1'>The plan the optimizer <b>would</b> use, without running the statement:</p> \n" +
				"          <pre class='mb-2' style='background:#f4f4f4;padding:6px 8px;border-radius:3px;'>explain plan for &lt;statement&gt;;\nselect * from table(DBMS_XPLAN.DISPLAY);</pre> \n" +
				"          <p class='mb-0 text-muted'>These need no extra license from Oracle. <code>DBMS_XPLAN.DISPLAY_AWR</code> and SQL Monitor reports do (the Diagnostics Pack and the Tuning Pack).</p> \n" +
				"        </div> \n" +
				"      </div> \n" +
				"    </div> \n" +
				" \n" +
				"    <script> \n" +
				"        function oraShowplanPasteShow() { \n" +
				"            var plan = document.getElementById('plan').value; \n" +
				"            var msg  = document.getElementById('ora-showplan-paste-msg'); \n" +
				"            msg.textContent = plan.trim() ? '' : 'Paste a plan first.'; \n" +
				"            if (plan.trim()) showOracleShowplanDialog(plan, document.getElementById('sql').value, ''); \n" +
				"        } \n" +
				"        function oraShowplanPasteReadFile(file) { \n" +
				"            if (!file) return; \n" +
				"            var reader = new FileReader(); \n" +
				"            reader.onload = function(e) { document.getElementById('plan').value = e.target.result; }; \n" +
				"            reader.readAsText(file); \n" +
				"        } \n" +
				"        function oraShowplanPasteLoadFile(event) { \n" +
				"            oraShowplanPasteReadFile(event.target.files[0]); \n" +
				"            event.target.value = ''; \n" + // so that choosing the same file again fires onchange again
				"        } \n" +
				"        function oraShowplanPasteDragOver(event) { \n" +
				"            event.preventDefault(); \n" +
				"            event.currentTarget.style.borderColor = '#4a90d9'; \n" +
				"        } \n" +
				"        function oraShowplanPasteDragLeave(event) { \n" +
				"            event.currentTarget.style.borderColor = ''; \n" +
				"        } \n" +
				"        function oraShowplanPasteDrop(event) { \n" +
				"            event.preventDefault(); \n" +
				"            event.currentTarget.style.borderColor = ''; \n" +
				"            oraShowplanPasteReadFile(event.dataTransfer.files && event.dataTransfer.files[0]); \n" +
				"        } \n" +
				"    </script> \n";

		// A plan was posted: put it in the form too (closing the dialog must not leave an empty page) and open the dialog on it
		if (StringUtil.hasValue(plan))
		{
			str += "" +
				"    <script> \n" +
				"        document.getElementById('plan').value = '" + StringEscapeUtils.escapeEcmaScript(plan) + "'; \n" +
				"        document.getElementById('sql').value  = '" + StringEscapeUtils.escapeEcmaScript(StringUtil.nullToValue(sql, "")) + "'; \n" +
				"        oraShowplanPasteShow(); \n" +
				"    </script> \n";
		}

		str += "" +
				HtmlStatic.getJavaScriptAtEnd(true) +
				"</body> \n" +
				" \n" +
				"</html> \n" +
				"";
		return str;
	}
}
