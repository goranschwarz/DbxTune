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
package com.dbxtune.pcs.report.content.postgres;

import java.io.IOException;
import java.io.Writer;
import java.util.Set;

import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;

/**
 * The most bloated tables (estimated from the statistics, CmPgTableBloat), with the fix.
 */
public class PostgresTableBloat
extends PostgresAbstract
{
	private ResultSetTableModel _shortRstm; // null = not recorded
	private int _tableCount = -1;           // tables in the recording
	private int _naCount    = -1;           // of them without a bloat number (not measured, and is_na)
	private boolean _hasSource  = false;    // the recording has bloat_source (measured bloat, 2026-11)
	private boolean _hasTableMb = false;    // the recording has approx_table_mb

	public PostgresTableBloat(DailySummaryReportAbstract reportingInstance)
	{
		super(reportingInstance);
	}

	@Override
	public boolean hasMinimalMessageText()
	{
		return false;
	}

	@Override
	public boolean hasShortMessageText()
	{
		return true;
	}

	@Override
	public void writeMessageText(Writer w, MessageType messageType)
	throws IOException
	{
		if (_shortRstm == null)
		{
			w.append("<b>Table bloat was not recorded</b> (the table <code>CmPgTableBloat_abs</code> does not exist: the Counter Model CmPgTableBloat was not enabled).<br>\n");
			return;
		}

		// Tables without an estimate: say why (with a 'pg_monitor' login it is all of them)
		String naText = "";
		if (_naCount > 0)
		{
			naText = "<b>" + _naCount + " of " + _tableCount + " tables had no bloat number</b>: not measured (the extension pgstattuple is not in the database, or the table is small), "
					+ "and not estimated (some of their columns have no statistics in <code>pg_stats</code>). "
					+ (_naCount >= _tableCount * 0.9 // (nearly) all: a table whose columns the login may read (or with no columns) still gets an estimate
						? "When it is (nearly) <i>all</i> tables, the login can't see the statistics: pg_stats only shows the columns the login may SELECT, and a <code>pg_monitor</code> login may not. "
						+ "<b>Recommended fix</b>: <code>CREATE EXTENSION pgstattuple;</code> in each database (as a superuser): pg_monitor may then measure the bloat, without reading any data. "
						+ "(The alternative, <code>GRANT pg_read_all_data</code>, gives the login read access to all data: not recommended.) "
						: "Run ANALYZE on them (or the columns have a type without statistics), or create the extension pgstattuple in the database. ")
					+ "Tables without a number are not listed.<br>\n";
		}

		if (_shortRstm.getRowCount() == 0)
		{
			w.append(naText);
			if (_naCount < _tableCount * 0.9 || _tableCount <= 0)
				w.append("<b>No table had more than " + getMinBloatMb() + " MB of bloat</b> (property <code>" + getClass().getSimpleName() + ".minBloatMb</code>).<br>\n");
			return;
		}
		w.append(naText);

		w.append(getSectionDescriptionHtml(_shortRstm, true));
		w.append("Row Count: " + _shortRstm.getRowCount() + "&emsp;&emsp; To change number of <i>top</i> records, set property <code>" + getTopRowsPropertyName() + "=##</code><br>\n");
		w.append(toHtmlTable(_shortRstm));
	}

	@Override
	public String getSubject()
	{
		return "Table Bloat (order by: bloat_mb, origin: CmPgTableBloat / pgstattuple_approx or pg_stats)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue (the TableBloat alarm does that)
	}

	@Override
	public int getTopRowsDefault()
	{
		return 20;
	}

	/** Tables with less estimated bloat than this (MB) are not listed */
	private int getMinBloatMb()
	{
		return Configuration.getCombinedConfiguration().getIntProperty(getClass().getSimpleName() + ".minBloatMb", 10);
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		String schema = getReportingInstance().getDbmsSchemaName();
		String where  = "where 1 = 1 " + getReportPeriodSqlWhere().trim();

		if ( ! doTableExist(conn, schema, "CmPgTableBloat_abs") )
			return;

		// The bloat of a row: MEASURED (pgstattuple_approx, the columns approx_*) when there, else the ESTIMATE from pg_stats when it could be made (not is_na).
		// is_na: some columns have no statistics in pg_stats, and with a 'pg_monitor' login that is all tables (pg_stats only shows the columns the login may SELECT)
		Set<String> cols = getRecordedColumns(conn, "CmPgTableBloat_abs");
		_hasSource  = cols.contains("bloat_source");    // added 2026-11 (older recordings: the estimate only)
		_hasTableMb = cols.contains("approx_table_mb"); // added 2026-11, a little later
		String mbExpr  = _hasSource
				? "case when [bloat_source] = 'pgstattuple_approx' then [approx_bloat_mb] when [is_na] = false then [bloat_size_mb] end"
				: "case when [is_na] = false then [bloat_size_mb] end";
		String pctExpr = _hasSource
				? "case when t.[bloat_source] = 'pgstattuple_approx' then t.[approx_bloat_pct] when t.[is_na] = false then t.[bloat_pct] end"
				: "case when t.[is_na] = false then t.[bloat_pct] end";
		String tMbExpr = mbExpr.replace("[", "t.[");

		// Each table at its last sample in the period (the CM samples every 10 minutes)
		String sql = ""
			    + "select top " + getTopRows() + " \n"
			    + "     t.[dbname] \n"
			    + "    ,t.[schemaname] \n"
			    + "    ,t.[tablename] \n"
			    + "    ," + (_hasTableMb ? "coalesce(t.[approx_table_mb], t.[real_size_mb])" : "t.[real_size_mb]") + " as [table_mb] \n" // measured when there (relpages is only updated by VACUUM/ANALYZE)
			    + "    ," + tMbExpr  + " as [bloat_mb] \n"
			    + "    ," + pctExpr  + " as [bloat_pct] \n"
			    + "    ,m.[bloat_mb__max] \n"
			    + (_hasSource ? ""
			    + "    ,t.[bloat_source] \n"
			    + "    ,t.[approx_dead_pct] \n"
			    + "    ,t.[approx_free_pct] \n"
			    + "    ,t.[approx_scanned_pct] \n" : "")
			    + "    ,t.[fillfactor] \n"
			    + "    ,t.[SessionSampleTime] as [last_sample] \n"
			    + "from [CmPgTableBloat_abs] t \n"
			    + "join (select [dbname], [schemaname], [tablename], max([SessionSampleTime]) as [last_ts], max(" + mbExpr + ") as [bloat_mb__max] \n"
			    + "      from [CmPgTableBloat_abs] \n"
			    + "      " + where + " \n"
			    + "      group by [dbname], [schemaname], [tablename] \n"
			    + "     ) m on m.[dbname] = t.[dbname] and m.[schemaname] = t.[schemaname] and m.[tablename] = t.[tablename] and m.[last_ts] = t.[SessionSampleTime] \n"
			    + "where " + tMbExpr + " > " + getMinBloatMb() + " \n"
			    + "order by " + tMbExpr + " desc \n"
			    + "";

		// How many tables had no bloat number (neither measured nor estimated)
		String cntSql = conn.quotifySqlString("select count(distinct [dbname] || '.' || [schemaname] || '.' || [tablename]), count(distinct case when (" + mbExpr + ") is null then [dbname] || '.' || [schemaname] || '.' || [tablename] end) from [CmPgTableBloat_abs] " + where);
		try (java.sql.Statement stmnt = conn.createStatement(); java.sql.ResultSet rs = stmnt.executeQuery(cntSql))
		{
			if (rs.next())
			{
				_tableCount = rs.getInt(1);
				_naCount    = rs.getInt(2);
			}
		}
		catch (java.sql.SQLException ex)
		{
			_tableCount = _naCount = -1;
		}

		_shortRstm = executeQuery(conn, sql, true, "PostgresTableBloat");
		_shortRstm.setHighlightSortColumns("bloat_mb");

		_shortRstm.setDescription(
				"The tables with the most <b>bloat</b>: space in the table that holds no live rows (from the table <code>CmPgTableBloat_abs</code>, each table at its last sample).<br>" +
				"<code>bloat_source</code> tells how it was found: <b>pgstattuple_approx</b> = <i>measured</i> (the extension pgstattuple in the database; tables of " +
				Configuration.getCombinedConfiguration().getLongProperty("CmPgTableBloat.approx.minTableMb", 100) + " MB or more), " +
				"<b>pg_stats</b> = <i>estimated</i> from the planner statistics (row width and row count: run ANALYZE first if they are old).<br>" +
				"Bloat comes from updates and deletes that vacuum did not clean up in time. Vacuum makes the space reusable, but does not give it back to the OS.<br>" +
				"<b>What to do</b>:" +
				"<ul>" +
				"  <li>First fix <i>why</i>: the autovacuum advice in 'Dead Rows/Tuples', and what held vacuum back in 'Vacuum Health'. Otherwise the bloat comes back.</li>" +
				"  <li>To get the space back: <code>VACUUM FULL schema.table;</code> rewrites the table, but locks it (no reads or writes) while it runs, and needs space for a copy. " +
				"      <code>pg_repack</code> (an extension) does the same with a short lock only.</li>" +
				"  <li>Some free space is fine (and wanted with a fillfactor below 100: room for updates on the same page).</li>" +
				"</ul>" +
				"");
		_shortRstm.setColumnDescription("dbname"            , "The database");
		_shortRstm.setColumnDescription("schemaname"        , "The schema");
		_shortRstm.setColumnDescription("tablename"         , "The table");
		_shortRstm.setColumnDescription("table_mb"          , "The size of the table, in MB: measured by pgstattuple_approx when the bloat was measured, else from pg_class.relpages (only updated by VACUUM and ANALYZE: too small on a growing table)");
		_shortRstm.setColumnDescription("bloat_mb"          , "Space without live rows, in MB, at the last sample (measured or estimated: see bloat_source). The free space a fillfactor below 100 keeps on purpose is not counted.");
		_shortRstm.setColumnDescription("bloat_pct"         , "Bloat in percent of the table size");
		_shortRstm.setColumnDescription("bloat_mb__max"     , "The most bloat during the period, in MB");
		_shortRstm.setColumnDescription("bloat_source"      , "pgstattuple_approx = measured, pg_stats = estimated from the statistics");
		_shortRstm.setColumnDescription("approx_dead_pct"   , "Measured: dead rows in percent of the table (vacuum removes them)");
		_shortRstm.setColumnDescription("approx_free_pct"   , "Measured: free space in percent of the table (reusable, but not given back to the OS)");
		_shortRstm.setColumnDescription("approx_scanned_pct", "How much of the table pgstattuple_approx had to read (the pages vacuum has not marked all-visible)");
		_shortRstm.setColumnDescription("fillfactor"        , "The table's fillfactor (free space left on purpose in each page)");
		_shortRstm.setColumnDescription("last_sample"       , "When the table was last sampled (CmPgTableBloat samples every 10 minutes)");
	}
}
