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
package com.dbxtune.central.controllers;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.AseTune;
import com.dbxtune.SqlServerTune;
import com.dbxtune.central.pcs.CentralPersistReader;
import com.dbxtune.central.pcs.objects.DbxCentralSessions;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.SpaceForecast;
import com.dbxtune.utils.StringUtil;

public class SpaceForecastServlet
extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public static final String OUTPUT_TYPE_HTML_PAGE  = "html-page";
	public static final String OUTPUT_TYPE_HTML_TABLE = "html-table";
	public static final String OUTPUT_TYPE_JSON       = "json";
	
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
//		resp.setContentType("text/html");
		resp.setContentType("text/html; charset=UTF-8");
		resp.setCharacterEncoding("UTF-8");
//		resp.setContentType("application/json");
//		resp.setCharacterEncoding("UTF-8");
//		ServletOutputStream out = resp.getOutputStream();
		PrintWriter out = resp.getWriter();


		// Check that we have a READER
		if ( ! CentralPersistReader.hasInstance() )
		{
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "No PCS Reader to: DBX Central Database.");
			return;
		}
		CentralPersistReader reader = CentralPersistReader.getInstance();


		String payload = "-not-initialized-";
		try
		{
			// Check for known input parameters
			if (Helper.hasUnKnownParameters(req, resp, "srv", "srvName", "type", "days", "period", "outType", "sparklineWidth"))
				return;

			String srvName        = Helper.getParameter(req, new String[] {"srv", "srvName"});
			String type           = Helper.getParameter(req, "type"          , "os");
			String days           = Helper.getParameter(req, "days"          , "30");
			String samplePeriod   = Helper.getParameter(req, "period"        , "60");
			String outType        = Helper.getParameter(req, "outType"       , "html-page");
			String sparklineWidth = Helper.getParameter(req, "sparklineWidth", "800");

			int int_days           =  StringUtil.parseInt(days          , 30 );
			int int_samplePeriod   =  StringUtil.parseInt(samplePeriod  , 60 );
			int int_sparklineWidth =  StringUtil.parseInt(sparklineWidth, 800);
			
			
			// Check that "srv" exists
			if (StringUtil.isNullOrBlank(srvName))
			{
				resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "srv|srvName is mandatory parameter. "
						+ "Usage: /api/space/forecast"
						+ "?srv=SRVNAME"
						+ "&type=os"
						+ "&days=30"
						+ "&period=60"
						+ "&outType=html-page"
						+ "&sparklineWidth=800"
						);
				return;
			}
			else
			{
				if ( ! reader.hasServerSession(srvName) )
				{
					resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Server name '"+srvName+"' do not exist in the DBX Central Database.");
					return;
				}
			}

			// Check: days
			try { Integer.valueOf(days); }
			catch (NumberFormatException ex)
			{
				resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "days '" + days + "' is NOT a number. Default is 30 days.");
				return;
			}
			
			// Check: samplePeriod
			try { Integer.valueOf(samplePeriod); }
			catch (NumberFormatException ex)
			{
				resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "samplePeriod '" + samplePeriod + "' is NOT a number. Default is 60 minutes");
				return;
			}
			
			// Get what TYPE of server it is, so we can decide what Reports we want to create
			//DbxCentralSessions xxx = reader.getLastSession(DbxConnection conn, String serverName)

			// Check that "type" exists
//			String[] knownTypes = new String[] {"os", "db", "data", "wal", "log", "tlog", "xlog"};
			String[] knownTypes = new String[] {"os", "db", "data", "wal", "log", "tlog", "xlog", "all"};
			if ( ! StringUtil.containsAny(type, knownTypes))
			{
				resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "type '" + type + "' is unknown. KnownTypes={" + StringUtil.toCommaStrQuoted(knownTypes) + "}.");
				return;
			}
			// Normalize "aliased" types to "real" types
			if (type.equalsIgnoreCase("db"))   type = "data";
			if (type.equalsIgnoreCase("log"))  type = "wal";
			if (type.equalsIgnoreCase("tlog")) type = "wal";
			if (type.equalsIgnoreCase("xlog")) type = "wal";
			
			// Check that "outType" exists
			knownTypes = new String[] {OUTPUT_TYPE_HTML_PAGE, OUTPUT_TYPE_HTML_TABLE, OUTPUT_TYPE_JSON};
			if ( ! StringUtil.containsAny(outType, knownTypes))
			{
				resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "outType '" + outType + "' is unknown. KnownTypes={" + StringUtil.toCommaStrQuoted(knownTypes) + "}.");
				return;
			}

			// Get the type of collector, so we can decide what source table we want to get information from
			// for 'DbxCentral' (DbxcLocalMetrics) the session will probably be null.
			DbxCentralSessions session = reader.getLastSession(srvName);
			String dbxCollector = "-unknown-";
			if (session != null)
			{
				dbxCollector = session.getProductString();
			}

			// Normal handling: NOT ALL
			if ( ! type.equalsIgnoreCase("all") )
			{
				// Decide what source tables
				String cmName    = null;
				String graphName = null;
				if (AseTune.APP_NAME.equals(dbxCollector))
				{
					if (type.equalsIgnoreCase("os"))   { cmName = "CmOsDiskSpace";   graphName = "FsAvailableMb"; }
					if (type.equalsIgnoreCase("data")) { cmName = "CmOpenDatabases"; graphName = "DbDataSizeLeftMbGraph"; }
					if (type.equalsIgnoreCase("wal"))  { cmName = "CmOpenDatabases"; graphName = "DbLogSizeLeftMbGraph"; }
				}
				else if (SqlServerTune.APP_NAME.equals(dbxCollector))
				{
					if (type.equalsIgnoreCase("os"))   { cmName = "CmOsDiskSpace"; graphName = "FsAvailableMb"; }
					if (type.equalsIgnoreCase("data")) { cmName = "CmDatabases";   graphName = "DbDataSizeLeftMbGraph"; }
					if (type.equalsIgnoreCase("wal"))  { cmName = "CmDatabases";   graphName = "DbLogSizeLeftMbGraph"; }
				}
				else
				{
					if (type.equalsIgnoreCase("os"))   { cmName = "CmOsDiskSpace"; graphName = "FsAvailableMb"; }
					else
					{
						resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "type '" + type + "' is not supported for Collector '" + dbxCollector + "'.");
					}
				}
				
				// get Data
				DbxConnection conn = reader.getConnection(); // Get connection from a ConnectionPool
				try
				{
					SpaceForecast forecast = new SpaceForecast(conn, srvName, cmName, graphName)
						.setDays               ( int_days           )
						.setDownsamplePeriod   ( int_samplePeriod   )
						.setSparklineChartWidth( int_sparklineWidth )
						;

					// Make the forecast (reads data and does predictions)
					forecast.doForecast();

					// Produce some output
					if (outType.equalsIgnoreCase("html-page"))
					{
						payload = forecast.generateHtmlReport(true);
					}
					else if (outType.equalsIgnoreCase("html-table"))
					{
						payload = forecast.generateHtmlReport(false);
					}
					else if (outType.equalsIgnoreCase("json"))
					{
						resp.setContentType("application/json");
						payload = forecast.toJson();
					}
				}
				finally 
				{
					if (conn != null)
						reader.releaseConnection(conn);
				}
			}
			// Special case... if we want ALL (OS, DATA & WAL) at the same output
			// payload is NOT used... for returning data.
			// we are FLUSHING for every report
			else
			{
				if ( ! outType.toLowerCase().startsWith("html") )
				{
					resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "outType '" + outType +"' is not supported for type='all'.");
				}

				payload = "";
				List<String> execList = new ArrayList<>();
				
				// Add source tables to MAP
				if (AseTune.APP_NAME.equals(dbxCollector))
				{
					execList.add("CmOsDiskSpace"   + ":" + "FsAvailableMb");
					execList.add("CmOpenDatabases" + ":" + "DbDataSizeLeftMbGraph");
					execList.add("CmOpenDatabases" + ":" + "DbLogSizeLeftMbGraph");
				}
				else if (SqlServerTune.APP_NAME.equals(dbxCollector))
				{
					execList.add("CmOsDiskSpace"   + ":" + "FsAvailableMb");
					execList.add("CmDatabases"     + ":" + "DbDataSizeLeftMbGraph");
					execList.add("CmDatabases"     + ":" + "DbLogSizeLeftMbGraph");
				}
				else
				{
					execList.add("CmOsDiskSpace"   + ":" + "FsAvailableMb");
				}
				
				// get Data
				DbxConnection conn = reader.getConnection(); // Get connection from a ConnectionPool
				try
				{
					if (outType.toLowerCase().startsWith("html"))
					{
						out.println(SpaceForecast.generateHtmlReportBegin(srvName, int_days, int_samplePeriod));
						out.flush();
					}

					for (String entry : execList)
					{
						String[] sa = entry.split(":");
						String cmName    = sa[0];
						String graphName = sa[1];
						
						// Get forecast for each
						SpaceForecast forecast = new SpaceForecast(conn, srvName, cmName, graphName)
								.setDays               ( int_days )
								.setDownsamplePeriod   ( int_samplePeriod )
								.setSparklineChartWidth( int_sparklineWidth )
								;

						// Make the forecast (reads data and does predictions)
						forecast.doForecast();

						// Produce some output
						if (outType.toLowerCase().startsWith("html"))
						{
							String info = "-unknown-";
							if (graphName.equals("DbDataSizeLeftMbGraph")) info = "DBMS Data Report"; 
							if (graphName.equals("DbLogSizeLeftMbGraph" )) info = "DBMS Wal/Transaction Log Report"; 
							if (graphName.equals("FsAvailableMb"        )) info = "OS Disk Report"; 

							out.println("<h3>");
							out.println(info);
							out.println("</h3>");

							out.println("<p>");
							out.println(forecast.generateHtmlReport(false));
							out.println("</p>");
							out.println("<br>");
							out.flush();
						}
					}

					if (outType.toLowerCase().startsWith("html"))
					{
						out.println("<code>-end-of-report-</code>");
						out.println(SpaceForecast.generateHtmlReportEnd());
						out.flush();
					}
				}
				finally 
				{
					if (conn != null)
						reader.releaseConnection(conn);
				}
				
			}
		}
		catch (Exception e)
		{
			_logger.info("Problem accessing DBMS or writing JSON, Caught: " + e, e);
			throw new ServletException("Problem accessing db or writing JSON, Caught: " + e, e);
		}

		out.println(payload);

		out.flush();
		out.close();
	}
}
