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
package com.dbxtune.cm.rs;

import java.lang.invoke.MethodHandles;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JPanel;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.CounterController;
import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.alarm.AlarmHandler;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CmSummaryAbstract;
import com.dbxtune.cm.CounterSample;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.NoValidRowsInSample;
import com.dbxtune.cm.rs.gui.CmSummaryPanel;
import com.dbxtune.graph.TrendGraphDataPoint;
import com.dbxtune.sql.ResultSetMetaDataCached;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;

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

	public static final String[] MON_TABLES       = new String[] {};
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
//	public static final String GRAPH_NAME_XXX             = "xxx";
//	public static final String GRAPH_NAME_AA_NW_PACKET       = "aaPacketGraph";      // String x=GetCounters.CM_GRAPH_NAME__SUMMARY__AA_NW_PACKET;

	private void addTrendGraphs()
	{
//		String[] labels_xxx            = new String[] { "System+User CPU (@@cpu_busy + @@cpu_io)", "System CPU (@@cpu_io)", "User CPU (@@cpu_busy)" };
//		String[] labels_aaNwPacket       = new String[] { "@@pack_received", "@@pack_sent", "@@packet_errors" };
//		
//		addTrendGraphData(GRAPH_NAME_XXX,             new TrendGraphDataPoint(GRAPH_NAME_XXX,             labels_xxx,        LabelType.Static));
//		addTrendGraphData(GRAPH_NAME_AA_NW_PACKET,    new TrendGraphDataPoint(GRAPH_NAME_AA_NW_PACKET,    labels_aaNwPacket, LabelType.Static));

//		// GRAPH
//		addTrendGraph(GRAPH_NAME_XXX,
//			"CPU Summary, Global Variables", 	                        // Menu CheckBox text
//			"CPU Summary for all Engines (using @@cpu_busy, @@cpu_io)", // Label 
//			new String[] { "System+User CPU (@@cpu_busy + @@cpu_io)", "System CPU (@@cpu_io)", "User CPU (@@cpu_busy)" }, 
//			LabelType.Static,
//			true,  // is Percent Graph
//			false, // visible at start
//			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//			-1);   // minimum height
//
//		// GRAPH
//		addTrendGraph(GRAPH_NAME_AA_NW_PACKET,
//			"Network Packets received/sent, Global Variables", 	                            // Menu CheckBox text
//			"Network Packets received/sent per second, using @@pack_received, @@pack_sent", // Label 
//			new String[] { "@@pack_received", "@@pack_sent", "@@packet_errors" },
//			LabelType.Static,
//			false, // is Percent Graph
//			false, // visible at start
//			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//			-1);   // minimum height

//		// if GUI
//		if (getGuiController() != null && getGuiController().hasGUI())
//		{
//			// GRAPH
//			TrendGraph tg = null;
//			tg = new TrendGraph(GRAPH_NAME_XXX,
//				"CPU Summary, Global Variables", 	                        // Menu CheckBox text
//				"CPU Summary for all Engines (using @@cpu_busy, @@cpu_io)", // Label 
//				labels_xxx, 
//				true,  // is Percent Graph
//				this, 
//				false, // visible at start
//				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);   // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//
//			// GRAPH
//			tg = new TrendGraph(GRAPH_NAME_AA_NW_PACKET,
//					"Network Packets received/sent, Global Variables", 	                            // Menu CheckBox text
//					"Network Packets received/sent per second, using @@pack_received, @@pack_sent", // Label 
//					labels_aaNwPacket, 
//					false, // is Percent Graph
//					this, 
//					false, // visible at start
//					0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//					-1);   // minimum height
//				addTrendGraph(tg.getName(), tg, true);
//		}
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

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
//		String cols1, cols2, cols3;
//		cols1 = cols2 = cols3 = "";
//
//		cols1 = "* \n";
//		cols3 = "";
//
//		String fromTable = " from master..monState A \n";
//
//		String sql = "select " + cols1 + cols2 + cols3 + fromTable;
//		
//		return sql;

//		return "admin pid";
		return "-- Sorry, this is hardcoded in the Performance Counter";
	}

	@Override
	public CounterSample createCounterSample(String name, boolean negativeDiffCountersToZero, String[] diffColumns, CounterSample prevSample)
	{
		return new CounterSampleRsSummary(name, negativeDiffCountersToZero, diffColumns, prevSample);
	}

	/**
	 * Special class for collecting summary data... which isn't really accessing RepServer
	 * instead it takes data from this cm and the DbxConnection
	 */
	private static class CounterSampleRsSummary
	extends CounterSample
	{
		private static final long serialVersionUID = 1L;

		public CounterSampleRsSummary(String name, boolean negativeDiffCountersToZero, String[] diffColumns, CounterSample prevSample)
		{
			super(name, negativeDiffCountersToZero, diffColumns, prevSample);
		}
		
		@Override
		public boolean getSample(CountersModel cm, DbxConnection conn, String sql, List<String> pkList) 
		throws SQLException, NoValidRowsInSample
		{
			// TODO Auto-generated method stub
			//return super.getSample(cm, conn, sql, pkList);

			updateSampleTime(conn, cm);
			
			_rows = new ArrayList<List<Object>>();

			// Set ResultSet MetaData
			ResultSetMetaData rsmd = cm.getResultSetMetaData();
			if ( rsmd == null )
			{
				ResultSetMetaDataCached rsmdCached = new ResultSetMetaDataCached();

				//             columnName,            columnType,               columnTypeName, nullable, columnClassName             columnDisplaySize 
				rsmdCached.addColumn("serverName",          java.sql.Types.VARCHAR,   "varchar",      false,    String   .class.getName(),  30);
				rsmdCached.addColumn("rsVersion",           java.sql.Types.VARCHAR,   "varchar",      false,    String   .class.getName(),  255);
//				rsmdCached.addColumn("sampleTime",          java.sql.Types.TIMESTAMP, "datetime",     false,    Timestamp.class.getName(),  30);

				cm.setResultSetMetaData( rsmdCached );
				rsmd = rsmdCached;
			}
			
			if ( ! initColumnInfo(rsmd, pkList, 1) )
				return false;
			

			// set data
			List<Object> row = new ArrayList<>();
			row.add(conn.getDbmsServerName());
			row.add(conn.getDbmsVersionStr());
//			row.add(cm.getSampleTime());

			_rows.add(row);

			// Set last refresh time
			_lastLocalSampleTime = System.currentTimeMillis();

			return true;
		}
	}
	
	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
//		long srvVersion = getServerVersion();

		//---------------------------------
		// GRAPH:
		//---------------------------------
//		if (GRAPH_NAME_XXX.equals(tgdp.getName()))
//		{	
//			Double[] arr = new Double[3];
//
//			int ms = (int) (System.currentTimeMillis() % 1000l);
//			ms = ms < 0 ? ms+1000 : ms;
//
////			arr[0] = this.getAbsValueAsDouble (0, "Connections");
////			arr[1] = this.getAbsValueAsDouble (0, "distinctLogins");
////			arr[2] = this.getDiffValueAsDouble(0, "aaConnections");
//			arr[0] = Double.valueOf(5.1  * ms);
//			arr[1] = Double.valueOf(10.2 * ms);
//			arr[2] = Double.valueOf(15.5 * ms);
//			_logger.debug("updateGraphData("+GRAPH_NAME_XXX+"): Connections(Abs)='"+arr[0]+"', distinctLogins(Abs)='"+arr[1]+"', aaConnections(Diff)='"+arr[2]+"'.");
//
//			// Set the values
//			tgdp.setDataPoint(this.getTimestamp(), arr);
//		}

//		//---------------------------------
//		// GRAPH:
//		//---------------------------------
//		if (GRAPH_NAME_AA_NW_PACKET.equals(tgdp.getName()))
//		{	
//			Double[] arr = new Double[2];
//
//			arr[0] = this.getRateValueAsDouble (0, "PacketsReceived");
//			arr[1] = this.getRateValueAsDouble (0, "PacketsSent");
////			arr[2] = this.getRateValueAsDouble (0, "packet_errors");
////			_logger.debug("updateGraphData(aaPacketGraph): packet_errors='"+arr[0]+"', total_errors='"+arr[1]+"', packet_errors='"+arr[2]+"'.");
//			_logger.debug("updateGraphData(aaPacketGraph): PacketsReceived='"+arr[0]+"', PacketsSent='"+arr[1]+"'.");
//
//			// Set the values
//			tgdp.setDate(this.getTimestamp());
//			tgdp.setData(arr);
//		}
	}



	//----------------------------------------------------------------
	// ALARMS
	//----------------------------------------------------------------
	@Override
	public void sendAlarmRequest()
	{
		if ( ! AlarmHandler.hasInstance() )
			return;

		//-------------------------------------------------------
		// DbmsVersionStringChanged
		//-------------------------------------------------------
		doAlarmIfDbmsVersionStringWasChanged("rsVersion");

	}

	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		return addAlarmSettings_DbmsVersionStringChanged(null, "rsVersion");
	}
}
