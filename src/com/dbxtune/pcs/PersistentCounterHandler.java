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
package com.dbxtune.pcs;

import java.lang.invoke.MethodHandles;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.CounterController;
import com.dbxtune.ICounterController;
import com.dbxtune.Version;
import com.dbxtune.central.pcs.H2WriterStat;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.pcs.inspection.IObjectLookupInspector;
import com.dbxtune.pcs.sqlcapture.ISqlCaptureBroker;
import com.dbxtune.pcs.sqlcapture.SqlCaptureDetails;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.Memory;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.TimeUtils;


public class PersistentCounterHandler 
implements Runnable
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	
	/*---------------------------------------------------
	** Constants
	**---------------------------------------------------
	*/
	public static final String STATEMENT_CACHE_NAME = "statement_cache";

	public static final String  PROPKEY_ddl_doDdlLookupAndStore                  = "PersistentCounterHandler.ddl.doDdlLookupAndStore";
	public static final boolean DEFAULT_ddl_doDdlLookupAndStore                  = true;
                                                                                 
	public static final String  PROPKEY_ddl_enabledForDatabaseObjects            = "PersistentCounterHandler.ddl.enabledForDatabaseObjects";
	public static final boolean DEFAULT_ddl_enabledForDatabaseObjects            = true;
                                                                                 
	public static final String  PROPKEY_ddl_enabledForStatementCache             = "PersistentCounterHandler.ddl.enabledForStatementCache";
	public static final boolean DEFAULT_ddl_enabledForStatementCache             = true;
                                                                                 
	public static final String  PROPKEY_ddl_warnDdlInputQueueSizeThresh          = "PersistentCounterHandler.ddl.warnDdlInputQueueSizeThresh";
	public static final int     DEFAULT_ddl_warnDdlInputQueueSizeThresh          = 100;
                                                                                 
	public static final String  PROPKEY_ddl_warnDdlStoreQueueSizeThresh          = "PersistentCounterHandler.ddl.warnDdlStoreQueueSizeThresh";
	public static final int     DEFAULT_ddl_warnDdlStoreQueueSizeThresh          = 100;
                                                                                 
	public static final String  PROPKEY_ddl_afterDdlLookupSleepTimeInMs          = "PersistentCounterHandler.ddl.afterDdlLookupSleepTimeInMs";
	public static final int     DEFAULT_ddl_afterDdlLookupSleepTimeInMs          = 250;
                                                                                 
	public static final String  PROPKEY_ddl_doGetStatistics                      = "PersistentCounterHandler.ddl.doGetStatistics";
	public static final boolean DEFAULT_ddl_doGetStatistics                      = false;

	public static final String  PROPKEY_ddl_doSpDepends                          = "PersistentCounterHandler.ddl.doSpDepends";
	public static final boolean DEFAULT_ddl_doSpDepends                          = false;

	public static final String  PROPKEY_ddl_addDependantObjectsToDdlInQueue      = "PersistentCounterHandler.ddl.addDependantObjectsToDdlInQueue";
	public static final boolean DEFAULT_ddl_addDependantObjectsToDdlInQueue      = true;
                                                                                 
	public static final String  PROPKEY_warnQueueSizeThresh                      = "PersistentCounterHandler.warnQueueSizeThresh";
	public static final int     DEFAULT_warnQueueSizeThresh                      = 2;

	public static final String  PROPKEY_ddl_connCheckPeriod                      = "PersistentCounterHandler.ddl.connCheckPeriod";
	public static final long    DEFAULT_ddl_connCheckPeriod                      = 5 * 60 * 1000; // 5 minutes
//	public static final long    DEFAULT_ddl_connCheckPeriod                      = 10 * 1000; // test.. 10 seconds...
                                                                                 


	public static final String  PROPKEY_sqlCap_doSqlCaptureAndStore              = "PersistentCounterHandler.sqlCapture.doSqlCaptureAndStore";
	public static final boolean DEFAULT_sqlCap_doSqlCaptureAndStore              = false;
                                                                                 
//	public static final String  PROPKEY_sqlCap_xxx                               = "PersistentCounterHandler.sqlCapture.xxx-not-yet-used";
//	public static final boolean DEFAULT_sqlCap_xxx                               = true;
                                                                                 
	public static final String  PROPKEY_sqlCap_doSqlText                         = "PersistentCounterHandler.sqlCapture.doSqlText";
	public static final boolean DEFAULT_sqlCap_doSqlText                         = true;
                                                                                 
	public static final String  PROPKEY_sqlCap_doStatementInfo                   = "PersistentCounterHandler.sqlCapture.doStatementInfo";
	public static final boolean DEFAULT_sqlCap_doStatementInfo                   = true;
                                                                                 
	public static final String  PROPKEY_sqlCap_doPlanText                        = "PersistentCounterHandler.sqlCapture.doPlanText";
	public static final boolean DEFAULT_sqlCap_doPlanText                        = false;
                                                                                 
	public static final String  PROPKEY_sqlCap_sleepTimeInMs                     = "PersistentCounterHandler.sqlCapture.sleepTimeInMs";
	public static final int     DEFAULT_sqlCap_sleepTimeInMs                     = 1000;
	                                                                             
	public static final String  PROPKEY_sqlCap_connCheckPeriod                   = "PersistentCounterHandler.sqlCapture.connCheckPeriod";
	public static final long    DEFAULT_sqlCap_connCheckPeriod                   = 5 * 60 * 1000; // 5 minutes
//	public static final long    DEFAULT_sqlCap_connCheckPeriod                   = 10 * 1000; // test.. 10 seconds...
                                                                                 
	public static final String  PROPKEY_sqlCap_saveStatement_whereClause         = "PersistentCounterHandler.sqlCapture.saveStatement.where.clause";
	public static final String  DEFAULT_sqlCap_saveStatement_whereClause         = ""; // "" = do not use any where caluse

	public static final String  PROPKEY_sqlCap_saveStatement_gt_execTime         = "PersistentCounterHandler.sqlCapture.saveStatement.gt.execTime";
	public static final int     DEFAULT_sqlCap_saveStatement_gt_execTime         = -1; // -1 = save everything

	public static final String  PROPKEY_sqlCap_saveStatement_gt_logicalReads     = "PersistentCounterHandler.sqlCapture.saveStatement.gt.logicalReads";
	public static final int     DEFAULT_sqlCap_saveStatement_gt_logicalReads     = -1; // -1 = save everything

	public static final String  PROPKEY_sqlCap_saveStatement_gt_physicalReads    = "PersistentCounterHandler.sqlCapture.saveStatement.gt.physicalReads";
	public static final int     DEFAULT_sqlCap_saveStatement_gt_physicalReads    = -1; // -1 = save everything

	public static final String  PROPKEY_sqlCap_sendDdlForLookup                  = "PersistentCounterHandler.sqlCapture.sendDdlForLookup";
	public static final boolean DEFAULT_sqlCap_sendDdlForLookup                  = true;
                                                                                 
	public static final String  PROPKEY_sqlCap_sendDdlForLookup_gt_execTime      = "PersistentCounterHandler.sqlCapture.sendDdlForLookup.gt.execTime";
	public static final int     DEFAULT_sqlCap_sendDdlForLookup_gt_execTime      = -1; // -1 = always send

	public static final String  PROPKEY_sqlCap_sendDdlForLookup_gt_logicalReads  = "PersistentCounterHandler.sqlCapture.sendDdlForLookup.gt.logicalReads";
	public static final int     DEFAULT_sqlCap_sendDdlForLookup_gt_logicalReads  = -1; // -1 = always send

	public static final String  PROPKEY_sqlCap_sendDdlForLookup_gt_physicalReads = "PersistentCounterHandler.sqlCapture.sendDdlForLookup.gt.physicalReads";
	public static final int     DEFAULT_sqlCap_sendDdlForLookup_gt_physicalReads = -1; // -1 = always send

	public static final String  PROPKEY_sqlCap_sendSizeThreshold                 = "PersistentCounterHandler.sqlCapture.sendSizeThreshold";
	public static final int     DEFAULT_sqlCap_sendSizeThreshold                 = 1000;

	public static final String  PROPKEY_sqlCap_clearBeforeFirstPoll              = "PersistentCounterHandler.sqlCapture.clearBeforeFirstPoll";
	public static final boolean DEFAULT_sqlCap_clearBeforeFirstPoll              = true;

	public static final String  PROPKEY_sqlCap_warnStoreQueueSizeThresh          = "PersistentCounterHandler.sqlCapture.warnStoreQueueSizeThresh";
	public static final int     DEFAULT_sqlCap_warnStoreQueueSizeThresh          = 5000;

	public static final String  PROPKEY_sqlCap_isNonConfiguredMonitoringAllowed  = "PersistentCounterHandler.sqlCapture.isNonConfiguredMonitoringAllowed";
	public static final boolean DEFAULT_sqlCap_isNonConfiguredMonitoringAllowed  = true;

	public static final String  PROPKEY_WriterClass                              = "PersistentCounterHandler.WriterClass";
//	public static final String  DEFAULT_WriterClass                              = null; // no default

	static
	{
		Configuration.registerDefaultValue(PROPKEY_ddl_doDdlLookupAndStore,                  DEFAULT_ddl_doDdlLookupAndStore);
		Configuration.registerDefaultValue(PROPKEY_ddl_enabledForDatabaseObjects,            DEFAULT_ddl_enabledForDatabaseObjects);
		Configuration.registerDefaultValue(PROPKEY_ddl_enabledForStatementCache,             DEFAULT_ddl_enabledForStatementCache);
		Configuration.registerDefaultValue(PROPKEY_ddl_warnDdlInputQueueSizeThresh,          DEFAULT_ddl_warnDdlInputQueueSizeThresh);
		Configuration.registerDefaultValue(PROPKEY_ddl_warnDdlStoreQueueSizeThresh,          DEFAULT_ddl_warnDdlStoreQueueSizeThresh);
		Configuration.registerDefaultValue(PROPKEY_ddl_afterDdlLookupSleepTimeInMs,          DEFAULT_ddl_afterDdlLookupSleepTimeInMs);
		Configuration.registerDefaultValue(PROPKEY_ddl_doGetStatistics,                      DEFAULT_ddl_doGetStatistics);
		Configuration.registerDefaultValue(PROPKEY_ddl_doSpDepends,                          DEFAULT_ddl_doSpDepends);
		Configuration.registerDefaultValue(PROPKEY_ddl_addDependantObjectsToDdlInQueue,      DEFAULT_ddl_addDependantObjectsToDdlInQueue);
		Configuration.registerDefaultValue(PROPKEY_warnQueueSizeThresh,                      DEFAULT_warnQueueSizeThresh);

		Configuration.registerDefaultValue(PROPKEY_sqlCap_doSqlCaptureAndStore,              DEFAULT_sqlCap_doSqlCaptureAndStore);
//		Configuration.registerDefaultValue(PROPKEY_sqlCap_xxx,                               DEFAULT_sqlCap_xxx);
		Configuration.registerDefaultValue(PROPKEY_sqlCap_doSqlText,                         DEFAULT_sqlCap_doSqlText);
		Configuration.registerDefaultValue(PROPKEY_sqlCap_doStatementInfo,                   DEFAULT_sqlCap_doStatementInfo);
		Configuration.registerDefaultValue(PROPKEY_sqlCap_doPlanText,                        DEFAULT_sqlCap_doPlanText);
		Configuration.registerDefaultValue(PROPKEY_sqlCap_sleepTimeInMs,                     DEFAULT_sqlCap_sleepTimeInMs);
		Configuration.registerDefaultValue(PROPKEY_sqlCap_saveStatement_gt_execTime,         DEFAULT_sqlCap_saveStatement_gt_execTime);
		Configuration.registerDefaultValue(PROPKEY_sqlCap_saveStatement_gt_logicalReads,     DEFAULT_sqlCap_saveStatement_gt_logicalReads);
		Configuration.registerDefaultValue(PROPKEY_sqlCap_saveStatement_gt_physicalReads,    DEFAULT_sqlCap_saveStatement_gt_physicalReads);
		Configuration.registerDefaultValue(PROPKEY_sqlCap_sendDdlForLookup,                  DEFAULT_sqlCap_sendDdlForLookup);
		Configuration.registerDefaultValue(PROPKEY_sqlCap_sendDdlForLookup_gt_execTime,      DEFAULT_sqlCap_sendDdlForLookup_gt_execTime);
		Configuration.registerDefaultValue(PROPKEY_sqlCap_sendDdlForLookup_gt_logicalReads,  DEFAULT_sqlCap_sendDdlForLookup_gt_logicalReads);
		Configuration.registerDefaultValue(PROPKEY_sqlCap_sendDdlForLookup_gt_physicalReads, DEFAULT_sqlCap_sendDdlForLookup_gt_physicalReads);
		Configuration.registerDefaultValue(PROPKEY_sqlCap_sendSizeThreshold,                 DEFAULT_sqlCap_sendSizeThreshold);
		Configuration.registerDefaultValue(PROPKEY_sqlCap_clearBeforeFirstPoll,              DEFAULT_sqlCap_clearBeforeFirstPoll);
		Configuration.registerDefaultValue(PROPKEY_sqlCap_warnStoreQueueSizeThresh,          DEFAULT_sqlCap_warnStoreQueueSizeThresh);
		Configuration.registerDefaultValue(PROPKEY_sqlCap_isNonConfiguredMonitoringAllowed,  DEFAULT_sqlCap_isNonConfiguredMonitoringAllowed);

		// During development we might want to have debug enabled in this class
		//_logger.setLevel(Level.DEBUG);
	}

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
	private Thread   _sqlCaptureThread = null;
	private Thread   _sqlCaptureStorageThread  = null;

	private boolean  _doDdlLookupAndStore                 = DEFAULT_ddl_doDdlLookupAndStore;
	private boolean  _ddlLookup_enabledForDatabaseObjects = DEFAULT_ddl_enabledForDatabaseObjects;
	private boolean  _ddlLookup_enabledForStatementCache  = DEFAULT_ddl_enabledForStatementCache;

	/** The DDL consumer "thread" code */ 
	private Runnable _ddlStorage = null;

	/** The DDL Lookup "thread" code */ 
	private Runnable _ddlLookup  = null;

	/** Delegate Object lookups to DBMS dependent implementations */
	private IObjectLookupInspector _objectLookupInspector = null;

	/** Delegate SQL Capture to DBMS dependent implementations */
	private ISqlCaptureBroker _sqlCaptureBroker = null;

	private long _ddlLookup_checkConn_last   = 0;
	private long _ddlLookup_checkConn_period = DEFAULT_ddl_connCheckPeriod;
	
	/** last message on this was written at what time */
	private long _ddlLookup_infoMessage_last      = 0;
	
	/** print message every 5 minutes if queue is above _ddlLookup_infoMessage_queueSize */
	private long _ddlLookup_infoMessage_period    = 300 * 1000; //  
	
	/** print message every X minutes if queue is above this value */
	private int  _ddlLookup_infoMessage_queueSize = 20; 


	/** Should we capture SQL from monSysStatement and monSysSQLText */
	private boolean  _doSqlCaptureAndStore = DEFAULT_sqlCap_doSqlCaptureAndStore;
	/** The SQL Capture "thread" code */ 
	private Runnable _sqlCapture = null;
	/** The SQL Capture Storage Consumer "thread" code */ 
	private Runnable _sqlCaptureStorage = null;

	private long _sqlCapture_checkConn_last   = 0;
	private long _sqlCapture_checkConn_period = DEFAULT_sqlCap_connCheckPeriod;
	
	/** */
	private BlockingQueue<SqlCaptureDetails> _sqlCaptureStoreQueue = new LinkedBlockingQueue<SqlCaptureDetails>();
	
	private int _sqlCaptureSleepTimeInMs = DEFAULT_sqlCap_sleepTimeInMs;


	/** Configuration we were initialized with */
	private Configuration _props;
	
	/** a list of installed Writers */
	private List<IPersistWriter> _writerClasses = new LinkedList<IPersistWriter>();

	/** Time when the current consume() method started */
	private long _currentConsumeStartTime = 0;

	/** */
	private BlockingQueue<PersistContainer> _containerQueue = new LinkedBlockingQueue<PersistContainer>();
	private int _warnQueueSizeThresh = DEFAULT_warnQueueSizeThresh;

	/** */
	private BlockingQueue<ObjectLookupQueueEntry> _ddlInputQueue = new LinkedBlockingQueue<ObjectLookupQueueEntry>();
	private int	_warnDdlInputQueueSizeThresh = DEFAULT_ddl_warnDdlInputQueueSizeThresh;

	/** */
	private BlockingQueue<DdlDetails> _ddlStoreQueue = new LinkedBlockingQueue<DdlDetails>();
	private int	_warnDdlStoreQueueSizeThresh = DEFAULT_ddl_warnDdlStoreQueueSizeThresh;
	private long _warnDdlStoreQueueSizeThreshPrintEveryXms = 5000;
	private long _warnDdlStoreQueueSizeThreshPrintLastTime = 0;
	
	/** Sleep for X ms, after a DDL lookup has been done, this so we don't flood the system with there requests */
	private int _afterDdlLookupSleepTimeInMs = DEFAULT_ddl_afterDdlLookupSleepTimeInMs;
	
	/** */
	private boolean _doGetStatistics                 = DEFAULT_ddl_doGetStatistics;
	private boolean _doSpDepends                     = DEFAULT_ddl_doSpDepends;
	private boolean _addDependantObjectsToDdlInQueue = DEFAULT_ddl_addDependantObjectsToDdlInQueue;

	/** How many milliseconds have we maximum spent in <i>consume</i> */
	private long _maxConsumeTime      = 0;
	/** Last time the _maxConsumeTime was bumped */
	private long _maxConsumeTimeStamp = System.currentTimeMillis();

	/*---------------------------------------------------
	** Constructors
	**---------------------------------------------------
	*/
	public PersistentCounterHandler(IObjectLookupInspector objectLookupInspector, ISqlCaptureBroker sqlCaptureBroker)
	throws Exception
	{
		_objectLookupInspector = objectLookupInspector;
		_sqlCaptureBroker      = sqlCaptureBroker;
	}

//	public PersistentCounterHandler(Configuration props, IObjectLookupInspector objectLookupInspector, ISqlCaptureBroker sqlCaptureBroker)
//	throws Exception
//	{
//		_objectLookupInspector = objectLookupInspector;
//		_sqlCaptureBroker      = sqlCaptureBroker;
//		init(props);
//	}

	public ISqlCaptureBroker getSqlCaptureBroker()
	{
		return _sqlCaptureBroker;
	}

	public Configuration getConfig()
	{
		return _props;
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
	
	
	public boolean getConfig_doGetStatistics()                 { return _doGetStatistics; }
	public boolean getConfig_doSpDepends()                     { return _doSpDepends; }
	public boolean getConfig_addDependantObjectsToDdlInQueue() { return _addDependantObjectsToDdlInQueue; }
	
	/** Get All Writers */
	public Collection<IPersistWriter> getPersistWriters()
	{
		return _writerClasses; 
	}

	/** 
	 * Get first writer of the instance PersistWriterJdbc
	 * <p>
	 * If none was found: null will be returned.
	 */
	public PersistWriterJdbc getPersistWriterJdbc()
	{
		for (IPersistWriter entry : _writerClasses)
		{
			if (entry instanceof PersistWriterJdbc)
				return (PersistWriterJdbc) entry;
		}
		return null;
	}

	/** Initialize various member of the class */
	public synchronized void init(Configuration props)
	throws Exception
	{
		_props = props; 
//System.out.println("PersistanceCounterHandler.init(): props=" + StringUtil.toCommaStr(props));
//if (props != null)
//{
//	SortedSet<Object> keys = new TreeSet<Object>(props.keySet());
//	for (Object key : keys)
//		System.out.println("----- key=" + StringUtil.left("'" + key + "'", 100) + ", value='" + props.getProperty(key.toString()) + "'");
//
//	System.out.println("===== PROPKEY_ddl_doDdlLookupAndStore:     '" + PROPKEY_ddl_doDdlLookupAndStore + "'    : " + props.getProperty(PROPKEY_ddl_doDdlLookupAndStore));
//	System.out.println("===== PROPKEY_sqlCap_doSqlCaptureAndStore: '" + PROPKEY_sqlCap_doSqlCaptureAndStore + "': " + props.getProperty(PROPKEY_sqlCap_doSqlCaptureAndStore));
//}
		
		_logger.info("Initializing the Persistent Counter Handler functionality.");

		_warnQueueSizeThresh                 = _props.getIntProperty    (PROPKEY_warnQueueSizeThresh,                 DEFAULT_warnQueueSizeThresh);

		// DDL Lookup & Store Props
		_warnDdlInputQueueSizeThresh         = _props.getIntProperty    (PROPKEY_ddl_warnDdlInputQueueSizeThresh,     DEFAULT_ddl_warnDdlInputQueueSizeThresh);
		_warnDdlStoreQueueSizeThresh         = _props.getIntProperty    (PROPKEY_ddl_warnDdlStoreQueueSizeThresh,     DEFAULT_ddl_warnDdlStoreQueueSizeThresh);

		_afterDdlLookupSleepTimeInMs         = _props.getIntProperty    (PROPKEY_ddl_afterDdlLookupSleepTimeInMs,     DEFAULT_ddl_afterDdlLookupSleepTimeInMs);
		_doGetStatistics                     = _props.getBooleanProperty(PROPKEY_ddl_doGetStatistics,                 DEFAULT_ddl_doGetStatistics);
		_doSpDepends                         = _props.getBooleanProperty(PROPKEY_ddl_doSpDepends,                     DEFAULT_ddl_doSpDepends);
		_addDependantObjectsToDdlInQueue     = _props.getBooleanProperty(PROPKEY_ddl_addDependantObjectsToDdlInQueue, DEFAULT_ddl_addDependantObjectsToDdlInQueue);
		
		_doDdlLookupAndStore                 = _props.getBooleanProperty(PROPKEY_ddl_doDdlLookupAndStore,             DEFAULT_ddl_doDdlLookupAndStore);
		_ddlLookup_enabledForDatabaseObjects = _props.getBooleanProperty(PROPKEY_ddl_enabledForDatabaseObjects,       DEFAULT_ddl_enabledForDatabaseObjects);
		_ddlLookup_enabledForStatementCache  = _props.getBooleanProperty(PROPKEY_ddl_enabledForStatementCache,        DEFAULT_ddl_enabledForStatementCache);
		_ddlLookup_checkConn_period          = _props.getLongProperty   (PROPKEY_ddl_connCheckPeriod,                 DEFAULT_ddl_connCheckPeriod);
		_ddlLookup_checkConn_last            = System.currentTimeMillis();
		
		_doSqlCaptureAndStore                = _props.getBooleanProperty(PROPKEY_sqlCap_doSqlCaptureAndStore,         DEFAULT_sqlCap_doSqlCaptureAndStore);
		_sqlCapture_checkConn_period         = _props.getLongProperty   (PROPKEY_sqlCap_connCheckPeriod,              DEFAULT_sqlCap_connCheckPeriod);
		_sqlCapture_checkConn_last           = System.currentTimeMillis();

		// property: alarm.handleAlarmEventClass
		// NOTE: this could be a comma ',' separated list
		String writerClasses = _props.getProperty(PROPKEY_WriterClass);

		_logger.info("Configuration for PersistentCounterHandler");
		_logger.info("                  " + PROPKEY_WriterClass + "                              = " + writerClasses);
		_logger.info("                  " + PROPKEY_warnQueueSizeThresh + "                      = " + _warnQueueSizeThresh);
		
		_logger.info("                  " + PROPKEY_ddl_doDdlLookupAndStore + "                  = " + _doDdlLookupAndStore);
		if ( ! _doDdlLookupAndStore )
		_logger.info("             ---- ObjectLookup is DISABLED");
		else
		{
		_logger.info("             ---- ObjectLookupInspector ClassName                      = " + ( _objectLookupInspector == null ? "null" : _objectLookupInspector.getClass().getName()));
		_logger.info("                  " + PROPKEY_ddl_enabledForDatabaseObjects + "            = " + _ddlLookup_enabledForDatabaseObjects);
		_logger.info("                  " + PROPKEY_ddl_enabledForStatementCache + "             = " + _ddlLookup_enabledForStatementCache);
		_logger.info("                  " + PROPKEY_ddl_doGetStatistics + "                      = " + _doGetStatistics);
		_logger.info("                  " + PROPKEY_ddl_doSpDepends + "                          = " + _doSpDepends);
		_logger.info("                  " + PROPKEY_ddl_addDependantObjectsToDdlInQueue + "      = " + _addDependantObjectsToDdlInQueue);
		_logger.info("                  " + PROPKEY_ddl_afterDdlLookupSleepTimeInMs + "          = " + _afterDdlLookupSleepTimeInMs);
		_logger.info("                  " + PROPKEY_ddl_warnDdlInputQueueSizeThresh + "          = " + _warnDdlInputQueueSizeThresh);
		_logger.info("                  " + PROPKEY_ddl_warnDdlStoreQueueSizeThresh + "          = " + _warnDdlStoreQueueSizeThresh);
		}

		_logger.info("                  " + PROPKEY_sqlCap_doSqlCaptureAndStore     + "          = " + _doSqlCaptureAndStore);
		if ( ! _doSqlCaptureAndStore)
		_logger.info("             ---- SqlCapture is DISABLED");
		else
		{
		_logger.info("             ---- SqlCaptureBroker ClassName                           = " + ( _sqlCaptureBroker == null ? "null" : _sqlCaptureBroker.getClass().getName()));
		_logger.info("                  " + PROPKEY_sqlCap_doSqlText + "                         = " + _props.getProperty(PROPKEY_sqlCap_doSqlText));
		_logger.info("                  " + PROPKEY_sqlCap_doStatementInfo + "                   = " + _props.getProperty(PROPKEY_sqlCap_doStatementInfo));
		_logger.info("                  " + PROPKEY_sqlCap_doPlanText + "                        = " + _props.getProperty(PROPKEY_sqlCap_doPlanText));
		_logger.info("                  " + PROPKEY_sqlCap_sleepTimeInMs + "                     = " + _props.getProperty(PROPKEY_sqlCap_sleepTimeInMs));
		_logger.info("                  " + PROPKEY_sqlCap_saveStatement_whereClause  + "        = " + _props.getProperty(PROPKEY_sqlCap_saveStatement_whereClause));
		_logger.info("                  " + PROPKEY_sqlCap_saveStatement_gt_execTime + "         = " + _props.getProperty(PROPKEY_sqlCap_saveStatement_gt_execTime));
		_logger.info("                  " + PROPKEY_sqlCap_saveStatement_gt_logicalReads + "     = " + _props.getProperty(PROPKEY_sqlCap_saveStatement_gt_logicalReads));
		_logger.info("                  " + PROPKEY_sqlCap_saveStatement_gt_physicalReads + "    = " + _props.getProperty(PROPKEY_sqlCap_saveStatement_gt_physicalReads));
		_logger.info("                  " + PROPKEY_sqlCap_sendDdlForLookup + "                  = " + _props.getProperty(PROPKEY_sqlCap_sendDdlForLookup));
		_logger.info("                  " + PROPKEY_sqlCap_sendDdlForLookup_gt_execTime + "      = " + _props.getProperty(PROPKEY_sqlCap_sendDdlForLookup_gt_execTime));
		_logger.info("                  " + PROPKEY_sqlCap_sendDdlForLookup_gt_logicalReads + "  = " + _props.getProperty(PROPKEY_sqlCap_sendDdlForLookup_gt_logicalReads));
		_logger.info("                  " + PROPKEY_sqlCap_sendDdlForLookup_gt_physicalReads + " = " + _props.getProperty(PROPKEY_sqlCap_sendDdlForLookup_gt_physicalReads));
		_logger.info("                  " + PROPKEY_sqlCap_sendSizeThreshold + "                 = " + _props.getProperty(PROPKEY_sqlCap_sendSizeThreshold));
		_logger.info("                  " + PROPKEY_sqlCap_clearBeforeFirstPoll + "              = " + _props.getProperty(PROPKEY_sqlCap_clearBeforeFirstPoll));
		_logger.info("                  " + PROPKEY_sqlCap_warnStoreQueueSizeThresh + "          = " + _props.getProperty(PROPKEY_sqlCap_warnStoreQueueSizeThresh));
		_logger.info("                  " + PROPKEY_sqlCap_isNonConfiguredMonitoringAllowed + "  = " + _props.getProperty(PROPKEY_sqlCap_isNonConfiguredMonitoringAllowed));
		}


		if (_doDdlLookupAndStore)
			_logger.info("The most active objects/statements/etc, " + Version.getAppName() + " will do DDL Lookup and Store information about them. To turn this off, set the property 'PersistentCounterHandler.doDdlLookupAndStore' to 'false' in configuration for the PersistentCounterHandler module.");
		else
			_logger.info("No DDL Lookup and Store will be done. The property 'PersistentCounterHandler.doDdlLookupAndStore' is set to 'false' in configuration for the PersistentCounterHandler module.");

		// Initialize The Object Inspector
		if (_objectLookupInspector != null)
		{
			_objectLookupInspector.init(_props);
		}

		// Initialize The SQL Capture Broker
		if (_sqlCaptureBroker != null)
		{
			_sqlCaptureBroker.init(_props);
		}
		
		// Check writer classes
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

				if (writerClassName.startsWith("com.asetune."))
				{
					String oldName = writerClassName;
					String newName = writerClassName.replace("com.asetune.", "com.dbxtune.");;
					writerClassName = newName;
					_logger.warn("You passed an '" + PROPKEY_WriterClass + "' that starts with 'com.asetune...' [" + oldName + "], instead lets use the prefix 'com.dbxtune...' [" + newName + "].");
				}
				
				_logger.debug("Instantiating and Initializing WriterClass='" + writerClassName + "'.");
				try
				{
					Class<?> c = Class.forName( writerClassName );
					writerClass = (IPersistWriter) c.newInstance();
					_writerClasses.add( writerClass );
				}
				catch (ClassCastException e)
				{
					throw new ClassCastException("When trying to load writerWriter class '" + writerClassName + "'. The writerWriter do not seem to follow the interface 'com.dbxtune.pcs.IPersistWriter'");
				}
				catch (ClassNotFoundException e)
				{
					throw new ClassNotFoundException("Tried to load writerWriter class '" + writerClassName + "'.", e);
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
			_logger.warn("Persistant Counter Handler, lowOnMemoryHandler() has now been called " + _lowOnMemoryHandlerCalls + " times, decided to call outOfMemoryHandler() to do more extensive cleanup.");
			outOfMemoryHandler();
			_lowOnMemoryHandlerCalls = 0;
			return;
		}

		boolean fire = false;

		// Clear the DDL queues
		if (_ddlInputQueue.size() > 0)
		{
			_logger.warn("Persistant Counter Handler, lowOnMemoryHandler() was called. Emtying the DDL Lookup Input queue, which has " + _ddlInputQueue.size() + " entries.");
			_ddlInputQueue.clear();
			fire = true;
		}
		if (_ddlStoreQueue.size() > 0)
		{
			_logger.warn("Persistant Counter Handler, lowOnMemoryHandler() was called. Emtying the DDL Store/Write queue, which has " + _ddlStoreQueue.size() + " entries.");
			_ddlStoreQueue.clear();
			fire = true;
		}

		// SQL Capture queues
		if (_sqlCaptureStoreQueue.size() > 0)
		{
			_logger.warn("Persistant Counter Handler, lowOnMemoryHandler() was called. Emtying the SQL Capture Store/Write queue, which has " + _sqlCaptureStoreQueue.size() + " entries.");
			_sqlCaptureStoreQueue.clear();
			fire = true;
		}

		// SQL Capture Broker
		if (_sqlCaptureBroker != null)
		{
			_sqlCaptureBroker.lowOnMemoryHandler();
			fire = true;
		}

		if (fire)
			fireQueueSizeChange();		
	}

	/** Keep track of how many calls we have been made to outOfMemoryHandler() */
	private int _outOfMemoryHandlerCallCount = 0;
	public void outOfMemoryHandler()
	{
		_outOfMemoryHandlerCallCount++;
		_logger.warn("Persistant Counter Handler, outOfMemoryHandler() was called. callCount=" + _outOfMemoryHandlerCallCount + ".");

		boolean fire = false;

		// Clear the DDL queues
		if (_ddlInputQueue.size() > 0)
		{
			_logger.warn("Persistant Counter Handler, outOfMemoryHandler() was called. Emtying the DDL Lookup Input queue, which has " + _ddlInputQueue.size() + " entries.");
			_ddlInputQueue.clear();
			fire = true;
		}
		if (_ddlStoreQueue.size() > 0)
		{
			_logger.warn("Persistant Counter Handler, outOfMemoryHandler() was called. Emtying the DDL Store/Write queue, which has " + _ddlStoreQueue.size() + " entries.");
			_ddlStoreQueue.clear();
			fire = true;
		}

		// SQL Capture queues
		if (_sqlCaptureStoreQueue.size() > 0)
		{
			_logger.warn("Persistant Counter Handler, outOfMemoryHandler() was called. Emtying the SQL Capture Store/Write queue, which has " + _sqlCaptureStoreQueue.size() + " entries.");
			_sqlCaptureStoreQueue.clear();
			fire = true;
		}

		// SQL Capture Broker
		if (_sqlCaptureBroker != null)
		{
			_sqlCaptureBroker.outOfMemoryHandler();
			fire = true;
		}

		// Clear the PCS queues if the outOfMemoryHandler() has been called more that X number of times since we last emptied the queue
		if (_outOfMemoryHandlerCallCount > 3)
		{
			_logger.warn("Persistant Counter Handler, outOfMemoryHandler() was called. Emtying the Counter Store/Write queue, which has " + _containerQueue.size() + " entries.");
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
	
	public void add(PersistContainer cont)
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

			String h2WriterStat = H2WriterStat.getInstance().refreshCounters().getStatString();
			
			_logger.warn("The persistent queue has " + qsize + " entries. The persistent writer might not keep in pace. " + currentConsumeTimeStr + h2WriterStat);

			// call each writes to let them know about this.
			for (IPersistWriter pw : _writerClasses)
			{
				pw.storageQueueSizeWarning(qsize, _warnQueueSizeThresh);
			}
		}

		_containerQueue.add(cont);
		fireQueueSizeChange();
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
		addDdl(dbname, objectName, source, null, 0);
	}
	
	/** This was changed from privatte to public when I introduced ObjectLookUpInspectors, but I think it's more in the "private" form<p>
	 *  It's called internally from any ObjectLookUpInspectors for dependent objects */
	public void addDdl(String dbname, String objectName, String source, String dependParent, int dependLevel)
	{
		if (_logger.isDebugEnabled())
			_logger.debug("addDdl(): dbname=" + dbname + ", objectName=" + objectName + ", source=" + source + ", dependParent=" + dependParent + ", dependLevel=" + dependLevel + ". EXTRA_INFO: _doDdlLookupAndStore=" + _doDdlLookupAndStore + ", _writerClasses.size()=" + _writerClasses.size());

		if ( ! _doDdlLookupAndStore )
			return;

		if (_writerClasses.size() == 0)
			return;

		// Don't do empty ones...
		if (StringUtil.isNullOrBlank(dbname) || StringUtil.isNullOrBlank(objectName))
			return;
		
		boolean doPrintInfo = false;
		if ("PcsAddDdlObjectDialog".equals(source))
			doPrintInfo = true;

		// Check the input queue if it already exists
		for (ObjectLookupQueueEntry entry : _ddlInputQueue)
		{
			if (dbname.equals(entry._dbname) && objectName.equals(entry._objectName))
			{
				if (doPrintInfo)
					_logger.info("DEBUG: -<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<- Already in the 'DDL Input Queue', addDdl() exiting early. dbname='" + dbname + "', objectName='" + objectName + "'.");
					
//System.out.println("-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<- Already in the 'DDL Input Queue', addDdl() exiting early. dbname='" + dbname + "', objectName='" + objectName + "'.");
				if (_logger.isDebugEnabled())
					_logger.debug("-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<- Already in the 'DDL Input Queue', addDdl() exiting early. dbname='" + dbname + "', objectName='" + objectName + "'.");
				return;
			}
		}
		
		// Create a queue entry, which *might* be added to the lookup queue at the end
		ObjectLookupQueueEntry entry = new ObjectLookupQueueEntry(dbname, objectName, source, dependParent, dependLevel);

		if (_objectLookupInspector != null)
		{
			boolean allowLookup = _objectLookupInspector.allowInspection(entry);
			if (_logger.isDebugEnabled())
				_logger.debug("addDdl(): _objectLookupInspector.allowInspection(entry) returned: " + allowLookup);
			
			// Inspector said: DISCARD, so we can exit early
			if ( ! allowLookup )
			{
				if (doPrintInfo)
					_logger.info("DEBUG: addDdl(): <<<<<<<<<<----- _objectLookupInspector.allowInspection(entry) returned: " + allowLookup);
//System.out.println("addDdl(): <<<<<<<<<<----- _objectLookupInspector.allowInspection(entry) returned: " + allowLookup);
				return;
			}

			// Configuration: Should we allow: StatementCache Entries
			if ( entry.isStatementCacheEntry() || PersistentCounterHandler.STATEMENT_CACHE_NAME.equals(entry._dbname) )
			{
				allowLookup = _ddlLookup_enabledForStatementCache;
			}
			// Configuration: Should we allow: Other Database Objects
			else
			{
				allowLookup = _ddlLookup_enabledForDatabaseObjects;
			}
			
			if (doPrintInfo)
				_logger.info("DEBUG: addDdl(): allowLookup-2=" + allowLookup + ", entry._dbname=" + entry._dbname + ", _ddlLookup_enabledForStatementCache=" + _ddlLookup_enabledForStatementCache + ", _ddlLookup_enabledForDatabaseObjects=" + _ddlLookup_enabledForDatabaseObjects);

			if (_logger.isDebugEnabled())
				_logger.debug("addDdl(): allowLookup-2=" + allowLookup + ", entry._dbname=" + entry._dbname + ", _ddlLookup_enabledForStatementCache=" + _ddlLookup_enabledForStatementCache + ", _ddlLookup_enabledForDatabaseObjects=" + _ddlLookup_enabledForDatabaseObjects);

//if ( ! allowLookup )
//	System.out.println("addDdl(): <<<<<<<<<----- allowLookup-2=" + allowLookup + ", entry._dbname=" + entry._dbname + ", _ddlLookup_enabledForStatementCache=" + _ddlLookup_enabledForStatementCache + ", _ddlLookup_enabledForDatabaseObjects=" + _ddlLookup_enabledForDatabaseObjects);
			// exit if we do NOT allow lookup
			if ( ! allowLookup )
				return;
		}
		dbname     = entry._dbname;
		objectName = entry._objectName;

		if (doPrintInfo)
			entry.setPrintInfo(true);

		// check if DDL has NOT been saved in any writer class
		boolean doLookup = false;
		for (IPersistWriter pw : _writerClasses)
		{
			// Use special database name for -- PlanCache (SQL-Server) or StataementCache (ASE)
			if (entry.isStatementCacheEntry())
			{
				if ( ! pw.isDdlDetailsStored(PersistentCounterHandler.STATEMENT_CACHE_NAME, objectName) )
				{
					doLookup = true;
					break;
				}
			}
			else
			{
				if ( ! pw.isDdlDetailsStored(dbname, objectName) )
				{
					doLookup = true;
					break;
				}
			}
		}

		if ( ! doLookup )
		{
//System.out.println("-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#- The DDL for dbname '" + dbname + "', objectName '" + objectName + "', isStatementCacheEntry=" + entry.isStatementCacheEntry() + ". has already been stored by all the writers.");
			if (doPrintInfo)
				_logger.info("DEBUG: -#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#- The DDL for dbname '" + dbname + "', objectName '" + objectName + "', isStatementCacheEntry=" + entry.isStatementCacheEntry() + ". has already been stored by all the writers.");

			// DEBUG
			if (_logger.isDebugEnabled())
				_logger.debug("-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#-#- The DDL for dbname '" + dbname + "', objectName '" + objectName + "', isStatementCacheEntry=" + entry.isStatementCacheEntry() + ". has already been stored by all the writers.");
			return;
		}

		int qsize = _ddlInputQueue.size();
		if (qsize > _warnDdlInputQueueSizeThresh)
		{
			if (_logger.isDebugEnabled())
				_logger.debug("The DDL request Input queue has " + qsize + " entries. The persistent writer might not keep in pace.");
		}

		//-------------------------------------------
		// ADD it to the queue
		//-------------------------------------------
		_ddlInputQueue.add(entry);
		
		if (doPrintInfo)
			_logger.info("DEBUG: >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> add to LOOKUP_input_QUEUE  dbname '" + dbname + "', objectName '" + objectName + "', isStatementCacheEntry=" + entry.isStatementCacheEntry() + ". Current (_ddlInputQueue) queue size = " + _ddlInputQueue.size());

//System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> add to LOOKUP_input_QUEUE  dbname '" + dbname + "', objectName '" + objectName + "', isStatementCacheEntry=" + entry.isStatementCacheEntry() + ". Current (_ddlInputQueue) queue size = " + _ddlInputQueue.size());
		
		if (_logger.isDebugEnabled())
			_logger.debug(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> add to LOOKUP_input_QUEUE  dbname '" + dbname + "', objectName '" + objectName + "', isStatementCacheEntry=" + entry.isStatementCacheEntry() + ". Current (_ddlInputQueue) queue size = " + _ddlInputQueue.size());
		fireQueueSizeChange();
	}

	
	private void isInitialized()
	{
		if ( ! _initialized )
		{
			throw new RuntimeException("The Persistent Counter Handler module has NOT yet been initialized.");
		}
	}


	
	
	/**
	 * Check that the DDL Lookup DBMS Connection is OK (not in a transaction or similar)<br>
	 * If it's NOT ok, then simply close the connection and let the DBMS cleanup/close the transaction<br>
	 * On next lookup, a new connection will be attempted...
	 */
	private void checkObjectInfoLookupConnection()
	{
//System.out.println(">>>>>>>>>>>>>>>>>>>>> checkObjectInfoLookupConnection(): Just want to know how OFTEN this is called... that is if we want to have a Timeout or similar on it (if it's called to often)");
		DbxConnection conn = getLookupConnection();
		if (conn == null)
			return;

		// Sorry can't continue: There are no Object Lookup Inspector installed
		if (_objectLookupInspector == null)
			return;

		// Lets grab a new connection then...
		try
		{
			boolean ok = _objectLookupInspector.checkConnection(conn);
			if ( ! ok )
			{
//can we test this in any way...
				_logger.warn("When checking the DDL Lookup Connection, it returned NOT_OK. The Connection will be closed. The DBMS is responsible for clearing/cleaning up the transaction.");

				// Close the connection and let the DBMS handle any eventual rollbacks and cleanups
				conn.closeNoThrow();
				setDdlLookupConnection(null);
			}
		}
		catch (Exception ex)
		{
			_logger.error("Problems Checking DDL Lookup Connection. The DDL Lookup Connection will be CLOSED. Caught: " + ex, ex);

			// Close the connection and let the DBMS handle any eventual rollbacks and cleanups
			conn.closeNoThrow();
			setDdlLookupConnection(null);
		}
	}
	
	/**
	 * Check that the SQL Capture DBMS Connection is OK (not in a transaction or similar)<br>
	 * If it's NOT ok, then simply close the connection and let the DBMS cleanup/close the transaction<br>
	 * On next lookup, a new connection will be attempted...
	 */
	private void checkSqlCaptureConnection()
	{
//System.out.println(">>>>>>>>>>>>>>>>>>>>> checkSqlCaptureConnection(): Just want to know how OFTEN this is called... that is if we want to have a Timeout or similar on it (if it's called to often)");
		DbxConnection conn = getSqlCaptureConnection();
		if (conn == null)
			return;

		// Sorry can't continue: There are no SQL Capture Broker installed
		if (_sqlCaptureBroker == null)
			return;

		// Lets grab a new connection then...
		try
		{
			boolean ok = _sqlCaptureBroker.checkConnection(conn);
			if ( ! ok )
			{
//can we test this in any way...
				_logger.warn("When checking the SQL Capture Connection, it returned NOT_OK. The Connection will be closed. The DBMS is responsible for clearing/cleaning up the transaction.");

				// Close the connection and let the DBMS handle any eventual rollbacks and cleanups
				conn.closeNoThrow();
				setSqlCaptureConnection(null);
			}
		}
		catch (Exception ex)
		{
			_logger.error("Problems Checking SQL Capture Connection. The SQL Capture Connection will be CLOSED. Caught: " + ex, ex);

			// Close the connection and let the DBMS handle any eventual rollbacks and cleanups
			conn.closeNoThrow();
			setSqlCaptureConnection(null);
		}
	}
	
	/**
	 * Get DDL information from the database and pass it on to the storage thread
	 * 
	 * @param qe
	 * @param prevLookupTimeMs
	 * @return true if it did a lookup, false the lookup was discarded
	 */
	private boolean doObjectInfoLookup(ObjectLookupQueueEntry qe, long prevLookupTimeMs)
	{
		DbxConnection conn = getLookupConnection();
		if (conn == null)
			return false;

		if (_objectLookupInspector == null)
		{
			_logger.warn("doObjectInfoLookup(): Sorry can't continue: There are no Object Lookup Inspector installed.");
			return false;
		}

		if (qe == null)
		{
			_logger.warn("doObjectInfoLookup(): The passed queue entry was null, returning false.");
			return false;
		}

		String dbname       = qe._dbname;
		String objectName   = qe._objectName;
//		String source       = qe._source;
//		String dependParent = qe._dependParent;
//		int    dependLevel  = qe._dependLevel;

		// check AGAIN if DDL has NOT been saved in any writer class
		boolean doLookup = false;
		// Check if NOT STORED by any writer -->>> doLookup <<== TRUE
		for (IPersistWriter pw : _writerClasses)
		{
			if ( ! pw.isDdlDetailsStored(dbname, objectName) )
			{
				doLookup = true;
				break;
			}
		}
		// Check for DISCARDED by any writer -->>> doLookup <<== FALSE
		for (IPersistWriter pw : _writerClasses)
		{
			if ( pw.isDdlDetailsDiscarded(dbname, objectName) )
			{
				doLookup = false;
				break;
			}
		}
		if ( ! doLookup )
		{
			if (qe.isPrintInfo())
				_logger.info("DEBUG: doObjectInfoLookup(): <<<<<<< ---return---- The DDL for dbname '" + dbname + "', objectName '" + objectName + "' has already been stored/discarded by all the writers.");

//System.out.println("doObjectInfoLookup(): <<<<<<< ---return---- The DDL for dbname '" + dbname + "', objectName '" + objectName + "' has already been stored/discarded by all the writers.");
			if (_logger.isDebugEnabled())
				_logger.debug("doObjectInfoLookup(): The DDL for dbname '" + dbname + "', objectName '" + objectName + "' has already been stored/discarded by all the writers.");
			return false;
		}

		if (qe.isPrintInfo())
			_logger.info("DEBUG: Getting DDL information about object '" + dbname + "." + objectName + "', InputQueueSize=" + _ddlInputQueue.size() + ", StoreQueueSize=" + _ddlStoreQueue.size());

		if (_logger.isDebugEnabled())
			_logger.debug("Getting DDL information about object '" + dbname + "." + objectName + "', InputQueueSize=" + _ddlInputQueue.size() + ", StoreQueueSize=" + _ddlStoreQueue.size());

		// Print INFO message if IN-QUEUE is above X and a certain time has passed
		if (_ddlInputQueue.size() > _ddlLookup_infoMessage_queueSize)
		{
			long howLongAgo = System.currentTimeMillis() - _ddlLookup_infoMessage_last;
			if (howLongAgo > _ddlLookup_infoMessage_period)
			{
				_logger.info("DDL Lookup: InputQueueSize=" + _ddlInputQueue.size() + ", StoreQueueSize=" + _ddlStoreQueue.size() + ". Now getting DDL information about object '" + dbname + "." + objectName + "',");
				_ddlLookup_infoMessage_last = System.currentTimeMillis();
			}
		}

		// use the installed ObjectLookupInspector to get data
		// If the lookup produced a result, add it to the StorageQueue, which writes it to the database
		if (_logger.isDebugEnabled())
			_logger.debug("######################################################## DO LOOKUP: " + qe);

		boolean debugPrintConsole = Configuration.getCombinedConfiguration().getBooleanProperty("PersistentCounterHandler.doObjectInfoLookup.print.console", false); 
		if (debugPrintConsole)
			System.out.println("######################################################## DO LOOKUP >>>>: " + qe);

		if (qe.isPrintInfo())
			_logger.info("DEBUG: ######################################################## DO LOOKUP >>>>: " + qe);
		
		//-----------------------------------------------------------------------------------
		// Do object lookup
		//-----------------------------------------------------------------------------------
		List<DdlDetails> lookupEntryList = _objectLookupInspector.doObjectInfoLookup(conn, qe, this);

		if (qe.isPrintInfo())
			_logger.info("DEBUG: ######################################################## DO LOOKUP <<<<: " + lookupEntryList);

		if (debugPrintConsole)
			System.out.println("######################################################## DO LOOKUP <<<<: " + lookupEntryList);

		if (_logger.isDebugEnabled())
			_logger.debug("LOOKUP RETURNED: count=" + (lookupEntryList == null ? "-null-" : lookupEntryList.size()) + "  (which will be sent to _ddl***Store***Queue): " +  lookupEntryList);

//System.out.println("LOOKUP RETURNED: count=" + (lookupEntryList == null ? "-null-" : lookupEntryList.size()) + " (which will be sent to _ddl***Store***Queue): " +  lookupEntryList);

		if (lookupEntryList != null && !lookupEntryList.isEmpty())
		{
			boolean isSleepOptionSet = true;

			for (DdlDetails lookupEntry : lookupEntryList)
			{
				if (qe.isPrintInfo())
				{
					lookupEntry.setPrintInfo(true);
					_logger.info("DEBUG: LOOKUP RETURNED DETAILS: " +  lookupEntry.toStringDebug(1024));
				}
				
//System.out.println("LOOKUP RETURNED DETAILS: " +  lookupEntry.toStringDebug(1024));
				//System.out.println("LOOKUP RETURNED DETAILS: " +  lookupEntry.toStringDebug(1024));

				int qsize = _ddlStoreQueue.size();
				if (qsize > _warnDdlStoreQueueSizeThresh)
				{
					long lastMessagePrint = System.currentTimeMillis() - _warnDdlStoreQueueSizeThreshPrintLastTime;
					if (lastMessagePrint > _warnDdlStoreQueueSizeThreshPrintEveryXms)
					{
						_logger.warn("The DDL Storage queue has " + qsize + " entries. The persistent writer might not keep in pace.");
						_warnDdlStoreQueueSizeThreshPrintLastTime = System.currentTimeMillis();
					}
				}
				
				//-----------------------------------------------------------------------------------
				// Add to "storage" queue
				//-----------------------------------------------------------------------------------
				_ddlStoreQueue.add(lookupEntry);
//System.out.println("AFTER ADD TO: _ddlStoreQueue. size is: " + _ddlStoreQueue.size());
				
				if (qe.isPrintInfo())
					_logger.info("DEBUG: AFTER ADD TO: _ddlStoreQueue. size is: " + _ddlStoreQueue.size());

				if ( ! lookupEntry.isSleepOptionSet() )
					isSleepOptionSet = false;
			}
			fireQueueSizeChange();
			
			return isSleepOptionSet;
		}
		return false;
	}

	/**
	 * Mark the object as "discarded" in all writers
	 * 
	 * @param dbname      
	 * @param objectName  
	 */
	public void markDdlDetailsAsDiscarded(String dbname, String objectName)
	{
		// loop all writer classes
		for (IPersistWriter pw : _writerClasses)
		{
			pw.markDdlDetailsAsDiscarded(dbname, objectName);
		}
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
				_logger.error("The Persistent Writer got runtime error in consumeDdl() in Persistent Writer named '" + pw.getName() + "'. Continuing with next Writer...", t);
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
		
		// NOTE: 
		// SessionStartTime IS SET and dictaded by the writer (at start the above 'cont.getSessionStartTime()' is most likly NULL
		// and it's the Writer instance that saves/handles the SessionStartDate and does a new startSession()
		//
		
		// loop all writer classes
		for (IPersistWriter pw : _writerClasses)
		{
			// if we are about to STOP the service
			if ( ! isRunning() )
			{
				_logger.info("The service is about to stop, discarding a consume(ObjectLookupQueueEntry:Input) queue entry.");
				continue;
			}

			// Set/restore the original sessionStartTime
			cont.setSessionStartTime(initialSessionStartTime);

			// Start the clock
			long startTime = System.currentTimeMillis();

			// CALL THE installed Writer
			// AND catch all runtime errors that might come
			try 
			{
//				_logger.info("Persisting Counters using '" + pw.getName() + "' for sessionStartTime='" + cont.getSessionStartTime() + "', mainSampleTime='" + cont.getMainSampleTime() + "'. Previous persist took " + prevConsumeTimeMs + " ms. inserts=" + pw.getInserts() + ", updates=" + pw.getUpdates() + ", deletes=" + pw.getDeletes() + ", createTables=" + pw.getCreateTables() + ", alterTables=" + pw.getAlterTables() + ", dropTables=" + pw.getDropTables() + ".");

				// BEGIN-OF-SAMPLE If we want to do anything in here
				pw.beginOfSample(cont);

				// If a Session has not yet been started (or you need to "restart" one), go and do that.
				if ( ! pw.isSessionStarted() || cont.getStartNewSample() )
				{
					Timestamp newTs = cont.getMainSampleTime();
					cont.setSessionStartTime(newTs);
					pw.setSessionStartTime(newTs);

					pw.startSession(cont);
					// note: the above pw.startSession() must call: pw.setSessionStarted(true);
					// otherwise we will run this everytime
				}

				// Set the Session Start Time in the container.
				Timestamp newTs = pw.getSessionStartTime();
				cont.setSessionStartTime(newTs);

				// CREATE-DDL
				for (CountersModel cm : cont.getCounterObjects())
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
				for (CountersModel cm : cont.getCounterObjects())
				{
					pw.saveCounters(cm);
				}

				
				// END-OF-SAMPLE If we want to do anything in here
				pw.endOfSample(cont, false);

				// Stop clock and print statistics.
				long execTime = TimeUtils.msDiffNow(startTime);
				setConsumeTime(execTime);

				firePcsConsumeInfo(pw.getName(), cont.getServerNameOrAlias(), cont.getSessionStartTime(), cont.getMainSampleTime(), (int)execTime, pw.getStatistics());

				// Reset the statistics
				pw.resetCounters();
			}
			catch (Throwable t)
			{
				_logger.error("The Persistent Writer got runtime error in consume() in Persistent Writer named '" + pw.getName() + "'. Continuing with next Writer...", t);
				pw.endOfSample(cont, true);
			}
		}
	}

	private void setConsumeTime(long lastExecTime)
	{
//		_maxConsumeTime = Math.max(_maxConsumeTime, lastExecTime);
		
		if (lastExecTime > _maxConsumeTime)
		{
			_maxConsumeTime      = lastExecTime;
			_maxConsumeTimeStamp = System.currentTimeMillis();
		}
		else
		{
			// if we have a "bump in the road", meaning: one persist took an awfull long time, but normally this does not happen
			// then: lets slowly decrease the '_maxConsumeTime'
			
			// FIXME: how should this algorithm look like?   half the _maxConsumeTime every time, or only after ### ms from _maxConsumeTimeStamp...
			//        for now the PersistWriterToHttpJson.checkSendAlarm() uses a max value of 10 minutes...
			//_maxConsumeTime = _maxConsumeTime / 2;
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
				_logger.info("Starting Counters Storage Session '" + pw.getName() + "' for sessionStartTime='" + cont.getSessionStartTime() + "', server='" + cont.getServerName() + "', with serverAlias='" + cont.getServerNameAlias() + "', displayName='" + cont.getServerDisplayName() + "'.");

				pw.startSession(cont);
			}
			catch (Throwable t)
			{
				_logger.error("The Persistent Writer got runtime error when calling the method startSession() in Persistent Writer named '" + pw.getName() + "'. Continuing with next Writer...", t);
			}
		}
	}

	
	/**
	 * Read from the Input Queue, and Do lookups of DDL in the ASE database 
	 */
	private class DdlLookupQueueHandler
	implements Runnable
	{
		private void checkForConfigChanges()
		{
			// doSqlCaptureAndStore
			boolean doDdlLookupAndStore = _props.getBooleanProperty(PROPKEY_ddl_doDdlLookupAndStore, DEFAULT_ddl_doDdlLookupAndStore);
			if (doDdlLookupAndStore != _doDdlLookupAndStore)
			{
				_logger.info("DdlLookupQueueHandler: Discovered a config change in _doDdlLookupAndStore from '" + _doDdlLookupAndStore + "', to '" + doDdlLookupAndStore + "'.");
				_doDdlLookupAndStore = doDdlLookupAndStore;
			}

			// ddlLookup_enabledForDatabaseObjects
			boolean ddlLookup_enabledForDatabaseObjects = _props.getBooleanProperty(PROPKEY_ddl_enabledForDatabaseObjects, DEFAULT_ddl_enabledForDatabaseObjects);
			if (ddlLookup_enabledForDatabaseObjects != _ddlLookup_enabledForDatabaseObjects)
			{
				_logger.info("DdlLookupQueueHandler: Discovered a config change in _ddlLookup_enabledForDatabaseObjects from '" + _ddlLookup_enabledForDatabaseObjects + "', to '" + ddlLookup_enabledForDatabaseObjects + "'.");
				_ddlLookup_enabledForDatabaseObjects = ddlLookup_enabledForDatabaseObjects;
			}

			// ddlLookup_enabledForDatabaseObjects
			boolean ddlLookup_enabledForStatementCache = _props.getBooleanProperty(PROPKEY_ddl_enabledForStatementCache, DEFAULT_ddl_enabledForStatementCache);
			if (ddlLookup_enabledForStatementCache != _ddlLookup_enabledForStatementCache)
			{
				_logger.info("DdlLookupQueueHandler: Discovered a config change in _ddlLookup_enabledForStatementCache from '" + _ddlLookup_enabledForStatementCache + "', to '" + ddlLookup_enabledForStatementCache + "'.");
				_ddlLookup_enabledForStatementCache = ddlLookup_enabledForStatementCache;
			}

			// Check if Sleep time was changed
			int afterDdlLookupSleepTimeInMs = _props.getIntProperty(PROPKEY_ddl_afterDdlLookupSleepTimeInMs, DEFAULT_ddl_afterDdlLookupSleepTimeInMs);
			if (afterDdlLookupSleepTimeInMs != _afterDdlLookupSleepTimeInMs)
			{
				_logger.info("DdlLookupQueueHandler: Discovered a config change in sleep time 'after persist' from '" + _sqlCaptureSleepTimeInMs + "', to '" + afterDdlLookupSleepTimeInMs + "'.");
				_afterDdlLookupSleepTimeInMs = afterDdlLookupSleepTimeInMs;
			}

			// 
			boolean doGetStatistics = _props.getBooleanProperty(PROPKEY_ddl_doGetStatistics, DEFAULT_ddl_doGetStatistics);
			if (doGetStatistics != _doGetStatistics)
			{
				_logger.info("DdlLookupQueueHandler: Discovered a config change in _doGetStatistics from '" + _doGetStatistics + "', to '" + doGetStatistics + "'.");
				_doGetStatistics = doGetStatistics;
			}

			// 
			boolean doSpDepends = _props.getBooleanProperty(PROPKEY_ddl_doSpDepends, DEFAULT_ddl_doSpDepends);
			if (doSpDepends != _doSpDepends)
			{
				_logger.info("DdlLookupQueueHandler: Discovered a config change in _doSpDepends from '" + _doSpDepends + "', to '" + doSpDepends + "'.");
				_doSpDepends = doSpDepends;
			}

			// Add defendant Object
			boolean addDependantObjectsToDdlInQueue = _props.getBooleanProperty(PROPKEY_ddl_addDependantObjectsToDdlInQueue, DEFAULT_ddl_addDependantObjectsToDdlInQueue);
			if (addDependantObjectsToDdlInQueue != _addDependantObjectsToDdlInQueue)
			{
				_logger.info("DdlLookupQueueHandler: Discovered a config change in _addDependantObjectsToDdlInQueue from '" + _addDependantObjectsToDdlInQueue + "', to '" + addDependantObjectsToDdlInQueue + "'.");
				_addDependantObjectsToDdlInQueue = addDependantObjectsToDdlInQueue;
			}
		}

		@Override
		public void run()
		{
			String threadName = Thread.currentThread().getName();
			_logger.info("Starting a thread for the module '" + threadName + "'.");
	
			isInitialized();
	
			_running = true;
			long prevLookupTimeMs = 0;
	
			while(isRunning())
			{
//System.out.println("DdlLookupQueueHandler... at TOP: isRunning()=" + isRunning());
				//_logger.info("Thread '" + _thread.getName() + "', SLEEPS...");
				//try { Thread.sleep(5 * 1000); }
				//catch (InterruptedException ignore) {}
				
				if (_logger.isDebugEnabled())
					_logger.debug("Thread '" + threadName + "', waiting on queue...");
	
				try 
				{
//					ObjectLookupQueueEntry qe = _ddlInputQueue.take();
//					fireQueueSizeChange();
					ObjectLookupQueueEntry qe = _ddlInputQueue.poll(1000, TimeUnit.MILLISECONDS);
					if (qe != null)
						fireQueueSizeChange();

					checkForConfigChanges();

					// this should not happen, but just in case
					if ( ! _doDdlLookupAndStore )
						continue;

					// Make sure the container isn't empty.
					if (qe == null)
						continue;
	
					// if we are about to STOP the service
					if ( ! isRunning() )
					{
						_logger.info("The service is about to stop, discarding a consume(ObjectLookupQueueEntry:Input) queue entry.");
						continue;
					}

					// Go and store or consume the in-data/container
					long startTime = System.currentTimeMillis();
//					boolean didLookup = ddlLookup( qe, prevLookupTimeMs );
					boolean didLookup = doObjectInfoLookup( qe, prevLookupTimeMs );
					long stopTime = System.currentTimeMillis();
	
					prevLookupTimeMs = stopTime-startTime;
					if (_logger.isDebugEnabled())
						_logger.debug("It took " + prevLookupTimeMs + " ms to lookup the DDL " + qe + ".");

					// Check the ObjectInfoLookup Connection and see that it's healty... 
					// Meaning no open transaction left etc...
//System.out.println("DdlLookupQueueHandler... AFTER:doObjectInfoLookup()... prevLookupTimeMs=" + prevLookupTimeMs + ", didLookup=" + didLookup);
					if (didLookup)
					{
						// Only check every X minute
						long howLongAgo = System.currentTimeMillis() - _ddlLookup_checkConn_last;
//System.out.println("DdlLookupQueueHandler... AFTER:doObjectInfoLookup()... prevLookupTimeMs=" + prevLookupTimeMs + ", didLookup=" + didLookup + ", _ddlLookup_checkConn_period=" + _ddlLookup_checkConn_period + ", howLongAgo=" + howLongAgo);
						if (howLongAgo > _ddlLookup_checkConn_period)
						{
							checkObjectInfoLookupConnection();
							_ddlLookup_checkConn_last = System.currentTimeMillis();
						}
					}
					
					// Let others do some work. so we don't monopolize the server.
					if (didLookup && _afterDdlLookupSleepTimeInMs > 0)
						Thread.sleep((int)_afterDdlLookupSleepTimeInMs);
				} 
				catch (InterruptedException ex) 
				{
					_running = false;
				}
			}
	
			_logger.info("Emptying the DDL Input queue for module '" + threadName + "', which had " + _ddlInputQueue.size() + " entries.");
			_ddlInputQueue.clear();
			fireQueueSizeChange();

			// Close the Lookup Connection
			closeDdlLookupConnection();
			
			_logger.info("Thread '" + threadName + "' was stopped.");
		}
	}

	/**
	 * Read from the Storage Queue, and send use all Writers to save DDL 
	 */
	private class DdlStorageConsumer
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
					DdlDetails ddlDetails = _ddlStoreQueue.take();
//System.out.println("DdlStorageConsumer._ddlStoreQueue.take() ... afterTake: size=" + _ddlStoreQueue.size() + ", takenObject: " + ddlDetails);
					fireQueueSizeChange();

					// this should not happen, but just in case
					if ( ! _doDdlLookupAndStore )
						continue;

					// Make sure the container isn't empty.
					if (ddlDetails == null)
						continue;

					if (ddlDetails.isEmpty())
					{
						_logger.info("Skipping DDL Storage, it seems to be 'empty' (nothing to store). ddlDetails=" + ddlDetails);
						continue;
					}
					
					// if we are about to STOP the service
					if ( ! isRunning() )
					{
						_logger.info("The service is about to stop, discarding a consume(ObjectLookupQueueEntry:Store) queue entry.");
						continue;
					}


					//-----------------------------------------------------------------------------------
					// Go and store or consume the in-data/container
					//-----------------------------------------------------------------------------------
					long startTime = System.currentTimeMillis();
					saveDdl( ddlDetails, prevConsumeTimeMs );
					long stopTime = System.currentTimeMillis();
	
					prevConsumeTimeMs = stopTime-startTime;
					_logger.debug("It took " + prevConsumeTimeMs + " ms to persist the above DDL information (using all writers).");
					
				} 
				catch (InterruptedException ex) 
				{
					_running = false;
				}
			}
	
			_logger.info("Emptying the DDL Input queue for module '" + threadName + "', which had " + _ddlInputQueue.size() + " entries.");
			_ddlInputQueue.clear();
			fireQueueSizeChange();
	
			_logger.info("Thread '" + threadName + "' was stopped.");
		}
	}

	/**
	 * Read from the Container "in" queue, and use all Writers to save DATA 
	 */
	@Override
	public void run()
	{
		String threadName = _thread.getName();
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
				PersistContainer cont = _containerQueue.take();
				fireQueueSizeChange();

				// Make sure the container isn't empty.
				if (cont == null || (cont != null && cont.isEmpty()) )
					continue;

				// if we are about to STOP the service
				if ( ! isRunning() )
				{
					_logger.info("The service is about to stop, discarding a consume(PersistContainer) queue entry.");
					continue;
				}

				// Go and store or consume the in-data/container
				_currentConsumeStartTime = System.currentTimeMillis();
				long startTime = System.currentTimeMillis();
				consume( cont, prevConsumeTimeMs );
				long stopTime = System.currentTimeMillis();
				_currentConsumeStartTime = 0;

				prevConsumeTimeMs = stopTime-startTime;
				_logger.debug("It took " + prevConsumeTimeMs + " ms to persist the above information (using all writers).");
				
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

		_logger.info("Emptying the queue for module '" + threadName + "', which had " + _containerQueue.size() + " entries.");
		_containerQueue.clear();
		fireQueueSizeChange();

		_logger.info("Thread '" + threadName + "' was stopped.");
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

		if (_doDdlLookupAndStore)
		{
			// Start the DDL Lookup Thread
			_ddlLookup = new DdlLookupQueueHandler();
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

		if (_doSqlCaptureAndStore)
		{
			// Start the SQL Capture Thread
			_sqlCapture = new SqlCaptureHandler();
			_sqlCaptureThread = new Thread(_sqlCapture);
			_sqlCaptureThread.setName("SqlCaptureThread");
			_sqlCaptureThread.setDaemon(true);
			_sqlCaptureThread.start();

			// Start the SQL Capture Storage Thread
			_sqlCaptureStorage = new SqlCaptureStorageConsumer();
			_sqlCaptureStorageThread = new Thread(_sqlCaptureStorage);
			_sqlCaptureStorageThread.setName("SqlCaptureStorageThread");
			_sqlCaptureStorageThread.setDaemon(true);
			_sqlCaptureStorageThread.start();
		}
	}

	/**
	 * Stop this subsystem
	 */
	public void stop(boolean clearQueues, int maxWaitTimeInMs)
	{
		_running = false;

		if (clearQueues)
		{
			_containerQueue.clear();
			_ddlInputQueue.clear();
			_ddlStoreQueue.clear();
			fireQueueSizeChange();
		}

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

		if (_sqlCaptureThread != null)
		{
			_sqlCaptureThread.interrupt();
			_sqlCaptureThread = null;
		}

		if (_sqlCaptureStorageThread != null)
		{
			_sqlCaptureStorageThread.interrupt();
			_sqlCaptureStorageThread = null;
		}

		// Close the connections to the data store.
		for (IPersistWriter pw : _writerClasses)
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
	public List<IPersistWriter> getWriters()
	{
		return _writerClasses;
	}
	
	////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////
	//// DDL Lookup Database Connection
	////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////
	/** Connection to the DDL Lookup. */
	private DbxConnection _ddlLookupConn          = null;
	private int           _dbmsVersion             = 0;
	private long          _lastIsClosedCheck       = 0;
	private long          _lastIsClosedRefreshTime = 1200;
	
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
	private DbxConnection getLookupConnection()
	{
		// First check the "cached connection" if it's valid
		if (isDdlLookupConnected(false, true))
			return getDdlLookupConnection();

		// If the Counter Collector isn't running, no need to continue
		if ( ! CounterController.hasInstance() )
			return null;
		ICounterController cc = CounterController.getInstance();

		// If the Counter Collector isn't connected, no need to continue
		if ( ! cc.isMonConnected() )
			return null;
		
		// Lets grab a new connection then...
		try
		{
//			DbxConnection conn = _connProvider.getNewConnection(Version.getAppName() + "-ObjInfoLookup");
//			Connection    conn = AseConnectionFactory.getConnection(null, Version.getAppName() + "-DdlLookup", null);

			if (_objectLookupInspector != null)
			{
				DbxConnection conn = _objectLookupInspector.createConnection();
				setDdlLookupConnection(conn);
			}
			else
				_logger.error("There is no Object Lookup Inspector installed, so I can't grab a DB Connection to do the lookup.");
		}
		catch (Exception e)
		{
			_logger.error("Problems Getting DDL Lookup Connection. Caught: " + e);
			setDdlLookupConnection(null);
		}
		
		return getDdlLookupConnection();
	}

	/** Set the <code>Connection</code> to use for DDL Lookups. */
	public void setDdlLookupConnection(DbxConnection conn)
	{
		_ddlLookupConn = conn;
		if (_ddlLookupConn == null)
		{
			_dbmsVersion = 0;
		}
		else
		{
			if (_objectLookupInspector != null)
				_objectLookupInspector.onConnect(_ddlLookupConn);

//			_dbmsVersion = conn.getDbmsVersionNumber();
////			_dbmsVersion = AseConnectionUtils.getAseVersionNumber(_ddlLookupConn);
//
//			// Should we install Procs
//			try
//			{
//				if (_dbmsVersion >= Ver.ver(15,7))
//				{
//					// do not install use: sp_showoptstats instead
//				}
//				else if (_dbmsVersion >= Ver.ver(15,0))
//					AseConnectionUtils.checkCreateStoredProc(_ddlLookupConn, Ver.ver(15,0),     "sybsystemprocs", "sp__optdiag", VersionInfo.SP__OPTDIAG_CRDATE, VersionInfo.class, "sp__optdiag_v1_15_0.sql", "sa_role");
//				else
//					AseConnectionUtils.checkCreateStoredProc(_ddlLookupConn, Ver.ver(12,5,0,3), "sybsystemprocs", "sp__optdiag", VersionInfo.SP__OPTDIAG_CRDATE, VersionInfo.class, "sp__optdiag_v1_9_4.sql", "sa_role");
//			}
//			catch (Exception e)
//			{
//				// the checkCreateStoredProc, writes information to error log, so we don't need to do it again.
//			}
		}
	}

	/** Gets the <code>Connection</code> to the monitored server. */
	public DbxConnection getDdlLookupConnection()
	{
		return _ddlLookupConn;
	}

	/** Gets the <code>Connection</code> to the monitored server. */
	public void closeDdlLookupConnection()
	{
		if (_ddlLookupConn == null) 
			return;

		try
		{
			if ( ! _ddlLookupConn.isClosed() )
			{
				_ddlLookupConn.close();
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
		_ddlLookupConn = null;
	}

	public boolean isDdlLookupConnected(boolean forceConnectionCheck, boolean closeConnOnFailure)
	{
		if (_ddlLookupConn == null) 
			return false;

		// Cache the last call for X ms (default 1200 ms)
		if ( ! forceConnectionCheck )
		{
			long diff = System.currentTimeMillis() - _lastIsClosedCheck;
			if ( diff < _lastIsClosedRefreshTime)
			{
				_logger.debug("    <<--- isDdlLookupConnected(): not time for refresh. diff='" + diff + "', _lastIsClosedRefreshTime='" + _lastIsClosedRefreshTime + "'.");
				return true;
			}
		}

		// check the connection itself
		try
		{
			// jConnect issues RPC: sp_mda 0, 7 on isClosed()
			if (_ddlLookupConn.isClosed())
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
//	private static class ObjectLookupQueueEntry
//	{
//		public String _dbname;
//		public String _objectName;
//		public String _source;
//		public String _dependParent;
//		public int    _dependLevel;
//
//		public ObjectLookupQueueEntry(String dbname, String objectName, String source, String dependParent, int dependLevel)
//		{
//			_dbname       = dbname;
//			_objectName   = objectName;
//			_source       = source;
//			_dependParent = dependParent;
//			_dependLevel  = dependLevel;
//		}
//		
//		@Override
//		public String toString()
//		{
//			StringBuilder sb = new StringBuilder();
//			sb.append(_dbname).append(":").append(_objectName);
//			return sb.toString(); 
//		}
//	}


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
		public void pcsStorageQueueChange(int pcsQueueSize, int ddlLookupQueueSize, int ddlStoreQueueSize, int sqlCapStoreQueueSize, int sqlCapStoreQueueEntries);

//		/**
//		 * This will be called once for each of the Persist Writers
//		 * @param persistWriterName   Name of the Writer
//		 * @param sessionStartTime    Time when the SESSION started to collect data
//		 * @param mainSampleTime      Time for last main period (if sample time is 10 seconds, this will be the "head" time for all subsequent Performance Counters individual times)
//		 * @param persistTimeInMs     Number of milliseconds it took for the Performance Writer to store the data
//		 * @param inserts             Number of insert operations that was done for this save
//		 * @param updates             Number of update operations that was done for this save
//		 * @param deletes             Number of delete operations that was done for this save
//		 * @param createTables        Number of create table operations that was done for this save
//		 * @param alterTables         Number of alter table operations that was done for this save
//		 * @param dropTables          Number of drop table operations that was done for this save
//		 */
//		public void pcsConsumeInfo(String persistWriterName, Timestamp sessionStartTime, Timestamp mainSampleTime, int persistTimeInMs, int inserts, int updates, int deletes, int createTables, int alterTables, int dropTables, int ddlSaveCount, int ddlSaveCountSum);

		/**
		 * This will be called once for each of the Persist Writers
		 * @param persistWriterName   Name of the Writer
		 * @param sessionStartTime    Time when the SESSION started to collect data
		 * @param mainSampleTime      Time for last main period (if sample time is 10 seconds, this will be the "head" time for all subsequent Performance Counters individual times)
		 * @param persistTimeInMs     Number of milliseconds it took for the Performance Writer to store the data
		 * @param writerStatistics    The actual statistical counters object
		 */
		public void pcsConsumeInfo(String persistWriterName, Timestamp sessionStartTime, Timestamp mainSampleTime, int persistTimeInMs, PersistWriterStatistics writerStatistics);
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
		int ddlLookupQueueSize   = _ddlInputQueue .size();
		int ddlStoreQueueSize    = _ddlStoreQueue .size();
		int sqlCapStoreQueueSize = _sqlCaptureStoreQueue.size();
		int sqlCapStoreEntries   = getSqlCaptureStorageEntries();

		for (PcsQueueChange l : _queueChangeListeners)
			l.pcsStorageQueueChange(pcsQueueSize, ddlLookupQueueSize, ddlStoreQueueSize, sqlCapStoreQueueSize, sqlCapStoreEntries);
	}

	/** Kicked off when consume is done 
	 * @param ddlSaveCount */
	public void firePcsConsumeInfo(String persistWriterName, String serverName, Timestamp sessionStartTime, Timestamp mainSampleTime, int persistTimeInMs, PersistWriterStatistics writerStatistics)
	{
		int    pcsQueueSize = _containerQueue.size();
		String h2WriterStat = "";
		if ("PersistWriterJdbc".equals(persistWriterName))
			h2WriterStat = H2WriterStat.getInstance().refreshCounters().getStatString();

		_maxLenPersistWriterName = Math.max(_maxLenPersistWriterName, persistWriterName.length());
		_maxLenServerName        = Math.max(_maxLenServerName,        serverName       .length());

		_logger.info("Persisting Counters using " + StringUtil.left("'" + persistWriterName + "', ", _maxLenPersistWriterName+4)
				+ "for serverName="               + StringUtil.left("'" + serverName        + "', ", _maxLenServerName+4)
				+ "sessionStartTime='"            + StringUtil.left(sessionStartTime + "",23)     + "', "
				+ "mainSampleTime='"              + StringUtil.left(mainSampleTime   + "",23)     + "'. "
				+ "This persist took ["           + TimeUtils.msToTimeStrShort(persistTimeInMs) + "] " + StringUtil.left(persistTimeInMs + " ms. ", 10)
				+ "qs="                           + StringUtil.left(pcsQueueSize + ".", 4) // ###.
				+ "jvmMemoryLeftInMB="            + Memory.getMemoryLeftInMB() + ". " 
				+ h2WriterStat 
				+ writerStatistics.getStatisticsString() );

		for (PcsQueueChange l : _queueChangeListeners)
			l.pcsConsumeInfo(persistWriterName, sessionStartTime, mainSampleTime, persistTimeInMs, writerStatistics);
	}
	// Below are just used for formating the above string (for readability)
	private int _maxLenPersistWriterName = 0;
	private int _maxLenServerName        = 0;



	////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////
	//// SQL Capture Database Connection
	////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////
	/** Connection to the DDL Storage. */
	private DbxConnection _sqlCaptureConn                    = null;
//	private int           _sqlCaptureDbmsVersion             = 0;
	private long          _lastSqlCaptureIsClosedCheck       = 0;
	private long          _lastSqlCaptureIsClosedRefreshTime = 1200;

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
	private DbxConnection getSqlCaptureConnection()
	{
		// First check the "cached connection" if it's valid
		if (isSqlCaptureConnected(false, true))
			return getSqlCaptureConnectionXXX();

		// If the Counter Collector isn't running, no need to continue
		if ( ! CounterController.hasInstance() )
			return null;
		ICounterController cc = CounterController.getInstance();

		// If the Counter Collector isn't connected, no need to continue
		if ( ! cc.isMonConnected() )
			return null;
		
		// Lets grab a new connection then...
		try
		{
//			DbxConnection conn = _connProvider.getNewConnection(Version.getAppName() + "-ObjInfoLookup");
//			Connection    conn = AseConnectionFactory.getConnection(null, Version.getAppName() + "-DdlLookup", null);

			if (_sqlCaptureBroker != null)
			{
				DbxConnection conn = _sqlCaptureBroker.createConnection();
				setSqlCaptureConnection(conn);
			}
			else
				_logger.error("There is no SQL Capture Inspector installed, so I can't grab a DB Connection to do the lookup.");
		}
		catch (Exception e)
		{
			_logger.error("Problems Getting SQL Capture Connection. Caught: " + e);
			setSqlCaptureConnection(null);
		}
		
		return getSqlCaptureConnectionXXX();
	}

	/** Set the <code>Connection</code> to use for DDL Lookups. */
	public void setSqlCaptureConnection(DbxConnection conn)
	{
		_sqlCaptureConn = conn;
		if (conn != null)
		{
			if (_sqlCaptureBroker != null)
				_sqlCaptureBroker.onConnect(conn);
		}
	}

	/** Gets the <code>Connection</code> to the monitored server. */
	public DbxConnection getSqlCaptureConnectionXXX()
	{
		return _sqlCaptureConn;
	}

	/** Gets the <code>Connection</code> to the monitored server. */
	public void closeSqlCaptureConnection()
	{
		if (_sqlCaptureConn == null) 
			return;

		try
		{
			if ( ! _sqlCaptureConn.isClosed() )
			{
				_sqlCaptureConn.close();
				if (_logger.isDebugEnabled())
				{
					_logger.debug("SQL Capture Connection closed");
				}
			}
		}
		catch (SQLException ev)
		{
			_logger.error("closeSqlCaptureConnection", ev);
		}
		_sqlCaptureConn = null;
	}

	public boolean isSqlCaptureConnected(boolean forceConnectionCheck, boolean closeConnOnFailure)
	{
		if (_sqlCaptureConn == null) 
			return false;

		// Cache the last call for X ms (default 1200 ms)
		if ( ! forceConnectionCheck )
		{
			long diff = System.currentTimeMillis() - _lastSqlCaptureIsClosedCheck;
			if ( diff < _lastSqlCaptureIsClosedRefreshTime)
			{
				_logger.debug("    <<--- isSqlCaptureConnected(): not time for refresh. diff='" + diff + "', _lastSqlCaptureIsClosedRefreshTime='" + _lastSqlCaptureIsClosedRefreshTime + "'.");
				return true;
			}
		}

		// check the connection itself
		try
		{
			// jConnect issues/executing RPC: sp_mda 0, 7 on isClosed()
			if (_sqlCaptureConn.isClosed())
			{
				if (closeConnOnFailure)
					closeSqlCaptureConnection();
				return false;
			}
		}
		catch (SQLException e)
		{
			return false;
		}

		_lastSqlCaptureIsClosedCheck = System.currentTimeMillis();
		return true;
	}

	/**
	 * Read from the Input Queue, and Do lookups of DDL in the ASE database 
	 */
	private class SqlCaptureHandler
	implements Runnable
	{
		private void checkForConfigChanges()
		{
			// doSqlCaptureAndStore
			boolean doSqlCaptureAndStore = _props.getBooleanProperty(PROPKEY_sqlCap_doSqlCaptureAndStore, DEFAULT_sqlCap_doSqlCaptureAndStore);
			if (doSqlCaptureAndStore != _doSqlCaptureAndStore)
			{
				_logger.info("SqlCaptureHandler: Discovered a config change in _doSqlCaptureAndStore from '" + _doSqlCaptureAndStore + "', to '" + doSqlCaptureAndStore + "'.");
				_doSqlCaptureAndStore = doSqlCaptureAndStore;
			}

			// Sleep time 
			int sqlCaptureSleepTimeInMs = _props.getIntProperty(PROPKEY_sqlCap_sleepTimeInMs, DEFAULT_sqlCap_sleepTimeInMs);
			if (sqlCaptureSleepTimeInMs != _sqlCaptureSleepTimeInMs)
			{
				_logger.info("SqlCaptureHandler: Discovered a config change in sleep time from '" + _sqlCaptureSleepTimeInMs + "', to '" + sqlCaptureSleepTimeInMs + "'.");
				_sqlCaptureSleepTimeInMs = sqlCaptureSleepTimeInMs;
			}
		}

		@Override
		public void run()
		{
			String threadName = Thread.currentThread().getName();
			_logger.info("Starting a thread for the module '" + threadName + "'.");
	
			isInitialized();
	
			_running = true;
			long timeMs = 0;
	
			while(isRunning())
			{
//System.out.println("SqlCaptureHandler... at TOP: isRunning()=" + isRunning());
				try 
				{
					checkForConfigChanges();
					
					// this should not happen, but just in case
					if ( ! _doSqlCaptureAndStore )
					{
						Thread.sleep(1000);
						continue;
					}
					
					// Should we check if the "main" collector is paused
//					if ( CounterController.getInstance().isPauseSampling() ) // FIXME: isPauseSampling() was only in the GUI not in CounterController
//					{
//						_logger.debug("Counter Controller is paused... skipping: doSqlCapture()");
//						Thread.sleep(1000);
//						continue;
//					}

					// Go and store or consume the in-data/container
					long startTime = System.currentTimeMillis();
					doSqlCapture();
					long stopTime = System.currentTimeMillis();
	
					timeMs = stopTime-startTime;
//System.out.println("It took " + timeMs + " ms to Capture SQL Text/Statements.");
					if (_logger.isDebugEnabled())
						_logger.debug("It took " + timeMs + " ms to Capture SQL Text/Statements.");
					
					// Only check every X minute
					long howLongAgo = System.currentTimeMillis() - _sqlCapture_checkConn_last;
//System.out.println("SqlCaptureHandler... doSqlCapture()... timeMs=" + timeMs + ", _sqlCapture_checkConn_period=" + _sqlCapture_checkConn_period + ", howLongAgo=" + howLongAgo);
					if (howLongAgo > _sqlCapture_checkConn_period)
					{
						checkSqlCaptureConnection();
						_sqlCapture_checkConn_last = System.currentTimeMillis();
					}

					// Sleep before next "poll" time
					Thread.sleep(_sqlCaptureSleepTimeInMs);
				} 
				catch (InterruptedException ex) 
				{
					_running = false;
				}
				catch (Throwable t) 
				{
					_logger.warn("Problems when calling doSqlCapture(), Skipping the problem and continuing... (however the data fetched in above batch might not have been passed on to the writer thread)", t);
					// Sleep before next "poll" time
					try { Thread.sleep(_sqlCaptureSleepTimeInMs); }
					catch(InterruptedException ex) 
					{
						_running = false;
					}
				}
			}
	
			_logger.info("Emptying the DDL Input queue for module '" + threadName + "', which had " + _sqlCaptureStoreQueue.size() + " entries.");
			_sqlCaptureStoreQueue.clear();
			fireQueueSizeChange();

			// Close the Lookup Connection
			closeSqlCaptureConnection();
			
			_logger.info("Thread '" + threadName + "' was stopped.");
		}
	}

	/**
	 * Read from the Storage Queue, and send use all Writers to save DDL 
	 */
	private class SqlCaptureStorageConsumer
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
					SqlCaptureDetails sqlCaptureDetails = _sqlCaptureStoreQueue.take();
					fireQueueSizeChange();

					// this should not happen, but just in case
					if ( ! _doSqlCaptureAndStore )
						continue;

					// Make sure the container isn't empty.
					if (sqlCaptureDetails == null)
						continue;

					if (sqlCaptureDetails.isEmpty())
						continue;
					
					// if we are about to STOP the service
					if ( ! isRunning() )
					{
						_logger.info("The service is about to stop, discarding a consume(SqlCaptureDetails:Store) queue entry.");
						continue;
					}


					// Go and store or consume the in-data/container
					long startTime = System.currentTimeMillis();
					saveSqlCapture( sqlCaptureDetails, prevConsumeTimeMs );
					long stopTime = System.currentTimeMillis();

					prevConsumeTimeMs = stopTime-startTime;
//int avgBatchSize = 0;
//for (SqlCaptureDetails capDet : _sqlCaptureStoreQueue)
//	avgBatchSize += capDet.size();
//if (_sqlCaptureStoreQueue.size() > 0)
//	avgBatchSize = avgBatchSize / _sqlCaptureStoreQueue.size();
//System.out.println("saveSqlCapture(): ms=" + prevConsumeTimeMs + ", sqlCaptureDetails.size=" + sqlCaptureDetails.size() + ", _sqlCaptureStoreQueue.size=" + _sqlCaptureStoreQueue.size() + ", avgBatchSize=" + avgBatchSize);
					_logger.debug("It took " + prevConsumeTimeMs + " ms to persist the above SQL Capture information (using all writers).");
					
				} 
				catch (InterruptedException ex) 
				{
					_running = false;
				}
				catch (Throwable t) 
				{
					_logger.warn("Problems when calling saveSqlCapture(), Skipping the problem and continuing... (however the data wont be stored for this batch)", t);
				}
			}
	
			_logger.info("Emptying the SQL Capture Input queue for module '" + threadName + "', which had " + _sqlCaptureStoreQueue.size() + " entries.");
			_sqlCaptureStoreQueue.clear();
			fireQueueSizeChange();
	
			_logger.info("Thread '" + threadName + "' was stopped.");
		}
	}

	/**
	 * Get DDL information from the database and pass it on to the storage thread
	 * 
	 * @param qe
	 * @param prevLookupTimeMs
	 * @return true if it did a lookup, false the lookup was discarded
	 */
	private boolean doSqlCapture()
	{
		DbxConnection conn = getSqlCaptureConnection();
		if (conn == null)
			return false;

		if (_sqlCaptureBroker == null)
		{
			_logger.warn("doObjectInfoLookup(): Sorry can't continue: There are no Object Lookup Inspector installed.");
			return false;
		}
		
//System.out.println("doSqlCapture(): START");
		int entries = _sqlCaptureBroker.doSqlCapture(conn, this);
//System.out.println("doSqlCapture(): END - entries=" + entries);
		
		if (_logger.isDebugEnabled())
			_logger.debug("SQL Capture Broker returned: " +  entries);

		if (entries > 0)
		{
//			int qsize = _sqlCaptureStoreQueue.size();
//			if (qsize > _warnSqlCaptureStoreQueueSizeThresh)
//			{
//				_logger.warn("The SQL Capture Storage queue has " + qsize + " entries. The persistent writer might not keep in pace.");
//			}
			fireQueueSizeChange();
			
			return true;
		}
		return false;
	}

	/**
	 * Use all installed writers to store the SQL Capture information
	 * 
	 * @param ddlDetails
	 * @param prevConsumeTimeMs
	 */
	private void saveSqlCapture(SqlCaptureDetails sqlCaptureDetails, long prevConsumeTimeMs)
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
				// or use: beginOfSample(), saveSqlCaptureDetails(), saveCounters(), endOfSample()
				pw.saveSqlCaptureDetails(sqlCaptureDetails);
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
	public void addSqlCapture(SqlCaptureDetails sqlCaptureDetails)
	{
		_sqlCaptureStoreQueue.add(sqlCaptureDetails);
	}
	
	private int getSqlCaptureStorageEntries()
	{
		int entries = 0;
		for (SqlCaptureDetails capDet : _sqlCaptureStoreQueue)
			entries += capDet.size();
		return entries;
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





//    ###### capture SQL
//    RS> Col# Label             JDBC Type Name           Guessed DBMS type Source Table              
//    RS> ---- ----------------- ------------------------ ----------------- --------------------------
//    RS> 1    SPID              java.sql.Types.INTEGER   int               master.dbo.monSysStatement
//    RS> 2    KPID              java.sql.Types.INTEGER   int               master.dbo.monSysStatement
//    RS> 3    BatchID           java.sql.Types.INTEGER   int               master.dbo.monSysStatement
//    RS> 4    LineNumber        java.sql.Types.INTEGER   int               master.dbo.monSysStatement
//    RS> 5    dbname            java.sql.Types.VARCHAR   varchar(30)       -none-                    
//    RS> 6    procname          java.sql.Types.VARCHAR   varchar(255)      -none-                    
//    RS> 7    Elapsed_ms        java.sql.Types.INTEGER   int               -none-                    
//    RS> 8    CpuTime           java.sql.Types.INTEGER   int               master.dbo.monSysStatement
//    RS> 9    WaitTime          java.sql.Types.INTEGER   int               master.dbo.monSysStatement
//    RS> 10   MemUsageKB        java.sql.Types.INTEGER   int               master.dbo.monSysStatement
//    RS> 11   PhysicalReads     java.sql.Types.INTEGER   int               master.dbo.monSysStatement
//    RS> 12   LogicalReads      java.sql.Types.INTEGER   int               master.dbo.monSysStatement
//    RS> 13   RowsAffected      java.sql.Types.INTEGER   int               master.dbo.monSysStatement
//    RS> 14   ErrorStatus       java.sql.Types.INTEGER   int               master.dbo.monSysStatement
//    RS> 15   ProcNestLevel     java.sql.Types.INTEGER   int               master.dbo.monSysStatement
//    RS> 16   StatementNumber   java.sql.Types.INTEGER   int               master.dbo.monSysStatement
//    RS> 17   PagesModified     java.sql.Types.INTEGER   int               master.dbo.monSysStatement
//    RS> 18   PacketsSent       java.sql.Types.INTEGER   int               master.dbo.monSysStatement
//    RS> 19   PacketsReceived   java.sql.Types.INTEGER   int               master.dbo.monSysStatement
//    RS> 20   NetworkPacketSize java.sql.Types.INTEGER   int               master.dbo.monSysStatement
//    RS> 21   PlansAltered      java.sql.Types.INTEGER   int               master.dbo.monSysStatement
//    RS> 22   StartTime         java.sql.Types.TIMESTAMP datetime          master.dbo.monSysStatement
//    RS> 23   EndTime           java.sql.Types.TIMESTAMP datetime          master.dbo.monSysStatement
//    RS> 24   PlanID            java.sql.Types.INTEGER   int               master.dbo.monSysStatement
//    RS> 25   DBID              java.sql.Types.INTEGER   int               master.dbo.monSysStatement
//    RS> 26   ObjOwnerID        java.sql.Types.INTEGER   int               -none-                    
//    RS> 27   ProcedureID       java.sql.Types.INTEGER   int               master.dbo.monSysStatement
//    
//    
//    ###### select * from dbo.monSysStatement where SPID not in (select spid from master..sysprocesses p where p.program_name like 'AseTune%')
//    
//    RS> Col# Label             JDBC Type Name           Guessed DBMS type Source Table              
//    RS> ---- ----------------- ------------------------ ----------------- --------------------------
//    RS> 1    SPID              java.sql.Types.INTEGER   int               master.dbo.monSysStatement
//    RS> 2    InstanceID        java.sql.Types.TINYINT   tinyint           master.dbo.monSysStatement
//    RS> 3    KPID              java.sql.Types.INTEGER   int               master.dbo.monSysStatement
//    RS> 4    DBID              java.sql.Types.INTEGER   int               master.dbo.monSysStatement
//    RS> 5    ProcedureID       java.sql.Types.INTEGER   int               master.dbo.monSysStatement
//    RS> 6    PlanID            java.sql.Types.INTEGER   int               master.dbo.monSysStatement
//    RS> 7    BatchID           java.sql.Types.INTEGER   int               master.dbo.monSysStatement
//    ++++RS> 8    ContextID         java.sql.Types.INTEGER   int               master.dbo.monSysStatement
//    RS> 9    LineNumber        java.sql.Types.INTEGER   int               master.dbo.monSysStatement
//    RS> 10   CpuTime           java.sql.Types.INTEGER   int               master.dbo.monSysStatement
//    RS> 11   WaitTime          java.sql.Types.INTEGER   int               master.dbo.monSysStatement
//    RS> 12   MemUsageKB        java.sql.Types.INTEGER   int               master.dbo.monSysStatement
//    RS> 13   PhysicalReads     java.sql.Types.INTEGER   int               master.dbo.monSysStatement
//    RS> 14   LogicalReads      java.sql.Types.INTEGER   int               master.dbo.monSysStatement
//    RS> 15   PagesModified     java.sql.Types.INTEGER   int               master.dbo.monSysStatement
//    RS> 16   PacketsSent       java.sql.Types.INTEGER   int               master.dbo.monSysStatement
//    RS> 17   PacketsReceived   java.sql.Types.INTEGER   int               master.dbo.monSysStatement
//    RS> 18   NetworkPacketSize java.sql.Types.INTEGER   int               master.dbo.monSysStatement
//    RS> 19   PlansAltered      java.sql.Types.INTEGER   int               master.dbo.monSysStatement
//    RS> 20   RowsAffected      java.sql.Types.INTEGER   int               master.dbo.monSysStatement
//    RS> 21   ErrorStatus       java.sql.Types.INTEGER   int               master.dbo.monSysStatement
//    +++RS> 22   HashKey           java.sql.Types.INTEGER   int               master.dbo.monSysStatement
//    +++RS> 23   SsqlId            java.sql.Types.INTEGER   int               master.dbo.monSysStatement
//    RS> 24   ProcNestLevel     java.sql.Types.INTEGER   int               master.dbo.monSysStatement
//    RS> 25   StatementNumber   java.sql.Types.INTEGER   int               master.dbo.monSysStatement
//    RS> 26   DBName            java.sql.Types.VARCHAR   varchar(30)       master.dbo.monSysStatement
//    RS> 27   StartTime         java.sql.Types.TIMESTAMP datetime          master.dbo.monSysStatement
//    RS> 28   EndTime           java.sql.Types.TIMESTAMP datetime          master.dbo.monSysStatement
//    
//    
//    ###### select * from dbo.monSysSQLText   where SPID not in (select spid from master..sysprocesses p where p.program_name like 'AseTune%')
//    RS> Col# Label           JDBC Type Name         Guessed DBMS type Source Table            
//    RS> ---- --------------- ---------------------- ----------------- ------------------------
//    RS> 1    SPID            java.sql.Types.INTEGER int               master.dbo.monSysSQLText
//    RS> 2    InstanceID      java.sql.Types.TINYINT tinyint           master.dbo.monSysSQLText
//    RS> 3    KPID            java.sql.Types.INTEGER int               master.dbo.monSysSQLText
//    RS> 4    ServerUserID    java.sql.Types.INTEGER int               master.dbo.monSysSQLText
//    RS> 5    BatchID         java.sql.Types.INTEGER int               master.dbo.monSysSQLText
//    RS> 6    SequenceInBatch java.sql.Types.INTEGER int               master.dbo.monSysSQLText
//    RS> 7    SQLText         java.sql.Types.VARCHAR varchar(255)      master.dbo.monSysSQLText
//    +----+----------+--------+------------+-------+---------------+---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
//    |SPID|InstanceID|KPID    |ServerUserID|BatchID|SequenceInBatch|SQLText                                                                                                                                                                          |
//    +----+----------+--------+------------+-------+---------------+---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
//    |14  |0         |32571433|1           |989    |1              |select dbname=db_name(), spid=@@spid, username = user_name(), susername =suser_name(), trancount=@@trancount, tranchained=@@tranchained, transtate=@@transtate                   |
//    |14  |0         |32571433|1           |990    |1              |select dbname=db_name(dbid), table_name=object_name(id, dbid), lock_type=type, lock_count=count(*)  from master.dbo.syslocks  where spid = @@spid         group by dbid, id, type|
//    |14  |0         |32571433|1           |992    |1              |select @@tranchained                                                                                                                                                             |
//    |14  |0         |32571433|1           |994    |1              |select @@tranchained                                                                                                                                                             |
//    |14  |0         |32571433|1           |995    |1              |select * from dbo.monSysSQLText   where SPID not in (select spid from master..sysprocesses p where p.program_name like 'AseTune%')                                               |
//    +----+----------+--------+------------+-------+---------------+---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
//    Rows 5
//    
