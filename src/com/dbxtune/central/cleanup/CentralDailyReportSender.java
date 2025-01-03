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

import java.lang.invoke.MethodHandles;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.central.lmetrics.LocalMetricsPersistWriterJdbc;
import com.dbxtune.central.pcs.CentralPcsWriterHandler;
import com.dbxtune.central.pcs.CentralPersistWriterJdbc;
import com.dbxtune.central.pcs.ICentralPersistWriter;
import com.dbxtune.central.pcs.report.DailyCentralSummaryReport;
import com.dbxtune.sql.conn.ConnectionProp;
import com.dbxtune.sql.conn.DbxConnection;

import it.sauronsoftware.cron4j.Task;
import it.sauronsoftware.cron4j.TaskExecutionContext;

public class CentralDailyReportSender
extends Task
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public static final String  PROPKEY_start = "CentralDailyReportSender.start";
	public static final boolean DEFAULT_start = true;

	public static final String PROPKEY_cron = "CentralDailyReportSender.cron";
	public static final String DEFAULT_cron = "58 23 * * *"; // at 23:58 every day

	public static final String  PROPKEY_LOG_FILE_PATTERN = "CentralDailyReportSender.log.file.pattern";
	public static final String  DEFAULT_LOG_FILE_PATTERN    = "%d - %-5p - %-30c{1} - %m%n";
	
	public static final String EXTRA_LOG_NAME = CentralDailyReportSender.class.getSimpleName() + "-TaskLogger";

	@Override
	public void execute(TaskExecutionContext context) throws RuntimeException
	{
		_logger.info("");
		_logger.info("#############################################################################################");
		_logger.info("Begin task: Daily Central Summary Report");

//		_logger.info(" ---- No Daily Summary Report has yet been implemented ---- ");
		
		ConnectionProp connProps = null;
		CentralPcsWriterHandler centralPcsHandler = CentralPcsWriterHandler.getInstance();
		for (ICentralPersistWriter w : centralPcsHandler.getWriters())
		{
			if (w instanceof CentralPersistWriterJdbc)
			{
				connProps = ((CentralPersistWriterJdbc) w).getStorageConnectionProps();
				break;
			}
		}
		if ( connProps == null )
		{
			_logger.info("Skipping Daily Central Summary Report, no CentralPersistWriterJdbc Connection Properties Object was found.");
			return;
		}
		
		DbxConnection conn = null;
		try
		{
			_logger.info("Open DBMS connection to CentralPersistWriterJdbc. connProps=" + connProps);
			conn = DbxConnection.connect(null, connProps);
		}
		catch (Exception e)
		{
			_logger.error("Skipping Daily Central Summary Report. Problems connecting to CentralPersistWriterJdbc.", e);
			return;
		}

		
		if (conn != null)
		{
			try
			{
				_logger.info("Executing Daily Central Summary Report.");
				DailyCentralSummaryReport.createDailySummaryReport(conn, "DbxCentral", LocalMetricsPersistWriterJdbc.LOCAL_METRICS_SCHEMA_NAME);
			}
			catch (Exception e)
			{
				_logger.error("Problems when executing Daily Central Summary Report in CentralPersistWriterJdbc", e);
			}
		}

		
		if (conn != null)
		{
			_logger.info("Closing DBMS connection to CentralPersistWriterJdbc");
			conn.closeNoThrow();
		}
		
		_logger.info("End task: Daily Central Summary Report");
	}
}
