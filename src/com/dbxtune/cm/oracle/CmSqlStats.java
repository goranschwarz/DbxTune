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
package com.dbxtune.cm.oracle;

import java.lang.invoke.MethodHandles;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.naming.NameNotFoundException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CounterSample;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.oracle.gui.CmSqlStatsPanel;
import com.dbxtune.config.dict.MonTablesDictionary;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.pcs.PcsColumnOptions;
import com.dbxtune.pcs.PcsColumnOptions.ColumnType;
import com.dbxtune.pcs.PersistentCounterHandler;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.sql.conn.info.DbmsVersionInfoOracle;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmSqlStats
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmSqlStats.class.getSimpleName();
	public static final String   SHORT_NAME       = "SQL Stats";
	public static final String   HTML_DESC        =
		"<html>" +
		"What SQL Statements uses the resources? One row per SQL Statement (SQL_ID and execution plan) in the Shared Pool, with how many times it has been executed, and what it has used.<br>" +
		"<br>" +
		"By default: only statements that has been executed the last 30 minutes, and the top 1000 of them (the ones with most BUFFER_GETS). This can be changed in the options.<br>" +
		"<br>" +
		"<b>Tip</b>: Look at the DIFF or RATE values, sorted on <code>ELAPSED_TIME</code>, <code>CPU_TIME</code> or <code>BUFFER_GETS</code>. That is the statements that uses most resources right now.<br>" +
		"The time columns (<code>ELAPSED_TIME</code>, <code>CPU_TIME</code>, <code>..._WAIT_TIME</code>) are in microseconds.<br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] { "V$SQLSTATS" };
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
			 "EXECUTIONS"
			,"BUFFER_GETS"
			,"DISK_READS"
			,"DIRECT_WRITES"
			,"CPU_TIME"
			,"ELAPSED_TIME"
			,"APPLICATION_WAIT_TIME"
			,"CONCURRENCY_WAIT_TIME"
			,"CLUSTER_WAIT_TIME"
			,"USER_IO_WAIT_TIME"
			,"PLSQL_EXEC_TIME"
			,"JAVA_EXEC_TIME"
			,"SORTS"
			,"END_OF_FETCH_COUNT"
			,"PARSE_CALLS"
			,"ROWS_PROCESSED"
			,"SERIALIZABLE_ABORTS"
			,"FETCHES"
			,"LOADS"
			,"INVALIDATIONS"
			,"PX_SERVERS_EXECUTIONS"
			,"IO_CELL_OFFLOAD_ELIGIBLE_BYTES"
			,"IO_INTERCONNECT_BYTES"
			,"PHYSICAL_READ_REQUESTS"
			,"PHYSICAL_READ_BYTES"
			,"PHYSICAL_WRITE_REQUESTS"
			,"PHYSICAL_WRITE_BYTES"
			,"IO_CELL_UNCOMPRESSED_BYTES"
			,"IO_CELL_OFFLOAD_RETURNED_BYTES"
		};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 300; // (every sample is up to 'sample.topRows' statements with their SQL text: 5 minutes, as the statement CMs of SqlServerTune)
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.LARGE; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '" + CM_NAME + "'");

		return new CmSqlStats(counterController, guiController);
	}

	public CmSqlStats(ICounterController counterController, IGuiController guiController)
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
	private static final String  PROP_PREFIX                      = CM_NAME;

	public static final String  PROPKEY_sample_extraWhereClause   = PROP_PREFIX + ".sample.extraWhereClause";
	public static final String  DEFAULT_sample_extraWhereClause   = "BUFFER_GETS > 100";

	public static final String  PROPKEY_sample_afterPrevSample    = PROP_PREFIX + ".sample.afterPrevSample";
	public static final boolean DEFAULT_sample_afterPrevSample    = false;

	public static final String  PROPKEY_sample_lastXminutes       = PROP_PREFIX + ".sample.lastXminutes";
	public static final boolean DEFAULT_sample_lastXminutes       = true;

	public static final String  PROPKEY_sample_lastXminutesTime   = PROP_PREFIX + ".sample.lastXminutes.time";
	public static final int     DEFAULT_sample_lastXminutesTime   = 30;

	public static final String  PROPKEY_sample_topRows            = PROP_PREFIX + ".sample.topRows";
	public static final int     DEFAULT_sample_topRows            = 1000;

	public static final String  PROPKEY_ddlStore_topRows          = PROP_PREFIX + ".ddlStore.topRows";
	public static final int     DEFAULT_ddlStore_topRows          = 20;


	@Override
	protected void registerDefaultValues()
	{
		super.registerDefaultValues();

		Configuration.registerDefaultValue(PROPKEY_ddlStore_topRows,        DEFAULT_ddlStore_topRows);

		Configuration.registerDefaultValue(PROPKEY_sample_extraWhereClause, DEFAULT_sample_extraWhereClause);
		Configuration.registerDefaultValue(PROPKEY_sample_afterPrevSample,  DEFAULT_sample_afterPrevSample);
		Configuration.registerDefaultValue(PROPKEY_sample_lastXminutes,     DEFAULT_sample_lastXminutes);
		Configuration.registerDefaultValue(PROPKEY_sample_lastXminutesTime, DEFAULT_sample_lastXminutesTime);
		Configuration.registerDefaultValue(PROPKEY_sample_topRows,          DEFAULT_sample_topRows);
	}


	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public List<CmSettingsHelper> getLocalSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		list.add(new CmSettingsHelper("Extra Where Clause",                           PROPKEY_sample_extraWhereClause , String .class, conf.getProperty       (PROPKEY_sample_extraWhereClause , DEFAULT_sample_extraWhereClause ), DEFAULT_sample_extraWhereClause, CmSqlStatsPanel.TOOLTIP_sample_extraWhereClause ));
		list.add(new CmSettingsHelper("Show only SQL exected since last sample time", PROPKEY_sample_afterPrevSample  , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_afterPrevSample  , DEFAULT_sample_afterPrevSample  ), DEFAULT_sample_afterPrevSample , CmSqlStatsPanel.TOOLTIP_sample_afterPrevSample  ));
		list.add(new CmSettingsHelper("Show only SQL exected last 30 minutes",        PROPKEY_sample_lastXminutes     , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_lastXminutes     , DEFAULT_sample_lastXminutes     ), DEFAULT_sample_lastXminutes    , CmSqlStatsPanel.TOOLTIP_sample_lastXminutes     ));
		list.add(new CmSettingsHelper("Show only SQL exected last ## minutes",        PROPKEY_sample_lastXminutesTime , Integer.class, conf.getIntProperty    (PROPKEY_sample_lastXminutesTime , DEFAULT_sample_lastXminutesTime ), DEFAULT_sample_lastXminutesTime, CmSqlStatsPanel.TOOLTIP_sample_lastXminutesTime ));
		list.add(new CmSettingsHelper("Max Number of Rows",                           PROPKEY_sample_topRows          , Integer.class, conf.getIntProperty    (PROPKEY_sample_topRows          , DEFAULT_sample_topRows          ), DEFAULT_sample_topRows         , "Only get the ## statements that has most BUFFER_GETS (this to limit how much is fetched and stored on systems with many statements)." ));
		list.add(new CmSettingsHelper("Store Plans for Top Rows",                     PROPKEY_ddlStore_topRows        , Integer.class, conf.getIntProperty    (PROPKEY_ddlStore_topRows        , DEFAULT_ddlStore_topRows        ), DEFAULT_ddlStore_topRows       , "In every sample: store the execution plan, and information about the tables it uses, for the ## statements with most ELAPSED_TIME since the previous sample (a plan and a table is only stored once per recording). 0 = do not store." ));

		return list;
	}

	@Override
	public Map<String, PcsColumnOptions> getPcsColumnOptions()
	{
		Map<String, PcsColumnOptions> map = super.getPcsColumnOptions();

		// No settings in the super, create one, and set it at super
		if (map == null)
		{
			// The same SQL Text is seen in every sample, so only store it once
			map = new HashMap<>();
			map.put("SQL_FULLTEXT", new PcsColumnOptions(ColumnType.DICTIONARY_COMPRESSION));

			// Set the map in the super
			setPcsColumnOptions(map);
		}

		return map;
	}
	
	private void addTrendGraphs()
	{
	}

	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmSqlStatsPanel(this);
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try 
		{
			// from: https://docs.oracle.com/en/database/oracle/oracle-database/19/refrn/V-SQLSTATS.html#GUID-495DD17D-6741-433F-871D-C965EB221DA9

			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addTable("V$SQLSTATS",  "V$SQLSTATS displays basic performance statistics for SQL cursors and contains one row per SQL statement (that is, one row per unique value of SQL_ID). The column definitions for columns in V$SQLSTATS are identical to those in the V$SQL and V$SQLAREA views. However, the V$SQLSTATS view differs from V$SQL and V$SQLAREA in that it is faster, more scalable, and has a greater data retention (the statistics may still appear in this view, even after the cursor has been aged out of the shared pool). Note that V$SQLSTATS contains a subset of columns that appear in V$SQL and V$SQLAREA. </html>");

			mtd.addColumn("V$SQLSTATS", "SQL_TEXT",                                 "<html> First thousand characters of the SQL text for the current cursor </html>");
			mtd.addColumn("V$SQLSTATS", "SQL_FULLTEXT",                             "<html> Full text for the SQL statement exposed as a CLOB column. THe full text of a SQL statement can be retrieved using this column instead of joining with the V$SQLTEXT view. </html>");
			mtd.addColumn("V$SQLSTATS", "SQL_ID",                                   "<html> SQL identifier of the parent cursor in the library cache </html>");
			mtd.addColumn("V$SQLSTATS", "LAST_ACTIVE_TIME",                         "<html> Last time the statistics of a contributing cursor were updated </html>");
			mtd.addColumn("V$SQLSTATS", "LAST_ACTIVE_CHILD_ADDRESS",                "<html> Address of the contributing cursor that last updated these statistics </html>");
			mtd.addColumn("V$SQLSTATS", "PLAN_HASH_VALUE",                          "<html> Numeric representation of the current SQL plan for this cursor. Comparing one PLAN_HASH_VALUE to another easily identifies whether or not two plans are the same (rather than comparing the two plans line by line). </html>");
			mtd.addColumn("V$SQLSTATS", "PARSE_CALLS",                              "<html> Number of parse calls for all cursors with this SQL text and plan </html>");
			mtd.addColumn("V$SQLSTATS", "DISK_READS",                               "<html> Number of disk reads for all cursors with this SQL text and plan </html>");
			mtd.addColumn("V$SQLSTATS", "DIRECT_WRITES",                            "<html> Number of direct writes for all cursors with this SQL text and plan </html>");
			mtd.addColumn("V$SQLSTATS", "DIRECT_READS",                             "<html> Number of direct reads for all cursors with this SQL text and plan </html>");
			mtd.addColumn("V$SQLSTATS", "BUFFER_GETS",                              "<html> Number of buffer gets for all cursors with this SQL text and plan </html>");
			mtd.addColumn("V$SQLSTATS", "ROWS_PROCESSED",                           "<html> Total number of rows the parsed SQL statement returns </html>");
			mtd.addColumn("V$SQLSTATS", "SERIALIZABLE_ABORTS",                      "<html> Number of times the transaction failed to serialize, producing ORA-08177 errors, per cursor </html>");
			mtd.addColumn("V$SQLSTATS", "FETCHES",                                  "<html> Number of fetches associated with the SQL statement </html>");
			mtd.addColumn("V$SQLSTATS", "EXECUTIONS",                               "<html> Number of executions that took place on this object since it was brought into the library cache </html>");
			mtd.addColumn("V$SQLSTATS", "END_OF_FETCH_COUNT",                       "<html> Number of times this cursor was fully executed since the cursor was brought into the library cache. The value of this statistic is not incremented when the cursor is partially executed, either because it failed during the execution or because only the first few rows produced by this cursor are fetched before the cursor is closed or re-executed. By definition, the value of the END_OF_FETCH_COUNT column should be less or equal to the value of the EXECUTIONS column. </html>");
			mtd.addColumn("V$SQLSTATS", "LOADS",                                    "<html> Number of times the object was either loaded or reloaded </html>");
			mtd.addColumn("V$SQLSTATS", "VERSION_COUNT",                            "<html> number of cursors present in the cache with this SQL text and plan </html>");
			mtd.addColumn("V$SQLSTATS", "INVALIDATIONS",                            "<html> Number of times this child cursor has been invalidated </html>");
			mtd.addColumn("V$SQLSTATS", "PX_SERVERS_EXECUTIONS",                    "<html> Total number of executions performed by parallel execution servers (0 when the statement has never been executed in parallel) </html>");
			mtd.addColumn("V$SQLSTATS", "CPU_TIME",                                 "<html> CPU time (in microseconds) used by this cursor for parsing, executing, and fetching </html>");
			mtd.addColumn("V$SQLSTATS", "ELAPSED_TIME",                             "<html> Elapsed time (in microseconds) used by this cursor for parsing, executing, and fetching. If the cursor uses parallel execution, then ELAPSED_TIME is the cumulative time for the query coordinator, plus all parallel query slave processes. </html>");
			mtd.addColumn("V$SQLSTATS", "AVG_HARD_PARSE_TIME",                      "<html> Average hard parse time (in microseconds) used by this cursor </html>");
			mtd.addColumn("V$SQLSTATS", "APPLICATION_WAIT_TIME",                    "<html> Application wait time (in microseconds) </html>");
			mtd.addColumn("V$SQLSTATS", "CONCURRENCY_WAIT_TIME",                    "<html> Concurrency wait time (in microseconds) </html>");
			mtd.addColumn("V$SQLSTATS", "CLUSTER_WAIT_TIME",                        "<html> Cluster wait time (in microseconds). This value is specific to Oracle RAC. It shows the total time spent waiting for all waits that are categorized under the cluster class of wait events. The value is this column is an accumulated wait time spent waiting for Oracle RAC cluster resources. </html>");
			mtd.addColumn("V$SQLSTATS", "USER_IO_WAIT_TIME",                        "<html> User I/O wait time (in microseconds) </html>");
			mtd.addColumn("V$SQLSTATS", "PLSQL_EXEC_TIME",                          "<html> PL/SQL execution time (in microseconds) </html>");
			mtd.addColumn("V$SQLSTATS", "JAVA_EXEC_TIME",                           "<html> Java execution time (in microseconds) </html>");
			mtd.addColumn("V$SQLSTATS", "SORTS",                                    "<html> Number of sorts that were done for the child cursor </html>");
			mtd.addColumn("V$SQLSTATS", "SHARABLE_MEM",                             "<html> Total shared memory (in bytes) currently occupied by all cursors with this SQL text and plan </html>");
			mtd.addColumn("V$SQLSTATS", "TOTAL_SHARABLE_MEM",                       "<html> Total shared memory (in bytes) occupied by all cursors with this SQL text and plan if they were to be fully loaded in the shared pool (that is, cursor size) </html>");
			mtd.addColumn("V$SQLSTATS", "TYPECHECK_MEM",                            "<html> Typecheck memory </html>");
			mtd.addColumn("V$SQLSTATS", "IO_CELL_OFFLOAD_ELIGIBLE_BYTES",           "<html> Number of I/O bytes which can be filtered by the Exadata storage system See Also: Oracle Exadata Storage Server Software documentation for more information </html>");
			mtd.addColumn("V$SQLSTATS", "IO_INTERCONNECT_BYTES",                    "<html> Number of I/O bytes exchanged between Oracle Database and the storage system. Typically used for Cache Fusion or parallel queries. </html>");
			mtd.addColumn("V$SQLSTATS", "PHYSICAL_READ_REQUESTS",                   "<html> Number of physical read I/O requests issued by the monitored SQL. The requests may not be disk reads. </html>");
			mtd.addColumn("V$SQLSTATS", "PHYSICAL_READ_BYTES",                      "<html> Number of bytes read from disks by the monitored SQL </html>");
			mtd.addColumn("V$SQLSTATS", "PHYSICAL_WRITE_REQUESTS",                  "<html> Number of physical write I/O requests issued by the monitored SQL </html>");
			mtd.addColumn("V$SQLSTATS", "PHYSICAL_WRITE_BYTES",                     "<html> Number of bytes written to disks by the monitored SQL </html>");
			mtd.addColumn("V$SQLSTATS", "EXACT_MATCHING_SIGNATURE",                 "<html> Signature used when the CURSOR_SHARING parameter is set to EXACT </html>");
			mtd.addColumn("V$SQLSTATS", "FORCE_MATCHING_SIGNATURE",                 "<html> Signature used when the CURSOR_SHARING parameter is set to FORCE </html>");
			mtd.addColumn("V$SQLSTATS", "IO_CELL_UNCOMPRESSED_BYTES",               "<html> Number of uncompressed bytes (that is, size after decompression) that are offloaded to the Exadata cells See Also: Oracle Exadata Storage Server Software documentation for more information </html>");
			mtd.addColumn("V$SQLSTATS", "IO_CELL_OFFLOAD_RETURNED_BYTES",           "<html> Number of bytes that are returned by Exadata cell through the regular I/O path See Also: Oracle Exadata Storage Server Software documentation for more information </html>");
			mtd.addColumn("V$SQLSTATS", "DELTA_PARSE_CALLS",                        "<html> Number of parse calls for the cursor since the last Automatic Workload Repository (AWR) snapshot See Also: Oracle Database Concepts for an introduction to AWR </html>");
			mtd.addColumn("V$SQLSTATS", "DELTA_DISK_READS",                         "<html> Number of disk reads for the cursor since the last AWR snapshot </html>");
			mtd.addColumn("V$SQLSTATS", "DELTA_DIRECT_WRITES",                      "<html> Number of direct writes for the cursor since the last AWR snapshot </html>");
			mtd.addColumn("V$SQLSTATS", "DELTA_DIRECT_READS",                       "<html> Number of direct reads for the cursor since the last AWR snapshot </html>");
			mtd.addColumn("V$SQLSTATS", "DELTA_BUFFER_GETS",                        "<html> Number of buffer gets for the cursor since the last AWR snapshot </html>");
			mtd.addColumn("V$SQLSTATS", "DELTA_ROWS_PROCESSED",                     "<html> Number of rows returned by the cursor since the last AWR snapshot </html>");
			mtd.addColumn("V$SQLSTATS", "DELTA_FETCH_COUNT",                        "<html> Number of fetches for the cursor since the last AWR snapshot </html>");
			mtd.addColumn("V$SQLSTATS", "DELTA_EXECUTION_COUNT",                    "<html> Number of executions for the cursor since the last AWR snapshot </html>");
			mtd.addColumn("V$SQLSTATS", "DELTA_PX_SERVERS_EXECUTIONS",              "<html> Number of executions performed by parallel execution servers since the last AWR snapshot </html>");
			mtd.addColumn("V$SQLSTATS", "DELTA_END_OF_FETCH_COUNT",                 "<html> Number of times the cursor was fully executed since the last AWR snapshot </html>");
			mtd.addColumn("V$SQLSTATS", "DELTA_CPU_TIME",                           "<html> CPU time (in microseconds) for the cursor since the last AWR snapshot </html>");
			mtd.addColumn("V$SQLSTATS", "DELTA_ELAPSED_TIME",                       "<html> Database time (in microseconds) for the cursor since the last AWR snapshot </html>");
			mtd.addColumn("V$SQLSTATS", "DELTA_APPLICATION_WAIT_TIME",              "<html> Time spent by the cursor (in microseconds) in the Application wait class since the last AWR snapshot </html>");
			mtd.addColumn("V$SQLSTATS", "DELTA_CONCURRENCY_TIME",                   "<html> Time spent by the cursor (in microseconds) in the Concurrency wait class since the last AWR snapshot </html>");
			mtd.addColumn("V$SQLSTATS", "DELTA_CLUSTER_WAIT_TIME",                  "<html> Time spent by the cursor (in microseconds) in the Cluster wait class since the last AWR snapshot </html>");
			mtd.addColumn("V$SQLSTATS", "DELTA_USER_IO_WAIT_TIME",                  "<html> Time spent by the cursor (in microseconds) in the User I/O wait class since the last AWR snapshot </html>");
			mtd.addColumn("V$SQLSTATS", "DELTA_PLSQL_EXEC_TIME",                    "<html> Time spent by the cursor (in microseconds) executing PL/SQL since the last AWR snapshot </html>");
			mtd.addColumn("V$SQLSTATS", "DELTA_JAVA_EXEC_TIME",                     "<html> Time spent by the cursor (in microseconds) executing Java since the last AWR snapshot </html>");
			mtd.addColumn("V$SQLSTATS", "DELTA_SORTS",                              "<html> Number of sorts for the cursor since the last AWR snapshot </html>");
			mtd.addColumn("V$SQLSTATS", "DELTA_LOADS",                              "<html> Number of times the cursor was loaded since the last AWR snapshot </html>");
			mtd.addColumn("V$SQLSTATS", "DELTA_INVALIDATIONS",                      "<html> Number of times the cursor was invalidated since the last AWR snapshot </html>");
			mtd.addColumn("V$SQLSTATS", "DELTA_PHYSICAL_READ_REQUESTS",             "<html> Number of physical read I/O requests for the cursor since the last AWR snapshot </html>");
			mtd.addColumn("V$SQLSTATS", "DELTA_PHYSICAL_READ_BYTES",                "<html> Number of bytes read from disk for the cursor since the last AWR snapshot </html>");
			mtd.addColumn("V$SQLSTATS", "DELTA_PHYSICAL_WRITE_REQUESTS",            "<html> Number of physical write I/O requests for the cursor since the last AWR snapshot </html>");
			mtd.addColumn("V$SQLSTATS", "DELTA_PHYSICAL_WRITE_BYTES",               "<html> Number of bytes written to disk for the cursor since the last AWR snapshot </html>");
			mtd.addColumn("V$SQLSTATS", "DELTA_IO_INTERCONNECT_BYTES",              "<html> Number of I/O bytes exchanged between the Oracle database and the storage system for the cursor since the last AWR snapshot </html>");
			mtd.addColumn("V$SQLSTATS", "DELTA_CELL_OFFLOAD_ELIG_BYTES",            "<html> Number of I/O bytes which can be filtered by the Exadata storage system for the cursor since the last AWR snapshot See Also: Oracle Exadata Storage Server Software documentation for more information </html>");
			mtd.addColumn("V$SQLSTATS", "DELTA_CELL_UNCOMPRESSED_BYTES",            "<html> Number of uncompressed bytes that are offloaded to the Exadata cell for the cursor since the last AWR snapshot See Also: Oracle Exadata Storage Server Software documentation for more information </html>");
			mtd.addColumn("V$SQLSTATS", "CON_ID",                                   "<html> The ID of the container to which the data pertains. Possible values include:<ul><li>0: This value is used for rows containing data that pertain to the entire CDB. This value is also used for rows in non-CDBs.</li><li>1: This value is used for rows containing data that pertain to only the root</li><li>n: Where n is the applicable container ID for the rows containing data</li></ul> </html>");
			mtd.addColumn("V$SQLSTATS", "CON_DBID",                                 "<html> The database ID of the PDB </html>");
			mtd.addColumn("V$SQLSTATS", "OBSOLETE_COUNT",                           "<html> Number of times that a parent cursor became obsolete </html>");
			mtd.addColumn("V$SQLSTATS", "AVOIDED_EXECUTIONS",                       "<html> Number of executions attempted on this object, but prevented due to the SQL statement being in quarantine </html>");
			mtd.addColumn("V$SQLSTATS", "DELTA_AVOIDED_EXECUTIONS",                 "<html> Number of executions attempted on this object, but prevented due to the SQL statement being in quarantine, since the last AWR snapshot </html>");
		}
		catch (NameNotFoundException e) 
		{
		//	_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
			System.out.println("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
		}
	}

	@Override
	public String[] getDependsOnConfigForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return NEED_CONFIG;
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		// V$SQLSTATS has one row per SQL_ID and PLAN_HASH_VALUE (a statement can have more than one execution plan)
		// CON_ID is always 0 in a non-CDB (and in 11.2), in a CDB the same SQL_ID can exist in many containers
		pkCols.add("SQL_ID");
		pkCols.add("PLAN_HASH_VALUE");
		pkCols.add("CON_ID");

		return pkCols;
	}

	/** If we can't read the views: disable the CM, and tell what to do about it */
	@Override
	public boolean checkDependsOnOther(DbxConnection conn)
	{
		return OracleCmUtils.checkViewsAreReadable(this, conn, "V$SQLSTATS");
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		DbmsVersionInfoOracle oraVersionInfo = (DbmsVersionInfoOracle) versionInfo;

		// 11.2 and above
		String sql = getSql("CAST(0 as NUMBER(10))", "CAST(NULL as VARCHAR2(128))", "");

		// 12.1 and above, in a Container Database: what container is the statement in
		if (versionInfo.getLongVersion() >= Ver.ver(12,1) && oraVersionInfo.isCdb())
		{
			String sqlCdb = getSql("s.CON_ID", "c.NAME", "left join V$CONTAINERS c on c.CON_ID = s.CON_ID \n");

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
					_logger.warn(CM_NAME + ": The SQL for a CDB (version " + Ver.versionNumToStr(versionInfo.getLongVersion()) + ") failed, using the SQL for 11.2 instead. Caught: " + ex.getMessage().trim());
				}
			}
		}

		return sql;
	}

	/** A comment in the SQL, that in getSql() (at every sample) is replaced with the "since previous sample" where clause, if that option is enabled */
	private static final String SQL_WHERE_PLACEHOLDER = "/*afterPrevSample*/";

	private String getSql(String CON_ID, String CON_NAME, String joinContainers)
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		String  sample_extraWhereClause = conf.getProperty(       PROPKEY_sample_extraWhereClause, DEFAULT_sample_extraWhereClause);
		boolean sample_lastXminutes     = conf.getBooleanProperty(PROPKEY_sample_lastXminutes,     DEFAULT_sample_lastXminutes);
		int     sample_lastXminutesTime = conf.getIntProperty(    PROPKEY_sample_lastXminutesTime, DEFAULT_sample_lastXminutesTime);
		int     sample_topRows          = conf.getIntProperty(    PROPKEY_sample_topRows,          DEFAULT_sample_topRows);

		// Do we have extra where clauses
		String sql_sample_extraWhereClause = "  -- Extra where clauses will go here. (it will look like: AND the_extra_where_clause) \n";
		if ( ! StringUtil.isNullOrBlank(sample_extraWhereClause) )
			sql_sample_extraWhereClause = "  AND " + sample_extraWhereClause + "\n";

		String sql_sample_lastXminutes = "";
		if (sample_lastXminutes)
			sql_sample_lastXminutes = "  AND s.LAST_ACTIVE_TIME > SYSDATE - interval '" + sample_lastXminutesTime + "' minute \n";

		// The inner SELECT is sorted on BUFFER_GETS, and the outer SELECT takes the first ## rows of that
		String sql = ""
			    + "SELECT * FROM ( \n"
			    + "SELECT \n"
			    + "     s.SQL_ID \n"
			    + "    ,s.PLAN_HASH_VALUE \n"
			    + "    ," + CON_ID   + " as CON_ID \n"
			    + "    ," + CON_NAME + " as CON_NAME \n"
			    + " \n"
			    + "    ,s.EXECUTIONS \n"
			    + "    ,s.BUFFER_GETS \n"
			    + "    ,s.DISK_READS \n"
			    + "    ,s.DIRECT_WRITES \n"
			    + " \n"
			    + "    ,s.CPU_TIME \n"
			    + "    ,s.ELAPSED_TIME \n"
			    + "    ,s.AVG_HARD_PARSE_TIME \n"
			    + "    ,s.APPLICATION_WAIT_TIME \n"
			    + "    ,s.CONCURRENCY_WAIT_TIME \n"
			    + "    ,s.CLUSTER_WAIT_TIME \n"
			    + "    ,s.USER_IO_WAIT_TIME \n"
			    + "    ,s.PLSQL_EXEC_TIME \n"
			    + "    ,s.JAVA_EXEC_TIME \n"
			    + "    ,s.SORTS \n"
			    + "    ,s.SHARABLE_MEM \n"
			    + " \n"
			    + "    ,s.LAST_ACTIVE_TIME \n"
			    + "    ,s.LAST_ACTIVE_CHILD_ADDRESS \n"
			    + " \n"
			    + "    ,s.END_OF_FETCH_COUNT \n"
			    + "    ,s.PARSE_CALLS \n"
			    + "    ,s.ROWS_PROCESSED \n"
			    + "    ,s.SERIALIZABLE_ABORTS \n"
			    + "    ,s.FETCHES \n"
			    + "    ,s.LOADS \n"
			    + "    ,s.VERSION_COUNT \n"
			    + "    ,s.INVALIDATIONS \n"
			    + "    ,s.PX_SERVERS_EXECUTIONS \n"
			    + "    ,s.TOTAL_SHARABLE_MEM \n"
			    + "    ,s.TYPECHECK_MEM \n"
			    + "    ,s.IO_CELL_OFFLOAD_ELIGIBLE_BYTES \n"
			    + "    ,s.IO_INTERCONNECT_BYTES \n"
			    + "    ,s.PHYSICAL_READ_REQUESTS \n"
			    + "    ,s.PHYSICAL_READ_BYTES \n"
			    + "    ,s.PHYSICAL_WRITE_REQUESTS \n"
			    + "    ,s.PHYSICAL_WRITE_BYTES \n"
			    + "    ,s.IO_CELL_UNCOMPRESSED_BYTES \n"
			    + "    ,s.IO_CELL_OFFLOAD_RETURNED_BYTES \n"
			    + " \n"
			    + "    ,s.SQL_FULLTEXT \n"
			    + "FROM V$SQLSTATS s \n"
			    + joinContainers
				+ "WHERE 1 = 1 -- to make extra where clauses easier \n"
				+ sql_sample_extraWhereClause
				+ sql_sample_lastXminutes
				+ "  " + SQL_WHERE_PLACEHOLDER + " \n"
			    + "ORDER BY s.BUFFER_GETS desc \n"
			    + ") WHERE ROWNUM <= " + sample_topRows + " \n"
			    + "";

		return sql;
	}

	/**
	 * Called at every sample: if 'afterPrevSample' is enabled, put in a where clause for statements that has been executed since the previous sample
	 */
	@Override
	public String getSql()
	{
		String sql = super.getSql();
		if (sql == null)
			return null;

		Configuration conf = Configuration.getCombinedConfiguration();
		boolean sample_afterPrevSample = conf.getBooleanProperty(PROPKEY_sample_afterPrevSample, DEFAULT_sample_afterPrevSample);

		if ( ! sample_afterPrevSample )
			return sql;

		Timestamp prevSample = getPreviousSampleTime();
		if (prevSample == null)
			return sql.replace(SQL_WHERE_PLACEHOLDER, "AND 1=0"); // do not get any rows for the first sample...

		// LAST_ACTIVE_TIME is in the DBMS Servers time (SYSDATE), which can be in another time zone than this collector
		// So: let the DBMS calculate how long ago the previous sample was (in UTC), and take that off from SYSDATE
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		String prevSampleUtc = sdf.format(prevSample);

		return sql.replace(SQL_WHERE_PLACEHOLDER,
				"AND s.LAST_ACTIVE_TIME > SYSDATE - (CAST(SYS_EXTRACT_UTC(SYSTIMESTAMP) as DATE) - TO_DATE('" + prevSampleUtc + "', 'YYYY-MM-DD HH24:MI:SS'))");
	}


	/**
	 * The statements that used most elapsed time since the previous sample: send them to the "DDL Storage", which stores the execution plan
	 * and (from the plan) information about the tables: columns, statistics, indexes and sizes. See <code>ObjectLookupInspectorOracle</code><br>
	 * A plan and a table is only stored once per recording: PersistentCounterHandler.addDdl() knows what it already has.
	 */
	@Override
	public void sendDdlDetailsRequest(CounterSample absData, CounterSample diffData, CounterSample rateData)
	{
		if ( ! PersistentCounterHandler.hasInstance() || diffData == null )
			return;

		int topRows = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_ddlStore_topRows, DEFAULT_ddlStore_topRows);
		if (topRows <= 0)
			return;

		final int pos_SQL_ID          = diffData.findColumn("SQL_ID");
		final int pos_PLAN_HASH_VALUE = diffData.findColumn("PLAN_HASH_VALUE");
		final int pos_ELAPSED_TIME    = diffData.findColumn("ELAPSED_TIME");
		if (pos_SQL_ID == -1 || pos_PLAN_HASH_VALUE == -1 || pos_ELAPSED_TIME == -1)
			return;

		// Most elapsed time first
		List<List<Object>> sorted = new ArrayList<>(diffData.getDataCollection());
		sorted.sort((row1, row2) -> Double.compare(
				row2.get(pos_ELAPSED_TIME) instanceof Number ? ((Number) row2.get(pos_ELAPSED_TIME)).doubleValue() : 0,
				row1.get(pos_ELAPSED_TIME) instanceof Number ? ((Number) row1.get(pos_ELAPSED_TIME)).doubleValue() : 0));

		PersistentCounterHandler pch = PersistentCounterHandler.getInstance();

		for (int r = 0; r < sorted.size() && r < topRows; r++)
		{
			Object sqlId         = sorted.get(r).get(pos_SQL_ID);
			Object planHashValue = sorted.get(r).get(pos_PLAN_HASH_VALUE);
			Object elapsedTime   = sorted.get(r).get(pos_ELAPSED_TIME);

			// The rest did nothing in this sample
			if ( ! (elapsedTime instanceof Number) || ((Number) elapsedTime).doubleValue() <= 0 )
				break;

			// PLAN_HASH_VALUE 0 = a PL/SQL block or a DDL statement: there is no plan
			if (sqlId == null || ! (planHashValue instanceof Number) || ((Number) planHashValue).longValue() == 0)
				continue;

			pch.addDdl(PersistentCounterHandler.STATEMENT_CACHE_NAME, sqlId + "_" + ((Number) planHashValue).longValue(), CM_NAME + ".diff.top.ELAPSED_TIME");
		}
	}
}
