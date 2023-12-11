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
import java.lang.invoke.MethodHandles;
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

import com.asetune.central.lmetrics.LocalMetricsPersistWriterJdbc;
import com.asetune.central.pcs.CentralPersistReader;
import com.asetune.central.pcs.objects.DbxAlarmActive;
import com.asetune.central.pcs.objects.DbxCentralSessions;
import com.asetune.central.pcs.objects.HealthCheck;
import com.asetune.utils.StringUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Returns a JSON Structure
 * 
 * <pre>
 * [
 *     "srvName1": {
 *         "healthy": false,
 *         "alarms": [
 *             ...alarm details...
 *         ]
 *     },
 *     "srvName2": {
 *         "healthy": true,
 *         "alarms": []
 *     },
 * ]
 * </pre>
 * 
 * @author gorans
 *
 */
public class HealthCheckController
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
			if (Helper.hasUnKnownParameters(req, resp, "sessionName", "srv", "srvName",    "age",    "srvInfo", "serverInfo",   "res", "result"))
				return;
			
			// get parameter 'srv' or 'srvName'
			String  srv        = Helper.getParameter(req, new String[] {"sessionName", "srv", "srvName"} );
			String  ageStr     = Helper.getParameter(req, "age");
			boolean incSrvInfo = Helper.getParameter(req, new String[] {"srvInfo", "serverInfo"},  "true").trim().equalsIgnoreCase("true");
			String  resType    = Helper.getParameter(req, new String[] {"res", "result"}, "array" ).toLowerCase();

			int ageInSec = StringUtil.parseInt(ageStr, 0) * 60;

			// Check input for {res|result}, which can be: {arr|array|map}
			if ( ! StringUtil.equalsAny(resType, "arr", "array", "map") )
			{
				resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Paramater 'res' or 'result' is '"+resType+"', which is an unknown value. Allowed values are: 'array' or 'map'.");
				return;
			}
			if (resType.equals("arr")) resType = "array";


			// Put all servers into a MAP
			Map<String, HealthCheck> map = new LinkedHashMap<>();
			if (StringUtil.hasValue(srv))
			{
				// It might be a Comma Separated List
				for (String srvName : StringUtil.parseCommaStrToList(srv))
					map.put(srvName, new HealthCheck(srvName));
			}
			else
			{
				// Get all servers from the reader
				for (String srvName : reader.getServerSessions())
					map.put(srvName, new HealthCheck(srvName));
				
				// NOTE: 
				// Above entries will be sorted by: conf/SERVER_LIST
				// This can be disabled by property: CentralPersistReader.PROPKEY_SERVER_LIST_SORT
				// Another way to "sort" them in "your own" order is to specify the list in the URL like: dbxtune.acme.com:8080/api/healthcheck?srv=SRV1,SRV2,SRV3,SRV4,SRV5

				// Should we remove DbxCentral -- LocalMetrics
				// I don't think we want to check health on that...
				if (true)
					map.remove(LocalMetricsPersistWriterJdbc.LOCAL_METRICS_SCHEMA_NAME);
			}


			// Check that all the servers exists
			for (String srvName : map.keySet())
			{
				if ( ! reader.hasServerSession(srvName) )
				{
					resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Server name '"+srvName+"' do not exist in the DBX Central Database.");
					return;
				}
			}

			
			// Loop the entries and get "stuff"
			for (HealthCheck healthcheck : map.values())
			{
				String srvName = healthcheck.getServerName();
				
				// get Server Info
				DbxCentralSessions srvInfo = reader.getLastSession(srvName);
				healthcheck.setServerInfo(srvInfo);
				
				// get Active ALAMRS
				List<DbxAlarmActive> alarmList = reader.getAlarmActive(srvName);
				healthcheck.setAlarms(alarmList);
				
				// test if "healthy" should be true or false
				healthcheck.checkHealth(ageInSec);
				
				if ( ! incSrvInfo  &&  healthcheck.isHealthy() )
					healthcheck.setServerInfo(null);
			}


			// to JSON: Depending on 'result' = {array|map}
			ObjectMapper om = Helper.createObjectMapper();
			if (resType.equals("array"))
			{
				List<HealthCheck> retList = new ArrayList<>(map.values());
				payload = om.writeValueAsString(retList);
			}
			else
			{
				payload = om.writeValueAsString(map);
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
