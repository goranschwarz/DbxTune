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
package com.dbxtune.central.pcs;

import java.io.PrintWriter;
import java.lang.invoke.MethodHandles;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.NormalExitException;
import com.dbxtune.Version;
import com.dbxtune.sql.conn.ConnectionProp;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.StringUtil;

/**
 * Check what columns in any tables that *ONLY* consists of NULL Values - If so 'drop' the column
 * 
 * - Get all tables
 * - Loop all tables
 * - Construct SQL: SELECT count(*), count(c1) as c1, count(c1) as c2, count(c1) as c3 ... from table
 * - When 'c1' == 0 then do: 
 *                   ALTER TABLE "schema"."table" DROP COLUMN "c1", "c2"...
 *               or  ALTER TABLE "schema"."table" DROP COLUMN ("c1", "c2"...)
 */
public class H2CentralDropEmptyColumns
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private DbxConnection _conn;
	private Set<SchemaTable> _schemaTableColumnsSet = new LinkedHashSet<>();

	private String  _msgPrefix = "";
	private boolean _doDrop = true;

	private int _maxTableLength = 0;
	
	public H2CentralDropEmptyColumns(DbxConnection conn)
	{
		_conn = conn;
	}

	public void setDryRun(boolean status)
	{
		// ENABLE DryRun
		if (status == true)
		{
			_doDrop = false; // Do NOT drop columns
		}
		// DISABLE DryRun
		else
		{
			_doDrop = true; // Drop columns
		}
	}

	public void   setMessagePrefix(String prefix) { _msgPrefix = prefix; }
	public String getMessagePrefix()              { return _msgPrefix; }
	
	//----------------------------------------------------------------------------------
	/**
	 * Helper class to keep INFO about ONE table
	 */
	private static class SchemaTable
	{
		String _schema;
		String _table;
		int    _rowcount;
		List<String> _columns = new ArrayList<>();
		List<String> _columnsWithAllRowsIsNull = new ArrayList<>();

		public SchemaTable(String schema, String table)
		{
			_schema = schema;
			_table  = table; 
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(_schema, _table);
		}

		@Override
		public boolean equals(Object obj)
		{
			if ( this == obj )
				return true;
			if ( obj == null )
				return false;
			if ( getClass() != obj.getClass() )
				return false;
			SchemaTable other = (SchemaTable) obj;
			return Objects.equals(_schema, other._schema) && Objects.equals(_table, other._table);
		}

		public void setRowcount(int rowcount) { _rowcount = rowcount; }
		public int  getRowcount()             { return _rowcount; }

		public void addColumn(String colName)
		{
			_columns.add(colName);
		}

		public String getSql(DbxConnection conn)
		{
			String lq = "\"";
			String rq = "\"";
			if (conn != null)
			{
				lq = conn.getLeftQuote();
				rq = conn.getRightQuote();
			}

			String sql = ""
					+ "SELECT \n"
					+ "     COUNT(*) AS ROWCOUNT \n"
					;

			for (String colName : _columns)
			{
				// Skip some columns
				if (StringUtil.equalsAny(colName, "SessionStartTime", "SessionSampleTime", "CmSampleTime"))
					continue;
				
				sql += "    ,COUNT(" + lq+colName+rq + ") AS " +lq+colName+rq+ " \n";
			}

			sql += "FROM " + lq+_schema+rq + "." + lq+_table+rq + " \n";

			return sql;
		}

		public void addAllNullsColumn(String colName)
		{
			_columnsWithAllRowsIsNull.add(colName);
		}

		public int getAllNullsColumnCount()
		{
			return _columnsWithAllRowsIsNull.size();
		}

		public String getSqlDropUnusedColumns(DbxConnection conn)
		{
			String lq = "\"";
			String rq = "\"";
			if (conn != null)
			{
				lq = conn.getLeftQuote();
				rq = conn.getRightQuote();
			}

			String sql = "ALTER TABLE " + lq+_schema+rq + "." + lq+_table+rq + " DROP COLUMN(" + StringUtil.toCommaStrQuoted(lq, rq, _columnsWithAllRowsIsNull) + ")";

			return sql;
		}
	}

	private boolean getTablesAndColumns(DbxConnection conn)
	{
		String lq = "\"";
		String rq = "\"";
		if (conn != null)
		{
			lq = conn.getLeftQuote();
			rq = conn.getRightQuote();
		}

		String sql = ""
			    + "select TABLE_SCHEMA, TABLE_NAME \n"
			    + "from INFORMATION_SCHEMA.TABLES \n"
			    + "where TABLE_NAME like 'Cm%\\_%' ESCAPE '\\' \n" // NOTE: "\\" = "\"
			    + "order by TABLE_SCHEMA, TABLE_NAME \n"
			    + "";

		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while(rs.next())
			{
				String schName = rs.getString(1);
				String tabName = rs.getString(2);

				// Used later for formatting
				int len = 1 + schName.length() + tabName.length();
				_maxTableLength = Math.max(_maxTableLength, len);

				// Skip some tables
				if (tabName.endsWith("_abs") || tabName.endsWith("_diff") || tabName.endsWith("_rate") )
					continue;
				
				SchemaTable schemaTable = new SchemaTable(schName, tabName);
				_schemaTableColumnsSet.add(schemaTable);
			}
		}
		catch (SQLException ex)
		{
			_logger.error(_msgPrefix + "Problems execution 'getTables'. ErrorCode=" + ex.getErrorCode() + ", SqlState=" + ex.getSQLState() + ", Message=|" + ex.getMessage() + "|. SQL=\n" + sql);
			return false;
		}

		// Get Columns for each table
		for (SchemaTable entry : _schemaTableColumnsSet)
		{
			String schName = entry._schema;
			String tabName = entry._table;
			
//			sql = conn.quotifySqlString("SELECT * from [" + schName + "].[" + tabName + "] WHERE 1 = 2");
			sql = "SELECT * from " + lq+schName+rq + "." + lq+tabName+rq + " WHERE 1 = 2";
			
			try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
			{
				ResultSetMetaData rsmd = rs.getMetaData();
				
				for (int c=0; c<rsmd.getColumnCount(); c++)
				{
					String colName = rsmd.getColumnLabel(c+1);
					
					entry.addColumn(colName);
				}

				while(rs.next())
				{
					// Do nothing... WHERE 1=2
				}
			}
			catch (SQLException ex)
			{
				_logger.error(_msgPrefix + "Problems execution 'getColumns'. ErrorCode=" + ex.getErrorCode() + ", SqlState=" + ex.getSQLState() + ", Message=|" + ex.getMessage() + "|. SQL=\n" + sql);
				return false;
			}
			
		}
		
		return true;
	}

	private void cleanup(DbxConnection conn)
	{
		// Loop all Tables and execute SQL Statement to check for NULL Count
		for (SchemaTable entry : _schemaTableColumnsSet)
		{
			String sql = entry.getSql(conn);

//			_logger.info(_msgPrefix + "Checking schema='" + entry._schema + "', table='" + entry._table + "' for columns with ALL NULL values.");
			
			if (_logger.isDebugEnabled())
				_logger.debug(_msgPrefix + "cleanup(): SQL: " + sql);

			try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
			{
				ResultSetMetaData rsmd = rs.getMetaData();

				// Only 1 row... but I always loop...
				while(rs.next())
				{
					for (int c=0; c<rsmd.getColumnCount(); c++)
					{
						String colName      = rsmd.getColumnLabel(c+1);
						int    notNullCount = rs.getInt(c+1);
						
						if ("ROWCOUNT".equals(colName))
							entry.setRowcount(notNullCount);

						if (_logger.isTraceEnabled())
							_logger.trace(_msgPrefix + "cleanup(): schema='" + entry._schema + "', table='" + entry._table + "', column='" + colName+ "', notNullCount=" + notNullCount);
						
						if (notNullCount == 0)
							entry.addAllNullsColumn(colName);
					}
				}

				String formattedTableName = StringUtil.left("'" + entry._schema + "." + entry._table + "'.", _maxTableLength + 3);

				if (entry._columnsWithAllRowsIsNull.isEmpty())
					_logger.info(_msgPrefix + "OK: Checked: table " + formattedTableName + " Which had NO unused columns. (rowcount=" + entry.getRowcount() + ")");
				else
					_logger.info(_msgPrefix + ">>> Checked: table " + formattedTableName + " Unused column count was " + entry._columnsWithAllRowsIsNull.size() + ". Column names that will be dropped [" + StringUtil.toCommaStrQuoted(entry._columnsWithAllRowsIsNull) + "] at the end. (rowcount=" + entry.getRowcount() + ")");
			}
			catch (SQLException ex)
			{
				_logger.error(_msgPrefix + "Problems execution 'getNullRecordsForAllColumns'. ErrorCode=" + ex.getErrorCode() + ", SqlState=" + ex.getSQLState() + ", Message=|" + ex.getMessage() + "|. SQL=\n" + sql);
			}
		}
		

		// Remember DDL's that we did
		List<String> ddlList = new ArrayList<>();

		// Loop all Tables and DROP UNUSED Columns
		for (SchemaTable entry : _schemaTableColumnsSet)
		{
			if (entry.getAllNullsColumnCount() > 0)
			{
				_logger.info(_msgPrefix + "Found ALL NULL values in: schema='" + entry._schema + "', table='" + entry._table + "' columns=[" + StringUtil.toCommaStrQuoted(entry._columnsWithAllRowsIsNull) + "]. Those columns will ALL be DROPPED.");

				String sql = entry.getSqlDropUnusedColumns(conn);

				if (_doDrop)
				{
					try (Statement stmnt = conn.createStatement())
					{
						stmnt.executeUpdate(sql);
						ddlList.add(sql);
					}
					catch (SQLException ex)
					{
						_logger.error(_msgPrefix + "Problems execution 'dropColumns' in: schema='" + entry._schema + "', table='" + entry._table + "' columns=" + entry._columnsWithAllRowsIsNull + ". ErrorCode=" + ex.getErrorCode() + ", SqlState=" + ex.getSQLState() + ", Message=|" + ex.getMessage() + "|. SQL=\n" + sql);
					}
				}
				else
				{
					_logger.info(_msgPrefix + "NO_EXEC: SQL=" + sql);
				}
			}
		}
		
		if (ddlList.isEmpty())
		{
			_logger.info("No DDL executions was done. No tables was altered.");
		}
		else
		{
			_logger.info( ddlList.size() + " DDL executions was done. Below is a list of what was done.");
			int count = 0;
			for (String sql : ddlList)
			{
				count++;
				_logger.info(_msgPrefix + "DDL[" + count + "]: " + sql);
			}
		}
	}

	public void doWork()
	{

		if (_conn == null)
		{
		//	connect();
			throw new RuntimeException("No connection was specified.");
		}

		// Get table info
		getTablesAndColumns(_conn);

		// Do Cleanup
		cleanup(_conn);

	}



	
	
	
	
	//////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Print command line options.
	 * @param options
	 */
	public static void printHelp(Options options, String errorStr)
	{
		PrintWriter pw = new PrintWriter(System.out);

		if (errorStr != null)
		{
			pw.println();
			pw.println(errorStr);
			pw.println();
		}

		pw.println("usage: " + Version.getAppName() + " [-h] [-v] [-x] ");
		pw.println("              [-U <user>]  [-P <passwd>]  [-u <url>]");
		pw.println("              [-d|--dryrun");
//		pw.println("              [-L <logfile>] [-D <key=val>]");
		pw.println("  ");
		pw.println("options:");
		pw.println("  -h,--help                   Usage information.");
		pw.println("  -v,--version                Display " + Version.getAppName() + " and JVM Version.");
		pw.println("  -x,--debug <dbg1,dbg2>      Debug options: a comma separated string");
		pw.println("                              To get available option, do -x list");
		pw.println("  ");
		pw.println("  -d,--dry-run                Do not execute 'ALTER TABLE ... DROP COLUMN ...'");
		pw.println("  ");
		pw.println("  -U,--user <user>            Username when connecting to server.");
		pw.println("  -P,--passwd <passwd>        Password when connecting to server. null=noPasswd");
		pw.println("  -u,--url <url>              JDBC URL when connecting to server");
		pw.println("  ");
//		pw.println("  -L,--logfile <filename>     Name of the logfile where application logging is saved.");
		pw.println("  -D,--javaSystemProp <k=v>   Set Java System Property, same as java -Dkey=value");
		pw.println("  ");
		pw.flush();
	}

	/**
	 * Build the options parser. Has to be synchronized because of the way
	 * Options are constructed.
	 *
	 * @return an options parser.
	 */
	public static synchronized Options buildCommandLineOptions()
	{
		Options options = new Options();

		// create the Options
		options.addOption( Option.builder("h").longOpt("help"          ).hasArg(false).build() );
		options.addOption( Option.builder("v").longOpt("version"       ).hasArg(false).build() );
		options.addOption( Option.builder("x").longOpt("debug"         ).hasArg(true ).build() );

		options.addOption( Option.builder("U").longOpt("user"          ).hasArg(true ).build() );
		options.addOption( Option.builder("P").longOpt("passwd"        ).hasArg(true ).build() );
		options.addOption( Option.builder("u").longOpt("url"           ).hasArg(true ).build() );

		options.addOption( Option.builder("d").longOpt("dry-run"       ).hasArg(false).build() );

//		options.addOption( Option.builder("L").longOpt("logfile"       ).hasArg(true ).build() );
		options.addOption( Option.builder("D").longOpt("javaSystemProp").hasArgs().valueSeparator('=').build() ); // NOTE the hasArgs() instead of hasArg() *** the 's' at the end of hasArg<s>() does the trick...

		return options;
	}


	//---------------------------------------------------
	// Command Line Parsing
	//---------------------------------------------------
	public static CommandLine parseCommandLine(String[] args, Options options)
	throws ParseException
	{
		// create the command line parser
		CommandLineParser parser = new DefaultParser();

		// parse the command line arguments
		CommandLine cmd = parser.parse( options, args );

		if (_logger.isDebugEnabled())
		{
			for (Iterator<Option> it=cmd.iterator(); it.hasNext();)
			{
				Option opt = it.next();
				_logger.debug("parseCommandLine: swith='" + opt.getOpt() + "', value='" + opt.getValue() + "'.");
			}
		}

		return cmd;
	}

	//---------------------------------------------------
	// MAIN
	//---------------------------------------------------
	public static void main(String[] args)
	{
		Version.setAppName(H2CentralDropEmptyColumns.class.getSimpleName());
		
		Options options = buildCommandLineOptions();
		try
		{
			CommandLine cmd = parseCommandLine(args, options);

			//-------------------------------
			// HELP
			//-------------------------------
			if ( cmd.hasOption("help") )
			{
				printHelp(options, "The option '--help' was passed.");
			}
			//-------------------------------
			// VERSION
			//-------------------------------
			else if ( cmd.hasOption("version") )
			{
				System.out.println();
				System.out.println(Version.getAppName() + " Version: " + Version.getVersionStr() + " JVM: " + System.getProperty("java.version"));
				System.out.println();
			}
			//-------------------------------
			// Check for correct number of cmd line parameters
			//-------------------------------
			else if ( cmd.getArgs() != null && cmd.getArgs().length > 0 )
			{
//				String error = "Unknown options: " + StringUtil.toCommaStr(cmd.getArgs());
				String error = "Unknown options: " + Arrays.toString(cmd.getArgs());
				printHelp(options, error);
			}
			//-------------------------------
			// Start 
			//-------------------------------
			else
			{
//				String url = "jdbc:h2:tcp://gorans-ub3.home/DBXTUNE_CENTRAL_DB";
				String url = "jdbc:h2:tcp://localhost/DBXTUNE_CENTRAL_DB";
				String username = "sa";
				String password = "";

				if (cmd.hasOption("user"))   username = cmd.getOptionValue("user");
				if (cmd.hasOption("passwd")) password = cmd.getOptionValue("passwd");
				if (cmd.hasOption("url"))    url      = cmd.getOptionValue("url");
				
				ConnectionProp conProp = new ConnectionProp();
				conProp.setUsername(username);
				conProp.setPassword(password);
				conProp.setUrl(url);

				DbxConnection conn;
				
				_logger.info("Connecting to URL='" + url + "', username='" + username + "', password='*secret*'");
				try 
				{
					conn = DbxConnection.connect(null, conProp);
				} 
				catch (Exception ex) 
				{
					_logger.error("Problems connecting to URL='" + url + "', username='" + username + "', password='" + password + "'.");
					_logger.error("Caught: " + ex);
					_logger.error("Can NOT Continue, exiting...");
					
					throw new NormalExitException();
				}
				
				// Create object
				H2CentralDropEmptyColumns cleaner = new H2CentralDropEmptyColumns(conn);

				// Set options
				if (cmd.hasOption("dry-run"))
				{
					_logger.info("Executing in DRY-RUN mode. The DDL Statement to drop un-used columns will NOT be executed. Instead they will be printed to the output.");
					cleaner.setDryRun(true);
				}

				// DO WORK
				cleaner.doWork();
					
				_logger.info("End of processing in '" + H2CentralDropEmptyColumns.class.getSimpleName() + "'.");
			}
		}
		catch (ParseException pe)
		{
			String error = "Error: " + pe.getMessage();
			printHelp(options, error);
			System.exit(1);
		}
		catch (NormalExitException e)
		{
			// This was probably throws when checking command line parameters in the underlying DbxTune initialization: init(cmd)
			// do normal exit
			System.exit(1);
		}
		catch (Exception e)
		{
			System.out.println();
			System.out.println("Error: " + e.getMessage());
			System.out.println();
			System.out.println("Printing a stacktrace, where the error occurred.");
			System.out.println("--------------------------------------------------------------------");
			e.printStackTrace();
			System.out.println("--------------------------------------------------------------------");
			System.exit(1);
		}
	}
}


/* 
 * Below is an example of what SQL Statement we execute to check for Empty Columns (only NULL values in column)
 *
select
     count(*)                                             AS "ROWCOUNT"
    ,count("pagelatch_sh")                                AS "pagelatch_sh"
    ,count("pagelatch_ex")                                AS "pagelatch_ex"
    ,count("pageiolatch_sh")                              AS "pageiolatch_sh"
    ,count("sos_scheduler_yield")                         AS "sos_scheduler_yield"
    ,count("reserved_memory_allocation_ext")              AS "reserved_memory_allocation_ext"
    ,count("async_network_io")                            AS "async_network_io"
    ,count("writelog")                                    AS "writelog"
    ,count("preemptive_os_cryptacquirecontext")           AS "preemptive_os_cryptacquirecontext"
    ,count("preemptive_os_netvalidatepasswordpolicy")     AS "preemptive_os_netvalidatepasswordpolicy"
    ,count("preemptive_os_netvalidatepasswordpolicyfree") AS "preemptive_os_netvalidatepasswordpolicyfree"
    ,count("threadpool")                                  AS "threadpool"
    ,count("xe_timer_mutex")                              AS "xe_timer_mutex"
    ,count("preemptive_os_querycontextattributes")        AS "preemptive_os_querycontextattributes"
    ,count("preemptive_os_authorizationops")              AS "preemptive_os_authorizationops"
    ,count("preemptive_os_lookupaccountsid")              AS "preemptive_os_lookupaccountsid"
    ,count("preemptive_os_reverttoself")                  AS "preemptive_os_reverttoself"
    ,count("preemptive_os_closehandle")                   AS "preemptive_os_closehandle"
    ,count("preemptive_os_createfile")                    AS "preemptive_os_createfile"
    ,count("preemptive_os_findfile")                      AS "preemptive_os_findfile"
    ,count("sqltrace_file_write_io_completion")           AS "sqltrace_file_write_io_completion"
    ,count("msql_xp")                                     AS "msql_xp"
    ,count("preemptive_os_getprocaddress")                AS "preemptive_os_getprocaddress"
    ,count("preemptive_os_loadlibrary")                   AS "preemptive_os_loadlibrary"
    ,count("wait_on_sync_statistics_refresh")             AS "wait_on_sync_statistics_refresh"
    ,count("pageiolatch_ex")                              AS "pageiolatch_ex"
    ,count("logmgr_flush")                                AS "logmgr_flush"
    ,count("preemptive_os_deletesecuritycontext")         AS "preemptive_os_deletesecuritycontext"
    ,count("pageiolatch_up")                              AS "pageiolatch_up"
    ,count("lck_m_sch_m")                                 AS "lck_m_sch_m"
    ,count("sos_small_page_alloc")                        AS "sos_small_page_alloc"
    ,count("latch_sh")                                    AS "latch_sh"
    ,count("io_completion")                               AS "io_completion"
    ,count("preemptive_os_getdiskfreespace")              AS "preemptive_os_getdiskfreespace"
    ,count("write_completion")                            AS "write_completion"
    ,count("latch_ex")                                    AS "latch_ex"
    ,count("cmemthread")                                  AS "cmemthread"
    ,count("sni_critical_section")                        AS "sni_critical_section"
    ,count("qds_bckg_task")                               AS "qds_bckg_task"
    ,count("deadlock_enum_mutex")                         AS "deadlock_enum_mutex"
    ,count("resource_semaphore_mutex")                    AS "resource_semaphore_mutex"
    ,count("qry_profile_list_mutex")                      AS "qry_profile_list_mutex"
    ,count("sqltrace_file_buffer")                        AS "sqltrace_file_buffer"
    ,count("xe_session_sync")                             AS "xe_session_sync"
    ,count("pagelatch_up")                                AS "pagelatch_up"
    ,count("sos_process_affinity_mutex")                  AS "sos_process_affinity_mutex"
    ,count("xe_session_flush")                            AS "xe_session_flush"
    ,count("lck_m_x")                                     AS "lck_m_x"
    ,count("preemptive_os_setfilevaliddata")              AS "preemptive_os_setfilevaliddata"
    ,count("sleep_memorypool_allocatepages")              AS "sleep_memorypool_allocatepages"
    ,count("waitstat_mutex")                              AS "waitstat_mutex"
    ,count("preemptive_os_getfinalfilepathbyhandle")      AS "preemptive_os_getfinalfilepathbyhandle"
    ,count("preemptive_authenticate_launchpadd")          AS "preemptive_authenticate_launchpadd"
    ,count("lck_m_s")                                     AS "lck_m_s"
    ,count("lck_m_sch_s")                                 AS "lck_m_sch_s"
    ,count("preemptive_filesizeget")                      AS "preemptive_filesizeget"
    ,count("preemptive_oledbops")                         AS "preemptive_oledbops"
    ,count("preemptive_service_control_manger")           AS "preemptive_service_control_manger"
    ,count("sleep_bufferpool_helplw")                     AS "sleep_bufferpool_helplw"
    ,count("cxpacket")                                    AS "cxpacket"
    ,count("session_wait_stats_children")                 AS "session_wait_stats_children"
    ,count("sleep_bpool_steal")                           AS "sleep_bpool_steal"
    ,count("clr_task_start")                              AS "clr_task_start"
    ,count("clr_crst")                                    AS "clr_crst"
    ,count("performance_counters_rwlock")                 AS "performance_counters_rwlock"
    ,count("sos_worker_migration")                        AS "sos_worker_migration"
    ,count("async_io_completion")                         AS "async_io_completion"
    ,count("preemptive_os_getfileattributes")             AS "preemptive_os_getfileattributes"
    ,count("tracewrite")                                  AS "tracewrite"
    ,count("backupio")                                    AS "backupio"
    ,count("backupthread")                                AS "backupthread"
    ,count("preemptive_os_getvolumepathname")             AS "preemptive_os_getvolumepathname"
    ,count("xe_file_target_tvf")                          AS "xe_file_target_tvf"
    ,count("broker_shutdown")                             AS "broker_shutdown"
    ,count("hadr_filestream_iomgr")                       AS "hadr_filestream_iomgr"
    ,count("hadr_notification_worker_termination_sync")   AS "hadr_notification_worker_termination_sync"
    ,count("security_cng_provider_mutex")                 AS "security_cng_provider_mutex"
    ,count("deadlock_task_search")                        AS "deadlock_task_search"
    ,count("logpool_consumer")                            AS "logpool_consumer"
    ,count("preemptive_os_deletefile")                    AS "preemptive_os_deletefile"
    ,count("preemptive_os_removedirectory")               AS "preemptive_os_removedirectory"
    ,count("async_diskpool_lock")                         AS "async_diskpool_lock"
    ,count("latch_up")                                    AS "latch_up"
    ,count("broker_masterstart")                          AS "broker_masterstart"
    ,count("logbuffer")                                   AS "logbuffer"
    ,count("impprov_iowait")                              AS "impprov_iowait"
    ,count("backupbuffer")                                AS "backupbuffer"
    ,count("dac_init")                                    AS "dac_init"
    ,count("sqltrace_file_read_io_completion")            AS "sqltrace_file_read_io_completion"
    ,count("qds_async_persist_task_start")                AS "qds_async_persist_task_start"
    ,count("sos_sync_task_enqueue_event")                 AS "sos_sync_task_enqueue_event"
    ,count("qds_dyn_vector")                              AS "qds_dyn_vector"
from [gorans-ub3-ss].[CmWaitStats_WaitTypeTime]


+---------+------------+------------+--------------+-------------------+------------------------------+----------------+---------+---------------------------------+---------------------------------------+-------------------------------------------+----------+--------------+------------------------------------+------------------------------+------------------------------+--------------------------+-------------------------+------------------------+----------------------+---------------------------------+---------+----------------------------+-------------------------+-------------------------------+--------------+------------+-----------------------------------+--------------+-----------+--------------------+---------+-------------+------------------------------+----------------+--------+----------+--------------------+-------------+-------------------+------------------------+----------------------+--------------------+---------------+------------+--------------------------+----------------+-------+------------------------------+------------------------------+--------------+--------------------------------------+----------------------------------+-------+-----------+----------------------+-------------------+---------------------------------+-----------------------+--------+---------------------------+-----------------+--------------+--------+---------------------------+--------------------+-------------------+-------------------------------+----------+--------+------------+-------------------------------+------------------+---------------+---------------------+-----------------------------------------+---------------------------+--------------------+----------------+------------------------+-----------------------------+-------------------+--------+------------------+---------+--------------+------------+--------+--------------------------------+----------------------------+---------------------------+--------------+
|ROWCOUNT |pagelatch_sh|pagelatch_ex|pageiolatch_sh|sos_scheduler_yield|reserved_memory_allocation_ext|async_network_io|writelog |preemptive_os_cryptacquirecontext|preemptive_os_netvalidatepasswordpolicy|preemptive_os_netvalidatepasswordpolicyfree|threadpool|xe_timer_mutex|preemptive_os_querycontextattributes|preemptive_os_authorizationops|preemptive_os_lookupaccountsid|preemptive_os_reverttoself|preemptive_os_closehandle|preemptive_os_createfile|preemptive_os_findfile|sqltrace_file_write_io_completion|msql_xp  |preemptive_os_getprocaddress|preemptive_os_loadlibrary|wait_on_sync_statistics_refresh|pageiolatch_ex|logmgr_flush|preemptive_os_deletesecuritycontext|pageiolatch_up|lck_m_sch_m|sos_small_page_alloc|latch_sh |io_completion|preemptive_os_getdiskfreespace|write_completion|latch_ex|cmemthread|sni_critical_section|qds_bckg_task|deadlock_enum_mutex|resource_semaphore_mutex|qry_profile_list_mutex|sqltrace_file_buffer|xe_session_sync|pagelatch_up|sos_process_affinity_mutex|xe_session_flush|lck_m_x|preemptive_os_setfilevaliddata|sleep_memorypool_allocatepages|waitstat_mutex|preemptive_os_getfinalfilepathbyhandle|preemptive_authenticate_launchpadd|lck_m_s|lck_m_sch_s|preemptive_filesizeget|preemptive_oledbops|preemptive_service_control_manger|sleep_bufferpool_helplw|cxpacket|session_wait_stats_children|sleep_bpool_steal|clr_task_start|clr_crst|performance_counters_rwlock|sos_worker_migration|async_io_completion|preemptive_os_getfileattributes|tracewrite|backupio|backupthread|preemptive_os_getvolumepathname|xe_file_target_tvf|broker_shutdown|hadr_filestream_iomgr|hadr_notification_worker_termination_sync|security_cng_provider_mutex|deadlock_task_search|logpool_consumer|preemptive_os_deletefile|preemptive_os_removedirectory|async_diskpool_lock|latch_up|broker_masterstart|logbuffer|impprov_iowait|backupbuffer|dac_init|sqltrace_file_read_io_completion|qds_async_persist_task_start|sos_sync_task_enqueue_event|qds_dyn_vector|
+---------+------------+------------+--------------+-------------------+------------------------------+----------------+---------+---------------------------------+---------------------------------------+-------------------------------------------+----------+--------------+------------------------------------+------------------------------+------------------------------+--------------------------+-------------------------+------------------------+----------------------+---------------------------------+---------+----------------------------+-------------------------+-------------------------------+--------------+------------+-----------------------------------+--------------+-----------+--------------------+---------+-------------+------------------------------+----------------+--------+----------+--------------------+-------------+-------------------+------------------------+----------------------+--------------------+---------------+------------+--------------------------+----------------+-------+------------------------------+------------------------------+--------------+--------------------------------------+----------------------------------+-------+-----------+----------------------+-------------------+---------------------------------+-----------------------+--------+---------------------------+-----------------+--------------+--------+---------------------------+--------------------+-------------------+-------------------------------+----------+--------+------------+-------------------------------+------------------+---------------+---------------------+-----------------------------------------+---------------------------+--------------------+----------------+------------------------+-----------------------------+-------------------+--------+------------------+---------+--------------+------------+--------+--------------------------------+----------------------------+---------------------------+--------------+
|1 215 482|   1 215 482|   1 215 482|       590 538|          1 215 482|                     1 215 482|       1 215 482|1 212 117|                        1 193 756|                              1 193 670|                                  1 193 670| 1 190 987|     1 151 846|                           1 215 393|                     1 215 393|                       146 176|                 1 215 393|                1 215 399|               1 057 487|             1 038 373|                          970 852|1 201 755|                   1 201 755|                   89 507|                      1 213 297|     1 059 214|   1 194 334|                          1 215 399|     1 207 836|    106 519|           1 152 374|1 171 954|      814 162|                       564 596|         533 344| 899 869| 1 166 594|           1 192 749|      250 853|            336 999|                 505 067|               168 586|             980 791|              0|     649 442|                   324 332|         139 867|221 374|                        68 652|                       265 661|         2 188|                               409 257|                                 0|112 484|     16 461|               146 267|            146 267|                        1 103 133|                      0|  89 016|                     89 016|                0|             0|  30 619|                      7 990|              38 614|                  0|                              0|         0|       0|           0|                              0|                 0|              0|                    0|                                        0|                          0|             146 716|          81 521|                       0|                            0|                  0|       0|                 0|   14 653|       146 267|           0|       0|                         146 267|                      81 521|                     30 619|         5 529|
+---------+------------+------------+--------------+-------------------+------------------------------+----------------+---------+---------------------------------+---------------------------------------+-------------------------------------------+----------+--------------+------------------------------------+------------------------------+------------------------------+--------------------------+-------------------------+------------------------+----------------------+---------------------------------+---------+----------------------------+-------------------------+-------------------------------+--------------+------------+-----------------------------------+--------------+-----------+--------------------+---------+-------------+------------------------------+----------------+--------+----------+--------------------+-------------+-------------------+------------------------+----------------------+--------------------+---------------+------------+--------------------------+----------------+-------+------------------------------+------------------------------+--------------+--------------------------------------+----------------------------------+-------+-----------+----------------------+-------------------+---------------------------------+-----------------------+--------+---------------------------+-----------------+--------------+--------+---------------------------+--------------------+-------------------+-------------------------------+----------+--------+------------+-------------------------------+------------------+---------------+---------------------+-----------------------------------------+---------------------------+--------------------+----------------+------------------------+-----------------------------+-------------------+--------+------------------+---------+--------------+------------+--------+--------------------------------+----------------------------+---------------------------+--------------+
Rows 1

 */
