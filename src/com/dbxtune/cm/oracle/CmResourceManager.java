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
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.List;

import javax.naming.NameNotFoundException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.central.pcs.CentralPersistReader;
import com.dbxtune.cm.CmChartDescriptor;
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
import com.dbxtune.sql.conn.info.DbmsVersionInfoOracle;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmResourceManager
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmResourceManager.class.getSimpleName();
	public static final String   SHORT_NAME       = "Resource Manager";
	public static final String   HTML_DESC        =
		"<html>" +
		"What the Resource Manager does to the sessions, per consumer group: how much CPU each group has used, and how long it has had to <b>wait for CPU</b> because the resource plan held it back.<br>" +
		"<br>" +
		"The Resource Manager is always there: when no plan is set Oracle uses an internal one, and in the maintenance windows (nights and weekends) it switches to <code>DEFAULT_MAINTENANCE_PLAN</code>.<br>" +
		"So a system that is 'slow for no reason' during a maintenance window can be the Resource Manager giving the CPU to the automatic tasks.<br>" +
		"<br>" +
		"<b>Tip</b>: Look at the DIFF or RATE values of <code>CPU_WAIT_TIME</code>. The same wait is seen in the tab 'System Events' as the event <code>resmgr:cpu quantum</code>.<br>" +
		"Other things the plan can do, each with it's own counter: queue sessions (<code>QUEUE_LENGTH</code>, <code>QUEUED_TIME</code>), cancel statements (<code>SQL_CANCELED</code>) and kill sessions.<br>" +
		"<br>" +
		"<b>Note</b>: What the plans say (who gets how much) is in: DBMS Configuration, tab 'Resource Manager'.<br>" +
		"<b>Note</b>: The time columns are in milliseconds.<br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {CM_NAME, "V$RSRC_CONSUMER_GROUP", "V$RSRC_PLAN"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"REQUESTS",
		"CONSUMED_CPU_TIME",
		"CPU_WAIT_TIME",
		"CPU_WAITS",
		"YIELDS",
		"QUEUED_TIME",
		"QUEUE_TIME_OUTS",
		"ACTIVE_SESSION_LIMIT_HIT",
		"UNDO_LIMIT_HIT",
		"SQL_CANCELED",
		"ACTIVE_SESSIONS_KILLED",
		"IDLE_SESSIONS_KILLED",
		"IDLE_BLKR_SESSIONS_KILLED",
		"IO_SERVICE_TIME",
		"IO_SERVICE_WAITS",
		"PQS_QUEUED",
		"PQ_QUEUED_TIME"
		};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = 10;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.MEDIUM; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '" + CM_NAME + "'");

		return new CmResourceManager(counterController, guiController);
	}

	public CmResourceManager(ICounterController counterController, IGuiController guiController)
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
	public static final String GRAPH_NAME_CPU_WAIT_MS = "RsrcCpuWaitMs";

	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_CPU_WAIT_MS,
			"Resource Manager, CPU Wait Time per Consumer Group", 	                // Menu CheckBox text
			"Resource Manager, ms per Second that Sessions Waited for CPU, per Consumer Group (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MILLISEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null,
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.CPU,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		// CON_ID is always 0 in a non-CDB (and in 11.2), in a CDB every container has it's own consumer groups (with the same names)
		pkCols.add("CON_ID");
		pkCols.add("CONSUMER_GROUP");

		return pkCols;
	}

	/** If we can't read the views: disable the CM, and tell what to do about it */
	@Override
	public boolean checkDependsOnOther(DbxConnection conn)
	{
		return OracleCmUtils.checkViewsAreReadable(this, conn, "V$RSRC_CONSUMER_GROUP", "V$RSRC_PLAN");
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		DbmsVersionInfoOracle oraVersionInfo = (DbmsVersionInfoOracle) versionInfo;

		// 12.1 and above: the V$ views has CON_ID. Connected to CDB$ROOT we see the consumer groups of ALL containers, so there we show which container it is
		boolean hasConId  = versionInfo.getLongVersion() >= Ver.ver(12,1);
		boolean isCdbRoot = hasConId && oraVersionInfo.isCdbRoot();

		// The columns are the same from 11.2. The times are in milliseconds
		// PLAN_NAME: the resource plan that is active right now (in a CDB: the plan of the container)
		String sql = ""
			+ "select \n"
			+ "     " + (isCdbRoot ? "g.CON_ID" : "CAST(0    as NUMBER(10))")    + " as CON_ID \n"
			+ "    ," + (isCdbRoot ? "c.NAME"   : "CAST(NULL as VARCHAR2(128))") + " as CON_NAME \n"
			+ "    ,g.NAME as CONSUMER_GROUP \n"
			+ "    ,(select max(p.NAME) from V$RSRC_PLAN p where p.IS_TOP_PLAN = 'TRUE'" + (hasConId ? " and p.CON_ID = g.CON_ID" : "") + ") as PLAN_NAME \n"
			+ "    ,g.ACTIVE_SESSIONS \n"
			+ "    ,g.EXECUTION_WAITERS \n"
			+ "    ,g.QUEUE_LENGTH \n"
			+ "    ,g.REQUESTS \n"
			+ "    ,g.CONSUMED_CPU_TIME \n"
			+ "    ,g.CPU_WAIT_TIME \n"
			+ "    ,g.CPU_WAITS \n"
			+ "    ,g.YIELDS \n"
			+ "    ,g.QUEUED_TIME \n"
			+ "    ,g.QUEUE_TIME_OUTS \n"
			+ "    ,g.ACTIVE_SESSION_LIMIT_HIT \n"
			+ "    ,g.UNDO_LIMIT_HIT \n"
			+ "    ,g.SQL_CANCELED \n"
			+ "    ,g.ACTIVE_SESSIONS_KILLED \n"
			+ "    ,g.IDLE_SESSIONS_KILLED \n"
			+ "    ,g.IDLE_BLKR_SESSIONS_KILLED \n"
			+ "    ,g.IO_SERVICE_TIME \n"
			+ "    ,g.IO_SERVICE_WAITS \n"
			+ "    ,g.CURRENT_PQS_ACTIVE \n"
			+ "    ,g.CURRENT_PQS_QUEUED \n"
			+ "    ,g.PQS_QUEUED \n"
			+ "    ,g.PQ_QUEUED_TIME \n"
			+ "    ,g.CURRENT_UNDO_CONSUMPTION \n"
			+ "from V$RSRC_CONSUMER_GROUP g \n"
			+ (isCdbRoot ? "left join V$CONTAINERS c on c.CON_ID = g.CON_ID \n" : "")
			+ "order by 1, 3 \n"
			+ "";

		return sql;
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			mtd.addTable(CM_NAME, "What the Resource Manager does to the sessions, per consumer group.");

			mtd.addColumn(CM_NAME, "CON_ID",                    "<html>Container ID. 0 = This is not a Container Database (or the version is below 12.1), or we are connected to a PDB.</html>");
			mtd.addColumn(CM_NAME, "CON_NAME",                  "<html>Name of the Container (PDB) the consumer group is in. NULL if CON_ID is 0.</html>");
			mtd.addColumn(CM_NAME, "CONSUMER_GROUP",            "<html>Name of the consumer group. OTHER_GROUPS = the sessions that are not mapped to a group. _ORACLE_BACKGROUND_GROUP_ = Oracle's background processes.</html>");
			mtd.addColumn(CM_NAME, "PLAN_NAME",                 "<html>The resource plan that is active right now. It changes when a maintenance window opens (DEFAULT_MAINTENANCE_PLAN).</html>");
			mtd.addColumn(CM_NAME, "ACTIVE_SESSIONS",           "<html>Number of sessions in the group that are active right now.</html>");
			mtd.addColumn(CM_NAME, "EXECUTION_WAITERS",         "<html>Number of sessions in the group that are waiting for CPU right now (held back by the plan).</html>");
			mtd.addColumn(CM_NAME, "QUEUE_LENGTH",              "<html>Number of sessions that are queued right now, because the group has reached it's limit of active sessions.</html>");
			mtd.addColumn(CM_NAME, "REQUESTS",                  "<html>Number of requests (calls) that has been executed in the group.</html>");
			mtd.addColumn(CM_NAME, "CONSUMED_CPU_TIME",         "<html>CPU time that the sessions in the group has used, in milliseconds.</html>");
			mtd.addColumn(CM_NAME, "CPU_WAIT_TIME",             "<html>Time that the sessions in the group has waited for CPU because of the Resource Manager, in milliseconds.<br>This is the throttling. It does not include waits for disk, locks or latches.</html>");
			mtd.addColumn(CM_NAME, "CPU_WAITS",                 "<html>Number of times a session in the group had to wait for CPU because of the Resource Manager.</html>");
			mtd.addColumn(CM_NAME, "YIELDS",                    "<html>Number of times a session in the group had to give up the CPU to another session, because of the Resource Manager.</html>");
			mtd.addColumn(CM_NAME, "QUEUED_TIME",               "<html>Time that sessions has been queued, because the group had reached it's limit of active sessions, in milliseconds.</html>");
			mtd.addColumn(CM_NAME, "QUEUE_TIME_OUTS",           "<html>Number of times a queued request gave up, because it had waited longer than the plan allows.</html>");
			mtd.addColumn(CM_NAME, "ACTIVE_SESSION_LIMIT_HIT",  "<html>Number of times a session was queued because the group had reached it's limit of active sessions.</html>");
			mtd.addColumn(CM_NAME, "UNDO_LIMIT_HIT",            "<html>Number of times a statement was cancelled because the group had reached it's undo limit.</html>");
			mtd.addColumn(CM_NAME, "SQL_CANCELED",              "<html>Number of times a statement was cancelled because it used more than the plan allows (CPU time, IO, elapsed time).</html>");
			mtd.addColumn(CM_NAME, "ACTIVE_SESSIONS_KILLED",    "<html>Number of times an active session was killed because it used more than the plan allows.</html>");
			mtd.addColumn(CM_NAME, "IDLE_SESSIONS_KILLED",      "<html>Number of times a session was killed because it had been idle longer than the plan allows.</html>");
			mtd.addColumn(CM_NAME, "IDLE_BLKR_SESSIONS_KILLED", "<html>Number of times a session was killed because it had been idle too long while it was blocking another session.</html>");
			mtd.addColumn(CM_NAME, "IO_SERVICE_TIME",           "<html>Time that the sessions in the group has spent on IO, in milliseconds.</html>");
			mtd.addColumn(CM_NAME, "IO_SERVICE_WAITS",          "<html>Number of IO requests from the sessions in the group.</html>");
			mtd.addColumn(CM_NAME, "CURRENT_PQS_ACTIVE",        "<html>Number of parallel statements that are running right now in the group.</html>");
			mtd.addColumn(CM_NAME, "CURRENT_PQS_QUEUED",        "<html>Number of parallel statements that are queued right now in the group.</html>");
			mtd.addColumn(CM_NAME, "PQS_QUEUED",                "<html>Number of times a parallel statement has been queued in the group.</html>");
			mtd.addColumn(CM_NAME, "PQ_QUEUED_TIME",            "<html>Time that parallel statements has been queued in the group, in milliseconds.</html>");
			mtd.addColumn(CM_NAME, "CURRENT_UNDO_CONSUMPTION",  "<html>Undo that the group uses right now, in KB.</html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e);
		}
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		//--------------------------------------------------------
		// How many milliseconds per second the sessions of each consumer group waited for CPU, because of the Resource Manager
		// 1000 ms per second == one session that was held back all the time
		//--------------------------------------------------------
		if (GRAPH_NAME_CPU_WAIT_MS.equals(tgdp.getName()))
		{
			LinkedHashMap<String, Double> dataMap = new LinkedHashMap<>();

			for (int r = 0; r < this.size(); r++)
			{
				String conName = this.getAbsString(r, "CON_NAME");
				String group   = this.getAbsString(r, "CONSUMER_GROUP");
				Double msPerSec = this.getRateValueAsDouble(r, "CPU_WAIT_TIME");

				// group   or in a CDB: container:group   (CON_ID + CONSUMER_GROUP is the Primary Key, so the labels are unique)
				String label = StringUtil.hasValue(conName) ? conName + ":" + group : group;

				dataMap.put(label, msPerSec == null ? 0.0 : msPerSec);
			}

			if (dataMap.size() > 0)
				tgdp.setData(this.getTimestamp(), dataMap);
		}
	}

	/** Local graphs (beside the table): which consumer group uses the CPU, and which one is held back by the plan */
	@Override
	public List<CmChartDescriptor> createChartDescriptors()
	{
		List<CmChartDescriptor> list = new ArrayList<>();

		list.add(new CmChartDescriptor()
			.id("rsrc-cpu-used")
			.title("CPU Used by Consumer Group (ms)")
			.chartType(CmChartDescriptor.CHART_TYPE_DUAL_PIE_BAR)
			.labelColumn("CONSUMER_GROUP")
			.labelPrefixColumns("CON_NAME")
			.valueColumns("CONSUMED_CPU_TIME")
			.seriesLabels("CPU Used")
			.skipZeroRows(true)
			.pieOtherLimit(0.02)
			.splitRatio(0.6));

		// CPU_WAIT_TIME: the time the sessions of the group had to wait for CPU because of the Resource Manager plan
		list.add(new CmChartDescriptor()
			.id("rsrc-cpu-wait")
			.title("Waited for CPU by Consumer Group (ms): held back by the plan")
			.chartType(CmChartDescriptor.CHART_TYPE_DUAL_PIE_BAR)
			.labelColumn("CONSUMER_GROUP")
			.labelPrefixColumns("CON_NAME")
			.valueColumns("CPU_WAIT_TIME")
			.seriesLabels("CPU Wait")
			.skipZeroRows(true)
			.pieOtherLimit(0.02));

		return list;
	}
}
