/*******************************************************************************
 * Copyright (C) 2010-2027 Goran Schwarz
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
package com.dbxtune;

import java.lang.invoke.MethodHandles;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.db2.CmActiveStatements;
import com.dbxtune.cm.db2.CmBackups;
import com.dbxtune.cm.db2.CmConnectionSummary;
import com.dbxtune.cm.db2.CmErrorLog;
import com.dbxtune.cm.db2.CmHadr;
import com.dbxtune.cm.db2.CmMemory;
import com.dbxtune.cm.db2.CmMonGetBufferPool;
import com.dbxtune.cm.db2.CmMonGetConnection;
import com.dbxtune.cm.db2.CmMonGetIndex;
import com.dbxtune.cm.db2.CmMonGetTable;
import com.dbxtune.cm.db2.CmStatements;
import com.dbxtune.cm.db2.CmStoragePaths;
import com.dbxtune.cm.db2.CmSummary;
import com.dbxtune.cm.db2.CmSystemResourses;
import com.dbxtune.cm.db2.CmTableSpace;
import com.dbxtune.cm.db2.CmTableSize;
import com.dbxtune.cm.db2.CmTableSpaceAll;
import com.dbxtune.cm.db2.CmTimeSpent;
import com.dbxtune.cm.db2.CmTransactionLog;
import com.dbxtune.cm.db2.CmUtilities;
import com.dbxtune.cm.db2.Db2CmUtils;
import com.dbxtune.cm.os.CmOsDiskSpace;
import com.dbxtune.cm.os.CmOsIostat;
import com.dbxtune.cm.os.CmOsMeminfo;
import com.dbxtune.cm.os.CmOsMpstat;
import com.dbxtune.cm.os.CmOsNwInfo;
import com.dbxtune.cm.os.CmOsPs;
import com.dbxtune.cm.os.CmOsUptime;
import com.dbxtune.cm.os.CmOsVmstat;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.pcs.PersistContainer;
import com.dbxtune.pcs.PersistContainer.HeaderInfo;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfoDb2;
import com.dbxtune.utils.AseConnectionUtils;
import com.dbxtune.utils.Ver;


public class CounterControllerDb2
extends CounterControllerAbstract
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public static final int	   NUMBER_OF_PERFORMANCE_COUNTERS	= 54 + 10 + 20;

	/**
	 * The monitoring functions the Counter Models use. The login needs EXECUTE on them (see DbmsSetup/db2.txt, which grants them to the role DBXTUNE_MON).
	 * <p>
	 * They are checked once at connect, so the log says what is missing (each CM also checks its own, and is disabled if it can't use them).
	 */
	private static final String[] MONITOR_FUNCTIONS = new String[] {
			"MON_GET_DATABASE(-2)",
			"MON_GET_DATABASE_DETAILS(-2)",
			"MON_FORMAT_XML_TIMES_BY_ROW(CAST(NULL AS BLOB(8M)))",
			"MON_GET_INSTANCE(-2)",
			"MON_GET_TRANSACTION_LOG(-2)",
			"MON_GET_UNIT_OF_WORK(null, -2)",
			"MON_GET_CONNECTION(null, -2)",
			"MON_GET_ACTIVITY(null, -2)",
			"MON_GET_APPL_LOCKWAIT(null, -2)",
			"MON_GET_LOCKS(null, -2)",
			"MON_GET_PKG_CACHE_STMT(null, null, null, -2)",
			"MON_GET_TABLESPACE('', -2)",
			"MON_GET_CONTAINER('', -2)",
			"MON_GET_BUFFERPOOL('', -2)",
			"MON_GET_TABLE('', '', -2)",
			"MON_GET_INDEX('', '', -2)",
			"MON_GET_MEMORY_POOL(null, null, -2)",
			"MON_GET_MEMORY_SET(null, null, -2)",
			"MON_GET_HADR(-2)",
			"MON_GET_UTILITY(-2)",
			"ENV_GET_SYSTEM_RESOURCES()",
			"ADMIN_GET_STORAGE_PATHS('', -2)",
	};

	/**
	 * The default constructor
	 * @param hasGui should we create a GUI or NoGUI collection thread
	 */
	public CounterControllerDb2(boolean hasGui)
	{
		super(hasGui);
	}


	/**
	 * This will be used to create all the CountersModel objects.
	 * <p>
	 * If we are in GUI mode the graphical objects would also be
	 * created and added to the GUI system, this so they will be showned
	 * in the GUI before we are connected to any DBMS Server.
	 *  <p>
	 * initCounters() would be called on "connect" to be able to
	 * initialize the counter object using a specific DBMS release
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

		// Summary panel (the counters/values on the left side)
		CmSummary           .create(counterController, guiController);

		// Server
		CmConnectionSummary .create(counterController, guiController);
		CmMonGetConnection  .create(counterController, guiController);
		CmSystemResourses   .create(counterController, guiController);
		CmTransactionLog    .create(counterController, guiController);
		CmTimeSpent         .create(counterController, guiController);
		CmTableSpace        .create(counterController, guiController);
		CmTableSpaceAll     .create(counterController, guiController);
		CmStoragePaths      .create(counterController, guiController);
		CmHadr              .create(counterController, guiController);
		CmBackups           .create(counterController, guiController);
		CmUtilities         .create(counterController, guiController);
		CmErrorLog          .create(counterController, guiController);

		// Object Access
		CmActiveStatements  .create(counterController, guiController);
		CmStatements        .create(counterController, guiController);
		CmMonGetTable       .create(counterController, guiController);
		CmMonGetIndex       .create(counterController, guiController);
		CmTableSize         .create(counterController, guiController);

		// Cache
		CmMonGetBufferPool  .create(counterController, guiController);
		CmMemory            .create(counterController, guiController);

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
		createUserDefinedCounterModels(counterController, guiController);
		createUserDefinedCounterModelHostMonitors(counterController, guiController);

		// done
		setCountersIsCreated(true);
	}

	/**
	 * When we have a database connection, lets do some extra init this
	 * so all the CountersModel can decide what SQL to use. <br>
	 * SQL statement usually depends on what DBMS Server version we monitor.
	 *
	 * @param conn
	 * @param hasGui              is this initialized with a GUI?
	 * @param srvVersion          what is the Servers Executable version
	 * @param isClusterEnabled    is it a cluster
	 * @param monTablesVersion    what version of the monitor tables should we use
	 */
	@Override
	public void initCounters(DbxConnection conn, boolean hasGui, long srvVersion, boolean isClusterEnabled, long monTablesVersion)
	throws Exception
	{
		if (isInitialized())
			return;

		if ( ! AseConnectionUtils.isConnectionOk(conn, hasGui, null))
			throw new Exception("Trying to initialize the counters with a connection this seems to be broken.");

		if (! isCountersCreated())
			createCounters(hasGui);

		// What can this login do? The CM's only need EXECUTE on the monitoring functions (see DbmsSetup/db2.txt)
		// Tested by doing it (Db2 checks EXECUTE when the statement is compiled, so 'where 1=2' is enough), so privileges that comes from a ROLE or SQLADM are also counted
		List<String> missing = new ArrayList<>();
		for (String func : MONITOR_FUNCTIONS)
		{
			if ( ! canExecute(conn, "select 1 from table(" + func + ") t where 1=2") )
				missing.add(func.substring(0, func.indexOf('(')));
		}
		if (missing.isEmpty())
		{
			_logger.info("The Db2 login has all it needs: EXECUTE on all " + MONITOR_FUNCTIONS.length + " monitoring functions.");
		}
		else
		{
			StringBuilder fix = new StringBuilder();
			for (String name : missing)
				fix.append("GRANT EXECUTE ON FUNCTION SYSPROC.").append(name).append(" TO ROLE ").append(Db2CmUtils.MONITOR_ROLE).append("; ");
			_logger.warn("The Db2 login is missing EXECUTE on " + missing.size() + " of " + MONITOR_FUNCTIONS.length + " monitoring functions, CM's that can't use them will be DISABLED. Missing: " + missing
					+ ". Fix: " + fix + " (or: GRANT SQLADM ON DATABASE TO USER <user>;) See DbmsSetup/db2.txt");
		}

		// Things in the database configuration that makes Db2Tune show less
		if (conn.getDbmsVersionInfo() instanceof DbmsVersionInfoDb2)
		{
			DbmsVersionInfoDb2 versionInfo = (DbmsVersionInfoDb2) conn.getDbmsVersionInfo();
			String dbname = versionInfo.getDbName();

			// MON_REQ_METRICS / MON_ACT_METRICS / MON_OBJ_METRICS = NONE: the counters in the MON_GET_* functions are not collected (they stay 0)
			if ("NONE".equalsIgnoreCase(versionInfo.getMonReqMetrics())) _logger.warn("The database configuration MON_REQ_METRICS is NONE, so the request metrics (time spent, waits, rows, CPU) are not collected, and many Counter Models will show zeros. Fix: UPDATE DB CFG FOR " + dbname + " USING MON_REQ_METRICS BASE");
			if ("NONE".equalsIgnoreCase(versionInfo.getMonActMetrics())) _logger.warn("The database configuration MON_ACT_METRICS is NONE, so the activity metrics (statements in the package cache, running statements) are not collected. Fix: UPDATE DB CFG FOR " + dbname + " USING MON_ACT_METRICS BASE");
			if ("NONE".equalsIgnoreCase(versionInfo.getMonObjMetrics())) _logger.warn("The database configuration MON_OBJ_METRICS is NONE, so the object metrics (buffer pools, table spaces, tables) are not collected. Fix: UPDATE DB CFG FOR " + dbname + " USING MON_OBJ_METRICS BASE");

			// Not activated: when the last application disconnects, the database is deactivated, and all counters (and the package cache) are lost
			// Db2Tune's own connection keeps it active, but if Db2Tune is restarted, it starts from zero
			if (versionInfo.getActivationState() != null && ! versionInfo.isExplicitlyActivated())
				_logger.info("The database '" + dbname + "' is not explicitly activated (DB_ACTIVATION_STATE=" + versionInfo.getActivationState() + "). It is deactivated, and all monitor counters and the package cache are lost, when the last connection leaves. "
						+ "Db2Tune's own connection keeps it active while it runs. Recommended: ACTIVATE DATABASE " + dbname);

			// Plans of running statements: only if the explain tables are there (Db2Tune never creates objects in the monitored database)
			if (versionInfo.hasExplainTables())
				_logger.info("Execution plans of running statements will be captured: the explain tables were found in schema '" + versionInfo.getExplainSchema() + "' (the login also needs EXECUTE on SYSPROC.EXPLAIN_FROM_SECTION).");
			else
				_logger.info("Execution plans of running statements will NOT be captured: no explain tables were found in the login's schema '" + versionInfo.getSessionUser() + "' or in SYSTOOLS. "
						+ "To turn it on (no data access is needed), see DbmsSetup/db2.txt: CALL SYSPROC.SYSINSTALLOBJECTS('EXPLAIN', 'C', 'USERSPACE1', '" + versionInfo.getSessionUser() + "')");

			if (versionInfo.isPureScale() || versionInfo.isDpf())
				_logger.warn("This is a " + (versionInfo.isPureScale() ? "pureScale (" + versionInfo.getNumMembers() + " members)" : "partitioned DPF (" + versionInfo.getNumDbPartitions() + " partitions)") + " system. "
						+ "Db2Tune reads all members (MEMBER = -2), but it has only been tested with a single member.");
		}

		_logger.info("Initializing all CM objects, using DB2 version number " + srvVersion + " (" + Ver.versionNumToStr(srvVersion) + ").");

		// initialize all the CM's
		for (CountersModel cm : getCmList())
		{
			_logger.debug("Initializing CM named '" + cm.getName() + "', display name '" + cm.getDisplayName() + "', using DB2 version number " + srvVersion + ".");

			// set the version
			cm.setServerVersion(monTablesVersion);
			cm.setClusterEnabled(isClusterEnabled);

			// Now when we are connected to a server, and properties are set in the CM,
			// mark it as runtime initialized (or late initialization)
			// do NOT do this at the end of this method, because some of the above stuff
			// will be used by the below method calls.
			cm.setRuntimeInitialized(true);

			// Initializes SQL, use getServerVersion to check what we are connected to.
			cm.initSql(conn);

			// Use this method if we need to check anything in the database.
			cm.init(conn);

			// Initialize graphs for the version you just connected to
			// This will simply enable/disable graphs that should not be visible for the version we just connected to
			cm.initTrendGraphForVersion(monTablesVersion);
		}

		setInitialized(true);
	}

	/** Can the login execute this? (any error is a NO) */
	private static boolean canExecute(DbxConnection conn, String sql)
	{
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while (rs.next())
				;
			return true;
		}
		catch (SQLException ex)
		{
			_logger.debug("canExecute(): sql=|" + sql + "|, Caught: " + ex);
			return false;
		}
	}

	@Override
	public void checkServerSpecifics()
	{
		// If we want to check any DBMS specifics at the TOP of every "check loop"
	}

	@Override
	public PersistContainer.HeaderInfo createPcsHeaderInfo()
	{
		// Get session/head info
		Timestamp mainSampleTime   = null;
		String    dbmsServerName   = null;     // Used when storing information in Persistent Counter Store
		String    dbmsHostname     = null;     // Used when storing information in Persistent Counter Store
		Timestamp counterClearTime = new Timestamp(0);

		// NOTE: ENV_GET_INST_INFO() and ENV_GET_SYS_INFO() can be used by PUBLIC (tested on 11.5.8)
		// NOTE: The sample time is Db2's clock in UTC, read as UTC: so it is the right time also when Db2 runs in another time zone than the collector.
		//       CURRENT TIMESTAMP is Db2's local time without a zone, read as is it would be taken as the collector's local time (a Db2 in UTC and a
		//       collector in UTC+2 put every sample 2 hours back, and DbxCentral could not find the samples at the browser's "now").
		//       Only this "session sample time" is changed: the CMs' own sample time (getServerTimeCmd) stays Db2's local time, the same clock as the timestamps in the CMs.
		String sql = ""
				+ "select \n"
				+ "    current_timestamp - current timezone as UTC_TIME, \n"
				+ "    (select INST_NAME from table(SYSPROC.ENV_GET_INST_INFO())) as Instance, \n"
				+ "    (select HOST_NAME from table(SYSPROC.ENV_GET_SYS_INFO()) ) as onHost \n"
				+ "from SYSIBM.SYSDUMMY1";

		try
		{
			if ( ! isMonConnected(true, true) ) // forceConnectionCheck=true, closeConnOnFailure=true
				return null;

			Statement stmt = getMonConnection().createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next())
			{
				mainSampleTime   = rs.getTimestamp(1, Calendar.getInstance(TimeZone.getTimeZone("UTC")));
				dbmsServerName   = rs.getString(2);
				dbmsHostname     = rs.getString(3);
			}
			rs.close();
			stmt.close();
		}
		catch (SQLException sqlex)
		{
			// Is the connection lost?
			boolean forceConnectionCheck = true;
			boolean closeConnOnFailure   = true;
			if ( ! isMonConnected(forceConnectionCheck, closeConnOnFailure) )
			{
				_logger.info("Problems getting basic status info in 'Counter get loop', the connection is lost. So lets start from the top. Caught: " + sqlex);
				return null;
			}

			_logger.warn("Problems getting basic status info in 'Counter get loop', reverting back to 'static values'. SQL '" + sql + "', Caught: " + sqlex.toString() );
			mainSampleTime   = new Timestamp(System.currentTimeMillis());
			dbmsServerName   = "unknown";
			dbmsHostname     = "unknown";
			counterClearTime = new Timestamp(0);
		}

		return new HeaderInfo(mainSampleTime, dbmsServerName, dbmsHostname, counterClearTime);
	}

	@Override
	public String getServerTimeCmd()
	{
		return "SELECT CURRENT_TIMESTAMP from SYSIBM.SYSDUMMY1 \n";
	}

	@Override
	protected String getIsClosedSql()
	{
		return "SELECT 'Db2Tune-check:isClosed(conn)' from SYSIBM.SYSDUMMY1";
	}

	@Override
	public boolean isSqlBatchingSupported()
	{
		return false;
	}

	/**
	 * Called by the NO-GUI collector when connect fails. Throwing an Exception here STOPS the collector (it is a non-recoverable error),
	 * so only do that when a retry can never work: wrong user or password.
	 * <p>
	 * Tested with the Db2 JDBC driver 4.38.7 against 11.5.8:
	 * <ul>
	 *   <li>Wrong user or password: <code>ERRORCODE=-4214, SQLSTATE=28000</code> "Connection authorization failure occurred. Reason: User ID or Password invalid." (also an expired password)</li>
	 *   <li>Server down: <code>ERRORCODE=-4499, SQLSTATE=08001</code> "Connection refused" -- retried (before, every -4499 was taken as a wrong password, and the collector stopped when Db2 was down)</li>
	 *   <li>Wrong database name: <code>ERRORCODE=-4499, SQLSTATE=08004</code> "...which was either not found..." -- retried (and logged), a database being restored or cataloged can come back</li>
	 * </ul>
	 * The password is never part of the message.
	 */
	@Override
	public void noGuiConnectErrorHandler(SQLException ex, String dbmsUsername, String dbmsPassword, String dbmsServer, String dbmsHostPortStr, String jdbcUrlOptions)
	throws Exception
	{
		if ("28000".equals(ex.getSQLState()) || ex.getErrorCode() == -4214)
		{
			throw new Exception("The error message suggest that the wrong USER '" + dbmsUsername + "' or PASSWORD (or an expired password) was used to connect to DBMS server '" + dbmsServer + "'. This is a non-recoverable error. DBMS Error Message='" + ex.getMessage() + "'.", ex);
		}
	}
}
