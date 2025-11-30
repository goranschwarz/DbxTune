/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
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
package com.dbxtune.pcs.report.content.ase;

import java.io.IOException;
import java.io.Writer;

import com.dbxtune.cm.ase.CmOpenDatabases;
import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.pcs.report.content.IReportChart;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;

public class AseDbSize extends AseAbstract
{
//	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private ResultSetTableModel _shortRstm;

	public AseDbSize(DailySummaryReportAbstract reportingInstance)
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
		// Get a description of this section, and column names
		sb.append(getSectionDescriptionHtml(_shortRstm, true));

		// Last sample Database Size info
		sb.append("Row Count: " + _shortRstm.getRowCount() + "<br>\n");
		sb.append(toHtmlTable(_shortRstm));
		

		// link to DbxCentral graphs
		sb.append(getDbxCentralLinkWithDescForGraphs(false, "Below are Transaction Log and Data Size Usage of each Database during the day<br>\n"
		                                                  + "Presented as: \n"
		                                                  + "<ul> \n"
		                                                  + "  <li><b> Space Used in Percent </b> - When this gets <b>high</b> we could be in trouble. But the below 'Space Available' is a better indicator.</li> \n"
		                                                  + "  <li><b> Space Available in MB </b> - When This gets <b>low</b> we could be in trouble. No space = No more modifications. </li> \n"
		                                                  + "  <li><b> Space Used in MB      </b> - Just an indicator of how much MB we are actually using for the different databases.</li> \n"
		                                                  + "</ul> \n",
				"CmOpenDatabases_DbDataSizeUsedPctGraph",
				"CmOpenDatabases_DbDataSizeLeftMbGraph",
				"CmOpenDatabases_DbDataSizeUsedMbGraph",
				
				"CmOpenDatabases_DbLogSizeUsedPctGraph",
				"CmOpenDatabases_DbLogSizeLeftMbGraph",
				"CmOpenDatabases_DbLogSizeUsedMbGraph",

				"CmOpenDatabases_TempdbUsedMbGraph"
				));

		sb.append("<h4>DB Data Space Usage</h4> \n");
		_CmOpenDatabases_DbDataSizeUsedPctGraph.writeHtmlContent(sb, null, null);
		_CmOpenDatabases_DbDataSizeLeftMbGraph .writeHtmlContent(sb, null, null);
		_CmOpenDatabases_DbDataSizeUsedMbGraph .writeHtmlContent(sb, null, null);
		
		sb.append("<h4>DB Transaction Log Space Usage</h4> \n");
		_CmOpenDatabases_DbLogSizeUsedPctGraph .writeHtmlContent(sb, null, null);
		_CmOpenDatabases_DbLogSizeLeftMbGraph  .writeHtmlContent(sb, null, null);
		_CmOpenDatabases_DbLogSizeUsedMbGraph  .writeHtmlContent(sb, null, null);

		sb.append("<h4>Tempdb Space Usage</h4> \n");
		_CmOpenDatabases_TempdbUsedMbGraph     .writeHtmlContent(sb, null, null);
	}

	@Override
	public String getSubject()
	{
		return "Database Size in MB (origin: CmOpenDatabases / monOpenDatabases)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}


	@Override
	public String[] getMandatoryTables()
	{
		return new String[] { "CmOpenDatabases_abs" };
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		String sql = ""
			    + "select \n"
			    + "    [DBName], \n"
//			    + "    [DBID], \n"
//			    + "    [IsUserTempdb], \n"
//			    + "    [TransactionLogFull], \n"
			    + "\n"
			    + "    [DbSizeInMb], \n"
			    + "    [DataSizeInMb], \n"
			    + "    [LogSizeInMb], \n"
			    + "    [LogDataIsMixed], \n"
			    + "\n"
			    + "    [DataSizeUsedPct], \n"
			    + "    [DataSizeFreeInMb], \n"
			    + "    [DataSizeUsedInMb], \n"
			    + "\n"
			    + "    [LogSizeUsedPct], \n"
			    + "    [LogSizeFreeInMb], \n"
			    + "    [LogSizeUsedInMb], \n"
			    + "\n"
			    + "    [LastBackupFailed], \n"
			    + "    [BackupStartTime], \n"
			    + "    [LastTranLogDumpTime], \n"
			    + "    [LastDbBackupAgeInHours], \n"
			    + "    [LastLogBackupAgeInHours] \n"
			    + "\n"
			    + "from [CmOpenDatabases_abs] \n"
			    + "where [SessionSampleTime] = (select max([SessionSampleTime]) from [CmOpenDatabases_abs]) \n"
			    + "order by [DbSizeInMb] desc \n"
			    + "";
		
		_shortRstm = executeQuery(conn, sql, true, "CmOpenDatabases_abs");

		// Highlight sort column
		if (_shortRstm != null)
			_shortRstm.setHighlightSortColumns("DbSizeInMb");
			
		// Describe the table
		setSectionDescription(_shortRstm);

		String schema = getReportingInstance().getDbmsSchemaName();
		
		_CmOpenDatabases_DbDataSizeUsedPctGraph = createTsLineChart(conn, schema, CmOpenDatabases.CM_NAME, CmOpenDatabases.GRAPH_NAME_DATASIZE_USED_PCT, 100, true, null, "DB Data Space Used in Percent (Server->Databases)");
		_CmOpenDatabases_DbDataSizeLeftMbGraph  = createTsLineChart(conn, schema, CmOpenDatabases.CM_NAME, CmOpenDatabases.GRAPH_NAME_DATASIZE_LEFT_MB,   -1, true, null, "DB Data Space Available in MB (Server->Databases)");
		_CmOpenDatabases_DbDataSizeUsedMbGraph  = createTsLineChart(conn, schema, CmOpenDatabases.CM_NAME, CmOpenDatabases.GRAPH_NAME_DATASIZE_USED_MB,   -1, true, null, "DB Data Space Used in MB (Server->Databases)");

		_CmOpenDatabases_DbLogSizeUsedPctGraph  = createTsLineChart(conn, schema, CmOpenDatabases.CM_NAME, CmOpenDatabases.GRAPH_NAME_LOGSIZE_USED_PCT,  100, true, null, "DB Transaction Log Space Used in Percent (Server->Databases)");
		_CmOpenDatabases_DbLogSizeLeftMbGraph   = createTsLineChart(conn, schema, CmOpenDatabases.CM_NAME, CmOpenDatabases.GRAPH_NAME_LOGSIZE_LEFT_MB,    -1, true, null, "DB Transaction Log Space Available in MB (Server->Databases)");
		_CmOpenDatabases_DbLogSizeUsedMbGraph   = createTsLineChart(conn, schema, CmOpenDatabases.CM_NAME, CmOpenDatabases.GRAPH_NAME_LOGSIZE_USED_MB,    -1, true, null, "DB Transaction Log Space Used in MB (Server->Databases)");

		_CmOpenDatabases_TempdbUsedMbGraph      = createTsLineChart(conn, schema, CmOpenDatabases.CM_NAME, CmOpenDatabases.GRAPH_NAME_TEMPDB_USED_MB,     -1, true, null, "TempDB Space Used in MB (Server->Databases)");
	}

	private IReportChart _CmOpenDatabases_DbLogSizeUsedPctGraph;
	private IReportChart _CmOpenDatabases_DbLogSizeLeftMbGraph;
	private IReportChart _CmOpenDatabases_DbLogSizeUsedMbGraph;
	
	private IReportChart _CmOpenDatabases_DbDataSizeUsedPctGraph;
	private IReportChart _CmOpenDatabases_DbDataSizeLeftMbGraph;
	private IReportChart _CmOpenDatabases_DbDataSizeUsedMbGraph;

	private IReportChart _CmOpenDatabases_TempdbUsedMbGraph;

	/**
	 * Set descriptions for the table, and the columns
	 */
	private void setSectionDescription(ResultSetTableModel rstm)
	{
		if (rstm == null)
			return;
		
		// Section description
		rstm.setDescription(
				"Information from last collector sample from the table <code>CmOpenDatabases_abs</code><br>" +
				"This will show you sizes of all databases and there last Usage, on Data and Log <br>" +
				"");
	}
}

