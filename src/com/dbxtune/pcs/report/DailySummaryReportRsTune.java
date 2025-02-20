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

import com.dbxtune.pcs.report.content.os.CmOsNwInfoOverview;
import com.dbxtune.pcs.report.content.os.OsCpuUsageOverview;
import com.dbxtune.pcs.report.content.os.OsIoStatOverview;
import com.dbxtune.pcs.report.content.os.OsIoStatSlowIo;
import com.dbxtune.pcs.report.content.rs.RsRssdQueueSize;
import com.dbxtune.pcs.report.content.rs.RsWsRepLatency;

public class DailySummaryReportRsTune 
extends DailySummaryReportDefault
{
	@Override
	public void addReportEntries()
	{
		// Add the Alarms Active/History
		super.addReportEntries();

		// CPU
		addReportEntry( new OsCpuUsageOverview(this)   );

		// QUEUE SIZE & Latency
		addReportEntry( new RsRssdQueueSize(this)      );
		addReportEntry( new RsWsRepLatency(this)       );

		// Disk IO Activity
		addReportEntry( new OsIoStatOverview(this)     );
		addReportEntry( new OsIoStatSlowIo(this)       );

		// Network Activity
		addReportEntry( new CmOsNwInfoOverview(this)   );
	}
}
