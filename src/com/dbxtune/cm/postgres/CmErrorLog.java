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
package com.dbxtune.cm.postgres;

import java.awt.event.MouseEvent;
import java.lang.invoke.MethodHandles;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import javax.naming.NameNotFoundException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.alarm.AlarmHandler;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.alarm.events.AlarmEventConfigChanges;
import com.dbxtune.alarm.events.postgres.AlarmEventPgDeadlock;
import com.dbxtune.alarm.events.postgres.AlarmEventPgInsufficientResources;
import com.dbxtune.alarm.events.postgres.AlarmEventPgInternalError;
import com.dbxtune.alarm.events.postgres.AlarmEventPgSystemError;
import com.dbxtune.cache.XmlPlanCache;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CounterSample;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.CountersModelAppend;
import com.dbxtune.config.dict.MonTablesDictionary;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.config.dict.PostgresErrorCodeDictionary;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.gui.TabularCntrPanelAppend;
import com.dbxtune.pcs.PersistentCounterHandler;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmErrorLog
extends CountersModelAppend
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	// This needs to go "at top" -- if we want to use it in below "HTML_DESC"
	private static String pg_srv_errorlog(String type, String currentLogFile, String newLogFile, String username)
	{
		if (currentLogFile == null) currentLogFile = "log/CURRENT_LOG_FILE.csv";
		if (newLogFile     == null) newLogFile     = "";
		if (username       == null) username       = "";

		String CREATE_extension = "" +
				"  -- Create extension 'file_fdw' \n" +
				"  DO $$ BEGIN \n" +
				"      IF NOT EXISTS (SELECT * FROM pg_extension WHERE extname = 'file_fdw') \n" +
				"      THEN \n" +
				"          CREATE EXTENSION file_fdw; \n" +
				"      END IF; \n" +
				"  END; $$; \n" +
				"  \n";
		
		String CREATE_server = "" +
				"  -- Create server name 'pg_srv_errorlog' for the below foreign table \n" +
				"  DO $$ BEGIN \n" +
				"      IF NOT EXISTS (SELECT * FROM pg_foreign_server WHERE srvname = 'pg_srv_errorlog') \n" +
				"      THEN \n" +
				"          CREATE SERVER pg_srv_errorlog FOREIGN DATA WRAPPER file_fdw; \n" +
				"      END IF; \n" +
				"  END; $$; \n" +
				"  \n";

		String CREATE_table = "" +
				"  -- Create foreign table 'pg_srv_errorlog' to access the Postgres log file \n" +
				"  CREATE FOREIGN TABLE IF NOT EXISTS pg_srv_errorlog \n" +
				"  (\n" +
				"      log_time                 timestamp(3) with time zone,\n" +
				"      user_name                text,                       \n" +
				"      database_name            text,                       \n" +
				"      process_id               integer,                    \n" +
				"      connection_from          text,                       \n" +
				"      session_id               text,                       \n" +
				"      session_line_num         bigint,                     \n" +
				"      command_tag              text,                       \n" +
				"      session_start_time       timestamp with time zone,   \n" +
				"      virtual_transaction_id   text,                       \n" +
				"      transaction_id           bigint,                     \n" +
				"      error_severity           text,                       \n" +
				"      sql_state_code           text,                       \n" +
				"      message                  text,                       \n" +
				"      detail                   text,                       \n" +
				"      hint                     text,                       \n" +
				"      internal_query           text,                       \n" +
				"      internal_query_pos       integer,                    \n" +
				"      context                  text,                       \n" +
				"      query                    text,                       \n" +
				"      query_pos                integer,                    \n" +
				"      location                 text,                       \n" +
				"      application_name         text,                       \n" +
				"      backend_type             text,                       \n" +
				"      leader_pid               integer,                    \n" +
				"      query_id                 bigint                      \n" +
				"  ) SERVER pg_srv_errorlog                                 \n" +
				"  OPTIONS ( filename 'log/CURRENT_LOG_FILE.csv', format 'csv' ); \n" +
				"  \n";

		String CREATE_grant = "" +
				"  -- And if the monitoring user is *not* an superuser      \n" +
				"  GRANT EXECUTE ON FUNCTION pg_current_logfile(text) TO <username>; \n" +
				"  ALTER TABLE pg_srv_errorlog OWNER TO <username>;   \n" +
				"  GRANT pg_read_server_files TO <username>;          \n" +
		        "  \n";

		String CREATE_alter = "" +
				"  -- The above since we need to alter file name when it changes on 'rollover' \n" +
				"  ALTER FOREIGN TABLE pg_srv_errorlog OPTIONS ( SET filename 'log/NEW_LOG_FILE.csv' ); \n" +
				"  \n";

		if      ("extension".equalsIgnoreCase(type)) { return CREATE_extension; }
		else if ("server"   .equalsIgnoreCase(type)) { return CREATE_server;    }
		else if ("table"    .equalsIgnoreCase(type)) { return CREATE_table.replace("log/CURRENT_LOG_FILE.csv", currentLogFile); }
		else if ("grant"    .equalsIgnoreCase(type)) { return CREATE_grant; }
		else if ("alter"    .equalsIgnoreCase(type)) { return CREATE_alter.replace("log/NEW_LOG_FILE.csv", newLogFile); }
		else if ("tooltip"  .equalsIgnoreCase(type))
		{
			return ""
				+ CREATE_extension
				+ CREATE_server
				+ CREATE_table
				+ CREATE_grant.replace("<", "&lt;").replace(">", "&gt;")
				+ CREATE_alter
				;
		}
		else if ("to_errorlog".equalsIgnoreCase(type))
		{
			return ""
				+ CREATE_extension
				+ CREATE_server
				+ CREATE_table
				+ CREATE_grant
				+ CREATE_alter
				;
		}
		else
		{
			throw new RuntimeException("Unknown type '" + type + "'.");
		}
	}
	
	public static final String   CM_NAME          = CmErrorLog.class.getSimpleName();
	public static final String   SHORT_NAME       = "Errorlog";
	public static final String   HTML_DESC        = 
		"<html>" +
		"Look at the Postgres errorlog.<br>" +
		"<b>Note</b>: You must have enabled CSV or JSON logging.<br>" +
		"<br>" +
		"Suggested configuration:" +
		"<pre>" +
		"  ALTER SYSTEM SET logging_collector TO 'on'             \n" +
		"  ALTER SYSTEM SET log_destination   TO 'stderr,csvlog'  \n" +
		"  --OR: you can use jsonlog in version 16 and above      \n" +
		"  --ALTER SYSTEM SET log_destination TO 'stderr,jsonlog' \n" +
		"</pre>" +
		"<br>" +

		"<b>For CSV Logging: </b><br>" + 
		"We will use extention 'file_fdw' (which is part of the contib package) <br>" +
		"And create a table 'pg_srv_errorlog'<br>" +
		"This to be able to read the error log via a SQL Connection.<br>" +
		"<pre>" +
		pg_srv_errorlog("tooltip", "", "", "") +
		"</pre>" +
		"<br>" +
		"<br>" +

		"<b>For JSON Logging: </b><br>" + 
		"You may also need to grant 'execute' on two system functions (if you are <b>not</b> connected as <i>superuser</i>)<br>" +
		"This to be able to read the error log via a SQL Connection.<br>" +
		"<pre>" +
		"  GRANT EXECUTE ON FUNCTION pg_current_logfile(text) TO &lt;username&gt; \n" +
		"  GRANT EXECUTE ON FUNCTION pg_read_file(text)       TO &lt;username&gt; \n" +
		"</pre>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	// pg_current_logfile() was introduced in Postgres 10
	// JSON Logging was introduced in Postgres 15
//	public static final long     NEED_SRV_VERSION = Ver.ver(15);
	public static final long     NEED_SRV_VERSION = Ver.ver(10);
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"pg_srv_errorlog"};
	public static final String[] NEED_ROLES       = new String[] {}; // possibly: pg_read_server_files
	public static final String[] NEED_CONFIG      = new String[] {};

//	public static final String[] PCT_COLUMNS      = new String[] {};
//	public static final String[] DIFF_COLUMNS     = new String[] {"XXXdiffCols"};

//	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
//	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.OFF; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '"+CM_NAME+"'");

		return new CmErrorLog(counterController, guiController);
	}

	public CmErrorLog(ICounterController counterController, IGuiController guiController)
	{
		super(counterController, CM_NAME, GROUP_NAME, null, MON_TABLES, NEED_ROLES, NEED_CONFIG, NEED_SRV_VERSION, NEED_CE_VERSION, IS_SYSTEM_CM);

		setDisplayName(SHORT_NAME);
		setDescription(HTML_DESC);

		setIconFile(GUI_ICON_FILE);

		setCounterController(counterController);
		setGuiController(guiController);
		
		addTrendGraphs();
		
		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	private static final String  PROP_PREFIX                = CM_NAME;

	public static final String  PROPKEY_sample_JsonOverCsv  = PROP_PREFIX + ".sample.json.over.csv";
	public static final boolean DEFAULT_sample_JsonOverCsv  = false;

//TODO;	// look for 'execution plans', 'long running SQL Statements' and SEND/STORE them in PCS 

	private enum UseLogType
	{
		NOT_YET_DECIDED, CSV, JSON
	};
	private UseLogType _useLogType = UseLogType.NOT_YET_DECIDED;

	private void addTrendGraphs()
	{
	}

	@Override
	protected TabularCntrPanel createGui()
	{
		return new TabularCntrPanelAppend(this);
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		String name = "pg_srv_errorlog";
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addTable(name, "Reading the errorlog via SQL.");
			
			mtd.addColumn(name, "log_name"          ,       "<html>Time stamp with milliseconds</html>");
			mtd.addColumn(name, "user_name"         ,       "<html>User name</html>");
			mtd.addColumn(name, "database_name"     ,       "<html>Database name</html>");
			mtd.addColumn(name, "process_id"        ,       "<html>Process ID</html>");
			mtd.addColumn(name, "connection_from"   ,       "<html>Client host</html>");
//			mtd.addColumn(name, "remote_port"       ,       "<html>Client port</html>");
			mtd.addColumn(name, "session_id"        ,       "<html>Session ID</html>");
			mtd.addColumn(name, "session_line_num"  ,       "<html>Per-session line number</html>");
			mtd.addColumn(name, "command_tag"       ,       "<html>Current command tag or 'ps display'</html>");
			mtd.addColumn(name, "session_start_time",       "<html>Session start time</html>");
			mtd.addColumn(name, "virtual_transaction_id",   "<html>Virtual transaction ID</html>");
			mtd.addColumn(name, "transaction_id"    ,       "<html>Regular transaction ID</html>");
			mtd.addColumn(name, "error_severity"    ,       "<html>Error severity</html>");
			mtd.addColumn(name, "sql_state_code"    ,       "<html>SQLSTATE code</html>");
			mtd.addColumn(name, "sql_state_code_desc",       "<html>Description found at - https://www.postgresql.org/docs/current/errcodes-appendix.html </html>");
			mtd.addColumn(name, "message"           ,       "<html>Error message</html>");
			mtd.addColumn(name, "detail"            ,       "<html>Error message detail</html>");
			mtd.addColumn(name, "hint"              ,       "<html>Error message hint</html>");
			mtd.addColumn(name, "internal_query"    ,       "<html>Internal query that led to the error</html>");
			mtd.addColumn(name, "internal_query_pos",       "<html>Cursor index into internal query</html>");
			mtd.addColumn(name, "context"           ,       "<html>Error context</html>");
			mtd.addColumn(name, "query"             ,       "<html>Client-supplied query string</html>");
			mtd.addColumn(name, "query_pos"         ,       "<html>Cursor index into query string</html>");
			mtd.addColumn(name, "location"          ,       "<html>Error location function name</html>");
//			mtd.addColumn(name, "file_name"         ,       "<html>File name of error location</html>");
//			mtd.addColumn(name, "file_line_num"     ,       "<html>File line number of the error location</html>");
			mtd.addColumn(name, "application_name"  ,       "<html>Client application name</html>");
			mtd.addColumn(name, "backend_type"      ,       "<html>Type of backend</html>");
			mtd.addColumn(name, "leader_pid"        ,       "<html>Process ID of leader for active parallel workers</html>");
			mtd.addColumn(name, "query_id"          ,       "<html>Query ID</html>");

			mtd.addColumn(name, "pg_current_logfile",       "<html>Current log filename this entry was fetched from.</html>");
		}
		catch (NameNotFoundException e) 
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
		//	System.out.println("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
		}
	}

//	@Override
//	public boolean checkDependsOnVersion(DbxConnection conn)
//	{
//		DbmsVersionInfoSqlServer versionInfo = (DbmsVersionInfoSqlServer) conn.getDbmsVersionInfo();
//		if (versionInfo.isAzureDb() || versionInfo.isAzureSynapseAnalytics())
//		{
//			_logger.warn("When trying to initialize Counters Model '" + getName() + "', named '"+getDisplayName() + "', connected to Azure SQL Database or Analytics, which do NOT support reading the errorlog file via 'xp_readerrorlog'.");
//
//			setActive(false, "This info is NOT available in Azure SQL Database or Azure Synapse/Analytics.");
//
//			TabularCntrPanel tcp = getTabPanel();
//			if (tcp != null)
//			{
//				tcp.setToolTipText("This info is NOT available in Azure SQL Database or Azure Synapse/Analytics.");
//			}
//			return false;
//		}
//		
//		// FIXME: logging_collector must be enabled, and having JSON logging
//
//		return super.checkDependsOnVersion(conn);
//	}
	
	@Override
	public String[] getDependsOnConfigForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return NEED_CONFIG;
	}

	@Override
	public boolean createNewSqlForEveryRefresh()
	{
		// call: getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
		// before every refresh
		return true;
	}
	
	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		//(new Exception("DUMMY Exception from ::: getSqlForVersion")).printStackTrace();
		if (versionInfo.getLongVersion() >= NEED_SRV_VERSION)
		{
			// NOTE: We might be able to enhance this a bit by checking the log file(s)
			//       SELECT name, size/*long*/, modification/*timestamp*/ FROM pg_catalog.pg_ls_logdir() WHERE modification > clock_timestamp() - '24 hour'::interval;
			//       SELECT name, size/*long*/, modification/*timestamp*/ FROM pg_catalog.pg_ls_logdir() WHERE modification > clock_timestamp() - '2 minute'::interval;
			// To check if the file has "moved"
			// and also remember the "size" so we can start reading "at the end" of the file instead of the whole file!

			String sql = "select pg_current_logfile('csvlog')";
			if ( UseLogType.JSON.equals(_useLogType) )
				sql = "select pg_current_logfile('jsonlog')";

			try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
			{
				while(rs.next())
				{
					_pg_current_logfile = rs.getString(1); // pg_current_logfile('csvlog')
				}
			}
			catch (SQLException ex)
			{
				_logger.error("Problems getting current logfile name using SQL=|" + sql + "|.");
			}

			// Get current filename for table 'pg_srv_errorlog'
			if ( UseLogType.CSV.equals(_useLogType) )
			{
				sql = ""
					    + "select option_value --, * \n"
					    + "from information_schema.foreign_table_options \n"
					    + "where 1=1 \n"
					    + "  and foreign_table_catalog = 'postgres' \n"
					    + "  and foreign_table_schema  = 'public' \n"
					    + "  and foreign_table_name    = 'pg_srv_errorlog' \n"
					    + "  and option_name           = 'filename' \n"
					    + "";
					
				try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
				{
					while(rs.next())
					{
						_pg_current_fdw_logfile = rs.getString(1); // pg_current_logfile('jsonlog')
					}
				}
				catch (SQLException ex)
				{
					_logger.error("Problems getting current logfile for 'pg_srv_errorlog' name using SQL=|" + sql + "|.");
				}
				_logger.debug("currentFdwLogFile='" + _pg_current_fdw_logfile + "', newLogFile='" + _pg_current_logfile + "'.");

				// If the log file has changed...
				// ALTER the table
				if (StringUtil.hasValue(_pg_current_fdw_logfile) && ! _pg_current_fdw_logfile.equals(_pg_current_logfile))
				{
					sql = "ALTER FOREIGN TABLE pg_srv_errorlog OPTIONS (SET filename '" + _pg_current_logfile + "')";

					_logger.info("Altering table 'pg_srv_errorlog' to use new log filename. (currentFdwLogFile='" + _pg_current_fdw_logfile + "', newLogFile='" + _pg_current_logfile + "') using SQL=|" + sql + "|.");

					try (Statement stmnt = conn.createStatement())
					{
						stmnt.executeUpdate(sql);
					}
					catch(SQLException ex)
					{
						_logger.error("Problems altering table 'pg_srv_errorlog' to use new log filename. (currentFdwLogFile='" + _pg_current_fdw_logfile + "', newLogFile='" + _pg_current_logfile + "') using SQL=|" + sql + "|.", ex);
						return ""
								+ "DO $$ BEGIN \n"
								+ "    RAISE EXCEPTION 'Problems Execute: " + sql.replace("'", "''") + " \n"
								+ "    USING HINT = 'Check that current_user is authorised to alter the table.' \n"
								+ "END; $$ LANGUAGE plpgsql \n";
					}
				}
			}
		}
		else
		{
			_logger.error("This CM '" + CM_NAME + "' Needs version " + Ver.versionNumToStr(NEED_SRV_VERSION) + " or above, you calling getSqlForVersion() with version " + Ver.versionNumToStr(versionInfo.getLongVersion()));
		}

		// FIXME: Instead of using 'getPreviousSampleTime()', maybe we should do:
		//        - Save lastSampleDate in a file: /tmp/DbxTune/..../CmErrorlog.lastSample
		//        - If the file is not available (or we fail to parse) then fallback...
		// Then we can also change sendAlarm() not to SKIP first sample...
		// Possibly just "override" getPreviousSampleTime()
		
		// Sample only records we have not seen previously...
		String andTimestampGtLastSample = "";
		Timestamp prevSample = getPreviousSampleTime();
		if (prevSample != null)
		{
			andTimestampGtLastSample = "  AND log_time > '" + prevSample.toString() + "' \n";

			if ( UseLogType.JSON.equals(_useLogType) )
				andTimestampGtLastSample = "  AND entry->>'timestamp' > '" + prevSample.toString() + "' \n";
		}
		
		String sql = ""
				+ "-- FOUND 'CSV' in log_destination='" + _log_destination + "' which will be used.\n"
			    + "SELECT \n"
			    + "                                                      log_time \n"
			    + "    ,cast(user_name              as varchar(128) ) AS user_name \n"
			    + "    ,cast(database_name          as varchar(128) ) AS database_name \n"
			    + "    ,                                                 process_id \n"
			    + "    ,cast(connection_from        as varchar(128) ) AS connection_from \n"
			    + "    ,cast(session_id             as varchar(128) ) AS session_id \n"
			    + "    ,                                                 session_line_num \n"
			    + "    ,cast(command_tag            as varchar(128) ) AS command_tag \n"
			    + "    ,                                                 session_start_time \n"
			    + "    ,cast(virtual_transaction_id as varchar(32)  ) AS virtual_transaction_id \n"
			    + "    ,                                                 transaction_id \n"
			    + "    ,cast(error_severity         as varchar(15 ) ) AS error_severity \n"
			    + "    ,cast(sql_state_code         as varchar(15 ) ) AS sql_state_code \n"
			    + "    ,cast(''                     as varchar(60)  ) AS sql_state_code_desc \n"
			    + "    ,                                                 message \n"
			    + "    ,cast(detail                 as varchar(512) ) AS detail \n"
			    + "    ,cast(hint                   as varchar(512) ) AS hint \n"
			    + "    ,cast(internal_query         as varchar(4000)) AS internal_query \n"
			    + "    ,                                                 internal_query_pos \n"
			    + "    ,cast(context                as varchar(512) ) AS context \n"
			    + "    ,cast(query                  as varchar(4000)) AS query \n"
			    + "    ,                                                 query_pos \n"
			    + "    ,cast(location               as varchar(128) ) AS location \n"
			    + "    ,cast(application_name       as varchar(128) ) AS application_name \n"
			    + "    ,cast(backend_type           as varchar(128) ) AS backend_type \n"
			    + "    ,                                                 leader_pid \n"
			    + "    ,                                                 query_id \n"
			    + "    ,cast(''                     as varchar(128) ) AS pg_current_logfile \n"
				+ "FROM pg_srv_errorlog \n"
				+ "WHERE 1=1 \n"
				+ andTimestampGtLastSample
				+ "";

		if ( UseLogType.JSON.equals(_useLogType) )
		{
			sql = ""
				+ "-- FOUND 'JSON' in log_destination='" + _log_destination + "' which will be used.\n"
				+ "WITH errorlog AS \n"
				+ "( \n"
				+ "	SELECT v::json AS entry \n"
				+ "	FROM regexp_split_to_table( trim(pg_read_file('" + _pg_current_logfile + "'), E'\\n'), '[\\n\\r]+') AS v \n"
				+ ") \n"
				+ "SELECT \n"
				+ "     cast(entry->>'timestamp'         as timestamp(3) with time zone) AS log_time \n"
				+ "    ,cast(entry->>'user'              as varchar(128)               ) AS user_name \n"
				+ "    ,cast(entry->>'dbname'            as varchar(128)               ) AS database_name \n"
				+ "    ,cast(entry->>'pid'               as int                        ) AS process_id \n"
				+ "    ,cast(cast(entry->>'remote_host' as varchar(128)) || ':' || cast(entry->>'remote_port' as int) as varchar(128)) AS connection_from  \n"
				+ "    ,cast(entry->>'session_id'        as varchar(128)               ) AS session_id \n"
				+ "    ,cast(entry->>'line_num'          as bigint                     ) AS session_line_num \n"
				+ "    ,cast(entry->>'ps'                as varchar(128)               ) AS command_tag \n"
				+ "    ,cast(entry->>'session_start'     as varchar(128)               ) AS session_start_time \n"
				+ "    ,cast(entry->>'vxid'              as varchar(32)                ) AS virtual_transaction_id \n"
				+ "    ,cast(entry->>'txid'              as varchar(32)                ) AS transaction_id \n"
				+ "    ,cast(entry->>'error_severity'    as varchar(15)                ) AS error_severity \n"
				+ "    ,cast(entry->>'state_code'        as varchar(15)                ) AS sql_state_code \n"
				+ "    ,cast(''                          as varchar(60)                ) AS sql_state_code_desc \n"
//				+ "    ,cast(entry->>'message'           as varchar(4000)              ) AS message \n" // ADD in localCalculation()
				+ "    ,cast(entry->>'message'           as text                       ) AS message \n" // ADD in localCalculation()
				+ "    ,cast(entry->>'detail'            as varchar(512)               ) AS detail \n"
				+ "    ,cast(entry->>'hint'              as varchar(512)               ) AS hint \n"
				+ "    ,cast(entry->>'internal_query'    as varchar(4000)              ) AS internal_query \n"
				+ "    ,cast(entry->>'internal_position' as int                        ) AS internal_query_pos \n"
				+ "    ,cast(entry->>'context'           as varchar(512)               ) AS context \n"
				+ "    ,cast(entry->>'statement'         as varchar(4000)              ) AS query \n"
				+ "    ,cast(entry->>'cursor_position'   as bigint                     ) AS query_pos \n"
				+ "    ,cast(cast(entry->>'func_name' as varchar(128)) || ' ' || cast(entry->>'file_name' as varchar(128)) || ':' || cast(entry->>'file_line_num' as int) as varchar(128)) AS location  \n"
				+ "    ,cast(entry->>'application_name'  as varchar(128)               ) AS application_name \n"
				+ "    ,cast(entry->>'backend_type'      as varchar(128)               ) AS backend_type \n"
				+ "    ,cast(entry->>'leader_pid'        as int                        ) AS leader_pid \n"
				+ "    ,cast(entry->>'query_id'          as bigint                     ) AS query_id \n"
				+ "    ,cast(''                          as varchar(128)               ) AS pg_current_logfile \n" // ADD in localCalculation()
				+ "FROM errorlog \n"
				+ "WHERE 1 = 1 \n"
				+ andTimestampGtLastSample
				+ "";		
		}

//		String sql = ""
//			+ "WITH errorlog AS \n"
//			+ "( \n"
//			+ "	SELECT v::json AS entry \n"
//			+ "	FROM regexp_split_to_table( trim(pg_read_file('" + _pg_current_json_logfile + "'), E'\\n'), '[\\n\\r]+') AS v \n"
//			+ ") \n"
//			+ "SELECT \n"
//			+ "     cast(entry->>'timestamp'         as timestamp(3) with time zone) AS timestamp \n"
//			+ "    ,cast(entry->>'user'              as varchar(128)               ) AS user \n"
//			+ "    ,cast(entry->>'dbname'            as varchar(128)               ) AS dbname \n"
//			+ "    ,cast(entry->>'pid'               as int                        ) AS pid \n"
//			+ "    ,cast(entry->>'remote_host'       as varchar(128)               ) AS remote_host \n"
//			+ "    ,cast(entry->>'remote_port'       as int                        ) AS remote_port \n"
//			+ "    ,cast(entry->>'session_id'        as varchar(128)               ) AS session_id \n"
//			+ "    ,cast(entry->>'line_num'          as bigint                     ) AS line_num \n"
//			+ "    ,cast(entry->>'ps'                as varchar(128)               ) AS ps \n"
//			+ "    ,cast(entry->>'session_start'     as varchar(128)               ) AS session_start \n"
//			+ "    ,cast(entry->>'vxid'              as varchar(32)                ) AS vxid \n"
//			+ "    ,cast(entry->>'txid'              as varchar(32)                ) AS txid \n"
//			+ "    ,cast(entry->>'error_severity'    as varchar(15)                ) AS error_severity \n"
//			+ "    ,cast(entry->>'state_code'        as varchar(15)                ) AS state_code \n"
//			+ "    ,cast(''                          as varchar(60)                ) AS state_code_desc \n"
////			+ "    ,cast(entry->>'message'           as varchar(4000)              ) AS message \n" // ADD in localCalculation()
//			+ "    ,cast(entry->>'message'           as text                       ) AS message \n" // ADD in localCalculation()
//			+ "    ,cast(entry->>'detail'            as varchar(512)               ) AS detail \n"
//			+ "    ,cast(entry->>'hint'              as varchar(512)               ) AS hint \n"
//			+ "    ,cast(entry->>'internal_query'    as varchar(4000)              ) AS internal_query \n"
//			+ "    ,cast(entry->>'internal_position' as int                        ) AS internal_position \n"
//			+ "    ,cast(entry->>'context'           as varchar(128)               ) AS context \n"
//			+ "    ,cast(entry->>'statement'         as varchar(4000)              ) AS statement \n"
//			+ "    ,cast(entry->>'cursor_position'   as bigint                     ) AS cursor_position \n"
//			+ "    ,cast(entry->>'func_name'         as varchar(128)               ) AS func_name \n"
//			+ "    ,cast(entry->>'file_name'         as varchar(128)               ) AS file_name \n"
//			+ "    ,cast(entry->>'file_line_num'     as int                        ) AS file_line_num \n"
//			+ "    ,cast(entry->>'application_name'  as varchar(128)               ) AS application_name \n"
//			+ "    ,cast(entry->>'backend_type'      as varchar(128)               ) AS backend_type \n"
//			+ "    ,cast(entry->>'leader_pid'        as int                        ) AS leader_pid \n"
//			+ "    ,cast(entry->>'query_id'          as bigint                     ) AS query_id \n"
//			+ "    ,cast(''                          as varchar(128)               ) AS pg_current_logfile \n" // ADD in localCalculation()
//			+ "FROM errorlog \n"
//			+ "WHERE 1 = 1 \n"
//			+ andTimestampGtLastSample
//			+ "";		
		
		return sql; 
	}

	@Override
	public void localCalculation(CounterSample newSample)
	{
		PostgresErrorCodeDictionary dict = PostgresErrorCodeDictionary.getInstance();

		int pos__sql_state_code      = newSample.findColumn("sql_state_code");
		int pos__sql_state_code_desc = newSample.findColumn("sql_state_code_desc");
		int pos__pg_current_logfile  = newSample.findColumn("pg_current_logfile");
		int pos__message             = newSample.findColumn("message");

		for (int r = 0; r < newSample.getRowCount(); r++)
		{
			if (pos__sql_state_code != -1 && pos__sql_state_code_desc != -1)
			{
				String sql_state_code      = newSample.getValueAsString(r, pos__sql_state_code);
				String sql_state_code_desc = dict.getDescriptionPlain(sql_state_code);

				newSample.setValueAt(sql_state_code_desc, r, pos__sql_state_code_desc);
			}
			
			if (pos__pg_current_logfile != -1)
			{
				newSample.setValueAt(_pg_current_logfile, r, pos__pg_current_logfile);
			}
			
			// parse the 'message' column
			//  - if 'auto_explain' is enabled and we see messages above that value, lets store the 
			//    'explain' output in the PCS so we can use it later in for example: Daily Summary Reports
			if (pos__message != -1)
			{
				String message = newSample.getValueAsString(r, pos__message);
				if (message != null)
				{
					if (message.startsWith("duration: "))
					{
						handleLogMessage_duration(message);
					}
				}
			}
		}
	}

	/**
	 * Break-out the logic so we don't have to do EVERYTHING in localCalculation()
	 * @param message
	 */
	private void handleLogMessage_duration(String message)
	{
		// If NO PCS, get out of here
//		if ( ! PersistentCounterHandler.hasInstance() )
//			return;

		// duration: 11011.124 ms  execute : select pg_sleep(11)
		// duration: 11011.113 ms  plan:\n{..."Query Text": "select pg_sleep(11)\n",\n "Plan": {...}

//		boolean isFrom_logMinDurationStatement = message.indexOf(" execute : ") != -1;
		boolean isFrom_autoExaplain            = message.indexOf(" plan:\n")    != -1;

		if (isFrom_autoExaplain)
		{
			// Parse and get QueryId, then store the plan in PCS DDL Storage as StatementCache entry
			String queryId = getQueryIdentifier(message);

			// Send it to the PCS DDL Storage
			if (PersistentCounterHandler.hasInstance())
			{
				String pcsMessage = CM_NAME + "|" + message;
				PersistentCounterHandler.getInstance().addDdl(PersistentCounterHandler.STATEMENT_CACHE_NAME, queryId, pcsMessage);
			}
			
			// Add it to the "Plan Cache", so we can read it from CmActiveStatements (and CmPgStatements)
			if (XmlPlanCache.hasInstance())
			{
				XmlPlanCache.getInstance().setPlan(queryId, message);
//XmlPlanCache.getInstance().printStatistics();
			}
//System.out.println("handleLogMessage_duration(): XmlPlanCache.hasInstance()=" + XmlPlanCache.hasInstance() + ", queryId='" + queryId + "', plan=" + (XmlPlanCache.hasInstance() ? XmlPlanCache.getInstance().getPlan(queryId) : "-no-XmlPlanInstance-" ));
//			TODO; // In CmActiveStatements get from PcsReader (hopefully backed by some "some cache" at least the QueryId)
//			TODO; // Possibly in CmPgStatements (GUI) tool tip on QueryId, which gets the plan from above)
		}

	}

	/**
	 * Parse the messsage to get <code>Query Identifier</code>
	 * @param message
	 * @return queryId
	 */
	private static String getQueryIdentifier(String message)
	{
		if (message == null)
			return null;
		
		String queryId = null;

		int pos = message.indexOf("Query Identifier");
		// XML tag has a '-' in it
		if (pos == -1)
			pos = message.indexOf("Query-Identifier");

		if (pos != -1)
		{
			// TEXT: Query Identifier: -3416356442043621232
			// JSON:  "Query Identifier": -3416356442043621232,
			// XML:   <Query-Identifier>-3416356442043621232</Query-Identifier>
			// YAML: Query Identifier: -3416356442043621232
			
			// Position at the START of the ID
			String tmp = message.substring(pos + "Query Identifier".length());
			while (tmp.startsWith(":") || tmp.startsWith("\"") || tmp.startsWith(">") || tmp.startsWith(" "))
				tmp = tmp.substring(1);

			// Copy chars until END-OF the ID (note: I didn't want to loop while [-0123456789] if we have hex chars in the future...
			queryId = "";
			for (int c=0; c<tmp.length(); c++)
			{
				char ch = tmp.charAt(c);

				if (ch == ' ' || ch == ',' || ch == '<' || ch == '\n' || ch == '\r')
					break;

				queryId += ch;
			}
		}
		
		return queryId;
	}

	private static void test_getQueryIdentifier()
	{
		String text = "" +
				"duration: 4001.386 ms  plan:\n" +
				"Query Text: select pg_sleep(4)\n" +
				"\n" +
				"Result  (cost=0.00..0.03 rows=1 width=4) (actual time=4001.370..4001.372 rows=1 loops=1)\n" +
				"  Output: pg_sleep('4'::double precision)\n" +
				"Settings: work_mem = '16MB', effective_io_concurrency = '200', random_page_cost = '1.1', cpu_tuple_cost = '0.03'\n" +
				"Query Identifier: -3416356442043621232\n";

		String json = "" +
				"\n" +
				"duration: 4004.091 ms  plan:\n" +
				"{\n" +
				"  \"Query Text\": \"select pg_sleep(4)\n\",\n" +
				"  \"Plan\": {\n" +
				"    \"Node Type\": \"Result\",\n" +
				"    \"Parallel Aware\": false,\n" +
				"    \"Async Capable\": false,\n" +
				"    \"Startup Cost\": 0.00,\n" +
				"    \"Total Cost\": 0.03,\n" +
				"    \"Plan Rows\": 1,\n" +
				"    \"Plan Width\": 4,\n" +
				"    \"Actual Startup Time\": 4004.076,\n" +
				"    \"Actual Total Time\": 4004.077,\n" +
				"    \"Actual Rows\": 1,\n" +
				"    \"Actual Loops\": 1,\n" +
				"    \"Output\": [\"pg_sleep('4'::double precision)\"],\n" +
				"    \"Shared Hit Blocks\": 0,\n" +
				"    \"Shared Read Blocks\": 0,\n" +
				"    \"Shared Dirtied Blocks\": 0,\n" +
				"    \"Shared Written Blocks\": 0,\n" +
				"    \"Local Hit Blocks\": 0,\n" +
				"    \"Local Read Blocks\": 0,\n" +
				"    \"Local Dirtied Blocks\": 0,\n" +
				"    \"Local Written Blocks\": 0,\n" +
				"    \"Temp Read Blocks\": 0,\n" +
				"    \"Temp Written Blocks\": 0,\n" +
				"    \"I/O Read Time\": 0.000,\n" +
				"    \"I/O Write Time\": 0.000,\n" +
				"    \"Temp I/O Read Time\": 0.000,\n" +
				"    \"Temp I/O Write Time\": 0.000,\n" +
				"    \"WAL Records\": 0,\n" +
				"    \"WAL FPI\": 0,\n" +
				"    \"WAL Bytes\": 0\n" +
				"  },\n" +
				"  \"Settings\": {\n" +
				"    \"work_mem\": \"16MB\",\n" +
				"    \"effective_io_concurrency\": \"200\",\n" +
				"    \"random_page_cost\": \"1.1\",\n" +
				"    \"cpu_tuple_cost\": \"0.03\"\n" +
				"  },\n" +
				"  \"Query Identifier\": -3416356442043621232,\n" +
				"  \"Triggers\": [\n" +
				"  ]\n" +
				"}\n";
		
		String xml  = "" +
			    "\n" +
				"duration: 4004.094 ms  plan:\n" +
				"<explain xmlns=\"http://www.postgresql.org/2009/explain\">\n" +
				"  <Query-Text>select pg_sleep(4)\n" +
				"</Query-Text>\n" +
				"  <Plan>\n" +
				"    <Node-Type>Result</Node-Type>\n" +
				"    <Parallel-Aware>false</Parallel-Aware>\n" +
				"    <Async-Capable>false</Async-Capable>\n" +
				"    <Startup-Cost>0.00</Startup-Cost>\n" +
				"    <Total-Cost>0.03</Total-Cost>\n" +
				"    <Plan-Rows>1</Plan-Rows>\n" +
				"    <Plan-Width>4</Plan-Width>\n" +
				"    <Actual-Startup-Time>4004.079</Actual-Startup-Time>\n" +
				"    <Actual-Total-Time>4004.081</Actual-Total-Time>\n" +
				"    <Actual-Rows>1</Actual-Rows>\n" +
				"    <Actual-Loops>1</Actual-Loops>\n" +
				"    <Output>\n" +
				"      <Item>pg_sleep('4'::double precision)</Item>\n" +
				"    </Output>\n" +
				"    <Shared-Hit-Blocks>0</Shared-Hit-Blocks>\n" +
				"    <Shared-Read-Blocks>0</Shared-Read-Blocks>\n" +
				"    <Shared-Dirtied-Blocks>0</Shared-Dirtied-Blocks>\n" +
				"    <Shared-Written-Blocks>0</Shared-Written-Blocks>\n" +
				"    <Local-Hit-Blocks>0</Local-Hit-Blocks>\n" +
				"    <Local-Read-Blocks>0</Local-Read-Blocks>\n" +
				"    <Local-Dirtied-Blocks>0</Local-Dirtied-Blocks>\n" +
				"    <Local-Written-Blocks>0</Local-Written-Blocks>\n" +
				"    <Temp-Read-Blocks>0</Temp-Read-Blocks>\n" +
				"    <Temp-Written-Blocks>0</Temp-Written-Blocks>\n" +
				"    <I-O-Read-Time>0.000</I-O-Read-Time>\n" +
				"    <I-O-Write-Time>0.000</I-O-Write-Time>\n" +
				"    <Temp-I-O-Read-Time>0.000</Temp-I-O-Read-Time>\n" +
				"    <Temp-I-O-Write-Time>0.000</Temp-I-O-Write-Time>\n" +
				"    <WAL-Records>0</WAL-Records>\n" +
				"    <WAL-FPI>0</WAL-FPI>\n" +
				"    <WAL-Bytes>0</WAL-Bytes>\n" +
				"  </Plan>\n" +
				"  <Settings>\n" +
				"    <work_mem>16MB</work_mem>\n" +
				"    <effective_io_concurrency>200</effective_io_concurrency>\n" +
				"    <random_page_cost>1.1</random_page_cost>\n" +
				"    <cpu_tuple_cost>0.03</cpu_tuple_cost>\n" +
				"  </Settings>\n" +
				"  <Query-Identifier>-3416356442043621232</Query-Identifier>\n" +
				"  <Triggers>\n" +
				"  </Triggers>\n" +
				"</explain>\n";
				
		String yaml = "" +
			    "\n" + 
				"duration: 4001.410 ms  plan:\n" +
				"Query Text: \"select pg_sleep(4)\n\"\n" +
				"Plan: \n" +
				"  Node Type: \"Result\"\n" +
				"  Parallel Aware: false\n" +
				"  Async Capable: false\n" +
				"  Startup Cost: 0.00\n" +
				"  Total Cost: 0.03\n" +
				"  Plan Rows: 1\n" +
				"  Plan Width: 4\n" +
				"  Actual Startup Time: 4001.396\n" +
				"  Actual Total Time: 4001.397\n" +
				"  Actual Rows: 1\n" +
				"  Actual Loops: 1\n" +
				"  Output: \n" +
				"    - \"pg_sleep('4'::double precision)\"\n" +
				"  Shared Hit Blocks: 0\n" +
				"  Shared Read Blocks: 0\n" +
				"  Shared Dirtied Blocks: 0\n" +
				"  Shared Written Blocks: 0\n" +
				"  Local Hit Blocks: 0\n" +
				"  Local Read Blocks: 0\n" +
				"  Local Dirtied Blocks: 0\n" +
				"  Local Written Blocks: 0\n" +
				"  Temp Read Blocks: 0\n" +
				"  Temp Written Blocks: 0\n" +
				"  I/O Read Time: 0.000\n" +
				"  I/O Write Time: 0.000\n" +
				"  Temp I/O Read Time: 0.000\n" +
				"  Temp I/O Write Time: 0.000\n" +
				"  WAL Records: 0\n" +
				"  WAL FPI: 0\n" +
				"  WAL Bytes: 0\n" +
				"Settings: \n" +
				"  work_mem: \"16MB\"\n" +
				"  effective_io_concurrency: \"200\"\n" +
				"  random_page_cost: \"1.1\"\n" +
				"  cpu_tuple_cost: \"0.03\"\n" +
				"Query Identifier: -3416356442043621232\n" +
				"Triggers:\n";

		System.out.println("TEXT: " + ("-3416356442043621232".equals(getQueryIdentifier(text)) ? "OK" : "FAIL"));
		System.out.println("JSON: " + ("-3416356442043621232".equals(getQueryIdentifier(json)) ? "OK" : "FAIL"));
		System.out.println("XML : " + ("-3416356442043621232".equals(getQueryIdentifier(xml )) ? "OK" : "FAIL"));
		System.out.println("YAML: " + ("-3416356442043621232".equals(getQueryIdentifier(yaml)) ? "OK" : "FAIL"));
	}
//	public static void main(String[] args)
//	{
//		test_getQueryIdentifier();
//	}
	
	//---------------------------------------------
	// pg_read_file ( filename text [, offset bigint, length bigint ] [, missing_ok boolean ] ) --> text
	//
	// Returns all or part of a text file, starting at the given byte offset, returning at most length bytes 
	// (less if the end of file is reached first). If offset is negative, it is relative to the end of the file. 
	// If offset and length are omitted, the entire file is returned. 
	// The bytes read from the file are interpreted as a string in the database's encoding; an error 
	// is thrown if they are not valid in that encoding.
	// 
	// This function is restricted to superusers by default, but other users can be granted EXECUTE to run the function.
	//---------------------------------------------
	// Alternative to the above is using "tail -f" on the OS file and switch file, whenever that happens...
	//---------------------------------------------


	private String _pg_current_logfile      = null;
//	private String _pg_current_csv_logfile  = null;
//	private String _pg_current_json_logfile = null;
	private String _pg_data_directory       = null;
	private String _pg_current_fdw_logfile  = null;
	private String _log_destination         = null;
	
//	private boolean hasCsvLog()  { if (_log_destination == null) return false; return _log_destination.indexOf("csvlog")  != -1; }
//	private boolean hasJsonLog() { if (_log_destination == null) return false; return _log_destination.indexOf("jsonlog") != -1; }

	private void createPgSrvErrorlog(DbxConnection conn)
	throws SQLException
	{
		String sql;
		// Check if we are ALLOWED to create the table
		
		// Create the extension
		sql = pg_srv_errorlog("extension", "", "", "");
		try (Statement stmnt = conn.createStatement())
		{
			_logger.info("Trying to create extension 'file_fdw'.");
			stmnt.executeUpdate(sql);
		}

		// Create the foreign server
		sql = pg_srv_errorlog("server", "", "", "");
		try (Statement stmnt = conn.createStatement())
		{
			_logger.info("Trying to create: CREATE SERVER pg_srv_errorlog FOREIGN DATA WRAPPER file_fdw.");
			stmnt.executeUpdate(sql);
		}

		// Create the foreign table
		sql = pg_srv_errorlog("table", _pg_current_logfile, "", "");
		try (Statement stmnt = conn.createStatement())
		{
			_logger.info("Trying to create foreign table 'pg_srv_errorlog'.");
			stmnt.executeUpdate(sql);
		}

//		// do GRANT
//		sql = pg_srv_errorlog("grant", "", "", "");
//		try (Statement stmnt = conn.createStatement())
//		{
//			stmnt.executeUpdate(sql);
//		}
//
//		// Alter 
//		sql = pg_srv_errorlog("alter", "", _pg_current_fdw_logfile, "");
//		try (Statement stmnt = conn.createStatement())
//		{
//			stmnt.executeUpdate(sql);
//		}
	}

	/**
	 * Override the normal depends on role, and check that we can execute: xp_readerrorlog
	 * <br>
	 * If issues: in-activate this CM and set message: grant exec on xp_readerrorlog to <current-login>
	 */
	@Override
	public boolean checkDependsOnRole(DbxConnection conn)
	{
		String sql;

		sql = "select current_setting('log_destination')";
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while(rs.next())
				_log_destination = rs.getString(1);
		}
		catch(SQLException ex)
		{
			_logger.error("Problems executing SQL=|" + sql + "|", ex);

			setActive(false, "Sorry can't even get 'log_destination'."); 
			return false;
		}

		// What do we have in 'log_destination'
		// Then decide what logger we should use CSV or JSON
		boolean hasCsvLog  = false;
		boolean hasJsonLog = false;
		if (_log_destination != null)
		{
			hasCsvLog  = _log_destination.indexOf("csvlog")  != -1;
			hasJsonLog = _log_destination.indexOf("jsonlog") != -1;
		}

		boolean preferJsonOverCsv = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_sample_JsonOverCsv, DEFAULT_sample_JsonOverCsv);
		_useLogType = UseLogType.NOT_YET_DECIDED;
		
		if (hasCsvLog)
			_useLogType = UseLogType.CSV;
		
		if ( (hasJsonLog && preferJsonOverCsv) || (!hasCsvLog && hasJsonLog))
			_useLogType = UseLogType.JSON;

//System.out.println("checkDependsOnRole(): _log_destination='" + _log_destination + "', hasCsvLog=" + hasCsvLog + ", hasJsonLog=" + hasJsonLog + ", preferJsonOverCsv=" + preferJsonOverCsv + ", _useLogType=" + _useLogType + ".");

		//-----------------------------------------------------------
		// NO GOOD Log Type was found
		if ( UseLogType.NOT_YET_DECIDED.equals(_useLogType) )
		{
			_logger.error("To read the errorlog, this CM depends on CSV or JSON logging. Fix: ALTER SYSTEM SET log_destination = 'csvlog' --or 'jsonlog'. See 'https://www.postgresql.org/docs/current/runtime-config-logging.html'");

			setActive(false, "Postgres is not configgured for CSV or JSON logging. \n" +
					"Fix: ALTER SYSTEM SET log_destination = 'csvlog'  -- or 'stderr,csvlog'  \n" +
					"or:  ALTER SYSTEM SET log_destination = 'jsonlog' -- or 'stderr,jsonlog' \n" +
					"\n" +
					"See 'https://www.postgresql.org/docs/current/runtime-config-logging.html' \n" +
					"See 'https://betterstack.com/community/guides/logging/how-to-start-logging-with-postgresql/' \n" +
					"See 'https://www.cybertec-postgresql.com/en/json-logs-in-postgresql-15/' for more info. \n" +
					""); 
			
			return false; // <<<<<------ GET OUT OF HERE
		}
		
		//-----------------------------------------------------------
		// CHECK CSV requirements
		if ( UseLogType.CSV.equals(_useLogType) )
		{
			if (hasCsvLog && hasJsonLog && preferJsonOverCsv)
			{
				_logger.info("Both CSV and JSON logger was found. The decision to use CSV over JSON was made, you can override this by setting property: " + PROPKEY_sample_JsonOverCsv + "=true");
			}
			
			// Get CSV log name
			sql = "select pg_current_logfile('csvlog'), current_setting('data_directory')";
			try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
			{
				while(rs.next())
				{
					_pg_current_logfile = rs.getString(1); // pg_current_logfile('csvlog')
					_pg_data_directory  = rs.getString(2); // current_setting('data_directory')
				}
			}
			catch(SQLException ex)
			{
				_logger.error("Problems executing SQL=|" + sql + "|", ex);

				setActive(false, "Sorry can't get current log file: pg_current_logfile('csvlog')."); 
				return false; // <<<<<------ GET OUT OF HERE
			}

			// Check if the 'pg_srv_errorlog' table exists
			sql = "select * from pg_srv_errorlog where 1=2";
			try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
			{
				while(rs.next())
				{
				}
			}
			catch(SQLException ex)
			{
				_logger.error("Problems checking for table 'pg_srv_errorlog' executing SQL=|" + sql + "|");
				
				try 
				{
					_logger.info("Lets try to create the table...");
					createPgSrvErrorlog(conn);
				}
				catch (SQLException ex2)
				{
					_logger.error("Problems creating table 'pg_srv_errorlog'.", ex2);
					_logger.info("Here is an example of how you can create the table 'pg_srv_errorlog'. " + pg_srv_errorlog("to_errorlog", "", "", ""));

					setActive(false, "Problems accessing (or creating) table 'pg_srv_errorlog' to read the Postgres error log. Follow the tooltip help on how to create the table."); 
					return false; // <<<<<------ GET OUT OF HERE
				}
			}

			// If not 'superuser' then check
			//  - That we own the 'pg_srv_errorlog' table (so we can alter the filename option)
			//  - That we have role 'pg_read_server_files'
			
		} // end: check CSV

		//-----------------------------------------------------------
		// CHECK JSON requirements
		if ( UseLogType.JSON.equals(_useLogType) )
		{
			sql = "select pg_read_file('postmaster.pid'), pg_current_logfile('jsonlog'), current_setting('data_directory')";
			try (Statement stmnt = conn.createStatement())
			{
				boolean hasRs = stmnt.execute(sql);
				if (hasRs)
				{
					ResultSet rs = stmnt.getResultSet();
					while(rs.next())
					{
						//String pidFile    = rs.getString(1); // pid file
						_pg_current_logfile = rs.getString(2); // pg_current_logfile('jsonlog')
						_pg_data_directory  = rs.getString(3); // current_setting('data_directory')
					}
					rs.close();
				}
			}
			catch (SQLException ex)
			{
				// PostgreSQL: ErrorCode 0, SQLState 42501, ExceptionClass: org.postgresql.util.PSQLException
				// ERROR: permission denied for function pg_read_file
				if ( "42501".equals(ex.getSQLState()) )
				{
					String username = "<currentUserName>";
					if (conn.getConnPropOrDefault() != null)
						username = conn.getConnPropOrDefault().getUsername();

					setActive(false, 
							"You currently do not have execution permissions on 'pg_read_file' or 'pg_current_logfile' \n" +
							"Fix: GRANT EXECUTE ON FUNCTION pg_read_file(text)       TO " + username + " \n" +
							"Fix: GRANT EXECUTE ON FUNCTION pg_current_logfile(text) TO " + username + " \n" +
							""); 
					
					_logger.info("FIX: GRANT EXECUTE ON FUNCTION pg_read_file(text)       TO " + username);
					_logger.info("FIX: GRANT EXECUTE ON FUNCTION pg_current_logfile(text) TO " + username);

					TabularCntrPanel tcp = getTabPanel();
					if (tcp != null)
					{
						tcp.setToolTipText(
								"<html>You currently do not have execution permissions on 'pg_read_file' or 'pg_current_logfile' <br>" +
								"Fix: <code>GRANT EXECUTE ON FUNCTION pg_read_file(text)       TO " + username + "</code><br>" +
								"Fix: <code>GRANT EXECUTE ON FUNCTION pg_current_logfile(text) TO " + username + "</code><br>" +
								"</html>");
					}
				}
				else
				{
					_logger.error("checkDependsOnRole() - executing '" + sql + "'", ex);

					setActive(false, ex.getMessage()); 

					TabularCntrPanel tcp = getTabPanel();
					if (tcp != null)
					{
						tcp.setToolTipText(ex.getMessage());
					}
				}
				return false; // <<<<<------ GET OUT OF HERE
			}
		} // end: check JSON
		

		// This is PROBABLY overkill...
		// It's ONLY if we PASS the above checks AND the _pg_current_logfile is NOT SET... (which hopefully will never happen)
		if (StringUtil.isNullOrBlank(_pg_current_logfile))
		{
			_logger.error("To read the errorlog, this CM depends on CSV or JSON logging. See 'https://www.postgresql.org/docs/current/runtime-config-logging.html/' for more info.");

			setActive(false, "This depends on CSV or JSON Logging.\n" +
					"See 'https://www.postgresql.org/docs/current/runtime-config-logging.html' \n" +
					"See 'https://betterstack.com/community/guides/logging/how-to-start-logging-with-postgresql/' \n" +
					"See 'https://www.cybertec-postgresql.com/en/json-logs-in-postgresql-15/' for more info. \n" +
					""); 

			TabularCntrPanel tcp = getTabPanel();
			if (tcp != null)
			{
				tcp.setToolTipText(
						"<html>This depends on CSV or JSON Logging.<br>" +
						"See 'https://www.postgresql.org/docs/current/runtime-config-logging.html' <br>" +
						"See 'https://betterstack.com/community/guides/logging/how-to-start-logging-with-postgresql/' <br>" +
						"See 'https://www.cybertec-postgresql.com/en/json-logs-in-postgresql-15/' for more info.<br>" +
						"</html>");
			}
			return false; // <<<<<------ GET OUT OF HERE
		}

		return true;
	}

	@Override
	public String getToolTipTextOnTableCell(MouseEvent e, String colName, Object cellValue, int modelRow, int modelCol) 
	{
		if ("sql_state_code".equals(colName) )
		{
			if (cellValue != null)
			{
				return PostgresErrorCodeDictionary.getInstance().getDescriptionHtml(cellValue + "");
			}
		}
		
		if (cellValue != null && StringUtil.equalsAny(colName, "message", "hint", "context", "query"))
		{
			return "<html><pre>" + StringEscapeUtils.escapeHtml4(cellValue+"") + "</pre></html>";
		}
		
		return super.getToolTipTextOnTableCell(e, colName, cellValue, modelRow, modelCol);
	}
	
	@Override
	public void sendAlarmRequest()
	{
		if ( ! AlarmHandler.hasInstance() )
			return;

		// ABS Data from LAST refresh only
		List<List<Object>> lastRefreshRows = getDataCollectionForLastRefresh();

		// DO NOTHING on FIRST refresh
		if ( isFirstTimeSample() )
		{
			String rowsInThisRefresh = " (Rows in first refresh was " + (lastRefreshRows == null ? "-null-" : lastRefreshRows.size()) + ")";

			_logger.info("First time we check the errorlog, Alarms will NOT be checked, because it will include old error messages." + rowsInThisRefresh);
			return;
		}
		
		AlarmHandler alarmHandler = AlarmHandler.getInstance();
		
		// Exit early if NO ROWS
		if (lastRefreshRows == null)
			return;
		if (lastRefreshRows.size() == 0)
			return;

		// Get column positions
		int col_log_time__pos       = findColumn("log_time");
		int col_command_tag__pos    = findColumn("command_tag");
		int col_sql_state_code__pos = findColumn("sql_state_code");
		int col_message__pos        = findColumn("message");

		if (col_log_time__pos == -1 || col_command_tag__pos == -1 || col_sql_state_code__pos == -1 || col_message__pos == -1)
		{
			_logger.error("When checking for alarms, could not find all columns. skipping this. [log_time=" + col_log_time__pos + ", command_tag='" + col_command_tag__pos + "', sql_state_code=" + col_sql_state_code__pos + ", message=" + col_message__pos + "]");
			return;
		}
		
		//boolean debugPrint = System.getProperty("sendAlarmRequest.debug", "false").equalsIgnoreCase("true");

		// Loop all rows
		for (int r=0; r<lastRefreshRows.size(); r++)
		{
			List<Object> row = lastRefreshRows.get(r);

			// Get values...
			Timestamp log_time       = (Timestamp) row.get(col_log_time__pos);
			String    command_tag    = (String)    row.get(col_command_tag__pos);          // ps stands for ??? Postgres System ???
			String    sql_state_code = (String)    row.get(col_sql_state_code__pos);
			String    message        = (String)    row.get(col_message__pos);

//System.out.println("PG:CmErrorlog.sendAlarm.row["+r+"]: log_time=|" + log_time + "|, command_tag=|" + command_tag + "|, sql_state_code=|" + sql_state_code + "|, message=|" + message + "|.");

			// null "protection"
			if (log_time       == null) log_time       = null;
			if (command_tag    == null) command_tag    = "";
			if (sql_state_code == null) sql_state_code = "";
			if (message        == null) message        = "";

			//-------------------------------------------------------
			// InternalErrors
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("InternalErrors"))
			{
				// PG: XX000 == internal_error
				// PG: XX001 == data_corrupted
				// PG: XX002 == index_corrupted
				if (StringUtil.containsAny(sql_state_code, "XX000", "XX001", "XX002"))
				{
					String stateCodeDesc = PostgresErrorCodeDictionary.getInstance().getDescriptionPlain(sql_state_code);

					// Example of messages (when I corrupted a file):
					//  - page verification failed, calculated checksum 41079 but expected 44920
					//  - invalid page in block 0 of relation base/59493/3337937
					
					// Possibly parse out the DBID/OBJID and get any names instead of the ID's
					//String[] sa = message.split("\\/[0-9]+\\/[0-9]+"); // NOT TESTED
					//String dbname     = DbmsObjectIdCache.getInstance().getDBName(dbid);
					//String objectName = DbmsObjectIdCache.getInstance().getObjectName(dbid, objectid);

					String extendedDescText = "";
					String extendedDescHtml = StringUtil.toHtmlTable(row, this.getColumnNames());

					AlarmEvent ae = new AlarmEventPgInternalError(this, sql_state_code, stateCodeDesc, message, log_time);
					ae.setExtendedDescription(extendedDescText, extendedDescHtml);

					alarmHandler.addAlarm( ae );
				}
			} // end: InternalErrors

			//-------------------------------------------------------
			// SystemError (errors external to PostgreSQL itself)
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("SystemErrors"))
			{
				String stateCodeDesc = PostgresErrorCodeDictionary.getInstance().getDescriptionPlain(sql_state_code);

				// PG: 58000 == system_error
				// PG: 58030 == io_error
				// PG: 58P01 == undefined_file
				// PG: 58P02 == duplicate_file
				if (StringUtil.containsAny(sql_state_code, "58000", "58030", "58P01", "58P02"))
				{
					String extendedDescText = "";
					String extendedDescHtml = StringUtil.toHtmlTable(row, this.getColumnNames());

					AlarmEvent ae = new AlarmEventPgSystemError(this, sql_state_code, stateCodeDesc, message, log_time);
					ae.setExtendedDescription(extendedDescText, extendedDescHtml);

					alarmHandler.addAlarm( ae );
				}
			}
			
			//-------------------------------------------------------
			// InsufficientResources
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("InsufficientResources"))
			{
				String stateCodeDesc = PostgresErrorCodeDictionary.getInstance().getDescriptionPlain(sql_state_code);

				// PG: 53000 == insufficient_resources
				// PG: 53100 == disk_full
				// PG: 53200 == out_of_memory
				// PG: 53300 == too_many_connections
				// PG: 53400 == configuration_limit_exceeded
				if (StringUtil.containsAny(sql_state_code, "53000", "53100", "53200", "53300", "53400"))
				{
					String extendedDescText = "";
					String extendedDescHtml = StringUtil.toHtmlTable(row, this.getColumnNames());

					AlarmEvent ae = new AlarmEventPgInsufficientResources(this, sql_state_code, stateCodeDesc, message, log_time);
					ae.setExtendedDescription(extendedDescText, extendedDescHtml);

					alarmHandler.addAlarm( ae );
				}
			}
			
			//-------------------------------------------------------
			// DeadlockDetected
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("Deadlocks"))
			{
				String stateCodeDesc = PostgresErrorCodeDictionary.getInstance().getDescriptionPlain(sql_state_code);

				// PG: 40P01 == deadlock_detected
				if (StringUtil.containsAny(sql_state_code, "40P01"))
				{
					// Get column 'detail', which holds various details about the deadlock
					String detail = message;
					if (findColumn("detail") != -1)
						detail = (String) row.get(findColumn("detail"));
					
					String extendedDescText = "";
					String extendedDescHtml = StringUtil.toHtmlTable(row, this.getColumnNames());

					AlarmEvent ae = new AlarmEventPgDeadlock(this, sql_state_code, stateCodeDesc, detail, log_time);
					ae.setExtendedDescription(extendedDescText, extendedDescHtml);

					alarmHandler.addAlarm( ae );
				}
			}
			
			//-------------------------------------------------------
			// AlterSystem
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("AlterSystem"))
			{
				// Try to parse the "config_param" and it's "value"
				//message -->> execute : ALTER SYSTEM SET shared_buffers TO '1317MB'
				if ("ALTER SYSTEM".equals(command_tag))
				{
					String type     = "";
					String cfgParam = "";
					String cfgVal   = "";

					String tmpStr = message;

					int pos = StringUtil.indexOfIgnoreCase(tmpStr, "ALTER SYSTEM");
					if (pos != -1)
					{
						// Remove everything up to "ALTER SYSTEM"
						tmpStr = tmpStr.substring(pos).trim();
						tmpStr = StringUtils.removeStartIgnoreCase(tmpStr, "ALTER SYSTEM").trim();

						// Is it SET or RESET, and remove the SET/RESET
						if (StringUtils.startsWithIgnoreCase(tmpStr, "SET "))
						{
							type = "set";
							tmpStr = StringUtils.removeStartIgnoreCase(tmpStr, "SET ").trim();
						}
						if (StringUtils.startsWithIgnoreCase(tmpStr, "RESET "))
						{
							type = "reset";
							tmpStr = StringUtils.removeStartIgnoreCase(tmpStr, "RESET ").trim();
						}

						// Set cfgParam to the REST of the string (the "value" part will later be removed
						cfgParam = tmpStr;
						
						if ("set".equals(type))
						{
							// set/trim cfgParam again
							// and remove the cfgParam from tmpStr
							int nextPos = StringUtils.indexOfAny(tmpStr, " TO ", " to ", " To ", " to ", "=");
							if (nextPos != -1)
							{
								cfgParam = tmpStr.substring(0, nextPos).trim();
								tmpStr = tmpStr.substring(nextPos).trim();
							}

							// Remove "TO" or "="
							if (StringUtils.startsWithIgnoreCase(tmpStr, "TO "))
								tmpStr = StringUtils.removeStartIgnoreCase(tmpStr, "TO ").trim();
							if (StringUtils.startsWithIgnoreCase(tmpStr, "="))
								tmpStr = StringUtils.removeStartIgnoreCase(tmpStr, "=").trim();

							// Now we should have the "config value(s)" left
							cfgVal = tmpStr.trim();
							
							// If it's surounded with 'val', remove it
							if (cfgVal.startsWith("'") && cfgVal.endsWith("'"))
								cfgVal = StringUtils.substringBetween(cfgVal, "'", "'");
						}
					} // end: parse

					if (StringUtil.isNullOrBlank(cfgParam))
						cfgParam = message;

					// Create and send alarm
					AlarmEvent ae = new AlarmEventConfigChanges(this, cfgParam, message, log_time);
					alarmHandler.addAlarm( ae );

				} // end: ALTER SYSTEM

			} // end: AlterSystem

		} // end: loop rows

	} // end: method

	public static final String  PROPKEY_alarm_InternalErrors        = CM_NAME + ".alarm.system.on.InternalErrors";
	public static final boolean DEFAULT_alarm_InternalErrors        = true;

	public static final String  PROPKEY_alarm_SystemErrors          = CM_NAME + ".alarm.system.on.SystemErrors";
	public static final boolean DEFAULT_alarm_SystemErrors          = true;

	public static final String  PROPKEY_alarm_InsufficientResources = CM_NAME + ".alarm.system.on.InsufficientResources";
	public static final boolean DEFAULT_alarm_InsufficientResources = true;

	public static final String  PROPKEY_alarm_Deadlocks             = CM_NAME + ".alarm.system.on.Deadlocks";
	public static final boolean DEFAULT_alarm_Deadlocks             = true;

	public static final String  PROPKEY_alarm_AlterSystem           = CM_NAME + ".alarm.system.on.AlterSystem";
	public static final boolean DEFAULT_alarm_AlterSystem           = true;

	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		list.add(new CmSettingsHelper("InternalErrors"        , PROPKEY_alarm_InternalErrors       , Boolean.class, conf.getBooleanProperty(PROPKEY_alarm_InternalErrors        , DEFAULT_alarm_InternalErrors        ), DEFAULT_alarm_InternalErrors        , "On Error Codes XX000='internal_error', XX001='data_corrupted', XX002='index_corrupted': send 'AlarmEventPgInternalError'." ));
		list.add(new CmSettingsHelper("SystemErrors"          , PROPKEY_alarm_SystemErrors         , Boolean.class, conf.getBooleanProperty(PROPKEY_alarm_SystemErrors          , DEFAULT_alarm_SystemErrors          ), DEFAULT_alarm_SystemErrors          , "On Error Codes 58000='system_error', 58030='io_error', 58P01='undefined_file', 58P02='duplicate_file': send 'AlarmEventPgSystemError'." ));
		list.add(new CmSettingsHelper("InsufficientResources" , PROPKEY_alarm_InsufficientResources, Boolean.class, conf.getBooleanProperty(PROPKEY_alarm_InsufficientResources , DEFAULT_alarm_InsufficientResources ), DEFAULT_alarm_InsufficientResources , "On Error Codes 53000='insufficient_resources', 53100='disk_full', 53200='out_of_memory', 53300='too_many_connections', 53400='configuration_limit_exceeded': send 'AlarmEventPgInsufficientResources'." ));
		list.add(new CmSettingsHelper("DeadlockDetected"      , PROPKEY_alarm_Deadlocks            , Boolean.class, conf.getBooleanProperty(PROPKEY_alarm_Deadlocks             , DEFAULT_alarm_Deadlocks             ), DEFAULT_alarm_Deadlocks             , "On Error Codes 40P01='deadlock_detected': send 'AlarmEventPgDeadlock'." ));
		list.add(new CmSettingsHelper("AlterSystem"           , PROPKEY_alarm_AlterSystem          , Boolean.class, conf.getBooleanProperty(PROPKEY_alarm_AlterSystem           , DEFAULT_alarm_AlterSystem           ), DEFAULT_alarm_AlterSystem           , "If we see 'ALTER SYSTEM SET ...' , send 'AlarmEventConfigChanges'." ));

		return list;
	}
}
