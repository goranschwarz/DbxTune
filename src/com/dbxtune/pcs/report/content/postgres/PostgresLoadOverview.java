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

import com.dbxtune.cm.postgres.CmPgDatabase;
import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.pcs.report.content.IReportChart;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;

/**
 * How much work the server did during the day: commits, rollbacks, rows written, temp files, connections, cache hit (from CmPgDatabase).
 * <p>
 * The day in one row, per hour, and the charts. Built on MySqlLoadOverview. The oldest transaction, blocking locks and deadlocks charts are in 'Long Running Statements'.
 */
public class PostgresLoadOverview
extends PostgresAbstract
{
	private ResultSetTableModel _dayRstm;  // null = not recorded
	private ResultSetTableModel _hourRstm; // null = not recorded

	public PostgresLoadOverview(DailySummaryReportAbstract reportingInstance)
	{
		super(reportingInstance);
	}

	@Override
	public boolean hasMinimalMessageText()
	{
		return true;
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
		if (_dayRstm == null)
		{
			w.append("<b>The database statistics were not recorded</b> (the table <code>CmPgDatabase_diff</code> does not exist).<br>\n");
			return;
		}

		w.append(getSectionDescriptionHtml(_dayRstm, true));
		w.append(toHtmlTable(_dayRstm));

		w.append(getDbxCentralLinkWithDescForGraphs(false, "Below are Graphs/Charts that shows how much work the server did during the day.",
				"CmPgDatabase_" + CmPgDatabase.GRAPH_NAME_CONNECTIONS_SUM,
				"CmPgDatabase_" + CmPgDatabase.GRAPH_NAME_COMMITS,
				"CmPgDatabase_" + CmPgDatabase.GRAPH_NAME_ROLLBACKS,
				"CmPgDatabase_" + CmPgDatabase.GRAPH_NAME_ROWS_INS_UPD_DEL,
				"CmPgDatabase_" + CmPgDatabase.GRAPH_NAME_CACHE_HIT_PCT,
				"CmPgDatabase_" + CmPgDatabase.GRAPH_NAME_TEMP_BYTES,
				"CmPgDatabase_" + CmPgDatabase.GRAPH_NAME_TOTAL_READS
				));

		write(w, _CmPgDatabase_ConnectionsSum, "<b>Note</b>: Compare with max_connections. Many idle connections cost memory: a connection pooler (pgBouncer) can help.");
		write(w, _CmPgDatabase_Commits, null);

		// The minimal message (a mail that otherwise gets too big) stops here
		if (isMinimalMessageType())
			return;

		if (_hourRstm != null && _hourRstm.getRowCount() > 0)
		{
			w.append("<br>\n");
			w.append(getSectionDescriptionHtml(_hourRstm, true));
			w.append(toHtmlTable(_hourRstm));
		}

		write(w, _CmPgDatabase_Rollbacks, "<b>Note</b>: Rollbacks are often errors in the application (or a framework that rolls back read-only transactions). See the 'Error Log' section, if it was recorded.");
		write(w, _CmPgDatabase_InsUpDel, null);
		write(w, _CmPgDatabase_CacheHitPct, "<b>Note</b>: Below about 99% many reads have to go to the OS cache or the disk: shared_buffers may be too small for the data that is used.");
		write(w, _CmPgDatabase_TempBytes, "<b>Note</b>: Temp files are sorts and hashes that did not fit in work_mem. The statements are in Top SQL (temp_blks_written).");

		if (isFullMessageType())
			write(w, _CmPgDatabase_TotalReads, null);
	}

	private void write(Writer w, IReportChart chart, String note)
	throws IOException
	{
		if (chart != null)
			chart.writeHtmlContent(w, null, note);
	}

	@Override
	public String getSubject()
	{
		return "Load Overview of the full day (origin: CmPgDatabase / pg_stat_database)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		String schema = getReportingInstance().getDbmsSchemaName();
		String where  = "where 1 = 1 " + getReportPeriodSqlWhere().trim();

		Set<String> cols = getRecordedColumns(conn, "CmPgDatabase_diff");
		if ( ! cols.contains("xact_commit") )
			return;

		boolean hasSessions = cols.contains("sessions_fatal"); // 14+
		long maxConnections = getServerSettingLong(conn, "max_connections", -1);

		//-------------------------------------------------------------
		// The day in one row (all databases)
		//-------------------------------------------------------------
		String sql = ""
			    + "select \n"
			    + "     d.[commits] \n"
			    + "    ,cast(d.[commits] * 1.0 / nullif(d.[seconds], 0) as numeric(12,1)) as [commits_per_sec] \n"
			    + "    ,d.[rollbacks] \n"
			    + "    ,cast(100.0 * d.[rollbacks] / nullif(d.[commits] + d.[rollbacks], 0) as numeric(5,1)) as [rollback_pct] \n"
			    + "    ,d.[rows_inserted] \n"
			    + "    ,d.[rows_updated] \n"
			    + "    ,d.[rows_deleted] \n"
			    + "    ,d.[temp_mb] \n"
			    + "    ,d.[deadlocks] \n"
			    + "    ,d.[cache_hit_pct] \n"
			    + "    ,d.[disk_reads_mb] \n"
			    + (hasSessions ? ""
			    + "    ,d.[sessions] \n"
			    + "    ,d.[sessions_fatal] \n"
			    + "    ,d.[sessions_killed] \n"
			    + "    ,d.[sessions_abandoned] \n" : "")
			    + "    ,(select max(x.[conns]) from (select sum([numbackends]) as [conns] from [CmPgDatabase_abs] " + where + " group by [SessionSampleTime]) x) as [connections__max] \n"
			    + "    ," + maxConnections + " as [max_connections] \n"
			    + "    ,d.[hours] \n"
			    + "from (select \n"
			    + "           sum([xact_commit])   as [commits] \n"
			    + "          ,sum([xact_rollback]) as [rollbacks] \n"
			    + "          ,sum([tup_inserted])  as [rows_inserted] \n"
			    + "          ,sum([tup_updated])   as [rows_updated] \n"
			    + "          ,sum([tup_deleted])   as [rows_deleted] \n"
			    + "          ,cast(sum([temp_bytes]) / 1048576.0 as numeric(15,1)) as [temp_mb] \n"
			    + "          ,sum([deadlocks])     as [deadlocks] \n"
			    + "          ,cast(100.0 * sum([blks_hit]) / nullif(sum([blks_hit]) + sum([blks_read]), 0) as numeric(5,2)) as [cache_hit_pct] \n"
			    + "          ,cast(sum([blks_read]) / 128.0 as numeric(15,1)) as [disk_reads_mb] \n"
			    + (hasSessions ? ""
			    + "          ,sum([sessions])           as [sessions] \n"
			    + "          ,sum([sessions_fatal])     as [sessions_fatal] \n"
			    + "          ,sum([sessions_killed])    as [sessions_killed] \n"
			    + "          ,sum([sessions_abandoned]) as [sessions_abandoned] \n" : "")
			    + "          ,datediff('SECOND', min([SessionSampleTime]), max([SessionSampleTime])) as [seconds] \n"
			    + "          ,cast(datediff('SECOND', min([SessionSampleTime]), max([SessionSampleTime])) / 3600.0 as numeric(6,1)) as [hours] \n"
			    + "      from [CmPgDatabase_diff] \n"
			    + "      " + where + " \n"
			    + "     ) d \n"
			    + "";

		_dayRstm = executeQuery(conn, sql, true, "PostgresLoadOverviewDay");
		_dayRstm.setDescription(
				"The work of the whole server during the period, all databases together (from the tables <code>CmPgDatabase_diff</code> and <code>_abs</code>).<br>" +
				"Look for: a high <code>rollback_pct</code> (errors in the application), <code>temp_mb</code> (sorts and hashes that did not fit in work_mem), " +
				"<code>cache_hit_pct</code> below about 99, <code>connections__max</code> close to <code>max_connections</code>, " +
				"and <code>sessions_fatal</code> / <code>sessions_killed</code> / <code>sessions_abandoned</code> (failed logins, terminated sessions, clients that disappeared).<br>" +
				"");
		_dayRstm.setColumnDescription("commits"           , "Committed transactions");
		_dayRstm.setColumnDescription("commits_per_sec"   , "Committed transactions per second, on average over the period");
		_dayRstm.setColumnDescription("rollbacks"         , "Rolled back transactions (also the ones that failed with an error)");
		_dayRstm.setColumnDescription("rollback_pct"      , "Rollbacks in percent of all transactions");
		_dayRstm.setColumnDescription("rows_inserted"     , "Rows inserted");
		_dayRstm.setColumnDescription("rows_updated"      , "Rows updated");
		_dayRstm.setColumnDescription("rows_deleted"      , "Rows deleted");
		_dayRstm.setColumnDescription("temp_mb"           , "MB written to temp files (sorts and hashes that did not fit in work_mem)");
		_dayRstm.setColumnDescription("deadlocks"         , "Deadlocks");
		_dayRstm.setColumnDescription("cache_hit_pct"     , "Blocks found in shared_buffers, in percent of all block reads");
		_dayRstm.setColumnDescription("disk_reads_mb"     , "MB read outside shared_buffers (from the OS cache or the disk)");
		_dayRstm.setColumnDescription("sessions"          , "Sessions (logins) started (Postgres 14 and above)");
		_dayRstm.setColumnDescription("sessions_fatal"    , "Sessions that ended with a FATAL error, for example a failed login (Postgres 14 and above)");
		_dayRstm.setColumnDescription("sessions_killed"   , "Sessions that were terminated, for example with pg_terminate_backend() (Postgres 14 and above)");
		_dayRstm.setColumnDescription("sessions_abandoned", "Sessions where the client disconnected without logging out (Postgres 14 and above)");
		_dayRstm.setColumnDescription("connections__max"  , "The most connections at the same time (all databases)");
		_dayRstm.setColumnDescription("max_connections"   , "The setting max_connections (-1 = not known)");
		_dayRstm.setColumnDescription("hours"             , "The length of the period, in hours");

		//-------------------------------------------------------------
		// Per hour
		//-------------------------------------------------------------
		String hour = "formatdatetime([SessionSampleTime], 'yyyy-MM-dd HH:00')";
		sql = ""
			    + "select \n"
			    + "     h.[hour] \n"
			    + "    ,cast(h.[commits] * 1.0 / nullif(h.[seconds], 0) as numeric(12,1)) as [commits_per_sec] \n"
			    + "    ,cast(100.0 * h.[rollbacks] / nullif(h.[commits] + h.[rollbacks], 0) as numeric(5,1)) as [rollback_pct] \n"
			    + "    ,h.[rows_written] \n"
			    + "    ,h.[temp_mb] \n"
			    + "    ,h.[cache_hit_pct] \n"
			    + "    ,c.[connections__max] \n"
			    + "from (select \n"
			    + "           " + hour + " as [hour] \n"
			    + "          ,sum([xact_commit])   as [commits] \n"
			    + "          ,sum([xact_rollback]) as [rollbacks] \n"
			    + "          ,sum([tup_inserted] + [tup_updated] + [tup_deleted]) as [rows_written] \n"
			    + "          ,cast(sum([temp_bytes]) / 1048576.0 as numeric(15,1)) as [temp_mb] \n"
			    + "          ,cast(100.0 * sum([blks_hit]) / nullif(sum([blks_hit]) + sum([blks_read]), 0) as numeric(5,2)) as [cache_hit_pct] \n"
			    + "          ,sum([CmSampleMs]) / 1000.0 / nullif(count(distinct [datname]), 0) as [seconds] \n"
			    + "      from [CmPgDatabase_diff] \n"
			    + "      " + where + " \n"
			    + "      group by " + hour + " \n"
			    + "     ) h \n"
			    // The most connections at the same time in each hour: the sum over the databases per sample, then the max per hour
			    + "left join (select x.[hour], max(x.[conns]) as [connections__max] \n"
			    + "           from (select " + hour + " as [hour], sum([numbackends]) as [conns] from [CmPgDatabase_abs] " + where + " group by [SessionSampleTime]) x \n"
			    + "           group by x.[hour] \n"
			    + "          ) c on c.[hour] = h.[hour] \n"
			    + "order by h.[hour] \n"
			    + "";

		_hourRstm = executeQuery(conn, sql, true, "PostgresLoadOverviewHour");
		_hourRstm.setDescription("The same per hour: when during the day was the server busy.<br>");
		_hourRstm.setColumnDescription("hour"            , "The hour");
		_hourRstm.setColumnDescription("commits_per_sec" , "Committed transactions per second, on average in the hour");
		_hourRstm.setColumnDescription("rollback_pct"    , "Rollbacks in percent of all transactions");
		_hourRstm.setColumnDescription("rows_written"    , "Rows inserted + updated + deleted");
		_hourRstm.setColumnDescription("temp_mb"         , "MB written to temp files");
		_hourRstm.setColumnDescription("cache_hit_pct"   , "Blocks found in shared_buffers, in percent");
		_hourRstm.setColumnDescription("connections__max", "The most connections at the same time");

		//-------------------------------------------------------------
		// Charts
		//-------------------------------------------------------------
		_CmPgDatabase_ConnectionsSum = chart(conn, schema, CmPgDatabase.GRAPH_NAME_CONNECTIONS_SUM, -1,  "Connections, all Databases (" + CmPgDatabase.SHORT_NAME + ")");
		_CmPgDatabase_Commits        = chart(conn, schema, CmPgDatabase.GRAPH_NAME_COMMITS,         -1,  "Commits per Second, per Database (" + CmPgDatabase.SHORT_NAME + ")");
		_CmPgDatabase_Rollbacks      = chart(conn, schema, CmPgDatabase.GRAPH_NAME_ROLLBACKS,       -1,  "Rollbacks per Second, per Database (" + CmPgDatabase.SHORT_NAME + ")");
		_CmPgDatabase_InsUpDel       = chart(conn, schema, CmPgDatabase.GRAPH_NAME_ROWS_INS_UPD_DEL, -1, "Rows Inserted, Updated and Deleted per Second (" + CmPgDatabase.SHORT_NAME + ")");
		_CmPgDatabase_CacheHitPct    = chart(conn, schema, CmPgDatabase.GRAPH_NAME_CACHE_HIT_PCT,   100, "Cache Hit in Percent, per Database (" + CmPgDatabase.SHORT_NAME + ")");
		_CmPgDatabase_TempBytes      = chart(conn, schema, CmPgDatabase.GRAPH_NAME_TEMP_BYTES,      -1,  "Temp Files Written, per Database (" + CmPgDatabase.SHORT_NAME + ")");
		_CmPgDatabase_TotalReads     = chart(conn, schema, CmPgDatabase.GRAPH_NAME_TOTAL_READS,     -1,  "Block Reads per Second, all (" + CmPgDatabase.SHORT_NAME + ")");
	}

	/** A line chart, or null when the graph was not recorded */
	private IReportChart chart(DbxConnection conn, String schema, String graphName, int maxValue, String title)
	{
		if ( ! doTableExist(conn, schema, CmPgDatabase.CM_NAME + "_" + graphName) )
			return null;
		return createTsLineChart(conn, schema, CmPgDatabase.CM_NAME, graphName, maxValue, false, null, title);
	}

	private IReportChart _CmPgDatabase_ConnectionsSum;
	private IReportChart _CmPgDatabase_Commits;
	private IReportChart _CmPgDatabase_Rollbacks;
	private IReportChart _CmPgDatabase_InsUpDel;
	private IReportChart _CmPgDatabase_CacheHitPct;
	private IReportChart _CmPgDatabase_TempBytes;
	private IReportChart _CmPgDatabase_TotalReads;
}
