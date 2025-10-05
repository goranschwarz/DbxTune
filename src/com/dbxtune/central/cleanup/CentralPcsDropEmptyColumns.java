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

import com.dbxtune.central.pcs.CentralPcsWriterHandler;
import com.dbxtune.central.pcs.CentralPersistWriterJdbc;
import com.dbxtune.central.pcs.H2CentralDropEmptyColumns;
import com.dbxtune.central.pcs.ICentralPersistWriter;
import com.dbxtune.sql.conn.ConnectionProp;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;

import it.sauronsoftware.cron4j.Task;
import it.sauronsoftware.cron4j.TaskExecutionContext;

public class CentralPcsDropEmptyColumns
extends Task
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public static final String  PROPKEY_start = "CentralPcsDropEmptyColumns.start";
	public static final boolean DEFAULT_start = true;

	public static final String PROPKEY_cron = "CentralPcsDropEmptyColumns.cron";
//	public static final String DEFAULT_cron = "00 01 1 * *"; // 01:00 first day of the month
	public static final String DEFAULT_cron = "00 01 1 Feb,May,Aug,Nov *"; // 01:00 first day of: Feb,May,Aug,Nov (so every 3 month)


	public static final String  PROPKEY_LOG_FILE_PATTERN = "CentralPcsDropEmptyColumns.log.file.pattern";
	public static final String  DEFAULT_LOG_FILE_PATTERN    = "%d - %-5p - %m%n";
	
	public static final String  PROPKEY_dryRun = "CentralPcsDropEmptyColumns.dryRun";
	public static final boolean DEFAULT_dryRun = false;
//	public static final boolean DEFAULT_dryRun = true; // For test purposes

	public static final String EXTRA_LOG_NAME = CentralPcsDropEmptyColumns.class.getSimpleName() + "-TaskLogger";

	private static final String _prefix = "DROP-EMPTY-COLUMNS: ";

	private boolean _dryRun = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_dryRun, DEFAULT_dryRun);

	@Override
	public void execute(TaskExecutionContext context) throws RuntimeException
	{
		_logger.info("");
		_logger.info("#############################################################################################");
		_logger.info("Begin task: Persist Writer JDBC Data Cleanup - Drop Empty Columns");

		if ( ! CentralPcsWriterHandler.hasInstance() )
		{
			_logger.info("Skipping cleanup, no CentralPcsWriterHandler was found.");
			return;
		}

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
			_logger.info("Skipping cleanup, no CentralPersistWriterJdbc Connection Properties Object was found.");
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
			_logger.error("Skipping cleanup. Problems connecting to CentralPersistWriterJdbc.", e);
			return;
		}

		
		if (conn != null)
		{
			try
			{
				_logger.info("Executing Drop Empty Columns Cleanup in CentralPersistWriterJdbc");
				doCleanup(conn);
			}
			catch (Exception e)
			{
				_logger.error("Problems when executing Drop Empty Columns Cleanup in CentralPersistWriterJdbc", e);
			}
		}

		
		if (conn != null)
		{
			_logger.info("Closing DBMS connection to CentralPersistWriterJdbc");
			conn.closeNoThrow();
		}
		
		
		_logger.info("End task: Persist Writer JDBC Data Cleanup - Drop Empty Columns");
	}

	private void doCleanup(DbxConnection conn)
	throws Exception
	{
		// Create object
		H2CentralDropEmptyColumns cleaner = new H2CentralDropEmptyColumns(conn);

		// Set options
		if (_dryRun)
		{
			_logger.info("Executing in DRY-RUN mode. The DDL Statement to drop un-used columns will NOT be executed. Instead they will be printed to the output.");
			cleaner.setDryRun(true);
		}

		cleaner.setMessagePrefix(_prefix);
		
		// DO WORK
		cleaner.doWork();
	}
}
