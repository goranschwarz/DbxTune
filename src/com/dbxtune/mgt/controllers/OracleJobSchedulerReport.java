/*******************************************************************************
 * Copyright (C) 2010-2027 Goran Schwarz
 *
 * This file is part of DbxTune
 * DbxTune is a family of sub-products *Alarm-Coverage*, *## Batch-Coverage*
 *                                      *Error-Tracking* and *Report-Handling*
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
package com.dbxtune.mgt.controllers;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.invoke.MethodHandles;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.CounterController;
import com.dbxtune.Version;
import com.dbxtune.central.controllers.HtmlStatic;
import com.dbxtune.central.controllers.HtmlStatic.PageSection;
import com.dbxtune.pcs.MonRecordingInfo;
import com.dbxtune.pcs.OracleJobSchedulerExtractor;
import com.dbxtune.pcs.PersistReader;
import com.dbxtune.pcs.PersistentCounterHandler;
import com.dbxtune.pcs.report.DailySummaryReportSimple;
import com.dbxtune.pcs.report.content.oracle.OracleJobScheduler;
import com.dbxtune.pcs.report.content.oracle.OracleLoadOverview;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;

/**
 * "Scheduler Report for Today": the section 'OracleJobScheduler' of the Daily Summary Report, made NOW of the recording of today.
 * <p>
 * The same thing as SqlServerJobSchedulerReport does for SQL Server: first the log of the Scheduler is copied from the database into
 * the recording (OracleJobSchedulerExtractor: the tables are dropped and made again), then the report is written to the caller.
 */
public class OracleJobSchedulerReport
extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		resp.setContentType("text/html");
		resp.setCharacterEncoding("UTF-8");

		// Exit early if: No persistence Handler
		if ( ! PersistentCounterHandler.hasInstance() )
		{
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "No Persistent Counter Handler was found.");
			return;
		}

		// Exit early if: Counter Controller
		if ( ! CounterController.hasInstance() )
		{
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "No Counter Controller was found.");
			return;
		}

		// Exit early if: No PCS Storage Connection
		if ( PersistentCounterHandler.getInstance().getPersistWriterJdbc() == null )
		{
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "No JDBC Persistent Writer was found.");
			return;
		}

		DbxConnection pcsStorageConn = null;
		DbxConnection monConn        = null;
		try
		{
			String appName = Version.getAppName() + "-JobSchedulerReport";

			// Connections of our own, to the database and to the recording: the collector goes on sampling with its connections meanwhile
			_logger.info("Connecting to the Oracle database and to the PCS with appName '" + appName + "'.");
			monConn        = CounterController.getInstance().cloneMonConnection(appName);
			pcsStorageConn = PersistentCounterHandler.getInstance().getPersistWriterJdbc().cloneStorageConnection(appName);

			// Copy the log of the Scheduler into the recording
			int daysToCopy = Configuration.getCombinedConfiguration().getIntProperty(OracleJobSchedulerExtractor.PROPKEY_daysToCopy, OracleJobSchedulerExtractor.DEFAULT_daysToCopy);
			new OracleJobSchedulerExtractor(daysToCopy, monConn, pcsStorageConn).transfer();
		}
		catch (Exception ex)
		{
			closeConnections(monConn, pcsStorageConn);

			String msg = "Problems getting the log of the Oracle Scheduler into the recording. Caught: " + ex;
			if (ex instanceof SQLException)
				msg = "Problems getting the log of the Oracle Scheduler into the recording. ErrorCode=" + ((SQLException) ex).getErrorCode() + ", SqlState='" + ((SQLException) ex).getSQLState() + "', Message|" + ex.getMessage() + "|.";

			_logger.error(msg, ex);
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
			return;
		}

		// The report is written to 'writer', which is the response
		PrintWriter writer = resp.getWriter();
		createReport(writer, pcsStorageConn);

		// 'report.close()' closed the connection to the recording
		closeConnections(monConn, null);

		writer.flush();
		writer.close();
	}

	private void closeConnections(DbxConnection monConn, DbxConnection pcsStorageConn)
	{
		if (monConn != null)
			monConn.closeNoThrow();

		if (pcsStorageConn != null)
			pcsStorageConn.closeNoThrow();
	}

	private void createReport(Writer writer, DbxConnection pcsStorageConn)
	{
		MonRecordingInfo monRecordingInfo = PersistReader.getMonRecordingInfo(pcsStorageConn, null);

		DailySummaryReportSimple report = new DailySummaryReportSimple()
		{
			@Override
			public void addReportEntries()
			{
				addReportEntry( new OracleLoadOverview(this).withCollapsedHeader(true) );
				addReportEntry( new OracleJobScheduler(this) );
			}

			@Override
			public void createHtmlNavbar(Writer writer)
			throws IOException
			{
				writer.append(HtmlStatic.getHtmlNavbar(PageSection.None, null, true));
				writer.append("<br>");
			}
		};
		report.setWriter(writer);
		report.setConnection(pcsStorageConn);
		report.setServerName(CounterController.getInstance().getServerName() != null ? CounterController.getInstance().getServerName() : monRecordingInfo.getDbmsServerName());

		try
		{
			report.init();
			report.create();
			report.close(); // Note: this closes the underlying connection
		}
		catch (Exception ex)
		{
			_logger.error("Problems Creating Job Scheduler Report.", ex);
		}
	}
}
