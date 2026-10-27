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
package com.dbxtune.pcs.report.content.db2;

import java.io.IOException;
import java.io.Writer;

import com.dbxtune.cm.db2.CmHadr;
import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.pcs.report.content.IReportChart;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;

/**
 * HADR: if the standby databases were connected and in PEER state, and how far behind they were (from CmHadr). Built on OracleDataGuard.
 */
public class Db2Hadr
extends Db2Abstract
{
	private ResultSetTableModel _shortRstm; // null = HADR was not recorded

	public Db2Hadr(DailySummaryReportAbstract reportingInstance)
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
		if (_shortRstm == null || _shortRstm.getRowCount() == 0)
		{
			w.append("<b>HADR was not recorded.</b><br>\n");
			w.append("The database is not an HADR primary or standby (the Counter Model <code>CmHadr</code> disables itself when HADR is not used).<br>\n");
			return;
		}

		w.append(getSectionDescriptionHtml(_shortRstm, true));
		w.append("Row Count: " + _shortRstm.getRowCount() + "<br>\n");
		w.append(toHtmlTable(_shortRstm));

		w.append(getDbxCentralLinkWithDescForGraphs(false, "Below is how far behind the standby databases were during the day.",
				"CmHadr_HadrLogGapMb",
				"CmHadr_HadrReplayLagSec",
				"CmHadr_HadrLogWaitMs"
				));

		if (_CmHadr_HadrLogGapMb    != null) _CmHadr_HadrLogGapMb   .writeHtmlContent(w, null, null);
		if (_CmHadr_HadrReplayLagSec != null) _CmHadr_HadrReplayLagSec.writeHtmlContent(w, null, null);
		if (_CmHadr_HadrLogWaitMs   != null) _CmHadr_HadrLogWaitMs  .writeHtmlContent(w, null, "<b>Note</b>: In SYNC and NEARSYNC mode a commit on the primary waits for the standby: this is what that wait cost per log write.");
	}

	@Override
	public String getSubject()
	{
		return "HADR: Standby State and Lag (origin: CmHadr / MON_GET_HADR)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue (the alarms does that)
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		String schema = getReportingInstance().getDbmsSchemaName();
		String where  = "where 1 = 1 " + getReportPeriodSqlWhere().trim();

		// No mandatory table: a database without HADR is the normal case (and something the reader should be told), not a red ERROR
		if ( ! doTableExist(conn, schema, "CmHadr_abs") )
			return;

		// The standbys at the LAST sample, with the worst values of the period
		String sql = ""
			    + "select \n"
			    + "     l.[STANDBY_ID] \n"
			    + "    ,l.[HADR_ROLE] \n"
			    + "    ,l.[HADR_SYNCMODE] \n"
			    + "    ,l.[HADR_STATE] \n"
			    + "    ,l.[HADR_CONNECT_STATUS] \n"
			    + "    ,m.[samples_not_connected] \n"
			    + "    ,m.[samples_not_peer] \n"
			    + "    ,m.[samples_count] \n"
			    + "    ,l.[HADR_LOG_GAP_MB] \n"
			    + "    ,m.[HADR_LOG_GAP_MB__max] \n"
			    + "    ,l.[REPLAY_LAG_SEC] \n"
			    + "    ,m.[REPLAY_LAG_SEC__max] \n"
			    + "    ,m.[RECEIVE_LAG_SEC__max] \n"
			    + "    ,m.[AVG_LOG_HADR_WAIT_MS__max] \n"
			    + "    ,m.[STANDBY_SPOOL_PERCENT__max] \n"
			    + "    ,l.[STANDBY_REPLAY_DELAY] \n"
			    + "    ,l.[STANDBY_MEMBER_HOST] \n"
			    + "    ,l.[STANDBY_INSTANCE] \n"
			    + "    ,l.[PRIMARY_MEMBER_HOST] \n"
			    + "    ,l.[HADR_FLAGS] \n"
			    + "    ,l.[STANDBY_ERROR_TIME] \n"
			    + "    ,l.[HADR_LAST_TAKEOVER_TIME] \n"
			    + "from [CmHadr_abs] l \n"
			    + "join (select \n"
			    + "           [STANDBY_ID] \n"
			    + "          ,[LOG_STREAM_ID] \n"
			    + "          ,sum(case when [HADR_CONNECT_STATUS] <> 'CONNECTED' then 1 else 0 end) as [samples_not_connected] \n"
			    + "          ,sum(case when [HADR_STATE] <> 'PEER' then 1 else 0 end)               as [samples_not_peer] \n"
			    + "          ,count(*)                                                    as [samples_count] \n"
			    + "          ,max([HADR_LOG_GAP_MB])                                      as [HADR_LOG_GAP_MB__max] \n"
			    + "          ,max([REPLAY_LAG_SEC])                                       as [REPLAY_LAG_SEC__max] \n"
			    + "          ,max([RECEIVE_LAG_SEC])                                      as [RECEIVE_LAG_SEC__max] \n"
			    + "          ,max([AVG_LOG_HADR_WAIT_MS])                                 as [AVG_LOG_HADR_WAIT_MS__max] \n"
			    + "          ,max([STANDBY_SPOOL_PERCENT])                                as [STANDBY_SPOOL_PERCENT__max] \n"
			    + "      from [CmHadr_abs] \n"
			    + "      " + where + " \n"
			    + "      group by [STANDBY_ID], [LOG_STREAM_ID] \n"
			    + "     ) m on m.[STANDBY_ID] = l.[STANDBY_ID] and m.[LOG_STREAM_ID] = l.[LOG_STREAM_ID] \n"
			    + "where l.[SessionSampleTime] = (select max([SessionSampleTime]) from [CmHadr_abs] " + where + ") \n"
			    + "order by l.[STANDBY_ID], l.[LOG_STREAM_ID] \n"
			    + "";

		_shortRstm = executeQuery(conn, sql, true, "Db2Hadr");

		_shortRstm.setDescription(
				"The standby databases at the last collector sample, and the worst values of the period (from the table <code>CmHadr_abs</code>).<br>" +
				"Look for: <code>samples_not_connected</code> or <code>samples_not_peer</code> &gt; 0 (a takeover then can lose transactions, or is not allowed), and a high <code>REPLAY_LAG_SEC__max</code> (a takeover takes that long).<br>" +
				"");

		_shortRstm.setColumnDescription("STANDBY_ID"                , "The standby (1 = the principal standby, 2-3 = auxiliary standbys)");
		_shortRstm.setColumnDescription("HADR_ROLE"                 , "PRIMARY or STANDBY: the role of the database the collector is connected to");
		_shortRstm.setColumnDescription("HADR_SYNCMODE"             , "SYNC, NEARSYNC, ASYNC or SUPERASYNC: how long a commit waits for the standby");
		_shortRstm.setColumnDescription("HADR_STATE"                , "PEER = the standby is up to date. REMOTE_CATCHUP, LOCAL_CATCHUP = it is catching up. DISCONNECTED_PEER...");
		_shortRstm.setColumnDescription("HADR_CONNECT_STATUS"       , "CONNECTED, CONGESTED or DISCONNECTED");
		_shortRstm.setColumnDescription("samples_not_connected"     , "In how many collector samples the standby was not CONNECTED");
		_shortRstm.setColumnDescription("samples_not_peer"          , "In how many collector samples the standby was not in PEER state");
		_shortRstm.setColumnDescription("samples_count"             , "Number of collector samples in the period");
		_shortRstm.setColumnDescription("HADR_LOG_GAP_MB"           , "Log written on the primary, but not yet received by the standby, at the last sample (MB)");
		_shortRstm.setColumnDescription("HADR_LOG_GAP_MB__max"      , "The highest log gap during the period, in MB");
		_shortRstm.setColumnDescription("REPLAY_LAG_SEC"            , "How far behind the standby's replay was, at the last sample, in seconds");
		_shortRstm.setColumnDescription("REPLAY_LAG_SEC__max"       , "The highest replay lag during the period, in seconds");
		_shortRstm.setColumnDescription("RECEIVE_LAG_SEC__max"      , "The highest receive lag during the period, in seconds");
		_shortRstm.setColumnDescription("AVG_LOG_HADR_WAIT_MS__max" , "The highest average wait for the standby per log write, in ms (SYNC / NEARSYNC)");
		_shortRstm.setColumnDescription("STANDBY_SPOOL_PERCENT__max", "The fullest the standby's log spool was, in percent of HADR_SPOOL_LIMIT");
		_shortRstm.setColumnDescription("STANDBY_REPLAY_DELAY"      , "Seconds that the standby waits before it replays, on purpose (HADR_REPLAY_DELAY)");
		_shortRstm.setColumnDescription("STANDBY_MEMBER_HOST"       , "The standby host");
		_shortRstm.setColumnDescription("STANDBY_INSTANCE"          , "The standby instance");
		_shortRstm.setColumnDescription("PRIMARY_MEMBER_HOST"       , "The primary host");
		_shortRstm.setColumnDescription("HADR_FLAGS"                , "Special states: STANDBY_RECV_BLOCKED, STANDBY_LOG_DEVICE_FULL, ASSISTED_REMOTE_CATCHUP...");
		_shortRstm.setColumnDescription("STANDBY_ERROR_TIME"        , "The last time the standby had an error");
		_shortRstm.setColumnDescription("HADR_LAST_TAKEOVER_TIME"   , "The last takeover");

		// The graphs: only if they were recorded
		if (doTableExist(conn, schema, CmHadr.CM_NAME + "_" + CmHadr.GRAPH_NAME_LOG_GAP_MB))
			_CmHadr_HadrLogGapMb = createTsLineChart(conn, schema, CmHadr.CM_NAME, CmHadr.GRAPH_NAME_LOG_GAP_MB, -1, true, null, "HADR Log Gap in MB, per Standby (" + CmHadr.SHORT_NAME + ")");

		if (doTableExist(conn, schema, CmHadr.CM_NAME + "_" + CmHadr.GRAPH_NAME_REPLAY_LAG))
			_CmHadr_HadrReplayLagSec = createTsLineChart(conn, schema, CmHadr.CM_NAME, CmHadr.GRAPH_NAME_REPLAY_LAG, -1, true, null, "HADR Replay Lag in Seconds, per Standby (" + CmHadr.SHORT_NAME + ")");

		if (doTableExist(conn, schema, CmHadr.CM_NAME + "_" + CmHadr.GRAPH_NAME_LOG_WAIT_MS))
			_CmHadr_HadrLogWaitMs = createTsLineChart(conn, schema, CmHadr.CM_NAME, CmHadr.GRAPH_NAME_LOG_WAIT_MS, -1, true, null, "HADR Log Write Wait, Average ms per Wait, per Standby (" + CmHadr.SHORT_NAME + ")");
	}

	private IReportChart _CmHadr_HadrLogGapMb;     // null = not recorded
	private IReportChart _CmHadr_HadrReplayLagSec; // null = not recorded
	private IReportChart _CmHadr_HadrLogWaitMs;    // null = not recorded
}
