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
import com.asetune.pcs.report.content.postgres.PostgresConfig;
import com.asetune.pcs.report.content.postgres.PostgresDbSize;
import com.asetune.pcs.report.content.postgres.PostgresTopSql;
import com.asetune.pcs.report.content.postgres.PostgresTopTableAccess;
import com.asetune.pcs.report.content.postgres.PostgresTopTableSize;

public class DailySummaryReportPostgresTune 
extends DailySummaryReportDefault
{
	@Override
	public void addReportEntries()
	{
		// Add the Alarms Active/History
		super.addReportEntries();

		// CPU
		addReportEntry( new OsCpuUsageOverview    (this) );

		// SQL
		addReportEntry( new PostgresTopSql        (this) );
		addReportEntry( new PostgresTopTableAccess(this) );

		// Disk IO Activity
		addReportEntry( new OsSpaceUsageOverview  (this) );
		addReportEntry( new PostgresConfig        (this) );
		addReportEntry( new PostgresDbSize        (this) );  // DB SIZE and or growth
		addReportEntry( new PostgresTopTableSize  (this) );
		addReportEntry( new OsIoStatSlowIo        (this) );
	}
}
