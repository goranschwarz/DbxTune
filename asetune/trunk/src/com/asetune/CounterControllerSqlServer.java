package com.asetune;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import org.apache.log4j.Logger;

import com.asetune.cm.CountersModel;
import com.asetune.cm.sqlserver.CmActiveStatements;
import com.asetune.cm.sqlserver.CmDbIo;
import com.asetune.cm.sqlserver.CmDeviceIo;
import com.asetune.cm.sqlserver.CmOptimizer;
import com.asetune.cm.sqlserver.CmPerfCounters;
import com.asetune.cm.sqlserver.CmProcedureStats;
import com.asetune.cm.sqlserver.CmSchedulers;
import com.asetune.cm.sqlserver.CmSummary;
import com.asetune.cm.sqlserver.CmWaitStats;
import com.asetune.cm.sqlserver.CmWho;
import com.asetune.gui.MainFrame;
import com.asetune.pcs.PersistContainer;
import com.asetune.pcs.PersistContainer.HeaderInfo;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.AseConnectionUtils;


public class CounterControllerSqlServer 
extends CounterControllerAbstract
{
	private static Logger _logger = Logger.getLogger(CounterControllerSqlServer.class);

	public static final int	   NUMBER_OF_PERFORMANCE_COUNTERS	= 54 + 10 + 20; 

	/**
	 * The default constructor
	 * @param hasGui should we create a GUI or NoGUI collection thread
	 */
	public CounterControllerSqlServer(boolean hasGui)
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

		CmSummary          .create(counterController, guiController);

		CmWho              .create(counterController, guiController);
		CmSchedulers       .create(counterController, guiController);
		CmWaitStats        .create(counterController, guiController);
		CmPerfCounters     .create(counterController, guiController);
		CmOptimizer        .create(counterController, guiController);

		CmActiveStatements .create(counterController, guiController);

		CmProcedureStats   .create(counterController, guiController);

		CmDeviceIo         .create(counterController, guiController);
		CmDbIo             .create(counterController, guiController);


		// OS HOST Monitoring
//		CmOsIostat          .create(counterController, guiController);
//		CmOsVmstat          .create(counterController, guiController);
//		CmOsMpstat          .create(counterController, guiController);
//		CmOsUptime          .create(counterController, guiController);

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
		
		_logger.info("Initializing all CM objects, using MS SQL-Server version number "+srvVersion+".");

		// initialize all the CM's
		for (CountersModel cm : getCmList())
		{
			_logger.debug("Initializing CM named '"+cm.getName()+"', display name '"+cm.getDisplayName()+"', using MS SQL-Server version number "+srvVersion+".");

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
//		return new HeaderInfo(new Timestamp(System.currentTimeMillis()), "DUMMY_HANA", "DUMMY_HANA", new Timestamp(System.currentTimeMillis()));
		// Get session/head info
		String    sqlServerName    = null;
		String    sqlHostname      = null;
		Timestamp mainSampleTime   = null;
		Timestamp counterClearTime = null;

//		String sql = "select getdate(), @@servername, @@servername, CountersCleared='2000-01-01 00:00:00'";
		String sql = "select getdate(), convert(varchar(100),SERVERPROPERTY('InstanceName')), convert(varchar(100),SERVERPROPERTY('MachineName')), CountersCleared='2000-01-01 00:00:00'";

		try
		{
			if ( ! isMonConnected(true, true) ) // forceConnectionCheck=true, closeConnOnFailure=true
				return null;
				
			Statement stmt = getMonConnection().createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next())
			{
				mainSampleTime   = rs.getTimestamp(1);
				sqlServerName    = rs.getString(2);
				sqlHostname      = rs.getString(3);
				counterClearTime = rs.getTimestamp(4);
			}
			rs.close();
			stmt.close();
		}
		catch (SQLException sqlex)
		{
			// Connection is already closed.
			if ( "JZ0C0".equals(sqlex.getSQLState()) )
			{
				boolean forceConnectionCheck = true;
				boolean closeConnOnFailure   = true;
				if ( ! isMonConnected(forceConnectionCheck, closeConnOnFailure) )
				{
					_logger.info("Problems getting basic status info in 'Counter get loop'. SQL State 'JZ0C0', which means 'Connection is already closed'. So lets start from the top." );
					return null;
				}
			}
			
			_logger.warn("Problems getting basic status info in 'Counter get loop', reverting back to 'static values'. SQL '"+sql+"', Caught: " + sqlex.toString() );
			mainSampleTime   = new Timestamp(System.currentTimeMillis());
			sqlServerName    = "unknown";
			sqlHostname      = "unknown";
			counterClearTime = new Timestamp(0);
		}
//System.out.println("SQLSERVER: createPcsHeaderInfo(): mainSampleTime='"+mainSampleTime+"', sqlServerName='"+sqlServerName+"', sqlHostname='"+sqlHostname+"', counterClearTime='"+counterClearTime+"'");
		return new HeaderInfo(mainSampleTime, sqlServerName, sqlHostname, counterClearTime);
	}

	@Override
	public String getServerTimeCmd()
	{
		return "select getdate() \n";
	}

	@Override
	protected String getIsClosedSql()
	{
		return "SELECT 'SqlServerTune-check:isClosed(conn)'";
	}
}
