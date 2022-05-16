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

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import javax.naming.NameNotFoundException;

import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CmSybMessageHandler;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.rs.helper.RsDbidStripper;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.graph.TrendGraphDataPoint.LabelType;
import com.asetune.gui.MainFrame;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.conn.info.DbmsVersionInfo;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmAdminStatsBacklog
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmAdminStatsBacklog.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmAdminStatsBacklog.class.getSimpleName();
	public static final String   SHORT_NAME       = "Stats Backlog";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<p>Replication Server Stable Queueu Backlog Information</p>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_DISK;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"admin_stats_backlog"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
//		"Obs",
		"Last"
//		"Max",
//		"Avg ttl/obs"
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

		return new CmAdminStatsBacklog(counterController, guiController);
	}

	public CmAdminStatsBacklog(ICounterController counterController, IGuiController guiController)
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
	public static final String GRAPH_NAME_QUEUE_SIZE = "BlQueueSize";

	private void addTrendGraphs()
	{
////		String[] labels = new String[] { "-added-at-runtime-" };
//		String[] labels = TrendGraphDataPoint.RUNTIME_REPLACED_LABELS;
//		
//		addTrendGraphData(GRAPH_NAME_QUEUE_SIZE,       new TrendGraphDataPoint(GRAPH_NAME_QUEUE_SIZE, labels, LabelType.Dynamic));

		addTrendGraph(GRAPH_NAME_QUEUE_SIZE,
			"Backlog Size from 'admin statistics, backlog' in MB (col 'Last', Absolute Value)", // Menu CheckBox text
			"Backlog Size from 'admin statistics, backlog' in MB (col 'Last', Absolute Value)", // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB,
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.SPACE,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height
		
//		// if GUI
//		if (getGuiController() != null && getGuiController().hasGUI())
//		{
//			// GRAPH
//			TrendGraph tg = null;
//
//			//-----
//			tg = new TrendGraph(GRAPH_NAME_QUEUE_SIZE,
//				"Backlog Size from 'admin statistics, backlog' in MB (col 'Last', Absolute Value)", // Menu CheckBox text
//				"Backlog Size from 'admin statistics, backlog' in MB (col 'Last', Absolute Value)", // Label 
//				labels, 
//				false, // is Percent Graph
//				this, 
//				false, // visible at start
//				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);   // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//		}
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
//		if (GRAPH_NAME_QUEUE_SIZE.equals(tgdp.getName()))
//		{
//			int size = 0;
//			for (int i = 0; i < this.size(); i++)
//			{
//				String monitorVal = this.getAbsString       (i, "Monitor");
//				Double Obs        = this.getAbsValueAsDouble(i, "Obs");
//				if (monitorVal.indexOf("SQMRBacklogSeg") >= 0 && Obs > 0.0)
//					size++;
//			}
//			
//			if (size == 0)
//			{
//				_logger.info("Skipping adding entry to graph '"+tgdp.getName()+"'. There are NO 'Monitor' (SQMRBacklogSeg) values with a 'Obs' value above 0");
//			}
//			else
//			{
//				// Write 1 "line" for every SQMRBacklogSeg row
//				Double[] dArray = new Double[size];
//				String[] lArray = new String[dArray.length];
//				int i2 = 0;
////				for (int i = 0; i < dArray.length; i++)
//				for (int i = 0; i < this.size(); i++)
//				{
//					String monitorVal = this.getAbsString       (i, "Monitor");
//					Double Obs        = this.getAbsValueAsDouble(i, "Obs");
//					if (monitorVal.indexOf("SQMRBacklogSeg") >= 0 && Obs > 0.0)
//					{
//						lArray[i2] = this.getAbsString       (i, "Instance");
//						dArray[i2] = this.getAbsValueAsDouble(i, "Last");
//						i2++;
//					}
//				}
//
//				// Set the values
//				tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
//			}
//		}
		if (GRAPH_NAME_QUEUE_SIZE.equals(tgdp.getName()))
		{
			// STEP 1 -- put all desired/wanted entries in a MAP with <LabelName, RowPos> so we in STEP 2, easily can add desired records to the GraphDataArray
			LinkedHashMap<String, Integer> segRowsToUse = new LinkedHashMap<>(); 
//			LinkedHashMap<String, Integer> blkRowsToUse = new LinkedHashMap<>(); 

			for (int i = 0; i < this.size(); i++)
			{
				String instance   = this.getAbsString       (i, "Instance");
				String monitorVal = this.getAbsString       (i, "Monitor");
				Double Obs        = this.getAbsValueAsDouble(i, "Obs");

				// Add SEGMENTS (1M sizes) in a separate Map
				if (monitorVal.indexOf("SQMRBacklogSeg") >= 0 && Obs > 0.0)
				{
					// Skip 'USER' entries
					if (instance.contains(", USER"))
						continue;

					String label = instance;

					// Remove prefix: "SQMR, "
					if (label.startsWith("SQMR, "))
						label = label.replace("SQMR, ", "");
					
					// Remove DBID and append ('in-q' or 'out-q')
					// "108:1 LDS.mts, 0, SQT" -->> "LDS.mts, 0, SQT (in-q)"
					//  ^^^ ^  <<<<< remove DBID and if ':1' append with ' (in-q)', and of ':0' append with ' (out-q)'
					label = RsDbidStripper.stripDbid(label);
					
					Integer existingRowId = segRowsToUse.put(label, i);
					if (existingRowId != null)
						_logger.warn("DUPLICATE row for graph '" + tgdp.getName() + "'. label='" + label + "', CurrentRowId=" + i + ", ExistingRowId=" + existingRowId + ". Current instance='" + instance + "', Existing instance='" + this.getAbsString(existingRowId, "Instance") + "'.");
				}

				// Possibly also add "Blocks" (number of 16K blocked used)
				// But we need to add them via to a separate Map
				// and when we get the records we need to apply: #blocks * 16.0 / 1024.0
//				if (monitorVal.indexOf("SQMRBacklogBlock") >= 0 && Obs > 0.0)
//				{
//					possibly the same logic as above... and then at STEP 2... merge the maps seg & blk!
//				}
			}

			if (segRowsToUse.isEmpty())
			{
				_logger.info("Skipping adding entry to graph '"+tgdp.getName()+"'. There are NO 'Monitor' (SQMRBacklogSeg) values with a 'Obs' value above 0");
			}
			else
			{
				// Write 1 "line" for every SQMRBacklogSeg row
				Double[] dArray = new Double[segRowsToUse.size()];
				String[] lArray = new String[dArray.length];

				int i2 = 0;
				for (Entry<String, Integer> entry : segRowsToUse.entrySet())
				{
					String label = entry.getKey();
					int    row   = entry.getValue();

					lArray[i2] = label;
					dArray[i2] = this.getAbsValueAsDouble(row, "Last");

					i2++;
				}

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
			}
		}
	}
	

	@Override
	protected CmSybMessageHandler createSybMessageHandler()
	{
		CmSybMessageHandler msgHandler = super.createSybMessageHandler();
		
		msgHandler.addDiscardMsgNum(0);

		return msgHandler;
	}

	
//	1> admin statistics, backlog
//	Report Time:		01/29/15 05:41:28 PM
//	RS> Col# Label       JDBC Type Name         Guessed DBMS type
//	RS> ---- ----------- ---------------------- -----------------
//	RS> 1    Instance    java.sql.Types.CHAR    char(255)        
//	RS> 2    Monitor     java.sql.Types.CHAR    char(31)         
//	RS> 3    Obs         java.sql.Types.NUMERIC numeric(22,0)    
//	RS> 4    Last        java.sql.Types.NUMERIC numeric(22,0)    
//	RS> 5    Max         java.sql.Types.NUMERIC numeric(22,0)    
//	RS> 6    Avg ttl/obs java.sql.Types.NUMERIC numeric(22,0)    
//	+-----------------------------------------------+---------------+---+----+---+-----------+
//	|Instance                                       |Monitor        |Obs|Last|Max|Avg ttl/obs|
//	+-----------------------------------------------+---------------+---+----+---+-----------+
//	|SQMR, 101:0 GORAN_1_ERSSD.GORAN_1_ERSSD, 0, DSI|*SQMRBacklogSeg|0  |0   |0  |0          |
//	|SQMR, 102:0 L_SRV.dbname1, 0, GLOBAL RS        |*SQMRBacklogSeg|340|0   |0  |0          |
//	|SQMR, 102:1 L_SRV.dbname1, 0, GLOBAL RS        |*SQMRBacklogSeg|340|0   |0  |0          |
//	|SQMR, 103:0 L_SRV.dbname2, 0, GLOBAL RS        |*SQMRBacklogSeg|340|0   |0  |0          |
//	|SQMR, 103:1 L_SRV.dbname2, 0, GLOBAL RS        |*SQMRBacklogSeg|340|0   |0  |0          |
//	|SQMR, 104:0 L_SRV.dbname3, 0, GLOBAL RS        |*SQMRBacklogSeg|340|0   |0  |0          |
//	|SQMR, 104:1 L_SRV.dbname3, 0, GLOBAL RS        |*SQMRBacklogSeg|340|0   |0  |0          |
//	|SQMR, 105:0 L_SRV.dbname4, 0, GLOBAL RS        |*SQMRBacklogSeg|340|0   |0  |0          |
//	|SQMR, 105:1 L_SRV.dbname4, 0, GLOBAL RS        |*SQMRBacklogSeg|340|0   |0  |0          |
//	+-----------------------------------------------+---------------+---+----+---+-----------+
//	Rows 9
//	(9 rows affected)
//	===============================================================================
//	Report Time:		01/29/15 05:41:28 PM
//	RS> Col# Label       JDBC Type Name         Guessed DBMS type
//	RS> ---- ----------- ---------------------- -----------------
//	RS> 1    Instance    java.sql.Types.CHAR    char(255)        
//	RS> 2    Monitor     java.sql.Types.CHAR    char(31)         
//	RS> 3    Obs         java.sql.Types.NUMERIC numeric(22,0)    
//	RS> 4    Last        java.sql.Types.NUMERIC numeric(22,0)    
//	RS> 5    Max         java.sql.Types.NUMERIC numeric(22,0)    
//	RS> 6    Avg ttl/obs java.sql.Types.NUMERIC numeric(22,0)    
//	+-----------------------------------------------+-----------------+---+----+---+-----------+
//	|Instance                                       |Monitor          |Obs|Last|Max|Avg ttl/obs|
//	+-----------------------------------------------+-----------------+---+----+---+-----------+
//	|SQMR, 101:0 GORAN_1_ERSSD.GORAN_1_ERSSD, 0, DSI|*SQMRBacklogBlock|0  |0   |0  |0          |
//	|SQMR, 102:0 L_SRV.dbname1, 0, GLOBAL RS        |*SQMRBacklogBlock|341|0   |0  |0          |
//	|SQMR, 102:1 L_SRV.dbname1, 0, GLOBAL RS        |*SQMRBacklogBlock|341|0   |0  |0          |
//	|SQMR, 103:0 L_SRV.dbname2, 0, GLOBAL RS        |*SQMRBacklogBlock|341|0   |0  |0          |
//	|SQMR, 103:1 L_SRV.dbname2, 0, GLOBAL RS        |*SQMRBacklogBlock|341|0   |0  |0          |
//	|SQMR, 104:0 L_SRV.dbname3, 0, GLOBAL RS        |*SQMRBacklogBlock|341|0   |0  |0          |
//	|SQMR, 104:1 L_SRV.dbname3, 0, GLOBAL RS        |*SQMRBacklogBlock|341|0   |0  |0          |
//	|SQMR, 105:0 L_SRV.dbname4, 0, GLOBAL RS        |*SQMRBacklogBlock|341|0   |0  |0          |
//	|SQMR, 105:1 L_SRV.dbname4, 0, GLOBAL RS        |*SQMRBacklogBlock|341|0   |0  |0          |
//	+-----------------------------------------------+-----------------+---+----+---+-----------+
//	Rows 9
//	(9 rows affected)
//	===============================================================================


//	@Override
//	protected TabularCntrPanel createGui()
//	{
//		return new CmRaSysmonPanel(this);
//	}

	@Override
	public String[] getDependsOnConfigForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return NEED_CONFIG;
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addTable("admin_stats_backlog",  "");

			mtd.addColumn("admin_stats_backlog", "Instance",    "<html>FIXME</html>");
			mtd.addColumn("admin_stats_backlog", "Monitor",     "<html>FIXME</html>");
			mtd.addColumn("admin_stats_backlog", "Obs",         "<html>FIXME</html>");
			mtd.addColumn("admin_stats_backlog", "Last",        "<html>FIXME</html>");
			mtd.addColumn("admin_stats_backlog", "Max",         "<html>FIXME</html>");
			mtd.addColumn("admin_stats_backlog", "Avg ttl/obs", "<html>FIXME</html>");
		}
		catch (NameNotFoundException e) {/*ignore*/}
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("Instance");
		pkCols.add("Monitor");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		String sql = "admin statistics, backlog";
		return sql;
	}
}


//################################################################################################################
// The below is from MaxM DbxCentral
// * Possibly remove: ID:{1|0}   -- This due to when we drop and recreate a connection, the ID will be a new one... but we still want to *keep* the same name
// * And possibly remove records with ", USER" 
// * maybe: add filter so we can "skip" some names like "dummy" or similar
//################################################################################################################

// RS> Col# Label                             
// RS> ---- --------------------------------- 
// RS> 1    SessionStartTime                  
// RS> 2    SessionSampleTime                 
// RS> 3    CmSampleTime                      
// 
// RS> 29   SQMR, 108:0 LDS.mts, 0, USER      <<-- possibly remove
// RS> 30   SQMR, 108:1 LDS.mts, 0, USER      <<-- possibly remove
// RS> 31   SQMR, 108:1 LDS.mts, 0, SQT       <<<< Duplicate RS: 31, 4
// RS> 32   SQMR, 108:1 LDS.mts, 1, DSI       <<<< Duplicate RS: 32, 5
// RS> 4    SQMR, 209:1 LDS.mts, 0, SQT       <<<< Duplicate RS: 31, 4
// RS> 19   SQMR, 209:1 LDS.mts, 0, DSI       
// RS> 5    SQMR, 209:1 LDS.mts, 1, DSI       <<<< Duplicate RS: 32, 5
// RS> 12   SQMR, 209:1 LDS.mts, 1, SQT       
// 
// RS> 35   SQMR, 114:1 LDS.Linda, 0, SQT     <<<< Duplicate RS: 6, 35
// RS> 36   SQMR, 114:1 LDS.Linda, 1, DSI     <<<< Duplicate RS: 7, 36
// RS> 6    SQMR, 215:1 LDS.Linda, 0, SQT     <<<< Duplicate RS: 6, 35
// RS> 7    SQMR, 215:1 LDS.Linda, 1, DSI     <<<< Duplicate RS: 7, 36
// RS> 13   SQMR, 215:1 LDS.Linda, 0, DSI     
// RS> 14   SQMR, 215:1 LDS.Linda, 1, SQT     
// 
// RS> 37   SQMR, 117:1 LDS.PML, 0, SQT       <<<< Duplicate RS: 8, 37
// RS> 38   SQMR, 117:1 LDS.PML, 1, DSI       <<<< Duplicate RS: 9, 38
// RS> 8    SQMR, 218:1 LDS.PML, 0, SQT       <<<< Duplicate RS: 8, 37
// RS> 9    SQMR, 218:1 LDS.PML, 1, DSI       <<<< Duplicate RS: 9, 38
// RS> 15   SQMR, 218:1 LDS.PML, 0, DSI       
// RS> 16   SQMR, 218:1 LDS.PML, 1, SQT       
// 
// RS> 27   SQMR, 105:1 LDS.fs, 0, SQT        <<<< Duplicate RS: 10, 27
// RS> 28   SQMR, 105:1 LDS.fs, 1, DSI        <<<< Duplicate RS: 11, 28
// RS> 10   SQMR, 221:1 LDS.fs, 0, SQT        <<<< Duplicate RS: 10, 27
// RS> 11   SQMR, 221:1 LDS.fs, 1, DSI        <<<< Duplicate RS: 11, 28
// RS> 22   SQMR, 221:1 LDS.fs, 0, DSI        
// RS> 23   SQMR, 221:1 LDS.fs, 1, SQT        
// 
// RS> 33   SQMR, 111:1 LDS.b2b, 0, SQT       
// RS> 34   SQMR, 111:1 LDS.b2b, 1, DSI       
// RS> 20   SQMR, 212:1 LDS.b2b, 0, DSI       
// RS> 21   SQMR, 212:1 LDS.b2b, 1, SQT       
// 
// RS> 43   SQMR, 126:0 GORANS.dummy, 0, USER <<-- possibly remove
// RS> 44   SQMR, 126:1 GORANS.dummy, 0, USER <<-- possibly remove
// 
// 
// RS> 24   SQMR, 102:1 LDS.gorans, 0, USER   <<-- possibly remove
// RS> 25   SQMR, 102:1 LDS.gorans, 0, SQT    <<<< Duplicate RS: 25, 39, 41
// RS> 26   SQMR, 102:1 LDS.gorans, 1, DSI    <<<< Duplicate RS: 26, 40, 42
// RS> 17   SQMR, 206:1 LDS.gorans, 0, DSI    
// RS> 18   SQMR, 206:1 LDS.gorans, 1, SQT    
// RS> 39   SQMR, 120:1 LDS.gorans, 0, SQT    <<<< Duplicate RS: 25, 39, 41
// RS> 40   SQMR, 120:1 LDS.gorans, 1, DSI    <<<< Duplicate RS: 26, 40, 42
// RS> 41   SQMR, 123:1 LDS.gorans, 0, SQT    <<<< Duplicate RS: 25, 39, 41
// RS> 42   SQMR, 123:1 LDS.gorans, 1, DSI    <<<< Duplicate RS: 26, 40, 42
