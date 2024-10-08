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
import java.util.LinkedHashMap;

import com.asetune.gui.ResultSetTableModel;
import com.asetune.pcs.report.DailySummaryReportAbstract;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;

public class PostgresTopTableSize
extends PostgresAbstract
{
//	private static Logger _logger = Logger.getLogger(PostgresTopTableSize.class);

	private ResultSetTableModel _shortRstm;

	public PostgresTopTableSize(DailySummaryReportAbstract reportingInstance)
	{
		super(reportingInstance);
	}

	@Override
	public boolean hasMinimalMessageText()
	{
		return false;
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
		if (_shortRstm.getRowCount() == 0)
		{
			sb.append("No rows found <br>\n");
		}
		else
		{
			// Get a description of this section, and column names
			sb.append(getSectionDescriptionHtml(_shortRstm, true));

//			sb.append("Row Count: " + _shortRstm.getRowCount() + "<br>\n");
			sb.append("Row Count: " + _shortRstm.getRowCount() + "&emsp;&emsp; To change number of <i>top</i> records, set property <code>" + getTopRowsPropertyName() + "=##</code><br>\n");
			sb.append(toHtmlTable(_shortRstm));

			int sumTotalMb      = 0;
			int sumTotalDataMb  = 0;
			int sumTotalIndexMb = 0;
			int sumTotalToastMb = 0;
			for (int r=0; r<_shortRstm.getRowCount(); r++)
			{
				sumTotalMb      += _shortRstm.getValueAsInteger(r, "total_mb");
				sumTotalDataMb  += _shortRstm.getValueAsInteger(r, "data_mb");
				sumTotalIndexMb += _shortRstm.getValueAsInteger(r, "index_mb");
				sumTotalToastMb += _shortRstm.getValueAsInteger(r, "toast_mb");
			}

			LinkedHashMap<String, Object> summaryMap = new LinkedHashMap<>();
			summaryMap.put("Sum Size in MB",   sumTotalMb);
			summaryMap.put("Sum Data in MB",   sumTotalDataMb);
			summaryMap.put("Sum Index in MB",  sumTotalIndexMb);
			summaryMap.put("Sum TOAST in MB",  sumTotalToastMb + "&emsp;&emsp;&emsp;<i>(TOAST = The Oversized Attribute Storage Technique) or in short 'large rows that spans pages'.</i>");
			
			sb.append("<br>\n");
			sb.append(StringUtil.toHtmlTable(summaryMap));
			sb.append("<br>\n");
		}
	}

	@Override
	public String getSubject()
	{
		return "Largest Tables in any database (order by: total_mb)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
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
				"Top Tables in size are presented here (ordered by: total_mb) <br>" +
				"");

		// Columns description
		rstm.setColumnDescription("dbname"                     , "Database name this table is located in.");
		rstm.setColumnDescription("table_schema"               , "Schema name this table is located in.");
		rstm.setColumnDescription("table_name"                 , "Name of the table.");

		rstm.setColumnDescription("total_mb"                   , "MB of data + index + toast");
		rstm.setColumnDescription("row_estimate"               , "Number of rows in table");
		rstm.setColumnDescription("data_mb"                    , "Data size in MB");
		rstm.setColumnDescription("index_mb"                   , "Index size in MB");
		rstm.setColumnDescription("toast_mb"                   , "TOAST () size in MB (TOAST = The Oversized Attribute Storage Technique), or in short 'large rows that spans pages'.");
		rstm.setColumnDescription("oid"                        , "ID number, which can be found at the OS, in the 'data' directory. for the database #### (datid).");
	}

	@Override
	public String[] getMandatoryTables()
	{
		return new String[] { "CmPgTableSize_abs" };
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		int topRows = getTopRows();
//		int topRows = localConf.getIntProperty(this.getClass().getSimpleName()+".top", 30);

		String sql = ""
			    + "select top " + topRows + " \n"
			    + "     [dbname] \n"
			    + "    ,[table_schema] \n"
			    + "    ,[table_name] \n"
			    + "    ,[total_mb] \n"
			    + "    ,[row_estimate] \n"
			    + "    ,[data_mb] \n"
			    + "    ,[index_mb] \n"
			    + "    ,[toast_mb] \n"
			    + "    ,[oid] \n"
			    + "from [CmPgTableSize_abs] \n"
			    + "where [SessionSampleTime] = (select max([SessionSampleTime]) from [CmPgTableSize_abs]) \n"
			    + "order by [total_mb] desc \n"
			    + "";

		_shortRstm = executeQuery(conn, sql, false, "CmPgTableSize_abs");
		if (_shortRstm == null)
		{
			_shortRstm = ResultSetTableModel.createEmpty("CmPgTableSize_abs");
			return;
		}
		else
		{
			// Highlight sort column
			_shortRstm.setHighlightSortColumns("total_mb");

			// Describe the table
			setSectionDescription(_shortRstm);
		}
	}
}
