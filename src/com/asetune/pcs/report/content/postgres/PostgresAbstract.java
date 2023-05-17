/***************************************R****************************************
 * Copyright (C) 2010-2019 Goran SchwarzUD = 
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
package com.asetune.pcs.report.content.postgres;

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
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.asetune.gui.ResultSetTableModel;
import com.asetune.pcs.report.DailySummaryReportAbstract;
import com.asetune.pcs.report.content.ReportEntryAbstract;
import com.asetune.sql.SqlObjectName;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.DbUtils;
import com.asetune.utils.HtmlTableProducer;
import com.asetune.utils.MathUtils;
import com.asetune.utils.StringUtil;

public abstract class PostgresAbstract
extends ReportEntryAbstract
{
	private static Logger _logger = Logger.getLogger(PostgresAbstract.class);

	public PostgresAbstract(DailySummaryReportAbstract reportingInstance)
	{
		super(reportingInstance);
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
//		Set<PgTableInfo> tableInfoSet = getTableInformationFromMonDdlStorage(conn, tableList);
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
////		String problemDesc = "";
//		Set<String> tableList = SqlParserUtils.getTables(sqlText);
////		List<String> tableList = Collections.emptyList();
////		try { tableList = SqlParserUtils.getTables(sqlText, true); }
////		catch (ParseException pex) { problemDesc = pex + ""; }
//
////		// Get information about ALL tables in list 'tableList' from the DDL Storage
////		Set<PgTableInfo> tableInfoSet = getTableInformationFromMonDdlStorage(conn, tableList);
////		if (tableInfoSet.isEmpty() && StringUtil.isNullOrBlank(problemDesc))
////			problemDesc = "&emsp; &bull; No tables was found in the DDL Storage for tables: " + listToHtmlCode(tableList);
////
////		// And make it into a HTML table with various information about the table and indexes 
////		tableInfo = problemDesc + getTableInfoAsHtmlTable(tableInfoSet, tableList, true, "dsr-sub-table-tableinfo");
//
//		// Get information about ALL tables in list 'tableList' from the DDL Storage
//		tableInfo = getTableInfoAsHtmlTable(conn, tableList, true, "dsr-sub-table-tableinfo");
//		if (StringUtil.isNullOrBlank(tableInfo))
//			tableInfo = "&emsp; &bull; No tables was found in the DDL Storage for tables: " + listToHtmlCode(tableList);
//		
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
	public String getDbmsTableInfoAsHtmlTable(DbxConnection conn, Set<String> tableList, boolean includeIndexInfo, String classname)
	{
		// Get tables/indexes
		Set<PgTableInfo> tableInfoSet = getTableInformationFromMonDdlStorage(conn, tableList);
		if (tableInfoSet.isEmpty())
			return "";

		// And make it into a HTML table with various information about the table and indexes 
		return getTableInfoAsHtmlTable(tableInfoSet, tableList, includeIndexInfo, classname);
	}

	
	/**
	 * 
	 * @param dbname1 
	 * @param dbname2
	 * @return
	 */
	private String markIfDifferent(String dbname1, String dbname2)
	{
		if (StringUtil.isNullOrBlank(dbname1) || StringUtil.isNullOrBlank(dbname2))
			return dbname1;
		
		if (dbname1.equals(dbname2))
			return dbname1;
		
//		return "<mark>" + dbname1 + "</mark>";
		return "<span style='background-color: yellow'>" + dbname1 + "</span>";
	}
	/**
	 * 
	 * @param tableInfoList
	 * @param tableList
	 * @param includeIndexInfo
	 * @param classname
	 * @return
	 */
	public String getTableInfoAsHtmlTable(Set<PgTableInfo> tableInfoList, Set<String> tableList, boolean includeIndexInfo, String classname)
	{
		// Exit early: if no data
		if (tableInfoList == null)   return "";
		if (tableInfoList.isEmpty()) return "";
		
		
		StringBuilder sb = new StringBuilder();

		// Figure out if we are missing any rows in 'tableInfoList' that existed in 'tableList'
		if (tableList != null && tableList.size() > tableInfoList.size())
		{
			for(String tabName : tableList)
			{
				// Remove any "schema" names from the tableName
//				SqlObjectName sqlObj = new SqlObjectName(tabName, DbUtils.DB_PROD_NAME_SYBASE_ASE, "\"", false, true);
//				tabName = sqlObj.getObjectNameOrigin();

				boolean exists = false;
				for(PgTableInfo ti : tableInfoList)
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

		// Used to check when looping if the database is the SAME as the rest!
		String firstEntryDbname = null;
		if ( ! tableInfoList.isEmpty() )
			firstEntryDbname = tableInfoList.iterator().next().getDbName();			


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

		//--------------------------------------------------------------------------
		// Table BODY
		sb.append("<tbody> \n");
		for (PgTableInfo entry : tableInfoList)
		{
			LinkedHashMap<String, String> tableInfoMap = new LinkedHashMap<>();

			if (entry.isView())
			{
				tableInfoMap.put("Type"       ,            "<b>VIEW</b>"              );
				tableInfoMap.put("DBName"     ,            markIfDifferent(entry.getDbName(), firstEntryDbname));
				tableInfoMap.put("Schema"     ,            entry.getSchemaName()      );
				tableInfoMap.put("View"       ,            entry.getTableName()       );
//				tableInfoMap.put("Created"    ,            entry.getCrDate()+""       );
//				tableInfoMap.put("References" ,            entry.getViewReferences()+""); // instead show this in the: Index Info section
				tableInfoMap.put("DDL"        ,            getFormattedSqlAsTooltipDiv(entry._objectText, "View DDL", DbUtils.DB_PROD_NAME_POSTGRES));
			}
			else if (entry.isFunction())
			{
				tableInfoMap.put("Type"       ,            "<b>FUNCTION</b>"          );
				tableInfoMap.put("DBName"     ,            markIfDifferent(entry.getDbName(), firstEntryDbname));
				tableInfoMap.put("Schema"     ,            entry.getSchemaName()      );
				tableInfoMap.put("Function"   ,            entry.getTableName()       );
//				tableInfoMap.put("Created"    ,            entry.getCrDate()+""       );
//				tableInfoMap.put("References" ,            entry.getFunctionReferences()+""); // instead show this in the: Index Info section
				tableInfoMap.put("DDL"        ,            getFormattedSqlAsTooltipDiv(entry._objectText, "Function DDL", DbUtils.DB_PROD_NAME_POSTGRES));
			}
			else if (entry.isProcedure())
			{
				tableInfoMap.put("Type"       ,            "<b>PROCEDURE</b>"          );
				tableInfoMap.put("DBName"     ,            markIfDifferent(entry.getDbName(), firstEntryDbname));
				tableInfoMap.put("Schema"     ,            entry.getSchemaName()      );
				tableInfoMap.put("Function"   ,            entry.getTableName()       );
//				tableInfoMap.put("Created"    ,            entry.getCrDate()+""       );
//				tableInfoMap.put("References" ,            entry.getFunctionReferences()+""); // instead show this in the: Index Info section
				tableInfoMap.put("DDL"        ,            getFormattedSqlAsTooltipDiv(entry._objectText, "Procedure DDL", DbUtils.DB_PROD_NAME_POSTGRES));
			}
			else if (entry.isCursor())
			{
				tableInfoMap.put("Type"       ,            "<b>CURSOR</b>"          );
				tableInfoMap.put("DBName"     ,            markIfDifferent(entry.getDbName(), firstEntryDbname));
//				tableInfoMap.put("Schema"     ,            entry.getSchemaName()      );
//				tableInfoMap.put("Function"   ,            entry.getTableName()       );
				tableInfoMap.put("Created"    ,            entry.getCrDate()+""       );
//				tableInfoMap.put("References" ,            entry.getCursorReferences()+""); // instead show this in the: Index Info section
				tableInfoMap.put("SQL Text"   ,            getFormattedSqlAsTooltipDiv(entry._objectText, "Cursor SQL Text", DbUtils.DB_PROD_NAME_POSTGRES));
			}
			else if (entry.isRemoteTable())
			{
				tableInfoMap.put("Type"       ,            "<b>REMOTE TABLE</b>"      );
				tableInfoMap.put("DBName"     ,            markIfDifferent(entry.getDbName(), firstEntryDbname));
				tableInfoMap.put("Schema"     ,            entry.getSchemaName()      );
				tableInfoMap.put("Table"      ,            entry.getTableName()       );
				tableInfoMap.put("Remote SRV" ,            entry.getRemoteServerName());
				tableInfoMap.put("Remote DB"  ,            entry.getRemoteDBName()    );
				tableInfoMap.put("Remote TBL" ,            entry.getRemoteTableName() );
//				tableInfoMap.put("Created"    ,            entry.getCrDate()+""       );
				tableInfoMap.put("DDL"        ,            getTextAsTooltipDiv(entry._objectText, "Remote Table DDL"));
			}
			else // Any table
			{
				tableInfoMap.put("DBName"     ,             markIfDifferent(entry.getDbName(), firstEntryDbname));
				tableInfoMap.put("Schema"      ,            entry.getSchemaName() );
				tableInfoMap.put("Table"       ,            entry.getTableName()  );
				tableInfoMap.put("Rowcount"    , nf.format( entry.getRowTotal()  ));
				
				if (entry.getPartitionCount() != -1)
					tableInfoMap.put("Partitions"  , nf.format( entry.getPartitionCount()  ));
				
				tableInfoMap.put("Total MB"    , nf.format( entry.getTotalMb()   ));
				tableInfoMap.put("Data MB"     , nf.format( entry.getDataMb()    ));
				tableInfoMap.put("Data Pages"  , nf.format( entry.getDataPages() ));
				tableInfoMap.put("Index MB"    , nf.format( entry.getIndexMb()   ));
				tableInfoMap.put("Toast/LOB MB", entry.getToastMb() == -1 ? "-no-toast-" : nf.format( entry.getToastMb()   ));
//				tableInfoMap.put("Created"     , entry.getCrDate()+""       );
				tableInfoMap.put("Sampled"     , entry.getSampleTime()+""   );
				tableInfoMap.put("Index Count" , entry.getIndexCount() + (entry.getIndexCount() > 0 ? "" : " <b><font color='red'>&lt;&lt;-- Warning NO index</font></b>") );
				tableInfoMap.put("DDL Info"    , getTextAsTooltipDiv(entry._objectText, "Table Info"));
				tableInfoMap.put("Triggers"    , entry._triggersText == null ? "-no-triggers-" : getTextAsTooltipDiv(entry._triggersText, "Trigger Info"));
			}
			
			String tableInfo = HtmlTableProducer.createHtmlTable(tableInfoMap, "dsr-sub-table-other-info", true);
			String indexInfo = "";

			if (entry.isView()) 
			{
				// Build a bullet list of tables referenced by the view
				indexInfo = "";
				if (entry.getViewReferences() != null)
				{
					indexInfo += "Referenced Tables in this View: \n";
					indexInfo += "<ul> \n";
					for (String ref : entry.getViewReferences())
					{
						indexInfo += "<li>" + ref + "</li> \n";
					}
					indexInfo += "</ul> \n";
				}
			}
			else if (entry.isFunction()) 
			{
				// Build a bullet list of tables referenced by the function
				indexInfo = "";
				if (entry.getFunctionReferences() != null)
				{
					indexInfo += "Referenced Tables in this Function: \n";
					indexInfo += "<ul> \n";
					for (String ref : entry.getFunctionReferences())
					{
						indexInfo += "<li>" + ref + "</li> \n";
					}
					indexInfo += "</ul> \n";
				}
			}
			else if (entry.isProcedure()) 
			{
				// Build a bullet list of tables referenced by the PRODECURE
				indexInfo = "";
				if (entry.getFunctionReferences() != null) // NOTE: same as for FUNCTION
				{
					indexInfo += "Referenced Tables in this Procedure: \n";
					indexInfo += "<ul> \n";
					for (String ref : entry.getFunctionReferences()) // NOTE: same as for FUNCTION
					{
						indexInfo += "<li>" + ref + "</li> \n";
					}
					indexInfo += "</ul> \n";
				}
			}
			else if (entry.isCursor()) 
			{
				// Build a bullet list of tables referenced by the CURSOR
				indexInfo = "";
				if (entry.getCursorReferences() != null) 
				{
					indexInfo += "Referenced Tables in this Cursor: \n";
					indexInfo += "<ul> \n";
					for (String ref : entry.getCursorReferences()) // NOTE: same as for FUNCTION
					{
						indexInfo += "<li>" + ref + "</li> \n";
					}
					indexInfo += "</ul> \n";
				}
			}
			else if (entry.isRemoteTable()) 
			{
				indexInfo = "";
				if (entry.getRemoteTableOptionsMap() != null)
				{
					indexInfo += "Remote Table Options: \n";;
					indexInfo += "<table> \n";
					indexInfo += "  <thead> \n";
					indexInfo += "    <tr> <th>Option</th> <th>Value</th> </tr> \n";
					indexInfo += "  </thead> \n";
					indexInfo += "  <tbody> \n";
					for (Entry<String, String> opt : entry.getRemoteTableOptionsMap().entrySet())
					{
						indexInfo += "    <tr> <td>" + opt.getKey() + "</td><td>" + opt.getValue() + "</td></tr> \n";
					}
					indexInfo += "  </tbody> \n";
					indexInfo += "</table> \n";
					indexInfo += "<br> \n";
				}
				if (entry.getRemoteServerOptionsMap() != null)
				{
					indexInfo += "Remote Server Options: \n";;
					indexInfo += "<table> \n";
					indexInfo += "  <thead> \n";
					indexInfo += "    <tr> <th>Option</th> <th>Value</th> </tr> \n";
					indexInfo += "  </thead> \n";
					indexInfo += "  <tbody> \n";
					for (Entry<String, String> opt : entry.getRemoteServerOptionsMap().entrySet())
					{
						indexInfo += "    <tr> <td>" + opt.getKey() + "</td><td>" + opt.getValue() + "</td></tr> \n";
					}
					indexInfo += "  </tbody> \n";
					indexInfo += "</table> \n";
				}
			}
			else
			{
				// Get index information in a separate table
				indexInfo = getIndexInfoAsHtmlTable(entry, classname);
			}
			
			
			sb.append("<tr> \n");
			sb.append("  <td>").append( tableInfo ).append("</td> \n");

			if (includeIndexInfo)
				sb.append("  <td>").append( indexInfo ).append("</td> \n");

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

	public String getIndexInfoAsHtmlTable(PgTableInfo tableEntry, String classname)
	{
		// Exit early: if no data
		if (tableEntry == null)   return "";

		// Get the index list
		List<PgIndexInfo> indexInfoList = tableEntry.getIndexList();

		// Exit early: if no data
//		if (indexInfoList == null)   return "";
//		if (indexInfoList.isEmpty()) return "";
		
		StringBuilder sb = new StringBuilder();

		String className = "";
		if (StringUtil.hasValue(classname))
			className = " class='" + classname + "'";

		// Create a NumberFormatter for better Number printing
		NumberFormat nf = NumberFormat.getInstance();

		// Print Index Information
		if (indexInfoList != null && !indexInfoList.isEmpty())
		{
			sb.append("<table" + className + "> \n");

			//--------------------------------------------------------------------------
			// Table Header
			sb.append("<thead> \n");
			sb.append("<tr> \n");

			sb.append(createHtmlTh("Index Name"           , ""));
			sb.append(createHtmlTh("Keys"                 , ""));
			sb.append(createHtmlTh("Include"              , "Columns included on the leafe index page... create index... on(c1,c2) INCLUDE(c3,c4,c5)"));
			sb.append(createHtmlTh("Desciption"           , ""));
			sb.append(createHtmlTh("RowCount"             , ""));
			sb.append(createHtmlTh("Size Pages"           , ""));
			sb.append(createHtmlTh("Size MB"              , ""));
//			sb.append(createHtmlTh("Avg RowsPerPage"      , "Average Rows Per Page\nCalculted by: RowCount/Pages"));
			sb.append(createHtmlTh("Avg RowSize"          , "Average Bytes used per Row\nCalculated by: Pages*8192/RowCount"));

			sb.append(createHtmlTh("Index Scans"          , ""));
			sb.append(createHtmlTh("Rows Fetched"         , ""));
			sb.append(createHtmlTh("Rows Read"            , ""));
			sb.append(createHtmlTh("Fetch/Read Efficiency", ""));

			sb.append(createHtmlTh("Total Reads"          , ""));
			sb.append(createHtmlTh("Physical Reads"       , ""));
			sb.append(createHtmlTh("Cache Hit Percent"    , ""));

			sb.append(createHtmlTh("Table Space"          , ""));
			sb.append(createHtmlTh("DDL"                  , ""));

			sb.append("</tr> \n");
			sb.append("</thead> \n");

			//--------------------------------------------------------------------------
			// Table BODY
			sb.append("<tbody> \n");
			for (PgIndexInfo entry : indexInfoList)
			{
				long avgBytesPerRow = 0;
				if (entry.getRowCount() > 0)
					avgBytesPerRow = entry.getSizePages() * 8192L / entry.getRowCount();

				sb.append("<tr> \n");
				sb.append("  <td>").append(            entry.getIndexName()          ).append("</td> \n");
				sb.append("  <td>").append(            entry.getKeysStr()            ).append("</td> \n");
				sb.append("  <td>").append(            entry.getIncludeStr()         ).append("</td> \n");
				sb.append("  <td>").append(            entry.getDescription()        ).append("</td> \n");
				sb.append("  <td>").append( nf.format( entry.getRowCount()          )).append("</td> \n");
				sb.append("  <td>").append( nf.format( entry.getSizePages()         )).append("</td> \n");
				sb.append("  <td>").append( nf.format( entry.getSizeMb()            )).append("</td> \n");
				sb.append("  <td>").append( nf.format( avgBytesPerRow               )).append("</td> \n");

				sb.append("  <td>").append( diffAbsValues(entry._idx_scan                 , entry._abs_idx_scan                  )).append("</td> \n");
				sb.append("  <td>").append( diffAbsValues(entry._idx_tup_fetch            , entry._abs_idx_tup_fetch             )).append("</td> \n");
				sb.append("  <td>").append( diffAbsValues(entry._idx_tup_read             , entry._abs_idx_tup_read              )).append("</td> \n");
				sb.append("  <td>").append( diffAbsValues(entry._idx_fetch_read_efficiency, entry._abs_idx_fetch_read_efficiency )).append("</td> \n");

				sb.append("  <td>").append( diffAbsValues(entry._idx_total_read           , entry._abs_idx_total_read            )).append("</td> \n");
				sb.append("  <td>").append( diffAbsValues(entry._idx_physical_read        , entry._abs_idx_physical_read         )).append("</td> \n");
				sb.append("  <td>").append( diffAbsValues(entry._idx_cacheHitPct          , entry._abs_idx_cacheHitPct           )).append("</td> \n");
				
				sb.append("  <td>").append( entry.getTableSpace() == null ? "default" : entry.getTableSpace() ).append("</td> \n");
				sb.append("  <td>").append(            entry.getDdlText()            ).append("</td> \n");
				sb.append("</tr> \n");
			}
			sb.append("</tbody> \n");
			
			//--------------------------------------------------------------------------
			// Table Footer
//			sb.append("<tfoot> \n");
//			sb.append("<tr> \n");
	//
//			sb.append("  <td></td> \n"); // first column is empty, which is the "row name"
	//
//			sb.append("</tr> \n");
//			sb.append("</tfoot> \n");

			sb.append("</table> \n");
		}
		else
		{
			sb.append("<b>No Index(es) was found</b><br> \n");
		}
		

		
		//#######################################################################################
		// Make Extra small table (with 1 row DIFF, 1 row ABS) for TABLE DATA
		//#######################################################################################
		sb.append("<br> \n");
		sb.append("<b>Base Table information:</b><br> \n");
		sb.append("<table" + className + "> \n");

		//--------------------------------------------------------------------------
		// Table Header
		sb.append("<thead> \n");
		sb.append("<tr> \n");
		sb.append( createHtmlTh("Type"                     ,"DIFF=Difference Calculated Values. Counter changes in this reporting period.  \nABS=Absolute Values. Counters since last reboot, or reset!") );
		sb.append( createHtmlTh("seq_scan"                 ,"Number of sequential scans initiated on this table"                              ) );
		sb.append( createHtmlTh("seq_tup_read"             ,"Number of live rows fetched by sequential scans"                                 ) );
		sb.append( createHtmlTh("idx_scan"                 ,"Number of index scans initiated on this table"                                   ) );
		sb.append( createHtmlTh("idx_tup_fetch"            ,"Number of live rows fetched by index scans"                                      ) );
		sb.append( createHtmlTh("n_tup_crud"               ,"CRUD = CRreate Update Delete, so a summary of: n_tup_ins, n_tup_upd, n_tup_del"  ) );
		sb.append( createHtmlTh("n_tup_ins"                ,"Number of rows inserted"                                                         ) );
		sb.append( createHtmlTh("n_tup_upd"                ,"Number of rows updated"                                                          ) );
		sb.append( createHtmlTh("n_tup_hot_upd"            ,"Number of rows deleted"                                                          ) );
		sb.append( createHtmlTh("n_tup_del"                ,"Number of rows HOT updated (i.e., with no separate index update required)"       ) );
		sb.append( createHtmlTh("n_live_tup"               ,"Estimated number of live rows"                                                   ) );
		sb.append( createHtmlTh("n_dead_tup"               ,"Estimated number of dead rows"                                                   ) );
		sb.append( createHtmlTh("n_mod_since_analyze"      ,"Estimated number of rows modified since this table was last analyzed"            ) );
		sb.append( createHtmlTh("last_vacuum"              ,"Last time at which this table was manually vacuumed (not counting VACUUM FULL)"  ) );
		sb.append( createHtmlTh("last_autovacuum"          ,"Last time at which this table was vacuumed by the autovacuum daemon"             ) );
		sb.append( createHtmlTh("last_analyze"             ,"Last time at which this table was manually analyzed"                             ) );
		sb.append( createHtmlTh("last_autoanalyze"         ,"Last time at which this table was analyzed by the autovacuum daemon"             ) );
		sb.append( createHtmlTh("vacuum_count"             ,"Number of times this table has been manually vacuumed (not counting VACUUM FULL)") );
		sb.append( createHtmlTh("autovacuum_count"         ,"Number of times this table has been vacuumed by the autovacuum daemon"           ) );
		sb.append( createHtmlTh("analyze_count"            ,"Number of times this table has been manually analyzed"                           ) );
		sb.append( createHtmlTh("autoanalyze_count"        ,"Number of times this table has been analyzed by the autovacuum daemon"           ) );

		sb.append( createHtmlTh("heap_total_read"          ,"Total HEAP/DATA pages reads for this table... Calculated (heap_blks_hit + heap_blks_hit)") );
		sb.append( createHtmlTh("heap_physical_read"       ,"Number of disk blocks (HEAP/DATA pages) read from this table. real column name is: heap_blks_read") );
		sb.append( createHtmlTh("heap_cacheHitPct"         ,"Cache hit percent for HEAP pages. Calculated as: heap_blks_hit / (heap_blks_read + heap_blks_hit) * 100") );

		sb.append( createHtmlTh("idx_total_read"           ,"Total INDEX pages reads for this table... Calculated (idx_blks_hit + idx_blks_hit)") );
		sb.append( createHtmlTh("idx_physical_read"        ,"Number of disk blocks (INDEX pages) read from this table. real column name is: idx_blks_read") );
		sb.append( createHtmlTh("idx_cacheHitPct"          ,"Cache hit percent for INDEX pages. Calculated as: idx_blks_hit / (idx_blks_read + idx_blks_hit) * 100") );

		sb.append( createHtmlTh("toast_total_read"         ,"Total TOAST pages reads for this table... Calculated (toast_blks_hit + toast_blks_hit)") );
		sb.append( createHtmlTh("toast_physical_read"      ,"Number of disk blocks (TOAST pages) read from this table. real column name is: toast_blks_read") );
		sb.append( createHtmlTh("toast_cacheHitPct"        ,"Cache hit percent for TOAST pages. Calculated as: toast_blks_hit / (toast_blks_read + toast_blks_hit) * 100") );

		sb.append( createHtmlTh("toast_idx_total_read"     ,"Total TOAST INDEX pages reads for this table... Calculated (tidx_blks_hit + tidx_blks_hit)") );
		sb.append( createHtmlTh("toast_idx_physical_read"  ,"Number of disk blocks (TOAST INDEX pages) read from this table. real column name is: tidx_blks_read") );
		sb.append( createHtmlTh("toast_idx_cacheHitPct"    ,"Cache hit percent for TOAST INDEX pages. Calculated as: tidx_blks_hit / (tidx_blks_read + tidx_blks_hit) * 100") );
		sb.append("</tr> \n");
		sb.append("</thead> \n");

		//--------------------------------------------------------------------------
		// Table BODY
		sb.append("<tbody> \n");

		// ------------- DIFF -----------------------
		sb.append("<tr> \n");
		sb.append("  <td><b>DIFF</b></td> \n");
		sb.append("  <td><b>").append( nf.format( tableEntry._seq_scan                )).append("</b></td> \n");
		sb.append("  <td><b>").append( nf.format( tableEntry._seq_tup_read            )).append("</b></td> \n");

		sb.append("  <td><b>").append( nf.format( tableEntry._idx_scan                )).append("</b></td> \n");
		sb.append("  <td><b>").append( nf.format( tableEntry._idx_tup_fetch           )).append("</b></td> \n");

		sb.append("  <td><b>").append( nf.format( tableEntry._n_tup_crud              )).append("</b></td> \n");
		sb.append("  <td><b>").append( nf.format( tableEntry._n_tup_ins               )).append("</b></td> \n");
		sb.append("  <td><b>").append( nf.format( tableEntry._n_tup_upd               )).append("</b></td> \n");
		sb.append("  <td><b>").append( nf.format( tableEntry._n_tup_hot_upd           )).append("</b></td> \n");
		sb.append("  <td><b>").append( nf.format( tableEntry._n_tup_del               )).append("</b></td> \n");

		sb.append("  <td><b>").append( "-" /*nf.format( tableEntry._n_live_tup        )*/  ).append("</b></td> \n");
		sb.append("  <td><b>").append( "-" /*nf.format( tableEntry._n_dead_tup        )*/  ).append("</b></td> \n");

		sb.append("  <td><b>").append( "-" /*nf.format( tableEntry._n_mod_since_analyze )*/).append("</b></td> \n");

		sb.append("  <td><b>").append( "-"     /* tableEntry._last_vacuum      */      ).append("</b></td> \n");
		sb.append("  <td><b>").append( "-"     /* tableEntry._last_autovacuum  */      ).append("</b></td> \n");
		sb.append("  <td><b>").append( "-"     /* tableEntry._last_analyze     */      ).append("</b></td> \n");
		sb.append("  <td><b>").append( "-"     /* tableEntry._last_autoanalyze */      ).append("</b></td> \n");

		sb.append("  <td><b>").append( nf.format( tableEntry._vacuum_count            )).append("</b></td> \n");
		sb.append("  <td><b>").append( nf.format( tableEntry._autovacuum_count        )).append("</b></td> \n");
		sb.append("  <td><b>").append( nf.format( tableEntry._analyze_count           )).append("</b></td> \n");
		sb.append("  <td><b>").append( nf.format( tableEntry._autoanalyze_count       )).append("</b></td> \n");


		sb.append("  <td><b>").append( nf.format( tableEntry._heap_total_read         )).append("</b></td> \n");
		sb.append("  <td><b>").append( nf.format( tableEntry._heap_physical_read      )).append("</b></td> \n");
		sb.append("  <td><b>").append( nf.format( tableEntry._heap_cacheHitPct        )).append("</b></td> \n");

		sb.append("  <td><b>").append( nf.format( tableEntry._idx_total_read          )).append("</b></td> \n");
		sb.append("  <td><b>").append( nf.format( tableEntry._idx_physical_read       )).append("</b></td> \n");
		sb.append("  <td><b>").append( nf.format( tableEntry._idx_cacheHitPct         )).append("</b></td> \n");

		sb.append("  <td><b>").append( nf.format( tableEntry._toast_total_read        )).append("</b></td> \n");
		sb.append("  <td><b>").append( nf.format( tableEntry._toast_physical_read     )).append("</b></td> \n");
		sb.append("  <td><b>").append( nf.format( tableEntry._toast_cacheHitPct       )).append("</b></td> \n");

		sb.append("  <td><b>").append( nf.format( tableEntry._toast_idx_total_read    )).append("</b></td> \n");
		sb.append("  <td><b>").append( nf.format( tableEntry._toast_idx_physical_read )).append("</b></td> \n");
		sb.append("  <td><b>").append( nf.format( tableEntry._toast_idx_cacheHitPct   )).append("</b></td> \n");
		sb.append("</tr> \n");

		// ------------- ABS -----------------------
		sb.append("<tr> \n");
		sb.append("  <td><i>ABS</i></td> \n");
		sb.append("  <td><i>").append( nf.format( tableEntry._abs_seq_scan                )).append("</i></td> \n");
		sb.append("  <td><i>").append( nf.format( tableEntry._abs_seq_tup_read            )).append("</i></td> \n");

		sb.append("  <td><i>").append( nf.format( tableEntry._abs_idx_scan                )).append("</i></td> \n");
		sb.append("  <td><i>").append( nf.format( tableEntry._abs_idx_tup_fetch           )).append("</i></td> \n");

		sb.append("  <td><i>").append( nf.format( tableEntry._abs_n_tup_crud              )).append("</i></td> \n");
		sb.append("  <td><i>").append( nf.format( tableEntry._abs_n_tup_ins               )).append("</i></td> \n");
		sb.append("  <td><i>").append( nf.format( tableEntry._abs_n_tup_upd               )).append("</i></td> \n");
		sb.append("  <td><i>").append( nf.format( tableEntry._abs_n_tup_hot_upd           )).append("</i></td> \n");
		sb.append("  <td><i>").append( nf.format( tableEntry._abs_n_tup_del               )).append("</i></td> \n");

		sb.append("  <td><i>").append( nf.format( tableEntry._abs_n_live_tup              )).append("</i></td> \n");
		sb.append("  <td><i>").append( nf.format( tableEntry._abs_n_dead_tup              )).append("</i></td> \n");

		sb.append("  <td><i>").append( nf.format( tableEntry._abs_n_mod_since_analyze     )).append("</i></td> \n");

		sb.append("  <td><i>").append(            tableEntry._abs_last_vacuum              ).append("</i></td> \n");
		sb.append("  <td><i>").append(            tableEntry._abs_last_autovacuum          ).append("</i></td> \n");
		sb.append("  <td><i>").append(            tableEntry._abs_last_analyze             ).append("</i></td> \n");
		sb.append("  <td><i>").append(            tableEntry._abs_last_autoanalyze         ).append("</i></td> \n");

		sb.append("  <td><i>").append( nf.format( tableEntry._abs_vacuum_count            )).append("</i></td> \n");
		sb.append("  <td><i>").append( nf.format( tableEntry._abs_autovacuum_count        )).append("</i></td> \n");
		sb.append("  <td><i>").append( nf.format( tableEntry._abs_analyze_count           )).append("</i></td> \n");
		sb.append("  <td><i>").append( nf.format( tableEntry._abs_autoanalyze_count       )).append("</i></td> \n");


		sb.append("  <td><i>").append( nf.format( tableEntry._abs_heap_total_read         )).append("</i></td> \n");
		sb.append("  <td><i>").append( nf.format( tableEntry._abs_heap_physical_read      )).append("</i></td> \n");
		sb.append("  <td><i>").append( nf.format( tableEntry._abs_heap_cacheHitPct        )).append("</i></td> \n");

		sb.append("  <td><i>").append( nf.format( tableEntry._abs_idx_total_read          )).append("</i></td> \n");
		sb.append("  <td><i>").append( nf.format( tableEntry._abs_idx_physical_read       )).append("</i></td> \n");
		sb.append("  <td><i>").append( nf.format( tableEntry._abs_idx_cacheHitPct         )).append("</i></td> \n");

		sb.append("  <td><i>").append( nf.format( tableEntry._abs_toast_total_read        )).append("</i></td> \n");
		sb.append("  <td><i>").append( nf.format( tableEntry._abs_toast_physical_read     )).append("</i></td> \n");
		sb.append("  <td><i>").append( nf.format( tableEntry._abs_toast_cacheHitPct       )).append("</i></td> \n");

		sb.append("  <td><i>").append( nf.format( tableEntry._abs_toast_idx_total_read    )).append("</i></td> \n");
		sb.append("  <td><i>").append( nf.format( tableEntry._abs_toast_idx_physical_read )).append("</i></td> \n");
		sb.append("  <td><i>").append( nf.format( tableEntry._abs_toast_idx_cacheHitPct   )).append("</i></td> \n");
		sb.append("</tr> \n");

		sb.append("</tbody> \n");
		
		sb.append("</table> \n");

		
		return sb.toString();
	}

	/**
	 * FIXME
	 */
	public Set<PgTableInfo> getTableInformationFromMonDdlStorage(DbxConnection conn, Set<String> tableList)
	{
		// Exit early if nothing todo
		if (tableList == null)   return Collections.emptySet();
		if (tableList.isEmpty()) return Collections.emptySet();

		// Put all the results back in one list
		Set<PgTableInfo> combinedReturnList = new LinkedHashSet<>();
		
		for (String tableName : tableList)
		{
			try
			{
				Set<PgTableInfo> tmp = getTableInformationFromMonDdlStorage(conn, null, null, tableName);
				
				if (tmp != null && !tmp.isEmpty())
					combinedReturnList.addAll(tmp);
			}
			catch(SQLException ignore) 
			{
				// this is already logged in getTableInformationFromMonDdlStorage(...)
				//_logger.error("Problems reading from DDL Storage.");
			}
		}
		return combinedReturnList;
	}
	
	/**
	 * Get a list of PgTableInfo (basically sp_spaceused) 
	 * 
	 * @param conn       The connection to the PCS
	 * @param dbname     Name of the database the object is in. (can be "" or null; or contains '%' SQL wild-card)
	 * @param owner      Name of the schema   the object is in. (can be "" or null; or contains '%' SQL wild-card)
	 * @param table      Name of the table                      (can be "" or null; or contains '%' SQL wild-card), can also contain DBName.schema which will override the parameters dbname and owner
	 * 
	 * @return A list of found entries, each as an entry of PgTableInfo.
	 * 
	 * @throws SQLException In case of issues.
	 */
	public Set<PgTableInfo> getTableInformationFromMonDdlStorage(DbxConnection conn, String dbname, String owner, String table)
	throws SQLException
	{
		Set<PgTableInfo> result = new LinkedHashSet<>();

		// First level of (potential recursion)
		getTableInformationFromMonDdlStorage(conn, dbname, owner, table, 0, result);
		
		// Should we remove any VIEWS ?
		boolean removeViews = false;
		if (removeViews)
		{
			Set<PgTableInfo> tmp = new LinkedHashSet<>();
			for (PgTableInfo ti : result)
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
	private void getTableInformationFromMonDdlStorage(DbxConnection conn, String dbname, String owner, String table, int recursCallCount, Set<PgTableInfo> result)
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
		SqlObjectName sqlObj = new SqlObjectName(table, DbUtils.DB_PROD_NAME_POSTGRES, "\"", false, true, true);
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
			    + "select [dbname], [owner], [objectName], [type], [crdate], [sampleTime], [extraInfoText], [objectText], [dependList] \n"
			    + "from [MonDdlStorage] \n"
			    + "where 1 = 1 \n"
		//	    + "  and [type] in ('r', 'v', 'f', 'FN', 'P') \n" // This isn't really needed, since we check for "type" in the "read loop"
			         // r  = Relation (table)
			         // v  = View
			         // f  = Foreign Table (External Table)
			         // FN = FuNction (possibly Table Valued Function)
			         // P  = Procedure
			         // NOT NOW: Possibly also add 'm' Materialized view
			    + and_dbname
			    + and_owner
			    + and_table
			    + "";
		
		List<PgTableInfo> tmpList = new ArrayList<>();
		
		sql = conn.quotifySqlString(sql);
		try ( Statement stmnt = conn.createStatement() )
		{
			// Unlimited execution time
			stmnt.setQueryTimeout(0);
			try ( ResultSet rs = stmnt.executeQuery(sql) )
			{
				while(rs.next())
				{
					PgTableInfo ti = new PgTableInfo();
					
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

		// Loop the record we just found in MonDdlStorage
		for (PgTableInfo ti : tmpList)
		{
			// If the TableInformation has already been added, just grab next one.
			if (result.contains(ti))
				continue;

			//--------------------------------------------------------------
			// For views/functions, go and get referenced tables (stored in table 'MonDdlStorage', column 'extraInfoText' (with prefix 'TableList: t1, t2, t3'
			// Then read table information for those tables.
			//--------------------------------------------------------------
			if (ti.isView() || ti.isFunction() || ti.isProcedure() || ti.isCursor())
			{
//System.out.println("--------------- REPORT(type='"+ti._type+"'): ti._extraInfoText=" + ti._extraInfoText);
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
							List<String> references = StringUtil.parseCommaStrToList(row, true);
							
							// What "tables" do this view reference
							if (ti.isView())
							{
								ti._viewReferences = references;
							}
							else if (ti.isFunction())
							{
								ti._functionReferences = references;
							}
							else if (ti.isProcedure())
							{
								ti._functionReferences = references; // NOTE: same as for FUNCTION
							}
							else if (ti.isCursor())
							{
								ti._cursorReferences = references;
							}
//System.out.println("--------------- xxxxxxxxxx REPORT(type='"+ti._type+"'): references=" + references);

							//System.out.println("getTableInformationFromMonDdlStorage(recursCallCount="+recursCallCount+"): ti='"+ti.getFullTableName()+"', is a VIEW or FUNCTION, the following references will also be fetched: " + references);

							// Loop the list
							for (String tableName : references)
							{
								recursCallCount++;

								// Just call "this" method recursively... and the table lookups will be done
								getTableInformationFromMonDdlStorage(conn, dbname, null, tableName, recursCallCount, result);
							}
						}
					}
				}
			} // end: VIEW

			//--------------------------------------------------------------
			// For Remote (Foreign Data Wrapper) Tables get **VARIOUS** information
			//--------------------------------------------------------------
			else if (ti.isRemoteTable())
			{
//				if (StringUtil.hasValue(ti._extraInfoText))
				if (StringUtil.hasValue(ti._objectText))
				{
					// Add to the result
					result.add(ti);
					
					// Get values for: getRemoteServerName() & getRemoteServerOptions()
					getRemoteTableInfo(conn, ti);
				}
			} // end: REMOTE Table

			
			//--------------------------------------------------------------
			// For User Tables get **VARIOUS** information
			//--------------------------------------------------------------
			else if (ti.isUserTable())
			{
//				if (StringUtil.hasValue(ti._objectText) && StringUtil.hasValue(ti._extraInfoText))
				if (StringUtil.hasValue(ti._extraInfoText))
				{
					if (getTableAndIndexInfo(conn, ti))
					{
						// Add to the result
						result.add(ti);

						// Append any more information?
//						TODO; get pg_stat_***;
//						- also search and fix all "TODO-GORAN-FIX"
//						- formatted SQL under a "tooltip popup"

						//-------------------------------------------------------------------------------------------
						// also get Statistics how often the indexes was used/accessed during the report period
						// This is "optional", if the Counter Model Tables are not there, the info wont be filled in.
						//-------------------------------------------------------------------------------------------

						// get optimizer info: CmPgTables  -- pg_stat_user_tables 
						//  - seq_scan            -- Number of sequential scans initiated on this table
						//  - seq_tup_read        -- Number of live rows fetched by sequential scans
						//  - idx_scan            -- Number of index scans initiated on this table
						//  - idx_tup_fetch       -- Number of live rows fetched by index scans
						//  - n_tup_ins           -- Number of rows inserted
						//  - n_tup_upd           -- Number of rows updated (includes HOT updated rows)
						//  - n_tup_del           -- Number of rows deleted
						//  - n_tup_hot_upd       -- Number of rows HOT updated (i.e., with no separate index update required)
						//  - n_live_tup          -- Estimated number of live rows
						//  - n_dead_tup          -- Estimated number of dead rows
						//  - n_mod_since_analyze -- Estimated number of rows modified since this table was last analyzed
						//  - last_vacuum	      -- Last time at which this table was manually vacuumed (not counting VACUUM FULL)
						//  - last_autovacuum     -- Last time at which this table was vacuumed by the autovacuum daemon 
						//  - last_analyze        -- Last time at which this table was manually analyzed 
						//  - last_autoanalyze    -- Last time at which this table was analyzed by the autovacuum daemon 
						//  - vacuum_count        -- Number of times this table has been manually vacuumed (not counting VACUUM FULL) 
						//  - autovacuum_count    -- Number of times this table has been vacuumed by the autovacuum daemon 
						//  - analyze_count       -- Number of times this table has been manually analyzed 
						//  - autoanalyze_count   -- Number of times this table has been analyzed by the autovacuum daemon 
						getPgTables(conn, ti);

						// get optimizer info: CmPgTablesIo  -- pg_statio_user_tables
						//  - heap_blks_read  -- Number of disk blocks read from this table
						//  - heap_blks_hit   -- Number of buffer hits in this table
						//  - idx_blks_read   -- Number of disk blocks read from all indexes on this table
						//  - idx_blks_hit    -- Number of buffer hits in all indexes on this table
						//  - toast_blks_read -- Number of disk blocks read from this table's TOAST table (if any)
						//  - toast_blks_hit  -- Number of buffer hits in this table's TOAST table (if any)
						//  - tidx_blks_read  -- Number of disk blocks read from this table's TOAST table indexes (if any)
						//  - tidx_blks_hit   -- Number of buffer hits in this table's TOAST table indexes (if any)
						getPgTablesIo(conn, ti);

						// get execution info: CmPgIndexes -- pg_stat_user_indexes 
						//  - idx_scan      -- Number of index scans initiated on this index
						//  - idx_tup_read  -- Number of index entries returned by scans on this index
						//  - idx_tup_fetch -- Number of live table rows fetched by simple index scans using this index
						getPgIndexes(conn, ti);

						// get execution info: CmPgIndexesIo -- pg_statio_user_indexes
						//  - idx_blks_read -- Number of disk blocks read from this index
						//  - idx_blks_hit  -- Number of buffer hits in this index
						getPgIndexesIo(conn, ti);
						
						// get TRIGGER information for this table
						getTriggerInfo(conn, ti);
					}

				} // end: has: _objectText && _extraInfoText 
				
			} // end: USER Table
			else
			{
				_logger.warn("Unhandled type='" + ti.getType() + "' when reading results from PCS table 'MonDdlStorage'. ti=" + ti);
			}

		} // end: loop AseTableInfo
	}

	/**
	 * Parse the information from the DdlStorage columns 'objectText' and 'extraInfoText' where various information is stored in.
	 * @param ti
	 * @return
	 */
	private boolean getRemoteTableInfo(DbxConnection conn, PgTableInfo ti)
	{
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		// Below is how column 'objectText' typically looks like
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//   +----------------+-----------------+---------------------------+------------------------+-----------------+-------------+------------------+--------------+-----------+
		//   |ordinal_position|column_name      |data_type                  |character_maximum_length|numeric_precision|numeric_scale|datetime_precision|column_default|is_nullable|
		//   +----------------+-----------------+---------------------------+------------------------+-----------------+-------------+------------------+--------------+-----------+
		//   |               1|uuid             |character varying          |                      50|(NULL)           |(NULL)       |(NULL)            |(NULL)        |NO         |
		//   |               2|personnummer     |character varying          |                      50|(NULL)           |(NULL)       |(NULL)            |(NULL)        |YES        |
		//   |               3|foretagsid       |character varying          |                      50|(NULL)           |(NULL)       |(NULL)            |(NULL)        |NO         |
		//   |               4|forsakringsnummer|character varying          |                      50|(NULL)           |(NULL)       |(NULL)            |(NULL)        |YES        |
		//   |               5|beskrivning      |character varying          |                     200|(NULL)           |(NULL)       |(NULL)            |(NULL)        |YES        |
		//   |               6|typ              |character varying          |                      40|(NULL)           |(NULL)       |(NULL)            |(NULL)        |YES        |
		//   |               7|active           |boolean                    |(NULL)                  |(NULL)           |(NULL)       |(NULL)            |true          |NO         |
		//   |               8|uppdaterad       |timestamp without time zone|(NULL)                  |(NULL)           |(NULL)       |                 6|(NULL)        |YES        |
		//   |               9|skapad           |timestamp without time zone|(NULL)                  |(NULL)           |(NULL)       |                 6|(NULL)        |NO         |
		//   |              10|skapadav         |character varying          |                      50|(NULL)           |(NULL)       |(NULL)            |(NULL)        |YES        |
		//   |              11|atgardad         |boolean                    |(NULL)                  |(NULL)           |(NULL)       |(NULL)            |false         |NO         |
		//   |              12|atgardadav       |character varying          |                      50|(NULL)           |(NULL)       |(NULL)            |(NULL)        |YES        |
		//   |              13|atgardadtid      |timestamp without time zone|(NULL)                  |(NULL)           |(NULL)       |                 6|(NULL)        |YES        |
		//   |              14|dold             |boolean                    |(NULL)                  |(NULL)           |(NULL)       |(NULL)            |false         |NO         |
		//   |              15|doldav           |character varying          |                      50|(NULL)           |(NULL)       |(NULL)            |(NULL)        |YES        |
		//   |              16|doldtid          |timestamp without time zone|(NULL)                  |(NULL)           |(NULL)       |                 6|(NULL)        |YES        |
		//   +----------------+-----------------+---------------------------+------------------------+-----------------+-------------+------------------+--------------+-----------+

		//  +---------------------+--------------------+------------------+-----------+------------+
		//  |foreign_table_catalog|foreign_table_schema|foreign_table_name|option_name|option_value|
		//  +---------------------+--------------------+------------------+-----------+------------+
		//  |gorans_db2           |foreign_data        |t1                |schema_name|public      |
		//  |gorans_db2           |foreign_data        |t1                |table_name |t1          |
		//  +---------------------+--------------------+------------------+-----------+------------+
		
		//  +--------------------+-------------------------+-------------------+----------------------+------------------------+
		//  |foreign_server_name |foreign_data_wrapper_name|foreign_server_type|foreign_server_version|authorization_identifier|
		//  +--------------------+-------------------------+-------------------+----------------------+------------------------+
		//  |pg-1a-gs__gorans_db1|postgres_fdw             |(NULL)             |(NULL)                |postgres                |
		//  +--------------------+-------------------------+-------------------+----------------------+------------------------+

		//  +--------------------+-----------+------------+
		//  |foreign_server_name |option_name|option_value|
		//  +--------------------+-----------+------------+
		//  |pg-1a-gs__gorans_db1|host       |127.0.0.1   |
		//  |pg-1a-gs__gorans_db1|port       |5432        |
		//  |pg-1a-gs__gorans_db1|dbname     |gorans_db1  |
		//  +--------------------+-----------+------------+
		
		
//		if (StringUtil.hasValue(ti._extraInfoText))
		if (StringUtil.hasValue(ti._objectText))
		{
			List<ResultSetTableModel> extraInfoRstmList = ResultSetTableModel.parseTextTables(ti._objectText);

			ResultSetTableModel foreignTableOptions = ResultSetTableModel.getTableWithColumnNames(extraInfoRstmList, true, "foreign_table_catalog", "foreign_table_schema", "foreign_table_name", "option_name", "option_value");
//			ResultSetTableModel remoteServerInfo    = ResultSetTableModel.getTableWithColumnNames(extraInfoRstmList, true, "foreign_server_name", "foreign_data_wrapper_name", "foreign_server_type", "foreign_server_version", "authorization_identifier");
			ResultSetTableModel remoteServerOptions = ResultSetTableModel.getTableWithColumnNames(extraInfoRstmList, true, "foreign_server_name", "option_name", "option_value");

//			// REMOTE SERVER INFO
//			if (remoteServerInfo != null && !remoteServerInfo.isEmpty())
//			{
//				remoteServerInfo.setHandleColumnNotFoundAsNullValueInGetValues(true);
//				
//				int r = 0;
//
//				ti._rowtotal            = tableSizeInfo.getValueAsLong   (r, "row_estimate"                , true, -1L);
//				ti._partitionCount      = tableSizeInfo.getValueAsInteger(r, "partition_count"             , true, -1);
//				ti._object_id           = tableSizeInfo.getValueAsLong   (r, "oid"                         , true, -1L);
//
//				ti._totalMb             = tableSizeInfo.getValueAsDouble (r, "total_size_mb"               , true, -1d);
//				ti._dataMb              = tableSizeInfo.getValueAsDouble (r, "table_size_mb"               , true, -1d);
//				ti._dataPages           = tableSizeInfo.getValueAsLong   (r, "table_size_pgs"              , true, -1L);
//				ti._indexMb             = tableSizeInfo.getValueAsDouble (r, "index_size_mb"               , true, -1d);
//				ti._toastMb             = tableSizeInfo.getValueAsDouble (r, "toast_size_mb"               , true, -1d);
//			}

			// REMOTE TABLE OPTIONS
			if (foreignTableOptions != null && !foreignTableOptions.isEmpty())
			{
				foreignTableOptions.setHandleColumnNotFoundAsNullValueInGetValues(true);

				Map<String, String> map = new LinkedHashMap<>();

				for (int r=0; r<foreignTableOptions.getRowCount(); r++)
				{
					String option_name  = foreignTableOptions.getValueAsString(r, "option_name");
					String option_value = foreignTableOptions.getValueAsString(r, "option_value");
					map.put(option_name, option_value);
				}
				
				if ( ! map.containsKey("schema_name") ) map.put("schema_name", ti.getSchemaName());
				if ( ! map.containsKey("table_name" ) ) map.put("table_name" , ti.getTableName());
				
				ti._remoteTableOptionsMap = map;
				ti._remoteTableOptions = StringUtil.toCommaStr(map);
			}
			else // If it's empty, just add: 'schema_name' and 'table_name' as the default
			{
				Map<String, String> map = new LinkedHashMap<>();
				
				map.put("schema_name", ti.getSchemaName());
				map.put("table_name" , ti.getTableName());
				
				ti._remoteTableOptionsMap = map;
				ti._remoteTableOptions = StringUtil.toCommaStr(map);
			}

			// REMOTE SERVER OPTIONS
			if (remoteServerOptions != null && !remoteServerOptions.isEmpty())
			{
				remoteServerOptions.setHandleColumnNotFoundAsNullValueInGetValues(true);

				Map<String, String> map = new LinkedHashMap<>();

				for (int r=0; r<remoteServerOptions.getRowCount(); r++)
				{
					ti._remoteServerName = remoteServerOptions.getValueAsString(r, "foreign_server_name");

					String option_name  = remoteServerOptions.getValueAsString(r, "option_name");
					String option_value = remoteServerOptions.getValueAsString(r, "option_value");
					map.put(option_name, option_value);
					
					if ("dbname".equals(option_name))
					{
						ti._remoteDBName = option_value;
					}
				}
				
				ti._remoteServerOptionsMap = map;
				ti._remoteServerOptions = StringUtil.toCommaStr(map);
			}

			// valid entry
			return true;
		}
		
		// invalid entry
		return false;
	}


	/**
	 * Parse the information from the DdlStorage columns 'objectText' and 'extraInfoText' where various information is stored in.
	 * @param ti
	 * @return
	 */
	private boolean getTableAndIndexInfo(DbxConnection conn, PgTableInfo ti)
	{
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		// Below is how column 'objectText' typically looks like
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//   +----------------+-----------------+---------------------------+------------------------+-----------------+-------------+------------------+--------------+-----------+
		//   |ordinal_position|column_name      |data_type                  |character_maximum_length|numeric_precision|numeric_scale|datetime_precision|column_default|is_nullable|
		//   +----------------+-----------------+---------------------------+------------------------+-----------------+-------------+------------------+--------------+-----------+
		//   |               1|uuid             |character varying          |                      50|(NULL)           |(NULL)       |(NULL)            |(NULL)        |NO         |
		//   |               2|personnummer     |character varying          |                      50|(NULL)           |(NULL)       |(NULL)            |(NULL)        |YES        |
		//   |               3|foretagsid       |character varying          |                      50|(NULL)           |(NULL)       |(NULL)            |(NULL)        |NO         |
		//   |               4|forsakringsnummer|character varying          |                      50|(NULL)           |(NULL)       |(NULL)            |(NULL)        |YES        |
		//   |               5|beskrivning      |character varying          |                     200|(NULL)           |(NULL)       |(NULL)            |(NULL)        |YES        |
		//   |               6|typ              |character varying          |                      40|(NULL)           |(NULL)       |(NULL)            |(NULL)        |YES        |
		//   |               7|active           |boolean                    |(NULL)                  |(NULL)           |(NULL)       |(NULL)            |true          |NO         |
		//   |               8|uppdaterad       |timestamp without time zone|(NULL)                  |(NULL)           |(NULL)       |                 6|(NULL)        |YES        |
		//   |               9|skapad           |timestamp without time zone|(NULL)                  |(NULL)           |(NULL)       |                 6|(NULL)        |NO         |
		//   |              10|skapadav         |character varying          |                      50|(NULL)           |(NULL)       |(NULL)            |(NULL)        |YES        |
		//   |              11|atgardad         |boolean                    |(NULL)                  |(NULL)           |(NULL)       |(NULL)            |false         |NO         |
		//   |              12|atgardadav       |character varying          |                      50|(NULL)           |(NULL)       |(NULL)            |(NULL)        |YES        |
		//   |              13|atgardadtid      |timestamp without time zone|(NULL)                  |(NULL)           |(NULL)       |                 6|(NULL)        |YES        |
		//   |              14|dold             |boolean                    |(NULL)                  |(NULL)           |(NULL)       |(NULL)            |false         |NO         |
		//   |              15|doldav           |character varying          |                      50|(NULL)           |(NULL)       |(NULL)            |(NULL)        |YES        |
		//   |              16|doldtid          |timestamp without time zone|(NULL)                  |(NULL)           |(NULL)       |                 6|(NULL)        |YES        |
		//   +----------------+-----------------+---------------------------+------------------------+-----------------+-------------+------------------+--------------+-----------+
		
		
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		// Below is how column 'extraInfoText' typically looks like (without a clustered index)
		// AND with LOB data
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//   +--------+-----------+-----------+------------+---------------+-------+-------------+-------------+--------------+-------------+-------------+
		//   |dbname  |schema_name|table_name |row_estimate|partition_count|oid    |total_size_mb|table_size_mb|table_size_pgs|index_size_mb|toast_size_mb|
		//   +--------+-----------+-----------+------------+---------------+-------+-------------+-------------+--------------+-------------+-------------+
		//   |pcexport|ocgroup    |felfallning|      36,549|             -1|699,725|       40.727|       10.688|         1,364|       30.031|        0.008|
		//   +--------+-----------+-----------+------------+---------------+-------+-------------+-------------+--------------+-------------+-------------+
		//   Rows 1
        //   
		//   +--------+-----------+-----------+--------------------------------------+------------+-------+----------+-------+----------+---------------------------------------------------+------------------+----------------------------------------------------------------------------------------------------------------------------------------------+
		//   |dbname  |schema_name|table_name |index_name                            |row_estimate|oid    |size_pages|size_mb|tablespace|description                                        |index_keys        |ddl                                                                                                                                           |
		//   +--------+-----------+-----------+--------------------------------------+------------+-------+----------+-------+----------+---------------------------------------------------+------------------+----------------------------------------------------------------------------------------------------------------------------------------------+
		//   |pcexport|ocgroup    |felfallning|felfallning_pkey                      |      36,549|699,734|       518|  4.047|(NULL)    |unique=true, PK=true, excl=false, clustered=false  |uuid              |CREATE UNIQUE INDEX felfallning_pkey ON ocgroup.felfallning USING btree (uuid)                                                                |
		//   |pcexport|ocgroup    |felfallning|felfallning_active_vald_foretagsid_idx|      33,810|699,746|       655|  5.117|(NULL)    |unique=false, PK=false, excl=false, clustered=false|foretagsid        |CREATE INDEX felfallning_active_vald_foretagsid_idx ON ocgroup.felfallning USING btree (foretagsid) WHERE ((dold = false) AND (active = true))|
		//   |pcexport|ocgroup    |felfallning|felfallning_active_foretagsid_idx     |      35,560|699,747|       823|   6.43|(NULL)    |unique=false, PK=false, excl=false, clustered=false|foretagsid, skapad|CREATE INDEX felfallning_active_foretagsid_idx ON ocgroup.felfallning USING btree (foretagsid, skapad DESC) WHERE (active = true)             |
		//   |pcexport|ocgroup    |felfallning|felfallning_org_idx                   |      36,549|699,748|     1,038|  8.109|(NULL)    |unique=false, PK=false, excl=false, clustered=false|foretagsid        |CREATE INDEX felfallning_org_idx ON ocgroup.felfallning USING btree (foretagsid)                                                              |
		//   |pcexport|ocgroup    |felfallning|felfallning_ssn_idx                   |      36,549|699,749|       301|  2.352|(NULL)    |unique=false, PK=false, excl=false, clustered=false|personnummer      |CREATE INDEX felfallning_ssn_idx ON ocgroup.felfallning USING btree (personnummer)                                                            |
		//   |pcexport|ocgroup    |felfallning|felfallning_policy_idx                |      36,549|699,750|       494|  3.859|(NULL)    |unique=false, PK=false, excl=false, clustered=false|forsakringsnummer |CREATE INDEX felfallning_policy_idx ON ocgroup.felfallning USING btree (forsakringsnummer)                                                    |
		//   +--------+-----------+-----------+--------------------------------------+------------+-------+----------+-------+----------+---------------------------------------------------+------------------+----------------------------------------------------------------------------------------------------------------------------------------------+
		//   Rows 6
		
		if (StringUtil.hasValue(ti._extraInfoText))
		{
			List<ResultSetTableModel> extraInfoRstmList = ResultSetTableModel.parseTextTables(ti._extraInfoText);
			
			ResultSetTableModel tableSizeInfo = ResultSetTableModel.getTableWithColumnNames(extraInfoRstmList, true, "dbname", "schema_name", "table_name", "row_estimate");
			ResultSetTableModel indexSizeInfo = ResultSetTableModel.getTableWithColumnNames(extraInfoRstmList, true, "dbname", "schema_name", "table_name", "index_name");
			
			// TABLE INFORMATION
			if (tableSizeInfo != null && !tableSizeInfo.isEmpty())
			{
				tableSizeInfo.setHandleColumnNotFoundAsNullValueInGetValues(true);
				
				int r = 0;

				ti._rowtotal            = tableSizeInfo.getValueAsLong   (r, "row_estimate"                , true, -1L);
				ti._partitionCount      = tableSizeInfo.getValueAsInteger(r, "partition_count"             , true, -1);
				ti._object_id           = tableSizeInfo.getValueAsLong   (r, "oid"                         , true, -1L);

				ti._totalMb             = tableSizeInfo.getValueAsDouble (r, "total_size_mb"               , true, -1d);
				ti._dataMb              = tableSizeInfo.getValueAsDouble (r, "table_size_mb"               , true, -1d);
				ti._dataPages           = tableSizeInfo.getValueAsLong   (r, "table_size_pgs"              , true, -1L);
				ti._indexMb             = tableSizeInfo.getValueAsDouble (r, "index_size_mb"               , true, -1d);
				ti._toastMb             = tableSizeInfo.getValueAsDouble (r, "toast_size_mb"               , true, -1d);
			}

			// INDEX INFORMATION
			if (indexSizeInfo != null && !indexSizeInfo.isEmpty())
			{
				indexSizeInfo.setHandleColumnNotFoundAsNullValueInGetValues(true);

				for (int r=0; r<indexSizeInfo.getRowCount(); r++)
				{
					PgIndexInfo ii = new PgIndexInfo();

					ii._indexName   = indexSizeInfo.getValueAsString(r, "index_name"); 
					ii._rowcount    = indexSizeInfo.getValueAsLong  (r, "row_estimate", true, -1L);
					ii._IndexID     = indexSizeInfo.getValueAsLong  (r, "oid"         , true, -1L);
					ii._sizePages   = indexSizeInfo.getValueAsLong  (r, "size_pages"  , true, -1L);
					ii._sizeMb      = indexSizeInfo.getValueAsDouble(r, "size_mb"     , true, -1D);
					ii._tablespace  = indexSizeInfo.getValueAsString(r, "tablespace");
					ii._desc        = indexSizeInfo.getValueAsString(r, "description");
					ii._keysStr     = indexSizeInfo.getValueAsString (r, "index_keys");
					ii._keys        = StringUtil.parseCommaStrToList(ii._keysStr, true);
					ii._ddlTxt      = indexSizeInfo.getValueAsString(r, "ddl");

					if (StringUtil.hasValue(ii._ddlTxt))
					{
						ii._includeColsStr = StringUtils.substringBetween(ii._ddlTxt, "INCLUDE (", ")");
						ii._includeCols    = StringUtil.parseCommaStrToList(ii._includeColsStr, true);
					}
					
					// Add index to table list
					ti._indexList.add(ii);
				}
			}

			// valid entry
			return true;
		}
		
		// invalid entry
		return false;
	}

	
	private void getPgIndexes(DbxConnection conn, PgTableInfo ti)
	{
		String dbname     = ti._dbName;
		String schemaName = ti._schemaName;
		String tableName  = ti._tableName;

		// Check if table(s) exists, if not... just exit...
		if ( ! DbUtils.checkIfTableExistsNoThrow(conn, null, null, "CmPgIndexes_diff") )
		{
			_logger.warn("Table 'CmPgIndexes_diff' did not exists, skip this.");
			return;
		}

		if ( ! DbUtils.checkIfTableExistsNoThrow(conn, null, null, "CmPgIndexes_abs") )
		{
			_logger.warn("Table 'CmPgIndexes_abs' did not exists, skip this.");
			return;
		}
		
		String sql = ""
			    + "WITH diff as \n"
			    + "( \n"
			    + "    select \n"
			    + "         [dbname] \n"
			    + "        ,[schemaname] \n"
			    + "        ,[relname] \n"
			    + "        ,[indexrelname] \n"
			    + "        ,sum([idx_scan])      as [idx_scan] \n"
			    + "        ,sum([idx_tup_fetch]) as [idx_tup_fetch] \n"
			    + "        ,sum([idx_tup_read])  as [idx_tup_read] \n"
			    + "        ,sum([idx_tup_fetch]) / nullif(sum([idx_tup_read]),0) * 100.0 as [fetch_read_efficiency] \n"
			    + "    from [CmPgIndexes_diff] \n"
			    + "    where 1=1 \n"
			    + "      and [dbname]       = " + DbUtils.safeStr(dbname    ) + " \n"
			    + "      and [schemaname]   = " + DbUtils.safeStr(schemaName) + " \n"
			    + "      and [relname]      = " + DbUtils.safeStr(tableName ) + " \n"
			    + "    group by \n"
			    + "         [dbname] \n"
			    + "        ,[schemaname] \n"
			    + "        ,[relname] \n"
			    + "        ,[indexrelname] \n"
			    + ") \n"
			    + ",abs as \n"
			    + "( \n"
			    + "    select \n"
			    + "         [dbname] \n"
			    + "        ,[schemaname] \n"
			    + "        ,[relname] \n"
			    + "        ,[indexrelname] \n"
			    + "        ,[idx_scan]      as [abs_idx_scan] \n"
			    + "        ,[idx_tup_fetch] as [abs_idx_tup_fetch] \n"
			    + "        ,[idx_tup_read]  as [abs_idx_tup_read] \n"
			    + "        ,1.0*[idx_tup_fetch] / nullif(1.0*[idx_tup_read],0) * 100.0 as [abs_fetch_read_efficiency] \n"
			    + "    from [CmPgIndexes_abs] \n"
			    + "    where [SessionSampleTime] = (select max([SessionSampleTime]) from [CmPgIndexes_abs]) \n"
			    + "      and [dbname]       = " + DbUtils.safeStr(dbname    ) + " \n"
			    + "      and [schemaname]   = " + DbUtils.safeStr(schemaName) + " \n"
			    + "      and [relname]      = " + DbUtils.safeStr(tableName ) + " \n"
			    + ") \n"
			    + "select \n"
			    + "     diff.[dbname] \n"
			    + "    ,diff.[schemaname] \n"
			    + "    ,diff.[relname] \n"
			    + "    ,diff.[indexrelname] \n"
			    + " \n"
			    + "    ,diff.[idx_scan] \n"
			    + "    ,diff.[idx_tup_fetch] \n"
			    + "    ,diff.[idx_tup_read] \n"
			    + "    ,diff.[fetch_read_efficiency] \n"
			    + " \n"
			    + "    ,abs.[abs_idx_scan] \n"
			    + "    ,abs.[abs_idx_tup_fetch] \n"
			    + "    ,abs.[abs_idx_tup_read] \n"
			    + "    ,abs.[abs_fetch_read_efficiency] \n"
			    + "from diff \n"
			    + "left outer join abs on diff.[dbname]       = abs.[dbname] \n"
			    + "                   and diff.[schemaname]   = abs.[schemaname] \n"
			    + "                   and diff.[relname]      = abs.[relname] \n"
			    + "                   and diff.[indexrelname] = abs.[indexrelname] \n"
//			    + "--order by diff.[xxx] \n"
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
				//	String    dbname                        = rs.getString   (1);
				//	String    schemaname                    = rs.getString   (2);
				//	String    relname                       = rs.getString   (3);
					String    indexrelname                  = rs.getString   (4);
					
					long      idx_scan                      = rs.getLong     (5);
					long      idx_tup_fetch                 = rs.getLong     (6);
					long      idx_tup_read                  = rs.getLong     (7);
					double    idx_fetch_read_efficiency     = rs.getDouble   (8);
					
					long      abs_idx_scan                  = rs.getLong     (9);
					long      abs_idx_tup_fetch             = rs.getLong     (10);
					long      abs_idx_tup_read              = rs.getLong     (11);
					double    abs_idx_fetch_read_efficiency = rs.getDouble   (12);
					
					PgIndexInfo indexEntry = ti.getIndexInfoForName(indexrelname);
					if (indexEntry != null)
					{
						indexEntry._idx_scan                      = idx_scan;
						indexEntry._idx_tup_fetch                 = idx_tup_fetch;
						indexEntry._idx_tup_read                  = idx_tup_read;
						indexEntry._idx_fetch_read_efficiency     = idx_fetch_read_efficiency;

						indexEntry._abs_idx_scan                  = abs_idx_scan;
						indexEntry._abs_idx_tup_fetch             = abs_idx_tup_fetch;
						indexEntry._abs_idx_tup_read              = abs_idx_tup_read;
						indexEntry._abs_idx_fetch_read_efficiency = abs_idx_fetch_read_efficiency;
					}
				}
			}
		}
		catch(SQLException ex)
		{
			_logger.warn("Problems getting INDEX Information for dbname='" + dbname + "', schema='" + schemaName + "', table='" + tableName + "'. Skipping and continuing.", ex);
		}
	}
	
	
	private void getPgIndexesIo(DbxConnection conn, PgTableInfo ti)
	{
		String dbname     = ti._dbName;
		String schemaName = ti._schemaName;
		String tableName  = ti._tableName;

		// Check if table(s) exists, if not... just exit...
		if ( ! DbUtils.checkIfTableExistsNoThrow(conn, null, null, "CmPgIndexesIo_diff") )
		{
			_logger.warn("Table 'CmPgIndexesIo_diff' did not exists, skip this.");
			return;
		}

		if ( ! DbUtils.checkIfTableExistsNoThrow(conn, null, null, "CmPgIndexesIo_abs") )
		{
			_logger.warn("Table 'CmPgIndexesIo_abs' did not exists, skip this.");
			return;
		}
		
		String sql = ""
			    + "WITH diff as \n"
			    + "( \n"
			    + "    select \n"
			    + "         [dbname] \n"
			    + "        ,[schemaname] \n"
			    + "        ,[relname] \n"
			    + "        ,[indexrelname] \n"
			    + "        ,sum([idx_blks_read]) + sum([idx_blks_hit]) as [idx_total_read] \n"
			    + "        ,sum([idx_blks_read]) as [idx_physical_read] \n"
			    + "        ,sum([idx_blks_hit]) / nullif(sum([idx_blks_read]) + sum([idx_blks_hit]), 0) * 100.0 as [cacheHitPct] \n"
			    + "    from [CmPgIndexesIo_diff] \n"
			    + "    where 1=1 \n"
			    + "      and [dbname]       = " + DbUtils.safeStr(dbname    ) + " \n"
			    + "      and [schemaname]   = " + DbUtils.safeStr(schemaName) + " \n"
			    + "      and [relname]      = " + DbUtils.safeStr(tableName ) + " \n"
			    + "    group by \n"
			    + "         [dbname] \n"
			    + "        ,[schemaname] \n"
			    + "        ,[relname] \n"
			    + "        ,[indexrelname] \n"
			    + ") \n"
			    + ",abs as \n"
			    + "( \n"
			    + "    select \n"
			    + "         [dbname] \n"
			    + "        ,[schemaname] \n"
			    + "        ,[relname] \n"
			    + "        ,[indexrelname] \n"
			    + "        ,[idx_blks_read] + [idx_blks_hit] as [abs_idx_total_read] \n"
			    + "        ,[idx_blks_read] as [abs_idx_physical_read] \n"
			    + "        ,[idx_blks_hit]*1.0 / nullif([idx_blks_read] + [idx_blks_hit], 0) * 100.0 as [abs_cacheHitPct] \n"
			    + "    from [CmPgIndexesIo_abs] \n"
			    + "    where [SessionSampleTime] = (select max([SessionSampleTime]) from [CmPgIndexesIo_abs]) \n"
			    + "      and [dbname]       = " + DbUtils.safeStr(dbname    ) + " \n"
			    + "      and [schemaname]   = " + DbUtils.safeStr(schemaName) + " \n"
			    + "      and [relname]      = " + DbUtils.safeStr(tableName ) + " \n"
			    + ") \n"
			    + "select \n"
			    + "     diff.[dbname] \n"
			    + "    ,diff.[schemaname] \n"
			    + "    ,diff.[relname] \n"
			    + "    ,diff.[indexrelname] \n"
			    + " \n"
			    + "    ,diff.[idx_total_read] \n"
			    + "    ,diff.[idx_physical_read] \n"
			    + "    ,diff.[cacheHitPct] \n"
			    + " \n"
			    + "    ,abs.[abs_idx_total_read] \n"
			    + "    ,abs.[abs_idx_physical_read] \n"
			    + "    ,abs.[abs_cacheHitPct] \n"
			    + "from diff \n"
			    + "left outer join abs on diff.[dbname]       = abs.[dbname] \n"
			    + "                   and diff.[schemaname]   = abs.[schemaname] \n"
			    + "                   and diff.[relname]      = abs.[relname] \n"
			    + "                   and diff.[indexrelname] = abs.[indexrelname] \n"
//			    + "--order by diff.[xxx] \n"
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
				//	String    dbname                       = rs.getString   (1);
				//	String    schemaname                   = rs.getString   (2);
				//	String    relname                      = rs.getString   (3);
					String    indexrelname                 = rs.getString   (4);
					
					long      idx_total_read               = rs.getLong     (5);
					long      idx_physical_read            = rs.getLong     (6);
					double    idx_cacheHitPct              = rs.getDouble   (7);
					
					long      abs_idx_total_read           = rs.getLong     (8);
					long      abs_idx_physical_read        = rs.getLong     (9);
					double    abs_idx_cacheHitPct          = rs.getDouble   (10);
					
					PgIndexInfo indexEntry = ti.getIndexInfoForName(indexrelname);
					if (indexEntry != null)
					{
						indexEntry._idx_total_read          = idx_total_read;
						indexEntry._idx_physical_read       = idx_physical_read;
						indexEntry._idx_cacheHitPct         = MathUtils.round(idx_cacheHitPct, 1);

						indexEntry._abs_idx_total_read      = abs_idx_total_read;
						indexEntry._abs_idx_physical_read   = abs_idx_physical_read;
						indexEntry._abs_idx_cacheHitPct     = MathUtils.round(abs_idx_cacheHitPct, 1);
					}
				}
			}
		}
		catch(SQLException ex)
		{
			_logger.warn("Problems getting INDEX Information for dbname='" + dbname + "', schema='" + schemaName + "', table='" + tableName + "'. Skipping and continuing.", ex);
		}
	}
	
	
	private void getPgTables(DbxConnection conn, PgTableInfo ti)
	{
		String dbname     = ti._dbName;
		String schemaName = ti._schemaName;
		String tableName  = ti._tableName;

		// Check if table(s) exists, if not... just exit...
		if ( ! DbUtils.checkIfTableExistsNoThrow(conn, null, null, "CmPgTables_diff") )
		{
			_logger.warn("Table 'CmPgTables_diff' did not exists, skip this.");
			return;
		}

		if ( ! DbUtils.checkIfTableExistsNoThrow(conn, null, null, "CmPgTables_abs") )
		{
			_logger.warn("Table 'CmPgTables_abs' did not exists, skip this.");
			return;
		}

		// just to get Column names
		String dummySql = "select * from [CmPgTables_diff] where 1 = 2";
		ResultSetTableModel dummyRstm = executeQuery(conn, dummySql, false, "metadata");
		if (dummyRstm == null)
		{
			return;
		}

		
		// Some columns do not exists in some versions...
		// Lets simulate that they exists, but with some "default" value
		String col_with_diff__n_mod_since_analyze = "        ,-1 as [n_mod_since_analyze] \n";      // introduced in version 9.4
		String col_with_abs__n_mod_since_analyze  = "        ,-1 as [abs_n_mod_since_analyze] \n";  // introduced in version 9.4
		
		if (dummyRstm.hasColumnNoCase("n_mod_since_analyze")) // introduced in version 9.4
		{
			col_with_diff__n_mod_since_analyze = "        ,sum([n_mod_since_analyze]) as [n_mod_since_analyze] \n"; 
			col_with_abs__n_mod_since_analyze  = "        ,[n_mod_since_analyze] as [abs_n_mod_since_analyze] \n"; 
		}


		String sql = ""
			    + "WITH diff as \n"
			    + "( \n"
			    + "    select \n"
			    + "         [dbname] \n"
			    + "        ,[schemaname] \n"
			    + "        ,[relname] \n"
			    + " \n"
			    + "        ,sum([seq_scan])     as [seq_scan] \n"
			    + "        ,sum([seq_tup_read]) as [seq_tup_read] \n"
			    + " \n"
			    + "        ,sum([idx_scan])      as [idx_scan] \n"
			    + "        ,sum([idx_tup_fetch]) as [idx_tup_fetch] \n"
			    + " \n"
			    + "        ,sum([n_tup_ins]) + sum([n_tup_upd]) + sum([n_tup_del]) as [n_tup_crud] \n"
			    + "        ,sum([n_tup_ins])     as [n_tup_ins] \n"
			    + "        ,sum([n_tup_upd])     as [n_tup_upd] \n"
			    + "        ,sum([n_tup_del])     as [n_tup_del] \n"
			    + "        ,sum([n_tup_hot_upd]) as [n_tup_hot_upd] \n"
			    + " \n"
			    + "        ,sum([n_live_tup]) as [n_live_tup] \n"
			    + "        ,sum([n_dead_tup]) as [n_dead_tup] \n"
			    + " \n"
			    + col_with_diff__n_mod_since_analyze // "        ,sum([n_mod_since_analyze]) as [n_mod_since_analyze] \n"
			    + " \n"
			    + "        ,max([last_vacuum])       as [last_vacuum] \n"
			    + "        ,max([last_autovacuum])   as [last_autovacuum] \n"
			    + "        ,max([last_analyze])      as [last_analyze] \n"
			    + "        ,max([last_autoanalyze])  as [last_autoanalyze] \n"
			    + " \n"
			    + "        ,sum([vacuum_count])      as [vacuum_count] \n"
			    + "        ,sum([autovacuum_count])  as [autovacuum_count] \n"
			    + "        ,sum([analyze_count])     as [analyze_count] \n"
			    + "        ,sum([autoanalyze_count]) as [autoanalyze_count] \n"
			    + " \n"
			    + "    from [CmPgTables_diff] \n"
			    + "    where 1=1 \n"
			    + "      and [dbname]       = " + DbUtils.safeStr(dbname    ) + " \n"
			    + "      and [schemaname]   = " + DbUtils.safeStr(schemaName) + " \n"
			    + "      and [relname]      = " + DbUtils.safeStr(tableName ) + " \n"
			    + "    group by \n"
			    + "         [dbname] \n"
			    + "        ,[schemaname] \n"
			    + "        ,[relname] \n"
			    + ") \n"
			    + ",abs as \n"
			    + "( \n"
			    + "    select \n"
			    + "         [dbname] \n"
			    + "        ,[schemaname] \n"
			    + "        ,[relname] \n"
			    + " \n"
			    + "        ,[seq_scan]     as [abs_seq_scan] \n"
			    + "        ,[seq_tup_read] as [abs_seq_tup_read] \n"
			    + " \n"
			    + "        ,[idx_scan]      as [abs_idx_scan] \n"
			    + "        ,[idx_tup_fetch] as [abs_idx_tup_fetch] \n"
			    + " \n"
			    + "        ,[n_tup_ins] + [n_tup_upd] + [n_tup_del] as [abs_n_tup_crud] \n"
			    + "        ,[n_tup_ins]     as [abs_n_tup_ins] \n"
			    + "        ,[n_tup_upd]     as [abs_n_tup_upd] \n"
			    + "        ,[n_tup_del]     as [abs_n_tup_del] \n"
			    + "        ,[n_tup_hot_upd] as [abs_n_tup_hot_upd] \n"
			    + " \n"
			    + "        ,[n_live_tup] as [abs_n_live_tup] \n"
			    + "        ,[n_dead_tup] as [abs_n_dead_tup] \n"
			    + " \n"
			    + col_with_abs__n_mod_since_analyze // "        ,[n_mod_since_analyze] as [abs_n_mod_since_analyze] \n"
			    + " \n"
			    + "        ,[last_vacuum]       as [abs_last_vacuum] \n"
			    + "        ,[last_autovacuum]   as [abs_last_autovacuum] \n"
			    + "        ,[last_analyze]      as [abs_last_analyze] \n"
			    + "        ,[last_autoanalyze]  as [abs_last_autoanalyze] \n"
			    + " \n"
			    + "        ,[vacuum_count]      as [abs_vacuum_count] \n"
			    + "        ,[autovacuum_count]  as [abs_autovacuum_count] \n"
			    + "        ,[analyze_count]     as [abs_analyze_count] \n"
			    + "        ,[autoanalyze_count] as [abs_autoanalyze_count] \n"
			    + " \n"
			    + "    from [CmPgTables_abs] \n"
			    + "    where [SessionSampleTime] = (select max([SessionSampleTime]) from [CmPgTables_abs]) \n"
			    + "      and [dbname]       = " + DbUtils.safeStr(dbname    ) + " \n"
			    + "      and [schemaname]   = " + DbUtils.safeStr(schemaName) + " \n"
			    + "      and [relname]      = " + DbUtils.safeStr(tableName ) + " \n"
			    + ") \n"
			    + "select \n"
			    + "     diff.[dbname] \n"
			    + "    ,diff.[schemaname] \n"
			    + "    ,diff.[relname] \n"
			    + " \n"
			    + "    -- DIFF values \n"
			    + "    ,diff.[seq_scan] \n"
			    + "    ,diff.[seq_tup_read] \n"
			    + " \n"
			    + "    ,diff.[idx_scan] \n"
			    + "    ,diff.[idx_tup_fetch] \n"
			    + " \n"
			    + "    ,diff.[n_tup_crud] \n"
			    + "    ,diff.[n_tup_ins] \n"
			    + "    ,diff.[n_tup_upd] \n"
			    + "    ,diff.[n_tup_hot_upd] \n"
			    + "    ,diff.[n_tup_del] \n"
			    + " \n"
			    + "    ,diff.[n_live_tup] \n"
			    + "    ,diff.[n_dead_tup] \n"
			    + " \n"
			    + "    ,diff.[n_mod_since_analyze] \n"
			    + " \n"
			    + "    ,diff.[last_vacuum] \n"
			    + "    ,diff.[last_autovacuum] \n"
			    + "    ,diff.[last_analyze] \n"
			    + "    ,diff.[last_autoanalyze] \n"
			    + " \n"
			    + "    ,diff.[vacuum_count] \n"
			    + "    ,diff.[autovacuum_count] \n"
			    + "    ,diff.[analyze_count] \n"
			    + "    ,diff.[autoanalyze_count] \n"
			    + " \n"
			    + "    -- ABS values \n"
			    + "    ,abs.[abs_seq_scan] \n"
			    + "    ,abs.[abs_seq_tup_read] \n"
			    + " \n"
			    + "    ,abs.[abs_idx_scan] \n"
			    + "    ,abs.[abs_idx_tup_fetch] \n"
			    + " \n"
			    + "    ,abs.[abs_n_tup_crud] \n"
			    + "    ,abs.[abs_n_tup_ins] \n"
			    + "    ,abs.[abs_n_tup_upd] \n"
			    + "    ,abs.[abs_n_tup_hot_upd] \n"
			    + "    ,abs.[abs_n_tup_del] \n"
			    + " \n"
			    + "    ,abs.[abs_n_live_tup] \n"
			    + "    ,abs.[abs_n_dead_tup] \n"
			    + " \n"
			    + "    ,abs.[abs_n_mod_since_analyze] \n"
			    + " \n"
			    + "    ,abs.[abs_last_vacuum] \n"
			    + "    ,abs.[abs_last_autovacuum] \n"
			    + "    ,abs.[abs_last_analyze] \n"
			    + "    ,abs.[abs_last_autoanalyze] \n"
			    + " \n"
			    + "    ,abs.[abs_vacuum_count] \n"
			    + "    ,abs.[abs_autovacuum_count] \n"
			    + "    ,abs.[abs_analyze_count] \n"
			    + "    ,abs.[abs_autoanalyze_count] \n"
			    + "from diff \n"
			    + "left outer join abs on diff.[dbname]       = abs.[dbname] \n"
			    + "                   and diff.[schemaname]   = abs.[schemaname] \n"
			    + "                   and diff.[relname]      = abs.[relname] \n"
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
				//	String    dbname             = rs.getString   (1);
				//	String    schemaname         = rs.getString   (2);
				//	String    relname            = rs.getString   (3);
					
					// DIFF -------------------------------------------
					ti._seq_scan                 = rs.getLong     (4);
					ti._seq_tup_read             = rs.getLong     (5);

					ti._idx_scan                 = rs.getLong     (6);
					ti._idx_tup_fetch            = rs.getLong     (7);

					ti._n_tup_crud               = rs.getLong     (8);
					ti._n_tup_ins                = rs.getLong     (9);
					ti._n_tup_upd                = rs.getLong     (10);
					ti._n_tup_hot_upd            = rs.getLong     (11);
					ti._n_tup_del                = rs.getLong     (12);

					ti._n_live_tup               = rs.getLong     (13);
					ti._n_dead_tup               = rs.getLong     (14);
                                                 
					ti._n_mod_since_analyze      = rs.getLong     (15);
                                                 
					ti._last_vacuum              = rs.getTimestamp(16);
					ti._last_autovacuum          = rs.getTimestamp(17);
					ti._last_analyze             = rs.getTimestamp(18);
					ti._last_autoanalyze         = rs.getTimestamp(19);
                                                 
					ti._vacuum_count             = rs.getLong     (20);
					ti._autovacuum_count         = rs.getLong     (21);
					ti._analyze_count            = rs.getLong     (22);
					ti._autoanalyze_count        = rs.getLong     (23);

					// ABS ---------------------------------------------
					ti._abs_seq_scan             = rs.getLong     (24);
					ti._abs_seq_tup_read         = rs.getLong     (25);

					ti._abs_idx_scan             = rs.getLong     (26);
					ti._abs_idx_tup_fetch        = rs.getLong     (27);
                                                 
					ti._abs_n_tup_crud           = rs.getLong     (28);
					ti._abs_n_tup_ins            = rs.getLong     (29);
					ti._abs_n_tup_upd            = rs.getLong     (30);
					ti._abs_n_tup_hot_upd        = rs.getLong     (31);
					ti._abs_n_tup_del            = rs.getLong     (32);

					ti._abs_n_live_tup           = rs.getLong     (33);
					ti._abs_n_dead_tup           = rs.getLong     (34);
                                                 
					ti._abs_n_mod_since_analyze  = rs.getLong     (35);
                                                 
					ti._abs_last_vacuum          = rs.getTimestamp(36);
					ti._abs_last_autovacuum      = rs.getTimestamp(37);
					ti._abs_last_analyze         = rs.getTimestamp(38);
					ti._abs_last_autoanalyze     = rs.getTimestamp(39);
                                                 
					ti._abs_vacuum_count         = rs.getLong     (40);
					ti._abs_autovacuum_count     = rs.getLong     (41);
					ti._abs_analyze_count        = rs.getLong     (42);
					ti._abs_autoanalyze_count    = rs.getLong     (43);
				}
			}
		}
		catch(SQLException ex)
		{
			_logger.warn("Problems getting INDEX Information for dbname='" + dbname + "', schema='" + schemaName + "', table='" + tableName + "'. Skipping and continuing.", ex);
		}
	}
	
	
	private void getPgTablesIo(DbxConnection conn, PgTableInfo ti)
	{
		String dbname     = ti._dbName;
		String schemaName = ti._schemaName;
		String tableName  = ti._tableName;

		// Check if table(s) exists, if not... just exit...
		if ( ! DbUtils.checkIfTableExistsNoThrow(conn, null, null, "CmPgTablesIo_diff") )
		{
			_logger.warn("Table 'CmPgTablesIo_diff' did not exists, skip this.");
			return;
		}

		if ( ! DbUtils.checkIfTableExistsNoThrow(conn, null, null, "CmPgTablesIo_abs") )
		{
			_logger.warn("Table 'CmPgTablesIo_abs' did not exists, skip this.");
			return;
		}
		
		String sql = ""
			    + "WITH diff as \n"
			    + "( \n"
			    + "    select \n"
			    + "         [dbname] \n"
			    + "        ,[schemaname] \n"
			    + "        ,[relname] \n"
			    + " \n"
			    + "        ,sum([heap_blks_read]) + sum([heap_blks_hit]) as [heap_total_read] \n"
			    + "        ,sum([heap_blks_read]) as [heap_physical_read] \n"
			    + "        ,sum([heap_blks_hit]) / nullif(sum([heap_blks_read]) + sum([heap_blks_hit]), 0) * 100.0 as [heap_cacheHitPct] \n"
			    + " \n"
			    + "        ,sum([idx_blks_read]) + sum([idx_blks_hit]) as [idx_total_read] \n"
			    + "        ,sum([idx_blks_read]) as [idx_physical_read] \n"
			    + "        ,sum([idx_blks_hit]) / nullif(sum([idx_blks_read]) + sum([idx_blks_hit]), 0) * 100.0 as [idx_cacheHitPct] \n"
			    + " \n"
			    + "        ,sum([toast_blks_read]) + sum([toast_blks_hit]) as [toast_total_read] \n"
			    + "        ,sum([toast_blks_read]) as [toast_physical_read] \n"
			    + "        ,sum([toast_blks_hit]) / nullif(sum([toast_blks_read]) + sum([toast_blks_hit]), 0) * 100.0 as [toast_cacheHitPct] \n"
			    + " \n"
			    + "        ,sum([tidx_blks_read]) + sum([tidx_blks_hit]) as [toast_idx_total_read] \n"
			    + "        ,sum([tidx_blks_read]) as [toast_idx_physical_read] \n"
			    + "        ,sum([tidx_blks_hit]) / nullif(sum([tidx_blks_read]) + sum([tidx_blks_hit]), 0) * 100.0 as [toast_idx_cacheHitPct] \n"
			    + " \n"
			    + "    from [CmPgTablesIo_diff] \n"
			    + "    where 1=1 \n"
			    + "      and [dbname]       = " + DbUtils.safeStr(dbname    ) + " \n"
			    + "      and [schemaname]   = " + DbUtils.safeStr(schemaName) + " \n"
			    + "      and [relname]      = " + DbUtils.safeStr(tableName ) + " \n"
			    + "    group by \n"
			    + "         [dbname] \n"
			    + "        ,[schemaname] \n"
			    + "        ,[relname] \n"
			    + ") \n"
			    + ",abs as \n"
			    + "( \n"
			    + "    select \n"
			    + "         [dbname] \n"
			    + "        ,[schemaname] \n"
			    + "        ,[relname] \n"
			    + " \n"
			    + "        ,[heap_blks_read] + [heap_blks_hit] as [abs_heap_total_read] \n"
			    + "        ,[heap_blks_read] as [abs_heap_physical_read] \n"
			    + "        ,[heap_blks_hit] / nullif([heap_blks_read] + [heap_blks_hit], 0) * 100.0 as [abs_heap_cacheHitPct] \n"
			    + " \n"
			    + "        ,[idx_blks_read] + [idx_blks_hit] as [abs_idx_total_read] \n"
			    + "        ,[idx_blks_read] as [abs_idx_physical_read] \n"
			    + "        ,[idx_blks_hit]*1.0 / nullif([idx_blks_read] + [idx_blks_hit], 0) * 100.0 as [abs_idx_cacheHitPct] \n"
			    + " \n"
			    + "        ,[toast_blks_read] + [toast_blks_hit] as [abs_toast_total_read] \n"
			    + "        ,[toast_blks_read] as [abs_toast_physical_read] \n"
			    + "        ,[toast_blks_hit]*1.0 / nullif([toast_blks_read] + [toast_blks_hit], 0) * 100.0 as [abs_toast_cacheHitPct] \n"
			    + " \n"
			    + "        ,[tidx_blks_read] + [tidx_blks_hit] as [abs_toast_idx_total_read] \n"
			    + "        ,[tidx_blks_read] as [abs_toast_idx_physical_read] \n"
			    + "        ,[tidx_blks_hit]*1.0 / nullif([tidx_blks_read] + [tidx_blks_hit], 0) * 100.0 as [abs_toast_idx_cacheHitPct] \n"
			    + " \n"
			    + "    from [CmPgTablesIo_abs] \n"
			    + "    where [SessionSampleTime] = (select max([SessionSampleTime]) from [CmPgTablesIo_abs]) \n"
			    + "      and [dbname]       = " + DbUtils.safeStr(dbname    ) + " \n"
			    + "      and [schemaname]   = " + DbUtils.safeStr(schemaName) + " \n"
			    + "      and [relname]      = " + DbUtils.safeStr(tableName ) + " \n"
			    + ") \n"
			    + "select \n"
			    + "     diff.[dbname] \n"
			    + "    ,diff.[schemaname] \n"
			    + "    ,diff.[relname] \n"
			    + " \n"
			    + "    -- DIFF values \n"
			    + "    ,diff.[heap_total_read] \n"
			    + "    ,diff.[heap_physical_read] \n"
			    + "    ,diff.[heap_cacheHitPct] \n"
			    + " \n"
			    + "    ,diff.[idx_total_read] \n"
			    + "    ,diff.[idx_physical_read] \n"
			    + "    ,diff.[idx_cacheHitPct] \n"
			    + " \n"
			    + "    ,diff.[toast_total_read] \n"
			    + "    ,diff.[toast_physical_read] \n"
			    + "    ,diff.[toast_cacheHitPct] \n"
			    + " \n"
			    + "    ,diff.[toast_idx_total_read] \n"
			    + "    ,diff.[toast_idx_physical_read] \n"
			    + "    ,diff.[toast_idx_cacheHitPct] \n"
			    + " \n"
			    + "    -- ABS values \n"
			    + "    ,abs.[abs_heap_total_read] \n"
			    + "    ,abs.[abs_heap_physical_read] \n"
			    + "    ,abs.[abs_heap_cacheHitPct] \n"
			    + " \n"
			    + "    ,abs.[abs_idx_total_read] \n"
			    + "    ,abs.[abs_idx_physical_read] \n"
			    + "    ,abs.[abs_idx_cacheHitPct] \n"
			    + " \n"
			    + "    ,abs.[abs_toast_total_read] \n"
			    + "    ,abs.[abs_toast_physical_read] \n"
			    + "    ,abs.[abs_toast_cacheHitPct] \n"
			    + " \n"
			    + "    ,abs.[abs_toast_idx_total_read] \n"
			    + "    ,abs.[abs_toast_idx_physical_read] \n"
			    + "    ,abs.[abs_toast_idx_cacheHitPct] \n"
			    + "from diff \n"
			    + "left outer join abs on diff.[dbname]       = abs.[dbname] \n"
			    + "                   and diff.[schemaname]   = abs.[schemaname] \n"
			    + "                   and diff.[relname]      = abs.[relname] \n"
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
				//	String    dbname                       = rs.getString   (1);
				//	String    schemaname                   = rs.getString   (2);
				//	String    relname                      = rs.getString   (3);
					
					ti._heap_total_read              = rs.getLong     (4);
					ti._heap_physical_read           = rs.getLong     (5);
					ti._heap_cacheHitPct             = rs.getDouble   (6);

					ti._idx_total_read               = rs.getLong     (7);
					ti._idx_physical_read            = rs.getLong     (8);
					ti._idx_cacheHitPct              = rs.getDouble   (9);

					ti._toast_total_read             = rs.getLong     (10);
					ti._toast_physical_read          = rs.getLong     (11);
					ti._toast_cacheHitPct            = rs.getDouble   (12);

					ti._toast_idx_total_read         = rs.getLong     (13);
					ti._toast_idx_physical_read      = rs.getLong     (14);
					ti._toast_idx_cacheHitPct        = rs.getDouble   (15);


					ti._abs_heap_total_read          = rs.getLong     (16);
					ti._abs_heap_physical_read       = rs.getLong     (17);
					ti._abs_heap_cacheHitPct         = rs.getDouble   (18);

					ti._abs_idx_total_read           = rs.getLong     (19);
					ti._abs_idx_physical_read        = rs.getLong     (20);
					ti._abs_idx_cacheHitPct          = rs.getDouble   (21);

					ti._abs_toast_total_read         = rs.getLong     (22);
					ti._abs_toast_physical_read      = rs.getLong     (23);
					ti._abs_toast_cacheHitPct        = rs.getDouble   (24);

					ti._abs_toast_idx_total_read     = rs.getLong     (25);
					ti._abs_toast_idx_physical_read  = rs.getLong     (26);
					ti._abs_toast_idx_cacheHitPct    = rs.getDouble   (27);
				}
			}
		}
		catch(SQLException ex)
		{
			_logger.warn("Problems getting INDEX Information for dbname='" + dbname + "', schema='" + schemaName + "', table='" + tableName + "'. Skipping and continuing.", ex);
		}
	}

	private void getTriggerInfo(DbxConnection conn, PgTableInfo tableInfo)
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

		List<PgTableInfo> tmpList = new ArrayList<>();
		
		sql = conn.quotifySqlString(sql);
		try ( Statement stmnt = conn.createStatement() )
		{
			// Unlimited execution time
			stmnt.setQueryTimeout(0);
			try ( ResultSet rs = stmnt.executeQuery(sql) )
			{
				while(rs.next())
				{
					PgTableInfo ti = new PgTableInfo();
					
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
		for (PgTableInfo ti : tmpList)
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
		} // end: loop PgTableInfo

		// Set the text
		return sb.toString();
	}
	
	
	public static class PgIndexInfo
	{
		private long         _IndexID           = -1;
		private String       _indexName;
		private String       _tablespace;
		private long         _rowcount          = -1;
		private long         _sizePages         = -1;
		private double       _sizeMb            = -1;
		private String       _keysStr;
		private List<String> _keys;
		private String       _includeColsStr;
		private List<String> _includeCols;
		private String       _desc;
		private String       _ddlTxt;
		private Timestamp    _CreationDate;

//		private Timestamp    _ObjectCacheDate;
//		private long         _RowsInsUpdDel     = -1;
//		private long         _OptSelectCount    = -1;
//		private long         _UsedCount         = -1;
//		private long         _Operations        = -1;
//		private long         _AbsRowsInsUpdDel  = -1;
//		private long         _AbsOptSelectCount = -1;
//		private long         _AbsUsedCount      = -1;
//		private long         _AbsOperations     = -1;

		private long         _idx_scan;
		private long         _idx_tup_fetch;
		private long         _idx_tup_read;
		private double       _idx_fetch_read_efficiency;

		private long         _idx_total_read;
		private long         _idx_physical_read;
		private double       _idx_cacheHitPct;

		private long         _abs_idx_scan;
		private long         _abs_idx_tup_fetch;
		private long         _abs_idx_tup_read;
		private double       _abs_idx_fetch_read_efficiency;

		private long         _abs_idx_total_read;
		private long         _abs_idx_physical_read;
		private double       _abs_idx_cacheHitPct;

		public long         getIndexID           () { return _IndexID; }
		public String       getIndexName         () { return _indexName; }
		public String       getTableSpace        () { return _tablespace; }
		public long         getRowCount          () { return _rowcount; }
		public long         getSizePages         () { return _sizePages; }
		public double       getSizeMb            () { return MathUtils.round(_sizeMb, 1); }
		public String       getKeysStr           () { return _keysStr; }
		public List<String> getKeys              () { return _keys; }
		public String       getIncludeStr        () { return _includeColsStr == null ? "" : _includeColsStr; }
		public List<String> getIncludeCols       () { return _includeCols; }
		public String       getOriginDescription () { return _desc; }
		public String       getDdlText           () { return _ddlTxt == null ? "" : _ddlTxt; }
                                                 
//		public Timestamp    getCreationDate      () { return _CreationDate;       }
//		public String       getCreationDateStr   () { return _CreationDate    == null ? "--not-found--" : _CreationDate.toString(); }
		
		public String getDescription ()
		{
			List<String> list = new ArrayList<>();

			// Fix description: unique=true, PK=true, excl=false, clustered=false
			if (_desc.contains("unique=true"         )) list.add("unique");
			if (_desc.contains("unique=false"        )) list.add("non-unique");
			if (_desc.contains("PK=true"             )) list.add("PK");
			if (_desc.contains("excl=true"           )) list.add("exclusion");
			if (_desc.contains("clustered=true"      )) list.add("clustered");
			if (getDdlText().contains(" WHERE "      )) list.add("partialIndex");
			if (getDdlText().contains(" USING btree ")) list.add("btree");

			return StringUtil.toCommaStr(list);
		}

	}
	public static class PgTableInfo
//	extends GenericTableInfo
	{
		private String    _dbName;
		private String    _schemaName;
		private String    _tableName;
		private String    _type;
		private Timestamp _crdate;
		private Timestamp _sampleTime;
		private String    _extraInfoText;  // sp_spaceused tabname, 1
		private String    _objectText;     // sp_help tabname
		private String    _childObjects;   // Type:name, Type:name, Type:name
		private String    _triggersText;   // Text of any trigger definitions
		private long      _rowtotal;
		private long      _object_id;
		private int       _partitionCount;
		private double    _totalMb;
		private double    _dataMb;
		private long      _dataPages;
		private double    _indexMb;
		private double    _toastMb;
//		private int       _indexCount;
		private List<PgIndexInfo> _indexList = new ArrayList<>();

		public List<String> _viewReferences;     // if _type == "v",  this this will hold; table/views this view references
		public List<String> _functionReferences; // if _type == "FN", this this will hold; table/views this function references
		public List<String> _cursorReferences;   // if _type == "CU", this this will hold; table/views this cursor references
		
		private String    _remoteTableOptions;
		private Map<String, String> _remoteTableOptionsMap;

		private String    _remoteDBName;
		
		private String    _remoteServerName;
		private String    _remoteServerOptions;
		private Map<String, String> _remoteServerOptionsMap;

		// CmPgTables_diff
		private long      _seq_scan                = -1;
		private long      _seq_tup_read            = -1;

		private long      _idx_scan                = -1;
		private long      _idx_tup_fetch           = -1;

		private long      _n_tup_crud              = -1;
		private long      _n_tup_ins               = -1;
		private long      _n_tup_upd               = -1;
		private long      _n_tup_hot_upd           = -1;
		private long      _n_tup_del               = -1;

		private long      _n_live_tup              = -1;
		private long      _n_dead_tup              = -1;
                                                   
		private long      _n_mod_since_analyze     = -1;
                                                   
		private Timestamp _last_vacuum             = null;
		private Timestamp _last_autovacuum         = null;
		private Timestamp _last_analyze            = null;
		private Timestamp _last_autoanalyze        = null;
                                                   
		private long      _vacuum_count            = -1;
		private long      _autovacuum_count        = -1;
		private long      _analyze_count           = -1;
		private long      _autoanalyze_count       = -1;

		// CmPgTables_abs
		private long      _abs_seq_scan            = -1;
		private long      _abs_seq_tup_read        = -1;

		private long      _abs_idx_scan            = -1;
		private long      _abs_idx_tup_fetch       = -1;

		private long      _abs_n_tup_crud          = -1;
		private long      _abs_n_tup_ins           = -1;
		private long      _abs_n_tup_upd           = -1;
		private long      _abs_n_tup_hot_upd       = -1;
		private long      _abs_n_tup_del           = -1;

		private long      _abs_n_live_tup          = -1;
		private long      _abs_n_dead_tup          = -1;

		private long      _abs_n_mod_since_analyze = -1;

		private Timestamp _abs_last_vacuum         = null;
		private Timestamp _abs_last_autovacuum     = null;
		private Timestamp _abs_last_analyze        = null;
		private Timestamp _abs_last_autoanalyze    = null;

		private long      _abs_vacuum_count        = -1;
		private long      _abs_autovacuum_count    = -1;
		private long      _abs_analyze_count       = -1;
		private long      _abs_autoanalyze_count   = -1;
		
		
		// CmPgTablesIo_diff
		private long   _heap_total_read              = -1;
		private long   _heap_physical_read           = -1;
		private double _heap_cacheHitPct             = -1;

		private long   _idx_total_read               = -1;
		private long   _idx_physical_read            = -1;
		private double _idx_cacheHitPct              = -1;

		private long   _toast_total_read             = -1;
		private long   _toast_physical_read          = -1;
		private double _toast_cacheHitPct            = -1;

		private long   _toast_idx_total_read         = -1;
		private long   _toast_idx_physical_read      = -1;
		private double _toast_idx_cacheHitPct        = -1;

		// CmPgTablesIo_abs
		private long   _abs_heap_total_read          = -1;
		private long   _abs_heap_physical_read       = -1;
		private double _abs_heap_cacheHitPct         = -1;

		private long   _abs_idx_total_read           = -1;
		private long   _abs_idx_physical_read        = -1;
		private double _abs_idx_cacheHitPct          = -1;

		private long   _abs_toast_total_read         = -1;
		private long   _abs_toast_physical_read      = -1;
		private double _abs_toast_cacheHitPct        = -1;

		private long   _abs_toast_idx_total_read     = -1;
		private long   _abs_toast_idx_physical_read  = -1;
		private double _abs_toast_idx_cacheHitPct    = -1;

		
		public PgIndexInfo getIndexInfoForName(String indexName)
		{
			if (StringUtil.isNullOrBlank(indexName))
				return null;

			for (PgIndexInfo e : _indexList)
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
		
		public String    getDbName()             { return _dbName; }
		public String    getSchemaName()         { return _schemaName; }
		public String    getTableName()          { return _tableName; }
		public String    getType()               { return _type; }
		public Timestamp getCrDate()             { return _crdate; }
		public Timestamp getSampleTime()         { return _sampleTime; }

		public long      getObjectId()           { return _object_id; }
		public int       getPartitionCount()     { return _partitionCount; }

		
		public long      getRowTotal()           { return _rowtotal; }
		public double    getTotalMb()            { return MathUtils.round(_totalMb, 1); }
		public double    getDataMb()             { return MathUtils.round(_dataMb,  1); }
		public long      getDataPages()          { return _dataPages; }
		public double    getIndexMb()            { return MathUtils.round(_indexMb, 1); }
		public double    getToastMb()            { return MathUtils.round(_toastMb, 1); }
//		public int       getIndexCount()         { return _indexCount; }
		public int       getIndexCount()         { return getIndexList().size(); }
		public List<PgIndexInfo> getIndexList()  { return _indexList == null ? Collections.emptyList() : _indexList; }

		public boolean      isUserTable()           { return "r" .equals(_type); }
		public boolean      isView()                { return "v" .equals(_type); }
		public boolean      isRemoteTable()         { return "f" .equals(_type); }
		public boolean      isFunction()            { return "FN".equals(_type); }
		public boolean      isProcedure()           { return "P" .equals(_type); }
		public boolean      isCursor()              { return "CU".equals(_type); }

		public List<String> getViewReferences()     { return _viewReferences; }
		public List<String> getFunctionReferences() { return _functionReferences; }
		public List<String> getCursorReferences()   { return _cursorReferences; }

		public String    getRemoteServerName()      { return _remoteServerName; }
		public String    getRemoteServerOptions()   { return _remoteServerOptions; }
		public Map<String, String> getRemoteServerOptionsMap()   { return _remoteServerOptionsMap != null ? _remoteServerOptionsMap : Collections.emptyMap(); }

		public String    getRemoteDBName()          { return _remoteDBName; }

		public String    getRemoteTableOptions()    { return _remoteTableOptions; }
		public Map<String, String> getRemoteTableOptionsMap()   { return _remoteTableOptionsMap != null ? _remoteTableOptionsMap : Collections.emptyMap(); }
		public String    getRemoteTableName()
		{
			if (_remoteTableOptionsMap == null)
				return "unknown";

			String schema_name = _remoteTableOptionsMap.get("schema_name");
			String table_name  = _remoteTableOptionsMap.get("table_name");

			if (StringUtil.isNullOrBlank(schema_name)) schema_name = getSchemaName();
			if (StringUtil.isNullOrBlank(table_name )) table_name  = getTableName();
			
			return schema_name + "." + table_name; 
		}
		
		@Override
		public String toString()
		{
			return super.toString() + "objectId=" + getObjectId() + ", dbname='" + getDbName() + "', schemaName='" + getSchemaName() + "', relationName='" + getTableName() + "', type='" + getType() + "'.";
		}

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

			PgTableInfo other = (PgTableInfo) obj;
			return Objects.equals(_dbName    , other._dbName) 
			    && Objects.equals(_schemaName, other._schemaName) 
			    && Objects.equals(_tableName , other._tableName);
		}
	}
}
