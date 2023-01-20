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
package com.asetune.cm.postgres;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.alarm.AlarmHandler;
import com.asetune.alarm.events.AlarmEvent;
import com.asetune.alarm.events.postgres.AlarmEventPgArchiveError;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.graph.TrendGraphDataPoint.LabelType;
import com.asetune.cm.CountersModel;
import com.asetune.gui.MainFrame;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.conn.info.DbmsVersionInfo;
import com.asetune.utils.Configuration;
import com.asetune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmPgArchiver
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmPgArchiver.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmPgArchiver.class.getSimpleName();
	public static final String   SHORT_NAME       = "Wal Archiver";
	public static final String   HTML_DESC        = 
		"<html>" +
		"One row only, containing data about the WAL archiver process of the cluster.<br> <i>(WAL = Write Ahead Log, or the Transaction log)</i>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = Ver.ver(9,4);
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"pg_stat_archiver"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
			"archived_count",
			"failed_count"
	};
	
	// RS> Col# Label              JDBC Type Name           Guessed DBMS type Source Table    
	// RS> ---- ------------------ ------------------------ ----------------- ----------------
	// RS> 1    archived_count     java.sql.Types.BIGINT    int8              pg_stat_archiver
	// RS> 2    last_archived_wal  java.sql.Types.VARCHAR   text              pg_stat_archiver
	// RS> 3    last_archived_time java.sql.Types.TIMESTAMP timestamptz       pg_stat_archiver
	// RS> 4    failed_count       java.sql.Types.BIGINT    int8              pg_stat_archiver
	// RS> 5    last_failed_wal    java.sql.Types.VARCHAR   text              pg_stat_archiver
	// RS> 6    last_failed_time   java.sql.Types.TIMESTAMP timestamptz       pg_stat_archiver
	// RS> 7    stats_reset        java.sql.Types.TIMESTAMP timestamptz       pg_stat_archiver	

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

		return new CmPgArchiver(counterController, guiController);
	}

	public CmPgArchiver(ICounterController counterController, IGuiController guiController)
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
	public static final String GRAPH_NAME_ARCHIVED_COUNT = "ArchivedCount";
	
//	@Override
//	protected TabularCntrPanel createGui()
//	{
//		return new CmActiveStatementsPanel(this);
//	}

	@Override
	public boolean isBackgroundDataPollingEnabled()
	{
		// ALWAYS do this check. isActive() == false Always override isBackgroundDataPollingEnabled()
		return true;
	}
	
	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
//		return null;
		List <String> pkCols = new LinkedList<String>();

//		pkCols.add("pk");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return ""
				+ "select * \n"
				+ "    ,CAST( COALESCE( EXTRACT('epoch' FROM clock_timestamp()) - EXTRACT('epoch' FROM last_archived_time), -1) as int) AS last_archived_in_seconds \n"
				+ "from pg_stat_archiver \n"
				+ "";
	}

	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_ARCHIVED_COUNT,
				"Archived Files Count", 	                // Menu CheckBox text
				"Archived Files Count (archived_count) ("+SHORT_NAME+")", // Graph Label 
				TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL,
				new String[] {"archived_count"}, 
				LabelType.Static, 
				TrendGraphDataPoint.Category.OTHER,
				false, // is Percent Graph
				false, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height

	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		if (GRAPH_NAME_ARCHIVED_COUNT.equals(tgdp.getName()))
		{
			Double[] arr = new Double[1];

			arr[0] = this.getDiffValueAsDouble(0, "archived_count");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}
	}
	
	
	//----------------------------------------------------------------
	// ALARMS
	//----------------------------------------------------------------
	@Override
	public void sendAlarmRequest()
	{
		if ( ! hasDiffData() )
			return;

		if ( ! AlarmHandler.hasInstance() )
			return;

		AlarmHandler alarmHandler = AlarmHandler.getInstance();

		CountersModel cm = this;

		boolean debugPrint = Configuration.getCombinedConfiguration().getBooleanProperty("sendAlarmRequest.debug", _logger.isDebugEnabled());

		//-------------------------------------------------------
		// failed_count
		//-------------------------------------------------------
		if (isSystemAlarmsForColumnEnabledAndInTimeRange("failed_count"))
		{
			int row = 0;

			Integer failed_count_diff = cm.getDiffValueAsInteger(row, "failed_count");
			Integer failed_count_abs  = cm.getAbsValueAsInteger (row, "failed_count");
			if (failed_count_diff != null)
			{
				int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_failed_count, DEFAULT_alarm_failed_count);

				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest("+cm.getName()+"): threshold=" + threshold + ", failed_count_diff='" + failed_count_diff + "', failed_count_abs=" + failed_count_abs + ".");

				if (failed_count_diff > threshold)
				{
					String extendedDescText = cm.toTextTableString(DATA_RATE, row);
					String extendedDescHtml = cm.toHtmlTableString(DATA_RATE, row, true, false, false);
											
					AlarmEvent ae = new AlarmEventPgArchiveError(cm, threshold, failed_count_diff, failed_count_abs);
					ae.setExtendedDescription(extendedDescText, extendedDescHtml);
				
					alarmHandler.addAlarm( ae );
				}
			}
		}

		//-------------------------------------------------------
		// failed_count
		//-------------------------------------------------------
		if (isSystemAlarmsForColumnEnabledAndInTimeRange("last_archived_in_seconds"))
		{
			int row = 0;

			Integer last_archived_in_seconds = cm.getDiffValueAsInteger(row, "last_archived_in_seconds");
			if (last_archived_in_seconds != null)
			{
				int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_last_archived_in_seconds, DEFAULT_alarm_last_archived_in_seconds);

				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest("+cm.getName()+"): threshold=" + threshold + ", last_archived_in_seconds='" + last_archived_in_seconds + "'.");

				if (last_archived_in_seconds > threshold)
				{
					String last_archived_time = cm.getAbsString(row, "last_archived_time");

					String extendedDescText = cm.toTextTableString(DATA_RATE, row);
					String extendedDescHtml = cm.toHtmlTableString(DATA_RATE, row, true, false, false);
											
					AlarmEvent ae = new AlarmEventPgArchiveError(cm, threshold, last_archived_in_seconds, last_archived_time);
					ae.setExtendedDescription(extendedDescText, extendedDescHtml);
				
					alarmHandler.addAlarm( ae );
				}
			}
		}
	}


	public static final String  PROPKEY_alarm_failed_count               = CM_NAME + ".alarm.system.if.failed_count.gt";
	public static final int     DEFAULT_alarm_failed_count               = 0;

	public static final String  PROPKEY_alarm_last_archived_in_seconds   = CM_NAME + ".alarm.system.if.last_archived_in_seconds.gt";
	public static final int     DEFAULT_alarm_last_archived_in_seconds   = Integer.MAX_VALUE; // approximately 68 year... so basically disabled

	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		
		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;
		
		list.add(new CmSettingsHelper("failed_count"            , isAlarmSwitch, PROPKEY_alarm_failed_count            , Integer.class, conf.getIntProperty(PROPKEY_alarm_failed_count            , DEFAULT_alarm_failed_count            ), DEFAULT_alarm_failed_count            , "If 'diff.failed_count' is greater than ## then send 'AlarmEventPgArchiveError'." ));
		list.add(new CmSettingsHelper("last_archived_in_seconds", isAlarmSwitch, PROPKEY_alarm_last_archived_in_seconds, Integer.class, conf.getIntProperty(PROPKEY_alarm_last_archived_in_seconds, DEFAULT_alarm_last_archived_in_seconds), DEFAULT_alarm_last_archived_in_seconds, "If 'last_archived_in_seconds' is greater than ## then send 'AlarmEventPgArchiveError'." ));

		return list;
	}
}
