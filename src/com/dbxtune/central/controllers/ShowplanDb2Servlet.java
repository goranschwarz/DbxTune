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
import com.dbxtune.central.llm.LlmClientRegistry;
import com.dbxtune.cm.db2.Db2ExplainUtils;
import com.dbxtune.utils.DbUtils;
import com.dbxtune.utils.HtmlUtils;
import com.dbxtune.utils.StringUtil;


/**
 * The Db2 Execution Plan Viewer page: <code>/showplan/db2</code> (built on ShowplanOracleServlet / ShowplanMySqlServlet)
 * <p>
 * The plan is the JSON that Db2Tune records (CmActiveStatements, column EXEC_PLAN: the explain tables after EXPLAIN_FROM_SECTION,
 * see Db2ExplainUtils). It is parsed and drawn in the browser by <code>dbxShowplanDb2.js</code>, on this page.
 * <p>
 * NOTE: The plan dialog of dbxShowplan.js (the one the Active Statements panel opens) can not show a Db2 plan yet: on this branch
 * it has no vendor switch. So this page draws the plan itself; it is hooked into the dialog when the branches are merged.
 * <ul>
 *   <li>GET: a form to paste a plan into. The button draws what was pasted, without a round trip to the server.</li>
 *   <li>POST (plan, sql): the same page, with the plan that was posted drawn, and the plan as text (as db2exfmt shows it).
 *       This is how the links of a Daily Summary Report hand their plan over.</li>
 * </ul>
 */
public class ShowplanDb2Servlet
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
		out.print(createPage(null, null, null, null, null, null));
		out.flush();
		out.close();
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		String plan = request.getParameter("plan");
		String sql  = request.getParameter("sql");

		// Optional, from the links in a Daily Summary Report: where the plan came from (handed on to the LLM advice)
		String srv          = request.getParameter("srv");
		String dbname       = request.getParameter("dbname");
		String dbmsVersion  = request.getParameter("dbmsVersion");  // recorded by the Daily Summary Report
		String workloadData = request.getParameter("workloadData"); // execution statistics of the statement, from the same report

		if (_logger.isDebugEnabled())
			_logger.debug("/showplan/db2: received request from: remoteHost='" + request.getRemoteHost() + "', remoteAddr='" + request.getRemoteAddr() + "', remoteUser='" + request.getRemoteUser() + "'.");

		if (StringUtil.isNullOrBlank(plan))
		{
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Expecting a Db2 execution plan (the JSON Db2Tune records) in the parameter 'plan', but it was empty.");
			return;
		}

		response.setContentType("text/html; charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
		PrintWriter out = response.getWriter();
		out.print(createPage(plan, sql, srv, dbname, dbmsVersion, workloadData));
		out.flush();
		out.close();
	}

	/**
	 * @param plan          the plan JSON to draw, or null: just the form
	 * @param sql           the SQL statement of the plan, or null (the plan has the statement text Db2 explained)
	 * @param srv           the DbxTune server name the plan came from, or null (a pasted plan)
	 * @param dbname        the schema of the statement, or null
	 * @param dbmsVersion   the Db2 version a report recorded, or null
	 * @param workloadData  execution statistics of the statement as a report sends them (JSON), or null
	 */
	public static String createPage(String plan, String sql, String srv, String dbname, String dbmsVersion, String workloadData)
	{
		boolean llmEnabled = LlmClientRegistry.isFeatureEnabled();

		String str = "" +
				"<!DOCTYPE html> \n" +
				"<html lang='en'> \n" +
				" \n" +
				"<head> \n" +
				"    <meta charset='UTF-8'> \n" +
				"    <meta name='viewport' content='width=device-width, initial-scale=1'> \n" +
				"    <title>DbxTune - Db2 Execution Plan</title> \n" +
				"    <meta http-equiv='Cache-Control' content='no-cache, no-store, must-revalidate' /> \n" +
				"    <meta http-equiv='Pragma' content='no-cache' /> \n" +
				"    <meta http-equiv='Expires' content='0' /> \n" +
				" \n" +
				// jQuery, Bootstrap, dbxcentral.css, dbxcentral.utils.js and the login wiring: as every other DbxCentral page
				HtmlStatic.getUserDefinedContentHead() +

				// The renderer. dbxShowplanGraph.js is the layout code (and the colours) it shares with the other renderers, so it comes first
				HtmlUtils.createJsScriptTag("/scripts/dbxtune/js/dbxShowplanGraph.js") +
				HtmlUtils.createJsScriptTag("/scripts/dbxtune/js/dbxShowplanDb2.js") +
				"</head> \n" +
				" \n" +
				"<body> \n" +
				HtmlStatic.getHtmlNavbar(PageSection.Tools, "<li class='nav-item'><a class='nav-link' href='/showplan/'>All Showplan Viewers</a></li>", true) +

				"    <div class='container-fluid px-4 py-3'> \n" +
				"      <h2>Db2 Execution Plan Viewer</h2> \n" +
				"      <p class='text-muted'>Paste the plan that Db2Tune recorded (the <code>EXEC_PLAN</code> column of the 'Active Statements' tab: the explain tables after <code>EXPLAIN_FROM_SECTION</code>, as JSON) and press <b>Show Plan</b>. The plan is drawn in your browser.</p> \n" +
				"      <div class='card shadow-sm' style='max-width: 1100px;'> \n" +
				"        <div class='card-body'> \n" +
				"          <textarea class='form-control mb-3' id='plan' rows='10' wrap='off' spellcheck='false' style='font-family:monospace;font-size:0.8rem;' placeholder='Paste the plan JSON here ({\"planFormat\":\"db2-explain-tables-1\", ...})'></textarea> \n" +
				"          <details class='mb-3'> \n" +
				"            <summary style='cursor:pointer;font-size:0.85em;color:#555;'>SQL Text (optional)</summary> \n" +
				"            <p class='text-muted' style='font-size:0.8em;margin-top:6px;'>Only needed to show another text than the one in the plan (the plan has the statement Db2 explained).</p> \n" +
				"            <textarea class='form-control' id='sql' rows='6' spellcheck='false' style='font-family:monospace;font-size:0.8rem;' placeholder='Paste the SQL statement here (optional)...'></textarea> \n" +
				"          </details> \n" +
				"          <button type='button' class='btn btn-primary' onclick='db2ShowplanShow()'>Show Plan</button> \n" +
				"          <span id='db2-showplan-msg' style='margin-left:10px;font-size:0.85em;color:#b45309;'></span> \n" +
				"        </div> \n" +
				"      </div> \n" +

				// The plan
				"      <div id='db2-result' style='display:none' class='mt-3'> \n" +
				"        <h5>SQL Text</h5> \n" +
				"        <pre id='db2-sql' style='background:var(--sp-bg-code);color:var(--sp-text);padding:6px 8px;border-radius:3px;max-height:20vh;overflow:auto;white-space:pre-wrap;'></pre> \n" +
				"        <div class='mb-2'> \n" +
				"          <span style='font-size:0.85em;'>Layout:</span> \n" +
				"          <div class='btn-group btn-group-sm' role='group'> \n" +
				"            <button type='button' class='btn btn-outline-secondary active' id='db2-layout-v' onclick='db2ShowplanLayout(false)'>Top-down</button> \n" +
				"            <button type='button' class='btn btn-outline-secondary'        id='db2-layout-h' onclick='db2ShowplanLayout(true)'>Left-right</button> \n" +
				"          </div> \n" +
				(llmEnabled
					? "          <form id='db2-llm-form' method='post' action='/llm-advice' target='_blank' style='display:inline;margin-left:12px;' onsubmit='db2ShowplanLlmFill()'> \n" +
					  "            <input type='hidden' name='sql'> <input type='hidden' name='plan'> \n" +
					  "            <input type='hidden' name='dbVendor'     value='" + StringEscapeUtils.escapeHtml4(DbUtils.DB_PROD_NAME_DB2_LUW) + "'> \n" +
					  "            <input type='hidden' name='srv'          value='" + StringEscapeUtils.escapeHtml4(StringUtil.nullToValue(srv         , "")) + "'> \n" +
					  "            <input type='hidden' name='dbname'       value='" + StringEscapeUtils.escapeHtml4(StringUtil.nullToValue(dbname      , "")) + "'> \n" +
					  "            <input type='hidden' name='dbmsVersion'  value='" + StringEscapeUtils.escapeHtml4(StringUtil.nullToValue(dbmsVersion , "")) + "'> \n" +
					  "            <input type='hidden' name='workloadData' value='" + StringEscapeUtils.escapeHtml4(StringUtil.nullToValue(workloadData, "")) + "'> \n" +
					  "            <button type='submit' class='btn btn-sm btn-outline-primary'>Get LLM Optimization Advice</button> \n" +
					  "          </form> \n"
					: "") +
				"        </div> \n" +
				"        <div class='row'> \n" +
				"          <div class='col-lg-9'><div id='db2-diagram' style='overflow:auto;max-height:75vh;border:1px solid var(--sp-border);border-radius:4px;'></div></div> \n" +
				"          <div class='col-lg-3'><div id='db2-props' style='overflow:auto;max-height:75vh;font-size:0.8em;'></div></div> \n" +
				"        </div> \n" +
				"        <h5 class='mt-3'>Plan Analysis</h5> \n" +
				"        <ul id='db2-findings' style='font-size:0.85em;'></ul> \n" +
				(StringUtil.hasValue(plan)
					? "        <details class='mt-2'><summary>The plan as text (as db2exfmt shows it)</summary><pre style='font-size:0.8em;'>" + StringEscapeUtils.escapeHtml4(Db2ExplainUtils.planJsonToText(plan)) + "</pre></details> \n"
					: "") +
				"      </div> \n" +

				// How to get a plan
				"      <div class='card shadow-sm mt-3' style='max-width: 1100px;'> \n" +
				"        <div class='card-body' style='font-size:0.85em;'> \n" +
				"          <h6>Where the plans come from</h6> \n" +
				"          <p class='mb-1'>Db2Tune gets the plan of a statement that has run for a while (5 seconds, the tab 'Active Statements', column <code>EXEC_PLAN</code>) with <code>EXPLAIN_FROM_SECTION</code>: the plan Db2 really uses, from the package cache. "
				                     + "It needs the explain tables in the schema of the collector's login, and EXECUTE on the procedure (see <code>DbmsSetup/db2.txt</code>):</p> \n" +
				"          <pre class='mb-2' style='background:var(--sp-bg-code);color:var(--sp-text);padding:6px 8px;border-radius:3px;'>CALL SYSPROC.SYSINSTALLOBJECTS('EXPLAIN', 'C', 'USERSPACE1', '&lt;collector login&gt;');\nGRANT EXECUTE ON PROCEDURE SYSPROC.EXPLAIN_FROM_SECTION TO ROLE DBXTUNE_MON;</pre> \n" +
				"          <p class='mb-0'>The Daily Summary Report links its plans here (Top SQL: 'View Execution Plan').</p> \n" +
				"        </div> \n" +
				"      </div> \n" +
				"    </div> \n" +
				" \n" +
				"    <script> \n" +
				"        var _db2Parsed = null; \n" +
				"        var _db2Horizontal = false; \n" +
				"        function db2ShowplanShow() { \n" +
				"            var plan = document.getElementById('plan').value; \n" +
				"            var msg  = document.getElementById('db2-showplan-msg'); \n" +
				"            msg.textContent = ''; \n" +
				"            if (!plan.trim()) { msg.textContent = 'Paste a plan first.'; return; } \n" +
				"            var parsed = Db2Showplan.parseText(plan); \n" +
				"            if (!parsed) { msg.textContent = Db2Showplan.getLastParseError(); return; } \n" +
				"            _db2Parsed = parsed; \n" +
				"            DbxShowplanTheme.apply(document.body); \n" +
				"            var sql = document.getElementById('sql').value || parsed.statements[0].steps[0].root._planInfo.sqlText || ''; \n" +
				"            document.getElementById('db2-sql').textContent = sql; \n" +
				"            document.getElementById('db2-result').style.display = ''; \n" +
				"            db2ShowplanRender(); \n" +
				"        } \n" +
				"        function db2ShowplanLayout(horizontal) { \n" +
				"            _db2Horizontal = horizontal; \n" +
				"            document.getElementById('db2-layout-v').classList.toggle('active', !horizontal); \n" +
				"            document.getElementById('db2-layout-h').classList.toggle('active',  horizontal); \n" +
				"            db2ShowplanRender(); \n" +
				"        } \n" +
				"        function db2ShowplanRender() { \n" +
				"            Db2Showplan.render(document.getElementById('db2-diagram'), _db2Parsed, { \n" +
				"                horizontal:        _db2Horizontal, \n" +
				"                propsTarget:       document.getElementById('db2-props'), \n" +
				"                onFindingsChanged: db2ShowplanFindings \n" +
				"            }); \n" +
				"        } \n" +
				// The findings of the Plan Analysis: a click shows the box of the operator
				"        function db2ShowplanFindings(findings) { \n" +
				"            var ul = document.getElementById('db2-findings'); \n" +
				"            ul.innerHTML = ''; \n" +
				"            if (!findings.length) { var li0 = document.createElement('li'); li0.textContent = 'Nothing to point at in this plan.'; ul.appendChild(li0); return; } \n" +
				"            findings.forEach(function (f) { \n" +
				"                var li = document.createElement('li'); \n" +
				"                var b  = document.createElement('b'); \n" +
				"                b.style.color = f.severity === 'info' ? 'var(--sp-blue)' : 'var(--sp-err)'; \n" +
				"                b.textContent = (f.severity === 'info' ? 'Info' : 'Warning') + ' [' + f.category + ']' + (f.nodeId !== null && f.nodeId !== undefined ? ' [Node ' + f.nodeId + ']' : '') + ': '; \n" +
				"                li.appendChild(b); \n" +
				"                li.appendChild(document.createTextNode(f.title + '. ' + f.detail)); \n" +
				"                if (f.nodeId !== null && f.nodeId !== undefined) { \n" +
				"                    li.style.cursor = 'pointer'; \n" +
				"                    li.onclick = function () { \n" +
				"                        var box = document.querySelector('#db2-diagram .db2-plan-box[data-va=\"' + f.nodeId + '\"]'); \n" +
				"                        if (!box) return; \n" +
				"                        box.scrollIntoView({ block: 'center', inline: 'center' }); \n" +
				"                        box.classList.remove('db2-plan-flash'); void box.offsetWidth; box.classList.add('db2-plan-flash'); \n" +
				"                    }; \n" +
				"                } \n" +
				"                ul.appendChild(li); \n" +
				"            }); \n" +
				"        } \n" +
				"        function db2ShowplanLlmFill() { \n" +
				"            var f = document.getElementById('db2-llm-form'); \n" +
				"            f.elements['plan'].value = document.getElementById('plan').value; \n" +
				"            f.elements['sql'].value  = document.getElementById('db2-sql').textContent; \n" +
				"        } \n" +
				"    </script> \n";

		// A plan was posted: put it in the form too, and draw it
		if (StringUtil.hasValue(plan))
		{
			str += "" +
				"    <script> \n" +
				"        document.getElementById('plan').value = '" + StringEscapeUtils.escapeEcmaScript(plan) + "'; \n" +
				"        document.getElementById('sql').value  = '" + StringEscapeUtils.escapeEcmaScript(StringUtil.nullToValue(sql, "")) + "'; \n" +
				"        db2ShowplanShow(); \n" +
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
