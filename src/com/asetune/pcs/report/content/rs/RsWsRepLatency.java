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
package com.asetune.pcs.report.content.rs;

import java.io.IOException;
import java.io.Writer;

import com.asetune.pcs.report.DailySummaryReportAbstract;
import com.asetune.pcs.report.content.IReportChart;
import com.asetune.pcs.report.content.ase.AseAbstract;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;

public class RsWsRepLatency extends AseAbstract
{
//	private static Logger _logger = Logger.getLogger(RsWsRepLatency.class);

	public RsWsRepLatency(DailySummaryReportAbstract reportingInstance)
	{
		super(reportingInstance);
	}

	@Override
	public boolean hasShortMessageText()
	{
		return true;
	}

//	@Override
//	public void writeShortMessageText(Writer w)
//	throws IOException
//	{
//		writeMessageText(w);
//	}

	@Override
	public void writeMessageText(Writer sb, MessageType messageType)
	throws IOException
	{
		sb.append(getDbxCentralLinkWithDescForGraphs(false, "Below are Database Tranfer <i>Latency</i> or Age of Data at the Standby.",
				"CmWsRepLatency_DataAgeInSec"
				));

		_CmWsRepLatency_DataAgeInSec.writeHtmlContent(sb, null, null);
	}

	@Override
	public String getSubject()
	{
		return "Database Tranfer Latency or Age of Data at the Standby (origin: CmWsRepLatency)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}


	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		_CmWsRepLatency_DataAgeInSec = createTsLineChart(conn, "CmWsRepLatency", "DataAgeInSec", -1, true, null, "Data Age In Seconds, from Active->Standby");
	}

	private IReportChart _CmWsRepLatency_DataAgeInSec;
}
