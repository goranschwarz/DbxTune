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
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.dbxtune.pcs.report.content;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.text.StringEscapeUtils;

import com.dbxtune.utils.DbUtils;
import com.dbxtune.utils.StringUtil;

/**
 * Builds a "View Execution Plan" link to DbxCentral's own graphical Showplan viewer
 * ({@code /showplan/ase} or {@code /showplan/sqlserver}).
 * <p>
 * Unlike a plain {@code <a href='...#plan=...'>}, this does NOT embed the (potentially huge, up to
 * ~128KB) plan text in the link itself - that blew up report size, since the same plan text would
 * otherwise be duplicated into every link that needs it (this one, "Get LLM Optimization Advice",
 * "Copy XML"'s own hidden data block, ...), each copy additionally inflated ~2-4x by URL
 * percent-encoding.
 * <p>
 * Instead, the caller is expected to already be writing the plan text ONCE into a hidden
 * {@code <script type='text/xmldata' id='...'>} block on the page (both {@code AseTopCmStmntCacheDetails}
 * and {@code ExecutionPlanCollection} already do this, for "Copy XML"/"view plan"), and passes that
 * element's id as {@code planElementId} here. The generated link reads the plan text back out of
 * that element at CLICK time (via the shared {@code dsrOpenLink(...)} JS function - see
 * {@code AseTopCmStmntCacheDetails}/{@code ExecutionPlanCollection} for where it's defined) and opens
 * it in a new tab - so the plan text itself never appears more than once in the page/e-mail source.
 * <p>
 * This makes the link JavaScript-dependent: it will not do anything if the report is opened directly
 * in an e-mail client (same limitation "Copy XML" already has) - the tooltip says so.
 */
public class ShowplanLinkBuilder
{
	/**
	 * @param dbxCentralBaseUrl base URL of this DbxCentral instance, e.g. from {@code getReportingInstance().getDbxCentralPublicBaseUrl()}
	 * @param planElementId     id of the hidden {@code <script type='text/xmldata'>} element already holding the plan text, or null/blank if no plan is available
	 * @param sql               the SQL statement this plan belongs to, may be blank
	 * @param dbVendor          DBMS product name, e.g. {@link DbUtils#DB_PROD_NAME_SYBASE_ASE} or {@link DbUtils#DB_PROD_NAME_MSSQL} - other vendors return ""
	 * @param srv               name of an actively-monitored DbxTune server this plan came from, or null/blank
	 * @param dbname            database name on {@code srv} the SQL's tables live in, or null/blank
	 * @return the anchor tag HTML, or "" if no plan/base-URL was supplied or the vendor isn't supported
	 */
	public static String buildViewPlanLinkHtml(String dbxCentralBaseUrl, String planElementId, String sql, String dbVendor, String srv, String dbname)
	{
		return buildViewPlanLinkHtml(dbxCentralBaseUrl, planElementId, sql, dbVendor, srv, dbname, "View Execution Plan", true);
	}

	/**
	 * Same as {@link #buildViewPlanLinkHtml(String, String, String, String, String, String)}, but with control over
	 * the link text and the leading icon.
	 * <p>
	 * This is for callers that renders a LIST of plan links for the same SQL Statement (for example
	 * {@code AseTopSlowNormalizedSql}, where one <i>normalized</i> statement can map to several Statement
	 * Cache entries, each with its own plan). Such a caller wants to write the icon and the
	 * "View Execution Plan:" label ONCE, followed by short, per-plan anchors.
	 *
	 * @param linkText     text of the anchor, for example the Statement Cache name 'ss0087948680_1345721111'
	 * @param includeIcon  if a leading "open in new tab" icon should be part of the returned HTML
	 * @see #buildViewPlanLinkHtml(String, String, String, String, String, String)
	 */
	public static String buildViewPlanLinkHtml(String dbxCentralBaseUrl, String planElementId, String sql, String dbVendor, String srv, String dbname, String linkText, boolean includeIcon)
	{
		if (StringUtil.isNullOrBlank(planElementId) || StringUtil.isNullOrBlank(dbxCentralBaseUrl))
			return "";

		boolean isAse = DbUtils.isProductName(dbVendor, DbUtils.DB_PROD_NAME_SYBASE_ASE);
		String path = isAse ? "/showplan/ase"
				: DbUtils.isProductName(dbVendor, DbUtils.DB_PROD_NAME_MSSQL) ? "/showplan/sqlserver"
				: null;
		if (path == null)
			return "";

		// Best-effort hostname for the tooltip - fall back to the raw base URL if it doesn't parse
		String host = dbxCentralBaseUrl;
		try { host = new URL(dbxCentralBaseUrl).getHost(); } catch (MalformedURLException e) { /* fall back to raw base URL */ }

		return (includeIcon ? "<i class='fa-solid fa-arrow-up-right-from-square'></i>&nbsp;" : "")
				+ "<a href='javascript:void(0)' class='dsr-link' "
				+ "data-kind='showplan' "
				+ "data-path='" + esc(dbxCentralBaseUrl + path) + "' "
				+ "data-plan-id='" + esc(planElementId) + "' "
				+ (isAse ? "data-isxml='true' " : "")
				+ "data-sql='" + esc(StringUtil.nullToValue(sql, "")) + "' "
				+ "data-srv='" + esc(StringUtil.nullToValue(srv, "")) + "' "
				+ "data-dbname='" + esc(StringUtil.nullToValue(dbname, "")) + "' "
				+ "onclick='dsrOpenLink(this); return false;' "
				+ "title='Opens the graphical execution plan in DbxCentral at " + esc(host) + ". "
				+ "Requires JavaScript, so this will not work if you are reading this report in an e-mail client. "
				+ "DbxCentral must also be reachable/online.'>"
				+ StringEscapeUtils.escapeHtml4(StringUtil.nullToValue(linkText, "View Execution Plan")) + "</a>";
	}

	/**
	 * The {@code dsrOpenLink()} function that EVERY {@code class='dsr-link'} anchor's onclick calls -
	 * both "View Execution Plan" (see {@link #buildViewPlanLinkHtml}) and "Get LLM Optimization Advice"
	 * (see {@code LlmSqlContextBuilder.buildAdviceLinkHtmlJs}) - plus the workload harvester those links
	 * use ({@code SparklineHelper.getWorkloadHarvesterJs()}).
	 * <p>
	 * <b>This MUST be written into every report, unconditionally.</b> It used to be emitted as a
	 * side-effect of a section that happened to have execution plans to write
	 * ({@code AseTopCmStmntCacheDetails} / {@code ExecutionPlanCollection}, the latter behind
	 * {@code if (!_planMap.isEmpty())}). But the links themselves do NOT depend on a plan existing -
	 * an advice link is emitted for any statement with SQL text - so on a server where no showplans
	 * were captured the report ended up full of anchors calling a function that was never defined:
	 * <pre>    Uncaught ReferenceError: dsrOpenLink is not defined</pre>
	 * Emitting it once from the report scaffolding (see {@code DailySummaryReportDefault.createHtmlBody})
	 * removes that coupling entirely, and covers ASE, SQL Server and Postgres with one call.
	 * <p>
	 * The {@code if (typeof dsrOpenLink === 'undefined')} guard is kept so this stays harmless if some
	 * older/other code path also defines it.
	 */
	public static String getDsrLinkSupportJs()
	{
		StringBuilder sb = new StringBuilder();

		sb.append("\n");
		sb.append("<script type='text/javascript'> \n");
		sb.append("    if (typeof dsrOpenLink === 'undefined') { \n");
		sb.append("    function dsrOpenLink(a) \n"); // Opens "View Execution Plan"/"Get LLM Optimization Advice" in a new tab, reading
		sb.append("    { \n");                        // the (already page-embedded, never duplicated) plan text via 'data-plan-id' at click time.
		sb.append("        var d = a.dataset; \n");
		sb.append("        var planEl = d.planId ? document.getElementById(d.planId) : null; \n");
		sb.append("        var plan = planEl ? planEl.textContent : ''; \n");
		sb.append("        var params = {}; \n");
		sb.append("        function add(name, val) { if (val) params[name] = val; } \n");
		sb.append("        if (d.kind === 'showplan') { \n");
		sb.append("            add('plan', plan); \n");
		sb.append("            if (d.isxml) add('isXml', d.isxml); \n");
		sb.append("            add('sql', d.sql); \n");
		sb.append("            add('srv', d.srv); \n");
		sb.append("            add('dbname', d.dbname); \n");
		// The Showplan viewer has its OWN "Get LLM Optimization Advice" section, so it needs the
		// execution statistics too - otherwise asking for advice from inside the plan viewer silently
		// loses the workload profile that the report's own advice link would have sent.
		sb.append("            add('workloadData', dsrHarvestWorkload(a)); \n");
		sb.append("        } else { \n");
		sb.append("            add('sql', d.sql); \n");
		sb.append("            add('plan', plan); \n");
		sb.append("            add('dbVendor', d.vendor); \n");
		sb.append("            add('srv', d.srv); \n"); // ddlContext is resolved LIVE by dbxLlmAdvice.js from srv+dbname, not precomputed here
		sb.append("            add('dbname', d.dbname); \n");
		sb.append("            add('workloadData', dsrHarvestWorkload(a)); \n"); // execution statistics, read out of the page
		sb.append("        } \n");
		sb.append("        dsrPostToNewTab(d.path, params); \n");
		sb.append("    } \n");
		sb.append("\n");

		// Why POST and not window.open('...#plan=...'):
		//   The execution plan travels with the link, and a plan is BIG. Measured on real reports, the
		//   URL this used to build had a median of ~67KB and a maximum of ~845KB (ASE) / ~486KB (SQL
		//   Server). Chrome silently refuses to navigate to a URL that long - it opens the new tab and
		//   leaves it at 'about:blank#blocked', with no error anywhere. ASE hit it, SQL Server happened
		//   to stay just under the line, which is exactly the kind of "works on my data" trap to avoid.
		//   A form POST has no such limit (and Jetty's form-size cap is already lifted - see
		//   WebServerInitializerJetty: setMaxFormContentSize(-1)), so ALL payloads take the same path.
		//   ShowplanAseServlet/ShowplanSqlServerServlet/LlmAdviceServlet all accept these as POST params.
		sb.append("    function dsrPostToNewTab(action, params) \n");
		sb.append("    { \n");
		// The configured DbxCentral base URL often uses a DIFFERENT SCHEME than the page we are on:
		// this report is normally served BY DbxCentral over https, while 'dbxCentralPublicBaseUrl' is
		// commonly plain http. Posting to http from an https page is bad twice over:
		//   1) Chrome warns "The information you're about to submit is not secure", and
		//   2) the http->https 301 that follows converts the POST into a GET and DROPS THE BODY, so the
		//      target page opens with no plan at all and simply renders nothing.
		// Posting to a SAME-ORIGIN relative path avoids both - it inherits the page's own scheme/host
		// and never redirects. If the report is opened from somewhere else (a saved file, an e-mail
		// attachment) the host will not match and the absolute URL is used unchanged.
		sb.append("        try { \n");
		sb.append("            var u = new URL(action, window.location.href); \n");
		sb.append("            if (u.host && u.host === window.location.host) action = u.pathname + u.search; \n");
		sb.append("        } catch (e) { /* leave 'action' exactly as it was */ } \n");
		sb.append("        var f = document.createElement('form'); \n");
		sb.append("        f.method        = 'POST'; \n");
		sb.append("        f.action        = action; \n");
		sb.append("        f.target        = '_blank'; \n");
		sb.append("        f.acceptCharset = 'UTF-8'; \n");
		sb.append("        f.style.display = 'none'; \n");
		sb.append("        for (var k in params) \n");
		sb.append("        { \n");
		sb.append("            if (!Object.prototype.hasOwnProperty.call(params, k)) continue; \n");
		sb.append("            var i = document.createElement('input'); \n");
		sb.append("            i.type  = 'hidden'; \n");
		sb.append("            i.name  = k; \n");
		sb.append("            i.value = params[k]; \n");
		sb.append("            f.appendChild(i); \n");
		sb.append("        } \n");
		sb.append("        document.body.appendChild(f); \n");
		sb.append("        f.submit(); \n");
		sb.append("        document.body.removeChild(f); \n");
		sb.append("    } \n");
		sb.append("    } \n");
		sb.append("\n");

		// dsrHarvestWorkload()/dsrAddWorkload() - bare JS, spliced into the <script> opened above
		sb.append(SparklineHelper.getWorkloadHarvesterJs());

		sb.append("</script> \n");
		sb.append("\n");

		return sb.toString();
	}

	/**
	 * HTML-attribute-escape for a SINGLE-quoted attribute value (the convention used throughout this
	 * codebase, e.g. {@code data-sql='...'}). {@link StringEscapeUtils#escapeHtml4(String)} alone is
	 * NOT enough here - it does not escape {@code '}, and this is used to carry raw SQL text, which
	 * routinely contains single-quoted string literals that would otherwise break out of the
	 * attribute early.
	 */
	private static String esc(String s)
	{
		return StringEscapeUtils.escapeHtml4(s).replace("'", "&#39;");
	}
}
