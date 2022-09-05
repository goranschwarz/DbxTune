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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.alarm.AlarmHandler;
import com.asetune.alarm.events.AlarmEvent;
import com.asetune.alarm.events.postgres.AlarmEventPgReplicationAge;
import com.asetune.alarm.events.postgres.AlarmEventPgReplicationLag;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.graph.TrendGraphDataPoint.LabelType;
import com.asetune.gui.MainFrame;
import com.asetune.gui.ResultSetTableModel;
import com.asetune.gui.TrendGraph;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.conn.info.DbmsVersionInfo;
import com.asetune.utils.Configuration;
import com.asetune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
// this can be of help: 
//    https://www.datadoghq.com/blog/postgresql-monitoring/
//    https://github.com/DataDog/the-monitor/blob/master/postgresql/postgresql-monitoring.md

public class CmPgReplication
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmPgReplication.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmPgReplication.class.getSimpleName();
	public static final String   SHORT_NAME       = "Replication";
	public static final String   HTML_DESC        = 
		"<html>" +
		"One row per Replication destination/endpoint." +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"pg_stat_replication"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {};
//	RS> Col# Label            JDBC Type Name           Guessed DBMS type Source Table       
//	RS> ---- ---------------- ------------------------ ----------------- -------------------
//	RS> 1    pid              java.sql.Types.INTEGER   int4              pg_stat_replication
//	RS> 2    client_addr      java.sql.Types.OTHER     inet              pg_stat_replication
//	RS> 3    client_port      java.sql.Types.INTEGER   int4              pg_stat_replication
//	RS> 4    client_hostname  java.sql.Types.VARCHAR   text              pg_stat_replication
//	RS> 5    usename          java.sql.Types.VARCHAR   text              pg_stat_replication
//	RS> 6    usesysid         java.sql.Types.BIGINT    oid               pg_stat_replication
//	RS> 7    application_name java.sql.Types.VARCHAR   text              pg_stat_replication
//	RS> 8    backend_start    java.sql.Types.TIMESTAMP timestamptz       pg_stat_replication
//	RS> 9    backend_xmin     java.sql.Types.OTHER     xid               pg_stat_replication
//	RS> 10   state            java.sql.Types.VARCHAR   text              pg_stat_replication
//	RS> 11   sync_state       java.sql.Types.VARCHAR   text              pg_stat_replication
//	RS> 12   pending_kb       java.sql.Types.BIGINT    int8              -none-             
//	RS> 13   write_kb         java.sql.Types.BIGINT    int8              -none-             
//	RS> 14   flush_kb         java.sql.Types.BIGINT    int8              -none-             
//	RS> 15   replay_kb        java.sql.Types.BIGINT    int8              -none-             
//	RS> 16   total_lag_kb     java.sql.Types.BIGINT    int8              -none-             	

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
			guiController.splashWindowProgress("Loading: Counter Model '"+CM_NAME+"'");

		return new CmPgReplication(counterController, guiController);
	}

	public CmPgReplication(ICounterController counterController, IGuiController guiController)
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
		
		addTrendGraphs();
		
		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
//	private static final String PROP_PREFIX                         = CM_NAME;
                                                                    
	public static final String GRAPH_NAME_TOTAL_LAG       = "TotalLag";
	public static final String GRAPH_NAME_REP_AGE_SECONDS = "RepAgeSec";
	
//	@Override
//	protected TabularCntrPanel createGui()
//	{
//		return new CmPgReplicationPanel(this);
//	}
	
	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("client_addr");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		boolean has_backend_xmin       = false; // This was introduced in 9.4
		boolean has_column__sent_lsn   = false; // This was introduced in 10.0
		boolean has_column__reply_time = false; // This was introduced in 12.0 ???
		
		if (conn != null)
		{
			try 
			{
				String checkSql = "select * from pg_stat_replication where 1=2";
				ResultSetTableModel dummyRstm = ResultSetTableModel.executeQuery(conn, checkSql, "dummy");
				
				has_backend_xmin       = dummyRstm.hasColumn("backend_xmin");
				has_column__sent_lsn   = dummyRstm.hasColumn("sent_lsn");
				has_column__reply_time = dummyRstm.hasColumn("reply_time");
			}
			catch (SQLException ex)
			{
				_logger.error("Problems checking for column name 'sent_lsn' on table 'pg_stat_replication'. ErrorNum=" + ex.getErrorCode(), ex);
			}
		}
		else
		{
			if (versionInfo.getLongVersion() >= Ver.ver(9,4))
				has_backend_xmin = true;

			if (versionInfo.getLongVersion() >= Ver.ver(10))
				has_column__sent_lsn   = true;

			if (versionInfo.getLongVersion() >= Ver.ver(12))
				has_column__reply_time = true;
		}

		// column: 'backend_xmin'
		String backend_xmin = "";
		if (has_backend_xmin)
		{
			backend_xmin = "    ,cast(backend_xmin     as varchar(30)) AS backend_xmin \n";
		}
		
		// column: 'reply_time', 'reply_time_seconds'
		String reply_time = "";
		String reply_time_seconds = "";
		if (has_column__reply_time)
		{
			reply_time =         "    ,reply_time \n";
			reply_time_seconds = "    ,EXTRACT(EPOCH FROM (clock_timestamp() - reply_time))::int AS reply_time_seconds \n";
		}
		
		// SQL -- https://dataegret.com/2017/04/deep-dive-into-postgres-stats-pg_stat_replication/
		String sql_9 = ""
			    + "SELECT \n"
			    + "     pid \n"
			    + "    ,cast(client_addr      as varchar(30)) AS client_addr \n"
			    + "    ,client_port \n"
			    + "    ,cast(client_hostname  as varchar(80)) AS client_hostname \n"
			    + "    ,cast(usename          as varchar(80)) AS user_name \n"
			    + "    ,cast(usesysid         as bigint)      AS usesysid \n"
			    + "    ,cast(application_name as varchar(80)) AS application_name \n"
			    + "    ,backend_start \n"
			    + backend_xmin
			    + "    ,cast(state            as varchar(80)) AS state \n"
			    + "    ,cast(sync_state       as varchar(80)) AS sync_state \n"
			    + "    ,(pg_xlog_location_diff(pg_current_xlog_location() ,sent_location   ) / 1024)::bigint as pending_kb \n"
			    + "    ,(pg_xlog_location_diff(sent_location              ,write_location  ) / 1024)::bigint as write_kb \n"
			    + "    ,(pg_xlog_location_diff(write_location             ,flush_location  ) / 1024)::bigint as flush_kb \n"
			    + "    ,(pg_xlog_location_diff(flush_location             ,replay_location ) / 1024)::bigint as replay_kb \n"
			    + "    ,(pg_xlog_location_diff(pg_current_xlog_location() ,replay_location))::bigint / 1024  as total_lag_kb \n"
			    + "FROM pg_stat_replication; \n"
			    + "";

		String sql_10 = ""
			    + "SELECT \n"
			    + "     pid \n"
			    + "    ,cast(client_addr      as varchar(30)) AS client_addr \n"
			    + "    ,client_port \n"
			    + "    ,cast(client_hostname  as varchar(80)) AS client_hostname \n"
			    + "    ,cast(usename          as varchar(80)) AS user_name \n"
			    + "    ,cast(usesysid         as bigint)      AS usesysid \n"
			    + "    ,cast(application_name as varchar(80)) AS application_name \n"
			    + "    ,backend_start \n"
			    + "    ,cast(backend_xmin     as varchar(30)) AS backend_xmin \n"
			    + "    ,cast(state            as varchar(80)) AS state \n"
			    + "    ,cast(sync_state       as varchar(80)) AS sync_state \n"
			    + "    ,(pg_wal_lsn_diff(pg_current_wal_lsn() ,sent_lsn   ) / 1024)::bigint AS pending_kb \n"
			    + "    ,(pg_wal_lsn_diff(sent_lsn             ,write_lsn  ) / 1024)::bigint AS write_kb \n"
			    + "    ,(pg_wal_lsn_diff(write_lsn            ,flush_lsn  ) / 1024)::bigint AS flush_kb \n"
			    + "    ,(pg_wal_lsn_diff(flush_lsn            ,replay_lsn ) / 1024)::bigint AS replay_kb \n"
			    + "    ,(pg_wal_lsn_diff(pg_current_wal_lsn() ,replay_lsn))::bigint / 1024  AS total_lag_kb \n"
	    	    + "    ,EXTRACT(EPOCH FROM (clock_timestamp() - write_lag )) AS write_lag \n"
	    	    + "    ,EXTRACT(EPOCH FROM (clock_timestamp() - flush_lag )) AS flush_lag \n"
	    	    + "    ,EXTRACT(EPOCH FROM (clock_timestamp() - replay_lag)) AS replay_lag \n"
			    +       reply_time
			    +       reply_time_seconds
			    + "FROM pg_stat_replication \n"
			    + "";

		if (has_column__sent_lsn)
			return sql_10;
		else
			return sql_9;
	}

//	@Override
//	protected void registerDefaultValues()
//	{
//		super.registerDefaultValues();
//
//		Configuration.registerDefaultValue(PROPKEY_SlideTimeInSec,             DEFAULT_SlideTimeInSec);
//	}

//	/** Used by the: Create 'Offline Session' Wizard */
//	@Override
//	public List<CmSettingsHelper> getLocalSettings()
//	{
//		Configuration conf = Configuration.getCombinedConfiguration();
//		List<CmSettingsHelper> list = new ArrayList<>();
//		
//		list.add(new CmSettingsHelper("Slide Window Time", PROPKEY_SlideTimeInSec, Integer.class, conf.getIntProperty(PROPKEY_SlideTimeInSec, DEFAULT_SlideTimeInSec), DEFAULT_SlideTimeInSec, "Set number of seconds the 'slide window time' will keep 'tup_fetched' and 'tup_returned' for." ));
//
//		return list;
//	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			
			mtd.addColumn("pg_stat_replication",  "pending_kb",           "<html>difference between current and sent locations shows how many WAL have been generated, but haven’t yet been sent to the standbys</html>");
			mtd.addColumn("pg_stat_replication",  "write_kb",             "<html>difference between sent and written locations shows WAL in the network, that have been sent but not yet written</html>");
			mtd.addColumn("pg_stat_replication",  "flush_kb",             "<html>difference between write and flush locations shows WAL written but not flushed to the permanent storage – in case postgres crashes these changes will be lost, because they have not been flushed yet</html>");
			mtd.addColumn("pg_stat_replication",  "replay_kb",            "<html>difference between flush and replay locations shows WAL that has been flushed to the permanent storage but not yet replayed – when WAL will be replayed, changes from the master would reach the standby in full</html>");
			mtd.addColumn("pg_stat_replication",  "total_lag_kb",         "<html>difference between current location on master and replay location on standby shows a total lag.</html>");
		}
		catch (NameNotFoundException e) {/*ignore*/}
	}
	

	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_TOTAL_LAG,
			"Replication Lag in KB", 	                // Menu CheckBox text
			"Replication Lag in KB ("+SHORT_NAME+")", // Graph Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_KB,
			null, LabelType.Dynamic, 
			TrendGraphDataPoint.Category.REPLICATION,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_REP_AGE_SECONDS,
			"Replication Age in Seconds", 	                // Menu CheckBox text
			"Replication Age in Seconds ("+SHORT_NAME+")", // Graph Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_SECONDS,
			null, LabelType.Dynamic, 
			TrendGraphDataPoint.Category.REPLICATION,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

	}


	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		if (GRAPH_NAME_TOTAL_LAG.equals(tgdp.getName()))
		{
			// Write 1 "line" for every device
			Double[] dArray = new Double[this.size()];
			String[] lArray = new String[dArray.length];
			for (int i = 0; i < dArray.length; i++)
			{
				lArray[i] = this.getAbsString       (i, "client_addr");
				dArray[i] = this.getAbsValueAsDouble(i, "total_lag_kb");
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
		}

		if (GRAPH_NAME_REP_AGE_SECONDS.equals(tgdp.getName()))
		{
			if (this.hasColumn("reply_time_seconds"))
			{
				// Write 1 "line" for every device
				Double[] dArray = new Double[this.size()];
				String[] lArray = new String[dArray.length];
				for (int i = 0; i < dArray.length; i++)
				{
					lArray[i] = this.getAbsString       (i, "client_addr");
					dArray[i] = this.getAbsValueAsDouble(i, "reply_time_seconds");
				}

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
			}
			else
			{
				// set label that column 'reply_time_seconds' is not available
				TrendGraph tg = getTrendGraph(tgdp.getName());
				if (tg != null)
					tg.setWarningLabel("Depends on column 'reply_time_seconds' which wasn't found in current CM.");
			}
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

		CountersModel cm = this;

		//-------------------------------------------------------
		// total_lag_kb
		//-------------------------------------------------------
		if (isSystemAlarmsForColumnEnabledAndInTimeRange("total_lag_kb"))
		{
			for (int r=0; r<cm.getAbsRowCount(); r++)
			{
				String client_addr  = cm.getAbsString       (r, "client_addr");
				Double total_lag_kb = cm.getAbsValueAsDouble(r, "total_lag_kb");
				
				if (total_lag_kb != null)
				{
					int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_TotalLagKb, DEFAULT_alarm_TotalLagKb);

					if (debugPrint || _logger.isDebugEnabled())
						System.out.println("##### sendAlarmRequest("+cm.getName()+"): threshold="+threshold+", total_lag_kb='"+total_lag_kb+"'.");

					if (total_lag_kb.intValue() > threshold)
					{
						AlarmEvent alarm = new AlarmEventPgReplicationLag(cm, client_addr, total_lag_kb.intValue(), threshold); 
						AlarmHandler.getInstance().addAlarm(alarm);
					}
				}
			}
		}

		//-------------------------------------------------------
		// reply_time_seconds
		//-------------------------------------------------------
		if (isSystemAlarmsForColumnEnabledAndInTimeRange("reply_time_seconds"))
		{
			for (int r=0; r<cm.getAbsRowCount(); r++)
			{
				String client_addr        = cm.getAbsString       (r, "client_addr");
				Double reply_time_seconds = cm.getAbsValueAsDouble(r, "reply_time_seconds");
				
				if (reply_time_seconds != null)
				{
					int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_ReplyTimeInSeconds, DEFAULT_alarm_ReplyTimeInSeconds);

					if (debugPrint || _logger.isDebugEnabled())
						System.out.println("##### sendAlarmRequest("+cm.getName()+"): threshold="+threshold+", reply_time_seconds='"+reply_time_seconds+"'.");

					if (reply_time_seconds.intValue() > threshold)
					{
						AlarmEvent alarm = new AlarmEventPgReplicationAge(cm, client_addr, reply_time_seconds.intValue(), threshold); 
						AlarmHandler.getInstance().addAlarm(alarm);
					}
				}
			}
		}
	} // end: method

	public static final String  PROPKEY_alarm_TotalLagKb         = CM_NAME + ".alarm.system.if.total_lag_kb.gt";
	public static final int     DEFAULT_alarm_TotalLagKb         = 1024 * 100; // 100 MB

	public static final String  PROPKEY_alarm_ReplyTimeInSeconds = CM_NAME + ".alarm.system.if.reply_time_seconds.gt";
	public static final int     DEFAULT_alarm_ReplyTimeInSeconds = 60 * 30;

	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		
		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;
		
		list.add(new CmSettingsHelper("total_lag_kb"      , isAlarmSwitch, PROPKEY_alarm_TotalLagKb        , Integer.class, conf.getIntProperty(PROPKEY_alarm_TotalLagKb        , DEFAULT_alarm_TotalLagKb)        , DEFAULT_alarm_TotalLagKb        , "If 'total_lag_kb' is greater than this value, send 'AlarmEventPgReplicationLag'." ));
		list.add(new CmSettingsHelper("reply_time_seconds", isAlarmSwitch, PROPKEY_alarm_ReplyTimeInSeconds, Integer.class, conf.getIntProperty(PROPKEY_alarm_ReplyTimeInSeconds, DEFAULT_alarm_ReplyTimeInSeconds), DEFAULT_alarm_ReplyTimeInSeconds, "If 'reply_time_seconds' is greater than this value, send 'AlarmEventPgReplicationAge'." ));

		return list;
	}
}
