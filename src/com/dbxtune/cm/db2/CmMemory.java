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
import com.dbxtune.utils.StringUtil;

/**
 * The memory of the Db2 instance: the memory sets (instance, database, applications, private, FMP) and the memory pools in them
 * (buffer pools, package cache, lock list, sort...), and the total against INSTANCE_MEMORY.
 *
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmMemory
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmMemory.class.getSimpleName();
	public static final String   SHORT_NAME       = "Memory";
	public static final String   HTML_DESC        =
		"<html>" +
		"<p>The memory of the Db2 instance (all its databases):</p>" +
		"<ul>" +
		"    <li>ROW_TYPE SET - the memory sets: DBMS (the instance), DATABASE (shared memory of a database), APPLICATION, PRIVATE (the agents), FMP (fenced routines)</li>" +
		"    <li>ROW_TYPE POOL - the memory pools in the sets, per kind: BP (buffer pools), PACKAGE_CACHE, LOCK_MGR (lock list), SHARED_SORT, CAT_CACHE...</li>" +
		"    <li>ROW_TYPE LIMIT - all the committed memory against INSTANCE_MEMORY (the limit of the instance)</li>" +
		"</ul>" +
		"<p>With self tuning memory (SELF_TUNING_MEM, the default) Db2 moves memory between the buffer pools, the package cache, the lock list and the sort heap: this shows where it went.</p>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_CACHE;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {CM_NAME, "MON_GET_MEMORY_SET", "MON_GET_MEMORY_POOL"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = false;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 60;
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

		return new CmMemory(counterController, guiController);
	}

	public CmMemory(ICounterController counterController, IGuiController guiController)
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
	public static final String GRAPH_NAME_SET_MB      = "MemSetMb";
	public static final String GRAPH_NAME_POOL_MB     = "MemPoolMb";
	public static final String GRAPH_NAME_INST_PCT    = "InstMemPct";

	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_SET_MB,
			"Memory per Memory Set, Committed MB",                          // Menu CheckBox text
			"Memory per Memory Set, Committed MB (" + SHORT_NAME + ")",     // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null,
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.MEMORY,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_POOL_MB,
			"Memory per Memory Pool Type, Used MB",                         // Menu CheckBox text
			"Memory per Memory Pool Type, Used MB, Top 15 (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null,
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.MEMORY,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_INST_PCT,
			"Instance Memory Used in Percent of INSTANCE_MEMORY",           // Menu CheckBox text
			"Instance Memory Used in Percent of INSTANCE_MEMORY (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERCENT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Committed Pct" },
			LabelType.Static,
			TrendGraphDataPoint.Category.MEMORY,
			true,  // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("ROW_TYPE");
		pkCols.add("MEMORY_SET_TYPE");
		pkCols.add("DB_NAME");
		pkCols.add("MEMORY_POOL_TYPE");
		pkCols.add("MEMBER");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		// NOTE: The sizes are in KB. DB_NAME is NULL for the instance level sets (it is '' here, it is in the primary key)
		// NOTE: INSTANCE_MEMORY is in 4 KB pages (with AUTOMATIC the VALUE is the computed limit)
		String sql = ""
			+ "with s as (select * from table(MON_GET_MEMORY_SET(NULL, NULL, -2)) x) \n"
			+ "select /* ${cmCollectorName} */ \n"
			+ "     CAST('SET' AS VARCHAR(5))                                 as ROW_TYPE \n"
			+ "    ,s.MEMORY_SET_TYPE \n"
			+ "    ,COALESCE(s.DB_NAME, '')                                    as DB_NAME \n"
			+ "    ,CAST('' AS VARCHAR(32))                                    as MEMORY_POOL_TYPE \n"
			+ "    ,s.MEMBER \n"
			+ "    ,CAST(1 AS INTEGER)                                         as POOL_COUNT \n"
			+ "    ,CAST(s.MEMORY_SET_USED      / 1024.0 AS DECIMAL(15,1))     as USED_MB \n"
			+ "    ,CAST(s.MEMORY_SET_USED_HWM  / 1024.0 AS DECIMAL(15,1))     as USED_HWM_MB \n"
			+ "    ,CAST(s.MEMORY_SET_COMMITTED / 1024.0 AS DECIMAL(15,1))     as COMMITTED_MB \n"
			+ "    ,CAST(s.MEMORY_SET_SIZE      / 1024.0 AS DECIMAL(15,1))     as SIZE_MB \n"
			+ "    ,CAST(NULL AS DECIMAL(5,1))                                 as USED_PCT \n"
			+ "from s \n"
			+ "union all \n"
			+ "select \n"
			+ "     'POOL' \n"
			+ "    ,p.MEMORY_SET_TYPE \n"
			+ "    ,COALESCE(p.DB_NAME, '') \n"
			+ "    ,p.MEMORY_POOL_TYPE \n"
			+ "    ,p.MEMBER \n"
			+ "    ,CAST(count(*) AS INTEGER) \n"
			+ "    ,CAST(sum(p.MEMORY_POOL_USED)     / 1024.0 AS DECIMAL(15,1)) \n"
			+ "    ,CAST(sum(p.MEMORY_POOL_USED_HWM) / 1024.0 AS DECIMAL(15,1)) \n"
			+ "    ,CAST(NULL AS DECIMAL(15,1)) \n"
			+ "    ,CAST(NULL AS DECIMAL(15,1)) \n"
			+ "    ,CAST(NULL AS DECIMAL(5,1)) \n"
			+ "from table(MON_GET_MEMORY_POOL(NULL, NULL, -2)) p \n"
			+ "group by p.MEMORY_SET_TYPE, p.DB_NAME, p.MEMORY_POOL_TYPE, p.MEMBER \n"
			+ "union all \n"
			+ "select \n"
			+ "     'LIMIT' \n"
			+ "    ,'INSTANCE' \n"
			+ "    ,'' \n"
			+ "    ,'INSTANCE_MEMORY' \n"
			+ "    ,s.MEMBER \n"
			+ "    ,CAST(count(*) AS INTEGER) \n"
			+ "    ,CAST(sum(s.MEMORY_SET_USED)      / 1024.0 AS DECIMAL(15,1)) \n"
			+ "    ,CAST(sum(s.MEMORY_SET_USED_HWM)  / 1024.0 AS DECIMAL(15,1)) \n"
			+ "    ,CAST(sum(s.MEMORY_SET_COMMITTED) / 1024.0 AS DECIMAL(15,1)) \n"
			+ "    ,CAST(max(i.LIMIT_KB)             / 1024.0 AS DECIMAL(15,1)) \n"
			+ "    ,CAST(sum(s.MEMORY_SET_COMMITTED) * 100.0 / NULLIF(max(i.LIMIT_KB), 0) AS DECIMAL(5,1)) \n"
			+ "from s \n"
			+ "cross join (select max(CASE WHEN TRANSLATE(c.VALUE, '', '0123456789') = '' AND c.VALUE <> '' THEN CAST(c.VALUE AS BIGINT) * 4 ELSE 0 END) as LIMIT_KB \n"
			+ "            from SYSIBMADM.DBMCFG c where c.NAME = 'instance_memory') i \n"
			+ "group by s.MEMBER \n"
			+ "";

		return sql;
	}

	@Override
	public boolean checkDependsOnOther(DbxConnection conn)
	{
		return Db2CmUtils.checkFunctionsAreExecutable(this, conn, "MON_GET_MEMORY_SET(NULL, NULL, -2)", "MON_GET_MEMORY_POOL(NULL, NULL, -2)");
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addTable(CM_NAME, "The memory of the Db2 instance: memory sets, memory pools, and the total against INSTANCE_MEMORY.");

			mtd.addColumn(CM_NAME, "ROW_TYPE",         "<html>SET = a memory set. POOL = the memory pools of one kind in a set (summed). LIMIT = all memory sets against INSTANCE_MEMORY.</html>");
			mtd.addColumn(CM_NAME, "MEMORY_SET_TYPE",  "<html>DBMS (the instance), DATABASE (shared memory of a database), APPLICATION (shared memory of the applications of a database), PRIVATE (the agents), FMP (fenced routines).</html>");
			mtd.addColumn(CM_NAME, "DB_NAME",          "<html>The database of a DATABASE or APPLICATION memory set. Empty for the instance level sets.</html>");
			mtd.addColumn(CM_NAME, "MEMORY_POOL_TYPE", "<html>The kind of memory pool, for example BP (buffer pools), PACKAGE_CACHE, LOCK_MGR (the lock list), SHARED_SORT, CAT_CACHE (catalog cache), UTILITY, APPLICATION. Empty for a SET row.</html>");
			mtd.addColumn(CM_NAME, "MEMBER",           "<html>The member (0 if this is not pureScale or DPF).</html>");
			mtd.addColumn(CM_NAME, "POOL_COUNT",       "<html>Number of memory pools of this kind (for example one per buffer pool, or one per agent).</html>");
			mtd.addColumn(CM_NAME, "USED_MB",          "<html>Memory used, in MB.</html>");
			mtd.addColumn(CM_NAME, "USED_HWM_MB",      "<html>The most memory used (high water mark), in MB. For POOL rows: the sum of the high water marks of the pools.</html>");
			mtd.addColumn(CM_NAME, "COMMITTED_MB",     "<html>Memory committed (taken from the OS) by the set, in MB.</html>");
			mtd.addColumn(CM_NAME, "SIZE_MB",          "<html>For a SET: the memory reserved for the set, in MB. For LIMIT: INSTANCE_MEMORY, in MB (with AUTOMATIC: the limit Db2 computed).</html>");
			mtd.addColumn(CM_NAME, "USED_PCT",         "<html>For LIMIT: the committed memory of all sets, in percent of INSTANCE_MEMORY.</html>");
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
		// Committed MB per memory set (and database)
		//--------------------------------------------------------
		if (GRAPH_NAME_SET_MB.equals(tgdp.getName()))
		{
			Map<String, Double> map = new LinkedHashMap<>();
			for (int r = 0; r < this.getAbsRowCount(); r++)
			{
				if ( ! "SET".equals(this.getAbsString(r, "ROW_TYPE")) )
					continue;

				String dbname = this.getAbsString(r, "DB_NAME");
				String label  = this.getAbsString(r, "MEMORY_SET_TYPE") + (StringUtil.hasValue(dbname) ? " " + dbname : "");
				Double val    = this.getAbsValueAsDouble(r, "COMMITTED_MB");
				map.merge(label, val == null ? 0d : val, Double::sum);
			}
			if ( ! map.isEmpty() )
				tgdp.setData(this.getTimestamp(), map);
		}

		//--------------------------------------------------------
		// Used MB per memory pool type (and database), the 15 biggest
		//--------------------------------------------------------
		if (GRAPH_NAME_POOL_MB.equals(tgdp.getName()))
		{
			List<Integer> rowIds = new ArrayList<>();
			for (int r = 0; r < this.getAbsRowCount(); r++)
				if ("POOL".equals(this.getAbsString(r, "ROW_TYPE")) && this.getAbsValueAsDouble(r, "USED_MB") != null)
					rowIds.add(r);
			rowIds.sort((r1, r2) -> this.getAbsValueAsDouble(r2, "USED_MB").compareTo(this.getAbsValueAsDouble(r1, "USED_MB")));

			Map<String, Double> map = new LinkedHashMap<>();
			for (int i = 0; i < Math.min(15, rowIds.size()); i++)
			{
				int    r      = rowIds.get(i);
				String dbname = this.getAbsString(r, "DB_NAME");
				String label  = this.getAbsString(r, "MEMORY_POOL_TYPE") + (StringUtil.hasValue(dbname) ? " " + dbname : "");
				map.merge(label, this.getAbsValueAsDouble(r, "USED_MB"), Double::sum);
			}
			if ( ! map.isEmpty() )
				tgdp.setData(this.getTimestamp(), map);
		}

		//--------------------------------------------------------
		// Committed memory in percent of INSTANCE_MEMORY (the highest of the members)
		//--------------------------------------------------------
		if (GRAPH_NAME_INST_PCT.equals(tgdp.getName()))
		{
			Double max = null;
			for (int r = 0; r < this.getAbsRowCount(); r++)
			{
				Double val = "LIMIT".equals(this.getAbsString(r, "ROW_TYPE")) ? this.getAbsValueAsDouble(r, "USED_PCT") : null;
				if (val != null && (max == null || val > max))
					max = val;
			}
			tgdp.setDataPoint(this.getTimestamp(), new Double[] { max == null ? 0d : max });
		}
	}
}
