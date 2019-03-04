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
package com.asetune.central.controllers;

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

import org.apache.log4j.Logger;

import com.asetune.DbxTune;
import com.asetune.Version;
import com.asetune.alarm.writers.AlarmWriterToFile;
import com.asetune.central.DbxTuneCentral;
import com.asetune.central.cleanup.CentralH2Defrag;
import com.asetune.central.cleanup.CentralH2Defrag.H2StorageInfo;
import com.asetune.central.cleanup.DataDirectoryCleaner;
import com.asetune.central.pcs.CentralPersistReader;
import com.asetune.central.pcs.objects.DbxCentralSessions;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;
import com.asetune.utils.FileUtils;
import com.asetune.utils.StringUtil;
import com.asetune.utils.TimeUtils;

public class OverviewServlet extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	private static final Logger _logger = Logger.getLogger(MethodHandles.lookup().lookupClass());

	private final String HOME_DIR = DbxTuneCentral.getAppHomeDir();
	private final String INFO_DIR = DbxTuneCentral.getAppInfoDir();
	private final String LOG_DIR  = DbxTuneCentral.getAppLogDir();
	private final String CONF_DIR = DbxTuneCentral.getAppConfDir();
	private final String DATA_DIR = DbxTuneCentral.getAppDataDir();

	private List<String> getFilesDbxTune()
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

	private String getSrvDescription(String srvName)
	{
		String file = DbxTuneCentral.getAppConfDir() + "/SERVER_LIST";
		File f = new File(file);

		String description = "";
		try
		{
			FileInputStream in = new FileInputStream(f);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));

			String tmp;
			while ((tmp = br.readLine()) != null)
			{
				if (StringUtil.hasValue(tmp))
				{
					if ( ! tmp.startsWith("#") )
					{
						String[] sa = tmp.split(";");
						if (sa.length >= 3)
						{
							String name    = sa[0].trim();
							//String enabled = sa[1].trim();
							String desc    = sa[2].trim();

							if (srvName.equals(name))
								return desc;
						}
					}
				}
			}
			br.close();
			in.close();
		}
		catch (Exception ex)
		{
			return ex.toString();
		}
		return description;
	}

	private List<String> getFilesActiveAlarms()
	{
		String directory = LOG_DIR;

		List<String> fileNames = new ArrayList<>();
		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(directory)))
		{
			for (Path path : directoryStream)
			{
				if (path.getFileName().toString().matches("ALARM\\.ACTIVE\\..*\\.txt"))
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
	
	private List<String> getFilesInConfDir()
	{
		String directory = CONF_DIR;

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

	private boolean isTodayH2DbTimestamp(String name)
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

	private List<String> getFilesH2Dbs(H2DbFileType type)
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
		if (Helper.hasUnKnownParameters(req, resp, "refresh"))
			return;

		
		String refreshStr = Helper.getParameter(req, "refresh", "0");

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

		out.println("<p>Sections");
		out.println("<ul>");
		out.println("  <li><a href='#active'            >Active Recordings                             </a> </li>");
		out.println("  <li><a href='#alarms'            >Active Alarms                                 </a> </li>");
		out.println("  <li><a href='#logfiles'          >All file(s) in the LOG Directory              </a> </li>");
		out.println("  <li><a href='#conffiles'         >All file(s) in the CONF Directory             </a> </li>");
		out.println("  <li><a href='#central'           >DbxCentral databases                          </a> </li>");
		out.println("  <li><a href='#offline'           >Available offline databases                   </a> </li>");
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

		//----------------------------------------------------
		// ACTIVE Recordings
		//----------------------------------------------------
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
//		out.println("<br><hr>");
//		out.println("<h3>Active Recordings</h3>");
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
		// get Config files from local *running* DbxTune servers
		HashMap<String, Configuration> srvInfoMap = new HashMap<>();
		for (String file : getFilesDbxTune())
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
		// Loop and print values from the Persist Reader
		for (DbxCentralSessions session : centralSessionList)
		{
			if (session.hasStatus(DbxCentralSessions.ST_DISABLED))
			{
				_logger.info("List Active Recording: Skipping server '"+session.getServerName()+"', due to status: DISABLED.");
				continue;
			}
			
//			// If last sample is older than 24 Hours lets not present it as an active recording
//			int threshold = 3600 * 24;
//			if (session.getLastSampleAgeInSec() > threshold || session.getLastSampleAgeInSec() < 0)
//			{
//				_logger.info("List Active Recording: Skipping server '"+session.getServerName()+"', due to 'old age'. " +
//						"LastSampleAgeInSec="+session.getLastSampleAgeInSec() + " ("+TimeUtils.msToTimeStr("%HH:%MM:%SS", session.getLastSampleAgeInSec()*1000)+")" +
//						", threshold="+threshold+" seconds. ("+TimeUtils.msToTimeStr("%HH:%MM:%SS", threshold*1000)+")");
//				continue;
//			}
			
			boolean isLocalCollector = session.getCollectorIsLocal();
			
			String appName     = session.getProductString();
			String srvName     = session.getServerName();
			String srvDesc     = session.getServerDescription();
			int    refreshRate = session.getCollectorSampleInterval();
			long   refreshAge  = session.getLastSampleAgeInSec();
			String collectHost = session.getCollectorHostname();
			String collectUrl  = session.getCollectorCurrentUrl();
			String url         = "jdbc:h2:tcp://"+collectHost+":19092/"+srvName+"_"+(new SimpleDateFormat("yyyy-MM-dd").format(new Date()));

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
		

//		for (String file : getFilesDbxTune())
//		{
//			File f = new File(file);
//			
//			Configuration conf = new Configuration(file);
//			String appName     = conf.getProperty("dbxtune.app.name", "-unknown-");
//			String srvName     = f.getName().split("\\.")[0];
//			String srvDesc     = getSrvDescription(srvName);
//			String refreshRate = conf.getProperty("dbxtune.refresh.rate", "-unknown-");
//			String url         = conf.getProperty("pcs.h2.jdbc.url");
//			String dbxTuneUrl  = dbxTuneGuiUrl + url;
//
//			srvConfigMap.put(srvName, conf);
//			
//			out.println("  <tr>");
//			out.println("    <td>" + appName + "</td>");
//			out.println("    <td>" + srvName + "</td>");
//			out.println("    <td>" + srvDesc + "</td>");
//			out.println("    <td>" + refreshRate + "</td>");
//			out.println("    <td><div title='" + linkToolTip + "'><a href='" + dbxTuneUrl + "'><code>" + url + "</code></a></div></td>");
//			out.println("    <td><a href='/log?name="+srvName+".log'><code>"+srvName+".log</code></a></td>");
//			out.println("    <td><a href='/log?name="+srvName+".log&discard=Persisting Counters using'><code>"+srvName+".log</code></a></td>");
//			out.println("    <td><input type='text' placeholder='filter-out some text (regexp can be used), hit <enter> to search' class='search' size='80' style='border:none' onkeydown='openLogFileWithDiscard(this, \""+srvName+".log\")'/></td>");
//			out.println("  </tr>");
//			
//		}
		out.println("</tbody>");
		out.println("</table>");
		out.println("</div>"); // end: card-body
		out.println("</div>"); // end: card

		//----------------------------------------------------
		// ALARMS
		//----------------------------------------------------
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
//		out.println("<br><hr>");
//		out.println("<h3>Active Alarms</h3>");
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

//				if (StringUtil.hasValue(alarmFileActive) && f_alarmFileActive.exists())
				if (StringUtil.hasValue(alarmFileActive))
				{
					lastUpdateTs   = "" + (new Timestamp(f_alarmFileActive.lastModified()));
					lastUpdateAge  = TimeUtils.msToTimeStr("%?DD[d ]%?HH[:]%MM:%SS", System.currentTimeMillis()-f_alarmFileActive.lastModified());
//					logFileTd      = "<a href='/log?name="+f_alarmFileLog.getName()+"'>file</a> <a href='/alarmLog?name="+f_alarmFileLog.getName()+"&type=CANCEL'>table</a>: "+f_alarmFileLog.getName();
					logFileTd      = "<a href='/log?name="+f_alarmFileLog.getName()+"'>file</a> <a href='/alarmLog?name="+f_alarmFileLog.getName()+"&type=CANCEL'>table</a> <a href='/alarmLog?name="+srvName+"&method=pcs'>pcs</a>: "+f_alarmFileLog.getName();
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
					logFileTd      = "<a href='/alarmLog?name="+srvName+"&method=pcs'>pcs</a>: -no-active-alarm-file-";
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
				logFileTd      = "<a href='/alarmLog?name="+srvName+"&method=pcs'>pcs</a>: -remote-file-";
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

//			out.println("  <tr> <td>1</td> <td>2</td> <td>3</td> <td>4</td> <td>5</td> <td>6</td> <td>7</td> </tr>");
		}
		
//		for (String file : getFilesActiveAlarms())
//		{
//			File f = new File(file);
//			String srvName = f.getName().split("\\.")[2];
//
//			if ("unknown".equals(srvName))
//				continue;
//
//			String lastUpdateTs   = "" + (new Timestamp(f.lastModified()));
//			String lastUpdateAge  = TimeUtils.msToTimeStr("%?DD[d ]%?HH[:]%MM:%SS", System.currentTimeMillis()-f.lastModified());
//			File   logFile        = new File(LOG_DIR + "/ALARM.LOG." + srvName + ".log");
////			String logFileTd      = "<a href='/alarmLog?name=ALARM.LOG." + srvName + ".log&type=CANCEL'>ALARM.LOG." + srvName + ".log</a>";
//			String logFileTd      = "<a href='/log?name=ALARM.LOG." + srvName + ".log'>file</a> <a href='/alarmLog?name=ALARM.LOG." + srvName + ".log&type=CANCEL'>table</a>: ALARM.LOG." + srvName + ".log";
//			String fileContent    = getActiveAlarmContent(f);
//			String lastLogLine    = getLastLine(logFile);
////			String lastLogLineTs  = "FIXME"; //datetime.datetime.strptime( lastLogLine.split(' - ')[0], '%Y-%m-%d %H:%M:%S.%f' )
//			String lastLogLineAge = getLastLineAge(lastLogLine); //str( datetime.datetime.now() - lastLogLineTs ).split('.', 2)[0][:-3]
//
//			long refreshRate = 60;
//			int  refreshRateAlarmMultiplyer = 10;
//			String luTdAttr = "";
//			Configuration conf = srvInfoMap.get(srvName);
//			if (conf != null)
//			{
//				refreshRate = StringUtil.parseInt(conf.getProperty("dbxtune.refresh.rate", "60"), 60);
//			}
//			else
//			{
//				DbxCentralSessions session = centralSessionMap.get(srvName);
//				if (session != null)
//				{
//					refreshRate = session.getLastSampleAgeInSec();
//				}
//			}
//			long alarmFileUpdateAgeSec = (System.currentTimeMillis() - f.lastModified()) / 1000;
//			if ( alarmFileUpdateAgeSec > (refreshRate * refreshRateAlarmMultiplyer) )
//				luTdAttr = "style='background-color:rgb(255,0,0);'";
//			
//			out.println("  <tr> ");
//			out.println("    <td>"              + srvName        + "</td>");
//			out.println("    <td>"              + logFileTd      + "</td>");
//			out.println("    <td>"              + lastUpdateTs   + "</td>");
//			out.println("    <td "+luTdAttr+">" + lastUpdateAge  + "</td>");
//			out.println("    <td><pre>"         + fileContent    + "</pre></td>");
//			out.println("    <td>"              + lastLogLineAge + "</td>");
//			out.println("    <td><pre>"         + lastLogLine    + "</pre></td>");
//			out.println("  </tr>");
//		}
		out.println("</tbody>");
		out.println("</table>");
		out.println("</div>"); // end: card-body
		out.println("</div>"); // end: card

		//----------------------------------------------------
		// ALL files in LOG directory
		//----------------------------------------------------
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

//		out.println("<table>");
//		out.println("<thead>");
//		out.println("  <tr>");
//		out.println("    <th>File</th>");
//		out.println("    <th>Size GB</th>");
//		out.println("    <th>Size MB</th>");
//		out.println("    <th>Size KB</th>");
//		out.println("    <th>Tail-f (last 500)</th>");
//		out.println("    <th>Tail-f (last 500 + discard)</th>");
//		out.println("    <th>Show (discard some)</th>");
//		out.println("    <th>Discard Text</th>");
//		out.println("  </tr>");
//		out.println("</thead>");
//		out.println("<tbody>");
//		for (String file : getFilesInLogDir())
//		{
//			File f = new File(file);
//			if (f.isDirectory())
//				continue;
//
//			String sizeInGB = String.format("%.1f GB", f.length() / 1024.0 / 1024.0 / 1024.0);
//			String sizeInMB = String.format("%.1f MB", f.length() / 1024.0 / 1024.0);
//			String sizeInKB = String.format("%.1f KB", f.length() / 1024.0);
//			
//			String urlDiscardStr = "&discard=Persisting Counters using|Sent subscription data for server";
//			out.println("  <tr>");
//			out.println("    <td><a href='/log?name="+f.getName()+"'>"+f.getName()+"</a></td>");
//			out.println("    <td>" + sizeInGB     + "</td>");
//			out.println("    <td>" + sizeInMB     + "</td>");
//			out.println("    <td>" + sizeInKB     + "</td>");
//			out.println("    <td><a href='/log?name="+f.getName()+"&tail=500'><code>"+f.getName()+"</code></a></td>");
//			out.println("    <td><a href='/log?name="+f.getName()+urlDiscardStr+"&tail=500'><code>"+f.getName()+"</code></a></td>");
//			out.println("    <td><a href='/log?name="+f.getName()+urlDiscardStr+"'><code>"+f.getName()+"</code></a></td>");
//			out.println("    <td><input type='text' placeholder='filter-out some text (regexp can be used), hit <enter> to search' class='search' size='80' style='border:none' onkeydown='openLogFileWithDiscard(this, \""+f.getName()+"\")'/></td>");
//			out.println("  </tr>");
//
//		}
//		out.println("</tbody>");
//		out.println("</table>");
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
			out.println("      | <a href='/log?name="+f.getName()+"&tail=500'>tail</a>");
			out.println("      | <a href='/log?name="+f.getName()+urlDiscardStr+"&tail=500'>tail+discard</a>");
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


		//----------------------------------------------------
		// ALL files in CONF directory
		//----------------------------------------------------
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
		for (String file : getFilesInConfDir())
		{
			File f = new File(file);
			if (f.isDirectory())
				continue;

			out.println("  <tr>");
			out.println("    <td><a href='/conf?name="+f.getName()+"'>"+f.getName()+"</a></td>");
//			out.println("    <td><a href='/log?name="+f.getName()+"'><code>"+f.getName()+"</code></a></td>");
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

				String jdbcUrl  = "jdbc:h2:tcp://" + collectorHostname + "/" + dbName;
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
			fn = "DBX_CENTRAL.console";                   out.println("  <li><a href='/log?name=" + fn + "'>plain</a> | <a href='/log?name=" + fn+urlDiscardStr + "'>discard</a> | <a href='/log?name=" + fn + "&tail=500'>tail</a> | <a href='/log?name=" + fn+urlDiscardStr + "&tail=500'>tail+discard</a> &#8680; <a href='/log?name="+fn+"'>"+fn+"</a> </li>");
			fn = "DBX_CENTRAL.log";                       out.println("  <li><a href='/log?name=" + fn + "'>plain</a> | <a href='/log?name=" + fn+urlDiscardStr + "'>discard</a> | <a href='/log?name=" + fn + "&tail=500'>tail</a> | <a href='/log?name=" + fn+urlDiscardStr + "&tail=500'>tail+discard</a> &#8680; <a href='/log?name="+fn+"'>"+fn+"</a> </li>");
			fn = "DBX_CENTRAL_CentralH2Defrag.log";       out.println("  <li><a href='/log?name=" + fn + "'>plain</a> | <a href='/log?name=" + fn+urlDiscardStr + "'>discard</a> | <a href='/log?name=" + fn + "&tail=500'>tail</a> | <a href='/log?name=" + fn+urlDiscardStr + "&tail=500'>tail+discard</a> &#8680; <a href='/log?name="+fn+"'>"+fn+"</a> </li>");
			fn = "DBX_CENTRAL_DataDirectoryCleaner.log";  out.println("  <li><a href='/log?name=" + fn + "'>plain</a> | <a href='/log?name=" + fn+urlDiscardStr + "'>discard</a> | <a href='/log?name=" + fn + "&tail=500'>tail</a> | <a href='/log?name=" + fn+urlDiscardStr + "&tail=500'>tail+discard</a> &#8680; <a href='/log?name="+fn+"'>"+fn+"</a> </li>");
			fn = "DBX_CENTRAL_CentralPcsJdbcCleaner.log"; out.println("  <li><a href='/log?name=" + fn + "'>plain</a> | <a href='/log?name=" + fn+urlDiscardStr + "'>discard</a> | <a href='/log?name=" + fn + "&tail=500'>tail</a> | <a href='/log?name=" + fn+urlDiscardStr + "&tail=500'>tail+discard</a> &#8680; <a href='/log?name="+fn+"'>"+fn+"</a> </li>");
//			out.println("  <li><a href='/log?name=DBX_CENTRAL.console'>                   DBX_CENTRAL.console                   </a></li>");
//			out.println("  <li><a href='/log?name=DBX_CENTRAL.log'>                       DBX_CENTRAL.log                       </a></li>");
//			out.println("  <li><a href='/log?name=DBX_CENTRAL_CentralH2Defrag.log'>       DBX_CENTRAL_CentralH2Defrag.log       </a></li>");
//			out.println("  <li><a href='/log?name=DBX_CENTRAL_DataDirectoryCleaner.log'>  DBX_CENTRAL_DataDirectoryCleaner.log  </a></li>");
//			out.println("  <li><a href='/log?name=DBX_CENTRAL_CentralPcsJdbcCleaner.log'> DBX_CENTRAL_CentralPcsJdbcCleaner.log </a></li>");
			out.println("</ul>");
			out.println("</p>");

//			out.println("<p><br></p>");

			
			// Print some content of the Central Database
			if (CentralPersistReader.hasInstance())
			{
				CentralPersistReader reader = CentralPersistReader.getInstance();
				DbxConnection conn = null;
				String sql = null;
				try
				{
					conn = reader.getConnection();

					String lq = conn.getLeftQuote();  // Note no replacement is needed, since we get it from the connection
					String rq = conn.getRightQuote(); // Note no replacement is needed, since we get it from the connection

					sql = ""
					    + "select \n"
					    + "	   " + lq + "ServerName"    + rq + ", \n"
					    + "	   " + lq + "OnHostname"    + rq + ", \n"
					    + "	   " + lq + "ProductString" + rq + ", \n"
					    + "	   min(" + lq + "SessionStartTime" + rq + ") as " + lq + "FirstSample" + rq + ", \n"
					    + "	   max(" + lq + "LastSampleTime"   + rq + ") as " + lq + "LastSample"  + rq + ", \n"
//					    + "	   sum(" + lq + "NumOfSamples"     + rq + ") as " + lq + "NumOfSamples" + rq + ", \n" // Note this might be off/faulty after a: Data Retention Cleanup
					    + "	   datediff(day, max(" + lq + "LastSampleTime"   + rq + "), CURRENT_TIMESTAMP)   as " + lq + "LastSampleAgeInDays" + rq + ", \n"
					    + "	   datediff(day, min(" + lq + "SessionStartTime" + rq + "), max(" + lq + "LastSampleTime" + rq + ")) as " + lq + "NumOfDaysSampled" + rq + " \n"
					    + "from " + lq + "DbxCentralSessions" + rq + " \n"
					    + "group by " + lq + "ServerName" + rq + ", " + lq + "OnHostname" + rq + ", " + lq + "ProductString" + rq + " \n"
					    + "order by 1 \n"
					    + "";
					
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
        out.println("<div id='offline' class='card border-dark mb-3'>");
        out.println("<h5 class='card-header'>Available offline databases</h5>");
        out.println("<div class='card-body'>");
        out.println("<p>Historical database recordings.</p>");
        out.println("Column description");
        out.println("<ul>");
        out.println("<li><b>File             </b> - Name of the database file</li>");
        out.println("<li><b>DayOfWeek        </b> - What day of the week is this recording for (just extract the YYYY-MM-DD and try to convert it into a day of week)</li>");
        out.println("<li><b>Saved Max GB     </b> - Maximum size of the File before it was <i>compressed</i> using <code>shutdown defrag</code>, which is done with with <i>PCS H2 <b>rollover</b></i>. The value is updated by DataDirectoryCleaner.check(), when it's executed by the scheduler (default; at 23:54). This value is also the one used when calulating how much space we need for H2 databases in the next 24 hours. If the value is negative, no <i>max</i> value has yet been found/saved.</li>");
        out.println("<li><b>File Size GB     </b> - Current File size in GB</li>");
        out.println("<li><b>File Size MB     </b> - Current File size in MB</li>");
        out.println("<li><b>Shrink Size GB   </b> - Difference in SavedGB-CurrentGB, which is how much space is saved by doing 'shutdown defrag' when closing the db on 'rollover'.</li>");
        out.println("<li><b>URL              </b> - Click here to view the <b>detailed</b> recording. Note: You must have the Native DbxTune application started on your PC/Client machine.</li>");
        out.println("</ul>");
        out.println("<p>Note: Offline databases with <b>todays</b> timestamp will be marked in <span style='background-color:rgb(204, 255, 204);'>light green</span>, which probably is the active recording.</p>");

		File dataDir = new File(DbxTuneCentral.getAppDataDir());
		File dataDirRes = null;
		try { dataDirRes = dataDir.toPath().toRealPath().toFile(); } catch(IOException ex) { _logger.warn("Problems resolving File->Path->File");}

		double freeGb   = dataDir.getUsableSpace() / 1024.0 / 1024.0 / 1024.0;
//		double freeGb   = dataDir.getFreeSpace()   / 1024.0 / 1024.0 / 1024.0;
//		double usableGb = dataDir.getUsableSpace() / 1024.0 / 1024.0 / 1024.0;
		double totalGb  = dataDir.getTotalSpace()  / 1024.0 / 1024.0 / 1024.0;
		double pctUsed  = 100.0 - (freeGb / totalGb * 100.0);
		
		long       sumH2RecordingsUsageMb = DataDirectoryCleaner.getH2RecodingFileSizeMb();
		BigDecimal sumH2RecordingsUsageGb = new BigDecimal( sumH2RecordingsUsageMb /1024.0 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);

		out.println("<p>");
		out.println("File system usage at '"+dataDir+"', resolved to '"+dataDirRes+"'.<br>");
//		out.println(String.format("Free = %.1f GB, Usable = %.1f GB, Total = %.1f GB <br>", freeGb, usableGb, totalGb));
		out.println(String.format("Free = %.1f GB, Total = %.1f GB, Percent Used = %.1f %%<br>", freeGb, totalGb, pctUsed));
		out.println("With H2 Database Recordings Size of " + sumH2RecordingsUsageMb + " MB (" + sumH2RecordingsUsageGb + " GB).");
		out.println("</p>");

		// Get the same "saved file size info" as DataDirectoryCleaner
		String fileName = dataDirRes.getAbsolutePath() + File.separatorChar + Configuration.getCombinedConfiguration().getProperty(DataDirectoryCleaner.PROPKEY_savedFileInfo_filename, DataDirectoryCleaner.DEFAULT_savedFileInfo_filename);
		Configuration savedFileInfo = new Configuration(fileName);
		_logger.info("Loaded file '"+savedFileInfo.getFilename()+"' to store File Size Information, with "+savedFileInfo.size()+" entries.");
		
//		for (Path root : FileSystems.getDefault().getRootDirectories()) 
//		{
//			out.print(root + ": ");
//			try {
//				FileStore store = Files.getFileStore(root);
//				out.println("available = " + (store.getUsableSpace()/1024/1024) + " MB, total = " + (store.getTotalSpace()/1024/1024) + " MB <br>");
//			} catch (IOException e) {
//				out.println("error querying space: " + e.toString() + " <br>");
//			}
//		}
		

		String tableHead 
				= "  <tr>"
				+ "    <th>File</th>"
				+ "    <th>DayOfWeek</th>"
				+ "    <th>Saved Max GB</th>"
				+ "    <th>File Size GB</th>"
				+ "    <th>File Size MB</th>"
				+ "    <th>Shrink Size GB</th>"
				+ "    <th>Url (green row is active recording)</th>"
				+ "  </tr>";
		
		out.println("<table>");
		out.println("<thead>");
		out.println(tableHead);
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
				out.println(tableHead); // Also add new "headers" so we don't have to scroll that much
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

			String url        = "jdbc:h2:tcp://" + collectorHostname + "/" + dbName;
//			String dbxTuneUrl = dbxTuneGuiUrl + url;
			String dbxTuneUrl  = dbxTuneGuiUrl.replace(":PORT/", ":"+DbxTune.getGuiWebPort(dbxTuneName)+"/") + url;

			
			String style = "";
			if (isTodayH2DbTimestamp(f.getName()))
			{
				style = "style='background-color:rgb(204, 255, 204);'"; // Very Light green
				
				// Point the URL to the ACTIVE recording (which is on non-default port)
				if (session != null)
				{
					url        = session.getCollectorCurrentUrl();
					dbxTuneUrl = dbxTuneGuiUrl.replace(":PORT/", ":"+DbxTune.getGuiWebPort(dbxTuneName)+"/") + url;
				}
			}
				
			out.println("  <tr>");
			out.println("    <td "+style+">" + dbName         + "</td>");
			out.println("    <td "+style+">" + dayOfWeek      + "</td>");
			out.println("    <td "+style+">" + savedSizeInGB  + "</td>");
			out.println("    <td "+style+">" + sizeInGB       + "</td>");
			out.println("    <td "+style+">" + sizeInMB       + "</td>");
			out.println("    <td "+style+">" + diffSizeInGB   + "</td>");
			out.println("    <td "+style+"><div title='"+linkToolTip+"'><a href='" + dbxTuneUrl + "'><code>" + url + "</code></a></div></td>");
			out.println("  </tr>");
		}
		out.println("</tbody>");
		out.println("</table>");
		out.println("</div>"); // end: card-body
		out.println("</div>"); // end: card

		
		//----------------------------------------------------
		// ACTIVE Recordings (file content)
		//----------------------------------------------------
		out.println("<div id='active_filecontent' class='card border-dark mb-3'>");
		out.println("<h5 class='card-header'>Active Recordings, full meta-data file content</h5>");
		out.println("<div class='card-body'>");
		out.println("<p>When a DbxTune collector starts it writes a <i>information</i> file with various content, this file is deleted when the collector stops. So this is also <i>proof</i> that the collector <i>lives</i></p>");
		out.println("<p>This section just lists the content of those files.</p>");
//		out.println("<br><hr>");
//		out.println("<h3>Active Recordings, full file content</h3>");
		for (String file : getFilesDbxTune())
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

		
		out.println("</div>");

		// Write some JavaScript code
		out.println(HtmlStatic.getJavaScriptAtEnd(true));
		
		out.println("</body>");
		out.println("</html>");
		out.flush();
		out.close();
	}

}
