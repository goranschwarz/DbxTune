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
package com.dbxtune.central.llm;

import java.lang.invoke.MethodHandles;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.pcs.report.content.ase.AseAbstract;
import com.dbxtune.pcs.report.content.postgres.PostgresAbstract;
import com.dbxtune.pcs.report.content.sqlserver.SqlServerAbstract;
import com.dbxtune.sql.SqlParserUtils;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.DbUtils;
import com.dbxtune.utils.HtmlQueryString;
import com.dbxtune.utils.StringUtil;

/**
 * Builds the plain-text "DDL/index/stats context" bundle used as LLM prompt
 * input, for callers that don't already have it in hand (the Active
 * Statements web dialog, the Showplan viewer - see {@code LlmContextServlet}).
 * <p>
 * Reuses the same DDL Storage subsystem ({@code MonDdlStorage}, populated by
 * each Collector) that the Daily Summary Report already queries via
 * {@code AseAbstract}/{@code SqlServerAbstract}/{@code PostgresAbstract}'s
 * {@code getTableInformationFromMonDdlStorage(...)} - just through each
 * vendor's {@code getTableInfoPlainText(...)} static convenience method
 * (which needs only a connection, not a full report-entry instance).
 * <p>
 * Report classes that already hold a fetched {@code Set<XxxTableInfo>} (the
 * Daily Summary Report, at generation time) should call each vendor's
 * {@code getTableInfoAsPlainText(...)} instance method directly instead of
 * this class, to avoid a second DDL Storage lookup.
 * <p>
 * Both methods here gate on {@link LlmClientRegistry#isFeatureEnabledViaDbxCentral()}, not the
 * plain {@link LlmClientRegistry#isFeatureEnabled()} - Daily Summary Report generation (the other
 * caller of {@link #buildDdlContext}/{@link #buildAdviceLinkHtml}, besides {@code LlmContextServlet})
 * can run inside a Collector process during rollover, which has its own separate config file and
 * would otherwise never see {@code DbxCentral.llm.enabled} unless that property were duplicated into
 * every Collector's config - see {@link LlmClientRegistry#isFeatureEnabledViaDbxCentral()}'s javadoc.
 */
public class LlmSqlContextBuilder
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	/**
	 * Parse {@code sqlText} for table names and look up their DDL/index/stats
	 * from the DDL Storage subsystem for the given {@code dbVendor}.
	 *
	 * @param conn     connection to the server's persisted counter store (PCS/H2), holding {@code MonDdlStorage}
	 * @param dbname   current database name (used to prefer objects in that database when duplicates exist)
	 * @param sqlText  the SQL statement to parse for table names
	 * @param dbVendor DBMS product name, e.g. {@link DbUtils#DB_PROD_NAME_SYBASE_ASE}, {@link DbUtils#DB_PROD_NAME_MSSQL}, {@link DbUtils#DB_PROD_NAME_POSTGRES}
	 * @return plain-text DDL/index/stats bundle, or "" if nothing was found / vendor not supported
	 */
	public static String buildDdlContext(DbxConnection conn, String dbname, String sqlText, String dbVendor)
	{
		if ( ! LlmClientRegistry.isFeatureEnabledViaDbxCentral() )
			return "";

		if (conn == null || StringUtil.isNullOrBlank(sqlText) || StringUtil.isNullOrBlank(dbVendor))
			return "";

		Set<String> tableList = SqlParserUtils.getTables(sqlText);
		if (tableList.isEmpty())
			return "";

		try
		{
			if (DbUtils.isProductName(dbVendor, DbUtils.DB_PROD_NAME_SYBASE_ASE))
				return AseAbstract.getTableInfoPlainText(conn, dbname, tableList);

			if (DbUtils.isProductName(dbVendor, DbUtils.DB_PROD_NAME_MSSQL))
				return SqlServerAbstract.getTableInfoPlainText(conn, dbname, tableList);

			if (DbUtils.isProductName(dbVendor, DbUtils.DB_PROD_NAME_POSTGRES))
				return PostgresAbstract.getTableInfoPlainText(conn, dbname, tableList);

			_logger.info("buildDdlContext(): no DDL Storage lookup implemented for dbVendor='{}', skipping DDL context.", dbVendor);
			return "";
		}
		catch (Exception ex)
		{
			_logger.warn("buildDdlContext(): problem looking up DDL Storage info for dbVendor='" + dbVendor + "', tableList=" + tableList + ".", ex);
			return "";
		}
	}

	/**
	 * Build a plain {@code <a href='...'>} link to the standalone {@code /llm-advice} page,
	 * with {@code sql}/{@code ddlContext}/{@code dbVendor} pre-filled - as a URL <b>fragment</b>
	 * ({@code #...}), not a query string ({@code ?...}).
	 * <p>
	 * Used from the Daily Summary Report, where the generated HTML can end up being sent as an
	 * e-mail - so, unlike the Active Statements dialog and the Showplan viewer (which call
	 * {@code dbxLlmAdvice.open(...)} directly via inline JavaScript), this needs to be a plain
	 * hyperlink that opens a real page rather than JavaScript embedded in the report.
	 * <p>
	 * The fragment is never sent to the server (browsers strip everything after {@code #} before
	 * issuing the HTTP request), so it isn't subject to Jetty's/any reverse proxy's URI-length
	 * limit the way a query string would be - real DDL context + a large SQL statement can easily
	 * exceed a typical ~8KB server limit and trigger "413/414 URI Too Long". {@code /llm-advice}
	 * already reads all of this purely client-side (see {@code LlmAdviceServlet}), so moving it
	 * from {@code location.search} to {@code location.hash} is the only change needed there.
	 *
	 * @param dbxCentralBaseUrl base URL of this DbxCentral instance, e.g. from {@code getReportingInstance().getDbxCentralPublicBaseUrl()}
	 * @param sql               the SQL statement
	 * @param ddlContext        plain-text DDL/index/stats bundle (from {@link #buildDdlContext} or a vendor's {@code getTableInfoAsPlainText}), may be blank
	 * @param dbVendor          DBMS product name
	 */
	public static String buildAdviceLinkHtml(String dbxCentralBaseUrl, String sql, String ddlContext, String dbVendor)
	{
		return buildAdviceLinkHtml(dbxCentralBaseUrl, sql, ddlContext, null, dbVendor);
	}

	/**
	 * Same as {@link #buildAdviceLinkHtml(String, String, String, String)}, but also includes the
	 * execution plan when the caller already has one in hand (e.g. a cached XML plan already
	 * resolved for this row), so it doesn't have to be re-fetched from {@code /llm-advice}.
	 *
	 * @param plan execution plan text/XML, or null/blank if not available
	 */
	public static String buildAdviceLinkHtml(String dbxCentralBaseUrl, String sql, String ddlContext, String plan, String dbVendor)
	{
		return buildAdviceLinkHtml(dbxCentralBaseUrl, sql, ddlContext, plan, dbVendor, "Get LLM Optimization Advice", true);
	}

	/**
	 * Same as {@link #buildAdviceLinkHtml(String, String, String, String, String)}, but with control over
	 * the link text and the leading icon.
	 * <p>
	 * This is for callers that renders a LIST of advice links for the same SQL Statement (for example
	 * {@code AseTopSlowNormalizedSql}, where one <i>normalized</i> statement maps to several Statement
	 * Cache entries, each with its own plan). Such a caller wants to write the icon and the
	 * "Get LLM Optimization Advice:" label ONCE, followed by short, per-entry anchors.
	 *
	 * @param linkText     text of the anchor, for example "SQL only"
	 * @param includeIcon  if a leading "open in new tab" icon should be part of the returned HTML
	 */
	public static String buildAdviceLinkHtml(String dbxCentralBaseUrl, String sql, String ddlContext, String plan, String dbVendor, String linkText, boolean includeIcon)
	{
		if ( ! LlmClientRegistry.isFeatureEnabledViaDbxCentral() )
			return "";

		if (StringUtil.isNullOrBlank(sql))
			return "";

		// No base URL set on the HtmlQueryString itself - getQuery() below gives just the encoded
		// "key=val&key=val" part, which we then attach as a fragment (#), not a query string (?).
		HtmlQueryString qs = new HtmlQueryString();
		qs.add("sql", sql);
		qs.add("dbVendor", dbVendor);
		if (StringUtil.hasValue(ddlContext))
			qs.add("ddlContext", ddlContext);
		if (StringUtil.hasValue(plan))
			qs.add("plan", plan);

		String href = StringUtil.nullToValue(dbxCentralBaseUrl, "") + "/llm-advice#" + qs.getQuery();

		// Best-effort hostname for the tooltip - fall back to the raw base URL if it doesn't parse
		String host = dbxCentralBaseUrl;
		try { host = new java.net.URL(dbxCentralBaseUrl).getHost(); } 
		catch (java.net.MalformedURLException e) { /* fall back to raw base URL */ }

		// 'dsrAddWorkload()' appends the execution statistics (harvested from the sparkline sub-table in this
		// very page) to the href, then lets the normal link work. It is a pure enrichment: with JavaScript
		// disabled - which is the whole point of this plain variant - the href above is used as-is.
		// See SparklineHelper.getWorkloadHarvesterJs().
		return (includeIcon ? "<i class='fa-solid fa-arrow-up-right-from-square'></i>&nbsp;" : "") // icon
				+ "<a href='" + href + "' target='_blank' "
				+ "onclick='if (typeof dsrAddWorkload === \"function\") return dsrAddWorkload(this); return true;' "
				+ "title='Opens LLM Optimization Advice in DbxCentral at " + host + " - requires DbxCentral to be reachable; will not work if it is offline'>"
				+ esc(StringUtil.nullToValue(linkText, "Get LLM Optimization Advice")) + "</a>";
	}

	/**
	 * Same intent as {@link #buildAdviceLinkHtml(String, String, String, String, String)}, but for
	 * callers that already have an execution plan payload written ONCE to the page in a hidden
	 * {@code <script type='text/xmldata' id='...'>} block (both {@code AseTopCmStmntCacheDetails} and
	 * {@code ExecutionPlanCollection} already do this, for "Copy XML"/"view plan").
	 * <p>
	 * Unlike {@link #buildAdviceLinkHtml(String, String, String, String, String)}, this does NOT embed
	 * the plan as a URL fragment (a small {@code data-*} attribute-carrying link reads it back out of
	 * {@code planElementId} at CLICK time, via the shared {@code dsrOpenLink(...)} JS function - see
	 * {@code AseTopCmStmntCacheDetails}/{@code ExecutionPlanCollection} for where it's defined), and it
	 * does NOT precompute {@code ddlContext} at all - {@code buildDdlContext(...)} (a DDL Storage
	 * lookup, run at report-GENERATION time against a PCS connection the generator already has open)
	 * can be even bigger than the plan itself, and unlike the plan it's not deduplicated by anything -
	 * every row would otherwise carry its own copy whether or not anyone ever clicks the link. Instead,
	 * {@code srv}/{@code dbname} are carried through, and {@code dbxLlmAdvice.js}'s
	 * {@code fetchDdlContextBySrv(...)} resolves the DDL/index/stats context LIVE at click time, via the
	 * same {@code /api/cc/mgt/table-info}/{@code /api/cc/mgt/query-store} proxy-to-Collector endpoints
	 * {@code dbxShowplan.js}'s own "Get LLM Optimization Advice" section already uses - so nothing is
	 * computed or stored in the report at all unless the link is actually clicked.
	 * <p>
	 * This makes the link JavaScript-dependent: it will not do anything if the report is opened
	 * directly in an e-mail client (same limitation "Copy XML" already has) - the tooltip says so.
	 *
	 * @param dbxCentralBaseUrl base URL of this DbxCentral instance, e.g. from {@code getReportingInstance().getDbxCentralPublicBaseUrl()}
	 * @param sql               the SQL statement
	 * @param planElementId     id of the hidden {@code <script type='text/xmldata'>} element already holding the plan text, or null/blank if no plan is available
	 * @param dbVendor          DBMS product name
	 * @param srv               name of an actively-monitored DbxTune server this SQL came from, or null/blank - required (together with {@code dbname}) for the live DDL-context lookup
	 * @param dbname            database name on {@code srv} the SQL's tables live in, or null/blank
	 */
	public static String buildAdviceLinkHtmlJs(String dbxCentralBaseUrl, String sql, String planElementId, String dbVendor, String srv, String dbname)
	{
		return buildAdviceLinkHtmlJs(dbxCentralBaseUrl, sql, planElementId, dbVendor, srv, dbname, "Get LLM Optimization Advice", true);
	}

	/**
	 * Same as {@link #buildAdviceLinkHtmlJs(String, String, String, String, String, String)}, but with
	 * control over the link text and the leading icon.
	 * <p>
	 * This is for callers that renders a LIST of advice links for the same SQL Statement (for example
	 * {@code AseTopSlowNormalizedSql}, where one <i>normalized</i> statement maps to several Statement
	 * Cache entries, each with its own plan - so the reader can ask for advice based on any one of
	 * those plans). Such a caller wants to write the icon and the "Get LLM Optimization Advice:" label
	 * ONCE, followed by short, per-plan anchors.
	 *
	 * @param linkText     text of the anchor, for example the Statement Cache name 'ss0087948680_1345721111'
	 * @param includeIcon  if a leading "open in new tab" icon should be part of the returned HTML
	 */
	public static String buildAdviceLinkHtmlJs(String dbxCentralBaseUrl, String sql, String planElementId, String dbVendor, String srv, String dbname, String linkText, boolean includeIcon)
	{
		if ( ! LlmClientRegistry.isFeatureEnabledViaDbxCentral() )
			return "";

		if (StringUtil.isNullOrBlank(sql))
			return "";

		// Best-effort hostname for the tooltip - fall back to the raw base URL if it doesn't parse
		String host = dbxCentralBaseUrl;
		try { host = new URL(dbxCentralBaseUrl).getHost(); } catch (MalformedURLException e) { /* fall back to raw base URL */ }

		return (includeIcon ? "<i class='fa-solid fa-arrow-up-right-from-square'></i>&nbsp;" : "") // icon
				+ "<a href='javascript:void(0)' class='dsr-link' "
				+ "data-kind='llmadvice' "
				+ "data-path='" + esc(StringUtil.nullToValue(dbxCentralBaseUrl, "") + "/llm-advice") + "' "
				+ (StringUtil.hasValue(planElementId) ? "data-plan-id='" + esc(planElementId) + "' " : "")
				+ "data-sql='" + esc(sql) + "' "
				+ "data-vendor='" + esc(StringUtil.nullToValue(dbVendor, "")) + "' "
				+ "data-srv='" + esc(StringUtil.nullToValue(srv, "")) + "' "
				+ "data-dbname='" + esc(StringUtil.nullToValue(dbname, "")) + "' "
				+ "onclick='dsrOpenLink(this); return false;' "
				+ "title='Opens LLM Optimization Advice in DbxCentral at " + esc(host) + ". "
				+ "Requires JavaScript, so this will not work if you are reading this report in an e-mail client. "
				+ "DbxCentral must also be reachable/online.'>"
				+ esc(StringUtil.nullToValue(linkText, "Get LLM Optimization Advice")) + "</a>";
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
