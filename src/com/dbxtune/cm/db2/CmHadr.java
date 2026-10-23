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
package com.dbxtune.cm.db2;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.naming.NameNotFoundException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.alarm.AlarmHandler;
import com.dbxtune.alarm.events.AlarmDescriptionProviderBase;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.alarm.events.db2.AlarmEventDb2Hadr;
import com.dbxtune.central.pcs.CentralPersistReader;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CounterSample;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.config.dict.MonTablesDictionary;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.graph.TrendGraphDataPoint;
import com.dbxtune.graph.TrendGraphDataPoint.LabelType;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.sql.conn.info.DbmsVersionInfoDb2;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.MathUtils;

/**
 * HADR (High Availability Disaster Recovery): the state of the connection to each standby, and how far behind it is (MON_GET_HADR).
 *
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmHadr
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmHadr.class.getSimpleName();
	public static final String   SHORT_NAME       = "HADR";
	public static final String   HTML_DESC        =
		"<html>" +
		"<p>HADR (High Availability Disaster Recovery): one row per standby (MON_GET_HADR on the primary).</p>" +
		"<ul>" +
		"    <li><b>HADR_CONNECT_STATUS</b> should be CONNECTED, and <b>HADR_STATE</b> PEER (in SYNC, NEARSYNC and ASYNC mode; in SUPERASYNC it is REMOTE_CATCHUP).</li>" +
		"    <li><b>HADR_LOG_GAP_MB</b>: log the primary has written that the standby has not received. <b>STANDBY_RECV_REPLAY_GAP_MB</b>: log the standby has received but not replayed.</li>" +
		"    <li><b>REPLAY_LAG_SEC</b>: how much older the standby's replayed log is than the primary's (a takeover has to replay it first).</li>" +
		"    <li><b>LOG_HADR_WAIT_TIME</b>: the time the primary's log writes waited for the standby (in SYNC/NEARSYNC every commit waits for it).</li>" +
		"</ul>" +
		"<p>The tab is disabled when the database has no HADR (HADR_ROLE STANDARD at connect).</p>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {CM_NAME, "MON_GET_HADR"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	// NOTE: HEARTBEAT_MISSED and HEARTBEAT_EXPECTED are counted since the database started (not a window)
	public static final String[] DIFF_COLUMNS     = new String[] {
		"LOG_HADR_WAIT_TIME",
		"LOG_HADR_WAITS_TOTAL",
		"HEARTBEAT_MISSED",
		"HEARTBEAT_EXPECTED"
	};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.SMALL; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '" + CM_NAME + "'");

		return new CmHadr(counterController, guiController);
	}

	public CmHadr(ICounterController counterController, IGuiController guiController)
	{
		super(counterController, guiController,
				CM_NAME, GROUP_NAME, /*sql*/null, /*pkList*/null,
				DIFF_COLUMNS, PCT_COLUMNS, MON_TABLES,
				NEED_ROLES, NEED_CONFIG, NEED_SRV_VERSION, NEED_CE_VERSION,
				NEGATIVE_DIFF_COUNTERS_TO_ZERO, IS_SYSTEM_CM, DEFAULT_POSTPONE_TIME);

		setDisplayName(SHORT_NAME);
		setDescription(HTML_DESC);

		setIconFile(GUI_ICON_FILE);

		setShowClearTime(false);

		setCounterController(counterController);
		setGuiController(guiController);

		addTrendGraphs();

		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	public static final String GRAPH_NAME_LOG_GAP_MB    = "HadrLogGapMb";
	public static final String GRAPH_NAME_REPLAY_LAG    = "HadrReplayLagSec";
	public static final String GRAPH_NAME_LOG_WAIT_MS   = "HadrLogWaitMs";

	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_LOG_GAP_MB,
			"HADR Log Gap in MB, per Standby",                                // Menu CheckBox text
			"HADR Log Gap in MB: not received + received but not replayed, per Standby (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null,
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.REPLICATION,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_REPLAY_LAG,
			"HADR Replay Lag in Seconds, per Standby",                        // Menu CheckBox text
			"HADR Replay Lag in Seconds, per Standby (" + SHORT_NAME + ")",   // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_SECONDS, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null,
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.REPLICATION,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_LOG_WAIT_MS,
			"HADR Log Write Wait, Avg ms per Wait, per Standby",              // Menu CheckBox text
			"HADR Log Write Wait, Average ms per Wait, per Standby (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MILLISEC, CentralPersistReader.SampleType.AUTO),
			null,
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.REPLICATION,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("STANDBY_ID");
		pkCols.add("LOG_STREAM_ID");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		// NOTE: The gaps are in bytes, LOG_HADR_WAIT_* in milliseconds, the times since the last receive and the timeouts in seconds
		String sql = ""
			+ "select /* ${cmCollectorName} */ \n"
			+ "     h.STANDBY_ID \n"
			+ "    ,h.LOG_STREAM_ID \n"
			+ "    ,h.HADR_ROLE \n"
			+ "    ,h.HADR_SYNCMODE \n"
			+ "    ,h.HADR_STATE \n"
			+ "    ,h.HADR_CONNECT_STATUS \n"
			+ "    ,h.HADR_CONNECT_STATUS_TIME \n"
			+ "    ,h.STANDBY_MEMBER_HOST \n"
			+ "    ,h.STANDBY_INSTANCE \n"
			+ "    ,h.PRIMARY_MEMBER_HOST \n"
			+ "    ,h.HADR_FLAGS \n"
			+ "    ,CAST(h.HADR_LOG_GAP / 1048576.0 AS DECIMAL(15,1))                          as HADR_LOG_GAP_MB \n"
			+ "    ,CAST(h.STANDBY_RECV_REPLAY_GAP / 1048576.0 AS DECIMAL(15,1))               as STANDBY_RECV_REPLAY_GAP_MB \n"
			+ "    ,SECONDS_BETWEEN(h.PRIMARY_LOG_TIME, h.STANDBY_LOG_TIME)                     as RECEIVE_LAG_SEC \n"
			+ "    ,SECONDS_BETWEEN(h.PRIMARY_LOG_TIME, h.STANDBY_REPLAY_LOG_TIME)              as REPLAY_LAG_SEC \n"
			+ "    ,h.STANDBY_REPLAY_DELAY \n"
			+ "    ,h.LOG_HADR_WAIT_CUR \n"
			+ "    ,h.LOG_HADR_WAIT_TIME \n"
			+ "    ,h.LOG_HADR_WAITS_TOTAL \n"
			+ "    ,CAST(h.LOG_HADR_WAIT_TIME * 1.0 / NULLIF(h.LOG_HADR_WAITS_TOTAL, 0) AS DECIMAL(12,3)) as AVG_LOG_HADR_WAIT_MS \n"
			+ "    ,h.HEARTBEAT_INTERVAL \n"
			+ "    ,h.HEARTBEAT_MISSED \n"
			+ "    ,h.HEARTBEAT_EXPECTED \n"
			+ "    ,h.TIME_SINCE_LAST_RECV \n"
			+ "    ,h.HADR_TIMEOUT \n"
			+ "    ,h.PEER_WINDOW \n"
			+ "    ,h.PEER_WINDOW_END \n"
			+ "    ,h.STANDBY_RECV_BUF_PERCENT \n"
			+ "    ,h.STANDBY_SPOOL_LIMIT \n"
			+ "    ,h.STANDBY_SPOOL_PERCENT \n"
			+ "    ,h.READS_ON_STANDBY_ENABLED \n"
			+ "    ,h.PRIMARY_LOG_FILE \n"
			+ "    ,h.PRIMARY_LOG_TIME \n"
			+ "    ,h.STANDBY_LOG_FILE \n"
			+ "    ,h.STANDBY_LOG_TIME \n"
			+ "    ,h.STANDBY_REPLAY_LOG_FILE \n"
			+ "    ,h.STANDBY_REPLAY_LOG_TIME \n"
			+ "    ,h.STANDBY_ERROR_TIME \n"
			+ "    ,h.HADR_LAST_TAKEOVER_TIME \n"
			+ "from table(MON_GET_HADR(-2)) h \n"
			+ "order by h.STANDBY_ID, h.LOG_STREAM_ID \n"
			+ "";

		return sql;
	}

	/** Disabled when the database has no HADR (an INFO, not a WARN: most databases has no HADR) */
	@Override
	public boolean checkDependsOnOther(DbxConnection conn)
	{
		DbmsVersionInfo versionInfo = conn.getDbmsVersionInfo();
		if (versionInfo instanceof DbmsVersionInfoDb2 && ! ((DbmsVersionInfoDb2) versionInfo).isHadr())
		{
			String msg = "The database has no HADR (HADR_ROLE is " + ((DbmsVersionInfoDb2) versionInfo).getHadrRole() + "), this tab is disabled.";
			_logger.info("When trying to initialize Counters Model '" + getName() + "', named '" + getDisplayName() + "': " + msg);
			setActive(false, msg);

			TabularCntrPanel tcp = getTabPanel();
			if (tcp != null)
				tcp.setToolTipText("<html>" + msg + "</html>");
			return false;
		}

		return Db2CmUtils.checkFunctionsAreExecutable(this, conn, "MON_GET_HADR(-2)");
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addTable(CM_NAME, "HADR: the state of each standby, and how far behind it is (MON_GET_HADR).");

			mtd.addColumn(CM_NAME, "STANDBY_ID",                 "<html>1 = the principal standby, 2 and 3 = auxiliary standbys.</html>");
			mtd.addColumn(CM_NAME, "LOG_STREAM_ID",              "<html>The log stream (0 if this is not pureScale).</html>");
			mtd.addColumn(CM_NAME, "HADR_ROLE",                  "<html>PRIMARY or STANDBY (this database).</html>");
			mtd.addColumn(CM_NAME, "HADR_SYNCMODE",              "<html>SYNC, NEARSYNC, ASYNC or SUPERASYNC: when a commit on the primary is complete (on disk on the standby, in memory on the standby, sent, or not waited for).</html>");
			mtd.addColumn(CM_NAME, "HADR_STATE",                 "<html>PEER (the standby has all log), REMOTE_CATCHUP (it reads log from the primary to catch up), REMOTE_CATCHUP_PENDING, DISCONNECTED_PEER, DISCONNECTED...<br><b>Alarm</b>: NotPeer</html>");
			mtd.addColumn(CM_NAME, "HADR_CONNECT_STATUS",        "<html>CONNECTED, CONGESTED (the network can not keep up) or DISCONNECTED.<br><b>Alarm</b>: NotConnected</html>");
			mtd.addColumn(CM_NAME, "HADR_CONNECT_STATUS_TIME",   "<html>When HADR_CONNECT_STATUS changed to what it is now.</html>");
			mtd.addColumn(CM_NAME, "STANDBY_MEMBER_HOST",        "<html>The host of the standby.</html>");
			mtd.addColumn(CM_NAME, "STANDBY_INSTANCE",           "<html>The instance of the standby.</html>");
			mtd.addColumn(CM_NAME, "PRIMARY_MEMBER_HOST",        "<html>The host of the primary.</html>");
			mtd.addColumn(CM_NAME, "HADR_FLAGS",                 "<html>Conditions, for example STANDBY_RECV_BLOCKED (the standby can not receive more log: its buffer and spool are full), ASSISTED_REMOTE_CATCHUP, STANDBY_LOG_DEVICE_FULL.</html>");
			mtd.addColumn(CM_NAME, "HADR_LOG_GAP_MB",            "<html>Log the primary has written that the standby has not received (an average), in MB.</html>");
			mtd.addColumn(CM_NAME, "STANDBY_RECV_REPLAY_GAP_MB", "<html>Log the standby has received but not replayed (an average), in MB.</html>");
			mtd.addColumn(CM_NAME, "RECEIVE_LAG_SEC",            "<html>How much older the newest log the standby has received is than the primary's newest log, in seconds.</html>");
			mtd.addColumn(CM_NAME, "REPLAY_LAG_SEC",             "<html>How much older the newest log the standby has replayed is than the primary's newest log, in seconds. A takeover has to replay it first.<br><b>Alarm</b>: ReplayLagInSec</html>");
			mtd.addColumn(CM_NAME, "STANDBY_REPLAY_DELAY",       "<html>HADR_REPLAY_DELAY: the configured delay of the replay on the standby, in seconds (0 = none). A delayed standby is behind on purpose.</html>");
			mtd.addColumn(CM_NAME, "LOG_HADR_WAIT_CUR",          "<html>How long the current log write on the primary has waited for the standby, in milliseconds (0 = it does not wait now).</html>");
			mtd.addColumn(CM_NAME, "LOG_HADR_WAIT_TIME",         "<html>Time the primary's log writes waited for the standby, in milliseconds.</html>");
			mtd.addColumn(CM_NAME, "LOG_HADR_WAITS_TOTAL",       "<html>Number of times the primary's log writes waited for the standby.</html>");
			mtd.addColumn(CM_NAME, "AVG_LOG_HADR_WAIT_MS",       "<html>Average wait for the standby per log write, in milliseconds (in Diff/Rate: for this sample).</html>");
			mtd.addColumn(CM_NAME, "HEARTBEAT_INTERVAL",         "<html>Seconds between heartbeats.</html>");
			mtd.addColumn(CM_NAME, "HEARTBEAT_MISSED",           "<html>Heartbeats that did not arrive on time, since the database started (Diff/Rate: in this sample).<br><b>Alarm</b>: HeartbeatMissed (in this sample)</html>");
			mtd.addColumn(CM_NAME, "HEARTBEAT_EXPECTED",         "<html>Heartbeats expected, since the database started (Diff/Rate: in this sample).</html>");
			mtd.addColumn(CM_NAME, "TIME_SINCE_LAST_RECV",       "<html>Seconds since the primary received anything from the standby.</html>");
			mtd.addColumn(CM_NAME, "HADR_TIMEOUT",               "<html>HADR_TIMEOUT: seconds without contact before the connection is seen as broken.</html>");
			mtd.addColumn(CM_NAME, "PEER_WINDOW",                "<html>HADR_PEER_WINDOW: seconds the primary keeps waiting for the standby (keeps PEER behaviour) after the connection is lost.</html>");
			mtd.addColumn(CM_NAME, "PEER_WINDOW_END",            "<html>When the peer window ends (when the primary stops waiting for a lost standby).</html>");
			mtd.addColumn(CM_NAME, "STANDBY_RECV_BUF_PERCENT",   "<html>How full the standby's receive buffer is, in percent.</html>");
			mtd.addColumn(CM_NAME, "STANDBY_SPOOL_LIMIT",        "<html>HADR_SPOOL_LIMIT: how much log the standby can spool to disk while the replay is behind, in 4 KB pages.</html>");
			mtd.addColumn(CM_NAME, "STANDBY_SPOOL_PERCENT",      "<html>How much of the spool is used, in percent.</html>");
			mtd.addColumn(CM_NAME, "READS_ON_STANDBY_ENABLED",   "<html>Y if the standby accepts read-only connections.</html>");
			mtd.addColumn(CM_NAME, "PRIMARY_LOG_FILE",           "<html>The primary's current log file.</html>");
			mtd.addColumn(CM_NAME, "PRIMARY_LOG_TIME",           "<html>The time of the newest log record on the primary.</html>");
			mtd.addColumn(CM_NAME, "STANDBY_LOG_FILE",           "<html>The log file the standby receives into.</html>");
			mtd.addColumn(CM_NAME, "STANDBY_LOG_TIME",           "<html>The time of the newest log record the standby has received.</html>");
			mtd.addColumn(CM_NAME, "STANDBY_REPLAY_LOG_FILE",    "<html>The log file the standby replays.</html>");
			mtd.addColumn(CM_NAME, "STANDBY_REPLAY_LOG_TIME",    "<html>The time of the newest log record the standby has replayed.</html>");
			mtd.addColumn(CM_NAME, "STANDBY_ERROR_TIME",         "<html>When the standby last had an error (see its db2diag.log). Empty = never.</html>");
			mtd.addColumn(CM_NAME, "HADR_LAST_TAKEOVER_TIME",    "<html>When the last takeover was.</html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e);
		}
	}

	/** AVG_LOG_HADR_WAIT_MS: in the diff (and rate) data, the average for this sample only */
	@Override
	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
	{
		int pos_waitTime = diffData.findColumn("LOG_HADR_WAIT_TIME");
		int pos_waits    = diffData.findColumn("LOG_HADR_WAITS_TOTAL");
		int pos_avgMs    = diffData.findColumn("AVG_LOG_HADR_WAIT_MS");

		if (pos_waitTime == -1 || pos_waits == -1 || pos_avgMs == -1)
			return;

		for (int r = 0; r < diffData.getRowCount(); r++)
		{
			long   waits    = diffData.getValueAsLong  (r, pos_waits   , 0L);
			double waitTime = diffData.getValueAsDouble(r, pos_waitTime, 0d);
			BigDecimal avgMs = waits <= 0 ? new BigDecimal(0) : MathUtils.roundToBigDecimal(waitTime / waits, 3);
			diffData.setValueAt(avgMs, r, pos_avgMs);
		}
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		if (GRAPH_NAME_LOG_GAP_MB.equals(tgdp.getName()))
		{
			Map<String, Double> map = new LinkedHashMap<>();
			for (int r = 0; r < this.getAbsRowCount(); r++)
			{
				String standby = this.getAbsString(r, "STANDBY_MEMBER_HOST");
				Double gap     = this.getAbsValueAsDouble(r, "HADR_LOG_GAP_MB");
				Double replay  = this.getAbsValueAsDouble(r, "STANDBY_RECV_REPLAY_GAP_MB");
				map.put(standby + " not received", gap    == null ? 0d : gap);
				map.put(standby + " not replayed", replay == null ? 0d : replay);
			}
			if ( ! map.isEmpty() )
				tgdp.setData(this.getTimestamp(), map);
		}

		if (GRAPH_NAME_REPLAY_LAG.equals(tgdp.getName()))
		{
			Map<String, Double> map = new LinkedHashMap<>();
			for (int r = 0; r < this.getAbsRowCount(); r++)
			{
				Double lag = this.getAbsValueAsDouble(r, "REPLAY_LAG_SEC");
				map.put(this.getAbsString(r, "STANDBY_MEMBER_HOST"), lag == null ? 0d : lag);
			}
			if ( ! map.isEmpty() )
				tgdp.setData(this.getTimestamp(), map);
		}

		if (GRAPH_NAME_LOG_WAIT_MS.equals(tgdp.getName()))
		{
			Map<String, Double> map = new LinkedHashMap<>();
			for (int r = 0; r < this.getDiffRowCount(); r++)
			{
				Double avg = this.getDiffValueAsDouble(r, "AVG_LOG_HADR_WAIT_MS");
				map.put(this.getDiffString(r, "STANDBY_MEMBER_HOST"), avg == null ? 0d : avg);
			}
			if ( ! map.isEmpty() )
				tgdp.setData(this.getTimestamp(), map);
		}
	}

	@Override
	public void sendAlarmRequest()
	{
		if ( ! hasAbsData() )
			return;

		if ( ! AlarmHandler.hasInstance() )
			return;

		boolean debugPrint = Configuration.getCombinedConfiguration().getBooleanProperty("sendAlarmRequest.debug", _logger.isDebugEnabled());

		CountersModel cm   = this;
		Configuration conf = Configuration.getCombinedConfiguration();

		for (int r = 0; r < cm.getAbsRowCount(); r++)
		{
			// Only on the primary (on a standby with reads enabled the rows describe the primary)
			if ( ! "PRIMARY".equals(cm.getAbsString(r, "HADR_ROLE")) )
				continue;

			final String standby       = cm.getAbsString(r, "STANDBY_MEMBER_HOST") + "/" + cm.getAbsString(r, "STANDBY_INSTANCE");
			final String standbyHost   = cm.getAbsString(r, "STANDBY_MEMBER_HOST");
			String       state         = cm.getAbsString(r, "HADR_STATE");
			String       connectStatus = cm.getAbsString(r, "HADR_CONNECT_STATUS");
			String       syncMode      = cm.getAbsString(r, "HADR_SYNCMODE");

			if (debugPrint || _logger.isDebugEnabled())
				System.out.println("##### sendAlarmRequest(" + cm.getName() + "): standby='" + standby + "', HADR_STATE='" + state + "', HADR_CONNECT_STATUS='" + connectStatus + "', HADR_SYNCMODE='" + syncMode + "', REPLAY_LAG_SEC=" + cm.getAbsValue(r, "REPLAY_LAG_SEC") + ", HEARTBEAT_MISSED=" + cm.getAbsValue(r, "HEARTBEAT_MISSED") + ".");

			//-------------------------------------------------------
			// NotConnected
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("NotConnected") && conf.getBooleanProperty(PROPKEY_alarm_NotConnected, DEFAULT_alarm_NotConnected))
			{
				if ( ! "CONNECTED".equals(connectStatus) )
				{
					AlarmEvent ae = new AlarmEventDb2Hadr(cm, "NotConnected", standby, state, connectStatus, syncMode, "Since " + cm.getAbsString(r, "HADR_CONNECT_STATUS_TIME") + ".");
					addHadrAlarm(ae, r, standbyHost, "NotConnected", "HADR_CONNECT_STATUS", "CONNECTED");
				}
			}

			//-------------------------------------------------------
			// NotPeer: in SYNC, NEARSYNC and ASYNC the standby should be in PEER state (in SUPERASYNC it never is)
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("NotPeer") && conf.getBooleanProperty(PROPKEY_alarm_NotPeer, DEFAULT_alarm_NotPeer))
			{
				if ("CONNECTED".equals(connectStatus) && ! "SUPERASYNC".equals(syncMode) && ! "PEER".equals(state))
				{
					AlarmEvent ae = new AlarmEventDb2Hadr(cm, "NotPeer", standby, state, connectStatus, syncMode, null);
					addHadrAlarm(ae, r, standbyHost, "NotPeer", "HADR_STATE", "PEER");
				}
			}

			//-------------------------------------------------------
			// HeartbeatMissed: in this sample (HEARTBEAT_MISSED is counted since the database started, so the DIFF value)
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("HeartbeatMissed") && hasDiffData())
			{
				int  threshold = conf.getIntProperty(PROPKEY_alarm_HeartbeatMissed, DEFAULT_alarm_HeartbeatMissed);
				int  diffRow   = cm.getRowIdWhere(DATA_DIFF, "STANDBY_MEMBER_HOST", standbyHost);
				long missed    = diffRow == -1 ? 0 : cm.getDiffValueAsLong(diffRow, "HEARTBEAT_MISSED"  , 0L);
				long expected  = diffRow == -1 ? 0 : cm.getDiffValueAsLong(diffRow, "HEARTBEAT_EXPECTED", 0L);
				if ("CONNECTED".equals(connectStatus) && missed > threshold)
				{
					AlarmEvent ae = new AlarmEventDb2Hadr(cm, "HeartbeatMissed", standby, state, connectStatus, syncMode, missed + " of " + expected + " heartbeats missed in the last sample, threshold=" + threshold);
					addHadrAlarm(ae, r, standbyHost, "HeartbeatMissed", "HADR_CONNECT_STATUS", "CONNECTED"); // (the abs HEARTBEAT_MISSED is since the start: not a cancel text)
				}
			}

			//-------------------------------------------------------
			// ReplayLagInSec (the configured HADR_REPLAY_DELAY is not counted)
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("ReplayLagInSec"))
			{
				int    threshold   = conf.getIntProperty(PROPKEY_alarm_ReplayLagInSec, DEFAULT_alarm_ReplayLagInSec);
				Double lag         = cm.getAbsValueAsDouble(r, "REPLAY_LAG_SEC");
				long   replayDelay = cm.getAbsValueAsLong(r, "STANDBY_REPLAY_DELAY", 0L);
				if (lag != null && lag - replayDelay > threshold)
				{
					AlarmEvent ae = new AlarmEventDb2Hadr(cm, standby, lag - replayDelay, cm.getAbsValueAsDouble(r, "STANDBY_RECV_REPLAY_GAP_MB"), threshold);
					addHadrAlarm(ae, r, standbyHost, "ReplayLagInSec", "REPLAY_LAG_SEC", null);
				}
			}
		}
	}

	/** The extended description (the row when it was raised), the cancel text (the value of the column now), and how to disable it */
	private void addHadrAlarm(AlarmEvent ae, int r, final String standbyHost, String alarmName, final String colName, final String okValue)
	{
		final String raiseText = toTextTableString(DATA_ABS, r);
		final String raiseHtml = toHtmlTableString(DATA_ABS, r, true, false, false);
		ae.setAlarmDescriptionProvider(this, new AlarmDescriptionProviderBase(null, null, null)
		{
			@Override
			public void setValues(CountersModel cm, AlarmEvent alarmEvent, AlarmPhase phase)
			{
				alarmEvent.setExtendedDescription(raiseText, raiseHtml);
				if (AlarmPhase.CANCEL.equals(phase))
				{
					int rowId = cm.getRowIdWhere(DATA_ABS, "STANDBY_MEMBER_HOST", standbyHost);
					if (rowId == -1)
						alarmEvent.setCancelDescription("The standby '" + standbyHost + "' is no longer in MON_GET_HADR");
					else
						alarmEvent.setCancelDescription(getAlarmCancelText(colName, cm.getAbsValue(rowId, colName), okValue == null ? "" : " (expected " + okValue + ")"));
				}
			}
		});

		ae.createAlarmOptionsMessage(this, alarmName);

		AlarmHandler.getInstance().addAlarm(ae);
	}

	public static final String  PROPKEY_alarm_NotConnected     = CM_NAME + ".alarm.system.if.HADR_CONNECT_STATUS.notConnected";
	public static final boolean DEFAULT_alarm_NotConnected     = true;

	public static final String  PROPKEY_alarm_NotPeer          = CM_NAME + ".alarm.system.if.HADR_STATE.notPeer";
	public static final boolean DEFAULT_alarm_NotPeer          = true;

	public static final String  PROPKEY_alarm_HeartbeatMissed  = CM_NAME + ".alarm.system.if.HEARTBEAT_MISSED.gt";
	public static final int     DEFAULT_alarm_HeartbeatMissed  = 1;

	public static final String  PROPKEY_alarm_ReplayLagInSec   = CM_NAME + ".alarm.system.if.REPLAY_LAG_SEC.gt";
	public static final int     DEFAULT_alarm_ReplayLagInSec   = 300;

	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;

		list.add(new CmSettingsHelper("NotConnected"   , isAlarmSwitch, PROPKEY_alarm_NotConnected   , Boolean.class, conf.getBooleanProperty(PROPKEY_alarm_NotConnected   , DEFAULT_alarm_NotConnected   ), DEFAULT_alarm_NotConnected   , "If HADR_CONNECT_STATUS is not CONNECTED (on the primary) then send 'AlarmEventDb2Hadr'." ));
		list.add(new CmSettingsHelper("NotPeer"        , isAlarmSwitch, PROPKEY_alarm_NotPeer        , Boolean.class, conf.getBooleanProperty(PROPKEY_alarm_NotPeer        , DEFAULT_alarm_NotPeer        ), DEFAULT_alarm_NotPeer        , "If the standby is connected but HADR_STATE is not PEER (and HADR_SYNCMODE is not SUPERASYNC) then send 'AlarmEventDb2Hadr'." ));
		list.add(new CmSettingsHelper("HeartbeatMissed", isAlarmSwitch, PROPKEY_alarm_HeartbeatMissed, Integer.class, conf.getIntProperty    (PROPKEY_alarm_HeartbeatMissed, DEFAULT_alarm_HeartbeatMissed), DEFAULT_alarm_HeartbeatMissed, "If more than ## heartbeats were missed in the sample (the diff of HEARTBEAT_MISSED) then send 'AlarmEventDb2Hadr'." ));
		list.add(new CmSettingsHelper("ReplayLagInSec" , isAlarmSwitch, PROPKEY_alarm_ReplayLagInSec , Integer.class, conf.getIntProperty    (PROPKEY_alarm_ReplayLagInSec , DEFAULT_alarm_ReplayLagInSec ), DEFAULT_alarm_ReplayLagInSec , "If REPLAY_LAG_SEC (minus the configured STANDBY_REPLAY_DELAY) is greater than ## seconds then send 'AlarmEventDb2Hadr'." ));

		return list;
	}
}
