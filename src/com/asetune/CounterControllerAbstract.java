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
package com.asetune;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLRecoverableException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.log4j.Logger;

import com.asetune.cm.CmToolTipSupplierDefault;
import com.asetune.cm.CounterModelHostMonitor;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CountersModel;
import com.asetune.cm.CountersModelUserDefined;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.graph.TrendGraphDataPoint.LabelType;
import com.asetune.gui.ISummaryPanel;
import com.asetune.gui.MainFrame;
import com.asetune.gui.SplashWindow;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.gui.TrendGraph;
import com.asetune.gui.swing.GTable.ITableTooltip;
import com.asetune.hostmon.HostMonitor;
import com.asetune.hostmon.HostMonitorConnection;
import com.asetune.pcs.PersistContainer.HeaderInfo;
import com.asetune.sql.conn.ConnectionProp;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.conn.info.DbmsVersionInfo;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;

import it.sauronsoftware.cron4j.Scheduler;
import net.miginfocom.swing.MigLayout;

public abstract class CounterControllerAbstract
//extends Thread
implements ICounterController
{
	/** Log4j logging. */
	private static Logger _logger = Logger.getLogger(CounterControllerAbstract.class);

	private String _supportedProductString = null;
	@Override public void   setSupportedProductName(String str) { _supportedProductString = str; }
	@Override public String getSupportedProductName()           { return _supportedProductString; }

	public static final String PROPKEY_isClosed_timeout = "CounterController.isClosed.timeout";
	public static final int    DEFAULT_isClosed_timeout = 3;
	
//	protected Thread   _thread  = null;
//	protected boolean  _running = false;
	private boolean _hasGui;

	/** The counterModels has been created */
	private boolean _isCountersCreated  = false; 

	/** have we been initialized or not */
	private boolean _isInitialized      = false; 

	/** if we should do refreshing of the counters, or is refreshing paused */
	private boolean _refreshIsEnabled   = false;

	/** if any monitoring thread is currently getting information from the monitored server */
	private boolean _isRefreshing = false;

	/** When did we start last refresh. setInRefresh(true) */
	private long _startRefreshTime = 0;

	/** How many milliseconds did we spend in last refresh. diff between setInRefresh(true) -> setInRefresh(false) */
	private long _refreshTimeInMs = 0;
	
	private String _waitEvent = "";
	
	/** Keep a list of all CounterModels that you have initialized */
	protected List<CountersModel> _CmList = new ArrayList<CountersModel>();

	/** 
	 * Store DBMS properties, Would normally be populated when connecting to a DBMS that we monitor.<br>
	 * We would get some extra information that is NOT generic over all databases, like 'Edition', or 'PageSize'...<br>
	 * or some other specifics for that DBMS Vendor that is NOT populated by another vendor.<br> 
	 * 
	 * Initial use of this would be to check if we connected to a SQL-Server 'Azure' instance<br>
	 * In that case the Dynamic Management Views will an alternate name.
	 */
	private Configuration _dbmsProperties = new Configuration();

//	/** is sp_configure 'capture missing statistics' set or not */
//	protected boolean _config_captureMissingStatistics = false;
//
//	/** is sp_configure 'enable metrics capture' set or not */
//	protected boolean _config_enableMetricsCapture = false;
//	
//	/** is sp_configure 'kernel mode' set to 'process' or 'threaded'. will be false for lower versions of ASE than 15.7, and true if ASE 15.7 and sp_configure 'kernel mode' returns 'threaded' */
//	protected boolean _config_threadedKernelMode = false;
//	
//	/** A list of roles which the connected user has */
//	protected List<String> _activeRoleList = null;

	/** Statistic field: first sample time */
	private Timestamp _statFirstSampleTime = null;
	/** Statistic field: last sample time */
	private Timestamp _statLastSampleTime  = null;

	/** Normal sleep time */
	private int _defaultSleepTimeInSec = 10;

//	public static final String TRANLOG_DISK_IO_TOOLTIP =
//		  "Below is a table that describes how a fast or slow disk affects number of transactions per second.<br>" +
//		  "The below description tries to exemplify number of transactions per seconds based on Disk IO responsiveness on the <b>LOG</b> Device.<br>" +
//		  
//		  "<TABLE ALIGN='left' BORDER=1 CELLSPACING=0 CELLPADDING=0 WIDTH='100%'> " +
//		  "  <TR ALIGN='left' VALIGN='left'> <TH>Device Type      </TH> <TH>Typical IOPS </TH> <TH>Typical ms/IO</TH> <TH>Xactn/Sec      </TH> </TR> " +
//		  "  <!--                                    -----------------          -------------          -------------          --------------- -->" +
//		  "  <TR ALIGN='left' VALIGN='left'> <TD>RAID 6           </TD> <TD>600          </TD> <TD>10-25        </TD> <TD>40-100         </TD> </TR> " +
//		  "  <TR ALIGN='left' VALIGN='left'> <TD>RAID 5           </TD> <TD>800          </TD> <TD>2-6          </TD> <TD>166-500        </TD> </TR> " +
//		  "  <TR ALIGN='left' VALIGN='left'> <TD>RAID 0 (RAID 10) </TD> <TD>1000         </TD> <TD>1-2          </TD> <TD>500-1000       </TD> </TR> " +
//		  "  <TR ALIGN='left' VALIGN='left'> <TD>FC SCSI (tier 1) </TD> <TD>200-400      </TD> <TD>2-6          </TD> <TD>166-500        </TD> </TR> " +
//		  "  <TR ALIGN='left' VALIGN='left'> <TD>SAS SCSI (tier 2)</TD> <TD>150-200      </TD> <TD>4-8          </TD> <TD>125-250        </TD> </TR> " +
//		  "  <TR ALIGN='left' VALIGN='left'> <TD>SATA (tier 3)    </TD> <TD>100-150      </TD> <TD>6-10         </TD> <TD>100-166        </TD> </TR> " +
//		  "  <TR ALIGN='left' VALIGN='left'> <TD>SATA/SAS SSD     </TD> <TD>10000-20000  </TD> <TD>0.05-0.1     </TD> <TD>10,000-20,000  </TD> </TR> " +
//		  "  <TR ALIGN='left' VALIGN='left'> <TD>PCIe SSD         </TD> <TD>100000-200000</TD> <TD>0.01-0.005   </TD> <TD>100,000-200,000</TD> </TR> " +
//		  "</TABLE> " +
//		  "The above table was found in the document: <br> " +
//		  "http://www.sybase.com/detail?id=1091630<br>" +
//		  "http://www.sybase.com/files/White_Papers/Managing-DBMS-Workloads-v1.0-WP.pdf<br>";
//
//
//	/** This is a input to the SplashScreen */
//	public static final int	   NUMBER_OF_PERFORMANCE_COUNTERS	= 54 + 10 + 20; 
//	// 10 extra steps not for performance counters 
//	// 20 extra for init time of XX seconds or so

	
	private CountersModel _summaryCm     = null;
	private String        _summaryCmName = null;
	private ISummaryPanel _summaryPanel  = null;

	/** This is where the counters are <b>actually</b> fetched from the monitored server */
	private CounterCollectorThreadAbstract _counterCollectorThread = null;

	/**
	 * The default constructor
	 * @param hasGui should we create a GUI or NoGUI collection thread
	 */
	public CounterControllerAbstract(boolean hasGui)
	{
		_hasGui = hasGui;
		_counterCollectorThread = createCounterCollectorThread(hasGui);
		
//		setDbmsDataTypeResolver( createDbmsDataTypeResolver() );
	}

	/**
	 * Was this Controller created with a GUI or not
	 * @return
	 */
	public boolean hasGui()
	{
		return _hasGui;
	}

	/**
	 * Get the collector thread implementation
	 * @return The collector thread implementation
	 */
	public CounterCollectorThreadAbstract getCounterCollectorThread()
	{
		return _counterCollectorThread;
	}

	/**
	 * Create a counter collector<br>
	 * This could be overridden by any subclass if we want a change in behavior
	 * 
	 * @param hasGui should we create a GUI or NoGUI collection thread
	 */
	public CounterCollectorThreadAbstract createCounterCollectorThread(boolean hasGui)
	{
		if (hasGui)
			return new CounterCollectorThreadGui(this);
		else
			return new CounterCollectorThreadNoGui(this);
	}

	/**
	 * Initialize the Controller, and also calls init() on the CollectorThread
	 */
	@Override
	public void init() throws Exception
	{
		getCounterCollectorThread().init(hasGui());
	}

	/** 
	 * The controller should get details about a specific collection 
	 */
	@Override
	public abstract HeaderInfo createPcsHeaderInfo();


	/**
	 * Should the connection monitoring watchdog be started or not (ony for the GUI client)
	 */
	@Override
	public boolean shouldWeStart_connectionWatchDog()
	{
		return true;
	}

	/**
	 * Start the collector thread
	 */
	@Override
	public void start()
	{
		_counterCollectorThread.start();
	}

	/** shutdown or stop any collector */
	@Override
	public void shutdown()
	{
		_logger.info("Stopping the collector thread.");
//		_running = false;
//		if (_thread != null)
//			_thread.interrupt();
		getCounterCollectorThread().shutdown();
	}

	@Override
	public CountersModel getSummaryCm()
	{
		return _summaryCm;
	}

	@Override
	public void setSummaryCm(CountersModel cm)
	{
		_summaryCm = cm;
		_summaryCmName = cm == null ? null : cm.getName();
	}

	@Override
	public String getSummaryCmName()
	{
		return _summaryCmName;
	}

	@Override
	public ISummaryPanel getSummaryPanel()
	{
		return _summaryPanel;
	}

	@Override
	public void setSummaryPanel(ISummaryPanel summaryPanel)
	{
		_summaryPanel = summaryPanel;
	}

	
	/** Add a CounterModel */
	@Override
	public void addCm(CountersModel cm)
	{ 
		List<CountersModel> cmList = getCmList();
		if ( ! cmList.contains(cm) )
			cmList.add(cm);
	}

	/** Get a list of all available <code>CountersModel</code> that exists. System and UDC */
	@Override
	public List<CountersModel> getCmList()
	{ 
		return _CmList; 
	}

	/** */
	@Override
	public void setDefaultSleepTimeInSec(int sleepTime)
	{
		_defaultSleepTimeInSec = sleepTime;
	}

	/** */
	@Override
	public int getDefaultSleepTimeInSec()
	{
		return _defaultSleepTimeInSec;
	}

	/** 
	 * Get any <code>CountersModel</code> that depends on a specific ASE configuration 
	 * @return a List of CountersModel objects
	 */
	@Override
//	public List<CountersModel> getCmListDependsOnConfig(String cfgName, DbxConnection conn, long srvVersion, boolean isClusterEnabled)
	public List<CountersModel> getCmListDependsOnConfig(String cfgName, DbxConnection conn)
	{
		DbmsVersionInfo versionInfo = conn.getDbmsVersionInfo();
		
		ArrayList<CountersModel> cmList = new ArrayList<CountersModel>();
		for (CountersModel cm : getCmList())
		{
//			String[] sa = cm.getDependsOnConfigForVersion(conn, srvVersion, isClusterEnabled);
			String[] sa = cm.getDependsOnConfigForVersion(conn, versionInfo);
//			String[] sa = cm.getDependsOnConfig();
			if ( sa == null )
				continue;

			for (String cfg : sa)
			{
				int index = cfg.indexOf("=");
				if (index >= 0)
					cfg = cfg.substring(0, index).trim();
					
				if ( cfgName.equals(cfg) && ! cmList.contains(cm) )
					cmList.add(cm);
			}
		}
		return cmList; 
	}
//	/** simply do: return getCmListDependsOnConfig(cfg, null, 0, false); */
//	public List<CountersModel> getCmListDependsOnConfig(String cfg)
//	{
//		return getCmListDependsOnConfig(cfg, null, 0, false);
//	}

	/** 
	 * Get <code>CountersModel</code> object for a CM that has the "short" name for example CMprocCallStack 
	 * @return if the CM is not found a null will be return
	 */
	@Override
	public CountersModel getCmByName(String name) 
	{
		if (StringUtil.isNullOrBlank(name))
			return null;

		for (CountersModel cm : getCmList())
		{
			if (cm != null && cm.getName().equalsIgnoreCase(name))
			{
				return cm;
			}
		}
		return null;
	}
	
	/** 
	 * Get <code>CountersModel</code> object for a CM that has the "long" name for example 'Procedure Call Stack' for CMprocCallStack
	 * @return if the CM is not found a null will be return
	 */
	@Override
	public CountersModel getCmByDisplayName(String name) 
	{
		if (StringUtil.isNullOrBlank(name))
			return null;

		for (CountersModel cm : getCmList())
		{
			if (cm != null && cm.getDisplayName().equalsIgnoreCase(name))
			{
				return cm;
			}
		}
		return null;
	}

	/**
	 * Reset All CM's etc, this so we build new SQL statements if we connect to a different ASE version<br>
	 * Most possible called from disconnect() or similar
	 */
	public void reset()
	{
		reset(true);
	}

	/**
	 * Reset All CM's etc, this so we build new SQL statements if we connect to a different ASE version<br>
	 * Most possible called from disconnect() or similar
	 * 
	 * @param resetAllCms call reset() on all cm's
	 */
	@Override
	public void reset(boolean resetAllCms)
	{
		// have we been initialized or not
		_isInitialized      = false; 

		// if we should do refreshing of the counters, or is refreshing paused
		_refreshIsEnabled   = false;

		_waitEvent = "";
		
		if (resetAllCms)
		{
			for (CountersModel cm : getCmList())
				cm.reset();
		}
	}

	
	@Override
	public void setWaitEvent(String str)
	{
		_waitEvent = str;
	}
	@Override
	public String getWaitEvent()
	{
		return _waitEvent;
	}

	@Override
	public boolean isInitialized()
	{
		return _isInitialized;
	}
	public void setInitialized(boolean initialized)
	{
		_isInitialized = initialized;
	}

	public boolean isCountersCreated()
	{
		return _isCountersCreated;
	}
	public void setCountersIsCreated(boolean created)
	{
		_isCountersCreated = created;
	}

	/**
	 * When we have a database connection, lets do some extra init this
	 * so all the CountersModel can decide what SQL to use. <br>
	 * SQL statement usually depends on what ASE Server version we monitor.
	 * 
	 * @param conn
	 * @param hasGui              is this initialized with a GUI?
	 * @param srvVersion          what is the ASE Executable version
	 * @param isClusterEnabled    is it a cluster ASE
	 * @param monTablesVersion    what version of the MDA tables should we use
	 */
	@Override
//	public abstract void initCounters(Connection conn, boolean hasGui, long srvVersion, boolean isClusterEnabled, long monTablesVersion)
//	throws Exception;
	public abstract void initCounters(DbxConnection conn, boolean hasGui, long srvVersion, boolean isClusterEnabled, long monTablesVersion)
	throws Exception;

//	public void initCounters(Connection conn, boolean hasGui, long srvVersion, boolean isClusterEnabled, long monTablesVersion)
//	throws Exception
//	{
//		if (_isInitialized)
//			return;
//
//		if ( ! AseConnectionUtils.isConnectionOk(conn, hasGui, null))
//			throw new Exception("Trying to initialize the counters with a connection this seems to be broken.");
//
//			
//		if (! _countersIsCreated)
//			createCounters();
//		
//		_logger.info("Initializing all CM objects, using ASE server version number "+srvVersion+", isClusterEnabled="+isClusterEnabled+" with monTables Install version "+monTablesVersion+".");
//
//		// Get active ASE Roles
//		//List<String> activeRoleList = AseConnectionUtils.getActiveRoles(conn);
//		_activeRoleList = AseConnectionUtils.getActiveRoles(conn);
//		
//		// Get active Monitor Configuration
//		Map<String,Integer> monitorConfigMap = AseConnectionUtils.getMonitorConfigs(conn);
//
//		// Get some specific configurations
////		if (srvVersion >= 15031)
////		if (srvVersion >= 1503010)
//		if (srvVersion >= Ver.ver(15,0,3,1))
//			_config_captureMissingStatistics = AseConnectionUtils.getAseConfigRunValueBooleanNoEx(conn, "capture missing statistics");
//
////		if (srvVersion >= 15020)
////		if (srvVersion >= 1502000)
//		if (srvVersion >= Ver.ver(15,0,2))
//			_config_enableMetricsCapture = AseConnectionUtils.getAseConfigRunValueBooleanNoEx(conn, "enable metrics capture");
//
//		_config_threadedKernelMode = false;
////		if (srvVersion >= 15700)
////		if (srvVersion >= 1570000)
//		if (srvVersion >= Ver.ver(15,7))
//		{
//			String kernelMode = AseConnectionUtils.getAseConfigRunValueStrNoEx(conn, "kernel mode");
//			_config_threadedKernelMode = "threaded".equals(kernelMode);
//		}
//
//		
//		// in version 15.0.3.1 compatibility_mode was introduced, this to use 12.5.4 optimizer & exec engine
//		// This will hurt performance, especially when querying sysmonitors table, so set this to off
////		if (srvVersion >= 15031)
////		if (srvVersion >= 1503010)
//		if (srvVersion >= Ver.ver(15,0,3,1))
//			AseConnectionUtils.setCompatibilityMode(conn, false);
//
//		// initialize all the CM's
//		Iterator<CountersModel> i = _CMList.iterator();
//		while (i.hasNext())
//		{
//			CountersModel cm = i.next();
//
//			if (cm != null)
//			{
//				_logger.debug("Initializing CM named '"+cm.getName()+"', display name '"+cm.getDisplayName()+"', using ASE server version number "+srvVersion+".");
//
//				// set the version
////				cm.setServerVersion(srvVersion);
//				cm.setServerVersion(monTablesVersion);
//				cm.setClusterEnabled(isClusterEnabled);
//				
//				// set the active roles, so it can be used in initSql()
//				cm.setActiveRoles(_activeRoleList);
//
//				// set the ASE Monitor Configuration, so it can be used in initSql() and elsewhere
//				cm.setMonitorConfigs(monitorConfigMap);
//
//				// Now when we are connected to a server, and properties are set in the CM, 
//				// mark it as runtime initialized (or late initialization)
//				// do NOT do this at the end of this method, because some of the above stuff
//				// will be used by the below method calls.
//				cm.setRuntimeInitialized(true);
//
//				// Initializes SQL, use getServerVersion to check what we are connected to.
//				cm.initSql(conn);
//
//				// Use this method if we need to check anything in the database.
//				// for example "deadlock pipe" may not be active...
//				// If server version is below 15.0.2 statement cache info should not be VISABLE
//				cm.init(conn);
//				
//				// Initialize graphs for the version you just connected to
//				// This will simply enable/disable graphs that should not be visible for the ASE version we just connected to
//				cm.initTrendGraphForVersion(monTablesVersion);
//			}
//		}
//
//		// Make some specific Cluster settings
//		if (isClusterEnabled)
//		{
//			int systemView = AseConnectionUtils.CE_SYSTEM_VIEW_UNKNOWN;
//
//			if (AseTune.hasGUI())
//			{
////				systemView = SummaryPanel.getInstance().getClusterView();
//				systemView = CounterController.getSummaryPanel().getClusterView();
//			}
//			else
//			{
//				String clusterView = "cluster";
//
//				// hmmm would this be Configuration.PCS or Configuration.CONF, lets try PCS
//				Configuration conf = Configuration.getInstance(Configuration.PCS);
//				if (conf != null)
//					clusterView = conf.getProperty("cluster.system_view", clusterView);
//
//				if (clusterView.equalsIgnoreCase("instance"))
//					systemView = AseConnectionUtils.CE_SYSTEM_VIEW_INSTANCE;
//				else
//					systemView = AseConnectionUtils.CE_SYSTEM_VIEW_CLUSTER;
//			}
//
//			// make the setting
//			AseConnectionUtils.setClusterEditionSystemView(conn, systemView);
//		}
//			
//		_isInitialized = true;
//	}

//	/**
//	 *
//	 * In some versions we need to do some checks before we refresh the counters
//	 * <ul>
//	 * <li> 15.5, 15.0.3 ESD#1, 15.0.3.ESD#2, 15.0.3.ESD#3<br>
//	 *   if: sp_configure 'capture missing statistics' is true, then the sysstatistics table in
//	 *       the master database will be updated on mon* tables, so the transaction log might get full<br>
//	 *   <br>
//	 *   CR# 581760:<br>
// 	 *   "capture missing statistics" will capture statistics for MDA Tables<br>
//	 *   http://www-dse/cgi-bin/websql/websql.dir/QTS/bugsheet.hts?GO=GO&bugid=581760<br>
//	 * </li>
//	 * </ul>
//	 * 
//	 * @param conn a Connection to the database
//	 */
//	public void checkForFullTransLogInMaster(Connection conn)
//	{
//		if (conn == null)
//			return;
//
//		//----------------------
//		// In some versions we need to do some checks before we refresh the counters
//		// - 15.0.3 ESD#1, 15.0.3.ESD#2, 15.0.3.ESD#3 (fix is estimated for the 'bharani' release)
//		//   if: sp_configure 'capture missing statistics' is true, then the sysstatistics table in
//		//       the master database will be updated on mon* tables, so the transaction log might get full
//		// CR# 581760
//		// "capture missing statistics" will capture statistics for MDA Tables
//		// http://www-dse/cgi-bin/websql/websql.dir/QTS/bugsheet.hts?GO=GO&bugid=581760
//		//----------------------
//		try
//		{
//			long srvVersion = 0;
//			if (MonTablesDictionary.hasInstance())
//				srvVersion = MonTablesDictionary.getInstance().getAseExecutableVersionNum();
////				srvVersion = MonTablesDictionary.getInstance().srvVersionNum;
//
////			if (    srvVersion >= 15031 && srvVersion <= 15033
////				 || srvVersion >= 15500 && srvVersion <= 15501
////			   )
////			if ( srvVersion >= 15031 && srvVersion < 16000 )
////			if ( srvVersion >= 1503010 && srvVersion < 1600000 )
//			if ( srvVersion >= Ver.ver(15,0,3,1) && srvVersion < Ver.ver(16,0) )
//			{
//				if (_config_captureMissingStatistics || _config_enableMetricsCapture)
//				{
//					_logger.debug("Checking for full transaction log in the master database.");
//	
//					String sql = "select 'isLoggFull' = lct_admin('logfull', db_id('master'))," +
//					             "       'spaceLeft'  = lct_admin('logsegment_freepages', db_id('master')) " +      // Describes the free space available for a database
//					             "                    - lct_admin('reserve', 0) " +                                 // Returns the current last-chance threshold of the transaction log in the database from which the command was issued
//					             "                    - lct_admin('reserved_for_rollbacks', db_id('master'), 0) " + // Determines the number of pages reserved for rollbacks in the master database
//					             "                    - @@thresh_hysteresis";                                       // Take away hysteresis
//	
//					int isLogFull = 0;
//					int spaceLeft = 0;
//	
//					Statement stmt = conn.createStatement();
//					ResultSet rs = stmt.executeQuery(sql);
//					while (rs.next())
//					{
//						isLogFull   = rs.getInt(1);
//						spaceLeft   = rs.getInt(2);
//					}
//					rs.close();
//					stmt.close();
//	
//					_logger.debug("Checking for full transaction log in the master database. Results: isLogFull='"+isLogFull+"', spaceLeft='"+spaceLeft+"'.");
//	
//					if (isLogFull > 0  ||  spaceLeft <= 50) // 50 pages, is 100K in a 2K ASE (well int's NOT MB at least)
//					{
//						_logger.warn("Truncating the transaction log in the master database. Issuing SQL 'dump tran master with truncate_only'. isLogFull='"+isLogFull+"', spaceLeftInPages='"+spaceLeft+"'.");
//						stmt = conn.createStatement();
//						stmt.execute("dump tran master with truncate_only");
//						stmt.close();
//						
//						if (AseTune.hasGUI())
//						{
//							String dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());
//
//							JOptionPane optionPane = new JOptionPane(
//									"<html>" +
//									"Transaction log in the master database has been <b>truncated</b>. <br>" +
//									"<br>" +
//									"Space left in the master database transaction log was: spaceLeftInPages='"+spaceLeft+"'.<br>" +
//									"If the transaction log in the master database becomes full, the system <i>could</i> <b>halt</b>...<br>" +
//									"<br>" +
//									"SQL Issued: <code>dump tran master with truncate_only</code><br>" +
//									"</html>",
//									JOptionPane.INFORMATION_MESSAGE);
//							JDialog dialog = optionPane.createDialog(MainFrame.getInstance(), "ASE master transaction log was truncated @ "+dateStr);
//							dialog.setModal(false);
//							dialog.setVisible(true);
//						}
//					}
//				}
//			}
//		}
//		catch (SQLException e)
//		{
//			_logger.warn("When checking for a full transaction log in master, we got problem, but skipping this problem and continuing.", e);
//		}
//	}

	
	/**
	 * This will be used to create all the CountersModel objects.
	 * <p>
	 * If we are in GUI mode the graphical objects would also be 
	 * created and added to the GUI system, this so they will be showned 
	 * in the GUI before we are connected to any ASE Server.
	 *  <p>
	 * initCounters() would be called on "connect" to be able to 
	 * initialize the counter object using a specific ASE release
	 * this so we can decide what monitor tables and columns we could use.
	 */
	@Override
	abstract public void createCounters(boolean hasGui);

//	public void createCounters(boolean hasGui);
//	{
//		if (_countersIsCreated)
//			return;
//
//		_logger.info("Creating ALL CM Objects.");
//
//		ICounterController counterController = this;
//		MainFrame          guiController     = hasGui ? MainFrame.getInstance() : null;
//
//		CmSummary          .create(counterController, guiController);
//
//		CmObjectActivity   .create(counterController, guiController);
//		CmProcessActivity  .create(counterController, guiController);
//		CmSpidWait         .create(counterController, guiController);
//		CmSpidCpuWait      .create(counterController, guiController);
//		CmOpenDatabases    .create(counterController, guiController);
//		CmTempdbActivity   .create(counterController, guiController);
//		CmSysWaits         .create(counterController, guiController);
//		CmExecutionTime    .create(counterController, guiController);
//		CmEngines          .create(counterController, guiController);
//		CmThreads          .create(counterController, guiController);
//		CmSysLoad          .create(counterController, guiController);
//		CmDataCaches       .create(counterController, guiController);
//		CmCachePools       .create(counterController, guiController);
//		CmDeviceIo         .create(counterController, guiController);
//		CmIoQueueSum       .create(counterController, guiController);
//		CmIoQueue          .create(counterController, guiController);
//		CmIoControllers    .create(counterController, guiController);
//		CmSpinlockActivity .create(counterController, guiController);
//		CmSpinlockSum      .create(counterController, guiController);
//		CmSysmon           .create(counterController, guiController);
//		CmMemoryUsage      .create(counterController, guiController);
//		CmRaSenders        .create(counterController, guiController);
//		CmRaLogActivity    .create(counterController, guiController);
//		CmRaScanners       .create(counterController, guiController);
//		CmRaScannersTime   .create(counterController, guiController);
////		CmRaSqlActivity    .create(counterController, guiController);
////		CmRaSqlMisses      .create(counterController, guiController);
//		CmRaSysmon         .create(counterController, guiController);
//		CmCachedProcs      .create(counterController, guiController);
//		CmCachedProcsSum   .create(counterController, guiController);
//		CmProcCacheLoad    .create(counterController, guiController);
//		CmProcCallStack    .create(counterController, guiController);
//		CmCachedObjects    .create(counterController, guiController);
//		CmErrorLog         .create(counterController, guiController);
//		CmDeadlock         .create(counterController, guiController);
//		CmLockTimeout      .create(counterController, guiController);
//		CmPCacheModuleUsage.create(counterController, guiController);
//		CmPCacheMemoryUsage.create(counterController, guiController);
//		CmStatementCache   .create(counterController, guiController);
//		CmStmntCacheDetails.create(counterController, guiController);
//		CmActiveObjects    .create(counterController, guiController);
//		CmActiveStatements .create(counterController, guiController);
//		CmBlocking         .create(counterController, guiController);
//		CmTableCompression .create(counterController, guiController);
//		CmMissingStats     .create(counterController, guiController);
//		CmQpMetrics        .create(counterController, guiController);
//		CmSpMonitorConfig  .create(counterController, guiController);
//		CmWorkQueues       .create(counterController, guiController);
//
//		// OS HOST Monitoring
//		CmOsIostat         .create(counterController, guiController);
//		CmOsVmstat         .create(counterController, guiController);
//		CmOsMpstat         .create(counterController, guiController);
//		CmOsUptime         .create(counterController, guiController);
//
//		// USER DEFINED COUNTERS
//		createUserDefinedCounterModels();
//		createUserDefinedCounterModelHostMonitors();
//
//		// done
//		_countersIsCreated = true;
//	}

	/**
	 * A User Defined Counter
	 * <p>
	 * Can be defined from the properties file:
	 * <pre>
	 * #-----------------------------------------------------------
	 * # Go and get information about you'r own thing
	 * # Normally used to get a some application specific counters
	 * # If it's a "load" application how many X have we loaded so far
	 * #-----------------------------------------------------------
	 * udc.appSpecificCounter.name=App1 loading
	 * udc.appSpecificCounter.sql=select appName, loadCount=count(*) from appTable
	 * udc.appSpecificCounter.pkPos=1
	 * udc.appSpecificCounter.diff=loadCount
	 * 
	 * 
	 * #-----------------------------------------------------------
	 * # Here is a example where we are not satisfied with the current AseTune counters
	 * # so lets make "a new and improved" tab.
	 * #
	 * # In this example we are also using ASE version dependensies
	 * # meaning newer versions of the ASE server might have "extra" columns in there MDA tables
	 * #
	 * #
	 * # Here is a example where we are not satisfied with the current AseTune counters
	 * # so lets make "a new and improved" tab.
	 * #
	 * # In this example we are also using ASE version dependensies
	 * # meaning newer versions of the ASE server might have "extra" columns in there MDA tables
	 * #-----------------------------------------------------------
	 * udc.procAcct.name=EXTRA PROCESS ACTIVITY
	 * udc.procAcct.sql=      select SPID, KPID, CPUTime, LogicalReads, PhysicalReads       from monProcessActivity
	 * udc.procAcct.sql.12510=select SPID, KPID, CPUTime, LogicalReads, PhysicalReads, WaitTime       from monProcessActivity
	 * udc.procAcct.sql.12520=select SPID, KPID, CPUTime, LogicalReads, PhysicalReads, WaitTime, PagesRead       from monProcessActivity
	 * udc.procAcct.sql.12530=select SPID, KPID, CPUTime, LogicalReads, PhysicalReads, WaitTime, PagesRead, PhysicalWrites, PagesWritten       from monProcessActivity
	 * udc.procAcct.sql.12540=select SPID, KPID, CPUTime, LogicalReads, PhysicalReads, WaitTime, PagesRead, PhysicalWrites, PagesWritten, MemUsageKB       from monProcessActivity
	 * udc.procAcct.sql.15010=select SPID, KPID, CPUTime, LogicalReads, PhysicalReads, WaitTime, PagesRead, PhysicalWrites, PagesWritten, MemUsageKB, NewCol1=getdate()       from monProcessActivity
	 * udc.procAcct.sql.15020=select SPID, KPID, CPUTime, LogicalReads, PhysicalReads, WaitTime, PagesRead, PhysicalWrites, PagesWritten, MemUsageKB, NewCol1=getdate(), AndAotherNewCol=getdate()       from monProcessActivity
	 * udc.procAcct.pkPos=1, 2 
	 * udc.procAcct.diff=PhysicalReads, LogicalReads, PagesRead, PhysicalWrites, PagesWritten
	 * udc.procAcct.toolTipMonTables=monProcessActivity
	 * </pre>
	 * @param guiController 
	 * @param counterController 
	 */
	public void createUserDefinedCounterModels(ICounterController counterController, IGuiController guiController)
	{
		Configuration conf = null;
		if(DbxTune.hasGui())
		{
			conf = Configuration.getCombinedConfiguration();
		}
		else
		{
			conf = Configuration.getInstance(Configuration.PCS);
		}
		
		if (conf == null)
			return;

		createUserDefinedCounterModels(counterController, guiController, conf);
	}

	public static int createUserDefinedCounterModels(Configuration conf)
	{
		// FIXME: when we have multiple flavors of AseTune (RepServer, IQ, HANA, MSSQL, etc): we probably can't trust this
//		ICounterController counterController = GetCounters.getInstance();
		ICounterController counterController = CounterController.getInstance();
		IGuiController     guiController     = Configuration.hasGui() ? MainFrame.getInstance() : null;

		return createUserDefinedCounterModels(counterController, guiController, conf);
	}


	public static int createUserDefinedCounterModels(ICounterController counterController, IGuiController guiController, Configuration conf)
	{
		if (conf == null)
			throw new IllegalArgumentException("The passed Configuration can't be null");
			//return 1;

		int failCount = 0;

		String prefix = "udc.";

		for (String name : conf.getUniqueSubKeys(prefix, false))
		{
//			if (getInstance().getCmByName(name) != null)
			if (CounterController.getInstance().getCmByName(name) != null)
			{
				_logger.info("Already loaded the UDC named '"+name+"', skipping this and continue with next one.");
				continue;
			}
			SplashWindow.drawProgress("Loading: User Defined Counter Model '"+name+"'");
			
			String startKey = prefix + name + ".";
			
			Map<Integer, String> sqlVer = null;

			_logger.info("Loading/Initializing User Defined Counter '"+name+"'.");

			// Get the individual properties
			final String  udcName          = conf.getProperty(startKey    + "name");
			final String  udcDisplayName   = conf.getProperty(startKey    + "displayName", udcName);
			final String  udcDescription   = conf.getProperty(startKey    + "description", "");
			final String  udcSqlInit       = conf.getProperty(startKey    + "sqlInit");
			final String  udcSqlClose      = conf.getProperty(startKey    + "sqlClose");
			final String  udcSql           = conf.getProperty(startKey    + "sql");
			final String  udcPk            = conf.getProperty(startKey    + "pk");
			final String  udcPkPos         = conf.getProperty(startKey    + "pkPos");
			final String  udcDiff          = conf.getProperty(startKey    + "diff");
			final boolean udcNDCT0  = conf.getBooleanProperty(startKey    + "negativeDiffCountersToZero", false);
			final String  udcNeedRole      = conf.getProperty(startKey    + "needRole");
			final String  udcNeedConfig    = conf.getProperty(startKey    + "needConfig");
			final int     udcNeedVersion   = conf.getIntProperty(startKey + "needVersion", 0);
			final int     udcNeedCeVersion = conf.getIntProperty(startKey + "needCeVersion", 0);
			final String  udcTTMT          = conf.getProperty(startKey    + "toolTipMonTables");

			// The below values are read in method: addUdcGraph()
//			int     udcGraphType        = -1;
//			boolean udcHasGraph  = conf.getBooleanProperty(startKey + "graph", false);
//			String  udcGraphTypeStr     = conf.getProperty(startKey + "graph.type", "byCol");
//			String  udcGraphName        = conf.getProperty(startKey + "graph.name");
//			String  udcGraphLabel       = conf.getProperty(startKey + "graph.label");
//			String  udcGraphMenuLabel   = conf.getProperty(startKey + "graph.menuLabel");
//			String  udcGraphDataCols    = conf.getProperty(startKey + "graph.data.cols");
//			String  udcGraphDataMethods = conf.getProperty(startKey + "graph.data.methods");
//			String  udcGraphDataLabels  = conf.getProperty(startKey + "graph.data.labels");

			// CHECK for mandatory properties
			if (udcName == null)
			{
				_logger.error("Can't initialize User Defined Counter '"+name+"', no 'name' has been defined.");
				failCount++;
				continue;
			}
			if (udcSql == null)
			{
				_logger.error("Can't initialize User Defined Counter '"+name+"', no 'sql' has been defined.");
				failCount++;
				continue;
			}
			if (udcPkPos != null)
			{
				_logger.error("Can't initialize User Defined Counter '"+name+"', 'pkPos' are not longer supported, please use 'pk' instead.");
				failCount++;
				continue;
			}

			if (udcPk == null)
			{
				_logger.error("Can't initialize User Defined Counter '"+name+"', no 'pk' has been defined.");
				failCount++;
				continue;
			}

			// Check/get version specific SQL strings
			String sqlVersionPrefix = startKey + "sql" + ".";
			//int     sqlVersionHigh = -1;
			for (String key : conf.getKeys(sqlVersionPrefix))
			{
				String sqlVersionStr = key.substring(sqlVersionPrefix.length());
				int    sqlVersionNumInKey = 0;

				try
				{
					sqlVersionNumInKey = Integer.parseInt(sqlVersionStr);
				}
				catch(NumberFormatException ex)
				{
					_logger.warn("Problems initialize User Defined Counter '"+name+"', a sql.##### where ##### should specify ASE server version if faulty in the string '"+sqlVersionStr+"'.");
					failCount++;
					continue;
				}
				
				// Add all the udName.sql.#VERSION# to the map.
				// we will descide what version to use later on.
				if (sqlVer == null)
					sqlVer = new HashMap<Integer, String>();

				sqlVer.put(
					new Integer(sqlVersionNumInKey), 
					conf.getProperty(sqlVersionPrefix + sqlVersionNumInKey) );
			}

			// Get me some array variables, that we will "spit" properties into
			List<String>     udcPkList          = new LinkedList<String>();
			String[] udcPkArray         = {};
			String[] udcDiffArray       = {};
			String[] udcTTMTArray       = {};
			String[] udcNeedRoleArray   = {};
			String[] udcNeedConfigArray = {};
			String[] udcPctArray        = {}; // not used, just initialized

			// Split some properties using commas "," as the delimiter  
			if (udcPk         != null) udcPkArray          = udcPk        .split(",");
			if (udcDiff       != null) udcDiffArray        = udcDiff      .split(",");
			if (udcTTMT       != null) udcTTMTArray        = udcTTMT      .split(",");
			if (udcNeedRole   != null) udcNeedRoleArray    = udcNeedRole  .split(",");
			if (udcNeedConfig != null) udcNeedConfigArray  = udcNeedConfig.split(",");

			

			
			// Get rid of extra " " spaces from the split 
			for (int i=0; i<udcPkArray        .length; i++) udcPkArray        [i] = udcPkArray        [i].trim(); 
			for (int i=0; i<udcDiffArray      .length; i++) udcDiffArray      [i] = udcDiffArray      [i].trim(); 
			for (int i=0; i<udcTTMTArray      .length; i++) udcTTMTArray      [i] = udcTTMTArray      [i].trim(); 
			for (int i=0; i<udcNeedRoleArray  .length; i++) udcNeedRoleArray  [i] = udcNeedRoleArray  [i].trim(); 
			for (int i=0; i<udcNeedConfigArray.length; i++) udcNeedConfigArray[i] = udcNeedConfigArray[i].trim(); 

			for (int i=0; i<udcPkArray   .length; i++) udcPkList.add( udcPkArray[i] ); 

			_logger.info("Creating User Defined Counter '"+name+"' with sql '"+udcSql+"'.");
			if (guiController != null && guiController.hasGUI())
				guiController.splashWindowProgress("Loading: UD Counter Model '"+name+"'");

			// Finally create the Counter model and all it's surroundings...
			@SuppressWarnings("serial")
			CountersModel cm = new CountersModelUserDefined(counterController, name, MainFrame.TCP_GROUP_UDC, udcSql, sqlVer,
					udcPkList, //pk1, pk2, pk3, 
					udcDiffArray, udcPctArray, 
					udcTTMTArray, udcNeedRoleArray, udcNeedConfigArray, 
					udcNeedVersion, udcNeedCeVersion, 
					udcNDCT0)
			{
				@Override
				protected JPanel createGui()
				{
					TabularCntrPanel tcp = new TabularCntrPanel(this);

					tcp.setToolTipText( udcDescription );
					tcp.setIcon( SwingUtils.readImageIcon(Version.class, "images/ud_counter_activity.png") );

					return tcp;
				}
			};

			cm.setSqlInit(udcSqlInit);
			cm.setSqlClose(udcSqlClose);
			cm.setDisplayName(udcDisplayName);
			cm.setDescription(udcDescription);

			// The below will register/add itself to the Counter Controller and call createGui if we have a GUI
			cm.setCounterController(counterController);
			cm.setGuiController(guiController);


			//
			// User defined graphs, if any
			//
			addUdcGraph(cm, udcName, startKey, conf);

			// Register at the template 
			CounterSetTemplates.register(cm);

//			_CMList.add(cm); // this is done in setCounterController()
		}

		return failCount;
	}

	private static void addUdcGraph(CountersModel cm, String udcName, String startKey, Configuration conf)
	{
		int     udcGraphType        = -1;
		boolean udcHasGraph  = conf.getBooleanProperty(startKey + "graph", false);
		String  udcGraphTypeStr     = conf.getProperty(startKey + "graph.type", "byCol");
		String  udcGraphName        = conf.getProperty(startKey + "graph.name");
		String  udcGraphLabel       = conf.getProperty(startKey + "graph.label");
		String  udcGraphProps       = conf.getProperty(startKey + "graph.props");
//		String  udcGraphCategory    = conf.getProperty(startKey + "graph.category");
		String  udcGraphMenuLabel   = conf.getProperty(startKey + "graph.menuLabel");
		String  udcGraphDataCols    = conf.getProperty(startKey + "graph.data.cols");
		String  udcGraphDataMethods = conf.getProperty(startKey + "graph.data.methods");
		String  udcGraphDataLabels  = conf.getProperty(startKey + "graph.data.labels");

		if ( ! udcHasGraph )
			return;

		String name = udcName;
		boolean addGraph = true;

		if (udcGraphName == null)
			udcGraphName = udcName + "Graph";

		if (udcGraphLabel == null)
			udcGraphLabel = udcName + " Graph";

		if (udcGraphMenuLabel == null)
			udcGraphMenuLabel = udcGraphLabel;

		if (udcGraphDataCols == null)
		{
			_logger.error("Can't add a graph to the User Defined Counter '"+name+"', no 'graph.data.cols' has been defined.");
			addGraph = false;
		}
		if (udcGraphDataMethods == null)
		{
			_logger.error("Can't add a graph to the User Defined Counter '"+name+"', no 'graph.data.methods' has been defined.");
			addGraph = false;
		}
		if (udcGraphDataLabels == null)
			udcGraphDataLabels = udcGraphDataCols;

		if      (udcGraphTypeStr.equalsIgnoreCase("byCol")) udcGraphType = TrendGraph.TYPE_BY_COL;
		else if (udcGraphTypeStr.equalsIgnoreCase("byRow"))	udcGraphType = TrendGraph.TYPE_BY_ROW;
		else
		{
			_logger.error("Can't add a graph to the User Defined Counter '"+name+"', no 'graph.type' can only be 'byCol' or 'byRow'.");
			addGraph = false;
		}

		String[] udcGraphDataColsArr    = {};
		String[] udcGraphDataMethodsArr = {};
		String[] udcGraphDataLabelsArr  = {};

		// Split some properties using commas "," as the delimiter  
		if (udcGraphDataCols    != null) udcGraphDataColsArr    = udcGraphDataCols   .split(",");
		if (udcGraphDataMethods != null) udcGraphDataMethodsArr = udcGraphDataMethods.split(",");
		if (udcGraphDataLabels  != null) udcGraphDataLabelsArr  = udcGraphDataLabels .split(",");

		// Get rid of extra " " spaces from the split 
		for (int i=0; i<udcGraphDataColsArr   .length; i++) udcGraphDataColsArr   [i] = udcGraphDataColsArr   [i].trim(); 
		for (int i=0; i<udcGraphDataMethodsArr.length; i++) udcGraphDataMethodsArr[i] = udcGraphDataMethodsArr[i].trim(); 
		for (int i=0; i<udcGraphDataLabelsArr .length; i++) udcGraphDataLabelsArr [i] = udcGraphDataLabelsArr [i].trim(); 

		if (udcGraphDataColsArr.length != udcGraphDataMethodsArr.length)
		{
			_logger.error("Can't add a graph to the User Defined Counter '"+name+"'. 'graph.data.cols' has "+udcGraphDataColsArr.length+" entries while 'graph.data.methods' has "+udcGraphDataMethodsArr.length+" entries, they has to be equal.");
			addGraph = false;
		}
		if (udcGraphDataColsArr.length != udcGraphDataLabelsArr.length)
		{
			_logger.error("Can't add a graph to the User Defined Counter '"+name+"'. 'graph.data.cols' has "+udcGraphDataColsArr.length+" entries while 'graph.data.labels' has "+udcGraphDataLabelsArr.length+" entries, they has to be equal.");
			addGraph = false;
		}

		for (int i=0; i<udcGraphDataMethodsArr.length; i++) 
		{
			if ( ! CountersModel.isValidGraphMethod(udcGraphDataMethodsArr[i], true))
			{
				_logger.error("Can't add a graph to the User Defined Counter '"+name+"'. 'The graph method '"+udcGraphDataMethodsArr[i]+"' is unknown.");
				_logger.error("Valid method names is: "+CountersModel.getValidGraphMethodsString(true));
				addGraph = false;
			}
		}
		
		LabelType udcGraphSeriesLabelType = LabelType.Static;
		if (udcGraphType == TrendGraph.TYPE_BY_ROW)
		{
			udcGraphSeriesLabelType = LabelType.Dynamic;
			if (udcGraphDataColsArr.length > 1)
			{
				_logger.warn("Add a graph using type 'byRow' to the User Defined Counter '"+name+"'. Only the first entry in 'graph.data.cols', 'graph.data.labels', 'graph.data.methods' will be used");
			}
		}

		if (addGraph)
		{
			cm.addTrendGraph(
				udcGraphName,           // Name of the graph
				udcGraphMenuLabel,      // Menu Checkbox text
				udcGraphLabel,          // udcGraphLabel
				udcGraphProps,          // udcGraphProps
				udcGraphDataLabelsArr,  // Labels for each plotline
				udcGraphSeriesLabelType,// seriesLabelType             LabelType.Static or LabelType.Dynamic
				TrendGraphDataPoint.Category.OTHER,
				false,                  // isPercentGraph, 
				true,                   // visibleAtStart
				0,                      // needsVersion, 
				-1);                    // minimumHeight

			// Trend Graph (extras for UserDefined)
			if (DbxTune.hasGui())
			{
    			TrendGraph tg = cm.getTrendGraph(udcGraphName);
    			tg.setGraphType(udcGraphType);
    			tg.setGraphCalculations(udcGraphDataColsArr, udcGraphDataMethodsArr);
			}
			
			// Data Point (extras for UserDefined)
			cm.setGraphType(udcGraphType);
			cm.setGraphCalculations(udcGraphDataColsArr, udcGraphDataMethodsArr);

			
//			if (DbxTune.hasGui())
//			{
//				// GRAPH
//				TrendGraph tg = new TrendGraph(
//						udcGraphName,      // Name of the raph
//						udcGraphMenuLabel, // Menu Checkbox text
//						udcGraphLabel,     // Label on the graph
//						udcGraphDataLabelsArr, // Labels for each plotline
//						false,
//						cm, 
//						true, // initial visible
//						0,    // valid from version
//						-1);
//				tg.setGraphType(udcGraphType);
//				tg.setGraphCalculations(udcGraphDataColsArr, udcGraphDataMethodsArr);
//				cm.addTrendGraph(udcGraphName, tg, true);
//			}
//			
//			// Data Point
//			cm.setGraphType(udcGraphType);
//			cm.setGraphCalculations(udcGraphDataColsArr, udcGraphDataMethodsArr);
//			cm.addTrendGraphData(udcGraphName, new TrendGraphDataPoint(udcGraphName, udcGraphDataLabelsArr, LabelType.Static));
		}
	}
	
	
	/**
	 * 
	 */
	public void createUserDefinedCounterModelHostMonitors(ICounterController counterController, IGuiController guiController)
	{
		Configuration conf = null;
		if(DbxTune.hasGui())
		{
//			conf = Configuration.getInstance(Configuration.CONF);
			conf = Configuration.getCombinedConfiguration();
		}
		else
		{
			conf = Configuration.getInstance(Configuration.PCS);
		}
		
		if (conf == null)
			return;

		createUserDefinedCounterModelHostMonitors(counterController, guiController, conf);
	}

	/**
	 * 
	 * @param conf
	 * @return
	 */
	public static int createUserDefinedCounterModelHostMonitors(ICounterController counterController, IGuiController guiController, Configuration conf)
	{
		if (conf == null)
			throw new IllegalArgumentException("The passed Configuration can't be null");
			//return 1;

		int failCount = 0;

		String prefix = "hostmon.udc.";

		for (String name : conf.getUniqueSubKeys(prefix, false))
		{
			SplashWindow.drawProgress("Loading: Host Monitor User Defined Counter '"+name+"'");
			
			String startKey = prefix + name + ".";

			_logger.debug("STARTING TO Initializing Host Monitor User Defined Counter '"+name+"'.");

//			final String  udcName          = conf.getProperty(startKey    + "name");
//			final String  udcDisplayName   = conf.getProperty(startKey    + "displayName", udcName);
			final String  udcDisplayName   = conf.getProperty(startKey    + "displayName", name);
			final String  udcDescription   = conf.getProperty(startKey    + "description", "");

			// The below values are read in method: addUdcGraph()
//			final boolean udcHasGraph  = conf.getBooleanProperty(startKey + "graph", false);
//			final String  udcGraphTypeStr     = conf.getProperty(startKey + "graph.type", "byCol");
//			final String  udcGraphName        = conf.getProperty(startKey + "graph.name");
//			final String  udcGraphLabel       = conf.getProperty(startKey + "graph.label");
//			final String  udcGraphMenuLabel   = conf.getProperty(startKey + "graph.menuLabel");
//			final String  udcGraphDataCols    = conf.getProperty(startKey + "graph.data.cols");
//			final String  udcGraphDataMethods = conf.getProperty(startKey + "graph.data.methods");
//			final String  udcGraphDataLabels  = conf.getProperty(startKey + "graph.data.labels");

			_logger.info("Creating User Defined Host Monitor Counter '"+name+"'.");
			if (guiController != null && guiController.hasGUI())
				guiController.splashWindowProgress("Loading: UD Host Counter Model '"+name+"'");

			
			boolean negativeDiffCountersToZero = true;
			boolean systemCm = false;
			int     defaultPostponeTime = 0;

			@SuppressWarnings("serial")
			CountersModel cm = new CounterModelHostMonitor(name, MainFrame.TCP_GROUP_HOST_MONITOR, CounterModelHostMonitor.HOSTMON_UD_CLASS, name, negativeDiffCountersToZero, systemCm, defaultPostponeTime)
			{
				@Override
				protected JPanel createGui()
				{
					TabularCntrPanel tcp = new TabularCntrPanel(this)
					{
						private static final long	serialVersionUID	= 1L;

						JLabel  l_hostmonThreadNotInit_lbl;
						JLabel  l_hostmonThreadIsRunning_lbl;
						JLabel  l_hostmonThreadIsStopped_lbl;
						JButton l_hostmonStart_but;
						JButton l_hostmonStop_but;

						@Override
						protected JPanel createLocalOptionsPanel()
						{
							JPanel panel = SwingUtils.createPanel("Host Monitor", true);
							panel.setLayout(new MigLayout("ins 5, gap 0", "", "0[0]0"));
							panel.setToolTipText(
								"<html>" +
									"Use this panel to check or controll the underlying Host Monitoring Thread.<br>" +
									"You can Start and/or Stop the hostmon thread.<br>" +
								"</html>");

							l_hostmonThreadNotInit_lbl    = new JLabel("<html><b>Not yet initialized</b></html>");
							l_hostmonThreadIsRunning_lbl  = new JLabel("<html>Is running</html>");
							l_hostmonThreadIsStopped_lbl  = new JLabel("<html><b>Is stopped</b></html>");
							l_hostmonStart_but            = new JButton("Start");
							l_hostmonStop_but             = new JButton("Stop");

							l_hostmonThreadNotInit_lbl  .setToolTipText("<html>Indicates wheather the underlying Host Monitor Thread has not yet been initialized.</html>");
							l_hostmonThreadIsRunning_lbl.setToolTipText("<html>Indicates wheather the underlying Host Monitor Thread is running.</html>");
							l_hostmonThreadIsStopped_lbl.setToolTipText("<html>Indicates wheather the underlying Host Monitor Thread is running.</html>");
							l_hostmonStart_but          .setToolTipText("<html>Start the underlying Host Monitor Thread.</html>");
							l_hostmonStop_but           .setToolTipText("<html>Stop the underlying Host Monitor Thread.</html>");

							l_hostmonThreadNotInit_lbl  .setVisible(true);
							l_hostmonThreadIsRunning_lbl.setVisible(false);
							l_hostmonThreadIsStopped_lbl.setVisible(false);
							l_hostmonStart_but          .setVisible(false);
							l_hostmonStop_but           .setVisible(false);

							panel.add( l_hostmonThreadNotInit_lbl,   "hidemode 3, wrap 10");
							panel.add( l_hostmonThreadIsRunning_lbl, "hidemode 3, wrap 10");
							panel.add( l_hostmonThreadIsStopped_lbl, "hidemode 3, wrap 10");
							panel.add( l_hostmonStart_but,           "hidemode 3, wrap");
							panel.add( l_hostmonStop_but,            "hidemode 3, wrap");

							l_hostmonStart_but.addActionListener(new ActionListener()
							{
								@Override
								public void actionPerformed(ActionEvent e)
								{
									CountersModel cm = getCm();
									if (cm != null)
									{
										HostMonitor hostMonitor = (HostMonitor) cm.getClientProperty(HostMonitor.PROPERTY_NAME);
										if (hostMonitor != null)
										{
											try
											{
												hostMonitor.setPaused(false);
												hostMonitor.start();
											}
											catch (Exception ex)
											{
												SwingUtils.showErrorMessage("Start", "Problems Starting the Host Monitoring Thread.", ex);
											}
										}
									}
								}
							});

							l_hostmonStop_but.addActionListener(new ActionListener()
							{
								@Override
								public void actionPerformed(ActionEvent e)
								{
									CountersModel cm = getCm();
									if (cm != null)
									{
										HostMonitor hostMonitor = (HostMonitor) cm.getClientProperty(HostMonitor.PROPERTY_NAME);
										if (hostMonitor != null)
										{
											hostMonitor.setPaused(true);
											hostMonitor.shutdown();
										}
									}
								}
							});
							return panel;
						}

						@Override
						public void checkLocalComponents()
						{
							CountersModel cm = getCm();
							if (cm != null)
							{
								HostMonitor hostMonitor = (HostMonitor) cm.getClientProperty(HostMonitor.PROPERTY_NAME);
								if (hostMonitor != null)
								{
									boolean isRunning = hostMonitor.isRunning();
									boolean isPaused  = hostMonitor.isPaused();

									l_hostmonThreadIsRunning_lbl.setText("<html>Command: <b>"+hostMonitor.getCommand()+"</b></html>");
									l_hostmonThreadNotInit_lbl  .setVisible( false );

									if ( hostMonitor.isOsCommandStreaming() )
									{
										l_hostmonThreadIsRunning_lbl.setVisible(   isRunning );
										l_hostmonThreadIsStopped_lbl.setVisible( ! isRunning );
										l_hostmonStart_but          .setVisible( ! isRunning );
										l_hostmonStop_but           .setVisible(   isRunning );
									}
									else
									{
										l_hostmonThreadIsRunning_lbl.setVisible( true );
										l_hostmonThreadIsStopped_lbl.setText("<html>This module has <b>no</b> background thread.<br>And it's executed on every <b>refresh</b>.</html>");
										l_hostmonThreadIsStopped_lbl.setVisible( true );

									//	l_hostmonThreadIsRunning_lbl.setVisible( false );
									//	l_hostmonThreadIsStopped_lbl.setVisible( false );
										l_hostmonStart_but          .setVisible( false );
										l_hostmonStop_but           .setVisible( false );
									}

									if (isPaused)
										setWatermarkText("Warning: The host monitoring thread is Stopped/Paused!");
								}
								else
								{
									setWatermarkText("Host Monitoring is Disabled or Initializing at Next sample.");
									l_hostmonThreadNotInit_lbl  .setVisible( true );
									l_hostmonThreadIsRunning_lbl.setVisible( false );
									l_hostmonThreadIsStopped_lbl.setVisible( false );
									l_hostmonStart_but          .setVisible( false );
									l_hostmonStop_but           .setVisible( false );
									if (cm.getSampleException() != null)
										setWatermarkText(cm.getSampleException().toString());
								}
							}
						}
					};

					tcp.setToolTipText( udcDescription );
					tcp.setIcon( SwingUtils.readImageIcon(Version.class, "images/hostmon_ud_counter_activity.png") );

					return tcp;
				}
			};


			cm.setDisplayName(udcDisplayName);
			cm.setDescription(udcDescription);

			// The below will register/add itself to the Counter Controller and call createGui if we have a GUI
			cm.setCounterController(counterController);
			cm.setGuiController(guiController);
			
			// Add a graph
			addUdcGraph(cm, name, startKey, conf);

			// Register at the template 
			CounterSetTemplates.register(cm);

			//_CMList.add(cm);
		}

		return failCount;
	}

	
	
	
	
	
	
	/**
	 * Interrupt the sleep and request a new refresh.<br>
	 * Note: this will be disregarded if we not sleeping waiting for a new refresh.
	 */
	@Override
	public void doRefresh()
	{
		if ( isRefreshing() )
			_logger.info("Sorry, can't do refresh now, we are already in a Performance Counter Refresh");
		else
			doInterrupt();
	}

	/**
	 * calls interrupt() on the refresh thread.<br>
	 * So if the refresh thread is asleap waiting for next refresh it will be interupted and
	 * start to do a new refresh of data.
	 */
	public void doInterrupt()
	{
		// interrupt the collector thread
//		if (_thread != null)
//		{
//			_logger.debug("Sending 'interrupt' to the thread '"+_thread.getName()+"', this was done by thread '"+Thread.currentThread().getName()+"'.");
//			_thread.interrupt();
//		}
		getCounterCollectorThread().doInterrupt();
	}

	private boolean _isSleeping = false;
	/** Check if we are sleeping by calling the method sleep() in this class. */
	@Override
	public boolean isSleeping()
	{
		return _isSleeping;
	}
	/** Set the sleeping flag */
	@Override
	public void setSleeping(boolean b)
	{
		_isSleeping = b;
	}
	/**
	 * Sleep for X ms, should only be used when GUI should be able to be interrupted.
	 * @param ms sleep time
	 * @return true if we were slept the whole time, false if we were interrupted.
	 */
	@Override
	public boolean sleep(int ms)
	{
		try 
		{
			_isSleeping = true;
			Thread.sleep(ms); 
			return true;
		}
		catch (InterruptedException ignore)
		{
			if (_logger.isDebugEnabled())
				_logger.debug("Thread '"+Thread.currentThread().getName()+"' was interrupted.", new Exception("Dummy exception, to get callstack from where this happened."));
			return false;
		}
		finally
		{
			_isSleeping = false;
		}
	}

	/** 
	 * Interrupt the thread if it's sleeping using the sleep() method in this class.<br>
	 * If Thread:sleep() has been used it wont do interrupts, just skipping your request.
	 */
	@Override
	public void doInterruptSleep()
	{
		if ( isSleeping() )
			doInterrupt();
		else
			_logger.info("Sorry, can't interrupt sleep now, because WE ARE NOT SLEEPING.");
	}
	
	/** enable/continue refreshing of monitor counters from the monitored server */
	@Override
	public void enableRefresh()
	{
		_refreshIsEnabled = true;

		// If we where in sleep at the getCounters loop
//		if (_thread != null)
//		{
//			_logger.debug("Sending 'interrupt' to the thread '"+_thread.getName()+"' if it was sleeping... This was done by thread '"+Thread.currentThread().getName()+"'.");
//			_thread.interrupt();
//		}
		doInterrupt();
	}

	/** pause/disable refreshing of monitor counters from the monitored server */
	@Override
	public void disableRefresh()
	{
		_refreshIsEnabled = false;
	}

	/** if we refreshing of the counters is enabled, or paused */
	@Override
	public boolean isRefreshEnabled()
	{
		return _refreshIsEnabled;
	}


	/**
	 * Are we currently getting counter information from the monitored server
	 * @return yes or no
	 */
	@Override
	public boolean isRefreshing()
	{
		return _isRefreshing;
	}

	/**
	 * Indicate the we are currently in the process of getting counter information from the monitored server
	 * @param true=isRefresing
	 */
	@Override
	public void setInRefresh(boolean s)
	{
		_isRefreshing = s;
		if (_isRefreshing)
		{
			_startRefreshTime = System.currentTimeMillis();
		}
		else
		{
			_refreshTimeInMs = System.currentTimeMillis() - _startRefreshTime;
		}
	}

	/** How many milliseconds did we spend in last refresh. diff between setInRefresh(true) -> setInRefresh(false) */
	@Override
	public long getLastRefreshTimeInMs()
	{
		return _refreshTimeInMs;
	}
	
//	public void clearComponents()
//	{
//		if ( ! _isInitialized )
//			return;
//
//		if (!isRefreshingCounters())
//		{
//			MainFrame.clearSummaryData();
//
//			Iterator<CountersModel> i = _CMList.iterator();
//			while (i.hasNext())
//			{
//				CountersModel cm = i.next();
//				
//				if (cm != null)
//				{
//					cm.clear();
//				}
//			}
//			
//			SummaryPanel.getInstance().clearGraph();
//
////			MainFrame.statusFld.setText("");
//		}
//	}

	
	////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////
	//// database connection
	////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////
//	private Connection         _conn                      = null;
	private DbxConnection      _conn                      = null;
	private long               _lastIsClosedCheck         = 0;
	private long               _lastIsClosedRefreshTime   = 2000;

	/** When did we do a connect last time */
	private Date _lastMonConnectTime = null;

	/** If controller want's to disconnect/stop collecting after a specific time */
	private Date _stopMonConnectTime = null;

//	/**
//	 * Override this if any implementers wont allow isClosed() check...<br>
//	 * For example in GUI mode the Event Dispath Thread we do not want to do it...<br>
//	 * NOTE: This is a BIG workaround, which should be implemented in another way
//	 * 
//	 * @return true if allowed
//	 */
//	protected boolean allowNonCachedIsClosedCheck()
//	{
//		return true;
//	}

	/**
	 * Do we have a connection to the database?<br>
	 * <b>NOTE:</b> Do NOT call the database to check it, just use the last information we got.
	 * The last status should be maintained everytime a physical check is done via isMonConnected().<br>
	 * On SQLExceptions, we should check if the database connection is still open/valid.
	 * <p>
	 * This is probably called from GUI places where we dont want a fast answer.
	 * @return true or false
	 */
	@Override
	public boolean isMonConnectedStatus()
	{
		if (_lastIsMonConnectedReturned < 0)
			isMonConnected();
		
		return _lastIsMonConnectedReturned > 0;
	}

	/**
	 * Do we have a connection to the database?
	 * @return true or false
	 */
	@Override
	public boolean isMonConnected()
	{
//		return isMonConnected(false, false);
		return isMonConnected(false, true);
	}
	/** remember the status of last call to isMonConnected() -1=notInitialized, 0=false, 1=true*/
	private int _lastIsMonConnectedReturned = -1;

	/**
	 * Do we have a connection to the database?
	 * @return true or false
	 */
	@Override
	public boolean isMonConnected(boolean forceConnectionCheck, boolean closeConnOnFailure)
	{
		if (_conn == null) 
		{
			_lastIsMonConnectedReturned = 0; //false;
			return false;
		}

		//DEBUG: System.out.print("isMonConnected(forceConnectionCheck="+forceConnectionCheck+", closeConnOnFailure="+closeConnOnFailure+")");
		// Cache the last call for X ms (default 1200 ms)
		if ( ! forceConnectionCheck )
		{
			long diff = System.currentTimeMillis() - _lastIsClosedCheck;
			if ( diff < _lastIsClosedRefreshTime)
			{
				//DEBUG: System.out.println("    <<--- isMonConnected(): not time for refresh. diff='"+diff+"', _lastIsClosedRefreshTime='"+_lastIsClosedRefreshTime+"'.");
				_lastIsMonConnectedReturned = 1; //true;
				return true;
			}

//			if ( ! allowNonCachedIsClosedCheck() )
//			{
//				if (_logger.isDebugEnabled())
//				{
////					long diff = System.currentTimeMillis() - _lastIsClosedCheck;
//					_logger.debug("isMonConnected(forceConnectionCheck="+forceConnectionCheck+", closeConnOnFailure="+closeConnOnFailure+") Skipping isClosed() towards the server. due to allowNonCachedIsClosedCheck() returned FALSE. Instead returning last known state, which was '"+_lastIsMonConnectedReturned+"', Last server check () was made "+TimeUtils.msToTimeStr(diff)+" ago, _lastIsClosedRefreshTime="+_lastIsClosedRefreshTime);
//				}
////	long diff = System.currentTimeMillis() - _lastIsClosedCheck;
//	System.out.println("isMonConnected(forceConnectionCheck="+forceConnectionCheck+", closeConnOnFailure="+closeConnOnFailure+") <--return-("+_lastIsMonConnectedReturned+"). Skipping isClosed() towards the server. due to allowNonCachedIsClosedCheck() returned FALSE. Instead returning last known state, which was '"+_lastIsMonConnectedReturned+"', Last server check () was made "+TimeUtils.msToTimeStr(diff)+" ago, _lastIsClosedRefreshTime="+_lastIsClosedRefreshTime);
//				return _lastIsMonConnectedReturned;
//			}

		}

		// check the connection itself
		try
		{
			//DEBUG: System.out.println("    DO: isClosed");
			// jConnect issues RPC: sp_mda 0, 7 on isClosed()
//			if (_conn.isClosed()) // hmmm this might block if done from 2 places at the same time... which happend when SQL Timeout occurs.
			if (isClosed(_conn))  // so lets use our own implementation... which does 'select 1' with a timeout
			{
				if (closeConnOnFailure)
					closeMonConnection();

				_lastIsMonConnectedReturned = 0; //false;
				return false;
			}
		}
		catch (SQLException e)
		{
			_lastIsMonConnectedReturned = 0; //false;
			return false;
		}

		_lastIsClosedCheck = System.currentTimeMillis();
		_lastIsMonConnectedReturned = 1; //true;
		return true;
	}
	// Simulate the _conn.isClosed() functionality, but add a query timeout...
	// it looks like jConnect isClosed() could hang if you call many simultaneously
	protected boolean isClosed(DbxConnection conn)
	throws SQLException
	{
		int       timeout = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_isClosed_timeout, DEFAULT_isClosed_timeout);
		String    sql     =  getIsClosedSql();
//		Statement stmnt   = null;
//		ResultSet rs      = null;

		try ( Statement stmnt = conn.createStatement() )
		{
//System.out.println("isClosed(): autoCommit="+conn.getAutoCommit()+", sql="+sql);
			
			if (_logger.isDebugEnabled())
				_logger.debug("isClosed(): sql="+sql);
			
//			stmnt = conn.createStatement();
			stmnt.setQueryTimeout(timeout);

			ResultSet rs = stmnt.executeQuery(sql);

			while (rs.next())
			{
				rs.getString(1);
			}
			rs.close();
//			stmnt.close();

//			if (conn.isDatabaseProduct(DbUtils.DB_PROD_NAME_ORACLE))
//			{
//				String oracleCheck = "select value from V$SESSTAT where STATISTIC# = 4 and SID = sys_context('USERENV','SID')";
//				rs = stmnt.executeQuery(oracleCheck);
//				while (rs.next())
//					System.out.println("isClosed["+Thread.currentThread().getName()+"]:CheckOpenCursorCount="+rs.getInt(1));
//				rs.close();
//			}

			// false = connection is alive, NOT Closed
			return false;
		}
		catch (SQLException ex)
		{
//e.printStackTrace();
			if (    ex instanceof SQLRecoverableException                                    // Oracle   - java.sql.SQLRecoverableException: Closed Connection
			     || "JZ0C0".equals(ex.getSQLState())                                         // jConnect - connection is already closed...
			     || ex.toString().toLowerCase().indexOf("connection is closed") >= 0         // DB2 & SQL-Server
			     || ex.toString().toLowerCase().indexOf("connection has been closed") >= 0   // Postgres
			   ) 
			{
				// Do nothing for known problems / Exceptions...
			}
			else
			{
				_logger.warn("isClosed(conn) had problems. sql='"+sql+"'.", ex);
			}

			throw ex;
		}
		catch(RuntimeException ex)
		{
			_logger.error("Problem in method: isClosed(), returning true and continuing. Caught: "+ex+".", ex);
			return true;
		}
//		finally
//		{
//			System.out.println("-------------------------------------------------------: "+Thread.currentThread().getName());
//			if (rs    != null) try { System.out.println("isClosed["+Thread.currentThread().getName()+"]("+(rsCloseCount++)+"):Closing ResultSet"); rs   .close(); } catch(SQLException ex) { ex.printStackTrace(); _logger.debug("Problems closing ResultSet", ex); }
////			if (stmnt != null) try { System.out.println("isClosed["+Thread.currentThread().getName()+"]("+(stCloseCount++)+"):Closing Statement"); stmnt.close(); } catch(SQLException ex) { ex.printStackTrace(); _logger.debug("Problems closing Statement", ex); }
//
////			if ("CounterCollectorThreadGui".equals(Thread.currentThread().getName()))
////				new Exception("DUMMY").printStackTrace();
//			
//			String oracleCheck = "select value from V$SESSTAT where STATISTIC# = 4 and SID = sys_context('USERENV','SID')";
//			Statement stmnt = conn.createStatement();
//			rs = stmnt.executeQuery(oracleCheck);
//			while (rs.next())
//				System.out.println("isClosed["+Thread.currentThread().getName()+"]:CheckOpenCursorCount="+rs.getInt(1));
//			if (rs    != null) try { rs   .close(); } catch(SQLException ex) { ex.printStackTrace(); _logger.debug("Problems closing ResultSet", ex); }
//			if (stmnt != null) try { stmnt.close(); } catch(SQLException ex) { ex.printStackTrace(); _logger.debug("Problems closing Statement", ex); }
//		}
	}
//	static int rsCloseCount=0;
//	static int stCloseCount=0;
	
	/**
	 * SQL Statement that will be issued when checking if a connection is alive
	 * @return
	 */
	protected abstract String getIsClosedSql();

	/**
	 * Does the Server support many SQL Statements in the same SQL Batch
	 * @return 
	 */
	@Override
	public boolean isSqlBatchingSupported()
	{
		return true;
	}

	/**
	 * get last connect time (or actually when setMonConnection() was called last time).
	 */
	@Override
	public Date getMonConnectionTime()
	{
		return _lastMonConnectTime;
	}

	@Override
	public void setMonDisConnectTime(Date time)
	{
		_stopMonConnectTime = time;
	}
	@Override
	public Date getMonDisConnectTime()
	{
		return _stopMonConnectTime;
	}

	/**
	 * Set the <code>Connection</code> to use for monitoring.
	 */
	@Override
//	public void setMonConnection(Connection conn)
	public void setMonConnection(DbxConnection conn)
	{
		_conn = conn;
		if (isMonConnected())
		{
			_lastMonConnectTime = new Date();
			onMonConnect(conn);
		}
	}

	/**
	 * Gets the <code>Connection</code> to the monitored server.
	 */
	@Override
//	public Connection getMonConnection()
	public DbxConnection getMonConnection()
	{
		return _conn;
	}

	/**
	 * After a Monitor Connection has been created, this is called so we can do various static settings on the newly created connection
	 * <p>
	 * This is called from {@link #setMonConnection(DbxConnection)}
	 */
	@Override
	public void onMonConnect(DbxConnection conn)
	{
	}

	/** Gets the <code>Connection</code> to the monitored server. */
	@Override
	public void closeMonConnection()
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
			_logger.error("closeMonConnection", ev);
		}

		cleanupMonConnection();
		_conn = null;
	}

	/** override this method to cleanup stuff on disconnect */
	@Override
	public void cleanupMonConnection()
	{
		getCounterCollectorThread().cleanupMonConnection();
	}
	
	
	////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////
	//// SSH connection
	////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////
//	private SshConnection      _sshConn                      = null;
	private HostMonitorConnection _hostMonConn               = null;
	private long               _sshLastIsClosedCheck         = 0;
	private long               _sshLastIsClosedRefreshTime   = 1200;

	/**
	 * Do we have a connection to the HOST?
	 * @return true or false
	 */
	@Override
	public boolean isHostMonConnected()
	{
		return isHostMonConnected(false, false);
	}
	/**
	 * Do we have a connection to the HOST?
	 * @return true or false
	 */
	public boolean isHostMonConnected(boolean forceConnectionCheck, boolean closeConnOnFailure)
	{
//		if (_sshConn == null) 
		if (_hostMonConn == null) 
			return false;

		// Cache the last call for X ms (default 1200 ms)
		if ( ! forceConnectionCheck )
		{
			long diff = System.currentTimeMillis() - _sshLastIsClosedCheck;
			if ( diff < _sshLastIsClosedRefreshTime)
			{
				return true;
			}
		}

		// check the connection itself
		try
		{
//			if (_sshConn.isClosed())
			if (_hostMonConn.isConnectionClosed())
			{
				if (closeConnOnFailure)
					closeHostMonConnection();
				return false;
			}
		}
		catch (Exception e)
		{
			return false;
		}

		_sshLastIsClosedCheck = System.currentTimeMillis();
		return true;
	}

	/**
	 * Set the <code>SshConnection</code> to use for monitoring.
	 */
//	@Override
//	public void setHostMonConnection(SshConnection sshConn)
//	{
//		_sshConn = sshConn;
//	}
	@Override
	public void setHostMonConnection(HostMonitorConnection hostMonConn)
	{
		_hostMonConn = hostMonConn;
	}

	/**
	 * Gets the <code>SshConnection</code> to the monitored server.
	 */
//	@Override
//	public SshConnection getHostMonConnection()
//	{
//		return _sshConn;
//	}
	@Override
	public HostMonitorConnection getHostMonConnection()
	{
		return _hostMonConn;
	}

	/** Gets the <code>SshConnection</code> to the monitored server. */
	@Override
	public void closeHostMonConnection()
	{
//		if (_sshConn == null) 
		if (_hostMonConn == null) 
			return;

		try
		{
//			if ( ! _sshConn.isClosed() )
			if ( ! _hostMonConn.isConnectionClosed() )
			{
//				_sshConn.close();
				_hostMonConn.closeConnection();
				if (_logger.isDebugEnabled())
				{
					_logger.debug("SSH Connection closed");
				}
			}
		}
		catch (Exception ev)
		{
			_logger.error("closeHostMonConnection", ev);
		}
		_conn = null;
	}
	

	//==================================================================
	// BEGIN: statistical mehods
	//==================================================================
	/**
	 * Call this whenever we do a new sample
	 * @param mainSampleTime the time of the sample.
	 */
	@Override
	public void setStatisticsTime(Timestamp mainSampleTime)
	{
		if (_statFirstSampleTime == null)
			_statFirstSampleTime = mainSampleTime;

		_statLastSampleTime = mainSampleTime;
	}

	/**
	 * Reset statistical times (first/last) sample times<br>
	 * This would be called by the statistical "sender" after a disconnect.
	 */
	@Override
	public void resetStatisticsTime()
	{
		_statFirstSampleTime = null;
		_statLastSampleTime  = null;
	}

	/**
	 * Get first sample time, used by the statistical send<br>
	 * If no samples has been done it will return null
	 */
	@Override
	public Timestamp getStatisticsFirstSampleTime()
	{
		return _statFirstSampleTime;
	}

	/**
	 * Get last sample time, used by the statistical send
	 * If no samples has been done it will return null
	 */
	@Override
	public Timestamp getStatisticsLastSampleTime()
	{
		return _statLastSampleTime;
	}
	//==================================================================
	// END: statistical mehods
	//==================================================================

	
	
	
	//==================================================================
	// BEGIN: CM Tooltip
	//==================================================================
	@Override
	public ITableTooltip createCmToolTipSupplier(CountersModel cm)
	{
		return new CmToolTipSupplierDefault(cm);
	}
	//==================================================================
	// END: CM Tooltip
	//==================================================================


	
	
	
	//==================================================================
	// BEGIN: CM Demand Refresh List
	//==================================================================
	private Set<String> _cmDemandRefreshList = new HashSet<String>();
	
	@Override
	public boolean isCmInDemandRefreshList(String name)
	{
		return _cmDemandRefreshList.contains(name);
	}

	@Override
	public void addCmToDemandRefreshList(String name)
	{
		_cmDemandRefreshList.add(name);
	}

	@Override
	public void removeCmFromDemandRefreshList(String name)
	{
		_cmDemandRefreshList.remove(name);
	}

	@Override
	public void clearCmDemandRefreshList()
	{
		_cmDemandRefreshList.clear();
	}

	@Override
	public Set<String> getCmDemandRefreshList()
	{
		return _cmDemandRefreshList;
	}

	@Override
	public int getCmDemandRefreshListCount()
	{
		return _cmDemandRefreshList.size();
	}
	


	public static final String PROPKEY_cmDemandRefreshSleepTime = "CounterCollector.cmDemandRefreshSleepTime";
	public static final int    DEFAULT_cmDemandRefreshSleepTime = 3;

	public static final String PROPKEY_cmDemandRefreshSleepTime_forMaxTimeInSec = "CounterCollector.cmDemandRefreshSleepTime.forMaxTimeInSec";
	public static final int    DEFAULT_cmDemandRefreshSleepTime_forMaxTimeInSec = 300; // 5 Minutes

	private int _cmDemandRefreshSleepTime                 = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_cmDemandRefreshSleepTime,                 DEFAULT_cmDemandRefreshSleepTime);
	private int _cmDemandRefreshSleepTime_forMaxTimeInSec = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_cmDemandRefreshSleepTime_forMaxTimeInSec, DEFAULT_cmDemandRefreshSleepTime_forMaxTimeInSec);

	// Set this when we start to do 'demand refresh' 
	private long _cmDemandRefreshSleepTime_startTime = -1;

	/**
	 * Used in the check loop to check if we need to sleep for a shorter time that the suggested time.
	 */
	@Override
	public int getCmDemandRefreshSleepTime(int suggestedSleepTime, long lastRefreshTimeInMs)
	{
//System.out.println("getCmDemandRefreshSleepTime(suggestedSleepTime="+suggestedSleepTime+", lastRefreshTimeInMs="+lastRefreshTimeInMs+"): shorterSleepTime="+_cmDemandRefreshSleepTime);
		// If we have no DemandRefresh in the List, simply return the suggestedSleepTime
		if (getCmDemandRefreshListCount() == 0)
			return suggestedSleepTime;

		int sleepTime        = suggestedSleepTime;
		int shorterSleepTime = _cmDemandRefreshSleepTime;
		
		boolean isDemandRefresh = false;

		// Loop all the CM's (in the demand list) and possibly decide for a shorted sleep time
		for (String cmName : getCmDemandRefreshList())
		{
			CountersModel cm = getCmByName(cmName);
			if (cm == null)
				continue;

			// if the CM's hasn't been refreshed in a while, then FORCE a shorter sleep time then the suggested
			long cmRefreshedInMs = System.currentTimeMillis() - cm.getPrevLocalRefreshTime();
			long expireValue = (suggestedSleepTime * 1000) + lastRefreshTimeInMs + 1000; // last 1000 is just add an extra second...
//System.out.println("getCmDemandRefreshSleepTime(): cmName='"+cmName+"', cmRefreshedInMs="+cmRefreshedInMs+" > expireValue="+expireValue+". if="+(cmRefreshedInMs > expireValue) );
			if (cmRefreshedInMs > expireValue)
			{
				isDemandRefresh = true;

				long inDemandRefreshForXSec = 0;
				if (_cmDemandRefreshSleepTime_startTime != -1)
				{
					inDemandRefreshForXSec = (System.currentTimeMillis() - _cmDemandRefreshSleepTime_startTime) / 1000;
				}
				
				// Decide if we want to "oner" the override, or if we should dismiss it due to: "that we have been in override-sleep for to long"
				if (inDemandRefreshForXSec > _cmDemandRefreshSleepTime_forMaxTimeInSec)
				{
					sleepTime = suggestedSleepTime;
					_logger.info("Override-sleep time is dismissed. Sleep time will be " + sleepTime + ". This since we have requested 'override-sleep' for " + inDemandRefreshForXSec + " seconds, and the '" + PROPKEY_cmDemandRefreshSleepTime_forMaxTimeInSec + "' is set to '" + _cmDemandRefreshSleepTime_forMaxTimeInSec + "'.");
					break;
				}
				else
				{
					sleepTime = shorterSleepTime;
					_logger.info("Setting override-sleep time to "+sleepTime+" (from "+suggestedSleepTime+"). This since previous sample had 'demand-refresh'. Decision based on CM '"+cmName+"' with "+cmRefreshedInMs+" ms since last refresh. Requested Counters: "+getCmDemandRefreshList());
					break;
				}
			}
		}

		if (isDemandRefresh)
		{
			if (_cmDemandRefreshSleepTime_startTime == -1)
				_cmDemandRefreshSleepTime_startTime = System.currentTimeMillis();
		}
		else
		{
			_cmDemandRefreshSleepTime_startTime = -1;
		}
		
		return sleepTime;
	}
	//==================================================================
	// END: CM Demand Refresh List
	//==================================================================

	
//	//==================================================================
//	// BEGIN: DBMS Datatype Resolver
//	//==================================================================
//	private DbmsDataTypeResolver _dbmsDataTypeResolver = null; 
//
//	@Override
//	public DbmsDataTypeResolver getDbmsDataTypeResolver()
//	{
//		return _dbmsDataTypeResolver;
//	}
//
//	@Override
//	public void setDbmsDataTypeResolver(DbmsDataTypeResolver resolver)
//	{
//		_dbmsDataTypeResolver = resolver;
//	}
//
//	@Override
//	public DbmsDataTypeResolver createDbmsDataTypeResolver()
//	{
//		return null;
//	}
//
//	//==================================================================
//	// END: DBMS Datatype Resolver
//	//==================================================================

	
	//==================================================================
	// BEGIN: NO-GUI methods
	//==================================================================
	@Override
	public DbxConnection noGuiConnect(String dbmsUsername, String dbmsPassword, String dbmsServer, String dbmsHostPortStr, String jdbcUrlOptions) 
	throws SQLException, Exception
	{
		String jdbcDriver = "";
		String jdbcUrl    = "";

		String dbxTune = Version.getAppName();
		if      ("AseTune"       .equalsIgnoreCase(dbxTune)) { throw new Exception("This is handled in CounterControllerAse"); }
		else if ("IqTune"        .equalsIgnoreCase(dbxTune)) { jdbcDriver = "com.sybase.jdbc42.jdbc.SybDriver";             jdbcUrl = "jdbc:sybase:Tds:"     + dbmsHostPortStr; }
		else if ("RsTune"        .equalsIgnoreCase(dbxTune)) { jdbcDriver = "com.sybase.jdbc42.jdbc.SybDriver";             jdbcUrl = "jdbc:sybase:Tds:"     + dbmsHostPortStr; }
		else if ("RaxTune"       .equalsIgnoreCase(dbxTune)) { jdbcDriver = "com.sybase.jdbc42.jdbc.SybDriver";             jdbcUrl = "jdbc:sybase:Tds:"     + dbmsHostPortStr; }
		else if ("SqlServerTune" .equalsIgnoreCase(dbxTune)) { jdbcDriver = "com.microsoft.sqlserver.jdbc.SQLServerDriver"; jdbcUrl = "jdbc:sqlserver://"    + dbmsHostPortStr; }
//		else if ("PostgresTune"  .equalsIgnoreCase(dbxTune)) { jdbcDriver = "org.postgresql.Driver";                        jdbcUrl = "jdbc:postgresql://"   + dbmsHostPortStr + "/postgres"; }
		else if ("MySqlTune"     .equalsIgnoreCase(dbxTune)) { jdbcDriver = "com.mysql.jdbc.Driver";                        jdbcUrl = "jdbc:mysql://"        + dbmsHostPortStr; }
		else if ("Db2Tune"       .equalsIgnoreCase(dbxTune)) { jdbcDriver = "com.ibm.db2.jcc.DB2Driver";                    jdbcUrl = "jdbc:db2://"          + dbmsHostPortStr; }
		else if ("OracleTune"    .equalsIgnoreCase(dbxTune)) { jdbcDriver = "oracle.jdbc.OracleDriver";                     jdbcUrl = "jdbc:oracle:thin:@//" + dbmsHostPortStr; }
		else if ("HanaTune"      .equalsIgnoreCase(dbxTune)) { jdbcDriver = "com.sap.db.jdbc.Driver";                       jdbcUrl = "jdbc:sap://"          + dbmsHostPortStr; }
		else if ("PostgresTune"  .equalsIgnoreCase(dbxTune))
		{
			// If we do NOT pass a database name in the URL, then we will add "/postgres" as the default database
			String pgDefaultDbname = "";

			if ( ! dbmsHostPortStr.contains("/") )
				pgDefaultDbname = "/postgres";

			jdbcDriver = "org.postgresql.Driver";
			jdbcUrl = "jdbc:postgresql://" + dbmsHostPortStr + pgDefaultDbname;
		}
		else throw new Exception(Version.getAppName() + " Do not yet support no-gui mode.");

		// Load the driver
//		try
//		{
//			if (StringUtil.hasValue(jdbcDriver))
//				Class.forName(jdbcDriver).newInstance();
//		}
//		catch (Exception ex)
//		{
//			_logger.info("Trying to load JDBC Driver '', the old way, via 'Class.forName(jdbcDriver).newInstance()' and got problems, which we will disregard, and trying with the standard way 'DriverManager.getConnection(jdbcUrl, jdbcProps)'. Caught: "+ex);
//		}

		Properties jdbcProps = new Properties();
		jdbcProps.putAll(StringUtil.parseCommaStrToMap(jdbcUrlOptions, "=", ";"));
		
		jdbcProps.put("user",     dbmsUsername);
		jdbcProps.put("password", dbmsPassword);

		// set Application name
		String jdbcAppNameProp = null;
		if      ("SqlServerTune" .equalsIgnoreCase(dbxTune)) { jdbcAppNameProp = "applicationName"; }
		else if ("PostgresTune"  .equalsIgnoreCase(dbxTune)) { jdbcAppNameProp = "ApplicationName"; }
		else if ("MySqlTune"     .equalsIgnoreCase(dbxTune)) { jdbcAppNameProp = ""; }
		else if ("Db2Tune"       .equalsIgnoreCase(dbxTune)) { jdbcAppNameProp = ""; }
		else if ("OracleTune"    .equalsIgnoreCase(dbxTune)) { jdbcAppNameProp = ""; }
		else if ("HanaTune"      .equalsIgnoreCase(dbxTune)) { jdbcAppNameProp = ""; }
		
		if (StringUtil.hasValue(jdbcAppNameProp))
			jdbcProps.put(jdbcAppNameProp, Version.getAppName());

		
		// Set some extra connection properties based on DbxApplication
		// NOTE: Maybe we should try to do this somewhere in DbxConnection by parsing the URL Start...
		if ("SqlServerTune" .equalsIgnoreCase(dbxTune)) 
		{
			int socketTimeout = Configuration.getCombinedConfiguration().getIntProperty("SqlServerTune.jdbc.socketTimeout", 60*1000);
			jdbcProps.put("socketTimeout", socketTimeout+"");
			
			_logger.info("Adding SQL-Server JDBC connection property 'socketTimeout="+socketTimeout+"'");
		}
		

		// Make the connection, and create a DbxConnection
		try
		{
			_logger.info("Connecting to: jdbcUrl='"+jdbcUrl+"'.");
			_logger.debug("Connecting to: jdbcUrl='"+jdbcUrl+"', jdbcProps='"+jdbcProps+"'.");

			Connection jdbcConn = DriverManager.getConnection(jdbcUrl, jdbcProps);
			DbxConnection conn = DbxConnection.createDbxConnection(jdbcConn);

			// set the connection props so it can be reused...
			ConnectionProp cp = new ConnectionProp();
			cp.setLoginTimeout ( 20 );
			cp.setDriverClass  ( jdbcDriver );
			cp.setUrl          ( jdbcUrl );
			cp.setUrlOptions   ( jdbcProps );
			cp.setUsername     ( dbmsUsername );
			cp.setPassword     ( dbmsPassword );
			cp.setAppName      ( Version.getAppName() );
			cp.setAppVersion   ( Version.getVersionStr() );
//			cp.setHostPort     ( hosts, ports );
//			cp.setSshTunnelInfo( sshTunnelInfo );

			conn.setConnProp(cp);
			DbxConnection.setDefaultConnProp(cp);

			// Return the connection
			return conn;
		}
		catch(SQLException ex)
		{
			jdbcProps.put("password", "*secret*");
			_logger.info("Connection properties used when tring to connecting using: DriverManager.getConnection(jdbcUrl='"+jdbcUrl+"', jdbcProps='"+jdbcProps+"')");
			throw ex;
		}
	}
	
	@Override
	public void noGuiConnectErrorHandler(SQLException ex, String dbmsUsername, String dbmsPassword, String dbmsServer, String dbmsHostPortStr, String jdbcUrlOptions) 
	throws Exception
	{
		// Error checking for "invalid password" or other "unrecoverable errors"
	}

	//==================================================================
	// END: NO-GUI methods
	//==================================================================

	//==================================================================
	// BEGIN: DBMS Properties
	//==================================================================
	/** 
	 * Set a DBMS property, Would normally be done when connecting to it.<br>
	 * We would get some extra information that is NOT generic over all databases, like 'Edition', or 'PageSize'...<br>
	 * or some other specifics for that DBMS Vendor that is NOT populated by another vendor.<br> 
	 * 
	 * Initial use of this would be to check if we connected to a SQL-Server 'Azure' instance<br>
	 * In that case the Dynamic Management Views will an alternate name.
	 * 
	 * @return The previous value stored here (null if no value existed)
	 */
	@Override public Object  setDbmsProperty(String name, String  value) { return _dbmsProperties.setProperty(name, value); }
	@Override public int     setDbmsProperty(String name, int     value) { return _dbmsProperties.setProperty(name, value); }
	@Override public boolean setDbmsProperty(String name, boolean value) { return _dbmsProperties.setProperty(name, value); }

	/** Check if we have a DBMS property, This is DBMS Vendor Specific Settings... but NOT DBMS Configurations */
	@Override
	public boolean hasDbmsProperty(String name)
	{
		return _dbmsProperties.hasProperty(name);
	}

	/** Get a DBMS property, This is DBMS Vendor Specific Settings... but NOT DBMS Configurations */
	@Override public String  getDbmsProperty    (String name, String  defaultValue) { return _dbmsProperties.getProperty       (name, defaultValue); }
	@Override public int     getDbmsPropertyInt (String name, int     defaultValue) { return _dbmsProperties.getIntProperty    (name, defaultValue); }
	@Override public boolean getDbmsPropertyBool(String name, boolean defaultValue) { return _dbmsProperties.getBooleanProperty(name, defaultValue); }

	/** Get a DBMS property, This is DBMS Vendor Specific Settings... but NOT DBMS Configurations */
	@Override
	public Configuration getDbmsProperty()
	{
		return _dbmsProperties;
	}

	/**
	 * Check if a specific DBMS Option is enabled 
	 * 
	 * @return true if it's enabled... false if it's disabled
	 * @throws RuntimeException if the Counter Controller is not yet initialized
	 */
	@Override
	public boolean isDbmsOptionEnabled(DbmsOption dbmsOption)
	{
//		if ( ! isInitialized() )
//			throw new RuntimeException("The Counter Controller is not yet initialized.");

		return false;
	}

	//==================================================================
	// END: DBMS Properties
	//==================================================================

	@Override
	public void prepareForPcsDatabaseRollover()
	{
		for (CountersModel cm : getCmList())
		{
			cm.prepareForPcsDatabaseRollover();
		}
	}

	@Override
	public void doLastRecordingActionBeforeDatabaseRollover(DbxConnection recordingDbConn)
	{
		// override this if thsi Vendor needs to save anything...
	}


	//==================================================================
	// BEGIN: Scheduler
	//==================================================================
    private Scheduler _scheduler = null;

    /**
	 * Create any cron4j.Scheduler
	 * @param hasGui
	 * @return
	 */
	@Override
	public Scheduler createScheduler(boolean hasGui)
	{
		return null;
	}

	/** Get the installed scheduler instance */
	@Override
	public Scheduler getScheduler()
	{
		return _scheduler;
	}

	/** Set the installed scheduler instance */
	@Override
	public void setScheduler(Scheduler scheduler)
	{
		_scheduler = scheduler;
	}

	/** Starts the installed scheduler, if we have any installed */
	@Override
	public void startScheduler()
	{
		Scheduler scheduler = getScheduler();
		if (scheduler != null)
		{
			_logger.info("Starting scheduler thread.");
			//_scheduler.setName("cron4j-sched"); // this would have been nice...
			_scheduler.setDaemon(true);
			_scheduler.start();
		}
	}

	@Override
	public void stopScheduler()
	{
		Scheduler scheduler = getScheduler();
		if (scheduler != null)
		{
			_logger.info("Stopping scheduler thread.");
			scheduler.stop();
		}
	}
	//==================================================================
	// END: Scheduler
	//==================================================================


	//==================================================================
	// BEGIN: serverAliasName and serverDisplayName
	//==================================================================
	private String _serverAliasName;
	private String _serverDisplayName;

	/** 
	 * (in no-gui mode) the Collector can specify '-A' or '--aliasName' this would be that value <br>
	 * NOTE: This might be removed when a better solution is implemented
	 */
	@Override
	public String getServerAliasName()
	{
		return _serverAliasName;
	}

	/** 
	 * (in no-gui mode) the Collector can specify '-N' or '--aliasName' this would be that value <br>
	 * NOTE: This might be removed when a better solution is implemented
	 */
	@Override
	public void setServerAliasName(String aliasName)
	{
		_serverAliasName = aliasName;
	}

	/** 
	 * (in no-gui mode) the Collector can specify '-N' or '--displayName' this would be that value <br>
	 * NOTE: This might be removed when a better solution is implemented
	 */
	@Override
	public String getServerDisplayName()
	{
		return _serverDisplayName;
	}

	/** 
	 * (in no-gui mode) the Collector can specify '-N' or '--displayName' this would be that value <br>
	 * NOTE: This might be removed when a better solution is implemented
	 */
	@Override
	public void setServerDisplayName(String displayName)
	{
		_serverDisplayName = displayName;
	}
	//==================================================================
	// END: serverDisplayName
	//==================================================================
}
