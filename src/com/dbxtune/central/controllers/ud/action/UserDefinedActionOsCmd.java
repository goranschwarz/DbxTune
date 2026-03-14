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
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.dbxtune.central.controllers.ud.action;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.invoke.MethodHandles;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.PlatformUtils;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.TimeUtils;

public class UserDefinedActionOsCmd
extends UserDefinedActionAbstract
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public enum OutputType
	{
		STDOUT,
		STDERR
	}

//	public static final String PROPKEY_startTime = "startTime.default";
//	public static final String DEFAULT_startTime = "-2";
//	
//	private String    _defaultStartTime = DEFAULT_startTime;
//	private Timestamp _startTime;
//	private Timestamp _endTime;

	public UserDefinedActionOsCmd(Configuration conf)
	throws Exception
	{
		super(conf);
	}

//	@Override
//	public List<String> getJavaScriptList()
//	{
//		List<String> list = new ArrayList<>();
//		
//		list.add("/scripts/bootstrap-table/1.12.1/bootstrap-table.js");
//		list.add("/scripts/bootstrap-table/1.12.1/extensions/filter-control/bootstrap-table-filter-control.js");
//
//		return list;
//	}

//	@Override
//	public List<String> getCssList()
//	{
//		List<String> list = new ArrayList<>();
//		
//		list.add("/scripts/bootstrap-table/1.12.1/bootstrap-table.min.css");
//
//		return list;
//	}
	
	@Override
	public String[] getKnownParameters()
	{
		return new String[] {};
	}

	@Override
	public Map<String, String> getParameterDescription()
	{
		LinkedHashMap<String, String> map = new LinkedHashMap<>();
		
		map.put("refresh",              "Auto refresh the page after this amount of seconds.<br>"
		                                    + "<br>"
		                                    + "Example: <code>60</code> (every 60 seconds) <br>"
		                                    + "Example: <code>0 </code> (turn OFF auto refresh, just press F5 instead) <br>"
		                                    + "<b>Default</b>: <code>" +  getPageRefreshTime() + "</code>");

		return map;
	}

	@Override
	public void checkUrlParameters(Map<String, String> parameterMap) throws Exception
	{
	}


	@Override
	public void init(Configuration conf)
	throws Exception
	{
		super.init(conf);
		
		// Do local initializations here
//		_defaultStartTime = conf.getProperty(PROPKEY_startTime, DEFAULT_startTime);

	}

	@Override
	public String getOnServerName()
	{
//		return StringUtil.getHostname();
		return StringUtil.getHostnameWithDomain();
	}

	@Override
	public void createContent(PrintWriter pageOut, PrintWriter mailOut)
	throws Exception
	{
		createStyle(pageOut, mailOut);
		createJsFunctions(pageOut);

		doWork(pageOut, mailOut);
	}


	private void createStyle(PrintWriter pageOut, PrintWriter mailOut)
	throws IOException
	{
		println(pageOut, mailOut, "<style type='text/css'>");

//		println(pageOut, mailOut, ".cmd-command      { background-color: #f8f9fa; padding: 15px; margin: 15px 0; border-left: 4px solid #007bff; font-family: 'Courier New', monospace; font-size: 0.9rem; white-space: pre-wrap; overflow-x: auto; }");
		println(pageOut, mailOut, ".cmd-command      { background-color: #f8f9fa; padding: 15px; margin: 15px 0; border-left: 4px solid #007bff; font-family: 'Courier New', monospace; font-size: 9pt; white-space: pre; overflow-x: auto; }");
//		println(pageOut, mailOut, ".cmd-output       { background-color: #1e1e1e; color: #d4d4d4; padding: 20px; font-family: 'Courier New', monospace; font-size: 0.9rem; border-radius: 4px; max-height: 600px; overflow-y: auto; line-height: 1.2; }");
		println(pageOut, mailOut, ".cmd-output       { background-color: #1e1e1e; color: #d4d4d4; padding: 20px; font-family: 'Courier New', monospace; font-size: 9pt; border-radius: 4px; overflow-x: auto; line-height: 1.2; }");
//		println(pageOut, mailOut, ".cmd-stdout       { color: #d4d4d4; margin: 0; white-space: pre-wrap; word-wrap: break-word; }");
//		println(pageOut, mailOut, ".cmd-stderr       { color: #f48771; margin: 0; white-space: pre-wrap; word-wrap: break-word; }");
		println(pageOut, mailOut, ".cmd-stdout       { color: #d4d4d4; margin: 0; white-space: pre; overflow-wrap: normal; }");
		println(pageOut, mailOut, ".cmd-stderr       { color: #f48771; margin: 0; white-space: pre; overflow-wrap: normal; }");
		println(pageOut, mailOut, ".cmd-info         { background-color: #d1ecf1; color: #0c5460; padding: 10px; margin: 15px 0; border-left: 4px solid #17a2b8; border-radius: 4px; }");
		println(pageOut, mailOut, ".cmd-exit-success { background-color: #d4edda; color: #155724; padding: 10px; margin: 15px 0; border-left: 4px solid #28a745; border-radius: 4px; font-weight: bold; }");
		println(pageOut, mailOut, ".cmd-exit-error   { background-color: #f8d7da; color: #721c24; padding: 10px; margin: 15px 0; border-left: 4px solid #dc3545; border-radius: 4px; font-weight: bold; }");

		println(pageOut, mailOut, "</style>");

		// For Mail: we might need to change the CSS --- ***Oooo my good*** I hate Outlook Classic that uses Word as it's HTML Renderer
		if (mailOut != null)
		{
			// For Mail: Outlook Classic, we might want to change the CSS a bit (since MSO is rendering using Word)
			mailOut.println("<!--[if mso]>");
			mailOut.println("<style type='text/css'>");

			mailOut.println(".cmd-command { " +
					"font-size: 9pt !important; " +
					"mso-para-margin: 0 !important; " +
					"}");

			mailOut.println(".cmd-output { " +
					"background-color: #1e1e1e !important; " +
					"color: #d4d4d4 !important; " +
					"font-family: 'Courier New', Courier, monospace !important; " +
					"font-size: 9pt !important; " +
					"padding: 15pt !important; }");

			mailOut.println("</style>");
			mailOut.println("<![endif]-->");
		}
	}

	private void createJsFunctions(PrintWriter pageOut)
	throws IOException
	{
		// JavaScript functions
		pageOut.println();
		pageOut.println("<!-- ########################################################################## -->");
		pageOut.println("<!-- Local JavaScript functions -->");
		pageOut.println("<!-- ########################################################################## -->");
		pageOut.println("<script>");

		pageOut.println("//------------------------------------------------------------------");
		pageOut.println("function addCmdOutput(message, type)");
		pageOut.println("{");
		pageOut.println("    var divClass = 'cmd-unknown';");
		pageOut.println("    if      (type === '" + OutputType.STDOUT + "') divClass = 'cmd-stdout';");
		pageOut.println("    else if (type === '" + OutputType.STDERR + "') divClass = 'cmd-stderr';");
		pageOut.println();
		pageOut.println("    var div = document.createElement('div');");
		pageOut.println("    div.className = divClass;");
		pageOut.println("    div.textContent = message;");
		pageOut.println("    document.getElementById('cmd-output').appendChild(div);");
		pageOut.println();
		pageOut.println("    var outputDiv = document.getElementById('cmd-output');");
//		out.println("    outputDiv.scrollTop = outputDiv.scrollHeight;");
		pageOut.println("    div.scrollIntoView();"); // NOT the 'outputDiv', but the 'div' -- the message
		pageOut.println("}");
		pageOut.println();

		pageOut.println("//------------------------------------------------------------------");
		pageOut.println("function addInfoTop(message)");
		pageOut.println("{");
		pageOut.println("    var div = document.createElement('div');");
		pageOut.println("    div.className = 'cmd-info';");
		pageOut.println("    div.textContent = message;");
		pageOut.println("    document.getElementById('cmd-info-messages-top').appendChild(div);");
		pageOut.println("}");
		pageOut.println();

		pageOut.println("//------------------------------------------------------------------");
		pageOut.println("function addInfoBottom(message)");
		pageOut.println("{");
		pageOut.println("    var div = document.createElement('div');");
		pageOut.println("    div.className = 'cmd-info';");
		pageOut.println("    div.textContent = message;");
		pageOut.println("    document.getElementById('cmd-info-messages-bottom').appendChild(div);");
		pageOut.println("    div.scrollIntoView();");
		pageOut.println("}");
		pageOut.println();

		pageOut.println("//------------------------------------------------------------------");
		pageOut.println("function showExitCode(code)");
		pageOut.println("{");
		pageOut.println("    var div = document.createElement('div');");
		pageOut.println("    div.className = code === 0 ? 'cmd-exit-success' : 'cmd-exit-error';");
		pageOut.println("    div.textContent = 'Process exited with code: ' + code;");
		pageOut.println("    document.getElementById('cmd-info-messages-bottom').appendChild(div);");
		pageOut.println("    div.scrollIntoView();");
		pageOut.println("}");
		pageOut.println("</script>");
		pageOut.flush();
	}
	

	//----------------------------------------------------------------------
	// Below is Java code to call above JavaScript functions
	//----------------------------------------------------------------------
	private void sendCmdOutput(PrintWriter pageOut, PrintWriter mailOut, String message, OutputType type)
	{
		pageOut.println("<script>addCmdOutput('" + escapeJavaScript(message) + "', '" + type + "');</script>");
		pageOut.flush();
		
		if (mailOut != null)
		{
			String divClass = OutputType.STDOUT.equals(type) ? "cmd-stdout" : "cmd-stderr";
			
			// Modern clients
			mailOut.print("<!--[if !mso]><!-->");
			mailOut.print("<div class='" + divClass + "'>" + escapeHtml(message) + "</div>");
			mailOut.print("<!--<![endif]-->");

			// Outlook Classic
			mailOut.print("<!--[if mso]>");
			mailOut.print(escapeHtml(message));
			mailOut.print("<![endif]-->");

			mailOut.print("\n"); // NOTE: This is the ONLY new-line in this section
		}
	}
	
	private void sendInfoTop(PrintWriter pageOut, PrintWriter mailOut, String message)
	{
		pageOut.println("<script>addInfoTop('" + escapeJavaScript(message) + "');</script>");
		pageOut.flush();
		
		if (mailOut != null)
		{
			String divClass = "cmd-info";
			mailOut.println("<div class='" + divClass + "'>" + message + "</div>");
		}
	}
	
	private void sendInfoBottom(PrintWriter pageOut, PrintWriter mailOut, String message)
	{
		pageOut.println("<script>addInfoBottom('" + escapeJavaScript(message) + "');</script>");
		pageOut.flush();
		
		if (mailOut != null)
		{
			String divClass = "cmd-info";
			mailOut.println("<div class='" + divClass + "'>" + message + "</div>");
		}
	}
	
	private void sendExitCode(PrintWriter pageOut, PrintWriter mailOut, int exitCode)
	{
		pageOut.println("<script>showExitCode(" + exitCode + ");</script>");
		pageOut.flush();
		
		if (mailOut != null)
		{
			String divClass = exitCode == 0 ? "cmd-exit-success" : "cmd-exit-error";
			mailOut.println("<div class='" + divClass + "'>Process exited with code: " + exitCode + "</div>");
		}
	}

	private void sendScrollTop(PrintWriter pageOut, PrintWriter mailOut)
	{
		pageOut.println("<script>window.scrollTo({ top: 0, behavior: 'smooth' });</script>");
		pageOut.flush();
		
		if (mailOut != null)
		{
			// do nothing
		}
	}
	
	private void sendCmdOutputStart(PrintWriter pageOut, PrintWriter mailOut)
	{
		// PAGE OUT: do nothing

		if (mailOut != null)
		{
			// Modern clients (iOS, Gmail, etc.)
			mailOut.print("<!--[if !mso]><!-->");
			mailOut.print("<div class='cmd-output'>");
			mailOut.print("<!--<![endif]-->");

			// Outlook Classic
			mailOut.print("<!--[if mso]>");
			mailOut.print("<pre class='cmd-output'>");
			mailOut.print("<![endif]-->");
			
			mailOut.print("\n"); // NOTE: This is the ONLY new-line in this section
		}
	}

	private void sendCmdOutputEnd(PrintWriter pageOut, PrintWriter mailOut)
	{
		// PAGE OUT: do nothing

		if (mailOut != null)
		{
			// Modern clients
			mailOut.print("<!--[if !mso]><!-->");
			mailOut.print("</div>");
			mailOut.print("<!--<![endif]-->");

			// Outlook Classic
			mailOut.print("<!--[if mso]>");
			mailOut.print("</pre>");
			mailOut.print("<![endif]-->");

			mailOut.print("\n"); // NOTE: This is the ONLY new-line in this section
		}
	}

	
	//-----------------------------------------------------------------------------------------
	//-----------------------------------------------------------------------------------------
	//-----------------------------------------------------------------------------------------
	//-----------------------------------------------------------------------------------------

	/**
	 * Here are the MAIN part
	 * @param pageOut 
	 */
	private void doWork(PrintWriter pageOut, PrintWriter mailOut) 
	throws Exception
	{
		String command = getCommand();
		String[] pbCmd = new String[] {"/bin/bash", "-c", command};
		if (PlatformUtils.isWindows())
		{
			pbCmd = new String[] {"cmd.exe", "/c", command};
		}

		// This is the OUT 'divs' we will add output to.
		println(pageOut, mailOut, "<div>");
		println(pageOut, mailOut, "  <div>Command to execute:</div>");
		println(pageOut, null   , "  <div class='cmd-command'>" + escapeHtml(command) + "</div>");  // NOT WRITTEN TO MAIL
		if (mailOut != null)
		{
			// Modern clients
			mailOut.println("<!--[if !mso]><!-->");
			mailOut.println("<div class='cmd-command'>" + escapeHtml(command) + "</div>");
			mailOut.println("<!--<![endif]-->");

			// Outlook Classic
			mailOut.println("<!--[if mso]>");
			mailOut.println("<div class='cmd-command'><pre>" + escapeHtml(command) + "</pre></div>");
			mailOut.println("<![endif]-->");
		}

		println(pageOut, mailOut, "  <div id='cmd-info-messages-top'></div>");
		println(pageOut, null   , "  <div id='cmd-output' class='cmd-output'></div>"); // NOT WRITTEN TO MAIL
		println(pageOut, mailOut, "  <div id='cmd-info-messages-bottom'></div>");
		println(pageOut, mailOut, "</div>");
		pageOut.flush();

		
		long startTime = System.currentTimeMillis();
		
		// Execute the command
		ProcessBuilder pb = new ProcessBuilder(pbCmd);

		// Merge stderr into stdout
		pb.redirectErrorStream(true); 
		
		Process process = null;
		BufferedReader reader = null;
		
		try
		{
			sendInfoTop(pageOut, mailOut, "Starting command execution...");
//			Thread.yield(); // Give time for the message to reach the browser
			
			process = pb.start();
			
			reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

			sendCmdOutputStart(pageOut, mailOut);
			
			// Read merged output line by line
			String line;
			while ((line = reader.readLine()) != null)
			{
				sendCmdOutput(pageOut, mailOut, line, OutputType.STDOUT);
			}

			sendCmdOutputEnd(pageOut, mailOut);
			
			// Wait for process to complete
			int exitCode = process.waitFor();
			
			String execTime = TimeUtils.msToTimeStr("%?HH[:]%MM:%SS.%ms", System.currentTimeMillis() - startTime);
			sendInfoBottom(pageOut, mailOut, "Command execution completed. Execution Time " + execTime);
			sendExitCode(pageOut, mailOut, exitCode);
			sendScrollTop(pageOut, mailOut);
		}
		catch (InterruptedException e)
		{
			sendInfoBottom(pageOut, mailOut, "Command execution interrupted: " + e.getMessage());
			if (process != null)
			{
				process.destroy();
			}
		}
		catch (IOException e)
		{
			sendInfoBottom(pageOut, mailOut, "Error executing command: " + e.getMessage());
		}
		finally
		{
			try
			{
				if (reader != null) reader.close();
			}
			catch (IOException e)
			{
				sendInfoBottom(pageOut, mailOut, "Error closing stream: " + e.getMessage());
			}
		}
	}
}
