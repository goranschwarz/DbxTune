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
import com.dbxtune.alarm.events.oracle.AlarmEventOraLastBackupFailed;
import com.dbxtune.alarm.events.oracle.AlarmEventOraOldBackup;
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
import com.dbxtune.utils.Configuration;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmRmanBackup
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmRmanBackup.class.getSimpleName();
	public static final String   SHORT_NAME       = "RMAN Backups";
	public static final String   HTML_DESC        =
		"<html>" +
		"The RMAN backup jobs from the last days (newest first): what was backed up, did it work, how long did it take, and how big was it.<br>" +
		"<br>" +
		"One row is one RMAN job (one RMAN session). <code>INPUT_TYPE</code> is the 'biggest' thing the job did: a job that makes a backup of the database <b>and</b> the archived logs is shown as <code>DB FULL</code> or <code>DB INCR</code>.<br>" +
		"The last successful job, and the last finished job, of every type is always shown (even if it is older than the days we look back).<br>" +
		"<br>" +
		"<b>Note</b>: No rows means that the database is not backed up with RMAN (or at least not within the time the control file remembers, see <code>control_file_record_keep_time</code>).<br>" +
		"In that case no alarm is sent. Backups made in other ways (storage snapshots, export...) are not known to the database.<br>" +
		"<b>Note</b>: In a Container Database, connect to CDB$ROOT to see the backup jobs. When connected to a PDB, only jobs that was started from inside that PDB are shown.<br>" +
		"<b>Note</b>: If this is slow (it reads the control file), gather statistics on the fixed objects: <code>exec DBMS_STATS.GATHER_FIXED_OBJECTS_STATS</code><br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {CM_NAME, "V$RMAN_BACKUP_JOB_DETAILS"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 1800;
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

		return new CmRmanBackup(counterController, guiController);
	}

	public CmRmanBackup(ICounterController counterController, IGuiController guiController)
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
	public static final String  PROPKEY_sample_days = CM_NAME + ".sample.days";
	public static final int     DEFAULT_sample_days = 7;

	@Override
	protected void registerDefaultValues()
	{
		super.registerDefaultValues();

		Configuration.registerDefaultValue(PROPKEY_sample_days, DEFAULT_sample_days);
	}

	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public List<CmSettingsHelper> getLocalSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		list.add(new CmSettingsHelper("Show Jobs for the last ## Days", PROPKEY_sample_days, Integer.class, conf.getIntProperty(PROPKEY_sample_days, DEFAULT_sample_days), DEFAULT_sample_days, "Show the RMAN backup jobs that started in the last ## days. (the last successful job, and the last finished job, of every type is always shown)" ));

		return list;
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("SESSION_KEY");
		pkCols.add("COMMAND_ID");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		int days = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_sample_days, DEFAULT_sample_days);

		// One row for every RMAN backup job. This looks the same in all versions (and in a CDB the jobs belongs to the whole CDB)
		//   OUTCOME          - makes the 7 different STATUS values into: OK, FAILED or RUNNING
		//   IS_LAST_OF_TYPE  - YES for the newest job of every INPUT_TYPE that has finished (so this is where we see if the last backup of a type FAILED)
		//   AGE_HOURS        - is calculated with the DBMS clock (SYSDATE), so we do not care about time zones
		// Note: CAST(... as NUMBER(#,1)) is needed to keep decimals (a NUMBER without precision is treated as a BIGINT), and ELAPSED_SECONDS/COMPRESSION_RATIO is a FLOAT
		String sql = ""
			+ "select \n"
			+ "     SESSION_KEY \n"
			+ "    ,COMMAND_ID \n"
			+ "    ,INPUT_TYPE \n"
			+ "    ,STATUS \n"
			+ "    ,OUTCOME \n"
			+ "    ,IS_LAST_OF_TYPE \n"
			+ "    ,START_TIME \n"
			+ "    ,END_TIME \n"
			+ "    ,ELAPSED_SECONDS \n"
			+ "    ,AGE_HOURS \n"
			+ "    ,INPUT_MB \n"
			+ "    ,OUTPUT_MB \n"
			+ "    ,COMPRESSION_RATIO \n"
			+ "    ,INPUT_MB_PER_SEC \n"
			+ "    ,OUTPUT_MB_PER_SEC \n"
			+ "    ,OUTPUT_DEVICE_TYPE \n"
			+ "    ,AUTOBACKUP_DONE \n"
			+ "    ,OPTIMIZED \n"
			+ "from (select \n"
			+ "           j.* \n"
			+ "          ,CAST(case when j.OUTCOME <> 'RUNNING' and row_number() over (partition by j.INPUT_TYPE, decode(j.OUTCOME, 'RUNNING', 1, 0) order by j.START_TIME desc, j.SESSION_KEY desc) = 1 then 'YES' else 'NO' end as VARCHAR2(3)) as IS_LAST_OF_TYPE \n"
			+ "          ,row_number() over (partition by j.INPUT_TYPE, j.OUTCOME order by j.START_TIME desc, j.SESSION_KEY desc) as OUTCOME_ROW_NUM \n"
			+ "      from (select \n"
			+ "                 SESSION_KEY \n"
			+ "                ,CAST(nvl(COMMAND_ID, '-') as VARCHAR2(40))                           as COMMAND_ID \n"
			+ "                ,INPUT_TYPE \n"
			+ "                ,STATUS \n"
			+ "                ,CAST(case when STATUS in ('COMPLETED', 'COMPLETED WITH WARNINGS') then 'OK' when STATUS like 'RUNNING%' then 'RUNNING' else 'FAILED' end as VARCHAR2(10)) as OUTCOME \n"
			+ "                ,START_TIME \n"
			+ "                ,END_TIME \n"
			+ "                ,CAST(ELAPSED_SECONDS                                 as NUMBER(12,0)) as ELAPSED_SECONDS \n"
			+ "                ,CAST((SYSDATE - nvl(END_TIME, START_TIME)) * 24      as NUMBER(12,1)) as AGE_HOURS \n"
			+ "                ,CAST(INPUT_BYTES          / 1048576                  as NUMBER(18,1)) as INPUT_MB \n"
			+ "                ,CAST(OUTPUT_BYTES         / 1048576                  as NUMBER(18,1)) as OUTPUT_MB \n"
			+ "                ,CAST(COMPRESSION_RATIO                               as NUMBER(10,2)) as COMPRESSION_RATIO \n"
			+ "                ,CAST(INPUT_BYTES_PER_SEC  / 1048576                  as NUMBER(18,1)) as INPUT_MB_PER_SEC \n"
			+ "                ,CAST(OUTPUT_BYTES_PER_SEC / 1048576                  as NUMBER(18,1)) as OUTPUT_MB_PER_SEC \n"
			+ "                ,OUTPUT_DEVICE_TYPE \n"
			+ "                ,AUTOBACKUP_DONE \n"
			+ "                ,OPTIMIZED \n"
			+ "            from V$RMAN_BACKUP_JOB_DETAILS \n"
			+ "           ) j \n"
			+ "     ) \n"
			+ "where START_TIME > SYSDATE - " + days + " \n"
			+ "   or IS_LAST_OF_TYPE = 'YES' \n"
			+ "   or (OUTCOME = 'OK' and OUTCOME_ROW_NUM = 1) \n"
			+ "order by START_TIME desc, SESSION_KEY desc \n"
			+ "";

		return sql;
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			mtd.addTable(CM_NAME, "The RMAN backup jobs from the last days (newest first): what was backed up, did it work, how long did it take, and how big was it.");

			mtd.addColumn(CM_NAME, "SESSION_KEY",        "<html>Identifies the RMAN job (session). Use it to see the output of the job: <code>select OUTPUT from V$RMAN_OUTPUT where SESSION_KEY = ## order by RECID</code><br><b>Note</b>: V$RMAN_OUTPUT is kept in memory, so it is empty after the instance has been restarted.</html>");
			mtd.addColumn(CM_NAME, "COMMAND_ID",         "<html>ID of the job. If the RMAN script did not set one (<code>set command id to '...'</code>), it is the time when the RMAN session started.</html>");
			mtd.addColumn(CM_NAME, "INPUT_TYPE",         "<html>What was backed up. If the job backed up several things, the 'biggest' one is shown, in this order:<br><code>DB FULL, RECVR AREA, DB INCR, DATAFILE FULL, DATAFILE INCR, ARCHIVELOG, CONTROLFILE, SPFILE</code></html>");
			mtd.addColumn(CM_NAME, "STATUS",             "<html>How the job ended (or that it is running):<br><code>COMPLETED, COMPLETED WITH WARNINGS, COMPLETED WITH ERRORS, FAILED, RUNNING, RUNNING WITH WARNINGS, RUNNING WITH ERRORS</code></html>");
			mtd.addColumn(CM_NAME, "OUTCOME",            "<html>A simpler version of STATUS:<ul><li><b>OK</b> - COMPLETED or COMPLETED WITH WARNINGS</li><li><b>FAILED</b> - FAILED or COMPLETED WITH ERRORS</li><li><b>RUNNING</b> - the job is running right now</li></ul></html>");
			mtd.addColumn(CM_NAME, "IS_LAST_OF_TYPE",    "<html>YES = This is the newest finished job of this INPUT_TYPE.<br>If OUTCOME is FAILED for such a row, the alarm 'LastBackupFailed' is sent.</html>");
			mtd.addColumn(CM_NAME, "ELAPSED_SECONDS",    "<html>How long the job took, in seconds.</html>");
			mtd.addColumn(CM_NAME, "AGE_HOURS",          "<html>How many hours ago the job ended (if it is still running: how many hours ago it started).<br><b>Formula</b>: (SYSDATE - nvl(END_TIME, START_TIME)) * 24</html>");
			mtd.addColumn(CM_NAME, "INPUT_MB",           "<html>How many MB that was read.</html>");
			mtd.addColumn(CM_NAME, "OUTPUT_MB",          "<html>How many MB that was written (size of the backup).</html>");
			mtd.addColumn(CM_NAME, "COMPRESSION_RATIO",  "<html>INPUT / OUTPUT. 1 = not compressed (or nothing to gain).</html>");
			mtd.addColumn(CM_NAME, "INPUT_MB_PER_SEC",   "<html>Read speed in MB per second.</html>");
			mtd.addColumn(CM_NAME, "OUTPUT_MB_PER_SEC",  "<html>Write speed in MB per second.</html>");
			mtd.addColumn(CM_NAME, "OUTPUT_DEVICE_TYPE", "<html>Where the backup was written: DISK, SBT_TAPE (a media manager / backup software), or * (both).</html>");
			mtd.addColumn(CM_NAME, "AUTOBACKUP_DONE",    "<html>YES = the control file and spfile was automatically backed up by this job.</html>");
			mtd.addColumn(CM_NAME, "OPTIMIZED",          "<html>YES = backup optimization was used (files that already has a backup was skipped).</html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e);
		}
	}

	/** Find the newest successful backup job where INPUT_TYPE matches the regexp. @return rowId, or -1 if no such row was found */
	private static int getLastOkBackupRow(CountersModel cm, String inputTypeRegex)
	{
		int    lastRow = -1;
		Double lastAge = null;

		for (int r=0; r<cm.getAbsRowCount(); r++)
		{
			String inputType = cm.getAbsString(r, "INPUT_TYPE");
			Double ageHours  = cm.getAbsValueAsDouble(r, "AGE_HOURS");

			if ( ! "OK".equals(cm.getAbsString(r, "OUTCOME")) || inputType == null || ageHours == null || ! inputType.matches(inputTypeRegex) )
				continue;

			if (lastAge == null || ageHours < lastAge)
			{
				lastRow = r;
				lastAge = ageHours;
			}
		}
		return lastRow;
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

		// No rows: the database is NOT backed up with RMAN (so we can't say anything about the backups)
		if (cm.getAbsRowCount() == 0)
			return;

		//-------------------------------------------------------
		// LastDbBackupAgeInHours
		//-------------------------------------------------------
		if (isSystemAlarmsForColumnEnabledAndInTimeRange("LastDbBackupAgeInHours"))
		{
			int    threshold      = conf.getIntProperty(PROPKEY_alarm_LastDbBackupAgeInHours          , DEFAULT_alarm_LastDbBackupAgeInHours);
			String inputTypeRegex = conf.getProperty   (PROPKEY_alarm_LastDbBackupAgeInHoursInputTypes, DEFAULT_alarm_LastDbBackupAgeInHoursInputTypes);

			int    lastRow     = getLastOkBackupRow(cm, inputTypeRegex);
			Double ageHours    = lastRow == -1 ? null : cm.getAbsValueAsDouble(lastRow, "AGE_HOURS");
			String lastType    = lastRow == -1 ? null : cm.getAbsString       (lastRow, "INPUT_TYPE");
			String lastEndTime = lastRow == -1 ? null : cm.getAbsString       (lastRow, "END_TIME");

			if (debugPrint || _logger.isDebugEnabled())
				System.out.println("##### sendAlarmRequest(" + cm.getName() + "): LastDbBackupAgeInHours: threshold=" + threshold + ", inputTypeRegex='" + inputTypeRegex + "', AGE_HOURS='" + ageHours + "', INPUT_TYPE='" + lastType + "', END_TIME='" + lastEndTime + "'.");

			// No successful database backup at all, or it's to old
			if (ageHours == null || ageHours > threshold)
			{
				AlarmEvent ae = new AlarmEventOraOldBackup(cm, threshold, lastType, lastEndTime, ageHours);

				// The below is called from AlarmHandler on: Raise, RE-RAISE & CANCEL
				ae.setAlarmDescriptionProvider(this, new AlarmDescriptionProviderBase(null, null, threshold)
				{
					@Override
					public void setValues(CountersModel cm, AlarmEvent alarmEvent, AlarmPhase phase)
					{
						// The newest successful database backup, in the CURRENT data
						int rowId = getLastOkBackupRow(cm, inputTypeRegex);
						if (rowId == -1)
							return;

						// Always: (RAISE, RE-RAISE, CANCEL) - Set the row as a table
						String extendedDescText = cm.toTextTableString(DATA_ABS, rowId);
						String extendedDescHtml = cm.toHtmlTableString(DATA_ABS, rowId, true, false, false);
						alarmEvent.setExtendedDescription(extendedDescText, extendedDescHtml);

						// Set CANCEL message (values that was found AFTER last raise/re-raise event)
						if (AlarmPhase.CANCEL.equals(phase))
						{
							String cancelMsg = getAlarmCancelText("The last successful RMAN database backup", cm.getAbsValueAsDouble(rowId, "AGE_HOURS") + " hours old (type '" + cm.getAbsString(rowId, "INPUT_TYPE") + "', ended " + cm.getAbsString(rowId, "END_TIME") + ")", " hours");
							alarmEvent.setCancelDescription(cancelMsg);
						}
					}
				});

				// Information about how to disable this alarm
				ae.createAlarmOptionsMessage(this, "LastDbBackupAgeInHours");

				AlarmHandler.getInstance().addAlarm(ae);
			}
		} // end: LastDbBackupAgeInHours

		//-------------------------------------------------------
		// LastBackupFailed
		//-------------------------------------------------------
		if (isSystemAlarmsForColumnEnabledAndInTimeRange("LastBackupFailed") && conf.getBooleanProperty(PROPKEY_alarm_LastBackupFailed, DEFAULT_alarm_LastBackupFailed))
		{
			int maxAgeHours = conf.getIntProperty(PROPKEY_sample_days, DEFAULT_sample_days) * 24;

			for (int r=0; r<cm.getAbsRowCount(); r++)
			{
				String inputType = cm.getAbsString       (r, "INPUT_TYPE");
				String status    = cm.getAbsString       (r, "STATUS");
				Double ageHours  = cm.getAbsValueAsDouble(r, "AGE_HOURS", 0d);

				// Only the newest finished job of every type, and if that was a long time ago we do not care
				if ( ! "YES".equals(cm.getAbsString(r, "IS_LAST_OF_TYPE")) || ageHours > maxAgeHours )
					continue;

				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest(" + cm.getName() + "): LastBackupFailed: INPUT_TYPE='" + inputType + "', STATUS='" + status + "', OUTCOME='" + cm.getAbsString(r, "OUTCOME") + "', AGE_HOURS='" + ageHours + "'.");

				if ("FAILED".equals(cm.getAbsString(r, "OUTCOME")))
				{
					AlarmEvent ae = new AlarmEventOraLastBackupFailed(cm, inputType, status, cm.getAbsString(r, "START_TIME"), cm.getAbsString(r, "END_TIME"), cm.getAbsValueAsLong(r, "SESSION_KEY", -1L));

					// The below is called from AlarmHandler on: Raise, RE-RAISE & CANCEL
					ae.setAlarmDescriptionProvider(this, new AlarmDescriptionProviderBase(null, null, null)
					{
						@Override
						public void setValues(CountersModel cm, AlarmEvent alarmEvent, AlarmPhase phase)
						{
							// The newest finished job of this type, in the CURRENT data (on CANCEL: this is a newer job than the one that failed)
							int rowId = -1;
							for (int i=0; i<cm.getAbsRowCount() && rowId == -1; i++)
							{
								if (inputType.equals(cm.getAbsString(i, "INPUT_TYPE")) && "YES".equals(cm.getAbsString(i, "IS_LAST_OF_TYPE")))
									rowId = i;
							}
							String label = "The last RMAN backup of type '" + inputType + "'";

							if (rowId != -1)
							{
								// Always: (RAISE, RE-RAISE, CANCEL) - Set the row as a table
								String extendedDescText = cm.toTextTableString(DATA_ABS, rowId);
								String extendedDescHtml = cm.toHtmlTableString(DATA_ABS, rowId, true, false, false);
								alarmEvent.setExtendedDescription(extendedDescText, extendedDescHtml);

								// Set CANCEL message (values that was found AFTER last raise/re-raise event)
								if (AlarmPhase.CANCEL.equals(phase))
									alarmEvent.setCancelDescription(getAlarmCancelText(label + ": STATUS", "'" + cm.getAbsString(rowId, "STATUS") + "' (started " + cm.getAbsString(rowId, "START_TIME") + ")"));
							}
							else
							{
								if (AlarmPhase.CANCEL.equals(phase))
									alarmEvent.setCancelDescription(getAlarmCancelTextGone(label));
							}
						}
					});

					// Information about how to disable this alarm
					ae.createAlarmOptionsMessage(this, "LastBackupFailed");

					AlarmHandler.getInstance().addAlarm(ae);
				}
			} // end: loop-rows
		} // end: LastBackupFailed
	}

	public static final String  PROPKEY_alarm_LastDbBackupAgeInHours           = CM_NAME + ".alarm.system.if.LastDbBackupAgeInHours.gt";
	public static final int     DEFAULT_alarm_LastDbBackupAgeInHours           = 36;

	public static final String  PROPKEY_alarm_LastDbBackupAgeInHoursInputTypes = CM_NAME + ".alarm.system.if.LastDbBackupAgeInHours.inputTypes";
	public static final String  DEFAULT_alarm_LastDbBackupAgeInHoursInputTypes = "DB (FULL|INCR)";

	public static final String  PROPKEY_alarm_LastBackupFailed                 = CM_NAME + ".alarm.system.if.LastBackup.is.FAILED";
	public static final boolean DEFAULT_alarm_LastBackupFailed                 = true;

	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;

		list.add(new CmSettingsHelper("LastDbBackupAgeInHours"           , isAlarmSwitch, PROPKEY_alarm_LastDbBackupAgeInHours          , Integer.class, conf.getIntProperty    (PROPKEY_alarm_LastDbBackupAgeInHours          , DEFAULT_alarm_LastDbBackupAgeInHours          ), DEFAULT_alarm_LastDbBackupAgeInHours          , "If the last successful RMAN database backup is older than ## hours (or if there is none) then send 'AlarmEventOraOldBackup'. No alarm if the database has no RMAN backup jobs at all." ));
		list.add(new CmSettingsHelper("LastDbBackupAgeInHours InputTypes",                PROPKEY_alarm_LastDbBackupAgeInHoursInputTypes, String .class, conf.getProperty       (PROPKEY_alarm_LastDbBackupAgeInHoursInputTypes, DEFAULT_alarm_LastDbBackupAgeInHoursInputTypes), DEFAULT_alarm_LastDbBackupAgeInHoursInputTypes, "If 'LastDbBackupAgeInHours' is true; What counts as a database backup: 'INPUT_TYPE' must match this regexp. To only accept full backups: 'DB FULL'", new RegExpInputValidator()));
		list.add(new CmSettingsHelper("LastBackupFailed"                 , isAlarmSwitch, PROPKEY_alarm_LastBackupFailed                , Boolean.class, conf.getBooleanProperty(PROPKEY_alarm_LastBackupFailed                , DEFAULT_alarm_LastBackupFailed                ), DEFAULT_alarm_LastBackupFailed                , "If the newest finished RMAN job of any 'INPUT_TYPE' has STATUS 'FAILED' or 'COMPLETED WITH ERRORS' then send 'AlarmEventOraLastBackupFailed'. (jobs older than '" + PROPKEY_sample_days + "' days are not checked)" ));

		return list;
	}
}
