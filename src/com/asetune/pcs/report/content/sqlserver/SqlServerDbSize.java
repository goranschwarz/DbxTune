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
package com.asetune.pcs.report.content.sqlserver;

import com.asetune.gui.ResultSetTableModel;
import com.asetune.pcs.report.DailySummaryReportAbstract;
import com.asetune.pcs.report.content.ReportChartObject;
import com.asetune.pcs.report.content.ase.AseAbstract;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;

public class SqlServerDbSize extends AseAbstract
{
//	private static Logger _logger = Logger.getLogger(AseCpuUsageOverview.class);

	private ResultSetTableModel _shortRstm;

	public SqlServerDbSize(DailySummaryReportAbstract reportingInstance)
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
		                                                  + "FIXME: Presented as: \n"
		                                                  + "<ul> \n"
		                                                  + "  <li>FIXME: <b> Space Used in Percent   </b> - When this gets <b>high</b> we could be in trouble. But the below 'Space Left to use' is a better indicator.</li> \n"
		                                                  + "  <li>FIXME: <b> Space Left to use in MB </b> - When This gets <b>low</b> we could be in trouble. No space = No more modifications. </li> \n"
		                                                  + "  <li>FIXME: <b> Space used in MB        </b> - Just an indicator of how much MB we are actually using for the different databases.</li> \n"
		                                                  + "</ul> \n",
				"CmDatabases_DbSizeMb",

				"CmDatabases_DbDataSizeUsedPctGraph",
				"CmDatabases_DbDataSizeLeftMbGraph",
				"CmDatabases_DbDataSizeUsedMbGraph",

				"CmDatabases_DbLogSizeUsedPctGraph",
				"CmDatabases_DbLogSizeLeftMbGraph",
				"CmDatabases_DbLogSizeUsedMbGraph",

				"CmDatabases_TempdbUsedMbGraph",
		
				"CmDatabases_OsDiskUsedPct",
				"CmDatabases_OsDiskFreeMb",
				"CmDatabases_OsDiskUsedMb"
				));

		sb.append("<h4>DB Size</h4> \n");
		sb.append(_CmDatabases_DbSizeMb              .getHtmlContent(null, null));

		sb.append("<h4>DB Data Space Usage</h4> \n");
		sb.append(_CmDatabases_DbDataSizeUsedPctGraph.getHtmlContent(null, null));
		sb.append(_CmDatabases_DbDataSizeLeftMbGraph .getHtmlContent(null, null));
		sb.append(_CmDatabases_DbDataSizeUsedMbGraph .getHtmlContent(null, null));
		
		sb.append("<h4>DB Transaction Log Space Usage</h4> \n");
		sb.append(_CmDatabases_DbLogSizeUsedPctGraph .getHtmlContent(null, null));
		sb.append(_CmDatabases_DbLogSizeLeftMbGraph  .getHtmlContent(null, null));
		sb.append(_CmDatabases_DbLogSizeUsedMbGraph  .getHtmlContent(null, null));

		sb.append("<h4>Tempdb Space Usage</h4> \n");
		sb.append(_CmDatabases_TempdbUsedMbGraph     .getHtmlContent(null, null));
		
		sb.append("<h4>DB OS Disk Space Usage (if we get near full here, we are in trouble)</h4> \n");
		sb.append(_CmDatabases_OsDiskUsedPct         .getHtmlContent(null, null));
		sb.append(_CmDatabases_OsDiskFreeMb          .getHtmlContent(null, null));
		sb.append(_CmDatabases_OsDiskUsedMb          .getHtmlContent(null, null));
		
		return sb.toString();
	}

	@Override
	public String getSubject()
	{
		return "Database Size in MB (origin: CmDatabases)";
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
			    + "     [DBName] \n"
			    + "    ,[compatibility_level] \n"
			    + "    ,[recovery_model_desc] \n"
			    + "    ,[DataFileGroupCount] \n"
			    + "\n"
			    + "    ,[DbSizeInMb] \n"
			    + "    ,[DataSizeInMb] \n"
			    + "    ,[LogSizeInMb] \n"
			    + "\n"
			    + "    ,[DataSizeUsedPct] \n"
			    + "    ,[DataSizeFreeInMb] \n"
			    + "    ,[DataSizeUsedInMb] \n"
			    + "\n"
			    + "    ,[LogSizeUsedPct] \n"
			    + "    ,[LogSizeFreeInMb] \n"
			    + "    ,[LogSizeUsedInMb] \n"
			    + "\n"
			    + "    ,[DataOsDisk] \n"
			    + "    ,[DataOsDiskFreeMb] \n"
			    + "    ,[DataOsDiskUsedMb] \n"
			    + "    ,[DataOsDiskFreePct] \n"
			    + "    ,[DataOsDiskUsedPct] \n"
			    + "    ,[DataNextGrowthSizeMb] \n"
			    + "\n"
			    + "    ,[LogOsDisk] \n"
			    + "    ,[LogOsDiskFreeMb] \n"
			    + "    ,[LogOsDiskUsedMb] \n"
			    + "    ,[LogOsDiskFreePct] \n"
			    + "    ,[LogOsDiskUsedPct] \n"
			    + "    ,[LogNextGrowthSizeMb] \n"
			    + "\n"
			    + "    ,[DataOsFileName] \n"
			    + "    ,[LogOsFileName] \n"
			    + "from [CmDatabases_abs] x \n"
			    + "where [SessionSampleTime] = (select max([SessionSampleTime]) from [CmDatabases_abs]) \n"
			    + "order by [DbSizeInMb] desc \n"
			    + "";
		
		_shortRstm = executeQuery(conn, sql, true, "CmDatabases_abs");

		// Describe the table
		setSectionDescription(_shortRstm);
		
		_CmDatabases_DbSizeMb               = createChart(conn, "CmDatabases", "DbSizeMb"              , -1,  null, "Database Size in MB (Server->Databases)");

		_CmDatabases_DbDataSizeUsedPctGraph = createChart(conn, "CmDatabases", "DbDataSizeUsedPctGraph", 100, null, "DB Data Space used in Percent (Server->Databases)");
		_CmDatabases_DbDataSizeLeftMbGraph  = createChart(conn, "CmDatabases", "DbDataSizeLeftMbGraph" , -1,  null, "DB Data Space left to use in MB (Server->Databases)");
		_CmDatabases_DbDataSizeUsedMbGraph  = createChart(conn, "CmDatabases", "DbDataSizeUsedMbGraph" , -1,  null, "DB Data Space used in MB (Server->Databases)");

		_CmDatabases_DbLogSizeUsedPctGraph  = createChart(conn, "CmDatabases", "DbLogSizeUsedPctGraph" , 100, null, "DB Transaction Log Space used in Percent (Server->Databases)");
		_CmDatabases_DbLogSizeLeftMbGraph   = createChart(conn, "CmDatabases", "DbLogSizeLeftMbGraph"  , -1,  null, "DB Transaction Log Space left to use in MB (Server->Databases)");
		_CmDatabases_DbLogSizeUsedMbGraph   = createChart(conn, "CmDatabases", "DbLogSizeUsedMbGraph"  , -1,  null, "DB Transaction Log Space used in MB (Server->Databases)");

		_CmDatabases_TempdbUsedMbGraph      = createChart(conn, "CmDatabases", "TempdbUsedMbGraph"     , -1,  null, "TempDB Space used in MB (Server->Databases)");

		_CmDatabases_OsDiskUsedPct          = createChart(conn, "CmDatabases", "OsDiskUsedPct"         , 100, null, "DB OS Disk Space used in Percent (Server->Databases)");
		_CmDatabases_OsDiskFreeMb           = createChart(conn, "CmDatabases", "OsDiskFreeMb"          , -1,  null, "DB OS Disk Space free/left in MB (Server->Databases)");
		_CmDatabases_OsDiskUsedMb           = createChart(conn, "CmDatabases", "OsDiskUsedMb"          , -1,  null, "DB OS Disk Space used in MB (Server->Databases)");
	}

	private ReportChartObject _CmDatabases_DbSizeMb;

	private ReportChartObject _CmDatabases_DbDataSizeUsedPctGraph;
	private ReportChartObject _CmDatabases_DbDataSizeLeftMbGraph;
	private ReportChartObject _CmDatabases_DbDataSizeUsedMbGraph;

	private ReportChartObject _CmDatabases_DbLogSizeUsedPctGraph;
	private ReportChartObject _CmDatabases_DbLogSizeLeftMbGraph;
	private ReportChartObject _CmDatabases_DbLogSizeUsedMbGraph;
	
	private ReportChartObject _CmDatabases_TempdbUsedMbGraph;

	private ReportChartObject _CmDatabases_OsDiskUsedPct;
	private ReportChartObject _CmDatabases_OsDiskFreeMb;
	private ReportChartObject _CmDatabases_OsDiskUsedMb;


	/**
	 * Set descriptions for the table, and the columns
	 */
	private void setSectionDescription(ResultSetTableModel rstm)
	{
		if (rstm == null)
			return;
		
		// Section description
		rstm.setDescription(
				"Information from last collector sample from the table <code>CmDatabases_abs</code><br>" +
				"This will show you sizes of all databases and there last Usage, on Data and Log <br>" +
				"");
	}
}

