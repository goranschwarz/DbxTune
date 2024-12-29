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
package com.dbxtune.central.cleanup;

import org.apache.log4j.Logger;

import com.dbxtune.pcs.report.DsrPriority;

import it.sauronsoftware.cron4j.Task;
import it.sauronsoftware.cron4j.TaskExecutionContext;

public class DsrPriorityFileWriter
extends Task
{
	private static Logger _logger = Logger.getLogger(DsrPriorityFileWriter.class);

	public static final String  PROPKEY_start = "DailySummaryReport.priority.file.writer.start";
	public static final boolean DEFAULT_start = true;

	public static final String PROPKEY_cron = "DailySummaryReport.priority.file.writer.cron";
	public static final String DEFAULT_cron = "59 23 * * *"; // at 23:59 every day

	public static final String  PROPKEY_LOG_FILE_PATTERN = "DailySummaryReport.priority.file.writer.log.file.pattern";
	public static final String  DEFAULT_LOG_FILE_PATTERN    = "%d - %-5p - %-30c{1} - %m%n";
	
	public static final String EXTRA_LOG_NAME = DsrPriorityFileWriter.class.getSimpleName() + "-TaskLogger";

	@Override
	public void execute(TaskExecutionContext context) throws RuntimeException
	{
		_logger.info("");
		_logger.info("#############################################################################################");
		_logger.info("Begin task: Daily Summary Report Priority File Writer");

		DsrPriority.createOrReplaceProgresFile();
		
		_logger.info("End task: Daily Summary Report Priority File Writer");
	}
}
