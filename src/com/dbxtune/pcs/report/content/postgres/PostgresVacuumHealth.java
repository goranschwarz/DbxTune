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

import org.apache.commons.lang3.StringUtils;

import com.dbxtune.cm.postgres.CmPgDatabase;
import com.dbxtune.cm.postgres.CmSummary;
import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.pcs.report.content.IReportChart;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;

/**
 * Transaction ID wraparound: how old the databases and tables are (in transaction ids), what kept vacuum from freezing,
 * and (Postgres 19) the tables that autovacuum did not get to in time.
 * <p>
 * From CmPgDatabase (datfrozenxid_age, datminmxid_age), CmSummary (the oldest xmin holders), CmPgTables (frozenxid_age) and CmPgAutovacuumScores (19).
 */
public class PostgresVacuumHealth
extends PostgresAbstract
{
	/** Postgres stops assigning transaction ids (writes fail) about 3 million before 2^31 */
	private static final long XID_LIMIT = 2147483648L;

	private ResultSetTableModel _dbRstm;      // null = not recorded (an older recording)
	private ResultSetTableModel _holdersRstm; // null = not recorded
	private ResultSetTableModel _tablesRstm;  // null = not recorded
	private ResultSetTableModel _scoresRstm;  // null = not recorded (before 19)

	private String _holderVerdict;            // null = nothing to say
	private long   _maxDbAge = -1;
	private long   _freezeMaxAge;
	private long   _mxidFreezeMaxAge;

	public PostgresVacuumHealth(DailySummaryReportAbstract reportingInstance)
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
		//----- databases
		if (_dbRstm == null)
		{
			w.append("<b>The transaction id age of the databases was not recorded</b> (a recording from an older PostgresTune).<br>\n");
		}
		else
		{
			w.append(getSectionDescriptionHtml(_dbRstm, true));
			w.append(toHtmlTable(_dbRstm));

			if (_CmPgDatabase_XidAge != null)
			{
				w.append(getDbxCentralLinkWithDescForGraphs(false, "Below is the transaction id age of each database during the day.",
						"CmPgDatabase_XidAge"
						));
				_CmPgDatabase_XidAge.writeHtmlContent(w, null, "<b>Note</b>: At about 2.1 billion Postgres stops accepting writes. Anti-wraparound autovacuum starts at autovacuum_freeze_max_age (" + String.format("%,d", _freezeMaxAge) + ").");
			}
		}

		//----- what held vacuum back
		if (_holdersRstm != null)
		{
			w.append("<br>\n");
			w.append(getSectionDescriptionHtml(_holdersRstm, true));
			if (_holdersRstm.getRowCount() == 0)
				w.append("Nothing held back vacuum during the period (no old snapshots, prepared transactions, slots or standby feedback).<br>\n");
			else
				w.append(toHtmlTable(_holdersRstm));
			if (_holderVerdict != null)
				w.append("<p><b>Verdict</b>: " + _holderVerdict + "</p>\n");

			if (isFullMessageType() && _CmSummary_maxXid != null)
				_CmSummary_maxXid.writeHtmlContent(w, null, null);
		}

		//----- the oldest tables
		if (_tablesRstm != null && _tablesRstm.getRowCount() > 0)
		{
			w.append("<br>\n");
			w.append(getSectionDescriptionHtml(_tablesRstm, true));
			w.append("Row Count: " + _tablesRstm.getRowCount() + "&emsp;&emsp; To change number of <i>top</i> records, set property <code>" + getTopRowsPropertyName() + "=##</code><br>\n");
			w.append(toHtmlTable(_tablesRstm));
		}

		//----- 19: tables autovacuum did not get to
		if (_scoresRstm != null)
		{
			w.append("<br>\n");
			w.append(getSectionDescriptionHtml(_scoresRstm, true));
			if (_scoresRstm.getRowCount() == 0)
				w.append("No table had an autovacuum score of 1.0 or more (autovacuum kept up with all tables).<br>\n");
			else
				w.append(toHtmlTable(_scoresRstm));
		}
	}

	@Override
	public String getSubject()
	{
		return "Vacuum Health: Transaction ID Wraparound (origin: CmPgDatabase, CmSummary, CmPgTables, CmPgAutovacuumScores)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		// As the XidWraparound alarm (default 1 billion)
		long threshold = Configuration.getCombinedConfiguration().getLongProperty(CmPgDatabase.PROPKEY_alarm_XidWraparound, CmPgDatabase.DEFAULT_alarm_XidWraparound);
		return _maxDbAge > threshold;
	}

	@Override
	public int getTopRowsDefault()
	{
		return 20;
	}

	/** A holder of the oldest xmin is named in the verdict when it is at least this old (in transaction ids) */
	private long getHolderMinAge()
	{
		return Configuration.getCombinedConfiguration().getLongProperty(getClass().getSimpleName() + ".holder.minAge", 1_000_000);
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		String schema = getReportingInstance().getDbmsSchemaName();
		String where  = "where 1 = 1 " + getReportPeriodSqlWhere().trim();

		_freezeMaxAge     = getServerSettingLong(conn, "autovacuum_freeze_max_age",           200_000_000);
		_mxidFreezeMaxAge = getServerSettingLong(conn, "autovacuum_multixact_freeze_max_age", 400_000_000);

		//-------------------------------------------------------------
		// Per database: the age at the last sample, and the highest of the period
		//-------------------------------------------------------------
		Set<String> dbCols = getRecordedColumns(conn, "CmPgDatabase_abs");
		if (dbCols.contains("datfrozenxid_age"))
		{
			String sql = ""
				    + "select \n"
				    + "     l.[datname] \n"
				    + "    ,l.[datfrozenxid_age] \n"
				    + "    ,m.[datfrozenxid_age__max] \n"
				    + "    ,cast(100.0 * m.[datfrozenxid_age__max] / " + XID_LIMIT    + " as numeric(6,1)) as [xid_pct_of_limit] \n"
				    + "    ,cast(100.0 * m.[datfrozenxid_age__max] / " + _freezeMaxAge + " as numeric(8,1)) as [xid_pct_of_freeze_max_age] \n"
				    + "    ,l.[datminmxid_age] \n"
				    + "    ,m.[datminmxid_age__max] \n"
				    + "    ,cast(100.0 * m.[datminmxid_age__max] / " + XID_LIMIT        + " as numeric(6,1)) as [mxid_pct_of_limit] \n"
				    + "    ,cast(100.0 * m.[datminmxid_age__max] / " + _mxidFreezeMaxAge + " as numeric(8,1)) as [mxid_pct_of_freeze_max_age] \n"
				    + "from [CmPgDatabase_abs] l \n"
				    + "join (select \n"
				    + "           [datname] \n"
				    + "          ,max([datfrozenxid_age]) as [datfrozenxid_age__max] \n"
				    + "          ,max([datminmxid_age])   as [datminmxid_age__max] \n"
				    + "      from [CmPgDatabase_abs] \n"
				    + "      " + where + " \n"
				    + "      group by [datname] \n"
				    + "     ) m on m.[datname] = l.[datname] \n"
				    + "where l.[SessionSampleTime] = (select max([SessionSampleTime]) from [CmPgDatabase_abs] " + where + ") \n"
				    + "order by m.[datfrozenxid_age__max] desc \n"
				    + "";

			_dbRstm = executeQuery(conn, sql, true, "PostgresVacuumHealthDb");
			_dbRstm.setHighlightSortColumns("datfrozenxid_age__max");

			for (int r = 0; r < _dbRstm.getRowCount(); r++)
				_maxDbAge = Math.max(_maxDbAge, _dbRstm.getValueAsLong(r, "datfrozenxid_age__max", true, -1L));

			_dbRstm.setDescription(
					"How old each database is, in transaction ids: the age of its oldest unfrozen row (from the table <code>CmPgDatabase_abs</code>, the last collector sample and the highest of the period).<br>" +
					"Transaction ids wrap around after 2^31 (about 2.1 billion). Before that, Postgres stops accepting writes, so <b>vacuum must freeze old rows</b>. " +
					"Autovacuum forces an (anti-wraparound) vacuum on a table when it is older than <code>autovacuum_freeze_max_age</code> (now " + String.format("%,d", _freezeMaxAge) + ").<br>" +
					"Look for: <code>xid_pct_of_freeze_max_age</code> far above 100 (the forced vacuums do not finish, or something holds them back, see below), and <code>xid_pct_of_limit</code> above 50.<br>" +
					"");

			_dbRstm.setColumnDescription("datname"                   , "The database");
			_dbRstm.setColumnDescription("datfrozenxid_age"          , "age(datfrozenxid) at the last sample: the oldest unfrozen transaction id in the database, as an age");
			_dbRstm.setColumnDescription("datfrozenxid_age__max"     , "The highest age during the period");
			_dbRstm.setColumnDescription("xid_pct_of_limit"          , "The highest age in percent of 2^31 (about 2.1 billion): at 100 the database stops accepting writes");
			_dbRstm.setColumnDescription("xid_pct_of_freeze_max_age" , "The highest age in percent of autovacuum_freeze_max_age: above 100 anti-wraparound vacuums are (or should be) running");
			_dbRstm.setColumnDescription("datminmxid_age"            , "mxid_age(datminmxid) at the last sample: the same for multixact ids (row locks shared by several transactions)");
			_dbRstm.setColumnDescription("datminmxid_age__max"       , "The highest multixact age during the period");
			_dbRstm.setColumnDescription("mxid_pct_of_limit"         , "The highest multixact age in percent of 2^31");
			_dbRstm.setColumnDescription("mxid_pct_of_freeze_max_age", "The highest multixact age in percent of autovacuum_multixact_freeze_max_age");

			if (doTableExist(conn, schema, CmPgDatabase.CM_NAME + "_" + CmPgDatabase.GRAPH_NAME_XID_AGE))
				_CmPgDatabase_XidAge = createTsLineChart(conn, schema, CmPgDatabase.CM_NAME, CmPgDatabase.GRAPH_NAME_XID_AGE, -1, true, null, "Transaction ID Age per Database (" + CmPgDatabase.SHORT_NAME + ")");
		}

		//-------------------------------------------------------------
		// What held vacuum back: the oldest xmin of the server, by holder (CmSummary)
		// Vacuum can't remove (or freeze) rows that are newer than the oldest xmin, whoever holds it
		//-------------------------------------------------------------
		Set<String> sumCols = getRecordedColumns(conn, "CmSummary_abs");
		String[][] holders = new String[][] {
			{ "oldest_backend_xmin_age"    , "A session (an open transaction, or a long query) keeps an old snapshot",                         "Find it in 'Long Running Statements' / pg_stat_activity (backend_xmin, xact_start). Commit or end it; set idle_in_transaction_session_timeout." },
			{ "oldest_prepared_xact_age"   , "A prepared transaction (two-phase commit) that was never committed or rolled back",                "SELECT * FROM pg_prepared_xacts; then COMMIT PREPARED / ROLLBACK PREPARED 'gid'." },
			{ "oldest_replication_slot_age", "A replication slot (xmin / catalog_xmin): its consumer is behind or gone",                          "See the 'Replication / HA' section; drop a slot that is not used: SELECT pg_drop_replication_slot('name');" },
			{ "oldest_replica_xact_age"    , "A standby with hot_standby_feedback = on runs a long query (the primary keeps the rows it reads)", "Find the long query on the standby; or limit it with max_standby_streaming_delay / turn off hot_standby_feedback." },
		};
		StringBuilder union = new StringBuilder();
		for (String[] h : holders)
		{
			if ( ! sumCols.contains(h[0].toLowerCase()) )
				continue;
			if (union.length() > 0)
				union.append("union all \n");
			union.append("select '" + h[0] + "' as [holder] \n")
			     .append("      ,max([" + h[0] + "]) as [age__max] \n")
			     .append("      ,(select min([SessionSampleTime]) from [CmSummary_abs] x " + where + " and x.[" + h[0] + "] = (select max([" + h[0] + "]) from [CmSummary_abs] " + where + ")) as [age__max_time] \n")
			     .append("      ,sum(case when [" + h[0] + "] > 0 then 1 else 0 end) as [samples_held] \n")
			     .append("      ,count(*) as [samples_count] \n")
			     .append("from [CmSummary_abs] " + where + " \n");
		}
		if (union.length() > 0)
		{
			// Only the holders that held something (CmSummary has -1 = nothing)
			String sql = "select * from ( \n" + union + ") x \nwhere [samples_held] > 0 \norder by [age__max] desc \n";
			_holdersRstm = executeQuery(conn, sql, true, "PostgresVacuumHealthHolders");

			_holdersRstm.setDescription(
					"What kept vacuum from removing dead rows and freezing old ones: the oldest transaction id that is still needed, by who holds it (from the table <code>CmSummary_abs</code>).<br>" +
					"Vacuum can't clean up rows newer than this, whoever holds it. A big age here means dead rows pile up (bloat), and the database age above can't go down.<br>" +
					"");
			_holdersRstm.setColumnDescription("holder"       , "Who held the oldest transaction id: a session, a prepared transaction, a replication slot, or a standby (hot_standby_feedback)");
			_holdersRstm.setColumnDescription("age__max"     , "The highest age (in transaction ids) during the period");
			_holdersRstm.setColumnDescription("age__max_time", "When it was the highest");
			_holdersRstm.setColumnDescription("samples_held" , "In how many collector samples it held something");
			_holdersRstm.setColumnDescription("samples_count", "Number of collector samples in the period");

			// The verdict: the biggest holder, when it is old enough to matter
			long   bestAge = -1;
			String bestTxt = null;
			for (int r = 0; r < _holdersRstm.getRowCount(); r++)
			{
				String holder = _holdersRstm.getValueAsString(r, "holder");
				long   age    = _holdersRstm.getValueAsLong(r, "age__max", true, -1L);
				for (String[] h : holders)
				{
					if (h[0].equals(holder) && age > bestAge)
					{
						bestAge = age;
						bestTxt = h[1] + " (up to " + String.format("%,d", age) + " transaction ids old, at " + StringUtils.left(_holdersRstm.getValueAsString(r, "age__max_time"), 19) + "). <i>Fix</i>: " + h[2];
					}
				}
			}
			if (bestAge >= getHolderMinAge())
				_holderVerdict = bestTxt;

			if (doTableExist(conn, schema, CmSummary.CM_NAME + "_" + CmSummary.GRAPH_NAME_MAX_XID))
				_CmSummary_maxXid = createTsLineChart(conn, schema, CmSummary.CM_NAME, CmSummary.GRAPH_NAME_MAX_XID, -1, false, null, "Oldest Transaction ID Age, by Holder (" + CmSummary.SHORT_NAME + ")");
		}

		//-------------------------------------------------------------
		// The oldest tables (CmPgTables, at the last sample of each table)
		//-------------------------------------------------------------
		Set<String> tabCols = getRecordedColumns(conn, "CmPgTables_abs");
		if (tabCols.contains("frozenxid_age"))
		{
			String sql = ""
				    + "select top " + getTopRows() + " \n"
				    + "     t.[dbname] \n"
				    + "    ,t.[schemaname] \n"
				    + "    ,t.[relname] \n"
				    + "    ,t.[frozenxid_age] \n"
				    + "    ,cast(100.0 * t.[frozenxid_age] / " + _freezeMaxAge + " as numeric(8,1)) as [pct_of_freeze_max_age] \n"
				    + "    ,t.[minmxid_age] \n"
				    + "    ,t.[n_live_tup] \n"
				    + "    ,t.[n_dead_tup] \n"
				    + "    ,t.[last_autovacuum] \n"
				    + "    ,t.[last_vacuum] \n"
				    + "    ,t.[autovacuum_count] \n"
				    + "    ,t.[reloptions] \n"
				    + "from [CmPgTables_abs] t \n"
				    + "join (select [dbname], [schemaname], [relname], max([SessionSampleTime]) as [last_ts] \n"
				    + "      from [CmPgTables_abs] \n"
				    + "      " + where + " \n"
				    + "      group by [dbname], [schemaname], [relname] \n"
				    + "     ) l on l.[dbname] = t.[dbname] and l.[schemaname] = t.[schemaname] and l.[relname] = t.[relname] and l.[last_ts] = t.[SessionSampleTime] \n"
				    + "order by t.[frozenxid_age] desc \n"
				    + "";

			_tablesRstm = executeQuery(conn, sql, true, "PostgresVacuumHealthTables");
			_tablesRstm.setHighlightSortColumns("frozenxid_age");

			_tablesRstm.setDescription(
					"The oldest tables, in transaction ids: age(relfrozenxid) at the last sample of each table (from the table <code>CmPgTables_abs</code>, only tables that were read or written).<br>" +
					"A table above 100% of autovacuum_freeze_max_age gets a forced (anti-wraparound) vacuum, that can't be cancelled by a lock conflict and reads the whole table. " +
					"A big table that stays far above 100% is not frozen in time: check <code>autovacuum_vacuum_cost_limit</code> and the holders above.<br>" +
					"");
			_tablesRstm.setColumnDescription("dbname"               , "The database");
			_tablesRstm.setColumnDescription("schemaname"           , "The schema");
			_tablesRstm.setColumnDescription("relname"              , "The table");
			_tablesRstm.setColumnDescription("frozenxid_age"        , "age(relfrozenxid): the oldest unfrozen transaction id in the table, as an age");
			_tablesRstm.setColumnDescription("pct_of_freeze_max_age", "frozenxid_age in percent of autovacuum_freeze_max_age (or the table's own setting in reloptions)");
			_tablesRstm.setColumnDescription("minmxid_age"          , "mxid_age(relminmxid): the same for multixact ids");
			_tablesRstm.setColumnDescription("n_live_tup"           , "Estimated live rows");
			_tablesRstm.setColumnDescription("n_dead_tup"           , "Estimated dead rows");
			_tablesRstm.setColumnDescription("last_autovacuum"      , "When autovacuum last vacuumed the table");
			_tablesRstm.setColumnDescription("last_vacuum"          , "When the table was last vacuumed manually");
			_tablesRstm.setColumnDescription("autovacuum_count"     , "Autovacuums since the statistics were reset");
			_tablesRstm.setColumnDescription("reloptions"           , "The table's own settings (for example autovacuum_freeze_max_age, autovacuum_enabled)");
		}

		//-------------------------------------------------------------
		// Postgres 19: tables that autovacuum wanted to process (score >= 1) in many samples, so it did not get to them in time
		//-------------------------------------------------------------
		if (doTableExist(conn, schema, "CmPgAutovacuumScores_abs"))
		{
			String sql = ""
				    + "select top " + getTopRows() + " \n"
				    + "     [dbname] \n"
				    + "    ,[schemaname] \n"
				    + "    ,[relname] \n"
				    + "    ,sum(case when [score] >= 1 then 1 else 0 end) as [samples_score_over_1] \n"
				    + "    ,(select count(distinct [SessionSampleTime]) from [CmPgAutovacuumScores_abs] " + where + ") as [samples_count] \n"
				    + "    ,max([score]) as [score__max] \n"
				    + "    ,case \n"
				    + "         when max([xid_score])           = max([score]) then 'xid (wraparound)' \n"
				    + "         when max([mxid_score])          = max([score]) then 'multixact (wraparound)' \n"
				    + "         when max([vacuum_score])        = max([score]) then 'dead rows' \n"
				    + "         when max([vacuum_insert_score]) = max([score]) then 'inserted rows' \n"
				    + "         when max([analyze_score])       = max([score]) then 'changed rows (analyze)' \n"
				    + "         else '' \n"
				    + "     end as [driven_by] \n"
				    + "    ,sum(case when [score] >= 1 and [do_vacuum] = false and [do_analyze] = false then 1 else 0 end) as [samples_not_scheduled] \n"
				    + "from [CmPgAutovacuumScores_abs] \n"
				    + where + " \n"
				    + "group by [dbname], [schemaname], [relname] \n"
				    + "having sum(case when [score] >= 1 then 1 else 0 end) > 0 \n"
				    + "order by sum(case when [score] >= 1 then 1 else 0 end) desc, max([score]) desc \n"
				    + "";

			_scoresRstm = executeQuery(conn, sql, true, "PostgresVacuumHealthScores");
			_scoresRstm.setHighlightSortColumns("samples_score_over_1");

			_scoresRstm.setDescription(
					"Postgres 19 and above: tables that autovacuum wanted to process (score 1.0 or more) in many samples, so it did not get to them in time (from the table <code>CmPgAutovacuumScores_abs</code>).<br>" +
					"Autovacuum processes the highest score first. A table that stays above 1.0 for long means too few autovacuum workers (<code>autovacuum_max_workers</code>) or a too low cost limit. " +
					"<code>samples_not_scheduled</code> &gt; 0: autovacuum is turned off for the table (reloptions autovacuum_enabled = false).<br>" +
					"");
			_scoresRstm.setColumnDescription("dbname"               , "The database");
			_scoresRstm.setColumnDescription("schemaname"           , "The schema");
			_scoresRstm.setColumnDescription("relname"              , "The table");
			_scoresRstm.setColumnDescription("samples_score_over_1" , "In how many samples the table's score was 1.0 or more (autovacuum would process it)");
			_scoresRstm.setColumnDescription("samples_count"        , "Number of samples of autovacuum scores in the period");
			_scoresRstm.setColumnDescription("score__max"           , "The highest score during the period");
			_scoresRstm.setColumnDescription("driven_by"            , "Which part score was the highest: wraparound (xid/multixact), dead rows, inserted rows, or changed rows (analyze)");
			_scoresRstm.setColumnDescription("samples_not_scheduled", "Samples with a score of 1.0 or more, but neither vacuum nor analyze would run: autovacuum is turned off for the table");
		}
	}

	private IReportChart _CmPgDatabase_XidAge; // null = not recorded
	private IReportChart _CmSummary_maxXid;    // null = not recorded
}
