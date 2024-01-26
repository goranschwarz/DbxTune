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
import com.asetune.pcs.report.content.IReportChart;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;

public class SqlServerDbSize 
extends SqlServerAbstract
{
//	private static Logger _logger = Logger.getLogger(SqlServerDbSize.class);

	private ResultSetTableModel _shortRstm;

	public SqlServerDbSize(DailySummaryReportAbstract reportingInstance)
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
		sb.append(_shortRstm.toHtmlTableString("sortable", true, true, null, new ReportEntryTableStringRenderer()
		{
			@Override
			public String cellValue(ResultSetTableModel rstm, int row, int col, String colName, Object objVal, String strVal)
			{
				/**
				 * If compatibility_level is below "tempdb", then mark the cell as RED
				 */
				if ("compatibility_level".equals(colName))
				{
					int compatLevel_tempdb = -1;
					for (int r=0; r<rstm.getRowCount(); r++)
					{
						String dbname = rstm.getValueAsString(r, "DBName");
						if ("tempdb".equals(dbname))
						{
							compatLevel_tempdb = rstm.getValueAsInteger(r, "compatibility_level", true, -1);
							break;
						}
					}
					int compatLevel_curDb = StringUtil.parseInt(strVal, -1);
					if (compatLevel_curDb < compatLevel_tempdb)
					{
						String tooltip = "Column 'compatibility_level' " + compatLevel_curDb + dbVersionToStr(compatLevel_curDb) + " is less than the 'server level'.\n"
								+ "You may not take advantage of new functionality, which is available at this SQL-Server version... \n"
								+ "Server compatibility_level is: " + compatLevel_tempdb + dbVersionToStr(compatLevel_tempdb);

						strVal = "<div title=\""+tooltip+"\"> <font color='red'>" + strVal + "</font><div>";
					}
				}

				/**
				 * If 'collation_name' is not same as "tempdb", then mark the cell as RED
				 */
				if ("collation_name".equals(colName))
				{
					String collation_name_tempdb = "";
					for (int r=0; r<rstm.getRowCount(); r++)
					{
						String dbname = rstm.getValueAsString(r, "DBName");
						if ("tempdb".equals(dbname))
						{
							collation_name_tempdb = rstm.getValueAsString(r, "collation_name", true, "");
							break;
						}
					}
					String collation_name_curDb = strVal;
					if (collation_name_curDb != null && !collation_name_curDb.equals(collation_name_tempdb))
					{
						String tooltip = "Column 'collation_name' " + collation_name_curDb + " is different than 'tempdb'.\n"
								+ "This might give you substandard performance, due to implicit convertions... \n";

						strVal = "<div title=\""+tooltip+"\"> <font color='red'>" + strVal + "</font><div>";
					}
				}

				/**
				 * If 'LogSizeInMb' is *high*, then mark the cell as RED
				 * Larger than 'DataSizeInMb'
				 */
				if ("LogSizeInMb".equals(colName))
				{
					int    DataSizeInMb  = rstm.getValueAsInteger(row, "DataSizeInMb", true, -1);
					int    LogSizeInMb   = rstm.getValueAsInteger(row, "LogSizeInMb",  true, -1);
					String recoveryModel = rstm.getValueAsString (row, "recovery_model_desc");

					// Only check FULL recovery model
					if ( ! "FULL".equalsIgnoreCase(recoveryModel) )
						return strVal;

					// log size needs to bee above some value: 256 MB 
					if (LogSizeInMb < 256) 
						return strVal;
					
					// When LOG-SIZE is above DATA-SIZE  (take away 100 MB from the log, if it's "close" to DataSize)
					if ((LogSizeInMb - 100) > DataSizeInMb)
					{
						String tooltip = "Column 'LogSizeInMb' is high. You need to 'backup' or 'truncate' the transaction log every now and then.";
						strVal = "<div title=\""+tooltip+"\"> <font color='red'>" + strVal + "</font><div>";
					}
				}
				return strVal;
			}

			private String dbVersionToStr(int compatLevel)
			{
				switch (compatLevel)
				{
//				case 180: return compatLevel + " (SQL Server 2025)"; // just guessing here
//				case 170: return compatLevel + " (SQL Server 2023)"; // just guessing here
				case 160: return compatLevel + " (SQL Server 2021)"; // just guessing here
				case 150: return compatLevel + " (SQL Server 2019)";
				case 140: return compatLevel + " (SQL Server 2017)";
				case 130: return compatLevel + " (SQL Server 2016)";
				case 120: return compatLevel + " (SQL Server 2014)";
				case 110: return compatLevel + " (SQL Server 2012)";
				case 100: return compatLevel + " (SQL Server 2008 or 2008 R2)";
				case 90:  return compatLevel + " (SQL Server 2005)";
				case 80:  return compatLevel + " (SQL Server 2000)";
				}
				
				return compatLevel + " (unknown)";
			}
		}));
		

		// link to DbxCentral graphs
		sb.append(getDbxCentralLinkWithDescForGraphs(false, "Below are Transaction Log and Data Size Usage of each Database during the day<br>\n"
		                                                  + "FIXME: Presented as: \n"
		                                                  + "<ul> \n"
		                                                  + "  <li>FIXME: <b> Space Used in Percent </b> - When this gets <b>high</b> we could be in trouble. But the below 'Space Available' is a better indicator.</li> \n"
		                                                  + "  <li>FIXME: <b> Space Available in MB </b> - When This gets <b>low</b> we could be in trouble. No space = No more modifications. </li> \n"
		                                                  + "  <li>FIXME: <b> Space Used in MB      </b> - Just an indicator of how much MB we are actually using for the different databases.</li> \n"
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
		_CmDatabases_DbSizeMb              .writeHtmlContent(sb, null, null);

		sb.append("<h4>DB Data Space Usage</h4> \n");
		_CmDatabases_DbDataSizeUsedPctGraph.writeHtmlContent(sb, null, null);
		_CmDatabases_DbDataSizeLeftMbGraph .writeHtmlContent(sb, null, null);
		_CmDatabases_DbDataSizeUsedMbGraph .writeHtmlContent(sb, null, null);
		
		sb.append("<h4>DB Transaction Log Space Usage</h4> \n");
		_CmDatabases_DbLogSizeUsedPctGraph .writeHtmlContent(sb, null, null);
		_CmDatabases_DbLogSizeLeftMbGraph  .writeHtmlContent(sb, null, null);
		_CmDatabases_DbLogSizeUsedMbGraph  .writeHtmlContent(sb, null, null);

		sb.append("<h4>Tempdb Space Usage</h4> \n");
		_CmDatabases_TempdbUsedMbGraph     .writeHtmlContent(sb, null, null);
		
		sb.append("<h4>DB OS Disk Space Usage (if we get near full here, we are in trouble)</h4> \n");
		_CmDatabases_OsDiskUsedPct         .writeHtmlContent(sb, null, null);
		_CmDatabases_OsDiskFreeMb          .writeHtmlContent(sb, null, null);
		_CmDatabases_OsDiskUsedMb          .writeHtmlContent(sb, null, null);
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
	public String[] getMandatoryTables()
	{
		return new String[] { "CmDatabases_abs" };
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		// just to get Column names
		String dummySql = "select * from [CmDatabases_abs] where 1 = 2";
		ResultSetTableModel dummyRstm = executeQuery(conn, dummySql, false, "metadata");

		String collation_name = "";
		if (dummyRstm.hasColumn("collation_name"))
		{
			collation_name = "    ,[collation_name] \n";
		}
		
		String sql = ""
			    + "select \n"
			    + "     [DBName] \n"
			    + "    ,[compatibility_level] \n"
			    + "    ,[recovery_model_desc] \n"
			    + collation_name
//			    + "    ,[DataFileGroupCount] \n"
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
			    + "    ,[LastDbBackupTime] \n"
			    + "    ,[LastDbBackupAgeInHours] \n"
			    + "\n"
			    + "    ,[LogOsDisk] \n"
			    + "    ,[LogOsDiskFreeMb] \n"
			    + "    ,[LogOsDiskUsedMb] \n"
			    + "    ,[LogOsDiskFreePct] \n"
			    + "    ,[LogOsDiskUsedPct] \n"
			    + "    ,[LogNextGrowthSizeMb] \n"
			    + "    ,[LastLogBackupTime] \n"
			    + "    ,[LastLogBackupAgeInHours] \n"
			    + "\n"
			    + "    ,[DataOsFileName] \n"
			    + "    ,[LogOsFileName] \n"
			    + "from [CmDatabases_abs] x \n"
			    + "where [SessionSampleTime] = (select max([SessionSampleTime]) from [CmDatabases_abs]) \n"
			    + "order by [DbSizeInMb] desc \n"
			    + "";
		
		_shortRstm = executeQuery(conn, sql, true, "CmDatabases_abs");

		// Highlight sort column
		_shortRstm.setHighlightSortColumns("DbSizeInMb");

		// Describe the table
		setSectionDescription(_shortRstm);
		
		String schema = getReportingInstance().getDbmsSchemaName();

		_CmDatabases_DbSizeMb               = createTsLineChart(conn, schema, "CmDatabases", "DbSizeMb"              , -1,  true , null, "Database Size in MB (Server->Databases)");

		_CmDatabases_DbDataSizeUsedPctGraph = createTsLineChart(conn, schema, "CmDatabases", "DbDataSizeUsedPctGraph", 100, true , null, "DB Data Space Used in Percent (Server->Databases)");
		_CmDatabases_DbDataSizeLeftMbGraph  = createTsLineChart(conn, schema, "CmDatabases", "DbDataSizeLeftMbGraph" , -1,  true , null, "DB Data Space Available in MB (Server->Databases)");
		_CmDatabases_DbDataSizeUsedMbGraph  = createTsLineChart(conn, schema, "CmDatabases", "DbDataSizeUsedMbGraph" , -1,  true , null, "DB Data Space Used in MB (Server->Databases)");

		_CmDatabases_DbLogSizeUsedPctGraph  = createTsLineChart(conn, schema, "CmDatabases", "DbLogSizeUsedPctGraph" , 100, true , null, "DB Transaction Log Space Used in Percent (Server->Databases)");
		_CmDatabases_DbLogSizeLeftMbGraph   = createTsLineChart(conn, schema, "CmDatabases", "DbLogSizeLeftMbGraph"  , -1,  true , null, "DB Transaction Log Space Available in MB (Server->Databases)");
		_CmDatabases_DbLogSizeUsedMbGraph   = createTsLineChart(conn, schema, "CmDatabases", "DbLogSizeUsedMbGraph"  , -1,  true , null, "DB Transaction Log Space Used in MB (Server->Databases)");

		_CmDatabases_TempdbUsedMbGraph      = createTsLineChart(conn, schema, "CmDatabases", "TempdbUsedMbGraph"     , -1,  false, null, "TempDB Space used in MB (Server->Databases)");

		_CmDatabases_OsDiskUsedPct          = createTsLineChart(conn, schema, "CmDatabases", "OsDiskUsedPct"         , 100, false, null, "DB OS Disk Space Used in Percent (Server->Databases)");
		_CmDatabases_OsDiskFreeMb           = createTsLineChart(conn, schema, "CmDatabases", "OsDiskFreeMb"          , -1,  false, null, "DB OS Disk Space Available in MB (Server->Databases)");
		_CmDatabases_OsDiskUsedMb           = createTsLineChart(conn, schema, "CmDatabases", "OsDiskUsedMb"          , -1,  false, null, "DB OS Disk Space Used in MB (Server->Databases)");
	}

	private IReportChart _CmDatabases_DbSizeMb;

	private IReportChart _CmDatabases_DbDataSizeUsedPctGraph;
	private IReportChart _CmDatabases_DbDataSizeLeftMbGraph;
	private IReportChart _CmDatabases_DbDataSizeUsedMbGraph;

	private IReportChart _CmDatabases_DbLogSizeUsedPctGraph;
	private IReportChart _CmDatabases_DbLogSizeLeftMbGraph;
	private IReportChart _CmDatabases_DbLogSizeUsedMbGraph;
	
	private IReportChart _CmDatabases_TempdbUsedMbGraph;

	private IReportChart _CmDatabases_OsDiskUsedPct;
	private IReportChart _CmDatabases_OsDiskFreeMb;
	private IReportChart _CmDatabases_OsDiskUsedMb;


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

