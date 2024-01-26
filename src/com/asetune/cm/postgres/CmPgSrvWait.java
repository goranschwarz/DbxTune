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

import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.central.pcs.CentralPersistReader;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.postgres.gui.CmPgPidWaitPanel;
import com.asetune.cm.postgres.gui.CmPgSrvWaitPanel;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.config.dict.PostgresWaitTypeDictionary;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.graph.TrendGraphDataPoint.LabelType;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.conn.info.DbmsVersionInfo;
import com.asetune.utils.MathUtils;
import com.asetune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmPgSrvWait
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmPgSrvWait.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmPgSrvWait.class.getSimpleName();
	public static final String   SHORT_NAME       = "Server Wait";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<p>What are the server waiting for.</p>" +
		"<br><br>" +
//		"Table Background colors:" +
//		"<ul>" +
//		"    <li>DARK BEIGE          - Entry is skipped/discarded from LOCAL Graphs</li>" +
//		"    <li>BEIGE               - Entry is skipped/discarded from TREND Graphs</li>" +
//		"</ul>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = Ver.ver(9, 6);
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"pg_wait_sampling_profile"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {"wait_count_pct"};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"wait_count",
		"est_wait_time_ms"
		};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.ALL; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '"+CM_NAME+"'");

		return new CmPgSrvWait(counterController, guiController);
	}

	public CmPgSrvWait(ICounterController counterController, IGuiController guiController)
	{
		super(counterController,
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
//	public static final String  PROPKEY_sqlSkipFilterWaitTypes       = CM_NAME + ".sql.skip.filter.waitTypes";
//	public static final String  DEFAULT_sqlSkipFilterWaitTypes       = null;

//	public static final String  PROPKEY_sqlSkipFilterEnabled         = CM_NAME + ".sql.skip.filter.enabled";
//	public static final boolean DEFAULT_sqlSkipFilterEnabled         = true;

	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmPgSrvWaitPanel(this);
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

		pkCols.add("event_type");
		pkCols.add("event");

		return pkCols;
	}

//	private static final int POS_wait_type         = 0;
//	private static final int POS_SkipInLocalGraphs = 9;
//	private static final int POS_SkipInTrendGraphs = 10;

	// pg_wait_sampling configuration
	private int  _pgWaitSampling_profilePeriod = -1;
	
	// If we should "reset" counters in the DBMS every now and then
	// This so we don't get to many "slots" (when a clients disconnects DBMS, the slot in 'pg_wait_sampling' is not automatically cleaned-up/removed...
	// So every now and then lets call 'select pg_wait_sampling_reset_profile()'
	private long _lastRefreshTime = -1;
	private long _lastResetTime = -1;
	private long _resetAfterMinutes = 60 * 24; // Or should we just RESET at midnight... (but that is not good to)
	// NOTE: We can't call pg_wait_sampling_reset_profile(), because then we will "remove" data entries for SrvWait level
	//       In CmPgPidWait we instead need to check for pg_stat_activity, and remove entries that are not in there!
//TODO; // Implement the above

//TODO; // Implement GUI Panel
//TODO; // ADD Graphs

//TODO; // in PID Wait -- Check if Config 'pg_wait_sampling.profile_pid = true'     -- If not DISABLE
//TODO; // in PID Wait -- Check if Config 'pg_wait_sampling.profile_queries = true' -- If not DISABLE

	/**
	 * Called before <code>refreshGetData(conn)</code> where we can make various checks
	 * <p>
	 * Note: this is a special case since Company X is recreating the Postgres Server 
	 *       (every now and then) during the day/night...
	 *       We need to check/create the extension before polling data from it!
	 */
	@Override
	public boolean beforeRefreshGetData(DbxConnection conn) throws Exception
	{
		return PostgresCmHelper.pgWaitSampling_beforeRefreshGetData(this, conn);
	}

	@Override
	public boolean checkDependsOnOther(DbxConnection conn)
	{
		return PostgresCmHelper.pgWaitSampling_checkDependsOnOther(this, conn);
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
//		List<String> skipList = getDefaultLocalGraphsSkipList();
//		String skipWaitTypes = "";
//		if (skipList.size() > 0 && Configuration.getCombinedConfiguration().getBooleanProperty(CmPgSrvWait.PROPKEY_sqlSkipFilterEnabled, CmPgSrvWait.DEFAULT_sqlSkipFilterEnabled))
//			skipWaitTypes = "  and wait_type NOT IN (" + StringUtil.toCommaStrQuoted("'", skipList) + ")";

		// Get Configuration for 'pg_wait_sampling.profile_period'
		if (_pgWaitSampling_profilePeriod < 0)
		{
			// Get config from pg_wait_sampling
			String sql = "select cast(setting as int) as setting from pg_settings where name = 'pg_wait_sampling.profile_period'";
			try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
			{
				while(rs.next())
				{
					_pgWaitSampling_profilePeriod = rs.getInt(1);
					_logger.info(CM_NAME + ": Configuration for 'pg_wait_sampling.profile_period' is set to: " + _pgWaitSampling_profilePeriod);
				}
			}
			catch (SQLException ex)
			{
				_pgWaitSampling_profilePeriod = 10;
				_logger.error("Problems getting configuration for 'pg_wait_sampling.profile_period', setting the value to '10'. continuing. SQL=|" + sql + "|.", ex);
			}
		}
		
		String sql = ""
			    + "SELECT \n"
			    + "     CAST(event_type as varchar(128)) AS event_type \n"
			    + "    ,CAST(event      as varchar(128)) AS event \n"
			    + "    ,CAST(SUM(count) as BIGINT)       AS wait_count \n"
			    + "    ,CAST(SUM(count) as BIGINT) * " + _pgWaitSampling_profilePeriod + " AS est_wait_time_ms /* pg_wait_sampling.profile_period = " + _pgWaitSampling_profilePeriod + " */\n"
			    + "    ,CAST(-1 as NUMERIC(6,1))         AS wait_count_pct \n"
			    + "FROM pg_wait_sampling_profile \n"
			    + "WHERE 1 = 1 \n"
			    + "  AND event_type != 'Activity' \n"   // DBMS process waiting for activity in its main processing loop
			    + "  AND event      != 'ClientRead' \n" // Server is waiting for Commands from Client
			    + "  AND event      != 'CheckpointWriteDelay' \n" 
			    + "GROUP BY event_type, event \n"
			    + "ORDER BY event_type, event \n"
			    + "";

//	    + "    ,COUNT(distinct pid) AS pid_count \n" // do we want this or not ???
//	    + "  AND pid IN (SELECT pid FROM pg_stat_activity) \n" // We can't discard all disconnected clients (then we lose data-points, and we can't do srv-wait for a longer period)
		
		return sql;
	}






	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		String name = "pg_wait_sampling_profile";
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addTable(name, "pg_wait_sampling_profile is a Postgres Pro Enterprise extension for collecting sampling-based statistics on wait events.");
			
			mtd.addColumn(name, "event_type",       "<html>"
			                                             + "Name of wait event type. "
			                                             + "<br>Hover over a cell to get a detailed description.<br>"
			                                             + "</html>");
			mtd.addColumn(name, "event",            "<html>"
			                                             + "Name of wait event. <br>"
			                                             + "<br>Hover over a cell to get a detailed description.<br>"
			                                             + "</html>");
//			mtd.addColumn(name, "pid_count",        "<html>"
//			                                             + "How many pid's are this 'wait' based on. <br>"
//			                                             + "<b>formula</b>: sum(pid) <br>"
//			                                             + "</html>");
			mtd.addColumn(name, "wait_count",       "<html>"
			                                             + "Count of samples. <br>"
			                                             + "</html>");
			mtd.addColumn(name, "est_wait_time_ms", "<html>"
			                                             + "Esimated time in milliseconds the server waited on this wait type. <br>"
			                                             + "<b>formula</b>: sum(count) * pg_wait_sampling.profile_period <br>"
			                                             + "</html>");
			mtd.addColumn(name, "wait_count_pct",   "<html>"
			                                             + "Percent this 'wait type' was compared to all others."
			                                             + "<b>formula</b>: <i>this_row_wait_count</i> / sum(<i>all_rows_wait_count</i>) * 100 <br>"
			                                             + "</html>");
		}
		catch (NameNotFoundException e) 
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
		//	System.out.println("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
		}
	}


	private void calcWaitCountPct(CounterSample dataSample)
	{
		int pos_wait_count     = dataSample.findColumn("wait_count");
		int pos_wait_count_pct = dataSample.findColumn("wait_count_pct");
		
		if (pos_wait_count == -1 || pos_wait_count_pct == -1)
		{
			_logger.warn(getName() + ": calcWaitCountPct(), Problems finding column 'wait_count'=" + pos_wait_count + " or 'wait_count_pct'=" + pos_wait_count_pct + ".");
			return;
		}

		// First get 'wait_count' SUM
		long sum_wait_count = 0;
		for (int rowId=0; rowId < dataSample.getRowCount(); rowId++)
		{
			sum_wait_count += dataSample.getValueAsLong(rowId, pos_wait_count);
		}

		// Second; loop rows and calculate PCT
		for (int rowId=0; rowId < dataSample.getRowCount(); rowId++)
		{
			long row_wait_count = dataSample.getValueAsLong(rowId, pos_wait_count);

			// Calc:
			BigDecimal wait_count_pct =	(sum_wait_count == 0) 
					? new BigDecimal(0) // when sum_wait_count == 0
					: MathUtils.roundToBigDecimal( (row_wait_count*1.0) / (sum_wait_count*1.0) * 100.0, 1);

			// Set value
			dataSample.setValueAt(wait_count_pct, rowId, pos_wait_count_pct);
		}
	}

	/**
	 * Calculate the ABS 'wait_count_pct'
	 */
	@Override
	public void localCalculation(CounterSample newSample)
	{
		calcWaitCountPct(newSample);
	}

	/**
	 * Calculate the ABS 'wait_count_pct' on DIFF (and RATE will get the data from DIFF)
	 */
	@Override
	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
	{
		calcWaitCountPct(diffData);
	}
	

	@Override
	public String getToolTipTextOnTableCell(MouseEvent e, String colName, Object cellValue, int modelRow, int modelCol) 
	{
		if ("event_type".equals(colName) )
		{
			return PostgresWaitTypeDictionary.getWaitEventTypeDescription(cellValue + "");
		}
		if ("event".equals(colName) )
		{
			return PostgresWaitTypeDictionary.getWaitEventDescription(cellValue + "");
		}
		
		return super.getToolTipTextOnTableCell(e, colName, cellValue, modelRow, modelCol);
	}
	

	//---------------------------------------------------------------------------------
	//---------------------------------------------------------------------------------
	// Graph stuff
	//---------------------------------------------------------------------------------
	//---------------------------------------------------------------------------------

	public static final String   GRAPH_NAME_SRV_WAIT_TYPE_MS  = "SrvWaitTypeMs";
	public static final String   GRAPH_NAME_SRV_WAIT_MS       = "SrvWaitMs";

	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_SRV_WAIT_TYPE_MS,
			"Server Estimated Wait Time in ms, group by 'event_type'", 	                   // Menu CheckBox text
			"Server Estimated Wait Time in ms, group by 'event_type' ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MILLISEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null,
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.WAITS,
			false, // is Percent Graph
			false, // visible at start
			0,    // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			0);  // minimum height

		addTrendGraph(GRAPH_NAME_SRV_WAIT_MS,
			"Server Estimated Wait Time in ms, group by 'wait_type', 'event'", 	                   // Menu CheckBox text
			"Server Estimated Wait Time in ms, group by 'wait_type', 'event' ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MILLISEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null,
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.WAITS,
			false, // is Percent Graph
			false, // visible at start
			0,    // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			160);  // minimum height

	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		String graphName  = tgdp.getName();
		
		//--------------------------------------------------------
		if (GRAPH_NAME_SRV_WAIT_TYPE_MS.equals(graphName))
		{
			LinkedHashMap<String, Double> dataMap = new LinkedHashMap<String, Double>();

			// Loop all rows, but add/group by 'event_type' -- BufferPin, Client, Extension, IO, IPC, Lock, LWLock, Timeout
			for (int r = 0; r < this.size(); r++)
			{
				String event_type = this.getAbsString        (r, "event_type");
				Double wait_count = this.getRateValueAsDouble(r, "est_wait_time_ms");

				Double sum_wait_count = dataMap.get(event_type);
				if (sum_wait_count == null)
					sum_wait_count = new Double(0);

				sum_wait_count += wait_count;
				
				dataMap.put(event_type, sum_wait_count);
			}

			if (dataMap.size() > 0)
				tgdp.setData(this.getTimestamp(), dataMap);
		}

		//--------------------------------------------------------
		if (GRAPH_NAME_SRV_WAIT_MS.equals(graphName))
		{
			// Start With: 1 "line" for every wait_type
			// but some are SKIPPED, so we need to make the array SHORTER at the end
			Double[] dArray = new Double[this.size()];
			String[] lArray = new String[dArray.length];
			
			for (int r=0; r<dArray.length; r++)
			{
				String event_type = this.getAbsString        (r, "event_type");
				String event      = this.getAbsString        (r, "event");
				Double wait_count = this.getRateValueAsDouble(r, "est_wait_time_ms");

				lArray[r] = event_type + "::" + event;
				dArray[r] = wait_count;
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
		}
	}


	//--------------------------------------------------------------------------------------
	//--------------------------------------------------------------------------------------
	//-- Alarm Handling
	//--------------------------------------------------------------------------------------
	//--------------------------------------------------------------------------------------
//	@Override
//	public void sendAlarmRequest()
//	{
//		// Check for "Toxic" waits... and alarm
//		// But right now I don't know what waits should be considered as that... 
//		// So I need to investigate that and implement this later!
//	}
}
