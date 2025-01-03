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
package com.dbxtune.pcs.report;

import java.lang.invoke.MethodHandles;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.report.content.DbmsConfigIssues;
import com.dbxtune.pcs.report.content.os.CmOsNwInfoOverview;
import com.dbxtune.pcs.report.content.os.OsCpuUsageOverview;
import com.dbxtune.pcs.report.content.os.OsIoStatOverview;
import com.dbxtune.pcs.report.content.os.OsIoStatSlowIo;
import com.dbxtune.pcs.report.content.os.OsSpaceUsageOverview;
import com.dbxtune.pcs.report.content.sqlserver.SqlServerCmDeviceIo;
import com.dbxtune.pcs.report.content.sqlserver.SqlServerConfiguration;
import com.dbxtune.pcs.report.content.sqlserver.SqlServerCpuUsageOverview;
import com.dbxtune.pcs.report.content.sqlserver.SqlServerDbSize;
import com.dbxtune.pcs.report.content.sqlserver.SqlServerDeadlocks;
import com.dbxtune.pcs.report.content.sqlserver.SqlServerJobScheduler;
import com.dbxtune.pcs.report.content.sqlserver.SqlServerMissingIndexes;
import com.dbxtune.pcs.report.content.sqlserver.SqlServerPlanCacheHistory;
import com.dbxtune.pcs.report.content.sqlserver.SqlServerQueryStore;
import com.dbxtune.pcs.report.content.sqlserver.SqlServerSlowCmDeviceIo;
import com.dbxtune.pcs.report.content.sqlserver.SqlServerTopCmExecCursors;
import com.dbxtune.pcs.report.content.sqlserver.SqlServerTopCmExecFunctionStats;
import com.dbxtune.pcs.report.content.sqlserver.SqlServerTopCmExecProcedureStats;
import com.dbxtune.pcs.report.content.sqlserver.SqlServerTopCmExecQueryStatPerDb;
import com.dbxtune.pcs.report.content.sqlserver.SqlServerTopCmExecQueryStats;
import com.dbxtune.pcs.report.content.sqlserver.SqlServerTopCmExecTriggerStats;
import com.dbxtune.pcs.report.content.sqlserver.SqlServerTopCmIndexOpStat;
import com.dbxtune.pcs.report.content.sqlserver.SqlServerTopCmIndexPhysicalAvgPageUsedPct;
import com.dbxtune.pcs.report.content.sqlserver.SqlServerTopCmIndexPhysicalTabSize;
import com.dbxtune.pcs.report.content.sqlserver.SqlServerTopCmTableSize;
import com.dbxtune.pcs.report.content.sqlserver.SqlServerUnusedIndexes;
import com.dbxtune.pcs.report.content.sqlserver.SqlServerWaitStats;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.StringUtil;

public class DailySummaryReportSqlServerTune 
extends DailySummaryReportDefault
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	@Override
	public void addReportEntries()
	{
		// Add the Alarms Active/History
		super.addReportEntries();

		// CPU
		addReportEntry( new SqlServerCpuUsageOverview(this)   );
		addReportEntry( new SqlServerWaitStats(this)          );
		addReportEntry( new OsCpuUsageOverview(this)          );
		
		// Job Agent/Scheduler
		addReportEntry( new SqlServerJobScheduler           (this));

		// SQL
		addReportEntry( new SqlServerPlanCacheHistory       (this));        // Check if the Plan Cache can be trusted... https://www.brentozar.com/archive/2018/07/tsql2sday-how-much-plan-cache-history-do-you-have/
		addReportEntry( new SqlServerDeadlocks              (this));
//		addReportEntry( new SqlServerTopCmExecQueryStatsDb  (this)); // Older version of the "DB" statistics
		addReportEntry( new SqlServerTopCmExecQueryStatPerDb(this));
		addReportEntry( new SqlServerTopCmExecQueryStats    (this, SqlServerTopCmExecQueryStats.ReportType.CPU_TIME));
		addReportEntry( new SqlServerTopCmExecQueryStats    (this, SqlServerTopCmExecQueryStats.ReportType.EST_WAIT_TIME));
		addReportEntry( new SqlServerTopCmExecQueryStats    (this, SqlServerTopCmExecQueryStats.ReportType.TEMPDB_SPILLS));
		addReportEntry( new SqlServerTopCmExecQueryStats    (this, SqlServerTopCmExecQueryStats.ReportType.LOGICAL_READS));
		addReportEntry( new SqlServerTopCmExecQueryStats    (this, SqlServerTopCmExecQueryStats.ReportType.LOGICAL_WRITES));
		addReportEntry( new SqlServerTopCmExecQueryStats    (this, SqlServerTopCmExecQueryStats.ReportType.PHYSICAL_READS));
		addReportEntry( new SqlServerTopCmExecQueryStats    (this, SqlServerTopCmExecQueryStats.ReportType.EXECUTION_COUNT));
//		addReportEntry( new SqlServerTopCmExecQueryStats    (this, SqlServerTopCmExecQueryStats.ReportType.RECENTLY_COMPILED));
		addReportEntry( new SqlServerTopCmExecQueryStats    (this, SqlServerTopCmExecQueryStats.ReportType.MEMORY_GRANTS));
		addReportEntry( new SqlServerTopCmExecProcedureStats(this));	// Part of mail-message
		addReportEntry( new SqlServerTopCmExecFunctionStats (this));
		addReportEntry( new SqlServerTopCmExecTriggerStats  (this));
		addReportEntry( new SqlServerTopCmExecCursors       (this));

		// QueryStore
		addReportEntry( new SqlServerQueryStore(this)         );

		// Disk IO Activity
		addReportEntry( new SqlServerCmDeviceIo(this)         );
		addReportEntry( new SqlServerSlowCmDeviceIo(this)     );
		addReportEntry( new OsSpaceUsageOverview(this)        );
		addReportEntry( new OsIoStatOverview(this)            );
		addReportEntry( new OsIoStatSlowIo(this)              );

		// Network Activity
		addReportEntry( new CmOsNwInfoOverview(this)          );

		// SQL: Accessed Tables
		addReportEntry( new SqlServerTopCmIndexOpStat(this, SqlServerTopCmIndexOpStat.ReportType.BY_LOCKS) );
		addReportEntry( new SqlServerTopCmIndexOpStat(this, SqlServerTopCmIndexOpStat.ReportType.BY_WAITS) );
		addReportEntry( new SqlServerTopCmIndexOpStat(this, SqlServerTopCmIndexOpStat.ReportType.BY_CRUD)  );
		addReportEntry( new SqlServerTopCmIndexOpStat(this, SqlServerTopCmIndexOpStat.ReportType.BY_IO)    );
		addReportEntry( new SqlServerTopCmTableSize(this) );
		addReportEntry( new SqlServerTopCmIndexPhysicalTabSize(this) );
		addReportEntry( new SqlServerTopCmIndexPhysicalAvgPageUsedPct(this) );

		// SQL: Missing/Unused Indexes
		addReportEntry( new SqlServerMissingIndexes(this) );
		addReportEntry( new SqlServerUnusedIndexes(this) );

		// Database Size
		addReportEntry( new SqlServerDbSize(this)             );

		// Configuration
		addReportEntry( new SqlServerConfiguration(this)      );
		addReportEntry( new DbmsConfigIssues(this)            );
	}




	/**
	 * Create a Map of "other information" like "SOrt Order" and other information, used in the "Recording Information" section
	 */
	@Override
	public Map<String, String> createDbmsOtherInfoMap(DbxConnection conn)
	{
		Map<String, String> otherInfo = new LinkedHashMap<>();
		String sql;

		//-------------------------------------------------------
		// License Info
		//-------------------------------------------------------
		// This can be grabbed from the Version String
		sql = "select top 1 [srvVersion] from [CmSummary_abs]";
		
		sql = conn.quotifySqlString(sql);
		try ( Statement stmnt = conn.createStatement() )
		{
			String versionStr = "";

			// Unlimited execution time
			stmnt.setQueryTimeout(0);
			try ( ResultSet rs = stmnt.executeQuery(sql) )
			{
				while(rs.next())
					versionStr = rs.getString(1);
			}

			// Remove: newLines, Tabs, and collapse double space into single space
			versionStr = versionStr.replace('\n', ' ').replace('\t', ' ');
			while(versionStr.contains("  "))
				versionStr = versionStr.replace("  ", " ");

			String edition = "";
			if      (versionStr.contains("Microsoft Corporation Standard Edition"  )) edition = "Standard Edition";
			else if (versionStr.contains("Microsoft Corporation Enterprise Edition")) edition = "Enterprise Edition";
			else if (versionStr.contains("Microsoft Corporation Web Edition"       )) edition = "Web Edition";
			else if (versionStr.contains("Microsoft Corporation Developer Edition" )) edition = "Developer Edition";
			else if (versionStr.contains("Microsoft Corporation Express Edition"   )) edition = "Express Edition";
			else if (versionStr.contains("Microsoft SQL Azure "                    )) edition = "Azure";
			
			if (StringUtil.hasValue(edition))
				otherInfo.put("SQL Server Edition", edition);
		}
		catch(Exception ex)
		{
			_logger.warn("Problems getting first row for @@version from CmSummary_abs.", ex);
		}
		

		
		//-------------------------------------------------------
		// Collation/Sort Order
		//-------------------------------------------------------
		sql = ""
			    + "select [configText] \n"
			    + "from [MonSessionDbmsConfigText] \n"
			    + "where [configName] = 'SqlServerHelpSort' \n"
			    + "  and [SessionStartTime] = (select max([SessionStartTime]) from [MonSessionDbmsConfigText]) \n"
			    + "";

		sql = conn.quotifySqlString(sql);
		try ( Statement stmnt = conn.createStatement() )
		{
			String configText = "";

			// Unlimited execution time
			stmnt.setQueryTimeout(0);
			try ( ResultSet rs = stmnt.executeQuery(sql) )
			{
				while(rs.next())
					configText = rs.getString(1);
			}

			// +-------------------------------------------------------------------------------------------------------------------------------+
			// |Server default collation                                                                                                       |
			// +-------------------------------------------------------------------------------------------------------------------------------+
			// |Latin1-General-100, case-insensitive, accent-sensitive, kanatype-insensitive, width-insensitive, supplementary characters, UTF8|
			// +-------------------------------------------------------------------------------------------------------------------------------+
			// Rows 1
			// +--------------------------------+
			// |SERVERPROPERTY_Collation        |
			// +--------------------------------+
			// |Latin1_General_100_CI_AS_SC_UTF8|
			// +--------------------------------+
			// Rows 1
			
			String collationName = "";
			String collationDesc = "";

			for (ResultSetTableModel rstm : ResultSetTableModel.parseTextTables(configText))
			{
				if (rstm.hasColumn("Server default collation"))
				{
					collationDesc = rstm.getValueAsString(0, 0); // rowPos & colPos is at ModelPosition, starting at 0
				}

				if (rstm.hasColumn("SERVERPROPERTY_Collation"))
				{
					collationName = rstm.getValueAsString(0, 0); // rowPos & colPos is at ModelPosition, starting at 0
				}
			}

			// Put them in the "desired" order
			if (StringUtil.hasValue(collationName)) otherInfo.put("Collation Name"       , collationName);
			if (StringUtil.hasValue(collationDesc)) otherInfo.put("Collation Description", collationDesc);
		}
		catch(Exception ex)
		{
			_logger.warn("Problems getting SQL Server Config Information from table 'MonSessionDbmsConfigText'.", ex);
		}

		//-------------------------------------------------------
		// return
		//-------------------------------------------------------
		return otherInfo;
	}

}
