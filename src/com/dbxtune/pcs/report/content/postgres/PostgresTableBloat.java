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
	private int _naCount    = -1;           // of them without an estimate (is_na)

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
			naText = "<b>" + _naCount + " of " + _tableCount + " tables had no bloat estimate</b> (is_na): some of their columns have no statistics in <code>pg_stats</code>. "
					+ (_naCount >= _tableCount * 0.9 // (nearly) all: a table whose columns the login may read (or with no columns) still gets an estimate
						? "When it is (nearly) <i>all</i> tables, the login can't see the statistics: pg_stats only shows the columns the login may SELECT, and a <code>pg_monitor</code> login may not. "
						+ "Fix: <code>GRANT pg_read_all_data TO &lt;user&gt;;</code> (Postgres 14 and above, read access to all data), or a login with SELECT on the tables. "
						: "Run ANALYZE on them (or the columns have a type without statistics). ")
					+ "Tables without an estimate are not listed (the estimate would be close to 100% for all of them).<br>\n";
		}

		if (_shortRstm.getRowCount() == 0)
		{
			w.append(naText);
			if (_naCount < _tableCount * 0.9 || _tableCount <= 0)
				w.append("<b>No table had more than " + getMinBloatMb() + " MB of estimated bloat</b> (property <code>" + getClass().getSimpleName() + ".minBloatMb</code>).<br>\n");
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
		return "Table Bloat, estimated (order by: bloat_size_mb, origin: CmPgTableBloat)";
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

		// Each table at its last sample in the period (the CM samples every 10 minutes)
		String sql = ""
			    + "select top " + getTopRows() + " \n"
			    + "     t.[dbname] \n"
			    + "    ,t.[schemaname] \n"
			    + "    ,t.[tablename] \n"
			    + "    ,t.[real_size_mb] \n"
			    + "    ,t.[bloat_size_mb] \n"
			    + "    ,t.[bloat_pct] \n"
			    + "    ,m.[bloat_size_mb__max] \n"
			    + "    ,t.[fillfactor] \n"
			    + "    ,t.[is_na] \n"
			    + "    ,t.[SessionSampleTime] as [last_sample] \n"
			    + "from [CmPgTableBloat_abs] t \n"
			    + "join (select [dbname], [schemaname], [tablename], max([SessionSampleTime]) as [last_ts], max([bloat_size_mb]) as [bloat_size_mb__max] \n"
			    + "      from [CmPgTableBloat_abs] \n"
			    + "      " + where + " \n"
			    + "      group by [dbname], [schemaname], [tablename] \n"
			    + "     ) m on m.[dbname] = t.[dbname] and m.[schemaname] = t.[schemaname] and m.[tablename] = t.[tablename] and m.[last_ts] = t.[SessionSampleTime] \n"
			    + "where t.[bloat_size_mb] > " + getMinBloatMb() + " \n"
			    + "  and t.[is_na] = false \n" // no estimate (some columns without statistics): bloat_pct is close to 100 and meaningless
			    + "order by t.[bloat_size_mb] desc \n"
			    + "";

		// How many tables had no estimate (with a 'pg_monitor' login: all of them, pg_stats only shows the columns the login may SELECT)
		String cntSql = conn.quotifySqlString("select count(distinct [dbname] || '.' || [schemaname] || '.' || [tablename]), count(distinct case when [is_na] = true then [dbname] || '.' || [schemaname] || '.' || [tablename] end) from [CmPgTableBloat_abs] " + where);
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
		_shortRstm.setHighlightSortColumns("bloat_size_mb");

		_shortRstm.setDescription(
				"The tables with the most <b>bloat</b>: space in the table that holds no live rows (from the table <code>CmPgTableBloat_abs</code>, each table at its last sample).<br>" +
				"It is an <b>estimate</b> from the planner statistics (row width and row count), not a measurement: <code>is_na</code> = true means no estimate could be made (for example a column type without statistics). " +
				"Run ANALYZE first if the statistics are old.<br>" +
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
		_shortRstm.setColumnDescription("real_size_mb"      , "The size of the table, in MB");
		_shortRstm.setColumnDescription("bloat_size_mb"     , "Estimated space without live rows, in MB, at the last sample");
		_shortRstm.setColumnDescription("bloat_pct"         , "Estimated bloat in percent of the table size");
		_shortRstm.setColumnDescription("bloat_size_mb__max", "The most estimated bloat during the period, in MB");
		_shortRstm.setColumnDescription("fillfactor"        , "The table's fillfactor (free space left on purpose in each page)");
		_shortRstm.setColumnDescription("is_na"             , "true = the estimate could not be made for this table");
		_shortRstm.setColumnDescription("last_sample"       , "When the table was last sampled (CmPgTableBloat samples every 10 minutes)");
	}
}
