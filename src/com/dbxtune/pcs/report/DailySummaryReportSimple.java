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
package com.dbxtune.pcs.report;

import java.io.IOException;
import java.io.Writer;
import java.lang.invoke.MethodHandles;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.pcs.report.IProgressReporter.State;
import com.dbxtune.pcs.report.content.DailySummaryReportContent;
import com.dbxtune.pcs.report.content.IReportEntry;
import com.dbxtune.pcs.report.content.RecordingInfo;
import com.dbxtune.pcs.report.senders.ReportSenderNoOp;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.HeartbeatMonitor;

public class DailySummaryReportSimple
extends DailySummaryReportDefault
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	@Override
	public void init() throws Exception
	{
		setReportSender(new ReportSenderNoOp());

		super.init();
	}
	
	@Override
	public void save()
	{
		// Nope: We don't want to save
	}
	
	@Override
	public void addReportEntriesTop()
	{
		// Only the Recording Info
		addReportEntry( new RecordingInfo(this).withCollapsedHeader(true) );
	}

	@Override
	public void addReportEntriesBottom()
	{
		// None
	}
	
	/** Collapse the Table Of Content section */
	@Override 
	public boolean isCollapsedHeaderForTocSection() { return true; }

	/** Do not create Exec Time Report section */
	@Override 
	public boolean createExecTimeReportSection() { return false; }

	/** */
	@Override
	public String getReportName() { return "Job Scheduler Report"; }
	
	private Writer _writer;
	public void   setWriter(Writer writer) { _writer = writer; }
	public Writer getWriter()              { return _writer; }
	
	
	@Override
	public void create() throws InterruptedException, IOException
	{
		DailySummaryReportContent content = new DailySummaryReportContent();
		content.setServerName( getServerName() );

		// Get Configuration possibly from the DbxCentral
//		Configuration pcsSavedConf = getConfigFromDbxCentral(getServerName());
		Configuration pcsSavedConf = getConfigFromPcs();
		Configuration localConf    = Configuration.getCombinedConfiguration();
		
		// Iterate all entries and create the report
		int entryCount = getReportEntries().size();
		int count = 0;
		IProgressReporter progressReporter = getProgressReporter();
		for (IReportEntry entry : getReportEntries())
		{
			// First force a Garbage Collection
			System.gc();

			count++;
			int pctDone = (int) ( ((count*1.0) / (entryCount*1.0)) * 100.0 );
			try
			{
				if (entry.isEnabled())
				{
					entry.setExecStartTime();

					// Report Progress 
					if (progressReporter != null)
					{
						boolean doNextEntry = progressReporter.setProgress(State.BEFORE, entry, "Creating ReportEntry '" + entry.getClass().getSimpleName() + "', with Subject '" + entry.getSubject() + "'.", pctDone);
						if ( doNextEntry == false )
							throw new InterruptedException("Report Creation was aborted...");
					}

					// Check if tables etc exists
					entry.checkForIssuesBeforeCreate(getConnection());
					if ( ! entry.hasProblem() )
					{
						// Create the report, this may take time since it executes SQL Statements
						_logger.info("Creating ReportEntry '" + entry.getClass().getSimpleName() + "', with Subject '" + entry.getSubject() + "'. Percent done: " + pctDone);

						// the ReportEntry needs any "helper" indexes to perform it's duties faster... create them
						entry.createReportingIndexes(getConnection());
						
						// Create
						entry.create(getConnection(), getServerName(), pcsSavedConf, localConf);
					}

					// Report Progress 
					if (progressReporter != null)
					{
						boolean doNextEntry = progressReporter.setProgress(State.AFTER, entry, "Creating ReportEntry '" + entry.getClass().getSimpleName() + "', with Subject '" + entry.getSubject() + "'.", pctDone);
						if ( doNextEntry == false )
							throw new InterruptedException("Report Creation was aborted...");
					}

					// Set how long it took
					entry.setExecEndTime();
				}
			} 
			catch (RuntimeException rte) 
			{
				_logger.warn("Problems creating ReportEntry for '" + entry.getClass().getSimpleName() + "'. Caught RuntimeException, continuing with next entry.", rte);
			}
			
			// If this is done from the Collectors thread... and each of the reports are taking a long time... 
			// Then we might want to "ping" the collector supervisor, that we are still "alive"
			HeartbeatMonitor.doHeartbeat();
//System.out.println("  ******* Used Memory " + Memory.getUsedMemoryInMB() + " MB ****** create(): AFTER: "+ entry.getClass().getSimpleName());
		}

		// Where do we write to
		Writer writer = getWriter();

		// Create content
		createHtml(writer);

		boolean hasIssueToReport = hasIssueToReport();
		content.setNothingToReport( ! hasIssueToReport );

		// set the content
		setReportContent(content);
	}
}
