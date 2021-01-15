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
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;

public class SqlServerTopCmTableSize 
extends SqlServerAbstract
{
	private ResultSetTableModel _shortRstm;

	public SqlServerTopCmTableSize(DailySummaryReportAbstract reportingInstance)
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
			sb.append(getSectionDescriptionHtml(_shortRstm, true));
			
			sb.append("No rows found <br>\n");
		}
		else
		{
			// Get a description of this section, and column names
			sb.append(getSectionDescriptionHtml(_shortRstm, true));

//			sb.append("Row Count: " + _shortRstm.getRowCount() + "<br>\n");
			sb.append("Row Count: " + _shortRstm.getRowCount() + "&emsp;&emsp; To change number of <i>top</i> records, set property <code>" + getTopRowsPropertyName() + "=##</code><br>\n");
			sb.append(toHtmlTable(_shortRstm));
		}
	}

	@Override
	public String getSubject()
	{
		return "Top TABLE Size (order by: SizeInMb, origin: CmTableSize/dm_db_partition_stats)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}


	@Override
	public String[] getMandatoryTables()
	{
		return new String[] { "CmTableSize_abs" };
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
//		int topRows = localConf.getIntProperty(this.getClass().getSimpleName()+".top", 20);
		int topRows = getTopRows();
//		int havingAbove = 100;

		String sql = getCmDiffColumnsAsSqlComment("CmTableSize")
				+ "select top " + topRows + " \n"
			    + "    * \n"
			    + "from [CmTableSize_abs] x \n"
			    + "where [SessionSampleTime] = (select max([SessionSampleTime]) from [CmTableSize_abs]) \n"
			    + "order by [TotalReservedSizeMB] desc \n"
			    + "";

		_shortRstm = executeQuery(conn, sql, false, "TopTableSize");
		if (_shortRstm == null)
		{
			_shortRstm = ResultSetTableModel.createEmpty("TopTableSize");
			return;
		}
		else
		{
			// Remove some columns which we dont really need
			_shortRstm.removeColumnNoCase("SessionStartTime");
			_shortRstm.removeColumnNoCase("SessionSampleTime");
			_shortRstm.removeColumnNoCase("CmSampleMs");
			_shortRstm.removeColumnNoCase("CmNewDiffRateRow");
			_shortRstm.removeColumnNoCase("RowCountDiff");

			// Describe the table
			setSectionDescription(_shortRstm);

			// Calculate Duration
//			setDurationColumn(_shortRstm, "CmSampleTime_min", "CmSampleTime_max", "Duration");
		}
	}
	
	/**
	 * Set descriptions for the table, and the columns
	 */
	private void setSectionDescription(ResultSetTableModel rstm)
	{
		if (rstm == null)
			return;

		//dummy
//		if (_isTableSizeConfigured)
//			return;

		// Section description
		rstm.setDescription(
				"Table Size (ordered by: TotalReservedSizeMB) <br>" +
				"<br>" +
//				"Note 1: This is the same column layout as the previos 'Top TABLE/INDEX Activity', so the column 'UsageInMb_max' which we order by is quite far to the right...<br>" +
//				"Note 2: Listed tables needs to be above 100 MB<br>" + // see variable 'havingAbove' in the create() method 
//				"<br>" +
//				"Config: <code>" + CmObjectActivity.PROPKEY_sample_tabRowCount + " = " + _isTableSizeConfigured + " </code><br>" +
//				"Disable Time: " + _tableSizeDisableTimeStr + "<br>" +
//				"<br>" +
				"SQL-Server Source table is 'sys.dm_db_partition_stats'. <br>" +
				"PCS Source table is 'CmTableSize'. (PCS = Persistent Counter Store) <br>" +
				"<br>" +
//				"The report <i>summarizes</i> (min/max/count/sum/avg) all entries/samples from the <i>source_DIFF</i> table. <br>" +
//				"Typically the column name <i>postfix</i> will tell you what aggregate function was used. <br>" +
				"");

		// Columns description
//		rstm.setColumnDescription("CmSampleTime_min"          , "First entry was sampled.");
	}
}
