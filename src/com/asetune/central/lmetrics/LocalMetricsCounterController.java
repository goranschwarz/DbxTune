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
package com.asetune.central.lmetrics;

import java.sql.Timestamp;

import org.apache.log4j.Logger;

import com.asetune.CounterControllerAbstract;
import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CountersModel;
import com.asetune.cm.os.CmOsDiskSpace;
import com.asetune.cm.os.CmOsIostat;
import com.asetune.cm.os.CmOsMeminfo;
import com.asetune.cm.os.CmOsMpstat;
import com.asetune.cm.os.CmOsNwInfo;
import com.asetune.cm.os.CmOsPs;
import com.asetune.cm.os.CmOsUptime;
import com.asetune.cm.os.CmOsVmstat;
import com.asetune.pcs.PersistContainer;
import com.asetune.pcs.PersistContainer.HeaderInfo;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;

public class LocalMetricsCounterController extends CounterControllerAbstract
{
	private static Logger _logger = Logger.getLogger(LocalMetricsCounterController.class);

	/**
	 * The default constructor
	 * @param hasGui should we create a GUI or NoGUI collection thread
	 * @param refreshProcess 
	 * @param pdf 
	 */
	public LocalMetricsCounterController()
	{
		super(false);
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
	public void initCounters(DbxConnection conn, boolean hasGui, long srvVersion, boolean isClusterEnabled, long monTablesVersion)
	throws Exception
	{
		if (isInitialized())
			return;

		Configuration conf = Configuration.getCombinedConfiguration();
		
//		if (conn == null || conn != null && !conn.isConnectionOk(hasGui, _pdf))
//			throw new Exception("Trying to initialize the counters with a connection this seems to be broken.");

			
		if (! isCountersCreated())
			createCounters(hasGui);
		
		_logger.info("Initializing all CM objects.");
//		_logger.info("Initializing all CM objects, using DBMS server version number " + srvVersion + " (" + Ver.versionNumToStr(srvVersion) + "), isClusterEnabled=" + isClusterEnabled + " with monTables Install version " + monTablesVersion + " (" + Ver.versionNumToStr(monTablesVersion) + ").");

		// initialize all the CM's
		for (CountersModel cm : getCmList())
		{
//			_logger.debug("Initializing CM named '" + cm.getName() + "', display name '" + cm.getDisplayName() + "', using ASE server version number " + srvVersion + ".");
			_logger.debug("Initializing CM named '" + cm.getName() + "', display name '" + cm.getDisplayName() + "'.");

			// set the version
//			cm.setServerVersion(monTablesVersion);
//			cm.setClusterEnabled(isClusterEnabled);
			
			// set the active roles, so it can be used in initSql()
//			cm.setActiveServerRolesOrPermissions(conn.getActiveServerRolesOrPermissions());

			// set the ASE Monitor Configuration, so it can be used in initSql() and elsewhere
//			cm.setMonitorConfigs(monitorConfigMap);

			// Enable all added CM's (except if it's disabled)
			// Should this be done here or SOMEWHERE ELSE
			boolean persist     = conf.getBooleanProperty(cm.getName() + ".persistCounters"     , true);
			boolean persistAbs  = conf.getBooleanProperty(cm.getName() + ".persistCounters.abs" , true);
			boolean persistDiff = conf.getBooleanProperty(cm.getName() + ".persistCounters.diff", true);
			boolean persistRate = conf.getBooleanProperty(cm.getName() + ".persistCounters.rate", true);

			cm.setPersistCounters    (persist    , false);
			cm.setPersistCountersAbs (persistAbs , false);
			cm.setPersistCountersDiff(persistDiff, false);
			cm.setPersistCountersRate(persistRate, false);

			// Now when we are connected to a server, and properties are set in the CM, 
			// mark it as runtime initialized (or late initialization)
			// do NOT do this at the end of this method, because some of the above stuff
			// will be used by the below method calls.
			cm.setRuntimeInitialized(true);

			// Initializes SQL, use getServerVersion to check what we are connected to.
//			cm.initSql(conn);

			// Use this method if we need to check anything in the database.
			// for example "deadlock pipe" may not be active...
			// If server version is below 15.0.2 statement cache info should not be VISABLE
//			cm.init(conn);
			
			// Initialize graphs for the version you just connected to
			// This will simply enable/disable graphs that should not be visible for the ASE version we just connected to
			cm.initTrendGraphForVersion(monTablesVersion);
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

		_logger.info("Creating ALL Local Monitor CM Objects.");

		ICounterController counterController = this;
		IGuiController     guiController     = null;

		// H2 ???

		// OS HOST Monitoring
		CmOsIostat          .create(counterController, guiController);
		CmOsVmstat          .create(counterController, guiController);
		CmOsMpstat          .create(counterController, guiController);
		CmOsUptime          .create(counterController, guiController);
		CmOsMeminfo         .create(counterController, guiController);
		CmOsNwInfo          .create(counterController, guiController);
		CmOsDiskSpace       .create(counterController, guiController);
		CmOsPs              .create(counterController, guiController);

		// USER DEFINED COUNTERS
		createUserDefinedCounterModelHostMonitors(counterController, guiController);

		// done
		setCountersIsCreated(true);
	}


	@Override
	public PersistContainer.HeaderInfo createPcsHeaderInfo()
	{
		Timestamp mainSampleTime   = new Timestamp(System.currentTimeMillis());
		String    dbmsServerName   = "DbxCentral";     // Used when storing information in Persistent Counter Store 
		String    dbmsHostname     = "localhost";      // Used when storing information in Persistent Counter Store
		Timestamp counterClearTime = new Timestamp(0);

		return new HeaderInfo(mainSampleTime, dbmsServerName, dbmsHostname, counterClearTime);
	}


	@Override
	public void checkServerSpecifics()
	{
	}

	@Override
	public String getServerTimeCmd()
	{
		return "select CURRENT_TIMESTAMP \n";
	}

	@Override
	protected String getIsClosedSql()
	{
		return "select 'DbxCentral-LocalMetrics-check:isClosed(conn)'";
	}
}
