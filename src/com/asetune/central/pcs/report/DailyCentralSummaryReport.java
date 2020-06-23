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
package com.asetune.central.pcs.report;

import org.apache.log4j.Logger;

import com.asetune.pcs.report.DailySummaryReportFactory;
import com.asetune.pcs.report.IDailySummaryReport;
import com.asetune.sql.conn.DbxConnection;

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
	private void createDailySummaryReport(DbxConnection conn, String serverName)
	{
		if ( ! DailySummaryReportFactory.isCreateReportEnabled() )
		{
			_logger.info("Daily Summary Report is NOT Enabled, this can be enabled using property '"+DailySummaryReportFactory.PROPKEY_create+"=true'.");
			return;
		}

		if ( ! DailySummaryReportFactory.isCreateReportEnabledForServer(serverName) )
		{
			_logger.info("Daily Summary Report is NOT Enabled, for serverName '"+serverName+"'. Check property '"+DailySummaryReportFactory.PROPKEY_filter_keep_servername+"' or '"+DailySummaryReportFactory.PROPKEY_filter_skip_servername+"'.");
			return;
		}

		IDailySummaryReport report = DailySummaryReportFactory.createDailySummaryReport(serverName);
		if (report == null)
		{
			_logger.info("Daily Summary Report: create did not pass a valid report instance, skipping report creation.");
			return;
		}

		report.setConnection(conn);
		report.setServerName(serverName);
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
}
