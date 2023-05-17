/*******************************************************************************
DailySummaryReportHanaTune * Copyright (C) 2010-2020 Goran Schwarz
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
package com.asetune.central.pcs.report;

import com.asetune.pcs.MonRecordingInfo;
import com.asetune.pcs.report.DailySummaryReportDefault;
import com.asetune.pcs.report.content.AlarmsActive;
import com.asetune.pcs.report.content.AlarmsHistory;
import com.asetune.pcs.report.content.DbxTunePcsTablesSize;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;

/**
 * Override "stuff" in DailySummaryReportDefault
 */
public class DailySummaryReportCentralDefault 
extends DailySummaryReportDefault
{

	@Override
	public void addReportEntriesTop()
	{
		addReportEntry( new DbxCentralRecordingInfo(this) );
		addReportEntry( new AlarmsActive(this)  );
		addReportEntry( new AlarmsHistory(this) );
	}

	@Override
	public void addReportEntries()
	{
	}

	@Override
	public void addReportEntriesBottom()
	{
//		addReportEntry( new DbxTuneErrors(this) );
		addReportEntry( new DbxTunePcsTablesSize(this) );
	}

	@Override
	public Configuration getConfigFromPcs(String type)
	{
		Configuration conf = new Configuration();
		
		return conf;
	}

	@Override
	public MonRecordingInfo getRecordingInfo(DbxConnection conn)
	{
//		TODO;// Implement a MonRecordingDbxCentralInfo object (with possibly a interface of MonRecordingInfo as RecordingInfo)
		//return new MonRecordingInfo(conn, null);
		return null;
	}

	/**
	 * We don't need to get to the Recording to detect this... Since it will be running locally
	 * @return
	 */
	@Override
	public boolean isWindows()
	{
		String OS = System.getProperty("os.name").toLowerCase();

		return OS.contains("win");
	}
	
//	/**
//	 * @return First try getReportPeriodBeginTime(), if not available use getRecordingStartTime()
//	 */
//	@Override
//	public Timestamp getReportPeriodOrRecordingBeginTime() 
//	{
//new Exception("TODO: NEEDS TO BE PROPERLY IMPLEMENTED").printStackTrace();
//		return new Timestamp(System.currentTimeMillis());
//	}
//
//	/**
//	 * @return First try getReportPeriodEndTime(), if not available use getRecordingEndTime()
//	 */
//	@Override
//	public Timestamp getReportPeriodOrRecordingEndTime()
//	{ 
//new Exception("TODO: NEEDS TO BE PROPERLY IMPLEMENTED").printStackTrace();
//		return new Timestamp(System.currentTimeMillis());
//	}

}
