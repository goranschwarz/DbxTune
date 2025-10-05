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
package com.dbxtune.pcs.report.content.os;

import java.io.IOException;
import java.io.Writer;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.cm.CountersModel;
import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.pcs.report.content.IReportChart;
import com.dbxtune.pcs.report.content.RecordingInfo;
import com.dbxtune.pcs.report.content.SparklineHelper;
import com.dbxtune.pcs.report.content.SparklineHelper.AggType;
import com.dbxtune.pcs.report.content.SparklineHelper.DataSource;
import com.dbxtune.pcs.report.content.SparklineHelper.SparkLineParams;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.DbUtils;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.TimeUtils;

public class OsCpuUsageOverview extends OsAbstract
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public OsCpuUsageOverview(DailySummaryReportAbstract reportingInstance)
	{
		super(reportingInstance);
	}

	@Override
	public boolean isEnabled()
	{
		// If super is DISABLED, no need to continue
		boolean isEnabled = super.isEnabled();
		if ( ! isEnabled )
			return isEnabled;

//		// NOT For Windows
//		String dbmsVerStr = getReportingInstance().getDbmsVersionStr();
//		if (StringUtil.hasValue(dbmsVerStr))
//		{
//			if (dbmsVerStr.indexOf("Windows") != -1)
//			{
//				setDisabledReason("This DBMS is running on Windows, which is not supported by this report.");
//				return false;
//			}
//		}
		return true;
	}
	
	@Override
	public boolean hasMinimalMessageText()
	{
		return true;
	}

	@Override
	public boolean hasShortMessageText()
	{
		return true;
	}

	@Override
	public void writeMessageText(Writer sb, MessageType messageType)
	throws IOException
	{
		sb.append(getDbxCentralLinkWithDescForGraphs(false, "Below are CPU Graphs/Charts with various information that can help you decide how the DBMS is handling the load on the Operating System Level.",
				"CmOsMpstat_MpSum",
				"CmOsMpstat_MpCpu",
				"CmOsUptime_AdjLoadAverage",
				_isWindows ? "CmOsMeminfo_WinPaging" : "CmOsVmstat_SwapInOut"
				));

		_CmOsMpstat_MpSum         .writeHtmlContent(sb, null, null);
		_CmOsMpstat_MpCpu         .writeHtmlContent(sb, null, null);
		_CmOsUptime_AdjLoadAverage.writeHtmlContent(sb, null, null);
		
		if (_CmOsVmstat_SwapInOut  != null) _CmOsVmstat_SwapInOut .writeHtmlContent(sb, null, null);
		if (_CmOsMeminfo_WinPaging != null) _CmOsMeminfo_WinPaging.writeHtmlContent(sb, null, null);

		_CmOsMeminfo_MemAvailable .writeHtmlContent(sb, null, null);
		
		if (_CmOsPs_WinPs          != null) _CmOsPs_WinPs.writeHtmlContent(sb, null, null);

		if (_isWindows && _CmOsPs_topProcessesCpuByPid != null)
		{
			RecordingInfo recordingInfo = getReportingInstance().getReportEntryRecordingInfo();
			if (recordingInfo != null)
			{
				String hostMonHostName = recordingInfo.getHostMonitorHostname();
				String osCoreInfo      = recordingInfo.getOsCoreInfo();

				sb.append("<p>\n");
				sb.append("Some info about hostname <code>" + hostMonHostName + "</code><br>" + osCoreInfo);
				sb.append("</p>\n");
			}

			sb.append("<p> \n");
			sb.append("The above chart shows consumed CPU Second(s) per second. <br> \n");
			sb.append("Processes with the same name will be collapsed into a single entity <i>(splitting them on different PID's may cause number of entries to be many, less readable)</i>. <br> \n");
			sb.append("<b>Note</b>: Consumed 'CPU Seconds' for a single PID can be higher than 1. Because: For example 4 Cores system, one PID was running at 100% utilization (consuming all 4 cores), then it will consume 4 seconds... <br> \n");
			sb.append("</p> \n");

			if (_CmOsPs_topProcessesCpuByName != null)
			{
				sb.append("<p> \n");
				sb.append("Below is basically the same as above, but instead of showing the <b>individual</b> PID's we list CPU Time by <b>ProcessName</b></i>. <br> \n");
				sb.append("Only top/first " + _topCpuProcessCountByName + " records will be displayed in this table. <br> \n");
				sb.append("</p> \n");
				sb.append(_CmOsPs_topProcessesCpuByName.toHtmlTableString("sortable"));
			}

			sb.append("<br> \n");
			sb.append("<p> \n");
			sb.append("Below is a Summary Table with Processes that has consumed the most CPU Seconds on this system for the whole Recording Period <i>(probably 24 hours)</i>. <br> \n");
			sb.append("Only top/first " + _topCpuProcessCountByPid + " records will be displayed in this table. <br> \n");
			sb.append("</p> \n");
			sb.append(_CmOsPs_topProcessesCpuByPid.toHtmlTableString("sortable"));
		}

		// Write JavaScript code for CPU SparkLine
		if (isFullMessageType())
		{
			for (String str : _miniChartJsList)
				sb.append(str);
		}
	}

	@Override
	public String getSubject()
	{
		return "OS CPU Usage graph of the full day (origin: CmOsMpstat_abs,CmOsUptime_abs / os-cmd:mpstat,uptime)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}

	/** 
	 * This could potentially be "moved" up one level in the class hierarchy 
	 *  
	 * @param pcsTableName
	 * @param conn
	 * @param localConf
	 */
	private void initSkipNewOrDiffRateRows(String schemaName, String pcsTableName, DbxConnection conn, Configuration localConf)
	{
		if (schemaName == null)
			schemaName = "";

		if (StringUtil.hasValue(schemaName))
			schemaName = "[" + schemaName + "].";

		// Get a dummy "metadata" for the table (so we can check what columns exists)
		String dummySql = "select * from " + schemaName + "[" + pcsTableName + "] where 1 = 2"; // just to get Column names
		ResultSetTableModel dummyRstm = executeQuery(conn, dummySql, true, "metadata");

		// DO NOT TRUST: new data that hasn't yet been DIFF Calculated (it only has 1 sample, so it's probably Asolute values, which are *to high*)
		// If we would trust the above values, it will/may create statistical problems (showing to high values in specific periods)
		boolean skipNewDiffRateRows = localConf.getBooleanProperty(this.getClass().getSimpleName()+".skipNewDiffRateRows", true);

		//  SQL for: only records that has been diff calculations (not first time seen, some ASE Versions has a bug that do not clear counters on reuse)
		_sql_and_skipNewOrDiffRateRows = "";
		if (dummyRstm.hasColumn("CmRowState")) // New column name for 'CmNewDiffRateRow' (which is a bitwise state column)
		{
			// the below will produce for H2:     and  BITAND([CmRowState], 1) = ???   
			//                        for OTHERS: and  ([CmRowState] & 1) = ???
			_sql_and_skipNewOrDiffRateRows = "  and " + conn.toBitAnd("[CmRowState]", CountersModel.ROW_STATE__IS_DIFF_OR_RATE_ROW) + " = 0 \n";
		}
		else if (dummyRstm.hasColumn("CmNewDiffRateRow"))
		{
			// This is the "old" way... and used for backward compatibility
			_sql_and_skipNewOrDiffRateRows = "  and [CmNewDiffRateRow] = 0 \n"; 
		}
		
//FIXME; double check the code for "CmNewDiffRateRow and CmRowState"... ESPECIALLY if we move it up in the class hierarchy (also see AseTopCmStmntCacheDetails for more details/fields)

		// Used by "sparkline" charts to filter out "new diff/rate" rows
		_whereFilter_skipNewDiffRateRows = !skipNewDiffRateRows ? "" : _sql_and_skipNewOrDiffRateRows;
	}
	private String _sql_and_skipNewOrDiffRateRows;
	private String _whereFilter_skipNewDiffRateRows;

	
	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		_isWindows = isWindows();

		// For Linux/Unix do NOT show chart-line for "idlePct"
		String idlePct = "idlePct";

		// If the SQL Server is hosted on Windows... remove chart-line: "% Idle Time" 
		if (_isWindows)
		{
			idlePct = "% Idle Time";
		}

		String schema = getReportingInstance().getDbmsSchemaName();
		
		boolean exists_CmOsPs_WinPs = DbUtils.checkIfTableExistsNoThrow(conn, null, schema, "CmOsPs_diff");
		if (exists_CmOsPs_WinPs)
			initSkipNewOrDiffRateRows(schema, "CmOsPs_diff", conn, localConf);
		
		int maxValue = 100;
		_CmOsMpstat_MpSum          = createTsLineChart(conn, schema, "CmOsMpstat", "MpSum",          maxValue, false, idlePct, "mpstat: CPU usage Summary (Host Monitor->OS CPU(mpstat))");
		_CmOsMpstat_MpCpu          = createTsLineChart(conn, schema, "CmOsMpstat", "MpCpu",          maxValue, false, null,    "mpstat: CPU usage per core (usr+sys+iowait) (Host Monitor->OS CPU(mpstat))");
		_CmOsUptime_AdjLoadAverage = createTsLineChart(conn, schema, "CmOsUptime", "AdjLoadAverage", -1,       false, null,    "uptime: Adjusted Load Average (Host Monitor->OS Load Average(uptime))");

		if ( ! _isWindows)
			_CmOsVmstat_SwapInOut  = createTsLineChart(conn, schema, "CmOsVmstat", "SwapInOut",      -1,       false, null,    "vmstat: Swap In/Out per sec (Host Monitor->OS CPU(vmstat))");
		else
			_CmOsMeminfo_WinPaging = createTsLineChart(conn, schema, "CmOsMeminfo", "WinPaging",     -1,       false, null,    "meminfo: Windows Paging or Swap Usage (Host Monitor->OS Memory Info)");
			
		_CmOsMeminfo_MemAvailable  = createTsLineChart(conn, schema, "CmOsMeminfo", "MemAvailable",  -1,       false, null,    "meminfo: Available Memory (Host Monitor->OS Memory Info)");

		if ( _isWindows )
		{
			_CmOsPs_WinPs                 = createTsLineChart(conn, schema, "CmOsPs", "WinPs",       -1,       false, null,    "ps: Windows Process CPU Used Seconds (per Second) (Host Monitor->OS Top Process(ps))");
			_CmOsPs_topProcessesCpuByPid  = getProcessSumTableByPid(conn);
			_CmOsPs_topProcessesCpuByName = getProcessSumTableByName(conn);
		}
	}

	public static final String PROPKEY_topCpuProcessCountByPid   = "DailySummaryReport.OsCpuUsageOverview.topCpuProcessCount.byPid";
	public static final int    DEFAULT_topCpuProcessCountByPid   = 60;
	
	public static final String PROPKEY_topCpuProcessCountByName  = "DailySummaryReport.OsCpuUsageOverview.topCpuProcessCount.byName";
	public static final int    DEFAULT_topCpuProcessCountByName  = 30;
	
	private boolean _isWindows = false;
	private int     _topCpuProcessCountByPid  = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_topCpuProcessCountByPid , DEFAULT_topCpuProcessCountByPid);
	private int     _topCpuProcessCountByName = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_topCpuProcessCountByName, DEFAULT_topCpuProcessCountByName);

	private IReportChart _CmOsMpstat_MpSum;
	private IReportChart _CmOsMpstat_MpCpu;
	private IReportChart _CmOsUptime_AdjLoadAverage;
	private IReportChart _CmOsVmstat_SwapInOut;
	private IReportChart _CmOsMeminfo_WinPaging;
	private IReportChart _CmOsMeminfo_MemAvailable;
	private IReportChart _CmOsPs_WinPs;

	private ResultSetTableModel _CmOsPs_topProcessesCpuByPid;
	private ResultSetTableModel _CmOsPs_topProcessesCpuByName;

	private List<String>        _miniChartJsList = new ArrayList<>();

	/**
	 * Get a table with ProcessName, Id, UsedCpuSeconds, Pct <br>
	 * of all processes during the recording period!
	 * 
	 * @param conn
	 * @return
	 */
	private ResultSetTableModel getProcessSumTableByPid(DbxConnection conn)
	{
		// If table do NOT exists, no need to continue!
		if ( ! DbUtils.checkIfTableExistsNoThrow(conn, null, null, "CmOsPs_diff") )
		{
			_logger.info("Skipping 'Top CPU Processes Usage' table output, since the table 'CmOsPs_diff' did not exist.");
			
			return null;
		}

		String schema       = getReportingInstance().getDbmsSchemaName();
//		String schemaPrefix = getReportingInstance().getDbmsSchemaNameSqlPrefix();

		String sql = ""
			    + "SELECT \n"
			    + "     [ProcessName] \n"
			    + "    ,[Id] \n"
			    + "    ,SUM([CPU(s)]) AS [UsedCpuSeconds] \n"
			    + "    ,''            AS [UsedCpuSecondsHms] \n"
			    + "    ,COUNT(*)      AS [SampleCount] \n"
			    + "    ,CAST(sum([CPU(s)]) * 100.0 / sum(sum([CPU(s)])) over () as numeric(10,1)) AS [Pct] \n"
			    + "    ,CAST('' as varchar(512))                                as [UsedCpuSeconds__chart] \n"
			    + "    ,MIN([SessionSampleTime]) AS [FirstSampleTime] \n"
			    + "    ,MAX([SessionSampleTime]) AS [LastSampleTime] \n"
			    + "FROM [CmOsPs_diff] \n"
			    + "WHERE [CPU(s)] > 0.0 \n"
				+ getReportPeriodSqlWhere()
				+ _sql_and_skipNewOrDiffRateRows // only records that has been diff calculations (not first time seen, when it swaps in/out due to execution every x minute)
			    + "GROUP BY [Id], [ProcessName] \n"
			    + "ORDER BY [UsedCpuSeconds] DESC \n"
			    + "";
		
		ResultSetTableModel rstm = executeQuery(conn, sql, false, "topCpuUsage");
		
		if (rstm != null)
		{
			// Describe the table
			rstm.setColumnDescription("ProcessName"          , "The name of the OS Process");
			rstm.setColumnDescription("Id"                   , "Process ID");
			rstm.setColumnDescription("UsedCpuSeconds"       , "CPU Seconds Consumed by this process for the entire Reporting Period");
			rstm.setColumnDescription("UsedCpuSecondsHms"    , "Same as 'UsedCpuSeconds', just presented as HH:MM:SS for readability!");
			rstm.setColumnDescription("SampleCount"          , "Number of records found in the sample period for this PID.");
			rstm.setColumnDescription("Pct"                  , "Whats the Percent of all the rows in the table.");
			rstm.setColumnDescription("FirstSampleTime"      , "First Date/Time in this period where this PID was recorded");
			rstm.setColumnDescription("LastSampleTime"       , "Last Date/Time in this period where this PID was recorded");

			// Set 'UsedCpuSecondsHms'
			for (int r=0; r<rstm.getRowCount(); r++)
			{
				// Get seconds (yes sub seconds will be "lost" in translation...)
				int usedCpuSeconds = rstm.getValueAsInteger(r, "UsedCpuSeconds", true, 0);

				// Translate seconds to [3d] HH:MM:SS
				String hms = TimeUtils.secToTimeStrLong(usedCpuSeconds);
				
				// Set the value
				rstm.setValueAtWithOverride(hms, r, "UsedCpuSecondsHms");
			}
			
			// Only keep first 100 rows
			rstm.removeRowsAfterSize(_topCpuProcessCountByPid);

			// Highlight sort column
			rstm.setHighlightSortColumns("UsedCpuSeconds");

			// Set 'Sparklines'... Mini Chart on
			String whereKeyColumn = "Id";

			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, rstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("UsedCpuSeconds__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsSchemaName           (schema)
					.setDbmsTableName            ("CmOsPs_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
					.setDbmsDataValueColumnName  ("CPU(s)").setGroupDataAggregationType(AggType.SUM)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setDbmsExtraWhereClause     (_whereFilter_skipNewDiffRateRows)
//					.setSparklineTooltipPostfix  ("SUM 'UsedCpuSeconds' in below period")
					.validate()));

		}
		
		return rstm;
	}

	/**
	 * Get a table with ProcessName, Id, UsedCpuSeconds, Pct <br>
	 * of all processes during the recording period!
	 * 
	 * @param conn
	 * @return
	 */
	private ResultSetTableModel getProcessSumTableByName(DbxConnection conn)
	{
		// If table do NOT exists, no need to continue!
		if ( ! DbUtils.checkIfTableExistsNoThrow(conn, null, null, "CmOsPs_diff") )
		{
			_logger.info("Skipping 'Top CPU Processes Usage' table output, since the table 'CmOsPs_diff' did not exist.");
			
			return null;
		}

		String schema       = getReportingInstance().getDbmsSchemaName();
//		String schemaPrefix = getReportingInstance().getDbmsSchemaNameSqlPrefix();

		String sql = ""
			    + "SELECT \n"
			    + "     [ProcessName] \n"
			    + "    ,SUM([CPU(s)])        AS [UsedCpuSeconds] \n"
			    + "    ,''                   AS [UsedCpuSecondsHms] \n"
			    + "    ,COUNT(*)             AS [SampleCount] \n"
			    + "    ,COUNT(distinct [Id]) AS [ProcCount] \n"
			    + "    ,CAST(sum([CPU(s)]) * 100.0 / sum(sum([CPU(s)])) over () as numeric(10,1)) AS [Pct] \n"
			    + "    ,CAST('' as varchar(512))                                as [UsedCpuSeconds__chart] \n"
			    + "    ,MIN([SessionSampleTime]) AS [FirstSampleTime] \n"
			    + "    ,MAX([SessionSampleTime]) AS [LastSampleTime] \n"
			    + "FROM [CmOsPs_diff] \n"
			    + "WHERE [CPU(s)] > 0.0 \n"
				+ getReportPeriodSqlWhere()
				+ _sql_and_skipNewOrDiffRateRows // only records that has been diff calculations (not first time seen, when it swaps in/out due to execution every x minute)
			    + "GROUP BY [ProcessName] \n"
			    + "ORDER BY [UsedCpuSeconds] DESC \n"
			    + "";
		
		ResultSetTableModel rstm = executeQuery(conn, sql, false, "topCpuUsage");
		
		if (rstm != null)
		{
			// Describe the table
			rstm.setColumnDescription("ProcessName"          , "The name of the OS Process");
			rstm.setColumnDescription("UsedCpuSeconds"       , "CPU Seconds Consumed by this process for the entire Reporting Period");
			rstm.setColumnDescription("UsedCpuSecondsHms"    , "Same as 'UsedCpuSeconds', just presented as HH:MM:SS for readability!");
			rstm.setColumnDescription("SampleCount"          , "Number of records found in the sample period for this ProcessName.");
			rstm.setColumnDescription("ProcCount"            , "Number of distinct PID's found in the sample period for this ProcessName.");
			rstm.setColumnDescription("Pct"                  , "Whats the Percent of all the rows in the table.");
			rstm.setColumnDescription("UsedCpuSeconds__chart", "A Mini Chart for CPU Usage, grouped in 10 minutes interval");
			rstm.setColumnDescription("FirstSampleTime"      , "First Date/Time in this period where this ProcessName was recorded");
			rstm.setColumnDescription("LastSampleTime"       , "Last Date/Time in this period where this ProcessName was recorded");

			// Set 'UsedCpuSecondsHms'
			for (int r=0; r<rstm.getRowCount(); r++)
			{
				// Get seconds (yes sub seconds will be "lost" in translation...)
				int usedCpuSeconds = rstm.getValueAsInteger(r, "UsedCpuSeconds", true, 0);

				// Translate seconds to [3d] HH:MM:SS
				String hms = TimeUtils.secToTimeStrLong(usedCpuSeconds);
				
				// Set the value
				rstm.setValueAtWithOverride(hms, r, "UsedCpuSecondsHms");
			}
			
			// Only keep first 100 rows
			rstm.removeRowsAfterSize(_topCpuProcessCountByName);

			// Highlight sort column
			rstm.setHighlightSortColumns("UsedCpuSeconds");

			// Set 'Sparklines'... Mini Chart on
			String whereKeyColumn = "ProcessName";

			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, rstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("UsedCpuSeconds__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsSchemaName           (schema)
					.setDbmsTableName            ("CmOsPs_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
					.setDbmsDataValueColumnName  ("CPU(s)").setGroupDataAggregationType(AggType.SUM)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setDbmsExtraWhereClause     (_whereFilter_skipNewDiffRateRows)
//					.setSparklineTooltipPostfix  ("SUM 'UsedCpuSeconds' in below period")
					.validate()));

		}
		
		return rstm;
	}
}

