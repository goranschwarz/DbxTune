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
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.dbxtune.pcs.report.content.os;

import java.io.IOException;
import java.io.Writer;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.alarm.AlarmHandler;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.alarm.events.AlarmEventSpaceFullPrediction;
import com.dbxtune.central.controllers.SpaceForecastServlet;
import com.dbxtune.central.lmetrics.LocalMetricsPersistWriterJdbc;
import com.dbxtune.cm.os.CmOsDiskSpace;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.pcs.report.content.IReportChart;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.SpaceForecast;
import com.dbxtune.utils.SpaceForecast.SpaceForecastResult;
import com.dbxtune.utils.SpaceForecast.SpaceForecastResult.SpaceType;
import com.dbxtune.utils.SpaceFullPredictor;
import com.dbxtune.utils.SpaceFullPredictor.PredictionResult;
import com.dbxtune.utils.SpaceFullPredictor.SourceDataSize;
import com.dbxtune.utils.StringUtil;

public class OsSpaceUsageOverview extends OsAbstract
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public static final String PROPKEY_DaysToDiskFull_error          = "OsSpaceUsageOverview.alarm.DaysToDiskFull.error";
	public static final int    DEFAULT_DaysToDiskFull_error          = 7;

	public static final String PROPKEY_DaysToDiskFull_warning        = "OsSpaceUsageOverview.alarm.DaysToDiskFull.warning";
	public static final int    DEFAULT_DaysToDiskFull_warning        = 14;

	/** This is a CSV list of names... The list can contain regular expressions */
	public static final String PROPKEY_DaysToDiskFull_SkipNames      = "OsSpaceUsageOverview.alarm.DaysToDiskFull.skip.names";
	public static final String DEFAULT_DaysToDiskFull_SkipNames      = "";
	
	public static final String  PROPKEY_DaysToDiskFull_alarmIsEnable = "OsSpaceUsageOverview.alarm.DaysToDiskFull.enable";
	public static final boolean DEFAULT_DaysToDiskFull_alarmIsEnable = true;

	public static final String PROPKEY_DaysToDiskFull_historicalDays = "OsSpaceUsageOverview.dbxCentral.DaysToDiskFull.days";
	public static final int    DEFAULT_DaysToDiskFull_historicalDays = 30;

	public static final String PROPKEY_DaysToDiskFull_sampleMinutes  = "OsSpaceUsageOverview.dbxCentral.DaysToDiskFull.sampleMinutes";
	public static final int    DEFAULT_DaysToDiskFull_sampleMinutes  = 60;

//	private ResultSetTableModel _daysToDiskFullRstm;
	private String              _dbxCentralSpaceFullCallError = "";
	private String              _daysToDiskFullTableStr = "";
	
	public OsSpaceUsageOverview(DailySummaryReportAbstract reportingInstance)
	{
		super(reportingInstance);
	}

	@Override
	public boolean isEnabled()
	{
		// If super is DISABLED, no need to continue
		boolean isEnabled = super.isEnabled();
		if ( ! isEnabled )
			return isEnabled;

		// NOT For Windows
//		String dbmsVerStr = getReportingInstance().getDbmsVersionStr();
//		if (StringUtil.hasValue(dbmsVerStr))
//		{
//			if (dbmsVerStr.indexOf("Windows") >= 0)
//			{
//				setDisabledReason("This DBMS is running on Windows, wich is not supported by this report.");
//				return false;
//			}
//		}
		return true;
	}
	
	@Override
	public boolean hasMinimalMessageText()
	{
		return false;
	}

	@Override
	public boolean hasShortMessageText()
	{
		return true;
	}

//	@Override
//	public void writeShortMessageText(Writer w)
//	throws IOException
//	{
//		writeMessageText(w);
//	}

	@Override
	public void writeMessageText(Writer w, MessageType messageType)
	throws IOException
	{
//		int errorThreshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_DaysToDiskFull_error  , DEFAULT_DaysToDiskFull_error);
//		int warnThreshold  = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_DaysToDiskFull_warning, DEFAULT_DaysToDiskFull_warning);
//
//		w.append("Below is a table trying to estimate how many days we have left until the disk gets full.<br>\n");
//		w.append(" &nbsp; &bull; If days left is below " + errorThreshold + " the cell will be marked as 'red'.   <br>\n");
//		w.append(" &nbsp; &bull; If days left is below " + warnThreshold  + " the cell will be marked as 'orange'.<br>\n");
//		w.append(_daysToDiskFullTableStr);
//		w.append("<br>");

		// DAYS TO FULL REPORT
		boolean printDaysToFull = true;
		if (printDaysToFull)
		{
			if (StringUtil.hasValue(_dbxCentralSpaceFullCallError))
			{
				w.append("<p style='color: red;'>Problems Calling DbxCentral to get 'long' (30d) Space History, falling back to 'local' (24h) history. <br>Problem: " + _dbxCentralSpaceFullCallError + "</p>");
			}

			w.append(_daysToDiskFullTableStr);
			w.append("<br> \n");
			w.append("To change Thresholds for <b>Critical</b> use property: <code>" + PROPKEY_DaysToDiskFull_error   + " = ##</code><br> \n");
			w.append("To Change Thresholds for <b>Warning</b>  use property: <code>" + PROPKEY_DaysToDiskFull_warning + " = ##</code><br> \n");
			w.append("<br> \n");
		}
		
		w.append(getDbxCentralLinkWithDescForGraphs(false, "Below are Disk Space Usage on the Operating System Level.",
				"CmOsDiskSpace_FsUsedPct",
				"CmOsDiskSpace_FsAvailableMb",
				"CmOsDiskSpace_FsUsedMb"
				));
		
		_CmOsDiskSpace_FsUsedPct    .writeHtmlContent(w, null, null);
		_CmOsDiskSpace_FsAvailableMb.writeHtmlContent(w, null, null);
		_CmOsDiskSpace_FsUsedMb     .writeHtmlContent(w, null, null);
	}

	@Override
	public String getSubject()
	{
		return "OS Disk Space Usage (origin: CmOsDiskSpace_abs / os-cmd:df)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}

//	private void getDaysToDiskFull(DbxConnection conn, String schema)
//	{
//		
//		String tabName = "[CmOsDiskSpace_abs]";
//
//		if (StringUtil.hasValue(schema))
//			tabName = "[" + schema + "]." + tabName;
//		
//		
//		String sql = ""
//			    + " \n"
//			    + "WITH stats AS \n"
//			    + "( \n"
//			    + "    SELECT \n"
//			    + "         [MountedOn]         AS [MountedOn] \n"
//			    + "        ,[Filesystem] \n"
//			    + "        ,MAX([Size-MB])      AS [TotalMb] \n"
//			    + "        ,MIN([Available-MB]) AS [PeekAvailableMb] \n"
//			    + "    FROM " + tabName + " \n"
//			    + "    GROUP BY [MountedOn], [Filesystem] \n"
//			    + "), \n"
//			    + "firstSample AS \n"
//			    + "( \n"
//			    + "    SELECT \n"
//			    + "         [MountedOn] \n"
//			    + "        ,[Filesystem] \n"
//			    + "        ,[Available-MB] AS [FreeStartMb] \n"
//			    + "    FROM " + tabName + " \n"
//			    + "    WHERE [SessionSampleTime] = (SELECT MIN([SessionSampleTime]) FROM " + tabName + ") \n"
//			    + "), \n"
//			    + "lastSample AS \n"
//			    + "( \n"
//			    + "    SELECT \n"
//			    + "         [MountedOn] \n"
//			    + "        ,[Filesystem] \n"
//			    + "        ,[Available-MB] AS [FreeEndMb] \n"
//			    + "    FROM " + tabName + " \n"
//			    + "    WHERE [SessionSampleTime] = (SELECT MAX([SessionSampleTime]) FROM " + tabName + ") \n"
//			    + "), \n"
//			    + "peekSample AS \n"
//			    + "( \n"
//			    + "    SELECT \n"
//			    + "         d.[MountedOn] \n"
//			    + "        ,d.[Filesystem] \n"
//			    + "        ,MIN(d.[SessionSampleTime]) AS [PeekTime] \n"
//			    + "    FROM " + tabName + " d \n"
//			    + "    JOIN stats s \n"
//			    + "      ON d.[MountedOn]    = s.[MountedOn] \n"
//			    + "     AND d.[Filesystem]   = s.[Filesystem] \n"
//			    + "     AND d.[Available-MB] = s.[PeekAvailableMb] \n"
//			    + "    GROUP BY \n"
//			    + "         d.[MountedOn] \n"
//			    + "        ,d.[Filesystem] \n"
//			    + ") \n"
//			    + "SELECT \n" // MountedOn, Filesystem, DaysToDiskFull_AtPeek, DaysToDiskFull_WithoutPeek, DailyDeltaMb, TotalMb, FreeMbAtSampleStart, FreeMbAtSampleEnd, LowestFreeMbPeek, LowestFreeMbPeekTime
//			    + "     s.[MountedOn] \n"
//			    + "    ,s.[Filesystem] \n"
//			    + "    ,CASE \n"
//			    + "         WHEN (l.[FreeEndMb] - f.[FreeStartMb]) < 0 \n"
//			    + "         THEN CAST( CAST(s.[PeekAvailableMb] AS FLOAT) / ABS(l.[FreeEndMb] - f.[FreeStartMb]) as NUMERIC(10,0)) \n"
////				+ "         WHEN (LEAST(l.[FreeEndMb], s.[PeekAvailableMb]) - f.[FreeStartMb]) < 0 \n"
////				+ "         THEN CAST( CAST(LEAST(l.[FreeEndMb], s.[PeekAvailableMb]) AS FLOAT) / ABS(LEAST(l.[FreeEndMb], s.[PeekAvailableMb]) - f.[FreeStartMb]) as NUMERIC(10,0)) \n"
////TODO; // Which of the above should I use ????				
//			    + "         ELSE NULL \n"
//			    + "     END                               AS [DaysToDiskFull_AtPeek] \n"
//			    + "    ,CASE \n"
//			    + "         WHEN (l.[FreeEndMb] - f.[FreeStartMb]) < 0 \n"
//			    + "         THEN CAST( CAST(l.[FreeEndMb] AS FLOAT) / ABS(l.[FreeEndMb] - f.[FreeStartMb]) as NUMERIC(10,0)) \n"
//			    + "         ELSE NULL \n"
//			    + "     END                               AS [DaysToDiskFull_WithoutPeek] \n"
//			    + "    ,(l.[FreeEndMb] - f.[FreeStartMb]) AS [DailyDeltaMb] \n"
//			    + "    ,s.[TotalMb]                       AS [TotalMb] \n"
//			    + "    ,f.[FreeStartMb]                   AS [FreeMbAtSampleStart] \n"
//			    + "    ,l.[FreeEndMb]                     AS [FreeMbAtSampleEnd] \n"
//			    + "    ,s.[PeekAvailableMb]               AS [LowestFreeMbPeek] \n"
//			    + "    ,p.[PeekTime]                      AS [LowestFreeMbPeekTime] \n"
//			    + "FROM stats s \n"
//			    + "JOIN firstSample f \n"
//			    + "      ON s.[MountedOn]  = f.[MountedOn] \n"
//			    + "     AND s.[Filesystem] = f.[Filesystem] \n"
//			    + "JOIN lastSample l \n"
//			    + "      ON s.[MountedOn]  = l.[MountedOn] \n"
//			    + "     AND s.[Filesystem] = l.[Filesystem] \n"
//			    + "JOIN peekSample p \n"
//			    + "      ON s.[MountedOn]  = p.[MountedOn] \n"
//			    + "     AND s.[Filesystem] = p.[Filesystem] \n"
//			    + "WHERE s.[TotalMb] > 1024 \n"
//			    + "  AND s.[MountedOn] NOT LIKE '/boot/%' \n"
//			    + "  AND s.[MountedOn] NOT LIKE '/sys/%'; \n"
//			    + "";
//		
//		// EXECUTE
//		_daysToDiskFullRstm = executeQuery(conn, sql, false, "DaysToDiskFull");
//		
//		if (_daysToDiskFullRstm == null)
//		{
//			_daysToDiskFullRstm = ResultSetTableModel.createEmpty("DaysToDiskFull");
//			_daysToDiskFullTableStr = "<b>No data was found</b>";
//
//			return;
//		}
//		else
//		{
//			int errorThreshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_DaysToDiskFull_error  , DEFAULT_DaysToDiskFull_error);
//			int warnThreshold  = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_DaysToDiskFull_warning, DEFAULT_DaysToDiskFull_warning);
//
//			TableStringRenderer htmlTabRenderer = new TableStringRenderer()
//			{
//				@Override
//				public String tagThAttr(ResultSetTableModel rstm, int col, String colName, boolean nowrapPreferred)
//				{
//					return "nowrap";
//				}
//
//				@Override
//				public String tagTdAttr(ResultSetTableModel rstm, int row, int col, String colName, Object objVal, String strVal, boolean nowrapPreferred)
//				{
//					if ("DaysToDiskFull_AtPeek".equals(colName) || "DaysToDiskFull_WithoutPeek".equals(colName))
//					{
//						if (objVal != null && objVal instanceof Number)
//						{
//							int intVal = ((Number)objVal).intValue();
//
//							if (intVal <= errorThreshold)
//							{
//								// TODO: Can we RAISE some kind of Alarm Here ????
//								if (AlarmHandler.hasInstance())
//								{
//									String srvName = getReportingInstance().getDbmsServerName();
//
//									String    MountedOn                  = rstm.getValueAsString   (row, "MountedOn");
//									String    Filesystem                 = rstm.getValueAsString   (row, "Filesystem");
//									int       DaysToDiskFull_AtPeek      = rstm.getValueAsInteger  (row, "DaysToDiskFull_AtPeek");
//									int       DaysToDiskFull_WithoutPeek = rstm.getValueAsInteger  (row, "DaysToDiskFull_WithoutPeek");
////									int       TotalMB                    = rstm.getValueAsInteger  (row, "TotalMB");
////									int       FreeMbAtSampleEnd          = rstm.getValueAsInteger  (row, "FreeMbAtSampleEnd");
////									int       DailyDeltaMb               = rstm.getValueAsInteger  (row, "DailyDeltaMb");
////									int       LowestFreeMbPeek           = rstm.getValueAsInteger  (row, "LowestFreeMbPeek");
////									Timestamp LowestFreeMbPeekTime       = rstm.getValueAsTimestamp(row, "LowestFreeMbPeekTime");
//
//									String extraAscii = rstm.toAsciiTableString();
//									String extraHtml  = rstm.createHtmlKeyValueTableFromRow(row, null, null);
//
//									AlarmEvent ae = new AlarmEventOsDiskFullPrediction(srvName, "DSR:OsSpaceUsageOverview", AlarmEvent.Severity.ERROR, 
//											MountedOn, Filesystem, DaysToDiskFull_AtPeek, DaysToDiskFull_WithoutPeek, 
//											errorThreshold);
//									ae.setExtendedDescription(extraAscii, extraHtml);
//// --DONE-- //TODO; // Check if we can FIX/SET an alarm as a "SingleShoot" or "AlarmAndThenCancel"... or similar... otherwise the alarm wont be canceled (since "OsSpaceUsageOverview" is NEVER in the list of "re-fresched" CM's 
////TODO; // Check Column names... they may not be the best (and or column positions) 
////--DONE-- //TODO; // Possible change (NULL) "no new disk allocation/incrementation"
////--DONE-- //TODO; // Possible change "DaysToDiskFull_xxx" is above 365 -- Maybe "more than 365 days"
////TODO; // In the CmOsDiskSpace ... can we remove some "rows" like we do here (Size or start with ...) so they wont be in the Graph's 
//
////TODO; // Check if the "schema" thing works on DbxCentral LocalMonitoring
//
////TODO; // Also check ASE (SQL Server) Database Size "DaysToDiskFull_DATA/LOG"
//
//									AlarmHandler.getInstance().addAlarm(ae);
//								}
//								return "nowrap bgcolor='red'";
//							}
//
//							if (intVal <= warnThreshold)
//							{
//								// TODO: Can we RAISE some kind of Alarm Here ????
//								return "nowrap bgcolor='orange'";
//							}
//						}
//					}
//
//					return "nowrap";
//				}
//
//				@Override
//				public String cellValue(ResultSetTableModel rstm, int row, int col, String colName, Object objVal, String strVal)
//				{
//					if ("DaysToDiskFull_AtPeek".equals(colName) || "DaysToDiskFull_WithoutPeek".equals(colName))
//					{
//						if (rstm.isNull(objVal))
//						{
//							return "<i>Free space is unchanged</i>";
////							return "<i>no size raise</i>";
////							return "<i>no size increase</i>";
////							return "<i>no size enlargement</i>";
//						}
//						if (objVal != null && objVal instanceof Number)
//						{
//							int intVal = ((Number)objVal).intValue();
//							if (intVal > 365)
//								return "<i>Above One Year</i>";
//						}
//					}
//
//					return TableStringRenderer.super.cellValue(rstm, row, col, colName, objVal, strVal);
//				}
//			};
//			_daysToDiskFullTableStr = _daysToDiskFullRstm.toHtmlTableString("sorttable", htmlTabRenderer);
//		}
//	}

	private void getDaysToDiskFull_fromLocalRecording(DbxConnection conn, String schema)
	throws Exception
	{
		try
		{
			// Create the predictor instance
			SpaceFullPredictor fsFullPredictor = new SpaceFullPredictor(conn);

			Set<PredictionResult> fsResult = fsFullPredictor.predictSpaceFull(
					SourceDataSize.KB,
					"MountedOn", 
					"Filesystem", 
					"SessionSampleTime", 
					"Size-KB", 
					"Used-KB", 
					"Available-KB", 
					schema, 
					"CmOsDiskSpace_abs");

			// And write the report to a String that will be added by method: writeMessageText(...)
			_daysToDiskFullTableStr = fsFullPredictor.generateHtmlReport(fsResult, false, false, 
					"Below is a table trying to estimate how many days we have left until the Disk Space gets full." +
					"Alarms will be sent if/when CRITICAL is reached.");

//System.out.println(">>>>>>>>>>>>>>>>> OsSpaceUsageOverview::getDaysToDiskFull(): AlarmHandler.hasInstance()=" + AlarmHandler.hasInstance() + ", SpaceFullPredictor.getCriticalAlerts(fsResult).size()=" + SpaceFullPredictor.getCriticalAlerts(fsResult).size());
			// Create Alarms on any Critical Alerts
			boolean doCheckAlarms = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_DaysToDiskFull_alarmIsEnable, DEFAULT_DaysToDiskFull_alarmIsEnable);
			if (doCheckAlarms)
			{
				Set<PredictionResult> severityCriticals = SpaceFullPredictor.getSeverityCriticals(fsResult);
				Set<PredictionResult> severityWarnings  = SpaceFullPredictor.getSeverityWarnings (fsResult);
				Set<PredictionResult> severityOk        = SpaceFullPredictor.getSeverityOk       (fsResult);
				
				_logger.info("Resources Severity Count: Criticals=" + severityCriticals.size() + ", Warnings=" + severityWarnings.size() + ", OK=" + severityOk.size() + ".");
				
				if ( ! severityCriticals.isEmpty() ) // Why isn't this true ??? ... lets try with 'severityCriticals.size() > 0' instead...
//				if ( severityCriticals.size() > 0 )
				{
					_logger.info("DUMMY-DEBUG: Resources Severity Count: Criticals=" + severityCriticals.size() + ", Warnings=" + severityWarnings.size() + ", OK=" + severityOk.size() + ".");
					for (PredictionResult pr : severityCriticals)
					{
						_logger.info("DUMMY-DEBUG: pr.getFmtResourceAndSecondaryNamePlainStr()=" + pr.getFmtResourceAndSecondaryNamePlainStr());

						if (AlarmHandler.hasInstance())
						{
							String srvName = getReportingInstance().getDbmsServerName();

//							System.out.println("  - " + pr._resourceName + " will be full in " + pr._hoursUntilFull + " hours!");
							String extraAscii = pr.toString();
							String extraHtml  = pr.toHtmlKeyValueTable(null); //rstm.createHtmlKeyValueTableFromRow(row, null, null);

							AlarmEvent ae = new AlarmEventSpaceFullPrediction(
									srvName, 
									"DSR:" + this.getClass().getSimpleName(), 
									AlarmEvent.Severity.ERROR, 
									SpaceType.OS_DISK,
									pr._resourceName, 
									pr._secondaryName, 
									pr._hoursUntilFull, 
									fsFullPredictor.getCriticalThresholdHours());

							ae.setExtendedDescription(extraAscii, extraHtml);

							// And finally send the alarm
							AlarmHandler.getInstance().addAlarm( ae );
						}
						else
						{
							String msg = pr.getFmtResourceAndSecondaryNamePlainStr() + " will be full in " + pr.getFmtHoursUntilFull() + " hours (" + pr.getFmtDaysUntilFull() + " days). FreeMb=" + pr.getFmtFreeMb() + ", GrowthMbPerDay=" + pr.getFmtGrowthMbPerDay();
							_logger.warn("NO ALARM HANDLER: " + msg);
						}
					}
				}
			}
		}
		catch (Exception ex)
		{
			// TODO: Should we LOG this to '_logger' or to 'DailySummaryReport' ???
			// For now just "put" the stacktrace as the "Table" 
			//ex.printStackTrace();
//			_daysToDiskFullTableStr = StringUtil.toHtmlPre(StringUtil.stackTraceToString(ex));
			
			throw ex;
		}
	}		
	
	private void getDaysToDiskFull_fromDbxCentral(String srvName) 
	throws Exception
	{
		// If the Server name is 'DbxCentral' we must mean LocalMetrics for DbxCentral, so lets use that SCHEMA name as the server name
		if ("DbxCentral".equals(srvName))
		{
			srvName = LocalMetricsPersistWriterJdbc.LOCAL_METRICS_SCHEMA_NAME;
		}

		try
		{
			int dataErrorThreshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_DaysToDiskFull_error  , DEFAULT_DaysToDiskFull_error);
			int dataWarnThreshold  = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_DaysToDiskFull_warning, DEFAULT_DaysToDiskFull_warning);

			int days               = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_DaysToDiskFull_historicalDays, DEFAULT_DaysToDiskFull_historicalDays);
			int sampleMinutes      = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_DaysToDiskFull_sampleMinutes , DEFAULT_DaysToDiskFull_sampleMinutes);
			
			//  Get URL's to be used in below messages
			String osForcasterUrl = getDbxCentralSpaceForecastUrl(srvName, days, sampleMinutes, SpaceType.OS_DISK, SpaceForecastServlet.OUTPUT_TYPE_HTML_PAGE);

			// Make the REST Call and get data as an Object
			SpaceForecast osForcaster = dbxCentralSpaceForecastRestCall(srvName, days, sampleMinutes, SpaceForecast.SpaceForecastResult.SpaceType.OS_DISK);

			osForcaster.setCriticalThresholdDays(dataErrorThreshold);
			osForcaster.setWarningThresholdDays (dataWarnThreshold);

			// And write the report to a String that will be added by method: writeMessageText(...)
			_daysToDiskFullTableStr = osForcaster.generateHtmlReport(false, 
					"Below is a table trying to estimate how many days we have left until the Disk Space gets full. " +
					"Alarms will be sent if/when CRITICAL is reached. " +
					"<a href='" + osForcasterUrl + "' target='_blank'>Check Prediction Again</a>");


			// Create Alarms on any Critical Alerts (from ALL results) NOTE: if we JUST want for DATA or WAL, change 'allResults' --> 'dataResult' || 'walResult'
			boolean doCheckAlarms = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_DaysToDiskFull_alarmIsEnable, DEFAULT_DaysToDiskFull_alarmIsEnable);
			if (doCheckAlarms)
			{
				List<String> skipNames = StringUtil.parseCommaStrToList(Configuration.getCombinedConfiguration().getProperty(PROPKEY_DaysToDiskFull_SkipNames, DEFAULT_DaysToDiskFull_SkipNames), true);

				List<SpaceForecastResult> severityCriticals = new ArrayList<>();
				List<SpaceForecastResult> severityWarnings  = new ArrayList<>();
				List<SpaceForecastResult> severityOthers    = new ArrayList<>();

				// Add OS Severities
				severityCriticals.addAll(osForcaster.getSeverityCriticals());
				severityWarnings .addAll(osForcaster.getSeverityWarnings());
				severityOthers   .addAll(osForcaster.getSeverityOthers());
				

				_logger.info("Resources Severity Count: Criticals=" + severityCriticals.size() + ", Warnings=" + severityWarnings.size() + ", OK=" + severityOthers.size() + ".");
				
				if ( ! severityCriticals.isEmpty() )
				{
					for (SpaceForecastResult sfr : severityCriticals)
					{
						boolean doAlarm = true;

						String name = sfr.mount;
						if (StringUtil.matchesAny(name, skipNames))
						{
							doAlarm = false;
						}

						if (doAlarm)
						{
							if (AlarmHandler.hasInstance())
							{
//								String srvName = sfr.srvName; //getReportingInstance().getDbmsServerName();
								
								String extraAscii = sfr.toString();
								String extraHtml  = sfr.toHtmlKeyValueTable(null);
								
								int threshold = dataErrorThreshold;

								AlarmEvent ae = new AlarmEventSpaceFullPrediction(
										srvName, 
										"DSR:" + this.getClass().getSimpleName(), 
										AlarmEvent.Severity.ERROR, 
										sfr.spaceType,
										sfr.mount, 
										sfr.extraName, 
										sfr.hoursToFull.longValue(), 
										threshold);

								ae.setExtendedDescription(extraAscii, extraHtml);

								// And finally send the alarm
								AlarmHandler.getInstance().addAlarm( ae );
							}
							else
							{
								String msg = sfr.mount + " will be full in " + sfr.hoursToFull + " hours (" + sfr.hoursToFull/24 + " days). lastFreeSizeMb=" + sfr.diskInfoEntry.lastFreeSizeMb + ", slopeMbPerHour=" + sfr.slopeMbPerHour;
								_logger.warn("NO ALARM HANDLER: " + msg);
							}
						}
					}
				}
			}
		}
		catch (Exception ex)
		{
			// TODO: Should we LOG this to '_logger' or to 'DailySummaryReport' ???
			// For now just "put" the stacktrace as the "Table" 
			//ex.printStackTrace();
//			_daysToDiskFullTableStr = StringUtil.toHtmlPre(StringUtil.stackTraceToString(ex));
			
			_dbxCentralSpaceFullCallError = ex.getMessage();
			throw ex;
		}
	}		

	/**
	 * First Get from DbxCentral (a REST Call) -- and get Prediction for last 30 days.<br>
	 * If that FAILES go to local Database Recording (only 24 hours)
	 * 
	 * @param conn
	 * @param schema
	 */
	private void getDaysToDiskFull(DbxConnection conn)
	{
		String serverName     = getReportingInstance().getServerName();     // Needed to call DbxCentral
		String localPcsSchema = getReportingInstance().getDbmsSchemaName(); // This is probably NULL for a DBMS Collector
		
		boolean tryDbxCentral = true; // So we easily can SKIP DbxCentral if desirable
		boolean useFallback   = false;
		
		if (tryDbxCentral)
		{
			try
			{
				getDaysToDiskFull_fromDbxCentral(serverName);
			}
			catch (Exception ex)
			{
				int days = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_DaysToDiskFull_historicalDays, DEFAULT_DaysToDiskFull_historicalDays);
				
				_logger.warn("Space Prediction on DbxCentral failed (longer history " + days + "d). Fallback to LocalRecording (only 24h history).", ex);
				useFallback = true;
			}
		}
		else
		{
			useFallback = true;
		}
		
		if (useFallback)
		{
			try
			{
				getDaysToDiskFull_fromLocalRecording(conn, localPcsSchema);
			}
			catch (Exception ex)
			{
				// TODO: Should we LOG this to '_logger' or to 'DailySummaryReport' ???
				// For now just "put" the stacktrace as the "Table" 
				//ex.printStackTrace();
				_daysToDiskFullTableStr = StringUtil.toHtmlPre(StringUtil.stackTraceToString(ex));
			}
		}
	}

	
	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		getDaysToDiskFull(conn);

		String schema = getReportingInstance().getDbmsSchemaName();

		int maxValue = 100;
		_CmOsDiskSpace_FsUsedPct     = createTsLineChart(conn, schema, CmOsDiskSpace.CM_NAME, CmOsDiskSpace.GRAPH_NAME_USED_PCT,     maxValue, false, null, "df: Space Used in Percent, at MountPoint (Host Monitor->OS Disk Space Usage(df))");
		_CmOsDiskSpace_FsAvailableMb = createTsLineChart(conn, schema, CmOsDiskSpace.CM_NAME, CmOsDiskSpace.GRAPH_NAME_AVAILABLE_MB, -1,       false, null, "df: Space Available in MB, at MountPoint (Host Monitor->OS Disk Space Usage(df))");
		_CmOsDiskSpace_FsUsedMb      = createTsLineChart(conn, schema, CmOsDiskSpace.CM_NAME, CmOsDiskSpace.GRAPH_NAME_USED_MB,      -1,       false, null, "df: Space Used in MB, at MountPoint (Host Monitor->OS Disk Space Usage(df))");
	}

	private IReportChart _CmOsDiskSpace_FsUsedPct;
	private IReportChart _CmOsDiskSpace_FsAvailableMb;
	private IReportChart _CmOsDiskSpace_FsUsedMb;
}
