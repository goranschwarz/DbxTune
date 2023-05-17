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
package com.asetune.pcs.report.content;

import java.io.IOException;
import java.io.Writer;

import com.asetune.central.lmetrics.LocalMetricsPersistWriterJdbc;
import com.asetune.gui.ResultSetTableModel;
import com.asetune.pcs.report.DailySummaryReportAbstract;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;

public class DbxTunePcsTablesSize
extends ReportEntryAbstract
{
	private ResultSetTableModel _rstm;
	

	public DbxTunePcsTablesSize(DailySummaryReportAbstract reportingInstance)
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
		if (_rstm.getRowCount() == 0)
		{
			sb.append("No PCS Tables found Alarms <br>\n");
		}
		else
		{
			// Get a description of this section, and column names
			sb.append(getSectionDescriptionHtml(_rstm, true));
			
			sb.append("Table Count: " + _rstm.getRowCount() + "<br>\n");
			sb.append(toHtmlTable(_rstm));
		}
	}

	@Override
	public String getSubject()
	{
		return "Persistent Counter Storage (PCS) Tables Size";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false;
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		String extraWhereClause = "";

		// Should we reduce the content if it's for 'DbxCentral'
		if ("DbxCentral".equals(srvName))
		{
			extraWhereClause = "  AND [TABLE_SCHEMA] IN ('PUBLIC', '" + LocalMetricsPersistWriterJdbc.LOCAL_METRICS_SCHEMA_NAME + "') \n";
		}

		String sql = ""
			    + "SELECT \n"
			    + "     [TABLE_SCHEMA] \n"
			    + "    ,[TABLE_NAME] \n"
			    + "    ,[ROW_COUNT_ESTIMATE] \n"
			    + "    ,cast(DISK_SPACE_USED(QUOTE_IDENT([TABLE_SCHEMA])||'.'||QUOTE_IDENT([TABLE_NAME]))/1024.0/1024.0 as numeric(12,1)) as [DiskSpaceUsedMb] \n"
			    + "FROM [INFORMATION_SCHEMA].[TABLES] \n"
			    + "WHERE ([TABLE_NAME] LIKE '%_abs' OR [TABLE_NAME] like 'Mon%' OR [TABLE_NAME] like 'Dbx%') \n"
			    + extraWhereClause
			    + "ORDER BY [DiskSpaceUsedMb] desc \n"
			    + "";

		_rstm = executeQuery(conn, sql, true, "PCS Tables Size");

		// Describe the table
		setSectionDescription(_rstm);
	}

	/**
	 * Set descriptions for the table, and the columns
	 */
	private void setSectionDescription(ResultSetTableModel rstm)
	{
		if (rstm == null)
			return;
		
		rstm.setDescription("For <i>debug</i> purposes, this will report approximate Table Sizes on what's stored in the recording database.<br>"
				+ "This can help us to discover if we want to disable som Counters or polling less data from the DBMS (how often we sample data).");
	}
}
