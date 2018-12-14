package com.asetune.cm.ase;

import java.sql.Connection;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.alarm.AlarmHandler;
import com.asetune.alarm.events.AlarmEvent;
import com.asetune.alarm.events.AlarmEventConfigResourceIsUsedUp;
import com.asetune.alarm.events.AlarmEventErrorLogEntry;
import com.asetune.alarm.events.AlarmEventFullTranLog;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.CountersModelAppend;
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

			Object o_errorNumber  = (Integer) row.get(col_ErrorNumber_pos);
			Object o_severity     = (Integer) row.get(col_Severity_pos);
			Object o_ErrorMessage = (String)  row.get(col_ErrorMessage_pos);

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
			// Long running transaction
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
				// ErrorNumber=7413, Severity=10, ErrorMessage=1 task(s) are sleeping waiting for space to become available in the log segment for database GAS_prod.
				if (errorNumber == 7413)
				{
					String extendedDescText = ErrorMessage;
					String extendedDescHtml = ErrorMessage;
						
					AlarmEvent ae = new AlarmEventFullTranLog(this, 0, "monErrorlog");
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
						String extendedDescText = getErrorRecordAsText(lastRefreshRows, r);
						String extendedDescHtml = getErrorRecordAsHtml(lastRefreshRows, r);

						AlarmEvent.Severity alarmSeverity = AlarmEvent.Severity.WARNING;
						if (severity > 17)
							alarmSeverity = AlarmEvent.Severity.ERROR;
						
						AlarmEvent ae = new AlarmEventErrorLogEntry(this, alarmSeverity, errorNumber, severity, ErrorMessage, threshold);
						ae.setExtendedDescription(extendedDescText, extendedDescHtml);

						alarmHandler.addAlarm( ae );
					}
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

	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		
		list.add(new CmSettingsHelper("UserConnections"        , PROPKEY_alarm_UserConnections    , Boolean.class, conf.getBooleanProperty(PROPKEY_alarm_UserConnections    , DEFAULT_alarm_UserConnections    ), DEFAULT_alarm_UserConnections    , "On Error 1601, send 'AlarmEvent FIXME'." ));
		list.add(new CmSettingsHelper("TransactionLogFull"     , PROPKEY_alarm_TransactionLogFull , Boolean.class, conf.getBooleanProperty(PROPKEY_alarm_TransactionLogFull , DEFAULT_alarm_TransactionLogFull ), DEFAULT_alarm_TransactionLogFull , "On Error 7413, send 'AlarmEventFullTranLog'." ));
		list.add(new CmSettingsHelper("Severity"               , PROPKEY_alarm_Severity           , Integer.class, conf.getIntProperty    (PROPKEY_alarm_Severity           , DEFAULT_alarm_Severity           ), DEFAULT_alarm_Severity           , "If 'Severity' is greater than ## then send 'AlarmEvent FIXME'." ));
		list.add(new CmSettingsHelper("SkipList ErrorNumber(s)", PROPKEY_alarm_ErrorNumberSkipList, String .class, conf.getProperty       (PROPKEY_alarm_ErrorNumberSkipList, DEFAULT_alarm_ErrorNumberSkipList), DEFAULT_alarm_ErrorNumberSkipList, "Skip errors number in this list, that is if Severity is above that rule. format(comma separated list of numbers): 123, 321, 231" ));

		return list;
	}
}

