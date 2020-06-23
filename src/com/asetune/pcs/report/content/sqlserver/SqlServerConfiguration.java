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
package com.asetune.pcs.report.content.sqlserver;

import com.asetune.gui.ResultSetTableModel;
import com.asetune.pcs.report.DailySummaryReportAbstract;
import com.asetune.pcs.report.content.ase.AseAbstract;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;

public class SqlServerConfiguration extends AseAbstract
{
//	private static Logger _logger = Logger.getLogger(SqlServerConfiguration.class);

	ResultSetTableModel _shortRstm;

	public SqlServerConfiguration(DailySummaryReportAbstract reportingInstance)
	{
		super(reportingInstance);
	}

	@Override
	public String getMessageText()
	{
		StringBuilder sb = new StringBuilder();

		if (_shortRstm.getRowCount() == 0)
		{
			sb.append("No rows found <br>\n");
		}
		else
		{
			// Get a description of this section, and column names
			sb.append(getSectionDescriptionHtml(_shortRstm, true));

			sb.append("Row Count: ").append(_shortRstm.getRowCount()).append("<br>\n");
			sb.append(_shortRstm.toHtmlTableString("sortable"));
		}

		return sb.toString();
	}

	@Override
	public String getSubject()
	{
		return "SQL-Server Configuration (Only non-defaults)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}


	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		// Get 'statement cache size' from saved configuration 
		String sql = "select * \n"
				   + "from [MonSessionDbmsConfig] \n"
				   + "where [SessionStartTime] = (select max([SessionStartTime]) from [MonSessionDbmsConfig]) \n"
				   + "  and [NonDefault] = 1 \n"
				   + "order by [Name]";
		_shortRstm = executeQuery(conn, sql, false, "SqlServerConfig");

		if (_shortRstm == null)
		{
			_shortRstm = ResultSetTableModel.createEmpty("SqlServerConfig");
			return;
		}
	}
}
