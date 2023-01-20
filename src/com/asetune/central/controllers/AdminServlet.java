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

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.asetune.central.DbxTuneCentral;
import com.asetune.central.cleanup.DataDirectoryCleaner;
import com.asetune.central.pcs.CentralPcsWriterHandler;
import com.asetune.central.pcs.CentralPcsWriterHandler.NotificationType;
import com.asetune.central.pcs.CentralPersistReader;
import com.asetune.central.pcs.objects.DbxCentralServerDescription;
import com.asetune.central.pcs.objects.DbxCentralSessions;
import com.asetune.utils.Configuration;
import com.asetune.utils.PlatformUtils;
import com.asetune.utils.StringUtil;
import com.asetune.utils.TimeUtils;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AdminServlet extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	private static final Logger _logger = Logger.getLogger(MethodHandles.lookup().lookupClass());

	public static String getParameter(HttpServletRequest req, String name, String defaultVal)
	{
		String val = req.getParameter(name);
		if (val == null)
			val = defaultVal;
		return val;
	}

	private void printHelp(ServletOutputStream out, String msg) 
	throws IOException
	{
		out.println("<html>");
		out.println("<body>");
		if (msg != null)
		{
			out.println("<br>");
			out.println("<b>" + msg + "</b><br>");
		}

		out.println("<br>");
		out.println("Available commands: /admin?op=name<br>");
		
		out.println("<table border=1 cellpadding=1>");
		out.println("  <tr> <th> Operation Name            </th> <th> Description </th> </tr>");
		out.println("  <tr> <td> removeServer              </td> <td> Removed a DBMS Server from the system. </td> </tr>");
		out.println("  <tr> <td> disableServer             </td> <td> Disable a DBMS Server from the system. </td> </tr>");
		out.println("  <tr> <td> ddc|DataDirectoryCleaner  </td> <td> Execute a 'Data Directory Cleanup'.    </td> </tr>");
		out.println("</table>");
		out.println("");

		out.println("</body>");
		out.println("</html>");
		out.flush();
		
		out.close();
		return;
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		ServletOutputStream out = resp.getOutputStream();
		resp.setContentType("text/html");
		resp.setCharacterEncoding("UTF-8");
//		resp.setContentType("application/json");
//		resp.setCharacterEncoding("UTF-8");

		String inputOp   = req.getParameter("op");
		String inputName = req.getParameter("name");
		
		_logger.info("input: op = '"+inputOp+"'.");

		if (StringUtil.isNullOrBlank(inputOp))
			throw new ServletException("No input parameter named 'op'.");

		if ("help".equals(inputOp))
		{
			printHelp(out, null);
			return;
		}
		else if ("removeServer".equals(inputOp))
		{
			if (StringUtil.isNullOrBlank(inputName))
				throw new ServletException("No input parameter named 'name'.");

			boolean stopCollector        = getParameter(req, "stopCollector",        "true").trim().equalsIgnoreCase("true");
			boolean removeFromServerList = getParameter(req, "removeFromServerList", "true").trim().equalsIgnoreCase("true");
			boolean removeLogFiles       = getParameter(req, "removeLogFiles",       "true").trim().equalsIgnoreCase("true");
			boolean removeDsrFiles       = getParameter(req, "removeDsrFiles",       "true").trim().equalsIgnoreCase("true");
			boolean removeDbmsData       = getParameter(req, "removeDbmsData",       "true").trim().equalsIgnoreCase("true");
			boolean removeH2Files        = getParameter(req, "removeH2Files",        "true").trim().equalsIgnoreCase("true");
			
			// Remove the server name from the system
			ActionObject ao = removeManagedServer(inputName, stopCollector, removeFromServerList, removeLogFiles, removeDsrFiles, removeDbmsData, removeH2Files);

			ObjectMapper om = Helper.createObjectMapper();
			String payload = om.writeValueAsString(ao);
			
			out.println(payload);
			
			out.flush();
			out.close();
			return;
		}
		else if ("disableServer".equals(inputOp) || "enableServer".equals(inputOp))
		{
			if (StringUtil.isNullOrBlank(inputName))
				throw new ServletException("No input parameter named 'name'.");

			boolean disable = "disableServer".equals(inputOp);

			ActionObject ao = enableOrDisableManagedServer(inputName, disable);

			ObjectMapper om = Helper.createObjectMapper();
			String payload = om.writeValueAsString(ao);
			
			out.println(payload);
			
			out.flush();
			out.close();
			return;
		}
		else if ("addSrv".equals(inputOp))
		{
		}
		else if ("startSrv".equals(inputOp))
		{
		}
		else if ("stopSrv".equals(inputOp))
		{
		}
		else if ("getConfig".equals(inputOp))
		{
		}
		else if ("setConfig".equals(inputOp))
		{
		}
		else if ("DataDirectoryCleaner".equals(inputOp) || "ddc".equals(inputOp))
		{
			out.println("<html>");
			out.println("<body>");
			out.println(" - START: DataDirectoryCleaner<br>");
			out.flush();
			
			DataDirectoryCleaner t = new DataDirectoryCleaner();
			t.execute(null);

			out.println(" - END: DataDirectoryCleaner<br>");
			out.println("</body>");
			out.println("</html>");
			out.flush();
			
			out.close();
			return;
		}
		else if ("clearActiveAlarms".equals(inputOp))
		{
			if (StringUtil.isNullOrBlank(inputName))
				throw new ServletException("No input parameter named 'name'.");

			ActionObject ao = clearActiveAlarmsForServer(inputName);

			ObjectMapper om = Helper.createObjectMapper();
			String payload = om.writeValueAsString(ao);
			
			out.println(payload);
			
			out.flush();
			out.close();
			return;
		}
		else 
		{
			printHelp(out, "No operation named '"+inputOp+"'.");
			return;
			//throw new ServletException("No operation named '"+inputOp+"'.");
		}
		
		
		out.println("Not yet implemented...");
		out.flush();
		out.close();
	}


	/**
	 * Check if the server/service is started<br>
	 * If the collector/service is NOT Started:
	 * <ul>
	 *   <li>Remove  H2 storage files (if we can find any) </li>
	 *   <li>Removes Schema in the PCS DBMS </li>
	 *   <li>Removes MetaData for server in the PCS DBMS </li>
	 *   <li>Remove entry from SERVER_LIST file (if we can find any) </li>
	 * </ul>
	 * @param name
	 * 
	 * @return A ActionObject that can be converted to a JSON object with what we have done
	 */
	private ActionObject removeManagedServer(String name, boolean stopCollector, boolean removeFromServerList, boolean removeLogFiles, boolean removeDsrFiles, boolean removeDbmsData, boolean removeH2Files)
	{
		// Remove files
		// Remove PCS - Schema
		// Remove entry from ServerList

		ActionObject ao = new ActionObject();

//		boolean returnHere = true;
//		if (returnHere)
//		{
//			ao.add("Dummy", new ActionType(ActionStatus.SUCCESS, "Dummy '"+name+"'."));
//			return ao;
//		}
		
		// Check if collector/service is up and running...
		//FIXME: not yet done.
		// we can use Java9 ProcessHandle 
		// or https://github.com/profesorfalken/jProcesses
		
		// STOP/KILL: Collector
		boolean waitforStopCollector = false;
		if (stopCollector)
		{
			try
			{
				// Get the servers controller file (where we find the PID) in property "dbxtune.pid"
				// possibly checking if the server runs on the same host as DbxCentral
				// issue 'kill #pid#'
				// wait for it to stop...
				int pid = stopCollector(name);
				ao.add("Stopping DbxTune Collector", new ActionType(ActionStatus.SUCCESS, "Succeeded signaling a stopping request (kill " + pid + ") for server '"+name+"'."));
				
				waitforStopCollector = true;
			}
			catch (Exception e) 
			{
				ao.add("Stopping DbxTune Collector", new ActionType(ActionStatus.FAIL, "Problems stopping server '"+name+"'. Caught: "+e));
			}
		}

		// REMOVE: DBMS content/data
		if (removeDbmsData)
		{
			CentralPersistReader reader = CentralPersistReader.getInstance();

			// Schema
			try
			{
				reader.removeServerSchema(name);
				ao.add("Remove DBMS Data", new ActionType(ActionStatus.SUCCESS, "Removed the schema '"+name+"'."));

				// Writer may have cached information
				if (CentralPcsWriterHandler.hasInstance())
				{
					CentralPcsWriterHandler.getInstance().fireNotification(NotificationType.DROP_SERVER, name);
				}
			} 
			catch (SQLException e) 
			{
				ao.add("Remove DBMS Data", new ActionType(ActionStatus.FAIL, "Problems removing the schema '"+name+"'. Caught: "+e));
			}

			// MetaData
			try 
			{ 
				int rowCount = reader.removeServerMetaData(name);
				ao.add("Remove Meta Data", new ActionType(ActionStatus.SUCCESS, "Removed the MetaData for '"+name+"'. rowCount="+rowCount));
			} 
			catch (SQLException e) 
			{
				ao.add("Remove Meta Data", new ActionType(ActionStatus.FAIL, "Problems removing the MetaData for '"+name+"'. Caught: "+e));
			}			
		}

		// REMOVE: H2 DBMS DB Files
		if (removeH2Files)
		{
			String directory = DbxTuneCentral.getAppDataDir();
			List<String> removedList = new ArrayList<>();

			try 
			{
				try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(directory)))
				{
					for (Path path : directoryStream)
					{
						File f = path.toFile();
						String filename = f.getName();
						if (f.isFile() && filename.startsWith(name) && (filename.endsWith(".mv.db") || filename.endsWith(".trace.db")))
						{
							String fn = "";
							if (filename.endsWith(".mv.db")   ) fn = filename.replace(".mv.db", "");    // remove extention
							if (filename.endsWith(".trace.db")) fn = filename.replace(".trace.db", ""); // remove extention
							fn = fn.replace(name, ""); // remove servername

							// is it a TimeStamped file
							if (fn.matches("_[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]"))
							{
								f.delete();
								removedList.add(f.getName());
							}
						}
					}
				}

				ao.add("Remove H2 DB Files", new ActionType(ActionStatus.SUCCESS, "Removed "+removedList.size()+" DB Files for '"+name+"'. Here is the list "+removedList));
			}
			catch (Exception e) 
			{
				ao.add("Remove H2 DB Files", new ActionType(ActionStatus.FAIL, "Problems removing the H2 DB Files for '"+name+"'. Caught: "+e));
			}
		}

		// REMOVE: Cache entries in the Writer
		
		
		// REMOVE: ServerList
		if (removeFromServerList)
		{
			String filename = DbxCentralServerDescription.getDefaultFile();
			try 
			{
				boolean foundRow = DbxCentralServerDescription.removeFromFile(filename, name, true); // 3 param: only comment out the file

				if (foundRow)
					ao.add("Remove From Server List", new ActionType(ActionStatus.SUCCESS, "Removed server in ServerList '"+filename+"' for '"+name+"'."));
				else
					ao.add("Remove From Server List", new ActionType(ActionStatus.FAIL, "Server name '"+name+"' was NOT found in ServerList '"+filename+"'."));
			}
			catch (Exception e) 
			{
				ao.add("Remove From Server List", new ActionType(ActionStatus.FAIL, "Problems removing ServerList '"+filename+"' for '"+name+"'. Caught: "+e));
			}
		}

		// REMOVE: LogFiles
		if (removeLogFiles)
		{
			String directory = DbxTuneCentral.getAppLogDir();
			List<String> removedList = new ArrayList<>();
			
			try 
			{
				try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(directory)))
				{
					for (Path path : directoryStream)
					{
						File f = path.toFile();
						String filename = f.getName();
						// Note the: startsWith(name+".log")  will include all "rollover" log files (.log, .log.1, .log.2)
						if (f.isFile() && filename.startsWith(name) && (filename.startsWith(name+".log") || filename.endsWith(".console")))
						{
							f.delete();
							removedList.add(f.getName());
						}

						// Also remove "ALARM" file(s)
						if (f.isFile() && (filename.equals("ALARM.ACTIVE."+name+".txt") || filename.equals("ALARM.LOG."+name+".log")) )
						{
							f.delete();
							removedList.add(f.getName());
						}
					}
				}

				ao.add("Remove Collector Log File", new ActionType(ActionStatus.SUCCESS, "Removed "+removedList.size()+" Collector Log Files for '"+name+"'. Here is the list "+removedList));
			}
			catch (Exception e) 
			{
				ao.add("Remove Collector Log File", new ActionType(ActionStatus.FAIL, "Problems Collector Log Files for '"+name+"'. Caught: "+e));
			}
		}

		// REMOVE: DSR - Daily Summary Report files
		if (removeDsrFiles)
		{
			String directory = DbxTuneCentral.getAppReportsDir();
			List<String> removedList = new ArrayList<>();
			
			try 
			{
				try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(directory)))
				{
					for (Path path : directoryStream)
					{
						File f = path.toFile();
						String filename = f.getName();

						if (f.isFile() && filename.startsWith(name) && (filename.endsWith(".html")))
						{
							String fn = filename;

							// is it a TimeStamped file YYYY-MM-DD_HHMM
							if (fn.matches("[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]_[0-9][0-9][0-9][0-9]"))
							{
								f.delete();
								removedList.add(f.getName());
							}
						}
					}
				}

				ao.add("Remove Collector DSR File", new ActionType(ActionStatus.SUCCESS, "Removed "+removedList.size()+" Collector DSR Files for '"+name+"'. Here is the list "+removedList));
			}
			catch (Exception e) 
			{
				ao.add("Remove Collector DSR File", new ActionType(ActionStatus.FAIL, "Problems Collector DSR Files for '"+name+"'. Caught: "+e));
			}
		}

		// Wait for STOP/KILL: Collector
		if (waitforStopCollector)
		{
			long timeout = Configuration.getCombinedConfiguration().getLongProperty("collector.stop.timeout", 65 * 1000);

			try
			{
				// Wait for server to be stopped
				long waitTime = waitforStopCollector(name, timeout);
				ao.add("Waitfor Stopping DbxTune Collector", new ActionType(ActionStatus.SUCCESS, "Succeeded waiting for server '"+name+"' to stop. waitTime=" + waitTime + ", maxWaitTime="+timeout));
			}
			catch (TimeoutException e) 
			{
				ao.add("Stopping DbxTune Collector", new ActionType(ActionStatus.FAIL, "Timeout waiting for server '"+name+"' to stop. The server will Hopefully soon be stopped. waited for " + timeout + " ms."));
			}
		}

		return ao;
	}



	/**
	 * Enable or Disable a Server<br>
	 * @param name
	 * 
	 * @return A ActionObject that can be converted to a JSON object with what we have done
	 */
	private ActionObject enableOrDisableManagedServer(String name, boolean disable)
	{
		ActionObject ao = new ActionObject();

		CentralPersistReader reader = CentralPersistReader.getInstance();

		// DISABLE
		if (disable)
		{
			try
			{
				reader.sessionStatusSet(name, DbxCentralSessions.ST_DISABLED);
				ao.add("DISABLE", new ActionType(ActionStatus.SUCCESS, "Disabled server '"+name+"'."));
			} 
			catch (SQLException e) 
			{
				ao.add("DISABLE", new ActionType(ActionStatus.FAIL, "Problems Disabling server '"+name+"'. Caught: "+e));
			}
		}
		else
		{
			try
			{
				reader.sessionStatusUnSet(name, DbxCentralSessions.ST_DISABLED);
				ao.add("ENABLE", new ActionType(ActionStatus.SUCCESS, "Enable server '"+name+"'."));
			} 
			catch (SQLException e) 
			{
				ao.add("ENABLE", new ActionType(ActionStatus.FAIL, "Problems Enabling server '"+name+"'. Caught: "+e));
			}
		}

		return ao;
	}
	
	
	
	/**
	 * Clear Active Alarms for a Specific Server in DbxCentral<br>
	 * @param name
	 * 
	 * @return A ActionObject that can be converted to a JSON object with what we have done
	 */
	private ActionObject clearActiveAlarmsForServer(String name)
	{
		ActionObject ao = new ActionObject();

		CentralPersistReader reader = CentralPersistReader.getInstance();

		try
		{
			int rowsDeleted = reader.clearAlarmsAllActive(name);
			ao.add("REMOVE-ACTIVE-ALARMS", new ActionType(ActionStatus.SUCCESS, "Removed " + rowsDeleted + " Active Alarm(s) for server '"+name+"'."));
		} 
		catch (SQLException e) 
		{
			ao.add("REMOVE-ACTIVE-ALARMS", new ActionType(ActionStatus.FAIL, "Problems removing Active Alarm(s) for server '"+name+"'. Caught: "+e));
		}

		return ao;
	}
	
	
	
	
	
	
	private int stopCollector(String serverName)
	throws Exception
	{
		String directory = DbxTuneCentral.getAppInfoDir();

		// Check if the file exists
		File f = new File(directory + "/" + serverName + ".dbxtune");
		if ( ! f.exists() )
			throw new Exception("The controller file '" + f.getAbsolutePath() + "' didn't exists, so the PID is unknown");

		// Read the file
		Configuration conf = new Configuration(f.getAbsolutePath());
		conf.load();
		
		// Get the PID
		int pid = conf.getIntProperty("dbxtune.pid", -1);
		if (pid == -1)
			throw new Exception("Can't find a PID ");
		
		// Possibly if we want to check that it runs on same server
//		String tcpPort = conf.getProperty("pcs.h2.tcp.url", null);

		// KILL the PID
		//throw new Exception("stopCollector() -- Not yet implemented.");
		
		List<String> cmd = new ArrayList<>();
//		String cmd = "kill " + pid;
		if (PlatformUtils.isWindows())
		{
			cmd.add("taskkill");
			cmd.add("/F");
			cmd.add("/PID");
			cmd.add(pid + "");
		}
		else
		{
			cmd.add("kill");
			cmd.add(pid + "");
		}
			

		Process process = new ProcessBuilder(cmd).start();
		int exitCode = process.waitFor();

		if (exitCode != 0)
			throw new Exception("Expected return code 0 when issuing command '" + cmd + "'. Actual return code was "+ exitCode);
		
		return pid;
	}

	private long waitforStopCollector(String serverName, long timeout)
	throws TimeoutException
	{
		long startTime = System.currentTimeMillis();
		
		String directory = DbxTuneCentral.getAppInfoDir();
		File f = new File(directory + "/" + serverName + ".dbxtune");
		
		while(true)
		{
			long waitTime = TimeUtils.msDiffNow(startTime);

			if (waitTime > timeout)
				throw new TimeoutException("Timeout waiting for server '" + serverName + "' to shutdown. waitTime=" + waitTime + ", timeout=" + timeout + ".");

			if (f.exists())
			{
				_logger.info("Waiting for server '" + serverName + "' to shutdown. waitTime=" + waitTime + ", timeout=" + timeout + ". (expecting file '" + f .getAbsolutePath() + "' to be removed.");
				try { Thread.sleep(1000); }
				catch(InterruptedException ignore) {}
			}
			else
			{
				_logger.info("Done waiting for server '" + serverName + "' to shutdown. waitTime=" + waitTime + ". The server has now been stopped.");
				return waitTime;
			}
		}
	}
	
	

	public enum ActionStatus
	{
		SUCCESS,
		FAIL
	};
	@JsonPropertyOrder(value = {"status", "message"}, alphabetic = true)
	public static class ActionType
	{
		private ActionStatus _status;
		private String       _message;
		
		public String getStatus() { return _status.toString(); }
		public String getMessage() { return _message; }

		ActionType(ActionStatus status, String message) 
		{
			_status  = status;
			_message = message;
		}
	};
	/**
	 * Object to hold actions that was made during
	 */
	private static class ActionObject
	{
		private Map<String, ActionType> _map = new LinkedHashMap<>();

		public void add(String action, ActionType status)
		{
			_map.put(action, status);
		}
		
		public Map<String, ActionType> getActions()
		{
			return _map;
		}
	}
}
