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
package com.dbxtune.pcs.report.content.sqlserver;

import java.io.IOException;
import java.io.Writer;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.alarm.AlarmHandler;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.alarm.events.AlarmEventSpaceFullPrediction;
import com.dbxtune.central.controllers.SpaceForecastServlet;
import com.dbxtune.cm.sqlserver.CmDatabases;
import com.dbxtune.gui.ResultSetTableModel;
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

public class SqlServerDbSize 
extends SqlServerAbstract
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public static final String PROPKEY_DaysToFull_data_error   = "SqlServerDbSize.alarm.DaysToFull.data.error";
	public static final int    DEFAULT_DaysToFull_data_error   = 7;

	public static final String PROPKEY_DaysToFull_data_warning = "SqlServerDbSize.alarm.DaysToFull.data.warning";
	public static final int    DEFAULT_DaysToFull_data_warning = 14;

	public static final String PROPKEY_DaysToFull_log_error     = "SqlServerDbSize.alarm.DaysToFull.log.error";
	public static final int    DEFAULT_DaysToFull_log_error     = 7;

	public static final String PROPKEY_DaysToFull_log_warning   = "SqlServerDbSize.alarm.DaysToFull.log.warning";
	public static final int    DEFAULT_DaysToFull_log_warning   = 14;

	/** This is a CSV list of names... The list can contain regular expressions */
	public static final String PROPKEY_DaysToFull_SkipDbnames   = "SqlServerDbSize.alarm.DaysToFull.skip.dbnames";
	public static final String DEFAULT_DaysToFull_SkipDbnames   = "";
	
	public static final String  PROPKEY_DaysToFull_alarmIsEnable = "SqlServerDbSize.alarm.DaysToFull.enable";
	public static final boolean DEFAULT_DaysToFull_alarmIsEnable = false;

	public static final String PROPKEY_DaysToFull_historicalDays = "SqlServerDbSize.dbxCentral.DaysToFull.days";
	public static final int    DEFAULT_DaysToFull_historicalDays = 30;

	public static final String PROPKEY_DaysToFull_sampleMinutes = "SqlServerDbSize.dbxCentral.DaysToFull.sampleMinutes";
	public static final int    DEFAULT_DaysToFull_sampleMinutes = 60;

	private ResultSetTableModel _shortRstm;
	private String              _dbxCentralSpaceFullCallError = "";
	private String              _daysToFullDataTableStr = "";
	private String              _daysToFullWalTableStr  = "";

	public SqlServerDbSize(DailySummaryReportAbstract reportingInstance)
	{
		super(reportingInstance);
	}

	@Override
	public boolean hasMinimalMessageText()
	{
		return false;
	}

	@Override
	public boolean hasShortMessageText()
	{
		return false;
	}

//	@Override
//	public void writeShortMessageText(Writer w)
//	throws IOException
//	{
//	}

	@Override
	public void writeMessageText(Writer sb, MessageType messageType)
	throws IOException
	{
		// Get a description of this section, and column names
		sb.append(getSectionDescriptionHtml(_shortRstm, true));

		// Last sample Database Size info
		sb.append("Row Count: " + _shortRstm.getRowCount() + "<br>\n");
		sb.append(_shortRstm.toHtmlTableString("sortable", true, true, null, new ReportEntryTableStringRenderer()
		{
			@Override
			public String cellValue(ResultSetTableModel rstm, int row, int col, String colName, Object objVal, String strVal)
			{
				/**
				 * If compatibility_level is below "tempdb", then mark the cell as RED
				 */
				if ("compatibility_level".equals(colName))
				{
					int compatLevel_tempdb = -1;
					for (int r=0; r<rstm.getRowCount(); r++)
					{
						String dbname = rstm.getValueAsString(r, "DBName");
						if ("tempdb".equals(dbname))
						{
							compatLevel_tempdb = rstm.getValueAsInteger(r, "compatibility_level", true, -1);
							break;
						}
					}
					int compatLevel_curDb = StringUtil.parseInt(strVal, -1);
					if (compatLevel_curDb < compatLevel_tempdb)
					{
						String tooltip = "Column 'compatibility_level' " + compatLevel_curDb + dbVersionToStr(compatLevel_curDb) + " is less than the 'server level'.\n"
								+ "You may not take advantage of new functionality, which is available at this SQL-Server version... \n"
								+ "Server compatibility_level is: " + compatLevel_tempdb + dbVersionToStr(compatLevel_tempdb);

						strVal = "<div title=\""+tooltip+"\"> <font color='red'>" + strVal + "</font><div>";
					}
				}

				/**
				 * If 'collation_name' is not same as "tempdb", then mark the cell as RED
				 */
				if ("collation_name".equals(colName))
				{
					String collation_name_tempdb = "";
					for (int r=0; r<rstm.getRowCount(); r++)
					{
						String dbname = rstm.getValueAsString(r, "DBName");
						if ("tempdb".equals(dbname))
						{
							collation_name_tempdb = rstm.getValueAsString(r, "collation_name", true, "");
							break;
						}
					}
					String collation_name_curDb = strVal;
					if (collation_name_curDb != null && !collation_name_curDb.equals(collation_name_tempdb))
					{
						String tooltip = "Column 'collation_name' " + collation_name_curDb + " is different than 'tempdb'.\n"
								+ "This might give you substandard performance, due to implicit convertions... \n";

						strVal = "<div title=\""+tooltip+"\"> <font color='red'>" + strVal + "</font><div>";
					}
				}

				/**
				 * If 'LogSizeInMb' is *high*, then mark the cell as RED
				 * Larger than 'DataSizeInMb'
				 */
				if ("LogSizeInMb".equals(colName))
				{
					int    DataSizeInMb  = rstm.getValueAsInteger(row, "DataSizeInMb", true, -1);
					int    LogSizeInMb   = rstm.getValueAsInteger(row, "LogSizeInMb",  true, -1);
					String recoveryModel = rstm.getValueAsString (row, "recovery_model_desc");

					// Only check FULL recovery model
					if ( ! "FULL".equalsIgnoreCase(recoveryModel) )
						return strVal;

					// log size needs to bee above some value: 256 MB 
					if (LogSizeInMb < 256) 
						return strVal;
					
					// When LOG-SIZE is above DATA-SIZE  (take away 100 MB from the log, if it's "close" to DataSize)
					if ((LogSizeInMb - 100) > DataSizeInMb)
					{
						String tooltip = "Column 'LogSizeInMb' is high. You need to 'backup' or 'truncate' the transaction log every now and then.";
						strVal = "<div title=\""+tooltip+"\"> <font color='red'>" + strVal + "</font><div>";
					}
				}
				return strVal;
			}

			private String dbVersionToStr(int compatLevel)
			{
				switch (compatLevel)
				{
//				case 180: return compatLevel + " (SQL Server 2025)"; // just guessing here
//				case 170: return compatLevel + " (SQL Server 2023)"; // just guessing here
				case 160: return compatLevel + " (SQL Server 2021)"; // just guessing here
				case 150: return compatLevel + " (SQL Server 2019)";
				case 140: return compatLevel + " (SQL Server 2017)";
				case 130: return compatLevel + " (SQL Server 2016)";
				case 120: return compatLevel + " (SQL Server 2014)";
				case 110: return compatLevel + " (SQL Server 2012)";
				case 100: return compatLevel + " (SQL Server 2008 or 2008 R2)";
				case 90:  return compatLevel + " (SQL Server 2005)";
				case 80:  return compatLevel + " (SQL Server 2000)";
				}
				
				return compatLevel + " (unknown)";
			}
		}));


		// DAYS TO FULL REPORT
		boolean printDaysToFull = true;
		if (printDaysToFull)
		{
			sb.append("<br> \n");
			sb.append("<br> \n");

			if (StringUtil.hasValue(_dbxCentralSpaceFullCallError))
			{
				sb.append("<p style='color: red;'>Problems Calling DbxCentral to get 'long' (30d) Space History, falling back to 'local' (24h) history. <br>Problem: " + _dbxCentralSpaceFullCallError + "</p>");
			}

			sb.append(_daysToFullDataTableStr);
			sb.append("<br> \n");
			sb.append("To change Thresholds for <b>Critical</b> use property: <code>" + PROPKEY_DaysToFull_data_error   + " = ##</code><br> \n");
			sb.append("To Change Thresholds for <b>Warning</b>  use property: <code>" + PROPKEY_DaysToFull_data_warning + " = ##</code><br> \n");
			sb.append("<br> \n");

			sb.append(_daysToFullWalTableStr);
			sb.append("<br> \n");
			sb.append("To change Thresholds for <b>Critical</b> use property: <code>" + PROPKEY_DaysToFull_log_error   + " = ##</code><br> \n");
			sb.append("To Change Thresholds for <b>Warning</b>  use property: <code>" + PROPKEY_DaysToFull_log_warning + " = ##</code><br> \n");
			sb.append("<br> \n");
		}

		// link to DbxCentral graphs
		sb.append(getDbxCentralLinkWithDescForGraphs(false, "Below are Transaction Log and Data Size Usage of each Database during the day<br>\n"
		                                                  + "FIXME: Presented as: \n"
		                                                  + "<ul> \n"
		                                                  + "  <li>FIXME: <b> Space Used in Percent </b> - When this gets <b>high</b> we could be in trouble. But the below 'Space Available' is a better indicator.</li> \n"
		                                                  + "  <li>FIXME: <b> Space Available in MB </b> - When This gets <b>low</b> we could be in trouble. No space = No more modifications. </li> \n"
		                                                  + "  <li>FIXME: <b> Space Used in MB      </b> - Just an indicator of how much MB we are actually using for the different databases.</li> \n"
		                                                  + "</ul> \n",
				"CmDatabases_DbSizeMb",

				"CmDatabases_DbDataSizeUsedPctGraph",
				"CmDatabases_DbDataSizeLeftMbGraph",
				"CmDatabases_DbDataSizeUsedMbGraph",

				"CmDatabases_DbLogSizeUsedPctGraph",
				"CmDatabases_DbLogSizeLeftMbGraph",
				"CmDatabases_DbLogSizeUsedMbGraph",

				"CmDatabases_TempdbUsedMbGraph",
		
				"CmDatabases_OsDiskUsedPct",
				"CmDatabases_OsDiskFreeMb",
				"CmDatabases_OsDiskUsedMb"
				));

		sb.append("<h4>DB Size</h4> \n");
		_CmDatabases_DbSizeMb              .writeHtmlContent(sb, null, null);

		sb.append("<h4>DB Data Space Usage</h4> \n");
		_CmDatabases_DbDataSizeUsedPctGraph.writeHtmlContent(sb, null, null);
		_CmDatabases_DbDataSizeLeftMbGraph .writeHtmlContent(sb, null, null);
		_CmDatabases_DbDataSizeUsedMbGraph .writeHtmlContent(sb, null, null);
		
		sb.append("<h4>DB Transaction Log Space Usage</h4> \n");
		_CmDatabases_DbLogSizeUsedPctGraph .writeHtmlContent(sb, null, null);
		_CmDatabases_DbLogSizeLeftMbGraph  .writeHtmlContent(sb, null, null);
		_CmDatabases_DbLogSizeUsedMbGraph  .writeHtmlContent(sb, null, null);

		sb.append("<h4>Tempdb Space Usage</h4> \n");
		_CmDatabases_TempdbUsedMbGraph     .writeHtmlContent(sb, null, null);
		
		sb.append("<h4>DB OS Disk Space Usage (if we get near full here, we are in trouble)</h4> \n");
		_CmDatabases_OsDiskUsedPct         .writeHtmlContent(sb, null, null);
		_CmDatabases_OsDiskFreeMb          .writeHtmlContent(sb, null, null);
		_CmDatabases_OsDiskUsedMb          .writeHtmlContent(sb, null, null);
	}

	@Override
	public String getSubject()
	{
		return "Database Size in MB (origin: CmDatabases)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}


	@Override
	public String[] getMandatoryTables()
	{
		return new String[] { "CmDatabases_abs" };
	}

	private void getDaysToDiskFull_fromLocalRecording(DbxConnection conn, String schema)
	throws Exception
	{
		try
		{
			int dataErrorThreshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_DaysToFull_data_error  , DEFAULT_DaysToFull_data_error);
			int dataWarnThreshold  = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_DaysToFull_data_warning, DEFAULT_DaysToFull_data_warning);

			int logErrorThreshold  = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_DaysToFull_log_error  , DEFAULT_DaysToFull_log_error);
			int logWarnThreshold   = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_DaysToFull_log_warning, DEFAULT_DaysToFull_log_warning);

			// Create the DATA predictor instance
			SpaceFullPredictor dataFullPredictor = new SpaceFullPredictor(conn)
					.setCriticalThresholdDays(dataErrorThreshold)
					.setWarningThresholdDays (dataWarnThreshold);

			// Create the WAL predictor instance
			SpaceFullPredictor walFullPredictor = new SpaceFullPredictor(conn)
					.setCriticalThresholdDays(logErrorThreshold)
					.setWarningThresholdDays (logWarnThreshold);

			// Combined results
			Set<PredictionResult> allResults = new LinkedHashSet<>();			

			// DATA
			Set<PredictionResult> dataResult = dataFullPredictor.predictSpaceFull(
					SourceDataSize.MB,
					"DBName", 
					"'DATA'", // NOTE: Note a column but a constant
					"SessionSampleTime", 
					"DataSizeInMb",
					"DataSizeUsedInMb", 
					"DataSizeFreeInMb", 
					schema, 
					"CmDatabases_abs");

			allResults.addAll(dataResult);

			// And write the report to a String that will be added by method: writeMessageText(...)
			_daysToFullDataTableStr = dataFullPredictor.generateHtmlReport(dataResult, false, false, 
					"Below is a table trying to estimate how many days we have left until the <b>DATA</b> gets full.<br>" +
					"Note: Databases with AutoGrow don't have to take CRITICAL and WARNINGS seriously. But if it's turned OFF, you will have a full DATA file at that point. " + 
					"<b>No</b> Alarms will be sent if/when CRITICAL is reached. ");

			// LOG/WALL
			Set<PredictionResult> walResult = walFullPredictor.predictSpaceFull(
					SourceDataSize.MB,
					"DBName", 
					"'LOG'", // NOTE: Note a column but a constant
					"SessionSampleTime", 
					"LogSizeInMb",
					"LogSizeUsedInMb", 
					"LogSizeFreeInMb", 
					schema, 
					"CmDatabases_abs");

			allResults.addAll(walResult);

			// And write the report to a String that will be added by method: writeMessageText(...)
			_daysToFullWalTableStr = walFullPredictor.generateHtmlReport(walResult, false, false, 
					"Below is a table trying to estimate how many days we have left until the <b>LOG/WAL</b> gets full." +
					"<br>Note: Databases with AutoGrow don't have to take CRITICAL and WARNINGS seriously. But if it's turned OFF, you will have a full LOG/WAL file at that point." +
					"<b>No</b> Alarms will be sent if/when CRITICAL is reached. ");
			

			// Create Alarms on any Critical Alerts (from ALL results) NOTE: if we JUST want for DATA or WAL, change 'allResults' --> 'dataResult' || 'walResult'
			boolean doCheckAlarms = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_DaysToFull_alarmIsEnable, DEFAULT_DaysToFull_alarmIsEnable);
			if (doCheckAlarms)
			{
				List<String> skipDbNames = StringUtil.parseCommaStrToList(Configuration.getCombinedConfiguration().getProperty(PROPKEY_DaysToFull_SkipDbnames, DEFAULT_DaysToFull_SkipDbnames), true);

				Set<PredictionResult> severityCriticals = SpaceFullPredictor.getSeverityCriticals(dataResult);
				Set<PredictionResult> severityWarnings  = SpaceFullPredictor.getSeverityWarnings (dataResult);
				Set<PredictionResult> severityOk        = SpaceFullPredictor.getSeverityOk       (dataResult);
				
				_logger.info("Resources Severity Count: Criticals=" + severityCriticals.size() + ", Warnings=" + severityWarnings.size() + ", OK=" + severityOk.size() + ".");
				
				if ( ! severityCriticals.isEmpty() )
				{
					for (PredictionResult pr : severityCriticals)
					{
						boolean doAlarm = true;

						String dbname = pr._resourceName;
						if (StringUtil.matchesAny(dbname, skipDbNames))
						{
							doAlarm = false;
						}

						if (doAlarm)
						{
							if (AlarmHandler.hasInstance())
							{
								String srvName = getReportingInstance().getDbmsServerName();
								
								String extraAscii = pr.toString();
								String extraHtml  = pr.toHtmlKeyValueTable(null);

								SpaceType spaceType = "DATA".equals(pr._secondaryName) ? SpaceType.DBMS_DATA : SpaceType.DBMS_WAL;
								
//								int threshold = "DATA".equals(pr._secondaryName) ? dataFullPredictor.getCriticalThresholdHours() : walFullPredictor.getCriticalThresholdHours();
								int threshold = SpaceType.DBMS_DATA.equals(spaceType) ? dataFullPredictor.getCriticalThresholdHours() : walFullPredictor.getCriticalThresholdHours();

								AlarmEvent ae = new AlarmEventSpaceFullPrediction(
										srvName, 
										"DSR:" + this.getClass().getSimpleName(), 
										AlarmEvent.Severity.ERROR, 
										spaceType,
										pr._resourceName, 
										pr._secondaryName, 
										pr._hoursUntilFull, 
										threshold);

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
		}
		catch (Exception ex)
		{
			// TODO: Should we LOG this to '_logger' or to 'DailySummaryReport' ???
			// For now just "put" the stacktrace as the "Table" 
			//ex.printStackTrace();
//			_daysToFullDataTableStr = StringUtil.toHtmlPre(StringUtil.stackTraceToString(ex));
			throw ex;
		}
	}		

	private void getDaysToDiskFull_fromDbxCentral(String srvName) 
	throws Exception
	{
	    try
		{
			int dataErrorThreshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_DaysToFull_data_error  , DEFAULT_DaysToFull_data_error);
			int dataWarnThreshold  = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_DaysToFull_data_warning, DEFAULT_DaysToFull_data_warning);

			int logErrorThreshold  = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_DaysToFull_log_error  , DEFAULT_DaysToFull_log_error);
			int logWarnThreshold   = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_DaysToFull_log_warning, DEFAULT_DaysToFull_log_warning);

			int days               = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_DaysToFull_historicalDays, DEFAULT_DaysToFull_historicalDays);
			int sampleMinutes      = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_DaysToFull_sampleMinutes , DEFAULT_DaysToFull_sampleMinutes);
			
			//  Get URL's to be used in below messages
			String dataForcasterUrl = getDbxCentralSpaceForecastUrl(srvName, days, sampleMinutes, SpaceType.DBMS_DATA, SpaceForecastServlet.OUTPUT_TYPE_HTML_PAGE);
			String walForcasterUrl  = getDbxCentralSpaceForecastUrl(srvName, days, sampleMinutes, SpaceType.DBMS_WAL,  SpaceForecastServlet.OUTPUT_TYPE_HTML_PAGE);

			// Make the REST Call and get data as an Object
			SpaceForecast dataForcaster = dbxCentralSpaceForecastRestCall(srvName, days, sampleMinutes, SpaceType.DBMS_DATA);
			SpaceForecast walForcaster  = dbxCentralSpaceForecastRestCall(srvName, days, sampleMinutes, SpaceType.DBMS_WAL);

			dataForcaster.setCriticalThresholdDays(dataErrorThreshold);
			dataForcaster.setWarningThresholdDays (dataWarnThreshold);

			walForcaster.setCriticalThresholdDays(logErrorThreshold);
			walForcaster.setWarningThresholdDays (logWarnThreshold);
			
			// And write the report to a String that will be added by method: writeMessageText(...)
			_daysToFullDataTableStr = dataForcaster.generateHtmlReport(false, 
					"Below is a table trying to estimate how many days we have left until the <b>DATA</b> gets full.<br>" +
					"Note: Databases with AutoGrow don't have to take CRITICAL and WARNINGS seriously. But if it's turned OFF, you will have a full DATA file at that point. " + 
					"<b>No</b> Alarms will be sent if/when CRITICAL is reached. " +
					"<a href='" + dataForcasterUrl + "' target='_blank'>Check Prediction Again</a>");

			// And write the report to a String that will be added by method: writeMessageText(...)
			_daysToFullWalTableStr = walForcaster.generateHtmlReport(false, 
					"Below is a table trying to estimate how many days we have left until the <b>LOG/WAL</b> gets full." +
					"<br>Note: Databases with AutoGrow don't have to take CRITICAL and WARNINGS seriously. But if it's turned OFF, you will have a full LOG/WAL file at that point." +
					"<b>No</b> Alarms will be sent if/when CRITICAL is reached. " +
					"<a href='" + walForcasterUrl + "' target='_blank'>Check Prediction Again</a>");
			

			// Create Alarms on any Critical Alerts (from ALL results) NOTE: if we JUST want for DATA or WAL, change 'allResults' --> 'dataResult' || 'walResult'
			boolean doCheckAlarms = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_DaysToFull_alarmIsEnable, DEFAULT_DaysToFull_alarmIsEnable);
			if (doCheckAlarms)
			{
				List<String> skipDbNames = StringUtil.parseCommaStrToList(Configuration.getCombinedConfiguration().getProperty(PROPKEY_DaysToFull_SkipDbnames, DEFAULT_DaysToFull_SkipDbnames), true);

				List<SpaceForecastResult> severityCriticals = new ArrayList<>();
				List<SpaceForecastResult> severityWarnings  = new ArrayList<>();
				List<SpaceForecastResult> severityOthers    = new ArrayList<>();

				// Add DATA Severities
				severityCriticals.addAll(dataForcaster.getSeverityCriticals());
				severityWarnings .addAll(dataForcaster.getSeverityWarnings());
				severityOthers   .addAll(dataForcaster.getSeverityOthers());
				
				// Add WAL Severities
				severityCriticals.addAll(walForcaster.getSeverityCriticals());
				severityWarnings .addAll(walForcaster.getSeverityWarnings());
				severityOthers   .addAll(walForcaster.getSeverityOthers());
				

				_logger.info("Resources Severity Count: Criticals=" + severityCriticals.size() + ", Warnings=" + severityWarnings.size() + ", OK=" + severityOthers.size() + ".");
				
				if ( ! severityCriticals.isEmpty() )
				{
					for (SpaceForecastResult sfr : severityCriticals)
					{
						boolean doAlarm = true;

						String dbname = sfr.mount;
						if (StringUtil.matchesAny(dbname, skipDbNames))
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
								
								int threshold = SpaceType.DBMS_DATA.equals(sfr.spaceType) ? dataForcaster.getCriticalThresholdHours() : walForcaster.getCriticalThresholdHours();

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
//			_daysToFullDataTableStr = StringUtil.toHtmlPre(StringUtil.stackTraceToString(ex));
			
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
				int days = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_DaysToFull_historicalDays, DEFAULT_DaysToFull_historicalDays);
				
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
				_daysToFullDataTableStr = StringUtil.toHtmlPre(StringUtil.stackTraceToString(ex));
			}
		}
	}

	

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		// just to get Column names
		String dummySql = "select * from [CmDatabases_abs] where 1 = 2";
		ResultSetTableModel dummyRstm = executeQuery(conn, dummySql, false, "metadata");

		String collation_name = "";
		if (dummyRstm.hasColumn("collation_name"))
		{
			collation_name = "    ,[collation_name] \n";
		}
		
		String sql = ""
			    + "select \n"
			    + "     [DBName] \n"
			    + "    ,[compatibility_level] \n"
			    + "    ,[recovery_model_desc] \n"
			    + collation_name
//			    + "    ,[DataFileGroupCount] \n"
			    + "\n"
			    + "    ,[DbSizeInMb] \n"
			    + "    ,[DataSizeInMb] \n"
			    + "    ,[LogSizeInMb] \n"
			    + "\n"
			    + "    ,[DataSizeUsedPct] \n"
			    + "    ,[DataSizeFreeInMb] \n"
			    + "    ,[DataSizeUsedInMb] \n"
			    + "\n"
			    + "    ,[LogSizeUsedPct] \n"
			    + "    ,[LogSizeFreeInMb] \n"
			    + "    ,[LogSizeUsedInMb] \n"
			    + "\n"
			    + "    ,[DataOsDisk] \n"
			    + "    ,[DataOsDiskFreeMb] \n"
			    + "    ,[DataOsDiskUsedMb] \n"
			    + "    ,[DataOsDiskFreePct] \n"
			    + "    ,[DataOsDiskUsedPct] \n"
			    + "    ,[DataNextGrowthSizeMb] \n"
			    + "    ,[LastDbBackupTime] \n"
			    + "    ,[LastDbBackupAgeInHours] \n"
			    + "\n"
			    + "    ,[LogOsDisk] \n"
			    + "    ,[LogOsDiskFreeMb] \n"
			    + "    ,[LogOsDiskUsedMb] \n"
			    + "    ,[LogOsDiskFreePct] \n"
			    + "    ,[LogOsDiskUsedPct] \n"
			    + "    ,[LogNextGrowthSizeMb] \n"
			    + "    ,[LastLogBackupTime] \n"
			    + "    ,[LastLogBackupAgeInHours] \n"
			    + "\n"
			    + "    ,[DataOsFileName] \n"
			    + "    ,[LogOsFileName] \n"
			    + "from [CmDatabases_abs] x \n"
			    + "where [SessionSampleTime] = (select max([SessionSampleTime]) from [CmDatabases_abs]) \n"
			    + "order by [DbSizeInMb] desc \n"
			    + "";
		
		_shortRstm = executeQuery(conn, sql, true, "CmDatabases_abs");

		// Highlight sort column
		_shortRstm.setHighlightSortColumns("DbSizeInMb");

		// Describe the table
		setSectionDescription(_shortRstm);
		
		// Create HTML table for "Days To Full DATA/LOG"
		getDaysToDiskFull(conn);

		String schema = getReportingInstance().getDbmsSchemaName(); // This is probably NULL for a DBMS Collector
		
		_CmDatabases_DbSizeMb               = createTsLineChart(conn, schema, CmDatabases.CM_NAME, CmDatabases.GRAPH_NAME_DB_SIZE_MB         , -1,  true , null, "Database Size in MB (Server->Databases)");

		_CmDatabases_DbDataSizeUsedPctGraph = createTsLineChart(conn, schema, CmDatabases.CM_NAME, CmDatabases.GRAPH_NAME_DATASIZE_USED_PCT  , 100, true , null, "DB Data Space Used in Percent (Server->Databases)");
		_CmDatabases_DbDataSizeLeftMbGraph  = createTsLineChart(conn, schema, CmDatabases.CM_NAME, CmDatabases.GRAPH_NAME_DATASIZE_LEFT_MB   , -1,  true , null, "DB Data Space Available in MB (Server->Databases)");
		_CmDatabases_DbDataSizeUsedMbGraph  = createTsLineChart(conn, schema, CmDatabases.CM_NAME, CmDatabases.GRAPH_NAME_DATASIZE_USED_MB   , -1,  true , null, "DB Data Space Used in MB (Server->Databases)");

		_CmDatabases_DbLogSizeUsedPctGraph  = createTsLineChart(conn, schema, CmDatabases.CM_NAME, CmDatabases.GRAPH_NAME_LOGSIZE_USED_PCT   , 100, true , null, "DB Transaction Log Space Used in Percent (Server->Databases)");
		_CmDatabases_DbLogSizeLeftMbGraph   = createTsLineChart(conn, schema, CmDatabases.CM_NAME, CmDatabases.GRAPH_NAME_LOGSIZE_LEFT_MB    , -1,  true , null, "DB Transaction Log Space Available in MB (Server->Databases)");
		_CmDatabases_DbLogSizeUsedMbGraph   = createTsLineChart(conn, schema, CmDatabases.CM_NAME, CmDatabases.GRAPH_NAME_LOGSIZE_USED_MB    , -1,  true , null, "DB Transaction Log Space Used in MB (Server->Databases)");

		_CmDatabases_TempdbUsedMbGraph      = createTsLineChart(conn, schema, CmDatabases.CM_NAME, CmDatabases.GRAPH_NAME_TEMPDB_USED_MB     , -1,  false, null, "TempDB Space used in MB (Server->Databases)");

		_CmDatabases_OsDiskUsedPct          = createTsLineChart(conn, schema, CmDatabases.CM_NAME, CmDatabases.GRAPH_NAME_OS_DISK_USED_PCT   , 100, false, null, "DB OS Disk Space Used in Percent (Server->Databases)");
		_CmDatabases_OsDiskFreeMb           = createTsLineChart(conn, schema, CmDatabases.CM_NAME, CmDatabases.GRAPH_NAME_OS_DISK_FREE_MB    , -1,  false, null, "DB OS Disk Space Available in MB (Server->Databases)");
		_CmDatabases_OsDiskUsedMb           = createTsLineChart(conn, schema, CmDatabases.CM_NAME, CmDatabases.GRAPH_NAME_OS_DISK_USED_MB    , -1,  false, null, "DB OS Disk Space Used in MB (Server->Databases)");
	}

	private IReportChart _CmDatabases_DbSizeMb;

	private IReportChart _CmDatabases_DbDataSizeUsedPctGraph;
	private IReportChart _CmDatabases_DbDataSizeLeftMbGraph;
	private IReportChart _CmDatabases_DbDataSizeUsedMbGraph;

	private IReportChart _CmDatabases_DbLogSizeUsedPctGraph;
	private IReportChart _CmDatabases_DbLogSizeLeftMbGraph;
	private IReportChart _CmDatabases_DbLogSizeUsedMbGraph;
	
	private IReportChart _CmDatabases_TempdbUsedMbGraph;

	private IReportChart _CmDatabases_OsDiskUsedPct;
	private IReportChart _CmDatabases_OsDiskFreeMb;
	private IReportChart _CmDatabases_OsDiskUsedMb;


	/**
	 * Set descriptions for the table, and the columns
	 */
	private void setSectionDescription(ResultSetTableModel rstm)
	{
		if (rstm == null)
			return;
		
		// Section description
		rstm.setDescription(
				"Information from last collector sample from the table <code>CmDatabases_abs</code><br>" +
				"This will show you sizes of all databases and there last Usage, on Data and Log <br>" +
				"");
	}
}

