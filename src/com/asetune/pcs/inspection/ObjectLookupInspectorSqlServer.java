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
 * the Free Software Foundation, version 3 of the License.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.asetune.pcs.inspection;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.asetune.Version;
import com.asetune.pcs.DdlDetails;
import com.asetune.pcs.ObjectLookupQueueEntry;
import com.asetune.pcs.PersistentCounterHandler;
import com.asetune.sql.SqlObjectName;
import com.asetune.sql.SqlParserUtils;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.AseSqlScript;
import com.asetune.utils.Configuration;
import com.asetune.utils.DbUtils;
import com.asetune.utils.SqlServerUtils;
import com.asetune.utils.StringUtil;

public class ObjectLookupInspectorSqlServer
extends ObjectLookupInspectorAbstract
{
	private static Logger _logger = Logger.getLogger(ObjectLookupInspectorSqlServer.class);

//	private long _parserExceptionCount = 0;
//	private ConcurrentHashMap<String, Integer> _parserFailCache = new ConcurrentHashMap<>();
	
	public static final String  PROPKEY_xmlPlan_parseAndSendTables = Version.getAppName() + "." + ObjectLookupInspectorSqlServer.class.getSimpleName() + ".xmlPlan.parse.send.tables";
	public static final boolean DEFAULT_xmlPlan_parseAndSendTables = true;

	public static final String  PROPKEY_view_parseAndSendTables    = Version.getAppName() + "." + ObjectLookupInspectorSqlServer.class.getSimpleName() + ".view.parse.send.tables";
	public static final boolean DEFAULT_view_parseAndSendTables    = true;

	@Override
	public boolean allowInspection(ObjectLookupQueueEntry lookupEntry)
	{
		if (lookupEntry == null)
			return false;

		if (lookupEntry._dbname == null || lookupEntry._objectName == null)
			return false;

		// dbname 32767 
		if (lookupEntry._dbname.equals("32767"))
			return false;

		
		SqlObjectName sqlObj = new SqlObjectName(lookupEntry._objectName, DbUtils.DB_PROD_NAME_MSSQL, "\"", false, false, true);
		String schemaName = sqlObj.getSchemaName();
		String objectName = sqlObj.getObjectName();

		// Discard a bunch of entries
		if (objectName.startsWith("sys"))       return false;
		if (objectName.startsWith("SYS"))       return false;
		if (objectName.startsWith("#"))         return false;
		if ("sys".equalsIgnoreCase(schemaName)) return false;


		// Mark this as a "StatementCacheEntry"
		if (lookupEntry._objectName.startsWith("0x"))
		{
			lookupEntry.setStatementCacheEntry(true);
			//entry._dbname = PersistentCounterHandler.STATEMENT_CACHE_NAME;
		}
			
		
		return true;
	}

	/**
	 * When a new connection has been made, install some extra "stuff" in SQL-Server if it doesn't already exists
	 */
	@Override
	public void onConnect(DbxConnection conn)
	{
	}

	@Override
	public List<DdlDetails> doObjectInfoLookup(DbxConnection conn, ObjectLookupQueueEntry qe, PersistentCounterHandler pch)
	{
		final String dbname           = qe._dbname;
		final String originObjectName = qe._objectName;
		      String objectName       = qe._objectName;
		final String source           = qe._source;
		final String dependParent     = qe._dependParent;
		final int    dependLevel      = qe._dependLevel;

		// FIXME: dbname  can be a Integer
		// FIXME: objName can be a Integer
		// Do the lookup, then check _ddlCache if it has been stored.

		// Check if the input is a: ExecutionPlan
		boolean isExecutionPlan = false;
		if (qe.isStatementCacheEntry() || objectName.startsWith("0x"))
		{
			isExecutionPlan = true;
		}


		// Statement Cache objects
		if (isExecutionPlan)
		{
			//dbname          = PersistentCounterHandler.STATEMENT_CACHE_NAME;

			DdlDetails storeEntry = new DdlDetails(PersistentCounterHandler.STATEMENT_CACHE_NAME, objectName);
			storeEntry.setCrdate( new Timestamp(System.currentTimeMillis()) );
			storeEntry.setSource( source );
			storeEntry.setDependLevel( dependLevel );
//			entry.setOwner("ssql");
			storeEntry.setOwner(dbname);
			storeEntry.setType("SS");
			storeEntry.setSampleTime( new Timestamp(System.currentTimeMillis()) );

			try
			{
				String xmlPlan = SqlServerUtils.getXmlQueryPlan(conn, objectName);
//System.out.println("ObjectLookupInspectorSqlServer.doObjectInfoLookup(): ExecPlanHandle='" + objectName + "', getXmlQueryPlan returned: " + xmlPlan);
//				entry.setObjectText( xmlPlan );
				storeEntry.setExtraInfoText( xmlPlan ); // AseTune uses setExtraInfoText(), so lets stick with that
				
				// Look for specific texts in the XML plan and setExtraInfoText() if found! 
				if (xmlPlan != null)
				{
					int cnt = 0;

					boolean hasMissingIndexes = false;
					boolean hasWarnings       = false;
					if (xmlPlan.contains("<MissingIndexes>"))
					{
						cnt++;
						hasMissingIndexes = true;
					}
					if (xmlPlan.contains("<Warnings>"))
					{
						cnt++;
						hasWarnings = true;
					}

					// Others we might want to look for: 
					//  -- Queries_with_Index_Scans_Due_to_Implicit_Conversions
					//  -- Memory Grants... spill to disk...
					// <Warnings><SpillToTempDb SpillLevel="1"></Warnings>
					// <WaitStats>this should also hopefully be part of a QueryPlan</WaitStats>
					
					if ( cnt > 0 )
					{
						// Build a JSON String with the info, it might be easier to parse if we add many different options here.
						StringBuilder sb = new StringBuilder();
						String comma = "";
						// BEGIN JSON
						sb.append("{");

						if (hasMissingIndexes)
						{
							sb.append(comma).append("\"hasMissingIndexes\": true");
							comma = ", ";
						}

						if (hasWarnings)
						{
							sb.append(comma).append("\"hasWarnings\": true");
							comma = ", ";
						}

						// END JSON
						sb.append("}");

						// Set the JSON to ExtraInfoText
						//entry.setExtraInfoText(sb.toString()); // AseTune uses setExtraInfoText(), so: use some of the other fields: objectText, dependsText, optdiagText
						storeEntry.setObjectText(sb.toString());
					}
					
					// Get SQL Text from the XML Plan
					// Then parse the SQL Text, and get tables... Then send of those table to a new DDL Lookup
// Not sure if the below is ready YET, the SqlParserUtils.getTables() probably needs a bit more work...
					if (Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_xmlPlan_parseAndSendTables, DEFAULT_xmlPlan_parseAndSendTables))
					{
						String sqlText = SqlServerUtils.getSqlTextFromXmlPlan(xmlPlan);

						// Get list of tables in the SQL Text and add them for a DDL Lookup/Store
						Set<String> tableList = SqlParserUtils.getTables(sqlText);
						for (String tableName : tableList)
							pch.addDdl(dbname, tableName, this.getClass().getSimpleName() + ".xmlPlan.sql");
						
//						if ( ! StringUtil.startsWithIgnoreBlankIgnoreCase(sqlText, "create ") )
//						{
//							// NOT in failed cache
//							if ( ! _parserFailCache.contains(sqlText) )
//							{
//								try 
//								{
//									List<String> tableList = SqlParserUtils.getTables(sqlText, true);
//									for (String tableName : tableList)
//										pch.addDdl(originDbname, tableName, this.getClass().getSimpleName() + ".xmlPlan.sql");
//								}
//								catch (Exception pex) 
//								{ 
//									if (Configuration.getCombinedConfiguration().getBooleanProperty(this.getClass().getSimpleName() + ".log.parse.errors", false))
//										_logger.warn("Problems parsing SQL=|" + sqlText + "|. Skipping this and just continuing. Caught: " + pex, pex);
//
//									_parserExceptionCount++;
//									
//									// Cache FAILED statements
//									int count = _parserFailCache.contains(sqlText) ? _parserFailCache.get(sqlText) : 1;
//									_parserFailCache.put(sqlText, count);
//
//									// check that the cache isn't to big 
//									while (_parserFailCache.size() > 500)
//									{
//										// Get a entry from the cache to remove (well, this isn't random... but anyway hopefully it's not the one we just added)
//										String cachedSqlText = null;
//										for (String tmp : _parserFailCache.keySet())
//										{
//											cachedSqlText = tmp;
//											break;
//										}
//										if (cachedSqlText != null)
//											_parserFailCache.remove(cachedSqlText);
//									}
//								}
//							}
//						}
					} // end: config
				} // end: xmlPlan
			}
			catch(SQLException e)
			{
				String msg = "Problems getting text from sys.dm_exec_query_plan, about '" + objectName + "'. Msg=" + e.getErrorCode() + ", Text='" + e.getMessage() + "'. Caught: " + e;
				_logger.warn(msg); 
				storeEntry.setObjectText( msg );
			}

			// Return the list of objects to be STORED in DDL Storage
			return Arrays.asList(storeEntry);  // <<<<<<<<<<------------<<<<<<<<<<------------<<<<<<<<<<------------
			
		} // end: isExecutionPlan
		else // all other "objects"
		{
			// Get a list of DBMS Object we want to get information for!
			List<DdlDetails> objectList = getDbmsObjectList(conn, dbname, objectName, source, dependParent, dependLevel);
			
			// The object was NOT found
			if (objectList.isEmpty())
			{
				// If the future, do NOT do lookup of this table.
				pch.markDdlDetailsAsDiscarded(dbname, originObjectName);

				_logger.info("DDL Lookup: Can't find any information for dbname='" + dbname + "', objectName='" + objectName + "', source='" + source + "', dependLevel=" + dependLevel + ". Skipping DDL Storage of this object. Also adding it to the 'discard' list.");
				return null;
			}
	

			// Add entries that should be stored to this list
			List<DdlDetails> returnList = new ArrayList<>();

			//----------------------------------------------------------------------------------
			// Do lookups of the entries found in the DBMS Dictionary
			//----------------------------------------------------------------------------------
			for (DdlDetails storeEntry : objectList)
			{
				String type = storeEntry.getType();

				// https://docs.microsoft.com/en-us/sql/relational-databases/system-catalog-views/sys-objects-transact-sql?view=sql-server-ver15
                // 
				//   AF = Aggregate function (CLR)
				//   C  = CHECK constraint
				//   D  = DEFAULT (constraint or stand-alone)
				//   F  = FOREIGN KEY constraint
				//   FN = SQL scalar function
				//   FS = Assembly (CLR) scalar-function
				//   FT = Assembly (CLR) table-valued function
				//   IF = SQL inline table-valued function
				//   IT = Internal table
				//   P  = SQL Stored Procedure
				//   PC = Assembly (CLR) stored-procedure
				//   PG = Plan guide
				//   PK = PRIMARY KEY constraint
				//   R  = Rule (old-style, stand-alone)
				//   RF = Replication-filter-procedure
				//   S  = System base table
				//   SN = Synonym
				//   SO = Sequence object
				//   U  = Table (user-defined)
				//   V  = View
				//   EC = Edge constraint
                //   
				//   ---- Applies to: SQL Server 2012 (11.x) and later.
				//   SQ = Service queue
				//   TA = Assembly (CLR) DML trigger
				//   TF = SQL table-valued-function
				//   TR = SQL DML trigger
				//   TT = Table type
				//   UQ = UNIQUE constraint
				//   X = Extended stored procedure
                //   
				//   ---- Applies to: SQL Server 2014 (12.x) and later, Azure SQL Database, Azure Synapse Analytics, Analytics Platform System (PDW).
				//   ST = STATS_TREE
                //   
				//   ---- Applies to: SQL Server 2016 (13.x) and later, Azure SQL Database, Azure Synapse Analytics, Analytics Platform System (PDW).
				//   ET = External Table
				
				if ( "U".equals(type) || "S".equals(type) )
				{
					// This should be stored
					returnList.add(storeEntry);

					//--------------------------------------------
					// Just do sp_help to get info (which will include index key information)
					String sql = "exec " + storeEntry.getDbname() + "..sp_help '" + storeEntry.getSchemaAndObjectName() + "' ";

					try	(AseSqlScript ss = new AseSqlScript(conn, 10)) 
					{
						ss.setRsAsAsciiTable(true);
						storeEntry.setObjectText( ss.executeSqlStr(sql, true) ); 
					} 
					catch (SQLException e) 
					{ 
						storeEntry.setObjectText( e.toString() ); 
					}
	
					//--------------------------------------------
					// GET sp__optdiag
					if (pch.getConfig_doGetStatistics())
					{
						// get table statistics -- not yet implemented
						//entry.setOptdiagText(...);
					}
		
					//--------------------------------------------
					// GET SOME OTHER STATISTICS
					// - Table size 
					// - Index size 
					
//					sql = "exec " + entry.getDbname() + "..sp_spaceused '" + entry.getOwner() + "." + entry.getObjectName() + "' ";
//					sql = "select * from sys.dm_db_index_physical_stats(db_id('" + entry.getDbname() + "'), " + entry.getObjectId() + ", DEFAULT, DEFAULT, 'SAMPLED')";
//					sql = "SELECT /* " + Version.getAppName() + ":ObjectLookupInspector */ \n"
//					    + "     db_name(s.database_id) AS dbname \n"
//						+ "    ,sc.name                AS schema_name \n"
//						+ "    ,o.name                 AS object_name \n"
//						+ "    ,i.name                 AS index_name \n"
//						+ "    ,s.* \n"
//					    + "FROM sys.dm_db_index_physical_stats(db_id('" + entry.getDbname() + "'), " + entry.getObjectId() + ", DEFAULT, DEFAULT, 'SAMPLED') s \n"
//					    + "LEFT OUTER JOIN [" +  entry.getDbname() + "].sys.indexes  i ON s.object_id = i.object_id and s.index_id = i.index_id \n"
//					    + "LEFT OUTER JOIN [" +  entry.getDbname() + "].sys.objects  o ON s.object_id = o.object_id \n"
//					    + "LEFT OUTER JOIN [" +  entry.getDbname() + "].sys.schemas sc ON o.schema_id = sc.schema_id \n"
//						+ "";
					sql = ""
						+ "SELECT /* " + Version.getAppName() + ":" + this.getClass().getSimpleName() + " */ \n"
						+ "     dbname                           = db_name() \n"
						+ "    ,schema_name                      = sc.name \n"
						+ "    ,object_name                      = o.name \n"
						+ "    ,index_name                       = ISNULL(i.name, 'HEAP') \n"
						+ "    ,s.index_id \n"
					//	+ "    ,object_id                        = max(s.object_id) \n"
						+ " \n"
//						+ "    ,IndexType                        = CASE s.index_id WHEN 0 THEN 'HEAP' WHEN 1 THEN 'CLUSTERED' ELSE 'NON-CLUSTERED' END \n"
						+ "    ,IndexType                        = i.type_desc \n"
						+ "    ,IndexFillFactor                  = i.fill_factor \n"
						+ "    ,IndexIsDisabled                  = i.is_disabled \n"
						+ "    ,IndexIsFiltered                  = i.has_filter \n"
						+ "    ,StatsUpdated                     = NULLIF( MAX(ISNULL(STATS_DATE(s.object_id, s.index_id), '2000-01-01')), '2000-01-01') -- this to get rid of: Warning: Null value is eliminated by an aggregate or other SET operation. \n"
						+ "    ,PartitionCount                   = MAX(s.partition_number) \n"
						+ "    ,row_count                        = SUM(s.row_count) \n"
						+ "    ,RowsPerPage                      = CAST(SUM(s.row_count)*1.0 / NULLIF(SUM(s.used_page_count), 0) as DECIMAL(12,1)) \n"
					//	+ "    ,AvgCalcBytesPerRow               = (SUM(s.in_row_used_page_count)+SUM(s.row_overflow_used_page_count))*8*1024 / NULLIF(SUM(s.row_count), 0) \n"
						+ " \n"
						+ "    ,TotalUsedSizeMB                  = CAST(SUM(s.used_page_count)              / 128.0 AS DECIMAL(12,1)) \n"
						+ "    ,InRowUsedSizeMB                  = CAST(SUM(s.in_row_used_page_count)       / 128.0 AS DECIMAL(12,1)) \n"
						+ "    ,LobUsedSizeMB                    = CAST(SUM(s.lob_used_page_count)          / 128.0 AS DECIMAL(12,1)) \n"
						+ "    ,OverflowUsedSizeMB               = CAST(SUM(s.row_overflow_used_page_count) / 128.0 AS DECIMAL(12,1)) \n"
						+ " \n"
						+ "    ,used_page_count                  = SUM(s.used_page_count) \n"
						+ "    ,reserved_page_count              = SUM(s.reserved_page_count) \n"
						+ " \n"
						+ "    ,in_row_used_page_count           = SUM(s.in_row_used_page_count) \n"
						+ "    ,in_row_data_page_count           = SUM(s.in_row_data_page_count) \n"
						+ "    ,in_row_reserved_page_count       = SUM(s.in_row_reserved_page_count) \n"
						+ " \n"
						+ "    ,lob_used_page_count              = SUM(s.lob_used_page_count) \n"
						+ "    ,lob_reserved_page_count          = SUM(s.lob_reserved_page_count) \n"
						+ " \n"
						+ "    ,row_overflow_used_page_count     = SUM(s.row_overflow_used_page_count) \n"
						+ "    ,row_overflow_reserved_page_count = SUM(s.row_overflow_reserved_page_count) \n"
						+ " \n"
						+ "FROM [" +  storeEntry.getDbname() + "].sys.dm_db_partition_stats s \n"
						+ "LEFT OUTER JOIN [" +  storeEntry.getDbname() + "].sys.indexes  i WITH (READUNCOMMITTED) ON s.object_id = i.object_id AND s.index_id = i.index_id \n"
						+ "LEFT OUTER JOIN [" +  storeEntry.getDbname() + "].sys.objects  o WITH (READUNCOMMITTED) ON s.object_id = o.object_id \n"
						+ "LEFT OUTER JOIN [" +  storeEntry.getDbname() + "].sys.schemas sc WITH (READUNCOMMITTED) ON o.schema_id = sc.schema_id \n"
						+ "WHERE 1=1 \n"
						+ "  AND o.object_id = " + storeEntry.getObjectId() + " \n"
						+ "  AND o.is_ms_shipped = 0 \n"
					//	+ "  AND s.row_count > 0 \n"
						+ "GROUP BY sc.name, o.name, i.name, s.index_id \n"
						+ "ORDER BY s.index_id \n"
//						+ "ORDER BY sc.name, o.name, s.index_id \n"
						+ "";

					
					try	(AseSqlScript ss = new AseSqlScript(conn, 10)) 
					{
						ss.setRsAsAsciiTable(true);
						storeEntry.setExtraInfoText( ss.executeSqlStr(sql, true) ); 
					}
					catch (SQLException e) 
					{ 
						storeEntry.setExtraInfoText( e.toString() ); 
					}

					// TODO: more info to save (this is from ASE)
					// - datachange(table_name, partition_name, column_name)
					// - can we get some other statistics from sysstatistics
					//   like when was statistics updated for this table
					// - function: derived_stats(objnamme|id, indexname|indexid, [ptn_name|ptn_id], 'stats')
					//             stats = dpcr | data page cluster ratio
					//                     ipcr | index page cluster ratio
					//                     drcr | data row cluster ratio
					//                     lgio | large io efficiency
					//                     sput | space utilization
					//             to get Cluster Ratio etc...
					// - try to calculate "if tab has a lot of unused space / fragmented"
				}

				else if (   
				       "P" .equals(type)  // Stored procedure
				    || "TR".equals(type)  // SQL DML Trigger
				    || "V" .equals(type)  // View
				    || "C" .equals(type)  // CHECK constraint
				    || "D" .equals(type)  // Default or DEFAULT constraint
				    || "R" .equals(type)  // Rule
				    || "X" .equals(type)) // Extended stored procedure
				{
					// This should be stored
					returnList.add(storeEntry);

					//--------------------------------------------
					// GET OBJECT TEXT
					String sql = " select c.text "
						+ " from " + storeEntry.getDbname() + "..sysobjects o, " + storeEntry.getDbname() + "..syscomments c, " + storeEntry.getDbname() + "..sysusers u \n"
						+ " where o.name = '" + storeEntry.getObjectName() + "' \n"
						+ "   and u.name = '" + storeEntry.getOwner() + "' \n" 
						+ "   and o.id   = c.id \n"
						+ "   and o.uid  = u.uid \n"
						//+ " order by c.number, c.colid2, c.colid ";
						+ " order by c.number, c.colid ";

					String sqlText = "";

					try (Statement statement = conn.createStatement(); ResultSet rs = statement.executeQuery(sql) )
					{
						while(rs.next())
						{
							sqlText += rs.getString(1);
						}

						storeEntry.setObjectText( sqlText );
					}
					catch (SQLException e)
					{
						storeEntry.setObjectText( e.toString() );
					}
						
						
					//--------------------------------------------
					// if VIEW: Possibly parse the SQL statement and extract Tables, send those tables to 'DDL Storage'...
					// This so we can lookup size etc of those tables...
					// LATER: net.sf.jsqlparser.util.TablesNamesFinder didn't yet support 'create view...'
					// So we have to view with implementing this!
					if ("V".equals(type) && StringUtil.hasValue(sqlText))
					{
						if (Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_view_parseAndSendTables, DEFAULT_view_parseAndSendTables))
						{
//							List<String> tableList = Collections.emptyList();
//							try
//							{
//								tableList = SqlParserUtils.getTables(sqlText, true); 
//							}
//							catch (ParseException pex) 
//							{ 
//								_logger.warn("Problems Parsing VIEW '" + storeEntry.getObjectName() + "' to get table names. SqlText=|" + sqlText + "|. Skipping and continuing...", pex);
//							}

							// Get list of tables in the SQL Text and add them for a DDL Lookup/Store
							Set<String> tableList = SqlParserUtils.getTables(sqlText);
							
							// Post to DDL Storage, for lookup
							for (String tableName : tableList)
							{
								pch.addDdl(dbname, tableName, this.getClass().getSimpleName() + ".resolve.view");
							}
							
							// Add table list to the saved entry
							storeEntry.setExtraInfoText( "TableList: " + StringUtil.toCommaStr(tableList) ); 
						}
					}
				}
				else
				{
					// Unknown type
					// DO NOT STORE --- returnList.add(entry);
					continue;
				}
	
				//--------------------------------------------
				// GET sp_depends
				if (pch.getConfig_doSpDepends())
				{
					String sql = "exec " + storeEntry.getDbname() + "..sp_depends '" + storeEntry.getOwner() + "." + storeEntry.getObjectName() + "' "; 
					
					try	(AseSqlScript ss = new AseSqlScript(conn, 10)) 
					{
						ss.setRsAsAsciiTable(true);
						storeEntry.setDependsText( ss.executeSqlStr(sql, true) ); 
					} 
					catch (SQLException e) 
					{ 
						storeEntry.setDependsText( e.toString() ); 
					}
		
					if (pch.getConfig_addDependantObjectsToDdlInQueue())
					{
						sql = "exec " + storeEntry.getDbname() + "..sp_depends '" + storeEntry.getSchemaAndObjectName() + "' "; 
		
						ArrayList<String> dependList = new ArrayList<String>();
						try
						{
							Statement statement = conn.createStatement();
							ResultSet rs = statement.executeQuery(sql);
							ResultSetMetaData rsmd = rs.getMetaData();
		
							// lets search for 'object' column, in no case, if it changes...
							int objectname_pos = -1;
							for (int c=1; c<=rsmd.getColumnCount(); c++)
							{
								if (rsmd.getColumnLabel(c).toLowerCase().equals("name"))
								{
									objectname_pos = c;
									break;
								}
							}
							if (objectname_pos > 0)
							{
								String objNamePrev = ""; // Used to check if next row is a "new" object
								while(rs.next())
								{
									// Get the dependent object name
									String depOnObjectName = rs.getString(objectname_pos);

									// get next row if we are not interested in this one.
									if (depOnObjectName == null)
										continue;
									if (depOnObjectName.equals(objNamePrev))
										continue;

									// Strip off the beginning of the string, which holds the owner
									// example: "dbo.sp_addmessage"
									int beginIndex = depOnObjectName.indexOf('.') + 1;
									if (beginIndex < 0)
										beginIndex = 0;
									String shortObjName = depOnObjectName.substring(beginIndex);

									dependList.add(shortObjName);

									// Don't add SystemProcedure/systemTables dependencies
									if ( ! shortObjName.startsWith("sp_") && ! shortObjName.startsWith("sys"))
									{
										pch.addDdl(storeEntry.getDbname(), shortObjName, source, objectName, dependLevel + 1);
									}
									
									// remember last record, so we can filter out if we see same record on next row
									objNamePrev = depOnObjectName;
								}
							}
							else
							{
								_logger.debug("When getting dependent objects using 'sp_depends', I was expecting a column named 'name'. But it wasn't found. The result set had " + rsmd.getColumnCount() + " columns. Skipping lookup for dependent object for '" + storeEntry.getFullObjectName() + "'.");
							}
							rs.close();
							statement.close();
						}
						catch (SQLException e)
						{
							// If we didn't have any results for the table, then:
							// java.sql.SQLException: JZ0R2: No result set for this query.
							if ( ! "JZ0R2".equals(e.getSQLState()) )
							{
								_logger.warn("Problems getting 'sp_depends' for table '" + storeEntry.getFullObjectName() + "'. SqlState='" + e.getSQLState() + "', Caught: " + e);
							}
						}
						if (dependList.size() > 0)
							storeEntry.setDependList(dependList);
					}
				}

			} // end: for (DdlDetails entry : objectList)

			// Return the list of objects to be STORED in DDL Storage
			return returnList;
			
		} // end: other objects than Statement Cache
	}


	private List<DdlDetails> getDbmsObjectList(DbxConnection conn, String dbname, String objectName, String source, String dependParent, int dependLevel)
	{
		String originObjectName = objectName;

		// Keep a list of objects we need to work with
		// because: if we get more than one proc/table with different owners
		ArrayList<DdlDetails> objectList = new ArrayList<DdlDetails>();
		
		// Remove any "dbname" or "schemaName" from the objectName if it has any
		// The lookup below will add entries for ALL tables with the name (if there are several tables with same name, the lookup will add ALL tables)
		SqlObjectName sqlObjectName = new SqlObjectName(conn, objectName);
		objectName = sqlObjectName.getObjectName();
		
		// GET type and creation time
		String sql = ""
			    + "SELECT o.object_id, \n"  // Object id
			    + "       o.type, \n"       // UserTable, view etc...
			    + "       s.name, \n"       // Schema name
			    + "       o.name, \n"       // Object name
			    + "       o.create_date \n" // Creation date
			    + "FROM [" + dbname + "].sys.objects o \n"
			    + "INNER JOIN [" + dbname + "].sys.schemas s ON o.schema_id = s.schema_id \n"
			    + "WHERE o.name = '" + objectName + "' \n"  // Trust that the DBMS will handle Case Sensitivity
			    + "  AND o.is_ms_shipped = 0 \n"
			    + "";

		try ( Statement statement = conn.createStatement(); ResultSet rs = statement.executeQuery(sql) )
		{
			while(rs.next())
			{
				DdlDetails entry = new DdlDetails();
				
				entry.setDbname          ( dbname );
				entry.setSearchDbname    ( dbname );
				entry.setObjectName      ( objectName );
				entry.setSearchObjectName( originObjectName ); // NOT Stored in DDL Store, used for: isDdlDetailsStored(), markDdlDetailsAsStored()
				entry.setSource          ( source );
				entry.setDependParent    ( dependParent );
				entry.setDependLevel     ( dependLevel );
				entry.setSampleTime      ( new Timestamp(System.currentTimeMillis()) );

				entry.setObjectId        ( rs.getInt      (1) );
				entry.setType            ( rs.getString   (2) );
				entry.setSchemaName      ( rs.getString   (3) ); // setOwner() and setSchemaName() is the same
				entry.setObjectName      ( rs.getString   (4) ); // Use the object name stored in the DBMS (for MS SQL-Server ASE it will always be stored as it was originally created)
				entry.setCrdate          ( rs.getTimestamp(5) );
				
				objectList.add(entry);
			}

			return objectList;
		}
		catch (SQLException e)
		{
			_logger.error("Problems Getting basic information about DDL for dbname='" + dbname + "', objectName='" + objectName + "', source='" + source + "', dependLevel=" + dependLevel + ". Skipping DDL Storage of this object. Caught: " + e);
			return Collections.emptyList();
			//return null;
		}
	}
}
