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
import com.asetune.utils.StringUtil;
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

			boolean removeFromServerList = getParameter(req, "removeFromServerList", "true").trim().equalsIgnoreCase("true");
			boolean removeLogFiles       = getParameter(req, "removeLogFiles",       "true").trim().equalsIgnoreCase("true");
			boolean removeDbmsData       = getParameter(req, "removeDbmsData",       "true").trim().equalsIgnoreCase("true");
			boolean removeH2Files        = getParameter(req, "removeH2Files",        "true").trim().equalsIgnoreCase("true");
			
			// Remove the server name from the system
			ActionObject ao = removeManagedServer(inputName, removeFromServerList, removeLogFiles, removeDbmsData, removeH2Files);

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
	private ActionObject removeManagedServer(String name, boolean removeFromServerList, boolean removeLogFiles, boolean removeDbmsData, boolean removeH2Files)
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
				DbxCentralServerDescription.removeFromFile(filename, name, true); // 3 param: only comment out the file

				ao.add("Remove From Server List", new ActionType(ActionStatus.SUCCESS, "Removed server in ServerList '"+filename+"' for '"+name+"'."));
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
					}
				}

				ao.add("Remove Collector Log File", new ActionType(ActionStatus.SUCCESS, "Removed "+removedList.size()+" Collector Log Files for '"+name+"'. Here is the list "+removedList));
			}
			catch (Exception e) 
			{
				ao.add("Remove Collector Log File", new ActionType(ActionStatus.FAIL, "Problems Collector Log Files for '"+name+"'. Caught: "+e));
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
