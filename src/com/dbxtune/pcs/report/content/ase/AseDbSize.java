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
package com.dbxtune.pcs.report.content.ase;

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
import com.dbxtune.cm.ase.CmOpenDatabases;
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

public class AseDbSize 
extends AseAbstract
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private ResultSetTableModel _shortRstm;

	public AseDbSize(DailySummaryReportAbstract reportingInstance)
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

	public static final String PROPKEY_DaysToFull_data_error   = "AseDbSize.alarm.DaysToFull.data.error";
	public static final int    DEFAULT_DaysToFull_data_error   = 7;

	public static final String PROPKEY_DaysToFull_data_warning = "AseDbSize.alarm.DaysToFull.data.warning";
	public static final int    DEFAULT_DaysToFull_data_warning = 14;

	public static final String PROPKEY_DaysToFull_log_error     = "AseDbSize.alarm.DaysToFull.log.error";
	public static final int    DEFAULT_DaysToFull_log_error     = 7;

	public static final String PROPKEY_DaysToFull_log_warning   = "AseDbSize.alarm.DaysToFull.log.warning";
	public static final int    DEFAULT_DaysToFull_log_warning   = 14;

	/** This is a CSV list of names... The list can contain regular expressions */
	public static final String PROPKEY_DaysToFull_SkipDbnames   = "AseDbSize.alarm.DaysToFull.skip.dbnames";
	public static final String DEFAULT_DaysToFull_SkipDbnames   = "";
	
	public static final String  PROPKEY_DaysToFull_alarmIsEnable = "AseDbSize.alarm.DaysToFull.enable";
	public static final boolean DEFAULT_DaysToFull_alarmIsEnable = true;

	public static final String PROPKEY_DaysToFull_historicalDays = "AseDbSize.dbxCentral.DaysToFull.days";
	public static final int    DEFAULT_DaysToFull_historicalDays = 30;

	public static final String PROPKEY_DaysToFull_sampleMinutes = "AseDbSize.dbxCentral.DaysToFull.sampleMinutes";
	public static final int    DEFAULT_DaysToFull_sampleMinutes = 60;

//	private ResultSetTableModel _daysToFullRstm;
	private String              _dbxCentralSpaceFullCallError = "";
	private String              _daysToFullDataTableStr = "";
	private String              _daysToFullWalTableStr  = "";
	

	@Override
	public void writeMessageText(Writer sb, MessageType messageType)
	throws IOException
	{
		// Get a description of this section, and column names
		sb.append(getSectionDescriptionHtml(_shortRstm, true));

		// Last sample Database Size info
		sb.append("Row Count: " + _shortRstm.getRowCount() + "<br>\n");
		sb.append(toHtmlTable(_shortRstm));

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
		                                                  + "Presented as: \n"
		                                                  + "<ul> \n"
		                                                  + "  <li><b> Space Used in Percent </b> - When this gets <b>high</b> we could be in trouble. But the below 'Space Available' is a better indicator.</li> \n"
		                                                  + "  <li><b> Space Available in MB </b> - When This gets <b>low</b> we could be in trouble. No space = No more modifications. </li> \n"
		                                                  + "  <li><b> Space Used in MB      </b> - Just an indicator of how much MB we are actually using for the different databases.</li> \n"
		                                                  + "</ul> \n",
				"CmOpenDatabases_DbDataSizeUsedPctGraph",
				"CmOpenDatabases_DbDataSizeLeftMbGraph",
				"CmOpenDatabases_DbDataSizeUsedMbGraph",
				
				"CmOpenDatabases_DbLogSizeUsedPctGraph",
				"CmOpenDatabases_DbLogSizeLeftMbGraph",
				"CmOpenDatabases_DbLogSizeUsedMbGraph",

				"CmOpenDatabases_TempdbUsedMbGraph"
				));

		sb.append("<h4>DB Data Space Usage</h4> \n");
		_CmOpenDatabases_DbDataSizeUsedPctGraph.writeHtmlContent(sb, null, null);
		_CmOpenDatabases_DbDataSizeLeftMbGraph .writeHtmlContent(sb, null, null);
		_CmOpenDatabases_DbDataSizeUsedMbGraph .writeHtmlContent(sb, null, null);
		
		sb.append("<h4>DB Transaction Log Space Usage</h4> \n");
		_CmOpenDatabases_DbLogSizeUsedPctGraph .writeHtmlContent(sb, null, null);
		_CmOpenDatabases_DbLogSizeLeftMbGraph  .writeHtmlContent(sb, null, null);
		_CmOpenDatabases_DbLogSizeUsedMbGraph  .writeHtmlContent(sb, null, null);

		sb.append("<h4>Tempdb Space Usage</h4> \n");
		_CmOpenDatabases_TempdbUsedMbGraph     .writeHtmlContent(sb, null, null);
	}

	@Override
	public String getSubject()
	{
		return "Database Size in MB (origin: CmOpenDatabases / monOpenDatabases)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}


	@Override
	public String[] getMandatoryTables()
	{
		return new String[] { "CmOpenDatabases_abs" };
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
					"CmOpenDatabases_abs");

			allResults.addAll(dataResult);

			// And write the report to a String that will be added by method: writeMessageText(...)
			_daysToFullDataTableStr = dataFullPredictor.generateHtmlReport(dataResult, false, false, 
					"Below is a table trying to estimate how many days we have left until the <b>DATA</b> gets full." + 
					"Alarms will be sent if/when CRITICAL is reached.");
			
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
					"CmOpenDatabases_abs");

			allResults.addAll(walResult);

			// And write the report to a String that will be added by method: writeMessageText(...)
			_daysToFullWalTableStr = walFullPredictor.generateHtmlReport(walResult, false, false, 
					"Below is a table trying to estimate how many days we have left until the <b>LOG/WAL</b> gets full." +
					"Alarms will be sent if/when CRITICAL is reached.");
			

			// Create Alarms on any Critical Alerts (from ALL results) NOTE: if we JUST want for DATA or WAL, change 'allResults' --> 'dataResult' || 'walResult'
			boolean doCheckAlarms = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_DaysToFull_alarmIsEnable, DEFAULT_DaysToFull_alarmIsEnable);
			if (doCheckAlarms)
			{
				List<String> skipDbNames = StringUtil.parseCommaStrToList(Configuration.getCombinedConfiguration().getProperty(PROPKEY_DaysToFull_SkipDbnames, DEFAULT_DaysToFull_SkipDbnames), true);

				Set<PredictionResult> severityCriticals = SpaceFullPredictor.getSeverityCriticals(allResults);
				Set<PredictionResult> severityWarnings  = SpaceFullPredictor.getSeverityWarnings (allResults);
				Set<PredictionResult> severityOk        = SpaceFullPredictor.getSeverityOk       (allResults);
				
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
					"Below is a table trying to estimate how many days we have left until the <b>DATA</b> gets full. " + 
					"Alarms will be sent if/when CRITICAL is reached. " +
					"<a href='" + dataForcasterUrl + "' target='_blank'>Check Prediction Again</a>");

			// And write the report to a String that will be added by method: writeMessageText(...)
			_daysToFullWalTableStr = walForcaster.generateHtmlReport(false, 
					"Below is a table trying to estimate how many days we have left until the <b>LOG/WAL</b> gets full. " +
					"Alarms will be sent if/when CRITICAL is reached. " +
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
		String sql = ""
			    + "select \n"
			    + "    [DBName], \n"
//			    + "    [DBID], \n"
//			    + "    [IsUserTempdb], \n"
//			    + "    [TransactionLogFull], \n"
			    + "\n"
			    + "    [DbSizeInMb], \n"
			    + "    [DataSizeInMb], \n"
			    + "    [LogSizeInMb], \n"
			    + "    [LogDataIsMixed], \n"
			    + "\n"
			    + "    [DataSizeUsedPct], \n"
			    + "    [DataSizeFreeInMb], \n"
			    + "    [DataSizeUsedInMb], \n"
			    + "\n"
			    + "    [LogSizeUsedPct], \n"
			    + "    [LogSizeFreeInMb], \n"
			    + "    [LogSizeUsedInMb], \n"
			    + "\n"
			    + "    [LastBackupFailed], \n"
			    + "    [BackupStartTime], \n"
			    + "    [LastTranLogDumpTime], \n"
			    + "    [LastDbBackupAgeInHours], \n"
			    + "    [LastLogBackupAgeInHours] \n"
			    + "\n"
			    + "from [CmOpenDatabases_abs] \n"
			    + "where [SessionSampleTime] = (select max([SessionSampleTime]) from [CmOpenDatabases_abs]) \n"
			    + "order by [DbSizeInMb] desc \n"
			    + "";
		
		_shortRstm = executeQuery(conn, sql, true, "CmOpenDatabases_abs");

		// Highlight sort column
		if (_shortRstm != null)
			_shortRstm.setHighlightSortColumns("DbSizeInMb");
			
		// Describe the table
		setSectionDescription(_shortRstm);

		// Create HTML table for "Days To Full DATA/LOG"
		getDaysToDiskFull(conn);

		String schema = getReportingInstance().getDbmsSchemaName(); // This is probably NULL for a DBMS Collector
		
		_CmOpenDatabases_DbDataSizeUsedPctGraph = createTsLineChart(conn, schema, CmOpenDatabases.CM_NAME, CmOpenDatabases.GRAPH_NAME_DATASIZE_USED_PCT, 100, true, null, "DB Data Space Used in Percent (Server->Databases)");
		_CmOpenDatabases_DbDataSizeLeftMbGraph  = createTsLineChart(conn, schema, CmOpenDatabases.CM_NAME, CmOpenDatabases.GRAPH_NAME_DATASIZE_LEFT_MB,   -1, true, null, "DB Data Space Available in MB (Server->Databases)");
		_CmOpenDatabases_DbDataSizeUsedMbGraph  = createTsLineChart(conn, schema, CmOpenDatabases.CM_NAME, CmOpenDatabases.GRAPH_NAME_DATASIZE_USED_MB,   -1, true, null, "DB Data Space Used in MB (Server->Databases)");

		_CmOpenDatabases_DbLogSizeUsedPctGraph  = createTsLineChart(conn, schema, CmOpenDatabases.CM_NAME, CmOpenDatabases.GRAPH_NAME_LOGSIZE_USED_PCT,  100, true, null, "DB Transaction Log Space Used in Percent (Server->Databases)");
		_CmOpenDatabases_DbLogSizeLeftMbGraph   = createTsLineChart(conn, schema, CmOpenDatabases.CM_NAME, CmOpenDatabases.GRAPH_NAME_LOGSIZE_LEFT_MB,    -1, true, null, "DB Transaction Log Space Available in MB (Server->Databases)");
		_CmOpenDatabases_DbLogSizeUsedMbGraph   = createTsLineChart(conn, schema, CmOpenDatabases.CM_NAME, CmOpenDatabases.GRAPH_NAME_LOGSIZE_USED_MB,    -1, true, null, "DB Transaction Log Space Used in MB (Server->Databases)");

		_CmOpenDatabases_TempdbUsedMbGraph      = createTsLineChart(conn, schema, CmOpenDatabases.CM_NAME, CmOpenDatabases.GRAPH_NAME_TEMPDB_USED_MB,     -1, true, null, "TempDB Space Used in MB (Server->Databases)");
	}

	private IReportChart _CmOpenDatabases_DbLogSizeUsedPctGraph;
	private IReportChart _CmOpenDatabases_DbLogSizeLeftMbGraph;
	private IReportChart _CmOpenDatabases_DbLogSizeUsedMbGraph;
	
	private IReportChart _CmOpenDatabases_DbDataSizeUsedPctGraph;
	private IReportChart _CmOpenDatabases_DbDataSizeLeftMbGraph;
	private IReportChart _CmOpenDatabases_DbDataSizeUsedMbGraph;

	private IReportChart _CmOpenDatabases_TempdbUsedMbGraph;

	/**
	 * Set descriptions for the table, and the columns
	 */
	private void setSectionDescription(ResultSetTableModel rstm)
	{
		if (rstm == null)
			return;
		
		// Section description
		rstm.setDescription(
				"Information from last collector sample from the table <code>CmOpenDatabases_abs</code><br>" +
				"This will show you sizes of all databases and there last Usage, on Data and Log <br>" +
				"");
	}
}

