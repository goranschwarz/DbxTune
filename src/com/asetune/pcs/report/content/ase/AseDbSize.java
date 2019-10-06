/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
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

import com.asetune.gui.ResultSetTableModel;
import com.asetune.pcs.report.DailySummaryReportAbstract;
import com.asetune.pcs.report.content.ReportChartObject;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;

public class AseDbSize extends AseAbstract
{
//	private static Logger _logger = Logger.getLogger(AseCpuUsageOverview.class);

	private ResultSetTableModel _shortRstm;

	public AseDbSize(DailySummaryReportAbstract reportingInstance)
	{
		super(reportingInstance);
	}

	@Override
	public String getMessageText()
	{
		StringBuilder sb = new StringBuilder();

		// Get a description of this section, and column names
		sb.append(getSectionDescriptionHtml(_shortRstm, true));

		// Last sample Database Size info
		sb.append("Row Count: ").append(_shortRstm.getRowCount()).append("<br>\n");
		sb.append(_shortRstm.toHtmlTableString("sortable"));
		

		// link to DbxCentral graphs
		sb.append(getDbxCentralLinkWithDescForGraphs(false, "Below are Transaction Log and Data Size Usage of each Database during the day<br>\n"
		                                                  + "Presented as: \n"
		                                                  + "<ul> \n"
		                                                  + "  <li><b> Space Used in Percent   </b> - When this gets <b>high</b> we could be in trouble. But the below 'Space Left to use' is a better indicator.</li> \n"
		                                                  + "  <li><b> Space Left to use in MB </b> - When This gets <b>low</b> we could be in trouble. No space = No more modifications. </li> \n"
		                                                  + "  <li><b> Space used in MB        </b> - Just an indicator of how much MB we are actually using for the different databases.</li> \n"
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
		sb.append(_CmOpenDatabases_DbDataSizeUsedPctGraph.getHtmlContent(null, null));
		sb.append(_CmOpenDatabases_DbDataSizeLeftMbGraph .getHtmlContent(null, null));
		sb.append(_CmOpenDatabases_DbDataSizeUsedMbGraph .getHtmlContent(null, null));
		
		sb.append("<h4>DB Transaction Log Space Usage</h4> \n");
		sb.append(_CmOpenDatabases_DbLogSizeUsedPctGraph .getHtmlContent(null, null));
		sb.append(_CmOpenDatabases_DbLogSizeLeftMbGraph  .getHtmlContent(null, null));
		sb.append(_CmOpenDatabases_DbLogSizeUsedMbGraph  .getHtmlContent(null, null));

		sb.append("<h4>Tempdb Space Usage</h4> \n");
		sb.append(_CmOpenDatabases_TempdbUsedMbGraph     .getHtmlContent(null, null));
		
		return sb.toString();
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

		// Describe the table
		setSectionDescription(_shortRstm);
		
		_CmOpenDatabases_DbDataSizeUsedPctGraph = createChart(conn, "CmOpenDatabases", "DbDataSizeUsedPctGraph", 100, null, "DB Data Space used in Percent (Server->Databases)");
		_CmOpenDatabases_DbDataSizeLeftMbGraph  = createChart(conn, "CmOpenDatabases", "DbDataSizeLeftMbGraph" , -1,  null, "DB Data Space left to use in MB (Server->Databases)");
		_CmOpenDatabases_DbDataSizeUsedMbGraph  = createChart(conn, "CmOpenDatabases", "DbDataSizeUsedMbGraph" , -1,  null, "DB Data Space used in MB (Server->Databases)");

		_CmOpenDatabases_DbLogSizeUsedPctGraph  = createChart(conn, "CmOpenDatabases", "DbLogSizeUsedPctGraph" , 100, null, "DB Transaction Log Space used in Percent (Server->Databases)");
		_CmOpenDatabases_DbLogSizeLeftMbGraph   = createChart(conn, "CmOpenDatabases", "DbLogSizeLeftMbGraph"  , -1,  null, "DB Transaction Log Space left to use in MB (Server->Databases)");
		_CmOpenDatabases_DbLogSizeUsedMbGraph   = createChart(conn, "CmOpenDatabases", "DbLogSizeUsedMbGraph"  , -1,  null, "DB Transaction Log Space used in MB (Server->Databases)");

		_CmOpenDatabases_TempdbUsedMbGraph      = createChart(conn, "CmOpenDatabases", "TempdbUsedMbGraph"     , -1,  null, "TempDB Space used in MB (Server->Databases)");
	}

	private ReportChartObject _CmOpenDatabases_DbLogSizeUsedPctGraph;
	private ReportChartObject _CmOpenDatabases_DbLogSizeLeftMbGraph;
	private ReportChartObject _CmOpenDatabases_DbLogSizeUsedMbGraph;
	
	private ReportChartObject _CmOpenDatabases_DbDataSizeUsedPctGraph;
	private ReportChartObject _CmOpenDatabases_DbDataSizeLeftMbGraph;
	private ReportChartObject _CmOpenDatabases_DbDataSizeUsedMbGraph;

	private ReportChartObject _CmOpenDatabases_TempdbUsedMbGraph;

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

