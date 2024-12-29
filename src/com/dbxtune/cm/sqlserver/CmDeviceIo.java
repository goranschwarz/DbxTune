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
package com.dbxtune.cm.sqlserver;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import com.dbxtune.CounterControllerAse;
import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.central.pcs.CentralPersistReader;
import com.dbxtune.cm.CounterSample;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.sqlserver.gui.CmDeviceIoPanel;
import com.dbxtune.config.dict.MonTablesDictionary;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.graph.TrendGraphDataPoint;
import com.dbxtune.graph.TrendGraphDataPoint.LabelType;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.sql.conn.info.DbmsVersionInfoSqlServer;

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

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"deviceIo", "dm_io_virtual_file_stats", "master_files"};
	public static final String[] NEED_ROLES       = new String[] {"VIEW SERVER STATE"};
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
	public static final String GRAPH_NAME_SRW_ALL_DISK_KB = "KbAllSRW";

	public static final String GRAPH_NAME_SRW_ALL_DISK_IO = "IoAllSRW";
	public static final String GRAPH_NAME_RW_ALL_DISK_IO  = "IoAllRW";
	public static final String GRAPH_NAME_R_ALL_DISK_IO   = "IoAllR";
	public static final String GRAPH_NAME_W_ALL_DISK_IO   = "IoAllW";
	
	public static final String GRAPH_NAME_RW_DISK_IO      = "IoRW";
	public static final String GRAPH_NAME_R_DISK_IO       = "IoR";
	public static final String GRAPH_NAME_W_DISK_IO       = "IoW";

//	public static final String GRAPH_NAME_RW_AVG_KB_PER_IO= "AvgKbPerIoRW";  // This is not done so far... maybe in the future
	public static final String GRAPH_NAME_R_AVG_KB_PER_IO = "AvgKbPerIoR";
	public static final String GRAPH_NAME_W_AVG_KB_PER_IO = "AvgKbPerIoW";

	public static final String GRAPH_NAME_RW_SERVICE_TIME = "SvcTimeRW";
	public static final String GRAPH_NAME_R_SERVICE_TIME  = "SvcTimeR";
	public static final String GRAPH_NAME_W_SERVICE_TIME  = "SvcTimeW";

	private void addTrendGraphs()
	{
		//-----
		addTrendGraph(GRAPH_NAME_SRW_ALL_DISK_KB,
			"Disk Throughput in KB (Sum,Read,Write), per Second and ALL Devices", // Menu CheckBox text
			"Disk Throughput in KB (Sum,Read,Write), per Second and ALL Devices ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_KB, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Sum_KB_ALL_devices", "Read_KB_ALL_devices", "Write_KB_ALL_devices" },
			LabelType.Static,
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_SRW_ALL_DISK_IO,
			"Number of Disk Operations (Sum,Read,Write), per Second and ALL Devices", // Menu CheckBox text
			"Number of Disk Operations (Sum,Read,Write), per Second and ALL Devices ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Sum_ALL_devices", "Read_ALL_devices", "Write_ALL_devices" },
			LabelType.Static,
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_RW_ALL_DISK_IO,
			"Number of Disk Operations (Read+Write), per Second and ALL Devices", // Menu CheckBox text
			"Number of Disk Operations (Read+Write), per Second and ALL Devices ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "ALL_devices" },
			LabelType.Static,
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_R_ALL_DISK_IO,
			"Number of Disk Operations (Read), per Second and ALL Devices", // Menu CheckBox text
			"Number of Disk Operations (Read), per Second and ALL Devices ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "ALL_devices" },
			LabelType.Static,
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_W_ALL_DISK_IO,
			"Number of Disk Operations (Write), per Second and ALL Devices", // Menu CheckBox text
			"Number of Disk Operations (Write), per Second and ALL Devices ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "ALL_devices" },
			LabelType.Static,
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height



		//-----
		addTrendGraph(GRAPH_NAME_RW_DISK_IO,
			"Number of Disk Operations (Read+Write), per Second and Device", // Menu CheckBox text
			"Number of Disk Operations (Read+Write), per Second and Device ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_R_DISK_IO,
			"Number of Disk Operations (Read), per Second and Device", // Menu CheckBox text
			"Number of Disk Operations (Read), per Second and Device ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_W_DISK_IO,
			"Number of Disk Operations (Write), per Second and Device", // Menu CheckBox text
			"Number of Disk Operations (Write), per Second and Device ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height



		//-----
//		addTrendGraph(GRAPH_NAME_RW_AVG_KB_PER_IO,
//			"Average (Read+Write) KB Per Disk IO, per Device", // Menu CheckBox text
//			"Average (Read+Write) KB Per Disk IO, per Device ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
//			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_KB, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
//			null, 
//			LabelType.Dynamic,
//			TrendGraphDataPoint.Category.DISK,
//			false, // is Percent Graph
//			true,  // visible at start
//			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_R_AVG_KB_PER_IO,
			"Average (Read) KB Per Disk IO, per Device", // Menu CheckBox text
			"Average (Read) KB Per Disk IO, per Device ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_KB, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_W_AVG_KB_PER_IO,
			"Average (Write) KB Per Disk IO, per Device", // Menu CheckBox text
			"Average (Write) KB Per Disk IO, per Device ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_KB, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height



		//-----
		addTrendGraph(GRAPH_NAME_RW_SERVICE_TIME,
			"Device IO Service Time (Read+Write), per Device",                 // Menu CheckBox text
			"Device IO Service Time (Read+Write) in Milliseconds, per Device ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MILLISEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_R_SERVICE_TIME,
			"Device IO Service Time (Read), per Device",                 // Menu CheckBox text
			"Device IO Service Time (Read) in Milliseconds, per Device ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MILLISEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			false, // visible at start
			0, // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_W_SERVICE_TIME,
			"Device IO Service Time (Write), per Device",                 // Menu CheckBox text
			"Device IO Service Time (Write) in Milliseconds, per Device ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MILLISEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			false, // visible at start
			0, // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

	}

	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmDeviceIoPanel(this);
	}

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
			mtd.addTable("deviceIo",  "");

			mtd.addColumn("deviceIo",  "dbname",          "<html>Name of the database this device is attached to </html>");
			mtd.addColumn("deviceIo",  "type",            "<html>Data or Log </html>");
			mtd.addColumn("deviceIo",  "LogicalName",     "<html>The logical name of the device </html>");

			mtd.addColumn("deviceIo",  "TotalIOs",        "<html>" +
			                                                   "Total number of IO's issued on this device.<br>" +
			                                                   "<b>Formula</b>: Reads + Writes<br>" +
			                                              "</html>");
			mtd.addColumn("deviceIo",  "Reads",           "<html>Number of Reads made on this device </html>");
			mtd.addColumn("deviceIo",  "ReadsKB",         "<html>How Many KB that was Read from this device </html>");
			mtd.addColumn("deviceIo",  "AvgReadKbPerIo",  "<html>" +
			                                                 "Average Read KB Per issued IO.<br>" +
			                                                 "<b>Formula</b>: ReadsKB / Reads <br>" +
			                                              "</html>");
			mtd.addColumn("deviceIo",  "ReadsPct",        "<html>How many of the TotalIO's was Reads in Percent</html>");

			mtd.addColumn("deviceIo",  "Writes",           "<html>Number of Writes made on this device </html>");
			mtd.addColumn("deviceIo",  "WritesKB",         "<html>How Many KB that was Written from this device </html>");
			mtd.addColumn("deviceIo",  "AvgWriteKbPerIo", "<html>" +
			                                                  "Average Write KB Per issued IO.<br>" +
			                                                  "<b>Formula</b>: WritesKB / Writes <br>" +
			                                              "</html>");
			mtd.addColumn("deviceIo",  "WritesPct",       "<html>How many of the TotalIO's was Writes in Percent</html>");
			mtd.addColumn("deviceIo",  "IOTime",          "<html>How many milliseconds were we spending on waiting for ALL IO's</html>");
			mtd.addColumn("deviceIo",  "ReadTime",        "<html>How many milliseconds were we spending on waiting for Read IO's</html>");
			mtd.addColumn("deviceIo",  "WriteTime",       "<html>How many milliseconds were we spending on waiting for Write IO's</html>");
			
			mtd.addColumn("deviceIo",  "AvgServ_ms",      "<html>" +
			                                                   "Service time on the disk.<br>" +
			                                                   "This is basically the average time it took to make a disk IO on this device.<br>" +
			                                                   "<b>Formula</b>: IOTime / (Reads + Writes) <br>" +
			                                              "</html>");
			mtd.addColumn("deviceIo",  "ReadServiceTimeMs", "<html>" +
			                                                   "Service time on the disk for <b>Read</b> operations.<br>" +
			                                                   "This is basically the average time it took to make a <b>read</b> IO on this device.<br>" +
			                                                   "<b>Formula</b>: ReadTime / Reads <br>" +
			                                              "</html>");
			mtd.addColumn("deviceIo",  "WriteServiceTimeMs", "<html>" +
			                                                   "Service time on the disk for <b>Write</b> operations.<br>" +
			                                                   "This is basically the average time it took to make a <b>write</b> IO on this device.<br>" +
			                                                   "<b>Formula</b>: WriteTime / Writes <br>" +
			                                              "</html>");
			mtd.addColumn("deviceIo",  "SizeOnDiskMB",    "<html>Disk size for this device</html>");
			mtd.addColumn("deviceIo",  "lname",           "<html>The logical name</html>");
			mtd.addColumn("deviceIo",  "PhysicalName",    "<html>Full path to this device/file</html>");
			mtd.addColumn("deviceIo",  "database_id",     "<html>ID of the database</html>");
			mtd.addColumn("deviceIo",  "file_id",         "<html>ID of the file</html>");
		}
		catch (NameNotFoundException e) 
		{
		//	_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
			System.out.println("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
		}
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("database_id");
		pkCols.add("file_id");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		DbmsVersionInfoSqlServer ssVersionInfo = (DbmsVersionInfoSqlServer) versionInfo;
//		long srvVersion = ssVersionInfo.getLongVersion();

		String dm_io_virtual_file_stats = "dm_io_virtual_file_stats";
		String master_files             = "master_files";
		
		if (ssVersionInfo.isAzureSynapseAnalytics())
		{
			dm_io_virtual_file_stats = "dm_pdw_nodes_io_virtual_file_stats";
			master_files             = "master_files";  // SAME NAME IN AZURE ????
		}

		// Special thing for Azure SQL Database
		String joinMasterFiles = "JOIN sys." + master_files +" b ON a.file_id = b.file_id AND a.database_id = b.database_id \n";
		if (ssVersionInfo.isAzureDb() || ssVersionInfo.isAzureSynapseAnalytics())
		{
			// NOTE: for Azure SQL Database, tempdb will have faulty 'devicename' and 'physical_name' (but lets fix that LATER)
			joinMasterFiles = "JOIN sys.database_files b ON a.file_id = b.file_id AND a.database_id in (db_id('tempdb'), db_id()) \n";
		}

		String cols;

		String TotalIOs = "(a.num_of_reads + a.num_of_writes)";

		cols = "dbname             = isnull(db_name(a.database_id), 'unknow-dbid-' + cast(a.database_id as varchar(30))), \n" +
		       "type               = CASE \n" +
		       "                         WHEN a.file_id = 2 THEN 'Log' \n" +
		       "                         ELSE 'Data' \n" +
		       "                     END, \n" +
		       "LogicalName        = isnull(db_name(a.database_id), 'unknow-dbid-' + cast(a.database_id as varchar(30))) \n" +
		       "                   + CASE \n" +
		       "                         WHEN a.file_id = 1 THEN '_Data' \n" +
		       "                         WHEN a.file_id = 2 THEN '_Log' \n" +
		       "                         ELSE '_fid_' + cast(a.file_id as varchar(10)) \n" +
		       "                     END, \n" +
		       "TotalIOs           = "+TotalIOs+", \n" +
		       "Reads              = a.num_of_reads, \n" +
		       "ReadsKB            = (a.num_of_bytes_read / 1024), \n" +
		       "AvgReadKbPerIo     = convert(numeric(10,1), (a.num_of_bytes_read / 1024.0) / (1.0 * NULLIF(a.num_of_reads, 0))), \n" +
		       "ReadsPct           = CASE WHEN "+TotalIOs+" > 0 \n" +
		       "                          THEN convert(numeric(10,1), (a.num_of_reads + 0.0) / ("+TotalIOs+" + 0.0) * 100.0 ) \n" +
		       "                          ELSE convert(numeric(10,1), 0.0 ) \n" +
		       "                     END, \n" +
		       "Writes             = a.num_of_writes, \n" +
		       "WritesKB           = (a.num_of_bytes_written / 1024), \n" +
		       "AvgWriteKbPerIo    = convert(numeric(10,1), (a.num_of_bytes_written / 1024.0) / (1.0 * NULLIF(a.num_of_writes, 0))), \n" +
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
		       "lname              = b.name, \n" +
		       "PhysicalName       = b.physical_name, \n" +
		       "database_id        = a.database_id, \n" +
		       "file_id            = a.file_id \n";

		String sql = 
			"select /* ${cmCollectorName} */ \n" + 
			cols +
			"FROM sys." + dm_io_virtual_file_stats + " (NULL, NULL) a \n" +
			joinMasterFiles +
//			"ORDER BY a.io_stall DESC \n";
			"ORDER BY a.database_id, a.file_id \n";

		return sql;
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
//		long srvVersion = getServerVersion();

		//------------------------------------------------
		// ---- ALL DISK Throughput in KB PER SECOND GRAPHS
		//------------------------------------------------
		if (GRAPH_NAME_SRW_ALL_DISK_KB.equals(tgdp.getName()))
		{
			Double readsKB  = this.getRateValueSum("ReadsKB");
			Double writesKB = this.getRateValueSum("WritesKB");

			Double[] arr = new Double[3];
			arr[0] = readsKB + writesKB;
			arr[1] = readsKB;
			arr[2] = writesKB;

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		//------------------------------------------------
		// ---- ALL DISK IO's PER SECOND GRAPHS
		//------------------------------------------------
		if (GRAPH_NAME_SRW_ALL_DISK_IO.equals(tgdp.getName()))
		{
			Double[] arr = new Double[3];
			arr[0] = this.getRateValueSum("TotalIOs");
			arr[1] = this.getRateValueSum("Reads");
			arr[2] = this.getRateValueSum("Writes");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		if (GRAPH_NAME_RW_ALL_DISK_IO.equals(tgdp.getName()))
		{
			Double[] arr = new Double[1];
			arr[0] = this.getRateValueSum("TotalIOs");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		if (GRAPH_NAME_R_ALL_DISK_IO.equals(tgdp.getName()))
		{
			Double[] arr = new Double[1];
			arr[0] = this.getRateValueSum("Reads");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		if (GRAPH_NAME_W_ALL_DISK_IO.equals(tgdp.getName()))
		{
			Double[] arr = new Double[1];
			arr[0] = this.getRateValueSum("Writes");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		
		//------------------------------------------------
		// ---- DISK IO's PER SECOND GRAPHS
		//------------------------------------------------
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
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
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
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
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
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
		}


		//------------------------------------------------
		// ---- AVERAGE KB PER IO
		//------------------------------------------------
//		if (GRAPH_NAME_RW_AVG_KB_PER_IO.equals(tgdp.getName()))
//		{
//			// Write 1 "line" for every device
//			Double[] dArray = new Double[this.size()];
//			String[] lArray = new String[dArray.length];
//			for (int i = 0; i < dArray.length; i++)
//			{
//				lArray[i] = this.getRateString       (i, "LogicalName");
//				dArray[i] = this.getRateValueAsDouble(i, "TotalIOs");
//			}
//
//			// Set the values
//			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
//		}

		if (GRAPH_NAME_R_AVG_KB_PER_IO.equals(tgdp.getName()))
		{
			// Write 1 "line" for every device
			Double[] dArray = new Double[this.size()];
			String[] lArray = new String[dArray.length];
			for (int i = 0; i < dArray.length; i++)
			{
				lArray[i] = this.getRateString       (i, "LogicalName");
				dArray[i] = this.getRateValueAsDouble(i, "AvgReadKbPerIo");
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
		}

		if (GRAPH_NAME_W_AVG_KB_PER_IO.equals(tgdp.getName()))
		{
			// Write 1 "line" for every device
			Double[] dArray = new Double[this.size()];
			String[] lArray = new String[dArray.length];
			for (int i = 0; i < dArray.length; i++)
			{
				lArray[i] = this.getRateString       (i, "LogicalName");
				dArray[i] = this.getRateValueAsDouble(i, "AvgWriteKbPerIo");
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
		}

		
		//------------------------------------------------
		// ---- SERVICE TIME GRAPHS
		//------------------------------------------------
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
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
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
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
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
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
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

		int pos_ReadsKB         = -1;
		int pos_WritesKB        = -1;
		int pos_AvgReadKbPerIo  = -1;
		int pos_AvgWriteKbPerIo = -1;
		
		// Find column Id's
		List<String> colNames = diffData.getColNames();
		if (colNames == null)
			return;
		for (int colId = 0; colId < colNames.size(); colId++)
		{
			String colName = (String) colNames.get(colId);
			if      (colName.equals("Reads"))              ReadsId                = colId;
//			else if (colName.equals("APFReads"))           APFReadsId             = colId;
			else if (colName.equals("Writes"))             WritesId               = colId;
			else if (colName.equals("IOTime"))             IOTimeId               = colId;

			else if (colName.equals("ReadsPct"))           ReadsPctId             = colId;
//			else if (colName.equals("APFReadsPct"))        APFReadsPctId          = colId;
			else if (colName.equals("WritesPct"))          WritesPctId            = colId;
			else if (colName.equals("AvgServ_ms"))         AvgServ_msId           = colId;

			else if (colName.equals("ReadTime"))           pos_ReadTime           = colId;
			else if (colName.equals("WriteTime"))          pos_WriteTime          = colId;
			else if (colName.equals("ReadServiceTimeMs"))  pos_ReadServiceTimeMs  = colId;
			else if (colName.equals("WriteServiceTimeMs")) pos_WriteServiceTimeMs = colId;

			else if (colName.equals("ReadsKB"))            pos_ReadsKB            = colId;
			else if (colName.equals("WritesKB"))           pos_WritesKB           = colId;
			else if (colName.equals("AvgReadKbPerIo"))     pos_AvgReadKbPerIo     = colId;
			else if (colName.equals("AvgWriteKbPerIo"))    pos_AvgWriteKbPerIo    = colId;
		}

		// Loop on all diffData rows
		for (int rowId = 0; rowId < diffData.getRowCount(); rowId++)
		{
			Reads    = diffData.getValueAsInteger(rowId, ReadsId   , 0);
//			APFReads = diffData.getValueAsInteger(rowId, APFReadsId, 0);
			Writes   = diffData.getValueAsInteger(rowId, WritesId  , 0);
			IOTime   = diffData.getValueAsInteger(rowId, IOTimeId  , 0);
			if (pos_ReadTime  >= 0) ReadTime  = diffData.getValueAsInteger(rowId, pos_ReadTime , 0);
			if (pos_WriteTime >= 0) WriteTime = diffData.getValueAsInteger(rowId, pos_WriteTime, 0);

			int totIo = Reads + Writes;

			//--------------------
			//---- AvgServ_ms
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

			
			//--------------------
			//---- AvgReadKbPerIo
			if (Reads > 0)
			{
				int ReadsKB = diffData.getValueAsInteger(rowId, pos_ReadsKB, 0);
				double calc = (ReadsKB + 0.0) / (Reads + 0.0);
				
				BigDecimal newVal = new BigDecimal(calc).setScale(1, BigDecimal.ROUND_HALF_EVEN);
				//System.out.println(">>> ReadsKB=" + ReadsKB + ", Reads=" + Reads + ", calc=" + calc + ", newVal=" + newVal + ", pos_AvgReadKbPerIo=" + pos_AvgReadKbPerIo);
				diffData.setValueAt(newVal, rowId, pos_AvgReadKbPerIo);
			}
			else
				diffData.setValueAt(new BigDecimal(0), rowId, pos_AvgReadKbPerIo);

			//--------------------
			//---- AvgWriteKbPerIo
			if (Writes > 0)
			{
				int WritesKB = diffData.getValueAsInteger(rowId, pos_WritesKB, 0);
				double calc = (WritesKB + 0.0) / (Writes + 0.0);
				
				BigDecimal newVal = new BigDecimal(calc).setScale(1, BigDecimal.ROUND_HALF_EVEN);
				//System.out.println(">>> WritesKB=" + WritesKB + ", Writes=" + Writes + ", calc=" + calc + ", newVal=" + newVal + ", pos_AvgWriteKbPerIo=" + pos_AvgWriteKbPerIo);
				diffData.setValueAt(newVal, rowId, pos_AvgWriteKbPerIo);
			}
			else
				diffData.setValueAt(new BigDecimal(0), rowId, pos_AvgWriteKbPerIo);
		}
	}
}
