package com.asetune.tools.sqlcapture;

/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.asetune.CounterControllerAbstract;
import com.asetune.cm.CountersModel;
import com.asetune.pcs.PersistContainer.HeaderInfo;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.Ver;


/*
 * MDA Table version information should be in the various Cm* objects
 * Or at http://www.asemon.se/usage_report.php?mda=all
 * This page has approximately 40 versions stored right now (and more is coming as someone starts a new version that we hasn't seen before)
 */

public class RefreshCounters 
extends CounterControllerAbstract
{
	public RefreshCounters(boolean hasGui)
	{
		super(hasGui);
	}

	/** Log4j logging. */
	private static Logger _logger = Logger.getLogger(RefreshCounters.class);

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
	}


	/** Init needs to be implemented by any subclass */
	@Override
	public void init()
	throws Exception
	{
		// Create all the CM objects, the objects will be added to _CMList
		createCounters(hasGui());
	}

	@Override
	public void initCounters(DbxConnection conn, boolean hasGui, int srvVersion, boolean isClusterEnabled, int monTablesVersion)
	throws Exception
	{
		if (isInitialized())
			return;
	
		if ( ! AseConnectionUtils.isConnectionOk(conn, hasGui, null))
			throw new Exception("Trying to initialize the counters with a connection this seems to be broken.");
	
			
		if (! isCountersCreated())
			createCounters(true);
		
		_logger.info("Capture SQL; Initializing all CM objects, using ASE server version number "+srvVersion+", isClusterEnabled="+isClusterEnabled+" with monTables Install version "+monTablesVersion+".");
	
		// Get active ASE Roles
		List<String> activeRoleList = AseConnectionUtils.getActiveSystemRoles(conn);
//		_activeRoleList = AseConnectionUtils.getActiveRoles(conn);
		
		// Get active Monitor Configuration
		Map<String,Integer> monitorConfigMap = AseConnectionUtils.getMonitorConfigs(conn);


		// in version 15.0.3.1 compatibility_mode was introduced, this to use 12.5.4 optimizer & exec engine
		// This will hurt performance, especially when querying sysmonitors table, so set this to off
//		if (aseVersion >= 1503010)
		if (srvVersion >= Ver.ver(15,0,3,1))
			AseConnectionUtils.setCompatibilityMode(conn, false);

		// initialize all the CM's
		for (CountersModel cm : getCmList())
		{
			if (cm != null)
			{
				_logger.debug("Initializing CM named '"+cm.getName()+"', display name '"+cm.getDisplayName()+"', using ASE server version number "+srvVersion+".");

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
	public void createCounters(boolean hasGui)
	{
		if (isCountersCreated())
			return;

		_logger.info("Creating ALL CM Objects.");

//		GetCounters counterController = this;
//		MainFrame   guiController     = hasGui ? MainFrame.getInstance() : null;

//		CmSummary          .create(counterController, guiController);
//
//		CmObjectActivity   .create(counterController, guiController);
//		CmProcessActivity  .create(counterController, guiController);
//		CmSpidWait         .create(counterController, guiController);
		// done
		setCountersIsCreated(true);
	}

	/** The run is located in any implementing subclass */
//	@Override
//	public void run()
//	{
//	}





	@Override
	public HeaderInfo createPcsHeaderInfo()
	{
		throw new RuntimeException("createPcsHeaderInfo(): THIS SHOULD NOT BE CALLED... will be removed later.");
	}

//	@Override
//	public void start()
//	{
//		throw new RuntimeException("start(): THIS SHOULD NOT BE CALLED... will be removed later.");
//	}


	@Override
	public void checkServerSpecifics()
	{
	}


	@Override
	public String getServerTimeCmd()
	{
		return "select getdate() \n";
	}

	@Override
	protected String getIsClosedSql()
	{
		return "select 'AseTune-check:isClosed(conn)'";
	}
}
