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
*/

package com.dbxtune.cm;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.CounterController;
import com.dbxtune.DbxTune;
import com.dbxtune.ICounterController;
import com.dbxtune.ICounterController.DbmsOption;
import com.dbxtune.IGuiController;
import com.dbxtune.Version;
import com.dbxtune.alarm.AlarmHandler;
import com.dbxtune.alarm.IUserDefinedAlarmInterrogator;
import com.dbxtune.alarm.UserDefinedAlarmHandler;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.alarm.events.AlarmEventConfigResourceIsUsedUp;
import com.dbxtune.alarm.events.AlarmEventProcedureCacheOutOfMemory;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.graph.ChartDataHistoryCreator;
import com.dbxtune.graph.ChartDataHistoryManager;
import com.dbxtune.graph.ChartDataHistorySeries;
import com.dbxtune.graph.TrendGraphDataPoint;
import com.dbxtune.graph.TrendGraphDataPoint.Category;
import com.dbxtune.graph.TrendGraphDataPoint.LabelType;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.gui.TrendGraph;
import com.dbxtune.gui.swing.ColumnHeaderPropsEntry;
import com.dbxtune.gui.swing.GTabbedPane;
import com.dbxtune.gui.swing.GTable;
import com.dbxtune.gui.swing.GTable.ITableTooltip;
import com.dbxtune.hostmon.HostMonitorMetaData;
import com.dbxtune.pcs.DictCompression;
import com.dbxtune.pcs.PcsColumnOptions;
import com.dbxtune.pcs.PersistReader.PcsSavedException;
import com.dbxtune.pcs.PersistentCounterHandler;
import com.dbxtune.sql.ResultSetMetaDataCached;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.TdsConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.sql.showplan.transform.SqlServerShowPlanXmlTransformer;
import com.dbxtune.utils.AseConnectionUtils;
import com.dbxtune.utils.AseSqlScript;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.CronUtils;
import com.dbxtune.utils.NumberUtils;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.SwingUtils;
import com.dbxtune.utils.TimeUtils;
import com.dbxtune.utils.Ver;
import com.fasterxml.jackson.core.JsonGenerator;
import com.sybase.jdbcx.SybConnection;
import com.sybase.jdbcx.SybMessageHandler;

import it.sauronsoftware.cron4j.InvalidPatternException;
import it.sauronsoftware.cron4j.SchedulingPattern;

public class CountersModel 
extends AbstractTableModel
implements Cloneable, ITableTooltip
{
    private static final long serialVersionUID = -7486772146682031469L;

	/** Log4j logging. */
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public static final int    DATA_ABS           = 1;
	public static final int    DATA_DIFF          = 2;
	public static final int    DATA_RATE          = 3;

	/** Who is responsible for collecting data for this CM */
	private ICounterController  _counterController = null;

	/** Who holds the GUI part of the CM */
	private IGuiController      _guiController     = null;
	
	/** Filename of Icon that any GUI will use */
	private String             _guiIconFile        = null;
	/** If the above file can't be found use this one instead */
	private String             _guiIconFileDefault = "images/CmNoIcon.png";

	/** SybMessageHaandler used when querying the monitored server */
	private CmSybMessageHandler _sybMessageHandler = null;

	private boolean            _negativeDiffCountersToZero = true;

	private Timer              _refreshTimer      = new Timer(200, new RefreshTimerAction());
	private long               _refreshTimerStartTime = 0; // when we start the timer, just record the time we started it...
	private String             _name;
	private boolean            _systemCm;
	private String             _displayName       = null;  // Name that will be tabname etc
	private String             _description       = "";    // Can be used for tool tips etc
	private String             _groupName         = null;
	private String             _problemDesc       = "";    // Can be used for tool tips etc
	private Exception          _sampleException   = null;

	// Sample info, this members are set by the "main" sample thread 
	private String             _serverName        = "";
	private Timestamp          _sampleTimeHead    = null;
	private Timestamp          _counterClearTime  = null;
	private boolean            _showClearTime     = true;
	private boolean            _isCountersCleared = false;  // if counters has been cleared between previous sample and current sample. set in setCounterClearTime()
	private Timestamp          _sampleTime        = null;
	private long               _sampleInterval    = 0;

	// Individual timings of sample SQL, GUI LocalCalculation updates
	private long               _sqlRefreshStartTime = 0;
	private long               _sqlRefreshTime      = 0;
	private long               _guiRefreshStartTime = 0;
	private long               _guiRefreshTime      = 0;
	private long               _lcRefreshStartTime  = 0;
	private long               _lcRefreshTime       = 0;

	// private int typeModel;
	private boolean            _isInitialized     = false;
	private boolean            _runtimeInitialized= false;
	private long               _serverVersion     = 0;
	private boolean            _isClusterEnabled  = false;
	private List<String>       _activeServerRolesOrPermissions = null;
	private Map<String,Integer>_monitorConfigsMap = null;
	private String             _sqlInit           = null;
	private String             _sqlClose          = null; // Not used yet
	private String             _sqlRequest        = null;
	private String             _sqlWhere          = "";
	private TabularCntrPanel   _tabPanel          = null;
	private List<String>       _pkCols            = null;
//	private List<String>       _pkColsOrigin      = null;

	private int                _sqlQueryTimeout   = DEFAULT_sqlQueryTimeout;
	public  static final int   DEFAULT_sqlQueryTimeout = 10;

	private String[]           _monTablesInQuery     = null;
	
	private Set<String>        _cmsDependsOnMe       = null;
	private Set<String>        _dependsOnCm          = null;
	private String[]           _dependsOnRole        = null;
	private String[]           _dependsOnConfig      = null;
	private long               _dependsOnVersion     = 0;
	private long               _dependsOnCeVersion   = 0;
	private List<StoredProcCheck> _dependsOnStoredProc  = null; // containes: StoredProcCheck objects
	
	/** If we should refresh this CM in a different manner than the default refresh rate. 0=useDefault, >0=number of seconds between samples */
	private int                _postponeTime         = DEFAULT_postponeTime;
	public  static final int   DEFAULT_postponeTime  = 0;

	/** Is postpone enabled or disabled */
	private boolean            _postponeIsEnabled    = DEFAULT_postponeIsEnabled;
	public static final boolean DEFAULT_postponeIsEnabled = true;

	/** every time the CM is refreshed set this to System.currentTimeMillis() */
	private long               _lastLocalRefreshTime = 0;

	/** When was the previous data sample captured... System.currentTimeMillis() */
	private long               _prevLocalRefreshTime = 0;

//	private ResultSetMetaData  _rsmd;
	private ResultSetMetaDataCached  _rsmdCached;

	// Structure for storing list of columns to compute difference between two samples
	private String[]           _diffColumns = null;
	private boolean[]          _isDiffCol   = null;

	// Structure for storing list of columns to display rate as PCT rather than pure rate
	private String[]           _pctColumns  = null;
	private boolean[]          _isPctCol    = null;

	// In the filter (check for nonZeroValues) disregards these column(s)
	private String[]           _diffDissColumns = null;
	private boolean[]          _isDiffDissCol   = null;

	
	/** incremented every time a refresh() happens, done by incRefreshCounter() */
	private int _refreshCounter  = 0;
	/** incremented every time a refresh() happens, summary of how many rows collected */
	private int _sumRowCount     = 0;
//	private int _sumRowCountAbs  = 0;
//	private int _sumRowCountDiff = 0;
//	private int _sumRowCountRate = 0;

	/** if this CM has valid data for last sample */
	private boolean            _hasValidSampleData = false;

	/** If this CM is valid, the connected ASE Server might not support 
	 * this CM due to to early version or not configured properly. */
	private boolean            _isActive = true;

	/** A collection of Graphs connected to this CM */
	private Map<String,TrendGraph> _trendGraphs = new LinkedHashMap<String,TrendGraph>();

	/** A collection of Graphs actual data connected to the CM 
	 *  The graph data should still be calculated even in a non-gui environment
	 *  so it can be persisted, This makes it <b>much</b> easier to redraw a
	 *  graph when the off-line data is viewed in a later stage */
	private Map<String,TrendGraphDataPoint> _trendGraphsData = new LinkedHashMap<String,TrendGraphDataPoint>();

	private boolean            _filterAllZeroDiffCounters = false;
	private boolean            _sampleDataIsPaused        = false;
	private boolean            _sampleDataInBackground    = false;
	private boolean            _persistCounters           = false;
	private boolean            _persistCountersAbs        = true;
	private boolean            _persistCountersDiff       = true;
	private boolean            _persistCountersRate       = true;

	// "List" of tables to be dropped if we get Exceptions in refreshGetData()
	Set<String> _dropTempTableList = new HashSet<String>();

	// "List" of tables to be droped if we get Exceptions in refreshGetData()
	Set<String> _cleanupCmdsOnExceptionList = new HashSet<String>();

	// 
	private int clearCmLevel;

	private CounterSample _prevSample = null;      // Contains old raw data (previous sample)
	private CounterSample _newSample  = null;      // Contains new raw data
	private CounterSample _diffData   = null;       // diff between newSample and oldSample data (not filtered)
	private CounterSample _rateData   = null;       // diffData / sampleInterval
	
	private int _dataSource = getDefaultDataSource();//DATA_RATE;

	private boolean _dataInitialized=false;
	private boolean _firstTimeSample=true;
	private boolean _sqlInitDone=false;

	public enum State { NORMAL, SRV_IN_SHUTDOWN };
	private State _state = State.NORMAL;

	/** Class for handling User Defined Alarms */
	private IUserDefinedAlarmInterrogator _udAlarmInterrorgator = null;

	private GTable.ITableTooltip _cmToolTipSupplier = null;
	private Map<String, String> _cmLocalToolTipColumnDescriptions = new HashMap<String, String>();
	
	// Columns that should be first in the output table
//	private Set<String> _preferredColumnOrder = new LinkedHashSet<String>();
	private LinkedHashMap<String, ColumnHeaderPropsEntry> _preferredColumnProps = new LinkedHashMap<String, ColumnHeaderPropsEntry>();

	private int _aseError_2714_count = 0;
	private int _aseError_2714_actionThreshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_aseError_2714_actionThreshold, DEFAULT_aseError_2714_actionThreshold);

	
	public static final String 	PROPKEY_aseError_2714_actionThreshold = "CounterModel.ase.error.2714.action.threshold";
	public static final int     DEFAULT_aseError_2714_actionThreshold = 5;

	// A row can have different states (this should be a BITMAP: 1, 2, 4, 8, 16, 32 .... one bit for each state)
	public static final int ROW_STATE__IS_DIFF_OR_RATE_ROW = 1; 
	public static final int ROW_STATE__IS_AGGREGATED_ROW = 2; 

	// How many times have we got a timeoutException. This is set to 0 each time we succeed  
	private int  _sequentialTimeoutExceptionCount = 0;
	// At what time was the oldest timeoutException. This is set to -1 each time we succeed  
	private long _oldestTimeoutExceptionTime = -1;

	// If this is set Alarms is DISABLED until we have passed this time... Compared to: System.currentTimeMillis()  
	private long _alarmsIsDisabledUntilTs = -1;
	
	
	//-------------------------------------------------------
	// BEGIN: Graph members
	//-------------------------------------------------------
	/** 1 trace line by: sum/avg/min/max on a column */
	public static final int GRAPH_TYPE_BY_COL = 1;
	/** 1 trace line for each row in the dataset */
	public static final int GRAPH_TYPE_BY_ROW = 2;

	private static String[] _graphMethodsAbs = {
		"absVal",  "absMax",  "absMin",  "absAvg",  "absAvgGtZero",  "absSum"};
	private static String[] _graphMethodsAll = {
		"absVal",  "absMax",  "absMin",  "absAvg",  "absAvgGtZero",  "absSum",
		"diffVal", "diffMax", "diffMin", "diffAvg", "diffAvgGtZero", "diffSum",
		"rateVal", "rateMax", "rateMin", "rateAvg", "rateAvgGtZero", "rateSum"};

	private static String[] _graphMethodsByColAbs = {
		"absMax",  "absMin",  "absAvg",  "absAvgGtZero",  "absSum"};
	private static String[] _graphMethodsByColAll = {
		"absMax",  "absMin",  "absAvg",  "absAvgGtZero",  "absSum",
		"diffMax", "diffMin", "diffAvg", "diffAvgGtZero", "diffSum",
		"rateMax", "rateMin", "rateAvg", "rateAvgGtZero", "rateSum"};

	private static String[] _graphMethodsByRow = {"absVal", "diffVal", "rateVal"};

	/** type of graph that we can produce
	 *  - 1 trace line by: sum/avg/min/max on a column
	 *  - 1 trace line for each row in the dataset 
	 */
	private int _graphType = GRAPH_TYPE_BY_COL;

	/** What columns in the counter data set do we use */
	private String[]           _graphDataColNames          = {};
	/** What methods do we apply on _graphDataColNames */
	private String[]           _graphDataMethods           = {};

	//-------------------------------------------------------
	// END: Graph members
	//-------------------------------------------------------


	/**
	 * Reset this CM, this so we build new SQL statements if we connect to a different ASE version<br>
	 * Called from GetCounters.reset(), which most possible was called from disconnect() or similar
	 */
	public void reset()
	{
		// Problem stuff
		_problemDesc       = "";    // Can be used for tool tips etc
		_sampleException   = null;

		// Sample info, this members are set by the "main" sample thread 
		_serverName        = "";
		_sampleTimeHead    = null;
		_counterClearTime  = null;
		_sampleTime        = null;
		_sampleInterval    = 0;
		_isCountersCleared = false;

		// basic stuff
		setInitialized(false);
		setRuntimeInitialized(false);
		_serverVersion     = 0;
		_isClusterEnabled  = false;
		_activeServerRolesOrPermissions = null;
		_monitorConfigsMap = null;
		_sqlInit           = null;
		_sqlClose          = null; // Not used yet
		_sqlRequest        = null;
		_sqlWhere          = "";
		_pkCols            = null;

		// Structure for storing list of columns to compute difference between two samples
		//_diffColumns = null; // set by the constructor
		_isDiffCol   = null;   // this will be refreshed

		// Structure for storing list of columns to display rate as PCT rather than pure rate
		//_pctColumns  = null; // set by the constructor
		_isPctCol    = null;   // this will be refreshed

		// In the filter (check for nonZeroValues) disregards these column(s)
		//_diffDissColumns = null; // set by the constructor
		_isDiffDissCol   = null;   // this will be refreshed

		_rsmdCached = null;
		
		// incremented every time a refresh() happens, done by incRefreshCounter()
		//_refreshCounter  = 0;  // probably NOT reset, then send counter info will show 0, reset is done by resetStatCounters()
		// incremented every time a refresh() happens, summary of how many rows collected
		//_sumRowCount     = 0;  // probably NOT reset, then send counter info will show 0, reset is done by resetStatCounters()

		// if this CM has valid data for last sample
		_hasValidSampleData = false;

		// If this CM is valid, the connected ASE Server might not support 
		// this CM due to to early version or not configured properly.
		_isActive = true;


		// data 
		_prevSample = null;      // Contains old raw data (previous sample)
		_newSample  = null;      // Contains new raw data
		_diffData   = null;       // diff between newSample and oldSample data (not filtered)
		_rateData   = null;       // diffData / sampleInterval
		
		// _dataSource = DATA_RATE;  // lets keep current view

		// more basic stuff
		_dataInitialized = false;
		_firstTimeSample = true;
		_sqlInitDone     = false;

		// new delta/rate row flag
		_isNewDeltaOrRateRow = null;
		
		// Aggregate
		private_resetAggregates();

		// reset panel, if we got one
		if (_tabPanel != null)
			_tabPanel.reset();
		
		// reset trendgraph DATA
		for (TrendGraphDataPoint tgdp : getTrendGraphData().values())
			tgdp.clear();
	}


	/** Override this to get the initial state */
	public int getDefaultDataSource()
	{
		return DATA_RATE;
	}


	/*---------------------------------------------------
	** BEGIN: constructors
	**---------------------------------------------------
	*/
	public CountersModel()
	{
	}

	/**
	 * @param name                        Name of the Counter Model
	 * @param groupName                   Name of the Group this counter belongs to, can be null
	 * @param sql                         SQL Used to grab a sample from the counter data
	 * @param pkList                      A list of columns that will be used during diff calculations to lookup values in previous samples
	 * @param diffColumns                 Columns to do diff calculations on
	 * @param pctColumns                  Columns that is to be considered as Percent calculated columns, (they still need to be apart of diffColumns)
	 * @param monTables                   What monitor tables are accessed in this query, used for TOOLTIP lookups
	 * @param dependsOnRole               Needs following role(s)
	 * @param dependsOnConfig             Check that these configurations are above 0
	 * @param dependsOnVersion            What version of ASE do we need to sample for this CounterModel
	 * @param dependsOnCeVersion          What version of ASE-CE do we need to sample for this CounterModel
	 * @param negativeDiffCountersToZero  if diff calculations is negative, reset the counter to zero.
	 * @param systemCm                    Is this a system CM
	 */
	public CountersModel
	(
			ICounterController counterController,
			IGuiController guiController,
			String       name,             // Name of the Counter Model
			String       groupName,        // Name of the Group this counter belongs to, can be null
			String       sql,              // SQL Used to grab a sample from the counter data
			List<String> pkList,           // A list of columns that will be used during diff calculations to lookup values in previous samples
			String[]     diffColumns,      // Columns to do diff calculations on
			String[]     pctColumns,       // Columns that is to be considered as Percent calculated columns, (they still need to be apart of diffColumns)
			String[]     monTables,        // What monitor tables are accessed in this query, used for TOOLTIP lookups
			String[]     dependsOnRole,    // Needs following role(s)
			String[]     dependsOnConfig,  // Check that these configurations are above 0
			long         dependsOnVersion, // What version of ASE do we need to sample for this CounterModel
			long         dependsOnCeVersion, // What version of ASE-CE do we need to sample for this CounterModel
			boolean      negativeDiffCountersToZero, // if diff calculations is negative, reset the counter to zero.
			boolean      systemCm
	)
	{
		this(counterController, guiController, name, groupName, sql, pkList, diffColumns, pctColumns, monTables, dependsOnRole, dependsOnConfig, dependsOnVersion, dependsOnCeVersion, negativeDiffCountersToZero, systemCm, 0);
	}


	/**
	 * @param name                        Name of the Counter Model
	 * @param groupName                   Name of the Group this counter belongs to, can be null
	 * @param sql                         SQL Used to grab a sample from the counter data
	 * @param pkList                      A list of columns that will be used during diff calculations to lookup values in previous samples
	 * @param diffColumns                 Columns to do diff calculations on
	 * @param pctColumns                  Columns that is to be considered as Percent calculated columns, (they still need to be apart of diffColumns)
	 * @param monTables                   What monitor tables are accessed in this query, used for TOOLTIP lookups
	 * @param dependsOnRole               Needs following role(s)
	 * @param dependsOnConfig             Check that these configurations are above 0
	 * @param dependsOnVersion            What version of ASE do we need to sample for this CounterModel
	 * @param dependsOnCeVersion          What version of ASE-CE do we need to sample for this CounterModel
	 * @param negativeDiffCountersToZero  if diff calculations is negative, reset the counter to zero.
	 * @param systemCm                    Is this a system CM
	 * @param defaultPostponeTime         default postpone time
	 */
	public CountersModel
	(
			ICounterController counterController,
			IGuiController guiController,
			String       name,
			String       groupName,
			String       sql,
			List<String> pkList,
			String[]     diffColumns,
			String[]     pctColumns,
			String[]     monTables,
			String[]     dependsOnRole,
			String[]     dependsOnConfig,
			long         dependsOnVersion,
			long         dependsOnCeVersion,
			boolean      negativeDiffCountersToZero,
			boolean      systemCm,
			int          defaultPostponeTime
	)
	{
		// NOT the best place to put it, but it was to complicated (to much work) to put it somewhere else
		// lets rethinks this at a later stage.
//		SplashWindow.drawProgress("Loading: Counter Model '"+name+"'");

		// Initialize a model for use with a JTable
		_name      = name;
		_groupName = groupName;
		_systemCm  = systemCm;

		// Check if name is OK
		checkInConstructor(); 

		// Register it at the CounterController
		setCounterController(counterController);
		
		// set the GUI Controller (if any)
//		setGuiController(guiController); // THIS MUST BE DONE LATER...

		_sqlRequest         = sql;
		_sqlWhere           = "";
		_pkCols             = pkList;
//		_pkColsOrigin       = pkList == null ? null : new ArrayList<String>(pkList); // Copy the list
		_diffColumns        = diffColumns;
		_pctColumns         = pctColumns;
		_monTablesInQuery   = monTables;
		_dependsOnRole      = dependsOnRole;
		_dependsOnConfig    = dependsOnConfig;
		_dependsOnVersion   = dependsOnVersion;
		_dependsOnCeVersion = dependsOnCeVersion;
		_negativeDiffCountersToZero = negativeDiffCountersToZero;
		_postponeTime       = defaultPostponeTime;

		setUserDefinedAlarmInterrogator(createUserDefinedAlarmHandler());

		_sybMessageHandler  = createSybMessageHandler();
		
		_cmToolTipSupplier  = createToolTipSupplier();
		
		_cmLocalToolTipColumnDescriptions = createLocalToolTipTextOnTableColumnHeader();
		initLocalToolTipTextOnTableColumnHeader();
		
		//		filterColId       = -1;
		_prevSample       = null; // Contains old raw data
		_newSample        = null; // Contains new raw data
		_diffData         = null; // diff between newSample and oldSample data (not filtered)

		String[] emptyArray = {};
		if (_diffColumns      == null) _diffColumns      = emptyArray;
		if (_diffDissColumns  == null) _diffDissColumns  = emptyArray;
		if (_pctColumns       == null) _pctColumns       = emptyArray;
		if (_monTablesInQuery == null) _monTablesInQuery = emptyArray;
		if (_dependsOnRole    == null) _dependsOnRole    = emptyArray;
		if (_dependsOnConfig  == null) _dependsOnConfig  = emptyArray;

		// Set some default values
		_postponeIsEnabled = getDefaultPostponeIsEnabled();
		_postponeTime      = getDefaultPostponeTime();
		_sqlQueryTimeout   = getDefaultQueryTimeout();

		// register some default values before loading values.
		registerDefaultValues();

		// Load saved properties
		loadProps();

		setDataInitialized(false);

		// Initialize alarms
		initAlarms();

		// Print the alarm configuration.
		printSystemAlarmConfig();
	}

	protected void checkInConstructor()
	{
		if ( ! isValidCmName(getName(), isSystemCm()) )
		{
			String err = checkForValidCmName(getName());
			throw new RuntimeException("Problems when creating cm '"+getName()+"'. "+err);
		}
	}

	/*---------------------------------------------------
	** END: constructors
	**---------------------------------------------------
	*/

	public CountersModel copyForOfflineRead()
	{
//System.out.println("CountersModel.copyForOfflineRead(): this="+this);
		CountersModel cm = copyForStorage();
		
		// Remove all the rows in the CM, so that new can be added
		// if this is not done, all the old rows will still be visible when displaying it in the JTable
		cm.clearForRead();

//System.out.println("CountersModel.copyForOfflineRead(): return cm="+cm);
		return cm;
	}
	public CountersModel copyForStorage()
	{
		CountersModel c = null;
		try
		{
			c = (CountersModel) this.clone();
		}
		catch (CloneNotSupportedException e)
		{
			_logger.warn("In '"+this.getName()+"', caught: "+e, e);
			c = new CountersModel();
		}

		c._sampleException            = this._sampleException;
		c._sybMessageHandler          = this._sybMessageHandler;

		c._refreshTimer               = null;
		c._negativeDiffCountersToZero = this._negativeDiffCountersToZero;

		c._name                       = this._name;
		c._groupName                  = this._groupName;
		c._systemCm                   = this._systemCm;
		c._displayName                = this._displayName;
		c._description                = this._description;
		c._problemDesc                = this._problemDesc;

		c._serverName                 = this._serverName;
		c._sampleTimeHead             = this._sampleTimeHead;
		c._counterClearTime           = this._counterClearTime;
		c._isCountersCleared          = this._isCountersCleared;
		c._sampleTime                 = this._sampleTime;
		c._sampleInterval             = this._sampleInterval;       // Can we use this: or do we need to call method: cm.getLastSampleInterval(), which depends on '_newSample': return (_newSample == null) ? 0 : _newSample.getSampleInterval();

//System.out.println("CM: copyForStorage(): cmName="+StringUtil.left(this._name, 30)+", fromClass="+StringUtil.left(this.getClass().getSimpleName(), 30)+", newClassName="+StringUtil.left(c.getClass().getSimpleName(), 30)+": _sampleTime="+this._sampleTime+", _sampleIntervall="+this._sampleInterval+", getLastSampleInterval()="+this.getLastSampleInterval()+", getSampleInterval()="+this.getSampleInterval()+", getTimestamp()="+this.getTimestamp());

		c._sqlRefreshStartTime        = this._sqlRefreshStartTime;
		c._sqlRefreshTime             = this._sqlRefreshTime;
		c._guiRefreshStartTime        = this._guiRefreshStartTime;
		c._guiRefreshTime             = this._guiRefreshTime;
		c._lcRefreshStartTime         = this._lcRefreshStartTime;
		c._lcRefreshTime              = this._lcRefreshTime;
		
		c._isInitialized              = this._isInitialized;
		c._runtimeInitialized         = this._runtimeInitialized;
		c._serverVersion              = this._serverVersion;
		c._isClusterEnabled           = this._isClusterEnabled;
		c._activeServerRolesOrPermissions = this._activeServerRolesOrPermissions;      // no need to full copy, static usage
		c._monitorConfigsMap          = this._monitorConfigsMap;   // no need to full copy, static usage
		c._sqlInit                    = this._sqlInit;
		c._sqlClose                   = this._sqlClose;
		c._sqlRequest                 = this._sqlRequest;
		c._sqlWhere                   = this._sqlWhere;
		c._tabPanel                   = this._tabPanel;           // do we really need to copy this one?
		c._pkCols                     = this._pkCols;              // no need to full copy, static usage
//		c._pkColsOrigin               = this._pkColsOrigin;

		c._monTablesInQuery           = this._monTablesInQuery;    // no need to full copy, static usage

		c._cmsDependsOnMe             = this._cmsDependsOnMe;      // no need to full copy, static usage
		c._dependsOnCm                = this._dependsOnCm;         // no need to full copy, static usage
		c._dependsOnRole              = this._dependsOnRole;       // no need to full copy, static usage
		c._dependsOnConfig            = this._dependsOnConfig;     // no need to full copy, static usage
		c._dependsOnVersion           = this._dependsOnVersion;
		c._dependsOnCeVersion         = this._dependsOnCeVersion;
		c._dependsOnStoredProc        = this._dependsOnStoredProc; // no need to full copy, static usage

		c._postponeTime               = this._postponeTime;
		c._postponeIsEnabled          = this._postponeIsEnabled;
		c._lastLocalRefreshTime       = this._lastLocalRefreshTime;
		c._prevLocalRefreshTime       = this._prevLocalRefreshTime;

		// Do we need to clone this, I think it's safe not to...
		// in SampleCnt we do: cm.setResultSetMetaData(rs.getMetaData());  so a new one will be created
		c._rsmdCached                 = this._rsmdCached;

		c._diffColumns                = this._diffColumns;     // no need to full copy, static usage, i think
		c._isDiffCol                  = this._isDiffCol;       // no need to full copy, static usage, i think

		c._pctColumns                 = this._pctColumns;      // no need to full copy, static usage, i think
		c._isPctCol                   = this._isPctCol;        // no need to full copy, static usage, i think

		c._diffDissColumns            = this._diffDissColumns; // no need to full copy, static usage, i think
		c._isDiffDissCol              = this._isDiffDissCol;   // no need to full copy, static usage, i think
		
		c._hasValidSampleData         = this._hasValidSampleData;
		
		c._isActive                   = this._isActive;

		c._trendGraphs                = new LinkedHashMap<String,TrendGraph>(_trendGraphs);
		// needs to be copied/cloned, since the TrendGraphDataPoint is reused.
		c._trendGraphsData = new LinkedHashMap<String,TrendGraphDataPoint>(_trendGraphsData);

		c._filterAllZeroDiffCounters  = this._filterAllZeroDiffCounters;
		c._sampleDataIsPaused         = this._sampleDataIsPaused;
		c._sampleDataInBackground     = this._sampleDataInBackground;
		c._persistCounters            = this._persistCounters;
		c._persistCountersAbs         = this._persistCountersAbs;
		c._persistCountersDiff        = this._persistCountersDiff;
		c._persistCountersRate        = this._persistCountersRate;


		// Shouldn't these ones has to be copied/cloned, wont they be overwritten if the PersistHandler takes to long time...
//		c._prevSample                 = this._prevSample;
//		c._newSample                  = this._newSample;
//		c._diffData                   = this._diffData;
//		c._rateData                   = this._rateData;
		c._prevSample                 = this._prevSample == null ? null : new CounterSample(this._prevSample, true);
		c._newSample                  = this._newSample  == null ? null : new CounterSample(this._newSample,  true);
		c._diffData                   = this._diffData   == null ? null : new CounterSample(this._diffData,   true);
		c._rateData                   = this._rateData   == null ? null : new CounterSample(this._rateData,   true);
		
		c._dataSource                 = this._dataSource;

		c._dataInitialized            = this._dataInitialized;
		c._firstTimeSample            = this._firstTimeSample;

		c._refreshCounter             = this._refreshCounter;
		c._sumRowCount                = this._sumRowCount;
//		c._sumRowCountAbs             = this._sumRowCountAbs;
//		c._sumRowCountDiff            = this._sumRowCountDiff;
//		c._sumRowCountRate            = this._sumRowCountRate;
		
		c._isNonConfiguredMonitoringAllowed       = this._isNonConfiguredMonitoringAllowed;
		c._hasNonConfiguredMonitoringHappened     = this._hasNonConfiguredMonitoringHappened;
		c._lastNonConfiguredMonitoringMessageList = this._lastNonConfiguredMonitoringMessageList; // Should we clone this or not...

		c._isNewDeltaOrRateRow = this._isNewDeltaOrRateRow;

		private_copyAggregates(c);

		return c;
	}
	
	/*---------------------------------------------------
	** BEGIN: implementing TableModel or overriding AbstractTableModel
	**---------------------------------------------------
	*/
	public void printTableModelListener()
	{
		TableModelListener[] tml = getTableModelListeners();
		for (int i = 0; i < tml.length; i++)
		{
			if (tml[i] instanceof TabularCntrPanel)
				System.out.println("TableModelListener["+i+"] = "+((TabularCntrPanel)tml[i]).getPanelName());
			else
				System.out.println("TableModelListener["+i+"] = "+tml[i]);
		}
	}
	@Override
	public void addTableModelListener(TableModelListener l)
	{
//		System.out.println("+++addTableModelListener(l="+l+")");
		super.addTableModelListener(l);
//		printTableModelListener();
	}
	@Override
	public void removeTableModelListener(TableModelListener l)
	{
//		System.out.println("---removeTableModelListener(l="+l+")");
		super.removeTableModelListener(l);
//		printTableModelListener();
	}

//	public Class getColumnClass(int columnIndex)
//	{
//		if (!isDataInitialized())   return null;
//		if (chosenData == null) return null;
//		return chosenData.get(row, col);
//	}
	@Override
	public Class<?> getColumnClass(int columnIndex)
	{
		if (!isDataInitialized())   return null;

		CounterTableModel data = getCounterData();
		if (data == null) return Object.class;
//System.out.println("Cm="+getName()+", colIndex="+columnIndex+", class="+data.getColumnClass(columnIndex)+", colname="+getColumnName(columnIndex));
		return data.getColumnClass(columnIndex);
	}

	@Override
	public int getColumnCount()
	{
		CounterTableModel data = getCounterData();
		int c = 0;
		if (isDataInitialized() && data != null)
			c = data.getColumnCount();
//		System.out.println(_name+":getColumnCount() <- "+c);
		return c;
    }

	@Override
	public String getColumnName(int col)
	{
		CounterTableModel data = getCounterData();
		String s = null;
		if (isDataInitialized() && data != null)
			s = data.getColumnName(col);
//		System.out.println(_name+":getColumnName(col="+col+") <- '"+s+"'.");
		return s;
	}

	@Override
	public int getRowCount()
	{
		int c = 0;
		CounterTableModel data = getCounterData();
		if (isDataInitialized() && data != null)
			c = data.getRowCount();
//		System.out.println(_name+":getRowCount() <- "+c);
		return c;
    }

	@Override
	public Object getValueAt(int row, int col)
	{
		if (!isDataInitialized())   return null;

		CounterTableModel data = getCounterData();
		if (data == null) return null;
		return data.getValueAt(row, col);
    }

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex)
	{
		return false;
    }

//	public void setValueAt(Object value, int rowIndex, int columnIndex)
//	{
//	}
	
	@Override
	public int findColumn(String colName)
	{
		CounterTableModel data = getCounterData();
		int pos = -1;
		if (isDataInitialized() && data != null)
			pos = data.findColumn(colName);
//			pos = data.getColId(colName);
//		System.out.println(_name+":getColumnPos(colName="+colName+") <- "+pos);
		return pos;
	}

	/*---------------------------------------------------
	** END: implementing TableModel or overriding AbstractTableModel
	**---------------------------------------------------
	*/

	public List<String> getColumnNames()
	{
		CounterTableModel data = getCounterData();

		if (data == null)
			return Collections.emptyList();

		return data.getColNames();
	}
	
	/**
	 * Simply a convenience method that calls <code>findColumn(colName)</code>
	 * @param colName Column name to check for existance
	 * @return true if the column exists, otherwise false
	 */
	public boolean hasColumn(String colName)
	{
		return findColumn(colName) >= 0;
	}

	/**
	 * Simply a convenience method that calls <code>hasColumns(colName)</code> for all passed columns
	 * 
	 * @param colNames 
	 * @return true if all columns exists, otherwise false
	 */
	public boolean hasColumns(String... colNames)
	{
		for (String colName : colNames)
		{
			if ( ! hasColumn(colName) )
				return false;

		}
		return true;
	}

	/**
	 * Get a list of column names that are missing in this CM
	 * 
	 * @param colNames 
	 * @return A List with the missing columns. <br>
	 *         If none is missing the list will be empty. <br>
	 *         A List will always be returned
	 */
	public List<String> getMissingColumns(String... colNames)
	{
		List<String> list = new ArrayList<>();

		for (String colName : colNames)
		{
			if ( ! hasColumn(colName) )
				list.add(colName);
		}
		return list;
	}


	//----------------------------------------------------------
	// BEGIN: Helper methods to check for CounterSample column names
	//----------------------------------------------------------
	private int _findCsColumn_missing_count = 0;
	public void findCsColumn_resetMissingCounter()
	{
		_findCsColumn_missing_count = 0;
	}
	public boolean findCsColumn_hasMissingColumns()
	{
		return _findCsColumn_missing_count > 0;
	}
	public int findCsColumn_getMissingColumns()
	{
		return _findCsColumn_missing_count;
	}
	public int findCsColumn(CounterSample cs, String... colNames)
	{
		return findCsColumn(cs, true, colNames);
	}
	public int findCsColumn(CounterSample cs, boolean printError, String... colNames)
	{
		int pos = -1;
		for (String name : colNames)
		{
			pos = cs.findColumn(name);

			if (pos != -1)
				return pos;
		}
		
		_findCsColumn_missing_count++;
		if (printError)
			_logger.warn("Cant find any of the column(s) " + StringUtil.toCommaStrQuoted("'", colNames) + " in CounterSample '" + cs.getName() + "'.");

		return -1;
	}
	//----------------------------------------------------------
	// END: Helper methods to check for CounterSample column names
	//----------------------------------------------------------



	/**
	 * Set who is responsible for collecting the counters for this CounterModel
	 * @param counterController
	 */
	public void setCounterController(ICounterController counterController)
	{
		_counterController = counterController;
		// If we have a Counter Controller, add the CM to it
		if (_counterController != null)
			_counterController.addCm(this);
	}
	/**
	 * Set who is responsible for collecting the counters for this CounterModel<br>
	 * But DO NOT register/add this CM to the internal list of CM's
	 * @param counterController
	 */
	public void setCounterController_withoutAddingCurrentCmToCmList(ICounterController counterController)
	{
		_counterController = counterController;
	}
	/**
	 * Get who is responsible for collecting the counters for this CounterModel
	 * @return counterController
	 */
	public ICounterController getCounterController()
	{
		if (_counterController != null)
			return _counterController;

		_logger.error("LOGGING RUNTIME EXCEPTION... instead of throwing...", new RuntimeException("In CM '" + getName() + "' when calling getCounterController(), the '_counterController' was NOT INITIALIZED"));

		// Remove this, this is just for backward compatibility.
		return CounterController.getInstance();
	}
	/**
	 * Does this CounterModel have a counter controller
	 * @return counterController
	 */
	public boolean hasCounterController()
	{
		if (_counterController != null)
			return true;

		return false;
//		_logger.error("LOGGING RUNTIME EXCEPTION... instead of throwing...", new RuntimeException("In CM '" + getName() + "' when calling hasCounterController(), the '_counterController' was NOT INITIALIZED"));
//
//		// Remove this, this is just for backward compatibility.
//		return CounterController.hasInstance();
	}

	/**
	 * <p>
	 * Set the "main" GUI controller
	 * </p>
	 * <p>
	 * If the tool was started in GUI mode, this will also create the GUI 
	 * using method <code>createGui()</code> and add it to the GUI Controller
	 * </p>
	 * @param guiController
	 */
	public void setGuiController(IGuiController guiController)
	{
		_guiController = guiController;
		// If we have a GUI Controller, create a new GUI and add it to the Controller.
		if (_guiController != null)
		{
			if (_guiController.hasGUI())
			{
//				_guiController.addPanel(createGui());

				JPanel p = createGui();

				if (p instanceof TabularCntrPanel)
				{
					TabularCntrPanel tcp = (TabularCntrPanel)p;
					
					if (getIconFile() != null)
					{
						ImageIcon icon = SwingUtils.readImageIcon(Version.class, getIconFile());

						// Get default icon if we can't find the first icon
						if (icon == null)
							icon = SwingUtils.readImageIcon(Version.class, getIconFileDefault());
							
						tcp.setIcon(icon);
					}
				}
				_guiController.addPanel(p);
			}
		}
	}
	
	/**
	 * Get the controlling GUI
	 * @return
	 */
	public IGuiController getGuiController()
	{
		return _guiController;
	}
	
	/**
	 * Check if we have GUI controller
	 * @return
	 */
	public boolean hasGuiController()
	{
		return getGuiController() != null;
	}
	
	/** set Icon Filename which the GUI can use */
	public void setIconFile(String filename)
	{
		_guiIconFile = filename;
	}
	/** get Icon Filename which the GUI can use */
	public String getIconFile()
	{
		return _guiIconFile;
	}

	/** set Icon Filename which the GUI can use */
	public void setIconFileDefault(String filename)
	{
		_guiIconFileDefault = filename;
	}
	/** get Icon Filename which the GUI can use */
	public String getIconFileDefault()
	{
		return _guiIconFileDefault;
	}

	/**
	 * Create a GUI panel, this one should be overridden by subclasses if you want to change behavior.
	 */
//	protected TabularCntrPanel createGui()
	protected JPanel createGui()
	{
		TabularCntrPanel tcp = new TabularCntrPanel(this);

//		if (getIconFile() != null)
//		{
//			ImageIcon icon = SwingUtils.readImageIcon(Version.class, getIconFile());
//
//			// Get default icon if we can't find the first icon
//			if (icon == null)
//				icon = SwingUtils.readImageIcon(Version.class, getIconFileDefault());
//				
//			tcp.setIcon(icon);
//		}

		return tcp;
	}
	
	/** 
	 * Used to set off-line counter data<br>
	 * Most likely has to be override to handle local offline data for subclasses
	 * @param colSqlDataType 
	 */
	public void setOfflineColumnNames(int type, List<String> cols, List<Integer> sqlTypes)
	{
//new Exception(this+": DUMMY: CounterModel setOfflineColumnNames").printStackTrace();
//System.out.println("++++++++++++++++++++++++++++++++"+this+"::setOfflineColumnNames(): type="+type+", cols='"+cols+"'.");
//		CounterTableModel data = null;
		CounterSample data = null;
		if      (type == DATA_ABS)  { if (_newSample == null) {_newSample = new CounterSample("offline-abs",  false, null, null); data = _newSample;} else data = _newSample;}
		else if (type == DATA_DIFF) { if (_diffData  == null) {_diffData  = new CounterSample("offline-diff", false, null, null); data = _diffData;}  else data = _diffData;}
		else if (type == DATA_RATE) { if (_rateData  == null) {_rateData  = new CounterSample("offline-rate", false, null, null); data = _rateData;}  else data = _rateData;}
		else
			throw new RuntimeException("Only ABS, DIFF, or RATE data is available.");

		data.setColumnNames(cols);
		data.setSqlType(sqlTypes);
		initColumnStuff(data);
	}

	/** 
	 * Used to set off-line counter data<br>
	 * Most likely has to be override to handle local offline data for subclasses
	 */
	public void setOfflineValueAt(int type, Object value, int row, int col)
	{
//System.out.println("++++++++++++++++++++++++++++++++"+this+"::setOfflineValueAt(type="+type+", row="+row+", col="+col+", value='"+value+"')");
		CounterTableModel data = null;
		if      (type == DATA_ABS)  { if (_newSample == null) {_newSample = new CounterSample("offline-abs",  false, null, null); data = _newSample;} else data = _newSample;}
		else if (type == DATA_DIFF) { if (_diffData  == null) {_diffData  = new CounterSample("offline-diff", false, null, null); data = _diffData;}  else data = _diffData;}
		else if (type == DATA_RATE) { if (_rateData  == null) {_rateData  = new CounterSample("offline-rate", false, null, null); data = _rateData;}  else data = _rateData;}
		else
			throw new RuntimeException("Only ABS, DIFF, or RATE data is available.");

		// NOTE: this might suffer from performance problems if: fireTableCellUpdated() 
		//       on the JTable is fired...
		data.setValueAt(value, row, col);
	}

	/**
	 * Checks if the name of the CM is acceptable
	 */
	public static boolean isValidCmName(String name)
	{
		return checkForValidCmName(name, false) == null;
	}
	private static boolean isValidCmName(String name, boolean systemCm)
	{
		return checkForValidCmName(name, systemCm) == null;
	}

	/**
	 * Checks if the name of the CM is acceptable
	 * @param name
	 * @return null if OK otherwise a error message
	 */
	public static String checkForValidCmName(String name)
	{
		return checkForValidCmName(name, false);
	}
	private static String checkForValidCmName(String name, boolean systemCm)
	{
		if (name == null)
			return "Name can't be a null pointer";
		
//		name = name.trim();
		String lowerName = name.toLowerCase();

		if (name.length() == 0)
			return "Name can't zero length";
		
		if (name.length() > 20) 
			return "Name can't be more than 20 characters";

		if ( ! systemCm )
		{
			if ( lowerName.startsWith("cm") ) 
				return "Name can't start with 'CM', only system counters can do that.";
		}
		
		if ( ! name.matches("[a-zA-Z0-9]*") ) 
			return "Name can only contain normal characters (a-Z, A-Z or 0-9)";
		
		return null;
	}
	
	public boolean   showClearTime()       { return _showClearTime; }
	public String    getServerName()       { return _serverName; }
	public Timestamp getSampleTimeHead()   { return _sampleTimeHead; }
	public Timestamp getCounterClearTime() { return _counterClearTime; }
	public Timestamp getSampleTime()       { return _sampleTime; }
	public long      getSampleInterval()   { return _sampleInterval; }
	public boolean   isCountersCleared()   { return _isCountersCleared; }

//	public Timestamp getPreviousSampleTimeHead() { return _prevSample == null ? null : _prevSample.getSampleTimeHead(); }
	public Timestamp getPreviousSampleTime()     { return _prevSample == null ? null : _prevSample.getSampleTime(); }

	
	public void setShowClearTime(boolean b)              { _showClearTime = b; }
	public void setServerName(String name)               { _serverName = name; }
	public void setSampleTimeHead(Timestamp timeHead)    { _sampleTimeHead = timeHead; }
	public void setSampleTime(Timestamp time)            { _sampleTime = time; }
	public void setSampleInterval(long interval)         { _sampleInterval = interval; }
	public void setTimeInfo(Timestamp timeHead, Timestamp clearTime, Timestamp sampleTime, long intervall)
	{
		setSampleTimeHead(timeHead);
		setCounterClearTime(clearTime);
		setSampleTime(sampleTime);
		setSampleInterval(intervall);
	}
	public void setCounterClearTime(Timestamp clearTime) 
	{
		setIsCountersCleared(false);
		
		// if NOT first sample
		if (_prevSample != null)
		{
			Timestamp prevSampleTime = getPreviousSampleTime(); // hopefully this returns null in "offline mode"
			if (prevSampleTime != null && clearTime != null)
			{
				if (clearTime.getTime() > prevSampleTime.getTime())
					setIsCountersCleared(true);
			}
			else
			{
    			// counters hasn't been cleared previously and we have a valid clearTime 
    			if (_counterClearTime == null && clearTime != null)
    				setIsCountersCleared(true);
    
    			// New clear time is later than previous clearTime
    			if (_counterClearTime != null && clearTime != null)
    			{
    				if (clearTime.getTime() > _counterClearTime.getTime())
    					setIsCountersCleared(true);
    			}
			}
		}

		_counterClearTime = clearTime; 
	}

	public void setIsCountersCleared(boolean isCountersCleared)
	{
		_isCountersCleared = isCountersCleared;
	}


	/** How many milliseconds did we spend on executing the SQL statement that fetched the performance counters */
	public long getSqlRefreshTime()       { return _sqlRefreshTime; }
	/** Used by the PersistReader to set refresh time */
	public void setSqlRefreshTime(long t) { _sqlRefreshTime = t; }
	public void beginSqlRefresh() { _sqlRefreshStartTime = System.currentTimeMillis(); }
	public void endSqlRefresh()   {	_sqlRefreshTime = System.currentTimeMillis() - _sqlRefreshStartTime; }

	/** How many milliseconds did we spend on executing the GUI code that updated the GUI */
	public long getGuiRefreshTime()       { return _guiRefreshTime; }
	/** Used by the PersistReader to set refresh time */
	public void setGuiRefreshTime(long t) { _guiRefreshTime = t; }
	public void beginGuiRefresh() { _guiRefreshStartTime = System.currentTimeMillis(); }
	public void endGuiRefresh()   {	_guiRefreshTime = System.currentTimeMillis() - _guiRefreshStartTime; }

	/** How many milliseconds did we spend on executing the LocalCalculation code */
	public long getLcRefreshTime()       { return _lcRefreshTime; }
	/** Used by the PersistReader to set refresh time */
	public void setLcRefreshTime(long t) { _lcRefreshTime = t; }
	/** Begin time of Local Calculation */
	public void beginLcRefresh() { _lcRefreshStartTime = System.currentTimeMillis(); }
	/** End time of Local Calculation */
	public long endLcRefresh()   {	_lcRefreshTime = System.currentTimeMillis() - _lcRefreshStartTime; return _lcRefreshTime; }

	public int getQueryTimeout()
	{
		return _sqlQueryTimeout;
	}

	public void setQueryTimeout(int queryTimeout)
	{
		_sqlQueryTimeout = queryTimeout;
	}
	/**  */
	public void setQueryTimeout(int queryTimeout, boolean saveProps) 
	{ 
		// No need to continue if we are not changing it
		if (getQueryTimeout() == queryTimeout)
			return;

		setQueryTimeout(queryTimeout);
		
		if (saveProps)
			saveProps();

		if (getTabPanel() != null)
			getTabPanel().setQueryTimeout(queryTimeout);
	}

	/** Used by the: Create 'Offline Session' Wizard */
	public List<CmSettingsHelper> getLocalSettings()
	{
		return null;
	}

	/** Used by the: Config Servlet */
	public List<CmSettingsHelper> getCollectorOptions()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		String base = this.getName() + ".";
		
		list.add(new CmSettingsHelper("Is Paused"                                 , base + PROPKEY_sampleDataIsPaused         , Boolean.class, isDataPollingPaused()         , getDefaultIsDataPollingPaused()          , "Used to temporarily pause collection of this counters" ));
		list.add(new CmSettingsHelper("Postpone Time"                             , base + PROPKEY_postponeTime               , Integer.class, getPostponeTime()             , getDefaultPostponeTime()                 , "If you want to skip some intermidiate samples, Here you can specify minimum seconds between samples. tip: '10m' is 10 minutes, '24h' is 24 hours" ));
		list.add(new CmSettingsHelper("Is Postpone Enabled"                       , base + PROPKEY_postponeIsEnabled          , Boolean.class, isPostponeEnabled()           , getDefaultPostponeIsEnabled()            , "Should we use postpone time or not. This mean that we can temporaraly disable the postpone, without changing the postpone time..." ));
		list.add(new CmSettingsHelper("Query Timeout"                             , base + PROPKEY_queryTimeout               , Integer.class, getQueryTimeout()             , getDefaultQueryTimeout()                 , "How long do we wait for DBMS to deliver results for this Performance Counter... Empty value = set to the default value."  ));
		list.add(new CmSettingsHelper("Reset Negative delta/rate counters to Zero", base + PROPKEY_negativeDiffCountersToZero , Boolean.class, isNegativeDiffCountersToZero(), getDefaultIsNegativeDiffCountersToZero() , "If the differance between 'this' and 'previous' data sample has negative counter values, reset them to be <b>zero</b>"  ));

		list.add(new CmSettingsHelper("Save Counters"                             , base + PROPKEY_persistCounters            , Boolean.class, isPersistCountersEnabled()    , getDefaultIsPersistCountersEnabled()     , "Save counters to PCS (Persistent Counter Storage)." ));
		list.add(new CmSettingsHelper("Save Counters Abs"                         , base + PROPKEY_persistCounters_abs        , Boolean.class, isPersistCountersAbsEnabled() , getDefaultIsPersistCountersAbsEnabled()  , "Save Absolute Counter Values" ));
		list.add(new CmSettingsHelper("Save Counters Diff"                        , base + PROPKEY_persistCounters_diff       , Boolean.class, isPersistCountersDiffEnabled(), getDefaultIsPersistCountersDiffEnabled() , "Save Difference Calculation between two samples"));
		list.add(new CmSettingsHelper("Save Counters Rate"                        , base + PROPKEY_persistCounters_rate       , Boolean.class, isPersistCountersRateEnabled(), getDefaultIsPersistCountersRateEnabled() , "Save the Calculated Number per second (diff values / seconds between two samples)."));

		return list;
	}

	/**
	 * Check if a specific DBMS Option is enabled 
	 * 
	 * @return true if it's enabled... false if it's disabled
	 * @throws RuntimeException if the Counter Cotroller is not yet initialized
	 */
	public boolean isDbmsOptionEnabled(DbmsOption dbmsOption)
	{
		ICounterController cc = getCounterController();
		
		if (cc == null)
			throw new RuntimeException("The Counter Controller is null.");

		return cc.isDbmsOptionEnabled(dbmsOption);
	}

	/**
	 * Called from GeCountersGui/GetCountersNoGui to check if we should querey the monitored server
	 * for Counters. 
	 * @return true if data should be polled
	 */
	public boolean isRefreshable()
	{
		//---------------------------
		// GUI
		//---------------------------
		if (DbxTune.hasGui())
		{
			if ( ! isActive() )
			{
				setValidSampleData(false);
				return false;
			}

			//-------------------------------------
			// SHOULD WE REFRESH OR NOT
			//-------------------------------------
			boolean refresh = false;

			// Always for Summary
			// isRefreshable() is overridden in a subclass and always returns true
//			if ( getName().equals(SummaryPanel.CM_NAME) )
//				refresh = true;

			// The Summary CM (or the Controller) demands that we refresh this CM
			// This could be that the Summary CM has found a "long running transaction" or a "blocking lock" or something simular
			if ( getCounterController().isCmInDemandRefreshList(this.getName()) )
				refresh = true;

			// Current TAB is visible
//			if ( equalsTabPanel(MainFrame.getActiveTab()) )
			if ( getGuiController() != null && equalsTabPanel(getGuiController().getActiveTab()) )
				refresh = true;

			// Current TAB is un-docked (in it's own window)
			if (getTabPanel() != null && getGuiController() != null)
			{
				GTabbedPane tp = getGuiController().getTabbedPane();
				if (tp.isTabUnDocked(getDisplayName()))
					refresh = true;
				
//				String parentTitle = tp.getParentTitleName(getDisplayName());
//if (getName().equals("CmCachePools")) System.out.println("1: isRefreshable() cm='"+getName()+"', ParentTitle='"+parentTitle+"'.");
//				if (parentTitle != null)
//				{
//					Component parentComp = tp.getComponentAtTitle(parentTitle);
//if (getName().equals("CmCachePools")) System.out.println("2: --- isRefreshable() cm='"+getName()+"', ParentTitle='"+parentTitle+"', parentComp.classname='"+parentComp.getClass().getName()+"'");
//					if (parentComp instanceof GTabbedPane)
//					{
//						GTabbedPane parentTabbedPane = (GTabbedPane)parentComp;
//if (getName().equals("CmCachePools")) System.out.println("3: --- --- --- isRefreshable() cm='"+getName()+"', ParentTitle='"+parentTitle+"', parentTabbedPane='"+parentTabbedPane.getClass().getName()+"', parentTabbedPane.isTabUnDocked(parentTitle)="+parentTabbedPane.isTabUnDocked(parentTitle));
//						if (parentTabbedPane.isTabUnDocked(parentTitle))
//						{
//							String selectedTitle = parentTabbedPane.getSelectedTitle(true);
//if (getName().equals("CmCachePools")) System.out.println("4: --- --- --- isRefreshable() cm='"+getName()+"', ParentTitle='"+parentTitle+"', selectedTitle='"+selectedTitle+"'. cmTabName='"+getDisplayName()+"'");
//							if (getDisplayName().equals(selectedTitle))
//								refresh = true;
//						}
//					}
//				}
			}
//System.out.println(getName()+".isRefreshable(): After isTabUndocked(): refresh="+refresh);

			// Current CM has active graphs
			if ( hasActiveGraphs() )
				refresh = true;

			// Background poll is checked
			if ( isBackgroundDataPollingEnabled() )
				refresh = true;

			// Store data in DB && we have storage
			if ( isPersistCountersEnabled() && PersistentCounterHandler.hasInstance() )
				refresh = true;

			// NO-REFRESH if data polling is PAUSED
			if ( isDataPollingPaused() )
				refresh = false;

			// Check postpone
			if ( getTimeToNextPostponedRefresh() > 0 )
			{
				if (_logger.isDebugEnabled()) 
					_logger.debug("Next refresh for the cm '"+getName()+"' will have to wait '"+TimeUtils.msToTimeStr(getTimeToNextPostponedRefresh())+"'.");
				refresh = false;
			}

			// Check if any CM's that depends on Me needs refresh at this time
			// Which overrides the above PostPoneTime
			if (doAnyDependantCmNeedMeToRefresh())
				refresh = true;
			
			// If we are not connected anymore, do not try to refresh
			if (refresh)
				refresh = isConnected();

			if ( ! refresh)
				setValidSampleData(false);

//System.out.println(getName()+".isRefreshable(): <<< refresh="+refresh);
			return refresh;
		}
		//---------------------------
		// NO-GUI
		//---------------------------
		else
		{
			if ( ! isActive() )
				return false;

			boolean refresh = false;
			if ( isPersistCountersEnabled() )
				refresh = true;

			// The Summary CM (or the Controller) demands that we refresh this CM
			// This could be that the Summary CM has found a "long running transaction" or a "blocking lock" or something simular
			if ( getCounterController().isCmInDemandRefreshList(this.getName()) )
				refresh = true;
			
			// Check postpone
			if ( getTimeToNextPostponedRefresh() > 0 )
			{
				if (_logger.isDebugEnabled()) 
					_logger.debug("Next refresh for the cm '"+getName()+"' will have to wait '"+TimeUtils.msToTimeStr(getTimeToNextPostponedRefresh())+"'.");
				refresh = false;
			}

			return refresh;
		}
	}

	public boolean isDiffCalcEnabled() 
	{
		if ( _diffColumns == null || (_diffColumns != null && _diffColumns.length == 0) )
				return false;
		
		return true;
	}

//	public void setNegativeDiffCountersToZero(boolean b)
//	{
//		setNegativeDiffCountersToZero(b, true);
//	}
	public void setNegativeDiffCountersToZero(boolean b, boolean saveProps)
	{
		// No need to continue if we are not changing it
		if (isNegativeDiffCountersToZero() == b)
			return;

		_negativeDiffCountersToZero = b;
		if (saveProps)
			saveProps();

		if (getTabPanel() != null)
			getTabPanel().setOptionNegativeDiffCntToZero(b);
	}
	public boolean isNegativeDiffCountersToZero()
	{
		return _negativeDiffCountersToZero;
	}

//	public void setFilterAllZero(boolean b)
//	{
//		setFilterAllZero(b, true);
//	}
	public void setFilterAllZero(boolean b, boolean saveProps)
	{
		// No need to continue if we are not changing it
		if (isFilterAllZero() == b)
			return;

		_filterAllZeroDiffCounters = b;
		if (saveProps)
			saveProps();
	}
	public boolean isFilterAllZero()
	{
		return _filterAllZeroDiffCounters;
	}

//	public void setPauseDataPolling(boolean b)
//	{
//		setPauseDataPolling(b, true);
//	}
	public void setPauseDataPolling(boolean b, boolean saveProps)
	{
		// No need to continue if we are not changing it
		if (isDataPollingPaused() == b)
			return;

		_sampleDataIsPaused = b;
		if (saveProps)
			saveProps();

		if (getTabPanel() != null)
			getTabPanel().setOptionPauseDataPolling(b);
	}

	public boolean isDataPollingPaused()
	{
		return _sampleDataIsPaused;
	}

	
//	public void setBackgroundDataPollingEnabled(boolean b)
//	{
//		setBackgroundDataPollingEnabled(b, true);
//	}
	public void setBackgroundDataPollingEnabled(boolean b, boolean saveProps)
	{
		// No need to continue if we are not changing it
		if (isBackgroundDataPollingEnabled() == b)
			return;

		_sampleDataInBackground = b;
		if (saveProps)
			saveProps();

		if (getTabPanel() != null)
			getTabPanel().setOptionEnableBgPolling(b);
	}

	public boolean isBackgroundDataPollingEnabled()
	{
		return _sampleDataInBackground;
	}
	public boolean isBackgroundDataPollingEditable()
	{
		return ! hasActiveGraphs();
	}

//	public void setPersistCounters(boolean b)
//	{
//		setPersistCounters(b, true);
//	}
	public void setPersistCounters(boolean b, boolean saveProps)
	{
		// No need to continue if we are not changing it
		if (isPersistCountersEnabled() == b)
			return;

		_persistCounters = b;
		//_logger.error("PERSIST setPersistData() method, NOT FULLY IMPLEMENTED.", new Exception("PERSIST setPersistData() method, NOT FULLY IMPLEMENTED.") );
		if (saveProps)
			saveProps();

		if (getTabPanel() != null)
			getTabPanel().setOptionPersistCounters(b);
	}
	public boolean isPersistCountersEnabled()
	{
		return _persistCounters;
	}
	
//	public void setPersistCountersAbs(boolean b)
//	{
//		setPersistCountersAbs(b, true);
//	}
	public void setPersistCountersAbs(boolean b, boolean saveProps)
	{
		// No need to continue if we are not changing it
		if (isPersistCountersAbsEnabled() == b)
			return;

		_persistCountersAbs = b;
		//_logger.error("PERSIST setPersistData() method, NOT FULLY IMPLEMENTED.", new Exception("PERSIST setPersistData() method, NOT FULLY IMPLEMENTED.") );
		if (saveProps)
			saveProps();

		if (getTabPanel() != null)
			getTabPanel().setOptionPersistCountersAbs(b);
	}
	public boolean isPersistCountersAbsEnabled()
	{
		return _persistCountersAbs;
	}
	public boolean isPersistCountersAbsEditable()
	{
//		return isPersistCountersEnabled();
		return true;
	}
	
//	public void setPersistCountersDiff(boolean b)
//	{
//		setPersistCountersDiff(b, true);
//	}
	public void setPersistCountersDiff(boolean b, boolean saveProps)
	{
		// No need to continue if we are not changing it
		if (isPersistCountersDiffEnabled() == b)
			return;

		_persistCountersDiff = b;
		//_logger.error("PERSIST setPersistData() method, NOT FULLY IMPLEMENTED.", new Exception("PERSIST setPersistData() method, NOT FULLY IMPLEMENTED.") );
		if (saveProps)
			saveProps();

		if (getTabPanel() != null)
			getTabPanel().setOptionPersistCountersDiff(b);
	}
	public boolean isPersistCountersDiffEnabled()
	{
		return _persistCountersAbs;
	}
	public boolean isPersistCountersDiffEditable()
	{
//		return isPersistCountersEnabled();
		return true;
	}
	
//	public void setPersistCountersRate(boolean b)
//	{
//		setPersistCountersRate(b, true);
//	}
	public void setPersistCountersRate(boolean b, boolean saveProps)
	{
		// No need to continue if we are not changing it
		if (isPersistCountersRateEnabled() == b)
			return;

		_persistCountersRate = b;
		//_logger.error("PERSIST setPersistData() method, NOT FULLY IMPLEMENTED.", new Exception("PERSIST setPersistData() method, NOT FULLY IMPLEMENTED.") );
		if (saveProps)
			saveProps();

		if (getTabPanel() != null)
			getTabPanel().setOptionPersistCountersRate(b);
	}
	public boolean isPersistCountersRateEnabled()
	{
		return _persistCountersAbs;
	}
	public boolean isPersistCountersRateEditable()
	{
//		return isPersistCountersEnabled();
		return true;
	}
	
	public void setDescription(String desc)
	{
		_description = desc;
	}
	/**
	 * Get a description, which can be used for tooltip etc.
	 * @return
	 * @see #getName()
	 */
	public String getDescription()
	{
		return _description;
	}
	public void setDisplayName(String str)
	{
		_displayName = str;
	}
	/**
	 * Get the long descriptive name of the cm, can be used as a 'tab name'
	 * @return
	 * @see #getName()
	 */
	public String getDisplayName()
	{
		return _displayName;
	}

	
	public void setDiffDissColumns(List<String> cols) { setDiffDissColumns(cols.toArray(new String[cols.size()])); }
	public void setDiffColumns    (List<String> cols) { setDiffColumns    (cols.toArray(new String[cols.size()])); }
	public void setPctColumns     (List<String> cols) { setPctColumns     (cols.toArray(new String[cols.size()])); }

	public void setDiffDissColumns(String[] cols) { _diffDissColumns = cols; }
	public void setDiffColumns(String[] cols)     { _diffColumns     = cols; }
	public void setPctColumns(String[] cols)      { _pctColumns      = cols; }

	public String[] getDiffDissColumns()     { return _diffDissColumns; }
	public String[] getDiffColumns()         { return _diffColumns; }
	public String[] getPctColumns()          { return _pctColumns; }

	public boolean isDiffDissColumn(int col) { return _isDiffDissCol == null ? false : _isDiffDissCol[col]; }
	public boolean isDiffColumn(int col)     { return _isDiffCol     == null ? false : _isDiffCol[col]; }
	public boolean isPctColumn(int col)      { return _isPctCol      == null ? false : _isPctCol[col]; }

//	public boolean discardDiffPctHighlighterOnAbsTable() { return _dataSource == DATA_ABS; }
	public boolean discardDiffHighlighterOnAbsTable() { return getDataSource() == DATA_ABS; }
	public boolean discardPctHighlighterOnAbsTable()  { return false; } // false == SHOW PCT values as RED even in ABS samples


	public List<String> getPk()
	{
		return _pkCols;
	}
	/**
	 * Set the list of columns that should be used as a primary key for this Performance Counter.<br>
	 * set it to null if the Performance Counter only generates 1 row.
	 * @param pkCols
	 */
	public void setPk(List<String> pkCols)
	{
		_pkCols = pkCols;
	}

	/**
	 * Get the raw data from the underlying TableModel<br>
	 * This is used by the Persistent Counter Storage.
	 * <p>
	 * <b>NOTE:</b> maybe we should rewrite the PCS to use TableModel instead
	 * 
	 * @param whatData DATA_ABS or DATA_DIFF or DATA_RATE
	 * @return The raw data...
	 */
	public List<List<Object>> getDataCollection(int whatData)
	{
		CounterSample data = null;

		if      (whatData == DATA_ABS)  data = _newSample;
		else if (whatData == DATA_DIFF) data = _diffData;
		else if (whatData == DATA_RATE) data = _rateData;
		else
			throw new RuntimeException("Only ABS, DIFF, or RATE data is available.");

		if (data == null)
			return null;

		return data.getDataCollection();
	}

	/**
	 * Get the column names in a List from the underlying TableModel<br>
	 * This is used by the Persistent Counter Storage
	 * <p>
	 * <b>NOTE:</b> maybe we should rewrite the PCS to use TableModel instead
	 * 
	 * @param whatData DATA_ABS or DATA_DIFF or DATA_RATE
	 * @return A list of column names
	 */
	public synchronized List<String> getColNames(int whatData)
	{
		CounterSample data = null;

		if      (whatData == DATA_ABS)  data = _newSample;
		else if (whatData == DATA_DIFF) data = _diffData;
		else if (whatData == DATA_RATE) data = _rateData;
		else
			throw new RuntimeException("Only ABS, DIFF, or RATE data is available. you passed whatData="+whatData);

		if (data == null)
			return null;

		return data.getColNames();
	}

	public TabularCntrPanel getTabPanel()
	{
		return _tabPanel;
	}
	public void setTabPanel(TabularCntrPanel tp)
	{
		_tabPanel = tp;

		if (_tabPanel != null)
		{
			_tabPanel.setCm(this);
		}
	}


	/** Map to store various Components or objects that is loosely connected to this CM, could for example have been created in a extended CM Constructor which we need to be able to grab at a later stage */
	private HashMap<String, Object> _clientProperty = new HashMap<String, Object>();
	/**
	 * Set a specific property that we need to grab somewhere else<br>
	 * This can store various Components or objects that is loosely connected to this CM, 
	 * could for example have been created in a extended CM Constructor which we need 
	 * to be able to grab at a later stage
	 * 
	 * @param key name of the loosely connected object
	 * @param value the Object itself
	 */
	public void setClientProperty(String key, Object value)
	{
		if (_logger.isDebugEnabled())
			_logger.debug(getName()+".setClientProperty(key='"+key+"', value='"+value+"') valueDataType='"+(value==null?"null":value.getClass().getName())+"'.");

		_clientProperty.put(key, value);
	}

	/**
	 * Get a specific property that we need to grab somewhere else<br>
	 * This can fetch various Components or objects that is loosely connected to this CM, 
	 * could for example have been created in a extended CM Constructor which we need 
	 * to be able to grab at a later stage
	 * 
	 * @param key name of the loosely connected object
	 * @return the object, if it couldn't be found a null value will be returned
	 */
	public Object getClientProperty(String key)
	{
		Object obj = _clientProperty.get(key);
		if (_logger.isDebugEnabled())
			_logger.debug(getName()+".getClientProperty(key='"+key+"') returns='"+obj+"', type='"+(obj==null?"null":obj.getClass().getName())+"'.");
		return obj;
	}

	/** 
	 * Override this method if you still want to sample data, 
	 * even if you are not a active tab.
	 * This would typicaly be used if a graph depends on this data 
	 */
	public boolean isDependantDataCollector()
	{
		return false;
	}

	/**
	 * Create a tool tip supplier...<br>
	 * The default is to let the CounterController create it...
	 * @return
	 */
	public GTable.ITableTooltip createToolTipSupplier()
	{
		// No need to continue if we DO NOT have a GUI
		if ( ! hasGuiController() )
			return null;

		if (hasCounterController())
			return getCounterController().createCmToolTipSupplier(this);

		return null;
	}

	public GTable.ITableTooltip getToolTipSupplier()
	{
		return _cmToolTipSupplier;
	}

	/**
	 * Get tooltip for a specific Table Column
	 * @param colName
	 * @return the tooltip
	 */
	@Override
	public String getToolTipTextOnTableColumnHeader(String colName)
	{
		String tooltip = getLocalToolTipTextOnTableColumnHeader(colName);
		if (StringUtil.hasValue(tooltip))
			return tooltip;
		
		if (_cmToolTipSupplier == null)
			return null;

		return _cmToolTipSupplier.getToolTipTextOnTableColumnHeader(colName);
	}

	/**
	 * Get description stored locally for this CM 
	 * @param colName
	 * @return a description of the column if any was present, or NULL of no description was found.
	 */
	public String getLocalToolTipTextOnTableColumnHeader(String colName)
	{
		return _cmLocalToolTipColumnDescriptions.get(colName);
	}

	/**
	 * Create a Map that holds columns description stored locally for this CM 
	 * @return
	 */
	public Map<String, String> createLocalToolTipTextOnTableColumnHeader()
	{
		return new HashMap<String, String>();
	}

	/**
	 * Initialize columns description stored locally for this CM 
	 * @param colName
	 * @return
	 */
	public void initLocalToolTipTextOnTableColumnHeader()
	{
	}

	/**
	 * Add columns description stored locally for this CM 
	 * @param colName
	 * @param description Note: this <b>can</b> be in HTML format
	 * @return Previous value that was used. returns null if it wasn't previously set
	 */
	public String setLocalToolTipTextOnTableColumnHeader(String colName, String description)
	{
		return _cmLocalToolTipColumnDescriptions.put(colName, description);
	}

	/**
	 * Get a readable PK value Map<br>
	 * This can/will be used by tooltip to show "readable" values instead if ID fields.<br>
	 * Example: DBID -> DBName 
	 * @param modelRow
	 * @return a LinkedHashMap with the values to be displayed in a tooltip. If null, then no rewrite is available; Show: PK=val
	 */
	public Map<String, String> getPkRewriteMap(int modelRow)
	{
		return null;
	}

	/**
	 * Get 1 rows as a HTML Table. <br>
	 * Left column is column names (in bold)<br>
	 * Right column is row content
	 * 
	 * @param whatData          DATA_ABS, DATA_DIFF, DATA_RATE
	 * @param mrow              row in the CounterModel
	 * @param borders           
	 * @param stripedRows
	 * @param addOuterHtmlTags
	 * @return
	 */
	public String toHtmlTableString(int whatData, int mrow, boolean borders, boolean stripedRows, boolean addOuterHtmlTags)
	{
		StringBuilder sb = new StringBuilder(1024);

		if (addOuterHtmlTags)
			sb.append("<html>\n");

		int cols = getColumnCount();
		String border      = borders ? " border=1"      : " border=0";
		String cellPadding = borders ? ""               : " cellpadding=1";
		String cellSpacing = borders ? ""               : " cellspacing=0";

		String stripeColor = "#f2f2f2"; // Light gary; //Configuration.getCombinedConfiguration().getProperty(   PROPKEY_HtmlToolTip_stripeColor,   DEFAULT_HtmlToolTip_stripeColor);
		int    maxStrLen   = Integer.MAX_VALUE;        //Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_HtmlToolTip_maxCellLength, DEFAULT_HtmlToolTip_maxCellLength);
		
		// One row for every column
		sb.append("<table class='dbx-table-basic'").append(border).append(cellPadding).append(cellSpacing).append(">\n");
		for (int c=0; c<cols; c++)
		{
			String stripeTag = "";
			if (stripedRows && ((c % 2) == 0) )
				stripeTag = " bgcolor='" + stripeColor + "'";
			
			Object objVal = getValue(whatData, mrow,c); // DATA_ABS, DATA_DIFF, DATA_RATE
			String strVal = "";
			if (objVal != null)
			{
				strVal = objVal.toString();

				// Remove any leading/ending HTML tags
				if (StringUtils.startsWithIgnoreCase(strVal, "<html>"))
				{
					strVal = strVal.substring("<html>".length());
					strVal = strVal.trim();
					if (StringUtils.endsWithIgnoreCase(strVal, "</html>"))
						strVal = strVal.substring(0, strVal.length() - "</html>".length() );
				}
				else
				{
					// check for XML content "somewhere" in the string
					if (strVal.indexOf("<?xml") >= 0)
					{
						String conf = Configuration.getCombinedConfiguration().getProperty("toHtmlTableString.xml", "TO_ESCAPED_TEXT");

						if ("TO_ESCAPED_TEXT".equals(conf))
						{
							// if there are any XML tag in the field... Then surround the value with a '<pre>' tag and escape all "xml" tags etc...
							strVal = "<pre>" + StringEscapeUtils.escapeHtml4(strVal) + "</pre>";
//							strVal = "<pre>" + StringEscapeUtils.escapeXml10(strVal) + "</pre>";
						}
					}
					else if (strVal.indexOf("<ShowPlanXML xmlns=") >= 0)
					{
						SqlServerShowPlanXmlTransformer t = new SqlServerShowPlanXmlTransformer();
						try
						{
							// Get HTML (what type/look-and-feel) is decided by configuration in: SqlServerShowPlanXmlTransformer.PROKKEY_transform
							strVal = t.toHtml(strVal);
						}
						catch (Exception ex)
						{
							strVal = "Could not translate SQL-Server ShowPlanXML to HTML text. Caught: " + ex;
							_logger.error(strVal);
						}
					}
					else
					{
						// make '\n' into '<br>'
						strVal = strVal.replace("\n", "<br>");
					}
				}
				
				// If it's a LONG String, only display first 'maxStrLen' characters...
				int strValLen = strVal.length(); 
				if (strValLen > maxStrLen)
				{
					strVal =  strVal.substring(0, maxStrLen);
					strVal += "...<br><font color='orange'><i><b>NOTE:</b> content is truncated after " + maxStrLen + " chars (actual length is "+strValLen+").</i></font>";
				}
			}
			
			sb.append("<tr").append(stripeTag).append(">\n");
			sb.append("  <td nowrap><b>").append(getColumnName(c)).append("</b>&nbsp;</td>\n");
			sb.append("  <td nowrap>")   .append(strVal)          .append("</td>\n");
			sb.append("</tr>\n");
		}
		sb.append("</table>\n");

		if (addOuterHtmlTags)
			sb.append("</html>\n");

		return sb.toString();
	}
	

	/**
	 * Get 1 rows as a Text Table. <br>
	 * Left column is column names (in bold)<br>
	 * Right column is row content
	 * 
	 * @param whatData          DATA_ABS, DATA_DIFF, DATA_RATE
	 * @param mrow              row in the CounterModel
	 * @return
	 */
	public String toTextTableString(int whatData, int mrow)
	{
		StringBuilder sb = new StringBuilder(1024);

		List<String> colNames = getColNames(whatData);
		int cols = getColumnCount();
		int maxColLen = 0;
		for (String col : colNames)
			maxColLen = Math.max(maxColLen, col.length());

		// One row for every column
		for (int c=0; c<cols; c++)
		{
			Object objVal = getValue(whatData, mrow,c); // DATA_ABS, DATA_DIFF, DATA_RATE
			
			sb.append(StringUtil.left(getColumnName(c),maxColLen)).append(": ");
			sb.append(objVal).append("\n");
		}

		return sb.toString();
	}

	/**
	 * Create a HTML Table 
	 * 
	 * @param whatData          DATA_ABS, DATA_DIFF, DATA_RATE
	 * @param conf              If we want to set specific properties on the table (like: borders=true, stripedRows=true, addOuterHtmlTags=true)
	 * @param columns           The columns we want in the table
	 * @return
	 */
	public String toHtmlTableForColumns(int whatData, Configuration conf, String... columns)
	{
		boolean borders          = conf == null ? false             : conf.getBooleanProperty("borders"         , false);
		boolean stripedRows      = conf == null ? false             : conf.getBooleanProperty("stripedRows"     , false);
		boolean addOuterHtmlTags = conf == null ? false             : conf.getBooleanProperty("addOuterHtmlTags", false);
		String  stripeColor      = conf == null ? "#f2f2f2"         : conf.getProperty       ("stripeColor"     , "#f2f2f2");
		int     maxStrLen        = conf == null ? Integer.MAX_VALUE : conf.getIntProperty    ("maxStrLen"       , Integer.MAX_VALUE);

		StringBuilder sb = new StringBuilder(1024);

		if (addOuterHtmlTags)
			sb.append("<html> \n");

		String border      = borders ? " border=1"      : " border=0";
		String cellPadding = borders ? ""               : " cellpadding=1";
		String cellSpacing = borders ? ""               : " cellspacing=0";

		// One row for every column
		sb.append("<table class='dbx-table-basic'").append(border).append(cellPadding).append(cellSpacing).append(">\n");

		// Table head
		sb.append("<thead> \n");
		sb.append("<tr> \n");
		for (String colName : columns)
		{
			sb.append("  <th>").append(colName).append("</th>\n");
		}
		sb.append("</tr> \n");
		sb.append("</thead> \n");
		
		// Table Body
		sb.append("<tbody> \n");
		int rowc = getRowCount(whatData);
		for (int r=0; r<rowc; r++)
		{
			String stripeTag = "";
			if (stripedRows && ((r % 2) == 0) )
				stripeTag = " bgcolor='" + stripeColor + "'";
			
			sb.append("<tr").append(stripeTag).append("> \n");

			for (String colName : columns)
			{
				Object objVal = null;
				String strVal = "";

				int colPos = findColumn(colName);
				
				if (colPos == -1)
					objVal = "ColName='" + colName + "': not found";
				else
					objVal = getValue(whatData, r, colPos); // DATA_ABS, DATA_DIFF, DATA_RATE
				
				if (objVal != null)
				{
					strVal = objVal.toString();

					// Remove any leading/ending HTML tags
					if (StringUtils.startsWithIgnoreCase(strVal, "<html>"))
					{
						strVal = strVal.substring("<html>".length());
						strVal = strVal.trim();
						if (StringUtils.endsWithIgnoreCase(strVal, "</html>"))
							strVal = strVal.substring(0, strVal.length() - "</html>".length() );
					}
					else
					{
						// check for XML content "somewhere" in the string
						if (strVal.indexOf("<?xml") >= 0)
						{
							if ("TO_ESCAPED_TEXT".equals(Configuration.getCombinedConfiguration().getProperty("toHtmlTableString.xml", "TO_ESCAPED_TEXT")))
							{
								// if there are any XML tag in the field... Then surround the value with a '<pre>' tag and escape all "xml" tags etc...
								strVal = "<pre>" + StringEscapeUtils.escapeHtml4(strVal) + "</pre>";
							}
						}
						else if (strVal.indexOf("<ShowPlanXML xmlns=") >= 0)
						{
							SqlServerShowPlanXmlTransformer t = new SqlServerShowPlanXmlTransformer();
							try
							{
								// Get HTML (what type/look-and-feel) is decided by configuration in: SqlServerShowPlanXmlTransformer.PROKKEY_transform
								strVal = t.toHtml(strVal);
							}
							catch (Exception ex)
							{
								strVal = "Could not translate SQL-Server ShowPlanXML to HTML text. Caught: " + ex;
								_logger.error(strVal);
							}
						}
						else
						{
							// make '\n' into '<br>'
							strVal = strVal.replace("\n", "<br>");
						}
					}
					
					// If it's a LONG String, only display first 'maxStrLen' characters...
					int strValLen = strVal.length(); 
					if (strValLen > maxStrLen)
					{
						strVal =  strVal.substring(0, maxStrLen);
						strVal += "...<br><font color='orange'><i><b>NOTE:</b> content is truncated after " + maxStrLen + " chars (actual length is "+strValLen+").</i></font>";
					}
				}
				
				sb.append("  <td nowrap>").append(strVal).append("</td> \n");
			}
			sb.append("</tr> \n");
		}
		sb.append("</tbody> \n");
		sb.append("</table> \n");

		if (addOuterHtmlTags)
			sb.append("</html> \n");

		return sb.toString();
	}
	

	/**
	 * Used by the TabularCntrPanel.JTable to get tool tip on a cell level.
	 * Override it to set specific tooltip... 
	 * @param e
	 * @param colName
	 * @param modelRow
	 * @param modelCol
	 * @return
	 */
	@Override
	public String getToolTipTextOnTableCell(MouseEvent e, String colName, Object cellValue, int modelRow, int modelCol) 
	{
		if (_cmToolTipSupplier == null)
			return null;

		// Check if we are in REFRESH for this CM, then return NULL, otherwise we may get into "strange lock/deadlock" situations with EDT (if we fetch data from the CM using get{abs|diff|rate}xxx)
		if (isInRefresh())
		{
			_logger.info("Skipping tooltip for cm="+getName()+", due to isInRefresh()=true. Otherwise we could run into strange locking situations with the Event Dispatch Thread");
//			return "Currently refreshing counters, tooltip is not available at this point, retry in a second.";
			return "Sorry: tooltip isn't available, while counters are refreshed... Retry in a second.";
		}
		
		// OR we can start to use 
//		Lock lock = new ReentrantLock();
//		......
//		if (lock.tryLock())
//		{
//			// Got the lock
//			try
//			{
//				// Process record
//			}
//			finally
//			{
//				// Make sure to unlock so that we don't cause a deadlock
//				lock.unlock();
//			}
//		}
//		else
//		{
//			// Someone else had the lock, abort
//		}		

		return _cmToolTipSupplier.getToolTipTextOnTableCell(e, colName, cellValue, modelRow, modelCol);
	}
//	@Override
//	public String getToolTipTextOnTableCell(MouseEvent e, String colName, Object cellValue, int modelRow, int modelCol) 
//	{
//		// Get tip on WaitEventID
//		if ("WaitEventID".equals(colName))
//		{
//			//Object cellVal = getValueAt(modelRow, modelCol);
//			if (cellValue instanceof Number)
//			{
//				int waitEventId = ((Number)cellValue).intValue();
//				if (waitEventId > 0)
//					return MonWaitEventIdDictionary.getInstance().getToolTipText(waitEventId);
//			}
//		}
//
//		// Get tip on Remark (at least in CMobjectActivity/CM_NAME__OBJECT_ACTIVITY)
//		if ("Remark".equals(colName))
//		{
//			//Object cellVal = getValueAt(modelRow, modelCol);
//			if (cellValue instanceof String)
//			{
//				String key = (String)cellValue;
//				if ( ! StringUtil.isNullOrBlank(key) )
//					return RemarkDictionary.getInstance().getToolTipText(key);
//			}
//		}
//
//		// If we are CONNECTED and we have a USER DEFINED TOOLTIP for this columns
//		if (cellValue != null)
//		{
//			String sql = MainFrame.getUserDefinedToolTip(getName(), colName);
//
//			// If we reading an offline database, go there to fetch data...
//			if ( sql != null && ! isConnected() )
//			{
//				// IF SPID, get values from JTable in OFFLINE MODE
//				if (    "SPID"          .equalsIgnoreCase(colName) // From a bunch of places
//				     || "OldestTranSpid".equalsIgnoreCase(colName) // from CmOpenDatabases
//				     || "KPID"          .equalsIgnoreCase(colName) // From a bunch of places
//				     || "OwnerPID"      .equalsIgnoreCase(colName) // CmSpinlockActivity
//				     || "LastOwnerPID"  .equalsIgnoreCase(colName) // CmSpinlockActivity
//				   )
//				{
//					// Determine the COLUMN name to be used in the search
//					String whereColName = "SPID";
//					if (    "KPID"          .equalsIgnoreCase(colName) // From a bunch of places
//						 || "OwnerPID"      .equalsIgnoreCase(colName) // CmSpinlockActivity
//						 || "LastOwnerPID"  .equalsIgnoreCase(colName) // CmSpinlockActivity
//					   )
//					{
//						whereColName = "KPID";
//					}
//					
//					if (MainFrame.isOfflineConnected())
//					{
//						// FIXME: _counterController is NOT set for UDC Counters (especially when initialized from OfflineStorage)
////						CountersModel cm = getCounterController().getCmByName(GetCounters.CM_NAME__PROCESS_ACTIVITY);
//						CountersModel cm = getCounterController().getCmByName(CmProcessActivity.CM_NAME);
//						TabularCntrPanel tcp = cm.getTabPanel();
//						if (tcp != null)
//						{
//							tcp.tabSelected();
//							cm = tcp.getDisplayCm();
//							if (cm != null)
//							{
//								CounterTableModel ctmAbs  = cm.getCounterDataAbs();
//								CounterTableModel ctmDiff = cm.getCounterDataDiff();
//								CounterTableModel ctmRate = cm.getCounterDataRate();
//								if (ctmRate == null)
//								{
////									return "<html>Counters of type 'rate' was not saved for Performance Counter '"+GetCounters.CM_DESC__PROCESS_ACTIVITY+"'.</html>";
//									return "<html>Counters of type 'rate' was not saved for Performance Counter '"+CmProcessActivity.SHORT_NAME+"'.</html>";
//								}
//								else
//								{
//									int cellPidInt = -1;
//									if (cellValue instanceof Number)
//										cellPidInt = ((Number)cellValue).intValue();
//									else
//									{
//										return "<html>" +
//												"Current Cell value '"+cellValue+"' is not a <i>Number</i>.<br>" +
//												"The object type is <code>"+cellValue.getClass().getName()+"</code><br>" +
//												"It must be of datatype <code>Number</code><br>" +
//												"</html>";
//									}
//										
//									int pid_pos = ctmRate.findColumn(whereColName);
//									int rowCount = ctmRate.getRowCount();
//									for (int r=0; r<rowCount; r++)
//									{
//										Object rowCellValue = ctmRate.getValueAt(r, pid_pos);
//										int rowPidInt = -1;
//										if (rowCellValue instanceof Number)
//											rowPidInt = ((Number)rowCellValue).intValue();
//										else
//											continue;
////										System.out.println("CellValue='"+cellValue+"', tableRow="+r+", Value="+ctmRate.getValueAt(r, spid_pos)+", TableObjType="+ctmRate.getValueAt(r, spid_pos).getClass().getName()+", cellValueObjType="+cellValue.getClass().getName());
//										
////										if ( cellValue.equals(ctmRate.getValueAt(r, spid_pos)) )
//										if ( cellPidInt == rowPidInt )
//										{
//											StringBuilder sb = new StringBuilder(300);
//											sb.append("<html>\n");
//											sb.append("<table border=0 cellspacing=0 >\n");
////											sb.append("<table border=1 cellspacing=0 >\n");
////											sb.append("<table BORDER=1 CELLSPACING=0 CELLPADDING=0>\n");
//
//											sb.append("<tr>");
//											sb.append("<td nowrap bgcolor=\"#cccccc\"><font color=\"#000000\"><b>").append("Column Name")      .append("</b></font></td>");
//											sb.append("<td nowrap bgcolor=\"#cccccc\"><font color=\"#000000\"><b>").append("Absolute Counters").append("</b></font></td>");
//											sb.append("<td nowrap bgcolor=\"#cccccc\"><font color=\"#000000\"><b>").append("Diff Counters")    .append("</b></font></td>");
//											sb.append("<td nowrap bgcolor=\"#cccccc\"><font color=\"#000000\"><b>").append("Rate Counters")    .append("</b></font></td>");
//											sb.append("</tr>\n");
//
//											for (int c=0; c<ctmRate.getColumnCount(); c++)
//											{
////												System.out.println("XXXX: colName='"+ctm.getColumnName(c)+"', value='"+ctm.getValueAt(r, c)+"'.");
//
//												if ( (c % 2) == 0 )
//													sb.append("<tr bgcolor=\"#ffffff\">"); // white
//												else
//													sb.append("<tr bgcolor=\"#ffffcc\">"); // light yellow
//
//												sb.append("<td nowrap bgcolor=\"#cccccc\"><font color=\"#000000\"><b>").append(ctmRate.getColumnName(c)).append("</b></font></td>");
//
//												sb.append("<td nowrap>").append(ctmAbs ==null?"":ctmAbs .getValueAt(r, c)).append("</td>");
//												sb.append("<td nowrap>").append(ctmDiff==null?"":ctmDiff.getValueAt(r, c)).append("</td>");
//												sb.append("<td nowrap>").append(ctmRate==null?"":ctmRate.getValueAt(r, c)).append("</td>");
//												sb.append("</tr>\n");
//											}
//											sb.append("</table>\n");
//											sb.append("</html>\n");
//											return sb.toString();
//										}
//									}
//								}
////								return "<html>Can't find the SPID '"+cellValue+"' in Performance Counter '"+GetCounters.CM_DESC__PROCESS_ACTIVITY+"'.</html>";
//								return "<html>Can't find the "+whereColName+" '"+cellValue+"' in Performance Counter '"+CmProcessActivity.SHORT_NAME+"'.</html>";
//							}
//						}
//					} // end: offline
//				} // end: SPID
//
//				return "<html>" +
//				       "No runtime tool tip available for '"+colName+"'. <br>" +
//				       "Not connected to the monitored server.<br>" +
//				       "</html>";
//			}
//
//			if (sql != null)
//			{
//				try
//				{
//					Connection conn = getCounterController().getMonConnection();
//
//					StringBuilder sb = new StringBuilder(300);
//					sb.append("<html>\n");
////					sb.append("<table BORDER=1 CELLSPACING=0 CELLPADDING=0>\n");
//					sb.append("<table border=1>\n");
//
//					PreparedStatement stmt = conn.prepareStatement(sql);
//					stmt.setObject(1, cellValue);
//
//					ResultSet rs = stmt.executeQuery();
//					ResultSetMetaData rsmd = rs.getMetaData();
//					int cols = rsmd.getColumnCount();
//
//					sb.append("<tr>");
//					for (int c=1; c<=cols; c++)
//						sb.append("<td nowrap>").append(rsmd.getColumnName(c)).append("</td>");
//					sb.append("</tr>\n");
//
//					while (rs.next())
//					{
//						sb.append("<tr>");
//						for (int c=1; c<=cols; c++)
//							sb.append("<td nowrap>").append(rs.getObject(c)).append("</td>");
//						sb.append("</tr>\n");
//					}
//					sb.append("</table>\n");
//					sb.append("</html>\n");
//
//					for (SQLWarning sqlw = stmt.getWarnings(); sqlw != null; sqlw = sqlw.getNextWarning())
//					{
//						// IGNORE: DBCC execution completed. If DBCC printed error messages, contact a user with System Administrator (SA) role.
//						if (sqlw.getMessage().startsWith("DBCC execution completed. If DBCC"))
//							continue;
//
//						sb = sb.append(sqlw.getMessage()).append("<br>");
//					}
//					rs.close();
//					stmt.close();
//					
//					return sb.toString();
//				}
//				catch (SQLException ex)
//				{
//					_logger.warn("Problems when executing sql for cm='"+getName()+"', getToolTipTextOnTableCell(colName='"+colName+"', cellValue='"+cellValue+"'): "+sql, ex);
//					return "<html>" +  
//					       "Trying to get tooltip details for colName='"+colName+"', value='"+cellValue+"'.<br>" +
//					       "Problems when executing sql: "+sql+"<br>" +
//					       ex.toString() +
//					       "</html>";
//				}
//			}
//		}
//
//		return null;
//	}


	//-------------------------------------------
	// BEGIN: TrendGraph
	//-------------------------------------------
	public boolean hasTrendGraph()
	{
		return ! (_trendGraphs.size() == 0);
	}
	
	public boolean hasActiveGraphs()
	{
		if (_trendGraphs.size() == 0)
			return false;

		boolean active = false;
		for (Iterator<String> it = _trendGraphs.keySet().iterator(); it.hasNext();)
		{
			String graphName = (String)it.next();
			TrendGraph tg = getTrendGraph(graphName);
			if (tg.isGraphEnabled())
			{
				active = true;
				break;
			}
		}

		return active;
	}
	
	public void updateGraph(TrendGraph tg)
	{
		if (tg == null)
			return;
		TrendGraphDataPoint tgdp = getTrendGraphData(tg.getName());
		tg.addPoint(tgdp);
		tg.setMinimumChartArea();
	}

	public void updateGraphs()
	{
		if (_trendGraphs.size() == 0)
			return;

		for (Iterator<String> it = _trendGraphs.keySet().iterator(); it.hasNext();)
		{
			String graphName = (String)it.next();
			TrendGraph tg = getTrendGraph(graphName);
			updateGraph(tg);
		}
	}

	/**
	 * Add a TrendGraph<br>
	 * This is used for both GUI and NO-GUI mode (in NO-GUI mode only the TrendGraphDataPoint object is created)
	 * 
	 * @param name               Name of the trend graph (probably used in updateGraph() to check what trend graph is about to be updated)
	 * @param chkboxText         Content of the JCheckbox text in the main menu
	 * @param graphLabel         The Label of the graph
	 * @param graphProps         A JSON String with Graph properties like x/y axis scaling labels
	 * @param seriesLabels       An String[] of series labels (that will be deisplaied at the graph bottom). This can be NULL if seriesLabelType is Static
	 * @param seriesLabelType    If the seriesLabels are static or dynamically added
	 * @param isPercentGraph     true if the MAX scale of the graph should be 100
	 * @param visibleAtStart     true if the graph should be visable when starting DbxTune the first time
	 * @param needsVersion       What DBMS version is this graph valid from. (use com.dbxtune.utils.Ver to generate a value). 0 = any version
	 * @param minimumHeight      Minimum height of the graph content in pixels. (-1 = Let the layout manager decide this)
	 */
//	public void addTrendGraph(String name, String chkboxText, String graphLabel, String[] seriesLabels, LabelType seriesLabelType, TrendGraphDataPoint.Category graphCategory, boolean isPercentGraph, boolean visibleAtStart, long needsVersion, int minimumHeight)
	public void addTrendGraph(String name, String chkboxText, String graphLabel, String graphProps, String[] seriesLabels, LabelType seriesLabelType, TrendGraphDataPoint.Category graphCategory, boolean isPercentGraph, boolean visibleAtStart, long needsVersion, int minimumHeight)
	{
		if (LabelType.Dynamic.equals(seriesLabelType))
			seriesLabels = TrendGraphDataPoint.RUNTIME_REPLACED_LABELS;
		
		if (seriesLabels == null)
			seriesLabels = new String[] {""};
		
//		String graphProps = null; // FIXME this should be the method parameter graphProps

		// Get UserDefined: graph 'category' properties
		Configuration conf = Configuration.getCombinedConfiguration();
		String udCategoryProp = this.getClass().getSimpleName()+".graph."+name+".category";
		String udCategory     = conf.getProperty(udCategoryProp, null);
		if (StringUtil.hasValue(udCategory))
		{
			_logger.info(getName() + ": Overriding/Reading default value of '"+graphCategory+"' for Graph Category using property '"+udCategoryProp+"' with value '"+udCategory+"'.");

			try {
				graphCategory = Category.valueOf(udCategory);
			} catch (IllegalArgumentException e) {
				_logger.error("Problems parsing Graph Category Value '"+udCategory+"' for the property '"+udCategoryProp+"'. known values: "+StringUtil.toCommaStr(Category.values())+". Caught: "+e);
			}
		}

		TrendGraphDataPoint tgdp = new TrendGraphDataPoint(name, graphLabel, graphProps, graphCategory, isPercentGraph, visibleAtStart, seriesLabels, seriesLabelType); 
		addTrendGraphData(name, tgdp);
		
		
		if (getGuiController() != null && getGuiController().hasGUI())
		{
			CountersModel cm = this;
			long startTime = System.currentTimeMillis();
			
			// GRAPH
			TrendGraph tg = new TrendGraph(name, chkboxText, graphLabel, 
				seriesLabels, 
				isPercentGraph,
				cm, 
				visibleAtStart,
				needsVersion, 
				minimumHeight);
			
			long execTime = System.currentTimeMillis() - startTime;
			if (_logger.isDebugEnabled())
				_logger.debug("addTrendGraph('"+name+"'): crTimeMs="+execTime);
//System.out.println("addTrendGraph('"+name+"'): crTimeMs="+execTime);

			addTrendGraph(name, tg, true);
		}
	}

	/**
	 * Add a TrendGraph to this cm
	 * 
	 * @param name Name of the Graph
	 * @param tg Trend Graph object
	 * @param addToSummary add this graph to the MainFrame summary panel.
	 */
	private void addTrendGraph(String name, TrendGraph tg, boolean addToSummary)
	{
		if (_trendGraphs.containsKey(name))
			throw new RuntimeException("Sorry the trend graph named '"+name+"' is already used, the name must be unique.");

		_trendGraphs.put(name, tg);
		
		tg.setCm(this);

		if (addToSummary)
		{
			MainFrame.addGraphViewMenu( tg.getViewMenuItem() );
			getCounterController().getSummaryPanel().addTrendGraph(tg);
		}
	}
	
	public TrendGraph getTrendGraph(String name)
	{
		if (StringUtil.isNullOrBlank(name))
			return null;

		return (TrendGraph) _trendGraphs.get(name);
	}
	
	public Map<String,TrendGraph> getTrendGraphs()
	{
		return _trendGraphs;
	}

	public void setTrendGraphEnable(String name, boolean enable)
	{
		TrendGraph tg = getTrendGraph(name);
		if (tg == null)
			throw new RuntimeException("The TrendGraph '"+name+"', couldn't be found in the CounterModel '"+getName()+"'.");

		tg.setEnable(enable);
	}

	public boolean isTrendGraphEnabled(String name)
	{
		TrendGraph tg = getTrendGraph(name);
		if (tg == null)
			throw new RuntimeException("The TrendGraph '"+name+"', couldn't be found in the CounterModel '"+getName()+"'.");

		return tg.isGraphEnabled();
	}

	public void initTrendGraphForVersion(long serverVersion)
	{
		if (_trendGraphs.size() == 0)
			return;

		for (Iterator<String> it = _trendGraphs.keySet().iterator(); it.hasNext();)
		{
			String graphName = (String)it.next();
			TrendGraph tg = getTrendGraph(graphName);
			
			tg.initializeGraphForVersion(serverVersion);
		}
	}

	/**
	 * Override this to add graph specific menu items
	 * @param list
	 */
	public List<JComponent> createGraphSpecificMenuItems()
	{
		return null;
	}

	//-------------------------------------------
	// END: TrendGraph
	//-------------------------------------------

	public static String[] getValidGraphMethods(boolean allMethods)
	{
		return allMethods ? _graphMethodsAll : _graphMethodsAbs;
	}
	
	public static String[] getValidGraphMethods(String graphType, boolean allMethods)
	{
		if      ("byCol".equals(graphType))  return allMethods ? _graphMethodsByColAll : _graphMethodsByColAbs;
		else if ("byRow".equals(graphType))  return _graphMethodsByRow;
		else                                 return new String[] {};
	}
	
	public static String getValidGraphMethodsString(boolean allMethods)
	{
		String ops = "";
		String[] sa = allMethods ? _graphMethodsAll : _graphMethodsAbs;
		for (int i=0; i<sa.length; i++)
		{
			ops += "'" + sa[i] + "', ";
		}
		return ops;
	}
	
	public static boolean isValidGraphMethod(String op, boolean allMethods)
	{
		String[] sa = allMethods ? _graphMethodsAll : _graphMethodsAbs;
		for (int i=0; i<sa.length; i++)
		{
			if (sa[i].equals(op))
				return true;
		}
		return false;
	}

	public void setGraphCalculations(String[] dataCols, String[] dataOpers)
	{
		_graphDataColNames = dataCols;
		_graphDataMethods  = dataOpers;
	}

	public int getGraphType()
	{
		return _graphType;
	}
	public void setGraphType(int type)
	{
		if (type != GRAPH_TYPE_BY_COL && type != GRAPH_TYPE_BY_ROW)
		{
			throw new IllegalArgumentException("Graph type can only be 'TYPE_BY_COL/byCol' or 'TYPE_BY_ROW/byRow'.");
		}
		_graphType = type;
	}

	//-------------------------------------------
	// BEGIN: TrendGraphDataPoint
	//-------------------------------------------
	private void updateUserDefinedGraphData(TrendGraphDataPoint tgdp)
	{
		if (tgdp == null)
			return;

		if ( _graphDataColNames.length == 0)
			return;
		
		String graphName = tgdp.getName();

		if (_graphType == GRAPH_TYPE_BY_COL)
		{
			Double[] dataArray = new Double[_graphDataColNames.length];
			for (int i=0; i<_graphDataColNames.length; i++)
			{
				String colName = _graphDataColNames[i];
				String op      = _graphDataMethods[i];
				Double data    = null;

				if      (op.equals("rateMax"))       data = this.getRateValueMax(colName);
				else if (op.equals("rateMin"))       data = this.getRateValueMin(colName);
				else if (op.equals("rateAvg"))       data = this.getRateValueAvg(colName);
				else if (op.equals("rateAvgGtZero")) data = this.getRateValueAvgGtZero(colName);
				else if (op.equals("rateSum"))       data = this.getRateValueSum(colName);

				else if (op.equals("diffMax"))       data = this.getDiffValueMax(colName);
				else if (op.equals("diffMin"))       data = this.getDiffValueMin(colName);
				else if (op.equals("diffAvg"))       data = this.getDiffValueAvg(colName);
				else if (op.equals("diffAvgGtZero")) data = this.getDiffValueAvgGtZero(colName);
				else if (op.equals("diffSum"))       data = this.getDiffValueSum(colName);

				else if (op.equals("absMax"))        data = this.getAbsValueMax(colName);
				else if (op.equals("absMin"))        data = this.getAbsValueMin(colName);
				else if (op.equals("absAvg"))        data = this.getAbsValueAvg(colName);
				else if (op.equals("absAvgGtZero"))  data = this.getAbsValueAvgGtZero(colName);
				else if (op.equals("absSum"))        data = this.getAbsValueSum(colName);
				else
				{
					_logger.warn("Graph named '"+graphName+"' has unknown operator '"+op+"' for column '"+colName+"', cm='"+this.getName()+"'.");
				}

				dataArray[i] = data;
			}
			if (_logger.isDebugEnabled())
			{
				String debugStr = "Graph named '" + graphName + "', cm='"+this.getName()+"', type 'byCol' add data: ";
				for (int i=0; i<_graphDataColNames.length; i++)
				{
					debugStr += _graphDataColNames[i] + "='" + dataArray[i] + "', ";
				}
				_logger.debug(debugStr);
			}

			// SET DATA
			tgdp.setDataPoint(this.getTimestamp(), dataArray);
		}
		else if (_graphType == GRAPH_TYPE_BY_ROW)
		{
			// One graph line for each row, the array size needs to be:
			String[] labelArray = new String[this.getRowCount()];
			Double[] dataArray  = new Double[this.getRowCount()];

			// Get Label to write below graph, if -1, the PK value for row is used
			String[] strArr = tgdp.getLabel();
			int labelPos = -1;
			if (strArr != null && strArr.length >= 1)
				labelPos = this.findColumn(strArr[0]);
			
			for (int i=0; i<this.getRowCount(); i++)
			{
				Object labelObj = null;
				String colName  = _graphDataColNames[0];
				String op       = _graphDataMethods[0];
				Double data     = null;

				// Compose a LABEL
				if (labelPos >= 0)
					labelObj = this.getValueAt(i, labelPos);
				else
				{
					if      (op.equals("rateVal")) labelObj = this.getRatePkValue(i);
					else if (op.equals("diffVal")) labelObj = this.getDiffPkValue(i);
					else if (op.equals("absVal"))  labelObj = this.getAbsPkValue(i);
				}
				if (labelObj == null)
					labelObj = "row-"+i;

				// Get data
				if      (op.equals("rateVal")) data = this.getRateValueAsDouble(i, colName);
				else if (op.equals("diffVal")) data = this.getDiffValueAsDouble(i, colName);
				else if (op.equals("absVal"))  data = this.getAbsValueAsDouble(i,  colName);
				else
				{
					_logger.warn("Graph named '"+graphName+"' has unknown operator '"+op+"' for column '"+colName+"', cm='"+this.getName()+"'.");
				}

				 // label: remove any PK delimiter at the end
				String labelStr = labelObj.toString().trim();
				if (labelStr.endsWith(CounterSample.PK_STR_DELIMITER))
					labelStr = labelStr.substring(0, labelStr.length()-1);
				
				labelArray[i] = labelStr;
				dataArray[i]  = data;
			}

			if (_logger.isDebugEnabled())
			{
				_logger.debug("Graph named '" + graphName + "', cm='"+this.getName()+"', type 'byRow' add data: ");;
				for (int i=0; i<dataArray.length; i++)
				{
					_logger.debug(" :::: row="+i+", data='"+dataArray[i]+"', label='"+labelArray[i]+"'.");
				}
			}

			// SET DATA
			tgdp.setDataPoint(this.getTimestamp(), labelArray, dataArray);
		}
		else
		{
			_logger.warn("Unknown graph type("+_graphType+").");
		}
	} // end: method

	
	/** Called so the object can calculate what data to be used/send to any graphs in a later stage 
	 * 	This method is called once for every "data sample" we do. */
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		updateUserDefinedGraphData(tgdp);
	}
	
	/** Called so the object can calculate what data to be used/send to any graphs in a later stage 
	 *  This method is called once for every "data sample" we do. 
	 *  and if not overridden, it will simple just call updateGraphData(TrendGraphDataPoint) for each installed TrendGraphDataPoint */
	public void updateGraphData()
	{
		if (_trendGraphsData.size() == 0)
			return;
		
		if (getRowCount() == 0)
		{
			_logger.debug("Current sample has 0 rows for CM '"+getName()+"' when trying to update it's graphs. Skipping updateGraphData().");
			return;
		}

		for (TrendGraphDataPoint tgdp : _trendGraphsData.values()) 
		{
			try
			{
				updateGraphData(tgdp);
				
				//System.out.println("cm='"+StringUtil.left(this.getName(),25)+"', _trendGraphData="+tgdp);
				if (_logger.isDebugEnabled())
					_logger.debug("cm='"+StringUtil.left(this.getName(),25)+"', _trendGraphData="+tgdp);

				// Check if history is ENABLED for this TrendGraph
				if (isGraphDataHistoryEnabled(tgdp.getName()))
				{
					if (tgdp.hasData())
					{
						// Get instance
						String cmName    = this.getName();
						String chartName = tgdp.getName();
						ChartDataHistorySeries cdhs = ChartDataHistoryManager.getInstance(cmName, chartName, getGraphDataHistoryTimeInterval(chartName));

						// Add this data point to the history
						cdhs.add(tgdp);
					}
				}
			}
			catch (Exception ex)
			{
				_logger.warn("Problems in updateGraphData() for CM='" + getName() + "', graphName='" + tgdp.getName() + "'.", ex);
			}
		}
	}
	

	/**
	 * SHould we save a "in-memory" history for this graph.<br>
	 * This will/may be  used for Alarms and other various reasons!
	 *  
	 * @param name  Name of the graph/chart
	 * @return
	 */
	public boolean isGraphDataHistoryEnabled(String graphName)
	{
		return false;
	}

	/**
	 * For how many minutes should we hold the in-memory counter values
	 *  
	 * @param name  Name of the graph/chart
	 * @return minutes to hold (default is -1, which means to use the default value)
	 */
	public int getGraphDataHistoryTimeInterval(String name)
	{
		return ChartDataHistorySeries.DEFAULT_KEEP_AGE_IN_MINUTES; // Default: 60 minutes
	}

	/**
	 * Get a HTML 'img' tag looking something like:<br> <code>&lt;img  width='500' height='180' src='data:image/png;base64,iVBORw0...'&gt;</code>
	 * @param graphName Name of the Graph
	 * @param seriesNames     Only show the following series (empty for ALL series)
	 * @return null if not found, else a HTML 'img' tag
	 */
	public String getGraphDataHistoryAsHtmlImage(String graphName, String... seriesNames)
	{
		return getGraphDataHistoryAsHtmlImage(graphName, 0, seriesNames);
	}


	/**
	 * Get a HTML 'img' tag looking something like:<br> <code>&lt;img  width='500' height='180' src='data:image/png;base64,iVBORw0...'&gt;</code>
	 * @param graphName Name of the Graph
	 * @param timeLimit       0 = All data, # = Only last # MINUTES of data points 
	 * @param seriesNames     Only show the following series (empty for ALL series)
	 * @return null if not found, else a HTML 'img' tag
	 */
	public String getGraphDataHistoryAsHtmlImage(String graphName, int timeLimit, String... seriesNames)
	{
		String cmName    = this.getName();
		
		if (ChartDataHistoryManager.hasInstance(cmName, graphName))
		{
			// Get the Data Series
			ChartDataHistorySeries cdhs = ChartDataHistoryManager.getInstance(cmName, graphName, getGraphDataHistoryTimeInterval(graphName));

			// Get the TGDP so we can get the "Graph Label"
			TrendGraphDataPoint tgdp = getTrendGraphData(graphName);

			if (tgdp == null)
				throw new RuntimeException("NO_TREND_GRAPH_DATA_POINT: For cm='" + cmName + "', graphName='" + graphName + "', could not find any TrendGraphDataPoint for graphName. If this happens, something is seriously wrong. ");

			// CREATE the 'img' tag
			return ChartDataHistoryCreator.getChartAsHtmlImage(tgdp.getGraphLabel(), cdhs, seriesNames);
		}
		else
		{
			// graph not found
			_logger.info("getGraphDataHistoryAsHtmlImage(): NO_CHART_DATA_HISTORY_SERIES: For cm='" + cmName + "', graphName='" + graphName + "', could not find any ChartDataHistorySeries for graphName '" + graphName + "'.");
			return null;
		}
	}

	/** Round a double value using # scale */
	public double round(double val, int scale)
	{
		return new BigDecimal(val).setScale(scale, RoundingMode.HALF_UP).doubleValue();
	}
	/** Round a double value using 1 as scale */
	public double round1(double val)
	{
		return round(val, 1);
	}
	/** Round a double value using 2 as scale */
	public double round2(double val)
	{
		return round(val, 2);
	}
	/** Round a double value using 3 as scale */
	public double round3(double val)
	{
		return round(val, 3);
	}
	/** Round a double value using 4 as scale */
	public double round4(double val)
	{
		return round(val, 4);
	}
	/** Round a double value using 5 as scale */
	public double round5(double val)
	{
		return round(val, 5);
	}
	/** Round a double value using 6 as scale */
	public double round6(double val)
	{
		return round(val, 6);
	}

	
	/**
	 * Check if we have the DATA MODEL for any graph (this is true for nogui) 
	 * @return
	 */
	public boolean hasTrendGraphData()
	{
		return _trendGraphsData.size() > 0;
	}
	
	public TrendGraphDataPoint getTrendGraphData(String name)
	{
		return _trendGraphsData.get(name);
	}
	
	public Map<String,TrendGraphDataPoint> getTrendGraphData()
	{
		return _trendGraphsData;
	}

	public int getTrendGraphCount()
	{
		if (getTrendGraphData() == null)
			return 0;
		
		return getTrendGraphData().size();
	}

	public int getTrendGraphCountWithData()
	{
		if (getTrendGraphData() == null)
			return 0;
		
		int cnt = 0;
		for (TrendGraphDataPoint tgdp : getTrendGraphData().values())
		{
			if (tgdp.hasData())
				cnt ++;
		}
		return cnt;
	}
	
	/**
	 * FIXME: describe me 
	 * 
	 * @param name Name of the data set
	 * @param tgdp 
	 */
	private void addTrendGraphData(String name, TrendGraphDataPoint tgdp)
	{
		if (_trendGraphsData.containsKey(name))
			throw new RuntimeException("Sorry the trend graph named '"+name+"' is already used, the name must be unique.");

		_trendGraphsData.put(name, tgdp);
	}
	
//	public void addTrendGraphData(String name)
//	{
//		TrendGraphDataPoint tgdp = new TrendGraphDataPoint(name);
//		addTrendGraphData(name, tgdp);
//	}

	public String[] getTrendGraphNames()
	{
		String[] names = new String[_trendGraphsData.size()];

		if (_trendGraphsData.size() == 0)
			return names;

		int i = 0;
		for (Iterator<String> it = _trendGraphsData.keySet().iterator(); it.hasNext();)
		{
			String graphName = it.next();
			names[i] = graphName;
			i++;
		}
		return names;
	}
	
	//-------------------------------------------
	// END: TrendGraphDataPoint
	//-------------------------------------------

	/**
	 * Helper method to create a PK String/Object, which is used in the HashMap to identify unique rows.<br>
	 * 
	 * @param pk
	 * @return
	 */
	public static String createPkStr(String... pk)
	{
		return CounterSample.createPkStr(pk);
	}

	public boolean equalsTabPanel(Component comp)
	{
		if (comp == null)
			return false;
		return comp.equals(_tabPanel);
	}

	/** 
	 * create the default SybMessageHandler which is used when refreshing the counters<p>
	 * override this if you want to discard you'r own specific messages<p>
	 * NOTE: 
	 */
	protected CmSybMessageHandler createSybMessageHandler()
	{
		return new CmSybMessageHandler(this, true);
	}
	/** Get message handler that is used when refreshing counters */
	public CmSybMessageHandler getSybMessageHandler()
	{
		return _sybMessageHandler;
	}
	/** Set message handler that is used when refreshing counters */
	public void setSybMessageHandler(CmSybMessageHandler msgHandler)
	{
		_sybMessageHandler = msgHandler;
	}
	
	/** */
	public void setActiveServerRolesOrPermissions(List<String> permissionList)
	{
		_activeServerRolesOrPermissions = permissionList;
	}
	/** if not initialized it will return null */
	public List<String> getActiveServerRolesOrPermissions()
	{
		return _activeServerRolesOrPermissions;
	}

	/** check if the <b>locally cached</b> List of role names contains the role */
	public boolean isServerRoleOrPermissionActive(String role)
	{
		if ( ! isRuntimeInitialized() ) throw new RuntimeException("This can't be called before the CM has been connected to any monitored server.");
		if (_activeServerRolesOrPermissions == null) 
			return false;

		return _activeServerRolesOrPermissions.contains(role);
	}
	
	/** */
	public void setMonitorConfigs(Map<String,Integer> monitorConfigs)
	{
		_monitorConfigsMap = monitorConfigs;
	}
	/** */
	public Map<String,Integer> getMonitorConfigMap()
	{
		return _monitorConfigsMap;
	}

	/** Get the "in-memory" configuration for: sp_configure 'Monitoring'
	 * @param The name of the config
	 * @return -1 if the configName can't be found, else it's the "Run Value" from sp_configure.
	 */
	public int getMonitorConfig(String configName)
	{
		if ( ! isRuntimeInitialized() ) throw new RuntimeException("This can't be called before the CM has been connected to any monitored server.");
		if (_monitorConfigsMap == null)
			return -1;

		Integer config = _monitorConfigsMap.get(configName); 
		if (config != null)
			return config.intValue();
		return -1;
	}
	
	/** */
	public void setServerVersion(long serverVersion)
	{
		_serverVersion = serverVersion;
	}
	/** */
	public long getServerVersion()
	{
		if ( ! isRuntimeInitialized() ) throw new RuntimeException("This can't be called before the CM has been connected to any monitored server.");
		return _serverVersion;
	}

	/** */
	public void setClusterEnabled(boolean isClusterEnabled)
	{
		_isClusterEnabled = isClusterEnabled;
	}
	/** */
	public boolean isClusterEnabled()
	{
		if ( ! isRuntimeInitialized() ) throw new RuntimeException("This can't be called before the CM has been connected to any monitored server.");
		return _isClusterEnabled;
	}

	/** 
	 * Convert a int version to a string version 
	 * <p>
	 * <code>15030 will be "15.0.3"</code>
	 * <code>15031 will be "15.0.3 ESD#1"</code>
	 */
	public String getServerVersionStr()
	{
		if ( ! isRuntimeInitialized() ) throw new RuntimeException("This can't be called before the CM has been connected to any monitored server.");
		return Ver.versionNumToStr( getServerVersion() );
	}

	/** In here we could call getServerVersion() and decide what SQL syntax we should 
	 * use and what monitor tables and coulmns we should query.
	 * <p>
	 * To set the new version dependent SQL statement, use setSql() method. 
	 * <p>
	 * If getSql() shows NULL or empty SQL statement, this method will be called
	 * to compose a new SQL statement. 
	 */
	public void initSql(DbxConnection conn)
	{
		// Get what version we are connected to.
		DbmsVersionInfo versionInfo = conn.getDbmsVersionInfo();

		// Get what configuration we depends on
		String[] dependsOnConfig = getDependsOnConfigForVersion(conn, versionInfo);
		setDependsOnConfig(dependsOnConfig);
		
		// Generate the SQL, for the specific ASE version
		String sql = getSqlForVersion(conn, versionInfo);

		// Replace any "${cmCollectorName}" --> "xxxTune:CmName"
		sql = replaceCollectorName(sql);
		
		// set the SQL to use
		if (useSqlPrefix())
		{
			String sqlPrefix = getSqlPrefix(); 
			setSql(sqlPrefix + sql);
		}
		else
		{
			setSql(sql);
		}

		// Generate the SQL INIT, for the specific ASE version
		String sqlInit = getSqlInitForVersion(conn, versionInfo);
		setSqlInit(sqlInit);

		// Generate the SQL CLOSE, for the specific ASE version
		String sqlClose = getSqlCloseForVersion(conn, versionInfo);
		setSqlClose(sqlClose);

		// Generate PrimaryKey, for the specific ASE version
		List<String> pkList = getPkForVersion(conn, versionInfo);
//		if (pkList != null && pkList.isEmpty()) // NOTE: not sure if we can do this here: since "empty" list I think does "diffCalc - but on CM's with a single row"
//			pkList = null;
		setPk(pkList);
		
		// Set specific column descriptions
		addMonTableDictForVersion(conn, versionInfo);
	}

	/** 
	 * If you want to use SQL Prefix in a query, this can be used to automatically create a prefix string
	 * <p>
	 * The default is to use: <code>&#47* APPNAME:CmName *&#47; \n</code> 
	 */
	public String getSqlPrefix()
	{
		return "/* " + Version.getAppName() + ":" + getName() + " */ \n";
	}

	/** 
	 * If you want to use SQL Prefix in a query, this can be used to automatically create a prefix string
	 * <p>
	 * The default is to use: <code>&#47* APPNAME:CmName *&#47; \n</code> 
	 */
	public String replaceCollectorName(String sql)
	{
		if (sql == null)
			return null;
	
		Configuration conf = Configuration.getCombinedConfiguration();

		boolean replaceCollectorName = conf.getBooleanProperty("cm.replaceCollectorName", true);
		if (replaceCollectorName)
		{
			if (conf.hasProperty("cm." + getName() + ".replaceCollectorName"))
			{
				replaceCollectorName = conf.getBooleanProperty("cm." + getName() + ".replaceCollectorName", true);
			}
		}
		if (replaceCollectorName)
			return sql.replace("${cmCollectorName}", Version.getAppName() + ":" + getName());

		return sql;
	}

	/**
	 * Checks if we should add any SQL Prefix<br>
	 * First check property: <code>cm.sqlPrefix.use</code><br>
	 * Second check property: <code>cm.<i>cmName</i>.sqlPrefix.use</code><br>
	 * 
	 * @return true if we should call method: getSqlPrefix
	 */
	public boolean useSqlPrefix()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		
		boolean useSqlPrefix = conf.getBooleanProperty("cm.sqlPrefix.use", true);
		if (useSqlPrefix)
		{
			if (conf.hasProperty("cm." + getName() + ".sqlPrefix.use"))
			{
				useSqlPrefix = conf.getBooleanProperty("cm." + getName() + ".sqlPrefix.use", true);
			}
		}
		return useSqlPrefix;
	}

	/**
	 * Get the SQL Statement for getting the "current time" on the server.<br>
	 * If null is returned, the java System.currentTime() will be used.
	 * @return
	 */
	public String getServerTimeCmd()
	{
		return getCounterController().getServerTimeCmd();
	}

	/**
	 * Does the Server support many SQL Statements in the same SQL Batch
	 * @return 
	 */
	public boolean isSqlBatchingSupported()
	{
		return getCounterController().isSqlBatchingSupported();
	}

	
	/**
	 * If we want to create a new SQL Statement at every refresh, then return true
	 * <p>
	 * Override this in any CM where we need this
	 * 
	 * @return true = call <code>getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)</code> before every refresh
	 */
	public boolean createNewSqlForEveryRefresh()
	{
		return false;
	}

	/**
	 * This one could or <br>should</b> be called to get a SQL string that will be used
	 * to get data for a specific Performance Counter.
	 * <p>
	 * It could also be used check what SQL that will be generated for a specific ASE version.
	 * <p>
	 * This method would typically be called from initSql(), which will set the correct SQL 
	 * (by calling setSql()) for the desired ASE version.
	 */
	// FIXME: maybe declare this method and class as abstract, instead of throwing the exception.
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		throw new UnsupportedOperationException("The method CountersModel.getSqlForVersion(Connection conn, long srvVersion, boolean isClusterEnabled) has NOT been overridden, which should be done. CM Name='"+getName()+"'.");
	}

	public String getSqlInitForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return null;
	}

	public String getSqlCloseForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return null;
	}

	/**
	 * Create any PrimaryKey that is used when we need to do difference calculations.
	 * <br>
	 * If null or empty list is returned = No difference Calculations will/can be done.
	 * 
	 * @param conn
	 * @param srvVersion
	 * @param isClusterEnabled
	 * @return A List of Column names
	 */
	// FIXME: maybe declare this method and class as abstract, instead of throwing the exception.
//	public abstract List<String> getPkForVersion(Connection conn, long srvVersion, boolean isClusterEnabled);
//	public List<String> getPkForVersion(DbxConnection conn, long srvVersion, boolean isClusterEnabled)
//	{
//		throw new UnsupportedOperationException("The method CountersModel.getPkForVersion(long srvVersion, boolean isClusterEnabled) has NOT been overridden, which should be done. CM Name='"+getName()+"'.");
//	}
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		throw new UnsupportedOperationException("The method CountersModel.getPkForVersion(long srvVersion, boolean isClusterEnabled) has NOT been overridden, which should be done. CM Name='"+getName()+"'.");
	}

//	public String[] getDependsOnConfigForVersion(DbxConnection conn, long srvVersion, boolean isClusterEnabled)
//	{
//		return null;
//	}
	public String[] getDependsOnConfigForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return null;
	}

//	public void addMonTableDictForVersion(DbxConnection conn, long srvVersion, boolean isClusterEnabled)
//	{
//	}
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
	}

	/** 
	 * This method is used to check if the CounterTable should be initialized or not.<br>
	 * for example "deadlock pipe" may not be active...<br>
	 * If server version is below 15.0.2 'statement cache' info should not be 
	 * polled since those monTables doesn't exist. And the GUI Table for this should
	 * not be VISABLE... call setActive(false) in those cases.
	 */
	public void init(DbxConnection conn)
	throws Exception // I think it's OK to do this here
	{
		if (checkDependsOnVersion(conn))
		{
			if (checkDependsOnConfig(conn))
			{
				if (checkDependsOnRole(conn))
				{
					if (checkDependsOnStoredProc(conn))
					{
						checkDependsOnOther(conn);

						// init stuff on the GUI part, if we have one...
						if (getTabPanel() != null)
							getTabPanel().onCmInit();
					}
				}
			}
		}
	}
	
//	/**
//	 * Checks if we meet all the requirements for this CM
//	 * <p>
//	 * This typically calls
//	 * <ul>
//	 *   <li>checkDependsOnVersion()</li>
//	 *   <li>checkDependsOnConfig()</li>
//	 *   <li>checkDependsOnRole()</li>
//	 *   <li>checkDependsOnStoredProc()</li>
//	 *   <li>checkDependsOnOther()</li>
//	 *   <li>etc</li>
//	 * </ul>
//	 * 
//	 * Override this if you have special needs...
//	 * 
//	 * @param conn
//	 * @return null or empty string if we should proceed, otherwise a message why this CM doesn't meet the requirements
//	 */
//	public String checkRequirements(DbxConnection conn)
//	{
//		FIXME: call this from init() and do good things
//	}

	public boolean isInitialized()
	{
		return _isInitialized;
	}
	public void setInitialized(boolean init)
	{
		_isInitialized = init;
	}

	/**
	 * This will be true AFTER late initialization (after the monitored server is connected)
	 * @return
	 */
	public boolean isRuntimeInitialized()
	{
		return _runtimeInitialized;
	}
	/**
	 * Set this after the monitored server is connected, and late time initialization has been done.
	 * @return
	 */
	public void setRuntimeInitialized(boolean init)
	{
		_runtimeInitialized = init;
	}

	/** If this method returns FALSE, this CounterModel is disabled.<br>
	 *  This due to that during initialization, it found out that it should 
	 *  not be used for some reason.
	 */
	public boolean isActive()
	{
		return _isActive;
	}
	public void setActive(boolean enable, String problemDescription)
	{
		_isActive    = enable;
		_problemDesc = problemDescription;
		if (_problemDesc == null)
			_problemDesc = "";
		
		if (_tabPanel != null)
		{
			_tabPanel.setEnabled(enable);
		}

		// Reset some counters when we enable
		if (enable)
		{
			// How many times have we got a timeoutException. This is set to 0 each time we succeed  
			_sequentialTimeoutExceptionCount = 0;

			// At what time was the oldest timeoutException. This is set to -1 each time we succeed  
			_oldestTimeoutExceptionTime = -1;
		}
	}
	/** */
	public String getProblemDesc()
	{
		return _problemDesc;
	}

	/** */
	public void setState(State state)
	{
		_state = state;
	}

	/** */
	public State getState()
	{
		return _state;
	}

	/** */
	public String getStateDescription()
	{
		if (_state == State.SRV_IN_SHUTDOWN) 
			return "The DBMS Server is waiting for a SHUTDOWN, data collection is put on hold...";

		return "";
	}

	/** */
	public String getSql()
	{
		return _sqlRequest;
	}
	/** */
	public void setSql(String sql)
	{
		_sqlRequest = sql;
	}

	/** */
	public String getSqlWhere()
	{
		return _sqlWhere;
	}
	/** */
	public void setSqlWhere(String extraWhere)
	{
		_sqlWhere = " " + extraWhere;
	}

	/** */
	public String getSqlInit()
	{
		return _sqlInit;
	}
	public void setSqlInit(String sql)
	{
		_sqlInit = sql;
	}
	public boolean doSqlInit()
	{
		return doSqlInit(getCounterController().getMonConnection());
	}
	public boolean doSqlInit(DbxConnection conn)
	{
		if (conn == null)
			throw new IllegalArgumentException("The passed conn is null.");

		String sql = getSqlInit();
		if (StringUtil.hasValue(sql))
		{
			int batchCounter = 0;
			try
			{
				BufferedReader br = new BufferedReader( new StringReader(sql) );
				for(String sqlBatch=AseSqlScript.readCommand(br); sqlBatch!=null; sqlBatch=AseSqlScript.readCommand(br))
				{
					sql = sqlBatch;
					
					if (_logger.isDebugEnabled())
					{
						_logger.debug("##### BEGIN doSqlInit(send sql), batchCounter="+batchCounter+" ############################### "+ getName());
						_logger.debug(sql);
						_logger.debug("##### END   doSqlInit(send sql), batchCounter="+batchCounter+" ############################### "+ getName());
						_logger.debug("");
					}
					_logger.info(getName()+": doSqlInit() sending statement: "+sql.trim());

					try
					{
						Statement stmt = conn.createStatement();
						stmt.execute(sql);
						stmt.close();
					}
					catch (SQLException e)
					{
						_logger.warn("Problem when executing the 'init' SQL statement: ErrorCode="+e.getErrorCode()+", SQL="+sql, e);
						//return false;
					}
					batchCounter++;
				}
				br.close();
			}
			catch (IOException ex) 
			{
				_logger.error("While reading the input SQL 'go' String, caught: "+ex, ex);
			}
		}
		return true;
	}
	
	/** */
	public String getSqlClose()
	{
		return _sqlClose;
	}
	public void setSqlClose(String sql)
	{
		_sqlClose = sql;
	}
	public void doSqlClose()
	{
		doSqlClose(getCounterController().getMonConnection());
	}
	public void doSqlClose(DbxConnection conn)
	{
		if (conn == null)
			throw new IllegalArgumentException("The passed conn is null.");

		String sql = getSqlClose();
		if (StringUtil.hasValue(sql))
		{
			// SET the message handler used for this CM
			// this is needed in the doSqlClose(), but not in the doSqlInit() where the maeesage handler has already been installed...
			SybMessageHandler curMsgHandler = null;
			if (conn instanceof SybConnection)
			{
				curMsgHandler = ((SybConnection)conn).getSybMessageHandler();
				((SybConnection)conn).setSybMessageHandler(getSybMessageHandler());
			}
			// Set a TDS Message Handler
			if (conn instanceof TdsConnection)
				((TdsConnection)conn).setSybMessageHandler(getSybMessageHandler());
			
			int batchCounter = 0;
			try
			{
				BufferedReader br = new BufferedReader( new StringReader(sql) );
				for(String sqlBatch=AseSqlScript.readCommand(br); sqlBatch!=null; sqlBatch=AseSqlScript.readCommand(br))
				{
					sql = sqlBatch;
					
					if (_logger.isDebugEnabled())
					{
						_logger.debug("##### BEGIN doSqlClose(send sql), batchCounter="+batchCounter+" ############################### "+ getName());
						_logger.debug(sql);
						_logger.debug("##### END   doSqlClose(send sql), batchCounter="+batchCounter+" ############################### "+ getName());
						_logger.debug("");
					}
					_logger.info(getName()+": doSqlClose() sending statement: "+sql.trim());

					try
					{
						Statement stmt = conn.createStatement();
						stmt.execute(sql);
						stmt.close();
					}
					catch (SQLException e)
					{
						_logger.warn("Problem when executing the 'close' SQL statement: ErrorCode="+e.getErrorCode()+", SQL="+sql, e);
						//return;// false;
					}
					batchCounter++;
				}
				br.close();
			}
			catch (IOException ex) 
			{
				_logger.error("While reading the input SQL 'go' String, caught: "+ex, ex);
			}
			finally
			{
				// restore old message handler
				if (curMsgHandler != null)
				{
					((SybConnection)conn).setSybMessageHandler(curMsgHandler);
				}
				// Restore old message handler
				if (conn instanceof TdsConnection)
					((TdsConnection)conn).restoreSybMessageHandler();
			}
		}
	}

	/**
	 * Closing the CounterModel
	 */
	public void close()
	{
		if (isInitialized())
		{
			if (isConnected())
				doSqlClose();
		}
	}


	public String[] getMonTablesInQuery()
	{
		return _monTablesInQuery;
	}

	public String[] getDependsOnRole()
	{
		return _dependsOnRole;
	}
	public void setDependsOnRole(String[] dependsOnRole)
	{
		_dependsOnRole = dependsOnRole;
		if (_dependsOnRole == null)
		{
			String[] emptyArray = {};
			_dependsOnRole = emptyArray;
		}

	}
	/**  */
	public boolean checkDependsOnRole(DbxConnection conn)
	{
		String[] dependsOnRole = getDependsOnRole();

		if (dependsOnRole == null)
			return true;

		boolean rc = true;
		String didNotHaveRoles = "";
		for (int i=0; i<dependsOnRole.length; i++)
		{
			if (dependsOnRole[i] == null || (dependsOnRole[i] != null && dependsOnRole[i].trim().equals("")) )
				continue;

			String roleName = dependsOnRole[i].trim();
//			boolean b = AseConnectionUtils.hasRole(conn, dependsOnRole[i].trim());
			if ( ! isServerRoleOrPermissionActive(roleName) )
			{
				didNotHaveRoles += roleName + ", ";
				rc = false;
			}
		}

		if (rc == false)
		{
			// take away ending comma ', '
			if (didNotHaveRoles.endsWith(", "))
				didNotHaveRoles = didNotHaveRoles.substring(0, didNotHaveRoles.length()-2);

			if (_logger.isDebugEnabled()) 
				_logger.debug(getName() + ": should be HIDDEN.");
			_logger.warn("When trying to initialize Counters Model '"+getName()+"', named '"+getDisplayName()+"'. The following role(s) were needed '"+StringUtil.toCommaStr(dependsOnRole)+"', and you do not have the following role(s) '"+didNotHaveRoles+"'.");

			setActive(false, "This info is only available if you have '"+StringUtil.toCommaStr(dependsOnRole)+"' role(s) enabled.\nYou are missing the following role(s) '"+didNotHaveRoles+"'.");

			TabularCntrPanel tcp = getTabPanel();
			if (tcp != null)
			{
				tcp.setToolTipText("<html>This tab will only be visible if you have '"+StringUtil.toCommaStr(dependsOnRole)+"' role(s) enabled.<br>You are missing the following role(s) '"+didNotHaveRoles+"'.</html>");
			}
			return false;
		}
		return rc;
	}

	
	public String[] getDependsOnConfig()
	{
		return _dependsOnConfig;
	}
	public void setDependsOnConfig(String[] dependsOnConfig)
	{
		if (dependsOnConfig == null)
			dependsOnConfig = new String[] {};

		_dependsOnConfig = dependsOnConfig;
	}

	
	/** Check a specific configuration parameter */
	public boolean checkDependsOnConfig(DbxConnection conn, String configNameVal)
	{
		if (configNameVal == null || (configNameVal != null && configNameVal.equals("")) )
			throw new IllegalArgumentException("checkDependsOnConfig(): configNameVal='"+configNameVal+"' must be a value.");

		String[] configNameArr = configNameVal.split("=");
		String configName    = configNameVal;
		String reConfigValue = null;
		if (configNameArr.length >= 1) configName    = configNameArr[0];
		if (configNameArr.length >= 2) reConfigValue = configNameArr[1];

		int configHasValue = AseConnectionUtils.getAseConfigRunValueNoEx(conn, configName);
		if (_logger.isDebugEnabled()) 
			_logger.debug("Checking for DBMS Configuration '"+configName+"', which has value '"+configHasValue+"'. Option to re-configure to value '"+reConfigValue+"' if not set.");

		// In NO_GUI mode, we might want to auto configure monitoring...
		boolean doReconfigure = false;
		if ( ! DbxTune.hasGui() )
		{
//			Configuration conf = Configuration.getInstance(Configuration.CONF);
//			Configuration conf = Configuration.getCombinedConfiguration();
			Configuration conf = Configuration.getInstance(Configuration.PCS);
			doReconfigure = conf.getBooleanProperty("offline.configuration.fix", false);
		}
		//doReconfigure = true; // if you want to force when testing

		// If no config value has been specified, we can't do initialization...
		// Doing it with a dummy value of 1 or simular would be dangerous, lets use 'statement cache size'
		// as an example... then we would enable statement cache... and to a SMALL value as well = BAD-BAD-BAD
		if (reConfigValue == null)
			doReconfigure = false;

		// Should we do RECONFIGURE
		if (configHasValue <= 0  &&  doReconfigure)
		{
			// CHECK IF WE HAVE "sa role", so we can re-configure
			boolean hasSaRole = AseConnectionUtils.hasRole(conn, AseConnectionUtils.SA_ROLE);
			if ( ! hasSaRole )
			{
				_logger.warn("Can not adjust the configuration '"+configName+"' to value '"+reConfigValue+"'. To do that the connected user needs to have '"+AseConnectionUtils.SA_ROLE+"'.");
			}
			else
			{
				_logger.info("DBMS Configuration '"+configName+"' for Counters Model '"+getName()+"', named '"+getDisplayName()+"', will be reconfigured from value '"+configHasValue+"' to value '"+reConfigValue+"'.");

				try
				{
					AseConnectionUtils.setAseConfigValue(conn, configName, reConfigValue);
				}
				catch (SQLException e)
				{
					_logger.error("Problems setting DBMS configuration '"+configName+"' to '"+reConfigValue+"'. Caught: "+AseConnectionUtils.sqlExceptionToString(e));
				}

				configHasValue = AseConnectionUtils.getAseConfigRunValueNoEx(conn, configName);
				if (_logger.isDebugEnabled()) 
					_logger.debug("After re-config, the DBMS Configuration '"+configName+"', now has value '"+configHasValue+"'.");
			}
		}

		if (configHasValue > 0)
		{
			if (_logger.isDebugEnabled()) 
				_logger.debug(getName() + ": should be VISABLE.");
			return true;
		}
		else
		{
			if (isNonConfiguredMonitoringAllowed())
			{
				_logger.warn("Non Configured Monitoring is allowed: When trying to initialize Counters Model '"+getName()+"', named '"+getDisplayName()+"' in DBMS Version "+getServerVersionStr()+", I found that '"+configName+"' wasn't configured (which is done with: sp_configure '"+configName+"'), so counters in '"+getDisplayName()+"' may not be relyable.");
				return true;
			}

			if (_logger.isDebugEnabled()) 
				_logger.debug(getName() + ": should be HIDDEN.");
			String reconfigOptionStr = " or using the nogui mode: --reconfigure switch";
			if (reConfigValue == null)
				reconfigOptionStr = "";
			_logger.warn("When trying to initialize Counters Model '"+getName()+"', named '"+getDisplayName()+"' in DBMS Version "+getServerVersionStr()+", I found that '"+configName+"' wasn't configured (which is done with: sp_configure '"+configName+"'"+reconfigOptionStr+"), so monitoring information about '"+getDisplayName()+"' will NOT be enabled.");

			String problemDesc = getProblemDesc();
			if (problemDesc == null)
				problemDesc = "";

			problemDesc += "sp_configure '"+configName+"' has NOT been enabled.\n";
			setActive(false, problemDesc);

			TabularCntrPanel tcp = getTabPanel();
			if (tcp != null)
			{
				tcp.setToolTipText("This tab will only be visible if: sp_configure '"+configName+"' has been enabled.");
			}
			return false;
		}
	}

	/** Check all configuration parameters assigned, using getDependsOnConfig to get configurations */
	public boolean checkDependsOnConfig(DbxConnection conn)
	{
		String[] dependsOnConfig = getDependsOnConfig();

		if (dependsOnConfig == null)
			return true;

		boolean rc = true;
		for (int i=0; i<dependsOnConfig.length; i++)
		{
			if (dependsOnConfig[i] == null || (dependsOnConfig[i] != null && dependsOnConfig[i].trim().equals("")) )
				continue;

			boolean b = checkDependsOnConfig(conn, dependsOnConfig[i].trim());
			if ( ! b )
				rc = false;
		}
		return rc;
	}

	/** get a list of all the missing configuration for this CM
	 * @return null if nothing is missing, or a comma separated string with the missing configs
	 */
	public String getMissigConfig(DbxConnection conn)
	{
		String[] dependsOnConfig = getDependsOnConfig();

		if (dependsOnConfig == null)
			return null;

		String missing = "";
		for (int i=0; i<dependsOnConfig.length; i++)
		{
			if (dependsOnConfig[i] == null || (dependsOnConfig[i] != null && dependsOnConfig[i].trim().equals("")) )
				continue;

			String configName = dependsOnConfig[i].trim();

			// Strip away any "reconfig value": "enable monitoring=1" -> "enable monitoring"
			if (configName.indexOf('=') >= 0)
				configName = configName.substring(0, configName.indexOf('=')).trim();

			// Get config from the server
			int configValue = AseConnectionUtils.getAseConfigRunValueNoEx(conn, configName);
			if (configValue == 0)
				missing += configName + ", ";				
		}
		missing = StringUtil.removeLastComma(missing);
		
		// If we still are missing the parameters: search the: getNonConfiguedMonitoringMessageList()
		// THIS will be cheaper if we do search the message list FIRST instead of getting ASE Config... 
		// but that wont work if the language isn't ENGLISH.... so lets change that later... 
		if (StringUtil.isNullOrBlank(missing))
		{
			// try to parse the messages received and get what's missing.
			Set<String> paramSet = new LinkedHashSet<>();
			List<String> cfgMsgList = getNonConfiguedMonitoringMessageList();
			if (cfgMsgList != null)
			{
				for (String msg : cfgMsgList)
				{
					// '12036: Collection of monitoring data for table 'monProcessStatement' requires that the 'per object statistics active' configuration option(s) be enabled. To set the necessary configuration, contact a user who has the System Administrator (SA) role.'
					String searchFor = "' requires that the '";
					int searchPos = msg.indexOf(searchFor);
					if (searchPos >= 0)
					{
						int endPos = msg.indexOf("'", searchPos + searchFor.length());
						if (endPos >= 0)
						{
							String cfgParam = msg.substring(searchPos + searchFor.length(), endPos);
							paramSet.add(cfgParam);
						}
					}
					
				}
			}
			// Add the Set to the "missing" string...
			if (paramSet.size() > 0)
			{
				for (String param : paramSet)
					missing += param + ", ";				

				missing = StringUtil.removeLastComma(missing);
			}
		} // end: search messages

		if (StringUtil.isNullOrBlank(missing))
			return null;

		return missing;
	}

	/** 
	 * Convert a int version to a string version 
	 * <p>
	 * <code>15030 will be "15.0.3"</code>
	 * <code>15031 will be "15.0.3 ESD#1"</code>
	 */
	public String getDependsOnVersionStr()
	{
		return Ver.versionNumToStr( getDependsOnVersion() );
	}
	public long getDependsOnVersion()
	{
		return _dependsOnVersion;
	}
	public void setDependsOnVersion(long version)
	{
		_dependsOnVersion = version;
	}

	public String getDependsOnCeVersionStr()
	{
		return Ver.versionNumToStr( getDependsOnCeVersion() );
	}
	public long getDependsOnCeVersion()
	{
		return _dependsOnCeVersion;
	}
	public void setDependsOnCeVersion(long version)
	{
		_dependsOnCeVersion = version;
	}

	public boolean checkDependsOnVersion(DbxConnection conn)
	{
		if (_dependsOnVersion == 0)
			return true;

		long needsVersion   = getDependsOnVersion();
		long srvVersion     = getServerVersion();

		if ( isClusterEnabled() && getDependsOnCeVersion() > 0 )
		{
			return checkDependsOnCeVersion();
		}

		if (srvVersion >= needsVersion)
		{
			return true;
		}
		else
		{
			if (_logger.isDebugEnabled()) 
				_logger.debug(getName() + ": should be HIDDEN.");
			_logger.warn("When trying to initialize Counters Model '" + getName() + "', named '" + getDisplayName() + "' in DBMS Version " + getServerVersionStr() + ", I need at least DBMS Version " + getDependsOnVersionStr() + " for that.");

			setActive(false, "This info is only available if DBMS Version is above " + getDependsOnVersionStr());

			TabularCntrPanel tcp = getTabPanel();
			if (tcp != null)
			{
				tcp.setToolTipText("This tab will only be visible if DBMS Version is over " + getDependsOnVersionStr());
			}
			return false;
		}
	}
	public boolean checkDependsOnCeVersion()
	{
		if (_dependsOnCeVersion == 0)
			return true;

		long needsCeVersion = getDependsOnCeVersion();
		long srvVersion     = getServerVersion();

		if (srvVersion >= needsCeVersion)
		{
			return true;
		}
		else
		{
			if (_logger.isDebugEnabled())
				_logger.debug(getName() + ": should be HIDDEN.");
			_logger.warn("When trying to initialize Counters Model '" + getName() + ")', named '" + getDisplayName() + "' in DBMS Cluster Edition Version " + getServerVersionStr()+", I need at least DBMS Cluster Edition Version "+getDependsOnCeVersionStr() + " for that.");

			setActive(false, "This info is only available if DBMS Cluster Edition Server Version is above " + getDependsOnCeVersionStr());

			TabularCntrPanel tcp = getTabPanel();
			if (tcp != null)
			{
				tcp.setToolTipText("This tab will only be visible if DBMS Cluster Edition Server Version is over " + getDependsOnCeVersionStr());
			}
			return false;
		}
	}

	/**
	 * Add dependency for a stored proc, if it's not there it will be created
	 * 
	 * @param dbname              Name of the database the proc should be in (mandatory)
	 * @param procName            Name of the procedure (mandatory)
	 * @param procDateThreshold   Recreate if the procedure is created "earlier" than this date (mandatory)
	 * @param scriptLocation      In what "directory" (actually a classname) do we find the script (mandatory)
	 * @param scriptName          Name of the script (from within the jar file or classpath) (mandatory)
	 * @param needsRoleToRecreate What ROLE inside ASE server do we need to create this procedure (can be null, no roles would be checked)
	 */
	public void addDependsOnStoredProc(String dbname, String procName, Date procDateThreshold, Class<?> scriptLocation, String scriptName, String needsRoleToRecreate, long needSrvVersion)
	{
		if (dbname            == null) throw new IllegalArgumentException("addDependsOnStoredProc(): 'dbname' cant be null");
		if (procName          == null) throw new IllegalArgumentException("addDependsOnStoredProc(): 'procName' cant be null");
		if (procDateThreshold == null) throw new IllegalArgumentException("addDependsOnStoredProc(): 'procDateThreshold' cant be null");
		if (scriptLocation    == null) throw new IllegalArgumentException("addDependsOnStoredProc(): 'scriptLocation' cant be null");
		if (scriptName        == null) throw new IllegalArgumentException("addDependsOnStoredProc(): 'scriptName' cant be null");

		if (_dependsOnStoredProc == null)
			_dependsOnStoredProc = new LinkedList<StoredProcCheck>();

		StoredProcCheck spc = new StoredProcCheck(dbname, procName, procDateThreshold, scriptLocation, scriptName, needsRoleToRecreate, needSrvVersion);
		_dependsOnStoredProc.add(spc);
	}
	
	public boolean checkDependsOnStoredProc(DbxConnection conn, String dbname, String procName, Date procDateThreshold, Class<?> scriptLocation, String scriptName, String needsRoleToRecreate, long needSrvVersion)
	{
		long srvVersion = AseConnectionUtils.getAseVersionNumber(conn);
		if (srvVersion < needSrvVersion)
		{
			_logger.warn("When trying to checking stored procedure '"+procName+"' in '"+dbname+"' the Current DBMS Version is to low '"+Ver.versionNumToStr(srvVersion)+"', this procedure needs DBMS Version '"+Ver.versionNumToStr(needSrvVersion)+"' to install.");
			return false;
		}

		// If procName does not exists
		// or is of an earlier version than procDateThreshold
		// GO AND CREATE IT.
		Date crDate = AseConnectionUtils.getObjectCreationDate(conn, dbname, procName);
		if (crDate == null || ( crDate != null && crDate.getTime() < procDateThreshold.getTime()) )
		{
			if (crDate == null)
				_logger.info("Checking for stored procedure '"+procName+"' in '"+dbname+"', which was NOT found.");
			else
				_logger.info("Checking for stored procedure '"+procName+"' in '"+dbname+"', which was to old, crdate was '"+crDate+"', re-creation threshold date is '"+procDateThreshold+"'.");

			boolean hasProc = false;

			// CHECK IF WE HAVE "some ROLE", so we can create the proc
			boolean hasRole = true;
			if (needsRoleToRecreate != null && !needsRoleToRecreate.equals(""))
			{
				hasRole = AseConnectionUtils.hasRole(conn, needsRoleToRecreate);
			}

			if ( ! hasRole )
			{
				_logger.warn("Can not (re)create procedure '"+procName+"' in '"+dbname+"', for doing that the connected user needs to have '"+needsRoleToRecreate+"'.");
			}
			else
			{
				AseSqlScript script = null;
				try
				{
					script = new AseSqlScript(conn, 30); // 30 sec timeout
					_logger.info("Creating procedure '"+procName+"' in '"+dbname+"'.");
					script.setMsgPrefix(scriptName+": ");
					script.execute(scriptLocation, scriptName);
					hasProc = true;
				}
				catch (SQLException e) 
				{
					_logger.error("Problem loading the script '"+scriptName+"'.", e);
				}
				finally
				{
					if (script != null)
						script.close();
				}
			}

			if ( ! hasProc )
			{
				String msg = "Missing stored proc '"+procName+"' in database '"+dbname+"' please create it.";
				setActive(false, msg);

				if (_logger.isDebugEnabled()) 
					_logger.debug(getName() + ": should be HIDDEN.");
//				_logger.warn("When trying to initialize Counters Model '"+getName()+"', named '"+getDisplayName()+"' in DBMS Version "+getServerVersion()+", "+msg+" (connect with a user that has '"+needsRoleToRecreate+"' or load the proc from '$"+DbxTune.getInstance().getAppHomeEnvName()+"/classes' or unzip dbxtune.jar. under the class '"+scriptLocation.getClass().getName()+"' you will find the script '"+scriptName+"').");
				_logger.warn("When trying to initialize Counters Model '"+getName()+"', named '"+getDisplayName()+"' in DBMS Version "+getServerVersion()+", "+msg+" (connect with a user that has '"+needsRoleToRecreate+"' or load the proc from '$DBXTUNE_HOME/classes' or unzip dbxtune.jar. under the class '"+scriptLocation.getClass().getName()+"' you will find the script '"+scriptName+"').");

				TabularCntrPanel tcp = getTabPanel();
				if (tcp != null)
				{
					tcp.setToolTipText("This tab will only be visible if the stored procedure '"+procName+"' exists in '"+dbname+"'.");
				}
			}
		}
		else
		{
			_logger.info("No Need to re-create procedure '"+procName+"' in '"+dbname+"', creation date was '"+crDate+"', re-creation threshold date is '"+procDateThreshold+"'.");
		}
		return true;
	}
	public boolean checkDependsOnStoredProc(DbxConnection conn)
	{
		if (_dependsOnStoredProc == null)
			return true;

		boolean rc = true;
		for (StoredProcCheck spc : _dependsOnStoredProc)
		{
			boolean b = checkDependsOnStoredProc(conn, spc._dbname, spc._procName, spc._procDateThreshold, spc._scriptLocation, spc._scriptName, spc._needsRoleToRecreate, spc._needSrvVersion);
			if ( ! b )
				rc = false;
		}
		return rc;
	}

	
	
	/** 
	 * Check other stuff this CM might need to start
	 * <p>
	 * Override this to get specific or UserDefined Checkings
	 */
	public boolean checkDependsOnOther(DbxConnection conn)
	{
		return true;
	}



	protected void setSystemCm(boolean b)
	{
		_systemCm = b;
	}
	public boolean isSystemCm()
	{
		return _systemCm;
	}
	
	protected void setName(String name)
	{
		_name = name;
	}
	/** 
	 * get the short name of this CM 
	 * 
	 * @return The short name
	 * @see #getDisplayName()
	 */
	public String getName()
	{
		return _name;
	}

	/**
	 * What group does this CM belong to
	 * @param name
	 */
	protected void setGroupName(String groupName)
	{
		_groupName = groupName;
	}
	/**
	 * @return What group does this CM belong to
	 */
	public String getGroupName()
	{
		return _groupName;
	}

	/**
	 * Get the preferred column order and properties<br>
	 * 
	 * @return Column names <b>not</b> in the Map should be put at the end
	 */
	public LinkedHashMap<String, ColumnHeaderPropsEntry> getPreferredColumnProps()
	{
		return _preferredColumnProps;
	}

	/**
	 * Set the preferred column properties<br>
	 * Column names <b>not</b> in the Map should be put at the end
	 * @param columns
	 */
	public void setPreferredColumnProps(LinkedHashMap<String, ColumnHeaderPropsEntry> columns)
	{
		_preferredColumnProps = columns;
	}

	/**
	 * Add a column properties<br>
	 * @param colname
	 */
	public void addPreferredColumnOrder(ColumnHeaderPropsEntry colProps)
	{
		LinkedHashMap<String, ColumnHeaderPropsEntry> columns = getPreferredColumnProps();
		if (columns != null && colProps != null)
		{
			columns.put(colProps.getColumnName(), colProps);
		}
	}

//	/**
//	 * Get the preferred column order<br>
//	 * Meaning columns that should be first in the output table
//	 * 
//	 * @return a Set that contains the preferred columns order. Column names <b>not</b> in the set should be put at the end
//	 */
//	public Set<String> getPreferredColumnOrder()
//	{
//		return _preferredColumnOrder;
//	}
//
//	/**
//	 * Add a column to the preferred order<br>
//	 * Column names <b>not</b> in the set should be put at the end
//	 * 
//	 * @param colname
//	 */
//	public void addPreferredColumnOrder(String colname)
//	{
//		Set<String> columns = getPreferredColumnOrder();
//		if (columns != null && colname != null)
//			columns.add(colname);
//	}
//
//	/**
//	 * Set the preferred column order<br>
//	 * Column names <b>not</b> in the set should be put at the end
//	 * @param columns
//	 */
//	public void setPreferredColumnOrder(Set<String> columns)
//	{
//		_preferredColumnOrder = columns;
//	}

	/**
	 * Called by the CounterSample when reading the ResultSet if any of the PK column is NULL
	 * <p>
	 * In this method, we can resolve any potential NULL values and replace them with "something else"
	 * <p>
	 * NOTE: Override this method to do something in any CM, the default implementation does nothing, just returns a NULL value...
	 * 
	 * @param counterSample
	 * @param rsRowNumber
	 * @param rsColumnNumber
	 * @param colName
	 * @return
	 */
	public Object pkNullValueResolver(CounterSample counterSample, int rsRowNumber, int rsColumnNumber, String colName)
	{
		return null;
	}


	/**
	 * do local calculation, this should be overridden for local calculations...
	 * <p>
	 * This only allow changing Absolute values, and it's called before the localCalculation(prevSample, newSample, diffData)
	 * 
	 * @param newSample the new values
	 */
	public void localCalculation(CounterSample newSample)
	{
	}


	/**
	 * do local calculation, this should be overridden for local calculations...
	 * <p>
	 * This only allow changing Absolute values, and it's called before the localCalculation(prevSample, newSample, diffData)<br>
	 * It's NOT called the first time... It's only called if prevSample != null
	 * <p>
	 * So use this if you need to change some values in the Absolute values, but you need to check something in <b>previous</b> sample.
	 * 
	 * @param prevSample the previous sample (note: if this is null, this method wont be called)
	 * @param newSample the new values
	 */
	public void localCalculation(CounterSample prevSample, CounterSample newSample)
	{
	}


	/**
	 * do local calculation, this should be overridden for local calculations...
	 * 
	 * @param prevSample
	 * @param newSample
	 * @param diffData
	 */
	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
	{
	}


	/**
	 * Do local adjustments on the rateData
	 * 
	 * @param tmpRateData
	 */
	public void localCalculationRatePerSec(CounterSample rateData, CounterSample diffData)
	{
	}


	/** 
	 * In here we can choose the discard/change values from ResultSet, when doing SQL Refresh <br>
	 * overridden to change the "resultset"
	 * 
	 * @param cnt the CounterSample object, if you want to access meta data
	 * @param thisRow a List<Object> with the currect row we just pulled of from the ResultSet
	 * @param prevRow a List<Object> with the last row that was added to the CounterSample
	 * 
	 * @return true to add row, false to discard row
	 */
	public boolean hookInSqlRefreshBeforeAddRow(CounterSample cnt, List<Object> thisRow, List<Object> prevRow)
	{
		return true;
	}

	/**
	 * Just after all internal member fields has been set, but before calling wrapperFor_sendAlarmRequest() we can do some adjustements...<br>
	 * For example CounterModelAppend uses this hookin to set some internal data structures.
	 */
	public void hookInNearEndOfRefreshGetData()
	{
	}

	/**
	 * Check if we are connected to any "online" data source 
	 * @return
	 */
	public boolean isConnected()
	{
		// If it's the GUI thread - AWT Event Dipatch Thread
		// Get the "cached" or last known status, maintaned by getCounterController().isMonConnected()
		// So never access the server to check if the connection is alive...
		if (SwingUtils.isEventQueueThread())
			return getCounterController().isMonConnectedStatus();

		// else 
		return getCounterController().isMonConnected();
	}

	/**
	 * Check if we are connected to any "offline" data source 
	 * @return
	 */
	public boolean isOfflineConnected()
	{
		return MainFrame.isOfflineConnected();
	}

	/**
	 * This is called at the end of every refresh 
	 * @return
	 */
	public void endOfRefresh()
	{
		if (getTabPanel() != null)
		{
			getTabPanel().setWatermark();
		}
	}

	/**
	 * Should we do diff calculation or not
	 * @return
	 */
	private boolean doDiffCalc()
	{
		if ( ! isDiffCalcEnabled() )
			return false;

		if (_pkCols == null || (_pkCols != null && _pkCols.size() == 0) )
		{
			if (_diffColumns.length == 0)
				return false;
		}
		return true;
	}

	/**
	 * Get the last sample exception if any
	 * @return null if no last exception
	 */
	public Exception getSampleException()
	{
		return _sampleException; 
	}
	/**
	 * If a Exception happened while we sampled for data, the this can be used to set the Exception
	 * @param e
	 */
	public void setSampleException(Exception e) 
	{
		_sampleException = e;
		if (e != null)
		{
			if      (e instanceof PcsSavedException)            { /*do nothing*/ }
			else if (e instanceof LostConnectionException)      { /*do nothing*/ }
			else if (e instanceof NoValidRowsInSample)          _logger.debug("setSampleException() for cm '"+getName()+"'. " + e.toString()); // do not pass the "stacktrace" in the errorlog
			else if (e instanceof DependsOnCmPostponeException) _logger.info ("setSampleException() for cm '"+getName()+"'. " + e.toString()); // do not pass the "stacktrace" in the errorlog
			else if (e instanceof SQLException)                 _logger.info ("setSampleException() for cm '"+getName()+"'. " + e.toString()); // do not pass the "stacktrace" in the errorlog
			else                                                _logger.warn ("setSampleException() for cm '"+getName()+"'. " + e.toString(), e);
		}
	}

	/** Get time when we last did a data refresh */
	public long getLastLocalRefreshTime()
	{ 
		return _lastLocalRefreshTime; 
	}

	/**
	 * Get time when we did the PREVIOUS data refresh, NOTE this is grabbed by System.currentTimeMillis() 
	 * If no previous sample has been done, it will return 0
	 */
	public long getPrevLocalRefreshTime()
	{
//		return _prevSample == null ? 0 : _prevSample.getLastLocalSampleTime();
		return _prevLocalRefreshTime;
	}

	/**  */
	public boolean isPostponeEnabled() 
	{
		return _postponeIsEnabled; 
	}
	/** If this CM has a "delay" option set */
	public int getPostponeTime() 
	{
		return _postponeTime; 
	}
//	/** Set smallest time between two samples */
//	public void setPostponeTime(int seconds) 
//	{
//		setPostponeTime(seconds, true);
//	}
	/** Set smallest time between two samples */
	public void setPostponeTime(int seconds, boolean saveProps) 
	{ 
		// No need to continue if we are not changing it
		if (getPostponeTime() == seconds)
			return;

		_postponeTime = seconds;
		
		if (saveProps)
			saveProps();

		if (getTabPanel() != null)
			getTabPanel().setPostponeTime(seconds);
	}

	/** Set smallest time between two samples */
	public void setPostponeIsEnabled(boolean enabled, boolean saveProps) 
	{ 
		// No need to continue if we are not changing it
		if (isPostponeEnabled() == enabled)
			return;

		_postponeIsEnabled = enabled;
		
		if (saveProps)
			saveProps();

		if (getTabPanel() != null)
			getTabPanel().setPostponeIsEnabled(enabled);
	}

	/** get dependant CM's postpone time */
	public CountersModel getDependantCmThatHasPostponeTime()
	{
		Set<String> cmSet = getDependsOnCm();
		if (cmSet == null)
			return null;

		for (String cmName : cmSet)
		{
			CountersModel cm = getCounterController().getCmByName(cmName);
			if (cm != null)
			{
				long postpone = cm.getTimeToNextPostponedRefresh();
				if (postpone > 0)
					return cm;
			}
		}
		return null;
	}

	/** do we need to postpone next refresh */
	public long getTimeToNextPostponedRefresh()
	{
		if ( doAnyDependantCmNeedMeToRefresh() )
			return 0;

		if ( ! isPostponeEnabled() )
			return 0;

		if (getPostponeTime() == 0)
			return 0;

		if ( getCounterController().isCmInDemandRefreshList(this.getName()) )
			return 0;
		
		// do not postpone next sample if diff data hasnt been calculated
		if (isDiffCalcEnabled() && ! hasDiffData())
			return 0;

		long timeNow         = System.currentTimeMillis();
		long msToNextRefresh = (getLastLocalRefreshTime() + (getPostponeTime() * 1000)) - timeNow;

		if (msToNextRefresh < 0)
			return 0;
		return msToNextRefresh;
	}

	/** 
	 * initialize Diss/Diff/Pct column bitmaps 
	 */
	private void initColumnStuff(CounterSample cnt)
	{
		// Initialize isDiffDissCol array
		if (_isDiffDissCol == null)
		{
			List<String> colNames = cnt.getColNames();
			if (colNames != null)
			{
				_isDiffDissCol = new boolean[ colNames.size() ];
				for (int i = 0; i < _isDiffDissCol.length; i++)
				{
					String colname = (String) colNames.get(i);
					boolean found = false;
					for (int j = 0; j < _diffDissColumns.length; j++)
					{
						if (colname.equals(_diffDissColumns[j]))
							found = true;
					}
					_logger.trace(_name+" col["+i+"]="+found+", colname="+colname);
					_isDiffDissCol[i] = found;
				}
			}
		}

		// Initialize isDiffCol array
		if (_isDiffCol == null)
		{
			List<String> colNames = cnt.getColNames();
			if (colNames != null)
			{
				_isDiffCol = new boolean[ colNames.size() ];
				for (int i = 0; i < _isDiffCol.length; i++)
				{
					String colname = (String) colNames.get(i);
					boolean found = false;
					for (int j = 0; j < _diffColumns.length; j++)
					{
						if (colname.equals(_diffColumns[j]))
							found = true;
					}
					_logger.trace(_name+" col["+i+"]="+found+", colname="+colname);
					_isDiffCol[i] = found;
				}
			}
		}

		// Initialize isPctCol array
		if (_isPctCol == null)
		{
			List<String> colNames = cnt.getColNames();
			if (colNames != null)
			{
				_isPctCol = new boolean[ colNames.size() ];
				for (int i = 0; i < _isPctCol.length; i++)
				{
					String colname = (String) colNames.get(i);
					boolean found = false;
					for (int j = 0; j < _pctColumns.length; j++)
					{
						if (colname.equals(_pctColumns[j]))
							found = true;
					}
					_logger.trace(_name+" col["+i+"]="+found+", colname="+colname);
					_isPctCol[i] = found;
				}
			}
		}
	}

	/**
	 * Override this to check for other dependency checks than time diff of 1000 ms
	 * @throws Exception
	 */
	public void checkDependsOnOtherCm(CountersModel cm)
	throws Exception
	{
		if (cm == null)
			return;

		long timeDiff = System.currentTimeMillis() - cm.getLastLocalRefreshTime();
		long timeLimit = 5000;
		if (timeDiff > timeLimit)
		{
			String msg = "The cm '"+this.getName()+"' depends on '"+cm.getName()+"', which last refresh time was '"+timeDiff+"' ms ago. " +
					"Next refresh time for '"+cm.getName()+"' is in "+TimeUtils.msToTimeStr("%HH:%MM:%SS", cm.getTimeToNextPostponedRefresh())+". " +
					"The limit has to be less than '"+timeLimit+"' ms.";
			//_logger.warn(msg);
			throw new DependsOnCmPostponeException(msg);
		}
	}
	/** Check other CM's for dependencis */
	public void checkDependsOnOtherCm()
	throws Exception
	{
		Set<String> cmSet = getDependsOnCm();
		if (cmSet == null)
			return;

		for (String cmName : cmSet)
		{
			CountersModel cm = getCounterController().getCmByName(cmName);
			if (cm == null)
			{
				String msg = "The cm '"+this.getName()+"' depends on '"+cmName+"', which can't be found.";
				//_logger.error(msg);
				throw new Exception(msg);
			}
			
			checkDependsOnOtherCm(cm);
		}
	}
	/** Add a CM that we depends upon */
	public void addDependsOnCm(String cmName)
	{
		if (_dependsOnCm == null)
			_dependsOnCm = new LinkedHashSet<String>();
		_dependsOnCm.add(cmName);
	}
	/** Get a list of CM's that we depends upon */
	public Set<String> getDependsOnCm()
	{
		return _dependsOnCm;
	}

	/** Add a CM that this CM depends upon */
	public void addCmDependsOnMe(String cmName)
	{
		if (_cmsDependsOnMe == null)
			_cmsDependsOnMe = new LinkedHashSet<String>();
		_cmsDependsOnMe.add(cmName);
	}
	/** Get a list of CM's that we depends upon */
	public Set<String> getCmDependsOnMe()
	{
		return _cmsDependsOnMe;
	}

	/** Check all CM's that depends on ME, if they needs to be refreshed...
	 * Since the depends on me to have data, then I need to refresh
	 * @return
	 */
	private boolean doAnyDependantCmNeedMeToRefresh()
	{
		Set<String> cmSet = getCmDependsOnMe();
		if (cmSet == null)
			return false;
		
		for (String cmName : cmSet)
		{
			CountersModel cm = getCounterController().getCmByName(cmName);
			if (cm == null)
			{
				String msg = "The cm '"+this.getName()+"' wants to check if cm '"+cmName+"' needs to be refreshed, bat that CM ("+cmName+") can't be found.";
				_logger.warn(msg);
			}
			else
			{
				if (cm.isRefreshable())
				{
					return true;
				}
			}
		}
		return false;
	}

	public void incRefreshCounter() { _refreshCounter++; }
	public int  getRefreshCounter() { return _refreshCounter; }

//	public void incSumRowCountAbs(int inc) { _sumRowCountAbs += inc; }
//	public int  getSumRowCountAbs() { return _sumRowCountAbs; }
//
//	public void incSumRowCountDiff(int inc) { _sumRowCountDiff += inc; }
//	public int  getSumRowCountDiff() { return _sumRowCountDiff; }
//
//	public void incSumRowCountRate(int inc) { _sumRowCountRate += inc; }
//	public int  getSumRowCountRate() { return _sumRowCountRate; }

	public int  getSumRowCount() { return _sumRowCount; }
//	public void incSumRowCount() { _sumRowCount += getRowCount(); }
	public void incSumRowCount(int inc) { _sumRowCount += inc; }

	/** Used as an indicator if we are in refresh() method... Then some operations might not be advisable. A bwtter alternative is to use ReentrantLock() + lock.tryLock() */
	private boolean _isInRefresh = false;
	/** Are we in refreh or not */
	public boolean isInRefresh()
	{
		return _isInRefresh;
		
		// NOTE: It might be better to solve this using the below code instead...
//		Lock lock = new ReentrantLock();
//		......
//		if (lock.tryLock())
//		{
//			// Got the lock
//			try
//			{
//				// Process record
//			}
//			finally
//			{
//				// Make sure to unlock so that we don't cause a deadlock
//				lock.unlock();
//			}
//		}
//		else
//		{
//			// Someone else had the lock, abort
//		}
	}

	/** called from GUI to refresh data */
	public final synchronized void refresh() throws Exception
	{
		refresh(getCounterController().getMonConnection());
	}

	/** Refresh data */
	public final synchronized void refresh(DbxConnection conn) throws Exception
	{
		// check if we depends on other CM's
		try 
		{ 
			checkDependsOnOtherCm(); 
		}
		catch (DependsOnCmPostponeException e)
		{
			// If we depends on another CM's postpone time, just get out of here
			// NOTE: should we do setValidSampleData(false) here or not?, I think so...
			setValidSampleData(false);
			return;
		}

		// If other CM's depends on Me that needs refres
//		if ( doAnyDependantCmNeedMeToRefresh() )
//		{
//			// simply continue with refresh
//		}
//		else
//		{
			// is it time to do refresh or not
    		if (getTimeToNextPostponedRefresh() > 0)
    		{
    			if (_logger.isDebugEnabled()) 
    				_logger.debug("Next refresh for the cm '"+getName()+"' will have to wait '"+TimeUtils.msToTimeStr(getTimeToNextPostponedRefresh())+"'.");
    			// NOTE: should we do setValidSampleData(false) here or not?, I think so...
    			setValidSampleData(false);
    			return;
    		}
//		}

		// SET the message handler used for this CM
		SybMessageHandler curMsgHandler = null;
		if (conn instanceof SybConnection)
		{
			curMsgHandler = ((SybConnection)conn).getSybMessageHandler();
			((SybConnection)conn).setSybMessageHandler(getSybMessageHandler());
		}
		// Set a TDS Message Handler
		if (conn instanceof TdsConnection)
			((TdsConnection)conn).setSybMessageHandler(getSybMessageHandler());
		
		// now MAKE the refresh
		int rowsFetched = 0;
		try
		{
			_isInRefresh = true;
			
			// reset some stuff
			setNonConfiguredMonitoringHappened(false); // also resets the message(s) etc...
			setSampleException(null);

			// execute any "pre-check" for this CM
			boolean doRefresh = beforeRefreshGetData(conn);
			
			if (doRefresh)
			{
				// call the implementation
				rowsFetched = refreshGetData(conn);
			}

			// if we fetched any rows, this means that we would have a Valid Sample
			// a rowcount with -1 = FAILURE
			//   rowcount with 0  = SUCCESS, but now rows where fetched. 
			setValidSampleData( rowsFetched > 0 || hasTrendGraphData() );

			// FIXME: whatabout clearing the JTable entries????
			//        or should we leave the "old" entries in there, they are not valid anymore????
			// but I need to look at this in more detail... so offline and tailMode arn't fucked up...
			
			// Simulate a slow connection...
			//try { Thread.sleep(300); }
			//catch (InterruptedException ignore) {}
		}
		catch (Exception e)
		{
			setValidSampleData(false);
			setSampleException(e);
			
			// If we got an exception, go and check if we are still connected
			// NOTE: refreshGetData(conn) above doesnt throw SQLException on problems...
			//       so any implemeters of refreshGetData(conn) need to think a bit...
			if ( ! getCounterController().isMonConnected(true, true) ) // forceConnectionCheck=true, closeConnOnFailure=true
			{
				_logger.warn("When refreshing the data for cm '"+getName()+"', we Caught an Exception and we are no longer connected to the monitored server. Exception="+e);
				throw new LostConnectionException("When refreshing the data for cm '"+getName()+"', we Caught an Exception and we are no longer connected to the monitored server. Caught: "+e, e);
			}

			throw e;
		}
		finally
		{
			if (hasNonConfiguredMonitoringHappened())
			{
				setNonConfiguredMonitoringMissingParams(getMissigConfig(conn));
				fireNonConfiguredMonitoringHappened();
			}

			// restore old message handler
			if (curMsgHandler != null)
			{
				((SybConnection)conn).setSybMessageHandler(curMsgHandler);
			}
			// Restore old message handler
			if (conn instanceof TdsConnection)
				((TdsConnection)conn).restoreSybMessageHandler();

			_isInRefresh = false;
		}

		// increment counter
		if (rowsFetched > 0)
		{
			incRefreshCounter();
			incSumRowCount(rowsFetched);
		}
		setInitialized(true);

		// Set last refresh time
		_lastLocalRefreshTime = System.currentTimeMillis();

		_isInRefresh = false;
	}

	/**
	 * Create a Object that a sample can be put into
	 * <p>
	 * This can be overridden if you want to use another implementation. 
	 */
	public CounterSample createCounterSample(String name, boolean negativeDiffCountersToZero, String[] diffColumns, CounterSample prevSample)
	{
		return new CounterSample(name, negativeDiffCountersToZero, diffColumns, prevSample);
	}

	/**
	 * Called just before <code>refreshGetData</code> so we can verify <i>whatever</i>
	 * <br>
	 * This is empty and should be implemented by any CM that needs it!
	 * 
	 * @param conn
	 * @return true         true = continue and refresh, false = skip refresh 
	 * @throws Exception    throws if we do not want to continue with refreshGetData(conn)
	 */
	public boolean beforeRefreshGetData(DbxConnection conn) throws Exception
	{
		return true;
	}

	/**
	 * This is the method to override if you want to different refresh
	 * TODO: check for more Exception, so we don't leave this code without resetting: _newSample, _diffData, _rateData
	 * 
	 * @param conn
	 * @return number of rows in the new sample.
	 * @throws Exception
	 */
	protected int refreshGetData(DbxConnection conn) throws Exception
	{
		if (_logger.isDebugEnabled())
			_logger.debug("Entering refreshCM() method for " + _name);

		if (conn == null)
			return -1;

		if (_logger.isDebugEnabled())
			_logger.debug("Refreshing Counters for '"+getName()+"'.");

		// Start the timer which will be kicked of after X milliseconds
		// This so we can do something if the refresh takes to long time
		_refreshTimer.start();
		_refreshTimerStartTime = System.currentTimeMillis();


		// If the CounterModel need to be initialized by executing any 
		// specific SQL statement the firts time around
		if ( ! _sqlInitDone )
		{
			_sqlInitDone = doSqlInit();
		}
//		String sqlInit = getSqlInit();
//		if (sqlInit != null && !sqlInit.trim().equals(""))
//		{
//			if ( ! _sqlInitDone )
//			{
//				try
//				{
////					Statement stmt = conn.createStatement();
////					stmt.execute(sqlInit);
////					stmt.close();
//					doSqlInit(conn, sqlInit);
//
//					_sqlInitDone = true;
//				}
//				catch (SQLException e)
//				{
//					_logger.warn("Problem when executing the 'init' SQL statement: "+sqlInit, e);
//				}
//			}
//		}

		if (getSql() == null)
		{
			initSql(conn);
		}

		//--------------------------------------------------
		// NOTE:
		//--------------------------------------------------
		// Store the NEW values in a TEMP structure
		// Move it over to the "real" structure "near the end"
		// if this is NOT done, Swing will use the modified model with the "unmodified" view, 
		// this since tableChanged() has not yet been called and readjust the view to the model.
		//--------------------------------------------------
		CounterSample tmpDiffData = null;
		CounterSample tmpRateData = null;
//		CounterSample tmpNewSample = new CounterSample(_name, _negativeDiffCountersToZero, _diffColumns, _prevSample);
		CounterSample tmpNewSample = createCounterSample(_name, _negativeDiffCountersToZero, _diffColumns, _prevSample);
		try
		{
			setSampleException(null);
			beginSqlRefresh();
			
			// If we need to create a new SQL Statement for every Sample
			if (createNewSqlForEveryRefresh())
			{
				String sql = getSqlForVersion(conn, conn.getDbmsVersionInfo());

				// Replace any "${cmCollectorName}" --> "xxxTune:CmName"
				sql = replaceCollectorName(sql);
				
				// set the SQL to use
				if (useSqlPrefix())
				{
					String sqlPrefix = getSqlPrefix(); 
					setSql(sqlPrefix + sql);
				}
				else
				{
					setSql(sql);
				}
			}

			// construct the SQL to execute
			String sql = getSql() + getSqlWhere();

			// SAMPLE data
			if ( tmpNewSample.getSample(this, conn, sql, _pkCols) == false )
				return -1;
			
			// If we want to test some error handling
			boolean exceptionTest = false;
//exceptionTest = true;
			if (exceptionTest)
			{
    			if ("CmProcessActivity".equals(getName()))
    				throw new SQLException("dummy test of '2714'...", "XXXX", 2714);
			}
			
			_sequentialTimeoutExceptionCount = 0;
			_oldestTimeoutExceptionTime = -1;
		}
		catch (NoValidRowsInSample e)
		{
			setSampleException(e);

			if (_tabPanel != null)
				_tabPanel.setWatermarkText(e.getMessage());

			// No data should be visible, so reset some structures
			// NOTE: is this enough or should we do fireXXXXChange as well... 
			_newSample = null;
			_diffData  = null;
			_rateData  = null;
			
			return -1;
		}
		catch (SQLException e)
		{
			setSampleException(e);

			if (_tabPanel != null)
				_tabPanel.setWatermarkText(e.getMessage());

			// No data should be visible, so reset some structures
			// NOTE: is this enough or should we do fireXXXXChange as well... 
			_newSample = null;
			_diffData  = null;
			_rateData  = null;

			// Will this work, or will it just "hang" as well
			if (conn instanceof SybConnection)
			{
				_logger.info("Calling 'cancel()' on the SybConnection.");
				((SybConnection)conn).cancel();
				
				// Execute the cancel in some "timeout" sensitive code block...
				// If it takes to long... close() the connection... 
				// but that might also take to long time... Then what...
				//conn.close();
			}
			
			// For Postgres and possibly others (if we are sampling ALL CM's in a transaction) 
			// If we are in a transaction (AutoCommit=false) AND had an error, we can't continue 
			// issuing SQL Statements until we "end" the transaction and start a a new
			try
			{
				if (conn.getAutoCommit() == false)
				{
					// Lets rollback and start a new transaction
					conn.rollback();

					_logger.info("After Exception in refresh() for cm '" + getName() + "', the connection was in AutoCommit. So lets rollback and start a new transaction.");
				}
			}
			catch (SQLException getAcEx)
			{
				_logger.warn("Problems AFTER Exception in refresh() for cm '" + getName() + "', Trying call getAutoCommit(). SQLException=" + getAcEx);
			}

			// If we got an exception, go and check if we are still connected
			if ( ! getCounterController().isMonConnected(true, true) ) // forceConnectionCheck=true, closeConnOnFailure=true
			{
				_logger.warn("When refreshing the data for cm '"+getName()+"', we Caught an Exception and we are no longer connected to the monitored server. Exception="+e);
				//return -1;
				throw new LostConnectionException("When refreshing the data for cm '"+getName()+"', we Caught an Exception and we are no longer connected to the monitored server. Caught: "+e, e);
			}

			// Msg 10353:               You must have any of the following role(s) to execute this command/procedure: 'mon_role' . Please contact a user with the appropriate role for help.
			// Msg 12052:               Collection of monitoring data for table '%.*s' requires that the %s configuration option(s) be enabled. To set the necessary configuration, contact a user who has the System Administrator (SA) role.
			// Msg 12036: ASE 12.5.0.3: Collection of monitoring data for table '%.*s' requires that the '%s' configuration option(s) be enabled. To set the necessary configuration, contact a user who has the System Administrator (SA) role.
			//     12036: ASE 15.7.0.0: Incomplete configuration on instance ID %d. Collection of monitoring data for table '%.*s' requires that the %s configuration option(s) be enabled. To set the necessary configuration, contact a user who has the System Administrator (SA) role.
			// so lets reinitialize CM, to check again for next sample
			int    errorCode = e.getErrorCode();
			String errorMsg  = e.getMessage();
			if (errorCode == 10353 || errorCode == 12052 || errorCode == 12036)
			{
				// check if 'enable monitoring' is still ON
				// If this is not ON anymore, do something drastic like 'stop minitoring' or shutdown
				if ( ! AseConnectionUtils.getAseConfigRunValueBooleanNoEx(conn, "enable monitoring") )
				{
					_logger.warn("Monitoring has been disabled. someone has done (sp_configure 'enable monitoring', 0) from another session. I cant continue... Closing the database connection.");
//					try { conn.close(); }
//					catch (SQLException ignore) {}
					getCounterController().closeMonConnection();
					
					// NOTE: can we do this in a better way?
					// I dont like 'hasGUI()' switches...
					if (DbxTune.hasGui())
					{
						String dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());

						// OK, this is non-modal, but the OK button doesn't work, fix this later, and use the X on the window instead
						JOptionPane optionPane = new JOptionPane(
								"<html>" +
								  "<h2>Monitoring has been disabled.</h2>" +
								  "Someone has done: <br>" +
								  "<code>sp_configure 'enable monitoring', 0</code><br>" +
								  "from another session<br>" +
								  "<br>" +
								  "I can't continue monitoring... <br>" +
								  "Closing the database connection.<br>" +
								  "Please reconnect to the server again.<br>" +
								"</html>", 
								JOptionPane.ERROR_MESSAGE);
//						JDialog dialog = optionPane.createDialog(MainFrame.getInstance(), "Monitoring has been disabled @ "+dateStr);
						JDialog dialog = optionPane.createDialog((getGuiController() == null ? null : getGuiController().getGuiHandle()), "Monitoring has been disabled @ "+dateStr);
						dialog.setModal(false);
						dialog.setVisible(true);

						MainFrame.getInstance().setStatus(MainFrame.ST_DISCONNECT);
						if (getGuiController() != null)
							getGuiController().setStatus(MainFrame.ST_DISCONNECT);
					}
				}
				else
				{
					// Maybe downgrade this to a INFO message in a later release...
					_logger.warn("Trying to Re-Initializing Performance Counter '"+getDisplayName()+"' shortName='"+getName()+"', After receiving MsgNumber '"+errorCode+"', with Description '"+errorMsg+"'.");
	
					initSql(conn);
	
					try
					{
						//cm.clear(); // clears the Counters and GUI parts
						init(conn);
					}
					catch (Exception ex2)
					{
						_logger.warn("Problems Re-Initializing Performance Counter '"+getDisplayName()+"' shortName='"+getName()+"', After MsgNumber '"+errorCode+"', with Description '"+errorMsg+"', Caught: "+ex2, ex2);
					}
				}
			}

			// --- ASE ---- 
			// 2714 There is already an object named '#xxxxxxxxxx' in the database.
			if (errorCode == 2714)
			{
				// Mark the connection for "reconnect" on the next sample
				// This to try to workaround this issue
				// - checking/dropping at the start of the SQL Batch...
				// - dropping the tables at the end of this method: dropTempTables()
				_aseError_2714_count++;
				_logger.info("Temp Table Creation error for cm '"+getName()+"'. Setting _aseError_2714_count="+_aseError_2714_count+"' after "+_aseError_2714_actionThreshold+" errors an action will be taken, which is 're-connect'.");
				if (_aseError_2714_count > _aseError_2714_actionThreshold)
				{
					_logger.warn("Temp Table Creation error for CM='"+getName()+"'. The connection is marked for 're-connect'. After all CM's has been refreshed the DBMS connection will be re-established. (_aseError_2714_count="+_aseError_2714_count+", _aseError_2714_actionThreshold="+_aseError_2714_actionThreshold+").");

					// Mark the connection...
					conn.setConnectionMark(DbxConnection.MarkTypes.MarkForReConnect);
					
					// Reset the counter...
					_aseError_2714_count = 0;
				}
			}

			// --- ASE ---- 
			// Error=701, Severity=17, Text=There is not enough procedure cache to run this procedure, trigger, or SQL batch. Retry later, or ask your SA to reconfigure ASE with more procedure cache.
			// If we got a AlarmHandler... send alarm about this...
			if (errorCode == 701)
			{
				if (AlarmHandler.hasInstance())
				{
					AlarmEvent alrmEvent = new AlarmEventProcedureCacheOutOfMemory(this);
					AlarmHandler.getInstance().addAlarm(alrmEvent);
				}
			}

			// --- ASE ---- 
			// Error=1204, Severity=17, Text=ASE has run out of LOCKS. Re-run your command when there are fewer active users, or contact a user with System Administrator (SA) role to reconfigure ASE with more LOCKS.
			if (errorCode == 1204 && AlarmHandler.hasInstance())
			{
				AlarmHandler.getInstance().addAlarm( new AlarmEventConfigResourceIsUsedUp(this, "number of locks", errorCode, errorMsg, null) );				
			}
			
			
			// Look for Timeout exception
			boolean isTimeoutException = false;
			
			if (e instanceof SQLTimeoutException) // Not sure in what JDBC Version this was introduced, but not all JDBC Drivers are using this.
				isTimeoutException = true;
			
			if ("JZ006".equals(e.getSQLState()))
			{
				String msg = e.toString();
				if (msg != null)
				{
					// Below is a couple of timeout examples 
					//--------------------------------------------------------
					// JZ006: Caught IOException: java.io.IOException: JZ0T3: Read operation timed out.
					// JZ006: Se ha detectado IOException: java.io.IOException: JZ0T3: Ha finalizado el tiempo de espera para la operaci?n de lectura.
					// JZ006: IOException: java.io.IOException: JZ0T3: Lesevorgang dauerte zu lange (time out) wurde abgefangen.
					// JZ006: IOException ??: java.io.IOException: JZ0T3: ?? ?? ??? ???????.
					//
					// JZ006: Caught IOException: java.net.SocketTimeoutException: Read timed out.
					// JZ006: Se ha detectado IOException: java.net.SocketTimeoutException: Read timed out.
					//--------------------------------------------------------

					if (msg.indexOf("JZ0T3") > 0)
						isTimeoutException = true;

					if (msg.indexOf("SocketTimeoutException") > 0)
						isTimeoutException = true;
				}
			}
			if ("HY008".equals(e.getSQLState())) // MS SQL Server
			{
				// ErrorCode=0, SqlState=HY008, Message=|The query has timed out.|. execTimeInMs=10002, SQL: ...
				//    com.microsoft.sqlserver.jdbc.SQLServerException: The query has timed out.
				//            at com.microsoft.sqlserver.jdbc.TDSCommand.checkForInterrupt(IOBuffer.java:7819)
				//            at com.microsoft.sqlserver.jdbc.TDSParser.parse(tdsparser.java:98)
				//            at com.microsoft.sqlserver.jdbc.TDSParser.parse(tdsparser.java:37)
				//            at com.microsoft.sqlserver.jdbc.SQLServerResultSet$FetchBuffer.nextRow(SQLServerResultSet.java:5473)
				//            at com.microsoft.sqlserver.jdbc.SQLServerResultSet.fetchBufferNext(SQLServerResultSet.java:1790)
				//            at com.microsoft.sqlserver.jdbc.SQLServerResultSet.next(SQLServerResultSet.java:1048)
				//            at com.dbxtune.cm.CounterSample.getSample(CounterSample.java:1117)
				//            at com.dbxtune.cm.CountersModel.refreshGetData(CountersModel.java:5069)
				//            at com.dbxtune.cm.CountersModel.refresh(CountersModel.java:4901)
				//            at com.dbxtune.cm.CountersModel.refresh(CountersModel.java:4837)
				//            at com.dbxtune.CounterCollectorThreadNoGui.run(CounterCollectorThreadNoGui.java:1525)
				isTimeoutException = true;
			}
			if (isTimeoutException)
			{
				// Simply increment the counter (which is set to 0 on every success... or 'no-timeout')
				_sequentialTimeoutExceptionCount++;

				// Remember when when the first/oldest Timeout Exception happened.
				if (_oldestTimeoutExceptionTime < 0)
					_oldestTimeoutExceptionTime = System.currentTimeMillis();

				// If we want to disable "any" CM due to TimeoutExceptions... 
				// see: CmExecQueryStatPerDb.handleTimeoutException() as a "base code" that can be "moved" in here... 
				handleTimeoutException();
			}

			// Do cleanup...
			// If we got an exception, go and check if we are still connected
			if ( getCounterController().isMonConnected(false, true) ) // forceConnectionCheck=true, closeConnOnFailure=true
			{
				dropTempTables(conn);
				execCleanupCmdsOnException(conn);
			}

			// throw the exception to caller
			// Think more about this...
			//throw e;

			return -1; // NOTE: <<<<<<----------- get-out-of-here <<<<<<<<----------------
		} // end: SQLException
		finally
		{
			// Stop the timer.
			_refreshTimer.stop();
			endSqlRefresh();
		}

		// Grab some stuff from the PREVIOUS Sample before it's overwritten
		if (_prevSample != null)
			_prevLocalRefreshTime = _prevSample.getLastLocalSampleTime();

//System.out.println("_prevLocalRefreshTime="+_prevLocalRefreshTime+", age="+(System.currentTimeMillis()-_prevLocalRefreshTime)+", cmName='"+getName()+"'.");
		
		// translate some fields in the Absolute Counters
		beginLcRefresh();
		localCalculation(tmpNewSample);
		if (_prevSample != null)
			localCalculation(_prevSample, tmpNewSample);
		long firstLcTime = endLcRefresh();

		// initialize Diss/Diff/Pct column bitmaps
		initColumnStuff(tmpNewSample);


		// Do local sorting of the New-Sample 
		List<SortOptions> sortOptionsList = getLocalSortOptions();
		if (sortOptionsList != null && !sortOptionsList.isEmpty())
		{
			tmpNewSample.sort(sortOptionsList);
		}
		
		
		// Do Column Aggregation.
		// calculateAggregateRow does: 
		//  - getAggregateColumns(), which calls createAggregateColumns() if we do not yet have created any aggregates columns
		//  - note: if createAggregateColumns() returns NULL, calculateAggregateRow() will exit early
		// Question: Should we call this before or after the above "local sorting"
		try
		{
			// below may end up with data overflow (due to sum may overflow the data-type, and if we want to store the data in PCS we want to keep the origin data-type)
			// hence the try/catch
			calculateAggregateRow(DATA_ABS, tmpNewSample, false);
		}
		catch (RuntimeException ex)
		{
			_logger.error(getName() + ": Problems when performing Column Aggregation. Skipping this and continuing.", ex);
		}
		
		// if it's the first time sampling...
		if (isFirstTimeSample())
		{
//			saveDdl();
		}

		// Used later
//		final List<Integer> deletedRows  = new ArrayList<Integer>();
//		final List<Integer> newDeltaRows = new ArrayList<Integer>();
		final List<Integer> deletedRows  = null;
		final List<Integer> newDeltaRows = null;

		// Set sample time and interval
		setSampleTime(    tmpNewSample.getSampleTime()     );
		setSampleInterval(tmpNewSample.getSampleInterval() );

		// If NO PK, then we don't need to do some stuff.
		if ( ! doDiffCalc() )
		{
//			setSampleTime(    tmpNewSample.getSampleTime()     );
//			setSampleInterval(tmpNewSample.getSampleInterval() );

//			if (_prevSample != null)
//			{
//				tmpNewSample.interval = tmpNewSample.samplingTime.getTime() - _prevSample.samplingTime.getTime(); 
//				setSampleInterval(tmpNewSample.getSampleInterval());
//			}

			tmpDiffData = tmpNewSample;
			tmpRateData = tmpNewSample;

			// Compute local stuff
			beginLcRefresh();
			localCalculation(_prevSample, tmpNewSample, null);
			long secondLcTime = endLcRefresh();
			setLcRefreshTime(firstLcTime + secondLcTime);

			setDataInitialized(true); // maybe move this to the Run class later, which does the init
		}
		else
		{
			if (_prevSample != null)
			{
				// old sample is not null, so we can compute the diffs
//				tmpDiffData = CounterSample.computeDiffCnt(_prevSample, tmpNewSample, deletedRows, _pkCols, _isDiffCol, _isCountersCleared);
				tmpDiffData = computeDiffCnt(_prevSample, tmpNewSample, deletedRows, newDeltaRows, _pkCols, _isDiffCol, _isCountersCleared);
				
				// Aggregated MIN/MAX values has to be calculated for DIFF counters
				if (hasAggregatedRowId())
					calculateAggregateRow(DATA_DIFF, tmpDiffData, false);
			}
	
			if (tmpDiffData == null)
			{
//				setSampleTime(tmpNewSample.samplingTime);
//				setSampleInterval(0);	
			}
			else
			{
//				setSampleTime(tmpDiffData.samplingTime);
//				setSampleInterval(tmpDiffData.interval);
	
				beginLcRefresh();

				// Compute local stuff
				// NOTE: this needs to be done BEFORE computeRatePerSec()
				//       otherwise the PCT columns will still be the DIFF values
				localCalculation(_prevSample, tmpNewSample, tmpDiffData);
	
				// we got some data, compute the rates and update the data model
//				tmpRateData = CounterSample.computeRatePerSec(tmpDiffData, _isDiffCol, _isPctCol);
				tmpRateData = computeRatePerSec(tmpDiffData, _isDiffCol, _isPctCol);

				// Aggregated MIN/MAX values has to be calculated for RATE counters
				if (hasAggregatedRowId())
					calculateAggregateRow(DATA_RATE, tmpDiffData, false);

				// Compute local stuff for RatePerSec, here we can adjust some stuff if needed
				localCalculationRatePerSec(tmpRateData, tmpDiffData);
	
				long secondLcTime = endLcRefresh();
				setLcRefreshTime(firstLcTime + secondLcTime);

				// If we would like to have a "localRateCalculation()" this would be the place to implement it
				//localRateCalculation(tmpRateData)

				setDataInitialized(true); // maybe move this to the Run class later, which does the init
			}
		}

		// Check if there is any rows that we want to interrogate more, , every CM's has to implement this.
		sendDdlDetailsRequest(tmpNewSample, tmpDiffData, tmpRateData);


//		if ( ! DbxTune.hasGui() )
//		{
//			// NO GUI, move structures
//			_prevSample = tmpNewSample;
//			_newSample  = tmpNewSample;
//			_diffData   = tmpDiffData;
//			_rateData   = tmpRateData;
//
//			// if it's the first time sampling...
//			if (isFirstTimeSample())
//			{
//				fireTableStructureChanged();
//			}
//
//			// Calculte what values we should have in the graphs
//			// this has to be after _prevSample, _newSample, _diffData, _rateData has been SET
//			updateGraphData();
//
//			// Do we want to send an Alarm somewhere, every CM's has to implement this.
//			wrapperFor_sendAlarmRequest();
//
//			setFirstTimeSample(false);
//		}
//		else // HAS GUI
//		{
//			// Make them final copies to be used in the doWork/Runnable below
//			final CounterSample fTmpNewSample = tmpNewSample;
//			final CounterSample fTmpDiffData = tmpDiffData;
//			final CounterSample fTmpRateData = tmpRateData;
//
//			final CountersModel thisCm = this;
//
//			Runnable doWork = new Runnable()
//			{
//				@Override
//				public void run()
//				{
//					// IMPORTANT: move datastructure.
//					_prevSample = fTmpNewSample;
//					_newSample  = fTmpNewSample;
//					_diffData   = fTmpDiffData;
//					_rateData   = fTmpRateData;
//					
//					beginGuiRefresh();
//
//					// Set: Info fields
//					if (getGuiController() != null)
//						getGuiController().setStatus(MainFrame.ST_STATUS2_FIELD, "GUI refresh of '"+_displayName+"'");
//
//					// Simulate a SLOW action, for example a massive sort...
//					// which would case the GUI to block for a while...
//					//try { Thread.sleep(250); }
//					//catch (InterruptedException ignore) {}
//
//					try
//					{
//						if (getTabPanel() != null && !getTabPanel().isTableInitialized())
//						{
//							//System.out.println(getName()+":-fireTable-STRUCTURE-CHANGED-");
//							if (_logger.isDebugEnabled())
//								_logger.debug(getName()+":------doFireTableStructureChanged------");
//							fireTableStructureChanged();
//							
//							// Hmm do I need to do this here...
//							//getTabPanel().adjustTableColumnWidth();
//						}
//						else if (isFirstTimeSample() && isDataInitialized())
//						{
//							//System.out.println(getName()+":-fireTable-STRUCTURE-CHANGED-");
//							if (_logger.isDebugEnabled()) 
//								_logger.debug(getName()+":------doFireTableStructureChanged------");
//							fireTableStructureChanged();
//						}
//						else
//						{
//							//System.out.println(getName()+":-fireTableData-CHANGED-");
//							if (_logger.isDebugEnabled()) 
//								_logger.debug(getName()+":-fireTableData-CHANGED-");
//							fireTableDataChanged();
//						}
//					}
//					catch(Throwable t)
//					{
//						_logger.warn("Problem when doing fireTableStructureChanged() or fireTableDataChanged(), for the CM='"+thisCm.getName()+"'", t);
//					}
//
//					// Calculte what values we should have in the graphs
//					// this has to be after _prevSample, _newSample, _diffData, _rateData has been SET
//					// since we do this differred in case we use Swing, it has to be done here.
//					updateGraphData();
//		
//					// reset: Info fields
////					MainFrame.getInstance().setStatus(MainFrame.ST_STATUS2_FIELD, "");
//					if (getGuiController() != null)
//						getGuiController().setStatus(MainFrame.ST_STATUS2_FIELD, "");
//
//					endGuiRefresh();
//
//					// send DDL Request info based on the sorted JTable
//					if (getTabPanel() != null)
//					{
//						getTabPanel().ddlRequestInfoSave();
//					}
//
//					// Do we want to send an Alarm somewhere, every CM's has to implement this.
//					wrapperFor_sendAlarmRequest();
//
//					setFirstTimeSample(false);
//
//				} // end: run method
//			};
//
//			//			try{SwingUtilities.invokeAndWait(doWork);}
//			//			catch (Exception e) {e.printStackTrace();}
//
//			// INVOKE the above RUNNABLE on the SWING EDT (Event Dispather Thread)
//			if ( ! SwingUtilities.isEventDispatchThread() )
//				SwingUtilities.invokeLater(doWork);
//			else
//				doWork.run();
//		}


		// At the very end we can assign the internal members
		// and for GUI lets kick off "change listeners" to update the GUI, which needs to be done in the SWING - Event Dispatcher Thread
		
		// assign members
		_prevSample = tmpNewSample;
		_newSample  = tmpNewSample;
		_diffData   = tmpDiffData;
		_rateData   = tmpRateData;

		// if it's the first time sampling... but I do not think we have to do this when we do NOT have a GUI
//		if ( isFirstTimeSample() && ! DbxTune.hasGui() )
//			fireTableStructureChanged();

		// Calculte what values we should have in the graphs
		// this has to be after _prevSample, _newSample, _diffData, _rateData has been SET
		updateGraphData();

		// last chance to hookin and do something with data
		hookInNearEndOfRefreshGetData();
		
		// Do we want to send an Alarm somewhere, every CM's has to implement this.
		// NOTE: This is now called from CounterCollectorThreadGui/CounterCollectorThreadNoGui
		//       This so the Alarm "creation" can include/reference data from CM's that has been refreshed later than the current CM (so we can include data in the Extended descriptions)
//		wrapperFor_sendAlarmRequest();
		
		// Remember "first time sample" for the "doWork" object 
		final boolean localFirstTimeSample = isFirstTimeSample();

		// first sample is DONE
		// This is MOVED "outside" of this method -- "at the end" of CounterCollectorThread{Gui|NoGui}  (after check alarms)
//		setFirstTimeSample(false);

		// If we have a GUI we need to do some extra stuff
		// let the GUI know that we have new data, done via fire*
		if ( DbxTune.hasGui() )
		{
			final CountersModel thisCm = this;

			Runnable doWork = new Runnable()
			{
				@Override
				public void run()
				{
					beginGuiRefresh();

					// Set: Info fields
					if (getGuiController() != null)
						getGuiController().setStatus(MainFrame.ST_STATUS2_FIELD, "GUI refresh of '"+_displayName+"'");

					try
					{
						if (getTabPanel() != null && !getTabPanel().isTableInitialized())
						{
							//System.out.println(getName()+":-fireTable-STRUCTURE-CHANGED-");
							if (_logger.isDebugEnabled())
								_logger.debug(getName()+":------doFireTableStructureChanged------");
							fireTableStructureChanged();
							
							// Hmm do I need to do this here...
							//getTabPanel().adjustTableColumnWidth();
						}
						else if (localFirstTimeSample && isDataInitialized())
						{
							//System.out.println(getName()+":-fireTable-STRUCTURE-CHANGED-");
							if (_logger.isDebugEnabled()) 
								_logger.debug(getName()+":------doFireTableStructureChanged------");
							fireTableStructureChanged();
						}
						else
						{
							//System.out.println(getName()+":-fireTableData-CHANGED-");
							if (_logger.isDebugEnabled()) 
								_logger.debug(getName()+":-fireTableData-CHANGED-");
							fireTableDataChanged();
						}
					}
					catch(Throwable t)
					{
						_logger.warn("Problem when doing fireTableStructureChanged() or fireTableDataChanged(), for the CM='"+thisCm.getName()+"'", t);
					}

					// reset: Info fields
//					MainFrame.getInstance().setStatus(MainFrame.ST_STATUS2_FIELD, "");
					if (getGuiController() != null)
						getGuiController().setStatus(MainFrame.ST_STATUS2_FIELD, "");

					endGuiRefresh();

					// send DDL Request info based on the sorted JTable
					if (getTabPanel() != null)
					{
						getTabPanel().ddlRequestInfoSave();
					}
				} // end: run method
			};

			// INVOKE the above RUNNABLE on the SWING EDT (Event Dispather Thread)
			if ( ! SwingUtilities.isEventDispatchThread() )
				SwingUtilities.invokeLater(doWork);
			else
				doWork.run();
		}
		
		
		// Finally return number of records found in LAST refresh
		return (tmpNewSample != null) ? tmpNewSample.getRowCount() : -1;
	}

	/**
	 * How many times have we got a "TimeoutException" when executing SQL <br>
	 * Note: This counter is set to 0 every time we executed the CM's SQL successfully.
	 * @return
	 */
	public int getSequentialTimeoutExceptionCount()
	{
		return _sequentialTimeoutExceptionCount;
	}

	/**
	 * get when was the first/oldest Timeout Exception happened.
	 * Note: This counter is set to 0 every time we executed the CM's SQL successfully.
	 * 
	 * @return -1 of no Timeout Exception happened, otherwise the value of 'System.currentTimeMillis()' when it happened
	 */
	public long getOldestTimeoutExceptionTime()
	{
		return _oldestTimeoutExceptionTime;
	}

	/**
	 * If you want the CounterSample to be locally sorted...
	 * @return How a List of SortOptions you want it to be sorted... or null if no local sorting
	 */
	public List<SortOptions> getLocalSortOptions()
	{
		return null;
	}


	/**
	 * This method is called <b>after</b> all CM's has been refreshed<br>
	 * So in here you can get data from other collectors that has been refreshed during this sample
	 * 
	 * @param refreshedCms a Map of the what CM's that were refreshed during this sample.
	 */
	public void doPostRefresh(LinkedHashMap<String, CountersModel> refreshedCms)
	{
	}


	public void dropTempTables(DbxConnection conn) // also implement addTempTable("")
	{
		for (String tabname : _dropTempTableList)
		{
			_logger.info("Trying to drop temporary table '"+tabname+"' after we have received an SQLException in refreshGetData()");

			try
			{
				Statement stmnt = conn.createStatement();
				stmnt.executeUpdate("drop table "+tabname);
				stmnt.close();
			}
			catch(SQLException e)
			{
				_logger.warn("Problems drop temporary table '"+tabname+"'. Msg="+e.getErrorCode()+", "+e.getMessage());
			}
		}
	}

	/**
	 * Add a tablename to be droped in case of we have any exceptions during refreshGetData()
	 * @param tabname
	 */
	public void addDropTempTable(String tabname)
	{
		_dropTempTableList.add(tabname);
	}

	/**
	 * Add a SQL Statement (for now just cmd's that do not return a ResultSet) to be executed in case of we have any exceptions during refreshGetData()
	 * @param tabname
	 */
	public void addCleanupCmdOnException(String cmd)
	{
		_cleanupCmdsOnExceptionList.add(cmd);
	}

	public void execCleanupCmdsOnException(DbxConnection conn) // also implement addTempTable("")
	{
		for (String cmd : _cleanupCmdsOnExceptionList)
		{
			_logger.info("Trying execute cmd '"+cmd+"' after we have received an SQLException in refreshGetData()");

			try
			{
				Statement stmnt = conn.createStatement();
				stmnt.executeUpdate(cmd);
				stmnt.close();
			}
			catch(SQLException e)
			{
				_logger.warn("Problems executing '"+cmd+"'. Msg="+e.getErrorCode()+", "+e.getMessage());
			}
		}
	}

	/**
	 * Compute the difference between two samples<br>
	 * NOTE: most of the code is the same in CounterModelHostMonitor.computeDiffCnt(): so if you change this, make sure you get the same logic in CounterModelHostMonitor
	 * 
	 * @param oldSample         Previous sample
	 * @param newSample         Previous sample
	 * @param deletedRows       if not null, a list of rowId's, that was part of oldSample but not part of newSample
	 * @param pkCols            not used for the moment
	 * @param isDiffCol         which columns should be difference calculated
	 * @param isCountersCleared If the counters has been cleared 
	 * @return
	 */
	public CounterSample computeDiffCnt(CounterSample oldSample, CounterSample newSample, List<Integer> deletedRows, List<Integer> newDeltaRows, List<String> pkCols, boolean[] isDiffCol, boolean isCountersCleared)
	{
//		return CounterSample.computeDiffCnt(prevSample, newSample, deletedRows, pkCols, isDiffCol, isCountersCleared);
		// Initialize result structure
		CounterSample diffCnt = new CounterSample(newSample, false, newSample._name+"-diff");

		long newTsMilli      = newSample.getSampleTime().getTime();
		long oldTsMilli      = oldSample.getSampleTime().getTime();
		int newTsNano        = newSample.getSampleTime().getNanos();
		int oldTsNano        = oldSample.getSampleTime().getNanos();

		// Check if TsMilli has really ms precision (not the case before JDK 1.4)
		if ((newTsMilli - (newTsMilli / 1000) * 1000) == newTsNano / 1000000)
			// JDK > 1.3.1
			diffCnt.setSampleInterval(newTsMilli - oldTsMilli);
		else
			diffCnt.setSampleInterval(newTsMilli - oldTsMilli + (newTsNano - oldTsNano) / 1000000);

		List<Object> newRow;
		List<Object> oldRow;
		List<Object> diffRow;

		// Special case, only one row for each sample, no key
		if ( ! diffCnt.hasPkCols() )
		{
			oldRow = oldSample.getRow(0);
			newRow = newSample.getRow(0);
			diffRow = new ArrayList<Object>();
			for (int i = 0; i < newSample.getColumnCount(); i++)
			{
				// This looks ugly: should really be done using the same logic as below... But this is really never used...
				diffRow.add(Integer.valueOf(((Integer) (newRow.get(i))).intValue() - ((Integer) (oldRow.get(i))).intValue()));
			}
			diffCnt.addRow(this, diffRow);
			return diffCnt;
		}

		// Keep a array of what rows that we access of the old values
		// this will help us find out what rows we "deleted" from the previous to the new sample
		// or actually rows that are no longer available in the new sample...
		boolean oldSampleAccessArr[] = new boolean[oldSample.getRowCount()]; // default values: false

		if (_isNewDeltaOrRateRow == null)
			_isNewDeltaOrRateRow = new boolean[newSample.getRowCount()+10]; // default values: false... add some extra fields...

		// Loop on all rows from the NEW sample
		for (int newRowId = 0; newRowId < newSample.getRowCount(); newRowId++)
		{
			newRow = newSample.getRow(newRowId);
			diffRow = new ArrayList<Object>(newRow.size());
			
			// get PK of the new row
			String newPk = newSample.getPkValue(newRowId);

			// Retrieve old same row
			int oldRowId = oldSample.getRowNumberForPkValue(newPk);
			
			// if old Row EXISTS, we can do diff calculation
			if (oldRowId != -1)
			{
				// Mark the row as "not deleted" / or "accessed"
				if (oldRowId >= 0 && oldRowId < oldSampleAccessArr.length)
					oldSampleAccessArr[oldRowId] = true;
				
				// Old row found, compute the diff's
				oldRow = oldSample.getRow(oldRowId);
				for (int i = 0; i < newSample.getColumnCount(); i++)
				{
					if ( ! isDiffCol[i] )
						diffRow.add(newRow.get(i));
					else
					{
						//checkType(oldSample, oldRowId, i, newSample, newRowId, i);
						//if ((newRow.get(i)).getClass().toString().equals("class java.math.BigDecimal"))
						Object oldRowObj = oldRow.get(i);
						Object newRowObj = newRow.get(i);

						String colName = newSample.getColumnName(i);

						if (newRowObj == null)
						{
							diffRow.add(null);
						}
						else if ( newRowObj instanceof Number )
						{
							Number diffValue = diffColumnValue((Number)oldRowObj, (Number)newRowObj, diffCnt.getNegativeDiffCountersToZero(), newSample.getName(), colName, isCountersCleared);
							diffRow.add(diffValue);
						}
						else
						{
							// Try to convert the String into a Number
							try
							{
								Number diffValue = diffColumnValue(NumberUtils.toNumber(oldRowObj), NumberUtils.toNumber(newRowObj), diffCnt.getNegativeDiffCountersToZero(), newSample.getName(), colName, isCountersCleared);
								diffRow.add(diffValue);
							}
							catch(NumberFormatException nfe)
							{
								_logger.warn("CounterSampleSetName='"+newSample._name+"', className='"+newRowObj.getClass().getName()+"' columns can't be 'diff' calculated. colName='"+colName+"', key='"+newPk+"', oldObj='"+oldRowObj+"', newObj='"+newRowObj+"'. Trying to convert it to a Number Caught: "+nfe);
								diffRow.add(newRowObj);
							}
								
//							_logger.warn("CounterSampleSetName='"+newSample._name+"', className='"+newRowObj.getClass().getName()+"' columns can't be 'diff' calculated. colName='"+colName+"', key='"+newPk+"', oldObj='"+oldRowObj+"', newObj='"+newRowObj+"'.");
//							diffRow.add(newRowObj);
						}
					}
				}

				setNewDeltaOrRateRow(newRowId, false);
			} // end: old row was found
			else
			{
				// Row was NOT found in previous sample, which means it's a "new" row for this sample.
				// So we do not need to do DIFF calculation, just add the raw data...
				for (int i = 0; i < newSample.getColumnCount(); i++)
				{
					diffRow.add(newRow.get(i));
				}
				
				if (newDeltaRows != null)
					newDeltaRows.add(newRowId);
				
				setNewDeltaOrRateRow(newRowId, true);
			}

			diffCnt.addRow(this, diffRow);

		} // end: row loop
		
		// What rows was DELETED from previous sample.
		// meaning, rows in the previous sample that was NOT part of the new sample.
		if (deletedRows != null)
		{
			for (int i=0; i<oldSampleAccessArr.length; i++)
			{
				if (oldSampleAccessArr[i] == false)
				{
					deletedRows.add(i);
				}
			}
		}

		return diffCnt;
	}

	/**
	 * Do difference calculations newColVal - prevColVal
	 * 
	 * @param prevColVal                 previous sample value
	 * @param newColVal                  current/new sample value
	 * @param negativeDiffCountersToZero if the counter is less than 0, reset it to 0
	 * @param counterSetName             Used as a prefix for messages
	 * @param isCountersCleared          if counters has been cleared
	 * 
	 * @return the difference of the correct subclass of Number
	 */
	protected Number diffColumnValue(Number prevColVal, Number newColVal, boolean negativeDiffCountersToZero, String counterSetName, String colName, boolean isCountersCleared)
	{
		Number diffColVal = null;
		
		if (newColVal == null)
			return null;

		if (prevColVal == null)
			return newColVal;

		if (newColVal instanceof BigDecimal)
		{
			// Get scale of the value so we can preserve that after the difference calculation.
			int scale = ((BigDecimal)newColVal).scale();
			
			diffColVal = new BigDecimal( newColVal.doubleValue() - prevColVal.doubleValue() ).setScale(scale, BigDecimal.ROUND_HALF_EVEN);
			if (diffColVal.doubleValue() < 0)
				if (negativeDiffCountersToZero)
					diffColVal = new BigDecimal(0);
		}
		else if (newColVal instanceof Byte)
		{
			diffColVal = Byte.valueOf((byte) (newColVal.byteValue() - prevColVal.byteValue()));
			if (diffColVal.intValue() < 0)
				if (negativeDiffCountersToZero)
					diffColVal = Byte.valueOf("0");
		}
		else if (newColVal instanceof Double)
		{
			diffColVal = Double.valueOf(newColVal.doubleValue() - prevColVal.doubleValue());
			if (diffColVal.doubleValue() < 0)
				if (negativeDiffCountersToZero)
					diffColVal = Double.valueOf(0);
		}
		else if (newColVal instanceof Float)
		{
			diffColVal = Float.valueOf(newColVal.floatValue() - prevColVal.floatValue());
			if (diffColVal.floatValue() < 0)
				if (negativeDiffCountersToZero)
					diffColVal = Float.valueOf(0);
		}
		else if (newColVal instanceof Integer)
		{
// Saving this code for future, the test shows that calculations is OK even with overflow counters...
// 1: either I miss something here
// 2: or Java handles this "auto-magically"
//			// Deal with counter counter overflows by calculating the: prevSample(numbers-up-to-INT-MAX-value) + newSample(negativeVal - INT-MIN)
//			if (newColVal.intValue() < 0 && prevColVal.intValue() >= 0)
//			{
////				// example prevColVal=2147483646, newColVal=-2147483647: The difference SHOULD BE: 3  
////				int restVal = Integer.MAX_VALUE - prevColVal.intValue(); // get changes up to the overflow: 2147483647 - 2147483646 == 7 
////				int overflv = newColVal.intValue() - Integer.MIN_VALUE;  // get changes after the overflow: -2147483647 - -2147483648 = 8
////				diffColVal = Integer.valueOf( restVal + overflv );
////System.out.println("restVal: "+restVal);
////System.out.println("overflv: "+overflv);
////System.out.println("=result: "+diffColVal);
//
//				// Or simplified (one-line)
////				diffColVal = Integer.valueOf( (Integer.MAX_VALUE - prevColVal.intValue()) + (newColVal.intValue() - Integer.MIN_VALUE) );
//
//				// Deal with counter overflows by bumping up to a higher data type: and adding the negative delta on top of Integer.MAX_VALUE -------*/ 
//				long newColVal_bumped = Integer.MAX_VALUE + (newColVal.longValue() - Integer.MIN_VALUE);
//				diffColVal = Integer.valueOf( (int) newColVal_bumped - prevColVal.intValue() );
//			}
//			else
//				diffColVal = Integer.valueOf(newColVal.intValue() - prevColVal.intValue());
			
			diffColVal = Integer.valueOf(newColVal.intValue() - prevColVal.intValue());
			if (diffColVal.intValue() < 0)
				if (negativeDiffCountersToZero)
					diffColVal = Integer.valueOf(0);
		}
		else if (newColVal instanceof Long)
		{
			diffColVal = Long.valueOf(newColVal.longValue() - prevColVal.longValue());
			if (diffColVal.longValue() < 0)
				if (negativeDiffCountersToZero)
					diffColVal = Long.valueOf(0);
		}
		else if (newColVal instanceof Short)
		{
			diffColVal = Short.valueOf((short) (newColVal.shortValue() - prevColVal.shortValue()));
			if (diffColVal.shortValue() < 0)
				if (negativeDiffCountersToZero)
					diffColVal = Short.valueOf("0");
		}
		else if (newColVal instanceof AtomicInteger)
		{
			diffColVal = new AtomicInteger(newColVal.intValue() - prevColVal.intValue());
			if (diffColVal.intValue() < 0)
				if (negativeDiffCountersToZero)
					diffColVal = new AtomicInteger(0);
		}
		else if (newColVal instanceof AtomicLong)
		{
			diffColVal = new AtomicLong(newColVal.longValue() - prevColVal.longValue());
			if (diffColVal.longValue() < 0)
				if (negativeDiffCountersToZero)
					diffColVal = new AtomicLong(0);
		}
		else
		{
			_logger.warn(counterSetName+": failure in diffColumnValue(colName='"+colName+"', prevColVal='"+prevColVal+"', newColVal='"+newColVal+"'), with prevColVal='"+prevColVal.getClass().getName()+"', newColVal='"+newColVal.getClass().getName()+"'. Returning the new value instead.");
			return newColVal;
		}

		return diffColVal;
	}


	/**
	 * Compute "rate" or increments per second
	 * <p>
	 * Use the difference calculated columns and divide it by the sample interval to achieve "rate per second" or "increments per second".
	 * 
	 * @param diffData  Data that already has been undergone difference calculations
	 * @param isDiffCol what counters is difference calculated ones, which means that we need to do "rate" calculation
	 * @param isPctCol  what counters is considered as percent calculated columns (do not do "rate" calculations on these)
	 * 
	 * @return a "rate" calculated object
	 */
	private CounterSample computeRatePerSec(CounterSample diffData, boolean[] isDiffCol, boolean[] isPctCol)
	{
//		return CounterSample.computeRatePerSec(diffData, isDiffCol, isPctCol);
		// Initialize result structure
		CounterSample rate  = new CounterSample(diffData, false, diffData._name+"-rate");

		int sampleInterval = diffData.getSampleInterval();
		
		// - Loop on all rows in the DIFF structure
		// - Do calculations on them
		// - And add them to the RATE structure
		for (int rowId=0; rowId < diffData.getRowCount(); rowId++) 
		{
			// Get the row from the DIFF structure
			List<Object> diffRow = diffData.getRow(rowId);

			// Create a new ROW "structure" for each row in the DIFF
			List<Object> newRow = new ArrayList<Object>();

			for (int i=0; i<diffData.getColumnCount(); i++) 
			{
				// Get the RAW object from the DIFF structure
				Object originObject = diffRow.get(i);

				// If the below IF statements is not true... keep the same object
				Object newObject    = originObject;

				// If PCT column DO nothing.
				if ( isPctCol[i] ) 
				{
				}
				// If this is a column that has DIFF calculation.
				else if ( isDiffCol[i] ) 
				{
					double val = 0;

					// What to do if we CANT DO DIVISION
					if (sampleInterval == 0)
						newObject = "N/A";

					if (originObject == null)
					{
						newObject = null;
					}
					// Calculate rate
					else if (originObject instanceof Number)
					{
						// Get the object as a Double value
						if ( originObject instanceof Number )
							val = ((Number)originObject).doubleValue();
						else
							val = Double.parseDouble( originObject.toString() );

						// interval is in MilliSec, so val has to be multiplied by 1000
						val = (val * 1000) / sampleInterval;
						BigDecimal newVal = new BigDecimal( val ).setScale(1, BigDecimal.ROUND_HALF_EVEN);

						// Set the new object
						newObject = newVal;
					}
					// Unsupported columns, skip the calculation
					else
					{
						String colName = diffData.getColumnName(i);
						_logger.warn("CounterSampleSetName='"+diffData._name+"', className='"+originObject.getClass().getName()+"' columns can't be 'rate' calculated. colName='"+colName+"', originObject='"+originObject+"', keeping this object.");
						newObject = originObject;
					}
				}

				// set the data in the new row
				newRow.add(newObject);

			} // end: row loop

			rate.addRow(this, newRow);

		} // end: all rows loop
		
		return rate;
	}

	/**
	 * Called when a timeout has been found in the refreshGetData() method
	 * <p>
	 * This method should be overridden by a CounterMoitor object
	 */
	public void handleTimeoutException()
	{
	}

	/**
	 * Request DDL information for first 10 rows
	 * 
	 * @param absData
	 * @param diffData
	 * @param rateData
	 */
	public void sendDdlDetailsRequest(CounterSample absData, CounterSample diffData, CounterSample rateData)
	{
//System.out.println("sendDdlDetailsRequest(): cm="+getName()+", absData="+absData+", diffData="+diffData+", rateData="+rateData);
		if ( ! PersistentCounterHandler.hasInstance() )
			return;
		if ( absData == null )
			return;

		// How MANY rows should we save
		int maxNumOfDdlsToPersist = getMaxNumOfDdlsToPersist();
		if (maxNumOfDdlsToPersist <= 0)
			return;

		PersistentCounterHandler pch = PersistentCounterHandler.getInstance();

		String[] colInfo = getDdlDetailsColNames();
		if ( colInfo == null || (colInfo != null && colInfo.length < 2) )
			throw new RuntimeException("getDdlDetailsColNames() must return a String array with a length of 2 or more. colInfoArr='"+StringUtil.toCommaStr(colInfo)+"'.");

		int DBName_pos     = absData.findColumn(colInfo[0]);
		int ObjectName_pos = absData.findColumn(colInfo[1]);

//System.out.println("sendDdlDetailsRequest(): cm="+getName()+", DBName_pos["+colInfo[0]+"]="+DBName_pos+", ObjectName_pos["+colInfo[1]+"]="+ObjectName_pos+".");
		if (DBName_pos == -1 || ObjectName_pos == -1)
			return;

		// Save first X rows from the Absolute values
//		int rows = Math.min(maxNumOfDdlsToPersist, absData.getRowCount());
		int rowsToSend = maxNumOfDdlsToPersist;
		int sentRows = 0;
		for (int r=0; r<absData.getRowCount(); r++)
		{
			Object DBName_obj     = absData.getValueAt(r, DBName_pos);
			Object ObjectName_obj = absData.getValueAt(r, ObjectName_pos);

			if (DBName_obj instanceof String && ObjectName_obj instanceof String)
			{
				if (sendDdlDetailsRequestForSpecificRow((String)DBName_obj, (String)ObjectName_obj, r, absData, diffData, rateData))
				{
					pch.addDdl((String)DBName_obj, (String)ObjectName_obj, getName()+".abs, row="+r);
					sentRows++;
					if (sentRows >= rowsToSend)
						break;
				}
			}
		}

		// From here on we need diffData to continue
		if (diffData == null)
			return;

		// No need to continue if all rows has already been added :)
		int rows = Math.min(maxNumOfDdlsToPersist, diffData.getRowCount());
		if (rows == diffData.getRowCount())
			return;

		// Here we can sort to TOP values in the rate/diff Data structures
		// This so we can collect what is the hottest tables/procs right now
		String sortDescOnColumns[] = getDdlDetailsSortOnColName();
		if (sortDescOnColumns == null)
			return;
		for (final String column : sortDescOnColumns)
		{
			final int colPos = diffData.findColumn(column);
			if (colPos == -1)
			{
				_logger.error("sendDdlDetailsRequest() cmName='"+diffData.getName()+"', sortDescOnColumns='"+StringUtil.toCommaStr(sortDescOnColumns)+"', but column '"+column+"' can't be found in diffValues (known cols="+StringUtil.toCommaStr(diffData.getColNames())+"), trying with next column.");
				continue;
			}

			//FIXME: the below can be done more efficient
			// instead of copyList + sort + takeFirt#Rows
			// do some kind of sort into a array holding only top X rows (some kind of bubble sort on the Top#Rows array)
			
			// Take a copy of the data, and sort it...
			ArrayList<List<Object>> sorted = new ArrayList<List<Object>>( diffData.getDataCollection() );
			Collections.sort(sorted,
				new Comparator<List<Object>>()
				{
					@Override
					public int compare(List<Object> o1, List<Object> o2)
					{
						Object objVal1 = o1.get(colPos);
						Object objVal2 = o2.get(colPos);
						
						if (objVal1 instanceof Number && objVal2 instanceof Number)
						{
							if ( ((Number)objVal1).doubleValue() < ((Number)objVal2).doubleValue() ) return 1;
							if ( ((Number)objVal1).doubleValue() > ((Number)objVal2).doubleValue() ) return -1;
							return 0;
						}
						_logger.warn("CM='"+getName()+"', NOT A NUMBER colName='"+column+"', colPos="+colPos+": objVal1="+objVal1.getClass().getName()+", objVal2="+objVal2.getClass().getName());
						return 0;
					}
				});

			// Now take first records
			for (int r=0; r<rows; r++)
			{
				Object DBName_obj     = sorted.get(r).get(DBName_pos);
				Object ObjectName_obj = sorted.get(r).get(ObjectName_pos);

				// discard diff values that are zero (this will/may happen when load is NOT HIGH)
				Object counter_obj = sorted.get(r).get(colPos);
				if (counter_obj instanceof Number)
				{
					Number counter = (Number)counter_obj;
					//if (counter.doubleValue() == 0.0)
					if (counter.intValue() == 0)
					{
						if (_logger.isDebugEnabled())
							_logger.debug("- - - - - - - - - CM("+getName()+").sendDdlDetailsRequest  - - - - - Skipping ZERO VALUE for dbname='"+DBName_obj+"', ObjectName='"+ObjectName_obj+"', column='"+column+"', row="+r+", counterObj="+counter_obj+", counter.intValue()="+counter.intValue());
						continue;
					}
				}
				else
				{
					continue; // Not a number so lets grab next...
				}

//Object sortOnCol_obj  = sorted.get(r).get(colPos);
//System.out.println("CM='"+getName()+"', DIFF TOP("+rows+") ROWS: "+column+" = "+sortOnCol_obj+", db='"+DBName_obj+"', objName='"+ObjectName_obj+"'.");

				if (DBName_obj instanceof String && ObjectName_obj instanceof String)
					pch.addDdl((String)DBName_obj, (String)ObjectName_obj, getName()+".diff.sortCol."+column+", row="+r);
			}
		}

//		// Here we can sort to TOP values in the rate/diff Data structures
//		// This so we can collect what is the hottest tables/procs right now
//		String sortDescOnColumns[] = getDdlDetailsSortOnColName();
//		for (String column : sortDescOnColumns)
//		{
//			int colPos = diffData.findColumn(column);
//			if (colPos == -1)
//			{
//				_logger.error("sendDdlDetailsRequest() sortDescOnColumns='"+StringUtil.toCommaStr(sortDescOnColumns)+"', but column '"+column+"' can't be found in diffValues, trying with next column.");
//				continue;
//			}
//
//			for (List<Object> row : data)
//			{
//				Object objVal = row.get(colPos);
//				if (objVal instanceof Number)
//				{
//				}
//			}
//		}

// SOME EXAMPLE OF BUBBLE SORT
//		int temp_vote;
//		int temp_candidate;
//		int[] candidate = new int[10];
//		int[] vote = new int[10];
//
//		for (int i=0; i<vote.length; i++)
//		{
//			for (int j=0; j<vote.length-1; j++)
//			{
//				if (vote[j] < vote[j+1])
//				{
//					temp_vote = vote[j];
//					vote[j]   = vote[j+1];
//					vote[j+1] = temp_vote;
//					
//					temp_candidate = candidate[j];
//      				candidate[j]   = candidate[j+1];
//       				candidate[j+1] = temp_candidate;
//				}
//			}
//		}
			
	} // end: method

	/**
	 * Override this to discard records from the save set
	 * 
	 * @param dBName
	 * @param objectName
	 * @param row
	 * @param absData
	 * @param diffData
	 * @param rateData
	 * 
	 * @return true if the row is to be sent to the DDL storage 
	 */
	public boolean sendDdlDetailsRequestForSpecificRow(String dBName, String objectName, int row, CounterSample absData, CounterSample diffData, CounterSample rateData)
	{
		return true;
	}




	/** 
	 * Get number of rows to save/request ddl information for 
	 * if 0 is return no lookup will be done.
	 */
	public int getMaxNumOfDdlsToPersist()
	{
		return 0;
	}

	/** 
	 * Get Column names to where DBName and ObjectName is called, this must always return at least a array with 2 strings. 
	 */
	public String[] getDdlDetailsColNames()
	{
		String[] sa = {"DBName", "ObjectName"};
		return sa;
	}
	/**
	 * Sort descending on this column(s) to get values from the diff/rate structure<br>
	 * One sort for every value will be done, meaning we can do "top" for more than 1 column<br>
	 * So if we want to do top 10 LogicalReads AND top 10 LockContention
	 * If this one returns null, this will not be done
	 * @return
	 */
	public String[] getDdlDetailsSortOnColName()
	{
		return null;
	}


	/**
	 * Get Options for how to store specific columns in the Persistence Counter Store 
	 * <p>
	 * In the returned Map we specify for example: If the column needs to be (dictionary) compressed... (if the back end DBMS support that)
	 * 
	 * @return a Map with ColumnNames and Options
	 */
	public Map<String, PcsColumnOptions> getPcsColumnOptions()
	{
		return _pcsColumnOptions;
	}

	private Map<String, PcsColumnOptions> _pcsColumnOptions = null;

	/**
	 * Set Options for how to store specific columns in the Persistence Counter Store 
	 * 
	 * @param map The new map of Column Options
	 * @return The old Column Option Map
	 */
	public Map<String, PcsColumnOptions> setPcsColumnOptions(Map<String, PcsColumnOptions> map)
	{
		Map<String, PcsColumnOptions> curMap = _pcsColumnOptions;
		
		_pcsColumnOptions = map;

		return curMap;
	}
	
	/**
	 * Check if a column is marked for Dictionary Compression
	 * @param colName
	 * @return
	 */
	public boolean isDictionaryCompressedColumn(String colName)
	{
		if ( ! DictCompression.isEnabled() )
			return false;

		Map<String, PcsColumnOptions> cmColOpt = getPcsColumnOptions();
		if (cmColOpt == null)   return false;
		if (cmColOpt.isEmpty()) return false;

		PcsColumnOptions colType = cmColOpt.get(colName);
		if (colType != null)
		{
			if (colType.isColumnType(PcsColumnOptions.ColumnType.DICTIONARY_COMPRESSION))
				return true;
		}
		return false;
	}

	/**
	 * Resolve a column name to a dictionary compressed column
	 * 
	 * @param colName The Original name
	 * @return null if not a dictionary compressed column, otherwise the "new" column name
	 */
	public String resolvDictionaryCompressedColumn(String colName)
	{
//		if ( ! DictCompression.hasInstance() )
//			throw new RuntimeException("Column name '" + colName + "' in CM '" + getName() + "' has DICTIONARY_COMPRESSION enabled, but no instance of DictCompression could be found.");

		if ( ! DictCompression.isEnabled() )
			return null;

		if (isDictionaryCompressedColumn(colName))
		{
			DictCompression dcc = DictCompression.getInstance();
			return dcc.getDigestSourceColumnName(colName);
		}
		return null;
	}
	

//	/**
//	 * Set Options for how to store specific columns in the Persistence Counter Store 
//	 * <p>
//	 * Here we specify for example: If the column needs to be (dictionary) compressed... (if the back end DBMS support that)
//	 * 
//	 * @return Any old mapping
//	 */
//	public Map<String, PcsColumnOptions> setPcsColumnOptions(Map<String, PcsColumnOptions> map)
//	{
//		return _pcsColumnOptions;
//	}
//
//	/** used by: getPcsColumnOptions() and setPcsColumnOptions() */
//	private Map<String, PcsColumnOptions> _pcsColumnOptions = null;


	/** list of SQL Warning to ignore during processing */
	private List<IgnoreSqlWarning> _ignoreSqlWarnings = new ArrayList<>();

	/**
	 * Set a list of SQL Warning to ignore during processing
	 */
	public void setIgnoreSqlWarnings(List<IgnoreSqlWarning> list)
	{
		_ignoreSqlWarnings = list;
	}

	/**
	 * @return a list (never null) which holds SQLWarning information to ignore during processing
	 */
	public List<IgnoreSqlWarning> getIgnoreSqlWarnings()
	{
		if (_ignoreSqlWarnings == null)
			return Collections.emptyList();

		return _ignoreSqlWarnings;
	}


	/**
	 * When reading Strings from the DBMS, should we "trim" the string (default is to only "right trim" them)
	 * @return true = do <code>str.trim()</code>   false=only-right-trim
	 */
	public boolean isStringTrimEnabled()
	{
		return false;
	}
	
	
	//----------------------------------------------------------------------------------------------------------
	// ALARM HANDLING
	//----------------------------------------------------------------------------------------------------------
	public static final String  PROPKEY_ALARM_isAlarmsEnabled = "<CMNAME>.alarm.enabled";
	public static final boolean DEFAULT_ALARM_isAlarmsEnabled = true;

	public static final String  PROPKEY_ALARM_isSystemAlarmsEnabled = "<CMNAME>.alarm.system.enabled";
	public static final boolean DEFAULT_ALARM_isSystemAlarmsEnabled = true;

	public static final String  PROPKEY_ALARM_isSystemAlarmsForColumnEnabled = "<CMNAME>.alarm.system.enabled.<COLNAME>";
	public static final boolean DEFAULT_ALARM_isSystemAlarmsForColumnEnabled = true;

	public static final String  PROPKEY_ALARM_isSystemAlarmsForColumnInTimeRange = "<CMNAME>.alarm.system.enabled.<COLNAME>.timeRange.cron";
	public static final String  DEFAULT_ALARM_isSystemAlarmsForColumnInTimeRange = "* * * * *";

	public static final String  PROPKEY_ALARM_isUserdefinedAlarmsEnabled = "<CMNAME>.alarm.userdefined.enabled";
	public static final boolean DEFAULT_ALARM_isUserdefinedAlarmsEnabled = true;

	/** Simply replace a tag &lt;CMNAME&gt; with the name of the CM */ 
	public String replaceCmName(String propKey)
	{
		return replaceCmName(getName(), propKey);
	}
	/** Simply replace a tag &lt;CMNAME&gt; with the name of the CM and &lt;COLNAME&gt; with the passed colname */ 
	public String replaceCmAndColName(String propKey, String colname)
	{
		return replaceCmAndColName(getName(), propKey, colname);
	}

//	/** Simply replace a tag &lt;CMNAME&gt; with the name of the CM */ 
//	public static String replaceCmName(CountersModel cm, String propKey)
//	{
//		return replaceCmName(cm.getName(), propKey);
//	}
//	/** Simply replace a tag &lt;CMNAME&gt; with the name of the CM and &lt;COLNAME&gt; with the passed colname */ 
//	public static String replaceCmAndColName(CountersModel cm, String propKey, String colname)
//	{
//		return replaceCmAndColName(cm.getName(), propKey, colname);
//	}

	/** Simply replace a tag &lt;CMNAME&gt; with the name of the CM */ 
	public static String replaceCmName(String cmName, String propKey)
	{
		return propKey.replace("<CMNAME>", cmName);
	}
	/** Simply replace a tag &lt;CMNAME&gt; with the name of the CM and &lt;COLNAME&gt; with the passed colname */ 
	public static String replaceCmAndColName(String cmName, String propKey, String colname)
	{
		return propKey.replace("<CMNAME>", cmName).replace("<COLNAME>", colname);
	}
	
	
	/**
	 * PRIVATE/LOCAL wrapper of sendAlarmRequest(...) so we can determine what to do...
	 */
//	protected void wrapperFor_sendAlarmRequest(CounterSample absData, CounterSample diffData, CounterSample rateData)
	public void wrapperFor_sendAlarmRequest()
	{
//		System.out.println("################################## wrapperFor_sendAlarmRequest(): cm='"+getName()+"'.");
		if ( ! isAlarmEnabled() )
			return;
		
		if (isAlarmsTemporaryDisabled())
			return;
		
		// Check here if we had a "timeout" and return... OR should we do it "somewhere else" or in some other way
		if (getSequentialTimeoutExceptionCount() > 0)
		{
			_logger.info("Skipping calling 'sendAlarmRequest()' for CM='" + getName() + "'. Reason: getSequentialTimeoutExceptionCount()==" + getSequentialTimeoutExceptionCount() + " is above ZERO.");
			return;
		}

		// Add a "message" to the AlarmHandler saying that THIS CM have undergone Alarm detection
		// So when endOfScan() is called, we can see WHAT CM's we can CANCEL alarms for
		// endOfScan() should NOT cancel alarms for CM's that we have NOT made any alarms checks for
		// For example if we would have (several) "timeout" on CM's the TimeToLive wont be trustworthy and then we will CANCEL alarms even if we shouldn't
		if (AlarmHandler.hasInstance())
		{
			AlarmHandler.getInstance().addUndergoneAlarmDetection(getName());
		}
		
		// Lets use the "built in" logic... which any CM/subclass has to implement :)
		if (hasSystemAlarms() && isSystemAlarmsEnabled()) 
		{
			// The alarmRequest() implementation could throw RuntimeExceptions etc... then we dont want to abort...
			try
			{
				sendAlarmRequest();
			}
			catch (Throwable t)
			{
				_logger.warn("Problems executing sendAlarmRequest() in CM '"+getName()+"'. This will be ignored, and monitoring will continue... Caught: "+t, t);
			}
		}

		//
		// The default behavior is to load a java source file, that is compiled "on the fly"<br>
		// The User Defined logic is implemented by the Java Source file
		// 
		if (hasUserDefinedAlarmInterrogator() && isUserDefinedAlarmsEnabled() )
		{
//			if ( ! UserDefinedAlarmHandler.hasInstance() )
//			return;
//
//			IUserDefinedAlarmInterrogator interrogator = UserDefinedAlarmHandler.getInstance().newClassInstance(this);
//			if (interrogator != null)
//			{
//				interrorgator.interrogateCounterData(this, absData, diffData, rateData);
//			}
		
			// FIXME: sendAlarmRequest_internal() should be called, which calls overridden/local sendAlarmRequest() where this/local should be empty
			// FIXME: The UserDefinedAlarmHander should also consider other loaded JAR files
			try
			{
//System.out.println("DEBUG: Create new object interrogator for "+getName());
//_udAlarmInterrorgator = createUserDefinedAlarmHandler(); // create a new object on every execution... this to check if the Java source file is re-compiled on changes
				getUserDefinedAlarmInterrogator().interrogateCounterData(this);
			}
			catch (Throwable t) 
			{
				_logger.warn("Problems executing User Defined Alarm Interrogater in CM '"+getName()+"'. This will be ignored, and monitoring will continue... Caught: "+t, t);
			}

			// or use: compile on the fly java and store class/bytecode in memory and execute it via reflection
			//         This is what I currently look at, or leaning towards...
			// Some examples:
			//      https://fivedots.coe.psu.ac.th/~ad/jg/javaArt1/onTheFlyArt1.pdf
			//      http://www.soulmachine.me/blog/2015/07/22/compile-and-run-java-source-code-in-memory/
			//      http://www.beyondlinux.com/2011/07/20/3-steps-to-dynamically-compile-instantiate-and-run-a-java-class/
			//      http://janino-compiler.github.io/janino/
			//      https://commons.apache.org/proper/commons-jci/usage.html
			//      
			
			// or use: LUA script language... http://www.luaj.org/luaj/3.0/README.html
			//         This might be an overkill

			// or use: JSR 223 calling various script languages (like JavaScript, Groovy or some other dynamic script language
			//         The problem with this seems to be that we need to "export" the CounterSample in some way...
			//   Groovy: http://groovy-lang.org/integrating.html
		}

		boolean simplePropertyBasedLogic = false;
		if (simplePropertyBasedLogic)
		{
			// Not yet implemented...
			// But we might do a simplified logic, like the one we have with UserDefinedCounter Graph...
			// we may use something like the "system alarms" properties:
			// ----------------------------------------------------------------------
			//    <CMNAME>.alarm.userdefined.enabled.<COLNAME>={true|false}
			//    <CMNAME>.alarm.userdefined.if.<COLNAME>.{eq|neq|gt|lt}=value
			//    <CMNAME>.alarm.userdefined.if.<COLNAME>.{eq|neq|gt|lt}.alarmEventClass=com.dbxtune.alarm.events.AlarmEventUserDefinedColumnWarning
			// ----------------------------------------------------------------------
			//
			/* ****************************************************************************************************
			 * BELOW IS ONLY AN EXAMPLE OF HOW THIS MIGHT BE IMPLEMENTED, the below is not correct and dosn't work
			 * ****************************************************************************************************
			public void createSimpleUsedDefinedColumnInterrogators()
			{
				// - Loop Configuration and get keys: <CMNAME>.alarm.userdefined.if.<COLNAME>.{eq|neq|gt|lt}=value
				// - create object SimpleUserDefinedColumnInterrogator(colname, op, value)
				// - Add it to a List<SimpleUserDefinedColumnInterrogator>
			}
			public void checkSimpleUsedDefinedColumnInterrogators()
			{
				for (SimpleUserDefinedColumnInterrogator si : _simpleUserDefinedInterrogatorList)
				{
					if (si.test(val))
						// Send alarm
				}
			}

			public static class SimpleUserDefinedColumnInterrogator
			{
				public enum Operator { EQ, NEQ, GT, LT };

				private String        _colname;
				private Operator      _op;
				private Comparable<?> _value;

				public SimpleUserDefinedColumnInterrogator(String colname, Operator op, Comparable<?> value)
				{
					_colname = colname;
					_op      = op;
					_value   = value;
				}
				
				public boolean test(Comparable<?> val)
				{
					switch (_op)
					{
					case EQ:  return   _value.equals(val);
					case NEQ: return ! _value.equals(val);
					case GT:  return _value.compareTo(val) < 0; // Note: this wont work, it's only an example
					case LT:  return _value.compareTo(val) > 0; // Note: this wont work, it's only an example
					}
				}
			}
			*/
		}
	}

	/**
	 * Initialize stuff that has to do with alarms<br>
	 * This is empty, and any CM's that need special code for this, can override this method
	 */
	public void initAlarms()
	{
	}

	/**
	 * Print how any alarms are configured
	 */
	public void printSystemAlarmConfig()
	{
		List<CmSettingsHelper> alarms = getLocalAlarmSettings();
		if (alarms.isEmpty())
		{
			_logger.info("System Defined Alarms are NOT enabled for '"+getName()+"'.");
			return;
		}

		Configuration conf = Configuration.getCombinedConfiguration();
		
		
		boolean isAlarmEnabled        = isAlarmEnabled();
		boolean isSystemAlarmsEnabled = isSystemAlarmsEnabled();
		String  prefix = "       ";
		
		_logger.info("System Defined Alarms properties are listed below for '"+getName()+"'.");

		_logger.info(prefix + replaceCmName(PROPKEY_ALARM_isAlarmsEnabled) + " = " + isAlarmEnabled + (isAlarmEnabled ? "" : "        ##### NOTE: all Alarms are DISABLED, so below propertirs wont be used."));
//		if ( ! isAlarmEnabled )
//			return;

		_logger.info(prefix + replaceCmName(PROPKEY_ALARM_isSystemAlarmsEnabled) + " = " + isSystemAlarmsEnabled + (isSystemAlarmsEnabled ? "" : "        ##### NOTE: System Alarms are DISABLED, so below propertirs wont be used."));
//		if ( ! isSystemAlarmsEnabled )
//			return;
		
		for (CmSettingsHelper sh : alarms)
		{
			String colname = sh.getName();
			
			// <CMNAME>.alarm.system.enabled.<COLNAME>
			// Only for names that do not contain ' ' spaces... FIXME: This is UGGLY, create a type/property which describes if we should write the '*.enable.*' property or not...
//			if ( sh.getName().indexOf(' ') == -1)
			if ( sh.isAlarmSwitch() )
			{
				String propName = replaceCmAndColName(getName(), PROPKEY_ALARM_isSystemAlarmsForColumnEnabled, colname);
				String propVal  = conf.getProperty(propName, DEFAULT_ALARM_isSystemAlarmsForColumnEnabled+"");

				// At what times can the alarms be triggered
				String cronProp = replaceCmAndColName(PROPKEY_ALARM_isSystemAlarmsForColumnInTimeRange, colname);
				String cronPat  = Configuration.getCombinedConfiguration().getProperty(cronProp, DEFAULT_ALARM_isSystemAlarmsForColumnInTimeRange);

				// Check that the 'cron' sceduling pattern is valid...
				String cronPatStr = cronPat;
				if (cronPatStr.startsWith("!"))
				{
					cronPatStr = cronPatStr.substring(1);
				}
				if ( ! SchedulingPattern.validate(cronPatStr) )
					_logger.error("The cron scheduling pattern '"+cronPatStr+"' is NOT VALID. for the property '"+replaceCmAndColName(PROPKEY_ALARM_isSystemAlarmsForColumnInTimeRange, colname)+"', this will not be used at runtime, furter warnings will also be issued.");

				String cronPatDesc = CronUtils.getCronExpressionDescriptionForAlarms(cronPat);

				_logger.info(prefix + propName + " = " + propVal);
				_logger.info(prefix + cronProp + " = " + cronPat + "   #-- desc: " + cronPatDesc);
				
			}
		}
		
		for (CmSettingsHelper sh : alarms)
		{
			// Various properties defined by the CounterModel
			// probably looks like: <CMNAME>.alarm.system.if.<COLNAME>.gt
			String propName = sh.getPropName();
			String propVal  = conf.getProperty(propName, sh.getDefaultValue());

			_logger.info(prefix + propName + " = " + propVal);
		}
	}

	/**
	 * Any CM that wants to send Alarm Requests somewhere should implement this
	 * 
	 * @param absData
	 * @param diffData
	 * @param rateData
	 */
//	public void sendAlarmRequest(CounterSample absData, CounterSample diffData, CounterSample rateData)
	public void sendAlarmRequest()
	{
		// empty implementation, any subclass can implement it.
	}

	/**
	 * If alarm handling is enabled or disabled for this specific cm<br>
	 * The idea is that Alarms could be enabled/disabled for specific cm's temporary during runtime (by the gui) 
	 */
	public boolean isAlarmEnabled()
	{
		return Configuration.getCombinedConfiguration().getBooleanProperty(replaceCmName(PROPKEY_ALARM_isAlarmsEnabled), DEFAULT_ALARM_isAlarmsEnabled);
	}

	/**
	 * For various reasons alarms can be temporarily disabled, until a specific time
	 */
	public boolean isAlarmsTemporaryDisabled()
	{
		// Negate this at the end (return) for easier logic
		boolean isAlarmEnabled = true;

		// Check if the alarm has been "temporarily" disabled
		if (_alarmsIsDisabledUntilTs > 0)
		{
			long timeDiffMs = TimeUtils.msDiffNow(_alarmsIsDisabledUntilTs);
			if (timeDiffMs >= 0)
			{
				_logger.info("For CM '" + getName() + "' the temporary Alarm Check Inhibition Time has passed. ENABLING the Alarm Checks again. (diabled-until-time was '" + TimeUtils.toStringYmdHms(_alarmsIsDisabledUntilTs) + "', which passed " + (timeDiffMs/1000) + " seconds ago)");
				_alarmsIsDisabledUntilTs = 0;
			}
			else
			{
				long timeDiffMsPos = timeDiffMs * -1;
				_logger.info("For CM '" + getName() + "' ALL Alarm Checks has been temporary disabled for " + TimeUtils.msToTimeStrDHMS(timeDiffMsPos) + " (HH:MM:SS), alarms will be re-enabled at '" + TimeUtils.toStringYmdHms(_alarmsIsDisabledUntilTs) + "'.");
				
				isAlarmEnabled = false;
			}
		}
		
		return ! isAlarmEnabled;
	}

	/**
	 * Sometimes you may want to disable alarms for X minutes (maybe at startup or similar)
	 * @param minutes
	 */
	public void setDisableAlarmsForXMinutes(int minutes)
	{
		setDisableAlarmsForXSeconds(minutes * 60);
	}

	/**
	 * Sometimes you may want to disable alarms for X seconds (maybe at startup or similar)
	 * @param minutes
	 */
	public void setDisableAlarmsForXSeconds(int seconds)
	{
		_alarmsIsDisabledUntilTs = System.currentTimeMillis() + (seconds * 1000);
	}
	/**
	 * Get the Time since epoch (in ms) until alarms for this CM is temporary disabled.
	 * @return 
	 * <ul>
	 *    <li> -1 == If it never has been set</li>
	 *    <li> 0  == Time has passed and was reset to 0 by isAlarmsTemporaryDisabled()</li>
	 *    <li> ## == The time (epoch in ms) until the "disable"/inhibition expires </li>
	 * </ul>
	 */
	public long getDisableAlarmsReEnableTime()
	{
		return _alarmsIsDisabledUntilTs;
	}

	/** Tells weather this CM has any SYSTEM Alarms defined */
	public boolean hasSystemAlarms()
	{
		return ! getLocalAlarmSettings().isEmpty();
	}

	/** Is SYSTEM Alarms enabled or disabled */
	public boolean isSystemAlarmsEnabled()
	{
		return hasSystemAlarms() && Configuration.getCombinedConfiguration().getBooleanProperty(replaceCmName(PROPKEY_ALARM_isSystemAlarmsEnabled), DEFAULT_ALARM_isSystemAlarmsEnabled);
	}

	/** Is SYSTEM Alarms enabled or disabled */
	public boolean isSystemAlarmsForColumnEnabled(String colname)
	{
		return Configuration.getCombinedConfiguration().getBooleanProperty(replaceCmAndColName(PROPKEY_ALARM_isSystemAlarmsForColumnEnabled, colname), DEFAULT_ALARM_isSystemAlarmsForColumnEnabled);
	}
	/** Is SYSTEM Alarms enabled or disabled */
	public boolean isSystemAlarmsForColumnEnabledAndInTimeRange(String colname)
	{
		boolean enabled = isSystemAlarmsForColumnEnabled(colname);
		if ( ! enabled )
			return enabled;

		String cronStr = Configuration.getCombinedConfiguration().getProperty(replaceCmAndColName(PROPKEY_ALARM_isSystemAlarmsForColumnInTimeRange, colname), DEFAULT_ALARM_isSystemAlarmsForColumnInTimeRange);

		if (DEFAULT_ALARM_isSystemAlarmsForColumnInTimeRange.equals(cronStr))
			return enabled;

		// is this a negation/not within time period...
		boolean negation = false;
		if (cronStr.startsWith("!"))
		{
			negation = true;
			cronStr  = cronStr.substring(1);
		}

		try
		{
			SchedulingPattern sp = new SchedulingPattern(cronStr);
			enabled = sp.match(System.currentTimeMillis());

			// if cron started with a '!' then invert the boolean
			if (negation)
				enabled = ! enabled;
		}
		catch(InvalidPatternException ex)
		{
			_logger.error("The specified 'cron' value '"+cronStr+"' for property '"+replaceCmAndColName(PROPKEY_ALARM_isSystemAlarmsForColumnInTimeRange, colname)+"' is not a valid cron-pattern. This will be disregarded. Caught: "+ex);
		}

		return enabled;
	}

	/** Is USER DEFINED Alarms enabled or disabled */
	public boolean isUserDefinedAlarmsEnabled()
	{
		return hasUserDefinedAlarmInterrogator() && Configuration.getCombinedConfiguration().getBooleanProperty(replaceCmName(PROPKEY_ALARM_isUserdefinedAlarmsEnabled), DEFAULT_ALARM_isUserdefinedAlarmsEnabled);
	}
	
	/**
	 * Create a Class that handler User Defined Alarm handling
	 * @return
	 */
	public IUserDefinedAlarmInterrogator createUserDefinedAlarmHandler()
	{
		//System.out.println("################################## createUserDefinedAlarmHandler(): cm='"+getName()+"'.");
		if ( ! UserDefinedAlarmHandler.hasInstance() )
			return null;

		IUserDefinedAlarmInterrogator inst = UserDefinedAlarmHandler.getInstance().newClassInstance(this);
		return inst;
	}
	/** Set a User Defined Alarm Interrogator */
	public void setUserDefinedAlarmInterrogator(IUserDefinedAlarmInterrogator interrogator)
	{
		_udAlarmInterrorgator = interrogator;
	}
	/** Get the User Defined Alarm Interrogator */
	public IUserDefinedAlarmInterrogator getUserDefinedAlarmInterrogator()
	{
		return _udAlarmInterrorgator;
	}
	/** Check if we have a User Defined Alarm Interrogator */
	public boolean hasUserDefinedAlarmInterrogator()
	{
		return _udAlarmInterrorgator != null;
	}

	/** Used by the: Create 'Offline Session' Wizard */
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		return Collections.emptyList();
//		return new ArrayList<CmSettingsHelper>();

		//--------------------------------------------------------------------------------------------
		// Example from ase.CmSummary:
		//--------------------------------------------------------------------------------------------
		// Configuration conf = Configuration.getCombinedConfiguration();
		// List<CmSettingsHelper> list = new ArrayList<>();
		// 
		// list.add(new CmSettingsHelper(PROPKEY_alarm_CPUTime             , Integer.class, conf.getIntProperty(PROPKEY_alarm_CPUTime             , DEFAULT_alarm_CPUTime             ), "If 'CPUTime' is greater than ## then send 'AlarmEventHighCpuUtilization'." ));
		// list.add(new CmSettingsHelper(PROPKEY_alarm_LockWaits           , Integer.class, conf.getIntProperty(PROPKEY_alarm_LockWaits           , DEFAULT_alarm_LockWaits           ), "If 'LockWaits' is greater than ## then send 'AlarmEventBlockingLockAlarm'." ));
		// list.add(new CmSettingsHelper(PROPKEY_alarm_oldestOpenTranInSec , Integer.class, conf.getIntProperty(PROPKEY_alarm_oldestOpenTranInSec , DEFAULT_alarm_oldestOpenTranInSec ), "If 'oldestOpenTranInSec' is greater than ## then send 'AlarmEventLongRunningTransaction'." ));
		// list.add(new CmSettingsHelper(PROPKEY_alarm_fullTranslogCount   , Integer.class, conf.getIntProperty(PROPKEY_alarm_fullTranslogCount   , DEFAULT_alarm_fullTranslogCount   ), "If 'fullTranslogCount' is greater than ## then send 'AlarmEventFullTranLog'." ));
        // 
		// return list;
		//--------------------------------------------------------------------------------------------
	}

	/**
	 * A simple wrapper method that calls <code>CmSettingsHelper.create_isAlarmEnabled(this, name)</code>
	 * 
	 * @param name
	 * @param cm
	 * @return
	 */
	public CmSettingsHelper createIsAlarmEnabled(String name, CountersModel cm)
	{
		return CmSettingsHelper.create_isAlarmEnabled(this, name);
	}

	/**
	 * A simple wrapper method that calls <code>CmSettingsHelper.create_timeRangeCron(this, name)</code>
	 * 
	 * @param name
	 * @param cm
	 * @return
	 */
	public CmSettingsHelper createTimeRangeCron(String name, CountersModel cm)
	{
		return CmSettingsHelper.create_timeRangeCron(this, name);
	}




	/**
	 * FIXME: Describe what this is used for
	 */
	public void clearForRead()
	{
		if (_prevSample != null) _prevSample.removeAllRows();
		if (_newSample  != null) _newSample .removeAllRows();
		if (_diffData   != null) _diffData  .removeAllRows();
		if (_rateData   != null) _rateData  .removeAllRows();

		_isNewDeltaOrRateRow = null;
	}

	public enum ClearOption
	{
		NORMAL,
		COLUMN_CHANGES
	};
	
	public void clear()
	{
		clear(ClearOption.NORMAL);
	}
	public synchronized void clear(ClearOption... clearOptions)
	{
		_prevSample       = null;
		_newSample        = null;
		_diffData         = null;
		_rateData         = null;

		setValidSampleData(false);
		setDataInitialized(false);
		setTimeInfo(null, null, null, 0);
//		selectedModelRow  = -1;

		// Handle options
		for (ClearOption clearOption : clearOptions)
		{
			if (ClearOption.NORMAL.equals(clearOption))
			{
				_dataSource = getDefaultDataSource();
			}

			// STRUCTURAL CHANGES
			if (ClearOption.COLUMN_CHANGES.equals(clearOption))
			{
				// The below is a BASIC version of reset()

				// basic stuff
				setInitialized(false);
				setRuntimeInitialized(false);

				_sqlRequest        = null;
				_sqlWhere          = "";
				_pkCols            = null;

				_isDiffCol   = null;   // this will be refreshed
				_isPctCol    = null;   // this will be refreshed
				_isDiffDissCol   = null;   // this will be refreshed

				_rsmdCached = null;
				
				// if this CM has valid data for last sample
				_hasValidSampleData = false;

				// more basic stuff
				_dataInitialized = false;
				_firstTimeSample = true;

				// new delta/rate row flag
				_isNewDeltaOrRateRow = null;
				
				// Aggregate
				private_resetAggregates();
			}
		}

		// Clear dates on panel
		if (_tabPanel != null)
		{
			_tabPanel.reset();
		}
		
		_pcsColumnOptions = null;

//		_refreshCounter = 0;

//		System.out.println(_name+":------doFireTableStructureChanged------");
		fireTableStructureChanged();
	}

	/**
	 * Reset statistical counters that will be sent to www.dbxtune.com<br>
	 * This is called from CheckForUpdates.sendCounterUsageInfo(), which will send of the statistics.
	 */
	public void resetStatCounters()
	{
		// incremented every time a refresh() happens, done by incRefreshCounter()
		_refreshCounter  = 0;

		// incremented every time a refresh() happens, summary of how many rows collected
		_sumRowCount     = 0;
		
//		_sumRowCountAbs  = 0;
//		_sumRowCountDiff = 0;
//		_sumRowCountRate = 0;
	}
	

	/**
	 * Reset all Counter "sampling" options that can be changed by this CM
	 * <p>
	 * If you want to change "specific" variables etc in the implementing CM, 
	 * then implement this method in that CM, but make sure you call <code>super.resetCounterOptionsToDefaults()</code>
	 */
	public void resetCounterOptionsToDefaults()
	{
		boolean save = true;

// Below is from: loadProps()
//---------------------------------------
//		setQueryTimeout(                     tempProps.getIntProperty(    base + PROPKEY_queryTimeout,                   getDefaultQueryTimeout()) );
//        
//		setDataSource(                       tempProps.getIntProperty(    base + PROPKEY_currentDataSource,              getDefaultDataSource())                         ,false);
//                                                                                                                                                                         
//		setFilterAllZero(                    tempProps.getBooleanProperty(base + PROPKEY_filterAllZeroDiffCounters,      getDefaultIsFilterAllZero())                    ,false);
//		setPauseDataPolling(                 tempProps.getBooleanProperty(base + PROPKEY_sampleDataIsPaused,             getDefaultIsDataPollingPaused())                ,false);
//		setBackgroundDataPollingEnabled(     tempProps.getBooleanProperty(base + PROPKEY_sampleDataInBackground,         getDefaultIsBackgroundDataPollingEnabled())     ,false);
//		setNegativeDiffCountersToZero(       tempProps.getBooleanProperty(base + PROPKEY_negativeDiffCountersToZero,     getDefaultIsNegativeDiffCountersToZero())       ,false);
//		setPersistCounters(                  tempProps.getBooleanProperty(base + PROPKEY_persistCounters,                getDefaultIsPersistCountersEnabled())           ,false);
//		setPersistCountersAbs(               tempProps.getBooleanProperty(base + PROPKEY_persistCounters_abs,            getDefaultIsPersistCountersAbsEnabled())        ,false);
//		setPersistCountersDiff(              tempProps.getBooleanProperty(base + PROPKEY_persistCounters_diff,           getDefaultIsPersistCountersDiffEnabled())       ,false);
//		setPersistCountersRate(              tempProps.getBooleanProperty(base + PROPKEY_persistCounters_rate,           getDefaultIsPersistCountersRateEnabled())       ,false);
//		setNonConfiguredMonitoringAllowed(   tempProps.getBooleanProperty(base + PROPKEY_nonConfiguredMonitoringAllowed, getDefaultIsNonConfiguredMonitoringAllowed())   ,false);
//		setNewDeltaOrRateRowHighlightEnabled(tempProps.getBooleanProperty(base + PROPKEY_highlightNewDateOrRateRows,     getDefaultIsNewDeltaOrRateRowHighlightEnabled()),false);
//
//		setPostponeTime(                     tempProps.getIntProperty    (base + PROPKEY_postponeTime,                   getDefaultPostponeTime())                       ,false);
//---------------------------------------

		setQueryTimeout(                      getDefaultQueryTimeout()                       , save);

		setDataSource(                        getDefaultDataSource()                         , save);

		setFilterAllZero(                     getDefaultIsFilterAllZero()                    , save);
		setPauseDataPolling(                  getDefaultIsDataPollingPaused()                , save);
		setBackgroundDataPollingEnabled(      getDefaultIsBackgroundDataPollingEnabled()     , save);
		setNegativeDiffCountersToZero(        getDefaultIsNegativeDiffCountersToZero()       , save);
		setPersistCounters(                   getDefaultIsPersistCountersEnabled()           , save);
		setPersistCountersAbs(                getDefaultIsPersistCountersAbsEnabled()        , save);
		setPersistCountersDiff(               getDefaultIsPersistCountersDiffEnabled()       , save);
		setPersistCountersRate(               getDefaultIsPersistCountersRateEnabled()       , save);
		setNonConfiguredMonitoringAllowed(    getDefaultIsNonConfiguredMonitoringAllowed()   , save);
		setNewDeltaOrRateRowHighlightEnabled( getDefaultIsNewDeltaOrRateRowHighlightEnabled(), save);
		
		setPostponeTime(                      getDefaultPostponeTime()                       , save);
		setPostponeIsEnabled(                 getDefaultPostponeIsEnabled()                  , save);

		
		// All Local options
		List<CmSettingsHelper> localSettings = getLocalSettings();
		if (localSettings != null)
		{
			Configuration readConf  = Configuration.getCombinedConfiguration();
			Configuration writeConf = Configuration.getInstance(Configuration.USER_TEMP);

			for (CmSettingsHelper cmSettingsHelper : localSettings)
			{
				String propName = cmSettingsHelper.getPropName();
				String defVal   = cmSettingsHelper.getDefaultValue();
				
				if (readConf.hasProperty(propName))
				{
					// If we DO NOT have the default value
					String currentValue = readConf.getProperty(propName, defVal);
					if ( ! currentValue.equals(defVal) )
					{
						String propVal = Configuration.USE_DEFAULT_PREFIX + defVal;
						writeConf.setProperty(propName, propVal);
System.out.println("CM='"+getName()+"': writeConf.setProperty(propName='" + propName + "', to: " + propVal);

						// We need to update CmPanel to refresh it's GUI components
						TabularCntrPanel tabPanel = getTabPanel();
						if (tabPanel != null)
						{
							tabPanel.refreshLocalOptionsPanel(propName, propVal);
						}
					}
				}
			}
		}
	}


	/**
	 * Get basic configuration "html" chars/tags is accepted.<br>
	 * This would be overrided by modules that does not support Properties by ShowCmPropertiesDialog
	 * @return
	 */
	public String getBasicConfigurationDescription()
	{
		return "This module '"+getDisplayName()+"' should have <code>ShowCmPropertiesDialog</code> availablity.";
	}

	
	
	public int getDataSource()
	{
		return _dataSource;
	}
	public void setDataSource(int dataSource, boolean saveProps)
	{
		if (dataSource != DATA_ABS && dataSource != DATA_DIFF && dataSource != DATA_RATE)
			throw new RuntimeException("Unknown dataView was specified. you specified dataView="+dataSource+". known values: DATA_ABS="+DATA_ABS+", DATA_DIFF="+DATA_DIFF+", DATA_RATE="+DATA_RATE+".");
		_dataSource = dataSource;

		// Update GUI's that are listening.
		fireTableDataChanged(); // does not do resort
		//fireTableStructureChanged(); // does resort, but: slow and makes GUI "hopp" (columns resize)
		if (getTabPanel() != null)
			getTabPanel().sortDatatable();

		if (saveProps)
			saveProps();
	}
	
	public CounterTableModel getCounterDataAbs()
	{
		return _newSample;
	}
	public CounterTableModel getCounterDataDiff()
	{
		return _diffData;
	}
	public CounterTableModel getCounterDataRate()
	{
		return _rateData;
	}

	public CounterSample getCounterSampleAbs()
	{
		return _newSample;
	}
	public CounterSample getCounterSampleDiff()
	{
		return _diffData;
	}
	public CounterSample getCounterSampleRate()
	{
		return _rateData;
	}

	public CounterTableModel getCounterData()
	{
		return getCounterData(getDataSource());
	}
	protected CounterTableModel getCounterData(int whatData)
	{
		CounterTableModel data = null;
	
		if      (whatData == DATA_ABS)  data = getCounterDataAbs();
		else if (whatData == DATA_DIFF) data = getCounterDataDiff();
		else if (whatData == DATA_RATE) data = getCounterDataRate();
		else
			throw new RuntimeException("Only ABS, DIFF, or RATE data is available.");
		return data;
	}

	public static String getWhatDataTranslationStr(int whatData)
	{
		if      (whatData == DATA_ABS)  return "DATA_ABS";
		else if (whatData == DATA_DIFF) return "DATA_DIFF";
		else if (whatData == DATA_RATE) return "DATA_RATE";
		else return "UNKNOWN DATA TYPE OF '"+whatData+"'.";
	}


	public int getRowCount(int type)
	{
		CounterTableModel ctm = getCounterData(type);
		if (ctm == null)
			return 0;
		return ctm.getRowCount();
	}
	public int getAbsRowCount()
	{
		CounterTableModel ctm = getCounterDataAbs();
		if (ctm == null)
			return 0;
		return ctm.getRowCount();
	}
	public int getDiffRowCount()
	{
		CounterTableModel ctm = getCounterDataDiff();
		if (ctm == null)
			return 0;
		return ctm.getRowCount();
	}
	public int getRateRowCount()
	{
		CounterTableModel ctm = getCounterDataRate();
		if (ctm == null)
			return 0;
		return ctm.getRowCount();
	}


	public int getColumnCount(int type)
	{
		CounterTableModel ctm = getCounterData(type);
		if (ctm == null)
			return 0;
		return ctm.getColumnCount();
	}
	public int getAbsColumnCount()
	{
		CounterTableModel ctm = getCounterDataAbs();
		if (ctm == null)
			return 0;
		return ctm.getColumnCount();
	}
	public int getDiffColumnCount()
	{
		CounterTableModel ctm = getCounterDataDiff();
		if (ctm == null)
			return 0;
		return ctm.getColumnCount();
	}
	public int getRateColumnCount()
	{
		CounterTableModel ctm = getCounterDataRate();
		if (ctm == null)
			return 0;
		return ctm.getColumnCount();
	}


	public boolean hasData()
	{
		int totalRows = 0;

		totalRows += getAbsRowCount();
		totalRows += getDiffRowCount();
		totalRows += getRateRowCount();
		
		return totalRows > 0;
	}

	public boolean hasAbsData()
	{
		return getCounterDataAbs() != null;
	}

	public boolean hasDiffData()
	{
		return getCounterDataDiff() != null;
	}

	public boolean hasRateData()
	{
		return getCounterDataRate() != null;
	}
//	public boolean hasAbsData()
//	{
//		CounterTableModel ctm = getCounterDataAbs();
//		if (ctm == null)
//			return false;
//		return ctm.getRowCount() > 0;
//	}
//
//	public boolean hasDiffData()
//	{
//		CounterTableModel ctm = getCounterDataDiff();
//		if (ctm == null)
//			return false;
//		return ctm.getRowCount() > 0;
//	}
//
//	public boolean hasRateData()
//	{
//		CounterTableModel ctm = getCounterDataRate();
//		if (ctm == null)
//			return false;
//		return ctm.getRowCount() > 0;
//	}

	/** this should return true if the current sample has any data to show */
	public boolean hasValidSampleData()
	{
		return _hasValidSampleData;
	}
	/** set this to true if the tab has valid sample data for current sample period */
	public void setValidSampleData(boolean valid)
	{
		_hasValidSampleData = valid;
	}

//	public Timestamp getTimestamp()
//	{
//		return (_newSample == null) ? null : _newSample.getSampleTime();
//
////		if (_diffData == null)
////			return null;
////		return _diffData.samplingTime;
//	}
	public Timestamp getTimestamp()
	{
		Timestamp ts = null;

		if (_newSample != null) 
			ts = _newSample.getSampleTime();

		// use fallback here, since: _newSample is not maintained by CounterModelHostMonitor and possibly others.
		if (ts == null)
			ts = getSampleTime();

		if (ts == null)
		{
			Exception dummyEx = new Exception("Dummy Exception, for for traceability/callstack... or from where the method was called.");
			_logger.warn("getTimestamp(): returns null. This is probably not anticipated, appending a stacktrace...", dummyEx);
		}

		return ts;
	}

//	public int getLastSampleInterval()
//	{
//		return (_newSample == null) ? 0 : _newSample.getSampleInterval();
//
////		if (_diffData != null)
////			return (int) _diffData.interval;
////		return 0;
//	}
	public int getLastSampleInterval()
	{
		int interval = 0;

		if (_newSample != null) 
			interval = _newSample.getSampleInterval();

		// use fallback here, since: _newSample is not maintained by CounterModelHostMonitor and possibly others.
		if (interval == 0)
			interval = (int) getSampleInterval();

		return interval;
	}
	
	// Return number of rows in the diff table
	public synchronized int size()
	{
		return (_diffData == null) ? 0 : _diffData.getRowCount();
	}

	//---------------------------------------------------------------------------
	// Double
	//---------------------------------------------------------------------------
	private Double getValueAsDouble(int whatData, int rowId, int colPos)
	{
		return getValueAsDouble(whatData, rowId, colPos, null);
	}

	// 
	private synchronized Double getValueAsDouble(int whatData, int rowId, int colPos, Double def)
	{
		Object o = getValue(whatData, rowId, colPos);
		if (o == null)
			return def;

		if (o instanceof Number)
			return Double.valueOf(((Number) o).doubleValue());
		else
			return Double.valueOf(Double.parseDouble(o.toString()));
	}

	// 
	private Double getValueAsDouble(int whatData, int rowId, String colname, boolean caseSensitive)
	{
		return getValueAsDouble(whatData, rowId, colname, caseSensitive, null);
	}

	// 
	private synchronized Double getValueAsDouble(int whatData, int rowId, String colname, boolean caseSensitive, Double def)
	{
		Object o = getValue(whatData, rowId, colname, caseSensitive);
		if (o == null)
			return def;

		if (o instanceof Number)
			return Double.valueOf(((Number) o).doubleValue());
		else
			return Double.valueOf(Double.parseDouble(o.toString()));
	}

	private Double getValueAsDouble(int whatData, String pkStr, String colname, boolean caseSensitive)
	{
		return getValueAsDouble(whatData, pkStr, colname, caseSensitive, null);
	}

	// 
	private synchronized Double getValueAsDouble(int whatData, String pkStr, String colname, boolean caseSensitive, Double def)
	{
		Object o = getValue(whatData, pkStr, colname, caseSensitive);
		if (o == null)
			return def;

		if (o instanceof Number)
			return Double.valueOf(((Number) o).doubleValue());
		else
			return Double.valueOf(Double.parseDouble(o.toString()));
	}

	//---------------------------------------------------------------------------
	// Integer
	//---------------------------------------------------------------------------
	private Integer getValueAsInteger(int whatData, int rowId, int colPos)
	{
		return getValueAsInteger(whatData, rowId, colPos, null);
	}

	// 
	private synchronized Integer getValueAsInteger(int whatData, int rowId, int colPos, Integer def)
	{
		Object o = getValue(whatData, rowId, colPos);
		if (o == null)
			return def;

		if (o instanceof Number)
			return Integer.valueOf(((Number) o).intValue());
		else
			return Integer.valueOf(Integer.parseInt(o.toString()));
	}

	// 
	private Integer getValueAsInteger(int whatData, int rowId, String colname, boolean caseSensitive)
	{
		return getValueAsInteger(whatData, rowId, colname, caseSensitive, null);
	}

	// 
	private synchronized Integer getValueAsInteger(int whatData, int rowId, String colname, boolean caseSensitive, Integer def)
	{
		Object o = getValue(whatData, rowId, colname, caseSensitive);
		if (o == null)
			return def;

		if (o instanceof Number)
			return Integer.valueOf(((Number) o).intValue());
		else
			return Integer.valueOf(Integer.parseInt(o.toString()));
	}

	private Integer getValueAsInteger(int whatData, String pkStr, String colname, boolean caseSensitive)
	{
		return getValueAsInteger(whatData, pkStr, colname, caseSensitive, null);
	}

	// 
	private synchronized Integer getValueAsInteger(int whatData, String pkStr, String colname, boolean caseSensitive, Integer def)
	{
		Object o = getValue(whatData, pkStr, colname, caseSensitive);
		if (o == null)
			return def;

		if (o instanceof Number)
			return Integer.valueOf(((Number) o).intValue());
		else
			return Integer.valueOf(Integer.parseInt(o.toString()));
	}

	//---------------------------------------------------------------------------
	// Long
	//---------------------------------------------------------------------------
	private Long getValueAsLong(int whatData, int rowId, int colPos)
	{
		return getValueAsLong(whatData, rowId, colPos, null);
	}

	// 
	private synchronized Long getValueAsLong(int whatData, int rowId, int colPos, Long def)
	{
		Object o = getValue(whatData, rowId, colPos);
		if (o == null)
			return def;

		if (o instanceof Number)
			return Long.valueOf(((Number) o).longValue());
		else
			return Long.valueOf(Long.parseLong(o.toString()));
	}

	// 
	private Long getValueAsLong(int whatData, int rowId, String colname, boolean caseSensitive)
	{
		return getValueAsLong(whatData, rowId, colname, caseSensitive, null);
	}

	// 
	private synchronized Long getValueAsLong(int whatData, int rowId, String colname, boolean caseSensitive, Long def)
	{
		Object o = getValue(whatData, rowId, colname, caseSensitive);
		if (o == null)
			return def;

		if (o instanceof Number)
			return Long.valueOf(((Number) o).longValue());
		else
			return Long.valueOf(Long.parseLong(o.toString()));
	}

	private Long getValueAsLong(int whatData, String pkStr, String colname, boolean caseSensitive)
	{
		return getValueAsLong(whatData, pkStr, colname, caseSensitive, null);
	}

	// 
	private synchronized Long getValueAsLong(int whatData, String pkStr, String colname, boolean caseSensitive, Long def)
	{
		Object o = getValue(whatData, pkStr, colname, caseSensitive);
		if (o == null)
			return def;

		if (o instanceof Number)
			return Long.valueOf(((Number) o).longValue());
		else
			return Long.valueOf(Long.parseLong(o.toString()));
	}

	//---------------------------------------------------------------------------
	// Timestamp
	//---------------------------------------------------------------------------
	private Timestamp getValueAsTimestamp(int whatData, int rowId, int colPos)
	{
		return getValueAsTimestamp(whatData, rowId, colPos, null);
	}

	// 
	private synchronized Timestamp getValueAsTimestamp(int whatData, int rowId, int colPos, Timestamp def)
	{
		Object o = getValue(whatData, rowId, colPos);
		if (o == null)
			return def;

		if (o instanceof Timestamp)
			return (Timestamp) o;
		else
		{
			try
			{
				SimpleDateFormat sdf = new SimpleDateFormat();
				java.util.Date date = sdf.parse(o.toString());
				return new Timestamp(date.getTime());
			}
			catch(ParseException e)
			{
				try	{ return TimeUtils.parseToTimestampX(o.toString());	}
				catch(ParseException e2) {}
				
				_logger.warn("Problem reading Timestamp value for: whatData=" + whatData + ", rowId=" + rowId + ", colPos='" + colPos + ", value='" + o + "', returning DEFAULT='" + def + "'. Caught: " + e);
				return def;
			}
		}
	}

	// 
	private Timestamp getValueAsTimestamp(int whatData, int rowId, String colname, boolean caseSensitive)
	{
		return getValueAsTimestamp(whatData, rowId, colname, caseSensitive, null);
	}

	// 
	private synchronized Timestamp getValueAsTimestamp(int whatData, int rowId, String colname, boolean caseSensitive, Timestamp def)
	{
		Object o = getValue(whatData, rowId, colname, caseSensitive);
		if (o == null)
			return def;

		if (o instanceof Timestamp)
			return (Timestamp) o;
		else
		{
			try
			{
				SimpleDateFormat sdf = new SimpleDateFormat();
				java.util.Date date = sdf.parse(o.toString());
				return new Timestamp(date.getTime());
			}
			catch(ParseException e)
			{
				try	{ return TimeUtils.parseToTimestampX(o.toString());	}
				catch(ParseException e2) {}
				
				_logger.warn("Problem reading Timestamp value for: whatData=" + whatData + ", rowId=" + rowId + ", colname='" + colname + ", caseSensitive=" + caseSensitive + ", value='" + o + "', returning DEFAULT='" + def + "'. Caught: " + e);
				return def;
			}
		}
	}

	private Timestamp getValueAsTimestamp(int whatData, String pkStr, String colname, boolean caseSensitive)
	{
		return getValueAsTimestamp(whatData, pkStr, colname, caseSensitive, null);
	}

	// 
	private synchronized Timestamp getValueAsTimestamp(int whatData, String pkStr, String colname, boolean caseSensitive, Timestamp def)
	{
		Object o = getValue(whatData, pkStr, colname, caseSensitive);
		if (o == null)
			return def;

		if (o instanceof Timestamp)
			return (Timestamp) o;
		else
		{
			try
			{
				SimpleDateFormat sdf = new SimpleDateFormat();
				java.util.Date date = sdf.parse(o.toString());
				return new Timestamp(date.getTime());
			}
			catch(ParseException e)
			{
				try	{ return TimeUtils.parseToTimestampX(o.toString());	}
				catch(ParseException e2) {}
				
				_logger.warn("Problem reading Timestamp value for: whatData=" + whatData + ", pkStr=" + pkStr + ", colname='" + colname + ", caseSensitive=" + caseSensitive + ", value='" + o + "', returning DEFAULT='" + def + "'. Caught: " + e);
				return def;
			}
		}
	}


	//---------------------------------------------------------------------------
	// Value
	//---------------------------------------------------------------------------
	// Return the value of a cell by ROWID (rowId, ColumnName)
	protected synchronized Object getValue(int whatData, int rowId, String colname, boolean caseSensitive)
	{
		return getValue(whatData, rowId, colname, caseSensitive, null);
	}

	// Return the value of a cell by ROWID (rowId, ColumnName)
	private synchronized Object getValue(int whatData, int rowId, String colname, boolean caseSensitive, Object def)
	{
		CounterTableModel data = null;

		if      (whatData == DATA_ABS)  data = getCounterDataAbs();
		else if (whatData == DATA_DIFF) data = getCounterDataDiff();
		else if (whatData == DATA_RATE) data = getCounterDataRate();
		else
			throw new RuntimeException("Only ABS, DIFF, or RATE data is available.");

		if (data == null)
			return def;

		int idCol = data.findColumn(colname, caseSensitive);
		if (idCol == -1)
		{
			if (_logger.isDebugEnabled()) 
				_logger.debug("getValue: Can't find the column '" + colname + "'.");
			return null;
		}
		if (data.getRowCount() <= rowId)
			return null;

		return data.getValueAt(rowId, idCol);
	}

	// Return the value of a cell by ROWID (rowId, ColumnName)
	private Object getValue(int whatData, int rowId, int colId)
	{
		return getValue(whatData, rowId, colId, null);
	}

	// Return the value of a cell by ROWID (rowId, ColumnName)
	private synchronized Object getValue(int whatData, int rowId, int colId, Object def)
	{
		CounterTableModel data = null;

		if      (whatData == DATA_ABS)  data = getCounterDataAbs();
		else if (whatData == DATA_DIFF) data = getCounterDataDiff();
		else if (whatData == DATA_RATE) data = getCounterDataRate();
		else
			throw new RuntimeException("Only ABS, DIFF, or RATE data is available.");

		if (data == null)
			return def;

		if (colId < 0 || colId > data.getColumnCount())
		{
			if (_logger.isDebugEnabled()) 
				_logger.debug("getValue: column id " + colId + " is out of range. column Count is "+data.getColumnCount());
			return null;
		}
		if (data.getRowCount() <= rowId)
			return null;

		return data.getValueAt(rowId, colId);
	}

	// 
	private Object getValue(int whatData, String pkStr, String colname, boolean caseSensitive)
	{
		return getValue(whatData, pkStr, colname, caseSensitive, null);
	}
	/**
	 *  Return the value of a cell by ROWID (rowId, colId)
	 *  rowId starts at 0
	 *  colId starts at 
	 *  NOTE: note tested (2007-07-13)
	 */
	// Return the value of a cell by keyVal, (keyVal, ColumnName)
//	private synchronized Double getValue(int whatData, String pkStr, String colname)
	private synchronized Object getValue(int whatData, String pkStr, String colname, boolean caseSensitive, Object def)
	{
		CounterTableModel data = null;

		if      (whatData == DATA_ABS)  data = getCounterDataAbs();
		else if (whatData == DATA_DIFF) data = getCounterDataDiff();
		else if (whatData == DATA_RATE) data = getCounterDataRate();
		else
			throw new RuntimeException("Only ABS, DIFF, or RATE data is available.");

		if (data == null)
		{
			if (_logger.isDebugEnabled()) 
				_logger.debug(getName()+".getValue(whatData="+getWhatDataTranslationStr(whatData)+", pkStr='"+pkStr+"', colname='"+colname+"'): data==null; return null");
			return def;
		}

		// Get the rowId, if not found, return null
		int rowId = data.getRowNumberForPkValue(pkStr);
		if (rowId < 0)
		{
			if (_logger.isDebugEnabled())
				_logger.debug(getName()+".getValue(whatData="+getWhatDataTranslationStr(whatData)+", pkStr='"+pkStr+"', colname='"+colname+"'): rowId="+rowId+": rowId < 0; return null");
			return def;
		}

		// Got object for the RowID and column name
		Object o = getValue(whatData, rowId, colname, caseSensitive);
		if (o == null)
		{
			if (_logger.isDebugEnabled()) 
				_logger.debug(getName()+".getValue(whatData="+getWhatDataTranslationStr(whatData)+", pkStr='"+pkStr+"', colname='"+colname+"'): rowId="+rowId+": o==null; return null");
			return def;
		}

//		if (o instanceof Double)
//			return (Double) o;
//		else
//			return Double.valueOf(o.toString());
		return o;
	}
	/**
	 * Get a int array of rows where colValue matches values in the column name
	 * @param whatData DATA_ABS or DATA_DIFF or DATA_RATE
	 * @param colname Name of the column to search in
	 * @param colvalue Value to search for in the above column name
	 * @return int[] an array of integers where rows meets above search criteria, if nothing was found return null
	 */
	private int[] getRowIdsWhere(int whatData, String colname, boolean caseSensitive, Object colvalue)
	{
		CounterTableModel data = null;

		if      (whatData == DATA_ABS)  data = getCounterDataAbs();
		else if (whatData == DATA_DIFF) data = getCounterDataDiff();
		else if (whatData == DATA_RATE) data = getCounterDataRate();
		else
			throw new RuntimeException("Only ABS, DIFF, or RATE data is available.");

		if (data == null)
			return null;

//		int idCol = data.getColNames().indexOf(colname);
		int idCol = data.findColumn(colname, caseSensitive);
		if (idCol == -1)
		{
			_logger.info("getRowIdsWhere: Can't find the column '" + colname + "'.");
			return null;
		}
		if (data.getRowCount() == 0)
			return null;

		ArrayList<Integer> rowsList = new ArrayList<Integer>();
		for (int i = 0; i < data.getRowCount(); i++)
		{
			Object o = data.getValueAt(i, idCol);
			if (o == null)
				continue;

			if (o instanceof Number && colvalue instanceof Number)
			{
				if (NumberUtils.compareValues((Number)o, (Number)colvalue))
				{
					rowsList.add(i);
				}
			}
			else if (o.equals(colvalue))
			{
				rowsList.add(i);
			}
		}
		if (rowsList.isEmpty())
			return null;

		// Convert the list into a array
		int ia[] = new int[rowsList.size()];
		for (int i=0; i<rowsList.size(); i++)
			ia[i] = rowsList.get(i);

		return ia;
	}

	/** If we want to discard Aggregated rows in methods: getMaxValue() */
	public boolean discardAggregatedRowFromMax()
	{
		return true;
	}

	/** If we want to discard Aggregated rows in methods: getMinValue() */
	public boolean discardAggregatedRowFromMin()
	{
		return true;
	}

	/** If we want to discard Aggregated rows in methods: getSumValue() */
	public boolean discardAggregatedRowFromSumAndAvg()
	{
		return true;
	}

	/** If we want to discard Aggregated rows in methods: getCountGtZero() */
	public boolean discardAggregatedRowFromCountGtZero()
	{
		return true;
	}
	
//	/** If we want to discard Aggregated rows in methods: getAvgValue() */
//	public boolean discardAggregatedRowFromAvg()
//	{
//		return true;
//	}

//	/** If we want to discard Aggregated rows in methods: getAvgValueGtZero() */
//	public boolean discardAggregatedRowFromAvgGtZero()
//	{
//		return true;
//	}

	private synchronized Double getMaxValue(int whatData, int[] rowIds, String colname, boolean caseSensitive)
	{
		CounterTableModel data = null;

		if      (whatData == DATA_ABS)  data = getCounterDataAbs();
		else if (whatData == DATA_DIFF) data = getCounterDataDiff();
		else if (whatData == DATA_RATE) data = getCounterDataRate();
		else
			throw new RuntimeException("Only ABS, DIFF, or RATE data is available.");

		if (data == null)
			return null;

		int colPos = data.findColumn(colname, caseSensitive);
		if (colPos == -1)
		{
			_logger.info("getMaxValue: Can't find the column '" + colname + "'.");
			return null;
		}
		return getMaxValue(whatData, rowIds, colPos);
	}
	// Return the MAX of the values of a column (ColumnName)
	private synchronized Double getMaxValue(int whatData, int[] rowIds, int colPos)
	{
		CounterTableModel data = null;

		if      (whatData == DATA_ABS)  data = getCounterDataAbs();
		else if (whatData == DATA_DIFF) data = getCounterDataDiff();
		else if (whatData == DATA_RATE) data = getCounterDataRate();
		else
			throw new RuntimeException("Only ABS, DIFF, or RATE data is available.");

		if (data == null)
			return null;

		if (colPos < 0 || colPos > data.getColumnCount())
		{
			if (_logger.isDebugEnabled()) 
				_logger.debug("getMaxValue: column pos " + colPos + " is out of range. column Count is "+data.getColumnCount());
			return null;
		}
		if (data.getRowCount() == 0)
			return null;

		double maxResult = 0;
		double result = 0;

		int count = data.getRowCount();
		if (rowIds != null)
			count = rowIds.length;

		for (int i = 0; i < count; i++)
		{
			int rowId = i;
			if (rowIds != null)
				rowId = rowIds[i];

			// If the row is an Aggregated row... skip this
			if (discardAggregatedRowFromMax() && isAggregateRow(rowId))
				continue;
			
			Object o = data.getValueAt(rowId, colPos);
			if (o == null)
				continue;

			if (o instanceof Number)
			{
				if (_logger.isDebugEnabled()) 
					_logger.debug("Colname='" + data.getColumnName(colPos) + "', Number: " + ((Number) o).doubleValue());
				result = ((Number) o).doubleValue();
			}
			else
			{
				if (_logger.isDebugEnabled()) 
					_logger.debug("Colname='" + data.getColumnName(colPos) + "', toString(): " + o.toString());
				result = Double.parseDouble(o.toString());
			}

			if (result > maxResult)
			{
				maxResult = result;
			}
		}
		return Double.valueOf(maxResult);
	}

	// Return the MIN of the values of a column (ColumnName)
	private synchronized Double getMinValue(int whatData, int[] rowIds, String colname, boolean caseSensitive)
	{
		CounterTableModel data = null;

		if      (whatData == DATA_ABS)  data = getCounterDataAbs();
		else if (whatData == DATA_DIFF) data = getCounterDataDiff();
		else if (whatData == DATA_RATE) data = getCounterDataRate();
		else
			throw new RuntimeException("Only ABS, DIFF, or RATE data is available.");

		if (data == null)
			return null;

		int colPos = data.findColumn(colname, caseSensitive);
		if (colPos == -1)
		{
			_logger.info("getMinValue: Can't find the column '" + colname + "'.");
			return null;
		}
		return getMinValue(whatData, rowIds, colPos);
	}
	// Return the MIN of the values of a column (ColumnName)
	private synchronized Double getMinValue(int whatData, int[] rowIds, int colPos)
	{
		CounterTableModel data = null;

		if      (whatData == DATA_ABS)  data = getCounterDataAbs();
		else if (whatData == DATA_DIFF) data = getCounterDataDiff();
		else if (whatData == DATA_RATE) data = getCounterDataRate();
		else
			throw new RuntimeException("Only ABS, DIFF, or RATE data is available.");

		if (data == null)
			return null;

		if (colPos < 0 || colPos > data.getColumnCount())
		{
			if (_logger.isDebugEnabled()) 
				_logger.debug("getMinValue: column pos " + colPos + " is out of range. column Count is "+data.getColumnCount());
			return null;
		}
		if (data.getRowCount() == 0)
			return null;

		double minResult = 0;
		double result = 0;

		int count = data.getRowCount();
		if (rowIds != null)
			count = rowIds.length;

		for (int i = 0; i < count; i++)
		{
			int rowId = i;
			if (rowIds != null)
				rowId = rowIds[i];

			// If the row is an Aggregated row... skip this
			if (discardAggregatedRowFromMin() && isAggregateRow(rowId))
				continue;
			
			Object o = data.getValueAt(rowId, colPos);
			if (o == null)
				continue;

			if (o instanceof Number)
			{
				if (_logger.isDebugEnabled()) 
					_logger.debug("Colname='" + data.getColumnName(colPos) + "', Number: " + ((Number) o).doubleValue());
				result = ((Number) o).doubleValue();
			}
			else
			{
				if (_logger.isDebugEnabled()) 
					_logger.debug("Colname='" + data.getColumnName(colPos) + "', toString(): " + o.toString());
				result = Double.parseDouble(o.toString());
			}

			if (result < minResult)
			{
				minResult = result;
			}
		}
		return Double.valueOf(minResult);
	}

	// Return the sum of the values of a Long column (ColumnName)
	private synchronized Double getSumValue(int whatData, int[] rowIds, String colname, boolean caseSensitive)
	{
		CounterTableModel data = null;

		if      (whatData == DATA_ABS)  data = getCounterDataAbs();
		else if (whatData == DATA_DIFF) data = getCounterDataDiff();
		else if (whatData == DATA_RATE) data = getCounterDataRate();
		else
			throw new RuntimeException("Only ABS, DIFF, or RATE data is available.");

		if (data == null)
			return null;

		int colPos = data.findColumn(colname, caseSensitive);
		if (colPos == -1)
		{
			_logger.info("getSumValue: Can't find the column '" + colname + "'.");
			return null;
		}
		return getSumValue(whatData, rowIds, colPos);
	}
	// Return the sum of the values of a Long column (ColumnName)
	private synchronized Double getSumValue(int whatData, int[] rowIds, int colPos)
	{
		CounterTableModel data = null;

		if      (whatData == DATA_ABS)  data = getCounterDataAbs();
		else if (whatData == DATA_DIFF) data = getCounterDataDiff();
		else if (whatData == DATA_RATE) data = getCounterDataRate();
		else
			throw new RuntimeException("Only ABS, DIFF, or RATE data is available.");

		if (data == null)
			return null;

		if (colPos < 0 || colPos > data.getColumnCount())
		{
			if (_logger.isDebugEnabled()) 
				_logger.debug("getSumValue: column pos " + colPos + " is out of range. column Count is "+data.getColumnCount());
			return null;
		}
		if (data.getRowCount() == 0)
			return null;

		double result = 0;

		int count = data.getRowCount();
		if (rowIds != null)
			count = rowIds.length;

		for (int i = 0; i < count; i++)
		{
			int rowId = i;
			if (rowIds != null)
				rowId = rowIds[i];

			// If the row is an Aggregated row... skip this
			if (discardAggregatedRowFromSumAndAvg() && isAggregateRow(rowId))
				continue;
			
			Object o = data.getValueAt(rowId, colPos);
			if (o == null)
				continue;

			if (o instanceof Long)
			{
				if (_logger.isDebugEnabled()) 
					_logger.debug("Colname='" + data.getColumnName(colPos) + "', Long: " + ((Long) o).longValue());
				result += ((Long) o).longValue();
			}
			else if (o instanceof Double)
			{
				if (_logger.isDebugEnabled()) 
					_logger.debug("Colname='" + data.getColumnName(colPos) + "', Double: " + ((Double) o).doubleValue());
				result += ((Double) o).doubleValue();
			}
			else
			{
				if (_logger.isDebugEnabled()) 
					_logger.debug("Colname='" + data.getColumnName(colPos) + "', toString(): " + o.toString());
				result += Double.parseDouble(o.toString());
			}
		}
		return Double.valueOf(result);
	}

	// Return the sum of the values of a Long column (ColumnName)
	private synchronized int getCountGtZero(int whatData, int[] rowIds, String colname, boolean caseSensitive)
	{
		CounterTableModel data = null;

		if      (whatData == DATA_ABS)  data = getCounterDataAbs();
		else if (whatData == DATA_DIFF) data = getCounterDataDiff();
		else if (whatData == DATA_RATE) data = getCounterDataRate();
		else
			throw new RuntimeException("Only ABS, DIFF, or RATE data is available.");

		if (data == null)
			return 0;

		int colPos = data.findColumn(colname, caseSensitive);
		if (colPos == -1)
		{
			_logger.info("getMaxValue: Can't find the column '" + colname + "'.");
			return 0;
		}
		return getCountGtZero(whatData, rowIds, colPos);
	}
	// Return the sum of the values of a Long column (ColumnName)
	private synchronized int getCountGtZero(int whatData, int[] rowIds, int colPos)
	{
		CounterTableModel data = null;

		if      (whatData == DATA_ABS)  data = getCounterDataAbs();
		else if (whatData == DATA_DIFF) data = getCounterDataDiff();
		else if (whatData == DATA_RATE) data = getCounterDataRate();
		else
			throw new RuntimeException("Only ABS, DIFF, or RATE data is available.");

		if (data == null)
			return 0;

		if (colPos < 0 || colPos > data.getColumnCount())
		{
			if (_logger.isDebugEnabled()) 
				_logger.debug("getCountGtZero: column pos " + colPos + " is out of range. column Count is "+data.getColumnCount());
			return 0;
		}
		if (data.getRowCount() == 0)
			return 0;

		int counter = 0;

		int count = data.getRowCount();
		if (rowIds != null)
			count = rowIds.length;

		for (int i = 0; i < count; i++)
		{
			int rowId = i;
			if (rowIds != null)
				rowId = rowIds[i];

			// If the row is an Aggregated row... skip this
			if (discardAggregatedRowFromCountGtZero() && isAggregateRow(rowId))
				continue;
			
			Object o = data.getValueAt(rowId, colPos);
			if (o == null)
				continue;

			if (o instanceof Number)
			{
				if ( ((Number)o).doubleValue() > 0.0 )
					counter++;
			}
			else
			{
				_logger.warn("NOT A INSTANCE OF NUMBER: Colname='" + data.getColumnName(colPos) + "', toString(): " + o.toString());
			}
		}
		return counter;
	}

	// Return the AVG of the values of a Long column (ColumnName)
	private synchronized Double getAvgValue(int whatData, int[] rowIds, String colname, boolean caseSensitive)
	{
		CounterTableModel data = null;

		if      (whatData == DATA_ABS)  data = getCounterDataAbs();
		else if (whatData == DATA_DIFF) data = getCounterDataDiff();
		else if (whatData == DATA_RATE) data = getCounterDataRate();
		else
			throw new RuntimeException("Only ABS, DIFF, or RATE data is available.");

		if (data == null)
			return null;

		Double sum = getSumValue(whatData, rowIds, colname, caseSensitive);
		if (sum == null)
			return null;

		int count = data.getRowCount();
		if (rowIds != null)
			count = rowIds.length;
		
		if (discardAggregatedRowFromSumAndAvg() && hasAggregatedRowId())
			count--;
			
		if (count == 0)
			return Double.valueOf(0);
		else
			return Double.valueOf(sum.doubleValue() / count);
	}

	// Return the AVG of the values of a Long column (ColumnName)
	private synchronized Double getAvgValue(int whatData, int[] rowIds, int colPos)
	{
		CounterTableModel data = null;

		if      (whatData == DATA_ABS)  data = getCounterDataAbs();
		else if (whatData == DATA_DIFF) data = getCounterDataDiff();
		else if (whatData == DATA_RATE) data = getCounterDataRate();
		else
			throw new RuntimeException("Only ABS, DIFF, or RATE data is available.");

		if (data == null)
			return null;

		Double sum = getSumValue(whatData, rowIds, colPos);
		if (sum == null)
			return null;

		int count = data.getRowCount();
		if (rowIds != null)
			count = rowIds.length;
		
		if (discardAggregatedRowFromSumAndAvg() && hasAggregatedRowId())
			count--;
			
		if (count == 0)
			return Double.valueOf(0);
		else
			return Double.valueOf(sum.doubleValue() / count);
	}

	// Return the AVG of the values of a Long column (ColumnName)
	private synchronized Double getAvgValueGtZero(int whatData, int[] rowIds, String colname, boolean caseSensitive)
	{
		CounterTableModel data = null;

		if      (whatData == DATA_ABS)  data = getCounterDataAbs();
		else if (whatData == DATA_DIFF) data = getCounterDataDiff();
		else if (whatData == DATA_RATE) data = getCounterDataRate();
		else
			throw new RuntimeException("Only ABS, DIFF, or RATE data is available.");

		if (data == null)
			return null;

		Double sum = getSumValue(whatData, rowIds, colname, caseSensitive);
		if (sum == null)
			return null;

		int count = getCountGtZero(whatData, rowIds, colname, caseSensitive);
		
		if (count == 0)
			return Double.valueOf(0);
		else
			return Double.valueOf(sum.doubleValue() / count);
	}

	// Return the AVG of the values of a Long column (ColumnName)
	private synchronized Double getAvgValueGtZero(int whatData, int[] rowIds, int colPos)
	{
		CounterTableModel data = null;

		if      (whatData == DATA_ABS)  data = getCounterDataAbs();
		else if (whatData == DATA_DIFF) data = getCounterDataDiff();
		else if (whatData == DATA_RATE) data = getCounterDataRate();
		else
			throw new RuntimeException("Only ABS, DIFF, or RATE data is available.");

		if (data == null)
			return null;

		Double sum = getSumValue(whatData, rowIds, colPos);
		if (sum == null)
			return null;

		int count = getCountGtZero(whatData, rowIds, colPos);
		
		if (count == 0)
			return Double.valueOf(0);
		else
			return Double.valueOf(sum.doubleValue() / count);
	}

	// Return the Primary Key for a specific row
	private synchronized String getPkValue(int whatData, int rowId)
	{
		CounterTableModel data = null;

		if      (whatData == DATA_ABS)  data = getCounterDataAbs();
		else if (whatData == DATA_DIFF) data = getCounterDataDiff();
		else if (whatData == DATA_RATE) data = getCounterDataRate();
		else
			throw new RuntimeException("Only ABS, DIFF, or RATE data is available.");

		if (data == null)
			return null;

		return data.getPkValue(rowId);
	}

	/** Return the rowId for the Primary Key */
	private synchronized int getRowIdForPkValue(int whatData, String pkStr)
	{
		CounterTableModel data = null;

		if      (whatData == DATA_ABS)  data = getCounterDataAbs();
		else if (whatData == DATA_DIFF) data = getCounterDataDiff();
		else if (whatData == DATA_RATE) data = getCounterDataRate();
		else
			throw new RuntimeException("Only ABS, DIFF, or RATE data is available.");

		if (data == null)
			return -1;

		// Get the rowId, if not found, return -1
		return data.getRowNumberForPkValue(pkStr);
	}

	private NumberFormat _nf = NumberFormat.getInstance();
	private NumberFormat getNumberFormat()
	{
		if (_nf == null)
			return NumberFormat.getInstance();
		
		return _nf;
	}
	public void getNumberFormat(NumberFormat nf)
	{
		_nf = nf;
	}
	
	public String toFormatedStr(Object o)
	{
		if (o == null)
			return "";
		
		if (o instanceof Number)
			return getNumberFormat().format(o);

		return o.toString();
	}


	//--------------------------------------------------------------
	// Wrapper functions to read ABSOLUTE values
	//--------------------------------------------------------------
//	public String getAbsString        (int    rowId, int    colPos)                          { Object o = getValue     (DATA_ABS, rowId, colPos);        return (o==null) ?  "" : o.toString(); }
//	public String getAbsString        (int    rowId, String colname)                         { Object o = getValue     (DATA_ABS, rowId, colname, true); return (o==null) ?  "" : o.toString(); }
//	public String getAbsString        (int    rowId, String colname, boolean cs)             { Object o = getValue     (DATA_ABS, rowId, colname,   cs); return (o==null) ?  "" : o.toString(); }
//	public String getAbsString        (int    rowId, String colname, boolean cs, String def) { Object o = getValue     (DATA_ABS, rowId, colname,   cs); return (o==null) ? def : o.toString(); }
//	public String getAbsString        (String pkStr, String colname)                         { Object o = getValue     (DATA_ABS, pkStr, colname, true); return (o==null) ?  "" : o.toString(); }
//	public String getAbsString        (String pkStr, String colname, boolean cs)             { Object o = getValue     (DATA_ABS, pkStr, colname,   cs); return (o==null) ?  "" : o.toString(); }
//	public String getAbsString        (String pkStr, String colname, boolean cs, String def) { Object o = getValue     (DATA_ABS, pkStr, colname,   cs); return (o==null) ? def : o.toString(); }
	public String getAbsString        (int    rowId, int    colPos)                          { Object o = getValue     (DATA_ABS, rowId, colPos);        return toFormatedStr(o); }
	public String getAbsString        (int    rowId, String colname)                         { Object o = getValue     (DATA_ABS, rowId, colname, true); return toFormatedStr(o); }
	public String getAbsString        (int    rowId, String colname, boolean cs)             { Object o = getValue     (DATA_ABS, rowId, colname,   cs); return toFormatedStr(o); }
	public String getAbsString        (int    rowId, String colname, boolean cs, String def) { Object o = getValue     (DATA_ABS, rowId, colname,   cs); return toFormatedStr((o==null)?def:o); }
	public String getAbsString        (String pkStr, String colname)                         { Object o = getValue     (DATA_ABS, pkStr, colname, true); return toFormatedStr(o); }
	public String getAbsString        (String pkStr, String colname, boolean cs)             { Object o = getValue     (DATA_ABS, pkStr, colname,   cs); return toFormatedStr(o); }
	public String getAbsString        (String pkStr, String colname, boolean cs, String def) { Object o = getValue     (DATA_ABS, pkStr, colname,   cs); return toFormatedStr((o==null)?def:o); }
	
	public Object getAbsValue         (int    rowId, int    colPos)                          { Object o = getValue     (DATA_ABS, rowId, colPos); return o; }
	public Object getAbsValue         (int    rowId, String colname)                         { return getValue         (DATA_ABS, rowId, colname, true); }
	public Object getAbsValue         (int    rowId, String colname, boolean cs)             { return getValue         (DATA_ABS, rowId, colname,   cs); }
	public Object getAbsValue         (String pkStr, String colname)                         { return getValue         (DATA_ABS, pkStr, colname, true); }
	public Object getAbsValue         (String pkStr, String colname, boolean cs)             { return getValue         (DATA_ABS, pkStr, colname,   cs); }
	
	public Double getAbsValueAsDouble (int    rowId, int    colPos)                          { return getValueAsDouble (DATA_ABS, rowId, colPos);             }
	public Double getAbsValueAsDouble (int    rowId, int    colPos, Double def)              { return getValueAsDouble (DATA_ABS, rowId, colPos, def);        }
	public Double getAbsValueAsDouble (int    rowId, String colname)                         { return getValueAsDouble (DATA_ABS, rowId, colname, true);      }
	public Double getAbsValueAsDouble (int    rowId, String colname, boolean cs)             { return getValueAsDouble (DATA_ABS, rowId, colname,   cs);      }
	public Double getAbsValueAsDouble (int    rowId, String colname, Double def)             { return getValueAsDouble (DATA_ABS, rowId, colname, true, def); }
	public Double getAbsValueAsDouble (int    rowId, String colname, boolean cs, Double def) { return getValueAsDouble (DATA_ABS, rowId, colname,   cs, def); }
	public Double getAbsValueAsDouble (String pkStr, String colname)                         { return getValueAsDouble (DATA_ABS, pkStr, colname, true);      }
	public Double getAbsValueAsDouble (String pkStr, String colname, boolean cs)             { return getValueAsDouble (DATA_ABS, pkStr, colname,   cs);      }
	public Double getAbsValueAsDouble (String pkStr, String colname, Double def)             { return getValueAsDouble (DATA_ABS, pkStr, colname, true, def); }
	public Double getAbsValueAsDouble (String pkStr, String colname, boolean cs, Double def) { return getValueAsDouble (DATA_ABS, pkStr, colname,   cs, def); }
	
	public Integer getAbsValueAsInteger(int    rowId, int    colPos)                          { return getValueAsInteger(DATA_ABS, rowId, colPos);             }
	public Integer getAbsValueAsInteger(int    rowId, int    colPos, Integer def)             { return getValueAsInteger(DATA_ABS, rowId, colPos, def);        }
	public Integer getAbsValueAsInteger(int    rowId, String colname)                         { return getValueAsInteger(DATA_ABS, rowId, colname, true);      }
	public Integer getAbsValueAsInteger(int    rowId, String colname, boolean cs)             { return getValueAsInteger(DATA_ABS, rowId, colname,   cs);      }
	public Integer getAbsValueAsInteger(int    rowId, String colname, Integer def)            { return getValueAsInteger(DATA_ABS, rowId, colname, true, def); }
	public Integer getAbsValueAsInteger(int    rowId, String colname, boolean cs, Integer def){ return getValueAsInteger(DATA_ABS, rowId, colname,   cs, def); }
	public Integer getAbsValueAsInteger(String pkStr, String colname)                         { return getValueAsInteger(DATA_ABS, pkStr, colname, true);      }
	public Integer getAbsValueAsInteger(String pkStr, String colname, boolean cs)             { return getValueAsInteger(DATA_ABS, pkStr, colname,   cs);      }
	public Integer getAbsValueAsInteger(String pkStr, String colname, Integer def)            { return getValueAsInteger(DATA_ABS, pkStr, colname, true, def); }
	public Integer getAbsValueAsInteger(String pkStr, String colname, boolean cs, Integer def){ return getValueAsInteger(DATA_ABS, pkStr, colname,   cs, def); }
	
	public Long   getAbsValueAsLong  (int    rowId, int    colPos)                           { return getValueAsLong   (DATA_ABS, rowId, colPos);             }
	public Long   getAbsValueAsLong  (int    rowId, int    colPos, Long def)                 { return getValueAsLong   (DATA_ABS, rowId, colPos, def);        }
	public Long   getAbsValueAsLong  (int    rowId, String colname)                          { return getValueAsLong   (DATA_ABS, rowId, colname, true);      }
	public Long   getAbsValueAsLong  (int    rowId, String colname, boolean cs)              { return getValueAsLong   (DATA_ABS, rowId, colname,   cs);      }
	public Long   getAbsValueAsLong  (int    rowId, String colname, Long def)                { return getValueAsLong   (DATA_ABS, rowId, colname, true, def); }
	public Long   getAbsValueAsLong  (int    rowId, String colname, boolean cs, Long def)    { return getValueAsLong   (DATA_ABS, rowId, colname,   cs, def); }
	public Long   getAbsValueAsLong  (String pkStr, String colname)                          { return getValueAsLong   (DATA_ABS, pkStr, colname, true);      }
	public Long   getAbsValueAsLong  (String pkStr, String colname, boolean cs)              { return getValueAsLong   (DATA_ABS, pkStr, colname,   cs);      }
	public Long   getAbsValueAsLong  (String pkStr, String colname, Long def)                { return getValueAsLong   (DATA_ABS, pkStr, colname, true, def); }
	public Long   getAbsValueAsLong  (String pkStr, String colname, boolean cs, Long def)    { return getValueAsLong   (DATA_ABS, pkStr, colname,   cs, def); }
	
	public Timestamp getAbsValueAsTimestamp(int    rowId, int    colPos)                            { return getValueAsTimestamp(DATA_ABS, rowId, colPos);             }
	public Timestamp getAbsValueAsTimestamp(int    rowId, int    colPos, Timestamp def)             { return getValueAsTimestamp(DATA_ABS, rowId, colPos, def);        }
	public Timestamp getAbsValueAsTimestamp(int    rowId, String colname)                           { return getValueAsTimestamp(DATA_ABS, rowId, colname, true);      }
	public Timestamp getAbsValueAsTimestamp(int    rowId, String colname, boolean cs)               { return getValueAsTimestamp(DATA_ABS, rowId, colname,   cs);      }
	public Timestamp getAbsValueAsTimestamp(int    rowId, String colname, Timestamp def)            { return getValueAsTimestamp(DATA_ABS, rowId, colname, true, def); }
	public Timestamp getAbsValueAsTimestamp(int    rowId, String colname, boolean cs, Timestamp def){ return getValueAsTimestamp(DATA_ABS, rowId, colname,   cs, def); }
	public Timestamp getAbsValueAsTimestamp(String pkStr, String colname)                           { return getValueAsTimestamp(DATA_ABS, pkStr, colname, true);      }
	public Timestamp getAbsValueAsTimestamp(String pkStr, String colname, boolean cs)               { return getValueAsTimestamp(DATA_ABS, pkStr, colname,   cs);      }
	public Timestamp getAbsValueAsTimestamp(String pkStr, String colname, Timestamp def)            { return getValueAsTimestamp(DATA_ABS, pkStr, colname, true, def); }
	public Timestamp getAbsValueAsTimestamp(String pkStr, String colname, boolean cs, Timestamp def){ return getValueAsTimestamp(DATA_ABS, pkStr, colname,   cs, def); }
	
	public Double getAbsValueMax      (int    colPos)                                        { return getMaxValue      (DATA_ABS, null,  colPos);        }
	public Double getAbsValueMax      (String colname)                                       { return getMaxValue      (DATA_ABS, null,  colname, true); }
	public Double getAbsValueMax      (String colname, boolean cs)                           { return getMaxValue      (DATA_ABS, null,  colname,   cs); }
	
	public Double getAbsValueMin      (int    colPos)                                        { return getMinValue      (DATA_ABS, null,  colPos);        }
	public Double getAbsValueMin      (String colname)                                       { return getMinValue      (DATA_ABS, null,  colname, true); }
	public Double getAbsValueMin      (String colname, boolean cs)                           { return getMinValue      (DATA_ABS, null,  colname,   cs); }
	
	public Double getAbsValueAvg      (int    colPos)                                        { return getAvgValue      (DATA_ABS, null,  colPos);        }
	public Double getAbsValueAvg      (String colname)                                       { return getAvgValue      (DATA_ABS, null,  colname, true); }
	public Double getAbsValueAvg      (String colname, boolean cs)                           { return getAvgValue      (DATA_ABS, null,  colname,   cs); }
	
	public Double getAbsValueAvgGtZero(int    colPos)                                        { return getAvgValueGtZero(DATA_ABS, null,  colPos);        }
	public Double getAbsValueAvgGtZero(String colname)                                       { return getAvgValueGtZero(DATA_ABS, null,  colname, true); }
	public Double getAbsValueAvgGtZero(String colname, boolean cs)                           { return getAvgValueGtZero(DATA_ABS, null,  colname,   cs); }
	
	public Double getAbsValueSum      (int    colPos)                                        { return getSumValue      (DATA_ABS, null,  colPos);        }
	public Double getAbsValueSum      (String colname)                                       { return getSumValue      (DATA_ABS, null,  colname, true); }
	public Double getAbsValueSum      (String colname, boolean cs)                           { return getSumValue      (DATA_ABS, null,  colname,   cs); }
	
	public String getAbsPkValue       (int    rowId)                                         { return getPkValue       (DATA_ABS, rowId  ); }
	public int    getAbsRowIdForPkValue(String pkStr)                                        { return getRowIdForPkValue(DATA_ABS, pkStr); }
	
	public int[]  getAbsRowIdsWhere   (String colname, Object colval)                        { return getRowIdsWhere   (DATA_ABS, colname, true, colval); }
	public int[]  getAbsRowIdsWhere   (String colname, boolean cs, Object colval)            { return getRowIdsWhere   (DATA_ABS, colname,   cs, colval); }
	
	public Double getAbsValueMax      (int[] rowIds, String colname)                         { return getMaxValue      (DATA_ABS, rowIds,  colname, true); }
	public Double getAbsValueMax      (int[] rowIds, String colname, boolean cs)             { return getMaxValue      (DATA_ABS, rowIds,  colname,   cs); }
	
	public Double getAbsValueMin      (int[] rowIds, String colname)                         { return getMinValue      (DATA_ABS, rowIds,  colname, true); }
	public Double getAbsValueMin      (int[] rowIds, String colname, boolean cs)             { return getMinValue      (DATA_ABS, rowIds,  colname,   cs); }
	
	public Double getAbsValueAvg      (int[] rowIds, String colname)                         { return getAvgValue      (DATA_ABS, rowIds,  colname, true); }
	public Double getAbsValueAvg      (int[] rowIds, String colname, boolean cs)             { return getAvgValue      (DATA_ABS, rowIds,  colname,   cs); }
	
	public Double getAbsValueAvgGtZero(int[] rowIds, String colname)                         { return getAvgValueGtZero(DATA_ABS, rowIds,  colname, true); }
	public Double getAbsValueAvgGtZero(int[] rowIds, String colname, boolean cs)             { return getAvgValueGtZero(DATA_ABS, rowIds,  colname,   cs); }
	
	public Double getAbsValueSum      (int[] rowIds, String colname)                         { return getSumValue      (DATA_ABS, rowIds,  colname, true); }
	public Double getAbsValueSum      (int[] rowIds, String colname, boolean cs)             { return getSumValue      (DATA_ABS, rowIds,  colname,   cs); }

	
	//--------------------------------------------------------------
	// Wrapper functions to read DIFF (new-old) values
	//--------------------------------------------------------------
//	public String getDiffString        (int    rowId, int    colPos)                          { Object o = getValue     (DATA_DIFF, rowId, colPos);        return (o==null)?"":o.toString(); }
//	public String getDiffString        (int    rowId, String colname)                         { Object o = getValue     (DATA_DIFF, rowId, colname, true); return (o==null)?"":o.toString(); }
//	public String getDiffString        (int    rowId, String colname, boolean cs)             { Object o = getValue     (DATA_DIFF, rowId, colname,   cs); return (o==null)?"":o.toString(); }
//	public String getDiffString        (String pkStr, String colname)                         { Object o = getValue     (DATA_DIFF, pkStr, colname, true); return (o==null)?"":o.toString(); }
//	public String getDiffString        (String pkStr, String colname, boolean cs)             { Object o = getValue     (DATA_DIFF, pkStr, colname,   cs); return (o==null)?"":o.toString(); }
	public String getDiffString        (int    rowId, int    colPos)                          { Object o = getValue     (DATA_DIFF, rowId, colPos);        return toFormatedStr(o); }
	public String getDiffString        (int    rowId, String colname)                         { Object o = getValue     (DATA_DIFF, rowId, colname, true); return toFormatedStr(o); }
	public String getDiffString        (int    rowId, String colname, boolean cs)             { Object o = getValue     (DATA_DIFF, rowId, colname,   cs); return toFormatedStr(o); }
	public String getDiffString        (String pkStr, String colname)                         { Object o = getValue     (DATA_DIFF, pkStr, colname, true); return toFormatedStr(o); }
	public String getDiffString        (String pkStr, String colname, boolean cs)             { Object o = getValue     (DATA_DIFF, pkStr, colname,   cs); return toFormatedStr(o); }
	
	public Object getDiffValue         (int    rowId, int    colPos)                          { Object o = getValue     (DATA_DIFF, rowId, colPos);  return o; }
	public Object getDiffValue         (int    rowId, String colname)                         { return getValue         (DATA_DIFF, rowId, colname, true); }
	public Object getDiffValue         (int    rowId, String colname, boolean cs)             { return getValue         (DATA_DIFF, rowId, colname,   cs); }
	public Object getDiffValue         (String pkStr, String colname)                         { return getValue         (DATA_DIFF, pkStr, colname, true); }
	public Object getDiffValue         (String pkStr, String colname, boolean cs)             { return getValue         (DATA_DIFF, pkStr, colname,   cs); }
	
	public Double getDiffValueAsDouble (int    rowId, int    colPos)                          { return getValueAsDouble (DATA_DIFF, rowId, colPos);             }
	public Double getDiffValueAsDouble (int    rowId, int    colPos, Double def)              { return getValueAsDouble (DATA_DIFF, rowId, colPos, def);        }
	public Double getDiffValueAsDouble (int    rowId, String colname)                         { return getValueAsDouble (DATA_DIFF, rowId, colname, true);      }
	public Double getDiffValueAsDouble (int    rowId, String colname, boolean cs)             { return getValueAsDouble (DATA_DIFF, rowId, colname,   cs);      }
	public Double getDiffValueAsDouble (int    rowId, String colname, Double def)             { return getValueAsDouble (DATA_DIFF, rowId, colname, true, def); }
	public Double getDiffValueAsDouble (int    rowId, String colname, boolean cs, Double def) { return getValueAsDouble (DATA_DIFF, rowId, colname,   cs, def); }
	public Double getDiffValueAsDouble (String pkStr, String colname)                         { return getValueAsDouble (DATA_DIFF, pkStr, colname, true);      }
	public Double getDiffValueAsDouble (String pkStr, String colname, boolean cs)             { return getValueAsDouble (DATA_DIFF, pkStr, colname,   cs);      }
	public Double getDiffValueAsDouble (String pkStr, String colname, Double def)             { return getValueAsDouble (DATA_DIFF, pkStr, colname, true, def); }
	public Double getDiffValueAsDouble (String pkStr, String colname, boolean cs, Double def) { return getValueAsDouble (DATA_DIFF, pkStr, colname,   cs, def); }
	
	public Integer getDiffValueAsInteger(int    rowId, int    colPos)                          { return getValueAsInteger(DATA_DIFF, rowId, colPos);             }
	public Integer getDiffValueAsInteger(int    rowId, int    colPos, Integer def)             { return getValueAsInteger(DATA_DIFF, rowId, colPos, def);        }
	public Integer getDiffValueAsInteger(int    rowId, String colname)                         { return getValueAsInteger(DATA_DIFF, rowId, colname, true);      }
	public Integer getDiffValueAsInteger(int    rowId, String colname, boolean cs)             { return getValueAsInteger(DATA_DIFF, rowId, colname,   cs);      }
	public Integer getDiffValueAsInteger(int    rowId, String colname, Integer def)            { return getValueAsInteger(DATA_DIFF, rowId, colname, true, def); }
	public Integer getDiffValueAsInteger(int    rowId, String colname, boolean cs, Integer def){ return getValueAsInteger(DATA_DIFF, rowId, colname,   cs, def); }
	public Integer getDiffValueAsInteger(String pkStr, String colname)                         { return getValueAsInteger(DATA_DIFF, pkStr, colname, true);      }
	public Integer getDiffValueAsInteger(String pkStr, String colname, boolean cs)             { return getValueAsInteger(DATA_DIFF, pkStr, colname,   cs);      }
	public Integer getDiffValueAsInteger(String pkStr, String colname, Integer def)            { return getValueAsInteger(DATA_DIFF, pkStr, colname, true, def); }
	public Integer getDiffValueAsInteger(String pkStr, String colname, boolean cs, Integer def){ return getValueAsInteger(DATA_DIFF, pkStr, colname,   cs, def); }
	
	public Long   getDiffValueAsLong  (int    rowId, int    colPos)                           { return getValueAsLong   (DATA_DIFF, rowId, colPos);             }
	public Long   getDiffValueAsLong  (int    rowId, int    colPos, Long def)                 { return getValueAsLong   (DATA_DIFF, rowId, colPos, def);        }
	public Long   getDiffValueAsLong  (int    rowId, String colname)                          { return getValueAsLong   (DATA_DIFF, rowId, colname, true);      }
	public Long   getDiffValueAsLong  (int    rowId, String colname, boolean cs)              { return getValueAsLong   (DATA_DIFF, rowId, colname,   cs);      }
	public Long   getDiffValueAsLong  (int    rowId, String colname, Long def)                { return getValueAsLong   (DATA_DIFF, rowId, colname, true, def); }
	public Long   getDiffValueAsLong  (int    rowId, String colname, boolean cs, Long def)    { return getValueAsLong   (DATA_DIFF, rowId, colname,   cs, def); }
	public Long   getDiffValueAsLong  (String pkStr, String colname)                          { return getValueAsLong   (DATA_DIFF, pkStr, colname, true);      }
	public Long   getDiffValueAsLong  (String pkStr, String colname, boolean cs)              { return getValueAsLong   (DATA_DIFF, pkStr, colname,   cs);      }
	public Long   getDiffValueAsLong  (String pkStr, String colname, Long def)                { return getValueAsLong   (DATA_DIFF, pkStr, colname, true, def); }
	public Long   getDiffValueAsLong  (String pkStr, String colname, boolean cs, Long def)    { return getValueAsLong   (DATA_DIFF, pkStr, colname,   cs, def); }

	// NOTE: Do we really need this for DIFF
	public Timestamp getDiffValueAsTimestamp(int    rowId, int    colPos)                            { return getValueAsTimestamp(DATA_DIFF, rowId, colPos);             }
	public Timestamp getDiffValueAsTimestamp(int    rowId, int    colPos, Timestamp def)             { return getValueAsTimestamp(DATA_DIFF, rowId, colPos, def);        }
	public Timestamp getDiffValueAsTimestamp(int    rowId, String colname)                           { return getValueAsTimestamp(DATA_DIFF, rowId, colname, true);      }
	public Timestamp getDiffValueAsTimestamp(int    rowId, String colname, boolean cs)               { return getValueAsTimestamp(DATA_DIFF, rowId, colname,   cs);      }
	public Timestamp getDiffValueAsTimestamp(int    rowId, String colname, Timestamp def)            { return getValueAsTimestamp(DATA_DIFF, rowId, colname, true, def); }
	public Timestamp getDiffValueAsTimestamp(int    rowId, String colname, boolean cs, Timestamp def){ return getValueAsTimestamp(DATA_DIFF, rowId, colname,   cs, def); }
	public Timestamp getDiffValueAsTimestamp(String pkStr, String colname)                           { return getValueAsTimestamp(DATA_DIFF, pkStr, colname, true);      }
	public Timestamp getDiffValueAsTimestamp(String pkStr, String colname, boolean cs)               { return getValueAsTimestamp(DATA_DIFF, pkStr, colname,   cs);      }
	public Timestamp getDiffValueAsTimestamp(String pkStr, String colname, Timestamp def)            { return getValueAsTimestamp(DATA_DIFF, pkStr, colname, true, def); }
	public Timestamp getDiffValueAsTimestamp(String pkStr, String colname, boolean cs, Timestamp def){ return getValueAsTimestamp(DATA_DIFF, pkStr, colname,   cs, def); }
	
	public Double getDiffValueMax      (int    colPos)                                        { return getMaxValue      (DATA_DIFF, null,  colPos);        }
	public Double getDiffValueMax      (String colname)                                       { return getMaxValue      (DATA_DIFF, null,  colname, true); }
	public Double getDiffValueMax      (String colname, boolean cs)                           { return getMaxValue      (DATA_DIFF, null,  colname,   cs); }
	
	public Double getDiffValueMin      (int    colPos)                                        { return getMinValue      (DATA_DIFF, null,  colPos);        }
	public Double getDiffValueMin      (String colname)                                       { return getMinValue      (DATA_DIFF, null,  colname, true); }
	public Double getDiffValueMin      (String colname, boolean cs)                           { return getMinValue      (DATA_DIFF, null,  colname,   cs); }
	
	public Double getDiffValueAvg      (int    colPos)                                        { return getAvgValue      (DATA_DIFF, null,  colPos);        }
	public Double getDiffValueAvg      (String colname)                                       { return getAvgValue      (DATA_DIFF, null,  colname, true); }
	public Double getDiffValueAvg      (String colname, boolean cs)                           { return getAvgValue      (DATA_DIFF, null,  colname,   cs); }

	public Double getDiffValueAvgGtZero(int    colPos)                                        { return getAvgValueGtZero(DATA_DIFF, null,  colPos);        }
	public Double getDiffValueAvgGtZero(String colname)                                       { return getAvgValueGtZero(DATA_DIFF, null,  colname, true); }
	public Double getDiffValueAvgGtZero(String colname, boolean cs)                           { return getAvgValueGtZero(DATA_DIFF, null,  colname,   cs); }
	
	public Double getDiffValueSum      (int    colPos)                                        { return getSumValue      (DATA_DIFF, null,  colPos);        }
	public Double getDiffValueSum      (String colname)                                       { return getSumValue      (DATA_DIFF, null,  colname, true); }
	public Double getDiffValueSum      (String colname, boolean cs)                           { return getSumValue      (DATA_DIFF, null,  colname,   cs); }
	
	public String getDiffPkValue       (int    rowId)                                         { return getPkValue       (DATA_DIFF, rowId  ); }
	public int    getDiffRowIdForPkValue(String pkStr)                                        { return getRowIdForPkValue(DATA_DIFF, pkStr); }
                                                                                  
	public int[]  getDiffRowIdsWhere   (String colname, Object colval)                        { return getRowIdsWhere   (DATA_DIFF, colname, true, colval); }
	public int[]  getDiffRowIdsWhere   (String colname, boolean cs, Object colval)            { return getRowIdsWhere   (DATA_DIFF, colname,   cs, colval); }
	
	public Double getDiffValueMax      (int[] rowIds, String colname)                         { return getMaxValue      (DATA_DIFF, rowIds,  colname, true); }
	public Double getDiffValueMax      (int[] rowIds, String colname, boolean cs)             { return getMaxValue      (DATA_DIFF, rowIds,  colname,   cs); }
	
	public Double getDiffValueMin      (int[] rowIds, String colname)                         { return getMinValue      (DATA_DIFF, rowIds,  colname, true); }
	public Double getDiffValueMin      (int[] rowIds, String colname, boolean cs)             { return getMinValue      (DATA_DIFF, rowIds,  colname,   cs); }
	
	public Double getDiffValueAvg      (int[] rowIds, String colname)                         { return getAvgValue      (DATA_DIFF, rowIds,  colname, true); }
	public Double getDiffValueAvg      (int[] rowIds, String colname, boolean cs)             { return getAvgValue      (DATA_DIFF, rowIds,  colname,   cs); }
	
	public Double getDiffValueAvgGtZero(int[] rowIds, String colname)                         { return getAvgValueGtZero(DATA_DIFF, rowIds,  colname, true); }
	public Double getDiffValueAvgGtZero(int[] rowIds, String colname, boolean cs)             { return getAvgValueGtZero(DATA_DIFF, rowIds,  colname,   cs); }
	
	public Double getDiffValueSum      (int[] rowIds, String colname)                         { return getSumValue      (DATA_DIFF, rowIds,  colname, true); }
	public Double getDiffValueSum      (int[] rowIds, String colname, boolean cs)             { return getSumValue      (DATA_DIFF, rowIds,  colname,   cs); }

	
	//--------------------------------------------------------------
	// Wrapper functions to read RATE DIFF/time values
	//--------------------------------------------------------------
//	public String getRateString        (int    rowId, int    colPos)                          { Object o = getValue     (DATA_RATE, rowId, colPos);        return (o==null)?"":o.toString(); }
//	public String getRateString        (int    rowId, String colname)                         { Object o = getValue     (DATA_RATE, rowId, colname, true); return (o==null)?"":o.toString(); }
//	public String getRateString        (int    rowId, String colname, boolean cs)             { Object o = getValue     (DATA_RATE, rowId, colname,   cs); return (o==null)?"":o.toString(); }
//	public String getRateString        (String pkStr, String colname)                         { Object o = getValue     (DATA_RATE, pkStr, colname, true); return (o==null)?"":o.toString(); }
//	public String getRateString        (String pkStr, String colname, boolean cs)             { Object o = getValue     (DATA_RATE, pkStr, colname,   cs); return (o==null)?"":o.toString(); }
	public String getRateString        (int    rowId, int    colPos)                          { Object o = getValue     (DATA_RATE, rowId, colPos);        return toFormatedStr(o); }
	public String getRateString        (int    rowId, String colname)                         { Object o = getValue     (DATA_RATE, rowId, colname, true); return toFormatedStr(o); }
	public String getRateString        (int    rowId, String colname, boolean cs)             { Object o = getValue     (DATA_RATE, rowId, colname,   cs); return toFormatedStr(o); }
	public String getRateString        (String pkStr, String colname)                         { Object o = getValue     (DATA_RATE, pkStr, colname, true); return toFormatedStr(o); }
	public String getRateString        (String pkStr, String colname, boolean cs)             { Object o = getValue     (DATA_RATE, pkStr, colname,   cs); return toFormatedStr(o); }
	
	public Object getRateValue         (int    rowId, int    colPos)                          { Object o = getValue     (DATA_RATE, rowId, colPos);  return o; }
	public Object getRateValue         (int    rowId, String colname)                         { return getValue         (DATA_RATE, rowId, colname, true); }
	public Object getRateValue         (int    rowId, String colname, boolean cs)             { return getValue         (DATA_RATE, rowId, colname,   cs); }
	public Object getRateValue         (String pkStr, String colname)                         { return getValue         (DATA_RATE, pkStr, colname, true); }
	public Object getRateValue         (String pkStr, String colname, boolean cs)             { return getValue         (DATA_RATE, pkStr, colname,   cs); }
	
	public Double getRateValueAsDouble (int    rowId, int    colPos)                          { return getValueAsDouble (DATA_RATE, rowId, colPos);             }
	public Double getRateValueAsDouble (int    rowId, int    colPos, Double def)              { return getValueAsDouble (DATA_RATE, rowId, colPos, def);        }
	public Double getRateValueAsDouble (int    rowId, String colname)                         { return getValueAsDouble (DATA_RATE, rowId, colname, true);      }
	public Double getRateValueAsDouble (int    rowId, String colname, boolean cs)             { return getValueAsDouble (DATA_RATE, rowId, colname,   cs);      }
	public Double getRateValueAsDouble (int    rowId, String colname, Double def)             { return getValueAsDouble (DATA_RATE, rowId, colname, true, def); }
	public Double getRateValueAsDouble (int    rowId, String colname, boolean cs, Double def) { return getValueAsDouble (DATA_RATE, rowId, colname,   cs, def); }
	public Double getRateValueAsDouble (String pkStr, String colname)                         { return getValueAsDouble (DATA_RATE, pkStr, colname, true);      }
	public Double getRateValueAsDouble (String pkStr, String colname, boolean cs)             { return getValueAsDouble (DATA_RATE, pkStr, colname,   cs);      }
	public Double getRateValueAsDouble (String pkStr, String colname, Double def)             { return getValueAsDouble (DATA_RATE, pkStr, colname, true, def); }
	public Double getRateValueAsDouble (String pkStr, String colname, boolean cs, Double def) { return getValueAsDouble (DATA_RATE, pkStr, colname,   cs, def); }
	
	public Integer getRateValueAsInteger(int    rowId, int    colPos)                          { return getValueAsInteger(DATA_RATE, rowId, colPos);             }
	public Integer getRateValueAsInteger(int    rowId, int    colPos, Integer def)             { return getValueAsInteger(DATA_RATE, rowId, colPos, def);        }
	public Integer getRateValueAsInteger(int    rowId, String colname)                         { return getValueAsInteger(DATA_RATE, rowId, colname, true);      }
	public Integer getRateValueAsInteger(int    rowId, String colname, boolean cs)             { return getValueAsInteger(DATA_RATE, rowId, colname,   cs);      }
	public Integer getRateValueAsInteger(int    rowId, String colname, Integer def)            { return getValueAsInteger(DATA_RATE, rowId, colname, true, def); }
	public Integer getRateValueAsInteger(int    rowId, String colname, boolean cs, Integer def){ return getValueAsInteger(DATA_RATE, rowId, colname,   cs, def); }
	public Integer getRateValueAsInteger(String pkStr, String colname)                         { return getValueAsInteger(DATA_RATE, pkStr, colname, true);      }
	public Integer getRateValueAsInteger(String pkStr, String colname, boolean cs)             { return getValueAsInteger(DATA_RATE, pkStr, colname,   cs);      }
	public Integer getRateValueAsInteger(String pkStr, String colname, Integer def)            { return getValueAsInteger(DATA_RATE, pkStr, colname, true, def); }
	public Integer getRateValueAsInteger(String pkStr, String colname, boolean cs, Integer def){ return getValueAsInteger(DATA_RATE, pkStr, colname,   cs, def); }
	
	public Long   getRateValueAsLong  (int    rowId, int    colPos)                           { return getValueAsLong   (DATA_RATE, rowId, colPos);             }
	public Long   getRateValueAsLong  (int    rowId, int    colPos, Long def)                 { return getValueAsLong   (DATA_RATE, rowId, colPos, def);        }
	public Long   getRateValueAsLong  (int    rowId, String colname)                          { return getValueAsLong   (DATA_RATE, rowId, colname, true);      }
	public Long   getRateValueAsLong  (int    rowId, String colname, boolean cs)              { return getValueAsLong   (DATA_RATE, rowId, colname,   cs);      }
	public Long   getRateValueAsLong  (int    rowId, String colname, Long def)                { return getValueAsLong   (DATA_RATE, rowId, colname, true, def); }
	public Long   getRateValueAsLong  (int    rowId, String colname, boolean cs, Long def)    { return getValueAsLong   (DATA_RATE, rowId, colname,   cs, def); }
	public Long   getRateValueAsLong  (String pkStr, String colname)                          { return getValueAsLong   (DATA_RATE, pkStr, colname, true);      }
	public Long   getRateValueAsLong  (String pkStr, String colname, boolean cs)              { return getValueAsLong   (DATA_RATE, pkStr, colname,   cs);      }
	public Long   getRateValueAsLong  (String pkStr, String colname, Long def)                { return getValueAsLong   (DATA_RATE, pkStr, colname, true, def); }
	public Long   getRateValueAsLong  (String pkStr, String colname, boolean cs, Long def)    { return getValueAsLong   (DATA_RATE, pkStr, colname,   cs, def); }

	// NOTE: Do we really need this for RATE
	public Timestamp getRateValueAsTimestamp(int    rowId, int    colPos)                            { return getValueAsTimestamp(DATA_RATE, rowId, colPos);             }
	public Timestamp getRateValueAsTimestamp(int    rowId, int    colPos, Timestamp def)             { return getValueAsTimestamp(DATA_RATE, rowId, colPos, def);        }
	public Timestamp getRateValueAsTimestamp(int    rowId, String colname)                           { return getValueAsTimestamp(DATA_RATE, rowId, colname, true);      }
	public Timestamp getRateValueAsTimestamp(int    rowId, String colname, boolean cs)               { return getValueAsTimestamp(DATA_RATE, rowId, colname,   cs);      }
	public Timestamp getRateValueAsTimestamp(int    rowId, String colname, Timestamp def)            { return getValueAsTimestamp(DATA_RATE, rowId, colname, true, def); }
	public Timestamp getRateValueAsTimestamp(int    rowId, String colname, boolean cs, Timestamp def){ return getValueAsTimestamp(DATA_RATE, rowId, colname,   cs, def); }
	public Timestamp getRateValueAsTimestamp(String pkStr, String colname)                           { return getValueAsTimestamp(DATA_RATE, pkStr, colname, true);      }
	public Timestamp getRateValueAsTimestamp(String pkStr, String colname, boolean cs)               { return getValueAsTimestamp(DATA_RATE, pkStr, colname,   cs);      }
	public Timestamp getRateValueAsTimestamp(String pkStr, String colname, Timestamp def)            { return getValueAsTimestamp(DATA_RATE, pkStr, colname, true, def); }
	public Timestamp getRateValueAsTimestamp(String pkStr, String colname, boolean cs, Timestamp def){ return getValueAsTimestamp(DATA_RATE, pkStr, colname,   cs, def); }
	
	public Double getRateValueMax      (int    colPos)                                        { return getMaxValue      (DATA_RATE, null,  colPos);        }
	public Double getRateValueMax      (String colname)                                       { return getMaxValue      (DATA_RATE, null,  colname, true); }
	public Double getRateValueMax      (String colname, boolean cs)                           { return getMaxValue      (DATA_RATE, null,  colname,   cs); }
	
	public Double getRateValueMin      (int    colPos)                                        { return getMinValue      (DATA_RATE, null,  colPos);        }
	public Double getRateValueMin      (String colname)                                       { return getMinValue      (DATA_RATE, null,  colname, true); }
	public Double getRateValueMin      (String colname, boolean cs)                           { return getMinValue      (DATA_RATE, null,  colname,   cs); }
	
	public Double getRateValueAvg      (int    colPos)                                        { return getAvgValue      (DATA_RATE, null,  colPos);        }
	public Double getRateValueAvg      (String colname)                                       { return getAvgValue      (DATA_RATE, null,  colname, true); }
	public Double getRateValueAvg      (String colname, boolean cs)                           { return getAvgValue      (DATA_RATE, null,  colname,   cs); }
	
	public Double getRateValueAvgGtZero(int    colPos)                                        { return getAvgValueGtZero(DATA_RATE, null,  colPos);        }
	public Double getRateValueAvgGtZero(String colname)                                       { return getAvgValueGtZero(DATA_RATE, null,  colname, true); }
	public Double getRateValueAvgGtZero(String colname, boolean cs)                           { return getAvgValueGtZero(DATA_RATE, null,  colname,   cs); }
	
	public Double getRateValueSum      (int    colPos)                                        { return getSumValue      (DATA_RATE, null,  colPos);        }
	public Double getRateValueSum      (String colname)                                       { return getSumValue      (DATA_RATE, null,  colname, true); }
	public Double getRateValueSum      (String colname, boolean cs)                           { return getSumValue      (DATA_RATE, null,  colname,   cs); }
	
	public String getRatePkValue       (int    rowId)                                         { return getPkValue       (DATA_RATE, rowId  ); }
	public int    getRateRowIdForPkValue(String pkStr)                                        { return getRowIdForPkValue(DATA_RATE, pkStr); }
                                                                                  
	public int[]  getRateRowIdsWhere   (String colname, Object colval)                        { return getRowIdsWhere   (DATA_RATE, colname, true, colval); }
	public int[]  getRateRowIdsWhere   (String colname, boolean cs, Object colval)            { return getRowIdsWhere   (DATA_RATE, colname,   cs, colval); }
	
	public Double getRateValueMax      (int[] rowIds, String colname)                         { return getMaxValue      (DATA_RATE, rowIds,  colname, true); }
	public Double getRateValueMax      (int[] rowIds, String colname, boolean cs)             { return getMaxValue      (DATA_RATE, rowIds,  colname,   cs); }
	
	public Double getRateValueMin      (int[] rowIds, String colname)                         { return getMinValue      (DATA_RATE, rowIds,  colname, true); }
	public Double getRateValueMin      (int[] rowIds, String colname, boolean cs)             { return getMinValue      (DATA_RATE, rowIds,  colname,   cs); }
	
	public Double getRateValueAvg      (int[] rowIds, String colname)                         { return getAvgValue      (DATA_RATE, rowIds,  colname, true); }
	public Double getRateValueAvg      (int[] rowIds, String colname, boolean cs)             { return getAvgValue      (DATA_RATE, rowIds,  colname,   cs); }
	
	public Double getRateValueAvgGtZero(int[] rowIds, String colname)                         { return getAvgValueGtZero(DATA_RATE, rowIds,  colname, true); }
	public Double getRateValueAvgGtZero(int[] rowIds, String colname, boolean cs)             { return getAvgValueGtZero(DATA_RATE, rowIds,  colname,   cs); }
	
	public Double getRateValueSum      (int[] rowIds, String colname)                         { return getSumValue      (DATA_RATE, rowIds,  colname, true); }
	public Double getRateValueSum      (int[] rowIds, String colname, boolean cs)             { return getSumValue      (DATA_RATE, rowIds,  colname,   cs); }

	
	public List<Integer> getAbsRowIdsWhere (Map<String, Object> nameValue, boolean caseSensitive) { return getRowIdsWhere(DATA_ABS , nameValue, caseSensitive); }
	public List<Integer> getAbsRowIdsWhere (Map<String, Object> nameValue)                        { return getRowIdsWhere(DATA_ABS , nameValue, true); }

	public List<Integer> getDiffRowIdsWhere(Map<String, Object> nameValue, boolean caseSensitive) { return getRowIdsWhere(DATA_DIFF, nameValue, caseSensitive); }
	public List<Integer> getDiffRowIdsWhere(Map<String, Object> nameValue)                        { return getRowIdsWhere(DATA_DIFF, nameValue, true); }

	public List<Integer> getRateRowIdsWhere(Map<String, Object> nameValue, boolean caseSensitive) { return getRowIdsWhere(DATA_RATE, nameValue, caseSensitive); }
	public List<Integer> getRateRowIdsWhere(Map<String, Object> nameValue)                        { return getRowIdsWhere(DATA_RATE, nameValue, true); }

	/**
	 * Get all values that matches
	 * 
	 * @param nameValue   a map of column names/values that we are searching for
	 * @return            List of row numbers.   empty if no rows was found.
	 * @throws RuntimeException if any of the column names you searched for was not found
	 */
//	private int[] getRowIdsWhere(int whatData, String colname, boolean caseSensitive, String colvalue)
	private List<Integer> getRowIdsWhere(int whatData, Map<String, Object> nameValue, boolean caseSensitive)
	{
		List<Integer> foundRows = new ArrayList<Integer>();
		
		CounterTableModel data = null;

		if      (whatData == DATA_ABS)  data = getCounterDataAbs();
		else if (whatData == DATA_DIFF) data = getCounterDataDiff();
		else if (whatData == DATA_RATE) data = getCounterDataRate();
		else
			throw new RuntimeException("Only ABS, DIFF, or RATE data is available.");

		if (data == null)
			return foundRows;

		int rowc = getRowCount();
		for (int r=0; r<rowc; r++)
		{
			int matchColCount = 0;
			for (Entry<String, Object> e : nameValue.entrySet())
			{
				String whereColName = e.getKey();
				Object whereColVal  = e.getValue();
				
				int colId = data.findColumn(whereColName, caseSensitive);
				if (colId == -1)
					throw new RuntimeException("Can't find column '"+whereColName+"' in TableModel named '"+getName()+"'.");

				Object rowColVal = data.getValueAt(r, colId);
				
//				if (whereColVal == null && rowColVal == NULL_REPLACE)
				if (whereColVal == null && rowColVal == null)
				{
					matchColCount++;
				}
				else if (whereColVal != null)
				{
					if (whereColVal instanceof Number && rowColVal instanceof Number)
					{
						if (NumberUtils.compareValues((Number)whereColVal, (Number)rowColVal))
						{
							matchColCount++;
						}
					}
					else if (whereColVal.equals(rowColVal))
					{
						matchColCount++;
					}
				}
			}
			
			if (matchColCount == nameValue.size())
				foundRows.add(r);
		}
		
		return foundRows;
	}

	/**
	 * This is called from CounterSample when we see a "Duplicate Key"
	 * 
	 * @param counterSample   The CounterSample which this happened on
	 * @param keyStr          The PK
	 * @param curRow          Existing row
	 * @param newRow          New row (that causes the duplicate)
	 * 
	 * @return true for print WARNINGS, false to NOT printing any Warning message
	 */
	public boolean actionForSampleDuplicateKey(CounterSample counterSample, String keyStr, List<Object> curRow, List<Object> newRow)
	{
		return true;
	}

	public void setDataInitialized(boolean b)
	{
		_dataInitialized = b;
	}
	public boolean isDataInitialized()
	{
		return _dataInitialized;
	}


	public void setFirstTimeSample(boolean b)
	{
		_firstTimeSample = b;
	}
	public boolean isFirstTimeSample()
	{
		return _firstTimeSample;
	}


	public boolean hasResultSetMetaData()
	{
		return (_rsmdCached != null);
	}

	public void setResultSetMetaData(ResultSetMetaData rsmd)
	{
		if (rsmd instanceof ResultSetMetaDataCached)
		{
			_rsmdCached = (ResultSetMetaDataCached) rsmd;
		}
		else
		{
			try 
			{
				_rsmdCached = createResultSetMetaData(rsmd);
			}
			catch(SQLException ex)
			{
				_logger.warn("Problems creating a Cached ResultSetMetaData, continuing with the passed value.", ex);
			}
		}
	}

	public ResultSetMetaDataCached getResultSetMetaData()
	{
		return _rsmdCached;
	}

	public ResultSetMetaDataCached createResultSetMetaData(ResultSetMetaData rsmd)
	throws SQLException
	{
		// Copy/Clone the ResultSetMetaData some JDBC implementations needs this (otherwise we might get a 'The result set is closed', this was first seen for MS SQL-Server JDBC Driver)
		if (rsmd == null)
			return null;

		// Get DBMS Product which we are connected to
		String productName = null;

		if (rsmd instanceof HostMonitorMetaData)
		{
			productName = "HostMonitor";
		}
		else
		{
			productName = getCounterController().getMonConnection().getDatabaseProductName(); // This should be cached... if not then we hit severe performance bottleneck
		}
		
		
		// Create the new object
		ResultSetMetaDataCached originRsmd = new ResultSetMetaDataCached(rsmd, productName);

		// If the CM needs to "modify" anything in the ResultSet, rename columns or data type modifications
		originRsmd = modifyResultSetMetaData(originRsmd);
		
		// re-map any data types from the SOURCE DBMS to a more "normalized form", for example:
		//   - Sybase ASE: unsigned int          -->> bigint (and a bunch of other stuff)
		//   - Oracle:     number(...)           -->> int, bigint or similar
		//   - Postgres:   oid, int2, int4, int8 -->> better data types
		ResultSetMetaDataCached normalizedSourceRsmd = originRsmd.createNormalizedRsmd(productName);

		// Change CHAR into VARCHAR (due to less storage in PCS)
		if (changeJdbcCharIntoVarchar())
		{
			for (int i=0; i<normalizedSourceRsmd.getColumnCount(); i++)
			{
				int jdbcPos = i + 1;

				int jdbcType = normalizedSourceRsmd.getColumnType(jdbcPos);
				if (jdbcType == Types.CHAR)
				{
					normalizedSourceRsmd.setColumnType(jdbcPos, Types.VARCHAR);
					if (_logger.isDebugEnabled())
						_logger.debug("CM='" + getName() + "': Changing JDBC type from Types.CHAR to Types.VARCHAR for column '" + normalizedSourceRsmd.getColumnLabel(jdbcPos) + "'.");
				}
				if (jdbcType == Types.NCHAR)
				{
					normalizedSourceRsmd.setColumnType(jdbcPos, Types.NVARCHAR);
					if (_logger.isDebugEnabled())
						_logger.debug("CM='" + getName() + "': Changing JDBC type from Types.NCHAR to Types.NVARCHAR for column '" + normalizedSourceRsmd.getColumnLabel(jdbcPos) + "'.");
				}
			}
		}

		// return the Normalized Metadata Object
		return normalizedSourceRsmd;
	}

	/**
	 * Implement this if you want to rename columns or do data type modifications etc...
	 * 
	 * @param rsmdc    The ResultSetMetaData
	 * @return The new/modified ResultSetMetaDataCached (by default the base implementation just returns the input
	 */
	public ResultSetMetaDataCached modifyResultSetMetaData(ResultSetMetaDataCached rsmdc)
	{
		return rsmdc;
	}

	/**
	 * Transform Types.CHAR -> Types.VARCHAR  and  Types.NCHAR -> Types.NVARCHAR
	 * <p>
	 * This so the PCS (Persistent Counter Store) will minimize how many characters that are saved...
	 * <p>
	 * Simply override this in any CM that we do NOT want this behaviour
	 * 
	 * @return true == Do Transform. false == leave columns as CHAR or NCHAR
	 */
	public boolean changeJdbcCharIntoVarchar()
	{
		return true;
	}
	
	/**
	 * This is called when a PCS Database is about to be rolled over into a new database (timestamp)
	 * <p>
	 * In here we might want to do various stuff...
	 */
	public void prepareForPcsDatabaseRollover()
	{
		// Nothing done... any CM Implementation can override this.
	}


	private boolean _inLoadProps = false;
	protected void saveProps()
  	{
		Configuration tempProps = Configuration.getInstance(Configuration.USER_TEMP);
		String base = this.getName() + ".";

		// when we do setXXX in loadProps, those methods calls saveProps()
		// and when that happens it overwrite stuff, so this is a simple workaround...
		if (_inLoadProps)
			return;

		if (tempProps != null)
		{
			tempProps.setProperty(base + PROPKEY_currentDataSource,              getDataSource());
			tempProps.setProperty(base + PROPKEY_filterAllZeroDiffCounters,      isFilterAllZero());
			tempProps.setProperty(base + PROPKEY_sampleDataIsPaused,             isDataPollingPaused());
			tempProps.setProperty(base + PROPKEY_sampleDataInBackground,         isBackgroundDataPollingEnabled());
			tempProps.setProperty(base + PROPKEY_negativeDiffCountersToZero,     isNegativeDiffCountersToZero());
			tempProps.setProperty(base + PROPKEY_persistCounters,                isPersistCountersEnabled());
			tempProps.setProperty(base + PROPKEY_persistCounters_abs,            isPersistCountersAbsEnabled());
			tempProps.setProperty(base + PROPKEY_persistCounters_diff,           isPersistCountersDiffEnabled());
			tempProps.setProperty(base + PROPKEY_persistCounters_rate,           isPersistCountersRateEnabled());
			tempProps.setProperty(base + PROPKEY_nonConfiguredMonitoringAllowed, isNonConfiguredMonitoringAllowed());
			tempProps.setProperty(base + PROPKEY_highlightNewDateOrRateRows,     isNewDeltaOrRateRowHighlightEnabled());

			tempProps.setProperty(base + PROPKEY_postponeIsEnabled,              isPostponeEnabled());
			tempProps.setProperty(base + PROPKEY_postponeTime,                   getPostponeTime());
			tempProps.setProperty(base + PROPKEY_queryTimeout,                   getQueryTimeout());

			tempProps.save();
		}
	}

	protected void registerDefaultValues()
	{
		String base = this.getName() + ".";

		Configuration.registerDefaultValue(base + PROPKEY_queryTimeout,                   getDefaultQueryTimeout());

		Configuration.registerDefaultValue(base + PROPKEY_currentDataSource,              getDefaultDataSource());

		Configuration.registerDefaultValue(base + PROPKEY_filterAllZeroDiffCounters,      getDefaultIsFilterAllZero());
		Configuration.registerDefaultValue(base + PROPKEY_sampleDataIsPaused,             getDefaultIsDataPollingPaused());
		Configuration.registerDefaultValue(base + PROPKEY_sampleDataInBackground,         getDefaultIsBackgroundDataPollingEnabled());
		Configuration.registerDefaultValue(base + PROPKEY_negativeDiffCountersToZero,     getDefaultIsNegativeDiffCountersToZero());
		Configuration.registerDefaultValue(base + PROPKEY_persistCounters,                getDefaultIsPersistCountersEnabled());
		Configuration.registerDefaultValue(base + PROPKEY_persistCounters_abs,            getDefaultIsPersistCountersAbsEnabled());
		Configuration.registerDefaultValue(base + PROPKEY_persistCounters_diff,           getDefaultIsPersistCountersDiffEnabled());
		Configuration.registerDefaultValue(base + PROPKEY_persistCounters_rate,           getDefaultIsPersistCountersRateEnabled());
		Configuration.registerDefaultValue(base + PROPKEY_nonConfiguredMonitoringAllowed, getDefaultIsNonConfiguredMonitoringAllowed());
		Configuration.registerDefaultValue(base + PROPKEY_highlightNewDateOrRateRows,     getDefaultIsNewDeltaOrRateRowHighlightEnabled());

		Configuration.registerDefaultValue(base + PROPKEY_postponeIsEnabled,              getDefaultPostponeIsEnabled());
		Configuration.registerDefaultValue(base + PROPKEY_postponeTime,                   getDefaultPostponeTime());
	}

	protected void loadProps()
	{
//		Configuration tempProps = Configuration.getInstance(Configuration.TEMP);
//		Configuration confProps = Configuration.getInstance(Configuration.CONF);
		Configuration tempProps = Configuration.getCombinedConfiguration();
		Configuration confProps = Configuration.getCombinedConfiguration();
		String base = this.getName() + ".";

		if (confProps != null)
		{
			setQueryTimeout(confProps.getIntProperty(PROPKEY_queryTimeout, getQueryTimeout()));
			setQueryTimeout(confProps.getIntProperty(base + PROPKEY_queryTimeout, getQueryTimeout()));
		}

		if (tempProps != null)
		{
			_inLoadProps = true;

			setQueryTimeout(                     tempProps.getIntProperty(    base + PROPKEY_queryTimeout,                   getDefaultQueryTimeout()) );
                                                 
			setDataSource(                       tempProps.getIntProperty(    base + PROPKEY_currentDataSource,              getDefaultDataSource())                         ,false);
                                                                                                                                                                             
			setFilterAllZero(                    tempProps.getBooleanProperty(base + PROPKEY_filterAllZeroDiffCounters,      getDefaultIsFilterAllZero())                    ,false);
			setPauseDataPolling(                 tempProps.getBooleanProperty(base + PROPKEY_sampleDataIsPaused,             getDefaultIsDataPollingPaused())                ,false);
			setBackgroundDataPollingEnabled(     tempProps.getBooleanProperty(base + PROPKEY_sampleDataInBackground,         getDefaultIsBackgroundDataPollingEnabled())     ,false);
			setNegativeDiffCountersToZero(       tempProps.getBooleanProperty(base + PROPKEY_negativeDiffCountersToZero,     getDefaultIsNegativeDiffCountersToZero())       ,false);
			setPersistCounters(                  tempProps.getBooleanProperty(base + PROPKEY_persistCounters,                getDefaultIsPersistCountersEnabled())           ,false);
			setPersistCountersAbs(               tempProps.getBooleanProperty(base + PROPKEY_persistCounters_abs,            getDefaultIsPersistCountersAbsEnabled())        ,false);
			setPersistCountersDiff(              tempProps.getBooleanProperty(base + PROPKEY_persistCounters_diff,           getDefaultIsPersistCountersDiffEnabled())       ,false);
			setPersistCountersRate(              tempProps.getBooleanProperty(base + PROPKEY_persistCounters_rate,           getDefaultIsPersistCountersRateEnabled())       ,false);
			setNonConfiguredMonitoringAllowed(   tempProps.getBooleanProperty(base + PROPKEY_nonConfiguredMonitoringAllowed, getDefaultIsNonConfiguredMonitoringAllowed())   ,false);
			setNewDeltaOrRateRowHighlightEnabled(tempProps.getBooleanProperty(base + PROPKEY_highlightNewDateOrRateRows,     getDefaultIsNewDeltaOrRateRowHighlightEnabled()),false);

			setPostponeTime(                     tempProps.getIntProperty    (base + PROPKEY_postponeTime,                   getDefaultPostponeTime())                       ,false);

			_inLoadProps = false;
		}
	}

	public static final String PROPKEY_currentDataSource              = "currentDataSource";
	public static final String PROPKEY_filterAllZeroDiffCounters      = "filterAllZeroDiffCounters";
	public static final String PROPKEY_sampleDataIsPaused             = "sampleDataIsPaused";
	public static final String PROPKEY_sampleDataInBackground         = "sampleDataInBackground";
	public static final String PROPKEY_negativeDiffCountersToZero     = "negativeDiffCountersToZero";
	public static final String PROPKEY_persistCounters                = "persistCounters";
	public static final String PROPKEY_persistCounters_abs            = "persistCounters.abs";
	public static final String PROPKEY_persistCounters_diff           = "persistCounters.diff";
	public static final String PROPKEY_persistCounters_rate           = "persistCounters.rate";
	public static final String PROPKEY_postponeIsEnabled              = "postponeIsEnabled";
	public static final String PROPKEY_postponeTime                   = "postponeTime";
	public static final String PROPKEY_queryTimeout                   = "queryTimeout";
	public static final String PROPKEY_nonConfiguredMonitoringAllowed = "nonConfiguredMonitoringAllowed";
	public static final String PROPKEY_highlightNewDateOrRateRows     = "highlightNewDateOrRateRows";


	
	/*---------------------------------------------------
	**---------------------------------------------------
	**---------------------------------------------------
	**---- SUBCLASSES ---- SUBCLASES ---- SUBCLASES ----- 
	**---------------------------------------------------
	**---------------------------------------------------
	**---------------------------------------------------
	*/

	/**
	 * This timer is started just before we execute the SQL ststement that refreshes the data
	 * And it's stopped when the execution is finnished
	 * If X ms has elipsed in the database... show some info to any GUI that we are still in refresh... 
	 */
	private class RefreshTimerAction implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent actionevent)
		{
			if (_tabPanel != null)
			{
				// maybe use property change listeners instead: firePropChanged("status", "refreshing");

				long timeSpent = System.currentTimeMillis() - _refreshTimerStartTime;
				_tabPanel.setWatermarkText("Refreshing data, passed time "+TimeUtils.msToTimeStr("%MM:%SS.%ms", timeSpent));
			}
		}
	}

	/**
	 * Just a placeholder object that is used/stored in the List _dependsOnStoredProc
	 */
	private class StoredProcCheck
	{
		String   _dbname;               // in what database should the proc be in
		String   _procName;             // name of the proc
		Date     _procDateThreshold;    // recreate if the proc is created "earlier" than this date
		Class<?> _scriptLocation;       // in what "directory" (actually a classname) do we find the script 
		String   _scriptName;           // name of the script (from within the jar file or classpath)
		String   _needsRoleToRecreate;  // what ROLE inside ASE server do we need to create this proc
		long     _needSrvVersion;       // no need to create proc if server is below this version

		StoredProcCheck(String dbname, String procName, Date procDateThreshold, 
				Class<?> scriptLocation, String scriptName, String needsRoleToRecreate, long needSrvVersion)
		{
			_dbname              = dbname;
			_procName            = procName;
			_procDateThreshold   = procDateThreshold;
			_scriptLocation      = scriptLocation;
			_scriptName          = scriptName;
			_needsRoleToRecreate = needsRoleToRecreate;
			_needSrvVersion      = needSrvVersion;
		}
	}

	public int     getDefaultQueryTimeout()                             { return DEFAULT_sqlQueryTimeout; }
	public int     getDefaultPostponeTime()                             { return DEFAULT_postponeTime; }
	public boolean getDefaultPostponeIsEnabled()                        { return DEFAULT_postponeIsEnabled; }
	public boolean getDefaultIsFilterAllZero()                          { return false; }
	public boolean getDefaultIsDataPollingPaused()                      { return false; }
	public boolean getDefaultIsBackgroundDataPollingEnabled()           { return false; }
	public boolean getDefaultIsNegativeDiffCountersToZero()             { return true; }

	public boolean getDefaultIsPersistCountersEnabled()                 { return false; }
	public boolean getDefaultIsPersistCountersAbsEnabled()              { return true; }
	public boolean getDefaultIsPersistCountersDiffEnabled()             { return true; }
	public boolean getDefaultIsPersistCountersRateEnabled()             { return true; }

	/** Get what template level this CM should be part of. the level can be <code>SMALL, MEDIUM, LARGE, ALL or OFF</code> */
	public Type    getTemplateLevel()                                   { return Type.ALL; }




	//-------------------------------------------------------
	// BEGIN: NON CONFIGURED MONITORING 
	//-------------------------------------------------------
	private boolean            _isNonConfiguredMonitoringAllowed       = false;
	private boolean            _hasNonConfiguredMonitoringHappened     = false;
	private LinkedList<String> _lastNonConfiguredMonitoringMessageList = null;
	private String             _nonConfiguredMonitoringMissingParams   = null;

	public boolean getDefaultIsNonConfiguredMonitoringAllowed()
	{
		return false;
	}
	public boolean isNonConfiguredMonitoringAllowed()
	{
		return _isNonConfiguredMonitoringAllowed;
	}
	public boolean setNonConfiguredMonitoringAllowed(boolean isItAllowed)
	{
		return _isNonConfiguredMonitoringAllowed = isItAllowed;
	}
	public void setNonConfiguredMonitoringAllowed(boolean isItAllowed, boolean saveProps)
	{
		// No need to continue if we are not changing it
		if (isNonConfiguredMonitoringAllowed() == isItAllowed)
			return;

		setNonConfiguredMonitoringAllowed(isItAllowed);
		if (saveProps)
			saveProps();
	}


//	public ArrayList<INonConfiguredMonitoring> _nonConfiguredMonitoringListeners = new ArrayList<INonConfiguredMonitoring>();
//	public void removeNonConfiguredMonitoringHappenedListener(INonConfiguredMonitoring listener)
//	{
//		_nonConfiguredMonitoringListeners.add(listener);
//	}
//	public void addNonConfiguredMonitoringHappenedListener(INonConfiguredMonitoring)
//	{
//		_nonConfiguredMonitoringListeners.remove(listener);
//	}
	public void fireNonConfiguredMonitoringHappened()
	{
//		for (INonConfiguredMonitoring listener : _nonConfiguredMonitoringListeners)
//			listener.someMethodInTheListener();

		_logger.warn("CM: name='"+getName()+"', hasNonConfiguredMonitoringHappened()=true, getNonConfiguedMonitoringMessage='"+getNonConfiguredMonitoringMessage(false)+"'.");

		// the below is done within the watermark check
//		TabularCntrPanel tcp = getTabPanel();
//		if (tcp != null)
//		{
//			tcp.setNonConfiguredMonitoring(...);
//		}
	}

	public boolean hasNonConfiguredMonitoringHappened()
	{
		return _hasNonConfiguredMonitoringHappened;
	}
	public void setNonConfiguredMonitoringHappened(boolean hasHappen)
	{
		// Reset the message structure if value is false
		if (hasHappen == false)
		{
			resetNonConfiguredMonitoringMessage();
			resetNonConfiguredMonitoringMissingParams();
		}

		_hasNonConfiguredMonitoringHappened = hasHappen;
	}

	public void addNonConfiguedMonitoringMessage(String message)
	{
		if (message == null)
			return;

		if (_lastNonConfiguredMonitoringMessageList == null)
			_lastNonConfiguredMonitoringMessageList = new LinkedList<String>();

		message = StringUtil.removeLastNewLine(message);

		_lastNonConfiguredMonitoringMessageList.add(message);
	}
	public void resetNonConfiguredMonitoringMessage()
	{
		_lastNonConfiguredMonitoringMessageList = null;
	}
	/** @returns a string, if empty a "" string will be returned */
	public String getNonConfiguredMonitoringMessage(boolean asHtmlStr)
	{
		if (_lastNonConfiguredMonitoringMessageList == null)     return "";
		if (_lastNonConfiguredMonitoringMessageList.size() == 0) return "";
		if (_lastNonConfiguredMonitoringMessageList.size() == 1) return _lastNonConfiguredMonitoringMessageList.get(0);
		
		StringBuilder sb = new StringBuilder();
		for (String msg : _lastNonConfiguredMonitoringMessageList)
			sb.append(msg).append("\n");
		return sb.toString();
	}
	public List<String> getNonConfiguedMonitoringMessageList()
	{
		return _lastNonConfiguredMonitoringMessageList;
	}

	public void resetNonConfiguredMonitoringMissingParams()
	{
		_nonConfiguredMonitoringMissingParams = null;
	}
	public void setNonConfiguredMonitoringMissingParams(String params)
	{
		_nonConfiguredMonitoringMissingParams = params;
	}
	public String getNonConfiguredMonitoringMissingParams()
	{
		if (_nonConfiguredMonitoringMissingParams == null)     
			return "";
		return _nonConfiguredMonitoringMissingParams;
	}
	//-------------------------------------------------------
	// END: NON CONFIGURED MONITORING 
	//-------------------------------------------------------


	//-------------------------------------------------------
	// BEGIN: HIGHLIGHT NEW DIFF/RATE ROWS 
	//-------------------------------------------------------
	private boolean[] _isNewDeltaOrRateRow = null;
	private boolean   _isNewDeltaOrRateRowHighlightEnabled = true;

	public boolean getDefaultIsNewDeltaOrRateRowHighlightEnabled()
	{
		return true;
	}
	public boolean isNewDeltaOrRateRowHighlightEnabled()
	{
		return _isNewDeltaOrRateRowHighlightEnabled;
	}
	public boolean setNewDeltaOrRateRowHighlightEnabled(boolean isItAllowed)
	{
		return _isNewDeltaOrRateRowHighlightEnabled = isItAllowed;
	}
	public void setNewDeltaOrRateRowHighlightEnabled(boolean toValue, boolean saveProps)
	{
		// No need to continue if we are not changing it
		if (isNewDeltaOrRateRowHighlightEnabled() == toValue)
			return;

		setNewDeltaOrRateRowHighlightEnabled(toValue);
		if (saveProps)
			saveProps();
	}

	public boolean isNewDeltaOrRateRow(int mrow)
	{
		if (_isNewDeltaOrRateRow == null)
			return false;

		if (mrow < 0 || mrow >= _isNewDeltaOrRateRow.length)
		{
			_logger.warn("isNewDeltaOrRateRow(mrow="+mrow+"): is out of range: _isNewDeltaOrRateRow.length="+_isNewDeltaOrRateRow.length);
			return false;
		}
		// For development purposes
		//if (mrow == 10)
		//	return true;
		
		return _isNewDeltaOrRateRow[mrow];
	}

	public void setNewDeltaOrRateRow(int mrow, boolean isNewDeltaOrRateRow)
	{
		if (_isNewDeltaOrRateRow == null)
			_isNewDeltaOrRateRow = new boolean[100]; // default values: false...

//		ArrayList<Integer> xxx = new ArrayList<Integer>();
//		xxx.add(111);
		
		// If the array is to small, expand it
		int curentSize = _isNewDeltaOrRateRow.length;
		if (mrow >= curentSize) 
		{
			int newSize = (curentSize*3)/2 + 1;
			_isNewDeltaOrRateRow = Arrays.copyOf(_isNewDeltaOrRateRow, newSize);
		}

		if (mrow >= 0 && mrow < _isNewDeltaOrRateRow.length)
			_isNewDeltaOrRateRow[mrow] = isNewDeltaOrRateRow;
		else
			_logger.warn("computeDiffCnt(): cm='"+getName()+"', _isNewDeltaOrRateRow[newRowId="+mrow+"]=FALSE, _isNewDeltaOrRateRow.length="+_isNewDeltaOrRateRow.length+".");
	}

	
	//-------------------------------------------------------
	// BEGIN: toJson and to toXml 
	//-------------------------------------------------------
	public String toXml()
	{
		return "NOT-YET_IMPLEMENTED";
	}

	//-------------------------------------------------------
	// BEGIN: toJson and to toXml 
	//-------------------------------------------------------
	/**
	 * output
	 * <pre>
	 * "absCounter|diffCounter|rateCounter" :
	 * [
	 *     {
	 *         "col1" : "somedata",
	 *         "col2" : "somedata",
	 *         "col3" : "somedata"
	 *     },
	 *     {
	 *         "col1" : "somedata",
	 *         "col2" : "somedata",
	 *         "col3" : "somedata"
	 *     },
	 *     {
	 *         "col1" : "somedata",
	 *         "col2" : "somedata",
	 *         "col3" : "somedata"
	 *     }
	 * ]
	 * </pre>
	 * @param gen
	 * @param type
	 * @throws IOException
	 */
//---------------------------------------------------------
// The below id for GSON Writer
//---------------------------------------------------------
//	protected void writeJsonCounterSample(JsonWriter w, int type)
//			throws IOException
//	{
//		// Write the TYPE
//		String counterType;
//		if      (type == DATA_ABS)  counterType = "absCounters";
//		else if (type == DATA_DIFF) counterType = "diffCounters";
//		else if (type == DATA_RATE) counterType = "rateCounters";
//		else throw new IOException("Unknown type="+type);
//
//		// Set name
//		w.name(counterType);
//		
//		// Write an array of row objects: [ 
//		//                                  { "c1":"data", "c2":"data", "c3":"data" }, 
//		//                                  { "c1":"data", "c2":"data", "c3":"data" } 
//		//                                ]  
//		w.beginArray();
//		int rowc = getRowCount(type);
//		int colc = getColumnCount(type);
//		for (int r=0; r<rowc; r++)
//		{
//			w.beginObject();
//			for (int c=0; c<colc; c++)
//			{
//				Object obj  = getValue(type, r, c);
//				String name = getColumnName(c);  
//				
//				if (name == null)
//					throw new IOException("When writing JSON CM='"+getName()+"', CounterType="+type+", row="+r+", col="+c+", Column Name was 'null' (not set).");
//
//				w.name(name);
//				if (obj == null)
//					w.nullValue();
//				else
//				{
//					if      (obj instanceof Number)  w.value( (Number)  obj );
//					else if (obj instanceof Boolean) w.value( (Boolean) obj );
//					else                             w.value( obj.toString() );
//				}
//			}
//			w.endObject();
//		}
//		w.endArray();		
//	}
	protected void writeJsonCounterSample(JsonGenerator gen, int type)
			throws IOException
	{
		// Write the TYPE
		String counterType;
		if      (type == DATA_ABS)  counterType = "absCounters";
		else if (type == DATA_DIFF) counterType = "diffCounters";
		else if (type == DATA_RATE) counterType = "rateCounters";
		else throw new IOException("Unknown type="+type);

		// Set name
		gen.writeFieldName(counterType);
		
		// Write an array of row objects: [ 
		//                                  { "c1":"data", "c2":"data", "c3":"data" }, 
		//                                  { "c1":"data", "c2":"data", "c3":"data" } 
		//                                ]  
		gen.writeStartArray();
		int rowc = getRowCount(type);
		int colc = getColumnCount(type);
		for (int r=0; r<rowc; r++)
		{
			gen.writeStartObject();
			for (int c=0; c<colc; c++)
			{
				Object obj  = getValue(type, r, c);
				String name = getColumnName(c);  
				
				if (name == null)
					throw new IOException("When writing JSON CM='"+getName()+"', CounterType="+type+", row="+r+", col="+c+", Column Name was 'null' (not set).");

				gen.writeFieldName(name);
				if (obj == null)
					gen.writeNull();
				else
				{
					if      (obj instanceof Number)  gen.writeNumber ( obj.toString() );
					else if (obj instanceof Boolean) gen.writeBoolean( (Boolean) obj  );
					else                             gen.writeString ( obj.toString() );
				}
			}
			gen.writeEndObject();
		}
		gen.writeEndArray();		
	}
	
	public static CountersModel parseJson(String json)
	{
		throw new RuntimeException("NOT-YET-IMPLEMENTED");
//		JsonElement je = new JsonParser().parse(json);
//		if (je.isJsonObject())
//		{
//			JsonObject jo = je.getAsJsonObject();
//			JsonElement cmName = jo.get("cmName");
//			if (cmName != null)
//				cmName.
//			
//			jo = jo.getAsJsonObject("data");
//			JsonArray jarray = jo.getAsJsonArray("translations");
//			jo = jarray.get(0).getAsJsonObject();
//			String result = jo.get("translatedText").toString();
//			return result;
//		}
	}
	
	public void toJson(JsonGenerator gen, JsonCmWriterOptions writerOptions)
	throws IOException
	{
		// Previously we had various check if we should "send" or not, in PersistContainer.toJsonMessage(...)
		// Now I have moved them a bit "closer" to where they are written
		// For the moment do not send Append Models
		if (this instanceof CountersModelAppend) 
		{
			if (_logger.isDebugEnabled())
				_logger.debug(getName() + ": toJson(): Skipping due to: instance is 'CountersModelAppend'.");
			return;
		}

		if ( ! isActive() )
		{
			if ("CmSummary".equals(getName()))
			{
				// NOTE: CmSummary is always disabled (I don't know why... I can probably fix that by override isActive() in CmSummary...)
				// But I didn't know the consequences so (for now) I choose to do it like this...
				// BUT: I need to look at this: so we can trust isActive() in more cases without doing "exceptions" for CmSummary
			}
			else
			{
				if (_logger.isDebugEnabled())
					_logger.debug(getName() + ": toJson(): Skipping due to: isActive() is FALSE");
				return;
			}
		}

//		if ( ! hasValidSampleData() )
//		{
//			if (_logger.isDebugEnabled())
//				_logger.debug(getName() + ": toJson(): Skipping due to: hasValidSampleData() is FALSE");
//			return;
//		}

//		if ( ! hasData() && ! hasTrendGraphData() )
//		{
//			if (_logger.isDebugEnabled())
//				_logger.debug(getName() + ": toJson(): Skipping due to: hasData()==FALSE && hasTrendGraphData()==FALSE");
//			return;
//		}

//		// if we ONLY write graph data, but there is no graphs with data
//		// or possibly move this check INTO the cm method: cm.toJson(w, writeOptions);
//		if ( writerOptions.writeCounters == false && (writerOptions.writeGraphs && getTrendGraphCountWithData() == 0))
//		{
//			if (_logger.isDebugEnabled())
//				_logger.debug(getName() + ": toJson(): Skipping due to: writeCounters=FALSE && writeGraphs==TRUE but getTrendGraphCountWithData()==ZERO");
//			return;
//		}
//
		// Check some other parameters
		if (writerOptions.throwOnMissingMandatoryParams)
		{
			if (getSampleTime() == null) throw new NullPointerException("When writing CM '"+getName()+"' to JSON. 'sessionSampleTime' value was NULL, which is mandatory.");
			if (getTimestamp()  == null) throw new NullPointerException("When writing CM '"+getName()+"' to JSON. 'cmSampleTime' value was NULL, which is mandatory.");
		}
		else
		{
			boolean doReturn = false;
			if (getSampleTime() == null) { doReturn = true; _logger.warn("When writing CM '"+getName()+"' to JSON. 'sessionSampleTime' value was NULL, which is mandatory, skipping this CM and continue with next.."); }
			if (getTimestamp()  == null) { doReturn = true; _logger.warn("When writing CM '"+getName()+"' to JSON. 'cmSampleTime' value was NULL, which is mandatory, skipping this CM and continue with next..."); }
			
			if (doReturn)
				return;
		}


		gen.writeStartObject(); // START: this CS

		gen.writeStringField("cmName",            getName());
		gen.writeStringField("sessionSampleTime", TimeUtils.toStringIso8601(getSampleTime()));
		gen.writeStringField("cmSampleTime",      TimeUtils.toStringIso8601(getTimestamp()));
		gen.writeNumberField("cmSampleMs",        getLastSampleInterval());
		gen.writeStringField("type",              isSystemCm() ? "SYSTEM" : "USER_DEFINED");

		// Write some statistical fields
//		boolean writeStats = true;
//		if (writeStats)
		if (writerOptions.writeStats)
		{
			gen.writeFieldName("sampleDetails");
			gen.writeStartObject();

			gen.writeNumberField ("graphCount",           getTrendGraphCount());
			gen.writeNumberField ("graphCountWithData",   getTrendGraphCountWithData());
			gen.writeNumberField ("absRows",              getAbsRowCount());
			gen.writeNumberField ("diffRows",             getDiffRowCount());
			gen.writeNumberField ("rateRows",             getRateRowCount());

			gen.writeNumberField ("sqlRefreshTime",       getSqlRefreshTime());
			gen.writeNumberField ("guiRefreshTime",       getGuiRefreshTime());
			gen.writeNumberField ("lcRefreshTime",        getLcRefreshTime());
			gen.writeBooleanField("hasNonConfiguredMonitoringHappened",   hasNonConfiguredMonitoringHappened());
			gen.writeStringField ("nonConfiguredMonitoringMissingParams", getNonConfiguredMonitoringMissingParams());
			gen.writeStringField ("nonConfiguredMonitoringMessage",       getNonConfiguredMonitoringMessage(false));
			gen.writeBooleanField("isCountersCleared",    isCountersCleared());
			gen.writeBooleanField("hasValidSampleData",   hasValidSampleData());
			gen.writeStringField ("exceptionMsg",         getSampleException() == null ? null : getSampleException().toString());
			gen.writeStringField ("exceptionFullText",    StringUtil.exceptionToString(getSampleException()));
			
			gen.writeEndObject(); // END: Counters
		}

		// If has collected data, BUT we DO NOT want to send Counters & there are NO graphs to send (we can stop here)
		// meaning: we just send "what" we have collected, or just the "header"...
		if ( writerOptions.writeCounters == false && (writerOptions.writeGraphs && getTrendGraphCountWithData() == 0))
		{
			if (_logger.isDebugEnabled())
				_logger.debug(getName() + ": toJson(): Skipping due to: writeCounters=FALSE && writeGraphs==TRUE but getTrendGraphCountWithData()==ZERO");

			gen.writeEndObject(); // END: this CM

			return;
		}

		
		// JSON: counters
		if (writerOptions.writeCounters && hasData())
		{
			gen.writeFieldName("counters");
			gen.writeStartObject();

			// JSON: metaData
			if (writerOptions.writeMetaData && hasResultSetMetaData())
			{
				ResultSetMetaDataCached rsmd = (ResultSetMetaDataCached) getResultSetMetaData();
				try
				{
					gen.writeFieldName("metaData");
					gen.writeStartArray(); 

					// { "colName" : "someColName", "jdbcTypeName" : "java.sql.Types.DECIMAL", "guessedDbmsType" : "decimal(16,1)" }
					for (int c=1; c<=rsmd.getColumnCount(); c++) // Note: ResultSetMetaData starts at 1 not 0
					{
						gen.writeStartObject();
						
						// Always Write Column Name
						gen.writeStringField ("columnName"     , rsmd.getColumnLabel(c));

						// Write JDBC info
						if (writerOptions.writeMetaData_jdbc)
						{
							gen.writeStringField ("jdbcTypeName"   , ResultSetTableModel.getColumnJavaSqlTypeName(rsmd.getColumnType(c)));
							gen.writeStringField ("javaClassName"  , rsmd.getColumnClassName(c));
							gen.writeStringField ("guessedDbmsType", ResultSetTableModel.getColumnTypeName(rsmd, c));
						}

						// Write Counter Model info
						if (writerOptions.writeMetaData_cm)
						{
							gen.writeBooleanField("isDiffColumn"   , isDiffColumn(c-1)); // column pos starts at 0 in the CM
							gen.writeBooleanField("isPctColumn"    , isPctColumn(c-1));  // column pos starts at 0 in the CM
						}
						gen.writeEndObject();
					}
					gen.writeEndArray(); 
				}
				catch (Exception ex)
				{
					_logger.error("Write JSON JDBC MetaData data, for CM='"+getName()+"'. Caught: "+ex, ex);
				}
			}

			if (hasAbsData() && writerOptions.writeCounters_abs)
				writeJsonCounterSample(gen, DATA_ABS);

			if (hasDiffData() && writerOptions.writeCounters_diff)
				writeJsonCounterSample(gen, DATA_DIFF);

			if (hasRateData() && writerOptions.writeCounters_rate)
				writeJsonCounterSample(gen, DATA_RATE);

			gen.writeEndObject(); // END: Counters
		}

//System.out.println("                graph [srvName='"+getServerName()+"', CmName="+StringUtil.left(getName(),30)+"]: writeGraphs="+writeGraphs+", hasTrendGraphData()="+hasTrendGraphData());
//		if (writeGraphs && hasTrendGraphData()) // note use 'hasTrendGraphData()' and NOT 'hasTrendGraph()' which is only true in GUI mode
		if (writerOptions.writeGraphs && hasTrendGraphData()) // note use 'hasTrendGraphData()' and NOT 'hasTrendGraph()' which is only true in GUI mode
		{
			gen.writeFieldName("graphs");
			gen.writeStartArray(); 
			for (String graphName : getTrendGraphData().keySet())
			{
				TrendGraphDataPoint tgdp = getTrendGraphData(graphName);
				
//System.out.println("                graph [srvName='"+getServerName()+"', CmName="+StringUtil.left(getName(),30)+", graphName="+StringUtil.left(tgdp.getName(),30)+"]: tgdp.hasData()="+tgdp.hasData()+", data="+StringUtil.toCommaStr(tgdp.getData())+", labels="+StringUtil.toCommaStr(tgdp.getLabel()));
				// Do not write empty graphs
				if ( ! tgdp.hasData() )
					continue;

				gen.writeStartObject();
				gen.writeStringField ("cmName" ,           getName());
				gen.writeStringField ("sessionSampleTime", TimeUtils.toStringIso8601(getSampleTime()));

				gen.writeStringField ("graphName" ,     tgdp.getName());
				gen.writeStringField ("graphLabel",     tgdp.getGraphLabel());
				gen.writeStringField ("graphProps",     tgdp.getGraphProps());
				gen.writeStringField ("graphCategory",  tgdp.getCategory().toString());
				gen.writeBooleanField("percentGraph",   tgdp.isPercentGraph());
				gen.writeBooleanField("visibleAtStart", tgdp.isVisibleAtStart());
				gen.writeFieldName("data");
//				w.writeStartArray(); 
//				// loop all data
//				Double[] dataArr  = tgdp.getData();
//				String[] labelArr = tgdp.getLabel();
////				if (dataArr  == null) throw new IllegalArgumentException("The CM '"+getName()+"', graph '"+tgdp.getName()+"' has a null pointer for it's DATA array.");
////				if (labelArr == null) throw new IllegalArgumentException("The CM '"+getName()+"', graph '"+tgdp.getName()+"' has a null pointer for it's LABEL array.");
//				if (dataArr != null && labelArr != null)
//				{
//					for (int d=0; d<dataArr.length; d++)
//					{
//						Double data  = dataArr[d];
//						String label = null;
//						if (d < labelArr.length)
//							label = labelArr[d];
//
//						if (data == null)
//						{
//							_logger.warn("Writing JSON Graph, data was null, setting it to 0. For cm='"+getName()+"', graphName='"+graphName+"', label='"+label+"', data="+data);
//							data = 0d;
//						}
//
//						w.writeStartObject();
//
//						w.writeStringField("label",     label);
//						w.writeNumberField("dataPoint", data);
//
//						w.writeEndObject();
//					}
//				}
//				w.writeEndArray(); 

				gen.writeStartObject(); // BEGIN: data

				Double[] dataArr  = tgdp.getData();
				String[] labelArr = tgdp.getLabel();
				if (dataArr != null && labelArr != null)
				{
					for (int d=0; d<dataArr.length; d++)
					{
						Double data  = dataArr[d];
						String label = null;
						if (d < labelArr.length)
							label = labelArr[d];

						if (StringUtil.isNullOrBlank(label))
						{
System.out.println("DEBUG: Writing JSON Graph, LABEL was NULL or blank '" + label + "', setting it to 'lbl-"+d+"'. For cm='"+getName()+"', graphName='"+graphName+"', label='"+label+"', data="+data);
							if (_logger.isDebugEnabled())
								_logger.debug("Writing JSON Graph, LABEL was NULL or blank '" + label + "', setting it to 'lbl-"+d+"'. For cm='"+getName()+"', graphName='"+graphName+"', label='"+label+"', data="+data);
							label = "lbl-"+d;
						}

						if (data == null)
						{
							if (_logger.isDebugEnabled())
								_logger.debug("Writing JSON Graph, DATA was null, setting it to 0. For cm='"+getName()+"', graphName='"+graphName+"', label='"+label+"', data="+data);
							data = 0d;
						}

						gen.writeNumberField(label, data);
					}
				}
				gen.writeEndObject(); // END: data
				
				gen.writeEndObject(); // END: GraphName
			}
			gen.writeEndArray(); 
		}

		gen.writeEndObject(); // END: this CM
	}


//--------------------------------------------------------------
// How a JSON might look like
//--------------------------------------------------------------
//	{
//		"name" : "CmExample",
//		"sampleIntervall" : 111111,
//		"sampleTime" : "2017-01-01 11:22:33.123",
//		"type" : "system|userdefined"
//	
//		"MetaData" : 
//		{
//			[
//				{ "colName" : "someColName", "jdbcTypeName" : "java.sql.Types.DECIMAL", "guessedDbmsType" : "decimal(16,1)" }
//			]
//		},
//		"counters" :
//		{
//			"absCounters" :
//			[
//				{ "colname" : "XxxXxxx", "data" : "someVal" },
//				{ "colname" : "XxxXxxx", "data" : "someVal" },
//				{ "colname" : "XxxXxxx", "data" : "someVal" },
//				{ "colname" : "XxxXxxx", "data" : "someVal" },
//			],
//			"diffCounters" :
//			[
//			],
//			"rateCounters" :
//			[
//			],
//		}
//		"graphs" :
//		[
//			{ 
//				"name" : "xxxxx",
//				"data" : 
//				[
//					{"label" : "label-1", "dataPoint" : 1.1 },
//					{"label" : "label-2", "dataPoint" : 2.2 },
//					{"label" : "label-3", "dataPoint" : 3.3 },
//					{"label" : "label-4", "dataPoint" : 4.4 },
//					{"label" : "label-5", "dataPoint" : 5.5 }
//				]
//			}
//		]
//	}
	
	
	
	//-------------------------------------------------------
	// BEGIN: Aggregation 
	//-------------------------------------------------------

	/** MetatData for Aggregated Columns */
	private Map<String, AggregationType> _aggregateColumns = null;

	/** The actual Aggregated ROW ID, or row number */
	private int _aggregatedRowId = -1;

	private void private_resetAggregates()
	{
		_aggregateColumns    = null;
		_aggregatedRowId     = -1;
	}

	private void private_copyAggregates(CountersModel copy)
	{
		copy._aggregateColumns    = this._aggregateColumns;
		copy._aggregatedRowId     = this._aggregatedRowId;
	}


	/**
	 * Implemented by any CM that want/needs Column Aggregation
	 * @return
	 */
	public Map<String, AggregationType> createAggregateColumns()
	{
		return null;
	}

	/**
	 * Get the Map of columns that want/needs Column Aggregation 
	 * <p>
	 * If none exists, we will call createAggregateColumns(), which is responsible for creating the columns that any CM want/needs to be aggregated
	 * @return
	 */
	public Map<String, AggregationType> getAggregateColumns()
	{
		// Create if we havn't done that yet (and if createSummaryColumns() returns null... we do not have any SUM Columns
		if (_aggregateColumns == null)
		{
			_aggregateColumns = createAggregateColumns();
		}
		return _aggregateColumns;
	}

	/**
	 * Does re-calculate aggregation
	 *
	 * @param counterType 
	 * @param cs
	 */
	public void reCalculateAggregateRow(int counterType, CounterSample cs)
	{
		calculateAggregateRow(counterType, cs, true); // true == doRecalculate
	}

	/**
	 * Does the actual aggregation
	 *
	 * @param counterType 
	 * @param cs
	 * @param doReCalc   If we want to re-calculate
	 */
	private void calculateAggregateRow(int counterType, CounterSample cs, boolean doReCalc)
	{
		Map<String, AggregationType> aggCols = getAggregateColumns();

		if (aggCols == null)
			return;
		if (aggCols.isEmpty())
			return;

		int colCount = cs.getColumnCount();
		int rowCount = cs.getRowCount();

		// For DIFF and RATE values only do MIN/MAX
		if ( ! doReCalc )
		{
			if (counterType == DATA_DIFF || counterType == DATA_RATE)
			{
				for (int c = 0; c < colCount; c++)
				{
					String colName  = cs.getColumnName(c);
					int    jdbcType = cs.getColSqlType(c);

					AggregationType aggType = aggCols.get(colName);
					if (aggType != null)
					{
						AggregationType.Agg aggregationType = aggType.getAggregationType();

						if (AggregationType.Agg.MIN.equals(aggregationType) || AggregationType.Agg.MAX.equals(aggregationType))
						{
							Object dataValue = null;

							// Loop all rows and do SUM
							for (int r = 0; r < rowCount; r++)
							{
								Object colVal = cs.getValueAsObject(r, c);

								if (AggregationType.Agg.MIN.equals(aggregationType))
									dataValue = privateAggregate_doMinForValue(dataValue, colVal, jdbcType);

								if (AggregationType.Agg.MAX.equals(aggregationType))
									dataValue = privateAggregate_doMaxForValue(dataValue, colVal, jdbcType);
							}

							cs.setValueAt(dataValue, getAggregatedRowId(), c);
						}
					}
				}
				return;
			}
		}

//System.out.println("");
//System.out.println(getName() + ": >>> Entring calculateSummaryRow(): pkCols=" + getPk() + ", aggCols=" + aggCols);
		
		// loop columns
		// - if It's part of PK -->> add '_Total' (if it has any String columns)
		// - if it's a SummaryColumn:
		//      -> Check for what data type
		// 
		//      - Loop rows: and do SUM on all rows

		// Create a new List where we will PUT values for ALL columns
		List<Object> aggRow = new ArrayList<>(colCount);

		// If any of the AVG columns needs post processing (has installed callback)
		boolean needsPostCalculation = false;
		
		// loop columns
		// If part of PK, do SUM
		// else add NULL value
		for (int c = 0; c < colCount; c++)
		{
			Object addValue = null;
			String colName  = cs.getColumnName(c);
			int    jdbcType = cs.getColSqlType(c);

			AggregationType aggType = aggCols.get(colName);
			if (aggType != null)
			{
				Object sumValue = null;
				Object minValue = null;
				Object maxValue = null;
				int    rowCountForAverageCalculation = 0;
				AggregationType.Agg aggregationType = aggType.getAggregationType();

				// If we have installed call backs, mark that we need to do them AFTER all Aggregations are DONE.
				if (aggType.hasAvgCallbacks())
					needsPostCalculation = true;

				if (privateAggregate_isSummarizableForJdbcType(jdbcType))
				{
					// Loop all rows and do SUM
					for (int r = 0; r < rowCount; r++)
					{
						// (on re-calc) do NOT include values for the aggregated row (that would be strange) 
						if (r == _aggregatedRowId)
							continue;

						Object colVal = cs.getValueAsObject(r, c);
						sumValue = privateAggregate_doSummaryForValue(sumValue, colVal, jdbcType);
						minValue = privateAggregate_doMinForValue    (minValue, colVal, jdbcType);
						maxValue = privateAggregate_doMaxForValue    (maxValue, colVal, jdbcType);
//						System.out.println("calculateAggregateRow("+newSample.getName()+"): r="+r+", colName=|"+getColumnName(c)+"|, colVal=|"+colVal+"|, sumValue="+sumValue+", minValue="+minValue+", maxValue="+maxValue+", jdbcType="+jdbcType);

						if (colVal != null && AggregationType.Agg.AVG.equals(aggregationType))
						{
							if (aggType.isAverageTreatZeroAsNull())
							{
								if (colVal instanceof Number)
								{
									if (((Number)colVal).floatValue() != 0f)
										rowCountForAverageCalculation++;
								}
							}
							else
							{
								rowCountForAverageCalculation++;
							}
						}
//						System.out.println(getName() + ":     > " + aggregationType + " SUM-ROW-VAL column[row="+r+", col="+c+", name='"+colName+"']: colVal=|" + colVal + "|, aggregationType=|" + aggregationType + "|, rowCountForAverageCalculation=|" + rowCountForAverageCalculation + "|, sumValue=|" + sumValue +"|, JDBC_TYPE=" + jdbcType + " - " + ResultSetTableModel.getColumnJavaSqlTypeName(jdbcType) + ".");
					}
					
					if (AggregationType.Agg.SUM.equals(aggType.getAggregationType()))
					{
						addValue = sumValue;
//						System.out.println(getName() + ":   - " + aggregationType + ": SUM-COL column[col="+c+", name='"+colName+"']: value=|" + addValue +"|, JDBC_TYPE=" + jdbcType + " - " + ResultSetTableModel.getColumnJavaSqlTypeName(jdbcType) + ".");
					}
					else if (AggregationType.Agg.AVG.equals(aggType.getAggregationType()))
					{
						if (aggType.isAverageCalcInMethodLocalCalculation())
						{
							// TODO in the future; use installed callback to do "user defined" calculations, which can operate (use an algorithm) on other columns that the agregated column.
							//addValue = aggType.doAvgCalc(this, colName, sumValue, jdbcType, rowCountForAverageCalculation);

							addValue = null;
//							System.out.println(getName() + ":   - " + aggregationType + ": AVG-COL column[col="+c+", name='"+colName+"']: value=|" + addValue +"|, JDBC_TYPE=" + jdbcType + " - " + ResultSetTableModel.getColumnJavaSqlTypeName(jdbcType) + ". AVERAGE VALUE IS CALCULATED IN METHOD: localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData);");
						}
						else
						{
							addValue = privateAggregate_doAverageForValue(sumValue, jdbcType, rowCountForAverageCalculation);
//							System.out.println(getName() + ":   - " + aggregationType + ": AVG-COL column[col="+c+", name='"+colName+"']: value=|" + addValue +"|, JDBC_TYPE=" + jdbcType + " - " + ResultSetTableModel.getColumnJavaSqlTypeName(jdbcType) + ".");
						}
					}
					else if (AggregationType.Agg.MIN.equals(aggType.getAggregationType()))
					{
						addValue = minValue;
					}
					else if (AggregationType.Agg.MAX.equals(aggType.getAggregationType()))
					{
						addValue = maxValue;
					}
					else // UNKNOWN Aggregation type
					{
						addValue = null;
//						System.out.println(getName() + ":   - AGGREGATION: NOT-SUPPORTED-AGGREGATION-TYPE: --" + " + aggregationType + " + "-- column[col="+c+", name='"+colName+"']: value=|" + addValue +"|.");
					}
				}
				else
				{
					_logger.error(getName() + ": Problems in calculateSummaryRow(). Column name '" + colName + "' can't be summarized, it's NOT a summarizable data type JDBC_TYPE=" + jdbcType + " - " + ResultSetTableModel.getColumnJavaSqlTypeName(jdbcType) + ". Adding NULL value instead.");

					// Add NULL value
					addValue = null;
//System.out.println(getName() + ":   - SUM: NOT-SUPPORTED_DATATYPE column[col="+c+", name='"+colName+"']: value=|" + addValue +"|.");
				}
			}
			else
			{
				List<String> pkList = getPk();
				if (pkList != null && pkList.contains(colName))
				{
					// Add PK VALUE
					addValue = privateAggregate_getAggregatePkValueForJdbcType(jdbcType);
					addValue = calculateAggregateRow_getAggregatePkColumnDataProvider(cs, colName, c, jdbcType, addValue);
//System.out.println(getName() + ":   - SUM: PK column[col="+c+", name='"+colName+"']: value=|" + addValue +"|.");
				}
				else
				{
					// Add NULL value
					addValue = null;
					addValue = calculateAggregateRow_nonAggregatedColumnDataProvider(cs, colName, c, jdbcType, addValue);
//System.out.println(getName() + ":   - SUM: COL-NOT-IN-SUM-LIST column[col="+c+", name='"+colName+"']: value=|" + addValue +"|.");
				}
			}
			
			// Finally ADD the column value to the list that holds the ROW
			aggRow.add(addValue);
		}

		// Sanity check
		if (colCount != aggRow.size())
		{
			_logger.error(getName() + ": Problems in calculateSummaryRow(). Column Count differs in tableColCount=" + colCount + " and sumRow=" + aggRow.size());
		}

		if (doReCalc)
		{
			if (_aggregatedRowId == -1)
				throw new RuntimeException("calculateAggregateRow() RE-CALC do NOT have an _aggregatedRowId. Cont continue. counterType=" + getWhatDataTranslationStr(counterType) + ", cs.RowCnt=" + cs.getRowCount() + ", _aggregatedRowId=" + _aggregatedRowId + ", cm='" + getName() + "', csName='" + cs.getName() + "'. aggRow="+aggRow);
			else
				cs.setRow(this, _aggregatedRowId, aggRow);
		}
		else
		{
			_aggregatedRowId = cs.addRow(this, aggRow);
		}


		// Do POST Processing
		if (needsPostCalculation)
		{
			// loop columns
			for (int c = 0; c < colCount; c++)
			{
				String colName  = cs.getColumnName(c);
				int    jdbcType = cs.getColSqlType(c);
//				int    numPrec  = cs.getColSqlPrecision(c);
//				int    numScale = cs.getColSqlScale(c);

				AggregationType aggType = aggCols.get(colName);
				if (aggType != null)
				{
					// Call: Average callback
					if (aggType.hasAvgCallbacks())
					{
						try
						{
							AggregationAverageCallback cb = aggType.getAvgCallback();
							if (cb != null)
							{
								Object avgVal = cb.doAvgCalc(this, cs, counterType, _aggregatedRowId, colName, jdbcType);

								if (_logger.isDebugEnabled())
									_logger.debug("calculateAggregateRow(): cm='" + getName() + "', AvgCallback: colName='" + colName + "', newValue=" + avgVal + ".");

								cs.setValueAt(avgVal, _aggregatedRowId, c);
							}
						}
						catch (RuntimeException ex)
						{
							_logger.warn("Problems when doing AVG user-defined aggregation, skipping this and continuing...", ex);
						}
					}
				}
			}
		}
	}


	/** Checks if we have a Aggregated ROW ID */
	public boolean hasAggregatedRowId()
	{
		return _aggregatedRowId != -1;
	}

	/** 
	 * Get the Aggregated ROW ID. 
	 * @return The RowID where the aggregated row was inserted to the CountersModel (-1 if not any was added to the CountersModel)
	 */
	public int getAggregatedRowId()
	{
		return _aggregatedRowId;
	}

	/** Set what ROW ID, that holds any Aggregated Column (used in: PersistReader, when loading data from the PCS) */
	public void setAggregatedRowId(int mrow)
	{
		_aggregatedRowId = mrow;
	}

	/** Check if the TableModel row is the Aggregated Row */
	public boolean isAggregateRow(int mrow)
	{
		return _aggregatedRowId == mrow;
	}

	/**
	 * Simple class to hold any Aggregated Column MetaData
	 * @author goran
	 */
	public static class AggregationType
	{
		private Agg _type;
		private String _colName;
		private boolean _onAverageTreatZeroAsNull = false;
		private boolean _doAverageCalcInMethodLocalCalculation = false;
		private AggregationAverageCallback _avgCalcCallback;

		public enum Agg
		{
			SUM,
			AVG,
			MIN,
			MAX
		};

//		public AggregationType(Agg type, String colName, int datatype)
		public AggregationType(String colName, Agg type)
		{
			_colName = colName;
			_type    = type;
		}

		/** Can be used for a AVERAGE to decide if we should discard 0 values in rowCount. avg=sum/rowCount */
		public AggregationType(String colName, Agg type, boolean onAverageTreatZeroAsNull, boolean doAverageCalcInMethodLocalCalculation)
		{
			_colName = colName;
			_type    = type;
			_onAverageTreatZeroAsNull = onAverageTreatZeroAsNull;
			_doAverageCalcInMethodLocalCalculation = doAverageCalcInMethodLocalCalculation;
		}

		/** Can be used AVG calculations */
		public AggregationType(String colName, Agg type, AggregationAverageCallback avgCalcCallback)
		{
			_colName = colName;
			_type    = type;

			_avgCalcCallback = avgCalcCallback;
		}

		public String getColumnName()
		{
			return _colName;
		}
		public Agg getAggregationType()
		{
			return _type;
		}
		public boolean isAverageTreatZeroAsNull()
		{
			return _onAverageTreatZeroAsNull;
		}
		public boolean isAverageCalcInMethodLocalCalculation()
		{
			return _doAverageCalcInMethodLocalCalculation;
		}

		public AggregationAverageCallback getAvgCallback()
		{
			return _avgCalcCallback;
		}
		public boolean hasAvgCallbacks()
		{
			return _avgCalcCallback != null;
		}
	}
	
	/**
	 * Can be used to do Average Aggregation in an alternate way than the default implementation.
	 * @author goran
	 */
	public static interface AggregationAverageCallback
	{
		Object doAvgCalc(CountersModel countersModel, CounterSample cs, int counterType, int aggRowId, String colName, int jdbcType);
	}

	/**
	 * 
	 * @param newSample
	 * @param colName       Name of the column
	 * @param colPos        Starting at 0
	 * @param jdbcType      JDBC DataType
	 * @param addValue      The value passed
	 * @return
	 */
	public Object calculateAggregateRow_getAggregatePkColumnDataProvider(CounterSample newSample, String colName, int colPos, int jdbcType, Object addValue)
	{
		return addValue;
	}
	/**
	 * 
	 * @param newSample
	 * @param colName       Name of the column
	 * @param colPos        Starting at 0
	 * @param jdbcType      JDBC DataType
	 * @param addValue      The value passed
	 * @return
	 */
	public Object calculateAggregateRow_nonAggregatedColumnDataProvider(CounterSample newSample, String colName, int colPos, int jdbcType, Object addValue)
	{
		return null;
	}

	/**
	 *  Responsible for doing the SUM aggregation for various data types 
	 */
	private Object privateAggregate_doSummaryForValue(Object sumVal, Object val, int jdbcType)
	{
		// No need to SUM, just return "what we got so far"
		if (val == null)
			return sumVal;

		boolean dummy = false;
		if (dummy) 
		{
			// dummy just to simplify commenting out of the first if statement
		}
//		else if (jdbcType == java.sql.Types.TINYINT)
//		{
//		}
		else if (jdbcType == java.sql.Types.SMALLINT)
		{
			if (sumVal == null) 
				sumVal = Short.valueOf((short)0);

//			return Short.valueOf( ((Number)sumVal).shortValue() + ((Number)val).shortValue() );
			return ((Number)sumVal).shortValue() + ((Number)val).shortValue();
		}
		else if (jdbcType == java.sql.Types.INTEGER)
		{
			if (sumVal == null) 
				sumVal = Integer.valueOf(0);

			return Integer.valueOf( ((Number)sumVal).intValue() + ((Number)val).intValue() );
		}
		else if (jdbcType == java.sql.Types.BIGINT)
		{
			if (sumVal == null) 
				sumVal = Long.valueOf(0);

			return Long.valueOf( ((Number)sumVal).longValue() + ((Number)val).longValue() );
		}
		else if (jdbcType == java.sql.Types.FLOAT || jdbcType == java.sql.Types.REAL)
		{
			if (sumVal == null) 
				sumVal = Float.valueOf(0);

			return Float.valueOf( ((Number)sumVal).floatValue() + ((Number)val).floatValue() );
		}
		else if (jdbcType == java.sql.Types.DOUBLE)
		{
			if (sumVal == null) 
				sumVal = Double.valueOf(0);

			return Double.valueOf( ((Number)sumVal).doubleValue() + ((Number)val).doubleValue() );
		}
		else if (jdbcType == java.sql.Types.NUMERIC || jdbcType == java.sql.Types.DECIMAL)
		{
			if (sumVal == null) 
				sumVal = new BigDecimal(0);

			return ((BigDecimal)sumVal).add( (BigDecimal) val );
		}
		
		_logger.error(getName() + ": Unhandled JDBC datatype " + jdbcType + " - " + ResultSetTableModel.getColumnJavaSqlTypeName(jdbcType) + ", in xxx_doSummaryForValue().");
		return null;
	}
	/**
	 *  Responsible for doing the AVG calculation for various data types 
	 */
	private Object privateAggregate_doAverageForValue(Object val, int jdbcType, int rowCount)
	{
		// No need to SUM, just return "what we got so far"
		if (val == null)
			return val;

		// No rows (no average), return null
		if (rowCount <= 0)
			return null;
		
		if ( ! (val instanceof Number) )
		{
			System.out.println("private_doAverageForValue(): sumVal must be an instance of Number. The object '" + val + "' is of type " + val.getClass());
		}
			
		boolean dummy = false;
		if (dummy) 
		{
			// dummy just to simplify commenting out of the first if statement
		}
//		else if (jdbcType == java.sql.Types.TINYINT)
//		{
//		}
		else if (jdbcType == java.sql.Types.SMALLINT)
		{
			return (short) ((Number)val).shortValue() / rowCount; // What data type will this return ???
		}
		else if (jdbcType == java.sql.Types.INTEGER)
		{
			return (int) ((Number)val).intValue() / rowCount; // What data type will this return ???
		}
		else if (jdbcType == java.sql.Types.BIGINT)
		{
			return (long) ((Number)val).longValue() / rowCount; // What data type will this return ???
		}
		else if (jdbcType == java.sql.Types.FLOAT || jdbcType == java.sql.Types.REAL)
		{
			return (float) ((Number)val).floatValue() / rowCount; // What data type will this return ???
		}
		else if (jdbcType == java.sql.Types.DOUBLE)
		{
			return (double) ((Number)val).doubleValue() / rowCount; // What data type will this return ???
		}
		else if (jdbcType == java.sql.Types.NUMERIC || jdbcType == java.sql.Types.DECIMAL)
		{
			BigDecimal retVal;
//			retVal = ((BigDecimal)val).divide( BigDecimal.valueOf(rowCount) );
			retVal = ((BigDecimal)val).divide( BigDecimal.valueOf(rowCount), RoundingMode.HALF_UP );
			return retVal;
		}
		
		_logger.error(getName() + ": Unhandled JDBC datatype " + jdbcType + " - " + ResultSetTableModel.getColumnJavaSqlTypeName(jdbcType) + ", in xxx_doSummaryForValue().");
		return null;
	}
	/**
	 *  Responsible for doing the MIN aggregation for various data types 
	 */
	private Object privateAggregate_doMinForValue(Object minVal, Object val, int jdbcType)
	{
		// No need to MIN, just return "what we got so far"
		if (val == null)
			return minVal;

		boolean dummy = false;
		if (dummy) 
		{
			// dummy just to simplify commenting out of the first if statement
		}
//		else if (jdbcType == java.sql.Types.TINYINT)
//		{
//		}
		else if (jdbcType == java.sql.Types.SMALLINT)
		{
			if (minVal == null) 
				minVal = ((Number)val).shortValue();

			return Short.valueOf( (short) Math.min( ((Number)minVal).shortValue(), ((Number)val).shortValue() ) );
		}
		else if (jdbcType == java.sql.Types.INTEGER)
		{
			if (minVal == null) 
				minVal = ((Number)val).intValue();

			return Math.min( ((Number)minVal).intValue(), ((Number)val).intValue() );
		}
		else if (jdbcType == java.sql.Types.BIGINT)
		{
			if (minVal == null) 
				minVal = ((Number)val).longValue();

			return Math.min( ((Number)minVal).longValue(), ((Number)val).longValue() );
		}
		else if (jdbcType == java.sql.Types.FLOAT || jdbcType == java.sql.Types.REAL)
		{
			if (minVal == null) 
				minVal = ((Number)val).floatValue();

			return Math.min( ((Number)minVal).floatValue(), ((Number)val).floatValue() );
		}
		else if (jdbcType == java.sql.Types.DOUBLE)
		{
			if (minVal == null) 
				minVal = ((Number)val).doubleValue();

			return Math.min( ((Number)minVal).doubleValue(), ((Number)val).doubleValue() );
		}
		else if (jdbcType == java.sql.Types.NUMERIC || jdbcType == java.sql.Types.DECIMAL)
		{
			if (minVal == null) 
				minVal = (BigDecimal) val;

			return ((BigDecimal)minVal).min( (BigDecimal) val );
		}
		
		_logger.error(getName() + ": Unhandled JDBC datatype " + jdbcType + " - " + ResultSetTableModel.getColumnJavaSqlTypeName(jdbcType) + ", in private_doMinForValue().");
		return null;
	}
	/**
	 *  Responsible for doing the MIN aggregation for various data types 
	 */
	private Object privateAggregate_doMaxForValue(Object maxVal, Object val, int jdbcType)
	{
		// No need to MAX, just return "what we got so far"
		if (val == null)
			return maxVal;

		boolean dummy = false;
		if (dummy) 
		{
			// dummy just to simplify commenting out of the first if statement
		}
//		else if (jdbcType == java.sql.Types.TINYINT)
//		{
//		}
		else if (jdbcType == java.sql.Types.SMALLINT)
		{
			if (maxVal == null) 
				maxVal = ((Number)val).shortValue();

			return Short.valueOf( (short) Math.max( ((Number)maxVal).shortValue(), ((Number)val).shortValue() ) );
		}
		else if (jdbcType == java.sql.Types.INTEGER)
		{
			if (maxVal == null) 
				maxVal = ((Number)val).intValue();

			return Math.max( ((Number)maxVal).intValue(), ((Number)val).intValue() );
		}
		else if (jdbcType == java.sql.Types.BIGINT)
		{
			if (maxVal == null) 
				maxVal = ((Number)val).longValue();

			return Math.max( ((Number)maxVal).longValue(), ((Number)val).longValue() );
		}
		else if (jdbcType == java.sql.Types.FLOAT || jdbcType == java.sql.Types.REAL)
		{
			if (maxVal == null) 
				maxVal = ((Number)val).floatValue();

			return Math.max( ((Number)maxVal).floatValue(), ((Number)val).floatValue() );
		}
		else if (jdbcType == java.sql.Types.DOUBLE)
		{
			if (maxVal == null) 
				maxVal = ((Number)val).doubleValue();

			return Math.max( ((Number)maxVal).doubleValue(), ((Number)val).doubleValue() );
		}
		else if (jdbcType == java.sql.Types.NUMERIC || jdbcType == java.sql.Types.DECIMAL)
		{
			if (maxVal == null) 
				maxVal = (BigDecimal) val;

			return ((BigDecimal)maxVal).max( (BigDecimal) val );
		}
		
		_logger.error(getName() + ": Unhandled JDBC datatype " + jdbcType + " - " + ResultSetTableModel.getColumnJavaSqlTypeName(jdbcType) + ", in private_doMaxForValue().");
		return null;
	}


	/** 
	 * Used by method {@link #calculateAggregateRow_getAggregatePkColumnDataProvider()} to set String Values<br>
	 * For other data types a zero (0) is assigned (in the specified JdbcType)
	 */
	public static final String calculateAggregateRow__DEFAULT_STRING_VALUE = "_Total";
	/**
	 * 
	 * @param jdbcType
	 * @return
	 */
	private Object privateAggregate_getAggregatePkValueForJdbcType(int jdbcType)
	{
		String strVal = calculateAggregateRow__DEFAULT_STRING_VALUE;

		switch (jdbcType)
		{
		case java.sql.Types.BIT:                     return Boolean.valueOf(false);
		case java.sql.Types.TINYINT:                 return Byte.valueOf(Byte.parseByte("0"));
		case java.sql.Types.SMALLINT:                return Short.valueOf(Short.parseShort("0"));
		case java.sql.Types.INTEGER:                 return Integer.valueOf(0);
		case java.sql.Types.BIGINT:                  return Long.valueOf(0);
		case java.sql.Types.FLOAT:                   return Float.valueOf(0);
		case java.sql.Types.REAL:                    return Float.valueOf(0);
		case java.sql.Types.DOUBLE:                  return Double.valueOf(0);
		case java.sql.Types.NUMERIC:                 return BigDecimal.valueOf(0);
		case java.sql.Types.DECIMAL:                 return BigDecimal.valueOf(0);
		case java.sql.Types.CHAR:                    return strVal;
		case java.sql.Types.VARCHAR:                 return strVal;
		case java.sql.Types.LONGVARCHAR:             return strVal;
		case java.sql.Types.DATE:                    return new Date(0);
		case java.sql.Types.TIME:                    return new Time(0);
		case java.sql.Types.TIMESTAMP:               return new Timestamp(0);
		case java.sql.Types.BINARY:                  return null;
		case java.sql.Types.VARBINARY:               return null;
		case java.sql.Types.LONGVARBINARY:           return null;
		case java.sql.Types.NULL:                    return null;
		case java.sql.Types.OTHER:                   return null;
		case java.sql.Types.JAVA_OBJECT:             return new Object();
		case java.sql.Types.DISTINCT:                return null;
		case java.sql.Types.STRUCT:                  return null;
		case java.sql.Types.ARRAY:                   return null;
		case java.sql.Types.BLOB:                    return null;
		case java.sql.Types.CLOB:                    return null;
		case java.sql.Types.REF:                     return null;
		case java.sql.Types.DATALINK:                return null;
		case java.sql.Types.BOOLEAN:                 return Boolean.valueOf(false);

	    //------------------------- JDBC 4.0 -----------------------------------
	    case java.sql.Types.ROWID:                   return null;
	    case java.sql.Types.NCHAR:                   return strVal;
	    case java.sql.Types.NVARCHAR:                return strVal;
	    case java.sql.Types.LONGNVARCHAR:            return strVal;
	    case java.sql.Types.NCLOB:                   return null;
	    case java.sql.Types.SQLXML:                  return null;

	    //--------------------------JDBC 4.2 -----------------------------
	    case java.sql.Types.REF_CURSOR:              return null;
	    case java.sql.Types.TIME_WITH_TIMEZONE:      return null;
	    case java.sql.Types.TIMESTAMP_WITH_TIMEZONE: return null;

		default:
			_logger.error(getName() + ": Unknow JDBC datatype[" + jdbcType + "], in xxx_getAggregatePkValueForJdbcType().");
			return null;
		}
	}
	
	/**
	 * 
	 * @param jdbcType
	 * @return
	 */
	private boolean privateAggregate_isSummarizableForJdbcType(int jdbcType)
	{
		switch (jdbcType)
		{
		case java.sql.Types.BIT:                     return false;
		case java.sql.Types.TINYINT:                 return true;   // OK
		case java.sql.Types.SMALLINT:                return true;   // OK
		case java.sql.Types.INTEGER:                 return true;   // OK
		case java.sql.Types.BIGINT:                  return true;   // OK
		case java.sql.Types.FLOAT:                   return true;   // OK
		case java.sql.Types.REAL:                    return true;   // OK
		case java.sql.Types.DOUBLE:                  return true;   // OK
		case java.sql.Types.NUMERIC:                 return true;   // OK
		case java.sql.Types.DECIMAL:                 return true;   // OK
		case java.sql.Types.CHAR:                    return false;
		case java.sql.Types.VARCHAR:                 return false;
		case java.sql.Types.LONGVARCHAR:             return false;
		case java.sql.Types.DATE:                    return false;
		case java.sql.Types.TIME:                    return false;
		case java.sql.Types.TIMESTAMP:               return false;
		case java.sql.Types.BINARY:                  return false;
		case java.sql.Types.VARBINARY:               return false;
		case java.sql.Types.LONGVARBINARY:           return false;
		case java.sql.Types.NULL:                    return false;
		case java.sql.Types.OTHER:                   return false;
		case java.sql.Types.JAVA_OBJECT:             return false;
		case java.sql.Types.DISTINCT:                return false;
		case java.sql.Types.STRUCT:                  return false;
		case java.sql.Types.ARRAY:                   return false;
		case java.sql.Types.BLOB:                    return false;
		case java.sql.Types.CLOB:                    return false;
		case java.sql.Types.REF:                     return false;
		case java.sql.Types.DATALINK:                return false;
		case java.sql.Types.BOOLEAN:                 return false;

	    //------------------------- JDBC 4.0 -----------------------------------
	    case java.sql.Types.ROWID:                   return false;
	    case java.sql.Types.NCHAR:                   return false;
	    case java.sql.Types.NVARCHAR:                return false;
	    case java.sql.Types.LONGNVARCHAR:            return false;
	    case java.sql.Types.NCLOB:                   return false;
	    case java.sql.Types.SQLXML:                  return false;

	    //--------------------------JDBC 4.2 -----------------------------
	    case java.sql.Types.REF_CURSOR:              return false;
	    case java.sql.Types.TIME_WITH_TIMEZONE:      return false;
	    case java.sql.Types.TIMESTAMP_WITH_TIMEZONE: return false;

		default:
			_logger.error(getName() + ": Unknow JDBC datatype, in xxx_isSummarizableForJdbcType().");
			return false;
		}
	}


	//-------------------------------------------------------
	// END: Aggregation 
	//-------------------------------------------------------
}
