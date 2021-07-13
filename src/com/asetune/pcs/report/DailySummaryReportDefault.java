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
package com.asetune.pcs.report;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import com.asetune.pcs.PersistWriterToHttpJson;
import com.asetune.pcs.report.IProgressReporter.State;
import com.asetune.pcs.report.content.AlarmsActive;
import com.asetune.pcs.report.content.AlarmsHistory;
import com.asetune.pcs.report.content.DailySummaryReportContent;
import com.asetune.pcs.report.content.DbxTuneErrors;
import com.asetune.pcs.report.content.IReportEntry;
import com.asetune.pcs.report.content.RecordingInfo;
import com.asetune.pcs.report.content.ReportContent;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;
import com.asetune.utils.HeartbeatMonitor;
import com.asetune.utils.StringUtil;
import com.asetune.utils.TimeUtils;

public class DailySummaryReportDefault
extends DailySummaryReportAbstract
{
	private static Logger _logger = Logger.getLogger(DailySummaryReportDefault.class);
	private List<IReportEntry> _reportEntries = new ArrayList<>();

	public boolean useBootstrap()
	{
		return true;
	}
	
	@Override
	public void create()
	throws InterruptedException, IOException
	{
		// Set output for TimeStamp format for Strings in ResultSetTableModel to: yyyy-MM-dd HH:mm:ss
//		System.setProperty(ResultSetTableModel.PROPKEY_TimestampToStringFmt, ResultSetTableModel.DEFAULT_TimestampToStringFmt_YMD_HMS);
		
		DailySummaryReportContent content = new DailySummaryReportContent();
		content.setServerName( getServerName() );

// Moved to init()
//		addReportEntries();
//		_logger.info("Initiated Daily Summary Report with " + _reportEntries.size() + " report entries.");

		// Get Configuration possibly from the DbxCentral
//		Configuration pcsSavedConf = getConfigFromDbxCentral(getServerName());
		Configuration pcsSavedConf = getConfigFromPcs();
		Configuration localConf    = Configuration.getCombinedConfiguration();
		
		// Iterate all entries and create the report
		int entryCount = _reportEntries.size();
		int count = 0;
		IProgressReporter progressReporter = getProgressReporter();
		for (IReportEntry entry : _reportEntries)
		{
			// First force a Garbage Collection
			System.gc();
//System.out.println("  ******* Used Memory " + Memory.getUsedMemoryInMB() + " MB ****** create(): BEFORE: "+ entry.getClass().getSimpleName());

			count++;
			int pctDone = (int) ( ((count*1.0) / (entryCount*1.0)) * 100.0 );
			try
			{
				if (entry.isEnabled())
				{
					// Report Progress 
					if (progressReporter != null)
					{
						boolean doNextEntry = progressReporter.setProgress(State.BEFORE, entry, "Creating ReportEntry '" + entry.getClass().getSimpleName() + "', with Subject '" + entry.getSubject() + "'.", pctDone);
						if ( doNextEntry == false )
							throw new InterruptedException("Report Creation was aborted...");
					}

					// Check if tables etc exists
					entry.checkForIssuesBeforeCreate(getConnection());
					if ( ! entry.hasProblem() )
					{
						// Create the report, this may take time since it executes SQL Statements
						_logger.info("Creating ReportEntry '" + entry.getClass().getSimpleName() + "', with Subject '" + entry.getSubject() + "'. Percent done: " + pctDone);

						// the ReportEntry needs any "helper" indexes to perform it's duties faster... create them
						entry.createReportingIndexes(getConnection());
						
						// Create
						entry.create(getConnection(), getServerName(), pcsSavedConf, localConf);
					}

					// Report Progress 
					if (progressReporter != null)
					{
						boolean doNextEntry = progressReporter.setProgress(State.AFTER, entry, "Creating ReportEntry '" + entry.getClass().getSimpleName() + "', with Subject '" + entry.getSubject() + "'.", pctDone);
						if ( doNextEntry == false )
							throw new InterruptedException("Report Creation was aborted...");
					}

				}
			} 
			catch (RuntimeException rte) 
			{
				_logger.warn("Problems creating ReportEntry for '" + entry.getClass().getSimpleName() + "'. Caught RuntimeException, continuing with next entry.", rte);
			}
			
			// If this is done from the Collectors thread... and each of the reports are taking a long time... 
			// Then we might want to "ping" the collector supervisor, that we are still "alive"
			HeartbeatMonitor.doHeartbeat();
//System.out.println("  ******* Used Memory " + Memory.getUsedMemoryInMB() + " MB ****** create(): AFTER: "+ entry.getClass().getSimpleName());
		}

		
		// Create and set TEXT/HTML Content
//		String htmlText = createHtml();
//		StringWriter writer = new StringWriter();
		
		// Create a temporary file (used to back the report content so we don't have to hold the string in memory
		// it will be deleted when the JVM stops
		File file = File.createTempFile("dsr_tmp_", ".html");
		file.deleteOnExit();
//		Writer writer = new FileWriter(file);
		try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) 
		{
			createHtml(writer);
		}

		// Create the "ShortMessage" used for email messages etc
		StringWriter shortMessageWriter = new StringWriter();
		createShortMessage(shortMessageWriter);
		
		
//		// When creating a Text Table, it may be large...
//		// So if the HTML Output is Large... lets not create a Text Table. We will probably get an OutOfMemory Error
//		int textSizeLimit = 5*1024*1024; // 5MB
//
//		// Check if we should create a Text Table
//		String clearText = "Text Size will be to large. htmlSize=" + htmlText.length() + ", limit=" + textSizeLimit + ". The text size will be bigger. So skipping text generation.";
//		if (htmlText.length() < textSizeLimit)
//			clearText = createText();

		// Set main Report in content
//		content.setReportAsHtml(htmlText);
		content.setReportFile(file);
//		content.setReportAsText(clearText);
		
		// Set Short Message in content
		content.setShortMessage(shortMessageWriter.toString());
		content.setShortMessageOfHtml(true);

		boolean hasIssueToReport = hasIssueToReport();
		content.setNothingToReport( ! hasIssueToReport );

		// set the content
		setReportContent(content);
	}

	public void addReportEntry(IReportEntry entry)
	{
		_reportEntries.add(entry);
	}

	public IReportEntry getReportEntry(Class<?> classToReturn)
	{
		for (IReportEntry entry : _reportEntries)
		{
			if (classToReturn.isInstance(entry))
				return entry;
		}
		return null;
	}

	@Override
	public void addReportEntriesTop()
	{
		addReportEntry( new RecordingInfo(this) );
		addReportEntry( new AlarmsActive(this)  );
		addReportEntry( new AlarmsHistory(this) );
	}

	@Override
	public void addReportEntries()
	{
	}

	@Override
	public void addReportEntriesBottom()
	{
		addReportEntry( new DbxTuneErrors(this) );
	}

	@Override
	public List<IReportEntry> getReportEntries()
	{
		return _reportEntries;
	}

	/**
	 * Get values from PCS table <code>MonSessionParams</code>
	 * 
	 * @return all Configuration objects (where Type = "combined.config")
	 */
	public Configuration getConfigFromPcs()
	{
		return getConfigFromPcs("combined.config");
	}

	public Configuration getLocalConfig()
	{
		return Configuration.getCombinedConfiguration();
	}

	/**
	 * Get values from PCS table <code>MonSessionParams</code>
	 * 
	 * @param type           can be null, then all "types" will be searched, but last one found will be returned.
	 * NOTE: current order is "cm", "system.config", "pcs.config", "combined.config", "system.properties", "temp.config"
	 * FIXME: look at what order we should *REALLY* use
	 * 
	 * @return all Configuration objects
	 */
	public Configuration getConfigFromPcs(String type)
	{
		DbxConnection conn = getConnection();
		
		String whereType = "";
		if (StringUtil.hasValue(type))
			whereType = "where [Type] = '" + type + "' \n";
		
		String tabName = "MonSessionParams";
		String sql = ""
			    + "select [Type], [ParamName], [ParamValue] \n"
			    + "from ["+tabName+"] \n"
			    + whereType
//			    + "order by [Type] \n"
			    + "";

		Configuration conf = new Configuration();
		
		sql = conn.quotifySqlString(sql);
		try ( Statement stmnt = conn.createStatement() )
		{
			// Unlimited execution time
			stmnt.setQueryTimeout(0);
			try ( ResultSet rs = stmnt.executeQuery(sql) )
			{
				while(rs.next())
				{
					String rType = rs.getString(1);
					String rName = rs.getString(2);
					String rVal  = rs.getString(3);

					if (rName != null && rVal != null)
					{
						Object oldVal = conf.put(rName, rVal);
						if (oldVal != null)
						{
							if ( ! oldVal.equals(rVal) )
							{
								if (_logger.isDebugEnabled())
									_logger.debug("getConfigFromPcs(): Replacing: newType='" + rType + "', key='" + rName + "', oldVal='" + oldVal + "', newValue='" + rVal + "'.");
							}
						}
					}
				}
			}
		}
		catch(SQLException ex)
		{
			_logger.warn("Problems getting values from '"+tabName+"': " + ex);
		}

		return conf;
//		return Configuration.EMPTY_CONFIGURATION;
	}


	public Configuration getConfigFromDbxCentral(String serverName)
	{
		// FIXME: get Configuration from DbxCentral... or should we get the configuration from PCS table "MonSessionParams", like we do in ReportEntryAbstract.getRecordingSessionParameter(...)
		String dbxCentralUrl = Configuration.getCombinedConfiguration().getProperty(PersistWriterToHttpJson.PROPKEY_url, null);
		if (StringUtil.hasValue(dbxCentralUrl))
		{
			// Fix 'http://localhost:8080/api/pcs/receiver' --> 'http://localhost:8080/api/getConfig/DailySummaryReport?srvName='+serverName
			// Call the URL...
			// Transform the results to a Configuration Object
		}
		
		return Configuration.EMPTY_CONFIGURATION;
	}

	/**
	 * Check all ReportEntries, if any of them has something to report or not
	 * @return 
	 */
	public boolean hasIssueToReport()
	{
		for (IReportEntry entry : _reportEntries)
		{
			if ( entry.hasIssueToReport() )
				return true;
		}
		return false;
	}


	private void createDbxTuneCss(Writer w)
	throws IOException
	{
		w.append("\n");
		w.append("    <!-- DbxTune CSS --> \n");
		w.append("    <style type='text/css'> \n");
		w.append("        body { \n");
		w.append("            -webkit-text-size-adjust: 100%; \n");
		w.append("            -ms-text-size-adjust: 100%; \n");
		w.append("            font-family: Arial, Helvetica, sans-serif; \n");
		w.append("            font-family: Arial, Helvetica, sans-serif; \n");
//		sb.append("            line-height: 1;  /* overiding bootstrap, which has 1.5 */ \n");  
		w.append("            line-height: normal;  /* overiding bootstrap, which has 1.5 */ \n");  
		w.append("        } \n");
		w.append("        pre { \n");
		w.append("            font-size: 10px; \n");
		w.append("            word-wrap: none; \n");
		w.append("            white-space: no-wrap; \n");
//		sb.append("            space: nowrap; \n");
		w.append("        } \n");
		w.append("        table { \n");
		w.append("            mso-table-layout-alt: fixed; \n"); // not sure about this - https://gist.github.com/webtobesocial/ac9d052595b406d5a5c1
		w.append("            mso-table-overlap: never; \n");    // not sure about this - https://gist.github.com/webtobesocial/ac9d052595b406d5a5c1
		w.append("            mso-table-wrap: none; \n");        // not sure about this - https://gist.github.com/webtobesocial/ac9d052595b406d5a5c1
		w.append("            border-collapse: collapse; \n");
		w.append("        } \n");
		w.append("        th { \n");
		w.append("            border: 1px solid black; \n");
		w.append("            text-align: left; \n");
		w.append("            padding: 2px; \n");
		w.append("            white-space: nowrap; \n");
		w.append("            background-color: gray; \n");
		w.append("            color: white; \n");
		w.append("        } \n");
		w.append("        td { \n");
		w.append("            border: 1px solid black; \n");
		w.append("        /*  text-align: left; */ \n");
		w.append("            padding: 2px; \n");
		w.append("            white-space: nowrap; \n");
		w.append("        } \n");
		w.append("        tr:nth-child(odd) { \n");
		w.append("            background-color: white; \n"); // Otherwise it will be transparent (and bootstrap will write "right line" of a box in the table)
		w.append("        } \n");
		w.append("        tr:nth-child(even) { \n");
		w.append("            background-color: #f2f2f2; \n");
		w.append("        } \n");
		w.append("        h2 { \n");
		w.append("            border-bottom: 2px solid black; \n");
		w.append("            border-top: 2px solid black; \n");
		w.append("            margin-bottom: 3px; \n");
		w.append("        } \n");
		w.append("        h3 { \n");
		w.append("            border-bottom: 1px solid black; \n");
		w.append("            border-top: 1px solid black; \n");
		w.append("            margin-bottom: 3px; \n");
		w.append("        } \n");
		w.append("\n");
//		sb.append("        /* the below is to HIDE/SHOW content (in mail, which can not execute javascript) */ \n");
		w.append("        .hide-show { \n");
		w.append("            display: none; \n");
		w.append("        } \n");
		w.append("        input[type='checkbox']:checked ~ .hide-show { \n");
		w.append("            display: block; \n");
		w.append("        } \n");
		w.append("\n");
		w.append("        dsr-warning-list { \n");
		w.append("            color: orange; \n");
		w.append("        } \n");
		w.append("        dsr-warning-list li { \n");
		w.append("            color: orange; \n");
		w.append("        } \n");
		w.append("\n");
		w.append("        /* Example settings: scrollbar if to big */ \n");
		w.append("        xmp {                      \n");
		w.append("//            background: #C0C0C0;   \n");
		w.append("//            font-size: 0.3em;      \n");
		w.append("            white-space: pre-wrap; \n"); // wrap long rows
		w.append("            width: 100%;           \n");
		w.append("            max-height: 400px;     \n");
		w.append("            overflow: auto;        \n");
		w.append("            margin-top: 0px;       \n");
		w.append("            margin-right: 0px;     \n");
		w.append("            margin-bottom: 0px;    \n");
		w.append("            margin-left: 0px;      \n");
		w.append("        }                          \n");
		w.append("\n");
		w.append("        /* Display the 'max' value as text 'over' the sparkline mini-chart */ \n");
		w.append("        .sparkline-max-val {					\n"); // write on top of 'sparkline-wrapper' where the real sparkline chart is located
		w.append("            position:  absolute; 			\n"); 
		w.append("            z-index:     2; 					\n"); // Sparkline has 1 ... so write "above" the sparkline
		w.append("            left:        2px; 				\n");
		w.append("            top:         -2px; 				\n");
		w.append("            font-size:   9px;				\n");
		w.append("            font-family: Tahoma, Arial;		\n");
		w.append("            color:       black;				\n");
		w.append("            display:     none;				\n"); // This will be changed when sparkline is loaded
		w.append("        }									\n");
		w.append("        .sparkline-wrapper {					\n");
		w.append("            position: relative;				\n");
		w.append("        }									\n");
		w.append("\n");
		w.append("        /* The below data-tooltip is used to show Actual exected SQL Text, as a tooltip where a normalized text is in a table cell */ \n");
		w.append("        [data-tooltip] {						\n");
		w.append("            position: relative;				\n");
//		sb.append("            cursor: help;					\n");
		w.append("        }									\n");
		w.append("        										\n");
		w.append("        /* 'tooltip' CSS settings for SQL Text... */ \n");
		w.append("        [data-tooltip]:hover::before {		\n");
		w.append("            content: attr(data-tooltip);		\n");
		w.append("            position: absolute;				\n");
		w.append("            z-index: 103; 					\n");
		w.append("            top: 20px;						\n");
		w.append("            left: 30px;						\n");
		w.append("            width: 1800px;					\n");
		w.append("            padding: 10px;					\n");
		w.append("            background: #454545;				\n");
		w.append("            color: #fff;						\n");
//		sb.append("            background: black;				\n");
//		sb.append("            color: white;					\n");
//		sb.append("            font-size: 12px;					\n");
		w.append("            font-size: 11px;					\n");
		w.append("            font-family: Courier;			\n");
		w.append("            white-space: pre-wrap;			\n");
		w.append("        }									\n");
		w.append("        [data-title]:hover::after {			\n");
		w.append("            content: '';						\n");
		w.append("            position: absolute;				\n");
		w.append("            bottom: -12px;					\n");
		w.append("            left: 8px;						\n");
		w.append("            border: 8px solid transparent;	\n");
		w.append("            border-bottom: 8px solid #000;	\n");
		w.append("        }									\n");
		w.append("\n");
		w.append("        /* SQL Text Content in modal view */ \n");
		w.append("        .dbx-view-sqltext-content { 			\n");
		w.append("            font-size: 11px;					\n");
		w.append("            white-space: pre-wrap;			\n");
		w.append("        } 									\n");
		w.append("\n");
		w.append("        /* Override some 'prism' formatting to be smaller & wrap long lines */ \n");
		w.append("        code[class*=language-], pre[class*=language-] { 			\n");
		w.append("            font-size: 11px;										\n");
		w.append("            white-space: pre-wrap;								\n");
		w.append("        } 														\n");
		w.append("\n");
		w.append("        /* fix issue with bootstrap and jquery-sparkline tooltip background is smaller than it should be */ \n");
		w.append("        .jqstooltip { 											\n");
		w.append("            box-sizing: content-box;								\n");
		w.append("        } 														\n");
		w.append("\n");
		w.append("    </style> \n");
		w.append("\n");
	}
	
	//-------------------------------------------------------------------------------------------
	// BEGIN: HTML Message Report 
	//-------------------------------------------------------------------------------------------
	public void createHtmlHead(Writer writer)
	throws IOException
	{
		// Have we got a "external file" for HTML Headers... where we can change CSS etc
		String htmlHeadFile = Configuration.getCombinedConfiguration().getProperty(DailySummaryReportFactory.PROPKEY_reportHtml_headFile, DailySummaryReportFactory.DEFAULT_reportHtml_headFile);
		if (StringUtil.hasValue(htmlHeadFile))
		{
			File f = new File(htmlHeadFile);

			try
			{
				String htmlHeadContent = FileUtils.readFileToString(f, StandardCharsets.UTF_8);
				// replace variable(s) in content
				htmlHeadContent = htmlHeadContent.replace("${DBMS_SERVER_NAME}", getServerName());
					
				//return htmlHeadContent;
				writer.append(htmlHeadContent);
				return;
			}
			catch(IOException ex)
			{
				_logger.error("Problems reading External HTML HEAD File '" + f.getAbsolutePath() + "', Skipping this and using System provided header. Caught: "+ex, ex);
			}
		}

		//-------------------------------------------------------------------
		// Normal logic: Default System provided HTML Header
		//-------------------------------------------------------------------
//		StringBuilder sb = new StringBuilder();
		Writer w = writer;
		
		String titleReportPeriod = "";
//		if (hasReportPeriod())
//		{
//			if (getReportPeriodBeginTime() != null) titleReportPeriod +=         TimeUtils.getCurrentTimeForFileNameHm(getReportPeriodBeginTime().getTime());
//			if (getReportPeriodEndTime()   != null)	titleReportPeriod += " - " + TimeUtils.getCurrentTimeForFileNameHm(getReportPeriodEndTime()  .getTime());
//		}
		String    reportBeginDateStr = TimeUtils.getCurrentTimeForFileNameYmd(getReportBeginTime().getTime());
//		String    reportBeginTimeStr = TimeUtils.getCurrentTimeForFileNameHm (getReportBeginTime().getTime());
//		String    reportEndDateStr   = TimeUtils.getCurrentTimeForFileNameYmd(getReportEndTime()  .getTime());
//		String    reportEndTimeStr   = TimeUtils.getCurrentTimeForFileNameHm (getReportEndTime()  .getTime());

		titleReportPeriod = " - " + reportBeginDateStr;

		w.append("\n");
		w.append("<head> \n");
		w.append("    <title>DSR: ").append(getServerName()).append(titleReportPeriod).append("</title> \n");
		w.append("    <link href='data:image/x-icon;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAABmJLR0QA/wD/AP+gvaeTAAAACXBIWXMAAAsTAAALEwEAmpwYAAAAB3RJTUUH4QwQDwcpgoqRjwAAAB1pVFh0Q29tbWVudAAAAAAAQ3JlYXRlZCB3aXRoIEdJTVBkLmUHAAABoklEQVQ4y72SP2haURTGv/veS8HFJWOGhgwNiNsli5uQISkIpkUi+GfJI1vALbg1cTFBBPeCkMHqkIzJG5QQxOlZQRqEdpA45Fk1BN7yNt+XyZTEPy2l9Gznu9/9nXPPucC/CsuyHnVdZzab5SJfv99vttvtaU+5XKaUklLKhYBgMEgpJWu1GgFAmxxEo1GRTqepqiq+/tz9BdkOAlfXz6njnAMAhBALyqyc8KJHtmwSusmWTV70SKyccjQabXY6necCszG6Of8Znzde3NHq9TpLpRJisRg0TcPt7TfoX+6B7z8AElAUwHUBIYD1d9PARCJBKSVDoRAN44oAsHdwSJJ8sB0aN00+2A5Jcu/gcKozJR6Pw+fzIZVKYWtrWwAAxy4AYNnrgfZmCcteDyZ6JnNM0zRZqVQ4GAyyM2fw/mOS3bseCEJVVIzdMQQE1lbf4vL8TEQiEXa7XQQCgfmLyOVycwdZKBQopWQ+n5/tGQ6H+8VikX/1pQ3DYDgcZrVa/SOA+lqwbfuTZVkgXTQajaPfAZTXQjKZhN/vx87OB/yXeAKvTsN3xZdB4gAAAABJRU5ErkJggg==' rel='icon' type='image/x-icon' /> \n");
		w.append("\n");
		w.append("    <meta charset='utf-8'/> \n");
		w.append("    <meta name='viewport' content='width=device-width, initial-scale=1, shrink-to-fit=no'> \n");
		w.append("    <meta name='x-apple-disable-message-reformatting' /> \n");
//		sb.append("    <meta name='viewport' content='width=device-width, user-scalable=no, initial-scale=1, maximum-scale=1, minimal-ui'>\n");
		w.append("\n");

		if (useBootstrap())
		{
			// Or should we "read/import" the CSS in here ... (so that it works for MAIL readers as well, since they can't access URL's
		//	sb.append("    <link rel='stylesheet' href='https://stackpath.bootstrapcdn.com/bootstrap/4.1.3/css/bootstrap.min.css' integrity='sha384-MCw98/SFnGE8fJT3GXwEOngsV7Zt27NXFoaoApmYm81iuXoPkFOJwJ8ERdknLPMO' crossorigin='anonymous'> \n");
		//	sb.append("    <link rel='stylesheet' href='https://cdn.jsdelivr.net/npm/bootstrap@4.5.3/dist/css/bootstrap.min.css' integrity='sha384-TX8t27EcRE3e/ihU7zmQxVncDAy5uIKz4rEkgIXeMed4M0jlfIDPvg6uqKI2xXr2' crossorigin='anonymous'> \n");

			String bootstrapCss = "";
//			String DBXTUNE_HOME = Configuration.getCombinedConfiguration().getProperty("DBXTUNE_HOME");
//			File bootstrapCssFile = new File(DBXTUNE_HOME + "/resources/WebContent/scripts/bootstrap/4.5.3/css/bootstrap.css");
//			if (bootstrapCssFile.exists())
//			{
//				String bootstrapCss = FileUtils.readFileToString(bootstrapCssFile, StandardCharsets.UTF_8);
//			}
		
			// if we want to "read/import" the CSS from local file
			try 
			{
				bootstrapCss = com.asetune.utils.FileUtils.readFile(ReportContent.class, "bootstrap_453.css");
			}
			catch (Exception ex)
			{
				_logger.error("Problems reading file 'bootstrap_453.css'. Caught: " + ex, ex);
				bootstrapCss = "/* Problems reading file 'bootstrap_453.css'. Caught: " + ex + " */";
			}

			// Write bootstrap CSS -- but invisible for Outlook
//			w.append(" \n");
//			w.append("<!--[if !mso]><!--> \n"); // BEGIN: IGNORE THIS SECTION FOR OUTLOOK

			w.append("<style type='text/css'> \n");
			w.append(bootstrapCss);
			w.append("</style> \n");

//			w.append("<!--<![endif]--> \n"); // END: IGNORE THIS SECTION FOR OUTLOOK
//			w.append(" \n");


			//-----------------------------------------------------------------
			// Only for OUTLOOK -- https://www.hteumeuleu.com/2020/outlook-rendering-engine/
			//-----------------------------------------------------------------
			// <!--[if mso]>
			// <p>This is only visible in Outlook 2007-2019 on Windows.</p>
			// <![endif]-->
			//-----------------------------------------------------------------
			//sb.append("<!--[if mso]> \n"); // BEGIN: ONLY FOR OUTLOOK
			//sb.append("<![endif]-->  \n"); // END: ONLY FOR OUTLOOK

			//-----------------------------------------------------------------
			// Ignored for OUTLOOK -- https://www.hteumeuleu.com/2020/outlook-rendering-engine/
			//-----------------------------------------------------------------
			// <!--[if !mso]><!-->
			// <p>This is everything but The Outlooks.</p>
			// <!--<![endif]-->
			//-----------------------------------------------------------------
			//sb.append("<!--[if !mso]><!--> \n"); // BEGIN: IGNORE THIS SECTION FOR OUTLOOK
			//sb.append("<!--<![endif]-->    \n"); // END: IGNORE THIS SECTION FOR OUTLOOK
		}
		
//		sb.append("    <link rel='stylesheet' href='https://cdnjs.cloudflare.com/ajax/libs/prism/1.22.0/themes/prism.min.css'> \n");
		w.append("    <link rel='stylesheet' href='https://cdnjs.cloudflare.com/ajax/libs/prism/1.22.0/themes/prism-okaidia.min.css'> \n");

		createDbxTuneCss(w);

		w.append("    <SCRIPT src='http://www.dbxtune.com/sorttable.js'></SCRIPT> \n");
//		sb.append("    <SCRIPT src='https://code.jquery.com/jquery-3.2.1.min.js'></SCRIPT> \n");                       // NOTE: FIXME -- This should be located "elsewhere"
//		sb.append("    <SCRIPT src='https://omnipotent.net/jquery.sparkline/2.1.2/jquery.sparkline.js'></SCRIPT> \n"); // NOTE: FIXME -- This should be located "elsewhere"

		w.append("    <SCRIPT src='https://cdnjs.cloudflare.com/ajax/libs/jquery/3.5.1/jquery.min.js'></SCRIPT> \n");
		w.append("    <SCRIPT src='https://cdnjs.cloudflare.com/ajax/libs/jquery-sparklines/2.1.2/jquery.sparkline.min.js'></SCRIPT> \n");
		
		if (useBootstrap())
		{
//			sb.append("    <script src='https://cdnjs.cloudflare.com/ajax/libs/popper.js/1.12.9/umd/popper.min.js' integrity='sha384-ApNbgh9B+Y1QKtv3Rn7W3mgPxhU9K/ScQsAP7hUibX39j7fakFPskvXusvfa0b4Q' crossorigin='anonymous'></script> \n");
//			sb.append("    <script src='https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/js/bootstrap.min.js'       integrity='sha384-JZR6Spejh4U02d8jOt6vLEHfe/JQGiRRSQQxSfFWpi1MquVdAyjUar5+76PVCmYl' crossorigin='anonymous'></script> \n");
			w.append("    <script src='https://cdn.jsdelivr.net/npm/popper.js@1.16.1/dist/umd/popper.min.js'      integrity='sha384-9/reFTGAW83EW2RDu2S0VKaIzap3H66lZH81PoYlFhbGU+6BZp6G7niu735Sk7lN' crossorigin='anonymous'></script> \n");
			w.append("    <script src='https://cdn.jsdelivr.net/npm/bootstrap@4.5.3/dist/js/bootstrap.min.js'     integrity='sha384-w1Q4orYjBQndcko6MimVbzY0tgp4pWB4lZ7lr30WKz0vr/aWKhXdBNmNb5D92v7s' crossorigin='anonymous'></script> \n");
		}
		
		w.append("    <script src='https://cdnjs.cloudflare.com/ajax/libs/prism/1.22.0/prism.min.js'></script> \n");
		w.append("    <script src='https://cdnjs.cloudflare.com/ajax/libs/prism/1.22.0/components/prism-sql.min.js'></script> \n");


		w.append("\n");
		w.append("    <script type='text/javascript'>  \n");
		w.append("        function toggle_visibility(id) \n");
		w.append("        { \n");
		w.append("           var e = document.getElementById(id); \n");
		w.append("           if(e.style.display == 'block') \n");
		w.append("              e.style.display = 'none'; \n");
		w.append("           else \n");
		w.append("              e.style.display = 'block'; \n");
		w.append("           return false; \n");
		w.append("        } \n");
		w.append("\n");
		w.append("    </script> \n");

//		<STYLE type="text/css">
//		  /* Sortable tables */
//		  table.sortable thead {
//		    background-color:#eee;
//		    color:#666666;
//		    font-weight: bold;
//		    cursor: default;
//		  }
//		  body { font-size : 100%; font-family : Verdana,Helvetica,Arial,sans-serif; }
//		  h1, h2, h3 { font-size : 150%; }
//		  table { margin: 1em; border-collapse: collapse; font-size : 90%; }
//		  td, th { padding: .1em; border: 1px #ccc solid; font-size : 90%; }
//		  thead { background: #fc9; } </STYLE>
		w.append("</head> \n");
		w.append("\n");
	}


	public void createHtmlBody(Writer sb)
	throws IOException
	{
		sb.append("<body>\n");
//		sb.append("<body style'min-width: 100%'>\n");
//		sb.append("<body style'min-width: 2048px'>\n");
//		sb.append("<body style'min-width: 1024px'>\n");

		if (useBootstrap())
		{
			sb.append("<div class='container-fluid'> \n"); // BEGIN: Bootstrap 4 container
		}

		sb.append("\n");
		
		// TOC HEADER
		if (useBootstrap())
		{
			// Bootstrap "card" - BEGIN
			sb.append("<!--[if !mso]><!--> \n"); // BEGIN: IGNORE THIS SECTION FOR OUTLOOK
			sb.append("<div id='toc' class='card border-dark mb-3'>");
			sb.append("<h5 class='card-header'><b>Daily Summary Report for Servername: ").append(getServerName()).append("</b></h5>");
			sb.append("<div class='card-body'>");
			sb.append("<!--<![endif]-->    \n"); // END: IGNORE THIS SECTION FOR OUTLOOK
			
			sb.append("<!--[if mso]> \n"); // BEGIN: ONLY FOR OUTLOOK
			sb.append("<h2>Daily Summary Report for Servername: ").append(getServerName()).append("</h2>\n");
			sb.append("<![endif]-->  \n"); // END: ONLY FOR OUTLOOK
		}
		else
		{
			// Normal HTML - H2 heading
			sb.append("<h2>Daily Summary Report for Servername: ").append(getServerName()).append("</h2>\n");
		}
		sb.append( createDbxCentralLink(true) );

		//--------------------------------------------------
		// TOC
		sb.append("<br> \n");
		sb.append("Links to Report Sections. \n");
		sb.append("<ul> \n");
		for (IReportEntry entry : _reportEntries)
		{
			String tocSubject = entry.getSubject();
			String tocDiv     = StringUtil.stripAllNonAlphaNum(tocSubject);

			// Strip off parts that may be details
			int firstLeftParentheses = tocSubject.indexOf("(");
			if (firstLeftParentheses != -1)
				tocSubject = tocSubject.substring(0, firstLeftParentheses - 1).trim();

			String liContent = "<a href='#" + tocDiv + "'>" + tocSubject + "</a>";
			
			sb.append("<li>").append(liContent).append("</li> \n");
		}
		sb.append("</ul> \n");
		sb.append("\n<br>");

		// TOC FOOTER
		if (useBootstrap())
		{
			// Bootstrap "card" - END
			sb.append("<!--[if !mso]><!--> \n"); // BEGIN: IGNORE THIS SECTION FOR OUTLOOK
			sb.append("</div>"); // end: card-body
			sb.append("</div>"); // end: card
			sb.append("<!--<![endif]-->    \n"); // END: IGNORE THIS SECTION FOR OUTLOOK
		}
//System.out.println("  ******* Used Memory " + Memory.getUsedMemoryInMB() + " MB ****** at createHtmlBody(): after TOC");


		//--------------------------------------------------
		// ALL REPORTS
		for (IReportEntry entry : _reportEntries)
		{
			String tocSubject = entry.getSubject();
			String tocDiv     = StringUtil.stripAllNonAlphaNum(tocSubject);

			// Add a section header
			sb.append("\n");
			sb.append("\n");
			sb.append("<!-- ================================================================================= -->\n");
			sb.append("<!-- " + entry.getSubject()                                                        + " -->\n");
			sb.append("<!-- ================================================================================= -->\n");

			// Section HEADER
			if (useBootstrap())
			{
				// Bootstrap "card" - BEGIN
				sb.append("<!--[if !mso]><!--> \n"); // BEGIN: IGNORE THIS SECTION FOR OUTLOOK
				sb.append("<div id='").append(tocDiv).append("' class='card border-dark mb-3'>");
				sb.append("<h5 class='card-header'><b>").append(entry.getSubject()).append("</b></h5>");
				sb.append("<div class='card-body'>");
				sb.append("<!--<![endif]-->    \n"); // END: IGNORE THIS SECTION FOR OUTLOOK
				
				sb.append("<!--[if mso]> \n"); // BEGIN: ONLY FOR OUTLOOK
				sb.append("<h2 id='").append(tocDiv).append("'>").append(entry.getSubject()).append("</h2> \n");
				sb.append("<![endif]-->  \n"); // END: ONLY FOR OUTLOOK
			}
			else
			{
				// Normal HTML - H2 heading
				sb.append("<h2 id='").append(tocDiv).append("'>").append(entry.getSubject()).append("</h2> \n");
			}

			if (entry.isEnabled())
			{
				try
				{
					// Warning messages
					if (entry.hasWarningMsg())
						sb.append(entry.getWarningMsg());

					// Get the message text
					if ( ! entry.hasProblem() )
						entry.writeMessageText(sb);
					
					// If the entry indicates that it has a problem... then print that.
					if ( entry.hasProblem() )
						sb.append(entry.getProblemText());
					
					// if we should append anything after an entry... Possibly '<br>\n'
					sb.append(entry.getEndOfReportText());

					// Notes for how to: Disable this entry
					if (entry.canBeDisabled())
					{
						sb.append("<br>");
						sb.append("<i>To disable this report entry, put the following in the configuration file. ");
						sb.append("<code>").append(entry.getIsEnabledConfigKeyName()).append(" = false</code></i><br>\n");
					}
				}
				catch (RuntimeException rte)
				{
					sb.append("Problems 'writing' the HTML report text for section '" + entry.getSubject() + "'. Caught: " + rte + "\n");
					sb.append("Continuing with next report section... <br> \n");
					sb.append("Exception: <br> \n");
					sb.append("<pre><code> \n");
					sb.append(StringUtil.exceptionToString(rte));
					sb.append("</code></pre> \n");
				}
			}
			else
			{
				String reason = entry.getDisabledReason();
				if (StringUtil.hasValue(reason))
				{
					// Entry is DISABLED
					sb.append("This entry is <b>disabled</b>, reason:<br>");
					sb.append(reason);
					sb.append("<br>");
				}
				else
				{
					// Entry is DISABLED
					sb.append("This entry is <b>disabled</b>, to enable it; put the following in the configuration file. ");
					sb.append("<code>").append(entry.getIsEnabledConfigKeyName()).append(" = false</code><br>");
				}
			}

			// Section FOOTER
			if (useBootstrap())
			{
				// Bootstrap "card" - END
				sb.append("<!--[if !mso]><!--> \n"); // BEGIN: IGNORE THIS SECTION FOR OUTLOOK
				sb.append("</div>"); // end: card-body
				sb.append("</div>"); // end: card
				sb.append("<!--<![endif]-->    \n"); // END: IGNORE THIS SECTION FOR OUTLOOK
			}
			
//System.gc();
//System.out.println("  ******* Used Memory " + Memory.getUsedMemoryInMB() + " MB ****** "+ entry.getClass().getSimpleName());
		}
		sb.append("\n<br>");

		//--------------------------------------------------
		// END
		sb.append("\n<br>");
		sb.append("\n<br>");
		sb.append("\n<code>--end-of-report--</code> \n");

		// Some static code for showing dialogs
		sb.append("\n");
		sb.append( createShowSqlTextDialogHtml() );
		sb.append( createShowSqlTextDialogJs()   );

		sb.append("\n");
		sb.append("</div> \n"); // END: Bootstrap 4 container
		sb.append("</body> \n");

		// Collect some garbage
		System.gc();
	}

	public void createHtml(Writer writer) 
	throws IOException
	{
//		writer.append("<html>\n");
		writer.append("<!doctype html>  \n");
		writer.append("<html lang='en'> \n");

		try
		{
			createHtmlHead(writer);
			createHtmlBody(writer);
		}
		catch (RuntimeException rte)
		{
			writer.append("<b>Problems creating HTML report</b>, Caught RuntimeException: ").append(rte.toString()).append("<br> \n");
			writer.append("<pre>\n");
			writer.append(StringUtil.exceptionToString(rte));
			writer.append("</pre>\n");
			
			_logger.warn("Problems creating HTML Daily Summary Report. Caught: "+rte, rte);
		}

		writer.append("</html> \n");
		writer.flush();
	}
	//-------------------------------------------------------------------------------------------
	// END: HTML Message Report 
	//-------------------------------------------------------------------------------------------

	
	//-------------------------------------------------------------------------------------------
	// BEGIN: Short Message 
	//-------------------------------------------------------------------------------------------
	public void createShortMessageHead(Writer w)
	throws IOException
	{
		w.append("\n");
		w.append("<head> \n");

//		w.append("    <title>DSR: ").append(getServerName()).append(titleReportPeriod).append("</title> \n");
//		w.append("    <link href='data:image/x-icon;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAABmJLR0QA/wD/AP+gvaeTAAAACXBIWXMAAAsTAAALEwEAmpwYAAAAB3RJTUUH4QwQDwcpgoqRjwAAAB1pVFh0Q29tbWVudAAAAAAAQ3JlYXRlZCB3aXRoIEdJTVBkLmUHAAABoklEQVQ4y72SP2haURTGv/veS8HFJWOGhgwNiNsli5uQISkIpkUi+GfJI1vALbg1cTFBBPeCkMHqkIzJG5QQxOlZQRqEdpA45Fk1BN7yNt+XyZTEPy2l9Gznu9/9nXPPucC/CsuyHnVdZzab5SJfv99vttvtaU+5XKaUklLKhYBgMEgpJWu1GgFAmxxEo1GRTqepqiq+/tz9BdkOAlfXz6njnAMAhBALyqyc8KJHtmwSusmWTV70SKyccjQabXY6necCszG6Of8Znzde3NHq9TpLpRJisRg0TcPt7TfoX+6B7z8AElAUwHUBIYD1d9PARCJBKSVDoRAN44oAsHdwSJJ8sB0aN00+2A5Jcu/gcKozJR6Pw+fzIZVKYWtrWwAAxy4AYNnrgfZmCcteDyZ6JnNM0zRZqVQ4GAyyM2fw/mOS3bseCEJVVIzdMQQE1lbf4vL8TEQiEXa7XQQCgfmLyOVycwdZKBQopWQ+n5/tGQ6H+8VikX/1pQ3DYDgcZrVa/SOA+lqwbfuTZVkgXTQajaPfAZTXQjKZhN/vx87OB/yXeAKvTsN3xZdB4gAAAABJRU5ErkJggg==' rel='icon' type='image/x-icon' /> \n");
//		w.append("\n");
		w.append("    <meta charset='utf-8'/> \n");
		w.append("    <meta name='viewport' content='width=device-width, initial-scale=1, shrink-to-fit=no'> \n");
		w.append("    <meta name='x-apple-disable-message-reformatting' /> \n");

		createDbxTuneCss(w);
		
		w.append("</head> \n");
		w.append("\n");
	}

	public void createShortMessageBody(Writer w)
	throws IOException
	{
		w.append("<body>\n");
		w.append("\n");
		
		// Normal HTML - H2 heading
		w.append("<h2>Daily Summary Report for Servername: ").append(getServerName()).append("</h2>\n");

		w.append( createDbxCentralLink(false) );

		//--------------------------------------------------
		// TOC
		w.append("<br> \n");
		w.append("Links to Report Sections. \n");
		w.append("<ul> \n");
		for (IReportEntry entry : _reportEntries)
		{
			// Skip if "section" should not be part of the Short Message
			if ( ! entry.hasShortMessageText() )
				continue;

			String tocSubject = entry.getSubject();
			String tocDiv     = StringUtil.stripAllNonAlphaNum(tocSubject);

			// Strip off parts that may be details
			int firstLeftParentheses = tocSubject.indexOf("(");
			if (firstLeftParentheses != -1)
				tocSubject = tocSubject.substring(0, firstLeftParentheses - 1).trim();

			String liContent = "<a href='#" + tocDiv + "'>" + tocSubject + "</a>";
			
			w.append("<li>").append(liContent).append("</li> \n");
		}
		w.append("</ul> \n");
		w.append("\n<br>");

		//--------------------------------------------------
		// ALL REPORTS
		for (IReportEntry entry : _reportEntries)
		{
			// Skip if "section" should not be part of the Short Message
			if ( ! entry.hasShortMessageText() )
				continue;

			String tocSubject = entry.getSubject();
			String tocDiv     = StringUtil.stripAllNonAlphaNum(tocSubject);

			// Add a section header
			w.append("\n");
			w.append("\n");
			w.append("<!-- ================================================================================= -->\n");
			w.append("<!-- " + entry.getSubject()                                                        + " -->\n");
			w.append("<!-- ================================================================================= -->\n");

			// Section HEADER
			w.append("<h2 id='").append(tocDiv).append("'>").append(entry.getSubject()).append("</h2> \n");

			if (entry.isEnabled())
			{
				try
				{
					// Warning messages
					if (entry.hasWarningMsg())
						w.append(entry.getWarningMsg());

					// Get the message text
					if ( ! entry.hasProblem() )
						entry.writeShortMessageText(w);
					
					// If the entry indicates that it has a problem... then print that.
					if ( entry.hasProblem() )
						w.append(entry.getProblemText());
					
					// if we should append anything after an entry... Possibly '<br>\n'
					w.append(entry.getEndOfReportText());

					// Notes for how to: Disable this entry
					if (entry.canBeDisabled())
					{
						w.append("<br>");
						w.append("<i>To disable this report entry, put the following in the configuration file. ");
						w.append("<code>").append(entry.getIsEnabledConfigKeyName()).append(" = false</code></i><br>\n");
					}
				}
				catch (RuntimeException rte)
				{
					w.append("Problems 'writing' the HTML report text for section '" + entry.getSubject() + "'. Caught: " + rte + "\n");
					w.append("Continuing with next report section... <br> \n");
					w.append("Exception: <br> \n");
					w.append("<pre><code> \n");
					w.append(StringUtil.exceptionToString(rte));
					w.append("</code></pre> \n");
				}
			}
			else
			{
				String reason = entry.getDisabledReason();
				if (StringUtil.hasValue(reason))
				{
					// Entry is DISABLED
					w.append("This entry is <b>disabled</b>, reason:<br>");
					w.append(reason);
					w.append("<br>");
				}
				else
				{
					// Entry is DISABLED
					w.append("This entry is <b>disabled</b>, to enable it; put the following in the configuration file. ");
					w.append("<code>").append(entry.getIsEnabledConfigKeyName()).append(" = false</code><br>");
				}
			}

			// Section FOOTER
		}
		w.append("\n<br>");

		//--------------------------------------------------
		// END
		w.append("\n<br>");
		w.append("\n<br>");
		w.append("\n<code>--end-of-short-report--</code> \n");

		w.append("\n");
		w.append("</body> \n");
	}

	/**
	 * Create a short message, that for example can be used as a mail message
	 * 
	 * @param writer
	 * @throws IOException
	 */
	public void createShortMessage(Writer writer) 
	throws IOException
	{
//		writer.append("<html>\n");
		writer.append("<!doctype html>  \n");
		writer.append("<html lang='en'> \n");

		try
		{
			createShortMessageHead(writer);
			createShortMessageBody(writer);
		}
		catch (RuntimeException rte)
		{
			writer.append("<b>Problems creating Short Message report</b>, Caught RuntimeException: ").append(rte.toString()).append("<br> \n");
			writer.append("<pre>\n");
			writer.append(StringUtil.exceptionToString(rte));
			writer.append("</pre>\n");
			
			_logger.warn("Problems creating Daily Summary Report (ShortMessage). Caught: "+rte, rte);
		}

		writer.append("</html> \n");
		writer.flush();
	}
	//-------------------------------------------------------------------------------------------
	// END: Short Message 
	//-------------------------------------------------------------------------------------------
}
