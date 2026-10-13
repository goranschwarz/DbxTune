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
import com.dbxtune.alarm.events.oracle.AlarmEventOraArchiveDestError;
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
import com.dbxtune.utils.StringUtil;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmArchiveDest
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmArchiveDest.class.getSimpleName();
	public static final String   SHORT_NAME       = "Archive Dest";
	public static final String   HTML_DESC        =
		"<html>" +
		"Where the archived redo logs are written (the archive log destinations), and if it works.<br>" +
		"<br>" +
		"A destination is a local directory (<code>TARGET=PRIMARY</code>) or a standby database (<code>TARGET=STANDBY</code>, Data Guard).<br>" +
		"So this is also the primary side's view of the redo shipping to the standby databases.<br>" +
		"<br>" +
		"<b>If a <code>MANDATORY</code> destination has <code>STATUS=ERROR</code>, the online redo logs can't be reused, and the database 'hangs' when all of them are full.</b><br>" +
		"<code>ERROR</code>, <code>FAIL_DATE</code> and <code>FAILURE_COUNT</code> shows what the problem is.<br>" +
		"<br>" +
		"<b>Note</b>: After a failure Oracle waits <code>REOPEN_SECS</code> seconds (default 300), then it sets <code>STATUS</code> back to VALID and clears the error columns, until the next attempt (the next log switch) fails.<br>" +
		"So for a destination that is permanently broken, the error (and the alarm) comes and goes with every log switch.<br>" +
		"<b>Note</b>: Destinations that are not used (<code>STATUS=INACTIVE</code>) are not shown.<br>" +
		"<b>Note</b>: If the database is in NOARCHIVELOG mode, a destination is shown, but nothing is written to it.<br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_DISK;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {CM_NAME, "V$ARCHIVE_DEST", "V$ARCHIVE_DEST_STATUS"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
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

		return new CmArchiveDest(counterController, guiController);
	}

	public CmArchiveDest(ICounterController counterController, IGuiController guiController)
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

		pkCols.add("DEST_ID");

		return pkCols;
	}

	/** If we can't read the views: disable the CM, and tell what to do about it */
	@Override
	public boolean checkDependsOnOther(DbxConnection conn)
	{
		return OracleCmUtils.checkViewsAreReadable(this, conn, "V$ARCHIVE_DEST", "V$ARCHIVE_DEST_STATUS");
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		// One row for every destination that is in use. This looks the same in all versions (and in a CDB the destinations belongs to the whole CDB)
		String sql = ""
			+ "select \n"
			+ "     d.DEST_ID \n"
			+ "    ,d.DEST_NAME \n"
			+ "    ,d.STATUS \n"
			+ "    ,s.TYPE \n"
			+ "    ,d.TARGET \n"
			+ "    ,d.BINDING \n"
			+ "    ,d.DESTINATION \n"
			+ "    ,d.DB_UNIQUE_NAME \n"
			+ "    ,d.VALID_NOW \n"
			+ "    ,d.VALID_TYPE \n"
			+ "    ,d.VALID_ROLE \n"
			+ "    ,d.ARCHIVER \n"
			+ "    ,d.TRANSMIT_MODE \n"
			+ "    ,d.AFFIRM \n"
			+ "    ,d.LOG_SEQUENCE \n"
			+ "    ,s.ARCHIVED_THREAD# \n"
			+ "    ,s.ARCHIVED_SEQ# \n"
			+ "    ,s.APPLIED_THREAD# \n"
			+ "    ,s.APPLIED_SEQ# \n"
			+ "    ,s.GAP_STATUS \n"
			+ "    ,s.DATABASE_MODE \n"
			+ "    ,s.RECOVERY_MODE \n"
			+ "    ,s.PROTECTION_MODE \n"
			+ "    ,s.SYNCHRONIZATION_STATUS \n"
			+ "    ,d.FAIL_DATE \n"
			+ "    ,d.FAIL_SEQUENCE \n"
			+ "    ,d.FAILURE_COUNT \n"
			+ "    ,d.MAX_FAILURE \n"
			+ "    ,d.REOPEN_SECS \n"
			+ "    ,d.DELAY_MINS \n"
			+ "    ,d.ERROR \n"
			+ "from V$ARCHIVE_DEST d \n"
			+ "left join V$ARCHIVE_DEST_STATUS s on s.DEST_ID = d.DEST_ID \n"
			+ "where d.STATUS <> 'INACTIVE' \n"
			+ "order by d.DEST_ID \n"
			+ "";

		return sql;
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			mtd.addTable(CM_NAME, "Where the archived redo logs are written (the archive log destinations), and if it works.");

			mtd.addColumn(CM_NAME, "DEST_ID",                "<html>Destination number (1-31). This is the parameter: <code>log_archive_dest_<i>n</i></code></html>");
			mtd.addColumn(CM_NAME, "STATUS",                 "<html>State of the destination."
			                                                     + "<ul>"
			                                                     + "<li><b>VALID</b>     - It works.</li>"
			                                                     + "<li><b>ERROR</b>     - The last attempt to write to the destination failed. See column ERROR for the reason. After REOPEN_SECS seconds Oracle sets it back to VALID, until the next attempt.</li>"
			                                                     + "<li><b>DISABLED</b>  - Oracle has stopped using the destination after an error.</li>"
			                                                     + "<li><b>FULL</b>      - The quota of the destination is used up.</li>"
			                                                     + "<li><b>BAD PARAM</b> - The parameter has an error.</li>"
			                                                     + "<li><b>DEFERRED</b>  - Manually (and temporarily) switched off by a DBA.</li>"
			                                                     + "<li><b>ALTERNATE</b> - An alternate destination, that is used if another destination fails.</li>"
			                                                     + "</ul></html>");
			mtd.addColumn(CM_NAME, "TYPE",                   "<html>Type of destination: LOCAL (a directory), PHYSICAL / LOGICAL / SNAPSHOT (a standby database of that type)...</html>");
			mtd.addColumn(CM_NAME, "TARGET",                 "<html>PRIMARY = a local directory, STANDBY = a remote (standby) database.</html>");
			mtd.addColumn(CM_NAME, "BINDING",                "<html>MANDATORY = archiving to this destination MUST succeed before an online redo log can be reused (if it fails, the database will 'hang').<br>OPTIONAL = an online redo log can be reused even if archiving to this destination failed.</html>");
			mtd.addColumn(CM_NAME, "DESTINATION",            "<html>Directory or service name. <code>USE_DB_RECOVERY_FILE_DEST</code> means the Fast Recovery Area (see the tab 'Recovery Area').</html>");
			mtd.addColumn(CM_NAME, "LOG_SEQUENCE",           "<html>Sequence number of the last redo log that was archived to this destination.</html>");
			mtd.addColumn(CM_NAME, "ARCHIVED_SEQ#",          "<html>Sequence number of the last redo log that was archived (received) at this destination.</html>");
			mtd.addColumn(CM_NAME, "APPLIED_SEQ#",           "<html>Sequence number of the last redo log that was applied on the standby database (0 for a local destination).<br>If this is far behind ARCHIVED_SEQ#, the standby is not applying redo.</html>");
			mtd.addColumn(CM_NAME, "GAP_STATUS",             "<html>For a standby destination: is any redo missing at the standby. 'NO GAP' is the good value.</html>");
			mtd.addColumn(CM_NAME, "FAIL_DATE",              "<html>When the last error happened.</html>");
			mtd.addColumn(CM_NAME, "FAIL_SEQUENCE",          "<html>Sequence number of the redo log that was archived when the last error happened.</html>");
			mtd.addColumn(CM_NAME, "FAILURE_COUNT",          "<html>Number of failures in a row.</html>");
			mtd.addColumn(CM_NAME, "REOPEN_SECS",            "<html>How many seconds Oracle waits after an error, before it tries the destination again.</html>");
			mtd.addColumn(CM_NAME, "ERROR",                  "<html>The error message from the last failure.</html>");
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

		//-------------------------------------------------------
		// Status
		//-------------------------------------------------------
		if (isSystemAlarmsForColumnEnabledAndInTimeRange("Status"))
		{
			String statusRegex = conf.getProperty(PROPKEY_alarm_StatusIn, DEFAULT_alarm_StatusIn);

			for (int r=0; r<cm.getAbsRowCount(); r++)
			{
				String destName = cm.getAbsString(r, "DEST_NAME");
				String status   = cm.getAbsString(r, "STATUS");

				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest(" + cm.getName() + "): Status: destName='" + destName + "', statusRegex='" + statusRegex + "', STATUS='" + status + "'.");

				if (StringUtil.hasValue(statusRegex) && status != null && status.matches(statusRegex))
				{
					String binding      = cm.getAbsString(r, "BINDING");
					String target       = cm.getAbsString(r, "TARGET");
					String destination  = cm.getAbsString(r, "DESTINATION");
					String error        = cm.getAbsString(r, "ERROR");
					String failDate     = cm.getAbsString(r, "FAIL_DATE");
					int    failureCount = cm.getAbsValueAsInteger(r, "FAILURE_COUNT", 0);

					AlarmEvent ae = new AlarmEventOraArchiveDestError(cm, destName, status, binding, target, destination, error, failDate, failureCount, statusRegex);

					// The below is called from AlarmHandler on: Raise, RE-RAISE & CANCEL
					ae.setAlarmDescriptionProvider(this, new AlarmDescriptionProviderBase(cm.getPk(), cm.getAbsPkValue(r), null)
					{
						@Override
						public void setValues(CountersModel cm, AlarmEvent alarmEvent, AlarmPhase phase)
						{
							int    rowId = cm.getAbsRowIdForPkValue(getRowPk());
							String label = "Archive destination '" + destName + "'";

							if (rowId != -1)
							{
								// Always: (RAISE, RE-RAISE, CANCEL) - Set the row as a table
								String extendedDescText = cm.toTextTableString(DATA_ABS, rowId);
								String extendedDescHtml = cm.toHtmlTableString(DATA_ABS, rowId, true, false, false);
								alarmEvent.setExtendedDescription(extendedDescText, extendedDescHtml);

								// Set CANCEL message (values that was found AFTER last raise/re-raise event)
								if (AlarmPhase.CANCEL.equals(phase))
									alarmEvent.setCancelDescription(getAlarmCancelText(label + ": STATUS", "'" + cm.getAbsString(rowId, "STATUS") + "'"));
							}
							else
							{
								// The row is gone: the destination is no longer in use (STATUS=INACTIVE)
								if (AlarmPhase.CANCEL.equals(phase))
									alarmEvent.setCancelDescription(getAlarmCancelTextGone(label) + " (it is no longer in use)");
							}
						}
					});

					// Information about how to disable this alarm
					ae.createAlarmOptionsMessage(this, "Status");

					AlarmHandler.getInstance().addAlarm(ae);
				}
			} // end: loop-rows
		} // end: Status
	}

	public static final String  PROPKEY_alarm_StatusIn = CM_NAME + ".alarm.system.if.Status.in";
	public static final String  DEFAULT_alarm_StatusIn = "(ERROR|DISABLED|FULL|BAD PARAM)";

	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;

		list.add(new CmSettingsHelper("Status", isAlarmSwitch, PROPKEY_alarm_StatusIn, String.class, conf.getProperty(PROPKEY_alarm_StatusIn, DEFAULT_alarm_StatusIn), DEFAULT_alarm_StatusIn, "If 'STATUS' matches this regexp then send 'AlarmEventOraArchiveDestError' (severity ERROR if the destination is MANDATORY, otherwise WARNING).", new RegExpInputValidator()));

		return list;
	}
}
