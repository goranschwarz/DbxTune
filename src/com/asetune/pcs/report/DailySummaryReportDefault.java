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
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import com.asetune.pcs.PersistWriterToHttpJson;
import com.asetune.pcs.report.IProgressReporter.State;
import com.asetune.pcs.report.content.AlarmsActive;
import com.asetune.pcs.report.content.AlarmsHistory;
import com.asetune.pcs.report.content.DailySummaryReportContent;
import com.asetune.pcs.report.content.DbxTuneErrors;
import com.asetune.pcs.report.content.DbxTunePcsTablesSize;
import com.asetune.pcs.report.content.IReportEntry;
import com.asetune.pcs.report.content.IReportEntry.MessageType;
import com.asetune.pcs.report.content.RecordingInfo;
import com.asetune.pcs.report.content.ReportContent;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;
import com.asetune.utils.CountingWriter;
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
					entry.setExecStartTime();
//					long entryStartTime = System.currentTimeMillis();

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

					// Set how long it took
					entry.setExecEndTime();
//					entry.setExecTime(TimeUtils.msDiffNow(entryStartTime));
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
//		file.deleteOnExit(); // remove the file in removeOldReports() instead
//		Writer writer = new FileWriter(file);
//		try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) 
		try (Writer writer = new CountingWriter(Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8))) 
		{
			createHtml(writer);
		}

		// Create the "ShortMessage" used for email messages etc
//		StringWriter shortMessageWriter = new StringWriter();
		StringWriter shortMessageStringWriter = new StringWriter();
		Writer shortMessageWriter = new CountingWriter(shortMessageStringWriter);
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
		content.setShortMessage(shortMessageStringWriter.toString());
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
		addReportEntry( new DbxTunePcsTablesSize(this) );
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
		w.append("\n");
		w.append("        table { \n");
		w.append("            mso-table-layout-alt: fixed; \n"); // not sure about this - https://gist.github.com/webtobesocial/ac9d052595b406d5a5c1
		w.append("            mso-table-overlap: never; \n");    // not sure about this - https://gist.github.com/webtobesocial/ac9d052595b406d5a5c1
		w.append("            mso-table-wrap: none; \n");        // not sure about this - https://gist.github.com/webtobesocial/ac9d052595b406d5a5c1
//		w.append("            mso-cellspacing: 20px; \n");
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
		w.append("\n");
		w.append("<!--[if mso]> \n");
		w.append("        sparkline-td { \n");
//		w.append("            mso-width-alt: 1100; \n");
		w.append("            width: 1100!important; \n");
//		w.append("            white-space: nowrap; \n");
		w.append("        } \n");
		w.append("<![endif]--> \n");

//		// SQL Text -- for ALL Other
//		w.append("        .sqltext {                                \n");
//		w.append("        }                                         \n");
//
//		// SQL Text -- for Microsoft Outlook
//		w.append("<!--[if mso]> \n");
//		w.append("        .sqltext {                                \n");
//		w.append("            max-width: 500px;                     \n");
//		w.append("            margin: auto;                         \n");
//		w.append("            border: 3px solid #73AD21;            \n");
//		
//		w.append("            font-family: monospace;               \n");
//		w.append("            white-space: pre-wrap;                \n");
////		w.append("            display: block;                       \n");
////		w.append("            margin: 1em 0;                        \n");
//		w.append("        }                                         \n");
//		w.append("<![endif]--> \n");
//
//		w.append("        .sparklines-table th {                 \n");
////		w.append("            border: none;                         \n");
////		w.append("            font-size: 12px;                      \n");
////		w.append("            color: black;                         \n");
////		w.append("            background-color: transparent;        \n");
////		w.append("            border-bottom: 1px solid black;       \n");
////		w.append("            border-bottom: 1px dotted black;       \n");
//		w.append("        }                                         \n");
//		w.append("        .sparklines-table td {                 \n");
////		w.append("            border: none;                         \n");
//		w.append("        }                                         \n");
		
		
		w.append("\n");
		w.append("        .dsr-sub-table-chart {                    \n");
		w.append("            border: none;                         \n");
		w.append("        }                                         \n");
		w.append("        .dsr-sub-table-chart th {                 \n");
		w.append("            border: none;                         \n");
		w.append("            font-size: 12px;                      \n");
		w.append("            color: black;                         \n");
		w.append("            background-color: transparent;        \n");
//		w.append("            border-bottom: 1px solid black;       \n");
		w.append("            border-bottom: 1px dotted black;       \n");
		w.append("        }                                         \n");
		w.append("        .dsr-sub-table-chart td {                 \n");
		w.append("            border: none;                         \n");
		w.append("        }                                         \n");
		w.append("        .dsr-sub-table-chart tr:nth-child(odd) {  \n");
		w.append("            background-color: transparent;        \n");
		w.append("        }                                         \n");
		w.append("        .dsr-sub-table-chart tr:nth-child(even) { \n");
		w.append("            background-color: transparent;        \n");
		w.append("        }                                         \n");
		w.append("\n");
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
//		w.append("            background: #C0C0C0;   \n");
//		w.append("            font-size: 0.3em;      \n");
		w.append("            white-space: pre-wrap; \n"); // wrap long rows
		w.append("            width: 100%;           \n");
		w.append("            max-height: 400px;     \n");
		w.append("            overflow: auto;        \n");
		w.append("            margin-top: 0px;       \n");
		w.append("            margin-right: 0px;     \n");
		w.append("            margin-bottom: 0px;    \n");
		w.append("            margin-left: 0px;      \n");
		w.append("        }                          \n");
		w.append("        .dbx-xml-text-cell {        \n");
//		w.append("            background: #C0C0C0;   \n");
//		w.append("            font-size: 0.3em;      \n");
		w.append("            white-space: pre-wrap; \n"); // wrap long rows
		w.append("            width: 100%;           \n");
		w.append("            max-height: 200px;     \n");
		w.append("            overflow: auto;        \n");
		w.append("            margin-top: 0px;       \n");
		w.append("            margin-right: 0px;     \n");
		w.append("            margin-bottom: 0px;    \n");
		w.append("            margin-left: 0px;      \n");
		w.append("        }                          \n");
		w.append("        .dbx-sql-text-cell {        \n");
//		w.append("            background: #C0C0C0;   \n");
//		w.append("            font-size: 0.3em;      \n");
//		w.append("            white-space: pre-wrap; \n"); // wrap long rows
		w.append("            width: 100%;           \n");
//		w.append("            width: 400px;          \n");
		w.append("            max-height: 200px;     \n");
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
		w.append("        /* bootstrap collapsed related */ \n");
		w.append("        .card-header .fa {									\n");
		w.append("            transition: .3s transform ease-in-out;			\n");
		w.append("        }														\n");
		w.append("        .card-header .collapsed .fa {							\n");
		w.append("            transform: rotate(90deg);							\n");
		w.append("        }														\n");
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
		String reportBeginDateStr = "-unknown-start-time-";
		if (getReportBeginTime() != null)
		{
			reportBeginDateStr = TimeUtils.getCurrentTimeForFileNameYmd(getReportBeginTime().getTime());
		}

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

		// chart.js
		w.append("    <script src='https://cdnjs.cloudflare.com/ajax/libs/Chart.js/3.7.0/chart.min.js'   integrity='sha512-TW5s0IT/IppJtu76UbysrBH9Hy/5X41OTAbQuffZFU6lQ1rdcLHzpU5BzVvr/YFykoiMYZVWlr/PX1mDcfM9Qg==' crossorigin='anonymous' referrerpolicy='no-referrer'></script> \n");
//		w.append("    <script src='https://cdnjs.cloudflare.com/ajax/libs/Chart.js/3.7.0/chart.js'       integrity='sha512-uLlukEfSLB7gWRBvzpDnLGvzNUluF19IDEdUoyGAtaO0MVSBsQ+g3qhLRL3GTVoEzKpc24rVT6X1Pr5fmsShBg==' crossorigin='anonymous' referrerpolicy='no-referrer'></script> \n");
//old//		w.append("    <script src='https://cdnjs.cloudflare.com/ajax/libs/Chart.js/2.7.3/Chart.bundle.js' integrity='sha512-oaUGh3C8smdaT0kMeyQ7xS1UY60lko23ZRSnRljkh2cbB7GJHZjqe3novnhSNc+Qj21dwBE5dFBqhcUrFc9xIw==' crossorigin='anonymous' referrerpolicy='no-referrer'></script>\n");
//old//		w.append("    <script src='https://cdnjs.cloudflare.com/ajax/libs/Chart.js/2.7.3/Chart.js'        integrity='sha512-YB9sg4Z0/6+Q2qyde+om6RdPat0bLazJXJe15qHmZ9FjckJKxHOpHbp1mGTnHq7fzljiKbMEPiwHSLU2cX8qHA==' crossorigin='anonymous' referrerpolicy='no-referrer'></script>\n");

		// moment.js
		w.append("    <script src='https://cdnjs.cloudflare.com/ajax/libs/moment.js/2.29.1/moment.min.js'                             integrity='sha512-qTXRIMyZIFb8iQcfjXWCO8+M5Tbc38Qi5WzdPOYZHIlZpzBHG3L3by84BBBOiRGiEb7KKtAOAs5qYdUiZiQNNQ==' crossorigin='anonymous' referrerpolicy='no-referrer'></script> \n");
		w.append("    <script src='https://cdnjs.cloudflare.com/ajax/libs/chartjs-adapter-moment/1.0.0/chartjs-adapter-moment.min.js' integrity='sha512-oh5t+CdSBsaVVAvxcZKy3XJdP7ZbYUBSRCXDTVn0ODewMDDNnELsrG9eDm8rVZAQg7RsDD/8K3MjPAFB13o6eA==' crossorigin='anonymous' referrerpolicy='no-referrer'></script> \n");

		// font awesome
		w.append("    <link rel='stylesheet' href='https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.2.0/css/all.min.css' integrity='sha512-xh6O/CkQoPOWDdYTDqeRdPCVd1SpvCA9XXcUnZS2FmJNp1coAFzvtCN9BmamE+4aHK8yyUHUSCcJHgXloTyT2A==' crossorigin='anonymous' referrerpolicy='no-referrer' /> \n");

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

		w.append("\n");
		w.append("    <script type='text/javascript'>  \n");
		w.append("        function collapseSection(name) 			\n");
		w.append("        { 										\n");
		w.append("           console.log('collapseSection(): name=|' + name + '|.'); \n");
		w.append("           if (name === 'ALL') 					\n");
		w.append("           { 										\n");
		w.append("               $('.collapse').collapse('hide');	\n");
//		w.append("               $('#collapse_toc').collapse('show');	\n"); // But keep the TOC open
		w.append("           } 										\n");
		w.append("           else 									\n");
		w.append("           { 										\n");
		w.append("               $('#collapse_' + name).collapse('hide');	\n");
		w.append("               $('#heading_' + name)[0].scrollIntoView({ behavior: 'smooth', block: 'nearest' });	\n"); // Scroll to this HEADING otherwise it may be "confusing"
//		w.append("               document.getElementById('heading_' + name).scrollIntoView();	\n"); // Scroll to this HEADING otherwise it may be "confusing"
		w.append("           } 										\n");
		w.append("           return false; 							\n");
		w.append("        } 										\n");
		w.append("\n");
		w.append("        function expandSection(name) 				\n");
		w.append("        { 										\n");
		w.append("           console.log('expandSection(): name=|' + name + '|.'); \n");
		w.append("           if (name === 'ALL') 					\n");
		w.append("           { 										\n");
		w.append("               $('.collapse').collapse('show');	\n");
		w.append("           } 										\n");
		w.append("           else 									\n");
		w.append("           { 										\n");
		w.append("               $('#collapse_' + name).collapse('show');	\n");
		w.append("           } 										\n");
		w.append("           return false; 							\n");
		w.append("        } 										\n");
		w.append("    </script> \n");
		w.append("\n");

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


	public void createHtmlBody(Writer writer)
	throws IOException
	{
		writer.append("<body>\n");
//		sb.append("<body style'min-width: 100%'>\n");
//		sb.append("<body style'min-width: 2048px'>\n");
//		sb.append("<body style'min-width: 1024px'>\n");

		if (useBootstrap())
		{
			writer.append("<div class='container-fluid'> \n"); // BEGIN: Bootstrap 4 container
		}
		writer.append("\n");

		
		// Create an area where we can add/show progress bars
		writer.append("<div id='progress-area' style='background-color: white; position:fixed; top:50px; left:30px; width:100%; z-index: 9999;'>\n");
		writer.append("</div>\n");
		

		// Collapseable group div 
		if (useBootstrap())
		{
			writer.append("<div id='accordion' role='tablist' aria-multiselectable='true'> \n");			
		}
		
		// TOC HEADER
		if (useBootstrap())
		{
			// Bootstrap "card" - BEGIN
//			sb.append("<!--[if !mso]><!--> \n"); // BEGIN: IGNORE THIS SECTION FOR OUTLOOK
//			sb.append("<div id='toc' class='card border-dark mb-3'>");
//			sb.append("<h5 class='card-header'><b>Daily Summary Report for Servername: ").append(getServerName()).append("</b></h5>");
//			sb.append("<div class='card-body'>");
//			sb.append("<!--<![endif]-->    \n"); // END: IGNORE THIS SECTION FOR OUTLOOK
//			
//			sb.append("<!--[if mso]> \n"); // BEGIN: ONLY FOR OUTLOOK
//			sb.append("<h2>Daily Summary Report for Servername: ").append(getServerName()).append("</h2>\n");
//			sb.append("<![endif]-->  \n"); // END: ONLY FOR OUTLOOK

			// Bootstrap "card" - BEGIN
			writer.append("<!--[if !mso]><!--> \n"); // BEGIN: IGNORE THIS SECTION FOR OUTLOOK
			writer.append("<div class='card border-dark mb-3' id='toc'> \n");
			writer.append("<h5 class='card-header' role='tab' id='heading_toc'> \n");
			writer.append("  <a data-toggle='collapse' data-parent='#accordion' href='#collapse_toc' aria-expanded='true' aria-controls='collapse_toc' class='d-block'> \n");
			writer.append("    <i class='fa fa-chevron-down float-right'></i> \n");
			writer.append("    <b>").append("Daily Summary Report for Servername: ").append(getServerName()).append("</b> \n");
			writer.append("  </a> \n");
			writer.append("</h5> \n");
			writer.append("<div id='collapse_toc' class='collapse show' role='tabpanel' aria-labelledby='heading_toc'> \n");
			writer.append("<div class='card-body'> \n");
			writer.append("<!--<![endif]-->    \n"); // END: IGNORE THIS SECTION FOR OUTLOOK

			writer.append("<!--[if mso]> \n"); // BEGIN: ONLY FOR OUTLOOK
			writer.append("<h2>Daily Summary Report for Servername: ").append(getServerName()).append("</h2> \n");
			writer.append("<![endif]-->  \n"); // END: ONLY FOR OUTLOOK
		}
		else
		{
			// Normal HTML - H2 heading
			writer.append("<h2>Daily Summary Report for Servername: ").append(getServerName()).append("</h2>\n");
		}
		writer.append( createDbxCentralLink(true) );

		//--------------------------------------------------
		// TOC
		writer.append("<br> \n");
		writer.append("Links to Report Sections. \n");
		writer.append("<ul> \n");
		for (IReportEntry entry : _reportEntries)
		{
			String tocSubject = entry.getSubject();
			String tocDiv     = StringUtil.stripAllNonAlphaNum(tocSubject);

			// Strip off parts that may be details
			int firstLeftParentheses = tocSubject.indexOf("(");
			if (firstLeftParentheses != -1)
				tocSubject = tocSubject.substring(0, firstLeftParentheses - 1).trim();

			String liContent = "<a href='#" + tocDiv + "'>" + tocSubject + "</a>";
			
			writer.append("<li>").append(liContent).append("</li> \n");
		}
		writer.append("</ul> \n");
		writer.append("\n<br>");

		// TOC FOOTER
		if (useBootstrap())
		{
			// Bootstrap "card" - END
			writer.append("<a href='javascript:void(0);' onclick=\"collapseSection('ALL');\">Collapse</a> ");
			writer.append(" or \n");
			writer.append("<a href='javascript:void(0);' onclick=\"expandSection('ALL');\">Expand</a> ");
			writer.append(" ALL sections \n");

			writer.append("<!--[if !mso]><!--> \n"); // BEGIN: IGNORE THIS SECTION FOR OUTLOOK
			writer.append("</div> \n"); // end: card-body
			writer.append("</div> \n"); // end: collapse
			writer.append("</div> \n"); // end: card
			writer.append("<!--<![endif]-->    \n"); // END: IGNORE THIS SECTION FOR OUTLOOK
		}
		
//System.out.println("  ******* Used Memory " + Memory.getUsedMemoryInMB() + " MB ****** at createHtmlBody(): after TOC");


		//--------------------------------------------------
		// ALL REPORTS
		for (IReportEntry entry : _reportEntries)
		{
			// So we can gather some statistics
			entry.beginWriteEntry(writer, MessageType.FULL_MESSAGE);

			String tocSubject = entry.getSubject();
			String tocDiv     = StringUtil.stripAllNonAlphaNum(tocSubject);

			// Add a section header
			writer.append("\n");
			writer.append("\n");
			writer.append("<!-- ================================================================================= -->\n");
			writer.append("<!-- " + entry.getSubject()                                                        + " -->\n");
			writer.append("<!-- ================================================================================= -->\n");

			// Section HEADER
			if (useBootstrap())
			{
//				// Bootstrap "card" - BEGIN
//				sb.append("<!--[if !mso]><!--> \n"); // BEGIN: IGNORE THIS SECTION FOR OUTLOOK
//				sb.append("<div id='").append(tocDiv).append("' class='card border-dark mb-3'>");
//				sb.append("<h5 class='card-header'><b>").append(entry.getSubject()).append("</b></h5>");
//				sb.append("<div class='card-body'>");
//				sb.append("<!--<![endif]-->    \n"); // END: IGNORE THIS SECTION FOR OUTLOOK
//				
//				sb.append("<!--[if mso]> \n"); // BEGIN: ONLY FOR OUTLOOK
//				sb.append("<h2 id='").append(tocDiv).append("'>").append(entry.getSubject()).append("</h2> \n");
//				sb.append("<![endif]-->  \n"); // END: ONLY FOR OUTLOOK

				// Bootstrap "card" - BEGIN
				writer.append("<!--[if !mso]><!--> \n"); // BEGIN: IGNORE THIS SECTION FOR OUTLOOK
				writer.append("<div class='card border-dark mb-3' id='").append(tocDiv).append("'> \n");
				writer.append("<h5 class='card-header' role='tab' id='heading_").append(tocDiv).append("'> \n");
				writer.append("  <a data-toggle='collapse' data-parent='#accordion' href='#collapse_").append(tocDiv).append("' aria-expanded='true' aria-controls='collapse_").append(tocDiv).append("' class='d-block'> \n");
				writer.append("    <i class='fa fa-chevron-down float-right'></i> \n");
				writer.append("    <b>").append(entry.getSubject()).append("</b> \n");
				writer.append("  </a> \n");
				writer.append("</h5> \n");
				writer.append("<div id='collapse_").append(tocDiv).append("' class='collapse show' role='tabpanel' aria-labelledby='heading_").append(tocDiv).append("'> \n");
				writer.append("<div class='card-body'> \n");
				writer.append("<!--<![endif]-->    \n"); // END: IGNORE THIS SECTION FOR OUTLOOK

				writer.append("<!--[if mso]> \n"); // BEGIN: ONLY FOR OUTLOOK
				writer.append("<h2 id='").append(tocDiv).append("'>").append(entry.getSubject()).append("</h2> \n");
				writer.append("<![endif]-->  \n"); // END: ONLY FOR OUTLOOK
			}
			else
			{
				// Normal HTML - H2 heading
				writer.append("<h2 id='").append(tocDiv).append("'>").append(entry.getSubject()).append("</h2> \n");
			}

			if (entry.isEnabled())
			{
				try
				{
					entry.setCurrentMessageType(MessageType.FULL_MESSAGE);

					// Warning messages
					if (entry.hasWarningMsg())
						writer.append(entry.getWarningMsg());

					// Get the message text
					if ( ! entry.hasProblem() )
						entry.writeMessageText(writer, entry.getCurrentMessageType());
					
					// If the entry indicates that it has a problem... then print that.
					if ( entry.hasProblem() )
						writer.append(entry.getProblemText());
					
					// if we should append anything after an entry... Possibly '<br>\n'
					writer.append(entry.getEndOfReportText());

					// Notes for how to: Disable this entry
					if (entry.canBeDisabled())
					{
						writer.append("<br>");
						writer.append("<i>To disable this report entry, put the following in the configuration file. ");
						writer.append("<code>").append(entry.getIsEnabledConfigKeyName()).append(" = false</code></i><br>\n");
					}

					writer.append("<a href='javascript:void(0);' onclick=\"collapseSection('" + tocDiv + "');\">Collapse this section</a> <br>\n");
				}
				catch (RuntimeException rte)
				{
					writer.append("Problems 'writing' the HTML report text for section '" + entry.getSubject() + "'. Caught: " + rte + "\n");
					writer.append("Continuing with next report section... <br> \n");
					writer.append("Exception: <br> \n");
					writer.append("<pre><code> \n");
					writer.append(StringUtil.exceptionToString(rte));
					writer.append("</code></pre> \n");
				}
				finally
				{
					entry.setCurrentMessageType(null);
				}
			}
			else
			{
				String reason = entry.getDisabledReason();
				if (StringUtil.hasValue(reason))
				{
					// Entry is DISABLED
					writer.append("This entry is <b>disabled</b>, reason:<br>");
					writer.append(reason);
					writer.append("<br>");
				}
				else
				{
					// Entry is DISABLED
					writer.append("This entry is <b>disabled</b>, to enable it; put the following in the configuration file. ");
					writer.append("<code>").append(entry.getIsEnabledConfigKeyName()).append(" = false</code><br>");
				}
			}

			// Section FOOTER
			if (useBootstrap())
			{
				// Bootstrap "card" - END
				writer.append("<!--[if !mso]><!--> \n"); // BEGIN: IGNORE THIS SECTION FOR OUTLOOK
				writer.append("</div> \n"); // end: card-body
				writer.append("</div> \n"); // end: collapse
				writer.append("</div> \n"); // end: card
				writer.append("<!--<![endif]-->    \n"); // END: IGNORE THIS SECTION FOR OUTLOOK
			}

			// So we can gather some statistics
			entry.endWriteEntry(writer, MessageType.FULL_MESSAGE);
			
//System.gc();
//System.out.println("  ******* Used Memory " + Memory.getUsedMemoryInMB() + " MB ****** "+ entry.getClass().getSimpleName());
		}
		writer.append("\n<br>");

		//--------------------------------------------------
		// DEBUG - Write time it too to create each report entry
		printExecTimeReport(writer, MessageType.FULL_MESSAGE);

		
		// Collapseable group div 
		if (useBootstrap())
		{
			writer.append("</div> \n");			
		}

		//--------------------------------------------------
		// END
		writer.append("\n<br>");
		writer.append("\n<br>");
		writer.append("\n<code>--end-of-report--</code> \n");

		// Some static code for showing dialogs
		writer.append("\n");
		writer.append( createShowSqlTextDialogHtml() );
		writer.append( createShowSqlTextDialogJs()   );

		writer.append("\n");
		writer.append("</div> \n"); // END: Bootstrap 4 container
		writer.append("</body> \n");

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

			// So we can gather some statistics
			entry.beginWriteEntry(w, MessageType.SHORT_MESSAGE);

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
					entry.setCurrentMessageType(MessageType.SHORT_MESSAGE);

					// Warning messages
					if (entry.hasWarningMsg())
						w.append(entry.getWarningMsg());

					// Get the message text
//					if ( ! entry.hasProblem() )
//						entry.writeShortMessageText(w);
					if ( ! entry.hasProblem() )
						entry.writeMessageText(w, entry.getCurrentMessageType());
					
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
				finally
				{
					entry.setCurrentMessageType(null);
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

			
			// So we can gather some statistics
			entry.endWriteEntry(w, MessageType.SHORT_MESSAGE);

		}
		w.append("\n<br>");

		//--------------------------------------------------
		// DEBUG - Write time it too to create each report entry
		printExecTimeReport(w, MessageType.SHORT_MESSAGE);

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
	
	
	private void printExecTimeReport(Writer w, MessageType messageType) 
	throws IOException
	{
		String PROPKEY_printExecTime_fullMsg  = "DailySummaryReport.report.entry.enabled.printExecTime.fullMessage";
		String PROPKEY_printExecTime_shortMsg = "DailySummaryReport.report.entry.enabled.printExecTime.shortMessage";

		boolean printExecTime   = false;
		boolean printShortMsgKb = false;

		String propName = "";
		if (MessageType.FULL_MESSAGE.equals(messageType))
		{
			printExecTime   = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_printExecTime_fullMsg, true);
			printShortMsgKb = false; // SHORT Message "statistics" isn't yet available (it has not yet been created/written)
		}
		else if (MessageType.SHORT_MESSAGE.equals(messageType))
		{
			printExecTime   = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_printExecTime_shortMsg, true);
			printShortMsgKb = true; // both FULL and SHORT should be available now  
		}

		if (printExecTime)
		{
			String tocDiv = "sectionTime";
			String headingName = "Exec/Creation time for each Report Section";

			// Section HEADER
			if (useBootstrap())
			{
//				// Bootstrap "card" - BEGIN
//				w.append("<!--[if !mso]><!--> \n"); // BEGIN: IGNORE THIS SECTION FOR OUTLOOK
//				w.append("<div id='").append(tocDiv).append("' class='card border-dark mb-3'>");
//				w.append("<h5 class='card-header'><b>").append(headingName).append("</b></h5>");
//				w.append("<div class='card-body'>");
//				w.append("<!--<![endif]-->    \n"); // END: IGNORE THIS SECTION FOR OUTLOOK
//				
//				w.append("<!--[if mso]> \n"); // BEGIN: ONLY FOR OUTLOOK
//				w.append("<h2 id='").append(tocDiv).append("'>").append(headingName).append("</h2> \n");
//				w.append("<![endif]-->  \n"); // END: ONLY FOR OUTLOOK

				// Bootstrap "card" - BEGIN
				w.append("<!--[if !mso]><!--> \n"); // BEGIN: IGNORE THIS SECTION FOR OUTLOOK
				w.append("<div class='card border-dark mb-3' id='").append(tocDiv).append("'> \n");
				w.append("<h5 class='card-header' role='tab' id='heading_").append(tocDiv).append("'> \n");
				w.append("  <a data-toggle='collapse' data-parent='#accordion' href='#collapse_").append(tocDiv).append("' aria-expanded='true' aria-controls='collapse_").append(tocDiv).append("' class='d-block'> \n");
				w.append("    <i class='fa fa-chevron-down float-right'></i> \n");
				w.append("    <b>").append(headingName).append("</b> \n");
				w.append("  </a> \n");
				w.append("</h5> \n");
				w.append("<div id='collapse_").append(tocDiv).append("' class='collapse show' role='tabpanel' aria-labelledby='heading_").append(tocDiv).append("'> \n");
				w.append("<div class='card-body'> \n");
				w.append("<!--<![endif]-->    \n"); // END: IGNORE THIS SECTION FOR OUTLOOK

				w.append("<!--[if mso]> \n"); // BEGIN: ONLY FOR OUTLOOK
				w.append("<h2 id='").append(tocDiv).append("'>").append(headingName).append("</h2> \n");
				w.append("<![endif]-->  \n"); // END: ONLY FOR OUTLOOK
			}
			else
			{
				// Normal HTML - H2 heading
				w.append("<h2 id='").append(tocDiv).append("'>").append(headingName).append("</h2> \n");
			}

			SimpleDateFormat sdf_HMS = new SimpleDateFormat("HH:mm:ss");

			w.append("This is a DEBUG Section, just to see how long each Report Section takes. \n");
			w.append("<br> \n");
			w.append("<table class='sortable'> \n");

			w.append("<tr> \n");
			w.append("  <th>Section Name       </th> \n");
			w.append("  <th>Start Time HH:MM:SS</th> \n");
			w.append("  <th>Exec Time HH:MM:SS </th> \n");
			w.append("  <th>Full Msg KB        </th> \n");
			if (printShortMsgKb) 
				w.append("  <th>Short Msg KB   </th> \n");
			w.append("</tr> \n");
			
			long totalExecTime   = 0;
			long totalFullMsgKb  = 0;
			long totalShortMsgKb = 0;

			NumberFormat nf = NumberFormat.getInstance();

			for (IReportEntry entry : _reportEntries)
			{
				totalExecTime   += entry.getExecTime();
				totalFullMsgKb  += entry.getCharsWrittenKb(MessageType.FULL_MESSAGE)  < 0 ? 0 : entry.getCharsWrittenKb(MessageType.FULL_MESSAGE);
				totalShortMsgKb += entry.getCharsWrittenKb(MessageType.SHORT_MESSAGE) < 0 ? 0 : entry.getCharsWrittenKb(MessageType.SHORT_MESSAGE);

				String tocSubject    = entry.getSubject();
				String execTimeStr   = TimeUtils.msToTimeStrDHMS( entry.getExecTime() );
				String startTimeStr  = sdf_HMS.format( new Date(entry.getExecStartTime()) );
				String fullMsgKbStr  = nf.format(entry.getCharsWrittenKb(MessageType.FULL_MESSAGE));
				String shortMsgKbStr = nf.format(entry.getCharsWrittenKb(MessageType.SHORT_MESSAGE));

				// Strip off parts that may be details
				int firstLeftParentheses = tocSubject.indexOf("(");
				if (firstLeftParentheses != -1)
					tocSubject = tocSubject.substring(0, firstLeftParentheses - 1).trim();

				// Add the row
				w.append("<tr> \n");
				w.append("   <td>"               + tocSubject    + "</td> \n");
				w.append("   <td>"               + startTimeStr  + "</td> \n");
				w.append("   <td>"               + execTimeStr   + "</td> \n");
				w.append("   <td align='right'>" + fullMsgKbStr  + "</td> \n");
				if (printShortMsgKb) 
					w.append("   <td align='right'>" + shortMsgKbStr + "</td> \n");
				w.append("</tr> \n");
			}

			// SUMMARY Time
			w.append("<tr> \n");
			w.append("   <td><b>Summary</b></td> \n");
			w.append("   <td></td> \n");
			w.append("   <td><b>" + TimeUtils.msToTimeStrDHMS( totalExecTime )   + "</b></td> \n");
			w.append("   <td align='right'><b>"     + nf.format(totalFullMsgKb)  + "</b></td> \n");
			if (printShortMsgKb) 
				w.append("   <td align='right'><b>" + nf.format(totalShortMsgKb) + "</b></td> \n");
			w.append("</tr> \n");

			w.append("</table> \n");
			w.append("\n<br>");
			w.append("<i>To disable this report entry, put the following in the configuration file. <code>" + propName + " = false</code></i> <br> \n");

			if (MessageType.FULL_MESSAGE.equals(messageType))
				w.append("<a href='javascript:void(0);' onclick=\"collapseSection('" + tocDiv + "');\">Collapse this section</a> <br>\n");

			// Section FOOTER
			if (useBootstrap())
			{
				// Bootstrap "card" - END
				w.append("<!--[if !mso]><!--> \n"); // BEGIN: IGNORE THIS SECTION FOR OUTLOOK
				w.append("</div> \n"); // end: card-body
				w.append("</div> \n"); // end: collapse
				w.append("</div> \n"); // end: card
				w.append("<!--<![endif]-->    \n"); // END: IGNORE THIS SECTION FOR OUTLOOK
			}
		}
	}
}
