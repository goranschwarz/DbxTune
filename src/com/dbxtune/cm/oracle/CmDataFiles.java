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
import com.dbxtune.alarm.events.oracle.AlarmEventOraDataFileProblem;
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
public class CmDataFiles
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmDataFiles.class.getSimpleName();
	public static final String   SHORT_NAME       = "Data Files";
	public static final String   HTML_DESC        =
		"<html>" +
		"One row per data file and temp file, and if something is wrong with it. Normally there is nothing to see here: this tab is for it's alarms.<br>" +
		"<br>" +
		"What it looks for:" +
		"<ul>" +
		"  <li><code>STATUS</code> is not ONLINE (or SYSTEM): the file is OFFLINE, or it needs RECOVER. What is in it can not be read.</li>" +
		"  <li><code>NEEDS_RECOVERY</code> is YES, or <code>FILE_ERROR</code> says something: the file is missing, or it is older than the rest of the database.</li>" +
		"  <li><code>CORRUPT_BLOCKS</code> is above 0: RMAN has found blocks that can not be read. A query that touches one of them fails (ORA-01578), until then nobody knows.</li>" +
		"  <li><code>BACKUP_MODE</code> is ACTIVE for hours: someone did <code>BEGIN BACKUP</code> and never <code>END BACKUP</code>.</li>" +
		"</ul>" +
		"<b>Note</b>: The corrupt blocks are the ones that RMAN has seen (a backup or a <code>VALIDATE</code>). A database that is not backed up with RMAN has an empty list, corrupt or not.<br>" +
		"<b>Note</b>: How big the files are and how full, is in the tab 'Tablespaces'. How much they are read and written, is in the tab 'File IO'.<br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_DISK;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {CM_NAME, "V$DATAFILE", "V$DATAFILE_HEADER", "V$RECOVER_FILE", "V$BACKUP", "V$DATABASE_BLOCK_CORRUPTION", "V$TEMPFILE"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 300;
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

		return new CmDataFiles(counterController, guiController);
	}

	public CmDataFiles(ICounterController counterController, IGuiController guiController)
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

		// The file number is unique in the whole database (also in a Container Database). Data files and temp files have their own numbers
		pkCols.add("FILE_TYPE");
		pkCols.add("FILE_ID");

		return pkCols;
	}

	/** If we can't read the views: disable the CM, and tell what to do about it */
	@Override
	public boolean checkDependsOnOther(DbxConnection conn)
	{
		return OracleCmUtils.checkViewsAreReadable(this, conn, "V$DATAFILE", "V$DATAFILE_HEADER", "V$RECOVER_FILE", "V$BACKUP", "V$DATABASE_BLOCK_CORRUPTION", "V$TEMPFILE", "V$TABLESPACE");
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		DbmsVersionInfoOracle oraVersionInfo = (DbmsVersionInfoOracle) versionInfo;

		// 12.1 and above: the V$ views has CON_ID, and the same tablespace number (TS#) exists in every container
		// Connected to CDB$ROOT we see the files of ALL containers, so there we show which container it is
		boolean hasConId  = versionInfo.getLongVersion() >= Ver.ver(12,1);
		boolean isCdbRoot = hasConId && oraVersionInfo.isCdbRoot();

		// Note: CAST(... as NUMBER(18,1)) is needed to keep decimals (a NUMBER without precision is treated as a BIGINT)
		//       NEEDS_RECOVERY: V$RECOVER_FILE has a row for the file, or the header of the file says so
		String sql = ""
			+ "select \n"
			+ "     " + (isCdbRoot ? "d.CON_ID" : "CAST(0    as NUMBER(10))")    + " as CON_ID \n"
			+ "    ," + (isCdbRoot ? "c.NAME"   : "CAST(NULL as VARCHAR2(128))") + " as CON_NAME \n"
			+ "    ,CAST('DATA' as VARCHAR2(4))                                                                 as FILE_TYPE \n"
			+ "    ,d.FILE#                                                                                     as FILE_ID \n"
			+ "    ,t.NAME                                                                                      as TABLESPACE_NAME \n"
			+ "    ,d.STATUS \n"
			+ "    ,d.ENABLED \n"
			+ "    ,CAST(NVL2(rf.FILE#, 'YES', h.RECOVER) as VARCHAR2(3))                                       as NEEDS_RECOVERY \n"
			+ "    ,CAST(NVL(rf.ERROR, h.ERROR) as VARCHAR2(100))                                               as FILE_ERROR \n"
			+ "    ,NVL(x.CORRUPT_BLOCKS, 0)                                                                    as CORRUPT_BLOCKS \n"
			+ "    ,CAST(b.STATUS as VARCHAR2(20))                                                              as BACKUP_MODE \n"
			+ "    ,case when b.STATUS = 'ACTIVE' then b.TIME end                                               as BACKUP_MODE_SINCE \n"
			+ "    ,CAST(case when b.STATUS = 'ACTIVE' then (SYSDATE - b.TIME) * 24 end as NUMBER(10,1))        as BACKUP_MODE_HOURS \n"
			+ "    ,CAST(d.BYTES / 1048576 as NUMBER(18,1))                                                     as SIZE_MB \n"
			+ "    ,h.FUZZY \n"
			+ "    ,d.CHECKPOINT_TIME \n"
			+ "    ,d.CREATION_TIME \n"
			+ "    ,d.NAME                                                                                      as FILE_NAME \n"
			+ "from V$DATAFILE d \n"
			+ (isCdbRoot ? "left join V$CONTAINERS c      on c.CON_ID = d.CON_ID \n" : "")
			+ "left join V$TABLESPACE t      on t.TS#    = d.TS#" + (hasConId ? " and t.CON_ID = d.CON_ID" : "") + " \n"
			+ "left join V$DATAFILE_HEADER h on h.FILE#  = d.FILE# \n"
			+ "left join V$RECOVER_FILE rf   on rf.FILE# = d.FILE# \n"
			+ "left join V$BACKUP b          on b.FILE#  = d.FILE# \n"
			+ "left join (select FILE#, sum(BLOCKS) as CORRUPT_BLOCKS from V$DATABASE_BLOCK_CORRUPTION group by FILE#) x on x.FILE# = d.FILE# \n"
			+ "union all \n"
			+ "select \n"
			+ "     " + (isCdbRoot ? "f.CON_ID" : "CAST(0    as NUMBER(10))") + " \n"
			+ "    ," + (isCdbRoot ? "c.NAME"   : "CAST(NULL as VARCHAR2(128))") + " \n"
			+ "    ,CAST('TEMP' as VARCHAR2(4)) \n"
			+ "    ,f.FILE# \n"
			+ "    ,t.NAME \n"
			+ "    ,f.STATUS \n"
			+ "    ,f.ENABLED \n"
			+ "    ,CAST(NULL as VARCHAR2(3)) \n"
			+ "    ,CAST(NULL as VARCHAR2(100)) \n"
			+ "    ,0 \n"
			+ "    ,CAST(NULL as VARCHAR2(20)) \n"
			+ "    ,CAST(NULL as DATE) \n"
			+ "    ,CAST(NULL as NUMBER(10,1)) \n"
			+ "    ,CAST(f.BYTES / 1048576 as NUMBER(18,1)) \n"
			+ "    ,CAST(NULL as VARCHAR2(3)) \n"
			+ "    ,CAST(NULL as DATE) \n"
			+ "    ,f.CREATION_TIME \n"
			+ "    ,f.NAME \n"
			+ "from V$TEMPFILE f \n"
			+ (isCdbRoot ? "left join V$CONTAINERS c      on c.CON_ID = f.CON_ID \n" : "")
			+ "left join V$TABLESPACE t      on t.TS#    = f.TS#" + (hasConId ? " and t.CON_ID = f.CON_ID" : "") + " \n"
			+ "order by 3, 4 \n"
			+ "";

		return sql;
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			mtd.addTable(CM_NAME, "One row per data file and temp file, and if something is wrong with it.");

			mtd.addColumn(CM_NAME, "CON_ID",             "<html>Container ID. 0 = This is not a Container Database (or the version is below 12.1), or we are connected to a PDB.</html>");
			mtd.addColumn(CM_NAME, "CON_NAME",           "<html>Name of the Container (PDB) the file belongs to. NULL if CON_ID is 0.</html>");
			mtd.addColumn(CM_NAME, "FILE_TYPE",          "<html>DATA = a data file (V$DATAFILE). TEMP = a temp file (V$TEMPFILE).</html>");
			mtd.addColumn(CM_NAME, "FILE_ID",            "<html>The number of the file (FILE#). Data files and temp files have their own numbers.</html>");
			mtd.addColumn(CM_NAME, "TABLESPACE_NAME",    "<html>The tablespace that the file belongs to.</html>");
			mtd.addColumn(CM_NAME, "STATUS",             "<html>Data file: ONLINE, SYSTEM (online, and part of the SYSTEM tablespace), OFFLINE, RECOVER (needs recovery), SYSOFF.<br>Temp file: ONLINE or OFFLINE.</html>");
			mtd.addColumn(CM_NAME, "ENABLED",            "<html>READ WRITE, READ ONLY or DISABLED (no access).</html>");
			mtd.addColumn(CM_NAME, "NEEDS_RECOVERY",     "<html>YES = the file needs media recovery (it is in V$RECOVER_FILE, or the header of the file says so). NO = it does not.<br>Empty = not known (a temp file, or a file which header can't be read).</html>");
			mtd.addColumn(CM_NAME, "FILE_ERROR",         "<html>What Oracle says is wrong with the file: FILE NOT FOUND, CANNOT OPEN FILE, OFFLINE NORMAL, WRONG FILE TYPE... Empty = nothing.</html>");
			mtd.addColumn(CM_NAME, "CORRUPT_BLOCKS",     "<html>Number of corrupt blocks in the file, that RMAN has found (V$DATABASE_BLOCK_CORRUPTION).<br>The list is updated when RMAN backs up or validates the file: so 0 means 'none seen at the last backup'.</html>");
			mtd.addColumn(CM_NAME, "BACKUP_MODE",        "<html>ACTIVE = the file is in backup mode (ALTER DATABASE or TABLESPACE ... BEGIN BACKUP). NOT ACTIVE = it is not.<br>RMAN does not use backup mode.</html>");
			mtd.addColumn(CM_NAME, "BACKUP_MODE_SINCE",  "<html>When the file was put in backup mode. Empty if it is not.</html>");
			mtd.addColumn(CM_NAME, "BACKUP_MODE_HOURS",  "<html>Number of hours that the file has been in backup mode. Empty if it is not.<br><b>Formula</b>: (SYSDATE - V$BACKUP.TIME) * 24</html>");
			mtd.addColumn(CM_NAME, "SIZE_MB",            "<html>Size of the file in MB.</html>");
			mtd.addColumn(CM_NAME, "FUZZY",              "<html>YES = the file has changes after it's last checkpoint (normal for a file that is open). NO = it is consistent (a read only file, or a file that was closed in a normal way).</html>");
			mtd.addColumn(CM_NAME, "CHECKPOINT_TIME",    "<html>Time of the last checkpoint of the file.</html>");
			mtd.addColumn(CM_NAME, "CREATION_TIME",      "<html>When the file was created.</html>");
			mtd.addColumn(CM_NAME, "FILE_NAME",          "<html>Name of the file.</html>");
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

		String skipTablespaceRegex = conf.getProperty(PROPKEY_alarm_SkipTablespaceRegex, DEFAULT_alarm_SkipTablespaceRegex);

		for (int r=0; r<cm.getAbsRowCount(); r++)
		{
			String fileType       = cm.getAbsString(r, "FILE_TYPE");
			String fileId         = cm.getAbsString(r, "FILE_ID");
			String fileName       = cm.getAbsString(r, "FILE_NAME");
			String tsName         = cm.getAbsString(r, "TABLESPACE_NAME");
			String conName        = cm.getAbsString(r, "CON_NAME");
			String status         = cm.getAbsString(r, "STATUS");
			String needsRecovery  = cm.getAbsString(r, "NEEDS_RECOVERY");
			String fileError      = cm.getAbsString(r, "FILE_ERROR");
			Double corruptBlocks  = cm.getAbsValueAsDouble(r, "CORRUPT_BLOCKS");
			Double backupModeHours= cm.getAbsValueAsDouble(r, "BACKUP_MODE_HOURS");

			if (fileType == null || status == null)
				continue;

			// tablespace   or in a CDB: container:tablespace
			String tsLabel   = StringUtil.hasValue(conName) ? conName + ":" + tsName : tsName + "";

			// fileKey identifies the file in the alarm (it must be short), fileLabel is how it is written in the text
			String fileKey   = fileType + "#" + fileId;
			String fileLabel = "#" + fileId + " '" + fileName + "' (tablespace " + tsLabel + ")";

			// Tablespaces we do not care about
			if (StringUtil.hasValue(skipTablespaceRegex) && ((tsName + "").matches(skipTablespaceRegex) || tsLabel.matches(skipTablespaceRegex)))
				continue;

			// The four alarms below gets the same extra text: the row as a table, and at CANCEL what the row says now. So ONE provider for all of them
			// The below is called from AlarmHandler on: Raise, RE-RAISE & CANCEL
			AlarmDescriptionProviderBase descriptionProvider = new AlarmDescriptionProviderBase(cm.getPk(), cm.getAbsPkValue(r), null)
			{
				@Override
				public void setValues(CountersModel cm, AlarmEvent alarmEvent, AlarmPhase phase)
				{
					int    rowId = cm.getAbsRowIdForPkValue(getRowPk());
					String label = "File " + fileLabel;

					if (rowId != -1)
					{
						// Always: (RAISE, RE-RAISE, CANCEL) - Set the row as a table
						String extendedDescText = cm.toTextTableString(DATA_ABS, rowId);
						String extendedDescHtml = cm.toHtmlTableString(DATA_ABS, rowId, true, false, false);
						alarmEvent.setExtendedDescription(extendedDescText, extendedDescHtml);

						// Set CANCEL message (values that was found AFTER last raise/re-raise event)
						if (AlarmPhase.CANCEL.equals(phase))
							alarmEvent.setCancelDescription(getAlarmCancelText(label + ": STATUS", "'" + cm.getAbsString(rowId, "STATUS") + "'"
									+ ", NEEDS_RECOVERY='"  + cm.getAbsString(rowId, "NEEDS_RECOVERY") + "'"
									+ ", CORRUPT_BLOCKS="   + cm.getAbsString(rowId, "CORRUPT_BLOCKS")
									+ ", BACKUP_MODE='"     + cm.getAbsString(rowId, "BACKUP_MODE")    + "'"));
					}
					else
					{
						// The row is gone: the file (or it's tablespace) was dropped
						if (AlarmPhase.CANCEL.equals(phase))
							alarmEvent.setCancelDescription(getAlarmCancelTextGone(label));
					}
				}
			};

			//-------------------------------------------------------
			// FileNotOnline: a data file that is OFFLINE, RECOVER or SYSOFF, or a temp file that is OFFLINE
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("FileNotOnline"))
			{
				String onlineRegex = conf.getProperty(PROPKEY_alarm_FileNotOnlineOkStatus, DEFAULT_alarm_FileNotOnlineOkStatus);

				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest(" + cm.getName() + "): FileNotOnline: file=" + fileLabel + ", okStatusRegex='" + onlineRegex + "', STATUS='" + status + "'.");

				if (StringUtil.hasValue(onlineRegex) && ! status.matches(onlineRegex))
				{
					AlarmEvent ae = new AlarmEventOraDataFileProblem(cm, "FileNotOnline", fileKey, fileLabel, fileType, status, fileError);
					ae.setAlarmDescriptionProvider(this, descriptionProvider);

					// Information about how to disable this alarm
					ae.createAlarmOptionsMessage(this, "FileNotOnline");

					AlarmHandler.getInstance().addAlarm(ae);
				}
			}

			//-------------------------------------------------------
			// FileNeedsRecovery: Oracle says that the file needs media recovery, or that something is wrong with it
			// (a tablespace that was taken offline in the normal way has FILE_ERROR = 'OFFLINE NORMAL': that is not a file that needs recovery)
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("FileNeedsRecovery") && conf.getBooleanProperty(PROPKEY_alarm_FileNeedsRecovery, DEFAULT_alarm_FileNeedsRecovery))
			{
				boolean hasFileError = StringUtil.hasValue(fileError) && ! "OFFLINE NORMAL".equals(fileError);

				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest(" + cm.getName() + "): FileNeedsRecovery: file=" + fileLabel + ", NEEDS_RECOVERY='" + needsRecovery + "', FILE_ERROR='" + fileError + "'.");

				if (("YES".equals(needsRecovery) && ! "OFFLINE NORMAL".equals(fileError)) || hasFileError)
				{
					AlarmEvent ae = new AlarmEventOraDataFileProblem(cm, "FileNeedsRecovery", fileKey, fileLabel, fileType, status, fileError);
					ae.setAlarmDescriptionProvider(this, descriptionProvider);

					// Information about how to disable this alarm
					ae.createAlarmOptionsMessage(this, "FileNeedsRecovery");

					AlarmHandler.getInstance().addAlarm(ae);
				}
			}

			//-------------------------------------------------------
			// CorruptBlocks: RMAN has found blocks that can't be read
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("CorruptBlocks"))
			{
				int threshold = conf.getIntProperty(PROPKEY_alarm_CorruptBlocks, DEFAULT_alarm_CorruptBlocks);

				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest(" + cm.getName() + "): CorruptBlocks: file=" + fileLabel + ", threshold=" + threshold + ", CORRUPT_BLOCKS='" + corruptBlocks + "'.");

				if (corruptBlocks != null && corruptBlocks > threshold)
				{
					AlarmEvent ae = new AlarmEventOraDataFileProblem(cm, fileKey, fileLabel, corruptBlocks.longValue(), threshold);
					ae.setAlarmDescriptionProvider(this, descriptionProvider);

					// Information about how to disable this alarm
					ae.createAlarmOptionsMessage(this, "CorruptBlocks");

					AlarmHandler.getInstance().addAlarm(ae);
				}
			}

			//-------------------------------------------------------
			// BackupModeInHours: BEGIN BACKUP without an END BACKUP
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("BackupModeInHours"))
			{
				double threshold = conf.getDoubleProperty(PROPKEY_alarm_BackupModeInHours, DEFAULT_alarm_BackupModeInHours);

				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest(" + cm.getName() + "): BackupModeInHours: file=" + fileLabel + ", threshold=" + threshold + ", BACKUP_MODE_HOURS='" + backupModeHours + "'.");

				if (backupModeHours != null && backupModeHours > threshold)
				{
					AlarmEvent ae = new AlarmEventOraDataFileProblem(cm, fileKey, fileLabel, cm.getAbsString(r, "BACKUP_MODE_SINCE"), backupModeHours, threshold);
					ae.setAlarmDescriptionProvider(this, descriptionProvider);

					// Information about how to disable this alarm
					ae.createAlarmOptionsMessage(this, "BackupModeInHours");

					AlarmHandler.getInstance().addAlarm(ae);
				}
			}

		} // end: loop-rows
	}

	public static final String  PROPKEY_alarm_SkipTablespaceRegex   = CM_NAME + ".alarm.system.skip.tablespaces";
	public static final String  DEFAULT_alarm_SkipTablespaceRegex   = "";

	public static final String  PROPKEY_alarm_FileNotOnlineOkStatus = CM_NAME + ".alarm.system.if.Status.notIn";
	public static final String  DEFAULT_alarm_FileNotOnlineOkStatus = "(ONLINE|SYSTEM)";

	public static final String  PROPKEY_alarm_FileNeedsRecovery     = CM_NAME + ".alarm.system.if.NeedsRecovery";
	public static final boolean DEFAULT_alarm_FileNeedsRecovery     = true;

	public static final String  PROPKEY_alarm_CorruptBlocks         = CM_NAME + ".alarm.system.if.CorruptBlocks.gt";
	public static final int     DEFAULT_alarm_CorruptBlocks         = 0;

	public static final String  PROPKEY_alarm_BackupModeInHours     = CM_NAME + ".alarm.system.if.BackupModeInHours.gt";
	public static final double  DEFAULT_alarm_BackupModeInHours     = 12;

	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;
		CmSettingsHelper.Type isPreCheck    = CmSettingsHelper.Type.IS_PRE_CHECK;

		list.add(new CmSettingsHelper("SkipTablespaceRegex", isPreCheck   , PROPKEY_alarm_SkipTablespaceRegex  , String .class, conf.getProperty       (PROPKEY_alarm_SkipTablespaceRegex  , DEFAULT_alarm_SkipTablespaceRegex  ), DEFAULT_alarm_SkipTablespaceRegex  , "Skip files where 'TABLESPACE_NAME' (in a CDB also 'container:TABLESPACE_NAME') matches this regexp. For example a tablespace that is offline on purpose.", new RegExpInputValidator()));

		list.add(new CmSettingsHelper("FileNotOnline"      , isAlarmSwitch, PROPKEY_alarm_FileNotOnlineOkStatus, String .class, conf.getProperty       (PROPKEY_alarm_FileNotOnlineOkStatus, DEFAULT_alarm_FileNotOnlineOkStatus), DEFAULT_alarm_FileNotOnlineOkStatus, "If 'STATUS' does NOT match this regexp then send 'AlarmEventOraDataFileProblem'.", new RegExpInputValidator()));
		list.add(new CmSettingsHelper("FileNeedsRecovery"  , isAlarmSwitch, PROPKEY_alarm_FileNeedsRecovery    , Boolean.class, conf.getBooleanProperty(PROPKEY_alarm_FileNeedsRecovery    , DEFAULT_alarm_FileNeedsRecovery    ), DEFAULT_alarm_FileNeedsRecovery    , "If 'NEEDS_RECOVERY' is YES, or 'FILE_ERROR' has a value (other than 'OFFLINE NORMAL'), then send 'AlarmEventOraDataFileProblem'." ));
		list.add(new CmSettingsHelper("CorruptBlocks"      , isAlarmSwitch, PROPKEY_alarm_CorruptBlocks        , Integer.class, conf.getIntProperty    (PROPKEY_alarm_CorruptBlocks        , DEFAULT_alarm_CorruptBlocks        ), DEFAULT_alarm_CorruptBlocks        , "If 'CORRUPT_BLOCKS' is greater than ## then send 'AlarmEventOraDataFileProblem'." ));
		list.add(new CmSettingsHelper("BackupModeInHours"  , isAlarmSwitch, PROPKEY_alarm_BackupModeInHours    , Double .class, conf.getDoubleProperty (PROPKEY_alarm_BackupModeInHours    , DEFAULT_alarm_BackupModeInHours    ), DEFAULT_alarm_BackupModeInHours    , "If 'BACKUP_MODE_HOURS' is greater than ##.# hours then send 'AlarmEventOraDataFileProblem'." ));

		return list;
	}
}
