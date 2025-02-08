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
package com.dbxtune.cm.ase;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.alarm.AlarmHandler;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.alarm.events.AlarmEventHighCpuUtilization;
import com.dbxtune.alarm.events.AlarmEventHighCpuUtilization.CpuType;
import com.dbxtune.central.pcs.CentralPersistReader;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CounterSample;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.ase.gui.CmEnginesPanel;
import com.dbxtune.config.dict.MonTablesDictionary;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.graph.TrendGraphDataPoint;
import com.dbxtune.graph.TrendGraphDataPoint.LabelType;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.gui.TrendGraph;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.sql.conn.info.DbmsVersionInfoSybaseAse;
import com.dbxtune.utils.AseConnectionUtils;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.MathUtils;
import com.dbxtune.utils.Ver;

/**
 * Use ASE monEngine table to gather CPU Usage statistics
 * <p>
 * in ASE 15.7 threaded mode, the IOCPUTime seems to use the DiskController Thread for all engines...<br>
 * This leads to a higher IO Usage than it *really* is...<br>
 * If option PROPKEY_collapse_IoCpuTime_to_IdleCpuTime is true the IOCPUTime will be moved/collapsed into IdleCPUTime<br>
 * This might be addressed in ASE CR#757246, see CmEnginesPanel for a more detailed description.
 * <p>
 *  
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmEngines
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmEngines.class.getSimpleName();
	public static final String   SHORT_NAME       = "Engines";
	public static final String   HTML_DESC        = 
		"<html>" +
		"What ASE Server engine is working. <br>" +
		"In here we can also see what engines are doing/checking for IO's" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"monEngine"};
	public static final String[] NEED_ROLES       = new String[] {"mon_role"};
	public static final String[] NEED_CONFIG      = new String[] {"enable monitoring=1"};

	public static final String[] PCT_COLUMNS      = new String[] {"NonIdleCPUTimePct", "SystemCPUTimePct", "UserCPUTimePct", "IOCPUTimePct", "IdleCPUTimePct"};//"CPUTime", "SystemCPUTime", "UserCPUTime", "IdleCPUTime", "IOCPUTime"};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"CPUTime", "SystemCPUTime", "UserCPUTime", "IdleCPUTime", "IOCPUTime", "Yields", 
		"DiskIOChecks", "DiskIOPolled", "DiskIOCompleted", "ContextSwitches" 
		, "HkgcPendingItems"      // Number of items yet to be garbage collected by housekeeper garbage collector on this engine
		, "HkgcOverflows"         // Number of items that could not be queued to housekeeper garbage collector due to queue overflows
		, "HkgcPendingItemsDcomp" // Number of data pages yet to be page compressed by housekeeper garbage collector on this engine
		, "HkgcOverflowsDcomp"    // Number of data pages that could not be queued to housekeeper garbage collector for data page compression due to queue overflows
		, "HkgcOverflowsCmpact"   // Number of items that could not be queued to housekeeper garbage collector for compact due to queue overflows
		, "HkgcOverflowsRec"      // Number of items requested from rollback that could not be queued to housekeeper garbage collector due to queue overflows
		, "LotsDelRolXacts"       // Number of roll-backed transactions which made lots of empty pages
		, "LotsDelCmtXacts"       // Number of committed transactions which made lots of empty pages
//		, "LongXacts"             // Number of transactions last for equal or more than 1 hours
		, "PostcommitPageCount"   // Number of pages deallocated in post commit phase
		, "HkgcReq"               // Total number of events that were queued in Housekeeper Garbage Collection(HKGC) queues
		, "HkgcReqCmpact"         // Total number of events that were queued in Housekeeper Garbage Collection(HKGC) queues for compact
		, "HkgcReqDcomp"          // Total number of events that were queued in Housekeeper Garbage Collection(HKGC) queues for page compression
		, "HkgcReqRec"            // Total number of events that were queued in Housekeeper Garbage Collection(HKGC) queues from UNDO
		, "HkgcRetries"           // Total number of times we tried for queued items.
		};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
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

		return new CmEngines(counterController, guiController);
	}

	public CmEngines(ICounterController counterController, IGuiController guiController)
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

		addTrendGraphs();
		
		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
//	sp_configure 'kernel mode' = 'threaded'|'process'
	private String  _config_kernelMode = "";
	private boolean _collapse_IoCpuTime_to_IdleCpuTime = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_collapse_IoCpuTime_to_IdleCpuTime, DEFAULT_collapse_IoCpuTime_to_IdleCpuTime);

	private static final String  PROP_PREFIX                               = CM_NAME;

	public static final String   PROPKEY_collapse_IoCpuTime_to_IdleCpuTime = PROP_PREFIX + ".collapse.IoCpuTime_to_IdleCpuTime";
	public static final boolean  DEFAULT_collapse_IoCpuTime_to_IdleCpuTime = true;


	public static final String   GRAPH_NAME_CPU_SUM = "cpuSum"; //GetCounters.CM_GRAPH_NAME__ENGINE__CPU_SUM;
	public static final String   GRAPH_NAME_CPU_ENG = "cpuEng"; //GetCounters.CM_GRAPH_NAME__ENGINE__CPU_ENG;

	@Override
	protected void registerDefaultValues()
	{
		super.registerDefaultValues();

		Configuration.registerDefaultValue(PROPKEY_collapse_IoCpuTime_to_IdleCpuTime, DEFAULT_collapse_IoCpuTime_to_IdleCpuTime);
	}

	public boolean inThreadedMode()
	{
		return "threaded".equalsIgnoreCase(_config_kernelMode);
	}

	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmEnginesPanel(this);
	}

	private void addTrendGraphs()
	{
//		String[] sumLabels = new String[] { "System+User CPU", "System CPU", "User CPU" };
////		String[] engLabels = new String[] { "eng-0" };
//		String[] engLabels = TrendGraphDataPoint.RUNTIME_REPLACED_LABELS;
//		
//		addTrendGraphData(GRAPH_NAME_CPU_SUM, new TrendGraphDataPoint(GRAPH_NAME_CPU_SUM, sumLabels, LabelType.Static));
//		addTrendGraphData(GRAPH_NAME_CPU_ENG, new TrendGraphDataPoint(GRAPH_NAME_CPU_ENG, engLabels, LabelType.Dynamic));

		addTrendGraph(GRAPH_NAME_CPU_SUM,
			"CPU Summary", 	                                 // Menu CheckBox text
			"CPU Summary for all Engines ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERCENT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "System+User CPU", "System CPU", "User CPU" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.CPU,
			true, // is Percent Graph
			true, // visible at start
			0,    // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);  // minimum height

		addTrendGraph(GRAPH_NAME_CPU_ENG,
			"CPU per Engine",                       // Menu CheckBox text
			"CPU Usage per Engine (System + User) ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERCENT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.CPU,
			true, // is Percent Graph
			true, // visible at start
			0,    // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);  // minimum height

	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			mtd.addColumn("monEngine", "NonIdleCPUTimePct"  ,"<html>'CPU Busy' Percent calculation<br>         <b>Formula</b>: (SystemCPUTime + UserCPUTime [+ IOCPUTime]) / CPUTime * 100.0<br> <b>Note</b>: IOCPUTime depends on ASE Version and if 'Collapse IOCPUTime into IdleCPUTime' is enabled or not.<br></html>");
			mtd.addColumn("monEngine", "SystemCPUTimePct"   ,"<html>'System CPU Busy' Percent calculation.<br> <b>Formula</b>: SystemCPUTime / CPUTime * 100.0   <br></html>");
			mtd.addColumn("monEngine", "UserCPUTimePct"     ,"<html>'User CPU Busy' Percent calculation.<br>   <b>Formula</b>: UserCPUTime / CPUTime * 100.0     <br></html>");
			mtd.addColumn("monEngine", "IOCPUTimePct"       ,"<html>'IO CPU Busy' Percent calculation.<br>     <b>Formula</b>: IOCPUTime / CPUTime * 100.0       <br> <b>Note</b>: IOCPUTime depends on ASE Version and if 'Collapse IOCPUTime into IdleCPUTime' is enabled or not. (-1 if 'Collapse...' is enabled)<br></html>");
			mtd.addColumn("monEngine", "IdleCPUTimePct"     ,"<html>'Idle' Percent calculation.<br>            <b>Formula</b>: IdleCPUTime / CPUTime * 100.0     <br></html>");
		}
		catch (NameNotFoundException e) 
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
		//	System.out.println("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
		}
	}

	@Override
	public String[] getDependsOnConfigForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return NEED_CONFIG;
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

		pkCols.add("EngineNumber");

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

		// Get if we are in "process" or "threaded" kernel mode, which will be an input if "IOCPUTime" should be treated as "IdleCPUTime" 
		// This is only valid for ASE 15.7 and relates to: CR 757246- sp_sysmon IO Busy is over weighted in threaded mode.
		//sp_configure 'kernel mode' = 'threaded'|'process'
		if (srvVersion >= Ver.ver(15,7) && conn != null)
		{
			_config_kernelMode = AseConnectionUtils.getAseConfigRunValueStrNoEx(conn, "kernel mode");
//System.out.println("getSqlForVersion(): _config_kernelMode="+_config_kernelMode);
		}
		
		_collapse_IoCpuTime_to_IdleCpuTime = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_collapse_IoCpuTime_to_IdleCpuTime, DEFAULT_collapse_IoCpuTime_to_IdleCpuTime);
//System.out.println("getSqlForVersion(): collapse_IoCpuTime_to_IdleCpuTime="+_collapse_IoCpuTime_to_IdleCpuTime);

		
		String ThreadID = "";
		if (srvVersion >= Ver.ver(15,7))
		{
			ThreadID = "ThreadID, ";
		}

		if (isClusterEnabled)
		{
			cols1 += "InstanceID, ";
		}

		// 15.7.0.1 counters
		String HkgcPendingItemsDcomp = "";
		String HkgcOverflowsDcomp    = "";
		String nl_15701              = "";
		if (srvVersion >= Ver.ver(15,7,0,1))
		{
			HkgcPendingItemsDcomp = "HkgcPendingItemsDcomp, ";
			HkgcOverflowsDcomp    = "HkgcOverflowsDcomp, ";
			nl_15701              = "\n";
		}

		// 15.7 & kernelMode='threaded' & PROPKEY_collapse_IoCpuTime_to_IdleCpuTime
		// we may be able to collapse IOCPUTime -> IdleCPUTime
		String IOCPUTime           = "";
		String IdleCPUTime         = "IdleCPUTime, ";
		
		String NonIdleCPUTime_calc = "SystemCPUTime + UserCPUTime";
		if (srvVersion >= Ver.ver(15,5) || (srvVersion >= Ver.ver(15,0,3) && isClusterEnabled) )
		{
			NonIdleCPUTime_calc = "convert(bigint,SystemCPUTime) + convert(bigint,UserCPUTime) + convert(bigint,IOCPUTime)";

			// take away IOCPUTime in 15.7 & inThreadedMode & checkBoxIsEnabled
			if (srvVersion >= Ver.ver(15,7) && inThreadedMode() && _collapse_IoCpuTime_to_IdleCpuTime)
				NonIdleCPUTime_calc = "convert(bigint,SystemCPUTime) + convert(bigint,UserCPUTime)";
				
		}
		
		// ------------------------------------------------------------------------------------------------
		// New monEngines Cols in ASE 15.7 SP 136, and above 16.0 SP2 PL2
		// ------------------------------------------------------------------------------------------------
		String HkgcOverflowsCmpact = ""; // Number of items that could not be queued to housekeeper garbage collector for compact due to queue overflows	          
		String HkgcOverflowsRec    = ""; // Number of items requested from rollback that could not be queued to housekeeper garbage collector due to queue overflows	
		String LotsDelRolXacts     = ""; // Number of roll-backed transactions which made lots of empty pages	                                                      
		String LotsDelCmtXacts     = ""; // Number of committed transactions which made lots of empty pages	                                                      
		String LongXacts           = ""; // Number of transactions last for equal or more than 1 hours	                                                              
		String PostcommitPageCount = ""; // Number of pages deallocated in post commit phase	                                                                      
		String HkgcReq             = ""; // Total number of events that were queued in Housekeeper Garbage Collection(HKGC) queues	                                  
		String HkgcReqCmpact       = ""; // Total number of events that were queued in Housekeeper Garbage Collection(HKGC) queues for compact	                      
		String HkgcReqDcomp        = ""; // Total number of events that were queued in Housekeeper Garbage Collection(HKGC) queues for page compression	          
		String HkgcReqRec          = ""; // Total number of events that were queued in Housekeeper Garbage Collection(HKGC) queues from UNDO	                      
		String HkgcRetries         = ""; // Total number of times we tried for queued items.	                                                                      
		String nl_1570_sp136       = "";
//		if ( (srvVersion >= Ver.ver(15,7,0, 136) && srvVersion < Ver.ver(16,0)) )
		if ( (srvVersion >= Ver.ver(15,7,0, 136) && srvVersion < Ver.ver(16,0)) || srvVersion >= Ver.ver(16,0,0, 2,2) )
		{
			HkgcOverflowsCmpact = "HkgcOverflowsCmpact, ";
			HkgcOverflowsRec    = "HkgcOverflowsRec, ";
			LotsDelRolXacts     = "LotsDelRolXacts, ";
			LotsDelCmtXacts     = "LotsDelCmtXacts, ";
			LongXacts           = "LongXacts, ";
			PostcommitPageCount = "PostcommitPageCount, ";
			HkgcReq             = "HkgcReq, ";
			HkgcReqCmpact       = "HkgcReqCmpact, ";
			HkgcReqDcomp        = "HkgcReqDcomp, ";
			HkgcReqRec          = "HkgcReqRec, ";
			HkgcRetries         = "HkgcRetries, ";
			nl_1570_sp136       = "\n";
		}


		String NonIdleCPUTimePct = "NonIdleCPUTimePct = CASE WHEN CPUTime > 0 \n"+
			       		           "                         THEN convert(numeric(10,1), (("+NonIdleCPUTime_calc+" + 0.0) / (CPUTime + 0.0)) * 100.0 ) \n" + 
			       		           "                         ELSE convert(numeric(10,1), 0.0 ) \n" +
			       		           "                    END, \n";
		String SystemCPUTimePct  = "SystemCPUTimePct  = CASE WHEN CPUTime > 0 \n" + 
		                           "                         THEN convert(numeric(10,1), ((SystemCPUTime + 0.0) / (CPUTime + 0.0)) * 100.0 ) \n" +
		                           "                         ELSE convert(numeric(10,1), 0.0 )  \n" +
		                           "                    END,  \n";
		String UserCPUTimePct    = "UserCPUTimePct    = CASE WHEN CPUTime > 0   \n" +
		                           "                         THEN convert(numeric(10,1), ((UserCPUTime + 0.0) / (CPUTime + 0.0)) * 100.0 )   \n" +
		                           "                         ELSE convert(numeric(10,1), 0.0 )   \n" +
		                           "                    END,   \n";
		String IOCPUTimePct      = "";
		String IdleCPUTimePct    = "IdleCPUTimePct    = CASE WHEN CPUTime > 0   \n" +
		                           "                         THEN convert(numeric(10,1), ((IdleCPUTime + 0.0) / (CPUTime + 0.0)) * 100.0 )   \n" +
		                           "                         ELSE convert(numeric(10,1), 0.0 )   \n" +
		                           "                    END,  \n";

//		if (srvVersion >= 1550000 || (srvVersion >= 1503000 && isClusterEnabled) )
		if (srvVersion >= Ver.ver(15,5) || (srvVersion >= Ver.ver(15,0,3) && isClusterEnabled) )
		{
			IOCPUTime            = "IOCPUTime, ";
			IOCPUTimePct         = "IOCPUTimePct      = CASE WHEN CPUTime > 0   \n" +
			                       "                         THEN convert(numeric(10,1), ((IOCPUTime + 0.0) / (CPUTime + 0.0)) * 100.0 )   \n" +
			                       "                         ELSE convert(numeric(10,1), 0.0 )   \n" +
			                       "                    END,   \n";
		}

		if (srvVersion >= Ver.ver(15,7) && inThreadedMode() && _collapse_IoCpuTime_to_IdleCpuTime)
		{
			IOCPUTime    = "IOCPUTime    = convert(int, -1), \n";
			IOCPUTimePct = "IOCPUTimePct = convert(numeric(10,1), -1.0), \n";
			IdleCPUTime  = "IdleCPUTime  = IdleCPUTime + IOCPUTime, ";
		}
		
		cols1 += "EngineNumber, CurrentKPID, PreviousKPID, CPUTime, SystemCPUTime, UserCPUTime, \n";
		cols1 += IOCPUTime + IdleCPUTime + "\n" +
				NonIdleCPUTimePct + SystemCPUTimePct + UserCPUTimePct + IOCPUTimePct + IdleCPUTimePct +
				"ContextSwitches, Connections, \n";

		cols2 += "";
		cols3 += "ProcessesAffinitied, Status, StartTime, StopTime, AffinitiedToCPU, "+ThreadID+"OSPID";

		if (srvVersion >= Ver.ver(12,5,3,2))
		{
			cols2 += "Yields, DiskIOChecks, DiskIOPolled, DiskIOCompleted, \n";
		}
		if (srvVersion >= Ver.ver(15,0,2,5))
		{
			cols2 += "MaxOutstandingIOs, ";
		}
		if (srvVersion >= Ver.ver(15,0))
		{
			cols2 += "HkgcMaxQSize, HkgcPendingItems, HkgcHWMItems, HkgcOverflows, \n";
		}
		cols2 += HkgcPendingItemsDcomp + HkgcOverflowsDcomp + nl_15701;

		// New Columns for ASE 15.7 SP136, and above 16.0 SP2 PL2
		cols2 += HkgcOverflowsCmpact + HkgcOverflowsRec + LotsDelRolXacts + LotsDelCmtXacts + LongXacts + nl_1570_sp136 +
				PostcommitPageCount + HkgcReq + HkgcReqCmpact + HkgcReqDcomp + HkgcReqRec + HkgcRetries + nl_1570_sp136;

		String sql = 
			"select " + cols1 + cols2 + cols3 + "\n" +
			"from master..monEngine \n" +
			"where Status = 'online' \n" +
			"order by 1,2\n";

		return sql;
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		long srvVersion = getServerVersion();
		// NOTE: IOCPUTime was introduced in ASE 15.5

		if (GRAPH_NAME_CPU_SUM.equals(tgdp.getName()))
		{
			Double[] dataArray  = new Double[3];
			String[] labelArray = new String[3];
			if (srvVersion >= Ver.ver(15,5))
			{
				if (srvVersion >= Ver.ver(15,7) && inThreadedMode() && _collapse_IoCpuTime_to_IdleCpuTime)
				{
					// dummy for easier logic (multiple negation are hard to understand)
				}
				else
				{
					dataArray  = new Double[4];
					labelArray = new String[4];
				}
			}

			labelArray[0] = "System+User CPU";
			labelArray[1] = "System CPU";
			labelArray[2] = "User CPU";

//			dataArray[0] = this.getDiffValueAvg("CPUTime");
//			dataArray[1] = this.getDiffValueAvg("SystemCPUTime");
//			dataArray[2] = this.getDiffValueAvg("UserCPUTime");
			dataArray[0] = Math.min(100.0, this.getDiffValueAvg("NonIdleCPUTimePct"));
			dataArray[1] = Math.min(100.0, this.getDiffValueAvg("SystemCPUTimePct"));
			dataArray[2] = Math.min(100.0, this.getDiffValueAvg("UserCPUTimePct"));

			if (srvVersion >= Ver.ver(15,5))
			{
				if (srvVersion >= Ver.ver(15,7) && inThreadedMode() && _collapse_IoCpuTime_to_IdleCpuTime)
				{
					// dummy for easier logic (multiple negation are hard to understand)
					if (_logger.isDebugEnabled())
						_logger.debug("updateGraphData(cpuSum): NonIdleCPUTimePct='"+dataArray[0]+"', SystemCPUTimePct='"+dataArray[1]+"', UserCPUTimePct='"+dataArray[2]+"'.");
				}
				else
				{
    				labelArray[0] = "System+User+IO CPU";
    
//    				dataArray[3]  = this.getDiffValueAvg("IOCPUTime");
    				dataArray[3]  = Math.min(100.0, this.getDiffValueAvg("IOCPUTimePct"));
    				labelArray[3] = "IO CPU";
    
    				if (_logger.isDebugEnabled())
    					_logger.debug("updateGraphData(cpuSum): NonIdleCPUTimePct='"+dataArray[0]+"', SystemCPUTimePct='"+dataArray[1]+"', UserCPUTimePct='"+dataArray[2]+"', IOCPUTimePct='"+dataArray[3]+"'.");
				}
			}
			else
			{
				if (_logger.isDebugEnabled())
					_logger.debug("updateGraphData(cpuSum): NonIdleCPUTimePct='"+dataArray[0]+"', SystemCPUTimePct='"+dataArray[1]+"', UserCPUTimePct='"+dataArray[2]+"'.");
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), labelArray, dataArray);
		}

		if (GRAPH_NAME_CPU_ENG.equals(tgdp.getName()))
		{
			// Set label on the TrendGraph if we are above 15.5
			if (srvVersion >= Ver.ver(15,5))
			{
				if (srvVersion >= Ver.ver(15,7) && inThreadedMode() && _collapse_IoCpuTime_to_IdleCpuTime)
				{
    				TrendGraph tg = getTrendGraph(tgdp.getName());
    				if (tg != null)
    					tg.setChartLabel("CPU Usage per Engine (System + User)");
				}
				else
				{
    				TrendGraph tg = getTrendGraph(tgdp.getName());
    				if (tg != null)
    					tg.setChartLabel("CPU Usage per Engine (System + User + IO)");
				}
			}
			
			Double[] engCpuArray = new Double[this.size()];
			String[] engNumArray = new String[engCpuArray.length];
			for (int i = 0; i < engCpuArray.length; i++)
			{
				String instanceId   = null;
				if (isClusterEnabled())
					instanceId = this.getAbsString(i, "InstanceID");
				String engineNumber = this.getAbsString(i, "EngineNumber");

//				engCpuArray[i] = this.getDiffValueAsDouble(i, "CPUTime");
				engCpuArray[i] = Math.min(100.0, this.getDiffValueAsDouble(i, "NonIdleCPUTimePct"));
				if (instanceId == null)
					engNumArray[i] = "eng-" + engineNumber;
				else
					engNumArray[i] = instanceId + ":" + "eng-" + engineNumber;
				
				// in Cluster Edition the labels will look like 'eng-{InstanceId}:{EngineNumber}'
				// in ASE SMP Version the labels will look like 'eng-{EngineNumber}'
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), engNumArray, engCpuArray);
		}
	}


	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public List<CmSettingsHelper> getLocalSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		
		list.add(new CmSettingsHelper("Callapse IOCPUTime into IdleCPUTime", PROPKEY_collapse_IoCpuTime_to_IdleCpuTime , Boolean.class, conf.getBooleanProperty(PROPKEY_collapse_IoCpuTime_to_IdleCpuTime , DEFAULT_collapse_IoCpuTime_to_IdleCpuTime ), DEFAULT_collapse_IoCpuTime_to_IdleCpuTime, CmEnginesPanel.TOOLTIP_collapse_IoCpuTime_to_IdleCpuTime ));

		return list;
	}


	/** 
	 * Compute the CPU times in pct instead of numbers of usage seconds since last sample
	 */
	@Override
	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
	{
		long srvVersion = getServerVersion();
		// NOTE: IOCPUTime was introduced in ASE 15.5
		
		// Compute the avgServ column, which is IOTime/(Reads+APFReads+Writes)
		int CPUTime,                    SystemCPUTime,             UserCPUTime,             IOCPUTime,             IdleCPUTime;
		int CPUTime_pos           = -1, SystemCPUTime_pos    = -1, UserCPUTime_pos    = -1, IOCPUTime_pos    = -1, IdleCPUTime_pos    = -1;
		int NonIdleCPUTimePct_pos = -1, SystemCPUTimePct_pos = -1, UserCPUTimePct_pos = -1, IOCPUTimePct_pos = -1, IdleCPUTimePct_pos = -1;
	
		// Find column Id's
		List<String> colNames = diffData.getColNames();
		if (colNames==null) return;
	
		for (int colId=0; colId < colNames.size(); colId++) 
		{
			String colName = colNames.get(colId);
			if      (colName.equals("CPUTime"))           CPUTime_pos           = colId;
			else if (colName.equals("SystemCPUTime"))     SystemCPUTime_pos     = colId;
			else if (colName.equals("UserCPUTime"))       UserCPUTime_pos       = colId;
			else if (colName.equals("IOCPUTime"))         IOCPUTime_pos         = colId;
			else if (colName.equals("IdleCPUTime"))       IdleCPUTime_pos       = colId;

			else if (colName.equals("NonIdleCPUTimePct")) NonIdleCPUTimePct_pos = colId;
			else if (colName.equals("SystemCPUTimePct"))  SystemCPUTimePct_pos  = colId;
			else if (colName.equals("UserCPUTimePct"))    UserCPUTimePct_pos    = colId;
			else if (colName.equals("IOCPUTimePct"))      IOCPUTimePct_pos      = colId;
			else if (colName.equals("IdleCPUTimePct"))    IdleCPUTimePct_pos    = colId;
		}

		// Loop on all diffData rows
		for (int rowId=0; rowId < diffData.getRowCount(); rowId++) 
		{
			CPUTime	=       ((Number)diffData.getValueAt(rowId, CPUTime_pos      )).intValue();
			SystemCPUTime = ((Number)diffData.getValueAt(rowId, SystemCPUTime_pos)).intValue();
			UserCPUTime =   ((Number)diffData.getValueAt(rowId, UserCPUTime_pos  )).intValue();
			IdleCPUTime =   ((Number)diffData.getValueAt(rowId, IdleCPUTime_pos  )).intValue();

			IOCPUTime = 0;
//			if (srvVersion >= 1550000)
			if (srvVersion >= Ver.ver(15,5))
			{
				IOCPUTime = ((Number)diffData .getValueAt(rowId, IOCPUTime_pos  )).intValue();
			}

			if (_logger.isDebugEnabled())
				_logger.debug("----CPUTime = "+CPUTime+", SystemCPUTime = "+SystemCPUTime+", UserCPUTime = "+UserCPUTime+", IOCPUTime = "+IOCPUTime+", IdleCPUTime = "+IdleCPUTime);

			// Handle divided by 0... (this happens if a engine goes offline
			BigDecimal calcCPUTime       = null;
			BigDecimal calcSystemCPUTime = null;
			BigDecimal calcUserCPUTime   = null;
			BigDecimal calcIoCPUTime     = null;
			BigDecimal calcIdleCPUTime   = null;

			if( CPUTime == 0 )
			{
				calcCPUTime       = new BigDecimal( 0 );
				calcSystemCPUTime = new BigDecimal( 0 );
				calcUserCPUTime   = new BigDecimal( 0 );
				calcIoCPUTime     = new BigDecimal( 0 );
				calcIdleCPUTime   = new BigDecimal( 0 );
			}
			else
			{
				int sumSystemUserIo = SystemCPUTime + UserCPUTime + IOCPUTime;
				calcCPUTime       = new BigDecimal( ((1.0 * sumSystemUserIo) / CPUTime) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
				calcSystemCPUTime = new BigDecimal( ((1.0 * SystemCPUTime  ) / CPUTime) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
				calcUserCPUTime   = new BigDecimal( ((1.0 * UserCPUTime    ) / CPUTime) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
				calcIoCPUTime     = new BigDecimal( ((1.0 * IOCPUTime      ) / CPUTime) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
				calcIdleCPUTime   = new BigDecimal( ((1.0 * IdleCPUTime    ) / CPUTime) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
			}

			if (_logger.isDebugEnabled())
				_logger.debug("++++calc:CPUTime = "+calcCPUTime+", calc:SystemCPUTime = "+calcSystemCPUTime+", calc:UserCPUTime = "+calcUserCPUTime+", calc:IoCPUTime = "+calcIoCPUTime+", calc:IdleCPUTime = "+calcIdleCPUTime);
	
			diffData.setValueAt(calcCPUTime,       rowId, NonIdleCPUTimePct_pos );
			diffData.setValueAt(calcSystemCPUTime, rowId, SystemCPUTimePct_pos  );
			diffData.setValueAt(calcUserCPUTime,   rowId, UserCPUTimePct_pos    );
			diffData.setValueAt(calcIdleCPUTime,   rowId, IdleCPUTimePct_pos    );
	
			if (srvVersion >= Ver.ver(15,5))
			{
				diffData.setValueAt(calcIoCPUTime, rowId, IOCPUTimePct_pos);

				if (srvVersion >= Ver.ver(15,7) && inThreadedMode() && _collapse_IoCpuTime_to_IdleCpuTime)
				{
					diffData.setValueAt(Integer   .valueOf( -1 ), rowId, IOCPUTime_pos);
					diffData.setValueAt(BigDecimal.valueOf( -1 ), rowId, IOCPUTimePct_pos);
				}
			}
		}
	}
	
	/**
	 * Local adjustments to the rate values 
	 */
	@Override
	public void localCalculationRatePerSec(CounterSample rateData, CounterSample DiffData)
	{
		long srvVersion = getServerVersion();

		List<String> colNames = rateData.getColNames();
		if (colNames==null) 
			return;

		int IOCPUTime_pos    = -1;
		int IOCPUTimePct_pos = -1;

		// Get column position
		for (int colId=0; colId < colNames.size(); colId++) 
		{
			String colName = colNames.get(colId);
			if      (colName.equals("IOCPUTime"))         IOCPUTime_pos         = colId;
			else if (colName.equals("IOCPUTimePct"))      IOCPUTimePct_pos      = colId;
		}

		// Loop on all rateData rows
		for (int rowId=0; rowId < rateData.getRowCount(); rowId++) 
		{
			if (srvVersion >= Ver.ver(15,7) && inThreadedMode() && _collapse_IoCpuTime_to_IdleCpuTime)
			{
				rateData.setValueAt(Integer   .valueOf( -1 ), rowId, IOCPUTime_pos);
				rateData.setValueAt(BigDecimal.valueOf( -1 ), rowId, IOCPUTimePct_pos);
			}
		}
	}

	@Override
	public void sendAlarmRequest()
	{
		if ( ! hasRateData() )
			return;

		if ( ! AlarmHandler.hasInstance() )
			return;

		CountersModel cm = this; // Do this only to make it easier to "copy" this code into a User Defined Interrogater

		boolean debugPrint = Configuration.getCombinedConfiguration().getBooleanProperty("sendAlarmRequest.debug", _logger.isDebugEnabled());

		//-------------------------------------------------------
		// CPU Usage
		//-------------------------------------------------------
//		if (isSystemAlarmsForColumnEnabled("CPUTime"))
//		{
//			Double IdleCPUTimePct = cm.getRateValueAvg("IdleCPUTimePct");
//
//			if (IdleCPUTimePct != null)
//			{
//				Double cpuUsagePct = 100.0 - IdleCPUTimePct;
//
//				if (debugPrint || _logger.isDebugEnabled())
//					System.out.println("##### sendAlarmRequest("+cm.getName()+"): cpuUsagePct='"+cpuUsagePct+"'.");
//
//				if (AlarmHandler.hasInstance())
//				{
//					int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_CPUTime, DEFAULT_alarm_CPUTime);
//					if (cpuUsagePct.intValue() > threshold)
//						AlarmHandler.getInstance().addAlarm( new AlarmEventHighCpuUtilization(cm, cpuUsagePct) );
//				}
//			}
//		}
		if (isSystemAlarmsForColumnEnabledAndInTimeRange("CPUTime"))
		{
			Double NonIdleCPUTimePct = MathUtils.round( cm.getRateValueAvg("NonIdleCPUTimePct"), 1);
			Double SystemCPUTimePct  = MathUtils.round( cm.getRateValueAvg("SystemCPUTimePct"),  1);
			Double UserCPUTimePct    = MathUtils.round( cm.getRateValueAvg("UserCPUTimePct"),    1);
//			Double IOCPUTimePct      = MathUtils.round( cm.getRateValueAvg("IOCPUTimePct"),      1);
			Double IdleCPUTimePct    = MathUtils.round( cm.getRateValueAvg("IdleCPUTimePct"),    1);

			if (NonIdleCPUTimePct != null)
			{
				int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_CPUTime, DEFAULT_alarm_CPUTime);
				
				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest("+cm.getName()+"): threshold="+threshold+", NonIdleCPUTimePct='"+NonIdleCPUTimePct+"'.");

				if (NonIdleCPUTimePct.intValue() > threshold)
				{
					String extendedDescText = "";
					String extendedDescHtml = cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_CPU_SUM);

					AlarmEvent ae = new AlarmEventHighCpuUtilization(cm, threshold, CpuType.TOTAL_CPU, NonIdleCPUTimePct, UserCPUTimePct, SystemCPUTimePct, IdleCPUTimePct);
					ae.setExtendedDescription(extendedDescText, extendedDescHtml);

					AlarmHandler.getInstance().addAlarm(ae);
				}
			}
		}
	}

	@Override
	public boolean isGraphDataHistoryEnabled(String name)
	{
		// ENABLED for the following graphs
		if (GRAPH_NAME_CPU_SUM.equals(name)) return true;

		// default: DISABLED
		return false;
	}
	@Override
	public int getGraphDataHistoryTimeInterval(String name)
	{
		// Keep interval: default is 60 minutes
		return super.getGraphDataHistoryTimeInterval(name);
	}


	public static final String  PROPKEY_alarm_CPUTime             = CM_NAME + ".alarm.system.if.CPUTime.gt";
	public static final int     DEFAULT_alarm_CPUTime             = 90;
	
	
	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		
		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;
		
		list.add(new CmSettingsHelper("CPUTime", isAlarmSwitch, PROPKEY_alarm_CPUTime, Integer.class, conf.getIntProperty(PROPKEY_alarm_CPUTime             , DEFAULT_alarm_CPUTime             ), DEFAULT_alarm_CPUTime            , "If 'CPUTime' is greater than ## then send 'AlarmEventHighCpuUtilization'." ));

		return list;
	}
}
