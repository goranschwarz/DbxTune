/**
*/

package com.asetune.cm;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
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
import org.apache.log4j.Logger;

import com.asetune.CounterController;
import com.asetune.DbxTune;
import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.Version;
import com.asetune.alarm.AlarmHandler;
import com.asetune.alarm.IUserDefinedAlarmInterrogator;
import com.asetune.alarm.UserDefinedAlarmHandler;
import com.asetune.alarm.events.AlarmEvent;
import com.asetune.alarm.events.AlarmEventProcedureCacheOutOfMemory;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.graph.TrendGraphDataPoint.Category;
import com.asetune.graph.TrendGraphDataPoint.LabelType;
import com.asetune.gui.MainFrame;
import com.asetune.gui.ResultSetTableModel;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.gui.TrendGraph;
import com.asetune.gui.swing.ColumnHeaderPropsEntry;
import com.asetune.gui.swing.GTabbedPane;
import com.asetune.gui.swing.GTable;
import com.asetune.gui.swing.GTable.ITableTooltip;
import com.asetune.pcs.PersistReader.PcsSavedException;
import com.asetune.pcs.PersistentCounterHandler;
import com.asetune.sql.DbmsDataTypeResolver;
import com.asetune.sql.ResultSetMetaDataCached;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.conn.TdsConnection;
import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.AseSqlScript;
import com.asetune.utils.Configuration;
import com.asetune.utils.CronUtils;
import com.asetune.utils.NumberUtils;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;
import com.asetune.utils.TimeUtils;
import com.asetune.utils.Ver;
import com.fasterxml.jackson.core.JsonGenerator;
import com.google.gson.stream.JsonWriter;
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
	private static Logger      _logger            = Logger.getLogger(CountersModel.class);

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
	private int                _serverVersion     = 0;
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
	private int                _dependsOnVersion     = 0;
	private int                _dependsOnCeVersion   = 0;
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

	private ResultSetMetaData  _rsmd;

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

	private boolean dataInitialized=false;
	private boolean firstTimeSample=true;
	private boolean _sqlInitDone=false;

	private int maxRowSeen;
	
	public enum State { NORMAL, SRV_IN_SHUTDOWN };
	private State _state = State.NORMAL;

	/** Class for handling User Defined Alarms */
	private IUserDefinedAlarmInterrogator _udAlarmInterrorgator = null;

	private GTable.ITableTooltip _cmToolTipSupplier = null;
	private Map<String, String> _cmLocalToolTipColumnDescriptions = new HashMap<String, String>();
	
	// Columns that should be first in the output table
//	private Set<String> _preferredColumnOrder = new LinkedHashSet<String>();
	private HashMap<String, ColumnHeaderPropsEntry> _preferredColumnProps = new LinkedHashMap<String, ColumnHeaderPropsEntry>();

	private int _aseError_2714_count = 0;
	private int _aseError_2714_actionThreshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_aseError_2714_actionThreshold, DEFAULT_aseError_2714_actionThreshold);

	
	public static final String 	PROPKEY_aseError_2714_actionThreshold = "CounterModel.ase.error.2714.action.threshold";
	public static final int     DEFAULT_aseError_2714_actionThreshold = 5;

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
		dataInitialized = false;
		firstTimeSample = true;
		_sqlInitDone    = false;

		maxRowSeen      = 0; // this isn't used ???

		// new delta/rate row flag
		_isNewDeltaOrRateRow = null;

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
			String       name,             // Name of the Counter Model
			String       groupName,        // Name of the Group this counter belongs to, can be null
			String       sql,              // SQL Used to grab a sample from the counter data
			List<String> pkList,           // A list of columns that will be used during diff calculations to lookup values in previous samples
			String[]     diffColumns,      // Columns to do diff calculations on
			String[]     pctColumns,       // Columns that is to be considered as Percent calculated columns, (they still need to be apart of diffColumns)
			String[]     monTables,        // What monitor tables are accessed in this query, used for TOOLTIP lookups
			String[]     dependsOnRole,    // Needs following role(s)
			String[]     dependsOnConfig,  // Check that these configurations are above 0
			int          dependsOnVersion, // What version of ASE do we need to sample for this CounterModel
			int          dependsOnCeVersion, // What version of ASE-CE do we need to sample for this CounterModel
			boolean      negativeDiffCountersToZero, // if diff calculations is negative, reset the counter to zero.
			boolean      systemCm
	)
	{
		this(counterController, name, groupName, sql, pkList, diffColumns, pctColumns, monTables, dependsOnRole, dependsOnConfig, dependsOnVersion, dependsOnCeVersion, negativeDiffCountersToZero, systemCm, 0);
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
			String       name,
			String       groupName,
			String       sql,
			List<String> pkList,
			String[]     diffColumns,
			String[]     pctColumns,
			String[]     monTables,
			String[]     dependsOnRole,
			String[]     dependsOnConfig,
			int          dependsOnVersion,
			int          dependsOnCeVersion,
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
		maxRowSeen        = 0;

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
		c._sampleInterval             = this._sampleInterval;

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
		c._rsmd                       = this._rsmd;

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

		c.dataInitialized             = this.dataInitialized;
		c.firstTimeSample             = this.firstTimeSample;

		c.maxRowSeen                  = this.maxRowSeen;

		c._refreshCounter             = this._refreshCounter;
		c._sumRowCount                = this._sumRowCount;
//		c._sumRowCountAbs             = this._sumRowCountAbs;
//		c._sumRowCountDiff            = this._sumRowCountDiff;
//		c._sumRowCountRate            = this._sumRowCountRate;
		
		c._isNonConfiguredMonitoringAllowed       = this._isNonConfiguredMonitoringAllowed;
		c._hasNonConfiguredMonitoringHappened     = this._hasNonConfiguredMonitoringHappened;
		c._lastNonConfiguredMonitoringMessageList = this._lastNonConfiguredMonitoringMessageList; // Should we clone this or not...

		c._isNewDeltaOrRateRow = this._isNewDeltaOrRateRow;

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
	 * Get who is responsible for collecting the counters for this CounterModel
	 * @return counterController
	 */
	public ICounterController getCounterController()
	{
		if (_counterController != null)
			return _counterController;

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

		// Remove this, this is just for backward compatibility.
		return CounterController.hasInstance();
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
	 */
	public void setOfflineColumnNames(int type, List<String> cols)
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
	public boolean discardDiffPctHighlighterOnAbsTable() { return getDataSource() == DATA_ABS; }

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
		if (hasCounterController())
			return getCounterController().createCmToolTipSupplier(this);
		return null;
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
		sb.append("<table").append(border).append(cellPadding).append(cellSpacing).append(">\n");
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
	 * @param seriesLabels       An String[] of series labels (that will be deisplaied at the graph bottom). This can be NULL if seriesLabelType is Static
	 * @param seriesLabelType    If the seriesLabels are static or dynamically added
	 * @param isPercentGraph     true if the MAX scale of the graph should be 100
	 * @param visibleAtStart     true if the graph should be visable when starting DbxTune the first time
	 * @param needsVersion       What DBMS version is this graph valid from. (use com.asetune.utils.Ver to generate a value). 0 = any version
	 * @param minimumHeight      Minimum height of the graph content in pixels. (-1 = Let the layout manager decide this)
	 */
	public void addTrendGraph(String name, String chkboxText, String graphLabel, String[] seriesLabels, LabelType seriesLabelType, TrendGraphDataPoint.Category graphCategory, boolean isPercentGraph, boolean visibleAtStart, int needsVersion, int minimumHeight)
	{
		if (LabelType.Dynamic.equals(seriesLabelType))
			seriesLabels = TrendGraphDataPoint.RUNTIME_REPLACED_LABELS;
		
		if (seriesLabels == null)
			seriesLabels = new String[] {""};

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

		TrendGraphDataPoint tgdp = new TrendGraphDataPoint(name, graphLabel, graphCategory, isPercentGraph, visibleAtStart, seriesLabels, seriesLabelType); 
		addTrendGraphData(name, tgdp);
		
		
		if (getGuiController() != null && getGuiController().hasGUI())
		{
			CountersModel cm = this;
			
			// GRAPH
			TrendGraph tg = new TrendGraph(name, chkboxText, graphLabel, 
				seriesLabels, 
				isPercentGraph,
				cm, 
				visibleAtStart,
				needsVersion, 
				minimumHeight);
			
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

	public void initTrendGraphForVersion(int serverVersion)
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
			updateGraphData(tgdp);
			
			//System.out.println("cm='"+StringUtil.left(this.getName(),25)+"', _trendGraphData="+tgdp);
			if (_logger.isDebugEnabled())
				_logger.debug("cm='"+StringUtil.left(this.getName(),25)+"', _trendGraphData="+tgdp);
		}
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
	public String createPkStr(String... pk)
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
	public void setServerVersion(int serverVersion)
	{
		_serverVersion = serverVersion;
	}
	/** */
	public int getServerVersion()
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
		return Ver.versionIntToStr( getServerVersion() );
	}

	/** In here we could call getServerVersion() and decide what SQL syntax we should 
	 * use and what monitor tables and coulmns we should query.
	 * <p>
	 * To set the new version dependent SQL statement, use setSql() method. 
	 * <p>
	 * If getSql() shows NULL or empty SQL statement, this method will be called
	 * to compose a new SQL statement. 
	 */
	public void initSql(Connection conn)
	{
		// Get what version we are connected to.
		int     aseVersion       = getServerVersion();
		boolean isClusterEnabled = isClusterEnabled();

		// Get what configuration we depends on
		String[] dependsOnConfig = getDependsOnConfigForVersion(conn, aseVersion, isClusterEnabled);
		setDependsOnConfig(dependsOnConfig);
		
		// Generate the SQL, for the specific ASE version
		String sql = getSqlForVersion(conn, aseVersion, isClusterEnabled);
		setSql(sql);

		// Generate the SQL INIT, for the specific ASE version
		String sqlInit = getSqlInitForVersion(conn, aseVersion, isClusterEnabled);
		setSqlInit(sqlInit);

		// Generate the SQL CLOSE, for the specific ASE version
		String sqlClose = getSqlCloseForVersion(conn, aseVersion, isClusterEnabled);
		setSqlClose(sqlClose);

		// Generate PrimaryKey, for the specific ASE version
		List<String> pkList = getPkForVersion(conn, aseVersion, isClusterEnabled);
		setPk(pkList);
		
		// Set specific column descriptions
		addMonTableDictForVersion(conn, aseVersion, isClusterEnabled);
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
	 * This one could or <br>should</b> be called to get a SQL string that will be used
	 * to get data for a specific Performance Counter.
	 * <p>
	 * It could also be used check what SQL that will be generated for a specific ASE version.
	 * <p>
	 * This method would typically be called from initSql(), which will set the correct SQL 
	 * (by calling setSql()) for the desired ASE version.
	 */
	// FIXME: maybe declare this method and class as abstract, instead of throwing the exception.
//	public abstract String getSqlForVersion(Connection conn, int srvVersion, boolean isClusterEnabled);
	public String getSqlForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		throw new UnsupportedOperationException("The method CountersModel.getSqlForVersion(Connection conn, int srvVersion, boolean isClusterEnabled) has NOT been overridden, which should be done. CM Name='"+getName()+"'.");
	}

	public String getSqlInitForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		return null;
	}

	public String getSqlCloseForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		return null;
	}

	// FIXME: maybe declare this method and class as abstract, instead of throwing the exception.
//	public abstract List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled);
	public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		throw new UnsupportedOperationException("The method CountersModel.getPkForVersion(int srvVersion, boolean isClusterEnabled) has NOT been overridden, which should be done. CM Name='"+getName()+"'.");
	}

	public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		return null;
	}

	public void addMonTableDictForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
	}

	/** 
	 * This method is used to check if the CounterTable should be initialized or not.<br>
	 * for example "deadlock pipe" may not be active...<br>
	 * If server version is below 15.0.2 'statement cache' info should not be 
	 * polled since those monTables doesn't exist. And the GUI Table for this should
	 * not be VISABLE... call setActive(false) in those cases.
	 */
	public void init(Connection conn)
	throws Exception // I think it's OK to do this here
	{
		if (checkDependsOnVersion())
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
	public void setActive(boolean state, String problemDescription)
	{
		_isActive    = state;
		_problemDesc = problemDescription;
		if (_problemDesc == null)
			_problemDesc = "";
		
		if (_tabPanel != null)
		{
			_tabPanel.setEnabled(state);
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
			return "The ASE Server is waiting for a SHUTDOWN, data collection is put on hold...";

		return "";
	}

	/** */
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
	public boolean checkDependsOnRole(Connection conn)
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
	public boolean checkDependsOnConfig(Connection conn, String configNameVal)
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
			_logger.debug("Checking for ASE Configuration '"+configName+"', which has value '"+configHasValue+"'. Option to re-configure to value '"+reConfigValue+"' if not set.");

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
				_logger.info("ASE Configuration '"+configName+"' for Counters Model '"+getName()+"', named '"+getDisplayName()+"', will be reconfigured from value '"+configHasValue+"' to value '"+reConfigValue+"'.");

				try
				{
					AseConnectionUtils.setAseConfigValue(conn, configName, reConfigValue);
				}
				catch (SQLException e)
				{
					_logger.error("Problems setting ASE configuration '"+configName+"' to '"+reConfigValue+"'. Caught: "+AseConnectionUtils.sqlExceptionToString(e));
				}

				configHasValue = AseConnectionUtils.getAseConfigRunValueNoEx(conn, configName);
				if (_logger.isDebugEnabled()) 
					_logger.debug("After re-config, the ASE Configuration '"+configName+"', now has value '"+configHasValue+"'.");
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
				_logger.warn("Non Configured Monitoring is allowed: When trying to initialize Counters Model '"+getName()+"', named '"+getDisplayName()+"' in ASE Version "+getServerVersionStr()+", I found that '"+configName+"' wasn't configured (which is done with: sp_configure '"+configName+"'), so counters in '"+getDisplayName()+"' may not be relyable.");
				return true;
			}

			if (_logger.isDebugEnabled()) 
				_logger.debug(getName() + ": should be HIDDEN.");
			String reconfigOptionStr = " or using the nogui mode: --reconfigure switch";
			if (reConfigValue == null)
				reconfigOptionStr = "";
			_logger.warn("When trying to initialize Counters Model '"+getName()+"', named '"+getDisplayName()+"' in ASE Version "+getServerVersionStr()+", I found that '"+configName+"' wasn't configured (which is done with: sp_configure '"+configName+"'"+reconfigOptionStr+"), so monitoring information about '"+getDisplayName()+"' will NOT be enabled.");

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
	public boolean checkDependsOnConfig(Connection conn)
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
	public String getMissigConfig(Connection conn)
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
		return Ver.versionIntToStr( getDependsOnVersion() );
	}
	public int getDependsOnVersion()
	{
		return _dependsOnVersion;
	}
	public void setDependsOnVersion(int version)
	{
		_dependsOnVersion = version;
	}

	public String getDependsOnCeVersionStr()
	{
		return Ver.versionIntToStr( getDependsOnCeVersion() );
	}
	public int getDependsOnCeVersion()
	{
		return _dependsOnCeVersion;
	}
	public void setDependsOnCeVersion(int version)
	{
		_dependsOnCeVersion = version;
	}

	public boolean checkDependsOnVersion()
	{
		if (_dependsOnVersion == 0)
			return true;

		int needsVersion   = getDependsOnVersion();
		int aseVersion     = getServerVersion();

		if ( isClusterEnabled() && getDependsOnCeVersion() > 0 )
		{
			return checkDependsOnCeVersion();
		}

		if (aseVersion >= needsVersion)
		{
			return true;
		}
		else
		{
			if (_logger.isDebugEnabled()) 
				_logger.debug(getName() + ": should be HIDDEN.");
			_logger.warn("When trying to initialize Counters Model '"+getName()+"', named '"+getDisplayName()+"' in ASE Version "+getServerVersionStr()+", I need at least ASE Version "+getDependsOnVersionStr()+" for that.");

			setActive(false, "This info is only available if ASE Server Version is above " + getDependsOnVersionStr());

			TabularCntrPanel tcp = getTabPanel();
			if (tcp != null)
			{
				tcp.setToolTipText("This tab will only be visible if ASE Server Version is over "+getDependsOnVersionStr());
			}
			return false;
		}
	}
	public boolean checkDependsOnCeVersion()
	{
		if (_dependsOnCeVersion == 0)
			return true;

		int needsCeVersion = getDependsOnCeVersion();
		int aseVersion     = getServerVersion();

		if (aseVersion >= needsCeVersion)
		{
			return true;
		}
		else
		{
			if (_logger.isDebugEnabled())
				_logger.debug(getName() + ": should be HIDDEN.");
			_logger.warn("When trying to initialize Counters Model '"+getName()+")', named '"+getDisplayName()+"' in ASE Cluster Edition Version "+getServerVersionStr()+", I need at least ASE Cluster Edition Version "+getDependsOnCeVersionStr()+" for that.");

			setActive(false, "This info is only available if ASE Cluster Edition Server Version is above " + getDependsOnCeVersionStr());

			TabularCntrPanel tcp = getTabPanel();
			if (tcp != null)
			{
				tcp.setToolTipText("This tab will only be visible if ASE Cluster Edition Server Version is over "+getDependsOnCeVersionStr());
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
	public void addDependsOnStoredProc(String dbname, String procName, Date procDateThreshold, Class<?> scriptLocation, String scriptName, String needsRoleToRecreate, int needSrvVersion)
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
	
	public boolean checkDependsOnStoredProc(Connection conn, String dbname, String procName, Date procDateThreshold, Class<?> scriptLocation, String scriptName, String needsRoleToRecreate, int needSrvVersion)
	{
		int srvVersion = AseConnectionUtils.getAseVersionNumber(conn);
		if (srvVersion < needSrvVersion)
		{
			_logger.warn("When trying to checking stored procedure '"+procName+"' in '"+dbname+"' the Current ASE Version is to low '"+Ver.versionIntToStr(srvVersion)+"', this procedure needs ASE Version '"+Ver.versionIntToStr(needSrvVersion)+"' to install.");
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
//				_logger.warn("When trying to initialize Counters Model '"+getName()+"', named '"+getDisplayName()+"' in ASE Version "+getServerVersion()+", "+msg+" (connect with a user that has '"+needsRoleToRecreate+"' or load the proc from '$"+DbxTune.getInstance().getAppHomeEnvName()+"/classes' or unzip asetune.jar. under the class '"+scriptLocation.getClass().getName()+"' you will find the script '"+scriptName+"').");
				_logger.warn("When trying to initialize Counters Model '"+getName()+"', named '"+getDisplayName()+"' in ASE Version "+getServerVersion()+", "+msg+" (connect with a user that has '"+needsRoleToRecreate+"' or load the proc from '$DBXTUNE_HOME/classes' or unzip asetune.jar. under the class '"+scriptLocation.getClass().getName()+"' you will find the script '"+scriptName+"').");

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
	public boolean checkDependsOnStoredProc(Connection conn)
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
	public boolean checkDependsOnOther(Connection conn)
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
	public HashMap<String, ColumnHeaderPropsEntry> getPreferredColumnProps()
	{
		return _preferredColumnProps;
	}

	/**
	 * Set the preferred column properties<br>
	 * Column names <b>not</b> in the Map should be put at the end
	 * @param columns
	 */
	public void setPreferredColumnProps(HashMap<String, ColumnHeaderPropsEntry> columns)
	{
		_preferredColumnProps = columns;
	}

	/**
	 * Add a column properties<br>
	 * @param colname
	 */
	public void addPreferredColumnOrder(ColumnHeaderPropsEntry colProps)
	{
		HashMap<String, ColumnHeaderPropsEntry> columns = getPreferredColumnProps();
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

			// call the implementation
			rowsFetched = refreshGetData(conn);

			// if we fetched any rows, this means that we would have a Valid Sample
			// a rowcount with -1 = FAILURE
			//   rowcount with 0  = SUCCESS, but now rows where fetched. 
			setValidSampleData( rowsFetched > 0 );

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
			if ( tmpNewSample.getSample(this, conn, getSql()+getSqlWhere(), _pkCols) == false )
				return -1;
			
			// If we want to test some error handling
			boolean exceptionTest = false;
//exceptionTest = true;
			if (exceptionTest)
			{
    			if ("CmProcessActivity".equals(getName()))
    				throw new SQLException("dummy test of '2714'...", "XXXX", 2714);
			}
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

			// Look for Timeout exception
			boolean isTimeoutException = false;
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
			if (isTimeoutException)
			{
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

		// if it's the first time sampling...
		if (firstTimeSample)
		{
//			saveDdl();
//			firstTimeSample = false; // done later
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

		// Do we want to send an Alarm somewhere, every CM's has to implement this.
		// NOTE: This is moved a bit down, until the "end" so that the CM's _prevSample, _newSample, _diffData, _rateData has been SET...
		//sendAlarmRequest(fTmpNewSample, fTmpDiffData, fTmpRateData);


		if ( ! DbxTune.hasGui() )
		{
			// NO GUI, move structures
			_prevSample = tmpNewSample;
			_newSample  = tmpNewSample;
			_diffData   = tmpDiffData;
			_rateData   = tmpRateData;

			// if it's the first time sampling...
			if (firstTimeSample)
			{
				fireTableStructureChanged();
				firstTimeSample = false;
			}

			// Calculte what values we should have in the graphs
			// this has to be after _prevSample, _newSample, _diffData, _rateData has been SET
			updateGraphData();

			// Do we want to send an Alarm somewhere, every CM's has to implement this.
//			wrapperFor_sendAlarmRequest(tmpNewSample, tmpDiffData, tmpRateData);
			wrapperFor_sendAlarmRequest();
		}
		else // HAS GUI
		{
			// Make them final copies to be used in the doWork/Runnable below
			final CounterSample fTmpNewSample = tmpNewSample;
			final CounterSample fTmpDiffData = tmpDiffData;
			final CounterSample fTmpRateData = tmpRateData;

			final CountersModel thisCm = this;

			Runnable doWork = new Runnable()
			{
				@Override
				public void run()
				{
					// IMPORTANT: move datastructure.
					_prevSample = fTmpNewSample;
					_newSample  = fTmpNewSample;
					_diffData   = fTmpDiffData;
					_rateData   = fTmpRateData;
					
					beginGuiRefresh();

					// Set: Info fields
//					MainFrame.getInstance().setStatus(MainFrame.ST_STATUS2_FIELD, "GUI refresh of '"+_displayName+"'");
					if (getGuiController() != null)
						getGuiController().setStatus(MainFrame.ST_STATUS2_FIELD, "GUI refresh of '"+_displayName+"'");

					// Simulate a SLOW action, for example a massive sort...
					// which would case the GUI to block for a while...
					//try { Thread.sleep(250); }
					//catch (InterruptedException ignore) {}

					try
					{
//						System.out.println();
//						System.out.println(getName()+":#### KICK OFF - CHANGED ####");
						if (getTabPanel() != null && !getTabPanel().isTableInitialized())
						{
							//System.out.println(getName()+":-fireTable-STRUCTURE-CHANGED-");
							if (_logger.isDebugEnabled())
								_logger.debug(getName()+":------doFireTableStructureChanged------");
							fireTableStructureChanged();
							
							// Hmm do I need to do this here...
							//getTabPanel().adjustTableColumnWidth();
						}
						else if (firstTimeSample && isDataInitialized())
						{
							firstTimeSample = false;

							//System.out.println(getName()+":-fireTable-STRUCTURE-CHANGED-");
							if (_logger.isDebugEnabled()) 
								_logger.debug(getName()+":------doFireTableStructureChanged------");
							fireTableStructureChanged();
						}
						else
						{
							// Delete of individual rows...
							// is not really needed since we do: fireTableDataChanged()
							// This was implemented to some problems... but can probably be commented out
//							for (int row : deletedRows)
//							{
//								System.out.println(getName()+":-fireTableRows-DELETED("+row+","+row+")-");
//								fireTableRowsDeleted(row, row);
//							}

							// DEBUG: what listeners are called...
//							if ( "CMprocActivity".equals(getName()) )
//							{
//								int i=0;
//								for (TableModelListener tml : getTableModelListeners())
//								{
//									System.out.println("-LISTENER: fireTableDataChanged - thread("+Thread.currentThread().getName()+") - Listener("+(i++)+"): "+tml);
//								}
//							}
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

					// Calculte what values we should have in the graphs
					// this has to be after _prevSample, _newSample, _diffData, _rateData has been SET
					// since we do this differred in case we use Swing, it has to be done here.
					updateGraphData();
		
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

					// Do we want to send an Alarm somewhere, every CM's has to implement this.
//					wrapperFor_sendAlarmRequest(fTmpNewSample, fTmpDiffData, fTmpRateData);
					wrapperFor_sendAlarmRequest();

				} // end: run method
			};

			//			try{SwingUtilities.invokeAndWait(doWork);}
			//			catch (Exception e) {e.printStackTrace();}

			// INVOKE the above RUNNABLE on the SWING EDT (Event Dispather Thread)
			if ( ! SwingUtilities.isEventDispatchThread() )
				SwingUtilities.invokeLater(doWork);
			else
				doWork.run();
		}

//		if (_tabPanel != null)
//		{
//			// Update dates on panel
//			_tabPanel.setTimeInfo(getCounterClearTime(), getSampleTime(), getSampleInterval());
//		}
		
		return (tmpNewSample != null) ? tmpNewSample.getRowCount() : -1;
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
				diffRow.add(new Integer(((Integer) (newRow.get(i))).intValue() - ((Integer) (oldRow.get(i))).intValue()));
			}
			diffCnt.addRow(diffRow);
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
				
				// Old row found, compute the diffs
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

			diffCnt.addRow(diffRow);

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
			diffColVal = new Byte((byte) (newColVal.byteValue() - prevColVal.byteValue()));
			if (diffColVal.intValue() < 0)
				if (negativeDiffCountersToZero)
					diffColVal = new Byte("0");
		}
		else if (newColVal instanceof Double)
		{
			diffColVal = new Double(newColVal.doubleValue() - prevColVal.doubleValue());
			if (diffColVal.doubleValue() < 0)
				if (negativeDiffCountersToZero)
					diffColVal = new Double(0);
		}
		else if (newColVal instanceof Float)
		{
			diffColVal = new Float(newColVal.floatValue() - prevColVal.floatValue());
			if (diffColVal.floatValue() < 0)
				if (negativeDiffCountersToZero)
					diffColVal = new Float(0);
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
////				diffColVal = new Integer( restVal + overflv );
////System.out.println("restVal: "+restVal);
////System.out.println("overflv: "+overflv);
////System.out.println("=result: "+diffColVal);
//
//				// Or simplified (one-line)
////				diffColVal = new Integer( (Integer.MAX_VALUE - prevColVal.intValue()) + (newColVal.intValue() - Integer.MIN_VALUE) );
//
//				// Deal with counter overflows by bumping up to a higher data type: and adding the negative delta on top of Integer.MAX_VALUE -------*/ 
//				long newColVal_bumped = Integer.MAX_VALUE + (newColVal.longValue() - Integer.MIN_VALUE);
//				diffColVal = new Integer( (int) newColVal_bumped - prevColVal.intValue() );
//			}
//			else
//				diffColVal = new Integer(newColVal.intValue() - prevColVal.intValue());
			
			diffColVal = new Integer(newColVal.intValue() - prevColVal.intValue());
			if (diffColVal.intValue() < 0)
				if (negativeDiffCountersToZero)
					diffColVal = new Integer(0);
		}
		else if (newColVal instanceof Long)
		{
			diffColVal = new Long(newColVal.longValue() - prevColVal.longValue());
			if (diffColVal.longValue() < 0)
				if (negativeDiffCountersToZero)
					diffColVal = new Long(0);
		}
		else if (newColVal instanceof Short)
		{
			diffColVal = new Short((short) (newColVal.shortValue() - prevColVal.shortValue()));
			if (diffColVal.shortValue() < 0)
				if (negativeDiffCountersToZero)
					diffColVal = new Short("0");
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

			rate.addRow(newRow);

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
	 * PRIVATE/LOCAL wrapper of sendAlarmRequest(...) so we can detemine what to do...
	 */
//	protected void wrapperFor_sendAlarmRequest(CounterSample absData, CounterSample diffData, CounterSample rateData)
	protected void wrapperFor_sendAlarmRequest()
	{
//		System.out.println("################################## wrapperFor_sendAlarmRequest(): cm='"+getName()+"'.");
		if ( ! isAlarmEnabled() )
			return;

		// Lets use the "built in" logic... which any subclases has to implement :)
		if (hasSystemAlarms() && isSystemAlarmsEnabled()) 
		{
			sendAlarmRequest();
		}

		//
		// The default behaviour is to load a java source file, that is compiled "on the fly"<br>
		// The User Defined logic is implemented by the Java Source file
		// 
		if (hasUserDefinedAlarmInterrogator() && isUserDefinedAlarmsEnabled() )
		{
//			if ( ! UserDefinedAlarmHandler.hasInstance() )
//			return;
//
//			IUserDefinedAlarmInterrogator interrorgator = UserDefinedAlarmHandler.getInstance().newClassInstance(this);
//			if (interrorgator != null)
//			{
//				interrorgator.interrogateCounterData(this, absData, diffData, rateData);
//			}
		
			// FIXME: sendAlarmRequest_internal() should be called, which calls overrided/local sendAlarmRequest() where this/local should be empty
			// FIXME: The UserDefinedAlarmHander should also considder other loaded JAR files
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
			//    <CMNAME>.alarm.userdefined.if.<COLNAME>.{eq|neq|gt|lt}.alarmEventClass=com.asetune.alarm.events.AlarmEventUserDefinedColumnWarning
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
			// Only for names that do not contin ' ' spaces... FIXME: This is UGGLY, create a type/property which describes if we should write the '*.enable.*' property or not...
			if ( sh.getName().indexOf(' ') == -1)
			{
				String propName = replaceCmAndColName(getName(), PROPKEY_ALARM_isSystemAlarmsForColumnEnabled, colname);
				String propVal  = conf.getProperty(propName, DEFAULT_ALARM_isSystemAlarmsForColumnEnabled+"");

				// At what times can the alarms be triggered
				String cronProp = replaceCmAndColName(PROPKEY_ALARM_isSystemAlarmsForColumnInTimeRange, colname);
				String cronPat  = Configuration.getCombinedConfiguration().getProperty(cronProp, DEFAULT_ALARM_isSystemAlarmsForColumnInTimeRange);

				// Check that the 'cron' sceduling pattern is valid...
				String cronPatStr = cronPat;
				boolean negation = false;
				if (cronPatStr.startsWith("!"))
				{
					negation = true;
					cronPatStr = cronPatStr.substring(1);
				}
				if ( ! SchedulingPattern.validate(cronPatStr) )
					_logger.error("The cron scheduling pattern '"+cronPatStr+"' is NOT VALID. for the property '"+replaceCmAndColName(PROPKEY_ALARM_isSystemAlarmsForColumnInTimeRange, colname)+"', this will not be used at runtime, furter warnings will also be issued.");

				String cronPatDesc = CronUtils.getCronExpressionDescriptionForAlarms(cronPatStr);

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
	 * If alarm handeling is enabled or disabled for this specific cm<br>
	 * The idea is that Alarms could be enabled/disabled for specific cm's temporary during runtime (by the gui) 
	 */
	public boolean isAlarmEnabled()
	{
		return Configuration.getCombinedConfiguration().getBooleanProperty(replaceCmName(PROPKEY_ALARM_isAlarmsEnabled), DEFAULT_ALARM_isAlarmsEnabled);
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

		// is this a negation/not withing time period...
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

	public void clear()
	{
		clear(100);
	}
	public synchronized void clear(int clearLevel)
	{
		clearCmLevel = clearLevel;

		_prevSample       = null;
		_newSample        = null;
		_diffData         = null;
		_rateData         = null;

		setValidSampleData(false);
		setDataInitialized(false);
		setTimeInfo(null, null, null, 0);
		maxRowSeen        = 0;
//		selectedModelRow  = -1;

		if (clearCmLevel > 50)
			_dataSource = getDefaultDataSource();

		// Clear dates on panel
		if (_tabPanel != null)
		{
			_tabPanel.reset();
		}

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

	protected String getWhatDataTranslationStr(int whatData)
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

	public Timestamp getTimestamp()
	{
		return (_newSample == null) ? null : _newSample.getSampleTime();

//		if (_diffData == null)
//			return null;
//		return _diffData.samplingTime;
	}

	public int getLastSampleInterval()
	{
		return (_newSample == null) ? 0 : _newSample.getSampleInterval();

//		if (_diffData != null)
//			return (int) _diffData.interval;
//		return 0;
	}
	
	// Return number of rows in the diff table
	public synchronized int size()
	{
		return (_diffData == null) ? 0 : _diffData.getRowCount();
	}

	// 
	private synchronized Double getValueAsDouble(int whatData, int rowId, int colPos)
	{
		Object o = getValue(whatData, rowId, colPos);
		if (o == null)
			return null;

		if (o instanceof Number)
			return new Double(((Number) o).doubleValue());
		else
			return new Double(Double.parseDouble(o.toString()));
	}

	// 
	private synchronized Double getValueAsDouble(int whatData, int rowId, String colname)
	{
		Object o = getValue(whatData, rowId, colname);
		if (o == null)
			return null;

		if (o instanceof Number)
			return new Double(((Number) o).doubleValue());
		else
			return new Double(Double.parseDouble(o.toString()));
	}

	// Return the value of a cell by ROWID (rowId, ColumnName)
	private synchronized Object getValue(int whatData, int rowId, String colname)
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
		int idCol = data.findColumn(colname);
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
	private synchronized Object getValue(int whatData, int rowId, int colId)
	{
		CounterTableModel data = null;

		if      (whatData == DATA_ABS)  data = getCounterDataAbs();
		else if (whatData == DATA_DIFF) data = getCounterDataDiff();
		else if (whatData == DATA_RATE) data = getCounterDataRate();
		else
			throw new RuntimeException("Only ABS, DIFF, or RATE data is available.");

		if (data == null)
			return null;

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
	private synchronized Double getValueAsDouble(int whatData, String pkStr, String colname)
	{
		Object o = getValue(whatData, pkStr, colname);
		if (o == null)
			return null;

		if (o instanceof Number)
			return new Double(((Number) o).doubleValue());
		else
			return new Double(Double.parseDouble(o.toString()));
	}

	/**
	 *  Return the value of a cell by ROWID (rowId, colId)
	 *  rowId starts at 0
	 *  colId starts at 
	 *  NOTE: note tested (2007-07-13)
	 */
	// Return the value of a cell by keyVal, (keyVal, ColumnName)
//	private synchronized Double getValue(int whatData, String pkStr, String colname)
	private synchronized Object getValue(int whatData, String pkStr, String colname)
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
			return null;
		}

		// Get the rowId, if not found, return null
		int rowId = data.getRowNumberForPkValue(pkStr);
		if (rowId < 0)
		{
			if (_logger.isDebugEnabled())
				_logger.debug(getName()+".getValue(whatData="+getWhatDataTranslationStr(whatData)+", pkStr='"+pkStr+"', colname='"+colname+"'): rowId="+rowId+": rowId < 0; return null");
			return null;
		}

		// Got object for the RowID and column name
		Object o = getValue(whatData, rowId, colname);
		if (o == null)
		{
			if (_logger.isDebugEnabled()) 
				_logger.debug(getName()+".getValue(whatData="+getWhatDataTranslationStr(whatData)+", pkStr='"+pkStr+"', colname='"+colname+"'): rowId="+rowId+": o==null; return null");
			return null;
		}

//		if (o instanceof Double)
//			return (Double) o;
//		else
//			return new Double(o.toString());
		return o;
	}
	/**
	 * Get a int array of rows where colValue matches values in the column name
	 * @param whatData DATA_ABS or DATA_DIFF or DATA_RATE
	 * @param colname Name of the column to search in
	 * @param colvalue Value to search for in the above column name
	 * @return int[] an array of integers where rows meets above search criteria, if nothing was found return null
	 */
	private int[] getRowIdsWhere(int whatData, String colname, String colvalue)
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
		int idCol = data.findColumn(colname);
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

			if (o.equals(colvalue))
				rowsList.add(i);
		}
		if (rowsList.isEmpty())
			return null;

		// Convert the list into a array
		int ia[] = new int[rowsList.size()];
		for (int i=0; i<rowsList.size(); i++)
			ia[i] = rowsList.get(i);

		return ia;
	}

	private synchronized Double getMaxValue(int whatData, int[] rowIds, String colname)
	{
		CounterTableModel data = null;

		if      (whatData == DATA_ABS)  data = getCounterDataAbs();
		else if (whatData == DATA_DIFF) data = getCounterDataDiff();
		else if (whatData == DATA_RATE) data = getCounterDataRate();
		else
			throw new RuntimeException("Only ABS, DIFF, or RATE data is available.");

		if (data == null)
			return null;

		int colPos = data.findColumn(colname);
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
		return new Double(maxResult);
	}

	// Return the MIN of the values of a column (ColumnName)
	private synchronized Double getMinValue(int whatData, int[] rowIds, String colname)
	{
		CounterTableModel data = null;

		if      (whatData == DATA_ABS)  data = getCounterDataAbs();
		else if (whatData == DATA_DIFF) data = getCounterDataDiff();
		else if (whatData == DATA_RATE) data = getCounterDataRate();
		else
			throw new RuntimeException("Only ABS, DIFF, or RATE data is available.");

		if (data == null)
			return null;

		int colPos = data.findColumn(colname);
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
		return new Double(minResult);
	}

	// Return the sum of the values of a Long column (ColumnName)
	private synchronized Double getSumValue(int whatData, int[] rowIds, String colname)
	{
		CounterTableModel data = null;

		if      (whatData == DATA_ABS)  data = getCounterDataAbs();
		else if (whatData == DATA_DIFF) data = getCounterDataDiff();
		else if (whatData == DATA_RATE) data = getCounterDataRate();
		else
			throw new RuntimeException("Only ABS, DIFF, or RATE data is available.");

		if (data == null)
			return null;

		int colPos = data.findColumn(colname);
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
		return new Double(result);
	}

	// Return the sum of the values of a Long column (ColumnName)
	private synchronized int getCountGtZero(int whatData, int[] rowIds, String colname)
	{
		CounterTableModel data = null;

		if      (whatData == DATA_ABS)  data = getCounterDataAbs();
		else if (whatData == DATA_DIFF) data = getCounterDataDiff();
		else if (whatData == DATA_RATE) data = getCounterDataRate();
		else
			throw new RuntimeException("Only ABS, DIFF, or RATE data is available.");

		if (data == null)
			return 0;

		int colPos = data.findColumn(colname);
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
	private synchronized Double getAvgValue(int whatData, int[] rowIds, String colname)
	{
		CounterTableModel data = null;

		if      (whatData == DATA_ABS)  data = getCounterDataAbs();
		else if (whatData == DATA_DIFF) data = getCounterDataDiff();
		else if (whatData == DATA_RATE) data = getCounterDataRate();
		else
			throw new RuntimeException("Only ABS, DIFF, or RATE data is available.");

		if (data == null)
			return null;

		Double sum = getSumValue(whatData, rowIds, colname);
		if (sum == null)
			return null;

		int count = data.getRowCount();
		if (rowIds != null)
			count = rowIds.length;
		
		if (count == 0)
			return new Double(0);
		else
			return new Double(sum.doubleValue() / count);
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
		
		if (count == 0)
			return new Double(0);
		else
			return new Double(sum.doubleValue() / count);
	}

	// Return the AVG of the values of a Long column (ColumnName)
	private synchronized Double getAvgValueGtZero(int whatData, int[] rowIds, String colname)
	{
		CounterTableModel data = null;

		if      (whatData == DATA_ABS)  data = getCounterDataAbs();
		else if (whatData == DATA_DIFF) data = getCounterDataDiff();
		else if (whatData == DATA_RATE) data = getCounterDataRate();
		else
			throw new RuntimeException("Only ABS, DIFF, or RATE data is available.");

		if (data == null)
			return null;

		Double sum = getSumValue(whatData, rowIds, colname);
		if (sum == null)
			return null;

		int count = getCountGtZero(whatData, rowIds, colname);
		
		if (count == 0)
			return new Double(0);
		else
			return new Double(sum.doubleValue() / count);
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
			return new Double(0);
		else
			return new Double(sum.doubleValue() / count);
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

	//--------------------------------------------------------------
	// Wrapper functions to read ABSOLUTE values
	//--------------------------------------------------------------
	public String getAbsString        (int    rowId, int    colPos)  { Object o = getValue     (DATA_ABS, rowId, colPos);  return (o==null)?"":o.toString(); }
	public String getAbsString        (int    rowId, String colname) { Object o = getValue     (DATA_ABS, rowId, colname); return (o==null)?"":o.toString(); }
	public String getAbsString        (String pkStr, String colname) { Object o = getValue     (DATA_ABS, pkStr, colname); return (o==null)?"":o.toString(); }
	public Object getAbsValue         (int    rowId, int    colPos)  { Object o = getValue     (DATA_ABS, rowId, colPos);  return o; }
	public Object getAbsValue         (int    rowId, String colname) { return getValue         (DATA_ABS, rowId, colname); }
	public Object getAbsValue         (String pkStr, String colname) { return getValue         (DATA_ABS, pkStr, colname); }
	public Double getAbsValueAsDouble (int    rowId, int    colPos)  { return getValueAsDouble (DATA_ABS, rowId, colPos);  }
	public Double getAbsValueAsDouble (int    rowId, String colname) { return getValueAsDouble (DATA_ABS, rowId, colname); }
	public Double getAbsValueAsDouble (String pkStr, String colname) { return getValueAsDouble (DATA_ABS, pkStr, colname); }
	public Double getAbsValueMax      (int    colPos)                { return getMaxValue      (DATA_ABS, null,  colPos);  }
	public Double getAbsValueMax      (String colname)               { return getMaxValue      (DATA_ABS, null,  colname); }
	public Double getAbsValueMin      (int    colPos)                { return getMinValue      (DATA_ABS, null,  colPos);  }
	public Double getAbsValueMin      (String colname)               { return getMinValue      (DATA_ABS, null,  colname); }
	public Double getAbsValueAvg      (int    colPos)                { return getAvgValue      (DATA_ABS, null,  colPos);  }
	public Double getAbsValueAvg      (String colname)               { return getAvgValue      (DATA_ABS, null,  colname); }
	public Double getAbsValueAvgGtZero(int    colPos)                { return getAvgValueGtZero(DATA_ABS, null,  colPos);  }
	public Double getAbsValueAvgGtZero(String colname)               { return getAvgValueGtZero(DATA_ABS, null,  colname); }
	public Double getAbsValueSum      (int    colPos)                { return getSumValue      (DATA_ABS, null,  colPos);  }
	public Double getAbsValueSum      (String colname)               { return getSumValue      (DATA_ABS, null,  colname); }
	public String getAbsPkValue       (int    rowId)                 { return getPkValue       (DATA_ABS, rowId  ); }

	public int[]  getAbsRowIdsWhere   (String colname, String colval){ return getRowIdsWhere   (DATA_ABS, colname, colval); }
	public Double getAbsValueMax      (int[] rowIds, String colname) { return getMaxValue      (DATA_ABS, rowIds,  colname); }
	public Double getAbsValueMin      (int[] rowIds, String colname) { return getMinValue      (DATA_ABS, rowIds,  colname); }
	public Double getAbsValueAvg      (int[] rowIds, String colname) { return getAvgValue      (DATA_ABS, rowIds,  colname); }
	public Double getAbsValueAvgGtZero(int[] rowIds, String colname) { return getAvgValueGtZero(DATA_ABS, rowIds,  colname); }
	public Double getAbsValueSum      (int[] rowIds, String colname) { return getSumValue      (DATA_ABS, rowIds,  colname); }

	//--------------------------------------------------------------
	// Wrapper functions to read DIFF (new-old) values
	//--------------------------------------------------------------
	public String getDiffString        (int    rowId, int    colPos)  { Object o = getValue     (DATA_DIFF, rowId, colPos);  return (o==null)?"":o.toString(); }
	public String getDiffString        (int    rowId, String colname) { Object o = getValue     (DATA_DIFF, rowId, colname); return (o==null)?"":o.toString(); }
	public String getDiffString        (String pkStr, String colname) { Object o = getValue     (DATA_DIFF, pkStr, colname); return (o==null)?"":o.toString(); }
	public Object getDiffValue         (int    rowId, int    colPos)  { Object o = getValue     (DATA_DIFF, rowId, colPos);  return o; }
	public Object getDiffValue         (int    rowId, String colname) { return getValue         (DATA_DIFF, rowId, colname); }
	public Object getDiffValue         (String pkStr, String colname) { return getValue         (DATA_DIFF, pkStr, colname); }
	public Double getDiffValueAsDouble (int    rowId, int    colPos)  { return getValueAsDouble (DATA_DIFF, rowId, colPos);  }
	public Double getDiffValueAsDouble (int    rowId, String colname) { return getValueAsDouble (DATA_DIFF, rowId, colname); }
	public Double getDiffValueAsDouble (String pkStr, String colname) { return getValueAsDouble (DATA_DIFF, pkStr, colname); }
	public Double getDiffValueMax      (int    colPos)                { return getMaxValue      (DATA_DIFF, null,  colPos);  }
	public Double getDiffValueMax      (String colname)               { return getMaxValue      (DATA_DIFF, null,  colname); }
	public Double getDiffValueMin      (int    colPos)                { return getMinValue      (DATA_DIFF, null,  colPos);  }
	public Double getDiffValueMin      (String colname)               { return getMinValue      (DATA_DIFF, null,  colname); }
	public Double getDiffValueAvg      (int    colPos)                { return getAvgValue      (DATA_DIFF, null,  colPos);  }
	public Double getDiffValueAvg      (String colname)               { return getAvgValue      (DATA_DIFF, null,  colname); }
	public Double getDiffValueAvgGtZero(int    colPos)                { return getAvgValueGtZero(DATA_DIFF, null,  colPos);  }
	public Double getDiffValueAvgGtZero(String colname)               { return getAvgValueGtZero(DATA_DIFF, null,  colname); }
	public Double getDiffValueSum      (int    colPos)                { return getSumValue      (DATA_DIFF, null,  colPos);  }
	public Double getDiffValueSum      (String colname)               { return getSumValue      (DATA_DIFF, null,  colname); }
	public String getDiffPkValue       (int    rowId)                 { return getPkValue       (DATA_DIFF, rowId  ); }

	public int[]  getDiffRowIdsWhere   (String colname, String colval){ return getRowIdsWhere   (DATA_DIFF, colname, colval); }
	public Double getDiffValueMax      (int[] rowIds, String colname) { return getMaxValue      (DATA_DIFF, rowIds,  colname); }
	public Double getDiffValueMin      (int[] rowIds, String colname) { return getMinValue      (DATA_DIFF, rowIds,  colname); }
	public Double getDiffValueAvg      (int[] rowIds, String colname) { return getAvgValue      (DATA_DIFF, rowIds,  colname); }
	public Double getDiffValueAvgGtZero(int[] rowIds, String colname) { return getAvgValueGtZero(DATA_DIFF, rowIds,  colname); }
	public Double getDiffValueSum      (int[] rowIds, String colname) { return getSumValue      (DATA_DIFF, rowIds,  colname); }

	//--------------------------------------------------------------
	// Wrapper functions to read RATE DIFF/time values
	//--------------------------------------------------------------
	public String getRateString        (int    rowId, int    colPos)  { Object o = getValue     (DATA_RATE, rowId, colPos);  return (o==null)?"":o.toString(); }
	public String getRateString        (int    rowId, String colname) { Object o = getValue     (DATA_RATE, rowId, colname); return (o==null)?"":o.toString(); }
	public String getRateString        (String pkStr, String colname) { Object o = getValue     (DATA_RATE, pkStr, colname); return (o==null)?"":o.toString(); }
	public Object getRateValue         (int    rowId, int    colPos)  { Object o = getValue     (DATA_RATE, rowId, colPos);  return o; }
	public Object getRateValue         (int    rowId, String colname) { return getValue         (DATA_RATE, rowId, colname); }
	public Object getRateValue         (String pkStr, String colname) { return getValue         (DATA_RATE, pkStr, colname); }
	public Double getRateValueAsDouble (int    rowId, int    colPos)  { return getValueAsDouble (DATA_RATE, rowId, colPos);  }
	public Double getRateValueAsDouble (int    rowId, String colname) { return getValueAsDouble (DATA_RATE, rowId, colname); }
	public Double getRateValueAsDouble (String pkStr, String colname) { return getValueAsDouble (DATA_RATE, pkStr, colname); }
	public Double getRateValueMax      (int    colPos)                { return getMaxValue      (DATA_RATE, null,  colPos);  }
	public Double getRateValueMax      (String colname)               { return getMaxValue      (DATA_RATE, null,  colname); }
	public Double getRateValueMin      (int    colPos)                { return getMinValue      (DATA_RATE, null,  colPos);  }
	public Double getRateValueMin      (String colname)               { return getMinValue      (DATA_RATE, null,  colname); }
	public Double getRateValueAvg      (int    colPos)                { return getAvgValue      (DATA_RATE, null,  colPos);  }
	public Double getRateValueAvg      (String colname)               { return getAvgValue      (DATA_RATE, null,  colname); }
	public Double getRateValueAvgGtZero(int    colPos)                { return getAvgValueGtZero(DATA_RATE, null,  colPos);  }
	public Double getRateValueAvgGtZero(String colname)               { return getAvgValueGtZero(DATA_RATE, null,  colname); }
	public Double getRateValueSum      (int    colPos)                { return getSumValue      (DATA_RATE, null,  colPos);  }
	public Double getRateValueSum      (String colname)               { return getSumValue      (DATA_RATE, null,  colname); }
	public String getRatePkValue       (int    rowId)                 { return getPkValue       (DATA_RATE, rowId  ); }

	public int[]  getRateRowIdsWhere   (String colname, String colval){ return getRowIdsWhere   (DATA_RATE, colname, colval); }
	public Double getRateValueMax      (int[] rowIds, String colname) { return getMaxValue      (DATA_RATE, rowIds,  colname); }
	public Double getRateValueMin      (int[] rowIds, String colname) { return getMinValue      (DATA_RATE, rowIds,  colname); }
	public Double getRateValueAvg      (int[] rowIds, String colname) { return getAvgValue      (DATA_RATE, rowIds,  colname); }
	public Double getRateValueAvgGtZero(int[] rowIds, String colname) { return getAvgValueGtZero(DATA_RATE, rowIds,  colname); }
	public Double getRateValueSum      (int[] rowIds, String colname) { return getSumValue      (DATA_RATE, rowIds,  colname); }

	

	public void setDataInitialized(boolean b)
	{
		dataInitialized = b;
	}
	public boolean isDataInitialized()
	{
		return dataInitialized;
	}


	public boolean hasResultSetMetaData()
	{
		return (_rsmd != null);
	}
//	public void setResultSetMetaData(ResultSetMetaData rsmd)
//	throws SQLException
//	{
//		// Copy/Clone the ResultSetMetaData some JDBC implementations needs this (otherwise we might get a 'The result set is closed', this was first seen for MS SQL-Server JDBC Driver)
//		if (rsmd == null)
//		{
//			_rsmd = null;
//		}
//		else
//		{
////			ResultSetMetaDataChangable xe = new ResultSetMetaDataChangable(rsmd);
////			_rsmd = rsmd;
//			String productName = getCounterController().getMonConnection().getDatabaseProductName(); // This should be cached... if not then we hit severe performance bottleneck
//			DbmsDataTypeResolver dataTypeResolver = getCounterController().getDbmsDataTypeResolver();
//			_rsmd = new ResultSetMetaDataCached(rsmd, dataTypeResolver, productName);
//		}
//	}
	public void setResultSetMetaData(ResultSetMetaData rsmd)
	{
		if ( ! (rsmd instanceof ResultSetMetaDataCached) )
		{
			try { rsmd = createResultSetMetaData(rsmd); }
			catch(SQLException ex)
			{
				_logger.warn("Problems creating a Cached ResultSetMetaData, continuing with the passed value.", ex);
			}
		}
		_rsmd = rsmd;
	}
	public ResultSetMetaData getResultSetMetaData()
	{
		return _rsmd;
	}
	public ResultSetMetaData createResultSetMetaData(ResultSetMetaData rsmd)
	throws SQLException
	{
		// Copy/Clone the ResultSetMetaData some JDBC implementations needs this (otherwise we might get a 'The result set is closed', this was first seen for MS SQL-Server JDBC Driver)
		if (rsmd == null)
			return null;

		// Get DBMS Product which we are connected to
		String productName = getCounterController().getMonConnection().getDatabaseProductName(); // This should be cached... if not then we hit severe performance bottleneck

		// Get DBMS DataType "resolver", null if none is installed
		DbmsDataTypeResolver dataTypeResolver = getCounterController().getDbmsDataTypeResolver();

		// Create the new object
		ResultSetMetaDataCached ret = new ResultSetMetaDataCached(rsmd, dataTypeResolver, productName);

		return ret;
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
		int      _needSrvVersion;       // no need to create proc if server is below this version

		StoredProcCheck(String dbname, String procName, Date procDateThreshold, 
				Class<?> scriptLocation, String scriptName, String needsRoleToRecreate, int needSrvVersion)
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
	 * @param w
	 * @param type
	 * @throws IOException
	 */
	private void writeJsonCounterSample(JsonWriter w, int type)
			throws IOException
	{
		// Write the TYPE
		String counterType;
		if      (type == DATA_ABS)  counterType = "absCounters";
		else if (type == DATA_DIFF) counterType = "diffCounters";
		else if (type == DATA_RATE) counterType = "rateCounters";
		else throw new IOException("Unknown type="+type);

		// Set name
		w.name(counterType);
		
		// Write an array of row objects: [ 
		//                                  { "c1":"data", "c2":"data", "c3":"data" }, 
		//                                  { "c1":"data", "c2":"data", "c3":"data" } 
		//                                ]  
		w.beginArray();
		int rowc = getRowCount(type);
		int colc = getColumnCount(type);
		for (int r=0; r<rowc; r++)
		{
			w.beginObject();
			for (int c=0; c<colc; c++)
			{
				Object obj  = getValue(type, r, c);
				String name = getColumnName(c);  
				
				if (name == null)
					throw new IOException("When writing JSON CM='"+getName()+"', CounterType="+type+", row="+r+", col="+c+", Column Name was 'null' (not set).");

				w.name(name);
				if (obj == null)
					w.nullValue();
				else
				{
					if      (obj instanceof Number)  w.value( (Number)  obj );
					else if (obj instanceof Boolean) w.value( (Boolean) obj );
					else                             w.value( obj.toString() );
				}
			}
			w.endObject();
		}
		w.endArray();		
	}
	private void writeJsonCounterSample(JsonGenerator w, int type)
			throws IOException
	{
		// Write the TYPE
		String counterType;
		if      (type == DATA_ABS)  counterType = "absCounters";
		else if (type == DATA_DIFF) counterType = "diffCounters";
		else if (type == DATA_RATE) counterType = "rateCounters";
		else throw new IOException("Unknown type="+type);

		// Set name
		w.writeFieldName(counterType);
		
		// Write an array of row objects: [ 
		//                                  { "c1":"data", "c2":"data", "c3":"data" }, 
		//                                  { "c1":"data", "c2":"data", "c3":"data" } 
		//                                ]  
		w.writeStartArray();
		int rowc = getRowCount(type);
		int colc = getColumnCount(type);
		for (int r=0; r<rowc; r++)
		{
			w.writeStartObject();
			for (int c=0; c<colc; c++)
			{
				Object obj  = getValue(type, r, c);
				String name = getColumnName(c);  
				
				if (name == null)
					throw new IOException("When writing JSON CM='"+getName()+"', CounterType="+type+", row="+r+", col="+c+", Column Name was 'null' (not set).");

				w.writeFieldName(name);
				if (obj == null)
					w.writeNull();
				else
				{
					if      (obj instanceof Number)  w.writeNumber ( obj.toString() );
					else if (obj instanceof Boolean) w.writeBoolean( (Boolean) obj  );
					else                             w.writeString ( obj.toString() );
				}
			}
			w.writeEndObject();
		}
		w.writeEndArray();		
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
	
	public void toJson(JsonGenerator w, boolean writeCounters, boolean writeGraphs)
	throws IOException
	{
//		JsonFactory jfactory = new JsonFactory();
//
//		Writer writer = new StringWriter();
//		JsonGenerator w = jfactory.createGenerator(writer);
		
		// Check MANDATORY parameters
		boolean throwOnMissingMandatoryParams = false;
		if (throwOnMissingMandatoryParams)
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
		
		w.writeStartObject();

		w.writeStringField("cmName",            getName());
		w.writeStringField("sessionSampleTime", TimeUtils.toStringIso8601(getSampleTime()));
		w.writeStringField("cmSampleTime",      TimeUtils.toStringIso8601(getTimestamp()));
		w.writeNumberField("cmSampleMs",        getLastSampleInterval());
		w.writeStringField("type",              isSystemCm() ? "SYSTEM" : "USER_DEFINED");

		// Write some statistical fields
		boolean writeStats = true;
		if (writeStats)
		{
			w.writeFieldName("sampleDetails");
			w.writeStartObject();

			w.writeNumberField ("graphCount",           getTrendGraphData() == null ? 0 : getTrendGraphData().size());
			w.writeNumberField ("absRows",              getAbsRowCount());
			w.writeNumberField ("diffRows",             getDiffRowCount());
			w.writeNumberField ("rateRows",             getRateRowCount());

			w.writeNumberField ("sqlRefreshTime",       getSqlRefreshTime());
			w.writeNumberField ("guiRefreshTime",       getGuiRefreshTime());
			w.writeNumberField ("lcRefreshTime",        getLcRefreshTime());
			w.writeBooleanField("hasNonConfiguredMonitoringHappened",   hasNonConfiguredMonitoringHappened());
			w.writeStringField ("nonConfiguredMonitoringMissingParams", getNonConfiguredMonitoringMissingParams());
			w.writeStringField ("nonConfiguredMonitoringMessage",       getNonConfiguredMonitoringMessage(false));
			w.writeBooleanField("isCountersCleared",    isCountersCleared());
			w.writeBooleanField("hasValidSampleData",   hasValidSampleData());
			w.writeStringField ("exceptionMsg",         getSampleException() == null ? null : getSampleException().toString());
			w.writeStringField ("exceptionFullText",    StringUtil.exceptionToString(getSampleException()));
			
			w.writeEndObject(); // END: Counters
		}

		if (writeCounters)
		{
			w.writeFieldName("counters");
			w.writeStartObject();

			if (hasResultSetMetaData())
			{
				ResultSetMetaDataCached rsmd = (ResultSetMetaDataCached) getResultSetMetaData();
				try
				{
					w.writeFieldName("metaData");
					w.writeStartArray(); 

					// { "colName" : "someColName", "jdbcTypeName" : "java.sql.Types.DECIMAL", "guessedDbmsType" : "decimal(16,1)" }
					for (int c=1; c<=rsmd.getColumnCount(); c++) // Note: ResultSetMetaData starts at 1 not 0
					{
						w.writeStartObject();
						w.writeStringField ("columnName"     , rsmd.getColumnLabel(c));
						w.writeStringField ("jdbcTypeName"   , ResultSetTableModel.getColumnJavaSqlTypeName(rsmd.getColumnType(c)));
						w.writeStringField ("javaClassName"  , rsmd.getColumnClassName(c));
						w.writeStringField ("guessedDbmsType", ResultSetTableModel.getColumnTypeName(rsmd, c));
						w.writeBooleanField("isDiffColumn"   , isDiffColumn(c-1)); // column pos starts at 0 in the CM
						w.writeBooleanField("isPctColumn"    , isPctColumn(c-1));  // column pos starts at 0 in the CM
						w.writeEndObject();
					}
					w.writeEndArray(); 
				}
				catch (SQLException ex)
				{
					_logger.error("Write JSON JDBC MetaData data, for CM='"+getName()+"'. Caught: "+ex, ex);
				}
			}

			if (hasAbsData())
				writeJsonCounterSample(w, DATA_ABS);

			if (hasDiffData())
				writeJsonCounterSample(w, DATA_DIFF);

			if (hasRateData())
				writeJsonCounterSample(w, DATA_RATE);

			w.writeEndObject(); // END: Counters
		}

		if (writeGraphs && hasTrendGraphData()) // note use 'hasTrendGraphData()' and NOT 'hasTrendGraph()' which is only true in GUI mode
		{
			w.writeFieldName("graphs");
			w.writeStartArray(); 
			for (String graphName : getTrendGraphData().keySet())
			{
				TrendGraphDataPoint tgdp = getTrendGraphData(graphName);

				w.writeStartObject();
				w.writeStringField ("cmName" ,           getName());
				w.writeStringField ("sessionSampleTime", TimeUtils.toStringIso8601(getSampleTime()));

				w.writeStringField ("graphName" ,     tgdp.getName());
				w.writeStringField ("graphLabel",     tgdp.getGraphLabel());
				w.writeStringField ("graphCategory",  tgdp.getCategory().toString());
				w.writeBooleanField("percentGraph",   tgdp.isPercentGraph());
				w.writeBooleanField("visibleAtStart", tgdp.isVisibleAtStart());
				w.writeFieldName("data");
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

				w.writeStartObject(); // BEGIN: data

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

						if (label == null)
						{
							if (_logger.isDebugEnabled())
								_logger.debug("Writing JSON Graph, LABEL was null, setting it to 'lbl-"+d+"'. For cm='"+getName()+"', graphName='"+graphName+"', label='"+label+"', data="+data);
							label = "lbl-"+d;
						}

						if (data == null)
						{
							if (_logger.isDebugEnabled())
								_logger.debug("Writing JSON Graph, DATA was null, setting it to 0. For cm='"+getName()+"', graphName='"+graphName+"', label='"+label+"', data="+data);
							data = 0d;
						}

						w.writeNumberField(label, data);
					}
				}
				w.writeEndObject(); // END: data
				
				w.writeEndObject(); // END: GraphName
			}
			w.writeEndArray(); 
		}

		w.writeEndObject(); // END: this CM
	}

	public void toJson(JsonWriter w, boolean writeCounters, boolean writeGraphs)
	throws IOException
	{
		w.beginObject();

		w.name("cmName")           .value(getName());
		w.name("cmSampleTime")     .value(TimeUtils.toStringIso8601(getTimestamp()));
		w.name("cmSampleMs")       .value(getLastSampleInterval());
		w.name("type")             .value(isSystemCm() ? "SYSTEM" : "USER_DEFINED");

		if (writeCounters)
		{
			w.name("counters");
			w.beginObject();

			if (hasResultSetMetaData())
			{
				ResultSetMetaDataCached rsmd = (ResultSetMetaDataCached) getResultSetMetaData();
				try
				{
					w.name("metaData");
					w.beginArray(); 

					// { "colName" : "someColName", "jdbcTypeName" : "java.sql.Types.DECIMAL", "guessedDbmsType" : "decimal(16,1)" }
					for (int c=1; c<=rsmd.getColumnCount(); c++) // Note: ResultSetMetaData starts at 1 not 0
					{
						w.beginObject();
						w.name("columnName")     .value(rsmd.getColumnLabel(c));
						w.name("jdbcTypeName")   .value(ResultSetTableModel.getColumnJavaSqlTypeName(rsmd.getColumnType(c)));
						w.name("javaClassName")  .value(rsmd.getColumnClassName(c));
						w.name("guessedDbmsType").value(ResultSetTableModel.getColumnTypeName(rsmd, c));
						w.name("isDiffColumn")   .value(isDiffColumn(c-1)); // column pos starts at 0 in the CM
						w.name("isPctColumn")    .value(isPctColumn(c-1));  // column pos starts at 0 in the CM
						w.endObject();
					}
					w.endArray();
				}
				catch (SQLException ex)
				{
					_logger.error("Write JSON JDBC MetaData data, for CM='"+getName()+"'. Caught: "+ex, ex);
				}
			}

			if (hasAbsData())
				writeJsonCounterSample(w, DATA_ABS);

			if (hasDiffData())
				writeJsonCounterSample(w, DATA_DIFF);

			if (hasRateData())
				writeJsonCounterSample(w, DATA_RATE);

			w.endObject(); // END: Counters
		}

		if (writeGraphs && hasTrendGraph())
		{
			w.name("graphs");
			w.beginArray();
			for (String graphName : getTrendGraphs().keySet())
			{
				TrendGraphDataPoint tgdp = getTrendGraphData(graphName);

				w.beginObject();
				w.name("graphName"       ).value(graphName);
//				w.name("graphDescription").value(tgdp.getDescription());
				w.name("data");
				w.beginArray();
				// loop all data
				Double[] dataArr  = tgdp.getData();
				String[] labelArr = tgdp.getLabel();
//				if (dataArr  == null) throw new IllegalArgumentException("The CM '"+getName()+"', graph '"+tgdp.getName()+"' has a null pointer for it's DATA array.");
//				if (labelArr == null) throw new IllegalArgumentException("The CM '"+getName()+"', graph '"+tgdp.getName()+"' has a null pointer for it's LABEL array.");
				if (dataArr != null && labelArr != null)
				{
					for (int d=0; d<dataArr.length; d++)
					{
						w.beginObject();

						Double data  = dataArr[d];
						String label = null;
						if (d < labelArr.length)
							label = labelArr[d];

						w.name("label")    .value(label);
						w.name("dataPoint").value(data);

						w.endObject();
					}
				}
				w.endArray();
				w.endObject(); // END: GraphName
			}
			w.endArray();
		}

		w.endObject(); // END: this CM
	}
// How a JSON might look like
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
	
}