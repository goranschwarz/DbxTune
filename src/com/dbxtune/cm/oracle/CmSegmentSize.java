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
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.cm.CmChartDescriptor;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CounterSample;
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
import com.dbxtune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmSegmentSize
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmSegmentSize.class.getSimpleName();
	public static final String   SHORT_NAME       = "Segment Size";
	public static final String   HTML_DESC        =
		"<html>" +
		"What takes the space? The biggest segments in the database: tables, indexes, partitions and LOBs.<br>" +
		"<br>" +
		"By default: the 200 biggest segments, sampled once an hour. This can be changed in the options.<br>" +
		"<br>" +
		"<code>TABLE_NAME</code> is the table that the segment belongs to, also for an index and for a LOB (which has a name like <code>SYS_LOB0000012345C00003$$</code>).<br>" +
		"<b>Tip</b>: Sort on <code>TABLE_NAME</code> to see a table together with it's indexes and LOBs.<br>" +
		"<br>" +
		"<b>Note</b>: <code>SIZE_MB</code> is the space that is allocated to the segment. Rows that are deleted does not make it smaller.<br>" +
		"<b>Note</b>: In a Container Database, connect to CDB$ROOT to see all PDBs.<br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_DISK;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {CM_NAME, "DBA_SEGMENTS"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 3600;
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

		return new CmSegmentSize(counterController, guiController);
	}

	public CmSegmentSize(ICounterController counterController, IGuiController guiController)
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
	public static final int     DEFAULT_sample_topRows = 200;

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

		list.add(new CmSettingsHelper("Max Number of Rows", PROPKEY_sample_topRows, Integer.class, conf.getIntProperty(PROPKEY_sample_topRows, DEFAULT_sample_topRows), DEFAULT_sample_topRows, "Only get the ## biggest segments (this to limit how much is fetched and stored on systems with many segments)." ));

		return list;
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		// CON_ID is always 0 in a non-CDB (and in 11.2), in a CDB the same segment name can exist in many containers
		pkCols.add("CON_ID");
		pkCols.add("OWNER");
		pkCols.add("SEGMENT_NAME");
		pkCols.add("PARTITION_NAME");
		pkCols.add("SEGMENT_TYPE");

		return pkCols;
	}

	/** PARTITION_NAME is part of the Primary Key, and it is NULL for a segment that is not a partition: use an empty string */
	@Override
	public Object pkNullValueResolver(CounterSample counterSample, int rsRowNumber, int rsColumnNumber, String colName)
	{
		return "";
	}

	/** If we can't read the views: disable the CM, and tell what to do about it */
	@Override
	public boolean checkDependsOnOther(DbxConnection conn)
	{
		return OracleCmUtils.checkViewsAreReadable(this, conn, "DBA_SEGMENTS", "DBA_INDEXES", "DBA_LOBS");
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		DbmsVersionInfoOracle oraVersionInfo = (DbmsVersionInfoOracle) versionInfo;

		// 11.2 and above: a non-CDB, or when we are connected to a PDB
		String sql = getSql(false);

		// 12.1 and above, connected to CDB$ROOT: the segments in EVERY container
		if (versionInfo.getLongVersion() >= Ver.ver(12,1) && oraVersionInfo.isCdbRoot())
		{
			String sqlCdb = getSql(true);

			// The SQL for 12.1 and above can NOT be tested on all versions: so "dry-run" it, and if it fails use the 11.2 SQL (which works on all versions)
			// conn is null when we just want to "view" the SQL (the CM Properties dialog)
			if (conn == null)
			{
				sql = sqlCdb;
			}
			else
			{
				try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery("select * from (" + sqlCdb + ") where 1=2"))
				{
					sql = sqlCdb;
				}
				catch (SQLException ex)
				{
					_logger.warn(CM_NAME + ": The SQL for a CDB (version " + Ver.versionNumToStr(versionInfo.getLongVersion()) + ") failed, only the segments in CDB$ROOT will be shown. Caught: " + ex.getMessage().trim());
				}
			}
		}

		return sql;
	}

	/**
	 * @param cdb   false = the DBA_ views (one container), true = the CDB_ views (all containers, they have the column CON_ID)
	 */
	private static String getSql(boolean cdb)
	{
		int topRows = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_sample_topRows, DEFAULT_sample_topRows);

		String views   = cdb ? "CDB" : "DBA";
		String conId   = cdb ? "CON_ID, " : "";

		// The inner SELECT (x) is sorted on BYTES, and then the first ## rows of that
		// The table name (for an index or a LOB) comes from DBA_INDEXES and DBA_LOBS, which are read ONCE (materialize)
		// Without that it is up to Oracle if it goes to them once for every row (in CmIndexUsage that was 29 seconds instead of 0.5)
		// Note: CAST(... as NUMBER(18,1)) is needed to keep decimals (a NUMBER without precision is treated as a BIGINT)
		return ""
			+ "with i as (select /*+ materialize */ " + conId + "OWNER, INDEX_NAME, TABLE_NAME \n"
			+ "           from " + views + "_INDEXES) \n"
			+ "    ,l as (select /*+ materialize */ " + conId + "OWNER, SEGMENT_NAME, INDEX_NAME, TABLE_NAME, COLUMN_NAME \n"
			+ "           from " + views + "_LOBS) \n"
			+ "select \n"
			+ "     " + (cdb ? "x.CON_ID" : "CAST(0    as NUMBER(10))")    + " as CON_ID \n"
			+ "    ," + (cdb ? "c.NAME"   : "CAST(NULL as VARCHAR2(128))") + " as CON_NAME \n"
			+ "    ,x.OWNER \n"
			+ "    ,x.SEGMENT_NAME \n"
			+ "    ,x.PARTITION_NAME \n"
			+ "    ,x.SEGMENT_TYPE \n"
			+ "    ,x.TABLESPACE_NAME \n"
			+ "    ,CAST(x.BYTES / 1048576 as NUMBER(18,1)) as SIZE_MB \n"
			+ "    ,x.EXTENTS \n"
			+ "    ,x.BLOCKS \n"
			+ "    ,CAST(case when x.SEGMENT_TYPE like 'TABLE%' or x.SEGMENT_TYPE = 'NESTED TABLE' then x.SEGMENT_NAME \n"
			+ "               when x.SEGMENT_TYPE like 'INDEX%'  then i.TABLE_NAME \n"
			+ "               when x.SEGMENT_TYPE = 'LOBINDEX'   then li.TABLE_NAME \n"
			+ "               when x.SEGMENT_TYPE like 'LOB%'    then l.TABLE_NAME \n"
			+ "          end as VARCHAR2(128)) as TABLE_NAME \n"
			+ "    ,CAST(case when x.SEGMENT_TYPE = 'LOBINDEX'   then li.COLUMN_NAME \n"
			+ "               when x.SEGMENT_TYPE like 'LOB%'    then l.COLUMN_NAME \n"
			+ "          end as VARCHAR2(4000)) as LOB_COLUMN \n"
			+ "from (select * from (select " + conId + "OWNER, SEGMENT_NAME, PARTITION_NAME, SEGMENT_TYPE, TABLESPACE_NAME, BYTES, EXTENTS, BLOCKS \n"
			+ "                     from " + views + "_SEGMENTS \n"
			+ "                     order by BYTES desc) \n"
			+ "      where ROWNUM <= " + topRows + ") x \n"
			+ (cdb ? "left join V$CONTAINERS c on c.CON_ID = x.CON_ID \n" : "")
			+ "left join i    on x.SEGMENT_TYPE like 'INDEX%'                                and i.OWNER  = x.OWNER and i.INDEX_NAME   = x.SEGMENT_NAME" + (cdb ? " and i.CON_ID  = x.CON_ID" : "") + " \n"
			+ "left join l    on x.SEGMENT_TYPE like 'LOB%' and x.SEGMENT_TYPE <> 'LOBINDEX' and l.OWNER  = x.OWNER and l.SEGMENT_NAME = x.SEGMENT_NAME" + (cdb ? " and l.CON_ID  = x.CON_ID" : "") + " \n"
			+ "left join l li on x.SEGMENT_TYPE = 'LOBINDEX'                                 and li.OWNER = x.OWNER and li.INDEX_NAME  = x.SEGMENT_NAME" + (cdb ? " and li.CON_ID = x.CON_ID" : "") + " \n"
			+ "order by x.BYTES desc \n"
			+ "";
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			mtd.addTable(CM_NAME, "What takes the space? The biggest segments in the database: tables, indexes, partitions and LOBs.");

			mtd.addColumn(CM_NAME, "CON_ID",          "<html>Container ID. 0 = This is not a Container Database (or the version is below 12.1), or we are connected to a PDB.</html>");
			mtd.addColumn(CM_NAME, "CON_NAME",        "<html>Name of the Container (PDB) the segment is in. NULL if CON_ID is 0.</html>");
			mtd.addColumn(CM_NAME, "OWNER",           "<html>The schema that owns the segment.</html>");
			mtd.addColumn(CM_NAME, "SEGMENT_NAME",    "<html>Name of the segment: the name of the table or the index. A LOB has a name made by Oracle, see TABLE_NAME and LOB_COLUMN.</html>");
			mtd.addColumn(CM_NAME, "PARTITION_NAME",  "<html>Name of the partition (or subpartition), empty if this is not a partition.</html>");
			mtd.addColumn(CM_NAME, "SEGMENT_TYPE",    "<html>TABLE, INDEX, TABLE PARTITION, INDEX PARTITION, LOBSEGMENT, LOBINDEX, LOB PARTITION, CLUSTER, TYPE2 UNDO...</html>");
			mtd.addColumn(CM_NAME, "TABLESPACE_NAME", "<html>The tablespace that the segment is in.</html>");
			mtd.addColumn(CM_NAME, "SIZE_MB",         "<html>Space in MB that is allocated to the segment.<br><b>Formula</b>: BYTES / 1024 / 1024</html>");
			mtd.addColumn(CM_NAME, "EXTENTS",         "<html>Number of extents that the segment has.</html>");
			mtd.addColumn(CM_NAME, "BLOCKS",          "<html>Number of blocks that are allocated to the segment.</html>");
			mtd.addColumn(CM_NAME, "TABLE_NAME",      "<html>The table that the segment belongs to: the table itself, the table of an index, or the table of a LOB.<br>NULL for other segment types (CLUSTER, TYPE2 UNDO...)</html>");
			mtd.addColumn(CM_NAME, "LOB_COLUMN",      "<html>For a LOB segment (or a LOB index): the column in TABLE_NAME that holds the LOB.</html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e);
		}
	}

	/** Local graphs (beside the table): the biggest segments, and how much each schema holds */
	@Override
	public List<CmChartDescriptor> createChartDescriptors()
	{
		List<CmChartDescriptor> list = new ArrayList<>();

		// The partitions and LOB segments of a table have their own rows: the ones with the same name are added up
		list.add(new CmChartDescriptor()
			.id("seg-biggest")
			.title("Biggest Segments (MB)")
			.chartType(CmChartDescriptor.CHART_TYPE_DUAL_PIE_BAR)
			.labelColumn("SEGMENT_NAME")
			.labelPrefixColumns("CON_NAME", "OWNER")
			.valueColumns("SIZE_MB")
			.seriesLabels("MB")
			.maxItems(15)
			.pieOtherLimit(0.02)
			.splitRatio(0.6));

		// NOTE: the table only has the biggest segments (see the options), so this is the size of THOSE per schema
		list.add(new CmChartDescriptor()
			.id("seg-by-schema")
			.title("Size per Schema (MB), of the segments in the table")
			.chartType(CmChartDescriptor.CHART_TYPE_DUAL_PIE_BAR)
			.labelColumn("OWNER")
			.groupByColumn("OWNER")
			.valueColumns("SIZE_MB")
			.seriesLabels("MB")
			.pieOtherLimit(0.02));

		list.add(new CmChartDescriptor()
			.id("seg-by-type")
			.title("Size per Segment Type (MB), of the segments in the table")
			.chartType(CmChartDescriptor.CHART_TYPE_DUAL_PIE_BAR)
			.labelColumn("SEGMENT_TYPE")
			.groupByColumn("SEGMENT_TYPE")
			.valueColumns("SIZE_MB")
			.seriesLabels("MB")
			.pieOtherLimit(0.02));

		return list;
	}
}
