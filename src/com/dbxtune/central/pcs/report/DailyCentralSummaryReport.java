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
package com.dbxtune.central.pcs.report;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.apache.log4j.Logger;

import com.dbxtune.pcs.report.DailySummaryReportFactory;
import com.dbxtune.pcs.report.IDailySummaryReport;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;

public class DailyCentralSummaryReport
{
	private static Logger _logger = Logger.getLogger(DailyCentralSummaryReport.class);

	//---------------------------------------------
	// NOTE -- NOTE -- NOTE -- NOTE -- NOTE -- NOTE --
	//---------------------------------------------
	// NOT YET IMPLEMENTED
	// Continue to work on this...

	//---------------------------------------------
	// BEGIN: Daily Summary Report
	//---------------------------------------------
	public static void createDailySummaryReport(DbxConnection conn, String serverName, String schemaName)
	{
		if ( ! DailySummaryReportFactory.isCreateReportEnabled() )
		{
			_logger.info("Daily Summary Report is NOT Enabled, this can be enabled using property '"+DailySummaryReportFactory.PROPKEY_create+"=true'.");
			return;
		}

//		if ( ! DailySummaryReportFactory.isCreateReportEnabledForServer(serverName) )
//		{
//			_logger.info("Daily Summary Report is NOT Enabled, for serverName '"+serverName+"'. Check property '"+DailySummaryReportFactory.PROPKEY_filter_keep_servername+"' or '"+DailySummaryReportFactory.PROPKEY_filter_skip_servername+"'.");
//			return;
//		}

		// For now HardCode the classes we want to use.
//		System.getProperties().setProperty(DailySummaryReportFactory.PROPKEY_reportClassname, "com.dbxtune.pcs.report.DailySummaryReport" + dbxCollector);
		System.getProperties().setProperty(DailySummaryReportFactory.PROPKEY_reportClassname, "com.dbxtune.central.pcs.report.DailySummaryReportDbxCentral");
		System.getProperties().setProperty(DailySummaryReportFactory.PROPKEY_senderClassname, "com.dbxtune.pcs.report.senders.ReportSenderToMail");
		
		IDailySummaryReport report = DailySummaryReportFactory.createDailySummaryReport();
		if (report == null)
		{
			_logger.info("Daily Summary Report: create did not pass a valid report instance, skipping report creation.");
			return;
		}

		// Check connection
		if (conn == null)
		{
			_logger.info("Daily Summary Report NO Connection was passed, conn=" + conn);
			return;
		}
		if ( ! conn.isConnectionOk() )
		{
			_logger.info("Daily Summary Report NO VALID Connection was passed (not connected), conn=" + conn);
			return;
		}

		// Set DBMS connection and schema name the "Local Metrics" tables are stored in
		report.setConnection(conn);
		report.setDbmsSchemaName(schemaName);

		// Set NAME on the server we are reporting at
		report.setServerName(serverName);
		
		// Set Reporting Period
		setReportingPeriod(report);
		
		try
		{
			// Initialize the Report, which also initialized the ReportSender
			report.init();

			// Create & and Send the report
			report.create();
			report.send();

			// Save the report
			report.save();

			// remove/ old reports from the "archive"
			report.removeOldReports();
		}
		catch(Exception ex)
		{
			_logger.error("Problems Sending Daily Summary Report. Caught: "+ex, ex);
		}
	}
	//---------------------------------------------
	// END: Daily Summary Report
	//---------------------------------------------

	private static void setReportingPeriod(IDailySummaryReport report)
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		boolean setReportingPeriod = conf.getBooleanProperty("dsr.reporting.period.set", true);
		if (setReportingPeriod)
		{
			// (Nearly 2 days) -->> 1 days 23 hours and 59 seconds
			// (Nearly 1 days) -->> 0 days 23 hours and 59 seconds
			int days            = conf.getIntProperty("dsr.reporting.period.days"        , 0); 
			int beginTimeHour   = conf.getIntProperty("dsr.reporting.period.start.hour"  , 0);
			int beginTimeMinute = conf.getIntProperty("dsr.reporting.period.start.minute", 0);
			int endTimeHour     = conf.getIntProperty("dsr.reporting.period.end.hour"    , 23);
			int endTimeMinute   = conf.getIntProperty("dsr.reporting.period.end.minute"  , 59);

			LocalDateTime now = LocalDateTime.now();
			
			Timestamp beginTs = Timestamp.valueOf(now.withHour(beginTimeHour).withMinute(beginTimeMinute).withSecond(0).withNano(0).minusDays(days));
			Timestamp endTs   = Timestamp.valueOf(now.withHour(endTimeHour).withMinute(endTimeMinute).withSecond(59).withNano(999_999_999));

			report.setReportPeriodBeginTime(beginTs);
			report.setReportPeriodEndTime(endTs);
		}
	}
}
