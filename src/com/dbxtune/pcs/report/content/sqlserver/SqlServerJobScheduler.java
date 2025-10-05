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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.invoke.MethodHandles;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.crypto.RuntimeCryptoException;

import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.SqlServerJobSchedulerExtractor;
import com.dbxtune.pcs.SqlServerJobSchedulerExtractor.SqlAgentInfo;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.pcs.report.content.SparklineHelper;
import com.dbxtune.pcs.report.content.SparklineHelper.SparklineResult;
import com.dbxtune.pcs.report.content.SparklineJfreeChart;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.DbUtils;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.TimeUtils;

public class SqlServerJobScheduler 
extends SqlServerAbstract
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public static final String PROPKEY_errors_skip_msg_numbers = "DailySummaryReport.SqlServerJobScheduler.skip.msg.numbers.csv";
//	public static final String DEFAULT_errors_skip_msg_numbers = "0, 1945, 8153, 15477, 50000";
	public static final String DEFAULT_errors_skip_msg_numbers = "50000";
	
	public static final String PROPKEY_errors_skip_below_severity = "DailySummaryReport.SqlServerJobScheduler.skip.severity.below";
	public static final int    DEFAULT_errors_skip_below_severity = 10; // 
	
	private List<String>        _miniChartJsList = new ArrayList<>();
	
	private boolean _pcsSchemaOrTableWasNotFound = false;

	private ResultSetTableModel _sysjobs     = null;
	private ResultSetTableModel _sysjobsteps = null;

	private ResultSetTableModel _job_history_overview_all = null;
	private ResultSetTableModel _job_history_overview     = null;
	private ResultSetTableModel _job_history_outliers     = null;
	private ResultSetTableModel _job_history_errors       = null;
	private ResultSetTableModel _job_history_errors_all   = null;

	/** Key=job_id, Val=CommandsToShowInTooltip */
	private Map<String, String> _jobCommandsMap = null;

	// The below are "Writers" used in methods: create/writeMessageText
	private StringWriter _js__jobId_stepId__to__allExecTimes = new StringWriter();
	private StringWriter _js__jobId__to__name                = new StringWriter();

	// The below Holds mapping that can be used by: HtmlTableRenderer
	private Map<String, List<String>>  _jobId_stepId__to__allExecTimes;
	private Map<String, String>        _jobId__to__name;
	private Map<String, StartStopTime> _jobId__to__startStopTime = new HashMap<>();
	
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
//		if (_job_history_outliers != null && _job_history_outliers.getRowCount() > 0)
//			return true;

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
		// get: Summary ALL
		//------------------------------------------------------------------------
		beginHtmlSubSection(w, "All Available Jobs Summary (both Enabled and Disabled)", "job_scheduler_allAvailable");
		
		if (_job_history_overview_all != null && _job_history_overview_all.getRowCount() > 0)
		{
//			w.append("<p>The summary table is ordered by 'sumTimeInSec'...</p> \n");
//			w.append("<p>The Historical Calculation (last columns) are based on the last " + overview_calcHistoricalDays + " days. <i>This can be changed with property <code>" + SqlServerJobSchedulerExtractor.PROPKEY_overview_calcHistoricalDays + " = ##</code></i></p>");
//			w.append(_job_history_overview_all.toHtmlTableString("sortable", true, true, null, htmlTableRenderer));

			String  divId       = "_job_history_overview_all__collapsable";
			boolean showAtStart = false;

			String htmlContent = ""
					+ "<p>Here you can see statistics for ALL jobs, even the ones that has not been started today<br> \n"
					+ "   Note: If a job is assigned to many scheduleders, the <code>next_scheduled_ts</code> is for the first scheduler about to start.</p> \n"
					+ "<p>Jobs are sorted by Job Name (disabled at the end, in gray)</p> \n"
					;

//			htmlContent += "<p style='color:red'><b>Found " + _job_history_errors_all.getRowCount() + " Job Steps with ERROR or WARNINGS in the recording period (no filter on Message Numbers)...</b></p>";
			htmlContent += _job_history_overview_all.toHtmlTableString("sortable", true, true, null, htmlTableRenderer);
			
			String showHideDiv = createShowHideDiv(divId, showAtStart, "Show/Hide all " + _job_history_overview_all.getRowCount() + " available Jobs...", htmlContent);
			
			w.append( msOutlookAlternateText(showHideDiv, "", "Note: " + _job_history_overview_all.getRowCount() + " Jobs are not shown in Microsoft Outlook, but available in a browser.") );
			
		}
		else
		{
			w.append("<p>NO Jobs was found...<br><br></p>");
		}
		endHtmlSubSection(w);

		
		//------------------------------------------------------------------------
		// get: Summary
		//------------------------------------------------------------------------
		beginHtmlSubSection(w, "Summary of All jobs in the Reporting Period", "job_scheduler_overview");
		
		if (_job_history_overview != null && _job_history_overview.getRowCount() > 0)
		{
			hasOverviewRecords = true;
			
			int overview_calcHistoricalDays = Configuration.getCombinedConfiguration().getIntProperty(SqlServerJobSchedulerExtractor.PROPKEY_overview_calcHistoricalDays, SqlServerJobSchedulerExtractor.DEFAULT_overview_calcHistoricalDays);

			String serverName = getReportingInstance().getServerName();
			
			String timeLinkUrl = "/api/cc/reports?srvName=" + serverName + "&reportName=sqlserver-job-scheduler-timeline&startTime=TODAY&onlyLevelZero=true";
			w.append("<p>The summary table is ordered by 'sumTimeInSec'...</p> \n");
			w.append("<p><a href='" + timeLinkUrl + "' target='_blank'>Open <i>Timeline</i> View</a> for All below jobs, for Today <a href='" + timeLinkUrl + "' target='_blank'>here</a> (same as open '<i>SQL Agent/Scheduler <b>Timeline</b> View for <b>Today</b></i>' on the start page)</p> \n");
			w.append("<p>The Historical Calculation (last columns, in gray) are based on the last " + overview_calcHistoricalDays + " days. <i>This can be changed with property <code>" + SqlServerJobSchedulerExtractor.PROPKEY_overview_calcHistoricalDays + " = ##</code></i></p>");
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

			if (_job_history_outliers != null && _job_history_outliers.getRowCount() > 0)
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

			if (_job_history_errors != null && _job_history_errors.getRowCount() > 0)
			{
				// in RED
				w.append("<p style='color:red'><b>Found " + _job_history_errors.getRowCount() + " Job Steps with ERROR or WARNINGS in the recording period...</b></p>");

				w.append(_job_history_errors.toHtmlTableString("sortable", true, true, null, htmlTableRenderer));
			}
			else
			{
				// in GREEN
				w.append("<p style='color:green'>Found 0 Job Steps with ERROR or WARNINGS in the recording period...<br><br></p>");
			}
			endHtmlSubSection(w);


			//------------------------------------------------------------------------
			// get: Errors ALL (no filter on Message Numbers etc)
			//------------------------------------------------------------------------
			if (_job_history_errors_all != null && _job_history_errors_all.getRowCount() > 0)
			{
				beginHtmlSubSection(w, "Job Steps with ERROR or WARNINGS (no filter on Message Numbers or Severity)", "job_step_with_errors_or_warnings_ALL");

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

		// Write some extra HTML and JavaScript to show Chart's on Jobs executions times by parsing 
		if (isFullMessageType())
		{
			//-----------------------------------------
			// Add modal popup for execution times of: (job_id, step_id)
			createStaticHtmlAndJavaScriptContent(w, getReportingInstance().getServerName());
			
			// add variable(s) for 'ExecTime' for all 'job_id' and 'step_id' so we can view them in detail (at a second level) modal dialog
			//createStaticJavaScript_lookupContent__jobId_stepId__to__allExecTimes(w, _tmpSaveConn, SourceType.PCS);
			w.append(_js__jobId_stepId__to__allExecTimes.toString());

			// we also need: 'jobId' to 'name'
			//createStaticJavaScript_lookupContent__jobId__to__name(w, _tmpSaveConn, SourceType.PCS);
			w.append(_js__jobId__to__name.toString());
		}
	}

	private static void setFromSecondsToHms(ResultSetTableModel rstm, int row, String sourceColumnName, String targetColumnName)
	{
		int seconds = rstm.getValueAsInteger(row, sourceColumnName, true, -1);
		if (seconds >= 0)
		{
			String hms = TimeUtils.secToTimeStrLong(seconds);
			rstm.setValueAtWithOverride(hms, row, targetColumnName);
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
		// add variable(s) for 'ExecTime' for all 'job_id' and 'step_id' so we can view them in detail (at a second level) modal dialog
		// The below does 2 things:
		//  1: Add code to a StringWriter, that later will be printed in the method: writeMessageText() 
		//  2: Add the values in Map '_jobId_stepId__to__allExecTimes' so we can use them in *this* code
//		_jobId_stepId__to__allExecTimes = createStaticJavaScript_lookupContent__jobId_stepId__to__allExecTimes(_js__jobId_stepId__to__allExecTimes, conn, SourceType.PCS);
		ReturnObject_allExecTimes retObj = createStaticJavaScript_lookupContent__jobId_stepId__to__allExecTimes(_js__jobId_stepId__to__allExecTimes, conn, SourceType.PCS);
		_jobId_stepId__to__allExecTimes = retObj.getTimeMap();

		// we also need: 'jobId' to 'name'
		_jobId__to__name = createStaticJavaScript_lookupContent__jobId__to__name(_js__jobId__to__name, conn, SourceType.PCS);


		
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
		// get: Overview ALL
		//================================
		sql = ""
			    + "SELECT \n"
			    + "     [job_name] \n"
			    + "    ,[enabled] \n"
			    + "    ,[exec_count] \n"
			    + "    ,'' AS [timeline] \n"
			    + "    ,'' AS [execGraph] \n"
			    + "    ,[step_count] \n"
			    + "    ,'' AS [stepCmds] \n"
			    + "    ,[last_started] \n"
			    + "    ,[days_since_last_start] \n"
			    + "    ,'' AS [sum_exec_hms] \n"
			    + "    ,'' AS [avg_exec_hms] \n"
			    + "    ,'' AS [max_exec_hms] \n"
			    + "    ,'' AS [min_exec_hms] \n"
			    + "    ,[sum_exec_seconds] \n"
			    + "    ,[avg_exec_seconds] \n"
			    + "    ,[max_exec_seconds] \n"
			    + "    ,[min_exec_seconds] \n"
			    + "    ,[next_scheduled_ts] \n"
			    + "    ,[next_scheduled_in_days] \n"
			    + "    ,[scheduler_name] \n"
			    + "    ,[scheduler_count] \n"
			    + "    ,'' AS [allExecTimes] \n"
			    + "    ,'' AS [allExecTimesChart] \n"
			    + "    ,[first_started] \n"
			    + "    ,[date_created] \n"
			    + "    ,[date_modified] \n"
			    + "    ,[last_modified_in_days] \n"
			    + "    ,[version_number] \n"
			    + "    ,[notify_eventlog_on] \n"
			    + "    ,[notify_mail_on] \n"
			    + "    ,[notify_mail] \n"
			    + "    ,[notify_netsend_on] \n"
			    + "    ,[notify_netsend] \n"
			    + "    ,[notify_page_on] \n"
			    + "    ,[notify_page] \n"
			    + "    ,[job_description] \n"
			    + "    ,[job_category] \n"
			    + "    ,[job_id] \n"

			    + "FROM [" + schema + "].[" + SqlServerJobSchedulerExtractor.SqlAgentInfo.job_history_overview_all + "] \n"
			    + "ORDER BY 2 DESC, 1 \n"
			    + "";
				
		_job_history_overview_all = executeQuery(conn, sql, true, "job_history_overview_all");
//		_job_history_overview_all.setHighlightSortColumns("sum_exec_seconds");

		// Set/fix 'HMS' columns
		for (int r=0; r<_job_history_overview_all.getRowCount(); r++)
		{
			setFromSecondsToHms(_job_history_overview_all, r, "sum_exec_seconds", "sum_exec_hms");
			setFromSecondsToHms(_job_history_overview_all, r, "avg_exec_seconds", "avg_exec_hms");
			setFromSecondsToHms(_job_history_overview_all, r, "max_exec_seconds", "max_exec_hms");
			setFromSecondsToHms(_job_history_overview_all, r, "min_exec_seconds", "min_exec_hms");
		}

		// Create SparkLine/charts for each "allExecTimes" at column "allExecTimesChart"
		for (int r=0; r<_job_history_overview_all.getRowCount(); r++)
		{
			String job_id  = _job_history_overview_all.getValueAsString(r, "job_id");
			String step_id = "0"; // in here we only want TOP Level

//			String allExecTimes = _job_history_overview_all.getValueAsString(r, "allExecTimes");
			String allExecTimes = getTooltipFor_allExecTimes(job_id, step_id);
			createSparklineForHistoryExecutions(allExecTimes, _job_history_overview_all, r, "allExecTimesChart");
		}

		_job_history_overview_all.setColumnDescription("job_name"                ,"Name of the JOB shat started this step");
		_job_history_overview_all.setColumnDescription("enabled"                 ,"1: If the job is ENABLED, 0: if the job is DISABLED.");
		_job_history_overview_all.setColumnDescription("exec_count"              ,"How many times has this job been executed (found in the history).");
		_job_history_overview_all.setColumnDescription("timeline"                ,"Click on the below for a TimeLine on 'today' or 'all executions'");
		_job_history_overview_all.setColumnDescription("execGraph"               ,"A tooltip/popup with timings of ALL jobs in the last ## days, click to see Graph on the exec times");
		_job_history_overview_all.setColumnDescription("step_count"              ,"How many steps does this job name have");
		_job_history_overview_all.setColumnDescription("stepCmds"                ,"A list of all commands in the job");

		_job_history_overview_all.setColumnDescription("last_started"            ,"Last date this job was started");
		_job_history_overview_all.setColumnDescription("days_since_last_start"   ,"Number of days since this job was started");
		_job_history_overview_all.setColumnDescription("sum_exec_hms"            ,"Summary of all execution time in Hour:Minute:Seconds");
		_job_history_overview_all.setColumnDescription("avg_exec_hms"            ,"Average execution time in Hour:Minute:Seconds");
		_job_history_overview_all.setColumnDescription("max_exec_hms"            ,"Maximum execution time in Hour:Minute:Seconds");
		_job_history_overview_all.setColumnDescription("min_exec_hms"            ,"Minimum execution time in Hour:Minute:Seconds");
		_job_history_overview_all.setColumnDescription("sum_exec_seconds"        ,"Summary of all execution time in Seconds");
		_job_history_overview_all.setColumnDescription("avg_exec_seconds"        ,"Average execution time in Seconds");
		_job_history_overview_all.setColumnDescription("max_exec_seconds"        ,"Maximum execution time in Seconds");
		_job_history_overview_all.setColumnDescription("min_exec_seconds"        ,"Minimum execution time in Seconds");
		_job_history_overview_all.setColumnDescription("next_scheduled_ts"       ,"Date when this job will be scheduled the next time");
		_job_history_overview_all.setColumnDescription("next_scheduled_in_days"  ,"Number of days when this job will be scheduled the next time");
		_job_history_overview_all.setColumnDescription("scheduler_name"          ,"What scheduler is responsible for starting the next execution of this job.");
		_job_history_overview_all.setColumnDescription("scheduler_count"         ,"Normally 1, but if the job is assigned to several schedulers it will be above 1");
		_job_history_overview_all.setColumnDescription("allExecTimes"            ,"A tooltip/popup with timings of ALL jobs in the last ## days, click to see Graph on the exec times");
		_job_history_overview_all.setColumnDescription("allExecTimesChart"       ,"A Chart with the timings of ALL jobs in the last ## days");

		_job_history_overview_all.setColumnDescription("first_started"           ,"First time this job started");
		_job_history_overview_all.setColumnDescription("date_created"            ,"When was this job created.");
		_job_history_overview_all.setColumnDescription("date_modified"           ,"When was this job modified.");
		_job_history_overview_all.setColumnDescription("last_modified_in_days"   ,"How many days has gone since the job was last modified");
		_job_history_overview_all.setColumnDescription("version_number"          ,"Internal Version Number of this job");

		_job_history_overview_all.setColumnDescription("notify_eventlog_on"      ,"Send notifications to Windows Event Log");
		_job_history_overview_all.setColumnDescription("notify_mail_on"          ,"Send notifications via email");
		_job_history_overview_all.setColumnDescription("notify_mail"             ,"What 'emails' will we send to");
		_job_history_overview_all.setColumnDescription("notify_netsend_on"       ,"Send notifications via netsend");
		_job_history_overview_all.setColumnDescription("notify_netsend"          ,"What 'netsenders' will we send to");
		_job_history_overview_all.setColumnDescription("notify_page_on"          ,"Send notifications via pager");
		_job_history_overview_all.setColumnDescription("notify_page"             ,"What 'pagers' will we send to");

		_job_history_overview_all.setColumnDescription("job_description"         ,"A description of the job");
		_job_history_overview_all.setColumnDescription("job_id"                  ,"The ID of the JOB");

		// Get All JobStep Commands for a 'job_id', and put it in a Map, used later to compose a ToolTip
		getJobStepCommandsTooltipForJobId(conn, _job_history_overview_all, "job_id");
		
		//================================
		// get: Overview
		//================================
		sql = ""
			    + "SELECT \n"
			    + "     [JobName] \n"
			    + "    ,'' AS [timeline] \n"
			    + "    ,'' AS [histGraph] \n"
			    + "    ,'' AS [execView] \n"
			    + "    ,[stepCount] \n"
			    + "    ,'' AS [stepCmds] \n"
			    + "    ,[runStatusDesc] \n"
			    + "    ,[execCount] \n"
			    + "    ,[sumTimeInSec] \n"
			    + "    ,[sumTime_HMS] \n"
			    + "    ,[avgTimeInSec] \n"
			    + "    ,[avgTime_HMS] \n"
			    + "    ,'' AS [avgHistTimeDiff] \n"
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
				+ "    ,'' AS [histAllExecTimes] \n"
			    + "    ,'' AS [histAllExecTimesChart] \n" // Fill this one with a chart of "histAllExecTimes"

			    + "    ,[job_id] \n"

			    + "FROM [" + schema + "].[" + SqlServerJobSchedulerExtractor.SqlAgentInfo.job_history_overview + "] \n"
			    + "ORDER BY [sumTimeInSec] DESC \n"
			    + "";
				
		_job_history_overview = executeQuery(conn, sql, true, "job_history_overview");
//		_job_history_overview.setHighlightSortColumns("avgTimeInSec");
		_job_history_overview.setHighlightSortColumns("sumTimeInSec");

		// Populate: _jobId__to__startStopTime 
		for (int r=0; r<_job_history_overview.getRowCount(); r++)
		{
			String    jobId         = _job_history_overview.getValueAsString   (r, "job_id");
			Timestamp firstExecTime = _job_history_overview.getValueAsTimestamp(r, "firstExecTime");
			Timestamp lastExecTime  = _job_history_overview.getValueAsTimestamp(r, "lastExecTime");
			int       avgTimeInSec  = _job_history_overview.getValueAsInteger  (r, "avgTimeInSec");
			
			// NOTE: 'lastExecTime' is NOT the stop time, so we need to add some execution time
			//       So lets add the 'avgTimeInSec'... It might not be 100% correct, but...
			lastExecTime = new Timestamp( lastExecTime.getTime() + (avgTimeInSec * 1000) );

			_jobId__to__startStopTime.put(jobId, new StartStopTime(firstExecTime, lastExecTime));
		}

		// Create sparkline/charts for each "histAllExecTimes" at column "histAllExecTimesChart"
		for (int r=0; r<_job_history_overview.getRowCount(); r++)
		{
			String job_id  = _job_history_overview.getValueAsString(r, "job_id");
			String step_id = "0"; // in here we only want TOP Level

//			String histAllExecTimes = _job_history_overview.getValueAsString(r, "histAllExecTimes");
			String histAllExecTimes = getTooltipFor_allExecTimes(job_id, step_id);
			createSparklineForHistoryExecutions(histAllExecTimes, _job_history_overview, r, "histAllExecTimesChart");
			
			// calculate the 'avgHistTimeDiff' column
			int avgTimeInSec     = _job_history_overview.getValueAsInteger(r, "avgTimeInSec");
			int histAvgTimeInSec = _job_history_overview.getValueAsInteger(r, "histAvgTimeInSec");
			int avgHistTimeDiff = avgTimeInSec - histAvgTimeInSec;
			String avgHistTimeDiffStr = TimeUtils.msToTimeStrDHMS(Math.abs(avgHistTimeDiff) * 1000);  // Math.abs() make negative numbers positive
			if (avgHistTimeDiff < 0)
				avgHistTimeDiffStr = "-" + avgHistTimeDiffStr + " <span style='color: green;'>(faster)</span>";
			if (avgHistTimeDiff > 0)
				avgHistTimeDiffStr =       avgHistTimeDiffStr + " <span style='color: red;'>(slower)</span>";

			int pos_avgHistTimeDiff = _job_history_overview.findColumn("avgHistTimeDiff");
			_job_history_overview.setValueAtWithOverride(avgHistTimeDiffStr, r, pos_avgHistTimeDiff);
		}
		
		_job_history_overview.setColumnDescription("JobName"           ,"Name of the JOB shat started this step");
		_job_history_overview.setColumnDescription("timeline"          ,"Click on the below for a TimeLine on 'today' or 'all executions'");
		_job_history_overview.setColumnDescription("histGraph"         ,"A tooltip/popup with timings of ALL jobs in the last ## days, click to see Graph on the exec times");
		_job_history_overview.setColumnDescription("execView"          ,"Open DbxTune/DbxCentral in historical mode and position to the start time of the job/step");
		_job_history_overview.setColumnDescription("stepCount"         ,"How many steps does this job name have");
		_job_history_overview.setColumnDescription("stepCmds"          ,"A list of all commands in the job");
		_job_history_overview.setColumnDescription("runStatusDesc"     ,"The outcome of the job. Can be: FAILED, SUCCESS, RETRY, CANCELED or IN PROGRESS");
		_job_history_overview.setColumnDescription("execCount"         ,"How many times has this JOB been executed in the reporting period");
		_job_history_overview.setColumnDescription("sumTimeInSec"      ,"Summary Execution time in Seconds for all Executions in reporting period");
		_job_history_overview.setColumnDescription("sumTime_HMS"       ,"Summary Execution time in Hour:Minute:Seconds for all Executions in reporting period");
		_job_history_overview.setColumnDescription("avgTimeInSec"      ,"Average Execution time in Seconds for all Executions in reporting period");
		_job_history_overview.setColumnDescription("avgTime_HMS"       ,"Average Execution time in Hour:Minute:Seconds for all Executions in reporting period");
		_job_history_overview.setColumnDescription("avgHistTimeDiff"   ,"The difference of 'avgTime_HMS' and 'histAvgTime_HMS' this could be a positive (Slower) or negative (faster) than the average history.");
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
		_job_history_overview.setColumnDescription("histAllExecTimes"  ,"A tooltip/popup with timings of ALL jobs in the last ## days, click to see Graph on the exec times");
		_job_history_overview.setColumnDescription("histAllExecTimesChart"  ,"A Chart with the timings of ALL jobs in the last ## days");

		_job_history_overview.setColumnDescription("job_id"            ,"The ID of the JOB");

		// Get All JobStep Commands for a 'job_id', and put it in a Map, used later to compose a ToolTip
		getJobStepCommandsTooltipForJobId(conn, _job_history_overview, "job_id");
		
		//================================
		// get: Outliers
		//================================
		sql = ""
		    + "SELECT \n"
		    + "     [execTime] \n"
		    + "    ,[JobName] \n"
		    + "    ,'' AS [timeline] \n"
		    + "    ,'' AS [histGraph] \n"
		    + "    ,'' AS [execView] \n"
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
		    + "    ,'' AS [histAllExecTimes] \n"
		    + "    ,'' AS [histAllExecTimesChart] \n" // Fill this one with a chart of "histAllExecTimes"
		    + "    ,[job_id] \n"
		    + "FROM [" + schema + "].[" + SqlServerJobSchedulerExtractor.SqlAgentInfo.job_history_outliers + "] \n"
		    + "ORDER BY [execTime] \n"
		    + "";
			
		_job_history_outliers = executeQuery(conn, sql, true, "job_history_outliers");

		// Create sparkline/charts for each "histAllExecTimes" at column "histAllExecTimesChart"
		int pos_histAllExecTimes = _job_history_outliers.findColumn("histAllExecTimes");
		for (int r=0; r<_job_history_outliers.getRowCount(); r++)
		{
			String job_id  = _job_history_outliers.getValueAsString(r, "job_id");
			String step_id = _job_history_outliers.getValueAsString(r, "step_id");

//			String histAllExecTimes = _job_history_outliers.getValueAsString(r, pos_histAllExecTimes);
			String histAllExecTimes = getTooltipFor_allExecTimes(job_id, step_id);
			createSparklineForHistoryExecutions(histAllExecTimes, _job_history_outliers, r, "histAllExecTimesChart");
			
			histAllExecTimes = formatAllExecTimes(histAllExecTimes);
			_job_history_outliers.setValueAtWithOverride(histAllExecTimes, r, pos_histAllExecTimes);
		}
		
		int outliers_calcHistoricalDays = Configuration.getCombinedConfiguration().getIntProperty(SqlServerJobSchedulerExtractor.PROPKEY_outliers_calcHistoricalDays, SqlServerJobSchedulerExtractor.DEFAULT_outliers_calcHistoricalDays);
		
		_job_history_outliers.setColumnDescription("execTime"              ,"When was it executed");
		_job_history_outliers.setColumnDescription("JobName"               ,"Name of the JOB shat started this step");
		_job_history_outliers.setColumnDescription("timeline"              ,"Click on the below for a TimeLine on 'today'.");
		_job_history_outliers.setColumnDescription("histGraph"             ,"A tooltip/popup with timings of ALL job steps in the last ## days, click to see Graph on the exec times");
		_job_history_outliers.setColumnDescription("execView"              ,"Open DbxTune/DbxCentral in historical mode and position to the start time of the job/step");
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
		
		// TODO: Check if above variable 'skipMsgList' entries are numbers ???... otherwise we will get Syntax Error...

		String sqlSkipMessageIds = "";
		if ( ! skipMsgList.isEmpty() )
		{
			sqlSkipMessageIds = "  AND ([sql_message_id] NOT IN(" + StringUtil.toCommaStr(skipMsgList) + ") AND [subsystem] = 'TSQL') \n";
		}

		int agent_skip_severityBelow = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_errors_skip_below_severity, DEFAULT_errors_skip_below_severity); 

		String sqlSkipBelowSeverity = "";
		if (agent_skip_severityBelow > 0)
		{
			sqlSkipBelowSeverity = "  AND ([sql_severity] >= " + agent_skip_severityBelow + " AND [subsystem] = 'TSQL') \n";
		}

		
		String sql_whereMessageLike = "";
		String searchErrorMessage = Configuration.getCombinedConfiguration().getProperty(SqlServerJobSchedulerExtractor.PROPKEY_search_errorMessage, SqlServerJobSchedulerExtractor.DEFAULT_search_errorMessage);
		if (StringUtil.hasValue(searchErrorMessage)) 
		{
			sql_whereMessageLike    = "   OR [message] LIKE '%" + searchErrorMessage + "%' \n";
		}
		
		sql = ""
		    + "SELECT "
		    + "     [execTime] \n"
		    + "    ,[JobName] \n"
		    + "    ,[step_name] \n"
		    + "    ,[step_id] \n"
		    + "    ,'' AS [timeline] \n"
		    + "    ,'' AS [histGraph] \n"
		    + "    ,'' AS [execView] \n"
		    + "    ,[subsystem] \n"
		    + "    ,'' AS [cmd] \n"
		    + "    ,[run_status_desc] \n"
		    + "    ,[retries_attempted] \n"
		    + "    ,[sql_message_id] \n"
		    + "    ,[sql_severity] \n"
		    + "    ,[message] \n"
		    + "    ,[job_id] \n"
		    + "FROM [" + schema + "].[" + SqlServerJobSchedulerExtractor.SqlAgentInfo.job_history_errors + "] \n"
		    + "WHERE 1 = 1 \n"
		    + sqlSkipMessageIds
		    + sqlSkipBelowSeverity
//		    + " OR [subsystem] <> 'TSQL' \n"
		    + sql_whereMessageLike
		    + "ORDER BY [execTime] \n"
		    + "";

		_job_history_errors = executeQuery(conn, sql, true, "job_history_errors");

		_job_history_errors.setColumnDescription("execTime"          ,"When was it executed");
		_job_history_errors.setColumnDescription("JobName"           ,"Name of the JOB shat started this step");
		_job_history_errors.setColumnDescription("step_name"         ,"Name of the job step");
		_job_history_errors.setColumnDescription("step_id"           ,"The step_id in the job");
		_job_history_errors.setColumnDescription("timeline"          ,"Click on the below for a TimeLine on 'today'.");
		_job_history_errors.setColumnDescription("histGraph"         ,"A tooltip/popup with timings of ALL job steps in the last ## days, click to see Graph on the exec times");
		_job_history_errors.setColumnDescription("execView"          ,"Open DbxTune/DbxCentral in historical mode and position to the start time of the job/step");
		_job_history_errors.setColumnDescription("histGraph"         ,"A tooltip/popup with timings of ALL job steps in the last ## days, click to see Graph on the exec times");
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
		    + "    ,'' AS [timeline] \n"
		    + "    ,'' AS [histGraph] \n"
		    + "    ,'' AS [execView] \n"
		    + "    ,[subsystem] \n"
		    + "    ,'' AS [cmd] \n"
		    + "    ,[run_status_desc] \n"
		    + "    ,[retries_attempted] \n"
		    + "    ,[sql_message_id] \n"
		    + "    ,[sql_severity] \n"
		    + "    ,[message] \n"
		    + "    ,[job_id] \n"
		    + "FROM [" + schema + "].[" + SqlServerJobSchedulerExtractor.SqlAgentInfo.job_history_errors + "] \n"
		    + "ORDER BY [execTime] \n"
		    + "";
				
		_job_history_errors_all = executeQuery(conn, sql, true, "job_history_errors_all");
//System.out.println("_job_history_errors_all:\n" + _job_history_errors_all.toAsciiTableString());

		_job_history_errors_all.setColumnDescription("execTime"          ,"When was it executed");
		_job_history_errors_all.setColumnDescription("JobName"           ,"Name of the JOB shat started this step");
		_job_history_errors_all.setColumnDescription("step_name"         ,"Name of the job step");
		_job_history_errors_all.setColumnDescription("step_id"           ,"The step_id in the job");
		_job_history_errors_all.setColumnDescription("timeline"          ,"Click on the below for a TimeLine on 'today'.");
		_job_history_errors_all.setColumnDescription("histGraph"         ,"A tooltip/popup with timings of ALL job steps in the last ## days, click to see Graph on the exec times");
		_job_history_errors_all.setColumnDescription("execView"          ,"Open DbxTune/DbxCentral in historical mode and position to the start time of the job/step");
		_job_history_errors_all.setColumnDescription("subsystem"         ,"What subsystem was used to execute");
		_job_history_errors_all.setColumnDescription("run_status_desc"   ,"The outcome of the job. Can be: FAILED, SUCCESS, WARNING, RETRY, CANCELED or IN PROGRESS");
		_job_history_errors_all.setColumnDescription("job_id"            ,"Just the ID of the job if you want to know");
		_job_history_errors_all.setColumnDescription("retries_attempted" ,"What is says");
		_job_history_errors_all.setColumnDescription("sql_message_id"    ,"SQL Server error number, if the SQL Job produced any errors");
		_job_history_errors_all.setColumnDescription("sql_severity"      ,"SQL Server error severity, if the SQL Job produced any errors");
		_job_history_errors_all.setColumnDescription("message"           ,"Any messages produced by the output");
	}
	



	/**
	 * Get ALL "step" Commands for a specific job id, and store them in the Map '_jobCommandsMap'
	 * <p>
	 * Use <code>getTooltipFor_jobAllCommands(jobId)</code> to get information for the above Map 
	 * 
	 * @param conn
	 * @param rstm
	 * @param colName
	 */
	private void getJobStepCommandsTooltipForJobId(DbxConnection conn, ResultSetTableModel rstm, String colName)
	{
		if (_jobCommandsMap == null)
			_jobCommandsMap = new HashMap<>();

		if (rstm== null)
		{
			_logger.error("getJobStepCommandsTooltipForJobId(): rstm can't be NULL. Skipping lookups...");
			return;
		}

		int colPos = rstm.findColumn(colName);
		if (colPos == -1)
		{
			_logger.error("getJobStepCommandsTooltipForJobId(): rstm.name=|" + rstm.getName() + "|, cant find column '" + colName + "'. Skipping lookups...");
			return;
		}

		// Get records and add them to: _jobCommandsMap
		for (int r=0; r<rstm.getRowCount(); r++)
		{
			String job_id = rstm.getValueAsString(r, colPos);
			
			if ( ! _jobCommandsMap.containsKey(job_id) )
			{
				String tooltip = getTooltipFor_jobAllCommands(conn, job_id);
				_jobCommandsMap.put(job_id, tooltip);
			}
		}
	}

	/**
	 * Format the history in some "clever" way... <br>
	 * For example with <NL> when appropriate (for example after Sunday, or when a new DATE occurs)
	 * 
	 * @param histAllExecTimes
	 * @return
	 */
	public static String formatAllExecTimes(String histAllExecTimes)
	{
//TODO; // implement this 
		return histAllExecTimes;
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

//	private String getTooltipFor_histAllExecTimes(String job_id, String strVal)
//	{
//		// not really using job_id here
//		
//		if (StringUtil.isNullOrBlank(strVal))
//			return "";
//
//		return strVal;
//		
//		// Newlines to HTML-Newlines
////		return strVal.replace("\n", "<BR>");
//	}
	private String getTooltipFor_allExecTimes(String job_id, String step_id)
	{
		String key = job_id + "____" + step_id;
		List<String> list = _jobId_stepId__to__allExecTimes.get(key);
		
		if (list == null || (list != null && list.isEmpty()) )
			return "No Execution Times was found for: jobId=" + job_id + ", stepId=" + step_id;

		StringBuilder sb = new StringBuilder();
		for (String entry : list)
			sb.append(entry).append("\n");

		return sb.toString();
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
				if ( ! colName.equals("histGraph"))
				{
					attr = "style='color:#9C7B57;'"; // #BA9368; == Camel
//					attr = "style='background-color:#EDC9AF;'"; // #EDC9AF; == Desert Sand
				}
			}

			return attr;
		}

		@Override
		public String tagTrAttr(ResultSetTableModel rstm, int row)
		{
			if ("job_history_overview_all".equals(rstm.getName()))
			{
				// Show DISABLED rows in "gray"
				int enabled = rstm.getValueAsInteger(row, "enabled", true, 1);
				if (enabled == 0)
				{
					return "style='color: gray;'";
				}
			}
			
			return null;
		}
		@Override
		public String cellValue(ResultSetTableModel rstm, int row, int col, String colName, Object objVal, String strVal)
		{
			//--------------------------------------------------------------
			// job_history_overview_all
			//--------------------------------------------------------------
			if ("job_history_overview_all".equals(rstm.getName()))
			{
				if ("timeline".equals(colName))
				{
					String jobName           = rstm.getValueAsString(row, "job_name"         , true, "unknown");
					String jobId             = rstm.getValueAsString(row, "job_id"           , true, "unknown");
					String firstExecTime     = rstm.getValueAsString(row, "first_started"    , true, "unknown");
					String lastExecTime      = rstm.getValueAsString(row, "last_started"     , true, "unknown");

//					String serverName = getReportingInstance().getDbmsServerName();
					String serverName = getReportingInstance().getServerName();
					String urlBase = ""
							+ "/api/cc/reports"
							+ "?srvName="    + serverName 
							+ "&reportName=" + "sqlserver-job-scheduler-timeline";

					return "" 
						+ "<a href='" + urlBase 
								+ "&jobName="              + jobName 
								+ "&jobId="                + jobId 
								+ "&onlyLevelZero="        + true
								+ "&refresh="              + 0
								+ "&startTime="            + firstExecTime 
								+ "&endTime="              + lastExecTime 
								+ "&minDurationInSeconds=" + 0
								+ "' target='_blank'>All Executions</a>"
						;
				}
				

				if ("stepCmds".equals(colName))
				{
					// Get properties we want to pass
					String job_name   = rstm.getValueAsString(row, "job_name");
					String job_id     = rstm.getValueAsString(row, "job_id");
					
					return "<div title='Click to Open' "
							+ "data-toggle='modal' "
							+ "data-target='#dbx-view-sqltext-dialog' "
							+ "data-objectname='" + job_name + "' "
							+ "data-tooltip=\""   + getTooltipFor_jobAllCommands(job_id) + "\" "
							+ ">&#x1F4AC; Show</div>"; // symbol popup with "..."
				}

				if ("allExecTimes".equals(colName))
				{
					// Get properties we want to pass
					String job_name   = rstm.getValueAsString(row, "job_name");
					String job_id     = rstm.getValueAsString(row, "job_id");
					String step_id    = "0"; // We only want to so the TOP LEVEL

					return "<div title='Click to Open' "
							+ "data-toggle='modal' "
							+ "data-target='#dbx-jobScheduler-timeline-dialog' "
							+ "data-objectname='" + job_name + "' "
							+ "data-tooltip=\""   + getTooltipFor_allExecTimes(job_id, step_id) + "\" "  // stepId=0 --- We only want to so the TOP LEVEL
							+ ">&#x1F4AC; Show</div>"; // symbol popup with "..."
				}
				// Same as above ("allExecTimes")
				if ("execGraph".equals(colName))
				{
					// Get properties we want to pass
					String job_name   = rstm.getValueAsString(row, "job_name");
					String job_id     = rstm.getValueAsString(row, "job_id");
					String step_id    = "0"; // We only want to so the TOP LEVEL

					return "<div title='Click to Open' "
							+ "data-toggle='modal' "
							+ "data-target='#dbx-jobScheduler-timeline-dialog' "
							+ "data-objectname='" + job_name + "' "
							+ "data-tooltip=\""   + getTooltipFor_allExecTimes(job_id, step_id) + "\" "  // stepId=0 --- We only want to so the TOP LEVEL
							+ ">&#x1F4AC; Show</div>"; // symbol popup with "..."
				}
			}
			
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
				
				if ("timeline".equals(colName))
				{
					String jobName           = rstm.getValueAsString(row, "JobName"          , true, "unknown");
					String jobId             = rstm.getValueAsString(row, "job_id"           , true, "unknown");
					String firstExecTime     = rstm.getValueAsString(row, "firstExecTime"    , true, "unknown");
					String lastExecTime      = rstm.getValueAsString(row, "lastExecTime"     , true, "unknown");
					String sumTimeInSec      = rstm.getValueAsString(row, "sumTimeInSec"     , true, "unknown");
//					String sumTime_HMS       = rstm.getValueAsString(row, "sumTime_HMS"      , true, "unknown");
					String histFirstExecTime = rstm.getValueAsString(row, "histFirstExecTime", true, "unknown");
//					String histAllExecTimes  = rstm.getValueAsString(row, "histAllExecTimes", true, "unknown");

					// lastExecTime...
					if ("-single-exec-".equals(lastExecTime))
					{
					//	lastExecTime = "1";
						int sumTimeInHours = Math.max(StringUtil.parseInt(sumTimeInSec, 1) / 3600, 1);
						lastExecTime = sumTimeInHours + "";
					}
//					String serverName = getReportingInstance().getDbmsServerName();
					String serverName = getReportingInstance().getServerName();
					String urlBase = ""
							+ "/api/cc/reports"
							+ "?srvName="    + serverName 
							+ "&reportName=" + "sqlserver-job-scheduler-timeline";

					return "" 
						+ "<a href='" + urlBase 
								+ "&jobName="              + jobName 
								+ "&jobId="                + jobId 
								+ "&onlyLevelZero="        + false
								+ "&refresh="              + 0 
								+ "&startTime="            + firstExecTime     
								+ "&endTime="              + lastExecTime 
								+ "&minDurationInSeconds=" + 0
								+ "' target='_blank'>Today</a>"
						+ " - "
						+ "<a href='" + urlBase 
								+ "&jobName="              + jobName 
								+ "&jobId="                + jobId 
								+ "&onlyLevelZero="        + true
								+ "&refresh="              + 0
								+ "&startTime="            + histFirstExecTime 
								+ "&minDurationInSeconds=" + 0
								+ "' target='_blank'>All</a>"
						;
				}
				

				if ("stepCmds".equals(colName))
				{
					// Get properties we want to pass
					String JobName    = rstm.getValueAsString(row, "JobName");
					String job_id     = rstm.getValueAsString(row, "job_id");
					
					return "<div title='Click to Open' "
							+ "data-toggle='modal' "
							+ "data-target='#dbx-view-sqltext-dialog' "
							+ "data-objectname='" + JobName + "' "
							+ "data-tooltip=\""   + getTooltipFor_jobAllCommands(job_id) + "\" "
							+ ">&#x1F4AC; Show</div>"; // symbol popup with "..."
				}

				if ("histAllExecTimes".equals(colName))
				{
					// Get properties we want to pass
					String jobName    = rstm.getValueAsString(row, "JobName");
					String job_id     = rstm.getValueAsString(row, "job_id");
					String step_id    = "0"; // We only want to so the TOP LEVEL

					return "<div title='Click to Open' "
							+ "data-toggle='modal' "
							+ "data-target='#dbx-jobScheduler-timeline-dialog' "
							+ "data-objectname='" + jobName + "' "
							+ "data-tooltip=\""   + getTooltipFor_allExecTimes(job_id, step_id) + "\" "
							+ ">&#x1F4AC; Show</div>"; // symbol popup with "..."
				}
				// Same as above ("histAllExecTimes")
				if ("histGraph".equals(colName))
				{
					// Get properties we want to pass
					String jobName    = rstm.getValueAsString(row, "JobName");
					String job_id     = rstm.getValueAsString(row, "job_id");
					String step_id    = "0"; // We only want to so the TOP LEVEL

					return "<div title='Click to Open' "
							+ "data-toggle='modal' "
							+ "data-target='#dbx-jobScheduler-timeline-dialog' "
							+ "data-objectname='" + jobName + "' "
							+ "data-tooltip=\""   + getTooltipFor_allExecTimes(job_id, step_id) + "\" "
							+ ">&#x1F4AC; Show</div>"; // symbol popup with "..."
				}

				if ("execView".equals(colName))
				{
					// Get properties we want to pass
					String jobId    = rstm.getValueAsString(row, "job_id"           , true, "unknown");
					String execTime = rstm.getValueAsString(row, "firstExecTime"    , true, "unknown");

					if (StringUtil.hasValue(execTime))
						execTime = execTime.substring(0, "YYYY-mm-dd HH:MM:SS".length());

					String serverName = getReportingInstance().getServerName();
					String url = ""
							+ "/graph.html"
							+ "?subscribe="     + false 
							+ "&sessionName="   + serverName 
							+ "&startTime="     + getJobStartTime(jobId, 15)
							+ "&endTime="       + getJobEndTime  (jobId, 15)
							+ "&markTime="      + execTime
							+ "&markStartTime=" + getJobStartTime(jobId, 0)
							+ "&markEndTime="   + getJobEndTime  (jobId, 0)
							;

					// Change "'" and "\n" into html characters: "'"=>"&#39;" and "\n"=>"&#10;"
					String tooltip = "Open DbxTune/DbxCentral in historical mode and position you at the below 'markTime'.\n"
							+ "startTime= '" + getJobStartTime(jobId, 15) + "'\n"
							+ "endTime=  '"  + getJobEndTime  (jobId, 15) + "'\n"
							+ "markTime='"   + execTime               + "'\n"
							+ "\n"
							+ "Note: 15 minutes have been added to the start/end time from the origin 'firstExecTime' and 'lastExecTime' of JobId '" + jobId + "'.";
					tooltip = tooltip.replace("'", "&#39;").replace("\n", "&#10;");
					return "<a href='" + url + "' title='" + tooltip + "' target='_blank'>DbxTune</a>";
				}
			}
			
			//--------------------------------------------------------------
			// job_history_outliers
			//--------------------------------------------------------------
			if ("job_history_outliers".equals(rstm.getName()))
			{
				if ("timeline".equals(colName))
				{
					String jobName  = rstm.getValueAsString(row, "JobName"     , true, "unknown");
					String jobId    = rstm.getValueAsString(row, "job_id"      , true, "unknown");
					String stepId   = rstm.getValueAsString(row, "step_id"     , true, "unknown");
//					String execTime = rstm.getValueAsString(row, "execTime"    , true, "unknown");
					String execTime = "TODAY";

					String serverName = getReportingInstance().getServerName();
					String urlBase = ""
							+ "/api/cc/reports"
							+ "?srvName="    + serverName 
							+ "&reportName=" + "sqlserver-job-scheduler-timeline";

					return "" 
						+ "<a href='" + urlBase 
								+ "&jobName="              + jobName 
								+ "&jobId="                + jobId 
//								+ "&stepId="               + stepId // NOT YET IMPLEMENTED
								+ "&onlyLevelZero="        + true
								+ "&refresh="              + 0
								+ "&startTime="            + execTime 
								+ "&minDurationInSeconds=" + 0
								+ "' target='_blank'>Today</a>"
						;
				}

				if ("cmd".equals(colName))
				{
					// Get properties we want to pass
					String jobName = rstm.getValueAsString(row, "JobName");
					String stepId  = rstm.getValueAsString(row, "step_id");
					
					return "<div title='Click to Open' "
							+ "data-toggle='modal' "
							+ "data-target='#dbx-view-sqltext-dialog' "
							+ "data-objectname='" + ("step_id=" + stepId +", jobName=" + jobName) + "' "
							+ "data-tooltip=\""   + getTooltipFor_jobStepCommand(jobName, stepId) + "\" "
							+ ">&#x1F4AC; Show</div>"; // symbol popup with "..."
				}

				if ("histAllExecTimes".equals(colName))
				{
					// Get properties we want to pass
					String jobName    = rstm.getValueAsString(row, "JobName");
					String job_id     = rstm.getValueAsString(row, "job_id");
					String step_id    = rstm.getValueAsString(row, "step_id");
					String execTime   = rstm.getValueAsString(row, "execTime");
					
					if (StringUtil.hasValue(execTime))
						execTime = execTime.substring(0, "YYYY-mm-dd HH:MM:SS".length());

					return "<div title='Click to Open' "
							+ "data-toggle='modal' "
							+ "data-target='#dbx-jobScheduler-timeline-dialog' "
							+ "data-objectname='" + jobName  + "' "
							+ "data-objectname='" + ("step_id=" + step_id +", jobName=" + jobName + ", execTime=" + execTime) + "' "
							+ "data-starttime='"  + execTime + "' "
							+ "data-tooltip=\""   + getTooltipFor_allExecTimes(job_id, step_id) + "\" "
							+ ">&#x1F4AC; Show</div>" // symbol popup with "..."
							;
				}
				// Same as above ("histAllExecTimes")
				if ("histGraph".equals(colName))
				{
					// Get properties we want to pass
					String jobName    = rstm.getValueAsString(row, "JobName");
					String job_id     = rstm.getValueAsString(row, "job_id");
					String step_id    = rstm.getValueAsString(row, "step_id");
					String execTime   = rstm.getValueAsString(row, "execTime");
					
					if (StringUtil.hasValue(execTime))
						execTime = execTime.substring(0, "YYYY-mm-dd HH:MM:SS".length());

					return "<div title='Click to Open' "
							+ "data-toggle='modal' "
							+ "data-target='#dbx-jobScheduler-timeline-dialog' "
							+ "data-objectname='" + jobName  + "' "
							+ "data-objectname='" + ("step_id=" + step_id +", jobName=" + jobName + ", execTime=" + execTime) + "' "
							+ "data-starttime='"  + execTime + "' "
							+ "data-tooltip=\""   + getTooltipFor_allExecTimes(job_id, step_id) + "\" "
							+ ">&#x1F4AC; Show</div>" // symbol popup with "..."
							;
				}

				if ("execView".equals(colName))
				{
					// Get properties we want to pass
					String jobId      = rstm.getValueAsString(row, "job_id");
					String stepId     = rstm.getValueAsString(row, "step_id");
					String execTime   = rstm.getValueAsString(row, "execTime");

					if (StringUtil.hasValue(execTime))
						execTime = execTime.substring(0, "YYYY-mm-dd HH:MM:SS".length());

//					String startTime     = getJobStartTime(jobId);
//					String endTime       = getJobEndTime  (jobId);
					String startTime     = getStartTime(jobId, stepId, execTime, 15);
					String endTime       = getEndTime  (jobId, stepId, execTime, 15);
					String markStartTime = getStartTime(jobId, stepId, execTime, 0);
					String markEndTime   = getEndTime  (jobId, stepId, execTime, 0);
					
					String serverName = getReportingInstance().getServerName();
					String url = ""
							+ "/graph.html"
							+ "?subscribe="     + false 
							+ "&sessionName="   + serverName 
							+ "&startTime="     + startTime
							+ "&endTime="       + endTime
							+ "&markTime="      + execTime
							+ "&markStartTime=" + markStartTime
							+ "&markEndTime="   + markEndTime
							;

					// Change "'" and "\n" into html characters: "'"=>"&#39;" and "\n"=>"&#10;"
					String tooltip = "Open DbxTune/DbxCentral in historical mode and position you at the below 'markTime'.\n"
							+ "startTime ='" + startTime + "'\n"
							+ "endTime  ='"  + endTime   + "'\n"
							+ "markTime='"   + execTime  + "'\n"
							+ "\n"
							+ "Note: 15 minutes have been added to the start/end time from the origin stepId='" + stepId + "', execTime='" + execTime + "' and jobId '" + jobId + "'.";
					tooltip = tooltip.replace("'", "&#39;").replace("\n", "&#10;");
					return "<a href='" + url + "' title='" + tooltip + "' target='_blank'>DbxTune</a>";
				}
			}

			//--------------------------------------------------------------
			// job_history_errors or job_history_error_all
			//--------------------------------------------------------------
			if ("job_history_errors".equals(rstm.getName()) || "job_history_errors_all".equals(rstm.getName()))
			{
				if ("timeline".equals(colName))
				{
					String jobName  = rstm.getValueAsString(row, "JobName"     , true, "unknown");
					String jobId    = rstm.getValueAsString(row, "job_id"      , true, "unknown");
					String stepId   = rstm.getValueAsString(row, "step_id"     , true, "unknown");
//					String execTime = rstm.getValueAsString(row, "execTime"    , true, "unknown");
					String execTime = "TODAY";

					String serverName = getReportingInstance().getServerName();
					String urlBase = ""
							+ "/api/cc/reports"
							+ "?srvName="    + serverName 
							+ "&reportName=" + "sqlserver-job-scheduler-timeline";

					return "" 
						+ "<a href='" + urlBase 
								+ "&jobName="              + jobName 
								+ "&jobId="                + jobId 
//								+ "&stepId="               + stepId // NOT YET IMPLEMENTED
								+ "&onlyLevelZero="        + true
								+ "&refresh="              + 0
								+ "&startTime="            + execTime 
//								+ "&endTime="              + lastExecTime 
								+ "&minDurationInSeconds=" + 0
								+ "' target='_blank'>Today</a>"
						;
				}

				if ("cmd".equals(colName))
				{
					// Get properties we want to pass
					String jobName = rstm.getValueAsString(row, "JobName");
					String step_id = rstm.getValueAsString(row, "step_id");
					
					return "<div title='Click to Open' "
							+ "data-toggle='modal' "
							+ "data-target='#dbx-view-sqltext-dialog' "
							+ "data-objectname='" + ("step_id=" + step_id +", jobName=" + jobName) + "' "
							+ "data-tooltip=\""   + getTooltipFor_jobStepCommand(jobName, step_id) + "\" "
							+ ">&#x1F4AC; Show</div>"; // symbol popup with "..."
				}

				if ("histGraph".equals(colName))
				{
					// Get properties we want to pass
					String jobName    = rstm.getValueAsString(row, "JobName");
					String job_id     = rstm.getValueAsString(row, "job_id");
					String step_id    = rstm.getValueAsString(row, "step_id");
					String execTime   = rstm.getValueAsString(row, "execTime");

					if (StringUtil.hasValue(execTime))
						execTime = execTime.substring(0, "YYYY-mm-dd HH:MM:SS".length());

					return "<div title='Click to Open' "
						+ "data-toggle='modal' "
						+ "data-target='#dbx-jobScheduler-timeline-dialog' "
						+ "data-objectname='" + ("step_id=" + step_id +", jobName=" + jobName + ", execTime=" + execTime) + "' "
						+ "data-starttime='"  + execTime + "' "
						+ "data-tooltip=\""   + getTooltipFor_allExecTimes(job_id, step_id) + "\" "
						+ ">&#x1F4AC; Show</div>" // symbol popup with "..."
						;
				}


				if ("execView".equals(colName))
				{
					// Get properties we want to pass
					String jobId      = rstm.getValueAsString(row, "job_id");
					String stepId     = rstm.getValueAsString(row, "step_id");
					String execTime   = rstm.getValueAsString(row, "execTime");

					if (StringUtil.hasValue(execTime))
						execTime = execTime.substring(0, "YYYY-mm-dd HH:MM:SS".length());

//					String startTime     = getJobStartTime(jobId);
//					String endTime       = getJobEndTime  (jobId);
					String startTime     = getStartTime(jobId, stepId, execTime, 15);
					String endTime       = getEndTime  (jobId, stepId, execTime, 15);
					String markStartTime = getStartTime(jobId, stepId, execTime, 0);
					String markEndTime   = getEndTime  (jobId, stepId, execTime, 0);

					String serverName = getReportingInstance().getServerName();
					String url = ""
							+ "/graph.html"
							+ "?subscribe="     + false 
							+ "&sessionName="   + serverName 
							+ "&startTime="     + startTime
							+ "&endTime="       + endTime
							+ "&markTime="      + execTime
							+ "&markStartTime=" + markStartTime
							+ "&markEndTime="   + markEndTime
							;

					// Change "'" and "\n" into html characters: "'"=>"&#39;" and "\n"=>"&#10;"
					String tooltip = "Open DbxTune/DbxCentral in historical mode and position you at the below 'markTime'.\n"
							+ "startTime ='" + startTime + "'\n"
							+ "endTime  ='"  + endTime   + "'\n"
							+ "markTime='"   + execTime  + "'\n"
							+ "\n"
							+ "Note: 15 minutes have been added to the start/end time from the origin stepId='" + stepId + "', execTime='" + execTime + "' and jobId '" + jobId + "'.";
					tooltip = tooltip.replace("'", "&#39;").replace("\n", "&#10;");
					return "<a href='" + url + "' title='" + tooltip + "' target='_blank'>DbxTune</a>";
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
			if ("JobName".equals(colName) || "job_name".equals(colName))
			{
				return getDescriptionForJobName(strVal);
			}

			return ResultSetTableModel.TableStringRenderer.super.cellToolTip(rstm, row, col, colName, objVal, strVal);
	
		} // end: cellToolTip(...)

	} // end: class


	private String getJobStartTime(String jobId, int subtractMinutes)
	{
		StartStopTime entry = _jobId__to__startStopTime.get(jobId);
		if (entry == null)
		{
			_logger.warn("getJobStartTime(): Lookup of jobId='" + jobId + "'. Nothing was found.");
			return "JOBID_NOT_FOUND[" + jobId + "]";
		}

		Timestamp ts = entry._startTime;
		if (ts == null)
			return "JOBID_TS_ISNULL[" + jobId + "]";

		// Subtract X minutes
		ts = new Timestamp( ts.getTime() - (subtractMinutes*60*1000) );
		
		return TimeUtils.toStringYmdHm(ts);
	}

	private String getJobEndTime(String jobId, int addMinutes)
	{
		StartStopTime entry = _jobId__to__startStopTime.get(jobId);
		if (entry == null)
		{
			_logger.warn("getJobStartTime(): Lookup of jobId='" + jobId + "'. Nothing was found.");
			return "JOBID_NOT_FOUND[" + jobId + "]";
		}

		Timestamp ts = entry._stopTime;
		if (ts == null)
			return "JOBID_TS_ISNULL[" + jobId + "]";

		// Add X minutes
		ts = new Timestamp( ts.getTime() + (addMinutes*60*1000) );
		
		return TimeUtils.toStringYmdHm(ts);
	}


	private static class StartStopTime
	{
		Timestamp _startTime;
		Timestamp _stopTime;

		public StartStopTime(Timestamp startTime, Timestamp stopTime)
		{
			_startTime = startTime;
			_stopTime  = stopTime;
		}
	}


	/**
	 * Get StartTime for a specific: jobId, stepId and execTime
	 * <p>
	 * If parameter 'addMinutes' those minutes will be decremented from the found 'ts='.<br>
	 * 
	 * The list entries look like:
	 * <pre>
	 * ts=2025-01-22 16:20:00, wd=Wednesday, HMS=00:01:16, sec=76, status=SUCCESS;
	 * ts=2025-01-22 16:40:00, wd=Wednesday, HMS=00:00:55, sec=55, status=SUCCESS;
	 * ts=2025-01-22 17:00:00, wd=Wednesday, HMS=00:01:10, sec=70, status=SUCCESS;
	 * ts=2025-01-22 17:20:00, wd=Wednesday, HMS=00:00:55, sec=55, status=SUCCESS;
	 * ts=2025-01-22 17:40:00, wd=Wednesday, HMS=00:00:58, sec=58, status=SUCCESS;
	 * ts=2025-01-22 18:00:00, wd=Wednesday, HMS=00:01:04, sec=64, status=SUCCESS;
	 * </pre>
	 * 
	 * @param jobId
	 * @param stepId
	 * @param execTime
	 * @param addMinutes   How many minutes <b>before</b> the found time would you like to add
	 * 
	 * @return Timestamp in format: YYYY-mm-dd HH:MM   ("" if not found, or "problem text" on exceptions)
	 */
	private String getStartTime(String jobId, String stepId, String execTimeStr, int addMinutes)
	{
		String key = jobId + "____" + stepId;
		List<String> list = _jobId_stepId__to__allExecTimes.get(key);
		
		if (list == null || (list != null && list.isEmpty()) )
			return "No Execution Times was found for: jobId=" + jobId + ", stepId=" + stepId;

		try
		{
			Timestamp execTs   = TimeUtils.parseToTimestampX(execTimeStr);
			Timestamp foundTs  = null;
//			int       foundSec = 0;
			
			for (String entry : list)
			{
				String tsStr  = StringUtils.substringBetween(entry, "ts=",  ",");
//				String secStr = StringUtils.substringBetween(entry, "sec=", ",");

				Timestamp ts  = TimeUtils.parseToTimestampX(tsStr);
//				int       sec = StringUtil.parseInt(secStr, 0);
				
				// If ENTRY is >= the passed 'execTime'
				if (ts.getTime() >= execTs.getTime())
				{
					foundTs = ts;
//					foundSec = sec;
					break; // GET OUT OF THE LOOP
				}
			}
			
			if (foundTs != null)
			{
				// Subtract X minutes
				Timestamp retTs = new Timestamp( foundTs.getTime() - (addMinutes*60*1000) );
				return TimeUtils.toStringYmdHm(retTs);
			}
			
			return "";
		}
		catch (ParseException ex)
		{
			return "Problems parsing time. Caught: " + ex.getMessage();
		}
	}

	/**
	 * Get EndTime for a specific: jobId, stepId and execTime
	 * <p>
	 * Number of seconds will be added to the 'ts='<br>
	 * Also parameter 'addMinutes' the end.<br>
	 * 
	 * The list entries look like:
	 * <pre>
	 * ts=2025-01-22 16:20:00, wd=Wednesday, HMS=00:01:16, sec=76, status=SUCCESS;
	 * ts=2025-01-22 16:40:00, wd=Wednesday, HMS=00:00:55, sec=55, status=SUCCESS;
	 * ts=2025-01-22 17:00:00, wd=Wednesday, HMS=00:01:10, sec=70, status=SUCCESS;
	 * ts=2025-01-22 17:20:00, wd=Wednesday, HMS=00:00:55, sec=55, status=SUCCESS;
	 * ts=2025-01-22 17:40:00, wd=Wednesday, HMS=00:00:58, sec=58, status=SUCCESS;
	 * ts=2025-01-22 18:00:00, wd=Wednesday, HMS=00:01:04, sec=64, status=SUCCESS;
	 * </pre>
	 * 
	 * @param jobId
	 * @param stepId
	 * @param execTime
	 * @param addMinutes   How many minutes <b>after</b> the found time would you like to add
	 * 
	 * @return Timestamp in format: YYYY-mm-dd HH:MM   ("" if not found, or "problem text" on exceptions)
	 */
	private String getEndTime(String jobId, String stepId, String execTimeStr, int addMinutes)
	{
		String key = jobId + "____" + stepId;
		List<String> list = _jobId_stepId__to__allExecTimes.get(key);
		
		if (list == null || (list != null && list.isEmpty()) )
			return "No Execution Times was found for: jobId=" + jobId + ", stepId=" + stepId;

		try
		{
			Timestamp execTs   = TimeUtils.parseToTimestampX(execTimeStr);
			Timestamp foundTs  = null;
			int       foundSec = 0;
			
			for (String entry : list)
			{
				String tsStr  = StringUtils.substringBetween(entry, "ts=",  ",");
				String secStr = StringUtils.substringBetween(entry, "sec=", ",");

				Timestamp ts  = TimeUtils.parseToTimestampX(tsStr);
				int       sec = StringUtil.parseInt(secStr, 0);

				// If ENTRY is >= the passed 'execTime'
				if (ts.getTime() >= execTs.getTime())
				{
					foundTs = ts;
					foundSec = sec;
					break; // GET OUT OF THE LOOP
				}
			}
			
			if (foundTs != null)
			{
				// Increment X seconds to the end time... How many seconds it took to run this step
				// Increment X minutes to the end time... Some extra time, so it will be easier to view in the timeline
				Timestamp retTs = new Timestamp( foundTs.getTime() + (foundSec*1000) + (addMinutes*60*1000) );
				return TimeUtils.toStringYmdHm(retTs);
			}
			
			return "";
		}
		catch (ParseException ex)
		{
			return "Problems parsing time. Caught: " + ex.getMessage();
		}
	}





	public enum SourceType
	{
		ONLINE, PCS
	};
	
	public static Map<String, String> createStaticJavaScript_lookupContent__jobId__to__name(Writer w, DbxConnection conn, SourceType sourceType)
//	public static void create_lookup_jobId_name(PrintWriter writer, DbxConnection conn, SourceType sourceType)
//	throws SQLException
	{
		PrintWriter  writer = (w instanceof PrintWriter) ? (PrintWriter)w : new PrintWriter(w);
		Map<String, String> retMap = new LinkedHashMap<>();

		String sql = "";
		if (SourceType.ONLINE.equals(sourceType))
		{
			sql = ""
			    + "SELECT \n"
			    + "     job_id \n"
			    + "    ,name \n"
			    + "FROM msdb.dbo.sysjobs \n"
			    + "ORDER BY 1 \n"
			    ;
		}
		else if (SourceType.PCS.equals(sourceType))
		{
			sql = ""
				+ "SELECT \n"
				+ "     [job_id] \n"
				+ "    ,[name] \n"
				+ "FROM [" + SqlServerJobSchedulerExtractor.PCS_SCHEMA_NAME + "].[" + SqlAgentInfo.sysjobs + "] \n"
			    + "ORDER BY 1 \n"
				;

			sql = conn.quotifySqlString(sql);
		}
		else
		{
			throw new RuntimeException("Unsupported sourceType='" + sourceType + "'.");
		}

		// Execute the SQL - with AutoClose
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql); )
		{
			// Write 'BEGIN'
			writer.println();
			writer.println("<!-- BEGIN: JAVA SCRIPT for 'lookup': for 'job_id' => 'job_name' --> ");
			writer.println("<script> ");
			writer.println("    // Create a Java Object that will hold data ");
			writer.println("    var _globalLookup__jobId__to__name = {} ");
			writer.println();
			while(rs.next())
			{
				String job_id   = rs.getString(1);
				String job_name = rs.getString(2);

				String key = job_id;
				String val = job_name;

				writer.println("    _globalLookup__jobId__to__name['" + key + "'] = `" + val + "`;");

				// Add to Return Map
				retMap.put(key, val);
			}

			// and some functions
			writer.println();
			writer.println("    // LOOKUP Function ");
			writer.println("    function lookup__jobId__to__name(job_id) ");
			writer.println("    { ");
			writer.println("        let name = _globalLookup__jobId__to__name[job_id]; ");
			writer.println("        if (name === undefined) ");
			writer.println("        { ");
			writer.println("            return job_id; ");
			writer.println("        } ");
			writer.println("        return name; ");
			writer.println("    } ");

			// Write 'END'
			writer.println("</script> ");
			writer.println("<!-- END: JAVA SCRIPT for 'lookup' for: 'job_id' => 'job_name' --> ");
			writer.println();
		}
		catch (SQLException ex)
		{
			String msg = "In '" + MethodHandles.lookup().lookupClass().getSimpleName() + "'. Problems executing SQL Statement. ErrorCode=" + ex.getErrorCode() + ", SQLState=" + ex.getSQLState() + ", Message=|" + ex.getMessage() + "|, SQL=|" + sql + "|.";
			_logger.error(msg);

			writer.println();
			writer.println("<!-- BEGIN: ERROR MESSAGE in Servlet  ");
			writer.println(msg);
			writer.println("  -- END: ERROR MESSAGE in Servlet --> ");
			writer.println();
		}

		return retMap;
	}


	public static void createStaticJavaScript_lookupContent__jobId_stepId__to__name(Writer w, DbxConnection conn, SourceType sourceType)
	throws SQLException
	{
		PrintWriter writer = (w instanceof PrintWriter) ? (PrintWriter)w : new PrintWriter(w);

		// NOT IMPLEMENTED
		String sql = ""
			    + "SELECT \n"
			    + "     job_id \n"
			    + "    ,step_id \n"
			    + "    ,step_name \n"
			    + "FROM msdb.dbo.sysjobsteps \n"
			    + "ORDER BY job_id, step_id \n"
			    + "";

		throw new RuntimeCryptoException("-NOT-YET-IMPLEMENTED-");
	}


//	/**
//	 * Produce: 
//	 * @param conn     DBMS Connection
//	 * @param sql      SQL Statement to execute
//	 * @param writer   Where to write content
//	 */
//	public static void create_allExecTimes_jobId_stepId(DbxConnection conn, String sql, PrintWriter writer)
//	throws SQLException
//	{
//		// Execute the SQL - with AutoClose
//		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql); )
//		{
//			// Write 'BEGIN'
//			writer.println();
//			writer.println("<!-- BEGIN: JAVA SCRIPT for 'allExecTime' for 'job_id' and 'step_id' --> ");
//			writer.println("<script> ");
//			writer.println("    // Create a Java Object that will hold ALL job instances ");
//			writer.println("    var _global_JobId_StepId_Instance = { ");
//			writer.println("    } ");
//			writer.println();
//
//			while(rs.next())
//			{
//				String job_id       = rs.getString(1);
//				String step_id      = rs.getString(2);
//				String allExecTimes = rs.getString(3);
//
//				String key = job_id + "____" + step_id;
//				
//				writer.println();
//				writer.println("    // -------- jobid='" + job_id + "', step_id='" + step_id + "', NOTE: The below uses backtick (`) so we can have newlines... ");
//				writer.println("    _global_JobId_StepId_Instance['" + key + "'] = `" + allExecTimes + "`;");
//			}
//
//			// Write 'END'
//			writer.println("</script> ");
//			writer.println("<!-- END: JAVA SCRIPT for 'allExecTime' for 'job_id' and 'step_id' --> ");
//			writer.println();
//		}
//	}

	public static class ReturnObject_allExecTimes
	{
		Map<String, List<String>> _timeMap;
		Map<String, StatObject>   _statMap;

		public ReturnObject_allExecTimes(Map<String, List<String>> timeMap, Map<String, StatObject> statMap)
		{
			_timeMap = timeMap;
			_statMap = statMap;
		}
		public Map<String, List<String>> getTimeMap() { return _timeMap; }
		public Map<String, StatObject>   getStatMap() { return _statMap; }
	}

	public static class StatObject
	{
		long _count = 0;
		long _min   = Long.MAX_VALUE;
		long _max   = Long.MIN_VALUE;
		long _avg   = 0;
		long _sum   = 0;
		
		public void add(long value)
		{
			_count++;
			_min = Math.min(_min, value);
			_max = Math.max(_max, value);
			_sum += value;
			_avg = _sum / _count;
		}

		public long getCount() { return _count; }
		public long getMin()   { return _min;   }
		public long getMax()   { return _max;   }
		public long getAvg()   { return _avg;   }
		public long getSum()   { return _sum;   }
		
		public String toJson()
		{
			return ("{#count#:" + _count + ", #min#:" + _min + ", #max#:" + _max + ", #sum#:" + _sum + ", #avg#:" + _avg + " }").replace('#', '"');
		}		
	}

//	public static Map<String, List<String>> createStaticJavaScript_lookupContent__jobId_stepId__to__allExecTimes(Writer w, DbxConnection conn, SourceType sourceType)
	public static ReturnObject_allExecTimes createStaticJavaScript_lookupContent__jobId_stepId__to__allExecTimes(Writer w, DbxConnection conn, SourceType sourceType)
	{
		PrintWriter  writer = (w instanceof PrintWriter) ? (PrintWriter)w : new PrintWriter(w);
		Map<String, List<String>> retMap = new LinkedHashMap<>();
		Map<String, StatObject>   sumMap = new LinkedHashMap<>();
		
		String sql = "";
		if (SourceType.ONLINE.equals(sourceType))
		{
			sql = ""
				+ "SELECT \n"
				+ "     job_id = cast(jh.job_id as varchar(40)) \n"
				+ "    ,jh.step_id \n"
				+ "    ,run_ts = convert(datetime, convert(varchar(8), jh.run_date)) \n"
				+ "                              + ' ' \n"
				+ "                              + stuff(stuff(right(1000000 + jh.run_time \n"
				+ "                                                 ,6) \n"
				+ "                                            ,3,0,':') \n"
				+ "                                      ,6,0,':') \n"
				+ "    ,run_duration_sec = jh.run_duration / 10000 * 3600 \n"
				+ "                      + jh.run_duration % 10000 / 100 * 60 \n"
				+ "                      + jh.run_duration % 100 \n"
				+ "    ,run_status_desc = \n"
				+ "        CASE \n"
				+ "            WHEN jh.run_status = 0 THEN 'FAILED' \n"
				+ "            WHEN jh.run_status = 1 THEN 'SUCCESS' \n"
				+ "            WHEN jh.run_status = 2 THEN 'RETRY' \n"
				+ "            WHEN jh.run_status = 3 THEN 'CANCELED' \n"
				+ "            WHEN jh.run_status = 4 THEN 'IN PROGRESS' \n"
				+ "            ELSE '-UNKNOWN-' + cast(jh.run_status as varchar(10)) + '-' \n"
				+ "        END \n"
			    + "FROM msdb.dbo.sysjobhistory jh \n"
			    + "ORDER BY 1, 2, 3 \n"
			    ;
		}
		else if (SourceType.PCS.equals(sourceType))
		{
			sql = ""
				+ "SELECT \n"
				+ "     [job_id] \n"
				+ "    ,[step_id] \n"
				+ "    ,[run_ts] \n"
				+ "    ,[run_duration_sec] \n"
				+ "    ,[run_status_desc] \n"
				+ "FROM [" + SqlServerJobSchedulerExtractor.PCS_SCHEMA_NAME + "].[" + SqlAgentInfo.sysjobhistory + "] \n"
			    + "ORDER BY 1, 2, 3 \n"
				;

			sql = conn.quotifySqlString(sql);
		}
		else
		{
			throw new RuntimeException("Unsupported sourceType='" + sourceType + "'.");
		}
		
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			// Write 'BEGIN'
			writer.println();
			writer.println("<!-- BEGIN: JAVA SCRIPT for 'lookup': for 'job_id:step_id' => 'allExecTimes' --> ");
			writer.println("<script> ");
			writer.println("    // Create a Java Object that will hold data ");
			writer.println("    var _globalLookup__jobId_stepId__to__allExecTimes = {} ");
			writer.println();
			
			String prevKey = "";
//			String val     = "";

			while(rs.next())
			{
				String    job_id           = rs.getString   (1);
				int       step_id          = rs.getInt      (2);
				Timestamp run_ts           = rs.getTimestamp(3);
				int       run_duration_sec = rs.getInt      (4);
				String    run_status_desc  = rs.getString   (5);

				String key = job_id + "____" + step_id;

				String val = ""
						+ "ts="       + TimeUtils.toStringYmdHms(run_ts)
						+ ", wd="     + StringUtil.left(TimeUtils.toWeekDay(run_ts), 9) // make it 9 chars
						+ ", HMS="    + TimeUtils.secToTimeStrHMS(run_duration_sec)
						+ ", sec="    + run_duration_sec
						+ ", status=" + run_status_desc
						+ ";"
						;

				if ( ! prevKey.equals(key))
				{
					writer.println();
					writer.println("    // -------------- ");
					writer.println("    _globalLookup__jobId_stepId__to__allExecTimes['" + key + "'] = [];");
				}
				writer.println("    _globalLookup__jobId_stepId__to__allExecTimes['" + key + "'].push(`" + val + "`);");
				
				// Add to sumMap
				StatObject sumEntry = sumMap.get(key);
				if (sumEntry == null)
				{
					sumEntry = new StatObject();
					sumMap.put(key, sumEntry);
				}
				sumEntry.add(run_duration_sec);
				
				// Add to Return Map
				List<String> list = retMap.get(key);
				if (list == null)
				{
					list = new ArrayList<>();
					retMap.put(key, list);
				}
				list.add(val);

//				writer.println("    _globalLookup__jobId_stepId__to__allExecTimes['" + key + "'] = `" + val + "`;");
				prevKey = key;
			}
			
			// NOTE: The above will probably produce a lot of "chars"...
			//       So another solution would be to append all "rows" to "holderVar" and only write that when 'key' is changing
			//       ANd don't forget to write "spills" in the "holderVar" after enf-of-loop to print out LAST row...

			// and some functions
			writer.println();
			writer.println("    // LOOKUP Function ");
			writer.println("    function lookup__jobId_stepId__to__allExecTimes(jobId, stepId) ");
			writer.println("    { ");
			writer.println("        let key = jobId + '____' + stepId; ");
			writer.println("        let val = _globalLookup__jobId_stepId__to__allExecTimes[key]; ");
			writer.println("        if (name === undefined) ");
			writer.println("        { ");
			writer.println("            return jobId + '|' + stepId; ");
			writer.println("        } ");
			writer.println("       if (Array.isArray(val)) ");
			writer.println("            return val.join('\\n'); ");
			writer.println("        return val; ");
			writer.println("    } ");

			// Write 'END'
			writer.println("</script> ");
			writer.println("<!-- END: JAVA SCRIPT for 'lookup': for 'job_id:step_id' => 'allExecTimes' --> ");
			writer.println();

		
		
		
			//----------------------------------------------------------------------------
			// allExecSummary
			//----------------------------------------------------------------------------

			// Write 'BEGIN'
			writer.println();
			writer.println("<!-- BEGIN: JAVA SCRIPT for 'lookup': for 'job_id:step_id' => 'allExecSummary' --> ");
			writer.println("<script> ");
			writer.println("    // Create a Java Object that will hold data ");
			writer.println("    var _globalLookup__jobId_stepId__to__allExecSummary = {} ");
			writer.println();
		
			for (Entry<String, StatObject> entry : sumMap.entrySet())
			{
				String key = entry.getKey();
				String val = entry.getValue().toJson();
				
				writer.println("    _globalLookup__jobId_stepId__to__allExecSummary['" + key + "'] = " + val + ";");
			}

			// and some functions
			writer.println();
			writer.println("    // LOOKUP Function ");
			writer.println("    function lookup__jobId_stepId__to__allExecSummary(jobId, stepId) ");
			writer.println("    { ");
			writer.println("        let key = jobId + '____' + stepId; ");
			writer.println("        let val = _globalLookup__jobId_stepId__to__allExecSummary[key]; ");
			writer.println("        if (name === undefined) ");
			writer.println("        { ");
			writer.println("            return jobId + '|' + stepId; ");
			writer.println("        } ");
			writer.println("       if (Array.isArray(val)) ");
			writer.println("            return val.join('\\n'); ");
			writer.println("        return val; ");
			writer.println("    } ");

			// Write 'END'
			writer.println("</script> ");
			writer.println("<!-- END: JAVA SCRIPT for 'lookup': for 'job_id:step_id' => 'allExecSummary' --> ");
			writer.println();
		}
		catch (SQLException ex)
		{
			String msg = "In '" + MethodHandles.lookup().lookupClass().getSimpleName() + "'. Problems executing SQL Statement. ErrorCode=" + ex.getErrorCode() + ", SQLState=" + ex.getSQLState() + ", Message=|" + ex.getMessage() + "|, SQL=|" + sql + "|.";
			_logger.error(msg);

			writer.println();
			writer.println("<!-- BEGIN: ERROR MESSAGE in Servlet  ");
			writer.println(msg);
			writer.println("  -- END: ERROR MESSAGE in Servlet --> ");
			writer.println();
		}

		return new ReturnObject_allExecTimes(retMap, sumMap); 
	}


	/**
	 * Create HTML and JavaScript code for the modal <code>dbx-jobScheduler-timeline-dialog</code>
	 * @param w              To what writer
	 * @param serverName     Name of the Server (The id/Display name in DbxCentral)... This is used if we want to click on a "time" in the chart and open a "chart.html" call to dig further...
	 */
	public static void createStaticHtmlAndJavaScriptContent(Writer w, String serverName)
	{
		PrintWriter  writer = (w instanceof PrintWriter) ? (PrintWriter)w : new PrintWriter(w);

		writer.println();
		writer.println("    <!-- Modal: Show SQL Server Job/Scheduler, with a graph and text of the executions  --> ");
		writer.println("    <div class='modal fade' id='dbx-jobScheduler-timeline-dialog' tabindex='-1' role='dialog' aria-labelledby='dbx-jobScheduler-timeline-label' aria-hidden='true'> ");
		writer.println("        <div class='modal-dialog modal-xl' style='max-width: 80%;' role='document'> ");
		writer.println("            <div class='modal-content'> ");
		writer.println("                <div class='modal-header'> ");
		writer.println("                    <h5 class='modal-title' id='dbx-jobScheduler-objectName'>Job Scheduler Timeline Chart</h5> ");
		writer.println("                    <button type='button' class='close' data-dismiss='modal' aria-label='Close'> ");
		writer.println("                        <span aria-hidden='true'>&times;</span> ");
		writer.println("                    </button> ");
		writer.println("                </div> ");
		writer.println("                <div class='modal-body' style='overflow-x: auto;'> ");
		writer.println("                    <canvas id='dbx-jobScheduler-timeline-chart' style='width: 77vw; display: none;'></canvas> ");
		writer.println("                    <div class='scroll-tree' style='width: 100%;'> ");
		writer.println("                        <pre><code id='dbx-jobScheduler-text-content' class='language-sql line-numbers dbx-view-sqltext-content' ></code></pre> ");
		writer.println("                    </div> ");
		writer.println("                </div> ");
		writer.println("                <div class='modal-footer'> ");
		writer.println("                    <button type='button' class='btn btn-secondary' data-dismiss='modal'>Close</button> ");
		writer.println("                </div> ");
		writer.println("            </div> ");
		writer.println("        </div> ");
		writer.println("    </div> ");
		writer.println();
		writer.println("    <script> ");
		writer.println();
		writer.println("        // When initializing the structure, 'serverName' is passed, which we set here as a global variable");
		writer.println("        var _global_jobSchedulerTimeline_serverName = '" + serverName + "'; ");
		writer.println();
		writer.println("        // On modal open (AFTER fully open) ");
		writer.println("        $('#dbx-jobScheduler-timeline-dialog').on('shown.bs.modal', function(e) ");
		writer.println("        { ");
		writer.println("            // highlight again, since the dialog DOM wasn't visible earlier ");
		writer.println("            Prism.highlightAll(); ");
//		writer.println();
//		writer.println("            // Scroll top top (do not seems to work) ");
//		writer.println("            $('#dbx-jobScheduler-timeline-dialog').animate({ scrollTop: 0 }, 'slow'); ");
		writer.println("        }); ");
		writer.println();
		writer.println("        // On modal open (BEFORE fully open) ");
		writer.println("        $('#dbx-jobScheduler-timeline-dialog').on('show.bs.modal', function(e) ");
		writer.println("        { ");
		writer.println("            // BEGIN: If there are open modals, we need to increment z-index... to make this modal 'on-top' ");
		writer.println("            const openModals = $('.modal.show'); ");
		writer.println("            if (openModals.length > 0) ");
		writer.println("            { ");
		writer.println("                console.log('#dbx-jobScheduler-timeline-dialog[show.bs.modal]: OTHER MODALS are OPEN: openModals.length=' + openModals.length); ");
		writer.println("                var highestZIndex = 0;");
		writer.println();
		writer.println("                // Loop through all open modals and find the highest z-index ");
		writer.println("                openModals.each(function() ");
		writer.println("                { ");
		writer.println("                    let currentZIndex = parseInt($(this).css('z-index'), 10); ");
		writer.println("                    if (currentZIndex > highestZIndex) ");
		writer.println("                    { ");
		writer.println("                        highestZIndex = currentZIndex; ");
		writer.println("                    } ");
		writer.println("                }); ");
		writer.println("                console.log('#dbx-jobScheduler-timeline-dialog[show.bs.modal]: OTHER MODALS are OPEN: newZIndex=' + newZIndex); ");
		writer.println();
		writer.println("                // Increment the z-index for the second modal, to ensure it's on top ");
		writer.println("                var newZIndex = highestZIndex + 10; ");
		writer.println("                $(this).css('z-index', newZIndex); ");
		writer.println("            } ");
		writer.println("            // END: check for other open modals ");
		writer.println();
		writer.println("            // Get 'data' attributes from events callers element/button ");
		writer.println("            let data = $(e.relatedTarget).data(); ");
		writer.println("            console.log('#dbx-jobScheduler-timeline-dialog[show.bs.modal]: data: ' + data, data); ");
		writer.println();
		writer.println("            // Read input from the 'data-' attributes... ");
		writer.println("            const jobId             = data.jobid; ");
		writer.println("            const stepId            = data.stepid; ");
		writer.println();
		writer.println("            let   inputStr          = data.tooltip; ");
		writer.println("            let   jobName           = data.objectname; ");
		writer.println("            let   jobStartTsMarker  = data.starttime; ");
		writer.println();
		writer.println("            // If 'jobid' and 'stepid' was passed, override some values");
		writer.println("            if (jobId !== undefined && stepId !== undefined) ");
		writer.println("            { ");
		writer.println("                console.log('#dbx-jobScheduler-timeline-dialog[show.bs.modal]: jobid=|' + jobId + '|, stepid=|' + stepId + '|. was passed... overriding some values') ");
		writer.println();
		writer.println("                inputStr = lookup__jobId_stepId__to__allExecTimes(jobId, stepId); ");
		writer.println("                jobName  = lookup__jobId__to__name(jobId); ");
		writer.println();
		writer.println("                jobName  = 'stepId=' + stepId + ', jobName=' + jobName; ");
		writer.println();
		writer.println("                if (jobStartTsMarker !== undefined) ");
		writer.println("                    jobName = jobName + ', startTime=' + jobStartTsMarker; ");
		writer.println("            } ");
		writer.println();
		writer.println("            $('#dbx-jobScheduler-objectName').text(jobName); ");
//		writer.println("            $('#dbx-jobScheduler-xxx-content',    this).text(data.tooltip); ");
		writer.println();
		writer.println("            console.log('#dbx-jobScheduler-timeline-dialog[show.bs.modal]: jobName=|' + jobName + '|, jobStartTsMarker=|' + jobStartTsMarker + '.', inputStr); ");
		writer.println();
		writer.println("            // Create the chart... ");
		writer.println("            createTimeLineChart(inputStr, jobName, jobStartTsMarker);");
		writer.println("        }); ");
		writer.println();
		writer.println("        // Open Chart Dialog by: jobId, stepId ");
		writer.println("        function openTimeLineChartDialog_byIds(jobId, stepId, startTsMarker)  ");
		writer.println("        { ");
		writer.println("            // How this is done to handel both 'data-target' and calling this function... ");
		writer.println("            // Simulate that it was called from an 'element'... Create an element, which we programatically 'click' on! ");
		writer.println("            var tempElement = $('<button>', { ");
		writer.println("                 type: 'button', ");
		writer.println("                     'data-toggle'    : 'modal', ");
		writer.println("                     'data-target'    : '#dbx-jobScheduler-timeline-dialog', ");
		writer.println("                     'data-jobid'     : jobId, ");
		writer.println("                     'data-stepid'    : stepId, ");
		writer.println("                     'data-starttime' : startTsMarker ");
		writer.println("            }); ");
		writer.println();
		writer.println("            // Append the element to the body (temporarily) ");
		writer.println("            $('body').append(tempElement); ");
		writer.println();
		writer.println("            // Trigger the click to open the modal decided above by: 'data-target': '#dbx-jobScheduler-timeline-dialog' ");
		writer.println("            tempElement.trigger('click'); ");
		writer.println();
		writer.println("            // Remove the element ");
		writer.println("            tempElement.remove(); ");
		writer.println("        } ");
		writer.println();
		writer.println("        // If you want to open the dialog using a function call, then use this function ");
		writer.println("        // Or do it by: <button ... data-toggle='modal' data-target='#dbx-jobScheduler-timeline-dialog' data-objectname='name' data-tooltip='ts input...' data-starttime='YYYY-mm-dd HH:MM:SS'> ");
		writer.println("        function openTimeLineChartDialog_byTsStr(input, name, startTsMarker)  ");
		writer.println("        { ");
		writer.println("            // How this is done to handel both 'data-target' and calling this function... ");
		writer.println("            // Simulate that it was called from an 'element'... Create an element, which we programatically 'click' on! ");
		writer.println("            var tempElement = $('<button>', { ");
		writer.println("                 type: 'button', ");
		writer.println("                     'data-toggle'     : 'modal', ");
		writer.println("                     'data-target'     : '#dbx-jobScheduler-timeline-dialog', ");
		writer.println("                     'data-tooltip'    : input, ");
		writer.println("                     'data-objectname' : name, ");
		writer.println("                     'data-starttime'  : startTsMarker ");
		writer.println("            }); ");
		writer.println();
		writer.println("            // Append the element to the body (temporarily) ");
		writer.println("            $('body').append(tempElement); ");
		writer.println();
		writer.println("            // Trigger the click to open the modal decided above by: 'data-target': '#dbx-jobScheduler-timeline-dialog' ");
		writer.println("            tempElement.trigger('click'); ");
		writer.println();
		writer.println("            // Remove the element ");
		writer.println("            tempElement.remove(); ");
		writer.println("        } ");
		writer.println();
		writer.println("        // Global object to hold any created chart instances ");
		writer.println("        let _jobSchedulerTimelineChartInstance = null; ");
		writer.println();
		writer.println("        function parseTsDataInput(input) ");
		writer.println("        { ");
		writer.println("            const lines      = input.split(';'); ");
		writer.println("            const timestamps = []; ");
		writer.println("            const values     = []; ");
		writer.println("            const status     = []; ");
		writer.println();
		writer.println("            lines.forEach(line => { ");
		writer.println("                const tsMatch     = line.match(/ts=([^,]+)/); ");
		writer.println("                const secMatch    = line.match(/sec=([^,]+)/); ");
		writer.println("                const statusMatch = line.match(/status=([^,]+)/); ");
		writer.println();
		writer.println("                if (tsMatch && secMatch)  ");
		writer.println("                { ");
		writer.println("                    timestamps.push(tsMatch[1]); ");
		writer.println("                    values    .push(parseInt(secMatch[1], 10)); ");
		writer.println("                    status    .push(statusMatch ? statusMatch[1] : 'UNKNOWN'); ");
		writer.println("                } ");
		writer.println("            }); ");
		writer.println();
		writer.println("            return { timestamps, values, status }; ");
		writer.println("        } ");
		writer.println();
		writer.println("        function formatSecondsToHMS(seconds)  ");
		writer.println("        { ");
		writer.println("            const h = Math.floor( seconds / 3600); ");
		writer.println("            const m = Math.floor((seconds % 3600) / 60); ");
		writer.println("            const s = Math.floor( seconds % 60); ");
		writer.println("            return `${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`; ");
		writer.println("        } ");
		writer.println();
		writer.println("        function createTimeLineChart(input, jobName, jobStartTsMarker)  ");
		writer.println("        { ");
		writer.println("            // Parse the data ");
		writer.println("            const parsedTsData = parseTsDataInput(input); ");
		writer.println();
		writer.println("            // Calculate average ");
//		writer.println("            const averageExecutionTime = data.datasets[0].data.reduce((a, b) => a + b, 0) / data.datasets[0].data.length; ");
		writer.println("            const avgExecTimeSec = parsedTsData.values.reduce((a, b) => a + b, 0) / parsedTsData.values.length; ");
		writer.println("            const avgExecTimeHms = formatSecondsToHMS(avgExecTimeSec); ");
		writer.println("            console.log('createTimeLineChart(): avgExecTimeSec=' + avgExecTimeSec + ', avgExecTimeHms=' + avgExecTimeHms); ");
		writer.println();
		writer.println("            // Chart options (created later) ");
		writer.println("            const chartOptions =  ");
		writer.println("            { ");
		writer.println("                type: 'line', ");
		writer.println("                data: { ");
		writer.println("                    labels: parsedTsData.timestamps, ");
		writer.println("                    datasets: [{ ");
		writer.println("                        label: 'Execution Time', ");
		writer.println("                        data: parsedTsData.values, ");
		writer.println("                        borderColor: 'rgba(75, 192, 192, 1)', // '#007bff', ");
		writer.println("                        tension: 0.4, ");
		writer.println("                        borderWidth: 2, ");
		writer.println("                        pointRadius: 4, ");
		writer.println("                        fill: false, ");
//		writer.println("                        pointBackgroundColor: parsedTsData.status.map(s => s === 'SUCCESS' ? 'green' : 'red') ");
		writer.println("                        pointBackgroundColor: parsedTsData.status.map(s => { ");
		writer.println("                            switch(s) { ");
		writer.println("                                case 'SUCCESS': return 'green';  ");
		writer.println("                                case 'WARNING': return 'yellow'; ");
		writer.println("                                case 'FAILED':  return 'red';    ");
		writer.println("                                case 'ERROR':   return 'red';    ");
		writer.println("                                default:        return 'gray';   ");
		writer.println("                            } ");
		writer.println("                        }) ");
		writer.println("                    }] ");
		writer.println("                }, ");
		writer.println("                options: { ");
		writer.println("                    responsive: true, ");
		writer.println("                    scales: { ");
		writer.println("                        y: { ");
		writer.println("                            ticks: { ");
		writer.println("                                callback: function(value) { ");
		writer.println("                                    return formatSecondsToHMS(value); ");
		writer.println("                                } ");
		writer.println("                            }, ");
		writer.println("                            title: { display: true, text: 'Duration (HH:MM:SS)' } ");
		writer.println("                        } ");
		writer.println("                    }, ");
		writer.println("                    plugins: { ");
		writer.println("                        title: { ");
		writer.println("                            display: true, ");
		writer.println("                            text: jobName ");
		writer.println("                        }, ");
		writer.println("                        legend: { ");
		writer.println("                            display: false ");
		writer.println("                        }, ");
		writer.println("                        tooltip: { ");
		writer.println("                            callbacks: { ");
		writer.println("                                title: function(context) { ");
		writer.println("                                    const isoDate = context[0].label; ");
		writer.println("                                    const date = new Date(isoDate); ");
		writer.println("                                    const days = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday']; ");
		writer.println("                                    const dayOfWeek = days[date.getDay()]; ");
//		writer.println("                                    return `${isoDate} (${dayOfWeek})` ");
		writer.println("                                    return isoDate + ' (' + dayOfWeek + ')'; ");
		writer.println("                                }, ");
		writer.println("                                label: function(tooltipItem) { ");
		writer.println("                                    return formatSecondsToHMS(tooltipItem.raw); ");
		writer.println("                                }, ");
		writer.println("                                afterBody: function(context) { ");
		writer.println("                                    return '\\nClick the Chart Point will:\\n - Open DbxTune at that specific time (adding 15m at start/end).'; ");
		writer.println("                                } ");
		writer.println("                            } ");
		writer.println("                        }, ");
//		writer.println("                        annotation: {} ");
		writer.println("                        annotation: { ");
		writer.println("                            annotations: { ");
		writer.println("                                averageLine: { ");
		writer.println("                                    type: 'line', ");
		writer.println("                                    yMin: avgExecTimeSec, ");
		writer.println("                                    yMax: avgExecTimeSec, ");
		writer.println("                                    borderColor: 'gray',");
		writer.println("                                    borderWidth: 2, ");
		writer.println("                                    borderDash: [5, 5], ");
		writer.println("                                    label: { ");
//		writer.println("                                        content: `Avg: ${averageExecutionTime.toFixed(2)}`, ");
		writer.println("                                        content: `Avg: ${avgExecTimeHms}`, ");
		writer.println("                                        display: true, ");
		writer.println("                                        backgroundColor: 'gray' ");
		writer.println("                                    } ");
		writer.println("                                } ");
		writer.println("                            } ");
		writer.println("                        } ");
		writer.println("                    }, ");
		writer.println("                    onClick: (event, elements, chart) => { ");
		writer.println("                        if (elements[0]) ");
		writer.println("                        {");
		writer.println("                            const i = elements[0].index; ");
		writer.println();
		writer.println("                            const ts      = moment(chart.data.labels[i]); ");
		writer.println("                            const seconds = chart.data.datasets[0].data[i]; ");
//		writer.println("console.log('onClick: ts=|' + ts + '|, seconds=|' + seconds + '|.', ts, seconds); ");
//		writer.println("alert('onClick: ts=|' + ts + '|, seconds=|' + seconds + '|.'); ");
		writer.println();
		writer.println("                            const srvName       = _global_jobSchedulerTimeline_serverName; ");
		writer.println("                            const tsFormat      = 'YYYY-MM-DD HH:mm:ss'; ");
		writer.println("                            const startTime     = ts.clone().subtract(15, 'minutes').format(tsFormat); ");
		writer.println("                            const endTime       = ts.clone().add(seconds, 'seconds').add(15, 'minutes').format(tsFormat); ");
		writer.println("                            const markTime      = ts.clone().format(tsFormat); ");
		writer.println("                            const markStartTime = ts.clone().format(tsFormat); ");
		writer.println("                            const markEndTime   = ts.clone().add(seconds, 'seconds').format(tsFormat); ");
		writer.println();
		writer.println("                            // Open URL in new tab ");
		writer.println("                            const url = '/graph.html?subscribe=false&sessionName=' + srvName + '&startTime=' + startTime + '&endTime=' + endTime + '&markTime=' + markTime + '&markStartTime=' + markStartTime + '&markEndTime=' + markEndTime; ");
		writer.println("                            window.open(url, '_blank').focus(); ");
		writer.println("                            ");
		writer.println("                        }");
		writer.println("                    } ");
		writer.println("                } ");
		writer.println("            }; ");
		writer.println();
		writer.println("            // Add a marker for 'current' time ");
		writer.println("            if (jobStartTsMarker !== undefined) ");
		writer.println("            { ");
		writer.println("                const highlightLineObj =  ");
		writer.println("                { ");
		writer.println("                    type: 'line', ");
		writer.println("                    xMin: jobStartTsMarker, ");
		writer.println("                    xMax: jobStartTsMarker, ");
		writer.println("                    borderColor: 'pink', ");
		writer.println("                    borderWidth: 2, ");
		writer.println("                    label: { ");
		writer.println("                        content: 'Start Time for this Job', ");
		writer.println("                        enabled: true, ");
		writer.println("                        position: 'top' ");
		writer.println("                    } ");
//		writer.println("                    // The below is only to get some tooltip on the marker line... alot of code for 'little value', which I'm not even sure it will work... ");
//		writer.println("                    // It did not work... possibly because 'yPos' wasn't found ");
//		writer.println("                    , ");
//		writer.println("                    enter: (context) => { ");
//		writer.println("                        let tooltip = document.getElementById('annotation-tooltip'); ");
//		writer.println("                        if (!tooltip) { ");
//		writer.println("                            tooltip = document.createElement('div'); ");
//		writer.println("                            tooltip.id = 'annotation-tooltip'; ");
//		writer.println("                            tooltip.style.position = 'absolute'; ");
//		writer.println("                            tooltip.style.backgroundColor = 'rgba(0, 0, 0, 0.8)'; ");
//		writer.println("                            tooltip.style.color = 'white'; ");
//		writer.println("                            tooltip.style.padding = '5px 10px'; ");
//		writer.println("                            tooltip.style.borderRadius = '5px'; ");
//		writer.println("                            tooltip.style.pointerEvents = 'none'; ");
//		writer.println("                            tooltip.style.zIndex = '1000'; ");
//		writer.println("                            context.chart.canvas.parentNode.appendChild(tooltip); ");
//		writer.println("                        } ");
//		writer.println("                        tooltip.innerHTML = 'Start Time for this Job'; ");
//		writer.println();                   
//		writer.println("                        // Calculate line position ");
//		writer.println("                        const chartRect = context.chart.canvas.getBoundingClientRect(); ");
//		writer.println("                        const xScale = context.chart.scales.x; ");
//		writer.println("                        const yScale = context.chart.scales.y; ");
//		writer.println("                         ");
//		writer.println("                        // Position tooltip at annotation line ");
//		writer.println("                        const xPos = xScale.getPixelForValue(jobStartTsMarker); // xMin = position ");
//		writer.println("                        const yPos = yScale.getPixelForValue(jobStartTsMarker); // hmm.. do I need to get the 'seconds pos' as well, or can I get the mouse position instead...");
//		writer.println();                   
//		writer.println("                        tooltip.style.left = `${chartRect.left + xPos}px`; ");
//		writer.println("                        tooltip.style.top = `${chartRect.top + yPos - 30}px`;  // Above the point ");
//		writer.println("                    }, ");
//		writer.println("                    leave: () => { ");
//		writer.println("                        const tooltip = document.getElementById('annotation-tooltip'); ");
//		writer.println("                        if (tooltip) tooltip.remove(); ");
//		writer.println("                    } ");
		writer.println("                }; ");
		writer.println();
		writer.println("                chartOptions.options.plugins.annotation.annotations['highlightLine'] = highlightLineObj; ");
		writer.println("                console.log('chartOptions adjustments after |jobStartTsMarker|.', chartOptions); ");
		writer.println("            } ");
		writer.println();
		writer.println("            if (_jobSchedulerTimelineChartInstance) { ");
		writer.println("                _jobSchedulerTimelineChartInstance.destroy(); ");
		writer.println("            } ");
		writer.println();
		writer.println("            // If we have data, generate the chart ");
		writer.println("            // When NO-DATA the input tect will still show the text (hopefully with a error message) ");
		writer.println("            if (parsedTsData.values.length > 0) ");
		writer.println("            { ");
		writer.println("                // Get element where to create the chart ");
		writer.println("                const ctx = document.getElementById('dbx-jobScheduler-timeline-chart').getContext('2d'); ");
		writer.println();
		writer.println("                // Create the chart ");
		writer.println("                _jobSchedulerTimelineChartInstance = new Chart(ctx, chartOptions); ");
		writer.println();
		writer.println("                // Make it visible ");
		writer.println("                document.getElementById('dbx-jobScheduler-timeline-chart').style.display='block'; ");
		writer.println();
		writer.println("                // Force re-render after a delay ");
		writer.println("                // If the timeline marker (the line) when 'execTime' is provided and it points at the LAST entry... Then it will be hidden... ");
		writer.println("                // But with this workaround, it will be visible again... after the re-render...");
		writer.println("                setTimeout(() => { _jobSchedulerTimelineChartInstance.update(); }, 1000); ");
		writer.println("            } ");
		writer.println();               
		writer.println("            // Set text ");
		writer.println("            document.getElementById('dbx-jobScheduler-text-content').textContent = input; ");
		writer.println();
//		writer.println("            // Open Modal ");
//		writer.println("            $('#dbx-jobScheduler-timeline-dialog').modal('show'); ");
//		writer.println();
//		writer.println("            // highlight again, since the dialog DOM wasn't visible earlier ");
//		writer.println("            Prism.highlightAll(); ");
		writer.println("        } ");
		writer.println();
		writer.println("    </script> ");
		writer.println();
	}
}
