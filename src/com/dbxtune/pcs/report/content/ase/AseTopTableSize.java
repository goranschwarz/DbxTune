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
import java.util.LinkedHashMap;

import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.pcs.report.content.postgres.PostgresAbstract;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;

public class AseTopTableSize
extends PostgresAbstract
{
//	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private ResultSetTableModel _shortRstm;

	public AseTopTableSize(DailySummaryReportAbstract reportingInstance)
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
			sb.append("Possibly 'Sample Table Row Count' is <b>disabled</b> check option <code>CmObjectActivity.sample.TabRowCount = true|false</code> <br>\n");
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
			int sumTotalLobMb   = 0;
			for (int r=0; r<_shortRstm.getRowCount(); r++)
			{
				sumTotalMb      += _shortRstm.getValueAsInteger(r, "TotalUsageInMb");
				sumTotalDataMb  += _shortRstm.getValueAsInteger(r, "DataUsageInMb");
				sumTotalIndexMb += _shortRstm.getValueAsInteger(r, "IndexUsageInMb");
				sumTotalLobMb   += _shortRstm.getValueAsInteger(r, "LOBUsageInMb");
			}

			LinkedHashMap<String, Object> summaryMap = new LinkedHashMap<>();
			summaryMap.put("Sum Size in MB",   sumTotalMb);
			summaryMap.put("Sum Data in MB",   sumTotalDataMb);
			summaryMap.put("Sum Index in MB",  sumTotalIndexMb);
			summaryMap.put("Sum LOB in MB",    sumTotalLobMb + "&emsp;&emsp;&emsp;<i>(LOB = 'text' or 'image' datatype, that lives OffRow -- Not in same space as the table data).</i>");
			
			sb.append("<br>\n");
			sb.append(StringUtil.toHtmlTable(summaryMap));
			sb.append("<br>\n");
		}
	}

	@Override
	public String getSubject()
	{
		return "Top TABLE Size in any database (order by: TotalUsageInMb)";
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
				"Top Tables in size are presented here (ordered by: TotalUsageInMb) <br>" +
				"");

		// Columns description
//		rstm.setColumnDescription("dbname"                     , "Database name this table is located in.");
//		rstm.setColumnDescription("table_schema"               , "Schema name this table is located in.");
//		rstm.setColumnDescription("table_name"                 , "Name of the table.");
//
//		rstm.setColumnDescription("total_mb"                   , "MB of data + index + toast");
//		rstm.setColumnDescription("row_estimate"               , "Number of rows in table");
//		rstm.setColumnDescription("data_mb"                    , "Data size in MB");
//		rstm.setColumnDescription("index_mb"                   , "Index size in MB");
//		rstm.setColumnDescription("toast_mb"                   , "TOAST () size in MB (TOAST = The Oversized Attribute Storage Technique), or in short 'large rows that spans pages'.");
//		rstm.setColumnDescription("oid"                        , "ID number, which can be found at the OS, in the 'data' directory. for the database #### (datid).");
	}

	@Override
	public String[] getMandatoryTables()
	{
		return new String[] { "CmObjectActivity_abs" };
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		int topRows = 2 * getTopRows();
//		int topRows = localConf.getIntProperty(this.getClass().getSimpleName()+".top", 30);

		String dummySql = "select * from [CmObjectActivity_abs] where 1 = 2";
		ResultSetTableModel dummyRstm = executeQuery(conn, dummySql, true, "metadata");

		
		String Inserts               = !dummyRstm.hasColumnNoCase("Inserts"              ) ? "" : "    ,max([Inserts])               as [Inserts] \n";
		String Updates               = !dummyRstm.hasColumnNoCase("Updates"              ) ? "" : "    ,max([Updates])               as [Updates] \n";
		String Deletes               = !dummyRstm.hasColumnNoCase("Deletes"              ) ? "" : "    ,max([Deletes])               as [Deletes] \n";

		String RowsInsUpdDel         = !dummyRstm.hasColumnNoCase("RowsInsUpdDel"        ) ? "" : "    ,max([RowsInsUpdDel])         as [RowsInsUpdDel] \n";
		String RowsInserted          = !dummyRstm.hasColumnNoCase("RowsInserted"         ) ? "" : "    ,max([RowsInserted])          as [RowsInserted] \n";
		String RowsUpdated           = !dummyRstm.hasColumnNoCase("RowsUpdated"          ) ? "" : "    ,max([RowsUpdated])           as [RowsUpdated] \n";
		String RowsDeleted           = !dummyRstm.hasColumnNoCase("RowsDeleted"          ) ? "" : "    ,max([RowsDeleted])           as [RowsDeleted] \n";

		String LockRequests          = !dummyRstm.hasColumnNoCase("LockRequests"         ) ? "" : "    ,max([LockRequests])          as [LockRequests] \n";
		String LockWaits             = !dummyRstm.hasColumnNoCase("LockWaits"            ) ? "" : "    ,max([LockWaits])             as [LockWaits] \n";
		String LockContPct           = !dummyRstm.hasColumnNoCase("LockContPct"          ) ? "" : "    ,max([LockContPct])           as [LockContPct] \n";

		String SharedLockWaitTime    = !dummyRstm.hasColumnNoCase("SharedLockWaitTime"   ) ? "" : "    ,max([SharedLockWaitTime])    as [SharedLockWaitTime] \n";
		String ExclusiveLockWaitTime = !dummyRstm.hasColumnNoCase("ExclusiveLockWaitTime") ? "" : "    ,max([ExclusiveLockWaitTime]) as [ExclusiveLockWaitTime] \n";
		String UpdateLockWaitTime    = !dummyRstm.hasColumnNoCase("UpdateLockWaitTime"   ) ? "" : "    ,max([UpdateLockWaitTime])    as [UpdateLockWaitTime] \n";
		String NumLevel0Waiters      = !dummyRstm.hasColumnNoCase("NumLevel0Waiters"     ) ? "" : "    ,max([NumLevel0Waiters])      as [NumLevel0Waiters] \n";
		String AvgLevel0WaitTime     = !dummyRstm.hasColumnNoCase("AvgLevel0WaitTime"    ) ? "" : "    ,max([AvgLevel0WaitTime])     as [AvgLevel0WaitTime] \n";

		String LrPerScan             = !dummyRstm.hasColumnNoCase("LrPerScan"            ) ? "" : "    ,max([LrPerScan])             as [LrPerScan] \n";
		String LogicalReads          = !dummyRstm.hasColumnNoCase("LogicalReads"         ) ? "" : "    ,max([LogicalReads])          as [LogicalReads] \n";
		String PhysicalReads         = !dummyRstm.hasColumnNoCase("PhysicalReads"        ) ? "" : "    ,max([PhysicalReads])         as [PhysicalReads] \n";
		String APFReads              = !dummyRstm.hasColumnNoCase("APFReads"             ) ? "" : "    ,max([APFReads])              as [APFReads] \n";
		String PagesRead             = !dummyRstm.hasColumnNoCase("PagesRead"            ) ? "" : "    ,max([PagesRead])             as [PagesRead] \n";
		String IOSize1Page           = !dummyRstm.hasColumnNoCase("IOSize1Page"          ) ? "" : "    ,max([IOSize1Page])           as [IOSize1Page] \n";
		String IOSize2Pages          = !dummyRstm.hasColumnNoCase("IOSize2Pages"         ) ? "" : "    ,max([IOSize2Pages])          as [IOSize2Pages] \n";
		String IOSize4Pages          = !dummyRstm.hasColumnNoCase("IOSize4Pages"         ) ? "" : "    ,max([IOSize4Pages])          as [IOSize4Pages] \n";
		String IOSize8Pages          = !dummyRstm.hasColumnNoCase("IOSize8Pages"         ) ? "" : "    ,max([IOSize8Pages])          as [IOSize8Pages] \n";
		
		String PhysicalWrites        = !dummyRstm.hasColumnNoCase("PhysicalWrites"       ) ? "" : "    ,max([PhysicalWrites])        as [PhysicalWrites] \n";
		String PagesWritten          = !dummyRstm.hasColumnNoCase("PagesWritten"         ) ? "" : "    ,max([PagesWritten])          as [PagesWritten] \n";

		String SessionSampleTime     = !dummyRstm.hasColumnNoCase("SessionSampleTime"    ) ? "" : "    ,max([SessionSampleTime])     as [SessionSampleTime] \n";
		String ObjectCacheDate       = !dummyRstm.hasColumnNoCase("ObjectCacheDate"      ) ? "" : "    ,max([ObjectCacheDate])       as [ObjectCacheDate] \n";
		
		String sql = ""
			+ "select top " + topRows + " \n"
			+ "     [DBName] \n"
			+ "    ,[ObjectName] \n"
			+ "    ,max([TabRowCount])                                                      as [TabRowCount] \n"
			+ "    ,sum([UsageInMb])                                                        as [TotalUsageInMb] \n"
			+ "    ,sum(case when [IndexID] in (0, 1)          then [UsageInMb] else 0 end) as [DataUsageInMb] \n"
			+ "    ,sum(case when [IndexID] not in (0, 1, 255) then [UsageInMb] else 0 end) as [IndexUsageInMb] \n"
			+ "    ,sum(case when [IndexID] = 255              then [UsageInMb] else 0 end) as [LOBUsageInMb] \n"
			+ " \n"
			+ "    ,'   --- extra info -->>   '                                             as [extra-info] \n"
			+ "    ,datediff(day, max([ObjectCacheDate]), CURRENT_TIMESTAMP)                as [valuesOnRightIsSinceDays] \n"
			+ " \n"
			+ Inserts
			+ Updates
			+ Deletes
			+ " \n"
			+ RowsInsUpdDel
			+ RowsInserted 
			+ RowsUpdated  
			+ RowsDeleted  
			+ " \n"
			+ LockRequests
			+ LockWaits
			+ LockContPct
			+ " \n"
			+ SharedLockWaitTime   
			+ ExclusiveLockWaitTime
			+ UpdateLockWaitTime   
			+ NumLevel0Waiters     
			+ AvgLevel0WaitTime    
			+ " \n"
			+ LrPerScan    
			+ LogicalReads 
			+ PhysicalReads
			+ APFReads     
			+ PagesRead    
			+ IOSize1Page  
			+ IOSize2Pages 
			+ IOSize4Pages 
			+ IOSize8Pages 
			+ " \n"
			+ PhysicalWrites
			+ PagesWritten  
			+ " \n"
			+ SessionSampleTime
			+ ObjectCacheDate
			+ " \n"
			+ "from [CmObjectActivity_abs] \n"
			+ "where [SessionSampleTime] = ( \n"
			+ "    select max([SessionSampleTime]) \n"
			+ "    from [CmObjectActivity_abs] \n"
			+ "    where [UsageInMb] > 0 \n"
			+ ") \n"
			+ "group by [DBName], [ObjectName] \n"
			+ "order by sum([UsageInMb]) desc \n"
			+ "";

		_shortRstm = executeQuery(conn, sql, false, "AseTableSize");
		if (_shortRstm == null)
		{
			_shortRstm = ResultSetTableModel.createEmpty("AseTableSize");
			return;
		}
		else
		{
			// Highlight sort column
			_shortRstm.setHighlightSortColumns("TotalUsageInMb");

			// Describe the table
			setSectionDescription(_shortRstm);
		}
	}
}
