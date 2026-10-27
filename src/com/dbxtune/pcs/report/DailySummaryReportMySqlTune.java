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
package com.dbxtune.pcs.report;

import com.dbxtune.pcs.report.content.DbmsConfigIssues;
import com.dbxtune.pcs.report.content.mysql.MySqlErrorLog;
import com.dbxtune.pcs.report.content.mysql.MySqlLoadOverview;
import com.dbxtune.pcs.report.content.mysql.MySqlLongRunningStmnts;
import com.dbxtune.pcs.report.content.mysql.MySqlMemory;
import com.dbxtune.pcs.report.content.mysql.MySqlReplication;
import com.dbxtune.pcs.report.content.mysql.MySqlTableSize;
import com.dbxtune.pcs.report.content.mysql.MySqlTopSql;
import com.dbxtune.pcs.report.content.mysql.MySqlTopTableAccess;
import com.dbxtune.pcs.report.content.mysql.MySqlWaitStats;
import com.dbxtune.pcs.report.content.os.CmOsNwInfoOverview;
import com.dbxtune.pcs.report.content.os.OsCpuUsageOverview;
import com.dbxtune.pcs.report.content.os.OsIoStatOverview;
import com.dbxtune.pcs.report.content.os.OsIoStatSlowIo;
import com.dbxtune.pcs.report.content.os.OsSpaceUsageOverview;

public class DailySummaryReportMySqlTune
extends DailySummaryReportDefault
{
	@Override
	public void addReportEntries()
	{
		// Add the Alarms Active/History
		super.addReportEntries();

		// Load / CPU / Waits
		addReportEntry( new MySqlLoadOverview(this)      );
		addReportEntry( new OsCpuUsageOverview(this)     );
		addReportEntry( new MySqlWaitStats(this)         );

		// SQL, Tables, Long Running and Blocking
		addReportEntry( new MySqlTopSql(this)            );
		addReportEntry( new MySqlTopTableAccess(this)    );
		addReportEntry( new MySqlLongRunningStmnts(this) );

		// Error Log and Replication
		addReportEntry( new MySqlErrorLog(this)          );
		addReportEntry( new MySqlReplication(this)       );

		// Space, Memory, Disk IO Activity
		addReportEntry( new MySqlTableSize(this)         );
		addReportEntry( new MySqlMemory(this)            );
		addReportEntry( new OsSpaceUsageOverview(this)   );
		addReportEntry( new OsIoStatOverview(this)       );
		addReportEntry( new OsIoStatSlowIo(this)         );

		// Network Activity
		addReportEntry( new CmOsNwInfoOverview(this)     );

		// Configuration
		addReportEntry( new DbmsConfigIssues(this)       );
	}
}
