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

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.dbxtune.central.DbxTuneCentral;
import com.dbxtune.central.pcs.CentralPersistReader;
import com.dbxtune.central.pcs.objects.DbxCentralServerDescription;
import com.dbxtune.central.pcs.objects.DbxCentralSessions;
import com.dbxtune.central.pcs.objects.DbxGraphProperties;
import com.dbxtune.utils.StringUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SessionsController 
extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	private static final Logger _logger = Logger.getLogger(MethodHandles.lookup().lookupClass());

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		ServletOutputStream out = resp.getOutputStream();
		resp.setContentType("text/html");
		resp.setCharacterEncoding("UTF-8");
//		resp.setContentType("application/json");
//		resp.setCharacterEncoding("UTF-8");


		// Check that we have a READER
		if ( ! CentralPersistReader.hasInstance() )
		{
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "No PCS Reader to: DBX Central Database.");
			return;
		}
		CentralPersistReader reader = CentralPersistReader.getInstance();


		String payload;
		try
		{
			// Check for known input parameters
			if (Helper.hasUnKnownParameters(req, resp, "type", "graphs", "status"))
				return;

			String type      = Helper.getParameter(req, "type",   "last");
			String graphs    = Helper.getParameter(req, "graphs", "false");
			String statusStr = Helper.getParameter(req, "status", "-1");

			if (type == null)
				type = "last";
			
			if (graphs == null)
				graphs = "false";
			
			boolean onlyLast  = type  .trim().equalsIgnoreCase("last");
			boolean getGraphs = graphs.trim().equalsIgnoreCase("true");

			int status = StringUtil.parseInt(statusStr, -1);
			
			// get Data
			List<DbxCentralSessions> list = reader.getSessions( onlyLast, status );
			
			// TODO: maybe order the list in the same order as the content of file "conf/SERVER_LIST"
			// ##===========================================================================
			// ## Fields in this file
			// ## 1 - ASE SERVERNAME
			// ## 2 - 1=Enabled, 0=Disabled
			// ## 3 - Some explanation for the role of this server
			// ##===========================================================================
            // 
			// PROD_A1_ASE; 1 ; PROD Active Side - New
			// PROD_B1_ASE; 1 ; PROD Standby Side - New
			// PROD_A_ASE ; 1 ; PROD Active Side
			// PROD_B_ASE ; 1 ; PROD Standby Side
			// DEV_ASE    ; 1 ; DEV
			// SYS_ASE    ; 0 ; SYS
			// INT_ASE    ; 0 ; INT
			// STAGE_ASE  ; 0 ; STAGE

			
//test this new code
//- Also add a Servlet that can fetch the DbxCentralServerDescription

			String filename = StringUtil.hasValue(DbxTuneCentral.getAppConfDir()) ? DbxTuneCentral.getAppConfDir() + "/SERVER_LIST" : "conf/SERVER_LIST";
			File f = new File(filename);
			if (f.exists())
			{
				
				Map<String, DbxCentralServerDescription> map = new HashMap<>();
				try 
				{
					map = DbxCentralServerDescription.getFromFile(filename);
				}
				catch (IOException ex)
				{
					_logger.warn("Problems reading file '"+f+"'. This is used to sort the 'sessions list'. Skipping this... Caught: "+ex);
				}

				if ( ! map.isEmpty() )
				{
					_logger.info("Sorting sessions using file '"+f+"' in the following order "+StringUtil.toCommaStrQuoted('"', map.keySet()));
					List<DbxCentralSessions> newList = new ArrayList<>();

					// foreach serverName in the list, transfer fetched list entries
					for (String srvName : map.keySet())
					{
						Iterator<DbxCentralSessions> i = list.iterator();
						while (i.hasNext())
						{
							DbxCentralSessions entry = i.next();
							if (srvName.equals(entry.getServerName()))
							{
								newList.add(entry);
								i.remove();
							}
						}
					}

					// copy all the entries that was NOT part of the SERVER_LIST file
					newList.addAll(list);
					
					// swap the lists
					list = newList;
				}
			}
			else
			{
				_logger.info("Sorting sessions will not be done. file '"+f+"' do not exist.");
			}

			if (getGraphs)
			{
				for (DbxCentralSessions s : list)
				{
					String    serverName       = s.getServerName();
					Timestamp sessionStartTime = s.getSessionStartTime();

					List<DbxGraphProperties> gp = reader.getGraphProperties(serverName, sessionStartTime.toString());
					s.setGraphProperties(gp);
				}
			}
			
			// to JSON
			ObjectMapper om = Helper.createObjectMapper();
			payload = om.writeValueAsString(list);
		}
		catch (Exception e)
		{
			_logger.info("Problem accessing DBMS or writing JSON, Caught: "+e, e);
			throw new ServletException("Problem accessing db or writing JSON, Caught: "+e, e);
		}
		
		out.println(payload);
		
		out.flush();
		out.close();
	}
}
