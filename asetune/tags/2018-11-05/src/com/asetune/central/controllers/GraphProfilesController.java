package com.asetune.central.controllers;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.security.Principal;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.asetune.central.pcs.CentralPersistReader;
import com.asetune.central.pcs.objects.DbxCentralProfile;
import com.asetune.utils.StringUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GraphProfilesController
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

		String payload;
		try
		{
			String name = req.getParameter("name");
			String user = req.getParameter("user");
			String type = req.getParameter("dbxTypeName");

			if (StringUtil.isNullOrBlank(user))
			{
    			Principal principal = req.getUserPrincipal();
    			if (principal != null)
    				user = principal.getName();
			}

			CentralPersistReader reader = CentralPersistReader.getInstance();

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
}
