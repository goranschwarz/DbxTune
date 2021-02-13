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

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.log4j.Logger;

import com.asetune.gui.ModelMissmatchException;
import com.asetune.gui.ResultSetTableModel;
import com.asetune.pcs.PersistentCounterHandler;
import com.asetune.pcs.report.DailySummaryReportAbstract;
import com.asetune.pcs.report.content.ReportEntryAbstract;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;
import com.asetune.utils.DbUtils;
import com.asetune.utils.StringUtil;

public abstract class AseAbstract
extends ReportEntryAbstract
{
	private static Logger _logger = Logger.getLogger(AseAbstract.class);

	protected int _statement_gt_execTime      = -1;
	protected int _statement_gt_logicalReads  = -1;
	protected int _statement_gt_physicalReads = -1;


	public AseAbstract(DailySummaryReportAbstract reportingInstance)
	{
		super(reportingInstance);
	}

	protected void getSlowQueryThresholds(DbxConnection conn)
	{
		String tabName = "MonSessionParams";
		String sql = ""
			    + "select [Type], [ParamName], [ParamValue] \n"
			    + "from ["+tabName+"] \n"
			    + "where [ParamName] in("
			    		+ "'"    + PersistentCounterHandler.PROPKEY_sqlCap_saveStatement_gt_execTime
			    		+ "', '" + PersistentCounterHandler.PROPKEY_sqlCap_saveStatement_gt_logicalReads
			    		+ "', '" + PersistentCounterHandler.PROPKEY_sqlCap_saveStatement_gt_physicalReads
			    		+ "') \n"
			    + "";

		sql = conn.quotifySqlString(sql);
		try ( Statement stmnt = conn.createStatement() )
		{
			// Unlimited execution time
			stmnt.setQueryTimeout(0);
			try ( ResultSet rs = stmnt.executeQuery(sql) )
			{
				while(rs.next())
				{
//					String type = rs.getString(1);
					String name = rs.getString(2);
					String val  = rs.getString(3);
					
					if (PersistentCounterHandler.PROPKEY_sqlCap_saveStatement_gt_execTime     .equals(name)) _statement_gt_execTime      = StringUtil.parseInt(val, -1);
					if (PersistentCounterHandler.PROPKEY_sqlCap_saveStatement_gt_logicalReads .equals(name)) _statement_gt_logicalReads  = StringUtil.parseInt(val, -1);
					if (PersistentCounterHandler.PROPKEY_sqlCap_saveStatement_gt_physicalReads.equals(name)) _statement_gt_physicalReads = StringUtil.parseInt(val, -1);
				}
			}
		}
		catch(SQLException ex)
		{
			_logger.warn("Problems getting values from '"+tabName+"': " + ex);
		}

		Configuration conf = Configuration.getCombinedConfiguration();

		if (_statement_gt_execTime      == -1) _statement_gt_execTime      = conf.getIntProperty(PersistentCounterHandler.PROPKEY_sqlCap_saveStatement_gt_execTime,      PersistentCounterHandler.DEFAULT_sqlCap_saveStatement_gt_execTime);
		if (_statement_gt_logicalReads  == -1) _statement_gt_logicalReads  = conf.getIntProperty(PersistentCounterHandler.PROPKEY_sqlCap_saveStatement_gt_logicalReads,  PersistentCounterHandler.DEFAULT_sqlCap_saveStatement_gt_logicalReads);
		if (_statement_gt_physicalReads == -1) _statement_gt_physicalReads = conf.getIntProperty(PersistentCounterHandler.PROPKEY_sqlCap_saveStatement_gt_physicalReads, PersistentCounterHandler.DEFAULT_sqlCap_saveStatement_gt_physicalReads);
	}
	

	public Set<String> getStatementCacheObjects(ResultSetTableModel rstm, String colName)
	{
		Set<String> set = new LinkedHashSet<>();
		
		int pos_colName = rstm.findColumn(colName);
		if (pos_colName != -1)
		{
			for (int r=0; r<rstm.getRowCount(); r++)
			{
				String name = rstm.getValueAsString(r, pos_colName);
				
				if (name != null && (name.trim().startsWith("*ss") || name.trim().startsWith("*sq")) )
				{
					set.add(name);
				}
			}
		}
		
		return set;
	}

	public String getXmlShowplanFromMonDdlStorage(DbxConnection conn, String sSqlIdStr)
	throws SQLException
	{
		String xml = "";
		
		if (StringUtil.isNullOrBlank(sSqlIdStr))
			return "";
		
		String sql = ""
			    + "select [objectName], [extraInfoText] as [SQLText] \n"
			    + "from [MonDdlStorage] \n"
			    + "where 1 = 1 \n"
			    + "  and [dbname]     = 'statement_cache' \n"
			    + "  and [owner]      = 'ssql' \n"
			    + "  and [objectName] = " + DbUtils.safeStr(sSqlIdStr) + " \n"
			    + "";
		
		sql = conn.quotifySqlString(sql);
		try ( Statement stmnt = conn.createStatement() )
		{
			// Unlimited execution time
			stmnt.setQueryTimeout(0);
			try ( ResultSet rs = stmnt.executeQuery(sql) )
			{
				while(rs.next())
				{
					xml += rs.getString(2);
				}

			}
		}
		catch(SQLException ex)
		{
			//_problem = ex;

			_logger.warn("Problems getting XML Showplan for name = '"+sSqlIdStr+"': " + ex);
			throw ex;
		}

		// Remove "stuff" this is not part of the XML
		if (StringUtil.hasValue(xml))
		{
			int xmlStart = xml.indexOf("<?xml version");
			if (xmlStart > 0) // If it doesn't start with "<?xml version" then strip that part off
			{
				xml = xml.substring(xmlStart);
			}
		}
		
		return xml;
	}

	public Map<String, String> getXmlShowplanMapFromMonDdlStorage(DbxConnection conn, Set<String> nameSet)
	throws SQLException
	{
		Map<String, String> map = new LinkedHashMap<>();

		for (String sSqlIdStr : nameSet)
		{
			String xml = getXmlShowplanFromMonDdlStorage(conn, sSqlIdStr);
			map.put(sSqlIdStr, xml);
		}

		return map;
	}


	public ResultSetTableModel getSqlStatementsFromMonDdlStorage(DbxConnection conn, Set<String> nameSet)
	throws SQLException
	{
		ResultSetTableModel ssqlRstm = null;
		
		for (String name : nameSet)
		{
			String sql = ""
				    + "select [objectName], [extraInfoText] as [SQLText] \n"
				    + "from [MonDdlStorage] \n"
				    + "where 1 = 1 \n"
				    + "  and [dbname]     = 'statement_cache' \n"
				    + "  and [owner]      = 'ssql' \n"
				    + "  and [objectName] = " + DbUtils.safeStr(name) + " \n"
				    + "";
			
			sql = conn.quotifySqlString(sql);
			try ( Statement stmnt = conn.createStatement() )
			{
				// Unlimited execution time
				stmnt.setQueryTimeout(0);
				try ( ResultSet rs = stmnt.executeQuery(sql) )
				{
//					ResultSetTableModel rstm = new ResultSetTableModel(rs, "ssqlRstm");
					ResultSetTableModel rstm = createResultSetTableModel(rs, "ssqlRstm", sql);
					
					if (ssqlRstm == null)
						ssqlRstm = rstm;
					else
						ssqlRstm.add(rstm);

					if (_logger.isDebugEnabled())
						_logger.debug("_ssqlRstm.getRowCount()="+ rstm.getRowCount());
				}
			}
			catch(SQLException ex)
			{
				//_problem = ex;

				_logger.warn("Problems getting SQL Statement name = '"+name+"': " + ex);
				throw ex;
			} 
			catch(ModelMissmatchException ex)
			{
				//_problem = ex;

				_logger.warn("Problems (merging into previous ResultSetTableModel) when getting SQL by name = '"+name+"': " + ex);
				
				throw new SQLException("Problems (merging into previous ResultSetTableModel) when getting SQL by name = '"+name+"': " + ex, ex);
			} 
		}
		
		// Replace the XML Showplan with JUST the SQL
		if (ssqlRstm != null)
		{
			int pos_SQLText = ssqlRstm.findColumn("SQLText");
			if (pos_SQLText >= 0)
			{
				for (int r=0; r<ssqlRstm.getRowCount(); r++)
				{
					String SQLText = ssqlRstm.getValueAsString(r, pos_SQLText);
					if (StringUtil.hasValue(SQLText))
					{
						int startPos = SQLText.indexOf("<![CDATA[");
						int endPos   = SQLText.indexOf("]]>");

						if (startPos >= 0 && endPos >= 0)
						{
							startPos += "<![CDATA[".length();
						//	endPos   -= "]]>"      .length();  // note: we do NOT need to take away 3 chars here... if we do we will truncate last 3 chars
							
							String newSQLText = SQLText.substring(startPos, endPos).trim();
							
							if (newSQLText.startsWith("SQL Text:"))
								newSQLText = newSQLText.substring("SQL Text:".length()).trim();

							// make it a bit more HTML like
							newSQLText = "<xmp>" + newSQLText + "</xmp>";

							// Finally SET the SQL Text
							ssqlRstm.setValueAtWithOverride(newSQLText, r, pos_SQLText);
						}
					}
				}
			}
		}
		
		return ssqlRstm;
	}
	


	//--------------------------------------------------------------------------------
	// BEGIN: SQL Capture - get SQL TEXT
	//--------------------------------------------------------------------------------
	public static class SqlCapExecutedSqlEntries
	{
		/** How many statements we had before it was shortened to a smaller size */
		int _actualSize;

		/** what we used as "max" size to keep in the list */
		int _topSize;

		List<SqlCapExecutedSqlEntry> _entryList = new ArrayList<>();
	}
	public static class SqlCapExecutedSqlEntry
	{
		String sqlTextHashId;
		int    execCount;
		int    elapsed_ms;
		int    cpuTime;
		int    waitTime;
		int    logicalReads;
		int    physicalReads;
		int    rowsAffected;
		int    memUsageKB;
		String sqlText;
	}

//	public int getSqlCapExecutedSqlText_topCount()
//	{
//		return 10;
//	}

	public int getSqlCapExecutedSqlText_topCount()
	{
		return 10;
	}
	public String getSqlCapExecutedSqlTextAsString(Map<Map<String, Object>, SqlCapExecutedSqlEntries> map, Map<String, Object> whereColValMap)
	{
		if (map == null)
			return "";

		StringBuilder sb = new StringBuilder();

		SqlCapExecutedSqlEntries entries = map.get(whereColValMap);
		
		if (entries == null)
		{
			return "No entry for '"+ whereColValMap +"' was found. known keys: " + map.keySet();
		}
		
		sb.append("-- There are ").append( entries._actualSize ).append(" Distinct SQL Text(s) found in the SQL Capture Recording, by column(s): " + whereColValMap + " \n");
		if (entries._actualSize > entries._topSize)
			sb.append("-- I will only show the first ").append(entries._topSize).append(" records below, ordered by 'CpuTime'.\n");
		if (entries._actualSize == 0)
		{
			sb.append("-- No records was found in the SQL Capture subsystem... This may be to the following reasons:\n");
			sb.append("--   * AseTune SQL Capture, not enabled! \n");
			sb.append("--   * The execution time was less than AseTune SQL Capture was configured to capture. \n");
			sb.append("--   * ASE Not configured to capture 'statement pipe active' or 'sql text pipe active' \n");
			sb.append("--   * ASE configuration 'statement pipe max messages' or 'sql text pipe max messages' was not high enough. \n");
			sb.append("--   * Or something else. \n");
		}
		sb.append("\n");

		NumberFormat nf = NumberFormat.getInstance();
		String sep = "";
		int    row = 0;
		for (SqlCapExecutedSqlEntry entry : entries._entryList)
		{
			row++;

			BigDecimal avgElapsed_ms    = new BigDecimal((entry.elapsed_ms   *1.0)/(entry.execCount*1.0)).setScale(1, BigDecimal.ROUND_HALF_EVEN);
			BigDecimal avgCpuTime       = new BigDecimal((entry.cpuTime      *1.0)/(entry.execCount*1.0)).setScale(1, BigDecimal.ROUND_HALF_EVEN);
			BigDecimal avgWaitTime      = new BigDecimal((entry.waitTime     *1.0)/(entry.execCount*1.0)).setScale(1, BigDecimal.ROUND_HALF_EVEN);
			BigDecimal avgLogicalReads  = new BigDecimal((entry.logicalReads *1.0)/(entry.execCount*1.0)).setScale(1, BigDecimal.ROUND_HALF_EVEN);
			BigDecimal avgPhysicalReads = new BigDecimal((entry.physicalReads*1.0)/(entry.execCount*1.0)).setScale(1, BigDecimal.ROUND_HALF_EVEN);
			BigDecimal avgRowsAffected  = new BigDecimal((entry.rowsAffected *1.0)/(entry.execCount*1.0)).setScale(1, BigDecimal.ROUND_HALF_EVEN);
			BigDecimal avgMemUsageKB    = new BigDecimal((entry.memUsageKB   *1.0)/(entry.execCount*1.0)).setScale(1, BigDecimal.ROUND_HALF_EVEN);

			BigDecimal waitTimePct      = new BigDecimal(((entry.waitTime*1.0)/(entry.elapsed_ms*1.0)) * 100.0).setScale(1, BigDecimal.ROUND_HALF_EVEN);
			
			sb.append(sep);
			sb.append("-------- SQL Text [" + row + "] --------------------------------------------------\n");
			sb.append("-- Count : " + nf.format(entry.execCount) + "\n");
			sb.append("-- TimeMs: [Elapsed="      + nf.format(entry.elapsed_ms)   + ", CpuTime="       + nf.format(entry.cpuTime)       + ", WaitTime=" + nf.format(entry.waitTime) + ", WaitTimePct=" + waitTimePct + "%]. Avg=[Elapsed=" + nf.format(avgElapsed_ms) + ", CpuTime=" + nf.format(avgCpuTime) + ", WaitTime=" + nf.format(avgWaitTime) + "]. \n");
			sb.append("-- Reads:  [LogicalReads=" + nf.format(entry.logicalReads) + ", PhysicalReads=" + nf.format(entry.physicalReads) + "]. Avg=[LogicalReads=" + nf.format(avgLogicalReads) + ", PhysicalReads=" + nf.format(avgPhysicalReads) + "]. \n");
			sb.append("-- Other:  [RowsAffected=" + nf.format(entry.rowsAffected) + ", MemUsageKB="    + nf.format(entry.memUsageKB)    + "]. Avg=[RowsAffected=" + nf.format(avgRowsAffected) + ", MemUsageKB="    + nf.format(avgMemUsageKB)    + "]. \n");
			sb.append("------------------------------------------------------------------------\n");
			sb.append( StringEscapeUtils.escapeHtml4(entry.sqlText) ).append("\n");
			//org.apache.commons.text.StringEscapeUtils

			sep = "\n\n";
		}
		
		return sb.toString();
	}
	public Map<Map<String, Object>, SqlCapExecutedSqlEntries> getSqlCapExecutedSqlText(Map<Map<String, Object>, SqlCapExecutedSqlEntries> map, DbxConnection conn, boolean hasDictCompCols, Map<String, Object> whereColValMap)
	{
		int topRows = getSqlCapExecutedSqlText_topCount();

//		String sqlTopRows     = topRows <= 0 ? "" : " top " + topRows + " ";
//		String tabName        = "MonSqlCapStatements";
//		String sqlTextColName = "SQLText";
//		String sqlTextColName = "SQLText" + DictCompression.DCC_MARKER;  shouldnt we check if DCC is enabled or not?
//
//		if (DictCompression.hasCompressedColumnNames(conn, null, tabName))
//		if (hasDictCompCols)
//			sqlTextColName += DictCompression.DCC_MARKER;
//
//		String col_NormSQLText = DictCompression.getRewriteForColumnName(tabName, sqlTextColName);

		if (map == null)
			map = new HashMap<>();
				
//		String sql = ""
//			    + "select " + sqlTopRows + " distinct " + col_NormSQLText + " \n"
//			    + "from [" + tabName + "] \n"
//			    + "where [" + colName + "] = " + DbUtils.safeStr(colVal) + " \n"
//			    + "";

		String whereColValStr = "";
		for (Entry<String, Object> entry : whereColValMap.entrySet())
		{
			whereColValStr += "  and [" + entry.getKey() + "] = " + DbUtils.safeStr( entry.getValue() ) + "\n";
		}

		String sql = ""
			    + "select \n"
			    + "     [SQLText$dcc$]         as [SQLTextHashId] \n"
			    + "    ,count(*)               as [cnt] \n"
			    + "    ,sum(s.[Elapsed_ms])    as [Elapsed_ms] \n"
			    + "    ,sum(s.[CpuTime])       as [CpuTime] \n"
			    + "    ,sum(s.[WaitTime])      as [WaitTime] \n"
			    + "    ,sum(s.[LogicalReads])  as [LogicalReads] \n"
			    + "    ,sum(s.[PhysicalReads]) as [PhysicalReads] \n"
			    + "    ,sum(s.[RowsAffected])  as [RowsAffected] \n"
			    + "    ,sum(s.[MemUsageKB])    as [MemUsageKB] \n"
			    + "    ,max(l.[colVal])        as [SQLText] \n"
			    + "from [MonSqlCapStatements] s \n"
			    + "join [MonSqlCapStatements$dcc$SQLText] l on l.[hashId] = s.[SQLText$dcc$] \n"
			    + "where 1=1 \n"
			    + whereColValStr
			    + "group by [SQLText$dcc$] \n"
			    + "order by [CpuTime] desc \n"	// FIXME: add this as a parameter, BUT: it also needs to be "passed" to getSqlCapExecutedSqlTextAsString
			    + "";
		
		sql = conn.quotifySqlString(sql);
//System.out.println("getExecutedSqlText(map="+map+", conn="+conn+", topRows="+topRows+", colName="+colName+", colVal="+colVal+"): SQL="+sql);
		try ( Statement stmnt = conn.createStatement() )
		{
			// Unlimited execution time
			stmnt.setQueryTimeout(0);
			try ( ResultSet rs = stmnt.executeQuery(sql) )
			{
				SqlCapExecutedSqlEntries entries = new SqlCapExecutedSqlEntries();

				int rowCount = 0;
				while(rs.next())
				{
					if (rowCount < topRows)
					{
						SqlCapExecutedSqlEntry entry = new SqlCapExecutedSqlEntry();

						int colPos = 1;
						entry.sqlTextHashId = rs.getString(colPos++);
						entry.execCount     = rs.getInt   (colPos++);
						entry.elapsed_ms    = rs.getInt   (colPos++);
						entry.cpuTime       = rs.getInt   (colPos++);
						entry.waitTime      = rs.getInt   (colPos++);
						entry.logicalReads  = rs.getInt   (colPos++);
						entry.physicalReads = rs.getInt   (colPos++);
						entry.rowsAffected  = rs.getInt   (colPos++);
						entry.memUsageKB    = rs.getInt   (colPos++);
						entry.sqlText       = rs.getString(colPos++);

						entries._entryList.add(entry);
					}
					rowCount++;
				}

				entries._actualSize = rowCount;
				entries._topSize    = topRows;

				map.put(whereColValMap, entries);
			}
		}
		catch(SQLException ex)
		{
			_logger.warn("Problems getting SQL Text by: " + whereColValMap + ": " + ex);
		}
		
		return map;
	}
	//--------------------------------------------------------------------------------
	// END: SQL Capture - get SQL TEXT
	//--------------------------------------------------------------------------------

	
	
	
	
	
	
	
//	//--------------------------------------------------------------------------------
//	// BEGIN: SQL Capture - get WAIT
//	//--------------------------------------------------------------------------------
//	public static class SqlCapWaitEntry
//	{
//		ResultSetTableModel waitTime;
//		ResultSetTableModel rootCauseInfo;
//	}
//
//	public String getSqlCapWaitTimeAsString(Map<Map<String, Object>, SqlCapWaitEntry> map, Map<String, Object> whereColValMap)
//	{
//		if (map == null)
//			return "";
//
//		SqlCapWaitEntry entry = map.get(whereColValMap);
//		
//		if (entry == null)
//		{
//			return "No entry for '"+ whereColValMap +"' was found. known keys: " + map.keySet();
//		}
//
//		if (entry.waitTime == null && entry.rootCauseInfo == null)
//		{
//			return "Entry for '"+ whereColValMap +"' was found. But NO INFORMATION has been assigned to it.";
//		}
//		
//		StringBuilder sb = new StringBuilder();
//
//		// Add some initial text
//		if (entry.waitTime != null)
//		{
//			sb.append("-------------------------------------------------------------------------------------------------------------------------------\n");
//			sb.append("-- Wait Information... or WaitEvents that this statement has been waited on. \n");
//			sb.append("-- However no 'individual' WaitEventID statistics is recorded in [monSysStatements] which is the source for [MonSqlCapStatements] \n");
//			sb.append("-- So the below information is fetched from [CmSpidWait_diff] for the approximate time, which holds Wait Statistics on a SPID level... \n");
//			sb.append("-- therefore WaitTime which is not relevant for *this* specific statement will/may be included. \n");
//			sb.append("-- So see the below values as a guidance! \n");
//			sb.append("-------------------------------------------------------------------------------------------------------------------------------\n");
//			sb.append("\n");
//
//			// Add WAIT Text
//			sb.append( StringEscapeUtils.escapeHtml4(entry.waitTime.toAsciiTableString()) ).append("\n");
//			sb.append("\n");
//			sb.append("\n");
//		}
//		
//		// Add some text about: Root Cause SQL Text
//		if (entry.rootCauseInfo != null)
//		{
//			sb.append("-------------------------------------------------------------------------------------------------------------------------------\n");
//			sb.append("-- Root Cause for Blocking Locks:\n");
//			sb.append("-- Below we try to investigate any 'root cause' for blocking locks that was found in above section.\n");
//			sb.append("--\n");
//			sb.append("-- Algorithm used to reveile 'root cause' for 'blocking lock(s)' : \n");
//			sb.append("--  - Grab records that has WaitTime from [MonSqlCapStatements] where [NormJavaSqlHashCode] = ? \n");
//			sb.append("--  - using the above: grab 'root cause spids' from [CmActiveStatements] that are 'waiting for a lock' \n");
//			sb.append("--  - then: grab SQL Statements for the 'root cause' records from [CmActiveStatements_abs] \n");
//			sb.append("-- NOTE: This is NOT a 100% truth of the story, due to: \n");
//			sb.append("--         - [CmActiveStatements] is only stored/snapshoted every 'sample time' (60 seconds or similar) \n");
//			sb.append("--           so we can/WILL MISS everything in between (statements that has already finnished and continued processing next statement) \n");
//			sb.append("--           or see info that is not relevant (information from 'next' statement) \n");
//			sb.append("-- Use this values as a guidance\n");
//			sb.append("-------------------------------------------------------------------------------------------------------------------------------\n");
//			sb.append("\n");
//
//			// Remove any HTML tags...
//			//entry.rootCauseInfo.stripHtmlTags("MonSqlText");
//			int pos_MonSqlText = entry.rootCauseInfo.findColumn("MonSqlText");
//			if (pos_MonSqlText != -1)
//			{
//				for (int r=0; r<entry.rootCauseInfo.getRowCount(); r++)
//				{
//					String monSqlText = entry.rootCauseInfo.getValueAsString(r, pos_MonSqlText);
//					if (StringUtil.hasValue(monSqlText))
//					{
//						monSqlText = monSqlText.replace("<html><pre>", "").replace("</pre></html>", "");
//						entry.rootCauseInfo.setValueAtWithOverride(monSqlText, r, pos_MonSqlText);
//					}
//				}
//			}
//
//			// Add RootCause Text
//			sb.append( StringEscapeUtils.escapeHtml4(entry.rootCauseInfo.toAsciiTableString()) ).append("\n");
//			sb.append("\n");
//			sb.append("\n");
//		}
//		else
//		{
//			sb.append("-------------------------------------------------------------------------------------------------------------------------------\n");
//			sb.append("-- Root Cause for Blocking Locks:\n");
//			sb.append("-- No blocking lock information was found... NOTE: This might not be 100% accurate.\n");
//			sb.append("-------------------------------------------------------------------------------------------------------------------------------\n");
//			
//		}
//		
//		return sb.toString();
//	}
//
//	public Map<Map<String, Object>, SqlCapWaitEntry> getSqlCapWaitTime(Map<Map<String, Object>, SqlCapWaitEntry> map, DbxConnection conn, boolean hasDictCompCols, Map<String, Object> whereColValMap)
//	{
////		int topRows = getSqlCapExecutedSqlText_topCount();
////
////		String sqlTopRows     = topRows <= 0 ? "" : " top " + topRows + " ";
////		String tabName        = "MonSqlCapStatements";
////		String sqlTextColName = "SQLText";
//////		String sqlTextColName = "SQLText" + DictCompression.DCC_MARKER;  shouldnt we check if DCC is enabled or not?
////
//////		if (DictCompression.hasCompressedColumnNames(conn, null, tabName))
////		if (hasDictCompCols)
////			sqlTextColName += DictCompression.DCC_MARKER;
////				
////		String col_NormSQLText = DictCompression.getRewriteForColumnName(tabName, sqlTextColName);
//
//		int recSampleTimeInSec = getReportingInstance().getRecordingSampleTime();
//
//		if (map == null)
//			map = new HashMap<>();
//
//		SqlCapWaitEntry waitEntry = map.get(whereColValMap);
//		if (waitEntry == null)
//		{
//			waitEntry = new SqlCapWaitEntry();
//			map.put(whereColValMap, waitEntry);
//		}
//
//
//		String whereColValStr = "";
//		for (Entry<String, Object> entry : whereColValMap.entrySet())
//		{
//			whereColValStr += "      and [" + entry.getKey() + "] = " + DbUtils.safeStr( entry.getValue() ) + "\n";
//		}
//
//		boolean getWaitInfo      = true;
//		boolean getRootCauseInfo = true;
//
//		if (getWaitInfo)
//		{
//			String sql = ""
//				    + "with cap as \n"
//				    + "( \n"
//				    + "    select [SPID] \n"
//				    + "          ,[KPID] \n"
//				    + "          ,[BatchID] \n"
//				    + "          ,[StartTime] \n"
//				    + "          ,[EndTime] \n"
////				    + "          ,(select [colVal] from [MonSqlCapStatements$dcc$SQLText] i where i.[hashId] = o.[SQLText$dcc$]) as [SQLText] \n"
//				    + "    from [MonSqlCapStatements] o \n"
//				    + "    where 1=1 \n"
//				    +      whereColValStr
//				    + "      and [WaitTime] > 0 \n"
//				    + ") \n"
//				    + "select \n"
//				    + "     min([SessionSampleTime]) as [SessionSampleTime_min] \n"
//				    + "    ,max([SessionSampleTime]) as [SessionSampleTime_max] \n"
//				    + "    ,cast('' as varchar(8))   as [Duration] \n"
//				    + "    ,count(*)                 as [cnt] \n"
//				    + "    ,max([WaitClassDesc])     as [WaitClassDesc] \n"
//				    + "    ,max([WaitEventDesc])     as [WaitEventDesc] \n"
//				    + "    ,[WaitEventID] \n"
//				    + "    ,sum([WaitTime])          as [WaitTime_sum] \n"
//				    + "    ,cast('' as varchar(8))   as [WaitTime_HMS] \n"
//					+ "    ,cast(sum([WaitTime])*1.0/sum([Waits])*1.0 as numeric(10,3)) as [AvgWaitTimeMs] \n"
//				    + "    ,sum([Waits])             as [Waits_sum] \n"
//				    + "from [CmSpidWait_diff] sw, cap \n"
//				    + "where sw.[SPID] = cap.[SPID] \n"
//				    + "  and sw.[KPID] = cap.[KPID] \n"
//				    + "  and sw.[SessionSampleTime] between dateadd(SECOND, -" + recSampleTimeInSec + ", cap.[StartTime]) and dateadd(SECOND, " + recSampleTimeInSec + ", cap.[EndTime]) \n"
////					+ "  and sw.[SessionSampleTime] between cap.[StartTime] and cap.[EndTime] \n"
//				    + "  and sw.[WaitTime] > 0 \n"
//				    + "  and sw.[WaitEventID] != 250 \n"
//				    + "group by [WaitEventID] \n"
//				    + "order by [WaitTime_sum] desc \n"
//				    + "";
//
//			sql = conn.quotifySqlString(sql);
//
//			try ( Statement stmnt = conn.createStatement() )
//			{
//				// Unlimited execution time
//				stmnt.setQueryTimeout(0);
//				try ( ResultSet rs = stmnt.executeQuery(sql) )
//				{
//					ResultSetTableModel rstm = new ResultSetTableModel(rs, "Wait Detailes for: " + whereColValMap.entrySet());
//
//					setDurationColumn(rstm, "SessionSampleTime_min", "SessionSampleTime_max", "Duration");
//					setMsToHms       (rstm, "WaitTime_sum", "WaitTime_HMS");
//
//					waitEntry.waitTime = rstm;
//				}
//			}
//			catch(SQLException ex)
//			{
//				_logger.warn("Problems getting SQL Text by: " + whereColValMap + ": " + ex);
//			}
//		}
//
//		// Root Cause SQL Text
//		if (getRootCauseInfo)
//		{
//			// DUE TO PERFORMANCE THIS IS DONE in 2 steps
//			//   1 - Get info about who is BLOCKING (time and SPID)
//			//   2 - with the above query... create a new query where the above ResultSet is injected as a *static* CTE (Common Table Expression)
//			//       the "static in-line CTE" will act as a "temporary table", without having to create a temporary table...
//			
//			String sql = ""
//				    + "select distinct \n"
////				    + "     sw.[SPID] \n"
////				    + "    ,sw.[KPID] \n"
////				    + "    ,sw.[BatchID] \n"
//				    + "     sw.[SessionSampleTime] \n"
//				    + "    ,sw.[BlockingSPID] \n"
////				    + "    ,sw.[WaitEventID] \n"
//				    + "from [CmActiveStatements_abs] sw, \n"
//				    + "( \n"
//				    + "    select \n"
//				    + "         [SPID] \n"
//				    + "        ,[KPID] \n"
//				    + "        ,[BatchID] \n"
//				    + "        ,[StartTime] \n"
//				    + "        ,[EndTime] \n"
//				    + "        ,[WaitTime] \n"
////				    + "    --  ,(select [colVal] from [MonSqlCapStatements$dcc$SQLText] i where i.[hashId] = o.[SQLText$dcc$]) as [SourceSQLText] \n"
//				    + "    from [MonSqlCapStatements] o \n"
//				    + "    where 1=1 \n"
//				    +      whereColValStr
//				    + "      and [WaitTime] > 0 \n"
//				    + ") cap \n"
//				    + "where sw.[SPID]    = cap.[SPID] \n"
//				    + "  and sw.[KPID]    = cap.[KPID] \n"
//				    + "  and sw.[BatchID] = cap.[BatchID] \n"
//				    + "  and sw.[SessionSampleTime] between dateadd(SECOND, -" + recSampleTimeInSec + ", cap.[StartTime]) and dateadd(SECOND, " + recSampleTimeInSec + ", cap.[EndTime]) \n"
//				    + "  and sw.[WaitTime] > 0 \n"
////				    + "  and sw.[WaitEventID] = 150 -- waiting for a lock \n"
//				    + "  and sw.[BlockingSPID] != 0 -- someone is blocking this SPID \n"
//				    + "";
//			
//			try
//			{
//				ResultSetTableModel tmpRstm = executeQuery(conn, sql, "tmpGetBlockingSessions");
//
//				if (tmpRstm.getRowCount() > 0)
//				{
//					String iasCte = tmpRstm.createStaticCte("ias");
//					
//					// Now create the "real" SQL using "iasCte" which was generated above
//					sql = ""
//							+ iasCte
//						    + "select \n"
//						    + "     oas.[SPID] \n"
//						    + "    ,oas.[KPID] \n"
//						    + "    ,oas.[StartTime] \n"
//						    + "    ,max(oas.[BlockingOthersMaxTimeInSec]) as [BlockingOthersMaxTimeInSec] \n"
////						    + "--  ,max(oas.[BlockingOtherSpids]) as [BlockingOtherSpids] \n"
//						    + "    ,max(oas.[ExecTimeInMs])       as [ExecTimeInMs] \n"
//						    + "    ,max(oas.[CpuTime])            as [CpuTime] \n"
//						    + "    ,max(oas.[WaitTime])           as [WaitTime] \n"
////						    + "--  ,max(oas.[UsefullExecTime])    as [UsefullExecTime] \n"
////						    + "--  ,max(oas.[MemUsageKB])         as [MemUsageKB] \n"
//						    + "    ,max(oas.[PhysicalReads])      as [PhysicalReads] \n"
//						    + "    ,max(oas.[LogicalReads])       as [LogicalReads] \n"
//						    + "    ,max(oas.[RowsAffected])       as [RowsAffected] \n"
//						    + "    ,max(select [colVal] from [CmActiveStatements$dcc$MonSqlText]   i where i.[hashId] = oas.[MonSqlText$dcc$]) as [MonSqlText] \n"
////						    + "    ,max(select [colVal] from [CmActiveStatements$dcc$ShowPlanText] i where i.[hashId] = oas.[ShowPlanText$dcc$]) as [ShowPlanText] \n"
//						    + "from [CmActiveStatements_abs] oas, ias \n"
//						    + "where oas.[SessionSampleTime] = ias.[SessionSampleTime] \n"
//						    + "  and oas.[SPID]              = ias.[BlockingSPID] \n"
//						    + "group by \n"
//						    + "     oas.[SPID] \n"
//						    + "    ,oas.[KPID] \n"
//						    + "    ,oas.[StartTime] \n"
//						    + "order by oas.[StartTime] \n"
//						    + "";
//
//					ResultSetTableModel rootCauseInfo = executeQuery(conn, sql, "rootCauseInfo for:" + whereColValMap.entrySet());
//					waitEntry.rootCauseInfo = rootCauseInfo;
//				}
//			}
//			catch (SQLException ex)
//			{
//				_logger.warn("Problems getting Root Cause for: " + whereColValMap + ": " + ex);
//			}
//		}
//		return map;
//	}
//	//--------------------------------------------------------------------------------
//	// END: SQL Capture - get WAIT
//	//--------------------------------------------------------------------------------

	//--------------------------------------------------------------------------------
	// BEGIN: SQL Capture - get WAIT
	//--------------------------------------------------------------------------------
	public static class SqlCapWaitEntry
	{
		ResultSetTableModel waitTime;
		String              waitTimeProblem;
		ResultSetTableModel rootCauseInfo;
		String              rootCauseInfoProblem;
	}

	public String getSqlCapWaitTimeAsString(Map<Map<String, Object>, SqlCapWaitEntry> map, Map<String, Object> whereColValMap)
	{
		if (map == null)
			return "";

		SqlCapWaitEntry entry = map.get(whereColValMap);
		
		if (entry == null)
		{
			return "No entry for '"+ whereColValMap +"' was found. known keys: " + map.keySet();
		}

		if (entry.waitTime == null && entry.rootCauseInfo == null)
		{
			return "Entry for '"+ whereColValMap +"' was found. But NO INFORMATION has been assigned to it.";
		}
		
		StringBuilder sb = new StringBuilder();

		// Add some initial text
		if (entry.waitTime != null)
		{
			sb.append("-------------------------------------------------------------------------------------------------------------------------------\n");
			sb.append("-- Wait Information... or WaitEvents that this statement has been waited on. \n");
			sb.append("-- However no 'individual' WaitEventID statistics is recorded in [monSysStatements] which is the source for [MonSqlCapStatements] \n");
			sb.append("-- So the below information is fetched from [MonSqlCapWaitInfo] for the approximate time, which holds Wait Statistics on a SPID level... \n");
			sb.append("-- therefore WaitTime which is not relevant for *this* specific statement will/may be included. \n");
			sb.append("-- If the client connections are 'short lived' (login; exec; logout;) NO wait information will be available. \n");
			sb.append("-- So see the below values as a guidance! \n");
			sb.append("-------------------------------------------------------------------------------------------------------------------------------\n");
			sb.append("\n");

			// Add WAIT Text
			sb.append( StringEscapeUtils.escapeHtml4(entry.waitTime.toAsciiTableString()) ).append("\n");
			sb.append("\n");
			sb.append("\n");
		}
		else
		{
			sb.append("-------------------------------------------------------------------------------------------------------------------------------\n");
			sb.append("-- Problems getting Wait Information:\n");
			sb.append("-- ").append( StringEscapeUtils.escapeHtml4(entry.waitTimeProblem) ).append("\n");
			sb.append("-------------------------------------------------------------------------------------------------------------------------------\n");
		}
		
		// Add some text about: Root Cause SQL Text
		if (entry.rootCauseInfo != null)
		{
			sb.append("-------------------------------------------------------------------------------------------------------------------------------\n");
			sb.append("-- Root Cause for Blocking Locks:\n");
			sb.append("-- Below we try to investigate any 'root cause' for blocking locks that was found in above section.\n");
			sb.append("--\n");
			sb.append("-- Algorithm used to reveile 'root cause' for 'blocking lock(s)' : \n");
			sb.append("--  - Grab records where BlockedBy{Spid|Kpid|BatchId} != 0 from [MonSqlCapStatements] where [NormJavaSqlHashCode] = ? \n");
			sb.append("--  - using the above: grab 'root cause spids', etc, SQLText from [MonSqlCapStatements] ... \n");
			sb.append("-- NOTE: This is NOT a 100% truth of the story, due to: \n");
			sb.append("--         - [MonSqlCapSpidInfo] is only stored/snapshoted every 'SQL Cap Sample Time' (1 seconds or similar) \n");
			sb.append("--           so we can/WILL MISS everything in between (statements that has already finnished and continued processing...) \n");
			sb.append("-- Use this values as a guidance\n");
			sb.append("-------------------------------------------------------------------------------------------------------------------------------\n");
			sb.append("\n");

			// Remove any HTML tags...
			//entry.rootCauseInfo.stripHtmlTags("MonSqlText");
			int pos_MonSqlText = entry.rootCauseInfo.findColumn("MonSqlText");
			if (pos_MonSqlText != -1)
			{
				for (int r=0; r<entry.rootCauseInfo.getRowCount(); r++)
				{
					String monSqlText = entry.rootCauseInfo.getValueAsString(r, pos_MonSqlText);
					if (StringUtil.hasValue(monSqlText))
					{
						monSqlText = monSqlText.replace("<html><pre>", "").replace("</pre></html>", "");
						entry.rootCauseInfo.setValueAtWithOverride(monSqlText, r, pos_MonSqlText);
					}
				}
			}

			// Add RootCause Text
			sb.append( StringEscapeUtils.escapeHtml4(entry.rootCauseInfo.toAsciiTableString()) ).append("\n");
			sb.append("\n");
			sb.append("\n");
		}
		else
		{
			if (StringUtil.hasValue(entry.rootCauseInfoProblem))
			{
				sb.append("-------------------------------------------------------------------------------------------------------------------------------\n");
				sb.append("-- Problems getting Root Cause for Blocking Locks:\n");
				sb.append("-- ").append( StringEscapeUtils.escapeHtml4(entry.rootCauseInfoProblem) ).append("\n");
				sb.append("-------------------------------------------------------------------------------------------------------------------------------\n");
			}
			else
			{
				sb.append("-------------------------------------------------------------------------------------------------------------------------------\n");
				sb.append("-- Root Cause for Blocking Locks:\n");
				sb.append("-- No blocking lock information was found... NOTE: This might not be 100% accurate.\n");
				sb.append("-------------------------------------------------------------------------------------------------------------------------------\n");
			}
		}
		
		return sb.toString();
	}

	public Map<Map<String, Object>, SqlCapWaitEntry> getSqlCapWaitTime(Map<Map<String, Object>, SqlCapWaitEntry> map, DbxConnection conn, boolean hasDictCompCols, Map<String, Object> whereColValMap)
	{
//		int topRows = getSqlCapExecutedSqlText_topCount();
//
//		String sqlTopRows     = topRows <= 0 ? "" : " top " + topRows + " ";
//		String tabName        = "MonSqlCapStatements";
//		String sqlTextColName = "SQLText";
////		String sqlTextColName = "SQLText" + DictCompression.DCC_MARKER;  shouldnt we check if DCC is enabled or not?
//
////		if (DictCompression.hasCompressedColumnNames(conn, null, tabName))
//		if (hasDictCompCols)
//			sqlTextColName += DictCompression.DCC_MARKER;
//				
//		String col_NormSQLText = DictCompression.getRewriteForColumnName(tabName, sqlTextColName);

//		int recSampleTimeInSec = getReportingInstance().getRecordingSampleTime();
		int recSampleTimeInSec = 1; // 1 second is the normal time the SqlCapture subsystem is sleeping between samples

		if (map == null)
			map = new HashMap<>();

		SqlCapWaitEntry waitEntry = map.get(whereColValMap);
		if (waitEntry == null)
		{
			waitEntry = new SqlCapWaitEntry();
			map.put(whereColValMap, waitEntry);
		}


		String whereColValStr = "";
		for (Entry<String, Object> entry : whereColValMap.entrySet())
		{
			whereColValStr += "      and [" + entry.getKey() + "] = " + DbUtils.safeStr( entry.getValue() ) + "\n";
		}

		boolean getWaitInfo      = true;
		boolean getRootCauseInfo = true;

		if (getWaitInfo)
		{
			String sql = ""
				    + "with cap as \n"
				    + "( \n"
				    + "    select [SPID] \n"
				    + "          ,[KPID] \n"
				    + "          ,[BatchID] \n"
				    + "          ,[StartTime] \n"
				    + "          ,[EndTime] \n"
//				    + "          ,(select [colVal] from [MonSqlCapStatements$dcc$SQLText] i where i.[hashId] = o.[SQLText$dcc$]) as [SQLText] \n"
				    + "    from [MonSqlCapStatements] o \n"
				    + "    where 1=1 \n"
				    +      whereColValStr
//				    + "      and [WaitTime] > 0 \n"
				    + ") \n"
				    + "select \n"
				    + "     min([sampleTime]) as [sampleTime_min] \n"
				    + "    ,max([sampleTime]) as [sampleTime_max] \n"
				    + "    ,cast('' as varchar(8))   as [Duration] \n"
				    + "    ,count(*)                 as [cnt] \n"
				    + "    ,cast('' as varchar(60))  as [WaitClassDesc] \n"
				    + "    ,cast('' as varchar(60))  as [WaitEventDesc] \n"
				    + "    ,max([WaitClassID])       as [WaitClassID] \n"
				    + "    ,[WaitEventID] \n"
				    + "    ,CASE WHEN [WaitEventID] = 150 THEN '>>>>' ELSE '' END as [Note] \n"
				    + "    ,sum([Waits_diff])        as [Waits_sum] \n"
				    + "    ,sum([WaitTime_diff])     as [WaitTime_sum] \n"
				    + "    ,max([WaitTime_diff])     as [WaitTime_max] \n"
				    + "    ,cast('' as varchar(8))   as [WaitTime_HMS] \n"
					+ "    ,CASE WHEN sum([Waits_diff]) > 0 THEN cast(sum([WaitTime_diff])*1.0/sum([Waits_diff])*1.0 as numeric(10,3)) ELSE null END as [AvgWaitTimeMs] \n"
				    + "from [MonSqlCapWaitInfo] sw, cap \n"
				    + "where sw.[SPID] = cap.[SPID] \n"
				    + "  and sw.[KPID] = cap.[KPID] \n"
				    + "  and sw.[sampleTime] between dateadd(SECOND, -" + recSampleTimeInSec + ", cap.[StartTime]) and dateadd(SECOND, " + recSampleTimeInSec + ", cap.[EndTime]) \n"
				    + "  and sw.[WaitTime_diff] > 0 \n"
//				    + "  and sw.[WaitEventID] != 250 \n"
				    + "group by [WaitEventID] \n"
				    + "order by [WaitTime_sum] desc \n"
				    + "";

			sql = conn.quotifySqlString(sql);

			try ( Statement stmnt = conn.createStatement() )
			{
				// Unlimited execution time
				stmnt.setQueryTimeout(0);
				try ( ResultSet rs = stmnt.executeQuery(sql) )
				{
					ResultSetTableModel rstm = new ResultSetTableModel(rs, "Wait Detailes for: " + whereColValMap.entrySet());
					
					rstm.setToStringTimestampFormat("yyyy-MM-dd HH:mm:ss");

					setWaitDesciption(rstm, "WaitEventID", "WaitEventDesc", "WaitClassID", "WaitClassDesc");
					setDurationColumn(rstm, "sampleTime_min", "sampleTime_max", "Duration");
					setMsToHms       (rstm, "WaitTime_sum", "WaitTime_HMS");

					waitEntry.waitTime = rstm;
				}
			}
			catch(SQLException ex)
			{
				waitEntry.waitTimeProblem = "Problems getting 'Wait Information' for: " + whereColValMap + ": " + ex;
				_logger.warn("Problems getting SQL Text by: " + whereColValMap + ": " + ex);
			}
		}

		// Root Cause SQL Text
		if (getRootCauseInfo)
		{
			String sql = ""
				    + "select \n"
				    + "     rc.[SPID] \n"
				    + "    ,rc.[KPID] \n"
				    + "    ,rc.[StartTime] \n"
				    + "    ,rc.[EndTime] \n"
				    + "    ,count(rc.*)                     as [BlkCnt] \n"
				    + "    ,rc.[LineNumber]                 as [Line] \n"
				    + "    ,max(rc.[Elapsed_ms])            as [Elapsed_ms] \n"
				    + "    ,max(rc.[CpuTime])               as [CpuTime] \n"
				    + "    ,max(rc.[WaitTime])              as [WaitTime] \n"
				    + "    ,max(rc.[PhysicalReads])         as [PhysReads] \n"
				    + "    ,max(rc.[LogicalReads])          as [LogicalReads] \n"
				    + "    ,max(rc.[RowsAffected])          as [RowsAffected] \n"
				    + "    ,max(vic.[BlockedByCommand])     as [Command] \n"
				    + "    ,max(vic.[BlockedByApplication]) as [Application] \n"
				    + "    ,max(vic.[BlockedByTranId])      as [TranId] \n"
//				    + "    ,max(select [colVal] from [MonSqlCapStatements$dcc$SQLText] i where i.[hashId] = rc.[SQLText$dcc$]) as [MonSqlText] \n"
//				    + "    ,max(select [colVal] from [MonSqlCapStatements$dcc$SQLText] i where i.[hashId] = vic.[SQLText$dcc$]) as [VictimSqlText] \n"
				    + "    ,max(select [colVal] from [MonSqlCapStatements$dcc$BlockedBySqlText] i where i.[hashId] = vic.[BlockedBySqlText$dcc$]) as [BlockedBySqlText] \n"
				    + "from [MonSqlCapStatements] vic \n"
				    + "join [MonSqlCapStatements] rc on vic.[BlockedBySpid] = rc.[SPID] and vic.[BlockedByKpid] = rc.[KPID] and vic.[BlockedByBatchId] = rc.[BatchID] \n"
				    + "where 1=1 \n"
				    +  whereColValStr.replace(" and [", " and vic.[")  // Add 'vic.' to the where clause otherwise we get: org.h2.jdbc.JdbcSQLSyntaxErrorException: Ambiguous column name "NormJavaSqlHashCode";
				    + "  and vic.[BlockedBySpid]    != 0 \n"
				    + "  and vic.[BlockedByKpid]    != 0 \n"
				    + "  and vic.[BlockedByBatchId] != 0 \n"
//				    + "  and vic.[ContextID] > 0 \n"
//				    + "  and  rc.[ContextID] > 0 \n"
				    + "  and vic.[CpuTime]  > 0 \n"  // Procedure "header" has CpuTime=0 and WaitTime=0
				    + "  and  rc.[CpuTime]  > 0 \n"  // Procedure "header" has CpuTime=0 and WaitTime=0
				    + "  and vic.[WaitTime] > 0 \n"  // Procedure "header" has CpuTime=0 and WaitTime=0
				    + "  and  rc.[WaitTime] > 0 \n"  // Procedure "header" has CpuTime=0 and WaitTime=0
				    + "group by \n"
				    + "   rc.[SPID] \n"
				    + "  ,rc.[KPID] \n"
				    + "  ,rc.[StartTime] \n"
				    + "  ,rc.[EndTime] \n"
				    + "  ,rc.[LineNumber] \n"
				    + "order by [StartTime] \n"
//				    + "order by [CpuTime] desc \n"
				    + "";

			try
			{
				ResultSetTableModel rootCauseInfo = executeQuery(conn, sql, "rootCauseInfo for:" + whereColValMap.entrySet());
				waitEntry.rootCauseInfo = rootCauseInfo;
				
				if (rootCauseInfo.getRowCount() == 0)
					waitEntry.rootCauseInfo = null;
			}
			catch (SQLException ex)
			{
				waitEntry.rootCauseInfoProblem = "Problems getting Root Cause for: " + whereColValMap + ": " + ex;
				_logger.warn("Problems getting Root Cause for: " + whereColValMap + ": " + ex);
			}
		}
		return map;
	}

	/**
	 * Lookup and set the WaitEvent and WaitClass descriptions
	 * 
	 * @param rstm
	 * @param waitEventId
	 * @param waitEventDesc
	 * @param waitClassId
	 * @param waitClassDesc
	 */
	public void setWaitDesciption(ResultSetTableModel rstm, String waitEventId, String waitEventDesc, String waitClassId, String waitClassDesc)
	{
		int pos_waitEventId   = rstm.findColumn(waitEventId);
		int pos_waitEventDesc = rstm.findColumn(waitEventDesc);
		
		int pos_waitClassId   = rstm.findColumn(waitClassId);
		int pos_waitClassDesc = rstm.findColumn(waitClassDesc);
		
//		if ( ! MonTablesDictionaryManager.hasInstance() )
//		{
//			_logger.warn("setWaitDesciption(): No 'MonTablesDictionary' was found... cant set descriptions...");
//			return;
//		}
//		MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
		
		// waitEventId -->> waitEventDesc
		if (pos_waitEventId >= 0 && pos_waitEventDesc >= 0)
		{
			for (int r=0; r<rstm.getRowCount(); r++)
			{
				Integer eventId = rstm.getValueAsInteger(r, pos_waitEventId);
				if (eventId != null)
				{
//					String desc = mtd.getWaitEventDescription(eventId);
					String desc = getWaitEventDescription(eventId);
					rstm.setValueAtWithOverride(desc, r, pos_waitEventDesc);
				}
			}
		}

		// waitClassId -->> waitClassDesc
		if (pos_waitClassId >= 0 && pos_waitClassDesc >= 0)
		{
			for (int r=0; r<rstm.getRowCount(); r++)
			{
				Integer classId = rstm.getValueAsInteger(r, pos_waitClassId);
				if (classId != null)
				{
//					String desc = mtd.getWaitClassDescription(classId);
					String desc = getWaitClassDescription(classId);
					rstm.setValueAtWithOverride(desc, r, pos_waitClassDesc);
				}
			}
		}
	}

	/**
	 * NOTE: This should REALLY be grabbed from the PCS (but it's not currently saved... so lets go ststic for now)
	 * @param classId
	 * @return A description. NULL if not found
	 */
	private String getWaitClassDescription(int classId)
	{
		switch (classId)
		{
			case 0: return "Process is running";
			case 1: return "waiting to be scheduled";
			case 2: return "waiting for a disk read to complete";
			case 3: return "waiting for a disk write to complete";
			case 4: return "waiting to acquire the log semaphore";
			case 5: return "waiting to take a lock";
			case 6: return "waiting for memory or a buffer";
			case 7: return "waiting for input from the network";
			case 8: return "waiting to output to the network";
			case 9: return "waiting for internal system event";
			case 10: return "waiting on another thread";
		}
		
		return null;
	}

	/**
	 * NOTE: This should REALLY be grabbed from the PCS (but it's not currently saved... so lets go ststic for now)
	 * @param eventId
	 * @return A description. NULL if not found
	 */
	private String getWaitEventDescription(int eventId)
	{
		switch (eventId)
		{
			case 0:  return "Process is running";
			case 11: return "recovery testing: pause to bring server down";
			case 12: return "recovery testing: pause to bring server down";
			case 13: return "recovery testing: pause to bring server down";
			case 14: return "recovery testing: pause to bring server down";
			case 15: return "recovery testing: pause to bring server down";
			case 16: return "recovery testing: pause to bring server down";
			case 17: return "recovery testing: pause to bring server down";
			case 18: return "xact coord: xact state update retry delay";
			case 19: return "xact coord: pause during idle loop";
			case 20: return "xact coord: pause during shutdown";
			case 21: return "xact coord: pause waiting for tasks to complete";
			case 22: return "auditing: waiting on notification routine";
			case 23: return "btree testing: pause for synchronisation";
			case 24: return "btree testing: pause for synchronisation";
			case 25: return "btree testing: pause for synchronisation";
			case 26: return "btree testing: pause for synchronisation";
			case 27: return "btree testing: pause for synchronisation";
			case 28: return "btree testing: pause for synchronisation";
			case 29: return "waiting for regular buffer read to complete";
			case 30: return "wait to write MASS while MASS is changing";
			case 31: return "waiting for buf write to complete before writing";
			case 32: return "waiting for an APF buffer read to complete";
			case 33: return "waiting for buffer read to complete";
			case 34: return "waiting for buffer write to complete";
			case 35: return "waiting for buffer validation to complete";
			case 36: return "waiting for MASS to finish writing before changing";
			case 37: return "wait for MASS to finish changing before changing";
			case 38: return "wait for mass to be validated or finish changing";
			case 39: return "wait for private buffers to be claimed";
			case 40: return "wait for diagnostic write to complete";
			case 41: return "wait to acquire latch";
			case 44: return "wait while database is quiesced";
			case 45: return "access methods testing: pause for synchronisation";
			case 46: return "wait for buf write to finish getting buf from LRU";
			case 47: return "wait for buf read to finish getting buf from LRU";
			case 48: return "wait to finish buf write when using clock scheme";
			case 49: return "wait to finish buf read when using clock scheme";
			case 50: return "write restarted due to device error (check OS log)";
			case 51: return "waiting for last i/o on MASS to complete";
			case 52: return "waiting for i/o on MASS initated by another task";
			case 53: return "waiting for MASS to finish changing to start i/o";
			case 54: return "waiting for write of the last log page to complete";
			case 55: return "wait for i/o to finish after writing last log page";
			case 56: return "checkpoint waiting for system databases to recover";
			case 57: return "checkpoint process idle loop";
			case 58: return "housekeeper wait for system databases to recover";
			case 59: return "hk: unused";
			case 60: return "hk: sleep until woken up";
			case 61: return "hk: pause for some time";
			case 62: return "probe: waiting for remote server to come up";
			case 63: return "probe: waiting for commit to be accepted";
			case 64: return "create index testing: unending sleep";
			case 65: return "waiting for listener sockets to be opened";
			case 66: return "nw handler: waiting for sys db recovery";
			case 67: return "nw handler: waiting for user task shutdown";
			case 69: return "wait while MASS is changing before removing it";
			case 70: return "waiting for device semaphore";
			case 71: return "test routine wait for read to complete";
			case 72: return "wait for write to complete";
			case 73: return "wait for mirrored write to complete";
			case 74: return "dbshutdown: wait until all tasks have synchronised";
			case 75: return "dbshutdown: wait for keep count to be zero";
			case 76: return "dbshutdown: timeout for failback retry";
			case 77: return "dropdb:wait for system tasks to complete in marking dbt";
			case 78: return "wait for system tasks to exit not in single mode";
			case 79: return "wait for system tasks to exit in single mode";
			case 80: return "pause if DB already being droped or created";
			case 81: return "pause for system tasks to complete when locking DB";
			case 82: return "index shrink test: pause for synchronisation";
			case 83: return "wait for DES state is changing";
			case 84: return "wait for checkpoint to complete in des_markdrop()";
			case 85: return "wait for flusher to queue full DFLPIECE";
			case 86: return "pause while dumpdb dumps stuff out";
			case 87: return "wait for page write to complete";
			case 88: return "wait for file to be created";
			case 89: return "wait for space in ULC to preallocate CLR";
			case 90: return "wait after error in ULC while logging CLR";
			case 91: return "waiting for disk buffer manager i/o to complete";
			case 92: return "waiting for synchronous disk buffer manager i/o";
			case 93: return "Waiting for mirrored read";
			case 94: return "Waiting for mirrored write";
			case 95: return "distributed xact co-ordinator idle loop";
			case 96: return "waiting for MS DTC service to start";
			case 97: return "waiting for MS DTC service to start while polling";
			case 98: return "sleep until xact is aborted";
			case 99: return "wait for data from client";
			case 100: return "wait for all i/o to complete before closing file";
			case 101: return "wait for a new engine to start up";
			case 102: return "wait until engine ready to offline";
			case 104: return "wait until an engine has been offlined";
			case 106: return "pause to synchronise with offlining engine";
			case 107: return "wait until all listeners terminate";
			case 108: return "final wait before offlining engine";
			case 109: return "sleep for completion of system command";
			case 110: return "wait for xpserver to start";
			case 111: return "wait for xpserver to terminate";
			case 112: return "wait for XP Server semaphore";
			case 113: return "pause before starting automatic mailer";
			case 114: return "wait for debugger to attach";
			case 115: return "wait for exercisers to be awakened";
			case 116: return "wait for exercisers to start completely";
			case 117: return "wait for exercisers to be awakened by the last user";
			case 118: return "wait for probe connection to be terminated";
			case 119: return "wait for last thread to run sanity checks";
			case 120: return "wait for file I/O semaphore";
			case 121: return "sleeping while file is busy or locked";
			case 122: return "wait for space in ULC to log commit record";
			case 123: return "wait after error logging commit record in ULC";
			case 124: return "wait for mass read to finish when getting page";
			case 125: return "wait for read to finish when getting cached page";
			case 126: return "waiting for node failure";
			case 127: return "waiting for node failover";
			case 128: return "waiting for failback to complete";
			case 129: return "index split testing: wait for manual intervention";
			case 130: return "wait for the work to be produced";
			case 131: return "wait for the work to be finished";
			case 132: return "wait for exerciser threads to complete";
			case 133: return "pause while target thread starts work";
			case 134: return "java thread timed wait";
			case 135: return "java thread indefinite wait";
			case 136: return "java sleep()";
			case 137: return "wait in heapwalk for GC to complete";
			case 138: return "wait during alloc for GC to complete";
			case 139: return "wait during free for GC to complete";
			case 140: return "wait to grab spinlock";
			case 141: return "wait for all processes to stop";
			case 142: return "wait for logical connection to free up";
			case 143: return "pause to synchronise with site manager";
			case 144: return "latch manager unit test";
			case 145: return "latch manager unit test";
			case 146: return "latch manager unit test";
			case 147: return "latch manager unit test";
			case 148: return "latch manager unit test";
			case 149: return "latch manager unit test";
			case 150: return "waiting for a lock";
			case 151: return "waiting for deadlock check configuration to change";
			case 152: return "wait for notification of flush";
			case 153: return "wait for XEND_PREP record to be rewritten";
			case 154: return "wait for MDA mutex to become free";
			case 155: return "wait for memory to be returned to block pool";
			case 156: return "wait for memory to be returned to fragment pool";
			case 157: return "wait for object to be returned to pool";
			case 158: return "wait for DBT to be freed after error mounting DB";
			case 159: return "wait for DBT to be freed up after mounting DB";
			case 160: return "wait for master DBT to be freed in mount DB";
			case 161: return "wait during testing";
			case 162: return "wait for dbshutdown to complete";
			case 163: return "stress test: monitor idle loop";
			case 164: return "stress test: random delay to simulate work";
			case 165: return "stress test: delay to test alarm handling";
			case 166: return "stress test: random delay to simulate work";
			case 167: return "stress test: wait for disk write to complete";
			case 168: return "stress test: wait for disk read to complete";
			case 169: return "wait for message";
			case 170: return "wait for a mailbox event";
			case 171: return "waiting for CTLIB event to complete";
			case 172: return "waiting for data on NETLIB listener socket";
			case 173: return "waiting to complete accept on socket connection";
			case 174: return "waiting to complete close on socket connection";
			case 175: return "waiting to complete connect to remote site";
			case 176: return "waiting until listener socket is ready";
			case 177: return "waiting for NWKINFO structure to be ready";
			case 178: return "waiting for client connection request";
			case 179: return "waiting while no network read or write is required";
			case 180: return "wait for connection request";
			case 181: return "wait for connection to be completed";
			case 182: return "pause before sending disconnect packet";
			case 183: return "wait on control structure initialisation";
			case 184: return "wait for more connection attempts";
			case 185: return "wait for named pipe connection request";
			case 186: return "wait on non-DECNET connection completion";
			case 187: return "wait until DECNET connection has been completed";
			case 188: return "wait until named pipe disconnect is complete";
			case 189: return "wait until winsock disconnect is complete";
			case 190: return "wait until outgoing connection has been set up";
			case 191: return "waiting for child to release latch on OAM page";
			case 192: return "waiting to next object ID";
			case 193: return "wait while testing RID list locking in or.c";
			case 194: return "waiting for free BYTIO structure";
			case 195: return "waiting for map IO in parallel dbcc backout";
			case 196: return "waiting for wspg IO in parallel dbcc backout";
			case 197: return "waiting for read to complete in parallel dbcc";
			case 198: return "waiting on OAM IO in parallel dbcc backout";
			case 199: return "waiting for scan to complete in parallel dbcc";
			case 200: return "waiting for page reads in parallel dbcc";
			case 201: return "waiting for disk read in parallel dbcc";
			case 202: return "waiting to re-read page in parallel dbcc";
			case 203: return "waiting on MASS_READING bit in parallel dbcc";
			case 204: return "waiting on MASS_READING on text pg in PLL dbcc";
			case 205: return "waiting on TPT lock in parallel dbcc";
			case 206: return "waiting in backout in map_tpt in PLL dbcc";
			case 207: return "waiting sending fault msg to parent in PLL dbcc";
			case 208: return "waiting for clients to close pipe";
			case 209: return "waiting for a pipe buffer to read";
			case 210: return "waiting for free buffer in pipe manager";
			case 211: return "waiting for free proc buffer";
			case 212: return "waiting for target task to terminate";
			case 213: return "this task stopped for debugging";
			case 214: return "waiting on run queue after yield";
			case 215: return "waiting on run queue after sleep";
			case 216: return "quiesce db hold waiting for agent to effect hold";
			case 217: return "quiesce db hold killing agent after attention";
			case 218: return "quiesce DB agent waiting for release signal";
			case 219: return "quiesce DB agent draining prepared transactions";
			case 220: return "quiesce DB agent paused in trace flag test hook";
			case 221: return "RAT Sleeping in retry sleep";
			case 222: return "RAT Sleeping during flush";
			case 223: return "RAT Sleeping during rewrite";
			case 224: return "sleeping in recovery due to traceflag 3427";
			case 225: return "sleeping in recovery due to traceflag 3442";
			case 226: return "sleeping in recovery due to traceflag 3428";
			case 227: return "waiting for site handler to be created";
			case 228: return "waiting for connections to release site handler";
			case 229: return "waiting for site handler to quit";
			case 230: return "waiting for site handler to complete setup";
			case 231: return "waiting in freesite for site handler to quit";
			case 232: return "waiting for slice manager command to complete";
			case 233: return "waiting for active processes during shutdown";
			case 234: return "site handler waiting to be created";
			case 235: return "waiting in sort manager due to traceflag 1512";
			case 236: return "waiting in sort manager due to traceflag 1513";
			case 237: return "waiting in sort manager due to traceflag 1514";
			case 238: return "waiting in sort manager due to traceflag 1516";
			case 239: return "waiting in sort manager due to traceflag 1517";
			case 240: return "waiting in sort manager due to traceflag 1510";
			case 241: return "waiting in sort manager due to traceflag 1511";
			case 242: return "waiting in sort manager due to traceflag 1521";
			case 243: return "waiting in sort manager due to traceflag 1522";
			case 244: return "waiting in sort manager due to traceflag 1523";
			case 245: return "waiting in sort manager due to traceflag 1524";
			case 246: return "waiting in sort manager due to traceflag 1525";
			case 247: return "sleeping on the network listenfail event";
			case 248: return "waiting after sending socket migration request";
			case 249: return "waiting to receive socket migration completion";
			case 250: return "waiting for incoming network data";
			case 251: return "waiting for network send to complete";
			case 252: return "waiting for KSM migrate semaphore";
			case 253: return "waiting for socket to finish migrating";
			case 254: return "wait for CAPS spinlock to become free";
			case 255: return "waiting for IDES state to change";
			case 256: return "waiting for IDES to finish being destroyed";
			case 257: return "waiting for IDES keep count to be zero";
			case 258: return "waiting for thread manager semaphore";
			case 259: return "waiting until logsegment last chance threshold is cleared";
			case 260: return "waiting for date or time in waitfor command";
			case 261: return "waiting for error event in waitfor command";
			case 262: return "waiting for process to exit in waitfor command";
			case 263: return "waiting for mirror to exit in waitfor command";
			case 266: return "waiting for message in worker thread mailbox";
			case 267: return "sleeping because max native threads exceeded";
			case 268: return "waiting for native thread to finish";
			case 269: return "waiting for all XLS perf test tasks to finish";
			case 270: return "waiting for all XLS perf test tasks to login";
			case 271: return "waiting for log space during ULC flush";
			case 272: return "waiting for lock on ULC";
			case 273: return "waiting for cild to start in XLS unit test";
			case 274: return "waiting for children to finish XLS unit test";
			case 275: return "XLS unit test waiting for barrier";
			case 276: return "killed process waiting on HA Failback";
			case 277: return "waiting for HA failback to complete";
			case 278: return "wait for killed tasks to die";
			case 279: return "sleep on des cloned state if tf2792 on in des_in_use_error()";
			case 280: return "wait for access to a memory manager semaphore";
			case 281: return "waiting for killed tasks going away";
			case 282: return "wait for checkpoint to complete in des_clone()";
			case 283: return "Waiting for Log writer to complete";
			case 284: return "waiting for DECNET NWKINFO structure to be ready";
			case 285: return "wait for HBC connection request";
			case 286: return "wait for client to release default buffer";
			case 287: return "wait for input from an HBC socket";
			case 288: return "wait for HBC client to close the connection";
			case 289: return "wait for (non-tds) connect to complete";
			case 290: return "wait for (non-tds) data from client";
			case 291: return "wait for (non-tds) write to complete";
			case 292: return "waiting for network read callback to complete";
			case 293: return "waiting for network send callback to complete";
			case 294: return "wait for duplicate des to be dropped/destroyed";
			case 295: return "wait for a buffer from the HBC pool";
			case 296: return "wait for clients to close before HBC shutdown";
			case 297: return "wait for ASE tasks to close before HBC shutdown";
			case 298: return "waiting for lock from lock object pool";
			case 299: return "wait for heap memory for backout tasks";
			case 300: return "waiting to issue a SSL read";
			case 301: return "waiting for a SSL read to complete";
			case 302: return "waiting to issue a SSL write";
			case 303: return "Task is waiting on a breakpoint";
			case 304: return "not implemented";
			case 305: return "waiting for QUIESCE DB agent to effect release";
			case 306: return "Task waiting for seq. no. range to be made safe";
			case 307: return "Waiting for tasks to queue ALS request.";
			case 308: return "Waiting for ULC Flusher to queue dirty pages.";
			case 309: return "Waiting for last started disk write to complete";
			case 310: return "Unmount database draining prepared transactions";
			case 311: return "dbshutdown waiting before reaching first barrier";
			case 312: return "dbshutdown waiting to start termination";
			case 313: return "recovery testing: pause to bring server down";
			case 314: return "Dump DB in diagnostic sleep loop, under trace flag";
			case 315: return "Waiting for write to complete on ecache";
			case 316: return "Waiting for ecache buffer copy to complete";
			case 317: return "Waiting for ecache buffer read to complete";
			case 318: return "Waiting for a virtual address to be free";
			case 319: return "Waiting in RTMS thread spawn for native threads";
			case 320: return "waiting for RTMS thread to finish";
			case 321: return "waiting for RTMS thread to finish fading out";
			case 322: return "waiting for RTMS thread to terminate";
			case 323: return "waiting for lava family semaphore";
			case 324: return "waiting for PDES state to change";
			case 325: return "waiting for PDES to finish being destroyed";
			case 326: return "waiting for PDES to finish flushing datachange";
			case 327: return "waiting for scan finish before activating device";
			case 328: return "waiting for scan finish before deactivating device";
			case 329: return "des_putdlevel0cnt() wait for checkpoint completion";
			case 330: return "pausing to re-sample dbinfo log first page number";
			case 331: return "waiting for site buffer to be freed";
			case 332: return "RAT Sleeping on dirty log mass";
			case 333: return "waiting for proc buffer destruction completion";
			case 334: return "waiting for Lava pipe buffer for write";
			case 335: return "waiting for Dump Database to complete";
			case 336: return "replication wait in writetext for indid to be set";
			case 337: return "buffer search is waiting for mass destruction";
			case 338: return "page allocation is waiting for mass destruction";
			case 339: return "waiting for mass destruction to complete";
			case 340: return "waiting to complete update of performance arrays";
			case 341: return "waiting for further updates to performance arrays";
			case 342: return "waiting for read to complete in reading archive";
			case 343: return "waiting for space in archive database";
			case 344: return "waiting for JRTMSServer response";
			case 345: return "waiting for JRTMSServer init port event";
			case 346: return "waiting for JRTMSServer admin or intr response";
			case 347: return "waiting for connection establishment on JVM";
			case 348: return "CMCC task waiting for disk read to complete";
			case 349: return "CMCC task wait for recovery to complete";
			case 350: return "CMCC wait for remote split/shrink to complete";
			case 351: return "CMCC task wait for BAST pending bit";
			case 352: return "CMCC task wait for transfer";
			case 353: return "CMCC task wait for physical lock release";
			case 354: return "CMCC task wait for physical lock acquisition";
			case 355: return "CMCC task wait for physical lock upgrade";
			case 356: return "CMCC task wait for physlock from another user";
			case 357: return "CMCC task wait for retrying physical lock";
			case 358: return "BCM Thread wait for request";
			case 359: return "BCM Thread first OAM wait for MASS Changing";
			case 360: return "CMCC task wait for last log page object lock";
			case 361: return "wait for checkpoint to complete in des_clone()";
			case 362: return "wait for DES PENDING_DROP state to change";
			case 363: return "(DESMGR) waiting for IDES to be revalidated";
			case 364: return "waiting for IDES to be revalidated";
			case 365: return "waiting for port manager to initialize";
			case 366: return "waiting for port manager to complete request";
			case 367: return "waiting for port manager to spawn listeners";
			case 368: return "waiting for port manager to terminate listeners";
			case 369: return "waiting for next port manager request";
			case 370: return "waiting for listeners to call back";
			case 371: return "OCM wait for clearing lock/data/bast pending flag";
			case 372: return "OCM wait for waitcount to be 0";
			case 373: return "OCM wait for releasing an in-use ocb";
			case 374: return "wait for lock pending/data pending to be cleared";
			case 375: return "OCM wait for finishing BAST handling";
			case 376: return "OCM wait for BAST for deadlock handling";
			case 377: return "OCM wait for checking TXCANCEL handling";
			case 378: return "wait for recovery to do non-indoubt lock process";
			case 379: return "wait for data pending to be cleared in FO recovery";
			case 380: return "lock/data pending to reset when OCM_ERR_DIDNTWAIT";
			case 381: return "PCM wait for messages";
			case 382: return "PCM wait for ACKs/replies";
			case 383: return "PCM wait for all fragments' ACKs/replies";
			case 384: return "PCM wait for event for daemon thread";
			case 385: return "indoubt to be reset when lock in OCM_ERR_INDOUBT";
			case 386: return "OCM wait for data to be initialized";
			case 387: return "granted indoubt process from non-recovery lock req";
			case 388: return "OCM TEST wait for node 1 to start chat";
			case 389: return "OCM wait for pushing data flag to be cleared";
			case 390: return "PCM TEST wait for CIPC messages are sent";
			case 392: return "CHECKPOINT wait for dbt state change";
			case 395: return "SHUTDOWN wait for other instance to go down";
			case 396: return "SHUTDOWN wait for cluster state change to complete";
			case 397: return "SHUTDOWN wait for remote instance to go down";
			case 398: return "SHUTDOWN wait for shutdown steps to complete";
			case 399: return "SHUTDOWN wait for active processes to complete";
			case 401: return "SRV_SCL wait for busy engine";
			case 404: return "RECOVERY wait for parallel recov. threads to exit";
			case 405: return "RECOVERY wait for reducing running recov. threads";
			case 406: return "RECOVERY wait for spawned recovery thread to exit";
			case 407: return "RECOVERY wait for other instances' replies";
			case 408: return "RECOVERY wait for replies from all the instances";
			case 411: return "CES wait for CIPC events(recv, done, or ces)";
			case 414: return "CIPCTEST wait for CIPC events";
			case 415: return "CIPCTEST wait for CIPC DMA events";
			case 417: return "CMS wait for CIPC events(recv, done, or ces)";
			case 420: return "THMGR wait for new event or new message";
			case 421: return "Wait for cross-instance free space sync. events";
			case 423: return "CLM wait for batched messages";
			case 424: return "CLM wait for local service messages";
			case 425: return "CLM wait for instance shutdown";
			case 426: return "CLM wait for cluster events or messages";
			case 427: return "CLM wait for global request messages";
			case 430: return "WLMGR wait for CES events";
			case 431: return "waiting for workload manager to come online";
			case 432: return "waiting for workload manager to recover";
			case 433: return "CLSADMIN wait for remote nodes' reply for add svr";
			case 434: return "CLSADMIN wait for remote nodes' reply for drop svr";
			case 435: return "CLSADMIN wait for remote nodes' reply for set hb";
			case 437: return "RECEVENT wait for events";
			case 440: return "Task waiting for rename of config file";
			case 441: return "waiting for unique sequence number";
			case 442: return "waiting for cluster physical lock acquisition";
			case 443: return "waiting for indoubt cluster physical lock";
			case 444: return "waiting for cluster physical lock lookup";
			case 445: return "waiting for cluster physical lock release";
			case 446: return "waiting for cluster physical lock";
			case 447: return "waiting for cluster object lock acquisition";
			case 448: return "waiting for indoubt cluster object lock";
			case 449: return "waiting for cluster object lock lookup";
			case 450: return "waiting for cluster object lock release";
			case 451: return "waiting for cluster object lock";
			case 452: return "waiting for cluster logical lock acquisition";
			case 453: return "waiting for indoubt cluster logical lock";
			case 454: return "waiting for cluster logical lock lookup";
			case 455: return "waiting for cluster logical lock release";
			case 456: return "waiting for cluster logical lock downgrade";
			case 457: return "waiting for cluster logical lock range verify";
			case 458: return "waiting for no queue cluster logical lock";
			case 459: return "waiting for no wait cluster logical lock";
			case 460: return "waiting for cluster logical lock";
			case 461: return "waiting for remote blocker info from lock owner";
			case 462: return "waiting for inquiry reply from lock directory";
			case 463: return "waiting for inquiry reply from lock master";
			case 464: return "waiting for lock value info from lock master";
			case 465: return "waiting for CLM lockspace rebuild";
			case 466: return "wait for indoubt status reset for lock masters";
			case 467: return "unused";
			case 468: return "shutdown waiting for the quiesce clean up.";
			case 469: return "PCM callback handler wait for temporary threads.";
			case 470: return "dump waiting for node failover recovery";
			case 471: return "wait for phylock release during LRU buf grab";
			case 472: return "pause for forced GC process";
			case 473: return "wait for reply to retain indoubt (master) lock req";
			case 474: return "waiting for physical lock release before acquire";
			case 475: return "waiting for physical lock BAST done before requeue";
			case 476: return "Waiting while changing errorlog location.";
			case 477: return "sleeping max ldapua native threads exceeded.";
			case 478: return "waiting for native ldapua thread to finish.";
			case 479: return "waiting to read compressed block size in pll dbcc";
			case 480: return "waiting to read compressed block in parallel dbcc";
			case 481: return "waiting for a logical page lock";
			case 482: return "Waiting for ack of a synchronous message";
			case 483: return "Waiting for ack of a multicast synchronous message";
			case 484: return "Waiting for large message buffer to be available";
			case 485: return "Waiting for regular message buffer to be available";
			case 486: return "Wait for regular CIPC buffer to send a multicast ";
			case 487: return "Waiting for physical lock request submission";
			case 488: return "Waiting for demand physical lock requester";
			case 489: return "waiting for physical lock to be timed out";
			case 490: return "CMCC wait for demand lock on status ERR/TIMEDOUT";
			case 491: return "waiting for workload manager sync action to finish";
			case 492: return "waiting for local no queue cluster logical lock";
			case 493: return "waiting for a logical row lock ";
			case 494: return "waiting for a logical table lock ";
			case 495: return "waiting for an address lock ";
			case 496: return "waiting for cluster logical lock endpoint lookup";
			case 497: return "Waiting for alarm to be up to check for GC";
			case 498: return "Waiting for CIPC message to be available to continue GC";
			case 499: return "waiting for a logical page lock with remote blocker";
			case 500: return "waiting for a logical row lock with remote blocker";
			case 501: return "waiting for a logical table lock with remote blocker";
			case 502: return "waiting for object replication to complete";
			case 503: return "RECOVERY waits for dependent database";
			case 504: return "waiting for compressed block header read to complete";
			case 505: return "waiting for compressed block read to complete";
			case 506: return "waiting for a modifier of the AP to complete";
			case 507: return "waiting for master key password to decrypt syb_extpasswdkey";
			case 508: return "waiting for buffer read in bufwait";
			case 509: return "waiting for buffer read in cmcc_bufsearch";
			case 510: return "waiting for buffer validation in bufnewpage";
			case 511: return "waiting for buffer validation in cm_ptnclean";
			case 512: return "waiting for buffer validation in cmcc_bufsearch";
			case 513: return "waiting for buffer validation in cmcc_cidchange_notify";
			case 514: return "waiting for MASS to finish writing in bufnewpage";
			case 515: return "waiting for MASS to finish writing cmcc_buf_lockforread";
			case 516: return "waiting for MASS to finish writing in cmcc_bufpredirty";
			case 517: return "waiting for MASS to finish writing in bt__mark_offset";
			case 518: return "wait for MASS to finish changing in bufnewpage";
			case 519: return "wait for MASS to finish changing cmcc_buf_lockforread";
			case 520: return "wait for MASS to finish changing in cmcc_bufpredirty";
			case 521: return "wait for MASS to finish changing in bt__mark_offset";
			case 522: return "wait for phylock release LRU bufgrab cm_grabmem_clock";
			case 523: return "CMCC task waiting for disk read cmcc__getphysicallock";
			case 524: return "CMCC task waiting for disk read cmcc_bufgetphysicallock";
			case 525: return "CMCC task waiting for disk read in cmcc_check_forread";
			case 526: return "CMCC task waiting for disk read in cmcc_bufsearch";
			case 527: return "CMCC task wait for recovery in cmcc_bufgetphysicallock";
			case 528: return "CMCC task wait for BAST pending bit with no lock/latch";
			case 529: return "CMCC task wait for BAST pending bit with SH lock";
			case 530: return "CMCC task wait for BAST pending bit with SH latch";
			case 531: return "CMCC wait BAST pending no lock/latch wait_mass_usable";
			case 532: return "CMCC task wait BAST pending no lock in wait_mass_usable";
			case 533: return "CMCC task wait BAST pending SH lock in wait_mass_usable";
			case 534: return "CMCC wait BAST pending SH latch in wait_mass_usable";
			case 535: return "CMCC task wait for transfer in cmcc_check_forread";
			case 536: return "CMCC task wait physical lock release in cmcc_bufsearch";
			case 537: return "CMCC wait physical lock release in wait_mass_usable";
			case 538: return "CMCC wait physical lock acquire in wait_mass_usable";
			case 539: return "CMCC wait physical lock upgrade in wait_mass_usable";
			case 540: return "CMCC task wait retrying physical lock in cmcc_buflatch";
			case 541: return "CMCC task wait retrying physical lock in cmcc_bufsearch";
			case 542: return "BCM Thread wait for write queue to be initialised";
			case 543: return "BCM Thread wait for request, primary BCM thread";
			case 544: return "BCM Thread wait for primary queue to be initialised";
			case 545: return "BCM Thread wait write queue to initialise sec_bcmt_proc";
			case 546: return "BCM Thread wait for request, secondary BCM thread";
			case 547: return "BCM Thread wait pri queue to initialise wri_bcmt_proc";
			case 548: return "BCM Thread wait sec queue to initialise wri_bcmt_proc";
			case 549: return "BCM Thread wait for request, writer BCM thread";
			case 550: return "BCM Thread pause 1 tick for LOCK_VALUESTATUS_UNKNOWN";
			case 551: return "checkpoint worker waiting for wakeup from parent";
			case 552: return "wait for reply to retain indoubt (shadow) lock req";
			case 561: return "wait for mass read to end in getting last log page";
			case 562: return "waiting for a native thread in PCI execution";
			case 563: return "waiting for PCIS_FREEZE in comatoze";
			case 564: return "Wait to see if the JRTMSServer has been started";
			case 565: return "Wait for others to start/stop JRTMSServer (start)";
			case 566: return "Wait for others to start/stop JRTMSServer (stop)";
			case 567: return "Wait for socket to JRTMSServer is built";
			case 568: return "RAT Waiting for flush in cluster";
			case 569: return "RAT Waiting for rewrite in cluster";
			case 570: return "CSI module waiting to issue a SSL read";
			case 571: return "CSI module waiting for a SSL read completion";
			case 572: return "CSI module waiting to process SSL write queue";
			case 573: return "wait for MDA CE mutex to become free";
			case 574: return "wait for further updates to performance arrays again";
			case 575: return "sleeping while file is busy or locked during write";
			case 576: return "waiting for scheduler to stop target task";
			case 577: return "sleeping because max native threads exceeded";
			case 578: return "Wait for next alarm or a built-in request";
			case 579: return "JS testing: Freeze jstask for specified time";
			case 580: return "Wait until jstask wakes up and processes request.";
			case 581: return "pause to synchronise with offlining engine";
			case 582: return "pause to synchronise with offlining engine";
			case 583: return "alarmed task waiting for node failure";
			case 584: return "alarmed task waiting for node failover";
			case 585: return "alarmed task waiting for failback to complete";
			case 586: return "wait for the sockets to be freed";
			case 587: return "wait for clients of orphaned connection to close";
			case 588: return "waiting for CTLIB event to complete";
			case 589: return "waiting for ctlib call to complete with CS_CANCELED";
			case 590: return "waiting for a resume on the endpoint";
			case 591: return "LW engine waiting for ldap native thread to complete";
			case 592: return "OS process engine waiting for native thread to finish";
			case 593: return "waiting in sort manager under traceflag 1510";
			case 594: return "wait to simulate a timing issue";
			case 595: return "wait while testing RID list locking in le_ridjoinop.cpp";
			case 596: return "Wait until heartbeat or check interval expires";
			case 597: return "PCM wait for all replies";
			case 598: return "PCM waits for reply from the remote node";
			case 599: return "PCM wait for all fragments";
			case 600: return "OCM wait for lock/data/bast pending flag to be cleared";
			case 601: return "OCM wait for 0 waitcount value";
			case 602: return "OCM wait for BAST handling to be finished";
			case 603: return "OCM wait for clearing pushing data flag";
			case 604: return "(Discarding DFL) wait for DFL service thread to exit";
			case 605: return "(DFL synch end) wait for DFL service thread to exit";
			case 606: return "wait for dump thread to initiate abort";
			case 607: return "wait for abort request from dumping node or FO recovery";
			case 608: return "DFL service thread waiting for remote DFL message/event";
			case 609: return "Wait for DFL mgr thread to wake-up and clear init state";
			case 610: return "Wait for node join completion by DFL remote srvc thread";
			case 611: return "Wait for the DFL remote service thread exit";
			case 612: return "wait for reply from participant nodes for release msg";
			case 613: return "Abort case: wait for participant nodes to leave dump";
			case 614: return "dump pause for node failover recovery";
			case 615: return "dropdb:wait for system tasks to complete to kill tasks";
			case 616: return "single user: wait for system tasks in cdbt";
			case 617: return "objid: wait for preallocation of objid for local tempdb";
			case 618: return "dropdb:wait for system tasks to complete at shutdown";
			case 619: return "wait for checkpoint to complete in des__validate_keepcnt_and_markdrop()";
			case 620: return "CLM clear appropriate internal status for ceventp";
			case 621: return "waiting for cluster logical lock range verify";
			case 622: return "waiting for inquiry reply from lock master";
			case 623: return "wait for indoubt status reset for shadow locks";
			case 624: return "Wait for large CIPC buffer for high priority request";
			case 625: return "Wait for regular CIPC buffer for high priority request";
			case 626: return "Wait for large CIPC buffer to send a multicast ";
			case 627: return "Pause before reading device while getting last log page";
			case 628: return "wait for steps for DB reclaiming logical locks";
			case 631: return "wait for buffer read in bufsearch before logpg refresh";
			case 632: return "wait on MASS_DESTROY bit in cmcc_bufsearch";
			case 633: return "CMCC wait for demand lock on status GRANTED/OK";
			case 634: return "CMCC wait on demand lock on status OUTOFLOCK/DEADLOCK";
			case 635: return "CMCC wait for demand lock in cmcc_bufgetphysicallock";
			case 636: return "CMCC wait for demand lock in cmcc_wait_till_mass_usable";
			case 637: return "PCM wait for service queue elmt in pcm__invoke_recvcb";
			case 638: return "wait for indoubt status reset for directory locks";
			case 639: return "wait for indoubt status reset for all type master locks";
			case 640: return "pwait for indoubt status reset for all shadow locks";
			case 641: return "pause before recovering sybsystemdb for traceflag 3681";
			case 642: return "pause before recovering model DB for traceflag 3681";
			case 643: return "pause before recovering tempdb for traceflag 3681";
			case 644: return "pause before sybsystemprocs recovery for traceflag 3681";
			case 645: return "RAT Sender sleeping on empty queue";
			case 646: return "RAT Sender sleeping on scan retry";
			case 647: return "waiting for thread pool config list lock";
			case 648: return "waiting for RSA key pair pool refreshment";
			case 649: return "KPP handler sleeping on password policy option";
			case 650: return "waiting for network attn callback to complete";
			case 651: return "pause code in qa_suspend";
			case 652: return "pause code in dmpx_testknob";
			case 653: return "pause code in dbr__au_init";
			case 654: return "waiting for termination after stack overflow";
			case 655: return "waiting while login on hold";
			case 656: return "waiting for network socket to close";
			case 657: return "waiting for IO to complete while receiving data via network controller";
			case 658: return "waiting for IO to complete while sending data via network controller";
			case 659: return "wait for checkpoint in des_amc_endxact_finishup()";
			case 660: return "waiting for transaction logs replication drain";
			case 661: return "waiting for serial access for hash statistics buffer reservation";
			case 662: return "waiting for in-memory HADR info to get populated";
			case 663: return "Wait to see if the JVM started successfully";
			case 664: return "Wait for status ready to start JRTMSServer";
			case 665: return "Wait for status ready to stop JRTMSServer";
			case 666: return "Wait for JVM ServerSocket ready";
			case 667: return "waiting for JRTMSServer admin or intr response";
			case 668: return "wait for the read to finish on the log page";
			case 669: return "Wait to check for multiple sessions executing the same query plan";
			case 670: return "Wait to check for multiple sessions executing the same query plan, xoltp select query";
			case 671: return "Wait to check for multiple sessions executing the same query plan, xoltp insert query";
			case 672: return "Wait for alter-database to finish moving a GAM extent";
			case 673: return "des_putdlevel0cnt() wait by L0 for waitutil completion";
			case 674: return "des_putdlevel0cnt() wait by waitutil for waitutil completion";
			case 675: return "des_putdlevel0cnt() wait by waitutil for L0 completion in cluster";
			case 676: return "des_putdlevel0cnt() wait by waitutil for L0 completion";
			case 677: return "RAT Scanner wait for dirty log page to be flushed to disk";
			case 678: return "RAT-CI scanner sleep on delayed commit";
			case 679: return "RAT Wait for RS to process command before triggering log re-scan";
			case 680: return "RAT Wait for RS to process command before triggering log re-scan";
			case 681: return "RAT Coordinator generic sleep";
			case 682: return "RAT-CI STPMGR task sleep on bootstrap";
			case 683: return "RAT-CI STPMGR task sleep on shutdown";
			case 684: return "RAT-CI Scanner task sleep on bootstrap";
			case 685: return "RAT-CI Scanner task sleep on shutdown";
			case 686: return "RAT-CI Scanner sleep on stream open";
			case 687: return "RAT-CI Scanner sleep on allocating a CMD package";
			case 688: return "RAT-CI Scanner sleep on stream flush";
			case 689: return "RAT-CI Scanner sleep on log flush";
			case 690: return "RAT-CI Scanner sleep on log rewrite";
			case 691: return "RAT-CI STPMGR sleep on truncation point received";
			case 692: return "RAT MPR MS Wait for sender tasks to be spawned";
			case 693: return "RAT MPR MS Wait for scanner tasks to be spawned";
			case 694: return "RAT MPR MS Wait for sender tasks to be terminated";
			case 695: return "RAT MPR MS Wait for scanner tasks to be terminated";
			case 696: return "RAT Wait for ENDXACT log record in 2PC prepare state to be rewritten as COMMIT or ROLLBACK (SMP)";
			case 697: return "RAT Wait for ENDXACT log record in 2PC prepare state to be rewritten as COMMIT or ROLLBACK (SDC)";
			case 698: return "Fork is in progress,wait till it completes";
			case 699: return "waiting for master key password to decrypt syb_hacmpkey";
			case 700: return "waiting for a logical ptn lock with remote blocker";
			case 701: return "waiting for a logical ptn lock";
			case 702: return "waiting for database encryption tasks to suspend";
			case 703: return "waiting for monHADRMembers table to be populated";
			case 704: return "waiting for lock on PLCBLOCK";
			case 705: return "waiting for MASS to be pinned to a different PLCBLOCK";
			case 706: return "Wait while recreating a DES because another process is already recreating it";
			case 707: return "Wait when retrieving a DES because the DES is being recreated by another process";
			case 708: return "waiting for log space during ULC flush with multiple PLCBLOCKS";
			case 709: return "waiting for HADR deactivation to complete";
			case 710: return "Single instance database shutdown draining prepared transactions";
			case 711: return "Waiting for release of physical locks";
			case 712: return "User task waits on commit for sync rep acknowledgement";
			case 713: return "User task waits on commit for sync rep acknowledgement or timeout";
			case 714: return "RAT Coordinator sleeps on sync cleanup";
			case 715: return "RAT-CI Scanner sleeps on close stream";
			case 716: return "RAT-CI Scanner sleeps on shutdown";
			case 717: return "CSI module waiting for csi mutex";
			case 718: return "User task sleeps on schema pre-load to finish";
			case 719: return "RAT Coordinator sleeps on sync pre-load cleanup";
			case 720: return "DUMP DATABASE waits for service thread to finish";
			case 721: return "RAT CI Scanner sleep on log extension";
			case 722: return "Encrypted Columns module waiting for csi mutex";
			case 723: return "RAT-task sleeping on memory allocation";
			case 724: return "Database Encryption waiting for csi factory creation";
			case 725: return "waiting for buffer consolidation to be complete in do_bt_release_rlocks";
			case 726: return "Waiting on write IO for buffer eviction to NV Cache to finish";
			case 727: return "Encrypted Columns module waiting for csi factory creation";
			case 728: return "NV cache lazy cleaner sleeping to be woken up";
			case 729: return "NV cache lazy cleaner paused for sometime";
			case 730: return "NV cache lazy cleaner performing batch reads";
			case 731: return "NV cache lazy cleaner performing batch writes";
			case 732: return "Grabber waiting for NV cache buffer cleaning to finish";
			case 733: return "Searcher waiting for NV cache buffer cleaning to finish";
			case 734: return "Delay decrement of SNAP plan share count in Snap::SnapJITCompile";
			case 736: return "waiting for MASS to finish writing in buf_consolidate_lfb_post";
			case 737: return "waiting for buffer consolidation to be complete in bufsearch_cache_getlatch";
			case 738: return "waiting for buffer consolidation to be complete in getpage_with_validation";
			case 739: return "waiting for buffer consolidation to be complete in getcachedpage";
			case 740: return "waiting for buffer consolidation to be complete in lfb__scan_massoffset";
			case 741: return "waiting for MASS to finish writing in lfb__scan_massoffset";
			case 742: return "wait for MASS to finish changing in lfb__scan_massoffset";
			case 743: return "Writer task waiting for Dual write to HDD to finish";
			case 744: return "Wait for start_js request or next restart attempt.";
			case 745: return "des_putdssscancnt() wait for BCP completion";
			case 746: return "des_putdssscancnt() wait for Snapshot scan completion";
			case 747: return "Waiting on read/write IO for NVCache to finish";
			case 748: return "wait for checkpoint on trgdes in des_amc_endxact_finishup()";
			case 749: return "wait for MASS to finish changing before consolidation";
			case 750: return "Reader waiting for HADR redirection semaphore to be released by refresher";
			case 751: return "Refresher waiting for HADR redirection semaphore to be released by refresher";
			case 752: return "Refresher waiting for HADR redirection semaphore to be released by readers";
			case 753: return "wait for IMRS log segment free space to increase by a given amount";
			case 754: return "Waiting for cm_dbclean to raise error";
			case 755: return "Waiting for cm_ptnclean to raise error";
			case 756: return "Waiting for cm_dbclean to raise error";
			case 757: return "Waiting for cm_ptnclean to raise error";
			case 758: return "Waiting to delay writing log pages";
			case 759: return "Waiting for another task for re-filling";
			case 777: return "dumpdb:pause_before_send_dpm";
			case 778: return "delay writing log pages while trace 9154 is on";
			case 779: return "Pause this thread for the sample period";
			case 780: return "Bucket pool manager consolidator sleep";
			case 781: return "Waiting for update on the bucket pool to complete";
			case 782: return "udfenceoff_all: Wait for registration to preempt";
			case 783: return "udfenceoff_all: Wait for preempt to complete";
			case 784: return "udfence_release_all: Wait for release to complete";
			case 785: return "udfence_reserve: Wait for other server to unreserve";
			case 786: return "udfence_reserve: Wait for preempt to complete";
			case 787: return "udfence_reserve: Wait for reservation to be released";
			case 788: return "udfence_reserve: Wait for reservation to complete";
			case 789: return "Pause for delayed I/Os during re-issue";
			case 790: return "Wait for jvm process to be created";
			case 791: return "Wait for backupserver to start";
			case 792: return "Wait for other IOCP callback to complete";
			case 793: return "Wait for IOCP callback to complete for IO requests";
			case 794: return "Wait for callback to complete before socket deletion";
			case 795: return "Wait for network controller to be available before clearing IO requests";
			case 796: return "Wait for all user DBs to be opened";
			case 797: return "Wait for model database to be unlocked for usedb()";
			case 798: return "Wait for model database to be unlocked when DBT_LOCK() fails";
			case 799: return "Wait for HADR server to become inactive";
			case 800: return "HADR login stall time";
			case 801: return "KPP handler pause event";
			case 802: return "wait for one of the servers in cluster to crash";
			case 803: return "wait for fixed time after one of the servers in cluster has crashed";
			case 804: return "wait when there are user tasks remaining before revalidating keepcnt";
			case 805: return "polite shutdown: wait for system tasks to drain if timeout is not over";
			case 806: return "polite shutdown: wait for system tasks to drain if timeout is just elapsed";
			case 807: return "wait for dbtable transit chain instantiation";
			case 808: return "pause finite number of times, for other parallel tasks to release DES";
			case 809: return "While binding descriptor to cache, wait for predetermined amount of time for concurrent tasks to drain out";
			case 810: return "transfer table: pause when traceflag 2789 is set for a fixed time";
			case 811: return "pause until transfer passes alloc unit when modifying alloc page indicator bit in transfer table";
			case 812: return "transfer table: pause when traceflag 2789 is set, for a fixed time after each allocation unit is transferred";
			case 813: return "pause for other end of the pipe to open in transfer table";
			case 814: return "sleep before checking for rollback of active transactions";
			case 815: return "SDC: wait for cluster failover to complete in checkpoint ";
			case 816: return "create partial index metadata setup, waiting for exclusive lock on table";
			case 817: return "DBCC DBREPAIR on local user tempdb : Pause on traceflag 3793, holding sysdatabases row lock";
			case 818: return "RAT waiting for deferred startup; CI mode";
			case 819: return "User task waiting upon commiting for RAT scanner bootstrap; CI mode";
			case 820: return "RAT waiting for deferred startup; MRP MS mode";
			case 821: return "RAT recovery retry sleep";
			case 822: return "RAT waiting for deferred startup";
			case 823: return "pause code in qa_hold";
			case 824: return "Delay decrement of SNAP plan share count in Snap::SnapCSJITCompile";
			case 825: return "Wait for LLVMCS to finish SNAP codegen";
			case 826: return "Wait for LLVMCS to finish SNAP JIT compilation";
			case 827: return "Waiting on NV cache meta data write to finish before populating meta data page";
			case 828: return "Lazy cleaner waiting on an ongoing flush on NV cache meta data page";
			case 829: return "Dual write issue task waiting for I/O to HDD to finish";
			case 830: return "Legacy HA connection frozen in command";
			case 831: return "Encrypted Columns module waiting for csi factory creation when CSI_FACTORY_SET is enabled";
			case 832: return "Database Encryption waiting for csi factory creation when CSI_FACTORY_SET is enabled";
			case 833: return "NV cache write issue task waiting for an ongoing NV cache write I/O";
			case 834: return "NV cache read issue task waiting for an ongoing NV cache write I/O";
			case 856: return "waiting until imrslogsegment last chance threshold is cleared";
			case 857: return "BUFAWRITE pause to wait for encryption buffers are released";
			case 858: return "Pause before retrying to grab additional buffer";
			case 859: return "Dump command paused to retry locking NV cache and lazy cleaner info block";
			case 860: return "waiting while rsa key pair is being generated";
			case 861: return "RAT background task sleeping on wakeup event";
			case 862: return "While trying to drop a DES, if it is in use pause temporarily for user to drain out";
			case 863: return "Bucket pool manager waiting for autotune task completion";
			case 864: return "KPP handler pause event";
			case 865: return "KPP handler pause event";
			case 866: return "KPP handler pause event";
			case 867: return "CMCC task wait for retrying address lock in cmcc_getrlock()";
			case 869: return "waiting for PDES to finish flushing in ptn__datachange_reset";
			case 870: return "waiting for PDES to finish flushing in ptn__datachange_adj_cols";
			case 871: return "RAT-CI coordinator waiting for acknowledgement from task on mode switch";
			case 872: return "RAT-CI coordinator waiting for acknowledgement from task that it is stopping";
			case 873: return "Wait for HADR server to become inactive for less than specified timeout";
			case 874: return "pause for a second inside imrslog__respgq_add before going to search_respgq";
			case 875: return "wait for mass writing to be cleared in smp_bufpredirtynb";
			case 876: return "wait if buffer is changed in smp_bufpredirty";
			case 877: return "waiting to start snapshot scan in des_putdsscancnt";
			case 878: return "Waiting for Dump History File";
			case 879: return "Waiting for engines to come online for parallel recovery";
			case 880: return "waiting for PLCBLOCK to be flushed";
			case 881: return "RECOVERY analysis pass waits for IMRSLOG fix phase to complete";
			case 882: return "IMRSLOG RECOVERY reconciliation phase waits for Syslogs analysis pass to complete";
			case 883: return "RECOVERY undo pass waits for IMRSLOG RECOVERY reconciliation phase to complete";
			case 884: return "Syslogs recovery thread clean up waits for IMRSLOG RECOVERY reconciliation phase to complete";
			case 885: return "Sysimrslogs recovery thread clean up waits for RECOVERY analysis pass to complete";
			case 886: return "Syslogs recovery thread clean up waits for IMRSLOG RECOVERY thread to exit";
			case 887: return "Pause before re-attempting to grab buffer in cm_grabmem_lru";
			case 888: return "wait for the IMRS GC thread to get spawned completely.";
			case 889: return "wait for the LOB GC thread to get spawned completely.";
			case 890: return "wait for the IMRS PACK thread to get spawned completely.";
			case 891: return "waiting for buffer read to complete if sdes is set to skip share latch";
			case 892: return "wait for the IMRS background tasks (GC) number to stabilize.";
			case 893: return "pause periodically till IMRS GC threads exit.";
			case 894: return "pause periodically till IMRS pack threads exit.";
			case 895: return "pause periodically till IMRS LOB GC threads exit.";
			case 896: return "wait for the HCB GC tasks number to stabilize.";
			case 897: return "pause periodically till HCB GC threads exit.";
			case 898: return "pause point 1 before retry pinning the buffer.";
			case 899: return "pause point 2 before retry pinning the buffer";
			case 900: return "pause before check to see if plcblock is still being flushed";
			case 901: return "pause thread that hits fatal internal PLC error";
			case 902: return "waiting for PLCBLOCK that is queued or being flushed";
			case 903: return "waiting for database thmgr counts being recovered";
			case 904: return "RAT-CI syslogs scanner wait for sysimrslogs scanner catching up";
			case 905: return "RAT-CI sysimrslogs scanner wait for syslogs scanner catching up";
			case 906: return "RAT-CI Scanner task sleep on bootstrap";
			case 907: return "RAT-CI Scanner sleep on stream flush";
			case 908: return "RAT-CI Scanner sleep on log flush";
			case 909: return "RAT-CI Scanner sleep on log flush";
			case 910: return "RAT-CI Scanner sleeps on shutdown";
			case 911: return "waiting to issue I/O until all changers finish changing the buffer.";
			case 913: return "RAT-CI Scanner sleeps on schema flush";
			case 914: return "IMRS Garbage Collector: sleep while waiting for more rows to GC.";
			case 915: return "IMRS Pack: sleep while waiting for more rows to pack.";
			case 916: return "IMRS LOB Garbage Collector: sleep while waiting for more LOB rows to GC.";
			case 917: return "Sleep during IMRS background thread spawning: changing number of IMRS background tasks status from STABLE to CHANGING before actually changing number of tasks.";
			case 918: return "Sleep during IMRS background thread initialization: changing number of IMRS background tasks status from STABLE to CHANGING before actually changing number of tasks.";
			case 919: return "Sleep during IMRS background thread clean-up: changing number of IMRS background tasks status from STABLE to CHANGING before actually changing number of tasks.";
			case 920: return "wait for the IMRS background tasks (LOB version GC) number to stabilize.";
			case 921: return "wait for the IMRS background tasks (Pack) number to stabilize.";
			case 922: return "HCB Garbage Collector: sleep until woken up to start GCing HCB nodes.";
			case 923: return "Sleep during HCB GC thread spawning: changing number of HCB GC tasks status from STABLE to CHANGING before actually changing number of tasks.";
			case 924: return "Sleep during HCB GC thread initialization: changing number of HCB GC tasks status from STABLE to CHANGING before actually changing number of tasks.";
			case 925: return "Sleep during HCB GC thread clean-up: changing number of HCB GC tasks status from STABLE to CHANGING before actually changing number of tasks.";
			case 926: return "HCB Autotune: If autotune interval = 0, sleep forever until woken up.";
			case 927: return "HCB Autotune: If autotune interval > 0, sleep until interval is complete.";
			case 928: return "waiting for ct_poll() to complete checking of pending async operations";
			case 929: return "waiting for log preallocation by other SPID";
			case 930: return "RAT-CI IMRSLOG Scanner sleeps on attaching to shared stream";
			case 931: return "Syslogs recovery thread waits for IMRSLOG RECOVERY thread before it starts the analysis pass";
			case 932: return "Waiting for completion of blocking thread operation";
		}
		
		return null;
	}

	//--------------------------------------------------------------------------------
	// END: SQL Capture - get WAIT
	//--------------------------------------------------------------------------------
}
