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
import com.asetune.pcs.report.content.os.OsIoStatSlowIo;
import com.asetune.pcs.report.content.os.OsSpaceUsageOverview;
import com.asetune.pcs.report.content.sqlserver.SqlServerConfiguration;
import com.asetune.pcs.report.content.sqlserver.SqlServerCpuUsageOverview;
import com.asetune.pcs.report.content.sqlserver.SqlServerDbSize;
import com.asetune.pcs.report.content.sqlserver.SqlServerSlowCmDeviceIo;
import com.asetune.pcs.report.content.sqlserver.SqlServerTopCmExecQueryStats;
import com.asetune.pcs.report.content.sqlserver.SqlServerTopCmIndexPhysicalAvgPageUsedPct;
import com.asetune.pcs.report.content.sqlserver.SqlServerTopCmIndexPhysicalTabSize;

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
		addReportEntry( new OsCpuUsageOverview(this)          );

		// SQL
		addReportEntry( new SqlServerTopCmExecQueryStats(this)    );
//		addReportEntry( new SqlServerTopCmExecProcedureStats(this));
		//addReportEntry( new SqlServerTopSlowSql(this)           );
		//addReportEntry( new SqlServerTopSlowProcCalls(this)     );

		// Disk IO Activity
		addReportEntry( new SqlServerSlowCmDeviceIo(this)     );
		addReportEntry( new OsSpaceUsageOverview(this)        );
		addReportEntry( new OsIoStatSlowIo(this)              );

		// SQL: Accessed Tables
//		addReportEntry( new SqlServerTopCmObjectActivity(this)    );
//		addReportEntry( new SqlServerTopCmObjectActivityTabSize(this) );
		addReportEntry( new SqlServerTopCmIndexPhysicalTabSize(this) );
		addReportEntry( new SqlServerTopCmIndexPhysicalAvgPageUsedPct(this) );

		// Database Size
		addReportEntry( new SqlServerDbSize(this)             );

		// Configuration
		addReportEntry( new SqlServerConfiguration(this)      );
	}
}
