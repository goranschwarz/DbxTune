package com.asetune.tools.sqlcapture;

/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.naming.NameNotFoundException;
import javax.swing.JDialog;
import javax.swing.JOptionPane;

import org.apache.log4j.Logger;

import com.asetune.CounterControllerAbstract;
import com.asetune.cm.CountersModel;
import com.asetune.utils.AseConnectionUtils;


/*
 * MDA Table version information should be in the various Cm* objects
 * Or at http://www.asemon.se/usage_report.php?mda=all
 * This page has approximately 40 versions stored right now (and more is coming as someone starts a new version that we hasn't seen before)
 */

public class RefreshCounters 
extends CounterControllerAbstract
{
	/** Log4j logging. */
	private static Logger _logger          = Logger.getLogger(RefreshCounters.class);

	/**
	 * Reset All CM's etc, this so we build new SQL statements if we connect to a different ASE version<br>
	 * Most possible called from disconnect() or similar
	 * 
	 * @param resetAllCms call reset() on all cm's
	 */
	@Override
	public void reset(boolean resetAllCms)
	{
		super.reset(resetAllCms);

//		// is sp_configure 'capture missing statistics' set or not
//		_config_captureMissingStatistics = false;
//
//		// is sp_configure 'enable metrics capture' set or not
//		_config_enableMetricsCapture = false;
//		
//		// is sp_configure 'kernel mode' is set to 'threaded'
//		_config_threadedKernelMode = false;
//		
//		// A list of roles which the connected user has
//		_activeRoleList = null;
	}


	/** Init needs to be implemented by any subclass */
	@Override
	public void init()
	throws Exception
	{
		// Create all the CM objects, the objects will be added to _CMList
		createCounters();
	}

	/** shutdown or stop any collector */
	@Override
	public void shutdown()
	{
		_logger.info("Stopping the collector thread.");
//		_running = false;
		if (_thread != null)
			_thread.interrupt();
	}
	
//	/**
//	 * When we have a database connection, lets do some extra init this
//	 * so all the CountersModel can decide what SQL to use. <br>
//	 * SQL statement usually depends on what ASE Server version we monitor.
//	 * 
//	 * @param conn
//	 * @param hasGui              is this initialized with a GUI?
//	 * @param aseVersion          what is the ASE Executable version
//	 * @param isClusterEnabled    is it a cluster ASE
//	 * @param monTablesVersion    what version of the MDA tables should we use
//	 */
//	@Override
//	public void initCounters(Connection conn, boolean hasGui, int aseVersion, boolean isClusterEnabled, int monTablesVersion)
//	throws Exception
//	{
//		if (isInitialized())
//			return;
//
//		if ( ! AseConnectionUtils.isConnectionOk(conn, hasGui, null))
//			throw new Exception("Trying to initialize the counters with a connection this seems to be broken.");
//
//			
//		if (! isCountersCreated())
//			createCounters();
//		
//		_logger.info("Initializing all CM objects, using ASE server version number "+aseVersion+", isClusterEnabled="+isClusterEnabled+" with monTables Install version "+monTablesVersion+".");
//
//		// Get active ASE Roles
//		//List<String> activeRoleList = AseConnectionUtils.getActiveRoles(conn);
//		_activeRoleList = AseConnectionUtils.getActiveRoles(conn);
//		
//		// Get active Monitor Configuration
//		Map<String,Integer> monitorConfigMap = AseConnectionUtils.getMonitorConfigs(conn);
//
//		// Get some specific configurations
////		if (aseVersion >= 15031)
//		if (aseVersion >= 1503010)
//			_config_captureMissingStatistics = AseConnectionUtils.getAseConfigRunValueBooleanNoEx(conn, "capture missing statistics");
//
////		if (aseVersion >= 15020)
//		if (aseVersion >= 1502000)
//			_config_enableMetricsCapture = AseConnectionUtils.getAseConfigRunValueBooleanNoEx(conn, "enable metrics capture");
//
//		_config_threadedKernelMode = false;
////		if (aseVersion >= 15700)
//		if (aseVersion >= 1570000)
//		{
//			String kernelMode = AseConnectionUtils.getAseConfigRunValueStrNoEx(conn, "kernel mode");
//			_config_threadedKernelMode = "threaded".equals(kernelMode);
//		}
//
//		
//		// in version 15.0.3.1 compatibility_mode was introduced, this to use 12.5.4 optimizer & exec engine
//		// This will hurt performance, especially when querying sysmonitors table, so set this to off
////		if (aseVersion >= 15031)
//		if (aseVersion >= 1503010)
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
//				_logger.debug("Initializing CM named '"+cm.getName()+"', display name '"+cm.getDisplayName()+"', using ASE server version number "+aseVersion+".");
//
//				// set the version
////				cm.setServerVersion(aseVersion);
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
//		setInitialized(true);
//	}
	@Override
	public void initCounters(Connection conn, boolean hasGui, int aseVersion, boolean isClusterEnabled, int monTablesVersion)
	throws Exception
	{
		if (isInitialized())
			return;
	
		if ( ! AseConnectionUtils.isConnectionOk(conn, hasGui, null))
			throw new Exception("Trying to initialize the counters with a connection this seems to be broken.");
	
			
		if (! isCountersCreated())
			createCounters();
		
		_logger.info("Capture SQL; Initializing all CM objects, using ASE server version number "+aseVersion+", isClusterEnabled="+isClusterEnabled+" with monTables Install version "+monTablesVersion+".");
	
		// Get active ASE Roles
		List<String> activeRoleList = AseConnectionUtils.getActiveRoles(conn);
//		_activeRoleList = AseConnectionUtils.getActiveRoles(conn);
		
		// Get active Monitor Configuration
		Map<String,Integer> monitorConfigMap = AseConnectionUtils.getMonitorConfigs(conn);


		// in version 15.0.3.1 compatibility_mode was introduced, this to use 12.5.4 optimizer & exec engine
		// This will hurt performance, especially when querying sysmonitors table, so set this to off
		if (aseVersion >= 1503010)
			AseConnectionUtils.setCompatibilityMode(conn, false);

		// initialize all the CM's
		for (CountersModel cm : _CMList)
		{
			if (cm != null)
			{
				_logger.debug("Initializing CM named '"+cm.getName()+"', display name '"+cm.getDisplayName()+"', using ASE server version number "+aseVersion+".");

				// set the version
//				cm.setServerVersion(aseVersion);
				cm.setServerVersion(monTablesVersion);
				cm.setClusterEnabled(isClusterEnabled);
				
				// set the active roles, so it can be used in initSql()
				cm.setActiveRoles(activeRoleList);

				// set the ASE Monitor Configuration, so it can be used in initSql() and elsewhere
				cm.setMonitorConfigs(monitorConfigMap);

				// Now when we are connected to a server, and properties are set in the CM, 
				// mark it as runtime initialized (or late initialization)
				// do NOT do this at the end of this method, because some of the above stuff
				// will be used by the below method calls.
				cm.setRuntimeInitialized(true);

				// Initializes SQL, use getServerVersion to check what we are connected to.
				cm.initSql(conn);

				// Use this method if we need to check anything in the database.
				// for example "deadlock pipe" may not be active...
				// If server version is below 15.0.2 statement cache info should not be VISABLE
				cm.init(conn);

				// Initialize graphs for the version you just connected to
				// This will simply enable/disable graphs that should not be visible for the ASE version we just connected to
				//cm.initTrendGraphForVersion(monTablesVersion);
			}
		}
			
		setInitialized(true);
	}
	
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
	public void createCounters()
	{
		if (isCountersCreated())
			return;

		_logger.info("Creating ALL CM Objects.");

//		GetCounters counterController = this;
//		MainFrame   guiController     = Configuration.hasGui() ? MainFrame.getInstance() : null;

//		CmSummary          .create(counterController, guiController);
//
//		CmObjectActivity   .create(counterController, guiController);
//		CmProcessActivity  .create(counterController, guiController);
//		CmSpidWait         .create(counterController, guiController);
		// done
		setCountersIsCreated(true);
	}

	/** The run is located in any implementing subclass */
	@Override
	public void run()
	{
	}

}
