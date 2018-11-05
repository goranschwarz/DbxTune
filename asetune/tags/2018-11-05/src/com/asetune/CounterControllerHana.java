package com.asetune;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import org.apache.log4j.Logger;

import com.asetune.cm.CountersModel;
import com.asetune.cm.hana.CmActiveStatements;
import com.asetune.cm.hana.CmCsTables;
import com.asetune.cm.hana.CmDeltaMerge;
import com.asetune.cm.hana.CmIoStat;
import com.asetune.cm.hana.CmLockStat;
import com.asetune.cm.hana.CmLockWait;
import com.asetune.cm.hana.CmPlanCacheDetails;
import com.asetune.cm.hana.CmPlanCacheOverview;
import com.asetune.cm.hana.CmServiceMemory;
import com.asetune.cm.hana.CmSummary;
import com.asetune.cm.os.CmOsDiskSpace;
import com.asetune.cm.os.CmOsIostat;
import com.asetune.cm.os.CmOsMeminfo;
import com.asetune.cm.os.CmOsMpstat;
import com.asetune.cm.os.CmOsNwInfo;
import com.asetune.cm.os.CmOsUptime;
import com.asetune.cm.os.CmOsVmstat;
import com.asetune.gui.MainFrame;
import com.asetune.pcs.PersistContainer;
import com.asetune.pcs.PersistContainer.HeaderInfo;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.AseConnectionUtils;


public class CounterControllerHana 
extends CounterControllerAbstract
{
	private static Logger _logger = Logger.getLogger(CounterControllerHana.class);

	public static final int	   NUMBER_OF_PERFORMANCE_COUNTERS	= 54 + 10 + 20; 

	/**
	 * The default constructor
	 * @param hasGui should we create a GUI or NoGUI collection thread
	 */
	public CounterControllerHana(boolean hasGui)
	{
		super(hasGui);
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

		ICounterController counterController = this;
		MainFrame          guiController     = hasGui ? MainFrame.getInstance() : null;

		CmSummary           .create(counterController, guiController);

		// Server Tab
		CmServiceMemory     .create(counterController, guiController);

		// Object/Access Tab
		CmActiveStatements  .create(counterController, guiController);
		CmCsTables          .create(counterController, guiController);
		CmDeltaMerge        .create(counterController, guiController);
		
		// Cache Tab
		CmPlanCacheOverview .create(counterController, guiController);
		CmPlanCacheDetails  .create(counterController, guiController);

		// Disk Tab
		CmIoStat            .create(counterController, guiController);
		CmLockWait          .create(counterController, guiController);
		CmLockStat          .create(counterController, guiController);

		// OS HOST Monitoring
		CmOsIostat          .create(counterController, guiController);
		CmOsVmstat          .create(counterController, guiController);
		CmOsMpstat          .create(counterController, guiController);
		CmOsUptime          .create(counterController, guiController);
		CmOsMeminfo         .create(counterController, guiController);
		CmOsNwInfo          .create(counterController, guiController);
		CmOsDiskSpace       .create(counterController, guiController);

		// USER DEFINED COUNTERS
		createUserDefinedCounterModels(counterController, guiController);
		createUserDefinedCounterModelHostMonitors(counterController, guiController);

		// done
		setCountersIsCreated(true);
	}

	/**
	 * When we have a database connection, lets do some extra init this
	 * so all the CountersModel can decide what SQL to use. <br>
	 * SQL statement usually depends on what ASE Server version we monitor.
	 * 
	 * @param conn
	 * @param hasGui              is this initialized with a GUI?
	 * @param srvVersion          what is the Servers Executable version
	 * @param isClusterEnabled    is it a cluster ASE
	 * @param monTablesVersion    what version of the MDA tables should we use
	 */
	@Override
//	public void initCounters(Connection conn, boolean hasGui, int srvVersion, boolean isClusterEnabled, int monTablesVersion)
//	throws Exception
	public void initCounters(DbxConnection conn, boolean hasGui, int srvVersion, boolean isClusterEnabled, int monTablesVersion)
	throws Exception
	{
		if (isInitialized())
		return;

		if ( ! AseConnectionUtils.isConnectionOk(conn, hasGui, null))
			throw new Exception("Trying to initialize the counters with a connection this seems to be broken.");

			
		if (! isCountersCreated())
			createCounters(hasGui);
		
		_logger.info("Initializing all CM objects, using HANA version number "+srvVersion+".");

		// initialize all the CM's
		for (CountersModel cm : getCmList())
		{
			_logger.debug("Initializing CM named '"+cm.getName()+"', display name '"+cm.getDisplayName()+"', using HANA version number "+srvVersion+".");

			// set the version
			cm.setServerVersion(monTablesVersion);
			cm.setClusterEnabled(isClusterEnabled);

			// set the active roles, so it can be used in initSql()
//			cm.setActiveRoles(_activeRoleList);

			// set the ASE Monitor Configuration, so it can be used in initSql() and elsewhere
//			cm.setMonitorConfigs(monitorConfigMap);

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
			cm.initTrendGraphForVersion(monTablesVersion);
		}

		setInitialized(true);
	}

	@Override
	public void checkServerSpecifics()
	{
	}

	@Override
	public PersistContainer.HeaderInfo createPcsHeaderInfo()
	{
		// Get session/head info
		String    aseServerName    = null;
		String    aseHostname      = null;
		Timestamp mainSampleTime   = null;
		Timestamp counterClearTime = null;

		String sql = "select CURRENT_TIMESTAMP as SRV_TIME, SYSTEM_ID, HOST from SYS.M_DATABASE";

		try
		{
			if ( ! isMonConnected(true, true) ) // forceConnectionCheck=true, closeConnOnFailure=true
				return null;

			Statement stmt = getMonConnection().createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next())
			{
				mainSampleTime   = rs.getTimestamp(1);
				aseServerName    = rs.getString(2);
				aseHostname      = rs.getString(3);
				//counterClearTime = rs.getTimestamp(4); // use this row if we can get when all counters are cleared... which I don't think is relevant for HANA
				counterClearTime = new Timestamp(0);     // since global counter clear isn't in HANA set it to 0
			}
			rs.close();
			stmt.close();
		}
		catch (SQLException sqlex)
		{
			// Check if Connection is already closed.
//			if ( "JZ0C0".equals(sqlex.getSQLState()) )
//			{
				boolean forceConnectionCheck = true;
				boolean closeConnOnFailure   = true;
				if ( ! isMonConnected(forceConnectionCheck, closeConnOnFailure) )
				{
					_logger.info("Problems getting basic status info in 'Counter get loop'. SQL State 'JZ0C0', which means 'Connection is already closed'. So lets start from the top." );
					return null;
				}
//			}
			
			_logger.warn("Problems getting basic status info in 'Counter get loop', reverting back to 'static values'. SQL '"+sql+"', Caught: " + sqlex.toString() );
			mainSampleTime   = new Timestamp(System.currentTimeMillis());
			aseServerName    = "unknown";
			aseHostname      = "unknown";
			counterClearTime = new Timestamp(0);
		}
		
		return new HeaderInfo(mainSampleTime, aseServerName, aseHostname, counterClearTime);
	}

	
//	// Simulate the _conn.isClosed() functionality, but add a query timeout...
//	// it looks like jConnect isClosed() could hang if you call many simultaneously
//	@Override
//	protected boolean isClosed(Connection conn)
//	throws SQLException
//	{
//		try
//		{
//			Statement stmnt   = conn.createStatement();
//			ResultSet rs      = stmnt.executeQuery("admin echo, 'RsTune-check:isClosed(conn)'");
//
//			stmnt.setQueryTimeout(5);
//			while (rs.next())
//			{
//				rs.getString(1);
//			}
//			rs.close();
//			stmnt.close();
//
//			// false = connection is alive, NOT Closed
//			return false;
//		}
//		catch (SQLException e)
//		{
//			if ( ! "JZ0C0".equals(e.getSQLState()) ) // connection is already closed...
//					_logger.warn("isClosed(conn) had problems", e);
//
//			throw e;
//		}
//	}

	@Override
	public String getServerTimeCmd()
	{
		return "SELECT CURRENT_TIMESTAMP FROM DUMMY \n";
	}

	@Override
	protected String getIsClosedSql()
	{
		return "SELECT 'HanaTune-check:isClosed(conn)' FROM DUMMY";
	}

	@Override
	public boolean isSqlBatchingSupported()
	{
		return false;
	}
}
