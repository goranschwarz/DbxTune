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

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.security.Principal;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.central.pcs.CentralPersistReader;
import com.dbxtune.central.pcs.objects.DbxCentralProfile;
import com.dbxtune.utils.StringUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GraphProfilesController
extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

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

		// Check for known input parameters
		if (Helper.hasUnKnownParameters(req, resp, "name", "user", "dbxTypeName"))
			return;

		
		String payload;
		try
		{
			String name = Helper.getParameter(req, "name");
			String user = Helper.getParameter(req, "user");
			String type = Helper.getParameter(req, "dbxTypeName");

			if (StringUtil.isNullOrBlank(user))
			{
    			Principal principal = req.getUserPrincipal();
    			if (principal != null)
    				user = principal.getName();
			}

			if (StringUtil.hasValue(name))
			{
				payload = reader.getGraphProfile(name, user);
			}
			else
			{
				List<DbxCentralProfile> list = reader.getGraphProfiles(type, user);

				// If we did not get any result... Try with no-user
				if ( list.isEmpty() && StringUtil.hasValue(user) )
				{
					_logger.info("No profiles was found for type='"+type+"', user='"+user+"'... Trying with user='' (no-user)");				
					list = reader.getGraphProfiles(type, "");
				}

				ObjectMapper om = Helper.createObjectMapper();
				payload = om.writeValueAsString(list);
//System.out.println("GraphProfilesController.getGraphProfiles(type=|"+type+"|, user=|"+user+"|): returns payload: "+payload);
			}
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
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		ServletOutputStream out = resp.getOutputStream();
		resp.setContentType("text/html");
		resp.setCharacterEncoding("UTF-8");

//		// Check that we have a READER
//		if ( ! CentralPersistReader.hasInstance() )
//		{
//			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "No PCS Reader to: DBX Central Database.");
//			return;
//		}
//		CentralPersistReader reader = CentralPersistReader.getInstance();

		// Check for known input parameters
		if (Helper.hasUnKnownParameters(req, resp, "dbxProduct"))
			return;


		String dbxProduct = req.getParameter("dbxProduct");
		System.out.println("dbxProduct="+dbxProduct);
		
		StringBuilder sb = new StringBuilder();
		String line = null;
		try {
			BufferedReader reader = req.getReader();
			while ((line = reader.readLine()) != null)
				sb.append(line);
		} catch (Exception e) { /*report an error*/ }
		String jsonStr = sb.toString();
		System.out.println("jsonStr="+jsonStr);

		ObjectMapper om = new ObjectMapper();
//		DbxCentralProfile profile = om.readValue(req.getReader(), DbxCentralProfile.class);
		DbxCentralProfile profile = om.readValue(jsonStr, DbxCentralProfile.class);
		
//		System.out.println("profile="+profile);
//		System.out.println("profile.getProductString()=|"+profile.getProductString()+"|.");
//		System.out.println("profile.getProfileValue() =|"+profile.getProfileValue()+"|.");
//		System.out.println("profile.getProfileName()  =|"+profile.getProfileName()+"|.");
		
		try
		{
			CentralPersistReader reader = CentralPersistReader.getInstance();
			reader.setGraphProfile(profile);
		}
		catch (Exception e)
		{
			_logger.info("Problem accessing DBMS or writing JSON, Caught: "+e, e);
			throw new ServletException("Problem accessing db, Caught: "+e, e);
		}
		
		String payload = "{}"; // it seems that I need to return "something"
		out.println(payload);
		
		out.flush();
		out.close();
	}
}
