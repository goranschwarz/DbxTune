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
import java.io.StringWriter;
import java.io.Writer;

import com.asetune.gui.ResultSetTableModel;
import com.asetune.pcs.report.DailySummaryReportAbstract;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;

public class SqlServerTopCmIndexPhysicalAvgPageUsedPct 
extends SqlServerAbstract
{
	private ResultSetTableModel _shortRstm;

	public SqlServerTopCmIndexPhysicalAvgPageUsedPct(DailySummaryReportAbstract reportingInstance)
	{
		super(reportingInstance);
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

			sb.append("Row Count: " + _shortRstm.getRowCount() + "<br>\n");
			sb.append(toHtmlTable(_shortRstm));
		}
	}

//	@Override
//	public String getMessageText()
//	{
//		StringBuilder sb = new StringBuilder();
//
//		if (_shortRstm.getRowCount() == 0)
//		{
//			sb.append(getSectionDescriptionHtml(_shortRstm, true));
//			
//			sb.append("No rows found <br>\n");
//		}
//		else
//		{
//			// Get a description of this section, and column names
//			sb.append(getSectionDescriptionHtml(_shortRstm, true));
//
//			sb.append("Row Count: ").append(_shortRstm.getRowCount()).append("<br>\n");
////			sb.append(_shortRstm.toHtmlTableString("sortable"));
//			sb.append(toHtmlTable(_shortRstm));
//		}
//
//		return sb.toString();
//	}

	@Override
	public String getSubject()
	{
		return "Average PAGE Usage Percent on tables (order by: avg_page_space_used_in_percent, origin: CmIndexPhysical/dm_db_index_physical_stats)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}


	@Override
	public String[] getMandatoryTables()
	{
		return new String[] { "CmIndexPhysical_abs" };
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		_shortRstm = createAvgPageUsedPct(conn, srvName, pcsSavedConf, localConf);
	}
	
	private int _topRows  = 20;
	private int _belowPct = 70;
	private int _minRows  = 1000;

	public ResultSetTableModel createAvgPageUsedPct(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		_topRows  = localConf.getIntProperty(this.getClass().getSimpleName()+".top"     , _topRows);
		_belowPct = localConf.getIntProperty(this.getClass().getSimpleName()+".belowPct", _belowPct);
		_minRows  = localConf.getIntProperty(this.getClass().getSimpleName()+".minRows" , _minRows);

		String sql = getCmDiffColumnsAsSqlComment("CmIndexPhysical")
				+ "select top " + _topRows + " \n"
			    + "    * \n"
			    + "from [CmIndexPhysical_abs] x \n"
			    + "where [SessionSampleTime] = (select max([SessionSampleTime]) from [CmIndexPhysical_abs]) \n"
			    + "  and [avg_page_space_used_in_percent] < " + _belowPct + " \n"
			    + "  and [record_count] > " + _minRows + " \n"
				+ getReportPeriodSqlWhere()
			    + "order by [avg_page_space_used_in_percent] \n"
			    + "";

		ResultSetTableModel rstm = executeQuery(conn, sql, false, "AvgPageUsedPct");
		if (rstm == null)
		{
			rstm = ResultSetTableModel.createEmpty("AvgPageUsedPct");
		}
		else
		{
			// Remove some columns which we dont really need
			rstm.removeColumnNoCase("SessionStartTime");
			rstm.removeColumnNoCase("SessionSampleTime");
			rstm.removeColumnNoCase("CmSampleMs");
			rstm.removeColumnNoCase("CmNewDiffRateRow");
			rstm.removeColumnNoCase("pageCountDiff");
			rstm.removeColumnNoCase("rowCountDiff");

			// Describe the table
			setSectionDescription(rstm);

			// Calculate Duration
			setDurationColumn(rstm, "CmSampleTime_min", "CmSampleTime_max", "Duration");
		}
		return rstm;
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
				"Average PAGE Usage Percent on tables (ordered by: avg_page_space_used_in_percent) <br>" +
				"<br>" +
				"Tables in here might be good to <i>rebuild</i> to <i>compact</i> and get rid of unused space (and cache memory). <br>" +
				"<br>" +
//				"Note 1: This is the same column layout as the previos 'Top TABLE/INDEX Activity', so the column 'UsageInMb_max' which we order by is quite far to the right...<br>" +
//				"Note 2: Listed tables needs to be above 100 MB<br>" + // see variable 'havingAbove' in the create() method 
//				"<br>" +
				"Config: <code>" + this.getClass().getSimpleName() + ".top      = " + _topRows  + " </code><br>" +
				"Config: <code>" + this.getClass().getSimpleName() + ".belowPct = " + _belowPct + " </code><br>" +
				"Config: <code>" + this.getClass().getSimpleName() + ".minRows  = " + _minRows  + " </code><br>" +
//				"Config: <code>" + CmObjectActivity.PROPKEY_sample_tabRowCount + " = " + _isTableSizeConfigured + " </code><br>" +
//				"Disable Time: " + _tableSizeDisableTimeStr + "<br>" +
//				"<br>" +
				"ASE Source table is 'sys.dm_db_index_physical_stats'. <br>" +
				"PCS Source table is 'CmIndexPhysical'. (PCS = Persistent Counter Store) <br>" +
				"<br>" +
//				"The report <i>summarizes</i> (min/max/count/sum/avg) all entries/samples from the <i>source_DIFF</i> table. <br>" +
//				"Typically the column name <i>postfix</i> will tell you what aggregate function was used. <br>" +
				"");

		// Columns description
//		rstm.setColumnDescription("CmSampleTime_min"          , "First entry was sampled.");
//		rstm.setColumnDescription("CmSampleTime_max"          , "Last entry was sampled.");
//		rstm.setColumnDescription("Duration"                  , "Difference between first/last sample");
//		rstm.setColumnDescription("DBName"                    , "Database name");
//		rstm.setColumnDescription("ObjectName"                , "Table or Index Name");
//		rstm.setColumnDescription("IndexName"                 , "DATA if it's DATA Pages access. otherwise the IndexName");
//		
//		rstm.setColumnDescription("UsedCount_sum"             , "");
//		rstm.setColumnDescription("samples_count"             , "");
//		rstm.setColumnDescription("Remark_cnt"                , "Number of times the column 'Remark' is NOT empty");
//		rstm.setColumnDescription("Remark_TabScan_cnt"        , "Number of times the column 'Remark' contained 'TabScan'");
//		rstm.setColumnDescription("LogicalReads_sum"          , "Number of LogicalReads in the period duration");
//		rstm.setColumnDescription("PhysicalReads_sum"         , "Number of PhysicalReads in the period duration");
//		rstm.setColumnDescription("APFReads_sum"              , "Number of APFReads in the period duration (NOTE: APF is Async Pre Fetch, and is likly a Physical read)");
//		rstm.setColumnDescription("PagesRead_sum"             , "Number of PagesRead in the period duration (NOTE: if it's a 8 Page read (IOSize8Pages), then this will be incremented 8 times, one for each 1 page read in the multi page read)");
//		rstm.setColumnDescription("IOSize1Page_sum"           , "Number of SINGLE page reads");
//		rstm.setColumnDescription("IOSize2Pages_sum"          , "Number of TWO (2) pages reads in the same IO Operation");
//		rstm.setColumnDescription("IOSize4Pages_sum"          , "Number of FOUR (4) pages reads in the same IO Operation");
//		rstm.setColumnDescription("IOSize8Pages_sum"          , "Number of EIGHT (8)  pages reads in the same IO Operation");
//		rstm.setColumnDescription("PhysicalWrites_sum"        , "Number of Physical Writes");
//		rstm.setColumnDescription("PagesWritten_sum"          , "Number of Pages Written to disk (If it uses LARGE Page IO, then this will be higher than PhyscalWrites)");
//		rstm.setColumnDescription("Operations_sum"            , "");
//		rstm.setColumnDescription("RowsInsUpdDel_sum"         , "Number of Insert/updates/deletes RECORDS that has been done");
//		rstm.setColumnDescription("RowsInserted_sum"          , "Number of Insert RECORDS that has been done");
//		rstm.setColumnDescription("RowsDeleted_sum"           , "Number of Deletes RECORDS that has been done");
//		rstm.setColumnDescription("RowsUpdated_sum"           , "Number of Updates RECORDS that has been done");
//		rstm.setColumnDescription("Inserts_sum"               , "Number of Insert Operations");
//		rstm.setColumnDescription("Updates_sum"               , "Number of Update Operations");
//		rstm.setColumnDescription("Deletes_sum"               , "Number of Delete Operations");
//		rstm.setColumnDescription("Scans_sum"                 , "");
//		rstm.setColumnDescription("TabRowCount_max"           , " (if available)");
//		rstm.setColumnDescription("UsageInMb_max"             , " (if available)");
//		rstm.setColumnDescription("NumUsedPages_max"          , " (if available)");
//		rstm.setColumnDescription("RowsPerPage_max"           , " (if available)");
//		rstm.setColumnDescription("LockScheme]"               , "what's the tables locking strategy 'allpages', 'datarows' or 'datapages'");
//		rstm.setColumnDescription("LockRequests_sum"          , "Number of lock REQUESTS done");
//		rstm.setColumnDescription("LockWaits_sum"             , "Number of lock WAITS done");
//		rstm.setColumnDescription("LockContPct_max"           , "Max Locking CONTENTENTION on this table");
//		rstm.setColumnDescription("SharedLockWaitTime_sum"    , "Wait TIME during shared locks");
//		rstm.setColumnDescription("ExclusiveLockWaitTime_sum" , "Wait TIME during exlusive locks");
//		rstm.setColumnDescription("UpdateLockWaitTime_sum"    , "Wait TIME during update locks");
//		rstm.setColumnDescription("OptSelectCount_sum"        , "");
//		rstm.setColumnDescription("ObjectCacheDate_max"       , "");
//		rstm.setColumnDescription("LastOptSelectDate_max"     , "");
//		rstm.setColumnDescription("LastUsedDate_max"          , "");
	}
}
