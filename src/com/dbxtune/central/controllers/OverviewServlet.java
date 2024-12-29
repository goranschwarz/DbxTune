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
package com.dbxtune.central.controllers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.time.DateUtils;
import org.apache.log4j.Logger;

import com.dbxtune.DbxTune;
import com.dbxtune.Version;
import com.dbxtune.alarm.writers.AlarmWriterToFile;
import com.dbxtune.central.DbxTuneCentral;
import com.dbxtune.central.cleanup.CentralH2Defrag;
import com.dbxtune.central.cleanup.DataDirectoryCleaner;
import com.dbxtune.central.cleanup.CentralH2Defrag.H2StorageInfo;
import com.dbxtune.central.controllers.ud.chart.IUserDefinedChart;
import com.dbxtune.central.controllers.ud.chart.UserDefinedChartManager;
import com.dbxtune.central.pcs.CentralPersistReader;
import com.dbxtune.central.pcs.objects.DbxCentralServerLayout;
import com.dbxtune.central.pcs.objects.DbxCentralSessions;
import com.dbxtune.pcs.report.DailySummaryReportFactory;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.FileUtils;
import com.dbxtune.utils.NumberUtils;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.TimeUtils;

public class OverviewServlet extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	private static final Logger _logger = Logger.getLogger(MethodHandles.lookup().lookupClass());

//	private static final String HOME_DIR    = DbxTuneCentral.getAppHomeDir();
	private static final String INFO_DIR    = DbxTuneCentral.getAppInfoDir();
	private static final String LOG_DIR     = DbxTuneCentral.getAppLogDir();
	private static final String CONF_DIR    = DbxTuneCentral.getAppConfDir();
	private static final String REPORTS_DIR = DbxTuneCentral.getAppReportsDir();
	private static final String DATA_DIR    = DbxTuneCentral.getAppDataDir();

	public static final String PROPKEY_LogfilesShortcuts = "logfiles.shortcuts";
	public static final String DEFAULT_LogfilesShortcuts = "*.console";

//	public static final String  PROPKEY_enableDownloadRecordings = "OverviewServlet.enable.download.recordings";
	public static final String  PROPKEY_enableDownloadRecordings = "download.recordings.enabled";
	public static final boolean DEFAULT_enableDownloadRecordings = true;
	
	public static List<String> getInfoFilesDbxTune()
	{
		String directory = INFO_DIR;

		List<String> fileNames = new ArrayList<>();
		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(directory)))
		{
			for (Path path : directoryStream)
			{
				if (path.toString().endsWith(".dbxtune"))
					fileNames.add(path.toString());
			}
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
		}

		// Sort the list
		Collections.sort(fileNames);
		
		return fileNames;
	}

//	private String getSrvDescription(String srvName)
//	{
//		String file = DbxTuneCentral.getAppConfDir() + "/SERVER_LIST";
//		File f = new File(file);
//
//		String description = "";
//		try
//		{
//			FileInputStream in = new FileInputStream(f);
//			BufferedReader br = new BufferedReader(new InputStreamReader(in));
//
//			String tmp;
//			while ((tmp = br.readLine()) != null)
//			{
//				if (StringUtil.hasValue(tmp))
//				{
//					if ( ! tmp.startsWith("#") )
//					{
//						String[] sa = tmp.split(";");
//						if (sa.length >= 3)
//						{
//							String name    = sa[0].trim();
//							//String enabled = sa[1].trim();
//							String desc    = sa[2].trim();
//
//							if (srvName.equals(name))
//								return desc;
//						}
//					}
//				}
//			}
//			br.close();
//			in.close();
//		}
//		catch (Exception ex)
//		{
//			return ex.toString();
//		}
//		return description;
//	}

//	private List<String> getFilesActiveAlarms()
//	{
//		String directory = LOG_DIR;
//
//		List<String> fileNames = new ArrayList<>();
//		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(directory)))
//		{
//			for (Path path : directoryStream)
//			{
//				if (path.getFileName().toString().matches("ALARM\\.ACTIVE\\..*\\.txt"))
//					fileNames.add(path.toString());
//			}
//		}
//		catch (IOException ex)
//		{
//			ex.printStackTrace();
//		}
//
//		// Sort the list
//		Collections.sort(fileNames);
//		
//		return fileNames;
//	}

	private List<String> getFilesInLogDir()
	{
		String directory = LOG_DIR;

		List<String> fileNames = new ArrayList<>();
		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(directory)))
		{
			for (Path path : directoryStream)
			{
				fileNames.add(path.toString());
			}
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
		}

		// Sort the list
		Collections.sort(fileNames);
		
		return fileNames;
	}
	
	public static List<File> getFilesInConfDir()
	{
		String directory = CONF_DIR;

		List<File> fileNames = new ArrayList<>();
		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(directory)))
		{
			for (Path path : directoryStream)
			{
				fileNames.add(path.toFile());
			}
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
		}

		// Sort the list
		Collections.sort(fileNames);
		
		return fileNames;
	}
	
	public static List<String> getFilesInReportsDir()
	{
		String directory = REPORTS_DIR;

		List<String> fileNames = new ArrayList<>();
		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(directory)))
		{
			for (Path path : directoryStream)
			{
				fileNames.add(path.toString());
			}
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
		}

		// Sort the list
		Collections.sort(fileNames);
		
		return fileNames;
	}
	
	public enum H2DbFileType
	{
		ALL, 
		OFFLINE, 
		ACTIVE, 
		OFFLINE_AND_ACTIVE, 
		DBX_CENTRAL
	};

	private static boolean isTodayH2DbTimestamp(String name)
	{
		if (name.toUpperCase().matches(".*_[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9].MV.DB"))
		{
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			String dateNow = sdf.format(new Date());

			int postfixLen1  = "yyyy-MM-dd.mv.db".length();
			int postfixLen2  = ".mv.db".length();
			
			String fileTs = name.substring(name.length()-postfixLen1, name.length()-postfixLen2);

			return dateNow.equals(fileTs);
		}
		return false;
	}

//	private List<String> getFilesH2Dbs(H2DbFileType type)
	public static List<String> getFilesH2Dbs(H2DbFileType type)
	{
		String directory = DATA_DIR;

		List<String> fileNames = new ArrayList<>();
		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(directory)))
		{
			for (Path path : directoryStream)
			{
				String pathStr = path.toString();
				
				if (pathStr.endsWith(".mv.db"))
				{
					String addFile = null;
					
					if      (H2DbFileType.ALL.equals(type)) 
					{
						addFile = pathStr;
					}
					else if (H2DbFileType.OFFLINE.equals(type) || H2DbFileType.ACTIVE.equals(type) || H2DbFileType.OFFLINE_AND_ACTIVE.equals(type))
					{
						if (pathStr.toUpperCase().matches(".*_[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9].MV.DB"))
						{
							if ( H2DbFileType.OFFLINE.equals(type) )
							{
								if ( ! isTodayH2DbTimestamp(pathStr) )
									addFile = pathStr;
							}
							else if ( H2DbFileType.ACTIVE.equals(type) )
							{
								if ( isTodayH2DbTimestamp(pathStr) )
									addFile = pathStr;
							}
							else if ( H2DbFileType.OFFLINE_AND_ACTIVE.equals(type) )
							{
								addFile = pathStr;
							}
						}
					}
					else if (H2DbFileType.DBX_CENTRAL.equals(type)) 
					{
						if (pathStr.indexOf("DBXTUNE_CENTRAL_DB") >= 0)
							addFile = pathStr;
					}
					
					if (StringUtil.hasValue(addFile))
						fileNames.add(addFile);
				}
			}
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
		}

		// Sort the list
		Collections.sort(fileNames);
		
		return fileNames;
	}
	
	private String getDayOfWeek(String input, String format)
	{
		try
		{
			SimpleDateFormat sdf = new SimpleDateFormat(format);
			Date parsedDate = sdf.parse(input);

			DateFormat df = new SimpleDateFormat("EEEE");
			String dayOfWeek = df.format(parsedDate);

			return dayOfWeek;
		}
		catch(ParseException ex)
		{
			return "unknown";
		}
	}

	private String getActiveAlarmContent(File f) 
	throws IOException
	{
		if ( ! f.exists() )
			return "File do not exists: "+f.getAbsolutePath();

		if ( f.length() == 0 )
			return "Empty file: "+f.getAbsolutePath();

		String content = FileUtils.readFile(f, null);
		if (content != null)
			content = content.trim();
		
		return content;
	}

	private String getLastLine(File f)
	{
		if ( ! f.exists() )
			return "File do not exists: "+f.getAbsolutePath();

		if ( f.length() == 0 )
			return "Empty file: "+f.getAbsolutePath();

		try
		{
			FileInputStream in = new FileInputStream(f);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));

			String strLine = null, tmp;
			while ((tmp = br.readLine()) != null)
			{
				if (StringUtil.hasValue(tmp))
					strLine = tmp;
			}
			in.close();

			if (strLine == null)
				strLine = "";

			return strLine;
		}
		catch (Exception ex)
		{
			return ex.toString();
		}
	}
	
	private String getLastLineAge(String line)
	{
//		String lastLogLineTs  = "FIXME"; //datetime.datetime.strptime( lastLogLine.split(' - ')[0], '%Y-%m-%d %H:%M:%S.%f' )
//		String lastLogLineAge = getLastLineAge(lastLogLine); //str( datetime.datetime.now() - lastLogLineTs ).split('.', 2)[0][:-3]

		if (StringUtil.isNullOrBlank(line))
			return "";

		if (line.startsWith("Empty file:") || line.startsWith("File do not exists:") )
			return "";

		try
		{
			String firstWord = line.split(" - ")[0];

			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
			Date date = sdf.parse(firstWord);

			long diff = System.currentTimeMillis() - date.getTime();
			return TimeUtils.msToTimeStr("%?DD[d ]%HH:%MM", diff);
		}
		catch(ParseException ex)
		{
			return ex.getMessage();
		}
	}

	/**
	 * Print "Collector Refresh Time" *BUTTONS* <br>
	 * Note: This is called recursively... 
	 * 
	 * @param out
	 * @param root
	 * @throws IOException
	 */
	private void printServerLayout_CmRefreshButtons(ServletOutputStream out, List<DbxCentralServerLayout> root) 
	throws IOException
	{
		for (DbxCentralServerLayout layoutEntry : root)
		{
			if (layoutEntry.isGroupEntry())
			{
				String labelText = layoutEntry.getText();

				out.println("<br>");
				out.println(labelText);

				if ( ! labelText.trim().toUpperCase().endsWith("<BR>")) // NOTE: This do NOT seems to work (I don't know why an extra <br> is added in some cases)
					out.println("<br>");

				printServerLayout_CmRefreshButtons(out, layoutEntry.getEntries());
			}
			else if (layoutEntry.isLabelEntry())
			{
				String labelText = layoutEntry.getText();

				out.println("<br>");
				if (StringUtil.hasValue(labelText))
				{
					out.println(labelText);

					if ( ! labelText.trim().toUpperCase().endsWith("<BR>")) // NOTE: This do NOT seems to work (I don't know why an extra <br> is added in some cases)
						out.println("<br>");
				}
			}
			else if (layoutEntry.isServerEntry())
			{
				DbxCentralSessions srvEntry = layoutEntry.getSrvSession();

				String srvName   = srvEntry.getServerName();
				String dbxType   = "dbx-button-" + srvEntry.getProductString().toLowerCase(); // dbx-button-asetune

				// NOTE: If the below text is CHANGED, ALSO Change in method: doGet(...) --> 'CM Refresh Time'
				String link   = "/graph.html?subscribe=true&cs=dark&startTime=2h&sessionName=" + srvName + "&graphList=CmSummary_CmRefreshTime&gcols=1";
				String button = "<a href='" + link + "' target='_blank' class='btn btn-sm btn-primary dbx-button-image " + dbxType + " mb-2 mr-2' role='button'>" + srvName + "</a>";
				
				out.println(button);
			}
			else
			{
				_logger.warn("---UNKNOWN---ENTRY: type=|" + layoutEntry.getType() + "|, text=|" + layoutEntry.getText() + "|.");
			}
		}
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
	throws ServletException, IOException
	{
		ServletOutputStream out = resp.getOutputStream();
		resp.setContentType("text/html");
		resp.setCharacterEncoding("UTF-8");
//		resp.setContentType("application/json");
//		resp.setCharacterEncoding("UTF-8");
		
		// Check for known input parameters
		if (Helper.hasUnKnownParameters(req, resp, "refresh", "op", "srv"))
			return;

		
		String refreshStr   = Helper.getParameter(req, "refresh", "0");
		String reqOperation = Helper.getParameter(req, "op"     , "");
		String reqSrvName   = Helper.getParameter(req, "srv"    , "");

		int refresh = StringUtil.parseInt(refreshStr, 0);


		String linkToolTip = "Open this recording in native/desktop DbxTune application.\nBy clicking this; a request will be sent to the first DbxTune application which you started on your local (PC) client";
		String dbxTuneGuiUrl = "http://localhost:PORT/connect-offline?url=";

		String username = System.getProperty("user.name");
		String hostname = InetAddress.getLocalHost().getHostName();
		
		out.println("<html>");
		out.println("<head>");

		out.println("<title>Server Overview</title> ");
		
		if (refresh > 0)
			out.println("<meta http-equiv='refresh' content='"+refresh+"' />");

		out.println(HtmlStatic.getOverviewHead());
		
		out.println("<style type='text/css'>");
		out.println("  table {border-collapse: collapse;}");
		out.println("  th {border: 1px solid black; text-align: left; padding: 2px; white-space: nowrap; background-color:gray; color:white;}");
		out.println("  td {border: 1px solid black; text-align: left; padding: 2px; white-space: nowrap; }");
		out.println("  tr:nth-child(even) {background-color: #f2f2f2;}");
//		out.println("  .topright { position: absolute; top: 8px; right: 16px; font-size: 14px; }"); // topright did not work with bootstrap (and navigation bar) 

		out.println("</style>");

		out.println("<link href='/scripts/prism/prism.css' rel='stylesheet' />");

		out.println("</head>");
		
		out.println("<body onload='updateLastUpdatedClock()'>");
		out.println("<script src='/scripts/prism/prism.js'></script> ");

		out.println(HtmlStatic.getOverviewNavbar());

		out.println("<div class='container-fluid'>");

		String ver = "Version: " + Version.getVersionStr() + ", Build: " + Version.getBuildStr();
		
		out.println("<h1>DbxTune - Central - " + username + "@" + hostname + "</h1>");

		out.println("<div class='topright'>"+ver+"</div>");

		out.println("<p>");
//		out.println("Page loaded: " + (new Timestamp(System.currentTimeMillis())) + ", " );
		out.println("Page loaded: <span id='last-update-ts'>" + (new Timestamp(System.currentTimeMillis())) + "</span>, ");
		out.println("for 'auto-refresh' every 60 second click <a href='/overview?refresh=60'>here</a><br>" );
		out.println("</p>");

		out.println("<p>");
		out.println("<a href='graph.html?subscribe=false&cs=dark&startTime=4h&sessionName=DbxCentral'                     target='_blank'>Show Local DbxCentral (OS) Metrics <b>pre selected</b>, for last 4 hours, in new tab.</a><br>");
		out.println("<a href='graph.html?subscribe=false&cs=dark&startTime=4h&sessionName=DbxcLocalMetrics&graphList=all' target='_blank'>Show Local DbxCentral (OS) Metrics <b>all graphs</b>, for last 4 hours, in new tab.</a><br>");
		out.println("</p>");

		out.println("<p>Sections");
		out.println("<ul>");
		out.println("  <li><a href='#ud-content'        >User Defined Content                          </a> </li>");
		out.println("  <li><a href='#cm-refresh-time'   >Collector Refresh Time                        </a> </li>");
		out.println("  <li><a href='#active'            >Active Recordings                             </a> </li>");
		out.println("  <li><a href='#alarms'            >Active Alarms                                 </a> </li>");
		out.println("  <li><a href='#logfiles'          >All file(s) in the LOG Directory              </a> </li>");
		out.println("  <li><a href='#conffiles'         >All file(s) in the CONF Directory             </a> </li>");
		out.println("  <li><a href='#reportfiles'       >All file(s) in the REPORTS Directory          </a> </li>");
		out.println("  <li><a href='#central'           >Dbx CENTRAL databases                         </a> </li>");
		out.println("  <li><a href='#offline'           >Available OFFLINE databases                   </a> </li>");
		out.println("  <li><a href='#active_filecontent'>Active Recordings, full meta-data file content</a> </li>");
		out.println("</ul>");
		out.println("</p>");
		
		out.println("<script>");
		out.println("                                                      ");
		out.println("function updateLastUpdatedClock() {                   ");
		out.println("    var ageInSec = Math.floor((new Date() - lastUpdateTime) / 1000);");
		out.println("    document.getElementById('last-update-ts').innerHTML = ageInSec + ' seconds ago'; ");
//		out.println("    console.log('updateLastUpdatedClock(): ' + document.getElementById('last-update-ts'));");
//		out.println("    console.log('updateLastUpdatedClock(): ' + ageInSec + ' seconds ago');");
		out.println("    setTimeout(updateLastUpdatedClock, 1000); ");
		out.println("}                                                     ");
		out.println("var lastUpdateTime = new Date();");
		out.println("");		
		out.println("function toggle_visibility(id) { ");
		out.println("    var e = document.getElementById(id); ");
		out.println("	 if (e.style.display == 'block') ");
		out.println("	     e.style.display = 'none'; ");
		out.println("	 else ");
		out.println("	     e.style.display = 'block'; ");
		out.println("	 } ");
		out.println("");
		out.println("</script>");
		out.println("");
		
/*
		out.println("");
		out.println("Auto update page");
		out.println("<select name='custom' id='auto-refresh' class='selectpicker box-shadow' onchange='autoRefresh();'>");
		out.println("  <option value='10'>10 seconds</option>");
		out.println("  <option value='20'>20 seconds</option>");
		out.println("  <option value='30'>30 seconds</option>");
		out.println("  <option value='40'>40 seconds</option>");
		out.println("  <option value='60'>60 seconds</option>");
		out.println("  <option value='Off' selected='selected'>Off</option>");
		out.println("</select>");
		out.println("");
		
		out.println("<script>");
		out.println("                                           ");
		out.println("function autoRefresh() {                   ");
		out.println("    var selectBox     = document.getElementById('auto-refresh'); ");
		out.println("    var selectedValue = selectBox.options[selectBox.selectedIndex].value; ");
		out.println("    console.log('autoRefresh(): selectedValue='+selectedValue);  ");
		out.println("    if (selectedValue !== 'Off') ");
		out.println("    { ");
		out.println("        setTimeout(autoRefresh, selectedValue*1000); ");
		out.println("        window.location.reload(); ");
		out.println("    } ");
		out.println("}                                          ");
		out.println("                                           ");
		out.println("</script>");
		out.println("");
*/

//		out.println("View online and historical performance charts for the monitored servers <a href='http://not-yet-implemented'>here</a><br>");
//		out.println("Download the native/desktop AseTune application <a href='http://not-yet-implemented'>here</a><br>");




		// -----------------------------------------------------------
		// get Config files from local *running* DbxTune collectors/servers
		HashMap<String, Configuration> srvInfoMap = new HashMap<>();
		for (String file : getInfoFilesDbxTune())
		{
			File f = new File(file);
			
			Configuration conf = new Configuration(file);
			String srvName     = f.getName().split("\\.")[0];

			srvInfoMap.put(srvName, conf);
		}

		// -----------------------------------------------------------
		// get entries from the Persist Reader
		LinkedHashMap<String, DbxCentralSessions> centralSessionMap = new LinkedHashMap<>();
		List<DbxCentralSessions> centralSessionList = new ArrayList<>();
		try
		{
			boolean onlyLast  = true;
			CentralPersistReader reader = CentralPersistReader.getInstance();
			centralSessionList = reader.getSessions( onlyLast, -1 );
			
			for (DbxCentralSessions s : centralSessionList)
				centralSessionMap.put(s.getServerName(), s);
		}
		catch(SQLException ex) 
		{
			out.println("Problems reading from PersistReader. Caught: "+ex);
		}


		// -----------------------------------------------------------
		// get entries from the Persist Reader (in the same ORDER as the "Landing page" 
		boolean orderedAsLandingPage = true;
		List<DbxCentralSessions>     orderedSessionList  = null;
		List<DbxCentralServerLayout> orderedServerLayout = null;
		if (orderedAsLandingPage)
		{

			String filename = StringUtil.hasValue(DbxTuneCentral.getAppConfDir()) ? DbxTuneCentral.getAppConfDir() + "/SERVER_LIST" : "conf/SERVER_LIST";
			File f = new File(filename);
			if (f.exists())
			{
				try 
				{
					orderedServerLayout = DbxCentralServerLayout.getFromFile(filename, CentralPersistReader.getInstance());
					orderedSessionList  = DbxCentralServerLayout.getServerSessions(orderedServerLayout);
				}
				catch (IOException ex)
				{
					_logger.warn("Problems reading file '" + f + "'. This is used to sort the 'sessions list'. Skipping this... Caught: " + ex);
				}
				catch(SQLException ex) 
				{
					out.println("Problems reading from PersistReader. Caught: " + ex);
				}
			}
			else
			{
				_logger.info("Sorting sessions will not be done. file '" + f + "' do not exist.");
			}
		}
			


		//----------------------------------------------------
		// User Defined Charts
		//----------------------------------------------------
		if (true)
		{
			out.println("<div id='ud-content' class='card border-dark mb-3'>");
			out.println("<h5 class='card-header'>User Defined Charts and Reports</h5>");
			out.println("<div class='card-body'>");
			
			// If no Chart was found, get instructions on HOW to create a User Defined Chart
			String templateText = UserDefinedChartManager.getInstance().getTemplateText();

			// -----------------------------------------------------------
			// Loop and print values from the Manager
			List<IUserDefinedChart> userDefinedCharts = UserDefinedChartManager.getInstance().getCharts();
			if (userDefinedCharts.isEmpty())
			{
				out.println("Below is a template how to create a User Defined Chart ");
				out.println("<a href='#' onclick=\"toggle_visibility('ud-chart-template');\">show/hide template</a> ");
				out.println("<div id='ud-chart-template' style='display:none;'>");

				out.println("<br><b>NO User Defined charts has been created.</b><br>");
				out.println("Create a file with the below content in directory: <code>" + CONF_DIR + "</code><br>");
				out.println("Name the file <code>someName.ud.content.props</code><br>");

				out.println("<hr>");
				out.println("<pre>");
				out.println(templateText);
				out.println("</pre>");
				out.println("<hr>");

				out.println("</div>");
			}
			else
			{
				out.println("<p>In here you can find various Chart(s), which is created by your organization!</p>");
				out.println("Column description");
				out.println("<ul>");
				out.println("<li><b>Name                       </b> - Name of the chart</li>");
				out.println("<li><b>Server                     </b> - Name of the server we are collecting data from</li>");
				out.println("<li><b>Chart Type                 </b> - Type of chart. (timeline-gantt/line/bar)</li>");
				out.println("<li><b>Description                </b> - Description, provided by your organization.</li>");
				out.println("<li><b>URL                        </b> - Click here to view the chart.</li>");
				out.println("<li><b>File Name                  </b> - Source file for the Chart.</li>");
				out.println("</ul>");
//				out.println("<br><hr>");
//				out.println("<h3>Active Recordings</h3>");
				out.println("<table>");
				out.println("<thead>");
				out.println("  <tr>");
				out.println("    <th>Name</th>");
				out.println("    <th>Server</th>");
				out.println("    <th>Chart Type</th>");
				out.println("    <th>Description</th>");
				out.println("    <th>URL</th>");
				out.println("    <th>File Name</th>");
				out.println("  </tr>");
				out.println("</thead>");
				out.println("<tbody>");

				for (IUserDefinedChart chart : UserDefinedChartManager.getInstance().getCharts())
				{
					String name        = chart.getName();
					String srvName     = chart.getDbmsServerName();
					String srvDesc     = chart.getDescription();
					String chartType   = chart.getChartType().toString();
					String url         = chart.getUrl(); // "/api/udc?name=" + name + "&srvName=" + srvName + "&startTime=-2");
					String filename    = chart.getConfigFilename();

					String td = chart.isValid() ? "<td>" : "<td style='color:red'>";

					out.println("  <tr>");
					out.println("    " + td + "<a href='" + url + "'>" + name + "</a></td>");
					out.println("    " + td + srvName + "</td>");
					out.println("    " + td + chartType + "</td>");
					out.println("    " + td + srvDesc + "</td>");
					out.println("    " + td + "<a href='" + url + "' target='_blank'><code>" + url + "</code></a></td>");
					out.println("    " + td + filename + "</td>");
					out.println("  </tr>");
					
//					String appName     = conf.getProperty("dbxtune.app.name", "-unknown-");
//					String srvName     = f.getName().split("\\.")[0];
//					String srvDesc     = getSrvDescription(srvName);
//					String refreshRate = conf.getProperty("dbxtune.refresh.rate", "-unknown-");
//					String url         = conf.getProperty("pcs.h2.jdbc.url");
//					String dbxTuneUrl  = dbxTuneGuiUrl + url;
	//
//					srvConfigMap.put(srvName, conf);
				}

				out.println("</tbody>");
				out.println("</table>");

			
				out.println("Below is a template how to create a User Defined Chart ");
				out.println("<a href='#' onclick=\"toggle_visibility('ud-chart-template');\">show/hide template</a> ");
				out.println("<div id='ud-chart-template' style='display:none;'>");

				out.println("<br>");
				out.println("Create a file with the below content in directory: <code>" + CONF_DIR + "</code><br>");
				out.println("Name the file <code>someName.ud.content.props</code><br>");

				out.println("<hr>");
				out.println("<pre>");
				out.println(templateText);
				out.println("</pre>");
				out.println("<hr>");

				out.println("</div>");
			}
			

			out.println("</div>"); // end: card-body
			out.println("</div>"); // end: card
		}



		//----------------------------------------------------
		// CM Refresh Time
		//----------------------------------------------------
		if (true)
		{
			out.println("<div id='cm-refresh-time' class='card border-dark mb-3'>");
			out.println("<h5 class='card-header'>Collector Refresh Time</h5>");
			out.println("<div class='card-body'>");

			out.println("<p>");
			out.println("Below is charts on <b><i>Refresh Time</i></b> for each individual DbxTune Collector<br>");
			out.println("<i>This may be used to check/enhance which CM's that may be suboptimal, and may need enhancements...<br>");
			out.println("   or how heavy DBMS Server load may impact in CM Refresh Time.</i>");
			out.println("</p>");

			// Choose what list to traverse
			List<DbxCentralSessions> sessionList = centralSessionList;
			if (orderedSessionList != null)
				sessionList = orderedSessionList;
			
			// Create "ALL" servers
			if (true)
			{
				List<String> allSrvList = new ArrayList<>();
				for (DbxCentralSessions session : sessionList)
				{
					if (session.hasStatus(DbxCentralSessions.ST_DISABLED))
					{
						_logger.info("List Active Recording: Skipping server '"+session.getServerName()+"', due to status: DISABLED.");
						continue;
					}

					String srvName   = session.getServerName();
					allSrvList.add(srvName);
				}

				// NOTE: If the below text is CHANGED, ALSO Change in method: printServerLayout_CmRefreshButtons(...)
				String link   = "/graph.html?subscribe=true&cs=dark&startTime=2h&sessionName=" + StringUtil.toCommaStr(allSrvList, ",") + "&graphList=CmSummary_CmRefreshTime&gcols=1";
				String button = "<a href='" + link + "' target='_blank' class='btn btn-sm btn-primary mb-2 mr-2' role='button'>ALL Servers</a>";

				out.println(button);
				out.println("<br>");
			}
			
			
			// And the "individual servers"
			if (orderedServerLayout != null)
			{
				printServerLayout_CmRefreshButtons(out, orderedServerLayout);
			}
			else
			{
				out.println("<br>");
				out.println("<b>Individual Servers</b>");
				
				for (DbxCentralSessions session : sessionList)
				{
					if (session.hasStatus(DbxCentralSessions.ST_DISABLED))
					{
						_logger.info("List Active Recording: Skipping server '"+session.getServerName()+"', due to status: DISABLED.");
						continue;
					}
	
					String srvName   = session.getServerName();
					String dbxType   = "dbx-button-" + session.getProductString().toLowerCase(); // dbx-button-asetune
	
					// NOTE: If the below text is CHANGED, ALSO Change in method: printServerLayout_CmRefreshButtons(...)
					String link   = "/graph.html?subscribe=true&cs=dark&startTime=2h&sessionName=" + srvName + "&graphList=CmSummary_CmRefreshTime&gcols=1";
					String button = "<a href='" + link + "' target='_blank' class='btn btn-sm btn-primary dbx-button-image " + dbxType + " mb-2 mr-2' role='button'>" + srvName + "</a>";
	
					out.println(button);
				}
			}
			
			out.println("</div>"); // end: card-body
			out.println("</div>"); // end: card
		}



		//----------------------------------------------------
		// ACTIVE Recordings
		//----------------------------------------------------
		if (true)
		{
			out.println("<div id='active' class='card border-dark mb-3'>");
			out.println("<h5 class='card-header'>Active Recordings</h5>");
			out.println("<div class='card-body'>");
			out.println("<p>What DbxTune collectors are currently running on <b>this</b> machine.</p>");
			out.println("Column description");
			out.println("<ul>");
			out.println("<li><b>AppName                    </b> - Type of DbxTune Collector</li>");
			out.println("<li><b>Server                     </b> - Name of the server we are collecting data from</li>");
			out.println("<li><b>Description                </b> - Description, fetched from the <code>conf/SERVER_LIST</code> file.</li>");
			out.println("<li><b>RefreshRate                </b> - How often does this collector fetch data</li>");
			out.println("<li><b>Age                        </b> - How many seconds since last collection was made for this server</li>");
			out.println("<li><b>isLocal                    </b> - If the collector is running on the Dbx Central host. (which enables us to view log files etc.)</li>");
			out.println("<li><b>URL                        </b> - Click here to view the <b>detailed</b> recording. Note: You must have the Native DbxTune application started on your PC/Client machine.</li>");
			out.println("<li><b>DbxTune Log File (full)    </b> - View the DbxTune log file</li>");
			out.println("<li><b>DbxTune Log File (discard) </b> - View the DbxTune log file, but discard lines containing 'Persisting Counters using', so it's easier to <i>spot</i> issues...</li>");
			out.println("<li><b>DbxTune Log File (free-text discard) </b> - View the DbxTune log file, but discard the text you typed in the input field</li>");
			out.println("</ul>");
//			out.println("<br><hr>");
//			out.println("<h3>Active Recordings</h3>");
			out.println("<table>");
			out.println("<thead>");
			out.println("  <tr>");
			out.println("    <th>AppName</th>");
			out.println("    <th>Server</th>");
			out.println("    <th>Description</th>");
			out.println("    <th>RefreshRate</th>");
			out.println("    <th>Age</th>");
			out.println("    <th>IsLocal</th>");
			out.println("    <th>URL</th>");
			out.println("    <th>DbxTune Log File (full)</th>");
			out.println("    <th>DbxTune Log File (discard)</th>");
			out.println("    <th>DbxTune Log File (free-text discard)</th>");
			out.println("  </tr>");
			out.println("</thead>");
			out.println("<tbody>");
			
			// -----------------------------------------------------------
			// Loop and print values from the Persist Reader
			for (DbxCentralSessions session : centralSessionList)
			{
				if (session.hasStatus(DbxCentralSessions.ST_DISABLED))
				{
					_logger.info("List Active Recording: Skipping server '"+session.getServerName()+"', due to status: DISABLED.");
					continue;
				}
				
//				// If last sample is older than 24 Hours lets not present it as an active recording
//				int threshold = 3600 * 24;
//				if (session.getLastSampleAgeInSec() > threshold || session.getLastSampleAgeInSec() < 0)
//				{
//					_logger.info("List Active Recording: Skipping server '"+session.getServerName()+"', due to 'old age'. " +
//							"LastSampleAgeInSec="+session.getLastSampleAgeInSec() + " ("+TimeUtils.msToTimeStr("%HH:%MM:%SS", session.getLastSampleAgeInSec()*1000)+")" +
//							", threshold="+threshold+" seconds. ("+TimeUtils.msToTimeStr("%HH:%MM:%SS", threshold*1000)+")");
//					continue;
//				}
				
				boolean isLocalCollector = session.getCollectorIsLocal();
				
				String appName     = session.getProductString();
				String srvName     = session.getServerName();
				String srvDesc     = session.getServerDescription();
				int    refreshRate = session.getCollectorSampleInterval();
				long   refreshAge  = session.getLastSampleAgeInSec();
				String collectHost = session.getCollectorHostname();
				String collectUrl  = session.getCollectorCurrentUrl();
				String url         = "jdbc:h2:tcp://"+collectHost+":19092/"+srvName+"_"+(new SimpleDateFormat("yyyy-MM-dd").format(new Date())+";IFEXISTS=TRUE");

				String logContentFull    = "NOT Local/Available";
				String logContentDiscard = "NOT Local/Available";
				String logContentFilter  = "NOT Local/Available";

				url = session.getCollectorCurrentUrl();

				if (isLocalCollector)
				{
					collectHost       = "Yes";
					logContentFull    = "<a href='/log?name="+srvName+".log'><code>"+srvName+".log</code></a>";
					logContentDiscard = "<a href='/log?name="+srvName+".log&discard=Persisting Counters using'><code>"+srvName+".log</code></a>";
					logContentFilter  = "<input type='text' placeholder='filter-out some text (regexp can be used), hit <enter> to search' class='search' size='80' style='border:none' onkeydown='openLogFileWithDiscard(this, \""+srvName+".log\")'/>";

					Configuration conf = srvInfoMap.get(srvName);
					if (conf != null)
					{
						if (conf.hasProperty("pcs.h2.jdbc.url"))
							url = conf.getProperty("pcs.h2.jdbc.url");
					}
				}

				if (StringUtil.hasValue(collectUrl))
					url = collectUrl;

				// set the correct link where to reach the H2 DB
				String dbxTuneUrl  = dbxTuneGuiUrl.replace(":PORT/", ":"+DbxTune.getGuiWebPort(appName)+"/") + url;
				
				String tdAttr = "";
				if (session.getLastSampleAgeInSec() > (session.getCollectorSampleInterval() * 5) )
					tdAttr = "style='background-color:red;'";
					
				out.println("  <tr>");
				out.println("    <td>" + appName + "</td>");
				out.println("    <td>" + srvName + "</td>");
				out.println("    <td>" + srvDesc + "</td>");
				out.println("    <td>" + refreshRate + "</td>");
				out.println("    <td "+tdAttr+">" + refreshAge + "</td>");
				out.println("    <td>" + collectHost + "</td>");
				out.println("    <td><div title='" + linkToolTip + "'><a href='" + dbxTuneUrl + "'><code>" + url + "</code></a></div></td>");
				out.println("    <td>" + logContentFull + "</td>");
				out.println("    <td>" + logContentDiscard + "</td>");
				out.println("    <td>" + logContentFilter + "</td>");
				out.println("  </tr>");
			}
			

//			for (String file : getFilesDbxTune())
//			{
//				File f = new File(file);
//				
//				Configuration conf = new Configuration(file);
//				String appName     = conf.getProperty("dbxtune.app.name", "-unknown-");
//				String srvName     = f.getName().split("\\.")[0];
//				String srvDesc     = getSrvDescription(srvName);
//				String refreshRate = conf.getProperty("dbxtune.refresh.rate", "-unknown-");
//				String url         = conf.getProperty("pcs.h2.jdbc.url");
//				String dbxTuneUrl  = dbxTuneGuiUrl + url;
	//
//				srvConfigMap.put(srvName, conf);
//				
//				out.println("  <tr>");
//				out.println("    <td>" + appName + "</td>");
//				out.println("    <td>" + srvName + "</td>");
//				out.println("    <td>" + srvDesc + "</td>");
//				out.println("    <td>" + refreshRate + "</td>");
//				out.println("    <td><div title='" + linkToolTip + "'><a href='" + dbxTuneUrl + "'><code>" + url + "</code></a></div></td>");
//				out.println("    <td><a href='/log?name="+srvName+".log'><code>"+srvName+".log</code></a></td>");
//				out.println("    <td><a href='/log?name="+srvName+".log&discard=Persisting Counters using'><code>"+srvName+".log</code></a></td>");
//				out.println("    <td><input type='text' placeholder='filter-out some text (regexp can be used), hit <enter> to search' class='search' size='80' style='border:none' onkeydown='openLogFileWithDiscard(this, \""+srvName+".log\")'/></td>");
//				out.println("  </tr>");
//				
//			}
			out.println("</tbody>");
			out.println("</table>");
			out.println("</div>"); // end: card-body
			out.println("</div>"); // end: card
		}



		//----------------------------------------------------
		// ALARMS
		//----------------------------------------------------
		if (true)
		{
			out.println("<div id='alarms' class='card border-dark mb-3'>");
			out.println("<h5 class='card-header'>Active Alarms</h5>");
			out.println("<div class='card-body'>");
			out.println("<p>Here you can view Alarm Activity. Both alarms that are currently <b>Active</b>, and the <i>Alarm Log</i> to check what has happened in the past.</p>");
			out.println("Column description");
			out.println("<ul>");
			out.println("<li><b>Server             </b> - Server that's monitored</li>");
			out.println("<li><b>Log File (history) </b> - Click <b>file</b> or <b>table</b> to view the content of the Alarm Log file. <b>file</b>=View it in a <i>raw</i> format (append <code>&discard=Text</code> to filter out <i>stuff</i>). <b>table</b>=View it in a html table, where you can sort or filter on the content. </li>");
			out.println("<li><b>Last Update        </b> - Last date the Alarm file was updated</li>");
			out.println("<li><b>Age(M:s)           </b> - Last date the Alarm file was updated in Minutes:Seconds, this is also a good indication that the collector process is <i>running</i> and doing checks.</li>");
			out.println("<li><b>Alarms             </b> - Content of the Active Alarm File. 'NO ACTIVE ALARMS.' is stating that we are in a <i>good</i> state.</li>");
			out.println("<li><b>LLL Age(H:M)       </b> - Last Log Line Age in Houres:Minutes - Just states the age of the <b>last</b> line in the Alarm Log</li>");
			out.println("<li><b>Last Log Line      </b> - Content of the <b>last</b> Alarm Log Line</li>");
			out.println("</ul>");
//			out.println("<br><hr>");
//			out.println("<h3>Active Alarms</h3>");
			out.println("<table>");
			out.println("<thead>");
			out.println("  <tr>");
			out.println("    <th>Server</th>");
			out.println("    <th>Log File (history)</th>");
			out.println("    <th>Last Updated</th>");
			out.println("    <th>Age(M:S)</th>");
			out.println("    <th>Alarms</th>");
			out.println("    <th>LLL Age(H:M)</th>");
			out.println("    <th>Last Log Line</th>");
			out.println("  </tr>");
			out.println("</thead>");
			out.println("<tbody>");

			for (DbxCentralSessions session : centralSessionList)
			{
				if (session.hasStatus(DbxCentralSessions.ST_DISABLED))
				{
					_logger.info("List Active Recording: Skipping server '"+session.getServerName()+"', due to status: DISABLED.");
					continue;
				}

				String srvName   = session.getServerName();
				String srvTdAttr = "";
				String infoFile  = session.getCollectorInfoFile();
				
				String alarmFileActive = "";
				String alarmFileLog    = "";

				String lastUpdateTs   = "-na-";
				String lastUpdateAge  = "-na-";
				String logFileTd      = "-na-";
				String fileContent    = "-na-";
				String lastLogLine    = "-na-";
				String lastLogLineAge = "-na-";
				String luTdAttr = "";

				boolean isLocalCollector = session.getCollectorIsLocal();
				if (isLocalCollector)
				{
					Configuration infoFileConf = new Configuration(infoFile);

					System.setProperty("SERVERNAME", srvName); // will be used when resolving variables in property values
					alarmFileActive = infoFileConf.getProperty(AlarmWriterToFile.PROPKEY_activeFilename, "");
					alarmFileLog    = infoFileConf.getProperty(AlarmWriterToFile.PROPKEY_logFilename   , "");

	//System.out.println("SRV='"+srvName+"', alarmFileActive="+alarmFileActive);
	//System.out.println("SRV='"+srvName+"', alarmFileLog="+alarmFileLog);

					long refreshRate = 60;
					int  refreshRateAlarmMultiplyer = 10;

					File f_alarmFileActive = new File(alarmFileActive);
					File f_alarmFileLog    = new File(alarmFileLog);

					if (_logger.isDebugEnabled())
						_logger.debug("OverviewServlet: infoFile='"+infoFile+"'[exists="+(new File(infoFile).exists())+"], alarmFileActive='"+alarmFileActive+"'[exists="+(new File(alarmFileActive).exists())+"], alarmFileLog='"+alarmFileLog+"'[exists="+(new File(alarmFileLog).exists())+"].");

//					if (StringUtil.hasValue(alarmFileActive) && f_alarmFileActive.exists())
					if (StringUtil.hasValue(alarmFileActive))
					{
						lastUpdateTs   = "" + (new Timestamp(f_alarmFileActive.lastModified()));
						lastUpdateAge  = TimeUtils.msToTimeStr("%?DD[d ]%?HH[:]%MM:%SS", System.currentTimeMillis()-f_alarmFileActive.lastModified());
//						logFileTd      = "<a href='/log?name="+f_alarmFileLog.getName()+"'>file</a> <a href='/alarmLog?name="+f_alarmFileLog.getName()+"&type=CANCEL'>table</a>: "+f_alarmFileLog.getName();
						logFileTd      = "<a href='/log?name="+f_alarmFileLog.getName()+"'>file</a> <a href='/alarmLog?name="+f_alarmFileLog.getName()+"&type=CANCEL'>table</a> <a href='/alarmLog?name="+srvName+"&age=7d&method=pcs'>pcs</a>: "+f_alarmFileLog.getName();
						fileContent    = getActiveAlarmContent(f_alarmFileActive);
						lastLogLine    = getLastLine(f_alarmFileLog);
						lastLogLineAge = getLastLineAge(lastLogLine);
						
						if (infoFileConf != null && infoFileConf.hasProperty("dbxtune.refresh.rate"))
							refreshRate = infoFileConf.getIntProperty("dbxtune.refresh.rate", 60);
						else
							refreshRate = session.getLastSampleAgeInSec();

						long alarmFileUpdateAgeSec = -1;
						if (session.getLastSampleTime() != null)
							alarmFileUpdateAgeSec = (System.currentTimeMillis() - session.getLastSampleTime().getTime()) / 1000;
						if (f_alarmFileActive.exists())
							alarmFileUpdateAgeSec = (System.currentTimeMillis() - f_alarmFileActive.lastModified()) / 1000;
						
						if ( alarmFileUpdateAgeSec > (refreshRate * refreshRateAlarmMultiplyer) )
							luTdAttr = "style='background-color:rgb(255,0,0);'";
						
						if ( ! "NO ACTIVE ALARMS.".equals(fileContent) )
							srvTdAttr = "style='background-color:rgb(255, 128, 128);'"; // SOME ALARMS == light red
						else
							srvTdAttr = "style='background-color:rgb(204, 255, 204);'"; // OK == light green
					}
					else
					{
						lastUpdateTs   = "" + session.getLastSampleTime();
						lastUpdateAge  = TimeUtils.msToTimeStr("%?DD[d ]%?HH[:]%MM:%SS", System.currentTimeMillis()-session.getLastSampleTime().getTime());
						logFileTd      = "-no-active-alarm-file-";
						logFileTd      = "<a href='/alarmLog?name="+srvName+"&age=7d&method=pcs'>pcs</a>: -no-active-alarm-file-";
						fileContent    = "-no-active-alarm-file-";
						lastLogLine    = "-no-active-alarm-file-";
						lastLogLineAge = "-no-active-alarm-file-";
						
						refreshRate = session.getLastSampleAgeInSec();

						long alarmFileUpdateAgeSec = -1;
						if (session.getLastSampleTime() != null)
							alarmFileUpdateAgeSec = (System.currentTimeMillis() - session.getLastSampleTime().getTime()) / 1000;
						if (f_alarmFileActive.exists())
							alarmFileUpdateAgeSec = (System.currentTimeMillis() - f_alarmFileActive.lastModified()) / 1000;

						if ( alarmFileUpdateAgeSec > (refreshRate * refreshRateAlarmMultiplyer) )
							luTdAttr = "style='background-color:rgb(255,0,0);'";
					}

					System.clearProperty("SERVERNAME");
				}
				else
				{
					lastUpdateTs   = "" + session.getLastSampleTime();
					lastUpdateAge  = TimeUtils.msToTimeStr("%?DD[d ]%?HH[:]%MM:%SS", System.currentTimeMillis()-session.getLastSampleTime().getTime());
					logFileTd      = "-remote-file-";
					logFileTd      = "<a href='/alarmLog?name="+srvName+"&age=7d&method=pcs'>pcs</a>: -remote-file-";
					fileContent    = "-remote-file-";
					lastLogLine    = "-remote-file-";
					lastLogLineAge = "-remote-file-";
					
				}

				out.println("  <tr> ");
				out.println("    <td "+srvTdAttr+">"      + srvName        + "</td>");
				out.println("    <td>"                    + logFileTd      + "</td>");
				out.println("    <td>"                    + lastUpdateTs   + "</td>");
				out.println("    <td "+luTdAttr+">"       + lastUpdateAge  + "</td>");
				out.println("    <td "+srvTdAttr+"><pre>" + fileContent    + "</pre></td>");
				out.println("    <td>"                    + lastLogLineAge + "</td>");
				out.println("    <td><pre>"               + lastLogLine    + "</pre></td>");
				out.println("  </tr>");

//				out.println("  <tr> <td>1</td> <td>2</td> <td>3</td> <td>4</td> <td>5</td> <td>6</td> <td>7</td> </tr>");
			}
			
//			for (String file : getFilesActiveAlarms())
//			{
//				File f = new File(file);
//				String srvName = f.getName().split("\\.")[2];
	//
//				if ("unknown".equals(srvName))
//					continue;
	//
//				String lastUpdateTs   = "" + (new Timestamp(f.lastModified()));
//				String lastUpdateAge  = TimeUtils.msToTimeStr("%?DD[d ]%?HH[:]%MM:%SS", System.currentTimeMillis()-f.lastModified());
//				File   logFile        = new File(LOG_DIR + "/ALARM.LOG." + srvName + ".log");
////				String logFileTd      = "<a href='/alarmLog?name=ALARM.LOG." + srvName + ".log&type=CANCEL'>ALARM.LOG." + srvName + ".log</a>";
//				String logFileTd      = "<a href='/log?name=ALARM.LOG." + srvName + ".log'>file</a> <a href='/alarmLog?name=ALARM.LOG." + srvName + ".log&type=CANCEL'>table</a>: ALARM.LOG." + srvName + ".log";
//				String fileContent    = getActiveAlarmContent(f);
//				String lastLogLine    = getLastLine(logFile);
////				String lastLogLineTs  = "FIXME"; //datetime.datetime.strptime( lastLogLine.split(' - ')[0], '%Y-%m-%d %H:%M:%S.%f' )
//				String lastLogLineAge = getLastLineAge(lastLogLine); //str( datetime.datetime.now() - lastLogLineTs ).split('.', 2)[0][:-3]
	//
//				long refreshRate = 60;
//				int  refreshRateAlarmMultiplyer = 10;
//				String luTdAttr = "";
//				Configuration conf = srvInfoMap.get(srvName);
//				if (conf != null)
//				{
//					refreshRate = StringUtil.parseInt(conf.getProperty("dbxtune.refresh.rate", "60"), 60);
//				}
//				else
//				{
//					DbxCentralSessions session = centralSessionMap.get(srvName);
//					if (session != null)
//					{
//						refreshRate = session.getLastSampleAgeInSec();
//					}
//				}
//				long alarmFileUpdateAgeSec = (System.currentTimeMillis() - f.lastModified()) / 1000;
//				if ( alarmFileUpdateAgeSec > (refreshRate * refreshRateAlarmMultiplyer) )
//					luTdAttr = "style='background-color:rgb(255,0,0);'";
//				
//				out.println("  <tr> ");
//				out.println("    <td>"              + srvName        + "</td>");
//				out.println("    <td>"              + logFileTd      + "</td>");
//				out.println("    <td>"              + lastUpdateTs   + "</td>");
//				out.println("    <td "+luTdAttr+">" + lastUpdateAge  + "</td>");
//				out.println("    <td><pre>"         + fileContent    + "</pre></td>");
//				out.println("    <td>"              + lastLogLineAge + "</td>");
//				out.println("    <td><pre>"         + lastLogLine    + "</pre></td>");
//				out.println("  </tr>");
//			}
			out.println("</tbody>");
			out.println("</table>");
			out.println("</div>"); // end: card-body
			out.println("</div>"); // end: card
		}



		//----------------------------------------------------
		// ALL files in LOG directory
		//----------------------------------------------------
		if (true)
		{
			out.println("<div id='logfiles' class='card border-dark mb-3'>");
			out.println("<h5 class='card-header'>All file(s) in the LOG Directory</h5>");
			out.println("<div class='card-body'>");
			out.println("<p>List all the files in the <i>log</i> directory <code>"+ (new File(LOG_DIR)) +"</code>, click the <i>Url</i> to view the content in the file.<br>");
			out.println("Note: If you want to filter out something from the content, Type it in the column <b>Discard Text</b> and hit <i>enter</i><br>");
			out.println("Column <b>View Options</b>");
			out.println("<ul>");
			out.println("  <li><code>plain        </code> - View all records in the file.</li>");
			out.println("  <li><code>discard      </code> - All records except <code>'Persisting Counters using|Sent subscription data for server'</code>.</li>");
			out.println("  <li><code>tail         </code> - Display last 500 records, and <i>follow</i> output as the file grows.</li>");
			out.println("  <li><code>tail+discard </code> - The 'tail' and 'discard' option combined.</li>");
			out.println("</ul>");
			out.println("</p>");

			List<String> shortcutLogfilesList = StringUtil.commaStrToList( Configuration.getCombinedConfiguration().getProperty(PROPKEY_LogfilesShortcuts, DEFAULT_LogfilesShortcuts) );
			if ( ! shortcutLogfilesList.isEmpty() )
			{
//				List<String> allFilesInLogDir = getFilesInLogDir();

				for (String shortcutEntry : shortcutLogfilesList)
				{
					// Strip of char '*' if name starts with "*."
					if (shortcutEntry.startsWith("*."))
						shortcutEntry = shortcutEntry.substring(1);

//					out.println("<b>Shortcuts for files ending with '" + shortcutEntry + "'</b>");
					out.println("<b>Shortcuts for files ending with '" + shortcutEntry + "'</b>. Or click <a href='javascript:openTailOnAllConsoleFiles()'>here</a> to open ALL below '" + shortcutEntry + "' files in tail mode. (a new tab for each file)");

					List<String> listOfFiles = new ArrayList<>();

					out.println("<table>");
					out.println("<table>");
					out.println("<thead>");
					out.println("  <tr>");
					out.println("    <th>File</th>");
					out.println("    <th>View Options</th>");
					out.println("    <th>Size GB</th>");
					out.println("    <th>Size MB</th>");
					out.println("    <th>Size KB</th>");
					out.println("    <th>Last Modified</th>");
					out.println("    <th>Discard Text</th>");
					out.println("  </tr>");
					out.println("</thead>");
					out.println("<tbody>");

					for (String file : getFilesInLogDir())
					{
						File f = new File(file);
						if (f.isDirectory())
							continue;

						if ( ! f.getName().endsWith(shortcutEntry) )
							continue;
						
						listOfFiles.add(f.getName());
						
						String sizeInGB = String.format("%.1f GB", f.length() / 1024.0 / 1024.0 / 1024.0);
						String sizeInMB = String.format("%.1f MB", f.length() / 1024.0 / 1024.0);
						String sizeInKB = String.format("%.1f KB", f.length() / 1024.0);
						
						String urlDiscardStr = "&discard=Persisting Counters using|Sent subscription data for server";
						out.println("  <tr>");
						out.println("    <td><a href='/log?name="+f.getName()+"'>"+f.getName()+"</a></td>");
						out.println("    <td>");
						out.println("      <a href='/log?name="+f.getName()+"'>plain</a>");
						out.println("      | <a href='/log?name="+f.getName()+urlDiscardStr+"'>discard</a>");
						out.println("      | <a href='/log?name="+f.getName()+"&tail=5000'>tail</a>");
						out.println("      | <a href='/log?name="+f.getName()+urlDiscardStr+"&tail=5000'>tail+discard</a>");
						out.println("    </td>");
						out.println("    <td>" + sizeInGB     + "</td>");
						out.println("    <td>" + sizeInMB     + "</td>");
						out.println("    <td>" + sizeInKB     + "</td>");
						out.println("    <td>" + (new Timestamp(f.lastModified())) + "</td>");
						out.println("    <td><input type='text' placeholder='filter-out some text (regexp can be used), hit <enter> to search' class='search' size='80' style='border:none' onkeydown='openLogFileWithDiscard(this, \""+f.getName()+"\")'/></td>");
						out.println("  </tr>");

					}
					out.println("</tbody>");
					out.println("</table>");
					out.println("<br>");

					// Build function that will open all files in tail mode
					out.println("");
					out.println("<script type='text/javascript'>");
//					out.println("    function openTailOnAllConsoleFiles() ");
//					out.println("    { ");
//					for (String fname : listOfFiles)
//						out.println("        window.open('/log?name=" + fname + "&tail=5000'); ");
//					out.println("    } ");
					out.println("    function openTailOnAllConsoleFiles() ");
					out.println("    { ");
					out.println("        let tmpArray = [];");

					for (String fname : listOfFiles)
						out.println("        tmpArray.push('" + fname + "'); ");

					out.println("");
					out.println("        for (let i = 0; i < tmpArray.length; i++) ");
					out.println("        { ");
					out.println("            setTimeout(function() ");
					out.println("            { ");
					out.println("                window.open('/log?name=' + tmpArray[i] + '&tail=5000'); ");
					out.println("            }, i * 1000); ");
					out.println("        } ");
					out.println("    } ");
					out.println("</script>");
					out.println("");
				}
			}

			
//			out.println("<table>");
//			out.println("<thead>");
//			out.println("  <tr>");
//			out.println("    <th>File</th>");
//			out.println("    <th>Size GB</th>");
//			out.println("    <th>Size MB</th>");
//			out.println("    <th>Size KB</th>");
//			out.println("    <th>Tail-f (last 500)</th>");
//			out.println("    <th>Tail-f (last 500 + discard)</th>");
//			out.println("    <th>Show (discard some)</th>");
//			out.println("    <th>Discard Text</th>");
//			out.println("  </tr>");
//			out.println("</thead>");
//			out.println("<tbody>");
//			for (String file : getFilesInLogDir())
//			{
//				File f = new File(file);
//				if (f.isDirectory())
//					continue;
	//
//				String sizeInGB = String.format("%.1f GB", f.length() / 1024.0 / 1024.0 / 1024.0);
//				String sizeInMB = String.format("%.1f MB", f.length() / 1024.0 / 1024.0);
//				String sizeInKB = String.format("%.1f KB", f.length() / 1024.0);
//				
//				String urlDiscardStr = "&discard=Persisting Counters using|Sent subscription data for server";
//				out.println("  <tr>");
//				out.println("    <td><a href='/log?name="+f.getName()+"'>"+f.getName()+"</a></td>");
//				out.println("    <td>" + sizeInGB     + "</td>");
//				out.println("    <td>" + sizeInMB     + "</td>");
//				out.println("    <td>" + sizeInKB     + "</td>");
//				out.println("    <td><a href='/log?name="+f.getName()+"&tail=5000'><code>"+f.getName()+"</code></a></td>");
//				out.println("    <td><a href='/log?name="+f.getName()+urlDiscardStr+"&tail=5000'><code>"+f.getName()+"</code></a></td>");
//				out.println("    <td><a href='/log?name="+f.getName()+urlDiscardStr+"'><code>"+f.getName()+"</code></a></td>");
//				out.println("    <td><input type='text' placeholder='filter-out some text (regexp can be used), hit <enter> to search' class='search' size='80' style='border:none' onkeydown='openLogFileWithDiscard(this, \""+f.getName()+"\")'/></td>");
//				out.println("  </tr>");
	//
//			}
//			out.println("</tbody>");
//			out.println("</table>");

			out.println("<b>All files in the log directory</b>");
			
			out.println("<table>");
			out.println("<thead>");
			out.println("  <tr>");
			out.println("    <th>File</th>");
			out.println("    <th>View Options</th>");
			out.println("    <th>Size GB</th>");
			out.println("    <th>Size MB</th>");
			out.println("    <th>Size KB</th>");
			out.println("    <th>Last Modified</th>");
			out.println("    <th>Discard Text</th>");
			out.println("  </tr>");
			out.println("</thead>");
			out.println("<tbody>");
			for (String file : getFilesInLogDir())
			{
				File f = new File(file);
				if (f.isDirectory())
					continue;

				String sizeInGB = String.format("%.1f GB", f.length() / 1024.0 / 1024.0 / 1024.0);
				String sizeInMB = String.format("%.1f MB", f.length() / 1024.0 / 1024.0);
				String sizeInKB = String.format("%.1f KB", f.length() / 1024.0);
				
				String urlDiscardStr = "&discard=Persisting Counters using|Sent subscription data for server";
				out.println("  <tr>");
				out.println("    <td><a href='/log?name="+f.getName()+"'>"+f.getName()+"</a></td>");
				out.println("    <td>");
				out.println("      <a href='/log?name="+f.getName()+"'>plain</a>");
				out.println("      | <a href='/log?name="+f.getName()+urlDiscardStr+"'>discard</a>");
				out.println("      | <a href='/log?name="+f.getName()+"&tail=5000'>tail</a>");
				out.println("      | <a href='/log?name="+f.getName()+urlDiscardStr+"&tail=5000'>tail+discard</a>");
				out.println("    </td>");
				out.println("    <td>" + sizeInGB     + "</td>");
				out.println("    <td>" + sizeInMB     + "</td>");
				out.println("    <td>" + sizeInKB     + "</td>");
				out.println("    <td>" + (new Timestamp(f.lastModified())) + "</td>");
				out.println("    <td><input type='text' placeholder='filter-out some text (regexp can be used), hit <enter> to search' class='search' size='80' style='border:none' onkeydown='openLogFileWithDiscard(this, \""+f.getName()+"\")'/></td>");
				out.println("  </tr>");

			}
			out.println("</tbody>");
			out.println("</table>");
			out.println("</div>"); // end: card-body
			out.println("</div>"); // end: card

			out.println("<script>");
			out.println("function openLogFileWithDiscard(element, filename) {  ");
			out.println("    if (event.key === 'Enter') { ");
			out.println("        window.open('/log?name='+filename+'&discard='+element.value, '_self')");
			out.println("    }");
			out.println("}                                                     ");
			out.println("</script>");
			out.println("");
		}


		//----------------------------------------------------
		// ALL files in CONF directory
		//----------------------------------------------------
		if (true)
		{
			out.println("<div id='conffiles' class='card border-dark mb-3'>");
			out.println("<h5 class='card-header'>All file(s) in the CONF Directory</h5>");
			out.println("<div class='card-body'>");
			out.println("<p>List all the files in the <i>conf</i> directory <code>"+ (new File(CONF_DIR)) +"</code>, click the <i>Url</i> to view the content in the file.<br>");
			out.println("Note: If you want to filter out something from the content, Type it in the column <b>Discard Text</b> and hit <i>enter</i></p>");
			out.println("</p>");

			out.println("<table>");
			out.println("<thead>");
			out.println("  <tr>");
			out.println("    <th>File</th>");
			out.println("    <th>Discard Text</th>");
			out.println("  </tr>");
			out.println("</thead>");
			out.println("<tbody>");
			for (File f : getFilesInConfDir())
			{
				if (f.isDirectory())
					continue;

				out.println("  <tr>");
				out.println("    <td><a href='/conf?name="+f.getName()+"'>"+f.getName()+"</a></td>");
//				out.println("    <td><a href='/log?name="+f.getName()+"'><code>"+f.getName()+"</code></a></td>");
				out.println("    <td><input type='text' placeholder='filter-out some text (regexp can be used), hit <enter> to search' class='search' size='80' style='border:none' onkeydown='openConfFileWithDiscard(this, \""+f.getName()+"\")'/></td>");
				out.println("  </tr>");

			}
			out.println("</tbody>");
			out.println("</table>");
			out.println("</div>"); // end: card-body
			out.println("</div>"); // end: card

			out.println("<script>");
			out.println("function openConfFileWithDiscard(element, filename) {  ");
			out.println("    if (event.key === 'Enter') { ");
			out.println("        window.open('/conf?name='+filename+'&discard='+element.value, '_self')");
			out.println("    }");
			out.println("}                                                     ");
			out.println("</script>");
			out.println("");
		}


		//----------------------------------------------------
		// ALL files in REPORTS directory
		//----------------------------------------------------
		String firstServerName = null;
		if (true)
		{
			out.println("<div id='reportfiles' class='card border-dark mb-3'>");
			out.println("<h5 class='card-header'>All file(s) in the REPORTS Directory</h5>");
			out.println("<div class='card-body'>");
			out.println("<p>List all the files in the <i>reports</i> directory <code>"+ (new File(REPORTS_DIR)) +"</code>, click the <i>Url</i> to view the content in the file.<br>");
			out.println("</p>");
			out.println("<p>File name is in the format: <i>srv</i>.<i>date(YYYY-MM-DD)</i>_<i>time(HHMM)</i>[.-NTR-].html<br>");
			out.println("Note: The file is created after midnight and reflects the <b>previous</b> day...<br>");
			out.println("Note: The <code>-NTR-</code> mark is just that it has 'Nothing To Report' and is only appended if so.<br>");
			out.println("</p>");

			int removeAfterDays = Configuration.getCombinedConfiguration().getIntProperty(DailySummaryReportFactory.PROPKEY_removeReportsAfterDays, DailySummaryReportFactory.DEFAULT_removeReportsAfterDays);
			out.println("<p>");
			out.println("Report files will be removed after " + removeAfterDays + " days. Which can be changed with property <code>" + DailySummaryReportFactory.PROPKEY_removeReportsAfterDays + " = ##</code>");
			out.println("</p>");

			// Create a Link to "latest" report for each SERVER
			out.println("<p>");
			out.println("<b>Direct link to the latest report for server:</b>");
			out.println("<ul>");
			out.println("  <li> <a href='/report?op=viewLatest&name=DbxCentral'>DbxCentral</a> </li>");
			for (DbxCentralSessions session : centralSessionList)
			{
				if (firstServerName == null)
					firstServerName = session.getServerName();

				if (firstServerName == null)
					firstServerName = "-unknown-";

				out.println("  <li> <a href='/report?op=viewLatest&name=" + session.getServerName() + "'>" + session.getServerName() + "</a> </li>");
			}
			out.println("</ul>");
			out.println("</p>");

			
			String tableHead 
				= "  <tr id='dsr-srv-SRVNAME'>"
				+ "    <th>Server Name</th>"
				+ "    <th>Report For Date</th>"
				+ "    <th>DayOfWeek</th>"
				+ "    <th>NTR</th>"
				+ "    <th>File</th>"
				+ "    <th>Size MB</th>"
				+ "    <th>Remove Report</th>"
				+ "  </tr>";

			if (firstServerName == null)
				firstServerName = "-unknown-";

			out.println("<table>");
			out.println("<thead>");
			out.println(tableHead.replace("SRVNAME", firstServerName));
			out.println("</thead>");
			out.println("<tbody>");

			String prevSrvName = null;

			for (String file : getFilesInReportsDir())
			{
				File f = new File(file);
				if (f.isDirectory())
					continue;
				
				String filename   = f.getName();
				String srvName    = "";
				String reportDate = "";
				//String reportTime = "";
				String reportDow  = "";
				boolean ntr = filename.indexOf(".-NTR-") >= 0;
				String reportSizeMb = NumberUtils.toMb( f.length(), 1) + "";

				int firstDot = filename.indexOf('.');
				int lastDot  = filename.lastIndexOf('.');
				if (firstDot >= 0 && lastDot >= 0)
				{
					srvName    = filename.substring(0, firstDot);
					reportDate = filename.substring(firstDot+1, lastDot);
					
					int sep = reportDate.indexOf('_');
					if (sep >= 0)
					{
						//reportTime = reportDate.substring(sep+1);  // NOTE: .-NTR- can still be in here... so for the moment it's not a *pure* time
						reportDate = reportDate.substring(0, sep);

						try 
						{
							// Parse the YYYY-MM-DD into a date object
							SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
							Date parsedDate = sdf.parse(reportDate);

							// subtract 1 day (sine it's the *actual* day that the report reflects)
							Date minusOneDay = DateUtils.addDays(parsedDate, -1);
							reportDate = sdf.format(minusOneDay);
							
							// get the DayOfWeek for this report
							reportDow = getDayOfWeek(reportDate, "yyyy-MM-dd");
						} 
						catch (ParseException ex) 
						{
							_logger.info("Problems parsing Report Date '" + reportDate + "'.");
						}
					}
				}

				// Add empty row when it's a new server
				if (prevSrvName != null && ! prevSrvName.equals(srvName))
				{
					out.println("  <tr>");
					out.println("    <td nowrap colspan='6' style='background-color:rgb(209, 224, 224);'>&nbsp;</td>");
					out.println("  </tr>");
					out.println(tableHead.replace("SRVNAME", srvName)); // Also add new "headers" so we don't have to scroll that much
				}
				prevSrvName = srvName;

				out.println("  <tr>");
				out.println("    <td>" + srvName + "</td>");
				out.println("    <td>" + reportDate + "</td>");
				out.println("    <td>" + reportDow  + "</td>");
				out.println("    <td>" + (ntr ? "<font color='green'>NTR</font>" : "")  + "</td>");
				out.println("    <td><a href='/report?op=view&name="   + filename + "'>"         + filename + "</a></td>");
				out.println("    <td>" + reportSizeMb  + "</td>");
				out.println("    <td><a href='/report?op=remove&name=" + filename + "'>Remove: " + filename + "</a></td>");
				out.println("  </tr>");
			}
			out.println("</tbody>");
			out.println("</table>");
			out.println("</div>"); // end: card-body
			out.println("</div>"); // end: card

			out.println("");
		}


		//----------------------------------------------------
		// DBX Central Databases
		//----------------------------------------------------
		if (true)
		{
			out.println("<div id='central' class='card border-dark mb-3'>");
			out.println("<h5 class='card-header'>Dbx Central database</h5>");
			out.println("<div class='card-body'>");

			out.println("<p>H2 Databases used by Dbx Central</p>");

			File dataDir = new File(DbxTuneCentral.getAppDataDir());
			File dataDirRes = null;
			try { dataDirRes = dataDir.toPath().toRealPath().toFile(); } catch(IOException ex) { _logger.warn("Problems resolving File->Path->File");}

//			double freeGb   = dataDir.getFreeSpace()   / 1024.0 / 1024.0 / 1024.0;
			double freeGb   = dataDir.getUsableSpace() / 1024.0 / 1024.0 / 1024.0;
//			double usableGb = dataDir.getUsableSpace() / 1024.0 / 1024.0 / 1024.0;
			double totalGb  = dataDir.getTotalSpace()  / 1024.0 / 1024.0 / 1024.0;
			double pctUsed  = 100.0 - (freeGb / totalGb * 100.0);
			
			out.println("<p>");
			out.println("File system usage at '"+dataDir+"', resolved to '"+dataDirRes+"'.<br>");
			out.println(String.format("Free = %.1f GB, Total = %.1f GB, Percent Used = %.1f %%<br>", freeGb, totalGb, pctUsed));
			out.println("</p>");
			
			H2StorageInfo h2StorageInfo = CentralH2Defrag.getH2StorageInfo();
			
			out.println("<table>");
			out.println("<thead>");
			out.println("  <tr>");
			out.println("    <th>File</th>");
			out.println("    <th>Size GB</th>");
			out.println("    <th>Size MB</th>");
			out.println("    <th>Saved Info</th>");
			out.println("    <th>JDBC Url</th>");
			out.println("    <th>WEB Url</th>");
			out.println("  </tr>");
			out.println("</thead>");
			out.println("<tbody>");
			
			for (String file : getFilesH2Dbs(H2DbFileType.DBX_CENTRAL))
			{
				File f = new File(file);

				String dbName = f.getName().split("\\.")[0];
				
				String srvName = dbName;
				if (dbName.matches(".*_[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]"))
				{
					srvName = dbName.substring(0, dbName.length()-"_yyyy-MM-dd".length());
				}
				String collectorHostname = hostname;
				DbxCentralSessions session = centralSessionMap.get(srvName);
				if (session != null)
					collectorHostname = session.getCollectorHostname(); 

				String jdbcUrl  = "jdbc:h2:tcp://" + collectorHostname + "/" + dbName + ";IFEXISTS=TRUE;DB_CLOSE_ON_EXIT=FALSE";
				String webUrl   = "http://"+collectorHostname+":8082" + "/login.jsp?url=" + jdbcUrl + "&driver=org.h2.Driver&user=sa";
				
				String sizeInGB = String.format("%.1f GB", f.length() / 1024.0 / 1024.0 / 1024.0);
				String sizeInMB = String.format("%.1f MB", f.length() / 1024.0 / 1024.0);
				
				out.println("  <tr>");
				out.println("    <td>" + dbName    + "</td>");
				out.println("    <td>" + sizeInGB  + "</td>");
				out.println("    <td>" + sizeInMB  + "</td>");
				out.println("    <td>" + h2StorageInfo  + "</td>");
				out.println("    <td><code>" + jdbcUrl + "</code></td>");
//				out.println("    <td><code>" + webUrl + "</code></td>");
				out.println("    <td><div title='Open a web query tool'><a href='" + webUrl + "'><code>" + webUrl + "</code></a></div></td>");
				out.println("  </tr>");
			}
			out.println("</tbody>");
			out.println("</table>");
			
			// Print detailes about how many MB we are consumimg per minute/hour
			if (h2StorageInfo != null)
			{
			}
			

			String fn = "";
			String urlDiscardStr = "&discard=Persisting Counters using|Sent subscription data for server";
			
			out.println("<p>");
			out.println("<br>");
			out.println("Quick links to Some Dbx Central log files.");
			out.println("<ul>");
			fn = "DBX_CENTRAL.console";                   out.println("  <li><a href='/log?name=" + fn + "'>plain</a> | <a href='/log?name=" + fn+urlDiscardStr + "'>discard</a> | <a href='/log?name=" + fn + "&tail=5000'>tail</a> | <a href='/log?name=" + fn+urlDiscardStr + "&tail=5000'>tail+discard</a> &#8680; <a href='/log?name="+fn+"'>"+fn+"</a> </li>");
			fn = "DBX_CENTRAL.log";                       out.println("  <li><a href='/log?name=" + fn + "'>plain</a> | <a href='/log?name=" + fn+urlDiscardStr + "'>discard</a> | <a href='/log?name=" + fn + "&tail=5000'>tail</a> | <a href='/log?name=" + fn+urlDiscardStr + "&tail=5000'>tail+discard</a> &#8680; <a href='/log?name="+fn+"'>"+fn+"</a> </li>");
			fn = "DBX_CENTRAL_H2WriterStatCronTask.log";  out.println("  <li><a href='/log?name=" + fn + "'>plain</a> | <a href='/log?name=" + fn+urlDiscardStr + "'>discard</a> | <a href='/log?name=" + fn + "&tail=5000'>tail</a> | <a href='/log?name=" + fn+urlDiscardStr + "&tail=5000'>tail+discard</a> &#8680; <a href='/log?name="+fn+"'>"+fn+"</a> </li>");
			fn = "DBX_CENTRAL_CentralH2Defrag.log";       out.println("  <li><a href='/log?name=" + fn + "'>plain</a> | <a href='/log?name=" + fn+urlDiscardStr + "'>discard</a> | <a href='/log?name=" + fn + "&tail=5000'>tail</a> | <a href='/log?name=" + fn+urlDiscardStr + "&tail=5000'>tail+discard</a> &#8680; <a href='/log?name="+fn+"'>"+fn+"</a> </li>");
			fn = "DBX_CENTRAL_DataDirectoryCleaner.log";  out.println("  <li><a href='/log?name=" + fn + "'>plain</a> | <a href='/log?name=" + fn+urlDiscardStr + "'>discard</a> | <a href='/log?name=" + fn + "&tail=5000'>tail</a> | <a href='/log?name=" + fn+urlDiscardStr + "&tail=5000'>tail+discard</a> &#8680; <a href='/log?name="+fn+"'>"+fn+"</a> </li>");
			fn = "DBX_CENTRAL_CentralPcsJdbcCleaner.log"; out.println("  <li><a href='/log?name=" + fn + "'>plain</a> | <a href='/log?name=" + fn+urlDiscardStr + "'>discard</a> | <a href='/log?name=" + fn + "&tail=5000'>tail</a> | <a href='/log?name=" + fn+urlDiscardStr + "&tail=5000'>tail+discard</a> &#8680; <a href='/log?name="+fn+"'>"+fn+"</a> </li>");
//			out.println("  <li><a href='/log?name=DBX_CENTRAL.console'>                   DBX_CENTRAL.console                   </a></li>");
//			out.println("  <li><a href='/log?name=DBX_CENTRAL.log'>                       DBX_CENTRAL.log                       </a></li>");
//			out.println("  <li><a href='/log?name=DBX_CENTRAL_CentralH2Defrag.log'>       DBX_CENTRAL_CentralH2Defrag.log       </a></li>");
//			out.println("  <li><a href='/log?name=DBX_CENTRAL_DataDirectoryCleaner.log'>  DBX_CENTRAL_DataDirectoryCleaner.log  </a></li>");
//			out.println("  <li><a href='/log?name=DBX_CENTRAL_CentralPcsJdbcCleaner.log'> DBX_CENTRAL_CentralPcsJdbcCleaner.log </a></li>");
			out.println("</ul>");
			out.println("</p>");

			fn = "DBX_CENTRAL_H2WriterStatCronTask.log";
			long numOfDays = 30;
//			String startDate = (new Timestamp(System.currentTimeMillis() - (1000*3600*24*numOfDays) )).toString().substring(0, "2019-01-01 HH:mm:ss".length());
//			String endDate   = (new Timestamp(System.currentTimeMillis())).toString().substring(0, "2019-01-01 HH:mm:ss".length());
			String startDate = TimeUtils.toStringIso8601( new Timestamp( System.currentTimeMillis() - (1000*3600*24*numOfDays) ) ).substring(0, "2019-01-01T".length()) + "00:00"; // Copy first part "YYYY-MM-DDT" then add "00:00" start of day
			String endDate   = TimeUtils.toStringIso8601( new Timestamp( System.currentTimeMillis()                            ) ).substring(0, "2019-01-01T".length()) + "23:59"; // Copy first part "YYYY-MM-DDT" then add "23:59" end of day
			out.println("<a href='/h2ws?filename=" + fn + "&startDate=" + startDate + "&endDate=" + endDate + "'>Show a Chart of H2 Read/Write Statistics for last " + numOfDays + " day</a><br>");
//			out.println("<p><br></p>");

			out.println("<br>");
			out.println("<a href='graph.html?subscribe=false&cs=dark&startTime=4h&sessionName=DbxCentral'                     target='_blank'>Show Local DbxCentral (OS) Metrics <b>pre selected</b>, for last 4 hours, in new tab.</a><br>");
			out.println("<a href='graph.html?subscribe=false&cs=dark&startTime=4h&sessionName=DbxcLocalMetrics&graphList=all' target='_blank'>Show Local DbxCentral (OS) Metrics <b>all graphs</b>, for last 4 hours, in new tab.</a><br>");
			out.println("<br>");
			
			
			// Print some content of the Central Database
			if (CentralPersistReader.hasInstance())
			{
				CentralPersistReader reader = CentralPersistReader.getInstance();
				DbxConnection conn = null;
				String sql = null;
				try
				{
					conn = reader.getConnection();

//					String lq = conn.getLeftQuote();  // Note no replacement is needed, since we get it from the connection
//					String rq = conn.getRightQuote(); // Note no replacement is needed, since we get it from the connection
//
//					sql = ""
//					    + "select \n"
//					    + "	   " + lq + "ServerName"    + rq + ", \n"
//					    + "	   " + lq + "OnHostname"    + rq + ", \n"
//					    + "	   " + lq + "ProductString" + rq + ", \n"
//					    + "	   min(" + lq + "SessionStartTime" + rq + ") as " + lq + "FirstSample" + rq + ", \n"
//					    + "	   max(" + lq + "LastSampleTime"   + rq + ") as " + lq + "LastSample"  + rq + ", \n"
////					    + "	   sum(" + lq + "NumOfSamples"     + rq + ") as " + lq + "NumOfSamples" + rq + ", \n" // Note this might be off/faulty after a: Data Retention Cleanup
//					    + "	   datediff(day, max(" + lq + "LastSampleTime"   + rq + "), CURRENT_TIMESTAMP)   as " + lq + "LastSampleAgeInDays" + rq + ", \n"
//					    + "	   datediff(day, min(" + lq + "SessionStartTime" + rq + "), max(" + lq + "LastSampleTime" + rq + ")) as " + lq + "NumOfDaysSampled" + rq + " \n"
//					    + "from " + lq + "DbxCentralSessions" + rq + " \n"
//					    + "group by " + lq + "ServerName" + rq + ", " + lq + "OnHostname" + rq + ", " + lq + "ProductString" + rq + " \n"
//					    + "order by 1 \n"
//					    + "";
					
					sql = ""
					    + "select \n"
					    + "	   [ServerName], \n"
					    + "	   [OnHostname], \n"
					    + "	   [ProductString], \n"
					    + "	   min([SessionStartTime]) as [FirstSample], \n"
					    + "	   max([LastSampleTime])   as [LastSample], \n"
//					    + "	   sum([NumOfSamples])     as [NumOfSamples], \n" // Note this might be off/faulty after a: Data Retention Cleanup
					    + "	   datediff(day, max([LastSampleTime]),   CURRENT_TIMESTAMP    ) as [LastSampleAgeInDays], \n"
					    + "	   datediff(day, min([SessionStartTime]), max([LastSampleTime])) as [NumOfDaysSampled] \n"
					    + "from [DbxCentralSessions] \n"
					    + "group by [ServerName], [OnHostname], [ProductString] \n"
					    + "order by 1 \n"
					    + "";

					// change '[' and ']' into DBMS specific Quoted Identifier Chars
					sql = conn.quotifySqlString(sql);
					
					try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
					{
						out.println("<p>");
						out.println("What <i>sessions</i> are stored in the Dbx Central database");
						out.println("</p>");
						
						out.println("<table>");
						out.println("<thead>");
						out.println("  <tr>");
						out.println("    <th>Server Name</th>");
						out.println("    <th>On Host</th>");
						out.println("    <th>Collector Type</th>");
						out.println("    <th>First Sample</th>");
						out.println("    <th>Last Sample</th>");
//						out.println("    <th>Number Of Samples</th>");
						out.println("    <th>Last Sample Age In Days</th>");
						out.println("    <th>Num Of Days Sampled</th>");
						out.println("  </tr>");
						out.println("</thead>");
						out.println("<tbody>");

						while (rs.next())
						{
							String luTdAttr = "";
							int lastSampleAgeInDays = rs.getInt("LastSampleAgeInDays");

							if ( lastSampleAgeInDays > 0 )
								luTdAttr = "style='background-color:rgb(255,0,0);'";
							
							int c=1;
							out.println("  <tr>");
							out.println("    <td>"              + rs.getString(c++) + "</td>");
							out.println("    <td>"              + rs.getString(c++) + "</td>");
							out.println("    <td>"              + rs.getString(c++) + "</td>");
							out.println("    <td>"              + rs.getString(c++) + "</td>");
							out.println("    <td>"              + rs.getString(c++) + "</td>");
//							out.println("    <td>"              + rs.getString(c++) + "</td>");
							out.println("    <td "+luTdAttr+">" + rs.getString(c++) + "</td>");
							out.println("    <td>"              + rs.getString(c++) + "</td>");
							out.println("  </tr>");
						}

						out.println("</tbody>");
						out.println("</table>");
					}
				}
				catch(SQLException ex)
				{
					out.println("Problems getting session Content from Dbx Central<br>");
					out.println("SQL Issued<br>");
					out.println("<pre>" + sql + "</pre>");
					out.println("SQL Exception<br>");
					out.println("<pre>" + ex + "</pre>");
					out.println("Stacktrace<br>");
					out.println("<pre>" + StringUtil.stackTraceToString(ex) + "</pre>");
					
				}
				finally 
				{
					if (reader  != null && conn != null)
						reader.releaseConnection(conn);
				}
				
			}

			out.println("</div>"); // end: card-body
			out.println("</div>"); // end: card
		}

		
		//----------------------------------------------------
		// OFFLINE Databases
		//----------------------------------------------------
		String dsrCurrentJdbcUrl = "";
		if (true)
		{
			boolean isDownloadRecordingEnabled = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_enableDownloadRecordings, DEFAULT_enableDownloadRecordings);

			out.println("<div id='offline' class='card border-dark mb-3'>");
	        out.println("<h5 class='card-header'>Available offline databases</h5>");
	        out.println("<div class='card-body'>");
	        out.println("<p>Historical database recordings.</p>");

	        if (isDownloadRecordingEnabled)
	        	out.println("<p>To download a recording, simply click the 'File' column. To disable download set property: <code>" + PROPKEY_enableDownloadRecordings + " = false</code></p>");
	        else
	        	out.println("<p>Download Recordings is <b>not</b> enabled. This can be enabled with property: <code>" + PROPKEY_enableDownloadRecordings + " = true</code></p>");
	        
	        out.println("Column description");
	        out.println("<ul>");
	        out.println("<li><b>File             </b> - Name of the database file</li>");
	        out.println("<li><b>DayOfWeek        </b> - What day of the week is this recording for (just extract the YYYY-MM-DD and try to convert it into a day of week)</li>");
	        out.println("<li><b>Saved Max GB     </b> - Maximum size of the File before it was <i>compressed</i> using <code>shutdown defrag</code>, which is done with with <i>PCS H2 <b>rollover</b></i>. The value is updated by DataDirectoryCleaner.check(), when it's executed by the scheduler (default; at 23:54). This value is also the one used when calulating how much space we need for H2 databases in the next 24 hours. If the value is negative, no <i>max</i> value has yet been found/saved.</li>");
	        out.println("<li><b>File Size GB     </b> - Current File size in GB</li>");
	        out.println("<li><b>File Size MB     </b> - Current File size in MB</li>");
	        out.println("<li><b>Shrink Size GB   </b> - Difference in SavedGB-CurrentGB, which is how much space is saved by doing 'shutdown defrag' when closing the db on 'rollover'.</li>");
	        out.println("<li><b>URL              </b> - Click here to view the <b>detailed</b> recording. Note: You must have the Native DbxTune application started on your PC/Client machine.</li>");
	        out.println("<li><b>DSR              </b> - Click here to create a <b>Daily Summary Report</b> from this recording. Note: You can choose detailes like begin and end time for the Reporting Period.</li>");
	        out.println("</ul>");
	        out.println("<p>Note: Offline databases with <b>todays</b> timestamp will be marked in <span style='background-color:rgb(204, 255, 204);'>light green</span>, which probably is the active recording.</p>");

			File dataDir = new File(DbxTuneCentral.getAppDataDir());
			File dataDirRes = null;
			try { dataDirRes = dataDir.toPath().toRealPath().toFile(); } catch(IOException ex) { _logger.warn("Problems resolving File->Path->File");}

			double freeGb   = dataDir.getUsableSpace() / 1024.0 / 1024.0 / 1024.0;
//			double freeGb   = dataDir.getFreeSpace()   / 1024.0 / 1024.0 / 1024.0;
//			double usableGb = dataDir.getUsableSpace() / 1024.0 / 1024.0 / 1024.0;
			double totalGb  = dataDir.getTotalSpace()  / 1024.0 / 1024.0 / 1024.0;
			double pctUsed  = 100.0 - (freeGb / totalGb * 100.0);
			
			long       sumH2RecordingsUsageMb = DataDirectoryCleaner.getH2RecodingFileSizeMb();
			BigDecimal sumH2RecordingsUsageGb = new BigDecimal( sumH2RecordingsUsageMb /1024.0 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);

			out.println("<p>");
			out.println("File system usage at '"+dataDir+"', resolved to '"+dataDirRes+"'.<br>");
//			out.println(String.format("Free = %.1f GB, Usable = %.1f GB, Total = %.1f GB <br>", freeGb, usableGb, totalGb));
			out.println(String.format("Free = %.1f GB, Total = %.1f GB, Percent Used = %.1f %%<br>", freeGb, totalGb, pctUsed));
			out.println("With H2 Database Recordings Size of " + sumH2RecordingsUsageMb + " MB (" + sumH2RecordingsUsageGb + " GB).");
			out.println("</p>");

			// Get the same "saved file size info" as DataDirectoryCleaner
			String fileName = dataDirRes.getAbsolutePath() + File.separatorChar + Configuration.getCombinedConfiguration().getProperty(DataDirectoryCleaner.PROPKEY_savedFileInfo_filename, DataDirectoryCleaner.DEFAULT_savedFileInfo_filename);
			Configuration savedFileInfo = new Configuration(fileName);
			_logger.info("Loaded file '"+savedFileInfo.getFilename()+"' to store File Size Information, with "+savedFileInfo.size()+" entries.");
			
//			for (Path root : FileSystems.getDefault().getRootDirectories()) 
//			{
//				out.print(root + ": ");
//				try {
//					FileStore store = Files.getFileStore(root);
//					out.println("available = " + (store.getUsableSpace()/1024/1024) + " MB, total = " + (store.getTotalSpace()/1024/1024) + " MB <br>");
//				} catch (IOException e) {
//					out.println("error querying space: " + e.toString() + " <br>");
//				}
//			}
			
//			String firstServerName = "XXXXXXX";

			String tableHead 
					= "  <tr id='offline-srv-SRVNAME'>"
					+ "    <th>File" + (isDownloadRecordingEnabled ? " (click to dowload)" : "") + "</th>"
					+ "    <th>DayOfWeek</th>"
					+ "    <th>Saved Max GB</th>"
					+ "    <th>File Size GB</th>"
					+ "    <th>File Size MB</th>"
					+ "    <th>Shrink Size GB</th>"
					+ "    <th>DSR (Daily Summary Report)</th>"
					+ "    <th>Url (green row is active recording), default H2 Port: 9092</th>"
					+ "  </tr>";
			
			out.println("<table>");
			out.println("<thead>");
			out.println(tableHead.replace("SRVNAME", firstServerName));
			out.println("</thead>");
			out.println("<tbody>");
			
			String prevSrvName = null;

			for (String file : getFilesH2Dbs(H2DbFileType.OFFLINE_AND_ACTIVE))
			{
				File f = new File(file);

				String dbName = f.getName().split("\\.")[0];
				
				String sa[] = dbName.split("_");
				String dayOfWeek = getDayOfWeek(sa[ sa.length - 1 ], "yyyy-MM-dd");
				
				String sizeInGB = String.format("%.1f GB", f.length() / 1024.0 / 1024.0 / 1024.0);
				String sizeInMB = String.format("%.1f MB", f.length() / 1024.0 / 1024.0);
				
				long savedSizeInGbLong = savedFileInfo.getLongProperty(f.getName(), -1);
				String savedSizeInGB = String.format("%.1f GB", savedSizeInGbLong / 1024.0 / 1024.0 / 1024.0);
				String diffSizeInGB  = String.format("%.1f GB", (savedSizeInGbLong - f.length()) / 1024.0 / 1024.0 / 1024.0);
				if (savedSizeInGbLong < 0)
				{
					savedSizeInGB = "n/a"; // Not found in 'savedFileInfo'
					diffSizeInGB  = "n/a"; // Not found in 'savedFileInfo'
				}
				if (f.length() == savedSizeInGbLong)
					diffSizeInGB  = "none"; // not compressed at all... probably failed in compression or simply not savedFileInfo was ...

				
				String srvName = dbName;
				if (dbName.matches(".*_[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]"))
				{
					srvName = dbName.substring(0, dbName.length()-"_yyyy-MM-dd".length());
				}
				// Add empty row when it's a new server
				if (prevSrvName != null && ! prevSrvName.equals(srvName))
				{
					out.println("  <tr>");
					out.println("    <td nowrap colspan='7' style='background-color:rgb(209, 224, 224);'>&nbsp;</td>");
					out.println("  </tr>");
					out.println(tableHead.replace("SRVNAME", srvName)); // Also add new "headers" so we don't have to scroll that much
				}
				prevSrvName = srvName;
				
				
				String collectorHostname = hostname;
				String dbxTuneName       = "AseTune";
				DbxCentralSessions session = centralSessionMap.get(srvName);
				if (session != null)
				{
					collectorHostname = session.getCollectorHostname(); 
					dbxTuneName       = session.getProductString();
				}

				String url        = "jdbc:h2:tcp://" + collectorHostname + "/" + dbName + ";IFEXISTS=TRUE;DB_CLOSE_ON_EXIT=FALSE";
//				String dbxTuneUrl = dbxTuneGuiUrl + url;
				String dbxTuneUrl  = dbxTuneGuiUrl.replace(":PORT/", ":"+DbxTune.getGuiWebPort(dbxTuneName)+"/") + url;

//				String downloadH2File = "<a href='/download-recording?name=" + f.getName() + "' download='" + f.getName() + "'>" + dbName + "</a>";
				String downloadH2File = "<a href='/download-recording?name=" + f.getName() + "'>" + dbName + "</a>";

				// If we are now allowed to download files, then just show the database / date name
				if ( ! isDownloadRecordingEnabled )
					downloadH2File = dbName; // 
				
				String style = "";
				if (isTodayH2DbTimestamp(f.getName()))
				{
					downloadH2File = dbName; // Just the name, since "current recording" can NOT be download.
					style = "style='background-color:rgb(204, 255, 204);'"; // Very Light green
					
					// Point the URL to the ACTIVE recording (which is on non-default port)
					if (session != null)
					{
						if (session.getCollectorCurrentUrl() != null)
							url = session.getCollectorCurrentUrl();
						dbxTuneUrl = dbxTuneGuiUrl.replace(":PORT/", ":"+DbxTune.getGuiWebPort(dbxTuneName)+"/") + url;
					}
				}
				
//				String dsrUrl = "/api/dsr?op=xxx&dbname="+dbName+"&onHost="+collectorHostname;
//				String dsrJdbcUrl = dbName;
				String dsrJdbcUrl = url.replace(";DB_CLOSE_ON_EXIT=FALSE", "");
				String dsrUrl = "'javascript:void(0);' onclick='return dbxOpenDsrDialog(\"" + dsrJdbcUrl + "\");'";
				String dsrTxt = dbName;

				// Save this for later
				if (reqOperation.equalsIgnoreCase("dsr-current-srv") && reqSrvName.equalsIgnoreCase(srvName))
				{
					dsrCurrentJdbcUrl = dsrJdbcUrl;
				}

				out.println("  <tr>");
				out.println("    <td "+style+">" + downloadH2File + "</td>");
				out.println("    <td "+style+">" + dayOfWeek      + "</td>");
				out.println("    <td "+style+">" + savedSizeInGB  + "</td>");
				out.println("    <td "+style+">" + sizeInGB       + "</td>");
				out.println("    <td "+style+">" + sizeInMB       + "</td>");
				out.println("    <td "+style+">" + diffSizeInGB   + "</td>");
				out.println("    <td "+style+"><a href=" + dsrUrl + "><code>" + dsrTxt + "</code></a></td>");
				out.println("    <td "+style+"><div title='"+linkToolTip+"'><a href='" + dbxTuneUrl + "'><code>" + url + "</code></a></div></td>");
				out.println("  </tr>");
			}
			out.println("</tbody>");
			out.println("</table>");
			out.println("</div>"); // end: card-body
			out.println("</div>"); // end: card
		} // end: OFFLINE Databases 

		
		//----------------------------------------------------
		// ACTIVE Recordings (file content)
		//----------------------------------------------------
		if (true)
		{
			out.println("<div id='active_filecontent' class='card border-dark mb-3'>");
			out.println("<h5 class='card-header'>Active Recordings, full meta-data file content</h5>");
			out.println("<div class='card-body'>");
			out.println("<p>When a DbxTune collector starts it writes a <i>information</i> file with various content, this file is deleted when the collector stops. So this is also <i>proof</i> that the collector <i>lives</i></p>");
			out.println("<p>This section just lists the content of those files.</p>");
//			out.println("<br><hr>");
//			out.println("<h3>Active Recordings, full file content</h3>");
			for (String file : getInfoFilesDbxTune())
			{
				File f = new File(file);
				String srvName = f.getName().split("\\.")[0];

				out.println("<h3>" + srvName + "</h3>");

				String fileContent = FileUtils.readFile(f, null);
				
				// remove some backslashes '\' for readability
				if (fileContent != null)
				{
					fileContent = fileContent.replace("\\\\", "\\");
					fileContent = fileContent.replace("\\:", ":");
					fileContent = fileContent.replace("\\=", "=");
				}
				
				out.println("Content of file: "+file);
				out.println("<hr>");
				out.println("<pre>");
				out.println("<code class='language-properties line-numbers'>");
				out.println(fileContent);
				out.println("</code>");
				out.println("</pre>");
				out.println("<hr>");
			}
			out.println("</div>"); // end: card-body
			out.println("</div>"); // end: card
			out.println("<br><br>");
		} // end: ACTIVE Recordings (file content)

		
		out.println("</div>");

		// Write Daily Summary HTML & JavaScript
		out.println(getHtmlForDailySummaryReportDialog());
		
		// Write some JavaScript code
		out.println(HtmlStatic.getJavaScriptAtEnd(true));

		// Actions for specific operations
		if (reqOperation.equalsIgnoreCase("dsr-current-srv") && StringUtil.hasValue(reqSrvName))
		{
			out.println("");
			out.println("<script>");
//			out.println("document.addEventListener('load', function()");   // or possibly use: document.addEventListener('DOMContentLoaded', function() {});
			out.println("document.addEventListener('DOMContentLoaded', function()");   // or possibly use: document.addEventListener('DOMContentLoaded', function() {});
			out.println("{");
			out.println("    console.log('on-load[DOMContentLoaded] ::: reqSrvName=|" + reqSrvName + "|'); ");
			out.println("");
			out.println("    var element = document.getElementById('offline-srv-" + reqSrvName + "'); ");
			out.println("    if (element === null) ");
			out.println("        element = document.getElementById('offline'); ");
			out.println("    element.scrollIntoView(); ");
			out.println("");
			out.println("    dbxOpenDsrDialog('" + dsrCurrentJdbcUrl + "');");
			out.println("});");			
			out.println("</script>");
			out.println("");
		}

		out.println("</body>");
		out.println("</html>");
		out.flush();
		out.close();

	} // end: doGet

	
	private static String getHtmlForDailySummaryReportDialog()
	{
//		return "FIXME";

		StringBuilder sb = new StringBuilder(1024);
		
		sb.append("	<!-- Modal: DSR Daily Summary Report dialog -->																					\n");
		sb.append("	<div class='modal fade' id='dbx-dsr-dialog' tabindex='-1' role='dialog' aria-labelledby='dbx-dsr-dialog' aria-hidden='true'>	\n");
		sb.append("		<div class='modal-dialog modal-dialog-centered modal-lg' role='document'>													\n");
		sb.append("		<div class='modal-content'>																									\n");
		sb.append("			<div class='modal-header'>																								\n");
		sb.append("			<h5 class='modal-title' id='dbx-dsr-dialog-title'>Create a Daily Summary Report - (With Time Bounderies)</h5>			\n");
		sb.append("			<button type='button' class='close' data-dismiss='modal' aria-label='Close'>											\n");
		sb.append("				<span aria-hidden='true'>&times;</span>																				\n");
		sb.append("			</button>																												\n");
		sb.append("			</div>																													\n");
		sb.append("			<div class='modal-body'>																								\n");
//		sb.append("				<ul>																												\n");
//		sb.append("					<li>Action: Describe me 1</li>																					\n");
//		sb.append("					<li>Action: Describe me 2</li>																					\n");
//		sb.append("				</ul>																												\n");
		sb.append("				<form>																													\n");
		sb.append("					<div class='form-row'>																								\n");
		sb.append("						<label for='dbx-dsr-dbname'>Recording DB Name (full URL or H2 DB name))</label>									\n");
		sb.append("						<input  id='dbx-dsr-dbname' type='text' class='form-control is-valid' placeholder='dbname' required>			\n");
		sb.append("					</div>																												\n");
		sb.append("					<div class='form-row'>																								\n");
		sb.append("						<div class='col-md-4 mb-3'>																						\n");
//		sb.append("						<div class='col-auto'>																							\n");
		sb.append("							<label for='dbx-dsr-username-txt'>DB User Name</label>														\n");
		sb.append("							<input  id='dbx-dsr-username-txt' type='text' class='form-control is-valid' placeholder='Username, sa is default' />	\n");
		sb.append("						</div>																											\n");
		sb.append("						<div class='col-md-4 mb-3'>																						\n");
//		sb.append("						<div class='col-auto'>																							\n");
		sb.append("							<label for='dbx-dsr-password-txt'>DB Password</label>														\n");
		sb.append("							<input  id='dbx-dsr-password-txt' type='password' class='form-control is-valid' placeholder='Password, blank is default' />	\n");
		sb.append("						</div>																											\n");
		sb.append("					</div>																											\n");
		sb.append("					<div class='form-row'>																								\n");
		sb.append("						<div class='col-md-4 mb-3'>																						\n");
//		sb.append("						<div class='col-auto'>																							\n");
		sb.append("							<label for='dbx-dsr-begin-time'>Report Begin Time</label>													\n");
		sb.append("							<input  id='dbx-dsr-begin-time' type='text' class='form-control is-valid' placeholder='HH:mm (or blank)' pattern='^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$' title='Must be HH:MM, where HH=00-23 and MM=00-59'/>	\n");
		sb.append("							<div class='invalid-feedback'>Must be HH:MM, where HH=00-23 and MM=00-59</div>								\n");
		sb.append("						</div>																											\n");
		sb.append("						<div class='col-md-4 mb-3'>																						\n");
//		sb.append("						<div class='col-auto'>																							\n");
		sb.append("							<label for='dbx-dsr-end-time'>Report End Time</label>														\n");
		sb.append("							<input  id='dbx-dsr-end-time' type='text' class='form-control is-valid' placeholder='HH:mm (or blank)' pattern='^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$' title='Must be HH:MM, where HH=00-23 and MM=00-59' />	\n");
		sb.append("							<div class='invalid-feedback'>Must be HH:MM, where HH=00-23 and MM=00-59</div>								\n");
		sb.append("						</div>																											\n");
		sb.append("					</div>																											\n");
		sb.append("				</form>																												\n");
		sb.append("				<div id='dbx-dsr-progress-div' class='progress'>																	\n");
		sb.append("					<div id='dbx-dsr-progress-bar' class='progress-bar progress-bar-striped active' role='progressbar' aria-valuenow='0' aria-valuemin='0' aria-valuemax='100' style='width:0%; min-width:25px'>	\n");
		sb.append("					0%																												\n");
		sb.append("					</div>																											\n");
		sb.append("				</div>																												\n");
		sb.append("				<div id='dbx-dsr-progress-txt'>																						\n");
		sb.append("				</div>																												\n");
//		sb.append("				<br>																												\n");
//		sb.append("				<table id='dbx-dsr-filter-table'																					\n"); 
//		sb.append("					class='table-responsive' 																						\n");
//		sb.append("					data-show-columns='false' 																						\n");
//		sb.append("					data-paging='false' 																							\n");
//		sb.append("					data-filtering='true'																							\n"); 
//		sb.append("					data-filter-control='true' 																						\n");
//		sb.append("					data-click-to-select='false'																					\n");
//		sb.append("					data-sorting='true'																								\n");
//		sb.append("					data-checkbox-header='true'																						\n");
//		sb.append("					data-maintain-selected='true'																					\n");
//		sb.append("					data-ignore-click-to-select-on=scrollToDisableCheckRow()>														\n");
//		sb.append("					<thead>																											\n");
//		sb.append("						<tr>																										\n");
//		sb.append("							<th data-field='visible'   data-checkbox='true'></th>													\n");
//		sb.append("							<th data-field='cm'        data-filter-control='input'  data-sortable='true'>Counter Model</th>			\n");
//		sb.append("							<th data-field='desc'      data-filter-control='input'  data-sortable='true'>Description</th>			\n");
//		sb.append("						</tr>																										\n");
//		sb.append("					</thead>																										\n");
//		sb.append("					<tbody>																											\n");
//		sb.append("						<tr> 																										\n");
//		sb.append("							<td class='bs-checkbox '><input data-index='0' name='btSelectItem' type='checkbox'></td>				\n");
//		sb.append("							<td>dummy-1</td> 																						\n");
//		sb.append("							<td>dummy-2</td> 																						\n");
//		sb.append("						</tr>																										\n");
//		sb.append("					</tbody>          																								\n");      
//		sb.append("				</table>																											\n");
		sb.append("			</div>																													\n");
		sb.append("			<div class='modal-footer'>																								\n");
//		sb.append("			<button type='button' class='btn btn-primary' data-dismiss='modal' id='dbx-dsr-dialog-ok'>Create</button>				\n");
		sb.append("			<button type='button' class='btn btn-primary' id='dbx-dsr-dialog-ok'>Create</button>				\n");
		sb.append("			<button type='button' class='btn btn-default' data-dismiss='modal'>Close</button>										\n");
		sb.append("			</div>																													\n");
		sb.append("		</div>																														\n");
		sb.append("		</div>																														\n");
		sb.append("	</div>  																														\n");
		sb.append("	<!-- -->																														\n");
		sb.append("\n");
		sb.append("\n");
		sb.append("\n");



//				<button id="dbx-filter"              class="btn btn-outline-light mx-2 my-0 my-sm-0" type="button">Filter</button>

		sb.append("<script>																												\n");
//		sb.append("	// Install functions for button: dbx-filter																			\n");
//		sb.append("	$('#dbx-filter').click( function() 																					\n");
//		sb.append("	{																													\n");
//		sb.append("		dbxOpenDsrDialog();																								\n");
//		sb.append("	});																													\n");
		sb.append("																														\n");
		sb.append("	$('#dbx-dsr-dialog').on('show.bs.modal', function () {																\n");
		sb.append("		$(this).find('.modal-body').css({																				\n");
		sb.append("			'max-height':'100%'																							\n");
		sb.append("		});																												\n");
		sb.append("	});																													\n");
		
//		sb.append("	// Example starter JavaScript for disabling form submissions if there are invalid fields							\n");
//		sb.append("	(function() {																										\n");
//		sb.append("	  'use strict';																										\n");
//        sb.append("																														\n");
//		sb.append("	  window.addEventListener('load', function() {																		\n");
//		sb.append("	    var form = document.getElementById('needs-validation');															\n");
//		sb.append("	    form.addEventListener('submit', function(event) {																\n");
//		sb.append("	      if (form.checkValidity() === false) {																			\n");
//		sb.append("	        event.preventDefault();																						\n");
//		sb.append("	        event.stopPropagation();																					\n");
//		sb.append("	      }																												\n");
//		sb.append("	      form.classList.add('was-validated');																			\n");
//		sb.append("	    }, false);																										\n");
//		sb.append("	  }, false);																										\n");
//		sb.append("	})();																												\n");
		
		sb.append("																														\n");
		sb.append("	// What should happen when we click OK in the dialog																\n");
		sb.append("	$('#dbx-dsr-dialog-ok').click( function() 																			\n");
		sb.append("	{																													\n");
		sb.append("		// Show progress field																							\n");
		sb.append("		$('#dbx-dsr-progress-div').show();																				\n");
		sb.append("		$('#dbx-dsr-progress-bar').css('width', '0%');																	\n");
		sb.append("		$('#dbx-dsr-progress-bar').html('0%');																			\n");
		sb.append("																														\n");
		sb.append("		// build the URL params																							\n");
		sb.append("		var dsrDbname    = $('#dbx-dsr-dbname').val();																	\n");
		sb.append("		var dsrUsername  = $('#dbx-dsr-username-txt').val();															\n");
		sb.append("		var dsrPassword  = $('#dbx-dsr-password-txt').val();															\n");
		sb.append("		var dsrBeginTime = $('#dbx-dsr-begin-time').val();																\n");
		sb.append("		var dsrEndTime   = $('#dbx-dsr-end-time').val();																\n");
		sb.append("																														\n");
//		sb.append("		console.log('dsrDbname    = |' + dsrDbname + '|');																\n");
//		sb.append("		console.log('dsrUsername  = |' + dsrUsername + '|');															\n");
//		sb.append("		console.log('dsrPassword  = |' + dsrPassword + '|');															\n");
//		sb.append("		console.log('dsrBeginTime = |' + dsrBeginTime + '|');															\n");
//		sb.append("		console.log('dsrEndTime   = |' + dsrEndTime + '|');																\n");
//		sb.append("																														\n");
		sb.append("		if (dsrDbname)    { dsrDbname    = '&dbname='    + dsrDbname;    }												\n");
		sb.append("		if (dsrUsername)  { dsrUsername  = '&username='  + dsrUsername;  }												\n");
		sb.append("		if (dsrPassword)  { dsrPassword  = '&password='  + dsrPassword;  }												\n");
		sb.append("		if (dsrBeginTime) { dsrBeginTime = '&beginTime=' + dsrBeginTime; }												\n");
		sb.append("		if (dsrEndTime)   { dsrEndTime   = '&endTime='   + dsrEndTime;   }												\n");
		sb.append("																														\n");
		sb.append("		// create the event source																						\n");
		sb.append("		var fullUrl = encodeURI('/api/dsr?op=get' + dsrDbname + dsrUsername + dsrPassword + dsrBeginTime + dsrEndTime);	\n");
		sb.append("		console.log('EventSource - FullURL: ' + fullUrl);																\n");
		sb.append("		var sse = new EventSource(fullUrl);																				\n");
		sb.append("																														\n");
		sb.append("		// set initial progress text																					\n");
		sb.append("		$('#dbx-dsr-progress-txt').text('Requested a DSR request. URL: ' + fullUrl);									\n");
		sb.append("																														\n");
		sb.append("		// ---------------------------------------------------------													\n");
		sb.append("		// PROGRESS																										\n");
		sb.append("		// ---------------------------------------------------------													\n");
		sb.append("		sse.addEventListener('progress', function(event) 																\n");
		sb.append("		{																												\n");
		sb.append("			console.log('ON-PROGRESS: ' + event, event);																\n");
		sb.append("																														\n");
		sb.append("			// data expected to be in JSON-format, so parse 															\n");
		sb.append("			var data = JSON.parse(event.data);																			\n");
		sb.append("																														\n");
		sb.append("			$('#dbx-dsr-progress-txt').css('color', 'black');															\n");
		sb.append("			$('#dbx-dsr-progress-txt').text(data.progressText);															\n");
		sb.append("																														\n");
		sb.append("			var pct = data.percentDone;																					\n");
		sb.append("			if (data.state === 'AFTER')																					\n");
		sb.append("			{																											\n");
		sb.append("				$('#dbx-dsr-progress-bar').css('width', pct+'%');														\n");
		sb.append("				$('#dbx-dsr-progress-bar').html(pct+'%');																\n");
		sb.append("			}																											\n");
		sb.append("																														\n");
		sb.append("			// update status								 															\n");
		sb.append("			if (data.state === 'AFTER' && pct === 100)																	\n");
		sb.append("			{																											\n");
		sb.append("				$('#dbx-dsr-progress-txt').text('Report Content will now be transferred... This may take time, then a new tab will be opened!');	\n");
		sb.append("			}																											\n");
		sb.append("		});																												\n");
		sb.append("																														\n");
		sb.append("		// ---------------------------------------------------------													\n");
		sb.append("		// COMPLETE																										\n");
		sb.append("		// ---------------------------------------------------------													\n");
		sb.append("		sse.addEventListener('complete', function(event) 																\n");
		sb.append("		{																												\n");
		sb.append("			console.log('ON-COMPLETE: ' + event, event);																\n");
		sb.append("																														\n");
		sb.append("			// data expected to be in JSON-format, so parse 															\n");
		sb.append("			var data = JSON.parse(event.data);																			\n");
		sb.append("																														\n");
		sb.append("			// close the connection to server																			\n");
		sb.append("			sse.close();																								\n");
		sb.append("																														\n");
		sb.append("			$('#dbx-dsr-progress-txt').css('color', 'black');															\n");
		sb.append("			$('#dbx-dsr-progress-txt').text('Report is complete...');													\n");
		sb.append("																														\n");
		sb.append("			var newDsrTab = window.open('','_blank');																	\n");
		sb.append("			newDsrTab.document.write(data.complete);																	\n");
		sb.append("																														\n");
		sb.append("			$('#dbx-dsr-progress-txt').text('A New tab with the Daily Summary Report has been opened.');				\n");
		sb.append("			$('#dbx-dsr-progress-div').hide();																			\n");
		sb.append("																														\n");
//		sb.append("			// Hide the dialog																							\n");
//		sb.append("			$('#dbx-dsr-dialog').modal('hide');																			\n");
		sb.append("		});																												\n");
		sb.append("																														\n");
//		sb.append("		// handle incoming messages																						\n");
//		sb.append("		sse.onmessage = function(event) 																				\n");
//		sb.append("		{																												\n");
//		sb.append("			console.log('ON-MESSAGE: ' + event, event);																	\n");
//		sb.append("																														\n");
//		sb.append("			$('#dbx-dsr-progress-txt').css('color', 'black');															\n");
//		sb.append("																														\n");
//		sb.append("			if (event.type == 'message') 																				\n");
//		sb.append("			{																											\n");
//		sb.append("				// data expected to be in JSON-format, so parse 														\n");
//		sb.append("				var data = JSON.parse(event.data);																		\n");
//		sb.append("																														\n");
//		sb.append("				if (data.hasOwnProperty('complete'))																	\n");
//		sb.append("				{																										\n");
//		sb.append("					// close the connection so browser does not keep connecting											\n");
//		sb.append("					sse.close();																						\n");
//		sb.append("																														\n");
//		sb.append("					$('#dbx-dsr-progress-txt').text('Report is complete...');											\n");
//		sb.append("																														\n");
//		sb.append("					var newDsrTab = window.open('','_blank');															\n");
//		sb.append("					newDsrTab.document.write(data.complete);															\n");
//		sb.append("																														\n");
//		sb.append("					$('#dbx-dsr-progress-txt').text('A New tab with the Daily Summary Report has been opened.');		\n");
//		sb.append("					$('#dbx-dsr-progress-div').hide();																	\n");
//		sb.append("																														\n");
////		sb.append("					// Hide the dialog																					\n");
////		sb.append("					$('#dbx-dsr-dialog').modal('hide');																	\n");
//		sb.append("				}																										\n");
//		sb.append("				// otherwise, it's a progress update so just update progress bar										\n");
//		sb.append("				else 																									\n");
//		sb.append("				{																										\n");
//		sb.append("					$('#dbx-dsr-progress-txt').text(data.progressText);													\n");
//		sb.append("																														\n");
//		sb.append("					var pct = data.percentDone;																			\n");
//		sb.append("					if (data.state === 'AFTER')																			\n");
//		sb.append("					{																									\n");
//		sb.append("						$('#dbx-dsr-progress-bar').css('width', pct+'%');												\n");
//		sb.append("						$('#dbx-dsr-progress-bar').html(pct+'%');														\n");
//		sb.append("					}																									\n");
//		sb.append("																														\n");
//		sb.append("					// update status								 													\n");
//		sb.append("					if (data.state === 'AFTER' && pct === 100)															\n");
//		sb.append("					{																									\n");
//		sb.append("						$('#dbx-dsr-progress-txt').text('Report Content will now be transferred... This may take time, then a new tab will be opened!');	\n");
//		sb.append("					}																									\n");
//		sb.append("				}																										\n");
//		sb.append("			}																											\n");
//		sb.append("			else						 																				\n");
//		sb.append("			{																											\n");
//		sb.append("				$('#dbx-dsr-progress-txt').text('Unhandled event type: ' + event.type);									\n");
//		sb.append("				$('#dbx-dsr-progress-txt').css('color', 'red');															\n");
//		sb.append("			}																											\n");
//		sb.append("		};																												\n");
		sb.append("		// ---------------------------------------------------------													\n");
		sb.append("		// ERROR																										\n");
		sb.append("		// ---------------------------------------------------------													\n");
		sb.append("		sse.onerror = function(event) 																					\n");
		sb.append("		{																												\n");
		sb.append("			sse.close();																								\n");
		sb.append("			console.log('SSE-ON-COMPLETE: ', event);																	\n");
		sb.append("			$('#dbx-dsr-progress-txt').text('ERROR: ' + event.data);													\n");
		sb.append("			$('#dbx-dsr-progress-txt').css('color', 'red');																\n");
		sb.append("		};																												\n");

//		sb.append("		var selectedRecords = $('#dbx-dsr-filter-table').bootstrapTable('getSelections');								\n");
//		sb.append("																														\n");
//		sb.append("		// hide ALL graphs																								\n");
//		sb.append("		for(let i=0; i<_graphMap.length; i++)																			\n");
//		sb.append("		{																												\n");
//		sb.append("			const dbxGraph = _graphMap[i];																				\n");
//		sb.append("			var x = document.getElementById(dbxGraph.getFullName());													\n");
//		sb.append("			x.style.display = 'none';																					\n");
//		sb.append("			// console.log('HIDE: x='+x, x);																			\n");
//		sb.append("		}																												\n");
//		sb.append("		// show marked ones ALL graphs																					\n");
//		sb.append("		for (let i = 0; i < selectedRecords.length; i++)																\n"); 
//		sb.append("		{																												\n");
//		sb.append("			const record = selectedRecords[i];																			\n");
//		sb.append("			var x = document.getElementById(record.fullName);															\n");
//		sb.append("			x.style.display = 'block';																					\n");
//		sb.append("			// console.log('SHOW: x='+x, x);																			\n");
//		sb.append("		}																												\n");
		sb.append("	});																													\n");

		sb.append("\n");
		sb.append("\n");
		sb.append("\n");

		sb.append("	function dbxOpenDsrDialog(dbname)																					\n");
		sb.append("	{																													\n");
		sb.append("		console.log('dbxOpenDsrDialog(dbname='+dbname+')');																\n");
		sb.append("																														\n");
//		sb.append("		// loop all available graphs and add it to the table in the dialog												\n");
//		sb.append("		if (_filterDialogContentArr.length === 0)																		\n");
//		sb.append("		{																												\n");
//		sb.append("			for(let i=0; i<_graphMap.length; i++)																		\n");
//		sb.append("			{																											\n");
//		sb.append("				const dbxGraph = _graphMap[i];																			\n");
//		sb.append("																														\n");
//		sb.append("				var row = {																								\n");
//		sb.append("					'visible'   : true,																					\n");
//		sb.append("					'desc'      : dbxGraph.getGraphLabel(),																\n");
//		sb.append("					'type'      : dbxGraph.getGraphCategory(),															\n");
//		sb.append("					'cm'        : dbxGraph.getCmName(),																	\n");
//		sb.append("					'graphName' : dbxGraph.getGraphName(),																\n");
//		sb.append("					'fullName'  : dbxGraph.getFullName(),																\n");
//		sb.append("				};																										\n");
//		sb.append("				_filterDialogContentArr.push(row);																		\n");
//		sb.append("			}																											\n");
//		sb.append("																														\n");
//		sb.append("			$('#dbx-dsr-filter-table').bootstrapTable({data: _filterDialogContentArr});									\n");
//		sb.append("		}																												\n");
		sb.append("																														\n");
		sb.append("		// set some fields																								\n");
		sb.append("		$('#dbx-dsr-dbname').val(dbname);																				\n");
		sb.append("																														\n");
		sb.append("		// Hide progress field																							\n");
		sb.append("		$('#dbx-dsr-progress-div').hide();																				\n");
		sb.append("		$('#dbx-dsr-progress-txt').text('');																			\n");
		sb.append("																														\n");
		sb.append("		// Show the dialog																								\n");
		sb.append("		$('#dbx-dsr-dialog').modal('show');																				\n");
		sb.append("																														\n");
		sb.append("		return false;																									\n");
		sb.append("	}																													\n");

		sb.append("</script>																											\n");
		
		sb.append("\n");
		sb.append("\n");
		sb.append("\n");

		return sb.toString();
	}
}
