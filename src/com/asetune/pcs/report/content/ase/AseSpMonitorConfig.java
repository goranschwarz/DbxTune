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
package com.asetune.pcs.report.content.ase;

import java.io.IOException;
import java.io.Writer;

import com.asetune.gui.ResultSetTableModel;
import com.asetune.pcs.report.DailySummaryReportAbstract;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;

public class AseSpMonitorConfig extends AseAbstract
{
//	private static Logger _logger = Logger.getLogger(AseStatementCacheUsageOverview.class);

	ResultSetTableModel _shortRstm;

	public AseSpMonitorConfig(DailySummaryReportAbstract reportingInstance)
	{
		super(reportingInstance);
	}

	@Override
	public boolean hasShortMessageText()
	{
		return false;
	}

	@Override
	public void writeShortMessageText(Writer w)
	throws IOException
	{
	}

	@Override
	public void writeMessageText(Writer sb)
	throws IOException
	{
		if (_shortRstm.getRowCount() == 0)
		{
			sb.append("No rows found <br>\n");
		}
		else
		{
			// Get a description of this section, and column names
			sb.append(getSectionDescriptionHtml(_shortRstm, true));

			sb.append("Row Count: " + _shortRstm.getRowCount() + "<br>\n");
			sb.append(toHtmlTable(_shortRstm));
		}
	}

	@Override
	public String getSubject()
	{
		return "sp_monitorconfig (last known)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}


	@Override
	public String[] getMandatoryTables()
	{
		return new String[] { "CmSpMonitorConfig_abs" };
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		// SessionStartTime	SessionSampleTime	CmSampleTime	CmSampleMs	CmNewDiffRateRow	Name	Num_free	Num_active	Pct_act	Max_Used	Reuse_cnt	Instance_Name
		String sql = "select [SessionSampleTime], [Name], [Num_free], [Num_active], [Pct_act], [Max_Used], [Reuse_cnt], [Instance_Name] \n"
				   + "from [CmSpMonitorConfig_abs] \n"
				   + "where [SessionSampleTime] = (select max([SessionSampleTime]) from [CmSpMonitorConfig_abs])";

		_shortRstm = executeQuery(conn, sql, false, "SpMonitorConfig");

		if (_shortRstm == null)
		{
			_shortRstm = ResultSetTableModel.createEmpty("SpMonitorConfig");
			return;
		}
	}
}
