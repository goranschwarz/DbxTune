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
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.naming.NameNotFoundException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.alarm.AlarmHandler;
import com.dbxtune.alarm.events.AlarmDescriptionProviderBase;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.alarm.events.AlarmEventConfigResourceIsLow;
import com.dbxtune.cm.CmSettingsHelper;
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
public class CmOpenCursors
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmOpenCursors.class.getSimpleName();
	public static final String   SHORT_NAME       = "Open Cursors";
	public static final String   HTML_DESC        =
		"<html>" +
		"The sessions that has most cursors open, and how close they are to the limit (the parameter <code>open_cursors</code>).<br>" +
		"<br>" +
		"A session that reaches the limit gets <code>ORA-01000: maximum open cursors exceeded</code> on it's next statement.<br>" +
		"That is almost never a limit that is too low: it is an application that opens statements (or result sets) and does not close them. " +
		"Such a session climbs slowly, and then fails after hours or days. This tab shows it while it climbs.<br>" +
		"<br>" +
		"By default: the 20 sessions with most open cursors. This can be changed in the options.<br>" +
		"<b>Tip</b>: To see WHICH statements a session has open: <code>select SQL_ID, count(*), max(SQL_TEXT) from V$OPEN_CURSOR where SID = ... group by SQL_ID order by 2 desc</code><br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {CM_NAME, "V$SESSTAT", "V$SESSION"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 60;
	public static final int      DEFAULT_QUERY_TIMEOUT          = 10;

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

		return new CmOpenCursors(counterController, guiController);
	}

	public CmOpenCursors(ICounterController counterController, IGuiController guiController)
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
	public static final String  PROPKEY_sample_topRows = CM_NAME + ".sample.topRows";
	public static final int     DEFAULT_sample_topRows = 20;

	@Override
	protected void registerDefaultValues()
	{
		super.registerDefaultValues();

		Configuration.registerDefaultValue(PROPKEY_sample_topRows, DEFAULT_sample_topRows);
	}

	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public List<CmSettingsHelper> getLocalSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		list.add(new CmSettingsHelper("Max Number of Rows", PROPKEY_sample_topRows, Integer.class, conf.getIntProperty(PROPKEY_sample_topRows, DEFAULT_sample_topRows), DEFAULT_sample_topRows, "Only get the ## sessions that has most cursors open." ));

		return list;
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		// SID is unique at any point in time, but it is reused when a session ends. SERIAL# makes it a "new" row
		pkCols.add("SID");
		pkCols.add("SERIAL#");

		return pkCols;
	}

	/** If we can't read the views: disable the CM, and tell what to do about it */
	@Override
	public boolean checkDependsOnOther(DbxConnection conn)
	{
		return OracleCmUtils.checkViewsAreReadable(this, conn, "V$SESSTAT", "V$STATNAME", "V$SESSION", "V$SYSTEM_PARAMETER");
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		DbmsVersionInfoOracle oraVersionInfo = (DbmsVersionInfoOracle) versionInfo;

		int topRows = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_sample_topRows, DEFAULT_sample_topRows);

		// 12.1 and above: the V$ views has CON_ID, and a PDB can have it's own value for 'open_cursors'
		// Connected to CDB$ROOT we see the sessions of ALL containers, so there we show which container it is
		boolean hasConId  = versionInfo.getLongVersion() >= Ver.ver(12,1);
		boolean isCdbRoot = hasConId && oraVersionInfo.isCdbRoot();

		// The limit for a session: the value that it's PDB has set, otherwise the value of the instance (CON_ID 0)
		String limit = hasConId
			? "NVL((select max(to_number(p.VALUE)) from V$SYSTEM_PARAMETER p where p.NAME = 'open_cursors' and p.CON_ID = x.CON_ID and p.CON_ID <> 0) \n"
			+ "           ,(select max(to_number(p.VALUE)) from V$SYSTEM_PARAMETER p where p.NAME = 'open_cursors' and p.CON_ID = 0))"
			: "(select max(to_number(p.VALUE)) from V$SYSTEM_PARAMETER p where p.NAME = 'open_cursors')";

		// x: the sessions sorted on open cursors, and then the first ## rows of that
		//    V$SESSTAT has one row per session and statistic: only the one statistic is read
		// y: adds the limit (for those rows only)
		// Note: CAST(... as NUMBER(5,1)) is needed to keep decimals (a NUMBER without precision is treated as a BIGINT)
		String sql = ""
			+ "select \n"
			+ "     " + (isCdbRoot ? "y.CON_ID" : "CAST(0    as NUMBER(10))")    + " as CON_ID \n"
			+ "    ," + (isCdbRoot ? "c.NAME"   : "CAST(NULL as VARCHAR2(128))") + " as CON_NAME \n"
			+ "    ,y.SID \n"
			+ "    ,y.SERIAL# \n"
			+ "    ,y.USERNAME \n"
			+ "    ,y.OPEN_CURSORS \n"
			+ "    ,y.OPEN_CURSORS_LIMIT \n"
			+ "    ,CAST(y.OPEN_CURSORS * 100 / NULLIF(y.OPEN_CURSORS_LIMIT, 0) as NUMBER(5,1)) as USED_PCT \n"
			+ "    ,y.STATUS \n"
			+ "    ,y.TYPE \n"
			+ "    ,y.OSUSER \n"
			+ "    ,y.MACHINE \n"
			+ "    ,y.PROGRAM \n"
			+ "    ,y.MODULE \n"
			+ "    ,y.LOGON_TIME \n"
			+ "from (select x.* \n"
			+ "            ," + limit + " as OPEN_CURSORS_LIMIT \n"
			+ "      from (select * from (select " + (hasConId ? "s.CON_ID, " : "") + "s.SID, s.SERIAL#, s.USERNAME, st.VALUE as OPEN_CURSORS, s.STATUS, s.TYPE, s.OSUSER, s.MACHINE, s.PROGRAM, s.MODULE, s.LOGON_TIME \n"
			+ "                           from V$SESSTAT st \n"
			+ "                           join V$SESSION s on s.SID = st.SID \n"
			+ "                           where st.STATISTIC# = (select STATISTIC# from V$STATNAME where NAME = 'opened cursors current') \n"
			+ "                           order by st.VALUE desc) \n"
			+ "            where ROWNUM <= " + topRows + ") x \n"
			+ "     ) y \n"
			+ (isCdbRoot ? "left join V$CONTAINERS c on c.CON_ID = y.CON_ID \n" : "")
			+ "order by y.OPEN_CURSORS desc \n"
			+ "";

		return sql;
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			mtd.addTable(CM_NAME, "The sessions that has most cursors open, and how close they are to the limit (the parameter open_cursors).");

			mtd.addColumn(CM_NAME, "CON_ID",             "<html>Container ID. 0 = This is not a Container Database (or the version is below 12.1), or we are connected to a PDB.</html>");
			mtd.addColumn(CM_NAME, "CON_NAME",           "<html>Name of the Container (PDB) the session is in. NULL if CON_ID is 0.</html>");
			mtd.addColumn(CM_NAME, "SID",                "<html>Session identifier.</html>");
			mtd.addColumn(CM_NAME, "SERIAL#",            "<html>Session serial number. SID is reused when a session ends, SID + SERIAL# is one session.</html>");
			mtd.addColumn(CM_NAME, "USERNAME",           "<html>The Oracle user. Empty for a background process.</html>");
			mtd.addColumn(CM_NAME, "OPEN_CURSORS",       "<html>Number of cursors that the session has open right now.<br><b>Formula</b>: V$SESSTAT for the statistic 'opened cursors current'</html>");
			mtd.addColumn(CM_NAME, "OPEN_CURSORS_LIMIT", "<html>The limit for ONE session: the parameter 'open_cursors'. In a Container Database: the value of the PDB that the session is in, if the PDB has it's own.</html>");
			mtd.addColumn(CM_NAME, "USED_PCT",           "<html>How close the session is to the limit, in percent. At 100 it gets ORA-01000 on it's next statement.<br><b>Formula</b>: OPEN_CURSORS * 100 / OPEN_CURSORS_LIMIT</html>");
			mtd.addColumn(CM_NAME, "STATUS",             "<html>ACTIVE (it is executing something), INACTIVE (it is waiting for the client), KILLED...</html>");
			mtd.addColumn(CM_NAME, "TYPE",               "<html>USER or BACKGROUND.</html>");
			mtd.addColumn(CM_NAME, "OSUSER",             "<html>The operating system user of the client.</html>");
			mtd.addColumn(CM_NAME, "MACHINE",            "<html>The host name of the client.</html>");
			mtd.addColumn(CM_NAME, "PROGRAM",            "<html>The program name of the client.</html>");
			mtd.addColumn(CM_NAME, "MODULE",             "<html>The module name that the application has set (DBMS_APPLICATION_INFO).</html>");
			mtd.addColumn(CM_NAME, "LOGON_TIME",         "<html>When the session logged on. A session that leaks cursors has normally been connected for a long time.</html>");
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
		// UsedPct: a session is close to the limit of 'open_cursors'
		// ONE alarm per container (not one per session): for the session that is closest to it's limit
		//-------------------------------------------------------
		if (isSystemAlarmsForColumnEnabledAndInTimeRange("UsedPct"))
		{
			double threshold = conf.getDoubleProperty(PROPKEY_alarm_UsedPct, DEFAULT_alarm_UsedPct);

			// container name -> the row with the highest USED_PCT in that container ("" when it is not a CDB)
			Map<String, Integer> worstRowPerContainer = new LinkedHashMap<>();
			for (int r=0; r<cm.getAbsRowCount(); r++)
			{
				Double usedPct = cm.getAbsValueAsDouble(r, "USED_PCT");
				if (usedPct == null)
					continue;

				String  conName  = cm.getAbsString(r, "CON_NAME") == null ? "" : cm.getAbsString(r, "CON_NAME");
				Integer worstRow = worstRowPerContainer.get(conName);

				if (worstRow == null || usedPct > cm.getAbsValueAsDouble(worstRow, "USED_PCT"))
					worstRowPerContainer.put(conName, r);
			}

			for (String conName : worstRowPerContainer.keySet())
			{
				int    r           = worstRowPerContainer.get(conName);
				Double usedPct     = cm.getAbsValueAsDouble(r, "USED_PCT");
				Double openCursors = cm.getAbsValueAsDouble(r, "OPEN_CURSORS");
				Double limit       = cm.getAbsValueAsDouble(r, "OPEN_CURSORS_LIMIT");
				String sid         = cm.getAbsString(r, "SID");
				String username    = cm.getAbsString(r, "USERNAME");
				String program     = cm.getAbsString(r, "PROGRAM");
				String machine     = cm.getAbsString(r, "MACHINE");

				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest(" + cm.getName() + "): UsedPct: container='" + conName + "', threshold=" + threshold + ", worst session: SID=" + sid + ", USED_PCT='" + usedPct + "', OPEN_CURSORS='" + openCursors + "', OPEN_CURSORS_LIMIT='" + limit + "'.");

				if (usedPct > threshold)
				{
					String cfgName     = StringUtil.hasValue(conName) ? StringUtil.truncate("open_cursors:" + conName, 80, true, null) : "open_cursors"; // it identifies the alarm, max 80 characters
					String inContainer = StringUtil.hasValue(conName) ? " in container '" + conName + "'" : "";

					// Note: the text of an alarm can be max 512 characters
					String warningText = "Session SID=" + sid + " (user='" + username + "', program='" + program + "', machine='" + machine + "')" + inContainer + " has " + openCursors.intValue() + " cursors open: "
							+ usedPct + "% of 'open_cursors' (" + limit.intValue() + ") in Server '" + cm.getServerName() + "'. At 100% it gets ORA-01000. "
							+ "This is normally an application that does not close it's statements. What it has open: select SQL_ID, count(*) from V$OPEN_CURSOR where SID = " + sid + " group by SQL_ID. (thresholdInPct=" + threshold + ")";

					AlarmEvent ae = new AlarmEventConfigResourceIsLow(cm, cfgName, usedPct, warningText, threshold, 0);

					// The below is called from AlarmHandler on: Raise, RE-RAISE & CANCEL
					ae.setAlarmDescriptionProvider(this, new AlarmDescriptionProviderBase(cm.getPk(), cm.getAbsPkValue(r), threshold)
					{
						@Override
						public void setValues(CountersModel cm, AlarmEvent alarmEvent, AlarmPhase phase)
						{
							int    rowId = cm.getAbsRowIdForPkValue(getRowPk());
							String label = "Session SID=" + sid + inContainer;

							if (rowId != -1)
							{
								// Always: (RAISE, RE-RAISE, CANCEL) - Set the row as a table
								String extendedDescText = cm.toTextTableString(DATA_ABS, rowId);
								String extendedDescHtml = cm.toHtmlTableString(DATA_ABS, rowId, true, false, false);
								alarmEvent.setExtendedDescription(extendedDescText, extendedDescHtml);

								// Set CANCEL message (values that was found AFTER last raise/re-raise event)
								if (AlarmPhase.CANCEL.equals(phase))
									alarmEvent.setCancelDescription(getAlarmCancelText(label + ": open cursors", toAlarmCancelValue(cm.getAbsValueAsDouble(rowId, "USED_PCT")) + "% of the limit, " + cm.getAbsString(rowId, "OPEN_CURSORS") + " cursors", "%"));
							}
							else
							{
								// The row is gone: the session has ended, or it is not among the sessions with most open cursors any more
								if (AlarmPhase.CANCEL.equals(phase))
									alarmEvent.setCancelDescription(getAlarmCancelTextGone(label));
							}
						}
					});

					// Information about how to disable this alarm
					ae.createAlarmOptionsMessage(this, "UsedPct");

					AlarmHandler.getInstance().addAlarm(ae);
				}
			} // end: loop-containers
		} // end: UsedPct
	}

	public static final String  PROPKEY_alarm_UsedPct = CM_NAME + ".alarm.system.if.UsedPct.gt";
	public static final double  DEFAULT_alarm_UsedPct = 90;

	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;

		list.add(new CmSettingsHelper("UsedPct", isAlarmSwitch, PROPKEY_alarm_UsedPct, Double.class, conf.getDoubleProperty(PROPKEY_alarm_UsedPct, DEFAULT_alarm_UsedPct), DEFAULT_alarm_UsedPct, "If 'USED_PCT' (open cursors in percent of the parameter 'open_cursors') is greater than ##.# for any session then send 'AlarmEventConfigResourceIsLow'. One alarm per container, for the session that is closest to the limit." ));

		return list;
	}
}
