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
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.central.controllers.HtmlStatic.PageSection;
import com.dbxtune.central.llm.LlmClientRegistry;
import com.dbxtune.utils.StringUtil;

/**
 * <pre>GET /llm-advice?sql=...&amp;ddlContext=...&amp;plan=...&amp;dbVendor=...&amp;jdbcUrl=...&amp;jdbcUser=...&amp;srv=...&amp;ts=...&amp;dbname=...&amp;provider=...</pre>
 * <p>
 * Standalone "Get LLM Optimization Advice" page, with the same navbar/login chrome as the rest of
 * DbxCentral (used by the mail-safe link the Daily Summary Report links here to, since generated
 * report HTML can end up being e-mailed and mail clients don't run inline JavaScript).
 * <p>
 * Also renders a collapsed "LLM Prompt Preview" section (lazy-loaded on first expand) - the exact
 * prompt DbxCentral would send, built via {@code dbxLlmAdvice.open({preview:true, ...})} without
 * actually calling a provider, so the user can paste it into a different LLM chat and compare
 * answers. That section works (and is shown) even when {@code DbxCentral.llm.enabled} is off, since
 * building the prompt touches no provider config/credentials - only the "Get LLM Optimization
 * Advice" section itself is skipped in that case.
 * <p>
 * All URL parameters are read client-side by dbxLlmAdvice.js - this servlet only renders the page
 * shell (standard head/navbar via {@link HtmlStatic}, plus the libraries dbxLlmAdvice.js optionally
 * uses: sql-formatter, Prism, marked, DOMPurify).
 */
public class LlmAdviceServlet
extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	/** Parameters this page understands, whether they arrive in the URL fragment (GET) or as POST fields. */
	private static final String[] PARAM_NAMES = { "sql", "ddlContext", "plan", "dbVendor", "jdbcUrl",
			"jdbcUser", "srv", "ts", "dbname", "workloadData", "provider" };

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
	throws ServletException, IOException
	{
		// No POSTed values -> the page reads everything from the URL fragment, as it always has
		writePage(resp, null);
	}

	/**
	 * Same page as {@link #doGet}, but fed from POST fields instead of the URL fragment.
	 * <p>
	 * This exists because the execution plan travels with the request, and a plan is BIG - measured on
	 * real Daily Summary Reports, the fragment-based URL reached ~845KB, and Chrome silently refuses to
	 * navigate that far (the new tab just sits at {@code about:blank#blocked}). The report's
	 * {@code dsrOpenLink()} therefore submits a form instead of calling {@code window.open()}; a POST
	 * body has no such limit. See {@code ShowplanLinkBuilder.getDsrLinkSupportJs()}.
	 */
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
	throws ServletException, IOException
	{
		req.setCharacterEncoding("UTF-8");

		Map<String, String> posted = new LinkedHashMap<>();
		for (String name : PARAM_NAMES)
		{
			String val = req.getParameter(name);
			if (StringUtil.hasValue(val))
				posted.put(name, val);
		}

		String json;
		try
		{
			json = Helper.createObjectMapper().writeValueAsString(posted);
		}
		catch (Exception ex)
		{
			_logger.warn("LlmAdviceServlet.doPost(): could not serialize the posted parameters, falling back to the URL fragment. Caught: " + ex, ex);
			json = null;
		}

		writePage(resp, json);
	}

	/**
	 * @param postedJson  JSON object with the parameters, when they came in via POST; null when the page
	 *                    should read them from the URL fragment instead (the GET/e-mail-safe path).
	 */
	private void writePage(HttpServletResponse resp, String postedJson)
	throws IOException
	{
		resp.setContentType("text/html;charset=UTF-8");
		resp.setCharacterEncoding("UTF-8");
		PrintWriter out = resp.getWriter();

		// The "LLM Prompt Preview" section below (dbxLlmAdvice.open({preview:true, ...})) never calls a
		// provider - it only builds and shows the prompt text via /api/llm/optimize-sql's preview mode,
		// so it's useful (and shown) even when the feature itself is off; only the "Get LLM Optimization
		// Advice" section actually needs it enabled. Same split dbxShowplan.js's modal already uses.
		boolean enabled = LlmClientRegistry.isFeatureEnabled();

		out.println("<!DOCTYPE html>");
		out.println("<html lang='en-US'>");
		out.println("<head>");
		out.println("  <meta charset='UTF-8'>");
		out.println("  <title>DbxTune - LLM Optimization Advice</title>");
		out.println("  <meta http-equiv='Cache-Control' content='no-cache, no-store, must-revalidate' />");
		out.println(HtmlStatic.getUserDefinedContentHead());
		out.println("  <link rel='stylesheet' href='/scripts/prism/prism-1.30.0.css'>");
		out.println("  <style> .dbx-llm-advice-container { max-width: 900px; margin: 20px auto; } </style>");
		out.println("</head>");
		out.println();
		out.println("<body>");
		out.println(HtmlStatic.getHtmlNavbar(PageSection.None, "", true));
		out.println("<div class='container-fluid dbx-llm-advice-container'>");
		out.println("  <h2>LLM Optimization Advice</h2>");
		// Shown first, right under the heading - not after the Advice result/error below it - so it's
		// immediately visible without scrolling, especially when the Advice call itself fails (e.g. not
		// logged in): the user can jump straight to the prompt and paste it into a different LLM chat.
		out.println("  <details id='llm-advice-preview-sect' style='border:1px solid #d0d0d0;border-radius:3px;background:#fafafa;padding:6px 10px;margin-bottom:14px;'>");
		out.println("    <summary style='cursor:pointer;font-weight:600;list-style:none;user-select:none;'>&#128203; LLM Prompt Preview</summary>");
		out.println("    <p class='text-muted' style='font-size:0.85em;margin-top:8px;'>"
				+ (enabled
					? "Build the exact prompt that would be sent to the LLM without actually sending it - handy to see what context gets included, or to paste into a different LLM chat and compare answers."
					: "Nothing gets sent anywhere - but here's the exact prompt DbxCentral would send. Copy it into any LLM chat (claude.ai, chatgpt.com, ...) yourself for a quick manual shortcut into the same advice.")
				+ "</p>");
		out.println("    <div id='llm-advice-preview-content'></div>");
		out.println("  </details>");
		if (enabled)
		{
			out.println("  <div id='llm-advice-content'></div>");
		}
		else
		{
			out.println("  <p>This feature is not enabled on this DbxCentral instance - but you can still preview the prompt above and paste it into an LLM chat yourself.</p>");
		}
		out.println("</div>");
		out.println();
		out.println("<script src='/scripts/sql-formatter/12.1.3/sql-formatter.min.js'></script>");
		out.println("<script src='/scripts/prism/prism-1.30.0.js'></script>");
		out.println("<script src='/scripts/marked/18.0.5/marked.min.js'></script>");
		out.println("<script src='/scripts/dompurify/3.4.11/purify.min.js'></script>");
		// srv/dbname (if any) only live in the URL fragment, never known server-side here - so, unlike
		// dbxShowplan.js's conditional include, always load these; dbxLlmAdvice.js's srv-based DDL-context
		// lookup (fetchDdlContextBySrv) needs DbxSqlTableNames.extractTablesAsync() to parse 'sql' for tables.
		out.println("<script src='/scripts/dbxtune/js/dbxSqlTableNames.js'></script>");
		out.println("<link rel='stylesheet' href='/scripts/dbxtune/css/dbxTableInfo.css'>");
		out.println("<script src='/scripts/dbxtune/js/dbxLlmAdvice.js'></script>");
		out.println("<script>");
		out.println("(function () {");
		out.println("    // Params are read from the URL FRAGMENT (#...), not the query string (?...) -");
		out.println("    // the fragment is never sent to the server, so a large SQL statement/DDL context");
		out.println("    // here never risks a 'URI Too Long' error the way a query string would.");
		out.println("    var qs = new URLSearchParams(window.location.hash.replace(/^#/, ''));");
		// When the report POSTed to us (large plans blow past the browser's URL limit - see doPost),
		// the values are embedded here instead. '</' is escaped so a value can never end this <script>.
		out.println("    var posted = " + (postedJson == null ? "null" : postedJson.replace("</", "<\\/")) + ";");
		out.println("    function P(n) { return posted ? (posted[n] || null) : qs.get(n); }");
		out.println("    var sql = P('sql');");
		out.println("    var previewSect = document.getElementById('llm-advice-preview-sect');");
		out.println();
		out.println("    if (!sql) {");
		if (enabled)
			out.println("        document.getElementById('llm-advice-content').textContent = \"No 'sql' parameter was supplied in the URL.\";");
		out.println("        previewSect.style.display = 'none';");
		out.println("        return;");
		out.println("    }");
		out.println();
		out.println("    // Common opts shared by both the Advice call below and the (lazily loaded) Preview section.");
		out.println("    var baseOpts = {");
		out.println("        sql:        sql,");
		out.println("        ddlContext: P('ddlContext'),");
		out.println("        plan:       P('plan'),");
		out.println("        dbVendor:   P('dbVendor'),");
		out.println("        jdbcUrl:    P('jdbcUrl'),");
		out.println("        jdbcUser:   P('jdbcUser'),");
		out.println("        srv:        P('srv'),"); // resolves ddlContext LIVE via that server's Collector - see dbxLlmAdvice.js
		out.println("        ts:         P('ts'),");
		out.println("        dbname:     P('dbname'),");
		// Raw execution statistics harvested from the Daily Summary Report's sparkline sub-table at click
		// time (see SparklineHelper.getWorkloadHarvesterJs()). dbxLlmAdvice.js turns this into the
		// 'workloadProfile' text that is sent to the LLM.
		out.println("        workloadData: P('workloadData')");
		out.println("    };");
		out.println();
		if (enabled)
		{
			out.println("    dbxLlmAdvice.open(Object.assign({}, baseOpts, {");
			out.println("        provider: P('provider'),");
			out.println("        target:   document.getElementById('llm-advice-content')");
			out.println("    }));");
			out.println();
		}
		out.println("    // Prompt Preview - lazy-loaded on first expand, same pattern dbxShowplan.js's modal uses.");
		out.println("    previewSect.addEventListener('toggle', function () {");
		out.println("        if (!this.open) return;");
		out.println("        var body = document.getElementById('llm-advice-preview-content');");
		out.println("        if (body.getAttribute('data-loaded') === 'true') return;");
		out.println("        body.setAttribute('data-loaded', 'true');");
		out.println("        dbxLlmAdvice.open(Object.assign({}, baseOpts, {");
		out.println("            preview: true,");
		out.println("            target:  body");
		out.println("        }));");
		out.println("    });");
		out.println("})();");
		out.println("</script>");
		out.println(HtmlStatic.getJavaScriptAtEnd(true));
		out.println("</body>");
		out.println("</html>");

		out.flush();
		out.close();
	}
}
