/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
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
import java.util.LinkedList;
import java.util.List;

import javax.swing.JPanel;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.Version;
import com.dbxtune.alarm.AlarmHandler;
import com.dbxtune.alarm.events.AlarmEventBlockingLockAlarm;
import com.dbxtune.alarm.events.AlarmEventLongRunningTransaction;
import com.dbxtune.central.pcs.CentralPersistReader;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CmSummaryAbstract;
import com.dbxtune.cm.CounterSample;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.postgres.gui.CmSummaryPanel;
import com.dbxtune.graph.TrendGraphDataPoint;
import com.dbxtune.graph.TrendGraphDataPoint.LabelType;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmSummary
//extends CountersModel
extends CmSummaryAbstract
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmSummary.class.getSimpleName();
	public static final String   SHORT_NAME       = "Summary";
	public static final String   HTML_DESC        = 
		"<html>" +
		"Overview of how the system performs." +
		"</html>";

	public static final String   GROUP_NAME       = null;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"pg_stat_bgwriter"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"xxxx"
	};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = false;
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
			guiController.splashWindowProgress("Loading: Counter Model '"+CM_NAME+"'");

		return new CmSummary(counterController, guiController);
	}

	public CmSummary(ICounterController counterController, IGuiController guiController)
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
		
		// THIS IS THE SUMMARY CM, so set this
		counterController.setSummaryCm(this);
		
		addTrendGraphs();
		addPostRefreshTrendGraphs();

		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	public static final String GRAPH_NAME_MAX_XID                = "maxXid";
	public static final String GRAPH_NAME_OLDEST_XACT_IN_SEC     = "oldestXactInSec";
	public static final String GRAPH_NAME_OLDEST_STMNT_IN_SEC    = "oldestStmntInSec";
	public static final String GRAPH_NAME_OLDEST_STATE_IN_SEC    = "oldestStateInSec";
	public static final String GRAPH_NAME_OLDEST_COMBO_IN_SEC    = "oldestComboInSec";
	public static final String GRAPH_NAME_BLOCKING_LOCK_COUNT    = "blkLockCount";
	public static final String GRAPH_NAME_BLOCKING_MAX_WAIT_TIME = "blkMaxWaitTime";

	
	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_MAX_XID,
				"MAX Transaction ID", 	                // Menu CheckBox text
				"MAX Transaction ID ("+SHORT_NAME+")", // Graph Label 
				TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
				new String[] {"oldest_backend_xmin_age", "oldest_prepared_xact_age", "oldest_replication_slot_age", "oldest_replica_xact_age"}, 
				LabelType.Static, 
				TrendGraphDataPoint.Category.OTHER,
				false, // is Percent Graph
				true, // visible at start
				Ver.ver(9,4),     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height

		addTrendGraph(GRAPH_NAME_OLDEST_XACT_IN_SEC,
				"Oldest Open Transaction in Seconds", 	                // Menu CheckBox text
				"Oldest Open Transaction in Seconds ("+SHORT_NAME+")", // Graph Label 
				TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_SECONDS, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
				new String[] {"oldest_xact_start_in_sec"}, 
				LabelType.Static, 
				TrendGraphDataPoint.Category.OTHER,
				false, // is Percent Graph
				true, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height

		addTrendGraph(GRAPH_NAME_OLDEST_STMNT_IN_SEC,
				"Oldest Statement in Seconds", 	                // Menu CheckBox text
				"Oldest Statement in Seconds ("+SHORT_NAME+")", // Graph Label 
				TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_SECONDS, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
				new String[] {"oldest_stmnt_start_in_sec"}, 
				LabelType.Static, 
				TrendGraphDataPoint.Category.OTHER,
				false, // is Percent Graph
				true, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height

		addTrendGraph(GRAPH_NAME_OLDEST_STATE_IN_SEC,
				"Oldest State in Seconds", 	                // Menu CheckBox text
				"Oldest State in Seconds ("+SHORT_NAME+")", // Graph Label 
				TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_SECONDS, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
				null, 
				LabelType.Dynamic, 
				TrendGraphDataPoint.Category.OTHER,
				false, // is Percent Graph
				true, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height

		addTrendGraph(GRAPH_NAME_OLDEST_COMBO_IN_SEC,
				"Oldest Xact/Stmnt/State in Seconds", 	                // Menu CheckBox text
				"Oldest Xact/Stmnt/State in Seconds ("+SHORT_NAME+")", // Graph Label 
				TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_SECONDS, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
				null, 
				LabelType.Dynamic, 
				TrendGraphDataPoint.Category.OTHER,
				false, // is Percent Graph
				true, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height

		addTrendGraph(GRAPH_NAME_BLOCKING_LOCK_COUNT,
				"Blocking Locks Count", 	                // Menu CheckBox text
				"Blocking Locks Count ("+SHORT_NAME+")", // Graph Label 
				TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
				new String[] {"blocking_lock_count"}, 
				LabelType.Static, 
				TrendGraphDataPoint.Category.OTHER,
				false, // is Percent Graph
				true, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height

		addTrendGraph(GRAPH_NAME_BLOCKING_MAX_WAIT_TIME,
				"Max Wait Time for Blocking Locks in Seconds", 	                // Menu CheckBox text
				"Max Wait Time for Blocking Locks in Seconds ("+SHORT_NAME+")", // Graph Label 
				TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_SECONDS, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
				new String[] {"blocking_lock_wait_in_sec"}, 
				LabelType.Static, 
				TrendGraphDataPoint.Category.OTHER,
				false, // is Percent Graph
				true, // visible at start
				Ver.ver(14),     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height

	}

	@Override
	protected JPanel createGui()
	{
		setTabPanel(null); // Don't think this is necessary, but lets do it anyway...

		CmSummaryPanel summaryPanel = new CmSummaryPanel(this);

		// THIS IS THE SUMMARY CM, so set this
//		CounterController.getInstance().setSummaryPanel( summaryPanel );
		getCounterController().setSummaryPanel( summaryPanel );

		// add listener, so that the GUI gets updated when data changes in the CM
		addTableModelListener( summaryPanel );

		return summaryPanel;
	}

	@Override
	public boolean isRefreshable()
	{
		// The SUMMARY should ALWAYS be refreshed
		return true;
	}

	@Override
	public String[] getDependsOnConfigForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return NEED_CONFIG;
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();
		return pkCols;
	}

	public static final String  PROPKEY_getBlockingLockCount = "CmSummary.getBlockingLockCount";
	public static final boolean DEFAULT_getBlockingLockCount = true;
	
	private static int _blockingLockCount = -1;

	public static int getBlockingLockCount()
	{
		return _blockingLockCount;
	}
	
	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		long srvVersion = versionInfo.getLongVersion();

		String oldest_backend_xmin_age_cte = "";
		String oldest_backend_xmin_age_sel = "";

		String oldest_prepared_xact_age_cte = "";
		String oldest_prepared_xact_age_sel = "";
		
		String oldest_replication_slot_age_cte = "";
		String oldest_replication_slot_age_sel = "";
		
		String oldest_replica_xact_age_cte     = "";
		String oldest_replica_xact_age_sel     = "";

		if (srvVersion >= Ver.ver(9,4))
		{
			oldest_backend_xmin_age_cte     = "        ,age(backend_xmin)                                     AS oldest_backend_xmin_age \n";
			oldest_backend_xmin_age_sel     = "    ,oldest_xact.oldest_backend_xmin_age \n";

			oldest_prepared_xact_age_cte    = "        ,COALESCE( (SELECT max(age(transaction))  FROM pg_prepared_xacts)   , -1) AS oldest_prepared_xact_age    \n";
			oldest_prepared_xact_age_sel    = "    ,static_info.oldest_prepared_xact_age \n";

			oldest_replication_slot_age_cte = "        ,COALESCE( (SELECT max(age(xmin))         FROM pg_replication_slots), -1) AS oldest_replication_slot_age \n";
			oldest_replication_slot_age_sel = "    ,static_info.oldest_replication_slot_age \n";

			oldest_replica_xact_age_cte     = "        ,COALESCE( (SELECT max(age(backend_xmin)) FROM pg_stat_replication) , -1) AS oldest_replica_xact_age     \n";
			oldest_replica_xact_age_sel     = "    ,static_info.oldest_replica_xact_age \n";
		}
		
		String blockingLockCount_cte  = "";
		String blockingLockCount_sel  = "";
		String blockingLockCount_join = "";
		if (Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_getBlockingLockCount, DEFAULT_getBlockingLockCount))
		{
			blockingLockCount_cte = ", blocking as ( \n"
					+ "    select \n"
				    + "         1        AS dummy_id \n"
					+ "        ,count(*) AS blocking_lock_count \n"
					+ "    from pg_locks \n"
					+ "    where NOT granted \n"
					+ ") \n"
					+ "";

			blockingLockCount_sel = ""
					+ "     /* columns from: blocking */ \n"
					+ "    ,blocking.blocking_lock_count \n";

			blockingLockCount_join = "left outer join blocking    on static_info.dummy_id = blocking   .dummy_id \n";

			if (srvVersion >= Ver.ver(14))
			{
				blockingLockCount_cte = ", blocking as ( \n"
						+ "    select \n"
					    + "         1              AS dummy_id \n"
						+ "        ,count(*)       AS blocking_lock_count \n"
						+ "        ,max(waitstart) AS blocking_lock_wait_start \n"
						+ "        ,CAST(COALESCE( max(EXTRACT('epoch' FROM clock_timestamp()) - EXTRACT('epoch' FROM waitstart)), -1) as numeric(12,1)) AS blocking_lock_wait_in_sec \n"
						+ "    from pg_locks \n"
						+ "    where NOT granted \n"
						+ ") \n"
						+ "";

				blockingLockCount_sel = ""
						+ "     /* columns from: blocking */ \n"
						+ "    ,blocking.blocking_lock_count \n"
						+ "    ,blocking.blocking_lock_wait_start \n"
						+ "    ,blocking.blocking_lock_wait_in_sec \n";
			}
		}

		String oxact_is_waiting       = "        ,waiting                             AS oxact_is_waiting \n";
		String oxact_is_waiting_type  = "        ,CAST('-unknown-' as varchar(60))    AS oxact_is_waiting_type \n";
		String oxact_is_waiting_event = "        ,CAST('-unknown-' as varchar(60))    AS oxact_is_waiting_event \n";
		if (srvVersion >= Ver.ver(10))
		{
			oxact_is_waiting       = "        ,CASE WHEN wait_event_type IS NULL THEN true ELSE false END  AS oxact_is_waiting \n";
			oxact_is_waiting_type  = "        ,CAST(wait_event_type as varchar(60))  AS oxact_is_waiting_type \n";
			oxact_is_waiting_event = "        ,CAST(wait_event      as varchar(60))  AS oxact_is_waiting_event \n";
		}

		String sql = ""
			    + "with static_info as ( \n"
			    + "    select \n"
			    + "         1 AS dummy_id \n"
			    + "        ,cast( split_part(cast(inet_server_addr() as varchar(30)), '/', 1) || ':' || cast(inet_server_port() as varchar(10)) AS varchar(30)) as instance_name \n"
			    + "        ,cast( split_part(cast(inet_server_addr() as varchar(30)), '/', 1) || ':' || cast(inet_server_port() as varchar(10)) AS varchar(30)) as host \n"
			    + "        ,cast( version() as varchar(255))                                         AS version \n"
			    + "        ,clock_timestamp()                                                        AS time_now \n"
			    + "        ,cast( extract(epoch from (CURRENT_TIMESTAMP - CURRENT_TIMESTAMP AT TIME ZONE 'UTC'))/60 as INT) AS utc_minute_diff \n"
			    + "        ,pg_postmaster_start_time()                                               AS start_time \n"
			    + "        ,pg_is_in_recovery()                                                      AS in_recovery \n"
				+ oldest_prepared_xact_age_cte
				+ oldest_replication_slot_age_cte
				+ oldest_replica_xact_age_cte
			    + ") \n"
			    + ",oldest_xact as ( \n"
			    + "    select \n"
			    + "         1                                      AS dummy_id \n"
			    + "        ,CAST(state            as varchar(60))  AS oxact_state \n"
			    + "        ,     pid                               AS oxact_pid \n"
			    + "        ,CAST(datname          as varchar(128)) AS oxact_dbname \n"
			    + "        ,CAST(usename          as varchar(128)) AS oxact_username \n"
			    + "        ,CAST(application_name as varchar(128)) AS oxact_appname \n"
			    + oxact_is_waiting
			    + oxact_is_waiting_type
			    + oxact_is_waiting_event
			    + " \n"
			    + "        ,CAST( COALESCE( EXTRACT('epoch' FROM clock_timestamp()) - EXTRACT('epoch' FROM xact_start  ), -1) as numeric(12,1)) AS oxact_xact_start_in_sec \n"
			    + "        ,CAST( COALESCE( EXTRACT('epoch' FROM clock_timestamp()) - EXTRACT('epoch' FROM query_start ), -1) as numeric(12,1)) AS oxact_stmnt_start_in_sec \n"
			    + "        ,CAST( CASE WHEN state = 'active' \n"
			    + "                    THEN COALESCE( EXTRACT('epoch' FROM clock_timestamp()) - EXTRACT('epoch' FROM query_start ), -1) /* active -- query-elapsed-time */ \n"
			    + "                    ELSE COALESCE( EXTRACT('epoch' FROM state_change     ) - EXTRACT('epoch' FROM query_start ), -1) /* else   -- last-exec-time*/ \n"
			    + "               END as numeric(12,1)) AS oxact_stmnt_last_exec_in_sec \n"
			    + "        ,CAST( COALESCE( EXTRACT('epoch' FROM clock_timestamp()) - EXTRACT('epoch' FROM state_change), -1) as numeric(12,1)) AS oxact_in_current_state_in_sec \n"
			    + " \n"
			    + "        ,CAST( clock_timestamp() - xact_start   as varchar(30)) AS oxact_xact_start_age \n"
			    + "        ,CAST( clock_timestamp() - query_start  as varchar(30)) AS oxact_stmnt_start_age \n"
			    + "        ,CAST( CASE WHEN state = 'active' \n"
			    + "                    THEN clock_timestamp() - query_start /* active -- query-elapsed-time */ \n"
			    + "                    ELSE state_change      - query_start /* else   -- last-exec-time*/ \n"
			    + "               END as varchar(30)) AS oxact_stmnt_last_exec_age \n"
			    + "        ,CAST( clock_timestamp() - state_change as varchar(30)) AS oxact_in_current_state_age \n"
			    + " \n"
			    + oldest_backend_xmin_age_cte
			    + "    from pg_stat_activity \n"
			    + "    where xact_start IS NOT NULL \n"
			    + "      and pid   != pg_backend_pid() \n"
			    + "      and application_name != '" + Version.getAppName() + "' \n"
			    + "    order by xact_start \n"
			    + "    limit 1 \n"
			    + ") \n"
			    + blockingLockCount_cte
			    + "select \n"
				+ "     /* columns from: Some static fields */ \n"
			    + "     static_info.instance_name \n"
			    + "    ,static_info.host \n"
			    + "    ,static_info.version \n"
			    + "    ,static_info.time_now \n"
			    + "    ,static_info.utc_minute_diff \n"
			    + "    ,static_info.start_time \n"
			    + "    ,static_info.in_recovery \n"
				+ oldest_prepared_xact_age_sel
				+ oldest_replication_slot_age_sel
				+ oldest_replica_xact_age_sel
			    + " \n"
			    + blockingLockCount_sel
			    + " \n"
				+ "     /* columns from: oldest_xact */ \n"
			    + "    ,oldest_xact.oxact_state \n"
			    + "    ,oldest_xact.oxact_pid \n"
			    + "    ,oldest_xact.oxact_dbname \n"
			    + "    ,oldest_xact.oxact_username \n"
			    + "    ,oldest_xact.oxact_appname \n"
			    + "    ,oldest_xact.oxact_is_waiting \n"
			    + "    ,oldest_xact.oxact_is_waiting_type \n"
			    + "    ,oldest_xact.oxact_is_waiting_event \n"
			    + "\n"
			    + "    ,oldest_xact.oxact_xact_start_in_sec \n"
			    + "    ,oldest_xact.oxact_stmnt_start_in_sec \n"
			    + "    ,oldest_xact.oxact_stmnt_last_exec_in_sec \n"
			    + "    ,oldest_xact.oxact_in_current_state_in_sec \n"
			    + "\n"
			    + "    ,oldest_xact.oxact_xact_start_age \n"
			    + "    ,oldest_xact.oxact_stmnt_start_age \n"
			    + "    ,oldest_xact.oxact_stmnt_last_exec_age \n"
			    + "    ,oldest_xact.oxact_in_current_state_age \n"
			    + "\n"
			    + oldest_backend_xmin_age_sel
			    + "from static_info \n"
//			    + "left outer join activity    on static_info.dummy_id = activity   .dummy_id \n"
			    + "left outer join oldest_xact on static_info.dummy_id = oldest_xact.dummy_id \n"
			    + blockingLockCount_join 
			    + "";
		
		return sql;
	}
	
	@Override
	public void localCalculation(CounterSample newSample)
	{
		_blockingLockCount = newSample.getValueAsInteger(0, "blocking_lock_count", false, -1);

		// Should we DEMAND a refresh of some CM's in this sample (since the Summary CM is first in the list)
		// Check LOCK WAITS and: request refresh of CmPgLocks, CmActiveStatements
		if (_blockingLockCount > 0)
		{
			getCounterController().addCmToDemandRefreshList(CmPgLocks.CM_NAME);
			getCounterController().addCmToDemandRefreshList(CmActiveStatements.CM_NAME);
		}
	}
	
	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		//---------------------------------
		// GRAPH:
		//---------------------------------
		if (GRAPH_NAME_MAX_XID.equals(tgdp.getName()))
		{
			Double[] arr = new Double[4];

			arr[0] = this.getAbsValueAsDouble(0, "oldest_backend_xmin_age",     -1d);
			arr[1] = this.getAbsValueAsDouble(0, "oldest_prepared_xact_age",    -1d);
			arr[2] = this.getAbsValueAsDouble(0, "oldest_replication_slot_age", -1d);
			arr[3] = this.getAbsValueAsDouble(0, "oldest_replica_xact_age",     -1d);

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		if (GRAPH_NAME_OLDEST_XACT_IN_SEC.equals(tgdp.getName()))
		{
			Double[] arr = new Double[1];

			arr[0] = this.getAbsValueAsDouble(0, "oxact_xact_start_in_sec", 0d);

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		if (GRAPH_NAME_OLDEST_STMNT_IN_SEC.equals(tgdp.getName()))
		{
			Double[] arr = new Double[1];

			arr[0] = this.getAbsValueAsDouble(0, "oxact_stmnt_last_exec_in_sec", 0d);

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		if (GRAPH_NAME_OLDEST_STATE_IN_SEC.equals(tgdp.getName()))
		{
			String[] larr = new String[1];
			Double[] darr = new Double[1];

			larr[0] = this.getAbsString       (0, "oxact_state", true, "none");
			darr[0] = this.getAbsValueAsDouble(0, "oxact_in_current_state_in_sec", 0d);

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), larr, darr);
		}

		if (GRAPH_NAME_OLDEST_COMBO_IN_SEC.equals(tgdp.getName()))
		{
			String[] larr = new String[3];
			Double[] darr = new Double[3];

			larr[0] = "xact_start_in_sec";
			darr[0] = this.getAbsValueAsDouble(0, "oxact_xact_start_in_sec", 0d);

			larr[1] = "stmnt_exec_in_sec";
			darr[1] = this.getAbsValueAsDouble(0, "oxact_stmnt_last_exec_in_sec", 0d);

			String state = this.getAbsString(0, "oxact_state", true, "none");
			larr[2] = "in_current_state_in_sec[" + state + "]";
			darr[2] = this.getAbsValueAsDouble(0, "oxact_in_current_state_in_sec", 0d);

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), larr, darr);
		}

		if (GRAPH_NAME_BLOCKING_LOCK_COUNT.equals(tgdp.getName()))
		{
			Double[] arr = new Double[1];

			arr[0] = this.getAbsValueAsDouble(0, "blocking_lock_count", 0d);

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		if (GRAPH_NAME_BLOCKING_MAX_WAIT_TIME.equals(tgdp.getName()))
		{
			Double[] arr = new Double[1];

			arr[0] = this.getAbsValueAsDouble(0, "blocking_lock_wait_in_sec", 0d);

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}
	}

	@Override
	public boolean isGraphDataHistoryEnabled(String name)
	{
		// ENABLED for the following graphs
//		if (GRAPH_NAME_MAX_XID               .equals(name)) return true;
//		if (GRAPH_NAME_OLDEST_XACT_IN_SEC    .equals(name)) return true;
//		if (GRAPH_NAME_OLDEST_STMNT_IN_SEC   .equals(name)) return true;
//		if (GRAPH_NAME_OLDEST_STATE_IN_SEC   .equals(name)) return true;
		if (GRAPH_NAME_OLDEST_COMBO_IN_SEC   .equals(name)) return true;
//		if (GRAPH_NAME_BLOCKING_LOCK_COUNT   .equals(name)) return true;
//		if (GRAPH_NAME_BLOCKING_MAX_WAIT_TIME.equals(name)) return true;

		// default: DISABLED
		return false;
	}
	@Override
	public int getGraphDataHistoryTimeInterval(String name)
	{
		// Keep interval: default is 60 minutes
		return super.getGraphDataHistoryTimeInterval(name);
	}
	

	//----------------------------------------------------------------
	// ALARMS
	//----------------------------------------------------------------
	@Override
	public void sendAlarmRequest()
	{
		if ( ! hasDiffData() )
			return;

		if ( ! AlarmHandler.hasInstance() )
			return;

		CountersModel cm = this;

		boolean debugPrint = Configuration.getCombinedConfiguration().getBooleanProperty("sendAlarmRequest.debug", _logger.isDebugEnabled());

		//-------------------------------------------------------
		// DbmsVersionStringChanged
		//-------------------------------------------------------
		doAlarmIfDbmsVersionStringWasChanged("version");

		//-------------------------------------------------------
		// BlockingLockCount
		//-------------------------------------------------------
		if (isSystemAlarmsForColumnEnabledAndInTimeRange("BlockingLockCount"))
		{
			Integer blocking_lock_count = cm.getAbsValueAsInteger(0, "blocking_lock_count");
			if (blocking_lock_count != null)
			{
				Double blocking_lock_wait_in_sec = cm.getAbsValueAsDouble(0, "blocking_lock_wait_in_sec", true, -1d);
				
				int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_BlockingLockCount, DEFAULT_alarm_BlockingLockCount);

				double waitThreshold = Configuration.getCombinedConfiguration().getDoubleProperty(PROPKEY_alarm_BlockingLockCount_minWaitTimeSec, DEFAULT_alarm_BlockingLockCount_minWaitTimeSec);

				double tmpBlockingLockWaitInSec = blocking_lock_wait_in_sec;
				if (tmpBlockingLockWaitInSec < 0.0)
					tmpBlockingLockWaitInSec = 999999d;
				
				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest("+cm.getName()+"): threshold="+threshold+", blocking_lock_count='"+blocking_lock_count+"', waitThreshold="+waitThreshold+", blocking_lock_wait_in_sec='"+blocking_lock_wait_in_sec+"'.");

				
				if (blocking_lock_count > threshold && tmpBlockingLockWaitInSec > waitThreshold)
					AlarmHandler.getInstance().addAlarm( new AlarmEventBlockingLockAlarm(cm, waitThreshold, blocking_lock_count, blocking_lock_wait_in_sec) );
			}
		}

		//-------------------------------------------------------
		// oldestOpenTranInSec
		//-------------------------------------------------------
		if (isSystemAlarmsForColumnEnabledAndInTimeRange("oldestOpenTranInSec"))
		{
			Integer oldest_xact_start_in_sec = cm.getAbsValueAsInteger(0, "oldest_xact_start_in_sec");
			if (oldest_xact_start_in_sec != null)
			{
				int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_oldestOpenTranInSec, DEFAULT_alarm_oldestOpenTranInSec);

				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest("+cm.getName()+"): threshold="+threshold+", oldest_xact_start_in_sec='"+oldest_xact_start_in_sec+"'.");

				if (oldest_xact_start_in_sec > threshold)
					AlarmHandler.getInstance().addAlarm( new AlarmEventLongRunningTransaction(cm, threshold, "-unknown-", oldest_xact_start_in_sec, "-unknown-") );
			}
		}

		//-------------------------------------------------------
		// oldestStatementInSec
		//-------------------------------------------------------
		if (isSystemAlarmsForColumnEnabledAndInTimeRange("oldestStatementInSec"))
		{
			Integer oldest_stmnt_start_in_sec = cm.getAbsValueAsInteger(0, "oldest_stmnt_start_in_sec");
			if (oldest_stmnt_start_in_sec != null)
			{
				int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_oldestStatementInSec, DEFAULT_alarm_oldestStatementInSec);

				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest("+cm.getName()+"): threshold="+threshold+", oldest_xact_start_in_sec='"+oldest_stmnt_start_in_sec+"'.");

				if (oldest_stmnt_start_in_sec > threshold)
					AlarmHandler.getInstance().addAlarm( new AlarmEventLongRunningTransaction(cm, threshold, "-unknown-", oldest_stmnt_start_in_sec, "-unknown-") );
			}
		}
	}

	
	
	public static final String  PROPKEY_alarm_BlockingLockCount                = CM_NAME + ".alarm.system.if.blocking_lock_count.gt";
	public static final int     DEFAULT_alarm_BlockingLockCount                = 0;

	public static final String  PROPKEY_alarm_BlockingLockCount_minWaitTimeSec = CM_NAME + ".alarm.system.if.blocking_lock_count.minWaitTimeSec";
	public static final double  DEFAULT_alarm_BlockingLockCount_minWaitTimeSec = 60.0;

	public static final String  PROPKEY_alarm_oldestOpenTranInSec              = CM_NAME + ".alarm.system.if.oldestOpenTranInSec.gt";
//	public static final int     DEFAULT_alarm_oldestOpenTranInSec              = 60 * 5; // 5 Minutes
	public static final int     DEFAULT_alarm_oldestOpenTranInSec              = CmActiveStatements.DEFAULT_alarm_OpenXactInSec; // 30 minutes

//	public static final String  PROPKEY_alarm_oldestOpenTranInSecSkipTranName  = CM_NAME + ".alarm.system.if.oldestOpenTranInSec.skip.tranName";
//	public static final String  DEFAULT_alarm_oldestOpenTranInSecSkipTranName  = "^(DUMP |\\$dmpxact).*";

	public static final String  PROPKEY_alarm_oldestStatementInSec             = CM_NAME + ".alarm.system.if.oldestStatementInSec.gt";
//	public static final int     DEFAULT_alarm_oldestStatementInSec             = 60 * 10; // 10 Minutes
	public static final int     DEFAULT_alarm_oldestStatementInSec             = CmActiveStatements.DEFAULT_alarm_StatementExecInSec; // 3 hours


	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		
		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;

		addAlarmSettings_DbmsVersionStringChanged(list, "version");
		list.add(new CmSettingsHelper("BlockingLockCount",                isAlarmSwitch, PROPKEY_alarm_BlockingLockCount               , Integer.class, conf.getIntProperty    (PROPKEY_alarm_BlockingLockCount               , DEFAULT_alarm_BlockingLockCount               ), DEFAULT_alarm_BlockingLockCount               , "If 'BlockingLockCount' is greater than ## then send 'AlarmEventBlockingLockAlarm'." ));
		list.add(new CmSettingsHelper("BlockingLockCount MinWaitTime",                   PROPKEY_alarm_BlockingLockCount_minWaitTimeSec, Double .class, conf.getDoubleProperty (PROPKEY_alarm_BlockingLockCount_minWaitTimeSec, DEFAULT_alarm_BlockingLockCount_minWaitTimeSec), DEFAULT_alarm_BlockingLockCount_minWaitTimeSec, "If 'BlockingLockCount' is true; then we can filter on 'blocking_lock_wait_in_sec' (only if Postgres is version 14 or above)"));
		list.add(new CmSettingsHelper("oldestOpenTranInSec",              isAlarmSwitch, PROPKEY_alarm_oldestOpenTranInSec             , Integer.class, conf.getIntProperty    (PROPKEY_alarm_oldestOpenTranInSec             , DEFAULT_alarm_oldestOpenTranInSec             ), DEFAULT_alarm_oldestOpenTranInSec             , "If 'oldestOpenTranInSec' is greater than ## then send 'AlarmEventLongRunningTransaction'." ));
//		list.add(new CmSettingsHelper("oldestOpenTranInSec SkipTranName",                PROPKEY_alarm_oldestOpenTranInSecSkipTranName , String .class, conf.getProperty       (PROPKEY_alarm_oldestOpenTranInSecSkipTranName , DEFAULT_alarm_oldestOpenTranInSecSkipTranName ), DEFAULT_alarm_oldestOpenTranInSecSkipTranName , "If 'oldestOpenTranInSec' is true; then we can filter out transaction names using a Regular expression... if (tranName.matches('regexp'))... This to remove alarms of 'DUMP DATABASE' or similar. A good place to test your regexp is 'http://www.regexplanet.com/advanced/java/index.html'.", new RegExpInputValidator()));
		list.add(new CmSettingsHelper("oldestStatementInSec",             isAlarmSwitch, PROPKEY_alarm_oldestStatementInSec            , Integer.class, conf.getIntProperty    (PROPKEY_alarm_oldestStatementInSec            , DEFAULT_alarm_oldestStatementInSec            ), DEFAULT_alarm_oldestStatementInSec            , "If 'oldestStatementInSec' is greater than ## then send 'AlarmEventLongRunningStatement'." ));

		return list;
	}
}
