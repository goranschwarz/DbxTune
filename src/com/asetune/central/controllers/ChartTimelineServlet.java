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
package com.asetune.central.controllers;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.InetAddress;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.asetune.Version;
import com.asetune.central.DbxTuneCentral;
import com.asetune.gui.ResultSetTableModel;
import com.asetune.sql.conn.ConnectionProp;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.StringUtil;

public class ChartTimelineServlet extends HttpServlet
{
//	private static final long serialVersionUID = 1L;
//	private static final Logger _logger = Logger.getLogger(MethodHandles.lookup().lookupClass());
//
////	private static final String HOME_DIR    = DbxTuneCentral.getAppHomeDir();
////	private static final String INFO_DIR    = DbxTuneCentral.getAppInfoDir();
////	private static final String LOG_DIR     = DbxTuneCentral.getAppLogDir();
//	private static final String CONF_DIR    = DbxTuneCentral.getAppConfDir();
////	private static final String REPORTS_DIR = DbxTuneCentral.getAppReportsDir();
////	private static final String DATA_DIR    = DbxTuneCentral.getAppDataDir();
//
//	@Override
//	protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
//	throws ServletException, IOException
//	{
//		ServletOutputStream out = resp.getOutputStream();
//		resp.setContentType("text/html");
//		resp.setCharacterEncoding("UTF-8");
////		resp.setContentType("application/json");
////		resp.setCharacterEncoding("UTF-8");
//		
//		// Check for known input parameters
//		if (Helper.hasUnKnownParameters(req, resp, "refresh", "name", "server", "age"))
//			return;
//
//		
//		int refresh = StringUtil.parseInt( Helper.getParameter(req, "refresh", "60"), 60);
//		int age     = StringUtil.parseInt( Helper.getParameter(req, "age"    , "2") , 2);
//
//
//		String username = System.getProperty("user.name");
//		String hostname = InetAddress.getLocalHost().getHostName();
//		
//		out.println("<html>");
//		out.println("<head>");
//
//		out.println("<title>Timeline</title> ");
//		
//		if (refresh > 0)
//			out.println("<meta http-equiv='refresh' content='"+refresh+"' />");
//
//		out.println(HtmlStatic.getOverviewHead());
//		
//		out.println("<style type='text/css'>");
//		out.println("  table {border-collapse: collapse;}");
//		out.println("  th {border: 1px solid black; text-align: left; padding: 2px; white-space: nowrap; background-color:gray; color:white;}");
//		out.println("  td {border: 1px solid black; text-align: left; padding: 2px; white-space: nowrap; }");
//		out.println("  tr:nth-child(even) {background-color: #f2f2f2;}");
////		out.println("  .topright { position: absolute; top: 8px; right: 16px; font-size: 14px; }"); // topright did not work with bootstrap (and navigation bar) 
//
//		out.println("</style>");
//
//		out.println("<link href='/scripts/prism/prism.css' rel='stylesheet' />");
//
//		out.println("</head>");
//		
//		out.println("<body onload='updateLastUpdatedClock()'>");
//		out.println("<script src='/scripts/prism/prism.js'></script> ");
//
//		out.println(HtmlStatic.getOverviewNavbar());
//
//		out.println("<div class='container-fluid'>");
//
//		String ver = "Version: " + Version.getVersionStr() + ", Build: " + Version.getBuildStr();
//		
//		out.println("<h1>DbxTune - Central - " + username + "@" + hostname + "</h1>");
//
//		out.println("<div class='topright'>"+ver+"</div>");
//
//		out.println("<p>");
////		out.println("Page loaded: " + (new Timestamp(System.currentTimeMillis())) + ", " );
//		out.println("Page loaded: <span id='last-update-ts'>" + (new Timestamp(System.currentTimeMillis())) + "</span>, ");
//		out.println("This page will 'auto-refresh' every " + refresh + " second. This can be changed with URL parameter 'refresh=##' (where ## is seconds)<br>" );
//		out.println("</p>");
//
////		out.println("<p>Sections");
////		out.println("<ul>");
////		out.println("  <li><a href='#active'            >Active Recordings                             </a> </li>");
////		out.println("  <li><a href='#alarms'            >Active Alarms                                 </a> </li>");
////		out.println("  <li><a href='#logfiles'          >All file(s) in the LOG Directory              </a> </li>");
////		out.println("  <li><a href='#conffiles'         >All file(s) in the CONF Directory             </a> </li>");
////		out.println("  <li><a href='#reportfiles'       >All file(s) in the REPORTS Directory          </a> </li>");
////		out.println("  <li><a href='#central'           >Dbx CENTRAL databases                         </a> </li>");
////		out.println("  <li><a href='#offline'           >Available OFFLINE databases                   </a> </li>");
////		out.println("  <li><a href='#active_filecontent'>Active Recordings, full meta-data file content</a> </li>");
////		out.println("</ul>");
////		out.println("</p>");
//		
//		out.println("<script>");
//		out.println("                                                      ");
//		out.println("function updateLastUpdatedClock() {                   ");
//		out.println("    var ageInSec = Math.floor((new Date() - lastUpdateTime) / 1000);");
//		out.println("    document.getElementById('last-update-ts').innerHTML = ageInSec + ' seconds ago'; ");
////		out.println("    console.log('updateLastUpdatedClock(): ' + document.getElementById('last-update-ts'));");
////		out.println("    console.log('updateLastUpdatedClock(): ' + ageInSec + ' seconds ago');");
//		out.println("    setTimeout(updateLastUpdatedClock, 1000); ");
//		out.println("}                                                     ");
//		out.println("var lastUpdateTime = new Date();");
//		out.println("</script>");
//		out.println("");
//		
//		out.println("<script type='text/javascript' src='https://www.gstatic.com/charts/loader.js'></script>");
//
//		try
//		{
//			_logger.info("Creating Timeline Chart");
//			createTimeline(out, age);
//		}
//		catch (Exception ex)
//		{
//			_logger.error("Error when creating Timeline chart", ex);
//
//			out.println( "<div>" );
//			out.println( "<b>ERROR: " + ex.toString() + "</b>");
//			out.println( "</div>" );
//		}
//
//		
//		
////		// -----------------------------------------------------------
////		// get entries from the Persist Reader
////		LinkedHashMap<String, DbxCentralSessions> centralSessionMap = new LinkedHashMap<>();
////		List<DbxCentralSessions> centralSessionList = new ArrayList<>();
////		try
////		{
////			boolean onlyLast  = true;
////			CentralPersistReader reader = CentralPersistReader.getInstance();
////			centralSessionList = reader.getSessions( onlyLast, -1 );
////			
////			for (DbxCentralSessions s : centralSessionList)
////				centralSessionMap.put(s.getServerName(), s);
////		}
////		catch(SQLException ex) 
////		{
////			out.println("Problems reading from PersistReader. Caught: "+ex);
////		}
//
//
//
//
//		
//		//----------------------------------------------------
//		// ACTIVE Recordings (file content)
//		//----------------------------------------------------
//		if (true)
//		{
////			out.println("<div id='active_filecontent' class='card border-dark mb-3'>");
////			out.println("<h5 class='card-header'>Active Recordings, full meta-data file content</h5>");
////			out.println("<div class='card-body'>");
////			out.println("<p>When a DbxTune collector starts it writes a <i>information</i> file with various content, this file is deleted when the collector stops. So this is also <i>proof</i> that the collector <i>lives</i></p>");
////			out.println("<p>This section just lists the content of those files.</p>");
////			for (String file : getInfoFilesDbxTune())
////			{
////				File f = new File(file);
////				String srvName = f.getName().split("\\.")[0];
////
////				out.println("<h3>" + srvName + "</h3>");
////
////				String fileContent = FileUtils.readFile(f, null);
////				
////				// remove some backslashes '\' for readability
////				if (fileContent != null)
////				{
////					fileContent = fileContent.replace("\\\\", "\\");
////					fileContent = fileContent.replace("\\:", ":");
////					fileContent = fileContent.replace("\\=", "=");
////				}
////				
////				out.println("Content of file: "+file);
////				out.println("<hr>");
////				out.println("<pre>");
////				out.println("<code class='language-properties line-numbers'>");
////				out.println(fileContent);
////				out.println("</code>");
////				out.println("</pre>");
////				out.println("<hr>");
////			}
////			out.println("</div>"); // end: card-body
////			out.println("</div>"); // end: card
////			out.println("<br><br>");
//		} // end: ACTIVE Recordings (file content)
//
//		
//		out.println("</div>");
//
//		// Write Daily Summary HTML & JavaScript
////		out.println(getHtmlForDailySummaryReportDialog());
//		
//		// Write some JavaScript code
//		out.println(HtmlStatic.getJavaScriptAtEnd(true));
//
//		out.println("</body>");
//		out.println("</html>");
//		out.flush();
//		out.close();
//
//	} // end: doGet
//
//	
//	private void createTimeline(ServletOutputStream out, int age) 
//	throws Exception
//	{
//		String jdbcUrl = "jdbc:sybase:Tds:sek-syb-p01.sek.se:8000/SEK_CONFIG_prod?ENCRYPT_PASSWORD=true";
////		String jdbcUser = "LIMITS";
////		String jdbcPass = "LIMITS";
////		String jdbcUser = "INSTAL";
////		String jdbcPass = "INSTALL";
//		String jdbcUser = "sa";
//		String jdbcPass = "BlaPenn@3xx";
//
//		String sql = ""
//			    + "select \n"
//			    + "      label    = meid \n"
//			    + "    , barText  = scriptname + ' ' + scriptargs \n"
//			    + "    , barColor = CASE WHEN eoj is null         THEN 'green' \n"
//			    + "                      WHEN status  = 'WARNING' THEN 'orange' \n"
//			    + "                      WHEN status != 'DONE'    THEN 'yellow' \n"
//			    + "                      ELSE 'blue' \n"
//			    + "                 END \n"
//			    + "    , startDate = soj \n"
//			    + "    , endDate   = isnull(eoj, getdate()) \n"
//			    + "from dbo.MxGJobStatus \n"
//			    + "where soj > dateadd(hour, -" + age + ", getdate()) \n"
//			    + "order by soj \n"
//			    + "";
//		
//		_logger.info("Timeline Chart: Connecting to URL='" + jdbcUrl + "', username='" + jdbcUser + "'.");
//		DbxConnection conn = connect(jdbcUrl, jdbcUser, jdbcPass);
//		
//
//		_logger.info("Timeline Chart: executing SQL=|" + StringUtils.normalizeSpace(sql) + "|.");
//
//		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql); )
//		{
//			ResultSetMetaData rsmd = rs.getMetaData();
//			int colCount = rsmd.getColumnCount();
//			if (colCount != 5)
//			{
//				throw new Exception("The ResultSet must have 5 columns (it has " + colCount + "). 1=[label:String], 2=[barText:String], 3=[barColor:String], 4=[startDate:Timestamp], 5=[endDate:Timestamp]");
//			}
//
//			int col4_datatype = rsmd.getColumnType(4);
//			int col5_datatype = rsmd.getColumnType(5);
//
//			if (col4_datatype != Types.TIMESTAMP) throw new Exception("The ResultSet for column 4 has to be of type TIMESTAMP, it was " + ResultSetTableModel.getColumnJavaSqlTypeName(col4_datatype) + ". Expected ResultSet: 1=[label:String], 2=[barText:String], 3=[barColor:String], 4=[startDate:Timestamp], 5=[endDate:Timestamp]");
//			if (col5_datatype != Types.TIMESTAMP) throw new Exception("The ResultSet for column 5 has to be of type TIMESTAMP, it was " + ResultSetTableModel.getColumnJavaSqlTypeName(col5_datatype) + ". Expected ResultSet: 1=[label:String], 2=[barText:String], 3=[barColor:String], 4=[startDate:Timestamp], 5=[endDate:Timestamp]");
//				
//			out.println("<script>");
//			out.println("     google.charts.load('current', {'packages':['timeline']});								");
//			out.println("     google.charts.setOnLoadCallback(drawChart);											");
//			out.println("     function drawChart()																	");
//			out.println("     {																						");
//			out.println("       var container = document.getElementById('timeline');								");
//			out.println("       var chart = new google.visualization.Timeline(container);							");
//			out.println("       var dataTable = new google.visualization.DataTable();								");
//			out.println("																							");
//			out.println("       dataTable.addColumn({ type: 'string', id: 'TextLabel' });							");
//			out.println("       dataTable.addColumn({ type: 'string', id: 'BarText'   });							");
//			out.println("       dataTable.addColumn({ type: 'string', role: 'style' });		// bar color			");
//			out.println("       dataTable.addColumn({ type: 'date', id: 'Start' });									");
//			out.println("       dataTable.addColumn({ type: 'date', id: 'End' });									");
//			out.println("       dataTable.addRows([																	");
//			
//			String prefix = " ";
//			int rows = 0;
//			while(rs.next())
//			{
//				rows++;
//				String label      = rs.getString   (1);
//				String barText    = rs.getString   (2);
//				String barColor   = rs.getString   (3);
//				Timestamp startTs = rs.getTimestamp(4);
//				Timestamp endTs   = rs.getTimestamp(5);
//				
//				if (StringUtil.isNullOrBlank(barColor))
//					barColor = "green";
//
//				if (endTs == null)
//					endTs = new Timestamp(System.currentTimeMillis());
//
//				label   = escapeJsQuote(label);
//				barText = escapeJsQuote(barText);
//
//				out.println("        " + prefix + "[ '" + label + "', '" + barText + "', '" + barColor + "', new Date('" + startTs + "'), new Date('" + endTs + "') ]");
//				prefix = ",";
//			}
//			_logger.info("Timeline Chart: read " + rows + " rows from the database into the chart.");
//			
//			out.println("       ]);																					");
//			out.println("																							");
//			out.println("       var options = 																		");
//			out.println("       {																					");
////			out.println("              timeline: { singleColor: '#8d8' },											");
////			out.println("              width: '100%',																");
////			out.println("              height: '100%'																");
//			out.println("       };																					");
//			out.println("																							");
//			out.println("       chart.draw(dataTable, options);														");
//			out.println("     }																						");
//			out.println("																							");
//			out.println("     // Scroll to bottom of scroll															");
//			out.println("     var element = document.getElementById('timeline');									");
//			out.println("     element.scrollTop = element.scrollHeight												");
//			out.println("</script>");
//
////			out.println("<div id='timeline' style='height: " + (rows * 25) +"px; width: " + 1000 + "px;'></div>");
////			out.println("<div id='timeline' style='height: 100vh;'></div>");
//			out.println("<div id='timeline' style='height: 80%;'></div>");
//
////			out.println("<script>");
////			out.println("     // Scroll to bottom of scroll															");
////			out.println("     var element = document.getElementById('timeline');									");
////			out.println("     element.scrollTop = element.scrollHeight;												");
////			out.println("</script>");
//		}
//
//		conn.closeNoThrow();
//
//
////		out.println("         [ 'a', 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', 'green', new Date(2020,10,20, 14,1,1), new Date(2020,10,20, 14,1,2) ],				");
////		out.println("         [ 'b', 'bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb', 'green', new Date(2020,10,20, 14,2,1), new Date(2020,10,20, 14,2,2) ],				");
////		out.println("         [ 'c', 'cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc', 'green', new Date(2020,10,20, 14,3,1), new Date(2020,10,20, 14,23,1) ],				");
////		out.println("         [ 'd', 'dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd', 'green', new Date(2020,10,20, 14,3,1), new Date(2020,10,20, 14,23,1) ],				");
////		out.println("         [ 'e', 'eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee', 'green', new Date(2020,10,20, 14,3,1), new Date(2020,10,20, 14,23,1) ],				");
////		out.println("         [ 'f', 'ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff', 'blue', new Date(2020,10,20, 14,3,1), new Date(2020,10,20, 14,23,1) ],				");
////		out.println("         [ 'g', 'gggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggg', 'red', new Date(2020,10,20, 14,3,1), new Date(2020,10,20, 14,23,1) ],				");
////		out.println("         [ 'h', 'hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh', 'orange', new Date(2020,10,20, 14,3,1), new Date(2020,10,20, 14,23,1) ]				");
//	}
//	
//	private String escapeJsQuote(String str)
//	{
//		if (str == null)
//			return str;
//		
//		return str.replace("'", "\\x27");
//	}
//	
//	private DbxConnection connect(String  jdbcUrl, String jdbcUser, String  jdbcPass)
//	throws Exception
//	{
//		DbxConnection conn = null;
//
//		if (jdbcPass == null)
//			jdbcPass = "";
//
//		ConnectionProp cp = new ConnectionProp();
//		cp.setUrl(jdbcUrl);
//		cp.setUsername(jdbcUser);
//		cp.setPassword(jdbcPass);
//		cp.setAppName(Version.getAppName());
//
//		conn = DbxConnection.connect(null, cp);
//
//		return conn;
//	}
//
//
//	private static String getHtmlForDailySummaryReportDialog()
//	{
////		return "FIXME";
//
//		StringBuilder sb = new StringBuilder(1024);
//		
//		sb.append("	<!-- Modal: DSR Daily Summary Report dialog -->																					\n");
//		sb.append("	<div class='modal fade' id='dbx-dsr-dialog' tabindex='-1' role='dialog' aria-labelledby='dbx-dsr-dialog' aria-hidden='true'>	\n");
//		sb.append("		<div class='modal-dialog modal-dialog-centered modal-lg' role='document'>													\n");
//		sb.append("		<div class='modal-content'>																									\n");
//		sb.append("			<div class='modal-header'>																								\n");
//		sb.append("			<h5 class='modal-title' id='dbx-dsr-dialog-title'>Create a Daily Summary Report - (With Time Bounderies)</h5>			\n");
//		sb.append("			<button type='button' class='close' data-dismiss='modal' aria-label='Close'>											\n");
//		sb.append("				<span aria-hidden='true'>&times;</span>																				\n");
//		sb.append("			</button>																												\n");
//		sb.append("			</div>																													\n");
//		sb.append("			<div class='modal-body'>																								\n");
////		sb.append("				<ul>																												\n");
////		sb.append("					<li>Action: Describe me 1</li>																					\n");
////		sb.append("					<li>Action: Describe me 2</li>																					\n");
////		sb.append("				</ul>																												\n");
//		sb.append("				<form>																													\n");
//		sb.append("					<div class='form-row'>																								\n");
//		sb.append("						<label for='dbx-dsr-dbname'>Recording DB Name (full URL or H2 DB name))</label>									\n");
//		sb.append("						<input  id='dbx-dsr-dbname' type='text' class='form-control is-valid' placeholder='dbname' required>			\n");
//		sb.append("					</div>																												\n");
//		sb.append("					<div class='form-row'>																								\n");
//		sb.append("						<div class='col-md-4 mb-3'>																						\n");
////		sb.append("						<div class='col-auto'>																							\n");
//		sb.append("							<label for='dbx-dsr-username-txt'>DB User Name</label>														\n");
//		sb.append("							<input  id='dbx-dsr-username-txt' type='text' class='form-control is-valid' placeholder='Username, sa is default' />	\n");
//		sb.append("						</div>																											\n");
//		sb.append("						<div class='col-md-4 mb-3'>																						\n");
////		sb.append("						<div class='col-auto'>																							\n");
//		sb.append("							<label for='dbx-dsr-password-txt'>DB Password</label>														\n");
//		sb.append("							<input  id='dbx-dsr-password-txt' type='password' class='form-control is-valid' placeholder='Password, blank is default' />	\n");
//		sb.append("						</div>																											\n");
//		sb.append("					</div>																											\n");
//		sb.append("					<div class='form-row'>																								\n");
//		sb.append("						<div class='col-md-4 mb-3'>																						\n");
////		sb.append("						<div class='col-auto'>																							\n");
//		sb.append("							<label for='dbx-dsr-begin-time'>Report Begin Time</label>													\n");
//		sb.append("							<input  id='dbx-dsr-begin-time' type='text' class='form-control is-valid' placeholder='HH:mm (or blank)' pattern='^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$' title='Must be HH:MM, where HH=00-23 and MM=00-59'/>	\n");
//		sb.append("							<div class='invalid-feedback'>Must be HH:MM, where HH=00-23 and MM=00-59</div>								\n");
//		sb.append("						</div>																											\n");
//		sb.append("						<div class='col-md-4 mb-3'>																						\n");
////		sb.append("						<div class='col-auto'>																							\n");
//		sb.append("							<label for='dbx-dsr-end-time'>Report End Time</label>														\n");
//		sb.append("							<input  id='dbx-dsr-end-time' type='text' class='form-control is-valid' placeholder='HH:mm (or blank)' pattern='^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$' title='Must be HH:MM, where HH=00-23 and MM=00-59' />	\n");
//		sb.append("							<div class='invalid-feedback'>Must be HH:MM, where HH=00-23 and MM=00-59</div>								\n");
//		sb.append("						</div>																											\n");
//		sb.append("					</div>																											\n");
//		sb.append("				</form>																												\n");
//		sb.append("				<div id='dbx-dsr-progress-div' class='progress'>																	\n");
//		sb.append("					<div id='dbx-dsr-progress-bar' class='progress-bar progress-bar-striped active' role='progressbar' aria-valuenow='0' aria-valuemin='0' aria-valuemax='100' style='width:0%; min-width:25px'>	\n");
//		sb.append("					0%																												\n");
//		sb.append("					</div>																											\n");
//		sb.append("				</div>																												\n");
//		sb.append("				<div id='dbx-dsr-progress-txt'>																						\n");
//		sb.append("				</div>																												\n");
////		sb.append("				<br>																												\n");
////		sb.append("				<table id='dbx-dsr-filter-table'																					\n"); 
////		sb.append("					class='table-responsive' 																						\n");
////		sb.append("					data-show-columns='false' 																						\n");
////		sb.append("					data-paging='false' 																							\n");
////		sb.append("					data-filtering='true'																							\n"); 
////		sb.append("					data-filter-control='true' 																						\n");
////		sb.append("					data-click-to-select='false'																					\n");
////		sb.append("					data-sorting='true'																								\n");
////		sb.append("					data-checkbox-header='true'																						\n");
////		sb.append("					data-maintain-selected='true'																					\n");
////		sb.append("					data-ignore-click-to-select-on=scrollToDisableCheckRow()>														\n");
////		sb.append("					<thead>																											\n");
////		sb.append("						<tr>																										\n");
////		sb.append("							<th data-field='visible'   data-checkbox='true'></th>													\n");
////		sb.append("							<th data-field='cm'        data-filter-control='input'  data-sortable='true'>Counter Model</th>			\n");
////		sb.append("							<th data-field='desc'      data-filter-control='input'  data-sortable='true'>Description</th>			\n");
////		sb.append("						</tr>																										\n");
////		sb.append("					</thead>																										\n");
////		sb.append("					<tbody>																											\n");
////		sb.append("						<tr> 																										\n");
////		sb.append("							<td class='bs-checkbox '><input data-index='0' name='btSelectItem' type='checkbox'></td>				\n");
////		sb.append("							<td>dummy-1</td> 																						\n");
////		sb.append("							<td>dummy-2</td> 																						\n");
////		sb.append("						</tr>																										\n");
////		sb.append("					</tbody>          																								\n");      
////		sb.append("				</table>																											\n");
//		sb.append("			</div>																													\n");
//		sb.append("			<div class='modal-footer'>																								\n");
////		sb.append("			<button type='button' class='btn btn-primary' data-dismiss='modal' id='dbx-dsr-dialog-ok'>Create</button>				\n");
//		sb.append("			<button type='button' class='btn btn-primary' id='dbx-dsr-dialog-ok'>Create</button>				\n");
//		sb.append("			<button type='button' class='btn btn-default' data-dismiss='modal'>Close</button>										\n");
//		sb.append("			</div>																													\n");
//		sb.append("		</div>																														\n");
//		sb.append("		</div>																														\n");
//		sb.append("	</div>  																														\n");
//		sb.append("	<!-- -->																														\n");
//		sb.append("\n");
//		sb.append("\n");
//		sb.append("\n");
//
//
//
////				<button id="dbx-filter"              class="btn btn-outline-light mx-2 my-0 my-sm-0" type="button">Filter</button>
//
//		sb.append("<script>																												\n");
////		sb.append("	// Install functions for button: dbx-filter																			\n");
////		sb.append("	$('#dbx-filter').click( function() 																					\n");
////		sb.append("	{																													\n");
////		sb.append("		dbxOpenDsrDialog();																								\n");
////		sb.append("	});																													\n");
//		sb.append("																														\n");
//		sb.append("	$('#dbx-dsr-dialog').on('show.bs.modal', function () {																\n");
//		sb.append("		$(this).find('.modal-body').css({																				\n");
//		sb.append("			'max-height':'100%'																							\n");
//		sb.append("		});																												\n");
//		sb.append("	});																													\n");
//		
////		sb.append("	// Example starter JavaScript for disabling form submissions if there are invalid fields							\n");
////		sb.append("	(function() {																										\n");
////		sb.append("	  'use strict';																										\n");
////        sb.append("																														\n");
////		sb.append("	  window.addEventListener('load', function() {																		\n");
////		sb.append("	    var form = document.getElementById('needs-validation');															\n");
////		sb.append("	    form.addEventListener('submit', function(event) {																\n");
////		sb.append("	      if (form.checkValidity() === false) {																			\n");
////		sb.append("	        event.preventDefault();																						\n");
////		sb.append("	        event.stopPropagation();																					\n");
////		sb.append("	      }																												\n");
////		sb.append("	      form.classList.add('was-validated');																			\n");
////		sb.append("	    }, false);																										\n");
////		sb.append("	  }, false);																										\n");
////		sb.append("	})();																												\n");
//		
//		sb.append("																														\n");
//		sb.append("	// What should happen when we click OK in the dialog																\n");
//		sb.append("	$('#dbx-dsr-dialog-ok').click( function() 																			\n");
//		sb.append("	{																													\n");
//		sb.append("		// Show progress field																							\n");
//		sb.append("		$('#dbx-dsr-progress-div').show();																				\n");
//		sb.append("		$('#dbx-dsr-progress-bar').css('width', '0%');																	\n");
//		sb.append("		$('#dbx-dsr-progress-bar').html('0%');																			\n");
//		sb.append("																														\n");
//		sb.append("		// build the URL params																							\n");
//		sb.append("		var dsrDbname    = $('#dbx-dsr-dbname').val();																	\n");
//		sb.append("		var dsrUsername  = $('#dbx-dsr-username-txt').val();															\n");
//		sb.append("		var dsrPassword  = $('#dbx-dsr-password-txt').val();															\n");
//		sb.append("		var dsrBeginTime = $('#dbx-dsr-begin-time').val();																\n");
//		sb.append("		var dsrEndTime   = $('#dbx-dsr-end-time').val();																\n");
//		sb.append("																														\n");
////		sb.append("		console.log('dsrDbname    = |' + dsrDbname + '|');																\n");
////		sb.append("		console.log('dsrUsername  = |' + dsrUsername + '|');															\n");
////		sb.append("		console.log('dsrPassword  = |' + dsrPassword + '|');															\n");
////		sb.append("		console.log('dsrBeginTime = |' + dsrBeginTime + '|');															\n");
////		sb.append("		console.log('dsrEndTime   = |' + dsrEndTime + '|');																\n");
////		sb.append("																														\n");
//		sb.append("		if (dsrDbname)    { dsrDbname    = '&dbname='    + dsrDbname;    }												\n");
//		sb.append("		if (dsrUsername)  { dsrUsername  = '&username='  + dsrUsername;  }												\n");
//		sb.append("		if (dsrPassword)  { dsrPassword  = '&password='  + dsrPassword;  }												\n");
//		sb.append("		if (dsrBeginTime) { dsrBeginTime = '&beginTime=' + dsrBeginTime; }												\n");
//		sb.append("		if (dsrEndTime)   { dsrEndTime   = '&endTime='   + dsrEndTime;   }												\n");
//		sb.append("																														\n");
//		sb.append("		// create the event source																						\n");
//		sb.append("		var fullUrl = encodeURI('/api/dsr?op=get' + dsrDbname + dsrUsername + dsrPassword + dsrBeginTime + dsrEndTime);	\n");
//		sb.append("		console.log('EventSource - FullURL: ' + fullUrl);																\n");
//		sb.append("		var sse = new EventSource(fullUrl);																				\n");
//		sb.append("																														\n");
//		sb.append("		// set initial progress text																					\n");
//		sb.append("		$('#dbx-dsr-progress-txt').text('Requested a DSR request. URL: ' + fullUrl);									\n");
//		sb.append("																														\n");
//		sb.append("		// ---------------------------------------------------------													\n");
//		sb.append("		// PROGRESS																										\n");
//		sb.append("		// ---------------------------------------------------------													\n");
//		sb.append("		sse.addEventListener('progress', function(event) 																\n");
//		sb.append("		{																												\n");
//		sb.append("			console.log('ON-PROGRESS: ' + event, event);																\n");
//		sb.append("																														\n");
//		sb.append("			// data expected to be in JSON-format, so parse 															\n");
//		sb.append("			var data = JSON.parse(event.data);																			\n");
//		sb.append("																														\n");
//		sb.append("			$('#dbx-dsr-progress-txt').css('color', 'black');															\n");
//		sb.append("			$('#dbx-dsr-progress-txt').text(data.progressText);															\n");
//		sb.append("																														\n");
//		sb.append("			var pct = data.percentDone;																					\n");
//		sb.append("			if (data.state === 'AFTER')																					\n");
//		sb.append("			{																											\n");
//		sb.append("				$('#dbx-dsr-progress-bar').css('width', pct+'%');														\n");
//		sb.append("				$('#dbx-dsr-progress-bar').html(pct+'%');																\n");
//		sb.append("			}																											\n");
//		sb.append("																														\n");
//		sb.append("			// update status								 															\n");
//		sb.append("			if (data.state === 'AFTER' && pct === 100)																	\n");
//		sb.append("			{																											\n");
//		sb.append("				$('#dbx-dsr-progress-txt').text('Report Content will now be transferred... This may take time, then a new tab will be opened!');	\n");
//		sb.append("			}																											\n");
//		sb.append("		});																												\n");
//		sb.append("																														\n");
//		sb.append("		// ---------------------------------------------------------													\n");
//		sb.append("		// COMPLETE																										\n");
//		sb.append("		// ---------------------------------------------------------													\n");
//		sb.append("		sse.addEventListener('complete', function(event) 																\n");
//		sb.append("		{																												\n");
//		sb.append("			console.log('ON-COMPLETE: ' + event, event);																\n");
//		sb.append("																														\n");
//		sb.append("			// data expected to be in JSON-format, so parse 															\n");
//		sb.append("			var data = JSON.parse(event.data);																			\n");
//		sb.append("																														\n");
//		sb.append("			// close the connection to server																			\n");
//		sb.append("			sse.close();																								\n");
//		sb.append("																														\n");
//		sb.append("			$('#dbx-dsr-progress-txt').css('color', 'black');															\n");
//		sb.append("			$('#dbx-dsr-progress-txt').text('Report is complete...');													\n");
//		sb.append("																														\n");
//		sb.append("			var newDsrTab = window.open('','_blank');																	\n");
//		sb.append("			newDsrTab.document.write(data.complete);																	\n");
//		sb.append("																														\n");
//		sb.append("			$('#dbx-dsr-progress-txt').text('A New tab with the Daily Summary Report has been opened.');				\n");
//		sb.append("			$('#dbx-dsr-progress-div').hide();																			\n");
//		sb.append("																														\n");
////		sb.append("			// Hide the dialog																							\n");
////		sb.append("			$('#dbx-dsr-dialog').modal('hide');																			\n");
//		sb.append("		});																												\n");
//		sb.append("																														\n");
////		sb.append("		// handle incoming messages																						\n");
////		sb.append("		sse.onmessage = function(event) 																				\n");
////		sb.append("		{																												\n");
////		sb.append("			console.log('ON-MESSAGE: ' + event, event);																	\n");
////		sb.append("																														\n");
////		sb.append("			$('#dbx-dsr-progress-txt').css('color', 'black');															\n");
////		sb.append("																														\n");
////		sb.append("			if (event.type == 'message') 																				\n");
////		sb.append("			{																											\n");
////		sb.append("				// data expected to be in JSON-format, so parse 														\n");
////		sb.append("				var data = JSON.parse(event.data);																		\n");
////		sb.append("																														\n");
////		sb.append("				if (data.hasOwnProperty('complete'))																	\n");
////		sb.append("				{																										\n");
////		sb.append("					// close the connection so browser does not keep connecting											\n");
////		sb.append("					sse.close();																						\n");
////		sb.append("																														\n");
////		sb.append("					$('#dbx-dsr-progress-txt').text('Report is complete...');											\n");
////		sb.append("																														\n");
////		sb.append("					var newDsrTab = window.open('','_blank');															\n");
////		sb.append("					newDsrTab.document.write(data.complete);															\n");
////		sb.append("																														\n");
////		sb.append("					$('#dbx-dsr-progress-txt').text('A New tab with the Daily Summary Report has been opened.');		\n");
////		sb.append("					$('#dbx-dsr-progress-div').hide();																	\n");
////		sb.append("																														\n");
//////		sb.append("					// Hide the dialog																					\n");
//////		sb.append("					$('#dbx-dsr-dialog').modal('hide');																	\n");
////		sb.append("				}																										\n");
////		sb.append("				// otherwise, it's a progress update so just update progress bar										\n");
////		sb.append("				else 																									\n");
////		sb.append("				{																										\n");
////		sb.append("					$('#dbx-dsr-progress-txt').text(data.progressText);													\n");
////		sb.append("																														\n");
////		sb.append("					var pct = data.percentDone;																			\n");
////		sb.append("					if (data.state === 'AFTER')																			\n");
////		sb.append("					{																									\n");
////		sb.append("						$('#dbx-dsr-progress-bar').css('width', pct+'%');												\n");
////		sb.append("						$('#dbx-dsr-progress-bar').html(pct+'%');														\n");
////		sb.append("					}																									\n");
////		sb.append("																														\n");
////		sb.append("					// update status								 													\n");
////		sb.append("					if (data.state === 'AFTER' && pct === 100)															\n");
////		sb.append("					{																									\n");
////		sb.append("						$('#dbx-dsr-progress-txt').text('Report Content will now be transferred... This may take time, then a new tab will be opened!');	\n");
////		sb.append("					}																									\n");
////		sb.append("				}																										\n");
////		sb.append("			}																											\n");
////		sb.append("			else						 																				\n");
////		sb.append("			{																											\n");
////		sb.append("				$('#dbx-dsr-progress-txt').text('Unhandled event type: ' + event.type);									\n");
////		sb.append("				$('#dbx-dsr-progress-txt').css('color', 'red');															\n");
////		sb.append("			}																											\n");
////		sb.append("		};																												\n");
//		sb.append("		// ---------------------------------------------------------													\n");
//		sb.append("		// ERROR																										\n");
//		sb.append("		// ---------------------------------------------------------													\n");
//		sb.append("		sse.onerror = function(event) 																					\n");
//		sb.append("		{																												\n");
//		sb.append("			sse.close();																								\n");
//		sb.append("			console.log('SSE-ON-COMPLETE: ', event);																	\n");
//		sb.append("			$('#dbx-dsr-progress-txt').text('ERROR: ' + event.data);													\n");
//		sb.append("			$('#dbx-dsr-progress-txt').css('color', 'red');																\n");
//		sb.append("		};																												\n");
//
////		sb.append("		var selectedRecords = $('#dbx-dsr-filter-table').bootstrapTable('getSelections');								\n");
////		sb.append("																														\n");
////		sb.append("		// hide ALL graphs																								\n");
////		sb.append("		for(let i=0; i<_graphMap.length; i++)																			\n");
////		sb.append("		{																												\n");
////		sb.append("			const dbxGraph = _graphMap[i];																				\n");
////		sb.append("			var x = document.getElementById(dbxGraph.getFullName());													\n");
////		sb.append("			x.style.display = 'none';																					\n");
////		sb.append("			// console.log('HIDE: x='+x, x);																			\n");
////		sb.append("		}																												\n");
////		sb.append("		// show marked ones ALL graphs																					\n");
////		sb.append("		for (let i = 0; i < selectedRecords.length; i++)																\n"); 
////		sb.append("		{																												\n");
////		sb.append("			const record = selectedRecords[i];																			\n");
////		sb.append("			var x = document.getElementById(record.fullName);															\n");
////		sb.append("			x.style.display = 'block';																					\n");
////		sb.append("			// console.log('SHOW: x='+x, x);																			\n");
////		sb.append("		}																												\n");
//		sb.append("	});																													\n");
//
//		sb.append("\n");
//		sb.append("\n");
//		sb.append("\n");
//
//		sb.append("	function dbxOpenDsrDialog(dbname)																					\n");
//		sb.append("	{																													\n");
//		sb.append("		console.log('dbxOpenDsrDialog(dbname='+dbname+')');																\n");
//		sb.append("																														\n");
////		sb.append("		// loop all available graphs and add it to the table in the dialog												\n");
////		sb.append("		if (_filterDialogContentArr.length === 0)																		\n");
////		sb.append("		{																												\n");
////		sb.append("			for(let i=0; i<_graphMap.length; i++)																		\n");
////		sb.append("			{																											\n");
////		sb.append("				const dbxGraph = _graphMap[i];																			\n");
////		sb.append("																														\n");
////		sb.append("				var row = {																								\n");
////		sb.append("					'visible'   : true,																					\n");
////		sb.append("					'desc'      : dbxGraph.getGraphLabel(),																\n");
////		sb.append("					'type'      : dbxGraph.getGraphCategory(),															\n");
////		sb.append("					'cm'        : dbxGraph.getCmName(),																	\n");
////		sb.append("					'graphName' : dbxGraph.getGraphName(),																\n");
////		sb.append("					'fullName'  : dbxGraph.getFullName(),																\n");
////		sb.append("				};																										\n");
////		sb.append("				_filterDialogContentArr.push(row);																		\n");
////		sb.append("			}																											\n");
////		sb.append("																														\n");
////		sb.append("			$('#dbx-dsr-filter-table').bootstrapTable({data: _filterDialogContentArr});									\n");
////		sb.append("		}																												\n");
//		sb.append("																														\n");
//		sb.append("		// set some fields																								\n");
//		sb.append("		$('#dbx-dsr-dbname').val(dbname);																				\n");
//		sb.append("																														\n");
//		sb.append("		// Hide progress field																							\n");
//		sb.append("		$('#dbx-dsr-progress-div').hide();																				\n");
//		sb.append("		$('#dbx-dsr-progress-txt').text('');																			\n");
//		sb.append("																														\n");
//		sb.append("		// Show the dialog																								\n");
//		sb.append("		$('#dbx-dsr-dialog').modal('show');																				\n");
//		sb.append("																														\n");
//		sb.append("		return false;																									\n");
//		sb.append("	}																													\n");
//
//		sb.append("</script>																											\n");
//		
//		sb.append("\n");
//		sb.append("\n");
//		sb.append("\n");
//
//		return sb.toString();
//	}
}
