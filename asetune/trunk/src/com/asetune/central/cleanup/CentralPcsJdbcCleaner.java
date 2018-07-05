package com.asetune.central.cleanup;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.time.DateUtils;
import org.apache.log4j.Logger;

import com.asetune.central.pcs.CentralPcsWriterHandler;
import com.asetune.central.pcs.CentralPersistWriterBase;
import com.asetune.central.pcs.CentralPersistWriterBase.Table;
import com.asetune.central.pcs.CentralPersistWriterJdbc;
import com.asetune.central.pcs.ICentralPersistWriter;
import com.asetune.sql.conn.ConnectionProp;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;
import com.asetune.utils.TimeUtils;

import it.sauronsoftware.cron4j.Task;
import it.sauronsoftware.cron4j.TaskExecutionContext;

public class CentralPcsJdbcCleaner
extends Task
{
	private static Logger _logger = Logger.getLogger(CentralPcsJdbcCleaner.class);

	public static final String  PROPKEY_start = "CentralPcsJdbcCleaner.start";
	public static final boolean DEFAULT_start = true;

	public static final String PROPKEY_cron = "CentralPcsJdbcCleaner.cron";
//	public static final String DEFAULT_cron = "30 01 1 * *"; // 01:30 first day of the month
	public static final String DEFAULT_cron = "30 23 * * 0"; // 23:30 Sunday
//	public static final String DEFAULT_cron = "* * * * *";

	public static final String PROPKEY_keepDays = "CentralPcsJdbcCleaner.keep.days";
	public static final int    DEFAULT_keepDays = 365;
//	public static final int    DEFAULT_keepDays = 60;

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
			_logger.info("Open DBMS connection to CentralPersistWriterJdbc. connProps="+connProps);
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
		String q = conn.getQuotedIdentifierChar();

		int keepDays = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_keepDays, DEFAULT_keepDays);
		Timestamp olderThan = new Timestamp( DateUtils.addDays(new Date(), -keepDays).getTime() );
		_logger.info(_prefix + "Retention period is "+keepDays+", so data that is older than "+keepDays+" days (or older than '"+olderThan+"') will be deleted. This can be changed with property '"+PROPKEY_keepDays+"'.");

		// Get "ServerNames" so we know what DBMS schemas we should iterate
		String tabName = CentralPersistWriterBase.getTableName(null, Table.CENTRAL_SESSIONS, null, true);
		String sql = "select distinct "+q+"ServerName"+q+" from "+tabName+" order by 1";
		List<String> schemaList = new ArrayList<>();
		// autoclose: stmnt, rs
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while (rs.next())
				schemaList.add(rs.getString(1));
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
		String q = conn.getQuotedIdentifierChar();
		String tabName;
		String sql;

		// Get expected number of samples to delete
		int sessionsSampleCount = -99;
		tabName = CentralPersistWriterBase.getTableName(schema, Table.SESSION_SAMPLES, null, true);
		sql = "select count(*) from "+tabName+" where "+q+"SessionSampleTime"+q+" < ?";
		try (PreparedStatement pstmnt = conn.prepareStatement(sql))
		{
			pstmnt.setTimestamp(1, olderThan);
			try (ResultSet rs = pstmnt.executeQuery())
			{
				while (rs.next())
					sessionsSampleCount = rs.getInt(1);
			}
		}
		_logger.info(_prefix + "schema='"+schema+"': Found "+sessionsSampleCount+" samples was found that was older than '"+olderThan+"'.");
		if (sessionsSampleCount <= 0)
		{
			_logger.info(_prefix + "schema='"+schema+"': ---- Nothing to cleanup in schema '"+schema+"'.");
			return 0;
		}

		//--------------------------------------------
		// OK: Lets start to delete stuff!
		//--------------------------------------------
		
		// Get "graph" tables
		List<String> graphTables = new ArrayList<>();
		tabName = CentralPersistWriterBase.getTableName(schema, Table.GRAPH_PROPERTIES, null, true);
		sql = "select distinct "+q+"TableName"+q+" from "+tabName+" order by 1";
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while (rs.next())
				graphTables.add(rs.getString(1));
		}

		// Iterate over Graph Tables and remove records
		for (String name : graphTables)
		{
			// TODO: delete TOP #### from ... in a loop so we do not use up to much transaction log for some databases
			
			sql = "delete from "+q+schema+q+"."+q+name+q+" where "+q+"SessionSampleTime"+q+" < ?";

			String dryRunComment = "";
			if (_dryRun)
			{
				sql += " and 1=2 -- DRY-RUN";
				dryRunComment = "... DRY-RUN: SQL=|"+sql+"|, olderThan='"+olderThan+"'.";
			}

			long startTime = System.currentTimeMillis();
			try (PreparedStatement pstmnt = conn.prepareStatement(sql))
			{
				pstmnt.setTimestamp(1, olderThan);
				int delCount = pstmnt.executeUpdate();
				_logger.info(_prefix + ">>>> Deleted "+delCount+" records from schema '"+schema+"' in table '"+name+"'. ["+TimeUtils.msDiffNowToTimeStr(startTime)+"]" + dryRunComment);
			}
		}
		
		// Remove records from "system" metadata tables in SCHEMA
	//	doCleanupForDbxTableInSchema(conn, schema, Table.ALARM_ACTIVE,           olderThan);
		doCleanupForDbxTableInSchema(conn, schema, Table.ALARM_HISTORY,          olderThan);
		doCleanupForDbxTableInSchema(conn, schema, Table.GRAPH_PROPERTIES,       olderThan);
		doCleanupForDbxTableInSchema(conn, schema, Table.SESSION_SAMPLE_DETAILS, olderThan);
		doCleanupForDbxTableInSchema(conn, schema, Table.SESSION_SAMPLE_SUM,     olderThan);
		doCleanupForDbxTableInSchema(conn, schema, Table.SESSION_SAMPLES,        olderThan);
		
		_logger.info(_prefix + "schema='"+schema+"': << Done cleanup. sessionsSampleCount="+sessionsSampleCount);

		return sessionsSampleCount;
	}

	private int doCleanupForDbxTableInSchema(DbxConnection conn, String schema, Table table, Timestamp olderThan)
	{
		String q = conn.getQuotedIdentifierChar();
		String fullTabName  = CentralPersistWriterBase.getTableName(schema, table, null, true);
		String shortTabName = CentralPersistWriterBase.getTableName(null,   table, null, false);

		String sql = null;
//		String sql = "delete from "+fullTabName+" where "+q+"SessionSampleTime"+q+" < ?";
		if      (Table.ALARM_ACTIVE          .equals(table)) return 0; // Do not delete from this
		else if (Table.ALARM_HISTORY         .equals(table)) sql = "delete from "+fullTabName+" where "+q+"SessionSampleTime"+q+" < ?";
		else if (Table.GRAPH_PROPERTIES      .equals(table)) sql = "delete from "+fullTabName+" where "+q+"SessionStartTime"+q+" < ?";
		else if (Table.SESSION_SAMPLE_DETAILS.equals(table)) sql = "delete from "+fullTabName+" where "+q+"SessionSampleTime"+q+" < ?";
		else if (Table.SESSION_SAMPLES       .equals(table)) sql = "delete from "+fullTabName+" where "+q+"SessionSampleTime"+q+" < ?";
		else if (Table.SESSION_SAMPLE_SUM    .equals(table)) sql = "delete from "+fullTabName+" where "+q+"SessionStartTime"+q+" < ?";

		if (StringUtil.isNullOrBlank(sql))
			return 0;

		String dryRunComment = "";
		if (_dryRun)
		{
			sql += " and 1=2 -- DRY-RUN";
			dryRunComment = "... DRY-RUN: SQL=|"+sql+"|, olderThan='"+olderThan+"'.";
		}
			
		int delCount = -99;
		try (PreparedStatement pstmnt = conn.prepareStatement(sql))
		{
			pstmnt.setTimestamp(1, olderThan);
			delCount = pstmnt.executeUpdate();
			_logger.info(_prefix + ">>>> Deleted "+delCount+" records from schema '"+schema+"' in table '"+shortTabName+"'." + dryRunComment);
		}
		catch(SQLException ex)
		{
			_logger.error(_prefix + "Problems deleting records from schema '"+schema+"' in table '"+shortTabName+"' using SQL=|"+sql+"|. Continuing anyway... Caught: "+ex);
		}
		return delCount;
	}
	
	private int doCleanupForCentralDbxTable(DbxConnection conn, Table table, Timestamp olderThan)
	{
		String q = conn.getQuotedIdentifierChar();
		String fullTabName  = CentralPersistWriterBase.getTableName(null, table, null, true);
		String shortTabName = CentralPersistWriterBase.getTableName(null, table, null, false);

		String sql = null;
//		String sql = "delete from "+fullTabName+" where "+q+"SessionSampleTime"+q+" < ?";
		if      (Table.CENTRAL_VERSION_INFO  .equals(table)) return 0; // Do not delete from this
		else if (Table.CENTRAL_GRAPH_PROFILES.equals(table)) return 0; // Do not delete from this
		else if (Table.CENTRAL_SESSIONS      .equals(table)) sql = "delete from "+fullTabName+" where "+q+"SessionStartTime"+q+" < ?";

		if (StringUtil.isNullOrBlank(sql))
			return 0;

		String dryRunComment = "";
		if (_dryRun)
		{
			sql += " and 1=2 -- DRY-RUN";
			dryRunComment = "... DRY-RUN: SQL=|"+sql+"|, olderThan='"+olderThan+"'.";
		}
			
		int delCount = -99;
		try (PreparedStatement pstmnt = conn.prepareStatement(sql))
		{
			pstmnt.setTimestamp(1, olderThan);
			delCount = pstmnt.executeUpdate();
			_logger.info(_prefix + ">>>> Deleted "+delCount+" records from DbxCentral table '"+shortTabName+"'." + dryRunComment);
		}
		catch(SQLException ex)
		{
			_logger.error(_prefix + "Problems deleting records from DbxCentral table '"+shortTabName+"' using SQL=|"+sql+"|. Continuing anyway... Caught: "+ex);
		}
		return delCount;
	}
	
}
