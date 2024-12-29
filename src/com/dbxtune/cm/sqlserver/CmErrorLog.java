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
package com.dbxtune.cm.sqlserver;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.alarm.AlarmHandler;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.alarm.events.AlarmEventConfigChanges;
import com.dbxtune.alarm.events.AlarmEventConfigResourceIsUsedUp;
import com.dbxtune.alarm.events.AlarmEventErrorLogEntry;
import com.dbxtune.alarm.events.AlarmEventFullTranLog;
import com.dbxtune.alarm.events.sqlserver.AlarmEventStackDump;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.CountersModelAppend;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.gui.TabularCntrPanelAppend;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.sql.conn.info.DbmsVersionInfoSqlServer;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmErrorLog
extends CountersModelAppend
{
	private static Logger        _logger          = Logger.getLogger(CmErrorLog.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmErrorLog.class.getSimpleName();
	public static final String   SHORT_NAME       = "Errorlog";
	public static final String   HTML_DESC        = 
		"<html>" +
		"Look at the SQL-Servers errorlog." +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"xp_readerrorlog"};
	public static final String[] NEED_ROLES       = new String[] {};//{"VIEW SERVER STATE"}; //    IF (NOT IS_SRVROLEMEMBER(N'securityadmin') = 1) // from: https://www.mssqltips.com/sqlservertip/1476/reading-the-sql-server-log-files-using-tsql/
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
	
	private void addTrendGraphs()
	{
	}

	@Override
	protected TabularCntrPanel createGui()
	{
		return new TabularCntrPanelAppend(this);
	}

	@Override
	public boolean checkDependsOnVersion(DbxConnection conn)
	{
		DbmsVersionInfoSqlServer versionInfo = (DbmsVersionInfoSqlServer) conn.getDbmsVersionInfo();
		if (versionInfo.isAzureDb() || versionInfo.isAzureSynapseAnalytics())
		{
			_logger.warn("When trying to initialize Counters Model '" + getName() + "', named '"+getDisplayName() + "', connected to Azure SQL Database or Analytics, which do NOT support reading the errorlog file via 'xp_readerrorlog'.");

			setActive(false, "This info is NOT available in Azure SQL Database or Azure Synapse/Analytics.");

			TabularCntrPanel tcp = getTabPanel();
			if (tcp != null)
			{
				tcp.setToolTipText("This info is NOT available in Azure SQL Database or Azure Synapse/Analytics.");
			}
			return false;
		}

		return super.checkDependsOnVersion(conn);
	}
	
	@Override
	public String[] getDependsOnConfigForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return NEED_CONFIG;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return "exec master.dbo.xp_readerrorlog 0, 1, NULL, NULL, ${prevSampleTs}, NULL"; 
	}

	/**
	 * Override the normal depends on role, and check that we can execute: xp_readerrorlog
	 * <br>
	 * If issues: inactuvate this CM and set message: grant exec on xp_readerrorlog to <current-login>
	 *  
	 */
	@Override
	public boolean checkDependsOnRole(DbxConnection conn)
	{
		String sql = "exec xp_readerrorlog 0, 1, null, null, '20500101' /* ${cmCollectorName} */ "; // a long time in the future
		try (Statement stmnt = conn.createStatement())
		{
			boolean hasRs = stmnt.execute(sql);
			if (hasRs)
			{
				ResultSet rs = stmnt.getResultSet();
				while(rs.next())
				{
					// do nothing, just read out all the rows (which should be zero)
				}
				rs.close();
			}
			return true;
		}
		catch (SQLException ex)
		{
			// Msg 229, Level 14, State 5:
			// Server 'prod-2a-mssql', Procedure 'xp_readerrorlog', Line 1 (Called from script row 15823)
			// The EXECUTE permission was denied on the object 'xp_readerrorlog', database 'mssqlsystemresource', schema 'sys'.
			if (ex.getErrorCode() == 229)
			{
				String username = "<currentUserName>";
				if (conn.getConnPropOrDefault() != null)
					username = conn.getConnPropOrDefault().getUsername();

				setActive(false, 
						"You currently do not have execution permissions on 'xp_readerrorlog'\n" +
						"Fix: grant exec on xp_readerrorlog to " + username); 

				TabularCntrPanel tcp = getTabPanel();
				if (tcp != null)
				{
					tcp.setToolTipText(
							"<html>You currently do not have execution permissions on 'xp_readerrorlog'.<br>" +
							"Fix: <code>grant exec on xp_readerrorlog to " + username + "</code></html>");
				}
			}
			else
			{
				_logger.error("checkDependsOnRole() - executing '"+sql+"'", ex);

				setActive(false, ex.getMessage()); 

				TabularCntrPanel tcp = getTabPanel();
				if (tcp != null)
				{
					tcp.setToolTipText(ex.getMessage());
				}
			}
			return false;
		}
	}

	@Override
	public String getSql()
	{
		// xp_readerrorlog parameters:
		// 1 - Value of error log file you want to read: 0 = current, 1 = Archive #1, 2 = Archive #2, etc...
		// 2 - Log file type: 1 or NULL = error log, 2 = SQL Agent log
		// 3 - Search string 1: String one you want to search for
		// 4 - Search string 2: String two you want to search for to further refine the results
		// 5 - Search from start time
		// 6 - Search to end time
		// 7 - Sort order for results: N'asc' = ascending, N'desc' = descending

		String sqlText = super.getSql();

		// Sample only records we have not seen previously...
		Timestamp prevSample = getPreviousSampleTime();
		if (prevSample == null)
		{
			// get all rows for the first sample... 
			// But the sendAlarmRequest() method will stop early... if isFirstTimeSample() is true
//			return "master.dbo.xp_readerrorlog 0, 1, NULL, NULL, NULL, NULL"; 
			return sqlText.replace("${prevSampleTs}", "NULL");
		}
		else
		{
			
//			return "master.dbo.xp_readerrorlog 0, 1, NULL, NULL, '" + prevSample + "', NULL"; 
			return sqlText.replace("${prevSampleTs}", "'" + prevSample.toString() + "'");
		}
	}


	
	@Override
	public void sendAlarmRequest()
	{
//		if ( ! hasDiffData() )
//			return;
		
		if ( ! AlarmHandler.hasInstance() )
			return;

		boolean debugPrint = Configuration.getCombinedConfiguration().getBooleanProperty("sendAlarmRequest.debug", _logger.isDebugEnabled());
		
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
		
//		CountersModel cm = this;
//		String dbmsSrvName = this.getServerName();

		// Exit early if NO ROWS
		if (lastRefreshRows == null)
			return;
		if (lastRefreshRows.size() == 0)
			return;

		int col_LogDate_pos     = findColumn("LogDate");
//		int col_ProcessInfo_pos = findColumn("ProcessInfo");
		int col_Text_pos        = findColumn("Text");

//		if (col_LogDate_pos < 0 || col_ProcessInfo_pos < 0 || col_Text_pos < 0)
//		{
//			_logger.error("When checking for alarms, could not find all columns. skipping this. [ErrorNumber_pos="+col_ErrorNumber_pos+", Severity_pos="+col_Severity_pos+", ErrorMessage_pos="+col_ErrorMessage_pos+"]");
//			return;
//		}
		if (col_LogDate_pos < 0)
		{
			_logger.error("When checking for alarms, could not find all columns. skipping this. [LogDate_pos="+col_LogDate_pos+"]");
			return;
		}
		if (col_Text_pos < 0)
		{
			_logger.error("When checking for alarms, could not find all columns. skipping this. [Text_pos="+col_Text_pos+"]");
			return;
		}
		
//		boolean debugPrint = System.getProperty("sendAlarmRequest.debug", "false").equalsIgnoreCase("true");

		List<String> errorNumberSkipList = null; // initiated later

		for (int r=0; r<lastRefreshRows.size(); r++)
		{
			List<Object> row = lastRefreshRows.get(r);

			Object o_Text = row.get(col_Text_pos);
			if (o_Text == null || !(o_Text instanceof String) )
			{
				_logger.error("When checking for alarms, the column 'Text' is NOT an String, skipping this row.");
				continue;
			}

			Object o_LogDate = row.get(col_LogDate_pos);
			if (o_LogDate == null || !(o_LogDate instanceof Timestamp) )
			{
				_logger.error("When checking for alarms, the column 'LogDate' is NOT an Timestamp, skipping this row.");
				continue;
			}

			Timestamp errorlogTs = (Timestamp) o_LogDate;
			String    errorTxt   = (String)    o_Text;
			int       errorNum   = -1;
			int       severity   = -1;

			// Error: 911, Severity: 16, State: 1.
			if (errorTxt.startsWith("Error: "))
			{
				int Severity_pos = errorTxt.indexOf("Severity: ");
				if (Severity_pos != -1)
				{
					// So we got row like: "Error: ###, Severity: ###"
					// Lets extract that and the NEXT row where the error message resides.
					if (r+1 < lastRefreshRows.size())
					{
						r++;
						row = lastRefreshRows.get(r);

						o_Text = row.get(col_Text_pos);
						if (o_Text != null && (o_Text instanceof String) )
						{
							// Parse the
							String[] sa = errorTxt.split(" ");
							if (sa.length >= 5)
							{
								errorNum = StringUtil.parseInt(sa[1].replace(",", ""), -1);
								severity = StringUtil.parseInt(sa[3].replace(",", ""), -1);

								errorTxt = (String) o_Text;
							}
						}
					}
				}
			}

			if (_logger.isDebugEnabled())
				_logger.debug("errorNum=" + errorNum + ", severity=" + severity + ", errorTxt=" + errorTxt);

			
			// Handle errors with NO ERROR numbers
			if (errorNum == -1 && severity == -1)
			{
				//-------------------------------------------------------
				// ConfigChanges
				//-------------------------------------------------------
				if (isSystemAlarmsForColumnEnabledAndInTimeRange("ConfigChanges"))
				{
					// Configuration option 'allow updates' changed from 0 to 1. Run the RECONFIGURE statement to install.
					// Configuration option 'show advanced options' changed from 0 to 1. Run the RECONFIGURE statement to install.
					if (errorTxt.indexOf("Configuration option '") >= 0 && errorTxt.indexOf("' changed from ") >= 0)
					{
						String configName = "unknown";
						String extendedDescText = errorTxt;
						String extendedDescHtml = errorTxt;

						// Get the configuration name
						int startPos = errorTxt.indexOf("'");
						if (startPos != -1)
						{
							startPos++;
							int endPos = errorTxt.indexOf("'", startPos);
							if (endPos != -1)
								configName = errorTxt.substring(startPos, endPos);
						}

						// TODO: POSSIBLY get column 'ProcessInfo', which holds 'spid###', then get ALL info for that SPID from: CmSessions
						//       This so we can see from what: login, hostname etc, that the re-config was made from...

						AlarmEvent ae = new AlarmEventConfigChanges(this, configName, errorTxt, errorlogTs);
						ae.setExtendedDescription(extendedDescText, extendedDescHtml);

						alarmHandler.addAlarm( ae );
					}
				}
				
				//-------------------------------------------------------
				// LongIoRequests
				//-------------------------------------------------------
				if (isSystemAlarmsForColumnEnabledAndInTimeRange("LongIoRequests"))
				{
					// SQL Server has encountered x occurrence(s) of I/O requests taking longer than 15 seconds to complete on file [Drive:\MSSQL\MSSQL.1\MSSQL\Data\xyz.mdf] in database [database].  The OS file handle is 0x00000000.  The offset of the latest long I/O is: 0x00000000000000
					String searchFor = " requests taking longer than ";
					if (errorTxt.indexOf(searchFor) >= 0)
					{
						String extendedDescText = errorTxt;
						String extendedDescHtml = errorTxt;

//						AlarmEvent ae = new AlarmEventErrorLogEntry(this, AlarmEvent.Severity.WARNING, searchFor, errorTxt);
						AlarmEvent ae = new AlarmEventErrorLogEntry(this, AlarmEvent.Severity.INFO, searchFor, errorTxt);
						ae.setExtendedDescription(extendedDescText, extendedDescHtml);
							
						alarmHandler.addAlarm( ae );
					}
				}
				
				// No need to continue, since we do NOT have error NUMBERS
				continue;

			} // end: Text only messages


			
			//-------------------------------------------------------
			// Full transaction log
			// Not 100% sure if they will be visible in the error log... but lets add it anyway...
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("TransactionLogFull"))
			{
				// Error: 9002, Severity: 17, State: 2
				// The log file for database '%.*ls' is full.  (...possibly more info...)
				if (errorNum == 9002)
				{
					String extendedDescText = errorTxt;
					String extendedDescHtml = errorTxt;

					// Get the configuration name
					String dbname = "-unknown-";
					int startPos = errorTxt.indexOf("'");
					if (startPos != -1)
					{
						startPos++;
						int endPos = errorTxt.indexOf("'", startPos);
						if (endPos != -1)
							dbname = errorTxt.substring(startPos, endPos);
					}
					
					AlarmEvent ae = new AlarmEventFullTranLog(this, 0, dbname, errorlogTs);
					ae.setExtendedDescription(extendedDescText, extendedDescHtml);
						
					alarmHandler.addAlarm( ae );
				}
			}

			//-------------------------------------------------------
			// PageErrorReadRetry
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("PageErrorReadRetry"))
			{
				// ErrorNumber=825, Severity=10, ErrorMessage=A read of the file '%ls' at offset %#016I64x succeeded after failing %d time(s) with error: %ls. Additional messages in the SQL Server error log and operating system error log may provide more detail. This error condition threatens database integrity and must be corrected. Complete a full database consistency check (DBCC CHECKDB). This error can be caused by many factors; for more information, see SQL Server Books Online.
				// Note that this is "only" severity 10... HENCE: We are **UPPGRADING** it to a more severe error
				if (errorNum == 825)
				{
					String extendedDescText = errorTxt;
					String extendedDescHtml = errorTxt;
						
					AlarmEvent.Severity alarmSeverity = AlarmEvent.Severity.WARNING;
					
					AlarmEvent ae = new AlarmEventErrorLogEntry(this, alarmSeverity, errorNum, severity, errorTxt, errorlogTs, -1);
					ae.setExtendedDescription(extendedDescText, extendedDescHtml);

					alarmHandler.addAlarm( ae );
				}
			}


			//-------------------------------------------------------
			// Out of 'user connections' --- or that we can't login dues to similar issues
			// Not 100% sure if they will be visible in the error log since it's only of Severity 16
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("UserConnections"))
			{
				// ErrorNumber=17300, Severity=16, ErrorMessage=SQL Server was unable to run a new system task, either because there is insufficient memory or the number of configured sessions exceeds the maximum allowed in the server. Verify that the server has adequate memory. Use sp_configure with option 'user connections' to check the maximum number of user connections allowed. Use sys.dm_exec_sessions to check the current number of sessions, including user processes.
				// ErrorNumber=17809, Severity=16, ErrorMessage=Could not connect because the maximum number of '%ld' user connections has already been reached. The system administrator can use sp_configure to increase the maximum value. The connection has been closed.%.*ls
				if (errorNum == 17300 || errorNum == 17809) // errorTxt.indexOf("user connections") >= 0)
				{
					String extendedDescText = errorTxt;
					String extendedDescHtml = errorTxt;

					AlarmEvent ae = new AlarmEventConfigResourceIsUsedUp(this, "user connections", errorNum, errorTxt, errorlogTs);
					ae.setExtendedDescription(extendedDescText, extendedDescHtml);

					alarmHandler.addAlarm( ae );
				}
			}

			
			//-------------------------------------------------------
			// Severity
			//-------------------------------------------------------
			if ( isSystemAlarmsForColumnEnabledAndInTimeRange("Severity"))
			{
				/* https://docs.microsoft.com/en-us/sql/relational-databases/errors-events/database-engine-error-severities?view=sql-server-ver15
				 * 
				 * 0-9    Informational messages that return status information or report errors that are not severe. The Database Engine does not raise system errors with severities of 0 through 9.
				 * 10     Informational messages that return status information or report errors that are not severe. For compatibility reasons, the Database Engine converts severity 10 to severity 0 before returning the error information to the calling application.
				 * 11-16  Indicate errors that can be corrected by the user.
				 * 11     Indicates that the given object or entity does not exist.
				 * 12     A special severity for queries that do not use locking because of special query hints. In some cases, read operations performed by these statements could result in inconsistent data, since locks are not taken to guarantee consistency.
				 * 13     Indicates transaction deadlock errors.
				 * 14     Indicates security-related errors, such as permission denied.
				 * 15     Indicates syntax errors in the Transact-SQL command.
				 * 16     Indicates general errors that can be corrected by the user.
				 * 
				 * ------------------------------------------------------------------------------------
				 * 17-19  Indicate software errors that cannot be corrected by the user. Inform your system administrator of the problem.
				 * ------------------------------------------------------------------------------------
				 * 17     Indicates that the statement caused SQL Server to run out of resources (such as memory, locks, or disk space for the database) or to exceed some limit set by the system administrator.
				 * 18     Indicates a problem in the Database Engine software, but the statement completes execution, and the connection to the instance of the Database Engine is maintained. The system administrator should be informed every time a message with a severity level of 18 occurs.
				 * 19     Indicates that a nonconfigurable Database Engine limit has been exceeded and the current batch process has been terminated. Error messages with a severity level of 19 or higher stop the execution of the current batch. Severity level 19 errors are rare and must be corrected by the system administrator or your primary support provider. Contact your system administrator when a message with a severity level 19 is raised. Error messages with a severity level from 19 through 25 are written to the error log.
				 * 
				 * ------------------------------------------------------------------------------------
				 * 20-24  Indicate system problems and are fatal errors, which means that the Database Engine task that is executing a statement or batch is no longer running. The task records information about what occurred and then terminates. In most cases, the application connection to the instance of the Database Engine may also terminate. If this happens, depending on the problem, the application might not be able to reconnect.
				 *        Error messages in this range can affect all of the processes accessing data in the same database and may indicate that a database or object is damaged. Error messages with a severity level from 19 through 24 are written to the error log.
				 * ------------------------------------------------------------------------------------
				 * 20     Indicates that a statement has encountered a problem. Because the problem has affected only the current task, it is unlikely that the database itself has been damaged.
				 * 21     Indicates that a problem has been encountered that affects all tasks in the current database, but it is unlikely that the database itself has been damaged.
				 * 22     Indicates that the table or index specified in the message has been damaged by a software or hardware problem.
				 *        Severity level 22 errors occur rarely. If one occurs, run DBCC CHECKDB to determine whether other objects in the database are also damaged. The problem might be in the buffer cache only and not on the disk itself. If so, restarting the instance of the Database Engine corrects the problem. To continue working, you must reconnect to the instance of the Database Engine; otherwise, use DBCC to repair the problem. In some cases, you may have to restore the database.
				 *        If restarting the instance of the Database Engine does not correct the problem, then the problem is on the disk. Sometimes destroying the object specified in the error message can solve the problem. For example, if the message reports that the instance of the Database Engine has found a row with a length of 0 in a nonclustered index, delete the index and rebuild it.
				 * 23     Indicates that the integrity of the entire database is in question because of a hardware or software problem.
				 *        Severity level 23 errors occur rarely. If one occurs, run DBCC CHECKDB to determine the extent of the damage. The problem might be in the cache only and not on the disk itself. If so, restarting the instance of the Database Engine corrects the problem. To continue working, you must reconnect to the instance of the Database Engine; otherwise, use DBCC to repair the problem. In some cases, you may have to restore the database.
				 * 24     Indicates a media failure. The system administrator may have to restore the database. You may also have to call your hardware vendor.
				 */

				int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_Severity, DEFAULT_alarm_Severity);

				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest("+getName()+"): threshold="+threshold+", severity='"+severity+"'.");

				if (severity > threshold && severity < 99)
				{
					// Initialize the error list if not done earlier
					if (errorNumberSkipList == null)
					{
						String errorListStr = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_ErrorNumberSkipList, DEFAULT_alarm_ErrorNumberSkipList);
						errorNumberSkipList = StringUtil.parseCommaStrToList(errorListStr);
					}
					if (errorNumberSkipList.contains( Integer.toString(errorNum)) )
					{
						// Skipping this error number
						if (_logger.isDebugEnabled())
							_logger.debug("ErrorNumber "+errorNum+" is part of the 'error-number-skip-list', so it wont be raised. (num="+errorNum+", severity="+severity+", text='"+errorTxt+"')");
					}
					else
					{
						String extendedDescText = errorTxt;
						String extendedDescHtml = errorTxt;

						AlarmEvent.Severity alarmSeverity = AlarmEvent.Severity.WARNING;
						if (severity > 17)
							alarmSeverity = AlarmEvent.Severity.ERROR;
						
						AlarmEvent ae = new AlarmEventErrorLogEntry(this, alarmSeverity, errorNum, severity, errorTxt, errorlogTs, threshold);
						ae.setExtendedDescription(extendedDescText, extendedDescHtml);

						alarmHandler.addAlarm( ae );
					}
				}
			} // end: severity
			
//			Possibly Check for Stack Traces...
//			see: code for ASE at the end --- ProcessInfected
//			start: "**Dump thread - "
//			end:   "Stack Signature for the dump is 0x"
				
			//-------------------------------------------------------
			// ProcessInfected
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("StackDump"))
			{
				// START ROW: 00:0004:00000:00308:2018/05/04 19:37:01.48 kernel  Current process (0x2bad014c) infected with signal 11 (SIGSEGV)
				//            ... collect anything in between, as the message ...
				//   END ROW: 00:0004:00000:00308:2018/05/04 19:37:01.48 kernel  end of stack trace, spid 308, kpid 732758348, suid 1
				// ErrorNumber=???, Severity=??, ErrorMessage=
				if (errorTxt.startsWith("**Dump thread - "))
				{
					// Read messages until 'end of stack trace, ' and stuff it in the below StringBuilder
					StringBuilder sb = new StringBuilder();

					// Continue to read messages (but do not move the "original r", create a new "rr" to loop on
					for (int rr=r; rr<lastRefreshRows.size(); rr++)
					{
						List<Object> rrRow = lastRefreshRows.get(rr);

						String ee_ErrorMessage = rrRow.get(col_Text_pos) + "";
						sb.append( ee_ErrorMessage ).append("\n");
						
						// STOP Looping when we find 'end of stack trace, '
						if (ee_ErrorMessage.startsWith("Stack Signature for the dump is 0x"))
							break;

						// OK: something is probably wrong (we didn't find 'end of stack trace, '), break the loop after 200 rows...
						if (rr >= 1000)
							break;
					}
					String fullErrorMessage = sb.toString();
						
					String extendedDescText = fullErrorMessage;
					String extendedDescHtml = "<pre>\n" + StringEscapeUtils.escapeHtml4(fullErrorMessage) + "\n</pre>";

//					AlarmEvent ae = new AlarmEventProcessInfected(this, fullErrorMessage);
					AlarmEvent ae = new AlarmEventStackDump(this, fullErrorMessage);
					ae.setExtendedDescription(extendedDescText, extendedDescHtml);
						
					alarmHandler.addAlarm( ae );
				}
			}

		} // end: rows loop
	}

	public static final String  PROPKEY_alarm_PageErrorReadRetry    = CM_NAME + ".alarm.system.on.PageErrorReadRetry";
	public static final boolean DEFAULT_alarm_PageErrorReadRetry    = true;

	public static final String  PROPKEY_alarm_UserConnections       = CM_NAME + ".alarm.system.on.UserConnections";
	public static final boolean DEFAULT_alarm_UserConnections       = true;

	public static final String  PROPKEY_alarm_TransactionLogFull    = CM_NAME + ".alarm.system.on.TransactionLogFull";
	public static final boolean DEFAULT_alarm_TransactionLogFull    = true;

	public static final String  PROPKEY_alarm_Severity              = CM_NAME + ".alarm.system.if.Severity.gt";
	public static final int     DEFAULT_alarm_Severity              = 16;

	public static final String  PROPKEY_alarm_ErrorNumberSkipList   = CM_NAME + ".alarm.system.errorNumber.skip.list";
//	public static final String  DEFAULT_alarm_ErrorNumberSkipList   = "";
	public static final String  DEFAULT_alarm_ErrorNumberSkipList   = "17810, 17832, 17836";
	// Below number are usually found when a "penetration test tool" is checking the environment
	// Num=17810, Severity=20, Text=Could not connect because the maximum number of '1' dedicated administrator connections already exists. Before a new connection can be made, the existing dedicated administrator connection must be dropped, either by logging off or ending the process. [CLIENT: 172.25.0.49] 
	// Num=17832, Severity=20, Text=The login packet used to open the connection is structurally invalid; the connection has been closed. Please contact the vendor of the client library. [CLIENT: 172.25.0.49] 
	// Num=17836, Severity=20, Text=Length specified in network packet payload did not match number of bytes read; the connection has been closed. Please contact the vendor of the client library. [CLIENT: 172.25.0.49] 

	public static final String  PROPKEY_alarm_ConfigChanges         = CM_NAME + ".alarm.system.on.ConfigChanges";
	public static final boolean DEFAULT_alarm_ConfigChanges         = true;

	public static final String  PROPKEY_alarm_LongIoRequests        = CM_NAME + ".alarm.system.on.LongIoRequests";
	public static final boolean DEFAULT_alarm_LongIoRequests        = true;

	public static final String  PROPKEY_alarm_StackDump             = CM_NAME + ".alarm.system.on.StackDump";
	public static final boolean DEFAULT_alarm_StackDump             = true;

	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;

		list.add(new CmSettingsHelper("PageErrorReadRetry"     , isAlarmSwitch, PROPKEY_alarm_PageErrorReadRetry , Boolean.class, conf.getBooleanProperty(PROPKEY_alarm_PageErrorReadRetry , DEFAULT_alarm_PageErrorReadRetry ), DEFAULT_alarm_PageErrorReadRetry , "On Error 825, send 'AlarmEventErrorLogEntry'. Note that this is 'only' severity 10... Hence: We are *UPPGRADING* it to a more severe error" ));
		list.add(new CmSettingsHelper("UserConnections"        , isAlarmSwitch, PROPKEY_alarm_UserConnections    , Boolean.class, conf.getBooleanProperty(PROPKEY_alarm_UserConnections    , DEFAULT_alarm_UserConnections    ), DEFAULT_alarm_UserConnections    , "On Error 17300 or 17809, send 'AlarmEventConfigResourceIsUsedUp'." ));
		list.add(new CmSettingsHelper("TransactionLogFull"     , isAlarmSwitch, PROPKEY_alarm_TransactionLogFull , Boolean.class, conf.getBooleanProperty(PROPKEY_alarm_TransactionLogFull , DEFAULT_alarm_TransactionLogFull ), DEFAULT_alarm_TransactionLogFull , "On Error 9002, send 'AlarmEventFullTranLog'." ));
		list.add(new CmSettingsHelper("ConfigChanges"          , isAlarmSwitch, PROPKEY_alarm_ConfigChanges      , Boolean.class, conf.getBooleanProperty(PROPKEY_alarm_ConfigChanges      , DEFAULT_alarm_ConfigChanges      ), DEFAULT_alarm_ConfigChanges      , "On error log message 'The configuration option '.*' has been changed', send 'AlarmEventConfigChanges'." ));
		list.add(new CmSettingsHelper("LongIoRequests"         , isAlarmSwitch, PROPKEY_alarm_LongIoRequests     , Boolean.class, conf.getBooleanProperty(PROPKEY_alarm_LongIoRequests     , DEFAULT_alarm_LongIoRequests     ), DEFAULT_alarm_LongIoRequests     , "On error log message 'I/O requests taking longer than 15 seconds to complete', send 'AlarmEventErrorLogEntry'." ));
		list.add(new CmSettingsHelper("StackDump"              , isAlarmSwitch, PROPKEY_alarm_StackDump          , Boolean.class, conf.getBooleanProperty(PROPKEY_alarm_StackDump          , DEFAULT_alarm_StackDump          ), DEFAULT_alarm_StackDump          , "On error log message '**Dump thread - ', collect the dump text and send 'AlarmEventStackDump'." ));

		list.add(new CmSettingsHelper("Severity"               , isAlarmSwitch, PROPKEY_alarm_Severity           , Integer.class, conf.getIntProperty    (PROPKEY_alarm_Severity           , DEFAULT_alarm_Severity           ), DEFAULT_alarm_Severity           , "If 'Severity' is greater than ## then send 'AlarmEventErrorLogEntry'." ));
		list.add(new CmSettingsHelper("SkipList ErrorNumber(s)",                PROPKEY_alarm_ErrorNumberSkipList, String .class, conf.getProperty       (PROPKEY_alarm_ErrorNumberSkipList, DEFAULT_alarm_ErrorNumberSkipList), DEFAULT_alarm_ErrorNumberSkipList, "Skip errors number in this list, that is if Severity is above that rule. format(comma separated list of numbers): 123, 321, 231" ));

		return list;
	}
	
	
//------------------------------
// The below was copied from Sybase ASE... and in SQL-Server all the details like error number are not in columns
//                                         so we would parse/look-for specific strings in the log...
//                                         if we want to send alarms... but lets keep the below comment for "a while"
//------------------------------

//	@Override
//	public void sendAlarmRequest()
//	{
////		if ( ! hasDiffData() )
////			return;
//		
//		if ( ! AlarmHandler.hasInstance() )
//			return;
//
//		// ABS Data from LAST refresh only
//		List<List<Object>> lastRefreshRows = getDataCollectionForLastRefresh();
//
//		// DO NOTHING on FIRST refresh
//		if ( isFirstTimeSample() )
//		{
//			String rowsInThisRefresh = " (Rows in first refresh was " + (lastRefreshRows == null ? "-null-" : lastRefreshRows.size()) + ")";
//
//			_logger.info("First time we check the errorlog, Alarms will NOT be checked, because it will include old error messages." + rowsInThisRefresh);
//			return;
//		}
//		
//		AlarmHandler alarmHandler = AlarmHandler.getInstance();
//		
////		CountersModel cm = this;
////		String dbmsSrvName = this.getServerName();
//
//		// Exit early if NO ROWS
//		if (lastRefreshRows == null)
//			return;
//		if (lastRefreshRows.size() == 0)
//			return;
//
//		int col_ErrorNumber_pos  = findColumn("ErrorNumber");
//		int col_Severity_pos     = findColumn("Severity");
//		int col_ErrorMessage_pos = findColumn("ErrorMessage");
//
//		if (col_ErrorNumber_pos < 0 || col_Severity_pos < 0 || col_ErrorMessage_pos < 0)
//		{
//			_logger.error("When checking for alarms, could not find all columns. skipping this. [ErrorNumber_pos="+col_ErrorNumber_pos+", Severity_pos="+col_Severity_pos+", ErrorMessage_pos="+col_ErrorMessage_pos+"]");
//			return;
//		}
//		
////		boolean debugPrint = System.getProperty("sendAlarmRequest.debug", "false").equalsIgnoreCase("true");
//
//		List<String> errorNumberSkipList = null; // initiated later
//		
//		for (int r=0; r<lastRefreshRows.size(); r++)
//		{
//			List<Object> row = lastRefreshRows.get(r);
//
//			Object o_errorNumber  = (Integer) row.get(col_ErrorNumber_pos);
//			Object o_severity     = (Integer) row.get(col_Severity_pos);
//			Object o_ErrorMessage = (String)  row.get(col_ErrorMessage_pos);
//
//			if (o_errorNumber == null || !(o_errorNumber instanceof Integer) )
//			{
//				_logger.error("When checking for alarms, the column 'ErrorNumber' is NOT an integer, skipping this row. [ErrorNumber="+o_errorNumber+"]");
//				continue;
//			}
//			if (o_severity == null || !(o_severity instanceof Integer) )
//			{
//				_logger.error("When checking for alarms, the column 'Severity' is NOT an integer, skipping this row. [Severity="+o_severity+"]");
//				continue;
//			}
//
//			int    errorNumber  = (Integer) o_errorNumber;
//			int    severity     = (Integer) o_severity;
//			String ErrorMessage = o_ErrorMessage == null ? "" : o_ErrorMessage.toString();
//
//			//-------------------------------------------------------
//			// There are not enough 'user connections' available to start a new process...
//			//-------------------------------------------------------
//			if (isSystemAlarmsForColumnEnabledAndInTimeRange("UserConnections"))
//			{
//				// ErrorNumber=1601, Severity=17, ErrorMessage=There are not enough 'user connections' available to start a new process...
//				if (errorNumber == 1601 || ErrorMessage.indexOf("There are not enough 'user connections' available to start a new process") >= 0)
//				{
//					String extendedDescText = ErrorMessage;
//					String extendedDescHtml = ErrorMessage;
//
//					AlarmEvent ae = new AlarmEventConfigResourceIsUsedUp(this, "number of user connections", 1601, ErrorMessage);
//					ae.setExtendedDescription(extendedDescText, extendedDescHtml);
//
//					alarmHandler.addAlarm( ae );
//				}
//			}
//
//			
//			//-------------------------------------------------------
//			// Full transaction log
//			//-------------------------------------------------------
//			if (isSystemAlarmsForColumnEnabledAndInTimeRange("TransactionLogFull"))
//			{
//				// ### maybe: ErrorNumber=2812, Severity=16, ErrorMessage=Stored procedure 'sp_thresholdaction' not found. Specify owner.objectname or use sp_help to check whether the object exists (sp_help may produce lots of output).
//
//				// ErrorNumber=7412, Severity=10, ErrorMessage=Space available in segment 'logsegment' has fallen critically low in database 'master'. All future modifications to this database will be suspended until the transaction log is successfully dumped and space becomes available.
//				// ErrorNumber=7413, Severity=10, ErrorMessage=1 task(s) are sleeping waiting for space to become available in the log segment for database GAS_prod.
//				if (errorNumber == 7412 || errorNumber == 7413)
//				{
//					String extendedDescText = ErrorMessage;
//					String extendedDescHtml = ErrorMessage;
//						
//					AlarmEvent ae = new AlarmEventFullTranLog(this, 0, "monErrorlog");
//					ae.setExtendedDescription(extendedDescText, extendedDescHtml);
//						
//					alarmHandler.addAlarm( ae );
//				}
//			}
//
//
//			//-------------------------------------------------------
//			// Severity
//			//-------------------------------------------------------
//			if ( isSystemAlarmsForColumnEnabledAndInTimeRange("Severity"))
//			{
//				/*
//				 * Level 10: Status Information
//                 *           Messages with severity level 10 provide additional information after certain commands have been executed and, typically, do not display the message number or severity level.
//                 *           
//                 * Level 11: Specified Database Object Not Found
//                 *           Messages with severity level 11 indicate that SAP ASE cannot find an object that is referenced in a command.
//                 *           
//                 * Level 12: Wrong Datatype Encountered
//                 *           Messages with severity level 12 indicate a problem with datatypes. For example, the user may have tried to enter a value of the wrong datatype in a column or to compare columns of different and incompatible datatypes.
//                 * 
//                 * Level 13: User Transaction Syntax Error
//                 *           Messages with severity level 13 indicate that something is wrong with the current user-defined transaction.
//                 * 
//                 * Level 14: Insufficient Permission to Execute Command
//                 *           Messages with severity level 14 mean that the user does not have the necessary permission to execute the command or access the database object. Users can ask the owner of the database object, the owner of the database, or the system administrator to grant them permission to use the command or object in question.
//                 * 
//                 * Level 15: Syntax Error in SQL Statement
//                 *           Messages with severity level 15 indicate that the user has made a mistake in the syntax of the command. The text of these error messages includes the line numbers on which the mistake occurs and the specific word near which it occurs.
//                 * 
//                 * Level 16: Miscellaneous User Error
//                 *           Most error messages with severity level 16 reflect that the user has made a nonfatal mistake that does not fall into any of the other categories. Severity level 16 and higher might also indicate software or hardware errors.
//                 * 
//                 * Level 17: Insufficient Resources
//                 *           Error messages with severity level 17 mean that the command has caused SAP ASE to run out of resources or to exceed some limit set by the system administrator. The user can continue, although he or she might not be able to execute a particular command
//                 * 
//                 * Level 18: Nonfatal Internal Error Detected
//                 *           Error messages with severity level 18 indicate an internal software bug. However, the command runs to completion, and the connection to SAP ASE is maintained.
//                 * 
//                 * Level 19: SAP ASE Fatal Error in Resource
//                 *           Error messages with severity level 19 indicate that some nonconfigurable internal limit has been exceeded and that SAP ASE cannot recover gracefully. You must reconnect to SAP ASE.
//                 * 
//                 * Level 20: SAP ASE Fatal Error in Current Process
//                 *           Error messages with severity level 20 indicate that SAP ASE has encountered a bug in a command. The problem has affected only the current process, and the database is unlikely to have been damaged. Run dbcc diagnostics. The user must reconnect to SAP ASE.
//                 * 
//                 * Level 21: SAP ASE Fatal Error in Database Processes
//                 *           Error messages with severity level 21 indicate that SAP ASE has encountered a bug that affects all the processes in the current database. However, it is unlikely that the database itself has been damaged. Restart SAP ASE and run dbcc diagnostics. The user must reconnect to SAP ASE.
//                 * 
//                 * Level 22: SAP ASE Fatal Error: Table Integrity Suspect
//                 *           Error messages with severity level 22 indicate that the table or index specified in the message has been previously damaged by a software or hardware problem.
//                 * 
//                 * Level 23: Fatal Error: Database Integrity Suspect
//                 *           Error messages with severity level 23 indicate that the integrity of the entire database is suspect due to previous damage caused by a software or hardware problem. Restart SAP ASE and run dbcc diagnostics.
//                 * 
//                 * Level 24: Hardware Error or System Table Corruption
//                 *           Error messages with severity level 24 reflect a media failure or (in rare cases) the corruption of sysusages. The system administrator may have to reload the database. You may need to call your hardware vendor.
//                 * 
//                 * Level 25: SAP ASE Internal Error
//                 *           Users do not see level 25 errors, as this level is used only for SAP ASE internal errors.
//                 * 
//                 * Level 26: Rule Error
//                 *           Error messages with severity level 26 reflect that an internal locking or synchronization rule has been broken. You must shut down and restart SAP ASE.
//                 *           
//				 * severity 99 is used for stacktraces etc... at least in MDA Table monErrorlog
//				 */
//
//				int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_Severity, DEFAULT_alarm_Severity);
//
//				if (severity > threshold && severity < 99)
//				{
//					// Initilize the errorlist if not done earlier
//					if (errorNumberSkipList == null)
//					{
//						String errorListStr = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_ErrorNumberSkipList, DEFAULT_alarm_ErrorNumberSkipList);
//						errorNumberSkipList = StringUtil.parseCommaStrToList(errorListStr);
//					}
//					if (errorNumberSkipList.contains( Integer.toString(errorNumber)) )
//					{
//						// Skipping this error number
//						if (_logger.isDebugEnabled())
//							_logger.debug("ErrorNumber "+errorNumber+" is part of the 'error-number-skip-list', so it wont be raised. (num="+errorNumber+", severity="+severity+", text='"+ErrorMessage+"')");
//					}
//					else
//					{
//						String extendedDescText = getErrorRecordAsText(lastRefreshRows, r);
//						String extendedDescHtml = getErrorRecordAsHtml(lastRefreshRows, r);
//
//						AlarmEvent.Severity alarmSeverity = AlarmEvent.Severity.WARNING;
//						if (severity > 17)
//							alarmSeverity = AlarmEvent.Severity.ERROR;
//						
//						AlarmEvent ae = new AlarmEventErrorLogEntry(this, alarmSeverity, errorNumber, severity, ErrorMessage, threshold);
//						ae.setExtendedDescription(extendedDescText, extendedDescHtml);
//
//						alarmHandler.addAlarm( ae );
//					}
//				}
//			}
//
//			
//			//-------------------------------------------------------
//			// ConfigChanges
//			//-------------------------------------------------------
//			if (isSystemAlarmsForColumnEnabledAndInTimeRange("ConfigChanges"))
//			{
//				// 00:0002:00000:00036:2019/03/26 01:41:39.17 server  The configuration option 'auditing' has been changed by 'sa' from '0' to '1'.
//				// ErrorNumber=???, Severity=??, ErrorMessage=
//				if (ErrorMessage.indexOf("The configuration option '") >= 0 && ErrorMessage.indexOf("' has been changed by ") >= 0)
//				{
//					String configName = "unknown";
//					String extendedDescText = ErrorMessage;
//					String extendedDescHtml = ErrorMessage;
//
//					// Get the configuration name
//					int startPos = ErrorMessage.indexOf("'");
//					if (startPos != -1)
//					{
//						startPos++;
//						int endPos = ErrorMessage.indexOf("'", startPos);
//						if (endPos != -1)
//							configName = ErrorMessage.substring(startPos, endPos);
//					}
//					
//					AlarmEvent ae = new AlarmEventConfigChanges(this, configName, ErrorMessage);
//					ae.setExtendedDescription(extendedDescText, extendedDescHtml);
//						
//					alarmHandler.addAlarm( ae );
//				}
//			}
//
//
//			//-------------------------------------------------------
//			// ProcessInfected
//			//-------------------------------------------------------
//			if (isSystemAlarmsForColumnEnabledAndInTimeRange("ProcessInfected"))
//			{
//				// START ROW: 00:0004:00000:00308:2018/05/04 19:37:01.48 kernel  Current process (0x2bad014c) infected with signal 11 (SIGSEGV)
//				//            ... collect anything in between, as the message ...
//				//   END ROW: 00:0004:00000:00308:2018/05/04 19:37:01.48 kernel  end of stack trace, spid 308, kpid 732758348, suid 1
//				// ErrorNumber=???, Severity=??, ErrorMessage=
//				if (ErrorMessage.indexOf("Current process (") >= 0 && ErrorMessage.indexOf(") infected with ") >= 0)
//				{
//					// Read messages until 'end of stack trace, ' and stuff it in the below StringBuilder
//					StringBuilder sb = new StringBuilder();
//
//					// Continue to read messages (but do not move the "original r", create a new "rr" to loop on
//					for (int rr=r; rr<lastRefreshRows.size(); rr++)
//					{
//						List<Object> rrRow = lastRefreshRows.get(rr);
//
//						String ee_ErrorMessage = rrRow.get(col_ErrorMessage_pos) + "";
//						sb.append( ee_ErrorMessage ).append("\n");
//						
//						// STOP Looping when we find 'end of stack trace, '
//						if (ee_ErrorMessage.startsWith("end of stack trace, "))
//							break;
//
//						// OK: something is probably wrong (we didn't find 'end of stack trace, '), break the loop after 200 rows...
//						if (rr >= 200)
//							break;
//					}
//					String fullErrorMessage = sb.toString();
//						
//					String extendedDescText = fullErrorMessage;
//					String extendedDescHtml = "<pre>\n" + fullErrorMessage + "\n</pre>";
//
//					AlarmEvent ae = new AlarmEventProcessInfected(this, fullErrorMessage);
//					ae.setExtendedDescription(extendedDescText, extendedDescHtml);
//						
//					alarmHandler.addAlarm( ae );
//				}
//			}
//		}
//	}
//
//	public static final String  PROPKEY_alarm_UserConnections       = CM_NAME + ".alarm.system.on.UserConnections";
//	public static final boolean DEFAULT_alarm_UserConnections       = true;
//
//	public static final String  PROPKEY_alarm_TransactionLogFull    = CM_NAME + ".alarm.system.on.TransactionLogFull";
//	public static final boolean DEFAULT_alarm_TransactionLogFull    = true;
//
//	public static final String  PROPKEY_alarm_Severity              = CM_NAME + ".alarm.system.if.Severity.gt";
//	public static final int     DEFAULT_alarm_Severity              = 16;
//
//	public static final String  PROPKEY_alarm_ErrorNumberSkipList   = CM_NAME + ".alarm.system.errorNumber.skip.list";
//	public static final String  DEFAULT_alarm_ErrorNumberSkipList   = "";
//
//	public static final String  PROPKEY_alarm_ConfigChanges         = CM_NAME + ".alarm.system.on.ConfigChanges";
//	public static final boolean DEFAULT_alarm_ConfigChanges         = true;
//
//	public static final String  PROPKEY_alarm_ProcessInfected       = CM_NAME + ".alarm.system.on.ProcessInfected";
//	public static final boolean DEFAULT_alarm_ProcessInfected       = true;
//
//	@Override
//	public List<CmSettingsHelper> getLocalAlarmSettings()
//	{
//		Configuration conf = Configuration.getCombinedConfiguration();
//		List<CmSettingsHelper> list = new ArrayList<>();
//
//		list.add(new CmSettingsHelper("UserConnections"        , PROPKEY_alarm_UserConnections    , Boolean.class, conf.getBooleanProperty(PROPKEY_alarm_UserConnections    , DEFAULT_alarm_UserConnections    ), DEFAULT_alarm_UserConnections    , "On Error 1601, send 'AlarmEventConfigResourceIsUsedUp'." ));
//		list.add(new CmSettingsHelper("TransactionLogFull"     , PROPKEY_alarm_TransactionLogFull , Boolean.class, conf.getBooleanProperty(PROPKEY_alarm_TransactionLogFull , DEFAULT_alarm_TransactionLogFull ), DEFAULT_alarm_TransactionLogFull , "On Error 7413, send 'AlarmEventFullTranLog'." ));
//		list.add(new CmSettingsHelper("ConfigChanges"          , PROPKEY_alarm_ConfigChanges      , Boolean.class, conf.getBooleanProperty(PROPKEY_alarm_ConfigChanges      , DEFAULT_alarm_ConfigChanges      ), DEFAULT_alarm_ConfigChanges      , "On error log message 'The configuration option '.*' has been changed', send 'AlarmEventConfigChanges'." ));
//		list.add(new CmSettingsHelper("ProcessInfected"        , PROPKEY_alarm_ProcessInfected    , Boolean.class, conf.getBooleanProperty(PROPKEY_alarm_ProcessInfected    , DEFAULT_alarm_ProcessInfected    ), DEFAULT_alarm_ProcessInfected    , "On error log message 'Current process .* infected with signal', send 'AlarmEventProcessInfected'." ));
//
//		list.add(new CmSettingsHelper("Severity"               , PROPKEY_alarm_Severity           , Integer.class, conf.getIntProperty    (PROPKEY_alarm_Severity           , DEFAULT_alarm_Severity           ), DEFAULT_alarm_Severity           , "If 'Severity' is greater than ## then send 'AlarmEventErrorLogEntry'." ));
//		list.add(new CmSettingsHelper("SkipList ErrorNumber(s)", PROPKEY_alarm_ErrorNumberSkipList, String .class, conf.getProperty       (PROPKEY_alarm_ErrorNumberSkipList, DEFAULT_alarm_ErrorNumberSkipList), DEFAULT_alarm_ErrorNumberSkipList, "Skip errors number in this list, that is if Severity is above that rule. format(comma separated list of numbers): 123, 321, 231" ));
//
//		return list;
//	}
}
