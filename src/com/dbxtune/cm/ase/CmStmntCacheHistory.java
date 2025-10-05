/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
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
package com.dbxtune.cm.ase;

import java.sql.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.cm.CounterSample;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.utils.Configuration;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmStmntCacheHistory
extends CountersModel
{
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmStmntCacheHistory.class.getSimpleName();
	public static final String   SHORT_NAME       = "Statement Cache History";
	public static final String   HTML_DESC        = 
		"<html>" +
		"Looking back at the Statement Cache<br>" +
		"How many entries are old vs new.<br>" +
		"If we see a big number at a specific time intervall (lets say today at 05:00)<br>" +
		"Then a <b>bunch<b> of statement was created/inserted in the cache, and probably some was evicted as well...<br>" +
		"<br>" +
		"So we can use this (especially in the Daily Summary Report) to see if we for example have Dynamic Statements that are not parametarized...<br>" +
		"Meaning we have Statement Cache <b>pollution</b><br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_CACHE;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"monCachedStatement"};
	public static final String[] NEED_ROLES       = new String[] {"mon_role"};
	public static final String[] NEED_CONFIG      = new String[] {"enable monitoring=1", "enable stmt cache monitoring=1", "statement cache size"}; // NO default for 'statement cache size' configuration

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
			 "stmnt_count_diff"
			,"exec_count_diff"
			,"total_elapsed_time_ms"
			,"total_cpu_time_ms"
			,"total_est_wait_time_ms"
			,"total_logical_reads"
			,"total_physical_reads"
			,"total_sort_count"
			,"total_sort_time"
			,"avg_qual_read_rows"
			,"avg_qual_write_rows"
	};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 3600; // 1 Hour
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;

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

		return new CmStmntCacheHistory(counterController, guiController);
	}

	public CmStmntCacheHistory(ICounterController counterController, IGuiController guiController)
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
		
//		addDependsOnCm(CmXxx.CM_NAME); // CMspinlockSum must have been executed before this cm

		addTrendGraphs();
		
		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	private static final String  PROP_PREFIX            = CM_NAME;
	
	public static final String  PROPKEY_sample_top_rows = PROP_PREFIX + ".sample.top";
	public static final int     DEFAULT_sample_top_rows = 1000;
	
	private void addTrendGraphs()
	{
	}

//	@Override
//	protected TabularCntrPanel createGui()
//	{
//		return new CmStmntCacheHistoryPanel(this);
//	}

	@Override
	public String[] getDependsOnConfigForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return NEED_CONFIG;
	}

//	@Override
//	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
//	{
//		FIXME: Add column descriptions
//	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
//		return null;
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("creation_date");
		pkCols.add("creation_hour");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		int topRows = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_sample_top_rows, DEFAULT_sample_top_rows);
		
		String sql = ""
			    + "SELECT TOP " + topRows + " \n"
//			    + "     creation_date          = ISNULL( CAST(cs.CachedDate AS date) , '9999-12-31') \n"
//			    + "    ,creation_hour          = ISNULL( CASE \n"
//			    + "                                  WHEN CAST(cs.CachedDate AS date) <> CAST(GETDATE() AS date) THEN -1 \n"
//			    + "                                  ELSE DATEPART(hh, cs.CachedDate) \n"
//			    + "                              END, -99) \n"
			    + "     creation_date          = CAST(cs.CachedDate AS date) \n"
			    + "    ,creation_hour          = CASE \n"
			    + "                                  WHEN CAST(cs.CachedDate AS date) <> CAST(GETDATE() AS date) THEN -1 \n"
			    + "                                  ELSE DATEPART(hh, cs.CachedDate) \n"
			    + "                              END \n"
			    + " \n"
			    + "    ,stmnt_count            = SUM(1) \n"
			    + "    ,exec_count             = SUM(cs.UseCount) \n"
			    + " \n"
			    + "    ,stmnt_count_diff       = SUM(1) \n"
			    + "    ,exec_count_diff        = SUM(cs.UseCount) \n"
			    + " \n"
			    + "    ,PlanSizeKB             = CAST(SUM(cs.MaxPlanSizeKB) as bigint)\n"
			    + "    ,LastUsedDate__min      = min(cs.LastUsedDate) \n"
			    + "    ,LastUsedDate__max      = max(cs.LastUsedDate) \n"
			    + " \n"
			    + "    ,ObjType_Dynamic_sq     = SUM(CASE WHEN cs.StmtType = 3 THEN 1 ELSE 0 END) \n"
			    + "    ,ObjType_Language_ss    = SUM(CASE WHEN cs.StmtType = 1 THEN 1 ELSE 0 END) \n"
			    + " \n"
			    + "    ,total_elapsed_time_ms  = SUM(cs.TotalElapsedTime) \n"
			    + "    ,total_cpu_time_ms      = SUM(cs.TotalCpuTime) \n"
			    + "    ,total_est_wait_time_ms = SUM(cs.TotalElapsedTime) - SUM(cs.TotalCpuTime) \n"
			    + "    ,total_logical_reads    = SUM(cs.TotalLIO) \n"
			    + "    ,total_physical_reads   = SUM(cs.TotalPIO) \n"
			    + "    ,total_sort_count       = SUM(cs.SortCount) \n"
			    + "    ,total_sort_time        = SUM(cs.TotalSortTime) \n"
			    + "    ,avg_qual_read_rows     = SUM(cs.AvgQualifyingReadRows) \n"
			    + "    ,avg_qual_write_rows    = SUM(cs.AvgQualifyingWriteRows) \n"
			    + " \n"
			    + "FROM monCachedStatement cs \n"
			    + "GROUP BY CAST(cs.CachedDate AS date), CASE WHEN CAST(cs.CachedDate AS date) <> CAST(GETDATE() AS date) THEN -1 ELSE DATEPART(hh, cs.CachedDate) END \n"
			    + "ORDER BY 1 DESC, 2 DESC \n"
			    + "";

		return sql;
	}
	
	@Override
	public Map<String, AggregationType> createAggregateColumns()
	{
		HashMap<String, AggregationType> aggColumns = new HashMap<>(getColumnCount());

		AggregationType tmp;
		
		// Create the columns ::::::::::::::::::::::::::::::::::::::::::::::::::::::::: And ADD it to the return Map 
		// The list is grabbed from: SqlCaptureStatementStatisticsSample.toResultSet()
//		tmp = new AggregationType("dbname",                 Types.VARCHAR, 30, 0);
        
		tmp = new AggregationType("stmnt_count",             AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("exec_count",              AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("stmnt_count_diff",        AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("exec_count_diff",         AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		
		tmp = new AggregationType("PlanSizeKB",              AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
//		tmp = new AggregationType("LastUsedDate__min",       AggregationType.Agg.MIN);   aggColumns.put(tmp.getColumnName(), tmp);
//		tmp = new AggregationType("LastUsedDate__max",       AggregationType.Agg.MAX);   aggColumns.put(tmp.getColumnName(), tmp);
// TODO: I get message: CmStmntCacheHistory: Problems in calculateSummaryRow(). Column name 'LastUsedDate__min' can't be summarized, it's NOT a summarizable data type JDBC_TYPE=93 - java.sql.Types.TIMESTAMP. Adding NULL value instead.

		tmp = new AggregationType("ObjType_Dynamic_sq",      AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("ObjType_Language_ss",     AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);

		tmp = new AggregationType("total_elapsed_time_ms",   AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("total_cpu_time_ms",       AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("total_est_wait_time_ms",  AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("total_logical_reads",     AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("total_physical_reads",    AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("total_sort_count",        AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("total_sort_time",         AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("avg_qual_read_rows",      AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);
		tmp = new AggregationType("avg_qual_write_rows",     AggregationType.Agg.SUM);   aggColumns.put(tmp.getColumnName(), tmp);

		return aggColumns;
	}

	@Override
	public Object calculateAggregateRow_getAggregatePkColumnDataProvider(CounterSample newSample, String colName, int c, int jdbcType, Object addValue)
	{
		if ("creation_date".equalsIgnoreCase(colName))
			return Date.valueOf("9999-12-31");
		
		if ("creation_hour".equalsIgnoreCase(colName))
			return Long.valueOf(-99);
		
		return null;
	}

	
}
