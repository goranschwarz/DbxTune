package com.asetune.central.controllers;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.asetune.central.pcs.CentralPersistReader;
import com.asetune.central.pcs.objects.DbxCentralProfile;
import com.asetune.central.pcs.objects.DbxGraphProperties;
import com.asetune.utils.StringUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GraphPropertiesController
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
			String sessionName      = req.getParameter("sessionName");
			String sessionStartTime = req.getParameter("sessionStartTime");
			
			CentralPersistReader reader = CentralPersistReader.getInstance();
			
			List<String> sessionNameList = StringUtil.commaStrToList(sessionName);
			List<DbxGraphProperties> list = new ArrayList<>();

			// Get available profiles 
			List<DbxCentralProfile> availableProfiles = reader.getGraphProfiles(null, null);

			// stuff the unique profile names in a Set
			Set<String> availableProfileNames = new HashSet<>();
			for (DbxCentralProfile profile : availableProfiles)
			{
				String profileName = profile.getProfileName();
				if (StringUtil.hasValue(profileName))
					availableProfileNames.add(profileName);
			}

			// get graph properties for all Servers in the sessionName (if it's a profilename, then skip that instance)
			for (String name : sessionNameList)
			{
				// if the input "sessionName" is a "profileName", then do not get properties for that
				if ( ! availableProfileNames.contains(name) )
					list.addAll(reader.getGraphProperties(name, sessionStartTime));
			}

//			payload = "sessionList="+sessionList;


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
//	@Override
//	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
//	{
//		ServletOutputStream out = resp.getOutputStream();
//
//		String payload;
//		try
//		{
//			String sessionName      = req.getParameter("sessionName");
//			String sessionStartTime = req.getParameter("sessionStartTime");
//
//			CentralPersistReader reader = CentralPersistReader.getInstance();
//
//			Map<String, DbxGraphProperties> map = new LinkedHashMap<>();
//
//			List<String> sessionNameList = StringUtil.commaStrToList(sessionName);
//			for (String name : sessionNameList)
//			{
//				List<DbxGraphProperties> list = reader.getGraphProperties(name, sessionStartTime);
//				
//				for (DbxGraphProperties gp : list)
//					map.put(gp.getTableName(), gp);
//			}
//			
//			
////			payload = "sessionList="+sessionList;
//
//
//			ObjectMapper om = Helper.createObjectMapper();
//			payload = om.writeValueAsString(map.values());
//		}
//		catch (Exception e)
//		{
//			_logger.info("Problem accessing DBMS or writing JSON, Caught: "+e, e);
//			throw new ServletException("Problem accessing db or writing JSON, Caught: "+e, e);
//		}
//		
//		out.println(payload);
//		
//		out.flush();
//		out.close();
//	}
}
