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
import java.lang.invoke.MethodHandles;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.asetune.central.pcs.CentralPersistReader;
import com.asetune.central.pcs.CentralPersistReader.SampleType;
import com.asetune.central.pcs.objects.DbxGraphData;
import com.asetune.utils.StringUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GraphDataController
extends HttpServlet
{
	private static final long serialVersionUID = 1L;
//	private static Logger _logger = Logger.getLogger(GraphDataController.class);
	private static final Logger _logger = Logger.getLogger(MethodHandles.lookup().lookupClass());
	

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		ServletOutputStream out = resp.getOutputStream();
		resp.setContentType("text/html");
		resp.setCharacterEncoding("UTF-8");
//		resp.setContentType("application/json");
//		resp.setCharacterEncoding("UTF-8");

		// Check for known input parameters
		if (Helper.hasUnKnownParameters(req, resp, "sessionName", "srv", "srvName",    "cmName", "graphName", "startTime", "endTime", "sampleType", "sampleValue"))
			return;

		String sessionName    = Helper.getParameter(req, new String[] {"sessionName", "srv", "srvName"} );
		String cmName         = Helper.getParameter(req, "cmName");
		String graphName      = Helper.getParameter(req, "graphName");
		String startTime      = Helper.getParameter(req, "startTime");
		String endTime        = Helper.getParameter(req, "endTime");
		String sampleTypeStr  = Helper.getParameter(req, "sampleType");
		String sampleValueStr = Helper.getParameter(req, "sampleValue");

		int sampleValue = -1;
		if (StringUtil.hasValue(sampleValueStr))
		{
			try { sampleValue = Integer.parseInt(sampleValueStr); }
			catch (NumberFormatException nfe)
			{
				sampleValue = -1;
				_logger.info("Skipping parameter 'sampleValue', which can't be converted to an integer. using default of "+sampleValue+", passed value sampleValueStr='"+sampleValueStr+"'. Caught: "+nfe);
			}
		}

		SampleType sampleType = SampleType.AUTO;
		if (StringUtil.hasValue(sampleTypeStr))
		{
//			try { sampleType = SampleType.valueOf(sampleTypeStr); }
			try { sampleType = SampleType.fromString(sampleTypeStr); }
			catch (IllegalArgumentException ex)
			{
				sampleType = SampleType.ALL;
				_logger.info("Skipping parameter 'sampleType', which can't be converted to a known value. using default of "+sampleType+", passed value sampleTypeStr='"+sampleTypeStr+"'. Caught: "+ex);
			}
		}
//System.out.println("GraphData: getGraphData(sessionName='"+sessionName+"', cmName='"+cmName+"', graphName='"+graphName+"', startTime='"+startTime+"', endTime='"+endTime+"', avgOverMinutes='"+avgOverMinutes+"'.)");

		// Check that "sessionName" or "srvName" exists
		if (CentralPersistReader.hasInstance())
		{
			CentralPersistReader reader = CentralPersistReader.getInstance();

//			boolean hasSession = true;
//			if ( ! reader.hasServerSession(sessionName) ) hasSession = false;
//			if ( ! LocalMetricsPersistWriterJdbc.LOCAL_METRICS_SCHEMA_NAME.equals(sessionName) ) hasSession = false; // LOCAL_METRICS_SCHEMA_NAME = "DbxcLocalMetrics";
//
//			if ( ! hasSession )
			if ( ! reader.hasServerSession(sessionName) )
			{
				resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Session/Server name '"+sessionName+"' do not exist in the DBX Central Database.");
				return;
			}
		}
		
		String payload;
		try
		{
			CentralPersistReader reader = CentralPersistReader.getInstance();
			List<DbxGraphData> list = reader.getGraphData(sessionName, cmName, graphName, startTime, endTime, sampleType, sampleValue);
			
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

//		String payload;
//		try
//		{
//			CentralPersistReader reader = CentralPersistReader.getInstance();
//			List<DbxGraphData> list = reader.getGraphData(sessionName, cmName, graphName, startTime);
//			
//			//ObjectMapper om = Helper.createObjectMapper();
//			//payload = om.writeValueAsString(list);
//
////			StringWriter sw = new StringWriter();
////
////			JsonFactory jfactory = new JsonFactory();
////			JsonGenerator w = jfactory.createGenerator(sw);
////			w.setPrettyPrinter(new DefaultPrettyPrinter());
////
////			w.writeStartArray();
////			for (DbxGraphData gd : list)
////			{
////				//String str = om.writeValueAsString(gd);
////				w.writeObject(gd);
////				//w.writeString( str );
////			}
////			w.writeEndArray();
////
////			w.close();
////			payload = sw.toString();
//
//			
//			ObjectMapper om = Helper.createObjectMapper();
//
////			StringBuilder sb = new StringBuilder();
////			sb.append("[ \n");
////			if (list.size() > 0)
////			{
////				Timestamp pts = null;
////    			for (DbxGraphData gd : list)
////    			{
////    				Timestamp ts = gd.getSessionSampleTime();
////    				if ("aaCpuGraph".equals(graphName) && pts != null)
////    					System.out.println(" ------("+Thread.currentThread().getName()+"): curTs='"+ts+"', curTs.getTime()='"+ts.getTime()+"'          pTs='"+pts+"', pTs.getTime()='"+pts.getTime()+"'.");
////    				if (pts != null)
////    				{
////    					if (ts.getTime() < pts.getTime())
////    					{
////    						System.out.println("XXXXXXXXXXXXX: curTs < prevTs: curTs='"+ts+"', prevTs='"+pts+"'.");
////    					}
////    				}
////    				pts = ts;
////    				
////    				String str = om.writeValueAsString(gd);
////    				sb.append(str);
////    				sb.append(", \n");
////    			}
////    			sb.delete(sb.length()-3, sb.length()); // remove last ", \n"
////			}
////			sb.append("]");
////			payload = sb.toString();
//
//		
//			String xxx = "";
//			xxx+="[ \n";
//			if (list.size() > 0)
//			{
//				Timestamp pts = null;
//    			for (DbxGraphData gd : list)
//    			{
//    				Timestamp ts = gd.getSessionSampleTime();
//    				if ("aaCpuGraph".equals(graphName) && pts != null)
//    					System.out.println(" ------("+Thread.currentThread().getName()+"): curTs='"+ts+"', curTs.getTime()='"+ts.getTime()+"'          pTs='"+pts+"', pTs.getTime()='"+pts.getTime()+"'.");
//    				if (pts != null)
//    				{
//    					if (ts.getTime() < pts.getTime())
//    					{
//    						System.out.println("XXXXXXXXXXXXX: curTs < prevTs: curTs='"+ts+"', prevTs='"+pts+"'.");
//    					}
//    				}
//    				pts = ts;
//    				
//    				String str = om.writeValueAsString(gd);
//    				xxx+=str;
//    				xxx+=", \n";
//    			}
//    			xxx = xxx.substring(0, xxx.length()-3);
//			}
//			xxx+="]";
//			payload = xxx;
//		
//		}
//		catch (Exception e)
//		{
//			e.printStackTrace();
//			System.out.println("fireGraphData(): Problems generating JSON.");
//			return;
//		}
//		
//		out.println(payload);
//		
//		out.flush();
//		out.close();
//
//		File toFileName = new File("c:\\tmp\\GraphDataController."+cmName+"."+graphName+".tmp.json");
//		System.out.println("GraphData: Writing JSON to file: "+toFileName.getAbsolutePath());
//		FileUtils.writeStringToFile(toFileName, payload);
//	}
//
//	public static void main(String[] args)
//	{
//		try
//		{
//			Map<String, Double> map = new LinkedHashMap<>();
//
//			map.put("1.0",     new Double(1.0));
//			map.put("1.1",     new Double(1.1));
//			map.put("1.12",    new Double(1.12));
//			map.put("1.123",   new Double(1.123));
//			map.put("1.1234",  new Double(1.1234));
//			map.put("1.12345", new Double(1.12345));
//
//			ObjectMapper om = Helper.createObjectMapper();
//			System.out.println(om.writeValueAsString(map));
//		}
//		catch(Exception ex)
//		{
//			ex.printStackTrace();
//		}
//	}
//}
