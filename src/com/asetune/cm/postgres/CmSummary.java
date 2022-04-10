/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
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
package com.asetune.cm.postgres;

import java.util.LinkedList;
import java.util.List;

import javax.swing.JPanel;

import org.apache.log4j.Logger;

import com.asetune.CounterController;
import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.postgres.gui.CmSummaryPanel;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.graph.TrendGraphDataPoint.LabelType;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmSummary
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmSummary.class);
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
		super(counterController,
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

		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	public static final String GRAPH_NAME_MAX_XID            = "maxXid";
//	public static final String GRAPH_NAME_AA_NW_PACKET       = "aaPacketGraph";      // String x=GetCounters.CM_GRAPH_NAME__SUMMARY__AA_NW_PACKET;

	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_MAX_XID,
				"MAX Transaction ID", 	                // Menu CheckBox text
				"MAX Transaction ID ("+SHORT_NAME+")", // Graph Label 
				TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL,
				new String[] {"oldest_running_xact_age", "oldest_prepared_xact_age", "oldest_replication_slot_age", "oldest_replica_xact_age"}, 
				LabelType.Static, 
				TrendGraphDataPoint.Category.OTHER,
				false, // is Percent Graph
				true, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height

	}

	@Override
	protected JPanel createGui()
	{
		setTabPanel(null); // Don't think this is necessary, but lets do it anyway...

		CmSummaryPanel summaryPanel = new CmSummaryPanel(this);

		// THIS IS THE SUMMARY CM, so set this
		CounterController.getInstance().setSummaryPanel( summaryPanel );

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
	public String[] getDependsOnConfigForVersion(DbxConnection conn, long srvVersion, boolean isClusterEnabled)
	{
		return NEED_CONFIG;
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, long srvVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();
		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, long srvVersion, boolean isClusterEnabled)
	{
		String oldest_running_xact_age     = "";
		String oldest_prepared_xact_age    = "";
		String oldest_replication_slot_age = "";
		String oldest_replica_xact_age     = "";

		if (srvVersion >= Ver.ver(9,4))
		{
			oldest_running_xact_age     = "    ,COALESCE( (SELECT max(age(backend_xmin)) FROM pg_stat_activity  WHERE state != 'idle' ), -1) AS oldest_running_xact_age     \n";
			oldest_prepared_xact_age    = "    ,COALESCE( (SELECT max(age(transaction))  FROM pg_prepared_xacts)                       , -1) AS oldest_prepared_xact_age    \n";
			oldest_replication_slot_age = "    ,COALESCE( (SELECT max(age(xmin))         FROM pg_replication_slots)                    , -1) AS oldest_replication_slot_age \n";
			oldest_replica_xact_age     = "    ,COALESCE( (SELECT max(age(backend_xmin)) FROM pg_stat_replication)                     , -1) AS oldest_replica_xact_age     \n";
		}

		return 
				"select \n" + 
//				"    *  \n" +
				"     cast( split_part(cast(inet_server_addr() as varchar(30)), '/', 1) || ':' || cast(inet_server_port() as varchar(10)) as varchar(30)) as instance_name\n" +
				"    ,cast( split_part(cast(inet_server_addr() as varchar(30)), '/', 1) || ':' || cast(inet_server_port() as varchar(10)) as varchar(30)) as host\n" +
				"    ,cast( version() as varchar(255))   as version \n" +
				"    ,CURRENT_TIMESTAMP                  as time_now \n" + 
				"    ,cast( extract(epoch from (CURRENT_TIMESTAMP - CURRENT_TIMESTAMP AT TIME ZONE 'UTC'))/60 as INT) as utc_minute_diff \n" +
				"    ,pg_postmaster_start_time()         as start_time \n" +
				"    ,pg_is_in_recovery()                as in_recovery \n" +
				"\n" +
				oldest_running_xact_age     +
				oldest_prepared_xact_age    +
				oldest_replication_slot_age +
				oldest_replica_xact_age     +
//				"from pg_catalog.pg_stat_bgwriter \n" +
				"";
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

			arr[0] = this.getAbsValueAsDouble(0, "oldest_running_xact_age",     -1d);
			arr[1] = this.getAbsValueAsDouble(0, "oldest_prepared_xact_age",    -1d);
			arr[2] = this.getAbsValueAsDouble(0, "oldest_replication_slot_age", -1d);
			arr[3] = this.getAbsValueAsDouble(0, "oldest_replica_xact_age",     -1d);

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}
	}


	//----------------------------------------------------------------
	// TODO: possibly add ALARM if any of the "old transaction" is "to big" so that transaction-wrap-around is eminent (which causes the DBMS server to STOP accepting Writes)
	//----------------------------------------------------------------
//	@Override
//	public void sendAlarmRequest()
//	{
//		if ( ! hasDiffData() )
//			return;
//
//		if ( ! AlarmHandler.hasInstance() )
//			return;
//
//		CountersModel cm = this;
//
//		boolean debugPrint = Configuration.getCombinedConfiguration().getBooleanProperty("sendAlarmRequest.debug", _logger.isDebugEnabled());
//
//		//-------------------------------------------------------
//		// Blocking Locks
//		//-------------------------------------------------------
//		if (isSystemAlarmsForColumnEnabledAndInTimeRange("OldestRunningXactAge"))
//		{
//			Double oldest_running_xact_age = cm.getAbsValueAsDouble (0, "oldest_running_xact_age");
//			if (oldest_running_xact_age != null)
//			{
//				int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_OldestRunningXactAge, DEFAULT_alarm_OldestRunningXactAge);
//
//				if (debugPrint || _logger.isDebugEnabled())
//					System.out.println("##### sendAlarmRequest("+cm.getName()+"): threshold="+threshold+", oldest_running_xact_age='"+oldest_running_xact_age+"'.");
//
//				if (oldest_running_xact_age.intValue() > threshold)
//					AlarmHandler.getInstance().addAlarm( new AlarmEventXxxAlarm(cm, threshold, oldest_running_xact_age) );
//			}
//		}
//	}
//
//	public static final String  PROPKEY_alarm_OldestRunningXactAge = CM_NAME + ".alarm.system.if.OldestRunningXactAge.gt"; // this should probably be in PERCENT of 2 billion instead of a number
//	public static final int     DEFAULT_alarm_OldestRunningXactAge = Integer.MAX_VALUE * 0.75;
//
//	@Override
//	public List<CmSettingsHelper> getLocalAlarmSettings()
//	{
//		Configuration conf = Configuration.getCombinedConfiguration();
//		List<CmSettingsHelper> list = new ArrayList<>();
//		
//		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;
//		
//		list.add(new CmSettingsHelper("OldestRunningXactAge", isAlarmSwitch, PROPKEY_alarm_OldestRunningXactAge, Integer.class, conf.getIntProperty(PROPKEY_alarm_OldestRunningXactAge, DEFAULT_alarm_OldestRunningXactAge), DEFAULT_alarm_OldestRunningXactAge, "If 'OldestRunningXactAge' is greater than ## then send 'AlarmEventXxxAlarm'." ));
//
//		return list;
//	}
}
