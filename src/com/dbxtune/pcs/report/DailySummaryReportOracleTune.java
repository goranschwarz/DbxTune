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
import com.dbxtune.pcs.report.content.oracle.OracleAlertLogErrors;
import com.dbxtune.pcs.report.content.oracle.OracleBackups;
import com.dbxtune.pcs.report.content.oracle.OracleDataGuard;
import com.dbxtune.pcs.report.content.oracle.OracleJobFailures;
import com.dbxtune.pcs.report.content.oracle.OracleLoadOverview;
import com.dbxtune.pcs.report.content.oracle.OracleSegmentSize;
import com.dbxtune.pcs.report.content.oracle.OracleSpaceOverview;
import com.dbxtune.pcs.report.content.oracle.OracleTopSegments;
import com.dbxtune.pcs.report.content.oracle.OracleTopSql;
import com.dbxtune.pcs.report.content.oracle.OracleUnusedIndexes;
import com.dbxtune.pcs.report.content.oracle.OracleWaitStats;
import com.dbxtune.pcs.report.content.os.CmOsNwInfoOverview;
import com.dbxtune.pcs.report.content.os.OsCpuUsageOverview;
import com.dbxtune.pcs.report.content.os.OsIoStatOverview;
import com.dbxtune.pcs.report.content.os.OsIoStatSlowIo;

public class DailySummaryReportOracleTune 
extends DailySummaryReportDefault
{
	@Override
	public void addReportEntries()
	{
		// Add the Alarms Active/History
		super.addReportEntries();

		// Load / CPU / Waits
		addReportEntry( new OracleLoadOverview(this)   );
		addReportEntry( new OsCpuUsageOverview(this)   );
		addReportEntry( new OracleWaitStats(this)      );

		// SQL, Tables and Indexes
		addReportEntry( new OracleTopSql(this)         );
		addReportEntry( new OracleTopSegments(this)    );

		// Alert Log, Backups, Jobs and Data Guard
		addReportEntry( new OracleAlertLogErrors(this) );
		addReportEntry( new OracleBackups(this)        );
		addReportEntry( new OracleJobFailures(this)    );
		addReportEntry( new OracleDataGuard(this)      );

		// Space, Disk IO Activity
		addReportEntry( new OracleSpaceOverview(this)  );
		addReportEntry( new OracleSegmentSize(this)    );
		addReportEntry( new OracleUnusedIndexes(this)  );
//		addReportEntry( new OsSpaceUsageOverview(this) );
		addReportEntry( new OsIoStatOverview(this)     );
		addReportEntry( new OsIoStatSlowIo(this)       );

		// Network Activity
		addReportEntry( new CmOsNwInfoOverview(this)   );

		// Configuration
		addReportEntry( new DbmsConfigIssues(this)     );
	}
}
