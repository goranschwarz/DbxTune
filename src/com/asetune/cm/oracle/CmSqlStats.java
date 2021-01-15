/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
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
package com.asetune.cm.oracle;

import java.sql.Connection;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.oracle.gui.CmSqlStatsPanel;
import com.asetune.cm.sqlserver.gui.CmExecQueryStatsPanel;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmSqlStats
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmServiceMemory.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmSqlStats.class.getSimpleName();
	public static final String   SHORT_NAME       = "SQL Stats";
	public static final String   HTML_DESC        = 
		"<html>" +
		"shows Oracle SQL Statistics. <br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

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
			,"AVG_HARD_PARSE_TIME"
			,"APPLICATION_WAIT_TIME"
			,"CONCURRENCY_WAIT_TIME"
			,"CLUSTER_WAIT_TIME"
			,"USER_IO_WAIT_TIME"
			,"PLSQL_EXEC_TIME"
			,"JAVA_EXEC_TIME"
			,"SORTS"
			,"SHARABLE_MEM"
			,"END_OF_FETCH_COUNT"
			,"PARSE_CALLS"
			,"ROWS_PROCESSED"
			,"SERIALIZABLE_ABORTS"
			,"FETCHES"
			,"LOADS"
			,"VERSION_COUNT"
			,"INVALIDATIONS"
			,"PX_SERVERS_EXECUTIONS"
			,"TOTAL_SHARABLE_MEM"
			,"TYPECHECK_MEM"
			,"IO_CELL_OFFLOAD_ELIGIBLE_BYTES"
			,"IO_INTERCONNECT_BYTES"
			,"PHYSICAL_READ_REQUESTS"
			,"PHYSICAL_READ_BYTES"
			,"PHYSICAL_WRITE_REQUESTS"
			,"PHYSICAL_WRITE_BYTES"
			,"IO_CELL_UNCOMPRESSED_BYTES"
			,"IO_CELL_OFFLOAD_RETURNED_BYTES"
		};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = false;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.ALL; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '"+CM_NAME+"'");

		return new CmSqlStats(counterController, guiController);
	}

	public CmSqlStats(ICounterController counterController, IGuiController guiController)
	{
		super(counterController,
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


	@Override
	protected void registerDefaultValues()
	{
		super.registerDefaultValues();

		Configuration.registerDefaultValue(PROPKEY_sample_extraWhereClause, DEFAULT_sample_extraWhereClause);
		Configuration.registerDefaultValue(PROPKEY_sample_afterPrevSample,  DEFAULT_sample_afterPrevSample);
		Configuration.registerDefaultValue(PROPKEY_sample_lastXminutes,     DEFAULT_sample_lastXminutes);
		Configuration.registerDefaultValue(PROPKEY_sample_lastXminutesTime, DEFAULT_sample_lastXminutesTime);
	}


	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public List<CmSettingsHelper> getLocalSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		
		list.add(new CmSettingsHelper("Extra Where Clause",                           PROPKEY_sample_extraWhereClause , String .class, conf.getProperty       (PROPKEY_sample_extraWhereClause , DEFAULT_sample_extraWhereClause ), DEFAULT_sample_extraWhereClause, CmExecQueryStatsPanel.TOOLTIP_sample_extraWhereClause ));
		list.add(new CmSettingsHelper("Show only SQL exected since last sample time", PROPKEY_sample_afterPrevSample  , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_afterPrevSample  , DEFAULT_sample_afterPrevSample  ), DEFAULT_sample_afterPrevSample , CmExecQueryStatsPanel.TOOLTIP_sample_afterPrevSample  ));
		list.add(new CmSettingsHelper("Show only SQL exected last 30 minutes",        PROPKEY_sample_lastXminutes     , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_lastXminutes     , DEFAULT_sample_lastXminutes     ), DEFAULT_sample_lastXminutes    , CmExecQueryStatsPanel.TOOLTIP_sample_lastXminutes     ));
		list.add(new CmSettingsHelper("Show only SQL exected last ## minutes",        PROPKEY_sample_lastXminutesTime , Integer.class, conf.getIntProperty    (PROPKEY_sample_lastXminutesTime , DEFAULT_sample_lastXminutesTime ), DEFAULT_sample_lastXminutesTime, CmExecQueryStatsPanel.TOOLTIP_sample_lastXminutesTime ));

		return list;
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
	public void addMonTableDictForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
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
		catch (NameNotFoundException e) {/*ignore*/}
	}

	@Override
	public String[] getDependsOnConfigForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		return NEED_CONFIG;
	}

	@Override
	public List<String> getPkForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("SQL_ID");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		String  sample_extraWhereClause = conf.getProperty(       PROPKEY_sample_extraWhereClause, DEFAULT_sample_extraWhereClause);
		boolean sample_lastXminutes     = conf.getBooleanProperty(PROPKEY_sample_lastXminutes,     DEFAULT_sample_lastXminutes);
		int     sample_lastXminutesTime = conf.getIntProperty(    PROPKEY_sample_lastXminutesTime, DEFAULT_sample_lastXminutesTime);

		// Do we have extra where clauses
		String sql_sample_extraWhereClause = "  -- Extra where clauses will go here. (it will look like: AND the_extra_where_clause) \n";
		if ( ! StringUtil.isNullOrBlank(sample_extraWhereClause) )
			sql_sample_extraWhereClause = "  AND " + sample_extraWhereClause + "\n";

		String sql_sample_lastXminutes = "";
		if (sample_lastXminutes)
			sql_sample_lastXminutes = "  AND LAST_ACTIVE_TIME > sysdate - interval '" + sample_lastXminutesTime + "' minute \n";
//			sql_sample_lastXminutes = "  AND last_execution_time > dateadd(mi, -"+sample_lastXminutesTime+", getdate())\n";

		String sql = ""
			    + "SELECT \n"
			    + "     SQL_ID                          \n" // java.sql.Types.VARCHAR   VARCHAR2(13)      -none- \n"
			    + "    ,PLAN_HASH_VALUE                 \n" // java.sql.Types.NUMERIC   NUMBER(0,-127)    -none- \n"
			    + " \n"
			    + "    ,EXECUTIONS                      \n" // java.sql.Types.NUMERIC   NUMBER(0,-127)    -none- \n"
			    + "    ,BUFFER_GETS                     \n" // java.sql.Types.NUMERIC   NUMBER(0,-127)    -none- \n"
			    + "    ,DISK_READS                      \n" // java.sql.Types.NUMERIC   NUMBER(0,-127)    -none- \n"
			    + "    ,DIRECT_WRITES                   \n" // java.sql.Types.NUMERIC   NUMBER(0,-127)    -none- \n"
			    + " \n"
			    + "    ,CPU_TIME                        \n" // java.sql.Types.NUMERIC   NUMBER(0,-127)    -none- \n"
			    + "    ,ELAPSED_TIME                    \n" // java.sql.Types.NUMERIC   NUMBER(0,-127)    -none- \n"
			    + "    ,AVG_HARD_PARSE_TIME             \n" // java.sql.Types.NUMERIC   NUMBER(0,-127)    -none- \n"
			    + "    ,APPLICATION_WAIT_TIME           \n" // java.sql.Types.NUMERIC   NUMBER(0,-127)    -none- \n"
			    + "    ,CONCURRENCY_WAIT_TIME           \n" // java.sql.Types.NUMERIC   NUMBER(0,-127)    -none- \n"
			    + "    ,CLUSTER_WAIT_TIME               \n" // java.sql.Types.NUMERIC   NUMBER(0,-127)    -none- \n"
			    + "    ,USER_IO_WAIT_TIME               \n" // java.sql.Types.NUMERIC   NUMBER(0,-127)    -none- \n"
			    + "    ,PLSQL_EXEC_TIME                 \n" // java.sql.Types.NUMERIC   NUMBER(0,-127)    -none- \n"
			    + "    ,JAVA_EXEC_TIME                  \n" // java.sql.Types.NUMERIC   NUMBER(0,-127)    -none- \n"
			    + "    ,SORTS                           \n" // java.sql.Types.NUMERIC   NUMBER(0,-127)    -none- \n"
			    + "    ,SHARABLE_MEM                    \n" // java.sql.Types.NUMERIC   NUMBER(0,-127)    -none- \n"
			    + " \n"
			    + "    ,LAST_ACTIVE_TIME                \n" // java.sql.Types.TIMESTAMP DATE              -none- \n"
			    + "    ,LAST_ACTIVE_CHILD_ADDRESS       \n" // java.sql.Types.VARBINARY RAW(8)            -none- \n"
			    + " \n"
			    + "    ,END_OF_FETCH_COUNT              \n" // java.sql.Types.NUMERIC   NUMBER(0,-127)    -none- \n"
			    + "    ,PARSE_CALLS                     \n" // java.sql.Types.NUMERIC   NUMBER(0,-127)    -none- \n"
			    + "    ,ROWS_PROCESSED                  \n" // java.sql.Types.NUMERIC   NUMBER(0,-127)    -none- \n"
			    + "    ,SERIALIZABLE_ABORTS             \n" // java.sql.Types.NUMERIC   NUMBER(0,-127)    -none- \n"
			    + "    ,FETCHES                         \n" // java.sql.Types.NUMERIC   NUMBER(0,-127)    -none- \n"
			    + "    ,LOADS                           \n" // java.sql.Types.NUMERIC   NUMBER(0,-127)    -none- \n"
			    + "    ,VERSION_COUNT                   \n" // java.sql.Types.NUMERIC   NUMBER(0,-127)    -none- \n"
			    + "    ,INVALIDATIONS                   \n" // java.sql.Types.NUMERIC   NUMBER(0,-127)    -none- \n"
			    + "    ,PX_SERVERS_EXECUTIONS           \n" // java.sql.Types.NUMERIC   NUMBER(0,-127)    -none- \n"
			    + "    ,TOTAL_SHARABLE_MEM              \n" // java.sql.Types.NUMERIC   NUMBER(0,-127)    -none- \n"
			    + "    ,TYPECHECK_MEM                   \n" // java.sql.Types.NUMERIC   NUMBER(0,-127)    -none- \n"
			    + "    ,IO_CELL_OFFLOAD_ELIGIBLE_BYTES  \n" // java.sql.Types.NUMERIC   NUMBER(0,-127)    -none- \n"
			    + "    ,IO_INTERCONNECT_BYTES           \n" // java.sql.Types.NUMERIC   NUMBER(0,-127)    -none- \n"
			    + "    ,PHYSICAL_READ_REQUESTS          \n" // java.sql.Types.NUMERIC   NUMBER(0,-127)    -none- \n"
			    + "    ,PHYSICAL_READ_BYTES             \n" // java.sql.Types.NUMERIC   NUMBER(0,-127)    -none- \n"
			    + "    ,PHYSICAL_WRITE_REQUESTS         \n" // java.sql.Types.NUMERIC   NUMBER(0,-127)    -none- \n"
			    + "    ,PHYSICAL_WRITE_BYTES            \n" // java.sql.Types.NUMERIC   NUMBER(0,-127)    -none- \n"
//			    + "    ,EXACT_MATCHING_SIGNATURE      \n" // java.sql.Types.NUMERIC   NUMBER(0,-127)    -none- \n"
//			    + "    ,FORCE_MATCHING_SIGNATURE      \n" // java.sql.Types.NUMERIC   NUMBER(0,-127)    -none- \n"
			    + "    ,IO_CELL_UNCOMPRESSED_BYTES      \n" // java.sql.Types.NUMERIC   NUMBER(0,-127)    -none- \n"
			    + "    ,IO_CELL_OFFLOAD_RETURNED_BYTES  \n" // java.sql.Types.NUMERIC   NUMBER(0,0)       -none- \n"
			    + " \n"
//			    + "    ,SQL_TEXT                      \n" // java.sql.Types.VARCHAR   VARCHAR2(1000)    -none- \n"
			    + "    ,SQL_FULLTEXT                    \n" // java.sql.Types.CLOB      CLOB              -none- \n"
			    + "FROM V$SQLSTATS \n"
				+ "WHERE 1 = 1 -- to make extra where clauses easier \n"
				+ sql_sample_extraWhereClause
				+ sql_sample_lastXminutes
			    + "ORDER BY BUFFER_GETS desc \n"
			    + "";

		return sql;
	}

	@Override
	public String getSql()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		boolean sample_afterPrevSample = conf.getBooleanProperty(PROPKEY_sample_afterPrevSample, DEFAULT_sample_afterPrevSample);

		if (sample_afterPrevSample)
		{
    		Timestamp prevSample = getPreviousSampleTime();
    		if (prevSample == null)
    			setSqlWhere("AND 1=0"); // do not get any rows for the first sample...
    		else
    			setSqlWhere("AND LAST_ACTIVE_TIME > '"+prevSample+"' "); 
		}
		else
			setSqlWhere("");

		// Now get the SQL from super method...
		return super.getSql();
	}

	
//	/** 
//	 * Get number of rows to save/request ddl information for 
//	 * if 0 is return no lookup will be done.
//	 */
//	@Override
//	public int getMaxNumOfDdlsToPersist()
//	{
//		return 10;
//	}
//
//	/** 
//	 * Get Column names to where DBName and ObjectName is called, this must always return at least a array with 2 strings. 
//	 */
//	@Override
//	public String[] getDdlDetailsColNames()
//	{
////		String[] sa = {"dbname", "plan_handle"};
//		String[] sa = {"SQL_ID", "PLAN_HASH_VALUE"};
//		return sa;
//	}
//	/**
//	 * Sort descending on this column(s) to get values from the diff/rate structure<br>
//	 * One sort for every value will be done, meaning we can do "top" for more than 1 column<br>
//	 * So if we want to do top 10 LogicalReads AND top 10 LockContention
//	 * If this one returns null, this will not be done
//	 * @return
//	 */
//	@Override
//	public String[] getDdlDetailsSortOnColName()
//	{
////fixme: maybe add an object for every column here, like new ObjectLookupSortPredicate("execution_count", GT, ABS|DIFF|RATE, 0);
////This so we dont extract so many "extra objects"...
//
//		String[] sa = {"EXECUTIONS", "CPU_TIME", "BUFFER_GETS", "ELAPSED_TIME"};
//		return sa;
//	}
}
