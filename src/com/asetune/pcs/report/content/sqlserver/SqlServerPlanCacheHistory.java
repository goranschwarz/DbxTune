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

import java.io.IOException;
import java.io.Writer;

import com.asetune.gui.ResultSetTableModel;
import com.asetune.pcs.report.DailySummaryReportAbstract;
import com.asetune.pcs.report.content.ase.AseAbstract;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;

public class SqlServerPlanCacheHistory extends AseAbstract
{
//	private static Logger _logger = Logger.getLogger(SqlServerPlanCacheHistory.class);

	ResultSetTableModel _shortRstm;

	public SqlServerPlanCacheHistory(DailySummaryReportAbstract reportingInstance)
	{
		super(reportingInstance);
	}

	@Override
	public boolean hasShortMessageText()
	{
		return true;
	}

	@Override
	public void writeMessageText(Writer sb, MessageType messageType)
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
		return "Plan Cache History";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}


	@Override
	public String[] getMandatoryTables()
	{
		return new String[] { "CmPlanCacheHistory_abs" };
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		// Get 'statement cache size' from saved configuration 
		String sql = "" 
			+ "select [creation_date] \n"
			+ "      ,[creation_hour] \n"
			+ "      ,max([plan_count])      as [plan_count] \n"
			+ "      ,sum([exec_count_diff]) as [exec_count] \n"
			+ "from [CmPlanCacheHistory_diff] \n"
			+ "group by [creation_date], [creation_hour] \n"
//			+ "where [SessionStartTime] = (select max([SessionStartTime]) from [CmPlanCacheHistory_abs]) \n"
			+ "order by [creation_date] desc, [creation_hour] desc";

		_shortRstm = executeQuery(conn, sql, false, "PlanCacheHistory");

		if (_shortRstm == null)
		{
			_shortRstm = ResultSetTableModel.createEmpty("PlanCacheHistory");
			return;
		}
		else
		{
			setSectionDescription(_shortRstm);
		}
	}

	/**
	 * Set descriptions for the table, and the columns
	 */
	private void setSectionDescription(ResultSetTableModel rstm)
	{
		if (rstm == null)
			return;
		
		// Section description
		rstm.setDescription(
				"How much Query Plan History does SQL Server have available? <br>" +
				"Looking at the top ## plans in the cache doesn't do me much good if: \n" +
				"<ul> \n" +
				"    <li>Someone restarted the server recently</li> \n" +
				"    <li>Someone ran DBCC FREEPROCCACHE</li> \n" +
				"    <li>Somebody's addicted to rebuilding indexes and updating stats (which invalidates plans for all affected objects)</li> \n" +
				"    <li>The server's under extreme memory pressure</li> \n" +
				"    <li>The developers <i>aren't parameterizing their queries</i></li> \n" +
				"    <li>The app has an <i>old version of NHibernate with the parameterization bug</i></li> \n" +
				"    <li>The .NET app calls <i>Parameters.Add without setting the parameter size</i></li> \n" +
//				"    <li>The Java app gets <code>CONVERT_IMPLICIT</code> when call <i><code>stmnt.setString()</code> on columns that has VARCHAR... Workaround: set connection parameter <code>sendStringParametersAsUnicode=false</code></i></li> \n" +
				"</ul> \n" +
				"<br>" +
				"The <code>plan_count</code> should not vary to much, and we should have a resonable number of days in the history. <br>" +
				"<br>" +
				"SQL Source table is 'sys.dm_exec_query_stats'. <br>" +
				"PCS Source table is 'CmPlanCacheHistory_diff'. (PCS = Persistent Counter Store) <br>" +
				"");

		// Columns description
		rstm.setColumnDescription("creation_date"       , "Date that 'plan_count' is for.");
		rstm.setColumnDescription("creation_hour"       , "If it's 0 or above it means hour of the day, -1 means: full day.");
		rstm.setColumnDescription("plan_count"          , "How many plans was created during this time frame (creation_date & creation_hour)");
		rstm.setColumnDescription("exec_count"          , "How many Executions has been done using plans created in this time frame (creation_date & creation_hour)");
	}

}
