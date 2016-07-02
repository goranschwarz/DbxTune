package com.asetune.cm.sqlserver;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import com.asetune.CounterControllerAse;
import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.sqlserver.gui.CmDeviceIoPanel;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.gui.TrendGraph;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmDeviceIo
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmDeviceIo.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmDeviceIo.class.getSimpleName();
	public static final String   SHORT_NAME       = "Devices";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<p>What devices are doing IO's and what's the approximare service time on the disk.</p>" +
		"Do not trust the service time <b>too</b> much...<br>" +
		"<br>" +
		CounterControllerAse.TRANLOG_DISK_IO_TOOLTIP +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_DISK;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final int      NEED_SRV_VERSION = 0;
	public static final int      NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"dm_io_virtual_file_stats", "master_files"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {"ReadsPct", "WritesPct"};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"TotalIOs", 
		"Reads", 
		"Writes", 
		"IOTime", 
		"ReadTime", 
		"WriteTime",
		"ReadsKB",
		"WritesKB"};

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

		return new CmDeviceIo(counterController, guiController);
	}

	public CmDeviceIo(ICounterController counterController, IGuiController guiController)
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
	public static final String GRAPH_NAME_RW_SERVICE_TIME = "SvcTimeRW";
	public static final String GRAPH_NAME_R_SERVICE_TIME  = "SvcTimeR";
	public static final String GRAPH_NAME_W_SERVICE_TIME  = "SvcTimeW";

	public static final String GRAPH_NAME_RW_DISK_IO      = "IoRW";
	public static final String GRAPH_NAME_R_DISK_IO       = "IoR";
	public static final String GRAPH_NAME_W_DISK_IO       = "IoW";

	private void addTrendGraphs()
	{
		String[] labels = new String[] { "-added-at-runtime-" };
		
		addTrendGraphData(GRAPH_NAME_RW_DISK_IO,      new TrendGraphDataPoint(GRAPH_NAME_RW_DISK_IO,      labels));
		addTrendGraphData(GRAPH_NAME_R_DISK_IO,       new TrendGraphDataPoint(GRAPH_NAME_R_DISK_IO,       labels));
		addTrendGraphData(GRAPH_NAME_W_DISK_IO,       new TrendGraphDataPoint(GRAPH_NAME_W_DISK_IO,       labels));

		addTrendGraphData(GRAPH_NAME_RW_SERVICE_TIME, new TrendGraphDataPoint(GRAPH_NAME_RW_SERVICE_TIME, labels));
		addTrendGraphData(GRAPH_NAME_R_SERVICE_TIME,  new TrendGraphDataPoint(GRAPH_NAME_R_SERVICE_TIME,  labels));
		addTrendGraphData(GRAPH_NAME_W_SERVICE_TIME,  new TrendGraphDataPoint(GRAPH_NAME_W_SERVICE_TIME,  labels));

		// if GUI
		if (getGuiController() != null && getGuiController().hasGUI())
		{
			// GRAPH
			TrendGraph tg = null;

			//-----
			tg = new TrendGraph(GRAPH_NAME_RW_DISK_IO,
				"Number of Disk Operations (Read+Write), per Second and Device", // Menu CheckBox text
				"Number of Disk Operations (Read+Write), per Second and Device", // Label 
				labels, 
				false, // is Percent Graph
				this, 
				true,  // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height
			addTrendGraph(tg.getName(), tg, true);

			//-----
			tg = new TrendGraph(GRAPH_NAME_R_DISK_IO,
				"Number of Disk Operations (Read), per Second and Device", // Menu CheckBox text
				"Number of Disk Operations (Read), per Second and Device", // Label 
				labels, 
				false, // is Percent Graph
				this, 
				false, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height
			addTrendGraph(tg.getName(), tg, true);

			//-----
			tg = new TrendGraph(GRAPH_NAME_W_DISK_IO,
				"Number of Disk Operations (Write), per Second and Device", // Menu CheckBox text
				"Number of Disk Operations (Write), per Second and Device", // Label 
				labels, 
				false, // is Percent Graph
				this, 
				false, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height
			addTrendGraph(tg.getName(), tg, true);



			//-----
			tg = new TrendGraph(GRAPH_NAME_RW_SERVICE_TIME,
				"Device IO Service Time (Read+Write), per Device",                 // Menu CheckBox text
				"Device IO Service Time (Read+Write) in Milliseconds, per Device", // Label 
				labels, 
				false, // is Percent Graph
				this, 
				true,  // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height
			addTrendGraph(tg.getName(), tg, true);

			//-----
			tg = new TrendGraph(GRAPH_NAME_R_SERVICE_TIME,
				"Device IO Service Time (Read), per Device",                 // Menu CheckBox text
				"Device IO Service Time (Read) in Milliseconds, per Device", // Label 
				labels, 
				false, // is Percent Graph
				this, 
				false, // visible at start
//				15702, // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				1570020, // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				0, // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height
			addTrendGraph(tg.getName(), tg, true);

			//-----
			tg = new TrendGraph(GRAPH_NAME_W_SERVICE_TIME,
				"Device IO Service Time (Write), per Device",                 // Menu CheckBox text
				"Device IO Service Time (Write) in Milliseconds, per Device", // Label 
				labels, 
				false, // is Percent Graph
				this, 
				false, // visible at start
				0, // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height
			addTrendGraph(tg.getName(), tg, true);
		}
	}

	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmDeviceIoPanel(this);
	}

	@Override
	public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		return NEED_CONFIG;
	}

	@Override
	public void addMonTableDictForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addTable("deviceIo",  "");

			mtd.addColumn("deviceIo",  "TotalIOs",     "<html>" +
			                                                   "Total number of IO's issued on this device.<br>" +
			                                                   "<b>Formula</b>: Reads + Writes<br>" +
			                                              "</html>");
//			mtd.addColumn("deviceIo",  "APFReadsPct",  "<html>" +
//			                                                   "Of all the issued Reads, what's the Asynch Prefetch Reads percentage.<br>" +
//			                                                   "<b>Formula</b>: APFReads / Reads * 100<br>" +
//			                                              "</html>");
			mtd.addColumn("deviceIo",  "WritesPct",    "<html>" +
			                                                   "Of all the issued IO's, what's the Write percentage.<br>" +
			                                                   "<b>Formula</b>: Writes / (Reads + Writes) * 100<br>" +
			                                              "</html>");
			mtd.addColumn("deviceIo",  "AvgServ_ms",   "<html>" +
			                                                   "Service time on the disk.<br>" +
			                                                   "This is basically the average time it took to make a disk IO on this device.<br>" +
			                                                   "Warning: ASE isn't timing each IO individually, Instead it uses the 'click ticks' to do it... This might change in the future.<br>" +
			                                                   "<b>Formula</b>: IOTime / (Reads + Writes) <br>" +
			                                              "</html>");
			mtd.addColumn("deviceIo",  "ReadServiceTimeMs", "<html>" +
			                                                   "Service time on the disk for <b>Read</b> operations.<br>" +
			                                                   "This is basically the average time it took to make a <b>read</b> IO on this device.<br>" +
			                                                   "Warning: ASE isn't timing each IO individually, Instead it uses the 'click ticks' to do it... This might change in the future.<br>" +
			                                                   "<b>Formula</b>: ReadTime / Reads <br>" +
			                                              "</html>");
			mtd.addColumn("deviceIo",  "WriteServiceTimeMs", "<html>" +
			                                                   "Service time on the disk for <b>Write</b> operations.<br>" +
			                                                   "This is basically the average time it took to make a <b>write</b> IO on this device.<br>" +
			                                                   "Warning: ASE isn't timing each IO individually, Instead it uses the 'click ticks' to do it... This might change in the future.<br>" +
			                                                   "<b>Formula</b>: WriteTime / Writes <br>" +
			                                              "</html>");
		}
		catch (NameNotFoundException e) {/*ignore*/}
	}

	@Override
	public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("database_id");
		pkCols.add("file_id");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		String cols;

		String TotalIOs = "(a.num_of_reads + a.num_of_writes)";

		cols = "dbname             = db_name(a.database_id), \n" +
		       "type               = CASE \n" +
		       "                         WHEN a.file_id = 2 THEN 'Log' \n" +
		       "                         ELSE 'Data' \n" +
		       "                     END, \n" +
		       "LogicalName        = b.name, \n" +
		       "TotalIOs           = "+TotalIOs+", \n" +
		       "Reads              = a.num_of_reads, \n" +
		       "ReadsKB            = (a.num_of_bytes_read / 1024), \n" +
		       "ReadsPct           = CASE WHEN "+TotalIOs+" > 0 \n" +
		       "                          THEN convert(numeric(10,1), (a.num_of_reads + 0.0) / ("+TotalIOs+" + 0.0) * 100.0 ) \n" +
		       "                          ELSE convert(numeric(10,1), 0.0 ) \n" +
		       "                     END, \n" +
		       "Writes             = a.num_of_writes, \n" +
		       "WritesKB           = (a.num_of_bytes_written / 1024), \n" +
		       "WritesPct          = CASE WHEN "+TotalIOs+" > 0 \n" +
		       "                          THEN convert(numeric(10,1), (a.num_of_writes + 0.0) / ("+TotalIOs+" + 0.0) * 100.0 ) \n" +
		       "                          ELSE convert(numeric(10,1), 0.0 ) \n" +
		       "                     END, \n" +
		       "IOTime             = a.io_stall, \n" + 
		       "ReadTime           = a.io_stall_read_ms, \n" + 
		       "WriteTime          = a.io_stall_write_ms, \n" + 
		       "AvgServ_ms         = CASE WHEN "+TotalIOs+" > 0 \n" +
		       "                          THEN convert(numeric(10,1), a.io_stall / convert(numeric(10,0), "+TotalIOs+")) \n" +
		       "                          ELSE convert(numeric(10,1), null) \n" +
		       "                     END, \n" +
		       "ReadServiceTimeMs  = CASE WHEN a.num_of_reads > 0 \n" +
		       "                          THEN convert(numeric(10,1), (a.io_stall_read_ms + 0.0) / (a.num_of_reads + 0.0) ) \n" +
		       "                          ELSE convert(numeric(10,1), null) \n" +
		       "                     END, \n" +
		       "WriteServiceTimeMs = CASE WHEN a.num_of_writes > 0 \n" +
		       "                          THEN convert(numeric(10,1), (a.io_stall_write_ms + 0.0) / (a.num_of_writes + 0.0) ) \n" +
		       "                          ELSE convert(numeric(10,1), null) \n" +
		       "                     END, \n" +
		       "SizeOnDiskMB       = ( ( a.size_on_disk_bytes / 1024 ) / 1024.0 ), \n" +
		       "PhysicalName = b.physical_name, \n" +
		       "a.database_id, \n" +
		       "a.file_id \n";

		String sql = 
			"select " + cols +
			"FROM sys.dm_io_virtual_file_stats (NULL, NULL) a \n" +
			"JOIN sys.master_files b ON a.file_id = b.file_id AND a.database_id = b.database_id \n" +
//			"ORDER BY a.io_stall DESC \n";
			"ORDER BY a.database_id, a.file_id \n";

		return sql;
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
//		int srvVersion = getServerVersion();

		// ---- DISK IO's PER SECOND GRAPHS
		if (GRAPH_NAME_RW_DISK_IO.equals(tgdp.getName()))
		{
			// Write 1 "line" for every device
			Double[] dArray = new Double[this.size()];
			String[] lArray = new String[dArray.length];
			for (int i = 0; i < dArray.length; i++)
			{
				lArray[i] = this.getRateString       (i, "LogicalName");
				dArray[i] = this.getRateValueAsDouble(i, "TotalIOs");
			}

			// Set the values
			tgdp.setDate(this.getTimestamp());
			tgdp.setLabel(lArray);
			tgdp.setData(dArray);
		}

		if (GRAPH_NAME_R_DISK_IO.equals(tgdp.getName()))
		{
			// Write 1 "line" for every device
			Double[] dArray = new Double[this.size()];
			String[] lArray = new String[dArray.length];
			for (int i = 0; i < dArray.length; i++)
			{
				lArray[i] = this.getRateString       (i, "LogicalName");
				dArray[i] = this.getRateValueAsDouble(i, "Reads");
			}

			// Set the values
			tgdp.setDate(this.getTimestamp());
			tgdp.setLabel(lArray);
			tgdp.setData(dArray);
		}

		if (GRAPH_NAME_W_DISK_IO.equals(tgdp.getName()))
		{
			// Write 1 "line" for every device
			Double[] dArray = new Double[this.size()];
			String[] lArray = new String[dArray.length];
			for (int i = 0; i < dArray.length; i++)
			{
				lArray[i] = this.getRateString       (i, "LogicalName");
				dArray[i] = this.getRateValueAsDouble(i, "Writes");
			}

			// Set the values
			tgdp.setDate(this.getTimestamp());
			tgdp.setLabel(lArray);
			tgdp.setData(dArray);
		}

		
		// ---- SERVICE TIME GRAPHS
		if (GRAPH_NAME_RW_SERVICE_TIME.equals(tgdp.getName()))
		{
			// Write 1 "line" for every device
			Double[] dArray = new Double[this.size()];
			String[] lArray = new String[dArray.length];
			for (int i = 0; i < dArray.length; i++)
			{
				lArray[i] = this.getRateString       (i, "LogicalName");
				dArray[i] = this.getRateValueAsDouble(i, "AvgServ_ms");
			}

			// Set the values
			tgdp.setDate(this.getTimestamp());
			tgdp.setLabel(lArray);
			tgdp.setData(dArray);
		}

		if (GRAPH_NAME_R_SERVICE_TIME.equals(tgdp.getName()))
		{
			// Write 1 "line" for every device
			Double[] dArray = new Double[this.size()];
			String[] lArray = new String[dArray.length];
			for (int i = 0; i < dArray.length; i++)
			{
				lArray[i] = this.getRateString       (i, "LogicalName");
				dArray[i] = this.getRateValueAsDouble(i, "ReadServiceTimeMs");
			}

			// Set the values
			tgdp.setDate(this.getTimestamp());
			tgdp.setLabel(lArray);
			tgdp.setData(dArray);
		}

		if (GRAPH_NAME_W_SERVICE_TIME.equals(tgdp.getName()))
		{
			// Write 1 "line" for every device
			Double[] dArray = new Double[this.size()];
			String[] lArray = new String[dArray.length];
			for (int i = 0; i < dArray.length; i++)
			{
				lArray[i] = this.getRateString       (i, "LogicalName");
				dArray[i] = this.getRateValueAsDouble(i, "WriteServiceTimeMs");
			}

			// Set the values
			tgdp.setDate(this.getTimestamp());
			tgdp.setLabel(lArray);
			tgdp.setData(dArray);
		}
	}

	/** 
	 * Compute the avgServ column, which is IOTime/(Reads+Writes)
	 */
	@Override
	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
	{
		int AvgServ_msId=-1, ReadsPctId=-1, WritesPctId=-1;

		int Reads,      Writes,      IOTime;
		int ReadsId=-1, WritesId=-1, IOTimeId=-1;
		
		int ReadTime     = -1, WriteTime     = -1;
		int pos_ReadTime = -1, pos_WriteTime = -1, pos_ReadServiceTimeMs = -1, pos_WriteServiceTimeMs = -1;

		// Find column Id's
		List<String> colNames = diffData.getColNames();
		if (colNames == null)
			return;
		for (int colId = 0; colId < colNames.size(); colId++)
		{
			String colName = (String) colNames.get(colId);
			if      (colName.equals("Reads"))       ReadsId       = colId;
//			else if (colName.equals("APFReads"))    APFReadsId    = colId;
			else if (colName.equals("Writes"))      WritesId      = colId;
			else if (colName.equals("IOTime"))      IOTimeId      = colId;

			else if (colName.equals("ReadsPct"))    ReadsPctId    = colId;
//			else if (colName.equals("APFReadsPct")) APFReadsPctId = colId;
			else if (colName.equals("WritesPct"))   WritesPctId   = colId;
			else if (colName.equals("AvgServ_ms"))  AvgServ_msId  = colId;

			else if (colName.equals("ReadTime"))           pos_ReadTime           = colId;
			else if (colName.equals("WriteTime"))          pos_WriteTime          = colId;
			else if (colName.equals("ReadServiceTimeMs"))  pos_ReadServiceTimeMs  = colId;
			else if (colName.equals("WriteServiceTimeMs")) pos_WriteServiceTimeMs = colId;
		}

		// Loop on all diffData rows
		for (int rowId = 0; rowId < diffData.getRowCount(); rowId++)
		{
			Reads    = ((Number) diffData.getValueAt(rowId, ReadsId))   .intValue();
//			APFReads = ((Number) diffData.getValueAt(rowId, APFReadsId)).intValue();
			Writes   = ((Number) diffData.getValueAt(rowId, WritesId))  .intValue();
			IOTime   = ((Number) diffData.getValueAt(rowId, IOTimeId))  .intValue();
			if (pos_ReadTime  >= 0) ReadTime  = ((Number) diffData.getValueAt(rowId, pos_ReadTime)).intValue();
			if (pos_WriteTime >= 0) WriteTime = ((Number) diffData.getValueAt(rowId, pos_WriteTime)).intValue();

			//--------------------
			//---- AvgServ_ms
			int totIo = Reads + Writes;
			if (totIo != 0)
			{
				// AvgServ_ms = (IOTime * 1000) / ( totIo);
				double calc = (IOTime + 0.0) / totIo;

				BigDecimal newVal = new BigDecimal(calc).setScale(1, BigDecimal.ROUND_HALF_EVEN);;
				diffData.setValueAt(newVal, rowId, AvgServ_msId);
			}
			else
				diffData.setValueAt(new BigDecimal(0), rowId, AvgServ_msId);

			//--------------------
			//---- ReadServiceTimeMs
			if (pos_ReadTime >= 0 && pos_ReadServiceTimeMs >= 0)
			{
				if (Reads != 0)
				{
					double calc = (ReadTime + 0.0) / Reads;
	
					BigDecimal newVal = new BigDecimal(calc).setScale(1, BigDecimal.ROUND_HALF_EVEN);;
					diffData.setValueAt(newVal, rowId, pos_ReadServiceTimeMs);
				}
				else
					diffData.setValueAt(new BigDecimal(0), rowId, pos_ReadServiceTimeMs);
			}

			//--------------------
			//---- WriteServiceTimeMs
			if (pos_WriteTime >= 0 && pos_WriteServiceTimeMs >= 0)
			{
				if (Writes != 0)
				{
					double calc = (WriteTime + 0.0) / Writes;
	
					BigDecimal newVal = new BigDecimal(calc).setScale(1, BigDecimal.ROUND_HALF_EVEN);;
					diffData.setValueAt(newVal, rowId, pos_WriteServiceTimeMs);
				}
				else
					diffData.setValueAt(new BigDecimal(0), rowId, pos_WriteServiceTimeMs);
			}

			//--------------------
			//---- ReadsPct
			if (totIo > 0)
			{
				double calc = (Reads + 0.0) / (Reads + Writes + 0.0) * 100.0;

				BigDecimal newVal = new BigDecimal(calc).setScale(1, BigDecimal.ROUND_HALF_EVEN);
				diffData.setValueAt(newVal, rowId, ReadsPctId);
			}
			else
				diffData.setValueAt(new BigDecimal(0), rowId, ReadsPctId);

//			//--------------------
//			//---- APFReadsPct
//			if (Reads > 0)
//			{
//				double calc = (APFReads + 0.0) / (Reads + 0.0) * 100.0;
//
//				BigDecimal newVal = new BigDecimal(calc).setScale(1, BigDecimal.ROUND_HALF_EVEN);
//				diffData.setValueAt(newVal, rowId, APFReadsPctId);
//			}
//			else
//				diffData.setValueAt(new BigDecimal(0), rowId, APFReadsPctId);

			//--------------------
			//---- WritesPct
			if (totIo > 0)
			{
				double calc = (Writes + 0.0) / (Reads + Writes + 0.0) * 100.0;

				BigDecimal newVal = new BigDecimal(calc).setScale(1, BigDecimal.ROUND_HALF_EVEN);
				diffData.setValueAt(newVal, rowId, WritesPctId);
			}
			else
				diffData.setValueAt(new BigDecimal(0), rowId, WritesPctId);
		}
	}
}
