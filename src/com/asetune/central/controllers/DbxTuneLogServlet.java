package com.asetune.central.controllers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
import java.sql.Timestamp;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.asetune.central.DbxTuneCentral;
import com.asetune.utils.StringUtil;
import com.asetune.utils.TimeUtils;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

public class DbxTuneLogServlet extends HttpServlet
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

		
		String inputName     = req.getParameter("name");
		String inputType     = req.getParameter("type");
		String inputMethod   = req.getParameter("method");
		String discardRegExp = req.getParameter("discard");

		if (StringUtil.isNullOrBlank(inputMethod))
			inputMethod = "plain";

		System.out.println("DbxTuneLogServlet: name   = '"+inputName+"'.");
		System.out.println("DbxTuneLogServlet: type   = '"+inputType+"'.");
		System.out.println("DbxTuneLogServlet: method = '"+inputMethod+"'.");

		if (StringUtil.isNullOrBlank(inputName))
			throw new ServletException("No input parameter named 'name'.");

		if  ("json".equalsIgnoreCase(inputMethod))
		{
			resp.setContentType("text/html");
//			resp.setContentType("application/json");
			resp.setCharacterEncoding("UTF-8");
			asJson(inputName, inputType, discardRegExp);
		}
		else if ("plain".equalsIgnoreCase(inputMethod))
		{
			resp.setContentType("text/html");
			resp.setCharacterEncoding("UTF-8");
			asCode(inputName, inputType, discardRegExp);
		}
		else
		{
			resp.setContentType("text/html");
			resp.setCharacterEncoding("UTF-8");
			asHtml(inputName, inputType, discardRegExp);
		}
	}

	private void asJson(String inputName, String inputType, String discardRegExp)
	throws ServletException, IOException
	{
		JsonFactory jfactory = new JsonFactory();
		JsonGenerator w = jfactory.createGenerator(out);
//		w.setPrettyPrinter(new DefaultPrettyPrinter());

		// Compile the regexp; if we got any
		Pattern pattern = null;
		if (StringUtil.hasValue(discardRegExp))
		{
    		try { pattern = Pattern.compile(discardRegExp); }
    		catch (PatternSyntaxException ex) { throw new ServletException("Error in 'discard' RegExp pattern. Caught: "+ex); }
		}
		
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
				// Discard rows by the regexp
				if (pattern != null)
				{
					if (pattern.matcher(line).find())
						continue;
				}
				
				String[] words = line.split(" - ");
				if (words.length < 5)
					continue;
				
				String timeStr    = words[0].trim();
				String severity   = words[1].trim();
				String threadName = words[2].trim();
				String className  = words[3].trim();
				String msgText    = words[4];
				if (words.length > 5)
				{
					for (int i = 5; i < words.length; i++)
					{
						msgText +=  words[i];
					}
				}

//				if (StringUtil.hasValue(inputType))
//				{
//					if ( ! inputType.equals(severity) )
//						continue;
//				}
					

				// Translate the logTimestamp into iso8601 format
				Timestamp ts = TimeUtils.parseToTimestamp(timeStr, "yyyy-MM-dd hh:mm:ss,SSS");
				timeStr = TimeUtils.toStringIso8601(ts);

				// Start object
				w.writeStartObject();

				// Write fields
				w.writeStringField("time"       , timeStr   );
				w.writeStringField("severity"   , severity  );
				w.writeStringField("threadName" , threadName );
				w.writeStringField("className"  , className );
				w.writeStringField("msgText"    , msgText   );

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

	private void asCode(String inputName, String inputType, String discardRegExp)
	throws ServletException, IOException
	{
		File f = new File(LOG_DIR+"/"+inputName);

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
		out.println("<h2>"+f.getAbsolutePath()+"</h2>");

		// Compile the regexp; if we got any
		Pattern pattern = null;
		if (StringUtil.hasValue(discardRegExp))
		{
    		try { pattern = Pattern.compile(discardRegExp); }
    		catch (PatternSyntaxException ex) 
    		{ 
    			out.println("<p>"); 
    			out.println("Error in 'discard' RegExp pattern. Caught: "+ex+"<br>"); 
    			out.println("So discarding will NOT be done<br>"); 
    			out.println("</p>"); 
    		}
		}
		
		
		out.println("<div id='log'>");
////		out.println("<textarea id='logtext' style='width:100%;height:100vw; overflow: hidden;'>");
//		out.println("<textarea id='logtext'>");
		out.println("<pre>");

		try ( FileInputStream in = new FileInputStream(f); 
		      BufferedReader  br = new BufferedReader(new InputStreamReader(in)); )
		{
//			_resp.setContentLengthLong(f.length() * 2);
			
			String line;
			while ((line = br.readLine()) != null)
			{
				// Discard rows by the regexp
				if (pattern != null)
				{
					if (pattern.matcher(line).find())
						continue;
				}
				
				String prefix  = "";
				String postfix = "";
				if      (line.indexOf(" - INFO  - " ) >= 0) { prefix = "<span style='background-color:rgb(240, 240, 240);'>"; postfix = "</span>"; }
				else if (line.indexOf(" - WARN  - " ) >= 0) { prefix = "<span style='background-color:rgb(255, 230, 153);'>"; postfix = "</span>"; }
				else if (line.indexOf(" - AFFECTED ") >= 0) { prefix = "<span style='background-color:rgb(255, 230, 153);'>"; postfix = "</span>"; }
				else if (line.indexOf(" - ERROR - " ) >= 0) { prefix = "<span style='background-color:rgb(255, 179, 179);'>"; postfix = "</span>"; }
				else if (line.indexOf(" - ERROR   " ) >= 0) { prefix = "<span style='background-color:rgb(255, 179, 179);'>"; postfix = "</span>"; }
				else if (line.indexOf(" - TRACE - " ) >= 0) { prefix = "<span style='background-color:rgb(240, 240, 240);'>"; postfix = "</span>"; }
				else if (line.indexOf(" - DEBUG - " ) >= 0) { prefix = "<span style='background-color:rgb(240, 240, 240);'>"; postfix = "</span>"; }
				else if (line.indexOf(" - FATAL - " ) >= 0) { prefix = "<span style='background-color:rgb(255,  51,  51);'>"; postfix = "</span>"; }

				out.print(prefix);
				out.println(line);
				out.print(postfix);
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

	private void asHtml(String inputName, String inputType, String discardRegExp)
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
		
		// Compile the regexp; if we got any
		Pattern pattern = null;
		if (StringUtil.hasValue(discardRegExp))
		{
    		try { pattern = Pattern.compile(discardRegExp); }
    		catch (PatternSyntaxException ex) 
    		{ 
    			out.println("<p>"); 
    			out.println("Error in 'discard' RegExp pattern. Caught: "+ex+"<br>"); 
    			out.println("So discarding will NOT be done<br>"); 
    			out.println("</p>"); 
    		}
		}
		
		
		out.println();
		out.println("<table id='alarmTable' class='alarmTableClass tablesorter'>");
		out.println("<thead> ");
		out.println("  <tr> ");
		out.println("    <th>Time</th>");
//		out.println("    <th data-sorter='shortDate' data-date-format='yyyymmdd'>Time</th>");
		out.println("    <th>Severity</th>");
		out.println("    <th>ThreadName</th>");
		out.println("    <th>AlarmClass</th>");
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
				// Discard rows by the regexp
				if (pattern != null)
				{
					if (pattern.matcher(line).find())
						continue;
				}
				
				String[] words = line.split(" - ");
				if (words.length < 5)
					continue;
				
				String timeStr    = words[0].trim();
				String severity   = words[1].trim();
				String threadName = words[2].trim();
				String className  = words[3].trim();
				String msgText    = words[4];
				if (words.length > 5)
				{
					for (int i = 5; i < words.length; i++)
					{
						msgText +=  words[i];
					}
				}

//				if (StringUtil.hasValue(inputType))
//				{
//					if ( ! inputType.equals(severity) )
//						continue;
//				}
					
				if ("ERROR".equals(severity))
//					out.println("<tr style='background-color:rgb(255,0,0);'>");
					out.println("<tr class='dbxErrorRow'>");
				else
					out.println("<tr>");
				
				// Translate the logTimestamp into iso8601 format
				Timestamp ts = TimeUtils.parseToTimestamp(timeStr, "yyyy-MM-dd hh:mm:ss,SSS");
				timeStr = TimeUtils.toStringIso8601(ts);

				out.println("  <td nowrap>" + timeStr    + "</td>");
				out.println("  <td nowrap>" + severity   + "</td>");
				out.println("  <td nowrap>" + threadName + "</td>");
				out.println("  <td nowrap>" + className  + "</td>");
				out.println("  <td nowrap>" + msgText    + "</td>");
				
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
}
