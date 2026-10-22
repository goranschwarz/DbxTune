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
package com.dbxtune.cm.mysql;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.naming.NameNotFoundException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.central.pcs.CentralPersistReader;
import com.dbxtune.cm.CmChartDescriptor;
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
public class CmTableSize
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmTableSize.class.getSimpleName();
	public static final String   SHORT_NAME       = "Table Size";
	public static final String   HTML_DESC        =
		"<html>" +
		"How large the InnoDB tables are: the file size, data, indexes and an estimate of the number of rows. " +
		"(information_schema.INNODB_TABLES, INNODB_TABLESPACES and INNODB_TABLESTATS, they need the PROCESS privilege, but no access to the tables)<br>" +
		"<br>" +
		"Only the 200 largest tables (property '" + CM_NAME + ".sample.topRows'), once an hour. Diff is the growth since the previous sample.<br>" +
		"<code>SchemaTotalMb</code> and the graph are for ALL tables in the schema.<br>" +
		"<br>" +
		"<b>Note</b>: The rows and the data/index sizes are the InnoDB statistics, an estimate. They are NULL for a table that has not been used since the server started.<br>" +
		"<b>Note</b>: The file size is only known for tables in their own file (innodb_file_per_table, the default).<br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {CM_NAME, "INNODB_TABLESPACES"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"RowsEstimateDiff",
		"FileSizeMb",
		"DataMb",
		"IndexMb"
		};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = false; // a table can shrink (truncate, optimize)
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 3600; // once an hour
	public static final int      DEFAULT_QUERY_TIMEOUT          = 60;

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

		return new CmTableSize(counterController, guiController);
	}

	public CmTableSize(ICounterController counterController, IGuiController guiController)
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
	public static final String  PROPKEY_sample_topRows = CM_NAME + ".sample.topRows";
	public static final int     DEFAULT_sample_topRows = 200;

	public static final String GRAPH_NAME_SCHEMA_SIZE_MB = "SchemaSizeMb";

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

		list.add(new CmSettingsHelper("Max Number of Rows", PROPKEY_sample_topRows, Integer.class, conf.getIntProperty(PROPKEY_sample_topRows, DEFAULT_sample_topRows), DEFAULT_sample_topRows, "Only get the ## largest tables. 0 = all." ));

		return list;
	}

	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_SCHEMA_SIZE_MB,
			"Size per Schema, in MB",    // Menu CheckBox text
			"Size per Schema, in MB (" + SHORT_NAME + ")", // Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null,
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.SPACE,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("SchemaName");
		pkCols.add("TableName");

		return pkCols;
	}

	/** If we can't read the views: disable the CM, and tell what to do about it (MariaDB has other names for them) */
	@Override
	public boolean checkDependsOnOther(DbxConnection conn)
	{
		return MySqlCmUtils.checkViewsAreReadable(this, conn, "information_schema.INNODB_TABLES", "information_schema.INNODB_TABLESPACES", "information_schema.INNODB_TABLESTATS");
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		int topRows = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_sample_topRows, DEFAULT_sample_topRows);

		// The InnoDB name is 'schema/table' (a partition: 'schema/table#p#partition'), in the file name encoding (special characters are written as @00xx)
		// The statistics are only valid if they are 'Initialized' (the table has been opened since the server started), otherwise they are 0: then NULL
		// The size of a table: the file size (file per table), otherwise the pages in the statistics
		// SchemaTotalMb / SchemaTables: a window function is evaluated before the LIMIT, so it's for ALL tables in the schema
		// NOTE: The row counts are DECIMAL, not BIGINT: DbxTune reads a NULL BIGINT as 0, and here NULL means "not known"
		String size     = "COALESCE(ts.FILE_SIZE, (st.CLUST_INDEX_SIZE + st.OTHER_INDEX_SIZE) * @@innodb_page_size)";
		String statsOk  = "st.STATS_INITIALIZED = 'Initialized'";
		String schema   = "SUBSTRING_INDEX(t.NAME, '/', 1)";

		String sql = ""
			+ "select /* ${cmCollectorName} */ \n"
			+ "     " + schema + "                                                                          as SchemaName \n"
			+ "    ,SUBSTRING_INDEX(t.NAME, '/', -1)                                                        as TableName \n"
			+ "    ,CAST(IF(" + statsOk + ", st.NUM_ROWS, NULL) as DECIMAL(20,0))                            as RowsEstimate \n"
			+ "    ,CAST(IF(" + statsOk + ", st.NUM_ROWS, NULL) as DECIMAL(20,0))                            as RowsEstimateDiff \n"
			+ "    ,CAST(ts.FILE_SIZE / 1048576.0 as DECIMAL(15,1))                                         as FileSizeMb \n"
			+ "    ,CAST(IF(" + statsOk + ", st.CLUST_INDEX_SIZE * @@innodb_page_size, NULL) / 1048576.0 as DECIMAL(15,1)) as DataMb \n"
			+ "    ,CAST(IF(" + statsOk + ", st.OTHER_INDEX_SIZE * @@innodb_page_size, NULL) / 1048576.0 as DECIMAL(15,1)) as IndexMb \n"
			+ "    ,CAST(IF(" + statsOk + ", ts.FILE_SIZE - (st.CLUST_INDEX_SIZE + st.OTHER_INDEX_SIZE) * @@innodb_page_size, NULL) / 1048576.0 as DECIMAL(15,1)) as UnusedMb \n"
			+ "    ,CAST(IF(" + statsOk + ", st.MODIFIED_COUNTER, NULL) as DECIMAL(20,0))                    as ModifiedRows \n"
			+ "    ,COALESCE(ts.SPACE_TYPE, 'General')                                                      as SpaceType \n"
			+ "    ,t.ROW_FORMAT                                                                            as RowFormat \n"
			+ "    ,CAST(SUM(" + size + ") OVER (PARTITION BY " + schema + ") / 1048576.0 as DECIMAL(15,1))  as SchemaTotalMb \n"
			+ "    ,COUNT(*) OVER (PARTITION BY " + schema + ")                                             as SchemaTables \n"
			+ "from information_schema.INNODB_TABLES t \n"
			+ "left join information_schema.INNODB_TABLESPACES ts on ts.SPACE = t.SPACE and ts.SPACE_TYPE = 'Single' \n"
			+ "left join information_schema.INNODB_TABLESTATS  st on st.TABLE_ID = t.TABLE_ID \n"
			+ "where t.NAME like '%/%' \n"
			+ "  and " + schema + " not in ('mysql', 'sys') \n"
			+ "order by " + size + " desc \n"
			+ (topRows > 0 ? "limit " + topRows + " \n" : "")
			+ "";

		return sql;
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			mtd.addTable(CM_NAME, "How large the InnoDB tables are: the file size, data, indexes and an estimate of the number of rows.");

			mtd.addColumn(CM_NAME, "SchemaName",       "<html>The schema (database).</html>");
			mtd.addColumn(CM_NAME, "TableName",        "<html>The table. A partition is shown as <code>table#p#partition</code>.<br>Special characters are written as <code>@00xx</code> (the InnoDB file name encoding).</html>");
			mtd.addColumn(CM_NAME, "RowsEstimate",     "<html>Number of rows, an estimate from the InnoDB statistics. NULL if the table has not been used since the server started.</html>");
			mtd.addColumn(CM_NAME, "RowsEstimateDiff", "<html>Same as RowsEstimate, but as a Diff: how many rows the table has grown since the previous sample.</html>");
			mtd.addColumn(CM_NAME, "FileSizeMb",       "<html>The size of the table's file (.ibd), in MB. Only for a table in its own file (innodb_file_per_table).</html>");
			mtd.addColumn(CM_NAME, "DataMb",           "<html>The data (the clustered index, which is the table), in MB, from the statistics.</html>");
			mtd.addColumn(CM_NAME, "IndexMb",          "<html>The other indexes, in MB, from the statistics.</html>");
			mtd.addColumn(CM_NAME, "UnusedMb",         "<html>FileSizeMb - DataMb - IndexMb: about how much of the file is not used (free pages, after deletes).<br>If it is large: <code>OPTIMIZE TABLE</code> gives it back to the file system (it rebuilds the table).</html>");
			mtd.addColumn(CM_NAME, "ModifiedRows",     "<html>Rows changed since the statistics were last updated. InnoDB updates them when about 10% of the rows has changed.</html>");
			mtd.addColumn(CM_NAME, "SpaceType",        "<html>Single: the table has its own file. General: the table is in a general tablespace, or in the system tablespace (ibdata1).</html>");
			mtd.addColumn(CM_NAME, "RowFormat",        "<html>Dynamic (the default), Compact, Redundant or Compressed.</html>");
			mtd.addColumn(CM_NAME, "SchemaTotalMb",    "<html>All tables in the schema, in MB (not only the ones shown).</html>");
			mtd.addColumn(CM_NAME, "SchemaTables",     "<html>Number of tables (and partitions) in the schema.</html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e);
		}
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		// The size of every schema we have a table for (SchemaTotalMb is the same on all rows of a schema), sorted by name, so the lines keep their order
		if (GRAPH_NAME_SCHEMA_SIZE_MB.equals(tgdp.getName()))
		{
			Map<String, Double> dataMap = new TreeMap<>();

			for (int r = 0; r < this.size(); r++)
			{
				String schemaName = this.getAbsString        (r, "SchemaName");
				Double totalMb    = this.getAbsValueAsDouble (r, "SchemaTotalMb");

				if (schemaName != null && totalMb != null)
					dataMap.put(schemaName, totalMb);
			}

			if (dataMap.size() > 0)
				tgdp.setData(this.getTimestamp(), dataMap);
		}
	}

	@Override
	public List<CmChartDescriptor> createChartDescriptors()
	{
		List<CmChartDescriptor> list = new ArrayList<>();

		list.add(new CmChartDescriptor()
			.id("size-by-schema")
			.title("Table Size by Schema (MB)")
			.chartType(CmChartDescriptor.CHART_TYPE_DUAL_PIE_BAR)
			.labelColumn("TableName")
			.groupByColumn("SchemaName")
			.valueColumns("FileSizeMb")
			.seriesLabels("File Size (MB)")
			.skipZeroRows(true)
			.pieOtherLimit(0.05)
			.splitRatio(0.6));

		list.add(new CmChartDescriptor()
			.id("biggest-tables")
			.title("Biggest Tables (MB)")
			.chartType(CmChartDescriptor.CHART_TYPE_DUAL_PIE_BAR)
			.labelColumn("TableName")
			.labelPrefixColumns("SchemaName")
			.valueColumns("FileSizeMb")
			.seriesLabels("File Size (MB)")
			.skipZeroRows(true)
			.maxItems(15)
			.pieOtherLimit(0.02));

		return list;
	}
}
