/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
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
package com.asetune.cm.ase;

import java.sql.Connection;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.alarm.AlarmHandler;
import com.asetune.alarm.events.AlarmEvent;
import com.asetune.alarm.events.AlarmEventConfigChanges;
import com.asetune.alarm.events.AlarmEventConfigResourceIsUsedUp;
import com.asetune.alarm.events.AlarmEventErrorLogEntry;
import com.asetune.alarm.events.AlarmEventFullTranLog;
import com.asetune.alarm.events.AlarmEventProcessInfected;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.CountersModelAppend;
import com.asetune.config.dict.AseErrorMessageDictionary;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.gui.TabularCntrPanelAppend;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;
import com.asetune.utils.Ver;

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
		"Look at the ASE Servers errorlog." +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"monErrorLog"};
	public static final String[] NEED_ROLES       = new String[] {"mon_role"};
	public static final String[] NEED_CONFIG      = new String[] {"enable monitoring=1", "errorlog pipe active=1", "errorlog pipe max=200"};

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
	
	private void addTrendGraphs()
	{
	}

	@Override
	protected TabularCntrPanel createGui()
	{
		return new TabularCntrPanelAppend(this);
	}

	@Override
	public String[] getDependsOnConfigForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		return NEED_CONFIG;
	}

	@Override
	public String getSqlForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		String cols1, cols2, cols3;
		cols1 = cols2 = cols3 = "";

		String instanceId = "";
		if (isClusterEnabled)
		{
			instanceId = "InstanceID, ";
		}

		cols1 = "Time, "+instanceId+"SPID, KPID, FamilyID, EngineNumber, ErrorNumber, Severity, ";
		cols2 = "";
		cols3 = "ErrorMessage";
//		if (srvVersion >= 12510)
//		if (srvVersion >= 1251000)
		if (srvVersion >= Ver.ver(12,5,1))
		{
			cols2 = "State, ";
		}
	
		String sql = 
			"select " + cols1 + cols2 + cols3 + "\n" +
			"from master..monErrorLog\n";

		return sql;
	}

	@Override
	public String getSql()
	{
		// Sample only records we have not seen previously...
		// But wait: The monErrorLog is a "state" table, so we would only get the last records anyway... or...
		// True but: if the ASE Connection is lost, we will reconnect... and then we get all old records once again!
		//           which means that we will report any errors *again*, which does happen!
		//           So this is as "failsafe" to net report old records after a reconnect-on-communication-failure
		Timestamp prevSample = getPreviousSampleTime();
		if (prevSample == null)
		{
			// get all rows for the first sample... 
			// But the sendAlarmRequest() method will stop early... if isFirstTimeSample() is true
			setSqlWhere(""); 
		}
		else
		{
			setSqlWhere("WHERE Time > '"+prevSample+"' "); 
		}

		// Now get the SQL from super method...
		return super.getSql();
	}

	

	/**
	 * Get one row vertically formated
	 * <pre>
	 * colName 1  : value
	 * colName 2  : value
	 * colName 99 : value
	 * </pre>
	 * @param lastRefreshRows
	 * @param rowNum
	 * @return
	 */
	private String getErrorRecordAsText(List<List<Object>> lastRefreshRows, int rowNum)
	{
		return getErrorRecordAsTextOrHtml(lastRefreshRows, rowNum, false);
	}
	
	private String getErrorRecordAsHtml(List<List<Object>> lastRefreshRows, int rowNum)
	{
		return getErrorRecordAsTextOrHtml(lastRefreshRows, rowNum, true);
	}

	private String getErrorRecordAsTextOrHtml(List<List<Object>> lastRefreshRows, int rowNum, boolean asHtml)
	{
		List<Object> row = lastRefreshRows.get(rowNum);

		// Get max Column name len
		int colNameMaxLen = 0;
		for (int c=0; c<row.size(); c++)
			colNameMaxLen = Math.max(colNameMaxLen, getColumnName(c).length());

		StringBuilder sb = new StringBuilder();
		
		if (asHtml)
		{
			sb.append("<table class='errorlogTableVertical'>\n");
//			sb.append("  <tr> <th>Column Name</th> <th>Value</th> </tr>\n");
			for (int c=0; c<row.size(); c++)
			{
				String name = getColumnName(c);
				Object val  = row.get(c);
				
				sb.append("  <tr> <td><b>").append(name).append("</b></td> <td>").append(val).append("</td> </tr>\n");
			}
			sb.append("</table>\n");
		}
		else
		{
			for (int c=0; c<row.size(); c++)
			{
				String name = getColumnName(c);
				Object val  = row.get(c);

				sb.append(StringUtil.left(name, colNameMaxLen));
				sb.append(" : ");
				sb.append(val);
				sb.append("\n");
			}
		}
		
		return sb.toString();
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

		int col_ErrorNumber_pos  = findColumn("ErrorNumber");
		int col_Severity_pos     = findColumn("Severity");
		int col_ErrorMessage_pos = findColumn("ErrorMessage");

		if (col_ErrorNumber_pos < 0 || col_Severity_pos < 0 || col_ErrorMessage_pos < 0)
		{
			_logger.error("When checking for alarms, could not find all columns. skipping this. [ErrorNumber_pos="+col_ErrorNumber_pos+", Severity_pos="+col_Severity_pos+", ErrorMessage_pos="+col_ErrorMessage_pos+"]");
			return;
		}
		
//		boolean debugPrint = System.getProperty("sendAlarmRequest.debug", "false").equalsIgnoreCase("true");

		List<String> errorNumberSkipList = null; // initiated later
		
		for (int r=0; r<lastRefreshRows.size(); r++)
		{
			List<Object> row = lastRefreshRows.get(r);

			Object o_errorNumber  = row.get(col_ErrorNumber_pos);
			Object o_severity     = row.get(col_Severity_pos);
			Object o_ErrorMessage = row.get(col_ErrorMessage_pos);

			if (o_errorNumber == null || !(o_errorNumber instanceof Integer) )
			{
				_logger.error("When checking for alarms, the column 'ErrorNumber' is NOT an integer, skipping this row. [ErrorNumber="+o_errorNumber+"]");
				continue;
			}
			if (o_severity == null || !(o_severity instanceof Integer) )
			{
				_logger.error("When checking for alarms, the column 'Severity' is NOT an integer, skipping this row. [Severity="+o_severity+"]");
				continue;
			}

			int    errorNumber  = (Integer) o_errorNumber;
			int    severity     = (Integer) o_severity;
			String ErrorMessage = o_ErrorMessage == null ? "" : o_ErrorMessage.toString();

			//-------------------------------------------------------
			// There are not enough 'user connections' available to start a new process
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("UserConnections"))
			{
				// ErrorNumber=1601, Severity=17, ErrorMessage=There are not enough 'user connections' available to start a new process...
				if (errorNumber == 1601 || ErrorMessage.indexOf("There are not enough 'user connections' available to start a new process") >= 0)
				{
					String extendedDescText = ErrorMessage;
					String extendedDescHtml = ErrorMessage;

					AlarmEvent ae = new AlarmEventConfigResourceIsUsedUp(this, "number of user connections", 1601, ErrorMessage);
					ae.setExtendedDescription(extendedDescText, extendedDescHtml);

					alarmHandler.addAlarm( ae );
				}
			}

			
			//-------------------------------------------------------
			// Full transaction log
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("TransactionLogFull"))
			{
				// ### maybe: ErrorNumber=2812, Severity=16, ErrorMessage=Stored procedure 'sp_thresholdaction' not found. Specify owner.objectname or use sp_help to check whether the object exists (sp_help may produce lots of output).

				// ErrorNumber=7412, Severity=10, ErrorMessage=Space available in segment 'logsegment' has fallen critically low in database 'master'. All future modifications to this database will be suspended until the transaction log is successfully dumped and space becomes available.
				// ErrorNumber=7413, Severity=10, ErrorMessage=1 task(s) are sleeping waiting for space to become available in the log segment for database GAS_prod.
				if (errorNumber == 7412 || errorNumber == 7413)
				{
					String extendedDescText = ErrorMessage;
					String extendedDescHtml = ErrorMessage;

					// Try to copy everything between "database" and a dot (".")
					String dbname = StringUtils.substringBetween(ErrorMessage, "database", ".");
					// Strip out chars we do not need or want
					if (StringUtil.hasValue(dbname))
						dbname = dbname.replace('\'', ' ').replace('.', ' ').trim();

					if (StringUtil.isNullOrBlank(dbname))
						dbname = "-no-dbname-found-in-errorlog-";

					AlarmEvent ae = new AlarmEventFullTranLog(this, 0, dbname);
					ae.setExtendedDescription(extendedDescText, extendedDescHtml);
						
					alarmHandler.addAlarm( ae );
				}
			}


			//-------------------------------------------------------
			// Severity
			//-------------------------------------------------------
			if ( isSystemAlarmsForColumnEnabledAndInTimeRange("Severity"))
			{
				/*
				 * Level 10: Status Information
                 *           Messages with severity level 10 provide additional information after certain commands have been executed and, typically, do not display the message number or severity level.
                 *           
                 * Level 11: Specified Database Object Not Found
                 *           Messages with severity level 11 indicate that SAP ASE cannot find an object that is referenced in a command.
                 *           
                 * Level 12: Wrong Datatype Encountered
                 *           Messages with severity level 12 indicate a problem with datatypes. For example, the user may have tried to enter a value of the wrong datatype in a column or to compare columns of different and incompatible datatypes.
                 * 
                 * Level 13: User Transaction Syntax Error
                 *           Messages with severity level 13 indicate that something is wrong with the current user-defined transaction.
                 * 
                 * Level 14: Insufficient Permission to Execute Command
                 *           Messages with severity level 14 mean that the user does not have the necessary permission to execute the command or access the database object. Users can ask the owner of the database object, the owner of the database, or the system administrator to grant them permission to use the command or object in question.
                 * 
                 * Level 15: Syntax Error in SQL Statement
                 *           Messages with severity level 15 indicate that the user has made a mistake in the syntax of the command. The text of these error messages includes the line numbers on which the mistake occurs and the specific word near which it occurs.
                 * 
                 * Level 16: Miscellaneous User Error
                 *           Most error messages with severity level 16 reflect that the user has made a nonfatal mistake that does not fall into any of the other categories. Severity level 16 and higher might also indicate software or hardware errors.
                 * 
                 * Level 17: Insufficient Resources
                 *           Error messages with severity level 17 mean that the command has caused SAP ASE to run out of resources or to exceed some limit set by the system administrator. The user can continue, although he or she might not be able to execute a particular command
                 * 
                 * Level 18: Nonfatal Internal Error Detected
                 *           Error messages with severity level 18 indicate an internal software bug. However, the command runs to completion, and the connection to SAP ASE is maintained.
                 * 
                 * Level 19: SAP ASE Fatal Error in Resource
                 *           Error messages with severity level 19 indicate that some nonconfigurable internal limit has been exceeded and that SAP ASE cannot recover gracefully. You must reconnect to SAP ASE.
                 * 
                 * Level 20: SAP ASE Fatal Error in Current Process
                 *           Error messages with severity level 20 indicate that SAP ASE has encountered a bug in a command. The problem has affected only the current process, and the database is unlikely to have been damaged. Run dbcc diagnostics. The user must reconnect to SAP ASE.
                 * 
                 * Level 21: SAP ASE Fatal Error in Database Processes
                 *           Error messages with severity level 21 indicate that SAP ASE has encountered a bug that affects all the processes in the current database. However, it is unlikely that the database itself has been damaged. Restart SAP ASE and run dbcc diagnostics. The user must reconnect to SAP ASE.
                 * 
                 * Level 22: SAP ASE Fatal Error: Table Integrity Suspect
                 *           Error messages with severity level 22 indicate that the table or index specified in the message has been previously damaged by a software or hardware problem.
                 * 
                 * Level 23: Fatal Error: Database Integrity Suspect
                 *           Error messages with severity level 23 indicate that the integrity of the entire database is suspect due to previous damage caused by a software or hardware problem. Restart SAP ASE and run dbcc diagnostics.
                 * 
                 * Level 24: Hardware Error or System Table Corruption
                 *           Error messages with severity level 24 reflect a media failure or (in rare cases) the corruption of sysusages. The system administrator may have to reload the database. You may need to call your hardware vendor.
                 * 
                 * Level 25: SAP ASE Internal Error
                 *           Users do not see level 25 errors, as this level is used only for SAP ASE internal errors.
                 * 
                 * Level 26: Rule Error
                 *           Error messages with severity level 26 reflect that an internal locking or synchronization rule has been broken. You must shut down and restart SAP ASE.
                 *           
				 * severity 99 is used for stacktraces etc... at least in MDA Table monErrorlog
				 */

				int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_Severity, DEFAULT_alarm_Severity);

				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest("+getName()+"): threshold="+threshold+", severity='"+severity+"'.");

				if (severity > threshold && severity < 99)
				{
					// Initilize the errorlist if not done earlier
					if (errorNumberSkipList == null)
					{
						String errorListStr = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_ErrorNumberSkipList, DEFAULT_alarm_ErrorNumberSkipList);
						errorNumberSkipList = StringUtil.parseCommaStrToList(errorListStr);
					}
					if (errorNumberSkipList.contains( Integer.toString(errorNumber)) )
					{
						// Skipping this error number
						if (_logger.isDebugEnabled())
							_logger.debug("ErrorNumber "+errorNumber+" is part of the 'error-number-skip-list', so it wont be raised. (num="+errorNumber+", severity="+severity+", text='"+ErrorMessage+"')");
					}
					else
					{
						String errorDesc     = AseErrorMessageDictionary.getInstance().getDescription(errorNumber);
						String errorDescTxt  = "\nError: "      + errorNumber + ", Description: "     + errorDesc + "\n";
						String errorDescHtml = "<br><b>Error: " + errorNumber + ", Description</b>: " + errorDesc + "<br>";

						String extendedDescText = getErrorRecordAsText(lastRefreshRows, r) + errorDescTxt;
						String extendedDescHtml = getErrorRecordAsHtml(lastRefreshRows, r) + errorDescHtml;

						AlarmEvent.Severity alarmSeverity = AlarmEvent.Severity.WARNING;
						if (severity > 17)
							alarmSeverity = AlarmEvent.Severity.ERROR;
						
						AlarmEvent ae = new AlarmEventErrorLogEntry(this, alarmSeverity, errorNumber, severity, ErrorMessage, threshold);
						ae.setExtendedDescription(extendedDescText, extendedDescHtml);

						alarmHandler.addAlarm( ae );
					}
				}
			}

			
			//-------------------------------------------------------
			// ConfigChanges
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("ConfigChanges"))
			{
				// 00:0002:00000:00036:2019/03/26 01:41:39.17 server  The configuration option 'auditing' has been changed by 'sa' from '0' to '1'.
				// ErrorNumber=???, Severity=??, ErrorMessage=
				if (ErrorMessage.indexOf("The configuration option '") >= 0 && ErrorMessage.indexOf("' has been changed by ") >= 0)
				{
					String configName = "unknown";
					String extendedDescText = ErrorMessage;
					String extendedDescHtml = ErrorMessage;

					// Get the configuration name
					int startPos = ErrorMessage.indexOf("'");
					if (startPos != -1)
					{
						startPos++;
						int endPos = ErrorMessage.indexOf("'", startPos);
						if (endPos != -1)
							configName = ErrorMessage.substring(startPos, endPos);
					}
					
					AlarmEvent ae = new AlarmEventConfigChanges(this, configName, ErrorMessage);
					ae.setExtendedDescription(extendedDescText, extendedDescHtml);
						
					alarmHandler.addAlarm( ae );
				}
			}


			//-------------------------------------------------------
			// ProcessInfected
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("ProcessInfected"))
			{
				// START ROW: 00:0004:00000:00308:2018/05/04 19:37:01.48 kernel  Current process (0x2bad014c) infected with signal 11 (SIGSEGV)
				//            ... collect anything in between, as the message ...
				//   END ROW:          >>>> 00308: <<<< when SPID is changing into a new SPID, if we can't find SPID: fallback on below "end of stack trace,"
				//   END ROW: 00:0004:00000:00308:2018/05/04 19:37:01.48 kernel  end of stack trace, spid 308, kpid 732758348, suid 1
				// ErrorNumber=???, Severity=??, ErrorMessage=
				if (ErrorMessage.indexOf("Current process (") >= 0 && ErrorMessage.indexOf(") infected with ") >= 0)
				{
					// Read messages until 'end of stack trace, ' and stuff it in the below StringBuilder
					StringBuilder sb = new StringBuilder();

					// get SPID and capture entries until we see a new SPID...
					// below is a "start" for that
					int col_SPID_pos = findColumn("SPID");

					// Get SPID for this record, so we can check/break when we see a new SPID
					Object o_firstSPID = col_SPID_pos == -1 ? null : row.get(col_SPID_pos);
					
					
					// Continue to read messages (but do not move the "original r", create a new "rr" to loop on
					for (int rr=r; rr<lastRefreshRows.size(); rr++)
					{
						List<Object> rrRow = lastRefreshRows.get(rr);

						String ee_ErrorMessage = rrRow.get(col_ErrorMessage_pos) + "";
						sb.append( ee_ErrorMessage ).append("\n");

						// look for SPID is changed, or: "end of stack trace, "
						// If 'SPID' is not found, lets fallback on "end of stack trace, ", or even at the very end, where we break after 300 rows
						Object o_SPID = col_SPID_pos == -1 ? null : rrRow.get(col_SPID_pos);
						if (o_SPID == null || !(o_SPID instanceof Integer) )
						{
							// STOP Looping when we find 'end of stack trace, '
							if (ee_ErrorMessage.startsWith("end of stack trace, "))
								break;
						}
						else
						{
							// STOP Looping when we find a new SPID
							if ( ! o_SPID.equals(o_firstSPID) )
								break;
						}
						
						// OK: something is probably wrong (we didn't find 'end of stack trace, ' or The same SPID just continues), break the loop after 300 rows...
						if (rr >= 300)
							break;
					}
					String fullErrorMessage = sb.toString();
						
					String extendedDescText = fullErrorMessage;
					String extendedDescHtml = "<pre>\n" + fullErrorMessage + "\n</pre>";

					AlarmEvent ae = new AlarmEventProcessInfected(this, fullErrorMessage);
					ae.setExtendedDescription(extendedDescText, extendedDescHtml);
						
					alarmHandler.addAlarm( ae );
				}
			}
		}
	}

	public static final String  PROPKEY_alarm_UserConnections       = CM_NAME + ".alarm.system.on.UserConnections";
	public static final boolean DEFAULT_alarm_UserConnections       = true;

	public static final String  PROPKEY_alarm_TransactionLogFull    = CM_NAME + ".alarm.system.on.TransactionLogFull";
	public static final boolean DEFAULT_alarm_TransactionLogFull    = true;

	public static final String  PROPKEY_alarm_Severity              = CM_NAME + ".alarm.system.if.Severity.gt";
	public static final int     DEFAULT_alarm_Severity              = 16;

	public static final String  PROPKEY_alarm_ErrorNumberSkipList   = CM_NAME + ".alarm.system.errorNumber.skip.list";
	public static final String  DEFAULT_alarm_ErrorNumberSkipList   = "";

	public static final String  PROPKEY_alarm_ConfigChanges         = CM_NAME + ".alarm.system.on.ConfigChanges";
	public static final boolean DEFAULT_alarm_ConfigChanges         = true;

	public static final String  PROPKEY_alarm_ProcessInfected       = CM_NAME + ".alarm.system.on.ProcessInfected";
	public static final boolean DEFAULT_alarm_ProcessInfected       = true;

	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;
		
		list.add(new CmSettingsHelper("UserConnections"        , isAlarmSwitch, PROPKEY_alarm_UserConnections    , Boolean.class, conf.getBooleanProperty(PROPKEY_alarm_UserConnections    , DEFAULT_alarm_UserConnections    ), DEFAULT_alarm_UserConnections    , "On Error 1601, send 'AlarmEventConfigResourceIsUsedUp'." ));
		list.add(new CmSettingsHelper("TransactionLogFull"     , isAlarmSwitch, PROPKEY_alarm_TransactionLogFull , Boolean.class, conf.getBooleanProperty(PROPKEY_alarm_TransactionLogFull , DEFAULT_alarm_TransactionLogFull ), DEFAULT_alarm_TransactionLogFull , "On Error 7413, send 'AlarmEventFullTranLog'." ));
		list.add(new CmSettingsHelper("ConfigChanges"          , isAlarmSwitch, PROPKEY_alarm_ConfigChanges      , Boolean.class, conf.getBooleanProperty(PROPKEY_alarm_ConfigChanges      , DEFAULT_alarm_ConfigChanges      ), DEFAULT_alarm_ConfigChanges      , "On error log message 'The configuration option '.*' has been changed', send 'AlarmEventConfigChanges'." ));
		list.add(new CmSettingsHelper("ProcessInfected"        , isAlarmSwitch, PROPKEY_alarm_ProcessInfected    , Boolean.class, conf.getBooleanProperty(PROPKEY_alarm_ProcessInfected    , DEFAULT_alarm_ProcessInfected    ), DEFAULT_alarm_ProcessInfected    , "On error log message 'Current process .* infected with signal', send 'AlarmEventProcessInfected'." ));

		list.add(new CmSettingsHelper("Severity"               , isAlarmSwitch, PROPKEY_alarm_Severity           , Integer.class, conf.getIntProperty    (PROPKEY_alarm_Severity           , DEFAULT_alarm_Severity           ), DEFAULT_alarm_Severity           , "If 'Severity' is greater than ## then send 'AlarmEventErrorLogEntry'." ));
		list.add(new CmSettingsHelper("SkipList ErrorNumber(s)"               , PROPKEY_alarm_ErrorNumberSkipList, String .class, conf.getProperty       (PROPKEY_alarm_ErrorNumberSkipList, DEFAULT_alarm_ErrorNumberSkipList), DEFAULT_alarm_ErrorNumberSkipList, "Skip errors number in this list, that is if Severity is above that rule. format(comma separated list of numbers): 123, 321, 231" ));

		return list;
	}
}



/*
 * 

FIXME: Info "alarm" on config changes
	look for: 
	"The configuration option '.*' has been changed by"
	00:0002:00000:00036:2019/03/26 01:41:39.17 server  The configuration option 'auditing' has been changed by 'sa' from '0' to '1'.


FIXME: alarm on Stacktace in ASE log
	look for START: 
		"Current process .* infected with signal"
		Current process (0x2bad014c) infected with signal 11 (SIGSEGV)
	look for END/UNTIL: 
		"end of stack trace, spid "
		end of stack trace, spid 308, kpid 732758348, suid 1
		
	check if we can use error number or "timestamp" to "group" the message... (instead of: start, end)


FIXME: SQL-Server errorlog
    sp_readerrorlog
    xp_readerrorlog -1, 1, NULL, NULL, '2019-01-14 00:00'




00:0004:00000:00308:2018/05/04 19:37:01.48 kernel  Current process (0x2bad014c) infected with signal 11 (SIGSEGV)
00:0004:00000:00308:2018/05/04 19:37:01.48 kernel  Address 0x0x0000000000f43be0 (query_text+0x1dd), siginfo (code, address) = (1, 0x0x0000000000000158)
00:0004:00000:00308:2018/05/04 19:37:01.48 kernel  **** Saved signal context (0x0x00002aaab78b0700): ****
00:0004:00000:00308:2018/05/04 19:37:01.48 kernel  uc_flags: 0x1, uc_link: 0x(nil)
00:0004:00000:00308:2018/05/04 19:37:01.48 kernel  uc_sigmask: 0x416000 0xb 0x1 0x158
00:0004:00000:00308:2018/05/04 19:37:01.48 kernel  uc_stack: ss_sp: 0x(nil), ss_size: 0x0, ss_flags: 0x2
00:0004:00000:00308:2018/05/04 19:37:01.48 kernel  General Registers (uc_mcontext.gregs):
00:0004:00000:00308:2018/05/04 19:37:01.48 kernel       PC : 0x0000000000f43be0 (query_text+0x1dd)
00:0004:00000:00308:2018/05/04 19:37:01.48 kernel          RAX : 0x00002aab46894800  RBX : (nil)
00:0004:00000:00308:2018/05/04 19:37:01.48 kernel          RCX : 0x6c6e6f2064610072  RDX : 0x0000000009090a79
00:0004:00000:00308:2018/05/04 19:37:01.48 kernel       RBP : 0x00002aaab78b4cf0  RSP : 0x00002aaab78b0cb0
00:0004:00000:00308:2018/05/04 19:37:01.48 kernel       R8  : 0x000000000000000c  R9  : 0x00002aab46894800
00:0004:00000:00308:2018/05/04 19:37:01.48 kernel       R10 : (nil)  R11 : 0x00007ffff5c71454
00:0004:00000:00308:2018/05/04 19:37:01.48 kernel       R12 : 0x00002aab45339e48  R13 : 0x00002aab0fe68318
00:0004:00000:00308:2018/05/04 19:37:01.48 kernel       R14 : 0x00002aaab78b5678  R15 : 0x00002aab46894800
00:0004:00000:00308:2018/05/04 19:37:01.48 kernel       RDI : 0x00002aab468948e4  RSI : 0x00002aab45339f70
00:0004:00000:00308:2018/05/04 19:37:01.48 kernel       RIP : 0x0000000000f43be0  CSGSFS : 0x0000000000000033
00:0004:00000:00308:2018/05/04 19:37:01.48 kernel       TRAPNO : 0x000000000000000e  ERR : 0x0000000000000004
00:0004:00000:00308:2018/05/04 19:37:01.48 kernel       EFL : 0x0000000000010246
00:0004:00000:00308:2018/05/04 19:37:01.48 kernel  **** end of signal context ****
00:0004:00000:00308:2018/05/04 19:37:01.48 kernel  ************************************
00:0004:00000:00308:2018/05/04 19:37:01.48 kernel  SQL causing error : exec sp_showplan 86, null, null, null, 'long'
00:0004:00000:00308:2018/05/04 19:37:01.48 kernel  Current statement number: 57 Current line number: 139
00:0004:00000:00308:2018/05/04 19:37:01.48 kernel  ************************************
00:0004:00000:00308:2018/05/04 19:37:01.48 server  SQL Text: exec sp_showplan 86, null, null, null, 'long'
00:0004:00000:00308:2018/05/04 19:37:01.48 server  SQL Text: select @a = query_text(@spid)

00:0004:00000:00308:2018/05/04 19:37:01.48 kernel  curdb = 1 tempdb = 2 pstat = 0x10000 p2stat = 0x101000
00:0004:00000:00308:2018/05/04 19:37:01.48 kernel  p3stat = 0x800 p4stat = 0x0 p5stat = 0xc008 p6stat = 0x10000011 p7stat = 0x10000
00:0004:00000:00308:2018/05/04 19:37:01.48 kernel  lasterror = 0 preverror = 0 transtate = 1
00:0004:00000:00308:2018/05/04 19:37:01.48 kernel  curcmd = 193 program = AseTune-nogui
00:0004:00000:00308:2018/05/04 19:37:01.48 kernel  extended error information: hostname: dbxtune login: sa
00:0004:00000:00308:2018/05/04 19:37:01.48 kernel  pc: 0x00000000012aee1c pcstkwalk+0x46e()
00:0004:00000:00308:2018/05/04 19:37:01.48 kernel  pc: 0x00000000012ae7ef ucstkgentrace+0x20f()
00:0004:00000:00308:2018/05/04 19:37:01.48 kernel  pc: 0x00000000012ab99e ucbacktrace+0x50()
00:0004:00000:00308:2018/05/04 19:37:01.48 kernel  pc: 0x000000000155496c terminate_process+0xf4c()
00:0004:00000:00308:2018/05/04 19:37:01.48 kernel  pc: 0x00000000012dfe54 kisignal+0x31b()
00:0004:00000:00308:2018/05/04 19:37:01.48 kernel  pc: 0x0000000000f43be0 query_text+0x1dd()
00:0004:00000:00308:2018/05/04 19:37:01.48 kernel  pc: 0x00000000015acba2 LeRun+0x8562()
00:0004:00000:00308:2018/05/04 19:37:01.48 kernel  pc: 0x00000000015a16fd LeEvals::LeEvRun(LeRunMode, LeRunStack*, short, LeDataRow*, unsigned char*, int*)+0x4d()
00:0004:00000:00308:2018/05/04 19:37:01.48 kernel  pc: 0x0000000001dffe3e LeScalarOp::_LeOpNext(ExeCtxt&)+0x4e()
00:0004:00000:00308:2018/05/04 19:37:01.48 kernel  pc: 0x0000000001ded8cf LeEmitNoSndOp::_LeOpNext(ExeCtxt&)+0x1af()
00:0004:00000:00308:2018/05/04 19:37:01.48 kernel  pc: 0x0000000001586603 LePlanNext+0x7d3()
00:0004:00000:00308:2018/05/04 19:37:01.48 kernel  [Handler pc: 0x0x0000000001b5aaa0 le_execerr installed by the following function:-]
00:0004:00000:00308:2018/05/04 19:37:01.48 kernel  pc: 0x0000000001b5bd86 exec_lava+0x506()
00:0004:00000:00308:2018/05/04 19:37:01.48 kernel  pc: 0x0000000001be19a5 s_execute+0xfb5()
00:0004:00000:00308:2018/05/04 19:37:01.48 kernel  [Handler pc: 0x0x0000000001c362a0 hdl_stack installed by the following function:-]
00:0004:00000:00308:2018/05/04 19:37:01.48 kernel  [Handler pc: 0x0x0000000001c059d0 s_handle installed by the following function:-]
00:0004:00000:00308:2018/05/04 19:37:01.48 kernel  pc: 0x0000000001c09621 sequencer+0xcb1()
00:0004:00000:00308:2018/05/04 19:37:01.48 kernel  pc: 0x0000000001bf44dc execproc+0x76c()
00:0004:00000:00308:2018/05/04 19:37:01.48 kernel  pc: 0x0000000001be42ca s_execute+0x38da()
00:0004:00000:00308:2018/05/04 19:37:01.48 kernel  [Handler pc: 0x0x0000000001c362a0 hdl_stack installed by the following function:-]
00:0004:00000:00308:2018/05/04 19:37:01.48 kernel  [Handler pc: 0x0x0000000001c059d0 s_handle installed by the following function:-]
00:0004:00000:00308:2018/05/04 19:37:01.48 kernel  pc: 0x0000000001c09621 sequencer+0xcb1()
00:0004:00000:00308:2018/05/04 19:37:01.48 kernel  pc: 0x000000000156d1f9 tdsrecv_language+0x1d9()
00:0004:00000:00308:2018/05/04 19:37:01.48 kernel  [Handler pc: 0x0x0000000001d13540 ut_handle installed by the following function:-]
00:0004:00000:00308:2018/05/04 19:37:01.48 kernel  pc: 0x000000000157c788 conn_hdlr+0xef8()
00:0004:00000:00308:2018/05/04 19:37:01.48 kernel  end of stack trace, spid 308, kpid 732758348, suid 1




---------------------------------------
-- Below is stacktrace that has 2 'end of stack trace' with 'FATAL UNHANDLED EXCEPTION: signal 0 hit while handling a previously hit signal' in between
-- maybe loop until we find a new SPID here instead 
---------------------------------------

00:0004:00000:00293:2020/01/27 12:18:54.33 kernel  Cannot read, host process disconnected: daniel-ingren 37876 spid: 293
00:0005:00000:01279:2020/01/27 12:20:05.16 kernel  Current process (0x72ff02a0) infected with signal 11 (SIGSEGV)
00:0005:00000:01279:2020/01/27 12:20:05.16 kernel  Current Process is running on Engine 3
00:0005:00000:01279:2020/01/27 12:20:05.16 kernel  server is using elf symbols for stack decoding (125189 symbols found)
00:0005:00000:01279:2020/01/27 12:20:05.16 kernel  Address 0x0x0000001100000000 (), siginfo (code, address) = (1, 0x0x0000001100000000)
00:0005:00000:01279:2020/01/27 12:20:05.16 kernel  **** Saved signal context (0x0x00002aaac0b1c900): ****
00:0005:00000:01279:2020/01/27 12:20:05.16 kernel  uc_flags: 0x1, uc_link: 0x(nil)
00:0005:00000:01279:2020/01/27 12:20:05.16 kernel  uc_sigmask: 0x7bfbf037 0xb 0x1 0x0
00:0005:00000:01279:2020/01/27 12:20:05.16 kernel  uc_stack: ss_sp: 0x(nil), ss_size: 0x0, ss_flags: 0x2
00:0005:00000:01279:2020/01/27 12:20:05.16 kernel  General Registers (uc_mcontext.gregs):
00:0005:00000:01279:2020/01/27 12:20:05.16 kernel       PC : 0x0000001100000000 ()
00:0005:00000:01279:2020/01/27 12:20:05.16 kernel          RAX : 0x0000001100000000  RBX : (nil)
00:0005:00000:01279:2020/01/27 12:20:05.16 kernel          RCX : (nil)  RDX : 0x0000000000000001
00:0005:00000:01279:2020/01/27 12:20:05.16 kernel       RBP : 0x00002aaac0b1d650  RSP : 0x00002aaac0b1d5d8
00:0005:00000:01279:2020/01/27 12:20:05.16 kernel       R8  : 0x00002aab5318caf8  R9  : 0x00000000ffffffff
00:0005:00000:01279:2020/01/27 12:20:05.16 kernel       R10 : 0x00002aab0fcdb368  R11 : (nil)
00:0005:00000:01279:2020/01/27 12:20:05.16 kernel       R12 : 0x00002aab40be17b0  R13 : 0x00002aab5318bba8
00:0005:00000:01279:2020/01/27 12:20:05.16 kernel       R14 : 0x00002aab5318dd00  R15 : 0x00002aab5318dbd8
00:0005:00000:01279:2020/01/27 12:20:05.16 kernel       RDI : 0x00002aab40be17b0  RSI : (nil)
00:0005:00000:01279:2020/01/27 12:20:05.16 kernel       RIP : 0x0000001100000000  CSGSFS : 0x0000000000000033
00:0005:00000:01279:2020/01/27 12:20:05.16 kernel       TRAPNO : 0x000000000000000e  ERR : 0x0000000000000014
00:0005:00000:01279:2020/01/27 12:20:05.16 kernel       EFL : 0x0000000000010206
00:0005:00000:01279:2020/01/27 12:20:05.16 kernel  **** end of signal context ****
00:0005:00000:01279:2020/01/27 12:20:05.16 kernel  ************************************
00:0005:00000:01279:2020/01/27 12:20:05.16 kernel  SQL causing error : update brev
  set brev.xanstal = bf.xid_anst_ratt
from brev_fel bf
where bf.xid = brev.xid
  and bf.xid_anst_fel = brev.xanstal

00:0005:00000:01279:2020/01/27 12:20:05.16 kernel  Current statement number: 1 Current line number: 1
00:0005:00000:01279:2020/01/27 12:20:05.16 kernel  ************************************
00:0005:00000:01279:2020/01/27 12:20:05.16 server  SQL Text: update brev
  set brev.xanstal = bf.xid_anst_ratt
from brev_fel bf
where bf.xid = brev.xid
  and bf.xid_anst_fel = brev.xanstal
00:0005:00000:01279:2020/01/27 12:20:05.16 server  SQL Text: update brev set brev.xanstal = bf.xid_anst_ratt from brev_fel bf where bf.xid = brev.xid and bf.xid_anst_fel = brev.xanstal
00:0005:00000:01279:2020/01/27 12:20:05.16 kernel  curdb = 4 tempdb = 2 pstat = 0x10100 p2stat = 0x101000
00:0005:00000:01279:2020/01/27 12:20:05.16 kernel  p3stat = 0x800 p4stat = 0x0 p5stat = 0x8 p6stat = 0x1 p7stat = 0x10000
00:0005:00000:01279:2020/01/27 12:20:05.16 kernel  lasterror = 0 preverror = 0 transtate = 0
00:0005:00000:01279:2020/01/27 12:20:05.16 kernel  curcmd = 197 program = SqlWindow
00:0005:00000:01279:2020/01/27 12:20:05.16 kernel  extended error information: hostname: gorans login: sa
00:0005:00000:01279:2020/01/27 12:20:05.16 kernel  pc: 0x0000000001534c10 pcstkwalk+0x482()
00:0005:00000:01279:2020/01/27 12:20:05.16 kernel  pc: 0x00000000015345cf ucstkgentrace+0x20f()
00:0005:00000:01279:2020/01/27 12:20:05.16 kernel  pc: 0x0000000001530e02 ucbacktrace+0x54()
00:0005:00000:01279:2020/01/27 12:20:05.16 kernel  pc: 0x00000000017fbd24 terminate_process+0xb14()
00:0005:00000:01279:2020/01/27 12:20:05.16 kernel  pc: 0x00000000015612a4 kisignal+0x868()
00:0005:00000:01279:2020/01/27 12:20:05.16 kernel  pc: 0x0000001100000000 ()
00:0005:00000:01279:2020/01/27 12:20:05.16 kernel  pc: 0x00000000022d79ad LeUpdateOp::_LeOpNext(ExeCtxt&)+0x14d()
00:0005:00000:01279:2020/01/27 12:20:05.16 kernel  pc: 0x00000000022b7e60 LeEmitNoSndOp::_LeOpNext(ExeCtxt&)+0x1c0()
00:0005:00000:01279:2020/01/27 12:20:05.16 kernel  pc: 0x000000000183bffd LePlanNext+0xed()
00:0005:00000:01279:2020/01/27 12:20:05.16 kernel  [Handler pc: 0x0x0000000001f91ed0 le_execerr installed by the following function:-]
00:0005:00000:01279:2020/01/27 12:20:05.16 kernel  pc: 0x0000000001f93334 exec_lava+0x4a4()
00:0005:00000:01279:2020/01/27 12:20:05.16 kernel  pc: 0x0000000002028383 s_execute+0x1a33()
00:0005:00000:01279:2020/01/27 12:20:05.16 kernel  [Handler pc: 0x0x000000000208e540 hdl_stack installed by the following function:-]
00:0005:00000:01279:2020/01/27 12:20:05.16 kernel  [Handler pc: 0x0x0000000002052ae0 s_handle installed by the following function:-]
00:0005:00000:01279:2020/01/27 12:20:05.16 kernel  pc: 0x0000000002057135 sequencer+0xd15()
00:0005:00000:01279:2020/01/27 12:20:05.16 kernel  pc: 0x000000000203cc66 execproc+0xcf6()
00:0005:00000:01279:2020/01/27 12:20:05.16 kernel  pc: 0x000000000202b02c s_execute+0x46dc()
00:0005:00000:01279:2020/01/27 12:20:05.16 kernel  [Handler pc: 0x0x000000000208e540 hdl_stack installed by the following function:-]
00:0005:00000:01279:2020/01/27 12:20:05.16 kernel  [Handler pc: 0x0x0000000002052ae0 s_handle installed by the following function:-]
00:0005:00000:01279:2020/01/27 12:20:05.16 kernel  pc: 0x0000000002057135 sequencer+0xd15()
00:0005:00000:01279:2020/01/27 12:20:05.16 kernel  pc: 0x000000000181aeaa tdsrecv_language+0x1ea()
00:0005:00000:01279:2020/01/27 12:20:05.16 kernel  [Handler pc: 0x0x00000000021a97c0 ut_handle installed by the following function:-]
00:0005:00000:01279:2020/01/27 12:20:05.16 kernel  pc: 0x0000000001831ea9 conn_hdlr+0x12f9()
00:0005:00000:01279:2020/01/27 12:20:05.16 kernel  end of stack trace, spid 1279, kpid 1929314976, suid 1
00:0005:00000:01279:2020/01/27 12:20:05.18 kernel  FATAL UNHANDLED EXCEPTION: signal 0 hit while handling a previously hit signal. The server cannot continue and will shut down.
00:0005:00000:01279:2020/01/27 12:20:05.18 kernel  pc: 0x0000000001534c10 pcstkwalk+0x482()
00:0005:00000:01279:2020/01/27 12:20:05.18 kernel  pc: 0x00000000015345cf ucstkgentrace+0x20f()
00:0005:00000:01279:2020/01/27 12:20:05.18 kernel  pc: 0x0000000001530e02 ucbacktrace+0x54()
00:0005:00000:01279:2020/01/27 12:20:05.18 kernel  pc: 0x00000000021fdd75 kepanic+0xe5()
00:0005:00000:01279:2020/01/27 12:20:05.18 kernel  pc: 0x0000000001561607 kisignal+0xbcb()
00:0005:00000:01279:2020/01/27 12:20:05.18 kernel  pc: 0x0000001100000000 ()
00:0005:00000:01279:2020/01/27 12:20:05.18 kernel  pc: 0x00000000022d79ad LeUpdateOp::_LeOpNext(ExeCtxt&)+0x14d()
00:0005:00000:01279:2020/01/27 12:20:05.18 kernel  pc: 0x00000000022b7e60 LeEmitNoSndOp::_LeOpNext(ExeCtxt&)+0x1c0()
00:0005:00000:01279:2020/01/27 12:20:05.18 kernel  pc: 0x000000000183bffd LePlanNext+0xed()
00:0005:00000:01279:2020/01/27 12:20:05.18 kernel  [Handler pc: 0x0x0000000000919347 cleanerr installed by the following function:-]
00:0005:00000:01279:2020/01/27 12:20:05.18 kernel  [Handler pc: 0x0x000000000208e520 hdl_backout installed by the following function:-]
00:0005:00000:01279:2020/01/27 12:20:05.18 kernel  pc: 0x0000000001f93334 exec_lava+0x4a4()
00:0005:00000:01279:2020/01/27 12:20:05.18 kernel  pc: 0x00000000017fb83b terminate_process+0x62b()
00:0005:00000:01279:2020/01/27 12:20:05.18 kernel  [Handler pc: 0x0x0000000001f91ed0 le_execerr installed by the following function:-]
00:0005:00000:01279:2020/01/27 12:20:05.18 kernel  pc: 0x0000000002057135 sequencer+0xd15()
00:0005:00000:01279:2020/01/27 12:20:05.18 kernel  pc: 0x0000000002028383 s_execute+0x1a33()
00:0005:00000:01279:2020/01/27 12:20:05.18 kernel  pc: 0x000000000202b02c s_execute+0x46dc()
00:0005:00000:01279:2020/01/27 12:20:05.18 kernel  [Handler pc: 0x0x000000000208e540 hdl_stack installed by the following function:-]
00:0005:00000:01279:2020/01/27 12:20:05.18 kernel  [Handler pc: 0x0x0000000002052ae0 s_handle installed by the following function:-]
00:0005:00000:01279:2020/01/27 12:20:05.18 kernel  pc: 0x0000000002057135 sequencer+0xd15()
00:0005:00000:01279:2020/01/27 12:20:05.18 kernel  pc: 0x000000000203cc66 execproc+0xcf6()
00:0005:00000:01279:2020/01/27 12:20:05.18 kernel  [Handler pc: 0x0x000000000208e540 hdl_stack installed by the following function:-]
00:0005:00000:01279:2020/01/27 12:20:05.18 kernel  [Handler pc: 0x0x0000000002052ae0 s_handle installed by the following function:-]
00:0005:00000:01279:2020/01/27 12:20:05.18 kernel  pc: 0x0000000001831ea9 conn_hdlr+0x12f9()
00:0005:00000:01279:2020/01/27 12:20:05.18 kernel  end of stack trace, spid 1279, kpid 1929314976, suid 1
00:0005:00000:01279:2020/01/27 12:20:05.18 kernel  ueshutdown: exiting
00:0000:00000:00000:2020/01/27 12:20:05.18 kernel  Main thread performing final shutdown.




*/