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
import java.util.List;

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

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmSgaStat
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmSgaStat.class.getSimpleName();
	public static final String   SHORT_NAME       = "SGA";
	public static final String   HTML_DESC        =
		"<html>" +
		"The parts of the SGA (the shared memory of the instance): how big they are, how big they have been, and how much is free in the pools.<br>" +
		"<br>" +
		"With automatic memory management (<code>sga_target</code> or <code>memory_target</code>) Oracle moves memory between the buffer cache and the pools.<br>" +
		"<code>OPER_COUNT</code> and <code>LAST_OPER_TYPE</code> shows when that happens (GROW / SHRINK). Many resize operations means that the SGA is too small for the load.<br>" +
		"<code>FREE_MB</code> of the <b>shared pool</b> is the one to look at: when it is (almost) 0 and fragmented, statements fail with 'ORA-04031: unable to allocate ... bytes of shared memory' (which also sends an alarm from the tab 'Alert Log').<br>" +
		"<br>" +
		"The rows 'Fixed SGA Size', 'Redo Buffers', 'Maximum SGA Size' and 'Free SGA Memory Available' are from V$SGAINFO: they are not components that Oracle resizes.<br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_CACHE;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {CM_NAME, "V$SGA_DYNAMIC_COMPONENTS", "V$SGAINFO", "V$SGASTAT"};
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
	@Override public Type    getTemplateLevel()                       { return Type.MEDIUM; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '" + CM_NAME + "'");

		return new CmSgaStat(counterController, guiController);
	}

	public CmSgaStat(ICounterController counterController, IGuiController guiController)
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
	public static final String GRAPH_NAME_COMPONENTS_MB = "SgaComponentsMb";
	public static final String GRAPH_NAME_POOL_FREE_MB  = "SgaPoolFreeMb";

	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_COMPONENTS_MB,
			"SGA, Size of the Components in MB", 	                // Menu CheckBox text
			"SGA Components, Size in MB (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null,
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.MEMORY,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_POOL_FREE_MB,
			"SGA, Free Memory in the Pools in MB", 	                // Menu CheckBox text
			"SGA Pools (shared pool, large pool...), Free Memory in MB (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB, CentralPersistReader.SampleType.MIN_OVER_SAMPLES),
			null,
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.MEMORY,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("COMPONENT");

		return pkCols;
	}

	/** If we can't read the views: disable the CM, and tell what to do about it */
	@Override
	public boolean checkDependsOnOther(DbxConnection conn)
	{
		return OracleCmUtils.checkViewsAreReadable(this, conn, "V$SGA_DYNAMIC_COMPONENTS", "V$SGAINFO", "V$SGASTAT");
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		// The components Oracle can resize (V$SGA_DYNAMIC_COMPONENTS), with the free memory of the pools (V$SGASTAT),
		// and 4 rows from V$SGAINFO for what is not a resizable component (ROW_TYPE = INFO)
		// This looks the same in all versions
		// Note: CAST(... as NUMBER(#,1)) is needed to keep decimals (a NUMBER without precision is treated as a BIGINT)
		String sql = ""
			+ "select \n"
			+ "     CAST('COMPONENT' as VARCHAR2(10))                                               as ROW_TYPE \n"
			+ "    ,c.COMPONENT \n"
			+ "    ,CAST(c.CURRENT_SIZE        / 1048576                 as NUMBER(18,1)) as CURRENT_MB \n"
			+ "    ,CAST(f.FREE_BYTES          / 1048576                 as NUMBER(18,1)) as FREE_MB \n"
			+ "    ,CAST(100 * f.FREE_BYTES / NULLIF(c.CURRENT_SIZE, 0)  as NUMBER(5,1))  as FREE_PCT \n"
			+ "    ,CAST(c.MIN_SIZE            / 1048576                 as NUMBER(18,1)) as MIN_MB \n"
			+ "    ,CAST(c.MAX_SIZE            / 1048576                 as NUMBER(18,1)) as MAX_MB \n"
			+ "    ,CAST(c.USER_SPECIFIED_SIZE / 1048576                 as NUMBER(18,1)) as USER_SPECIFIED_MB \n"
			+ "    ,c.OPER_COUNT \n"
			+ "    ,c.LAST_OPER_TYPE \n"
			+ "    ,c.LAST_OPER_MODE \n"
			+ "    ,c.LAST_OPER_TIME \n"
			+ "from V$SGA_DYNAMIC_COMPONENTS c \n"
			+ "left join (select POOL, sum(BYTES) as FREE_BYTES from V$SGASTAT where NAME = 'free memory' group by POOL) f on f.POOL = c.COMPONENT \n"
			+ "where c.CURRENT_SIZE > 0 or c.MAX_SIZE > 0 \n"
			+ "union all \n"
			+ "select \n"
			+ "     CAST('INFO' as VARCHAR2(10)) \n"
			+ "    ,i.NAME \n"
			+ "    ,CAST(i.BYTES / 1048576 as NUMBER(18,1)) \n"
			+ "    ,CAST(NULL as NUMBER(18,1)) \n"
			+ "    ,CAST(NULL as NUMBER(5,1)) \n"
			+ "    ,CAST(NULL as NUMBER(18,1)) \n"
			+ "    ,CAST(NULL as NUMBER(18,1)) \n"
			+ "    ,CAST(NULL as NUMBER(18,1)) \n"
			+ "    ,0 \n"
			+ "    ,CAST(NULL as VARCHAR2(13)) \n"
			+ "    ,CAST(NULL as VARCHAR2(9)) \n"
			+ "    ,CAST(NULL as DATE) \n"
			+ "from V$SGAINFO i \n"
			+ "where i.NAME in ('Fixed SGA Size', 'Redo Buffers', 'Maximum SGA Size', 'Free SGA Memory Available') \n"
			+ "order by 1, 3 desc \n"
			+ "";

		return sql;
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			mtd.addTable(CM_NAME, "The parts of the SGA: how big they are, how big they have been, and how much is free in the pools.");

			mtd.addColumn(CM_NAME, "ROW_TYPE",          "<html><b>COMPONENT</b> = a part of the SGA that Oracle can resize (from V$SGA_DYNAMIC_COMPONENTS).<br><b>INFO</b> = a fixed part, or a total (from V$SGAINFO).</html>");
			mtd.addColumn(CM_NAME, "COMPONENT",         "<html>Name of the SGA component, for example: 'DEFAULT buffer cache', 'shared pool', 'large pool', 'java pool', 'streams pool'.</html>");
			mtd.addColumn(CM_NAME, "CURRENT_MB",        "<html>Size right now, in MB.</html>");
			mtd.addColumn(CM_NAME, "FREE_MB",           "<html>Free memory in the pool, in MB (V$SGASTAT 'free memory'). Only for the pools (shared pool, large pool, java pool, streams pool).<br><b>Note</b>: free memory in the shared pool can be fragmented, so a big allocation can fail even if there is free memory.</html>");
			mtd.addColumn(CM_NAME, "FREE_PCT",          "<html>FREE_MB in percent of CURRENT_MB.</html>");
			mtd.addColumn(CM_NAME, "MIN_MB",            "<html>The smallest size the component has had since the instance was started, in MB.</html>");
			mtd.addColumn(CM_NAME, "MAX_MB",            "<html>The biggest size the component has had since the instance was started, in MB.</html>");
			mtd.addColumn(CM_NAME, "USER_SPECIFIED_MB", "<html>The size set with a parameter (like <code>shared_pool_size</code>), in MB. With automatic memory management this is the minimum size. 0 = not set.</html>");
			mtd.addColumn(CM_NAME, "OPER_COUNT",        "<html>Number of resize operations (GROW or SHRINK) since the instance was started.</html>");
			mtd.addColumn(CM_NAME, "LAST_OPER_TYPE",    "<html>The last resize operation: GROW, SHRINK, STATIC (never resized), INITIALIZING...</html>");
			mtd.addColumn(CM_NAME, "LAST_OPER_MODE",    "<html>How the last resize operation was started: MANUAL (by a DBA), DEFERRED or IMMEDIATE (by Oracle).</html>");
			mtd.addColumn(CM_NAME, "LAST_OPER_TIME",    "<html>When the last resize operation was started.</html>");
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
		// One "line" for every row that has a size
		//--------------------------------------------------------
		if (GRAPH_NAME_COMPONENTS_MB.equals(tgdp.getName()))
		{
			LinkedHashMap<String, Double> dataMap = new LinkedHashMap<String, Double>();

			for (int r = 0; r < this.size(); r++)
			{
				Double currentMb = this.getAbsValueAsDouble(r, "CURRENT_MB");
				if (currentMb != null && currentMb > 0)
					dataMap.put(this.getAbsString(r, "COMPONENT"), currentMb);
			}

			if (dataMap.size() > 0)
				tgdp.setData(this.getTimestamp(), dataMap);
		}

		//--------------------------------------------------------
		// One "line" for every pool (the rows that has FREE_MB)
		//--------------------------------------------------------
		if (GRAPH_NAME_POOL_FREE_MB.equals(tgdp.getName()))
		{
			LinkedHashMap<String, Double> dataMap = new LinkedHashMap<String, Double>();

			for (int r = 0; r < this.size(); r++)
			{
				Double freeMb = this.getAbsValueAsDouble(r, "FREE_MB");
				if (freeMb != null)
					dataMap.put(this.getAbsString(r, "COMPONENT"), freeMb);
			}

			if (dataMap.size() > 0)
				tgdp.setData(this.getTimestamp(), dataMap);
		}
	}
}
