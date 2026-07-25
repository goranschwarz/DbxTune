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
import com.dbxtune.central.llm.LlmClientRegistry;
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
		out.print(createPasteFormOutput());
		out.flush();
		out.close();
	}

	/**
	 * The paste-and-submit form shown for a plain {@code GET /showplan/ase} - shares the same
	 * navbar/head chrome as {@link #createShowplanOutput(String, String, String, String)} so landing
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
				HtmlStatic.getHtmlNavbar(PageSection.None, "<li class='nav-item'><a class='nav-link' href='/showplan/'>All Showplan Viewers</a></li>", true) +
				"    <div class='container-fluid px-4 py-3' style='max-width: 900px;'> \n" +
				"      <h2>ASE Showplan Viewer</h2> \n" +
				"      <p class='text-muted'>Paste either a <code>show_cached_plan_in_xml</code> XML plan or a classic <code>sp_showplan</code> text plan below and press Submit (the format is auto-detected).</p> \n" +
				"      <div class='card shadow-sm'> \n" +
				"        <div class='card-body'> \n" +
				"          <form id='showplan-form' action='/showplan/ase' method='post'> \n" +
				"            <textarea class='form-control mb-3' id='plan' name='plan' rows='20' placeholder='Paste the ASE Showplan (XML or text) here...'></textarea> \n" +
				"            <button type='submit' class='btn btn-primary'>Submit</button> \n" +
				"          </form> \n" +
				"        </div> \n" +
				"      </div> \n" +
				"    </div> \n" +
				" \n" +
				HtmlStatic.getJavaScriptAtEnd(true) +
				"</body> \n" +
				" \n" +
				"</html> \n" +
				"";
		return str;
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

		if (_logger.isDebugEnabled())
			_logger.debug("/showplan/ase: received request from: remoteHost='" + remoteHost + "', remoteAddr='" + remoteAddr + "', remotePort='" + remotePort + "', remoteUser='" + remoteUser + "'.");

		String payload = plan;
		if (StringUtil.isNullOrBlank(payload))
		{
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Expecting an ASE Showplan (either 'show_cached_plan_in_xml' XML or classic 'sp_showplan' text) as Payload, but an empty string was sent.");
			return;
		}

		String formattedOutput = createShowplanOutput(payload, sql, dbVendor, isXml);

		response.setContentType("text/html; charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
		PrintWriter out = response.getWriter();
		out.print(formattedOutput);
		out.flush();
		out.close();
	}


	public static String createShowplanOutput(String payload)
	{
		return createShowplanOutput(payload, null, null, null);
	}

	/**
	 * @param payload  the ASE plan - either XML (show_cached_plan_in_xml) or classic sp_showplan text
	 * @param sql      the SQL statement this plan belongs to, or null if unknown - when present,
	 *                 a "Get LLM Optimization Advice" button is shown
	 * @param dbVendor DBMS product name for {@code sql}, or null
	 * @param isXml    "true"/"false" if the caller already knows the payload's format, or null to
	 *                 have the client sniff for an XML/{@code <query>} prefix
	 */
	public static String createShowplanOutput(String payload, String sql, String dbVendor, String isXml)
	{
		// A caller may not pass a separate 'sql' param at all - a classic sp_showplan text capture
		// often already carries the statement itself, wrapped in "---- BEGIN: SQL Statement
		// Executed ----" / "---- END: SQL Statement Executed ----" markers right before the
		// "QUERY PLAN FOR STATEMENT" tree (same markers dbxSqlText.js's format-on-demand logic
		// already knows about, in a different context). Pull it out here instead, once, so both the
		// LLM Advice button and a plain "SQL Text" display section (added below) can use it exactly
		// like an explicitly-passed 'sql' param would - the two are the same concept, just from
		// different call sites.
		if (StringUtil.isNullOrBlank(sql))
			sql = extractEmbeddedSqlText(payload);
		sql = stripHtmlWrapper(sql);

		// Independent of the LLM feature flag - a plain SQL Text display has nothing to do with
		// whether LLM advice is enabled, unlike 'hasSql' below (kept as-is; only gates the LLM
		// button + its JS constants, both genuinely LLM-specific).
		boolean hasSqlText = StringUtil.hasValue(sql);
		boolean hasSql = hasSqlText && LlmClientRegistry.isFeatureEnabled();

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

				// dbxShowplanAse.js is a first-party file (no CDN mirror) and injects its own <style>
				// at runtime, so no separate CSS link is needed here.
				HtmlUtils.createJsScriptTag("/scripts/dbxtune/js/dbxShowplanAse.js") +
				// Same vendored Panzoom the modal dialog (dbxShowplan.js) uses - graph.html:1109.
				HtmlUtils.createJsScriptTag("/scripts/panzoom/4.5.1/panzoom.min.js") +

				(hasSql
					? "    <script src='/scripts/marked/18.0.5/marked.min.js'></script> \n"
					+ "    <script src='/scripts/dompurify/3.4.11/purify.min.js'></script> \n"
					+ "    <script src='/scripts/dbxtune/js/dbxLlmAdvice.js'></script> \n"
					: "") +

				// Only needed for the "SQL Text" section below, which (unlike the LLM Advice button)
				// doesn't depend on the LLM feature flag - just on whether we actually have any SQL
				// text to show.
				(hasSqlText
					? HtmlUtils.createCssLinkTag("/scripts/prism/prism-1.30.0.css")
					+ HtmlUtils.createJsScriptTag("/scripts/prism/prism-1.30.0.js")
					+ HtmlUtils.createJsScriptTag("/scripts/sql-formatter/12.1.3/sql-formatter.min.js")
					: "") +

				// #ase-showplan-viewport (not #ase-showplan itself) is the scrollable element - see
				// the matching comment by the button markup below for why the split matters.
				"    <style> #ase-showplan-viewport { overflow: auto; } </style> \n" +

				"</head> \n" +
				" \n" +
				"<body> \n" +
				HtmlStatic.getHtmlNavbar(PageSection.None, "<li class='nav-item'><a class='nav-link' href='/showplan/'>All Showplan Viewers</a></li>", true) +
				"    <div class='container-fluid px-4 py-3'> \n" +
				"      <h2>&#128202; ASE Execution Plan Viewer</h2> \n" +
				"      <div class='card shadow-sm'> \n" +
				"        <div class='card-body'> \n" +
				"          <div class='mb-3'> \n" +
				(hasSql
					? "            <button type='button' class='btn btn-primary mr-2' onclick='dbxLlmAdvice.open({sql: dbxLlmSql, plan: dbxLlmPlan, dbVendor: dbxLlmDbVendor});'>&#129302; Get LLM Optimization Advice</button> \n"
					: "") +
				"            <button type='button' id='aseShowplanStandaloneOrientationBtn' class='btn btn-outline-secondary btn-sm mr-2' onclick='aseShowplanStandaloneToggleOrientation();'>&#8646; Top-to-Bottom</button> \n" +
				"            <button type='button' id='aseShowplanStandaloneZoomBtn' class='btn btn-outline-secondary btn-sm' onclick='aseShowplanStandaloneToggleZoom();'>&#128269; Enable Zoom</button> \n" +
				"            <span style='font-size:0.8em;color:#888;margin-left:6px;'>Execution order is by VA# (starting at 0)</span> \n" +
				"          </div> \n" +
				// #ase-showplan (the Panzoom target) must NOT be the scrollable element itself - once
				// Panzoom is enabled it hijacks wheel events (to zoom instead of scroll) and moves
				// content via a CSS transform, which is a completely separate coordinate system from
				// a native scrollLeft/scrollTop. Putting overflow:auto directly on the Panzoom target
				// (the original bug here) made the two systems fight: the wheel-driven native scroll
				// got hijacked for zoom, so drag-panning was the only way left to reach content past
				// the edge, but the plain (non-drag) scrollbar was still visually there too - reported
				// as "some parts are cut off" when zoomed. The modal dialog (dbxShowplan.js) already
				// gets this right by keeping them separate (.modal-body scrolls, #dbx-view-
				// aseShowplan-graphContent is the Panzoom target with no overflow of its own) - this
				// mirrors that: #ase-showplan-viewport is the plain scrollable ancestor (used before
				// zoom is enabled, or for the small vertical scrollbar zoom doesn't need), #ase-
				// showplan is the actual Panzoom target and has no scroll behavior of its own. \n" +
				"          <div id='ase-showplan-viewport'> \n" +
				"            <div id='ase-showplan'>                          \n" +
				"            </div>                                           \n" +
				"          </div> \n" +
				(hasSqlText
					? "          <details open id='ase-showplan-sect-sql' style='border:1px solid #d0d0d0;border-radius:3px;background:#fafafa;margin-top:8px;'> \n"
					+ "            <summary style='cursor:pointer;padding:5px 10px;font-size:0.85em;font-weight:600;list-style:none;user-select:none;'>&#128196; SQL Text</summary> \n"
					+ "            <div style='padding:4px 8px 8px 8px;'> \n"
					+ "              <button type='button' class='btn btn-outline-secondary btn-sm' style='margin-bottom:4px;' onclick='aseShowplanStandaloneFormatSql();'>Format SQL</button> \n"
					+ "              <button type='button' class='btn btn-outline-secondary btn-sm' style='margin-bottom:4px;' onclick='aseShowplanStandaloneCopySql();'>Copy SQL</button> \n"
					+ "              <pre class='mb-0'><code id='ase-showplan-sqlContent' class='language-sql line-numbers'></code></pre> \n"
					+ "            </div> \n"
					+ "          </details> \n"
					: "") +
				"        </div> \n" +
				"      </div> \n" +
				"    </div> \n" +
				"                                                       \n" +
				"    <script>                                           \n" +
				"        const plan = '" + StringEscapeUtils.escapeEcmaScript(payload) + "'; \n" +
				"        const explicitIsXml = " + (StringUtil.hasValue(isXml) ? ("'" + StringEscapeUtils.escapeEcmaScript(isXml) + "'") : "null") + "; \n" +
				(hasSqlText
					? "        const aseShowplanStandaloneSql = '" + StringEscapeUtils.escapeEcmaScript(sql) + "'; \n"
					: "") +
				(hasSql
					// Same string as aseShowplanStandaloneSql above (hasSql implies hasSqlText) - reused
					// rather than escaping/embedding the SQL text into the page a second time.
					? "        const dbxLlmSql = aseShowplanStandaloneSql; \n"
					+ "        const dbxLlmPlan = '"     + StringEscapeUtils.escapeEcmaScript(payload) + "'; \n"
					+ "        const dbxLlmDbVendor = '" + StringEscapeUtils.escapeEcmaScript(StringUtil.nullToValue(dbVendor, "")) + "'; \n"
					: "") +
				"                                                       \n" +
				"        var aseShowplanStandaloneHorizontal = true; \n" +
				"        var aseShowplanStandaloneZoom = undefined; \n" +
				"        function aseShowplanStandaloneRender() { \n" +
				// Reset any Panzoom transform from a previous plan/layout before re-rendering - see
				// the matching comment in dbxShowplan.js's _aseShowplanRenderGraphicalPlan().
				"            if (aseShowplanStandaloneZoom !== undefined) { try { aseShowplanStandaloneZoom.reset(); } catch (ex) {} } \n" +
				"            var isXml; \n" +
				"            if (explicitIsXml === 'true') isXml = true; \n" +
				"            else if (explicitIsXml === 'false') isXml = false; \n" +
				"            else isXml = /^\\s*<\\?xml|^\\s*<query>/i.test(plan.replace(/^[\\s\\S]*?<pre>/i, '')); \n" +
				"            var parsed = null; \n" +
				"            try { parsed = isXml ? AseShowplan.parseXml(plan) : AseShowplan.parseText(plan); } catch (ex) { parsed = null; } \n" +
				"            var el = document.getElementById('ase-showplan'); \n" +
				"            if (parsed) { \n" +
				"                try { AseShowplan.render(el, parsed, { horizontal: aseShowplanStandaloneHorizontal, connectorStyle: 'lines', layout: 'compact' }); return; } catch (ex) { parsed = null; } \n" +
				"            } \n" +
				"            el.innerHTML = ''; \n" +
				"            var pre = document.createElement('pre'); \n" +
				"            pre.className = 'mb-0'; \n" +
				"            pre.style.whiteSpace = 'pre-wrap'; \n" +
				"            pre.textContent = 'Could not parse this plan into a diagram - showing raw text.\\n\\n' + plan; \n" +
				"            el.appendChild(pre); \n" +
				"        } \n" +
				"        function aseShowplanStandaloneToggleOrientation() { \n" +
				"            aseShowplanStandaloneHorizontal = !aseShowplanStandaloneHorizontal; \n" +
				"            document.getElementById('aseShowplanStandaloneOrientationBtn').innerHTML = aseShowplanStandaloneHorizontal ? '&#8646; Top-to-Bottom' : '&#8646; Left-to-Right'; \n" +
				"            aseShowplanStandaloneRender(); \n" +
				"        } \n" +
				// See the matching comment in dbxShowplan.js's aseShowplanToggleZoom() - a separate
				// Reset Zoom button used to sit here, but with zoom left enabled a mouse-wheel scroll
				// always zooms instead of scrolling the page. Merged into one Enable/Disable toggle:
				// disabling removes the wheel listener entirely (Panzoom's disableZoom option isn't
				// enough by itself - zoomWithWheel() calls event.preventDefault() before checking it,
				// so the scroll would still be eaten with nothing happening in its place) so the wheel
				// goes back to normal scrolling until zoom is explicitly re-enabled.
				"        function aseShowplanStandaloneToggleZoom() { \n" +
				"            var elem = document.getElementById('ase-showplan'); \n" +
				"            var btn  = document.getElementById('aseShowplanStandaloneZoomBtn'); \n" +
				"            if (aseShowplanStandaloneZoom === undefined) { \n" +
				"                if (!elem) return; \n" +
				// See the matching comment in dbxShowplan.js's aseShowplanToggleZoom() - lower step =
				// gentler trackpad zoom.
				"                aseShowplanStandaloneZoom = Panzoom(elem, { maxScale: 1, minScale: 0.01, step: 0.05 }); \n" +
				"                elem.addEventListener('wheel', aseShowplanStandaloneZoom.zoomWithWheel); \n" +
				"                if (btn) btn.innerHTML = '&#128269; Disable Zoom'; \n" +
				"            } else { \n" +
				"                try { aseShowplanStandaloneZoom.reset(); } catch (ex) {} \n" +
				"                if (elem) elem.removeEventListener('wheel', aseShowplanStandaloneZoom.zoomWithWheel); \n" +
				"                try { aseShowplanStandaloneZoom.destroy(); } catch (ex) {} \n" +
				"                aseShowplanStandaloneZoom = undefined; \n" +
				"                if (btn) btn.innerHTML = '&#128269; Enable Zoom'; \n" +
				"            } \n" +
				"        } \n" +
				(hasSqlText
					// Mirrors aseShowplanFormatSql()/aseShowplanCopySql() in dbxShowplan.js's modal
					// dialog - reimplemented standalone here rather than loading that whole file, since
					// this page already has its own self-contained JS for everything else.
					? "        function aseShowplanStandaloneFormatSql() { \n"
					// paramTypes.positional tells sql-formatter that a bare "?" is a valid positional
					// parameter placeholder (JDBC-style) rather than a syntax error - without it, any
					// captured SQL text containing "?" throws a parse error here instead of formatting.
					+ "            var formatOptions = { language: 'tsql', tabWidth: 4, keywordCase: 'upper', tabulateAlias: true, paramTypes: { positional: true } }; \n"
					+ "            var el = document.getElementById('ase-showplan-sqlContent'); \n"
					+ "            if (!el) return; \n"
					// ASE captures a dynamic SQL cursor's statement wrapped as "DYNAMIC_SQL <name>:
					// create proc <name> (...) as <actual query>" - stripped here before formatting, same
					// as the matching _aseStripDynamicSqlWrapper() in dbxShowplan.js's modal dialog.
					+ "            var sqlText = el.textContent.replace(/^\\s*DYNAMIC_SQL\\s+\\S+\\s*:\\s*create\\s+proc(?:edure)?\\s+\\S+\\s*(?:\\((?:[^()]|\\([^()]*\\))*\\))?\\s*as\\s*/i, ''); \n"
					+ "            try { el.textContent = sqlFormatter.format(sqlText, formatOptions); Prism.highlightElement(el); } catch (ex) { alert(ex); } \n"
					+ "        } \n"
					+ "        function aseShowplanStandaloneCopySql() { \n"
					+ "            var el = document.getElementById('ase-showplan-sqlContent'); \n"
					+ "            if (!el) return; \n"
					+ "            var ta = document.createElement('textarea'); ta.value = el.textContent; document.body.appendChild(ta); ta.select(); \n"
					+ "            try { document.execCommand('copy'); } catch (ex) { alert('Unable to copy\\n\\n' + ex); } \n"
					+ "            document.body.removeChild(ta); \n"
					+ "        } \n"
					+ "        document.getElementById('ase-showplan-sqlContent').textContent = aseShowplanStandaloneSql; \n"
					+ "        Prism.highlightAll(); \n"
					: "") +
				"        aseShowplanStandaloneRender(); \n" +
				"    </script>		                                    \n" +
				HtmlStatic.getJavaScriptAtEnd(true) +
				"</body>                                                \n" +
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
