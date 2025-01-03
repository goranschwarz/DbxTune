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
package com.dbxtune.central.pcs;

import java.lang.invoke.MethodHandles;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.utils.Configuration;

import it.sauronsoftware.cron4j.Task;
import it.sauronsoftware.cron4j.TaskExecutionContext;

public class H2WriterStatCronTask
extends Task
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public static final String  PROPKEY_start = "H2WriterStatCronTask.start";
	public static final boolean DEFAULT_start = true;

	public static final String PROPKEY_cron = "H2WriterStatCronTask.cron";
//	public static final String DEFAULT_cron = "59 0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23 * * *"; // every hour at 59 minutes
	public static final String DEFAULT_cron = "9,19,29,39,49,59 0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23 * * *"; // every hour at 9,19,29,39,49,59 minutes

	public static final String  PROPKEY_LOG_FILE_PATTERN = "H2WriterStatCronTask.log.file.pattern";
	public static final String  DEFAULT_LOG_FILE_PATTERN    = "%d - %-5p - %-30c{1} - %m%n";
	
	public static final String EXTRA_LOG_NAME = H2WriterStatCronTask.class.getSimpleName() + "-TaskLogger";

	public static final String  PROPKEY_sampleTimeFormat = "H2WriterStatCronTask.sampleTimeFormat";
	public static final String  DEFAULT_sampleTimeFormat = "%HH:%MM:%SS.%ms";

	private H2WriterStat _h2WriterStat;

	public H2WriterStatCronTask()
	{
		_h2WriterStat = new H2WriterStat(new H2WriterStat.CentralH2PerfCounterConnectionProvider());
		
		// Set time format
		String timeFmt = Configuration.getCombinedConfiguration().getProperty(PROPKEY_sampleTimeFormat, DEFAULT_sampleTimeFormat);
		_h2WriterStat.setSampleTimeFormat(timeFmt);
		
		// Refresh first time (done in a thread to "not stop things")
		Thread tmpThread = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				_h2WriterStat.refreshCounters();
			}
		});
		tmpThread.setDaemon(true);
		tmpThread.setName("H2WriterStatCronTask:constructorInit");
		tmpThread.start();
	}

	@Override
	public void execute(TaskExecutionContext context) throws RuntimeException
	{
		
		// Refresh the counters
		_h2WriterStat.refreshCounters();
		
		// Get a Statistics String
		String statStr = _h2WriterStat.getStatString();

		_logger.info("H2 Writer Statistics: "+statStr);
	}
}
