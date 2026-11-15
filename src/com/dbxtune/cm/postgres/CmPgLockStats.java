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
package com.dbxtune.cm.postgres;

import java.lang.invoke.MethodHandles;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.naming.NameNotFoundException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.central.pcs.CentralPersistReader;
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
import com.dbxtune.utils.Ver;

/**
 * Lock waits per lock type, for the whole server (pg_stat_lock, Postgres 19 and above).
 * <p>
 * NOTE: Not the same as CmPgLocks, which lists the locks held/waited for right now (pg_locks).
 */
public class CmPgLockStats
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmPgLockStats.class.getSimpleName();
	public static final String   SHORT_NAME       = "Lock Stats";
	public static final String   HTML_DESC        =
		"<html>" +
		"Lock waits per lock type, for the whole server (Postgres 19 and above).<br>" +
		"How often sessions had to wait for a lock, and for how long - without pg_wait_sampling.<br>" +
		"<i>The locks held or waited for right now are in 'Locks' (pg_locks).</i>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = Ver.ver(19);
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"pg_stat_lock"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
			"waits",
			"wait_time",
			"fastpath_exceeded"
	};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;

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

		return new CmPgLockStats(counterController, guiController);
	}

	public CmPgLockStats(ICounterController counterController, IGuiController guiController)
	{
		super(counterController, guiController,
				CM_NAME, GROUP_NAME, /*sql*/null, /*pkList*/null,
				DIFF_COLUMNS, PCT_COLUMNS, MON_TABLES,
				NEED_ROLES, NEED_CONFIG, NEED_SRV_VERSION, NEED_CE_VERSION,
				NEGATIVE_DIFF_COUNTERS_TO_ZERO, IS_SYSTEM_CM, DEFAULT_POSTPONE_TIME);

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
	public static final String GRAPH_NAME_WAITS     = "LockWaits";
	public static final String GRAPH_NAME_WAIT_TIME = "LockWaitTime";

	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_WAITS,
				"Lock Waits per Second, per Lock Type",                    // Menu CheckBox text
				"Lock Waits per Second, per Lock Type (" + SHORT_NAME + ")", // Graph Label
				TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
				null, LabelType.Dynamic,
				TrendGraphDataPoint.Category.LOCK,
				false, // is Percent Graph
				false, // visible at start
				Ver.ver(19), // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
				-1);   // minimum height

		addTrendGraph(GRAPH_NAME_WAIT_TIME,
				"Lock Wait Time in ms per Second, per Lock Type",                    // Menu CheckBox text
				"Lock Wait Time in ms per Second, per Lock Type (" + SHORT_NAME + ")", // Graph Label
				TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
				null, LabelType.Dynamic,
				TrendGraphDataPoint.Category.LOCK,
				false, // is Percent Graph
				true,  // visible at start
				Ver.ver(19), // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
				-1);   // minimum height
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		String colName = null;
		if (GRAPH_NAME_WAITS    .equals(tgdp.getName())) colName = "waits";
		if (GRAPH_NAME_WAIT_TIME.equals(tgdp.getName())) colName = "wait_time";
		if (colName == null)
			return;

		// One line per lock type
		Map<String, Double> map = new LinkedHashMap<>();
		for (int r = 0; r < this.getRateRowCount(); r++)
		{
			String locktype = this.getRateString(r, "locktype");
			if (locktype != null)
				map.put(locktype, this.getRateValueAsDouble(r, colName, 0d));
		}
		if ( ! map.isEmpty() )
			tgdp.setData(this.getTimestamp(), map);
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();
		pkCols.add("locktype");
		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return "SELECT * FROM pg_stat_lock \n";
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try
		{
			String tabname = "pg_stat_lock";
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			mtd.addTable(tabname, "One row per lock type: cluster-wide lock wait statistics (Postgres 19 and above).");

			mtd.addColumn(tabname, "locktype",          "<html>Type of the lockable object (relation, tuple, transactionid, advisory, ...): as pg_locks.locktype.</html>");
			mtd.addColumn(tabname, "waits",             "<html>Number of times a lock of this type had to wait.</html>");
			mtd.addColumn(tabname, "wait_time",         "<html>Total time spent waiting for locks of this type, in milliseconds.</html>");
			mtd.addColumn(tabname, "fastpath_exceeded", "<html>Number of times a lock of this type could not be taken by the fast path (the per-backend fast-path slots were full), and had to use the shared lock table.</html>");
			mtd.addColumn(tabname, "stats_reset",       "<html>Time at which these statistics were last reset.</html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e);
		}
	}
}
