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
package com.asetune.pcs.report;

import com.asetune.pcs.report.content.os.OsCpuUsageOverview;
import com.asetune.pcs.report.content.os.OsIoStatOverview;
import com.asetune.pcs.report.content.os.OsIoStatSlowIo;
import com.asetune.pcs.report.content.os.OsSpaceUsageOverview;
import com.asetune.pcs.report.content.sqlserver.SqlServerCmDeviceIo;
import com.asetune.pcs.report.content.sqlserver.SqlServerConfiguration;
import com.asetune.pcs.report.content.sqlserver.SqlServerCpuUsageOverview;
import com.asetune.pcs.report.content.sqlserver.SqlServerDbSize;
import com.asetune.pcs.report.content.sqlserver.SqlServerMissingIndexes;
import com.asetune.pcs.report.content.sqlserver.SqlServerQueryStore;
import com.asetune.pcs.report.content.sqlserver.SqlServerSlowCmDeviceIo;
import com.asetune.pcs.report.content.sqlserver.SqlServerTopCmExecCursors;
import com.asetune.pcs.report.content.sqlserver.SqlServerTopCmExecFunctionStats;
import com.asetune.pcs.report.content.sqlserver.SqlServerTopCmExecProcedureStats;
import com.asetune.pcs.report.content.sqlserver.SqlServerTopCmExecQueryStats;
import com.asetune.pcs.report.content.sqlserver.SqlServerTopCmExecTriggerStats;
import com.asetune.pcs.report.content.sqlserver.SqlServerTopCmIndexOpStat;
import com.asetune.pcs.report.content.sqlserver.SqlServerTopCmIndexPhysicalAvgPageUsedPct;
import com.asetune.pcs.report.content.sqlserver.SqlServerTopCmIndexPhysicalTabSize;
import com.asetune.pcs.report.content.sqlserver.SqlServerTopCmTableSize;
import com.asetune.pcs.report.content.sqlserver.SqlServerUnusedIndexes;
import com.asetune.pcs.report.content.sqlserver.SqlServerWaitStats;
import com.asetune.pcs.report.content.sqlserver.SqlServerTopCmIndexOpStat.ReportType;

public class DailySummaryReportSqlServerTune 
extends DailySummaryReportDefault
{
	@Override
	public void addReportEntries()
	{
		// Add the Alarms Active/History
		super.addReportEntries();

		// CPU
		addReportEntry( new SqlServerCpuUsageOverview(this)   );
		addReportEntry( new SqlServerWaitStats(this)          );
		addReportEntry( new OsCpuUsageOverview(this)          );
		
		// SQL
		addReportEntry( new SqlServerTopCmExecQueryStats    (this));
				// CmExecQueryStatsCpu         Order BY: CPU Usage (total_worker_time)
				// CmExecQueryStatsWaits       Order BY: CPU Usage (total_elapsed_time - total_worker_time)
				// CmExecQueryStatsSpills      Order BY: tempdb Spills
				// CmExecQueryStatsLReads      Order BY: Logical Reads
				// CmExecQueryStatsPReads      Order BY: Physical Reads
				// CmExecQueryStatsWrites      Order BY: Physical Writes
				// CmExecQueryStatsExecCnt     Order BY: Execution Count
				// CmExecQueryStatsCompile     Order BY: "recent compilations"
				// CmExecQueryStatsMemoryGrant Order BY: memory_grant
		addReportEntry( new SqlServerTopCmExecProcedureStats(this));
		addReportEntry( new SqlServerTopCmExecFunctionStats (this));
		addReportEntry( new SqlServerTopCmExecTriggerStats  (this));
		addReportEntry( new SqlServerTopCmExecCursors   (this));
		//addReportEntry( new SqlServerTopSlowSql(this)           );
		//addReportEntry( new SqlServerTopSlowProcCalls(this)     );

		// QueryStore
		addReportEntry( new SqlServerQueryStore(this)         );

		// Disk IO Activity
		addReportEntry( new SqlServerCmDeviceIo(this)         );
		addReportEntry( new SqlServerSlowCmDeviceIo(this)     );
		addReportEntry( new OsSpaceUsageOverview(this)        );
		addReportEntry( new OsIoStatOverview(this)            );
		addReportEntry( new OsIoStatSlowIo(this)              );

		// SQL: Accessed Tables
		addReportEntry( new SqlServerTopCmIndexOpStat(this, ReportType.BY_LOCKS) );
		addReportEntry( new SqlServerTopCmIndexOpStat(this, ReportType.BY_WAITS) );
		addReportEntry( new SqlServerTopCmIndexOpStat(this, ReportType.BY_CRUD)  );
		addReportEntry( new SqlServerTopCmIndexOpStat(this, ReportType.BY_IO)    );
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
	}
}
