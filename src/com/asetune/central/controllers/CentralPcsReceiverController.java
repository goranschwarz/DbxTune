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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.asetune.central.DbxCentralStatistics;
import com.asetune.central.DbxCentralStatistics.ServerEntry;
import com.asetune.central.check.ReceiverAlarmCheck;
import com.asetune.central.pcs.CentralPcsWriterHandler;
import com.asetune.central.pcs.DbxTuneSample;
import com.asetune.central.pcs.DbxTuneSample.CmEntry;
import com.asetune.central.pcs.DbxTuneSample.MissingFieldException;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;


//@RestController
public class CentralPcsReceiverController
extends HttpServlet
{
	private static final long serialVersionUID = 1L;
//	private static final Logger _logger = LoggerFactory.getLogger(CentralPcsReceiverController.class);
	private static final Logger _logger = Logger.getLogger(MethodHandles.lookup().lookupClass());

//	private final Map<String, SseEmitter> sseMap = new ConcurrentHashMap<>();
	
	public static final String PROPKEY_HOSTS_ALLOWED = "CentralPcsReceiver.hosts.allowed";
	public static final String DEFAULT_HOSTS_ALLOWED = "";

	public static String getBody(HttpServletRequest request) throws IOException
	{
		StringBuilder sb = new StringBuilder();
		BufferedReader br = null;

		try
		{
			InputStream inputStream = request.getInputStream();
			if ( inputStream != null )
			{
				br = new BufferedReader(new InputStreamReader(inputStream));
				char[] charBuffer = new char[128];
				int bytesRead = -1;
				while ((bytesRead = br.read(charBuffer)) > 0)
					sb.append(charBuffer, 0, bytesRead);
			}
			else
				sb.append("");
		}
		catch (IOException ex)
		{
			throw ex;
		}
		finally
		{
			if ( br != null )
			{
				try { br.close(); }
				catch (IOException ex) { throw ex; }
			}
		}

		return sb.toString();
	}

	// curl -X POST -d @/mnt/c/tmp/PersistWriterToHttpJson.tmp.json http://localhost:8080/api/pcs/receiver
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		String remoteHost = req.getRemoteHost();
		String remoteAddr = req.getRemoteAddr();
		int    remotePort = req.getRemotePort();
		String remoteUser = req.getRemoteUser();

		if (_logger.isDebugEnabled())
			_logger.debug("/api/pcs/receiver: received request from: remoteHost='"+remoteHost+"', remoteAddr='"+remoteAddr+"', remotePort='"+remotePort+"', remoteUser='"+remoteUser+"'");

		// Check if the remote host is allowed to send data.
		String allowedHosts = Configuration.getCombinedConfiguration().getProperty(PROPKEY_HOSTS_ALLOWED, DEFAULT_HOSTS_ALLOWED);
		if (StringUtil.hasValue(allowedHosts))
		{
			List<String> allowedHostList = StringUtil.commaStrToList(allowedHosts);

			// Check all entries in the allowedHostList using regex 
			boolean isAllowed = false;
			for (String regex : allowedHostList)
			{
				if (remoteAddr.matches(regex))
					isAllowed = true;
			}
			// If no match, then do NOT allow the hosts to enter data
			if ( ! isAllowed )
			{
				_logger.warn("The hostname '"+remoteHost+"' is NOT allowed to send Performance Counter Data. allowedHostList="+allowedHostList);
				resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "The hostname '"+remoteHost+"' is NOT allowed to send Performance Counter Data.");
				return;
				//throw new ServletException("The hostname '"+remoteHost+"' is NOT allowed to send Performance Counter Data.");
			}
		}

		// Get the JSON String
		String payload = getBody(req);
		
		// Parse the JSON String and send it to Central PCS
		try
		{
			parseJsonAndSaveToCentralPcs( payload );
		}
		catch (MissingFieldException ex) 
		{
			throw new ServletException(ex.getMessage(), ex);
		}
		
		
		// get the queue size and return that to the caller
		// This so caller can determine if it should wait until it sends next (so that it do not overload the PCS)
		if ( CentralPcsWriterHandler.hasInstance() )
		{
			int pcsQueueSize = CentralPcsWriterHandler.getInstance().getQueueSize();

			ServletOutputStream out = resp.getOutputStream();
			out.print("{ \"queueSize\": "+pcsQueueSize+" }");
			out.flush();
			out.close();
		}
	}

	/**
	 * Send a Server Sent Event to any clients that has registered on: /api/pcs/graph-data/{name}
	 * @param sample 
	 * @param payload what to send
	 * @param name 
	 */
	private static void pcsAdd(DbxTuneSample sample)
	{
		if ( ! CentralPcsWriterHandler.hasInstance() )
		{
			String srvName = sample == null ? "-sample-is-null-" : sample.getServerName();
			_logger.info("Trying to add sample for '"+srvName+"' to CentralPcsWriterHandler. But it has NO instance... Skipping pcsAdd(sample);");
			return;
		}

		// Add it to PCS for storage
		CentralPcsWriterHandler.getInstance().add(sample);

		// Increment some statistics
		String      srvName = sample.getServerName();
		ServerEntry srvStat = DbxCentralStatistics.getInstance().getServerEntry(srvName, sample);

		srvStat.incReceiveCount();
		srvStat.setLastSampleTime();
		
		for (CmEntry cmEntry : sample.getCollectors())
		{
			srvStat.incReceiveGraphCount(cmEntry.getGraphMap().size());
		}
	}

	/**
	 * Parse JSON String into a DbxTuneSample Object and post that to CentralPcsWriterHandler, which writes it to all PCS Writers
	 * @param jsonPayload
	 * @throws ServletException 
	 */
	public static void parseJsonAndSaveToCentralPcs(String jsonPayload) 
	throws MissingFieldException
	{
		DbxTuneSample sample = null;
		try
		{
			sample = DbxTuneSample.parseJson(jsonPayload);
		}
//		catch(JsonProcessingException ex) { ex.printStackTrace(); }
//		catch(IOException ex) { ex.printStackTrace(); }
		catch(IOException ex) 
		{
			_logger.error("Problem parsing incoming JSON message for DbxTuneSample", ex); 
		}
		
		// FIXME: send some error
		// sample can be null, if serverName is "unknown"
		if (sample == null)
			return;

		if (_logger.isDebugEnabled())
		{
			StringBuilder sb = new StringBuilder();
			
			sb.append("### The Sample has: "+sample.getCollectors().size()+" collector entries. sessionStartTime="+sample.getSessionStartTime()+", sessionSampleTime="+sample.getSessionSampleTime()+", serverName="+sample.getServerName()+", onHostname="+sample.getOnHostname()+".\n");
			for (CmEntry cmEntry : sample.getCollectors())
			{
				sb.append(String.format("    -- The CmEntry %s has: %2d Graph entries, %5d Abs rows, %5d Diff rows, %5d Rate rows. \n"
						, StringUtil.left(cmEntry.getName(), 30, "'")
						, cmEntry.getGraphMap()    .size()
						, cmEntry.getAbsCounters() .getRowCount()
						, cmEntry.getDiffCounters().getRowCount()
						, cmEntry.getRateCounters().getRowCount()
						));
			}
			_logger.debug(sb.toString());
		}
		
		// Maintain alarm functionality
		// Just notifies the 'ReceiverAlarmCheck' that we have received data from a specific collector 
		// The 'ReceiverAlarmCheck' periodically checks if we havn't received data from a specific server/instance in a "while", then do: alarm
		if (ReceiverAlarmCheck.hasInstance())
		{
			ReceiverAlarmCheck.getInstance().receivedData(sample);
		}

		// Send "live" data to any web "subscribers", so the subscribing browsers can be updated there graphs/charts
//		ChartBroadcastServlet.fireGraphData(sample);
		ChartBroadcastWebSocket.fireGraphData(sample);
		
		// Finally add the data to the Writer, which will store the data "somewhere" probably in a DB
		pcsAdd(sample);
		
	}
}