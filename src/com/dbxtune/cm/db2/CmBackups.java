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
package com.dbxtune.cm.db2;

import java.lang.invoke.MethodHandles;
import java.sql.Timestamp;
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
import com.dbxtune.alarm.events.AlarmEventLastBackupFailed;
import com.dbxtune.alarm.events.AlarmEventOldBackup;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.config.dict.MonTablesDictionary;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.utils.Configuration;

/**
 * The backups and the log archiving of the database, from the recovery history file (SYSIBMADM.DB_HISTORY):
 * one row per kind of backup (online/offline, full/incremental/delta, database/table space) and per log archive method.
 *
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmBackups
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmBackups.class.getSimpleName();
	public static final String   SHORT_NAME       = "Backups";
	public static final String   HTML_DESC        =
		"<html>" +
		"<p>The backups and the log archiving of the database, from the recovery history file (SYSIBMADM.DB_HISTORY).</p>" +
		"<p>One row per kind of backup (online or offline; full, incremental or delta; database or table space), and one row per log archive method (LOGARCHMETH1/2): " +
		"the last one (when, how long, where, OK or not), the last good one, and how many there were in the last 24 hours and 7 days.</p>" +
		"<p>Only the log archives of the last 8 days are read (there can be very many). The history file is read every 10 minutes (it can be big).</p>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {CM_NAME, "DB_HISTORY"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = false;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 600;
	public static final int      DEFAULT_QUERY_TIMEOUT          = 60;

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

		return new CmBackups(counterController, guiController);
	}

	public CmBackups(ICounterController counterController, IGuiController guiController)
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

		pkCols.add("OPERATION");
		pkCols.add("OPERATIONTYPE");
		pkCols.add("OBJECTTYPE");
		pkCols.add("DBPARTITIONNUM");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		// NOTE: START_TIME and END_TIME are strings 'yyyymmddhhmmss' (END_TIME is empty while it runs). SQLCODE is NULL when it went OK
		// NOTE: OPERATION B = backup (OPERATIONTYPE F/N = offline/online full, I/O = offline/online incremental, D/E = offline/online delta; OBJECTTYPE D = database, P = table space)
		//                 X = log archive (OPERATIONTYPE 1/2 = archived by LOGARCHMETH1/2. The other types are the log paths, not archives)
		// NOTE: The last entry of each kind is joined in (RN = 1): an aggregate over a NULL (a conditional max) gives the warning SQLSTATE 01003 every sample
		String sql = ""
			+ "with h as ( \n"
			+ "    select OPERATION, OPERATIONTYPE, COALESCE(OBJECTTYPE, '') as OBJECTTYPE, DBPARTITIONNUM, SQLCODE, LOCATION, DEVICETYPE, \n"
			+ "           TIMESTAMP_FORMAT(START_TIME, 'YYYYMMDDHH24MISS') as START_TS, \n"
			+ "           CASE WHEN LENGTH(RTRIM(COALESCE(END_TIME, ''))) = 14 THEN TIMESTAMP_FORMAT(END_TIME, 'YYYYMMDDHH24MISS') END as END_TS, \n"
			+ "           ROW_NUMBER() OVER (PARTITION BY OPERATION, OPERATIONTYPE, OBJECTTYPE, DBPARTITIONNUM ORDER BY START_TIME desc, EID desc) as RN \n"
			+ "    from SYSIBMADM.DB_HISTORY \n"
			+ "    where OPERATION = 'B' \n"
			+ "       or (OPERATION = 'X' and OPERATIONTYPE in ('1', '2') and START_TIME >= VARCHAR_FORMAT(CURRENT TIMESTAMP - 8 DAYS, 'YYYYMMDDHH24MISS')) \n"
			+ "), a as ( \n"
			+ "    select OPERATION, OPERATIONTYPE, OBJECTTYPE, DBPARTITIONNUM \n"
			+ "          ,max(CASE WHEN COALESCE(SQLCODE, 0) >= 0 THEN COALESCE(END_TS, TIMESTAMP('0001-01-01-00.00.00')) ELSE TIMESTAMP('0001-01-01-00.00.00') END) as LAST_GOOD_TS \n"
			+ "          ,sum(CASE WHEN START_TS >= CURRENT TIMESTAMP - 24 HOURS THEN 1 ELSE 0 END)                                      as COUNT_24H \n"
			+ "          ,sum(CASE WHEN START_TS >= CURRENT TIMESTAMP - 24 HOURS AND COALESCE(SQLCODE, 0) < 0 THEN 1 ELSE 0 END)         as FAILED_24H \n"
			+ "          ,sum(CASE WHEN START_TS >= CURRENT TIMESTAMP - 7 DAYS THEN 1 ELSE 0 END)                                        as COUNT_7D \n"
			+ "          ,sum(CASE WHEN START_TS >= CURRENT TIMESTAMP - 7 DAYS AND COALESCE(SQLCODE, 0) < 0 THEN 1 ELSE 0 END)           as FAILED_7D \n"
			+ "    from h \n"
			+ "    group by OPERATION, OPERATIONTYPE, OBJECTTYPE, DBPARTITIONNUM \n"
			+ ") \n"
			+ "select /* ${cmCollectorName} */ \n"
			+ "     CAST(CURRENT SERVER AS VARCHAR(128))                         as DB_NAME \n"
			+ "    ,CAST(CASE WHEN a.OPERATION = 'X' THEN 'Log Archive, LOGARCHMETH' || a.OPERATIONTYPE \n"
			+ "               ELSE CASE a.OPERATIONTYPE WHEN 'F' THEN 'Offline Full' WHEN 'N' THEN 'Online Full' \n"
			+ "                                         WHEN 'I' THEN 'Offline Incremental' WHEN 'O' THEN 'Online Incremental' \n"
			+ "                                         WHEN 'D' THEN 'Offline Delta' WHEN 'E' THEN 'Online Delta' ELSE a.OPERATIONTYPE END \n"
			+ "                    || CASE a.OBJECTTYPE WHEN 'D' THEN ' Database Backup' WHEN 'P' THEN ' Table Space Backup' ELSE ' Backup' END \n"
			+ "          END AS VARCHAR(60))                                       as BACKUP_KIND \n"
			+ "    ,a.OPERATION \n"
			+ "    ,a.OPERATIONTYPE \n"
			+ "    ,a.OBJECTTYPE \n"
			+ "    ,a.DBPARTITIONNUM \n"
			+ "    ,l.START_TS                                                    as LAST_START_TIME \n"
			+ "    ,l.END_TS                                                      as LAST_END_TIME \n"
			+ "    ,CAST(CASE WHEN COALESCE(l.SQLCODE, 0) < 0 THEN 'FAILED' WHEN l.END_TS IS NULL THEN 'RUNNING' ELSE 'OK' END AS VARCHAR(10)) as LAST_STATUS \n"
			+ "    ,COALESCE(l.SQLCODE, 0)                                        as LAST_SQLCODE \n"
			+ "    ,SECONDS_BETWEEN(l.END_TS, l.START_TS)                         as LAST_DURATION_SEC \n"
			+ "    ,l.LOCATION                                                    as LAST_LOCATION \n"
			+ "    ,l.DEVICETYPE                                                  as LAST_DEVICETYPE \n"
			+ "    ,NULLIF(a.LAST_GOOD_TS, TIMESTAMP('0001-01-01-00.00.00'))     as LAST_GOOD_END_TIME \n"
			+ "    ,CAST(NULL AS DECIMAL(10,1))                                   as LAST_GOOD_AGE_HOURS \n"
			+ "    ,a.COUNT_24H \n"
			+ "    ,a.FAILED_24H \n"
			+ "    ,a.COUNT_7D \n"
			+ "    ,a.FAILED_7D \n"
			+ "from a \n"
			+ "join h l on l.OPERATION = a.OPERATION and l.OPERATIONTYPE = a.OPERATIONTYPE and l.OBJECTTYPE = a.OBJECTTYPE and l.DBPARTITIONNUM = a.DBPARTITIONNUM and l.RN = 1 \n"
			+ "order by a.OPERATION, a.OPERATIONTYPE \n"
			+ "";

		return sql;
	}

	/** LAST_GOOD_AGE_HOURS: computed here, from the sample time (the server's clock) */
	@Override
	public void localCalculation(com.dbxtune.cm.CounterSample newSample)
	{
		int pos_LastGood = newSample.findColumn("LAST_GOOD_END_TIME");
		int pos_AgeHours = newSample.findColumn("LAST_GOOD_AGE_HOURS");

		Timestamp now = newSample.getSampleTime();
		if (pos_LastGood == -1 || pos_AgeHours == -1 || now == null)
			return;

		for (int r = 0; r < newSample.getRowCount(); r++)
		{
			Timestamp lastGood = newSample.getValueAsTimestamp(r, pos_LastGood);
			if (lastGood != null)
				newSample.setValueAt(new java.math.BigDecimal((now.getTime() - lastGood.getTime()) / 3_600_000.0).setScale(1, java.math.RoundingMode.HALF_EVEN), r, pos_AgeHours);
		}
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addTable(CM_NAME, "The backups and the log archiving of the database (SYSIBMADM.DB_HISTORY).");

			mtd.addColumn(CM_NAME, "DB_NAME",             "<html>The database.</html>");
			mtd.addColumn(CM_NAME, "BACKUP_KIND",         "<html>The kind of backup (online or offline; full, incremental or delta; database or table space), or the log archive method.</html>");
			mtd.addColumn(CM_NAME, "OPERATION",           "<html>B = backup, X = log archive.</html>");
			mtd.addColumn(CM_NAME, "OPERATIONTYPE",       "<html>For a backup: F/N = offline/online full, I/O = offline/online incremental, D/E = offline/online delta. For a log archive: 1/2 = LOGARCHMETH1/2.</html>");
			mtd.addColumn(CM_NAME, "OBJECTTYPE",          "<html>D = database, P = table space(s).</html>");
			mtd.addColumn(CM_NAME, "DBPARTITIONNUM",      "<html>The database partition (0 if this is not DPF).</html>");
			mtd.addColumn(CM_NAME, "LAST_START_TIME",     "<html>When the last one started.</html>");
			mtd.addColumn(CM_NAME, "LAST_END_TIME",       "<html>When the last one ended (empty while it runs).</html>");
			mtd.addColumn(CM_NAME, "LAST_STATUS",         "<html>OK, FAILED (an error SQLCODE) or RUNNING.<br><b>Alarm</b>: LastBackupFailed (database backups)</html>");
			mtd.addColumn(CM_NAME, "LAST_SQLCODE",        "<html>The SQLCODE of the last one (0 = OK).</html>");
			mtd.addColumn(CM_NAME, "LAST_DURATION_SEC",   "<html>How long the last one took, in seconds.</html>");
			mtd.addColumn(CM_NAME, "LAST_LOCATION",       "<html>Where the last one was written (a directory, a TSM/vendor library...).</html>");
			mtd.addColumn(CM_NAME, "LAST_DEVICETYPE",     "<html>D = disk, A = TSM, O = other vendor, N = null device...</html>");
			mtd.addColumn(CM_NAME, "LAST_GOOD_END_TIME",  "<html>When the last one that went OK ended.</html>");
			mtd.addColumn(CM_NAME, "LAST_GOOD_AGE_HOURS", "<html>Hours since the last one that went OK ended.<br><b>Alarm</b>: OldBackup (the newest good database backup of any kind)</html>");
			mtd.addColumn(CM_NAME, "COUNT_24H",           "<html>How many in the last 24 hours.</html>");
			mtd.addColumn(CM_NAME, "FAILED_24H",          "<html>How many failed in the last 24 hours.</html>");
			mtd.addColumn(CM_NAME, "COUNT_7D",            "<html>How many in the last 7 days.</html>");
			mtd.addColumn(CM_NAME, "FAILED_7D",           "<html>How many failed in the last 7 days.</html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e);
		}
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

		// Only database backups (not table space backups or log archives)
		String    dbname         = null;
		Timestamp newestGoodTime = null;
		int       newestGoodRow  = -1;
		Timestamp newestTime     = null;
		int       newestRow      = -1;
		for (int r = 0; r < cm.getAbsRowCount(); r++)
		{
			if ( ! "B".equals(cm.getAbsString(r, "OPERATION")) || ! "D".equals(cm.getAbsString(r, "OBJECTTYPE")) )
				continue;

			dbname = cm.getAbsString(r, "DB_NAME");

			Timestamp start = cm.getAbsValue(r, "LAST_START_TIME") instanceof Timestamp ? (Timestamp) cm.getAbsValue(r, "LAST_START_TIME") : null;
			if (start != null && (newestTime == null || start.after(newestTime)))
			{
				newestTime = start;
				newestRow  = r;
			}

			Timestamp good = cm.getAbsValue(r, "LAST_GOOD_END_TIME") instanceof Timestamp ? (Timestamp) cm.getAbsValue(r, "LAST_GOOD_END_TIME") : null;
			if (good != null && (newestGoodTime == null || good.after(newestGoodTime)))
			{
				newestGoodTime = good;
				newestGoodRow  = r;
			}
		}

		//-------------------------------------------------------
		// LastBackupFailed: the newest database backup (of any kind) failed
		//-------------------------------------------------------
		if (newestRow != -1 && isSystemAlarmsForColumnEnabledAndInTimeRange("LastBackupFailed"))
		{
			String status = cm.getAbsString(newestRow, "LAST_STATUS");

			if (debugPrint || _logger.isDebugEnabled())
				System.out.println("##### sendAlarmRequest(" + cm.getName() + "): LastBackupFailed: dbname='" + dbname + "', kind='" + cm.getAbsString(newestRow, "BACKUP_KIND") + "', LAST_STATUS='" + status + "'.");

			if ("FAILED".equals(status) && conf.getBooleanProperty(PROPKEY_alarm_LastBackupFailed, DEFAULT_alarm_LastBackupFailed))
			{
				AlarmEvent ae = new AlarmEventLastBackupFailed(cm, dbname, cm.getAbsString(newestRow, "BACKUP_KIND") + " (SQLCODE " + cm.getAbsValue(newestRow, "LAST_SQLCODE") + ")", 0); // (getAbsString formats numbers with the locale: -2 061)

				final String raiseText = cm.toTextTableString(DATA_ABS, newestRow);
				final String raiseHtml = cm.toHtmlTableString(DATA_ABS, newestRow, true, false, false);
				ae.setAlarmDescriptionProvider(this, new AlarmDescriptionProviderBase(null, null, null)
				{
					@Override
					public void setValues(CountersModel cm, AlarmEvent alarmEvent, AlarmPhase phase)
					{
						alarmEvent.setExtendedDescription(raiseText, raiseHtml);
						if (AlarmPhase.CANCEL.equals(phase))
							alarmEvent.setCancelDescription("A newer database backup went OK (or is running)");
					}
				});

				ae.createAlarmOptionsMessage(this, "LastBackupFailed");

				AlarmHandler.getInstance().addAlarm(ae);
			}
		}

		//-------------------------------------------------------
		// OldBackup: no good database backup (of any kind: full, incremental or delta) in ## hours
		//-------------------------------------------------------
		if (newestGoodRow != -1 && isSystemAlarmsForColumnEnabledAndInTimeRange("OldBackup"))
		{
			int    threshold = conf.getIntProperty(PROPKEY_alarm_OldBackup, DEFAULT_alarm_OldBackup);
			Double ageHours  = cm.getAbsValueAsDouble(newestGoodRow, "LAST_GOOD_AGE_HOURS");

			if (debugPrint || _logger.isDebugEnabled())
				System.out.println("##### sendAlarmRequest(" + cm.getName() + "): OldBackup: dbname='" + dbname + "', threshold=" + threshold + ", newest good backup='" + newestGoodTime + "', LAST_GOOD_AGE_HOURS=" + ageHours + ".");

			if (ageHours != null && ageHours > threshold)
			{
				AlarmEvent ae = new AlarmEventOldBackup(cm, threshold, dbname, newestGoodTime + "", ageHours.intValue());

				final String raiseText = cm.toTextTableString(DATA_ABS, newestGoodRow);
				final String raiseHtml = cm.toHtmlTableString(DATA_ABS, newestGoodRow, true, false, false);
				ae.setAlarmDescriptionProvider(this, new AlarmDescriptionProviderBase(null, null, threshold)
				{
					@Override
					public void setValues(CountersModel cm, AlarmEvent alarmEvent, AlarmPhase phase)
					{
						alarmEvent.setExtendedDescription(raiseText, raiseHtml);
						if (AlarmPhase.CANCEL.equals(phase))
							alarmEvent.setCancelDescription("A newer database backup went OK");
					}
				});

				ae.createAlarmOptionsMessage(this, "OldBackup");

				AlarmHandler.getInstance().addAlarm(ae);
			}
		}
	}

	public static final String  PROPKEY_alarm_LastBackupFailed = CM_NAME + ".alarm.system.if.LAST_STATUS.isFailed";
	public static final boolean DEFAULT_alarm_LastBackupFailed = true;

	public static final String  PROPKEY_alarm_OldBackup        = CM_NAME + ".alarm.system.if.LAST_GOOD_AGE_HOURS.gt";
	public static final int     DEFAULT_alarm_OldBackup        = 36;

	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;

		list.add(new CmSettingsHelper("LastBackupFailed", isAlarmSwitch, PROPKEY_alarm_LastBackupFailed, Boolean.class, conf.getBooleanProperty(PROPKEY_alarm_LastBackupFailed, DEFAULT_alarm_LastBackupFailed), DEFAULT_alarm_LastBackupFailed, "If the newest database backup (of any kind) FAILED, then send 'AlarmEventLastBackupFailed'." ));
		list.add(new CmSettingsHelper("OldBackup"       , isAlarmSwitch, PROPKEY_alarm_OldBackup       , Integer.class, conf.getIntProperty    (PROPKEY_alarm_OldBackup       , DEFAULT_alarm_OldBackup       ), DEFAULT_alarm_OldBackup       , "If the newest good database backup (full, incremental or delta) is older than ## hours, then send 'AlarmEventOldBackup'." ));

		return list;
	}
}
