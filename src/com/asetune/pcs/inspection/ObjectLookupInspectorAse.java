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
import com.asetune.cache.XmlPlanAseUtils;
import com.asetune.cache.XmlPlanCache;
import com.asetune.cache.XmlPlanCacheAse;
import com.asetune.cm.sql.VersionInfo;
import com.asetune.pcs.DdlDetails;
import com.asetune.pcs.ObjectLookupQueueEntry;
import com.asetune.pcs.PersistentCounterHandler;
import com.asetune.sql.SqlObjectName;
import com.asetune.sql.SqlParserUtils;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.AseSqlScript;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;
import com.asetune.utils.Ver;

public class ObjectLookupInspectorAse
extends ObjectLookupInspectorAbstract
{
	private static Logger _logger = Logger.getLogger(ObjectLookupInspectorAse.class);

	public static final String  PROPKEY_xmlPlan_parseAndSendTables = Version.getAppName() + "." + ObjectLookupInspectorAse.class.getSimpleName() + ".xmlPlan.parse.send.tables";
	public static final boolean DEFAULT_xmlPlan_parseAndSendTables = true;

	public static final String  PROPKEY_view_parseAndSendTables    = Version.getAppName() + "." + ObjectLookupInspectorAse.class.getSimpleName() + ".view.parse.send.tables";
	public static final boolean DEFAULT_view_parseAndSendTables    = true;

	
	private long    _dbmsVersion = 0;

	private static final List<Integer> skipSpDependsMessages = null;
//	private static final List<Integer> skipSpDependsMessages = Arrays.asList(0, 17462, 17463, 17464, 17465, 17466, 17467, 17468, 17469);
//	+-----+--------+------+-------------------------------------------------------------------------------------------------------------------------------------------------------------+------+--------+
//	|error|severity|dlevel|description                                                                                                                                                  |langid|sqlstate|
//	+-----+--------+------+-------------------------------------------------------------------------------------------------------------------------------------------------------------+------+--------+
//	|17461|0       |0     |Object does not exist in this database.                                                                                                                      |(NULL)|(NULL)  |
//	|17462|0       |0     |Things the object references in the current database.                                                                                                        |(NULL)|(NULL)  |
//	|17463|0       |0     |Things inside the current database that reference the object.                                                                                                |(NULL)|(NULL)  |
//	|17464|0       |0     |Object doesn't reference any object and no objects reference it.                                                                                             |(NULL)|(NULL)  |
//	|17465|0       |0     |The specified column (or all columns, if none was specified) in %1! has no dependencies on other objects, and no other object depends on any columns from it.|(NULL)|(NULL)  |
//	|17466|0       |0     |Dependent objects that reference column %1!.                                                                                                                 |(NULL)|(NULL)  |
//	|17467|0       |0     |Dependent objects that reference all columns in the table. Use sp_depends on each column to get more information.                                            |(NULL)|(NULL)  |
//	|17468|0       |0     |Columns referenced in stored procedures, views or triggers are not included in this report.                                                                  |(NULL)|(NULL)  |
//	|17469|0       |0     |Tables that reference this object:                                                                                                                           |(NULL)|(NULL)  |
//	+-----+--------+------+-------------------------------------------------------------------------------------------------------------------------------------------------------------+------+--------+


	@Override
	public boolean allowInspection(ObjectLookupQueueEntry entry)
	{
		if (entry == null)
			return false;

//		String dbname     = entry._dbname;
		String objectName = entry._objectName;
		
		// Discard a bunch of entries
		if (objectName.indexOf("temp worktable") >= 0 ) return false;
		if (objectName.startsWith("#"))                 return false;
		if (objectName.startsWith("ObjId:"))            return false;
		if (objectName.startsWith("Obj="))              return false;
		if (objectName.startsWith("sys"))               return false;

		// Discard entries '*??', but allow '*ss' and '*sq'
		if (objectName.startsWith("*"))
		{
			if (objectName.startsWith("*ss"))
			{
				// set the database to be statement cache, so the PersistentCounterHandler.addDdl() can discard the entry if it's already stored
//				entry._dbname = PersistentCounterHandler.STATEMENT_CACHE_NAME;
				entry.setStatementCacheEntry(true);

				// ALLOW: Statement Cache entry 
			}
			else if (objectName.startsWith("*sq"))
			{
				// set the database to be statement cache, so the PersistentCounterHandler.addDdl() can discard the entry if it's already stored
//				entry._dbname = PersistentCounterHandler.STATEMENT_CACHE_NAME;
				entry.setStatementCacheEntry(true);

				// ALLOW: Prepared statements from ct_dynamic and/or 
				//        Java PreparedStatement, if jConnect URL has DYNAMIC_PREPARE=true
				//        *sq object was introduced in ASE Server is above 15.7.0 ESD#2
				// ---------------------------------
				// In earlier ASE Versions:
				// Format of the Dynamic SQL statement (prior to ASE 15.7.0 ESD#2) is:
				// *12345612345678_ffffff
				// *_SPID_StmntId#_??????
				// ---------------------------------
				// *      = just a prefix
				// 1-6    = SPID in decimal format
				// 7-15   = Statement ID, just a incremental counter
				// _      = separator
				// ffffff = Hexadecimal value for something, which I did not figure out (hashkey of the parsed SQL Text)
				// ---------------------------------
				// There is no way to say what SQL Statement that is behind the LW Procedure 
				// so just get out of here (return)
				// ---------------------------------------------------------------------------
				// Below is a SQL that can be used to track old Dynamic SQL statements
				// select SPID=convert(int,substring(ObjectName,2,6)), StmntId=convert(int,substring(ObjectName,8,8)), MemUsageKB
				// from master..monCachedProcedures
				// where ObjectName like '*%'
				//   and ObjectName not like '*ss%'
			}
 			else
 			{
 				// Get out, do NOT allow this lookup... it's an unknown type staring with '*'
				return false; 
 			}
		}
		
		// allow inspection
		return true;
	}

	/**
	 * When a new connection has been made, install some extra "stuff" in ASE if it doesn't already exists
	 */
	@Override
	public void onConnect(DbxConnection conn)
	{
		_dbmsVersion = conn.getDbmsVersionNumber();
		
		// Should we install Procs
		try
		{
			if (_dbmsVersion >= Ver.ver(15,7))
			{
				// do not install use: sp_showoptstats instead
			}
			else if (_dbmsVersion >= Ver.ver(15,0))
				AseConnectionUtils.checkCreateStoredProc(conn, Ver.ver(15,0),     "sybsystemprocs", "sp__optdiag", VersionInfo.SP__OPTDIAG_CRDATE, VersionInfo.class, "sp__optdiag_v1_15_0.sql", "sa_role");
			else
				AseConnectionUtils.checkCreateStoredProc(conn, Ver.ver(12,5,0,3), "sybsystemprocs", "sp__optdiag", VersionInfo.SP__OPTDIAG_CRDATE, VersionInfo.class, "sp__optdiag_v1_9_4.sql", "sa_role");
		}
		catch (Exception e)
		{
			// the checkCreateStoredProc, writes information to error log, so we don't need to do it again.
		}
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<DdlDetails> doObjectInfoLookup(DbxConnection conn, ObjectLookupQueueEntry qe, PersistentCounterHandler pch)
	{
//		final String originDbname     = qe._dbname;
		final String dbname           = qe._dbname;
		      String objectName       = qe._objectName;
		final String originObjectName = qe._objectName;
		final String source           = qe._source;
		final String dependParent     = qe._dependParent;
		final int    dependLevel      = qe._dependLevel;

		// FIXME: dbname  can be a Integer
		// FIXME: objName can be a Integer
		// Do the lookup, then check _ddlCache if it has been stored.

		// Check if the input is a: Statement Cache object
		boolean isStatementCache = false;
		if (qe.isStatementCacheEntry() ||  objectName.startsWith("*ss") || objectName.startsWith("*sq") ) // *sq in ASE 15.7 esd#2, DynamicSQL can/will end up in statement cache
		{
			isStatementCache = true;
		}


		// Statement Cache objects
		if (isStatementCache)
		{
			DdlDetails entry = new DdlDetails(PersistentCounterHandler.STATEMENT_CACHE_NAME, objectName);
			entry.setCrdate( new Timestamp(System.currentTimeMillis()) );
			entry.setSource( source );
			entry.setDependLevel( dependLevel );
			entry.setOwner("ssql");
			entry.setType("SS");
			entry.setSampleTime( new Timestamp(System.currentTimeMillis()) );

			int sep = objectName.indexOf('_');
			String ssqlid = objectName.substring(3, sep);
			//haskey = objectName.substring(sep+1, objectName.length()-3);

			// Get the XmlPlanCache
			if (XmlPlanCache.hasInstance())
			{
				XmlPlanCache xmlPlanCache = XmlPlanCache.getInstance();
				if ( xmlPlanCache.isPlanCached(objectName) )
				{
					entry.setObjectText( "Xml Plan was cached, so only showing the XML Plan... fetched with: select show_cached_plan_in_xml(" + ssqlid + ", 0, 0)" ); 
					entry.setExtraInfoText( xmlPlanCache.getPlan(objectName) );
					
					// Since we got the XML info without talking to the DBMS, there is no need to sleep since we didn't produce any "load" at it... 
					entry.setSleepOption(false);
					return Arrays.asList(entry);  // <<<<<<<<<<------------<<<<<<<<<<------------<<<<<<<<<<------------
				}
			}
			
			String sql = 
				"set switch on 3604 with no_info \n" +
				"dbcc prsqlcache(" + ssqlid + ", 1) "; // 1 = also prints showplan"

			try	(AseSqlScript ss = new AseSqlScript(conn, 10, false)) 
			{
				ss.setUseGlobalMsgHandler(false);
				ss.setMsgPrefix("dbcc prsqlcache:" + dbname + "." + objectName + ": ");
				
				entry.setObjectText( ss.executeSqlStr(sql, true) ); 
			}
			catch (SQLException e) 
			{ 
				entry.setObjectText( e.toString() ); 
			}
			
			if (_dbmsVersion >= Ver.ver(15,7))
			{
				//-----------------------------------------------------------
				// From Documentation on: show_cached_plan_in_xml(statement_id, plan_id, level_of_detail)
				//-----------------------------------------------------------
				// statement_id
				//			     is the object ID of the lightweight procedure (A procedure that can be created and invoked 
				//			     internally by Adaptive Server). This is the SSQLID from monCachedStatement.
				// 
				// plan_id
				//			     is the unique identifier for the plan. This is the PlanID from monCachedProcedures. 
				//			     A value of zero for plan_id displays the showplan output for all cached plans for the indicated SSQLID.
				// 
				// level_of_detail
				//			     is a value from 0 - 6 indicating the amount of detail show_cached_plan_in_xml returns (see Table 2-6). 
				//			     level_of_detail determines which sections of showplan are returned by show_cached_plan_in_xml. 
				//			     The default value is 0.
				// 
				//			     The output of show_cached_plan_in_xml includes the plan_id and these sections:
				// 
				//			         parameter - contains the parameter values used to compile the query and the parameter values 
				//			                     that caused the slowest performance. The compile parameters are indicated with the 
				//			                     <compileParameters> and </compileParameters> tags. The slowest parameter values are 
				//			                     indicated with the <execParameters> and </execParameters> tags. 
				//			                     For each parameter, show_cached_plan_in_xml displays the:
				//			                        Number
				//			                        Datatype
				//			                        Value:    values that are larger than 500 bytes and values for insert-value statements 
				//			                                  do not appear. The total memory used to store the values for all parameters 
				//			                                  is 2KB for each of the two parameter sets.
				// 
				//			         opTree    - contains the query plan and the optimizer estimates. 
				//			                     The opTree section is delineated by the <opTree> and </opTree> tags.
				// 
				//			         execTree  - contains the query plan with the lava operator details. 
				//			                     The execTree section is identified by the tags <execTree> and </execTree>.
				//
				// level_of_detail parameter opTree execTree
				// --------------- --------- ------ --------
				// 0 (the default)       YES    YES         
				// 1                     YES                
				// 2                            YES         
				// 3                                     YES
				// 4                            YES      YES
				// 5                     YES             YES
				// 6                     YES    YES      YES
				//-----------------------------------------------------------

				sql = "select show_cached_plan_in_xml(" + ssqlid + ", 0, 0)";

				boolean rejectPlan = false;
				String xmlPlan = "";
				try
				{
					Statement stmnt = conn.createStatement();
					stmnt.setQueryTimeout(10);
					
					ResultSet rs = stmnt.executeQuery(sql);

					StringBuilder sb = new StringBuilder();
					sb.append(sql).append("\n");
					sb.append("------------------------------------------------------------------\n");
					while (rs.next())
					{
						String xmlStr = rs.getString(1); 
						sb.append(xmlStr);

						rejectPlan = XmlPlanCacheAse.rejectPlan(objectName, xmlStr);
						
						// Add it to the XmlPlanCache
						if (XmlPlanCache.hasInstance() && ! rejectPlan)
						{
							XmlPlanCache.getInstance().setPlan(objectName, xmlStr);
							xmlPlan = xmlStr;
						}
					}
					rs.close();
					stmnt.close();

					entry.setExtraInfoText( sb.toString().trim() );
				}
				catch(SQLException e)
				{
					String msg = "Problems getting text from Statement Cache about '" + objectName + "'. Msg=" + e.getErrorCode() + ", Text='" + e.getMessage() + "'. Caught: " + e;
					_logger.warn(msg); 
					entry.setExtraInfoText( msg );
				}
				
				// If the status of the plan is "not good enough", lets not save it... Hopefully we will get a better plan at next attempt.
				if (rejectPlan)
					return Collections.emptyList();   // <<<<<<<<<<------------<<<<<<<<<<------------<<<<<<<<<<------------
				
				// Get SQL Text from the XML Plan
				// Then parse the SQL Text, and get tables... Then send of those table to a new DDL Lookup
				if (Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_xmlPlan_parseAndSendTables, DEFAULT_xmlPlan_parseAndSendTables))
				{
					//Not sure if the below is ready YET, the SqlParserUtils.getTables() probably needs a bit more work...
					String sqlText = XmlPlanAseUtils.getSqlStatement(xmlPlan);
					
					// Get list of tables in the SQL Text and add them for a DDL Lookup/Store
					Set<String> tableList = SqlParserUtils.getTables(sqlText);
					for (String tableName : tableList)
						pch.addDdl(dbname, tableName, this.getClass().getSimpleName() + ".xmlPlan.sql");
				}
			}

			// Return the list of objects to be STORED in DDL Storage
			return Arrays.asList(entry);  // <<<<<<<<<<------------<<<<<<<<<<------------<<<<<<<<<<------------

		}
		else // all other objects
		{
//			// Keep a list of objects we need to work with
//			// because: if we get more than one proc/table with different owners
//			ArrayList<DdlDetails> objectList = new ArrayList<DdlDetails>();
//			
//			// Remove any "dbname" or "schemaName" from the objectName if it has any
//			// The lookup below will add entries for ALL tables with the name (if there are several tables with same name, the lookup will add ALL tables)
//			SqlObjectName sqlObjectName = new SqlObjectName(conn, objectName);
//			objectName = sqlObjectName.getObjectName();
//			
//			// get TYPE, OWNER CREATION_TIME and DBNAME where the record(s) was found 
//			String sql = ""
//					+ "declare @objectname varchar(255) \n"
//					+ "select @objectname = '" + objectName + "' \n"
//					+ "if exists (select 1 from " + dbname + ".dbo.sysobjects o, " + dbname + ".dbo.sysusers u where o.name = @objectname and o.uid = u.uid) \n"
//					+ "begin \n"
//					+ "    select o.type, u.name, o.crdate, '" + dbname + "' \n"
//					+ "    from " + dbname + ".dbo.sysobjects o, " + dbname + ".dbo.sysusers u \n"
//					+ "    where o.name = @objectname \n"
//					+ "      and o.uid = u.uid \n"
//					+ "end \n"
//					+ "else if ( substring(@objectname, 1, 3) = 'sp_' ) \n"
//					+ "begin \n"
//					+ "    select o.type, u.name, o.crdate, 'sybsystemprocs' \n"
//					+ "    from sybsystemprocs.dbo.sysobjects o, sybsystemprocs.dbo.sysusers u \n"
//					+ "    where o.name = @objectname \n"
//					+ "      and o.uid = u.uid \n"
//					+ "end \n"
//					+ "else \n"
//					+ "begin \n"
//					+ "    -- create a empty/dummy ResultSet with NO rows\n"
//					+ "    select '', '', convert(datetime, null), '' from sysobjects where id is null \n"
//					+ "end \n"
//					+ "";
//				
//			try ( Statement statement = conn.createStatement(); ResultSet rs = statement.executeQuery(sql) )
//			{
//				while(rs.next())
//				{
//					DdlDetails entry = new DdlDetails();
//					
////					entry.setDbname          ( dbname );
//					entry.setSearchDbname    ( dbname );
//					entry.setObjectName      ( objectName );
//					entry.setSearchObjectName( originObjectName ); // NOT Stored in DDL Store, used for: isDdlDetailsStored(), markDdlDetailsAsStored()
//					entry.setSource          ( source );
//					entry.setDependParent    ( dependParent );
//					entry.setDependLevel     ( dependLevel );
//					entry.setSampleTime      ( new Timestamp(System.currentTimeMillis()) );
//
//					entry.setType            ( rs.getString   (1) );
//					entry.setOwner           ( rs.getString   (2) );
//					entry.setCrdate          ( rs.getTimestamp(3) );
//					entry.setDbname          ( rs.getString   (4) );
//
//					objectList.add(entry);
//				}
//			}
//			catch (SQLException e)
//			{
//				_logger.error("Problems Getting basic information about DDL for dbname='" + dbname + "', objectName='" + objectName + "', source='" + source + "', dependLevel=" + dependLevel + ". Skipping DDL Storage of this object. Caught: " + e);
//				return null;
//			}

			// Get a list of DBMS Object we want to get information for!
			List<DdlDetails> objectList = getDbmsObjectList(conn, dbname, objectName, source, dependParent, dependLevel);
			
			// The object was NOT found
			if (objectList.isEmpty())
			{
				// if the source originates from an "xmlPlan.sql" (which is parsed for tables and sent of for DDL Storage) 
				// the dbname is "unknown" and most likely "tempdb", which is faulty
				// so we might have to look in "all" databases for the objectName...
				if (source.indexOf(".xmlPlan.sql") != -1)
				{
					// Get list of all accessible databases, and try to get objects from those databases
					try
					{
						List<String> dbList = AseConnectionUtils.getDatabaseList(conn);
						for (String db : dbList)
						{
							objectList = getDbmsObjectList(conn, db, objectName, source, dependParent, dependLevel);
							if ( ! objectList.isEmpty() )
								break;
						}
					}
					catch (SQLException ex)
					{
						_logger.error("Problems getting database list from DBMS. Caught: " + ex, ex);
						return Collections.emptyList();   // <<<<<<<<<<------------<<<<<<<<<<------------<<<<<<<<<<------------
					}
				}

				// after all-DB-Lookup (if we did that) is still NOT FOUND, then add it to the "skip" list
				if (objectList.isEmpty())
				{
					// If the future, do NOT do lookup of this table.
					pch.markDdlDetailsAsDiscarded(dbname, originObjectName);

					_logger.info("DDL Lookup: Can't find any information for dbname='" + dbname + "', objectName='" + objectName + "', source='" + source + "', dependLevel=" + dependLevel + ". Skipping DDL Storage of this object. Also adding it to the 'discard' list.");
					return Collections.emptyList();   // <<<<<<<<<<------------<<<<<<<<<<------------<<<<<<<<<<------------
				}
			}

			// Add entries that should be stored to this list
			List<DdlDetails> returnList = new ArrayList<>();
			
			//----------------------------------------------------------------------------------
			// Do lookups of the entries found in the DBMS Dictionary
			//----------------------------------------------------------------------------------
			for (DdlDetails entry : objectList)
			{
				String type = entry.getType();

				// Type definition from ASE 15.7, manual
				//   C  - computed column
				//   D  - default
				//   DD - decrypt default
				//   F  - SQLJ function
				//   N  - partition condition
				//   P  - Transact-SQL or SQLJ procedure
				//   PP - the predicate of a privilege
				//   PR - prepare objects (created by Dynamic SQL)
				//   R  - rule
				//   RI - referential constraint
				//   S  - system table
				//   TR - trigger
				//   U  - user table
				//   V  - view
				//   XP - extended stored procedure.

				if ( "U".equals(type) || "S".equals(type) )
				{
					// This should be stored
					returnList.add(entry);
					
					//--------------------------------------------
					// Just do sp_help, or use ddlgen.jar to get info
					String sql = "exec " + entry.getDbname() + "..sp_help '" + entry.getSchemaAndObjectName() + "' ";
	
					try	(AseSqlScript ss = new AseSqlScript(conn, 10, false)) 
					{
						ss.setUseGlobalMsgHandler(false);
						ss.setMsgPrefix("sp_help:" + entry.getFullObjectName() + ": ");
						
						entry.setObjectText( ss.executeSqlStr(sql, true) ); 
					}
					catch (SQLException e)
					{
						entry.setObjectText( e.toString() ); 
					}

					//--------------------------------------------
					// GET sp__optdiag
					if (pch.getConfig_doGetStatistics())
					{
						if (_dbmsVersion >= Ver.ver(15,7))
						{
							sql = "exec " + entry.getDbname() + "..sp_showoptstats '" + entry.getSchemaAndObjectName() + "' ";

							try
							{
								Statement stmnt = conn.createStatement();
								stmnt.setQueryTimeout(10);
								
								ResultSet rs = stmnt.executeQuery(sql);

								StringBuilder sb = new StringBuilder();
								sb.append(sql).append("\n");
								sb.append("------------------------------------------------------------------\n");
								while (rs.next())
								{
									sb.append(rs.getString(1));
								}
								rs.close();
								stmnt.close();

								entry.setOptdiagText( sb.toString().trim() );
							}
							catch(SQLException e)
							{
								String msg = "Problems getting sp_showoptstats, using sql '" + sql + "'. Msg=" + e.getErrorCode() + ", Text='" + e.getMessage() + "'. Caught: " + e;
								//_logger.warn(msg); 
								entry.setOptdiagText( msg );
							}
						}
						else
						{
							// do SP_OPTDIAG, but only on UNPARTITIONED tables
							sql="declare @partitions int \n" +
								"select @partitions = count(*) \n" +
								"from " + entry.getDbname() + "..sysobjects o, " + entry.getDbname() + "..sysusers u, " + entry.getDbname() + "..syspartitions p \n" +
								"where o.name  = '" + entry.getObjectName() + "' \n" +
								"  and u.name  = '" + entry.getOwner() + "' \n" +
								"  and o.id    = p.id \n" +
								"  and o.uid   = o.uid \n" +
								"  and p.indid = 0 \n" +
								"                  \n" +
								"if (@partitions > 1) \n" +
								"    print 'Table is partitioned, and this is not working so well with sp__optdiag, sorry.' \n" +
								"else \n" +
								"    exec " + entry.getDbname() + "..sp__optdiag '" + entry.getSchemaAndObjectName() + "' \n" +
								"";

							try	(AseSqlScript ss = new AseSqlScript(conn, 10, false)) 
							{
								ss.setUseGlobalMsgHandler(false);
								ss.setMsgPrefix("sp__optdiag:" + entry.getFullObjectName() + ": ");

								entry.setOptdiagText( ss.executeSqlStr(sql, true) ); 
							} 
							catch (SQLException e) 
							{ 
								entry.setOptdiagText( e.toString() ); 
							}
						}
					}
		
					//--------------------------------------------
					// GET SOME OTHER STATISTICS
					sql = "exec " + entry.getDbname() + "..sp_spaceused '" + entry.getSchemaAndObjectName() + "', 1 ";  // ,1 = get details on indexes
	
					try	(AseSqlScript ss = new AseSqlScript(conn, 10, false)) 
					{
						ss.setUseGlobalMsgHandler(false);
						ss.setMsgPrefix("sp_spaceused:" + entry.getFullObjectName() + ": ");

						entry.setExtraInfoText( ss.executeSqlStr(sql, true) ); 
					} 
					catch (SQLException e) 
					{ 
						entry.setExtraInfoText( e.toString() ); 
					}

					// TODO: more info to save
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
					//                          SELECT name AS index_name
					//                              , indid AS index_id
					//                              , derived_stat(id, indid, 'data page cluster ratio')  AS [data page cluster ratio]
					//                              , derived_stat(id, indid, 'index page cluster ratio') AS [index page cluster ratio]
					//                              , derived_stat(id, indid, 'data row cluster ratio')   AS [data row cluster ratio]
					//                              , derived_stat(id, indid, 'large io efficiency')      AS [large io efficiency]
					//                              , derived_stat(id, indid, 'space utilization')        AS [space utilization]
					//                          FROM <dbname>.dbo.sysindexes
					//                          WHERE id = <objectid>
					// - try to calculate "if tab has a lot of unused space / fragmented"

				}

				else if (
					   "P" .equals(type) // Proecedure
				    || "TR".equals(type) // Trigger
				    || "V" .equals(type) // View
				    || "D" .equals(type) // Default
				    || "R" .equals(type) // Rule
				    || "XP".equals(type)) // eXtened Procedure
				{
					// This should be stored
					returnList.add(entry);
					
					//--------------------------------------------
					// GET OBJECT TEXT
					String sql = " select c.text "
						+ " from " + entry.getDbname() + "..sysobjects o, " + entry.getDbname() + "..syscomments c, " + entry.getDbname() + "..sysusers u \n"
						+ " where o.name = '" + entry.getObjectName() + "' \n"
						+ "   and u.name = '" + entry.getOwner()      + "' \n" 
						+ "   and o.id   = c.id \n"
						+ "   and o.uid  = u.uid \n"
						+ " order by c.number, c.colid2, c.colid ";
	
					String sqlText = "";

					try (Statement statement = conn.createStatement(); ResultSet rs = statement.executeQuery(sql) )
					{
						while(rs.next())
						{
							sqlText += rs.getString(1);
						}

						entry.setObjectText( sqlText );
					}
					catch (SQLException e)
					{
						entry.setObjectText( e.toString() );
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
							Set<String> tableList = SqlParserUtils.getTables(sqlText);
//							List<String> tableList = Collections.emptyList();
//							try
//							{
//								tableList = SqlParserUtils.getTables(sqlText, true); 
//							}
//							catch (ParseException pex) 
//							{ 
//								_logger.warn("Problems Parsing VIEW '" + entry.getObjectName() + "' to get table names. SqlText=|" + sqlText + "|. Skipping and continuing...", pex);
//							}
								
							// Post to DDL Storage, for lookup
							for (String tableName : tableList)
							{
								pch.addDdl(dbname, tableName, this.getClass().getSimpleName() + ".resolve.view");
							}

							// Add table list to the saved entry
							entry.setExtraInfoText( "TableList: " + StringUtil.toCommaStr(tableList) ); 
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
					String sql = "exec " + entry.getDbname() + "..sp_depends '" + entry.getSchemaAndObjectName() + "' "; 
					
					try	(AseSqlScript ss = new AseSqlScript(conn, 10, false)) 
					{
						ss.setUseGlobalMsgHandler(false);
						ss.setMsgPrefix("sp_depends:" + entry.getFullObjectName() + ": ");
						//ss.setSybMessageNumberDebug(true);

						entry.setDependsText( ss.executeSqlStr(sql, true) ); 
					} 
					catch (SQLException e) 
					{ 
						entry.setDependsText( e.toString() ); 
					}
		
					if (pch.getConfig_addDependantObjectsToDdlInQueue())
					{
						sql = "exec " + entry.getDbname() + "..sp_depends '" + entry.getOwner() + "." + entry.getObjectName() + "' "; 
		
						ArrayList<String> dependList = new ArrayList<String>();
						try
						{
							Statement statement = conn.createStatement();
							ResultSet rs = statement.executeQuery(sql);
							ResultSetMetaData rsmd = rs.getMetaData();
		
							// lets search for 'object' column, in no case, if it changes...
							int object_pos = -1;
							for (int c=1; c<=rsmd.getColumnCount(); c++)
							{
								if (rsmd.getColumnLabel(c).toLowerCase().equals("object"))
								{
									object_pos = c;
									break;
								}
							}
							if (object_pos > 0)
							{
								while(rs.next())
								{
									// Get the dependent object name
									String depOnObjectName = rs.getString(object_pos);
		
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
										pch.addDdl(entry.getDbname(), shortObjName, source, objectName, dependLevel + 1);
									}
								}
							}
							else
							{
								_logger.debug("When getting dependent objects using 'sp_depends', I was expecting a column named 'object'. But it wasn't found. The result set had " + rsmd.getColumnCount() + " columns. Skipping lookup for dependent object for '" + entry.getFullObjectName() + "'.");
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
								_logger.warn("Problems getting 'sp_depends' for table '" + entry.getFullObjectName() + "'. SqlState='" + e.getSQLState() + "', Caught: " + e);
							}
						}
						if (dependList.size() > 0)
							entry.setDependList(dependList);
					}
				} // end: doSpDepends

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
		
		// get TYPE, OWNER CREATION_TIME and DBNAME where the record(s) was found 
		String sql = ""
				+ "declare @objectname varchar(255) \n"
				+ "select @objectname = '" + objectName + "' \n"   // Trust that the DBMS will handle Case Sensitivity
				+ "if exists (select 1 from " + dbname + ".dbo.sysobjects o, " + dbname + ".dbo.sysusers u where o.name = @objectname and o.uid = u.uid) \n"
				+ "begin \n"
				+ "    select o.type \n"           // Type
				+ "          ,u.name \n"           // Schema Name
				+ "          ,o.name \n"           // Object Name
				+ "          ,o.id \n"             // ID
				+ "          ,o.crdate \n"         // Creation Date
				+ "          ,'" + dbname + "' \n" // dbname
				+ "    from " + dbname + ".dbo.sysobjects o, " + dbname + ".dbo.sysusers u \n"
				+ "    where o.name = @objectname \n"
				+ "      and o.uid = u.uid \n"
				+ "end \n"
				+ "else if ( substring(@objectname, 1, 3) = 'sp_' ) \n"
				+ "begin \n"
				+ "    select o.type \n"           // Type             
				+ "          ,u.name \n"           // Schema Name      
				+ "          ,o.name \n"           // Object Name      
				+ "          ,o.id \n"             // ID               
				+ "          ,o.crdate \n"         // Creation Date    
				+ "          ,'sybsystemprocs' \n" // dbname           
				+ "    from sybsystemprocs.dbo.sysobjects o, sybsystemprocs.dbo.sysusers u \n"
				+ "    where o.name = @objectname \n"
				+ "      and o.uid = u.uid \n"
				+ "end \n"
				+ "else \n"
				+ "begin \n"
				+ "    -- create a empty/dummy ResultSet with NO rows\n"
				+ "    select '', '', convert(datetime, null), '' from sysobjects where id is null \n"
				+ "end \n"
				+ "";
			
		try ( Statement statement = conn.createStatement(); ResultSet rs = statement.executeQuery(sql) )
		{
			while(rs.next())
			{
				DdlDetails entry = new DdlDetails();

				entry.setSearchDbname    ( dbname );
				entry.setObjectName      ( objectName );
				entry.setSearchObjectName( originObjectName ); // NOT Stored in DDL Store, used for: isDdlDetailsStored(), markDdlDetailsAsStored()
				entry.setSource          ( source );
				entry.setDependParent    ( dependParent );
				entry.setDependLevel     ( dependLevel );
				entry.setSampleTime      ( new Timestamp(System.currentTimeMillis()) );

				entry.setType            ( rs.getString   (1) );
				entry.setOwner           ( rs.getString   (2) );
				entry.setObjectName      ( rs.getString   (3) ); // Use the object name stored in the DBMS (for Sybase ASE it will always be stored as it was originally created)
				entry.setObjectId        ( rs.getInt      (4) );
				entry.setCrdate          ( rs.getTimestamp(5) );
				entry.setDbname          ( rs.getString   (6) );

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
