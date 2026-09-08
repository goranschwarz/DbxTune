/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
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


public class ShowplanSqlServerServlet
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
		out.print(createHashDrivenOutput());
		out.flush();
		out.close();
	}

	/**
	 * Same page as {@link #createPasteFormOutput()} (paste form, in case there's nothing to show),
	 * but also loads the full Showplan dialog assets and a small script that reads the plan straight
	 * from the URL FRAGMENT ({@code #plan=...&sql=...&srv=...&dbname=...}) - never sent to the
	 * server, so a large XML plan never risks a "414 URI Too Long" error the way a query string
	 * would - and opens it via {@code showSqlServerShowplanDialog(...)} client-side. Used by the
	 * mail-safe "View Execution Plan" link the Daily Summary Report links here to (see
	 * {@code LlmAdviceServlet} for the identical pattern, and {@code ShowplanLinkBuilder} for the
	 * link itself).
	 * <p>
	 * If the fragment has no {@code plan} param (e.g. a bare {@code GET /showplan/sqlserver}), the
	 * script simply no-ops and this looks exactly like {@link #createPasteFormOutput()}.
	 */
	public static String createHashDrivenOutput()
	{
		String str = "" +
				"<!DOCTYPE html> \n" +
				"<html lang='en'> \n" +
				" \n" +
				"<head> \n" +
				"    <meta charset='UTF-8'> \n" +
				"    <meta name='viewport' content='width=device-width, initial-scale=1'> \n" +
				"    <title>DbxTune - SQL Server Showplan</title> \n" +
				"    <meta http-equiv='Cache-Control' content='no-cache, no-store, must-revalidate' /> \n" +
				"    <meta http-equiv='Pragma' content='no-cache' /> \n" +
				"    <meta http-equiv='Expires' content='0' /> \n" +
				" \n" +
				HtmlStatic.getUserDefinedContentHead() +
				HtmlUtils.createCssLinkTag("/scripts/jquery/ui/1.14.1/themes/smoothness/jquery-ui.css") +
				HtmlUtils.createJsScriptTag("/scripts/jquery/ui/1.14.1/jquery-ui.min.js") +
				HtmlUtils.createJsScriptTag("/scripts/jquery/ui/1.14.1/jquery.ui.touch-punch.min.js") +
				HtmlUtils.createJsScriptTag("/scripts/showplan/sqlserver/dist/qp.js", "https://www.dbxtune.com/sqlserver_showplan/dist/qp.js") +
				// The Plan Analysis section is driven by this - without it dbxShowplan.js hides that
				// section entirely. graph.html has always loaded it; this standalone page never did, so
				// "Plan Analysis" was silently missing here.
				HtmlUtils.createJsScriptTag("/scripts/dbxtune/js/dbxShowplanAnalyzer.js") +
				// The native graphical Showplan renderer. Loaded ALONGSIDE qp.js above, not instead of it -
				// the dialog toolbar toggles between the two, so both must be present. dbxShowplanGraph.js
				// holds the layout plumbing it shares with the ASE renderer, so it must come first.
				HtmlUtils.createJsScriptTag("/scripts/dbxtune/js/dbxShowplanGraph.js") +
				HtmlUtils.createJsScriptTag("/scripts/dbxtune/js/dbxShowplanSqlServer.js") +
				// Backs the toolbar's "Enable Zoom"/"Zoom to Fit" buttons. graph.html and the ASE standalone
				// page both load this; this page never did, so those buttons threw "Panzoom is not defined".
				HtmlUtils.createJsScriptTag("/scripts/panzoom/4.5.1/panzoom.min.js") +
				HtmlUtils.createCssLinkTag("/scripts/showplan/sqlserver/css/qp.css", "https://www.dbxtune.com/sqlserver_showplan/css/qp.css") +
				HtmlUtils.createJsScriptTag("/scripts/chartjs/2.7.3/Chart.bundle.js") +
				// srv/dbname (if any) only live in the URL fragment, never known server-side here - so
				// unlike createShowplanOutput()'s conditional include, always load these.
				HtmlUtils.createJsScriptTag("/scripts/dbxtune/js/dbxSqlTableNames.js") +
				HtmlUtils.createCssLinkTag("/scripts/dbxtune/css/dbxTableInfo.css") +
				"    <script src='/scripts/marked/18.0.5/marked.min.js'></script> \n" +
				"    <script src='/scripts/dompurify/3.4.11/purify.min.js'></script> \n" +
				"    <script src='/scripts/dbxtune/js/dbxLlmAdvice.js'></script> \n" +
				HtmlUtils.createCssLinkTag("/scripts/prism/prism-1.30.0.css") +
				HtmlUtils.createJsScriptTag("/scripts/prism/prism-1.30.0.js") +
				HtmlUtils.createJsScriptTag("/scripts/sql-formatter/12.1.3/sql-formatter.min.js") +
				HtmlUtils.createJsScriptTag("/scripts/dbxtune/js/dbxShowplan.js") +
				"</head> \n" +
				" \n" +
				"<body> \n" +
				HtmlStatic.getHtmlNavbar(PageSection.Tools, "<li class='nav-item'><a class='nav-link' href='/showplan/'>All Showplan Viewers</a></li>", true) +
				createPasteFormBodyHtml() +
				"    <script> \n" +
				"        (function () { \n" +
				"            // Params read from the URL FRAGMENT (#...), not the query string (?...) - the \n" +
				"            // fragment is never sent to the server, so a large XML plan/SQL text here never \n" +
				"            // risks a 'URI Too Long' error the way a query string would. Used by the mail-safe \n" +
				"            // 'View Execution Plan' link the Daily Summary Report links here to. \n" +
				"            var qs = new URLSearchParams(window.location.hash.replace(/^#/, '')); \n" +
				"            var plan = qs.get('plan'); \n" +
				"            if (!plan) return; \n" +
				"            var sql = qs.get('sql') || ''; \n" +
				"            var srv = qs.get('srv') || ''; \n" +
				"            var dbname = qs.get('dbname') || ''; \n" +
				// Execution statistics from the Daily Summary Report - this dialog's own LLM advice
				// section would otherwise lose the workload profile.
				"            var workloadData = qs.get('workloadData') || ''; \n" +
				"            // Also fill in the paste form behind the dialog - it has no 'reopen' mechanism of its \n" +
				"            // own (closing the dialog just clears its own containers), so without this, closing it \n" +
				"            // would leave the user staring at a form that looks like it never received their plan. \n" +
				"            document.getElementById('plan').value = plan; \n" +
				"            document.getElementById('sql').value = sql; \n" +
				"            document.getElementById('srv').value = srv; \n" +
				"            document.getElementById('dbname').value = dbname; \n" +
				"            ssShowplanPasteCheckSqlText(); \n" +
				"            showSqlServerShowplanDialog(plan, sql, '', { srv: srv, dbname: dbname, workloadData: workloadData }); \n" +
				"        })(); \n" +
				"    </script> \n" +
				HtmlStatic.getJavaScriptAtEnd(true) +
				"</body> \n" +
				" \n" +
				"</html> \n" +
				"";
		return str;
	}

	/**
	 * The paste-and-submit form shown for a plain {@code GET /showplan/sqlserver} - shares the same
	 * navbar/head chrome as {@link #createShowplanOutput(String, String, String, String, String)} so
	 * landing on this page and viewing a submitted plan look like the same section, not two different
	 * pages.
	 */
	public static String createPasteFormOutput()
	{
		String str = "" +
				"<!DOCTYPE html> \n" +
				"<html lang='en'> \n" +
				" \n" +
				"<head> \n" +
				"    <meta charset='UTF-8'> \n" +
				"    <meta name='viewport' content='width=device-width, initial-scale=1'> \n" +
				"    <title>DbxTune - SQL Server Showplan</title> \n" +
				"    <meta name='robots' content='max-image-preview:large' /> \n" +
				" \n" +
				HtmlStatic.getUserDefinedContentHead() +
				"</head> \n" +
				" \n" +
				"<body> \n" +
				HtmlStatic.getHtmlNavbar(PageSection.Tools, "<li class='nav-item'><a class='nav-link' href='/showplan/'>All Showplan Viewers</a></li>", true) +
				createPasteFormBodyHtml() +
				HtmlStatic.getJavaScriptAtEnd(true) +
				"</body> \n" +
				" \n" +
				"</html> \n" +
				"";
		return str;
	}

	/**
	 * The paste-form markup + its file-load/drag-drop/server-autocomplete script, factored out so
	 * {@link #createShowplanOutput(String, String, String, String, String)} can put the same real,
	 * working form behind the Showplan dialog it opens - the dialog itself has no "reopen"/
	 * navigate-away mechanism (closing it just clears its own containers), so without this closing
	 * the dialog would otherwise strand the user on a blank page. Mirrors
	 * {@code ShowplanAseServlet.createPasteFormBodyHtml()} - same fields/behavior, just XML-only (no
	 * isXml auto-detect needed) and posting to {@code /showplan/sqlserver}.
	 */
	private static String createPasteFormBodyHtml()
	{
		return "" +
				"    <div class='container-fluid px-4 py-3' style='max-width: 900px;'> \n" +
				"      <h2>SQL Server Showplan Viewer</h2> \n" +
				"      <p class='text-muted'>Paste an XML execution plan below and press Submit.</p> \n" +
				"      <div class='card shadow-sm'> \n" +
				"        <div class='card-body'> \n" +
				"          <form id='showplan-form' action='/showplan/sqlserver' method='post'> \n" +
				// Both optional - filling them in (with an actively-monitored DbxTune server name)
				// unlocks the same live Table Information section and DDL-enriched LLM advice the
				// modal Showplan dialog gets; left blank, this page behaves exactly as it always has -
				// showSqlServerShowplanDialog() itself no-ops the srv/dbname-gated sections when
				// meta.srv/meta.dbname are empty.
				"            <div class='form-row'> \n" +
				"              <div class='col-md-4 mb-2'> \n" +
				"                <label for='srv' class='mb-1' style='font-size:0.85em;color:#555;'>At Server Name (optional)</label> \n" +
				"                <input type='text' class='form-control form-control-sm' id='srv' name='srv' list='ss-showplan-srv-datalist' placeholder='e.g. PROD_SQL_01' autocomplete='off'> \n" +
				"                <datalist id='ss-showplan-srv-datalist'></datalist> \n" +
				"              </div> \n" +
				"              <div class='col-md-4 mb-2'> \n" +
				"                <label for='dbname' class='mb-1' style='font-size:0.85em;color:#555;'>Database Name (optional)</label> \n" +
				"                <input type='text' class='form-control form-control-sm' id='dbname' name='dbname' placeholder='e.g. mydb'> \n" +
				"              </div> \n" +
				"            </div> \n" +
				"            <p class='text-muted' style='font-size:0.8em;margin-top:-4px;'>Fill these in (with an actively-monitored DbxTune server) to also get a live Table Information section and DDL-enriched LLM advice for the tables this plan touches - leave blank to just view the diagram.</p> \n" +
				"            <div class='mb-2'> \n" +
				"              <button type='button' class='btn btn-outline-secondary btn-sm' onclick=\"document.getElementById('ss-showplan-file-input').click();\">&#128194; Load from File</button> \n" +
				"              <input type='file' id='ss-showplan-file-input' accept='.xml,.sqlplan' style='display:none' onchange='ssShowplanPasteLoadFile(event)'> \n" +
				"              <span style='font-size:0.8em;color:#888;margin-left:6px;'>or drag &amp; drop a file onto the text area below</span> \n" +
				"            </div> \n" +
				"            <textarea class='form-control mb-3' id='plan' name='plan' rows='20' placeholder='Paste the XML Showplan here...' oninput='ssShowplanPasteCheckSqlText()' ondragover='ssShowplanPasteDragOver(event)' ondragleave='ssShowplanPasteDragLeave(event)' ondrop='ssShowplanPasteDrop(event)'></textarea> \n" +
				// Optional - most XML plans already carry the SQL as a StatementText attribute, in
				// which case this stays collapsed. ssShowplanPasteCheckSqlText() below flips the
				// summary text and auto-opens it when the pasted plan doesn't have one, so the user
				// doesn't have to know up front whether their plan needs this.
				"            <details id='ss-showplan-sqltext-details' class='mb-3'> \n" +
				"              <summary id='ss-showplan-sqltext-summary' style='cursor:pointer;font-size:0.85em;color:#555;'>SQL Text (optional)</summary> \n" +
				"              <p class='text-muted' style='font-size:0.8em;margin-top:6px;'>Paste the SQL statement here if the plan above doesn't already include it - enables Table Information and LLM Optimization Advice for it.</p> \n" +
				"              <textarea class='form-control' id='sql' name='sql' rows='6' placeholder='Paste the SQL statement here (optional)...'></textarea> \n" +
				"            </details> \n" +
				"            <button type='submit' class='btn btn-primary'>Submit</button> \n" +
				"          </form> \n" +
				"        </div> \n" +
				"      </div> \n" +
				"    </div> \n" +
				" \n" +
				"    <script> \n" +
				"        function ssShowplanPasteCheckSqlText() { \n" +
				"            var planText = document.getElementById('plan').value; \n" +
				"            var sqlText  = document.getElementById('sql').value; \n" +
				"            var details  = document.getElementById('ss-showplan-sqltext-details'); \n" +
				"            var summary  = document.getElementById('ss-showplan-sqltext-summary'); \n" +
				"            var hasSql   = /StatementText\\s*=\\s*\"[^\"]*\\S[^\"]*\"/i.test(planText); \n" +
				"            if (hasSql) { \n" +
				"                summary.innerHTML = 'SQL Text (optional) &mdash; <span style=\"color:#0f5132;\">already found in the plan</span>'; \n" +
				"            } else if (planText.trim() === '') { \n" +
				"                summary.innerHTML = 'SQL Text (optional)'; \n" +
				"            } else { \n" +
				"                summary.innerHTML = 'SQL Text (optional) &mdash; <span style=\"color:#b45309;\">no SQL text found in the plan, consider pasting it below</span>'; \n" +
				"                if (!sqlText) details.open = true; \n" + // don't yank open/shut on top of something the user's already editing
				"            } \n" +
				"        } \n" +
				"        function ssShowplanPasteReadFile(file) { \n" +
				"            if (!file) return; \n" +
				"            var reader = new FileReader(); \n" +
				"            reader.onload = function(e) { document.getElementById('plan').value = e.target.result; ssShowplanPasteCheckSqlText(); }; \n" +
				"            reader.readAsText(file); \n" +
				"        } \n" +
				"        function ssShowplanPasteLoadFile(event) { \n" +
				"            ssShowplanPasteReadFile(event.target.files[0]); \n" +
				"            event.target.value = ''; \n" + // reset so re-selecting the same filename re-fires onchange
				"        } \n" +
				"        function ssShowplanPasteDragOver(event) { \n" +
				"            event.preventDefault(); \n" +
				"            event.currentTarget.style.borderColor = '#4a90d9'; \n" +
				"        } \n" +
				"        function ssShowplanPasteDragLeave(event) { \n" +
				"            event.currentTarget.style.borderColor = ''; \n" +
				"        } \n" +
				"        function ssShowplanPasteDrop(event) { \n" +
				"            event.preventDefault(); \n" +
				"            event.currentTarget.style.borderColor = ''; \n" +
				"            ssShowplanPasteReadFile(event.dataTransfer.files && event.dataTransfer.files[0]); \n" +
				"        } \n" +
				// Best-effort autocomplete - a typo'd/offline server name just surfaces the same
				// srv-not-found/collector-offline error the modal dialog's Table Information already
				// handles gracefully, so this list doesn't need to be authoritative.
				"        fetch('/api/sessions').then(function(r) { return r.json(); }).then(function(list) { \n" +
				"            var dl = document.getElementById('ss-showplan-srv-datalist'); \n" +
				"            (list || []).forEach(function(entry) { \n" +
				"                if (!entry || !entry.serverName) return; \n" +
				"                var opt = document.createElement('option'); \n" +
				"                opt.value = entry.serverName; \n" +
				"                dl.appendChild(opt); \n" +
				"            }); \n" +
				"        }).catch(function() {}); \n" +
				"    </script> \n" +
				"";
	}

	/**
	 * Take a STRING input and build up a SHOWPLAN page
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		String remoteHost = request.getRemoteHost();
		String remoteAddr = request.getRemoteAddr();
		int    remotePort = request.getRemotePort();
		String remoteUser = request.getRemoteUser();

		String plan     = request.getParameter("plan");
		String sql      = request.getParameter("sql");
		String dbVendor = request.getParameter("dbVendor");
		String srv      = request.getParameter("srv");    // optional - enables live table-info lookups
		String dbname   = request.getParameter("dbname"); // optional - required alongside srv above
		// Execution statistics from the Daily Summary Report - the plan viewer has its own
		// "Get LLM Optimization Advice" section, which would otherwise lose the workload profile.
		String workloadData = request.getParameter("workloadData");

		if (_logger.isDebugEnabled())
			_logger.debug("/showplan/sqlserver: received request from: remoteHost='" + remoteHost + "', remoteAddr='" + remoteAddr + "', remotePort='" + remotePort + "', remoteUser='" + remoteUser + "'.");

		String payload = plan;
		if (StringUtil.isNullOrBlank(payload))
		{
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Expecting a SQL Server XML Execution Plan as Payload, but an empty string was sent.");
			return;
		}

		String formattedOutput = createShowplanOutput(payload, sql, dbVendor, srv, dbname, workloadData);

		response.setContentType("text/html; charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
		PrintWriter out = response.getWriter();
		out.print(formattedOutput);
		out.flush();
		out.close();
	}


	public static String createShowplanOutput(String payload)
	{
		return createShowplanOutput(payload, null, null, null, null);
	}

	/**
	 * @param payload  the XML execution plan
	 * @param sql      the SQL statement this plan belongs to, or null if unknown
	 * @param dbVendor DBMS product name for {@code sql}, or null (unused - the dialog always treats
	 *                 this page's plans as SQL Server, same as {@code showSqlServerShowplanDialog()}'s
	 *                 own callers do)
	 * @param srv      name of an actively-monitored DbxTune server this plan came from, or null -
	 *                 when present together with {@code dbname}, unlocks the same live Table
	 *                 Information section and DDL-enriched LLM advice the modal Showplan dialog gets -
	 *                 passed straight through as its {@code meta} param
	 * @param dbname   database name on {@code srv} the tables in {@code sql} live in, or null
	 */
	public static String createShowplanOutput(String payload, String sql, String dbVendor, String srv, String dbname)
	{
		return createShowplanOutput(payload, sql, dbVendor, srv, dbname, null);
	}

	/** @param workloadData raw execution statistics JSON, forwarded to the dialog's LLM advice section (may be null) */
	public static String createShowplanOutput(String payload, String sql, String dbVendor, String srv, String dbname, String workloadData)
	{
		// Both required together - a table-info lookup needs to know both which server and which
		// database to ask. Gates dbxSqlTableNames.js/dbxTableInfo.css (only needed once the dialog's
		// Table Information section can actually do anything) and is passed into the dialog's own
		// meta={srv,dbname} param below, which is what actually turns on every srv/dbname-gated
		// feature (Table Information section, DDL-enriched LLM Advice) - all of that lives entirely
		// inside dbxShowplan.js now, same as the ASE standalone page.
		boolean hasSrvDbname = StringUtil.hasValue(srv) && StringUtil.hasValue(dbname);

		String str = "" +
				"<!DOCTYPE html> \n" +
				"<html lang='en'> \n" +
				" \n" +
				"<head> \n" +
				"    <meta charset='UTF-8'> \n" +
				"    <meta name='viewport' content='width=device-width, initial-scale=1'> \n" +
				"    <title>SQL Server Execution Plan Viewer</title> \n" +
                "     \n" +
				"    <meta http-equiv='Cache-Control' content='no-cache, no-store, must-revalidate' /> \n" +
				"    <meta http-equiv='Pragma' content='no-cache' /> \n" +
				"    <meta http-equiv='Expires' content='0' /> \n" +
				"     \n" +

				// Pulls in jQuery, Bootstrap 4.6.2 (CSS+JS), Font Awesome, dbxcentral.css,
				// dbxcentral.utils.js and dbxLoginModal.js - the same shared head/navbar/login-wiring
				// every other DbxCentral page uses.
				HtmlStatic.getUserDefinedContentHead() +

				// jQuery UI (draggable/resizable + touch-punch) - same includes graph.html uses for
				// the dialog's drag/resize chrome. Optional per dbxShowplan.js's own
				// $.fn.draggable/$.fn.resizable guards (the dialog still opens/functions without it,
				// just isn't draggable/resizable), included here for full parity with graph.html.
				HtmlUtils.createCssLinkTag("/scripts/jquery/ui/1.14.1/themes/smoothness/jquery-ui.css") +
				HtmlUtils.createJsScriptTag("/scripts/jquery/ui/1.14.1/jquery-ui.min.js") +
				HtmlUtils.createJsScriptTag("/scripts/jquery/ui/1.14.1/jquery.ui.touch-punch.min.js") +

				// The dialog's SQL Server tree view still renders via html-query-plan (QP) internally
				// (QP.showPlan()/QP.drawLines()) - same vendored copy graph.html already uses.
				HtmlUtils.createJsScriptTag("/scripts/showplan/sqlserver/dist/qp.js", "https://www.dbxtune.com/sqlserver_showplan/dist/qp.js") +
				// The Plan Analysis section is driven by this - without it dbxShowplan.js hides that
				// section entirely. graph.html has always loaded it; this standalone page never did, so
				// "Plan Analysis" was silently missing here.
				HtmlUtils.createJsScriptTag("/scripts/dbxtune/js/dbxShowplanAnalyzer.js") +
				// The native graphical Showplan renderer. Loaded ALONGSIDE qp.js above, not instead of it -
				// the dialog toolbar toggles between the two, so both must be present. dbxShowplanGraph.js
				// holds the layout plumbing it shares with the ASE renderer, so it must come first.
				HtmlUtils.createJsScriptTag("/scripts/dbxtune/js/dbxShowplanGraph.js") +
				HtmlUtils.createJsScriptTag("/scripts/dbxtune/js/dbxShowplanSqlServer.js") +
				// Backs the toolbar's "Enable Zoom"/"Zoom to Fit" buttons. graph.html and the ASE standalone
				// page both load this; this page never did, so those buttons threw "Panzoom is not defined".
				HtmlUtils.createJsScriptTag("/scripts/panzoom/4.5.1/panzoom.min.js") +
				HtmlUtils.createCssLinkTag("/scripts/showplan/sqlserver/css/qp.css", "https://www.dbxtune.com/sqlserver_showplan/css/qp.css") +

				// Chart.js - only used by the dialog's "Plan Analysis" wait-time bar (guarded with
				// typeof Chart !== 'undefined', so this is optional/degrades gracefully), included for
				// full parity with graph.html.
				HtmlUtils.createJsScriptTag("/scripts/chartjs/2.7.3/Chart.bundle.js") +

				// Needed for the dialog's Table Information section - same shared module dbxShowplan.js
				// itself already uses on graph.html.
				(hasSrvDbname
					? HtmlUtils.createJsScriptTag("/scripts/dbxtune/js/dbxSqlTableNames.js")
					+ HtmlUtils.createCssLinkTag("/scripts/dbxtune/css/dbxTableInfo.css")
					: "") +

				// Whether the dialog's LLM Advice section actually calls a provider (vs. falling back
				// to the "no exec" Prompt Preview section) is decided live, client-side, by the dialog
				// itself (dbxLlmAdvice.isEnabled()) - so these are always loaded once we're rendering an
				// actual plan, same as dbxShowplan.js's own unconditional use on graph.html. Gating this
				// on whether a 'sql' request param happened to be posted was wrong: the paste form has no
				// SQL Text field at all, and the dialog can usually still derive SQL text (or at least a
				// useful placeholder) straight from the plan itself via ssShowplanGetSql() - so a missing
				// 'sql' param here does not mean the dialog will have nothing to show.
				"    <script src='/scripts/marked/18.0.5/marked.min.js'></script> \n"
				+ "    <script src='/scripts/dompurify/3.4.11/purify.min.js'></script> \n"
				+ "    <script src='/scripts/dbxtune/js/dbxLlmAdvice.js'></script> \n"
				+ HtmlUtils.createCssLinkTag("/scripts/prism/prism-1.30.0.css")
				+ HtmlUtils.createJsScriptTag("/scripts/prism/prism-1.30.0.js")
				+ HtmlUtils.createJsScriptTag("/scripts/sql-formatter/12.1.3/sql-formatter.min.js") +

				// The real dialog - everything this page used to hand-build (the plain QP.showPlan()
				// call and single LLM button) lives here now; see the <script> block below, which just
				// calls showSqlServerShowplanDialog() once loaded.
				HtmlUtils.createJsScriptTag("/scripts/dbxtune/js/dbxShowplan.js") +

				"</head> \n" +
				" \n" +
				"<body> \n" +
				HtmlStatic.getHtmlNavbar(PageSection.Tools, "<li class='nav-item'><a class='nav-link' href='/showplan/'>All Showplan Viewers</a></li>", true) +
				// Same paste form as a plain GET sits behind the dialog opened below - the dialog has
				// no "reopen"/navigate-away mechanism of its own (closing it just clears its own
				// containers), so without this, closing it would strand the user on a blank page.
				createPasteFormBodyHtml() +
				"    <script> \n" +
				"        const plan = '"             + StringEscapeUtils.escapeEcmaScript(payload) + "'; \n" +
				"        const ssShowplanSql = '"    + StringEscapeUtils.escapeEcmaScript(StringUtil.nullToValue(sql, ""))    + "'; \n" +
				"        const ssShowplanSrv = '"    + StringEscapeUtils.escapeEcmaScript(StringUtil.nullToValue(srv, ""))    + "'; \n" +
				"        const ssShowplanDbname = '" + StringEscapeUtils.escapeEcmaScript(StringUtil.nullToValue(dbname, "")) + "'; \n" +
				// Execution statistics from the Daily Summary Report - see the GET path above.
				"        const ssShowplanWorkload = '" + StringEscapeUtils.escapeEcmaScript(StringUtil.nullToValue(workloadData, "")) + "'; \n" +
				"        showSqlServerShowplanDialog(plan, ssShowplanSql, '', { srv: ssShowplanSrv, dbname: ssShowplanDbname, workloadData: ssShowplanWorkload }); \n" +
				"    </script> \n" +
				HtmlStatic.getJavaScriptAtEnd(true) +
				"</body> \n" +
				" \n" +
				"</html> \n" +
				"";
		return str;
	}
}
