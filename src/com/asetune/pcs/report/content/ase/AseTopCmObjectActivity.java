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
package com.asetune.pcs.report.content.ase;

import java.io.IOException;
import java.io.Writer;
import java.sql.SQLException;
import java.sql.Types;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.h2.tools.SimpleResultSet;

import com.asetune.gui.ResultSetTableModel;
import com.asetune.pcs.report.DailySummaryReportAbstract;
import com.asetune.pcs.report.content.SparklineHelper;
import com.asetune.pcs.report.content.SparklineHelper.AggType;
import com.asetune.pcs.report.content.SparklineHelper.DataSource;
import com.asetune.pcs.report.content.SparklineHelper.SparkLineParams;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;
import com.asetune.utils.HtmlTableProducer;
import com.asetune.utils.HtmlTableProducer.ColumnCopyDef;
import com.asetune.utils.HtmlTableProducer.ColumnCopyRender;
import com.asetune.utils.HtmlTableProducer.ColumnCopyRow;
import com.asetune.utils.HtmlTableProducer.ColumnStatic;
import com.asetune.utils.StringUtil;

public class AseTopCmObjectActivity extends AseAbstract
{
	private static Logger _logger = Logger.getLogger(AseTopCmObjectActivity.class);

	private ResultSetTableModel _shortRstm;
	private ResultSetTableModel _sparklineRstm;
	private List<String>        _miniChartJsList = new ArrayList<>();

	private ReportType _reportType    = ReportType.LOGICAL_READS;
	private String     _sqlOrderByCol = "-unknown-";
	private String     _sqlHavingCol  = "-unknown-";
	private String     _orderByCol_noBrackets = "-unknown-";
	
	public enum ReportType
	{
		LOGICAL_READS, 
		LOCK_WAIT_TIME
	};
	
	public AseTopCmObjectActivity(DailySummaryReportAbstract reportingInstance, ReportType reportType)
	{
		super(reportingInstance);

		_reportType = reportType;

		if      (ReportType.LOGICAL_READS .equals(_reportType)) { _sqlOrderByCol = "[LogicalReads__sum]"; _sqlHavingCol = "[LogicalReads__sum]"; }
		else if (ReportType.LOCK_WAIT_TIME.equals(_reportType)) { _sqlOrderByCol = "[LockWaitTime__sum]"; _sqlHavingCol = "[LockWaitTime__sum]"; }
		else throw new IllegalArgumentException("Unhandled reportType='" + reportType + "'.");

		_orderByCol_noBrackets = _sqlOrderByCol.replace("[", "").replace("]", "");
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

			if (_sparklineRstm != null)
			{
				sb.append("<br>\n");
				sb.append("<details open> \n");
				sb.append("<summary>Details for above Statements, including SQL Text (click to collapse) </summary> \n");
				
				sb.append("<br>\n");
				sb.append("Sparklines stacked, Row Count: " + _sparklineRstm.getRowCount() + " (This is the same information as the in the above table, but some counter details left out).<br>\n");
				sb.append(toHtmlTable(_sparklineRstm));

				sb.append("\n");
				sb.append("</details> \n");
			}
		}
		
		// Write JavaScript code for CPU SparkLine
		if (isFullMessageType())
		{
			for (String str : _miniChartJsList)
				sb.append(str);
		}
	}

	private String getSubjectSubType()
	{
		if (ReportType.LOGICAL_READS .equals(_reportType)) return "Activity";
		if (ReportType.LOCK_WAIT_TIME.equals(_reportType)) return "Blocking Lock Wait Statistics";
		return "";
	}
	@Override
	public String getSubject()
	{
		return "Top TABLE/INDEX " + getSubjectSubType() + " (order by: " + _orderByCol_noBrackets + ", origin: CmObjectActivity / monOpenObjects)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}


	@Override
	public String[] getMandatoryTables()
	{
		return new String[] { "CmObjectActivity_diff" };
	}

//	@Override
//	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
//	{
////		int topRows = localConf.getIntProperty(this.getClass().getSimpleName()+".top", 20);
//		int topRows = getTopRows();
//		int havingAbove = 1000;
//
//		 // just to get Column names
//		String dummySql = "select * from [CmObjectActivity_diff] where 1 = 2";
//		ResultSetTableModel dummyRstm = executeQuery(conn, dummySql, true, "metadata");
//
//		// Create Column selects, but only if the column exists in the PCS Table
//		String IOSize1Page_sum           = !dummyRstm.hasColumnNoCase("IOSize1Page"          ) ? "" : "    ,sum([IOSize1Page])                                    as [IOSize1Page_sum]           -- 15.7.0 esd#2 \n";
//		String IOSize2Pages_sum          = !dummyRstm.hasColumnNoCase("IOSize2Pages"         ) ? "" : "    ,sum([IOSize2Pages])                                   as [IOSize2Pages_sum]          -- 15.7.0 esd#2 \n";
//		String IOSize4Pages_sum          = !dummyRstm.hasColumnNoCase("IOSize4Pages"         ) ? "" : "    ,sum([IOSize4Pages])                                   as [IOSize4Pages_sum]          -- 15.7.0 esd#2 \n";
//		String IOSize8Pages_sum          = !dummyRstm.hasColumnNoCase("IOSize8Pages"         ) ? "" : "    ,sum([IOSize8Pages])                                   as [IOSize8Pages_sum]          -- 15.7.0 esd#2 \n";
//                                                                                             
//		String Inserts_sum               = !dummyRstm.hasColumnNoCase("Inserts"              ) ? "" : "    ,sum([Inserts])                                        as [Inserts_sum]               -- 16.0 \n"; 
//		String Updates_sum               = !dummyRstm.hasColumnNoCase("Updates"              ) ? "" : "    ,sum([Updates])                                        as [Updates_sum]               -- 16.0 \n"; 
//		String Deletes_sum               = !dummyRstm.hasColumnNoCase("Deletes"              ) ? "" : "    ,sum([Deletes])                                        as [Deletes_sum]               -- 16.0 \n"; 
//		String Scans_sum                 = !dummyRstm.hasColumnNoCase("Scans"                ) ? "" : "    ,sum([Scans])                                          as [Scans_sum]                 -- 16.0 \n"; 
//		
//		String SharedLockWaitTime_sum    = !dummyRstm.hasColumnNoCase("SharedLockWaitTime"   ) ? "" : "    ,sum([SharedLockWaitTime])                             as [SharedLockWaitTime_sum]    -- 15.7 \n"; 
//		String ExclusiveLockWaitTime_sum = !dummyRstm.hasColumnNoCase("ExclusiveLockWaitTime") ? "" : "    ,sum([ExclusiveLockWaitTime])                          as [ExclusiveLockWaitTime_sum] -- 15.7 \n"; 
//		String UpdateLockWaitTime_sum    = !dummyRstm.hasColumnNoCase("UpdateLockWaitTime"   ) ? "" : "    ,sum([UpdateLockWaitTime])                             as [UpdateLockWaitTime_sum]    -- 15.7 \n"; 
//
//		String ObjectCacheDate_max       = !dummyRstm.hasColumnNoCase("ObjectCacheDate"      ) ? "" : "    ,max([ObjectCacheDate])                                as [ObjectCacheDate_max] \n"; 
//
//
//		String sql = getCmDiffColumnsAsSqlComment("CmObjectActivity")
//			    + "select top " + topRows + " \n"
//			    + "     [DBName]                                              as [DBName] \n"
//			    + "    ,[ObjectName]                                          as [ObjectName] \n"
//			    + "    ,[IndexName]                                           as [IndexName] \n"
//			    + "    ,cast('' as varchar(512))                              as [UsedCount__chart] \n"
//			    + "    ,sum([UsedCount])                                      as [UsedCount_sum] \n"
//			    + "    ,count(*)                                              as [samples_count] \n"
//			    + "    ,sum(CASE WHEN [Remark] = '' THEN 0 ELSE 1 END)        as [Remark_cnt] \n"
//			    + "    ,sum(CASE WHEN [Remark] = 'TabScan' THEN 1 ELSE 0 END) as [Remark_TabScan_cnt] \n"
//			    + "    ,cast('' as varchar(512))                              as [LogicalReads__chart] \n"
//			    + "    ,sum([LogicalReads])                                   as [LogicalReads_sum] \n"
//			    + "    ,sum([PhysicalReads])                                  as [PhysicalReads_sum] \n"
//			    + "    ,sum([APFReads])                                       as [APFReads_sum] \n"
//			    + "    ,sum([PagesRead])                                      as [PagesRead_sum] \n"
//			    + IOSize1Page_sum 
//			    + IOSize2Pages_sum
//			    + IOSize4Pages_sum
//			    + IOSize8Pages_sum
//			    + "    ,sum([PhysicalWrites])                                 as [PhysicalWrites_sum] \n"
//			    + "    ,sum([PagesWritten])                                   as [PagesWritten_sum] \n"
//			    + "    ,sum([Operations])                                     as [Operations_sum] \n"
//			    + "    ,cast('' as varchar(512))                              as [RowsInsUpdDel__chart] \n"
//			    + "    ,sum([RowsInsUpdDel])                                  as [RowsInsUpdDel_sum] \n"
//			    + "    ,sum([RowsInserted])                                   as [RowsInserted_sum] \n"
//			    + "    ,sum([RowsDeleted])                                    as [RowsDeleted_sum] \n"
//			    + "    ,sum([RowsUpdated])                                    as [RowsUpdated_sum] \n"
//			    + Inserts_sum
//			    + Updates_sum
//			    + Deletes_sum
//			    + Scans_sum
//			    + "    ,max([TabRowCount])                                    as [TabRowCount_max] \n"
//			    + "    ,max([UsageInMb])                                      as [UsageInMb_max] \n"
//			    + "    ,max([NumUsedPages])                                   as [NumUsedPages_max] \n"
//			    + "    ,max([RowsPerPage])                                    as [RowsPerPage_max] \n"
//			    + "    ,max([LockScheme])                                     as [LockScheme] \n"
//			    + "    ,sum([LockRequests])                                   as [LockRequests_sum] \n"
//			    + "    ,sum([LockWaits])                                      as [LockWaits_sum] \n"
//			    + "    ,max([LockContPct])                                    as [LockContPct_max] \n"
//			    + SharedLockWaitTime_sum   
//			    + ExclusiveLockWaitTime_sum
//			    + UpdateLockWaitTime_sum   
//			    + "    ,sum([OptSelectCount])                                 as [OptSelectCount_sum] \n"
//			    + ObjectCacheDate_max
//			    + "    ,max([LastOptSelectDate])                              as [LastOptSelectDate_max] \n"
//			    + "    ,max([LastUsedDate])                                   as [LastUsedDate_max] \n"
//			    
//			    + "    ,min([CmSampleTime])                                   as [CmSampleTime_min] \n"
//			    + "    ,max([CmSampleTime])                                   as [CmSampleTime_max] \n"
//			    + "    ,cast('' as varchar(30))                               as [Duration] \n"
//			    + "from [CmObjectActivity_diff] x \n"
//			    + "where 1 = 1 \n"
//				+ getReportPeriodSqlWhere()
//			    + "group by [DBName], [ObjectName], [IndexName] \n"
//			    + "having [LogicalReads_sum] > " + havingAbove + "\n"
//			    + "order by [LogicalReads_sum] desc \n"
//			    + "";
//
//		_shortRstm = executeQuery(conn, sql, false, "TopTableAccess");
//		if (_shortRstm == null)
//		{
//			_shortRstm = ResultSetTableModel.createEmpty("TopTableAccess");
//			return;
//		}
//		else
//		{
//			// Describe the table
//			setSectionDescription(_shortRstm);
//
//			// Calculate Duration
//			setDurationColumn(_shortRstm, "CmSampleTime_min", "CmSampleTime_max", "Duration");
//			
//			// Mini Chart on: 
//			String whereKeyColumn = "DBName, ObjectName, IndexName"; 
//
//			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
//					SparkLineParams.create       (DataSource.CounterModel)
//					.setHtmlChartColumnName      ("UsedCount__chart")
//					.setHtmlWhereKeyColumnName   (whereKeyColumn)
//					.setDbmsTableName            ("CmObjectActivity_diff")
//					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("UsedCount")   
//					.setDbmsWhereKeyColumnName   (whereKeyColumn)
////					.setSparklineTooltipPostfix  ("Number of 'UsedCount' in below period")
//					.validate()));
//
//			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
//					SparkLineParams.create       (DataSource.CounterModel)
//					.setHtmlChartColumnName      ("LogicalReads__chart")
//					.setHtmlWhereKeyColumnName   (whereKeyColumn)
//					.setDbmsTableName            ("CmObjectActivity_diff")
//					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("LogicalReads")   
//					.setDbmsWhereKeyColumnName   (whereKeyColumn)
////					.setSparklineTooltipPostfix  ("Number of 'LogicalReads' in below period")
//					.validate()));
//
//			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
//					SparkLineParams.create       (DataSource.CounterModel)
//					.setHtmlChartColumnName      ("RowsInsUpdDel__chart")
//					.setHtmlWhereKeyColumnName   (whereKeyColumn)
//					.setDbmsTableName            ("CmObjectActivity_diff")
//					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("RowsInsUpdDel")   
//					.setDbmsWhereKeyColumnName   (whereKeyColumn)
////					.setSparklineTooltipPostfix  ("Number of 'RowsInsUpdDel' in below period")
//					.validate()));
//		}
//	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
//		int topRows = localConf.getIntProperty(this.getClass().getSimpleName()+".top", 20);
		int topRows = getTopRows();
		int havingAbove = 1000;

		 // just to get Column names
		String dummySql = "select * from [CmObjectActivity_diff] where 1 = 2";
		ResultSetTableModel dummyRstm = executeQuery(conn, dummySql, true, "metadata");

		// Get ASE Page Size
		int asePageSize = -1;
		try	{ asePageSize = getAsePageSizeFromMonDdlStorage(conn); }
		catch (SQLException ex) { }
		int asePageSizeDivider = 1024 * 1024 / asePageSize; // 2k->512, 4k->256, 8k=128, 16k=64

		// Create Column selects, but only if the column exists in the PCS Table

		// SUM columns
		String UsedCount__sum               = !dummyRstm.hasColumnNoCase("UsedCount"            ) ? "" : "    ,sum([UsedCount])                                      as [UsedCount__sum] \n";
		String LogicalReads__sum            = !dummyRstm.hasColumnNoCase("LogicalReads"         ) ? "" : "    ,sum([LogicalReads])                                   as [LogicalReads__sum] \n";
		String LogicalReadsMb__sum          = !dummyRstm.hasColumnNoCase("LogicalReads"         ) ? "" : "    ,sum([LogicalReads]) / "+asePageSizeDivider+"          as [LogicalReadsMb__sum] \n";
		String PhysicalReads__sum           = !dummyRstm.hasColumnNoCase("PhysicalReads"        ) ? "" : "    ,sum([PhysicalReads])                                  as [PhysicalReads__sum] \n";
		String APFReads__sum                = !dummyRstm.hasColumnNoCase("APFReads"             ) ? "" : "    ,sum([APFReads])                                       as [APFReads__sum] \n";
		String PagesRead__sum               = !dummyRstm.hasColumnNoCase("PagesRead"            ) ? "" : "    ,sum([PagesRead])                                      as [PagesRead__sum] \n";
		String PhysicalWrites__sum          = !dummyRstm.hasColumnNoCase("PhysicalWrites"       ) ? "" : "    ,sum([PhysicalWrites])                                 as [PhysicalWrites__sum] \n";
		String PagesWritten__sum            = !dummyRstm.hasColumnNoCase("PagesWritten"         ) ? "" : "    ,sum([PagesWritten])                                   as [PagesWritten__sum] \n";
		String Operations__sum              = !dummyRstm.hasColumnNoCase("Operations"           ) ? "" : "    ,sum([Operations])                                     as [Operations__sum] \n";
		String RowsInsUpdDel__sum           = !dummyRstm.hasColumnNoCase("RowsInsUpdDel"        ) ? "" : "    ,sum([RowsInsUpdDel])                                  as [RowsInsUpdDel__sum] \n";
		String RowsInserted__sum            = !dummyRstm.hasColumnNoCase("RowsInserted"         ) ? "" : "    ,sum([RowsInserted])                                   as [RowsInserted__sum] \n";
		String RowsDeleted__sum             = !dummyRstm.hasColumnNoCase("RowsDeleted"          ) ? "" : "    ,sum([RowsDeleted])                                    as [RowsDeleted__sum] \n";
		String RowsUpdated__sum             = !dummyRstm.hasColumnNoCase("RowsUpdated"          ) ? "" : "    ,sum([RowsUpdated])                                    as [RowsUpdated__sum] \n";
		String OptSelectCount__sum          = !dummyRstm.hasColumnNoCase("OptSelectCount"       ) ? "" : "    ,sum([OptSelectCount])                                 as [OptSelectCount__sum] \n";
		String LockRequests__sum            = !dummyRstm.hasColumnNoCase("LockRequests"         ) ? "" : "    ,sum([LockRequests])                                   as [LockRequests__sum] \n";
		String LockWaits__sum               = !dummyRstm.hasColumnNoCase("LockWaits"            ) ? "" : "    ,sum([LockWaits])                                      as [LockWaits__sum] \n";

		String IOSize1Page__sum             = !dummyRstm.hasColumnNoCase("IOSize1Page"          ) ? "" : "    ,sum([IOSize1Page])                                    as [IOSize1Page__sum]           -- 15.7.0 esd#2 \n";
		String IOSize2Pages__sum            = !dummyRstm.hasColumnNoCase("IOSize2Pages"         ) ? "" : "    ,sum([IOSize2Pages])                                   as [IOSize2Pages__sum]          -- 15.7.0 esd#2 \n";
		String IOSize4Pages__sum            = !dummyRstm.hasColumnNoCase("IOSize4Pages"         ) ? "" : "    ,sum([IOSize4Pages])                                   as [IOSize4Pages__sum]          -- 15.7.0 esd#2 \n";
		String IOSize8Pages__sum            = !dummyRstm.hasColumnNoCase("IOSize8Pages"         ) ? "" : "    ,sum([IOSize8Pages])                                   as [IOSize8Pages__sum]          -- 15.7.0 esd#2 \n";

		String Inserts__sum                 = !dummyRstm.hasColumnNoCase("Inserts"              ) ? "" : "    ,sum([Inserts])                                        as [Inserts__sum]               -- 16.0 \n"; 
		String Updates__sum                 = !dummyRstm.hasColumnNoCase("Updates"              ) ? "" : "    ,sum([Updates])                                        as [Updates__sum]               -- 16.0 \n"; 
		String Deletes__sum                 = !dummyRstm.hasColumnNoCase("Deletes"              ) ? "" : "    ,sum([Deletes])                                        as [Deletes__sum]               -- 16.0 \n"; 
		String Scans__sum                   = !dummyRstm.hasColumnNoCase("Scans"                ) ? "" : "    ,sum([Scans])                                          as [Scans__sum]                 -- 16.0 \n"; 

		String LockWaitTime__sum            = !dummyRstm.hasColumnNoCase("SharedLockWaitTime"   ) ? "" : "    ,sum([SharedLockWaitTime]) + sum([ExclusiveLockWaitTime]) + sum([UpdateLockWaitTime]) as [LockWaitTime__sum] -- 15.7 \n"; 
		String SharedLockWaitTime__sum      = !dummyRstm.hasColumnNoCase("SharedLockWaitTime"   ) ? "" : "    ,sum([SharedLockWaitTime])                             as [SharedLockWaitTime__sum]    -- 15.7 \n"; 
		String ExclusiveLockWaitTime__sum   = !dummyRstm.hasColumnNoCase("ExclusiveLockWaitTime") ? "" : "    ,sum([ExclusiveLockWaitTime])                          as [ExclusiveLockWaitTime__sum] -- 15.7 \n"; 
		String UpdateLockWaitTime__sum      = !dummyRstm.hasColumnNoCase("UpdateLockWaitTime"   ) ? "" : "    ,sum([UpdateLockWaitTime])                             as [UpdateLockWaitTime__sum]    -- 15.7 \n"; 

		// ---------------------------------------------------------------------------------------------------------------
		// ------ Keep here for a while, due to: it's easier to see what columns are DIFF calculated during development
		// ---------------------------------------------------------------------------------------------------------------
		//public static final String[] DIFF_COLUMNS     = new String[] {
		//	"LogicalReads","PhysicalReads", "APFReads", "PagesRead", "PhysicalWrites", "PagesWritten",
		//	"UsedCount", "RowsInsUpdDel", "RowsInserted", "RowsDeleted", "RowsUpdated",
		//	"Operations", "LockRequests", "LockWaits", "HkgcRequests", "HkgcPending", "HkgcOverflows",
		//	"OptSelectCount", "PhysicalLocks", "PhysicalLocksRetained", "PhysicalLocksRetainWaited",
		//	"PhysicalLocksDeadlocks", "PhysicalLocksWaited", "PhysicalLocksPageTransfer",
		//	"TransferReqWaited", "TotalServiceRequests", "PhysicalLocksDowngraded", "PagesTransferred",
		//	"ClusterPageWrites", "SharedLockWaitTime", "ExclusiveLockWaitTime", "UpdateLockWaitTime",
		//	"HkgcRequestsDcomp", "HkgcOverflowsDcomp",
		//	"IOSize1Page", "IOSize2Pages", "IOSize4Pages", "IOSize8Pages",
		//	"PRSSelectCount", "PRSRewriteCount",
		//	"NumLevel0Waiters",
		//	"Scans", "Updates", "Inserts", "Deletes"};

		// AVG columns
		String AvgLogicalReads              = !dummyRstm.hasColumnNoCase("LogicalReads"         ) ? "" : "    ,sum([LogicalReads])          *1.0 / nullif(sum([UsedCount]), 0)  as [AvgLogicalReads] \n";
		String AvgLogicalReadsMb            = !dummyRstm.hasColumnNoCase("LogicalReads"         ) ? "" : "    ,sum([LogicalReads]) * 1.0 / "+asePageSizeDivider+" / nullif(sum([UsedCount]), 0)  as [AvgLogicalReadsMb] \n";
		String AvgPhysicalReads             = !dummyRstm.hasColumnNoCase("PhysicalReads"        ) ? "" : "    ,sum([PhysicalReads])         *1.0 / nullif(sum([UsedCount]), 0)  as [AvgPhysicalReads] \n";
		String AvgAPFReads                  = !dummyRstm.hasColumnNoCase("APFReads"             ) ? "" : "    ,sum([APFReads])              *1.0 / nullif(sum([UsedCount]), 0)  as [AvgAPFReads] \n";
		String AvgPagesRead                 = !dummyRstm.hasColumnNoCase("PagesRead"            ) ? "" : "    ,sum([PagesRead])             *1.0 / nullif(sum([UsedCount]), 0)  as [AvgPagesRead] \n";
		String AvgPhysicalWrites            = !dummyRstm.hasColumnNoCase("PhysicalWrites"       ) ? "" : "    ,sum([PhysicalWrites])        *1.0 / nullif(sum([UsedCount]), 0)  as [AvgPhysicalWrites] \n";
		String AvgOperations                = !dummyRstm.hasColumnNoCase("Operations"           ) ? "" : "    ,sum([Operations])            *1.0 / nullif(sum([UsedCount]), 0)  as [AvgOperations] \n";
		String AvgLockWaitTime              = !dummyRstm.hasColumnNoCase("SharedLockWaitTime"   ) ? "" : "    ,sum([SharedLockWaitTime]) + sum([ExclusiveLockWaitTime]) + sum([UpdateLockWaitTime])    *1.0 / nullif(sum([UsedCount]), 0)  as [AvgLockWaitTime] \n";
		String AvgRowsInsUpdDel             = !dummyRstm.hasColumnNoCase("RowsInsUpdDel"        ) ? "" : "    ,sum([RowsInsUpdDel])         *1.0 / nullif(sum([UsedCount]), 0)  as [AvgRowsInsUpdDel] \n";
		String AvgScans                     = !dummyRstm.hasColumnNoCase("Scans"                ) ? "" : "    ,sum([Scans])                 *1.0 / nullif(sum([UsedCount]), 0)  as [AvgScans] \n";
		String AvgSharedLockWaitTime        = !dummyRstm.hasColumnNoCase("SharedLockWaitTime"   ) ? "" : "    ,sum([SharedLockWaitTime])    *1.0 / nullif(sum([UsedCount]), 0)  as [AvgSharedLockWaitTime] \n";
		String AvgExclusiveLockWaitTime     = !dummyRstm.hasColumnNoCase("ExclusiveLockWaitTime") ? "" : "    ,sum([ExclusiveLockWaitTime]) *1.0 / nullif(sum([UsedCount]), 0)  as [AvgExclusiveLockWaitTime] \n";
		String AvgUpdateLockWaitTime        = !dummyRstm.hasColumnNoCase("UpdateLockWaitTime"   ) ? "" : "    ,sum([UpdateLockWaitTime])    *1.0 / nullif(sum([UsedCount]), 0)  as [AvgUpdateLockWaitTime] \n";
		String AvgLockRequests              = !dummyRstm.hasColumnNoCase("LockRequests"         ) ? "" : "    ,sum([LockRequests])          *1.0 / nullif(sum([UsedCount]), 0)  as [AvgLockRequests] \n";
		String AvgLockWaits                 = !dummyRstm.hasColumnNoCase("LockWaits"            ) ? "" : "    ,sum([LockWaits])             *1.0 / nullif(sum([UsedCount]), 0)  as [AvgLockWaits] \n";

		// Chart columns
		String UsedCount__chart             = !dummyRstm.hasColumnNoCase("UsedCount"            ) ? "" : "    ,cast('' as varchar(512))                              as [UsedCount__chart] \n";
		String LogicalReads__chart          = !dummyRstm.hasColumnNoCase("LogicalReads"         ) ? "" : "    ,cast('' as varchar(512))                              as [LogicalReads__chart] \n";
		String LogicalReadsMb__chart        = !dummyRstm.hasColumnNoCase("LogicalReads"         ) ? "" : "    ,cast('' as varchar(512))                              as [LogicalReadsMb__chart] \n";
		String PhysicalReads__chart         = !dummyRstm.hasColumnNoCase("PhysicalReads"        ) ? "" : "    ,cast('' as varchar(512))                              as [PhysicalReads__chart] \n";
		String APFReads__chart              = !dummyRstm.hasColumnNoCase("APFReads"             ) ? "" : "    ,cast('' as varchar(512))                              as [APFReads__chart] \n";
		String PagesRead__chart             = !dummyRstm.hasColumnNoCase("PagesRead"            ) ? "" : "    ,cast('' as varchar(512))                              as [PagesRead__chart] \n";
		String PhysicalWrites__chart        = !dummyRstm.hasColumnNoCase("PhysicalWrites"       ) ? "" : "    ,cast('' as varchar(512))                              as [PhysicalWrites__chart] \n";
		String Operations__chart            = !dummyRstm.hasColumnNoCase("Operations"           ) ? "" : "    ,cast('' as varchar(512))                              as [Operations__chart] \n";
		String RowsInsUpdDel__chart         = !dummyRstm.hasColumnNoCase("RowsInsUpdDel"        ) ? "" : "    ,cast('' as varchar(512))                              as [RowsInsUpdDel__chart] \n";
		String Scans__chart                 = !dummyRstm.hasColumnNoCase("Scans"                ) ? "" : "    ,cast('' as varchar(512))                              as [Scans__chart] \n";
		String LockRequests__chart          = !dummyRstm.hasColumnNoCase("LockRequests"         ) ? "" : "    ,cast('' as varchar(512))                              as [LockRequests__chart] \n";
		String LockWaits__chart             = !dummyRstm.hasColumnNoCase("LockWaits"            ) ? "" : "    ,cast('' as varchar(512))                              as [LockWaits__chart] \n";
		String LockWaitTime__chart          = !dummyRstm.hasColumnNoCase("SharedLockWaitTime"   ) ? "" : "    ,cast('' as varchar(512))                              as [LockWaitTime__chart] \n"; 
		String SharedLockWaitTime__chart    = !dummyRstm.hasColumnNoCase("SharedLockWaitTime"   ) ? "" : "    ,cast('' as varchar(512))                              as [SharedLockWaitTime__chart] \n"; 
		String ExclusiveLockWaitTime__chart = !dummyRstm.hasColumnNoCase("ExclusiveLockWaitTime") ? "" : "    ,cast('' as varchar(512))                              as [ExclusiveLockWaitTime__chart] \n"; 
		String UpdateLockWaitTime__chart    = !dummyRstm.hasColumnNoCase("UpdateLockWaitTime"   ) ? "" : "    ,cast('' as varchar(512))                              as [UpdateLockWaitTime__chart] \n"; 

		// "other" columns
		String ObjectCacheDate__max         = !dummyRstm.hasColumnNoCase("ObjectCacheDate"      ) ? "" : "    ,max([ObjectCacheDate])                                as [ObjectCacheDate__max] \n"; 
		String LastOptSelectDate__max       = !dummyRstm.hasColumnNoCase("LastOptSelectDate"    ) ? "" : "    ,max([LastOptSelectDate])                              as [LastOptSelectDate__max] \n"; 
		String LastUsedDate__max            = !dummyRstm.hasColumnNoCase("LastUsedDate"         ) ? "" : "    ,max([LastUsedDate])                                   as [LastUsedDate__max] \n"; 

		// Table size columns
		String UsageInMb__max               = !dummyRstm.hasColumnNoCase("UsageInMb"            ) ? "" : "    ,max([UsageInMb])                                      as [UsageInMb__max] \n";
		String TabRowCount__max             = !dummyRstm.hasColumnNoCase("TabRowCount"          ) ? "" : "    ,max([TabRowCount])                                    as [TabRowCount__max] \n";
		String NumUsedPages__max            = !dummyRstm.hasColumnNoCase("NumUsedPages"         ) ? "" : "    ,max([NumUsedPages])                                   as [NumUsedPages__max] \n";
		String RowsPerPage__max             = !dummyRstm.hasColumnNoCase("RowsPerPage"          ) ? "" : "    ,max([RowsPerPage])                                    as [RowsPerPage__max] \n";
		String LockScheme__max              = !dummyRstm.hasColumnNoCase("LockScheme"           ) ? "" : "    ,max([LockScheme])                                     as [LockScheme__max] \n";
		String LockContPct__max             = !dummyRstm.hasColumnNoCase("LockContPct"          ) ? "" : "    ,max([LockContPct])                                    as [LockContPct__max] \n";

		// Reset some columns
		if (asePageSize == -1)
		{
			LogicalReadsMb__chart = "";
			LogicalReadsMb__sum   = "";
			AvgLogicalReadsMb     = "";
		}

		String andLockWaits = "";
		if (ReportType.LOCK_WAIT_TIME.equals(_reportType))
			andLockWaits = "  and [LockWaits] > 0 \n";

		String sql = getCmDiffColumnsAsSqlComment("CmObjectActivity")
				+ "select top " + topRows + " \n"
				+ "     [DBName]                                              as [DBName] \n"
				+ "    ,[ObjectName]                                          as [ObjectName] \n"
				+ "    ,[IndexName]                                           as [IndexName] \n"

				+ UsedCount__chart
				+ UsedCount__sum

				+ "    ,count(*)                                              as [samples_count] \n"
				+ "    ,sum(CASE WHEN [Remark] = '' THEN 0 ELSE 1 END)        as [Remark_cnt] \n"
				+ "    ,sum(CASE WHEN [Remark] = 'TabScan' THEN 1 ELSE 0 END) as [Remark_TabScan_cnt] \n"
				
				+ LogicalReads__chart
				+ LogicalReads__sum
				+ AvgLogicalReads

				+ PhysicalReads__chart
				+ PhysicalReads__sum
				+ AvgPhysicalReads

				+ APFReads__chart
				+ APFReads__sum
				+ AvgAPFReads

				+ PagesRead__chart
				+ PagesRead__sum
				+ AvgPagesRead
				+ IOSize1Page__sum 
				+ IOSize2Pages__sum
				+ IOSize4Pages__sum
				+ IOSize8Pages__sum

				+ PhysicalWrites__chart
				+ PhysicalWrites__sum
				+ AvgPhysicalWrites
				+ PagesWritten__sum
				
				+ Operations__chart
				+ Operations__sum
				+ AvgOperations

				+ RowsInsUpdDel__chart
				+ RowsInsUpdDel__sum
				+ AvgRowsInsUpdDel
				+ RowsInserted__sum
				+ Inserts__sum
				+ RowsDeleted__sum
				+ Deletes__sum
				+ RowsUpdated__sum
				+ Updates__sum
				
				+ Scans__chart
				+ Scans__sum
				+ AvgScans
				
				+ LockRequests__chart
				+ LockRequests__sum
				+ AvgLockRequests
				
				+ LockWaits__chart
				+ LockWaits__sum
				+ AvgLockWaits

				+ LockWaitTime__chart
				+ LockWaitTime__sum
				+ AvgLockWaitTime

				+ LockContPct__max
				+ LockScheme__max

				+ SharedLockWaitTime__chart
				+ SharedLockWaitTime__sum
				+ AvgSharedLockWaitTime

				+ ExclusiveLockWaitTime__chart
				+ ExclusiveLockWaitTime__sum
				+ AvgExclusiveLockWaitTime

				+ UpdateLockWaitTime__chart
				+ UpdateLockWaitTime__sum
				+ AvgUpdateLockWaitTime

				+ TabRowCount__max
				+ UsageInMb__max
				+ NumUsedPages__max
				+ RowsPerPage__max

				+ OptSelectCount__sum
				+ ObjectCacheDate__max
				+ LastOptSelectDate__max 
				+ LastUsedDate__max 
				
				+ LogicalReadsMb__chart
				+ LogicalReadsMb__sum
				+ AvgLogicalReadsMb

				+ "    ,min([CmSampleTime])                                   as [CmSampleTime_min] \n"
				+ "    ,max([CmSampleTime])                                   as [CmSampleTime_max] \n"
				+ "    ,cast('' as varchar(30))                               as [Duration] \n"
				+ "from [CmObjectActivity_diff] x \n"
				+ "where 1 = 1 \n"
				+ getReportPeriodSqlWhere()
				+ andLockWaits
				+ "group by [DBName], [ObjectName], [IndexName] \n"
				+ "having " + _sqlHavingCol + " > " + havingAbove + "\n"
				+ "order by " + _sqlOrderByCol + " desc \n"
				+ "";

		_shortRstm = executeQuery(conn, sql, false, "TopTableAccess");
		if (_shortRstm == null)
		{
			_shortRstm = ResultSetTableModel.createEmpty("TopTableAccess");
			return;
		}
		else
		{
			// Highlight sort column
			_shortRstm.setHighlightSortColumns(_orderByCol_noBrackets);

			// Describe the table
			setSectionDescription(_shortRstm);

			// Calculate Duration
			setDurationColumn(_shortRstm, "CmSampleTime_min", "CmSampleTime_max", "Duration");
			
			//--------------------------------------------------------------------------------
			// Sparkline/Mini Charts
			//--------------------------------------------------------------------------------

			String whereKeyColumn = "DBName, ObjectName, IndexName"; 

//			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
//					SparkLineParams.create       (DataSource.CounterModel)
//					.setHtmlChartColumnName      ("UsedCount__chart")
//					.setHtmlWhereKeyColumnName   (whereKeyColumn)
//					.setDbmsTableName            ("CmObjectActivity_diff")
//					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("UsedCount")
//					.setDbmsWhereKeyColumnName   (whereKeyColumn)
//					.validate()));
//
//			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
//					SparkLineParams.create       (DataSource.CounterModel)
//					.setHtmlChartColumnName      ("LogicalReads__chart")
//					.setHtmlWhereKeyColumnName   (whereKeyColumn)
//					.setDbmsTableName            ("CmObjectActivity_diff")
//					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("LogicalReads")
//					.setDbmsWhereKeyColumnName   (whereKeyColumn)
//					.validate()));
//
//			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
//					SparkLineParams.create       (DataSource.CounterModel)
//					.setHtmlChartColumnName      ("RowsInsUpdDel__chart")
//					.setHtmlWhereKeyColumnName   (whereKeyColumn)
//					.setDbmsTableName            ("CmObjectActivity_diff")
//					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("RowsInsUpdDel")
//					.setDbmsWhereKeyColumnName   (whereKeyColumn)
//					.validate()));
//
//		
//		
//		
//			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
//					SparkLineParams.create       (DataSource.CounterModel)
//					.setHtmlChartColumnName      ("LockWaits__chart")
//					.setHtmlWhereKeyColumnName   (whereKeyColumn)
//					.setDbmsTableName            ("CmObjectActivity_diff")
//					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("LockWaits")
//					.setDbmsWhereKeyColumnName   (whereKeyColumn)
//					.validate()));
//
//			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
//					SparkLineParams.create       (DataSource.CounterModel)
//					.setHtmlChartColumnName      ("LockWaitTime__chart")
//					.setHtmlWhereKeyColumnName   (whereKeyColumn)
//					.setDbmsTableName            ("CmObjectActivity_diff")
//					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("[SharedLockWaitTime] + [ExclusiveLockWaitTime] + [UpdateLockWaitTime]").setDbmsDataValueColumnNameIsExpression(true)
//					.setDbmsWhereKeyColumnName   (whereKeyColumn)
//					.validate()));
//
//			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
//					SparkLineParams.create       (DataSource.CounterModel)
//					.setHtmlChartColumnName      ("SharedLockWaitTime__chart")
//					.setHtmlWhereKeyColumnName   (whereKeyColumn)
//					.setDbmsTableName            ("CmObjectActivity_diff")
//					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("SharedLockWaitTime")
//					.setDbmsWhereKeyColumnName   (whereKeyColumn)
//					.validate()));
//
//			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
//					SparkLineParams.create       (DataSource.CounterModel)
//					.setHtmlChartColumnName      ("ExclusiveLockWaitTime__chart")
//					.setHtmlWhereKeyColumnName   (whereKeyColumn)
//					.setDbmsTableName            ("CmObjectActivity_diff")
//					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("ExclusiveLockWaitTime")   
//					.setDbmsWhereKeyColumnName   (whereKeyColumn)
//					.validate()));
//
//			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
//					SparkLineParams.create       (DataSource.CounterModel)
//					.setHtmlChartColumnName      ("UpdateLockWaitTime__chart")
//					.setHtmlWhereKeyColumnName   (whereKeyColumn)
//					.setDbmsTableName            ("CmObjectActivity_diff")
//					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("UpdateLockWaitTime")   
//					.setDbmsWhereKeyColumnName   (whereKeyColumn)
//					.validate()));

			if (StringUtil.hasValue(UsedCount__chart))
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("UsedCount__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmObjectActivity_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
					.setDbmsDataValueColumnName  ("UsedCount")
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.validate()));

			if (StringUtil.hasValue(LogicalReads__chart))
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("LogicalReads__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmObjectActivity_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("LogicalReads")
					.setDbmsDataValueColumnName  ("sum(1.0*[LogicalReads]) / nullif(sum([UsedCount]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.validate()));

			if (StringUtil.hasValue(LogicalReadsMb__chart))
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("LogicalReadsMb__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmObjectActivity_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("LogicalReads")
					.setDbmsDataValueColumnName  ("sum([LogicalReads]) * 1.0 / " + asePageSizeDivider + "  / nullif(sum([UsedCount]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.validate()));

			if (StringUtil.hasValue(PhysicalReads__chart))
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("PhysicalReads__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmObjectActivity_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("PhysicalReads")
					.setDbmsDataValueColumnName  ("sum(1.0*[PhysicalReads]) / nullif(sum([UsedCount]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.validate()));

			if (StringUtil.hasValue(APFReads__chart))
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("APFReads__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmObjectActivity_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("APFReads")
					.setDbmsDataValueColumnName  ("sum(1.0*[APFReads]) / nullif(sum([UsedCount]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.validate()));

			if (StringUtil.hasValue(PagesRead__chart))
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("PagesRead__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmObjectActivity_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("PagesRead")
					.setDbmsDataValueColumnName  ("sum(1.0*[PagesRead]) / nullif(sum([UsedCount]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.validate()));

			if (StringUtil.hasValue(PhysicalWrites__chart))
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("PhysicalWrites__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmObjectActivity_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("PhysicalWrites")
					.setDbmsDataValueColumnName  ("sum(1.0*[PhysicalWrites]) / nullif(sum([UsedCount]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.validate()));

			if (StringUtil.hasValue(Operations__chart))
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("Operations__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmObjectActivity_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("Operations")
					.setDbmsDataValueColumnName  ("sum(1.0*[Operations]) / nullif(sum([UsedCount]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.validate()));

			if (StringUtil.hasValue(RowsInsUpdDel__chart))
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("RowsInsUpdDel__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmObjectActivity_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("RowsInsUpdDel")
					.setDbmsDataValueColumnName  ("sum(1.0*[RowsInsUpdDel]) / nullif(sum([UsedCount]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.validate()));

			if (StringUtil.hasValue(Scans__chart))
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("Scans__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmObjectActivity_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("Scans")
					.setDbmsDataValueColumnName  ("sum(1.0*[Scans]) / nullif(sum([UsedCount]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.validate()));

			if (StringUtil.hasValue(LockRequests__chart))
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("LockRequests__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmObjectActivity_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("LockRequests")
					.setDbmsDataValueColumnName  ("sum(1.0*[LockRequests]) / nullif(sum([UsedCount]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.validate()));

			if (StringUtil.hasValue(LockWaits__chart))
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("LockWaits__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmObjectActivity_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("LockWaits")
					.setDbmsDataValueColumnName  ("sum(1.0*[LockWaits]) / nullif(sum([UsedCount]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.validate()));

			if (StringUtil.hasValue(LockWaitTime__chart))
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("LockWaitTime__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmObjectActivity_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("LockWaitTime")
					.setDbmsDataValueColumnName  ("sum(1.0*[SharedLockWaitTime]) + sum(1.0*[ExclusiveLockWaitTime]) + sum(1.0*[UpdateLockWaitTime]) / nullif(sum([UsedCount]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.validate()));

			if (StringUtil.hasValue(SharedLockWaitTime__chart))
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("SharedLockWaitTime__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmObjectActivity_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("SharedLockWaitTime")
					.setDbmsDataValueColumnName  ("sum(1.0*[SharedLockWaitTime]) / nullif(sum([UsedCount]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.validate()));

			if (StringUtil.hasValue(ExclusiveLockWaitTime__chart))
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("ExclusiveLockWaitTime__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmObjectActivity_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("ExclusiveLockWaitTime")
					.setDbmsDataValueColumnName  ("sum(1.0*[ExclusiveLockWaitTime]) / nullif(sum([UsedCount]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.validate()));

			if (StringUtil.hasValue(UpdateLockWaitTime__chart))
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("UpdateLockWaitTime__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmObjectActivity_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("UpdateLockWaitTime")
					.setDbmsDataValueColumnName  ("sum(1.0*[UpdateLockWaitTime]) / nullif(sum([UsedCount]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.validate()));

			
//			String UsedCount__chart             = !dummyRstm.hasColumnNoCase("UsedCount"            ) ? "" : "    ,cast('' as varchar(512))                           as [UsedCount__chart] \n";
//			String LogicalReads__chart          = !dummyRstm.hasColumnNoCase("LogicalReads"         ) ? "" : "    ,cast('' as varchar(512))                           as [LogicalReads__chart] \n";
//			String PhysicalReads__chart         = !dummyRstm.hasColumnNoCase("PhysicalReads"        ) ? "" : "    ,cast('' as varchar(512))                           as [PhysicalReads__chart] \n";
//			String APFReads__chart              = !dummyRstm.hasColumnNoCase("APFReads"             ) ? "" : "    ,cast('' as varchar(512))                           as [APFReads__chart] \n";
//			String PagesRead__chart             = !dummyRstm.hasColumnNoCase("PagesRead"            ) ? "" : "    ,cast('' as varchar(512))                           as [PagesRead__chart] \n";
//			String PhysicalWrites__chart        = !dummyRstm.hasColumnNoCase("PhysicalWrites"       ) ? "" : "    ,cast('' as varchar(512))                           as [PhysicalWrites__chart] \n";
//			String Operations__chart            = !dummyRstm.hasColumnNoCase("Operations"           ) ? "" : "    ,cast('' as varchar(512))                           as [Operations__chart] \n";
//			String RowsInsUpdDel__chart         = !dummyRstm.hasColumnNoCase("RowsInsUpdDel"        ) ? "" : "    ,cast('' as varchar(512))                           as [RowsInsUpdDel__chart] \n";
//			String Scans__chart                 = !dummyRstm.hasColumnNoCase("Scans"                ) ? "" : "    ,cast('' as varchar(512))                           as [Scans__chart] \n";
//			String LockRequests__chart          = !dummyRstm.hasColumnNoCase("LockRequests"         ) ? "" : "    ,cast('' as varchar(512))                           as [LockRequests__chart] \n";
//			String LockWaits__chart             = !dummyRstm.hasColumnNoCase("LockWaits"            ) ? "" : "    ,cast('' as varchar(512))                           as [LockWaits__chart] \n";
//			String LockWaitTime__chart          = !dummyRstm.hasColumnNoCase("SharedLockWaitTime"   ) ? "" : "    ,cast('' as varchar(512))                           as [LockWaitTime__chart] \n"; 
//			String SharedLockWaitTime__chart    = !dummyRstm.hasColumnNoCase("SharedLockWaitTime"   ) ? "" : "    ,cast('' as varchar(512))                           as [SharedLockWaitTime__chart] \n"; 
//			String ExclusiveLockWaitTime__chart = !dummyRstm.hasColumnNoCase("ExclusiveLockWaitTime") ? "" : "    ,cast('' as varchar(512))                           as [ExclusiveLockWaitTime__chart] \n"; 
//			String UpdateLockWaitTime__chart    = !dummyRstm.hasColumnNoCase("UpdateLockWaitTime"   ) ? "" : "    ,cast('' as varchar(512))                           as [UpdateLockWaitTime__chart] \n"; 

			
			//----------------------------------------------------
			// Create a SQL-Details ResultSet based on values in _shortRstm
			//----------------------------------------------------
			SimpleResultSet srs = new SimpleResultSet();

			srs.addColumn("DBName",     Types.VARCHAR,       60, 0);
			srs.addColumn("ObjectName", Types.VARCHAR,       60, 0);
			srs.addColumn("IndexName",  Types.VARCHAR,       60, 0);
			srs.addColumn("sparklines", Types.VARCHAR,      512, 0); 
			srs.addColumn("otherInfo",  Types.VARCHAR,      512, 0); 
			srs.addColumn("indexInfo",  Types.VARCHAR,      512, 0); 

			// Position in the "source" _shortRstm table (values we will fetch)
			int pos_DBName     = _shortRstm.findColumn("DBName");
			int pos_ObjectName = _shortRstm.findColumn("ObjectName");
			int pos_IndexName  = _shortRstm.findColumn("IndexName");

			ColumnCopyRender msToHMS    = HtmlTableProducer.MS_TO_HMS;
			ColumnCopyRender oneDecimal = HtmlTableProducer.ONE_DECIMAL;
			
			HtmlTableProducer htp = new HtmlTableProducer(_shortRstm, "dsr-sub-table-chart");
			htp.setTableHeaders("Charts at 10 minute interval", "Total;style='text-align:right!important'", "Avg per call;style='text-align:right!important'", "", "");
			if (StringUtil.hasValue(UsedCount__chart            )) htp.add("used-cnt"    , new ColumnCopyRow().add( new ColumnCopyDef("UsedCount__chart"            ) ).add(new ColumnCopyDef("UsedCount__sum").setColBold()        ).addEmptyCol()                                                              .addEmptyCol()               .addRowLabelCopy() );
			if (StringUtil.hasValue(LogicalReads__chart         )) htp.add("l-read"      , new ColumnCopyRow().add( new ColumnCopyDef("LogicalReads__chart"         ) ).add(new ColumnCopyDef("LogicalReads__sum"                  )).add(new ColumnCopyDef("AvgLogicalReads"         , oneDecimal).setColBold()).add(new ColumnStatic("pgs")).addRowLabelCopy() );
			if (StringUtil.hasValue(LogicalReadsMb__chart       )) htp.add("l-read-mb"   , new ColumnCopyRow().add( new ColumnCopyDef("LogicalReadsMb__chart"       ) ).add(new ColumnCopyDef("LogicalReadsMb__sum"                )).add(new ColumnCopyDef("AvgLogicalReadsMb"       , oneDecimal).setColBold()).add(new ColumnStatic("mb" )).addRowLabelCopy() );
			if (StringUtil.hasValue(PhysicalReads__chart        )) htp.add("p-read"      , new ColumnCopyRow().add( new ColumnCopyDef("PhysicalReads__chart"        ) ).add(new ColumnCopyDef("PhysicalReads__sum"                 )).add(new ColumnCopyDef("AvgPhysicalReads"        , oneDecimal).setColBold()).add(new ColumnStatic("pgs")).addRowLabelCopy() );
			if (StringUtil.hasValue(APFReads__chart             )) htp.add("apf-read"    , new ColumnCopyRow().add( new ColumnCopyDef("APFReads__chart"             ) ).add(new ColumnCopyDef("APFReads__sum"                      )).add(new ColumnCopyDef("AvgAPFReads"             , oneDecimal).setColBold()).add(new ColumnStatic("pgs")).addRowLabelCopy() );
			if (StringUtil.hasValue(PagesRead__chart            )) htp.add("pgs-read"    , new ColumnCopyRow().add( new ColumnCopyDef("PagesRead__chart"            ) ).add(new ColumnCopyDef("PagesRead__sum"                     )).add(new ColumnCopyDef("AvgPagesRead"            , oneDecimal).setColBold()).add(new ColumnStatic("pgs")).addRowLabelCopy() );
			if (StringUtil.hasValue(PhysicalWrites__chart       )) htp.add("p-writes"    , new ColumnCopyRow().add( new ColumnCopyDef("PhysicalWrites__chart"       ) ).add(new ColumnCopyDef("PhysicalWrites__sum"                )).add(new ColumnCopyDef("AvgPhysicalWrites"       , oneDecimal).setColBold()).add(new ColumnStatic("pgs")).addRowLabelCopy() );
			if (StringUtil.hasValue(Operations__chart           )) htp.add("operations"  , new ColumnCopyRow().add( new ColumnCopyDef("Operations__chart"           ) ).add(new ColumnCopyDef("Operations__sum"                    )).add(new ColumnCopyDef("AvgOperations"           , oneDecimal).setColBold()).add(new ColumnStatic("#"  )).addRowLabelCopy() );
			if (StringUtil.hasValue(RowsInsUpdDel__chart        )) htp.add("row-iud"     , new ColumnCopyRow().add( new ColumnCopyDef("RowsInsUpdDel__chart"        ) ).add(new ColumnCopyDef("RowsInsUpdDel__sum"                 )).add(new ColumnCopyDef("AvgRowsInsUpdDel"        , oneDecimal).setColBold()).add(new ColumnStatic("#"  )).addRowLabelCopy() );
			if (StringUtil.hasValue(Scans__chart                )) htp.add("scans"       , new ColumnCopyRow().add( new ColumnCopyDef("Scans__chart"                ) ).add(new ColumnCopyDef("Scans__sum"                         )).add(new ColumnCopyDef("AvgScans"                , oneDecimal).setColBold()).add(new ColumnStatic("#"  )).addRowLabelCopy() );
			if (StringUtil.hasValue(LockRequests__chart         )) htp.add("lock-req"    , new ColumnCopyRow().add( new ColumnCopyDef("LockRequests__chart"         ) ).add(new ColumnCopyDef("LockRequests__sum"                  )).add(new ColumnCopyDef("AvgLockRequests"         , oneDecimal).setColBold()).add(new ColumnStatic("#"  )).addRowLabelCopy() );
			if (StringUtil.hasValue(LockWaits__chart            )) htp.add("lock-waits"  , new ColumnCopyRow().add( new ColumnCopyDef("LockWaits__chart"            ) ).add(new ColumnCopyDef("LockWaits__sum"                     )).add(new ColumnCopyDef("AvgLockWaits"            , oneDecimal).setColBold()).add(new ColumnStatic("#"  )).addRowLabelCopy() );
			if (StringUtil.hasValue(LockWaitTime__chart         )) htp.add("lock-wtime"  , new ColumnCopyRow().add( new ColumnCopyDef("LockWaitTime__chart"         ) ).add(new ColumnCopyDef("LockWaitTime__sum"         , msToHMS)).add(new ColumnCopyDef("AvgLockWaitTime"         , oneDecimal).setColBold()).add(new ColumnStatic("ms" )).addRowLabelCopy() );
			if (StringUtil.hasValue(SharedLockWaitTime__chart   )) htp.add("s-lock-wtime", new ColumnCopyRow().add( new ColumnCopyDef("SharedLockWaitTime__chart"   ) ).add(new ColumnCopyDef("SharedLockWaitTime__sum"   , msToHMS)).add(new ColumnCopyDef("AvgSharedLockWaitTime"   , oneDecimal).setColBold()).add(new ColumnStatic("ms" )).addRowLabelCopy() );
			if (StringUtil.hasValue(ExclusiveLockWaitTime__chart)) htp.add("x-lock-wtime", new ColumnCopyRow().add( new ColumnCopyDef("ExclusiveLockWaitTime__chart") ).add(new ColumnCopyDef("ExclusiveLockWaitTime__sum", msToHMS)).add(new ColumnCopyDef("AvgExclusiveLockWaitTime", oneDecimal).setColBold()).add(new ColumnStatic("ms" )).addRowLabelCopy() );
			if (StringUtil.hasValue(UpdateLockWaitTime__chart   )) htp.add("u-lock-wtime", new ColumnCopyRow().add( new ColumnCopyDef("UpdateLockWaitTime__chart"   ) ).add(new ColumnCopyDef("UpdateLockWaitTime__sum"   , msToHMS)).add(new ColumnCopyDef("AvgUpdateLockWaitTime"   , oneDecimal).setColBold()).add(new ColumnStatic("ms" )).addRowLabelCopy() );
			htp.validate();

			// Filter out some rows...
			htp.setRowFilter(new HtmlTableProducer.RowFilter()
			{
				@Override
				public boolean include(ResultSetTableModel rstm, int rstmRow, String rowKey)
				{
					if (StringUtil.equalsAny(rowKey, "pgs-read"))
					{
						return rstm.hasColumn("PhysicalWrites__sum") && rstm.getValueAsInteger(rstmRow, "PhysicalWrites__sum") > 1;
					}
					
					if (StringUtil.equalsAny(rowKey, "p-writes"))
					{
						return rstm.hasColumn("PhysicalWrites__sum") && rstm.getValueAsInteger(rstmRow, "PhysicalWrites__sum") > 1;
					}
					
					if (StringUtil.equalsAny(rowKey, "row-iud"))
					{
						return rstm.hasColumn("RowsInsUpdDel__sum") && rstm.getValueAsInteger(rstmRow, "RowsInsUpdDel__sum") > 1;
					}
					
					if (StringUtil.equalsAny(rowKey, "lock-waits", "lock-wtime", "s-lock-wtime", "x-lock-wtime", "u-lock-wtime"))
					{
						return rstm.hasColumn("LockWaits__sum") && rstm.getValueAsInteger(rstmRow, "LockWaits__sum") > 1;
					}

					return true;
				}
			});
			
//			LinkedHashMap<String, String> otherInfoMap = new LinkedHashMap<>();
//			otherInfoMap.put("TabRowCount__max"    , "Table Rowcount");
//			otherInfoMap.put("UsageInMb__max"      , "Table Used MB");
//			otherInfoMap.put("NumUsedPages__max"   , "Table Used Pages");
//			otherInfoMap.put("RowsPerPage__max"    , "Number of Rows per Page");
//			otherInfoMap.put("ObjectCacheDate__max", "Table/Index Cache Date");
//			otherInfoMap.put("LastUsedDate__max"   , "Table/Index Last Used Date");

//			// Get ASE Page Size
//			int asePageSize = -1;
//			try	{ asePageSize = getAsePageSizeFromMonDdlStorage(conn); }
//			catch (SQLException ex) { }
			

			// loop "data table" and create "sql table" 
			if (pos_DBName >= 0 && pos_ObjectName >= 0 && pos_IndexName >= 0)
			{
				for (int r=0; r<_shortRstm.getRowCount(); r++)
				{
					String DBName     = _shortRstm.getValueAsString(r, pos_DBName);
					String ObjectName = _shortRstm.getValueAsString(r, pos_ObjectName);
					String IndexName  = _shortRstm.getValueAsString(r, pos_IndexName);

					String sparklines = htp.getHtmlTextForRow(r);
//					String otherInfo  = "-FIXME-";//htp.getHtmlTextForRow(r);
//					String otherInfo  = _shortRstm.createHtmlKeyValueTableFromRow(r, otherInfoMap, "dsr-sub-table-other-info");

					// Get "sp_spaceused" information from "Mon DDL Storage"
					LinkedHashMap<String, String> otherInfoMap = new LinkedHashMap<>();
					String otherIndexInfoTable = "";
					String otherInfo = "--not-found-in-ddl-storage--";
					NumberFormat nf = NumberFormat.getInstance();

					Set<AseTableInfo> aseTableInfoSet = null;
					try	{ aseTableInfoSet = getTableInformationFromMonDdlStorage(conn, DBName, null, ObjectName); }
					catch (SQLException ex) { otherInfo = "Mon DDL Storage lookup, ERROR: " + ex.getMessage(); }
					
					if (aseTableInfoSet != null && !aseTableInfoSet.isEmpty())
					{
						AseTableInfo aseTableInfo = aseTableInfoSet.iterator().next(); // Get first entry from iterator (simulating: list.get(0))
						if (asePageSize != -1)
							otherInfoMap.put("ASE Page Size"  , "" + asePageSize);

//						otherInfoMap.put("DB Name"        ,            DBName  );
						otherInfoMap.put("Table Name"     ,            aseTableInfo.getFullTableName()  );
						otherInfoMap.put("Table Rowcount" , nf.format( aseTableInfo.getRowTotal()      ));
						otherInfoMap.put("Table Used MB"  , nf.format( aseTableInfo.getSizeMb()        ));
						otherInfoMap.put("Table Data MB"  , nf.format( aseTableInfo.getDataMb()        ));
						otherInfoMap.put("Table Index MB" , nf.format( aseTableInfo.getIndexMb()       ));
						otherInfoMap.put("Lock Schema"    ,            aseTableInfo.getLockScheme()     );
						otherInfoMap.put("Index Count"    , aseTableInfo.getIndexCount() + (aseTableInfo.getIndexCount() > 0 ? "" : " <b><font color='red'>&lt;&lt;-- Warning NO index</font></b>") );

						otherIndexInfoTable = getIndexInfoAsHtmlTable(aseTableInfo, "dsr-sub-table-other-info");
//						if (aseTableInfo.getIndexCount() > 0)
//						{
//							for (AseIndexInfo aseIndexInfo : aseTableInfo.getIndexList())
//							{
//								otherInfoMap.put("Index Keys: " + aseIndexInfo.getIndexName(), aseIndexInfo.getKeysStr());
//							}
//						}

						otherInfoMap.put("Table/Index Cache Date"     , !_shortRstm.hasColumn("ObjectCacheDate__max") ? "-unknown-" : _shortRstm.getValueAsString(r, "ObjectCacheDate__max"));
						otherInfoMap.put("Table/Index Last Used Date" , !_shortRstm.hasColumn("LastUsedDate__max")    ? "-unknown-" : _shortRstm.getValueAsString(r, "LastUsedDate__max"));
					}

					boolean getRstmTabInfo = true;
					if (getRstmTabInfo)
					{
						Integer val_TabRowCount__max  = !_shortRstm.hasColumn("TabRowCount__max")  ? -1 : _shortRstm.getValueAsInteger(r, "TabRowCount__max");
						Integer val_UsageInMb__max    = !_shortRstm.hasColumn("UsageInMb__max")    ? -1 : _shortRstm.getValueAsInteger(r, "UsageInMb__max");
						Integer val_NumUsedPages__max = !_shortRstm.hasColumn("NumUsedPages__max") ? -1 : _shortRstm.getValueAsInteger(r, "NumUsedPages__max");
						Integer val_RowsPerPage__max  = !_shortRstm.hasColumn("RowsPerPage__max")  ? -1 : _shortRstm.getValueAsInteger(r, "RowsPerPage__max");
						
						if (val_TabRowCount__max  != -1) otherInfoMap.put("TabRowCount__max" , nf.format(val_TabRowCount__max )); 
						if (val_UsageInMb__max    != -1) otherInfoMap.put("UsageInMb__max"   , nf.format(val_UsageInMb__max   ));
						if (val_NumUsedPages__max != -1) otherInfoMap.put("NumUsedPages__max", nf.format(val_NumUsedPages__max));
						if (val_RowsPerPage__max  !=  0) otherInfoMap.put("RowsPerPage__max" , nf.format(val_RowsPerPage__max ));
					}

					if ( ! otherInfoMap.isEmpty() )
					{
						otherInfo = HtmlTableProducer.createHtmlTable(otherInfoMap, "dsr-sub-table-other-info", true);
//						otherInfo += otherIndexInfoTable;
					}

					
					// add record to SimpleResultSet
					srs.addRow(DBName, ObjectName, IndexName, sparklines, otherInfo, otherIndexInfoTable);
				}
			}

			// GET SQLTEXT (only)
			try
			{
				// Note the 'srs' is populated when reading above ResultSet from query
				_sparklineRstm = createResultSetTableModel(srs, "Sparkline info", null, false); // DO NOT TRUNCATE COLUMNS
				srs.close();
			}
			catch (SQLException ex)
			{
				setProblemException(ex);
	
				_sparklineRstm = ResultSetTableModel.createEmpty("Sparkline info");
				_logger.warn("Problems getting 'Sparkline info': " + ex);
			}
		}
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
				"Table/Index " + getSubjectSubType() + " (ordered by: " + _orderByCol_noBrackets + ") <br>" +
				"<br>" +
				(ReportType.LOGICAL_READS.equals(_reportType) 
						? "What tables/indexes is accessed the most <br>" 
						: "What tables/indexes do have locking conflicts <br>") +
				"<br>" +
				"ASE Source table is 'master.dbo.monOpenObjectActivity'. <br>" +
				"PCS Source table is 'CmObjectActivity_diff'. (PCS = Persistent Counter Store) <br>" +
				"<br>" +
				"The report <i>summarizes</i> (min/max/count/sum/avg) all entries/samples from the <i>source_DIFF</i> table. <br>" +
				"Typically the column name <i>postfix</i> will tell you what aggregate function was used. <br>" +
				"");

		// Columns description
		rstm.setColumnDescription("CmSampleTime_min"          , "First entry was sampled.");
		rstm.setColumnDescription("CmSampleTime_max"          , "Last entry was sampled.");
		rstm.setColumnDescription("Duration"                  , "Difference between first/last sample");
		rstm.setColumnDescription("DBName"                    , "Database name");
		rstm.setColumnDescription("ObjectName"                , "Table or Index Name");
		rstm.setColumnDescription("IndexName"                 , "DATA if it's DATA Pages access. otherwise the IndexName");
		
		rstm.setColumnDescription("UsedCount__sum"             , "");
		rstm.setColumnDescription("samples_count"             , "");
		rstm.setColumnDescription("Remark_cnt"                , "Number of times the column 'Remark' is NOT empty");
		rstm.setColumnDescription("Remark_TabScan_cnt"        , "Number of times the column 'Remark' contained 'TabScan'");
		rstm.setColumnDescription("LogicalReads__sum"          , "Number of LogicalReads in the period duration");
		rstm.setColumnDescription("PhysicalReads__sum"         , "Number of PhysicalReads in the period duration");
		rstm.setColumnDescription("APFReads__sum"              , "Number of APFReads in the period duration (NOTE: APF is Async Pre Fetch, and is likly a Physical read)");
		rstm.setColumnDescription("PagesRead__sum"             , "Number of PagesRead in the period duration (NOTE: if it's a 8 Page read (IOSize8Pages), then this will be incremented 8 times, one for each 1 page read in the multi page read)");
		rstm.setColumnDescription("IOSize1Page__sum"           , "Number of SINGLE page reads");
		rstm.setColumnDescription("IOSize2Pages__sum"          , "Number of TWO (2) pages reads in the same IO Operation");
		rstm.setColumnDescription("IOSize4Pages__sum"          , "Number of FOUR (4) pages reads in the same IO Operation");
		rstm.setColumnDescription("IOSize8Pages__sum"          , "Number of EIGHT (8)  pages reads in the same IO Operation");
		rstm.setColumnDescription("PhysicalWrites__sum"        , "Number of Physical Writes");
		rstm.setColumnDescription("PagesWritten__sum"          , "Number of Pages Written to disk (If it uses LARGE Page IO, then this will be higher than PhyscalWrites)");
		rstm.setColumnDescription("Operations__sum"            , "");
		rstm.setColumnDescription("RowsInsUpdDel__sum"         , "Number of Insert/updates/deletes RECORDS that has been done");
		rstm.setColumnDescription("RowsInserted__sum"          , "Number of Insert RECORDS that has been done");
		rstm.setColumnDescription("RowsDeleted__sum"           , "Number of Deletes RECORDS that has been done");
		rstm.setColumnDescription("RowsUpdated__sum"           , "Number of Updates RECORDS that has been done");
		rstm.setColumnDescription("Inserts__sum"               , "Number of Insert Operations");
		rstm.setColumnDescription("Updates__sum"               , "Number of Update Operations");
		rstm.setColumnDescription("Deletes__sum"               , "Number of Delete Operations");
		rstm.setColumnDescription("Scans__sum"                 , "");
		rstm.setColumnDescription("TabRowCount_max"           , " (if available)");
		rstm.setColumnDescription("UsageInMb_max"             , " (if available)");
		rstm.setColumnDescription("NumUsedPages_max"          , " (if available)");
		rstm.setColumnDescription("RowsPerPage_max"           , " (if available)");
		rstm.setColumnDescription("LockScheme]"               , "what's the tables locking strategy 'allpages', 'datarows' or 'datapages'");
		rstm.setColumnDescription("LockRequests__sum"          , "Number of lock REQUESTS done");
		rstm.setColumnDescription("LockWaits__sum"             , "Number of lock WAITS done");
		rstm.setColumnDescription("LockContPct_max"           , "Max Locking CONTENTENTION on this table");
		rstm.setColumnDescription("SharedLockWaitTime__sum"    , "Wait TIME during shared locks");
		rstm.setColumnDescription("ExclusiveLockWaitTime__sum" , "Wait TIME during exlusive locks");
		rstm.setColumnDescription("UpdateLockWaitTime__sum"    , "Wait TIME during update locks");
		rstm.setColumnDescription("OptSelectCount__sum"        , "");
		rstm.setColumnDescription("ObjectCacheDate_max"       , "");
		rstm.setColumnDescription("LastOptSelectDate_max"     , "");
		rstm.setColumnDescription("LastUsedDate_max"          , "");
	}
}
