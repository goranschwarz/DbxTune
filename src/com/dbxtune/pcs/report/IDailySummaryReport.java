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
package com.dbxtune.pcs.report;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;

import com.dbxtune.pcs.report.content.DailySummaryReportContent;
import com.dbxtune.pcs.report.content.IReportEntry;
import com.dbxtune.pcs.report.content.RecordingInfo;
import com.dbxtune.pcs.report.senders.IReportSender;
import com.dbxtune.sql.conn.DbxConnection;

public interface IDailySummaryReport
extends AutoCloseable
{

	/**
	 * Set the JDBC Connection to the PCS database to use
	 * @param conn
	 */
	void setConnection(DbxConnection conn);

	/**
	 * In what Schema are the Reporting tables stored in
	 * @param schemaName
	 */
	void setDbmsSchemaName(String schemaName);

	/**
	 * For what server is this report for.
	 * @param serverName
	 */
	void setServerName(String serverName);

//	/**
//	 * If the DRS needs a MonTable Dictionary this is the method you create it with
//	 * @return
//	 */
//	MonTablesDictionary createMonTablesDictionary();
	// NOTE: The above was not saving the WaitEvent Descriptions to the PCS... so this may be implemented in the future... right now, lets do it statically

	/**
	 * Initialize whatever
	 * @throws Exception 
	 */
	void init() throws Exception;

	/**
	 * Get a list of Report Entries
	 * @return
	 */
	List<IReportEntry> getReportEntries();

	/**
	 * Get the 'Recording Info' entry
	 * @return
	 */
	RecordingInfo getReportEntryRecordingInfo();
	
	/**
	 * Default method which adds Report Entries by any implementations and is placed at the TOP of the report (called from init) 
	 */
	void addReportEntriesTop();
	
	/**
	 * Default method which adds Report Entries by any implementations and is placed at the MIDDLE of the report  (called from init) 
	 */
	void addReportEntries();

	/**
	 * Default method which adds Report Entries by any implementations and is placed at the BOTTOM of the report (called from init) 
	 */
	void addReportEntriesBottom();

	/**
	 * Create the report
	 * @throws InterruptedException 
	 */
	void create() throws InterruptedException, IOException;

	/**
	 * Send the report to "whatever"
	 */
	void send();

	/**
	 * Save a report for "archiving"... For example: To DbxCentral, so it can be viewed at a later stage.
	 */
	void save();

	/**
	 * If the report is saved by the save() method, then return the File here!
	 * @return File or null if not available
	 */
	File getReportFile();

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


	
	/**
	 * Set progress report, so we can track/print what we are doing when creating a Daily Summary Report.
	 */
	void setProgressReporter(IProgressReporter progressReporter);

	/**
	 * Get progress report, so we can track/print what we are doing when creating a Daily Summary Report.
	 */
	IProgressReporter getProgressReporter();

	

	/**
	 * If we want to restrict the begin time of the report (applying a WHERE clause in the reports)
	 * 
	 * @param hour    what minute. (-1 = unrestricted)
	 * @param minute  what minute. (-1 = unrestricted)
	 */
	void setReportPeriodBeginTime(int hour, int minute);

	/** If we want to restrict the begin time of the report (applying a WHERE clause in the reports) */
	void setReportPeriodBeginTime(Timestamp beginTs);

	/**
	 * If we want to restrict the begin time of the report (applying a WHERE clause in the reports)
	 * 
	 * @param hour    what minute. (-1 = unrestricted)
	 * @param minute  what minute. (-1 = unrestricted)
	 */
	void setReportPeriodEndTime(int hour, int minute);

	/** If we want to restrict the begin time of the report (applying a WHERE clause in the reports) */
	void setReportPeriodEndTime  (Timestamp endTs);
	
	/**
	 * Check if we have any begin/end time for the reporting period
	 * @return true or false
	 */
	boolean hasReportPeriod();

	/** 
	 * Get the Actual Reporting BEGIN Time
	 * <p>
	 * If it has been set by setReportPeriodBegin/EndTime() that will be reflected<br>
	 * If it'a the full day, the Recording Start/End time will be reflected<br>
	 */
	Timestamp getReportBeginTime();

	/** 
	 * Get the Actual Reporting END Time
	 * <p>
	 * If it has been set by setReportPeriodBegin/EndTime() that will be reflected<br>
	 * If it'a the full day, the Recording Start/End time will be reflected<br>
	 */
	Timestamp getReportEndTime();

	/**
	 * Close any internal objects, like the DBMS Connection
	 */
	@Override
	void close();

	/** What time did we initialize the Report */
	long getInitTime();

}
