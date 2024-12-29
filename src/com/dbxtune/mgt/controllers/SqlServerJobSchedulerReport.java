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
package com.dbxtune.mgt.controllers;

import java.awt.Desktop;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.invoke.MethodHandles;
import java.sql.SQLException;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.dbxtune.CounterController;
import com.dbxtune.Version;
import com.dbxtune.central.controllers.HtmlStatic;
import com.dbxtune.central.controllers.HtmlStatic.PageSection;
import com.dbxtune.pcs.MonRecordingInfo;
import com.dbxtune.pcs.PersistReader;
import com.dbxtune.pcs.PersistentCounterHandler;
import com.dbxtune.pcs.SqlServerJobSchedulerExtractor;
import com.dbxtune.pcs.report.DailySummaryReportSimple;
import com.dbxtune.pcs.report.content.sqlserver.SqlServerCpuUsageOverview;
import com.dbxtune.pcs.report.content.sqlserver.SqlServerJobScheduler;
import com.dbxtune.sql.conn.ConnectionProp;
import com.dbxtune.sql.conn.DbxConnection;

public class SqlServerJobSchedulerReport
extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	private static final Logger _logger = Logger.getLogger(MethodHandles.lookup().lookupClass());

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		resp.setContentType("text/html");
//		resp.setContentType("application/json");
		resp.setCharacterEncoding("UTF-8");

//		ServletOutputStream out = resp.getOutputStream();
		PrintWriter writer = resp.getWriter();

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
		DbxConnection monConn = null;
		try
		{
			String appName = Version.getAppName() + "-JobSchedulerReport";

			// Create a Connection to the Source DBMS (SQL Server)
			_logger.info("Connectiong to Source DBMS with appName '" + appName + "'.");
			monConn        = CounterController.getInstance().cloneMonConnection(appName);

			// Create a Connection to the PCS
			_logger.info("Connectiong to PCS with appName '" + appName + "'.");
			pcsStorageConn = PersistentCounterHandler.getInstance().getPersistWriterJdbc().cloneStorageConnection(appName);
		}
		catch (SQLException ex) 
		{
			closeConnections(monConn, pcsStorageConn);

			String msg = "Problems Creating a Connecting to PCS or Source DBMS. ErrorCode=" + ex.getErrorCode() + ", SqlSate='" + ex.getSQLState() + "', Message|" + ex.getMessage() + "|. Caught: " + ex;
			_logger.error(msg, ex);
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
		}
		catch (Exception ex) 
		{
			closeConnections(monConn, pcsStorageConn);

			String msg = "Problems Creating a Connecting to PCS or Source DBMS. Caught: " + ex;
			_logger.error(msg, ex);
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
		}
		

		// Extract Job Scheduler Info and Store it in the PCS (old JobSched tables will be dropped/recreated)
		// NOTE: Should we "notify" is some way the collector that we are using the connection?
		//       Or should we look at if the collector is in "refresh" or not...
		try
		{
			_logger.info("Extracting Information from SQL Server to local PCS storage.");
			transferJobScheduler(0, monConn, pcsStorageConn);
		}
		catch (SQLException ex)
		{
			closeConnections(monConn, pcsStorageConn);

			String msg = "Problems extracting SQL Server Job Scheduler information. ErrorCode=" + ex.getErrorCode() + ", SqlSate='" + ex.getSQLState() + "', Message|" + ex.getMessage() + "|.";
			_logger.error(msg, ex);
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
		}
		
		// Creating Job Scheduler Report, the content will be written to 'writer' which is grabbed from the HttpServletResponse
		createReport(writer, pcsStorageConn);
		
		// Finally close connections if not already done
		closeConnections(monConn, pcsStorageConn);
		
		writer.flush();
		writer.close();
	}
	
	private void closeConnections(DbxConnection monConn, DbxConnection pcsStorageConn)
	{
		if (monConn != null)
		{
			_logger.info("Closing Connection to Source SQL Server");
			monConn.closeNoThrow();
		}
		
		if (pcsStorageConn != null)
		{
			_logger.info("Closing Connection to PCS - Persistent Counter Storage");
			pcsStorageConn.closeNoThrow();
		}
	}
	
	private void transferJobScheduler(int daysToCopy, DbxConnection monConn, DbxConnection pcsStorageConn) 
	throws SQLException
	{
		SqlServerJobSchedulerExtractor jse = new SqlServerJobSchedulerExtractor(daysToCopy, monConn, pcsStorageConn);
		jse.transfer();
	}

	private void createReport(Writer writer, DbxConnection pcsStorageConn)
	{
		MonRecordingInfo monRecordingInfo = PersistReader.getMonRecordingInfo(pcsStorageConn, null);
//		MonVersionInfo   monVersionInfo   = PersistReader.getMonVersionInfo(pcsStorageConn, null);

//		String dbxCollector  = monRecordingInfo.getRecDbxAppName();
		String reportSrvName = monRecordingInfo.getDbmsServerName();
		
		// Lookup srvAlias for this name
		// FIXME: Not sure how to do this right now
		//        So for a ugly workaround FOR NOW, just replace '\' wit '__'
		// ONE WAY TO FIX IT: Write @@servername in the file "/home/sybase/.dbxtune/info/GS-1-WIN__SS_2016.dbxtune" as 'dbxtune.dbms.srvName' instead of current value which is 'gs-1-win:1433' (possibly change the current key 'dbxtune.dbms.srvName' to 'dbxtune.dbms.physSrvName')
		//        Then we can lookup all the "info files" (but we need to write this info somewhere also possibly in CmSummary ... 
		
		if (reportSrvName.contains("\\"))
			reportSrvName = reportSrvName.replace("\\", "__");

		DailySummaryReportSimple report = new DailySummaryReportSimple()
		{
			@Override
			public void addReportEntries()
			{
				addReportEntry( new SqlServerCpuUsageOverview(this).withCollapsedHeader(true) );
				addReportEntry( new SqlServerJobScheduler(this) );
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
		report.setServerName(reportSrvName);

		try {
			report.init();
			
			// Create & and Send the report
			report.create();
			
			report.close(); // Note: this closes the underlying connection
		} 
		catch (Exception ex) 
		{
			_logger.error("Problems Creating Job Scheduler Report.", ex);
		}
	}
	
	
	/**
	 * The below was used during development for simple testing...
	 * @param args
	 */
	public static void main(String[] args)
	{
		Properties log4jProps = new Properties();
		log4jProps.setProperty("log4j.rootLogger", "INFO, A1");
//		log4jProps.setProperty("log4j.rootLogger", "DEBUG, A1");
		log4jProps.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
		log4jProps.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
		log4jProps.setProperty("log4j.appender.A1.layout.ConversionPattern", "%d - %-5p - %-30c{1} - %m%n");
		PropertyConfigurator.configure(log4jProps);

		try
		{
			// Create a file and a Writer to store the report
			File tmpFile = File.createTempFile("xxx", ".html");
			tmpFile.deleteOnExit();
			Writer writer = new FileWriter(tmpFile);

			ConnectionProp sqlCp = new ConnectionProp();
			sqlCp.setUrl("jdbc:sqlserver://mm-op-dwutv.maxm.se");
			sqlCp.setUsername("sa");
			sqlCp.setPassword("__some_secret_password__");

			ConnectionProp pcsCp = new ConnectionProp();
			pcsCp.setUrl("jdbc:h2:file:C:/Users/goran/.dbxtune/data/MM-OP-DWUTV_2024-12-06;IFEXISTS=TRUE");
			pcsCp.setUsername("sa");
			pcsCp.setPassword("");
			
			// Create a connection to PCS and Source SQL Server
			DbxConnection monConn        = DbxConnection.connect(null, sqlCp);
			DbxConnection pcsStorageConn = DbxConnection.connect(null, pcsCp);


			SqlServerJobSchedulerReport dummy = new SqlServerJobSchedulerReport();

			dummy.transferJobScheduler(1, monConn, pcsStorageConn);
			dummy.createReport(writer, pcsStorageConn);
			
			// Open in a Browser
			if (true)
			{
				if (Desktop.isDesktopSupported())
				{
					Desktop desktop = Desktop.getDesktop();
					if ( desktop.isSupported(Desktop.Action.BROWSE) )
					{
						try
						{
							desktop.browse(tmpFile.toURI());
						}
						catch (Exception ex)
						{
							_logger.error("Problems when open the URL '" + tmpFile + "'. Caught: " + ex, ex);
						}
					}
				}
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}

	}
}
