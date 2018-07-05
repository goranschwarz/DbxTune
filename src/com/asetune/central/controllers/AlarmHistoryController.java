package com.asetune.central.controllers;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.asetune.central.pcs.CentralPersistReader;
import com.asetune.central.pcs.objects.DbxAlarmHistory;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AlarmHistoryController 
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
			String srv      = req.getParameter("srv");
			String age      = req.getParameter("age");
			String type     = req.getParameter("type");
			String category = req.getParameter("category");

			// get Data
			CentralPersistReader reader = CentralPersistReader.getInstance();
			List<DbxAlarmHistory> list = reader.getAlarmHistory( srv, age, type, category );
//			List<DbxAlarmHistory> list = reader.getAlarmHistory( srv );

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
