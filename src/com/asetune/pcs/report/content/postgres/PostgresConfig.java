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
package com.asetune.pcs.report.content.postgres;

import java.io.IOException;
import java.io.Writer;

import com.asetune.gui.ResultSetTableModel;
import com.asetune.pcs.report.DailySummaryReportAbstract;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;

public class PostgresConfig 
extends PostgresAbstract
{
//	private static Logger _logger = Logger.getLogger(PostgresConfig.class);

	private ResultSetTableModel _shortRstm;

	public PostgresConfig(DailySummaryReportAbstract reportingInstance)
	{
		super(reportingInstance);
	}

	@Override
	public boolean hasShortMessageText()
	{
		return false;
	}

//	@Override
//	public void writeShortMessageText(Writer w)
//	throws IOException
//	{
//	}

	@Override
	public void writeMessageText(Writer sb, MessageType messageType)
	throws IOException
	{
		// Get a description of this section, and column names
		sb.append(getSectionDescriptionHtml(_shortRstm, true));

		// Last sample Database Size info
		sb.append("Row Count: " + _shortRstm.getRowCount() + "<br>\n");
		sb.append(toHtmlTable(_shortRstm));
	}

	@Override
	public String getSubject()
	{
		return "Postgres Server Configuration (origin: MonSessionDbmsConfig / pg_settings)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}


	@Override
	public String[] getMandatoryTables()
	{
		return new String[] { "MonSessionDbmsConfig" };
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		// Get some configuration
		String categoryList  = localConf.getProperty(this.getClass().getSimpleName()+".category.list",       "'File Locations', 'Statistics / Query and Index Statistics Collector'");
		String paramNameList = localConf.getProperty(this.getClass().getSimpleName()+".parameter.name.list", "'max_connections'");

		String sql = ""
			+ "select \n"
			+ "     [Category] \n"
			+ "    ,[ParameterName] \n"
			+ "    ,[NonDefault] \n"
			+ "    ,[CurrentValue] \n"
			+ "    ,[Description] \n"
			+ "    ,[ExtraDescription] \n"
			+ "from [MonSessionDbmsConfig] \n"
			+ "where [SessionStartTime] = (select max([SessionStartTime]) from [MonSessionDbmsConfig]) \n"
			+ " and (    [Category] in("      + categoryList  + ") \n"
			+ "       or [ParameterName] in(" + paramNameList + ") \n"
			+ "     ) \n"
			+ "order by [Category], [ParameterName] \n"
			+ "";
		
		_shortRstm = executeQuery(conn, sql, true, "MonSessionDbmsConfig");

		// Describe the table
		setSectionDescription(_shortRstm);
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
				"Basic Postgres Server Configuration parameters.<br>" +
				"For example 'File Locations' to easier locate where database files are located on the Host Operating System." +
				"");
	}
}
