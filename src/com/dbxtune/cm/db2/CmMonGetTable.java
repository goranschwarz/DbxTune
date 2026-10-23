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
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.cm.CmChartDescriptor;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.config.dict.MonTablesDictionary;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.utils.Configuration;

/**
 * Activity per table: scans, rows read/inserted/updated/deleted, overflows, lock waits, buffer pool reads.
 * <p>
 * MON_GET_TABLE only has the tables that have been used since the database was activated. Only the most active ones are kept (property sample.topRows).
 *
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmMonGetTable
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmMonGetTable.class.getSimpleName();
	public static final String   SHORT_NAME       = "Table Activity";
	public static final String   HTML_DESC        =
		"<html>" +
		"<h4>Table Activity</h4>" +
		"Activity per table (and data partition): table scans, rows read / inserted / updated / deleted, overflow records, lock waits, buffer pool reads.<br>" +
		"<br>" +
		"Only the tables that have been used since the database was activated are there, and only the most active ones are kept (by rows read + modified; property <code>" + CM_NAME + ".sample.topRows</code>).<br>" +
		"<b>Tip</b>: TABLE_SCANS together with a high ROWS_READ means a missing (or unused) index. OVERFLOW_ACCESSES that grow means the table needs a REORG.<br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"MON_GET_TABLE", CM_NAME};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"TABLE_SCANS",
		"ROWS_READ",
		"ROWS_INSERTED",
		"ROWS_UPDATED",
		"ROWS_DELETED",
		"NO_CHANGE_UPDATES",
		"OVERFLOW_ACCESSES",
		"OVERFLOW_CREATES",
		"PAGE_REORGS",
		"LOCK_WAITS",
		"LOCK_WAIT_TIME",
		"LOCK_ESCALS",
		"OBJECT_DATA_L_READS",
		"OBJECT_DATA_P_READS",
		"DIRECT_READS",
		"DIRECT_WRITES"
	};

	// The counters starts from 0 when the database is activated again
	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = 60; //CountersModel.DEFAULT_sqlQueryTimeout;

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

		return new CmMonGetTable(counterController, guiController);
	}

	public CmMonGetTable(ICounterController counterController, IGuiController guiController)
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
	public static final int     DEFAULT_sample_topRows = 500;

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

		list.add(new CmSettingsHelper("Top Rows", PROPKEY_sample_topRows, Integer.class, conf.getIntProperty(PROPKEY_sample_topRows, DEFAULT_sample_topRows), DEFAULT_sample_topRows, "Only keep the ## most active tables (by rows read + inserted + updated + deleted since the database was activated). 0 = all." ));

		return list;
	}

	@Override
	public String[] getDependsOnConfigForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return NEED_CONFIG;
	}

	@Override
	public boolean checkDependsOnOther(DbxConnection conn)
	{
		return Db2CmUtils.checkFunctionsAreExecutable(this, conn, "MON_GET_TABLE('', '', -2)");
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addTable(CM_NAME,  "Activity per table (MON_GET_TABLE), the most active tables.");

			mtd.addColumn(CM_NAME, "TABSCHEMA",           "<html>Schema of the table.</html>");
			mtd.addColumn(CM_NAME, "TABNAME",             "<html>Table name.</html>");
			mtd.addColumn(CM_NAME, "DATA_PARTITION_ID",   "<html>The data partition of a partitioned table, -1 for a table that is not partitioned.</html>");
			mtd.addColumn(CM_NAME, "MEMBER",              "<html>The database member (0 if this is not pureScale or DPF).</html>");
			mtd.addColumn(CM_NAME, "TAB_TYPE",            "<html>USER_TABLE, CATALOG_TABLE, TEMP_TABLE (declared temporary)...</html>");
			mtd.addColumn(CM_NAME, "TAB_ORGANIZATION",    "<html>R = row organized, C = column organized (BLU).</html>");
			mtd.addColumn(CM_NAME, "TABLE_SCANS",         "<html>Number of scans of the table (not via an index). Together with a high ROWS_READ: a missing index?</html>");
			mtd.addColumn(CM_NAME, "ROWS_READ",           "<html>Rows read from the table.</html>");
			mtd.addColumn(CM_NAME, "ROWS_INSERTED",       "<html>Rows inserted.</html>");
			mtd.addColumn(CM_NAME, "ROWS_UPDATED",        "<html>Rows updated.</html>");
			mtd.addColumn(CM_NAME, "ROWS_DELETED",        "<html>Rows deleted.</html>");
			mtd.addColumn(CM_NAME, "NO_CHANGE_UPDATES",   "<html>Updates that did not change any value.</html>");
			mtd.addColumn(CM_NAME, "OVERFLOW_ACCESSES",   "<html>Reads of overflow records (a row that grew and was moved to another page: two page reads instead of one). Growing: the table needs a REORG.</html>");
			mtd.addColumn(CM_NAME, "OVERFLOW_CREATES",    "<html>Overflow records that were created.</html>");
			mtd.addColumn(CM_NAME, "PAGE_REORGS",         "<html>Page reorganizations (the free space in a page was compacted to fit a row).</html>");
			mtd.addColumn(CM_NAME, "LOCK_WAITS",          "<html>Times an application waited for a lock on this table.</html>");
			mtd.addColumn(CM_NAME, "LOCK_WAIT_TIME",      "<html>Time spent waiting for locks on this table, in milliseconds.</html>");
			mtd.addColumn(CM_NAME, "LOCK_ESCALS",         "<html>Lock escalations on this table (many row locks became a table lock).</html>");
			mtd.addColumn(CM_NAME, "OBJECT_DATA_L_READS", "<html>Logical reads of data pages (from the buffer pool).</html>");
			mtd.addColumn(CM_NAME, "OBJECT_DATA_P_READS", "<html>Physical reads of data pages (from disk).</html>");
			mtd.addColumn(CM_NAME, "DIRECT_READS",        "<html>Direct reads (LOB, LONG data).</html>");
			mtd.addColumn(CM_NAME, "DIRECT_WRITES",       "<html>Direct writes (LOB, LONG data).</html>");
			mtd.addColumn(CM_NAME, "DATA_MB",             "<html>Size of the data object, in MB (logical pages * page size).</html>");
			mtd.addColumn(CM_NAME, "INDEX_MB",            "<html>Size of the index object (all indexes of the table), in MB.</html>");
			mtd.addColumn(CM_NAME, "LOB_MB",              "<html>Size of the LOB and LONG objects, in MB.</html>");
			mtd.addColumn(CM_NAME, "STATS_ROWS_MODIFIED", "<html>Rows modified since the last RUNSTATS (the statistics may be old if this is high compared to the number of rows).</html>");
			mtd.addColumn(CM_NAME, "CARD",                "<html>Number of rows in the table, from the last RUNSTATS (SYSCAT.TABLES.CARD). -1 = no statistics.</html>");
			mtd.addColumn(CM_NAME, "STATS_TIME",          "<html>When RUNSTATS was done the last time (SYSCAT.TABLES.STATS_TIME). NULL = never.</html>");
			mtd.addColumn(CM_NAME, "TBSP_ID",             "<html>The table space (id) of the data.</html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e);
		}
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("TABSCHEMA");
		pkCols.add("TABNAME");
		pkCols.add("DATA_PARTITION_ID");
		pkCols.add("MEMBER");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		int topRows = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_sample_topRows, DEFAULT_sample_topRows);

		// NOTE: The table names have trailing blanks in MON_GET_TABLE, and DATA_PARTITION_ID is NULL for a table that is not partitioned (it's part of the PK, so -1)
		// NOTE: The sizes are in logical pages, the page size comes from the table space
		// NOTE: A table that is being created (not committed yet) can show up with NULL as its name
		String sql = ""
			+ "select /* ${cmCollectorName} */ \n"
			+ "     RTRIM(t.TABSCHEMA)                    as TABSCHEMA \n"
			+ "    ,RTRIM(t.TABNAME)                      as TABNAME \n"
			+ "    ,COALESCE(t.DATA_PARTITION_ID, -1)     as DATA_PARTITION_ID \n"
			+ "    ,t.MEMBER \n"
			+ "    ,t.TAB_TYPE \n"
			+ "    ,t.TAB_ORGANIZATION \n"
			+ "    ,t.TABLE_SCANS \n"
			+ "    ,t.ROWS_READ \n"
			+ "    ,t.ROWS_INSERTED \n"
			+ "    ,t.ROWS_UPDATED \n"
			+ "    ,t.ROWS_DELETED \n"
			+ "    ,t.NO_CHANGE_UPDATES \n"
			+ "    ,t.OVERFLOW_ACCESSES \n"
			+ "    ,t.OVERFLOW_CREATES \n"
			+ "    ,t.PAGE_REORGS \n"
			+ "    ,t.LOCK_WAITS \n"
			+ "    ,t.LOCK_WAIT_TIME \n"
			+ "    ,t.LOCK_ESCALS \n"
			+ "    ,t.OBJECT_DATA_L_READS \n"
			+ "    ,t.OBJECT_DATA_P_READS \n"
			+ "    ,t.DIRECT_READS \n"
			+ "    ,t.DIRECT_WRITES \n"
			+ "    ,CAST((t.DATA_OBJECT_L_PAGES + t.COL_OBJECT_L_PAGES + t.XDA_OBJECT_L_PAGES) * ts.PAGESIZE / 1048576.0 AS DECIMAL(15,1)) as DATA_MB \n"
			+ "    ,CAST(t.INDEX_OBJECT_L_PAGES * ts.PAGESIZE / 1048576.0 AS DECIMAL(15,1))                                                 as INDEX_MB \n"
			+ "    ,CAST((t.LOB_OBJECT_L_PAGES + t.LONG_OBJECT_L_PAGES) * ts.PAGESIZE / 1048576.0 AS DECIMAL(15,1))                         as LOB_MB \n"
			+ "    ,t.STATS_ROWS_MODIFIED \n"
			+ "    ,st.CARD \n"
			+ "    ,st.STATS_TIME \n"
			+ "    ,t.TBSP_ID \n"
			+ "from table(MON_GET_TABLE('', '', -2)) t \n"
			+ "left join SYSCAT.TABLESPACES ts on ts.TBSPACEID = t.TBSP_ID \n"
			+ "left join SYSCAT.TABLES      st on st.TABSCHEMA = t.TABSCHEMA and st.TABNAME = t.TABNAME \n"
			+ "where t.TABSCHEMA is not null and t.TABNAME is not null \n"
			+ "order by t.ROWS_READ + t.ROWS_INSERTED + t.ROWS_UPDATED + t.ROWS_DELETED desc \n"
			+ (topRows > 0 ? "fetch first " + topRows + " rows only \n" : "")
			+ "";

		return sql;
	}

	@Override
	public List<CmChartDescriptor> createChartDescriptors()
	{
		List<CmChartDescriptor> list = new ArrayList<>();

		list.add(new CmChartDescriptor()
			.id("rows-read")
			.title("Rows Read per Table (the most read tables)")
			.chartType(CmChartDescriptor.CHART_TYPE_DUAL_PIE_BAR)
			.labelColumn("TABNAME")
			.labelPrefixColumns("TABSCHEMA")
			.valueColumns("ROWS_READ")
			.seriesLabels("Rows Read")
			.skipZeroRows(true)
			.maxItems(15)
			.pieOtherLimit(0.02));

		list.add(new CmChartDescriptor()
			.id("rows-changed")
			.title("Rows Inserted, Updated and Deleted per Table")
			.chartType(CmChartDescriptor.CHART_TYPE_STACKED_BAR)
			.labelColumn("TABNAME")
			.labelPrefixColumns("TABSCHEMA")
			.valueColumns("ROWS_INSERTED", "ROWS_UPDATED", "ROWS_DELETED")
			.seriesLabels("Inserted", "Updated", "Deleted")
			.skipZeroRows(true)
			.maxItems(15));

		return list;
	}
}
