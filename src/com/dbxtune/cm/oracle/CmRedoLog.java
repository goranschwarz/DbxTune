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
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.naming.NameNotFoundException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.alarm.AlarmHandler;
import com.dbxtune.alarm.events.AlarmDescriptionProviderBase;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.alarm.events.oracle.AlarmEventOraRedoLogProblem;
import com.dbxtune.central.pcs.CentralPersistReader;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.config.dict.MonTablesDictionary;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.graph.TrendGraphDataPoint;
import com.dbxtune.graph.TrendGraphDataPoint.LabelType;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.utils.Configuration;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmRedoLog
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmRedoLog.class.getSimpleName();
	public static final String   SHORT_NAME       = "Redo Log";
	public static final String   HTML_DESC        =
		"<html>" +
		"The online redo log groups: size, state, if they are archived, and how often Oracle switches from one group to the next.<br>" +
		"<br>" +
		"Every change is first written to the CURRENT group. When it is full Oracle switches to the next group, and (in ARCHIVELOG mode) the full one is copied by the archiver.<br>" +
		"A group can only be reused when it is checkpointed (<code>STATUS=INACTIVE</code>) and archived (<code>ARCHIVED=YES</code>).<br>" +
		"<ul>" +
		"  <li><b>Many switches per hour</b> (a common rule of thumb: more than 4-6) means that the groups are too small for the load.</li>" +
		"  <li><b>All groups except the current one waiting to be archived</b> means that the archiver is stuck: at the next switch the database 'hangs'.</li>" +
		"  <li><b>An INVALID member</b> is a redo log file that Oracle can not use.</li>" +
		"</ul>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_DISK;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {CM_NAME, "V$LOG", "V$LOGFILE", "V$LOG_HISTORY"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;

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

		return new CmRedoLog(counterController, guiController);
	}

	public CmRedoLog(ICounterController counterController, IGuiController guiController)
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

		addTrendGraphs();

		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	public static final String GRAPH_NAME_SWITCHES_PER_HOUR = "RedoSwitchesPerHour";

	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_SWITCHES_PER_HOUR,
			"Redo Log, Switches in the Last Hour", 	                // Menu CheckBox text
			"Redo Log Switches in the Last Hour (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_COUNT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] {"Log Switches Last Hour"},
			LabelType.Static,
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("THREAD#");
		pkCols.add("GROUP#");

		return pkCols;
	}

	/** If we can't read the views: disable the CM, and tell what to do about it */
	@Override
	public boolean checkDependsOnOther(DbxConnection conn)
	{
		return OracleCmUtils.checkViewsAreReadable(this, conn, "V$LOG", "V$LOGFILE", "V$LOG_HISTORY", "V$DATABASE");
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		// One row for every online redo log group. This looks the same in all versions (and in a CDB the redo logs belongs to the whole CDB)
		// The log switches are counted per thread (in a RAC every instance has it's own thread), so that number is the same on all groups of a thread
		// Note: decode(..., 0) the "else 0" is needed. Without it the aggregate skips NULL values, and Oracle sends a warning with the result (which is treated as a failed sample)
		String sql = ""
			+ "select \n"
			+ "     l.THREAD# \n"
			+ "    ,l.GROUP# \n"
			+ "    ,l.SEQUENCE# \n"
			+ "    ,l.STATUS \n"
			+ "    ,l.ARCHIVED \n"
			+ "    ,CAST(l.BYTES / 1048576 as NUMBER(12,1)) as SIZE_MB \n"
			+ "    ,l.MEMBERS \n"
			+ "    ,nvl(f.INVALID_MEMBERS, 0)    as INVALID_MEMBERS \n"
			+ "    ,f.FIRST_MEMBER \n"
			+ "    ,l.FIRST_TIME \n"
			+ "    ,l.NEXT_TIME \n"
			+ "    ,CAST(case when l.ARCHIVED = 'NO' and l.STATUS not in ('CURRENT', 'CLEARING_CURRENT', 'UNUSED') then (SYSDATE - l.NEXT_TIME) * 86400 else 0 end as NUMBER(12,0)) as WAITING_FOR_ARCHIVE_SEC \n"
			+ "    ,nvl(h.SWITCHES_LAST_HOUR, 0) as SWITCHES_LAST_HOUR \n"
			+ "    ,nvl(h.SWITCHES_LAST_24H,  0) as SWITCHES_LAST_24H \n"
			+ "    ,d.LOG_MODE \n"
			+ "from V$LOG l \n"
			+ "left join (select GROUP# \n"
			+ "                 ,sum(decode(STATUS, 'INVALID', 1, 0)) as INVALID_MEMBERS \n"
			+ "                 ,min(MEMBER)                          as FIRST_MEMBER \n"
			+ "           from V$LOGFILE \n"
			+ "           where TYPE = 'ONLINE' \n"
			+ "           group by GROUP#) f on f.GROUP# = l.GROUP# \n"
			+ "left join (select THREAD# \n"
			+ "                 ,sum(case when FIRST_TIME > SYSDATE - 1/24 then 1 else 0 end) as SWITCHES_LAST_HOUR \n"
			+ "                 ,count(*)                                                     as SWITCHES_LAST_24H \n"
			+ "           from V$LOG_HISTORY \n"
			+ "           where FIRST_TIME > SYSDATE - 1 \n"
			+ "           group by THREAD#) h on h.THREAD# = l.THREAD# \n"
			+ "cross join (select LOG_MODE from V$DATABASE) d \n"
			+ "order by l.THREAD#, l.GROUP# \n"
			+ "";

		return sql;
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			mtd.addTable(CM_NAME, "The online redo log groups: size, state, if they are archived, and how often Oracle switches from one group to the next.");

			mtd.addColumn(CM_NAME, "THREAD#",            "<html>Redo thread. A single instance database has one thread (1), in a RAC every instance has it's own.</html>");
			mtd.addColumn(CM_NAME, "GROUP#",             "<html>Redo log group number.</html>");
			mtd.addColumn(CM_NAME, "SEQUENCE#",          "<html>Log sequence number of what the group holds right now.</html>");
			mtd.addColumn(CM_NAME, "STATUS",             "<html>State of the group."
			                                                 + "<ul>"
			                                                 + "<li><b>CURRENT</b>  - The group Oracle writes to right now.</li>"
			                                                 + "<li><b>ACTIVE</b>   - Not written to, but still needed for crash recovery (the checkpoint is not finished).</li>"
			                                                 + "<li><b>INACTIVE</b> - Not needed for crash recovery. It can be reused (in ARCHIVELOG mode: when it also is archived).</li>"
			                                                 + "<li><b>UNUSED</b>   - Never written to (a new group, or just after 'open resetlogs').</li>"
			                                                 + "<li><b>CLEARING / CLEARING_CURRENT</b> - Is being cleared.</li>"
			                                                 + "</ul></html>");
			mtd.addColumn(CM_NAME, "ARCHIVED",           "<html>YES = the group has been copied by the archiver (or the database is in NOARCHIVELOG mode). NO = it is waiting to be archived (the CURRENT group is always NO).</html>");
			mtd.addColumn(CM_NAME, "SIZE_MB",            "<html>Size of the group in MB.</html>");
			mtd.addColumn(CM_NAME, "MEMBERS",            "<html>Number of files (copies) in the group. More than one protects against the loss of a file.</html>");
			mtd.addColumn(CM_NAME, "INVALID_MEMBERS",    "<html>Number of files in the group that Oracle can not use (V$LOGFILE.STATUS = 'INVALID').<br><b>Note</b>: a file that was just added is INVALID until the group has been used once.</html>");
			mtd.addColumn(CM_NAME, "FIRST_MEMBER",       "<html>File name of the first member in the group.</html>");
			mtd.addColumn(CM_NAME, "FIRST_TIME",         "<html>When Oracle started to write to what the group holds right now.</html>");
			mtd.addColumn(CM_NAME, "NEXT_TIME",          "<html>When Oracle switched away from the group (when it stopped being the CURRENT one). NULL for the CURRENT group.</html>");
			mtd.addColumn(CM_NAME, "WAITING_FOR_ARCHIVE_SEC", "<html>For a group that is waiting to be archived (ARCHIVED = NO, and not the CURRENT group): how many seconds it has waited. Otherwise 0.<br>Normally this is a few seconds. If it keeps growing, the archiver is stuck.<br><b>Formula</b>: (SYSDATE - NEXT_TIME) * 86400</html>");
			mtd.addColumn(CM_NAME, "SWITCHES_LAST_HOUR","<html>Number of log switches in this thread the last hour (the same value on all groups of the thread).</html>");
			mtd.addColumn(CM_NAME, "SWITCHES_LAST_24H",  "<html>Number of log switches in this thread the last 24 hours (the same value on all groups of the thread).</html>");
			mtd.addColumn(CM_NAME, "LOG_MODE",           "<html>ARCHIVELOG or NOARCHIVELOG (the same value on all rows).</html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e);
		}
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		if (GRAPH_NAME_SWITCHES_PER_HOUR.equals(tgdp.getName()))
		{
			if (this.size() == 0)
				return;

			// The number is per thread (and repeated on every group of the thread): so add it once for every thread
			Set<Integer> threads = new LinkedHashSet<>();
			double switches = 0;
			for (int r = 0; r < this.size(); r++)
			{
				if (threads.add(this.getAbsValueAsInteger(r, "THREAD#", 0)))
					switches += this.getAbsValueAsDouble(r, "SWITCHES_LAST_HOUR", 0d);
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), new Double[] {switches});
		}
	}

	/** For one thread: how many groups are there, how many are NOT the current one, how many of those are waiting to be archived, and the shortest time one of them has waited. @return {groups, nonCurrent, waitingForArchive, minWaitingSec} */
	private static int[] getArchiveState(CountersModel cm, int thread)
	{
		int groups = 0, nonCurrent = 0, waitingForArchive = 0, minWaitingSec = Integer.MAX_VALUE;

		for (int r=0; r<cm.getAbsRowCount(); r++)
		{
			if (cm.getAbsValueAsInteger(r, "THREAD#", 0) != thread)
				continue;

			String status = cm.getAbsString(r, "STATUS");
			groups++;

			if ("CURRENT".equals(status) || "CLEARING_CURRENT".equals(status))
				continue;
			nonCurrent++;

			if ("NO".equals(cm.getAbsString(r, "ARCHIVED")) && ! "UNUSED".equals(status))
			{
				waitingForArchive++;
				minWaitingSec = Math.min(minWaitingSec, cm.getAbsValueAsInteger(r, "WAITING_FOR_ARCHIVE_SEC", 0));
			}
		}
		return new int[] {groups, nonCurrent, waitingForArchive, waitingForArchive == 0 ? 0 : minWaitingSec};
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
		// LogMemberInvalid
		//-------------------------------------------------------
		if (isSystemAlarmsForColumnEnabledAndInTimeRange("LogMemberInvalid") && conf.getBooleanProperty(PROPKEY_alarm_LogMemberInvalid, DEFAULT_alarm_LogMemberInvalid))
		{
			for (int r=0; r<cm.getAbsRowCount(); r++)
			{
				int invalidMembers = cm.getAbsValueAsInteger(r, "INVALID_MEMBERS", 0);

				if (invalidMembers > 0)
				{
					int thread = cm.getAbsValueAsInteger(r, "THREAD#", 0);
					int group  = cm.getAbsValueAsInteger(r, "GROUP#",  0);

					if (debugPrint || _logger.isDebugEnabled())
						System.out.println("##### sendAlarmRequest(" + cm.getName() + "): LogMemberInvalid: thread=" + thread + ", group=" + group + ", INVALID_MEMBERS=" + invalidMembers + ".");

					AlarmEvent ae = new AlarmEventOraRedoLogProblem(cm, thread, group, invalidMembers, cm.getAbsValueAsInteger(r, "MEMBERS", 0), cm.getAbsString(r, "FIRST_MEMBER"));

					// The below is called from AlarmHandler on: Raise, RE-RAISE & CANCEL
					ae.setAlarmDescriptionProvider(this, new AlarmDescriptionProviderBase(cm.getPk(), cm.getAbsPkValue(r), null)
					{
						@Override
						public void setValues(CountersModel cm, AlarmEvent alarmEvent, AlarmPhase phase)
						{
							int    rowId = cm.getAbsRowIdForPkValue(getRowPk());
							String label = "Redo log group " + group + " (thread " + thread + ")";

							if (rowId != -1)
							{
								// Always: (RAISE, RE-RAISE, CANCEL) - Set the row as a table
								String extendedDescText = cm.toTextTableString(DATA_ABS, rowId);
								String extendedDescHtml = cm.toHtmlTableString(DATA_ABS, rowId, true, false, false);
								alarmEvent.setExtendedDescription(extendedDescText, extendedDescHtml);

								// Set CANCEL message (values that was found AFTER last raise/re-raise event)
								if (AlarmPhase.CANCEL.equals(phase))
									alarmEvent.setCancelDescription(getAlarmCancelText(label + ": the number of INVALID members", cm.getAbsValueAsInteger(rowId, "INVALID_MEMBERS", 0)));
							}
							else
							{
								if (AlarmPhase.CANCEL.equals(phase))
									alarmEvent.setCancelDescription(getAlarmCancelTextGone(label));
							}
						}
					});

					// Information about how to disable this alarm
					ae.createAlarmOptionsMessage(this, "LogMemberInvalid");

					AlarmHandler.getInstance().addAlarm(ae);
				}
			} // end: loop-rows
		} // end: LogMemberInvalid

		//-------------------------------------------------------
		// LogsWaitingForArchive: in ARCHIVELOG mode, when ALL groups (except the current one) have been waiting to be archived for more than ## seconds
		// (just after a log switch a group is waiting for a few seconds, which is normal: so they must ALL have waited for a while)
		//-------------------------------------------------------
		if (isSystemAlarmsForColumnEnabledAndInTimeRange("LogsWaitingForArchive"))
		{
			int threshold = conf.getIntProperty(PROPKEY_alarm_LogsWaitingForArchive, DEFAULT_alarm_LogsWaitingForArchive);

			Set<Integer> threads = new LinkedHashSet<>();
			for (int r=0; r<cm.getAbsRowCount(); r++)
			{
				if ("ARCHIVELOG".equals(cm.getAbsString(r, "LOG_MODE")))
					threads.add(cm.getAbsValueAsInteger(r, "THREAD#", 0));
			}

			for (int thread : threads)
			{
				int[] state             = getArchiveState(cm, thread);
				int   groups            = state[0];
				int   nonCurrent        = state[1];
				int   waitingForArchive = state[2];
				int   minWaitingSec     = state[3];

				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest(" + cm.getName() + "): LogsWaitingForArchive: thread=" + thread + ", threshold=" + threshold + ", groups=" + groups + ", nonCurrent=" + nonCurrent + ", waitingForArchive=" + waitingForArchive + ", minWaitingSec=" + minWaitingSec + ".");

				if (nonCurrent > 0 && waitingForArchive >= nonCurrent && minWaitingSec > threshold)
				{
					AlarmEvent ae = new AlarmEventOraRedoLogProblem(cm, thread, waitingForArchive, groups, minWaitingSec, threshold);

					// The below is called from AlarmHandler on: Raise, RE-RAISE & CANCEL
					ae.setAlarmDescriptionProvider(this, new AlarmDescriptionProviderBase(null, null, threshold)
					{
						@Override
						public void setValues(CountersModel cm, AlarmEvent alarmEvent, AlarmPhase phase)
						{
							// Set CANCEL message (values that was found AFTER last raise/re-raise event)
							if (AlarmPhase.CANCEL.equals(phase))
							{
								int[] now = getArchiveState(cm, thread);
								alarmEvent.setCancelDescription("Thread " + thread + ": redo log groups waiting to be archived is now " + now[2] + " of " + now[0] + " groups (the shortest wait is " + now[3] + " seconds, threshold " + getThreshold() + " seconds)");
							}
						}
					});

					// Information about how to disable this alarm
					ae.createAlarmOptionsMessage(this, "LogsWaitingForArchive");

					AlarmHandler.getInstance().addAlarm(ae);
				}
			}
		} // end: LogsWaitingForArchive
	}

	public static final String  PROPKEY_alarm_LogMemberInvalid      = CM_NAME + ".alarm.system.if.LogMember.is.INVALID";
	public static final boolean DEFAULT_alarm_LogMemberInvalid      = true;

	public static final String  PROPKEY_alarm_LogsWaitingForArchive = CM_NAME + ".alarm.system.if.AllLogsWaitingForArchiveInSec.gt";
	public static final int     DEFAULT_alarm_LogsWaitingForArchive = 60;

	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;

		list.add(new CmSettingsHelper("LogMemberInvalid"     , isAlarmSwitch, PROPKEY_alarm_LogMemberInvalid     , Boolean.class, conf.getBooleanProperty(PROPKEY_alarm_LogMemberInvalid     , DEFAULT_alarm_LogMemberInvalid     ), DEFAULT_alarm_LogMemberInvalid     , "If 'INVALID_MEMBERS' is above 0 (a redo log file that Oracle can not use) then send 'AlarmEventOraRedoLogProblem'. Severity ERROR if all members of the group are invalid, otherwise WARNING." ));
		list.add(new CmSettingsHelper("LogsWaitingForArchive", isAlarmSwitch, PROPKEY_alarm_LogsWaitingForArchive, Integer.class, conf.getIntProperty    (PROPKEY_alarm_LogsWaitingForArchive, DEFAULT_alarm_LogsWaitingForArchive), DEFAULT_alarm_LogsWaitingForArchive, "In ARCHIVELOG mode: if ALL redo log groups except the current one have been waiting to be archived for more than ## seconds (the archiver is stuck) then send 'AlarmEventOraRedoLogProblem'." ));

		return list;
	}
}
