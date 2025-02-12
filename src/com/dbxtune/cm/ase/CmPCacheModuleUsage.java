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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.alarm.AlarmHandler;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.alarm.events.AlarmEventStatementCacheAboveConfig;
import com.dbxtune.central.pcs.CentralPersistReader;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.ase.gui.CmPCacheModuleUsagePanel;
import com.dbxtune.graph.TrendGraphDataPoint;
import com.dbxtune.graph.TrendGraphDataPoint.LabelType;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.sql.conn.info.DbmsVersionInfoSybaseAse;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmPCacheModuleUsage
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmPCacheModuleUsage.class.getSimpleName();
	public static final String   SHORT_NAME       = "Proc Cache Module Usage";
	public static final String   HTML_DESC        = 
		"<html>" +
		"What module of the ASE Server is using the 'procedure cache' or 'dynamic memory pool'" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_CACHE;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

//	public static final long     NEED_SRV_VERSION = 15010;
//	public static final long     NEED_SRV_VERSION = 1501000;
	public static final long     NEED_SRV_VERSION = Ver.ver(15,0,1);
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"monProcedureCacheModuleUsage"};
	public static final String[] NEED_ROLES       = new String[] {"mon_role"};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {"ActiveDiff", "ActiveDiffMb", "NumPagesReused"};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = false;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.LARGE; }

	private int     _refreshAseConfigThresholdInSec  = 3600;  // refresh ASE Configuration every X second
	private long    _lastAseConfigRefresh = 0;     // When was the ASE config last refreshed

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '"+CM_NAME+"'");

		return new CmPCacheModuleUsage(counterController, guiController);
	}

	public CmPCacheModuleUsage(ICounterController counterController, IGuiController guiController)
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
	
	public static final String GRAPH_NAME_MODULE_USAGE    = "Graph";   //String x=GetCounters.CM_GRAPH_NAME__PROC_CACHE_MODULE_USAGE__ABS_USAGE;
	public static final String GRAPH_NAME_MODULE_USAGE_MB = "GraphMb"; //String x=GetCounters.CM_GRAPH_NAME__PROC_CACHE_MODULE_USAGE__ABS_USAGE;

	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_MODULE_USAGE,
			"Procedure Cache Module Usage", 	                                 // Menu CheckBox text
			"Procedure Cache Module Usage (in page count) ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null,
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.SRV_CONFIG, // or CACHE
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			300);  // minimum height

		addTrendGraph(GRAPH_NAME_MODULE_USAGE_MB,
			"Procedure Cache Module Usage", 	                                 // Menu CheckBox text
			"Procedure Cache Module Usage (in MB) ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null,
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.SRV_CONFIG, // or CACHE
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			300);  // minimum height

//		// if GUI
//		if (getGuiController() != null && getGuiController().hasGUI())
//		{
//			// GRAPH
//			TrendGraph tg = null;
//			tg = new TrendGraph(GRAPH_NAME_MODULE_USAGE,
//				"Procedure Cache Module Usage", 	                                 // Menu CheckBox text
//				"Procedure Cache Module Usage (in page count) ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
//				labels, 
//				false, // is Percent Graph
//				this, 
//				false, // visible at start
//				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//				300);  // minimum height
//			addTrendGraph(tg.getName(), tg, true);
//		}
	}

	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmPCacheModuleUsagePanel(this);
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

		pkCols.add("ModuleID");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		DbmsVersionInfoSybaseAse aseVersionInfo = (DbmsVersionInfoSybaseAse) versionInfo;
		long    srvVersion       = aseVersionInfo.getLongVersion();
		boolean isClusterEnabled = aseVersionInfo.isClusterEdition();

		String InstanceID = "";
		if (isClusterEnabled && srvVersion >= Ver.ver(15,5))
		{
			InstanceID = "    InstanceID, \n";
		}

//		String sql = 
//			"select "+InstanceID+"ModuleName, ModuleID, Active, ActiveDiff = Active, HWM, NumPagesReused \n" +
//			"from master..monProcedureCacheModuleUsage\n" +
//			"order by ModuleID";
		String sql = 
			"select \n" + 
			InstanceID + 
			"    ModuleName, \n" + 
			"    ModuleID, \n" + 
			"    Active, \n" + 
			"    ActiveDiff   = Active, \n" + 
			"    ActiveMb     = convert(numeric(8,1), Active / 512.0), \n" + 
			"    ActiveDiffMb = convert(numeric(8,1), Active / 512.0), \n" + 
			"    HWM, \n" + 
			"    HwmMb        = convert(numeric(8,1), HWM / 512.0), \n" + 
			"    NumPagesReused \n" + 
			"from master.dbo.monProcedureCacheModuleUsage \n" + 
			"order by ModuleID \n" + 
			"";
				

		return sql;
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		if (GRAPH_NAME_MODULE_USAGE.equals(tgdp.getName()))
		{
			// Write 1 "line" for every row
			Double[] dArray = new Double[this.size()];
			String[] lArray = new String[dArray.length];
			for (int i = 0; i < dArray.length; i++)
			{
				lArray[i] = this.getAbsString       (i, "ModuleName");
				dArray[i] = this.getAbsValueAsDouble(i, "Active");
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
		}

		if (GRAPH_NAME_MODULE_USAGE_MB.equals(tgdp.getName()))
		{
			// Write 1 "line" for every row
			Double[] dArray = new Double[this.size()];
			String[] lArray = new String[dArray.length];
			for (int i = 0; i < dArray.length; i++)
			{
				lArray[i] = this.getAbsString       (i, "ModuleName");
				dArray[i] = this.getAbsValueAsDouble(i, "ActiveMb");
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
		}
	}


	/** 'statement cache size' run value, on init */
	private int _statementCacheConfigSizeMb = -1;
	
	/** 'procedure cache size' run value, on init */
	private int _procedureCacheConfigSizeMb = -1;
	
	@Override
	public boolean doSqlInit(DbxConnection conn)
	{
		boolean superRc = super.doSqlInit(conn);

		getAseConfig(conn, "initializing");
		
		return superRc;
	}
	@Override
	public void doSqlClose(DbxConnection conn)
	{
		// Reset value
		_statementCacheConfigSizeMb = -1;
		_procedureCacheConfigSizeMb = -1;
		
		super.doSqlClose(conn);
	}
	
	public void getAseConfig(DbxConnection conn, String calledFrom)
	{
		String sql     = "";
		String cfgName = "";

		//------------------------------------------------
		// Get 'statement cache size' RUN Size on init.
		cfgName = "statement cache size";
		sql = "select runValueInMb = isnull(value/512, -1) from master.dbo.sysconfigures where config = (select config from master.dbo.sysconfigures where comment = '"+cfgName+"')";
		try( Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql) )
		{
			while(rs.next())
				_statementCacheConfigSizeMb = rs.getInt(1);

			_logger.info("When "+calledFrom+" '"+getName()+"', Succeed get run value for ASE configuration '"+cfgName+"' = "+_statementCacheConfigSizeMb + " MB");
		}
		catch (SQLException ex)
		{
			_statementCacheConfigSizeMb = -1;
			_logger.warn("When "+calledFrom+" '"+getName()+"', failed to get ASE configuration '"+cfgName+"', continuing anyway. sql=|"+sql+"|, Caught: "+ex);
		}
		
		//------------------------------------------------
		// Get 'procedure cache size' RUN Size on init.
		cfgName = "procedure cache size";
		sql = "select runValueInMb = isnull(value/512, -1) from master.dbo.sysconfigures where config = (select config from master.dbo.sysconfigures where comment = '"+cfgName+"')";
		try( Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql) )
		{
			while(rs.next())
				_procedureCacheConfigSizeMb = rs.getInt(1);

			_logger.info("When "+calledFrom+" '"+getName()+"', Succeed get run value for ASE configuration '"+cfgName+"' = "+_procedureCacheConfigSizeMb + " MB");
		}
		catch (SQLException ex)
		{
			_procedureCacheConfigSizeMb = -1;
			_logger.warn("When "+calledFrom+" '"+getName()+"', failed to get ASE configuration '"+cfgName+"', continuing anyway. sql=|"+sql+"|, Caught: "+ex);
		}

		_lastAseConfigRefresh = System.currentTimeMillis();
	}

	@Override
	public void sendAlarmRequest()
	{
		if ( ! hasAbsData() )
			return;
		
		if ( ! AlarmHandler.hasInstance() )
			return;

		// Should we refresh ASE Configuration ???
		long lastRefreshAgeInSec = (System.currentTimeMillis() - _lastAseConfigRefresh) / 1000;
		if ( lastRefreshAgeInSec > _refreshAseConfigThresholdInSec)
		{
			_logger.info("Time to refresh ASE Configuration. refreshAgeInSec="+lastRefreshAgeInSec+", thresholdInSec="+_refreshAseConfigThresholdInSec);

			// refresh configuration
			DbxConnection conn = getCounterController().getMonConnection();
			getAseConfig(conn, "send-alarm-refresh-config");
		}

		// IF StatementCache isn't enabled... then there is nothing to do here...
		if (_statementCacheConfigSizeMb <= 0)
			return;
			
		CountersModel cm = this;
		
		boolean debugPrint = Configuration.getCombinedConfiguration().getBooleanProperty("sendAlarmRequest.debug", _logger.isDebugEnabled());
//debugPrint = true;

		// Get a array of rowId's where the column 'Name' has the value 'procedure cache size'
		int[] rqRows = this.getAbsRowIdsWhere("ModuleName", "Statement Cache");
		if (rqRows == null)
			_logger.warn("When checking for alarms in '"+getName()+"', getAbsRowIdsWhere('ModuleName', 'Statement Cache'), returned null, so I can't do more here.");
		else
		{
			//-------------------------------------------------------
			// Statement Cache Usage
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("StatementCacheUsage"))
			{
				Double activeMb = this.getAbsValueAsDouble(rqRows[0], "ActiveMb");

				if (activeMb != null)
				{
					Double stmntCachePctUsed = activeMb / _statementCacheConfigSizeMb * 100.0;
					Double procCachePctUsed  = activeMb / _procedureCacheConfigSizeMb * 100.0;
					
					int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_StatementCacheUsagePct, DEFAULT_alarm_StatementCacheUsagePct);

					if (debugPrint || _logger.isDebugEnabled())
						System.out.println("##### sendAlarmRequest("+cm.getName()+"): threshold="+threshold+", _statementCacheConfigSizeMb="+_statementCacheConfigSizeMb+", activeMb="+activeMb+", stmntCachePctUsed="+stmntCachePctUsed+", procCachePctUsed="+procCachePctUsed+".");

					if (stmntCachePctUsed.intValue() > threshold)
					{
						String extendedDescText = "";
						String extendedDescHtml = cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_MODULE_USAGE_MB);

						AlarmEvent ae = new AlarmEventStatementCacheAboveConfig(cm, _statementCacheConfigSizeMb, activeMb.intValue(), stmntCachePctUsed, procCachePctUsed, threshold);
						ae.setExtendedDescription(extendedDescText, extendedDescHtml);

						AlarmHandler.getInstance().addAlarm(ae);
					}
				}
			}
		}
	} // end: method

	@Override
	public boolean isGraphDataHistoryEnabled(String name)
	{
		// ENABLED for the following graphs
		if (GRAPH_NAME_MODULE_USAGE_MB.equals(name)) return true;

		// default: DISABLED
		return false;
	}
	@Override
	public int getGraphDataHistoryTimeInterval(String name)
	{
		// Keep interval: default is 60 minutes
		return super.getGraphDataHistoryTimeInterval(name);
	}

	
	public static final String  PROPKEY_alarm_StatementCacheUsagePct = CM_NAME + ".alarm.system.if.StatementCacheUsagePct.gt";
	public static final int     DEFAULT_alarm_StatementCacheUsagePct = 120;
	
	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		
		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;
		
		list.add(new CmSettingsHelper("StatementCacheUsagePct", isAlarmSwitch, PROPKEY_alarm_StatementCacheUsagePct, Integer.class, conf.getIntProperty(PROPKEY_alarm_StatementCacheUsagePct, DEFAULT_alarm_StatementCacheUsagePct), DEFAULT_alarm_StatementCacheUsagePct, "If 'StatementCacheUsagePct' is greater than ### Percent (example above 120% == something is wrong) then send 'AlarmEventStatementCacheAboveConfig'." ));

		return list;
	}

}
