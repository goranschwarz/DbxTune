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
import java.util.Set;

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
		if ( ! LlmClientRegistry.isFeatureEnabled() )
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
		if ( ! LlmClientRegistry.isFeatureEnabled() )
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
		return "<a href='" + href + "' target='_blank'>Get LLM Optimization Advice</a>";
	}
}
