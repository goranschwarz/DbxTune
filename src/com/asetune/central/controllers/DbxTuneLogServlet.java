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
import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.text.StringEscapeUtils;
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

		// Check for known input parameters
		if (Helper.hasUnKnownParameters(req, resp, "name", "type", "method", "discard", "tail"))
			return;
		
		String inputName     = Helper.getParameter(req, "name");
		String inputType     = Helper.getParameter(req, "type");
		String inputMethod   = Helper.getParameter(req, "method",  "plain");
		String discardRegExp = Helper.getParameter(req, "discard");
		String tailParam     = Helper.getParameter(req, "tail");

		int numOfLines = -1; // All lines
		if (tailParam != null)
		{
			try {
				numOfLines = Integer.parseInt(tailParam);
			} catch (NumberFormatException e) {
				_logger.error("Parameter 'tail' is not a number. tailParam='"+tailParam+"'.");

				resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Parameter 'tail' is not a number. tailParam='"+tailParam+"'.");
				return;
			}
		}

//		System.out.println("DbxTuneLogServlet: name   = '"+inputName+"'.");
//		System.out.println("DbxTuneLogServlet: type   = '"+inputType+"'.");
//		System.out.println("DbxTuneLogServlet: method = '"+inputMethod+"'.");
//		System.out.println("DbxTuneLogServlet: tail   = '"+tailParam+"'.");


		// Check for mandatory parameters
		if (StringUtil.isNullOrBlank(inputName))
		{
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Could not find mandatory parameter 'name'.");
			return;
		}


		// CHECK: that the input file really INSIDE the LOG_DIR and not outside (for example /tmp/filename or /var/log/message)
		Path logDirPath   = Paths.get(LOG_DIR);
		Path inputPath    = Paths.get(LOG_DIR+"/"+inputName);
		Path relativePath = logDirPath.relativize(inputPath);
		if (relativePath.startsWith(".."))
		{
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Sorry the file '" + inputPath + "' must be located in the LOG dir '" + logDirPath + "'.");
			return;
		}
	
		// Check if file EXISTS
		File inputFile = new File(LOG_DIR+"/"+inputName);
		if ( ! inputFile.exists() )
		{
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Sorry the file '" + inputFile + "' doesn't exist.");
			return;
		}

		
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
			asCode(inputName, inputType, discardRegExp, numOfLines);
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
		JsonGenerator gen = jfactory.createGenerator(out);
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
			gen.writeStartArray();

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
				gen.writeStartObject();

				// Write fields
				gen.writeStringField("time"       , timeStr   );
				gen.writeStringField("severity"   , severity  );
				gen.writeStringField("threadName" , threadName );
				gen.writeStringField("className"  , className );
				gen.writeStringField("msgText"    , msgText   );

				// end object
				gen.writeEndObject();
			}

			// end array
			gen.writeEndArray();

			// close
			gen.close();
		}
		catch (Exception ex)
		{
			throw new ServletException("Problems reading file '"+f+"'.", ex);
		}
	}

	private void asCode(String inputName, String inputType, String discardRegExp, int tailNumOfLines)
	throws ServletException, IOException
	{
		File f = new File(LOG_DIR+"/"+inputName);

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

		out.println("");
		out.println("<script type='text/javascript'>");

		out.println("");
		out.println("function refreshWithFilter(msg) ");
		out.println("{ ");
		out.println("  // Get regExp from a field ");
		out.println("  var discardText    = document.getElementById('logLine-discardText').value; ");
		out.println("  var tailNumOfLines = document.getElementById('tail-numOfLines').value; ");
		out.println("  var currentUrl     = window.location.href; ");
		out.println("");
//		out.println("  window.location.href = currentUrl + '&discard=' + discardText;");
		out.println("  const url = new URL(currentUrl); ");
		out.println("  const params = new URLSearchParams(url.search); ");
		out.println("  params.set('discard', discardText); ");
		out.println("  params.set('tail',    tailNumOfLines); ");
		out.println("");
//		out.println("  window.history.replaceState({}, '', `${location.pathname}?${params}`); ");
		out.println("  window.location.href = `${location.pathname}?${params}`;");
		out.println("} ");

		// Write javascript code to
		// - connect to the websocket that sends new rows
		// - etc
		if (tailNumOfLines > 0)
		{
//			out.println("");
//			out.println("/** ");
//			out.println(" * Is element within visible region of a scrollable container ");
//			out.println(" * @param {HTMLElement} el - element to test ");
//			out.println(" * @returns {boolean} true if within visible region, otherwise false ");
//			out.println(" */ ");
//			out.println("function checkVisible(el) { ");
//			out.println("    var rect = el.getBoundingClientRect(); ");
//			out.println("    return (rect.top >= 0) && (rect.bottom <= window.innerHeight); ");
//			out.println("} ");
//			out.println("");

			out.println("");
			out.println("// WebSocket is used for log-tail");
			out.println("var logTailWebSocket; ");
			out.println("");
			out.println("// date updates every time we get a new log-line");
			out.println("var dbxLastLogLineTime = Date.now();");
			out.println("var dbxLastLogLineDiscardCount = 0;");
			out.println("");
			out.println("/** ");
			out.println(" * Update clock... on when last log-line event was received ");
			out.println(" */ ");
			out.println("function updateLogLineClock() ");
			out.println("{ ");
			out.println("	let ageInSec = Math.floor( (Date.now() - dbxLastLogLineTime) / 1000 ); ");
			out.println("	let div = document.getElementById('logLine-feedback-time'); ");
			out.println("");
			out.println("	div.style.color = ''; ");
			out.println("	div.innerHTML   = 'Last log line received ' + ageInSec + ' seconds ago. Discard count ' + dbxLastLogLineDiscardCount + '<br>'; ");
			out.println("");
			out.println("	if (ageInSec > 120) ");
			out.println("		div.style.color = 'red'; ");
			out.println("");
			out.println("	setTimeout(updateLogLineClock, 1000); ");
			out.println("} ");
			out.println("");

			out.println("");
			out.println("// YYYY-MM-DD hh:mm:ss ");
			out.println("function dateToIsoStr(now) ");
			out.println("{ ");
			out.println("  let year   = '' + now.getFullYear(); ");
			out.println("  let month  = '' + (now.getMonth() + 1); if (month.length  == 1) { month  = '0' + month;  } ");
			out.println("  let day    = '' + now.getDate();        if (day.length    == 1) { day    = '0' + day;    } ");
			out.println("  let hour   = '' + now.getHours();       if (hour.length   == 1) { hour   = '0' + hour;   } ");
			out.println("  let minute = '' + now.getMinutes();     if (minute.length == 1) { minute = '0' + minute; } ");
			out.println("  let second = '' + now.getSeconds();     if (second.length == 1) { second = '0' + second; } ");
			out.println("  return year + '-' + month + '-' + day + ' ' + hour + ':' + minute + ':' + second; ");
			out.println("} ");
			out.println("");

			out.println("");
			out.println("function appendLogLine(msg) ");
			out.println("{ ");
			out.println("  // Get regExp from a field ");
			out.println("  var regExp = document.getElementById('logLine-discardText').value; ");
			out.println("");
			out.println("  // does the regExp contain anything ??? ");
			out.println("  if ( /\\S/.test(regExp) ) ");
			out.println("  { ");
			out.println("    var patt = new RegExp(regExp); ");
			out.println("    if ( ! patt.test(msg) ) ");
			out.println("    { ");
//			out.println("      document.getElementById('log').append(msg); ");
//			out.println("      document.getElementById('log').appendChild(document.createTextNode(msg)); ");
			out.println("      document.getElementById('log').insertAdjacentHTML('beforeend', msg); ");
			out.println("    } ");
			out.println("    else ");
			out.println("    { ");
			out.println("      dbxLastLogLineDiscardCount++;");
			out.println("	   document.getElementById('logLine-feedback').innerHTML = '<b>Last Discarded row (from above regexp filter):</b> <br>' + msg; ");
			out.println("    } ");
			out.println("  } ");
			out.println("  else // always add record ");
			out.println("  { ");
//			out.println("    document.getElementById('log').append(msg); ");
//			out.println("    document.getElementById('log').appendChild(document.createTextNode(msg)); ");
			out.println("    document.getElementById('log').insertAdjacentHTML('beforeend', msg); ");
			out.println("  } ");
			out.println("} ");
			out.println("");

			out.println("");
			out.println("function checkVisible(elm) ");
			out.println("{ ");
			out.println("  var rect = elm.getBoundingClientRect(); ");
			out.println("  var viewHeight = Math.max(document.documentElement.clientHeight, window.innerHeight); ");
			out.println("  return !(rect.bottom < 0 || rect.top - viewHeight >= 0); ");
			out.println("} ");
			out.println("");

			out.println("");
			out.println("function logTailConnectWs() ");
			out.println("{ ");
			out.println("  logTailWebSocket = new WebSocket('ws://' + location.host + '/logtail?name="+inputName+"'); ");
			out.println("");
			out.println("  logTailWebSocket.onopen = function(event) ");
			out.println("  { ");
			out.println("    console.log('logTailWebSocket:onopen(): ', event);");
			out.println("	 document.getElementById('logLine-feedback').innerHTML = 'Connection to log-tail-server OPENED -- ' + dateToIsoStr(new Date()); ");
			out.println("    document.getElementById('log').insertAdjacentHTML('beforeend', '<b><font color=\"blue\"><<<<<<--------- log tail STARTED (' + dateToIsoStr(new Date()) + ') --------->>>>>> \\n</font></b>'); ");
			out.println("");
			out.println("    // scroll to the end of the page");
//			out.println("    setTimeout(function() { window.scrollBy(0, 100000000000000000); }, 0); ");         // On mac/Safari this scrolls to TOP instead of bottom
//			out.println("    setTimeout(function() { window.scrollTo(0, document.body.scrollHeight); }, 0); "); // not tested
			out.println("    var lastElement = document.getElementById('logLine-feedback'); ");
			out.println("    setTimeout(function() { lastElement.scrollIntoView(); }, 0); ");
			out.println("  } ");
			out.println("");
			out.println("  logTailWebSocket.onmessage = function(event) ");
			out.println("  { ");
			out.println("    setTimeout(function() { ");
			out.println("      dbxLastLogLineTime = Date.now(); ");
			out.println("      appendLogLine(event.data + '\\n'); ");
			out.println("      if (checkVisible(document.getElementById('logLine-feedback'))) { ");
//			out.println("        window.scrollBy(0, 100000000000000000); ");          // On mac/Safari this scrolls to TOP instead of bottom
//			out.println("        window.scrollTo(0, document.body.scrollHeight); ");  // not tested
			out.println("        var lastElement = document.getElementById('logLine-feedback'); ");
			out.println("        setTimeout(function() { lastElement.scrollIntoView(); }, 0); ");
			out.println("      }");
			out.println("    },0); ");
			out.println("  } ");
			out.println("");
			out.println("  logTailWebSocket.onclose = function(event) ");
			out.println("  { ");
			out.println("    console.log('logTailWebSocket:onclose(): ', event);");
			out.println("	 document.getElementById('logLine-feedback').innerHTML = 'Connection to log-tail-server CLOSED -- ' + dateToIsoStr(new Date()); ");
			out.println("    document.getElementById('log').insertAdjacentHTML('beforeend', '<b><font color=\"blue\"><<<<<<--------- log tail CLOSED (' + dateToIsoStr(new Date()) + ') --------->>>>>> \\n</font></b>'); ");
			out.println("    setTimeout(logTailConnectWs, 5*1000);");
			out.println("  } ");
			out.println("} ");
			out.println("");

			out.println("//----------------------------------------------------------------------");
			out.println("// When window is loaded...");
			out.println("//----------------------------------------------------------------------");
			out.println("window.onload = function() ");
			out.println("{ ");
			out.println("  if (!!window.WebSocket) ");
			out.println("  { ");
			out.println("    // Start a clock so we see when we received last log line");
			out.println("    updateLogLineClock();");
			out.println("");
			out.println("    // Start log tail, using a WebSocket");
			out.println("    logTailConnectWs(); ");
			out.println("  } ");
			out.println("  else ");
			out.println("  { ");
			out.println("    alert('The browser does not support WebSocket, log-tail will not be done.'); ");
			out.println("  } ");
			out.println("} ");
			out.println("");
		}
		else
		{
			out.println("//----------------------------------------------------------------------");
			out.println("// When window is loaded...");
			out.println("//----------------------------------------------------------------------");
			out.println("window.onload = function() ");
			out.println("{ ");
			out.println("    // scroll to the end of the page");
//			out.println("    setTimeout(function() { window.scrollBy(0, 100000000000000000); }, 0); ");         // On mac/Safari this scrolls to TOP instead of bottom
//			out.println("    setTimeout(function() { window.scrollTo(0, document.body.scrollHeight); }, 0); "); // not tested
			out.println("    var lastElement = document.getElementById('end-of-file'); ");
			out.println("    setTimeout(function() { lastElement.scrollIntoView(); }, 0); ");
			out.println("} ");
			
		}
		out.println("</script>");
		out.println("");

		out.println("</head> ");
		out.println("");

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
		
		out.println("<hr>");
//		out.println("<div id='log'>");
		out.println("<div>");
////		out.println("<textarea id='logtext' style='width:100%;height:100vw; overflow: hidden;'>");
//		out.println("<textarea id='logtext'>");
//		out.println("<pre>");
		out.println("<pre id='log'>");

		try ( FileInputStream in = new FileInputStream(f); 
		      BufferedReader  br = new BufferedReader(new InputStreamReader(in)); )
		{
//			_resp.setContentLengthLong(f.length() * 2);
			
			LinkedList<String> tailList = null;
			if (tailNumOfLines > 0)
				tailList = new LinkedList<>();

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

				// Set a color of a log line based on the severity
				line = htmlColorizeLogLine(line);

				if (tailList != null)
				{
					tailList.addLast(line);

					if (tailList.size() > tailNumOfLines)
						tailList.removeFirst();
				}
				else
				{
					out.println(line);
				}
			}
			
			if (tailList != null)
			{
				for (String msg : tailList)
				{
					out.println(msg);
				}
			}

		}
		catch (Exception ex)
		{
			throw new ServletException("Problems reading file '"+f+"'.", ex);
		}

		out.println("</pre>");
//		out.println("</textarea>");
		out.println("</div>");
		out.println("<hr>");

		out.println("<div id='end-of-file'>");
		out.println("---END-OF-FILE---: <code>" + f + "</code><br>");
		out.println("</div>");
		out.println("<br>");

		if (tailNumOfLines > 0)
		{
			// Print when the last LogLine was received
			out.println("<div id='logLine-feedback-time'>");
			out.println("</div>");
			out.println("<br>");
		
			// Add a field se we can discard records locally
			out.println("<div id='logLine-discard'>");
			out.println("<b>Filter out some text on new records (regexp can be used):</b> <br>");
			out.println("<input id='logLine-discardText' type='text' size='100' value='" + (StringUtil.hasValue(discardRegExp) ? discardRegExp : "") + "'/><br>");
			out.println("<br>");
			out.println("<b>Tail number of records:</b> <br>");
			out.println("<input id='tail-numOfLines' type='text' size='10' value='" + (tailNumOfLines > 0 ? tailNumOfLines : 5000) + "'/><br>");
			out.println("<br>");
			out.println("<button onclick='refreshWithFilter()'>Refresh page with above filter</button>");
			out.println("</div>");
			out.println("<br>");

			// Print when the last LogLine was received
			out.println("<div id='logLine-feedback' style='white-space: nowrap;'>");
			out.println("</div>");
			out.println("<br>");
		}
		else
		{
			out.println("<font color='red'>Tail is not enabled.</font><br>");
			out.println("<br>");
			// Add a field se we can discard records locally
			out.println("<div id='logLine-discard'>");
			out.println("<b>Filter out some text on new records (regexp can be used):</b> <br>");
			out.println("<input id='logLine-discardText' type='text' size='100' value='" + (StringUtil.hasValue(discardRegExp) ? discardRegExp : "") + "'/><br>");
			out.println("<br>");
			out.println("<b>Tail number of records:</b> <br>");
			out.println("<input id='tail-numOfLines' type='text' size='10' value='" + (tailNumOfLines > 0 ? tailNumOfLines : 5000) + "'/><br>");
			out.println("<br>");
			out.println("<button onclick='refreshWithFilter()'>Refresh page AND Start Tail - with above filter</button>");
			out.println("</div>");
			out.println("<br>");
		}
		out.println("");
		out.println("<script type='text/javascript'>");
		out.println("// refresh on enter ");
		out.println("document.getElementById('logLine-discardText').addEventListener('keyup', function(event) { event.preventDefault(); if (event.keyCode === 13) { refreshWithFilter(); } } ); ");
		out.println("document.getElementById('tail-numOfLines')    .addEventListener('keyup', function(event) { event.preventDefault(); if (event.keyCode === 13) { refreshWithFilter(); } } ); ");
		out.println("</script>");
		out.println("");

		
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

	public static String htmlColorizeLogLine(String line)
	{
		if (line == null)
			return null;
		
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

		// Escape HTML Chars
		line = StringEscapeUtils.escapeHtml4(line);
		
		return prefix + line + postfix;
	}

	private void asHtml(String inputName, String inputType, String discardRegExp)
	throws ServletException, IOException
	{
		
		out.println("<html>");
		
		out.println("<head> ");
		out.println("<title>"+inputName+"</title> ");

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

		out.println("</head> ");
		out.println(" ");
		
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
