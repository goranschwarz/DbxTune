package com.asetune.cm.rax;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import javax.naming.NameNotFoundException;

import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CmSybMessageHandler;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.DbxTuneResultSetMetaData;
import com.asetune.cm.rax.gui.CmRaStatisticsPanel;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.graph.TrendGraphDataPoint.LabelType;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.gui.TrendGraph;
import com.asetune.utils.Configuration;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmRaStatistics
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmRaStatistics.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmRaStatistics.class.getSimpleName();
	public static final String   SHORT_NAME       = "RA Statistics";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<p>Replication Agent Statistics</p>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final int      NEED_SRV_VERSION = 0;
	public static final int      NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"ra_statistics"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"NumberValue"
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

		return new CmRaStatistics(counterController, guiController);
	}

	public CmRaStatistics(ICounterController counterController, IGuiController guiController)
	{
		super(counterController,
				CM_NAME, GROUP_NAME, /*sql*/null, /*pkList*/null, 
				DIFF_COLUMNS, PCT_COLUMNS, MON_TABLES, 
				NEED_ROLES, NEED_CONFIG, NEED_SRV_VERSION, NEED_CE_VERSION, 
				NEGATIVE_DIFF_COUNTERS_TO_ZERO, IS_SYSTEM_CM, DEFAULT_POSTPONE_TIME);

		setDisplayName(SHORT_NAME);
		setDescription(HTML_DESC);

		setIconFile(GUI_ICON_FILE);

		setBackgroundDataPollingEnabled(true, false);

		setCounterController(counterController);
		setGuiController(guiController);
		
		addTrendGraphs();
		
		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	private static final String  PROP_PREFIX                       = CM_NAME;

	public static final String  PROPKEY_sample_resetAfter          = PROP_PREFIX + ".sample.reset.after";
	public static final boolean DEFAULT_sample_resetAfter          = false;

	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmRaStatisticsPanel(this);
	}

	@Override
	protected void registerDefaultValues()
	{
		super.registerDefaultValues();

		Configuration.registerDefaultValue(PROPKEY_sample_resetAfter,  DEFAULT_sample_resetAfter);
	}


	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public List<CmSettingsHelper> getLocalSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		
		list.add(new CmSettingsHelper("Clear Counters", PROPKEY_sample_resetAfter , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_resetAfter  , DEFAULT_sample_resetAfter  ), DEFAULT_sample_resetAfter, CmRaStatisticsPanel.TOOLTIP_sample_resetAfter ));

		return list;
	}

	
	// NOTE: storage table name will be CmName_GraphName, so try to keep the name short
	public static final String GRAPH_NAME_JVM_MEM          = "JvmMemory";  
	public static final String GRAPH_NAME_JVM_MEM_PCT      = "JvmMemoryPct";  
	public static final String GRAPH_NAME_LTL_CMD_SENT     = "LtlCmdSent";  
	public static final String GRAPH_NAME_LTL_BYTES_SENT   = "LtlBytesSent";  
	public static final String GRAPH_NAME_LTL_KB_SENT      = "LtlKbSent";  
	public static final String GRAPH_NAME_LTL_MB_SENT      = "LtlMbSent";  
	public static final String GRAPH_NAME_LTI_QUEUE_SIZE   = "LtiQueueSize";  
	public static final String GRAPH_NAME_LR_OPERATIONS    = "LrOperations";  
	public static final String GRAPH_NAME_LR_TRANS         = "LrTrans";  
	public static final String GRAPH_NAME_LR_MAINT_FILTER  = "LrMaintFilter";  
	public static final String GRAPH_NAME_GLOBAL_LRU_CACHE = "GlobalLruCache";
	public static final String GRAPH_NAME_ORA_LR           = "OraLogRecords";	
	public static final String GRAPH_NAME_ORA_LOB_OP       = "OraLobOper";	
	public static final String GRAPH_NAME_ORA_RASD_CACHE   = "RasdCache";	

	private void addTrendGraphs()
	{
		String[] labels_jvmMem         = new String[] { "Maximum Memory - MB", "Total Memory Allocated - MB", "Free Memory - MB", "Memory Usage - MB" };
		String[] labels_jvmMemPct      = new String[] { "JVM % max memory used" };
		String[] labels_ltlCmdSent     = new String[] { "Number of LTL commands sent" };
		String[] labels_ltlBytesSent   = new String[] { "Total bytes Sent to RS" };
		String[] labels_ltlKbSent      = new String[] { "Total KB Sent to RS" };
		String[] labels_ltlMbSent      = new String[] { "Total MB Sent to RS" };
		String[] labels_ltiQueueSize   = new String[] { "Current number of commands in the LTI queue" };
		String[] labels_lrOperations   = new String[] { "Scanned", "Processed", "Skipped"};
		String[] labels_lrTrans        = new String[] { "Processed", "Skipped", "Opened", "Closed", "Committed", "Aborted (rolled back)", "System transactions skipped" };
		String[] labels_lrMaintFilter  = new String[] { "Maint user operations read from log and skipped" };
		String[] labels_globalLruCache = new String[] { "Number of object references in the internal LRU cache" };
		String[] labels_oraLr          = new String[] { "Queued", "Filtered" };
		String[] labels_oraLobOp       = new String[] { "Total LOB operations processed by query data from PDB" };
		String[] labels_oraRasdCache   = new String[] { "Proc cache size (abs)", "Proc hits (per sec)", "Proc misses (per sec)", "Proc_Name cache size (abs)", "Proc_Name hits (per sec)", "Proc_Name misses (per sec)" };
		
		addTrendGraphData(GRAPH_NAME_JVM_MEM,          new TrendGraphDataPoint(GRAPH_NAME_JVM_MEM,          labels_jvmMem,         LabelType.Static));
		addTrendGraphData(GRAPH_NAME_JVM_MEM_PCT,      new TrendGraphDataPoint(GRAPH_NAME_JVM_MEM_PCT,      labels_jvmMemPct,      LabelType.Static));
		addTrendGraphData(GRAPH_NAME_LTL_CMD_SENT,     new TrendGraphDataPoint(GRAPH_NAME_LTL_CMD_SENT,     labels_ltlCmdSent,     LabelType.Static));
		addTrendGraphData(GRAPH_NAME_LTL_BYTES_SENT,   new TrendGraphDataPoint(GRAPH_NAME_LTL_BYTES_SENT,   labels_ltlBytesSent,   LabelType.Static));
		addTrendGraphData(GRAPH_NAME_LTL_KB_SENT,      new TrendGraphDataPoint(GRAPH_NAME_LTL_KB_SENT,      labels_ltlKbSent,      LabelType.Static));
		addTrendGraphData(GRAPH_NAME_LTL_MB_SENT,      new TrendGraphDataPoint(GRAPH_NAME_LTL_MB_SENT,      labels_ltlMbSent,      LabelType.Static));
		addTrendGraphData(GRAPH_NAME_LTI_QUEUE_SIZE,   new TrendGraphDataPoint(GRAPH_NAME_LTI_QUEUE_SIZE,   labels_ltiQueueSize,   LabelType.Static));
		addTrendGraphData(GRAPH_NAME_LR_OPERATIONS,    new TrendGraphDataPoint(GRAPH_NAME_LR_OPERATIONS,    labels_lrOperations,   LabelType.Static));
		addTrendGraphData(GRAPH_NAME_LR_TRANS,         new TrendGraphDataPoint(GRAPH_NAME_LR_TRANS,         labels_lrTrans,        LabelType.Static));
		addTrendGraphData(GRAPH_NAME_LR_MAINT_FILTER,  new TrendGraphDataPoint(GRAPH_NAME_LR_MAINT_FILTER,  labels_lrMaintFilter,  LabelType.Static));
		addTrendGraphData(GRAPH_NAME_GLOBAL_LRU_CACHE, new TrendGraphDataPoint(GRAPH_NAME_GLOBAL_LRU_CACHE, labels_globalLruCache, LabelType.Static));
		addTrendGraphData(GRAPH_NAME_ORA_LR,           new TrendGraphDataPoint(GRAPH_NAME_ORA_LR,           labels_oraLr,          LabelType.Static));
		addTrendGraphData(GRAPH_NAME_ORA_LOB_OP,       new TrendGraphDataPoint(GRAPH_NAME_ORA_LOB_OP,       labels_oraLobOp,       LabelType.Static));
		addTrendGraphData(GRAPH_NAME_ORA_RASD_CACHE,   new TrendGraphDataPoint(GRAPH_NAME_ORA_RASD_CACHE,   labels_oraRasdCache,   LabelType.Static));

		// if GUI
		if (getGuiController() != null && getGuiController().hasGUI())
		{
			TrendGraph tg = null;

			// GRAPH
			tg = new TrendGraph(GRAPH_NAME_JVM_MEM,
				"JVM Memory Usage",                   // Menu CheckBox text
				"JVM Memory Usage (Absolute Value)",  // Label 
				labels_jvmMem, 
				false, // is Percent Graph
				this, 
				true,  // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height
			addTrendGraph(tg.getName(), tg, true);
				
			// GRAPH
			tg = new TrendGraph(GRAPH_NAME_JVM_MEM_PCT,
				"JVM Memory Usage in Percent",                   // Menu CheckBox text
				"JVM Memory Usage in Percent (Absolute Value)",  // Label 
				labels_jvmMemPct, 
				true, // is Percent Graph
				this, 
				true,  // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height
			addTrendGraph(tg.getName(), tg, true);
				
			// GRAPH
			tg = new TrendGraph(GRAPH_NAME_LTL_CMD_SENT,
				"LTI: Number of LTL commands sent to RepServer",                   // Menu CheckBox text
				"LTI: Number of LTL commands sent to RepServer (per second)",      // Label 
				labels_ltlCmdSent, 
				false, // is Percent Graph
				this, 
				true,  // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height
			addTrendGraph(tg.getName(), tg, true);
				
			// GRAPH
			tg = new TrendGraph(GRAPH_NAME_LTL_BYTES_SENT,
				"LTI: Number of Bytes sent to RepServer",                   // Menu CheckBox text
				"LTI: Number of Bytes sent to RepServer (per second)",      // Label 
				labels_ltlBytesSent, 
				false, // is Percent Graph
				this, 
				false,  // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height
			addTrendGraph(tg.getName(), tg, true);
				
			// GRAPH
			tg = new TrendGraph(GRAPH_NAME_LTL_KB_SENT,
				"LTI: Number of KB sent to RepServer",                   // Menu CheckBox text
				"LTI: Number of KB sent to RepServer (per second)",      // Label 
				labels_ltlKbSent, 
				false, // is Percent Graph
				this, 
				true,  // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height
			addTrendGraph(tg.getName(), tg, true);
				
			// GRAPH
			tg = new TrendGraph(GRAPH_NAME_LTL_MB_SENT,
				"LTI: Number of MB sent to RepServer",                   // Menu CheckBox text
				"LTI: Number of MB sent to RepServer (per second)",      // Label 
				labels_ltlMbSent, 
				false, // is Percent Graph
				this, 
				false,  // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height
			addTrendGraph(tg.getName(), tg, true);
				
			// GRAPH
			tg = new TrendGraph(GRAPH_NAME_LTI_QUEUE_SIZE,
				"LTI: Current number of commands in the LTI queue",                       // Menu CheckBox text
				"LTI: Current number of commands in the LTI queue (Absolute Value)",      // Label 
				labels_ltiQueueSize, 
				false, // is Percent Graph
				this, 
				true,  // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height
			addTrendGraph(tg.getName(), tg, true);
				
			// GRAPH
			tg = new TrendGraph(GRAPH_NAME_LR_OPERATIONS,
				"LogReader: Operations",                   // Menu CheckBox text
				"LogReader: Operations (per second)",      // Label 
				labels_lrOperations, 
				false, // is Percent Graph
				this, 
				true,  // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height
			addTrendGraph(tg.getName(), tg, true);
				
			// GRAPH
			tg = new TrendGraph(GRAPH_NAME_LR_TRANS,
				"LogReader: Transactions",                   // Menu CheckBox text
				"LogReader: Transactions (per second)",      // Label 
				labels_lrTrans, 
				false, // is Percent Graph
				this, 
				true,  // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height
			addTrendGraph(tg.getName(), tg, true);
				
			// GRAPH
			tg = new TrendGraph(GRAPH_NAME_LR_MAINT_FILTER,
				"LogReader: Maintenance user operations read from log devices and skipped",                   // Menu CheckBox text
				"LogReader: Maintenance user operations read from log devices and skipped (per second)",      // Label 
				labels_lrMaintFilter, 
				false, // is Percent Graph
				this, 
				false,  // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height
			addTrendGraph(tg.getName(), tg, true);
				
			// GRAPH
			tg = new TrendGraph(GRAPH_NAME_GLOBAL_LRU_CACHE,
				"Number of Object in the Global LRU Cache",                   // Menu CheckBox text
				"Number of Object in the Global LRU Cache (Absolute Value)",  // Label 
				labels_globalLruCache, 
				false, // is Percent Graph
				this, 
				false,  // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height
			addTrendGraph(tg.getName(), tg, true);
				
			// GRAPH
			tg = new TrendGraph(GRAPH_NAME_ORA_LR,
				"Number of Log Records (Oracle specific)",                  // Menu CheckBox text
				"Number of Log Records (per second), Oracle specific",      // Label 
				labels_oraLr, 
				false, // is Percent Graph
				this, 
				false,  // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height
			addTrendGraph(tg.getName(), tg, true);
				
			// GRAPH
			tg = new TrendGraph(GRAPH_NAME_ORA_LOB_OP,
				"Number of LOB Operations from PDB (Oracle specific)",                  // Menu CheckBox text
				"Number of LOB Operations from PDB (per second), Oracle specific",      // Label 
				labels_oraLobOp, 
				false, // is Percent Graph
				this, 
				false,  // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height
			addTrendGraph(tg.getName(), tg, true);
				
			// GRAPH
			tg = new TrendGraph(GRAPH_NAME_ORA_RASD_CACHE,
				"RepAgent System Database Repository Cache (Oracle specific)",                  // Menu CheckBox text
				"RepAgent System Database Repository Cache (per second), Oracle specific",      // Label 
				labels_oraRasdCache, 
				false, // is Percent Graph
				this, 
				false,  // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height
			addTrendGraph(tg.getName(), tg, true);
		}
	}
	
	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{	
		if (GRAPH_NAME_JVM_MEM.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[4];
			arr[0] = this.getAbsValueAsDouble("VM maximum memory - MB" ,        "NumberValue");
			arr[1] = this.getAbsValueAsDouble("VM total memory allocated - MB", "NumberValue");
			arr[2] = this.getAbsValueAsDouble("VM free memory - MB",            "NumberValue");
			arr[3] = this.getAbsValueAsDouble("VM memory usage - MB",           "NumberValue");
			
			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		if (GRAPH_NAME_JVM_MEM_PCT.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[1];
			arr[0] = this.getAbsValueAsDouble("VM % max memory used" , "NumberValue");
			
			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		if (GRAPH_NAME_LTL_CMD_SENT.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[1];
			arr[0] = this.getRateValueAsDouble("Number of LTL commands sent" , "NumberValue");
			
			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		if (GRAPH_NAME_LTL_BYTES_SENT.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[1];
			arr[0] = this.getRateValueAsDouble("Total bytes sent" , "NumberValue");
			
			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		if (GRAPH_NAME_LTL_KB_SENT.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[1];
			arr[0] = this.getRateValueAsDouble("Total bytes sent - KB" , "NumberValue");
			
			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		if (GRAPH_NAME_LTL_MB_SENT.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[1];
			arr[0] = this.getRateValueAsDouble("Total bytes sent - MB" , "NumberValue");
			
			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		if (GRAPH_NAME_LTI_QUEUE_SIZE.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[1];
			arr[0] = this.getAbsValueAsDouble("Current number of commands in the LTI queue" , "NumberValue");
			
			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		if (GRAPH_NAME_LR_OPERATIONS.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[3];
			arr[0] = this.getRateValueAsDouble("Total operations scanned",   "NumberValue");
			arr[1] = this.getRateValueAsDouble("Total operations processed", "NumberValue");
			arr[2] = this.getRateValueAsDouble("Total operations skipped",   "NumberValue");
			
			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		if (GRAPH_NAME_LR_TRANS.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[7];
			arr[0] = this.getRateValueAsDouble("Total transactions processed",             "NumberValue");
			arr[1] = this.getRateValueAsDouble("Total transactions skipped",               "NumberValue");
			arr[2] = this.getRateValueAsDouble("Total transactions opened",                "NumberValue");
			arr[3] = this.getRateValueAsDouble("Total transactions closed",                "NumberValue");
			arr[4] = this.getRateValueAsDouble("Total transactions committed",             "NumberValue");
			arr[5] = this.getRateValueAsDouble("Total transactions aborted (rolled back)", "NumberValue");
			arr[6] = this.getRateValueAsDouble("Total system transactions skipped",        "NumberValue");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		if (GRAPH_NAME_LR_MAINT_FILTER.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[1];
			arr[0] = this.getRateValueAsDouble("Total maintenance user operations filtered", "NumberValue");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		if (GRAPH_NAME_GLOBAL_LRU_CACHE.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[1];
			arr[0] = this.getAbsValueAsDouble("Items held in Global LRUCache", "NumberValue");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		if (GRAPH_NAME_ORA_LR.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[2];
			arr[0] = this.getRateValueAsDouble("Total log records queued",   "NumberValue");
			arr[1] = this.getRateValueAsDouble("Total log records filtered", "NumberValue");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		if (GRAPH_NAME_ORA_LOB_OP.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[1];
			arr[0] = this.getAbsValueAsDouble("Total LOB operations processed by query data from PDB", "NumberValue");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		if (GRAPH_NAME_ORA_RASD_CACHE.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[6];
			arr[0] = this.getAbsValueAsDouble ("Current Op Proc RASD marked object cache size",                "NumberValue"); // RASD = RepAgent System Database
			arr[1] = this.getRateValueAsDouble("Total number of Op Proc RASD marked object cache hits",        "NumberValue");
			arr[2] = this.getRateValueAsDouble("Total number of Op Proc RASD marked object cache misses",      "NumberValue");

			arr[3] = this.getAbsValueAsDouble ("Current Op Proc_Name RASD marked object cache size",           "NumberValue"); // RASD = RepAgent System Database
			arr[4] = this.getRateValueAsDouble("Total number of Op Proc_Name RASD marked object cache hits",   "NumberValue");
			arr[5] = this.getRateValueAsDouble("Total number of Op Proc_Name RASD marked object cache misses", "NumberValue");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}
	}
	
//	@Override
//	protected TabularCntrPanel createGui()
//	{
//		return new CmRaStatisticsPanel(this);
//	}


	
//	1> ra_statistics
//	+---------+------------------------------------------------------+------------------------------+
//	|Component|Statistic                                             |Value                         |
//	+---------+------------------------------------------------------+------------------------------+
//	|LTM      |Time statistics obtained                              |Thu Apr 09 01:07:03 CEST 2015 |
//	|LTM      |Time replication last started                         |Replication not started       |
//	|LTM      |Time statistics last reset                            |Statistics have not been reset|
//	|LTM      |Items held in Global LRUCache                         |1                             |
//	|VM       |VM maximum memory                                     |954466304                     |
//	|VM       |VM total memory allocated                             |48758784                      |
//	|VM       |VM free memory                                        |35978872                      |
//	|VM       |VM memory usage                                       |12779912                      |
//	|VM       |VM % max memory used                                  |1.34                          |
//	|LR       |Current operation queue size                          |0                             |
//	|LR       |Log reposition point locator                          |null                          |
//	|LR       |Last processed operation locator                      |null                          |
//	|LR       |Avg xlog operation wait time (ms)                     |0.0                           |
//	|LR       |Avg sender operation processing time (ms)             |0.0                           |
//	|LR       |Avg sender operation wait time (ms)                   |0.0                           |
//	|LR       |Avg change set send time (ms)                         |0.0                           |
//	|LR       |Number of sender operations processed                 |0                             |
//	|LR       |Current marked objects cache size                     |0                             |
//	|LTI      |Number of LTL commands sent                           |0.0                           |
//	|LTI      |Avg LTL command size                                  |0                             |
//	|LTI      |Avg LTL commands/sec                                  |0                             |
//	|LTI      |Total bytes sent                                      |0.0                           |
//	|LTI      |Avg Bytes/second during transmission                  |0.0                           |
//	|LTI      |Avg LTL buffer cache time                             |0.0                           |
//	|LTI      |Avg Rep Server turnaround time                        |0.0                           |
//	|LTI      |Avg time to create distributes                        |0.0                           |
//	|LTI      |Avg LTL buffer size                                   |0                             |
//	|LTI      |Avg LTM buffer utilization (%)                        |0.0                           |
//	|LTI      |Avg LTL commands/buffer                               |0                             |
//	|LTI      |Encoded column name cache size                        |5000.0                        |
//	|LTI      |Current number of commands in the LTI queue           |0.0                           |
//	|LTI      |Current number of unformatted command in the LTI queue|0.0                           |
//	|LTI      |Last QID sent                                         |None                          |
//	|LTI      |Last transaction id sent                              |None                          |
//	+---------+------------------------------------------------------+------------------------------+
//	Rows 34
//	(34 rows affected)

	@Override
	protected CmSybMessageHandler createSybMessageHandler()
	{
		CmSybMessageHandler msgHandler = super.createSybMessageHandler();

		// Msg 0, Level 20, State 0:
		// Server 'GORAN_1_RAX', Procedure 'ra_statistics reset', Line 1 (Called from script row 2631), Status 0, TranState 0:
		// successful
		msgHandler.addDiscardMsgNum(0);

		return msgHandler;
	}


	@Override
	public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		return NEED_CONFIG;
	}

	@Override
	public void addMonTableDictForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addTable("ra_statistics",  "");

			mtd.addColumn("ra_statistics", "Component",     "<html>What component in the RepAgent</html>");
			mtd.addColumn("ra_statistics", "Statistic",     "<html>Statistic Name</html>");
			mtd.addColumn("ra_statistics", "Value",         "<html>Value of the statistical counter</html>");
			mtd.addColumn("ra_statistics", "NumberValue",   "<html>Value of the statistical counter, but converted to a Number (BigDecimal).</html>");
			mtd.addColumn("ra_statistics", "Description",   "<html>Short description, as it was found in the manual.</html>");
		}
		catch (NameNotFoundException e) {/*ignore*/}
	}

	@Override
	public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();

//		pkCols.add("Component"); // Note if we change this also change: _xrstm.setPkCol at the end of this file
		pkCols.add("Statistic");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		boolean sample_resetAfter = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_sample_resetAfter, DEFAULT_sample_resetAfter);

		String sql = "ra_statistics \n";

		if (sample_resetAfter)
			sql += "go\nra_statistics reset \n";

		return sql;
	}
	@Override
	public void localCalculation(CounterSample newSample)
	{
		int  Component_pos = -1;
		int  Statistic_pos = -1;
		int  Value_pos     = -1;

		// Set the "new" column layout
		newSample.setColumnNames (_xrstm.getColumnNames());
		newSample.setSqlType     (_xrstm.getSqlTypes());
		newSample.setSqlTypeNames(_xrstm.getSqlTypeNames());
		newSample.setColClassName(_xrstm.getClassNames());

		// Resize the array of PK, because we will have added 2 columns
		newSample.setPkColArray(_xrstm.getPkColArray());
//		newSample.initPkStructures();

//		if ( ! hasResultSetMetaData() )
		try{ setResultSetMetaData(_xrstm); }
		catch(SQLException ex) { _logger.error("Problems setting setResultSetMetaData(), this is extreamly bisar if it happends.", ex); }

		// Find column Id's
		List<String> colNames = newSample.getColNames();
		if (colNames == null)
			return;
		for (int colId = 0; colId < colNames.size(); colId++)
		{
			String colName = (String) colNames.get(colId);
			if      (colName.equals("Component")) Component_pos = colId;
			else if (colName.equals("Statistic")) Statistic_pos = colId;
			else if (colName.equals("Value"))     Value_pos     = colId;
		}

		if (Component_pos == -1 || Statistic_pos == -1 || Value_pos == -1)
			return;

		final BigDecimal KB = new BigDecimal(1024);
		final BigDecimal MB = new BigDecimal(1024 * 1024);
        
		// Loop on all newSample rows, parse some of them, then add new records for the one parsed
		int rowc = newSample.getRowCount();
		for (int rowId = 0; rowId < rowc; rowId++)
		{
			String component  = (String) newSample.getValueAt(rowId, Component_pos);
			String statistics = (String) newSample.getValueAt(rowId, Statistic_pos);
			String value      = (String) newSample.getValueAt(rowId, Value_pos);

//			Long numberValue = null;
//			try	{ numberValue = new Long(value); }
//			catch(NumberFormatException nfe) {System.out.println("problems converting row="+rowId+", value='"+value+"'.");}

			// Try to convert to numbers
			BigDecimal numberValue = null;
			try	{ numberValue = new BigDecimal(value); }
			catch(NumberFormatException nfe) { /*ignore*/ }
//			catch(NumberFormatException nfe) { System.out.println("problems converting row="+rowId+", value='"+value+"'."); }

			String desc = RaxCounterDict.getDesc(statistics);

			newSample.setValueAt(numberValue, rowId, 3);
			newSample.setValueAt(desc,        rowId, 4);
			
			// Transform java memory into MB and add a new entry for it
			try
			{
//				1> ra_statistics
//				+---------+------------------------------------------------------+-----------------------------+
//				|Component|Statistic                                             |Value                        |
//				+---------+------------------------------------------------------+-----------------------------+
//				|VM       |VM maximum memory                                     |954466304                    |
//				|VM       |VM total memory allocated                             |119537664                    |
//				|VM       |VM free memory                                        |46779088                     |
//				|VM       |VM memory usage                                       |72758576                     |
//				|VM       |VM % max memory used                                  |7.62                         |
//				+---------+------------------------------------------------------+-----------------------------+

				if ("VM maximum memory".equals(statistics))
				{
					String newStatName = statistics+" - MB";
					BigDecimal newVal = (numberValue == null ? null : numberValue.divide(MB, 1, BigDecimal.ROUND_UP)); 
					addRow(newSample, component, newStatName, newVal+"", newVal, RaxCounterDict.getDesc(newStatName));
				}
				else if ("VM total memory allocated".equals(statistics))
				{
					String newStatName = statistics+" - MB";
					BigDecimal newVal = (numberValue == null ? null : numberValue.divide(MB, 1, BigDecimal.ROUND_UP)); 
					addRow(newSample, component, newStatName, newVal+"", newVal, RaxCounterDict.getDesc(newStatName));
				}
				else if ("VM free memory".equals(statistics))
				{
					String newStatName = statistics+" - MB";
					BigDecimal newVal = (numberValue == null ? null : numberValue.divide(MB, 1, BigDecimal.ROUND_UP)); 
					addRow(newSample, component, newStatName, newVal+"", newVal, RaxCounterDict.getDesc(newStatName));
				}
				else if ("VM memory usage".equals(statistics))
				{
					String newStatName = statistics+" - MB";
					BigDecimal newVal = (numberValue == null ? null : numberValue.divide(MB, 1, BigDecimal.ROUND_UP)); 
					addRow(newSample, component, newStatName, newVal+"", newVal, RaxCounterDict.getDesc(newStatName));
				}
				else if ("Total bytes sent".equals(statistics))
				{
					String newStatName = statistics+" - KB";
					BigDecimal newVal = (numberValue == null ? null : numberValue.divide(KB, 1, BigDecimal.ROUND_UP)); 
					addRow(newSample, component, newStatName, newVal+"", newVal, RaxCounterDict.getDesc(newStatName));

					newStatName = statistics+" - MB";
					newVal = (numberValue == null ? null : numberValue.divide(MB, 1, BigDecimal.ROUND_UP)); 
					addRow(newSample, component, newStatName, newVal+"", newVal, RaxCounterDict.getDesc(newStatName));
				}
				else if (    "Time statistics obtained"     .equals(statistics) 
				          || "Time replication last started".equals(statistics) 
				          || "Time statistics last reset"   .equals(statistics) 
				        )
				{
					try 
					{
						// Try to parse the 'Wed Apr 15 15:59:57 CEST 2015' into a "normal SQL Timestamp" 
						SimpleDateFormat parserSDF = new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy", Locale.ENGLISH);
						Timestamp ts = new Timestamp( parserSDF.parse(value).getTime() );
						newSample.setValueAt(ts+"", rowId, Value_pos);
						
						// set Counter Clear Time
						// Note: if "clear after sample" is ON, we still wont see the "YELLOW" color on the field, this since because the time 
						//       resolution at 'Time statistics last reset' is at second level, while the sample time is at millisecond level
						if ("Time statistics last reset".equals(statistics))
							setCounterClearTime(ts);  
					}
					catch (ParseException pe) { /* if it fails just keep the current value*/ }
				}

			}
			catch(Exception ex)
			{
			}
		}
//System.out.println("AFTER: startRowc="+rowc+", endRowc="+newSample.getRowCount());
	}
	private void addRow(CounterSample newSample, String compKey, String statKey, String strValue, BigDecimal numValue, String desc)
	{
		List<Object> row = new ArrayList<Object>(5);
		row.add(compKey);
		row.add(statKey);
		row.add(strValue);
		row.add(numValue);
		row.add(desc);
		newSample.addRow(row);
//		System.out.println("addRow(): compKey='"+compKey+"', statKey='"+statKey+"', strValue='"+strValue+"', numValue='"+numValue+"', desc='"+desc+"'.");
	}

	
	
	
	
	
	
	private static DbxTuneResultSetMetaData _xrstm = new DbxTuneResultSetMetaData();

	static
	{
		_xrstm.addStrColumn       ("Component",      1,  false, 255,   "FIXME: description");
		_xrstm.addStrColumn       ("Statistic",      2,  false, 100,    "FIXME: description");
		_xrstm.addStrColumn       ("Value",          3,  false, 100,    "FIXME: description");
//		_xrstm.addLongColumn      ("NumberValue",    4,  true,         "FIXME: description");
		_xrstm.addBigDecimalColumn("NumberValue",    4,  true,  20, 2, "FIXME: description");
		_xrstm.addStrColumn       ("Description",    5,  true,  255,   "FIXME: description");

//		_xrstm.setPkCol("Component", "Statistic");
		_xrstm.setPkCol("Statistic");
	}
}
