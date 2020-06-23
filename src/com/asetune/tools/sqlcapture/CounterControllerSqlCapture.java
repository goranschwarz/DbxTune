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
package com.asetune.tools.sqlcapture;

import java.util.Map;

import org.apache.log4j.Logger;

import com.asetune.CounterControllerAbstract;
import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CountersModel;
import com.asetune.cm.ase.ToolTipSupplierAse;
import com.asetune.gui.swing.GTable.ITableTooltip;
import com.asetune.pcs.PersistContainer;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.Ver;

public class CounterControllerSqlCapture extends CounterControllerAbstract
{
	/** Log4j logging. */
	private static Logger _logger = Logger.getLogger(CounterControllerSqlCapture.class);

//	/** This is a input to the SplashScreen */
//	public static final int	   NUMBER_OF_PERFORMANCE_COUNTERS	= 54 + 10 + 20; 
//	// 10 extra steps not for performance counters 
//	// 20 extra for init time of XX seconds or so

	private ProcessDetailFrame _pdf = null;
//	private RefreshProcess     _refreshProcess = null;
	/**
	 * The default constructor
	 * @param hasGui should we create a GUI or NoGUI collection thread
	 * @param refreshProcess 
	 * @param pdf 
	 */
	public CounterControllerSqlCapture(boolean hasGui, RefreshProcess refreshProcess, ProcessDetailFrame pdf)
	{
		super(hasGui);
//		_refreshProcess = refreshProcess;
		_pdf = pdf;
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
		super.reset(resetAllCms);
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

//		if ( ! AseConnectionUtils.isConnectionOk(conn, hasGui, null))
		if (conn == null || conn != null && !conn.isConnectionOk(hasGui, _pdf))
			throw new Exception("Trying to initialize the counters with a connection this seems to be broken.");

			
		if (! isCountersCreated())
			createCounters(hasGui);
		
		_logger.info("Initializing all CM objects, using ASE server version number "+srvVersion+" ("+Ver.versionNumToStr(srvVersion)+"), isClusterEnabled="+isClusterEnabled+" with monTables Install version "+monTablesVersion+" ("+Ver.versionNumToStr(monTablesVersion)+").");

		// Get active Monitor Configuration
		Map<String,Integer> monitorConfigMap = AseConnectionUtils.getMonitorConfigs(conn);

//		// Get some specific configurations
//		if (srvVersion >= Ver.ver(15,0,3,1))
//			_config_captureMissingStatistics = AseConnectionUtils.getAseConfigRunValueBooleanNoEx(conn, "capture missing statistics");
//
//		if (srvVersion >= Ver.ver(15,0,2))
//			_config_enableMetricsCapture = AseConnectionUtils.getAseConfigRunValueBooleanNoEx(conn, "enable metrics capture");
//
//		_config_threadedKernelMode = false;
//		if (srvVersion >= Ver.ver(15,7))
//		{
//			String kernelMode = AseConnectionUtils.getAseConfigRunValueStrNoEx(conn, "kernel mode");
//			_config_threadedKernelMode = "threaded".equals(kernelMode);
//		}

		
//		// in version 15.0.3.1 compatibility_mode was introduced, this to use 12.5.4 optimizer & exec engine
//		// This will hurt performance, especially when querying sysmonitors table, so set this to off
//		if (srvVersion >= Ver.ver(15,0,3,1))
//			AseConnectionUtils.setCompatibilityMode(conn, false);

		// initialize all the CM's
		for (CountersModel cm : getCmList())
		{
			_logger.debug("Initializing CM named '"+cm.getName()+"', display name '"+cm.getDisplayName()+"', using ASE server version number "+srvVersion+".");

			// set the version
			cm.setServerVersion(monTablesVersion);
			cm.setClusterEnabled(isClusterEnabled);
			
			// set the active roles, so it can be used in initSql()
			cm.setActiveServerRolesOrPermissions(conn.getActiveServerRolesOrPermissions());

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

		_logger.info("Creating ALL CM Objects.");

		ICounterController counterController = this;
		IGuiController     guiController     = _pdf;
                           
		CmProcessObjects .create(counterController, guiController);
		CmProcessWaits   .create(counterController, guiController);
		CmLocks          .create(counterController, guiController);

		// done
		setCountersIsCreated(true);
	}


	@Override
	public PersistContainer.HeaderInfo createPcsHeaderInfo()
	{
		return null;
	}


	@Override
	public void checkServerSpecifics()
	{
		//----------------------
		// In some versions we need to check if the transaction log is full to some reasons
		// If it is full it will be truncated.
		//----------------------
//		checkForFullTransLogInMaster(getMonConnection());
	}

	@Override
	public String getServerTimeCmd()
	{
		return "select getdate() \n";
	}

	@Override
	protected String getIsClosedSql()
	{
		return "select 'AseTune-SqlCapture-check:isClosed(conn)'";
	}

	@Override
	public ITableTooltip createCmToolTipSupplier(CountersModel cm)
	{
		return new ToolTipSupplierAse(cm);
	}
}
