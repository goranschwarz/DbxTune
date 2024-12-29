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

import java.io.IOException;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.dbxtune.central.pcs.CentralPersistReader;
import com.dbxtune.central.pcs.objects.DbxCentralSessions;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.TimeUtils;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;

public class HistorySampleForCmController 
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
			if (Helper.hasUnKnownParameters(req, resp, 
					"srv", "srvName", 
					"cm",  "cmName", 
					"ts",  "sampleTime", 
					"startTime", 
					"endTime",
					"addSampleTime"
					))
				return;
			
			String  input_srv             = Helper.getParameter(req, new String[] {"srv", "srvName"} );
			String  input_cmName          = Helper.getParameter(req, new String[] {"cm", "cmName"} );
			String  input_sampleTime      = Helper.getParameter(req, new String[] {"ts", "sampleTime"}, null );
			String  input_startTime       = Helper.getParameter(req, new String[] {"startTime"}, null );
			String  input_endTime         = Helper.getParameter(req, new String[] {"endTime"}, null );
			boolean input_addSampleTime   = Helper.getParameter(req, new String[] {"addSampleTime"}, "true" ).trim().equalsIgnoreCase("true");
			boolean input_sampleTimeShort = Helper.getParameter(req, new String[] {"sampleTimeShort"}, "true" ).trim().equalsIgnoreCase("true");

			if (input_sampleTime == null && input_startTime == null && input_endTime == null)
			{
				resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Paramater 'sampleTime' or 'startTime' or 'endTime' must be specified.");
			}
				
			Timestamp input_sampleTimeTs = null;
			Timestamp input_startTs      = null;
			Timestamp input_endTs        = null;
			if (StringUtil.hasValue(input_sampleTime)) input_sampleTimeTs = TimeUtils.parseToTimestamp(input_sampleTime, "yyyy-MM-dd HH:mm:ss");
			if (StringUtil.hasValue(input_startTime))  input_startTs      = TimeUtils.parseToTimestamp(input_startTime, "yyyy-MM-dd HH:mm:ss");
			if (StringUtil.hasValue(input_endTime))    input_endTs        = TimeUtils.parseToTimestamp(input_endTime  , "yyyy-MM-dd HH:mm:ss");

			
//			for (String srvName : StringUtil.parseCommaStrToList(input_srv))
//			{
//			}

			// Check that "srv" exists
//			if (StringUtil.hasValue(input_srv))
//			{
//				if ( ! reader.hasServerSession(input_srv) )
//				{
//					resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Server name '" + input_srv + "' do not exist in the DBX Central Database.");
//					return;
//				}
//			}

			// POSSIBLY:
			//  * Parse input_srv for CSV into List
			//  * Parse input_cmName for CSV into List
			// Then pas the above lists to: reader.getLastSampleForCm(srvList, cmList)


			// get Data: SrvName<CmName, JsonText>
			Map<String, Map<String,String>> map = reader.getHistorySampleForCm(input_srv, input_cmName, input_sampleTimeTs, input_startTs, input_endTs, input_addSampleTime, input_sampleTimeShort);
			
			if (map.isEmpty())
			{
				payload = "[]";
//System.out.println("LastSampleForCmController: DBMS-NO-RESULT: srvName='" + input_srv + "', CmName='" + input_cmName + "'.");
			}
			else
			{
				// Create an JSON Object Mapper
				ObjectMapper om = Helper.createObjectMapper();

				// 
				StringWriter sw = new StringWriter();

				JsonFactory jfactory = new JsonFactory();
				JsonGenerator gen = jfactory.createGenerator(sw);
				gen.setPrettyPrinter(new DefaultPrettyPrinter());
				gen.setCodec(om); // key to get 'gen.writeTree(cmLastSampleJsonTree)' to work
				

				gen.writeStartArray(); // ------------- START ARRAY ------------- 

				// Loop the output from "getLastSampleForCm()" and create a JSON
				for (Entry<String, Map<String, String>> srvEntry : map.entrySet())
				{
					String srvName = srvEntry.getKey();
					Map<String,String> cmEntryMap = srvEntry.getValue();

					// Get DbxTune Type (ProductName)
					DbxCentralSessions srvInfo = reader.getLastSession(srvName);
					String appName = srvInfo.getProductString();
					
//System.out.println("LastSampleForCmController: 1-XXX --- srvName='" + input_srv + "'.");
					gen.writeStartObject(); // ------------- START OBJECT -------------

					gen.writeStringField("appName", appName);
					gen.writeStringField("srvName", srvName);
					gen.writeFieldName("cmNames");
					gen.writeStartArray(); // ------------- START ARRAY -------------

					for (Entry<String, String> cmEntry : cmEntryMap.entrySet())
					{
						String cmName   = cmEntry.getKey();
						String jsonText = cmEntry.getValue();
//System.out.println("LastSampleForCmController: 2-XXX --- srvName='" + input_srv + "', cmName='" + cmName + "', jsonText=|" + jsonText + "|.");

						if (StringUtil.hasValue(jsonText))
						{
							// Parse the JSON Stored ad text in DBMS
							JsonNode cmLastSampleJsonTree = om.readTree(jsonText);

//System.out.println("LastSampleForCmController: DBMS: srvName='" + input_srv + "', CmName='" + input_cmName + "'. \n"
//		+ "\tjsonText            =|" + jsonText             + "|, \n"
//		+ "\tcmLastSampleJsonTree=|" + cmLastSampleJsonTree + "|.");

							gen.writeStartObject(); // ------------- START OBJECT -------------

							gen.writeStringField("cmName", cmName);

							// Write the parsed JSON Tree Structure... That was previously stored ad text in DBMS
							gen.writeFieldName("lastSample");
//							w.writeString(jsonText);              // This writes it as a *escaped* string... (so no PURE JSON structure)
//							gen.writeRawValue(jsonText);          // This writes "whatever" string we pass in the JSON Stream... (if it's a faulty Syntax... we are out of luck)
							gen.writeTree(cmLastSampleJsonTree);  // Write the FULL JSON Tree (as nodes, not just as a String)

							gen.writeEndObject(); // ------------- END OBJECT -------------
						}
					}

					gen.writeEndArray(); // ------------- END ARRAY -------------
					gen.writeEndObject(); // ------------- END OBJECT -------------
				}

				gen.writeEndArray(); // ------------- END ARRAY -------------
				gen.close();

				// And output as a String
				payload = sw.toString();
//System.out.println("LastSampleForCmController: PAYLOAD: srvName='" + input_srv + "', CmName='" + input_cmName + "'. returns: " + payload);
			}
		}
		catch (Exception e)
		{
//			_logger.error("Problems writing Counter JSON data for srvName='" + input_srv + "', CounterModel='" + input_cmName + "'.", ex);
			_logger.info("Problem accessing DBMS or writing JSON, Caught: "+e, e);
			throw new ServletException("Problem accessing db or writing JSON, Caught: "+e, e);
		}
		
		out.println(payload);
		
		out.flush();
		out.close();
	}
}
