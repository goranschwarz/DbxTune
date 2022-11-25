/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
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
package com.asetune.pcs.report.content.sqlserver;

import java.io.IOException;
import java.io.Writer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.h2.tools.SimpleResultSet;

import com.asetune.cm.SortOptions;
import com.asetune.cm.SortOptions.ColumnNameSensitivity;
import com.asetune.cm.SortOptions.DataSortSensitivity;
import com.asetune.cm.SortOptions.SortOrder;
import com.asetune.gui.ModelMissmatchException;
import com.asetune.gui.ResultSetTableModel;
import com.asetune.gui.ResultSetTableModel.TableStringRenderer;
import com.asetune.pcs.report.DailySummaryReportAbstract;
import com.asetune.pcs.report.DailySummaryReportDefault;
import com.asetune.pcs.report.content.SparklineHelper;
import com.asetune.pcs.report.content.SparklineHelper.AggType;
import com.asetune.pcs.report.content.SparklineHelper.DataSource;
import com.asetune.pcs.report.content.SparklineHelper.SparkLineParams;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;
import com.asetune.utils.DbUtils;
import com.asetune.utils.HtmlTableProducer;
import com.asetune.utils.HtmlTableProducer.ColumnCopyDef;
import com.asetune.utils.HtmlTableProducer.ColumnCopyRender;
import com.asetune.utils.HtmlTableProducer.ColumnCopyRow;
import com.asetune.utils.HtmlTableProducer.ColumnStatic;
import com.asetune.utils.StringUtil;
import com.asetune.utils.TimeUtils;

public class SqlServerQueryStore
extends SqlServerAbstract
{
	private static Logger _logger = Logger.getLogger(SqlServerQueryStore.class);

//	private ResultSetTableModel _shortRstm;
	private LinkedHashMap<String, QsDbReport> _dbMap = new LinkedHashMap<>();
	private QsDbReportSummary _qsSummaryDbReport;

	public SqlServerQueryStore(DailySummaryReportAbstract reportingInstance)
	{
		super(reportingInstance);
	}

	@Override
	public boolean hasShortMessageText()
	{
//		return false;
		return true;  // but only for the SUMMARY
	}

//	@Override
//	public void writeShortMessageText(Writer w)
//	throws IOException
//	{
//	}

	@Override
	public void writeMessageText(Writer w, MessageType messageType)
	throws IOException
	{
		if (_dbMap.isEmpty())
		{
			if (isFullMessageType())
			{
				w.append("No databases with 'Query Store' enabled was found or captured.\n");
				w.append("<br>\n");
				w.append("To enable Query Store for a database, you can do the following<br>\n");
				w.append("<pre>\n");
				w.append("USE master \n");
				w.append("go \n");
				w.append("ALTER DATABASE [dbname] SET QUERY_STORE = ON \n");
				w.append("go \n");
				w.append("ALTER DATABASE [dbname] SET QUERY_STORE (OPERATION_MODE = READ_WRITE, MAX_STORAGE_SIZE_MB = 2048) \n");
				w.append("go \n");
				w.append("--or if you want a bit more fine grained options/controll before we starts to persists to the Query Store: \n");
				w.append("--ALTER DATABASE [gs1] \n");
				w.append("--SET QUERY_STORE  \n");
				w.append("--( \n");
				w.append("--	OPERATION_MODE = READ_WRITE,  \n");
				w.append("--	MAX_STORAGE_SIZE_MB         = 2048,                 -- Determines the space issued to the Query Store. MAX_STORAGE_SIZE_MB is type bigint. The default value is 100 MB for SQL Server (SQL Server 2016 (13.x) through SQL Server 2017 (14.x)). Starting with SQL Server 2019 (15.x), the default value is 1 GB. \n");
				w.append("--	CLEANUP_POLICY = (STALE_QUERY_THRESHOLD_DAYS = 30), -- Describes the data retention policy of the Query Store. STALE_QUERY_THRESHOLD_DAYS determines the number of days for which the information for a query is kept in the Query Store. STALE_QUERY_THRESHOLD_DAYS is type bigint. The default value is 30. \n");
				w.append("--	DATA_FLUSH_INTERVAL_SECONDS = 900,                  -- Determines the frequency at which data written to the Query Store is persisted to disk. To optimize for performance, data collected by the Query Store is asynchronously written to the disk. The frequency at which this asynchronous transfer occurs is configured by using the DATA_FLUSH_INTERVAL_SECONDS argument. DATA_FLUSH_INTERVAL_SECONDS is type bigint. The default value is 900 (15 min). \n");
				w.append("--	INTERVAL_LENGTH_MINUTES     = 1440,                 -- Determines the time interval at which runtime execution statistics data is aggregated into the Query Store. To optimize for space usage, the runtime execution statistics in the runtime stats store are aggregated over a fixed time window. This fixed time window is configured by using the INTERVAL_LENGTH_MINUTES argument. INTERVAL_LENGTH_MINUTES is type bigint. The default value is 60. \n");
				w.append("--	SIZE_BASED_CLEANUP_MODE     = AUTO,                 -- Size-based cleanup will be automatically activated when size on disk reaches 90% of MAX_STORAGE_SIZE_MB. Size-based cleanup removes the least expensive and oldest queries first. It stops at approximately 80% of MAX_STORAGE_SIZE_MB. This value is the default configuration value. \n");
				w.append("--	MAX_PLANS_PER_QUERY         = 200,                  -- Defines the maximum number of plans maintained for each query. MAX_PLANS_PER_QUERY is type int. The default value is 200. \n");
				w.append("--	WAIT_STATS_CAPTURE_MODE     = ON                    -- Controls whether wait statistics will be captured per query. Starting with SQL Server 2017  \n");
				w.append("--	QUERY_CAPTURE_MODE          = CUSTOM,               -- Designates the currently active query capture mode. Each mode defines specific query capture policies. { ALL | AUTO | CUSTOM | NONE } \n");
				w.append("--	QUERY_CAPTURE_POLICY        =                       -- Allows control over the QUERY_CAPTURE_POLICY options. \n");
				w.append("--	( \n");
				w.append("--		STALE_CAPTURE_POLICY_THRESHOLD = 1 HOURS,       -- Defines the evaluation interval period to determine if a query should be captured. The default is 1 day, and it can be set from 1 hour to seven days. number is type int. \n");
				w.append("--		EXECUTION_COUNT                = 30,            -- Defines the number of times a query is executed over the evaluation period. The default is 30, which means that for the default Stale Capture Policy Threshold, a query must execute at least 30 times in one day to be persisted in the Query Store. \n");
				w.append("--		TOTAL_COMPILE_CPU_TIME_MS      = 1000,          -- Defines total elapsed compile CPU time used by a query over the evaluation period. The default is 1000 which means that for the default Stale Capture Policy Threshold, a query must have a total of at least one second of CPU time spent during query compilation in one day to be persisted in the Query Store.  \n");
				w.append("--		TOTAL_EXECUTION_CPU_TIME_MS    = 100            -- Defines total elapsed execution CPU time used by a query over the evaluation period. The default is 100 which means that for the default Stale Capture Policy Threshold, a query must have a total of at least 100 ms of CPU time spent during execution in one day to be persisted in the Query Store \n");
				w.append("--	) \n");
				w.append("--) \n");
				w.append("go \n");
				w.append("-- Check below URLs for more information about Query Store \n");
				w.append("https://docs.microsoft.com/en-us/sql/relational-databases/performance/monitoring-performance-by-using-the-query-store?view=sql-server-ver15 \n");
				w.append("https://docs.microsoft.com/en-us/sql/relational-databases/performance/how-query-store-collects-data?view=sql-server-ver15 \n");
				w.append("</pre>\n");
				w.append("<br>\n");
			}

			if (isShortMessageType())
			{
				w.append("No databases with 'Query Store' enabled was found or captured.\n");
				w.append("For tips on how to enable 'Query Store', see the <i>full</i> report!\n");
			}
			return; /** <<<<<<<<-------------------- */
		}

		// Write what databases we will make a report for
		w.append("The following databases was Captured with Query Store:\n");
		w.append("<ul>\n");
		for (String dbname : _dbMap.keySet())
		{
//			w.append("<li>").append(dbname).append("</li>\n");  // FIXME: This should be a LINK to below sections
			w.append("<li><a href='#").append("qs_").append(dbname).append("'>").append(dbname).append("</a></li>\n");  // FIXME: This should be a LINK to below sections
		}
		w.append("</ul>\n");


		// Merge ALL Query Store Configuration into ONE ResultSetTableModel then print that in a HTML Table
		ResultSetTableModel allConfRstm = null;
		for (QsDbReport entry : _dbMap.values())
		{
			if (allConfRstm == null)
			{
				allConfRstm = new ResultSetTableModel(entry._confRstm, "allConfRstm", true);
			}
			else
			{
				try { allConfRstm.add(entry._confRstm); }
				catch(ModelMissmatchException ex) { _logger.error("Problems adding Query Store Configuration for database ''.", ex);}
			}
		}
		if (allConfRstm != null && !allConfRstm.isEmpty())
		{
			w.append("Query Store Configuration/Options for all databases that has Query Store Enabled\n");

			w.append(allConfRstm.toHtmlTableString("sortable"));
			w.append("<br>\n");
			w.append("<br>\n");
		}

		// FULL MESAGE - write SUMMARY and ALL databases
		if (isFullMessageType())
		{
			// First Write SUMMARY REPORT For ALL Databases (only if we have more that 1 db's activated for Query Store)
			if (_qsSummaryDbReport != null)
				_qsSummaryDbReport.writeMessageText(w);

			// Write a report for each of the databases
			if (_dbMap.size() > 1)
			{
				for (QsDbReport entry : _dbMap.values())
				{
					entry.writeMessageText(w);
				}
			}
		}
		// SHORT MESAGE - Write SUMMARY or FIRST entry (if we only got 1 entry)
		else if (isShortMessageType())
		{
			if (_qsSummaryDbReport != null)
				_qsSummaryDbReport.writeMessageText(w);
		}
	}

	@Override
	public String getSubject()
	{
		return "Query Store Reports";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}


//	@Override
//	public String[] getMandatoryTables()
//	{
//		return new String[] { "CmIndexMissing_abs" };
//	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		try (ResultSet rs = conn.getMetaData().getSchemas())
		{
			while(rs.next())
			{
				String schemaName = rs.getString(1);
				if (schemaName != null && schemaName.startsWith("qs:"))
				{
					String dbname = schemaName.substring("qs:".length());
					QsDbReport qsDbReport = new QsDbReport(schemaName, dbname);

					qsDbReport.create(conn, srvName, pcsSavedConf, localConf);
					
					_dbMap.put(dbname, qsDbReport);
				}
			}
		}
		catch (SQLException ex)
		{
			_logger.error("Problems getting schemas in the PCS.");
			return;
		}
		
		// merge all QsDbReport into a "ALL_DB's"
		if (_dbMap.size() > 0) // Make the summary even if we only have one entry (since the Summary holds the "Sparkline")
		{
			_qsSummaryDbReport = new QsDbReportSummary();

			for (QsDbReport entry : _dbMap.values())
			{
				_qsSummaryDbReport.merge(entry);
			}
			_qsSummaryDbReport.sort();
			_qsSummaryDbReport.setTopRows(getTopRows()); // Keep only the ## top rows.
			_qsSummaryDbReport.postMerge(); // Fixes links etc to ShowPlans... NOT YET IMPLEMENTED
			_qsSummaryDbReport.createSqlTextTable(conn);
		}
	}

//	/**
//	 * Set descriptions for the table, and the columns
//	 */
//	private void setSectionDescription(ResultSetTableModel rstm)
//	{
//		if (rstm == null)
//			return;
//		
//		// Section description
//		rstm.setDescription(
//				"Information from last collector sample from the table <code>CmIndexMissing_abs</code><br>" +
//				"");
//	}

	
	//----------------------------------------------------------------------------------------------
	//-- CLASS: QsExecutionPlanCollection
	//----------------------------------------------------------------------------------------------
	/**
	 * Get execution plans from the PCS: Query Store
	 * 
	 * @author goran
	 */
	private static class QsExecutionPlanCollection
	extends ExecutionPlanCollection
	{
		String _schemaName;
		
		public QsExecutionPlanCollection(SqlServerAbstract reportEntry, ResultSetTableModel rstm, String id, String schemaName)
		{
			super(reportEntry, rstm, id);
			_schemaName = schemaName;
		}
		
//		@Override
//		public Map<String, String> getShowplanAsMap(DbxConnection conn, Set<String> nameSet)
//		throws SQLException
//		{
//			Map<String, String> planMap = new LinkedHashMap<>();
//			
//			for (String name : nameSet)
//			{
//				String sql = ""
//					    + "select [plan_id], [query_plan] \n"
//					    + "from [" + _schemaName + "].[query_store_plan] \n"
//					    + "where 1 = 1 \n"
//					    + "  and [plan_id] = " + DbUtils.safeStr(name) + " \n"
//					    + "";
//				
//				sql = conn.quotifySqlString(sql);
//				try ( Statement stmnt = conn.createStatement() )
//				{
//					// Unlimited execution time
//					stmnt.setQueryTimeout(0);
//					try ( ResultSet rs = stmnt.executeQuery(sql) )
//					{
//						while(rs.next())
//						{
//							String objectName    = rs.getString(1);
//							String extraInfoText = rs.getString(2);
//
//							planMap.put(objectName, extraInfoText);
//						}
//					}
//				}
//				catch(SQLException ex)
//				{
//					//_problem = ex;
//
//					_logger.warn("Problems getting SQL Statement name = '"+name+"': " + ex);
//					throw ex;
//				} 
//			}
//			
//			return planMap;
//		}
		@Override
		public Map<PlanKey, String> getShowplanAsMap(DbxConnection conn, Set<PlanKey> nameSet)
		throws SQLException
		{
			Map<PlanKey, String> planMap = new LinkedHashMap<>();
			
			for (PlanKey planKey : nameSet)
			{
				String dbname = planKey.getDbname();
				String planId = planKey.getPlanId();

				String sql = ""
					    + "select [plan_id], [query_plan] \n"
					    + "from [" + _schemaName + "].[query_store_plan] \n"
					    + "where 1 = 1 \n"
					    + "  and [plan_id] = " + DbUtils.safeStr(planId) + " \n"
					    + "";
				
				sql = conn.quotifySqlString(sql);
				try ( Statement stmnt = conn.createStatement() )
				{
					// Unlimited execution time
					stmnt.setQueryTimeout(0);
					try ( ResultSet rs = stmnt.executeQuery(sql) )
					{
						while(rs.next())
						{
							String objectName    = rs.getString(1);
							String extraInfoText = rs.getString(2);

							planMap.put(new PlanKey(dbname, objectName), extraInfoText);
						}
					}
				}
				catch(SQLException ex)
				{
					//_problem = ex;

					_logger.warn("Problems getting SQL Statement name = '"+planId+"': " + ex);
					throw ex;
				} 
			}
			
			return planMap;
		}
	}

	//----------------------------------------------------------------------------------------------
	//-- CLASS: QsDbReport
	//----------------------------------------------------------------------------------------------
	private class QsDbReportSummary
	extends QsDbReport
	{
		public QsDbReportSummary()
		{
			super("SUMMARY", "ALL Databases with Query Store Enabled.");
		}

		//----------------------------------------------------------
		// BEGIN: methods for merging into SUMMARY 
		//----------------------------------------------------------
		public void merge(QsDbReport entry)
		{
			//-----------------------------------------------------
			// Copy: ResultSets
			//-----------------------------------------------------
			if (_topCpuRstm  == null) _topCpuRstm  = new ResultSetTableModel(entry._topCpuRstm , "SUMMARY-CPU"   , false);
			if (_topWaitRstm == null) _topWaitRstm = new ResultSetTableModel(entry._topWaitRstm, "SUMMARY-WAIT"  , false);
			if (_confRstm    == null) _confRstm    = new ResultSetTableModel(entry._confRstm   , "SUMMARY-CONFIG", false);
			if (_recomRstm   == null) _recomRstm   = new ResultSetTableModel(entry._recomRstm  , "SUMMARY-RECOM" , false);

			try
			{
				_topCpuRstm .add(entry._topCpuRstm);
				_topWaitRstm.add(entry._topWaitRstm);
				_confRstm   .add(entry._confRstm);
				_recomRstm  .add(entry._recomRstm);
			}
			catch (ModelMissmatchException ex)
			{
				_logger.warn("Problems merging Query Store entry '" + _dbname + "' into 'SUMMARY'. Skipping this entry.", ex);
			}

			//-----------------------------------------------------
			// Copy: SQL Text for CPU and WAIT
			//-----------------------------------------------------
			if (_keyToSqlText  == null) _keyToSqlText  = new HashMap<>();
			if (_keyToWaitTime == null) _keyToWaitTime = new HashMap<>();

			_keyToSqlText .putAll(entry._keyToSqlText);
			_keyToWaitTime.putAll(entry._keyToWaitTime);

		
			//-----------------------------------------------------
			// Copy: Plan Text for CPU and WAIT
			//-----------------------------------------------------
			if (_planCollectionCpu  == null) _planCollectionCpu  = new QsExecutionPlanCollection(SqlServerQueryStore.this, _topCpuRstm,  "qs_SUMMARY_cpu", _schemaName);
			if (_planCollectionWait == null) _planCollectionWait = new QsExecutionPlanCollection(SqlServerQueryStore.this, _topWaitRstm, "qs_SUMMARY_wait", _schemaName);

			_planCollectionCpu .addShowplanMap(entry._planCollectionCpu .getShowplanAsMap());
			_planCollectionWait.addShowplanMap(entry._planCollectionWait.getShowplanAsMap());

			//-----------------------------------------------------
			// Copy: SparkLine - JavaScript code
			//-----------------------------------------------------
			_miniChartJsList.addAll(entry._miniChartJsList);
		}

		public void sort()
		{
			List<SortOptions> cpuSortOptions  = new ArrayList<>();
			List<SortOptions> waitSortOptions = new ArrayList<>();
			
			cpuSortOptions .add(new SortOptions("total_cpu_time_ms__sum"       , ColumnNameSensitivity.IN_SENSITIVE, SortOrder.DESCENDING, DataSortSensitivity.SENSITIVE));
			waitSortOptions.add(new SortOptions("total_query_wait_time_ms__sum", ColumnNameSensitivity.IN_SENSITIVE, SortOrder.DESCENDING, DataSortSensitivity.SENSITIVE));

			_topCpuRstm .sort(cpuSortOptions);
			_topWaitRstm.sort(waitSortOptions);
		}

		public void setTopRows(int topRows)
		{
			_topCpuRstm .setRowCount(topRows);
			_topWaitRstm.setRowCount(topRows);
		}

		public void postMerge()
		{
			// FIX various stuff like:
			//  * Execution plan DEV and variables... (right now the plan is displayed in the DATABASE section)

			setCpuSectionDescription(_topCpuRstm);
			setWaitSectionDescription(_topWaitRstm);
			
			// FIXME: remove extra rows in PlanCollection(s) 
			_planCollectionCpu .cleanupMap("dbname", "plan_id");
			_planCollectionWait.cleanupMap("dbname", "plan_id");
		}
		//----------------------------------------------------------
		// END: methods for merging into SUMMARY 
		//----------------------------------------------------------
	}

	/**
	 * Implements reporting for ONE database in PCS: Query Store
	 * @author goran
	 */
	private class QsDbReport
	{
		String _schemaName;
		String _dbname;
		ResultSetTableModel _topCpuRstm;
		ResultSetTableModel _cpuSqlTextRstm;
		ResultSetTableModel _topWaitRstm;
		ResultSetTableModel _confRstm;    // database_query_store_options
		ResultSetTableModel _recomRstm;   // dm_db_tuning_recommendations

		ExecutionPlanCollection _planCollectionCpu;
		ExecutionPlanCollection _planCollectionWait;

		Map<Map<String, Object>, QsSqlTextEntry> _keyToSqlText;
		Map<Map<String, Object>, QsWaitEntry>    _keyToWaitTime;
		
		List<String> _miniChartJsList = new ArrayList<>();

		public QsDbReport(String schemaName, String dbname)
		{
			_schemaName = schemaName;
			_dbname     = dbname;
		}

		
		public void writeMessageText(Writer w) 
		throws IOException
		{
			boolean useBootstrap = true;
			if (getReportingInstance() instanceof DailySummaryReportDefault)
				useBootstrap = ((DailySummaryReportDefault)getReportingInstance()).useBootstrap();

			if (useBootstrap)
			{
				// Bootstrap "card" - BEGIN
				w.append("<!--[if !mso]><!--> \n"); // BEGIN: IGNORE THIS SECTION FOR OUTLOOK
				w.append("<div id='").append("qs_").append(_dbname).append("' class='card border-dark mb-3'>");
				w.append("<h5 class='card-header'><b>Query Store Report for: ").append(_dbname).append("</b></h5>");
				w.append("<div class='card-body'>");
				w.append("<!--<![endif]-->    \n"); // END: IGNORE THIS SECTION FOR OUTLOOK
			
				w.append("<!--[if mso]> \n"); // BEGIN: ONLY FOR OUTLOOK
				w.append("<br>\n");
				w.append("<h3 id='").append("qs_").append(_dbname).append("'>Query Store Report for: ").append(_dbname).append("</h3> \n");
				w.append("<![endif]-->  \n"); // END: ONLY FOR OUTLOOK
			}
			else
			{
				w.append("<br>\n");
				w.append("<h3 id='").append("qs_").append(_dbname).append("'>Query Store Report for: ").append(_dbname).append("</h3> \n");
			}

			// Create a Renderer for the HTML Table
			TableStringRenderer tableRender = new ReportEntryTableStringRenderer()
			{
				@Override
				public String cellValue(ResultSetTableModel rstm, int row, int col, String colName, Object objVal, String strVal)
				{
					if ("txt".equals(colName))
					{
						// Get Actual Executed SQL Text for current 'plan_id'
						int    plan_id = rstm.getValueAsInteger(row, "plan_id");
						String dbname  = rstm.getValueAsString (row, "dbname");
						
						Map<String, Object> whereColValMap = new LinkedHashMap<>();
						whereColValMap.put("plan_id", plan_id);
						whereColValMap.put("!dbname", dbname);

						String sqlText = getQsSqlTextAsString(_keyToSqlText, whereColValMap, rstm, row);
						
						if (StringUtil.isNullOrBlank(sqlText))
							return "";

						// Put the "Actual Executed SQL Text" as a "tooltip"
						return "<div title='Click for Detailes' "
								+ "data-toggle='modal' "
								+ "data-target='#dbx-view-sqltext-dialog' "
								+ "data-objectname='" + plan_id + "' "
								+ "data-tooltip=\""   + sqlText     + "\" "
								+ ">&#x1F4AC;</div>"; // symbol popup with "..."
					}

					if ("wait".equals(colName))
					{
						// Get Actual Executed SQL Text for current 'plan_id'
						int    plan_id = rstm.getValueAsInteger(row, "plan_id");
						String dbname  = rstm.getValueAsString (row, "dbname");
						
						Map<String, Object> whereColValMap = new LinkedHashMap<>();
						whereColValMap.put("plan_id", plan_id);
						whereColValMap.put("!dbname", dbname);

						String waitText = getQsWaitTimeAsString(_keyToWaitTime, whereColValMap, rstm, row);

						if (StringUtil.isNullOrBlank(waitText))
							return "";

						// Put the "Actual Executed SQL Text" as a "tooltip"
						return "<div title='Click for Detailes' "
								+ "data-toggle='modal' "
								+ "data-target='#dbx-view-sqltext-dialog' "
								+ "data-objectname='" + plan_id + "' "
								+ "data-tooltip=\""   + waitText     + "\" "
								+ ">&#x1F4AC;</div>"; // symbol popup with "..."
					}

					if ("plan_text".equals(colName))
					{
						// Get Actual Executed SQL Text for current 'plan_id'
						String plan_id = rstm.getValueAsString(row, "plan_id");
						String dbname  = rstm.getValueAsString(row, "dbname");

						if (rstm == _topCpuRstm)
						{
							return _planCollectionCpu.getLinkText(dbname, plan_id, null, "-no-plan-");
						}
						else if (rstm == _topWaitRstm)
						{
							return _planCollectionWait.getLinkText(dbname, plan_id, null, "-no-plan-");
						}
					}

					return strVal;
				}

				@Override
				public String cellToolTip(ResultSetTableModel rstm, int row, int col, String colName, Object objVal, String strVal)
				{
					if (objVal instanceof Number && objVal != null && colName != null)
					{
						if (    colName.indexOf("_duration__sum" ) != -1
							 || colName.indexOf("_cpu_time__sum" ) != -1
							 || colName.indexOf("_wait_time__sum") != -1
							 || colName.indexOf("_clr_time__sum" ) != -1
						   )
						{
							// MicroSeconds to HH:MM:SS.ms
							return TimeUtils.usToTimeStrLong(((Number)objVal).longValue());
						}

//						if ( colName.indexOf("total_query_wait_time_ms__sum" ) != -1 )
						if ( colName.indexOf("_ms__sum" ) != -1 )
						{
							// MilliSeconds to HH:MM:SS.ms
							return TimeUtils.msToTimeStrLong(((Number)objVal).longValue());
						}
					}
					return null;
				}
			};
			
			//----------------------------------------------------
			//---- CPU
			//----------------------------------------------------
			// Create a default renderer
			if (_topCpuRstm != null) // always true... but just to use {} to scope 'tableRender' var
			{
				if (isFullMessageType())
				{
					w.append("<hr> \n");
					w.append(getSectionDescriptionHtml(_topCpuRstm, true));
					w.append("Row Count: " + _topCpuRstm.getRowCount() + "<br>\n");
					w.append(_topCpuRstm.toHtmlTableString("sortable", true, true, null, tableRender));
					
					// Write HTML/JavaScript Code for the Execution Plan...
					if (_planCollectionCpu  != null)
						_planCollectionCpu .writeMessageText(w);
				}

				if (_cpuSqlTextRstm != null)
				{
					w.append("<br>\n");
					w.append("<details open> \n");
					w.append("<summary>Details for above Plans, including SQL Text (click to collapse) </summary> \n");
					
					w.append("<br>\n");
					w.append("SQL Text by 'plan_id', Row Count: " + _cpuSqlTextRstm.getRowCount() + "\n");
					w.append(_cpuSqlTextRstm.toHtmlTableString("sortable", true, true, null, tableRender));

					w.append("\n");
					w.append("</details> \n");
				}
			}
			
			//----------------------------------------------------
			//---- WAIT
			//----------------------------------------------------
			w.append("<br>\n");
			w.append("<hr> \n");
			if (_topWaitRstm == null)
			{
				w.append("This SQL Server Version do NOT have 'wait statistics' in the Query Store. Needs at least SQL Server 2017.<br>\n");
			}
			else
			{
				if (isFullMessageType())
				{
					w.append(getSectionDescriptionHtml(_topWaitRstm, true));

					w.append("Row Count: " + _topWaitRstm.getRowCount() + "<br>\n");
					w.append(_topWaitRstm.toHtmlTableString("sortable", true, true, null, tableRender));

					// Write HTML/JavaScript Code for the Execution Plan...
					if (_planCollectionWait != null) 
						_planCollectionWait.writeMessageText(w);
				}
			}

			//----------------------------------------------------
			//---- Recommendations
			//----------------------------------------------------
			w.append("<br>\n");
			w.append("<hr> \n");
			if (_recomRstm == null)
			{
				w.append("This SQL Server Version do NOT have 'Tuning Recommendation' in the Query Store. Needs at least SQL Server 2017.<br>\n");
			}
			else
			{
				if (_recomRstm.isEmpty())
				{
					w.append("NO Tuning Recomendations from the Query Store subsystem.<br>");
				}
				else
				{
					w.append("Tuning Recomendations from the Quesry Store subsystem.<br>");

					w.append("Row Count: " + _recomRstm.getRowCount() + "<br>\n");
					w.append(toHtmlTable(_recomRstm));
//					w.append(_recomRstm.toHtmlTableString("sortable", true, true, null, tableRender));
				}
			}

			
			// Section FOOTER
			if (useBootstrap)
			{
				// Bootstrap "card" - END
				w.append("<!--[if !mso]><!--> \n"); // BEGIN: IGNORE THIS SECTION FOR OUTLOOK
				w.append("</div>"); // end: card-body
				w.append("</div>"); // end: card
				w.append("<!--<![endif]-->    \n"); // END: IGNORE THIS SECTION FOR OUTLOOK
			}

			// Write JavaScript code for CPU SparkLine
			if (isFullMessageType())
			{
				for (String str : _miniChartJsList)
					w.append(str);
			}
		}

		public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
		{
			String sql = "";

			boolean hasTable_query_store_wait_stats = DbUtils.checkIfTableExistsNoThrow(conn, null, _schemaName, "query_store_wait_stats");

			// Create helper index (which is needed for performance)
			if (hasTable_query_store_wait_stats)
			{
				try
				{
					if ( ! DbUtils.checkIfIndexExists(conn, null, _schemaName, "query_store_wait_stats", "query_store_wait_stats__plan_id") )
					{
						sql = conn.quotifySqlString("create index [query_store_wait_stats__plan_id] on [" + _schemaName + "].[query_store_wait_stats]([plan_id])");

						_logger.info("Creating helper index using sql='" + sql + "'.");

						conn.dbExec(sql);
					}
				}
				catch (SQLException ex)
				{
					_logger.error("Problems creating extra tables on schema='" + _schemaName + "', table='query_store_wait_stats', index='query_store_wait_stats__plan_id'. sql='" + sql + "'. Continuing anyway.", ex);
				}
			}

			
			//-----------------------------------------------------------
			// database_query_store_options
			//-----------------------------------------------------------
			sql = "select '" + _dbname + "' as [dbname], * from [" + _schemaName + "].[database_query_store_options] \n";
			_confRstm = executeQuery(conn, sql, false, _dbname + "_conf");


			//-----------------------------------------------------------
			// dm_db_tuning_recommendations
			//-----------------------------------------------------------
			sql = "select '" + _dbname + "' as [dbname], * from [" + _schemaName + "].[dm_db_tuning_recommendations] \n";
			_recomRstm = executeQuery(conn, sql, false, _dbname + "_conf");


			//-----------------------------------------------------------
			// runtime_stats
			//-----------------------------------------------------------
			 // just to get Column names
			String dummySql = "select * from [" + _schemaName + "].[query_store_runtime_stats] where 1 = 2";
			ResultSetTableModel dummyRstm = executeQuery(conn, dummySql, true, "metadata");

			// Create Column selects, but only if the column exists in the PCS Table
		//	boolean is2017 = dummyRstm.hasColumnNoCase("avg_num_physical_io_reads") && dummyRstm.hasColumnNoCase("avg_log_bytes_used") && dummyRstm.hasColumnNoCase("avg_tempdb_space_used"); 
			String avg_num_physical_io_reads       = !dummyRstm.hasColumnNoCase("avg_num_physical_io_reads") ? "" : "    ,sum([avg_num_physical_io_reads])                           as [avg_num_physical_io_reads__sum] \n";   // in 2017
			String avg_num_physical_io_reads_chart = !dummyRstm.hasColumnNoCase("avg_num_physical_io_reads") ? "" : "    ,''                                                         as [avg_num_physical_io_reads__chart] \n"; // in 2017
			String avg_log_bytes_used              = !dummyRstm.hasColumnNoCase("avg_log_bytes_used"       ) ? "" : "    ,sum([avg_log_bytes_used])                                  as [avg_log_bytes_used__sum] \n";          // in 2017
			String avg_log_bytes_used_kb           = !dummyRstm.hasColumnNoCase("avg_log_bytes_used"       ) ? "" : "    ,sum([avg_log_bytes_used]/1024.0)                           as [avg_log_bytes_used_kb__sum] \n";       // in 2017
			String avg_log_bytes_used_mb           = !dummyRstm.hasColumnNoCase("avg_log_bytes_used"       ) ? "" : "    ,sum([avg_log_bytes_used]/1024.0/1024.0)                    as [avg_log_bytes_used_mb__sum] \n";       // in 2017
			String avg_log_bytes_used_chart        = !dummyRstm.hasColumnNoCase("avg_log_bytes_used"       ) ? "" : "    ,''                                                         as [avg_log_bytes_used__chart] \n";        // in 2017
			String avg_tempdb_space_used           = !dummyRstm.hasColumnNoCase("avg_tempdb_space_used"    ) ? "" : "    ,sum([avg_tempdb_space_used])                               as [avg_tempdb_space_used__sum] \n";       // in 2017
			String avg_tempdb_space_used_kb        = !dummyRstm.hasColumnNoCase("avg_tempdb_space_used"    ) ? "" : "    ,sum([avg_tempdb_space_used]*8.0)                           as [avg_tempdb_space_used_kb__sum] \n";    // in 2017
			String avg_tempdb_space_used_mb        = !dummyRstm.hasColumnNoCase("avg_tempdb_space_used"    ) ? "" : "    ,sum([avg_tempdb_space_used]/128.0)                         as [avg_tempdb_space_used_mb__sum] \n";    // in 2017
			String avg_tempdb_space_used_chart     = !dummyRstm.hasColumnNoCase("avg_tempdb_space_used"    ) ? "" : "    ,''                                                         as [avg_tempdb_space_used__chart] \n";     // in 2017

			String total_num_physical_io_reads     = !dummyRstm.hasColumnNoCase("avg_num_physical_io_reads") ? "" : "    ,cast(sum([count_executions]) * sum([avg_num_physical_io_reads])          as bigint) as [total_num_physical_io_reads__sum] \n"; // in 2017
			String total_log_bytes_used            = !dummyRstm.hasColumnNoCase("avg_log_bytes_used"       ) ? "" : "    ,cast(sum([count_executions]) * sum([avg_log_bytes_used])                 as bigint) as [total_log_bytes_used__sum] \n"       ; // in 2017
			String total_log_bytes_used_kb         = !dummyRstm.hasColumnNoCase("avg_log_bytes_used"       ) ? "" : "    ,cast((sum([count_executions]) * sum([avg_log_bytes_used]))/1024.0        as bigint) as [total_log_bytes_used_kb__sum] \n"    ; // in 2017
			String total_log_bytes_used_mb         = !dummyRstm.hasColumnNoCase("avg_log_bytes_used"       ) ? "" : "    ,cast((sum([count_executions]) * sum([avg_log_bytes_used]))/1024.0/1024.0 as bigint) as [total_log_bytes_used_mb__sum] \n"    ; // in 2017
			String total_tempdb_space_used         = !dummyRstm.hasColumnNoCase("avg_tempdb_space_used"    ) ? "" : "    ,cast(sum([count_executions]) * sum([avg_tempdb_space_used])              as bigint) as [total_tempdb_space_used__sum] \n"    ; // in 2017
			String total_tempdb_space_used_kb      = !dummyRstm.hasColumnNoCase("avg_tempdb_space_used"    ) ? "" : "    ,cast((sum([count_executions]) * sum([avg_tempdb_space_used]))*8.0        as bigint) as [total_tempdb_space_used_kb__sum] \n" ; // in 2017
			String total_tempdb_space_used_mb      = !dummyRstm.hasColumnNoCase("avg_tempdb_space_used"    ) ? "" : "    ,cast((sum([count_executions]) * sum([avg_tempdb_space_used]))/128.0      as bigint) as [total_tempdb_space_used_mb__sum] \n" ; // in 2017

			String   avg_query_wait_time_ms_chart    = "";
			String total_query_wait_time_ms          = "";
			String   avg_query_wait_time_ms          = "";

			String   avg_query_wait_time_other_ms_chart    = "";
			String total_query_wait_time_other_ms          = "";
			String   avg_query_wait_time_other_ms          = "";

			String   avg_query_wait_time_paralell_ms_chart = "";
			String total_query_wait_time_paralell_ms       = "";
			String   avg_query_wait_time_paralell_ms       = "";

			if (hasTable_query_store_wait_stats)
			{
				  avg_query_wait_time_ms_chart          = "    ,''                                                         as [avg_query_wait_time_ms__chart] \n";
				total_query_wait_time_ms                = "    ,(select sum([total_query_wait_time_ms])                                             from [" + _schemaName + "].[query_store_wait_stats] w where w.[plan_id] = rs.[plan_id]) as [total_query_wait_time_ms__sum] \n";
//				  avg_query_wait_time_ms                = "    ,cast(-1, bigint)                                           as [avg_query_wait_time_other_ms__sum] \n";
				  avg_query_wait_time_ms                = "    ,(select sum([avg_query_wait_time_ms])/count(distinct w.[runtime_stats_interval_id]) from [" + _schemaName + "].[query_store_wait_stats] w where w.[plan_id] = rs.[plan_id]) as [avg_query_wait_time_ms__sum] \n";  

				  avg_query_wait_time_other_ms_chart    = "    ,''                                                         as [avg_query_wait_time_other_ms__chart] \n";
				total_query_wait_time_other_ms          = "    ,(select sum([total_query_wait_time_ms])                                             from [" + _schemaName + "].[query_store_wait_stats] w where w.[plan_id] = rs.[plan_id] and w.[wait_category] != 16) as [total_query_wait_time_other_ms__sum] \n"; // wait_category:16 = 'Parallelism'
				  avg_query_wait_time_other_ms          = "    ,(select sum([avg_query_wait_time_ms])/count(distinct w.[runtime_stats_interval_id]) from [" + _schemaName + "].[query_store_wait_stats] w where w.[plan_id] = rs.[plan_id] and w.[wait_category] != 16) as [avg_query_wait_time_other_ms__sum] \n";  

				  avg_query_wait_time_paralell_ms_chart = "    ,''                                                         as [avg_query_wait_time_paralell_ms__chart] \n";
				total_query_wait_time_paralell_ms       = "    ,(select sum([total_query_wait_time_ms])                                             from [" + _schemaName + "].[query_store_wait_stats] w where w.[plan_id] = rs.[plan_id] and w.[wait_category] = 16) as [total_query_wait_time_paralell_ms__sum] \n"; // wait_category:16 = 'Parallelism'
				  avg_query_wait_time_paralell_ms       = "    ,(select sum([avg_query_wait_time_ms])/count(distinct w.[runtime_stats_interval_id]) from [" + _schemaName + "].[query_store_wait_stats] w where w.[plan_id] = rs.[plan_id] and w.[wait_category] = 16) as [avg_query_wait_time_paralell_ms__sum] \n";
			}

			sql = ""
				    + "select top " + getTopRows() + " \n"
				    + "     '" + _dbname + "'                                                          as [dbname] \n"
				    + "    ,[plan_id] \n"
//				    + "    ,cast([plan_id] as varchar(20))                                             as [plan_text] \n"
				    + "    ,''                                                                         as [plan_text] \n"
				    + "    ,''                                                                         as [txt] \n"
				    + "    ,''                                                                         as [wait] \n"
				    + "    ,max([execution_type])                                                      as [execution_type] \n"
				    + "    ,max([execution_type_desc])                                                 as [execution_type_desc] \n"
				    + "    ,min([first_execution_time])                                                as [first_execution_time] \n"
				    + "    ,max([last_execution_time])                                                 as [last_execution_time] \n"
                                                                                                       
				    + "    ,''                                                                         as [count_executions__chart] \n"
				    + "    ,sum([count_executions])                                                    as [count_executions__sum] \n"

				    + "    ,''                                                                         as [avg_duration__chart] \n"
				    + "    ,cast(sum([count_executions]) * sum([avg_duration]/1000.0) as bigint)       as [total_duration_ms__sum] \n"
				    + "    ,sum([avg_duration]/1000.0)                                                 as [avg_duration_ms__sum] \n"

				    + "    ,''                                                                         as [avg_cpu_time__chart] \n"
				    + "    ,cast(sum([count_executions]) * sum([avg_cpu_time]/1000.0) as bigint)       as [total_cpu_time_ms__sum] \n"
				    + "    ,sum([avg_cpu_time]/1000.0)                                                 as [avg_cpu_time_ms__sum] \n"

// Wait time like this do NOT work if we are running in parallel (since 'duration' is shorter than 'cpu_time')
//				    + "    ,''                                                                         as [avg_wait_time__chart] \n"
//				    + "    ,cast((sum([count_executions]) * sum([avg_duration/1000.0])) - (sum([count_executions/1000.0]) * sum([avg_cpu_time/1000.0])) as bigint) as [total_wait_time_ms__sum] \n"
//				    + "    ,sum([avg_duration]/1000.0) - sum([avg_cpu_time]/1000.0)                    as [avg_wait_time__sum] \n"

				    +   avg_query_wait_time_ms_chart
				    + total_query_wait_time_ms
				    +   avg_query_wait_time_ms
				    
				    +   avg_query_wait_time_other_ms_chart
				    + total_query_wait_time_other_ms
				    +   avg_query_wait_time_other_ms
				    
				    +   avg_query_wait_time_paralell_ms_chart
				    + total_query_wait_time_paralell_ms
				    +   avg_query_wait_time_paralell_ms
				    
				    + "    ,''                                                                         as [avg_logical_io_reads__chart] \n"
				    + "    ,cast(sum([count_executions]) * sum([avg_logical_io_reads]) as bigint)      as [total_logical_io_reads__sum] \n"
				    + "    ,sum([avg_logical_io_reads])                                                as [avg_logical_io_reads__sum] \n"
				    
				    + "    ,''                                                                         as [avg_logical_io_reads_mb__chart] \n"
				    + "    ,sum([count_executions]) * sum([avg_logical_io_reads]) / 128                as [total_logical_io_reads_mb__sum] \n"
				    + "    ,sum([avg_logical_io_reads]) / 128.0                                        as [avg_logical_io_reads_mb__sum] \n"
				    
				    + "    ,''                                                                         as [avg_logical_io_writes__chart] \n"
				    + "    ,cast(sum([count_executions]) * sum([avg_logical_io_writes]) as bigint)     as [total_logical_io_writes__sum] \n"
				    + "    ,sum([avg_logical_io_writes])                                               as [avg_logical_io_writes__sum] \n"
				    
				    + "    ,''                                                                         as [avg_physical_io_reads__chart] \n"
				    + "    ,cast(sum([count_executions]) * sum([avg_physical_io_reads]) as bigint)     as [total_physical_io_reads__sum] \n"
				    + "    ,sum([avg_physical_io_reads])                                               as [avg_physical_io_reads__sum] \n"
				    
				    + "    ,''                                                                         as [avg_clr_time__chart] \n"
				    + "    ,cast(sum([count_executions]) * sum([avg_clr_time]/1000.0) as bigint)       as [total_clr_time_ms__sum] \n"
				    + "    ,sum([avg_clr_time]/1000.0)                                                 as [avg_clr_time_ms__sum] \n"
				    
				    + "    ,''                                                                         as [avg_dop__chart] \n"
//				    + "    ,cast(sum([count_executions]) * sum([avg_dop]) as bigint)                   as [total_dop__sum] \n"
//				    + "    ,sum([avg_dop])                                                             as [avg_dop__sum] \n"
				    + "    ,avg([avg_dop])                                                             as [avg_dop__avg] \n"
				    
				    + "    ,''                                                                                 as [avg_query_max_used_memory__chart] \n"
				    + "    ,cast(sum([count_executions]) * sum([avg_query_max_used_memory]) as bigint)         as [total_query_max_used_memory__sum] \n"
				    + "    ,cast(sum([count_executions]) * sum([avg_query_max_used_memory]) / 128.0 as bigint) as [total_query_max_used_memory_mb__sum] \n"
				    + "    ,sum([avg_query_max_used_memory])                                                   as [avg_query_max_used_memory__sum] \n"
				    + "    ,sum([avg_query_max_used_memory]/128.0)                                             as [avg_query_max_used_memory_mb__sum] \n"
				    
				    + "    ,''                                                                         as [avg_rowcount__chart] \n"
				    + "    ,cast(sum([count_executions]) * sum([avg_rowcount]) as bigint)              as [total_rowcount__sum] \n"
				    + "    ,sum([avg_rowcount])                                                        as [avg_rowcount__sum] \n"

				    + avg_num_physical_io_reads_chart 
				    + total_num_physical_io_reads
				    + avg_num_physical_io_reads 

				    + avg_log_bytes_used_chart
				    + total_log_bytes_used
				    + total_log_bytes_used_kb
				    + total_log_bytes_used_mb
				    + avg_log_bytes_used
				    + avg_log_bytes_used_kb
				    + avg_log_bytes_used_mb

				    + avg_tempdb_space_used_chart
				    + total_tempdb_space_used
				    + total_tempdb_space_used_kb
				    + total_tempdb_space_used_mb
				    + avg_tempdb_space_used
				    + avg_tempdb_space_used_kb
				    + avg_tempdb_space_used_mb


				    + "from [" + _schemaName + "].[query_store_runtime_stats] rs \n"
//					+ getReportPeriodSqlWhere()
				    + "group by [plan_id] \n"
				    + "order by [total_cpu_time_ms__sum] desc \n"
				    + "";
			
			_topCpuRstm = executeQuery(conn, sql, true, _dbname + "_topCpu");
			setCpuSectionDescription(_topCpuRstm);

			// Highlight sort column
			_topCpuRstm.setHighlightSortColumns("total_cpu_time_ms__sum");

			// - Get all "plann_handle" in table '_shortRstm'
			// - Get the Execution Plan all the "plann_handle"s
			// - In the table substitute the "plann_handle"s with a link that will display the XML Plan on the HTML Page
//			createPlanCollectorForCpu();
			_planCollectionCpu = new QsExecutionPlanCollection(SqlServerQueryStore.this, _topCpuRstm, "qs_" + _dbname + "_cpu", _schemaName);
//			_planCollectionCpu.getPlansAndSubstituteWithLinks(conn, "dbname", "plan_id", "plan_text", null, null);
			_planCollectionCpu.getPlans(conn, "dbname", "plan_id");

			//--------------------------------------------------------------------------------------
			// get Executed SQL Text for 'plan_id' from QueryStore
			//--------------------------------------------------------------------------------------
			if (_topCpuRstm.getRowCount() > 0)
			{
				for (int r=0; r<_topCpuRstm.getRowCount(); r++)
				{
					// Get Actual Executed SQL Text for current 'NormJavaSqlHashCode'
					Map<String, Object> whereColValMap = new LinkedHashMap<>();
					whereColValMap.put("plan_id", _topCpuRstm.getValueAsInteger(r, "plan_id"));
					whereColValMap.put("!dbname", _topCpuRstm.getValueAsString (r, "dbname"));

					_keyToSqlText = getQsSqlText(_keyToSqlText, conn, whereColValMap);
				}

//				calculateAvg(_topCpuRstm);
				
				// create a tooltip where the MICRO Seconds has been converted to a more readable format [HH:MM:SS.ms]
				
			}
			
			
			//-----------------------------------------------------------
			// WAIT --->>> Only if SQL-Server Version 2017 or above
			//-----------------------------------------------------------
			if (hasTable_query_store_wait_stats)
			{
//				sql = ""
//					    + "select top " + getTopRows() + " \n"
//					    + "     [plan_id] \n"
//					    + "    ,''                               as [plan_text] \n"
//					    + "    ,''                               as [txt] \n"
////					    + "--  ,[wait_stats_id] \n"
////					    + "--  ,[wait_category] \n"
//					    + "    ,[wait_category_desc] \n"
//
//					    + "    ,max([execution_type])           as [execution_type] \n"
//					    + "    ,max([execution_type_desc])      as [execution_type_desc] \n"
//					    + "    ,sum([total_query_wait_time_ms]) as [total_query_wait_time_ms] \n"
//					    + "    ,sum([avg_query_wait_time_ms])   as [avg_query_wait_time_ms] \n"
//					    + "    ,''                              as [avg_query_wait_time_ms__chart] \n"
////					    + "--  ,max([last_query_wait_time_ms])  as [last_query_wait_time_ms] \n"
//					    + "    ,min([min_query_wait_time_ms])   as [min_query_wait_time_ms] \n"
//					    + "    ,max([max_query_wait_time_ms])   as [max_query_wait_time_ms] \n"
////					    + "--  ,max([stdev_query_wait_time_ms]) as [stdev_query_wait_time_ms] \n"
//					    + "from [" + _schemaName + "].[query_store_wait_stats] \n"
////						+ getReportPeriodSqlWhere()
//					    + "group by [plan_id], [wait_category_desc] \n"
//					    + "order by [total_query_wait_time_ms] desc \n"
//					    + "";
				sql = ""
					    + "select top " + getTopRows() + " \n"
					    + "     '" + _dbname + "'                as [dbname] \n"
					    + "    ,[plan_id] \n"
//					    + "    ,cast([plan_id] as varchar(20))   as [plan_text] \n"
						+ "    ,''                               as [plan_text] \n"
					    + "    ,''                               as [txt] \n"
					    + "    ,''                               as [wait] \n"
					    + "    ,max([execution_type_desc])      as [execution_type_desc] \n"

					    + "    ,''                              as [total_query_wait_time_ms__chart] \n"
					    + "    ,sum([total_query_wait_time_ms]) as [total_query_wait_time_ms__sum] \n"

					    + "    ,''                              as [avg_query_wait_time_ms__chart] \n"
					    + "    ,sum([avg_query_wait_time_ms])   as [avg_query_wait_time_ms__sum] \n"
					    
//					    + "--  ,max([last_query_wait_time_ms])  as [last_query_wait_time_ms] \n"
					    + "    ,min([min_query_wait_time_ms])   as [min_query_wait_time_ms] \n"
					    + "    ,max([max_query_wait_time_ms])   as [max_query_wait_time_ms] \n"
//					    + "--  ,max([stdev_query_wait_time_ms]) as [stdev_query_wait_time_ms] \n"
					    + "from [" + _schemaName + "].[query_store_wait_stats] \n"
//						+ getReportPeriodSqlWhere()
					    + "group by [plan_id] \n"
					    + "order by [total_query_wait_time_ms__sum] desc \n"
					    + "";

				_topWaitRstm = executeQuery(conn, sql, true, _dbname + "_topWait");
				setWaitSectionDescription(_topWaitRstm);

				// Highlight sort column
				_topWaitRstm.setHighlightSortColumns("total_query_wait_time_ms__sum");

				// DESCRIBE_ME
//				createPlanCollectorForWait();
				_planCollectionWait = new QsExecutionPlanCollection(SqlServerQueryStore.this, _topWaitRstm, "qs_" + _dbname + "_wait", _schemaName);
//				_planCollectionWait.getPlansAndSubstituteWithLinks(conn, "dbname", "plan_id", "plan_text", null, null);
				_planCollectionWait.getPlans(conn, "dbname", "plan_id");

				//--------------------------------------------------------------------------------------
				// get "ALL Wait Entries" for 'plan_id' from QueryStore
				//--------------------------------------------------------------------------------------
				if (_topWaitRstm.getRowCount() > 0)
				{
					for (int r=0; r<_topWaitRstm.getRowCount(); r++)
					{
						// Get 'WitTimes' current 'NormJavaSqlHashCode'
						Map<String, Object> whereColValMap = new LinkedHashMap<>();
						whereColValMap.put("plan_id", _topWaitRstm.getValueAsInteger(r, "plan_id"));
						whereColValMap.put("!dbname", _topWaitRstm.getValueAsString (r, "dbname"));

						_keyToWaitTime = getQsWaitTime(_keyToWaitTime, conn, whereColValMap);
					}
				}
			}

			//-----------------------------------------------------------
			// COMPILE - not yet implemented
			//-----------------------------------------------------------
//			------- Compilation 
//			select top 20
//			     [query_id]                        
//			    ,max([object_id])                            as [object_id]
//			    ,max([query_hash])                           as [query_hash]
//			    ,max([is_internal_query])                    as [is_internal_query]               
//			--    ,max([query_parameterization_type])          as [query_parameterization_type]     
//			--    ,max([query_parameterization_type_desc])     as [query_parameterization_type_desc]
//			--    ,max([initial_compile_start_time])           as [initial_compile_start_time]      
//			--    ,max([last_compile_start_time])              as [last_compile_start_time]         
//			--    ,max([last_execution_time])                  as [last_execution_time]             
//			--    ,max([last_compile_batch_sql_handle])        as [last_compile_batch_sql_handle]   
//			--    ,max([last_compile_batch_offset_start])      as [last_compile_batch_offset_start] 
//			--    ,max([last_compile_batch_offset_end])        as [last_compile_batch_offset_end]   
//			    ,sum([count_compiles])                       as [count_compiles]                  
//			    ,sum([avg_compile_duration])                 as [avg_compile_duration]            
//			--    ,sum([last_compile_duration])                as [last_compile_duration]           
//			    ,sum([avg_bind_duration])                    as [avg_bind_duration]               
//			--    ,sum([last_bind_duration])                   as [last_bind_duration]              
//			    ,sum([avg_bind_cpu_time])                    as [avg_bind_cpu_time]               
//			--    ,sum([last_bind_cpu_time])                   as [last_bind_cpu_time]              
//			    ,sum([avg_optimize_duration])                as [avg_optimize_duration]           
//			--    ,sum([last_optimize_duration])               as [last_optimize_duration]          
//			    ,sum([avg_optimize_cpu_time])                as [avg_optimize_cpu_time]           
//			--    ,sum([last_optimize_cpu_time])               as [last_optimize_cpu_time]          
//			    ,sum([avg_compile_memory_kb])                as [avg_compile_memory_kb]           
//			--    ,sum([last_compile_memory_kb])               as [last_compile_memory_kb]          
//			--    ,sum([max_compile_memory_kb])                as [max_compile_memory_kb]           
//			--    ,max([is_clouddb_internal_query])            as [is_clouddb_internal_query]       
//
//			    ,sum([count_compiles])                       as [_count_compiles]                  
//			    ,sum([avg_compile_duration])  * sum([count_compiles])                as [total_compile_duration]            
//			    ,sum([avg_bind_duration])     * sum([count_compiles])                as [total_bind_duration]               
//			    ,sum([avg_bind_cpu_time])     * sum([count_compiles])                as [total_bind_cpu_time]               
//			    ,sum([avg_optimize_duration]) * sum([count_compiles])                as [total_optimize_duration]           
//			    ,sum([avg_optimize_cpu_time]) * sum([count_compiles])                as [total_optimize_cpu_time]           
//			    ,sum([avg_compile_memory_kb]) * sum([count_compiles])                as [total_compile_memory_kb]           
//
//			from [" + _schemaName + "].[query_store_query]
//			group by [query_id]
//			order by [total_compile_duration] desc

			
			
			
			// Mini Chart on "ExecCount"
			// Get data for: SparkLine - small chart values ... this will do:
			//  -- fill in the data cell with: <span class='aClassName' values='v1, v2, v2, v3...'>Mini Chart Here</span>
			//  -- return JavaScript Code to initialize the Spark line
			String whereKeyColumn = "plan_id"; 

			_miniChartJsList.add(SparklineHelper.createSparkline(conn, SqlServerQueryStore.this, _topCpuRstm, 
					SparkLineParams.create       (DataSource.QueryStore)
					.setSparklineClassNamePrefix (_dbname)
					.setHtmlChartColumnName      ("count_executions__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsSchemaName           (_schemaName)
					.setDbmsTableName            ("query_store_runtime_stats")
					.setDbmsDataValueColumnName  ("count_executions")   
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.validate()));
			
			_miniChartJsList.add(SparklineHelper.createSparkline(conn, SqlServerQueryStore.this, _topCpuRstm, 
					SparkLineParams.create       (DataSource.QueryStore)
					.setSparklineClassNamePrefix (_dbname)
					.setHtmlChartColumnName      ("avg_duration__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsSchemaName           (_schemaName)
					.setDbmsTableName            ("query_store_runtime_stats")
//					.setDbmsDataValueColumnName  ("avg_duration")   
					.setDbmsDataValueColumnName  ("sum([avg_duration]/1000.0)").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1)   // MS
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.validate()));
			
			_miniChartJsList.add(SparklineHelper.createSparkline(conn, SqlServerQueryStore.this, _topCpuRstm, 
					SparkLineParams.create       (DataSource.QueryStore)
					.setSparklineClassNamePrefix (_dbname)
					.setHtmlChartColumnName      ("avg_cpu_time__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsSchemaName           (_schemaName)
					.setDbmsTableName            ("query_store_runtime_stats")
//					.setDbmsDataValueColumnName  ("avg_cpu_time")   
					.setDbmsDataValueColumnName  ("sum([avg_cpu_time]/1000.0)").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1)   // MS
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.validate()));
			
//			_miniChartJsList.add(SparklineHelper.createSparkline(conn, SqlServerQueryStore.this, _topCpuRstm, 
//					SparkLineParams.create       (DataSource.QueryStore)
//					.setSparklineClassNamePrefix (_dbname)
//					.setHtmlChartColumnName      ("avg_wait_time__chart")
//					.setHtmlWhereKeyColumnName   (whereKeyColumn)
//					.setDbmsSchemaName           (_schemaName)
//					.setDbmsTableName            ("query_store_runtime_stats")
////					.setDbmsDataValueColumnName  ("avg_wait_time")
//					.setDbmsDataValueColumnName  ("cast((sum([count_executions]) * sum([avg_duration])) - (sum([count_executions]) * sum([avg_cpu_time])) as bigint)").setGroupDataAggregationType(AggType.USER_PROVIDED)
//					.setDbmsWhereKeyColumnName   (whereKeyColumn)
//					.setSparklineTooltipPostfix  ("WaitTime: 'total_duration' - 'total_cpu_time') in below time period")
//					.validate()));

			if (StringUtil.hasValue(avg_query_wait_time_ms_chart)) 
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, SqlServerQueryStore.this, _topCpuRstm, 
					SparkLineParams.create       (DataSource.QueryStore)
					.setSparklineClassNamePrefix (_dbname)
					.setHtmlChartColumnName      ("avg_query_wait_time_ms__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsSchemaName           (_schemaName)
					.setDbmsTableName            ("query_store_wait_stats")
					.setDbmsDataValueColumnName  ("avg_query_wait_time_ms").setDecimalScale(1)   // MS
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.validate()));

			if (StringUtil.hasValue(avg_query_wait_time_other_ms_chart)) 
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, SqlServerQueryStore.this, _topCpuRstm, 
            		SparkLineParams.create       (DataSource.QueryStore)
            		.setSparklineClassNamePrefix (_dbname)
            		.setHtmlChartColumnName      ("avg_query_wait_time_other_ms__chart")
            		.setHtmlWhereKeyColumnName   (whereKeyColumn)
            		.setDbmsSchemaName           (_schemaName)
            		.setDbmsTableName            ("query_store_wait_stats")
            		.setDbmsDataValueColumnName  ("sum(s.[avg_query_wait_time_ms]) / count(distinct s.[runtime_stats_interval_id])").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1)   // MS
            		.setDbmsExtraWhereClause     ("and s.[wait_category] != 16")  // 16 == Parallel
            		.setDbmsWhereKeyColumnName   (whereKeyColumn)
            		.validate()));
            
			if (StringUtil.hasValue(avg_query_wait_time_paralell_ms_chart)) 
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, SqlServerQueryStore.this, _topCpuRstm, 
            		SparkLineParams.create       (DataSource.QueryStore)
            		.setSparklineClassNamePrefix (_dbname)
            		.setHtmlChartColumnName      ("avg_query_wait_time_paralell_ms__chart")
            		.setHtmlWhereKeyColumnName   (whereKeyColumn)
            		.setDbmsSchemaName           (_schemaName)
            		.setDbmsTableName            ("query_store_wait_stats")
            		.setDbmsDataValueColumnName  ("sum(s.[avg_query_wait_time_ms]) / count(distinct s.[runtime_stats_interval_id])").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1)   // MS
            		.setDbmsExtraWhereClause     ("and s.[wait_category] = 16")  // 16 == Parallel
            		.setDbmsWhereKeyColumnName   (whereKeyColumn)
            		.validate()));
			
			_miniChartJsList.add(SparklineHelper.createSparkline(conn, SqlServerQueryStore.this, _topCpuRstm, 
					SparkLineParams.create       (DataSource.QueryStore)
					.setSparklineClassNamePrefix (_dbname)
					.setHtmlChartColumnName      ("avg_logical_io_reads__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsSchemaName           (_schemaName)
					.setDbmsTableName            ("query_store_runtime_stats")
					.setDbmsDataValueColumnName  ("avg_logical_io_reads")   
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.validate()));
			
			_miniChartJsList.add(SparklineHelper.createSparkline(conn, SqlServerQueryStore.this, _topCpuRstm, 
					SparkLineParams.create       (DataSource.QueryStore)
					.setSparklineClassNamePrefix (_dbname)
					.setHtmlChartColumnName      ("avg_logical_io_reads_mb__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsSchemaName           (_schemaName)
					.setDbmsTableName            ("query_store_runtime_stats")
//					.setDbmsDataValueColumnName  ("avg_logical_io_reads")   
            		.setDbmsDataValueColumnName  ("sum([avg_logical_io_reads]) / 128.0").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1)   // MB
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.validate()));
			
			_miniChartJsList.add(SparklineHelper.createSparkline(conn, SqlServerQueryStore.this, _topCpuRstm, 
					SparkLineParams.create       (DataSource.QueryStore)
					.setSparklineClassNamePrefix (_dbname)
					.setHtmlChartColumnName      ("avg_logical_io_writes__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsSchemaName           (_schemaName)
					.setDbmsTableName            ("query_store_runtime_stats")
					.setDbmsDataValueColumnName  ("avg_logical_io_writes")   
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.validate()));
			
			_miniChartJsList.add(SparklineHelper.createSparkline(conn, SqlServerQueryStore.this, _topCpuRstm, 
					SparkLineParams.create       (DataSource.QueryStore)
					.setSparklineClassNamePrefix (_dbname)
					.setHtmlChartColumnName      ("avg_physical_io_reads__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsSchemaName           (_schemaName)
					.setDbmsTableName            ("query_store_runtime_stats")
					.setDbmsDataValueColumnName  ("avg_physical_io_reads")   
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.validate()));
			
			_miniChartJsList.add(SparklineHelper.createSparkline(conn, SqlServerQueryStore.this, _topCpuRstm, 
					SparkLineParams.create       (DataSource.QueryStore)
					.setSparklineClassNamePrefix (_dbname)
					.setHtmlChartColumnName      ("avg_clr_time__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsSchemaName           (_schemaName)
					.setDbmsTableName            ("query_store_runtime_stats")
//					.setDbmsDataValueColumnName  ("avg_clr_time")   
					.setDbmsDataValueColumnName  ("sum([avg_clr_time]/1000.0)").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1)   // MS
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.validate()));
			
			_miniChartJsList.add(SparklineHelper.createSparkline(conn, SqlServerQueryStore.this, _topCpuRstm, 
					SparkLineParams.create       (DataSource.QueryStore)
					.setSparklineClassNamePrefix (_dbname)
					.setHtmlChartColumnName      ("avg_dop__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsSchemaName           (_schemaName)
					.setDbmsTableName            ("query_store_runtime_stats")
					.setDbmsDataValueColumnName  ("avg_dop")   
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.validate()));
			
			_miniChartJsList.add(SparklineHelper.createSparkline(conn, SqlServerQueryStore.this, _topCpuRstm, 
					SparkLineParams.create       (DataSource.QueryStore)
					.setSparklineClassNamePrefix (_dbname)
					.setHtmlChartColumnName      ("avg_query_max_used_memory__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsSchemaName           (_schemaName)
					.setDbmsTableName            ("query_store_runtime_stats")
//					.setDbmsDataValueColumnName  ("avg_query_max_used_memory")   
					.setDbmsDataValueColumnName  ("sum([avg_query_max_used_memory]/128.0)").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(3)   // MB
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.validate()));
			
			_miniChartJsList.add(SparklineHelper.createSparkline(conn, SqlServerQueryStore.this, _topCpuRstm, 
					SparkLineParams.create       (DataSource.QueryStore)
					.setSparklineClassNamePrefix (_dbname)
					.setHtmlChartColumnName      ("avg_rowcount__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsSchemaName           (_schemaName)
					.setDbmsTableName            ("query_store_runtime_stats")
					.setDbmsDataValueColumnName  ("avg_rowcount")   
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.validate()));
			
			if (StringUtil.hasValue(avg_num_physical_io_reads_chart)) 
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, SqlServerQueryStore.this, _topCpuRstm, 
					SparkLineParams.create       (DataSource.QueryStore)
					.setSparklineClassNamePrefix (_dbname)
					.setHtmlChartColumnName      ("avg_num_physical_io_reads__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsSchemaName           (_schemaName)
					.setDbmsTableName            ("query_store_runtime_stats")
					.setDbmsDataValueColumnName  ("avg_num_physical_io_reads")   
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.validate()));
			
			if (StringUtil.hasValue(avg_log_bytes_used_chart)) 
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, SqlServerQueryStore.this, _topCpuRstm, 
					SparkLineParams.create       (DataSource.QueryStore)
					.setSparklineClassNamePrefix (_dbname)
					.setHtmlChartColumnName      ("avg_log_bytes_used__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsSchemaName           (_schemaName)
					.setDbmsTableName            ("query_store_runtime_stats")
					.setDbmsDataValueColumnName  ("avg_log_bytes_used")   
					.setDbmsDataValueColumnName  ("sum([avg_log_bytes_used]/1024.0/1024)").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(3)   // MB
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.validate()));
			
			if (StringUtil.hasValue(avg_tempdb_space_used_chart)) 
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, SqlServerQueryStore.this, _topCpuRstm, 
					SparkLineParams.create       (DataSource.QueryStore)
					.setSparklineClassNamePrefix (_dbname)
					.setHtmlChartColumnName      ("avg_tempdb_space_used__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsSchemaName           (_schemaName)
					.setDbmsTableName            ("query_store_runtime_stats")
//					.setDbmsDataValueColumnName  ("avg_tempdb_space_used")   
					.setDbmsDataValueColumnName  ("sum([avg_tempdb_space_used]/128.0)").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(3)   // MB
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.validate()));
			

			// WAITS
			if (hasTable_query_store_wait_stats)
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, SqlServerQueryStore.this, _topWaitRstm, 
					SparkLineParams.create       (DataSource.QueryStore)
					.setSparklineClassNamePrefix (_dbname)
					.setHtmlChartColumnName      ("total_query_wait_time_ms__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsSchemaName           (_schemaName)
					.setDbmsTableName            ("query_store_wait_stats")
					.setDbmsDataValueColumnName  ("total_query_wait_time_ms")   
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.validate()));
			
			if (hasTable_query_store_wait_stats)
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, SqlServerQueryStore.this, _topWaitRstm, 
					SparkLineParams.create       (DataSource.QueryStore)
					.setSparklineClassNamePrefix (_dbname)
					.setHtmlChartColumnName      ("avg_query_wait_time_ms__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsSchemaName           (_schemaName)
					.setDbmsTableName            ("query_store_wait_stats")
					.setDbmsDataValueColumnName  ("avg_query_wait_time_ms")   
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.validate()));
			
		}

		
//		public void createSqlTextTable(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
		public void createSqlTextTable(DbxConnection conn)
		{
			SimpleResultSet srs = new SimpleResultSet();
			ResultSetTableModel rstm = _topCpuRstm;

			srs.addColumn("dbname"     , Types.VARCHAR,       60, 0);
			srs.addColumn("plan_id"    , Types.VARCHAR,       60, 0);
			srs.addColumn("sparklines" , Types.VARCHAR,      512, 0); 
			srs.addColumn("SQLText"    , Types.VARCHAR, 1024*128, 0); // this is 'text' in the origin table

			// Position in the "source" _shortRstm table (values we will fetch)
			int pos_dbname  = rstm.findColumn("dbname");
			int pos_plan_id = rstm.findColumn("plan_id");
//			int pos_query   = rstm.findColumn("SqlText");

			// Special Content Producer, that shows detailed Wait Information as a "tooltip help"
			HtmlTableProducer.ColumnContentProducer waitInfo = new HtmlTableProducer.ColumnContentProducer()
			{
				@Override
				public String getValue(ResultSetTableModel rstm, int rstmRow, String rowKey)
				{
					// Get Actual Executed SQL Text for current 'plan_id'
					int    plan_id = rstm.getValueAsInteger(rstmRow, "plan_id");
					String dbname  = rstm.getValueAsString (rstmRow, "dbname");
					
					Map<String, Object> whereColValMap = new LinkedHashMap<>();
					whereColValMap.put("plan_id", plan_id);
					whereColValMap.put("!dbname", dbname);
					
					String waitText = getQsWaitTimeAsString(_keyToWaitTime, whereColValMap, rstm, rstmRow);

					if (StringUtil.isNullOrBlank(waitText))
						return "<i>ms</i>";

					// Put the "Actual Executed SQL Text" as a "tooltip"
					return "<div title='Click for Detailes' "
							+ "data-toggle='modal' "
							+ "data-target='#dbx-view-sqltext-dialog' "
							+ "data-objectname='" + plan_id + "' "
							+ "data-tooltip=\""   + waitText     + "\" "
							+ "><i>ms</i>&#x1F4AC;</div>"; // ms... (Vertical Horizontal Ellipsis)
//							+ ">&#x1F4AC;</div>"; // symbol popup with "..."
				}
			};

			ColumnCopyRender msToHMS    = HtmlTableProducer.MS_TO_HMS;
//			ColumnCopyRender usToHMS    = HtmlTableProducer.US_TO_HMS;
			ColumnCopyRender oneDecimal = HtmlTableProducer.ONE_DECIMAL;

			HtmlTableProducer htp = new HtmlTableProducer(rstm, "dsr-sub-table-chart");
//			htp.setTableHeaders("Charts at 10 minute interval", "Total;style='text-align:right!important'", "Avg per exec;style='text-align:right!important'", "");
			htp.setTableHeaders("Charts at QS flush interval", "Total;style='text-align:right!important'", "Avg per exec;style='text-align:right!important'", "");
			if (rstm.hasColumn("count_executions__chart"               )) htp.add("exec-cnt"     , new ColumnCopyRow().add( new ColumnCopyDef("count_executions__chart"               ) ).add(new ColumnCopyDef("count_executions__sum").setColBold()              ).addEmptyCol()                                                                          .addEmptyCol() );
			if (rstm.hasColumn("avg_duration__chart"                   )) htp.add("exec-time"    , new ColumnCopyRow().add( new ColumnCopyDef("avg_duration__chart"                   ) ).add(new ColumnCopyDef("total_duration_ms__sum"                , msToHMS) ).add(new ColumnCopyDef("avg_duration_ms__sum"                , oneDecimal).setColBold()).add(new ColumnStatic("ms")) );
			if (rstm.hasColumn("avg_cpu_time__chart"                   )) htp.add("cpu-time"     , new ColumnCopyRow().add( new ColumnCopyDef("avg_cpu_time__chart"                   ) ).add(new ColumnCopyDef("total_cpu_time_ms__sum"                , msToHMS) ).add(new ColumnCopyDef("avg_cpu_time_ms__sum"                , oneDecimal).setColBold()).add(new ColumnStatic("ms")) );
//			if (rstm.hasColumn("avg_query_wait_time_ms__chart"         )) htp.add("wait-time"    , new ColumnCopyRow().add( new ColumnCopyDef("avg_query_wait_time_ms__chart"         ) ).add(new ColumnCopyDef("total_query_wait_time_ms__sum"         , msToHMS) ).add(new ColumnCopyDef("avg_query_wait_time_ms__sum"         , oneDecimal).setColBold()).add(new ColumnStatic("ms")) );
			if (rstm.hasColumn("avg_query_wait_time_ms__chart"         )) htp.add("wait-time"    , new ColumnCopyRow().add( new ColumnCopyDef("avg_query_wait_time_ms__chart"         ) ).add(new ColumnCopyDef("total_query_wait_time_ms__sum"         , msToHMS) ).add(new ColumnCopyDef("avg_query_wait_time_ms__sum"         , oneDecimal).setColBold()).add(waitInfo) );
		    if (rstm.hasColumn("avg_query_wait_time_other_ms__chart"   )) htp.add("wait-other"   , new ColumnCopyRow().add( new ColumnCopyDef("avg_query_wait_time_other_ms__chart"   ) ).add(new ColumnCopyDef("total_query_wait_time_other_ms__sum"   , msToHMS) ).add(new ColumnCopyDef("avg_query_wait_time_other_ms__sum"   , oneDecimal).setColBold()).add(new ColumnStatic("ms")) );
			if (rstm.hasColumn("avg_query_wait_time_paralell_ms__chart")) htp.add("wait-parallel", new ColumnCopyRow().add( new ColumnCopyDef("avg_query_wait_time_paralell_ms__chart") ).add(new ColumnCopyDef("total_query_wait_time_paralell_ms__sum", msToHMS) ).add(new ColumnCopyDef("avg_query_wait_time_paralell_ms__sum", oneDecimal).setColBold()).add(new ColumnStatic("ms")) );
			if (rstm.hasColumn("avg_clr_time__chart"                   )) htp.add("clr-time"     , new ColumnCopyRow().add( new ColumnCopyDef("avg_clr_time__chart"                   ) ).add(new ColumnCopyDef("total_clr_time_ms__sum"                , msToHMS) ).add(new ColumnCopyDef("avg_clr_time_ms__sum"                , oneDecimal).setColBold()).add(new ColumnStatic("ms")) );
			if (rstm.hasColumn("avg_dop__chart"                        )) htp.add("dop"          , new ColumnCopyRow().add( new ColumnCopyDef("avg_dop__chart"                        ) ).addEmptyCol()                                                             .add(new ColumnCopyDef("avg_dop__avg"                        , oneDecimal).setColBold()).add(new ColumnStatic("#")) );
			if (rstm.hasColumn("avg_rowcount__chart"                   )) htp.add("rowcount"     , new ColumnCopyRow().add( new ColumnCopyDef("avg_rowcount__chart"                   ) ).add(new ColumnCopyDef("total_rowcount__sum"                            ) ).add(new ColumnCopyDef("avg_rowcount__sum"                   , oneDecimal).setColBold()).add(new ColumnStatic("rows")) );
			if (rstm.hasColumn("avg_logical_io_reads__chart"           )) htp.add("l-read"       , new ColumnCopyRow().add( new ColumnCopyDef("avg_logical_io_reads__chart"           ) ).add(new ColumnCopyDef("total_logical_io_reads__sum"                    ) ).add(new ColumnCopyDef("avg_logical_io_reads__sum"           , oneDecimal).setColBold()).add(new ColumnStatic("pgs")) );
			if (rstm.hasColumn("avg_logical_io_reads_mb__chart"        )) htp.add("l-read-mb"    , new ColumnCopyRow().add( new ColumnCopyDef("avg_logical_io_reads_mb__chart"        ) ).add(new ColumnCopyDef("total_logical_io_reads_mb__sum"                 ) ).add(new ColumnCopyDef("avg_logical_io_reads_mb__sum"        , oneDecimal).setColBold()).add(new ColumnStatic("mb" )) );
			if (rstm.hasColumn("avg_physical_io_reads__chart"          )) htp.add("p-read"       , new ColumnCopyRow().add( new ColumnCopyDef("avg_physical_io_reads__chart"          ) ).add(new ColumnCopyDef("total_physical_io_reads__sum"                   ) ).add(new ColumnCopyDef("avg_physical_io_reads__sum"          , oneDecimal).setColBold()).add(new ColumnStatic("pgs")) );
			if (rstm.hasColumn("avg_num_physical_io_reads__chart"      )) htp.add("pio-read"     , new ColumnCopyRow().add( new ColumnCopyDef("avg_num_physical_io_reads__chart"      ) ).add(new ColumnCopyDef("total_num_physical_io_reads__sum"               ) ).add(new ColumnCopyDef("avg_num_physical_io_reads__sum"      , oneDecimal).setColBold()).add(new ColumnStatic("ios")) );
			if (rstm.hasColumn("avg_logical_io_writes__chart"          )) htp.add("l-write"      , new ColumnCopyRow().add( new ColumnCopyDef("avg_logical_io_writes__chart"          ) ).add(new ColumnCopyDef("total_logical_io_writes__sum"                   ) ).add(new ColumnCopyDef("avg_logical_io_writes__sum"          , oneDecimal).setColBold()).add(new ColumnStatic("pgs")) );
			if (rstm.hasColumn("avg_query_max_used_memory__chart"      )) htp.add("mem-used"     , new ColumnCopyRow().add( new ColumnCopyDef("avg_query_max_used_memory__chart"      ) ).add(new ColumnCopyDef("total_query_max_used_memory_mb__sum"            ) ).add(new ColumnCopyDef("avg_query_max_used_memory_mb__sum"   , oneDecimal).setColBold()).add(new ColumnStatic("mb")) );
			if (rstm.hasColumn("avg_log_bytes_used__chart"             )) htp.add("xlog-mb"      , new ColumnCopyRow().add( new ColumnCopyDef("avg_log_bytes_used__chart"             ) ).add(new ColumnCopyDef("total_log_bytes_used_mb__sum"                   ) ).add(new ColumnCopyDef("avg_log_bytes_used_mb__sum"          , oneDecimal).setColBold()).add(new ColumnStatic("mb")) );
			if (rstm.hasColumn("avg_tempdb_space_used__chart"          )) htp.add("tempdb"       , new ColumnCopyRow().add( new ColumnCopyDef("avg_tempdb_space_used__chart"          ) ).add(new ColumnCopyDef("total_tempdb_space_used_mb__sum"                ) ).add(new ColumnCopyDef("avg_tempdb_space_used_mb__sum"       , oneDecimal).setColBold()).add(new ColumnStatic("mb")) );
			htp.validate();
			
			// Filter out some rows...
			htp.setRowFilter(new HtmlTableProducer.RowFilter()
			{
				@Override
				public boolean include(ResultSetTableModel rstm, int rstmRow, String rowKey)
				{
					if ("wait-other".equals(rowKey) || "wait-parallel".equals(rowKey))
					{
						return rstm.hasColumn("avg_dop__avg") && rstm.getValueAsInteger(rstmRow, "avg_dop__avg") > 1;
					}
					return true;
				}
			});

			// Loop the RSTM and create a ResultSet that is later translated to another RSTM (containing spark-lines and SQL Text) 
			if (pos_dbname >= 0 && pos_plan_id >= 0)
			{
				for (int r=0; r<rstm.getRowCount(); r++)
				{
					String     dbname  = rstm.getValueAsString(r, pos_dbname);
					Integer    plan_id = rstm.getValueAsInteger(r, pos_plan_id);

					// Get SQL Text
					Map<String, Object> whereColValMap = new LinkedHashMap<>();
					whereColValMap.put("plan_id", plan_id);
					whereColValMap.put("!dbname", dbname);

					String sqlText = "--not-found--";
					QsSqlTextEntry entry = _keyToSqlText.get(whereColValMap);
					if (entry != null)
						sqlText = entry.sqlText;

					// Parse the 'sqlText' and extract Table Names, then get various table and index information
					String tableInfo = getDbmsTableInformationFromSqlText(conn, sqlText, DbUtils.DB_PROD_NAME_MSSQL);

//					// Parse the 'sqlText' and extract Table Names..
//					// - then get table information (like we do in 'AseTopCmObjectActivity')
//					String tableInfo = "";
//					boolean parseSqlText = true;
//					if (parseSqlText)
//					{
//						// Parse the SQL Text to get all tables that are used in the Statement
//						String problemDesc = "";
//						Set<String> tableList = SqlParserUtils.getTables(sqlText);
////						List<String> tableList = Collections.emptyList();
////						try { tableList = SqlParserUtils.getTables(sqlText, true); }
////						catch (ParseException pex) { problemDesc = pex + ""; }
//
//						// Get information about ALL tables in list 'tableList' from the DDL Storage (or other counter collectors)
//						Set<SqlServerTableInfo> tableInfoSet = getTableInformationFromMonDdlStorage(conn, tableList);
//						if (tableInfoSet.isEmpty() && StringUtil.isNullOrBlank(problemDesc))
//							problemDesc = "&emsp; &bull; No tables was found in the DDL Storage for tables: " + listToHtmlCode(tableList);
//
//						// And make it into a HTML table with various information about the table and indexes 
//						tableInfo = problemDesc + getTableInfoAsHtmlTable(tableInfoSet, tableList, true, "dsr-sub-table-tableinfo");
//
//						// Finally make up a message that will be appended to the SQL Text
//						if (StringUtil.hasValue(tableInfo))
//						{
//							// Surround with collapse div
//							tableInfo = ""
//									//+ "<!--[if !mso]><!--> \n" // BEGIN: IGNORE THIS SECTION FOR OUTLOOK
//
//									+ "\n<br>\n<br>\n"
//									+ "<details open> \n"
//									+ "<summary>Show/Hide Table information for " + tableList.size() + " table(s): " + listToHtmlCode(tableList) + "</summary> \n"
//									+ tableInfo
//									+ "</details> \n"
//
//									//+ "<!--<![endif]-->    \n" // END: IGNORE THIS SECTION FOR OUTLOOK
//									+ "";
//						}
//					}

					// get the "spark lines"
					String sparklines = htp.getHtmlTextForRow(r);

					sqlText = "<xmp>" + sqlText + "</xmp>" + tableInfo;

					// add record to SimpleResultSet
					srs.addRow(dbname, plan_id, sparklines, sqlText);
				}
			}

			// Create a ResultSetTableModel to hold the information
			try
			{
				// Note the 'srs' is populated when reading above ResultSet from query
				_cpuSqlTextRstm = createResultSetTableModel(srs, "Top SQL TEXT", null, false); // DO NOT TRUNCATE COLUMNS
				srs.close();

			}
			catch (SQLException ex)
			{
				setProblemException(ex);
			
				_cpuSqlTextRstm = ResultSetTableModel.createEmpty("Top SQL TEXT");
				_logger.warn("Problems getting Top SQL TEXT: " + ex);
			}
		}
		

		/**
		 * Set CPU column descriptions
		 */
		protected void setCpuSectionDescription(ResultSetTableModel rstm)
		{
			if (rstm == null)
				return;
			
			// Section description
			rstm.setDescription("<b>&bull; Top CPU</b> (ordered by: total_cpu_time)");

			// Columns description
			rstm.setColumnDescription("runtime_stats_id"                 , "Identifier of the row representing runtime execution statistics for the plan_id, execution_type and runtime_stats_interval_id. It is unique only for the past runtime statistics intervals. For currently active interval there may be multiple rows representing runtime statistics for the plan referenced by plan_id, with the execution type represented by execution_type. Typically, one row represents runtime statistics that are flushed to disk, while other(s) represent in-memory state. Hence, to get actual state for every interval you need to aggregate metrics, grouping by plan_id, execution_type and runtime_stats_interval_id.");
			rstm.setColumnDescription("plan_id"                          , "Foreign key. Joins to sys.query_store_plan (Transact-SQL).");
			rstm.setColumnDescription("runtime_stats_interval_id"        , "Foreign key. Joins to sys.query_store_runtime_stats_interval (Transact-SQL).");
			rstm.setColumnDescription("execution_type"                   , "Determines type of query execution: 0 - Regular execution (successfully finished); 3 - Client initiated aborted execution; 4 - Exception aborted execution;");
			rstm.setColumnDescription("execution_type_desc"              , "Textual description of the execution type field: 0 - Regular; 3 - Aborted; 4 - Exception;");
			rstm.setColumnDescription("first_execution_time"             , "First execution time for the query plan within the aggregation interval. This refers to the end time of the query execution.");
			rstm.setColumnDescription("last_execution_time"              , "Last execution time for the query plan within the aggregation interval. This refers to the end time of the query execution.");
			
			rstm.setColumnDescription("count_executions"                 , "Total count of executions for the query plan within the aggregation interval.");
			rstm.setColumnDescription("count_executions__sum"            , "Summary of: Total count of executions for the query plan within the recording period (usually 24 hours).");
			rstm.setColumnDescription("count_executions__chart"          , "Chart of:   Total count of executions for the query plan within the aggregation interval.");
                                                                         
			rstm.setColumnDescription("avg_duration_ms__sum"             , "Summary of: Average duration for the query plan within the recording period (usually 24 hours) (reported in milliseconds) .");
			rstm.setColumnDescription("avg_duration__sum"                , "Summary of: Average duration for the query plan within the recording period (usually 24 hours) (reported in microseconds) .");
			rstm.setColumnDescription("avg_duration__chart"              , "Chart of:   Average duration for the query plan within the aggregation interval (reported in microseconds) .");
			rstm.setColumnDescription("avg_duration"                     , "Average duration for the query plan within the aggregation interval (reported in microseconds) .");
			rstm.setColumnDescription("last_duration"                    , "Last duration for the query plan within the aggregation interval (reported in microseconds).");
			rstm.setColumnDescription("min_duration"                     , "Minimum duration for the query plan within the aggregation interval (reported in microseconds).");
			rstm.setColumnDescription("max_duration"                     , "Maximum duration for the query plan within the aggregation interval (reported in microseconds).");
			rstm.setColumnDescription("stdev_duration"                   , "Duration standard deviation for the query plan within the aggregation interval (reported in microseconds).");
                                                                         
			rstm.setColumnDescription("avg_cpu_time_ms__sum"             , "Summary of: Average CPU time for the query plan within the recording period (usually 24 hours) (reported in milliseconds).");
			rstm.setColumnDescription("avg_cpu_time__sum"                , "Summary of: Average CPU time for the query plan within the recording period (usually 24 hours) (reported in microseconds).");
			rstm.setColumnDescription("avg_cpu_time__chart"              , "Chart of:   Average CPU time for the query plan within the aggregation interval (reported in microseconds).");
			rstm.setColumnDescription("avg_cpu_time"                     , "Average CPU time for the query plan within the aggregation interval (reported in microseconds).");
			rstm.setColumnDescription("last_cpu_time"                    , "Last CPU time for the query plan within the aggregation interval (reported in microseconds).");
			rstm.setColumnDescription("min_cpu_time"                     , "Minimum CPU time for the query plan within the aggregation interval (reported in microseconds).");
			rstm.setColumnDescription("max_cpu_time"                     , "Maximum CPU time for the query plan within the aggregation interval (reported in microseconds).");
			rstm.setColumnDescription("stdev_cpu_time"                   , "CPU time standard deviation for the query plan within the aggregation interval (reported in microseconds).");
                                                                         
			rstm.setColumnDescription("avg_logical_io_reads__sum"        , "Summary of: Average number of logical I/O reads for the query plan within the recording period (usually 24 hours). (expressed as a number of 8KB pages read).");
			rstm.setColumnDescription("avg_logical_io_reads__chart"      , "Chart of:   Average number of logical I/O reads for the query plan within the aggregation interval. (expressed as a number of 8KB pages read).");
			rstm.setColumnDescription("avg_logical_io_reads"             , "Average number of logical I/O reads for the query plan within the aggregation interval. (expressed as a number of 8KB pages read).");
			rstm.setColumnDescription("last_logical_io_reads"            , "Last number of logical I/O reads for the query plan within the aggregation interval. (expressed as a number of 8KB pages read).");
			rstm.setColumnDescription("min_logical_io_reads"             , "Minimum number of logical I/O reads for the query plan within the aggregation interval. (expressed as a number of 8KB pages read).");
			rstm.setColumnDescription("max_logical_io_reads"             , "Maximum number of logical I/O reads for the query plan within the aggregation interval.(expressed as a number of 8KB pages read).");
			rstm.setColumnDescription("stdev_logical_io_reads"           , "Number of logical I/O reads standard deviation for the query plan within the aggregation interval. (expressed as a number of 8KB pages read).");
			                                                             
			rstm.setColumnDescription("avg_logical_io_writes__sum"       , "Summary of: Average number of logical I/O writes for the query plan within the recording period (usually 24 hours).");
			rstm.setColumnDescription("avg_logical_io_writes__chart"     , "Chart of:   Average number of logical I/O writes for the query plan within the aggregation interval.");
			rstm.setColumnDescription("avg_logical_io_writes"            , "Average number of logical I/O writes for the query plan within the aggregation interval.");
			rstm.setColumnDescription("last_logical_io_writes"           , "Last number of logical I/O writes for the query plan within the aggregation interval.");
			rstm.setColumnDescription("min_logical_io_writes"            , "Minimum number of logical I/O writes for the query plan within the aggregation interval.");
			rstm.setColumnDescription("max_logical_io_writes"            , "Maximum number of logical I/O writes for the query plan within the aggregation interval");
			rstm.setColumnDescription("stdev_logical_io_writes"          , "Number of logical I/O writes standard deviation for the query plan within the aggregation interval.");
			                                                             
			rstm.setColumnDescription("avg_physical_io_reads__sum"       , "Summary of: Average number of physical I/O reads for the query plan within the recording period (usually 24 hours) (expressed as a number of 8KB pages read).");
			rstm.setColumnDescription("avg_physical_io_reads__chart"     , "Chart of:   Average number of physical I/O reads for the query plan within the aggregation interval (expressed as a number of 8KB pages read).");
			rstm.setColumnDescription("avg_physical_io_reads"            , "Average number of physical I/O reads for the query plan within the aggregation interval (expressed as a number of 8KB pages read).");
			rstm.setColumnDescription("last_physical_io_reads"           , "Last number of physical I/O reads for the query plan within the aggregation interval (expressed as a number of 8KB pages read).");
			rstm.setColumnDescription("min_physical_io_reads"            , "Minimum number of physical I/O reads for the query plan within the aggregation interval (expressed as a number of 8KB pages read).");
			rstm.setColumnDescription("max_physical_io_reads"            , "Maximum number of physical I/O reads for the query plan within the aggregation interval (expressed as a number of 8KB pages read).");
			rstm.setColumnDescription("stdev_physical_io_reads"          , "Number of physical I/O reads standard deviation for the query plan within the aggregation interval (expressed as a number of 8KB pages read).");
			                                                             
			rstm.setColumnDescription("avg_clr_time_ms__sum"             , "Summary of: Average CLR time for the query plan within the recording period (usually 24 hours) (reported in milliseconds).");
			rstm.setColumnDescription("avg_clr_time__sum"                , "Summary of: Average CLR time for the query plan within the recording period (usually 24 hours) (reported in microseconds).");
			rstm.setColumnDescription("avg_clr_time__chart"              , "Chart of:   Average CLR time for the query plan within the aggregation interval (reported in microseconds).");
			rstm.setColumnDescription("avg_clr_time"                     , "Average CLR time for the query plan within the aggregation interval (reported in microseconds).");
			rstm.setColumnDescription("last_clr_time"                    , "Last CLR time for the query plan within the aggregation interval (reported in microseconds).");
			rstm.setColumnDescription("min_clr_time"                     , "Minimum CLR time for the query plan within the aggregation interval (reported in microseconds).");
			rstm.setColumnDescription("max_clr_time"                     , "Maximum CLR time for the query plan within the aggregation interval (reported in microseconds).");
			rstm.setColumnDescription("stdev_clr_time"                   , "CLR time standard deviation for the query plan within the aggregation interval (reported in microseconds).");
			                                                             
			rstm.setColumnDescription("avg_dop__avg"                     , "Average of: Average DOP (degree of parallelism) for the query plan within the recording period (usually 24 hours).");
			rstm.setColumnDescription("avg_dop__sum"                     , "Summary of: Average DOP (degree of parallelism) for the query plan within the recording period (usually 24 hours).");
			rstm.setColumnDescription("avg_dop__chart"                   , "Chart of:   Average DOP (degree of parallelism) for the query plan within the aggregation interval.");
			rstm.setColumnDescription("avg_dop"                          , "Average DOP (degree of parallelism) for the query plan within the aggregation interval.");
			rstm.setColumnDescription("last_dop"                         , "Last DOP (degree of parallelism) for the query plan within the aggregation interval.");
			rstm.setColumnDescription("min_dop"                          , "Minimum DOP (degree of parallelism) for the query plan within the aggregation interval.");
			rstm.setColumnDescription("max_dop"                          , "Maximum DOP (degree of parallelism) for the query plan within the aggregation interval.");
			rstm.setColumnDescription("stdev_dop"                        , "DOP (degree of parallelism) standard deviation for the query plan within the aggregation interval.");
			
			rstm.setColumnDescription("avg_query_max_used_memory__sum"   , "Summary of: Average memory grant (reported as the number of 8 KB pages) for the query plan within the recording period (usually 24 hours). Always 0 for queries using natively compiled memory optimized procedures.");
			rstm.setColumnDescription("avg_query_max_used_memory__chart" , "Chart of:   Average memory grant (reported as the number of 8 KB pages) for the query plan within the aggregation interval. Always 0 for queries using natively compiled memory optimized procedures.");
			rstm.setColumnDescription("avg_query_max_used_memory"        , "Average memory grant (reported as the number of 8 KB pages) for the query plan within the aggregation interval. Always 0 for queries using natively compiled memory optimized procedures.");
			rstm.setColumnDescription("last_query_max_used_memory"       , "Last memory grant (reported as the number of 8 KB pages) for the query plan within the aggregation interval. Always 0 for queries using natively compiled memory optimized procedures.");
			rstm.setColumnDescription("min_query_max_used_memory"        , "Minimum memory grant (reported as the number of 8 KB pages) for the query plan within the aggregation interval. Always 0 for queries using natively compiled memory optimized procedures.");
			rstm.setColumnDescription("max_query_max_used_memory"        , "Maximum memory grant (reported as the number of 8 KB pages) for the query plan within the aggregation interval. Always 0 for queries using natively compiled memory optimized procedures.");
			rstm.setColumnDescription("stdev_query_max_used_memory"      , "Memory grant standard deviation (reported as the number of 8 KB pages) for the query plan within the aggregation interval. Always 0 for queries using natively compiled memory optimized procedures.");
			
			rstm.setColumnDescription("avg_rowcount__sum"                , "Summary of: Average number of returned rows for the query plan within the recording period (usually 24 hours).");
			rstm.setColumnDescription("avg_rowcount__chart"              , "Chart of:   Average number of returned rows for the query plan within the aggregation interval.");
			rstm.setColumnDescription("avg_rowcount"                     , "Average number of returned rows for the query plan within the aggregation interval.");
			rstm.setColumnDescription("last_rowcount"                    , "Number of returned rows by the last execution of the query plan within the aggregation interval.");
			rstm.setColumnDescription("min_rowcount"                     , "Minimum number of returned rows for the query plan within the aggregation interval.");
			rstm.setColumnDescription("max_rowcount"                     , "Maximum number of returned rows for the query plan within the aggregation interval.");
			rstm.setColumnDescription("stdev_rowcount"                   , "Number of returned rows standard deviation for the query plan within the aggregation interval.");
			
			rstm.setColumnDescription("avg_num_physical_io_reads__sum"   , "Summary of: Average number of physical I/O reads for the query plan within the recording period (usually 24 hours) (expressed as a number of 8KB pages read).");
			rstm.setColumnDescription("avg_num_physical_io_reads__chart" , "Chart of:   Average number of physical I/O reads for the query plan within the aggregation interval (expressed as a number of 8KB pages read).");
			rstm.setColumnDescription("avg_num_physical_io_reads"        , "Average number of physical I/O reads for the query plan within the aggregation interval (expressed as a number of 8KB pages read).");
			rstm.setColumnDescription("last_num_physical_io_reads"       , "Last number of physical I/O reads for the query plan within the aggregation interval (expressed as a number of 8KB pages read).");
			rstm.setColumnDescription("min_num_physical_io_reads"        , "Minimum number of physical I/O reads for the query plan within the aggregation interval (expressed as a number of 8KB pages read).");
			rstm.setColumnDescription("max_num_physical_io_reads"        , "Maximum number of physical I/O reads for the query plan within the aggregation interval (expressed as a number of 8KB pages read).");
			rstm.setColumnDescription("stdev_num_physical_io_reads"      , "Number of physical I/O reads standard deviation for the query plan within the aggregation interval (expressed as a number of 8KB pages read).");
			
			rstm.setColumnDescription("avg_log_bytes_used__sum"          , "Summary of: Average number of bytes in the database log used by the query plan, wirecording period interval (usually 24 hours).");
			rstm.setColumnDescription("avg_log_bytes_used__chart"        , "Chart of:   Average number of bytes in the database log used by the query plan, within the aggregation interval.");
			rstm.setColumnDescription("avg_log_bytes_used"               , "Average number of bytes in the database log used by the query plan, within the aggregation interval.");
			rstm.setColumnDescription("last_log_bytes_used"              , "Number of bytes in the database log used by the last execution of the query plan, within the aggregation interval.");
			rstm.setColumnDescription("min_log_bytes_used"               , "Minimum number of bytes in the database log used by the query plan, within the aggregation interval.");
			rstm.setColumnDescription("max_log_bytes_used"               , "Maximum number of bytes in the database log used by the query plan, within the aggregation interval.");
			rstm.setColumnDescription("stdev_log_bytes_used"             , "Standard deviation of the number of bytes in the database log used by a query plan, within the aggregation interval.");
			
			rstm.setColumnDescription("avg_tempdb_space_used__sum"       , "Summary of: Average number of pages used in tempdb for the query plan within the recording period (usually 24 hours) (expressed as a number of 8KB pages).");
			rstm.setColumnDescription("avg_tempdb_space_used__chart"     , "Chart of:   Average number of pages used in tempdb for the query plan within the aggregation interval (expressed as a number of 8KB pages).");
			rstm.setColumnDescription("avg_tempdb_space_used"            , "Average number of pages used in tempdb for the query plan within the aggregation interval (expressed as a number of 8KB pages).");
			rstm.setColumnDescription("last_tempdb_space_used"           , "Last number of pages used in tempdb for the query plan within the aggregation interval (expressed as a number of 8KB pages).");
			rstm.setColumnDescription("min_tempdb_space_used"            , "Minimum number of pages used in tempdb for the query plan within the aggregation interval (expressed as a number of 8KB pages).");
			rstm.setColumnDescription("max_tempdb_space_used"            , "Maximum number of pages used in tempdb for the query plan within the aggregation interval (expressed as a number of 8KB pages).");
			rstm.setColumnDescription("stdev_tempdb_space_used"          , "Number of pages used in tempdb standard deviation for the query plan within the aggregation interval (expressed as a number of 8KB pages).");
			
			rstm.setColumnDescription("avg_page_server_io_reads__sum"    , "Summary of: Average number of page server I/O reads for the query plan within the recording period (usually 24 hours) (expressed as a number of 8KB pages read).");
			rstm.setColumnDescription("avg_page_server_io_reads__chart"  , "Chart of:   Average number of page server I/O reads for the query plan within the aggregation interval (expressed as a number of 8KB pages read).");
			rstm.setColumnDescription("avg_page_server_io_reads"         , "Average number of page server I/O reads for the query plan within the aggregation interval (expressed as a number of 8KB pages read).");
			rstm.setColumnDescription("last_page_server_io_reads"        , "Last number of page server I/O reads for the query plan within the aggregation interval (expressed as a number of 8KB pages read).");
			rstm.setColumnDescription("min_page_server_io_reads"         , "Minimum number of page server I/O reads for the query plan within the aggregation interval (expressed as a number of 8KB pages read).");
			rstm.setColumnDescription("max_page_server_io_reads"         , "Maximum number of page server I/O reads for the query plan within the aggregation interval (expressed as a number of 8KB pages read).");
			rstm.setColumnDescription("stdev_page_server_io_reads"       , "Number of page server I/O reads standard deviation for the query plan within the aggregation interval (expressed as a number of 8KB pages read).");
			

			rstm.setColumnDescription("total_duration_ms__sum"           , "Total duration for the query plan (reported in milliseconds).");
			rstm.setColumnDescription("total_duration__sum"              , "Total duration for the query plan (reported in microseconds).");
			rstm.setColumnDescription("total_cpu_time_ms__sum"           , "Total CPU time for the query plan (reported in milliseconds).");
			rstm.setColumnDescription("total_cpu_time__sum"              , "Total CPU time for the query plan (reported in microseconds).");
			rstm.setColumnDescription("total_logical_io_reads__sum"      , "Total number of logical I/O reads for the query plan. (expressed as a number of 8KB pages read).");
			rstm.setColumnDescription("total_logical_io_writes__sum"     , "Total number of logical I/O writes for the query plan.");
			rstm.setColumnDescription("total_physical_io_reads__sum"     , "Total number of physical I/O reads for the query plan (expressed as a number of 8KB pages read).");
			rstm.setColumnDescription("total_clr_time_ms__sum"           , "Total CLR time for the query plan (reported in milliseconds).");
			rstm.setColumnDescription("total_clr_time__sum"              , "Total CLR time for the query plan (reported in microseconds).");
			rstm.setColumnDescription("total_dop__sum"                   , "Total DOP (degree of parallelism) for the query plan.");
			rstm.setColumnDescription("total_query_max_used_memory__sum" , "Total memory grant (reported as the number of 8 KB pages) for the query plan. Always 0 for queries using natively compiled memory optimized procedures.");
			rstm.setColumnDescription("total_rowcount__sum"              , "Total number of returned rows for the query plan.");
			rstm.setColumnDescription("total_num_physical_io_reads__sum" , "Total number of physical I/O reads for the query plan (expressed as a number of 8KB pages read).");
			rstm.setColumnDescription("total_log_bytes_used__sum"        , "Total number of bytes in the database log used by the query plan.");
			rstm.setColumnDescription("total_tempdb_space_used__sum"     , "Total number of pages used in tempdb for the query plan (expressed as a number of 8KB pages).");
			rstm.setColumnDescription("total_page_server_io_reads__sum"  , "Total number of page server I/O reads for the query plan (expressed as a number of 8KB pages read).");
		
		}

		/**
		 * Set WAIT column descriptions
		 */
		protected void setWaitSectionDescription(ResultSetTableModel rstm)
		{
			if (rstm == null)
				return;
			
			// Section description
			rstm.setDescription("<b>&bull; Top WAIT</b> (ordered by: total_query_wait_time_ms)");

			// Columns description
			rstm.setColumnDescription("wait_stats_id"             , "Identifier of the row representing wait statistics for the plan_id, runtime_stats_interval_id, execution_type and wait_category. It is unique only for the past runtime statistics intervals. For the currently active interval, there may be multiple rows representing wait statistics for the plan referenced by plan_id, with the execution type represented by execution_type and the wait category represented by wait_category. Typically, one row represents wait statistics that are flushed to disk, while other(s) represent in-memory state. Hence, to get actual state for every interval you need to aggregate metrics, grouping by plan_id, runtime_stats_interval_id, execution_type and wait_category.");
			rstm.setColumnDescription("plan_id"                   , "Foreign key. Joins to sys.query_store_plan (Transact-SQL).");
			rstm.setColumnDescription("runtime_stats_interval_id" , "Foreign key. Joins to sys.query_store_runtime_stats_interval (Transact-SQL).");
			rstm.setColumnDescription("wait_category"             , "Wait types are categorized using the table below, and then wait time is aggregated across these wait categories. Different wait categories require a different follow-up analysis to resolve the issue, but wait types from the same category lead to similar troubleshooting experiences, and providing the affected query in addition to the waits is the missing piece to complete the majority of such investigations successfully.");
			rstm.setColumnDescription("wait_category_desc"        , "For textual description of the wait category field, review the table below.");
			rstm.setColumnDescription("execution_type"            , "Determines type of query execution: 0 - Regular execution (successfully finished); 3 - Client initiated aborted execution; 4 - Exception aborted execution;");
			rstm.setColumnDescription("execution_type_desc"       , "Textual description of the execution type field: 0 - Regular; 3 - Aborted; 4 - Exception;");
			rstm.setColumnDescription("total_query_wait_time_ms"  , "Total CPU wait time for the query plan within the aggregation interval and wait category (reported in milliseconds).");
			rstm.setColumnDescription("avg_query_wait_time_ms"    , "Average wait duration for the query plan per execution within the aggregation interval and wait category (reported in milliseconds).");
			rstm.setColumnDescription("last_query_wait_time_ms"   , "Last wait duration for the query plan within the aggregation interval and wait category (reported in milliseconds).");
			rstm.setColumnDescription("min_query_wait_time_ms"    , "Minimum CPU wait time for the query plan within the aggregation interval and wait category (reported in milliseconds).");
			rstm.setColumnDescription("max_query_wait_time_ms"    , "Maximum CPU wait time for the query plan within the aggregation interval and wait category (reported in milliseconds).");
			rstm.setColumnDescription("stdev_query_wait_time_ms"  , "Query wait duration standard deviation for the query plan within the aggregation interval and wait category (reported in milliseconds).");
		}

		protected String getQsSqlTextAsString(Map<Map<String, Object>, QsSqlTextEntry> map, Map<String, Object> whereColValMap, ResultSetTableModel rstm, int row)
		{
			if (map == null)
				return "";

			StringBuilder sb = new StringBuilder();
			NumberFormat nf = NumberFormat.getInstance();

			QsSqlTextEntry entry = map.get(whereColValMap);
			
			if (entry == null)
			{
				return "";
//				return "No entry for '"+ whereColValMap +"' was found. known keys: " + map.keySet();
			}
			
			if (StringUtil.hasValue(entry.sqlText))
			{
				if (rstm.getName().endsWith("_topCpu"))
				{
					sb.append("-- Some columns extracted from current row.\n");
					sb.append("-------------------------------------------------------------------------------------------------------------------------------\n");
					sb.append("-- dbname:                           ").append( rstm.getValueAsString(row, "dbname"                ) ).append("\n");
					sb.append("-- plan_id:                          ").append( rstm.getValueAsString(row, "plan_id"               ) ).append("\n");
					sb.append("-- first_execution_time:             ").append( rstm.getValueAsString(row, "first_execution_time"  ) ).append("\n");
					sb.append("-- last_execution_time:              ").append( rstm.getValueAsString(row, "last_execution_time"   ) ).append("\n");

					sb.append("-- count_executions__sum:            ").append(                 nf.format(rstm.getValueAsBigDecimal(row, "count_executions__sum"           ))).append("\n");

					sb.append("-- total_duration_ms__sum:           ").append( StringUtil.left(nf.format(rstm.getValueAsBigDecimal(row, "total_duration_ms__sum"          )),18)).append("  (in milli seconds), and in (HH:MM:SS.sss) ").append(TimeUtils.msToTimeStrLong(rstm.getValueAsLong(row, "total_duration_ms__sum"))).append(" \n");
					sb.append(">> avg_duration_sm__sum:             ").append( StringUtil.left(nf.format(rstm.getValueAsBigDecimal(row, "avg_duration_ms__sum"            )),18)).append("  (in milli seconds), and in (HH:MM:SS.sss) ").append(TimeUtils.msToTimeStrLong(rstm.getValueAsLong(row, "avg_duration_ms__sum"))).append(" \n");

					sb.append("-- total_cpu_time_ms__sum:           ").append( StringUtil.left(nf.format(rstm.getValueAsBigDecimal(row, "total_cpu_time_ms__sum"          )),18)).append("  (in milli seconds), and in (HH:MM:SS.sss) ").append(TimeUtils.msToTimeStrLong(rstm.getValueAsLong(row, "total_cpu_time_ms__sum"))).append(" \n");
					sb.append(">> avg_cpu_time_ms__sum:             ").append( StringUtil.left(nf.format(rstm.getValueAsBigDecimal(row, "avg_cpu_time_ms__sum"            )),18)).append("  (in milli seconds), and in (HH:MM:SS.sss) ").append(TimeUtils.msToTimeStrLong(rstm.getValueAsLong(row, "avg_cpu_time_ms__sum"))).append(" \n");

//					sb.append("-- total_wait_time_ms__sum:          ").append( StringUtil.left(nf.format(rstm.getValueAsBigDecimal(row, "total_wait_time_ms__sum"         )),18)).append("  (in milli seconds), and in (HH:MM:SS.sss) ").append(TimeUtils.msToTimeStrLong(rstm.getValueAsLong(row, "total_wait_time_ms__sum"))).append(" \n");
//					sb.append(">> avg_wait_time_ms__sum:            ").append( StringUtil.left(nf.format(rstm.getValueAsBigDecimal(row, "avg_wait_time_ms__sum"           )),18)).append("  (in milli seconds), and in (HH:MM:SS.sss) ").append(TimeUtils.msToTimeStrLong(rstm.getValueAsLong(row, "avg_wait_time_ms__sum"))).append(" \n");

					sb.append("-- total_logical_io_reads__sum:      ").append(                 nf.format(rstm.getValueAsBigDecimal(row, "total_logical_io_reads__sum"     ))).append("\n");
					sb.append("-- avg_logical_io_reads__sum:        ").append(                 nf.format(rstm.getValueAsBigDecimal(row, "avg_logical_io_reads__sum"       ))).append("\n");

					sb.append("-- total_logical_io_writes__sum:     ").append(                 nf.format(rstm.getValueAsBigDecimal(row, "total_logical_io_writes__sum"    ))).append("\n");
					sb.append("-- avg_logical_io_writes__sum:       ").append(                 nf.format(rstm.getValueAsBigDecimal(row, "avg_logical_io_writes__sum"      ))).append("\n");

					sb.append("-- total_physical_io_reads__sum:     ").append(                 nf.format(rstm.getValueAsBigDecimal(row, "total_physical_io_reads__sum"    ))).append("\n");
					sb.append("-- avg_physical_io_reads__sum:       ").append(                 nf.format(rstm.getValueAsBigDecimal(row, "avg_physical_io_reads__sum"      ))).append("\n");

					sb.append("-- total_clr_time_ms__sum:           ").append( StringUtil.left(nf.format(rstm.getValueAsBigDecimal(row, "total_clr_time_ms__sum"          )),18)).append("  (in milli seconds), and in (HH:MM:SS.sss) ").append(TimeUtils.msToTimeStrLong(rstm.getValueAsLong(row, "total_clr_time_ms__sum"))).append(" \n");
					sb.append("-- avg_clr_time_ms__sum:             ").append( StringUtil.left(nf.format(rstm.getValueAsBigDecimal(row, "avg_clr_time_ms__sum"            )),18)).append("  (in milli seconds), and in (HH:MM:SS.sss) ").append(TimeUtils.msToTimeStrLong(rstm.getValueAsLong(row, "avg_clr_time_ms__sum"))).append(" \n");

//					sb.append("-- total_dop__sum:                   ").append(                 nf.format(rstm.getValueAsBigDecimal(row, "total_dop__sum"                  ))).append("\n");
//					sb.append("-- avg_dop__sum:                     ").append(                 nf.format(rstm.getValueAsBigDecimal(row, "avg_dop__sum"                    ))).append("\n");
					sb.append("-- avg_dop__avg:                     ").append(                 nf.format(rstm.getValueAsBigDecimal(row, "avg_dop__avg"                    ))).append("\n");

					sb.append("-- total_query_max_used_memory__sum: ").append(                 nf.format(rstm.getValueAsBigDecimal(row, "total_query_max_used_memory__sum"))).append("\n");
					sb.append("-- avg_query_max_used_memory__sum:   ").append(                 nf.format(rstm.getValueAsBigDecimal(row, "avg_query_max_used_memory__sum"  ))).append("\n");

					sb.append("-- total_rowcount__sum:              ").append(                 nf.format(rstm.getValueAsBigDecimal(row, "total_rowcount__sum"             ))).append("\n");
					sb.append("-- avg_rowcount__sum:                ").append(                 nf.format(rstm.getValueAsBigDecimal(row, "avg_rowcount__sum"               ))).append("\n");

					if (rstm.hasColumn("total_num_physical_io_reads__sum"))                    
					{                                                                          
					sb.append("-- total_num_physical_io_reads__sum: ").append(                 nf.format(rstm.getValueAsBigDecimal(row, "total_num_physical_io_reads__sum"))).append("\n");
					sb.append("-- avg_num_physical_io_reads__sum:   ").append(                 nf.format(rstm.getValueAsBigDecimal(row, "avg_num_physical_io_reads__sum"  ))).append("\n");
					}                                                                          

					if (rstm.hasColumn("total_log_bytes_used__sum"))                           
					{                                                                          
					sb.append("-- total_log_bytes_used__sum:        ").append(                 nf.format(rstm.getValueAsBigDecimal(row, "total_log_bytes_used__sum"       ))).append("\n");
					sb.append("-- total_log_bytes_used_mb__sum:     ").append(                 nf.format(rstm.getValueAsBigDecimal(row, "total_log_bytes_used_mb__sum"    ))).append("\n");
					sb.append("-- avg_log_bytes_used__sum:          ").append(                 nf.format(rstm.getValueAsBigDecimal(row, "avg_log_bytes_used__sum"         ))).append("\n");
					}                                                                          

					if (rstm.hasColumn("total_tempdb_space_used__sum"))                        
					{                                                                          
					sb.append("-- total_tempdb_space_used__sum:     ").append(                 nf.format(rstm.getValueAsBigDecimal(row, "total_tempdb_space_used__sum"    ))).append("\n");
					sb.append("-- total_tempdb_space_used_mb__sum:  ").append(                 nf.format(rstm.getValueAsBigDecimal(row, "total_tempdb_space_used_mb__sum" ))).append("\n");
					sb.append("-- avg_tempdb_space_used__sum:       ").append(                 nf.format(rstm.getValueAsBigDecimal(row, "avg_tempdb_space_used__sum"      ))).append("\n");
					}
					sb.append("-------------------------------------------------------------------------------------------------------------------------------\n");
					sb.append("\n");
				}

				sb.append("-------------------------------------------------------------------------------------------------------------------------------\n");
				sb.append("-- SQL Statement that was executed. \n");
				if (StringUtil.hasValue(entry.objectName))
				{
					sb.append("-- Schema Name: ").append( entry.schemaName ).append("\n");
					sb.append("-- Object Name: ").append( entry.objectName ).append("\n");
				}
				sb.append("-------------------------------------------------------------------------------------------------------------------------------\n");
				sb.append(StringEscapeUtils.escapeHtml4(entry.sqlText));
			}
			else
			{
				if (StringUtil.hasValue(entry.sqlTextProblem))
				{
					sb.append("\n");
					sb.append("-------------------------------------------------------------------------------------------------------------------------------\n");
					sb.append("-- Problems when trying to get SQL Statement. \n");
					sb.append("-------------------------------------------------------------------------------------------------------------------------------\n");
					sb.append(StringEscapeUtils.escapeHtml4(entry.sqlTextProblem));
				}
				else
				{
					return null;
				}
			}

			return sb.toString();
		}

		protected String getQsWaitTimeAsString(Map<Map<String, Object>, QsWaitEntry> map, Map<String, Object> whereColValMap, ResultSetTableModel rstm, int row)
		{
			if (map == null)
				return "";

			QsWaitEntry entry = map.get(whereColValMap);
			
			if (entry == null)
			{
				return "";
//				return "No entry for '"+ whereColValMap +"' was found. known keys: " + map.keySet();
			}

			if (entry.waitTimeSum == null && entry.waitTimeDetails == null && entry.rootCauseInfo == null)
			{
				return "Entry for '"+ whereColValMap +"' was found. But NO INFORMATION has been assigned to it.";
			}
			
			StringBuilder sb = new StringBuilder();
			NumberFormat nf = NumberFormat.getInstance();

			// Add some initial text
			if (entry.waitTimeSum != null)
			{
				sb.append("-------------------------------------------------------------------------------------------------------------------------------\n");
				sb.append("-- Wait Information... or 'wait_category' that this statement has been waited on. \n");
				sb.append("-- The below section is a SUMMARY for the whole period (normally 24 hours) \n");
				sb.append("-- In next section holds more detailed information... \n");
				sb.append("-------------------------------------------------------------------------------------------------------------------------------\n");
				sb.append("\n");

				if (rstm.getName().endsWith("_topWait"))
				{
					sb.append("-- Some columns extracted from current row.\n");
					sb.append("-------------------------------------------------------------------------------------------------------------------------------\n");
					sb.append("-- dbname:                        ").append(                           rstm.getValueAsString    (row, "dbname"                           ) ).append("\n");
					sb.append("-- plan_id:                       ").append(                           rstm.getValueAsString    (row, "plan_id"                          ) ).append("\n");
					sb.append("-- total_query_wait_time_ms__sum: ").append( StringUtil.left(nf.format(rstm.getValueAsBigDecimal(row, "total_query_wait_time_ms__sum")),18)).append("  (in milli seconds), and in (HH:MM:SS.sss) ").append(TimeUtils.msToTimeStrLong(rstm.getValueAsLong(row, "total_query_wait_time_ms__sum"))).append(" \n");
					sb.append("-- avg_query_wait_time_ms__sum:   ").append( StringUtil.left(nf.format(rstm.getValueAsBigDecimal(row, "avg_query_wait_time_ms__sum"  )),18)).append("  (in milli seconds), and in (HH:MM:SS.sss) ").append(TimeUtils.msToTimeStrLong(rstm.getValueAsLong(row, "avg_query_wait_time_ms__sum"  ))).append(" \n");
					sb.append("-- min_query_wait_time_ms:        ").append( StringUtil.left(nf.format(rstm.getValueAsBigDecimal(row, "min_query_wait_time_ms"       )),18)).append("  (in milli seconds), and in (HH:MM:SS.sss) ").append(TimeUtils.msToTimeStrLong(rstm.getValueAsLong(row, "min_query_wait_time_ms"       ))).append(" \n");
					sb.append("-- max_query_wait_time_ms:        ").append( StringUtil.left(nf.format(rstm.getValueAsBigDecimal(row, "max_query_wait_time_ms"       )),18)).append("  (in milli seconds), and in (HH:MM:SS.sss) ").append(TimeUtils.msToTimeStrLong(rstm.getValueAsLong(row, "max_query_wait_time_ms"       ))).append(" \n");
					sb.append("-------------------------------------------------------------------------------------------------------------------------------\n");
					sb.append("\n");
				}
				
				// Add WAIT Text
				sb.append( StringEscapeUtils.escapeHtml4(entry.waitTimeSum.toAsciiTableString()) ).append("\n");
				sb.append("\n");
				sb.append("\n");
			}
			else
			{
				sb.append("-------------------------------------------------------------------------------------------------------------------------------\n");
				sb.append("-- Problems getting Wait Sum Information:\n");
				sb.append("-- ").append( StringEscapeUtils.escapeHtml4(entry.waitTimeSumProblem) ).append("\n");
				sb.append("-------------------------------------------------------------------------------------------------------------------------------\n");
			}
			
			// Add some initial text
			if (entry.waitTimeDetails != null)
			{
				sb.append("-------------------------------------------------------------------------------------------------------------------------------\n");
				sb.append("-- Detailed Wait Information... or 'wait_category' that this statement has been waited on. \n");
				sb.append("-- This section holds 'wait_category' for each 'runtime_stats_interval'. (which is 1 hour by default) \n");
				sb.append("-------------------------------------------------------------------------------------------------------------------------------\n");
				sb.append("\n");

				// Add WAIT Text (on every "new section" for 'runtime_stats_interval_id' add a "separator line +----+-------+----+" 
				sb.append( StringEscapeUtils.escapeHtml4(entry.waitTimeDetails.toAsciiTableString("runtime_stats_interval_id")) ).append("\n");
				sb.append("\n");
				sb.append("\n");
			}
			else
			{
				sb.append("-------------------------------------------------------------------------------------------------------------------------------\n");
				sb.append("-- Problems getting Wait Details Information:\n");
				sb.append("-- ").append( StringEscapeUtils.escapeHtml4(entry.waitTimeSumProblem) ).append("\n");
				sb.append("-------------------------------------------------------------------------------------------------------------------------------\n");
			}
			
//			// Add some text about: Root Cause SQL Text
//			if (entry.rootCauseInfo != null)
//			{
//				sb.append("-------------------------------------------------------------------------------------------------------------------------------\n");
//				sb.append("-- Root Cause for Blocking Locks:\n");
//				sb.append("-- Below we try to investigate any 'root cause' for blocking locks that was found in above section.\n");
//				sb.append("--\n");
//				sb.append("-- For SQL-Server, I'm not sure we can do this... but keep this code for future usage... or see: AseTopSlowNormalizedSql for how it was done there...\n");
//				sb.append("-------------------------------------------------------------------------------------------------------------------------------\n");
//				sb.append("\n");
//			}
			
			return sb.toString();
		}

		protected Map<Map<String, Object>, QsSqlTextEntry> getQsSqlText(Map<Map<String, Object>, QsSqlTextEntry> map, DbxConnection conn, Map<String, Object> whereColValMap)
		{
			if (map == null)
				map = new HashMap<>();

			QsSqlTextEntry textEntry = map.get(whereColValMap);
			if (textEntry == null)
			{
				textEntry = new QsSqlTextEntry();
				map.put(whereColValMap, textEntry);
			}

			String whereColValStr = "";
			for (Entry<String, Object> entry : whereColValMap.entrySet())
			{
				// Skip entries that starts with "!"
				if (entry.getKey().startsWith("!"))
					continue;

				whereColValStr += "  and [" + entry.getKey() + "] = " + DbUtils.safeStr( entry.getValue() ) + "\n";
			}

			String sql = ""
				    + "select qt.[query_sql_text] \n"
				    + "      ,q.[schema_name] \n"
				    + "      ,q.[object_name] \n"
				    + "from [" + _schemaName + "].[query_store_query_text] qt \n"
				    + "inner join [" + _schemaName + "].[query_store_query] q ON qt.[query_text_id] = q.[query_text_id] \n"
				    + "inner join [" + _schemaName + "].[query_store_plan]  p ON q.[query_id]       = p.[query_id] \n"
				    + "where 1 = 1 \n"
				    + whereColValStr
//				    + "where p.[plan_id] = 28 \n"
				    + "";
			
			sql = conn.quotifySqlString(sql);

			try ( Statement stmnt = conn.createStatement() )
			{
				// Unlimited execution time
				stmnt.setQueryTimeout(0);
				try ( ResultSet rs = stmnt.executeQuery(sql) )
				{
					while(rs.next())
					{
						textEntry.sqlText    = rs.getString(1);
						textEntry.schemaName = rs.getString(2);
						textEntry.objectName = rs.getString(3);
					}
				}
			}
			catch(SQLException ex)
			{
				textEntry.sqlTextProblem = "Problems getting 'SQL Text' for: " + whereColValMap + ": " + ex;
				_logger.warn("Problems getting SQL Text by: " + whereColValMap + ": " + ex);
			}

			return map;
		}

		protected Map<Map<String, Object>, QsWaitEntry> getQsWaitTime(Map<Map<String, Object>, QsWaitEntry> map, DbxConnection conn, Map<String, Object> whereColValMap)
		{
			if (map == null)
				map = new HashMap<>();

			QsWaitEntry waitEntry = map.get(whereColValMap);
			if (waitEntry == null)
			{
				waitEntry = new QsWaitEntry();
				map.put(whereColValMap, waitEntry);
			}

			String whereColValStr = "";
			for (Entry<String, Object> entry : whereColValMap.entrySet())
			{
				// Skip entries that starts with "!"
				if (entry.getKey().startsWith("!"))
					continue;

				whereColValStr += "      and [" + entry.getKey() + "] = " + DbUtils.safeStr( entry.getValue() ) + "\n";
			}

			boolean getWaitSumInfo     = true;
			boolean getWaitDetailsInfo = true;
			boolean getRootCauseInfo   = true;

			if (getWaitSumInfo)
			{
				String sql = ""
					    + "-- group by: wait_category_desc over for the whole period \n"
					    + "select \n"
					    + "     w.[plan_id] \n"
					    + "    ,w.[wait_category_desc] \n"
					    + "    ,count(*)                              as [rowcount] \n"
					    + "    ,min(i.[start_time])                   as [start_time] \n"
					    + "    ,max(i.[end_time])                     as [end_time] \n"
//					    + "    ,cast(min(i.[start_time]) as datetime) as [start_time_l] \n"
//					    + "    ,cast(max(i.[end_time])   as datetime) as [end_time_l] \n"
					    + "    ,cast('' as varchar(30))               as [duration] \n"
					    + "    ,sum(w.[total_query_wait_time_ms])     as [total_query_wait_time_ms__sum] \n"
					    + "    ,''                                    as [total_HMS] \n"
					    + "    ,avg(w.[avg_query_wait_time_ms])       as [avg_query_wait_time_ms__avg] \n"
					    + " \n"
					    + "    ,max(w.[last_query_wait_time_ms])      as [last_query_wait_time_ms__max] \n"
					    + "    ,min(w.[min_query_wait_time_ms])       as [min_query_wait_time_ms__min] \n"
					    + "    ,max(w.[max_query_wait_time_ms])       as [max_query_wait_time_ms__max] \n"
					    + "    ,avg(w.[stdev_query_wait_time_ms])     as [stdev_query_wait_time_ms__avg] \n"
					    + "from [" + _schemaName + "].[query_store_wait_stats] w \n"
					    + "inner join [" + _schemaName + "].[query_store_runtime_stats_interval] i ON w.[runtime_stats_interval_id] = i.[runtime_stats_interval_id] \n"
					    + "where 1 = 1 \n"
					    + whereColValStr
					    + "group by w.[wait_category_desc] \n"
					    + "order by [total_query_wait_time_ms__sum] desc \n"
					    + "";

				sql = conn.quotifySqlString(sql);

				try ( Statement stmnt = conn.createStatement() )
				{
					// Unlimited execution time
					stmnt.setQueryTimeout(0);
					try ( ResultSet rs = stmnt.executeQuery(sql) )
					{
						ResultSetTableModel rstm = new ResultSetTableModel(rs, "Wait Detailes for: " + whereColValMap.entrySet());
						
						rstm.setToStringTimestampFormat("yyyy-MM-dd HH:mm");

						setDurationColumn(rstm, "start_time", "end_time", "duration");
						setMsToHms       (rstm, "total_query_wait_time_ms__sum", "total_HMS");

						waitEntry.waitTimeSum = rstm;
					}
				}
				catch(SQLException ex)
				{
					waitEntry.waitTimeSumProblem = "Problems getting 'Wait Information' for: " + whereColValMap + ": " + ex;
					_logger.warn("Problems getting SQL Text by: " + whereColValMap + ": " + ex);
				}
			}

			if (getWaitDetailsInfo)
			{
				String sql = ""
					    + "-- Details for each individual period \n"
					    + "select \n"
					    + "     w.[plan_id] \n"
					    + "    ,w.[wait_category_desc] \n"
					    + "    ,w.[runtime_stats_interval_id] \n"
					    + "    ,i.[start_time] \n"
//					    + "    ,cast(i.[start_time] as datetime) as [start_time_dt] \n"
					    + "    ,w.[total_query_wait_time_ms] \n"
					    + "    ,w.[avg_query_wait_time_ms] \n"
					    + "    ,w.[last_query_wait_time_ms] \n"
					    + "    ,w.[min_query_wait_time_ms] \n"
					    + "    ,w.[max_query_wait_time_ms] \n"
					    + "    ,w.[stdev_query_wait_time_ms] \n"
					    + "from [" + _schemaName + "].[query_store_wait_stats] w \n"
					    + "inner join [" + _schemaName + "].[query_store_runtime_stats_interval] i ON w.[runtime_stats_interval_id] = i.[runtime_stats_interval_id] \n"
					    + "where 1 = 1 \n"
					    + whereColValStr
					    + "order by w.[runtime_stats_interval_id], w.[total_query_wait_time_ms] desc \n"
					    + "";

				sql = conn.quotifySqlString(sql);

				try ( Statement stmnt = conn.createStatement() )
				{
					// Unlimited execution time
					stmnt.setQueryTimeout(0);
					try ( ResultSet rs = stmnt.executeQuery(sql) )
					{
						ResultSetTableModel rstm = new ResultSetTableModel(rs, "Wait Detailes for: " + whereColValMap.entrySet());

						rstm.setToStringTimestampFormat("yyyy-MM-dd HH:mm");

//						setDurationColumn(rstm, "sampleTime_min", "sampleTime_max", "Duration");
//						setMsToHms       (rstm, "WaitTime_sum", "WaitTime_HMS");

						waitEntry.waitTimeDetails = rstm;
					}
				}
				catch(SQLException ex)
				{
					waitEntry.waitTimeDetailsProblem = "Problems getting 'Wait Information' for: " + whereColValMap + ": " + ex;
					_logger.warn("Problems getting SQL Text by: " + whereColValMap + ": " + ex);
				}
			}

			// Root Cause SQL Text
			if (getRootCauseInfo)
			{
//				String sql = "FIMXE";
//
//				try
//				{
//					ResultSetTableModel rootCauseInfo = executeQuery(conn, sql, "rootCauseInfo for:" + whereColValMap.entrySet());
//					waitEntry.rootCauseInfo = rootCauseInfo;
//					
//					if (rootCauseInfo.getRowCount() == 0)
//						waitEntry.rootCauseInfo = null;
//				}
//				catch (SQLException ex)
//				{
//					waitEntry.rootCauseInfoProblem = "Problems getting Root Cause for: " + whereColValMap + ": " + ex;
//					_logger.warn("Problems getting Root Cause for: " + whereColValMap + ": " + ex);
//				}
			}

			return map;
		}

	} // end: class: QsDbReport

	//----------------------------------------------------------------------------------------------
	//-- helper classes to store SQL-Text and WAIT-Info
	//----------------------------------------------------------------------------------------------
	public static class QsSqlTextEntry
	{
		String sqlText;
		String schemaName;
		String objectName;
		String sqlTextProblem;
	}

	public static class QsWaitEntry
	{
		ResultSetTableModel waitTimeSum;
		String              waitTimeSumProblem;
		ResultSetTableModel waitTimeDetails;
		String              waitTimeDetailsProblem;
		ResultSetTableModel rootCauseInfo;
		String              rootCauseInfoProblem;
	}
}


/*-------------------------------------------------------------------------------------------------------
 * Below is a bit of version information: 2016 -> 2017 -> 2019
 * I didn't check in what CU columns was introduced at...  but hopefully only at Major Version Change
 *------------------------------------------------------------------------------------------------------- 

---------------------------------------------------------|-----------------------------
--tabname                                                |colcount
---------------------------------------------------------|-----------------------------
                                                         |2016|2017|2019| Short Description
                                                         |----|----|----|--------------
select * from sys.database_query_store_options       --  |  16|  18|  22| >>2017[wait_stats_capture_mode, wait_stats_capture_mode_desc] >>2019[capture_policy_execution_count, capture_policy_total_compile_cpu_time_ms, capture_policy_total_execution_cpu_time_ms, capture_policy_stale_threshold_hours]
select * from sys.query_context_settings             --  |  12|  12|  12| ==
select * from sys.query_store_query_text             --  |   5|   5|   5| ==
select * from sys.query_store_query                  --  |  30|  30|  30| ==
select * from sys.query_store_plan                   --  |  21|  23|  23| >>2017[plan_forcing_type, plan_forcing_type_desc]
select * from sys.query_store_runtime_stats          --  |  53|  68|  68| >>2017(*num_physical_io_reads, *log_bytes_used, *tempdb_space_used) [avg_num_physical_io_reads, last_num_physical_io_reads, min_num_physical_io_reads, max_num_physical_io_reads, stdev_num_physical_io_reads,    avg_log_bytes_used, last_log_bytes_used, min_log_bytes_used, max_log_bytes_used, stdev_log_bytes_used,    avg_tempdb_space_used, last_tempdb_space_used, min_tempdb_space_used, max_tempdb_space_used, stdev_tempdb_space_used] 
select * from sys.query_store_wait_stats             --  |   -|  13|  13| ==
select * from sys.query_store_runtime_stats_interval --  |   4|   4|   4| == 
---------------------------------------------------------|-----------------------------

----------------
2019 - database_query_store_options ( 2 new cols [wait_stats_capture_mode*] in 2017, 4 new cols [capture_policy_*] in 2019)
----------------
RS> Col# Label                                      JDBC Type Name          Guessed DBMS type Source Table
RS> ---- ------------------------------------------ ----------------------- ----------------- ------------
RS> 1    desired_state                              java.sql.Types.SMALLINT smallint          -none-      
RS> 2    desired_state_desc                         java.sql.Types.NVARCHAR nvarchar(60)      -none-      
RS> 3    actual_state                               java.sql.Types.SMALLINT smallint          -none-      
RS> 4    actual_state_desc                          java.sql.Types.NVARCHAR nvarchar(60)      -none-      
RS> 5    readonly_reason                            java.sql.Types.INTEGER  int               -none-      
RS> 6    current_storage_size_mb                    java.sql.Types.BIGINT   bigint            -none-      
RS> 7    flush_interval_seconds                     java.sql.Types.BIGINT   bigint            -none-      
RS> 8    interval_length_minutes                    java.sql.Types.BIGINT   bigint            -none-      
RS> 9    max_storage_size_mb                        java.sql.Types.BIGINT   bigint            -none-      
RS> 10   stale_query_threshold_days                 java.sql.Types.BIGINT   bigint            -none-      
RS> 11   max_plans_per_query                        java.sql.Types.BIGINT   bigint            -none-      
RS> 12   query_capture_mode                         java.sql.Types.SMALLINT smallint          -none-      
RS> 13   query_capture_mode_desc                    java.sql.Types.NVARCHAR nvarchar(60)      -none-      
RS> 14   capture_policy_execution_count             java.sql.Types.INTEGER  int               -none-      NEW IN 2019
RS> 15   capture_policy_total_compile_cpu_time_ms   java.sql.Types.BIGINT   bigint            -none-      NEW IN 2019
RS> 16   capture_policy_total_execution_cpu_time_ms java.sql.Types.BIGINT   bigint            -none-      NEW IN 2019
RS> 17   capture_policy_stale_threshold_hours       java.sql.Types.INTEGER  int               -none-      NEW IN 2019
RS> 18   size_based_cleanup_mode                    java.sql.Types.SMALLINT smallint          -none-      
RS> 19   size_based_cleanup_mode_desc               java.sql.Types.NVARCHAR nvarchar(60)      -none-      
RS> 20   wait_stats_capture_mode                    java.sql.Types.SMALLINT smallint          -none-      NEW IN 2017
RS> 21   wait_stats_capture_mode_desc               java.sql.Types.NVARCHAR nvarchar(60)      -none-      NEW IN 2017
RS> 22   actual_state_additional_info               java.sql.Types.NVARCHAR nvarchar(4000)    -none-      

----------------
2019 - query_context_settings (no changes: 2016 -> 2017 -> 2019)
----------------
RS> Col# Label                     JDBC Type Name           Guessed DBMS type Source Table
RS> ---- ------------------------- ------------------------ ----------------- ------------
RS> 1    context_settings_id       java.sql.Types.BIGINT    bigint            -none-      
RS> 2    set_options               java.sql.Types.VARBINARY varbinary(16)     -none-      
RS> 3    language_id               java.sql.Types.SMALLINT  smallint          -none-      
RS> 4    date_format               java.sql.Types.SMALLINT  smallint          -none-      
RS> 5    date_first                java.sql.Types.TINYINT   tinyint           -none-      
RS> 6    status                    java.sql.Types.VARBINARY varbinary(4)      -none-      
RS> 7    required_cursor_options   java.sql.Types.INTEGER   int               -none-      
RS> 8    acceptable_cursor_options java.sql.Types.INTEGER   int               -none-      
RS> 9    merge_action_type         java.sql.Types.SMALLINT  smallint          -none-      
RS> 10   default_schema_id         java.sql.Types.INTEGER   int               -none-      
RS> 11   is_replication_specific   java.sql.Types.BIT       bit               -none-      
RS> 12   is_contained              java.sql.Types.VARBINARY varbinary(2)      -none-      

----------------
2019 - query_store_query_text (no changes: 2016 -> 2017 -> 2019)
----------------
RS> Col# Label                       JDBC Type Name           Guessed DBMS type Source Table
RS> ---- --------------------------- ------------------------ ----------------- ------------
RS> 1    query_text_id               java.sql.Types.BIGINT    bigint            -none-      
RS> 2    query_sql_text              java.sql.Types.NVARCHAR  nvarchar(max)     -none-      
RS> 3    statement_sql_handle        java.sql.Types.VARBINARY varbinary(88)     -none-      
RS> 4    is_part_of_encrypted_module java.sql.Types.BIT       bit               -none-      
RS> 5    has_restricted_text         java.sql.Types.BIT       bit               -none-      

----------------
2019 - query_store_query (no changes: 2016 -> 2017 -> 2019)
----------------
RS> Col# Label                            JDBC Type Name               Guessed DBMS type Source Table
RS> ---- -------------------------------- ---------------------------- ----------------- ------------
RS> 1    query_id                         java.sql.Types.BIGINT        bigint            -none-      
RS> 2    query_text_id                    java.sql.Types.BIGINT        bigint            -none-      
RS> 3    context_settings_id              java.sql.Types.BIGINT        bigint            -none-      
RS> 4    object_id                        java.sql.Types.BIGINT        bigint            -none-      
RS> 5    batch_sql_handle                 java.sql.Types.VARBINARY     varbinary(88)     -none-      
RS> 6    query_hash                       java.sql.Types.BINARY        binary(16)        -none-      
RS> 7    is_internal_query                java.sql.Types.BIT           bit               -none-      
RS> 8    query_parameterization_type      java.sql.Types.TINYINT       tinyint           -none-      
RS> 9    query_parameterization_type_desc java.sql.Types.NVARCHAR      nvarchar(60)      -none-      
RS> 10   initial_compile_start_time       microsoft.sql.DATETIMEOFFSET datetimeoffset    -none-      
RS> 11   last_compile_start_time          microsoft.sql.DATETIMEOFFSET datetimeoffset    -none-      
RS> 12   last_execution_time              microsoft.sql.DATETIMEOFFSET datetimeoffset    -none-      
RS> 13   last_compile_batch_sql_handle    java.sql.Types.VARBINARY     varbinary(88)     -none-      
RS> 14   last_compile_batch_offset_start  java.sql.Types.BIGINT        bigint            -none-      
RS> 15   last_compile_batch_offset_end    java.sql.Types.BIGINT        bigint            -none-      
RS> 16   count_compiles                   java.sql.Types.BIGINT        bigint            -none-      
RS> 17   avg_compile_duration             java.sql.Types.DOUBLE        float             -none-      
RS> 18   last_compile_duration            java.sql.Types.BIGINT        bigint            -none-      
RS> 19   avg_bind_duration                java.sql.Types.DOUBLE        float             -none-      
RS> 20   last_bind_duration               java.sql.Types.BIGINT        bigint            -none-      
RS> 21   avg_bind_cpu_time                java.sql.Types.DOUBLE        float             -none-      
RS> 22   last_bind_cpu_time               java.sql.Types.BIGINT        bigint            -none-      
RS> 23   avg_optimize_duration            java.sql.Types.DOUBLE        float             -none-      
RS> 24   last_optimize_duration           java.sql.Types.BIGINT        bigint            -none-      
RS> 25   avg_optimize_cpu_time            java.sql.Types.DOUBLE        float             -none-      
RS> 26   last_optimize_cpu_time           java.sql.Types.BIGINT        bigint            -none-      
RS> 27   avg_compile_memory_kb            java.sql.Types.DOUBLE        float             -none-      
RS> 28   last_compile_memory_kb           java.sql.Types.BIGINT        bigint            -none-      
RS> 29   max_compile_memory_kb            java.sql.Types.BIGINT        bigint            -none-      
RS> 30   is_clouddb_internal_query        java.sql.Types.BIT           bit               -none-      

----------------
2019 - query_store_plan ( 2 new cols [plan_forcing_type, plan_forcing_type_desc] in 2017, no changes: 2017 -> 2019)
----------------
RS> Col# Label                          JDBC Type Name               Guessed DBMS type Source Table
RS> ---- ------------------------------ ---------------------------- ----------------- ------------
RS> 1    plan_id                        java.sql.Types.BIGINT        bigint            -none-      
RS> 2    query_id                       java.sql.Types.BIGINT        bigint            -none-      
RS> 3    plan_group_id                  java.sql.Types.BIGINT        bigint            -none-      
RS> 4    engine_version                 java.sql.Types.NVARCHAR      nvarchar(32)      -none-      
RS> 5    compatibility_level            java.sql.Types.SMALLINT      smallint          -none-      
RS> 6    query_plan_hash                java.sql.Types.BINARY        binary(16)        -none-      
RS> 7    query_plan                     java.sql.Types.NVARCHAR      nvarchar(max)     -none-      
RS> 8    is_online_index_plan           java.sql.Types.BIT           bit               -none-      
RS> 9    is_trivial_plan                java.sql.Types.BIT           bit               -none-      
RS> 10   is_parallel_plan               java.sql.Types.BIT           bit               -none-      
RS> 11   is_forced_plan                 java.sql.Types.BIT           bit               -none-      
RS> 12   is_natively_compiled           java.sql.Types.BIT           bit               -none-      
RS> 13   force_failure_count            java.sql.Types.BIGINT        bigint            -none-      
RS> 14   last_force_failure_reason      java.sql.Types.INTEGER       int               -none-      
RS> 15   last_force_failure_reason_desc java.sql.Types.NVARCHAR      nvarchar(128)     -none-      
RS> 16   count_compiles                 java.sql.Types.BIGINT        bigint            -none-      
RS> 17   initial_compile_start_time     microsoft.sql.DATETIMEOFFSET datetimeoffset    -none-      
RS> 18   last_compile_start_time        microsoft.sql.DATETIMEOFFSET datetimeoffset    -none-      
RS> 19   last_execution_time            microsoft.sql.DATETIMEOFFSET datetimeoffset    -none-      
RS> 20   avg_compile_duration           java.sql.Types.DOUBLE        float             -none-      
RS> 21   last_compile_duration          java.sql.Types.BIGINT        bigint            -none-      
RS> 22   plan_forcing_type              java.sql.Types.INTEGER       int               -none-      NEW IN 2017
RS> 23   plan_forcing_type_desc         java.sql.Types.NVARCHAR      nvarchar(60)      -none-      NEW IN 2017

----------------
2019 - query_store_runtime_stats ( 15 new cols [*num_physical_io_reads, *log_bytes_used, *tempdb_space_used] in 2017, no changes: 2017 -> 2019)
----------------
Main columns {avg|last|max|min|stdev}:
	- duration
	- cpu_time
	- logical_io_reads
	- logical_io_writes
	- physical_io_reads
	- clr_time
	- dop
	- query_max_used_memory
	- rowcount
	- num_physical_io_reads -- NEW IN 2017
	- log_bytes_used        -- NEW IN 2017
	- tempdb_space_used     -- NEW IN 2017
----------------
RS> Col# Label                       JDBC Type Name               Guessed DBMS type Source Table
RS> ---- --------------------------- ---------------------------- ----------------- ------------
RS> 1    runtime_stats_id            java.sql.Types.BIGINT        bigint            -none-      
RS> 2    plan_id                     java.sql.Types.BIGINT        bigint            -none-      
RS> 3    runtime_stats_interval_id   java.sql.Types.BIGINT        bigint            -none-      
RS> 4    execution_type              java.sql.Types.TINYINT       tinyint           -none-      
RS> 5    execution_type_desc         java.sql.Types.NVARCHAR      nvarchar(60)      -none-      
RS> 6    first_execution_time        microsoft.sql.DATETIMEOFFSET datetimeoffset    -none-      
RS> 7    last_execution_time         microsoft.sql.DATETIMEOFFSET datetimeoffset    -none-      
RS> 8    count_executions            java.sql.Types.BIGINT        bigint            -none-      
--------------
RS> 9    avg_duration                java.sql.Types.DOUBLE        float             -none-      
RS> 10   last_duration               java.sql.Types.BIGINT        bigint            -none-      
RS> 11   min_duration                java.sql.Types.BIGINT        bigint            -none-      
RS> 12   max_duration                java.sql.Types.BIGINT        bigint            -none-      
RS> 13   stdev_duration              java.sql.Types.DOUBLE        float             -none-      
RS> 14   avg_cpu_time                java.sql.Types.DOUBLE        float             -none-      
RS> 15   last_cpu_time               java.sql.Types.BIGINT        bigint            -none-      
RS> 16   min_cpu_time                java.sql.Types.BIGINT        bigint            -none-      
RS> 17   max_cpu_time                java.sql.Types.BIGINT        bigint            -none-      
RS> 18   stdev_cpu_time              java.sql.Types.DOUBLE        float             -none-      
RS> 19   avg_logical_io_reads        java.sql.Types.DOUBLE        float             -none-      
RS> 20   last_logical_io_reads       java.sql.Types.BIGINT        bigint            -none-      
RS> 21   min_logical_io_reads        java.sql.Types.BIGINT        bigint            -none-      
RS> 22   max_logical_io_reads        java.sql.Types.BIGINT        bigint            -none-      
RS> 23   stdev_logical_io_reads      java.sql.Types.DOUBLE        float             -none-      
RS> 24   avg_logical_io_writes       java.sql.Types.DOUBLE        float             -none-      
RS> 25   last_logical_io_writes      java.sql.Types.BIGINT        bigint            -none-      
RS> 26   min_logical_io_writes       java.sql.Types.BIGINT        bigint            -none-      
RS> 27   max_logical_io_writes       java.sql.Types.BIGINT        bigint            -none-      
RS> 28   stdev_logical_io_writes     java.sql.Types.DOUBLE        float             -none-      
RS> 29   avg_physical_io_reads       java.sql.Types.DOUBLE        float             -none-      
RS> 30   last_physical_io_reads      java.sql.Types.BIGINT        bigint            -none-      
RS> 31   min_physical_io_reads       java.sql.Types.BIGINT        bigint            -none-      
RS> 32   max_physical_io_reads       java.sql.Types.BIGINT        bigint            -none-      
RS> 33   stdev_physical_io_reads     java.sql.Types.DOUBLE        float             -none-      
RS> 34   avg_clr_time                java.sql.Types.DOUBLE        float             -none-      
RS> 35   last_clr_time               java.sql.Types.BIGINT        bigint            -none-      
RS> 36   min_clr_time                java.sql.Types.BIGINT        bigint            -none-      
RS> 37   max_clr_time                java.sql.Types.BIGINT        bigint            -none-      
RS> 38   stdev_clr_time              java.sql.Types.DOUBLE        float             -none-      
RS> 39   avg_dop                     java.sql.Types.DOUBLE        float             -none-      
RS> 40   last_dop                    java.sql.Types.BIGINT        bigint            -none-      
RS> 41   min_dop                     java.sql.Types.BIGINT        bigint            -none-      
RS> 42   max_dop                     java.sql.Types.BIGINT        bigint            -none-      
RS> 43   stdev_dop                   java.sql.Types.DOUBLE        float             -none-      
RS> 44   avg_query_max_used_memory   java.sql.Types.DOUBLE        float             -none-      
RS> 45   last_query_max_used_memory  java.sql.Types.BIGINT        bigint            -none-      
RS> 46   min_query_max_used_memory   java.sql.Types.BIGINT        bigint            -none-      
RS> 47   max_query_max_used_memory   java.sql.Types.BIGINT        bigint            -none-      
RS> 48   stdev_query_max_used_memory java.sql.Types.DOUBLE        float             -none-      
RS> 49   avg_rowcount                java.sql.Types.DOUBLE        float             -none-      
RS> 50   last_rowcount               java.sql.Types.BIGINT        bigint            -none-      
RS> 51   min_rowcount                java.sql.Types.BIGINT        bigint            -none-      
RS> 52   max_rowcount                java.sql.Types.BIGINT        bigint            -none-      
RS> 53   stdev_rowcount              java.sql.Types.DOUBLE        float             -none-      
RS> 54   avg_num_physical_io_reads   java.sql.Types.DOUBLE        float             -none-      NEW IN 2017
RS> 55   last_num_physical_io_reads  java.sql.Types.BIGINT        bigint            -none-      NEW IN 2017
RS> 56   min_num_physical_io_reads   java.sql.Types.BIGINT        bigint            -none-      NEW IN 2017
RS> 57   max_num_physical_io_reads   java.sql.Types.BIGINT        bigint            -none-      NEW IN 2017
RS> 58   stdev_num_physical_io_reads java.sql.Types.DOUBLE        float             -none-      NEW IN 2017
RS> 59   avg_log_bytes_used          java.sql.Types.DOUBLE        float             -none-      NEW IN 2017
RS> 60   last_log_bytes_used         java.sql.Types.BIGINT        bigint            -none-      NEW IN 2017
RS> 61   min_log_bytes_used          java.sql.Types.BIGINT        bigint            -none-      NEW IN 2017
RS> 62   max_log_bytes_used          java.sql.Types.BIGINT        bigint            -none-      NEW IN 2017
RS> 63   stdev_log_bytes_used        java.sql.Types.DOUBLE        float             -none-      NEW IN 2017
RS> 64   avg_tempdb_space_used       java.sql.Types.DOUBLE        float             -none-      NEW IN 2017
RS> 65   last_tempdb_space_used      java.sql.Types.BIGINT        bigint            -none-      NEW IN 2017
RS> 66   min_tempdb_space_used       java.sql.Types.BIGINT        bigint            -none-      NEW IN 2017
RS> 67   max_tempdb_space_used       java.sql.Types.BIGINT        bigint            -none-      NEW IN 2017
RS> 68   stdev_tempdb_space_used     java.sql.Types.DOUBLE        float             -none-      NEW IN 2017

----------------
2019 - query_store_wait_stats (new in 2017, no changes: 2017 -> 2019)
----------------
RS> Col# Label                     JDBC Type Name          Guessed DBMS type Source Table
RS> ---- ------------------------- ----------------------- ----------------- ------------
RS> 1    wait_stats_id             java.sql.Types.BIGINT   bigint            -none-      
RS> 2    plan_id                   java.sql.Types.BIGINT   bigint            -none-      
RS> 3    runtime_stats_interval_id java.sql.Types.BIGINT   bigint            -none-      
RS> 4    wait_category             java.sql.Types.SMALLINT smallint          -none-      
RS> 5    wait_category_desc        java.sql.Types.NVARCHAR nvarchar(60)      -none-      
RS> 6    execution_type            java.sql.Types.TINYINT  tinyint           -none-      
RS> 7    execution_type_desc       java.sql.Types.NVARCHAR nvarchar(60)      -none-      
RS> 8    total_query_wait_time_ms  java.sql.Types.BIGINT   bigint            -none-      
RS> 9    avg_query_wait_time_ms    java.sql.Types.DOUBLE   float             -none-      
RS> 10   last_query_wait_time_ms   java.sql.Types.BIGINT   bigint            -none-      
RS> 11   min_query_wait_time_ms    java.sql.Types.BIGINT   bigint            -none-      
RS> 12   max_query_wait_time_ms    java.sql.Types.BIGINT   bigint            -none-      
RS> 13   stdev_query_wait_time_ms  java.sql.Types.DOUBLE   float             -none-      

----------------
2019 - query_store_runtime_stats_interval (no changes: 2016 -> 2017 -> 2019)
----------------
RS> Col# Label                     JDBC Type Name               Guessed DBMS type Source Table
RS> ---- ------------------------- ---------------------------- ----------------- ------------
RS> 1    runtime_stats_interval_id java.sql.Types.BIGINT        bigint            -none-      
RS> 2    start_time                microsoft.sql.DATETIMEOFFSET datetimeoffset    -none-      
RS> 3    end_time                  microsoft.sql.DATETIMEOFFSET datetimeoffset    -none-      
RS> 4    comment                   java.sql.Types.NVARCHAR      nvarchar(max)     -none-      

*/