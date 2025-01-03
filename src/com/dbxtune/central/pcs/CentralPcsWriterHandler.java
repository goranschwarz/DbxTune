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

/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.dbxtune.central.pcs;

import java.lang.invoke.MethodHandles;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.central.pcs.DbxTuneSample.CmEntry;
import com.dbxtune.pcs.PersistContainer;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.Memory;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.TimeUtils;


public class CentralPcsWriterHandler 
implements Runnable
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public static final String THREAD_NAME = "CentralPcsWriterHandler";
	
	/*---------------------------------------------------
	** Constants
	**---------------------------------------------------
	*/
//	public static final String STATEMENT_CACHE_NAME = "statement_cache";
//
//	public static final String  PROPKEY_ddl_doDdlLookupAndStore                  = "CentralPcsWriterHandler.ddl.doDdlLookupAndStore";
//	public static final boolean DEFAULT_ddl_doDdlLookupAndStore                  = true;
//                                                                                 
//	public static final String  PROPKEY_ddl_warnDdlInputQueueSizeThresh          = "CentralPcsWriterHandler.ddl.warnDdlInputQueueSizeThresh";
//	public static final int     DEFAULT_ddl_warnDdlInputQueueSizeThresh          = 100;
//                                                                                 
//	public static final String  PROPKEY_ddl_warnDdlStoreQueueSizeThresh          = "CentralPcsWriterHandler.ddl.warnDdlStoreQueueSizeThresh";
//	public static final int     DEFAULT_ddl_warnDdlStoreQueueSizeThresh          = 100;
//                                                                                 
//	public static final String  PROPKEY_ddl_afterDdlLookupSleepTimeInMs          = "CentralPcsWriterHandler.ddl.afterDdlLookupSleepTimeInMs";
//	public static final int     DEFAULT_ddl_afterDdlLookupSleepTimeInMs          = 250;
//                                                                                 
//	public static final String  PROPKEY_ddl_addDependantObjectsToDdlInQueue      = "CentralPcsWriterHandler.ddl.addDependantObjectsToDdlInQueue";
//	public static final boolean DEFAULT_ddl_addDependantObjectsToDdlInQueue      = true;
                                                                                 
	public static final String  PROPKEY_warnQueueSizeThresh                      = "CentralPcsWriterHandler.warnQueueSizeThresh";
	public static final int     DEFAULT_warnQueueSizeThresh                      = 2;



//	public static final String  PROPKEY_sqlCap_doSqlCaptureAndStore              = "CentralPcsWriterHandler.sqlCapture.doSqlCaptureAndStore";
//	public static final boolean DEFAULT_sqlCap_doSqlCaptureAndStore              = false;
//                                                                                 
//	public static final String  PROPKEY_sqlCap_doSqlText                         = "CentralPcsWriterHandler.sqlCapture.doSqlText";
//	public static final boolean DEFAULT_sqlCap_doSqlText                         = true;
//                                                                                 
//	public static final String  PROPKEY_sqlCap_doStatementInfo                   = "CentralPcsWriterHandler.sqlCapture.doStatementInfo";
//	public static final boolean DEFAULT_sqlCap_doStatementInfo                   = true;
//                                                                                 
//	public static final String  PROPKEY_sqlCap_doPlanText                        = "CentralPcsWriterHandler.sqlCapture.doPlanText";
//	public static final boolean DEFAULT_sqlCap_doPlanText                        = false;
//                                                                                 
//	public static final String  PROPKEY_sqlCap_sleepTimeInMs                     = "CentralPcsWriterHandler.sqlCapture.sleepTimeInMs";
//	public static final int     DEFAULT_sqlCap_sleepTimeInMs                     = 1000;
//	                                                                             
//	public static final String  PROPKEY_sqlCap_saveStatement_whereClause         = "CentralPcsWriterHandler.sqlCapture.saveStatement.where.clause";
//	public static final String  DEFAULT_sqlCap_saveStatement_whereClause         = ""; // "" = do not use any where caluse
//
//	public static final String  PROPKEY_sqlCap_saveStatement_gt_execTime         = "CentralPcsWriterHandler.sqlCapture.saveStatement.gt.execTime";
//	public static final int     DEFAULT_sqlCap_saveStatement_gt_execTime         = -1; // -1 = save everything
//
//	public static final String  PROPKEY_sqlCap_saveStatement_gt_logicalReads     = "CentralPcsWriterHandler.sqlCapture.saveStatement.gt.logicalReads";
//	public static final int     DEFAULT_sqlCap_saveStatement_gt_logicalReads     = -1; // -1 = save everything
//
//	public static final String  PROPKEY_sqlCap_saveStatement_gt_physicalReads    = "CentralPcsWriterHandler.sqlCapture.saveStatement.gt.physicalReads";
//	public static final int     DEFAULT_sqlCap_saveStatement_gt_physicalReads    = -1; // -1 = save everything
//
//	public static final String  PROPKEY_sqlCap_sendDdlForLookup                  = "CentralPcsWriterHandler.sqlCapture.sendDdlForLookup";
//	public static final boolean DEFAULT_sqlCap_sendDdlForLookup                  = true;
//                                                                                 
//	public static final String  PROPKEY_sqlCap_sendDdlForLookup_gt_execTime      = "CentralPcsWriterHandler.sqlCapture.sendDdlForLookup.gt.execTime";
//	public static final int     DEFAULT_sqlCap_sendDdlForLookup_gt_execTime      = -1; // -1 = always send
//
//	public static final String  PROPKEY_sqlCap_sendDdlForLookup_gt_logicalReads  = "CentralPcsWriterHandler.sqlCapture.sendDdlForLookup.gt.logicalReads";
//	public static final int     DEFAULT_sqlCap_sendDdlForLookup_gt_logicalReads  = -1; // -1 = always send
//
//	public static final String  PROPKEY_sqlCap_sendDdlForLookup_gt_physicalReads = "CentralPcsWriterHandler.sqlCapture.sendDdlForLookup.gt.physicalReads";
//	public static final int     DEFAULT_sqlCap_sendDdlForLookup_gt_physicalReads = -1; // -1 = always send
//
//	public static final String  PROPKEY_sqlCap_sendSizeThreshold                 = "CentralPcsWriterHandler.sqlCapture.sendSizeThreshold";
//	public static final int     DEFAULT_sqlCap_sendSizeThreshold                 = 1000;
//
//	public static final String  PROPKEY_sqlCap_clearBeforeFirstPoll              = "CentralPcsWriterHandler.sqlCapture.clearBeforeFirstPoll";
//	public static final boolean DEFAULT_sqlCap_clearBeforeFirstPoll              = true;
//
//	public static final String  PROPKEY_sqlCap_warnStoreQueueSizeThresh          = "CentralPcsWriterHandler.sqlCapture.warnStoreQueueSizeThresh";
//	public static final int     DEFAULT_sqlCap_warnStoreQueueSizeThresh          = 5000;
//
//	public static final String  PROPKEY_sqlCap_isNonConfiguredMonitoringAllowed  = "CentralPcsWriterHandler.sqlCapture.isNonConfiguredMonitoringAllowed";
//	public static final boolean DEFAULT_sqlCap_isNonConfiguredMonitoringAllowed  = true;

	public static final String  PROPKEY_WriterClass                              = "CentralPcsWriterHandler.WriterClass";
//	public static final String  DEFAULT_WriterClass                              = null; // no default

	public static final String  PROPKEY_localMetrics_doCapture                   = "CentralPcsWriterHandler.localMetrics.doCapture";
	public static final boolean DEFAULT_localMetrics_doCapture                   = true;

	
	static
	{
//		Configuration.registerDefaultValue(PROPKEY_ddl_doDdlLookupAndStore,                  DEFAULT_ddl_doDdlLookupAndStore);
//		Configuration.registerDefaultValue(PROPKEY_ddl_warnDdlInputQueueSizeThresh,          DEFAULT_ddl_warnDdlInputQueueSizeThresh);
//		Configuration.registerDefaultValue(PROPKEY_ddl_warnDdlStoreQueueSizeThresh,          DEFAULT_ddl_warnDdlStoreQueueSizeThresh);
//		Configuration.registerDefaultValue(PROPKEY_ddl_afterDdlLookupSleepTimeInMs,          DEFAULT_ddl_afterDdlLookupSleepTimeInMs);
//		Configuration.registerDefaultValue(PROPKEY_ddl_addDependantObjectsToDdlInQueue,      DEFAULT_ddl_addDependantObjectsToDdlInQueue);
		Configuration.registerDefaultValue(PROPKEY_warnQueueSizeThresh,                      DEFAULT_warnQueueSizeThresh);

//		Configuration.registerDefaultValue(PROPKEY_sqlCap_doSqlCaptureAndStore,              DEFAULT_sqlCap_doSqlCaptureAndStore);
//		Configuration.registerDefaultValue(PROPKEY_sqlCap_doSqlText,                         DEFAULT_sqlCap_doSqlText);
//		Configuration.registerDefaultValue(PROPKEY_sqlCap_doStatementInfo,                   DEFAULT_sqlCap_doStatementInfo);
//		Configuration.registerDefaultValue(PROPKEY_sqlCap_doPlanText,                        DEFAULT_sqlCap_doPlanText);
//		Configuration.registerDefaultValue(PROPKEY_sqlCap_sleepTimeInMs,                     DEFAULT_sqlCap_sleepTimeInMs);
//		Configuration.registerDefaultValue(PROPKEY_sqlCap_saveStatement_gt_execTime,         DEFAULT_sqlCap_saveStatement_gt_execTime);
//		Configuration.registerDefaultValue(PROPKEY_sqlCap_saveStatement_gt_logicalReads,     DEFAULT_sqlCap_saveStatement_gt_logicalReads);
//		Configuration.registerDefaultValue(PROPKEY_sqlCap_saveStatement_gt_physicalReads,    DEFAULT_sqlCap_saveStatement_gt_physicalReads);
//		Configuration.registerDefaultValue(PROPKEY_sqlCap_sendDdlForLookup,                  DEFAULT_sqlCap_sendDdlForLookup);
//		Configuration.registerDefaultValue(PROPKEY_sqlCap_sendDdlForLookup_gt_execTime,      DEFAULT_sqlCap_sendDdlForLookup_gt_execTime);
//		Configuration.registerDefaultValue(PROPKEY_sqlCap_sendDdlForLookup_gt_logicalReads,  DEFAULT_sqlCap_sendDdlForLookup_gt_logicalReads);
//		Configuration.registerDefaultValue(PROPKEY_sqlCap_sendDdlForLookup_gt_physicalReads, DEFAULT_sqlCap_sendDdlForLookup_gt_physicalReads);
//		Configuration.registerDefaultValue(PROPKEY_sqlCap_sendSizeThreshold,                 DEFAULT_sqlCap_sendSizeThreshold);
//		Configuration.registerDefaultValue(PROPKEY_sqlCap_clearBeforeFirstPoll,              DEFAULT_sqlCap_clearBeforeFirstPoll);
//		Configuration.registerDefaultValue(PROPKEY_sqlCap_warnStoreQueueSizeThresh,          DEFAULT_sqlCap_warnStoreQueueSizeThresh);
//		Configuration.registerDefaultValue(PROPKEY_sqlCap_isNonConfiguredMonitoringAllowed,  DEFAULT_sqlCap_isNonConfiguredMonitoringAllowed);
	}

	public enum NotificationType
	{
		DROP_SERVER
	};
	
	/*---------------------------------------------------
	** class members
	**---------------------------------------------------
	*/
	// implements singleton pattern
	private static CentralPcsWriterHandler _instance = null;

	private boolean  _initialized = false;
	private boolean  _running     = false;

	private Thread   _thread           = null;

	/** Configuration we were initialized with */
	private Configuration _conf;
	
	/** a list of installed Writers */
	private List<ICentralPersistWriter> _writerClasses = new LinkedList<ICentralPersistWriter>();

	/** Time when the current consume() method started */
	private long _currentConsumeStartTime = 0;

	/** */
	private BlockingQueue<DbxTuneSample> _containerQueue = new LinkedBlockingQueue<DbxTuneSample>();
	private int _warnQueueSizeThresh = DEFAULT_warnQueueSizeThresh;

	/** Flag to state that we are waiting for input on the queue. Meaning it's OK to send "interrupt" to the thread */
	private boolean _waitingOnQueueInput = false;

	/** How many milliseconds have we maximum spent in <i>consume</i> */
	private long _maxConsumeTime = 0;


	
	/** The Local Host Monitor consumer "thread" code */ 
	private Thread _localMetricsStorageThread = null;

	private LocalMetricsStorageConsumer _localMetricsStorage = null;
	
	/** Should we capture Local DbxCentral Metrics, (OS Metrics and possibly the PCS database Metrics */
	private boolean  _doLocalMetrcisCapture = DEFAULT_localMetrics_doCapture;
	
	/** Local DbxCentral Metrics Queue, waiting to be stored */
	private BlockingQueue<PersistContainer> _localMetricsQueue = new LinkedBlockingQueue<PersistContainer>();
	

	
	/*---------------------------------------------------
	** Constructors
	**---------------------------------------------------
	*/
	public CentralPcsWriterHandler()
	throws Exception
	{
	}
	
	/**
	 * Get a "public" string of how all writer are configured, no not reveal
	 * passwords or sensitive information.
	 */
	public int getQueueSize()
	{
		return _containerQueue.size();
	}
	
	/**
	 * Get a "public" string of how all writer are configured, no not reveal
	 * passwords or sensitive information.
	 */
	public String getConfigStr()
	{
		String configStr = "";

		// loop all writer classes
		for (ICentralPersistWriter pw : _writerClasses)
		{
			configStr += pw.getName() + "={" + pw.getConfigStr() + "}, ";
		}
		if (configStr.length() > 0)
			configStr = configStr.substring(0, configStr.length()-2);
		
		return configStr;
	}
	
//	public boolean getConfig_addDependantObjectsToDdlInQueue() { return _addDependantObjectsToDdlInQueue; }

	/** Initialize various member of the class */
	public synchronized void init(Configuration props)
	throws Exception
	{
		_conf = props; 
//System.out.println("PersistanceCounterHandler.init(): props="+StringUtil.toCommaStr(props));
//if (props != null)
//{
//	SortedSet<Object> keys = new TreeSet<Object>(props.keySet());
//	for (Object key : keys)
//		System.out.println("----- key="+StringUtil.left("'"+key+"'", 100)+", value='"+props.getProperty(key.toString())+"'");
//
//	System.out.println("===== PROPKEY_ddl_doDdlLookupAndStore:     '"+PROPKEY_ddl_doDdlLookupAndStore+"'    : "+props.getProperty(PROPKEY_ddl_doDdlLookupAndStore));
//	System.out.println("===== PROPKEY_sqlCap_doSqlCaptureAndStore: '"+PROPKEY_sqlCap_doSqlCaptureAndStore+"': "+props.getProperty(PROPKEY_sqlCap_doSqlCaptureAndStore));
//}
		
		_logger.info("Initializing the Persistent Writer Handler functionality.");

		_warnQueueSizeThresh             = _conf.getIntProperty    (PROPKEY_warnQueueSizeThresh,                 _warnQueueSizeThresh);

		_doLocalMetrcisCapture           = _conf.getBooleanProperty(PROPKEY_localMetrics_doCapture,              DEFAULT_localMetrics_doCapture);
		
		// DDL Lookup & Store Props
//		_warnDdlInputQueueSizeThresh     = _conf.getIntProperty    (PROPKEY_ddl_warnDdlInputQueueSizeThresh,     _warnDdlInputQueueSizeThresh);
//		_warnDdlStoreQueueSizeThresh     = _conf.getIntProperty    (PROPKEY_ddl_warnDdlStoreQueueSizeThresh,     _warnDdlStoreQueueSizeThresh);
//
//		_afterDdlLookupSleepTimeInMs     = _conf.getIntProperty    (PROPKEY_ddl_afterDdlLookupSleepTimeInMs,     _afterDdlLookupSleepTimeInMs);
//		_addDependantObjectsToDdlInQueue = _conf.getBooleanProperty(PROPKEY_ddl_addDependantObjectsToDdlInQueue, _addDependantObjectsToDdlInQueue);
//		
//		_doDdlLookupAndStore             = _conf.getBooleanProperty(PROPKEY_ddl_doDdlLookupAndStore,             DEFAULT_ddl_doDdlLookupAndStore);
//		
//		_doSqlCaptureAndStore            = _conf.getBooleanProperty(PROPKEY_sqlCap_doSqlCaptureAndStore,         DEFAULT_sqlCap_doSqlCaptureAndStore);

		// property: alarm.handleAlarmEventClass
		// NOTE: this could be a comma ',' separated list
		String writerClasses = _conf.getProperty(PROPKEY_WriterClass);

		_logger.info("Configuration for CentralPcsWriterHandler");
		_logger.info("                  "+PROPKEY_WriterClass+"                              = "+writerClasses);
		_logger.info("                  "+PROPKEY_warnQueueSizeThresh+"                      = "+_warnQueueSizeThresh);
//		_logger.info("                  "+PROPKEY_ddl_doDdlLookupAndStore+"                  = "+_doDdlLookupAndStore);
//		if (_doDdlLookupAndStore)
//		{
//		_logger.info("                  ObjectLookupInspector ClassName                      = "+( _objectLookupInspector == null ? "null" : _objectLookupInspector.getClass().getName()));
//		_logger.info("                  "+PROPKEY_ddl_addDependantObjectsToDdlInQueue+"      = "+_addDependantObjectsToDdlInQueue);
//		_logger.info("                  "+PROPKEY_ddl_afterDdlLookupSleepTimeInMs+"          = "+_afterDdlLookupSleepTimeInMs);
//		_logger.info("                  "+PROPKEY_ddl_warnDdlInputQueueSizeThresh+"          = "+_warnDdlInputQueueSizeThresh);
//		_logger.info("                  "+PROPKEY_ddl_warnDdlStoreQueueSizeThresh+"          = "+_warnDdlStoreQueueSizeThresh);
//		}
//
//		_logger.info("                  "+PROPKEY_sqlCap_doSqlCaptureAndStore    +"          = "+_doSqlCaptureAndStore);
//		if (_doSqlCaptureAndStore)
//		{
//		_logger.info("                  SqlCaptureBroker ClassName                           = "+( _sqlCaptureBroker == null ? "null" : _sqlCaptureBroker.getClass().getName()));
//		_logger.info("                  "+PROPKEY_sqlCap_doSqlText+"                         = "+_conf.getProperty(PROPKEY_sqlCap_doSqlText));
//		_logger.info("                  "+PROPKEY_sqlCap_doStatementInfo+"                   = "+_conf.getProperty(PROPKEY_sqlCap_doStatementInfo));
//		_logger.info("                  "+PROPKEY_sqlCap_doPlanText+"                        = "+_conf.getProperty(PROPKEY_sqlCap_doPlanText));
//		_logger.info("                  "+PROPKEY_sqlCap_sleepTimeInMs+"                     = "+_conf.getProperty(PROPKEY_sqlCap_sleepTimeInMs));
//		_logger.info("                  "+PROPKEY_sqlCap_saveStatement_whereClause +"        = "+_conf.getProperty(PROPKEY_sqlCap_saveStatement_whereClause));
//		_logger.info("                  "+PROPKEY_sqlCap_saveStatement_gt_execTime+"         = "+_conf.getProperty(PROPKEY_sqlCap_saveStatement_gt_execTime));
//		_logger.info("                  "+PROPKEY_sqlCap_saveStatement_gt_logicalReads+"     = "+_conf.getProperty(PROPKEY_sqlCap_saveStatement_gt_logicalReads));
//		_logger.info("                  "+PROPKEY_sqlCap_saveStatement_gt_physicalReads+"    = "+_conf.getProperty(PROPKEY_sqlCap_saveStatement_gt_physicalReads));
//		_logger.info("                  "+PROPKEY_sqlCap_sendDdlForLookup+"                  = "+_conf.getProperty(PROPKEY_sqlCap_sendDdlForLookup));
//		_logger.info("                  "+PROPKEY_sqlCap_sendDdlForLookup_gt_execTime+"      = "+_conf.getProperty(PROPKEY_sqlCap_sendDdlForLookup_gt_execTime));
//		_logger.info("                  "+PROPKEY_sqlCap_sendDdlForLookup_gt_logicalReads+"  = "+_conf.getProperty(PROPKEY_sqlCap_sendDdlForLookup_gt_logicalReads));
//		_logger.info("                  "+PROPKEY_sqlCap_sendDdlForLookup_gt_physicalReads+" = "+_conf.getProperty(PROPKEY_sqlCap_sendDdlForLookup_gt_physicalReads));
//		_logger.info("                  "+PROPKEY_sqlCap_sendSizeThreshold+"                 = "+_conf.getProperty(PROPKEY_sqlCap_sendSizeThreshold));
//		_logger.info("                  "+PROPKEY_sqlCap_clearBeforeFirstPoll+"              = "+_conf.getProperty(PROPKEY_sqlCap_clearBeforeFirstPoll));
//		_logger.info("                  "+PROPKEY_sqlCap_warnStoreQueueSizeThresh+"          = "+_conf.getProperty(PROPKEY_sqlCap_warnStoreQueueSizeThresh));
//		_logger.info("                  "+PROPKEY_sqlCap_isNonConfiguredMonitoringAllowed+"  = "+_conf.getProperty(PROPKEY_sqlCap_isNonConfiguredMonitoringAllowed));
//		}


//		if (_doDdlLookupAndStore)
//			_logger.info("The most active objects/statements/etc, "+Version.getAppName()+" will do DDL Lookup and Store information about them. To turn this off, set the property 'CentralPcsWriterHandler.doDdlLookupAndStore' to 'false' in configuration for the CentralPcsWriterHandler module.");
//		else
//			_logger.info("No DDL Lookup and Store will be done. The property 'CentralPcsWriterHandler.doDdlLookupAndStore' is set to 'false' in configuration for the CentralPcsWriterHandler module.");

//		// Initialize The Object Inspector
//		if (_objectLookupInspector != null)
//		{
//			_objectLookupInspector.init(_conf);
//		}

//		// Initialize The SQL Capture Broker
//		if (_sqlCaptureBroker != null)
//		{
//			_sqlCaptureBroker.init(_conf);
//		}
		
		// Check writer classes
		if (writerClasses == null)
		{
//			throw new Exception("The property 'CentralPcsWriterHandler.WriterClass' is mandatory for the CentralPcsWriterHandler module. It should contain one or several classes that implemets the ICentralPersistWriter interface. If you have more than one writer, specify them as a comma separated list.");
			_logger.info("No counters will be persisted. The property 'CentralPcsWriterHandler.WriterClass' is not found in configuration for the CentralPcsWriterHandler module. It should contain one or several classes that implemets the ICentralPersistWriter interface. If you have more than one writer, specify them as a comma separated list.");
		}
		else
		{
			String[] writerClassArray =  writerClasses.split(",");
			for (int i=0; i<writerClassArray.length; i++)
			{
				writerClassArray[i] = writerClassArray[i].trim();
				String writerClassName = writerClassArray[i];
				ICentralPersistWriter writerClass;
	
				if (writerClassName.startsWith("com.asetune."))
				{
					String oldName = writerClassName;
					String newName = writerClassName.replace("com.asetune.", "com.dbxtune.");;
					writerClassName = newName;
					_logger.warn("You passed an '" + PROPKEY_WriterClass + "' that starts with 'com.asetune...' [" + oldName + "], instead lets use the prefix 'com.dbxtune...' [" + newName + "].");
				}

				_logger.debug("Instantiating and Initializing WriterClass='"+writerClassName+"'.");
				try
				{
					Class<?> c = Class.forName( writerClassName );
					writerClass = (ICentralPersistWriter) c.newInstance();
					_writerClasses.add( writerClass );
				}
				catch (ClassCastException e)
				{
					throw new ClassCastException("When trying to load writerWriter class '"+writerClassName+"'. The writerWriter do not seem to follow the interface 'com.dbxtune.pcs.IPersistWriter'");
				}
				catch (ClassNotFoundException e)
				{
					throw new ClassNotFoundException("Tried to load writerWriter class '"+writerClassName+"'.", e);
				}
	
				// Now initialize the User Defined AlarmWriter
				writerClass.init(_conf);
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
	public static CentralPcsWriterHandler getInstance()
	{
		return _instance;
	}

	public static boolean hasInstance()
	{
		return (_instance != null);
	}

	public static void setInstance(CentralPcsWriterHandler inst)
	{
		_instance = inst;
	}

	
	//////////////////////////////////////////////
	//// memory
	//////////////////////////////////////////////
	/** Keep track of how many calls we have been made to lowOnMemoryHandler() */
	private int _lowOnMemoryHandlerCalls = 0;
	private int _lowOnMemoryHandlerCallsThreshold = 20;

	public void lowOnMemoryHandler()
	{
		// If lowOnMemoryHandler() has been called to many times, simply call outOfMemory() to do more extensive cleanup.
		_lowOnMemoryHandlerCalls++;
		if (_lowOnMemoryHandlerCalls > _lowOnMemoryHandlerCallsThreshold)
		{
			_logger.warn("Persistant Counter Handler, lowOnMemoryHandler() has now been called "+_lowOnMemoryHandlerCalls+" times, decided to call outOfMemoryHandler() to do more extensive cleanup.");
			outOfMemoryHandler();
			_lowOnMemoryHandlerCalls = 0;
			return;
		}

		boolean fire = false;

		if (fire)
			fireQueueSizeChange();		
	}

	/** Keep track of how many calls we have been made to outOfMemoryHandler() */
	private int _outOfMemoryHandlerCallCount = 0;
	public void outOfMemoryHandler()
	{
		_outOfMemoryHandlerCallCount++;
		_logger.warn("Persistant Counter Handler, outOfMemoryHandler() was called. callCount="+_outOfMemoryHandlerCallCount+".");

		boolean fire = false;

		// Clear the PCS queues if the outOfMemoryHandler() has been called more that X number of times since we last emptied the queue
		if (_outOfMemoryHandlerCallCount > 3)
		{
			_logger.warn("Persistant Counter Handler, outOfMemoryHandler() was called. Emtying the Counter Store/Write queue, which has "+_containerQueue.size()+" entries.");
			_containerQueue.clear();
			_outOfMemoryHandlerCallCount = 0;
			fire = true;
		}

		if (fire)
			fireQueueSizeChange();
	}

	
	//////////////////////////////////////////////
	//// xxx
	//////////////////////////////////////////////
	
	public void add(DbxTuneSample cont)
	{
		if (_writerClasses.size() == 0)
			return;
		if ( ! isRunning() )
		{
			_logger.warn("The Persistent Counter Handler is not running, discarding entry.");
			return;
		}

		int qsize = _containerQueue.size();
		if (qsize > _warnQueueSizeThresh)
		{
			long currentConsumeTimeMs    = System.currentTimeMillis() -_currentConsumeStartTime;
			String currentConsumeTimeStr = "The consumer is currently not active. ";
			if (_currentConsumeStartTime > 0)
				currentConsumeTimeStr = "The current consumer has been active for " + TimeUtils.msToTimeStr(currentConsumeTimeMs) + ". ";

			// Grab some H2 statistics
			String h2WriterStat = "";
			try { h2WriterStat = H2WriterStat.getInstance().refreshCounters().getStatString(); } 
			catch(Exception ex) { _logger.info("Problems getting H2WriterStat. Continuing anyway... Caught: "+ex, ex); }

			_logger.warn("The persistent queue has "+qsize+" entries. The persistent writer might not keep in pace. " + currentConsumeTimeStr + h2WriterStat);

			// call each writes to let them know about this.
			for (ICentralPersistWriter pw : _writerClasses)
			{
				pw.storageQueueSizeWarning(qsize, _warnQueueSizeThresh);
			}
		}

		_containerQueue.add(cont);
		fireQueueSizeChange();
	}

//	/**
//	 * NOT YET IMPLEMENTED, go and lookup the names of DBID, ObjectID. 
//	 * keep a local cache: "DBID -> Name" and "DBID:ObjectId -> Name"
//	 *  
//	 * @param dbid
//	 * @param objectId
//	 */
//	public void addDdl(int dbid, int objectId)
//	{
//		throw new RuntimeException("PersistCounterHandler: addDdl(dbid, objectId), has not yet been implemented.");
//	}
//
//	/**
//	 * Add a object for DDL lookup and storage in all the Writers
//	 * 
//	 * @param dbname
//	 * @param objectName
//	 */
//	public void addDdl(String dbname, String objectName, String source)
//	{
//		addDdl(dbname, objectName, source, null, 0);
//	}
//	
//	/** This was changed from privatte to public when I introduced ObjectLookUpInspectors, but I think it's more in the "private" form<p>
//	 *  It's called internally from any ObjectLookUpInspectors for dependent objects */
//	public void addDdl(String dbname, String objectName, String source, String dependParent, int dependLevel)
//	{
//		if ( ! _doDdlLookupAndStore )
//			return;
//
//		if (_writerClasses.size() == 0)
//			return;
//
//		// Don't do empty ones...
//		if (StringUtil.isNullOrBlank(dbname) || StringUtil.isNullOrBlank(objectName))
//			return;
//
//		// Check the input queue if it already exists
//		for (ObjectLookupQueueEntry entry : _ddlInputQueue)
//		{
//			if (dbname.equals(entry._dbname) && objectName.equals(entry._objectName))
//			{
//				if (_logger.isDebugEnabled())
//					_logger.debug("-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<- Already in the 'DDL Input Queue', addDdl() exiting early. dbname='"+dbname+"', objectName='"+objectName+"'.");
//				return;
//			}
//		}
//		
//		// Create a queue entry, which *might* be added to the lookup queue at the end
//		ObjectLookupQueueEntry entry = new ObjectLookupQueueEntry(dbname, objectName, source, dependParent, dependLevel);
//
//		if (_objectLookupInspector != null)
//		{
//			if ( ! _objectLookupInspector.allowInspection(entry) )
//				return;
//		}
//		dbname     = entry._dbname;
//		objectName = entry._objectName;
//
//		// check if DDL has NOT been saved in any writer class
//		boolean doLookup = false;
//		for (ICentralPersistWriter pw : _writerClasses)
//		{
//			if ( ! pw.isDdlDetailsStored(dbname, objectName) )
//			{
//				doLookup = true;
//				break;
//			}
//		}
//		if ( ! doLookup )
//		{
//			// DEBUG
//			if (_logger.isDebugEnabled())
//				_logger.debug("-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#- The DDL for dbname '"+dbname+"', objectName '"+objectName+"' has already been stored by all the writers.");
//			return;
//		}
//
//		int qsize = _ddlInputQueue.size();
//		if (qsize > _warnDdlInputQueueSizeThresh)
//		{
//			if (_logger.isDebugEnabled())
//				_logger.debug("The DDL request Input queue has "+qsize+" entries. The persistent writer might not keep in pace.");
//		}
//
//		_ddlInputQueue.add(entry);
//		if (_logger.isDebugEnabled())
//			_logger.debug(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> add lookup  dbname '"+dbname+"', objectName '"+objectName+"' Current queue size = "+_ddlInputQueue.size());
//		fireQueueSizeChange();
//	}

	
	private void isInitialized()
	{
		if ( ! _initialized )
		{
			throw new RuntimeException("The Persistent Counter Handler module has NOT yet been initialized.");
		}
	}


	
//	/**
//	 * Use all installed writers to store the DDL information
//	 * 
//	 * @param ddlDetails
//	 * @param prevConsumeTimeMs
//	 */
//	private void saveDdl(DdlDetails ddlDetails, long prevConsumeTimeMs)
//	{
//		// loop all writer classes
//		for (ICentralPersistWriter pw : _writerClasses)
//		{
//			// CALL THE installed Writer
//			// AND catch all runtime errors that might come
//			try 
//			{
//				// SAVE-SAMPLE
//				// In here we can "do it all" 
//				// or use: beginOfSample(), saveDdl(), saveCounters(), endOfSample()
//				pw.saveDdlDetails(ddlDetails);
//			}
//			catch (Throwable t)
//			{
//				_logger.error("The Persistent Writer got runtime error in consumeDdl() in Persistent Writer named '"+pw.getName()+"'. Continuing with next Writer...", t);
//			}
//		}
//	}

	/**
	 * Notify all Writers
	 * 
	 * @param cont
	 * @param prevConsumeTimeMs
	 */
	public void fireNotification(NotificationType type, String str)
	{
		// loop all writer classes
		for (ICentralPersistWriter pw : _writerClasses)
		{
			pw.notification(type, str);
		}
	}
	
	
	/**
	 * Use all installed writers to store the Persist information
	 * 
	 * @param cont
	 * @param prevConsumeTimeMs
	 */
	private void consume(DbxTuneSample cont, long prevConsumeTimeMs)
	{
		// Should we CLONE() the cont, this since it will be passed to several writers
		// each of the writer is changing the sessionStartTime in the container.
		// For the moment: copy/restore the cont.sessionStartTime...
//		Timestamp initialSessionStartTime = cont.getSessionStartTime();
		
		// loop all writer classes
		for (ICentralPersistWriter pw : _writerClasses)
		{
			// if we are about to STOP the service
			if ( ! isRunning() )
			{
				_logger.info("The service is about to stop, discarding a consume(ObjectLookupQueueEntry:Input) queue entry.");
				continue;
			}

			// Set/restore the original sessionStartTime
//			cont.setSessionStartTime(initialSessionStartTime);

			// Start the clock
			long startTime = System.currentTimeMillis();

			// CALL THE installed Writer
			// AND catch all runtime errors that might come
			try 
			{
				String sessionName = cont.getServerName();

				// Session name should NOT be 'unknown', then we had some issue when generating the container header in the collector
				// if we allow this, then we will create a schema named 'unknown', which isn't a good idea...
				if ("unknown".equals(sessionName))
				{
					_logger.warn("The sessioName in the container is 'unknown', which is not a valid session name. Discarding this consume(ObjectLookupQueueEntry:Input) queue entry.");
					continue;
				}
					
				
//				_logger.info("Persisting Counters using '"+pw.getName()+"' for sessionStartTime='"+cont.getSessionStartTime()+"', sessionSampleTime='"+cont.getSessionSampleTime()+"'. Previous persist took "+prevConsumeTimeMs+" ms. inserts="+pw.getInserts()+", updates="+pw.getUpdates()+", deletes="+pw.getDeletes()+", createTables="+pw.getCreateTables()+", alterTables="+pw.getAlterTables()+", dropTables="+pw.getDropTables()+".");

				// BEGIN-OF-SAMPLE If we want to do anything in here
				if ( ! pw.beginOfSample(cont) )
				{
					_logger.warn("Calling beginOfSample() failed for PersistWriter named '"+pw.getName()+"'. Discarding this consume(ObjectLookupQueueEntry:Input) queue entry and continuing with next writer.");
					continue;
				}

				// should we indicate that a new session should be started in the Writer
				boolean startNewSession = false;
				if (   cont.getSessionStartTime()            != null
				    &&   pw.getSessionStartTime(sessionName) != null
				    && cont.getSessionStartTime().getTime()  != pw.getSessionStartTime(sessionName).getTime()
				   )
				{
					startNewSession = true;
				}

				// If a Session has not yet been started (or you need to "restart" one), go and do that.
//				if ( ! pw.isSessionStarted() || cont.getStartNewSample() )
				if ( ! pw.isSessionStarted(sessionName) || startNewSession )
				{
					Long   contSessionStartTime     = cont.getSessionStartTime()            == null ? null : cont.getSessionStartTime().getTime();
					Long     pwSessionStartTime     =   pw.getSessionStartTime(sessionName) == null ? null :   pw.getSessionStartTime(sessionName).getTime() ;
					String contSessionStartTimeStr  = contSessionStartTime == null ? "-null-" : TimeUtils.toString(contSessionStartTime);
					String    pwSessionStartTimeStr =   pwSessionStartTime == null ? "-null-" : TimeUtils.toString(pwSessionStartTime);
					
					System.out.println("############################-STARTING-A-NEW-SESSION-########### srvName='"+cont.getServerName()+"'.");
					System.out.println("## Writer["+pw.getName()+"] -->> startSession(): ");
					System.out.println("##    !pw.isSessionStarted()="+!pw.isSessionStarted(sessionName));
					System.out.println("##    startNewSession       ="+startNewSession);
					System.out.println("##");
					System.out.println("##    cont.getSessionStartTime().getTime() = " + contSessionStartTime + " - '" + contSessionStartTimeStr + "'.");
					System.out.println("##      pw.getSessionStartTime().getTime() = " + pwSessionStartTime   + " - '" + pwSessionStartTimeStr   + "'.");
					System.out.println("###############################################################");

					Timestamp newTs = cont.getSessionStartTime();
					pw.setSessionStartTime(sessionName, newTs);

					pw.startSession(sessionName, cont);
					// note: the above pw.startSession() must call: pw.setSessionStarted(true);
					// otherwise we will run this everytime
				}

				// Set the Session Start Time in the container.
//				Timestamp newTs = pw.getSessionStartTime();
//				cont.setSessionStartTime(newTs);

//				// CREATE-DDL
//				for (CountersModel cm : cont.getCounterObjects())
//				{
//					// only call saveDdl() the first time...
//					if ( ! pw.isDdlCreated(cm) )
//					{
//						if (pw.saveDdl(cm))
//						{
//							pw.markDdlAsCreated(cm);
//						}
//					}
//				}

				// SAVE-SAMPLE
				// In here we can "do it all" 
				// or use: beginOfSample(), saveDdl(), saveCounters(), endOfSample()
				pw.saveSample(cont);

				
				// SAVE-COUNTERS
				for (CmEntry cme : cont._collectors)
				{
					pw.saveCounters(cme);
				}

				
				// END-OF-SAMPLE If we want to do anything in here
				pw.endOfSample(cont, false);

				// Stop clock and print statistics.
				long execTime = System.currentTimeMillis() - startTime;
				_maxConsumeTime = Math.max(_maxConsumeTime, execTime);
				
				firePcsConsumeInfo(pw.getName(), cont.getServerName(), cont.getSessionStartTime(), cont.getSessionSampleTime(), (int)execTime, pw.getStatistics());
//				_logger.info("Persisting Counters using '"+pw.getName()+"' for sessionStartTime='"+cont.getSessionStartTime()+"', sessionSampleTime='"+cont.getSessionSampleTime()+"'. This persist took "+execTime+" ms. inserts="+pw.getInserts()+", updates="+pw.getUpdates()+", deletes="+pw.getDeletes()+", createTables="+pw.getCreateTables()+", alterTables="+pw.getAlterTables()+", dropTables="+pw.getDropTables()+".");

				// Reset the statistics
				pw.resetCounters();
			}
			catch (Exception ex)
			{
				_logger.error("The Persistent Writer '"+pw.getName()+"' caught exception. Continuing with next Writer...", ex);
				pw.endOfSample(cont, true);
			}
			catch (Throwable t)
			{
				_logger.error("The Persistent Writer '"+pw.getName()+"' got runtime error in consume(). Continuing with next Writer...", t);
				pw.endOfSample(cont, true);
			}
		}
	}
	
	/**
	 * Get MAX time in milliseconds we have spent in consume for any writer
	 * @return
	 */
	public long getMaxConsumeTime()
	{
		return _maxConsumeTime;
	}
	
//	/** 
//	 * When we start a new session, lets call this method to get some 
//	 * idea what we are about to sample. 
//	 * @param cont a DbxTuneSample filled with <b>all</b> the available
//	 *             CounterModels we could sample.
//	 */
//	public void startSession(DbxTuneSample cont)
//	{
//		Iterator<ICentralPersistWriter> writerIter = _writerClasses.iterator();
//		while (writerIter.hasNext()) 
//		{
//			ICentralPersistWriter pw = writerIter.next();
//
//			// CALL THE installed Writer
//			// AND catch all runtime errors that might come
//			try 
//			{
//				_logger.info("Starting Counters Storage Session '"+pw.getName()+"' for sessionStartTime='"+cont.getSessionStartTime()+"', server='"+cont.getServerName()+"'.");
//
//				pw.startSession(cont.getServerName(), cont);
//			}
//			catch (Throwable t)
//			{
//				_logger.error("The Persistent Writer got runtime error when calling the method startSession() in Persistent Writer named '"+pw.getName()+"'. Continuing with next Writer...", t);
//			}
//		}
//	}

	
	/**
	 * Read from the Container "in" queue, and use all Writers to save DATA 
	 */
	@Override
	public void run()
	{
		String threadName = _thread.getName();
		_logger.info("Starting a thread for the module '"+threadName+"'.");

		isInitialized();

		_running = true;
		long prevConsumeTimeMs = 0;

		while(isRunning())
		{
			//_logger.info("Thread '"+_thread.getName()+"', SLEEPS...");
			//try { Thread.sleep(5 * 1000); }
			//catch (InterruptedException ignore) {}
			
			if (_logger.isDebugEnabled())
				_logger.debug("Thread '"+threadName+"', waiting on queue...");

			try 
			{
				DbxTuneSample cont = null;
				try
				{
					_waitingOnQueueInput = true;
					cont = _containerQueue.take();
					fireQueueSizeChange();
				}
				finally
				{
					_waitingOnQueueInput = false;
				}

				// Make sure the container isn't empty.
				if (cont == null || (cont != null && cont.isEmpty()) )
					continue;

				// if we are about to STOP the service
				if ( ! isRunning() )
				{
					_logger.info("The service is about to stop, discarding a consume(DbxTuneSample) queue entry.");
					continue;
				}

				// Go and store or consume the in-data/container
				_currentConsumeStartTime = System.currentTimeMillis();
				long startTime = System.currentTimeMillis();
				consume( cont, prevConsumeTimeMs );
				long stopTime = System.currentTimeMillis();
				_currentConsumeStartTime = 0;

				prevConsumeTimeMs = stopTime-startTime;
				_logger.debug("It took "+prevConsumeTimeMs+" ms to persist the above information (using all writers).");
				
// I think this is better to do AFTER they have been saved in saveAlarms() or that the clear() only removes alarms that are OLDER than X minutes
//				// Clear some stuff after each container.
//				if ( AlarmWriterToPcsJdbc.hasInstance() ) 
//					AlarmWriterToPcsJdbc.getInstance().clear();
			} 
			catch (InterruptedException ex) 
			{
				_running = false;
			}
		}

		_logger.info("Emptying the queue for module '"+threadName+"', which had "+_containerQueue.size()+" entries.");
		_containerQueue.clear();
		fireQueueSizeChange();

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
		_thread.setName(THREAD_NAME);
//		_thread.setDaemon(true);
		_thread.start();

		if (_doLocalMetrcisCapture)
		{
//			// Start the SQL Capture Thread
//			_sqlCapture = new SqlCaptureHandler();
//			_sqlCaptureThread = new Thread(_sqlCapture);
//			_sqlCaptureThread.setName("SqlCaptureThread");
//			_sqlCaptureThread.setDaemon(true);
//			_sqlCaptureThread.start();

			// Start the LocalMetrics Storage Thread
			_localMetricsStorage = new LocalMetricsStorageConsumer();
			_localMetricsStorageThread = new Thread(_localMetricsStorage);
			_localMetricsStorageThread.setName("LocalMetricsStorageThread");
			_localMetricsStorageThread.setDaemon(true);
			_localMetricsStorageThread.start();
		}
	}

	/**
	 * Stop this subsystem
	 */
	public void stop(boolean clearQueues, int maxWaitTimeInMs)
	{
		_logger.info("Received 'stop' request in CentralPcsWriterHandler. with clearQueues="+clearQueues+", maxWaitTimeInMs="+maxWaitTimeInMs);

		_running = false;

		if (clearQueues)
		{
			_containerQueue.clear();
//			_ddlInputQueue.clear();
//			_ddlStoreQueue.clear();
			fireQueueSizeChange();
		}

		if (_thread != null)
		{
			// interrupt the queue.take() ... or possibly(a better solution) send a "STOP" queue object on the queue
			if (_waitingOnQueueInput)
			{
				_logger.info("WaitingOnQueueInput: Interrupting the Thread that is waiting for queue input.");
				_thread.interrupt();
			}
			else
			{
				_logger.info("The thread '"+_thread.getName()+"' is currently (not waiting on Queue Input), so it's probably storing last received Queue Message in the datastore. We will NOT send interrupt, but instead wait for the message to be persisted in the datastore.");
			}

			_thread = null;
		}

		// LocalMetrics Storage
		if (_localMetricsStorageThread != null)
		{
			_localMetricsStorageThread.interrupt();
			_localMetricsStorageThread = null;
		}


		// Close the connections to the data store.
		for (ICentralPersistWriter pw : _writerClasses)
		{
			// Do this first, otherwise the wait time wont work, if close(); closes the db connection
			pw.stopServices(maxWaitTimeInMs);

			// Note: that stop service might stop the service before we disconnect from it
			//       so you could expect some error messages, which could be discarded
			pw.close();
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

	/**
	 * Get installed writer classes
	 * @return
	 */
	public List<ICentralPersistWriter> getWriters()
	{
		return _writerClasses;
	}
	

	/*---------------------------------------------------
	** sub classes
	**---------------------------------------------------
	*/


	/*---------------------------------------------------
	** Listener stuff
	**---------------------------------------------------
	*/
	/** interface to be invoked when any of the internal queue sizes is changed */
	public interface PcsQueueChange
	{
		/**
		 * This will be called when any of the queue changes in size
		 * @param pcsQueueSize          Queue size of the in bound container queue
		 * @param ddlLookupQueueSize    Queue size of the DDL Lookups to be done
		 * @param ddlStoreQueueSize     Queue size of the DDL Storage to be done
		 */
		public void pcsStorageQueueChange(int pcsQueueSize);

//		/**
//		 * This will be called once for each of the Persist Writers
//		 * @param persistWriterName   Name of the Writer
//		 * @param sessionStartTime    Time when the SESSION started to collect data
//		 * @param sessionSampleTime   Time for last main period (if sample time is 10 seconds, this will be the "head" time for all subsequent Performance Counters individual times)
//		 * @param persistTimeInMs     Number of milliseconds it took for the Performance Writer to store the data
//		 * @param inserts             Number of insert operations that was done for this save
//		 * @param updates             Number of update operations that was done for this save
//		 * @param deletes             Number of delete operations that was done for this save
//		 * @param createTables        Number of create table operations that was done for this save
//		 * @param alterTables         Number of alter table operations that was done for this save
//		 * @param dropTables          Number of drop table operations that was done for this save
//		 */
//		public void pcsConsumeInfo(String persistWriterName, Timestamp sessionStartTime, Timestamp sessionSampleTime, int persistTimeInMs, int inserts, int updates, int deletes, int createTables, int alterTables, int dropTables, int ddlSaveCount, int ddlSaveCountSum);

		/**
		 * This will be called once for each of the Persist Writers
		 * @param persistWriterName   Name of the Writer
		 * @param sessionStartTime    Time when the SESSION started to collect data
		 * @param sessionSampleTime   Time for last main period (if sample time is 10 seconds, this will be the "head" time for all subsequent Performance Counters individual times)
		 * @param persistTimeInMs     Number of milliseconds it took for the Performance Writer to store the data
		 * @param writerStatistics    The actual statistical counters object
		 */
		public void pcsConsumeInfo(String persistWriterName, Timestamp sessionStartTime, Timestamp sessionSampleTime, int persistTimeInMs, CentralPcsWriterStatistics writerStatistics);
	}

	/** listeners */
	Set<PcsQueueChange> _queueChangeListeners = new HashSet<PcsQueueChange>();

	/** Add any listeners that want to see changes */
	public void addChangeListener(PcsQueueChange l)
	{
		_queueChangeListeners.add(l);
	}

	/** Remove the listener */
	public void removeChangeListener(PcsQueueChange l)
	{
		_queueChangeListeners.remove(l);
	}

	/** Kicked off when new entries are added */
	protected void fireQueueSizeChange()
	{
		int pcsQueueSize         = _containerQueue.size();

		for (PcsQueueChange l : _queueChangeListeners)
			l.pcsStorageQueueChange(pcsQueueSize);
	}

	/** Kicked off when consume is done 
	 * @param serverName 
	 * @param ddlSaveCount */
//	public void firePcsConsumeInfo(String persistWriterName, Timestamp sessionStartTime, Timestamp sessionSampleTime, int persistTimeInMs, int inserts, int updates, int deletes, int createTables, int alterTables, int dropTables, int ddlSaveCount, int ddlSaveCountSum)
//	{
//		_logger.info("Persisting Counters using '"+persistWriterName+"' for sessionStartTime='"+sessionStartTime+"', sessionSampleTime='"+sessionSampleTime+"'. This persist took "+persistTimeInMs+" ms. inserts="+inserts+", updates="+updates+", deletes="+deletes+", createTables="+createTables+", alterTables="+alterTables+", dropTables="+dropTables+", ddlSaveCount="+ddlSaveCount+", ddlSaveCountSum="+ddlSaveCountSum+".");
//
//		for (PcsQueueChange l : _queueChangeListeners)
//			l.pcsConsumeInfo(persistWriterName, sessionStartTime, sessionSampleTime, persistTimeInMs, inserts, updates, deletes, createTables, alterTables, dropTables, ddlSaveCount, ddlSaveCountSum);
//	}
	public void firePcsConsumeInfo(String persistWriterName, String serverName, Timestamp sessionStartTime, Timestamp sessionSampleTime, int persistTimeInMs, CentralPcsWriterStatistics writerStatistics)
	{
		String h2WriterStat = H2WriterStat.getInstance().refreshCounters().getStatString();
		int    pcsQueueSize = _containerQueue.size();

		_maxLenPersistWriterName = Math.max(_maxLenPersistWriterName, persistWriterName.length());
		_maxLenServerName        = Math.max(_maxLenServerName,        serverName       .length());
		
		_logger.info("Persisting Counters using " + StringUtil.left("'"+persistWriterName+"', ", _maxLenPersistWriterName+4)
				+ "for serverName="               + StringUtil.left("'"+serverName       +"', ", _maxLenServerName+4)
				+ "sessionStartTime='"            + StringUtil.left(sessionStartTime +"",23)   + "', "
				+ "sessionSampleTime='"           + StringUtil.left(sessionSampleTime+"",23)   + "'. "
				+ "This persist took ["           + TimeUtils.msToTimeStrShort(persistTimeInMs) + "] " + StringUtil.left(persistTimeInMs + " ms. ", 10)
				+ "qs="                           + StringUtil.left(pcsQueueSize+".", 4) // ###.
				+ "jvmMemoryLeftInMB="            + Memory.getMemoryLeftInMB() + ". " 
				+ h2WriterStat 
				+ writerStatistics.getStatisticsString() );

		for (PcsQueueChange l : _queueChangeListeners)
			l.pcsConsumeInfo(persistWriterName, sessionStartTime, sessionSampleTime, persistTimeInMs, writerStatistics);
	}
	// Below are just used for formating the above string (for readability)
	private int _maxLenPersistWriterName = 0;
	private int _maxLenServerName        = 0;


	
	
	
	







	////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////
	//// SQL Capture Database Connection
	////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////
//	/** Connection to the DDL Storage. */
//	private DbxConnection _sqlCaptureConn                    = null;
////	private int           _sqlCaptureDbmsVersion             = 0;
//	private long          _lastSqlCaptureIsClosedCheck       = 0;
//	private long          _lastSqlCaptureIsClosedRefreshTime = 1200;
//
//	/**
//	 * Get a connection used to sample DDL information from a ASE Server.
//	 * <p>
//	 * If the "local cached" connection is NULL, or NOT Connected, then:
//	 * <ul>
//	 *  <li>Check if the Counter Collector thread is started.</li>
//	 *  <li>Check if the Counter Collector is connected to the Lookup Server.</li>
//	 * </ul>
//	 * If all the above is true, then grab a new connection the the DDL lookup server.
//	 * 
//	 * @return
//	 */
//	private DbxConnection getSqlCaptureConnection()
//	{
//		// First check the "cached connection" if it's valid
//		if (isSqlCaptureConnected(false, true))
//			return getSqlCaptureConnectionXXX();
//
//		// If the Counter Collector isn't running, no need to continue
//		if ( ! CounterController.hasInstance() )
//			return null;
//		ICounterController cc = CounterController.getInstance();
//
//		// If the Counter Collector isn't connected, no need to continue
//		if ( ! cc.isMonConnected() )
//			return null;
//		
//		// Lets grab a new connection then...
//		try
//		{
////			DbxConnection conn = _connProvider.getNewConnection(Version.getAppName() + "-ObjInfoLookup");
////			Connection    conn = AseConnectionFactory.getConnection(null, Version.getAppName() + "-DdlLookup", null);
//
//			if (_sqlCaptureBroker != null)
//			{
//				DbxConnection conn = _sqlCaptureBroker.createConnection();
//				setSqlCaptureConnection(conn);
//			}
//			else
//				_logger.error("There is no SQL Capture Inspector installed, so I can't grab a DB Connection to do the lookup.");
//		}
//		catch (Exception e)
//		{
//			_logger.error("Problems Getting SQL Capture Connection. Caught: " + e);
//			setSqlCaptureConnection(null);
//		}
//		
//		return getSqlCaptureConnectionXXX();
//	}
//
//	/** Set the <code>Connection</code> to use for DDL Lookups. */
//	public void setSqlCaptureConnection(DbxConnection conn)
//	{
//		_sqlCaptureConn = conn;
//		if (conn != null)
//		{
//			if (_sqlCaptureBroker != null)
//				_sqlCaptureBroker.onConnect(conn);
//		}
//	}
//
//	/** Gets the <code>Connection</code> to the monitored server. */
//	public DbxConnection getSqlCaptureConnectionXXX()
//	{
//		return _sqlCaptureConn;
//	}
//
//	/** Gets the <code>Connection</code> to the monitored server. */
//	public void closeSqlCaptureConnection()
//	{
//		if (_sqlCaptureConn == null) 
//			return;
//
//		try
//		{
//			if ( ! _sqlCaptureConn.isClosed() )
//			{
//				_sqlCaptureConn.close();
//				if (_logger.isDebugEnabled())
//				{
//					_logger.debug("SQL Capture Connection closed");
//				}
//			}
//		}
//		catch (SQLException ev)
//		{
//			_logger.error("closeSqlCaptureConnection", ev);
//		}
//		_sqlCaptureConn = null;
//	}
//
//	public boolean isSqlCaptureConnected(boolean forceConnectionCheck, boolean closeConnOnFailure)
//	{
//		if (_sqlCaptureConn == null) 
//			return false;
//
//		// Cache the last call for X ms (default 1200 ms)
//		if ( ! forceConnectionCheck )
//		{
//			long diff = System.currentTimeMillis() - _lastSqlCaptureIsClosedCheck;
//			if ( diff < _lastSqlCaptureIsClosedRefreshTime)
//			{
//				_logger.debug("    <<--- isSqlCaptureConnected(): not time for refresh. diff='" + diff + "', _lastSqlCaptureIsClosedRefreshTime='" + _lastSqlCaptureIsClosedRefreshTime + "'.");
//				return true;
//			}
//		}
//
//		// check the connection itself
//		try
//		{
//			// jConnect issues/executing RPC: sp_mda 0, 7 on isClosed()
//			if (_sqlCaptureConn.isClosed())
//			{
//				if (closeConnOnFailure)
//					closeSqlCaptureConnection();
//				return false;
//			}
//		}
//		catch (SQLException e)
//		{
//			return false;
//		}
//
//		_lastSqlCaptureIsClosedCheck = System.currentTimeMillis();
//		return true;
//	}
//
//	/**
//	 * Read from the Input Queue, and Do lookups of DDL in the ASE database 
//	 */
//	private class SqlCaptureHandler
//	implements Runnable
//	{
//		private void checkForConfigChanges()
//		{
//			// doSqlCaptureAndStore
//			boolean doSqlCaptureAndStore = _props.getBooleanProperty(PROPKEY_sqlCap_doSqlCaptureAndStore, DEFAULT_sqlCap_doSqlCaptureAndStore);
//			if (doSqlCaptureAndStore != _doSqlCaptureAndStore)
//			{
//				_logger.info("SqlCaptureHandler: Discovered a config change in _doSqlCaptureAndStore from '" + _doSqlCaptureAndStore + "', to '" + doSqlCaptureAndStore + "'.");
//				_doSqlCaptureAndStore = doSqlCaptureAndStore;
//			}
//
//			// Sleep time 
//			int sqlCaptureSleepTimeInMs = _props.getIntProperty(PROPKEY_sqlCap_sleepTimeInMs, DEFAULT_sqlCap_sleepTimeInMs);
//			if (sqlCaptureSleepTimeInMs != _sqlCaptureSleepTimeInMs)
//			{
//				_logger.info("SqlCaptureHandler: Discovered a config change in sleep time from '" + _sqlCaptureSleepTimeInMs + "', to '" + sqlCaptureSleepTimeInMs + "'.");
//				_sqlCaptureSleepTimeInMs = sqlCaptureSleepTimeInMs;
//			}
//		}
//
//		@Override
//		public void run()
//		{
//			String threadName = Thread.currentThread().getName();
//			_logger.info("Starting a thread for the module '" + threadName + "'.");
//	
//			isInitialized();
//	
//			_running = true;
//			long timeMs = 0;
//	
//			while(isRunning())
//			{
////System.out.println("SqlCaptureHandler... at TOP: isRunning()=" + isRunning());
//				try 
//				{
//					checkForConfigChanges();
//					
//					// this should not happen, but just in case
//					if ( ! _doSqlCaptureAndStore )
//					{
//						Thread.sleep(1000);
//						continue;
//					}
//					
//					// Should we check if the "main" collector is paused
////					if ( CounterController.getInstance().isPauseSampling() ) // FIXME: isPauseSampling() was only in the GUI not in CounterController
////					{
////						_logger.debug("Counter Controller is paused... skipping: doSqlCapture()");
////						Thread.sleep(1000);
////						continue;
////					}
//
//					// Go and store or consume the in-data/container
//					long startTime = System.currentTimeMillis();
//					doSqlCapture();
//					long stopTime = System.currentTimeMillis();
//	
//					timeMs = stopTime-startTime;
////System.out.println("It took " + timeMs + " ms to Capture SQL Text/Statements.");
//					if (_logger.isDebugEnabled())
//						_logger.debug("It took " + timeMs + " ms to Capture SQL Text/Statements.");
//					
//					// Only check every X minute
//					long howLongAgo = System.currentTimeMillis() - _sqlCapture_checkConn_last;
////System.out.println("SqlCaptureHandler... doSqlCapture()... timeMs=" + timeMs + ", _sqlCapture_checkConn_period=" + _sqlCapture_checkConn_period + ", howLongAgo=" + howLongAgo);
//					if (howLongAgo > _sqlCapture_checkConn_period)
//					{
//						checkSqlCaptureConnection();
//						_sqlCapture_checkConn_last = System.currentTimeMillis();
//					}
//
//					// Sleep before next "poll" time
//					Thread.sleep(_sqlCaptureSleepTimeInMs);
//				} 
//				catch (InterruptedException ex) 
//				{
//					_running = false;
//				}
//				catch (Throwable t) 
//				{
//					_logger.warn("Problems when calling doSqlCapture(), Skipping the problem and continuing... (however the data fetched in above batch might not have been passed on to the writer thread)", t);
//					// Sleep before next "poll" time
//					try { Thread.sleep(_sqlCaptureSleepTimeInMs); }
//					catch(InterruptedException ex) 
//					{
//						_running = false;
//					}
//				}
//			}
//	
//			_logger.info("Emptying the DDL Input queue for module '" + threadName + "', which had " + _sqlCaptureStoreQueue.size() + " entries.");
//			_sqlCaptureStoreQueue.clear();
//			fireQueueSizeChange();
//
//			// Close the Lookup Connection
//			closeSqlCaptureConnection();
//			
//			_logger.info("Thread '" + threadName + "' was stopped.");
//		}
//	}

	/**
	 * Read from the Storage Queue, and send use all Writers to save LocalMetrics
	 */
	private class LocalMetricsStorageConsumer
	implements Runnable
	{
		@Override
		public void run()
		{
			String threadName = Thread.currentThread().getName();
			_logger.info("Starting a thread for the module '" + threadName + "'.");
	
			isInitialized();
	
			_running = true;
			long prevConsumeTimeMs = 0;
	
			while(isRunning())
			{
				//_logger.info("Thread '" + _thread.getName() + "', SLEEPS...");
				//try { Thread.sleep(5 * 1000); }
				//catch (InterruptedException ignore) {}
				
				if (_logger.isDebugEnabled())
					_logger.debug("Thread '" + threadName + "', waiting on queue...");
	
				try 
				{
					PersistContainer pc = _localMetricsQueue.take();
					fireQueueSizeChange();

					// this should not happen, but just in case
					if ( ! _doLocalMetrcisCapture )
						continue;

					// Make sure the container isn't empty.
					if (pc == null)
						continue;

					if (pc.isEmpty())
						continue;
					
					// if we are about to STOP the service
					if ( ! isRunning() )
					{
						_logger.info("The service is about to stop, discarding a consume(LocalMetrics) queue entry.");
						continue;
					}


					// Go and store or consume the in-data/container
					long startTime = System.currentTimeMillis();
					saveLocalMetrics( pc, prevConsumeTimeMs );
					long stopTime = System.currentTimeMillis();

					prevConsumeTimeMs = stopTime-startTime;
					_logger.debug("It took " + prevConsumeTimeMs + " ms to persist the above LocalMetrics information (using all writers).");
					
				} 
				catch (InterruptedException ex) 
				{
					_running = false;
				}
				catch (Throwable t) 
				{
					_logger.warn("Problems when calling saveLocalMetrics(), Skipping the problem and continuing... (however the data wont be stored for this batch)", t);
				}
			}
	
			_logger.info("Emptying the LocalMetrics Input queue for module '" + threadName + "', which had " + _localMetricsQueue.size() + " entries.");
			_localMetricsQueue.clear();
			fireQueueSizeChange();
	
			_logger.info("Thread '" + threadName + "' was stopped.");
		}
	}

//	/**
//	 * Get DDL information from the database and pass it on to the storage thread
//	 * 
//	 * @param qe
//	 * @param prevLookupTimeMs
//	 * @return true if it did a lookup, false the lookup was discarded
//	 */
//	private boolean doSqlCapture()
//	{
//		DbxConnection conn = getSqlCaptureConnection();
//		if (conn == null)
//			return false;
//
//		if (_sqlCaptureBroker == null)
//		{
//			_logger.warn("doObjectInfoLookup(): Sorry can't continue: There are no Object Lookup Inspector installed.");
//			return false;
//		}
//		
////System.out.println("doSqlCapture(): START");
//		int entries = _sqlCaptureBroker.doSqlCapture(conn, this);
////System.out.println("doSqlCapture(): END - entries=" + entries);
//		
//		if (_logger.isDebugEnabled())
//			_logger.debug("SQL Capture Broker returned: " +  entries);
//
//		if (entries > 0)
//		{
////			int qsize = _sqlCaptureStoreQueue.size();
////			if (qsize > _warnSqlCaptureStoreQueueSizeThresh)
////			{
////				_logger.warn("The SQL Capture Storage queue has " + qsize + " entries. The persistent writer might not keep in pace.");
////			}
//			fireQueueSizeChange();
//			
//			return true;
//		}
//		return false;
//	}

	/**
	 * Use all installed writers to store the SQL Capture information
	 * 
	 * @param ddlDetails
	 * @param prevConsumeTimeMs
	 */
	private void saveLocalMetrics(PersistContainer pc, long prevConsumeTimeMs)
	{
//System.out.println("saveLocalMetrics(). _writerClasses="+_writerClasses);
		// loop all writer classes
		for (ICentralPersistWriter pw : _writerClasses)
		{
			// CALL THE installed Writer
			// AND catch all runtime errors that might come
			try 
			{
				// SAVE-SAMPLE
				// In here we can "do it all" 
				// or use: beginOfSample(), saveSqlCaptureDetails(), saveCounters(), endOfSample()
				pw.saveLocalMetricsSample(pc);
			}
			catch (Throwable t)
			{
				_logger.error("The Persistent Writer got runtime error in consumeDdl() in Persistent Writer named '" + pw.getName() + "'. Continuing with next Writer...", t);
			}
		}
	}

	/**
	 * Add information that is about to be stored by the SQL Capture storage thread
	 * @param sqlCaptureDetails
	 */
	public void addLocalMetrics(PersistContainer pc)
	{
		_localMetricsQueue.add(pc);
	}
	
//	private int getLocalMetricsStorageEntries()
//	{
//		int entries = 0;
//		for (PersistContainer pc : _localMetricsQueue)
//			entries += pc.size();
//		return entries;
//	}




	//////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////
	//// ---- TEST CODE ---- TEST CODE ---- TEST CODE ---- TEST CODE ----
	//////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////
//	public static void main(String[] args) 
//	{
//	}
}
