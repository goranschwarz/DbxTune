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
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.config.dict.MonTablesDictionary;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.sql.conn.info.DbmsVersionInfoOracle;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmIndexUsage
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmIndexUsage.class.getSimpleName();
	public static final String   SHORT_NAME       = "Index Usage";
	public static final String   HTML_DESC        =
		"<html>" +
		"Which indexes are used, and which are not? One row per index, with how big it is, how many times it has been used and when it was used last.<br>" +
		"An index that is never used still costs: every INSERT, UPDATE and DELETE has to maintain it, and it takes space.<br>" +
		"<br>" +
		"By default: the 500 biggest indexes, sampled once an hour. The indexes in the schemas that comes with Oracle (SYS, SYSTEM, XDB...) are not included.<br>" +
		"<br>" +
		"<b>Before you drop an index that looks unused</b>:" +
		"<ul>" +
		"  <li><code>LAST_USED</code> is empty = <i>not seen used since Oracle started to track it</i>. Look at <code>CREATED</code>, and think of what only runs at month end or year end.</li>" +
		"  <li>By default Oracle only looks at some of the executions (it samples), so the counters are lower than the truth, and an index that is used very seldom can be missed.</li>" +
		"  <li>A <code>UNIQUE</code> index is there to stop duplicates, and it does that also when no query uses it.</li>" +
		"  <li>An index on a foreign key can be needed even if no query uses it: without it the child table is locked and scanned when rows in the parent table are deleted.</li>" +
		"  <li>Make it <code>INVISIBLE</code> first, and drop it later: <code>ALTER INDEX ... INVISIBLE</code></li>" +
		"</ul>" +
		"<b>Note</b>: Oracle tracks this from version 12.2, and writes it to the dictionary every 15 minutes (the view <code>DBA_INDEX_USAGE</code>).<br>" +
		"<b>Note</b>: In a Container Database, connect to CDB$ROOT to see all PDBs.<br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {CM_NAME, "DBA_INDEX_USAGE", "DBA_INDEXES"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"TOTAL_ACCESS_COUNT",
		"TOTAL_EXEC_COUNT",
		"TOTAL_ROWS_RETURNED"
		};

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

		return new CmIndexUsage(counterController, guiController);
	}

	public CmIndexUsage(ICounterController counterController, IGuiController guiController)
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

		list.add(new CmSettingsHelper("Max Number of Rows", PROPKEY_sample_topRows, Integer.class, conf.getIntProperty(PROPKEY_sample_topRows, DEFAULT_sample_topRows), DEFAULT_sample_topRows, "Only get the ## biggest indexes (this to limit how much is fetched and stored on systems with many indexes)." ));

		return list;
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		// CON_ID is always 0 when we are not connected to CDB$ROOT, in a CDB the same index name can exist in many containers
		pkCols.add("CON_ID");
		pkCols.add("OWNER");
		pkCols.add("INDEX_NAME");

		return pkCols;
	}

	/** Only for 12.2 and above, and the views must be readable. Otherwise: disable the CM, and say why */
	@Override
	public boolean checkDependsOnOther(DbxConnection conn)
	{
		// First the views that exists in all versions: a login without privileges should be told about the grant, not about the version (which it can't read either)
		if ( ! OracleCmUtils.checkViewsAreReadable(this, conn, "DBA_INDEXES", "DBA_OBJECTS", "DBA_SEGMENTS", "DBA_USERS") )
			return false;

		long srvVersion = conn.getDbmsVersionInfo().getLongVersion();

		if (srvVersion < Ver.ver(12,2))
		{
			String msg = "Oracle tracks how the indexes are used from version 12.2 (the view DBA_INDEX_USAGE). This is version " + Ver.versionNumToStr(srvVersion) + ".";

			_logger.info("When trying to initialize Counters Model '" + getName() + "', named '" + getDisplayName() + "': " + msg);

			setActive(false, msg);

			TabularCntrPanel tcp = getTabPanel();
			if (tcp != null)
				tcp.setToolTipText(msg);

			return false;
		}

		return OracleCmUtils.checkViewsAreReadable(this, conn, "DBA_INDEX_USAGE");
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		DbmsVersionInfoOracle oraVersionInfo = (DbmsVersionInfoOracle) versionInfo;

		// A non-CDB, or when we are connected to a PDB
		String sql = getSql(false);

		// Connected to CDB$ROOT: the indexes in EVERY container
		if (oraVersionInfo.isCdbRoot())
		{
			String sqlCdb = getSql(true);

			// "dry-run" it, and if it fails use the SQL for one container
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
					_logger.warn(CM_NAME + ": The SQL for a CDB (version " + Ver.versionNumToStr(versionInfo.getLongVersion()) + ") failed, only the indexes in CDB$ROOT will be shown. Caught: " + ex.getMessage().trim());
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

		String views = cdb ? "CDB" : "DBA";
		String conId = cdb ? "CON_ID, " : "";

		// The dictionary views that are joined to DBA_INDEXES are read ONCE (materialize)
		// Without that, Oracle can go to DBA_SEGMENTS once for every index: 29 seconds instead of 0.5 (seen on 18c, with 23 indexes)
		// Note: CAST(... as NUMBER(18,1)) is needed to keep decimals (a NUMBER without precision is treated as a BIGINT)
		return ""
			+ "with s as (select /*+ materialize */ " + conId + "OWNER, SEGMENT_NAME, sum(BYTES) as BYTES \n"
			+ "           from " + views + "_SEGMENTS \n"
			+ "           where SEGMENT_TYPE like 'INDEX%' \n"
			+ "           group by " + conId + "OWNER, SEGMENT_NAME) \n"
			+ "    ,o as (select /*+ materialize */ " + conId + "OWNER, OBJECT_NAME, OBJECT_ID, CREATED \n"
			+ "           from " + views + "_OBJECTS \n"
			+ "           where OBJECT_TYPE = 'INDEX') \n"
			+ "    ,u as (select /*+ materialize */ " + conId + "OBJECT_ID, TOTAL_ACCESS_COUNT, TOTAL_EXEC_COUNT, TOTAL_ROWS_RETURNED, LAST_USED \n"
			+ "           from " + views + "_INDEX_USAGE) \n"
			+ "    ,m as (select /*+ materialize */ " + conId + "USERNAME \n"
			+ "           from " + views + "_USERS \n"
			+ "           where ORACLE_MAINTAINED = 'Y') \n"
			+ "select * from ( \n"
			+ "    select \n"
			+ "         " + (cdb ? "i.CON_ID" : "CAST(0    as NUMBER(10))")    + " as CON_ID \n"
			+ "        ," + (cdb ? "c.NAME"   : "CAST(NULL as VARCHAR2(128))") + " as CON_NAME \n"
			+ "        ,i.OWNER \n"
			+ "        ,i.INDEX_NAME \n"
			+ "        ,i.TABLE_NAME \n"
			+ "        ,i.UNIQUENESS \n"
			+ "        ,i.INDEX_TYPE \n"
			+ "        ,i.STATUS \n"
			+ "        ,i.VISIBILITY \n"
			+ "        ,CAST(NVL(s.BYTES, 0) / 1048576 as NUMBER(18,1)) as SIZE_MB \n"
			+ "        ,NVL(u.TOTAL_ACCESS_COUNT,  0) as TOTAL_ACCESS_COUNT \n"
			+ "        ,NVL(u.TOTAL_EXEC_COUNT,    0) as TOTAL_EXEC_COUNT \n"
			+ "        ,NVL(u.TOTAL_ROWS_RETURNED, 0) as TOTAL_ROWS_RETURNED \n"
			+ "        ,u.LAST_USED \n"
			+ "        ,CAST(SYSDATE - u.LAST_USED as NUMBER(10,1)) as DAYS_SINCE_USED \n"
			+ "        ,o.CREATED \n"
			+ "    from " + views + "_INDEXES i \n"
			+ (cdb ? "    left join V$CONTAINERS c on c.CON_ID = i.CON_ID \n" : "")
			+ "    left join o on o.OWNER     = i.OWNER and o.OBJECT_NAME  = i.INDEX_NAME" + (cdb ? " and o.CON_ID = i.CON_ID" : "") + " \n"
			+ "    left join u on u.OBJECT_ID = o.OBJECT_ID"                               + (cdb ? " and u.CON_ID = i.CON_ID" : "") + " \n"
			+ "    left join s on s.OWNER     = i.OWNER and s.SEGMENT_NAME = i.INDEX_NAME" + (cdb ? " and s.CON_ID = i.CON_ID" : "") + " \n"
			+ "    left join m on m.USERNAME  = i.OWNER"                                   + (cdb ? " and m.CON_ID = i.CON_ID" : "") + " \n"
			+ "    where m.USERNAME is null         -- not the schemas that comes with Oracle \n"
			+ "      and i.INDEX_TYPE <> 'LOB'      -- a LOB index is used by it's LOB, not by queries \n"
			+ "    order by NVL(s.BYTES, 0) desc, i.OWNER, i.INDEX_NAME \n"
			+ ") where ROWNUM <= " + topRows + " \n"
			+ "";
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			mtd.addTable(CM_NAME, "Which indexes are used, and which are not? One row per index, with how big it is, how many times it has been used and when it was used last.");

			mtd.addColumn(CM_NAME, "CON_ID",              "<html>Container ID. 0 = This is not a Container Database, or we are connected to a PDB.</html>");
			mtd.addColumn(CM_NAME, "CON_NAME",            "<html>Name of the Container (PDB) the index is in. NULL if CON_ID is 0.</html>");
			mtd.addColumn(CM_NAME, "OWNER",               "<html>The schema that owns the index.</html>");
			mtd.addColumn(CM_NAME, "INDEX_NAME",          "<html>Name of the index.</html>");
			mtd.addColumn(CM_NAME, "TABLE_NAME",          "<html>The table that the index is on.</html>");
			mtd.addColumn(CM_NAME, "UNIQUENESS",          "<html>UNIQUE or NONUNIQUE.<br>A UNIQUE index is there to stop duplicates (a primary key or a unique constraint), and it does that also when no query uses it.</html>");
			mtd.addColumn(CM_NAME, "INDEX_TYPE",          "<html>NORMAL, BITMAP, FUNCTION-BASED NORMAL, IOT - TOP, DOMAIN...</html>");
			mtd.addColumn(CM_NAME, "STATUS",              "<html>VALID, UNUSABLE or N/A (a partitioned index, where the status is on every partition).</html>");
			mtd.addColumn(CM_NAME, "VISIBILITY",          "<html>VISIBLE or INVISIBLE.<br>An INVISIBLE index is maintained, but the optimizer does not use it. This is how to test what happens if an index is dropped.</html>");
			mtd.addColumn(CM_NAME, "SIZE_MB",             "<html>Space in MB that is allocated to the index (for a partitioned index: all it's partitions).<br><b>Formula</b>: sum(BYTES) / 1024 / 1024 from DBA_SEGMENTS</html>");
			mtd.addColumn(CM_NAME, "TOTAL_ACCESS_COUNT",  "<html>Number of times the index has been accessed, as far as Oracle has tracked it. 0 = not seen used.</html>");
			mtd.addColumn(CM_NAME, "TOTAL_EXEC_COUNT",    "<html>Number of statement executions that used the index, as far as Oracle has tracked it.</html>");
			mtd.addColumn(CM_NAME, "TOTAL_ROWS_RETURNED", "<html>Number of rows that the index has returned, as far as Oracle has tracked it.</html>");
			mtd.addColumn(CM_NAME, "LAST_USED",           "<html>When the index was used last. NULL = not seen used since Oracle started to track it.</html>");
			mtd.addColumn(CM_NAME, "DAYS_SINCE_USED",     "<html>Number of days since LAST_USED. NULL = not seen used.<br><b>Formula</b>: SYSDATE - LAST_USED</html>");
			mtd.addColumn(CM_NAME, "CREATED",             "<html>When the index was created. An index that was created yesterday has not had much time to be used.</html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e);
		}
	}
}
