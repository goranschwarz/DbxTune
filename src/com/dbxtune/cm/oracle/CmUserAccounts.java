/*******************************************************************************
 * Copyright (C) 2010-2027 Goran Schwarz
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
package com.dbxtune.cm.oracle;

import java.lang.invoke.MethodHandles;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.alarm.AlarmHandler;
import com.dbxtune.alarm.events.AlarmDescriptionProviderBase;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.alarm.events.oracle.AlarmEventOraAccountProblem;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CmSettingsHelper.RegExpInputValidator;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.config.dict.MonTablesDictionary;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.sql.conn.info.DbmsVersionInfoOracle;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmUserAccounts
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmUserAccounts.class.getSimpleName();
	public static final String   SHORT_NAME       = "Accounts";
	public static final String   HTML_DESC        =
		"<html>" +
		"The database accounts: are they open or locked, and when does the password expire.<br>" +
		"<br>" +
		"A password expires when the profile of the account has a <code>PASSWORD_LIFE_TIME</code> (the DEFAULT profile has 180 days in many versions).<br>" +
		"When the password of an application account expires, the application can't log in any more. That is a classic way to get an outage, half a year after go-live.<br>" +
		"<code>EXPIRES_IN_DAYS</code> shows how long it is left (negative: it has already expired, and the account is switched to EXPIRED at the next login).<br>" +
		"<br>" +
		"<code>ACCOUNT_STATUS</code> <b>LOCKED(TIMED)</b> means that the account was locked by too many failed logins (<code>FAILED_LOGIN_ATTEMPTS</code> in the profile).<br>" +
		"<br>" +
		"<b>Note</b>: Accounts that came with Oracle are normally EXPIRED &amp; LOCKED. That is as it should be, and gives no alarm.<br>" +
		"<b>Note</b>: In a Container Database, connect to CDB$ROOT to see the accounts of all PDBs. A common user (C##...) is then shown once for every container.<br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {CM_NAME, "DBA_USERS", "DBA_PROFILES"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 3600;
	public static final int      DEFAULT_QUERY_TIMEOUT          = 30;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.SMALL; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '" + CM_NAME + "'");

		return new CmUserAccounts(counterController, guiController);
	}

	public CmUserAccounts(ICounterController counterController, IGuiController guiController)
	{
		super(counterController, guiController,
				CM_NAME, GROUP_NAME, /*sql*/null, /*pkList*/null,
				DIFF_COLUMNS, PCT_COLUMNS, MON_TABLES,
				NEED_ROLES, NEED_CONFIG, NEED_SRV_VERSION, NEED_CE_VERSION,
				NEGATIVE_DIFF_COUNTERS_TO_ZERO, IS_SYSTEM_CM, DEFAULT_POSTPONE_TIME);

		setDisplayName(SHORT_NAME);
		setDescription(HTML_DESC);

		setIconFile(GUI_ICON_FILE);

		setShowClearTime(false);

		setCounterController(counterController);
		setGuiController(guiController);

		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		// CON_ID is always 0 in a non-CDB (and in 11.2), in a CDB the same user name can exist in many containers
		pkCols.add("CON_ID");
		pkCols.add("USERNAME");

		return pkCols;
	}

	/** If we can't read the views: disable the CM, and tell what to do about it */
	@Override
	public boolean checkDependsOnOther(DbxConnection conn)
	{
		return OracleCmUtils.checkViewsAreReadable(this, conn, "DBA_USERS", "DBA_PROFILES");
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		DbmsVersionInfoOracle oraVersionInfo = (DbmsVersionInfoOracle) versionInfo;

		// 11.2 and above: no LAST_LOGIN, ORACLE_MAINTAINED, COMMON
		String sql11 = getSql("CAST(0 as NUMBER(10))", "CAST(NULL as VARCHAR2(128))", "DBA_USERS", "DBA_PROFILES", "", "",
				"CAST(NULL as DATE)", "CAST(NULL as VARCHAR2(1))", "CAST(NULL as VARCHAR2(3))", "u.USERNAME");

		// 12.1 and above, a non-CDB or connected to a PDB
		String sql12 = getSql("CAST(0 as NUMBER(10))", "CAST(NULL as VARCHAR2(128))", "DBA_USERS", "DBA_PROFILES", "", "",
				"CAST(u.LAST_LOGIN as DATE)", "u.ORACLE_MAINTAINED", "u.COMMON", "u.USERNAME");

		// 12.1 and above, connected to CDB$ROOT: the accounts in EVERY container
		String sqlCdb = getSql("u.CON_ID", "c.NAME", "CDB_USERS", "CDB_PROFILES", "left join V$CONTAINERS c on c.CON_ID = u.CON_ID \n", " and p.CON_ID = u.CON_ID",
				"CAST(u.LAST_LOGIN as DATE)", "u.ORACLE_MAINTAINED", "u.COMMON", "u.CON_ID, u.USERNAME");

		// The SQL's for 12.1 and above can NOT be tested on all versions: so "dry-run" them (the one that gives most first), and if they fail use the 11.2 SQL (which works on all versions)
		List<String> candidates = new ArrayList<>();
		if (versionInfo.getLongVersion() >= Ver.ver(12,1) && oraVersionInfo.isCdbRoot()) candidates.add(sqlCdb);
		if (versionInfo.getLongVersion() >= Ver.ver(12,1))                               candidates.add(sql12);

		// conn is null when we just want to "view" the SQL (the CM Properties dialog)
		if (conn == null)
			return candidates.isEmpty() ? sql11 : candidates.get(0);

		for (String candidate : candidates)
		{
			try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery("select * from (" + candidate + ") where 1=2"))
			{
				return candidate;
			}
			catch (SQLException ex)
			{
				_logger.warn(CM_NAME + ": A SQL for version " + Ver.versionNumToStr(versionInfo.getLongVersion()) + " failed, trying a simpler one. Caught: " + ex.getMessage().trim());
			}
		}

		return sql11;
	}

	private static String getSql(String CON_ID, String CON_NAME, String usersView, String profilesView, String joinContainers, String profileJoinContainer, String LAST_LOGIN, String ORACLE_MAINTAINED, String COMMON, String orderBy)
	{
		// One row for every account
		// PASSWORD_LIFE_TIME and FAILED_LOGIN_ATTEMPTS is from the profile of the account. If the profile says DEFAULT: the value of the profile named DEFAULT
		// EXPIRES_IN_DAYS is calculated with the DBMS clock (SYSDATE). NULL = the password does not expire
		// NOTE: the column PASSWORD in DBA_USERS is never read
		// Note: CAST(... as NUMBER(10,1)) is needed to keep decimals (a NUMBER without precision is treated as a BIGINT)
		String limit = "(select max(decode(p.LIMIT, 'DEFAULT', (select max(d.LIMIT) from " + profilesView + " d where d.PROFILE = 'DEFAULT' and d.RESOURCE_NAME = p.RESOURCE_NAME" + profileJoinContainer.replace("p.CON_ID", "d.CON_ID") + "), p.LIMIT)) "
		             + "from " + profilesView + " p where p.PROFILE = u.PROFILE and p.RESOURCE_NAME = '%s'" + profileJoinContainer + ")";

		return ""
			+ "select \n"
			+ "     " + CON_ID   + " as CON_ID \n"
			+ "    ," + CON_NAME + " as CON_NAME \n"
			+ "    ,u.USERNAME \n"
			+ "    ,u.ACCOUNT_STATUS \n"
			+ "    ,u.LOCK_DATE \n"
			+ "    ,u.EXPIRY_DATE \n"
			+ "    ,CAST(u.EXPIRY_DATE - SYSDATE as NUMBER(10,1)) as EXPIRES_IN_DAYS \n"
			+ "    ,u.PROFILE \n"
			+ "    ,CAST(" + String.format(limit, "PASSWORD_LIFE_TIME")    + " as VARCHAR2(40)) as PASSWORD_LIFE_TIME \n"
			+ "    ,CAST(" + String.format(limit, "FAILED_LOGIN_ATTEMPTS") + " as VARCHAR2(40)) as FAILED_LOGIN_ATTEMPTS \n"
			+ "    ," + LAST_LOGIN        + " as LAST_LOGIN \n"
			+ "    ,u.CREATED \n"
			+ "    ,u.DEFAULT_TABLESPACE \n"
			+ "    ,u.AUTHENTICATION_TYPE \n"
			+ "    ," + ORACLE_MAINTAINED + " as ORACLE_MAINTAINED \n"
			+ "    ," + COMMON            + " as COMMON \n"
			+ "from " + usersView + " u \n"
			+ joinContainers
			+ "order by " + orderBy + " \n"
			+ "";
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			mtd.addTable(CM_NAME, "The database accounts: are they open or locked, and when does the password expire.");

			mtd.addColumn(CM_NAME, "CON_ID",                "<html>Container ID. 0 = This is not a Container Database (or the version is below 12.1), or we are connected to a PDB.</html>");
			mtd.addColumn(CM_NAME, "CON_NAME",              "<html>Name of the Container (PDB) the account is in. NULL if CON_ID is 0.</html>");
			mtd.addColumn(CM_NAME, "ACCOUNT_STATUS",        "<html>State of the account."
			                                                    + "<ul>"
			                                                    + "<li><b>OPEN</b>             - it can be used.</li>"
			                                                    + "<li><b>EXPIRED(GRACE)</b>   - the password has expired, it still works but gives a warning at login (for PASSWORD_GRACE_TIME days).</li>"
			                                                    + "<li><b>EXPIRED</b>          - the password has expired, it must be changed at the next login (an application can't do that).</li>"
			                                                    + "<li><b>LOCKED</b>           - locked by a DBA.</li>"
			                                                    + "<li><b>LOCKED(TIMED)</b>    - locked by too many failed logins. It unlocks by itself after PASSWORD_LOCK_TIME days (if that is set).</li>"
			                                                    + "<li><b>EXPIRED &amp; LOCKED</b> - both. Normal for the accounts that came with Oracle.</li>"
			                                                    + "</ul></html>");
			mtd.addColumn(CM_NAME, "LOCK_DATE",             "<html>When the account was locked.</html>");
			mtd.addColumn(CM_NAME, "EXPIRY_DATE",           "<html>When the password expires (or expired). NULL = it does not expire.</html>");
			mtd.addColumn(CM_NAME, "EXPIRES_IN_DAYS",       "<html>Days until the password expires. Negative = it has already expired. NULL = it does not expire.<br><b>Formula</b>: EXPIRY_DATE - SYSDATE</html>");
			mtd.addColumn(CM_NAME, "PROFILE",               "<html>The profile of the account: where the password rules are set.</html>");
			mtd.addColumn(CM_NAME, "PASSWORD_LIFE_TIME",    "<html>How many days a password can be used (from the profile). UNLIMITED = it never expires.</html>");
			mtd.addColumn(CM_NAME, "FAILED_LOGIN_ATTEMPTS", "<html>How many failed logins in a row that locks the account (from the profile). UNLIMITED = it is never locked.</html>");
			mtd.addColumn(CM_NAME, "LAST_LOGIN",            "<html>When the account last logged in (12.1 and above, in the time of the DBMS host). NULL = never, or not known.<br><b>Note</b>: logins as SYSDBA are not recorded here.</html>");
			mtd.addColumn(CM_NAME, "AUTHENTICATION_TYPE",   "<html>How the account logs in: PASSWORD, EXTERNAL (operating system or Kerberos), GLOBAL (directory), NONE (a schema that can't log in).</html>");
			mtd.addColumn(CM_NAME, "ORACLE_MAINTAINED",     "<html>Y = the account came with Oracle (12.1 and above).</html>");
			mtd.addColumn(CM_NAME, "COMMON",                "<html>YES = a common user (exists in all containers, like C##... and SYSTEM), NO = a local user in one PDB (12.1 and above).</html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e);
		}
	}

	/** In a CDB: 'containerName:username', otherwise just the user name. Used to identify an alarm */
	private static String getAccountLabel(CountersModel cm, int rowId)
	{
		String conName  = cm.getAbsString(rowId, "CON_NAME");
		String username = cm.getAbsString(rowId, "USERNAME");

		return StringUtil.hasValue(conName) ? conName + ":" + username : username;
	}

	@Override
	public void sendAlarmRequest()
	{
		if ( ! hasAbsData() )
			return;

		if ( ! AlarmHandler.hasInstance() )
			return;

		boolean debugPrint = Configuration.getCombinedConfiguration().getBooleanProperty("sendAlarmRequest.debug", _logger.isDebugEnabled());

		CountersModel cm   = this;
		Configuration conf = Configuration.getCombinedConfiguration();

		String skipUsernameRegex = conf.getProperty(PROPKEY_alarm_SkipUsernameRegex, DEFAULT_alarm_SkipUsernameRegex);

		for (int r=0; r<cm.getAbsRowCount(); r++)
		{
			String username      = cm.getAbsString(r, "USERNAME");
			String accountStatus = cm.getAbsString(r, "ACCOUNT_STATUS");
			String account       = getAccountLabel(cm, r);

			if (username == null || accountStatus == null)
				continue;

			// Accounts we do not care about
			if (StringUtil.hasValue(skipUsernameRegex) && username.matches(skipUsernameRegex))
				continue;

			// A common user (C##..., SYSTEM...) is shown once for every container: it's one account, so only look at it in CDB$ROOT (CON_ID 1)
			if ("YES".equals(cm.getAbsString(r, "COMMON")) && cm.getAbsValueAsInteger(r, "CON_ID", 0) > 1)
				continue;

			//-------------------------------------------------------
			// PasswordExpiresInDays: only accounts that are in use: not the ones a DBA has locked (LOCKED)
			// LOCKED(TIMED) is temporary (too many failed logins), so that account is still "in use"
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("PasswordExpiresInDays"))
			{
				int     threshold     = conf.getIntProperty(PROPKEY_alarm_PasswordExpiresInDays, DEFAULT_alarm_PasswordExpiresInDays);
				Double  expiresInDays = cm.getAbsValueAsDouble(r, "EXPIRES_IN_DAYS");
				boolean lockedByDba   = accountStatus.replace("LOCKED(TIMED)", "").contains("LOCKED");

				if (expiresInDays != null && !lockedByDba)
				{
					if (debugPrint || _logger.isDebugEnabled())
						System.out.println("##### sendAlarmRequest(" + cm.getName() + "): PasswordExpiresInDays: account='" + account + "', threshold=" + threshold + ", EXPIRES_IN_DAYS='" + expiresInDays + "', ACCOUNT_STATUS='" + accountStatus + "'.");

					if (expiresInDays < threshold)
					{
						AlarmEvent ae = new AlarmEventOraAccountProblem(cm, account, accountStatus, cm.getAbsString(r, "EXPIRY_DATE"), expiresInDays, cm.getAbsString(r, "PROFILE"), threshold);

						// The below is called from AlarmHandler on: Raise, RE-RAISE & CANCEL
						ae.setAlarmDescriptionProvider(this, new AlarmDescriptionProviderBase(cm.getPk(), cm.getAbsPkValue(r), threshold)
						{
							@Override
							public void setValues(CountersModel cm, AlarmEvent alarmEvent, AlarmPhase phase)
							{
								int    rowId = cm.getAbsRowIdForPkValue(getRowPk());
								String label = "Account '" + account + "'";

								if (rowId != -1)
								{
									// Always: (RAISE, RE-RAISE, CANCEL) - Set the row as a table
									String extendedDescText = cm.toTextTableString(DATA_ABS, rowId);
									String extendedDescHtml = cm.toHtmlTableString(DATA_ABS, rowId, true, false, false);
									alarmEvent.setExtendedDescription(extendedDescText, extendedDescHtml);

									// Set CANCEL message (values that was found AFTER last raise/re-raise event)
									if (AlarmPhase.CANCEL.equals(phase))
									{
										Double EXPIRES_IN_DAYS = cm.getAbsValueAsDouble(rowId, "EXPIRES_IN_DAYS");
										String ACCOUNT_STATUS  = cm.getAbsString       (rowId, "ACCOUNT_STATUS");

										String cancelMsg = (EXPIRES_IN_DAYS == null)
											? label + ": the password does not expire any more (status " + ACCOUNT_STATUS + ")"
											: getAlarmCancelText(label + ": days until the password expires", toAlarmCancelValue(EXPIRES_IN_DAYS) + ", status is '" + ACCOUNT_STATUS + "'", " days");
										alarmEvent.setCancelDescription(cancelMsg);
									}
								}
								else
								{
									if (AlarmPhase.CANCEL.equals(phase))
										alarmEvent.setCancelDescription(getAlarmCancelTextGone(label));
								}
							}
						});

						// Information about how to disable this alarm
						ae.createAlarmOptionsMessage(this, "PasswordExpiresInDays");

						AlarmHandler.getInstance().addAlarm(ae);
					}
				}
			} // end: PasswordExpiresInDays

			//-------------------------------------------------------
			// AccountLockedTimed: locked by too many failed logins
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("AccountLockedTimed") && conf.getBooleanProperty(PROPKEY_alarm_AccountLockedTimed, DEFAULT_alarm_AccountLockedTimed))
			{
				if (accountStatus.contains("LOCKED(TIMED)"))
				{
					if (debugPrint || _logger.isDebugEnabled())
						System.out.println("##### sendAlarmRequest(" + cm.getName() + "): AccountLockedTimed: account='" + account + "', ACCOUNT_STATUS='" + accountStatus + "'.");

					AlarmEvent ae = new AlarmEventOraAccountProblem(cm, account, accountStatus, cm.getAbsString(r, "LOCK_DATE"), cm.getAbsString(r, "PROFILE"));

					// The below is called from AlarmHandler on: Raise, RE-RAISE & CANCEL
					ae.setAlarmDescriptionProvider(this, new AlarmDescriptionProviderBase(cm.getPk(), cm.getAbsPkValue(r), null)
					{
						@Override
						public void setValues(CountersModel cm, AlarmEvent alarmEvent, AlarmPhase phase)
						{
							int    rowId = cm.getAbsRowIdForPkValue(getRowPk());
							String label = "Account '" + account + "'";

							if (rowId != -1)
							{
								// Always: (RAISE, RE-RAISE, CANCEL) - Set the row as a table
								String extendedDescText = cm.toTextTableString(DATA_ABS, rowId);
								String extendedDescHtml = cm.toHtmlTableString(DATA_ABS, rowId, true, false, false);
								alarmEvent.setExtendedDescription(extendedDescText, extendedDescHtml);

								// Set CANCEL message (values that was found AFTER last raise/re-raise event)
								if (AlarmPhase.CANCEL.equals(phase))
									alarmEvent.setCancelDescription(getAlarmCancelText(label + ": ACCOUNT_STATUS", "'" + cm.getAbsString(rowId, "ACCOUNT_STATUS") + "'"));
							}
							else
							{
								if (AlarmPhase.CANCEL.equals(phase))
									alarmEvent.setCancelDescription(getAlarmCancelTextGone(label));
							}
						}
					});

					// Information about how to disable this alarm
					ae.createAlarmOptionsMessage(this, "AccountLockedTimed");

					AlarmHandler.getInstance().addAlarm(ae);
				}
			} // end: AccountLockedTimed
		} // end: loop-rows
	}

	public static final String  PROPKEY_alarm_PasswordExpiresInDays = CM_NAME + ".alarm.system.if.PasswordExpiresInDays.lt";
	public static final int     DEFAULT_alarm_PasswordExpiresInDays = 14;

	public static final String  PROPKEY_alarm_AccountLockedTimed    = CM_NAME + ".alarm.system.if.AccountStatus.is.LOCKED_TIMED";
	public static final boolean DEFAULT_alarm_AccountLockedTimed    = true;

	// SYS: it logs in through the password file (as SYSDBA), so it's expiry date in DBA_USERS is often long gone without that being a problem
	public static final String  PROPKEY_alarm_SkipUsernameRegex     = CM_NAME + ".alarm.system.skip.username";
	public static final String  DEFAULT_alarm_SkipUsernameRegex     = "(SYS)";

	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;

		list.add(new CmSettingsHelper("PasswordExpiresInDays", isAlarmSwitch, PROPKEY_alarm_PasswordExpiresInDays, Integer.class, conf.getIntProperty    (PROPKEY_alarm_PasswordExpiresInDays, DEFAULT_alarm_PasswordExpiresInDays), DEFAULT_alarm_PasswordExpiresInDays, "If 'EXPIRES_IN_DAYS' is less than ## days, for an account that a DBA has not locked (so not for LOCKED, but for OPEN, EXPIRED(GRACE), EXPIRED and LOCKED(TIMED)), then send 'AlarmEventOraAccountProblem'." ));
		list.add(new CmSettingsHelper("AccountLockedTimed"   , isAlarmSwitch, PROPKEY_alarm_AccountLockedTimed   , Boolean.class, conf.getBooleanProperty(PROPKEY_alarm_AccountLockedTimed   , DEFAULT_alarm_AccountLockedTimed   ), DEFAULT_alarm_AccountLockedTimed   , "If 'ACCOUNT_STATUS' has LOCKED(TIMED) (the account was locked by too many failed logins) then send 'AlarmEventOraAccountProblem'." ));
		list.add(new CmSettingsHelper("Skip Username"        ,                PROPKEY_alarm_SkipUsernameRegex    , String .class, conf.getProperty       (PROPKEY_alarm_SkipUsernameRegex    , DEFAULT_alarm_SkipUsernameRegex    ), DEFAULT_alarm_SkipUsernameRegex    , "For both alarms: skip accounts where 'USERNAME' matches this regexp. Example: (SYS|DBSNMP|APP_.*)", new RegExpInputValidator()));

		return list;
	}
}
