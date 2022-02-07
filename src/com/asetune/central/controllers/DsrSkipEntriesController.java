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
package com.asetune.central.controllers;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.invoke.MethodHandles;
import java.sql.SQLException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.asetune.central.pcs.CentralPersistReader;
import com.asetune.central.pcs.objects.DsrSkipEntry;
import com.asetune.utils.StringUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DsrSkipEntriesController
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
			if (Helper.hasUnKnownParameters(req, resp, "srvName", "className", "entryType"))
				return;
			
			String srvName   = Helper.getParameter(req, new String[] {"srvName"} );
			String className = Helper.getParameter(req, new String[] {"className"} );
			String entryType = Helper.getParameter(req, new String[] {"entryType"} );

			// Check that "srv" exists
			if (StringUtil.hasValue(srvName))
			{
//				if ( ! reader.hasServerSession(srvName) )
//				{
//					resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Server name '"+srvName+"' do not exist in the DBX Central Database.");
//					return;
//				}
			}

			// get Data
			List<DsrSkipEntry> list = reader.getDsrSkipEntries(srvName, className, entryType);

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

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		PrintWriter out = response.getWriter();
		
		// Check if we are LOGGED in (and has authority to ADD this record)
		if ( ! Helper.isAuthorized(request, response) )
			return;


		// Check that we have a READER
		if ( ! CentralPersistReader.hasInstance() )
		{
			// Send: 400
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No PCS Reader to: DBX Central Database.");
			return;
		}
		CentralPersistReader reader = CentralPersistReader.getInstance();

		try
		{
			ObjectMapper mapper = new ObjectMapper();
			DsrSkipEntry entry = mapper.readValue(request.getReader(), DsrSkipEntry.class);
			
			if (entry.validateMandatoryFields() != null)
			{
				// Send: 400
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, entry.validateMandatoryFields());
				return;
			}

			// ADD
			reader.addDsrSkipEntry(entry);
		}
		catch (IOException ex)
		{
			// Send: 400
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Caught: " + ex);
//			return;
		}
		catch (SQLException ex)
		{
			_logger.info("Problem accessing DBMS or writing JSON, Caught: "+ex, ex);

			// Send: 400
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Caught: " + ex);
			//throw new ServletException("Problem accessing db or writing JSON, Caught: "+ex, ex);
		}
		
		out.close();
	}


	@Override
	protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		PrintWriter out = response.getWriter();
		
		// Check if we are LOGGED in (and has authority to REMOVE this record)
		if ( ! Helper.isAuthorized(request, response) )
			return;


		// Check that we have a READER
		if ( ! CentralPersistReader.hasInstance() )
		{
			// Send: 400
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No PCS Reader to: DBX Central Database.");
			return;
		}
		CentralPersistReader reader = CentralPersistReader.getInstance();

		try
		{
//			ObjectMapper mapper = new ObjectMapper();
//			DsrSkipEntries entry = mapper.readValue(request.getReader(), DsrSkipEntries.class);
//			
//			int rowc = reader.removeDsrSkipEntry(entry);
//			if (rowc <= 0)
//			{
//				// 404 - not found
//				response.sendError(HttpServletResponse.SC_NOT_FOUND, "NOT FOUND: srvName='" + entry.getSrvName() + "', className='" + entry.getClassName() + "', entryType='" + entry.getEntryType() + "'.");
//			}

			// Check for known input parameters
			if (Helper.hasUnKnownParameters(request, response, "srvName", "className", "entryType", "stringVal"))
				return;
			
			String srvName   = Helper.getParameter(request, new String[] {"srvName"} );
			String className = Helper.getParameter(request, new String[] {"className"} );
			String entryType = Helper.getParameter(request, new String[] {"entryType"} );
			String stringVal = Helper.getParameter(request, new String[] {"stringVal"} );

			if (StringUtil.isNullOrBlank(srvName) || StringUtil.isNullOrBlank(className) || StringUtil.isNullOrBlank(entryType) || StringUtil.isNullOrBlank(stringVal))
			{
				// Send: 400
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing parameters, all of the following must have values: srvName='" + srvName + "', className='" + className + "', entryType='" + entryType + "', stringVal='" + stringVal + "'.");
				return;
			}

			int rowc = reader.removeDsrSkipEntry(srvName, className, entryType, stringVal);
			if (rowc <= 0)
			{
				// 404 - not found
				response.sendError(HttpServletResponse.SC_NOT_FOUND, "NOT FOUND: srvName='" + srvName + "', className='" + className + "', entryType='" + entryType + "', stringVal='" + stringVal + "'.");
			}
		}
		catch (IOException ex)
		{
			// Send: 400
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Caught: " + ex);
//			return;
		}
		catch (SQLException ex)
		{
			_logger.info("Problem accessing DBMS or writing JSON, Caught: "+ex, ex);

			// Send: 400
			//resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Caught: " + ex);
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
			//throw new ServletException("Problem accessing db or writing JSON, Caught: "+ex, ex);
		}
		
		out.close();
	}
}
