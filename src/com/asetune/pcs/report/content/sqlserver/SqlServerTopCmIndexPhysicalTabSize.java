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

public class SqlServerTopCmIndexPhysicalTabSize 
extends SqlServerAbstract
{
	private ResultSetTableModel _shortRstm;

	public SqlServerTopCmIndexPhysicalTabSize(DailySummaryReportAbstract reportingInstance)
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
		return "Top TABLE Size (order by: SizeInMb, origin: CmIndexPhysical/dm_db_index_physical_stats)";
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
//		int topRows = localConf.getIntProperty(this.getClass().getSimpleName()+".top", 20);
		int topRows = getTopRows();
//		int havingAbove = 100;

		 // just to get Column names
//		String dummySql = "select * from [CmIndexPhysical_diff] where 1 = 2";
//		ResultSetTableModel dummyRstm = executeQuery(conn, dummySql, true, "metadata");

		// Create Column selects, but only if the column exists in the PCS Table
//		String Inserts_sum             = !dummyRstm.hasColumnNoCase("Inserts"            ) ? "" : "    ,sum([Inserts])                                        as [Inserts_sum] -- 16.0 \n"; 
//		String Updates_sum             = !dummyRstm.hasColumnNoCase("Updates"            ) ? "" : "    ,sum([Updates])                                        as [Updates_sum] -- 16.0 \n"; 
//		String Deletes_sum             = !dummyRstm.hasColumnNoCase("Deletes"            ) ? "" : "    ,sum([Deletes])                                        as [Deletes_sum] -- 16.0 \n"; 
//		String Scans_sum               = !dummyRstm.hasColumnNoCase("Scans"              ) ? "" : "    ,sum([Scans])                                          as [Scans_sum]   -- 16.0 \n"; 
		

//		String sql = getCmDiffColumnsAsSqlComment("CmObjectActivity")
//			    + "select top " + topRows + " \n"
//			    + "     min([CmSampleTime])                                   as [CmSampleTime_min] \n"
//			    + "    ,max([CmSampleTime])                                   as [CmSampleTime_max] \n"
//			    + "    ,cast('' as varchar(30))                               as [Duration] \n"
//			    + "    ,[DBName]                                              as [DBName] \n"
//			    + "    ,[ObjectName]                                          as [ObjectName] \n"
//			    + "    ,[IndexName]                                           as [IndexName] \n"
//			    + "    ,max([UsageInMb])                                      as [UsageInMb_max] \n"
//			    + "    ,max([TabRowCount])                                    as [TabRowCount_max] \n"
//			    + "    ,max([NumUsedPages])                                   as [NumUsedPages_max] \n"
//			    + "    ,max([RowsPerPage])                                    as [RowsPerPage_max] \n"
//			    + "    ,max([LockScheme])                                     as [LockScheme] \n"
//			    + "    ,sum([UsedCount])                                      as [UsedCount_sum] \n"
//			    + "    ,count(*)                                              as [samples_count] \n"
//			    + "    ,sum(CASE WHEN [Remark] = '' THEN 0 ELSE 1 END)        as [Remark_cnt] \n"
//			    + "    ,sum(CASE WHEN [Remark] = 'TabScan' THEN 1 ELSE 0 END) as [Remark_TabScan_cnt] \n"
//			    + "    ,sum([LogicalReads])                                   as [LogicalReads_sum] \n"
//			    + "    ,sum([PhysicalReads])                                  as [PhysicalReads_sum] \n"
//			    + "    ,sum([APFReads])                                       as [APFReads_sum] \n"
//			    + "    ,sum([PagesRead])                                      as [PagesRead_sum] \n"
//			    + "    ,sum([IOSize1Page])                                    as [IOSize1Page_sum]     -- 15.7.0 esd#2 \n"
//			    + "    ,sum([IOSize2Pages])                                   as [IOSize2Pages_sum]    -- 15.7.0 esd#2 \n"
//			    + "    ,sum([IOSize4Pages])                                   as [IOSize4Pages_sum]    -- 15.7.0 esd#2 \n"
//			    + "    ,sum([IOSize8Pages])                                   as [IOSize8Pages_sum]    -- 15.7.0 esd#2 \n"
//			    + "    ,sum([PhysicalWrites])                                 as [PhysicalWrites_sum] \n"
//			    + "    ,sum([PagesWritten])                                   as [PagesWritten_sum] \n"
//			    + "    ,sum([Operations])                                     as [Operations_sum] \n"
//			    + "    ,sum([RowsInsUpdDel])                                  as [RowsInsUpdDel_sum] \n"
//			    + "    ,sum([RowsInserted])                                   as [RowsInserted_sum] \n"
//			    + "    ,sum([RowsDeleted])                                    as [RowsDeleted_sum] \n"
//			    + "    ,sum([RowsUpdated])                                    as [RowsUpdated_sum] \n"
//			    + Inserts_sum
//			    + Updates_sum
//			    + Deletes_sum
//			    + Scans_sum
//			    + "    ,sum([LockRequests])                                   as [LockRequests_sum] \n"
//			    + "    ,sum([LockWaits])                                      as [LockWaits_sum] \n"
//			    + "    ,max([LockContPct])                                    as [LockContPct_max] \n"
//			    + "    ,sum([SharedLockWaitTime])                             as [SharedLockWaitTime_sum]    -- 15.7 \n"
//			    + "    ,sum([ExclusiveLockWaitTime])                          as [ExclusiveLockWaitTime_sum] -- 15.7 \n"
//			    + "    ,sum([UpdateLockWaitTime])                             as [UpdateLockWaitTime_sum]    -- 15.7 \n"
//			    + "    ,sum([OptSelectCount])                                 as [OptSelectCount_sum] \n"
//			    + "    ,max([ObjectCacheDate])                                as [ObjectCacheDate_max] \n"
//			    + "    ,max([LastOptSelectDate])                              as [LastOptSelectDate_max] \n"
//			    + "    ,max([LastUsedDate])                                   as [LastUsedDate_max] \n"
//			    + "from [CmObjectActivity_diff] x \n"
//			    + "where x.[UsageInMb] > 0 \n"
//			    + "group by [DBName], [ObjectName], [IndexName] \n"
//			    + "having [UsageInMb_max] > " + havingAbove + "\n"
//			    + "order by [UsageInMb_max] desc \n"
//			    + "";

		String sql = getCmDiffColumnsAsSqlComment("CmIndexPhysical")
				+ "select top " + topRows + " \n"
			    + "    * \n"
			    + "from [CmIndexPhysical_abs] x \n"
			    + "where [SessionSampleTime] = (select max([SessionSampleTime]) from [CmIndexPhysical_abs]) \n"
			    + "order by [SizeInMb] desc \n"
			    + "";

		_shortRstm = executeQuery(conn, sql, false, "TopTableSize");
		if (_shortRstm == null)
		{
			_shortRstm = ResultSetTableModel.createEmpty("TopTableSize");
			return;
		}
		else
		{
			// Highlight sort column
			_shortRstm.setHighlightSortColumns("SizeInMb");

			// Remove some columns which we dont really need
			_shortRstm.removeColumnNoCase("SessionStartTime");
			_shortRstm.removeColumnNoCase("SessionSampleTime");
			_shortRstm.removeColumnNoCase("CmSampleMs");
			_shortRstm.removeColumnNoCase("CmNewDiffRateRow");
			_shortRstm.removeColumnNoCase("pageCountDiff");
			_shortRstm.removeColumnNoCase("rowCountDiff");

			// Describe the table
			setSectionDescription(_shortRstm);

			// Calculate Duration
			setDurationColumn(_shortRstm, "CmSampleTime_min", "CmSampleTime_max", "Duration");
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
				"Table/Index Size (ordered by: SizeInMb) <br>" +
				"<br>" +
//				"Note 1: This is the same column layout as the previos 'Top TABLE/INDEX Activity', so the column 'UsageInMb_max' which we order by is quite far to the right...<br>" +
//				"Note 2: Listed tables needs to be above 100 MB<br>" + // see variable 'havingAbove' in the create() method 
//				"<br>" +
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
