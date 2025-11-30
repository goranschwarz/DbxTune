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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.time.DateUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.central.lmetrics.LocalMetricsPersistWriterJdbc;
import com.dbxtune.central.pcs.CentralPcsWriterHandler;
import com.dbxtune.central.pcs.CentralPersistWriterBase;
import com.dbxtune.central.pcs.CentralPersistWriterBase.Table;
import com.dbxtune.central.pcs.CentralPersistWriterJdbc;
import com.dbxtune.central.pcs.ICentralPersistWriter;
import com.dbxtune.sql.conn.ConnectionProp;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.DbUtils;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.TimeUtils;

import it.sauronsoftware.cron4j.Task;
import it.sauronsoftware.cron4j.TaskExecutionContext;

public class CentralPcsJdbcCleaner
extends Task
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public static final String  PROPKEY_start = "CentralPcsJdbcCleaner.start";
	public static final boolean DEFAULT_start = true;

	public static final String PROPKEY_cron = "CentralPcsJdbcCleaner.cron";
//	public static final String DEFAULT_cron = "30 01 1 * *"; // 01:30 first day of the month
	public static final String DEFAULT_cron = "30 23 * * 0"; // 23:30 Sunday
//	public static final String DEFAULT_cron = "* * * * *";

	public static final String PROPKEY_keepDays = "CentralPcsJdbcCleaner.keep.days";
	public static final int    DEFAULT_keepDays = 365 + 62; // keep for 14 months (so we can see any "end-of-year" work
//	public static final int    DEFAULT_keepDays = 60;

	public static final String PROPKEY_cmHistory_keepDays = "CentralPcsJdbcCleaner.history.cm.keep.days";
	public static final int    DEFAULT_cmHistory_keepDays = 30; // 30 days for CM Details, while graph is using PROPKEY_keepDays

	public static final String PROPKEY_sessionsSampleDetails_keepDays = "CentralPcsJdbcCleaner.sessionsSampleDetails.cm.keep.days";
	public static final int    DEFAULT_sessionsSampleDetails_keepDays = 30; // 30 days for CM Details, while graph is using PROPKEY_keepDays

	public static final String PROPKEY_localMetricsCm_keepDays = "CentralPcsJdbcCleaner.localMetrics.cm.keep.days";
	public static final int    DEFAULT_localMetricsCm_keepDays = 30; // 30 days for CM Details, while graph is using PROPKEY_keepDays
	
//	public static final String  PROPKEY_printSpaceInfo = "CentralPcsJdbcCleaner.print.space.info";
//	public static final boolean DEFAULT_printSpaceInfo = true;

	public static final String  PROPKEY_LOG_FILE_PATTERN = "CentralPcsJdbcCleaner.log.file.pattern";
	public static final String  DEFAULT_LOG_FILE_PATTERN    = "%d - %-5p - %m%n";
	
	public static final String  PROPKEY_dryRun = "CentralPcsJdbcCleaner.dryRun";
	public static final boolean DEFAULT_dryRun = false;
//	public static final boolean DEFAULT_dryRun = true; // For test purposes

	public static final String EXTRA_LOG_NAME = CentralPcsJdbcCleaner.class.getSimpleName() + "-TaskLogger";

	private static final String _prefix = "DATA-RETENTION-CLEANUP: ";

	private boolean _dryRun = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_dryRun, DEFAULT_dryRun);

	@Override
	public void execute(TaskExecutionContext context) throws RuntimeException
	{
		_logger.info("");
		_logger.info("#############################################################################################");
		_logger.info("Begin task: Persist Writer JDBC Data Cleanup");

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
				_logger.info("Executing Retention Cleanup in CentralPersistWriterJdbc");
				doCleanup(conn);
			}
			catch (Exception e)
			{
				_logger.error("Problems when executing Cleanup in CentralPersistWriterJdbc", e);
			}
		}

		
		if (conn != null)
		{
			_logger.info("Closing DBMS connection to CentralPersistWriterJdbc");
			conn.closeNoThrow();
		}
		
		
		_logger.info("End task: Persist Writer JDBC Data Cleanup");
	}

	private void doCleanup(DbxConnection conn)
	throws Exception
	{
		String lq = conn.getLeftQuote();  // Note no replacement is needed, since we get it from the connection
		String rq = conn.getRightQuote(); // Note no replacement is needed, since we get it from the connection

		int keepDays = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_keepDays, DEFAULT_keepDays);
		Timestamp olderThan = new Timestamp( DateUtils.addDays(new Date(), -keepDays).getTime() );
		_logger.info(_prefix + "Retention period is " + keepDays + ", so data that is older than " + keepDays + " days (or older than '" + olderThan + "') will be deleted. This can be changed with property '" + PROPKEY_keepDays + "'.");

		// Get "ServerNames" so we know what DBMS schemas we should iterate
		String tabName = CentralPersistWriterBase.getTableName(conn, null, Table.CENTRAL_SESSIONS, null, true);
		String sql = "select distinct " + lq+"ServerName"+rq + " from " + tabName + " order by 1";
		List<String> schemaList = new ArrayList<>();
		// autoclose: stmnt, rs
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while (rs.next())
				schemaList.add(rs.getString(1));
		}

		// Check if we have a DbxcLocalMetrics Schema...
		Set<String> existingSchemaSet = DbUtils.getSchemaNames(conn);
		if (existingSchemaSet.contains(LocalMetricsPersistWriterJdbc.LOCAL_METRICS_SCHEMA_NAME))
		{
			// Do cleanup here
			doCleanupForLocalMetrics(conn, olderThan, olderThan);
		}
		
		
		// Iterate over SCHEMA:s
		int sumCleanupCount = 0;
		for (String schema : schemaList)
		{
			int cleanupCount = doCleanupForSchema(conn, schema, olderThan);
			sumCleanupCount += cleanupCount;
			
			// FIXME: Should we decrease "NumOfSamples" with the cleanupCount
			// FIXME: Should we also try to remove the schema if there are no more records in the schema?
			// maybe the best way to do this is:
			//     select count(*) from SCHEMA.DbxSessionSamples
			//     if (count == 0)
			//        - remove schema
			//        - remove metadata in central
			//     else
			//        - update NumOfSamples
		}
		
		// remove entries from the "base" Central dictionary
		if (sumCleanupCount > 0)
		{
			_logger.info(_prefix + "Start to cleanup records in DbxCentral MetaData tables.");
			doCleanupForCentralDbxTable(conn, Table.CENTRAL_SESSIONS,       olderThan);
		//	doCleanupForCentralDbxTable(conn, Table.CENTRAL_GRAPH_PROFILES, olderThan); // we might want to remove SCHEMAS in the JSON string...
		//	doCleanupForCentralDbxTable(conn, Table.CENTRAL_VERSION_INFO,   olderThan);
			_logger.info(_prefix + "End cleanup records in DbxCentral MetaData tables.");
		}
		else
		{
			_logger.info(_prefix + "Cleanup records in DbxCentral MetaData tables will not be done since no Session Data was removed.");
		}
	}

	/**
	 * 
	 * @param conn
	 * @param schema
	 * @return -1=Schema is empty and can be deleted, 0=Nothing Was Deleted, >0=Number of samples that was removed. 
	 * @throws Exception
	 */
	private int doCleanupForSchema(DbxConnection conn, String schema, Timestamp olderThan)
	throws Exception
	{
		String lq = conn.getLeftQuote();  // Note no replacement is needed, since we get it from the connection
		String rq = conn.getRightQuote(); // Note no replacement is needed, since we get it from the connection

		String tabName;
		String sql;

		// Get expected number of samples to delete
		int sessionsSampleCount = -99;
		tabName = CentralPersistWriterBase.getTableName(conn, schema, Table.SESSION_SAMPLES, null, true);
		sql = "select count(*) from " + tabName + " where " + lq+"SessionSampleTime"+rq + " < ? ";
		try (PreparedStatement pstmnt = conn.prepareStatement(sql))
		{
			pstmnt.setTimestamp(1, olderThan);
			try (ResultSet rs = pstmnt.executeQuery())
			{
				while (rs.next())
					sessionsSampleCount = rs.getInt(1);
			}
		}
		_logger.info(_prefix + "schema='" + schema + "': Found " + sessionsSampleCount + " samples was found that was older than '" + olderThan + "'.");
		if (sessionsSampleCount <= 0)
		{
			_logger.info(_prefix + "schema='" + schema + "': ---- Nothing to cleanup in schema '" + schema + "'.");
			return 0;
		}

		//--------------------------------------------
		// OK: Lets start to delete stuff!
		//--------------------------------------------
		
		// Get "graph" tables
		List<String> graphTables = new ArrayList<>();
		tabName = CentralPersistWriterBase.getTableName(conn, schema, Table.GRAPH_PROPERTIES, null, true);
		sql = "select distinct " + lq+"TableName"+rq + " from " + tabName + " order by 1 ";
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while (rs.next())
				graphTables.add(rs.getString(1));
		}

		// Iterate over Graph Tables and remove records
		for (String name : graphTables)
		{
			// check if the table exists... If not just continue...
			if ( ! DbUtils.checkIfTableExistsNoThrow(conn, null, schema, name) )
			{
				_logger.info(_prefix + "Skipping, table do not exist: schema '" + schema + "' table '" + name + "'.");
				continue;
			}
			
			// TODO: delete TOP #### from ... in a loop so we do not use up to much transaction log for some databases
			
			sql = "delete from " + lq+schema+rq + "." + lq+name+rq + " where " + lq+"SessionSampleTime"+rq + " < ? ";

			String dryRunComment = "";
			if (_dryRun)
			{
				sql += " and 1=2 -- DRY-RUN";
				dryRunComment = "... DRY-RUN: SQL=|" + sql + "|, olderThan='" + olderThan + "'.";
			}

			long startTime = System.currentTimeMillis();
			try (PreparedStatement pstmnt = conn.prepareStatement(sql))
			{
				pstmnt.setTimestamp(1, olderThan);
				int delCount = pstmnt.executeUpdate();
				_logger.info(_prefix + ">>>> Deleted " + delCount + " records from schema '" + schema + "' in table '" + name + "'. [" + TimeUtils.msDiffNowToTimeStr(startTime) + "], olderThan='" + olderThan + "'." + dryRunComment);
			}
			catch(SQLException ex)
			{
				_logger.error(_prefix + "Problems deleting records from schema '" + schema + "' in table '" + name + "' using SQL=|" + sql + "|. Continuing anyway... Caught: " + ex);
			}
		}
		
		// Remove records from "system" metadata tables in SCHEMA
	//	doCleanupForDbxTableInSchema(conn, schema, Table.ALARM_ACTIVE,           olderThan);
		doCleanupForDbxTableInSchema(conn, schema, Table.ALARM_HISTORY,          olderThan);
		doCleanupForDbxTableInSchema(conn, schema, Table.GRAPH_PROPERTIES,       olderThan);
		doCleanupForDbxTableInSchema(conn, schema, Table.SESSION_SAMPLE_DETAILS, olderThan);
		doCleanupForDbxTableInSchema(conn, schema, Table.SESSION_SAMPLE_SUM,     olderThan);
		doCleanupForDbxTableInSchema(conn, schema, Table.SESSION_SAMPLES,        olderThan);
		doCleanupForDbxTableInSchema(conn, schema, Table.CM_HISTORY_SAMPLE_JSON, olderThan);
		
		_logger.info(_prefix + "schema='" + schema + "': << Done cleanup. sessionsSampleCount=" + sessionsSampleCount);

		return sessionsSampleCount;
	}

	private int doCleanupForDbxTableInSchema(DbxConnection conn, String schema, Table table, Timestamp olderThan)
	{
		String lq = conn.getLeftQuote();  // Note no replacement is needed, since we get it from the connection
		String rq = conn.getRightQuote(); // Note no replacement is needed, since we get it from the connection

		String fullTabName  = CentralPersistWriterBase.getTableName(conn, schema, table, null, true);
		String shortTabName = CentralPersistWriterBase.getTableName(conn, null,   table, null, false);

		if (Table.CM_HISTORY_SAMPLE_JSON.equals(table))
		{
			int cmHistory_keepDays = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_cmHistory_keepDays, DEFAULT_cmHistory_keepDays);
			Timestamp cmHistory_olderThan = new Timestamp( DateUtils.addDays(new Date(), -cmHistory_keepDays).getTime() );
			_logger.info(_prefix + "Retention period for Specialized CM History (like CmActiveStatements) is " + cmHistory_keepDays + ", so data that is older than " + cmHistory_keepDays + " days (or older than '" + cmHistory_olderThan + "') will be deleted. This can be changed with property '" + PROPKEY_cmHistory_keepDays + "'.");

			// Override the default value
			olderThan = cmHistory_olderThan;
		}
		
		if (Table.SESSION_SAMPLE_DETAILS.equals(table))
		{
			// TODO: Can we check if we have a DETAILS collector database for this "schema"
			//       If we don't have details, we might just delete the details records... (since we wont be able to view it) 
			//       And if it's a remote Collector, we can have a Fixed number of days, lets say 30 days or so...
			// For now: just set it to the default: 30 days

			int sessionsSampleDetails_keepDays = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_sessionsSampleDetails_keepDays, DEFAULT_sessionsSampleDetails_keepDays);
			Timestamp sessionsSampleDetails_olderThan = new Timestamp( DateUtils.addDays(new Date(), -sessionsSampleDetails_keepDays).getTime() );
			_logger.info(_prefix + "Retention period for table " + shortTabName + " in schema '" + schema + "' is " + sessionsSampleDetails_keepDays + ", so data that is older than " + sessionsSampleDetails_keepDays + " days (or older than '" + sessionsSampleDetails_olderThan + "') will be deleted. This can be changed with property '" + PROPKEY_sessionsSampleDetails_keepDays + "'.");

			// Override the default value
			olderThan = sessionsSampleDetails_olderThan;
		}
		
		String sql = null;
		if      (Table.ALARM_ACTIVE          .equals(table)) return 0; // Do not delete from this
		else if (Table.CM_LAST_SAMPLE_JSON   .equals(table)) return 0; // Do not delete from this
		else if (Table.CM_HISTORY_SAMPLE_JSON.equals(table)) sql = "delete from " + fullTabName + " where " + lq+"SessionSampleTime"+rq + " < ? ";
		else if (Table.ALARM_HISTORY         .equals(table)) sql = "delete from " + fullTabName + " where " + lq+"SessionSampleTime"+rq + " < ? ";
		else if (Table.GRAPH_PROPERTIES      .equals(table)) sql = "delete from " + fullTabName + " where " + lq+"SessionStartTime" +rq + " < ? ";
		else if (Table.SESSION_SAMPLE_DETAILS.equals(table)) sql = "delete from " + fullTabName + " where " + lq+"SessionSampleTime"+rq + " < ? ";
		else if (Table.SESSION_SAMPLES       .equals(table)) sql = "delete from " + fullTabName + " where " + lq+"SessionSampleTime"+rq + " < ? ";
		else if (Table.SESSION_SAMPLE_SUM    .equals(table)) sql = "delete from " + fullTabName + " where " + lq+"SessionStartTime" +rq + " < ? ";

		if (StringUtil.isNullOrBlank(sql))
			return 0;

		String dryRunComment = "";
		if (_dryRun)
		{
			sql += " and 1=2 -- DRY-RUN";
			dryRunComment = "... DRY-RUN: SQL=|" + sql + "|, olderThan='" + olderThan + "'.";
		}
			
		int delCount = -99;
		try (PreparedStatement pstmnt = conn.prepareStatement(sql))
		{
			pstmnt.setTimestamp(1, olderThan);
			delCount = pstmnt.executeUpdate();
			_logger.info(_prefix + ">>>> Deleted " + delCount + " records from schema '" + schema + "' in table '" + shortTabName + "', olderThan='" + olderThan + "'." + dryRunComment);
		}
		catch(SQLException ex)
		{
			_logger.error(_prefix + "Problems deleting records from schema '" + schema + "' in table '" + shortTabName + "' using SQL=|" + sql + "|. Continuing anyway... Caught: " + ex);
		}
		return delCount;
	}
	
	private int doCleanupForCentralDbxTable(DbxConnection conn, Table table, Timestamp olderThan)
	{
		String lq = conn.getLeftQuote();  // Note no replacement is needed, since we get it from the connection
		String rq = conn.getRightQuote(); // Note no replacement is needed, since we get it from the connection

		String fullTabName  = CentralPersistWriterBase.getTableName(conn, null, table, null, true);
		String shortTabName = CentralPersistWriterBase.getTableName(conn, null, table, null, false);

		String sql = null;
		String sqlGetOldestEntry = null;
		if      (Table.CENTRAL_VERSION_INFO  .equals(table)) return 0; // Do not delete from this
		else if (Table.CENTRAL_GRAPH_PROFILES.equals(table)) return 0; // Do not delete from this
		else if (Table.CENTRAL_SESSIONS      .equals(table)) 
		{
			sql               = "delete from " + fullTabName + " where " + lq+"SessionStartTime"+rq + " < ? ";
			sqlGetOldestEntry = "select min(" + lq+"SessionStartTime"+rq + ") from " + fullTabName;
		}

		if (StringUtil.isNullOrBlank(sql))
			return 0;

		String dryRunComment = "";
		if (_dryRun)
		{
			sql += " and 1=2 -- DRY-RUN";
			dryRunComment = "... DRY-RUN: SQL=|" + sql + "|, olderThan='" + olderThan + "'.";
		}

		// do DELETE
		int delCount = -99;
		try (PreparedStatement pstmnt = conn.prepareStatement(sql))
		{
			pstmnt.setTimestamp(1, olderThan);
			delCount = pstmnt.executeUpdate();
			_logger.info(_prefix + ">>>> Deleted " + delCount + " records from DbxCentral table '" + shortTabName + "', olderThan='" + olderThan + "'." + dryRunComment);
		}
		catch(SQLException ex)
		{
			_logger.error(_prefix + "Problems deleting records from DbxCentral table '" + shortTabName + "' using SQL=|" + sql + "|. Continuing anyway... Caught: " + ex);
		}
		
		// Get oldest record, and print it...
		sql = sqlGetOldestEntry;
		if (delCount >= 0)
		{
			try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
			{
				Timestamp oldestTs = null;
				while (rs.next())
				{
					oldestTs = rs.getTimestamp(1);
				}
				_logger.info(_prefix + ">>>> Oldest 'SessionStartTime' is '" + oldestTs + "' from DbxCentral table '" + shortTabName + "'.");
			}
			catch(SQLException ex)
			{
				_logger.error(_prefix + "Problems getting last 'SessionStartTime' from DbxCentral table '" + shortTabName + "' using SQL=|" + sql + "|. Continuing anyway... Caught: " + ex);
			}
		}
		
		return delCount;
	}
	

	/**
	 * Cleanup older records for Local Metrics data.
	 * @param conn
	 * @param olderThan   Timestamp to delete older records... for Graphs
	 */
	private void doCleanupForLocalMetrics(DbxConnection conn, Timestamp alarmTables_olderThan, Timestamp graphTables_olderThan)
	{
		int localMetricsCm_keepDays = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_localMetricsCm_keepDays, DEFAULT_localMetricsCm_keepDays);
		Timestamp localMetricsCm_olderThan = new Timestamp( DateUtils.addDays(new Date(), -localMetricsCm_keepDays).getTime() );
		_logger.info(_prefix + "Retention period for Local Metrics CM is " + localMetricsCm_keepDays + ", so data that is older than " + localMetricsCm_keepDays + " days (or older than '" + localMetricsCm_olderThan + "') will be deleted. This can be changed with property '" + PROPKEY_localMetricsCm_keepDays + "'.");


		// Get tables (CM and Graph tables)
		// Use getColumns(... "SessionSampleTime") to filter out tables which looks like CM/Graph tables
		Set<String> alarmTables = new LinkedHashSet<>();
		Set<String> cmTables    = new LinkedHashSet<>();
		Set<String> graphTables = new LinkedHashSet<>();
		try (ResultSet rs = conn.getMetaData().getColumns(null, LocalMetricsPersistWriterJdbc.LOCAL_METRICS_SCHEMA_NAME, "%", "SessionSampleTime")) // (eller "CmSampleTime"
		{
			String tabName = rs.getString(3); // "TABLE_NAME"

			if (tabName.equals("MonAlarmHistory"))
				alarmTables.add(tabName);
			else if (tabName.endsWith("_abs") || tabName.endsWith("_diff") || tabName.endsWith("_rate"))
				cmTables.add(tabName);
			else
				graphTables.add(tabName);
		}
		catch(SQLException ex)
		{
			_logger.error("Problems getting tables using: conn.getMetaData().getColumns(null, 'DbxcLocalMetrics', '%', 'SessionSampleTime') ", ex);
		}

		String schema = LocalMetricsPersistWriterJdbc.LOCAL_METRICS_SCHEMA_NAME;
		
		// Cleanup Alarm tables (longer retention period)
		for (String tabName : alarmTables)
		{
			doCleanupTable(conn, schema, tabName, alarmTables_olderThan);
		}

		// Cleanup CM Details (shorter retention period)
		for (String tabName : cmTables)
		{
			doCleanupTable(conn, schema, tabName, localMetricsCm_olderThan);
		}

		// Cleanup Graph tables (longer retention period)
		for (String tabName : graphTables)
		{
			doCleanupTable(conn, schema, tabName, graphTables_olderThan);
		}
	}
	
	private void doCleanupTable(DbxConnection conn, String schema, String tabName, Timestamp olderThan)
	{
		String lq = conn.getLeftQuote();  // Note no replacement is needed, since we get it from the connection
		String rq = conn.getRightQuote(); // Note no replacement is needed, since we get it from the connection
		
		// TODO: delete TOP #### from ... in a loop so we do not use up to much transaction log for some databases
		
		String sql = "delete from " + lq+schema+rq + "." + lq+tabName+rq + " where " + lq+"SessionSampleTime"+rq + " < ? ";

		String dryRunComment = "";
		if (_dryRun)
		{
			sql += " and 1=2 -- DRY-RUN";
			dryRunComment = "... DRY-RUN: SQL=|" + sql + "|, olderThan='" + olderThan + "'.";
		}

		long startTime = System.currentTimeMillis();
		try (PreparedStatement pstmnt = conn.prepareStatement(sql))
		{
			pstmnt.setTimestamp(1, olderThan);
			int delCount = pstmnt.executeUpdate();
			_logger.info(_prefix + ">>>> Deleted " + delCount + " records from schema '" + schema + "' in table '" + tabName + "'. [" + TimeUtils.msDiffNowToTimeStr(startTime) + "], olderThan='" + olderThan + "'." + dryRunComment);
		}
		catch(SQLException ex)
		{
			_logger.error(_prefix + "Problems deleting records from schema '" + schema + "' in table '" + tabName + "' using SQL=|" + sql + "|. Continuing anyway... Caught: " + ex);
		}
	}

}
