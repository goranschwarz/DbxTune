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


public class ShowplanAseServlet
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
	 * from the URL FRAGMENT ({@code #plan=...&sql=...&isXml=...&srv=...&dbname=...}) - never sent to
	 * the server, so a large XML plan never risks a "414 URI Too Long" error the way a query string
	 * would - and opens it via {@code showAseShowplanDialog(...)} client-side. Used by the mail-safe
	 * "View Execution Plan" link the Daily Summary Report links here to (see {@code LlmAdviceServlet}
	 * for the identical pattern, and {@code ShowplanLinkBuilder} for the link itself).
	 * <p>
	 * If the fragment has no {@code plan} param (e.g. a bare {@code GET /showplan/ase}), the script
	 * simply no-ops and this looks exactly like {@link #createPasteFormOutput()}.
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
				"    <title>DbxTune - ASE Showplan</title> \n" +
				"    <meta http-equiv='Cache-Control' content='no-cache, no-store, must-revalidate' /> \n" +
				"    <meta http-equiv='Pragma' content='no-cache' /> \n" +
				"    <meta http-equiv='Expires' content='0' /> \n" +
				" \n" +
				HtmlStatic.getUserDefinedContentHead() +
				HtmlUtils.createCssLinkTag("/scripts/jquery/ui/1.14.1/themes/smoothness/jquery-ui.css") +
				HtmlUtils.createJsScriptTag("/scripts/jquery/ui/1.14.1/jquery-ui.min.js") +
				HtmlUtils.createJsScriptTag("/scripts/jquery/ui/1.14.1/jquery.ui.touch-punch.min.js") +
				// Shared layout plumbing - must load BEFORE dbxShowplanAse.js, which calls into it.
				HtmlUtils.createJsScriptTag("/scripts/dbxtune/js/dbxShowplanGraph.js") +
				HtmlUtils.createJsScriptTag("/scripts/dbxtune/js/dbxShowplanAse.js") +
				HtmlUtils.createJsScriptTag("/scripts/panzoom/4.5.1/panzoom.min.js") +
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
				"            var isXml = qs.get('isXml') === 'true'; \n" +
				"            var sql = qs.get('sql') || ''; \n" +
				"            var srv = qs.get('srv') || ''; \n" +
				"            var dbname = qs.get('dbname') || ''; \n" +
				// Execution statistics from the Daily Summary Report - this dialog has its own "Get LLM
				// Optimization Advice" section, which would otherwise lose the workload profile.
				"            var workloadData = qs.get('workloadData') || ''; \n" +
				"            // Also fill in the paste form behind the dialog - it has no 'reopen' mechanism of its \n" +
				"            // own (closing the dialog just clears its own containers), so without this, closing it \n" +
				"            // would leave the user staring at a form that looks like it never received their plan. \n" +
				"            document.getElementById('plan').value = plan; \n" +
				"            document.getElementById('sql').value = sql; \n" +
				"            document.getElementById('srv').value = srv; \n" +
				"            document.getElementById('dbname').value = dbname; \n" +
				"            aseShowplanPasteCheckSqlText(); \n" +
				"            showAseShowplanDialog(plan, sql, isXml, '', { srv: srv, dbname: dbname, workloadData: workloadData }); \n" +
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
	 * The paste-and-submit form shown for a plain {@code GET /showplan/ase} - shares the same
	 * navbar/head chrome as {@link #createShowplanOutput(String, String, String, String, String, String)} so landing
	 * on this page and viewing a submitted plan look like the same section, not two different pages.
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
				"    <title>DbxTune - ASE Showplan</title> \n" +
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
	 * {@link #createShowplanOutput(String, String, String, String, String, String)} can put the
	 * same real, working form behind the Showplan dialog it opens - the dialog itself has no
	 * "reopen"/navigate-away mechanism (closing it just clears its own containers), so without this
	 * closing the dialog would otherwise strand the user on a blank page.
	 */
	private static String createPasteFormBodyHtml()
	{
		return "" +
				"    <div class='container-fluid px-4 py-3' style='max-width: 900px;'> \n" +
				"      <h2>ASE Showplan Viewer</h2> \n" +
				"      <p class='text-muted'>Paste either a <code>show_cached_plan_in_xml</code> XML plan or a classic <code>sp_showplan</code> text plan below and press Submit (the format is auto-detected).</p> \n" +
				"      <div class='card shadow-sm'> \n" +
				"        <div class='card-body'> \n" +
				"          <form id='showplan-form' action='/showplan/ase' method='post'> \n" +
				// Both optional - filling them in (with an actively-monitored DbxTune server name)
				// unlocks the same live table size/rowcount tooltips, Table Information section and
				// DDL-enriched LLM advice the modal Showplan dialog gets; left blank, this page behaves
				// exactly as it always has - showAseShowplanDialog() itself no-ops the srv/dbname-gated
				// sections when meta.srv/meta.dbname are empty.
				"            <div class='form-row'> \n" +
				"              <div class='col-md-4 mb-2'> \n" +
				"                <label for='srv' class='mb-1' style='font-size:0.85em;color:#555;'>At Server Name (optional)</label> \n" +
				"                <input type='text' class='form-control form-control-sm' id='srv' name='srv' list='ase-showplan-srv-datalist' placeholder='e.g. PROD_ASE_01' autocomplete='off'> \n" +
				"                <datalist id='ase-showplan-srv-datalist'></datalist> \n" +
				"              </div> \n" +
				"              <div class='col-md-4 mb-2'> \n" +
				"                <label for='dbname' class='mb-1' style='font-size:0.85em;color:#555;'>Database Name (optional)</label> \n" +
				"                <input type='text' class='form-control form-control-sm' id='dbname' name='dbname' placeholder='e.g. mydb'> \n" +
				"              </div> \n" +
				"            </div> \n" +
				"            <p class='text-muted' style='font-size:0.8em;margin-top:-4px;'>Fill these in (with an actively-monitored DbxTune server) to also get live table size/row-count, an index breakdown, and DDL-enriched LLM advice for the tables this plan touches - leave blank to just view the diagram.</p> \n" +
				"            <div class='mb-2'> \n" +
				"              <button type='button' class='btn btn-outline-secondary btn-sm' onclick=\"document.getElementById('ase-showplan-file-input').click();\">&#128194; Load from File</button> \n" +
				"              <input type='file' id='ase-showplan-file-input' accept='.txt,.xml' style='display:none' onchange='aseShowplanPasteLoadFile(event)'> \n" +
				"              <span style='font-size:0.8em;color:#888;margin-left:6px;'>or drag &amp; drop a file onto the text area below</span> \n" +
				"            </div> \n" +
				"            <textarea class='form-control mb-3' id='plan' name='plan' rows='20' placeholder='Paste the ASE Showplan (XML or text) here...' oninput='aseShowplanPasteCheckSqlText()' ondragover='aseShowplanPasteDragOver(event)' ondragleave='aseShowplanPasteDragLeave(event)' ondrop='aseShowplanPasteDrop(event)'></textarea> \n" +
				// Optional - a plan captured by DbxTune itself already carries the SQL between
				// '---- BEGIN/END: SQL Statement Executed' markers (see extractEmbeddedSqlText()
				// server-side), in which case this stays collapsed. A plan pasted straight from
				// isql/sp_showplan output has no such markers, so aseShowplanPasteCheckSqlText() below
				// auto-opens it and flips the summary text to prompt for it.
				"            <details id='ase-showplan-sqltext-details' class='mb-3'> \n" +
				"              <summary id='ase-showplan-sqltext-summary' style='cursor:pointer;font-size:0.85em;color:#555;'>SQL Text (optional)</summary> \n" +
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
				"        function aseShowplanPasteCheckSqlText() { \n" +
				"            var planText = document.getElementById('plan').value; \n" +
				"            var sqlText  = document.getElementById('sql').value; \n" +
				"            var details  = document.getElementById('ase-showplan-sqltext-details'); \n" +
				"            var summary  = document.getElementById('ase-showplan-sqltext-summary'); \n" +
				"            var hasSql   = planText.indexOf('---- BEGIN: SQL Statement Executed') >= 0; \n" +
				"            if (hasSql) { \n" +
				"                summary.innerHTML = 'SQL Text (optional) &mdash; <span style=\"color:#0f5132;\">already found in the plan</span>'; \n" +
				"            } else if (planText.trim() === '') { \n" +
				"                summary.innerHTML = 'SQL Text (optional)'; \n" +
				"            } else { \n" +
				"                summary.innerHTML = 'SQL Text (optional) &mdash; <span style=\"color:#b45309;\">no SQL text found in the plan, consider pasting it below</span>'; \n" +
				"                if (!sqlText) details.open = true; \n" + // don't yank open/shut on top of something the user's already editing
				"            } \n" +
				"        } \n" +
				"        function aseShowplanPasteReadFile(file) { \n" +
				"            if (!file) return; \n" +
				"            var reader = new FileReader(); \n" +
				"            reader.onload = function(e) { document.getElementById('plan').value = e.target.result; aseShowplanPasteCheckSqlText(); }; \n" +
				"            reader.readAsText(file); \n" +
				"        } \n" +
				"        function aseShowplanPasteLoadFile(event) { \n" +
				"            aseShowplanPasteReadFile(event.target.files[0]); \n" +
				"            event.target.value = ''; \n" + // reset so re-selecting the same filename re-fires onchange
				"        } \n" +
				"        function aseShowplanPasteDragOver(event) { \n" +
				"            event.preventDefault(); \n" +
				"            event.currentTarget.style.borderColor = '#4a90d9'; \n" +
				"        } \n" +
				"        function aseShowplanPasteDragLeave(event) { \n" +
				"            event.currentTarget.style.borderColor = ''; \n" +
				"        } \n" +
				"        function aseShowplanPasteDrop(event) { \n" +
				"            event.preventDefault(); \n" +
				"            event.currentTarget.style.borderColor = ''; \n" +
				"            aseShowplanPasteReadFile(event.dataTransfer.files && event.dataTransfer.files[0]); \n" +
				"        } \n" +
				// Best-effort autocomplete - a typo'd/offline server name just surfaces the same
				// srv-not-found/collector-offline error the modal dialog's Table Information already
				// handles gracefully, so this list doesn't need to be authoritative.
				"        fetch('/api/sessions').then(function(r) { return r.json(); }).then(function(list) { \n" +
				"            var dl = document.getElementById('ase-showplan-srv-datalist'); \n" +
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
		String isXml    = request.getParameter("isXml"); // "true"/"false", or unset -> auto-detect client-side
		String srv      = request.getParameter("srv");    // optional - enables live table-info lookups
		String dbname   = request.getParameter("dbname"); // optional - required alongside srv above
		// Execution statistics harvested from the Daily Summary Report - the plan viewer has its own
		// "Get LLM Optimization Advice" section, which would otherwise lose the workload profile.
		String workloadData = request.getParameter("workloadData");

		if (_logger.isDebugEnabled())
			_logger.debug("/showplan/ase: received request from: remoteHost='" + remoteHost + "', remoteAddr='" + remoteAddr + "', remotePort='" + remotePort + "', remoteUser='" + remoteUser + "'.");

		String payload = plan;
		if (StringUtil.isNullOrBlank(payload))
		{
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Expecting an ASE Showplan (either 'show_cached_plan_in_xml' XML or classic 'sp_showplan' text) as Payload, but an empty string was sent.");
			return;
		}

		String formattedOutput = createShowplanOutput(payload, sql, dbVendor, isXml, srv, dbname, workloadData);

		response.setContentType("text/html; charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
		PrintWriter out = response.getWriter();
		out.print(formattedOutput);
		out.flush();
		out.close();
	}


	public static String createShowplanOutput(String payload)
	{
		return createShowplanOutput(payload, null, null, null, null, null);
	}

	/**
	 * @param payload  the ASE plan - either XML (show_cached_plan_in_xml) or classic sp_showplan text
	 * @param sql      the SQL statement this plan belongs to, or null if unknown
	 * @param dbVendor DBMS product name for {@code sql}, or null (unused - the dialog always treats
	 *                 this page's plans as ASE, same as {@code showAseShowplanDialog()}'s own callers do)
	 * @param isXml    "true"/"false" if the caller already knows the payload's format, or null to
	 *                 have the client sniff for an XML/{@code <query>} prefix
	 * @param srv      name of an actively-monitored DbxTune server this plan came from, or null -
	 *                 when present together with {@code dbname}, unlocks the same live table-info
	 *                 tooltips/warnings, Table Information section, and DDL-enriched LLM advice the
	 *                 modal Showplan dialog gets - passed straight through as its {@code meta} param
	 * @param dbname   database name on {@code srv} the tables in {@code sql} live in, or null
	 */
	public static String createShowplanOutput(String payload, String sql, String dbVendor, String isXml, String srv, String dbname)
	{
		return createShowplanOutput(payload, sql, dbVendor, isXml, srv, dbname, null);
	}

	/** @param workloadData raw execution statistics JSON, forwarded to the dialog's LLM advice section (may be null) */
	public static String createShowplanOutput(String payload, String sql, String dbVendor, String isXml, String srv, String dbname, String workloadData)
	{
		// A leading blank line/whitespace before "<?xml ...?>" (common in pasted/captured plans) is
		// otherwise a fatal error to any XML parser - the spec only allows the declaration as the
		// very first thing in the document - so strip it here, once, for every caller/format instead
		// of relying on each downstream consumer (e.g. AseShowplan.parseXml() in dbxShowplanAse.js)
		// to defend against it individually.
		if (payload != null)
			payload = payload.trim();

		// A caller may not pass a separate 'sql' param at all - a classic sp_showplan text capture
		// often already carries the statement itself, wrapped in "---- BEGIN: SQL Statement
		// Executed ----" / "---- END: SQL Statement Executed ----" markers right before the
		// "QUERY PLAN FOR STATEMENT" tree (same markers dbxSqlText.js's format-on-demand logic
		// already knows about, in a different context). Pull it out here instead, once, so the
		// dialog's SQL Text section can use it exactly like an explicitly-passed 'sql' param would.
		if (StringUtil.isNullOrBlank(sql))
			sql = extractEmbeddedSqlText(payload);
		sql = stripHtmlWrapper(sql);

		// Both required together - a table-info lookup needs to know both which server and which
		// database to ask. Gates dbxSqlTableNames.js/dbxTableInfo.css (only needed once the dialog's
		// Table Information section can actually do anything) and is passed into the dialog's own
		// meta={srv,dbname} param below, which is what actually turns on every srv/dbname-gated
		// feature (per-operator table-info tooltips, Table Information section, DDL-enriched LLM
		// Advice) - all of that now lives entirely inside dbxShowplan.js/dbxShowplanAse.js.
		boolean hasSrvDbname = StringUtil.hasValue(srv) && StringUtil.hasValue(dbname);

		String str = "" +
				"<!DOCTYPE html> \n" +
				"<html lang='en'> \n" +
				" \n" +
				"<head> \n" +
				"    <meta charset='UTF-8'> \n" +
				"    <meta name='viewport' content='width=device-width, initial-scale=1'> \n" +
				"    <title>ASE Execution Plan Viewer</title> \n" +
                "     \n" +
				"    <meta http-equiv='Cache-Control' content='no-cache, no-store, must-revalidate' /> \n" +
				"    <meta http-equiv='Pragma' content='no-cache' /> \n" +
				"    <meta http-equiv='Expires' content='0' /> \n" +
				"     \n" +

				// Pulls in jQuery, Bootstrap 4.6.2 (CSS+JS), Font Awesome, dbxcentral.css,
				// dbxcentral.utils.js and dbxLoginModal.js - the same shared head/navbar/login-wiring
				// every other DbxCentral page uses (see LlmAdviceServlet for the identical pattern),
				// rather than this page rolling its own Bootstrap include and a one-off <nav>.
				HtmlStatic.getUserDefinedContentHead() +

				// jQuery UI (draggable/resizable + touch-punch) - same includes graph.html uses for
				// the dialog's drag/resize chrome. Optional per dbxShowplan.js's own
				// $.fn.draggable/$.fn.resizable guards (the dialog still opens/functions without it,
				// just isn't draggable/resizable), included here for full parity with graph.html.
				HtmlUtils.createCssLinkTag("/scripts/jquery/ui/1.14.1/themes/smoothness/jquery-ui.css") +
				HtmlUtils.createJsScriptTag("/scripts/jquery/ui/1.14.1/jquery-ui.min.js") +
				HtmlUtils.createJsScriptTag("/scripts/jquery/ui/1.14.1/jquery.ui.touch-punch.min.js") +

				// dbxShowplanAse.js is a first-party file (no CDN mirror) and injects its own <style>
				// at runtime, so no separate CSS link is needed here. dbxShowplanGraph.js holds the
				// layout plumbing it shares with the SQL Server renderer, so it must come first.
				HtmlUtils.createJsScriptTag("/scripts/dbxtune/js/dbxShowplanGraph.js") +
				HtmlUtils.createJsScriptTag("/scripts/dbxtune/js/dbxShowplanAse.js") +
				HtmlUtils.createJsScriptTag("/scripts/panzoom/4.5.1/panzoom.min.js") +

				// dbxSqlTableNames.js is needed for two independent things: the Table Information
				// section (only useful once hasSrvDbname) AND the Graphical Plan's Reformatting finding
				// index suggestion, which only needs SQL text - no server context at all. Gating this
				// whole script on hasSrvDbname was wrong: pasting a plan with SQL text but no
				// srv/dbname (the paste form has no separate SQL Text field either way) silently
				// disabled the suggestion, which had nothing to do with Table Information. Always load
				// the JS module; only the Table Information section's own CSS still needs hasSrvDbname.
				HtmlUtils.createJsScriptTag("/scripts/dbxtune/js/dbxSqlTableNames.js") +
				(hasSrvDbname
					? HtmlUtils.createCssLinkTag("/scripts/dbxtune/css/dbxTableInfo.css")
					: "") +

				// Whether the dialog's LLM Advice section actually calls a provider (vs. falling back
				// to the "no exec" Prompt Preview section) is decided live, client-side, by the dialog
				// itself (dbxLlmAdvice.isEnabled()) - so these are always loaded once we're rendering an
				// actual plan, same as dbxShowplan.js's own unconditional use on graph.html; this page no
				// longer needs to know or care about the LlmClientRegistry feature flag server-side.
				// Gating this on hasSqlText was wrong: the paste form has no SQL Text field, and even
				// extractEmbeddedSqlText()'s best-effort scrape of the pasted plan text can come back
				// empty for plans with no embedded SQL - dbxLlmAdvice.js should still be present so the
				// dialog can show its "No SQL text is available for this plan." message instead of
				// looking like the feature itself failed to load.
				"    <script src='/scripts/marked/18.0.5/marked.min.js'></script> \n"
				+ "    <script src='/scripts/dompurify/3.4.11/purify.min.js'></script> \n"
				+ "    <script src='/scripts/dbxtune/js/dbxLlmAdvice.js'></script> \n"
				+ HtmlUtils.createCssLinkTag("/scripts/prism/prism-1.30.0.css")
				+ HtmlUtils.createJsScriptTag("/scripts/prism/prism-1.30.0.js")
				+ HtmlUtils.createJsScriptTag("/scripts/sql-formatter/12.1.3/sql-formatter.min.js") +

				// The real dialog - everything this page used to hand-build (toolbar, zoom/pan, Table
				// Information, LLM Advice/Preview, and the tooltip-clipping fight) lives here now; see
				// the <script> block below, which just calls showAseShowplanDialog() once loaded.
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
				"        const plan = '" + StringEscapeUtils.escapeEcmaScript(payload) + "'; \n" +
				"        const explicitIsXml = " + (StringUtil.hasValue(isXml) ? ("'" + StringEscapeUtils.escapeEcmaScript(isXml) + "'") : "null") + "; \n" +
				"        const aseShowplanSql = '"    + StringEscapeUtils.escapeEcmaScript(StringUtil.nullToValue(sql, ""))    + "'; \n" +
				"        const aseShowplanSrv = '"    + StringEscapeUtils.escapeEcmaScript(StringUtil.nullToValue(srv, ""))    + "'; \n" +
				"        const aseShowplanDbname = '" + StringEscapeUtils.escapeEcmaScript(StringUtil.nullToValue(dbname, "")) + "'; \n" +
				// Execution statistics from the Daily Summary Report - this dialog's own "Get LLM
				// Optimization Advice" section would otherwise lose the workload profile.
				"        const aseShowplanWorkload = '" + StringEscapeUtils.escapeEcmaScript(StringUtil.nullToValue(workloadData, "")) + "'; \n" +
				"        var isXml; \n" +
				"        if (explicitIsXml === 'true') isXml = true; \n" +
				"        else if (explicitIsXml === 'false') isXml = false; \n" +
				"        else isXml = /^\\s*<\\?xml|^\\s*<query>/i.test(plan.replace(/^[\\s\\S]*?<pre>/i, '')); \n" +
				"        showAseShowplanDialog(plan, aseShowplanSql, isXml, '', { srv: aseShowplanSrv, dbname: aseShowplanDbname, workloadData: aseShowplanWorkload }); \n" +
				"    </script> \n" +
				HtmlStatic.getJavaScriptAtEnd(true) +
				"</body> \n" +
				" \n" +
				"</html> \n" +
				"";
		return str;
	}

	/**
	 * Pulls the statement text out from between the "---- BEGIN: SQL Statement Executed ----" /
	 * "---- END: SQL Statement Executed ----" markers a classic sp_showplan text capture often
	 * carries right before its "QUERY PLAN FOR STATEMENT" tree - or null if the markers aren't
	 * present (e.g. an XML plan, or a text plan captured without them).
	 */
	private static String extractEmbeddedSqlText(String planText)
	{
		if (StringUtil.isNullOrBlank(planText))
			return null;

		String beginMarker = "---- BEGIN: SQL Statement Executed";
		String endMarker   = "---- END: SQL Statement Executed";

		int beginIdx = planText.indexOf(beginMarker);
		if (beginIdx < 0)
			return null;

		int contentStart = planText.indexOf('\n', beginIdx);
		if (contentStart < 0)
			return null;
		contentStart++; // skip past the newline itself

		int endIdx = planText.indexOf(endMarker, contentStart);
		if (endIdx < 0)
			return null;

		String sql = planText.substring(contentStart, endIdx).trim();
		return StringUtil.hasValue(sql) ? sql : null;
	}

	/**
	 * AseConnectionUtils.getShowplan() is called with addHtmlTags=true, which wraps its captured
	 * text in "&lt;html&gt;Showplan:&lt;pre&gt;...&lt;/pre&gt;&lt;/html&gt;" - that capture can end
	 * up feeding a "sql" field too (not just the plan itself), same root cause already worked around
	 * for the modal dialog's SQL Text field (_aseStripHtmlWrapper() in dbxShowplan.js). Applied here
	 * unconditionally (a no-op if the wrapper isn't present) rather than only for one of the two
	 * possible sources of {@code sql} (explicit param vs. extracted from the plan), since both are
	 * exposed to the same contamination.
	 */
	private static String stripHtmlWrapper(String text)
	{
		if (StringUtil.isNullOrBlank(text))
			return text;
		return text.replaceFirst("(?is)^.*?<pre>", "").replaceFirst("(?is)</pre>.*$", "");
	}
}
