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
package com.dbxtune.pcs.report.content.sqlserver;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.log4j.Logger;

import com.dbxtune.gui.ModelMissmatchException;
import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.pcs.report.content.ReportEntryAbstract;
import com.dbxtune.sql.SqlObjectName;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.DbUtils;
import com.dbxtune.utils.HtmlTableProducer;
import com.dbxtune.utils.MathUtils;
import com.dbxtune.utils.StringUtil;

public abstract class SqlServerAbstract
extends ReportEntryAbstract
{
	private static Logger _logger = Logger.getLogger(SqlServerAbstract.class);

	public SqlServerAbstract(DailySummaryReportAbstract reportingInstance)
	{
		super(reportingInstance);
	}

	public Set<String> getPlanHandleObjects(ResultSetTableModel rstm, String colName)
	{
		Set<String> set = new LinkedHashSet<>();
		
		int pos_colName = rstm.findColumn(colName);
		if (pos_colName != -1)
		{
			for (int r=0; r<rstm.getRowCount(); r++)
			{
				String name = rstm.getValueAsString(r, pos_colName);
				
				if (name != null && name.trim().startsWith("0x"))
				{
					set.add(name);
				}
			}
		}
		
		return set;
	}

	public ResultSetTableModel getShowplanFromMonDdlStorage(DbxConnection conn, Set<String> nameSet)
	throws SQLException
	{
		ResultSetTableModel planRstm = null;
		
		for (String name : nameSet)
		{
			String sql = ""
				    + "select [objectName], [extraInfoText] as [SQLText] \n"
				    + "from [MonDdlStorage] \n"
				    + "where 1 = 1 \n"
				    + "  and [dbname]     = 'statement_cache' \n"
//				    + "  and [owner]      = 'ssql' \n"
				    + "  and [objectName] = " + DbUtils.safeStr(name) + " \n"
				    + "";
			
			sql = conn.quotifySqlString(sql);
			try ( Statement stmnt = conn.createStatement() )
			{
				// Unlimited execution time
				stmnt.setQueryTimeout(0);
				try ( ResultSet rs = stmnt.executeQuery(sql) )
				{
//					ResultSetTableModel rstm = new ResultSetTableModel(rs, "planRstm");
					ResultSetTableModel rstm = createResultSetTableModel(rs, "planRstm", sql);
					
					if (planRstm == null)
						planRstm = rstm;
					else
						planRstm.add(rstm);

					if (_logger.isDebugEnabled())
						_logger.debug("_planRstm.getRowCount()="+ rstm.getRowCount());
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
//		if (planRstm != null)
//		{
//			int pos_SQLText = planRstm.findColumn("SQLText");
//			if (pos_SQLText >= 0)
//			{
//				for (int r=0; r<planRstm.getRowCount(); r++)
//				{
//					String SQLText = planRstm.getValueAsString(r, pos_SQLText);
//					if (StringUtil.hasValue(SQLText))
//					{
//						int startPos = SQLText.indexOf("<![CDATA[");
//						int endPos   = SQLText.indexOf("]]>");
//
//						if (startPos >= 0 && endPos >= 0)
//						{
//							startPos += "<![CDATA[".length();
//							endPos   -= "]]>"      .length();
//							
//							String newSQLText = SQLText.substring(startPos, endPos).trim();
//							
//							if (newSQLText.startsWith("SQL Text:"))
//								newSQLText = newSQLText.substring("SQL Text:".length()).trim();
//
//							// make it a bit more HTML like
//							newSQLText = "<xmp>" + newSQLText + "</xmp>";
//
//							// Finally SET the SQL Text
//							planRstm.setValueAtWithOverride(newSQLText, r, pos_SQLText);
//						}
//					}
//				}
//			}
//		}
		
		return planRstm;
	}

	public Map<String, String> getShowplanAsMapFromMonDdlStorage(DbxConnection conn, Set<String> nameSet)
	throws SQLException
	{
		Map<String, String> planMap = new LinkedHashMap<>();
		
		for (String name : nameSet)
		{
			String sql = ""
				    + "select [objectName], [extraInfoText] as [SQLText] \n"
				    + "from [MonDdlStorage] \n"
				    + "where 1 = 1 \n"
				    + "  and [dbname]     = 'statement_cache' \n"
//				    + "  and [owner]      = 'ssql' \n"
				    + "  and [objectName] = " + DbUtils.safeStr(name) + " \n"
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
						String objectName    = rs.getString(1);
						String extraInfoText = rs.getString(2);
						
						planMap.put(objectName, extraInfoText);
					}
				}
			}
			catch(SQLException ex)
			{
				//_problem = ex;

				_logger.warn("Problems getting SQL Statement name = '"+name+"': " + ex);
				throw ex;
			} 
		}
		
		return planMap;
	}






//	/**
//	 * Parse the passed SQL Text and extract table names<br>
//	 * Those table names we then get various information about like
//	 * <ul>
//	 *    <li>Table estimated RowCount, Size in various formats, Index Size, etc</li>
//	 *    <li>Each index will have a separate entry, also showing 'index_names', 'keys', sizes etc...</li>
//	 *    <li>Formatted SQL Text in a separate <i>hover-able</i> tool-tip or dialog pop-up</li>
//	 * </ul>
//	 * 
//	 * @param conn             Connection to the PCS -- Persistent Counter Storage
//	 * @param sqlText          The SQL Text we will parse for table names (which table names to lookup)
//	 * @param dbmsVendor       The DBMS Vendor the above SQL Text was executed in (SQL Dialect). If null or "" (a <i>standard</i> SQL type will be used)
//	 * @return                 HTML Text (table) with detailed information about the Table and Indexes used by the SQL Text
//	 */
//	public String getTableInformationFromSqlText(DbxConnection conn, String sqlText, String dbmsVendor)
//	{
//		// Possibly get from configuration
//		boolean parseSqlText = true;
//		if ( ! parseSqlText )
//			return "";
//		
//		String tableInfo = "";
//
//		// Parse the SQL Text to get all tables that are used in the Statement
//		String problemDesc = "";
//		Set<String> tableList = SqlParserUtils.getTables(sqlText);
////		List<String> tableList = Collections.emptyList();
////		try { tableList = SqlParserUtils.getTables(sqlText, true); }
////		catch (ParseException pex) { problemDesc = pex + ""; }
//
//		// Get information about ALL tables in list 'tableList' from the DDL Storage
//		Set<SqlServerTableInfo> tableInfoSet = getTableInformationFromMonDdlStorage(conn, tableList);
//		if (tableInfoSet.isEmpty() && StringUtil.isNullOrBlank(problemDesc))
//			problemDesc = "&emsp; &bull; No tables was found in the DDL Storage for tables: " + listToHtmlCode(tableList);
//
//		// And make it into a HTML table with various information about the table and indexes 
//		tableInfo = problemDesc + getTableInfoAsHtmlTable(tableInfoSet, tableList, true, "dsr-sub-table-tableinfo");
//
//		// Finally make up a message that will be appended to the SQL Text
//		if (StringUtil.hasValue(tableInfo))
//		{
//			// Surround with collapse div
//			tableInfo = ""
//					//+ "<!--[if !mso]><!--> \n" // BEGIN: IGNORE THIS SECTION FOR OUTLOOK
//
//					+ "\n<br>\n"
//					+ getFormattedSqlAsTooltipDiv(sqlText, dbmsVendor) + "\n"
////					+ "<br>\n"
//					+ "<details open> \n"
//					+ "<summary>Show/Hide Table information for " + tableList.size() + " table(s): " + listToHtmlCode(tableList) + "</summary> \n"
//					+ tableInfo
//					+ "</details> \n"
//
//					//+ "<!--<![endif]-->    \n" // END: IGNORE THIS SECTION FOR OUTLOOK
//					+ "";
//		}
//		
//		return tableInfo;
//	}

	/**
	 * Get various information from DDL Storage about table and it's indexes
	 * 
	 * @param conn
	 * @param tableList
	 * @param includeIndexInfo
	 * @param classname
	 * @return
	 */
	@Override
	public String getDbmsTableInfoAsHtmlTable(DbxConnection conn, String currentDbname, Set<String> tableList, boolean includeIndexInfo, String classname)
	{
		// Get tables/indexes
		Set<SqlServerTableInfo> tableInfoSet = getTableInformationFromMonDdlStorage(conn, tableList);
		if (tableInfoSet.isEmpty())
			return "";

		// And make it into a HTML table with various information about the table and indexes 
		return getTableInfoAsHtmlTable(currentDbname, tableInfoSet, tableList, includeIndexInfo, classname);
	}

	/**
	 * 
	 * @param currentDbname
	 * @param tableInfoList
	 * @param tableList
	 * @param includeIndexInfo
	 * @param classname
	 * @return
	 */
	public String getTableInfoAsHtmlTable(String currentDbname, Set<SqlServerTableInfo> tableInfoSet, Set<String> tableList, boolean includeIndexInfo, String classname)
	{
		// Exit early: if no data
		if (tableInfoSet == null)   return "";
		if (tableInfoSet.isEmpty()) return "";
		
		StringBuilder sb = new StringBuilder();

		// Figure out if we are missing any rows in 'tableInfoList' that existed in 'tableList'
		if (tableList != null && tableList.size() > tableInfoSet.size())
		{
			for(String tabName : tableList)
			{
				// Remove any "schema" names from the tableName
//				SqlObjectName sqlObj = new SqlObjectName(tabName, DbUtils.DB_PROD_NAME_SYBASE_ASE, "\"", false, true);
//				tabName = sqlObj.getObjectNameOrigin();

				boolean exists = false;
				for(SqlServerTableInfo ti : tableInfoSet)
				{
					if (tabName.equalsIgnoreCase(ti.getTableName()))
					{
						exists = true;
						break;
					}
				}
				if ( ! exists )
				{
					sb.append("&emsp; &bull; Table <code>").append(tabName).append("</code> was NOT found in the DDL Storage.<br>\n");
				}
			}
		}
		
		String className = "";
		if (StringUtil.hasValue(classname))
			className = " class='" + classname + "'";
		
		sb.append("<table" + className + "> \n");

		//--------------------------------------------------------------------------
		// Table Header
		sb.append("<thead> \n");
		sb.append("<tr> \n");

		sb.append("  <th>Table Info</th> \n");
		if (includeIndexInfo)
			sb.append("  <th>Index Info</th> \n");
		
		sb.append("</tr> \n");
		sb.append("</thead> \n");

		NumberFormat nf = NumberFormat.getInstance();

		boolean onlyShowObjectInCurrentDatabase = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_TOP_STATEMENTS_ONLY_LIST_OBJECTS_IN_CURRENT_DATABASE, DEFAULT_TOP_STATEMENTS_ONLY_LIST_OBJECTS_IN_CURRENT_DATABASE);
		int     numOfObjectNotInCurrentDatabase = 0;

		//--------------------------------------------------------------------------
		// Table BODY
		sb.append("<tbody> \n");
		for (SqlServerTableInfo entry : tableInfoSet)
		{
			// "list-objects-only-in-current-database"
			if (StringUtil.hasValue(currentDbname) && onlyShowObjectInCurrentDatabase)
			{
				if (StringUtil.hasValue(entry.getDbName()) && ! currentDbname.equalsIgnoreCase(entry.getDbName()))
				{
					numOfObjectNotInCurrentDatabase++;
					continue;
				}
			}

			LinkedHashMap<String, String> tableInfoMap = new LinkedHashMap<>();

			if (entry.isView())
			{
				tableInfoMap.put("Type"       ,                 "<b>VIEW</b>"              );
				tableInfoMap.put("DBName"     , markIfDifferent(entry.getDbName(), currentDbname));
				tableInfoMap.put("Schema"     ,                 entry.getSchemaName()      );
				tableInfoMap.put("View"       ,                 entry.getTableName()       );
				tableInfoMap.put("Created"    ,                 entry.getCrDate()+""       );
//				tableInfoMap.put("References" ,                 entry.getViewReferences()+""); // instead show this in the: Index Info section
				tableInfoMap.put("DDL"        ,                 getFormattedSqlAsTooltipDiv(entry._objectText, "View DDL", DbUtils.DB_PROD_NAME_MSSQL));
			}
			else // Any table
			{
				tableInfoMap.put("DBName"     , markIfDifferent(entry.getDbName(), currentDbname));
				tableInfoMap.put("Schema"     ,                 entry.getSchemaName()      );
				tableInfoMap.put("Table"      ,                 entry.getTableName()       );
				tableInfoMap.put("Created"    ,                 entry.getCrDate()+""       );
				tableInfoMap.put("Rowcount"   , nf.format(      entry.getRowTotal()       ));
				tableInfoMap.put("Partitions" , nf.format(      entry.getPartitionCount() ));
				tableInfoMap.put("Total MB"   , nf.format(      entry.getTotalMb()        ));
				tableInfoMap.put("InRow MB"   , nf.format(      entry.getInRowMb()        ));
				tableInfoMap.put("Lob MB"     , nf.format(      entry.getLobMb()          ));
				tableInfoMap.put("Overflow MB", nf.format(      entry.getRowOverflowMb()  ));
				tableInfoMap.put("Index MB"   , nf.format(      entry.getIndexMb()        ));
				tableInfoMap.put("Created"    ,                 entry.getCrDate()+""       );
				tableInfoMap.put("Sampled"    ,                 entry.getSampleTime()+""   );
				tableInfoMap.put("Index Count", entry.getIndexCount() + (entry.getIndexCount() > 0 ? "" : " <b><font color='red'>&lt;&lt;-- Warning NO index</font></b>") );
				tableInfoMap.put("DDL Info"   , entry._objectText   == null ? "-"             : getTextAsTooltipDiv(entry._objectText, "Table Info"));
//				tableInfoMap.put("Triggers"   , "-not-yet-impl-");
				tableInfoMap.put("Triggers"   , entry._triggersText == null ? "-no-triggers-" : getTextAsTooltipDiv(entry._triggersText, "Trigger Info"));
			}

			String tableInfo = HtmlTableProducer.createHtmlTable(tableInfoMap, "dsr-sub-table-other-info", true);
			String indexInfo = "";
			String missingIndexInfo = "";

			if (entry.isView()) 
			{
				// Build a bullet list of view references
				indexInfo = "";
				if (entry.getViewReferences() != null)
				{
					indexInfo += "<ul>";
					for (String viewRef : entry.getViewReferences())
					{
//						// Remove objects that are NOT part of "Current Database"
//						if (StringUtil.hasValue(currentDbname) && onlyShowObjectInCurrentDatabase)
//						{
//							if (StringUtil.hasValue(viewRef) && ! viewRef.contains(currentDbname))
//							{
//								numOfObjectNotInCurrentDatabase++;
//								continue;
//							}
//						}

						indexInfo += "<li>" + viewRef + "</li>";
					}
					indexInfo += "</ul>";
				}
			}
			else
			{
				// Get index information in a separate table
				indexInfo = getIndexInfoAsHtmlTable(entry, classname);

				// Get missing index information
				missingIndexInfo = getMissingIndexInfoAsHtmlTable(entry, classname);
			}
			
			
			sb.append("<tr> \n");
			sb.append("  <td>").append( tableInfo ).append("</td> \n");

			if (includeIndexInfo)
				sb.append("  <td>").append( indexInfo ).append( missingIndexInfo ).append("</td> \n");

			sb.append("</tr> \n");
		}
		sb.append("</tbody> \n");
		
		//--------------------------------------------------------------------------
		// Table Footer
//		sb.append("<tfoot> \n");
//		sb.append("<tr> \n");
//
//		sb.append("  <td></td> \n"); // first column is empty, which is the "row name"
//
//		sb.append("</tr> \n");
//		sb.append("</tfoot> \n");

		sb.append("</table> \n");
		
		// If we have SKIPPED dependent object (NOT in current database), then write some info about that!
		if (onlyShowObjectInCurrentDatabase && numOfObjectNotInCurrentDatabase > 0)
		{
			sb.append("<p> \n");
			sb.append("<b>NOTE:</b> " + numOfObjectNotInCurrentDatabase + " objects was found in the PCS, which will <b>NOT</b> be displayed, due to not matching currentDbname='" + currentDbname + "'.<br> \n");
			sb.append("This can be changed with config property '" + PROPKEY_TOP_STATEMENTS_ONLY_LIST_OBJECTS_IN_CURRENT_DATABASE + "=false' \n");
			sb.append("</p> \n");
		}
		
		return sb.toString();
	}

	public String getIndexInfoAsHtmlTable(SqlServerTableInfo tableEntry, String classname)
	{
		// Exit early: if no data
		if (tableEntry == null)   return "";

		// Get the index list
		List<SqlServerIndexInfo> indexInfoList = tableEntry.getIndexList();

		// Exit early: if no data
		if (indexInfoList == null)   return "";
		if (indexInfoList.isEmpty()) return "";
		
		StringBuilder sb = new StringBuilder();

		String className = "";
		if (StringUtil.hasValue(classname))
			className = " class='" + classname + "'";
		
		sb.append("<table" + className + "> \n");

		//--------------------------------------------------------------------------
		// Table Header
		sb.append("<thead> \n");
		sb.append("<tr> \n");

		sb.append(createHtmlTh("Index Name"               , ""));
		sb.append(createHtmlTh("IndexID"                  , ""));
		sb.append(createHtmlTh("Keys"                     , ""));
		sb.append(createHtmlTh("Include"                  , "Columns included on the leafe index page... create index... on(c1,c2) INCLUDE(c3,c4,c5)"));
		sb.append(createHtmlTh("Filter"                   , "Filter Expression used to create the index... create index... on(c1,c2) WHERE colName='someValue'"));
		sb.append(createHtmlTh("Description"              , ""));
		sb.append(createHtmlTh("Size MB"                  , ""));
		sb.append(createHtmlTh("Pages"                    , ""));
		sb.append(createHtmlTh("FillFactor"               , ""));
		sb.append(createHtmlTh("Avg RowsPerPage"          , "Calculated Average Rows per Page \nCalculted by: pages/rowcount"));
		sb.append(createHtmlTh("Last Update Stats"        , ""));
//		sb.append(createHtmlTh("Avg RowSize"              , "Average Bytes used per Row\nCalculted by: SizeBytes/TableRowcount"));
//		sb.append(createHtmlTh("Reserved MB"              , ""));
//		sb.append(createHtmlTh("Unused MB"                , ""));

		sb.append(createHtmlTh("READS"                             , "(diff <- abs) Number of READS by queries\nNote: This is just: user_seeks + user_scans + user_lookups'"));
		sb.append(createHtmlTh("WRITES"                            , "(diff <- abs) Number of WRITES by statemenst.\nNote: This is just: user_updates as another-name"));
		sb.append(createHtmlTh("user_seeks"                        , "(diff <- abs) Number of seeks by user queries."));
		sb.append(createHtmlTh("user_scans"                        , "(diff <- abs) Number of scans by user queries that did not use -seek- predicate."));
		sb.append(createHtmlTh("user_lookups"                      , "(diff <- abs) Number of bookmark lookups by user queries."));
		sb.append(createHtmlTh("user_updates"                      , "(diff <- abs) Number of updates by user queries. This includes Insert, Delete, and Updates representing number of operations done not the actual rows affected. For example, if you delete 1000 rows in one statement, this count increments by 1'"));
                                                                   
		sb.append(createHtmlTh("range_scan_count"                  , "Execution: (diff <- abs) range and table scans started on the index or heap"));
		sb.append(createHtmlTh("singleton_lookup_count"            , "Execution: (diff <- abs) single row retrievals from the index or heap"));
		sb.append(createHtmlTh("forwarded_fetch_count"             , "Execution: (diff <- abs) Count of rows that were fetched through a forwarding record"));
		sb.append(createHtmlTh("lob_fetch_in_pages"                , "Execution: (diff <- abs) Count of large object(LOB) pages retrieved from the LOB_DATA allocation unit"));
		sb.append(createHtmlTh("leaf_crud_count"                   , "Execution: (diff <- abs) leaf-level insert + deletes + ghosted-deletes + updates"));
		sb.append(createHtmlTh("leaf_insert_count"                 , "Execution: (diff <- abs) leaf-level inserts"));
		sb.append(createHtmlTh("leaf_delete_count"                 , "Execution: (diff <- abs) leaf-level deletes"));
		sb.append(createHtmlTh("leaf_update_count"                 , "Execution: (diff <- abs) leaf-level updates"));
		sb.append(createHtmlTh("leaf_ghost_count"                  , "Execution: (diff <- abs) leaf-level rows that are marked as deleted, but not yet removed"));
        sb.append(createHtmlTh("page_latch_wait_count"             , "Execution: (diff <- abs) number of times the Database Engine waited, because of latch contention."));
        sb.append(createHtmlTh("page_latch_wait_in_ms"             , "Execution: (diff <- abs) number of milliseconds the Database Engine waited, because of latch contention."));
		sb.append(createHtmlTh("page_io_latch_wait_count"          , "Execution: (diff <- abs) times the Database Engine waited on an I/O page latch."));
		sb.append(createHtmlTh("page_io_latch_wait_in_ms"          , "Execution: (diff <- abs) number of milliseconds the Database Engine waited on a page I/O latch"));
        sb.append(createHtmlTh("row_lock_count"                    , "Execution: (diff <- abs) number of row locks requested."));
        sb.append(createHtmlTh("row_lock_wait_count"               , "Execution: (diff <- abs) number of times the Database Engine waited on a row lock."));
        sb.append(createHtmlTh("row_lock_wait_in_ms"               , "Execution: (diff <- abs) Total number of milliseconds the Database Engine waited on a row lock."));
        sb.append(createHtmlTh("page_lock_count"                   , "Execution: (diff <- abs) number of page locks requested."));
        sb.append(createHtmlTh("page_lock_wait_count"              , "Execution: (diff <- abs) number of times the Database Engine waited on a page lock."));
        sb.append(createHtmlTh("page_lock_wait_in_ms"              , "Execution: (diff <- abs) Total number of milliseconds the Database Engine waited on a page lock."));
        sb.append(createHtmlTh("index_lock_promotion_attempt_count", "Execution: (diff <- abs) number of times the Database Engine tried to escalate locks."));
        sb.append(createHtmlTh("index_lock_promotion_count"        , "Execution: (diff <- abs) number of times the Database Engine escalated locks."));
		
		sb.append(createHtmlTh("DDL"                               , ""));

        sb.append("</tr> \n");
		sb.append("</thead> \n");

		NumberFormat nf = NumberFormat.getInstance();

		//--------------------------------------------------------------------------
		// Table BODY
		sb.append("<tbody> \n");
		for (SqlServerIndexInfo entry : indexInfoList)
		{
//			long avgBytesPerRow = (entry.getSizeKb() * 1024L) / tableEntry.getRowTotal();

			String description = entry.getDescription();
			if (StringUtil.hasValue(description))
			{
				if (description.startsWith("clustered, "))
					description = description.replace("clustered, ", "<b>clustered</b>, ");

				description = description.replace("unique, ", "<b>unique</b>, ");
			}
			
			sb.append("<tr> \n");
			sb.append("  <td>").append(            entry.getIndexName()            ).append("</td> \n");
			sb.append("  <td>").append(            entry.getIndexID()              ).append("</td> \n");
			sb.append("  <td>").append(            entry.getKeysStr()              ).append("</td> \n");
			sb.append("  <td>").append(            entry.getIncludeStr()           ).append("</td> \n");
			sb.append("  <td>").append(            entry.getFilterExpression()     ).append("</td> \n");
			sb.append("  <td>").append(            description                     ).append("</td> \n");
			sb.append("  <td>").append( nf.format( entry.getIndexSizeMb()         )).append("</td> \n");
			sb.append("  <td>").append( nf.format( entry.getIndexPages()          )).append("</td> \n");
			sb.append("  <td>").append( nf.format( entry.getFillFactor()          )).append("</td> \n");
			sb.append("  <td>").append( nf.format( entry.getRowsPerPage()         )).append("</td> \n");
			sb.append("  <td>").append(            entry.getLastUpdateStats()      ).append("</td> \n");
//			sb.append("  <td>").append( nf.format( entry.getAvgRowSizeBytes()     )).append("</td> \n");
//			sb.append("  <td>").append( nf.format( entry.getReservedMb()          )).append("</td> \n");
//			sb.append("  <td>").append( nf.format( entry.getUnusedMb()            )).append("</td> \n");

			sb.append("  <td>").append( diffAbsValues(entry._READS                             , entry._abs_READS                              )).append("</td> \n");
			sb.append("  <td>").append( diffAbsValues(entry._WRITES                            , entry._abs_WRITES                             )).append("</td> \n");
			sb.append("  <td>").append( diffAbsValues(entry._user_seeks                        , entry._abs_user_seeks                         )).append("</td> \n");
			sb.append("  <td>").append( diffAbsValues(entry._user_scans                        , entry._abs_user_scans                         )).append("</td> \n");
			sb.append("  <td>").append( diffAbsValues(entry._user_lookups                      , entry._abs_user_lookups                       )).append("</td> \n");
			sb.append("  <td>").append( diffAbsValues(entry._user_updates                      , entry._abs_user_updates                       )).append("</td> \n");
                                                                                                                                               
			sb.append("  <td>").append( diffAbsValues(entry._range_scan_count                  , entry._abs_range_scan_count                   )).append("</td> \n");
			sb.append("  <td>").append( diffAbsValues(entry._singleton_lookup_count            , entry._abs_singleton_lookup_count             )).append("</td> \n");
			sb.append("  <td>").append( diffAbsValues(entry._forwarded_fetch_count             , entry._abs_forwarded_fetch_count              )).append("</td> \n");
			sb.append("  <td>").append( diffAbsValues(entry._lob_fetch_in_pages                , entry._abs_lob_fetch_in_pages                 )).append("</td> \n");
			sb.append("  <td>").append( diffAbsValues(entry._leaf_crud_count                   , entry._abs_leaf_crud_count                    )).append("</td> \n");
			sb.append("  <td>").append( diffAbsValues(entry._leaf_insert_count                 , entry._abs_leaf_insert_count                  )).append("</td> \n");
			sb.append("  <td>").append( diffAbsValues(entry._leaf_delete_count                 , entry._abs_leaf_delete_count                  )).append("</td> \n");
			sb.append("  <td>").append( diffAbsValues(entry._leaf_update_count                 , entry._abs_leaf_update_count                  )).append("</td> \n");
			sb.append("  <td>").append( diffAbsValues(entry._leaf_ghost_count                  , entry._abs_leaf_ghost_count                   )).append("</td> \n");
			sb.append("  <td>").append( diffAbsValues(entry._page_latch_wait_count             , entry._abs_page_latch_wait_count              )).append("</td> \n");
			sb.append("  <td>").append( diffAbsValues(entry._page_latch_wait_in_ms             , entry._abs_page_latch_wait_in_ms              )).append("</td> \n");
			sb.append("  <td>").append( diffAbsValues(entry._page_io_latch_wait_count          , entry._abs_page_io_latch_wait_count           )).append("</td> \n");
			sb.append("  <td>").append( diffAbsValues(entry._page_io_latch_wait_in_ms          , entry._abs_page_io_latch_wait_in_ms           )).append("</td> \n");
			sb.append("  <td>").append( diffAbsValues(entry._row_lock_count                    , entry._abs_row_lock_count                     )).append("</td> \n");
			sb.append("  <td>").append( diffAbsValues(entry._row_lock_wait_count               , entry._abs_row_lock_wait_count                )).append("</td> \n");
			sb.append("  <td>").append( diffAbsValues(entry._row_lock_wait_in_ms               , entry._abs_row_lock_wait_in_ms                )).append("</td> \n");
			sb.append("  <td>").append( diffAbsValues(entry._page_lock_count                   , entry._abs_page_lock_count                    )).append("</td> \n");
			sb.append("  <td>").append( diffAbsValues(entry._page_lock_wait_count              , entry._abs_page_lock_wait_count               )).append("</td> \n");
			sb.append("  <td>").append( diffAbsValues(entry._page_lock_wait_in_ms              , entry._abs_page_lock_wait_in_ms               )).append("</td> \n");
			sb.append("  <td>").append( diffAbsValues(entry._index_lock_promotion_attempt_count, entry._abs_index_lock_promotion_attempt_count )).append("</td> \n");
			sb.append("  <td>").append( diffAbsValues(entry._index_lock_promotion_count        , entry._abs_index_lock_promotion_count         )).append("</td> \n");
			
			sb.append("  <td>").append( entry.getDdlText() ).append("</td> \n");

			sb.append("</tr> \n");
		}
		sb.append("</tbody> \n");
		
		//--------------------------------------------------------------------------
		// Table Footer
//		sb.append("<tfoot> \n");
//		sb.append("<tr> \n");
//
//		sb.append("  <td></td> \n"); // first column is empty, which is the "row name"
//
//		sb.append("</tr> \n");
//		sb.append("</tfoot> \n");

		sb.append("</table> \n");
		
		return sb.toString();
	}

	public String getMissingIndexInfoAsHtmlTable(SqlServerTableInfo tableEntry, String classname)
	{
		// Exit early: if no data
		if (tableEntry == null)                   return "";
		if (tableEntry._missingIndexInfo == null) return "";
			
		StringBuilder sb = new StringBuilder();

		sb.append("<!--[if !mso]><!--> \n"); // BEGIN: IGNORE THIS SECTION FOR OUTLOOK

		sb.append("<br> \n");
//		sb.append("<details open> \n");
		sb.append("<details> \n");
		sb.append("<summary>Show/Hide: " + tableEntry._missingIndexInfo.getRowCount() + " Missing Index Recomendations from SQL Server: sys.dm_db_missing_index_*</summary> \n");

		if ( tableEntry._missingIndexInfo.isEmpty() )
			sb.append("&emsp; &bull; <b>No</b> Missing Index Recomendations was found.");
		else
			sb.append(toHtmlTable(tableEntry._missingIndexInfo));

		sb.append("</details> \n");

		sb.append("<!--<![endif]-->    \n"); // END: IGNORE THIS SECTION FOR OUTLOOK

		return sb.toString();
	}
	
	/**
	 * FIXME
	 */
	public Set<SqlServerTableInfo> getTableInformationFromMonDdlStorage(DbxConnection conn, Set<String> tableList)
	{
		// Exit early if nothing todo
		if (tableList == null)   return Collections.emptySet();
		if (tableList.isEmpty()) return Collections.emptySet();

		// Put all the results back in one list
		Set<SqlServerTableInfo> combinedReturnSet = new LinkedHashSet<>();
		
		for (String tableName : tableList)
		{
			try
			{
				Set<SqlServerTableInfo> tmp = getTableInformationFromMonDdlStorage(conn, null, null, tableName);
				
				if (tmp != null && !tmp.isEmpty())
					combinedReturnSet.addAll(tmp);
			}
			catch(SQLException ignore) 
			{
				// this is already logged in getTableInformationFromMonDdlStorage(...)
				//_logger.error("Problems reading from DDL Storage.");
			}
		}
		return combinedReturnSet;
	}
	
	/**
	 * Get a list of SqlServerTableInfo (basically sp_spaceused) <br>
	 * This will get information from the following places
	 * <ul>
	 *   <li>CmIndexPhysical / sys.dm_db_index_physical_stats -- Last entry if it can be found!</li>
	 *   <li></li>
	 *   <li></li>
	 *   <li>MonDdlStorage - where sp_help / sys.dm_db_index_physical_stats is stored</li>
	 * </ul>
	 * 
	 * @param conn       The connection to the PCS
	 * @param dbname     Name of the database the object is in. (can be "" or null; or contains '%' SQL wild-card)
	 * @param owner      Name of the schema   the object is in. (can be "" or null; or contains '%' SQL wild-card)
	 * @param table      Name of the table                      (can be "" or null; or contains '%' SQL wild-card), can also contain DBName.schema which will override the parameters dbname and owner
	 * 
	 * @return A list of found entries, each as an entry of SqlServerTableInfo.
	 * 
	 * @throws SQLException In case of issues.
	 */
	public Set<SqlServerTableInfo> getTableInformationFromMonDdlStorage(DbxConnection conn, String dbname, String owner, String table)
	throws SQLException
	{
		Set<SqlServerTableInfo> result = new LinkedHashSet<>();

		// First level of (potential recursion)
		getTableInformationFromMonDdlStorage(conn, dbname, owner, table, 0, result);
		
		// Should we remove any VIEWS ?
		boolean removeViews = false;
		if (removeViews)
		{
			Set<SqlServerTableInfo> tmp = new LinkedHashSet<>();
			for (SqlServerTableInfo ti : result)
			{
				if (ti.isView())
					continue;

				tmp.add(ti);
			}
			tmp = result;
		}
		
		return result;
	}
	/**
	 * PRIVATE: called from the public getTableInformationFromMonDdlStorage. <br>
	 * Intention: If it's a view that we lookup, we will do recurse calls until we have found "all" the involved tables (and views)
	 * 
	 * @param conn
	 * @param dbname
	 * @param owner
	 * @param table
	 * @param recursCallCount      Fist call should be 0, then it increments (and we stop at 100 recursive calls)
	 * @param result               Pass in a (LinkedHash) Set where the information will be stored into. (a outer method will be used to return data to "client" caller) and if desired remove any VIEW objects
	 * @throws SQLException
	 */
	private void getTableInformationFromMonDdlStorage(DbxConnection conn, String dbname, String owner, String table, int recursCallCount, Set<SqlServerTableInfo> result)
	throws SQLException
	{
//		if (StringUtil.isNullOrBlank(dbname))
//			return null;
//		if (StringUtil.isNullOrBlank(table))
//			return null;
		
		if (result == null)
			throw new IllegalArgumentException("The passed Set<SqlServerTableInfo> 'result' can't be null.");

		// Stop recursive calls after X times (so we don't end up in an infinite loop)
		if (recursCallCount > 100)
		{
			_logger.warn("getTableInformationFromMonDdlStorage() infinite recursive call count is at " + recursCallCount + ". STOP doing recursive calls now... ");
			return;
		}
		
		// Check if 'table' has dbname/schema name specified.
		SqlObjectName sqlObj = new SqlObjectName(table, DbUtils.DB_PROD_NAME_MSSQL, "\"", false, false, true);
//		SqlObjectName sqlObj = new SqlObjectName(conn, table);
		if (sqlObj.hasCatalogName()) dbname = sqlObj.getCatalogName();
		if (sqlObj.hasSchemaName() ) owner  = sqlObj.getSchemaName();
		if (sqlObj.hasObjectName() ) table  = sqlObj.getObjectName();
		
		if ("dbo".equalsIgnoreCase(owner))
			owner = "";

//		String and_dbname = !StringUtil.hasValue(dbname) ? "" : "  and [dbname]     = " + DbUtils.safeStr(dbname) + " \n";
//		String and_owner  = !StringUtil.hasValue(owner)  ? "" : "  and [owner]      = " + DbUtils.safeStr(owner)  + " \n";
//		String and_table  = !StringUtil.hasValue(table)  ? "" : "  and [objectName] = " + DbUtils.safeStr(table)  + " \n";
		String and_dbname = !StringUtil.hasValue(dbname) ? "" : "  and lower([dbname])     = " + DbUtils.safeStr(dbname.toLowerCase()) + " \n";
		String and_owner  = !StringUtil.hasValue(owner)  ? "" : "  and lower([owner])      = " + DbUtils.safeStr(owner.toLowerCase())  + " \n";
		String and_table  = !StringUtil.hasValue(table)  ? "" : "  and lower([objectName]) = " + DbUtils.safeStr(table.toLowerCase())  + " \n";

		if (and_dbname.contains("%")) and_dbname = and_dbname.replace(" = ", " like ");
		if (and_owner .contains("%")) and_owner  = and_owner .replace(" = ", " like ");
		if (and_table .contains("%")) and_table  = and_table .replace(" = ", " like ");

		String sql = ""
			    + "select [dbname], [owner], [objectName], [type], [crdate], [sampleTime], [extraInfoText], [objectText], [dependList] \n"  // possibly also: [dependsText]
			    + "from [MonDdlStorage] \n"
			    + "where 1 = 1 \n"
			    + "  and [type] in ('U', 'V') \n"
			    + and_dbname
			    + and_owner
			    + and_table
			    + "";
		
		List<SqlServerTableInfo> tmpList = new ArrayList<>();
		
		sql = conn.quotifySqlString(sql);
		try ( Statement stmnt = conn.createStatement() )
		{
			// Unlimited execution time
			stmnt.setQueryTimeout(0);
			try ( ResultSet rs = stmnt.executeQuery(sql) )
			{
				while(rs.next())
				{
					SqlServerTableInfo ti = new SqlServerTableInfo();
					
					ti._dbName        = rs.getString(1);
					ti._schemaName    = rs.getString(2);
					ti._tableName     = rs.getString(3);
					ti._type          = rs.getString(4);
					ti._crdate        = rs.getTimestamp(5);
					ti._sampleTime    = rs.getTimestamp(6);

					ti._extraInfoText = rs.getString(7);
					ti._objectText    = rs.getString(8);
					ti._childObjects  = rs.getString(9);
					
					tmpList.add(ti);
				}
			}
		}
		catch(SQLException ex)
		{
			//_problem = ex;

			_logger.warn("Problems getting Table Information for dbname='" + dbname + "', owner='" + owner + "', table='" + table + "': " + ex);
			throw ex;
		}
//System.out.println("getTableInformationFromMonDdlStorage(): table='" + table + "', rowcount=" + rowcount + ", dbname='" + dbname + "', schema='" + owner + "', table='" + table + "'.");

		// Create an OUTPUT LIST
//		List<SqlServerTableInfo> retList = new ArrayList<>();
//		Set<SqlServerTableInfo> retSet = new LinkedHashSet<>();
		
		// Loop the record we just found in MonDdlStorage
		for (SqlServerTableInfo ti : tmpList)
		{
			// If the TableInforation has already been added, just grab next one.
			if (result.contains(ti))
				continue;

			// For views, go and get referenced tables (stored in table 'MonDdlStorage', column 'extraInfoText' (with prefix 'TableList: t1, t2, t3'
			// Then read table information for those tables.
			if (ti.isView())
			{
				if (StringUtil.hasValue(ti._extraInfoText))
				{
					// Add to the result (even for VIEWS)
					// NOTE: if we restrict this to only TABLES, then we might get an infinite loop on recursion when traversing DEPENDENT VIEWS
					result.add(ti);
					
					List<String> lines = StringUtil.readLines(ti._extraInfoText);
					for (String row : lines)
					{
						if (row.startsWith("TableList: "))
						{
							row = row.substring("TableList: ".length()).trim();
							List<String> viewReferences = StringUtil.parseCommaStrToList(row, true);

							// What "tables" do this view reference
							ti._viewReferences = viewReferences;
//System.out.println("getTableInformationFromMonDdlStorage(recursCallCount="+recursCallCount+"): ti='"+ti.getFullTableName()+"', is a VIEW, the following references will also be fetched: " + viewReferences);
							// Loop the list
							for (String tableName : viewReferences)
							{
								recursCallCount++;

								// Just call "this" method recursively... and the table lookups will be done
								getTableInformationFromMonDdlStorage(conn, dbname, null, tableName, recursCallCount, result);
							}
						}
					}
				}
				continue;
			} // end: VIEW

			// For User Tables get **VARIOUS** information
			if (ti.isUserTable())
			{
				if (StringUtil.hasValue(ti._objectText) && StringUtil.hasValue(ti._extraInfoText))
				{
					if (getTableAndIndexInfo(ti))
					{
						// Add to the result
						result.add(ti);
						
						//-------------------------------------------------------------------------------------------
						// also get Statistics how often the indexes was used/accessed during the report period
						// This is "optional", if the Counter Model Tables are not there, the info wont be filled in.
						//-------------------------------------------------------------------------------------------

						// get optimizer info: CmIndexUsage  -- sys.dm_db_index_usage_stats          (user_seeks, user_scans, user_lookups, user_updates)
						getOptimizerCounterInfo(conn, ti);

						// get execution info: CmIndexOpStat -- sys.dm_db_index_operational_stats    (range_scan_count, singleton_lookup_count) and (forwarded_fetch_count, lob_fetch_in_pages) possibly to get CrudCount (leaf_insert_count, leaf_delete_count, leaf_update_count)
						getExecutionCounterInfo(conn, ti);
						
						// get Missing index recommendations for THIS table
						getMissingIndexInfo(conn, ti);
						
						// get TRIGGER information for this table
						getTriggerInfo(conn, ti); // type='TR'
					}

				} // end: has: _objectText && _extraInfoText 
			}

		} // end: loop SqlServerTableInfo
		
		// Remove any "duplicates" from the OUTPUT List ??? (since we do recursive calls for VIEW's we probably need to do this.)
		// possibly convert it to a "set" and then back to a list again!
//		retList = new ArrayList<>( new LinkedHashSet<>(retList) );
		
//		return retSet;
	}

	/**
	 * Parse the information from the DdlStorage columns 'objectText' and 'extraInfoText' where various information is stored in.
	 * @param ti
	 * @return
	 */
	private boolean getTableAndIndexInfo(SqlServerTableInfo ti)
	{
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		// Below is how column 'objectText' typically looks like
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//   +--------------------------------+-----+----------+-----------------------+                                                                                                                           
		//   |Name                            |Owner|Type      |Created_datetime       |                                                                                                                           
		//   +--------------------------------+-----+----------+-----------------------+                                                                                                                           
		//   |Kund_till_Kontaktperson_relation|dbo  |user table|2020-06-12 14:15:07.023|                                                                                                                           
		//   +--------------------------------+-----+----------+-----------------------+                                                                                                                           
		//   Rows 1                                                                                                                                                                                                
		//                                                                                                                                                                                                         
		//                                                                                                                                                                                                         
		//   +----------------------------------------+-------+--------+------+----+-----+--------+------------------+--------------------+--------------------------------+                                       
		//   |Column_name                             |Type   |Computed|Length|Prec|Scale|Nullable|TrimTrailingBlanks|FixedLenNullInSource|Collation                       |                                       
		//   +----------------------------------------+-------+--------+------+----+-----+--------+------------------+--------------------+--------------------------------+                                       
		//   |Behorig_foretradare                     |bit    |no      |     1|    |     |yes     |(n/a)             |(n/a)               |(NULL)                          |                                       
		//   |Metod_att_styrka_foretradares_behorighet|varchar|no      |   255|    |     |yes     |no                |yes                 |Latin1_General_100_CI_AS_SC_UTF8|                                       
		//   |Fullmakt_registrerad                    |bit    |no      |     1|    |     |yes     |(n/a)             |(n/a)               |(NULL)                          |                                       
		//   |Kontaktperson_ref                       |varchar|no      |   255|    |     |no      |no                |no                  |Latin1_General_100_CI_AS_SC_UTF8|                                       
		//   |Kund_ref                                |varchar|no      |   255|    |     |no      |no                |no                  |Latin1_General_100_CI_AS_SC_UTF8|                                       
		//   |Email                                   |varchar|no      |   255|    |     |yes     |no                |yes                 |Latin1_General_100_CI_AS_SC_UTF8|                                       
		//   |Metod_att_styrka_foretradares_identitet |varchar|no      |   255|    |     |yes     |no                |yes                 |Latin1_General_100_CI_AS_SC_UTF8|                                       
		//   |ID_handlingens_nummer                   |varchar|no      |   255|    |     |yes     |no                |yes                 |Latin1_General_100_CI_AS_SC_UTF8|                                       
		//   |Typ_av_ID_handling                      |varchar|no      |   255|    |     |yes     |no                |yes                 |Latin1_General_100_CI_AS_SC_UTF8|                                       
		//   |ID_handlingens_giltighetstid            |date   |no      |     3|10  |0    |yes     |(n/a)             |(n/a)               |(NULL)                          |                                       
		//   +----------------------------------------+-------+--------+------+----+-----+--------+------------------+--------------------+--------------------------------+                                       
		//   Rows 10                                                                                                                                                                                               
		//                                                                                                                                                                                                         
		//   +---------------------------+------+---------+-------------------+                                                                                                                                    
		//   |Identity                   |Seed  |Increment|Not For Replication|                                                                                                                                    
		//   +---------------------------+------+---------+-------------------+                                                                                                                                    
		//   |No identity column defined.|(NULL)|(NULL)   |(NULL)             |                                                                                                                                    
		//   +---------------------------+------+---------+-------------------+                                                                                                                                    
		//   Rows 1                                                                                                                                                                                                
		//                                                                                                                                                                                                         
		//   +-----------------------------+                                                                                                                                                                       
		//   |RowGuidCol                   |                                                                                                                                                                       
		//   +-----------------------------+                                                                                                                                                                       
		//   |No rowguidcol column defined.|                                                                                                                                                                       
		//   +-----------------------------+                                                                                                                                                                       
		//   Rows 1                                                                                                                                                                                                
		//                                                                                                                                                                                                         
		//   +-------------------------+                                                                                                                                                                           
		//   |Data_located_on_filegroup|                                                                                                                                                                           
		//   +-------------------------+                                                                                                                                                                           
		//   |PRIMARY                  |                                                                                                                                                                           
		//   +-------------------------+                                                                                                                                                                           
		//   Rows 1                                                                                                                                                                                                
		//                                                                                                                                                                                                         
		//   +-------------------------------------------------------------+---------------------------------------------------+---------------------------+                                                       
		//   |index_name                                                   |index_description                                  |index_keys                 |                                                       
		//   +-------------------------------------------------------------+---------------------------------------------------+---------------------------+                                                       
		//   |INDEX_Kund_till_Kontaktperson_relation__Kontaktperson_ref__FK|nonclustered located on PRIMARY                    |Kontaktperson_ref          |                                                       
		//   |INDEX_Kund_till_Kontaktperson_relation__Kund_ref__FK         |nonclustered located on PRIMARY                    |Kund_ref                   |                                                       
		//   |UQ__Kund_til__163119BDDC9E2531                               |nonclustered, unique, unique key located on PRIMARY|Kund_ref, Kontaktperson_ref|                                                       
		//   +-------------------------------------------------------------+---------------------------------------------------+---------------------------+                                                       
		//   Rows 3                                                                                                                                                                                                
		//                                                                                                                                                                                                         
		//   +----------------------+-----------------------------------------------------------------+-------------+-------------+--------------+----------------------+---------------------------------------+  
		//   |constraint_type       |constraint_name                                                  |delete_action|update_action|status_enabled|status_for_replication|constraint_keys                        |  
		//   +----------------------+-----------------------------------------------------------------+-------------+-------------+--------------+----------------------+---------------------------------------+  
		//   |FOREIGN KEY           |CONSTRAINT_Kund_till_Kontaktperson_relation__Kontaktperson_ref_FK|No Action    |No Action    |Enabled       |Is_For_Replication    |Kontaktperson_ref                      |  
		//   |                      |                                                                 |             |             |              |                      |REFERENCES kyc.dbo.Personuppgifter (Id)|  
		//   |FOREIGN KEY           |CONSTRAINT_Kund_till_Kontaktperson_relation__Kund_ref_FK         |No Action    |No Action    |Enabled       |Is_For_Replication    |Kund_ref                               |  
		//   |                      |                                                                 |             |             |              |                      |REFERENCES kyc.dbo.Kund (Id)           |  
		//   |UNIQUE (non-clustered)|UQ__Kund_til__163119BDDC9E2531                                   |(n/a)        |(n/a)        |(n/a)         |(n/a)                 |Kund_ref, Kontaktperson_ref            |  
		//   +----------------------+-----------------------------------------------------------------+-------------+-------------+--------------+----------------------+---------------------------------------+  
		//   Rows 5                                                                                                                                                                                                
		//                                                                                                                                                                                                         
		//   No foreign keys reference table 'dbo.Kund_till_Kontaktperson_relation', or you do not have permissions on referencing tables.                                                                         
		//   No views with schema binding reference table 'dbo.Kund_till_Kontaktperson_relation'.                                                                                                                  


		
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		// Below is how column 'extraInfoText' typically looks like (without a clustered index)
		// AND with LOB data
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//   +-------------+-----------+-----------------+------------------------+--------+-------------+-----------------------+--------------+-----------+-----------+---------------+---------------+-------------+------------------+---------------+-------------------+----------------------+----------------------+--------------------------+-------------------+-----------------------+----------------------------+--------------------------------+
		//   |dbname       |schema_name|object_name      |index_name              |index_id|IndexType    |StatsUpdated           |PartitionCount|row_count  |RowsPerPage|TotalUsedSizeMB|InRowUsedSizeMB|LobUsedSizeMB|OverflowUsedSizeMB|used_page_count|reserved_page_count|in_row_used_page_count|in_row_data_page_count|in_row_reserved_page_count|lob_used_page_count|lob_reserved_page_count|row_overflow_used_page_count|row_overflow_reserved_page_count|
		//   +-------------+-----------+-----------------+------------------------+--------+-------------+-----------------------+--------------+-----------+-----------+---------------+---------------+-------------+------------------+---------------+-------------------+----------------------+----------------------+--------------------------+-------------------+-----------------------+----------------------------+--------------------------------+
		//   |Datawarehouse|stage      |linda_Benefit_XML|HEAP                    |       0|HEAP         |(NULL)                 |             1|  2,548,485|        2.2|        9,018.8|          198.9|      8,819.9|                 0|      1,154,408|          1,154,423|                25,456|                25,431|                    25,457|          1,128,952|              1,128,966|                           0|                               0|
		//   |Datawarehouse|stage      |linda_Benefit_XML|idx_Benefit_XML_NK      |       2|NON-CLUSTERED|2022-02-12 08:37:51.07 |             1|  2,548,485|      143.8|          138.5|          138.5|            0|                 0|         17,726|             17,737|                17,726|                17,586|                    17,737|                  0|                      0|                           0|                               0|
		//   |Datawarehouse|stage      |linda_Benefit_XML|idx_Benefit_XML_policyno|       3|NON-CLUSTERED|2022-02-12 08:37:54.563|             1|  2,548,485|      270.9|           73.5|           73.5|            0|                 0|          9,409|              9,409|                 9,409|                 9,368|                     9,409|                  0|                      0|                           0|                               0|
		//   |Datawarehouse|stage      |linda_Benefit_XML|idx_Benefit_XML_regno   |       4|NON-CLUSTERED|2022-02-12 08:38:07.59 |             1|  2,548,485|      287.7|           69.2|           69.2|            0|                 0|          8,859|              8,865|                 8,859|                 8,819|                     8,865|                  0|                      0|                           0|                               0|
		//   |Datawarehouse|stage      |linda_Benefit_XML|idx_Benefit_XML_ssn     |       5|NON-CLUSTERED|2022-02-12 08:38:17.633|             1|  2,548,485|      267.7|           74.4|           74.4|            0|                 0|          9,519|              9,529|                 9,519|                 9,474|                     9,529|                  0|                      0|                           0|                               0|
		//   +-------------+-----------+-----------------+------------------------+--------+-------------+-----------------------+--------------+-----------+-----------+---------------+---------------+-------------+------------------+---------------+-------------------+----------------------+----------------------+--------------------------+-------------------+-----------------------+----------------------------+--------------------------------+
		//   Rows 5
		
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		// Below is how column 'extraInfoText' typically looks like (WITH a clustered index)
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//   +-------------+-----------+------------+-----------------------------+--------+-------------+-----------------------+--------------+-----------+-----------+---------------+---------------+-------------+------------------+---------------+-------------------+----------------------+----------------------+--------------------------+-------------------+-----------------------+----------------------------+--------------------------------+
		//   |dbname       |schema_name|object_name |index_name                   |index_id|IndexType    |StatsUpdated           |PartitionCount|row_count  |RowsPerPage|TotalUsedSizeMB|InRowUsedSizeMB|LobUsedSizeMB|OverflowUsedSizeMB|used_page_count|reserved_page_count|in_row_used_page_count|in_row_data_page_count|in_row_reserved_page_count|lob_used_page_count|lob_reserved_page_count|row_overflow_used_page_count|row_overflow_reserved_page_count|
		//   +-------------+-----------+------------+-----------------------------+--------+-------------+-----------------------+--------------+-----------+-----------+---------------+---------------+-------------+------------------+---------------+-------------------+----------------------+----------------------+--------------------------+-------------------+-----------------------+----------------------------+--------------------------------+
		//   |Datawarehouse|etl        |FaktaKapital|CI_FaktaKapital              |       1|CLUSTERED    |2022-02-17 06:38:56.827|           356|119,002,563|       57.6|       16,127.6|       16,127.6|            0|                 0|      2,064,330|          2,201,918|             2,064,330|             2,052,983|                 2,201,918|                  0|                      0|                           0|                               0|
		//   +-------------+-----------+------------+-----------------------------+--------+-------------+-----------------------+--------------+-----------+-----------+---------------+---------------+-------------+------------------+---------------+-------------------+----------------------+----------------------+--------------------------+-------------------+-----------------------+----------------------------+--------------------------------+
		//   Rows 1

		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		// Below is how column 'extraInfoText' typically looks like (WITH a clustered index that has a couple of NON-CLUSTERED indexes)
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//   +-------------+-----------+-----------+------------------------------+--------+-------------+-----------------------+--------------+-----------+-----------+---------------+---------------+-------------+------------------+---------------+-------------------+----------------------+----------------------+--------------------------+-------------------+-----------------------+----------------------------+--------------------------------+
		//   |dbname       |schema_name|object_name|index_name                    |index_id|IndexType    |StatsUpdated           |PartitionCount|row_count  |RowsPerPage|TotalUsedSizeMB|InRowUsedSizeMB|LobUsedSizeMB|OverflowUsedSizeMB|used_page_count|reserved_page_count|in_row_used_page_count|in_row_data_page_count|in_row_reserved_page_count|lob_used_page_count|lob_reserved_page_count|row_overflow_used_page_count|row_overflow_reserved_page_count|
		//   +-------------+-----------+-----------+------------------------------+--------+-------------+-----------------------+--------------+-----------+-----------+---------------+---------------+-------------+------------------+---------------+-------------------+----------------------+----------------------+--------------------------+-------------------+-----------------------+----------------------------+--------------------------------+
		//   |Datawarehouse|dimen      |Radgivare  |UCI_Radgivare                 |       1|CLUSTERED    |2021-08-25 16:18:05.5  |             1|      1,938|       21.3|            0.7|            0.7|            0|                 0|             91|                127|                    91|                    83|                       127|                  0|                      0|                           0|                               0|
		//   |Datawarehouse|dimen      |Radgivare  |PK_Radgivare                  |       2|NON-CLUSTERED|2021-08-25 16:18:05.503|             1|      1,938|      242.3|            0.1|            0.1|            0|                 0|              8|                 25|                     8|                     6|                        25|                  0|                      0|                           0|                               0|
		//   |Datawarehouse|dimen      |Radgivare  |UNI BK_RadgivareKod_Aktuell1  |       4|NON-CLUSTERED|2022-02-17 06:23:40.817|             1|      1,641|      273.5|              0|              0|            0|                 0|              6|                 25|                     6|                     4|                        25|                  0|                      0|                           0|                               0|
		//   |Datawarehouse|dimen      |Radgivare  |NI BK_RadgivareKod            |      10|NON-CLUSTERED|2022-02-17 06:22:59.187|             1|      1,938|      276.9|            0.1|            0.1|            0|                 0|              7|                 25|                     7|                     5|                        25|                  0|                      0|                           0|                               0|
		//   |Datawarehouse|dimen      |Radgivare  |NCI_BK_Akt1                   |      11|NON-CLUSTERED|2021-08-25 16:18:06.98 |             1|      1,641|      182.3|            0.1|            0.1|            0|                 0|              9|                 25|                     9|                     7|                        25|                  0|                      0|                           0|                               0|
		//   +-------------+-----------+-----------+------------------------------+--------+-------------+-----------------------+--------------+-----------+-----------+---------------+---------------+-------------+------------------+---------------+-------------------+----------------------+----------------------+--------------------------+-------------------+-----------------------+----------------------------+--------------------------------+
		//   Rows 5

		// RS> Col# Label                            JDBC Type Name           Guessed DBMS type Source Table
		// RS> ---- -------------------------------- ------------------------ ----------------- ------------
		// RS> 1    dbname                           java.sql.Types.NVARCHAR  nvarchar(128)     -none-      
		// RS> 2    schema_name                      java.sql.Types.NVARCHAR  nvarchar(128)     -none-      
		// RS> 3    object_name                      java.sql.Types.NVARCHAR  nvarchar(128)     -none-      
		// RS> 4    index_name                       java.sql.Types.NVARCHAR  nvarchar(128)     -none-      
		// RS> 5    index_id                         java.sql.Types.INTEGER   int               -none-      
		// RS> 6    IndexType                        java.sql.Types.VARCHAR   varchar(13)       -none-      
		// RS> 7    StatsUpdated                     java.sql.Types.TIMESTAMP datetime          -none-      
		// RS> 8    PartitionCount                   java.sql.Types.INTEGER   int               -none-      
		// RS> 9    row_count                        java.sql.Types.BIGINT    bigint            -none-      
		// RS> 10   RowsPerPage                      java.sql.Types.DECIMAL   decimal(12,1)     -none-      
		// RS> 11   TotalUsedSizeMB                  java.sql.Types.DECIMAL   decimal(12,1)     -none-      
		// RS> 12   InRowUsedSizeMB                  java.sql.Types.DECIMAL   decimal(12,1)     -none-      
		// RS> 13   LobUsedSizeMB                    java.sql.Types.DECIMAL   decimal(12,1)     -none-      
		// RS> 14   OverflowUsedSizeMB               java.sql.Types.DECIMAL   decimal(12,1)     -none-      
		// RS> 15   used_page_count                  java.sql.Types.BIGINT    bigint            -none-      
		// RS> 16   reserved_page_count              java.sql.Types.BIGINT    bigint            -none-      
		// RS> 17   in_row_used_page_count           java.sql.Types.BIGINT    bigint            -none-      
		// RS> 18   in_row_data_page_count           java.sql.Types.BIGINT    bigint            -none-      
		// RS> 19   in_row_reserved_page_count       java.sql.Types.BIGINT    bigint            -none-      
		// RS> 20   lob_used_page_count              java.sql.Types.BIGINT    bigint            -none-      
		// RS> 21   lob_reserved_page_count          java.sql.Types.BIGINT    bigint            -none-      
		// RS> 22   row_overflow_used_page_count     java.sql.Types.BIGINT    bigint            -none-      
		// RS> 23   row_overflow_reserved_page_count java.sql.Types.BIGINT    bigint            -none-      		

		
		// Get table with column names: index_name, index_description, index_keys                                                       
		ResultSetTableModel rstmIndexKeys = ResultSetTableModel.getTableWithColumnNames(ResultSetTableModel.parseTextTables(ti._objectText), true, "index_name");
		
		// Get table with: various index information                                                       
		ResultSetTableModel rstmIndexInfo = ResultSetTableModel.parseTextTable(ti._extraInfoText);

		if (rstmIndexInfo != null && !rstmIndexInfo.isEmpty())
		{
			if (rstmIndexKeys != null) rstmIndexKeys.setHandleColumnNotFoundAsNullValueInGetValues(true);
			if (rstmIndexInfo != null) rstmIndexInfo.setHandleColumnNotFoundAsNullValueInGetValues(true);

//			int indexCount = 0;
//			List<SqlServerIndexInfo> indexInfoList = new ArrayList<>();

			for (int r=0; r<rstmIndexInfo.getRowCount(); r++)
			{
				int indexId = rstmIndexInfo.getValueAsInteger(r, "index_id");

				// BASE TABLE INFORMATION
				// 0 == No clustered index -- a HEAP table
				// 1 == table WITH Clustered index
				if (indexId == 0 || indexId == 1) 
				{
					ti._rowtotal            = rstmIndexInfo.getValueAsLong(r, "row_count"                   , true, -1L);
					ti._totalPages          = rstmIndexInfo.getValueAsLong(r, "used_page_count"             , true, -1L);
					ti._inRowPages          = rstmIndexInfo.getValueAsLong(r, "in_row_used_page_count"      , true, -1L);
					ti._lobPages            = rstmIndexInfo.getValueAsLong(r, "lob_used_page_count"         , true, -1L);
					ti._rowOverflowPages    = rstmIndexInfo.getValueAsLong(r, "row_overflow_used_page_count", true, -1L);
				}

				// INDEX INFORMATION (and a record for DATA/HEAP)
				// Clustered and Non-Clustered indexes
				{
					SqlServerIndexInfo ii = new SqlServerIndexInfo();
					ii._indexID     = indexId;
					ii._indexName   = rstmIndexInfo.getValueAsString (r, "index_name"            , true, null);
					ii._indexPages  = rstmIndexInfo.getValueAsLong   (r, "in_row_used_page_count", true, -1L);
					ii._desc        = rstmIndexInfo.getValueAsString (r, "IndexType"             , true, null);
					ii._fillFactor  = rstmIndexInfo.getValueAsInteger(r, "IndexFillFactor"       , true, -1);
					ii._rowsPerPage = rstmIndexInfo.getValueAsDouble (r, "RowsPerPage"           , true, -1D);

					// For everything else than HEAP
					if (indexId != 0)
					{
						ti._indexCount++;

						ii._lastUpdateStats = rstmIndexInfo.getValueAsTimestamp(r, "StatsUpdated", true, null);
					
						// Fill in SqlServerIndexInfo: _keysStr, _keys, _desc, 
						scrapeIndexInfo(ii._indexName, rstmIndexKeys, ii);
						
						ii._keysStr          = rstmIndexInfo.getValueAsString (r, "IndexKeys"   , true, ii._keysStr);
						ii._includeColsStr   = rstmIndexInfo.getValueAsString (r, "IndexInclude", true, null);
						ii._filterExpression = rstmIndexInfo.getValueAsString (r, "IndexFilter" , true, null);
						ii._ddlTxt           = rstmIndexInfo.getValueAsString (r, "DDL"         , true, null);

						ii._keys             = StringUtil.commaStrToList(ii._keysStr, true);
						ii._includeCols      = StringUtil.commaStrToList(ii._includeColsStr, true);
					}

					// Add index to table list
					ti._indexList.add(ii);
				}
			}
			
			// Calculate IndexSize SUMMARY at the table level
			if (ti._indexList != null)
			{
				for (SqlServerIndexInfo ii : ti._indexList)
				{
					ti._indexPages += ii._indexPages;
				}
			}

			// finally add the entry to the output list
			return true;
			//retList.add(ti);

		} // end: rstmIndexInfo

		return false;
	}

	private void getOptimizerCounterInfo(DbxConnection conn, SqlServerTableInfo ti)
	{
		String dbname     = ti._dbName;
		String schemaName = ti._schemaName;
		String tableName  = ti._tableName;

		// Check if table(s) exists, if not... just exit...
		if ( ! DbUtils.checkIfTableExistsNoThrow(conn, null, null, "CmIndexUsage_diff") )
		{
			_logger.warn("Table 'CmIndexUsage_diff' did not exists, skip reading 'optimizer information'.");
			return;
		}

		if ( ! DbUtils.checkIfTableExistsNoThrow(conn, null, null, "CmIndexUsage_abs") )
		{
			_logger.warn("Table 'CmIndexUsage_abs' did not exists, skip reading 'optimizer information'.");
			return;
		}
		
		// NOTE: INDEXES for below is created in: each ReportEntry.getReportingIndexes()
		
		// get optimizer info: CmIndexUsage  -- sys.dm_db_index_usage_stats          (user_seeks, user_scans, user_lookups, user_updates)
		String sql = ""
			+ "WITH diff as \n"
			+ "( \n"
			+ "    select \n"
			+ "         [DbName] \n"
			+ "        ,[SchemaName] \n"
			+ "        ,[TableName] \n"
			+ "        ,[IndexName] \n"
			+ "        ,[index_id] \n"
			+ "        ,sum([user_seeks]) + sum([user_seeks]) + sum([user_scans]) as [READS] \n"
			+ "        ,sum([user_updates])  as [WRITES] \n"
			+ "        ,sum([user_seeks])    as [user_seeks] \n"
			+ "        ,sum([user_scans])    as [user_scans] \n"
			+ "        ,sum([user_lookups])  as [user_lookups] \n"
			+ "        ,sum([user_updates])  as [user_updates] \n"
			+ "    from [CmIndexUsage_diff] \n"
			+ "    where 1 = 1 \n"
			+ "      and [DbName]     = " + DbUtils.safeStr(dbname    ) + " \n"
			+ "      and [SchemaName] = " + DbUtils.safeStr(schemaName) + " \n"
			+ "      and [TableName]  = " + DbUtils.safeStr(tableName ) + " \n"
			+ "    group by [DbName], [SchemaName], [TableName], [IndexName], [index_id] \n"
			+ ") \n"
			+ ", abs as \n"
			+ "( \n"
			+ "    select \n"
			+ "         [DbName] \n"
			+ "        ,[SchemaName] \n"
			+ "        ,[TableName] \n"
			+ "        ,[IndexName] \n"
			+ "        ,[index_id] \n"
			+ "        ,[user_seeks] + [user_seeks] + [user_scans] as [abs_READS] \n"
			+ "        ,[user_updates]  as [abs_WRITES] \n"
			+ "        ,[user_seeks]    as [abs_user_seeks] \n"
			+ "        ,[user_scans]    as [abs_user_scans] \n"
			+ "        ,[user_lookups]  as [abs_user_lookups] \n"
			+ "        ,[user_updates]  as [abs_user_updates] \n"
			+ "    from [CmIndexUsage_abs] \n"
			+ "    where [SessionSampleTime] = (select max([SessionSampleTime]) from [CmIndexUsage_abs]) \n"
			+ "      and [DbName]     = " + DbUtils.safeStr(dbname    ) + " \n"
			+ "      and [SchemaName] = " + DbUtils.safeStr(schemaName) + " \n"
			+ "      and [TableName]  = " + DbUtils.safeStr(tableName ) + " \n"
			+ ") \n"
			+ "select \n"
			+ "     diff.[DbName] \n"
			+ "    ,diff.[SchemaName] \n"
			+ "    ,diff.[TableName] \n"
			+ "    ,diff.[IndexName] \n"
			+ "    ,diff.[index_id] \n"
			+ " \n"
			+ "    ,diff.[READS] \n"
			+ "    ,diff.[WRITES] \n"
			+ "    ,diff.[user_seeks] \n"
			+ "    ,diff.[user_scans] \n"
			+ "    ,diff.[user_lookups] \n"
			+ "    ,diff.[user_updates] \n"
			+ " \n"
			+ "    ,abs .[abs_READS] \n"
			+ "    ,abs .[abs_WRITES] \n"
			+ "    ,abs. [abs_user_seeks] \n"
			+ "    ,abs. [abs_user_scans] \n"
			+ "    ,abs. [abs_user_lookups] \n"
			+ "    ,abs. [abs_user_updates] \n"
			+ "from diff \n"
			+ "left outer join abs on diff.[DbName]     = abs.[DbName] \n"
			+ "                   and diff.[SchemaName] = abs.[SchemaName] \n"
			+ "                   and diff.[TableName]  = abs.[TableName] \n"
			+ "                   and diff.[IndexName]  = abs.[IndexName] \n"
			+ "order by diff.[index_id] \n"
			+ "";

		sql = conn.quotifySqlString(sql);
		try ( Statement stmnt = conn.createStatement() )
		{
			// set some timeout
			stmnt.setQueryTimeout(10);
			
			try ( ResultSet rs = stmnt.executeQuery(sql) )
			{
				while(rs.next())
				{
				//	String    DbName             = rs.getString   (1);
				//	String    SchemaName         = rs.getString   (2);
				//	String    TableName          = rs.getString   (3);
					String    IndexName          = rs.getString   (4);
				//	int       IndexID            = rs.getInt      (5);
					
					long      READS              = rs.getLong     (6);
					long      WRITES             = rs.getLong     (7);
					long      user_seeks         = rs.getLong     (8);
					long      user_scans         = rs.getLong     (9);
					long      user_lookups       = rs.getLong     (10);
					long      user_updates       = rs.getLong     (11);
					
					long      abs_READS          = rs.getLong     (12);
					long      abs_WRITES         = rs.getLong     (13);
					long      abs_user_seeks     = rs.getLong     (14);
					long      abs_user_scans     = rs.getLong     (15);
					long      abs_user_lookups   = rs.getLong     (16);
					long      abs_user_updates   = rs.getLong     (17);
					
					SqlServerIndexInfo indexEntry = ti.getIndexInfoForName(IndexName);
					if (indexEntry != null)
					{
						indexEntry._READS            = READS;
						indexEntry._WRITES           = WRITES;
						indexEntry._user_seeks       = user_seeks;
						indexEntry._user_scans       = user_scans;
						indexEntry._user_lookups     = user_lookups;
						indexEntry._user_updates     = user_updates;

						indexEntry._abs_READS        = abs_READS;
						indexEntry._abs_WRITES       = abs_WRITES;
						indexEntry._abs_user_seeks   = abs_user_seeks;
						indexEntry._abs_user_scans   = abs_user_scans;
						indexEntry._abs_user_lookups = abs_user_lookups;
						indexEntry._abs_user_updates = abs_user_updates;
					}
				}
			}
		}
		catch(SQLException ex)
		{
			_logger.warn("Problems getting INDEX Information at 'getOptimizerCounterInfo()', for dbname='" + dbname + "', schema='" + schemaName + "', table='" + tableName + "'. Skipping and continuing.", ex);
		}
	}

	private void getExecutionCounterInfo(DbxConnection conn, SqlServerTableInfo ti)
	{
		String dbname     = ti._dbName;
		String schemaName = ti._schemaName;
		String tableName  = ti._tableName;

		// Check if table(s) exists, if not... just exit...
		if ( ! DbUtils.checkIfTableExistsNoThrow(conn, null, null, "CmIndexOpStat_diff") )
		{
			_logger.warn("Table 'CmIndexOpStat_diff' did not exists, skip reading 'execution information'.");
			return;
		}

		if ( ! DbUtils.checkIfTableExistsNoThrow(conn, null, null, "CmIndexOpStat_abs") )
		{
			_logger.warn("Table 'CmIndexOpStat_abs' did not exists, skip reading 'execution information'.");
			return;
		}

		// NOTE: INDEXES for below is created in: each ReportEntry.getReportingIndexes()
		
		// get optimizer info: CmIndexUsage  -- sys.dm_db_index_usage_stats          (user_seeks, user_scans, user_lookups, user_updates)
		String sql = ""
			+ "WITH diff as \n"
			+ "( \n"
			+ "    select \n"
			+ "         [DbName] \n"
			+ "        ,[SchemaName] \n"
			+ "        ,[TableName] \n"
			+ "        ,[IndexName] \n"
			+ "        ,[index_id] \n"
			+ "        ,sum([range_scan_count])                   as [range_scan_count] \n"
			+ "        ,sum([singleton_lookup_count])             as [singleton_lookup_count] \n"
			+ "        ,sum([forwarded_fetch_count])              as [forwarded_fetch_count] \n"
			+ "        ,sum([lob_fetch_in_pages])                 as [lob_fetch_in_pages] \n"
			+ "        ,sum([leaf_insert_count])                  as [leaf_insert_count] \n"
			+ "        ,sum([leaf_delete_count])                  as [leaf_delete_count] \n"
			+ "        ,sum([leaf_update_count])                  as [leaf_update_count] \n"
			+ "        ,sum([leaf_ghost_count])                   as [leaf_ghost_count] \n"
			+ "        ,sum([leaf_insert_count]) + sum([leaf_delete_count]) + sum([leaf_update_count]) + sum([leaf_ghost_count]) as [leaf_crud_count] \n"
			+ "        ,sum([page_latch_wait_count])              as [page_latch_wait_count] \n"
			+ "        ,sum([page_latch_wait_in_ms])              as [page_latch_wait_in_ms] \n"
			+ "        ,sum([page_io_latch_wait_count])           as [page_io_latch_wait_count] \n"
			+ "        ,sum([page_io_latch_wait_in_ms])           as [page_io_latch_wait_in_ms] \n"
			+ "        ,sum([row_lock_count])                     as [row_lock_count] \n"
			+ "        ,sum([row_lock_wait_count])                as [row_lock_wait_count] \n"
			+ "        ,sum([row_lock_wait_in_ms])                as [row_lock_wait_in_ms] \n"
			+ "        ,sum([page_lock_count])                    as [page_lock_count] \n"
			+ "        ,sum([page_lock_wait_count])               as [page_lock_wait_count] \n"
			+ "        ,sum([page_lock_wait_in_ms])               as [page_lock_wait_in_ms] \n"
			+ "        ,sum([index_lock_promotion_attempt_count]) as [index_lock_promotion_attempt_count] \n"
			+ "        ,sum([index_lock_promotion_count])         as [index_lock_promotion_count] \n"
			+ "    from [CmIndexOpStat_diff] \n"
			+ "    where 1 = 1 \n"
			+ "      and [DbName]     = " + DbUtils.safeStr(dbname    ) + " \n"
			+ "      and [SchemaName] = " + DbUtils.safeStr(schemaName) + " \n"
			+ "      and [TableName]  = " + DbUtils.safeStr(tableName ) + " \n"
			+ "    group by [DbName], [SchemaName], [TableName], [IndexName], [index_id] \n"
			+ ") \n"
			+ ", abs as \n"
			+ "( \n"
			+ "    select \n"
			+ "         [DbName] \n"
			+ "        ,[SchemaName] \n"
			+ "        ,[TableName] \n"
			+ "        ,[IndexName] \n"
			+ "        ,[index_id] \n"
			+ "        ,[range_scan_count]                   as [abs_range_scan_count] \n"
			+ "        ,[singleton_lookup_count]             as [abs_singleton_lookup_count] \n"
			+ "        ,[forwarded_fetch_count]              as [abs_forwarded_fetch_count] \n"
			+ "        ,[lob_fetch_in_pages]                 as [abs_lob_fetch_in_pages] \n"
			+ "        ,[leaf_insert_count] + [leaf_delete_count] + [leaf_update_count] + [leaf_ghost_count] as [abs_leaf_crud_count] \n"
			+ "        ,[leaf_insert_count]                  as [abs_leaf_insert_count] \n"
			+ "        ,[leaf_delete_count]                  as [abs_leaf_delete_count] \n"
			+ "        ,[leaf_update_count]                  as [abs_leaf_update_count] \n"
			+ "        ,[leaf_ghost_count]                   as [abs_leaf_ghost_count] \n"
			+ "        ,[page_latch_wait_count]              as [abs_page_latch_wait_count] \n"
			+ "        ,[page_latch_wait_in_ms]              as [abs_page_latch_wait_in_ms] \n"
			+ "        ,[page_io_latch_wait_count]           as [abs_page_io_latch_wait_count] \n"
			+ "        ,[page_io_latch_wait_in_ms]           as [abs_page_io_latch_wait_in_ms] \n"
			+ "        ,[row_lock_count]                     as [abs_row_lock_count] \n"
			+ "        ,[row_lock_wait_count]                as [abs_row_lock_wait_count] \n"
			+ "        ,[row_lock_wait_in_ms]                as [abs_row_lock_wait_in_ms] \n"
			+ "        ,[page_lock_count]                    as [abs_page_lock_count] \n"
			+ "        ,[page_lock_wait_count]               as [abs_page_lock_wait_count] \n"
			+ "        ,[page_lock_wait_in_ms]               as [abs_page_lock_wait_in_ms] \n"
			+ "        ,[index_lock_promotion_attempt_count] as [abs_index_lock_promotion_attempt_count] \n"
			+ "        ,[index_lock_promotion_count]         as [abs_index_lock_promotion_count] \n"
			+ "    from [CmIndexOpStat_abs] \n"
			+ "    where [SessionSampleTime] = (select max([SessionSampleTime]) from [CmIndexOpStat_abs]) \n"
			+ "      and [DbName]     = " + DbUtils.safeStr(dbname    ) + " \n"
			+ "      and [SchemaName] = " + DbUtils.safeStr(schemaName) + " \n"
			+ "      and [TableName]  = " + DbUtils.safeStr(tableName ) + " \n"
			+ ") \n"
			+ "select \n"
			+ "     diff.[DbName] \n"                     // pos=1
			+ "    ,diff.[SchemaName] \n"
			+ "    ,diff.[TableName] \n"
			+ "    ,diff.[IndexName] \n"
			+ "    ,diff.[index_id] \n"
			+ " \n"
			+ "    ,diff.[range_scan_count] \n"           // pos=6
			+ "    ,diff.[singleton_lookup_count] \n"
			+ "    ,diff.[forwarded_fetch_count] \n"
			+ "    ,diff.[lob_fetch_in_pages] \n"
			+ "    ,diff.[leaf_crud_count] \n"
			+ "    ,diff.[leaf_insert_count] \n"
			+ "    ,diff.[leaf_delete_count] \n"
			+ "    ,diff.[leaf_update_count] \n"
			+ "    ,diff.[leaf_ghost_count] \n"
			+ "    ,diff.[page_latch_wait_count] \n"
			+ "    ,diff.[page_latch_wait_in_ms] \n"
			+ "    ,diff.[page_io_latch_wait_count] \n"
			+ "    ,diff.[page_io_latch_wait_in_ms] \n"
			+ "    ,diff.[row_lock_count] \n"
			+ "    ,diff.[row_lock_wait_count] \n"
			+ "    ,diff.[row_lock_wait_in_ms] \n"
			+ "    ,diff.[page_lock_count] \n"
			+ "    ,diff.[page_lock_wait_count] \n"
			+ "    ,diff.[page_lock_wait_in_ms] \n"
			+ "    ,diff.[index_lock_promotion_attempt_count] \n"
			+ "    ,diff.[index_lock_promotion_count] \n"
			+ " \n"
			+ "    ,abs. [abs_range_scan_count] \n"            // pos=27
			+ "    ,abs. [abs_singleton_lookup_count] \n"
			+ "    ,abs. [abs_forwarded_fetch_count] \n"
			+ "    ,abs. [abs_lob_fetch_in_pages] \n"
			+ "    ,abs. [abs_leaf_crud_count] \n"
			+ "    ,abs. [abs_leaf_insert_count] \n"
			+ "    ,abs. [abs_leaf_delete_count] \n"
			+ "    ,abs. [abs_leaf_update_count] \n"
			+ "    ,abs. [abs_leaf_ghost_count] \n"
			+ "    ,abs. [abs_page_latch_wait_count] \n"
			+ "    ,abs. [abs_page_latch_wait_in_ms] \n"
			+ "    ,abs. [abs_page_io_latch_wait_count] \n"
			+ "    ,abs. [abs_page_io_latch_wait_in_ms] \n"
			+ "    ,abs. [abs_row_lock_count] \n"
			+ "    ,abs. [abs_row_lock_wait_count] \n"
			+ "    ,abs. [abs_row_lock_wait_in_ms] \n"
			+ "    ,abs. [abs_page_lock_count] \n"
			+ "    ,abs. [abs_page_lock_wait_count] \n"
			+ "    ,abs. [abs_page_lock_wait_in_ms] \n"
			+ "    ,abs. [abs_index_lock_promotion_attempt_count] \n"
			+ "    ,abs. [abs_index_lock_promotion_count] \n"          // pos=47
			+ " \n"
			+ "from diff \n"
			+ "left outer join abs on diff.[DbName]     = abs.[DbName] \n"
			+ "                   and diff.[SchemaName] = abs.[SchemaName] \n"
			+ "                   and diff.[TableName]  = abs.[TableName] \n"
			+ "                   and diff.[IndexName]  = abs.[IndexName] \n"
			+ "order by diff.[index_id] \n"
			+ "";

		sql = conn.quotifySqlString(sql);
		try ( Statement stmnt = conn.createStatement() )
		{
			// set some timeout
			stmnt.setQueryTimeout(10);
			
			try ( ResultSet rs = stmnt.executeQuery(sql) )
			{
				while(rs.next())
				{
				//	String    DbName                                 = rs.getString   (1);
				//	String    SchemaName                             = rs.getString   (2);
				//	String    TableName                              = rs.getString   (3);
					String    IndexName                              = rs.getString   (4);
				//	int       IndexID                                = rs.getInt      (5);
					                                                 
					long      range_scan_count                       = rs.getLong     (6);
					long      singleton_lookup_count                 = rs.getLong     (7);
					long      forwarded_fetch_count                  = rs.getLong     (8);
					long      lob_fetch_in_pages                     = rs.getLong     (9);
					long      leaf_crud_count                        = rs.getLong     (10);
					long      leaf_insert_count                      = rs.getLong     (11);
					long      leaf_delete_count                      = rs.getLong     (12);
					long      leaf_update_count                      = rs.getLong     (13);
					long      leaf_ghost_count                       = rs.getLong     (14);
					long      page_latch_wait_count                  = rs.getLong     (15);
					long      page_latch_wait_in_ms                  = rs.getLong     (16);
					long      page_io_latch_wait_count               = rs.getLong     (17);
					long      page_io_latch_wait_in_ms               = rs.getLong     (18);
					long      row_lock_count                         = rs.getLong     (19);
					long      row_lock_wait_count                    = rs.getLong     (20);
					long      row_lock_wait_in_ms                    = rs.getLong     (21);
					long      page_lock_count                        = rs.getLong     (22);
					long      page_lock_wait_count                   = rs.getLong     (23);
					long      page_lock_wait_in_ms                   = rs.getLong     (24);
					long      index_lock_promotion_attempt_count     = rs.getLong     (25);
					long      index_lock_promotion_count             = rs.getLong     (26);

					long      abs_range_scan_count                   = rs.getLong     (27);
					long      abs_singleton_lookup_count             = rs.getLong     (28);
					long      abs_forwarded_fetch_count              = rs.getLong     (29);
					long      abs_lob_fetch_in_pages                 = rs.getLong     (30);
					long      abs_leaf_crud_count                    = rs.getLong     (31);
					long      abs_leaf_insert_count                  = rs.getLong     (32);
					long      abs_leaf_delete_count                  = rs.getLong     (33);
					long      abs_leaf_update_count                  = rs.getLong     (34);
					long      abs_leaf_ghost_count                   = rs.getLong     (35);
					long      abs_page_latch_wait_count              = rs.getLong     (36);
					long      abs_page_latch_wait_in_ms              = rs.getLong     (37);
					long      abs_page_io_latch_wait_count           = rs.getLong     (38);
					long      abs_page_io_latch_wait_in_ms           = rs.getLong     (39);
					long      abs_row_lock_count                     = rs.getLong     (40);
					long      abs_row_lock_wait_count                = rs.getLong     (41);
					long      abs_row_lock_wait_in_ms                = rs.getLong     (42);
					long      abs_page_lock_count                    = rs.getLong     (43);
					long      abs_page_lock_wait_count               = rs.getLong     (44);
					long      abs_page_lock_wait_in_ms               = rs.getLong     (45);
					long      abs_index_lock_promotion_attempt_count = rs.getLong     (46);
					long      abs_index_lock_promotion_count         = rs.getLong     (47);
					
					SqlServerIndexInfo indexEntry = ti.getIndexInfoForName(IndexName);
					if (indexEntry != null)
					{
						indexEntry._range_scan_count                       = range_scan_count;
						indexEntry._singleton_lookup_count                 = singleton_lookup_count;
						indexEntry._forwarded_fetch_count                  = forwarded_fetch_count;
						indexEntry._lob_fetch_in_pages                     = lob_fetch_in_pages;
						indexEntry._leaf_crud_count                        = leaf_crud_count;
						indexEntry._leaf_insert_count                      = leaf_insert_count;
						indexEntry._leaf_delete_count                      = leaf_delete_count;
						indexEntry._leaf_update_count                      = leaf_update_count;
						indexEntry._leaf_ghost_count                       = leaf_ghost_count;
						indexEntry._page_latch_wait_count                  = page_latch_wait_count;
						indexEntry._page_latch_wait_in_ms                  = page_latch_wait_in_ms;
						indexEntry._page_io_latch_wait_count               = page_io_latch_wait_count;
						indexEntry._page_io_latch_wait_in_ms               = page_io_latch_wait_in_ms;
						indexEntry._row_lock_count                         = row_lock_count;
						indexEntry._row_lock_wait_count                    = row_lock_wait_count;
						indexEntry._row_lock_wait_in_ms                    = row_lock_wait_in_ms;
						indexEntry._page_lock_count                        = page_lock_count;
						indexEntry._page_lock_wait_count                   = page_lock_wait_count;
						indexEntry._page_lock_wait_in_ms                   = page_lock_wait_in_ms;
						indexEntry._index_lock_promotion_attempt_count     = index_lock_promotion_attempt_count;
						indexEntry._index_lock_promotion_count             = index_lock_promotion_count;

						indexEntry._abs_range_scan_count                   = abs_range_scan_count;
						indexEntry._abs_singleton_lookup_count             = abs_singleton_lookup_count;
						indexEntry._abs_forwarded_fetch_count              = abs_forwarded_fetch_count;
						indexEntry._abs_lob_fetch_in_pages                 = abs_lob_fetch_in_pages;
						indexEntry._abs_leaf_crud_count                    = abs_leaf_crud_count;
						indexEntry._abs_leaf_insert_count                  = abs_leaf_insert_count;
						indexEntry._abs_leaf_delete_count                  = abs_leaf_delete_count;
						indexEntry._abs_leaf_update_count                  = abs_leaf_update_count;
						indexEntry._abs_leaf_ghost_count                   = abs_leaf_ghost_count;
						indexEntry._abs_page_latch_wait_count              = abs_page_latch_wait_count;
						indexEntry._abs_page_latch_wait_in_ms              = abs_page_latch_wait_in_ms;
						indexEntry._abs_page_io_latch_wait_count           = abs_page_io_latch_wait_count;
						indexEntry._abs_page_io_latch_wait_in_ms           = abs_page_io_latch_wait_in_ms;
						indexEntry._abs_row_lock_count                     = abs_row_lock_count;
						indexEntry._abs_row_lock_wait_count                = abs_row_lock_wait_count;
						indexEntry._abs_row_lock_wait_in_ms                = abs_row_lock_wait_in_ms;
						indexEntry._abs_page_lock_count                    = abs_page_lock_count;
						indexEntry._abs_page_lock_wait_count               = abs_page_lock_wait_count;
						indexEntry._abs_page_lock_wait_in_ms               = abs_page_lock_wait_in_ms;
						indexEntry._abs_index_lock_promotion_attempt_count = abs_index_lock_promotion_attempt_count;
						indexEntry._abs_index_lock_promotion_count         = abs_index_lock_promotion_count;
					}
				}
			}
		}
		catch(SQLException ex)
		{
			_logger.warn("Problems getting INDEX Information at 'getExecutionCounterInfo()', for dbname='" + dbname + "', schema='" + schemaName + "', table='" + tableName + "'. Skipping and continuing.", ex);
		}
	}

	
	private static void scrapeIndexInfo(String indexName, ResultSetTableModel rstm, SqlServerIndexInfo indexInfo)
	{
		if (rstm == null)
			return;

		if (rstm.isEmpty())
			return;

		if (indexInfo == null)
			throw new IllegalArgumentException("scrapeIndexInfo(): indexInfo can't be null.");

		if (StringUtil.isNullOrBlank(indexName))
			throw new IllegalArgumentException("scrapeIndexInfo(): indexName can't be null or ''.");

		// possible columns:  index_name, index_description, index_keys
		List<Integer> rowIds = rstm.getRowIdsWhere("index_name", indexName);
		if ( rowIds.size() == 1 )
		{
			int row = rowIds.get(0);
			
			String xIndexKeys = rstm.getValueAsString(row, "index_keys");
			String xIndexDesc = rstm.getValueAsString(row, "index_description");
			
			// Set the values in the passed SqlServerIndexInfo
			indexInfo._keysStr        = xIndexKeys;
			indexInfo._keys           = StringUtil.commaStrToList(xIndexKeys, true);
			indexInfo._includeColsStr = "-not-impl-";
			indexInfo._includeCols    = StringUtil.commaStrToList(indexInfo._includeColsStr, true);
			indexInfo._desc           = xIndexDesc;
		}
	}
	
	private void getMissingIndexInfo(DbxConnection conn, SqlServerTableInfo ti)
	{
		if (ti == null)
			return;

		String dbname     = ti._dbName;
		String schemaName = ti._schemaName;
		String tableName  = ti._tableName;

		// Check if table(s) exists, if not... just exit...
		if ( ! DbUtils.checkIfTableExistsNoThrow(conn, null, null, "CmIndexMissing_abs") )
		{
			_logger.warn("Table 'CmIndexMissing_abs' did not exists, skip reading 'execution information'.");
			return;
		}

		String sql = ""
				+ "select \n"
				+ "     [index_handle] \n"
				+ "    ,[Impact] \n"
				+ "    ,[user_scans] \n"
				+ "    ,[user_seeks] \n"
				+ "    ,[last_user_scan] \n"
				+ "    ,[last_user_seek] \n"
				+ "    ,[avg_total_user_cost] \n"
				+ "    ,[avg_user_impact] \n"

				+ "    ,[equality_columns] \n"
				+ "    ,[inequality_columns] \n"
				+ "    ,[included_columns] \n"
				+ "    ,[CreateIndexStatement] \n"
				
				+ "from [CmIndexMissing_abs] x \n"
				+ "where [SessionSampleTime] = (select max([SessionSampleTime]) from [CmIndexMissing_abs]) \n"
				+ "  and [DbName]     = '" + dbname     + "' \n"
				+ "  and [SchemaName] = '" + schemaName + "' \n"
				+ "  and [TableName]  = '" + tableName  + "' \n"
				+ "order by [Impact] desc \n"
				+ "";
		try
		{
			ti._missingIndexInfo = executeQuery(conn, sql, "MissingIndexInfo");
		}
		catch (SQLException ex) 
		{
			_logger.error("Problems getting 'missing index' information for: dbname='" + dbname + "', schema='" + schemaName + "', table='" + tableName + "'.", ex);
		}
	}
		
	private void getTriggerInfo(DbxConnection conn, SqlServerTableInfo tableInfo)
	{
		if (tableInfo == null)
			return;

		// Get triggers from a list of: childObjects
		List<String> triggerNames = new ArrayList<>();
		for (String coe : StringUtil.parseCommaStrToList(tableInfo._childObjects, true))
		{
			if (coe.startsWith("TR:"))
			{
				triggerNames.add(coe.substring("TR:".length()));
			}
		}

		if (triggerNames.isEmpty())
			return;

		String fullObjName = tableInfo._dbName +  "." + tableInfo._schemaName + "." + tableInfo._tableName;
		String header = ""
				+ "------------------------------------------------------------------------------------------------\n"
				+ "-- Found " + triggerNames.size() + " Trigger(s) on table: " + fullObjName + "\n"
				+ "-- List: " + StringUtil.toCommaStr(triggerNames) + "\n"
				+ "------------------------------------------------------------------------------------------------\n"
				+ "\n"
				+ "";
		tableInfo._triggersText = header;

		// Get DDL text for ALL triggers in the above List
		for (String trName : triggerNames)
		{
			String trStr = getTriggerDdlText(conn, tableInfo._dbName, tableInfo._schemaName, tableInfo._tableName, trName);
			
			if (StringUtil.hasValue(trStr))
			{
				if (tableInfo._triggersText == null)
					tableInfo._triggersText = "";
				
				tableInfo._triggersText += trStr;
			}
		}
	}

	private String getTriggerDdlText(DbxConnection conn, String dbname, String schemaName, String tableName, String triggerName)
	{
//		String dbname = dbname;
		String owner  = schemaName;
		String table  = tableName;
		String trName = triggerName;

		String and_dbname = !StringUtil.hasValue(dbname) ? "" : "  and lower([dbname])     = " + DbUtils.safeStr(dbname.toLowerCase()) + " \n";
		String and_owner  = !StringUtil.hasValue(owner)  ? "" : "  and lower([owner])      = " + DbUtils.safeStr(owner .toLowerCase()) + " \n";
//		String and_table  = !StringUtil.hasValue(table)  ? "" : "  and lower([objectName]) = " + DbUtils.safeStr(table .toLowerCase()) + " \n";
		String and_trName = !StringUtil.hasValue(trName) ? "" : "  and lower([objectName]) = " + DbUtils.safeStr(trName.toLowerCase()) + " \n";

//		if (and_dbname.contains("%")) and_dbname = and_dbname.replace(" = ", " like ");
//		if (and_owner .contains("%")) and_owner  = and_owner .replace(" = ", " like ");
////		if (and_table .contains("%")) and_table  = and_table .replace(" = ", " like ");
//		if (and_trName.contains("%")) and_trName = and_trName.replace(" = ", " like ");

		String sql = ""
			    + "select [dbname], [owner], [objectName], [type], [crdate], [sampleTime], [extraInfoText], [objectText] \n"
			    + "from [MonDdlStorage] \n"
			    + "where 1 = 1 \n"
			    + "  and [type] = 'TR' \n"
			    + and_dbname
			    + and_owner
//			    + and_table
			    + and_trName
			    + "";
		
		List<SqlServerTableInfo> tmpList = new ArrayList<>();
		
		sql = conn.quotifySqlString(sql);
		try ( Statement stmnt = conn.createStatement() )
		{
			// Unlimited execution time
			stmnt.setQueryTimeout(0);
			try ( ResultSet rs = stmnt.executeQuery(sql) )
			{
				while(rs.next())
				{
					SqlServerTableInfo ti = new SqlServerTableInfo();
					
					ti._dbName        = rs.getString(1);
					ti._schemaName    = rs.getString(2);
					ti._tableName     = rs.getString(3);
					ti._type          = rs.getString(4);
					ti._crdate        = rs.getTimestamp(5);
					ti._sampleTime    = rs.getTimestamp(6);

					ti._extraInfoText = rs.getString(7);
					ti._objectText    = rs.getString(8);
					
					tmpList.add(ti);
				}
			}
		}
		catch(SQLException ex)
		{
			_logger.warn("Problems getting Trigger Information for dbname='" + dbname + "', owner='" + owner + "', table='" + table + "', trName='" + trName + "': " + ex);
		}

		if (tmpList.isEmpty())
			return "";

		if (tmpList.size() > 1)
			_logger.warn("Getting DDL Text for trigger: dbname='" + dbname + "', owner='" + owner + "', table='" + table + "', trName='" + trName + "': Found more that 1 row. they will also be added but... tmpList.size()=" + tmpList.size());

		// Output
		StringBuilder sb = new StringBuilder();
		
		// Loop the record we just found in MonDdlStorage
		for (SqlServerTableInfo ti : tmpList)
		{
			if (StringUtil.hasValue(ti._objectText))
			{
				sb.append("\n");
				sb.append("--##############################################################################################\n");
				sb.append("-- Trigger Name:   ").append(ti._tableName).append("\n");
				sb.append("-- Trigger CrDate: ").append(ti._crdate).append("\n");
				sb.append("--##############################################################################################\n");
				sb.append(ti._objectText);
				sb.append("--##############################################################################################\n");
				sb.append("\n");
			}
		} // end: loop SqlServerTableInfo

		// Set the text
		return sb.toString();
	}
		
	
	public static class SqlServerIndexInfo
	{
		private String       _indexName;
		private long         _indexPages;
//		private int          _sizeKb            = -1;
//		private int          _reservedKb        = -1;
//		private int          _unusedKb          = -1;
		private String       _keysStr;
		private List<String> _keys;
		private String       _includeColsStr;
		private List<String> _includeCols;
		private String       _filterExpression;
		private String       _desc;
		private String       _ddlTxt;
		private int          _indexID           = -1;
		private int          _fillFactor        = -1;
		private double       _rowsPerPage       = -1;
		private long         _rowcount;
		private Timestamp    _lastUpdateStats;
//		private Timestamp    _CreationDate;
//		private Timestamp    _ObjectCacheDate;
//		private long         _RowsInsUpdDel     = -1;
//		private long         _OptSelectCount    = -1;
//		private long         _UsedCount         = -1;
//		private long         _Operations        = -1;
//		private long         _AbsRowsInsUpdDel  = -1;
//		private long         _AbsOptSelectCount = -1;
//		private long         _AbsUsedCount      = -1;
//		private long         _AbsOperations     = -1;

//		public int    _avgRowSizeInBytes;
//		public Double _avgPgSpaceUsedInPct;
		
		// Optimizer Counters
		private long _READS                                  = -1;
		private long _WRITES                                 = -1;
		private long _user_seeks                             = -1;
		private long _user_scans                             = -1;
		private long _user_lookups                           = -1;
		private long _user_updates                           = -1;
                                                             
		private long _abs_READS                              = -1;
		private long _abs_WRITES                             = -1;
		private long _abs_user_seeks                         = -1;
		private long _abs_user_scans                         = -1;
		private long _abs_user_lookups                       = -1;
		private long _abs_user_updates                       = -1;

		// Execution Counters
		private long _range_scan_count                       = -1;
		private long _singleton_lookup_count                 = -1;
		private long _forwarded_fetch_count                  = -1;
		private long _lob_fetch_in_pages                     = -1;
		private long _leaf_crud_count                        = -1;
		private long _leaf_insert_count                      = -1;
		private long _leaf_delete_count                      = -1;
		private long _leaf_update_count                      = -1;
		private long _leaf_ghost_count                       = -1;
		private long _page_latch_wait_count                  = -1;
		private long _page_latch_wait_in_ms                  = -1;
		private long _page_io_latch_wait_count               = -1;
		private long _page_io_latch_wait_in_ms               = -1;
		private long _row_lock_count                         = -1;
		private long _row_lock_wait_count                    = -1;
		private long _row_lock_wait_in_ms                    = -1;
		private long _page_lock_count                        = -1;
		private long _page_lock_wait_count                   = -1;
		private long _page_lock_wait_in_ms                   = -1;
		private long _index_lock_promotion_attempt_count     = -1;
		private long _index_lock_promotion_count             = -1;

		private long _abs_range_scan_count                   = -1;
		private long _abs_singleton_lookup_count             = -1;
		private long _abs_forwarded_fetch_count              = -1;
		private long _abs_lob_fetch_in_pages                 = -1;
		private long _abs_leaf_crud_count                    = -1;
		private long _abs_leaf_insert_count                  = -1;
		private long _abs_leaf_delete_count                  = -1;
		private long _abs_leaf_update_count                  = -1;
		private long _abs_leaf_ghost_count                   = -1;
		private long _abs_page_latch_wait_count              = -1;
		private long _abs_page_latch_wait_in_ms              = -1;
		private long _abs_page_io_latch_wait_count           = -1;
		private long _abs_page_io_latch_wait_in_ms           = -1;
		private long _abs_row_lock_count                     = -1;
		private long _abs_row_lock_wait_count                = -1;
		private long _abs_row_lock_wait_in_ms                = -1;
		private long _abs_page_lock_count                    = -1;
		private long _abs_page_lock_wait_count               = -1;
		private long _abs_page_lock_wait_in_ms               = -1;
		private long _abs_index_lock_promotion_attempt_count = -1;
		private long _abs_index_lock_promotion_count         = -1;


		public String       getIndexName         () { return _indexName; }
		public long         getIndexPages        () { return _indexPages; }
		public long         getIndexSizeKb       () { return _indexPages * 8; }
		public double       getIndexSizeMb       () { return MathUtils.round(getIndexSizeKb() / 1024.0, 1); }
//		public int          getReservedKb        () { return _reservedKb; }
//		public int          getUnusedKb          () { return _unusedKb; }
		public String       getKeysStr           () { return _keysStr          == null ? "-" : _keysStr; }
		public List<String> getKeys              () { return _keys; }
		public String       getIncludeStr        () { return _includeColsStr   == null ? "-" : _includeColsStr; }
		public List<String> getIncludeCols       () { return _includeCols; }
		public String       getFilterExpression  () { return _filterExpression == null ? "-" : _filterExpression; }
		public String       getDescription       () { return _desc; }
		public String       getDdlText           () { return _ddlTxt; }
                                                 
		public int          getIndexID           () { return _indexID;            }
		public long         getRowCount          () { return _rowcount;           }
		public Timestamp    getLastUpdateStats   () { return _lastUpdateStats;    }
		public int          getFillFactor        () { return _fillFactor; }
		public double       getRowsPerPage       () { return _rowsPerPage; }
		
		
		
//		public Timestamp    getCreationDate      () { return _CreationDate;       }
//		public String       getCreationDateStr   () { return _CreationDate    == null ? "--not-found--" : _CreationDate.toString(); }
//		public Timestamp    getCacheDate         () { return _ObjectCacheDate;    }
//		public String       getCacheDateStr      () { return _ObjectCacheDate == null ? "--not-found--" : _ObjectCacheDate.toString(); }
//		public long         getRowsInsUpdDel     () { return _RowsInsUpdDel;     }
//		public long         getOptSelectCount    () { return _OptSelectCount;     }
//		public long         getUsedCount         () { return _UsedCount;          }
//		public long         getOperations        () { return _Operations;         }
//		public long         getAbsRowsInsUpdDel  () { return _AbsRowsInsUpdDel;   }
//		public long         getAbsOptSelectCount () { return _AbsOptSelectCount;  }
//		public long         getAbsUsedCount      () { return _AbsUsedCount;       }
//		public long         getAbsOperations     () { return _AbsOperations;      }

//		public int       getAvgRowSizeBytes()     { return _avgRowSizeInBytes; }
//		public Double    getAvgPageSpaceUsedPct() { return _avgPgSpaceUsedInPct; }
		
//		public double getReservedMb() { return MathUtils.round(getReservedKb() / 1024.0, 1); }
//		public double getUnusedMb()   { return MathUtils.round(getUnusedKb()   / 1024.0, 1); }
	}


	public static class SqlServerTableInfo
	{
		private int    _partitionCount;
		private long   _totalPages;
		private long   _inRowPages;
		private long   _lobPages;
		private long   _rowOverflowPages;
//		private long   _compressedPageCount;
//		private long   _forwardedRowCount;
//		private long   _ghostRowCount;
//		private long   _dataPages;
		private long   _indexPages;
		
		private String   _dbName;
		private String   _schemaName;
		private String   _tableName;
		public String    _type;
		public Timestamp _crdate;
		private Timestamp _sampleTime;
		private String   _extraInfoText;  // sys.dm_db_index_physical_stats
		private String   _objectText;     // sp_help tabname
		private String   _childObjects;   // Type:name, Type:name, Type:name
		private String   _triggersText;   // Text of any trigger definitions
		private long     _rowtotal;
//		private int      _reservedKb;
//		private int      _dataKb;
//		private int      _indexKb;
//		private int      _unusedKb;
//		private String   _lockSchema;
		private int      _indexCount;
		private List<SqlServerIndexInfo> _indexList = new ArrayList<>();

		public List<String> _viewReferences; // if _type == "V", this this will hold; table/views this view references

		public ResultSetTableModel _missingIndexInfo;
		
		public SqlServerIndexInfo getIndexInfoForName(String indexName)
		{
			if (StringUtil.isNullOrBlank(indexName))
				return null;

			for (SqlServerIndexInfo e : _indexList)
				if (indexName.equals(e._indexName))
					return e;
			return null;
		}
		
		public String getFullTableName()
		{
			String prefix = "";
			if (StringUtil.hasValue(_dbName))     prefix += _dbName     + ".";
			if (StringUtil.hasValue(_schemaName)) prefix += _schemaName + ".";
			return prefix + _tableName;
		}
		
		public String    getDbName()              { return _dbName; }
		public String    getSchemaName()          { return _schemaName; }
		public String    getTableName()           { return _tableName; }
		public String    getType()                { return _type; }
		public Timestamp getCrDate()              { return _crdate; }
		public Timestamp getSampleTime()          { return _sampleTime; }

//		public long      getCompressedPageCount() { return _compressedPageCount; }
//		public long      getForwardedRowCount()   { return _forwardedRowCount; }
//		public long      getGhostRowCount()       { return _ghostRowCount; }

		public long      getPartitionCount()      { return _partitionCount; }
		public long      getRowTotal()            { return _rowtotal; }
                                                  
//		public long      getSizePages()           { return getDataPages() + getIndexPages() + getLobPages() + getRowOverflowPages(); }
//		public long      getSizeKb()              { return getSizePages() * 8; }
//		public double    getSizeMb()              { return MathUtils.round(getSizeKb() / 1024.0, 1); }
                                                  
		public long      getTotalPages()          { return _totalPages; }
		public long      getTotalKb()             { return _totalPages * 8; }
		public double    getTotalMb()             { return MathUtils.round(getTotalKb() / 1024.0, 1); }
		                                          
		public long      getInRowPages()          { return _inRowPages; }
		public long      getInRowKb()             { return _inRowPages * 8; }
		public double    getInRowMb()             { return MathUtils.round(getInRowKb()     / 1024.0, 1); }
		                                          
		public long      getLobPages()            { return _lobPages; }
		public long      getLobKb()               { return _lobPages * 8; }
		public double    getLobMb()               { return MathUtils.round(getLobKb() / 1024.0, 1); }

		public long      getRowOverflowPages()    { return _rowOverflowPages; }
		public long      getRowOverflowKb()       { return _rowOverflowPages * 8; }
		public double    getRowOverflowMb()       { return MathUtils.round(getRowOverflowKb() / 1024.0, 1); }

		public long      getIndexPages()          { return _indexPages; }
		public long      getIndexKb()             { return _indexPages * 8; }
		public double    getIndexMb()             { return MathUtils.round(getIndexKb()    / 1024.0, 1); }
                                                  
		public int       getIndexCount()          { return _indexCount; }
		public List<SqlServerIndexInfo> getIndexList()  { return _indexList == null ? Collections.emptyList() : _indexList; }

		public boolean      isUserTable()         { return "U".equals(_type); }
		public boolean      isView()              { return "V".equals(_type); }
		public List<String> getViewReferences()   { return _viewReferences; }

		
		/////////////////////////////////////////////////////////////////////////////////
		// hashCode() & equals() so we can use Set/Map and sorting...
		/////////////////////////////////////////////////////////////////////////////////
		@Override
		public int hashCode()
		{
			return Objects.hash(_dbName, _schemaName, _tableName);
		}

		@Override
		public boolean equals(Object obj)
		{
			if ( this == obj ) return true;
			if ( obj == null ) return false;
			if ( getClass() != obj.getClass() ) return false;

			SqlServerTableInfo other = (SqlServerTableInfo) obj;
			return Objects.equals(_dbName    , other._dbName) 
			    && Objects.equals(_schemaName, other._schemaName) 
			    && Objects.equals(_tableName , other._tableName);
		}
	}
}
