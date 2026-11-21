/*******************************************************************************
 * Copyright (C) 2010-2027 Goran Schwarz
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
package com.dbxtune.pcs.report;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.central.DbxTuneCentral;
import com.dbxtune.pcs.PersistWriterToHttpJson;
import com.dbxtune.pcs.report.IProgressReporter.State;
import com.dbxtune.pcs.report.content.AlarmsActive;
import com.dbxtune.pcs.report.content.AlarmsHistory;
import com.dbxtune.pcs.report.content.DailySummaryReportContent;
import com.dbxtune.pcs.report.content.DbxTuneCmRefreshInfo;
import com.dbxtune.pcs.report.content.ShowplanLinkBuilder;
import com.dbxtune.pcs.report.content.DbxTuneErrors;
import com.dbxtune.pcs.report.content.DbxTunePcsTablesSize;
import com.dbxtune.pcs.report.content.IReportEntry;
import com.dbxtune.pcs.report.content.IReportEntry.IssueLevel;
import com.dbxtune.pcs.report.content.IReportEntry.MessageType;
import com.dbxtune.pcs.report.content.RecordingInfo;
import com.dbxtune.pcs.report.content.ReportStatus;
import com.dbxtune.pcs.report.content.ReportContent;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.CountingWriter;
import com.dbxtune.utils.HeartbeatMonitor;
import com.dbxtune.utils.HtmlUtils;
import com.dbxtune.utils.SpaceFullPredictor;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.TimeUtils;

public class DailySummaryReportDefault
extends DailySummaryReportAbstract
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private List<IReportEntry> _reportEntries = new ArrayList<>();

	public boolean useBootstrap()                              { return true; }	
	public boolean isCollapsedHeaderForTocSection()            { return false; }
//	public boolean isCollapsedHeaderForExecTimeReportSection() { return false; }
	public boolean isCollapsedHeaderForExecTimeReportSection() { return true; }
	public boolean createTocSection()                          { return true; }
	public boolean createExecTimeReportSection()               { return true; }
	public String  getReportName()                             { return "Daily Summary Report"; }
	
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
//		_logger.info("Initiated " + getReportName() + " with " + _reportEntries.size() + " report entries.");

		// Get Configuration possibly from the DbxCentral
//		Configuration pcsSavedConf = getConfigFromDbxCentral(getServerName());
		Configuration pcsSavedConf = getConfigFromPcs();
		Configuration localConf    = Configuration.getCombinedConfiguration();
		
		// Iterate all entries and create the report
		int entryCount = getReportEntries().size();
		int count = 0;
		IProgressReporter progressReporter = getProgressReporter();
		for (IReportEntry entry : getReportEntries())
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
		

		// Create the "MinimalMessage" used for email messages etc
		StringWriter minimalMessageStringWriter = new StringWriter();
		Writer minimalMessageWriter = new CountingWriter(minimalMessageStringWriter);
		createMinimalMessage(minimalMessageWriter);
		
		
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

		// Set Minimal Message in content
		content.setMinimalMessage(minimalMessageStringWriter.toString());
		content.setMinimalMessageOfHtml(true);

		content.setIssueLevel( getIssueLevel() );

		// set the content
		setReportContent(content);
	}

	public void addReportEntry(IReportEntry entry)
	{
		_reportEntries.add(entry);
	}

	public IReportEntry getReportEntry(Class<?> classToReturn)
	{
		for (IReportEntry entry : getReportEntries())
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
		addReportEntry( new ReportStatus(this)  );
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
		addReportEntry( new DbxTuneErrors       (this).withCollapsedHeader(true) );
		addReportEntry( new DbxTunePcsTablesSize(this).withCollapsedHeader(true) );
		addReportEntry( new DbxTuneCmRefreshInfo(this).withCollapsedHeader(true) );
	}

	@Override
	public List<IReportEntry> getReportEntries()
	{
		return _reportEntries;
	}

	@Override
	public RecordingInfo getReportEntryRecordingInfo()
	{
		for (IReportEntry entry : getReportEntries())
		{
			if (entry instanceof RecordingInfo)
				return (RecordingInfo) entry;
		}
		return null;
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
			    + "from [" + tabName + "] \n"
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
			_logger.warn("Problems getting values from '" + tabName + "': " + ex);
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
	 * The level of the whole report: the highest level of all ReportEntries (NONE = Nothing To Report)
	 */
	public IssueLevel getIssueLevel()
	{
		IssueLevel level = IssueLevel.NONE;
		for (IReportEntry entry : getReportEntries())
		{
			IssueLevel entryLevel = getIssueLevel(entry);
			if (entryLevel.compareTo(level) > 0)
				level = entryLevel;
		}
		return level;
	}

	/**
	 * The level of one ReportEntry, NONE if the entry is disabled or throws
	 */
	public static IssueLevel getIssueLevel(IReportEntry entry)
	{
		try
		{
			if ( ! entry.isEnabled() )
				return IssueLevel.NONE;

			IssueLevel level = entry.getIssueLevel();
			return level == null ? IssueLevel.NONE : level;
		}
		catch (RuntimeException rte)
		{
			// This may happen if the ReportEntry has problems (for example NullPointerException in getIssueLevel()...
			_logger.warn("Problems checking getIssueLevel() for ReportEntry '" + entry.getClass().getSimpleName() + "'. Caught RuntimeException, using level NONE for this entry.", rte);
			return IssueLevel.NONE;
		}
	}

	/** " [FYI]" or " [ISSUE]" badge for the TOC and the section headers, "" for level NONE */
	private static String getIssueLevelBadge(IReportEntry entry)
	{
		IssueLevel level = getIssueLevel(entry);
		return level == IssueLevel.NONE ? "" : " " + ReportStatus.levelBadge(level);
	}


	/**
	 * Mail: the tooltip texts (Table Info, Triggers...) each ONCE, as a CSS rule: {@code .dsr-tt-17:hover::before { content: "..." }}.
	 * The elements only have the class (see ReportEntryAbstract.getTextAsTooltipDiv). Same CSS (::before) as 'data-tooltip', so it
	 * shows in the same mail readers. The full report has the texts as hidden elements instead, see createHtmlBody().
	 */
	private void writeTooltipTextsAsCss(Writer w)
	throws IOException
	{
		if (getTooltipTexts().isEmpty())
			return;

		w.append("<style type='text/css'> \n");
		for (java.util.Map.Entry<String, Integer> e : getTooltipTexts().entrySet())
		{
			// CSS string: escape \ and ", a new line is \A (the rule has 'white-space: pre-wrap'), '<' as \3C so it never ends the <style>
			StringBuilder css = new StringBuilder();
			for (char c : e.getKey().toCharArray())
			{
				if      (c == '\\') css.append("\\\\");
				else if (c == '"')  css.append("\\\"");
				else if (c == '\n') css.append("\\A ");
				else if (c == '<')  css.append("\\3C ");
				else if (c != '\r') css.append(c);
			}
			w.append(".dsr-tt-").append(e.getValue().toString()).append(":hover::before { content: \"").append(css).append("\"; } \n");
		}
		w.append("</style> \n");
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
		w.append("            font-family: \"Segoe UI\", Arial, Helvetica, sans-serif; \n");
		w.append("            color: #1d2430; \n");
		w.append("            font-size: 14px; \n");
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
		w.append("            border: 0; \n"); // also over the border='1' attribute: one table style, thin row lines and no grid
		w.append("            font-size: 13px; \n");
		w.append("        } \n");
		w.append("        th { \n");
		w.append("            border: 0; \n");
		w.append("            border-bottom: 1px solid #d5dbe4; \n");
		w.append("            text-align: left; \n");
		w.append("            padding: 3px 6px; \n");
		w.append("            white-space: nowrap; \n");
		w.append("            background-color: #eef1f5; \n");
		w.append("            color: #3a4556; \n");
		w.append("            font-weight: 600; \n");
		w.append("        } \n");
		w.append("        td { \n");
		w.append("            border: 0; \n");
		w.append("            border-bottom: 1px solid #edf0f4; \n");
		w.append("        /*  text-align: left; */ \n");
		w.append("            padding: 2px 6px; \n");
		w.append("            white-space: nowrap; \n");
		w.append("        } \n");
		w.append("        tr:nth-child(odd) { \n");
		w.append("            background-color: white; \n"); // Otherwise it will be transparent (and bootstrap will write "right line" of a box in the table)
		w.append("        } \n");
		w.append("        tr:nth-child(even) { \n");
		w.append("            background-color: #f9fafb; \n");
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
		w.append("        h4 { \n");
		w.append("            border-bottom: 1px solid black; \n");
//		w.append("            border-top: 1px solid black; \n");
		w.append("            margin-bottom: 3px; \n");
		w.append("        } \n");
		w.append("        h5 { \n");
		w.append("            border-bottom: 1px solid black; \n");
//		w.append("            border-top: 1px solid black; \n");
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
		w.append("        /* Display the 'max/min' indicator value as text 'over' the sparkline mini-chart */ \n");
		w.append("        .sparkline-ind-val {					\n"); // write on top of 'sparkline-wrapper' where the real sparkline chart is located
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
		w.append("        [data-tooltip], .dsr-tt {				\n"); // .dsr-tt: the text is written once, see getTooltipTexts()
		w.append("            position: relative;				\n");
//		sb.append("            cursor: help;					\n");
		w.append("        }									\n");
		w.append("        										\n");
		w.append("        /* 'tooltip' CSS settings for SQL Text... */ \n");
		w.append("        [data-tooltip]:hover::before, .dsr-tt:hover::before { \n");
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
				_logger.error("Problems reading External HTML HEAD File '" + f.getAbsolutePath() + "', Skipping this and using System provided header. Caught: " + ex, ex);
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
				// Reads scripts/bootstrap/4.5.3/css/bootstrap.css -- Which is copied into bootstrap_453.css at com.dbxtune.pcs.report.content.bootstrap_453.css
//				bootstrapCss = com.dbxtune.utils.FileUtils.readFile(ReportContent.class, "bootstrap_453.css");

				// Reads scripts/bootstrap/4.6.2/css/bootstrap.css -- Which is copied into bootstrap_462.css at com.dbxtune.pcs.report.content.bootstrap_462.css
//				bootstrapCss = com.dbxtune.utils.FileUtils.readFile(ReportContent.class, "bootstrap_462.css");

				// Reads scripts/bootstrap/5.3.8/css/bootstrap.min.css -- Which is copied into bootstrap_538.css at com.dbxtune.pcs.report.content.bootstrap_538.css
				bootstrapCss = com.dbxtune.utils.FileUtils.readFile(ReportContent.class, "bootstrap_538.css");
			}
			catch (Exception ex)
			{
				_logger.error("Problems reading file 'bootstrap_538.css'. Caught: " + ex, ex);
				bootstrapCss = "/* Problems reading file 'bootstrap_538.css'. Caught: " + ex + " */";
			}

			// Write bootstrap CSS -- but invisible for Outlook
//			w.append(" \n");
//			w.append("<!--[if !mso]><!--> \n"); // BEGIN: IGNORE THIS SECTION FOR OUTLOOK

			w.append("<style type='text/css'> \n");
			w.append(bootstrapCss);
			w.append("</style> \n");

			w.append(SpaceFullPredictor.getHtmlCss());
			
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
		
//		w.append("    <link rel='stylesheet' href='https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/themes/prism-okaidia.min.css'> \n");
//		w.append("    <link rel='stylesheet' href='https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/plugins/line-numbers/prism-line-numbers.min.css'> \n");

		// In local the 'prism-line-numbers.min.css' and 'prism-okaidia.min.css' is included in '/scripts/prism/prism-1.30.0.css'
		addCssFile(w, "/scripts/prism/prism-1.30.0.css", "https://cdnjs.cloudflare.com/ajax/libs/prism/1.30.0/plugins/line-numbers/prism-line-numbers.min.css"
		                                                ,"https://cdnjs.cloudflare.com/ajax/libs/prism/1.30.0/themes/prism-okaidia.min.css");

		createDbxTuneCss(w);

//		w.append("    <SCRIPT src='http://www.dbxtune.com/sorttable.js'></SCRIPT> \n");
//		w.append("    <SCRIPT src='/scripts/sorttable/sorttable.js'></SCRIPT> \n");
		addJavaScriptFile(w, "/scripts/sorttable/sorttable.js", "http://www.dbxtune.com/sorttable.js");

//		w.append("    <SCRIPT src='https://cdnjs.cloudflare.com/ajax/libs/jquery/3.7.1/jquery.min.js'></SCRIPT> \n");
//		w.append("    <SCRIPT src='https://cdnjs.cloudflare.com/ajax/libs/jquery-sparklines/2.1.2/jquery.sparkline.min.js'></SCRIPT> \n");
		addJavaScriptFile(w, "/scripts/jquery/jquery-3.7.1.min.js"                     , "https://cdnjs.cloudflare.com/ajax/libs/jquery/3.7.1/jquery.min.js");
		addJavaScriptFile(w, "/scripts/jquery-sparklines/2.1.2/jquery.sparkline.min.js", "https://cdnjs.cloudflare.com/ajax/libs/jquery-sparklines/2.1.2/jquery.sparkline.min.js");
		
		if (useBootstrap())
		{
//			w.append("    <script src='https://cdn.jsdelivr.net/npm/popper.js@1.16.1/dist/umd/popper.min.js'      integrity='sha384-9/reFTGAW83EW2RDu2S0VKaIzap3H66lZH81PoYlFhbGU+6BZp6G7niu735Sk7lN' crossorigin='anonymous'></script> \n");
//			w.append("    <script src='https://cdn.jsdelivr.net/npm/bootstrap@4.5.3/dist/js/bootstrap.min.js'     integrity='sha384-w1Q4orYjBQndcko6MimVbzY0tgp4pWB4lZ7lr30WKz0vr/aWKhXdBNmNb5D92v7s' crossorigin='anonymous'></script> \n");
//			addJavaScriptFile(w, "/scripts/bootstrap/4.5.3/js/bootstrap.bundle.min.js", "https://cdn.jsdelivr.net/npm/bootstrap@4.5.3/dist/js/bootstrap.bundle.min.js");
			addJavaScriptFile(w, "/scripts/bootstrap/5.3.8/js/bootstrap.bundle.min.js", "https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/js/bootstrap.bundle.min.js");
		}

		// In local the 'prism.min.js' and 'components/prism-sql.min.js', 'plugins/line-numbers/prism-line-numbers.min.js'.
		// Is included in '/scripts/prism/prism-1.30.0.js'
//		w.append("    <script src='https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/prism.min.js'></script> \n");
//		w.append("    <script src='https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/components/prism-sql.min.js'></script> \n");
//		w.append("    <script src='https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/plugins/line-numbers/prism-line-numbers.min.js'></script> \n");
		addJavaScriptFile(w, "/scripts/prism/prism-1.30.0.js", "https://cdnjs.cloudflare.com/ajax/libs/prism/1.30.0/prism.min.js"
		                                                      ,"https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/components/prism-sql.min.js"
		                                                      ,"https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/plugins/line-numbers/prism-line-numbers.min.js");

		// chart.js
//		w.append("    <script src='https://cdnjs.cloudflare.com/ajax/libs/Chart.js/3.7.0/chart.min.js'   integrity='sha512-TW5s0IT/IppJtu76UbysrBH9Hy/5X41OTAbQuffZFU6lQ1rdcLHzpU5BzVvr/YFykoiMYZVWlr/PX1mDcfM9Qg==' crossorigin='anonymous' referrerpolicy='no-referrer'></script> \n");
		addJavaScriptFile(w, "/scripts/chartjs/4.5.1/dist/chart.umd.min.js", "https://cdnjs.cloudflare.com/ajax/libs/Chart.js/4.5.1/chart.umd.min.js");

		// chart.js plugins
//		w.append("    <script src='https://cdnjs.cloudflare.com/ajax/libs/chartjs-plugin-annotation/2.2.1/chartjs-plugin-annotation.min.js' integrity='sha512-qF3T5CaMgSRNrxzu69V3ZrYGnrbRMIqrkE+OrE01DDsYDNo8R1VrtYL8pk+fqhKxUBXQ2z+yV/irk+AbbHtBAg==' crossorigin='anonymous' referrerpolicy='no-referrer'></script> \n");
//		addJavaScriptFile(w, "/scripts/chartjs/chartjs-plugin-annotation-2.2.1/package/dist/chartjs-plugin-annotation.min.js", "https://cdnjs.cloudflare.com/ajax/libs/chartjs-plugin-annotation/2.2.1/chartjs-plugin-annotation.min.js");
		addJavaScriptFile(w, "/scripts/chartjs-plugin-annotation/3.1.0/chartjs-plugin-annotation.min.js", "https://cdnjs.cloudflare.com/ajax/libs/chartjs-plugin-annotation/3.1.0/chartjs-plugin-annotation.min.js");


		// moment.js
//		w.append("    <script src='https://cdnjs.cloudflare.com/ajax/libs/moment.js/2.29.1/moment.min.js'                             integrity='sha512-qTXRIMyZIFb8iQcfjXWCO8+M5Tbc38Qi5WzdPOYZHIlZpzBHG3L3by84BBBOiRGiEb7KKtAOAs5qYdUiZiQNNQ==' crossorigin='anonymous' referrerpolicy='no-referrer'></script> \n");
//		w.append("    <script src='https://cdnjs.cloudflare.com/ajax/libs/chartjs-adapter-moment/1.0.0/chartjs-adapter-moment.min.js' integrity='sha512-oh5t+CdSBsaVVAvxcZKy3XJdP7ZbYUBSRCXDTVn0ODewMDDNnELsrG9eDm8rVZAQg7RsDD/8K3MjPAFB13o6eA==' crossorigin='anonymous' referrerpolicy='no-referrer'></script> \n");
		addJavaScriptFile(w, "/scripts/moment/2.31.0/moment.min.js"                              , "https://cdnjs.cloudflare.com/ajax/libs/moment.js/2.31.0/moment.min.js");
		addJavaScriptFile(w, "/scripts/chartjs-adapter-moment/1.0.1/chartjs-adapter-moment.min.js", "https://cdnjs.cloudflare.com/ajax/libs/chartjs-adapter-moment/1.0.1/chartjs-adapter-moment.min.js");

		// font awesome
//		w.append("    <link rel='stylesheet' href='https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.2.0/css/all.min.css' integrity='sha512-xh6O/CkQoPOWDdYTDqeRdPCVd1SpvCA9XXcUnZS2FmJNp1coAFzvtCN9BmamE+4aHK8yyUHUSCcJHgXloTyT2A==' crossorigin='anonymous' referrerpolicy='no-referrer' /> \n");
		addCssFile(w, "/scripts/font-awesome/7.3.1/css/all.min.css", "https://cdn.jsdelivr.net/npm/@fortawesome/fontawesome-free@7.3.1/css/all.min.css");
		w.append("<style type='text/css'> :root { --fa-width: auto; } </style> \n"); // Font Awesome 7: no fixed icon width (as in FA 6)

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
		w.append("\n");
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


	// TODO: BEGIN - Possibly move this to DailySummaryReportAbstract
	public enum AddMethodForJsAndCss
	{
		EMBEDDED, LOCAL_FILE, REMOTE_URL
	};
	public AddMethodForJsAndCss getAddMethodForJsAndCss()
	{
		return AddMethodForJsAndCss.LOCAL_FILE;
	}
	public String getEmbeddedBasePath()
	{
		return DbxTuneCentral.getAppWebDir();
	}
	
	public void addCssFile(Writer w, String localUrl, String... remoteUrl)
	throws IOException
	{
//		AddMethodForJsAndCss addMethod = getAddMethodForJsAndCss();
		
//		// Embedding needs the "localUrl"
//		if (StringUtil.isNullOrBlank(localUrl))
//		{
//			// LOCAL_FILE needs: localUrl
//			if (AddMethodForJsAndCss.LOCAL_FILE.equals(addMethod))
//				return;
//
//			// EMBEDDED needs: localUrl
//			if (AddMethodForJsAndCss.EMBEDDED.equals(addMethod))
//				return;
//		}

		// LOCAL_FILE or REMOTE_URL
		if ( ! AddMethodForJsAndCss.EMBEDDED.equals(getAddMethodForJsAndCss()) )
		{
//			String url = AddMethodForJsAndCss.LOCAL_FILE.equals(getAddMethodForJsAndCss()) ? localUrl : remoteUrl;
//
//			w.append("    <link rel='stylesheet' type='text/css' href='" + url + "' title='Style'> \n");

			HtmlUtils.createCssLinkTag(w, localUrl, remoteUrl);
		}
		else // EMBEDDED
		{
			// Embedding needs the "localUrl"
			if (StringUtil.isNullOrBlank(localUrl))
				return;

			Path path = Path.of(getEmbeddedBasePath(), localUrl);

			w.append("\n");
			w.append("<!-- BEGIN: EMBEDDING local CSS file '" + localUrl + "' --> \n");
			w.append("<style> \n");
//			w.append(Files.readString(path));
			try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) 
			{
				reader.transferTo(w);
			}
			w.append("\n");
			w.append("</style> \n");
			w.append("<!-- END: EMBEDDING local CSS file '" + localUrl + "' --> \n");
			w.append("\n");
		}
	}

	public void addJavaScriptFile(Writer w, String localUrl, String... remoteUrl)
	throws IOException
	{
//		AddMethodForJsAndCss addMethod = getAddMethodForJsAndCss();
		
//		// Embedding needs the "localUrl"
//		if (StringUtil.isNullOrBlank(localUrl))
//		{
//			// LOCAL_FILE needs: localUrl
//			if (AddMethodForJsAndCss.LOCAL_FILE.equals(addMethod))
//				return;
//
//			// EMBEDDED needs: localUrl
//			if (AddMethodForJsAndCss.EMBEDDED.equals(addMethod))
//				return;
//		}

		if ( ! AddMethodForJsAndCss.EMBEDDED.equals(getAddMethodForJsAndCss()) )
		{
//			String url = AddMethodForJsAndCss.LOCAL_FILE.equals(addMethod) ? localUrl : remoteUrl;
//
//			w.append("    <SCRIPT src='" + url + "' type='text/javascript'></SCRIPT> \n");

			HtmlUtils.createJsScriptTag(w, localUrl, remoteUrl);
		}
		else // EMBEDDED
		{
			Path path = Path.of(getEmbeddedBasePath(), localUrl);

			w.append("\n");
			w.append("<!-- BEGIN: EMBEDDING local JS file '" + localUrl + "' --> \n");
			w.append("<script> \n");
//			w.append(Files.readString(path));
			try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) 
			{
				reader.transferTo(w);
			}
			w.append("\n");
			w.append("</script> \n");
			w.append("<!-- END: EMBEDDING local JS file '" + localUrl + "' --> \n");
			w.append("\n");

			throw new RuntimeException("NOT-YET-IMPLEMENTED");
		}
	}
	// TODO: END - Possibly move this to DailySummaryReportAbstract


	/**
	 * Overrode this if you want to create a navbar
	 * @param writer
	 */
	public void createHtmlNavbar(Writer writer)
	throws IOException
	{
	}

	/**
	 * Should we write the "Hold on: Loading page from server" banner at the top of the page?<br>
	 * Override this and return false if you do NOT want the banner.
	 */
	public boolean createPageLoadingBanner()
	{
		return true;
	}

	/**
	 * Write the <b>progress area</b>: the ONE "sticky" section at the top of the page where <b>all</b>
	 * load-progress information is presented, one row per activity:
	 * <ul>
	 *   <li><code>Hold on: Loading page from server... NN%</code> - while the HTML is transferred and
	 *       parsed. Written here, and removed again on <code>DOMContentLoaded</code>.</li>
	 *   <li><code>Initializing Sparklines:</code>  - {@link com.dbxtune.pcs.report.content.SparklineHelper}
	 *       moves its own progress div in here.</li>
	 *   <li><code>Initializing Chart Info :</code> - {@link com.dbxtune.pcs.report.content.ReportChartAbstract}
	 *       moves its own progress div in here.</li>
	 * </ul>
	 * NOTE: <code>position: sticky</code> (NOT <code>fixed</code>) so the area is part of the normal page
	 *       flow: it sits <b>above</b> the report and pushes it down, instead of covering the
	 *       "<i>... for Servername: XXX</i>" heading. It still stays at the top when scrolling.
	 * <p>
	 * NOTE: ALL the visual styling (background/border/padding) is on the <b>child rows</b>, never on the
	 *       area itself. So when every row is hidden or removed, the area collapses to zero height and
	 *       nothing at all is shown.
	 */
	public void createProgressAreaHtml(Writer w)
	throws IOException
	{
		w.append("\n");
		w.append("<!-- ================================================================================== -->\n");
		w.append("<!-- == Progress area: 'Loading page from server', 'Initializing Sparklines', etc.   == -->\n");
		w.append("<!-- == One sticky section at the top. Only visible when JavaScript is enabled.      == -->\n");
		w.append("<!-- ================================================================================== -->\n");
		w.append("<!--[if !mso]><!--> \n"); // BEGIN: IGNORE THIS SECTION FOR OUTLOOK
		w.append("<style type='text/css'> \n");
		w.append("    #progress-area                 { position: sticky; top: 0; z-index: 10050; display: flex; flex-direction: column; } \n");
		// Fixed row order. Without this the order is decided by WHICH loader happens to move its row
		// in here first (they appendTo() the area), which varies with the report content.
		w.append("    #progress-area #dsr-page-loading       { order: 1; } \n");
		w.append("    #progress-area #chartJs-progress-div   { order: 2; } \n");
		w.append("    #progress-area #sparkline-progress-div { order: 3; } \n");
		w.append("    #progress-area > div           { background-color: #fff3cd; color: #856404; border-bottom: 1px solid #ffeeba; \n");
		w.append("                                     padding: 6px 12px; text-align: center; font-size: 14px; } \n");
		w.append("    #progress-area label, \n");
		w.append("    #progress-area #dsr-pl-text    { display: inline-block; vertical-align: middle; margin: 0 8px 0 0; } \n");
		// The 'Loading page from server' bar is a plain div (it must work before jQuery/Bootstrap has
		// loaded), the other two are <progress> elements -- style them identically so the rows match.
		w.append("    #progress-area progress, \n");
		w.append("    #progress-area .dsr-pl-bar     { display: inline-block; vertical-align: middle; width: 50%; height: 20px; \n");
		w.append("                                     -webkit-appearance: none; appearance: none; \n");
		w.append("                                     background-color: #fff; border: 1px solid #e0c878; border-radius: 3px; overflow: hidden; } \n");
		w.append("    #progress-area #dsr-pl-fill                     { height: 100%; width: 0%; background-color: #856404; transition: width 0.2s linear; } \n");
		w.append("    #progress-area progress::-webkit-progress-bar   { background-color: #fff; } \n");
		w.append("    #progress-area progress::-webkit-progress-value { background-color: #856404; } \n");
		w.append("    #progress-area progress::-moz-progress-bar      { background-color: #856404; } \n");
		// The 'Stop' buttons come in as 'btn btn-primary btn-sm' -- a blue button on an amber banner.
		// Re-colour them to the same dark amber as the progress bar fill, so they belong to the banner
		// but still read clearly as a button.
		// An #id selector outranks Bootstrap's single-class rules, so no !important is needed.
		w.append("    #progress-area button          { padding: 1px 10px; font-size: 12px; line-height: 18px; \n");
		w.append("                                     color: #fff; background-color: #856404; border: 1px solid #6b4f03; border-radius: 3px; } \n");
		w.append("    #progress-area button:hover    { background-color: #6b4f03; } \n");
		w.append("</style> \n");
		w.append("<!--<![endif]-->    \n"); // END: IGNORE THIS SECTION FOR OUTLOOK
		w.append("\n");

		w.append("<div id='progress-area'> \n");

		// First row: "Hold on: Loading page from server..." -- removed again when the page is loaded
		if (createPageLoadingBanner())
			createPageLoadingBannerHtml(w, getReportEntries().size());

		w.append("</div> \n");
		w.append("\n");
	}

	/**
	 * Write the "Hold on: Loading page from server" row, which goes <b>inside</b> the progress area
	 * written by {@link #createProgressAreaHtml(Writer)}. It tells the user that the page is still
	 * being transferred from the server. (A big report on a slow network takes a while...)
	 * <p>
	 * How it works:
	 * <ul>
	 *   <li>The browser renders the HTML <i>while</i> it is streaming in, so a banner at the top of the body shows up early.</li>
	 *   <li>After every report section we write a tiny script tag that calls <code>dsrPageLoadProgress(#)</code>,
	 *       which the parser executes as soon as it reaches it -- that gives us a percentage for free.</li>
	 *   <li>On <code>DOMContentLoaded</code> (== the full page has been received and parsed) the banner is removed.</li>
	 * </ul>
	 * NOTE: The div is written as <code>display:none</code> and is made visible <b>by JavaScript</b>.
	 *       So in a mail reader (or any browser where JavaScript is disabled) it will <b>never</b> be visible,
	 *       and the page looks exactly as it did before. It is also wrapped in Outlook conditional
	 *       comments, just like the rest of this report.
	 * <p>
	 * NOTE: Plain JavaScript is used here (no jQuery/Bootstrap), since the libraries loaded in the
	 *       head section may very well still be downloading when this is executed.
	 *
	 * @param w             Where to write the HTML
	 * @param sectionCount  Number of report sections (used when calculating the percentage)
	 */
	public void createPageLoadingBannerHtml(Writer w, int sectionCount)
	throws IOException
	{
		w.append("<!--[if !mso]><!--> \n"); // BEGIN: IGNORE THIS SECTION FOR OUTLOOK
		w.append("<div id='dsr-page-loading' style='display: none;'> \n");
		w.append("    <span id='dsr-pl-text'>Hold on: Loading page from server...</span> \n");
		w.append("    <div class='dsr-pl-bar'><div id='dsr-pl-fill'></div></div> \n");
		w.append("</div> \n");
		w.append("<script type='text/javascript'> \n");
		w.append("    var dsrPageLoadTotal = " + sectionCount + "; \n");
		w.append("\n");
		w.append("    // Make the banner visible. \n");
		w.append("    // This is what keeps it INVISIBLE when JavaScript is disabled (mail readers etc). \n");
		w.append("    document.getElementById('dsr-page-loading').style.display = 'block'; \n");
		w.append("\n");
		w.append("    // Called from a small script tag written after every report section, while the page is streaming in \n");
		w.append("    function dsrPageLoadProgress(sectionsDone) \n");
		w.append("    { \n");
		w.append("        var pct  = dsrPageLoadTotal > 0 ? Math.round(sectionsDone * 100 / dsrPageLoadTotal) : 0; \n");
		w.append("        var text = document.getElementById('dsr-pl-text'); \n");
		w.append("        var fill = document.getElementById('dsr-pl-fill'); \n");
		w.append("        if (text) text.textContent = 'Hold on: Loading page from server... ' + pct + '% (section ' + sectionsDone + ' of ' + dsrPageLoadTotal + ')'; \n");
		w.append("        if (fill) fill.style.width = pct + '%'; \n");
		w.append("    } \n");
		w.append("\n");
		w.append("    // The page has now been fully received and parsed -- remove this row. \n");
		w.append("    // NOTE: Only the row is removed, the 'progress-area' itself stays: the sparkline and \n");
		w.append("    //       chart loaders move their own progress rows into it right after this. \n");
		w.append("    // NOTE: This listener is registered FIRST (we are at the top of the body), so this row \n");
		w.append("    //       is gone before those loaders add theirs. \n");
		w.append("    document.addEventListener('DOMContentLoaded', function() { \n");
		w.append("        var div = document.getElementById('dsr-page-loading'); \n");
		w.append("        if (div && div.parentNode) div.parentNode.removeChild(div); \n");
		w.append("    }); \n");
		w.append("</script> \n");
		w.append("<!--<![endif]-->    \n"); // END: IGNORE THIS SECTION FOR OUTLOOK
		w.append("\n");
	}

	/** The name of the section that shows the time it took to create each section */
	private static final String EXEC_TIME_SECTION_NAME = "Exec/Creation time for each Report Section";

	public void createHtmlBody(Writer writer)
	throws IOException
	{
		writer.append("<body>\n");
//		sb.append("<body style'min-width: 100%'>\n");
//		sb.append("<body style'min-width: 2048px'>\n");
//		sb.append("<body style'min-width: 1024px'>\n");

		// Create a navbar if we want one.
		createHtmlNavbar(writer);

		// Create the area where ALL load-progress is shown: the "Loading page from server" row, and
		// the "Initializing Sparklines" / "Initializing Chart Info" rows that move themselves in here.
		// NOTE: Written as early as possible in the body (and OUTSIDE 'container-fluid', so it is full
		//       width), so the browser can render it while the rest of the page is still streaming in.
		createProgressAreaHtml(writer);

		if (useBootstrap())
		{
			writer.append("<div class='container-fluid'> \n"); // BEGIN: Bootstrap 4 container

			// Layout: a sticky table of contents on the left (wide screens), the sections (cards) on the right
			writer.append("<style type='text/css'> \n");
			writer.append("    body { background: #f6f7f9; } \n");
			writer.append("    .dsr-layout { display: flex; align-items: flex-start; } \n");
			writer.append("    .dsr-main   { flex: 1 1 auto; min-width: 0; } \n");
			writer.append("    .dsr-toc    { position: sticky; top: 8px; flex: 0 0 var(--dsr-toc-w, 260px); max-height: calc(100vh - 16px); overflow-y: auto; \n");
			writer.append("                  font-size: 13px; background: #ffffff; border: 1px solid #dfe3ea; border-radius: 8px; padding: 10px 8px; margin-top: 8px; } \n");
			writer.append("    .dsr-toc .dsr-toc-head { display: flex; justify-content: space-between; align-items: center; font-size: 11px; letter-spacing: .05em; text-transform: uppercase; color: #5d6878; margin: 0 0 6px 6px; } \n");
			writer.append("    .dsr-toc-btn { border: 0; background: transparent; color: #5d6878; font-size: 14px; line-height: 1; padding: 2px 6px; border-radius: 4px; cursor: pointer; } \n");
			writer.append("    .dsr-toc-btn:hover { background: #eef2f8; color: #1d2430; } \n");
			// The grip between the sidebar and the sections: drag = width, double click = default width
			writer.append("    .dsr-toc-grip { flex: 0 0 18px; align-self: stretch; cursor: col-resize; position: relative; } \n");
			writer.append("    .dsr-toc-grip::after { content: ''; position: absolute; top: 8px; bottom: 0; left: 8px; width: 2px; border-radius: 1px; background: transparent; } \n");
			writer.append("    .dsr-toc-grip:hover::after, .dsr-toc-grip.dsr-dragging::after { background: #b9c3d3; } \n");
			// Collapsed: no sidebar, a small button on the left edge brings it back
			writer.append("    .dsr-toc-open { display: none; position: fixed; left: 0; top: 90px; z-index: 1000; border: 1px solid #dfe3ea; border-left: 0; border-radius: 0 6px 6px 0; \n");
			writer.append("                    background: #ffffff; color: #5d6878; padding: 8px 6px; font-size: 14px; line-height: 1; cursor: pointer; box-shadow: 0 1px 3px rgba(0,0,0,.08); } \n");
			writer.append("    .dsr-toc-open:hover { color: #1d2430; background: #eef2f8; } \n");
			writer.append("    .dsr-toc-collapsed .dsr-toc, .dsr-toc-collapsed .dsr-toc-grip { display: none; } \n");
			writer.append("    @media (min-width: 992px) { .dsr-toc-collapsed .dsr-toc-open { display: block; } } \n");
			writer.append("    .dsr-toc a  { display: flex; justify-content: space-between; align-items: center; gap: 6px; padding: 3px 6px; border-radius: 4px; color: #1d2430; text-decoration: none; } \n");
			writer.append("    .dsr-toc a:hover { background: #eef2f8; } \n");
			writer.append("    .dsr-toc a.dsr-toc-on { background: #e6eefb; font-weight: 600; } \n");
			writer.append("    @media (max-width: 991.98px) { .dsr-toc, .dsr-toc-grip { display: none; } } \n");
			writer.append("    .dsr-card { border: 1px solid #dfe3ea !important; border-radius: 8px; } \n");
			// Wide tables: scroll left/right inside the section, not cut off (the sections are as wide as the window)
			writer.append("    .dsr-card .card-body { overflow-x: auto; } \n");
			writer.append("    .dsr-main details { max-width: 100%; overflow-x: auto; } \n");
			writer.append("    .dsr-card > .card-header { background: #ffffff; border-bottom: 1px solid #e8ebf0; font-size: 16px; } \n");
			writer.append("    .dsr-card > .card-header a { color: #1d2430; text-decoration: none; } \n");
			writer.append("    .dsr-origin { font-family: Consolas, 'Courier New', monospace; font-size: 11.5px; color: #6b7686; font-weight: normal; margin-left: 10px; } \n");
			writer.append("</style> \n");
			writer.append("<div class='dsr-layout'> \n");
			writer.append("<button type='button' class='dsr-toc-open' title='Show the table of contents' onclick='dsrTocToggle()'>&#9776;</button> \n");
			writer.append("<nav class='dsr-toc' aria-label='Report sections'> \n");
			writer.append("  <div class='dsr-toc-head'><span>" + getServerName() + "</span><button type='button' class='dsr-toc-btn' title='Hide the table of contents' onclick='dsrTocToggle()'>&laquo;</button></div> \n");
			for (IReportEntry entry : getReportEntries())
				writer.append("  <a href='#" + StringUtil.stripAllNonAlphaNum(entry.getSubject()) + "'><span>" + splitSubject(entry.getSubject())[0] + "</span>" + getIssueLevelBadge(entry) + "</a> \n");
			if (createExecTimeReportSection() && Configuration.getCombinedConfiguration().getBooleanProperty("DailySummaryReport.report.entry.enabled.printExecTime.fullMessage", true))
				writer.append("  <a href='#sectionTime'><span>" + EXEC_TIME_SECTION_NAME + "</span></a> \n");
			writer.append("</nav> \n");
			writer.append("<div class='dsr-toc-grip' title='Drag to change the width (double click: default width)'></div> \n");
			// Right here (not at the end of the page): the sidebar gets its saved width / collapsed state before it is drawn, no jump
			writer.append("<script type='text/javascript'> \n");
			writer.append("(function() { \n");
			writer.append("    var lay  = document.currentScript.parentNode; \n");
			writer.append("    var toc  = lay.querySelector('.dsr-toc'); \n");
			writer.append("    var grip = lay.querySelector('.dsr-toc-grip'); \n");
			writer.append("    function getItem(k)    { try { return localStorage.getItem(k); } catch(e) { return null; } } \n");
			writer.append("    function setItem(k, v) { try { localStorage.setItem(k, v);    } catch(e) {} } \n");
			writer.append("    var w = parseInt(getItem('dsr.toc.width'), 10); \n");
			writer.append("    if (w >= 160 && w <= 800) lay.style.setProperty('--dsr-toc-w', w + 'px'); \n");
			writer.append("    if (getItem('dsr.toc.collapsed') === '1') lay.classList.add('dsr-toc-collapsed'); \n");
			writer.append("    window.dsrTocToggle = function() { setItem('dsr.toc.collapsed', lay.classList.toggle('dsr-toc-collapsed') ? '1' : '0'); }; \n");
			writer.append("    grip.addEventListener('pointerdown', function(e) { \n");
			writer.append("        e.preventDefault(); \n");
			writer.append("        var x0 = e.clientX, w0 = toc.getBoundingClientRect().width; \n");
			writer.append("        grip.classList.add('dsr-dragging'); \n");
			writer.append("        document.body.style.userSelect = 'none'; \n");
			writer.append("        function move(ev) { lay.style.setProperty('--dsr-toc-w', Math.max(160, Math.min(800, w0 + ev.clientX - x0)) + 'px'); } \n");
			writer.append("        function up() { \n");
			writer.append("            document.removeEventListener('pointermove', move); \n");
			writer.append("            document.removeEventListener('pointerup', up); \n");
			writer.append("            grip.classList.remove('dsr-dragging'); \n");
			writer.append("            document.body.style.userSelect = ''; \n");
			writer.append("            setItem('dsr.toc.width', Math.round(toc.getBoundingClientRect().width)); \n");
			writer.append("        } \n");
			writer.append("        document.addEventListener('pointermove', move); \n");
			writer.append("        document.addEventListener('pointerup', up); \n");
			writer.append("    }); \n");
			writer.append("    grip.addEventListener('dblclick', function() { lay.style.removeProperty('--dsr-toc-w'); setItem('dsr.toc.width', ''); }); \n");
			writer.append("})(); \n");
			writer.append("</script> \n");
			writer.append("<div class='dsr-main'> \n");
		}
		writer.append("\n");


		// Collapseable group div 
		if (useBootstrap())
		{
			writer.append("<div id='accordion' role='tablist' aria-multiselectable='true'> \n");			
		}
		
		// TOC HEADER
		IssueLevel reportLevel      = getIssueLevel();
		String     reportLevelColor = reportLevel == IssueLevel.NONE ? "#198754" : reportLevel == IssueLevel.FYI ? "#0d6efd" : "#dc3545";
		if (createTocSection())
		{
			if (useBootstrap())
			{
				// Bootstrap "card" - BEGIN
//				sb.append("<!--[if !mso]><!--> \n"); // BEGIN: IGNORE THIS SECTION FOR OUTLOOK
//				sb.append("<div id='toc' class='card border-dark mb-3'>");
//				sb.append("<h5 class='card-header'><b>" + getReportName() + " for Servername: ").append(getServerName()).append("</b></h5>");
//				sb.append("<div class='card-body'>");
//				sb.append("<!--<![endif]-->    \n"); // END: IGNORE THIS SECTION FOR OUTLOOK
//				
//				sb.append("<!--[if mso]> \n"); // BEGIN: ONLY FOR OUTLOOK
//				sb.append("<h2>" + getReportName() + " for Servername: ").append(getServerName()).append("</h2>\n");
//				sb.append("<![endif]-->  \n"); // END: ONLY FOR OUTLOOK

				String show = " show";
				if (isCollapsedHeaderForTocSection())
					show = "";

				// Bootstrap "card" - BEGIN
				writer.append("<!--[if !mso]><!--> \n"); // BEGIN: IGNORE THIS SECTION FOR OUTLOOK
				writer.append("<div class='card dsr-card mb-3 mt-2' id='toc' style='border-left:6px solid " + reportLevelColor + " !important;'> \n");
				writer.append("<h5 class='card-header' role='tab' id='heading_toc'> \n");
				writer.append("  <a data-bs-toggle='collapse' data-bs-parent='#accordion' href='#collapse_toc' aria-expanded='true' aria-controls='collapse_toc' class='d-block'> \n");
				writer.append("    <i class='fa fa-chevron-down float-end'></i> \n");
				writer.append("    <b>").append(getServerName()).append("</b> ").append(ReportStatus.levelBadge(getIssueLevel())).append(" <span class='dsr-origin'>").append(getReportName()).append(" &middot; ").append(getPeriodText()).append("</span> \n");
				writer.append("  </a> \n");
				writer.append("</h5> \n");
				writer.append("<div id='collapse_toc' class='collapse" + show + "' role='tabpanel' aria-labelledby='heading_toc'> \n");
				writer.append("<div class='card-body'> \n");
				writer.append("<!--<![endif]-->    \n"); // END: IGNORE THIS SECTION FOR OUTLOOK

				writer.append("<!--[if mso]> \n"); // BEGIN: ONLY FOR OUTLOOK
				writer.append("<h2>" + getReportName() + " for Servername: ").append(getServerName()).append("</h2> \n");
				writer.append("<![endif]-->  \n"); // END: ONLY FOR OUTLOOK
			}
			else
			{
				// Normal HTML - H2 heading
				writer.append("<h2>" + getReportName() + " for Servername: ").append(getServerName()).append("</h2>\n");
			}
			writer.append( createDbxCentralLink(true) );
			writer.append( createLlmAnalysisButton() );

			// Key facts: the same row as in the mail
			List<String> facts = getKeyFactCells();
			if ( ! facts.isEmpty() )
			{
				writer.append("<table role='presentation' style='border-collapse:collapse; margin-top:10px; font-size:13px;'><tr>");
				writer.append(String.join("", facts));
				writer.append("</tr></table> \n");
			}

			//--------------------------------------------------
			// TOC
			writer.append("<div class='d-lg-none'> \n"); // wide screens have the table of contents on the left
			writer.append("<br> \n");
			writer.append("Links to Report Sections. \n");
			writer.append("<ul> \n");
			for (IReportEntry entry : getReportEntries())
			{
				String tocSubject = entry.getSubject();
				String tocDiv     = StringUtil.stripAllNonAlphaNum(tocSubject);

				// Strip off parts that may be details
				int firstLeftParentheses = tocSubject.indexOf("(");
				if (firstLeftParentheses != -1)
					tocSubject = tocSubject.substring(0, firstLeftParentheses - 1).trim();

				String liContent = "<a href='#" + tocDiv + "'>" + tocSubject + "</a>";
				liContent += getIssueLevelBadge(entry);
				
				writer.append("<li>").append(liContent).append("</li> \n");
			}
			writer.append("</ul> \n");
			writer.append("</div> \n");
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
		}
		
		
//System.out.println("  ******* Used Memory " + Memory.getUsedMemoryInMB() + " MB ****** at createHtmlBody(): after TOC");


		//--------------------------------------------------
		// ALL REPORTS
		int sectionsDone = 0;
		for (IReportEntry entry : getReportEntries())
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

				String show = " show";
				if (entry.isCollapsedHeader())
					show = "";

				// Bootstrap "card" - BEGIN
				writer.append("<!--[if !mso]><!--> \n"); // BEGIN: IGNORE THIS SECTION FOR OUTLOOK
				writer.append("<div class='card dsr-card mb-3' id='").append(tocDiv).append("' style='border-left:5px solid ").append(sectionLevelColor(getIssueLevel(entry))).append(" !important;'> \n");
				writer.append("<h5 class='card-header' role='tab' id='heading_").append(tocDiv).append("'> \n");
				writer.append("  <a data-bs-toggle='collapse' data-bs-parent='#accordion' href='#collapse_").append(tocDiv).append("' aria-expanded='true' aria-controls='collapse_").append(tocDiv).append("' class='d-block'> \n");
				writer.append("    <i class='fa fa-chevron-down float-end'></i> \n");
				writer.append("    <b>").append(splitSubject(entry.getSubject())[0]).append("</b>").append(getIssueLevelBadge(entry)).append("<span class='dsr-origin'>").append(splitSubject(entry.getSubject())[1]).append("</span> \n");
				writer.append("  </a> \n");
				writer.append("</h5> \n");
				writer.append("<div id='collapse_").append(tocDiv).append("' class='collapse" + show + "' role='tabpanel' aria-labelledby='heading_").append(tocDiv).append("'> \n");
				writer.append("<div class='card-body'> \n");
				writer.append("<!--<![endif]-->    \n"); // END: IGNORE THIS SECTION FOR OUTLOOK

				writer.append("<!--[if mso]> \n"); // BEGIN: ONLY FOR OUTLOOK
				writer.append("<h2 id='").append(tocDiv).append("'>").append(entry.getSubject()).append(getIssueLevelBadge(entry)).append("</h2> \n");
				writer.append("<![endif]-->  \n"); // END: ONLY FOR OUTLOOK
			}
			else
			{
				// Normal HTML - H2 heading
				writer.append("<h2 id='").append(tocDiv).append("'>").append(entry.getSubject()).append(getIssueLevelBadge(entry)).append("</h2> \n");
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

			// Update the "Loading page from server" banner.
			// The browser executes this while the page is streaming in, which gives us a "percent loaded".
			sectionsDone++;
			if (createPageLoadingBanner())
				writer.append("<script type='text/javascript'>if (typeof dsrPageLoadProgress === 'function') dsrPageLoadProgress(" + sectionsDone + ");</script> \n");
			
//System.gc();
//System.out.println("  ******* Used Memory " + Memory.getUsedMemoryInMB() + " MB ****** "+ entry.getClass().getSimpleName());
		}
		writer.append("\n<br>");

		//--------------------------------------------------
		// DEBUG - Write time it too to create each report entry
		if (createExecTimeReportSection())
			printExecTimeReport(writer, MessageType.FULL_MESSAGE);

		
		// Collapseable group div 
		if (useBootstrap())
		{
			writer.append("</div> \n");			
		}
		if (useBootstrap())
			writer.append("</div> \n</div> \n"); // END: dsr-main, dsr-layout

		//--------------------------------------------------
		// END
		writer.append("\n<br>");
		writer.append("\n<br>");
		writer.append("\n<code>--end-of-report--</code> \n");

		// Some static code for showing dialogs
		writer.append("\n");
		writer.append( createShowSqlTextDialogHtml() );
		writer.append( createShowSqlTextDialogJs()   );

		// dsrOpenLink() + the workload harvester, used by EVERY 'dsr-link' anchor ("View Execution Plan"
		// and "Get LLM Optimization Advice").
		// NOTE: written unconditionally. Those anchors are emitted for any statement that has SQL text,
		//       so this must NOT depend on some section happening to have an execution plan to write -
		//       that is exactly how reports from a server with no captured showplans ended up throwing
		//       "Uncaught ReferenceError: dsrOpenLink is not defined" on every link.
		// The recorded DBMS version goes along with every link, so the LLM knows what the server supports.
		writer.append( ShowplanLinkBuilder.getDsrLinkSupportJs(getDbmsVersionStr()) );

		writer.append("\n");
		writer.append("</div> \n"); // END: Bootstrap 4 container
		
		writer.append("\n");
		writer.append("    <!-- ================================================================================= -->\n");
		writer.append("    <!-- == AT the BOTTOM: do stuff when the page is loaded. -->\n");
		writer.append("    <!-- ================================================================================= -->\n");
		// The tooltip texts (Table Info, Triggers...), each ONCE: the elements refer to them with 'data-tt' (see ReportEntryAbstract.getTextAsTooltipDiv)
		writer.append("<div id='dsr-tt-texts' style='display:none'> \n");
		for (java.util.Map.Entry<String, Integer> e : getTooltipTexts().entrySet())
			writer.append("<div id='dsr-tt-").append(e.getValue().toString()).append("'>").append(org.apache.commons.text.StringEscapeUtils.escapeHtml4(e.getKey())).append("</div>\n");
		writer.append("</div> \n");
		writer.append("    <script type='text/javascript'>  \n");
		writer.append("\n");
		writer.append("        // Put the tooltip texts (written once, above) into 'data-tooltip' of every element that refers to them: the hover and the popup read it \n");
		writer.append("        document.querySelectorAll('[data-tt]').forEach(function(el) { \n");
		writer.append("            var txt = document.getElementById('dsr-tt-' + el.getAttribute('data-tt')); \n");
		writer.append("            if (txt) el.setAttribute('data-tooltip', txt.textContent); \n");
		writer.append("        }); \n");
		writer.append("\n");
		writer.append("        // Table of contents: highlight the section that is in view \n");
		writer.append("        (function() { \n");
		writer.append("            if (typeof IntersectionObserver === 'undefined') return; \n");
		writer.append("            var links = {}; \n");
		writer.append("            document.querySelectorAll('.dsr-toc a').forEach(function(a) { links[a.getAttribute('href').substring(1)] = a; }); \n");
		writer.append("            var obs = new IntersectionObserver(function(entries) { \n");
		writer.append("                entries.forEach(function(e) { \n");
		writer.append("                    if ( ! e.isIntersecting || ! links[e.target.id] ) return; \n");
		writer.append("                    document.querySelectorAll('.dsr-toc a.dsr-toc-on').forEach(function(a) { a.classList.remove('dsr-toc-on'); }); \n");
		writer.append("                    links[e.target.id].classList.add('dsr-toc-on'); \n");
		writer.append("                    links[e.target.id].scrollIntoView({ block: 'nearest' }); \n");
		writer.append("                }); \n");
		writer.append("            }, { rootMargin: '0px 0px -75% 0px' }); \n"); // the section in the top quarter of the window
		writer.append("            Object.keys(links).forEach(function(id) { var el = document.getElementById(id); if (el) obs.observe(el); }); \n");
		writer.append("        })(); \n");
		writer.append("\n");
		writer.append("        // Functionality to CLOSE ALL 'details' sections of class 'dsr-details-tableinfo' \n");
		writer.append("        document.querySelectorAll('.dsr-details-tableinfo-close-all').forEach(link => { \n");
		writer.append("            link.addEventListener('click', function(e) {                                \n");
		writer.append("                e.preventDefault(); // prevent jump                                     \n");
		writer.append("                document.querySelectorAll('.dsr-details-tableinfo').forEach(d => {      \n");
		writer.append("                    d.open = false; // close them all                                   \n");
		writer.append("                });                                                                     \n");
		writer.append("            });                                                                         \n");
		writer.append("        });                                                                             \n");
		writer.append("\n");
		writer.append("    </script> \n");
		writer.append("\n");

		// The "AI Analysis" digest: last in the file, so DbxCentral finds it quickly
		writer.append( createLlmDigestScript() );

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
			
			_logger.warn("Problems creating HTML " + getReportName() + ". Caught: " + rte, rte);
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
		writeTooltipTextsAsCss(w); // the Table Info... tooltips, each text ONCE (no JavaScript in a mail)

		// Phones (iPhone Mail, Gmail app): key facts 2 per row, "In this mail" as one column. Outlook ignores this (and keeps the tables)
		w.append("    <style type='text/css'> \n");
		w.append("        @media screen and (max-width: 620px), screen and (max-device-width: 620px) { \n");
		w.append("            .dsr-m-kf td  { display: inline-block !important; width: 50% !important; box-sizing: border-box; } \n");
		w.append("            .dsr-m-toc td { display: block !important; width: 100% !important; } \n");
		w.append("        } \n");
		w.append("    </style> \n");
		
		w.append("</head> \n");
		w.append("\n");
	}

	public void createShortMessageBody(Writer w)
	throws IOException
	{
		w.append("<body style='margin:0; padding:0 10px; background:#ffffff;'>\n");
		w.append("\n");

		// The mail is built with tables and inline styles only: that is what Outlook (Word), Gmail and iPhone Mail all show the same way
		IssueLevel level      = getIssueLevel();
		String     levelColor = level == IssueLevel.NONE ? "#198754" : level == IssueLevel.FYI ? "#0d6efd" : "#dc3545";
		String     levelName  = level == IssueLevel.NONE ? "NTR"     : level == IssueLevel.FYI ? "FYI"     : "ISSUE";
		String     levelText  = level == IssueLevel.NONE ? "Nothing to report."
		                      : level == IssueLevel.FYI  ? "Nothing urgent, have a look when you have time."
		                      :                            "Something to act on, see the Report Status below.";

		String period = getPeriodText();
		String appName = StringUtil.hasValue(getRecDbxAppName()) ? " &middot; " + getRecDbxAppName() : "";

		w.append("<table role='presentation' width='100%' cellpadding='0' cellspacing='0' style='border-collapse:collapse; font-family:\"Segoe UI\", Arial, Helvetica, sans-serif; font-size:14px; color:#1d2430; background:#ffffff;'>\n");

		//--------------------------------------------------
		// Status banner, in the level's color
		w.append("<tr><td style='background:" + levelColor + "; border:0; padding:16px 20px; color:#ffffff; white-space:normal;'>\n");
		w.append("  <table role='presentation' width='100%' cellpadding='0' cellspacing='0' style='border-collapse:collapse;'>\n");
		w.append("    <tr>\n");
		w.append("      <td style='background:" + levelColor + "; border:0; padding:0; color:#ffffff; vertical-align:top; white-space:normal;'>\n");
		w.append("        <div style='font-size:12px; letter-spacing:1px; text-transform:uppercase;'>" + getReportName() + appName + "</div>\n");
		w.append("        <div style='font-size:22px; font-weight:600; margin-top:2px;'>" + getServerName() + "</div>\n");
		w.append("        <div style='font-size:13px; margin-top:2px;'>" + period + "</div>\n");
		w.append("      </td>\n");
		w.append("      <td align='right' style='background:" + levelColor + "; border:0; padding:0; vertical-align:top; white-space:nowrap;'><span style='display:inline-block; background:#ffffff; color:" + levelColor + "; font-weight:700; font-size:13px; padding:4px 10px; border-radius:4px;'>" + levelName + "</span></td>\n");
		w.append("    </tr>\n");
		w.append("    <tr><td colspan='2' style='background:" + levelColor + "; border:0; padding:10px 0 0 0; color:#ffffff; font-size:15px; white-space:normal;'>" + levelText + "</td></tr>\n");
		w.append("  </table>\n");
		w.append("</td></tr>\n");

		//--------------------------------------------------
		// One button to the full report, and a link to all reports
		String dbxCentralBaseUrl = getDbxCentralPublicBaseUrl();
		w.append("<tr><td style='background:#ffffff; border:0; padding:14px 0 4px; white-space:normal;'>\n");
		w.append("  <table role='presentation' cellpadding='0' cellspacing='0' style='border-collapse:collapse;'><tr>\n");
		w.append("    <td style='background:#1d2430; border:0; padding:0; border-radius:5px;'><a href='" + dbxCentralBaseUrl + "/report?op=viewLatest&name=" + getServerName() + "' style='display:inline-block; padding:9px 16px; color:#ffffff; text-decoration:none; font-weight:600; font-size:14px;'>Open the full report in DbxCentral</a></td>\n");
		w.append("    <td style='background:#ffffff; border:0; padding:0 0 0 14px; font-size:13px;'><a href='" + dbxCentralBaseUrl + "/overview#reportfiles' style='color:#2b5fb8;'>All reports</a></td>\n");
		w.append("  </tr></table>\n");
		w.append("  <div style='font-size:12px; color:#5d6878; margin-top:8px;'>This mail is a summary. The full report is in DbxCentral" + (level == IssueLevel.NONE ? "." : ", and attached to this mail.") + "</div>\n");
		w.append("</td></tr>\n");

		//--------------------------------------------------
		// Key facts: a few numbers that answer "was it a normal day?" (IReportEntry.getKeyFacts), 4 per row
		List<String> facts = getKeyFactCells();
		if ( ! facts.isEmpty() )
		{
			w.append("<tr><td style='background:#ffffff; border:0; padding:12px 0 4px; white-space:normal;'>\n");
			w.append("  <table role='presentation' class='dsr-m-kf' width='100%' cellpadding='0' cellspacing='0' style='border-collapse:collapse; font-size:13px;'>\n");
			for (int i = 0; i < facts.size(); i += 4)
				w.append("    <tr>").append(String.join("", facts.subList(i, Math.min(i + 4, facts.size())))).append("</tr>\n");
			w.append("  </table>\n");
			w.append("</td></tr>\n");
		}
		w.append("</table>\n");

		//--------------------------------------------------
		// Report Status first (where the level comes from), then what is in this mail
		IReportEntry reportStatus = getReportEntry(ReportStatus.class);
		if (reportStatus != null)
			writeShortMessageEntry(w, reportStatus);

		List<String> tocCells = new ArrayList<>();
		for (IReportEntry entry : getReportEntries())
		{
			// Skip if "section" should not be part of the Short Message
			if ( ! entry.hasShortMessageText() || entry == reportStatus )
				continue;

			String tocSubject = entry.getSubject();
			int firstLeftParentheses = tocSubject.indexOf("(");
			if (firstLeftParentheses != -1)
				tocSubject = tocSubject.substring(0, firstLeftParentheses).trim();

			tocCells.add("<a href='#" + StringUtil.stripAllNonAlphaNum(entry.getSubject()) + "' style='color:#2b5fb8;'>" + tocSubject + "</a>" + getIssueLevelBadge(entry));
		}
		int half = (tocCells.size() + 1) / 2; // column 1 = first half, column 2 = second half (reads top-down)
		w.append("<table role='presentation' width='100%' cellpadding='0' cellspacing='0' style='border-collapse:collapse; font-family:\"Segoe UI\", Arial, Helvetica, sans-serif;'>\n");
		w.append("<tr><td style='background:#ffffff; border:0; padding:16px 0 4px; white-space:normal;'>\n");
		w.append("  <div style='font-size:16px; font-weight:600; border-left:4px solid #8a94a3; padding-left:8px;'>In this mail</div>\n");
		// Two cells, each with a whole column: on a phone the cells are stacked (see the @media above) and the order stays the same
		w.append("  <table role='presentation' class='dsr-m-toc' width='100%' cellpadding='0' cellspacing='0' style='border-collapse:collapse; margin-top:6px; font-size:13px;'>\n");
		w.append("    <tr>\n");
		for (int col = 0; col < 2; col++)
		{
			w.append("      <td valign='top' style='background:#ffffff; padding:0 8px 0 0; width:50%; border:0; white-space:normal;'>\n");
			for (int i = col * half; i < Math.min((col + 1) * half, tocCells.size()); i++)
				w.append("        <div style='padding:2px 0;'>").append(tocCells.get(i)).append("</div>\n");
			w.append("      </td>\n");
		}
		w.append("    </tr>\n");
		w.append("  </table>\n");
		w.append("</td></tr>\n");
		w.append("</table>\n");

		//--------------------------------------------------
		// ALL REPORTS
		for (IReportEntry entry : getReportEntries())
		{
			// Skip if "section" should not be part of the Short Message
			if ( ! entry.hasShortMessageText() || entry == reportStatus )
				continue;

			writeShortMessageEntry(w, entry);
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
	 * Mail section header: a table row (Outlook), the left border in the section's level color (gray: no finding),
	 * the title (the subject before " (") and the data source ("origin: ..." in the subject) small on the right
	 */
	private static String mailSectionHeader(IReportEntry entry, String tocDiv)
	{
		String   color  = sectionLevelColor(getIssueLevel(entry));
		String[] parts  = splitSubject(entry.getSubject());
		String   title  = parts[0];
		String   origin = parts[1];

		return "<a name='" + tocDiv + "'></a>\n"
				+ "<table role='presentation' id='" + tocDiv + "' width='100%' cellpadding='0' cellspacing='0' style='border-collapse:collapse; margin-top:22px; margin-bottom:6px; font-family:\"Segoe UI\", Arial, Helvetica, sans-serif;'><tr>"
				+ "<td style='background:#ffffff; font-size:16px; font-weight:600; color:#1d2430; border:0; border-left:4px solid " + color + "; padding:2px 0 2px 8px; white-space:normal;'>" + title + getIssueLevelBadge(entry)
				+ (origin.isEmpty() ? "" : " <span style='font-size:11px; font-weight:normal; color:#8a94a3; font-family:Consolas, \"Courier New\", monospace; white-space:nowrap;'>&nbsp; " + origin + "</span>")
				+ "</td>"
				+ "</tr></table>\n";
	}

	/** A section's level color (left border of the section header): red ISSUE, blue FYI, gray when nothing to report */
	private static String sectionLevelColor(IssueLevel level)
	{
		return level == IssueLevel.ISSUE ? "#dc3545" : level == IssueLevel.FYI ? "#0d6efd" : "#8a94a3";
	}

	/** "Top SQL (order by: x, origin: CmXxx / dm_xxx)" -> { "Top SQL", "CmXxx / dm_xxx" }. No " (": { subject, "" } */
	private static String[] splitSubject(String subject)
	{
		int pos = subject.indexOf(" (");
		if (pos == -1)
			return new String[] { subject, "" };

		String inParen = subject.substring(pos + 2, subject.endsWith(")") ? subject.length() - 1 : subject.length());
		int oPos = inParen.indexOf("origin: ");
		String title = subject.substring(0, pos).replaceAll("[,;: ]+$", ""); // "DB File IO, (order by...)" -> "DB File IO"
		return new String[] { title, oPos != -1 ? inParen.substring(oPos + "origin: ".length()) : "" };
	}

	/** The "key facts" (IReportEntry.getKeyFacts), each as a table cell: a few numbers that answer "was it a normal day?" */
	private List<String> getKeyFactCells()
	{
		List<String> facts = new ArrayList<>();
		for (IReportEntry entry : getReportEntries())
		{
			if ( ! entry.isEnabled() )
				continue;
			try
			{
				for (java.util.Map.Entry<String, String> fact : entry.getKeyFacts().entrySet())
					facts.add("<td valign='top' style='background:#ffffff; padding:6px 8px; border:1px solid #e3e7ee; white-space:normal;'><div style='color:#5d6878; font-size:11px;'>" + fact.getKey() + "</div>" + fact.getValue() + "</td>");
			}
			catch (RuntimeException ex)
			{
				_logger.warn("Problems getting the key facts from report entry '" + entry.getClass().getSimpleName() + "'. Skipping it.", ex);
			}
		}
		return facts;
	}

	//--------------------------------------------------------------------------------
	// "AI Analysis": a digest of the report for an LLM (read by DbxCentral's LlmDsrAnalysisServlet)
	//--------------------------------------------------------------------------------

	/** Write the digest into the full report, and the "AI Analysis" button */
	public static final String  PROPKEY_llmDigestEnabled         = "DailySummaryReport.llm.digest.enabled";
	public static final boolean DEFAULT_llmDigestEnabled         = true;

	/** Max characters of the 'data' of one section (IReportEntry.getLlmDigest) */
	public static final String  PROPKEY_llmDigestSectionMaxChars = "DailySummaryReport.llm.digest.section.maxChars";
	public static final int     DEFAULT_llmDigestSectionMaxChars = 4000;

	/** Max characters of the whole digest: the sections that do not fit are only listed in 'dropped' */
	public static final String  PROPKEY_llmDigestTotalMaxChars   = "DailySummaryReport.llm.digest.total.maxChars";
	public static final int     DEFAULT_llmDigestTotalMaxChars   = 40000;

	/** The id of the {@code <script type='application/json'>} element that holds the digest */
	public static final String  LLM_DIGEST_ELEMENT_ID            = "dsr-llm-digest";

	/** The version of the digest's layout, for DbxCentral */
	public static final int     LLM_DIGEST_VERSION               = 1;

	private static boolean isLlmDigestEnabled()
	{
		return Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_llmDigestEnabled, DEFAULT_llmDigestEnabled);
	}

	/**
	 * The "AI Analysis" button: opens DbxCentral's page that builds an LLM prompt from this report's digest.
	 * <p>
	 * The report file name is not known here (it is given when the report is saved, after this), so the link names the
	 * server and the report day; DbxCentral finds the file and checks the period in its digest. When this page is itself
	 * served by DbxCentral as {@code /report?op=view&name=<file>}, the script uses that exact file instead, and a
	 * same-origin link (no http/https switch).
	 */
	private String createLlmAnalysisButton()
	{
		if ( ! isLlmDigestEnabled() )
			return "";
		String baseUrl = getDbxCentralPublicBaseUrl();
		Timestamp beginTs = getReportPeriodOrRecordingBeginTime();
		if (StringUtil.isNullOrBlank(baseUrl) || beginTs == null)
			return "";

		String query = "?srv=" + java.net.URLEncoder.encode(StringUtil.nullToValue(getServerName(), ""), StandardCharsets.UTF_8)
		             + "&period=" + new SimpleDateFormat("yyyy-MM-dd").format(beginTs);

		return "<div style='margin-top:8px;'>"
		     + "<a id='dsr-llm-analysis-btn' class='btn btn-sm btn-outline-primary' target='_blank' href='" + baseUrl + "/llm-dsr-analysis" + query + "'"
		     + " title='Open DbxCentral, where an AI (LLM) can review this whole report and give a prioritized list of improvements. Only numbers are sent, no SQL text.'>"
		     + "AI Analysis</a> <span style='color:#5d6878; font-size:12px;'>Ask an AI to review the whole report</span>"
		     + "</div>\n"
		     + "<script>(function() { try { \n"
		     + "  var a = document.getElementById('dsr-llm-analysis-btn'); \n"
		     + "  if ( ! a || ! /\\/report$/.test(window.location.pathname) ) return; \n"
		     + "  var p = new URLSearchParams(window.location.search); \n"
		     + "  if (p.get('op') === 'view' && p.get('name')) a.href = '/llm-dsr-analysis?name=' + encodeURIComponent(p.get('name')); \n"
		     + "  else a.href = '/llm-dsr-analysis' + a.href.substring(a.href.indexOf('?')); \n"
		     + "} catch (e) { /* keep the absolute link */ } })();</script>\n";
	}

	/**
	 * The digest of the whole report for an LLM, as {@code <script type='application/json' id='dsr-llm-digest'>}.
	 * Per section: the level, the reason, the key facts (what every entry has) and {@link IReportEntry#getLlmDigest()}.
	 * Numbers and names only, no SQL text. Sections with level ISSUE first, then FYI, then the rest, within the size limits.
	 */
	private String createLlmDigestScript()
	{
		if ( ! isLlmDigestEnabled() )
			return "";
		try
		{
			Configuration conf = Configuration.getCombinedConfiguration();
			int sectionMaxChars = conf.getIntProperty(PROPKEY_llmDigestSectionMaxChars, DEFAULT_llmDigestSectionMaxChars);
			int totalMaxChars   = conf.getIntProperty(PROPKEY_llmDigestTotalMaxChars,   DEFAULT_llmDigestTotalMaxChars);

			com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
			SimpleDateFormat ymdHm = new SimpleDateFormat("yyyy-MM-dd HH:mm");
			Timestamp beginTs = getReportPeriodOrRecordingBeginTime();
			Timestamp endTs   = getReportPeriodOrRecordingEndTime();

			com.fasterxml.jackson.databind.node.ObjectNode root = om.createObjectNode();
			root.put("version",     LLM_DIGEST_VERSION);
			root.put("srvName",     getServerName());
			root.put("dbxApp",      getRecDbxAppName());
			root.put("dbmsVersion", getDbmsVersionStr() == null ? null : getDbmsVersionStr().replaceAll("\\s+", " ").trim());
			root.put("periodBegin", beginTs == null ? null : ymdHm.format(beginTs));
			root.put("periodEnd",   endTs   == null ? null : ymdHm.format(endTs));
			root.put("reportLevel", getIssueLevel().name());
			root.put("createdAt",   ymdHm.format(new Date()));

			com.fasterxml.jackson.databind.node.ArrayNode sections = root.putArray("sections");
			com.fasterxml.jackson.databind.node.ArrayNode namesOnly = om.createArrayNode();
			com.fasterxml.jackson.databind.node.ArrayNode disabled = om.createArrayNode();
			com.fasterxml.jackson.databind.node.ArrayNode dropped  = om.createArrayNode();

			// ISSUE first, then FYI, then the rest (a stable sort keeps the report order within a level)
			List<IReportEntry> entries = new ArrayList<>(getReportEntries());
			entries.sort((a, b) -> getIssueLevel(b).compareTo(getIssueLevel(a)));

			int totalChars = om.writeValueAsString(root).length();
			for (IReportEntry entry : entries)
			{
				String subject = StringUtil.stripHtml(entry.getSubject());
				if ( ! entry.isEnabled() )
				{
					String reason = entry.getDisabledReason();
					disabled.add(subject + (StringUtil.hasValue(reason) ? " - " + oneLineText(reason, 200) : ""));
					continue;
				}

				try
				{
					IssueLevel level = getIssueLevel(entry);

					com.fasterxml.jackson.databind.node.ObjectNode keyFacts = om.createObjectNode();
					for (java.util.Map.Entry<String, String> fact : entry.getKeyFacts().entrySet())
						keyFacts.put(oneLineText(fact.getKey(), 100), oneLineText(fact.getValue(), 200));

					String data = entry.getLlmDigest();
					boolean cut = false;
					if (data != null && data.length() > sectionMaxChars)
					{
						int nl = data.lastIndexOf('\n', sectionMaxChars);
						data = data.substring(0, nl > 0 ? nl + 1 : sectionMaxChars);
						cut  = true;
					}

					boolean hasProblem = entry.hasProblem();
					String  warning    = entry.hasWarningMsg() ? oneLineText(entry.getWarningMsg(), 300) : null;
					String  reason     = level == IssueLevel.NONE ? null : oneLineText(entry.getIssueReason(), 300);

					// Level NONE and no numbers for the digest (the section may still have tables in the report): only its name
					if (level == IssueLevel.NONE && keyFacts.isEmpty() && StringUtil.isNullOrBlank(data) && ! hasProblem && warning == null)
					{
						namesOnly.add(subject);
						continue;
					}

					com.fasterxml.jackson.databind.node.ObjectNode sect = om.createObjectNode();
					sect.put("id",      StringUtil.stripAllNonAlphaNum(entry.getSubject())); // the anchor in the report, see the TOC
					sect.put("subject", subject);
					sect.put("level",   level.name());
					if (StringUtil.hasValue(reason)) sect.put("reason", reason);
					if ( ! keyFacts.isEmpty() )      sect.set("keyFacts", keyFacts);
					if (hasProblem)                  sect.put("problem", "The section could not be created completely (see the report)");
					if (warning != null)             sect.put("warning", warning);
					if (StringUtil.hasValue(data))   sect.put("data", data);
					if (cut)                         sect.put("cut", true);

					int sectChars = om.writeValueAsString(sect).length();
					if (totalChars + sectChars > totalMaxChars)
					{
						dropped.add(subject);
						continue;
					}
					totalChars += sectChars;
					sections.add(sect);
				}
				catch (RuntimeException ex)
				{
					_logger.warn("Problems creating the LLM digest for report entry '" + entry.getClass().getSimpleName() + "'. Skipping it.", ex);
				}
			}
			if ( ! namesOnly.isEmpty() ) root.set("namesOnly", namesOnly);
			if ( ! disabled.isEmpty() ) root.set("disabled", disabled);
			if ( ! dropped .isEmpty() ) root.set("dropped",  dropped);

			// '</' would end the script element: '<\/' is the same in JSON
			String json = om.writeValueAsString(root).replace("</", "<\\/");
			return "<script type='application/json' id='" + LLM_DIGEST_ELEMENT_ID + "'>" + json + "</script>\n";
		}
		catch (Exception ex)
		{
			_logger.warn("Problems creating the LLM digest for the Daily Summary Report. The report is written without it.", ex);
			return "";
		}
	}

	/** HTML to one line of plain text, cut at maxLen */
	private static String oneLineText(String html, int maxLen)
	{
		if (html == null)
			return null;
		String txt = org.apache.commons.text.StringEscapeUtils.unescapeHtml4(StringUtil.stripHtml(html)).replace(' ', ' ').replaceAll("\\s+", " ").trim();
		return txt.length() <= maxLen ? txt : txt.substring(0, maxLen) + "...";
	}

	/** The period of the report, for example "Thursday 2026-11-19 &nbsp;00:00 &ndash; 23:59" ("" when not known) */
	private String getPeriodText()
	{
		Timestamp beginTs = getReportPeriodOrRecordingBeginTime();
		Timestamp endTs   = getReportPeriodOrRecordingEndTime();
		if (beginTs == null || endTs == null)
			return "";
		String day = beginTs.toLocalDateTime().getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH);
		return day + " " + new SimpleDateFormat("yyyy-MM-dd").format(beginTs)
		     + " &nbsp;" + new SimpleDateFormat("HH:mm").format(beginTs) + " &ndash; " + new SimpleDateFormat("HH:mm").format(endTs);
	}

	/** One section of the mail (the SHORT message) */
	private void writeShortMessageEntry(Writer w, IReportEntry entry)
	throws IOException
	{
		// So we can gather some statistics
		entry.beginWriteEntry(w, MessageType.SHORT_MESSAGE);

		String tocDiv = StringUtil.stripAllNonAlphaNum(entry.getSubject());

		// Add a section header
		w.append("\n");
		w.append("\n");
		w.append("<!-- ================================================================================= -->\n");
		w.append("<!-- " + entry.getSubject()                                                        + " -->\n");
		w.append("<!-- ================================================================================= -->\n");

		// Section HEADER
		w.append(mailSectionHeader(entry, tocDiv));

		if (entry.isEnabled())
		{
			try
			{
				entry.setCurrentMessageType(MessageType.SHORT_MESSAGE);

				// Warning messages
				if (entry.hasWarningMsg())
					w.append(entry.getWarningMsg());

				// Get the message text
				if ( ! entry.hasProblem() )
					entry.writeMessageText(w, entry.getCurrentMessageType());

				// If the entry indicates that it has a problem... then print that.
				if ( entry.hasProblem() )
					w.append(entry.getProblemText());

				// if we should append anything after an entry... Possibly '<br>\n'
				w.append(entry.getEndOfReportText());
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

		// So we can gather some statistics
		entry.endWriteEntry(w, MessageType.SHORT_MESSAGE);
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
			
			_logger.warn("Problems creating " + getReportName() + " (ShortMessage). Caught: " + rte, rte);
		}

		writer.append("</html> \n");
		writer.flush();
	}
	//-------------------------------------------------------------------------------------------
	// END: Short Message 
	//-------------------------------------------------------------------------------------------
	
	
	//-------------------------------------------------------------------------------------------
	// BEGIN: Minimal Message 
	//-------------------------------------------------------------------------------------------
	public void createMinimalMessageHead(Writer w)
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
		writeTooltipTextsAsCss(w); // the Table Info... tooltips, each text ONCE (no JavaScript in a mail)
		
		w.append("</head> \n");
		w.append("\n");
	}

	public void createMinimalMessageBody(Writer w)
	throws IOException
	{
		w.append("<body>\n");
		w.append("\n");
		
		// Normal HTML - H2 heading
		w.append("<h2>" + getReportName() + " for Servername: ").append(getServerName()).append("</h2>\n");

		w.append( createDbxCentralLink(false) );

		//--------------------------------------------------
		// TOC
		w.append("<br> \n");
		w.append("Links to Report Sections. \n");
		w.append("<ul> \n");
		for (IReportEntry entry : getReportEntries())
		{
			// Skip if "section" should not be part of the Minimal Message
			if ( ! entry.hasMinimalMessageText() )
				continue;

			String tocSubject = entry.getSubject();
			String tocDiv     = StringUtil.stripAllNonAlphaNum(tocSubject);

			// Strip off parts that may be details
			int firstLeftParentheses = tocSubject.indexOf("(");
			if (firstLeftParentheses != -1)
				tocSubject = tocSubject.substring(0, firstLeftParentheses - 1).trim();

			String liContent = "<a href='#" + tocDiv + "'>" + tocSubject + "</a>";
			liContent += getIssueLevelBadge(entry);
			
			w.append("<li>").append(liContent).append("</li> \n");
		}
		w.append("</ul> \n");
		w.append("\n<br>");

		//--------------------------------------------------
		// ALL REPORTS
		for (IReportEntry entry : getReportEntries())
		{
			// Skip if "section" should not be part of the Minimal Message
			if ( ! entry.hasMinimalMessageText() )
				continue;

			// So we can gather some statistics
			entry.beginWriteEntry(w, MessageType.MINIMAL_MESSAGE);

			String tocSubject = entry.getSubject();
			String tocDiv     = StringUtil.stripAllNonAlphaNum(tocSubject);

			// Add a section header
			w.append("\n");
			w.append("\n");
			w.append("<!-- ================================================================================= -->\n");
			w.append("<!-- " + entry.getSubject()                                                        + " -->\n");
			w.append("<!-- ================================================================================= -->\n");

			// Section HEADER
			w.append("<h2 id='").append(tocDiv).append("'>").append(entry.getSubject()).append(getIssueLevelBadge(entry)).append("</h2> \n");

			if (entry.isEnabled())
			{
				try
				{
					entry.setCurrentMessageType(MessageType.MINIMAL_MESSAGE);

					// Warning messages
					if (entry.hasWarningMsg())
						w.append(entry.getWarningMsg());

					// Get the message text
					if ( ! entry.hasProblem() )
						entry.writeMessageText(w, entry.getCurrentMessageType());
					
					// If the entry indicates that it has a problem... then print that.
					if ( entry.hasProblem() )
						w.append(entry.getProblemText());
					
					// if we should append anything after an entry... Possibly '<br>\n'
					w.append(entry.getEndOfReportText());
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
			entry.endWriteEntry(w, MessageType.MINIMAL_MESSAGE);

		}
		w.append("\n<br>");

		//--------------------------------------------------
		// DEBUG - Write time it too to create each report entry
//		printExecTimeReport(w, MessageType.MINIMAL_MESSAGE);

		//--------------------------------------------------
		// END
		w.append("\n<br>");
		w.append("\n<br>");
		w.append("\n<code>--end-of-short-report--</code> \n");

		w.append("\n");
		w.append("</body> \n");
	}

	/**
	 * Create a Minimal message, that for example can be used as a mail message
	 * 
	 * @param writer
	 * @throws IOException
	 */
	public void createMinimalMessage(Writer writer) 
	throws IOException
	{
//		writer.append("<html>\n");
		writer.append("<!doctype html>  \n");
		writer.append("<html lang='en'> \n");

		try
		{
			createMinimalMessageHead(writer);
			createMinimalMessageBody(writer);
		}
		catch (RuntimeException rte)
		{
			writer.append("<b>Problems creating Minimal Message report</b>, Caught RuntimeException: ").append(rte.toString()).append("<br> \n");
			writer.append("<pre>\n");
			writer.append(StringUtil.exceptionToString(rte));
			writer.append("</pre>\n");
			
			_logger.warn("Problems creating " + getReportName() + " (MinimalMessage). Caught: " + rte, rte);
		}

		writer.append("</html> \n");
		writer.flush();
	}
	//-------------------------------------------------------------------------------------------
	// END: Minimal Message 
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
			String headingName = EXEC_TIME_SECTION_NAME;

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

				String show = " show";
				if (isCollapsedHeaderForExecTimeReportSection())
					show = "";

				// Bootstrap "card" - BEGIN
				w.append("<!--[if !mso]><!--> \n"); // BEGIN: IGNORE THIS SECTION FOR OUTLOOK
				w.append("<div class='card dsr-card mb-3' id='").append(tocDiv).append("' style='border-left:5px solid #8a94a3 !important;'> \n");
				w.append("<h5 class='card-header' role='tab' id='heading_").append(tocDiv).append("'> \n");
				w.append("  <a data-bs-toggle='collapse' data-bs-parent='#accordion' href='#collapse_").append(tocDiv).append("' aria-expanded='true' aria-controls='collapse_").append(tocDiv).append("' class='d-block'> \n");
				w.append("    <i class='fa fa-chevron-down float-end'></i> \n");
				w.append("    <b>").append(headingName).append("</b> \n");
				w.append("  </a> \n");
				w.append("</h5> \n");
				w.append("<div id='collapse_").append(tocDiv).append("' class='collapse" + show + "' role='tabpanel' aria-labelledby='heading_").append(tocDiv).append("'> \n");
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

			for (IReportEntry entry : getReportEntries())
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
