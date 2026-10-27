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
 * Activity per index: scans, index-only scans, key updates, page splits, buffer pool reads.
 * <p>
 * MON_GET_INDEX only has the indexes of tables that have been used since the database was activated. Only the most used ones are kept (property sample.topRows).
 *
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmMonGetIndex
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmMonGetIndex.class.getSimpleName();
	public static final String   SHORT_NAME       = "Index Activity";
	public static final String   HTML_DESC        =
		"<html>" +
		"<h4>Index Activity</h4>" +
		"Activity per index: index scans, index-only scans, key updates, page splits, pseudo deletes, buffer pool reads.<br>" +
		"<br>" +
		"Only the indexes of tables that have been used since the database was activated are there, and only the most used ones are kept (by logical reads; property <code>" + CM_NAME + ".sample.topRows</code>).<br>" +
		"<b>LASTUSED</b> (from the catalog) is the last day the index was used, also before the database was activated. An old date (or 0001-01-01 = never) on a non-unique index: maybe it is not needed.<br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"MON_GET_INDEX", CM_NAME};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"INDEX_SCANS",
		"INDEX_ONLY_SCANS",
		"INDEX_JUMP_SCANS",
		"KEY_UPDATES",
		"INCLUDE_COL_UPDATES",
		"PSEUDO_DELETES",
		"DEL_KEYS_CLEANED",
		"ROOT_NODE_SPLITS",
		"INT_NODE_SPLITS",
		"BOUNDARY_LEAF_NODE_SPLITS",
		"NONBOUNDARY_LEAF_NODE_SPLITS",
		"PAGE_ALLOCATIONS",
		"PSEUDO_EMPTY_PAGES",
		"EMPTY_PAGES_REUSED",
		"EMPTY_PAGES_DELETED",
		"PAGES_MERGED",
		"OBJECT_INDEX_L_READS",
		"OBJECT_INDEX_P_READS"
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

		return new CmMonGetIndex(counterController, guiController);
	}

	public CmMonGetIndex(ICounterController counterController, IGuiController guiController)
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
	public static final int     DEFAULT_sample_topRows = 1000;

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

		list.add(new CmSettingsHelper("Top Rows", PROPKEY_sample_topRows, Integer.class, conf.getIntProperty(PROPKEY_sample_topRows, DEFAULT_sample_topRows), DEFAULT_sample_topRows, "Only keep the ## most used indexes (by logical reads since the database was activated). 0 = all." ));

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
		return Db2CmUtils.checkFunctionsAreExecutable(this, conn, "MON_GET_INDEX('', '', -2)");
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addTable(CM_NAME,  "Activity per index (MON_GET_INDEX), the most used indexes.");

			mtd.addColumn(CM_NAME, "TABSCHEMA",                    "<html>Schema of the table.</html>");
			mtd.addColumn(CM_NAME, "TABNAME",                      "<html>Table name.</html>");
			mtd.addColumn(CM_NAME, "INDNAME",                      "<html>Index name (SYSCAT.INDEXES).</html>");
			mtd.addColumn(CM_NAME, "IID",                          "<html>Index id within the table.</html>");
			mtd.addColumn(CM_NAME, "DATA_PARTITION_ID",            "<html>The data partition of a partitioned table, -1 for a table that is not partitioned.</html>");
			mtd.addColumn(CM_NAME, "MEMBER",                       "<html>The database member (0 if this is not pureScale or DPF).</html>");
			mtd.addColumn(CM_NAME, "UNIQUERULE",                   "<html>P = primary key, U = unique, D = duplicates allowed.</html>");
			mtd.addColumn(CM_NAME, "COLNAMES",                     "<html>The key columns (+ = ascending, - = descending).</html>");
			mtd.addColumn(CM_NAME, "LASTUSED",                     "<html>The last day the index was used (SYSCAT.INDEXES.LASTUSED, updated by Db2 with a delay). 0001-01-01 = never used.</html>");
			mtd.addColumn(CM_NAME, "NLEAF",                        "<html>Number of leaf pages.</html>");
			mtd.addColumn(CM_NAME, "NLEVELS",                      "<html>Number of levels (the height of the index).</html>");
			mtd.addColumn(CM_NAME, "INDEX_SCANS",                  "<html>Number of scans of the index.</html>");
			mtd.addColumn(CM_NAME, "INDEX_ONLY_SCANS",             "<html>Scans that did not have to read the table (all columns that were needed are in the index).</html>");
			mtd.addColumn(CM_NAME, "INDEX_JUMP_SCANS",             "<html>Jump scans (a scan that skips parts of the index, when the first key column is not in the WHERE clause).</html>");
			mtd.addColumn(CM_NAME, "KEY_UPDATES",                  "<html>Updates of a key value (costly: a delete and an insert in the index).</html>");
			mtd.addColumn(CM_NAME, "INCLUDE_COL_UPDATES",          "<html>Updates of an INCLUDE column.</html>");
			mtd.addColumn(CM_NAME, "PSEUDO_DELETES",               "<html>Keys marked as deleted (cleaned up later).</html>");
			mtd.addColumn(CM_NAME, "DEL_KEYS_CLEANED",             "<html>Pseudo deleted keys that were cleaned up.</html>");
			mtd.addColumn(CM_NAME, "ROOT_NODE_SPLITS",             "<html>Splits of the root page (the index got one more level).</html>");
			mtd.addColumn(CM_NAME, "INT_NODE_SPLITS",              "<html>Splits of non-leaf pages.</html>");
			mtd.addColumn(CM_NAME, "BOUNDARY_LEAF_NODE_SPLITS",    "<html>Splits of the first or last leaf page (inserts of ascending or descending keys).</html>");
			mtd.addColumn(CM_NAME, "NONBOUNDARY_LEAF_NODE_SPLITS", "<html>Splits of other leaf pages. Many: think about PCTFREE, or a REORG of the index.</html>");
			mtd.addColumn(CM_NAME, "PAGE_ALLOCATIONS",             "<html>Pages allocated for the index.</html>");
			mtd.addColumn(CM_NAME, "PSEUDO_EMPTY_PAGES",           "<html>Leaf pages that only has pseudo deleted keys.</html>");
			mtd.addColumn(CM_NAME, "EMPTY_PAGES_REUSED",           "<html>Empty pages that were reused.</html>");
			mtd.addColumn(CM_NAME, "EMPTY_PAGES_DELETED",          "<html>Empty pages that were deleted.</html>");
			mtd.addColumn(CM_NAME, "PAGES_MERGED",                 "<html>Pages that were merged (online index defragmentation).</html>");
			mtd.addColumn(CM_NAME, "OBJECT_INDEX_L_READS",         "<html>Logical reads of index pages (from the buffer pool).</html>");
			mtd.addColumn(CM_NAME, "OBJECT_INDEX_P_READS",         "<html>Physical reads of index pages (from disk).</html>");
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
		pkCols.add("IID");
		pkCols.add("DATA_PARTITION_ID");
		pkCols.add("MEMBER");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		int topRows = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_sample_topRows, DEFAULT_sample_topRows);

		// NOTE: The names have trailing blanks in MON_GET_INDEX, and DATA_PARTITION_ID is NULL for a table that is not partitioned (it's part of the PK, so -1)
		String sql = ""
			+ "select /* ${cmCollectorName} */ \n"
			+ "     RTRIM(i.TABSCHEMA)                    as TABSCHEMA \n"
			+ "    ,RTRIM(i.TABNAME)                      as TABNAME \n"
			+ "    ,RTRIM(x.INDNAME)                      as INDNAME \n"
			+ "    ,i.IID \n"
			+ "    ,COALESCE(i.DATA_PARTITION_ID, -1)     as DATA_PARTITION_ID \n"
			+ "    ,i.MEMBER \n"
			+ "    ,x.UNIQUERULE \n"
			+ "    ,CAST(x.COLNAMES AS VARCHAR(640))      as COLNAMES \n"
			+ "    ,x.LASTUSED \n"
			+ "    ,i.NLEAF \n"
			+ "    ,i.NLEVELS \n"
			+ "    ,i.INDEX_SCANS \n"
			+ "    ,i.INDEX_ONLY_SCANS \n"
			+ "    ,i.INDEX_JUMP_SCANS \n"
			+ "    ,i.KEY_UPDATES \n"
			+ "    ,i.INCLUDE_COL_UPDATES \n"
			+ "    ,i.PSEUDO_DELETES \n"
			+ "    ,i.DEL_KEYS_CLEANED \n"
			+ "    ,i.ROOT_NODE_SPLITS \n"
			+ "    ,i.INT_NODE_SPLITS \n"
			+ "    ,i.BOUNDARY_LEAF_NODE_SPLITS \n"
			+ "    ,i.NONBOUNDARY_LEAF_NODE_SPLITS \n"
			+ "    ,i.PAGE_ALLOCATIONS \n"
			+ "    ,i.PSEUDO_EMPTY_PAGES \n"
			+ "    ,i.EMPTY_PAGES_REUSED \n"
			+ "    ,i.EMPTY_PAGES_DELETED \n"
			+ "    ,i.PAGES_MERGED \n"
			+ "    ,i.OBJECT_INDEX_L_READS \n"
			+ "    ,i.OBJECT_INDEX_P_READS \n"
			+ "from table(MON_GET_INDEX('', '', -2)) i \n"
			+ "left join SYSCAT.INDEXES x on x.TABSCHEMA = i.TABSCHEMA and x.TABNAME = i.TABNAME and x.IID = i.IID \n"
			+ "order by i.OBJECT_INDEX_L_READS desc \n"
			+ (topRows > 0 ? "fetch first " + topRows + " rows only \n" : "")
			+ "";

		return sql;
	}
}
