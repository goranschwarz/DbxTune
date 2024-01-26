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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.alarm.AlarmHandler;
import com.asetune.alarm.events.AlarmEvent;
import com.asetune.alarm.events.rs.AlarmEventRsSdUsage;
import com.asetune.central.pcs.CentralPersistReader;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.graph.TrendGraphDataPoint.LabelType;
import com.asetune.gui.MainFrame;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.conn.info.DbmsVersionInfo;
import com.asetune.utils.Configuration;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmAdminDiskSpace
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmAdminDiskSpace.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmAdminDiskSpace.class.getSimpleName();
	public static final String   SHORT_NAME       = "Partitions";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<p>Replication Server Stable Queueu Partitions Information</p>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_DISK;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"disk_space"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"Used Segs"
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

		return new CmAdminDiskSpace(counterController, guiController);
	}

	public CmAdminDiskSpace(ICounterController counterController, IGuiController guiController)
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
	public static final String GRAPH_NAME_QUEUE_SIZE      = "SdQueueSize";
	public static final String GRAPH_NAME_QUEUE_USAGE_PCT = "SdQueueUsagePct";

	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_QUEUE_SIZE,
			"Stable Device Usage in MB, from 'admin disk_space' (col 'Used Segs', Absolute Value)", // Menu CheckBox text
			"Stable Device Usage in MB, from 'admin disk_space' (col 'Used Segs', Absolute Value)", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.SPACE,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_QUEUE_USAGE_PCT,
			"Stable Device Usage in PCT, for all Partitions", // Menu CheckBox text
			"Stable Device Usage in PCT, for all Partitions", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERCENT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] {"Percent Used"}, 
			LabelType.Static,
			TrendGraphDataPoint.Category.SPACE,
			true,  // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		if (GRAPH_NAME_QUEUE_SIZE.equals(tgdp.getName()))
		{
			// Write 1 "line" for every device
			Double[] dArray = new Double[this.size() + 1];
			String[] lArray = new String[dArray.length];

			lArray[0] = "Sum-All-Devices";
			dArray[0] = this.getAbsValueSum("Used Segs");

			for (int i = 0; i < this.size(); i++)
			{
				lArray[i+1] = this.getAbsString       (i, "Logical");
				dArray[i+1] = this.getAbsValueAsDouble(i, "Used Segs");
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
		}

		if (GRAPH_NAME_QUEUE_USAGE_PCT.equals(tgdp.getName()))
		{
			// Write 1 "line" for every device
			Double[] dArray = new Double[1];

			Double TotalSegs = this.getAbsValueSum("Total Segs");
			Double UsedSegs  = this.getAbsValueSum("Used Segs");
			
			if (TotalSegs == null || UsedSegs == null)
				return;
			
			BigDecimal pct = new BigDecimal(UsedSegs / TotalSegs).setScale(2, BigDecimal.ROUND_HALF_EVEN);
			dArray[0] = pct.doubleValue();

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), dArray);
		}
	}
	
//	1> admin disk_space
//	+--------------------------------+-------+-------+----------+---------+----------+
//	|Partition                       |Logical|Part.Id|Total Segs|Used Segs|State     |
//	+--------------------------------+-------+-------+----------+---------+----------+
//	|c:\sybase\devices\GORAN_1_RS.sd1|sd1    |101    |200       |0        |ON-LINE///|
//	+--------------------------------+-------+-------+----------+---------+----------+
//	Rows 1
//	(1 rows affected)

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
			mtd.addTable("disk_space",  "");

			mtd.addColumn("disk_space", "Partition",     "<html>Physical name of the Partition</html>");
			mtd.addColumn("disk_space", "Logical",       "<html>Physical name of the Partition</html>");
			mtd.addColumn("disk_space", "Part.Id",       "<html>ID of the Partition</html>");
			mtd.addColumn("disk_space", "Total Segs",    "<html>How many MB does this Partition hold</html>");
			mtd.addColumn("disk_space", "Used Segs",     "<html>Number of <b>used</b> segments in MB<br>Note 1: 0 means it has never been used<br>Note 2: If it has ever been used it can not go below 1, even if it's empty.</html>");
			mtd.addColumn("disk_space", "State",         "<html>In what <i>state</i> the Partition is in.<br>"
					+ "<ul>"
					+ "  <li>ON-LINE  � The device is normal</li>"
					+ "  <li>OFF-LINE � The device cannot be found</li>"
					+ "  <li>DROPPED  � The device has been dropped but has not disappeared (some queues are still using it)</li>"
					+ "  <li>AUTO     � The device is automatically resizable. See Automatically Resizable Partitions in the Replication Server Administration Guide Volume 1.</li>"
					+ "</ul>"
					+ "</html>");
		}
		catch (NameNotFoundException e) 
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
		//	System.out.println("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
		}
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("Partition");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		String sql = "admin disk_space"; // see below on admin disk_space, MB (which adds 2 columns... but I don't know what version of RS supports that. 15.7.1 SP 305 has it)
		return sql;
	}
	/*
	1> admin disk_space
	RS> Col# Label      JDBC Type Name         Guessed DBMS type Source Table
	RS> ---- ---------- ---------------------- ----------------- ------------
	RS> 1    Partition  java.sql.Types.VARCHAR varchar(255)      -none-      
	RS> 2    Logical    java.sql.Types.VARCHAR varchar(31)       -none-      
	RS> 3    Part.Id    java.sql.Types.INTEGER int               -none-      
	RS> 4    Total Segs java.sql.Types.INTEGER int               -none-      
	RS> 5    Used Segs  java.sql.Types.INTEGER int               -none-      
	RS> 6    State      java.sql.Types.VARCHAR varchar(31)       -none-      
	+-------------------------------+-------+-------+----------+---------+----------+
	|Partition                      |Logical|Part.Id|Total Segs|Used Segs|State     |
	+-------------------------------+-------+-------+----------+---------+----------+
	|/sybdev/devices/rs/PROD_REP.sd1|sd1    |101    |16384     |10       |ON-LINE///|
	+-------------------------------+-------+-------+----------+---------+----------+
	(1 rows affected)

	1> admin disk_space,mb
	RS> Col# Label      JDBC Type Name         Guessed DBMS type Source Table
	RS> ---- ---------- ---------------------- ----------------- ------------
	RS> 1    Partition  java.sql.Types.VARCHAR varchar(255)      -none-      
	RS> 2    Logical    java.sql.Types.VARCHAR varchar(31)       -none-      
	RS> 3    Part.Id    java.sql.Types.INTEGER int               -none-      
	RS> 4    Total Segs java.sql.Types.INTEGER int               -none-      
	RS> 5    Used Segs  java.sql.Types.INTEGER int               -none-      
	RS> 6    Total MBs  java.sql.Types.INTEGER int               -none-      
	RS> 7    Used MBs   java.sql.Types.INTEGER int               -none-      
	RS> 8    State      java.sql.Types.VARCHAR varchar(31)       -none-      
	+-------------------------------+-------+-------+----------+---------+---------+--------+----------+
	|Partition                      |Logical|Part.Id|Total Segs|Used Segs|Total MBs|Used MBs|State     |
	+-------------------------------+-------+-------+----------+---------+---------+--------+----------+
	|/sybdev/devices/rs/PROD_REP.sd1|sd1    |101    |16384     |10       |16384    |10      |ON-LINE///|
	+-------------------------------+-------+-------+----------+---------+---------+--------+----------+
	(1 rows affected)
	*/	
	
	
	
	//--------------------------------------------------------------
	// Alarm handling
	//--------------------------------------------------------------
	@Override
	public void sendAlarmRequest()
	{
		if ( ! hasAbsData() )
			return;
		
		if ( ! AlarmHandler.hasInstance() )
			return;

		CountersModel cm = this;

		boolean debugPrint = Configuration.getCombinedConfiguration().getBooleanProperty("sendAlarmRequest.debug", _logger.isDebugEnabled());

		//-------------------------------------------------------
		// Space Used in Segs (MB)
		//-------------------------------------------------------
		if (isSystemAlarmsForColumnEnabledAndInTimeRange("SpaceUsedSegs"))
		{
			Double TotalSegs = cm.getAbsValueSum("Total Segs");
			Double UsedSegs  = cm.getAbsValueSum("Used Segs");
			
			if (TotalSegs != null && UsedSegs != null)
			{
				
				BigDecimal usedPct = new BigDecimal(100.0 * (UsedSegs / TotalSegs)).setScale(2, BigDecimal.ROUND_HALF_EVEN);
				int usedSpaceInMb = UsedSegs.intValue();
				int freeSpaceInMb = TotalSegs.intValue() - UsedSegs.intValue();

				int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_SpaceUsedSegs, DEFAULT_alarm_SpaceUsedSegs);

				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest("+cm.getName()+"): threshold="+threshold+", usedSpaceInMb="+usedSpaceInMb+", freeSpaceInMb="+freeSpaceInMb+", usedPct="+usedPct+".");

				if (usedSpaceInMb > threshold)
				{
					String extendedDescText = "";
					String extendedDescHtml =               cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_QUEUE_SIZE);
					       extendedDescHtml += "<br><br>" + cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_QUEUE_USAGE_PCT);

					AlarmEvent ae = new AlarmEventRsSdUsage(cm, threshold, "USED_SEGS", usedSpaceInMb, freeSpaceInMb, usedPct.doubleValue());
					ae.setExtendedDescription(extendedDescText, extendedDescHtml);

					AlarmHandler.getInstance().addAlarm(ae);
				}
			}
		}

		//-------------------------------------------------------
		// Space Free in Segs (MB)
		//-------------------------------------------------------
		if (isSystemAlarmsForColumnEnabledAndInTimeRange("SpaceFreeSegs"))
		{
			Double TotalSegs = cm.getAbsValueSum("Total Segs");
			Double UsedSegs  = cm.getAbsValueSum("Used Segs");
			
			if (TotalSegs != null && UsedSegs != null)
			{
				
				BigDecimal usedPct = new BigDecimal(UsedSegs / TotalSegs).setScale(2, BigDecimal.ROUND_HALF_EVEN);
				int usedSpaceInMb = UsedSegs.intValue();
				int freeSpaceInMb = TotalSegs.intValue() - UsedSegs.intValue();

				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest("+cm.getName()+"): usedSpaceInMb="+usedSpaceInMb+", freeSpaceInMb="+freeSpaceInMb+", usedPct="+usedPct+".");

				int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_SpaceFreeSegs, DEFAULT_alarm_SpaceFreeSegs);
				if (freeSpaceInMb < threshold)
				{
					String extendedDescText = "";
					String extendedDescHtml =               cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_QUEUE_SIZE);
					       extendedDescHtml += "<br><br>" + cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_QUEUE_USAGE_PCT);

					AlarmEvent ae = new AlarmEventRsSdUsage(cm, threshold, "FREE_SEGS", usedSpaceInMb, freeSpaceInMb, usedPct.doubleValue());
					ae.setExtendedDescription(extendedDescText, extendedDescHtml);

					AlarmHandler.getInstance().addAlarm(ae);
				}
			}
		}

		//-------------------------------------------------------
		// Space Usage in PCT
		//-------------------------------------------------------
		if (isSystemAlarmsForColumnEnabledAndInTimeRange("SpaceUsedPct"))
		{
			Double TotalSegs = cm.getAbsValueSum("Total Segs");
			Double UsedSegs  = cm.getAbsValueSum("Used Segs");
			
			if (TotalSegs != null && UsedSegs != null)
			{
				
				BigDecimal usedPct = new BigDecimal(UsedSegs / TotalSegs).setScale(2, BigDecimal.ROUND_HALF_EVEN);
				int usedSpaceInMb = UsedSegs.intValue();
				int freeSpaceInMb = TotalSegs.intValue() - UsedSegs.intValue();

				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest("+cm.getName()+"): usedSpaceInMb="+usedSpaceInMb+", freeSpaceInMb="+freeSpaceInMb+", usedPct="+usedPct+".");

				int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_SpaceUsedPct, DEFAULT_alarm_SpaceUsedPct);
				if (usedPct.intValue() > threshold)
				{
					String extendedDescText = "";
					String extendedDescHtml =               cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_QUEUE_SIZE);
					       extendedDescHtml += "<br><br>" + cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_QUEUE_USAGE_PCT);

					AlarmEvent ae = new AlarmEventRsSdUsage(cm, threshold, "USED_PCT", usedSpaceInMb, freeSpaceInMb, usedPct.doubleValue());
					ae.setExtendedDescription(extendedDescText, extendedDescHtml);

					AlarmHandler.getInstance().addAlarm(ae);
				}
			}
		}
	} // end: method

	@Override
	public boolean isGraphDataHistoryEnabled(String name)
	{
		// ENABLED for the following graphs
		if (GRAPH_NAME_QUEUE_SIZE     .equals(name)) return true;
		if (GRAPH_NAME_QUEUE_USAGE_PCT.equals(name)) return true;

		// default: DISABLED
		return false;
	}
	@Override
	public int getGraphDataHistoryTimeInterval(String name)
	{
		// Keep interval: default is 60 minutes
		return super.getGraphDataHistoryTimeInterval(name);
	}

	
	public static final String  PROPKEY_alarm_SpaceUsedSegs                      = CM_NAME + ".alarm.system.if.SpaceUsedSegs.gt";
	public static final int     DEFAULT_alarm_SpaceUsedSegs                      = 8192;
	
	public static final String  PROPKEY_alarm_SpaceFreeSegs                      = CM_NAME + ".alarm.system.if.SpaceFreeSegs.lt";
	public static final int     DEFAULT_alarm_SpaceFreeSegs                      = 2048;
	
	public static final String  PROPKEY_alarm_SpaceUsedPct                       = CM_NAME + ".alarm.system.if.SpaceUsedPct.lt";
	public static final int     DEFAULT_alarm_SpaceUsedPct                       = 70;
	
	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;
		
		list.add(new CmSettingsHelper("SpaceUsedSegs", isAlarmSwitch, PROPKEY_alarm_SpaceUsedSegs, Integer.class,  conf.getIntProperty(PROPKEY_alarm_SpaceUsedSegs, DEFAULT_alarm_SpaceUsedSegs), DEFAULT_alarm_SpaceUsedSegs, "If 'SpaceUsedSegs' is GREATER than ####, send 'AlarmEventRsSdUsage'."));
		list.add(new CmSettingsHelper("SpaceFreeSegs", isAlarmSwitch, PROPKEY_alarm_SpaceFreeSegs, Integer.class,  conf.getIntProperty(PROPKEY_alarm_SpaceFreeSegs, DEFAULT_alarm_SpaceFreeSegs), DEFAULT_alarm_SpaceFreeSegs, "If 'SpaceFreeSegs' is LESS than #### ,send  send 'AlarmEventRsSdUsage'."));
		list.add(new CmSettingsHelper("SpaceUsedPct",  isAlarmSwitch, PROPKEY_alarm_SpaceUsedPct , Integer.class,  conf.getIntProperty(PROPKEY_alarm_SpaceUsedPct , DEFAULT_alarm_SpaceUsedPct ), DEFAULT_alarm_SpaceUsedPct , "If 'SpaceUsedPct' is LESS than ##, send  send 'AlarmEventRsSdUsage'."));

		return list;
	}
}
