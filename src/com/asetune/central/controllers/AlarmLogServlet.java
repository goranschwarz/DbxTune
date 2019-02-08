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
import java.sql.Timestamp;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.asetune.central.DbxTuneCentral;
import com.asetune.central.pcs.CentralPersistReader;
import com.asetune.central.pcs.objects.DbxAlarmHistory;
import com.asetune.utils.StringUtil;
import com.asetune.utils.TimeUtils;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

public class AlarmLogServlet extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	private static final Logger _logger = Logger.getLogger(MethodHandles.lookup().lookupClass());

	private final String LOG_DIR  = DbxTuneCentral.getAppLogDir();

	ServletOutputStream out = null;
	HttpServletResponse _resp = null;
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		_resp = resp;
		out = resp.getOutputStream();

		
		// Check for known input parameters
		if (Helper.hasUnKnownParameters(req, resp, "name", "type", "method"))
			return;

		String inputName   = Helper.getParameter(req, "name");
		String inputType   = Helper.getParameter(req, "type");
		String inputMethod = Helper.getParameter(req, "method");
		
		System.out.println("AlarmLogServlet: name = '"+inputName+"'.");
		System.out.println("AlarmLogServlet: type = '"+inputType+"'.");

		if  ("json".equalsIgnoreCase(inputMethod))
		{
			if (StringUtil.isNullOrBlank(inputName))
				throw new ServletException("No input parameter named 'name'.");

			resp.setContentType("text/html");
//			resp.setContentType("application/json");
			resp.setCharacterEncoding("UTF-8");
			asJson(inputName, inputType);
		}
		else if ("plain".equalsIgnoreCase(inputMethod))
		{
			if (StringUtil.isNullOrBlank(inputName))
				throw new ServletException("No input parameter named 'name'.");

			resp.setContentType("text/html");
			resp.setCharacterEncoding("UTF-8");

			asCode(inputName, inputType);
		}
		else if ("pcs".equalsIgnoreCase(inputMethod))
		{
			resp.setContentType("text/html");
			resp.setCharacterEncoding("UTF-8");

			fromPcsAsHtmlTable(inputName, inputType);
		}
		else
		{
			if (StringUtil.isNullOrBlank(inputName))
				throw new ServletException("No input parameter named 'name'.");

			resp.setContentType("text/html");
			resp.setCharacterEncoding("UTF-8");
			asHtml(inputName, inputType);
		}
	}

	private void asJson(String inputName, String inputType)
	throws ServletException, IOException
	{
		JsonFactory jfactory = new JsonFactory();
		JsonGenerator w = jfactory.createGenerator(out);
//		w.setPrettyPrinter(new DefaultPrettyPrinter());
		
		// Open file and itterate
		File f = new File(LOG_DIR+"/"+inputName);
		try ( FileInputStream in = new FileInputStream(f); 
		      BufferedReader  br = new BufferedReader(new InputStreamReader(in)); )
		{
			// start array
			w.writeStartArray();

			String line;
			while ((line = br.readLine()) != null)
			{
				String[] words = line.split(" - ");
				if (words.length < 10)
					continue;
				
//				String timeStr   = words[0].trim();
//				String type      = words[1].trim();
//				String className = words[2].trim();
//				String srvName   = words[3].trim();
//				String cmName    = words[4].trim();
//				String extraInfo = words[5].trim();
//				String category  = words[6].trim();
//				String severity  = words[7].trim();
//				String state     = words[8].trim();
//				String duration  = words[9].trim();
//				String msgText   = words[10].trim();

				String timeStr   = "";
				String type      = "";
				String className = "";
				String srvName   = "";
				String cmName    = "";
				String extraInfo = "";
				String category  = "";
				String severity  = "";
				String state     = "";
				String duration  = "";
				String msgText   = "";

				if (words.length == 10)
				{
    				timeStr   = words[0].trim();
    				type      = words[1].trim();
    				className = words[2].trim();
    				srvName   = words[3].trim();
    				cmName    = words[4].trim();
    				extraInfo = words[5].trim();
    				category  = "";
    				severity  = words[6].trim();
    				state     = words[7].trim();
    				duration  = words[8].trim();
    				msgText   = words[9].trim();
				}
				else
				{
    				timeStr   = words[0].trim();
    				type      = words[1].trim();
    				className = words[2].trim();
    				srvName   = words[3].trim();
    				cmName    = words[4].trim();
    				extraInfo = words[5].trim();
    				category  = words[6].trim();
    				severity  = words[7].trim();
    				state     = words[8].trim();
    				duration  = words[9].trim();
    				msgText   = words[10].trim();
				}

				if (StringUtil.hasValue(inputType))
				{
					if ( ! inputType.equals(type) )
						continue;
				}

				String extraStyling = "";
				if ("ERROR".equals(severity))
					extraStyling = "background-color:rgb(255,0,0)";
				
				// Translate the logTimestamp into iso8601 format
				Timestamp ts = TimeUtils.parseToTimestamp(timeStr);
				timeStr = TimeUtils.toStringIso8601(ts);

				// Start object
				w.writeStartObject();

				// Write fields
				w.writeStringField("time"      , timeStr   );
				w.writeStringField("type"      , type      );
				w.writeStringField("className" , className );
				w.writeStringField("srvName"   , srvName   );
				w.writeStringField("cmName"    , cmName    );
				w.writeStringField("extraInfo" , extraInfo );
				w.writeStringField("category"  , category  );
				w.writeStringField("severity"  , severity  );
				w.writeStringField("state"     , state     );
				w.writeStringField("duration"  , duration  );
				w.writeStringField("msgText"   , msgText   );

				// end object
				w.writeEndObject();
			}

			// end array
			w.writeEndArray();

			// close
			w.close();
		}
		catch (Exception ex)
		{
			throw new ServletException("Problems reading file '"+f+"'.", ex);
		}
	}

	private void asCode(String inputName, String inputType)
	throws ServletException, IOException
	{
		out.println("<html> ");
		
		out.println("<style type='text/css'> ");
		out.println("body {                  ");
		out.println("	  height: 100%;      ");
//		out.println("	  overflow: hidden;  ");
		out.println("	}                    ");
        out.println("                        ");
//		out.println("	textarea {           ");
//		out.println("	  width: 100%;       ");
//		out.println("	  height: 100vw;     ");
//		out.println("	  overflow: hidden;  ");
//		out.println("	}                    ");
		out.println("</style> ");

		out.println("<body> ");

		out.println("<div id='log'>");
////		out.println("<textarea id='logtext' style='width:100%;height:100vw; overflow: hidden;'>");
//		out.println("<textarea id='logtext'>");
		out.println("<pre>");

		File f = new File(LOG_DIR+"/"+inputName);
		try ( FileInputStream in = new FileInputStream(f); 
		      BufferedReader  br = new BufferedReader(new InputStreamReader(in)); )
		{
//			_resp.setContentLengthLong(f.length() * 2);
			
			String line;
			while ((line = br.readLine()) != null)
			{
				out.println(line);
			}
		}
		catch (Exception ex)
		{
			throw new ServletException("Problems reading file '"+f+"'.", ex);
		}

		out.println("</pre>");
//		out.println("</textarea>");
		out.println("</div>");
		

		// FIXME: move codemirror to local resource
//		out.println("<link rel='stylesheet' href='https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.34.0/codemirror.css'> ");
//		out.println("<script src='https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.34.0/codemirror.js'></script> ");
//		out.println("<script> ");
////		out.println("  var editor = CodeMirror.fromTextArea('logtext', { "); // or document.getElementById("logtext")
//		out.println("  var logTextArea = document.getElementById('logtext');" );
//		out.println("  var editor = CodeMirror.fromTextArea(logTextArea, { ");
//		out.println("    mode: 'text/html', ");
//		out.println("    lineNumbers: true ");
//		out.println("  }); ");
//		out.println("</script> ");

		out.println("</body> ");
		out.println("</html> ");

		out.flush();
		out.close();
	}

	private void asHtml(String inputName, String inputType)
	throws ServletException, IOException
	{
		
		out.println("<html>");

//		out.println("<style type='text/css'>");
//		out.println("  table {border-collapse: collapse;}");
//		out.println("  th, td {border: 1px solid black; text-align: left; padding: 2px;}");
//		out.println("  tr:nth-child(even) {background-color: #f2f2f2;}");
//		out.println("</style>");
		
//		out.println("<script>");
//		out.println("function filterFunction()                                     ");
//		out.println("{                                                             ");
//		out.println("  var input, filter, table, tr, td, i;                        ");
//		out.println("  input  = document.getElementById('filterInput');            ");
//		out.println("  filter = input.value.toUpperCase();                         ");
//		out.println("  table  = document.getElementById('alarmTable');             ");
//		out.println("  tr     = table.getElementsByTagName('tr');                  ");
//		out.println("  for (r = 0; r < tr.length; r++)                             ");
//		out.println("  {                                                           ");
//		out.println("    tdArr = tr[r].getElementsByTagName('td');                 ");
//		out.println("    show = false;                                             ");
//		out.println("    for (c = 0; c < tdArr.length; c++)                        ");
//		out.println("    {                                                         ");
//		out.println("      td = tr[r].getElementsByTagName('td')[c];               ");
//		out.println("      if (td)                                                 ");
//		out.println("      {                                                       ");
//		out.println("        if (td.innerHTML.toUpperCase().indexOf(filter) > -1)  ");
//		out.println("        {                                                     ");
//		out.println("          show = true;                                        ");
//		out.println("          break;                                              ");
//		out.println("        }                                                     ");
//		out.println("      }                                                       ");
//		out.println("    }                                                         ");
//		out.println("    tr[r].style.display = show ? '' : 'none';                 ");
//		out.println("  }                                                           ");
//		out.println("}                                                             ");
//		out.println("</script>");

//		out.println("<script type='text/javascript' src='/scripts/tablefilter/tablefilter.js'></script>");
		
		out.println("<script type='text/javascript' src='/scripts/jquery/jquery-3.2.1.js'></script>");
		
		out.println("<!-- Tablesorter theme, note in the init section use: $('.tablesorter').tablesorter({ theme: 'metro-dark' }) --> ");
		out.println("<link rel='stylesheet' href='/scripts/tablesorter/css/theme.metro-dark.min.css'> ");
		out.println("<link rel='stylesheet' href='/scripts/css/dbxcentral_tablesorter.css'> ");
		
		out.println("<!-- Tablesorter script: required --> ");
		out.println("<script type='text/javascript' src='/scripts/tablesorter/js/jquery.tablesorter.js'></script> ");
		out.println("<script type='text/javascript' src='/scripts/tablesorter/js/jquery.tablesorter.widgets.js'></script> ");
//		out.println("<script type='text/javascript' src='/scripts/tablesorter/js/widgets/widget-scroller.js'></script> ");

		
		out.println("<body>");
		out.println("<h1>" + inputName + "</h1>");
		out.println("<p><b>Note:</b> Only <code>CANCEL</code> records are in the table.<br>Look for in <i>Duration</i> column, to get the time this Alarm was <i>active</i></p>");
		out.println("<p>Error records are marked with red.</p>");
		out.println();
//		out.println("<input type='text' id='filterInput' width='800' onkeyup='filterFunction()' placeholder='Filter...' title='Filter'>");
//		out.println("<br>");
//		out.println("<br>");
		out.println();
		out.println("<table id='alarmTable' class='alarmTableClass tablesorter'>");
		out.println("<thead> ");
		out.println("  <tr> ");
		out.println("    <th>Time</th>");
//		out.println("    <th data-sorter='shortDate' data-date-format='yyyymmdd'>Time</th>");
		out.println("    <th>Type</th>");
		out.println("    <th>AlarmClass</th>");
		out.println("    <th>SrvName</th>");
		out.println("    <th>CmName</th>");
		out.println("    <th>ExtraInfo</th>");
		out.println("    <th>Category</th>");
		out.println("    <th>Severity</th>");
		out.println("    <th>State</th>");
		out.println("    <th>Duration</th>");
		out.println("    <th>Text</th>");
		out.println("  </tr>");
		out.println("</thead> ");

		out.println("<tbody>");
		File f = new File(LOG_DIR+"/"+inputName);
		try
		{
			FileInputStream in = new FileInputStream(f);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));

			String line;
			while ((line = br.readLine()) != null)
			{
				String[] words = line.split(" - ");
				if (words.length < 10)
					continue;
				
//				String timeStr   = words[0].trim();
//				String type      = words[1].trim();
//				String className = words[2].trim();
//				String srvName   = words[3].trim();
//				String cmName    = words[4].trim();
//				String extraInfo = words[5].trim();
//				String category  = words[6].trim();
//				String severity  = words[7].trim();
//				String state     = words[8].trim();
//				String duration  = words[9].trim();
//				String msgText   = words[10].trim();

				String timeStr   = "";
				String type      = "";
				String className = "";
				String srvName   = "";
				String cmName    = "";
				String extraInfo = "";
				String category  = "";
				String severity  = "";
				String state     = "";
				String duration  = "";
				String msgText   = "";

				if (words.length == 10)
				{
    				timeStr   = words[0].trim();
    				type      = words[1].trim();
    				className = words[2].trim();
    				srvName   = words[3].trim();
    				cmName    = words[4].trim();
    				extraInfo = words[5].trim();
    				category  = "";
    				severity  = words[6].trim();
    				state     = words[7].trim();
    				duration  = words[8].trim();
    				msgText   = words[9].trim();
				}
				else
				{
    				timeStr   = words[0].trim();
    				type      = words[1].trim();
    				className = words[2].trim();
    				srvName   = words[3].trim();
    				cmName    = words[4].trim();
    				extraInfo = words[5].trim();
    				category  = words[6].trim();
    				severity  = words[7].trim();
    				state     = words[8].trim();
    				duration  = words[9].trim();
    				msgText   = words[10].trim();
				}
				

				if (StringUtil.hasValue(inputType))
				{
					if ( ! inputType.equals(type) )
						continue;
				}
					
				if ("ERROR".equals(severity))
//					out.println("<tr style='background-color:rgb(255,0,0);'>");
					out.println("<tr class='dbxErrorRow'>");
				else
					out.println("<tr>");
				
				// Translate the logTimestamp into iso8601 format
				Timestamp ts = TimeUtils.parseToTimestamp(timeStr);
				timeStr = TimeUtils.toStringIso8601(ts);

				out.println("  <td nowrap>" + timeStr   + "</td>");
				out.println("  <td nowrap>" + type      + "</td>");
				out.println("  <td nowrap>" + className + "</td>");
				out.println("  <td nowrap>" + srvName   + "</td>");
				out.println("  <td nowrap>" + cmName    + "</td>");
				out.println("  <td nowrap>" + extraInfo + "</td>");
				out.println("  <td nowrap>" + category  + "</td>");
				out.println("  <td nowrap>" + severity  + "</td>");
				out.println("  <td nowrap>" + state     + "</td>");
				out.println("  <td nowrap>" + duration  + "</td>");
				out.println("  <td nowrap>" + msgText   + "</td>");
				
				out.println("</tr>");
			}
			in.close();
		}
		catch (Exception ex)
		{
			throw new ServletException("Problems reading file '"+f+"'.", ex);
		}

//		out.println("<script>                                                                  ");
//		out.println("var tf = new TableFilter(document.querySelector('.alarmTableClass'), {    ");
//		out.println("    base_path: '/scripts/tablefilter/'                                    ");
//		out.println("});                                                                       ");
//		out.println("tf.init();                                                                ");
//		out.println("</script>                                                                 ");
		
		out.println("<script>                                                                  ");
		out.println("$(function() {                                                            ");
		out.println("    // call the tablesorter plugin                                        ");
		out.println("    $('#alarmTable').tablesorter({                                        ");
		out.println("         theme: 'metro-dark'                                              ");
//		out.println("        ,dateFormat : 'yyyymmdd'                                          ");
		out.println("        ,sortList: [[0,1]]                                                ");
		out.println("        ,showProcessing : true                                            ");
		out.println("        ,sortReset      : true                                            ");
		out.println("        ,widthFixed     : true                                            ");
		out.println("        ,headerTemplate : '{content} {icon}'                              ");
		out.println("        ,widgets        : [ 'uitheme', 'zebra', 'filter', 'resizable', 'stickyHeaders' ] ");
		out.println("        ,widgetOptions  : {                                               ");
		out.println("        	 storage_useSessionStorage : true                              ");
		out.println("        	,resizable_addLastColumn   : true                              ");
		out.println("        	,filter_placeholder        : { search : 'Search...' }          ");
		out.println("        	,filter_columnFilters      : true                              ");
		out.println("        }                                                                 ");
		out.println("        ,headers  : {                                                     ");
		out.println("           0: { dateFormat: 'yyyymmdd' }                                  ");
		out.println("        }                                                                 ");
		out.println("        ,debug          : false                                           ");
//		out.println("        ,debug          : true                                            ");
		out.println("    });                                                                   ");
		out.println("});                                                                       ");
		out.println("</script>                                                                 ");
		
		out.println("</tbody>");
		out.println("</table>");
		out.println("</body></html>");
		
		
		out.flush();
		out.close();
	}
	
	
	private void fromPcsAsHtmlTable(String inputName, String inputType)
	throws ServletException, IOException
	{
		out.println("<html>");

		out.println("<script type='text/javascript' src='/scripts/jquery/jquery-3.2.1.js'></script>");
		
		out.println("<!-- Tablesorter theme, note in the init section use: $('.tablesorter').tablesorter({ theme: 'metro-dark' }) --> ");
		out.println("<link rel='stylesheet' href='/scripts/tablesorter/css/theme.metro-dark.min.css'> ");
		out.println("<link rel='stylesheet' href='/scripts/css/dbxcentral_tablesorter.css'> ");
		
		out.println("<!-- Tablesorter script: required --> ");
		out.println("<script type='text/javascript' src='/scripts/tablesorter/js/jquery.tablesorter.js'></script> ");
		out.println("<script type='text/javascript' src='/scripts/tablesorter/js/jquery.tablesorter.widgets.js'></script> ");
//		out.println("<script type='text/javascript' src='/scripts/tablesorter/js/widgets/widget-scroller.js'></script> ");

		
		out.println("<body>");
		out.println("<h1>" + inputName + "</h1>");
//		out.println("<p><b>Note:</b> Only <code>CANCEL</code> records are in the table.<br>Look for in <i>Duration</i> column, to get the time this Alarm was <i>active</i></p>");
//		out.println("<p>Error records are marked with red.</p>");
		out.println();
		out.println();
		out.println("<table id='alarmTable' class='alarmTableClass tablesorter'>");
		out.println("<thead> ");
		out.println("  <tr> ");
		out.println("    <th>SessionStartTime</th>");
		out.println("    <th>SessionSampleTime</th>");
		out.println("    <th>EventTime</th>");
		out.println("    <th>SrvName</th>");
		out.println("    <th>Action</th>");
		out.println("    <th>AlarmClass</th>");
		out.println("    <th>ServiceType</th>");
		out.println("    <th>ServiceName</th>");
		out.println("    <th>ServiceInfo</th>");
		out.println("    <th>ExtraInfo</th>");
		out.println("    <th>Category</th>");
		out.println("    <th>Severity</th>");
		out.println("    <th>State</th>");
		out.println("    <th>RepeatCnt</th>");
		out.println("    <th>Duration</th>");
		out.println("    <th>CreateTime</th>");
		out.println("    <th>CancelTime</th>");
		out.println("    <th>TimeToLive</th>");
		out.println("    <th>Threshold</th>");
		out.println("    <th>Data</th>");
		out.println("    <th>LastData</th>");
		out.println("    <th>Description</th>");
		out.println("    <th>LastDescription</th>");
		out.println("    <th>ExtendedDescription</th>");
		out.println("    <th>LastExtendedDescription</th>");
		out.println("  </tr>");
		out.println("</thead> ");

		out.println("<tbody>");
		
		try
		{
			List<DbxAlarmHistory> list = CentralPersistReader.getInstance().getAlarmHistory(inputName, null, null, null);
			
			for (DbxAlarmHistory e : list)
			{
				if ("ERROR".equals(e.getSeverity()))
//					out.println("<tr style='background-color:rgb(255,0,0);'>");
					out.println("<tr class='dbxErrorRow'>");
				else
					out.println("<tr>");

//				@JsonPropertyOrder(value = {"sessionStartTime", "sessionSampleTime", "eventTime", "action", "alarmClass", 
//				"serviceType", "serviceName", "serviceInfo", "extraInfo", "category", "severity", "state", "repeatCnt", 
//				"duration", "createTime", "cancelTime", "timeToLive", "threshold", "data", "lastData", 
//				"description", "lastDescription", "extendedDescription", "lastExtendedDescription"}, alphabetic = true)
				
				out.println("  <td nowrap>" + e.getSessionStartTime()        + "</td>");
				out.println("  <td nowrap>" + e.getSessionSampleTime()       + "</td>");
				out.println("  <td nowrap>" + e.getEventTime()               + "</td>");
				out.println("  <td nowrap>" + e.getSrvName()                 + "</td>");
				out.println("  <td nowrap>" + e.getAction()                  + "</td>");
				out.println("  <td nowrap>" + e.getAlarmClass()              + "</td>");
				out.println("  <td nowrap>" + e.getServiceType()             + "</td>");
				out.println("  <td nowrap>" + e.getServiceName()             + "</td>");
				out.println("  <td nowrap>" + e.getServiceInfo()             + "</td>");
				out.println("  <td nowrap>" + e.getExtraInfo()               + "</td>");
				out.println("  <td nowrap>" + e.getCategory()                + "</td>");
				out.println("  <td nowrap>" + e.getSeverity()                + "</td>");
				out.println("  <td nowrap>" + e.getState()                   + "</td>");
				out.println("  <td nowrap>" + e.getRepeatCnt()               + "</td>");
				out.println("  <td nowrap>" + e.getDuration()                + "</td>");
				out.println("  <td nowrap>" + e.getCreateTime()              + "</td>");
				out.println("  <td nowrap>" + e.getCancelTime()              + "</td>");
				out.println("  <td nowrap>" + e.getTimeToLive()              + "</td>");
				out.println("  <td nowrap>" + e.getThreshold()               + "</td>");
				out.println("  <td nowrap>" + e.getData()                    + "</td>");
				out.println("  <td nowrap>" + e.getLastData()                + "</td>");
				out.println("  <td nowrap>" + e.getDescription()             + "</td>");
				out.println("  <td nowrap>" + e.getLastDescription()         + "</td>");
				out.println("  <td nowrap>" + e.getExtendedDescription()     + "</td>");
				out.println("  <td nowrap>" + e.getLastExtendedDescription() + "</td>");
				
				out.println("</tr>");
			}
		}
		catch (Exception ex)
		{
			throw new ServletException("Problems reading PCS.", ex);
		}

//		out.println("<script>                                                                  ");
//		out.println("var tf = new TableFilter(document.querySelector('.alarmTableClass'), {    ");
//		out.println("    base_path: '/scripts/tablefilter/'                                    ");
//		out.println("});                                                                       ");
//		out.println("tf.init();                                                                ");
//		out.println("</script>                                                                 ");
		
		out.println("<script>                                                                  ");
		out.println("$(function() {                                                            ");
		out.println("    // call the tablesorter plugin                                        ");
		out.println("    $('#alarmTable').tablesorter({                                        ");
		out.println("         theme: 'metro-dark'                                              ");
//		out.println("        ,dateFormat : 'yyyymmdd'                                          ");
		out.println("        ,sortList: [[0,1]]                                                ");
		out.println("        ,showProcessing : true                                            ");
		out.println("        ,sortReset      : true                                            ");
		out.println("        ,widthFixed     : true                                            ");
		out.println("        ,headerTemplate : '{content} {icon}'                              ");
		out.println("        ,widgets        : [ 'uitheme', 'zebra', 'filter', 'resizable', 'stickyHeaders' ] ");
		out.println("        ,widgetOptions  : {                                               ");
		out.println("        	 storage_useSessionStorage : true                              ");
		out.println("        	,resizable_addLastColumn   : true                              ");
		out.println("        	,filter_placeholder        : { search : 'Search...' }          ");
		out.println("        	,filter_columnFilters      : true                              ");
		out.println("        }                                                                 ");
		out.println("        ,headers  : {                                                     ");
		out.println("           0: { dateFormat: 'yyyymmdd' }                                  ");
		out.println("        }                                                                 ");
		out.println("        ,debug          : false                                           ");
//		out.println("        ,debug          : true                                            ");
		out.println("    });                                                                   ");
		out.println("});                                                                       ");
		out.println("</script>                                                                 ");
		
		out.println("</tbody>");
		out.println("</table>");
		out.println("</body></html>");
		
		
		out.flush();
		out.close();
	}
}
