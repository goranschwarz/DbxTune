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
package com.asetune.pcs.report.content.sqlserver;

import java.io.IOException;
import java.io.Writer;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.log4j.Logger;

import com.asetune.gui.ResultSetTableModel;
import com.asetune.pcs.SqlServerJobSchedulerExtractor;
import com.asetune.pcs.report.DailySummaryReportAbstract;
import com.asetune.pcs.report.content.SparklineHelper;
import com.asetune.pcs.report.content.SparklineHelper.SparklineResult;
import com.asetune.pcs.report.content.SparklineJfreeChart;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;
import com.asetune.utils.DbUtils;
import com.asetune.utils.StringUtil;
import com.asetune.utils.TimeUtils;

public class SqlServerJobScheduler 
extends SqlServerAbstract
{
	private static Logger _logger = Logger.getLogger(SqlServerJobScheduler.class);

	public static final String PROPKEY_errors_skip_msg_numbers = "DailySummaryReport.SqlServerJobScheduler.skip.msg.numbers.csv";
	public static final String DEFAULT_errors_skip_msg_numbers = "0, 1945, 8153, 15477, 50000";
	
	public static final String PROPKEY_errors_skip_below_severity = "DailySummaryReport.SqlServerJobScheduler.skip.severity.below";
	public static final int    DEFAULT_errors_skip_below_severity = -1;
	
	private List<String>        _miniChartJsList = new ArrayList<>();
	
	private boolean _pcsSchemaOrTableWasNotFound = false;

	private ResultSetTableModel _sysjobs     = null;
	private ResultSetTableModel _sysjobsteps = null;

	private ResultSetTableModel _job_history_overview   = null;
	private ResultSetTableModel _job_history_outliers   = null;
	private ResultSetTableModel _job_history_errors     = null;
	private ResultSetTableModel _job_history_errors_all = null;

	/** Key=job_id, Val=CommandsToShowInTooltip */
	private Map<String, String> _jobCommandsMap = null;
	
	
	public SqlServerJobScheduler(DailySummaryReportAbstract reportingInstance)
	{
		super(reportingInstance);
	}

	@Override
	public String getSubject()
	{
		return "Job Agent/Scheduler Information for the full day (origin: msdb.dbo.sysjobs, sysjobsteps, sysjobhistory)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		// If no tables or schema was found: _job_history_outliers & _job_history_errors will be NULL, so get out early...
		if (_pcsSchemaOrTableWasNotFound)
			return false;

		// If we have got any: Outliers
		if (_job_history_outliers != null && _job_history_outliers.getRowCount() > 0)
			return true;

		// If we have got any: Errors
		if (_job_history_errors != null && _job_history_errors.getRowCount() > 0)
			return true;

		return false;
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

	@Override
	public void writeMessageText(Writer w, MessageType messageType)
	throws IOException
	{

		// If the schema/table did not exists... It may be to the fact:
		//  * Job Agent/Scheduler is not enabled
		//  * The user used to monitor SQL Server did not have access to table: msdb.dbo.sysjobhistory 
		if (_pcsSchemaOrTableWasNotFound)
		{
			w.append("<p style='color:red'><b>No Job Scheduler schema or table was found in the recordings database.</b></p>"
					+ "<p>This may be to the following reasons:"
					+ "  <ul>"
					+ "    <li>Job Agent/Scheduler is not enabled on this SQL Server</li>"
					+ "    <li>The login used to monitor SQL Server did not have access to table: <code>msdb.dbo.sysjobhistory</code></li>"
					+ "  </ul>"
					+ "</p>");
			return;
		}
		
		
		boolean hasOverviewRecords = true;
		HtmlTableRenderer htmlTableRenderer = new HtmlTableRenderer();
		
		//------------------------------------------------------------------------
		// get: Summary
		//------------------------------------------------------------------------
		beginHtmlSubSection(w, "Summary of All jobs in the Reporting Period", "job_scheduler_overview");
		
		if (_job_history_overview.getRowCount() > 0)
		{
			hasOverviewRecords = true;
			
			int overview_calcHistoricalDays = Configuration.getCombinedConfiguration().getIntProperty(SqlServerJobSchedulerExtractor.PROPKEY_overview_calcHistoricalDays, SqlServerJobSchedulerExtractor.DEFAULT_overview_calcHistoricalDays);

//			w.append("<p>The summary table is ordered by 'avgTimeInSec'...</p> \n");
			w.append("<p>The summary table is ordered by 'sumTimeInSec'...</p> \n");
			w.append("<p>The Historical Calculation (last columns) are based on the last " + overview_calcHistoricalDays + " days. <i>This can be changed with property <code>" + SqlServerJobSchedulerExtractor.PROPKEY_overview_calcHistoricalDays + " = ##</code></i></p>");
			w.append(_job_history_overview.toHtmlTableString("sortable", true, true, null, htmlTableRenderer));
		}
		else
		{
			hasOverviewRecords = false;
			w.append("<p>NO Jobs has been executed in the recording period...<br><br></p>");
		}
		endHtmlSubSection(w);


		// If we Job executions, write some more info
		if (hasOverviewRecords)
		{
			//------------------------------------------------------------------------
			// get: Outliers
			//------------------------------------------------------------------------
			beginHtmlSubSection(w, "Job Steps with OUTLIERS", "job_step_outliers");
			w.append("<p>Outliers are the job steps that takes <b>longer</b> than the normal execution time.</p> \n");

			int outliers_calcHistoricalDays = Configuration.getCombinedConfiguration().getIntProperty(SqlServerJobSchedulerExtractor.PROPKEY_outliers_calcHistoricalDays, SqlServerJobSchedulerExtractor.DEFAULT_outliers_calcHistoricalDays);

			if (_job_history_outliers.getRowCount() > 0)
			{
				w.append("<p>The algorithm to find <i>outliers</i> is: "
						+ "<ul>"
						+ "  <li>Average execution time for the last " + outliers_calcHistoricalDays + " days. <i>This can be changed with property <code>" + SqlServerJobSchedulerExtractor.PROPKEY_outliers_calcHistoricalDays + " = ##</code></i>"
								+ "<br>Note: check column 'histDaysCount' to see how many days of history that was <b>actually</b> found/included (older history might have been deleted).</li>"
						+ "  <li>Multiplied by <a href='https://en.wikipedia.org/wiki/Standard_deviation' target='_blank'><i>Standard Deviation</i></a> times 2. <i>which is displayed in the below output as 'ThresholdInSec'</i></li>"
						+ "  <li>Average Execution time must be above 30 seconds.</li>"
						+ "  <li>And has been executed at least 10 times.</li>"
						+ "</ul>"
						+ "Also the Average/Min/Max execution times, is present in the below table."
						+ "</p> \n");

				// Add the image below, in top right corner of this "div"
				//   - Hide this div when Microsoft Outlook mail client
				//   - Hide this div for any reader ... except the ones that support JavaScript
				//     when the page loads, the JavaScript will set the div 'stdev_image' to 'block'
				w.append("<!--[if !mso]><!-->  \n"); 
				w.append("<div id='stdev_image' style='display:none'> \n"); 
//				String stDevImage = "https://upload.wikimedia.org/wikipedia/commons/thumb/8/8c/Standard_deviation_diagram.svg/220px-Standard_deviation_diagram.svg.png";
				String stDevImage = "https://d138zd1ktt9iqe.cloudfront.net/media/seo_landing_files/standard-deviation-1626765925.png";
				w.append("<a href='" + stDevImage + "' target='_blank'><img style='height:175px; position:absolute; top:0; right:0;' src='" + stDevImage + "'></img></a> \n");
				w.append("<!--<![endif]--> \n");
				w.append("</div> \n");
				
				// And for Browsers with JavaScript, SHOW the: stDevImage
				w.append("<script type='text/javascript'> \n");
				w.append("    document.getElementById('stdev_image').style.display = 'block'; \n");
				w.append("</script> \n");
				
				// in RED
				w.append("<p style='color:red'><b>Found " + _job_history_outliers.getRowCount() + " Job Steps with OUTLIERS in the recording period...</b></p>");

				w.append(_job_history_outliers.toHtmlTableString("sortable", true, true, null, htmlTableRenderer));
			}
			else
			{
				// in GREEN
				w.append("<p style='color:green'>Found " + _job_history_outliers.getRowCount() + " Job Steps with OUTLIERS in the recording period...<br><br></p>");
			}
			endHtmlSubSection(w);

			
			//------------------------------------------------------------------------
			// get: Errors
			//------------------------------------------------------------------------
			String agent_search_errorMessage = Configuration.getCombinedConfiguration().getProperty   (SqlServerJobSchedulerExtractor.PROPKEY_search_errorMessage, SqlServerJobSchedulerExtractor.DEFAULT_search_errorMessage);
			String agent_skip_msgNumberList  = Configuration.getCombinedConfiguration().getProperty   (PROPKEY_errors_skip_msg_numbers                           , DEFAULT_errors_skip_msg_numbers);
			int    agent_skip_severityBelow  = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_errors_skip_below_severity                        , DEFAULT_errors_skip_below_severity); 

			beginHtmlSubSection(w, "Job Steps with ERROR or WARNINGS", "job_step_with_errors_or_warnings");
			w.append("<p>If the value '<code>" + agent_search_errorMessage + "</code>' is found in the 'message' column, it will be presented as a <i>WARNING</i>.<br>"
					+ "<i>This can be changed with property <code>" + SqlServerJobSchedulerExtractor.PROPKEY_search_errorMessage + " = whatToSearchFor</code></i>"
					+ "</p> \n");

			w.append("<p>The following TSQL Error Codes(s) '<code>" + agent_skip_msgNumberList + "</code>' will be SKIPPED in this section.<br>"
					+ "<i>This can be changed with property <code>" + PROPKEY_errors_skip_msg_numbers + " = #, ##, ###</code></i>"
					+ "</p> \n");

			w.append("<p>And below TSQL Error Severity '<code>" + agent_skip_severityBelow + "</code>' will be SKIPPED in this section.<br>"
					+ "<i>This can be changed with property <code>" + PROPKEY_errors_skip_below_severity + " = #</code></i>"
					+ "</p> \n");

			if (_job_history_errors.getRowCount() > 0)
			{
				// in RED
				w.append("<p style='color:red'><b>Found " + _job_history_errors.getRowCount() + " Job Steps with ERROR or WARNINGS in the recording period...</b></p>");

				w.append(_job_history_errors.toHtmlTableString("sortable", true, true, null, htmlTableRenderer));
			}
			else
			{
				// in GREEN
				w.append("<p style='color:green'>Found " + _job_history_errors.getRowCount() + " Job Steps with ERROR or WARNINGS in the recording period...<br><br></p>");
			}
			endHtmlSubSection(w);


			//------------------------------------------------------------------------
			// get: Errors ALL (no filter on Message Numbers etc)
			//------------------------------------------------------------------------
			if (_job_history_errors_all.getRowCount() > 0)
			{
				beginHtmlSubSection(w, "Job Steps with ERROR or WARNINGS (no filter on Message Numbers)", "job_step_with_errors_or_warnings_ALL");

				if (_job_history_errors_all.getRowCount() > 0)
				{
					String  divId       = "_job_history_errors_all__collapsable";
					boolean showAtStart = false;

					String htmlContent = "";

//					htmlContent += "<p style='color:red'><b>Found " + _job_history_errors_all.getRowCount() + " Job Steps with ERROR or WARNINGS in the recording period (no filter on Message Numbers)...</b></p>";
					htmlContent += _job_history_errors_all.toHtmlTableString("sortable", true, true, null, htmlTableRenderer);
					
					String showHideDiv = createShowHideDiv(divId, showAtStart, "Show/Hide " + _job_history_errors_all.getRowCount() + " Job Steps with ERROR or WARNINGS in the recording period (no filter on Message Numbers)", htmlContent);
					
					w.append( msOutlookAlternateText(showHideDiv, "", "Note: " + _job_history_errors_all.getRowCount() + " ERROR or WARNINGS is not shown in Microsoft Outlook, but available in a browser.") );
				}
				endHtmlSubSection(w);
			}
		}

		// Write JavaScript code for CPU SparkLine
		if (isFullMessageType())
		{
			for (String str : _miniChartJsList)
				w.append(str);
		}
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
//		String schema = getReportingInstance().getDbmsSchemaName();
		String schema = SqlServerJobSchedulerExtractor.PCS_SCHEMA_NAME;
		String sql;

		if ( ! DbUtils.checkIfTableExistsNoThrow(conn, null, schema, SqlServerJobSchedulerExtractor.SqlAgentInfo.job_history_outliers.toString()) )
		{
			_pcsSchemaOrTableWasNotFound = true;
			return; // No need to continue
		}



		//================================
		// get: sysjobs
		//================================
		// +------------------------------------+----------------------------------------------+-------+------------------------------------------+-------------+-----------+-----------------+-------------------+-------------------+--------------+
		// |job_id                              |name                                          |enabled|description                               |start_step_id|category_id|owner            |date_created       |date_modified      |version_number|
		// +------------------------------------+----------------------------------------------+-------+------------------------------------------+-------------+-----------+-----------------+-------------------+-------------------+--------------+
		// |5263D126-CC33-4AF2-A269-08A0C9A6488E|Ladda Ekonomi                                 |0      |No description available.                 |1            |0          |AD\goran.schwarz |2022-02-14 14:56:35|2024-06-04 07:51:53|92            |
		// |786E41AD-98BB-4C97-907B-16B41F418D43|Importera HR (Manuell körning)                |1      |No description available.                 |1            |0          |AD\goran.schwarz |2024-06-19 11:21:41|2024-06-25 10:08:01|31            |
		// |497D6D3C-82B3-47FD-B207-1D45FE53BF19|Ladda Jeeves (DW + Analytics varje kvart)     |1      |No description available.                 |1            |0          |AD\goran.schwarz |2024-06-04 07:59:13|2024-10-29 12:54:13|117           |
		// |796F5568-BD31-4CD0-82B3-40754CC6E5E9|Importera Insclear                            |1      |No description available.                 |1            |0          |AD\goran.schwarz |2023-06-21 13:11:00|2024-02-02 10:04:37|9             |
		// |16A44DF4-FCE8-4E33-A7BB-4299D54629BC|Bonnaya Import                                |0      |Testjobb för att importera manuell fil    |1            |0          |AD\goran.schwarz |2023-04-06 08:53:05|2023-05-30 10:20:48|12            |
		// +------------------------------------+----------------------------------------------+-------+------------------------------------------+-------------+-----------+-----------------+-------------------+-------------------+--------------+
		sql = ""
				+ "SELECT * \n"
				+ "FROM [" + schema + "].[" + SqlServerJobSchedulerExtractor.SqlAgentInfo.sysjobs + "] \n"
				+ "";
		_sysjobs = executeQuery(conn, sql, true, "sysjobs");
//System.out.println(_sysjobs.toAsciiTableString());



		//================================
		// get: sysjobsteps
		//================================
		// +------------------------------------+-------+----------------+---------+-------------------------------------------+---------------+--------------------------------------------+--------------------+-----------------+------------------+--------------+---------------+--------+-------------------+------------------+--------------+--------------+---------------+----------------+----------------+-----------------+----------------+-------------+-------------+--------+------------------------------------+
		// |job_id                              |step_id|disabled_by_step|next_step|step_name                                  |subsystem      |command                                     |cmdexec_success_code|on_success_action|on_success_step_id|on_fail_action|on_fail_step_id|server  |database_name      |database_user_name|retry_attempts|retry_interval|os_run_priority|output_file_name|last_run_outcome|last_run_duration|last_run_retries|last_run_date|last_run_time|proxy_id|step_uid                            |
		// +------------------------------------+-------+----------------+---------+-------------------------------------------+---------------+--------------------------------------------+--------------------+-----------------+------------------+--------------+---------------+--------+-------------------+------------------+--------------+--------------+---------------+----------------+----------------+-----------------+----------------+-------------+-------------+--------+------------------------------------+
		// |5263D126-CC33-4AF2-A269-08A0C9A6488E|1      |(NULL)          |(NULL)   |Stage - Importera från Jeeves senaste året |TSQL           |exec dw.Importera_Jeeves_Senaste            |0                   |3                |0                 |2             |0              |(NULL)  |Datawarehouse      |(NULL)            |0             |0             |0              |(NULL)          |1               |31               |0               |20,240,603   |194,500      |(NULL)  |D6C64358-C9DD-4461-A9CE-7C8F68669A0C|
		// |5263D126-CC33-4AF2-A269-08A0C9A6488E|2      |(NULL)          |(NULL)   |DW - Skapa ekonomiinformation              |TSQL           |exec dw.Skapa_Ekonomiinformation            |0                   |3                |0                 |2             |0              |(NULL)  |Datawarehouse      |(NULL)            |0             |0             |0              |(NULL)          |1               |24               |0               |20,240,603   |194,531      |(NULL)  |5D87A72C-5160-45C2-A8B1-F4CD372D7511|
		// |5263D126-CC33-4AF2-A269-08A0C9A6488E|3      |(NULL)          |(NULL)   |DW - Aggregera ekonomiinformation          |TSQL           |exec dw.Aggregera_Ekonomiinformation        |0                   |3                |0                 |2             |0              |(NULL)  |Datawarehouse      |(NULL)            |0             |0             |0              |(NULL)          |1               |12               |0               |20,240,603   |194,555      |(NULL)  |7050C20E-B724-41E3-B0DC-A7BFFF30A5E3|
		// |5263D126-CC33-4AF2-A269-08A0C9A6488E|4      |(NULL)          |(NULL)   |Ladda dim balansräkning                    |TSQL           |exec [ekonomi].[Ladda_dim_balansrakning]    |0                   |3                |0                 |2             |0              |(NULL)  |Datawarehouse      |(NULL)            |0             |0             |0              |(NULL)          |1               |2                |0               |20,240,603   |194,607      |(NULL)  |B3F30F89-93C2-483D-B844-811608703B07|
		// |5263D126-CC33-4AF2-A269-08A0C9A6488E|5      |(NULL)          |(NULL)   |Ladda dim resultatrakning                  |TSQL           |exec [ekonomi].[Ladda_dim_resultatrakning]  |0                   |3                |0                 |2             |0              |(NULL)  |Datawarehouse      |(NULL)            |0             |0             |0              |(NULL)          |1               |19               |0               |20,240,603   |194,609      |(NULL)  |86874F6E-A34D-4E87-A558-36550FCED6A6|
		// +------------------------------------+-------+----------------+---------+-------------------------------------------+---------------+--------------------------------------------+--------------------+-----------------+------------------+--------------+---------------+--------+-------------------+------------------+--------------+--------------+---------------+----------------+----------------+-----------------+----------------+-------------+-------------+--------+------------------------------------+
		sql = ""
				+ "SELECT * \n"
				+ "FROM [" + schema + "].[" + SqlServerJobSchedulerExtractor.SqlAgentInfo.sysjobsteps + "] \n"
				+ "";
		_sysjobsteps = executeQuery(conn, sql, true, "sysjobsteps");
//System.out.println(_sysjobsteps.toAsciiTableString());


		//================================
		// get: Overview
		//================================
		sql = ""
			    + "SELECT \n"
			    + "     [JobName] \n"
			    + "    ,[stepCount] \n"
			    + "    ,'' AS [stepCmds] \n"
			    + "    ,[runStatusDesc] \n"
			    + "    ,[execCount] \n"
			    + "    ,[sumTimeInSec] \n"
			    + "    ,[sumTime_HMS] \n"
			    + "    ,[avgTimeInSec] \n"
			    + "    ,[avgTime_HMS] \n"
			    + "    ,[minTime_HMS] \n"
			    + "    ,[maxTime_HMS] \n"
			    + "    ,[stdevp] \n"
			    + "    ,[firstExecTime] \n"
			    + "    ,[lastExecTime] \n"

			    + "    ,[histExecCount] \n"
			    + "    ,[histDaysCount] \n"
			    + "    ,[histFirstExecTime] \n"
			    + "    ,[histAvgTimeInSec] \n"
			    + "    ,[histAvgTime_HMS] \n"
			    + "    ,[histMinTime_HMS] \n"
			    + "    ,[histMaxTime_HMS] \n"
			    + "    ,[histStdevp] \n"
			    + "    ,[histAllExecTimes] \n"
			    + "    ,'' AS [histAllExecTimesChart] \n" // Fill this one with a chart of "histAllExecTimes"

			    + "    ,[job_id] \n"

			    + "FROM [" + schema + "].[" + SqlServerJobSchedulerExtractor.SqlAgentInfo.job_history_overview + "] \n"
//			    + "ORDER BY [avgTimeInSec] DESC \n"
			    + "ORDER BY [sumTimeInSec] DESC \n"
			    + "";
				
		_job_history_overview = executeQuery(conn, sql, true, "job_history_overview");
//		_job_history_overview.setHighlightSortColumns("avgTimeInSec");
		_job_history_overview.setHighlightSortColumns("sumTimeInSec");

		// Create sparkline/charts for each "histAllExecTimes" at column "histAllExecTimesChart"
		for (int r=0; r<_job_history_overview.getRowCount(); r++)
		{
			String histAllExecTimes = _job_history_overview.getValueAsString(r, "histAllExecTimes");
			createSparklineForHistoryExecutions(histAllExecTimes, _job_history_overview, r, "histAllExecTimesChart");
		}
		
		_job_history_overview.setColumnDescription("JobName"           ,"Name of the JOB shat started this step");
		_job_history_overview.setColumnDescription("stepCount"         ,"How many steps does this job name have");
		_job_history_overview.setColumnDescription("stepCmds"          ,"A list of all commands in the job");
		_job_history_overview.setColumnDescription("runStatusDesc"     ,"The outcome of the job. Can be: FAILED, SUCCESS, RETRY, CANCELED or IN PROGRESS");
		_job_history_overview.setColumnDescription("execCount"         ,"How many times has this JOB been executed in the reporting period");
		_job_history_overview.setColumnDescription("sumTimeInSec"      ,"Summary Execution time in Seconds for all Executions in reporting period");
		_job_history_overview.setColumnDescription("sumTime_HMS"       ,"Summary Execution time in Hour:Minute:Seconds for all Executions in reporting period");
		_job_history_overview.setColumnDescription("avgTimeInSec"      ,"Average Execution time in Seconds for all Executions in reporting period");
		_job_history_overview.setColumnDescription("avgTime_HMS"       ,"Average Execution time in Hour:Minute:Seconds for all Executions in reporting period");
		_job_history_overview.setColumnDescription("minTime_HMS"       ,"Minimum Execution time in Hour:Minute:Seconds for all Executions in reporting period");
		_job_history_overview.setColumnDescription("maxTime_HMS"       ,"Maximum Execution time in Hour:Minute:Seconds for all Executions in reporting period");
		_job_history_overview.setColumnDescription("stdevp"            ,"Standard Deviation Number of the Execution Time. Note: A higher number means more 'outliers', A lower number means more 'equal' execution times.");
		_job_history_overview.setColumnDescription("firstExecTime"     ,"Time when this job was FIRST executed in in reporting period");
		_job_history_overview.setColumnDescription("lastExecTime"      ,"Time when this job was LAST executed in in reporting period");

		_job_history_overview.setColumnDescription("histExecCount"     ,"How many times was the job executed in the last ## days");
		_job_history_overview.setColumnDescription("histDaysCount"     ,"How many days is included in the history");
		_job_history_overview.setColumnDescription("histFirstExecTime" ,"First execution time  in the last ## days");
		_job_history_overview.setColumnDescription("histAvgTimeInSec"  ,"Average number of seconds this job took for the last ## days");
		_job_history_overview.setColumnDescription("histAvgTime_HMS"   ,"Average Hour:Minute:Seconds this job took for the last ## days");
		_job_history_overview.setColumnDescription("histMinTime_HMS"   ,"Minimum Hour:Minute:Seconds this job took for the last ## days");
		_job_history_overview.setColumnDescription("histMaxTime_HMS"   ,"Max Hour:Minute:Seconds this job took for the last ## days");
		_job_history_overview.setColumnDescription("histStdevp"        ,"Standard Deviation Number of the Execution Time for the last ## days. Note: A higher number means more 'outliers', A lower number means more 'equal' execution times.");
		_job_history_overview.setColumnDescription("histAllExecTimes"  ,"A tooltip/popup with timings of ALL jobs in the last ## days");
		_job_history_overview.setColumnDescription("histAllExecTimesChart"  ,"A Chart with the timings of ALL jobs in the last ## days");

		_job_history_overview.setColumnDescription("job_id"            ,"The ID of the JOB");

		// get tooltip for each job_id
		_jobCommandsMap = new HashMap<>();
		for (int r=0; r<_job_history_overview.getRowCount(); r++)
		{
			String job_id  = _job_history_overview.getValueAsString(r, "job_id");
			String tooltip = getTooltipFor_jobAllCommands(conn, job_id);
			_jobCommandsMap.put(job_id, tooltip);
		}
		
		//================================
		// get: Outliers
		//================================
		sql = ""
		    + "SELECT \n"
		    + "     [execTime] \n"
		    + "    ,[JobName] \n"
		    + "    ,[step_id] \n"
		    + "    ,'' AS [cmd] \n"
		    + "    ,[run_status_desc] \n"
		    + "    ,[step_name] \n"
		    + "    ,[deviationInPct] \n"
		    + "    ,[deviationInSec] \n"
		    + "    ,[timeInSec] \n"
		    + "    ,[time_HMS] \n"
		    + "    ,[histAvgTime_HMS] \n"
		    + "    ,[histMinTime_HMS] \n"
		    + "    ,[histMaxTime_HMS] \n"
		    + "    ,[histAvgTimeInSec] \n"
		    + "    ,[histMinTimeInSec] \n"
		    + "    ,[histMaxTimeInSec] \n"
		    + "    ,[histCount] \n"
		    + "    ,[histDaysCount] \n"
		    + "    ,[histStdevp] \n"
		    + "    ,[thresholdInSec] \n"
		    + "    ,[histAllExecTimes] \n"
		    + "    ,'' AS [histAllExecTimesChart] \n" // Fill this one with a chart of "histAllExecTimes"
		    + "    ,[job_id] \n"
		    + "FROM [" + schema + "].[" + SqlServerJobSchedulerExtractor.SqlAgentInfo.job_history_outliers + "] \n"
		    + "ORDER BY [execTime] \n"
		    + "";
			
		_job_history_outliers = executeQuery(conn, sql, true, "job_history_outliers");

		// Create sparkline/charts for each "histAllExecTimes" at column "histAllExecTimesChart"
		for (int r=0; r<_job_history_outliers.getRowCount(); r++)
		{
			String histAllExecTimes = _job_history_outliers.getValueAsString(r, "histAllExecTimes");
			createSparklineForHistoryExecutions(histAllExecTimes, _job_history_outliers, r, "histAllExecTimesChart");
		}
		
		int outliers_calcHistoricalDays = Configuration.getCombinedConfiguration().getIntProperty(SqlServerJobSchedulerExtractor.PROPKEY_outliers_calcHistoricalDays, SqlServerJobSchedulerExtractor.DEFAULT_outliers_calcHistoricalDays);
		
		_job_history_outliers.setColumnDescription("execTime"              ,"When was it executed");
		_job_history_outliers.setColumnDescription("JobName"               ,"Name of the JOB shat started this step");
		_job_history_outliers.setColumnDescription("step_id"               ,"The step_id in the job. NOTE: If you reorder the steps inside the job (Average execution time might be wrong, since the 'step_id' is not a 'real' id, but just a sequence number within the job...");
		_job_history_outliers.setColumnDescription("cmd"                   ,"What Job command was executed.");
		_job_history_outliers.setColumnDescription("run_status_desc"       ,"The outcome of the job. Can be: FAILED, SUCCESS, RETRY, CANCELED or IN PROGRESS");
		_job_history_outliers.setColumnDescription("step_name"             ,"Name of the job step.");
		_job_history_outliers.setColumnDescription("deviationInPct"        ,"How many Percent longer the exeution time was than the <i>normal</i> time.");
		_job_history_outliers.setColumnDescription("deviationInSec"        ,"How many seconds longer the exeution time was than the <i>normal</i> time.");
		_job_history_outliers.setColumnDescription("timeInSec"             ,"How many seconds <b>this</b> execution time was.");
		_job_history_outliers.setColumnDescription("time_HMS"              ,"Same as 'DurationInSec', but in the format Hour:Minute:Second");
		_job_history_outliers.setColumnDescription("histAvgTime_HMS"       ,"Same as 'HistAvgDurInSec', but in the format Hour:Minute:Second");
		_job_history_outliers.setColumnDescription("histMinTime_HMS"       ,"Same as 'HistMinDurInSec', but in the format Hour:Minute:Second");
		_job_history_outliers.setColumnDescription("histMaxTime_HMS"       ,"Same as 'HistMaxDurInSec', but in the format Hour:Minute:Second");
		_job_history_outliers.setColumnDescription("histAvgTimeInSec"      ,"How many seconds the Average execution time (for the last " + outliers_calcHistoricalDays + " days) was.");
		_job_history_outliers.setColumnDescription("histMinTimeInSec"      ,"How many seconds the Minimum execution time (for the last " + outliers_calcHistoricalDays + " days) was.");
		_job_history_outliers.setColumnDescription("histMaxTimeInSec"      ,"How many seconds the Maximum execution time (for the last " + outliers_calcHistoricalDays + " days) was.");
		_job_history_outliers.setColumnDescription("histCount"             ,"How many executions was done in the last " + outliers_calcHistoricalDays + " days)");
		_job_history_outliers.setColumnDescription("histDaysCount"         ,"How many days is included in the history");
		_job_history_outliers.setColumnDescription("histStdevp"            ,"Standard Deviation Number of the Execution Time. Note: A higher number means more 'outliers', A lower number means more 'equal' execution times.");
		_job_history_outliers.setColumnDescription("thresholdInSec"        ,"AvgExecTime * stddev * 2");
		_job_history_outliers.setColumnDescription("histAllExecTimes"      ,"A tooltip/popup with timings of ALL jobs in the last ## days");
		_job_history_outliers.setColumnDescription("histAllExecTimesChart" ,"AA Chart with the timings of ALL jobs in the last ## days");
		_job_history_outliers.setColumnDescription("job_id"                ,"Just the ID of the job if you want to know");


		//================================
		// get: Errors
		//================================
		String agent_skip_msgNumberList  = Configuration.getCombinedConfiguration().getProperty(PROPKEY_errors_skip_msg_numbers, DEFAULT_errors_skip_msg_numbers);
		List<String> skipMsgList = StringUtil.parseCommaStrToList(agent_skip_msgNumberList, true);
		
		// TODO: Check if skipMsgList entries are numbers ???

		String sqlSkipMessageIds = "";
		if ( ! skipMsgList.isEmpty() )
		{
			sqlSkipMessageIds = "  AND ([sql_message_id] NOT IN(" + StringUtil.toCommaStr(skipMsgList) + ") AND [subsystem] = 'TSQL') \n";
		}

		int agent_skip_severityBelow = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_errors_skip_below_severity, DEFAULT_errors_skip_below_severity); 
		
		String sqlSkipBelowSeverity = "";
		if (agent_skip_severityBelow != DEFAULT_errors_skip_below_severity)
		{
			sqlSkipBelowSeverity = "  AND ([sql_message_id] > " + agent_skip_severityBelow + " AND [subsystem] = 'TSQL') \n";
		}
		
		sql = ""
		    + "SELECT "
		    + "     [execTime] \n"
		    + "    ,[JobName] \n"
		    + "    ,[step_name] \n"
		    + "    ,[step_id] \n"
		    + "    ,[subsystem] \n"
		    + "    ,'' AS [cmd] \n"
		    + "    ,[run_status_desc] \n"
		    + "    ,[retries_attempted] \n"
		    + "    ,[sql_message_id] \n"
		    + "    ,[sql_severity] \n"
		    + "    ,[message] \n"
		    + "FROM [" + schema + "].[" + SqlServerJobSchedulerExtractor.SqlAgentInfo.job_history_errors + "] \n"
		    + "WHERE 1 = 1 \n"
		    + sqlSkipMessageIds
		    + sqlSkipBelowSeverity
		    + " OR [subsystem] <> 'TSQL' \n"
		    + "ORDER BY [execTime] \n"
		    + "";
				
		_job_history_errors = executeQuery(conn, sql, true, "job_history_errors");

		_job_history_errors.setColumnDescription("ExecutionDate"     ,"When was it executed");
		_job_history_errors.setColumnDescription("JobName"           ,"Name of the JOB shat started this step");
		_job_history_errors.setColumnDescription("step_name"         ,"Name of the job step");
		_job_history_errors.setColumnDescription("step_id"           ,"The step_id in the job");
		_job_history_errors.setColumnDescription("subsystem"         ,"What subsystem was used to execute");
		_job_history_errors.setColumnDescription("run_status_desc"   ,"The outcome of the job. Can be: FAILED, SUCCESS, WARNING, RETRY, CANCELED or IN PROGRESS");
		_job_history_errors.setColumnDescription("job_id"            ,"Just the ID of the job if you want to know");
		_job_history_errors.setColumnDescription("retries_attempted" ,"What is says");
		_job_history_errors.setColumnDescription("sql_message_id"    ,"SQL Server error number, if the SQL Job produced any errors");
		_job_history_errors.setColumnDescription("sql_severity"      ,"SQL Server error severity, if the SQL Job produced any errors");
		_job_history_errors.setColumnDescription("message"           ,"Any messages produced by the output");

//System.out.println("_job_history_errors:\n" + _job_history_errors.toAsciiTableString());
//		// copy '_job_history_errors' into '_job_history_errors_filtered'
//		_job_history_errors_filtered = _job_history_errors.copy();
//		
//		for (int r=0; r<_job_history_errors_filtered.getRowCount(); r++)
//		{
//			String message = _job_history_errors_filtered.getValueAsString(r, "message");
//			List<String> messages = parseErrorMessages(message); 
//		}

		//================================
		// get: Errors (ALL)
		//================================
		sql = ""
		    + "SELECT "
		    + "     [execTime] \n"
		    + "    ,[JobName] \n"
		    + "    ,[step_name] \n"
		    + "    ,[step_id] \n"
		    + "    ,[subsystem] \n"
		    + "    ,'' AS [cmd] \n"
		    + "    ,[run_status_desc] \n"
		    + "    ,[retries_attempted] \n"
		    + "    ,[sql_message_id] \n"
		    + "    ,[sql_severity] \n"
		    + "    ,[message] \n"
		    + "FROM [" + schema + "].[" + SqlServerJobSchedulerExtractor.SqlAgentInfo.job_history_errors + "] \n"
		    + "ORDER BY [execTime] \n"
		    + "";
				
		_job_history_errors_all = executeQuery(conn, sql, true, "job_history_errors_all");
//System.out.println("_job_history_errors_all:\n" + _job_history_errors_all.toAsciiTableString());

		_job_history_errors_all.setColumnDescription("ExecutionDate"     ,"When was it executed");
		_job_history_errors_all.setColumnDescription("JobName"           ,"Name of the JOB shat started this step");
		_job_history_errors_all.setColumnDescription("step_name"         ,"Name of the job step");
		_job_history_errors_all.setColumnDescription("step_id"           ,"The step_id in the job");
		_job_history_errors_all.setColumnDescription("subsystem"         ,"What subsystem was used to execute");
		_job_history_errors_all.setColumnDescription("run_status_desc"   ,"The outcome of the job. Can be: FAILED, SUCCESS, WARNING, RETRY, CANCELED or IN PROGRESS");
		_job_history_errors_all.setColumnDescription("job_id"            ,"Just the ID of the job if you want to know");
		_job_history_errors_all.setColumnDescription("retries_attempted" ,"What is says");
		_job_history_errors_all.setColumnDescription("sql_message_id"    ,"SQL Server error number, if the SQL Job produced any errors");
		_job_history_errors_all.setColumnDescription("sql_severity"      ,"SQL Server error severity, if the SQL Job produced any errors");
		_job_history_errors_all.setColumnDescription("message"           ,"Any messages produced by the output");
		
	}
	




	private String getTooltipFor_jobAllCommands(DbxConnection conn, String job_id)
	{
		String schema = SqlServerJobSchedulerExtractor.PCS_SCHEMA_NAME;

		String sql = ""
				+ "SELECT \n"
				+ "     [step_id] \n"
				+ "    ,[disabled_by_step] \n"
				+ "    ,[step_name] \n"
				+ "    ,[database_name] \n"
				+ "    ,[subsystem] \n"
				+ "    ,[command] \n"
				+ "FROM [" + schema + "].[" + SqlServerJobSchedulerExtractor.SqlAgentInfo.sysjobsteps + "] \n"
				+ "WHERE [job_id] = '" + job_id + "' \n"
				+ "";

		ResultSetTableModel rstm = executeQuery(conn, sql, true, "jobCommandsForJobId");
		
		// Return it as a ASCII Table
//		return rstm.toAsciiTableString();

		// Build a Custom "table"
		// 
		// #############################################################
		// ## step_id:   ## (IS DISABLED), name: ...
		// ## command ## subsystem ## dbname ###########################
		// asdfasd asd fas d asd  
		// #############################################################

		StringBuilder sb = new StringBuilder();
		for (int r=0; r<rstm.getRowCount(); r++)
		{
			String step_id          = rstm.getValueAsString(r, "step_id"         , true, "");
			String disabled_by_step = rstm.getValueAsString(r, "disabled_by_step", true, "");
			String step_name        = rstm.getValueAsString(r, "step_name"       , true, "");
			String database_name    = rstm.getValueAsString(r, "database_name"   , true, "");
			String subsystem        = rstm.getValueAsString(r, "subsystem"       , true, "");
			String command          = rstm.getValueAsString(r, "command"         , true, "");
			
			String line1 = "## step_id=" + step_id + " " + (StringUtil.hasValue(disabled_by_step) ? "(***DISABLED*** by step_id=" + disabled_by_step + ")" : "") + ", name='" + step_name + "'";
//			String line2 = ("## subsystem='" + subsystem + "'" + (StringUtil.hasValue(database_name) ? ", dbname='"+database_name+"' " : " ") + "------------------------------------------------------------------------------").substring(0, 80);
			String line2 = "## subsystem='" + subsystem + "'" + (StringUtil.hasValue(database_name) ? ", dbname='"+database_name+"'" : "");
			String line3 = command;
			sb.append("##==============================================================================\n");
			sb.append(line1).append("\n");
			sb.append(line2).append("\n");
			sb.append("##------------------------------------------------------------------------------\n");
			sb.append(line3).append("\n");
			sb.append("##------------------------------------------------------------------------------\n");
			sb.append("\n");
		}
		
		return StringEscapeUtils.escapeHtml4(sb.toString());
//		return sb.toString();

//		// Return it as a HTML Table (WITH SOME FORMATTING)
//		return rstm.toHtmlTableString("sortable jobstep-commands", true, true, null, new TableStringRenderer()
//		{
//			@Override
//			public String tagTdAttr(ResultSetTableModel rstm, int row, int col, String colName, Object objVal, String strVal, boolean nowrapPreferred)
//			{
//				if ("disabled_by_step".equals(colName))
//				{
//					if (strVal == null || ResultSetTableModel.DEFAULT_NULL_REPLACE.equals(strVal))
//						strVal = "";
//
//					if ( StringUtil.hasValue(strVal) )
//						return "style='background-color=red;'";
//				}
//				
//				return TableStringRenderer.super.tagTdAttr(rstm, row, col, colName, objVal, strVal, nowrapPreferred);
//			}
//
//			@Override
//			public String cellValue(ResultSetTableModel rstm, int row, int col, String colName, Object objVal, String strVal)
//			{
//				if (strVal == null || ResultSetTableModel.DEFAULT_NULL_REPLACE.equals(strVal))
//					strVal = "";
//
//				if ("command".equals(colName))
//				{
////					return StringEscapeUtils.escapeHtml4(strVal);
//					return "<pre><code>" + StringEscapeUtils.escapeHtml4(strVal) + "</pre></code>";
//				}
//
//				return strVal;
//			}
//		});
	}

	/**
	 * Try to make messages a bit more readable<br>
	 *  - If it's to long try to add NEWLINE somewhere
	 *  - NewLine on some special words
	 *  - If it looks like a "console log line" add newlines
	 * @param msg            The input
	 * @param newline        What is the String for newline
	 * @param subsystem      
	 * @param msgNumber 
	 * @param msgSeverity    
	 * @return
	 */
	private static String formatMessageString(String msg, String newline, String subsystem, int msgNumber, int msgSeverity)
	{
		if (msg == null)
			return null;
		
		if (msg.length() < 64)
			return msg;
		
		// "Executed as user: MAXM\\goran.schwarz. " -->> "Executed as user: MAXM\\goran.schwarz. <BR>"
		msg = msg.replaceFirst("Executed as user: \\S* ", "$0" + newline);
		
		// Console messages
		msg = msg.replace("DEBUG   - ",          newline + "DEBUG   - ");
		msg = msg.replace("INFO    - ",          newline + "INFO    - ");
		msg = msg.replace("WARNING - ",          newline + "WARNING - ");
		msg = msg.replace("ERROR   - ",          newline + "ERROR   - ");

		// Some Error messages
		msg = msg.replace("ERROR-MSG: ",         newline + "ERROR-MSG: ");
		
//		msg = msg.replace("[SQLSTATE ",          newline + "[SQLSTATE ");
//		msg = msg.replace("Warning! ",           newline + "Warning! ");
//		msg = msg.replace("Warning! ",           newline + "Warning! ");
//		msg = msg.replace("Caution: ",           newline + "Caution: ");
		// Make a newline *after* '[SQLSTATE #####] (Message ####)'
		msg = msg.replaceAll("\\[SQLSTATE \\d+\\] \\(Message \\d+\\)", "$0" + newline); 

		msg = msg.replace("Process Exit Code ",  newline + "Process Exit Code ");
		
		msg = msg.replace("The step failed."   , newline + "The step failed."); 
		msg = msg.replace("The step succeeded.", newline + "The step succeeded.");
		
		// Remove any "double newlines" (that only contains a period '.' char) 
		msg = msg.replace(newline + ".  " + newline, newline);
		
		// Remove any "double newlines"
		msg = msg.replace(newline + newline, newline);
		
		// If it's a T-SQL subsystem, lets remove some "stuff"
		if ("TSQL".equals(subsystem))
		{
			// SQLSTATE description: https://en.wikipedia.org/wiki/SQLSTATE
			
			// Simplify message: "[SQLSTATE 01000] (Message 50000)" -> "" (empty string)
//			msg = msg.replace("[SQLSTATE 01000] (Message " + msgNumber + ")", "");
//			msg = msg.replaceAll("\\[SQLSTATE 01\\d+\\] \\(Message " + msgNumber + "\\)", "");  // Remove any of the "warning" SQL STATES (which HAS the msgNumber)
			msg = msg.replaceAll("\\[SQLSTATE 01\\d+\\] \\(Message \\d+\\)", ""); // Remove any of the "warning" SQL STATES
		}
		
		
		return msg;
	}
	
	private String getDescriptionForJobName(String jobName)
	{
		if (_sysjobs == null)
			return "";

		// Get 'job_id' from '_sysjobs' (since we don't have the "job_id" in the input)
		String description = "";
		List<Integer> rowIds = _sysjobs.getRowIdsWhere("name", jobName);
		if ( ! rowIds.isEmpty() )
		{
			description = _sysjobs.getValueAsString(rowIds.get(0), "description");

//			// Reset "dummy" message
//			if ("No description available.".equals(description))
//				description = "";
		}
		return description;
	}
	
	private String getTooltipFor_jobAllCommands(String job_id)
	{
		if (_jobCommandsMap == null)
			return "";

		String tooltip = _jobCommandsMap.get(job_id);
		if (StringUtil.isNullOrBlank(tooltip))
			tooltip = "job_id '" + job_id + "' was not found...";

//		return StringEscapeUtils.escapeHtml4(tooltip);
		return tooltip;
	}

	private String getTooltipFor_histAllExecTimes(String job_id, String strVal)
	{
		// not really using job_id here
		
		if (StringUtil.isNullOrBlank(strVal))
			return "";

		return strVal;
		
		// Newlines to HTML-Newlines
//		return strVal.replace("\n", "<BR>");
	}

	private String getTooltipFor_jobStepCommand(String jobName, String step_id)
	{
		if (_sysjobs == null || _sysjobsteps == null)
			return "";

		// Get 'job_id' from '_sysjobs' (since we don't have the "job_id" in the input)
		String job_id = null;
		List<Integer> rowIds = _sysjobs.getRowIdsWhere("name", jobName);
		if ( ! rowIds.isEmpty() )
		{
			job_id = _sysjobs.getValueAsString(rowIds.get(0), "job_id");
		}

		if (StringUtil.isNullOrBlank(job_id))
		{
			_logger.info("getTooltipFor_jobStepCommand() name='" + jobName + "'. No records was found in 'sysjobs'.");
			return "No 'job_id' was found for jobName '" + jobName + "'.";
		}
		
		// Get 'job_id' from '_sysjobs'
		Map<String, Object> where = new HashMap<>();
		where.put("job_id",  job_id);
		where.put("step_id", Integer.parseInt(step_id)); // note: it's an integer in the '_sysjobsteps' table
		rowIds = _sysjobsteps.getRowIdsWhere(where);

		String tooltip = "not found";
		if (rowIds.isEmpty())
		{
			_logger.info("getTooltipFor_jobStepCommand() jobName='" + jobName + "', job_id='" + job_id + "', step_id='" + step_id + "'. No records was found in 'sysjobsteps'.");
			tooltip = "Not Found: jobName='" + jobName + "', job_id='" + job_id + "', step_id='" + step_id + "'. No records was found in 'sysjobsteps'";
		}
		else
		{
			String command = _sysjobsteps.getValueAsString(rowIds.get(0), "command");
			
			tooltip = command;
		}

		return StringEscapeUtils.escapeHtml4(tooltip);
	}


	/**
	 * Parse the following text into a "sparkline"<br>
	 * Create a "sparkline" (mini chart) and set it to the content of 'colName' 
	 * 
	 * <code>
	 * <pre>
	 * ts=2024-10-17 00:30:00, wd=Thu, HMS=04:59:42, sec=17982, status=SUCCESS;
	 * ts=2024-10-18 00:30:00, wd=Fri, HMS=04:30:30, sec=16230, status=FAILED;
	 * ts=2024-10-19 01:30:00, wd=Sat, HMS=05:31:54, sec=19914, status=SUCCESS;
	 * ts=2024-10-20 00:30:00, wd=Sun, HMS=04:49:14, sec=17354, status=SUCCESS;
	 * .....
	 * ts=2024-11-13 00:30:00, wd=Wed, HMS=04:39:08, sec=16748, status=SUCCESS;
	 * ts=2024-11-14 00:30:00, wd=Thu, HMS=05:11:03, sec=18663, status=SUCCESS;
	 * ts=2024-11-15 00:30:00, wd=Fri, HMS=01:09:00, sec=4140, status=FAILED;
	 * ts=2024-11-15 07:48:09, wd=Fri, HMS=00:00:21, sec=21, status=FAILED;
	 * ts=2024-11-16 01:30:00, wd=Sat, HMS=05:31:25, sec=19885, status=SUCCESS;
	 * </pre>
	 * </code>
	 * 
	 */
	private void createSparklineForHistoryExecutions(String textToParse, ResultSetTableModel rstm, int row, String colName)
	{
		List<String> lines = Arrays.asList(StringUtils.split(textToParse, "\n"));

		if (lines.isEmpty())
			return;
		
		int pos_chartColumnName = rstm.findColumnMandatory(colName);
		if (pos_chartColumnName == -1)
			return;
		
		SparklineResult result = new SparklineResult();
		for (String line : lines)
		{
			if ( ! line.startsWith("ts=") )
				continue;

			String tsStr     = StringUtil.substringBetweenTwoChars(line, "ts=",     ",").trim();
			String wdStr     = StringUtil.substringBetweenTwoChars(line, "wd=",     ",").trim();
			String hmsStr    = StringUtil.substringBetweenTwoChars(line, "HMS=",    ",").trim();
			String secStr    = StringUtil.substringBetweenTwoChars(line, "sec=",    ",").trim();
			String statusStr = StringUtil.substringBetweenTwoChars(line, "status=", ";").trim();

			try
			{
				Timestamp ts  = TimeUtils.parseToTimestamp(tsStr, "yyyy-MM-dd HH:mm:ss");
				int       sec = StringUtil.parseInt(secStr, -1);

				//System.out.println("xxxxx::: " + rstm.getName() + ", row=" + row + ", tsStr=|" + tsStr + "|, secStr=|" + secStr + "|. ts=|" + ts + "|, sec=|" + sec + "|, line=|" + line + "|.");
				
				String tooltip = ""
						+ "<br>"
						+ tsStr  + " (" + wdStr + ")<br>"
						+ hmsStr + " (" + secStr + " Seconds)<br>"
						+ "<br>"
						+ "Status: " + statusStr + "<br>"
						;

				result.beginTs .add(ts);
				result.values  .add(sec);
				result.tooltips.add(tooltip);
			}
			catch (Exception e) 
			{
				_logger.warn("Skipping some entry in createSparklineForHistoryExecutions(rstm.name='" + rstm.getName() + "', row=" + row + ", tsStr='" + tsStr + "', secStr='" + secStr + "') ... Caught: " + e);
			}
		}

		// We need to create a html class for every row (since the tooltip is different for every row)
		String sparklineClassName = "sparklines_" + rstm.getName() + "__chart_row_" + row;

		// Create a PNG, which will displayed when the reader do not have access to JavaScript
		String jfreeChartInlinePng = SparklineJfreeChart.create(result);

		// Create a "div" that will be added to the table cell
		String sparklineDiv = SparklineHelper.getSparklineDiv(result, sparklineClassName, jfreeChartInlinePng);

		// SET the content of that "cell"
		rstm.setValueAtWithOverride(sparklineDiv, row, pos_chartColumnName);

		// Create JavaScript code to initialize all SparkLines with a specific class name
		String sparklineJavaScriptInitCode = SparklineHelper.getJavaScriptInitCode(this, result, sparklineClassName, " Seconds");

		// Add it to a list which will have to be appended (in method: writeMessageText)
		_miniChartJsList.add(sparklineJavaScriptInitCode);
	}


//	private static class HtmlTableRenderer 
	private class HtmlTableRenderer 
	implements ResultSetTableModel.TableStringRenderer
	{
		@Override
		public String tagTdAttr(ResultSetTableModel rstm, int row, int col, String colName, Object objVal, String strVal, boolean nowrapPreferred)
		{
			String attr = ResultSetTableModel.TableStringRenderer.super.tagTdAttr(rstm, row, col, colName, objVal, strVal, nowrapPreferred);

			if ("run_status_desc".equals(colName) || "runStatusDesc".equals(colName))
			{
				if ("FAILED" .equals(strVal)) attr = "style='color:red;'";
				if ("SUCCESS".equals(strVal)) attr = "style='color:green;'";
				if ("WARNING".equals(strVal)) attr = "style='color:orange;'";
			}

			if (colName.startsWith("hist") || colName.startsWith("Hist"))
			{
				attr = "style='color:#9C7B57;'"; // #BA9368; == Camel
//				attr = "style='background-color:#EDC9AF;'"; // #EDC9AF; == Desert Sand
			}

			return attr;
		}

		@Override
		public String cellValue(ResultSetTableModel rstm, int row, int col, String colName, Object objVal, String strVal)
		{
			//--------------------------------------------------------------
			// job_history_overview
			//--------------------------------------------------------------
			if ("job_history_overview".equals(rstm.getName()))
			{
				// If "execCount" == 0 -->> Return empty for some columns
				if ("minTime_HMS".equals(colName) || "maxTime_HMS".equals(colName) || "lastExecTime".equals(colName))
				{
					if (rstm.getValueAsInteger(row, "execCount", true, -1) == 1)
						return "<div style='color:lightgray'>-single-exec-</div>";
				}

				if ("stepCmds".equals(colName))
				{
					// Get Actual Executed SQL Text for current row
				//	String query_hash = rstm.getValueAsString(row, "query_hash");
				//	String sqlText    = rstm.getValueAsString(row, "SqlText");
					String JobName    = rstm.getValueAsString(row, "JobName");
					String job_id     = rstm.getValueAsString(row, "job_id");
					
					// Put the "Actual Executed SQL Text" as a "tooltip"
					return "<div title='Click for Detailes' "
							+ "data-toggle='modal' "
							+ "data-target='#dbx-view-sqltext-dialog' "
							+ "data-objectname='" + JobName + "' "
							+ "data-tooltip=\""   + getTooltipFor_jobAllCommands(job_id) + "\" "
							+ ">&#x1F4AC;</div>"; // symbol popup with "..."
				}

				if ("histAllExecTimes".equals(colName))
				{
					// Get Actual Executed SQL Text for current row
				//	String query_hash = rstm.getValueAsString(row, "query_hash");
				//	String sqlText    = rstm.getValueAsString(row, "SqlText");
					String JobName    = rstm.getValueAsString(row, "JobName");
					String job_id     = rstm.getValueAsString(row, "job_id");
					
					// Put the "Actual Executed SQL Text" as a "tooltip"
					return "<div title='Click for Detailes' "
							+ "data-toggle='modal' "
							+ "data-target='#dbx-view-sqltext-dialog' "
							+ "data-objectname='" + JobName + "' "
							+ "data-tooltip=\""   + getTooltipFor_histAllExecTimes(job_id, strVal) + "\" "
							+ ">&#x1F4AC;</div>"; // symbol popup with "..."
				}
			}
			
			//--------------------------------------------------------------
			// job_history_outliers
			//--------------------------------------------------------------
			if ("job_history_outliers".equals(rstm.getName()))
			{
				if ("cmd".equals(colName))
				{
					String jobName = rstm.getValueAsString(row, "JobName");
					String stepId  = rstm.getValueAsString(row, "step_id");
					
					// Put the "Actual Executed SQL Text" as a "tooltip"
					return "<div title='Click for Detailes' "
							+ "data-toggle='modal' "
							+ "data-target='#dbx-view-sqltext-dialog' "
							+ "data-objectname='" + ("step_id=" + stepId +", jobName=" + jobName) + "' "
							+ "data-tooltip=\""   + getTooltipFor_jobStepCommand(jobName, stepId) + "\" "
							+ ">&#x1F4AC;</div>"; // symbol popup with "..."
				}

				if ("histAllExecTimes".equals(colName))
				{
					// Get Actual Executed SQL Text for current row
				//	String query_hash = rstm.getValueAsString(row, "query_hash");
				//	String sqlText    = rstm.getValueAsString(row, "SqlText");
					String JobName    = rstm.getValueAsString(row, "JobName");
					String job_id     = rstm.getValueAsString(row, "job_id");
					
					// Put the "Actual Executed SQL Text" as a "tooltip"
					return "<div title='Click for Detailes' "
							+ "data-toggle='modal' "
							+ "data-target='#dbx-view-sqltext-dialog' "
							+ "data-objectname='" + JobName + "' "
							+ "data-tooltip=\""   + getTooltipFor_histAllExecTimes(job_id, strVal) + "\" "
							+ ">&#x1F4AC;</div>"; // symbol popup with "..."
				}
			}

			//--------------------------------------------------------------
			// job_history_outliers
			//--------------------------------------------------------------
			if ("job_history_errors".equals(rstm.getName()) || "job_history_errors_all".equals(rstm.getName()))
			{
				if ("cmd".equals(colName))
				{
					String jobName = rstm.getValueAsString(row, "JobName");
					String stepId  = rstm.getValueAsString(row, "step_id");
					
					// Put the "Actual Executed SQL Text" as a "tooltip"
					return "<div title='Click for Detailes' "
							+ "data-toggle='modal' "
							+ "data-target='#dbx-view-sqltext-dialog' "
							+ "data-objectname='" + ("step_id=" + stepId +", jobName=" + jobName) + "' "
							+ "data-tooltip=\""   + getTooltipFor_jobStepCommand(jobName, stepId) + "\" "
							+ ">&#x1F4AC;</div>"; // symbol popup with "..."
				}

				if ("message".equals(colName))
				{
					String subsystem   = rstm.getValueAsString (row, "subsystem",      true, "");
					int    msgNumber   = rstm.getValueAsInteger(row, "sql_message_id", true, -1);
					int    msgSeverity = rstm.getValueAsInteger(row, "sql_severity",   true, -1);

					return formatMessageString(strVal, "<BR>", subsystem, msgNumber, msgSeverity);
				}
			}

		//	return ResultSetTableModel.TableStringRenderer.super.cellValue(rstm, row, col, colName, objVal, strVal);
			return strVal;

		} // end: cellValue(...)

		@Override
		public String cellToolTip(ResultSetTableModel rstm, int row, int col, String colName, Object objVal, String strVal)
		{
			if ("JobName".equals(colName))
			{
				return getDescriptionForJobName(strVal);
			}
			// TODO Auto-generated method stub
			return ResultSetTableModel.TableStringRenderer.super.cellToolTip(rstm, row, col, colName, objVal, strVal);
	
		} // end: cellToolTip(...)

	} // end: class
	
}
