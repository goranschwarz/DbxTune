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
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.alarm.AlarmHandler;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.alarm.events.postgres.AlarmEventPgWalSizeHigh;
import com.dbxtune.central.pcs.CentralPersistReader;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.graph.TrendGraphDataPoint;
import com.dbxtune.graph.TrendGraphDataPoint.LabelType;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.sql.ResultSetMetaDataCached;
import com.dbxtune.sql.ResultSetMetaDataCached.Entry;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.NumberUtils;
import com.dbxtune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmPgWal
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmPgWal.class.getSimpleName();
	public static final String   SHORT_NAME       = "Wal Info";
	public static final String   HTML_DESC        = 
		"<html>" +
		"One row only, containing data about WAL activity of the cluster.<br> <i>(WAL = Write Ahead Log, or the Transaction log)</i>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = Ver.ver(14);
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"pg_stat_wal", "pg_ls_waldir"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
			"wal_records",
			"wal_fpi",
			"wal_bytes",
			"wal_buffers_full",
			"wal_write",
			"wal_sync",
			"wal_write_time",
			"wal_sync_time"
	};
	
	// RS> Col# Label            JDBC Type Name           Guessed DBMS type Source Table
	// RS> ---- ---------------- ------------------------ ----------------- ------------
	// RS> 1    wal_records      java.sql.Types.BIGINT    int8              pg_stat_wal 
	// RS> 2    wal_fpi          java.sql.Types.BIGINT    int8              pg_stat_wal 
	// RS> 3    wal_bytes        java.sql.Types.NUMERIC   numeric(0,0)      pg_stat_wal 
	// RS> 4    wal_buffers_full java.sql.Types.BIGINT    int8              pg_stat_wal 
	// RS> 5    wal_write        java.sql.Types.BIGINT    int8              pg_stat_wal 
	// RS> 6    wal_sync         java.sql.Types.BIGINT    int8              pg_stat_wal 
	// RS> 7    wal_write_time   java.sql.Types.DOUBLE    float8            pg_stat_wal 
	// RS> 8    wal_sync_time    java.sql.Types.DOUBLE    float8            pg_stat_wal 
	// RS> 9    stats_reset      java.sql.Types.TIMESTAMP timestamptz       pg_stat_wal 	

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

		return new CmPgWal(counterController, guiController);
	}

	public CmPgWal(ICounterController counterController, IGuiController guiController)
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
	public static final String GRAPH_NAME_RECORDS     = "Records";
	public static final String GRAPH_NAME_WRITE_COUNT = "WCount";
	public static final String GRAPH_NAME_WRITE_TIME  = "WTime";
	public static final String GRAPH_NAME_WAL_KB      = "WalKb";
	public static final String GRAPH_NAME_FULL_COUNT  = "FullCount";
	public static final String GRAPH_NAME_WAL_SIZE_MB = "WalSizeMb";
	
//	@Override
//	protected TabularCntrPanel createGui()
//	{
//		return new CmActiveStatementsPanel(this);
//	}
	

	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_RECORDS,
				"WAL Records per Second", 	                // Menu CheckBox text
				"WAL Records per Second ("+SHORT_NAME+")", // Graph Label 
				TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
				new String[] {"wal_records"}, 
				LabelType.Static, 
				TrendGraphDataPoint.Category.OTHER,
				false, // is Percent Graph
				false, // visible at start
				Ver.ver(14),     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height

		addTrendGraph(GRAPH_NAME_WRITE_COUNT,
				"WAL Writes per Second", 	                // Menu CheckBox text
				"WAL Writes per Second ("+SHORT_NAME+")", // Graph Label 
				TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
				new String[] {"wal_write", "wal_sync"}, 
				LabelType.Static, 
				TrendGraphDataPoint.Category.DISK,
				false, // is Percent Graph
				false, // visible at start
				Ver.ver(14),     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height

		addTrendGraph(GRAPH_NAME_WRITE_TIME,
				"WAL Write Time in ms", 	                // Menu CheckBox text
				"WAL Write Time in ms ("+SHORT_NAME+")", // Graph Label 
				TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
				new String[] {"wal_write_time", "wal_sync_time"}, 
				LabelType.Static, 
				TrendGraphDataPoint.Category.DISK,
				false, // is Percent Graph
				false, // visible at start
				Ver.ver(14),     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height

		addTrendGraph(GRAPH_NAME_WAL_KB,
				"WAL KB Written per Second", 	                // Menu CheckBox text
				"WAL KB Written per Second ("+SHORT_NAME+")", // Graph Label 
				TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_KB, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
				new String[] {"wal_kb"}, 
				LabelType.Static, 
				TrendGraphDataPoint.Category.DISK,
				false, // is Percent Graph
				false, // visible at start
				Ver.ver(14),     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height

		addTrendGraph(GRAPH_NAME_FULL_COUNT,
				"WAL Buffer was Full Writes per Second", 	                // Menu CheckBox text
				"WAL Buffer was Full Writes per Second ("+SHORT_NAME+")", // Graph Label 
				TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
				new String[] {"wal_buffers_full"}, 
				LabelType.Static, 
				TrendGraphDataPoint.Category.DISK,
				false, // is Percent Graph
				false, // visible at start
				Ver.ver(14),     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height

		addTrendGraph(GRAPH_NAME_WAL_SIZE_MB,
				"pg_wal dir Size in MB", 	                // Menu CheckBox text
				"pg_wal dir Size in MB ("+SHORT_NAME+")", // Graph Label 
				TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
				new String[] {"pg_waldir_total_size_mb"}, 
				LabelType.Static, 
				TrendGraphDataPoint.Category.DISK,
				false, // is Percent Graph
				true,  // visible at start
				Ver.ver(14),     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height

	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		//---------------------------------
		if (GRAPH_NAME_RECORDS.equals(tgdp.getName()))
		{
			Double[] arr = new Double[1];

			arr[0] = this.getRateValueAsDouble(0, "wal_records", 0d);

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		//---------------------------------
		if (GRAPH_NAME_WRITE_COUNT.equals(tgdp.getName()))
		{
			Double[] arr = new Double[2];

			arr[0] = this.getRateValueAsDouble(0, "wal_write", 0d);
			arr[1] = this.getRateValueAsDouble(0, "wal_sync", 0d);

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		//---------------------------------
		if (GRAPH_NAME_WRITE_TIME.equals(tgdp.getName()))
		{
			Double[] arr = new Double[2];

			arr[0] = this.getRateValueAsDouble(0, "wal_write_time", 0d);
			arr[1] = this.getRateValueAsDouble(0, "wal_sync_time", 0d);

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		//---------------------------------
		if (GRAPH_NAME_WAL_KB.equals(tgdp.getName()))
		{
			Double[] arr = new Double[1];

			arr[0] = NumberUtils.round(this.getRateValueAsDouble(0, "wal_bytes", 0d) / 1024.0, 1);

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		//---------------------------------
		if (GRAPH_NAME_FULL_COUNT.equals(tgdp.getName()))
		{
			Double[] arr = new Double[1];

			arr[0] = this.getRateValueAsDouble(0, "wal_buffers_full", 0d);

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		//---------------------------------
		if (GRAPH_NAME_WAL_SIZE_MB.equals(tgdp.getName()))
		{
			Double[] arr = new Double[1];

			arr[0] = this.getRateValueAsDouble(0, "pg_waldir_total_size_mb", 0d);

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
//		return null;
		List <String> pkCols = new LinkedList<String>();

//		pkCols.add("pk");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		// possibly add: wal_write_time / wal_write AS wal_write_time_per_write   (it also needs to be maintained for DIFF level)
		// possibly add: wal_sync_time  / wal_sync  AS wal_sync_time_per_write    (it also needs to be maintained for DIFF level) 
//		return "select * from pg_stat_wal";
		
		// I would also want to use "pg_get_free_disk_space('pg_wal')", but there is no such functionality i Postgres
		// So I can't WARN about eminent SPACE ISSUES here, we have to trust that 'CmOsDiskSpace' is enabled and do want about that!!!

		// pg_ls_waldir -- Was introduced in PG 10, and pg_stat_wal in PG 14
		String sql = ""
			    + "with waldir as ( \n"
			    + "    select \n"
			    + "         cast(sum(size) / 1024 / 1024 as numeric(15,1)) AS pg_waldir_total_size_mb \n"
			    + "        ,min(modification) AS oldest_wal_file_ts \n"
			    + "        ,max(modification) AS latest_wal_file_ts \n"
			    + "        ,cast(age(max(modification), min(modification)) as varchar(60)) AS latest_vs_oldest_wal_file_ts_diff \n"
			    + "    from pg_ls_waldir() \n"
			    + "    where name not like '%.backup' \n"
			    + ") \n"
			    + "select ws.*, waldir.* \n"
			    + "from pg_stat_wal ws, waldir \n"
			    + "";
		return sql;
	}

	@Override
	public ResultSetMetaDataCached modifyResultSetMetaData(ResultSetMetaDataCached rsmdc)
	{
		for (Entry entry : rsmdc.getEntries())
		{
			if (    "wal_bytes".equals(entry.getColumnLabel()) 
			     && entry.getColumnType() == Types.NUMERIC 
			     && entry.getPrecision() == 0 
			     && entry.getScale() == 0
			   ) 
			{
				entry.setPrecision(38);
				entry.setScale(0);

				_logger.info("modifyResultSetMetaData: Cm='" + getName() + "', columnName='" + entry.getColumnLabel() + "', changing data type PRECISION from 0 to 38");
			}
		}
		
		return rsmdc;
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
		// free_connections
		//-------------------------------------------------------
		if (isSystemAlarmsForColumnEnabledAndInTimeRange("pg_waldir_total_size_mb"))
		{
			// There should only be 1 row... but lets use SUM anyway
			Double pg_waldir_total_size_mb = cm.getAbsValueSum("pg_waldir_total_size_mb");
			
			if (pg_waldir_total_size_mb != null)
			{
				int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_pgWaldirTotalSizeMb, DEFAULT_alarm_pgWaldirTotalSizeMb);

				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest("+cm.getName()+"): pg_waldir_total_size_mb: threshold="+threshold+", pg_waldir_total_size_mb='"+pg_waldir_total_size_mb+"'.");

				if (pg_waldir_total_size_mb.intValue() >= threshold)
				{
					String extendedDescText = "";
					String extendedDescHtml = cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_WAL_SIZE_MB);

					AlarmEvent ae = new AlarmEventPgWalSizeHigh(cm, pg_waldir_total_size_mb, threshold);
					ae.setExtendedDescription(extendedDescText, extendedDescHtml);

					AlarmHandler.getInstance().addAlarm(ae);
				}
			}
		}
	} // end: method

	@Override
	public boolean isGraphDataHistoryEnabled(String name)
	{
		// ENABLED for the following graphs
		if (GRAPH_NAME_WAL_SIZE_MB.equals(name)) return true;

		// default: DISABLED
		return false;
	}
	@Override
	public int getGraphDataHistoryTimeInterval(String name)
	{
		// Keep interval: default is 60 minutes
		//return super.getGraphDataHistoryTimeInterval(name);
		
		// Keep 4 hours in history
		return 60 * 4;
	}

	public static final String  PROPKEY_alarm_pgWaldirTotalSizeMb = CM_NAME + ".alarm.system.if.pg_waldir_total_size_mb.lt";
	public static final int     DEFAULT_alarm_pgWaldirTotalSizeMb = 32 * 1024; // 32 GB

	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		
		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;
		
		list.add(new CmSettingsHelper("pg_waldir_total_size_mb" , isAlarmSwitch, PROPKEY_alarm_pgWaldirTotalSizeMb , Integer.class, conf.getIntProperty(PROPKEY_alarm_pgWaldirTotalSizeMb , DEFAULT_alarm_pgWaldirTotalSizeMb ), DEFAULT_alarm_pgWaldirTotalSizeMb , "If 'pg_waldir_total_size_mb' is less than this value, send 'AlarmEventWalSizeHigh'." ));

		return list;
	}
}
