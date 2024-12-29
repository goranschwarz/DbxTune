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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import javax.naming.NameNotFoundException;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.alarm.AlarmHandler;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.alarm.events.AlarmEventConfigChanges;
import com.dbxtune.alarm.events.postgres.AlarmEventPgDeadlock;
import com.dbxtune.alarm.events.postgres.AlarmEventPgInsufficientResources;
import com.dbxtune.alarm.events.postgres.AlarmEventPgInternalError;
import com.dbxtune.alarm.events.postgres.AlarmEventPgSystemError;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CounterSample;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.CountersModelAppend;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.config.dict.MonTablesDictionary;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.config.dict.PostgresErrorCodeDictionary;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.gui.TabularCntrPanelAppend;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmErrorLogJson
extends CountersModelAppend
{
	private static Logger        _logger          = Logger.getLogger(CmErrorLogJson.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmErrorLogJson.class.getSimpleName();
	public static final String   SHORT_NAME       = "Errorlog";
	public static final String   HTML_DESC        = 
		"<html>" +
		"Look at the Postgres errorlog.<br>" +
		"<b>Note</b>: You must have enabled JSON logging.<br>" +
		"<br>" +
		"Suggested configuration:" +
		"<pre>" +
		"  ALTER SYSTEM SET logging_collector TO 'on'             \n" +
		"  ALTER SYSTEM SET log_destination   TO 'stderr,jsonlog' \n" +
		"</pre>" +
		"<br>" +
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
	public static final long     NEED_SRV_VERSION = Ver.ver(15);
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"pg_json_errorlog"};
	public static final String[] NEED_ROLES       = new String[] {}; // possibly: pg_read_server_files
	public static final String[] NEED_CONFIG      = new String[] {};

//	public static final String[] PCT_COLUMNS      = new String[] {};
//	public static final String[] DIFF_COLUMNS     = new String[] {"XXXdiffCols"};

//	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 300;
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

		return new CmErrorLogJson(counterController, guiController);
	}

	public CmErrorLogJson(ICounterController counterController, IGuiController guiController)
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
		String name = "pg_json_errorlog";
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addTable(name, "Reading the errorlog via SQL.");
			
			mtd.addColumn(name, "timestamp"         ,       "<html>Time stamp with milliseconds</html>");
			mtd.addColumn(name, "user"              ,       "<html>User name</html>");
			mtd.addColumn(name, "dbname"            ,       "<html>Database name</html>");
			mtd.addColumn(name, "pid"               ,       "<html>Process ID</html>");
			mtd.addColumn(name, "remote_host"       ,       "<html>Client host</html>");
			mtd.addColumn(name, "remote_port"       ,       "<html>Client port</html>");
			mtd.addColumn(name, "session_id"        ,       "<html>Session ID</html>");
			mtd.addColumn(name, "line_num"          ,       "<html>Per-session line number</html>");
			mtd.addColumn(name, "ps"                ,       "<html>Current ps display</html>");
			mtd.addColumn(name, "session_start"     ,       "<html>Session start time</html>");
			mtd.addColumn(name, "vxid"              ,       "<html>Virtual transaction ID</html>");
			mtd.addColumn(name, "txid"              ,       "<html>Regular transaction ID</html>");
			mtd.addColumn(name, "error_severity"    ,       "<html>Error severity</html>");
			mtd.addColumn(name, "state_code"        ,       "<html>SQLSTATE code</html>");
			mtd.addColumn(name, "state_code_desc"   ,       "<html>Description found at - https://www.postgresql.org/docs/current/errcodes-appendix.html </html>");
			mtd.addColumn(name, "message"           ,       "<html>Error message</html>");
			mtd.addColumn(name, "detail"            ,       "<html>Error message detail</html>");
			mtd.addColumn(name, "hint"              ,       "<html>Error message hint</html>");
			mtd.addColumn(name, "internal_query"    ,       "<html>Internal query that led to the error</html>");
			mtd.addColumn(name, "internal_position" ,       "<html>Cursor index into internal query</html>");
			mtd.addColumn(name, "context"           ,       "<html>Error context</html>");
			mtd.addColumn(name, "statement"         ,       "<html>Client-supplied query string</html>");
			mtd.addColumn(name, "cursor_position"   ,       "<html>Cursor index into query string</html>");
			mtd.addColumn(name, "func_name"         ,       "<html>Error location function name</html>");
			mtd.addColumn(name, "file_name"         ,       "<html>File name of error location</html>");
			mtd.addColumn(name, "file_line_num"     ,       "<html>File line number of the error location</html>");
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

			String sql = "select pg_current_logfile('jsonlog')";
			try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
			{
				while(rs.next())
				{
					_pg_current_json_logfile = rs.getString(1); // pg_current_logfile('jsonlog')
				}
			}
			catch (SQLException ex)
			{
				_logger.error("Problems getting current logfile name using SQL=|" + sql + "|.");
			}
		}
		else
		{
			_logger.error("This CM '" + CM_NAME + "' Needs version 16 or above, you calling getSqlForVersion() with version " + Ver.versionNumToStr(versionInfo.getLongVersion()));
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
			andTimestampGtLastSample = "  AND entry->>'timestamp' > '" + prevSample.toString() + "' \n";
		}
		
		String sql = ""
			+ "WITH errorlog AS \n"
			+ "( \n"
			+ "	SELECT v::json AS entry \n"
			+ "	FROM regexp_split_to_table( trim(pg_read_file('" + _pg_current_json_logfile + "'), E'\\n'), '[\\n\\r]+') AS v \n"
			+ ") \n"
			+ "SELECT \n"
			+ "     cast(entry->>'timestamp'         as timestamp(3) with time zone) AS timestamp \n"
			+ "    ,cast(entry->>'user'              as varchar(128)               ) AS user \n"
			+ "    ,cast(entry->>'dbname'            as varchar(128)               ) AS dbname \n"
			+ "    ,cast(entry->>'pid'               as int                        ) AS pid \n"
			+ "    ,cast(entry->>'remote_host'       as varchar(128)               ) AS remote_host \n"
			+ "    ,cast(entry->>'remote_port'       as int                        ) AS remote_port \n"
			+ "    ,cast(entry->>'session_id'        as varchar(128)               ) AS session_id \n"
			+ "    ,cast(entry->>'line_num'          as bigint                     ) AS line_num \n"
			+ "    ,cast(entry->>'ps'                as varchar(128)               ) AS ps \n"
			+ "    ,cast(entry->>'session_start'     as varchar(128)               ) AS session_start \n"
			+ "    ,cast(entry->>'vxid'              as varchar(32)                ) AS vxid \n"
			+ "    ,cast(entry->>'txid'              as varchar(32)                ) AS txid \n"
			+ "    ,cast(entry->>'error_severity'    as varchar(15)                ) AS error_severity \n"
			+ "    ,cast(entry->>'state_code'        as varchar(15)                ) AS state_code \n"
			+ "    ,cast(''                          as varchar(60)                ) AS state_code_desc \n"
//			+ "    ,cast(entry->>'message'           as varchar(4000)              ) AS message \n" // ADD in localCalculation()
			+ "    ,cast(entry->>'message'           as text                       ) AS message \n" // ADD in localCalculation()
			+ "    ,cast(entry->>'detail'            as varchar(512)               ) AS detail \n"
			+ "    ,cast(entry->>'hint'              as varchar(512)               ) AS hint \n"
			+ "    ,cast(entry->>'internal_query'    as varchar(4000)              ) AS internal_query \n"
			+ "    ,cast(entry->>'internal_position' as int                        ) AS internal_position \n"
			+ "    ,cast(entry->>'context'           as varchar(128)               ) AS context \n"
			+ "    ,cast(entry->>'statement'         as varchar(4000)              ) AS statement \n"
			+ "    ,cast(entry->>'cursor_position'   as bigint                     ) AS cursor_position \n"
			+ "    ,cast(entry->>'func_name'         as varchar(128)               ) AS func_name \n"
			+ "    ,cast(entry->>'file_name'         as varchar(128)               ) AS file_name \n"
			+ "    ,cast(entry->>'file_line_num'     as int                        ) AS file_line_num \n"
			+ "    ,cast(entry->>'application_name'  as varchar(128)               ) AS application_name \n"
			+ "    ,cast(entry->>'backend_type'      as varchar(128)               ) AS backend_type \n"
			+ "    ,cast(entry->>'leader_pid'        as int                        ) AS leader_pid \n"
			+ "    ,cast(entry->>'query_id'          as bigint                     ) AS query_id \n"
			+ "    ,cast(''                          as varchar(128)               ) AS pg_current_logfile \n" // ADD in localCalculation()
			+ "FROM errorlog \n"
			+ "WHERE 1 = 1 \n"
			+ andTimestampGtLastSample
			+ "";
		
		
		return sql; 
	}

	@Override
	public void localCalculation(CounterSample newSample)
	{
		PostgresErrorCodeDictionary dict = PostgresErrorCodeDictionary.getInstance();

		int pos__state_code         = newSample.findColumn("state_code");
		int pos__state_code_desc    = newSample.findColumn("state_code_desc");
		int pos__pg_current_logfile = newSample.findColumn("pg_current_logfile");

		for (int r = 0; r < newSample.getRowCount(); r++)
		{
			if (pos__state_code != -1 && pos__state_code_desc != -1)
			{
				String state_code      = newSample.getValueAsString(r, pos__state_code);
				String state_code_desc = dict.getDescriptionPlain(state_code);

				newSample.setValueAt(state_code_desc, r, pos__state_code_desc);
			}
			
			if (pos__pg_current_logfile != -1)
			{
				newSample.setValueAt(_pg_current_json_logfile, r, pos__pg_current_logfile);
			}
		}
	}
	
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


	private String _pg_current_json_logfile = null;
	private String _pg_data_directory = null;

	/**
	 * Override the normal depends on role, and check that we can execute: xp_readerrorlog
	 * <br>
	 * If issues: in-activate this CM and set message: grant exec on xp_readerrorlog to <current-login>
	 */
	@Override
	public boolean checkDependsOnRole(DbxConnection conn)
	{
//(new Exception("DUMMY Exception from ::: checkDependsOnRole")).printStackTrace();
		String sql = "select pg_read_file('postmaster.pid'), pg_current_logfile('jsonlog'), current_setting('data_directory')";
		try (Statement stmnt = conn.createStatement())
		{
			boolean hasRs = stmnt.execute(sql);
			if (hasRs)
			{
				ResultSet rs = stmnt.getResultSet();
				while(rs.next())
				{
					//String pidFile         = rs.getString(1); // pid file
					_pg_current_json_logfile = rs.getString(2); // pg_current_logfile('jsonlog')
					_pg_data_directory       = rs.getString(3); // current_setting('data_directory')
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
			return false;
		}
		
		// Check if JSON Logging is enabled
		if (StringUtil.isNullOrBlank(_pg_current_json_logfile))
		{
			_logger.error("To read the errorlog, this CM depends on JSON logging. See 'https://www.cybertec-postgresql.com/en/json-logs-in-postgresql-15/' for more info.");

			setActive(false, "This depends on JSON Logging.\n" +
					"See 'https://www.cybertec-postgresql.com/en/json-logs-in-postgresql-15/' for more info.\n" +
					""); 

			TabularCntrPanel tcp = getTabPanel();
			if (tcp != null)
			{
				tcp.setToolTipText(
						"<html>This depends on JSON Logging.<br>" +
						"See 'https://www.cybertec-postgresql.com/en/json-logs-in-postgresql-15/' for more info.<br>" +
						"</html>");
			}
			return false;
		}

		return true;
	}

	@Override
	public String getToolTipTextOnTableCell(MouseEvent e, String colName, Object cellValue, int modelRow, int modelCol) 
	{
		if ("state_code".equals(colName) )
		{
			if (cellValue != null)
			{
				return PostgresErrorCodeDictionary.getInstance().getDescriptionHtml(cellValue + "");
			}
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
		int col_timestamp__pos   = findColumn("timestamp");
		int col_ps__pos          = findColumn("ps");
		int col_state_code__pos  = findColumn("state_code");
		int col_message__pos     = findColumn("message");

		if (col_timestamp__pos == -1 || col_ps__pos == -1 || col_state_code__pos == -1 || col_message__pos == -1)
		{
			_logger.error("When checking for alarms, could not find all columns. skipping this. [timestamp=" + col_timestamp__pos + ", ps='" + col_ps__pos + "', state_code=" + col_state_code__pos + ", message=" + col_message__pos + "]");
			return;
		}
		
		//boolean debugPrint = System.getProperty("sendAlarmRequest.debug", "false").equalsIgnoreCase("true");

		// Loop all rows
		for (int r=0; r<lastRefreshRows.size(); r++)
		{
			List<Object> row = lastRefreshRows.get(r);

			// Get values...
			Timestamp timestamp  = (Timestamp) row.get(col_timestamp__pos);
			String    ps         = (String)    row.get(col_ps__pos);          // ps stands for ??? Postgres System ???
			String    state_code = (String)    row.get(col_state_code__pos);
			String    message    = (String)    row.get(col_message__pos);

//System.out.println("PG:CmErrorlog.sendAlarm.row["+r+"]: timestamp=|" + timestamp + "|, ps=|" + ps + "|, state_code=|" + state_code + "|, message=|" + message + "|.");

			// null "protection"
			if (timestamp  == null) timestamp  = null;
			if (ps         == null) ps         = "";
			if (state_code == null) state_code = "";
			if (message    == null) message    = "";

			//-------------------------------------------------------
			// InternalErrors
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("InternalErrors"))
			{
				// PG: XX000 == internal_error
				// PG: XX001 == data_corrupted
				// PG: XX002 == index_corrupted
				if (StringUtil.containsAny(state_code, "XX000", "XX001", "XX002"))
				{
					String stateCodeDesc = PostgresErrorCodeDictionary.getInstance().getDescriptionPlain(state_code);

					// Example of messages (when I corrupted a file):
					//  - page verification failed, calculated checksum 41079 but expected 44920
					//  - invalid page in block 0 of relation base/59493/3337937
					
					// Possibly parse out the DBID/OBJID and get any names instead of the ID's
					//String[] sa = message.split("\\/[0-9]+\\/[0-9]+"); // NOT TESTED
					//String dbname     = DbmsObjectIdCache.getInstance().getDBName(dbid);
					//String objectName = DbmsObjectIdCache.getInstance().getObjectName(dbid, objectid);

					String extendedDescText = "";
					String extendedDescHtml = StringUtil.toHtmlTable(row, this.getColumnNames());

					AlarmEvent ae = new AlarmEventPgInternalError(this, state_code, stateCodeDesc, message, timestamp);
					ae.setExtendedDescription(extendedDescText, extendedDescHtml);

					alarmHandler.addAlarm( ae );
				}
			} // end: InternalErrors

			//-------------------------------------------------------
			// SystemError (errors external to PostgreSQL itself)
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("SystemErrors"))
			{
				String stateCodeDesc = PostgresErrorCodeDictionary.getInstance().getDescriptionPlain(state_code);

				// PG: 58000 == system_error
				// PG: 58030 == io_error
				// PG: 58P01 == undefined_file
				// PG: 58P02 == duplicate_file
				if (StringUtil.containsAny(state_code, "58000", "58030", "58P01", "58P02"))
				{
					String extendedDescText = "";
					String extendedDescHtml = StringUtil.toHtmlTable(row, this.getColumnNames());

					AlarmEvent ae = new AlarmEventPgSystemError(this, state_code, stateCodeDesc, message, timestamp);
					ae.setExtendedDescription(extendedDescText, extendedDescHtml);

					alarmHandler.addAlarm( ae );
				}
			}
			
			//-------------------------------------------------------
			// InsufficientResources
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("InsufficientResources"))
			{
				String stateCodeDesc = PostgresErrorCodeDictionary.getInstance().getDescriptionPlain(state_code);

				// PG: 53000 == insufficient_resources
				// PG: 53100 == disk_full
				// PG: 53200 == out_of_memory
				// PG: 53300 == too_many_connections
				// PG: 53400 == configuration_limit_exceeded
				if (StringUtil.containsAny(state_code, "53000", "53100", "53200", "53300", "53400"))
				{
					String extendedDescText = "";
					String extendedDescHtml = StringUtil.toHtmlTable(row, this.getColumnNames());

					AlarmEvent ae = new AlarmEventPgInsufficientResources(this, state_code, stateCodeDesc, message, timestamp);
					ae.setExtendedDescription(extendedDescText, extendedDescHtml);

					alarmHandler.addAlarm( ae );
				}
			}
			
			//-------------------------------------------------------
			// DeadlockDetected
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("Deadlocks"))
			{
				String stateCodeDesc = PostgresErrorCodeDictionary.getInstance().getDescriptionPlain(state_code);

				// PG: 40P01 == deadlock_detected
				if (StringUtil.containsAny(state_code, "40P01"))
				{
					// Get column 'detail', which holds various details about the deadlock
					String detail = message;
					if (findColumn("detail") != -1)
						detail = (String) row.get(findColumn("detail"));
					
					String extendedDescText = "";
					String extendedDescHtml = StringUtil.toHtmlTable(row, this.getColumnNames());

					AlarmEvent ae = new AlarmEventPgDeadlock(this, state_code, stateCodeDesc, detail, timestamp);
					ae.setExtendedDescription(extendedDescText, extendedDescHtml);

					alarmHandler.addAlarm( ae );
				}
			}
			
			//-------------------------------------------------------
			// AlterSystem
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("AlterSystem"))
			{
				// Example
				// +-----------------------+--------+------+---------+-------------+-----------+---------------+--------+------------+-----------------------+------+----+--------------+----------+---------------+-----------------------------------------------------+------+----+--------------+-----------------+-------+---------+---------------+---------+---------+-------------+----------------+--------------+----------+--------+-----------------------+
				// |timestamp              |user    |dbname|pid      |remote_host  |remote_port|session_id     |line_num|ps          |session_start          |vxid  |txid|error_severity|state_code|state_code_desc|message                                              |detail|hint|internal_query|internal_position|context|statement|cursor_position|func_name|file_name|file_line_num|application_name|backend_type  |leader_pid|query_id|pg_current_logfile     |
				// +-----------------------+--------+------+---------+-------------+-----------+---------------+--------+------------+-----------------------+------+----+--------------+----------+---------------+-----------------------------------------------------+------+----+--------------+-----------------+-------+---------+---------------+---------+---------+-------------+----------------+--------------+----------+--------+-----------------------+
				// |2024-01-20 23:08:53.434|postgres|qwerty|3 196 557|192.168.0.161|     53 745|65abf12b.30c68d|      19|ALTER SYSTEM|2024-01-20 17:13:31 CET|24/275|0   |LOG           |          |               |execute : ALTER SYSTEM SET shared_buffers TO '1317MB'|      |    |              |                0|       |         |              0|         |         |            0|SqlWindow       |client backend|         0|       0|log/postgresql-Sat.json|
				// +-----------------------+--------+------+---------+-------------+-----------+---------------+--------+------------+-----------------------+------+----+--------------+----------+---------------+-----------------------------------------------------+------+----+--------------+-----------------+-------+---------+---------------+---------+---------+-------------+----------------+--------------+----------+--------+-----------------------+
				
				
				// Try to parse the "config_param" and it's "value"
				//message -->> execute : ALTER SYSTEM SET shared_buffers TO '1317MB'
				if ("ALTER SYSTEM".equals(ps))
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
					AlarmEvent ae = new AlarmEventConfigChanges(this, cfgParam, message, timestamp);
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
