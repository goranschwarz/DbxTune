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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.nio.file.Paths;
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

public class DbxTuneConfServlet extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	private static final Logger _logger = Logger.getLogger(MethodHandles.lookup().lookupClass());

	private final String CONF_DIR  = DbxTuneCentral.getAppConfDir();

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

//		System.out.println("DbxTuneConfServlet: name   = '"+inputName+"'.");
//		System.out.println("DbxTuneConfServlet: type   = '"+inputType+"'.");
//		System.out.println("DbxTuneConfServlet: method = '"+inputMethod+"'.");

		// Check for mandatory parameters
		if (StringUtil.isNullOrBlank(inputName))
		{
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Could not find mandatory parameter 'name'.");
			return;
		}

		// CHECK: that the input file really INSIDE the LOG_DIR and not outside (for example /tmp/filename or /var/log/message)
		Path logDirPath   = Paths.get(CONF_DIR);
		Path inputPath    = Paths.get(CONF_DIR + "/" + inputName);
		Path relativePath = logDirPath.relativize(inputPath);
		if (relativePath.startsWith(".."))
		{
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Sorry the file '" + inputPath + "' must be located in the CONF dir '" + logDirPath + "'.");
			return;
		}
	
		// Check if file EXISTS
		File inputFile = new File(CONF_DIR + "/" + inputName);
		if ( ! inputFile.exists() )
		{
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Sorry the file '" + inputFile + "' doesn't exist.");
			return;
		}


		if ("plain".equalsIgnoreCase(inputMethod))
		{
			resp.setContentType("text/html");
			resp.setCharacterEncoding("UTF-8");
			asCode(inputName, inputType, discardRegExp);
		}
		else
		{
			resp.setContentType("text/html");
			resp.setCharacterEncoding("UTF-8");
			asHtmlTable(inputName, inputType, discardRegExp);
		}
	}

	private void asCode(String inputName, String inputType, String discardRegExp)
	throws ServletException, IOException
	{
		File f = new File(CONF_DIR+"/"+inputName);

		out.println("<html> ");
		
		out.println("<head> ");
		out.println("<title>"+inputName+"</title> ");

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
		out.println("<link href='/scripts/prism/prism.css' rel='stylesheet' />");
		out.println("</head> ");

		out.println("<body> ");
		out.println("<script src='/scripts/prism/prism.js'></script> ");

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
		out.println("<code class='language-properties line-numbers'>");

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
				
//				String prefix  = "";
//				String postfix = "";
//				if      (line.indexOf(" - INFO  - " ) >= 0) { prefix = "<span style='background-color:rgb(240, 240, 240);'>"; postfix = "</span>"; }
//				else if (line.indexOf(" - WARN  - " ) >= 0) { prefix = "<span style='background-color:rgb(255, 230, 153);'>"; postfix = "</span>"; }
//				else if (line.indexOf(" - AFFECTED ") >= 0) { prefix = "<span style='background-color:rgb(255, 230, 153);'>"; postfix = "</span>"; }
//				else if (line.indexOf(" - ERROR - " ) >= 0) { prefix = "<span style='background-color:rgb(255, 179, 179);'>"; postfix = "</span>"; }
//				else if (line.indexOf(" - ERROR   " ) >= 0) { prefix = "<span style='background-color:rgb(255, 179, 179);'>"; postfix = "</span>"; }
//				else if (line.indexOf(" - TRACE - " ) >= 0) { prefix = "<span style='background-color:rgb(240, 240, 240);'>"; postfix = "</span>"; }
//				else if (line.indexOf(" - DEBUG - " ) >= 0) { prefix = "<span style='background-color:rgb(240, 240, 240);'>"; postfix = "</span>"; }
//				else if (line.indexOf(" - FATAL - " ) >= 0) { prefix = "<span style='background-color:rgb(255,  51,  51);'>"; postfix = "</span>"; }
//
//				out.print(prefix);
//				out.println(line);
//				out.print(postfix);

				line = StringUtil.toHtmlStringExceptNl(line);
				out.println(line);
			}
		}
		catch (Exception ex)
		{
			throw new ServletException("Problems reading file '"+f+"'.", ex);
		}

		out.println("</code>");
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

	private void asHtmlTable(String inputName, String inputType, String discardRegExp)
	throws ServletException, IOException
	{
		
		out.println("<html>");

		out.println("<head> ");
		out.println("<title>"+inputName+"</title> ");

		out.println("<script type='text/javascript' src='/scripts/jquery/jquery-3.7.0.js'></script>");
		
		out.println("<!-- Tablesorter theme, note in the init section use: $('.tablesorter').tablesorter({ theme: 'metro-dark' }) --> ");
		out.println("<link rel='stylesheet' href='/scripts/tablesorter/css/theme.metro-dark.min.css'> ");
		out.println("<link rel='stylesheet' href='/scripts/css/dbxcentral_tablesorter.css'> ");
		
		out.println("<!-- Tablesorter script: required --> ");
		out.println("<script type='text/javascript' src='/scripts/tablesorter/js/jquery.tablesorter.js'></script> ");
		out.println("<script type='text/javascript' src='/scripts/tablesorter/js/jquery.tablesorter.widgets.js'></script> ");
//		out.println("<script type='text/javascript' src='/scripts/tablesorter/js/widgets/widget-scroller.js'></script> ");

		out.println("</head> ");
		
		out.println("<body>");
		out.println("<h1>" + inputName + "</h1>");
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
		out.println("<table id='dbxTable' class='alarmTableClass tablesorter'>");
		out.println("<thead> ");
		out.println("  <tr> ");
		out.println("    <th>Key</th>");
		out.println("    <th>Value</th>");
		out.println("  </tr>");
		out.println("</thead> ");

		out.println("<tbody>");
		File f = new File(CONF_DIR+"/"+inputName);
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
				
				String[] words = line.split("=");
				if (words.length < 2)
					continue;
				
				String key = words[0].trim();
				String val = words[1].trim();
				if (words.length > 2)
				{
					for (int i = 2; i < words.length; i++)
					{
						val +=  words[i];
					}
				}

				out.println("  <td nowrap>" + key + "</td>");
				out.println("  <td nowrap>" + val + "</td>");
				
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
		out.println("    $('#dbxTable').tablesorter({                                        ");
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
