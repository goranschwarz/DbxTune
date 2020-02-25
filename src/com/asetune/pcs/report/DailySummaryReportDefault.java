/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import com.asetune.pcs.PersistWriterToHttpJson;
import com.asetune.pcs.report.content.AlarmsActive;
import com.asetune.pcs.report.content.AlarmsHistory;
import com.asetune.pcs.report.content.DailySummaryReportContent;
import com.asetune.pcs.report.content.IReportEntry;
import com.asetune.pcs.report.content.RecordingInfo;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;
import com.asetune.utils.HeartbeatMonitor;
import com.asetune.utils.StringUtil;

public class DailySummaryReportDefault
extends DailySummaryReportAbstract
{
	private static Logger _logger = Logger.getLogger(DailySummaryReportDefault.class);
	private List<IReportEntry> _reportEntries = new ArrayList<>();

	@Override
	public void create()
	{
		// Set output for TimeStamp format for Strings in ResultSetTableModel to: yyyy-MM-dd HH:mm:ss
//		System.setProperty(ResultSetTableModel.PROPKEY_TimestampToStringFmt, ResultSetTableModel.DEFAULT_TimestampToStringFmt_YMD_HMS);
		
		DailySummaryReportContent content = new DailySummaryReportContent();
		content.setServerName( getServerName() );

		addReportEntries();
		_logger.info("Initiated Daily Summary Report with " + _reportEntries.size() + " report entries.");

		// Get Configuration possibly from the DbxCentral
//		Configuration pcsSavedConf = getConfigFromDbxCentral(getServerName());
		Configuration pcsSavedConf = getConfigFromPcs();
		Configuration localConf    = Configuration.getCombinedConfiguration();
		
		// Iterate all entries and create the report
		for (IReportEntry entry : _reportEntries)
		{
			try
			{
				if (entry.isEnabled())
					entry.create(getConnection(), getServerName(), pcsSavedConf, localConf);
			} 
			catch (RuntimeException rte) 
			{
				_logger.warn("Problems creating ReportEntry for '" + entry.getClass().getSimpleName() + "'. Caught RuntimeException, continuing with next entry.", rte);
			}
			
			// If this is done from the Collectors thread... and each of the reports are taking a long time... 
			// Then we might want to "ping" the collector supervisor, that we are still "alive"
			HeartbeatMonitor.doHeartbeat();
		}

		
		// Create and set TEXT/HTML Content
		String htmlText = createHtml();
		
//		// When creating a Text Table, it may be large...
//		// So if the HTML Output is Large... lets not create a Text Table. We will probably get an OutOfMemory Error
//		int textSizeLimit = 5*1024*1024; // 5MB
//
//		// Check if we should create a Text Table
//		String clearText = "Text Size will be to large. htmlSize=" + htmlText.length() + ", limit=" + textSizeLimit + ". The text size will be bigger. So skipping text generation.";
//		if (htmlText.length() < textSizeLimit)
//			clearText = createText();

		content.setReportAsHtml(htmlText);
//		content.setReportAsText(clearText);

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

	public void addReportEntries()
	{
		addReportEntry( new RecordingInfo(this) );
		addReportEntry( new AlarmsActive(this)  );
		addReportEntry( new AlarmsHistory(this) );
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

//	public String createText()
//	{
//		StringBuilder sb = new StringBuilder();
//
//		sb.append("Daily Summary Report for Servername: ").append(getServerName()).append("\n");
//
//		for (IReportEntry entry : _reportEntries)
//		{
//			sb.append("=======================================================\n");
//			sb.append(" ").append(entry.getSubject()).append(" \n");
//			sb.append("-------------------------------------------------------\n");
//			sb.append(entry.getMsgAsText());
//		}
//		sb.append("\n");
//		
//		sb.append("\n");
//		sb.append("\n");
//		sb.append("--end-of-report--\n");
//
//		return sb.toString();
//	}

	public String createHtmlHead()
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
					
				return htmlHeadContent;
			}
			catch(IOException ex)
			{
				_logger.error("Problems reading External HTML HEAD File '" + f.getAbsolutePath() + "', Skipping this and using System provided header. Caught: "+ex, ex);
			}
		}

		//-------------------------------------------------------------------
		// Normal logic: Default System provided HTML Header
		//-------------------------------------------------------------------
		StringBuilder sb = new StringBuilder();
		
		sb.append("\n");
		sb.append("<head> \n");
		sb.append("    <title>DSR: ").append(getServerName()).append("</title>\n");
		sb.append("\n");
		sb.append("    <meta charset='utf-8'/>\n");
		sb.append("    <meta name='x-apple-disable-message-reformatting' />\n");
//		sb.append("    <meta name='viewport' content='width=device-width, user-scalable=no, initial-scale=1, maximum-scale=1, minimal-ui'>\n");
		sb.append("\n");
		sb.append("    <style type='text/css'> \n");
		sb.append("        body {\n");
		sb.append("            -webkit-text-size-adjust: 100%;\n");
		sb.append("            -ms-text-size-adjust: 100%;\n");
		sb.append("            font-family: Arial, Helvetica, sans-serif;\n");
		sb.append("        }\n");
		sb.append("        pre {\n");
		sb.append("            font-size: 10px;\n");
		sb.append("            word-wrap: none;\n");
		sb.append("            white-space: no-wrap;\n");
//		sb.append("            space: nowrap;\n");
		sb.append("        }\n");
		sb.append("        table {\n");
		sb.append("            mso-table-layout-alt: fixed;\n"); // not sure about this - https://gist.github.com/webtobesocial/ac9d052595b406d5a5c1
		sb.append("            mso-table-overlap: never;\n");    // not sure about this - https://gist.github.com/webtobesocial/ac9d052595b406d5a5c1
		sb.append("            mso-table-wrap: none;\n");        // not sure about this - https://gist.github.com/webtobesocial/ac9d052595b406d5a5c1
		sb.append("            border-collapse: collapse;\n");
		sb.append("        }\n");
		sb.append("        th {\n");
		sb.append("            border: 1px solid black;\n");
		sb.append("            text-align: left;\n");
		sb.append("            padding: 2px;\n");
		sb.append("            white-space: nowrap;\n");
		sb.append("            background-color: gray;\n");
		sb.append("            color: white;\n");
		sb.append("        }\n");
		sb.append("        td {\n");
		sb.append("            border: 1px solid black;\n");
		sb.append("            text-align: left;\n");
		sb.append("            padding: 2px;\n");
		sb.append("            white-space: nowrap;\n");
		sb.append("        }\n");
		sb.append("        tr:nth-child(even) {\n");
		sb.append("            background-color: #f2f2f2;\n");
		sb.append("        }\n");
		sb.append("        h2 {\n");
		sb.append("            border-bottom: 2px solid black;\n");
		sb.append("            border-top: 2px solid black;\n");
		sb.append("            margin-bottom: 3px;\n");
		sb.append("        }\n");
		sb.append("        h3 {\n");
		sb.append("            border-bottom: 1px solid black;\n");
		sb.append("            border-top: 1px solid black;\n");
		sb.append("            margin-bottom: 3px;\n");
		sb.append("        }\n");
		sb.append("\n");
//		sb.append("        /* the below is to HIDE/SHOW content (in mail, which can not execute javascript) */ \n");
		sb.append("        .hide-show {\n");
		sb.append("            display: none;\n");
		sb.append("        }\n");
		sb.append("        input[type='checkbox']:checked ~ .hide-show {\n");
		sb.append("            display: block;\n");
		sb.append("        }\n");
		sb.append("\n");
		sb.append("    </style> \n");
		sb.append("\n");
		sb.append("    <SCRIPT src='http://www.dbxtune.com/sorttable.js'></SCRIPT> \n");
		sb.append("\n");
		sb.append("    <script type='text/javascript'>  \n");
		sb.append("        function toggle_visibility(id) \n");
		sb.append("        { \n");
		sb.append("           var e = document.getElementById(id); \n");
		sb.append("           if(e.style.display == 'block') \n");
		sb.append("              e.style.display = 'none'; \n");
		sb.append("           else \n");
		sb.append("              e.style.display = 'block'; \n");
		sb.append("           return false; \n");
		sb.append("        } \n");
		sb.append("    </script> \n");

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
		sb.append("</head> \n");
		sb.append("\n");

		return sb.toString();
	}

	public String createHtmlBody()
	{
		StringBuilder sb = new StringBuilder();

		sb.append("<body>\n");
//		sb.append("<body style'min-width: 100%'>\n");
//		sb.append("<body style'min-width: 2048px'>\n");
//		sb.append("<body style'min-width: 1024px'>\n");
		sb.append("\n");

		sb.append("<h2>Daily Summary Report for Servername: ").append(getServerName()).append("</h2>\n");
		sb.append( createDbxCentralLink() );

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

		//--------------------------------------------------
		// ALL REPORTS
		for (IReportEntry entry : _reportEntries)
		{
			String tocSubject = entry.getSubject();
			String tocDiv     = StringUtil.stripAllNonAlphaNum(tocSubject);

			// Add a section header
			sb.append("<h2 id='").append(tocDiv).append("'>").append(entry.getSubject()).append("</h2> \n");

			if (entry.isEnabled())
			{
				// Get the message text
				sb.append(entry.getMessageText());
				
				// If the entry indicates that it has a problem... then print that.
				sb.append(entry.getProblemText());
				
				// if we should append anything after an entry... Possibly '<br>\n'
				sb.append(entry.getEndOfReportText());

				// Notes for how to: Disable this entry
				if (entry.canBeDisabled())
				{
					sb.append("<br>");
					sb.append("<i>To disable this report entry, put the following in the configuration file. ");
					sb.append("<code>").append(entry.getIsEnabledConfigKeyName()).append(" = false</code></i><br>");
				}
			}
			else
			{
				// Entry is DISABLED
				sb.append("This entry is <b>disabled</b>, to enable it; put the following in the configuration file. ");
				sb.append("<code>").append(entry.getIsEnabledConfigKeyName()).append(" = false</code><br>");
			}
		}
		sb.append("\n<br>");

		//--------------------------------------------------
		// END
		sb.append("\n<br>");
		sb.append("\n<br>");
		sb.append("\n<code>--end-of-report--</code>\n");

		sb.append("\n");
		sb.append("</body>\n");

		return sb.toString();
	}

//	public String getDbxCentralBaseUrl()
//	{
//		// initialize with default parameters, which may change below...
//		String dbxCentralProt = "http";
//		String dbxCentralHost = StringUtil.getHostnameWithDomain();
//		int    dbxCentralPort = 8080;
//
//		// get where DBX CENTRAL is located.
//		String sendToDbxCentralUrl = Configuration.getCombinedConfiguration().getProperty("PersistWriterToHttpJson.url", null);
//		if (StringUtil.hasValue(sendToDbxCentralUrl))
//		{
//			// Parse the URL and get protocol/host/port
//			try
//			{
//				URL url = new URL(sendToDbxCentralUrl);
//				
//				dbxCentralProt = url.getProtocol();
//				dbxCentralHost = url.getHost();
//				dbxCentralPort = url.getPort();
//			}
//			catch (MalformedURLException ex)
//			{
//				_logger.info("Daily Report: Problems parsing DbxCentral URL '" + sendToDbxCentralUrl + "', using defaults. Caught:" + ex);
//			}
//		}
//		
//		// Collector and DBX Central is located on the same host
//		// if 'localhost' or '127.0.0.1' then get REAL localhost name
//		if (dbxCentralHost.equalsIgnoreCase("localhost") || dbxCentralHost.equalsIgnoreCase("127.0.0.1"))
//		{
//			dbxCentralHost = StringUtil.getHostnameWithDomain();
//		}
//
//		// Compose URL's
//		String dbxCentralBaseUrl = dbxCentralProt + "://" + dbxCentralHost + ( dbxCentralPort == -1 ? "" : ":"+dbxCentralPort);
//
//		// Return a Text with links
//		return dbxCentralBaseUrl;
//	}
//
//	public String createDbxCentralLink()
//	{
//		String dbxCentralBaseUrl = getDbxCentralBaseUrl();
//		String dbxCentralUrlLast = dbxCentralBaseUrl + "/report?op=viewLatest&name="+getServerName();
//		String dbxCentralUrlAll  = dbxCentralBaseUrl + "/overview#reportfiles";
//
//		// Return a Text with links
//		return
//			"If you have problems to read this as a mail; Here is a <a href='" + dbxCentralUrlLast + "'>Link</a> to latest HTML Report stored in DbxCentral.<br>\n" +
//			"Or a <a href='" + dbxCentralUrlAll  + "'>link</a> to <b>all</b> Daily Reports.<br>\n" +
//			"";
//	}
//
	public String createHtml()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("<html>\n");

		try
		{
			sb.append(createHtmlHead());
			sb.append(createHtmlBody());
		}
		catch (RuntimeException rte)
		{
			sb.append("<b>Problems creating HTML report</b>, Caught RuntimeException: ").append(rte.toString()).append("<br>\n");
			sb.append("<pre>\n");
			sb.append(StringUtil.exceptionToString(rte));
			sb.append("</pre>\n");
			
			_logger.warn("Problems creating HTML Daily Summary Report. Caught: "+rte, rte);
		}

		sb.append("</html>\n");

		return sb.toString();
	}
}
