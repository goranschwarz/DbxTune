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

import com.asetune.pcs.report.content.ase.AseCpuUsageOverview;
import com.asetune.pcs.report.content.ase.AseDbSize;
import com.asetune.pcs.report.content.ase.AseErrorInfo;
import com.asetune.pcs.report.content.ase.AseSlowCmDeviceIo;
import com.asetune.pcs.report.content.ase.AseTopCmActiveStatements;
import com.asetune.pcs.report.content.ase.AseTopCmCachedProcs;
import com.asetune.pcs.report.content.ase.AseTopCmObjectActivity;
import com.asetune.pcs.report.content.ase.AseTopCmStmntCacheDetails;
import com.asetune.pcs.report.content.ase.AseTopSlowSql;
import com.asetune.pcs.report.content.os.OsCpuUsageOverview;
import com.asetune.pcs.report.content.os.OsIoStatSlowIo;
import com.asetune.pcs.report.content.ase.AseTopSlowProcCalls;

public class DailySummaryReportAseTune 
extends DailySummaryReportDefault
{
	
	@Override
	public void addReportEntries()
	{
		// Add the Alarms Active/History
		super.addReportEntries();

		// ASE Error Info
		addReportEntry( new AseErrorInfo(this)              );

		// CPU, just an overview
		addReportEntry( new AseCpuUsageOverview(this)       );
		addReportEntry( new OsCpuUsageOverview(this)        );
		
		// SQL: from mon SysStatements...
		addReportEntry( new AseTopSlowSql(this)             );
		addReportEntry( new AseTopSlowProcCalls(this)       );
		// SQL: from Cm's
		addReportEntry( new AseTopCmCachedProcs(this)       );
		addReportEntry( new AseTopCmStmntCacheDetails(this) );
		addReportEntry( new AseTopCmActiveStatements(this)  );
		// SQL: Accessed Tables
		addReportEntry( new AseTopCmObjectActivity(this)    );

		// Disk IO Activity (Slow devices & Overall charts)
		addReportEntry( new AseSlowCmDeviceIo(this)         );
		addReportEntry( new OsIoStatSlowIo(this)            );

		// Database Size
		addReportEntry( new AseDbSize(this)                 );
	}
}
