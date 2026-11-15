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
package com.dbxtune.cm.postgres;

import java.lang.invoke.MethodHandles;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.alarm.AlarmHandler;
import com.dbxtune.alarm.events.AlarmDescriptionProviderBase;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.alarm.events.postgres.AlarmEventPgStandbyLag;
import com.dbxtune.central.pcs.CentralPersistReader;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.config.dict.MonTablesDictionary;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.graph.TrendGraphDataPoint;
import com.dbxtune.graph.TrendGraphDataPoint.LabelType;
import com.dbxtune.gui.MainFramePostgres;
import com.dbxtune.sql.ResultSetMetaDataCached;
import com.dbxtune.sql.ResultSetMetaDataCached.Entry;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
// this can be of help: 
//    https://www.datadoghq.com/blog/postgresql-monitoring/
//    https://github.com/DataDog/the-monitor/blob/master/postgresql/postgresql-monitoring.md

public class CmPgWalReceiver
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmPgWalReceiver.class.getSimpleName();
	public static final String   SHORT_NAME       = "WAL Receiver";
	public static final String   HTML_DESC        = 
		"<html>" +
		"Only one row, showing statistics about the WAL receiver from that receiver's connected server<br>" +
		"" +
		"</html>";

	public static final String   GROUP_NAME       = MainFramePostgres.TCP_GROUP_REPLICATION;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = Ver.ver(9,6); // According to -- https://pgpedia.info/p/pg_stat_wal_receiver.html
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"pg_stat_wal_receiver"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {};

//  Version 16.2
//	RS> Col# Label                 JDBC Type Name           Guessed DBMS type Source Table        
//	RS> ---- --------------------- ------------------------ ----------------- --------------------
//	RS> 1    pid                   java.sql.Types.INTEGER   int4              pg_stat_wal_receiver
//	RS> 2    status                java.sql.Types.VARCHAR   text              pg_stat_wal_receiver
//	RS> 3    receive_start_lsn     java.sql.Types.OTHER     pg_lsn            pg_stat_wal_receiver
//	RS> 4    receive_start_tli     java.sql.Types.INTEGER   int4              pg_stat_wal_receiver
//	RS> 5    written_lsn           java.sql.Types.OTHER     pg_lsn            pg_stat_wal_receiver
//	RS> 6    flushed_lsn           java.sql.Types.OTHER     pg_lsn            pg_stat_wal_receiver
//	RS> 7    received_tli          java.sql.Types.INTEGER   int4              pg_stat_wal_receiver
//	RS> 8    last_msg_send_time    java.sql.Types.TIMESTAMP timestamptz       pg_stat_wal_receiver
//	RS> 9    last_msg_receipt_time java.sql.Types.TIMESTAMP timestamptz       pg_stat_wal_receiver
//	RS> 10   latest_end_lsn        java.sql.Types.OTHER     pg_lsn            pg_stat_wal_receiver
//	RS> 11   latest_end_time       java.sql.Types.TIMESTAMP timestamptz       pg_stat_wal_receiver
//	RS> 12   slot_name             java.sql.Types.VARCHAR   text              pg_stat_wal_receiver
//	RS> 13   sender_host           java.sql.Types.VARCHAR   text              pg_stat_wal_receiver
//	RS> 14   sender_port           java.sql.Types.INTEGER   int4              pg_stat_wal_receiver
//	RS> 15   conninfo              java.sql.Types.VARCHAR   text              pg_stat_wal_receiver

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.MEDIUM; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '" + CM_NAME + "'");

		return new CmPgWalReceiver(counterController, guiController);
	}

	public CmPgWalReceiver(ICounterController counterController, IGuiController guiController)
	{
		super(counterController, guiController,
				CM_NAME, GROUP_NAME, /*sql*/null, /*pkList*/null, 
				DIFF_COLUMNS, PCT_COLUMNS, MON_TABLES, 
				NEED_ROLES, NEED_CONFIG, NEED_SRV_VERSION, NEED_CE_VERSION, 
				NEGATIVE_DIFF_COUNTERS_TO_ZERO, IS_SYSTEM_CM, DEFAULT_POSTPONE_TIME);

		setDisplayName(SHORT_NAME);
		setDescription(HTML_DESC);

		setIconFile(GUI_ICON_FILE);

		setCounterController(counterController);
		setGuiController(guiController);
		
		addTrendGraphs();

		CounterSetTemplates.register(this);
	}

	public static final String GRAPH_NAME_REPLAY_DELAY_SEC = "ReplayDelaySec";
	public static final String GRAPH_NAME_REPLAY_BEHIND_KB = "ReplayBehindKb";

	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_REPLAY_DELAY_SEC,
			"Standby Replay Delay in Seconds",                                  // Menu CheckBox text
			"Standby Replay Delay in Seconds, behind the primary (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] {"replay_delay_sec"},
			LabelType.Static,
			TrendGraphDataPoint.Category.REPLICATION,
			false, // is Percent Graph
			false, // visible at start
			Ver.ver(10), // graph is valid from Server Version
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_REPLAY_BEHIND_KB,
			"Standby Received but not Replayed in KB",                          // Menu CheckBox text
			"Standby WAL Received but not yet Replayed in KB (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_KB, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] {"replay_behind_kb"},
			LabelType.Static,
			TrendGraphDataPoint.Category.REPLICATION,
			false, // is Percent Graph
			false, // visible at start
			Ver.ver(10), // graph is valid from Server Version
			-1);   // minimum height
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		if (getAbsRowCount() == 0 || findColumn("replay_delay_sec") == -1)
			return;

		if (GRAPH_NAME_REPLAY_DELAY_SEC.equals(tgdp.getName()))
			tgdp.setDataPoint(this.getTimestamp(), new Double[] { this.getAbsValueAsDouble(0, "replay_delay_sec", 0d) });

		if (GRAPH_NAME_REPLAY_BEHIND_KB.equals(tgdp.getName()))
			tgdp.setDataPoint(this.getTimestamp(), new Double[] { this.getAbsValueAsDouble(0, "replay_behind_kb", 0d) });
	}

	/** replay_behind_kb was > 0 in the previous sample (see ReplayDelay) */
	private boolean _prevSampleBehind = false;

	@Override
	public void sendAlarmRequest()
	{
		if ( ! hasAbsData() || getAbsRowCount() == 0 || findColumn("replay_delay_sec") == -1 )
			return;

		if ( ! AlarmHandler.hasInstance() )
			return;

		//-------------------------------------------------------
		// ReplayDelay: the standby applies the WAL late
		//-------------------------------------------------------
		if (isSystemAlarmsForColumnEnabledAndInTimeRange("ReplayDelay"))
		{
			long   threshold  = Configuration.getCombinedConfiguration().getLongProperty(PROPKEY_alarm_ReplayDelay, DEFAULT_alarm_ReplayDelay);
			Double delaySec   = getAbsValueAsDouble(0, "replay_delay_sec");
			Double behindKb   = getAbsValueAsDouble(0, "replay_behind_kb");
			String senderHost = getAbsString(0, "sender_host");

			// Behind in this AND the previous sample: pg_last_xact_replay_timestamp() is the commit time of the last APPLIED transaction,
			// so after an idle primary the first new WAL (received, applied a few ms later) shows a "delay" as long as the idle time.
			// Under load the timestamp is fresh, so a real delay is only seen when the standby stays behind.
			boolean behindNow  = behindKb != null && behindKb > 0;
			boolean behindPrev = _prevSampleBehind;
			_prevSampleBehind  = behindNow;

			if (behindNow && behindPrev && delaySec != null && delaySec > threshold)
			{
				AlarmEvent ae = new AlarmEventPgStandbyLag(this, senderHost, delaySec.longValue(), behindKb == null ? 0 : behindKb.longValue(), threshold);
				ae.setAlarmDescriptionProvider(this, new AlarmDescriptionProviderBase(null, null, threshold)
				{
					@Override
					public void setValues(CountersModel cm, AlarmEvent alarmEvent, AlarmPhase phase)
					{
						alarmEvent.setExtendedDescription("", cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_REPLAY_DELAY_SEC));
						if (AlarmPhase.CANCEL.equals(phase))
							alarmEvent.setCancelDescription(getAlarmCancelText("The standby replay delay", cm.getAbsRowCount() == 0 ? null : cm.getAbsValueAsDouble(0, "replay_delay_sec"), " seconds"));
					}
				});
				ae.createAlarmOptionsMessage(this, "ReplayDelay");
				AlarmHandler.getInstance().addAlarm(ae);
			}
		}
	}

	@Override
	public boolean isGraphDataHistoryEnabled(String name)
	{
		return GRAPH_NAME_REPLAY_DELAY_SEC.equals(name); // shown in the 'ReplayDelay' alarm
	}

	public static final String  PROPKEY_alarm_ReplayDelay = CM_NAME + ".alarm.system.if.ReplayDelay.sec.gt";
	public static final long    DEFAULT_alarm_ReplayDelay = 300;

	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		list.add(new CmSettingsHelper("ReplayDelay", CmSettingsHelper.Type.IS_ALARM_SWITCH, PROPKEY_alarm_ReplayDelay, Long.class, conf.getLongProperty(PROPKEY_alarm_ReplayDelay, DEFAULT_alarm_ReplayDelay), DEFAULT_alarm_ReplayDelay,
				"If the standby applies the WAL more than this number of seconds late ('replay_delay_sec'), send 'AlarmEventPgStandbyLag'. (Only when it is behind in two samples in a row: an idle primary is not a delay)" ));
		return list;
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addColumn("pg_stat_wal_receiver", "replay_behind_kb", "<html>WAL that this standby has received, but not yet applied (replayed), in KB: <code>pg_last_wal_receive_lsn() - pg_last_wal_replay_lsn()</code></html>");
			mtd.addColumn("pg_stat_wal_receiver", "replay_delay_sec", "<html>How old the data on this standby is, in seconds: <code>now() - pg_last_xact_replay_timestamp()</code>.<br>"
			                                                           + "<b>0 when nothing is waiting to be replayed</b> (when the primary is idle, the last replayed transaction can be old without any delay).</html>");
			mtd.addColumn("pg_stat_wal_receiver", "replay_paused",    "<html>Replay is paused (<code>pg_wal_replay_pause()</code>): the standby receives WAL, but does not apply it.</html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e);
		}
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
//	private static final String PROP_PREFIX                         = CM_NAME;
                                                                    
//	@Override
//	protected TabularCntrPanel createGui()
//	{
//		return new CmPgWalReceiverPanel(this);
//	}
	
	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

//		pkCols.add("slot_name");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		String sql = "SELECT * from pg_stat_wal_receiver ";

		// 10+: the lag seen from the standby itself. The delay is 0 while nothing waits to be replayed (an idle primary is no delay)
		if (versionInfo != null && versionInfo.getLongVersion() >= Ver.ver(10))
		{
			String sql10 = ""
				    + "SELECT r.* \n"
				    + "    ,CAST(pg_wal_lsn_diff(pg_last_wal_receive_lsn(), pg_last_wal_replay_lsn()) / 1024 AS bigint) AS replay_behind_kb \n"
				    + "    ,CAST(CASE WHEN pg_last_wal_receive_lsn() = pg_last_wal_replay_lsn() THEN 0 \n"
				    + "               ELSE coalesce(extract(epoch from (clock_timestamp() - pg_last_xact_replay_timestamp())), 0) \n"
				    + "          END AS numeric(12,1)) AS replay_delay_sec \n"
				    + "    ,pg_is_wal_replay_paused() AS replay_paused \n"
				    + "FROM pg_stat_wal_receiver r \n";

			// Dry-run (a fork may report a version, but not have the functions)
			if (conn == null || PostgresCmUtils.canExecute(conn, "select * from (" + sql10 + ") x where 1=2"))
				sql = sql10;
			else
				_logger.warn(CM_NAME + ": The SQL for version 10 and above (replay lag) failed, 'SELECT * from pg_stat_wal_receiver' is used (no replay lag, no alarm).");
		}

		return sql;
	}

	/**
	 *  FOR PCS -- Persistent Counter Storage -- change some data types
	 */
	@Override
	public ResultSetMetaDataCached modifyResultSetMetaData(ResultSetMetaDataCached rsmdc)
	{
		for (Entry entry : rsmdc.getEntries())
		{
			String colName = entry.getColumnLabel();

			//  'status'      -- from: CLOB --->>> varchar(30)
			//  'slot_name'   -- from: CLOB --->>> varchar(30)
			//  'sender_host' -- from: CLOB --->>> varchar(30)
			if (StringUtil.equalsAny(colName, "status", "slot_name", "sender_host"))
			{
				entry.setColumnType(Types.VARCHAR);
				entry.setPrecision(30);
				entry.setColumnDisplaySize(30);
			}

			//  'conninfo'    -- from: CLOB --->>> varchar(1024)
			if (StringUtil.equalsAny(colName, "conninfo"))
			{
				entry.setColumnType(Types.VARCHAR);
				entry.setPrecision(1024);
				entry.setColumnDisplaySize(1024);
			}

			//  'receive_start_lsn' -- from: JAVA_OBJECT --->>> varchar(30)
			//  'written_lsn'       -- from: JAVA_OBJECT --->>> varchar(30)
			//  'flushed_lsn'       -- from: JAVA_OBJECT --->>> varchar(30)
			//  'latest_end_lsn'    -- from: JAVA_OBJECT --->>> varchar(30)
			if (StringUtil.equalsAny(colName, "receive_start_lsn", "written_lsn", "flushed_lsn", "latest_end_lsn"))
			{
				entry.setColumnType(Types.VARCHAR);
				entry.setPrecision(30);
				entry.setColumnDisplaySize(30);
			}
		}

		return rsmdc;
	}

//	@Override
//	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
//	{
//		try 
//		{
//			String tabname = "pg_stat_wal_receiver";
//			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
//			
//			mtd.addTable(tabname, "xxxx.");
//
//			mtd.addColumn(tabname,  "xxxxxx",               "<html>xxxxx</html>");
//		}
//		catch (NameNotFoundException e) 
//		{
//			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
//		//	System.out.println("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
//		}
//	}
}
