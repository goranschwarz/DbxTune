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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.central.controllers.HtmlStatic.PageSection;
import com.dbxtune.central.llm.LlmClientRegistry;

/**
 * <pre>GET /llm-advice?sql=...&amp;ddlContext=...&amp;plan=...&amp;dbVendor=...&amp;jdbcUrl=...&amp;jdbcUser=...&amp;dbname=...&amp;provider=...</pre>
 * <p>
 * Standalone "Get LLM Optimization Advice" page, with the same navbar/login chrome as the rest of
 * DbxCentral (used by the mail-safe link the Daily Summary Report links here to, since generated
 * report HTML can end up being e-mailed and mail clients don't run inline JavaScript).
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

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
	throws ServletException, IOException
	{
		resp.setContentType("text/html;charset=UTF-8");
		resp.setCharacterEncoding("UTF-8");
		PrintWriter out = resp.getWriter();

		if ( ! LlmClientRegistry.isFeatureEnabled() )
		{
			out.println("<!DOCTYPE html>");
			out.println("<html lang='en-US'>");
			out.println("<head>");
			out.println("  <meta charset='UTF-8'>");
			out.println("  <title>DbxTune - LLM Optimization Advice</title>");
			out.println(HtmlStatic.getUserDefinedContentHead());
			out.println("</head>");
			out.println("<body>");
			out.println(HtmlStatic.getHtmlNavbar(PageSection.None, "", true));
			out.println("<div class='container-fluid' style='max-width:900px;margin:20px auto;'>");
			out.println("  <h2>LLM Optimization Advice</h2>");
			out.println("  <p>This feature is not enabled on this DbxCentral instance.</p>");
			out.println("</div>");
			out.println(HtmlStatic.getJavaScriptAtEnd(true));
			out.println("</body>");
			out.println("</html>");
			out.flush();
			out.close();
			return;
		}

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
		out.println("  <div id='llm-advice-content'></div>");
		out.println("</div>");
		out.println();
		out.println("<script src='/scripts/sql-formatter/12.1.3/sql-formatter.min.js'></script>");
		out.println("<script src='/scripts/prism/prism-1.30.0.js'></script>");
		out.println("<script src='/scripts/marked/18.0.5/marked.min.js'></script>");
		out.println("<script src='/scripts/dompurify/3.4.11/purify.min.js'></script>");
		out.println("<script src='/scripts/dbxtune/js/dbxLlmAdvice.js'></script>");
		out.println("<script>");
		out.println("(function () {");
		out.println("    // Params are read from the URL FRAGMENT (#...), not the query string (?...) -");
		out.println("    // the fragment is never sent to the server, so a large SQL statement/DDL context");
		out.println("    // here never risks a 'URI Too Long' error the way a query string would.");
		out.println("    var qs      = new URLSearchParams(window.location.hash.replace(/^#/, ''));");
		out.println("    var sql     = qs.get('sql');");
		out.println("    var content = document.getElementById('llm-advice-content');");
		out.println();
		out.println("    if (!sql) {");
		out.println("        content.textContent = \"No 'sql' parameter was supplied in the URL.\";");
		out.println("        return;");
		out.println("    }");
		out.println();
		out.println("    dbxLlmAdvice.open({");
		out.println("        sql:        sql,");
		out.println("        ddlContext: qs.get('ddlContext'),");
		out.println("        plan:       qs.get('plan'),");
		out.println("        dbVendor:   qs.get('dbVendor'),");
		out.println("        jdbcUrl:    qs.get('jdbcUrl'),");
		out.println("        jdbcUser:   qs.get('jdbcUser'),");
		out.println("        dbname:     qs.get('dbname'),");
		out.println("        provider:   qs.get('provider'),");
		out.println("        target:     content");
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
