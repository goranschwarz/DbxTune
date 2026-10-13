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
import java.util.LinkedHashMap;
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
import com.dbxtune.alarm.events.oracle.AlarmEventOraPdbNotOpen;
import com.dbxtune.central.pcs.CentralPersistReader;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CmSettingsHelper.RegExpInputValidator;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.config.dict.MonTablesDictionary;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.graph.TrendGraphDataPoint;
import com.dbxtune.graph.TrendGraphDataPoint.LabelType;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.sql.conn.info.DbmsVersionInfoOracle;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmPdbs
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmPdbs.class.getSimpleName();
	public static final String   SHORT_NAME       = "Containers";
	public static final String   HTML_DESC        =
		"<html>" +
		"The containers of a Container Database (12.1 and above): CDB$ROOT, PDB$SEED and the Pluggable Databases (PDBs). Are they open, how big are they, and how much do they use.<br>" +
		"<br>" +
		"<ul>" +
		"  <li><code>OPEN_MODE</code>  - READ WRITE is normal for a PDB, READ ONLY for PDB$SEED. <b>MOUNTED means that the PDB is closed</b>: nobody can connect to it.</li>" +
		"  <li><code>RESTRICTED</code> - YES means that only administrators can connect. A PDB typically ends up like that after patching, when it has unresolved 'plug in violations' (see PDB_PLUG_IN_VIOLATIONS).</li>" +
		"  <li><code>SESSIONS</code>   - user sessions in the container right now.</li>" +
		"</ul>" +
		"From 12.2 the resource usage of every container for the last minute is also shown (CPU, sessions running and waiting for CPU, I/O, memory).<br>" +
		"<br>" +
		"<b>Note</b>: After a restart of the CDB the PDBs are MOUNTED (closed), unless their state has been saved with: <code>ALTER PLUGGABLE DATABASE ... SAVE STATE</code><br>" +
		"<b>Note</b>: Connect to CDB$ROOT to see all containers. Connected to a PDB, only that PDB is shown.<br>" +
		"<b>Note</b>: The alarm is only sent on a PRIMARY database (on a standby the PDBs are normally not open).<br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {CM_NAME, "V$CONTAINERS", "V$RSRCPDBMETRIC"};
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

		return new CmPdbs(counterController, guiController);
	}

	public CmPdbs(ICounterController counterController, IGuiController guiController)
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
	/** PRIMARY, PHYSICAL STANDBY... set when we connect (in checkDependsOnOther) */
	private String _databaseRole = "PRIMARY";

	public static final String GRAPH_NAME_SESSIONS = "PdbSessions";
	public static final String GRAPH_NAME_CPU_PCT  = "PdbCpuPct";
	public static final String GRAPH_NAME_IO_MB    = "PdbIoMbPerSec";

	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_SESSIONS,
			"Containers, User Sessions", 	                // Menu CheckBox text
			"User Sessions per Container (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_COUNT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null,
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_CPU_PCT,
			"Containers, CPU Usage in Percent", 	                // Menu CheckBox text
			"CPU Usage in Percent per Container, for the last minute (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERCENT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null,
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.CPU,
			true,  // is Percent Graph
			false, // visible at start
			Ver.ver(12,2), // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_IO_MB,
			"Containers, IO in MB per Second", 	                // Menu CheckBox text
			"Disk IO in MB per Second per Container, for the last minute (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null,
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			false, // visible at start
			Ver.ver(12,2), // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("CON_ID");

		return pkCols;
	}

	/** Only for a Container Database (12.1 and above), and the views must be readable. Otherwise: disable the CM, and say why */
	@Override
	public boolean checkDependsOnOther(DbxConnection conn)
	{
		DbmsVersionInfoOracle oraVersionInfo = (DbmsVersionInfoOracle) conn.getDbmsVersionInfo();

		// "Is this a CDB" is read from V$DATABASE: without that we can't tell, so say that it's a grant that is missing
		if ( ! OracleCmUtils.checkViewsAreReadable(this, conn, "V$DATABASE") )
			return false;

		// Remember the role (PRIMARY, PHYSICAL STANDBY...) for the alarm. If we could not read it: PRIMARY
		_databaseRole = StringUtil.hasValue(oraVersionInfo.getDatabaseRole()) ? oraVersionInfo.getDatabaseRole() : "PRIMARY";

		if (oraVersionInfo.getLongVersion() < Ver.ver(12,1) || ! oraVersionInfo.isCdb())
		{
			String msg = "This is not a Container Database (containers and PDBs exists in Oracle 12.1 and above, when the database was created as a CDB).";

			_logger.info("When trying to initialize Counters Model '" + getName() + "', named '" + getDisplayName() + "': " + msg);

			setActive(false, msg);

			TabularCntrPanel tcp = getTabPanel();
			if (tcp != null)
				tcp.setToolTipText(msg);

			return false;
		}

		return OracleCmUtils.checkViewsAreReadable(this, conn, "V$CONTAINERS", "V$SESSION");
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		// 12.1: no resource metrics
		String noMetric = "CAST(NULL as NUMBER(18,1))";
		String sql121 = getSql("CAST(NULL as VARCHAR2(8))", noMetric, noMetric, noMetric, noMetric, noMetric, noMetric, noMetric, "");

		// 12.2 and above: RECOVERY_STATUS, and V$RSRCPDBMETRIC (the resource usage of every container for the last minute)
		String sql122 = getSql("c.RECOVERY_STATUS",
				"CAST(m.AVG_CPU_UTILIZATION  as NUMBER(18,1))",
				"CAST(m.AVG_RUNNING_SESSIONS as NUMBER(18,1))",
				"CAST(m.AVG_WAITING_SESSIONS as NUMBER(18,1))",
				"CAST(m.IOPS                 as NUMBER(18,1))",
				"CAST(m.IOMBPS               as NUMBER(18,1))",
				"CAST(m.SGA_BYTES / 1048576  as NUMBER(18,1))",
				"CAST(m.PGA_BYTES / 1048576  as NUMBER(18,1))",
				"left join V$RSRCPDBMETRIC m on m.CON_ID = c.CON_ID \n");

		// The SQL for 12.2 and above can NOT be tested on all versions: so "dry-run" it, and if it fails use the 12.1 SQL
		// conn is null when we just want to "view" the SQL (the CM Properties dialog)
		if (versionInfo.getLongVersion() < Ver.ver(12,2))
			return sql121;

		if (conn == null)
			return sql122;

		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery("select * from (" + sql122 + ") where 1=2"))
		{
			return sql122;
		}
		catch (SQLException ex)
		{
			_logger.warn(CM_NAME + ": The SQL with resource metrics (version " + Ver.versionNumToStr(versionInfo.getLongVersion()) + ") failed, CPU/IO/memory per container will not be shown. Caught: " + ex.getMessage().trim());
			return sql121;
		}
	}

	private static String getSql(String RECOVERY_STATUS, String CPU_PCT, String RUNNING_SESSIONS, String WAITING_SESSIONS, String IOPS, String IO_MB_PER_SEC, String SGA_MB, String PGA_MB, String joinMetric)
	{
		// One row for every container we can see: CDB$ROOT, PDB$SEED and the PDBs (connected to a PDB: only that one)
		// OPEN_TIME is a TIMESTAMP WITH TIME ZONE (which can't be stored): so it's CAST to a DATE
		// SESSIONS: decode(..., 0) the "else 0" is needed. Without it the aggregate skips NULL values, and Oracle sends a warning with the result (which is treated as a failed sample)
		// Note: CAST(... as NUMBER(#,1)) is needed to keep decimals (a NUMBER without precision is treated as a BIGINT)
		return ""
			+ "select \n"
			+ "     c.CON_ID \n"
			+ "    ,c.NAME \n"
			+ "    ,c.OPEN_MODE \n"
			+ "    ,c.RESTRICTED \n"
			+ "    ,CAST(c.OPEN_TIME as DATE)                         as OPEN_TIME \n"
			+ "    ," + RECOVERY_STATUS + " as RECOVERY_STATUS \n"
			+ "    ,CAST(c.TOTAL_SIZE / 1048576 as NUMBER(18,1))      as TOTAL_SIZE_MB \n"
			+ "    ,nvl(s.SESSIONS, 0)                                as SESSIONS \n"
			+ "    ,nvl(s.ACTIVE_SESSIONS, 0)                         as ACTIVE_SESSIONS \n"
			+ "    ," + CPU_PCT          + " as CPU_PCT \n"
			+ "    ," + RUNNING_SESSIONS + " as AVG_RUNNING_SESSIONS \n"
			+ "    ," + WAITING_SESSIONS + " as AVG_WAITING_SESSIONS \n"
			+ "    ," + IOPS             + " as IOPS \n"
			+ "    ," + IO_MB_PER_SEC    + " as IO_MB_PER_SEC \n"
			+ "    ," + SGA_MB           + " as SGA_MB \n"
			+ "    ," + PGA_MB           + " as PGA_MB \n"
			+ "from V$CONTAINERS c \n"
			+ "left join (select CON_ID, count(*) as SESSIONS, sum(decode(STATUS, 'ACTIVE', 1, 0)) as ACTIVE_SESSIONS \n"
			+ "           from V$SESSION \n"
			+ "           where TYPE = 'USER' \n"
			+ "           group by CON_ID) s on s.CON_ID = c.CON_ID \n"
			+ joinMetric
			+ "order by c.CON_ID \n"
			+ "";
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			mtd.addTable(CM_NAME, "The containers of a Container Database: CDB$ROOT, PDB$SEED and the Pluggable Databases (PDBs).");

			mtd.addColumn(CM_NAME, "CON_ID",               "<html>Container ID: 1 = CDB$ROOT, 2 = PDB$SEED, 3 and above = a Pluggable Database (PDB).</html>");
			mtd.addColumn(CM_NAME, "NAME",                 "<html>Name of the container.</html>");
			mtd.addColumn(CM_NAME, "OPEN_MODE",            "<html><b>READ WRITE</b> = open (normal for a PDB), <b>READ ONLY</b> = open for reading (normal for PDB$SEED, and on a standby), <b>MOUNTED</b> = closed: nobody can connect, <b>MIGRATE</b> = open for upgrade.</html>");
			mtd.addColumn(CM_NAME, "RESTRICTED",           "<html>YES = only users with the RESTRICTED SESSION privilege (administrators) can connect. NO = normal.</html>");
			mtd.addColumn(CM_NAME, "OPEN_TIME",            "<html>When the container was last opened (in the time of the DBMS host).</html>");
			mtd.addColumn(CM_NAME, "RECOVERY_STATUS",      "<html>ENABLED = normal. DISABLED = the PDB is excluded from recovery (12.2 and above).</html>");
			mtd.addColumn(CM_NAME, "TOTAL_SIZE_MB",        "<html>Size of all data files and temp files of the container, in MB. 0 if the container is closed.</html>");
			mtd.addColumn(CM_NAME, "SESSIONS",             "<html>Number of user sessions in the container right now.</html>");
			mtd.addColumn(CM_NAME, "ACTIVE_SESSIONS",      "<html>Number of user sessions in the container that are ACTIVE (doing something) right now.</html>");
			mtd.addColumn(CM_NAME, "CPU_PCT",              "<html>CPU usage of the container in percent of all CPUs, average for the last minute (V$RSRCPDBMETRIC.AVG_CPU_UTILIZATION, 12.2 and above).</html>");
			mtd.addColumn(CM_NAME, "AVG_RUNNING_SESSIONS", "<html>Average number of sessions that was running on CPU, for the last minute (12.2 and above).</html>");
			mtd.addColumn(CM_NAME, "AVG_WAITING_SESSIONS", "<html>Average number of sessions that was waiting for CPU (held back by the Resource Manager), for the last minute (12.2 and above).</html>");
			mtd.addColumn(CM_NAME, "IOPS",                 "<html>I/O requests per second, average for the last minute (12.2 and above).</html>");
			mtd.addColumn(CM_NAME, "IO_MB_PER_SEC",        "<html>I/O in MB per second, average for the last minute (12.2 and above).</html>");
			mtd.addColumn(CM_NAME, "SGA_MB",               "<html>SGA memory used by the container, in MB (12.2 and above).</html>");
			mtd.addColumn(CM_NAME, "PGA_MB",               "<html>PGA memory used by the container, in MB (12.2 and above).</html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e);
		}
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		// All the graphs is: one "line" for every container
		String colName = null;
		if (GRAPH_NAME_SESSIONS.equals(tgdp.getName())) colName = "SESSIONS";
		if (GRAPH_NAME_CPU_PCT .equals(tgdp.getName())) colName = "CPU_PCT";
		if (GRAPH_NAME_IO_MB   .equals(tgdp.getName())) colName = "IO_MB_PER_SEC";

		if (colName != null)
		{
			LinkedHashMap<String, Double> dataMap = new LinkedHashMap<String, Double>();

			for (int r = 0; r < this.size(); r++)
				dataMap.put(this.getAbsString(r, "NAME"), this.getAbsValueAsDouble(r, colName, 0d));

			if (dataMap.size() > 0)
				tgdp.setData(this.getTimestamp(), dataMap);
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
		// PdbNotOpen
		//-------------------------------------------------------
		if (isSystemAlarmsForColumnEnabledAndInTimeRange("PdbNotOpen"))
		{
			// On a standby the PDB's are normally not open: so only on a PRIMARY database
			if ( ! "PRIMARY".equals(_databaseRole) )
				return;

			String openModeRegex = conf.getProperty(PROPKEY_alarm_PdbNotOpenOpenModes, DEFAULT_alarm_PdbNotOpenOpenModes);
			String skipNameRegex = conf.getProperty(PROPKEY_alarm_PdbNotOpenSkipNames, DEFAULT_alarm_PdbNotOpenSkipNames);

			for (int r=0; r<cm.getAbsRowCount(); r++)
			{
				String pdbName    = cm.getAbsString(r, "NAME");
				String openMode   = cm.getAbsString(r, "OPEN_MODE");
				String restricted = cm.getAbsString(r, "RESTRICTED");

				if (pdbName == null || openMode == null)
					continue;

				if (StringUtil.hasValue(skipNameRegex) && pdbName.matches(skipNameRegex))
					continue;

				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest(" + cm.getName() + "): PdbNotOpen: pdb='" + pdbName + "', openModeRegex='" + openModeRegex + "', OPEN_MODE='" + openMode + "', RESTRICTED='" + restricted + "'.");

				if ((StringUtil.hasValue(openModeRegex) && openMode.matches(openModeRegex)) || "YES".equals(restricted))
				{
					AlarmEvent ae = new AlarmEventOraPdbNotOpen(cm, pdbName, openMode, restricted, openModeRegex);

					// The below is called from AlarmHandler on: Raise, RE-RAISE & CANCEL
					ae.setAlarmDescriptionProvider(this, new AlarmDescriptionProviderBase(cm.getPk(), cm.getAbsPkValue(r), null)
					{
						@Override
						public void setValues(CountersModel cm, AlarmEvent alarmEvent, AlarmPhase phase)
						{
							int    rowId = cm.getAbsRowIdForPkValue(getRowPk());
							String label = "Pluggable Database '" + pdbName + "'";

							if (rowId != -1)
							{
								// Always: (RAISE, RE-RAISE, CANCEL) - Set the row as a table
								String extendedDescText = cm.toTextTableString(DATA_ABS, rowId);
								String extendedDescHtml = cm.toHtmlTableString(DATA_ABS, rowId, true, false, false);
								alarmEvent.setExtendedDescription(extendedDescText, extendedDescHtml);

								// Set CANCEL message (values that was found AFTER last raise/re-raise event)
								if (AlarmPhase.CANCEL.equals(phase))
									alarmEvent.setCancelDescription(getAlarmCancelText(label + ": OPEN_MODE", "'" + cm.getAbsString(rowId, "OPEN_MODE") + "', RESTRICTED='" + cm.getAbsString(rowId, "RESTRICTED") + "'"));
							}
							else
							{
								// The row is gone: the PDB was dropped (or unplugged)
								if (AlarmPhase.CANCEL.equals(phase))
									alarmEvent.setCancelDescription(getAlarmCancelTextGone(label));
							}
						}
					});

					// Information about how to disable this alarm
					ae.createAlarmOptionsMessage(this, "PdbNotOpen");

					AlarmHandler.getInstance().addAlarm(ae);
				}
			} // end: loop-rows
		} // end: PdbNotOpen
	}

	public static final String  PROPKEY_alarm_PdbNotOpenOpenModes = CM_NAME + ".alarm.system.if.OpenMode.in";
	public static final String  DEFAULT_alarm_PdbNotOpenOpenModes = "(MOUNTED|MIGRATE)";

	// "PDB.SEED" and not "PDB\$SEED": a backslash is a pain to write in a properties file
	public static final String  PROPKEY_alarm_PdbNotOpenSkipNames = CM_NAME + ".alarm.system.if.OpenMode.skip.names";
	public static final String  DEFAULT_alarm_PdbNotOpenSkipNames = "(PDB.SEED)";

	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;

		list.add(new CmSettingsHelper("PdbNotOpen"           , isAlarmSwitch, PROPKEY_alarm_PdbNotOpenOpenModes, String.class, conf.getProperty(PROPKEY_alarm_PdbNotOpenOpenModes, DEFAULT_alarm_PdbNotOpenOpenModes), DEFAULT_alarm_PdbNotOpenOpenModes, "If 'OPEN_MODE' matches this regexp, or if 'RESTRICTED' is YES, then send 'AlarmEventOraPdbNotOpen'. Only on a PRIMARY database.", new RegExpInputValidator()));
		list.add(new CmSettingsHelper("PdbNotOpen Skip Names",                PROPKEY_alarm_PdbNotOpenSkipNames, String.class, conf.getProperty(PROPKEY_alarm_PdbNotOpenSkipNames, DEFAULT_alarm_PdbNotOpenSkipNames), DEFAULT_alarm_PdbNotOpenSkipNames, "If 'PdbNotOpen' is true; Skip containers where 'NAME' matches this regexp. Example: (PDB.SEED|TEST.*)", new RegExpInputValidator()));

		return list;
	}
}
