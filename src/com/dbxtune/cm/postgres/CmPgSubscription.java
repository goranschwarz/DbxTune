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
import com.dbxtune.alarm.events.postgres.AlarmEventPgSubscriptionError;
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
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.Ver;

/**
 * Logical replication, on the SUBSCRIBER side: one row per subscription, with its apply worker and its error/conflict counters.
 */
public class CmPgSubscription
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmPgSubscription.class.getSimpleName();
	public static final String   SHORT_NAME       = "Subscriptions";
	public static final String   HTML_DESC        =
		"<html>" +
		"Logical replication, on the <b>subscriber</b>: one row per subscription (in all databases).<br>" +
		"Apply and synchronization errors, conflicts (Postgres 18 and above), and when the apply worker last heard from the publisher.<br>" +
		"<i>Empty when this server has no subscriptions.</i>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFramePostgres.TCP_GROUP_REPLICATION;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = Ver.ver(15); // pg_stat_subscription_stats
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"pg_stat_subscription_stats", "pg_stat_subscription", "pg_subscription"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
			"apply_error_count",
			"sync_error_count",                 // 15-18
			"sync_table_error_count",           // 19 and above (sync_error_count was split)
			"sync_seq_error_count",             // 19 and above
			"confl_insert_exists",              // 18 and above
			"confl_update_origin_differs",      // 18 and above
			"confl_update_exists",              // 18 and above
			"confl_update_deleted",             // 19 and above
			"confl_update_missing",             // 18 and above
			"confl_delete_origin_differs",      // 18 and above
			"confl_delete_missing",             // 18 and above
			"confl_multiple_unique_conflicts"   // 18 and above
	};

	/** The sync error columns (one in 15-18, two in 19) */
	private static final String[] SYNC_ERROR_COLUMNS = new String[] {"sync_error_count", "sync_table_error_count", "sync_seq_error_count"};

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

		return new CmPgSubscription(counterController, guiController);
	}

	public CmPgSubscription(ICounterController counterController, IGuiController guiController)
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


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	public static final String GRAPH_NAME_ERRORS       = "Errors";
	public static final String GRAPH_NAME_LAST_MSG_AGE = "LastMsgAge";

	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_ERRORS,
				"Subscription Apply + Sync Errors, per Subscription",                    // Menu CheckBox text
				"Subscription Apply + Sync Errors, per Subscription (" + SHORT_NAME + ")", // Graph Label
				TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
				null, LabelType.Dynamic,
				TrendGraphDataPoint.Category.REPLICATION,
				false, // is Percent Graph
				false, // visible at start
				Ver.ver(15), // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
				-1);   // minimum height

		addTrendGraph(GRAPH_NAME_LAST_MSG_AGE,
				"Seconds Since the Last Message from the Publisher, per Subscription",                    // Menu CheckBox text
				"Seconds Since the Last Message from the Publisher, per Subscription (" + SHORT_NAME + ")", // Graph Label
				TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
				null, LabelType.Dynamic,
				TrendGraphDataPoint.Category.REPLICATION,
				false, // is Percent Graph
				false, // visible at start
				Ver.ver(15), // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
				-1);   // minimum height
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		if (GRAPH_NAME_ERRORS.equals(tgdp.getName()))
		{
			Map<String, Double> map = new LinkedHashMap<>();
			for (int r = 0; r < this.getDiffRowCount(); r++)
			{
				String name = this.getDiffString(r, "dbname") + "." + this.getDiffString(r, "subname");
				map.put(name, (double) (getDiffLong(r, "apply_error_count") + getSyncErrors(r, DATA_DIFF)));
			}
			if ( ! map.isEmpty() )
				tgdp.setData(this.getTimestamp(), map);
		}

		if (GRAPH_NAME_LAST_MSG_AGE.equals(tgdp.getName()))
		{
			Map<String, Double> map = new LinkedHashMap<>();
			for (int r = 0; r < this.getAbsRowCount(); r++)
			{
				String name = this.getAbsString(r, "dbname") + "." + this.getAbsString(r, "subname");
				map.put(name, this.getAbsValueAsDouble(r, "last_msg_age_sec", 0d));
			}
			if ( ! map.isEmpty() )
				tgdp.setData(this.getTimestamp(), map);
		}
	}

	/** DIFF value as a long, 0 when the column is missing (another version) or NULL */
	private long getDiffLong(int r, String colName)
	{
		if (findColumn(colName) == -1)
			return 0;
		Double d = getDiffValueAsDouble(r, colName);
		return d == null ? 0 : d.longValue();
	}

	/** The sync errors: sync_error_count (15-18), sync_table_error_count + sync_seq_error_count (19 and above) */
	private long getSyncErrors(int r, int dataType)
	{
		long sum = 0;
		for (String col : SYNC_ERROR_COLUMNS)
		{
			if (findColumn(col) == -1)
				continue;
			Double d = dataType == DATA_ABS ? getAbsValueAsDouble(r, col) : getDiffValueAsDouble(r, col);
			if (d != null)
				sum += d.longValue();
		}
		return sum;
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		// A subscription name is unique per database (not in the cluster)
		List <String> pkCols = new LinkedList<String>();
		pkCols.add("dbname");
		pkCols.add("subname");
		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		long srvVersion = versionInfo == null ? 0 : versionInfo.getLongVersion();

		// The apply worker (pg_stat_subscription also has one row per table synchronization worker, and in 16+ the parallel apply workers)
		String applyWorker = "w.relid IS NULL";                                          // 15
		if (srvVersion >= Ver.ver(16)) applyWorker = "w.relid IS NULL AND w.leader_pid IS NULL"; // 16: parallel apply workers have a leader_pid
		if (srvVersion >= Ver.ver(17)) applyWorker = "w.worker_type = 'apply'";                 // 17: worker_type

		// pg_subscription and pg_stat_subscription* are cluster-wide (pg_subscription is a shared catalog): no loop over the databases.
		// NOTE: Only the columns that everybody can read in pg_subscription (subconninfo is not readable without superuser)
		return ""
			    + "SELECT \n"
			    + "     d.datname       AS dbname \n"
			    + "    ,ss.* \n" // subid, subname, the error/conflict counters (version dependent), stats_reset
			    + "    ,s.subenabled    AS enabled \n"
			    + "    ,w.pid           AS apply_pid \n"
			    + "    ,(SELECT count(*) FROM pg_stat_subscription x WHERE x.subid = s.oid AND x.relid IS NOT NULL) AS sync_workers \n"
			    + "    ,CAST(extract(epoch FROM (clock_timestamp() - w.last_msg_receipt_time)) AS numeric(12,1)) AS last_msg_age_sec \n"
			    + "    ,CAST(w.received_lsn   AS varchar(30)) AS received_lsn \n"
			    + "    ,CAST(w.latest_end_lsn AS varchar(30)) AS latest_end_lsn \n"
			    + "    ,w.last_msg_send_time \n"
			    + "    ,w.last_msg_receipt_time \n"
			    + "    ,w.latest_end_time \n"
			    + "FROM pg_stat_subscription_stats ss \n"
			    + "JOIN pg_subscription s ON s.oid = ss.subid \n"
			    + "JOIN pg_database d ON d.oid = s.subdbid \n"
			    + "LEFT JOIN pg_stat_subscription w ON w.subid = s.oid AND " + applyWorker + " \n"
			    + "";
	}

	@Override
	public void sendAlarmRequest()
	{
		if ( ! hasDiffData() )
			return;

		if ( ! AlarmHandler.hasInstance() )
			return;

		CountersModel cm = this;
		Configuration conf = Configuration.getCombinedConfiguration();

		//-------------------------------------------------------
		// SubscriptionErrors: new apply or table/sequence synchronization errors since the previous sample
		//-------------------------------------------------------
		if (isSystemAlarmsForColumnEnabledAndInTimeRange("SubscriptionErrors"))
		{
			long threshold = conf.getLongProperty(PROPKEY_alarm_SubscriptionErrors, DEFAULT_alarm_SubscriptionErrors);
			for (int r = 0; r < getDiffRowCount(); r++)
			{
				String dbname      = getDiffString(r, "dbname");
				String subname     = getDiffString(r, "subname");
				long   applyErrors = getDiffLong(r, "apply_error_count");
				long   syncErrors  = getSyncErrors(r, DATA_DIFF);

				if (applyErrors + syncErrors > threshold)
				{
					AlarmEvent ae = new AlarmEventPgSubscriptionError(cm, dbname, subname, applyErrors, syncErrors, threshold);
					ae.setAlarmDescriptionProvider(this, new AlarmDescriptionProviderBase(getPk(), getDiffPkValue(r), threshold)
					{
						@Override
						public void setValues(CountersModel cm, AlarmEvent alarmEvent, AlarmPhase phase)
						{
							alarmEvent.setExtendedDescription("", cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_ERRORS));
							if ( ! AlarmPhase.CANCEL.equals(phase) )
								return;
							String label = "New errors in subscription '" + subname + "' (database '" + dbname + "')";
							int rowId = cm.getDiffRowIdForPkValue(getRowPk());
							alarmEvent.setCancelDescription(rowId == -1
									? getAlarmCancelTextGone("Subscription '" + subname + "' (database '" + dbname + "')")
									: getAlarmCancelText(label, getDiffLong(rowId, "apply_error_count") + getSyncErrors(rowId, DATA_DIFF)));
						}
					});
					ae.createAlarmOptionsMessage(this, "SubscriptionErrors");
					AlarmHandler.getInstance().addAlarm(ae);
				}
			}
		}
	}

	@Override
	public boolean isGraphDataHistoryEnabled(String name)
	{
		return GRAPH_NAME_ERRORS.equals(name); // shown in the 'SubscriptionErrors' alarm
	}

	public static final String  PROPKEY_alarm_SubscriptionErrors = CM_NAME + ".alarm.system.if.SubscriptionErrors.gt";
	public static final long    DEFAULT_alarm_SubscriptionErrors = 0;

	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		list.add(new CmSettingsHelper("SubscriptionErrors", CmSettingsHelper.Type.IS_ALARM_SWITCH, PROPKEY_alarm_SubscriptionErrors, Long.class, conf.getLongProperty(PROPKEY_alarm_SubscriptionErrors, DEFAULT_alarm_SubscriptionErrors), DEFAULT_alarm_SubscriptionErrors,
				"If a subscription got more than this number of new apply + table/sequence synchronization errors since the previous sample, send 'AlarmEventPgSubscriptionError'." ));

		return list;
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try
		{
			String tabname = "pg_stat_subscription_stats";
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			mtd.addTable(tabname, "One row per logical replication subscription: errors and conflicts.");

			mtd.addColumn(tabname, "dbname",                          "<html>The database of the subscription (pg_subscription.subdbid).</html>");
			mtd.addColumn(tabname, "subname",                         "<html>Name of the subscription.</html>");
			mtd.addColumn(tabname, "enabled",                         "<html>pg_subscription.subenabled: the subscription is enabled (false: it was disabled, by a user or by <code>disable_on_error</code>).</html>");
			mtd.addColumn(tabname, "apply_pid",                       "<html>Process ID of the apply worker. NULL: no apply worker runs (disabled, or it just failed and waits to be restarted).</html>");
			mtd.addColumn(tabname, "sync_workers",                    "<html>Number of running table synchronization workers (the initial copy of the tables).</html>");
			mtd.addColumn(tabname, "last_msg_age_sec",                "<html>Seconds since the apply worker got the last message from the publisher: <code>clock_timestamp() - last_msg_receipt_time</code>.<br>"
			                                                        + "An idle publisher still sends keepalives (about every <code>wal_sender_timeout / 2</code>), so a high value means the connection or the publisher has a problem.</html>");
			mtd.addColumn(tabname, "subid",                           "<html>OID of the subscription.</html>");
			mtd.addColumn(tabname, "apply_error_count",               "<html>Number of times an error occurred while applying changes. The replication stops at the failing change and retries it.</html>");
			mtd.addColumn(tabname, "sync_error_count",                "<html>Number of times an error occurred during the initial table synchronization (Postgres 15-18).</html>");
			mtd.addColumn(tabname, "sync_table_error_count",          "<html>Number of times an error occurred during the initial table synchronization (Postgres 19 and above).</html>");
			mtd.addColumn(tabname, "sync_seq_error_count",            "<html>Number of times an error occurred during the sequence synchronization (Postgres 19 and above).</html>");
			mtd.addColumn(tabname, "confl_insert_exists",             "<html>Number of times a row insertion violated a NOT DEFERRABLE unique constraint (Postgres 18 and above).</html>");
			mtd.addColumn(tabname, "confl_update_origin_differs",     "<html>Number of times an update was applied to a row that had been previously modified by another source (Postgres 18 and above, needs <code>track_commit_timestamp</code>).</html>");
			mtd.addColumn(tabname, "confl_update_exists",             "<html>Number of times that an updated row value violated a NOT DEFERRABLE unique constraint (Postgres 18 and above).</html>");
			mtd.addColumn(tabname, "confl_update_deleted",            "<html>Number of times the row to be updated was already deleted by another source (Postgres 19 and above).</html>");
			mtd.addColumn(tabname, "confl_update_missing",            "<html>Number of times the row to be updated was not found (Postgres 18 and above).</html>");
			mtd.addColumn(tabname, "confl_delete_origin_differs",     "<html>Number of times a delete was applied to a row that had been previously modified by another source (Postgres 18 and above, needs <code>track_commit_timestamp</code>).</html>");
			mtd.addColumn(tabname, "confl_delete_missing",            "<html>Number of times the row to be deleted was not found (Postgres 18 and above).</html>");
			mtd.addColumn(tabname, "confl_multiple_unique_conflicts", "<html>Number of times a row insertion or an updated row values violated multiple NOT DEFERRABLE unique constraints (Postgres 18 and above).</html>");
			mtd.addColumn(tabname, "stats_reset",                     "<html>Time at which these statistics were last reset.</html>");
			mtd.addColumn(tabname, "received_lsn",                    "<html>From pg_stat_subscription (the apply worker): the last write-ahead log location received.</html>");
			mtd.addColumn(tabname, "latest_end_lsn",                  "<html>From pg_stat_subscription: the last write-ahead log location reported to the origin WAL sender.</html>");
			mtd.addColumn(tabname, "last_msg_send_time",              "<html>From pg_stat_subscription: send time of the last message received from the origin WAL sender.</html>");
			mtd.addColumn(tabname, "last_msg_receipt_time",           "<html>From pg_stat_subscription: receipt time of the last message received from the origin WAL sender.</html>");
			mtd.addColumn(tabname, "latest_end_time",                 "<html>From pg_stat_subscription: time of the last write-ahead log location reported to the origin WAL sender.</html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e);
		}
	}
}
