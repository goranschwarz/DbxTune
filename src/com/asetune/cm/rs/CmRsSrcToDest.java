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
package com.asetune.cm.rs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.asetune.CounterController;
import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.central.pcs.CentralPersistReader;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.DbxTuneResultSetMetaData;
import com.asetune.cm.NoValidRowsInSample;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.graph.TrendGraphDataPoint.LabelType;
import com.asetune.gui.MainFrameRs;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.conn.info.DbmsVersionInfo;
import com.asetune.utils.RepServerUtils;

/**
 * This one is a little odd...<br>
 * <ul>
 *   <li>On initialization it will try to get what databases etc this RepServer is responsible for. <br>
 *       Note: It will NOT be refreshed after that, so if databases etc are added, the tool will need to be restarted 
 *   </li>
 *   <li>It will use data in CmAdminStats, so no local refresh will be done, hence no RCL is sent to RepServer</li>
 * </ul>
 * 
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmRsSrcToDest
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmRsSrcToDest.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmRsSrcToDest.class.getSimpleName();
	public static final String   SHORT_NAME       = "Source To Dest";
	public static final String   HTML_DESC        = 
		"<html>"
		+ "<p>RepServer Monitor And Performance Counters</p>"
		+ "Fetched using: <code>admin statistics,'ALL'</code>"
		+ "<br>"
		+ "This Tab reports a summary of the RS performance for a specific replication path from PDB -> RDB.  <br>"
		+ "<b>Ideally, if there isn't any latency, the rate of commands at each stage of the path will be nearly identical</b><br>"
		+ "Note: <i>taking into consideration that empty transactions and subscription where clauses may affect the number of commands processed.</i>  <br>"
		+ "<br>"
		+ "The backlog reported for the inbound (i) and outbound (o) queues are based on the number of active segments (in MB) in the queue in addition to the SQM Reader backlog for the inbound queue. <br> "
		+ "An active segment is one in which there are commands on the segment that have not been completely delivered to outbound queue or RDB.  <br>"
		+ "Active(i) or Active(o) may be high if there is a large DSI or DIST SQT cache and downstream latency is causing the SQT to buffer transactions<br>"
		+ "</html>";

	public static final String   GROUP_NAME       = MainFrameRs.TCP_GROUP_MC;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"stats"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"RACmds",
		"SQMInCmds",
//		"SegsActiveInMB",
//		"BacklogInMB",
		"SQMRCmds",
		"SQTCmds",
		"DISTCmds",
		"SQMOutCmds",
//		"SegsActiveOutMB",
		"DSICmds"
		};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = false;
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

		return new CmRsSrcToDest(counterController, guiController);
	}

	public CmRsSrcToDest(ICounterController counterController, IGuiController guiController)
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
		
		addDependsOnCm(CmAdminStats.CM_NAME); // CmAdminStats must have been executed before this cm

		addTrendGraphs();
		
		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	public static final String GRAPH_NAME_PERF_INDICATOR = "PerfInd";

	private void addTrendGraphs()
	{
		//-----
		addTrendGraph(GRAPH_NAME_PERF_INDICATOR,
			"Source to Destination - Performance Indicator", // Menu CheckBox text
			"Source to Destination - Performance Indicator (0: good, <0: DSI is Slow, >0: DSI is Fast)", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.AUTO, -1),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.OTHER,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		if (GRAPH_NAME_PERF_INDICATOR.equals(tgdp.getName()))
		{
			// Write 1 "line" for every device
			Double[] dArray = new Double[this.size()];
			String[] lArray = new String[dArray.length];
			for (int i = 0; i < dArray.length; i++)
			{
				String source = this.getRateString(i, "source");
				String dest   = this.getRateString(i, "destination");

				lArray[i] = source + " -> " + dest;
				dArray[i] = this.getRateValueAsDouble(i, "PerfInd");
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
		}
	}
	


	private List<RsDbInfo> _rsDbInfoList = new ArrayList<CmRsSrcToDest.RsDbInfo>();
	@Override
	public boolean doSqlInit(DbxConnection conn) 
	{
		if (conn == null)
			throw new IllegalArgumentException("The passed conn is null.");

		try
		{
			// Open a Gateway Connection to the RSSD
			RepServerUtils.connectGwRssd(conn);
			
			String sql = getInitSqlFromFile();
			if (sql == null)
				return true;
			
			Statement stmnt = conn.createStatement();
			ResultSet rs = stmnt.executeQuery(sql);
			while (rs.next())
			{
				RsDbInfo rsDbInfo = new RsDbInfo();
				rsDbInfo.type             = rs.getString (1);  // varchar(10)   not null, 
				rsDbInfo.local_rsid       = rs.getInt    (2);  // int           not null, 
				rsDbInfo.local_rsname     = rs.getString (3);  // varchar(30)   not null, 
				rsDbInfo.src_server       = rs.getString (4);  // varchar(30)   not null, 
				rsDbInfo.src_database     = rs.getString (5);  // varchar(30)   not null, 
				rsDbInfo.src_dbid         = rs.getInt    (6);  // int           not null, 
				rsDbInfo.src_connection   = rs.getString (7);  // varchar(80)   not null, 
				rsDbInfo.src_rsid         = rs.getInt    (8);  // int           not null, 
				rsDbInfo.src_rsname       = rs.getString (9);  // varchar(30)   not null, 
				rsDbInfo.is_origin        = rs.getBoolean(10);  // bit           not null, 
				rsDbInfo.dest_server      = rs.getString (11);  // varchar(30)   not null, 
				rsDbInfo.dest_database    = rs.getString (12); // varchar(30)   not null, 
				rsDbInfo.dest_dbid        = rs.getInt    (13); // int           not null, 
				rsDbInfo.dest_connection  = rs.getString (14); // varchar(80)   not null, 
				rsDbInfo.dest_rsid        = rs.getInt    (15); // int           not null, 
				rsDbInfo.dest_rsname      = rs.getString (16); // varchar(30)   not null, 
				// TABLE 
				rsDbInfo.num_repdefs      = rs.getInt    (17); // int               null, 
				rsDbInfo.num_tables       = rs.getInt    (18); // int               null, 
				rsDbInfo.num_table_subscr = rs.getInt    (19); // int               null,
				// MSA 
				rsDbInfo.dbrepid          = rs.getInt    (20); // int               null, 
				rsDbInfo.db_repdef_name   = rs.getString (21); // varchar(30)       null, 
				rsDbInfo.db_subscr_name   = rs.getString (22); // varchar(30)       null, 
				rsDbInfo.num_subsets      = rs.getInt    (23); // int               null,
				// WS 
				rsDbInfo.logical_conn     = rs.getString (24); // varchar(80)       null, 
				rsDbInfo.ldbid            = rs.getInt    (25); // int               null
				
				_rsDbInfoList.add(rsDbInfo);
			}
			rs.close();
			stmnt.close();
		}
		catch(SQLException ex)
		{
			_logger.error("Problems getting databases attached to this RepServer from the RSSD.");
		}
		finally
		{
			// CLOSE the Gateway Connection to the RSSD
			RepServerUtils.disconnectGwNoThrow(conn);
		}

		return true;
	}

	@Override
	public String[] getDependsOnConfigForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return NEED_CONFIG;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return "";
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("source");
		pkCols.add("destination");

		return pkCols;
	}

//	@Override
//	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
//	{
//		return "";
//	}

	@Override
	public CounterSample createCounterSample(String name, boolean negativeDiffCountersToZero, String[] diffColumns, CounterSample prevSample)
	{
		return new CounterSample(name, negativeDiffCountersToZero, diffColumns, prevSample)
		{
			private static final long serialVersionUID = 1L;

			private int _module_pos         = -1;
			private int _instance_pos       = -1;
			private int _instanceId_pos     = -1;
			private int _modTypeInstVal_pos = -1;
			private int _counterId_pos      = -1;
			private int _type_pos           = -1;
			private int _name_pos           = -1;
			private int _obs_pos            = -1;
			private int _total_pos          = -1;
			private int _last_pos           = -1;
//			private int _rateXsec_pos       = -1;
	
			@Override
			public boolean getSample(CountersModel cm, DbxConnection conn, String sql, List<String> pkList) 
			throws SQLException, NoValidRowsInSample
			{
				// Get the CmAdminStats which holds all the counter which a Module is based on
				CmAdminStats cmAdminStats = (CmAdminStats) CounterController.getInstance().getCmByName(CmAdminStats.CM_NAME);
//				if ( cmAdminStats.getColumnCount() == 0 )
//					return false;

//				db2db
//				--	58000 (@src_dbid)				Repagent: Commands received	REPAGENT	CmdsRecv
//				--	6000  (@src_ldbid)				SQM: Commands written to queue	SQM	CmdsWritten
//				--	6020  (@src_ldbid)				SQM: Active queue segments	SQM	SegsActive
//				--	62000 (special: @src_ldbid, @sqt_reader)	SQMR: Commands read from queue	SQMR	cmds	CmdsRead
//				--	24000 (special: see code)			SQT: Commands read from queue	SQT	cmds	CmdsRead
//				--	30000 (@src_dbid)				DIST: Commands read from inbound queue	DIST	cmds	CmdsRead
//				--	6000  (@dest_ldbid)				SQM: Commands written to queue	SQM	CmdsWritten
//				--	6020  (@dest_ldbid)				SQM: Active queue segments	SQM	SegsActive
//				--	62013 (@src_ldbid, @sqt_reader)			SQMR: Unread segments	SQMR	SQMRBacklogSeg
//				--	62013 (@dest_dbid, 10)				SQMR: Unread segments	SQMR	SQMRBacklogSeg
//				--	5030  (@dest_dbid)				DSI: Commands read from outbound queue	DSI	cmds	DSICmdsRead
//				rsi2db
//				--	59001	(src_rsid)	RSIUSER: RSI messages received	RSIUSER	RSIUMsgRecv
//				--	6000	(dest_ldbid)	SQM: Commands written to queue	SQM	CmdsWritten
//				--	6020	(dest_dbid)	SQM: Active queue segments	SQM	SegsActive
//				--	62013	(dest_dbid)	SQMR: Unread segments	SQMR	SQMRBacklogSeg
//				--	5030	(dest_dbid)	DSI: Commands read from outbound queue	DSI	cmds	DSICmdsRead
//				db2rsi
//				--	58000	(src_dbid)		Repagent: Commands received	REPAGENT	CmdsRecv
//				--	6000	(src_dbid)		SQM: Commands written to queue	SQM	CmdsWritten
//				--	6020	(src_dbid)		SQM: Active queue segments	SQM	SegsActive
//				--	62000	(src_ldbid, sqt_reader)	SQMR: Commands read from queue	SQMR	cmds	CmdsRead
//				--	24000	(src_ldbid)		SQT: Commands read from queue	SQT	cmds	CmdsRead
//				--	30000	(src_dbid)		DIST: Commands read from inbound queue	DIST	cmds	CmdsRead
//				--	6000	(dest_rsid)		SQM: Commands written to queue	SQM	CmdsWritten
//				--	6020	(dest_rsid)		SQM: Active queue segments	SQM	SegsActive
//				--	62013	(src_ldbid)		SQMR: Unread segments	SQMR	SQMRBacklogSeg
//				-- route backlog SQMR rs_statdetail.label looks like: SQMR, 16777320:0 BARCELONA15_RS_1, 0, GLOBAL RS
//				-- but the easy way to tell is that the instance_id is the @dest_rsid
//				--	62013	(dest_rsid)		SQMR: Unread segments	SQMR	SQMRBacklogSeg
//				--	4004	(dest_rsid)		RSI: Messages sent with type RSI_MESSAGE	RSI	MsgsSent
				
				Set<Integer> counterIdSet = new HashSet<Integer>();
				counterIdSet.add(58000); // 58000	Repagent: Commands received	REPAGENT	CmdsRecv
				counterIdSet.add(6000);  // 6000	SQM: Commands written to queue	SQM	CmdsWritten
				counterIdSet.add(6020);  // 6020	SQM: Active queue segments	SQM	SegsActive
				counterIdSet.add(62000); // 62000	SQMR: Commands read from queue	SQMR	cmds	CmdsRead
				counterIdSet.add(24000); // 24000	SQT: Commands read from queue	SQT	cmds	CmdsRead
				counterIdSet.add(30000); // 30000	DIST: Commands read from inbound queue	DIST	cmds	CmdsRead
//				counterIdSet.add(6000);  // 6000	SQM: Commands written to queue	SQM	CmdsWritten
//				counterIdSet.add(6020);  // 6020	SQM: Active queue segments	SQM	SegsActive
				counterIdSet.add(62013); // 62013	SQMR: Unread segments	SQMR	SQMRBacklogSeg
				counterIdSet.add(5030);  // 5030	DSI: Commands read from outbound queue	DSI	cmds	DSICmdsRead
		
				// rsi2db
				counterIdSet.add(59001); // 59001	RSIUSER: RSI messages received	RSIUSER	RSIUMsgRecv
		
				// db2rsi
				counterIdSet.add(4004);  // 4004	RSI: Messages sent with type RSI_MESSAGE	RSI	MsgsSent

				
				DbxTuneResultSetMetaData xrstm = new DbxTuneResultSetMetaData();
				xrstm.addStrColumn("type",               1,  false, 10, "what type is this: db2db, rsi2db, db2rsi");
				xrstm.addStrColumn("rsType",             2,  false, 10, "what RepType do we use: TABLE, MSA, WS");
				xrstm.addStrColumn("source",             3,  false, 62, "Source of the data");
				xrstm.addStrColumn("destination",        4,  false, 62, "Destination");
		
				xrstm.addLongColumn("RACmds",            5,  true, "RepAgent commands received"); // db2db
				xrstm.addLongColumn("SQMInCmds",         6,  true, "SQM Input"); // db2db
				xrstm.addLongColumn("SegsActiveInMB",    7,  true, "Input Active Segments in MB"); // db2db
				xrstm.addLongColumn("BacklogInMB",       8,  true, "Input Backlog"); // db2db
				xrstm.addLongColumn("SQMRCmds",          9,  true, "SQMR(SQT) commands procecced"); // db2db
				xrstm.addLongColumn("SQTCmds",           10, true, "SQT commands procecced"); // db2db
				xrstm.addLongColumn("DISTCmds",          11, true, "Dist commands procecced"); // db2db
				xrstm.addLongColumn("SQMOutCmds",        12, true, "SQM Output commands processed"); // db2db
				xrstm.addLongColumn("SegsActiveOutMB",   13, true, "Output Active segments in MB"); // db2db
				xrstm.addLongColumn("DSICmds",           14, true, "DSI commands processed"); // db2dbS
				xrstm.addBigDecimalColumn("PerfInd",     15, true, 5, 1, "<html>"
						+ "Deviation from optimal performance.<br>"
						+ "<ul>"
						+ "  <li>Near 0: <b>This is good.</b> Meaning: The DSI is applying Commands in same speed as we receive RepAgent Coammands. (RACmds =~ DSICmds)</li>"
						+ "  <li>Less than 0: The DSI is slower than the RepAgent feed. (RACmds > DSICmds)</li>"
						+ "  <li>Above 0: The DSI is faster than the RepAgent feed. The RA has probably slowed down or decreased it's rate and DSI is eating up the backlog (RACmds < DSICmds)</li>"
						+ "</ul>"
						+ "</html>");
				xrstm.addStrColumn ("ExtraInfo",         16, true, 255, ""); // db2db

				xrstm.setPkCol("source", "destination");
//				xrstm.addLongColumn("RSIUMsgSec",        4, true, ""); // rsi2db
//				xrstm.addLongColumn("SQMOutCmdsPerSec",  5, true, ""); // rsi2db
//				xrstm.addLongColumn("SegsActiveOut",     6, true, ""); // rsi2db
//				xrstm.addLongColumn("SegsActiveOutMB",   7, true, ""); // rsi2db
//				xrstm.addLongColumn("DSICmdsSec",        8, true, ""); // rsi2db
		//
//				xrstm.addLongColumn("RACmdsPerSec",      4, true, ""); // db2rsi
//				xrstm.addLongColumn("SQMInCmdsPerSec",   5, true, ""); // db2rsi
//				xrstm.addLongColumn("SegsActiveInMB",    6, true, ""); // db2rsi
//				xrstm.addLongColumn("SQMRCmdsPerSec",    7, true, ""); // db2rsi
//				xrstm.addLongColumn("SQTCmdsPerSec",     8, true, ""); // db2rsi
//				xrstm.addLongColumn("DISTCmdsPerSec",    9, true, ""); // db2rsi
//				xrstm.addLongColumn("SQMOutCmdsPerSec", 10, true, ""); // db2rsi
//				xrstm.addLongColumn("SegsActiveOutMB",  11, true, ""); // db2rsi
//				xrstm.addLongColumn("BacklogOutMB",     12, true, ""); // db2rsi
//				xrstm.addLongColumn("RSIMsgsSec",       13, true, ""); // db2rsi
		
//					values ('RS15.7', 'RateSummary db2db', 'RS Performance Summary (cmds/sec, backlog in MB):','RepAgent', 'SQM(i)', 'Active(i)',  'Backlog(i)', 'SQMR(SQT)', 'SQT',  'DIST',   'SQM(o)',    'Active(o)',  'DSI')
//					values ('RS15.7', 'RateSummary db2rsi','RS Performance Summary (cmds/sec, backlog in MB):','RepAgent', 'SQM(i)', 'Backlog(i)', 'SQMR(SQT)',  'SQT',       'DIST', 'SQM(o)', 'Active(r)', 'Backlog(r)', 'RSI')
//					values ('RS15.7', 'RateSummary rsi2db','RS Performance Summary (cmds/sec, backlog in MB):','RSIUser',  'SQM(i)', 'Active(i)',  'ActiveMB',   'DSI',       null,   null,     null,        null,         null)
				
				// Now set MetaData information...
				setColumnNames (xrstm.getColumnNames());
				setSqlType     (xrstm.getSqlTypes());
				setSqlTypeNames(xrstm.getSqlTypeNames());
				setColClassName(xrstm.getClassNames());

//				xrstm.setPkCol(cm.getPk());
				
				cm.setDiffColumns(DIFF_COLUMNS);

				setPkColArray(xrstm.getPkColArray());

				initPkStructures();

				if ( ! cm.hasResultSetMetaData() )
					cm.setResultSetMetaData(xrstm);


				// Reuse the sample time from CmAdminStats
				setSampleTime(cmAdminStats.getSampleTime());
				setSampleInterval(cmAdminStats.getSampleInterval());
				

				//-----------------------------------------------------
				// Get all records for the counters we want...
				//-----------------------------------------------------
				List<List<Object>> rows = cmAdminStats.getCounterIds(DATA_ABS, counterIdSet);

				if (rows.size() == 0)
				//	return false;
					throw new NoValidRowsInSample("Could not find any records for '"+CM_NAME+"' in CM '"+CmAdminStats.SHORT_NAME+"'.");

		
				// Find column Id's
				List<String> colNames = cmAdminStats.getColNames(DATA_ABS);
				if (colNames == null)
					throw new NoValidRowsInSample("Could not find any columns in CM '"+CmAdminStats.SHORT_NAME+"'.");
		
				for (int colId = 0; colId < colNames.size(); colId++)
				{
					String colName = (String) colNames.get(colId);
					if      (colName.equals("Module"))         _module_pos          = colId;
					else if (colName.equals("Instance"))       _instance_pos        = colId;
					else if (colName.equals("InstanceId"))     _instanceId_pos      = colId;
					else if (colName.equals("ModTypeInstVal")) _modTypeInstVal_pos  = colId;
					else if (colName.equals("CounterId"))      _counterId_pos       = colId;
					else if (colName.equals("Type"))           _type_pos            = colId;
					else if (colName.equals("Name"))           _name_pos            = colId;
					else if (colName.equals("Obs"))            _obs_pos             = colId;
					else if (colName.equals("Total"))          _total_pos           = colId;
					else if (colName.equals("Last"))           _last_pos            = colId;
//					else if (colName.equals("RateXsec"))       _rateXsec_pos        = colId;
				}
				
				for (RsDbInfo rsDbInfo : _rsDbInfoList)
				{
					System.out.println("-----------------------------------------------------------------------------");
					System.out.println("RsDbInfo: "+rsDbInfo);
					if (rsDbInfo.isDb2Db())
					{
						String x_type   = "DB->DB";
						String x_rsType = rsDbInfo.type;
						String x_source = rsDbInfo.src_connection;
						String x_dest   = rsDbInfo.dest_connection;
						
						int    src_dbid    = -1;
						int    dest_dbid   = -1;
						int    src_ldbid   = -1;
						int    dest_ldbid  = -1;
						int    sqtReader   = -1; // norm=11 or ws=21 ... Not sure about this one.. will be different if it's a WS or a normal replication
						String x_extraInfo = "";
						if ("TABLE".equals(rsDbInfo.type))
						{
							src_dbid    = rsDbInfo.src_dbid;
							dest_dbid   = rsDbInfo.dest_dbid;
							src_ldbid   = src_dbid;
							dest_ldbid  = dest_dbid;
							sqtReader   = 11;
							x_extraInfo = rsDbInfo.getTableInfo();
						}
						else if ("MSA".equals(rsDbInfo.type))
						{
							src_dbid    = rsDbInfo.src_dbid;
							dest_dbid   = rsDbInfo.dest_dbid;
							src_ldbid   = src_dbid;
							dest_ldbid  = dest_dbid;
							sqtReader   = 11;
							x_extraInfo = rsDbInfo.getMsaInfo();
						}
						else if ("WS".equals(rsDbInfo.type))
						{
							src_dbid    = rsDbInfo.src_dbid;
							dest_dbid   = rsDbInfo.dest_dbid;
							src_ldbid   = rsDbInfo.ldbid;
							dest_ldbid  = rsDbInfo.ldbid;
//							sqtReader   = 21;
							sqtReader   = 11;
							x_extraInfo = rsDbInfo.getWsInfo();
						}

						
						
						Long RACmds          = getRowCounter(rows, 58000, src_dbid,  -1);           // 58000	Repagent: Commands received	REPAGENT	CmdsRecv
						Long SQMInCmds       = getRowCounter(rows, 6000,  src_ldbid,  1);           // 6000	SQM: Commands written to queue	SQM	CmdsWritten
						Long SegsActiveInMB  = getRowCounter(rows, 6020,  src_ldbid,  1);           // 6020	SQM: Active queue segments	SQM	SegsActive
						Long BacklogInMB     = getRowCounter(rows, 62013, src_ldbid,  sqtReader);   // 62013	SQMR: Unread segments	SQMR	SQMRBacklogSeg
						Long SQMRCmds        = getRowCounter(rows, 62000, src_ldbid,  sqtReader);   // 62000	SQMR: Commands read from queue	SQMR	cmds	CmdsRead
						Long SQTCmds         = getRowCounter(rows, 24000, dest_dbid,  1);           // 24000	SQT: Commands read from queue	SQT	cmds	CmdsRead
						Long DISTCmds        = getRowCounter(rows, 30000, src_dbid,   -1);          // 30000	DIST: Commands read from inbound queue	DIST	cmds	CmdsRead
						Long SQMOutCmds      = getRowCounter(rows, 6000,  dest_ldbid, 0);           // 6000	SQM: Commands written to queue	SQM	CmdsWritten
						Long SegsActiveOutMB = getRowCounter(rows, 6020,  dest_ldbid, 0);           // 6020	SQM: Active queue segments	SQM	SegsActive
						Long DSICmds         = getRowCounter(rows, 5030,  dest_dbid,  -1);          // 5030	DSI: Commands read from outbound queue	DSI	cmds	DSICmdsRead

						BigDecimal PerfInd = new BigDecimal(0).setScale(1, BigDecimal.ROUND_HALF_EVEN);
						if ( RACmds != null && DSICmds != null)
						{
							if (RACmds > 0)
							{
								PerfInd = new BigDecimal( ((DSICmds.doubleValue()-RACmds.doubleValue())/RACmds.doubleValue())*100.0 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
								if (PerfInd.intValue() > 100)
									PerfInd = new BigDecimal(100).setScale(1, BigDecimal.ROUND_HALF_EVEN); 
							}
							else
							{
								if (DSICmds > 0)
									PerfInd = new BigDecimal(100).setScale(1, BigDecimal.ROUND_HALF_EVEN); 
							}
						}

						
						List<Object> row = new ArrayList<Object>();
						row.add(x_type);
						row.add(x_rsType);
						row.add(x_source);
						row.add(x_dest);

						row.add(RACmds);
						row.add(SQMInCmds);
						row.add(SegsActiveInMB);
						row.add(BacklogInMB);
						row.add(SQMRCmds);
						row.add(SQTCmds);
						row.add(DISTCmds);
						row.add(SQMOutCmds);
						row.add(SegsActiveOutMB);
						row.add(DSICmds);

						row.add(PerfInd);
						row.add(x_extraInfo);

						addRow(CmRsSrcToDest.this, row);
					}
					else if (rsDbInfo.isRsi2Db())
					{
						String x_type = "RSI->DB";
					}
					else if (rsDbInfo.isDb2Rsi())
					{
						String x_type = "DB->RSI";
					}
					else
					{
					}
				}
				
//				for (List<Object> row : rows)
//				{
//					counterId = (Integer)  row.get(counterId_pos);
//		
//					instance   = (String)  row.get(instance_pos);
//					instanceId = (Integer) row.get(instanceId_pos);
//					type       = (String)  row.get(type_pos);
//					name       = (String)  row.get(name_pos);
//					obs        = (Long)    row.get(obs_pos);
//					total      = (Long)    row.get(total_pos);
//					last       = (Long)    row.get(last_pos);
////					rateXsec   = (Long)    row.get(rateXsec_pos);
//					
//					System.out.println("instance='"+instance+"', instanceId="+instanceId+", type='"+type+"', name='"+name+"', obs="+obs+", total="+total+", last="+last+".");
//				}
				
				
				
//				// Finally add the rows
//				for (List<Object> row : rows)
//				{
//					addRow(row);
//				}
				
				return true;
			}
			
			private Long getRowCounter(List<List<Object>> rows, int inCounterId, int inInstanceId, int inInstanceVal)
			{
				for (List<Object> row : rows)
				{
					Integer counterId = (Integer)  row.get(_counterId_pos);
					if ( counterId != inCounterId)
						continue;
		
					Integer instanceId = (Integer) row.get(_instanceId_pos);
					if ( instanceId != inInstanceId)
						continue;

					Integer modTypeInstVal = (Integer) row.get(_modTypeInstVal_pos);
					if (inInstanceVal >= 0)
					{
						if ( modTypeInstVal != inInstanceVal)
							continue;
					}

					String  instance   = (String)  row.get(_instance_pos);
					String  type       = (String)  row.get(_type_pos);
					String  name       = (String)  row.get(_name_pos);
					Long    obs        = (Long)    row.get(_obs_pos);
					Long    total      = (Long)    row.get(_total_pos);
					Long    last       = (Long)    row.get(_last_pos);
					
					System.out.println("counterId="+counterId+"("+name+"), instanceId="+instanceId+", modTypeInstVal="+modTypeInstVal+", instance='"+instance+"', type='"+type+"', name='"+name+"', obs="+obs+", total="+total+", last="+last+".");

					if      ("OBSERVER".equals(type)) return obs;
					else if ("MONITOR" .equals(type)) return last;

				}
				System.out.println(">> NO VAL: inCounterId="+inCounterId+", inInstanceId="+inInstanceId+", inInstanceVal="+inInstanceVal+".");
				return null;
			}
		};
	}

	/** 
	 * Calculate the perfInd column based on DIFF Values for this sample
	 */
	@Override
	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
	{
		int RACmds_pos  = -1;
		int DSICmds_pos = -1;
		int PerfInd_pos = -1;

		// Find column Id's
		List<String> colNames = diffData.getColNames();
		if (colNames==null) return;
	
		for (int colId=0; colId < colNames.size(); colId++) 
		{
			String colName = colNames.get(colId);
			if      (colName.equals("RACmds"))   RACmds_pos  = colId;
			else if (colName.equals("DSICmds"))  DSICmds_pos = colId;
			else if (colName.equals("PerfInd"))  PerfInd_pos = colId;
		}

		// Loop on all diffData rows
		for (int rowId=0; rowId < diffData.getRowCount(); rowId++) 
		{
			Long RACmds	 = (Long)diffData.getValueAt(rowId, RACmds_pos );
			Long DSICmds = (Long)diffData.getValueAt(rowId, DSICmds_pos);

			BigDecimal PerfInd = new BigDecimal(0).setScale(1, BigDecimal.ROUND_HALF_EVEN);
			if ( RACmds != null && DSICmds != null)
			{
				if (RACmds > 0)
				{
					PerfInd = new BigDecimal( ((DSICmds.doubleValue()-RACmds.doubleValue())/RACmds.doubleValue())*100.0 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
					if (PerfInd.intValue() > 100)
						PerfInd = new BigDecimal(100).setScale(1, BigDecimal.ROUND_HALF_EVEN); 
				}
				else
				{
					if (DSICmds > 0)
						PerfInd = new BigDecimal(100).setScale(1, BigDecimal.ROUND_HALF_EVEN); 
				}
			}

			// Set the PerfIndicator column
			diffData.setValueAt(PerfInd, rowId, PerfInd_pos);
		}
	}
	
	@Override
	public String getToolTipTextOnTableColumnHeader(String colname)
	{
		ResultSetMetaData md = getResultSetMetaData();
		if (md instanceof DbxTuneResultSetMetaData)
		{
			DbxTuneResultSetMetaData xmd = (DbxTuneResultSetMetaData)md;
			return xmd.getDescription(colname);
		}

		return super.getToolTipTextOnTableColumnHeader(colname);
	}

	private String getInitSqlFromFile()
	{
		Class<CmRsSrcToDest> clazz    = CmRsSrcToDest.class;
		String                      filename = "CmAdminStatsSrc2Dest_init.sql";

		StringBuffer sb = new StringBuffer("");
		try
		{
			URL url = clazz.getResource(filename);
			if(url != null)
			{
				BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
				br = new BufferedReader(new InputStreamReader(url.openStream()));

				String row = null;
				for (row=br.readLine(); row!=null && !row.trim().equalsIgnoreCase("go"); row=br.readLine()) 
				{
					sb.append(row).append("\n");
				}
				br.close();
			}
			else
			{
				_logger.error("Problems reading file '"+filename+"'. at class '"+clazz+"'. The URL was null, returned from clazz.getResource(filename)");
			}
		}
		catch(IOException e)
		{
//			return null;
			_logger.error("Problems reading file '"+filename+"'. at class '"+clazz+"'. Caught: "+e, e);
		}

		if ( sb.length() == 0)
			return null;
		
		return sb.toString();
	}
	
	private static class RsDbInfo
	{
		String  type             = null; 
		int     local_rsid       = 0; 
		String  local_rsname     = null; 
		String  src_server       = null; 
		String  src_database     = null; 
		int     src_dbid         = 0; 
		String  src_connection   = null; 
		int     src_rsid         = 0; 
		String  src_rsname       = null; 
		boolean is_origin        = true; 
		String  dest_server      = null; 
		String  dest_database    = null; 
		int     dest_dbid        = 0; 
		String  dest_connection  = null; 
		int     dest_rsid        = 0; 
		String  dest_rsname      = null; 
		// only for TABLE 
		int     num_repdefs      = 0; 
		int     num_tables       = 0; 
		int     num_table_subscr = 0;
		// only for MSA 
		int     dbrepid          = 0; 
		String  db_repdef_name   = null; 
		String  db_subscr_name   = null; 
		int     num_subsets      = 0;
		// only for WS 
		String  logical_conn     = null; 
		int     ldbid            = 0;
		
		@Override
		public String toString()
		{
			return super.toString() 
					+ ": type='"+type+"', " 
					+ "local_rsid="+local_rsid+", local_rsname='"+local_rsname+"', "
					+ "src_server='"+src_server+"', src_database='"+src_database+"', src_dbid="+src_dbid+", src_connection='"+src_connection+"', src_rsid="+src_rsid+", src_rsname='"+src_rsname+"', "
					+ "is_origin="+is_origin+", "
					+ "dest_server='"+dest_server+"', dest_database='"+dest_database+"', dest_dbid="+dest_dbid+", dest_connection='"+dest_connection+"', dest_rsid="+dest_rsid+", dest_rsname='"+dest_rsname+"', "
					+ "TABLE(num_repdefs="+num_repdefs+", num_tables="+num_tables+", num_table_subscr="+num_table_subscr+"), "
					+ "MSA(dbrepid="+dbrepid+", db_repdef_name='"+db_repdef_name+"', db_subscr_name='"+db_subscr_name+"', num_subsets="+num_subsets+"), "
					+ "WS(logical_conn='"+logical_conn+"', ldbid="+ldbid+").";
		}

		public String getTableInfo()
		{
			return "num_repdefs="+num_repdefs+", num_tables="+num_tables+", num_table_subscr="+num_table_subscr;
		}

		public String getMsaInfo()
		{
			return "dbrepid="+dbrepid+", db_repdef_name='"+db_repdef_name+"', db_subscr_name='"+db_subscr_name+"', num_subsets="+num_subsets;
		}

		public String getWsInfo()
		{
			return "logical_conn='"+logical_conn+"', ldbid="+ldbid;
		}

		public boolean isDb2Db()
		{
			if (type.equals("WS")) 
				return true;

			return src_rsname.equals(dest_rsname);
		}
		public boolean isRsi2Db()
		{
			if (type.equals("WS")) 
				return false;
			
			return local_rsname.equals(dest_rsname);
		}
		public boolean isDb2Rsi()
		{
			if (type.equals("WS")) 
				return false;
			
			return ! local_rsname.equals(dest_rsname);
		}
	}
}
