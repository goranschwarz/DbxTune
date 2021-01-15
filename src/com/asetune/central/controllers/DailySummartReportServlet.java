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
import java.io.PrintWriter;
import java.lang.invoke.MethodHandles;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.asetune.pcs.MonRecordingInfo;
import com.asetune.pcs.PersistReader;
import com.asetune.pcs.report.DailySummaryReportFactory;
import com.asetune.pcs.report.IDailySummaryReport;
import com.asetune.pcs.report.IProgressReporter;
import com.asetune.pcs.report.IProgressReporter.State;
import com.asetune.pcs.report.content.DailySummaryReportContent;
import com.asetune.pcs.report.content.IReportEntry;
import com.asetune.sql.conn.ConnectionProp;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.StringUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.MediaType;
import com.google.gson.Gson;

public class DailySummartReportServlet extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	private static final Logger _logger = Logger.getLogger(MethodHandles.lookup().lookupClass());

	/** Used as ContentType for Server Sent Events */
	public static final String TEXT_EVENT_STREAM = "text/event-stream";


	private void printHelp(ServletOutputStream out, String msg) 
	throws IOException
	{
		out.println("<html>");
		out.println("<body>");
		if (msg != null)
		{
			out.println("<br>");
			out.println("<b>" + msg + "</b><br>");
		}

		out.println("<br>");
		out.println("Available commands: /api/sdr?op=name<br>");
		
		out.println("<table border=1 cellpadding=1>");
		out.println("  <tr> <th> Operation Name            </th> <th> Description </th> </tr>");
		out.println("  <tr> <td> list                      </td> <td> Get name of Summary items </td> </tr>");
		out.println("  <tr> <td> get                       </td> <td> xxx </td> </tr>");
		out.println("  <tr> <td>    items                  </td> <td> xxx </td> </tr>");
		out.println("  <tr> <td>    begin-time             </td> <td> xxx </td> </tr>");
		out.println("  <tr> <td>    end-time               </td> <td> xxx </td> </tr>");
		out.println("</table>");
		out.println("");

		out.println("</body>");
		out.println("</html>");
		out.flush();
		
		out.close();
		return;
	}
	
	private DbxConnection connect(String  jdbcUrl, String jdbcUser, String  jdbcPass)
	throws Exception
	{
		DbxConnection conn = null;

		String dbxCollector = null;
		String reportSrvName  = null;

		if (jdbcPass == null)
			jdbcPass = "";

		ConnectionProp cp = new ConnectionProp();
		cp.setUrl(jdbcUrl);
		cp.setUsername(jdbcUser);
		cp.setPassword(jdbcPass);

		conn = DbxConnection.connect(null, cp);

		// - Check if it's a PCS database
		// - get DbxTune TYPE (AseTune, RsTune, SqlServerTune, etc...)
		// - get Server Name
		// - get start/first sample Timestamp
		// - get   end/last  sample Timestamp
		if ( ! PersistReader.isOfflineDb(conn) )
			throw new Exception("This do NOT look like a DbxTune recording... can't continue.");

		MonRecordingInfo monRecordingInfo = PersistReader.getMonRecordingInfo(conn, null);
//		MonVersionInfo   monVersionInfo   = PersistReader.getMonVersionInfo(conn, null);

		dbxCollector  = monRecordingInfo.getRecDbxAppName();
		reportSrvName = monRecordingInfo.getDbmsServerName();

		return conn;
	}

//	@Override
//	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
//	{
//		String currentUsername = "-no-principal-";
//
//		Principal principal = req.getUserPrincipal();
//		if (principal != null)
//			currentUsername = principal.getName();
//
//		ServletOutputStream out = resp.getOutputStream();
//		resp.setContentType("text/html");
//		resp.setCharacterEncoding("UTF-8");
////		resp.setContentType("application/json");
////		resp.setCharacterEncoding("UTF-8");
//		out.println("Hello Dummy Servlet: currentUsername='"+currentUsername+"', getRemoteUser()='"+req.getRemoteUser()+"'.");
//		out.flush();
//		out.close();
//	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
	throws ServletException, IOException
	{
//		ServletOutputStream out = resp.getOutputStream();
//		resp.setContentType("text/html");
//		resp.setCharacterEncoding("UTF-8");
//		resp.setContentType("application/json");
//		resp.setCharacterEncoding("UTF-8");

		String inputOp   = req.getParameter("op");
//		String inputName = req.getParameter("name");
//		
//		_logger.info("input: op = '"+inputOp+"'.");
//
//		if (StringUtil.isNullOrBlank(inputOp))
//			throw new ServletException("No input parameter named 'op'.");

		if ("help".equals(inputOp))
		{
			resp.setContentType("text/html");
			resp.setCharacterEncoding("UTF-8");

			ServletOutputStream out = resp.getOutputStream();
			printHelp(out, null);
			return;
		}
		else if ("list".equals(inputOp))
		{
//			resp.setContentType("text/html");
			resp.setContentType("application/json");
			resp.setCharacterEncoding("UTF-8");
			
			String jdbcUrl  = AdminServlet.getParameter(req, "dbname",   "").trim();
			String jdbcUser = AdminServlet.getParameter(req, "username", "sa").trim();
			String jdbcPass = AdminServlet.getParameter(req, "password", "").trim();

			try (IDailySummaryReport report = createReport(resp, jdbcUrl, jdbcUser, jdbcPass))
			{

				Map<String, String> reportNames = new LinkedHashMap<>();
				for (IReportEntry entry : report.getReportEntries())
				{
					reportNames.put(entry.getClass().getSimpleName(), entry.getSubject());
				}
				//return reportNames;

				String payload = new Gson().toJson(reportNames);

				ServletOutputStream out = resp.getOutputStream();
				out.println(payload);

				out.flush();
				out.close();				
			}
			catch (Exception ex)
			{
				resp.sendError(500, ex.getMessage());
			}
			return;
		}
		else if ("get".equals(inputOp))
		{
			try
			{
				resp.setContentType(TEXT_EVENT_STREAM); // "text/event-stream";
				resp.setCharacterEncoding("UTF-8");

				String jdbcUrl      = AdminServlet.getParameter(req, "dbname",    "")  .trim();
				String jdbcUser     = AdminServlet.getParameter(req, "username",  "sa").trim();
				String jdbcPass     = AdminServlet.getParameter(req, "password",  "")  .trim();
				String beginTimeStr = AdminServlet.getParameter(req, "beginTime", "")  .trim();
				String endTimeStr   = AdminServlet.getParameter(req, "endTime",   "")  .trim();

				LocalTime beginTime = null;
				LocalTime endTime   = null;
				if (StringUtil.hasValue(beginTimeStr)) beginTime = LocalTime.parse(beginTimeStr);
				if (StringUtil.hasValue(  endTimeStr))   endTime = LocalTime.parse(endTimeStr);

				try (IDailySummaryReport report = createReport(resp, jdbcUrl, jdbcUser, jdbcPass))
				{
					// Set a "Report Progress" object
					report.setProgressReporter(new IProgressReporter()
					{
						@Override
						public boolean setProgress(IProgressReporter.State state, IReportEntry entry, String msg, int guessedPercentDone)
						{
							String prefix = "";
							if (IProgressReporter.State.BEFORE.equals(state)) prefix = "Create: ";
							if (IProgressReporter.State.AFTER .equals(state)) prefix = "Done: ";

//							System.out.println("DailySummaryReport.setProgress(status='"+msg+"', guessedPercentDone="+guessedPercentDone+")");
							msg = prefix + entry.getClass().getSimpleName() + " --- " + entry.getSubject();

							try
							{
								Map<String, Object> params = new HashMap<>();
								params.put("state"       , state.toString());
								params.put("progressText", msg);
								params.put("percentDone" , guessedPercentDone);
								String json = new ObjectMapper().writeValueAsString(params);

								writeSseEvent(resp, "progress", json);
							}
							catch (Exception ex)
							{
								_logger.error("Problems sending 'status' request via Server Sent Events", ex);
							}
							
							return true;
						}
					});

					if (beginTime != null) report.setReportPeriodBeginTime(beginTime.getHour(), beginTime.getMinute());
					if (  endTime != null) report.setReportPeriodEndTime  (  endTime.getHour(),   endTime.getMinute());

					// Create & and Send the report
					report.create();

					// TODO: Change the below "in some way"
					// - current usage take to much memory: getReportAsHtml() -> serialize-to-JSON-string -> writeSseEvent()
					// - wanted: content.getReportFile() -> makeItIntoJson -> writeSseEvent(using-a-Writer-so-we-do-not-have-a-intermediate-string)

					// Get Content and the HTML output
					DailySummaryReportContent content = report.getReportContent();
					String htmlReport = content.getReportAsHtml();

					// Construct "complete" message
					Map<String, Object> params = new HashMap<>();
					params.put("complete", htmlReport);
					String completeJson = new ObjectMapper().writeValueAsString(params);

					// Send "complete" message
					writeSseEvent(resp, "complete", completeJson);

					PrintWriter writer = resp.getWriter();
					writer.flush();
					writer.close();
				} // end: auto-close
			}
			catch (Exception ex)
			{
				_logger.error("Problems doGet(): op='" + inputOp + "', message=" + ex.getMessage(), ex);

				// Send "complete" message
				writeSseEvent(resp, "error", ex.getMessage());

				// sendError do not work...
				//resp.sendError(500, ex.getMessage());
			}
			return;
		}
		else if ("getConfig".equals(inputOp))
		{
		}
		else if ("setConfig".equals(inputOp))
		{
		}
		else 
		{
			resp.setContentType("text/html");
//			resp.setContentType("application/json");
			resp.setCharacterEncoding("UTF-8");

			ServletOutputStream out = resp.getOutputStream();
			printHelp(out, "No operation named '"+inputOp+"'.");
			return;
			//throw new ServletException("No operation named '"+inputOp+"'.");
		}
		
//		out.println("Not yet implemented...");
//		out.flush();
//		out.close();
	}

	private IDailySummaryReport createReport(HttpServletResponse resp, String jdbcUrl, String jdbcUser, String jdbcPass)
	throws Exception
	{
//		if (StringUtil.isNullOrBlank(jdbcUrl))
//		{
//			String filename = "SYB_SEK_MXGT60_2020-10-01.mv.db";
//			
//			String H2DB_FILE_TEMPLATE = "jdbc:h2:file:${filename};IFEXISTS=TRUE";
//			String tmpUrl = H2DB_FILE_TEMPLATE.replace("${filename}", filename.replace(".mv.db", ""));
//
//			jdbcUrl = tmpUrl;
//		}
		// If it's a FILENAME, then see it as a H2 dbfile
		if (StringUtil.hasValue(jdbcUrl) && !jdbcUrl.startsWith("jdbc:"))
		{
			String filename = jdbcUrl;
			
			String H2DB_FILE_TEMPLATE = "jdbc:h2:file:${filename};IFEXISTS=TRUE";
			String tmpUrl = H2DB_FILE_TEMPLATE.replace("${filename}", filename.replace(".mv.db", ""));

			jdbcUrl = tmpUrl;
		}
		
		// Send progress (if we are connected with a Server Sent Event stream */
		if (resp.getContentType() != null && resp.getContentType().indexOf(TEXT_EVENT_STREAM) != -1)
		{
			Map<String, Object> params = new HashMap<>();
			params.put("state"       , State.BEFORE.toString());
			params.put("progressText", "Connecting to URL: " + jdbcUrl);
			params.put("percentDone" , 0);
			String json = new ObjectMapper().writeValueAsString(params);

			// Send "complete" message
			writeSseEvent(resp, "progress", json);			
		}
		
		DbxConnection conn = connect(jdbcUrl, jdbcUser, jdbcPass);
		
		MonRecordingInfo monRecordingInfo = PersistReader.getMonRecordingInfo(conn, null);
//		MonVersionInfo   monVersionInfo   = PersistReader.getMonVersionInfo(conn, null);

		String dbxCollector  = monRecordingInfo.getRecDbxAppName();
		String reportSrvName = monRecordingInfo.getDbmsServerName();

		System.getProperties().setProperty(DailySummaryReportFactory.PROPKEY_reportClassname, "com.asetune.pcs.report.DailySummaryReport" + dbxCollector);
		System.getProperties().setProperty(DailySummaryReportFactory.PROPKEY_senderClassname, "com.asetune.pcs.report.senders.ReportSenderNoOp");

		IDailySummaryReport report = DailySummaryReportFactory.createDailySummaryReport();
		if (report == null)
		{
//			System.out.println("Daily Summary Report: create did not pass a valid report instance, skipping report creation.");
//			return;
			throw new Exception("Daily Summary Report: create did not pass a valid report instance, skipping report creation.");
		}
		
		
		report.setConnection(conn);
		report.setServerName(reportSrvName);

//		if (_params._beginTimeHour > 0 || _params._beginTimeMinute > 0) report.setReportPeriodBeginTime(_params._beginTimeHour, _params._beginTimeMinute);
//		if (_params._endTimeHour   > 0 || _params._endTimeMinute   > 0) report.setReportPeriodEndTime  (_params._endTimeHour  , _params._endTimeMinute);

		// Initialize the Report, which also initialized the ReportSender
		report.init();

		return report;
	}

	/**
	 * Write a single server-sent event to the response stream for the given event and message.
	 * 
	 * @param resp     The response to write to
	 * @param event    The event to write
	 * @param data     The message data to include
	 * 
	 * @throws IOException  If an I/O error occurs
	 */
	protected void writeSseEvent(HttpServletResponse resp, String event, String data)
	throws IOException 
	{
		// get the writer to send text responses
		PrintWriter writer = resp.getWriter();

		// write the event type (make sure to include the double newline)
//		writer.write("event: " + event + "\n\n");
		writer.write("event: " + event + "\n");
//System.out.println("SSE-EVENT: " + event);

		// write the actual data
		// this could be simple text or could be JSON-encoded text that the client then decodes
		writer.write("data: " + data + "\n\n");
//System.out.println("SSE-DATA: " + message);

		// flush the buffers to make sure the container sends the bytes
		writer.flush();
		resp.flushBuffer();
	}
}
