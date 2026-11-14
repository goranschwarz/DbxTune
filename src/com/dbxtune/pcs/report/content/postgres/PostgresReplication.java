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
import java.lang.invoke.MethodHandles;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.cm.postgres.CmPgRepSlotStats;
import com.dbxtune.cm.postgres.CmPgRepSlots;
import com.dbxtune.cm.postgres.CmPgReplication;
import com.dbxtune.cm.postgres.CmPgSubscription;
import com.dbxtune.cm.postgres.CmPgWalReceiver;
import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.pcs.report.content.IReportChart;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;

/**
 * Replication / HA: the role of the server during the period, the standbys of this primary, the replication slots, this standby's
 * replay delay, the logical replication subscriptions, and logical decoding (spill to disk).
 * <p>
 * Each part is only shown when it was recorded and had rows in the period (a server without replication is the normal case). Built on MySqlReplication.
 */
public class PostgresReplication
extends PostgresAbstract
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private String _roleText;                 // null = CmSummary had no in_recovery

	private ResultSetTableModel _standbysRstm; // null = nothing recorded
	private ResultSetTableModel _slotsRstm;
	private ResultSetTableModel _receiverRstm;
	private ResultSetTableModel _subsRstm;
	private ResultSetTableModel _decodingRstm;
	private String              _decodingVerdict;

	public PostgresReplication(DailySummaryReportAbstract reportingInstance)
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

	private boolean hasAnything()
	{
		return _standbysRstm != null || _slotsRstm != null || _receiverRstm != null || _subsRstm != null || _decodingRstm != null;
	}

	@Override
	public void writeMessageText(Writer w, MessageType messageType)
	throws IOException
	{
		if (_roleText != null)
			w.append("<p>" + _roleText + "</p>\n");

		if ( ! hasAnything() )
		{
			w.append("<b>No replication was recorded</b>: no standbys connected to this server, no replication slots, no WAL receiver (this is not a standby) and no logical replication subscriptions.<br>\n");
			return;
		}

		//----- standbys of this primary
		if (_standbysRstm != null)
		{
			w.append("<h4>Standbys of this server</h4>\n");
			w.append(getSectionDescriptionHtml(_standbysRstm, true));
			w.append(toHtmlTable(_standbysRstm));
			writeChart(w, _CmPgReplication_TotalLag, "CmPgReplication_" + CmPgReplication.GRAPH_NAME_TOTAL_LAG, "Below is how far behind each standby was (in KB of WAL) during the day.", null);
			if (isFullMessageType())
				writeChart(w, _CmPgReplication_RepAgeSec, null, null, null);
		}

		//----- replication slots
		if (_slotsRstm != null)
		{
			w.append("<h4>Replication slots</h4>\n");
			w.append(getSectionDescriptionHtml(_slotsRstm, true));
			w.append(toHtmlTable(_slotsRstm));
			writeChart(w, _CmPgRepSlots_LagInKb, "CmPgRepSlots_" + CmPgRepSlots.GRAPH_NAME_LAG_KB, "Below is how much WAL each slot kept (in KB) during the day.", "<b>Note</b>: A slot keeps the WAL until its consumer has read it: an inactive slot can fill the disk.");
		}

		//----- this standby
		if (_receiverRstm != null)
		{
			w.append("<h4>This server as a standby (WAL receiver)</h4>\n");
			w.append(getSectionDescriptionHtml(_receiverRstm, true));
			w.append(toHtmlTable(_receiverRstm));
			writeChart(w, _CmPgWalReceiver_ReplayDelaySec, "CmPgWalReceiver_" + CmPgWalReceiver.GRAPH_NAME_REPLAY_DELAY_SEC, "Below is how far behind this standby was in applying the WAL (seconds) during the day.", null);
			if (isFullMessageType())
				writeChart(w, _CmPgWalReceiver_ReplayBehindKb, null, null, null);
		}

		//----- logical replication: subscriptions (this server is a subscriber)
		if (_subsRstm != null)
		{
			w.append("<h4>Logical replication subscriptions</h4>\n");
			w.append(getSectionDescriptionHtml(_subsRstm, true));
			w.append(toHtmlTable(_subsRstm));
			writeChart(w, _CmPgSubscription_Errors, "CmPgSubscription_" + CmPgSubscription.GRAPH_NAME_ERRORS, "Below are the apply and synchronization errors per subscription during the day.", null);
		}

		//----- logical decoding (this server is a publisher)
		if (_decodingRstm != null)
		{
			w.append("<h4>Logical decoding (publisher side)</h4>\n");
			w.append(getSectionDescriptionHtml(_decodingRstm, true));
			w.append(toHtmlTable(_decodingRstm));
			if (_decodingVerdict != null)
				w.append("<p><b>Verdict</b>: " + _decodingVerdict + "</p>\n");
			if (isFullMessageType())
				writeChart(w, _CmPgRepSlotStats_SpillMb, null, null, null);
		}
	}

	/** A chart (when recorded), with the DbxCentral link when a description is given */
	private void writeChart(Writer w, IReportChart chart, String dbxCentralGraph, String description, String note)
	throws IOException
	{
		if (chart == null)
			return;
		if (description != null)
			w.append(getDbxCentralLinkWithDescForGraphs(false, description, dbxCentralGraph));
		chart.writeHtmlContent(w, null, note);
	}

	@Override
	public String getSubject()
	{
		return "Replication / HA: Standbys, Slots, Subscriptions (origin: CmPgReplication, CmPgRepSlots, CmPgWalReceiver, CmPgSubscription, CmPgRepSlotStats)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue (the alarms does that)
	}

	/** Does the table exist, and have rows in the report period? */
	private boolean hasRows(DbxConnection conn, String schema, String tabName, String where)
	{
		if ( ! doTableExist(conn, schema, tabName) )
			return false;
		String sql = conn.quotifySqlString("select count(*) from [" + tabName + "] " + where);
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			return rs.next() && rs.getLong(1) > 0;
		}
		catch (SQLException ex)
		{
			return false;
		}
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		String schema = getReportingInstance().getDbmsSchemaName();
		String where  = "where 1 = 1 " + getReportPeriodSqlWhere().trim();

		//-------------------------------------------------------------
		// The role during the period (CmSummary.in_recovery)
		//-------------------------------------------------------------
		if (getRecordedColumns(conn, "CmSummary_abs").contains("in_recovery"))
		{
			String sql = conn.quotifySqlString(""
				    + "select \n"
				    + "     sum(case when [in_recovery] = true  then 1 else 0 end) \n"
				    + "    ,sum(case when [in_recovery] = false then 1 else 0 end) \n"
				    + "    ,min(case when [in_recovery] = true  then [SessionSampleTime] end) \n"
				    + "    ,min(case when [in_recovery] = false then [SessionSampleTime] end) \n"
				    + "from [CmSummary_abs] " + where + " \n");
			try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
			{
				if (rs.next())
				{
					long standby = rs.getLong(1);
					long primary = rs.getLong(2);
					if      (standby > 0 && primary > 0) _roleText = "<b>The server changed role during the period</b> (a failover or switchover): standby in " + standby + " samples (first " + rs.getString(3) + "), primary in " + primary + " samples (first " + rs.getString(4) + "). The changes are listed in 'Recording Information'.";
					else if (standby > 0)                _roleText = "The server was a <b>standby</b> (in recovery) the whole period.";
					else if (primary > 0)                _roleText = "The server was a <b>primary</b> (not in recovery) the whole period.";
				}
			}
			catch (SQLException ex)
			{
				_logger.info("Problems reading the role from CmSummary_abs. Caught: " + ex);
			}
		}

		//-------------------------------------------------------------
		// Standbys connected to this server (CmPgReplication = pg_stat_replication)
		//-------------------------------------------------------------
		if (hasRows(conn, schema, "CmPgReplication_abs", where))
		{
			Set<String> cols = getRecordedColumns(conn, "CmPgReplication_abs");
			boolean hasReplayLag = cols.contains("replay_lag");
			boolean hasReplyTime = cols.contains("reply_time_seconds");

			String sql = ""
				    + "select \n"
				    + "     l.[application_name] \n"
				    + "    ,l.[client_addr] \n"
				    + "    ,l.[state]      as [state__last] \n"
				    + "    ,l.[sync_state] as [sync_state__last] \n"
				    + "    ,l.[total_lag_kb] \n"
				    + "    ,m.[total_lag_kb__max] \n"
				    + (hasReplayLag ? "    ,m.[replay_lag__max] \n" : "")
				    + (hasReplyTime ? "    ,m.[reply_time_seconds__max] \n" : "")
				    + "    ,m.[samples_not_streaming] \n"
				    + "    ,m.[samples_seen] \n"
				    + "    ,(select count(distinct [SessionSampleTime]) from [CmSummary_abs] " + where + ") as [samples_count] \n"
				    + "    ,m.[last_seen] \n"
				    + "from [CmPgReplication_abs] l \n"
				    + "join (select \n"
				    + "           [application_name] \n"
				    + "          ,[client_addr] \n"
				    + "          ,max([total_lag_kb])                                          as [total_lag_kb__max] \n"
				    + (hasReplayLag ? "          ,max([replay_lag])                                            as [replay_lag__max] \n" : "")
				    + (hasReplyTime ? "          ,max([reply_time_seconds])                                    as [reply_time_seconds__max] \n" : "")
				    + "          ,sum(case when [state] <> 'streaming' then 1 else 0 end)        as [samples_not_streaming] \n"
				    + "          ,count(*)                                                     as [samples_seen] \n"
				    + "          ,max([SessionSampleTime])                                     as [last_seen] \n"
				    + "      from [CmPgReplication_abs] \n"
				    + "      " + where + " \n"
				    + "      group by [application_name], [client_addr] \n"
				    + "     ) m on m.[application_name] = l.[application_name] and m.[client_addr] = l.[client_addr] and m.[last_seen] = l.[SessionSampleTime] \n"
				    + "order by m.[total_lag_kb__max] desc \n"
				    + "";

			_standbysRstm = executeQuery(conn, sql, true, "PostgresReplicationStandbys");
			_standbysRstm.setHighlightSortColumns("total_lag_kb__max");

			_standbysRstm.setDescription(
					"The standbys (and pg_basebackup / logical replication walsenders) connected to this server: their last state, and the worst lag of the period (from the table <code>CmPgReplication_abs</code>).<br>" +
					"Look for: a high <code>total_lag_kb__max</code> (the standby is behind: a failover would lose or wait for that WAL), <code>samples_not_streaming</code> &gt; 0 (catching up or disconnected), " +
					"and <code>samples_seen</code> lower than <code>samples_count</code> (the standby was not connected all the time).<br>" +
					"");
			_standbysRstm.setColumnDescription("application_name"       , "The standby's application_name (in primary_conninfo), or the walsender client");
			_standbysRstm.setColumnDescription("client_addr"            , "The standby's address");
			_standbysRstm.setColumnDescription("state__last"            , "The state at the last sample where it was seen: streaming, catchup, startup, backup");
			_standbysRstm.setColumnDescription("sync_state__last"       , "async, potential, sync or quorum (synchronous_standby_names)");
			_standbysRstm.setColumnDescription("total_lag_kb"           , "WAL written on this server but not yet replayed on the standby, in KB, at the last sample");
			_standbysRstm.setColumnDescription("total_lag_kb__max"      , "The highest total_lag_kb during the period");
			_standbysRstm.setColumnDescription("replay_lag__max"        , "The highest replay_lag (seconds from a commit here until the standby replayed it) during the period");
			_standbysRstm.setColumnDescription("reply_time_seconds__max", "The longest time since the standby's last reply, in seconds");
			_standbysRstm.setColumnDescription("samples_not_streaming"  , "In how many samples the state was not 'streaming'");
			_standbysRstm.setColumnDescription("samples_seen"           , "In how many samples the standby was connected");
			_standbysRstm.setColumnDescription("samples_count"          , "Number of collector samples in the period");
			_standbysRstm.setColumnDescription("last_seen"              , "The last sample where the standby was connected");

			_CmPgReplication_TotalLag  = chart(conn, schema, CmPgReplication.CM_NAME, CmPgReplication.GRAPH_NAME_TOTAL_LAG,       "Total Replication Lag in KB, per Standby (" + CmPgReplication.SHORT_NAME + ")");
			_CmPgReplication_RepAgeSec = chart(conn, schema, CmPgReplication.CM_NAME, CmPgReplication.GRAPH_NAME_REP_AGE_SECONDS, "Seconds Since the Standby's Last Reply (" + CmPgReplication.SHORT_NAME + ")");
		}

		//-------------------------------------------------------------
		// Replication slots (CmPgRepSlots = pg_replication_slots)
		//-------------------------------------------------------------
		if (hasRows(conn, schema, "CmPgRepSlots_abs", where))
		{
			Set<String> cols = getRecordedColumns(conn, "CmPgRepSlots_abs");
			boolean hasWalStatus = cols.contains("wal_status");
			boolean hasInvReason = cols.contains("invalidation_reason");

			String sql = ""
				    + "select \n"
				    + "     l.[slot_name] \n"
				    + "    ,l.[slot_type] \n"
				    + "    ,l.[database] \n"
				    + "    ,l.[active]  as [active__last] \n"
				    + "    ,l.[lag_mb] \n"
				    + "    ,m.[lag_mb__max] \n"
				    + (hasWalStatus ? ""
				    + "    ,l.[wal_status] as [wal_status__last] \n"
				    + "    ,case m.[wal_status__worst] when 4 then 'lost' when 3 then 'unreserved' when 2 then 'extended' when 1 then 'reserved' else '' end as [wal_status__worst] \n" : "")
				    + (hasInvReason ? "    ,cast(l.[invalidation_reason] as varchar(100)) as [invalidation_reason] \n" : "")
				    + "    ,m.[samples_inactive] \n"
				    + "    ,m.[samples_seen] \n"
				    + "    ,m.[last_seen] \n"
				    + "from [CmPgRepSlots_abs] l \n"
				    + "join (select \n"
				    + "           [slot_name] \n"
				    + "          ,max([lag_mb])                                         as [lag_mb__max] \n"
				    + (hasWalStatus ? "          ,max(case [wal_status] when 'lost' then 4 when 'unreserved' then 3 when 'extended' then 2 when 'reserved' then 1 else 0 end) as [wal_status__worst] \n" : "")
				    + "          ,sum(case when [active] = false then 1 else 0 end)       as [samples_inactive] \n"
				    + "          ,count(*)                                              as [samples_seen] \n"
				    + "          ,max([SessionSampleTime])                              as [last_seen] \n"
				    + "      from [CmPgRepSlots_abs] \n"
				    + "      " + where + " \n"
				    + "      group by [slot_name] \n"
				    + "     ) m on m.[slot_name] = l.[slot_name] and m.[last_seen] = l.[SessionSampleTime] \n"
				    + "where l.[slot_name] not like 'pg_basebackup_%' \n"
				    + "order by m.[lag_mb__max] desc \n"
				    + "";

			_slotsRstm = executeQuery(conn, sql, true, "PostgresReplicationSlots");
			_slotsRstm.setHighlightSortColumns("lag_mb__max");

			_slotsRstm.setDescription(
					"The replication slots on this server: the WAL each one kept, its WAL status, and how often nobody used it (from the table <code>CmPgRepSlots_abs</code>; pg_basebackup's temporary slots are left out).<br>" +
					"A slot keeps all WAL (and for a logical slot, old catalog rows) until its consumer has read it. " +
					"Look for: <code>samples_inactive</code> close to <code>samples_seen</code> (a slot nobody reads from: drop it if it is not needed), " +
					"a growing <code>lag_mb</code>, and <code>wal_status__worst</code> 'unreserved' or 'lost' (the WAL was, or was about to be, removed: the consumer must be rebuilt).<br>" +
					"");
			_slotsRstm.setColumnDescription("slot_name"          , "The slot");
			_slotsRstm.setColumnDescription("slot_type"          , "physical (a standby) or logical (a subscription / CDC tool)");
			_slotsRstm.setColumnDescription("database"           , "The database of a logical slot");
			_slotsRstm.setColumnDescription("active__last"       , "Was the slot used at the last sample where it was seen");
			_slotsRstm.setColumnDescription("lag_mb"             , "WAL kept by the slot (from restart_lsn to now), in MB, at the last sample");
			_slotsRstm.setColumnDescription("lag_mb__max"        , "The most WAL kept during the period, in MB");
			_slotsRstm.setColumnDescription("wal_status__last"   , "reserved / extended / unreserved / lost at the last sample (Postgres 13 and above)");
			_slotsRstm.setColumnDescription("wal_status__worst"  , "The worst wal_status during the period");
			_slotsRstm.setColumnDescription("invalidation_reason", "Why the slot was invalidated (Postgres 17 and above): wal_removed, rows_removed, idle_timeout, ...");
			_slotsRstm.setColumnDescription("samples_inactive"   , "In how many samples the slot was not used");
			_slotsRstm.setColumnDescription("samples_seen"       , "In how many samples the slot existed");
			_slotsRstm.setColumnDescription("last_seen"          , "The last sample where the slot existed");

			_CmPgRepSlots_LagInKb = chart(conn, schema, CmPgRepSlots.CM_NAME, CmPgRepSlots.GRAPH_NAME_LAG_KB, "WAL Kept by each Replication Slot, in KB (" + CmPgRepSlots.SHORT_NAME + ")");
		}

		//-------------------------------------------------------------
		// This server as a standby (CmPgWalReceiver = pg_stat_wal_receiver)
		//-------------------------------------------------------------
		if (hasRows(conn, schema, "CmPgWalReceiver_abs", where))
		{
			Set<String> cols = getRecordedColumns(conn, "CmPgWalReceiver_abs");
			boolean hasReplay = cols.contains("replay_delay_sec");

			String sql = ""
				    + "select \n"
				    + "     l.[sender_host] \n"
				    + "    ,l.[sender_port] \n"
				    + "    ,l.[slot_name] \n"
				    + "    ,l.[status] as [status__last] \n"
				    + (hasReplay ? ""
				    + "    ,l.[replay_delay_sec] \n"
				    + "    ,m.[replay_delay_sec__max] \n"
				    + "    ,(select min([SessionSampleTime]) from [CmPgWalReceiver_abs] x " + where + " and x.[sender_host] = l.[sender_host] and x.[replay_delay_sec] = m.[replay_delay_sec__max]) as [replay_delay_sec__max_time] \n"
				    + "    ,m.[replay_behind_kb__max] \n"
				    + "    ,m.[samples_paused] \n" : "")
				    + "    ,m.[samples_not_streaming] \n"
				    + "    ,m.[samples_seen] \n"
				    + "    ,(select count(distinct [SessionSampleTime]) from [CmSummary_abs] " + where + ") as [samples_count] \n"
				    + "from [CmPgWalReceiver_abs] l \n"
				    + "join (select \n"
				    + "           [sender_host] \n"
				    + (hasReplay ? ""
				    + "          ,max([replay_delay_sec])                                  as [replay_delay_sec__max] \n"
				    + "          ,max([replay_behind_kb])                                  as [replay_behind_kb__max] \n"
				    + "          ,sum(case when [replay_paused] = true then 1 else 0 end)  as [samples_paused] \n" : "")
				    + "          ,sum(case when [status] <> 'streaming' then 1 else 0 end) as [samples_not_streaming] \n"
				    + "          ,count(*)                                                 as [samples_seen] \n"
				    + "          ,max([SessionSampleTime])                                 as [last_seen] \n"
				    + "      from [CmPgWalReceiver_abs] \n"
				    + "      " + where + " \n"
				    + "      group by [sender_host] \n"
				    + "     ) m on m.[sender_host] = l.[sender_host] and m.[last_seen] = l.[SessionSampleTime] \n"
				    + "";

			_receiverRstm = executeQuery(conn, sql, true, "PostgresReplicationReceiver");

			_receiverRstm.setDescription(
					"This server is a standby: the primary (or upstream standby) it streams from, and how far behind it was in applying the WAL (from the table <code>CmPgWalReceiver_abs</code>).<br>" +
					"<code>replay_delay_sec</code> is how old the data on this standby is (counted only while there is received WAL that is not yet applied, so an idle primary is no delay). " +
					"Look for: a high <code>replay_delay_sec__max</code> (queries here see old data, and a failover would take longer: often long queries on the standby that conflict with the replay, see max_standby_streaming_delay), " +
					"and <code>samples_paused</code> &gt; 0 (somebody ran pg_wal_replay_pause()).<br>" +
					"");
			_receiverRstm.setColumnDescription("sender_host"               , "The server this standby streams the WAL from");
			_receiverRstm.setColumnDescription("sender_port"               , "Its port");
			_receiverRstm.setColumnDescription("slot_name"                 , "The replication slot used on the sender (if any)");
			_receiverRstm.setColumnDescription("status__last"              , "The WAL receiver's status at the last sample: streaming, catchup, ...");
			_receiverRstm.setColumnDescription("replay_delay_sec"          , "How far behind the replay was at the last sample, in seconds");
			_receiverRstm.setColumnDescription("replay_delay_sec__max"     , "The most it was behind during the period, in seconds");
			_receiverRstm.setColumnDescription("replay_delay_sec__max_time", "When it was the most behind");
			_receiverRstm.setColumnDescription("replay_behind_kb__max"     , "The most WAL received but not yet applied, in KB");
			_receiverRstm.setColumnDescription("samples_paused"            , "In how many samples the replay was paused (pg_wal_replay_pause())");
			_receiverRstm.setColumnDescription("samples_not_streaming"     , "In how many samples the WAL receiver was not 'streaming'");
			_receiverRstm.setColumnDescription("samples_seen"              , "In how many samples the WAL receiver was running");
			_receiverRstm.setColumnDescription("samples_count"             , "Number of collector samples in the period");

			_CmPgWalReceiver_ReplayDelaySec = chart(conn, schema, CmPgWalReceiver.CM_NAME, CmPgWalReceiver.GRAPH_NAME_REPLAY_DELAY_SEC, "Standby Replay Delay in Seconds (" + CmPgWalReceiver.SHORT_NAME + ")");
			_CmPgWalReceiver_ReplayBehindKb = chart(conn, schema, CmPgWalReceiver.CM_NAME, CmPgWalReceiver.GRAPH_NAME_REPLAY_BEHIND_KB, "Standby WAL Received but not Applied, in KB (" + CmPgWalReceiver.SHORT_NAME + ")");
		}

		//-------------------------------------------------------------
		// Logical replication subscriptions (CmPgSubscription), this server is a subscriber
		//-------------------------------------------------------------
		if (hasRows(conn, schema, "CmPgSubscription_abs", where))
		{
			Set<String> cols = getRecordedColumns(conn, "CmPgSubscription_diff");
			// The error counters always, the conflict counters (18+) only those that happened (most are 0: a wide table of zeros helps nobody)
			List<String> counters = new ArrayList<>();
			for (String c : new String[] {"apply_error_count", "sync_error_count", "sync_table_error_count", "sync_seq_error_count",
			                              "confl_insert_exists", "confl_update_origin_differs", "confl_update_exists", "confl_update_deleted", "confl_update_missing",
			                              "confl_delete_origin_differs", "confl_delete_missing", "confl_multiple_unique_conflicts"})
			{
				if ( ! cols.contains(c) )
					continue;
				if (c.startsWith("confl_") && sumOf(conn, "CmPgSubscription_diff", c, where) <= 0)
					continue;
				counters.add(c);
			}

			StringBuilder sums = new StringBuilder();
			for (String c : counters)
				sums.append("    ,(select sum(d.[" + c + "]) from [CmPgSubscription_diff] d " + where.replace("where 1 = 1", "where 1 = 1 and d.[dbname] = l.[dbname] and d.[subname] = l.[subname]") + ") as [" + c + "__sum] \n");

			String sql = ""
				    + "select \n"
				    + "     l.[dbname] \n"
				    + "    ,l.[subname] \n"
				    + "    ,l.[enabled] as [enabled__last] \n"
				    + "    ,m.[samples_disabled] \n"
				    + "    ,m.[samples_no_apply_worker] \n"
				    + "    ,m.[last_msg_age_sec__max] \n"
				    + sums
				    + "    ,m.[samples_seen] \n"
				    + "from [CmPgSubscription_abs] l \n"
				    + "join (select \n"
				    + "           [dbname] \n"
				    + "          ,[subname] \n"
				    + "          ,sum(case when [enabled] = false then 1 else 0 end)                    as [samples_disabled] \n"
				    + "          ,sum(case when [enabled] = true and coalesce([apply_pid], 0) = 0 then 1 else 0 end) as [samples_no_apply_worker] \n"
				    + "          ,max([last_msg_age_sec])                                                as [last_msg_age_sec__max] \n"
				    + "          ,count(*)                                                              as [samples_seen] \n"
				    + "          ,max([SessionSampleTime])                                              as [last_seen] \n"
				    + "      from [CmPgSubscription_abs] \n"
				    + "      " + where + " \n"
				    + "      group by [dbname], [subname] \n"
				    + "     ) m on m.[dbname] = l.[dbname] and m.[subname] = l.[subname] and m.[last_seen] = l.[SessionSampleTime] \n"
				    + "order by l.[dbname], l.[subname] \n"
				    + "";

			_subsRstm = executeQuery(conn, sql, true, "PostgresReplicationSubscriptions");

			_subsRstm.setDescription(
					"The logical replication subscriptions on this server (it is a subscriber): errors and conflicts during the period (from the tables <code>CmPgSubscription_abs</code> and <code>_diff</code>).<br>" +
					"An apply error stops the replication at the failing change (it is retried until it works, or the subscription is disabled with <code>disable_on_error</code>). The error text is in the error log.<br>" +
					"Look for: <code>apply_error_count__sum</code> &gt; 0, <code>samples_disabled</code> or <code>samples_no_apply_worker</code> &gt; 0, and a high <code>last_msg_age_sec__max</code> (no contact with the publisher).<br>" +
					"Conflicts (<code>confl_*</code>, Postgres 18 and above) are changes that did not fit the subscriber's data: they are counted, and the change is applied or skipped depending on the conflict. Only the conflict types that happened are shown.<br>" +
					"");
			_subsRstm.setColumnDescription("dbname"                 , "The database of the subscription");
			_subsRstm.setColumnDescription("subname"                , "The subscription");
			_subsRstm.setColumnDescription("enabled__last"          , "Was the subscription enabled at the last sample");
			_subsRstm.setColumnDescription("samples_disabled"       , "In how many samples it was disabled");
			_subsRstm.setColumnDescription("samples_no_apply_worker", "In how many samples it was enabled but no apply worker ran (it failed and waited to be restarted)");
			_subsRstm.setColumnDescription("last_msg_age_sec__max"  , "The longest time without a message from the publisher, in seconds (an idle publisher still sends keepalives)");
			_subsRstm.setColumnDescription("samples_seen"           , "In how many samples the subscription existed");
			for (String c : counters)
				_subsRstm.setColumnDescription(c + "__sum", "The sum of " + c + " during the period");

			_CmPgSubscription_Errors = chart(conn, schema, CmPgSubscription.CM_NAME, CmPgSubscription.GRAPH_NAME_ERRORS, "Subscription Apply + Sync Errors, per Subscription (" + CmPgSubscription.SHORT_NAME + ")");
		}

		//-------------------------------------------------------------
		// Logical decoding on this server (CmPgRepSlotStats), it is a publisher
		//-------------------------------------------------------------
		if (hasRows(conn, schema, "CmPgRepSlotStats_diff", where))
		{
			String sql = ""
				    + "select \n"
				    + "     [slot_name] \n"
				    + "    ,max([dbname])        as [dbname] \n"
				    + "    ,sum([total_mb])      as [total_mb__sum] \n"
				    + "    ,sum([total_txns])    as [total_txns__sum] \n"
				    + "    ,sum([spill_mb])      as [spill_mb__sum] \n"
				    + "    ,sum([spill_txns])    as [spill_txns__sum] \n"
				    + "    ,sum([spill_count])   as [spill_count__sum] \n"
				    + "    ,sum([stream_mb])     as [stream_mb__sum] \n"
				    + "    ,sum([stream_txns])   as [stream_txns__sum] \n"
				    + "from [CmPgRepSlotStats_diff] \n"
				    + where + " \n"
				    + "group by [slot_name] \n"
				    + "order by sum([spill_mb]) desc, sum([total_mb]) desc \n"
				    + "";

			_decodingRstm = executeQuery(conn, sql, true, "PostgresReplicationDecoding");
			_decodingRstm.setHighlightSortColumns("spill_mb__sum");

			_decodingRstm.setDescription(
					"Logical decoding on this server (it is a publisher): how much each logical slot decoded, and how much was spilled to disk (from the table <code>CmPgRepSlotStats_diff</code>).<br>" +
					"A transaction that does not fit in <code>logical_decoding_work_mem</code> is spilled to disk while it is decoded (extra disk I/O on the publisher, and the subscriber gets it later), or streamed when the subscription has <code>streaming = on</code>.<br>" +
					"");
			_decodingRstm.setColumnDescription("slot_name"       , "The logical replication slot");
			_decodingRstm.setColumnDescription("dbname"          , "The database it decodes");
			_decodingRstm.setColumnDescription("total_mb__sum"   , "MB of decoded transactions sent to the output plugin during the period");
			_decodingRstm.setColumnDescription("total_txns__sum" , "Decoded transactions during the period");
			_decodingRstm.setColumnDescription("spill_mb__sum"   , "MB spilled to disk during the period");
			_decodingRstm.setColumnDescription("spill_txns__sum" , "Transactions that were spilled to disk");
			_decodingRstm.setColumnDescription("spill_count__sum", "Times transactions were spilled (a big transaction is spilled many times)");
			_decodingRstm.setColumnDescription("stream_mb__sum"  , "MB streamed to the subscriber before the transaction ended");
			_decodingRstm.setColumnDescription("stream_txns__sum", "Transactions that were streamed");

			// The verdict: spill
			double spillMb = 0;
			for (int r = 0; r < _decodingRstm.getRowCount(); r++)
			{
				Double v = _decodingRstm.getValueAsDouble(r, "spill_mb__sum");
				spillMb += v == null ? 0 : v;
			}
			if (spillMb > 0)
			{
				long workMemKb = getServerSettingLong(conn, "logical_decoding_work_mem", 65536);
				_decodingVerdict = String.format("%.1f", spillMb) + " MB was spilled to disk while decoding. Big transactions did not fit in logical_decoding_work_mem (now " + (workMemKb < 1024 ? workMemKb + " kB" : (workMemKb / 1024) + " MB") + "). "
						+ "<i>Fix</i>: ALTER SYSTEM SET logical_decoding_work_mem = '" + Math.max(128, workMemKb / 1024 * 4) + "MB'; SELECT pg_reload_conf(); (it is per slot), "
						+ "or let the subscriber get big transactions while they run: ALTER SUBSCRIPTION ... SET (streaming = on); (parallel on Postgres 16 and above).";
			}

			_CmPgRepSlotStats_SpillMb = chart(conn, schema, CmPgRepSlotStats.CM_NAME, CmPgRepSlotStats.GRAPH_NAME_SPILL_MB, "Logical Decoding Spilled to Disk, MB per Second, per Slot (" + CmPgRepSlotStats.SHORT_NAME + ")");
		}
	}

	/** sum(column) in the report period, or 0 on errors */
	private long sumOf(DbxConnection conn, String tabName, String colName, String where)
	{
		String sql = conn.quotifySqlString("select sum([" + colName + "]) from [" + tabName + "] " + where);
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			return rs.next() ? rs.getLong(1) : 0;
		}
		catch (SQLException ex)
		{
			return 0;
		}
	}

	/** A line chart, or null when the graph was not recorded */
	private IReportChart chart(DbxConnection conn, String schema, String cmName, String graphName, String title)
	{
		if ( ! doTableExist(conn, schema, cmName + "_" + graphName) )
			return null;
		return createTsLineChart(conn, schema, cmName, graphName, -1, true, null, title);
	}

	private IReportChart _CmPgReplication_TotalLag;
	private IReportChart _CmPgReplication_RepAgeSec;
	private IReportChart _CmPgRepSlots_LagInKb;
	private IReportChart _CmPgWalReceiver_ReplayDelaySec;
	private IReportChart _CmPgWalReceiver_ReplayBehindKb;
	private IReportChart _CmPgSubscription_Errors;
	private IReportChart _CmPgRepSlotStats_SpillMb;
}
