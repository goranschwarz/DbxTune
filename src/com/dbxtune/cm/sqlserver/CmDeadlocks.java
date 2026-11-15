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
package com.dbxtune.cm.sqlserver;

import java.io.StringReader;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.NameNotFoundException;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.h2.tools.SimpleResultSet;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CounterSample;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.CountersModelAppend;
import com.dbxtune.cm.NoValidRowsInSample;
import com.dbxtune.config.dict.MonTablesDictionary;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.gui.TabularCntrPanelAppend;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.sql.conn.info.DbmsVersionInfoSqlServer;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;

/**
 * Deadlocks, one row per deadlock, read from the <code>xml_deadlock_report</code> events in the <code>system_health</code> Extended Events files.
 * <p>
 * How it works:
 * <ul>
 *   <li>Reads only when the deadlock counter in {@link CmSummary} went up (and a few minutes after, since XE writes the events to file with a delay).</li>
 *   <li>Reads only the events that are new since the previous read: <code>fn_xe_file_target_read_file(..., file_name, file_offset)</code> of the last event read.
 *       The first read (no file/offset yet) reads all files, and keeps the events of the last ## minutes.</li>
 *   <li>Parses the deadlock XML in Java (much cheaper than doing it in T-SQL like sp_BlitzLock does),
 *       and saves summary columns plus the deadlock XML (without the native call stacks).</li>
 * </ul>
 * The rows are used by the deadlock alarm in {@link CmSummary} and by the Daily Summary Report.
 */
public class CmDeadlocks
extends CountersModelAppend
{
	private static final Logger  _logger          = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmDeadlocks.class.getSimpleName();
	public static final String   SHORT_NAME       = "Deadlocks";
	public static final String   HTML_DESC        =
		"<html>" +
		"<p>Deadlocks, one row per deadlock (from the 'system_health' Extended Events session).</p>" +
		"<p>Only read when the deadlock counter went up, and only the events that are new since the previous read.</p>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/CmDeadlock.png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"fn_xe_file_target_read_file"};
	public static final String[] NEED_ROLES       = new String[] {"VIEW SERVER STATE"};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = 10;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public Type    getTemplateLevel()                       { return Type.SMALL; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '" + CM_NAME + "'");

		return new CmDeadlocks(counterController, guiController);
	}

	public CmDeadlocks(ICounterController counterController, IGuiController guiController)
	{
		super(counterController, guiController, CM_NAME, GROUP_NAME, null, MON_TABLES, NEED_ROLES, NEED_CONFIG, NEED_SRV_VERSION, NEED_CE_VERSION, IS_SYSTEM_CM);

		setDisplayName(SHORT_NAME);
		setDescription(HTML_DESC);

		setIconFile(GUI_ICON_FILE);

		setCounterController(counterController);
		setGuiController(guiController);

		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	private static final String  PROP_PREFIX                      = CM_NAME;

	public static final String  PROPKEY_sample_firstReadMinutes   = PROP_PREFIX + ".sample.firstReadMinutes";
	public static final int     DEFAULT_sample_firstReadMinutes   = 15;

	public static final String  PROPKEY_sample_pendingReadSeconds = PROP_PREFIX + ".sample.pendingReadSeconds";
	public static final int     DEFAULT_sample_pendingReadSeconds = 300;

	public static final String  PROPKEY_sample_maxRowsPerSample   = PROP_PREFIX + ".sample.maxRowsPerSample";
	public static final int     DEFAULT_sample_maxRowsPerSample   = 500;

	public static final String  PROPKEY_sample_maxErrors          = PROP_PREFIX + ".sample.maxErrors";
	public static final int     DEFAULT_sample_maxErrors          = 3;

	public static final String  TOOLTIP_sample_firstReadMinutes   = "The first read (no previous read to continue from) keeps deadlocks from the last ## minutes.";
	public static final String  TOOLTIP_sample_pendingReadSeconds = "Read for ## seconds after the deadlock counter went up (Extended Events writes the events to file with a delay).";
	public static final String  TOOLTIP_sample_maxRowsPerSample   = "Save at most ## deadlocks per sample (the rest are only counted in the log).";
	public static final String  TOOLTIP_sample_maxErrors          = "After ## failed or timed out reads in a row, this CM is turned off (enable it again to retry).";

	@Override
	protected void registerDefaultValues()
	{
		super.registerDefaultValues();

		Configuration.registerDefaultValue(PROPKEY_sample_firstReadMinutes,   DEFAULT_sample_firstReadMinutes);
		Configuration.registerDefaultValue(PROPKEY_sample_pendingReadSeconds, DEFAULT_sample_pendingReadSeconds);
		Configuration.registerDefaultValue(PROPKEY_sample_maxRowsPerSample,   DEFAULT_sample_maxRowsPerSample);
		Configuration.registerDefaultValue(PROPKEY_sample_maxErrors,          DEFAULT_sample_maxErrors);
	}

	/** Used by the: Create 'Offline Session' Wizard, and the collector settings */
	@Override
	public List<CmSettingsHelper> getLocalSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		list.add(new CmSettingsHelper("First Read Minutes"          , PROPKEY_sample_firstReadMinutes  , Integer.class, conf.getIntProperty(PROPKEY_sample_firstReadMinutes  , DEFAULT_sample_firstReadMinutes  ), DEFAULT_sample_firstReadMinutes  , TOOLTIP_sample_firstReadMinutes  ));
		list.add(new CmSettingsHelper("Read Seconds After Counter"  , PROPKEY_sample_pendingReadSeconds, Integer.class, conf.getIntProperty(PROPKEY_sample_pendingReadSeconds, DEFAULT_sample_pendingReadSeconds), DEFAULT_sample_pendingReadSeconds, TOOLTIP_sample_pendingReadSeconds));
		list.add(new CmSettingsHelper("Max Rows Per Sample"         , PROPKEY_sample_maxRowsPerSample  , Integer.class, conf.getIntProperty(PROPKEY_sample_maxRowsPerSample  , DEFAULT_sample_maxRowsPerSample  ), DEFAULT_sample_maxRowsPerSample  , TOOLTIP_sample_maxRowsPerSample  ));
		list.add(new CmSettingsHelper("Max Errors Before Turning Off", PROPKEY_sample_maxErrors        , Integer.class, conf.getIntProperty(PROPKEY_sample_maxErrors          , DEFAULT_sample_maxErrors          ), DEFAULT_sample_maxErrors          , TOOLTIP_sample_maxErrors          ));

		return list;
	}

	@Override
	protected TabularCntrPanel createGui()
	{
		return new TabularCntrPanelAppend(this);
	}

	@Override
	public boolean checkDependsOnVersion(DbxConnection conn)
	{
		DbmsVersionInfoSqlServer versionInfo = (DbmsVersionInfoSqlServer) conn.getDbmsVersionInfo();
		if (versionInfo.isAzureDb() || versionInfo.isAzureSynapseAnalytics())
		{
			_logger.warn("When trying to initialize Counters Model '" + getName() + "', named '" + getDisplayName() + "', connected to Azure SQL Database or Analytics, which do NOT have the 'system_health' Extended Events files.");

			setActive(false, "This info is NOT available in Azure SQL Database or Azure Synapse/Analytics.");

			TabularCntrPanel tcp = getTabPanel();
			if (tcp != null)
				tcp.setToolTipText("This info is NOT available in Azure SQL Database or Azure Synapse/Analytics.");

			return false;
		}

		return super.checkDependsOnVersion(conn);
	}

	@Override
	public String[] getDependsOnConfigForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return NEED_CONFIG;
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			String cmName = this.getName();
			mtd.addTable(cmName, HTML_DESC);

			mtd.addColumn(cmName, "EventTime"       , "<html>When the deadlock was detected (server local time).</html>");
			mtd.addColumn(cmName, "DeadlockId"      , "<html>Unique id of the deadlock: event time (UTC) and the victim's process id.</html>");
			mtd.addColumn(cmName, "DbName"          , "<html>Database of the victim.</html>");
			mtd.addColumn(cmName, "ProcessCount"    , "<html>Number of sessions (spids) in the deadlock.</html>");
			mtd.addColumn(cmName, "IsParallel"      , "<html>A parallel (intra-query) deadlock: exchange events or sync points in the resource list.</html>");
			mtd.addColumn(cmName, "VictimSpid"      , "<html>Session id of the victim (it got error 1205).</html>");
			mtd.addColumn(cmName, "VictimLogin"     , "<html>Login of the victim.</html>");
			mtd.addColumn(cmName, "VictimHost"      , "<html>Host name of the victim.</html>");
			mtd.addColumn(cmName, "VictimApp"       , "<html>Application name of the victim.</html>");
			mtd.addColumn(cmName, "VictimProc"      , "<html>Procedure (and line) the victim was executing, if any.</html>");
			mtd.addColumn(cmName, "VictimStatement" , "<html>Statement the victim was executing (input buffer if the statement text is not available).</html>");
			mtd.addColumn(cmName, "OtherSpids"      , "<html>Session id's of the other sessions.</html>");
			mtd.addColumn(cmName, "OtherProcs"      , "<html>Procedures the other sessions were executing.</html>");
			mtd.addColumn(cmName, "OtherStatements" , "<html>Statements the other sessions were executing.</html>");
			mtd.addColumn(cmName, "Objects"         , "<html>Objects (tables) the locks were on.</html>");
			mtd.addColumn(cmName, "LockTypes"       , "<html>Lock resource types (keylock, pagelock, objectlock, ...).</html>");
			mtd.addColumn(cmName, "IsolationLevels" , "<html>Transaction isolation levels of the sessions.</html>");
			mtd.addColumn(cmName, "MaxWaitTimeMs"   , "<html>Longest wait (ms) of the sessions when the deadlock was detected.</html>");
			mtd.addColumn(cmName, "ShapeKey"        , "<html>Deadlocks with the same objects and procedures/statements have the same key (used to group them).</html>");
			mtd.addColumn(cmName, "DeadlockXml"     , "<html>The deadlock report XML (without native call stacks). Save it as a <code>.xdl</code> file to open it in SSMS.</html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e);
		}
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		// The SQL is executed in CounterSamplePrivate.getSample() -> createResultSet()
		return "-- reads 'xml_deadlock_report' from 'system_health*.xel', see CmDeadlocks.createResultSet()";
	}

	@Override
	public CounterSample createCounterSample(String name, boolean negativeDiffCountersToZero, String[] diffColumns, CounterSample prevSample)
	{
		return new CounterSamplePrivate(name, negativeDiffCountersToZero, diffColumns, prevSample);
	}

	private static class CounterSamplePrivate
	extends CounterSample
	{
		private static final long serialVersionUID = 1L;

		public CounterSamplePrivate(String name, boolean negativeDiffCountersToZero, String[] diffColumns, CounterSample prevSample)
		{
			super(name, negativeDiffCountersToZero, diffColumns, prevSample);
		}

		@Override
		public boolean getSample(CountersModel cm, DbxConnection conn, String sql, List<String> pkList)
		throws SQLException, NoValidRowsInSample
		{
			// set the sample time (from the server) and interval
			updateSampleTime(conn, cm);

			_rows = new ArrayList<List<Object>>();

			ResultSet rs = ((CmDeadlocks)cm).createResultSet(conn);

			ResultSetMetaData originRsmd = rs.getMetaData();
			if ( ! cm.hasResultSetMetaData() )
				cm.setResultSetMetaData( cm.createResultSetMetaData(originRsmd) );

			if (readResultset(cm, rs, cm.getResultSetMetaData(), originRsmd, pkList, 0))
				rs.close();

			return true;
		}
	}


	//------------------------------------------------------------
	// State between samples
	//------------------------------------------------------------
	/** file_name of the last event read (null = no read yet) */
	private String  _bookmarkFile;
	/** file_offset of the last event read */
	private long    _bookmarkOffset;
	/** DeadlockId's from the latest reads (events in the bookmark buffer may come back) */
	private Set<String> _recentIds = new LinkedHashSet<>();
	private static final int RECENT_IDS_MAX = 2000;

	/** System time when the deadlock counter in CmSummary last went up (0 = never) */
	private long    _lastCounterMoveTime;
	/** Failed reads in a row */
	private int     _errorCount;

	@Override
	public void close()
	{
		_bookmarkFile        = null;
		_bookmarkOffset      = 0;
		_recentIds.clear();
		_lastCounterMoveTime = 0;
		_errorCount          = 0;

		super.close();
	}

	/**
	 * Read the new deadlock events (if the deadlock counter moved), parse them, and return one row per deadlock as a ResultSet.
	 */
	private ResultSet createResultSet(DbxConnection conn)
	throws SQLException
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		int firstReadMinutes   = conf.getIntProperty(PROPKEY_sample_firstReadMinutes,   DEFAULT_sample_firstReadMinutes);
		int pendingReadSeconds = conf.getIntProperty(PROPKEY_sample_pendingReadSeconds, DEFAULT_sample_pendingReadSeconds);
		int maxRowsPerSample   = conf.getIntProperty(PROPKEY_sample_maxRowsPerSample,   DEFAULT_sample_maxRowsPerSample);
		int maxErrors          = conf.getIntProperty(PROPKEY_sample_maxErrors,          DEFAULT_sample_maxErrors);

		SimpleResultSet srs = createEmptyResultSet();

		// Did the deadlock counter go up in this sample? (CmSummary is always refreshed before the other CM's)
		CountersModel cmSummary = getCounterController() == null ? null : getCounterController().getCmByName(CmSummary.CM_NAME);
		if (cmSummary != null && cmSummary.hasDiffData() && cmSummary.getDiffValueAsInteger(0, "deadlockCount", false, 0) > 0)
			_lastCounterMoveTime = System.currentTimeMillis();

		// Read only for a while after the counter moved (the events are written to the file with a delay)
		if (_lastCounterMoveTime == 0 || (System.currentTimeMillis() - _lastCounterMoveTime) > pendingReadSeconds * 1000L)
			return srs;

		long startTime = System.currentTimeMillis();
		List<Deadlock> deadlocks;
		try
		{
			deadlocks = readNewDeadlocks(conn, firstReadMinutes);

			long execTime = System.currentTimeMillis() - startTime;
			if (execTime > getQueryTimeout() * 1000L)
				throw new SQLException("Reading the deadlock events took " + execTime + " ms, which is more than the query timeout of " + getQueryTimeout() + " seconds.");

			_errorCount = 0;
		}
		catch (SQLException ex)
		{
			_errorCount++;
			if (_errorCount >= maxErrors)
			{
				String msg = "Turned off after " + _errorCount + " failed reads in a row. Last problem: " + ex.getMessage();
				_logger.warn(getName() + ": " + msg);
				setActive(false, msg);
				_errorCount = 0;
			}
			throw ex;
		}

		int saved   = 0;
		int skipped = 0;
		for (Deadlock d : deadlocks)
		{
			if (saved >= maxRowsPerSample)
			{
				skipped++;
				continue;
			}
			saved++;
			srs.addRow(
				d.eventTime,
				d.getDeadlockId(),
				d.getVictimDbName(),
				d.getSpids().size(),
				d.parallel,
				d.getVictim() == null ? null : d.getVictim().spid,
				d.getVictim() == null ? null : d.getVictim().login,
				d.getVictim() == null ? null : d.getVictim().host,
				d.getVictim() == null ? null : d.getVictim().app,
				d.getVictim() == null ? null : d.getVictim().getProcNameAndLine(),
				d.getVictim() == null ? null : StringUtil.truncate(d.getVictim().getStatement(), 4000, false, null),
				d.getOtherSpids(),
				d.getOtherProcs(),
				StringUtil.truncate(d.getOtherStatements(), 8000, false, null),
				d.getObjects(),
				d.getLockTypes(),
				d.getIsolationLevels(),
				d.getMaxWaitTimeMs(),
				d.getShapeKey(),
				d.xml
			);
		}
		if (skipped > 0)
			_logger.info(getName() + ": Read " + deadlocks.size() + " deadlocks, saved " + maxRowsPerSample + ", skipped " + skipped + " (" + PROPKEY_sample_maxRowsPerSample + "=" + maxRowsPerSample + ").");

		if (_logger.isDebugEnabled())
			_logger.debug(getName() + ": read " + deadlocks.size() + " new deadlocks in " + (System.currentTimeMillis() - startTime) + " ms. bookmark=" + _bookmarkFile + ":" + _bookmarkOffset);

		return srs;
	}

	private static SimpleResultSet createEmptyResultSet()
	{
		SimpleResultSet srs = new SimpleResultSet();
		srs.addColumn("EventTime",       Types.TIMESTAMP,   0, 0);
		srs.addColumn("DeadlockId",      Types.VARCHAR,    80, 0);
		srs.addColumn("DbName",          Types.VARCHAR,   128, 0);
		srs.addColumn("ProcessCount",    Types.INTEGER,     0, 0);
		srs.addColumn("IsParallel",      Types.BOOLEAN,     0, 0);
		srs.addColumn("VictimSpid",      Types.INTEGER,     0, 0);
		srs.addColumn("VictimLogin",     Types.VARCHAR,   128, 0);
		srs.addColumn("VictimHost",      Types.VARCHAR,   128, 0);
		srs.addColumn("VictimApp",       Types.VARCHAR,   128, 0);
		srs.addColumn("VictimProc",      Types.VARCHAR,   300, 0);
		srs.addColumn("VictimStatement", Types.VARCHAR, Integer.MAX_VALUE, 0);
		srs.addColumn("OtherSpids",      Types.VARCHAR,   200, 0);
		srs.addColumn("OtherProcs",      Types.VARCHAR,  1000, 0);
		srs.addColumn("OtherStatements", Types.VARCHAR, Integer.MAX_VALUE, 0);
		srs.addColumn("Objects",         Types.VARCHAR,  1000, 0);
		srs.addColumn("LockTypes",       Types.VARCHAR,   200, 0);
		srs.addColumn("IsolationLevels", Types.VARCHAR,   200, 0);
		srs.addColumn("MaxWaitTimeMs",   Types.BIGINT,      0, 0);
		srs.addColumn("ShapeKey",        Types.VARCHAR,    20, 0);
		srs.addColumn("DeadlockXml",     Types.VARCHAR, Integer.MAX_VALUE, 0);
		return srs;
	}

	/**
	 * Read the deadlock events that are new since the last read (the bookmark).
	 * If the bookmark file is gone (rolled over), read all files and keep the last ## minutes.
	 */
	private List<Deadlock> readNewDeadlocks(DbxConnection conn, int firstReadMinutes)
	throws SQLException
	{
		if (_bookmarkFile != null)
		{
			try
			{
				return readDeadlocks(conn, _bookmarkFile, _bookmarkOffset, -1);
			}
			catch (SQLException ex)
			{
				// The file we continue from has been removed (rollover), read from the start
				_logger.info(getName() + ": Could not continue reading from file '" + _bookmarkFile + "', offset " + _bookmarkOffset + ". Reading all 'system_health' files instead. Reason: " + ex.getMessage());
				_bookmarkFile   = null;
				_bookmarkOffset = 0;
			}
		}
		return readDeadlocks(conn, null, 0, firstReadMinutes);
	}

	private List<Deadlock> readDeadlocks(DbxConnection conn, String fromFile, long fromOffset, int keepMinutes)
	throws SQLException
	{
		String sql = ""
				+ "select /* ${cmCollectorName} */ \n"
				+ "     file_name \n"
				+ "    ,file_offset \n"
				+ "    ,event_data \n"
				+ "    ,utc_offset_min = datediff(minute, sysutcdatetime(), sysdatetime()) \n"
				+ "    ,srv_utc_now    = sysutcdatetime() \n"
				+ "from sys.fn_xe_file_target_read_file(N'system_health*.xel', NULL, ?, ?) \n"
				+ "where object_name = N'xml_deadlock_report' \n"
				;
		sql = sql.replace("${cmCollectorName}", getName());

		List<Deadlock> list = new ArrayList<>();
		try (PreparedStatement pstmnt = conn.prepareStatement(sql))
		{
			pstmnt.setQueryTimeout(getQueryTimeout());
			if (fromFile == null)
			{
				pstmnt.setNull(1, Types.NVARCHAR);
				pstmnt.setNull(2, Types.BIGINT);
			}
			else
			{
				pstmnt.setString(1, fromFile);
				pstmnt.setLong  (2, fromOffset);
			}

			try (ResultSet rs = pstmnt.executeQuery())
			{
				while (rs.next())
				{
					String    fileName     = rs.getString   (1);
					long      fileOffset   = rs.getLong     (2);
					String    eventData    = rs.getString   (3);
					int       utcOffsetMin = rs.getInt      (4);
					Timestamp srvUtcNow    = rs.getTimestamp(5);

					_bookmarkFile   = fileName;
					_bookmarkOffset = fileOffset;

					Deadlock d;
					try
					{
						d = Deadlock.parseEvent(eventData, utcOffsetMin);
					}
					catch (Exception ex)
					{
						_logger.warn(getName() + ": Problems parsing a deadlock event, skipping it. Caught: " + ex);
						continue;
					}

					// First read: keep only the last ## minutes
					if (keepMinutes > 0 && srvUtcNow != null && d.eventTimeUtc != null)
					{
						Instant srvNow = srvUtcNow.toLocalDateTime().toInstant(ZoneOffset.UTC);
						if (d.eventTimeUtc.isBefore(srvNow.minusSeconds(keepMinutes * 60L)))
							continue;
					}

					// Events in the same buffer as the bookmark can be read again
					if ( ! _recentIds.add(d.getDeadlockId()) )
						continue;

					list.add(d);
				}
			}
		}

		// Keep the "recent" set small
		while (_recentIds.size() > RECENT_IDS_MAX)
		{
			String first = _recentIds.iterator().next();
			_recentIds.remove(first);
		}

		return list;
	}


	//------------------------------------------------------------
	// Parsed deadlock
	//------------------------------------------------------------
	/** One process (session/thread) in a deadlock */
	public static class DlProcess
	{
		public String    id;
		public int       spid;
		public int       ecid;
		public boolean   victim;
		public String    login;
		public String    host;
		public String    app;
		public String    dbName;
		public String    isolationLevel;
		public String    waitResource;
		public String    lockMode;
		public long      waitTimeMs;
		public Timestamp tranStarted;
		public String    tranName;
		public String    procName;  // null for ad hoc SQL
		public int       procLine;
		public String    stmtText;  // text of the top frame (empty on older versions)
		public String    inputBuf;

		/** Statement text, or the input buffer if the statement text is not available */
		public String getStatement()
		{
			if (StringUtil.hasValue(stmtText))
				return stmtText;
			return inputBuf;
		}

		public String getProcNameAndLine()
		{
			if (procName == null)
				return null;
			return procName + (procLine > 0 ? " (line " + procLine + ")" : "");
		}
	}

	/** One locked resource in a deadlock */
	public static class DlResource
	{
		public String       type;       // keylock, pagelock, objectlock, ridlock, exchangeEvent, ...
		public String       objectName;
		public String       indexName;
		public String       mode;
		public List<String> ownerIds    = new ArrayList<>();
		public List<String> ownerModes  = new ArrayList<>();
		public List<String> waiterIds   = new ArrayList<>();
		public List<String> waiterModes = new ArrayList<>();

		/** objectName, or the resource type if there is no object (exchangeEvent etc) */
		public String getObjectOrType()
		{
			return StringUtil.hasValue(objectName) ? objectName : type;
		}
	}

	/** A parsed deadlock report */
	public static class Deadlock
	{
		public Timestamp        eventTime;    // server local time
		public Instant          eventTimeUtc;
		public boolean          parallel;
		public List<DlProcess>  processes = new ArrayList<>();
		public List<DlResource> resources = new ArrayList<>();
		/** The &lt;deadlock&gt; element, without the &lt;stackFrames&gt; */
		public String           xml;

		private static final Pattern DEADLOCK_ELEMENT = Pattern.compile("<deadlock[ >].*</deadlock>", Pattern.DOTALL);
		private static final Pattern STACK_FRAMES     = Pattern.compile("\\s*<stackFrames>.*?</stackFrames>", Pattern.DOTALL);

		/**
		 * Parse a XE <code>xml_deadlock_report</code> event (<code>&lt;event ...&gt;&lt;data name="xml_report"&gt;&lt;value&gt;&lt;deadlock&gt;...</code>)
		 * @param eventData     The event XML
		 * @param utcOffsetMin  Server time zone offset in minutes (the event timestamp is UTC, the other times in the report are server local time)
		 */
		public static Deadlock parseEvent(String eventData, int utcOffsetMin)
		throws Exception
		{
			Instant eventTimeUtc = null;
			Matcher tsm = Pattern.compile("<event [^>]*timestamp=\"([^\"]+)\"").matcher(eventData);
			if (tsm.find())
				eventTimeUtc = Instant.parse(tsm.group(1));

			Matcher m = DEADLOCK_ELEMENT.matcher(eventData);
			if ( ! m.find() )
				throw new Exception("No <deadlock> element found in the event.");

			Timestamp eventTime = eventTimeUtc == null ? null : Timestamp.valueOf(LocalDateTime.ofInstant(eventTimeUtc, ZoneOffset.UTC).plusMinutes(utcOffsetMin));

			Deadlock d = parseDeadlock(m.group(), eventTime);
			d.eventTimeUtc = eventTimeUtc;
			return d;
		}

		/**
		 * Parse a &lt;deadlock&gt; element (as saved in column DeadlockXml)
		 * @param deadlockXml  The XML
		 * @param eventTime    When it was detected (server local time)
		 */
		public static Deadlock parseDeadlock(String deadlockXml, Timestamp eventTime)
		throws Exception
		{
			Deadlock d = new Deadlock();
			d.eventTime = eventTime;
			d.xml       = STACK_FRAMES.matcher(deadlockXml).replaceAll("");

			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			dbf.setExpandEntityReferences(false);
			DocumentBuilder db = dbf.newDocumentBuilder();
			db.setErrorHandler(null); // throw on errors, without printing "[Fatal Error]" to stderr
			Document doc = db.parse(new InputSource(new StringReader(d.xml)));
			Element root = doc.getDocumentElement();

			// Victims: <victim-list><victimProcess id=""/></victim-list>, or (older) <deadlock victim="">
			Set<String> victimIds = new LinkedHashSet<>();
			for (Element vp : elements(root, "victimProcess"))
				victimIds.add(vp.getAttribute("id"));
			if (victimIds.isEmpty() && StringUtil.hasValue(root.getAttribute("victim")))
				victimIds.add(root.getAttribute("victim"));

			// Processes
			for (Element pe : elements(child(root, "process-list"), "process"))
			{
				DlProcess p = new DlProcess();
				p.id             = pe.getAttribute("id");
				p.spid           = toInt(pe.getAttribute("spid"));
				p.ecid           = toInt(pe.getAttribute("ecid"));
				p.victim         = victimIds.contains(p.id);
				p.login          = nullIfEmpty(pe.getAttribute("loginname"));
				p.host           = nullIfEmpty(pe.getAttribute("hostname"));
				p.app            = nullIfEmpty(pe.getAttribute("clientapp"));
				p.dbName         = nullIfEmpty(pe.getAttribute("currentdbname"));
				p.isolationLevel = nullIfEmpty(pe.getAttribute("isolationlevel"));
				p.waitResource   = nullIfEmpty(pe.getAttribute("waitresource"));
				p.lockMode       = nullIfEmpty(pe.getAttribute("lockMode"));
				p.waitTimeMs     = toInt(pe.getAttribute("waittime"));
				p.tranStarted    = toTimestamp(pe.getAttribute("lasttranstarted"));
				p.tranName       = nullIfEmpty(pe.getAttribute("transactionname"));

				Element stack = child(pe, "executionStack");
				List<Element> frames = elements(stack, "frame");
				if ( ! frames.isEmpty() )
				{
					Element top = frames.get(0);
					String procName = top.getAttribute("procname");
					if (StringUtil.hasValue(procName) && !"adhoc".equalsIgnoreCase(procName) && !"unknown".equalsIgnoreCase(procName))
					{
						p.procName = procName;
						p.procLine = toInt(top.getAttribute("line"));
					}
					String text = top.getTextContent() == null ? null : top.getTextContent().trim();
					if (StringUtil.hasValue(text) && !"unknown".equalsIgnoreCase(text))
						p.stmtText = text;
				}
				Element inputBuf = child(pe, "inputbuf");
				if (inputBuf != null)
					p.inputBuf = nullIfEmpty(inputBuf.getTextContent() == null ? null : inputBuf.getTextContent().trim());

				d.processes.add(p);
			}

			// Resources
			for (Element re : childElements(child(root, "resource-list")))
			{
				DlResource r = new DlResource();
				r.type       = re.getTagName();
				r.objectName = nullIfEmpty(re.getAttribute("objectname"));
				r.indexName  = nullIfEmpty(re.getAttribute("indexname"));
				r.mode       = nullIfEmpty(re.getAttribute("mode"));

				for (Element o : elements(child(re, "owner-list"), "owner"))
				{
					r.ownerIds  .add(o.getAttribute("id"));
					r.ownerModes.add(o.getAttribute("mode"));
				}
				for (Element w : elements(child(re, "waiter-list"), "waiter"))
				{
					r.waiterIds  .add(w.getAttribute("id"));
					r.waiterModes.add(w.getAttribute("mode"));
				}

				if ("exchangeEvent".equals(r.type) || "SyncPoint".equals(r.type))
					d.parallel = true;

				d.resources.add(r);
			}

			return d;
		}

		/** Unique id: event time (UTC) + victim process id */
		public String getDeadlockId()
		{
			DlProcess v = getVictim();
			return (eventTimeUtc != null ? eventTimeUtc.toString() : String.valueOf(eventTime)) + "/" + (v == null ? "-" : v.id);
		}

		/** The (first) victim, or null */
		public DlProcess getVictim()
		{
			for (DlProcess p : processes)
				if (p.victim)
					return p;
			return null;
		}

		public String getVictimDbName()
		{
			DlProcess v = getVictim();
			if (v != null && v.dbName != null)
				return v.dbName;
			return processes.isEmpty() ? null : processes.get(0).dbName;
		}

		/** Distinct spids, victim first */
		public List<Integer> getSpids()
		{
			Set<Integer> set = new LinkedHashSet<>();
			DlProcess v = getVictim();
			if (v != null)
				set.add(v.spid);
			for (DlProcess p : processes)
				set.add(p.spid);
			return new ArrayList<>(set);
		}

		/** The process for a process id */
		public DlProcess getProcess(String id)
		{
			for (DlProcess p : processes)
				if (p.id.equals(id))
					return p;
			return null;
		}

		public String getOtherSpids()
		{
			List<Integer> spids = getSpids();
			DlProcess v = getVictim();
			if (v != null)
				spids.remove(Integer.valueOf(v.spid));
			return join(spids, ", ");
		}

		public String getOtherProcs()
		{
			Set<String> set = new LinkedHashSet<>();
			for (DlProcess p : processes)
				if (!p.victim && p.procName != null)
					set.add(p.getProcNameAndLine());
			return set.isEmpty() ? null : join(set, ", ");
		}

		public String getOtherStatements()
		{
			// one per spid
			Map<Integer, String> map = new LinkedHashMap<>();
			DlProcess v = getVictim();
			for (DlProcess p : processes)
			{
				if (p.victim || (v != null && p.spid == v.spid))
					continue;
				if (!map.containsKey(p.spid) && StringUtil.hasValue(p.getStatement()))
					map.put(p.spid, p.getStatement());
			}
			if (map.isEmpty())
				return null;
			StringBuilder sb = new StringBuilder();
			for (Map.Entry<Integer, String> e : map.entrySet())
				sb.append(sb.length() == 0 ? "" : "\n\n").append("-- spid ").append(e.getKey()).append("\n").append(e.getValue());
			return sb.toString();
		}

		/** Sorted distinct objects, like "db.dbo.a <-> db.dbo.b" */
		public String getObjects()
		{
			Set<String> set = new TreeSet<>();
			for (DlResource r : resources)
				if (StringUtil.hasValue(r.objectName))
					set.add(r.objectName);
			if (set.isEmpty())
				return parallel ? "(parallel query)" : null;
			return join(set, " <-> ");
		}

		public String getLockTypes()
		{
			Set<String> set = new TreeSet<>();
			for (DlResource r : resources)
				set.add(r.type);
			return set.isEmpty() ? null : join(set, ", ");
		}

		public String getIsolationLevels()
		{
			Set<String> set = new TreeSet<>();
			for (DlProcess p : processes)
				if (p.isolationLevel != null)
					set.add(p.isolationLevel);
			return set.isEmpty() ? null : join(set, ", ");
		}

		public long getMaxWaitTimeMs()
		{
			long max = 0;
			for (DlProcess p : processes)
				max = Math.max(max, p.waitTimeMs);
			return max;
		}

		/** Deadlocks with the same objects and procedures/statements get the same key */
		public String getShapeKey()
		{
			Set<String> who = new TreeSet<>();
			for (DlProcess p : processes)
			{
				if (p.procName != null)
					who.add(p.procName);
				else if (p.getStatement() != null)
					who.add(normalizeSql(p.getStatement()));
			}
			String key = getObjects() + "|" + join(who, "|");
			try
			{
				byte[] md5 = MessageDigest.getInstance("MD5").digest(key.getBytes(StandardCharsets.UTF_8));
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < 8; i++)
					sb.append(String.format("%02x", md5[i]));
				return sb.toString();
			}
			catch (Exception ex)
			{
				return Integer.toHexString(key.hashCode());
			}
		}

		/** Collapse white space, replace numbers and strings, so the same statement with different values is the same */
		private static String normalizeSql(String sql)
		{
			String s = sql.replaceAll("'[^']*'", "'?'").replaceAll("\\b\\d+\\b", "?").replaceAll("\\s+", " ").trim();
			return s.length() > 300 ? s.substring(0, 300) : s;
		}

		private static String join(Iterable<?> list, String sep)
		{
			StringBuilder sb = new StringBuilder();
			for (Object o : list)
				sb.append(sb.length() == 0 ? "" : sep).append(o);
			return sb.toString();
		}
	}

	//------------------------------------------------------------
	// Small XML helpers
	//------------------------------------------------------------
	private static Element child(Element parent, String name)
	{
		if (parent == null)
			return null;
		for (Element e : childElements(parent))
			if (e.getTagName().equals(name))
				return e;
		return null;
	}

	private static List<Element> childElements(Element parent)
	{
		List<Element> list = new ArrayList<>();
		if (parent == null)
			return list;
		NodeList nl = parent.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++)
			if (nl.item(i).getNodeType() == Node.ELEMENT_NODE)
				list.add((Element) nl.item(i));
		return list;
	}

	/** All descendants with the name, in document order */
	private static List<Element> elements(Element parent, String name)
	{
		List<Element> list = new ArrayList<>();
		if (parent == null)
			return list;
		NodeList nl = parent.getElementsByTagName(name);
		for (int i = 0; i < nl.getLength(); i++)
			list.add((Element) nl.item(i));
		return list;
	}

	private static int toInt(String str)
	{
		if (StringUtil.isNullOrBlank(str))
			return 0;
		try { return Integer.parseInt(str.trim()); }
		catch (NumberFormatException ex) { return 0; }
	}

	/** "2026-11-15T19:36:07.753" (server local time) */
	private static Timestamp toTimestamp(String str)
	{
		if (StringUtil.isNullOrBlank(str))
			return null;
		try { return Timestamp.valueOf(str.trim().replace('T', ' ')); }
		catch (IllegalArgumentException ex) { return null; }
	}

	private static String nullIfEmpty(String str)
	{
		return StringUtil.hasValue(str) ? str : null;
	}
}
