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
package com.asetune.central.controllers;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.asetune.central.pcs.CentralPersistReader;
import com.asetune.central.pcs.objects.DbxCentralSessions;
import com.asetune.utils.StringUtil;
import com.asetune.utils.TimeUtils;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class HistoryActiveSamplesForCmController 
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
			if (Helper.hasUnKnownParameters(req, resp, "srv", "srvName", "cm", "cmName", "startTime", "endTime"))
				return;
			
			String input_srv       = Helper.getParameter(req, new String[] {"srv", "srvName"} );
			String input_cmName    = Helper.getParameter(req, new String[] {"cm", "cmName"}, null ); // Optional
			String input_startTime = Helper.getParameter(req, new String[] {"startTime"} );
			String input_endTime   = Helper.getParameter(req, new String[] {"endTime"} );

			Timestamp input_startTs = TimeUtils.parseToTimestamp(input_startTime, "yyyy-MM-dd HH:mm:ss");
			Timestamp input_endTs   = TimeUtils.parseToTimestamp(input_endTime  , "yyyy-MM-dd HH:mm:ss");

			
			// POSSIBLY:
			//  * Parse input_srv for CSV into List
			//  * Parse input_cmName for CSV into List
			// Then pas the above lists to: reader.getLastSampleForCm(srvList, cmList)


			// get Data: sampleTime<SrvName, Set<CmNames>>
			Map<Timestamp, Map<String, Set<String>>> map = reader.getHistoryActiveSamplesForCm(input_srv, input_cmName, input_startTs, input_endTs);
			
			if (map.isEmpty())
			{
				payload = "[]";
//System.out.println("LastSampleForCmController: DBMS-NO-RESULT: srvName='" + input_srv + "', CmName='" + input_cmName + "'.");
			}
			else
			{
				// Create an JSON Object Mapper
				ObjectMapper om = Helper.createObjectMapper();
//				om.configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true);

				// 
				StringWriter sw = new StringWriter();

				JsonFactory jfactory = new JsonFactory();
				JsonGenerator gen = jfactory.createGenerator(sw);
				gen.setPrettyPrinter(new DefaultPrettyPrinter());
				gen.setCodec(om); // key to get 'gen.writeTree(cmLastSampleJsonTree)' to work
				
				// JSON will look like:
				// [
				//     {
				//         "sampleTime": #######,
				//         "sampleTimeIso8601": "2023-06-08T00:20:53.716+02:00",
				//         "srvNames": 
				//         [ 
				//             { 
				//                 "srvName": "GORAN_UB3_DS",
				//                 "appName": "AseTune",
				//                 "cmNames": ["CmActiveStatements", "CmXxx", "CmYyy"]
				//             },
				//             { 
				//                 "srvName": "GORAN_UB4_DS",
				//                 "appName": "AseTune",
				//                 "cmNames": ["CmActiveStatements", "CmAaa", "CmBbb", "CmCcc"]
				//             },
				//         ],
				//     },
				//     {
				//         "sampleTime": #######,
				//         ...
				//     }
				// ]

				gen.writeStartArray(); // ------------- START ARRAY 'outer' ------------- 

				for (Entry<Timestamp, Map<String, Set<String>>> srvEntry : map.entrySet())
				{
					gen.writeStartObject(); // ------------- START OBJECT 'sample' -------------

					Timestamp ts = srvEntry.getKey();
					Map<String,Set<String>> srvEntryMap = srvEntry.getValue();

					gen.writeNumberField("sampleTime",        ts.getTime());
					gen.writeStringField("sampleTimeIso8601", TimeUtils.toStringIso8601(ts));
					gen.writeFieldName  ("srvNames");
					gen.writeStartArray(); // ------------- START ARRAY 'srvNames' ------------- 

					for (Entry<String, Set<String>> srvCmEntry : srvEntryMap.entrySet())
					{
						String srvName = srvCmEntry.getKey();
						Set<String> cmSet = srvCmEntry.getValue();

						// Get DbxTune Type (ProductName)
						DbxCentralSessions srvInfo = reader.getLastSession(srvName);
						String appName = srvInfo.getProductString();

						gen.writeStartObject(); // ------------- START OBJECT 'srvName' -------------
						gen.writeStringField("srvName", srvName);
						gen.writeStringField("appName", appName);
						gen.writeFieldName("cmNames");
						gen.writeStartArray(); // ------------- START ARRAY 'cmNames' ------------- 
						for (String cmName : cmSet)
							gen.writeString(cmName);
						gen.writeEndArray();  // ------------- END ARRAY 'cmNames' -------------
						gen.writeEndObject(); // ------------- END OBJECT 'srvName' -------------
						gen.writeEndArray();  // ------------- END ARRAY 'srvNames' -------------
					}

					gen.writeEndObject(); // ------------- END OBJECT 'sample' -------------
				}

				gen.writeEndArray(); // ------------- END ARRAY 'outer' -------------
				gen.close();

				// And output as a String
				payload = sw.toString();
//System.out.println("HistoryActiveSamplesForCmController: PAYLOAD: input_srv=|" + input_srv + "|, input_cmName=|" + input_cmName + "|, input_startTs=|" + input_startTs + "|, input_endTs=|" + input_endTs + "|. returns: " + payload);
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
	




	//---------------------------------------------------------------------------------
	//-- Below is SIMPLE test code
	//---------------------------------------------------------------------------------
	private static Map<Timestamp, Map<String, Set<String>>> dummyRecords()
	throws Exception
	{
		Map<Timestamp, Map<String, Set<String>>> map = new TreeMap<>();
		Map<String, Set<String>> srvMap = null;
		Set<String> cmSet = null;

		srvMap = new LinkedHashMap<>();
		cmSet = new LinkedHashSet<>();
		cmSet.add("CmAaa");
		cmSet.add("CmBbb");
		srvMap.put("GORAN_UB3_DS", cmSet);
		map.put(new Timestamp(System.currentTimeMillis()), srvMap);

		Thread.sleep(10);

		srvMap = new LinkedHashMap<>();
		cmSet = new LinkedHashSet<>();
		cmSet.add("CmCcc");
		cmSet.add("CmDdd");
		srvMap.put("GORAN_UB4_DS", cmSet);
		map.put(new Timestamp(System.currentTimeMillis()), srvMap);
		
		
		return map;
	}
	public static void main(String[] args)
	{
		try
		{
			Map<Timestamp, Map<String, Set<String>>> map = dummyRecords();
			System.out.println("MAP=" + map);

			
			ObjectMapper om = Helper.createObjectMapper();

			// 
			StringWriter sw = new StringWriter();

			JsonFactory jfactory = new JsonFactory();
			JsonGenerator gen = jfactory.createGenerator(sw);
			gen.setPrettyPrinter(new DefaultPrettyPrinter());
			gen.setCodec(om); // key to get 'gen.writeTree(cmLastSampleJsonTree)' to work

//			gen.writeStartArray(); // ------------- START ARRAY ------------- 
//			gen.writeStartObject(); // ------------- START OBJECT -------------
//			gen.writeStringField("srvName", "xxxx");
//			gen.writeEndObject(); // ------------- END OBJECT -------------
//			gen.writeEndArray(); // ------------- END ARRAY -------------


			gen.writeStartArray(); // ------------- START ARRAY 'outer' ------------- 

			for (Entry<Timestamp, Map<String, Set<String>>> srvEntry : map.entrySet())
			{
				Timestamp ts = srvEntry.getKey();
				Map<String,Set<String>> srvEntryMap = srvEntry.getValue();
System.out.println("TS=|"+ts+"|, srvEntryMap=|"+srvEntryMap+"|.");

				gen.writeStartObject(); // ------------- START OBJECT 'sample' -------------

				gen.writeNumberField("sampleTime",        ts.getTime());
				gen.writeStringField("sampleTimeIso8601", TimeUtils.toStringIso8601(ts));
				gen.writeFieldName  ("srvNames");
				gen.writeStartArray(); // ------------- START ARRAY 'srvNames' ------------- 

				for (Entry<String, Set<String>> srvCmEntry : srvEntryMap.entrySet())
				{
					String srvName = srvCmEntry.getKey();
					Set<String> cmSet = srvCmEntry.getValue();
System.out.println("    srvName=|"+srvName+"|, cmSet=|"+cmSet+"|.");

					String appName = "DummyAppName";

					gen.writeStartObject(); // ------------- START OBJECT 'srvName' -------------
					gen.writeStringField("srvName", srvName);
					gen.writeStringField("appName", appName);
					gen.writeFieldName("cmNames");
					gen.writeStartArray(); // ------------- START ARRAY 'cmNames' ------------- 
					for (String cmName : cmSet)
						gen.writeString(cmName);
					gen.writeEndArray(); // ------------- END ARRAY 'cmNames' -------------
					gen.writeEndObject(); // ------------- END OBJECT 'srvName' -------------
					gen.writeEndArray(); // ------------- END ARRAY 'srvNames' -------------
				}

				gen.writeEndObject(); // ------------- END OBJECT 'sample' -------------
			}

			gen.writeEndArray(); // ------------- END ARRAY 'outer' -------------
			gen.close();

			
			// And output as a String
			String payload = sw.toString();
System.out.println("PAYLOAD: " + payload);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		
	}
}
