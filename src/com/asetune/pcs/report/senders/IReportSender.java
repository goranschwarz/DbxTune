package com.asetune.pcs.report.senders;

import com.asetune.pcs.report.content.DailySummaryReportContent;

public interface IReportSender
{
	/**
	 * @return Name of this module 
	 */
	String getName();
	
	/**
	 * Initialize whatever
	 * @throws Exception 
	 */
	void init() throws Exception;

	/**
	 * Prints the configuration
	 */
	void printConfig();

	/**
	 * Send the report to "whatever"
	 * @param reportContent 
	 */
	void send(DailySummaryReportContent reportContent);

}
