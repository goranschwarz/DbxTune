package com.asetune.central.controllers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.asetune.central.DbxCentralStatistics;
import com.asetune.central.DbxCentralStatistics.ServerEntry;
import com.asetune.central.pcs.CentralPcsWriterHandler;
import com.asetune.central.pcs.DbxTuneSample;
import com.asetune.central.pcs.DbxTuneSample.CmEntry;
import com.asetune.utils.StringUtil;
import com.fasterxml.jackson.core.JsonProcessingException;


//@RestController
public class CentralPcsReceiverController
extends HttpServlet
{
	private static final long serialVersionUID = 1L;
//	private static final Logger _logger = LoggerFactory.getLogger(CentralPcsReceiverController.class);
	private static final Logger _logger = Logger.getLogger(MethodHandles.lookup().lookupClass());

//	private final Map<String, SseEmitter> sseMap = new ConcurrentHashMap<>();

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
		String payload = getBody(req);

		// TODO Auto-generated method stub
//		super.doPost(req, resp);
//		logger.info("2222222: /api/pcs/receiver: payload=NOT-PRINTED");

		DbxTuneSample sample = null;
		try
		{
			sample = DbxTuneSample.parseJson(payload);
		}
		catch(JsonProcessingException ex) { ex.printStackTrace(); }
		catch(IOException ex) { ex.printStackTrace(); }
		
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

		// Send "live" data to any web "subscribers", so the subscibing browsers can be updated there graphs/charts
//		ChartBroadcastServlet.fireGraphData(sample);
		ChartBroadcastWebSocket.fireGraphData(sample);
		
		// Finally add the data to the Writer, which will store the data "somewhere" probably in a DB
		pcsAdd(sample);
		
		// get the queue size and return that to the caller
		// This so caller can determen if it should wait untill it sends next (so that it do not overload the PCS)
		
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
	private void pcsAdd(DbxTuneSample sample)
	{
		if ( ! CentralPcsWriterHandler.hasInstance() )
		{
			String srvName = sample == null ? "-sample-is-null-" : sample.getServerName();
			_logger.info("Trying to add sample for '"+srvName+"' to CentralPcsWriterHandler. But it has NO instance... Skipping pcsAdd(sample);");
			return;
		}

		// Add it to PCS for storage
		CentralPcsWriterHandler.getInstance().add(sample);

		// Increement some statistics
		String      srvName = sample.getServerName();
		ServerEntry srvStat = DbxCentralStatistics.getInstance().getServerEntry(srvName);

		srvStat.incReceiveCount();
		
		for (CmEntry cmEntry : sample.getCollectors())
		{
			srvStat.incReceiveGraphCount(cmEntry.getGraphMap().size());
		}
	}

//	/**
//	 * Send a Server Sent Event to any clients that has registered on: /api/pcs/graph-data/{name}
//	 * @param payload what to send
//	 * @param name 
//	 */
//	private void fireGraphData(String name, String payload)
//	{
//		ChartBroadcastServlet.fireGraphData(name, payload);
////		logger.info("fireGraphData: sseMap.size() = " + sseMap.size());
////		sseMap.forEach( (key, sse) -> 
////		{
////			try
////			{
////				sse.send(payload);
////			}
////			catch (Throwable e)
////			{
////				e.printStackTrace();
////				logger.info("Removing key='"+key+"' from client map.");
////				sseMap.remove(key);
////			}
////		});
//	}
//java.lang.IllegalStateException: ResponseBodyEmitter is already set complete

	// CLIENT: curl http://localhost:8080/api/pcs/graph-data/gorans
//	@GetMapping("/api/pcs/graph-data/{name}")
//	public SseEmitter graphData(@PathVariable String name)
//	{
////		SseEmitter sseEmitter = new SseEmitter();
//		final SseEmitter sseEmitter = new SseEmitter(60*60*1000L); // live for 1 hour
////		final SseEmitter sseEmitter = new SseEmitter(10*1000L);
//
//		final String uuid = UUID.randomUUID().toString();
//		sseEmitter.onTimeout(    () -> { logger.info("sseEmitter.onTimeout()    sseMap.remove('"+uuid+"') returned=" + sseMap.remove(uuid)); } );
////		sseEmitter.onCompletion( () -> { logger.info("sseEmitter.onCompletion() sseMap.remove('"+uuid+"') returned=" + sseMap.remove(uuid)); sseEmitter.complete(); } );
//		
//		sseMap.put(uuid, sseEmitter);
//		logger.info("/api/pcs/graph-data/"+name+" uuid="+uuid+", sseMap.size()="+sseMap.size());
//		
//		return sseEmitter;
//	}

//	@RequestMapping(value="/api/pcs/receiveAsJson", method=RequestMethod.POST)
//	public void receive(@RequestBody Map<String, Object> payload)
//	{
////		System.out.println("111111: /api/pcs/receive: payload="+payload);
//		logger.info("payload="+payload);
//		
//		for (String key : payload.keySet())
//		{
//			Object val = payload.get(key);
//			System.out.println("111111: /api/pcs/receiveAsJson: key-val: NOT-PRINTED");
////			System.out.println("KEY='"+key+"', VAL='"+val+"', valueType="+val.getClass().getName());
//		}
//	}
	
// time curl -H "Accept: application/json" -H "Content-type: text/plain"       -X POST -d @/mnt/c/tmp/PersistWriterToHttpJson.tmp.json http://localhost:8080/api/pcs/receive
// http://localhost:8080/graph_subscribe.html
// curl http://localhost:8080/api/pcs/graph-data/xxxx
//	@RequestMapping(value = "/api/pcs/receive", method = RequestMethod.POST, consumes = "text/plain")
//	public void receive2(@RequestBody String payload)
//	{
//		logger.info("2222222: /api/pcs/receive: payload=NOT-PRINTED");
//
//		DbxTuneSample sample = null;
//		try
//		{
//			sample = DbxTuneSample.parseJson(payload);
//		}
//		catch(JsonProcessingException ex) { ex.printStackTrace(); }
//		catch(IOException ex) { ex.printStackTrace(); }
//		
//		// FIXME: send some error
//		if (sample == null)
//			return;
//
//		System.out.println("### The Sample has: "+sample.getCollectors().size()+" collector entries. sessionStartTime="+sample.getSessionStartTime()+", sessionSampleTime="+sample.getSessionSampleTime()+", serverName="+sample.getServerName()+", onHostname="+sample.getOnHostname()+".");
//		for (CmEntry cmEntry : sample.getCollectors())
//		{
//			System.out.println("    -- The CmEntry '"+cmEntry.getName()+"' has: "
//					+ cmEntry.getGraphMap()    .size()      +" Graph entries, "
//					+ cmEntry.getAbsCounters() .getRowCount()+" Abs rows, "
//					+ cmEntry.getDiffCounters().getRowCount()+" Diff rows, "
//					+ cmEntry.getRateCounters().getRowCount()+" Rate rows, "
//					);
//			for (GraphEntry graphEntry : cmEntry.getGraphMap().values())
//			{
//				if ("aaCpuGraph".equals(graphEntry.getName()))
//					fireGraphData(graphEntry.getOriginJsonStr());
//			}
//		}
//		
//		// Finally add the data to the Writer, which will store the data "somewhere" probably in a DB
//		if (CentralPcsWriterHandler.hasInstance())
//		{
//			CentralPcsWriterHandler writer = CentralPcsWriterHandler.getInstance();
//			writer.add(sample);
//		}
//	}
}


//curl -H "Accept: application/json" -H "Content-type: application/json" -X POST -d '{"name":"value"}' http://localhost:8080/receive
// curl -H "Accept: application/json" -H "Content-type: application/json" -X POST -d '{"name":"value"}' http://localhost:8080/api/pcs/receive

// curl -H "Accept: application/json" -H "Content-type: text/plain"       -X POST -d '{"fname":"goran"}' http://localhost:8080/api/pcs/receive2