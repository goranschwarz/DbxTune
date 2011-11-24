/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.pcs;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;

import com.asetune.GetCounters;
import com.asetune.Version;
import com.asetune.cm.CountersModel;
import com.asetune.cm.sql.VersionInfo;
import com.asetune.utils.AseConnectionFactory;
import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.AseSqlScript;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;


public class PersistentCounterHandler 
implements Runnable
{
	private static Logger _logger          = Logger.getLogger(PersistentCounterHandler.class);

	
	/*---------------------------------------------------
	** Constants
	**---------------------------------------------------
	*/

	/*---------------------------------------------------
	** class members
	**---------------------------------------------------
	*/
	// implements singleton pattern
	private static PersistentCounterHandler _instance = null;

	private boolean  _initialized = false;
	private boolean  _running     = false;

	private Thread   _thread           = null;
	private Thread   _ddlStorageThread = null;
	private Thread   _ddlLookupThread  = null;
	
	/** The DDL consumer "thread" code */ 
	private Runnable _ddlStorage = null;

	/** The DDL Lookup "thread" code */ 
	private Runnable _ddlLookup  = null;

	/** what entries has already been stored in the back-end, this so we can sort out fast if wee need to store or not */
//	private HashSet<String> _ddlCache = new HashSet<String>();
	
	/** Configuration we were initialized with */
	private Configuration _props;
	
	/** a list of installed Writers */
	private List<IPersistWriter> _writerClasses = new LinkedList<IPersistWriter>();

	/** */
	private BlockingQueue<PersistContainer> _containerQueue = new LinkedBlockingQueue<PersistContainer>();
	private int _warnQueueSizeThresh = 2;

	/** */
	private BlockingQueue<DdlQueueEntry> _ddlInputQueue = new LinkedBlockingQueue<DdlQueueEntry>();
	private int	_warnDdlInputQueueSizeThresh = 100;

	/** */
	private BlockingQueue<DdlDetails> _ddlStoreQueue = new LinkedBlockingQueue<DdlDetails>();
	private int	_warnDdlStoreQueueSizeThresh = 100;
	
	/** Sleep for X ms, after a DDL lookup has been done, this so we don't flood the system with there requests */
	private int _afterDdlLookupSleepTimeInMs = 500;
	
	/** */
	private boolean _addDependantObjectsToDdlInQueue = true;

	/*---------------------------------------------------
	** Constructors
	**---------------------------------------------------
	*/
	public PersistentCounterHandler()
	throws Exception
	{
	}

	public PersistentCounterHandler(Configuration props)
	throws Exception
	{
		init(props);
	}

	/**
	 * Get a "public" string of how all writer are configured, no not reveal
	 * passwords or sensitive information.
	 */
	public String getConfigStr()
	{
		String configStr = "";

		// loop all writer classes
		for (IPersistWriter pw : _writerClasses)
		{
			configStr += pw.getName() + "={" + pw.getConfigStr() + "}, ";
		}
		if (configStr.length() > 0)
			configStr = configStr.substring(0, configStr.length()-2);
		
		return configStr;
	}

	/** Initialize various member of the class */
	public synchronized void init(Configuration props)
	throws Exception
	{
		_props = props; 
		
		_logger.info("Initializing the Persistent Counter Handler functionality.");

		_warnQueueSizeThresh         = _props.getIntProperty("PersistentCounterHandler.warnQueueSizeThresh",         _warnQueueSizeThresh);
		_warnDdlInputQueueSizeThresh = _props.getIntProperty("PersistentCounterHandler.warnDdlInputQueueSizeThresh", _warnDdlInputQueueSizeThresh);
		_warnDdlStoreQueueSizeThresh = _props.getIntProperty("PersistentCounterHandler.warnDdlStoreQueueSizeThresh", _warnDdlStoreQueueSizeThresh);

		_afterDdlLookupSleepTimeInMs     = _props.getIntProperty    ("PersistentCounterHandler.afterDdlLookupSleepTimeInMs",    _afterDdlLookupSleepTimeInMs);
		_addDependantObjectsToDdlInQueue = _props.getBooleanProperty("PersistentCounterHandler.addDependantObjectsToDdlInQueue", _addDependantObjectsToDdlInQueue);
		
		// property: alarm.handleAlarmEventClass
		// NOTE: this could be a comma ',' separated list
		String writerClasses = _props.getProperty("PersistentCounterHandler.WriterClass");
		if (writerClasses == null)
		{
//			throw new Exception("The property 'PersistentCounterHandler.WriterClass' is mandatory for the PersistentCounterHandler module. It should contain one or several classes that implemets the IPersistWriter interface. If you have more than one writer, specify them as a comma separated list.");
			_logger.info("No counters will be persisted. The property 'PersistentCounterHandler.WriterClass' is not found in configuration for the PersistentCounterHandler module. It should contain one or several classes that implemets the IPersistWriter interface. If you have more than one writer, specify them as a comma separated list.");
		}
		else
		{
			String[] writerClassArray =  writerClasses.split(",");
			for (int i=0; i<writerClassArray.length; i++)
			{
				writerClassArray[i] = writerClassArray[i].trim();
				String writerClassName = writerClassArray[i];
				IPersistWriter writerClass;
	
				_logger.debug("Instantiating and Initializing WriterClass='"+writerClassName+"'.");
				try
				{
					Class<?> c = Class.forName( writerClassName );
					writerClass = (IPersistWriter) c.newInstance();
					_writerClasses.add( writerClass );
				}
				catch (ClassCastException e)
				{
					throw new ClassCastException("When trying to load writerWriter class '"+writerClassName+"'. The writerWriter do not seem to follow the interface 'com.asetune.pcs.IPersistWriter'");
				}
				catch (ClassNotFoundException e)
				{
					throw new ClassNotFoundException("Tried to load writerWriter class '"+writerClassName+"'.", e);
				}
	
				// Now initialize the User Defined AlarmWriter
				writerClass.init(_props);
				writerClass.startServices();
			}
			if (_writerClasses.size() == 0)
			{
				_logger.warn("No Persistent Counter Writers has been installed, NO counters will be saved.");
			}
		}

		_initialized = true;
	}

	/*---------------------------------------------------
	** Methods
	**---------------------------------------------------
	*/
	
	//////////////////////////////////////////////
	//// Instance
	//////////////////////////////////////////////
	public static PersistentCounterHandler getInstance()
	{
		return _instance;
	}

	public static boolean hasInstance()
	{
		return (_instance != null);
	}

	public static void setInstance(PersistentCounterHandler inst)
	{
		_instance = inst;
	}

	
	//////////////////////////////////////////////
	//// xxx
	//////////////////////////////////////////////
	
	public void add(PersistContainer cont)
	{
		if (_writerClasses.size() == 0)
			return;

		int qsize = _containerQueue.size();
		if (qsize > _warnQueueSizeThresh)
		{
			_logger.warn("The persistent queue has "+qsize+" entries. The persistent writer might not keep in pace.");
		}

		_containerQueue.add(cont);
	}

	/**
	 * NOT YET IMPLEMENTED, go and lookup the names of DBID, ObjectID. 
	 * keep a local cache: "DBID -> Name" and "DBID:ObjectId -> Name"
	 *  
	 * @param dbid
	 * @param objectId
	 */
	public void addDdl(int dbid, int objectId)
	{
		throw new RuntimeException("PersistCounterHandler: addDdl(dbid, objectId), has not yet been implemented.");
	}

	/**
	 * Add a object for DDL lookup and storage in all the Writers
	 * 
	 * @param dbname
	 * @param objectName
	 */
	public void addDdl(String dbname, String objectName, String source)
	{
		addDdl(dbname, objectName, source, 0);
	}
	private void addDdl(String dbname, String objectName, String source, int dependLevel)
	{
		if (_writerClasses.size() == 0)
			return;

		// Don't do empty ones...
		if (StringUtil.isNullOrBlank(dbname) || StringUtil.isNullOrBlank(objectName))
			return;

		if (objectName.equals("temp worktable"))
			return;
		
//		if (objectName.startsWith("#"))
//			return;
		
		// check if DDL has NOT been saved in any writer class
		boolean doLookup = false;
		for (IPersistWriter pw : _writerClasses)
		{
			if ( ! pw.isDdlDetailsStored(dbname, objectName) )
			{
				doLookup = true;
				break;
			}
		}
		if ( ! doLookup )
		{
			// DEBUG
			_logger.debug("The DDL for dbname '"+dbname+"', objectName '"+objectName+"' has already been stored by all the writers.");
			return;
		}

		int qsize = _ddlInputQueue.size();
		if (qsize > _warnDdlInputQueueSizeThresh)
		{
			if (_logger.isDebugEnabled())
				_logger.debug("The DDL request Input queue has "+qsize+" entries. The persistent writer might not keep in pace.");
		}

		DdlQueueEntry entry = new DdlQueueEntry(dbname, objectName, source, dependLevel);
		_ddlInputQueue.add(entry);
	}

	
	private void isInitialized()
	{
		if ( ! _initialized )
		{
			throw new RuntimeException("The Persistent Counter Handler module has NOT yet been initialized.");
		}
	}

	

	/**
	 * Get DDL information from the database and pass it on to the storage thread
	 * 
	 * @param qe
	 * @param prevLookupTimeMs
	 * @return true if it did a lookup, false the lookup was discarded
	 */
	private boolean ddlLookup(DdlQueueEntry qe, long prevLookupTimeMs)
	{
		Connection conn = getLookupConnection();
		if (conn == null)
			return false;

		String dbname     = qe._dbname;
		String objectName = qe._objectName;
		String source     = qe._source;
		int dependLevel   = qe._dependLevel;

		// FIXME: dbname  can be a Integer
		// FIXME: objName can be a Integer
		// Do the lookup, then check _ddlCache if it has been stored.

		// Statement Cache object
		boolean isStatementCache = false;
		String  ssqlid = null;
		if (objectName.startsWith("*ss"))
		{
			isStatementCache = true;
			dbname           = "statement_cache";
			int sep = objectName.indexOf('_');
			ssqlid = objectName.substring(3, sep);
			//haskey = objectName.substring(sep+1, objectName.length()-3);
		}

		// check AGAIN if DDL has NOT been saved in any writer class
		boolean doLookup = false;
		for (IPersistWriter pw : _writerClasses)
		{
			if ( ! pw.isDdlDetailsStored(dbname, objectName) )
			{
				doLookup = true;
				break;
			}
		}
		if ( ! doLookup )
		{
			_logger.debug("ddlLookup(): The DDL for dbname '"+dbname+"', objectName '"+objectName+"' has already been stored by all the writers.");
			return false;
		}

		if (_logger.isDebugEnabled())
			_logger.debug("Getting DDL information about object '"+dbname+"."+objectName+"', InputQueueSize="+_ddlInputQueue.size()+", StoreQueueSize="+_ddlStoreQueue.size());
System.out.println("Getting DDL information about object '"+dbname+"."+objectName+"', InputQueueSize="+_ddlInputQueue.size()+", StoreQueueSize="+_ddlStoreQueue.size());

		// Statement Cache objects
		if (isStatementCache)
		{
			DdlDetails entry = new DdlDetails(dbname, objectName);
			entry.setCrdate( new Timestamp(System.currentTimeMillis()) );
			entry.setSource( source );
			entry.setDependLevel( dependLevel );
			entry.setOwner("ssql");
			entry.setType("SS");
			String sql = 
				"set switch on 3604 with no_info \n" +
				"dbcc prsqlcache("+ssqlid+", 1) "; // 1 = also prints showplan"
			
			AseSqlScript ss = new AseSqlScript(conn, 10);
			try	{ 
				entry.setObjectText( ss.executeSqlStr(sql) ); 
			} catch (SQLException e) { 
				entry.setObjectText( e.toString() ); 
			} finally {
				ss.close();
			}
			
			_ddlStoreQueue.add(entry);
		}
		else // all other tables
		{
			// Keep a list of objects we need to work with
			// because: if we get more than one proc/table with different owners
			ArrayList<DdlDetails> objectList = new ArrayList<DdlDetails>();
			
			// GET type and creation time
			String sql = 
				"select o.type, u.name, o.crdate \n" +
				"from "+dbname+"..sysobjects o, "+dbname+"..sysusers u \n" +
				"where o.name = '"+objectName+"' \n" +
				"  and o.uid = u.uid ";
			try
			{
				Statement statement = conn.createStatement();
				ResultSet rs = statement.executeQuery(sql);
				while(rs.next())
				{
					DdlDetails entry = new DdlDetails();
					
					entry.setDbname     ( dbname );
					entry.setObjectName ( objectName );
					entry.setSource     ( source );
					entry.setDependLevel( dependLevel );

					entry.setType       ( rs.getString   (1) );
					entry.setOwner      ( rs.getString   (2) );
					entry.setCrdate     ( rs.getTimestamp(3) );
					
					objectList.add(entry);
				}
			}
			catch (SQLException e)
			{
				_logger.error("Problems Getting basic information about DDL for dbname='"+dbname+"', objectName='"+objectName+"', source='"+source+"', dependLevel="+dependLevel+". Skipping DDL Storage of this object. Caught: "+e);
				return false;
			}
	
			// The object was NOT found
			if (objectList.size() == 0)
			{
				_logger.info("DDL Lookup. Can't find any information for dbname='"+dbname+"', objectName='"+objectName+"', source='"+source+"', dependLevel="+dependLevel+". Skipping DDL Storage of this object.");
				return false;
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
	
					ss = new AseSqlScript(conn, 10);
					try	{ 
						entry.setObjectText( ss.executeSqlStr(sql) ); 
					} catch (SQLException e) { 
						entry.setObjectText( e.toString() ); 
					} finally {
						ss.close();
					}
	
					//--------------------------------------------
					// GET sp__optdiag
//					if (_aseVersion >= 15700)
//						sql = "exec "+entry.getDbname()+"..sp_showoptstats '"+entry.getOwner()+"."+entry.getObjectName()+"' ";
//					else
//						sql = "exec "+entry.getDbname()+"..sp__optdiag '"+entry.getOwner()+"."+entry.getObjectName()+"' "; 
//	
//					ss = new AseSqlScript(conn, 10);
//					try	{ 
//						entry.setOptdiagText( ss.executeSqlStr(sql) ); 
//					} catch (SQLException e) { 
//						entry.setOptdiagText( e.toString() ); 
//					} finally {
//						ss.close();
//					}
	
					//--------------------------------------------
					// GET SOME OTHER STATISTICS
					sql = "exec "+entry.getDbname()+"..sp_spaceused '"+entry.getObjectName()+"' ";
	
					ss = new AseSqlScript(conn, 10);
					try	{ 
						entry.setExtraInfoText( ss.executeSqlStr(sql) ); 
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
	
				ss = new AseSqlScript(conn, 10);
				try	{ 
					entry.setDependsText( ss.executeSqlStr(sql) ); 
				} catch (SQLException e) { 
					entry.setDependsText( e.toString() ); 
				} finally {
					ss.close();
				}
	
				if (_addDependantObjectsToDdlInQueue)
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
									addDdl(entry.getDbname(), shortObjName, source, dependLevel + 1);
							}
						}
						else
						{
							_logger.warn("When getting dependent objects using 'sp_depends', I was expecting a column named 'object'. But it wasn't found. The result set had "+rsmd.getColumnCount()+" columns. Skipping lookup for dependent object for '"+entry.getFullObjectName()+"'.");
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
	
				int qsize = _ddlStoreQueue.size();
				if (qsize > _warnDdlStoreQueueSizeThresh)
				{
					_logger.warn("The DDL Storage queue has "+qsize+" entries. The persistent writer might not keep in pace.");
				}
				_ddlStoreQueue.add(entry);
	
			} // end: for (DdlDetails entry : objectList)

		} // end: isStatementCache == false
		
		return true;
	}

	/**
	 * Use all installed writers to store the DDL information
	 * 
	 * @param ddlDetails
	 * @param prevConsumeTimeMs
	 */
	private void saveDdl(DdlDetails ddlDetails, long prevConsumeTimeMs)
	{
		// loop all writer classes
		for (IPersistWriter pw : _writerClasses)
		{
			// CALL THE installed Writer
			// AND catch all runtime errors that might come
			try 
			{
				// SAVE-SAMPLE
				// In here we can "do it all" 
				// or use: beginOfSample(), saveDdl(), saveCounters(), endOfSample()
				pw.saveDdlDetails(ddlDetails);
			}
			catch (Throwable t)
			{
				_logger.error("The Persistent Writer got runtime error in consumeDdl() in Persistent Writer named '"+pw.getName()+"'. Continuing with next Writer...", t);
			}
		}
	}

	/**
	 * Use all installed writers to store the Persist information
	 * 
	 * @param cont
	 * @param prevConsumeTimeMs
	 */
	private void consume(PersistContainer cont, long prevConsumeTimeMs)
	{
		// Should we CLONE() the cont, this since it will be passed to several writers
		// each of the writer is changing the sessionStartTime in the container.
		// For the moment: copy/restore the cont.sessionStartTime...
		Timestamp initialSessionStartTime = cont.getSessionStartTime();
		
		// loop all writer classes
		for (IPersistWriter pw : _writerClasses)
		{
			// Set/restore the original sessionStartTime
			cont.setSessionStartTime(initialSessionStartTime);

			// Start the clock
			long startTime = System.currentTimeMillis();

			// CALL THE installed Writer
			// AND catch all runtime errors that might come
			try 
			{
//				_logger.info("Persisting Counters using '"+pw.getName()+"' for sessionStartTime='"+cont.getSessionStartTime()+"', mainSampleTime='"+cont.getMainSampleTime()+"'. Previous persist took "+prevConsumeTimeMs+" ms. inserts="+pw.getInserts()+", updates="+pw.getUpdates()+", deletes="+pw.getDeletes()+", createTables="+pw.getCreateTables()+", alterTables="+pw.getAlterTables()+", dropTables="+pw.getDropTables()+".");

				// BEGIN-OF-SAMPLE If we want to do anything in here
				pw.beginOfSample(cont);
				pw.resetCounters();

				// If a Session has not yet been started (or you need to "restart" one), go and do that.
				if ( ! pw.isSessionStarted() || cont.getStartNewSample() )
				{
					Timestamp newTs = cont.getMainSampleTime();
					cont.setSessionStartTime(newTs);
					pw.setSessionStartTime(newTs);

					pw.startSession(cont);
				}

				// Set the Session Start Time in the container.
				Timestamp newTs = pw.getSessionStartTime();
				cont.setSessionStartTime(newTs);
				
				// CREATE-DDL
				for (CountersModel cm : cont._counterObjects)
				{
					// only call saveDdl() the first time...
					if ( ! pw.isDdlCreated(cm) )
					{
						if (pw.saveDdl(cm))
						{
							pw.markDdlAsCreated(cm);
						}
					}
				}

				// SAVE-SAMPLE
				// In here we can "do it all" 
				// or use: beginOfSample(), saveDdl(), saveCounters(), endOfSample()
				pw.saveSample(cont);

				
				// SAVE-COUNTERS
				for (CountersModel cm : cont._counterObjects)
				{
					pw.saveCounters(cm);
				}

				
				// END-OF-SAMPLE If we want to do anything in here
				pw.endOfSample(cont, false);

				// Stop clock and print statistics.
				long stopTime = System.currentTimeMillis();
				long execTime = stopTime-startTime;
				_logger.info("Persisting Counters using '"+pw.getName()+"' for sessionStartTime='"+cont.getSessionStartTime()+"', mainSampleTime='"+cont.getMainSampleTime()+"'. This persist took "+execTime+" ms. inserts="+pw.getInserts()+", updates="+pw.getUpdates()+", deletes="+pw.getDeletes()+", createTables="+pw.getCreateTables()+", alterTables="+pw.getAlterTables()+", dropTables="+pw.getDropTables()+".");
			}
			catch (Throwable t)
			{
				_logger.error("The Persistent Writer got runtime error in consume() in Persistent Writer named '"+pw.getName()+"'. Continuing with next Writer...", t);
				pw.endOfSample(cont, true);
			}
		}
	}
	
	/** 
	 * When we start a new session, lets call this method to get some 
	 * idea what we are about to sample. 
	 * @param cont a PersistContainer filled with <b>all</b> the available
	 *             CounterModels we could sample.
	 */
	public void startSession(PersistContainer cont)
	{
		Iterator<IPersistWriter> writerIter = _writerClasses.iterator();
		while (writerIter.hasNext()) 
		{
			IPersistWriter pw = writerIter.next();

			// CALL THE installed Writer
			// AND catch all runtime errors that might come
			try 
			{
				_logger.info("Starting Counters Storage Session '"+pw.getName()+"' for sessionStartTime='"+cont.getSessionStartTime()+"', server='"+cont.getServerName()+"'.");

				pw.startSession(cont);
			}
			catch (Throwable t)
			{
				_logger.error("The Persistent Writer got runtime error when calling the method startSession() in Persistent Writer named '"+pw.getName()+"'. Continuing with next Writer...", t);
			}
		}
	}

	
	/**
	 * Read from the Input Queue, and Do lookups of DDL in the ASE database 
	 */
	private class DdlLookup
	implements Runnable
	{
		public void run()
		{
			String threadName = Thread.currentThread().getName();
			_logger.info("Starting a thread for the module '"+threadName+"'.");
	
			isInitialized();
	
			_running = true;
			long prevLookupTimeMs = 0;
	
			while(_running)
			{
				//_logger.info("Thread '"+_thread.getName()+"', SLEEPS...");
				//try { Thread.sleep(5 * 1000); }
				//catch (InterruptedException ignore) {}
				
				if (_logger.isDebugEnabled())
					_logger.debug("Thread '"+threadName+"', waiting on queue...");
	
				try 
				{
					DdlQueueEntry qe = _ddlInputQueue.take();
	
					// Make sure the container isn't empty.
					if (qe == null)
						continue;
	
					// Go and store or consume the in-data/container
					long startTime = System.currentTimeMillis();
					boolean didLookup = ddlLookup( qe, prevLookupTimeMs );
					long stopTime = System.currentTimeMillis();
	
					prevLookupTimeMs = stopTime-startTime;
					_logger.debug("It took "+prevLookupTimeMs+" ms to lookup the DDL "+qe+".");
					
					// Let others do some work. so we don't monopolize the server.
					if (didLookup && _afterDdlLookupSleepTimeInMs > 0)
						Thread.sleep((int)_afterDdlLookupSleepTimeInMs);
				} 
				catch (InterruptedException ex) 
				{
					_running = false;
				}
			}
	
			_logger.info("Emptying the DDL Input queue for module '"+threadName+"', which had "+_ddlInputQueue.size()+" entries.");
			_ddlInputQueue.clear();

			// Close the Lookup Connection
			closeDdlLookupConnection();
			
			_logger.info("Thread '"+threadName+"' was stopped.");
		}
	}

	/**
	 * Read from the Storage Queue, and send use all Writers to save DDL 
	 */
	private class DdlStorageConsumer
	implements Runnable
	{
		public void run()
		{
			String threadName = Thread.currentThread().getName();
			_logger.info("Starting a thread for the module '"+threadName+"'.");
	
			isInitialized();
	
			_running = true;
			long prevConsumeTimeMs = 0;
	
			while(_running)
			{
				//_logger.info("Thread '"+_thread.getName()+"', SLEEPS...");
				//try { Thread.sleep(5 * 1000); }
				//catch (InterruptedException ignore) {}
				
				if (_logger.isDebugEnabled())
					_logger.debug("Thread '"+threadName+"', waiting on queue...");
	
				try 
				{
					DdlDetails ddlDetails = _ddlStoreQueue.take();
	
					// Make sure the container isn't empty.
					if (ddlDetails == null)
						continue;
	
					// Go and store or consume the in-data/container
					long startTime = System.currentTimeMillis();
					saveDdl( ddlDetails, prevConsumeTimeMs );
					long stopTime = System.currentTimeMillis();
	
					prevConsumeTimeMs = stopTime-startTime;
					_logger.debug("It took "+prevConsumeTimeMs+" ms to persist the above DDL information (using all writers).");
					
				} 
				catch (InterruptedException ex) 
				{
					_running = false;
				}
			}
	
			_logger.info("Emptying the DDL Input queue for module '"+threadName+"', which had "+_ddlInputQueue.size()+" entries.");
			_ddlInputQueue.clear();
	
			_logger.info("Thread '"+threadName+"' was stopped.");
		}
	}

	/**
	 * Read from the Container "in" queue, and use all Writers to save DATA 
	 */
	public void run()
	{
		String threadName = _thread.getName();
		_logger.info("Starting a thread for the module '"+threadName+"'.");

		isInitialized();

		_running = true;
		long prevConsumeTimeMs = 0;

		while(_running)
		{
			//_logger.info("Thread '"+_thread.getName()+"', SLEEPS...");
			//try { Thread.sleep(5 * 1000); }
			//catch (InterruptedException ignore) {}
			
			if (_logger.isDebugEnabled())
				_logger.debug("Thread '"+threadName+"', waiting on queue...");

			try 
			{
				PersistContainer cont = _containerQueue.take();

				// Make sure the container isn't empty.
				if (cont == null)                     continue;
				if (cont._counterObjects == null)	  continue;
				if (cont._counterObjects.size() <= 0) continue;

				// Go and store or consume the in-data/container
				long startTime = System.currentTimeMillis();
				consume( cont, prevConsumeTimeMs );
				long stopTime = System.currentTimeMillis();

				prevConsumeTimeMs = stopTime-startTime;
				_logger.debug("It took "+prevConsumeTimeMs+" ms to persist the above information (using all writers).");
				
			} 
			catch (InterruptedException ex) 
			{
				_running = false;
			}
		}

		_logger.info("Emptying the queue for module '"+threadName+"', which had "+_containerQueue.size()+" entries.");
		_containerQueue.clear();

		_logger.info("Thread '"+threadName+"' was stopped.");
	}

	/**
	 * Are we running or not
	 */
	public boolean isRunning()
	{
		return _running;
	}

	/**
	 * Start this subsystem
	 */
	public void start()
	{
		if (_writerClasses.size() == 0)
		{
			_logger.warn("No Persistent Counter Writers has been installed, The service thread will NOT be started and NO counters will be saved.");
			return;
		}

		isInitialized();

		// Start the Container Persist Thread
		_thread = new Thread(this);
		_thread.setName("PersistentCounterHandler");
		_thread.setDaemon(true);
		_thread.start();

		// Start the DDL Lookup Thread
		_ddlLookup = new DdlLookup();
		_ddlLookupThread = new Thread(_ddlLookup);
		_ddlLookupThread.setName("DdlLookupThread");
		_ddlLookupThread.setDaemon(true);
		_ddlLookupThread.start();

		// Start the DDL Storage Thread
		_ddlStorage = new DdlStorageConsumer();
		_ddlStorageThread = new Thread(_ddlStorage);
		_ddlStorageThread.setName("DdlStorageThread");
		_ddlStorageThread.setDaemon(true);
		_ddlStorageThread.start();
	}

	/**
	 * Stop this subsystem
	 */
	public void stop()
	{
		_running = false;

		if (_thread != null)
		{
			_thread.interrupt();
			_thread = null;
		}

		if (_ddlLookupThread != null)
		{
			_ddlLookupThread.interrupt();
			_ddlLookupThread = null;
		}

		if (_ddlStorageThread != null)
		{
			_ddlStorageThread.interrupt();
			_ddlStorageThread = null;
		}

		// Close the connections to the datastore.
		for (IPersistWriter pw : _writerClasses)
		{
			pw.close();
			pw.stopServices();
		}
	}

	/**
	 * Check if we have any writers installed/attached
	 * @return
	 */
	public boolean hasWriters()
	{
		return (_writerClasses.size() > 0);
	}

	
	////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////
	//// DDL Lookup Database Connection
	////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////
	/** ASE Connection to the monitored server. */
	private Connection _conn                    = null;
	private int        _aseVersion              = 0;
	private long       _lastIsClosedCheck       = 0;
	private long       _lastIsClosedRefreshTime = 1200;
	
	/**
	 * Get a connection used to sample DDL information from a ASE Server.
	 * <p>
	 * If the "local cached" connection is NULL, or NOT Connected, then:
	 * <ul>
	 *  <li>Check if the Counter Collector thread is started.</li>
	 *  <li>Check if the Counter Collector is connected to the Lookup Server.</li>
	 * </ul>
	 * If all the above is true, then grab a new connection the the DDL lookup server.
	 * 
	 * @return
	 */
	private Connection getLookupConnection()
	{
		// First check the "cached connection" if it's valid
		if (isDdlLookupConnected(false, true))
			return getDdlLookupConnection();

		// If the Counter Collector isn't running, no need to continue
		if ( ! GetCounters.hasInstance() )
			return null;
		GetCounters cc = GetCounters.getInstance();

		// If the Counter Collector isn't connected, no need to continue
		if ( ! cc.isMonConnected() )
			return null;
		
		// Lets grab a new connection then...
		try
		{
			Connection conn = AseConnectionFactory.getConnection(null, Version.getAppName()+"-DdlLookup", null);
			setDdlLookupConnection(conn);
		}
		catch (Exception e)
		{
			_logger.error("Problems Getting DDL Lookup Connection. Caught: "+e);
			setDdlLookupConnection(null);
		}
		
		return getDdlLookupConnection();
	}

	/** Set the <code>Connection</code> to use for DDL Lookups. */
	public void setDdlLookupConnection(Connection conn)
	{
		_conn = conn;
		if (_conn == null)
		{
			_aseVersion = 0;
		}
		else
		{
			_aseVersion = AseConnectionUtils.getAseVersionNumber(_conn);

			// Should we install Procs
			try
			{
				if (_aseVersion >= 15700)
				{
					// do not install use: sp_showoptstats instead
				}
				else if (_aseVersion >= 15000)
					AseConnectionUtils.checkCreateStoredProc(_conn, 15000, "sybsystemprocs", "sp__optdiag", VersionInfo.SP__OPTDIAG_CRDATE, VersionInfo.class, "sp__optdiag_v1_15_0.sql", "sa_role");
				else
					AseConnectionUtils.checkCreateStoredProc(_conn, 12503, "sybsystemprocs", "sp__optdiag", VersionInfo.SP__OPTDIAG_CRDATE, VersionInfo.class, "sp__optdiag_v1_9_4.sql", "sa_role");
			}
			catch (Exception e)
			{
				// the checkCreateStoredProc, writes information to error log, so we don't need to do it again.
			}
		}
	}

	/** Gets the <code>Connection</code> to the monitored server. */
	public Connection getDdlLookupConnection()
	{
		return _conn;
	}

	/** Gets the <code>Connection</code> to the monitored server. */
	public void closeDdlLookupConnection()
	{
		if (_conn == null) 
			return;

		try
		{
			if ( ! _conn.isClosed() )
			{
				_conn.close();
				if (_logger.isDebugEnabled())
				{
					_logger.debug("Connection closed");
				}
			}
		}
		catch (SQLException ev)
		{
			_logger.error("closeDdlLookupConnection", ev);
		}
		_conn = null;
	}

	public boolean isDdlLookupConnected(boolean forceConnectionCheck, boolean closeConnOnFailure)
	{
		if (_conn == null) 
			return false;

		// Cache the last call for X ms (default 1200 ms)
		if ( ! forceConnectionCheck )
		{
			long diff = System.currentTimeMillis() - _lastIsClosedCheck;
			if ( diff < _lastIsClosedRefreshTime)
			{
				_logger.debug("    <<--- isDdlLookupConnected(): not time for refresh. diff='"+diff+"', _lastIsClosedRefreshTime='"+_lastIsClosedRefreshTime+"'.");
				return true;
			}
		}

		// check the connection itself
		try
		{
			// jConnect issues RPC: sp_mda 0, 7 on isClosed()
			if (_conn.isClosed())
			{
				if (closeConnOnFailure)
					closeDdlLookupConnection();
				return false;
			}
		}
		catch (SQLException e)
		{
			return false;
		}

		_lastIsClosedCheck = System.currentTimeMillis();
		return true;
	}

	/*---------------------------------------------------
	** sub classes
	**---------------------------------------------------
	*/
	private static class DdlQueueEntry
	{
		public String _dbname;
		public String _objectName;
		public String _source;
		public int    _dependLevel;

		public DdlQueueEntry(String dbname, String objectName, String source, int dependLevel)
		{
			_dbname      = dbname;
			_objectName  = objectName;
			_source      = source;
			_dependLevel = dependLevel;
		}
		
		public String toString()
		{
			StringBuilder sb = new StringBuilder();
			sb.append(_dbname).append(":").append(_objectName);
			return sb.toString(); 
		}
	}

	//////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////
	//// ---- TEST CODE ---- TEST CODE ---- TEST CODE ---- TEST CODE ----
	//////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////
//	public static void main(String[] args) 
//	{
//	}
}
