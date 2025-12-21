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
import java.lang.invoke.MethodHandles;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.gui.ResultSetTableModel.TableStringRenderer;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;

public class AseConfiguration extends AseAbstract
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	ResultSetTableModel _shortRstm;
	String              _cacheConfig;

	public AseConfiguration(DailySummaryReportAbstract reportingInstance)
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

			sb.append("Row Count: " + _shortRstm.getRowCount() + "<br>\n");

			// Create a default renderer
			TableStringRenderer tableRender = new ReportEntryTableStringRenderer()
			{
				@Override
				public String cellValue(ResultSetTableModel rstm, int row, int col, String colName, Object objVal, String strVal)
				{
					if ("Pending".equals(colName) && "true".equalsIgnoreCase(strVal))
					{
						return "<span style='background-color:red;'>" + strVal + "</span>";
					}
					return strVal;
				}
			};
			sb.append(_shortRstm.toHtmlTableString("sortable", true, true, null, tableRender));
		}
		
		if (StringUtil.hasValue(_cacheConfig))
		{
			String htmlContent = ""
					+ "<h4>ASE Cache Configuration</h4>"
					+ "<pre style='font-size: 1rem;'> \n"
					+ "<code> \n"
					+ _cacheConfig + "\n"
					+ "</code> \n"
					+ "</pre> \n"
					;

			String divId = "ase_cache_config";
			boolean visibleAtStart = false;
			String showHideDiv = createShowHideDiv(divId, visibleAtStart, "Show/Hide ASE Cache Configuration", htmlContent);
			
			sb.append("<br>");
			sb.append( msOutlookAlternateText(showHideDiv, "Show/Hide ASE Cache Configuration", "") );
		}
	}

	@Override
	public String getSubject()
	{
		return "ASE Configuration (Only non-defaults)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}


	@Override
	public String[] getMandatoryTables()
	{
		return new String[] { "MonSessionDbmsConfig" };
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		// Get 'statement cache size' from saved configuration 
		String sql = "select * \n"
				   + "from [MonSessionDbmsConfig] \n"
				   + "where [SessionStartTime] = (select max([SessionStartTime]) from [MonSessionDbmsConfig]) \n"
				   + "  and [NonDefault] = " + conn.toBooleanValueString(true) + " \n"
				   + "  and [SectionName] != 'Monitoring' \n"
				   + "order by [ConfigName]";
		_shortRstm = executeQuery(conn, sql, false, "AseConfig");

		if (_shortRstm == null)
		{
			_shortRstm = ResultSetTableModel.createEmpty("AseConfig");
			return;
		}

		// Highlight sort column
		_shortRstm.setHighlightSortColumns("ConfigName");
		
		
		//-------------------------------------------------------
		// ASE Cache Config
		//-------------------------------------------------------
		sql = ""
			+ "select [configText] \n"
			+ "from [MonSessionDbmsConfigText] \n"
			+ "where [configName] = 'AseCacheConfig' \n"
			+ "  and [SessionStartTime] = (select max([SessionStartTime]) from [MonSessionDbmsConfigText]) \n"
			+ "";

		// transform all "[" and "]" to DBMS Vendor Quoted Identifier Chars 
		sql = conn.quotifySqlString(sql);

		try ( Statement stmnt = conn.createStatement() )
		{
			// Query Timeout
			stmnt.setQueryTimeout( getDsrQueryTimeoutInSec() );
			
			try ( ResultSet rs = stmnt.executeQuery(sql) )
			{
				while(rs.next())
				{
					_cacheConfig = rs.getString(1);
				}
			}
		}
		catch(SQLException ex)
		{
			_logger.warn("Problems getting 'ASE Cache Config': " + ex + ". SQL=|" + sql + "|.");
		}		
	}
}




//1> select "configText"
//2> from "MonSessionDbmsConfigText" 
//3> where "configName" = 'AseCacheConfig'
//4>   and "SessionStartTime" = (select max("SessionStartTime") from "MonSessionDbmsConfigText")
// configText                                                                      
// --------------------------------------------------------------------------------
// +--------------------------+
//|ConfigSnapshotAtDateTime  |
//+--------------------------+
//|Dec  9 2025 12:54:22:070AM|
//+--------------------------+
//Rows 1
// 
//######################################################################################
//## ASE Memory available for reconfiguration
//######################################################################################
//2247.8 MB available for reconfiguration.
// 
//######################################################################################
//## sp_helpcache
//######################################################################################
//+------------------+-----------+----------+----------+----------------+
//|Cache Name        |Config Size|Run Size  |Overhead  |Cache Type      |
//+------------------+-----------+----------+----------+----------------+
//|default data cache|95000.0 Mb |95000.0 Mb|3874.29 Mb|Default         |
//|log_cache         | 512.00 Mb | 512.00 Mb|  20.54 Mb|Log Only        |
//|tempdb_cache      |1024.00 Mb |1024.00 Mb|  41.12 Mb|Mixed, HK Ignore|
//+------------------+-----------+----------+----------+----------------+
//Rows 3
// 
// 
//Memory Available For      Memory Configured
//Named Caches              To Named Caches
//--------------------       ----------------
//96536.0 Mb                  96536.0 Mb
// 
// 
//------------------ Cache Binding Information: ------------------ 
// 
//Cache Name           Entity Name                Type               Index Name                    Status
//----------           -----------                ----               ----------                    ------
//log_cache            PML.dbo.syslogs            table                                              V
//log_cache            Linda.dbo.syslogs          table                                              V
//tempdb_cache         tempdb                     database                                           V
// 
//######################################################################################
//## sp_cacheconfig
//######################################################################################
//+------------------+------+----------------+------------+------------+
//|Cache Name        |Status|Type            |Config Value|Run Value   |
//+------------------+------+----------------+------------+------------+
//|default data cache|Active|Default         | 95000.00 Mb| 95000.00 Mb|
//|log_cache         |Active|Log Only        |   512.00 Mb|   512.00 Mb|
//|tempdb_cache      |Active|Mixed, HK Ignore|  1024.00 Mb|  1024.00 Mb|
//+------------------+------+----------------+------------+------------+
//Rows 3
//                                            ------------ ------------
//                            Total    96536.0 Mb   96536.0 Mb   
//==========================================================================
//Cache: default data cache,   Status: Active,   Type: Default
//      Config Size: 95000.00 Mb,   Run Size: 95000.00 Mb
//      Config Replacement: strict LRU,   Run Replacement: strict LRU
//      Config Partition:            4,   Run Partition:            4
//+--------+-------------+------------+------------+-----------+
//|IO Size |Wash Size    |Config Size |Run Size    |APF Percent|
//+--------+-------------+------------+------------+-----------+
//|    8 Kb|    245760 Kb| 75446.00 Mb| 75446.00 Mb|    10     |
//|   64 Kb|  10011648 Kb| 19554.00 Mb| 19554.00 Mb|    30     |
//+--------+-------------+------------+------------+-----------+
//Rows 2
//==========================================================================
//Cache: log_cache,   Status: Active,   Type: Log Only
//      Config Size: 512.00 Mb,   Run Size: 512.00 Mb
//      Config Replacement: relaxed LRU,   Run Replacement: relaxed LRU
//      Config Partition:            1,   Run Partition:            1
//+--------+-------------+------------+------------+-----------+
//|IO Size |Wash Size    |Config Size |Run Size    |APF Percent|
//+--------+-------------+------------+------------+-----------+
//|    8 Kb|     10648 Kb|    52.00 Mb|    52.00 Mb|    10     |
//|   16 Kb|     61440 Kb|   460.00 Mb|   460.00 Mb|    10     |
//+--------+-------------+------------+------------+-----------+
//Rows 2
//==========================================================================
//Cache: tempdb_cache,   Status: Active,   Type: Mixed, HK Ignore
//      Config Size: 1024.00 Mb,   Run Size: 1024.00 Mb
//      Config Replacement: strict LRU,   Run Replacement: strict LRU
//      Config Partition:            4,   Run Partition:            4
//+--------+-------------+------------+------------+-----------+
//|IO Size |Wash Size    |Config Size |Run Size    |APF Percent|
//+--------+-------------+------------+------------+-----------+
//|    8 Kb|    127776 Kb|   624.00 Mb|   624.00 Mb|    10     |
//|   64 Kb|     81920 Kb|   400.00 Mb|   400.00 Mb|    10     |
//+--------+-------------+------------+------------+-----------+
//Rows 2
// 
//######################################################################################
//## select * from master..syscacheinfo
//######################################################################################
//+------------------+------------+----------------+-----------+--------+------------------+---------------+-----------------+--------------+--------+-------+----------+------+
//|cache_name        |cache_status|cache_type      |config_size|run_size|config_replacement|run_replacement|config_partitions|run_partitions|overhead|cacheid|instanceid|scope |
//+------------------+------------+----------------+-----------+--------+------------------+---------------+-----------------+--------------+--------+-------+----------+------+
//|default data cache|Active      |Default         |     95 000|  95 000|Strict LRU        |Strict LRU     |                4|             4|3 874,29|      0|         0|Global|
//|log_cache         |Active      |Log Only        |        512|     512|Relaxed LRU       |Relaxed LRU    |                1|             1|   20,54|      1|         0|Global|
//|tempdb_cache      |Active      |Mixed, HK Ignore|      1 024|   1 024|Strict LRU        |Strict LRU     |                4|             4|   41,12|      2|         0|Global|
//+------------------+------------+----------------+-----------+--------+------------------+---------------+-----------------+--------------+--------+-------+----------+------+
//Rows 3
// 
//######################################################################################
//## select * from master..syspoolinfo
//######################################################################################
//+------------------+-------+-----------+--------+-----------+---------+-------+----------+------+
//|cache_name        |io_size|config_size|run_size|apf_percent|wash_size|cacheid|instanceid|scope |
//+------------------+-------+-----------+--------+-----------+---------+-------+----------+------+
//|default data cache|8      |     75 446|  75 446|         10|245760   |      0|         0|Global|
//|log_cache         |8      |         52|      52|         10|10648    |      1|         0|Global|
//|tempdb_cache      |8      |        624|     624|         10|127776   |      2|         0|Global|
//|log_cache         |16     |        460|     460|         10|61440    |      1|         0|Global|
//|tempdb_cache      |64     |        400|     400|         10|81920    |      2|         0|Global|
//|default data cache|64     |     19 554|  19 554|         30|10011648 |      0|         0|Global|
//+------------------+-------+-----------+--------+-----------+---------+-------+----------+------+
//Rows 6
// 
//######################################################################################
//## select * from master..syscachepoolinfo
//######################################################################################
//+------------------+------------+----------------+-----------------+--------------+------------------------+---------------------+-----------------------+--------------------+--------------+------------+----------------+-------------+----------------+--------------+-------+----------+------+
//|cache_name        |cache_status|cache_type      |cache_config_size|cache_run_size|cache_config_replacement|cache_run_replacement|cache_config_partitions|cache_run_partitions|cache_overhead|pool_io_size|pool_config_size|pool_run_size|pool_apf_percent|pool_wash_size|cacheid|instanceid|scope |
//+------------------+------------+----------------+-----------------+--------------+------------------------+---------------------+-----------------------+--------------------+--------------+------------+----------------+-------------+----------------+--------------+-------+----------+------+
//|default data cache|Active      |Default         |           95 000|        95 000|Strict LRU              |Strict LRU           |                      4|                   4|      3 874,29|8           |          75 446|       75 446|              10|245760        |      0|         0|Global|
//|default data cache|Active      |Default         |           95 000|        95 000|Strict LRU              |Strict LRU           |                      4|                   4|      3 874,29|64          |          19 554|       19 554|              30|10011648      |      0|         0|Global|
//|log_cache         |Active      |Log Only        |              512|           512|Relaxed LRU             |Relaxed LRU          |                      1|                   1|         20,54|8           |              52|           52|              10|10648         |      1|         0|Global|
//|log_cache         |Active      |Log Only        |              512|           512|Relaxed LRU             |Relaxed LRU          |                      1|                   1|         20,54|16          |             460|          460|              10|61440         |      1|         0|Global|
//|tempdb_cache      |Active      |Mixed, HK Ignore|            1 024|         1 024|Strict LRU              |Strict LRU           |                      4|                   4|         41,12|64          |             400|          400|              10|81920         |      2|         0|Global|
//|tempdb_cache      |Active      |Mixed, HK Ignore|            1 024|         1 024|Strict LRU              |Strict LRU           |                      4|                   4|         41,12|8           |             624|          624|              10|127776        |      2|         0|Global|
//+------------------+------------+----------------+-----------------+--------------+------------------------+---------------------+-----------------------+--------------------+--------------+------------+----------------+-------------+----------------+--------------+-------+----------+------+
//Rows 6
