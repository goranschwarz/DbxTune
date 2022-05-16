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
package com.asetune.cm.ase;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;
import javax.swing.JCheckBoxMenuItem;

import com.asetune.CounterControllerAse;
import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.ase.gui.CmDeviceIoPanel;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.graph.TrendGraphDataPoint.LabelType;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.gui.TrendGraph;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.conn.info.DbmsVersionInfo;
import com.asetune.sql.conn.info.DbmsVersionInfoSybaseAse;
import com.asetune.utils.Ver;

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

	public static final String[] MON_TABLES       = new String[] {"monDeviceIO"};
	public static final String[] NEED_ROLES       = new String[] {"mon_role"};
	public static final String[] NEED_CONFIG      = new String[] {"enable monitoring=1"};

	public static final String[] PCT_COLUMNS      = new String[] {"ReadsPct", "APFReadsPct", "WritesPct"};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"TotalIOs", "Reads", "APFReads", "Writes", "DevSemaphoreRequests", "DevSemaphoreWaits", "IOTime", 
		"ReadTime", "WriteTime"};

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

		setCounterController(counterController);
		setGuiController(guiController);

		addTrendGraphs();
		
		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	public static final String GRAPH_NAME_RW_ALL_DISK_IO  = "IoAllRW";
	public static final String GRAPH_NAME_R_ALL_DISK_IO   = "IoAllR";
	public static final String GRAPH_NAME_W_ALL_DISK_IO   = "IoAllW";
	
	public static final String GRAPH_NAME_RW_DISK_IO      = "IoRW";
	public static final String GRAPH_NAME_R_DISK_IO       = "IoR";
	public static final String GRAPH_NAME_W_DISK_IO       = "IoW";

	public static final String GRAPH_NAME_RW_SERVICE_TIME = "SvcTimeRW";
	public static final String GRAPH_NAME_R_SERVICE_TIME  = "SvcTimeR";
	public static final String GRAPH_NAME_W_SERVICE_TIME  = "SvcTimeW";

	private void addTrendGraphs()
	{
		//-----
		addTrendGraph(GRAPH_NAME_RW_ALL_DISK_IO,
			"Number of Disk Operations (Read+Write), per Second and ALL Devices", // Menu CheckBox text
			"Number of Disk Operations (Read+Write), per Second and ALL Devices ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC,
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
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC,
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
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC,
			new String[] { "ALL_devices" },
			LabelType.Static,
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height



		addTrendGraph(GRAPH_NAME_RW_DISK_IO,
			"Number of Disk Operations (Read+Write), per Second and Device", // Menu CheckBox text
			"Number of Disk Operations (Read+Write), per Second and Device ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC,
			null, LabelType.Dynamic, 
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_R_DISK_IO,
			"Number of Disk Operations (Read), per Second and Device", // Menu CheckBox text
			"Number of Disk Operations (Read), per Second and Device ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC,
			null, LabelType.Dynamic, 
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_W_DISK_IO,
			"Number of Disk Operations (Write), per Second and Device", // Menu CheckBox text
			"Number of Disk Operations (Write), per Second and Device ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERSEC,
			null, LabelType.Dynamic, 
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height



		//-----
		addTrendGraph(GRAPH_NAME_RW_SERVICE_TIME,
			"Device IO Service Time (Read+Write), per Device",                 // Menu CheckBox text
			"Device IO Service Time (Read+Write) in Milliseconds, per Device ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MILLISEC,
			null, LabelType.Dynamic, 
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_R_SERVICE_TIME,
			"Device IO Service Time (Read), per Device",                 // Menu CheckBox text
			"Device IO Service Time (Read) in Milliseconds, per Device ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MILLISEC,
			null, LabelType.Dynamic, 
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			false, // visible at start
			Ver.ver(15,7,0,2), // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_W_SERVICE_TIME,
			"Device IO Service Time (Write), per Device",                 // Menu CheckBox text
			"Device IO Service Time (Write) in Milliseconds, per Device ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MILLISEC,
			null, LabelType.Dynamic, 
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			false, // visible at start
			Ver.ver(15,7,0,2), // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
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
			mtd.addColumn("monDeviceIO",  "TotalIOs",     "<html>" +
			                                                   "Total number of IO's issued on this device.<br>" +
			                                                   "<b>Formula</b>: Reads + Writes<br>" +
			                                              "</html>");
			mtd.addColumn("monDeviceIO",  "APFReadsPct",  "<html>" +
			                                                   "Of all the issued Reads, what's the Asynch Prefetch Reads percentage.<br>" +
			                                                   "<b>Formula</b>: APFReads / Reads * 100<br>" +
			                                              "</html>");
			mtd.addColumn("monDeviceIO",  "WritesPct",    "<html>" +
			                                                   "Of all the issued IO's, what's the Write percentage.<br>" +
			                                                   "<b>Formula</b>: Writes / (Reads + Writes) * 100<br>" +
			                                              "</html>");
			mtd.addColumn("monDeviceIO",  "AvgServ_ms",   "<html>" +
			                                                   "Service time on the disk.<br>" +
			                                                   "This is basically the average time it took to make a disk IO on this device.<br>" +
			                                                   "Warning: ASE isn't timing each IO individually, Instead it uses the 'click ticks' to do it... This might change in the future.<br>" +
			                                                   "<b>Formula</b>: IOTime / (Reads + Writes) <br>" +
			                                                   "<b>Note</b>: If there is few I/O's this value might be a bit off, this due to 'click ticks' is 100 ms by default.<br>" +
			                                                   "<b>Note</b>: However in ASE 15.7.0 ESD#2 and beyond the time resolution seems to be better (measured at 1 ms level).<br>" +
			                                              "</html>");
			mtd.addColumn("monDeviceIO",  "ReadServiceTimeMs", "<html>" +
			                                                   "Service time on the disk for <b>Read</b> operations.<br>" +
			                                                   "This is basically the average time it took to make a <b>read</b> IO on this device.<br>" +
			                                                   "Warning: ASE isn't timing each IO individually, Instead it uses the 'click ticks' to do it... This might change in the future.<br>" +
			                                                   "<b>Formula</b>: ReadTime / Reads <br>" +
			                                                   "<b>Note</b>: If there is few I/O's this value might be a bit off, this due to 'click ticks' is 100 ms by default.<br>" +
			                                                   "<b>Note</b>: However in ASE 15.7.0 ESD#2 and beyond the time resolution seems to be better (measured at 1 ms level).<br>" +
			                                              "</html>");
			mtd.addColumn("monDeviceIO",  "WriteServiceTimeMs", "<html>" +
			                                                   "Service time on the disk for <b>Write</b> operations.<br>" +
			                                                   "This is basically the average time it took to make a <b>write</b> IO on this device.<br>" +
			                                                   "Warning: ASE isn't timing each IO individually, Instead it uses the 'click ticks' to do it... This might change in the future.<br>" +
			                                                   "<b>Formula</b>: WriteTime / Writes <br>" +
			                                                   "<b>Note</b>: If there is few I/O's this value might be a bit off, this due to 'click ticks' is 100 ms by default.<br>" +
			                                                   "<b>Note</b>: However in ASE 15.7.0 ESD#2 and beyond the time resolution seems to be better (measured at 1 ms level).<br>" +
			                                              "</html>");
		}
		catch (NameNotFoundException e) {/*ignore*/}
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		DbmsVersionInfoSybaseAse aseVersionInfo = (DbmsVersionInfoSybaseAse) versionInfo;
//		long    srvVersion       = aseVersionInfo.getLongVersion();
		boolean isClusterEnabled = aseVersionInfo.isClusterEdition();

		List <String> pkCols = new LinkedList<String>();

		if (isClusterEnabled)
			pkCols.add("InstanceID");

		pkCols.add("LogicalName");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		DbmsVersionInfoSybaseAse aseVersionInfo = (DbmsVersionInfoSybaseAse) versionInfo;
		long    srvVersion       = aseVersionInfo.getLongVersion();
		boolean isClusterEnabled = aseVersionInfo.isClusterEdition();

		String cols1, cols2, cols3;
		cols1 = cols2 = cols3 = "";

		if (isClusterEnabled)
		{
			cols1 += "InstanceID, ";
		}

//		String TotalIOs = "TotalIOs = (Reads + Writes)";
//		if (srvVersion > 15000) 
//			TotalIOs = "TotalIOs = (convert(bigint,Reads) + convert(bigint,Writes))";
//		String TotalIOs = "(Reads + Writes)";
		String TotalIOs = "(convert(numeric(10,0),Reads) + convert(numeric(10,0),Writes))"; // due to "Arithmetic overflow occurred" in 12.5.x
//		if (srvVersion > 15000) 
//		if (srvVersion > 1500000) 
		if (srvVersion > Ver.ver(15,0)) 
			TotalIOs = "(convert(bigint,Reads) + convert(bigint,Writes))";

		String DeviceType = "";
//		if (srvVersion >= 15020) 
//		if (srvVersion >= 1502000) 
		if (srvVersion >= Ver.ver(15,0,2)) 
		{
			DeviceType = 
				"DeviceType = CASE \n" +
				"               WHEN getdevicetype(PhysicalName) = 1 THEN 'RAW Device  '\n" +
				"               WHEN getdevicetype(PhysicalName) = 2 THEN 'BLOCK Device'\n" +
				"               WHEN getdevicetype(PhysicalName) = 3 THEN 'File        '\n" +
				"               ELSE '-unknown-'+convert(varchar(5), getdevicetype(PhysicalName))+'-'\n" +
				"             END,\n";
		}
		
		// ASE 15.7.0 ESD#2
		String ReadTime           = ""; // Total time spent reading from the device (ms)
		String WriteTime          = ""; // Total time spent writing to the device (ms)
		String ReadServiceTimeMs  = ""; // CALCULATED
		String WriteServiceTimeMs = ""; // CALCULATED
//		String nl_15702           = ""; // NL for this section
//		if (srvVersion >= 15702)
//		if (srvVersion >= 1570020)
		if (srvVersion >= Ver.ver(15,7,0,2))
		{
			ReadTime           = "ReadTime, ";  // DO DIFF CALC
			WriteTime          = "WriteTime, "; // DO DIFF CALC
			ReadServiceTimeMs  = "ReadServiceTimeMs  = CASE WHEN Reads > 0 \n" +
			                     "                          THEN convert(numeric(10,1), (ReadTime + 0.0) / (Reads + 0.0) ) \n" +
			                     "                          ELSE convert(numeric(10,1), null) \n" +
			                     "                     END, \n";
			WriteServiceTimeMs = "WriteServiceTimeMs = CASE WHEN Writes > 0 \n" +
			                     "                          THEN convert(numeric(10,1), (WriteTime + 0.0) / (Writes + 0.0) ) \n" +
			                     "                          ELSE convert(numeric(10,1), null) \n" +
			                     "                     END, \n";
//			nl_15702  = "\n";
		}
		
		cols1 += "LogicalName, TotalIOs = "+TotalIOs+", \n" +
		         "Reads, \n" +
		         "ReadsPct = CASE WHEN "+TotalIOs+" > 0 \n" +
		         "                THEN convert(numeric(10,1), (Reads + 0.0) / ("+TotalIOs+" + 0.0) * 100.0 ) \n" +
		         "                ELSE convert(numeric(10,1), 0.0 ) \n" +
		         "           END, \n" +
		         "APFReads, \n" +
		         "APFReadsPct = CASE WHEN Reads > 0 \n" +
		         "                   THEN convert(numeric(10,1), (APFReads + 0.0) / (Reads + 0.0) * 100.0 ) \n" +
		         "                   ELSE convert(numeric(10,1), 0.0 ) \n" +
		         "              END, \n" +
		         "Writes, \n" +
		         "WritesPct = CASE WHEN "+TotalIOs+" > 0 \n" +
		         "                 THEN convert(numeric(10,1), (Writes + 0.0) / ("+TotalIOs+" + 0.0) * 100.0 ) \n" +
		         "                 ELSE convert(numeric(10,1), 0.0 ) \n" +
		         "            END, \n" +
		         "DevSemaphoreRequests, DevSemaphoreWaits, IOTime, " + ReadTime + WriteTime + "\n";
		cols2 += "AvgServ_ms = CASE \n" +
				 "               WHEN "+TotalIOs+" > 0 \n" +
				 "               THEN convert(numeric(10,1), IOTime / convert(numeric(10,0), "+TotalIOs+")) \n" +
				 "               ELSE convert(numeric(10,1), null) \n" +
				 "             END, \n" +
				 ReadServiceTimeMs +
				 WriteServiceTimeMs;
		cols3 += DeviceType+" PhysicalName";
//		if (srvVersion >= 15010 || (srvVersion >= 12540 && srvVersion < 15000) )
//		if (srvVersion >= 1501000 || (srvVersion >= 1254000 && srvVersion < 1500000) )
		if (srvVersion >= Ver.ver(15,0,1) || (srvVersion >= Ver.ver(12,5,4) && srvVersion < Ver.ver(15,0)) )
		{
		}

		String sql = 
			"select " + cols1 + cols2 + cols3 + "\n" +
			"from master..monDeviceIO\n" +
			"order by LogicalName" + (isClusterEnabled ? ", InstanceID" : "") + "\n";

		// Simulate a lost connection
		//sql += "go\n waitfor delay '00:00:03' select syb_quit()";

		return sql;
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		long srvVersion = getServerVersion();

		//------------------------------------------------
		// ---- ALL DISK IO's PER SECOND GRAPHS
		//------------------------------------------------
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
			int tranPos = -1;
			if (getCounterDataAbs() != null)
				tranPos = getCounterDataAbs().findColumn("ReadServiceTimeMs");
	
			// Only available in 15.7.0 esd#2 and above 
//			if ( srvVersion < 15702 || tranPos == -1 )
//			if ( srvVersion < 1570020 || tranPos == -1 )
			if ( srvVersion < Ver.ver(15,7,0,2) || tranPos == -1 )
			{
				// disable the transactions graph checkbox...
				TrendGraph tg = getTrendGraph(GRAPH_NAME_R_SERVICE_TIME);
				if (tg != null)
				{
					JCheckBoxMenuItem menuItem = tg.getViewMenuItem();
					if (menuItem.isSelected())
						menuItem.doClick();
				}
			}
			else
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
		}

		if (GRAPH_NAME_W_SERVICE_TIME.equals(tgdp.getName()))
		{
			int tranPos = -1;
			if (getCounterDataAbs() != null)
				tranPos = getCounterDataAbs().findColumn("WriteServiceTimeMs");
	
			// Only available in 15.7.0 esd#2 and above 
//			if ( srvVersion < 15702 || tranPos == -1 )
//			if ( srvVersion < 1570020 || tranPos == -1 )
			if ( srvVersion < Ver.ver(15,7,0,2) || tranPos == -1 )
			{
				// disable the transactions graph checkbox...
				TrendGraph tg = getTrendGraph(GRAPH_NAME_W_SERVICE_TIME);
				if (tg != null)
				{
					JCheckBoxMenuItem menuItem = tg.getViewMenuItem();
					if (menuItem.isSelected())
						menuItem.doClick();
				}
			}
			else
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
	}

	/** 
	 * Compute the avgServ column, which is IOTime/(Reads+Writes)
	 */
	@Override
	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
	{
		int AvgServ_msId=-1, ReadsPctId=-1, APFReadsPctId=-1, WritesPctId=-1;

		int Reads,      APFReads,      Writes,      IOTime;
		int ReadsId=-1, APFReadsId=-1, WritesId=-1, IOTimeId=-1;
		
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
			else if (colName.equals("APFReads"))    APFReadsId    = colId;
			else if (colName.equals("Writes"))      WritesId      = colId;
			else if (colName.equals("IOTime"))      IOTimeId      = colId;

			else if (colName.equals("ReadsPct"))    ReadsPctId    = colId;
			else if (colName.equals("APFReadsPct")) APFReadsPctId = colId;
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
			APFReads = ((Number) diffData.getValueAt(rowId, APFReadsId)).intValue();
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

			//--------------------
			//---- APFReadsPct
			if (Reads > 0)
			{
				double calc = (APFReads + 0.0) / (Reads + 0.0) * 100.0;

				BigDecimal newVal = new BigDecimal(calc).setScale(1, BigDecimal.ROUND_HALF_EVEN);
				diffData.setValueAt(newVal, rowId, APFReadsPctId);
			}
			else
				diffData.setValueAt(new BigDecimal(0), rowId, APFReadsPctId);

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
