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

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.cache.XmlPlanAseUtils;
import com.dbxtune.gui.ModelMissmatchException;
import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.PersistentCounterHandler;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.pcs.report.content.ReportEntryAbstract;
import com.dbxtune.sql.SqlObjectName;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.DbUtils;
import com.dbxtune.utils.HtmlTableProducer;
import com.dbxtune.utils.MathUtils;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.TimeUtils;

public abstract class AseAbstract
extends ReportEntryAbstract
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

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

	/** 
	 * get ASE Page Size as bytes.
	 * <ul>
	 *   <li>2k  = <b> 2048  </b></li>
	 *   <li>4k  = <b> 4096  </b></li>
	 *   <li>8k  = <b> 8192  </b></li>
	 *   <li>16k = <b> 16384 </b></li>
	 * </ul>
	 * 
	 * @param conn
	 * @return
	 * @throws SQLException
	 */
	public int getAsePageSizeFromMonDdlStorage(DbxConnection conn)
	throws SQLException
	{
		String sql = "select top 1 [asePageSize] from [CmSummary_abs]";
		
		sql = conn.quotifySqlString(sql);
		try ( Statement stmnt = conn.createStatement() )
		{
			int asePageSize = -1;

			// Unlimited execution time
			stmnt.setQueryTimeout(0);
			try ( ResultSet rs = stmnt.executeQuery(sql) )
			{
				while(rs.next())
				{
					asePageSize = rs.getInt(1);
				}
			}

			return asePageSize;
		}
		catch(SQLException ex)
		{
			//_problem = ex;

			_logger.warn("Problems getting ASE Page Size from DDL Storage.", ex);
			throw ex;
		}
	}

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
		Set<AseTableInfo> tableInfoSet = getTableInformationFromMonDdlStorage(conn, tableList);
		if (tableInfoSet.isEmpty())
			return "";

		// And make it into a HTML table with various information about the table and indexes 
		return getTableInfoAsHtmlTable(currentDbname, tableInfoSet, tableList, includeIndexInfo, classname);
	}

	public String getTableInfoAsHtmlTable(String currentDbname, Set<AseTableInfo> tableInfoList, Set<String> tableList, boolean includeIndexInfo, String classname)
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
				for(AseTableInfo ti : tableInfoList)
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
		for (AseTableInfo entry : tableInfoList)
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
				tableInfoMap.put("DDL"        ,                 getFormattedSqlAsTooltipDiv(entry._objectText, "View DDL", DbUtils.DB_PROD_NAME_SYBASE_ASE));
			}
			else // Any table
			{
				tableInfoMap.put("DBName"     , markIfDifferent(entry.getDbName(), currentDbname));
				tableInfoMap.put("Schema"     ,                 entry.getSchemaName() );
				tableInfoMap.put("Table"      ,                 entry.getTableName()  );
				tableInfoMap.put("Rowcount"   , nf.format(      entry.getRowTotal()  ));
				tableInfoMap.put("Total MB"   , nf.format(      entry.getSizeMb()    ));
				tableInfoMap.put("Data MB"    , nf.format(      entry.getDataMb()    ));
				tableInfoMap.put("Data Pages" , nf.format(      entry.getDataPages() ));
				tableInfoMap.put("Index MB"   , nf.format(      entry.getIndexMb()   ));
				tableInfoMap.put("LOB MB"     , entry.getLobMb() == -1 ? "-no-lob-" : nf.format( entry.getLobMb() ));
				tableInfoMap.put("Created"    ,                entry.getCrDate()+""       );
				tableInfoMap.put("Sampled"    ,                entry.getSampleTime()+""   );
				tableInfoMap.put("Lock Scheme",                entry.getLockScheme() );
				tableInfoMap.put("Index Count", entry.getIndexCount() + (entry.getIndexCount() > 0 ? "" : " <b><font color='red'>&lt;&lt;-- Warning NO index</font></b>") );
				tableInfoMap.put("DDL Info"   , getTextAsTooltipDiv(entry._objectText, "Table Info"));
				tableInfoMap.put("Triggers"   , entry._triggersText == null ? "-no-triggers-" : getTextAsTooltipDiv(entry._triggersText, "Trigger Info"));
			}
			
			String tableInfo = HtmlTableProducer.createHtmlTable(tableInfoMap, "dsr-sub-table-other-info", true);
			String indexInfo = "";

			if (entry.isView()) 
			{
				// Build a bullet list of view references
				indexInfo = "";
				if (entry.getViewReferences() != null)
				{
					indexInfo += "View references the following tables:";
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

	public String getIndexInfoAsHtmlTable(AseTableInfo tableEntry, String classname)
	{
		// Exit early: if no data
		if (tableEntry == null)   return "";

		// Get the index list
		List<AseIndexInfo> indexInfoList = tableEntry.getIndexList();

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

//		sb.append("  <th>Index Name</th> \n");
//		sb.append("  <th>Keys</th> \n");
//		sb.append("  <th>IndexID</th> \n");
//		sb.append("  <th>Description</th> \n");
//		sb.append("  <th>Size MB</th> \n");
//		sb.append("  <th>Size Pages</th> \n");
//		sb.append("  <th title='Average Bytes used per Row\nCalculted by: SizeBytes/TableRowcount'>Avg RowSize</th> \n");
//		sb.append("  <th>Reserved MB</th> \n");
//		sb.append("  <th>Unused MB</th> \n");
////		sb.append("  <th>DDL</th> \n");
//
//		String tt1 = "\nTime span: During the report period.";
//		sb.append("  <th title='Number of Insert, Update and Deletes that has been done." + tt1                      + "'>RowsInsUpdDel</th> \n");
//		sb.append("  <th title='Number of times the optimizer selected this index to be used in a query plan." + tt1 + "'>OptSelectCount</th> \n");
//		sb.append("  <th title='Number of times the object was used in a plan during execution." + tt1               + "'>UsedCount</th> \n");
//		sb.append("  <th title='Number of times the object was accessed." + tt1                                      + "'>Operations</th> \n");
//		sb.append("  <th title='Number of scans performed on this object." + tt1                                     + "'>Scans</th> \n");
//
//		sb.append("  <th title='Indicates the date and time when the index structure was added to the ASE cache."    + "'>CacheDate</th> \n");
//		sb.append("  <th title='When was the index created."                                                         + "'>CrDate</th> \n");

		String tt = "\n(in bold) Time span: During the report period."
		          + "\n"
		          + "\n(in italic): Since object was cached (see CacheDate)."
		          + "\nNOTE: This counter is an integer that may/will wrap, if incremeted frequently."
		          + "\nUse this to decide if the index has **ever** been used since CacheDate."
		          + "\nA low value indicates that the index has NOT been used. And therefor may be an *unused* index.";

		sb.append(createHtmlTh("Index Name"     , ""));
		sb.append(createHtmlTh("Keys"           , ""));
		sb.append(createHtmlTh("IndexID"        , ""));
		sb.append(createHtmlTh("Description"    , ""));
		sb.append(createHtmlTh("Size MB"        , ""));
		sb.append(createHtmlTh("Size Pages"     , ""));
		sb.append(createHtmlTh("Avg RowSize"    , "Average Bytes used per Row\nCalculted by: SizeBytes/TableRowcount"));
		sb.append(createHtmlTh("Reserved MB"    , ""));
		sb.append(createHtmlTh("Unused MB"      , ""));
//		sb.append(createHtmlTh("DDL"            , ""));

		sb.append(createHtmlTh("RowsInsUpdDel"  , "Number of Insert, Update and Deletes that has been done." + tt));
		sb.append(createHtmlTh("OptSelectCount" , "Number of times the optimizer selected this index to be used in a query plan." + tt));
		sb.append(createHtmlTh("UsedCount"      , "Number of times the object was used in a plan during execution." + tt));
		sb.append(createHtmlTh("Operations"     , "Number of times the object was accessed." + tt));
		sb.append(createHtmlTh("Scans"          , "Number of scans performed on this object." + tt));

		sb.append(createHtmlTh("CacheDate"      , "Indicates the date and time when the index structure was added to the ASE cache"));
		sb.append(createHtmlTh("CrDate"         , "When was the index created"));

		sb.append(createHtmlTh("RowsInserted"         , ""));
		sb.append(createHtmlTh("RowsUpdated"          , ""));
		sb.append(createHtmlTh("RowsDeleted"          , ""));
		sb.append(createHtmlTh("LogicalReads"         , ""));
		sb.append(createHtmlTh("PhysicalReads"        , ""));
		sb.append(createHtmlTh("APFReads"             , "Physical disk IO, but issued as a Prefetch, or read-ahead-of-time-when-we-need-it..."));
		sb.append(createHtmlTh("LockWaits"            , "Number of times we had to wait for a lock"));
		sb.append(createHtmlTh("SharedLockWaitTime"   , "Wait time in milliseconds"));
		sb.append(createHtmlTh("ExclusiveLockWaitTime", "Wait time in milliseconds"));
		sb.append(createHtmlTh("UpdateLockWaitTime"   , "Wait time in milliseconds"));
		

//		String tt2 = "\nTime span: Since object was cached (see CacheDate)."
//		           + "\nNOTE: This counter is an integer that may/will wrap, if incremeted frequently."
//		           + "\nUse this to decide if the index has **ever** been used since CacheDate."
//		           + "\nA low value indicates that the index has NOT been used. And therefor may be an *unused* index.";
//		sb.append("  <th title='Number of Insert, Update and Deletes that has been done." + tt2                      + "'>AbsRowsInsUpdDel</th> \n");
//		sb.append("  <th title='Number of times the optimizer selected this index to be used in a query plan." + tt2 + "'>AbsOptSelectCount</th> \n");
//		sb.append("  <th title='Number of times the object was used in a plan during execution." + tt2               + "'>AbsUsedCount</th> \n");
//		sb.append("  <th title='Number of times the object was accessed." + tt2                                      + "'>AbsOperations</th> \n");

		sb.append("</tr> \n");
		sb.append("</thead> \n");

		NumberFormat nf = NumberFormat.getInstance();

		//--------------------------------------------------------------------------
		// Table BODY
		sb.append("<tbody> \n");
		for (AseIndexInfo entry : indexInfoList)
		{
			long avgBytesPerRow = tableEntry.getRowTotal() == 0 ? -1 : (entry.getSizeKb() * 1024L) / tableEntry.getRowTotal();

			sb.append("<tr> \n");
			sb.append("  <td>").append(            entry.getIndexName()          ).append("</td> \n");
			sb.append("  <td>").append(            entry.getKeysStr()            ).append("</td> \n");
			sb.append("  <td>").append(            entry.getIndexID()            ).append("</td> \n");
			sb.append("  <td>").append(            entry.getDescription()        ).append("</td> \n");
			sb.append("  <td>").append( nf.format( entry.getSizeMb()            )).append("</td> \n");
			sb.append("  <td>").append( nf.format( entry.getSizePages()         )).append("</td> \n");
			sb.append("  <td>").append( nf.format( avgBytesPerRow               )).append("</td> \n");
			sb.append("  <td>").append( nf.format( entry.getReservedMb()        )).append("</td> \n");
			sb.append("  <td>").append( nf.format( entry.getUnusedMb()          )).append("</td> \n");
//			sb.append("  <td>").append(            entry.getDdlText()            ).append("</td> \n");

			sb.append("  <td>").append( diffAbsValues(entry.getRowsInsUpdDel() , entry.getAbsRowsInsUpdDel()  ) ).append("</td> \n");
			sb.append("  <td>").append( diffAbsValues(entry.getOptSelectCount(), entry.getAbsOptSelectCount() ) ).append("</td> \n");
			sb.append("  <td>").append( diffAbsValues(entry.getUsedCount()     , entry.getAbsUsedCount()      ) ).append("</td> \n");
			sb.append("  <td>").append( diffAbsValues(entry.getOperations()    , entry.getAbsOperations()     ) ).append("</td> \n");
			sb.append("  <td>").append( diffAbsValues(entry.getScans()         , entry.getAbsScans()          ) ).append("</td> \n");

			sb.append("  <td>").append(            entry.getCacheDateStr()       ).append("</td> \n");
			sb.append("  <td>").append(            entry.getCreationDateStr()    ).append("</td> \n");

			sb.append("  <td>").append( diffAbsValues(entry.getRowsInserted         (), entry.getAbsRowsInserted         () ) ).append("</td> \n");
			sb.append("  <td>").append( diffAbsValues(entry.getRowsUpdated          (), entry.getAbsRowsUpdated          () ) ).append("</td> \n");
			sb.append("  <td>").append( diffAbsValues(entry.getRowsDeleted          (), entry.getAbsRowsDeleted          () ) ).append("</td> \n");
			
			sb.append("  <td>").append( diffAbsValues(entry.getLogicalReads         (), entry.getAbsLogicalReads         () ) ).append("</td> \n");
			sb.append("  <td>").append( diffAbsValues(entry.getPhysicalReads        (), entry.getAbsPhysicalReads        () ) ).append("</td> \n");
			sb.append("  <td>").append( diffAbsValues(entry.getAPFReads             (), entry.getAbsAPFReads             () ) ).append("</td> \n");
			sb.append("  <td>").append( diffAbsValues(entry.getLockWaits            (), entry.getAbsLockWaits            () ) ).append("</td> \n");
			sb.append("  <td>").append( diffAbsValues(entry.getSharedLockWaitTime   (), entry.getAbsSharedLockWaitTime   () ) ).append("</td> \n");
			sb.append("  <td>").append( diffAbsValues(entry.getExclusiveLockWaitTime(), entry.getAbsExclusiveLockWaitTime() ) ).append("</td> \n");
			sb.append("  <td>").append( diffAbsValues(entry.getUpdateLockWaitTime   (), entry.getAbsUpdateLockWaitTime   () ) ).append("</td> \n");

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

	/**
	 * FIXME
	 */
	public Set<AseTableInfo> getTableInformationFromMonDdlStorage(DbxConnection conn, Set<String> tableList)
	{
		// Exit early if nothing todo
		if (tableList == null)   return Collections.emptySet();
		if (tableList.isEmpty()) return Collections.emptySet();

		// Put all the results back in one list
		Set<AseTableInfo> combinedReturnList = new LinkedHashSet<>();
		
		for (String tableName : tableList)
		{
			try
			{
				Set<AseTableInfo> tmp = getTableInformationFromMonDdlStorage(conn, null, null, tableName);
				
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
	 * Get a list of AseTableInfo (basically sp_spaceused) 
	 * 
	 * @param conn       The connection to the PCS
	 * @param dbname     Name of the database the object is in. (can be "" or null; or contains '%' SQL wild-card)
	 * @param owner      Name of the schema   the object is in. (can be "" or null; or contains '%' SQL wild-card)
	 * @param table      Name of the table                      (can be "" or null; or contains '%' SQL wild-card), can also contain DBName.schema which will override the parameters dbname and owner
	 * 
	 * @return A list of found entries, each as an entry of AseTableInfo.
	 * 
	 * @throws SQLException In case of issues.
	 */
	public Set<AseTableInfo> getTableInformationFromMonDdlStorage(DbxConnection conn, String dbname, String owner, String table)
	throws SQLException
	{
		Set<AseTableInfo> result = new LinkedHashSet<>();

		// First level of (potential recursion)
		getTableInformationFromMonDdlStorage(conn, dbname, owner, table, 0, result);
		
		// Should we remove any VIEWS ?
		boolean removeViews = false;
		if (removeViews)
		{
			Set<AseTableInfo> tmp = new LinkedHashSet<>();
			for (AseTableInfo ti : result)
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
	private void getTableInformationFromMonDdlStorage(DbxConnection conn, String dbname, String owner, String table, int recursCallCount, Set<AseTableInfo> result)
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
		SqlObjectName sqlObj = new SqlObjectName(table, DbUtils.DB_PROD_NAME_SYBASE_ASE, "\"", false, false, true);
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
			    + "  and [type] in ('U', 'V') \n"
			    + and_dbname
			    + and_owner
			    + and_table
			    + "";
		
		List<AseTableInfo> tmpList = new ArrayList<>();
		
		sql = conn.quotifySqlString(sql);
		try ( Statement stmnt = conn.createStatement() )
		{
			// Unlimited execution time
			stmnt.setQueryTimeout(0);
			try ( ResultSet rs = stmnt.executeQuery(sql) )
			{
				while(rs.next())
				{
					AseTableInfo ti = new AseTableInfo();
					
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
//		Set<AseTableInfo> retList = new LinkedHashSet<>();
		
		// Loop the record we just found in MonDdlStorage
		for (AseTableInfo ti : tmpList)
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
//				if (StringUtil.hasValue(ti._objectText) && StringUtil.hasValue(ti._extraInfoText))
				if (StringUtil.hasValue(ti._extraInfoText))
				{
					if (getTableAndIndexInfo(conn, ti))
					{
						// Add to the result
						result.add(ti);

						// Append any more information?

						// get TRIGGER information for this table
						getTriggerInfo(conn, ti); // type='TR'
					}

				} // end: has: _objectText && _extraInfoText 
				
			} // end: USER Table

		} // end: loop AseTableInfo
		
//		return retList;
	}
	
	/**
	 * Parse the information from the DdlStorage columns 'objectText' and 'extraInfoText' where various information is stored in.
	 * @param ti
	 * @return
	 */
	private boolean getTableAndIndexInfo(DbxConnection conn, AseTableInfo ti)
	{
		// Parse the values from "sp_spaceused" looking like below (between the ### lines)
		//####################################################################################
		//     index_name size    reserved unused
		//     ---------- ------- -------- ------
		//     MATSET_ND0 1824 KB 2080 KB  256 KB
		//     MATSET_ND2 1008 KB 1180 KB  172 KB
		//     MATSET_ND3 5928 KB 6036 KB  108 KB
		//     name       rowtotal reserved data     index_size unused
		//     ---------- -------- -------- -------- ---------- ------
		//     MATSET_DBF 66932    36084 KB 26776 KB 8760 KB    532 KB		
		//####################################################################################
		// arrayPos:    0     1        2  3     4  5    6  7      8  9
		//####################################################################################
		
		// If we want to parse out the "index names" and *INDEX COLUMNS* ... we can use the below from column "objectText"
		//   |Object has the following indexes
		//   | 
		//   | index_name index_keys                          index_description    index_max_rows_per_page index_fillfactor index_reservepagegap index_created       index_local 
		//   | ---------- ----------------------------------- -------------------- ----------------------- ---------------- -------------------- ------------------- ------------
		//   | MATSET_ND0  M_LABEL, M_TYPE, M_US_TYPE         nonclustered, unique                       0                0                    0 Nov 13 2021  2:38AM Global Index
		//   | MATSET_ND2  M_US_TYPE                          nonclustered                               0                0                    0 Nov 13 2021  2:38AM Global Index
		//   | MATSET_ND3  M_DISPLAY_LABEL, M_TYPE, M_US_TYPE nonclustered, unique                       0                0                    0 Nov 13 2021  2:38AM Global Index
		//   | index_ptn_name       index_ptn_seg index_ptn_comp    
		//   | -------------------- ------------- ------------------
		//   | MATSET_ND0_343374906 default       inherit from index
		//   | MATSET_ND2_343374906 default       inherit from index
		//   | MATSET_ND3_343374906 default       inherit from index
		//   |No defined keys for this object.


		boolean doAdd = false;

		int asePageSize = -1; 
		try { asePageSize = getAsePageSizeFromMonDdlStorage(conn) / 1024; }
		catch(SQLException ignore) {}

		if (StringUtil.hasValue(ti._extraInfoText))
		{
			// Read the content line by line
			List<String> extraInfoList  = StringUtil.readLines(ti._extraInfoText);
//			List<String> objectInfoList = StringUtil.readLines(ti._objectText);

			int indexCount = 0;
			List<AseIndexInfo> indexInfoList = new ArrayList<>();

			for (String line : extraInfoList)
			{
				if (_logger.isDebugEnabled())
					_logger.debug("############## line=|" + line + "|, for dbname='" + ti._dbName + "', schema='" + ti._schemaName + "', table='" + ti._tableName + "'");

				if (line.endsWith(" KB"))
				{
					String[] sa = line.split("[ \t\n\f\r]+");

					if (_logger.isDebugEnabled())
						_logger.debug("####### >>>>>> sa.length=" + sa.length + ". sa[]=" + StringUtil.toCommaStrQuoted(sa) + ", line=|" + line + "|");

					//INDEX entry
					if (sa.length == 7)
					{
						indexCount++;

						AseIndexInfo indexInfo = new AseIndexInfo();
						indexInfo._indexName  = sa[0];
						indexInfo._sizeKb     = StringUtil.parseInt(sa[1], -1);
						indexInfo._sizePages  = indexInfo._sizeKb / asePageSize;
						indexInfo._reservedKb = StringUtil.parseInt(sa[3], -1);
						indexInfo._unusedKb   = StringUtil.parseInt(sa[5], -1);

						// Set indexInfo fields: _keysStr, _keys, _desc, _CreationDate, _ddlTxt
						scrapeIndexInfo(ti.getFullTableName(), indexInfo._indexName, ti._objectText, indexInfo);

						if (_logger.isDebugEnabled())
							_logger.debug("####### >>>>>> INDEX tab=|" + ti.getFullTableName() + "|, index='" + indexInfo._indexName + "', DDL=|" + indexInfo._ddlTxt + "|.");
						
						indexInfoList.add(indexInfo);
					}

					// TABLE entry
					if (sa.length == 10)
					{
//						AseTableInfo ti = new AseTableInfo();
						ti._tableName  = sa[0];
						ti._rowtotal   = StringUtil.parseInt(sa[1], -1);
						ti._reservedKb = StringUtil.parseInt(sa[2], -1);
						ti._dataKb     = StringUtil.parseInt(sa[4], -1);
						ti._dataPages  = ti._dataKb / asePageSize;
						ti._indexKb    = StringUtil.parseInt(sa[6], -1);
						ti._unusedKb   = StringUtil.parseInt(sa[8], -1);
						ti._indexPages = ti._indexKb / asePageSize;

						ti._indexCount = indexCount;
						ti._indexList  = indexInfoList;

						indexCount = 0;
						indexInfoList = new ArrayList<>();

						// get Lock Schema: "allpages", "datapages", "datarows"
						ti._lockSchema = scrapeObjectText("^Lock scheme .*", 2, ti._objectText);

						//retList.add(ti);
						doAdd = true;
					}
				} // end: line.endsWith(" KB")
			} // end: loop lines
			
			// Check for LOB, which is named 't<tablename>'
			AseIndexInfo ii = ti.getIndexInfoForName("t" + ti._tableName);
			if (ii != null)
			{
				ti._lobKb    = ii.getSizeKb();
				ti._lobPages = ti._lobKb / asePageSize;

				ii._desc     = "LOB - Large Object (text/image)";
				ii._keysStr  = "-lob-data-";
				ii._keys     = Arrays.asList("-lob-data-");
				ii._CreationDate = ti._crdate;
			}
		} // end: has _extraInfoText
		
		// also get Statistics how often the indexes was used/accessed during the report period
		// As the index 'ix_DSR_CmObjectActivity_diff_DBName_ObjectName_IndexName' exists, this is a pretty cheap operation (if NOT exists, should we create one?)
		try
		{
			if ( ! DbUtils.checkIfIndexExists(conn, null, null, "CmObjectActivity_diff", "ix_DSR_CmObjectActivity_diff_DBName_ObjectName_IndexName") )
			{
				String sql = conn.quotifySqlString("create index [ix_DSR_CmObjectActivity_diff_DBName_ObjectName_IndexName] on [CmObjectActivity_diff] ([DBName], [ObjectName], [IndexName])");

				long startTime = System.currentTimeMillis();
				try (Statement stmnt = conn.createStatement())
				{
					stmnt.executeUpdate(sql);
					_logger.info("ReportEntry '" + this.getClass().getSimpleName() + "'. Created helper index to support Daily Summary Report. SQL='" + sql + "' ExecTime=" + TimeUtils.msDiffNowToTimeStr(startTime));
				}
				catch (SQLException ex)
				{
					_logger.warn("Problems index to help ReportEntry '" + this.getClass().getSimpleName() + "'. SQL='" + sql + "'. Continuing without the index. Caught: " + ex);
				}
			}
		}
		catch (SQLException ex)
		{
			_logger.warn("Problems checking if index exists to help ReportEntry '" + this.getClass().getSimpleName() + "'. Continuing without the index. Caught: " + ex);
		}

		// NOTE: INDEXES for below is created in: each ReportEntry.getReportingIndexes()
		
		// just to get Column names
		String dummySql = "select * from [CmObjectActivity_diff] where 1 = 2";
		ResultSetTableModel dummyRstm = executeQuery(conn, dummySql, false, "metadata");
		if (dummyRstm == null)
		{
			return false;
		}

		
		// Some columns do not exists in some versions...
		// Lets simulate that they exists, but with some "default" value
		String col_with_diff__Scans                 = "        ,-1 as [Scans] \n";
		String col_with_diff__LockWaits             = "        ,-1 as [LockWaits] \n";
		String col_with_diff__SharedLockWaitTime    = "        ,-1 as [SharedLockWaitTime] \n";
		String col_with_diff__ExclusiveLockWaitTime = "        ,-1 as [ExclusiveLockWaitTime] \n";
		String col_with_diff__UpdateLockWaitTime    = "        ,-1 as [UpdateLockWaitTime] \n";

		String col_with_abs__Scans                  = "        ,-1 as [AbsScans] \n";
		String col_with_abs__LockWaits              = "        ,-1 as [AbsLockWaits] \n";
		String col_with_abs__SharedLockWaitTime     = "        ,-1 as [AbsSharedLockWaitTime] \n";
		String col_with_abs__ExclusiveLockWaitTime  = "        ,-1 as [AbsExclusiveLockWaitTime] \n";
		String col_with_abs__UpdateLockWaitTime     = "        ,-1 as [AbsUpdateLockWaitTime] \n";
		
		if (dummyRstm.hasColumnNoCase("Scans"))
		{
			col_with_diff__Scans                 = "        ,sum([Scans]) as [Scans] \n"; 
			col_with_abs__Scans                  = "        ,[Scans] as [AbsScans] \n"; 
		}

		if (dummyRstm.hasColumnNoCase("LockWaits"))
		{
			col_with_diff__LockWaits             = "        ,sum([LockWaits]) as [LockWaits] \n"; 
			col_with_abs__LockWaits              = "        ,[LockWaits] as [AbsLockWaits] \n"; 
		}
		if (dummyRstm.hasColumnNoCase("SharedLockWaitTime"))
		{
			col_with_diff__SharedLockWaitTime    = "        ,sum([SharedLockWaitTime]) as [SharedLockWaitTime] \n"; 
			col_with_abs__SharedLockWaitTime     = "        ,[SharedLockWaitTime] as [AbsSharedLockWaitTime] \n"; 
		}
		if (dummyRstm.hasColumnNoCase("ExclusiveLockWaitTime"))
		{
			col_with_diff__ExclusiveLockWaitTime = "        ,sum([ExclusiveLockWaitTime]) as [ExclusiveLockWaitTime] \n"; 
			col_with_abs__ExclusiveLockWaitTime  = "        ,[ExclusiveLockWaitTime] as [AbsExclusiveLockWaitTime] \n"; 
		}
		if (dummyRstm.hasColumnNoCase("UpdateLockWaitTime"))
		{
			col_with_diff__UpdateLockWaitTime    = "        ,sum([UpdateLockWaitTime]) as [UpdateLockWaitTime] \n"; 
			col_with_abs__UpdateLockWaitTime     = "        ,[UpdateLockWaitTime] as [AbsUpdateLockWaitTime] \n"; 
		}
		
		// If we want we could probably add the following columns as well
		//  * LogicalReads          -- Total number of times a buffer for this object has been retrieved from a buffer cache without requiring a read from disk.
		//  * PhysicalReads         -- Number of buffers read from disk.
		//  * APFReads              -- Number of APF buffers read from disk.
		//  * (PagesRead)           -- Total number of pages read.
		//  * (PhysicalWrites)      -- Total number of buffers written to disk.
		//  * (PagesWritten)        -- Total number of pages written to disk.
		//  * LockRequests          -- Number of requests for a lock on the object.
		//  * LockWaits             -- Number of times a task waited for an object lock.
		//  * SharedLockWaitTime    -- The total amount of time, in milliseconds, that all tasks spent waiting for a shared lock.
		//  * ExclusiveLockWaitTime -- The total amount of time, in milliseconds, that all tasks spent waiting for an exclusive lock.
		//  * UpdateLockWaitTime    -- The total amount of time, in milliseconds, that all tasks spent waiting for an update lock.
		//  * Updates               -- Number of updates (operations) performed on this object.
		//  * Inserts               -- Number of inserts (operations) performed on this object.
		//  * Deletes               -- Number of deletes (operations) performed on this object.

		// create SQL Statement to get ...
		String sql = ""
			+ "WITH diff as \n"
			+ "( \n"
			+ "    select \n"
			+ "         [DBName] \n"
			+ "        ,[ObjectName] \n"
			+ "        ,[IndexName] \n"
			+ "        ,[IndexID] \n"
			+ "        ,max([ObjectCacheDate])       as [ObjectCacheDate] \n"
			+ "        ,sum([RowsInsUpdDel])         as [RowsInsUpdDel] \n"
			+ "        ,sum([RowsInserted])          as [RowsInserted] \n"
			+ "        ,sum([RowsUpdated])           as [RowsUpdated] \n"
			+ "        ,sum([RowsDeleted])           as [RowsDeleted] \n"
			+ "        ,sum([OptSelectCount])        as [OptSelectCount] \n"
			+ "        ,sum([UsedCount])             as [UsedCount] \n"
			+ "        ,sum([Operations])            as [Operations] \n"
			+          col_with_diff__Scans                   // "        ,sum([Scans]) as [Scans] \n"
			+ "        ,sum([LogicalReads])          as [LogicalReads] \n"
			+ "        ,sum([PhysicalReads])         as [PhysicalReads] \n"
			+ "        ,sum([APFReads])              as [APFReads] \n"
			+          col_with_diff__LockWaits               // "        ,sum([LockWaits])             as [LockWaits] \n"
			+          col_with_diff__SharedLockWaitTime      // "        ,sum([SharedLockWaitTime])    as [SharedLockWaitTime] \n"
			+          col_with_diff__ExclusiveLockWaitTime   // "        ,sum([ExclusiveLockWaitTime]) as [ExclusiveLockWaitTime] \n"
			+          col_with_diff__UpdateLockWaitTime      // "        ,sum([UpdateLockWaitTime])    as [UpdateLockWaitTime] \n"
			+ "    from [CmObjectActivity_diff] \n"
			+ "    where [DBName]     = " + DbUtils.safeStr(ti._dbName)    + " \n"
			+ "      and [ObjectName] = " + DbUtils.safeStr(ti._tableName) + " \n"
			+ "    group by [DBName], [ObjectName], [IndexName], [IndexID] \n"
			+ ") \n"
			+ ", abs as \n"
			+ "( \n"
			+ "    select \n"
			+ "         [DBName] \n"
			+ "        ,[ObjectName] \n"
			+ "        ,[IndexName] \n"
			+ "        ,[RowsInsUpdDel]         as [AbsRowsInsUpdDel] \n"
			+ "        ,[RowsInserted]          as [AbsRowsInserted] \n"
			+ "        ,[RowsUpdated]           as [AbsRowsUpdated] \n"
			+ "        ,[RowsDeleted]           as [AbsRowsDeleted] \n"
			+ "        ,[OptSelectCount]        as [AbsOptSelectCount] \n"
			+ "        ,[UsedCount]             as [AbsUsedCount] \n"
			+ "        ,[Operations]            as [AbsOperations] \n"
			+          col_with_abs__Scans                 // "        ,[Scans] as [AbsScans] \n"
			+ "        ,[LogicalReads]          as [AbsLogicalReads] \n"
			+ "        ,[PhysicalReads]         as [AbsPhysicalReads] \n"
			+ "        ,[APFReads]              as [AbsAPFReads] \n"
			+          col_with_abs__LockWaits             // "        ,[LockWaits]             as [AbsLockWaits] \n"
			+          col_with_abs__SharedLockWaitTime    // "        ,[SharedLockWaitTime]    as [AbsSharedLockWaitTime] \n"
			+          col_with_abs__ExclusiveLockWaitTime // "        ,[ExclusiveLockWaitTime] as [AbsExclusiveLockWaitTime] \n"
			+          col_with_abs__UpdateLockWaitTime    // "        ,[UpdateLockWaitTime]    as [AbsUpdateLockWaitTime] \n"
			+ "    from [CmObjectActivity_abs] \n"
			+ "    where [SessionSampleTime] = (select max([SessionSampleTime]) from [CmObjectActivity_abs]) \n"
			+ "      and [DBName]     = " + DbUtils.safeStr(ti._dbName)    + " \n"
			+ "      and [ObjectName] = " + DbUtils.safeStr(ti._tableName) + " \n"
			+ ") \n"
			+ "select \n"
			+ "     diff.[DBName] \n"
			+ "    ,diff.[ObjectName] \n"
			+ "    ,diff.[IndexName] \n"
			+ "    ,diff.[IndexID] \n"

			+ "    ,diff.[RowsInsUpdDel] \n"
			+ "    ,diff.[RowsInserted] \n"
			+ "    ,diff.[RowsUpdated] \n"
			+ "    ,diff.[RowsDeleted] \n"
			+ "    ,diff.[OptSelectCount] \n"
			+ "    ,diff.[UsedCount] \n"
			+ "    ,diff.[Operations] \n"
			+ "    ,diff.[Scans] \n"
			+ "    ,diff.[ObjectCacheDate] \n"
			+ "    ,diff.[LogicalReads] \n"
			+ "    ,diff.[PhysicalReads] \n"
			+ "    ,diff.[APFReads] \n"
			+ "    ,diff.[LockWaits] \n"
			+ "    ,diff.[SharedLockWaitTime] \n"
			+ "    ,diff.[ExclusiveLockWaitTime] \n"
			+ "    ,diff.[UpdateLockWaitTime] \n"

			+ "    ,abs. [AbsRowsInsUpdDel] \n"
			+ "    ,abs. [AbsRowsInserted] \n"
			+ "    ,abs. [AbsRowsUpdated] \n"
			+ "    ,abs. [AbsRowsDeleted] \n"
			+ "    ,abs. [AbsOptSelectCount] \n"
			+ "    ,abs. [AbsUsedCount] \n"
			+ "    ,abs. [AbsOperations] \n"
			+ "    ,abs. [AbsScans] \n"
			+ "    ,abs .[AbsLogicalReads] \n"
			+ "    ,abs .[AbsPhysicalReads] \n"
			+ "    ,abs .[AbsAPFReads] \n"
			+ "    ,abs .[AbsLockWaits] \n"
			+ "    ,abs .[AbsSharedLockWaitTime] \n"
			+ "    ,abs .[AbsExclusiveLockWaitTime] \n"
			+ "    ,abs .[AbsUpdateLockWaitTime] \n"
			+ "from diff \n"
			+ "left outer join abs on diff.[DBName]     = abs.[DBName] \n"
			+ "                   and diff.[ObjectName] = abs.[ObjectName] \n"
			+ "                   and diff.[IndexName]  = abs.[IndexName] \n"
			+ "order by diff.[IndexID] \n"
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
				//	String    DBName                   = rs.getString   (1);
				//	String    ObjectName               = rs.getString   (2);
					String    IndexName                = rs.getString   (3);
					int       IndexID                  = rs.getInt      (4);
					
					long      RowsInsUpdDel            = rs.getLong     (5);
					long      RowsInserted             = rs.getLong     (6);
					long      RowsUpdated              = rs.getLong     (7);
					long      RowsDeleted              = rs.getLong     (8);
					long      OptSelectCount           = rs.getLong     (9);
					long      UsedCount                = rs.getLong     (10);
					long      Operations               = rs.getLong     (11);
					long      Scans                    = rs.getLong     (12);
					Timestamp ObjectCacheDate          = rs.getTimestamp(13);
					long      LogicalReads             = rs.getLong     (14);
					long      PhysicalReads            = rs.getLong     (15);
					long      APFReads                 = rs.getLong     (16);
					long      LockWaits                = rs.getLong     (17);
					long      SharedLockWaitTime       = rs.getLong     (18);
					long      ExclusiveLockWaitTime    = rs.getLong     (19);
					long      UpdateLockWaitTime       = rs.getLong     (20);

					long      AbsRowsInsUpdDel         = rs.getLong     (21);
					long      AbsRowsInserted          = rs.getLong     (22);
					long      AbsRowsUpdated           = rs.getLong     (23);
					long      AbsRowsDeleted           = rs.getLong     (24);
					long      AbsOptSelectCount        = rs.getLong     (25);
					long      AbsUsedCount             = rs.getLong     (26);
					long      AbsOperations            = rs.getLong     (27);
					long      AbsScans                 = rs.getLong     (28);
					long      AbsLogicalReads          = rs.getLong     (29);
					long      AbsPhysicalReads         = rs.getLong     (30);
					long      AbsAPFReads              = rs.getLong     (31);
					long      AbsLockWaits             = rs.getLong     (32);
					long      AbsSharedLockWaitTime    = rs.getLong     (33);
					long      AbsExclusiveLockWaitTime = rs.getLong     (34);
					long      AbsUpdateLockWaitTime    = rs.getLong     (35);
					
					AseIndexInfo indexEntry = ti.getIndexInfoForName(IndexName);

					if (indexEntry != null)
					{
						indexEntry._IndexID                  = IndexID;
						indexEntry._ObjectCacheDate          = ObjectCacheDate;
						indexEntry._RowsInsUpdDel            = RowsInsUpdDel;
						indexEntry._RowsInserted             = RowsInserted;
						indexEntry._RowsUpdated              = RowsUpdated;
						indexEntry._RowsDeleted              = RowsDeleted;
						indexEntry._OptSelectCount           = OptSelectCount;
						indexEntry._UsedCount                = UsedCount;
						indexEntry._Operations               = Operations;
						indexEntry._Scans                    = Scans;
						indexEntry._LogicalReads             = LogicalReads         ;
						indexEntry._PhysicalReads            = PhysicalReads        ;
						indexEntry._APFReads                 = APFReads             ;
						indexEntry._LockWaits                = LockWaits            ;
						indexEntry._SharedLockWaitTime       = SharedLockWaitTime   ;
						indexEntry._ExclusiveLockWaitTime    = ExclusiveLockWaitTime;
						indexEntry._UpdateLockWaitTime       = UpdateLockWaitTime   ;

						indexEntry._AbsRowsInsUpdDel         = AbsRowsInsUpdDel;
						indexEntry._AbsRowsInserted          = AbsRowsInserted;
						indexEntry._AbsRowsUpdated           = AbsRowsUpdated;
						indexEntry._AbsRowsDeleted           = AbsRowsDeleted;
						indexEntry._AbsOptSelectCount        = AbsOptSelectCount;
						indexEntry._AbsUsedCount             = AbsUsedCount;
						indexEntry._AbsOperations            = AbsOperations;
						indexEntry._AbsScans                 = AbsScans;
						indexEntry._AbsLogicalReads          = AbsLogicalReads         ;
						indexEntry._AbsPhysicalReads         = AbsPhysicalReads        ;
						indexEntry._AbsAPFReads              = AbsAPFReads             ;
						indexEntry._AbsLockWaits             = AbsLockWaits            ;
						indexEntry._AbsSharedLockWaitTime    = AbsSharedLockWaitTime   ;
						indexEntry._AbsExclusiveLockWaitTime = AbsExclusiveLockWaitTime;
						indexEntry._AbsUpdateLockWaitTime    = AbsUpdateLockWaitTime   ;
					}
					else
					{
						indexEntry = new AseIndexInfo();

						indexEntry._indexName          = IndexName;
						indexEntry._sizeKb             = -1;
						indexEntry._reservedKb         = -1;
						indexEntry._unusedKb           = -1;
						indexEntry._keysStr            = "-unknown-";
					//	indexEntry._keys               = null;
						indexEntry._desc               = "-unknown-";
						indexEntry._ddlTxt             = "";

						indexEntry._IndexID                  = IndexID;
						indexEntry._ObjectCacheDate          = ObjectCacheDate;

						indexEntry._RowsInsUpdDel            = RowsInsUpdDel        ;
						indexEntry._RowsInserted             = RowsInserted         ;
						indexEntry._RowsUpdated              = RowsUpdated          ;
						indexEntry._RowsDeleted              = RowsDeleted          ;
						indexEntry._OptSelectCount           = OptSelectCount       ;
						indexEntry._UsedCount                = UsedCount            ;
						indexEntry._Operations               = Operations           ;
						indexEntry._Scans                    = Scans                ;
						indexEntry._LogicalReads             = LogicalReads         ;
						indexEntry._PhysicalReads            = PhysicalReads        ;
						indexEntry._APFReads                 = APFReads             ;
						indexEntry._LockWaits                = LockWaits            ;
						indexEntry._SharedLockWaitTime       = SharedLockWaitTime   ;
						indexEntry._ExclusiveLockWaitTime    = ExclusiveLockWaitTime;
						indexEntry._UpdateLockWaitTime       = UpdateLockWaitTime   ;

						indexEntry._AbsRowsInsUpdDel         = AbsRowsInsUpdDel        ;
						indexEntry._AbsRowsInserted          = RowsInserted            ;
						indexEntry._AbsRowsUpdated           = RowsUpdated             ;
						indexEntry._AbsRowsDeleted           = RowsDeleted             ;
						indexEntry._AbsOptSelectCount        = AbsOptSelectCount       ;
						indexEntry._AbsUsedCount             = AbsUsedCount            ;
						indexEntry._AbsOperations            = AbsOperations           ;
						indexEntry._AbsScans                 = AbsScans                ;
						indexEntry._AbsLogicalReads          = AbsLogicalReads         ;
						indexEntry._AbsPhysicalReads         = AbsPhysicalReads        ;
						indexEntry._AbsAPFReads              = AbsAPFReads             ;
						indexEntry._AbsLockWaits             = AbsLockWaits            ;
						indexEntry._AbsSharedLockWaitTime    = AbsSharedLockWaitTime   ;
						indexEntry._AbsExclusiveLockWaitTime = AbsExclusiveLockWaitTime;
						indexEntry._AbsUpdateLockWaitTime    = AbsUpdateLockWaitTime   ;

						// Copy some "stuff" from current Table Information
						if ("DATA".equals(indexEntry._indexName))
						{
							indexEntry._sizeKb          = ti._dataKb;
							indexEntry._sizePages       = ti._dataKb / asePageSize;

							indexEntry._reservedKb      = ti._reservedKb;
							indexEntry._unusedKb        = ti._unusedKb;

							indexEntry._keysStr         = "-data-";
							indexEntry._desc            = "-data-";

							indexEntry._CreationDate    = ti._crdate;
						}

						ti._indexList.add(indexEntry);
					}
				}
			}
		}
		catch(SQLException ex)
		{
			_logger.warn("Problems getting INDEX Information at 'getTableAndIndexInfo()', for dbname='" + ti._dbName + "', owner='" + ti._schemaName + "', table='" + ti._tableName + "'.", ex);
			//throw ex;
		}
		
		return doAdd;
	}
// select * from "PUBLIC"."CmObjectActivity_diff" where 1=2
// go prsi
//	RS> Col# Label                 JDBC Type Name           Guessed DBMS type Source Table                                       
//	RS> ---- --------------------- ------------------------ ----------------- ---------------------------------------------------
//	RS> 1    SessionStartTime      java.sql.Types.TIMESTAMP TIMESTAMP         PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
//	RS> 2    SessionSampleTime     java.sql.Types.TIMESTAMP TIMESTAMP         PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
//	RS> 3    CmSampleTime          java.sql.Types.TIMESTAMP TIMESTAMP         PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
//	RS> 4    CmSampleMs            java.sql.Types.INTEGER   INTEGER           PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
//	RS> 5    CmNewDiffRateRow      java.sql.Types.TINYINT   TINYINT           PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
//	RS> 6    DBID                  java.sql.Types.INTEGER   INTEGER           PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
//	RS> 7    ObjectID              java.sql.Types.INTEGER   INTEGER           PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
//	RS> 8    DBName                java.sql.Types.VARCHAR   VARCHAR(30)       PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
//	RS> 9    ObjectName            java.sql.Types.VARCHAR   VARCHAR(259)      PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
//	RS> 10   IndexID               java.sql.Types.INTEGER   INTEGER           PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
//	RS> 11   IndexName             java.sql.Types.VARCHAR   VARCHAR(30)       PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
//	RS> 12   LockScheme            java.sql.Types.VARCHAR   VARCHAR(30)       PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
//	RS> 13   Remark                java.sql.Types.VARCHAR   VARCHAR(60)       PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
//	RS> 14   Scans                 java.sql.Types.INTEGER   INTEGER           PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
//	RS> 15   LastScanDate          java.sql.Types.TIMESTAMP TIMESTAMP         PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
//	RS> 16   LastScanDateDiff      java.sql.Types.INTEGER   INTEGER           PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
//	RS> 17   LockRequests          java.sql.Types.INTEGER   INTEGER           PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
//	RS> 18   LockWaits             java.sql.Types.INTEGER   INTEGER           PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
//	RS> 19   LockContPct           java.sql.Types.DECIMAL   DECIMAL(10,1)     PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
//	RS> 20   SharedLockWaitTime    java.sql.Types.INTEGER   INTEGER           PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
//	RS> 21   ExclusiveLockWaitTime java.sql.Types.INTEGER   INTEGER           PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
//	RS> 22   UpdateLockWaitTime    java.sql.Types.INTEGER   INTEGER           PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
//	RS> 23   NumLevel0Waiters      java.sql.Types.INTEGER   INTEGER           PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
//	RS> 24   AvgLevel0WaitTime     java.sql.Types.REAL      REAL              PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
//	RS> 25   LrPerScan             java.sql.Types.DECIMAL   DECIMAL(14,1)     PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
//	RS> 26   LogicalReads          java.sql.Types.INTEGER   INTEGER           PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
//	RS> 27   PhysicalReads         java.sql.Types.INTEGER   INTEGER           PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
//	RS> 28   APFReads              java.sql.Types.INTEGER   INTEGER           PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
//	RS> 29   PagesRead             java.sql.Types.INTEGER   INTEGER           PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
//	RS> 30   IOSize1Page           java.sql.Types.INTEGER   INTEGER           PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
//	RS> 31   IOSize2Pages          java.sql.Types.INTEGER   INTEGER           PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
//	RS> 32   IOSize4Pages          java.sql.Types.INTEGER   INTEGER           PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
//	RS> 33   IOSize8Pages          java.sql.Types.INTEGER   INTEGER           PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
//	RS> 34   PhysicalWrites        java.sql.Types.INTEGER   INTEGER           PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
//	RS> 35   PagesWritten          java.sql.Types.INTEGER   INTEGER           PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
//	RS> 36   UsedCount             java.sql.Types.INTEGER   INTEGER           PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
//	RS> 37   Operations            java.sql.Types.INTEGER   INTEGER           PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
//	RS> 38   TabRowCount           java.sql.Types.BIGINT    BIGINT            PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
//	RS> 39   UsageInMb             java.sql.Types.INTEGER   INTEGER           PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
//	RS> 40   UsageInKb             java.sql.Types.INTEGER   INTEGER           PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
//	RS> 41   NumUsedPages          java.sql.Types.BIGINT    BIGINT            PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
//	RS> 42   RowsPerPage           java.sql.Types.DECIMAL   DECIMAL(11,1)     PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
//	RS> 43   RowsInsUpdDel         java.sql.Types.BIGINT    BIGINT            PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
//	RS> 44   RowsInserted          java.sql.Types.INTEGER   INTEGER           PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
//	RS> 45   RowsDeleted           java.sql.Types.INTEGER   INTEGER           PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
//	RS> 46   RowsUpdated           java.sql.Types.INTEGER   INTEGER           PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
//	RS> 47   OptSelectCount        java.sql.Types.INTEGER   INTEGER           PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
//	RS> 48   MaxInsRowsInXact      java.sql.Types.INTEGER   INTEGER           PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
//	RS> 49   MaxUpdRowsInXact      java.sql.Types.INTEGER   INTEGER           PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
//	RS> 50   MaxDelRowsInXact      java.sql.Types.INTEGER   INTEGER           PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
//	RS> 51   Inserts               java.sql.Types.INTEGER   INTEGER           PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
//	RS> 52   Updates               java.sql.Types.INTEGER   INTEGER           PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
//	RS> 53   Deletes               java.sql.Types.INTEGER   INTEGER           PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
//	RS> 54   LastInsertDateDiff    java.sql.Types.INTEGER   INTEGER           PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
//	RS> 55   LastUpdateDateDiff    java.sql.Types.INTEGER   INTEGER           PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
//	RS> 56   LastDeleteDateDiff    java.sql.Types.INTEGER   INTEGER           PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
//	RS> 57   LastInsertDate        java.sql.Types.TIMESTAMP TIMESTAMP         PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
//	RS> 58   LastUpdateDate        java.sql.Types.TIMESTAMP TIMESTAMP         PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
//	RS> 59   LastDeleteDate        java.sql.Types.TIMESTAMP TIMESTAMP         PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
//	RS> 60   HkgcRequests          java.sql.Types.INTEGER   INTEGER           PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
//	RS> 61   HkgcPending           java.sql.Types.INTEGER   INTEGER           PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
//	RS> 62   HkgcOverflows         java.sql.Types.INTEGER   INTEGER           PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
//	RS> 63   HkgcRequestsDcomp     java.sql.Types.INTEGER   INTEGER           PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
//	RS> 64   HkgcPendingDcomp      java.sql.Types.INTEGER   INTEGER           PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
//	RS> 65   HkgcOverflowsDcomp    java.sql.Types.INTEGER   INTEGER           PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
//	RS> 66   PRSSelectCount        java.sql.Types.INTEGER   INTEGER           PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
//	RS> 67   LastPRSSelectDate     java.sql.Types.TIMESTAMP TIMESTAMP         PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
//	RS> 68   PRSRewriteCount       java.sql.Types.INTEGER   INTEGER           PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
//	RS> 69   LastPRSRewriteDate    java.sql.Types.TIMESTAMP TIMESTAMP         PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
//	RS> 70   ObjectCacheDate       java.sql.Types.TIMESTAMP TIMESTAMP         PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
//	RS> 71   LastOptSelectDate     java.sql.Types.TIMESTAMP TIMESTAMP         PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
//	RS> 72   LastUsedDate          java.sql.Types.TIMESTAMP TIMESTAMP         PROD_A1_ASE_2022-03-03.PUBLIC.CmObjectActivity_diff
	private static String scrapeIndexInfo(String tableName, String indexName, String objectInfoStr, AseIndexInfo indexInfo)
	{
		if (StringUtil.isNullOrBlank(objectInfoStr))
			return "";

		if (StringUtil.isNullOrBlank(indexName))
			throw new IllegalArgumentException("scrapeIndexInfo(): indexName can't be null or ''.");
		
		List<String> objectInfoList = StringUtil.readLines(objectInfoStr);

		// 1: get row with "index_name"
		// 2: get row after that: "---------- ------------- ---------", which is a length indicator of "where" to substring 
		// 4: Get the correct index name
		// 5: extract indexName, indexKeys, indexDesc by using the delimiters ---- ---- ---- positions
		// 6: construct a "guessed": create index statement...

		// The input might look something like this....
		//   |...rows deleted...
		//   |Object has the following indexes
		//   | 
		//   | index_name index_keys                          index_description    index_max_rows_per_page index_fillfactor index_reservepagegap index_created       index_local 
		//   | ---------- ----------------------------------- -------------------- ----------------------- ---------------- -------------------- ------------------- ------------
		//   | MATSET_ND0  M_LABEL, M_TYPE, M_US_TYPE         nonclustered, unique                       0                0                    0 Nov 13 2021  2:38AM Global Index
		//   | MATSET_ND2  M_US_TYPE                          nonclustered                               0                0                    0 Nov 13 2021  2:38AM Global Index
		//   | MATSET_ND3  M_DISPLAY_LABEL, M_TYPE, M_US_TYPE nonclustered, unique                       0                0                    0 Nov 13 2021  2:38AM Global Index
		//   | index_ptn_name       index_ptn_seg index_ptn_comp    
		//   | -------------------- ------------- ------------------
		//   | MATSET_ND0_343374906 default       inherit from index
		//   | MATSET_ND2_343374906 default       inherit from index
		//   | MATSET_ND3_343374906 default       inherit from index
		//   |No defined keys for this object.
		//   |...rows deleted...

		// get "index_name "
		String indexNameHeadLine    = null;
		int    indexNameHeadLineNum = -1;
		for (int r=0; r<objectInfoList.size(); r++)
		{
			if (objectInfoList.get(r).startsWith("index_name ")) // note the " " (space) needs to be here... since the line is not yet split
			{
				indexNameHeadLine    = objectInfoList.get(r);
				indexNameHeadLineNum = r;
				break;
			}
		}

		// get first line after "index_name ", which should be "--------"
		String indexNameLineHeadSep    = null; 
		int    indexNameLineHeadSepNum = indexNameHeadLineNum + 1;
		if (indexNameLineHeadSepNum < objectInfoList.size()) 
			indexNameLineHeadSep = objectInfoList.get(indexNameLineHeadSepNum);
		if (indexNameLineHeadSep != null && !indexNameLineHeadSep.startsWith("---"))
			indexNameLineHeadSep = null;

		// get index *NAME* line
		String indexNameEntryLine    = null;
//		int    indexNameEntryLineNum = -1;
		for (int r=0; r<objectInfoList.size(); r++)
		{
			if (objectInfoList.get(r).startsWith(indexName + " ")) // note the " " (space) needs to be here... since the line is not yet split
			{
				indexNameEntryLine = objectInfoList.get(r);
//				indexNameEntryLineNum = r;
				break;
			}
		}

		// Scrape columns: index_name, index_keys, index_description, index_created
		if (indexNameHeadLine != null && indexNameLineHeadSep != null && indexNameEntryLine != null)
		{
			String xIndexName = scrapeStringForColumn("index_name"       , indexNameEntryLine, indexNameHeadLine, indexNameLineHeadSep);
			String xIndexKeys = scrapeStringForColumn("index_keys"       , indexNameEntryLine, indexNameHeadLine, indexNameLineHeadSep);
			String xIndexDesc = scrapeStringForColumn("index_description", indexNameEntryLine, indexNameHeadLine, indexNameLineHeadSep);
			String xIndexCrTs = scrapeStringForColumn("index_created"    , indexNameEntryLine, indexNameHeadLine, indexNameLineHeadSep);

			// parse index creation time
			Timestamp xCrTs = null;
			if (StringUtil.hasValue(xIndexCrTs))
			{
				try
				{
					SimpleDateFormat sdf = new SimpleDateFormat("MMM dd yyyy hh:mmaa");
					Date date = sdf.parse(xIndexCrTs);
					if (date != null)
						xCrTs = new Timestamp(date.getTime());
				}
				catch(ParseException ex) 
				{
					_logger.warn("Problem parsing CreationDate for table='" +  tableName + "', index='" + indexName + "', xIndexCrTs='" + xIndexCrTs + "'. Skipping and continuing... Caught: " + ex);
				}
			}

			// Compose the DDL
			String unique       = xIndexDesc.contains  ("unique")       ?  "unique "       : "";
			String nonclustered = xIndexDesc.startsWith("nonclustered") ?  "nonclustered " : "";
			String clustered    = xIndexDesc.startsWith("clustered")    ?  "clustered "    : "";

			String ddlText = "create " + unique + nonclustered + clustered + "index " + xIndexName + " on " + tableName + "(" + xIndexKeys + ")";
			
			// Set the values in the passed AseIndexInfo
			if (indexInfo != null)
			{
				indexInfo._keysStr      = xIndexKeys;
				indexInfo._keys         = StringUtil.commaStrToList(xIndexKeys, true);
				indexInfo._desc         = xIndexDesc;
				indexInfo._CreationDate = xCrTs;
				indexInfo._ddlTxt       = ddlText;
			}
			return ddlText;
		}
		
		return "";
	}
	/**
	 * <pre>
	 *   index_name index_keys                          index_description    index_max_rows_per_page index_fillfactor index_reservepagegap index_created       index_local |
	 *   ---------- ----------------------------------- -------------------- ----------------------- ---------------- -------------------- ------------------- ------------|
	 *   MATSET_ND0  M_LABEL, M_TYPE, M_US_TYPE         nonclustered, unique                       0                0                    0 Nov 13 2021  2:38AM Global Index|
	 * </pre>
	 *
	 * @param colName     name of the column we want to extract
	 * @param entryLine   The data "row" that we want to scrape a column from
	 * @param headLine    the table column heads
	 * @param headSepLine the line with all the column separators <code>-------- --------- --------</code>
	 * @return The data in the desired column. NULL if something unexpected happened!
	 */
	private static String scrapeStringForColumn(String colName, String entryLine, String headLine, String headSepLine)
	{
		String[] saHead = headLine   .split("[ \t\n\f\r]+");
		String[] saSep  = headSepLine.split("[ \t\n\f\r]+");

		if (saHead.length != saSep.length)
		{
			System.out.println("getStringForPos(): saHead.length=" + saHead.length + ", saSep.length=" + saSep.length + ". the length of the two arrays must mastch in size");
			System.out.println("                   headLine=|" + headLine + "|, headSepLine=|" + headSepLine + "|.");
			return null;
		}

		// Get "colName" position
		int colNameSaPos = -1;
		for (int i=0; i<saHead.length; i++)
		{
			if (colName.equals(saHead[i]))
			{
				colNameSaPos = i;
				break;
			}
		}
		if (colNameSaPos == -1)
		{
			System.out.println("getStringForPos(): colName='" + colName + "' was NOT found. headLine=|" + headLine + "|.");
			return null;
		}

		// get start/stop position for the desired column!
		int startPos = -1;
		int endPos   = -1;

		for (int i=0; i<=colNameSaPos; i++)
		{
			startPos = endPos + 1;
			endPos   = startPos + saSep[i].length();
		}

		if (_logger.isDebugEnabled())
			_logger.debug("scrapeStringForColumn(): colName='" + colName + "', colNameSaPos=" + colNameSaPos + ", startPos=" + startPos + ", endPos=" + endPos + ", entryLine.length()=" + entryLine.length());

		if (startPos != -1 && endPos > startPos && endPos < entryLine.length())
		{
			String retStr = entryLine.substring(startPos, endPos).trim();
			return retStr;
		}
		
		return null;
	}
//    public static void main(String[] args)
//    {
//    	String indexNameHeadLine    = "index_name index_keys                          index_description    index_max_rows_per_page index_fillfactor index_reservepagegap index_created       index_local";
//    	String indexNameLineHeadSep = "---------- ----------------------------------- -------------------- ----------------------- ---------------- -------------------- ------------------- ------------";
//    	String indexNameEntryLine   = "MATSET_ND0  M_LABEL, M_TYPE, M_US_TYPE         nonclustered, unique                       0                0                    0 Nov 13 2021  2:38AM Global Index";
//    
//    	String xIndexName = scrapeStringForColumn("index_name"       , indexNameEntryLine, indexNameHeadLine, indexNameLineHeadSep);
//    	String xIndexKeys = scrapeStringForColumn("index_keys"       , indexNameEntryLine, indexNameHeadLine, indexNameLineHeadSep);
//    	String xIndexDesc = scrapeStringForColumn("index_description", indexNameEntryLine, indexNameHeadLine, indexNameLineHeadSep);
//    	String xIndexCrTs = scrapeStringForColumn("index_created"    , indexNameEntryLine, indexNameHeadLine, indexNameLineHeadSep);
//    
//    	System.out.println("=================== xIndexName='" + xIndexName + "'");
//    	System.out.println("=================== xIndexKeys='" + xIndexKeys + "'");
//    	System.out.println("=================== xIndexDesc='" + xIndexDesc + "'");
//    	System.out.println("=================== xIndexCrTs='" + xIndexCrTs + "'");
//    }

	/**
	 * Scrape the input string 'objectText' and get first matching line, then get a specific word of that line
	 * 
	 * @param rowRegEx       Used to <b>match<b/> a specific row to get data from
	 * @param wordIndex      (start at 0), which word on the first found line should we extract (note: it respects quotes strings as <i>one</i> word)
	 * @param objectText     input string to search in
	 * @return String of the word ("" if not found, or the found row has less words that the desired wordIndex)
	 */
	private static String scrapeObjectText(String rowRegEx, int wordIndex, String objectText)
	{
		// Early exit
		if (StringUtil.isNullOrBlank(objectText))
			return "";

		// Convert to a List of "lines"
		List<String> objectInfoList = StringUtil.readLines(objectText);

		// Do the work
		return scrapeObjectText(rowRegEx, wordIndex, objectInfoList);
	}

	/**
	 * Scrape the input List of Strings and get first matching line, then get a specific word of that line
	 * 
	 * @param rowRegEx       Used to <b>match<b/> a specific row to get data from
	 * @param wordIndex      (start at 0), which word on the first found line should we extract (note: it respects quotes strings as <i>one</i> word)
	 * @param objectTextList List if strings to search in
	 * @return String of the word ("" if not found, or the found row has less words that the desired wordIndex)
	 */
	private static String scrapeObjectText(String rowRegEx, int wordIndex, List<String> objectTextList)
	{
		// Early exit
		if (objectTextList == null)   return "";
		if (objectTextList.isEmpty()) return "";

		for (String line : objectTextList)
		{
			if (line.matches(rowRegEx))
			{
				String tmp = StringUtil.wordRespectQuotes(line, wordIndex);
				return tmp == null ? "" : tmp;
			}
		}
		
		return "";
	}
	
	private void getTriggerInfo(DbxConnection conn, AseTableInfo tableInfo)
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
		
		List<AseTableInfo> tmpList = new ArrayList<>();
		
		sql = conn.quotifySqlString(sql);
		try ( Statement stmnt = conn.createStatement() )
		{
			// Unlimited execution time
			stmnt.setQueryTimeout(0);
			try ( ResultSet rs = stmnt.executeQuery(sql) )
			{
				while(rs.next())
				{
					AseTableInfo ti = new AseTableInfo();
					
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
		for (AseTableInfo ti : tmpList)
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
		} // end: loop AseTableInfo

		// Set the text
		return sb.toString();
	}
		
	
	public static class AseIndexInfo
	{
		private String       _indexName;
		private int          _sizeKb            = -1;
		private int          _sizePages         = -1;
		private int          _reservedKb        = -1;
		private int          _unusedKb          = -1;
		private String       _keysStr;
		private List<String> _keys;
		private String       _desc;
		private String       _ddlTxt;
		private int          _IndexID           = -1;
		private Timestamp    _CreationDate;
		private Timestamp    _ObjectCacheDate;
		private long         _RowsInsUpdDel         = -1;
		private long         _OptSelectCount        = -1;
		private long         _UsedCount             = -1;
		private long         _Operations            = -1;
		private long         _Scans                 = -1;
		private long         _RowsInserted          = -1;
		private long         _RowsUpdated           = -1;
		private long         _RowsDeleted           = -1;
		private long         _LogicalReads          = -1;
		private long         _PhysicalReads         = -1;
		private long         _APFReads              = -1;
		private long         _LockWaits             = -1;
		private long         _SharedLockWaitTime    = -1;
		private long         _ExclusiveLockWaitTime = -1;
		private long         _UpdateLockWaitTime    = -1;
		
		
		private long         _AbsRowsInsUpdDel         = -1;
		private long         _AbsOptSelectCount        = -1;
		private long         _AbsUsedCount             = -1;
		private long         _AbsOperations            = -1;
		private long         _AbsScans                 = -1;
		private long         _AbsRowsInserted          = -1;
		private long         _AbsRowsUpdated           = -1;
		private long         _AbsRowsDeleted           = -1;
		private long         _AbsLogicalReads          = -1;
		private long         _AbsPhysicalReads         = -1;
		private long         _AbsAPFReads              = -1;
		private long         _AbsLockWaits             = -1;
		private long         _AbsSharedLockWaitTime    = -1;
		private long         _AbsExclusiveLockWaitTime = -1;
		private long         _AbsUpdateLockWaitTime    = -1;

		public String       getIndexName             () { return _indexName; }
		public int          getSizeKb                () { return _sizeKb; }
		public int          getSizePages             () { return _sizePages; }
		public int          getReservedKb            () { return _reservedKb; }
		public int          getUnusedKb              () { return _unusedKb; }
		public String       getKeysStr               () { return _keysStr; }
		public List<String> getKeys                  () { return _keys; }
		public String       getDescription           () { return _desc; }
		public String       getDdlText               () { return _ddlTxt; }
                                                     
		public int          getIndexID               () { return _IndexID;            }
		public Timestamp    getCreationDate          () { return _CreationDate;       }
		public String       getCreationDateStr       () { return _CreationDate    == null ? "--not-found--" : _CreationDate.toString(); }
		public Timestamp    getCacheDate             () { return _ObjectCacheDate;    }
		public String       getCacheDateStr          () { return _ObjectCacheDate == null ? "--not-found--" : _ObjectCacheDate.toString(); }
		                                             
		public long         getRowsInsUpdDel         () { return _RowsInsUpdDel;     }
		public long         getOptSelectCount        () { return _OptSelectCount;     }
		public long         getUsedCount             () { return _UsedCount;          }
		public long         getOperations            () { return _Operations;         }
		public long         getScans                 () { return _Scans;              }
		public long         getRowsInserted          () { return _RowsInserted         ; }
		public long         getRowsUpdated           () { return _RowsUpdated          ; }
		public long         getRowsDeleted           () { return _RowsDeleted          ; }
		public long         getLogicalReads          () { return _LogicalReads         ; }
		public long         getPhysicalReads         () { return _PhysicalReads        ; }
		public long         getAPFReads              () { return _APFReads             ; }
		public long         getLockWaits             () { return _LockWaits            ; }
		public long         getSharedLockWaitTime    () { return _SharedLockWaitTime   ; }
		public long         getExclusiveLockWaitTime () { return _ExclusiveLockWaitTime; }
		public long         getUpdateLockWaitTime    () { return _UpdateLockWaitTime   ; }

		public long         getAbsRowsInsUpdDel         () { return _AbsRowsInsUpdDel;   }
		public long         getAbsOptSelectCount        () { return _AbsOptSelectCount;  }
		public long         getAbsUsedCount             () { return _AbsUsedCount;       }
		public long         getAbsOperations            () { return _AbsOperations;      }
		public long         getAbsScans                 () { return _AbsScans;           }
		public long         getAbsRowsInserted          () { return _AbsRowsInserted         ; }
		public long         getAbsRowsUpdated           () { return _AbsRowsUpdated          ; }
		public long         getAbsRowsDeleted           () { return _AbsRowsDeleted          ; }
		public long         getAbsLogicalReads          () { return _AbsLogicalReads         ; }
		public long         getAbsPhysicalReads         () { return _AbsPhysicalReads        ; }
		public long         getAbsAPFReads              () { return _AbsAPFReads             ; }
		public long         getAbsLockWaits             () { return _AbsLockWaits            ; }
		public long         getAbsSharedLockWaitTime    () { return _AbsSharedLockWaitTime   ; }
		public long         getAbsExclusiveLockWaitTime () { return _AbsExclusiveLockWaitTime; }
		public long         getAbsUpdateLockWaitTime    () { return _AbsUpdateLockWaitTime   ; }

		public double getSizeMb()     { return MathUtils.round(getSizeKb()     / 1024.0, 1); }
		public double getReservedMb() { return MathUtils.round(getReservedKb() / 1024.0, 1); }
		public double getUnusedMb()   { return MathUtils.round(getUnusedKb()   / 1024.0, 1); }
	}

	public static class AseTableInfo
	{
		private String   _dbName;
		private String   _schemaName;
		private String   _tableName;
		public String    _type;
		public Timestamp _crdate;
		private Timestamp _sampleTime;
		private String   _extraInfoText;  // sp_spaceused tabname, 1
		private String   _objectText;     // sp_help tabname
		private String   _childObjects;   // Type:name, Type:name, Type:name
		private String   _triggersText;   // Text of any trigger definitions
		private int      _rowtotal;
		private int      _reservedKb;
		private int      _dataKb;
		private int      _indexKb;
		private int      _lobKb = -1;
		private int      _dataPages;
		private int      _indexPages;
		private int      _lobPages;
		private int      _unusedKb;
		private String   _lockSchema;
		private int      _indexCount;
		private List<AseIndexInfo> _indexList = new ArrayList<>();

		public List<String> _viewReferences; // if _type == "V", this this will hold; table/views this view references
		
		public AseIndexInfo getIndexInfoForName(String indexName)
		{
			if (StringUtil.isNullOrBlank(indexName))
				return null;

			for (AseIndexInfo e : _indexList)
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
		
		public String    getDbName()     { return _dbName; }
		public String    getSchemaName() { return _schemaName; }
		public String    getTableName()  { return _tableName; }
		public String    getType()       { return _type; }
		public Timestamp getCrDate()     { return _crdate; }
		public Timestamp getSampleTime() { return _sampleTime; }

		public int       getRowTotal()   { return _rowtotal; }
		public int       getReservedKb() { return _reservedKb; }
		public int       getDataKb()     { return _dataKb; }
		public int       getDataPages()  { return _dataPages; }
		public int       getIndexKb()    { return _indexKb; }
		public int       getIndexPages() { return _indexPages; }
		public int       getLobKb()      { return _lobKb; }
		public int       getLobPages()   { return _lobPages; }
		public int       getUnusedKb()   { return _unusedKb; }

		public int       getSizeKb()     { return _reservedKb - _unusedKb; }
		public double    getSizeMb()     { return MathUtils.round(getSizeKb() / 1024.0, 1); }

		public double    getReservedMb() { return MathUtils.round(getReservedKb() / 1024.0, 1); }
		public double    getDataMb()     { return MathUtils.round(getDataKb()     / 1024.0, 1); }
		public double    getLobMb()      { return getLobKb() == -1 ? -1d : MathUtils.round(getLobKb() / 1024.0, 1); }
		public double    getIndexMb()    { return MathUtils.round(getIndexKb()    / 1024.0, 1); }
		public double    getUnusedMb()   { return MathUtils.round(getUnusedKb()   / 1024.0, 1); }
		public String    getLockScheme() { return _lockSchema; }
		public int       getIndexCount() { return _indexCount; }
		public List<AseIndexInfo> getIndexList()  { return _indexList == null ? Collections.emptyList() : _indexList; }

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

			AseTableInfo other = (AseTableInfo) obj;
			return Objects.equals(_dbName    , other._dbName) 
			    && Objects.equals(_schemaName, other._schemaName) 
			    && Objects.equals(_tableName , other._tableName);
		}
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


	public String extractSqlStatementsFromXmlShowplan(String xmlPlan)
	{
		return XmlPlanAseUtils.getSqlStatement(xmlPlan);
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
		long   execCount;
		long   elapsed_ms;
		long   cpuTime;
		long   waitTime;
		long   logicalReads;
		long   physicalReads;
		long   rowsAffected;
		long   memUsageKB;
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

			BigDecimal waitTimePct      = entry.elapsed_ms == 0 ? new BigDecimal(0) : new BigDecimal(((entry.waitTime*1.0)/(entry.elapsed_ms*1.0)) * 100.0).setScale(1, BigDecimal.ROUND_HALF_EVEN);
			
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
//			    + "where [" + colName + "] = " + DbUtils.safeStr(colVal) + " \n"REMOVE-THIS: 
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
						entry.execCount     = rs.getLong  (colPos++);
						entry.elapsed_ms    = rs.getLong  (colPos++);
						entry.cpuTime       = rs.getLong  (colPos++);
						entry.waitTime      = rs.getLong  (colPos++);
						entry.logicalReads  = rs.getLong  (colPos++);
						entry.physicalReads = rs.getLong  (colPos++);
						entry.rowsAffected  = rs.getLong  (colPos++);
						entry.memUsageKB    = rs.getLong  (colPos++);
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
			_logger.warn("Problems getting SQL Text by: " + whereColValMap + ": " + ex, ex);
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

					setWaitDescription(rstm, "WaitEventID", "WaitEventDesc", "WaitClassID", "WaitClassDesc");
					setDurationColumn (rstm, "sampleTime_min", "sampleTime_max", "Duration");
					setMsToHms        (rstm, "WaitTime_sum", "WaitTime_HMS");

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
	public void setWaitDescription(ResultSetTableModel rstm, String waitEventId, String waitEventDesc, String waitClassId, String waitClassDesc)
	{
		int pos_waitEventId   = rstm.findColumn(waitEventId);
		int pos_waitEventDesc = rstm.findColumn(waitEventDesc);
		
		int pos_waitClassId   = rstm.findColumn(waitClassId);
		int pos_waitClassDesc = rstm.findColumn(waitClassDesc);
		
//		if ( ! MonTablesDictionaryManager.hasInstance() )
//		{
//			_logger.warn("setWaitDescription(): No 'MonTablesDictionary' was found... cant set descriptions...");
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
