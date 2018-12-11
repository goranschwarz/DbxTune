package com.asetune.cm.iq;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.graph.TrendGraphDataPoint.LabelType;
import com.asetune.gui.MainFrame;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */

/**
 * sp_iqstatistics procedure
 * Returns serial number, name, description, value, and unit specifier for each available statistic, or a specified statistic. 
 * @author I063869
 *
 */
public class CmIqStatistics
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmAdminWhoSqm.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmIqStatistics.class.getSimpleName();
	public static final String   SHORT_NAME       = "statistics";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<h4>sp_iqstatistics</h4>" +
		"Returns serial number, name, description, value, and unit specifier for each available statistic, or a specified statistic." +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"sp_iqstatistics", "sp_iqstatistics_pivot"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {"stat_value"};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = 60; //CountersModel.DEFAULT_sqlQueryTimeout;

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

		return new CmIqStatistics(counterController, guiController);
	}

	public CmIqStatistics(ICounterController counterController, IGuiController guiController)
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
		setBackgroundDataPollingEnabled(true, false);
		
		setCounterController(counterController);
		setGuiController(guiController);
		
		addTrendGraphs();
		
		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	// NOTE: storage table name will be CmName_GraphName, so try to keep the name short
	public static final String GRAPH_NAME_STAT_OPER = "OperationsGraph"; 
	public static final String GRAPH_NAME_STAT_DISK = "DiskGraph"; 
	public static final String GRAPH_NAME_STAT_CPUS = "CPUGraph";
	
	
	private void addTrendGraphs()
	{
//		String[] labels_oper = new String[] { "[Connections Active]", "[Operations Waiting]", "[Operations Active]", "[Active Load Statements]" };
//		String[] labels_disk = new String[] { "[Main Store Disk Reads]",	"[Main Store Disk Writes]", "[Temp Store Disk Reads]", "[Temp Store Disk Writes]", "[Cache Dbspace Disk Reads]", "[Cache Dbspace Disk Writes]"} ;
//		String[] labels_cpus = new String[] { "[Cpu Total Time]", "[Cpu User Time]", "[Cpu System Time]"} ;
//				
//		addTrendGraphData(GRAPH_NAME_STAT_OPER, new TrendGraphDataPoint(GRAPH_NAME_STAT_OPER, labels_oper, LabelType.Static));
//		addTrendGraphData(GRAPH_NAME_STAT_DISK, new TrendGraphDataPoint(GRAPH_NAME_STAT_DISK, labels_disk, LabelType.Static));
//		addTrendGraphData(GRAPH_NAME_STAT_CPUS, new TrendGraphDataPoint(GRAPH_NAME_STAT_CPUS, labels_cpus, LabelType.Static));

		addTrendGraph(GRAPH_NAME_STAT_OPER,
			"Connections, Operations and Load", // Menu CheckBox text
			"Connections, Operations and Load ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			new String[] { "Connections Active", "Operations Waiting", "Operations Active", "Active Load Statements" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height
		
		addTrendGraph(GRAPH_NAME_STAT_DISK,
			"Disk activity", // Menu CheckBox text
			"Disk activity ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			new String[] { "Main Store Disk Reads",	"Main Store Disk Writes", "Temp Store Disk Reads", "Temp Store Disk Writes", "Cache Dbspace Disk Reads", "Cache Dbspace Disk Writes"}, 
			LabelType.Static,
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_STAT_CPUS,
			"CPU usage",                     // Menu CheckBox text
			"CPU usage (100 per core) ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			new String[] { "Cpu Total Time", "Cpu User Time", "Cpu System Time"}, 
			LabelType.Static,
			TrendGraphDataPoint.Category.CPU,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height
		
		
//		// if GUI
//		if (getGuiController() != null && getGuiController().hasGUI())
//		{
//			// GRAPH
//			TrendGraph tg = null;
//			tg = new TrendGraph(GRAPH_NAME_STAT_OPER,
//				"Connections, Operations and Load", // Menu CheckBox text
//				"Connections, Operations and Load ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
//				labels_oper, 
//				false, // is Percent Graph
//				this, 
//				false, // visible at start
//				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				-1);   // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//			
//			tg = new TrendGraph(GRAPH_NAME_STAT_DISK,
//					"Disk activity", // Menu CheckBox text
//					"Disk activity ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
//					labels_disk, 
//					false, // is Percent Graph
//					this, 
//					false, // visible at start
//					0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//					-1);   // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//
//			tg = new TrendGraph(GRAPH_NAME_STAT_CPUS,
//					"CPU usage",                     // Menu CheckBox text
//					"CPU usage (100 per core) ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
//					labels_disk, 
//					false, // is Percent Graph
//					this, 
//					false, // visible at start
//					0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//					-1);   // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//		}
	}
	
	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		if (GRAPH_NAME_STAT_OPER.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[4];

			arr[0] = this.getAbsValueAsDouble("ConnectionsActive" , "stat_value");
			arr[1] = this.getAbsValueAsDouble("OperationsWaiting",  "stat_value");
			arr[2] = this.getAbsValueAsDouble("OperationsActive",   "stat_value");
			arr[3] = this.getAbsValueAsDouble("OperationsActiveloadTableStatement", "stat_value");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
//			tgdp.setDate(this.getTimestamp());
//			tgdp.setData(arr);
		}
		
			
		if (GRAPH_NAME_STAT_DISK.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[6];

			arr[0] = this.getRateValueAsDouble("MainStoreDiskReads" ,    "stat_value");
			arr[1] = this.getRateValueAsDouble("MainStoreDiskWrites",    "stat_value");
			arr[2] = this.getRateValueAsDouble("TempStoreDiskReads",     "stat_value");
			arr[3] = this.getRateValueAsDouble("TempStoreDiskWrites",    "stat_value");
			arr[4] = this.getRateValueAsDouble("CacheDbspaceDiskReads",  "stat_value");
			arr[5] = this.getRateValueAsDouble("CacheDbspaceDiskWrites", "stat_value");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
//			tgdp.setDate(this.getTimestamp());
//			tgdp.setData(arr);
		}
		
		if (GRAPH_NAME_STAT_CPUS.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[3];
			
			Double CpuTotalTime   = getDiffValueAsDouble("CpuTotalTime",  "stat_value");//.doubleValue();
			Double CpuSystemTime  = getDiffValueAsDouble("CpuSystemTime", "stat_value");//.doubleValue();
			Double CpuUserTime    = getDiffValueAsDouble("CpuUserTime",   "stat_value");//.doubleValue();
			double interval  = getLastSampleInterval();
	
			if (CpuTotalTime != null && CpuSystemTime != null && CpuUserTime != null)
			{
				double msCPU       = CpuTotalTime .doubleValue() * 1000;
				double msCPUUser   = CpuUserTime  .doubleValue() * 1000;
				double msCPUSystem = CpuSystemTime.doubleValue() * 1000;
				
				BigDecimal pctCPU       = new BigDecimal( (msCPU       / interval) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
				BigDecimal pctUserCPU   = new BigDecimal( (msCPUUser   / interval) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
				BigDecimal pctSystemCPU = new BigDecimal( (msCPUSystem / interval) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
	
				arr[0] = pctCPU      .doubleValue();
				arr[1] = pctSystemCPU.doubleValue();
				arr[2] = pctUserCPU  .doubleValue();
				//_logger.debug("updateGraphData("+tgdp.getName()+"): pctCPU='"+arr[0]+"', pctSystemCPU='"+arr[1]+"', pctUserCPU='"+arr[2]+"'.");
	
			}
			else
			{
				arr[0] = 0.0;
				arr[1] = 0.0;
				arr[2] = 0.0;
				//_logger.debug("updateGraphData("+tgdp.getName()+"): some-value-was-null... CpuTotalTime='"+CpuTotalTime+"', CpuSystemTime='"+CpuSystemTime+"', CpuUserTime='"+CpuUserTime+"'. Adding a 0 pct CPU Usage.");
			}
			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
//			tgdp.setDate(this.getTimestamp());
//			tgdp.setData(arr);
		}		
	}
	
	@Override
	public String[] getDependsOnConfigForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		return NEED_CONFIG;
	}

	@Override
	public void addMonTableDictForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addTable("sp_iqstatistics",  "Returns serial number, name, description, value, and unit specifier for each available statistic, or a specified statistic.");

			mtd.addColumn("sp_iqstatistics", "stat_num",  "<html>Serial number of a statistic</html>");
			mtd.addColumn("sp_iqstatistics", "stat_name",  "<html>Name of statistic</html>");
			mtd.addColumn("sp_iqstatistics", "stat_value",  "<html>Value of statistic</html>");
			mtd.addColumn("sp_iqstatistics", "stat_unit",  "<html>Unit specifier</html>");
			mtd.addColumn("sp_iqstatistics", "stat_desc",  "<html>Description of statistic</html>");
		}
		catch (NameNotFoundException e) {/*ignore*/}
	}

	@Override
	public List<String> getPkForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("stat_name");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		String sql = 
			"select \n" + 
			"    stat_num,  \n" +
			"    stat_name,  \n" +
			"    stat_value = CASE \n" +
			"                   WHEN IsNumeric(stat_value) = 1 THEN convert(numeric(20,5), stat_value) \n" +
			"                   ELSE null  \n" +
			"                 END, \n" +
			"    stat_unit, \n" +
			"    stat_desc \n" +
			"from sp_iqstatistics() \n" + 
			"where IsNumeric(stat_value) = 1 \n";

		return sql;
	}
}
