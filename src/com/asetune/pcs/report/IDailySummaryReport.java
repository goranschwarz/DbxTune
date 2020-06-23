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
package com.asetune.pcs.report;

import com.asetune.pcs.report.content.DailySummaryReportContent;
import com.asetune.pcs.report.senders.IReportSender;
import com.asetune.sql.conn.DbxConnection;

public interface IDailySummaryReport
{

	/**
	 * Set the JDBC Connection to the PCS database to use
	 * @param conn
	 */
	void setConnection(DbxConnection conn);

	/**
	 * For what server is this report for.
	 * @param serverName
	 */
	void setServerName(String serverName);

	/**
	 * Initialize whatever
	 * @throws Exception 
	 */
	void init() throws Exception;

	/**
	 * Create the report
	 */
	void create();

	/**
	 * Send the report to "whatever"
	 */
	void send();

	/**
	 * Save a report for "archiving"... For example: To DbxCentral, so it can be viewed at a later stage.
	 */
	public void save();

	/**
	 * Remove old report from the "archive"
	 */
	void removeOldReports();


	/**
	 * Set the implementation for a ReportSender
	 * @param reportSender
	 */
	void setReportSender(IReportSender reportSender);

	
	
	/**
	 * Set the report content object, normally done in create()
	 * @param content
	 */
	void setReportContent(DailySummaryReportContent content);

	/**
	 * Get the report content object, normally done in send()
	 * @param content
	 */
	DailySummaryReportContent getReportContent();

}
