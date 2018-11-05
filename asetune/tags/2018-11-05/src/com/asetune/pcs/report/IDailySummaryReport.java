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
