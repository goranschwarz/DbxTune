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
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.central.pcs.CentralPersistReader;
import com.dbxtune.central.pcs.objects.DbxCentralSessions;
import com.dbxtune.utils.StringUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CollectorRefreshController 
extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public static final String REFRESH_REQUEST_SERVERNAME_TEMPLATE = "SERVERNAME";
	public static final String REFRESH_REQUEST_FILE_NAME_TEMPLATE = System.getProperty("java.io.tmpdir") + File.separatorChar + "DbxTune.refresh." + REFRESH_REQUEST_SERVERNAME_TEMPLATE;

	
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

		// SRVNAME, ResponceText
		Map<String, String> resultMap = new LinkedHashMap<>();

		try
		{
			// Check for known input parameters
			if (Helper.hasUnKnownParameters(req, resp, "srv", "srvName"))
				return;
			
			String input_srv = Helper.getParameter(req, new String[] {"srv", "srvName"} );

			for (String srvName : StringUtil.parseCommaStrToList(input_srv))
			{
				// Check that "srv" exists
				if (StringUtil.hasValue(srvName))
				{
					if ( ! reader.hasServerSession(srvName) )
					{
//						resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Server name '" + srvName + "' do not exist in the DBX Central Database.");
//						return;
						resultMap.put(srvName, "Server name '" + srvName + "' do not exist in the DBX Central Database.");
						continue;
					}
				}
				
				DbxCentralSessions srvInfo = reader.getLastSession(srvName);
				if ( ! srvInfo.getCollectorIsLocal() )
				{
//					resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Server name '" + srvName + "' isn't running locally. Refresh of remote collectors are not yet supported.");
//					return;
					resultMap.put(srvName, "Server name '" + srvName + "' isn't running locally. Refresh of remote collectors are not yet supported.");
					continue;
				}

				// Simply write a temp file, that will indicate a "refresh" request to any of the collector
				// The file will be removed by the collector when it sees it
				String filename = REFRESH_REQUEST_FILE_NAME_TEMPLATE.replace(REFRESH_REQUEST_SERVERNAME_TEMPLATE, srvName);

				_logger.info("Creating file '" + filename + "' as a notification for a collector refresh.");
				try { Files.createFile(Paths.get(filename)); }
				catch (FileAlreadyExistsException ignore) {}
				
				resultMap.put(srvName, "SUCCESS");
			}
		}
		catch (Exception e)
		{
//			_logger.error("Problems writing Counter JSON data for srvName='" + input_srv + "', CounterModel='" + input_cmName + "'.", ex);
			_logger.info("Problem accessing DBMS or writing JSON, Caught: "+e, e);
			throw new ServletException("Problem accessing db or writing JSON, Caught: "+e, e);
		}

		String payload = "";
		
		ObjectMapper om = Helper.createObjectMapper();
		payload = om.writeValueAsString(resultMap);

		out.println(payload);
		
		out.flush();
		out.close();
	}
}
