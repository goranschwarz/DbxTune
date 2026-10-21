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
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.dbxtune.cm.mysql;

import java.lang.invoke.MethodHandles;
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
import com.dbxtune.alarm.events.mysql.AlarmEventMyReplicationLag;
import com.dbxtune.alarm.events.mysql.AlarmEventMyReplicationStopped;
import com.dbxtune.central.pcs.CentralPersistReader;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CmSettingsHelper.IntegerInputValidator;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.config.dict.MonTablesDictionary;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.graph.TrendGraphDataPoint;
import com.dbxtune.graph.TrendGraphDataPoint.LabelType;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.sql.conn.info.DbmsVersionInfoMySql;
import com.dbxtune.utils.Configuration;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmReplication
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmReplication.class.getSimpleName();
	public static final String   SHORT_NAME       = "Replication";
	public static final String   HTML_DESC        =
		"<html>" +
		"Replication on this server, when it is a <b>replica</b>: one row per replication channel.<br>" +
		"<br>" +
		"<b>IoThread</b> is the receiver, it gets the transactions from the source. <b>SqlThread</b> is the applier, it applies them here.<br>" +
		"<b>ApplyLagSec</b> is how far behind the source this replica is, in seconds.<br>" +
		"<br>" +
		"This tab is <b>disabled</b> when the server is not a replica (no replication channel is configured when MySqlTune connects).<br>" +
		"Group Replication (InnoDB Cluster) channels are not shown.<br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {CM_NAME, "replication_connection_configuration", "replication_connection_status", "replication_applier_status", "replication_applier_status_by_worker"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {"TransactionRetries", "ReceivedHeartbeats"};

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

		return new CmReplication(counterController, guiController);
	}

	public CmReplication(ICounterController counterController, IGuiController guiController)
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
	public static final String GRAPH_NAME_APPLY_LAG = "ReplLagSec";

	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_APPLY_LAG,
			"Replication Lag in Seconds",                        // Menu CheckBox text
			"Replication Lag in Seconds, per Channel (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_SECONDS, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null,
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.REPLICATION,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("ChannelName");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		// NOTE: The timestamps in these tables are '0000-00-00 00:00:00' when nothing has happened yet. They are made NULL with "> '1971-01-01'"
		//       (sorting or MIN/MAX on them gives warning 1292 'Incorrect datetime value', and a warning makes the sample fail)
		//
		// ApplyLagSec: how old (on the source) the oldest transaction is that a worker is applying right now.
		//              If nothing is being applied, but there are received transactions that are not applied (the applier is stopped or waiting):
		//              the time since the last APPLIED transaction was committed on the source (an upper limit: we do not know when the next one was committed)
		//              Tested (8.4): the same as Seconds_Behind_Source when the applier is blocked. When the applier is stopped Seconds_Behind_Source is NULL.
		String sql = ""
			+ "select /* ${cmCollectorName} */ \n"
			+ "     IF(cc.CHANNEL_NAME = '', '(default)', cc.CHANNEL_NAME) as ChannelName \n"
			+ "    ,cc.HOST                                               as SourceHost \n"
			+ "    ,cc.PORT                                               as SourcePort \n"
			+ "    ,cs.SOURCE_UUID                                        as SourceUuid \n"
			+ "    ,cs.SERVICE_STATE                                      as IoThread \n"
			+ "    ,ap.SERVICE_STATE                                      as SqlThread \n"
			+ "    ,CAST(CASE \n"
			+ "        WHEN w.applyingAgeUs is not null \n"
			+ "            THEN w.applyingAgeUs / 1000000.0 \n"
			+ "        WHEN cs.LAST_QUEUED_TRANSACTION_ORIGINAL_COMMIT_TIMESTAMP > w.lastAppliedCommit \n"
			+ "         AND w.lastAppliedCommit > '1971-01-01' \n"
			+ "            THEN TIMESTAMPDIFF(MICROSECOND, w.lastAppliedCommit, NOW(6)) / 1000000.0 \n"
			+ "        ELSE 0 \n"
			+ "     END AS DECIMAL(12,1))                                 as ApplyLagSec \n"
			+ "    ,ac.DESIRED_DELAY                                      as DesiredDelaySec \n"
			+ "    ,ap.REMAINING_DELAY                                    as RemainingDelaySec \n"
			+ "    ,CAST(IF(cs.LAST_HEARTBEAT_TIMESTAMP > '1971-01-01', TIMESTAMPDIFF(SECOND, cs.LAST_HEARTBEAT_TIMESTAMP, NOW(6)), NULL) AS SIGNED) as SecSinceLastHeartbeat \n"
			+ "    ,w.workers                                             as Workers \n"
			+ "    ,w.workersApplying                                     as WorkersApplying \n"
			+ "    ,ap.COUNT_TRANSACTIONS_RETRIES                         as TransactionRetries \n"
			+ "    ,cs.COUNT_RECEIVED_HEARTBEATS                          as ReceivedHeartbeats \n"
			+ "    ,cs.LAST_ERROR_NUMBER                                  as IoErrorNumber \n"
			+ "    ,cs.LAST_ERROR_MESSAGE                                 as IoErrorMessage \n"
			+ "    ,IF(cs.LAST_ERROR_TIMESTAMP > '1971-01-01', cs.LAST_ERROR_TIMESTAMP, NULL) as IoErrorTime \n"
			+ "    ,COALESCE(we.LAST_ERROR_NUMBER,  co.LAST_ERROR_NUMBER,  0)  as SqlErrorNumber \n"
			+ "    ,COALESCE(we.LAST_ERROR_MESSAGE, co.LAST_ERROR_MESSAGE, '') as SqlErrorMessage \n"
			+ "    ,IF(COALESCE(we.LAST_ERROR_TIMESTAMP, co.LAST_ERROR_TIMESTAMP) > '1971-01-01', COALESCE(we.LAST_ERROR_TIMESTAMP, co.LAST_ERROR_TIMESTAMP), NULL) as SqlErrorTime \n"
			+ "    ,cs.LAST_QUEUED_TRANSACTION                            as LastQueuedTrx \n"
			+ "    ,IF(cs.LAST_QUEUED_TRANSACTION_ORIGINAL_COMMIT_TIMESTAMP > '1971-01-01', cs.LAST_QUEUED_TRANSACTION_ORIGINAL_COMMIT_TIMESTAMP, NULL) as LastQueuedTrxCommitTime \n"
			+ "    ,IF(w.lastAppliedCommit > '1971-01-01', w.lastAppliedCommit, NULL) as LastAppliedTrxCommitTime \n"
			+ "    ,cc.AUTO_POSITION                                      as AutoPosition \n"
			+ "    ,cc.USER                                               as ReplicationUser \n"
			+ "from performance_schema.replication_connection_configuration cc \n"
			+ "join performance_schema.replication_connection_status        cs on cs.CHANNEL_NAME = cc.CHANNEL_NAME \n"
			+ "left join performance_schema.replication_applier_configuration ac on ac.CHANNEL_NAME = cc.CHANNEL_NAME \n"
			+ "left join performance_schema.replication_applier_status        ap on ap.CHANNEL_NAME = cc.CHANNEL_NAME \n"
			+ "left join performance_schema.replication_applier_status_by_coordinator co on co.CHANNEL_NAME = cc.CHANNEL_NAME and co.LAST_ERROR_NUMBER <> 0 \n"
			+ "left join (select CHANNEL_NAME \n"
			+ "                 ,count(*)                                as workers \n"
			+ "                 ,sum(IF(APPLYING_TRANSACTION <> '', 1, 0)) as workersApplying \n"
			+ "                 ,max(CASE WHEN APPLYING_TRANSACTION <> '' AND APPLYING_TRANSACTION_ORIGINAL_COMMIT_TIMESTAMP > '1971-01-01' \n"
			+ "                           THEN TIMESTAMPDIFF(MICROSECOND, APPLYING_TRANSACTION_ORIGINAL_COMMIT_TIMESTAMP, NOW(6)) END) as applyingAgeUs \n"
			+ "                 ,max(LAST_APPLIED_TRANSACTION_ORIGINAL_COMMIT_TIMESTAMP) as lastAppliedCommit \n"
			+ "           from performance_schema.replication_applier_status_by_worker \n"
			+ "           group by CHANNEL_NAME) w on w.CHANNEL_NAME = cc.CHANNEL_NAME \n"
			+ "left join (select CHANNEL_NAME, LAST_ERROR_NUMBER, LAST_ERROR_MESSAGE, LAST_ERROR_TIMESTAMP \n"
			+ "                 ,row_number() over (partition by CHANNEL_NAME order by LAST_ERROR_TIMESTAMP desc) as rn \n"
			+ "           from performance_schema.replication_applier_status_by_worker \n"
			+ "           where LAST_ERROR_NUMBER <> 0) we on we.CHANNEL_NAME = cc.CHANNEL_NAME and we.rn = 1 \n"
			+ "where cc.CHANNEL_NAME not like 'group_replication%' \n"
			+ "order by cc.CHANNEL_NAME \n"
			+ "";

		return sql;
	}

	/** If we can't read the views, or this is not a replica: disable the CM, and tell why */
	@Override
	public boolean checkDependsOnOther(DbxConnection conn)
	{
		if ( ! MySqlCmUtils.checkViewsAreReadable(this, conn, "performance_schema.replication_connection_configuration", "performance_schema.replication_connection_status", "performance_schema.replication_applier_status_by_worker") )
			return false;

		DbmsVersionInfoMySql versionInfo = (DbmsVersionInfoMySql) conn.getDbmsVersionInfo();
		if ( ! versionInfo.isReplica() )
		{
			String msg = "This server is not a replica (no replication channel was configured when MySqlTune connected).";
			_logger.info("When trying to initialize Counters Model '" + getName() + "', named '" + getDisplayName() + "': " + msg + " The Counter Model is DISABLED.");
			setActive(false, msg);

			if (getTabPanel() != null)
				getTabPanel().setToolTipText("<html>" + msg + "<br>If replication is set up later, MySqlTune must reconnect to see it.</html>");

			return false;
		}

		return true;
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			mtd.addTable(CM_NAME, "Replication on this server (when it is a replica): one row per replication channel.");

			mtd.addColumn(CM_NAME, "ChannelName",              "<html>Name of the replication channel. <code>(default)</code> is the default channel (its real name is an empty string).</html>");
			mtd.addColumn(CM_NAME, "SourceHost",               "<html>The source server this channel replicates from.</html>");
			mtd.addColumn(CM_NAME, "SourcePort",               "<html>The port of the source server.</html>");
			mtd.addColumn(CM_NAME, "SourceUuid",               "<html>server_uuid of the source server.</html>");
			mtd.addColumn(CM_NAME, "IoThread",                 "<html>State of the <b>receiver</b> (IO thread), which gets the transactions from the source and writes them to the relay log.<br>ON, OFF or CONNECTING (it is trying to reconnect to the source).<br><b>Alarm</b>: IoThreadNotRunning</html>");
			mtd.addColumn(CM_NAME, "SqlThread",                "<html>State of the <b>applier</b> (SQL thread), which applies the transactions from the relay log.<br>ON or OFF.<br><b>Alarm</b>: SqlThreadNotRunning</html>");
			mtd.addColumn(CM_NAME, "ApplyLagSec",              "<html>How far behind the source this replica is, in seconds.<br>"
			                                                 + "<b>Formula</b>: When a transaction is being applied: now - the time it was committed on the source (the oldest, if several workers are applying).<br>"
			                                                 + "When nothing is being applied, but transactions are received and not yet applied (the applier is stopped or waiting): now - the time the <i>last applied</i> transaction was committed on the source. "
			                                                 + "That is an upper limit (we do not know when the next one was committed), and it grows while the applier is stopped (Seconds_Behind_Source is NULL then).<br>"
			                                                 + "Otherwise 0.<br>"
			                                                 + "<b>Note</b>: A delayed replica (DesiredDelaySec) is behind on purpose.<br>"
			                                                 + "<b>Alarm</b>: ApplyLagInSec</html>");
			mtd.addColumn(CM_NAME, "DesiredDelaySec",          "<html>SOURCE_DELAY: the replica waits this many seconds before it applies a transaction (a delayed replica).</html>");
			mtd.addColumn(CM_NAME, "RemainingDelaySec",        "<html>For a delayed replica: seconds left before the next transaction is applied. NULL if it is not waiting.</html>");
			mtd.addColumn(CM_NAME, "SecSinceLastHeartbeat",    "<html>Seconds since the last heartbeat from the source. The source sends one when it has nothing else to send (every HEARTBEAT_PERIOD seconds).<br>A big value while IoThread is ON means that the connection to the source may be broken.</html>");
			mtd.addColumn(CM_NAME, "Workers",                  "<html>Number of applier workers (replica_parallel_workers).</html>");
			mtd.addColumn(CM_NAME, "WorkersApplying",          "<html>Number of workers that are applying a transaction right now.</html>");
			mtd.addColumn(CM_NAME, "TransactionRetries",       "<html>Number of times a transaction was retried by the applier (for example after a deadlock or a lock wait timeout).</html>");
			mtd.addColumn(CM_NAME, "ReceivedHeartbeats",       "<html>Number of heartbeats received from the source.</html>");
			mtd.addColumn(CM_NAME, "IoErrorNumber",            "<html>The last error of the receiver (IO thread). 0 = no error.</html>");
			mtd.addColumn(CM_NAME, "IoErrorMessage",           "<html>The last error of the receiver (IO thread).</html>");
			mtd.addColumn(CM_NAME, "IoErrorTime",              "<html>When the last error of the receiver (IO thread) happened.</html>");
			mtd.addColumn(CM_NAME, "SqlErrorNumber",           "<html>The last error of the applier (SQL thread). From the worker with the newest error, or from the coordinator. 0 = no error.</html>");
			mtd.addColumn(CM_NAME, "SqlErrorMessage",          "<html>The last error of the applier (SQL thread).</html>");
			mtd.addColumn(CM_NAME, "SqlErrorTime",             "<html>When the last error of the applier (SQL thread) happened.</html>");
			mtd.addColumn(CM_NAME, "LastQueuedTrx",            "<html>GTID of the last transaction that was received (written to the relay log).</html>");
			mtd.addColumn(CM_NAME, "LastQueuedTrxCommitTime",  "<html>When the last received transaction was committed on the source.</html>");
			mtd.addColumn(CM_NAME, "LastAppliedTrxCommitTime", "<html>When the last applied transaction was committed on the source. The data on this replica is up to date to this time.</html>");
			mtd.addColumn(CM_NAME, "AutoPosition",             "<html>1 = GTID auto positioning is used (SOURCE_AUTO_POSITION=1).</html>");
			mtd.addColumn(CM_NAME, "ReplicationUser",          "<html>The user that the replica uses to connect to the source.</html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e);
		}
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		if (GRAPH_NAME_APPLY_LAG.equals(tgdp.getName()))
		{
			// One line per channel
			Double[] dArray = new Double[this.size()];
			String[] lArray = new String[dArray.length];
			for (int i = 0; i < dArray.length; i++)
			{
				lArray[i] = this.getAbsString(i, "ChannelName");
				dArray[i] = this.getAbsValueAsDouble(i, "ApplyLagSec");
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
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

		for (int r=0; r<cm.getAbsRowCount(); r++)
		{
			String channelName = cm.getAbsString(r, "ChannelName");
			String sourceHost  = cm.getAbsString(r, "SourceHost") + ":" + cm.getAbsString(r, "SourcePort");
			String ioThread    = cm.getAbsString(r, "IoThread");
			String sqlThread   = cm.getAbsString(r, "SqlThread");

			//-------------------------------------------------------
			// IoThreadNotRunning
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("IoThreadNotRunning") && conf.getBooleanProperty(PROPKEY_alarm_IoThreadNotRunning, DEFAULT_alarm_IoThreadNotRunning))
			{
				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest(" + cm.getName() + "): IoThreadNotRunning: channel='" + channelName + "', IoThread='" + ioThread + "'.");

				if ( ! "ON".equalsIgnoreCase(ioThread) )
				{
					int    errNum  = cm.getAbsValueAsInteger(r, "IoErrorNumber", 0);
					String errMsg  = cm.getAbsString(r, "IoErrorMessage");
					String errTime = cm.getAbsString(r, "IoErrorTime");

					AlarmEvent ae = new AlarmEventMyReplicationStopped(cm, "IoThreadNotRunning", channelName, sourceHost, ioThread, errNum, errMsg, errTime);

					// The below is called from AlarmHandler on: Raise, RE-RAISE & CANCEL
					ae.setAlarmDescriptionProvider(this, new AlarmDescriptionProviderBase(cm.getPk(), cm.getAbsPkValue(r), null)
					{
						@Override
						public void setValues(CountersModel cm, AlarmEvent alarmEvent, AlarmPhase phase)
						{
							int    rowId = cm.getAbsRowIdForPkValue(getRowPk());
							String label = "Replication receiver (IO thread) for channel '" + channelName + "'";

							if (rowId != -1)
							{
								// Always: (RAISE, RE-RAISE, CANCEL)
								String extendedDescText = cm.toTextTableString(DATA_ABS, rowId);
								String extendedDescHtml = cm.toHtmlTableString(DATA_ABS, rowId, true, false, false);
								alarmEvent.setExtendedDescription(extendedDescText, extendedDescHtml);

								// Set CANCEL message (values that was found AFTER last raise/re-raise event)
								if (AlarmPhase.CANCEL.equals(phase))
									alarmEvent.setCancelDescription(label + " is now " + cm.getAbsString(rowId, "IoThread") + ".");
							}
							else
							{
								if (AlarmPhase.CANCEL.equals(phase))
									alarmEvent.setCancelDescription(getAlarmCancelTextGone(label));
							}
						}
					});

					// Information about how to disable this alarm
					ae.createAlarmOptionsMessage(this, "IoThreadNotRunning");

					AlarmHandler.getInstance().addAlarm(ae);
				}
			} // end: IoThreadNotRunning

			//-------------------------------------------------------
			// SqlThreadNotRunning
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("SqlThreadNotRunning") && conf.getBooleanProperty(PROPKEY_alarm_SqlThreadNotRunning, DEFAULT_alarm_SqlThreadNotRunning))
			{
				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest(" + cm.getName() + "): SqlThreadNotRunning: channel='" + channelName + "', SqlThread='" + sqlThread + "'.");

				if ( ! "ON".equalsIgnoreCase(sqlThread) )
				{
					int    errNum  = cm.getAbsValueAsInteger(r, "SqlErrorNumber", 0);
					String errMsg  = cm.getAbsString(r, "SqlErrorMessage");
					String errTime = cm.getAbsString(r, "SqlErrorTime");

					AlarmEvent ae = new AlarmEventMyReplicationStopped(cm, "SqlThreadNotRunning", channelName, sourceHost, sqlThread, errNum, errMsg, errTime);

					// The below is called from AlarmHandler on: Raise, RE-RAISE & CANCEL
					ae.setAlarmDescriptionProvider(this, new AlarmDescriptionProviderBase(cm.getPk(), cm.getAbsPkValue(r), null)
					{
						@Override
						public void setValues(CountersModel cm, AlarmEvent alarmEvent, AlarmPhase phase)
						{
							int    rowId = cm.getAbsRowIdForPkValue(getRowPk());
							String label = "Replication applier (SQL thread) for channel '" + channelName + "'";

							if (rowId != -1)
							{
								// Always: (RAISE, RE-RAISE, CANCEL)
								String extendedDescText = cm.toTextTableString(DATA_ABS, rowId);
								String extendedDescHtml = cm.toHtmlTableString(DATA_ABS, rowId, true, false, false);
								extendedDescHtml += "<br><br>" + cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_APPLY_LAG, channelName);
								alarmEvent.setExtendedDescription(extendedDescText, extendedDescHtml);

								// Set CANCEL message (values that was found AFTER last raise/re-raise event)
								if (AlarmPhase.CANCEL.equals(phase))
									alarmEvent.setCancelDescription(label + " is now " + cm.getAbsString(rowId, "SqlThread") + " (lag " + toAlarmCancelValue(cm.getAbsValueAsDouble(rowId, "ApplyLagSec")) + " seconds).");
							}
							else
							{
								if (AlarmPhase.CANCEL.equals(phase))
									alarmEvent.setCancelDescription(getAlarmCancelTextGone(label));
							}
						}
					});

					// Information about how to disable this alarm
					ae.createAlarmOptionsMessage(this, "SqlThreadNotRunning");

					AlarmHandler.getInstance().addAlarm(ae);
				}
			} // end: SqlThreadNotRunning

			//-------------------------------------------------------
			// ApplyLagInSec
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("ApplyLagInSec"))
			{
				int    threshold    = conf.getIntProperty(PROPKEY_alarm_ApplyLagInSec, DEFAULT_alarm_ApplyLagInSec);
				int    desiredDelay = cm.getAbsValueAsInteger(r, "DesiredDelaySec", 0);
				Double applyLagSec  = cm.getAbsValueAsDouble(r, "ApplyLagSec");

				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest(" + cm.getName() + "): ApplyLagInSec: channel='" + channelName + "', threshold=" + threshold + ", desiredDelay=" + desiredDelay + ", ApplyLagSec='" + applyLagSec + "'.");

				// A delayed replica (SOURCE_DELAY) is behind on purpose
				if (applyLagSec != null && applyLagSec > threshold + desiredDelay)
				{
					AlarmEvent ae = new AlarmEventMyReplicationLag(cm, channelName, sourceHost, applyLagSec, sqlThread, desiredDelay, threshold);

					// The below is called from AlarmHandler on: Raise, RE-RAISE & CANCEL
					ae.setAlarmDescriptionProvider(this, new AlarmDescriptionProviderBase(cm.getPk(), cm.getAbsPkValue(r), threshold)
					{
						@Override
						public void setValues(CountersModel cm, AlarmEvent alarmEvent, AlarmPhase phase)
						{
							int    rowId = cm.getAbsRowIdForPkValue(getRowPk());
							String label = "Replication channel '" + channelName + "'";

							if (rowId != -1)
							{
								// Always: (RAISE, RE-RAISE, CANCEL) - Set graph values
								String extendedDescText = cm.toTextTableString(DATA_ABS, rowId);
								String extendedDescHtml = cm.toHtmlTableString(DATA_ABS, rowId, true, false, false);
								extendedDescHtml += "<br><br>" + cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_APPLY_LAG, channelName);
								alarmEvent.setExtendedDescription(extendedDescText, extendedDescHtml);

								// Set CANCEL message (values that was found AFTER last raise/re-raise event)
								if (AlarmPhase.CANCEL.equals(phase))
								{
									String cancelMsg = getAlarmCancelText(label + ": apply lag", toAlarmCancelValue(cm.getAbsValueAsDouble(rowId, "ApplyLagSec")), " seconds");
									alarmEvent.setCancelDescription(cancelMsg);
								}
							}
							else
							{
								if (AlarmPhase.CANCEL.equals(phase))
									alarmEvent.setCancelDescription(getAlarmCancelTextGone(label));
							}
						}
					});

					// Information about how to disable this alarm
					ae.createAlarmOptionsMessage(this, "ApplyLagInSec");

					AlarmHandler.getInstance().addAlarm(ae);
				}
			} // end: ApplyLagInSec

		} // end: loop-rows
	}

	@Override
	public boolean isGraphDataHistoryEnabled(String name)
	{
		// ENABLED for the following graphs
		if (GRAPH_NAME_APPLY_LAG.equals(name)) return true;

		// default: DISABLED
		return false;
	}

	public static final String  PROPKEY_alarm_IoThreadNotRunning  = CM_NAME + ".alarm.system.if.IoThread.isNotRunning";
	public static final boolean DEFAULT_alarm_IoThreadNotRunning  = true;

	public static final String  PROPKEY_alarm_SqlThreadNotRunning = CM_NAME + ".alarm.system.if.SqlThread.isNotRunning";
	public static final boolean DEFAULT_alarm_SqlThreadNotRunning = true;

	public static final String  PROPKEY_alarm_ApplyLagInSec       = CM_NAME + ".alarm.system.if.ApplyLagSec.gt";
	public static final int     DEFAULT_alarm_ApplyLagInSec       = 300;

	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;

		list.add(new CmSettingsHelper("IoThreadNotRunning" , isAlarmSwitch, PROPKEY_alarm_IoThreadNotRunning , Boolean.class, conf.getBooleanProperty(PROPKEY_alarm_IoThreadNotRunning , DEFAULT_alarm_IoThreadNotRunning ), DEFAULT_alarm_IoThreadNotRunning , "If 'IoThread' (the receiver) is not ON (OFF or CONNECTING) then send 'AlarmEventMyReplicationStopped'. Severity ERROR if it stopped with an error, else WARNING." ));
		list.add(new CmSettingsHelper("SqlThreadNotRunning", isAlarmSwitch, PROPKEY_alarm_SqlThreadNotRunning, Boolean.class, conf.getBooleanProperty(PROPKEY_alarm_SqlThreadNotRunning, DEFAULT_alarm_SqlThreadNotRunning), DEFAULT_alarm_SqlThreadNotRunning, "If 'SqlThread' (the applier) is not ON then send 'AlarmEventMyReplicationStopped'. Severity ERROR if it stopped with an error, else WARNING." ));
		list.add(new CmSettingsHelper("ApplyLagInSec"      , isAlarmSwitch, PROPKEY_alarm_ApplyLagInSec      , Integer.class, conf.getIntProperty    (PROPKEY_alarm_ApplyLagInSec      , DEFAULT_alarm_ApplyLagInSec      ), DEFAULT_alarm_ApplyLagInSec      , "If 'ApplyLagSec' is greater than ## seconds (+ 'DesiredDelaySec' for a delayed replica) then send 'AlarmEventMyReplicationLag'.", new IntegerInputValidator()));

		return list;
	}
}
