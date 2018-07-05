package com.asetune.pcs.inspection;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.asetune.cache.XmlPlanCache;
import com.asetune.cache.XmlPlanCacheAse;
import com.asetune.cm.sql.VersionInfo;
import com.asetune.pcs.DdlDetails;
import com.asetune.pcs.ObjectLookupQueueEntry;
import com.asetune.pcs.PersistentCounterHandler;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.AseSqlScript;
import com.asetune.utils.Ver;

public class ObjectLookupInspectorAse
extends ObjectLookupInspectorAbstract
{
	private static Logger _logger = Logger.getLogger(ObjectLookupInspectorAse.class);

	private int _dbmsVersion = 0;

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
		// Discard entries '*??', but allow '*ss' and '*sq'
		if (objectName.startsWith("*"))
		{
			if (objectName.startsWith("*ss"))
			{
				// set the database to be statement cache, so the PersistentCounterHandler.addDdl() can discard the entry if it's already stored
				entry._dbname = PersistentCounterHandler.STATEMENT_CACHE_NAME;

				// ALLOW: Statement Cache entry 
			}
			else if (objectName.startsWith("*sq"))
			{
				// set the database to be statement cache, so the PersistentCounterHandler.addDdl() can discard the entry if it's already stored
				entry._dbname = PersistentCounterHandler.STATEMENT_CACHE_NAME;

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
				// ffffff = Hexadecimal value for something, which I did not figure out
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
	public DdlDetails doObjectInfoLookup(DbxConnection conn, ObjectLookupQueueEntry qe, PersistentCounterHandler pch)
	{
		String dbname       = qe._dbname;
		String objectName   = qe._objectName;
		String source       = qe._source;
		String dependParent = qe._dependParent;
		int    dependLevel  = qe._dependLevel;

		// FIXME: dbname  can be a Integer
		// FIXME: objName can be a Integer
		// Do the lookup, then check _ddlCache if it has been stored.

		// Check if the input is a: Statement Cache object
		boolean isStatementCache = false;
		String  ssqlid = null;
		if (objectName.startsWith("*ss") || objectName.startsWith("*sq") ) // *sq in ASE 15.7 esd#2, DynamicSQL can/will end up in statement cache
		{
			isStatementCache = true;
			dbname           = PersistentCounterHandler.STATEMENT_CACHE_NAME;
			int sep = objectName.indexOf('_');
			ssqlid = objectName.substring(3, sep);
			//haskey = objectName.substring(sep+1, objectName.length()-3);
		}


		// Statement Cache objects
		if (isStatementCache)
		{
			DdlDetails entry = new DdlDetails(dbname, objectName);
			entry.setCrdate( new Timestamp(System.currentTimeMillis()) );
			entry.setSource( source );
			entry.setDependLevel( dependLevel );
			entry.setOwner("ssql");
			entry.setType("SS");
			entry.setSampleTime( new Timestamp(System.currentTimeMillis()) );

			// Get the XmlPlanCache
			if (XmlPlanCache.hasInstance())
			{
    			XmlPlanCache xmlPlanCache = XmlPlanCache.getInstance();
    			if ( xmlPlanCache.isPlanCached(objectName) )
    			{
    				entry.setObjectText( "Xml Plan was cached, so only showing the XML Plan... fetched with: select show_cached_plan_in_xml("+ssqlid+", 0, 0)" ); 
    				entry.setExtraInfoText( xmlPlanCache.getPlan(objectName) );
    
    				// Since we got the XML info without talking to the DBMS, there is no need to sleep since we didn't produce any "load" at it... 
    				entry.setSleepOption(false);
    				return entry;
    			}
			}
			
			String sql = 
				"set switch on 3604 with no_info \n" +
				"dbcc prsqlcache("+ssqlid+", 1) "; // 1 = also prints showplan"
			
			AseSqlScript ss = new AseSqlScript(conn, 10, false);
			ss.setUseGlobalMsgHandler(false);
			ss.setMsgPrefix("dbcc prsqlcache:"+dbname+"."+objectName+": ");
			try	{ 
				entry.setObjectText( ss.executeSqlStr(sql, true) ); 
			} catch (SQLException e) { 
				entry.setObjectText( e.toString() ); 
			} finally {
				ss.close();
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

				sql = "select show_cached_plan_in_xml("+ssqlid+", 0, 0)";

//				ss = new AseSqlScript(conn, 10, false);
//				try	{
//					entry.setExtraInfoText( ss.executeSqlStr(sql, true) );
//				} catch (SQLException e) {
//					entry.setExtraInfoText( e.toString() );
//				} finally {
//					ss.close();
//				}

				boolean rejectPlan = false;
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
							XmlPlanCache.getInstance().setPlan(objectName, xmlStr);
					}
					rs.close();
					stmnt.close();

					entry.setExtraInfoText( sb.toString().trim() );
				}
				catch(SQLException e)
				{
					String msg = "Problems getting text from Statement Cache about '"+objectName+"'. Msg="+e.getErrorCode()+", Text='" + e.getMessage() + "'. Caught: "+e;
					_logger.warn(msg); 
					entry.setExtraInfoText( msg );
				}
				
				// If the status of the plan is "not good enough", lets not save it... Hopefully we will get a better plan at next attempt.
				if (rejectPlan)
					return null;
			}
			return entry;
//			_ddlStoreQueue.add(entry);
//			fireQueueSizeChange();
		}
		else // all other tables
		{
//			String sql = "";
			// Keep a list of objects we need to work with
			// because: if we get more than one proc/table with different owners
			ArrayList<DdlDetails> objectList = new ArrayList<DdlDetails>();
			
			// get TYPE, OWNER CREATION_TIME and DBNAME where the record(s) was found 
			StringBuilder qsb = new StringBuilder(); 
				qsb.append("declare @objectname varchar(255) \n");
				qsb.append("select @objectname = '").append(objectName).append("' \n");
				qsb.append("if exists (select 1 from ").append(dbname).append(".dbo.sysobjects o, ").append(dbname).append(".dbo.sysusers u where o.name = @objectname and o.uid = u.uid) \n");
				qsb.append("begin \n");
				qsb.append("    select o.type, u.name, o.crdate, '").append(dbname).append("' \n");
				qsb.append("    from ").append(dbname).append(".dbo.sysobjects o, ").append(dbname).append(".dbo.sysusers u \n");
				qsb.append("    where o.name = @objectname \n");
				qsb.append("      and o.uid = u.uid \n");
				qsb.append("end \n");
				qsb.append("else if ( substring(@objectname, 1, 3) = 'sp_' ) \n");
				qsb.append("begin \n");
				qsb.append("    select o.type, u.name, o.crdate, 'sybsystemprocs' \n");
				qsb.append("    from sybsystemprocs.dbo.sysobjects o, sybsystemprocs.dbo.sysusers u \n");
				qsb.append("    where o.name = @objectname \n");
				qsb.append("      and o.uid = u.uid \n");
				qsb.append("end \n");
				qsb.append("else \n");
				qsb.append("begin \n");
				qsb.append("    -- create a empty/dummy ResultSet with NO rows\n");
				qsb.append("    select '', '', convert(datetime, null), '' from sysobjects where id is null \n");
				qsb.append("end \n");
				
			String sql = qsb.toString();
			try
			{
				Statement statement = conn.createStatement();
				ResultSet rs = statement.executeQuery(sql);
				while(rs.next())
				{
					DdlDetails entry = new DdlDetails();
					
//					entry.setDbname      ( dbname );
					entry.setSearchDbname( dbname );
					entry.setObjectName  ( objectName );
					entry.setSource      ( source );
					entry.setDependParent( dependParent );
					entry.setDependLevel ( dependLevel );
					entry.setSampleTime  ( new Timestamp(System.currentTimeMillis()) );

					entry.setType       ( rs.getString   (1) );
					entry.setOwner      ( rs.getString   (2) );
					entry.setCrdate     ( rs.getTimestamp(3) );
					entry.setDbname     ( rs.getString   (4) );

					objectList.add(entry);
				}
				rs.close();
			}
			catch (SQLException e)
			{
				_logger.error("Problems Getting basic information about DDL for dbname='"+dbname+"', objectName='"+objectName+"', source='"+source+"', dependLevel="+dependLevel+". Skipping DDL Storage of this object. Caught: "+e);
				return null;
			}
	
			// The object was NOT found
			if (objectList.size() == 0)
			{
				_logger.info("DDL Lookup. Can't find any information for dbname='"+dbname+"', objectName='"+objectName+"', source='"+source+"', dependLevel="+dependLevel+". Skipping DDL Storage of this object.");
				return null;
			}

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
				AseSqlScript ss;
	
				if ( "U".equals(type) || "S".equals(type) )
				{
					//--------------------------------------------
					// Just do sp_help, or use ddlgen.jar to get info
					sql = "exec "+entry.getDbname()+"..sp_help '"+entry.getOwner()+"."+entry.getObjectName()+"' ";
	
					ss = new AseSqlScript(conn, 10, false);
					ss.setUseGlobalMsgHandler(false);
					ss.setMsgPrefix("sp_help:"+entry.getDbname()+"."+entry.getOwner()+"."+entry.getObjectName()+": ");
					try	{ 
						entry.setObjectText( ss.executeSqlStr(sql, true) ); 
					} catch (SQLException e) { 
						entry.setObjectText( e.toString() ); 
					} finally {
						ss.close();
					}
	
					//--------------------------------------------
					// GET sp__optdiag
					if (_dbmsVersion >= Ver.ver(15,7))
					{
						sql = "exec "+entry.getDbname()+"..sp_showoptstats '"+entry.getOwner()+"."+entry.getObjectName()+"' ";

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
							String msg = "Problems getting sp_showoptstats, using sql '"+sql+"'. Msg="+e.getErrorCode()+", Text='" + e.getMessage() + "'. Caught: "+e;
							//_logger.warn(msg); 
							entry.setOptdiagText( msg );
						}
					}
					else
					{
						// do SP_OPTDIAG, but only on UNPARTITIONED tables
						sql="declare @partitions int \n" +
							"select @partitions = count(*) \n" +
							"from "+entry.getDbname()+"..sysobjects o, "+entry.getDbname()+"..sysusers u, "+entry.getDbname()+"..syspartitions p \n" +
							"where o.name = '"+entry.getObjectName()+"' \n" +
							"  and u.name = '"+entry.getOwner()+"' \n" +
							"  and o.id  = p.id \n" +
							"  and o.uid = o.uid \n" +
							"  and p.indid = 0 \n" +
							"                  \n" +
							"if (@partitions > 1) \n" +
							"    print 'Table is partitioned, and this is not working so well with sp__optdiag, sorry.' \n" +
							"else \n" +
							"    exec "+entry.getDbname()+"..sp__optdiag '"+entry.getOwner()+"."+entry.getObjectName()+"' \n" +
							"";

						ss = new AseSqlScript(conn, 10, false);
						ss.setUseGlobalMsgHandler(false);
						ss.setMsgPrefix("sp__optdiag:"+entry.getDbname()+"."+entry.getOwner()+"."+entry.getObjectName()+": ");
						try	{ 
							entry.setOptdiagText( ss.executeSqlStr(sql, true) ); 
						} catch (SQLException e) { 
							entry.setOptdiagText( e.toString() ); 
						} finally {
							ss.close();
						}
				}
		
					//--------------------------------------------
					// GET SOME OTHER STATISTICS
					sql = "exec "+entry.getDbname()+"..sp_spaceused '"+entry.getOwner()+"."+entry.getObjectName()+"', 1 ";  // ,1 = get details on indexes
	
					ss = new AseSqlScript(conn, 10, false);
					ss.setUseGlobalMsgHandler(false);
					ss.setMsgPrefix("sp_spaceused:"+entry.getDbname()+"."+entry.getOwner()+"."+entry.getObjectName()+": ");
					try	{ 
						entry.setExtraInfoText( ss.executeSqlStr(sql, true) ); 
					} catch (SQLException e) { 
						entry.setExtraInfoText( e.toString() ); 
					} finally {
						ss.close();
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
					// - try to calculate "if tab has a lot of unused space / fragmented"
				}
				if (   "P" .equals(type) 
				    || "TR".equals(type) 
				    || "V" .equals(type) 
				    || "D" .equals(type) 
				    || "R" .equals(type) 
				    || "XP".equals(type))
				{
					//--------------------------------------------
					// GET OBJECT TEXT
					sql = " select c.text "
						+ " from "+entry.getDbname()+"..sysobjects o, "+entry.getDbname()+"..syscomments c, "+entry.getDbname()+"..sysusers u \n"
						+ " where o.name = '"+entry.getObjectName()+"' \n"
						+ "   and u.name = '"+entry.getOwner()+"' \n" 
						+ "   and o.id   = c.id \n"
						+ "   and o.uid  = u.uid \n"
						+ " order by c.number, c.colid2, c.colid ";
	
					try
					{
						StringBuilder sb = new StringBuilder();
	
						Statement statement = conn.createStatement();
						ResultSet rs = statement.executeQuery(sql);
						while(rs.next())
						{
							String textPart = rs.getString(1);
							sb.append(textPart);
						}
						rs.close();
						statement.close();
	
						entry.setObjectText( sb.toString() );
					}
					catch (SQLException e)
					{
						entry.setObjectText( e.toString() );
					}
				}
				else
				{
					// Unknown type
				}
	
				//--------------------------------------------
				// GET sp_depends
				sql = "exec "+entry.getDbname()+"..sp_depends '"+entry.getOwner()+"."+entry.getObjectName()+"' "; 
	
				ss = new AseSqlScript(conn, 10, false, skipSpDependsMessages);
				ss.setUseGlobalMsgHandler(false);
				ss.setMsgPrefix("sp_depends:"+entry.getDbname()+"."+entry.getOwner()+"."+entry.getObjectName()+": ");
				//ss.setSybMessageNumberDebug(true);
				try	{ 
					entry.setDependsText( ss.executeSqlStr(sql, true) ); 
				} catch (SQLException e) { 
					entry.setDependsText( e.toString() ); 
				} finally {
					ss.close();
				}
	
				if (pch.getConfig_addDependantObjectsToDdlInQueue())
				{
					sql = "exec "+entry.getDbname()+"..sp_depends '"+entry.getOwner()+"."+entry.getObjectName()+"' "; 
	
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
							_logger.debug("When getting dependent objects using 'sp_depends', I was expecting a column named 'object'. But it wasn't found. The result set had "+rsmd.getColumnCount()+" columns. Skipping lookup for dependent object for '"+entry.getFullObjectName()+"'.");
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
							_logger.warn("Problems getting 'sp_depends' for table '"+entry.getFullObjectName()+"'. SqlState='"+e.getSQLState()+"', Caught: "+e);
						}
					}
					if (dependList.size() > 0)
						entry.setDependList(dependList);
				}

				return entry;
//				int qsize = _ddlStoreQueue.size();
//				if (qsize > _warnDdlStoreQueueSizeThresh)
//				{
//					_logger.warn("The DDL Storage queue has "+qsize+" entries. The persistent writer might not keep in pace.");
//				}
//				_ddlStoreQueue.add(entry);
//				fireQueueSizeChange();
	
			} // end: for (DdlDetails entry : objectList)

		} // end: isStatementCache == false
		
		return null;
	}


	
//	/**
//	 * Get DDL information from the database and pass it on to the storage thread
//	 * 
//	 * @param qe
//	 * @param prevLookupTimeMs
//	 * @return true if it did a lookup, false the lookup was discarded
//	 */
//	private boolean doObjectInfoLookup(ObjectLookupQueueEntry qe, long prevLookupTimeMs)
//	{
//		DbxConnection conn = getLookupConnection();
//		if (conn == null)
//			return false;
//
//		_objectLookupInspector.doObjectInfoLookup(conn, qe, prevLookupTimeMs);
//		
////The below should be moved into an DbmsObjectInfoLookup class that is implemented differently for the various DBMS we should monitor
//
//
//		String dbname       = qe._dbname;
//		String objectName   = qe._objectName;
//		String source       = qe._source;
//		String dependParent = qe._dependParent;
//		int    dependLevel  = qe._dependLevel;
//
//		// FIXME: dbname  can be a Integer
//		// FIXME: objName can be a Integer
//		// Do the lookup, then check _ddlCache if it has been stored.
//
//		// Statement Cache object
//		boolean isStatementCache = false;
//		String  ssqlid = null;
//		if (objectName.startsWith("*ss") || objectName.startsWith("*sq") ) // *sq in ASE 15.7 esd#2, DynamicSQL can/will end up in statement cache
//		{
//			isStatementCache = true;
//			dbname           = STATEMENT_CACHE_NAME;
//			int sep = objectName.indexOf('_');
//			ssqlid = objectName.substring(3, sep);
//			//haskey = objectName.substring(sep+1, objectName.length()-3);
//		}
//
//		// check AGAIN if DDL has NOT been saved in any writer class
//		boolean doLookup = false;
//		for (IPersistWriter pw : _writerClasses)
//		{
//			if ( ! pw.isDdlDetailsStored(dbname, objectName) )
//			{
//				doLookup = true;
//				break;
//			}
//		}
//		if ( ! doLookup )
//		{
//			_logger.debug("doObjectInfoLookup(): The DDL for dbname '"+dbname+"', objectName '"+objectName+"' has already been stored by all the writers.");
//			return false;
//		}
//
//		if (_logger.isDebugEnabled())
//			_logger.debug("Getting DDL information about object '"+dbname+"."+objectName+"', InputQueueSize="+_ddlInputQueue.size()+", StoreQueueSize="+_ddlStoreQueue.size());
//
//		// Print INFO message if IN-QUEUE is above X and a certain time has pased
//		if (_ddlInputQueue.size() > _ddlLookup_infoMessage_queueSize)
//		{
//			long howLongAgo = System.currentTimeMillis() - _ddlLookup_infoMessage_last;
//			if (_ddlLookup_infoMessage_period > howLongAgo)
//			{
//				_logger.info("DDL Lookup: InputQueueSize="+_ddlInputQueue.size()+", StoreQueueSize="+_ddlStoreQueue.size()+". Now getting DDL information about object '"+dbname+"."+objectName+"',");
//				_ddlLookup_infoMessage_last = System.currentTimeMillis();
//			}
//		}
//
//		// Statement Cache objects
//		if (isStatementCache)
//		{
//			DdlDetails entry = new DdlDetails(dbname, objectName);
//			entry.setCrdate( new Timestamp(System.currentTimeMillis()) );
//			entry.setSource( source );
//			entry.setDependLevel( dependLevel );
//			entry.setOwner("ssql");
//			entry.setType("SS");
//			entry.setSampleTime( new Timestamp(System.currentTimeMillis()) );
//			String sql = 
//				"set switch on 3604 with no_info \n" +
//				"dbcc prsqlcache("+ssqlid+", 1) "; // 1 = also prints showplan"
//			
//			AseSqlScript ss = new AseSqlScript(conn, 10);
//			try	{ 
//				entry.setObjectText( ss.executeSqlStr(sql, true) ); 
//			} catch (SQLException e) { 
//				entry.setObjectText( e.toString() ); 
//			} finally {
//				ss.close();
//			}
//			
////			if (_dbmsVersion >= 15700)
////			if (_dbmsVersion >= 1570000)
//			if (_dbmsVersion >= Ver.ver(15,7))
//			{
//				//-----------------------------------------------------------
//				// From Documentation on: show_cached_plan_in_xml(statement_id, plan_id, level_of_detail)
//				//-----------------------------------------------------------
//				// statement_id
//				//			     is the object ID of the lightweight procedure (A procedure that can be created and invoked 
//				//			     internally by Adaptive Server). This is the SSQLID from monCachedStatement.
//				// 
//				// plan_id
//				//			     is the unique identifier for the plan. This is the PlanID from monCachedProcedures. 
//				//			     A value of zero for plan_id displays the showplan output for all cached plans for the indicated SSQLID.
//				// 
//				// level_of_detail
//				//			     is a value from 0 - 6 indicating the amount of detail show_cached_plan_in_xml returns (see Table 2-6). 
//				//			     level_of_detail determines which sections of showplan are returned by show_cached_plan_in_xml. 
//				//			     The default value is 0.
//				// 
//				//			     The output of show_cached_plan_in_xml includes the plan_id and these sections:
//				// 
//				//			         parameter - contains the parameter values used to compile the query and the parameter values 
//				//			                     that caused the slowest performance. The compile parameters are indicated with the 
//				//			                     <compileParameters> and </compileParameters> tags. The slowest parameter values are 
//				//			                     indicated with the <execParameters> and </execParameters> tags. 
//				//			                     For each parameter, show_cached_plan_in_xml displays the:
//				//			                        Number
//				//			                        Datatype
//				//			                        Value:    values that are larger than 500 bytes and values for insert-value statements 
//				//			                                  do not appear. The total memory used to store the values for all parameters 
//				//			                                  is 2KB for each of the two parameter sets.
//				// 
//				//			         opTree    - contains the query plan and the optimizer estimates. 
//				//			                     The opTree section is delineated by the <opTree> and </opTree> tags.
//				// 
//				//			         execTree  - contains the query plan with the lava operator details. 
//				//			                     The execTree section is identified by the tags <execTree> and </execTree>.
//				//
//				// level_of_detail parameter opTree execTree
//				// --------------- --------- ------ --------
//				// 0 (the default)       YES    YES         
//				// 1                     YES                
//				// 2                            YES         
//				// 3                                     YES
//				// 4                            YES      YES
//				// 5                     YES             YES
//				// 6                     YES    YES      YES
//				//-----------------------------------------------------------
//
//				sql = "select show_cached_plan_in_xml("+ssqlid+", 0, 0)";
//
////				ss = new AseSqlScript(conn, 10);
////				try	{
////					entry.setExtraInfoText( ss.executeSqlStr(sql, true) );
////				} catch (SQLException e) {
////					entry.setExtraInfoText( e.toString() );
////				} finally {
////					ss.close();
////				}
//				try
//				{
//					Statement stmnt = conn.createStatement();
//					stmnt.setQueryTimeout(10);
//					
//					ResultSet rs = stmnt.executeQuery(sql);
//
//					StringBuilder sb = new StringBuilder();
//					sb.append(sql).append("\n");
//					sb.append("------------------------------------------------------------------\n");
//					while (rs.next())
//					{
//						sb.append(rs.getString(1));
//					}
//					rs.close();
//					stmnt.close();
//
//					entry.setExtraInfoText( sb.toString().trim() );
//				}
//				catch(SQLException e)
//				{
//					String msg = "Problems getting text from Statement Cache about '"+objectName+"'. Msg="+e.getErrorCode()+", Text='" + e.getMessage() + "'. Caught: "+e;
//					_logger.warn(msg); 
//					entry.setExtraInfoText( msg );
//				}
//			}
//			_ddlStoreQueue.add(entry);
//			fireQueueSizeChange();
//		}
//		else // all other tables
//		{
//			// Keep a list of objects we need to work with
//			// because: if we get more than one proc/table with different owners
//			ArrayList<DdlDetails> objectList = new ArrayList<DdlDetails>();
//			
//			// GET type and creation time
//			String sql = 
//				"select o.type, u.name, o.crdate \n" +
//				"from "+dbname+"..sysobjects o, "+dbname+"..sysusers u \n" +
//				"where o.name = '"+objectName+"' \n" +
//				"  and o.uid = u.uid ";
//			try
//			{
//				Statement statement = conn.createStatement();
//				ResultSet rs = statement.executeQuery(sql);
//				while(rs.next())
//				{
//					DdlDetails entry = new DdlDetails();
//					
//					entry.setDbname      ( dbname );
//					entry.setObjectName  ( objectName );
//					entry.setSource      ( source );
//					entry.setDependParent( dependParent );
//					entry.setDependLevel ( dependLevel );
//					entry.setSampleTime  ( new Timestamp(System.currentTimeMillis()) );
//
//					entry.setType       ( rs.getString   (1) );
//					entry.setOwner      ( rs.getString   (2) );
//					entry.setCrdate     ( rs.getTimestamp(3) );
//					
//					objectList.add(entry);
//				}
//				rs.close();
//			}
//			catch (SQLException e)
//			{
//				_logger.error("Problems Getting basic information about DDL for dbname='"+dbname+"', objectName='"+objectName+"', source='"+source+"', dependLevel="+dependLevel+". Skipping DDL Storage of this object. Caught: "+e);
//				return false;
//			}
//	
//			// The object was NOT found
//			if (objectList.size() == 0)
//			{
//				_logger.info("DDL Lookup. Can't find any information for dbname='"+dbname+"', objectName='"+objectName+"', source='"+source+"', dependLevel="+dependLevel+". Skipping DDL Storage of this object.");
//				return false;
//			}
//	
//	
//			for (DdlDetails entry : objectList)
//			{
//				String type = entry.getType();
//				// Type definition from ASE 15.7, manual
//				//   C  - computed column
//				//   D  - default
//				//   DD - decrypt default
//				//   F  - SQLJ function
//				//   N  - partition condition
//				//   P  - Transact-SQL or SQLJ procedure
//				//   PP - the predicate of a privilege
//				//   PR - prepare objects (created by Dynamic SQL)
//				//   R  - rule
//				//   RI - referential constraint
//				//   S  - system table
//				//   TR - trigger
//				//   U  - user table
//				//   V  - view
//				//   XP - extended stored procedure.
//				AseSqlScript ss;
//	
//				if ( "U".equals(type) || "S".equals(type) )
//				{
//					//--------------------------------------------
//					// Just do sp_help, or use ddlgen.jar to get info
//					sql = "exec "+entry.getDbname()+"..sp_help '"+entry.getOwner()+"."+entry.getObjectName()+"' ";
//	
//					ss = new AseSqlScript(conn, 10);
//					try	{ 
//						entry.setObjectText( ss.executeSqlStr(sql, true) ); 
//					} catch (SQLException e) { 
//						entry.setObjectText( e.toString() ); 
//					} finally {
//						ss.close();
//					}
//	
//					//--------------------------------------------
//					// GET sp__optdiag
////					if (_dbmsVersion >= 15700)
////					if (_dbmsVersion >= 1570000)
//					if (_dbmsVersion >= Ver.ver(15,7))
//					{
//						sql = "exec "+entry.getDbname()+"..sp_showoptstats '"+entry.getOwner()+"."+entry.getObjectName()+"' ";
//
//						try
//						{
//							Statement stmnt = conn.createStatement();
//							stmnt.setQueryTimeout(10);
//							
//							ResultSet rs = stmnt.executeQuery(sql);
//
//							StringBuilder sb = new StringBuilder();
//							sb.append(sql).append("\n");
//							sb.append("------------------------------------------------------------------\n");
//							while (rs.next())
//							{
//								sb.append(rs.getString(1));
//							}
//							rs.close();
//							stmnt.close();
//
//							entry.setOptdiagText( sb.toString().trim() );
//						}
//						catch(SQLException e)
//						{
//							String msg = "Problems getting sp_showoptstats, using sql '"+sql+"'. Msg="+e.getErrorCode()+", Text='" + e.getMessage() + "'. Caught: "+e;
//							//_logger.warn(msg); 
//							entry.setOptdiagText( msg );
//						}
//					}
//					else
//					{
//						// do SP_OPTDIAG, but only on UNPARTITIONED tables
//						sql="declare @partitions int \n" +
//							"select @partitions = count(*) \n" +
//							"from "+entry.getDbname()+"..sysobjects o, "+entry.getDbname()+"..sysusers u, "+entry.getDbname()+"..syspartitions p \n" +
//							"where o.name = '"+entry.getObjectName()+"' \n" +
//							"  and u.name = '"+entry.getOwner()+"' \n" +
//							"  and o.id  = p.id \n" +
//							"  and o.uid = o.uid \n" +
//							"  and p.indid = 0 \n" +
//							"                  \n" +
//							"if (@partitions > 1) \n" +
//							"    print 'Table is partitioned, and this is not working so well with sp__optdiag, sorry.' \n" +
//							"else \n" +
//							"    exec "+entry.getDbname()+"..sp__optdiag '"+entry.getOwner()+"."+entry.getObjectName()+"' \n" +
//							"";
//
//						ss = new AseSqlScript(conn, 10);
//						try	{ 
//							entry.setOptdiagText( ss.executeSqlStr(sql, true) ); 
//						} catch (SQLException e) { 
//							entry.setOptdiagText( e.toString() ); 
//						} finally {
//							ss.close();
//						}
//				}
//		
//					//--------------------------------------------
//					// GET SOME OTHER STATISTICS
//					sql = "exec "+entry.getDbname()+"..sp_spaceused '"+entry.getOwner()+"."+entry.getObjectName()+"', 1 ";
//	
//					ss = new AseSqlScript(conn, 10);
//					try	{ 
//						entry.setExtraInfoText( ss.executeSqlStr(sql, true) ); 
//					} catch (SQLException e) { 
//						entry.setExtraInfoText( e.toString() ); 
//					} finally {
//						ss.close();
//					}
//					// TODO: more info to save
//					// - datachange(table_name, partition_name, column_name)
//					// - can we get some other statistics from sysstatistics
//					//   like when was statistics updated for this table
//					// - function: derived_stats(objnamme|id, indexname|indexid, [ptn_name|ptn_id], 'stats')
//					//             stats = dpcr | data page cluster ratio
//					//                     ipcr | index page cluster ratio
//					//                     drcr | data row cluster ratio
//					//                     lgio | large io efficiency
//					//                     sput | space utilization
//					//             to get Cluster Ratio etc...
//					// - try to calculate "if tab has a lot of unused space / fragmented"
//				}
//				if (   "P" .equals(type) 
//				    || "TR".equals(type) 
//				    || "V" .equals(type) 
//				    || "D" .equals(type) 
//				    || "R" .equals(type) 
//				    || "XP".equals(type))
//				{
//					//--------------------------------------------
//					// GET OBJECT TEXT
//					sql = " select c.text "
//						+ " from "+entry.getDbname()+"..sysobjects o, "+entry.getDbname()+"..syscomments c, "+entry.getDbname()+"..sysusers u \n"
//						+ " where o.name = '"+entry.getObjectName()+"' \n"
//						+ "   and u.name = '"+entry.getOwner()+"' \n" 
//						+ "   and o.id   = c.id \n"
//						+ "   and o.uid  = u.uid \n"
//						+ " order by c.number, c.colid2, c.colid ";
//	
//					try
//					{
//						StringBuilder sb = new StringBuilder();
//	
//						Statement statement = conn.createStatement();
//						ResultSet rs = statement.executeQuery(sql);
//						while(rs.next())
//						{
//							String textPart = rs.getString(1);
//							sb.append(textPart);
//						}
//						rs.close();
//						statement.close();
//	
//						entry.setObjectText( sb.toString() );
//					}
//					catch (SQLException e)
//					{
//						entry.setObjectText( e.toString() );
//					}
//				}
//				else
//				{
//					// Unknown type
//				}
//	
//				//--------------------------------------------
//				// GET sp_depends
//				sql = "exec "+entry.getDbname()+"..sp_depends '"+entry.getOwner()+"."+entry.getObjectName()+"' "; 
//	
//				ss = new AseSqlScript(conn, 10);
//				try	{ 
//					entry.setDependsText( ss.executeSqlStr(sql, true) ); 
//				} catch (SQLException e) { 
//					entry.setDependsText( e.toString() ); 
//				} finally {
//					ss.close();
//				}
//	
//				if (_addDependantObjectsToDdlInQueue)
//				{
//					sql = "exec "+entry.getDbname()+"..sp_depends '"+entry.getOwner()+"."+entry.getObjectName()+"' "; 
//	
//					ArrayList<String> dependList = new ArrayList<String>();
//					try
//					{
//						Statement statement = conn.createStatement();
//						ResultSet rs = statement.executeQuery(sql);
//						ResultSetMetaData rsmd = rs.getMetaData();
//	
//						// lets search for 'object' column, in no case, if it changes...
//						int object_pos = -1;
//						for (int c=1; c<=rsmd.getColumnCount(); c++)
//						{
//							if (rsmd.getColumnLabel(c).toLowerCase().equals("object"))
//							{
//								object_pos = c;
//								break;
//							}
//						}
//						if (object_pos > 0)
//						{
//							while(rs.next())
//							{
//								// Get the dependent object name
//								String depOnObjectName = rs.getString(object_pos);
//	
//								// Strip off the beginning of the string, which holds the owner
//								// example: "dbo.sp_addmessage"
//								int beginIndex = depOnObjectName.indexOf('.') + 1;
//								if (beginIndex < 0)
//									beginIndex = 0;
//								String shortObjName = depOnObjectName.substring(beginIndex);
//
//								dependList.add(shortObjName);
//
//								// Don't add SystemProcedure/systemTables dependencies
//								if ( ! shortObjName.startsWith("sp_") && ! shortObjName.startsWith("sys"))
//								{
//									addDdl(entry.getDbname(), shortObjName, source, objectName, dependLevel + 1);
//								}
//							}
//						}
//						else
//						{
//							_logger.debug("When getting dependent objects using 'sp_depends', I was expecting a column named 'object'. But it wasn't found. The result set had "+rsmd.getColumnCount()+" columns. Skipping lookup for dependent object for '"+entry.getFullObjectName()+"'.");
//						}
//						rs.close();
//						statement.close();
//					}
//					catch (SQLException e)
//					{
//						// If we didn't have any results for the table, then:
//						// java.sql.SQLException: JZ0R2: No result set for this query.
//						if ( ! "JZ0R2".equals(e.getSQLState()) )
//						{
//							_logger.warn("Problems getting 'sp_depends' for table '"+entry.getFullObjectName()+"'. SqlState='"+e.getSQLState()+"', Caught: "+e);
//						}
//					}
//					if (dependList.size() > 0)
//						entry.setDependList(dependList);
//				}
//	
//				int qsize = _ddlStoreQueue.size();
//				if (qsize > _warnDdlStoreQueueSizeThresh)
//				{
//					_logger.warn("The DDL Storage queue has "+qsize+" entries. The persistent writer might not keep in pace.");
//				}
//				_ddlStoreQueue.add(entry);
//				fireQueueSizeChange();
//	
//			} // end: for (DdlDetails entry : objectList)
//
//		} // end: isStatementCache == false
//		
//		return true;
//	}
	
}
