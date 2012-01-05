/**
*/

package com.asetune.cm;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;

import org.apache.log4j.Logger;

import com.asetune.AseTune;
import com.asetune.GetCounters;
import com.asetune.MonTablesDictionary;
import com.asetune.MonWaitEventIdDictionary;
import com.asetune.RemarkDictionary;
import com.asetune.TrendGraphDataPoint;
import com.asetune.gui.MainFrame;
import com.asetune.gui.SummaryPanel;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.gui.TrendGraph;
import com.asetune.gui.swing.GTabbedPane;
import com.asetune.gui.swing.GTable.ITableTooltip;
import com.asetune.pcs.PersistentCounterHandler;
import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.AseSqlScript;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;
import com.asetune.utils.TimeUtils;
import com.sybase.jdbcx.SybConnection;
import com.sybase.jdbcx.SybMessageHandler;

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

	/** SybMessageHaandler used when querying the monitored server */
	private CmSybMessageHandler _sybMessageHandler = null;

	private boolean            _negativeDiffCountersToZero = true;

	private Timer              _refreshTimer      = new Timer(200, new RefreshTimerAction());
	private String             _name;
	private boolean            _systemCm;
	private String             _displayName       = null;  // Name that will be tabname etc
	private String             _description       = "";    // Can be used for tool tips etc
	private String             _problemDesc       = "";    // Can be used for tool tips etc
	private Exception          _sampleException   = null;

	// Sample info, this members are set by the "main" sample thread 
	private String             _serverName        = "";
	private Timestamp          _sampleTimeHead    = null;
	private Timestamp          _counterClearTime  = null;
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
	private List<String>       _activeRoleList    = null;
	private Map<String,Integer>_monitorConfigsMap = null;
	private String             _sqlInit           = null;
	private String             _sqlClose          = null; // Not used yet
	private String             _sqlRequest        = null;
	private String             _sqlWhere          = "";
	private TabularCntrPanel   tabPanel           = null;
	private List<String>       _pkCols            = null;
//	private List<String>       _pkColsOrigin      = null;

	public  static final int   DEFAULT_sqlQueryTimeout = 10;
	private int                _sqlQueryTimeout   = DEFAULT_sqlQueryTimeout;

	private String[]           _monTablesInQuery     = null;
	
	private List<String>       _dependsOnCm          = null;
	private String[]           _dependsOnRole        = null;
	private String[]           _dependsOnConfig      = null;
	private int                _dependsOnVersion     = 0;
	private int                _dependsOnCeVersion   = 0;
	private List<StoredProcCheck> _dependsOnStoredProc  = null; // containes: StoredProcCheck objects
	
	/** If we should refresh this CM in a different manner than the default refresh rate. 0=useDefault, >0=number of seconds between samples */
	private int                _postponeTime         = 0;

	/** every time the CM is refreshed set this to System.currentTimeMillis() */
	private long               _lastLocalRefreshTime = 0;

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
	private Map<String,TrendGraphDataPoint> _trendGraphsData = new HashMap<String,TrendGraphDataPoint>();

	private boolean            _filterAllZeroDiffCounters = false;
	private boolean            _sampleDataIsPaused        = false;
	private boolean            _sampleDataInBackground    = false;
	private boolean            _persistCounters           = false;
	private boolean            _persistCountersAbs        = true;
	private boolean            _persistCountersDiff       = true;
	private boolean            _persistCountersRate       = true;

	// 
	private int clearCmLevel;

	private SamplingCnt _prevSample = null;      // Contains old raw data (previous sample)
	private SamplingCnt _newSample  = null;      // Contains new raw data
	private SamplingCnt _diffData   = null;       // diff between newSample and oldSample data (not filtered)
	private SamplingCnt _rateData   = null;       // diffData / sampleInterval
	
	private int _dataSource = DATA_RATE;

	private boolean dataInitialized=false;
	private boolean firstTimeSample=true;
	private boolean _sqlInitDone=false;

	private int maxRowSeen;
	
	
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

		// basic stuff
		setInitialized(false);
		setRuntimeInitialized(false);
		_serverVersion     = 0;
		_isClusterEnabled  = false;
		_activeRoleList    = null;
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

		// reset panel, if we got one
		if (tabPanel != null)
			tabPanel.reset();
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
			String       name,             // Name of the Counter Model
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
		this(name, sql, pkList, diffColumns, pctColumns, monTables, dependsOnRole, dependsOnConfig, dependsOnVersion, dependsOnCeVersion, negativeDiffCountersToZero, systemCm, 0);
	}


	/**
	 * @param name                        Name of the Counter Model
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
			String       name,
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
		// Initialize a model for use with a JTable
		_name     = name;
		_systemCm = systemCm;

		// Check if name is OK
		checkInConstructor(); 

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

		_sybMessageHandler  = createSybMessageHandler();
		
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

		// Load saved properties
		loadProps();

		setDataInitialized(false);
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
		CountersModel cm = copyForStorage();
		
		// Remove all the rows in the CM, so that new can be added
		// if this is not done, all the old rows will still be visible when displaying it in the JTable
		cm.clearForRead();

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

		c._sybMessageHandler          = this._sybMessageHandler;

		c._refreshTimer               = null;
		c._negativeDiffCountersToZero = this._negativeDiffCountersToZero;

		c._name                       = this._name;
		c._systemCm                   = this._systemCm;
		c._displayName                = this._displayName;
		c._description                = this._description;
		c._problemDesc                = this._problemDesc;

		c._serverName                 = this._serverName;
		c._sampleTimeHead             = this._sampleTimeHead;
		c._counterClearTime           = this._counterClearTime;
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
		c._activeRoleList             = this._activeRoleList;      // no need to full copy, static usage
		c._monitorConfigsMap          = this._monitorConfigsMap;   // no need to full copy, static usage
		c._sqlInit                    = this._sqlInit;
		c._sqlClose                   = this._sqlClose;
		c._sqlRequest                 = this._sqlRequest;
		c._sqlWhere                   = this._sqlWhere;
		c.tabPanel                    = this.tabPanel;           // do we really need to copy this one?
		c._pkCols                     = this._pkCols;              // no need to full copy, static usage
//		c._pkColsOrigin               = this._pkColsOrigin;

		c._monTablesInQuery           = this._monTablesInQuery;    // no need to full copy, static usage

		c._dependsOnCm                = this._dependsOnCm;         // no need to full copy, static usage
		c._dependsOnRole              = this._dependsOnRole;       // no need to full copy, static usage
		c._dependsOnConfig            = this._dependsOnConfig;     // no need to full copy, static usage
		c._dependsOnVersion           = this._dependsOnVersion;
		c._dependsOnCeVersion         = this._dependsOnCeVersion;
		c._dependsOnStoredProc        = this._dependsOnStoredProc; // no need to full copy, static usage

		c._postponeTime               = this._postponeTime;
		c._lastLocalRefreshTime       = this._lastLocalRefreshTime;

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
		c._trendGraphsData = new HashMap<String,TrendGraphDataPoint>(_trendGraphsData);

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
		c._prevSample                 = this._prevSample == null ? null : new SamplingCnt(this._prevSample, true);
		c._newSample                  = this._newSample  == null ? null : new SamplingCnt(this._newSample,  true);
		c._diffData                   = this._diffData   == null ? null : new SamplingCnt(this._diffData,   true);
		c._rateData                   = this._rateData   == null ? null : new SamplingCnt(this._rateData,   true);
		
		c._dataSource                 = this._dataSource;

		c.dataInitialized             = this.dataInitialized;
		c.firstTimeSample             = this.firstTimeSample;

		c.maxRowSeen                  = this.maxRowSeen;

		c._refreshCounter             = this._refreshCounter;
		c._sumRowCount                = this._sumRowCount;
//		c._sumRowCountAbs             = this._sumRowCountAbs;
//		c._sumRowCountDiff            = this._sumRowCountDiff;
//		c._sumRowCountRate            = this._sumRowCountRate;
		
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
	public void addTableModelListener(TableModelListener l)
	{
//		System.out.println("+++addTableModelListener(l="+l+")");
		super.addTableModelListener(l);
//		printTableModelListener();
	}
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
	public Class<?> getColumnClass(int columnIndex)
	{
		if (!isDataInitialized())   return null;

		CounterTableModel data = getCounterData();
		if (data == null) return Object.class;
		return data.getColumnClass(columnIndex);
	}

	public int getColumnCount()
	{
		CounterTableModel data = getCounterData();
		int c = 0;
		if (isDataInitialized() && data != null)
			c = data.getColumnCount();
//		System.out.println(_name+":getColumnCount() <- "+c);
		return c;
    }

	public String getColumnName(int col)
	{
		CounterTableModel data = getCounterData();
		String s = null;
		if (isDataInitialized() && data != null)
			s = data.getColumnName(col);
//		System.out.println(_name+":getColumnName(col="+col+") <- '"+s+"'.");
		return s;
	}

	public int getRowCount()
	{
		int c = 0;
		CounterTableModel data = getCounterData();
		if (isDataInitialized() && data != null)
			c = data.getRowCount();
//		System.out.println(_name+":getRowCount() <- "+c);
		return c;
    }

	public Object getValueAt(int row, int col)
	{
		if (!isDataInitialized())   return null;

		CounterTableModel data = getCounterData();
		if (data == null) return null;
		return data.getValueAt(row, col);
    }

	public boolean isCellEditable(int rowIndex, int columnIndex)
	{
		return false;
    }

//	public void setValueAt(Object value, int rowIndex, int columnIndex)
//	{
//	}
	
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
	 * Used to set off-line counter data<br>
	 * Most likely has to be override to handle local offline data for subclasses
	 */
	public void setValueAt(int type, Object value, int row, int col)
	{
		CounterTableModel data = null;
		if      (type == DATA_ABS)  { if (_newSample == null) {_newSample = new SamplingCnt("offline-abs",  false, null); data = _newSample;} else data = _newSample;}
		else if (type == DATA_DIFF) { if (_diffData  == null) {_diffData  = new SamplingCnt("offline-diff", false, null); data = _diffData;}  else data = _diffData;}
		else if (type == DATA_RATE) { if (_rateData  == null) {_rateData  = new SamplingCnt("offline-rate", false, null); data = _rateData;}  else data = _rateData;}
		else
			throw new RuntimeException("Only ABS, DIFF, or RATE data is available.");

		// NOTE: this might suffer from performance problems if: fireTableCellUpdated() 
		//       on the JTable is fired...
		data.setValueAt(value, row, col);
	}

	/** 
	 * Used to set off-line counter data<br>
	 * Most likely has to be override to handle local offline data for subclasses
	 */
	public void setColumnNames(int type, List<String> cols)
	{
//		CounterTableModel data = null;
		SamplingCnt data = null;
		if      (type == DATA_ABS)  { if (_newSample == null) {_newSample = new SamplingCnt("offline-abs",  false, null); data = _newSample;} else data = _newSample;}
		else if (type == DATA_DIFF) { if (_diffData  == null) {_diffData  = new SamplingCnt("offline-diff", false, null); data = _diffData;}  else data = _diffData;}
		else if (type == DATA_RATE) { if (_rateData  == null) {_rateData  = new SamplingCnt("offline-rate", false, null); data = _rateData;}  else data = _rateData;}
		else
			throw new RuntimeException("Only ABS, DIFF, or RATE data is available.");

		data.setColumnNames(cols);
		initColumnStuff(data);
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
	
	public String    getServerName()       { return _serverName; }
	public Timestamp getSampleTimeHead()   { return _sampleTimeHead; }
	public Timestamp getCounterClearTime() { return _counterClearTime; }
	public Timestamp getSampleTime()       { return _sampleTime; }
	public long      getSampleInterval()   { return _sampleInterval; }

	public void setServerName(String name)               { _serverName = name; }
	public void setSampleTimeHead(Timestamp timeHead)    { _sampleTimeHead = timeHead; }
	public void setCounterClearTime(Timestamp clearTime) { _counterClearTime = clearTime; }
	public void setSampleTime(Timestamp time)            { _sampleTime = time; }
	public void setSampleInterval(long interval)         { _sampleInterval = interval; }
	public void setTimeInfo(Timestamp timeHead, Timestamp clearTime, Timestamp sampleTime, long intervall)
	{
		setSampleTimeHead(timeHead);
		setCounterClearTime(clearTime);
		setSampleTime(sampleTime);
		setSampleInterval(intervall);
	}

	/** How many milliseconds did we spend on executing the SQL statement that fetched the performance counters */
	public long getSqlRefreshTime()      { return _sqlRefreshTime; }
	/** Used by the PersistReader to set refresh time */
	public void setSqlRefreshTime(int t) { _sqlRefreshTime = t; }
	public void beginSqlRefresh() { _sqlRefreshStartTime = System.currentTimeMillis(); }
	public void endSqlRefresh()   {	_sqlRefreshTime = System.currentTimeMillis() - _sqlRefreshStartTime; }

	/** How many milliseconds did we spend on executing the GUI code that updated the GUI */
	public long getGuiRefreshTime()      { return _guiRefreshTime; }
	/** Used by the PersistReader to set refresh time */
	public void setGuiRefreshTime(int t) { _guiRefreshTime = t; }
	public void beginGuiRefresh() { _guiRefreshStartTime = System.currentTimeMillis(); }
	public void endGuiRefresh()   {	_guiRefreshTime = System.currentTimeMillis() - _guiRefreshStartTime; }

	/** How many milliseconds did we spend on executing the LocalCalculation code */
	public long getLcRefreshTime()      { return _lcRefreshTime; }
	/** Used by the PersistReader to set refresh time */
	public void setLcRefreshTime(int t) { _lcRefreshTime = t; }
	/** Begin time of Local Calculation */
	public void beginLcRefresh() { _lcRefreshStartTime = System.currentTimeMillis(); }
	/** End time of Local Calculation */
	public void endLcRefresh()   {	_lcRefreshTime = System.currentTimeMillis() - _lcRefreshStartTime; }

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
		if (AseTune.hasGUI())
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

			// Current TAB is visible
			if ( equalsTabPanel(MainFrame.getActiveTab()) )
				refresh = true;

			// Current TAB is un-docked (in it's own window)
			if (getTabPanel() != null)
			{
				JTabbedPane tp = MainFrame.getTabbedPane();
				if (tp instanceof GTabbedPane)
				{
					GTabbedPane gtp = (GTabbedPane) tp;
					if (gtp.isTabUnDocked(getDisplayName()))
						refresh = true;
				}
			}

			// Current CM has active graphs
			if ( hasActiveGraphs() )
				refresh = true;

			// Background poll is checked
			if ( isBackgroundDataPollingEnabled() )
				refresh = true;

			// Store data in DB && we have storage
			if ( isPersistCountersEnabled() && PersistentCounterHandler.getInstance() != null)
				refresh = true;

			// NO-REFRESH if data polling is PAUSED
			if ( isDataPollingPaused() )
				refresh = false;

			// Check postpone
			if ( getTimeToNextPostponedRefresh() > 0 )
			{
				_logger.debug("Next refresh for the cm '"+getName()+"' will have to wait '"+TimeUtils.msToTimeStr(getTimeToNextPostponedRefresh())+"'.");
				refresh = false;
			}

			// If we are not connected anymore, do not try to refresh
			if (refresh)
				refresh = AseTune.getCounterCollector().isMonConnected(false, true);

			if ( ! refresh)
				setValidSampleData(false);

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

			// Check postpone
			if ( getTimeToNextPostponedRefresh() > 0 )
			{
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

	
	public void setDiffDissColumns(String[] cols) { _diffDissColumns = cols; }
	public void setDiffColumns(String[] cols)     { _diffColumns     = cols; }
	public void setPctColumns(String[] cols)      { _pctColumns      = cols; }

	public String[] getDiffDissColumns()     { return _diffDissColumns; }
	public String[] getDiffColumns()         { return _diffColumns; }
	public String[] getPctColumns()          { return _pctColumns; }

	public boolean isDiffDissColumn(int col) { return _isDiffDissCol == null ? false : _isDiffDissCol[col]; }
	public boolean isDiffColumn(int col)     { return _isDiffCol     == null ? false : _isDiffCol[col]; }
	public boolean isPctColumn(int col)      { return _isPctCol      == null ? false : _isPctCol[col]; }

	public boolean discardDiffPctHighlighterOnAbsTable() { return _dataSource == DATA_ABS; }

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
		SamplingCnt data = null;

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
		SamplingCnt data = null;

		if      (whatData == DATA_ABS)  data = _newSample;
		else if (whatData == DATA_DIFF) data = _diffData;
		else if (whatData == DATA_RATE) data = _rateData;
		else
			throw new RuntimeException("Only ABS, DIFF, or RATE data is available.");

		if (data == null)
			return null;

		return data.getColNames();
	}

	public TabularCntrPanel getTabPanel()
	{
		return tabPanel;
	}
	public void setTabPanel(TabularCntrPanel tp)
	{
		tabPanel = tp;

		if (tabPanel != null)
		{
			tabPanel.setCm(this);
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
			_logger.debug("getName().setClientProperty(key='"+key+"', value='"+value+"') valueDataType='"+(value==null?"null":value.getClass().getName())+"'.");

		_clientProperty.put(key, value);
	}

	/**
	 * Get a specific property that we need to grab somewhere else<br>
	 * This can fetch various Components or objects that is loosely connected to this CM, 
	 * could for example have been created in a extended CM Constructor which we need 
	 * to be able to grab at a later stage
	 * 
	 * @param key name of the loosely connected object
	 * @return the object, if it couldnt be found a null value will be returned
	 */
	public Object getClientProperty(String key)
	{
		Object obj = _clientProperty.get(key);
		if (_logger.isDebugEnabled())
		_logger.debug("getName().getClientProperty(key='"+key+"') returns='"+obj+"', type='"+(obj==null?"null":obj.getClass().getName())+"'.");
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
	 * Get tooltip for a specific Table Column
	 * @param colName
	 * @return the tooltip
	 */
	@Override
	public String getToolTipTextOnTableColumnHeader(String colName)
	{
		return MonTablesDictionary.getInstance().getDescription(getMonTablesInQuery(), colName);
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
		// Get tip on WaitEventID
		if ("WaitEventID".equals(colName))
		{
			//Object cellVal = getValueAt(modelRow, modelCol);
			if (cellValue instanceof Number)
			{
				int waitEventId = ((Number)cellValue).intValue();
				if (waitEventId > 0)
					return MonWaitEventIdDictionary.getInstance().getToolTipText(waitEventId);
			}
		}

		// Get tip on Remark (at least in CMobjectActivity/CM_NAME__OBJECT_ACTIVITY)
		if ("Remark".equals(colName))
		{
			//Object cellVal = getValueAt(modelRow, modelCol);
			if (cellValue instanceof String)
			{
				String key = (String)cellValue;
				if ( ! StringUtil.isNullOrBlank(key) )
					return RemarkDictionary.getInstance().getToolTipText(key);
			}
		}

		// If we are CONNECTED and we have a USER DEFINED TOOLTIP for this columns
		if (cellValue != null)
		{
			String sql = MainFrame.getUserDefinedToolTip(getName(), colName);

			if ( sql != null && ! AseTune.getCounterCollector().isMonConnected() )
			{
				// IF SPID, get values from JTable in OFFLINE MODE
				if ("SPID".equalsIgnoreCase(colName))
				{
					if (MainFrame.isOfflineConnected())
					{
						CountersModel cm = GetCounters.getCmByName(GetCounters.CM_NAME__PROCESS_ACTIVITY);
						TabularCntrPanel tcp = cm.getTabPanel();
						if (tcp != null)
						{
							tcp.tabSelected();
							cm = tcp.getDisplayCm();
							if (cm != null)
							{
								CounterTableModel ctmAbs  = cm.getCounterDataAbs();
								CounterTableModel ctmDiff = cm.getCounterDataDiff();
								CounterTableModel ctmRate = cm.getCounterDataRate();
								if (ctmRate != null)
								{
									int spid_pos = ctmRate.findColumn("SPID");
									int rowCount = ctmRate.getRowCount();
									for (int r=0; r<rowCount; r++)
									{
										if ( cellValue.equals(ctmRate.getValueAt(r, spid_pos)) )
										{
											StringBuilder sb = new StringBuilder(300);
											sb.append("<html>\n");
											sb.append("<table border=0 cellspacing=0 >\n");
//											sb.append("<table border=1 cellspacing=0 >\n");
//											sb.append("<table BORDER=1 CELLSPACING=0 CELLPADDING=0>\n");

											sb.append("<tr>");
											sb.append("<td nowrap bgcolor=\"#cccccc\"><font color=\"#000000\"><b>").append("Column Name")      .append("</b></font></td>");
											sb.append("<td nowrap bgcolor=\"#cccccc\"><font color=\"#000000\"><b>").append("Absolute Counters").append("</b></font></td>");
											sb.append("<td nowrap bgcolor=\"#cccccc\"><font color=\"#000000\"><b>").append("Diff Counters")    .append("</b></font></td>");
											sb.append("<td nowrap bgcolor=\"#cccccc\"><font color=\"#000000\"><b>").append("Rate Counters")    .append("</b></font></td>");
											sb.append("</tr>\n");

											for (int c=0; c<ctmRate.getColumnCount(); c++)
											{
//												System.out.println("XXXX: colName='"+ctm.getColumnName(c)+"', value='"+ctm.getValueAt(r, c)+"'.");

												if ( (c % 2) == 0 )
													sb.append("<tr bgcolor=\"#ffffff\">"); // white
												else
													sb.append("<tr bgcolor=\"#ffffcc\">"); // light yellow

												sb.append("<td nowrap bgcolor=\"#cccccc\"><font color=\"#000000\"><b>").append(ctmRate.getColumnName(c)).append("</b></font></td>");

												sb.append("<td nowrap>").append(ctmAbs ==null?"":ctmAbs .getValueAt(r, c)).append("</td>");
												sb.append("<td nowrap>").append(ctmDiff==null?"":ctmDiff.getValueAt(r, c)).append("</td>");
												sb.append("<td nowrap>").append(ctmRate==null?"":ctmRate.getValueAt(r, c)).append("</td>");
												sb.append("</tr>\n");
											}
											sb.append("</table>\n");
											sb.append("</html>\n");
											return sb.toString();
										}
									}
								}
							}
						}
					} // end: offline
				} // end: SPID

				return "<html>" +
				       "No runtime tool tip available for '"+colName+"'. <br>" +
				       "Not connected to the monitored server.<br>" +
				       "</html>";
			}

			if (sql != null)
			{
				try
				{
					Connection conn = AseTune.getCounterCollector().getMonConnection();

					StringBuilder sb = new StringBuilder(300);
					sb.append("<html>\n");
//					sb.append("<table BORDER=1 CELLSPACING=0 CELLPADDING=0>\n");
					sb.append("<table border=1>\n");

					PreparedStatement stmt = conn.prepareStatement(sql);
					stmt.setObject(1, cellValue);

					ResultSet rs = stmt.executeQuery();
					ResultSetMetaData rsmd = rs.getMetaData();
					int cols = rsmd.getColumnCount();

					sb.append("<tr>");
					for (int c=1; c<=cols; c++)
						sb.append("<td nowrap>").append(rsmd.getColumnName(c)).append("</td>");
					sb.append("</tr>\n");

					while (rs.next())
					{
						sb.append("<tr>");
						for (int c=1; c<=cols; c++)
							sb.append("<td nowrap>").append(rs.getObject(c)).append("</td>");
						sb.append("</tr>\n");
					}
					sb.append("</table>\n");
					sb.append("</html>\n");

					for (SQLWarning sqlw = stmt.getWarnings(); sqlw != null; sqlw = sqlw.getNextWarning())
					{
						// IGNORE: DBCC execution completed. If DBCC printed error messages, contact a user with System Administrator (SA) role.
						if (sqlw.getMessage().startsWith("DBCC execution completed. If DBCC"))
							continue;

						sb = sb.append(sqlw.getMessage()).append("<br>");
					}
					rs.close();
					stmt.close();
					
					return sb.toString();
				}
				catch (SQLException ex)
				{
					_logger.warn("Problems when executing sql: "+sql, ex);
					return "<html>" +  
					       "Problems when executing sql: "+sql+"<br>" +
					       ex.toString() +
					       "</html>";
				}
			}
		}

		return null;
	}

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
	 * Add a TrendGraph to this cm
	 * 
	 * @param name Name of the Graph
	 * @param tg Trend Graph object
	 * @param addToSummary add this graph to the MainFrame summary panel.
	 */
	public void addTrendGraph(String name, TrendGraph tg, boolean addToSummary)
	{
		_trendGraphs.put(name, tg);
		
		tg.setCm(this);

		if (addToSummary)
		{
			MainFrame.addGraphViewMenu( tg.getViewMenuItem() );
			SummaryPanel.getInstance().addTrendGraph(tg);
		}
	}
	
	public TrendGraph getTrendGraph(String name)
	{
		return (TrendGraph) _trendGraphs.get(name);
	}
	
	public Map<String,TrendGraph> getTrendGraphs()
	{
		return _trendGraphs;
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
			tgdp.setDate(this.getTimestamp());
			tgdp.setData(dataArray);
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

				labelArray[i] = labelObj.toString();
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
			tgdp.setDate(this.getTimestamp());
			tgdp.setData(dataArray);
			tgdp.setLabel(labelArray);
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
	 *  and if not overriden, it will simple just call updateGraphData(TrendGraphDataPoint) foreach installed TrendGraphDataPoint */
	public void updateGraphData()
	{
		if (_trendGraphsData.size() == 0)
			return;

		for (Map.Entry<String,TrendGraphDataPoint> entry : _trendGraphsData.entrySet()) 
		{
		//	String              graphName = entry.getKey();
			TrendGraphDataPoint tgdp      = entry.getValue();

			updateGraphData(tgdp);
			
			//System.out.println("cm='"+StringUtil.left(this.getName(),25)+"', _trendGraphData="+tgdp);
			if (_logger.isDebugEnabled())
				_logger.debug("cm='"+StringUtil.left(this.getName(),25)+"', _trendGraphData="+tgdp);
		}
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
	public void addTrendGraphData(String name, TrendGraphDataPoint tgdp)
	{
		_trendGraphsData.put(name, tgdp);
	}
	
	public void addTrendGraphData(String name)
	{
		TrendGraphDataPoint tgdp = new TrendGraphDataPoint(name);
		addTrendGraphData(name, tgdp);
	}

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


	public boolean equalsTabPanel(Component comp)
	{
		return comp.equals(tabPanel);
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
	public void setActiveRoles(List<String> activeRoleList)
	{
		_activeRoleList = activeRoleList;
	}
	/** check if the <b>locally cached</b> List of role names contains the role */
	public boolean isRoleActive(String role)
	{
		if ( ! isRuntimeInitialized() ) throw new RuntimeException("This can't be called before the CM has been connected to any monitored server.");
		if (_activeRoleList == null) 
			return false;

		return _activeRoleList.contains(role);
	}
	
	/** */
	public void setMonitorConfigs(Map<String,Integer> monitorConfigs)
	{
		_monitorConfigsMap = monitorConfigs;
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
		return AseConnectionUtils.versionIntToStr( getServerVersion() );
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
	public void setActive(boolean b, String problemDescription)
	{
		_isActive    = b;
		_problemDesc = problemDescription;
		if (_problemDesc == null)
			_problemDesc = "";
		
		if (tabPanel != null)
		{
			tabPanel.setEnabled(b);
		}
	}
	/** */
	public String getProblemDesc()
	{
		return _problemDesc;
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
		doSqlClose(AseTune.getCounterCollector().getMonConnection());
	}
	public void doSqlClose(Connection conn)
	{
		if (conn == null)
			throw new IllegalArgumentException("The passed conn is null.");

		String sql = getSqlClose();
		if (sql != null && !sql.trim().equals(""))
		{
			try
			{
				Statement stmt = conn.createStatement();
				stmt.execute(sql);
				stmt.close();
			}
			catch (SQLException e)
			{
				_logger.warn("Problem when executing the 'close' SQL statement.", e);
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
			if (AseTune.getCounterCollector().isMonConnected())
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
			if ( ! isRoleActive(roleName) )
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

			_logger.debug(getName() + ": should be HIDDEN.");
			_logger.warn("When trying to initialize Counters Models ("+getName()+") The following role(s) were needed '"+StringUtil.toCommaStr(dependsOnRole)+"', and you do not have the following role(s) '"+didNotHaveRoles+"'.");

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
		_logger.debug("Checking for ASE Configuration '"+configName+"', which has value '"+configHasValue+"'. Option to re-configure to value '"+reConfigValue+"' if not set.");

		// In NO_GUI mode, we might want to auto configure monitoring...
		boolean doReconfigure = false;
		if ( ! AseTune.hasGUI() )
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
				_logger.debug("After re-config, the ASE Configuration '"+configName+"', now has value '"+configHasValue+"'.");
			}
		}

		if (configHasValue > 0)
		{
			_logger.debug(getName() + ": should be VISABLE.");
			return true;
		}
		else
		{
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

	/** 
	 * Convert a int version to a string version 
	 * <p>
	 * <code>15030 will be "15.0.3"</code>
	 * <code>15031 will be "15.0.3 ESD#1"</code>
	 */
	public String getDependsOnVersionStr()
	{
		return AseConnectionUtils.versionIntToStr( getDependsOnVersion() );
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
		return AseConnectionUtils.versionIntToStr( getDependsOnCeVersion() );
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
			_logger.debug(getName() + ": should be HIDDEN.");
			_logger.warn("When trying to initialize Counters Models ("+getName()+") in ASE Version "+getServerVersionStr()+", I need at least ASE Version "+getDependsOnVersionStr()+" for that.");

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
			_logger.debug(getName() + ": should be HIDDEN.");
			_logger.warn("When trying to initialize Counters Models ("+getName()+") in ASE Cluster Edition Version "+getServerVersionStr()+", I need at least ASE Cluster Edition Version "+getDependsOnCeVersionStr()+" for that.");

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
	public void addDependsOnStoredProc(String dbname, String procName, Date procDateThreshold, Class<?> scriptLocation, String scriptName, String needsRoleToRecreate)
	{
		if (dbname            == null) throw new IllegalArgumentException("addDependsOnStoredProc(): 'dbname' cant be null");
		if (procName          == null) throw new IllegalArgumentException("addDependsOnStoredProc(): 'procName' cant be null");
		if (procDateThreshold == null) throw new IllegalArgumentException("addDependsOnStoredProc(): 'procDateThreshold' cant be null");
		if (scriptLocation    == null) throw new IllegalArgumentException("addDependsOnStoredProc(): 'scriptLocation' cant be null");
		if (scriptName        == null) throw new IllegalArgumentException("addDependsOnStoredProc(): 'scriptName' cant be null");

		if (_dependsOnStoredProc == null)
			_dependsOnStoredProc = new LinkedList<StoredProcCheck>();

		StoredProcCheck spc = new StoredProcCheck(dbname, procName, procDateThreshold, scriptLocation, scriptName, needsRoleToRecreate);
		_dependsOnStoredProc.add(spc);
	}
	
	public boolean checkDependsOnStoredProc(Connection conn, String dbname, String procName, Date procDateThreshold, Class<?> scriptLocation, String scriptName, String needsRoleToRecreate)
	{
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

				_logger.debug(getName() + ": should be HIDDEN.");
				_logger.warn("When trying to initialize Counters Models ("+getName()+") in ASE Version "+getServerVersion()+", "+msg+" (connect with a user that has '"+needsRoleToRecreate+"' or load the proc from '$ASETUNE_HOME/classes' or unzip asetune.jar. under the class '"+scriptLocation.getClass().getName()+"' you will find the script '"+scriptName+"').");

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
			boolean b = checkDependsOnStoredProc(conn, spc._dbname, spc._procName, spc._procDateThreshold, spc._scriptLocation, spc._scriptName, spc._needsRoleToRecreate);
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
	 * do local calculation, this should be overridden for local calculations...
	 * 
	 * @param prevSample
	 * @param newSample
	 * @param diffData
	 */
	public void localCalculation(SamplingCnt prevSample, SamplingCnt newSample, SamplingCnt diffData)
	{
	}

	/**
	 * Check if we are connected to any "online" data source 
	 * @return
	 */
	public boolean isConnected()
	{
		return AseTune.getCounterCollector().isMonConnected();
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
			if (e instanceof DependsOnCmPostponeException)
				_logger.info(e.toString()); // do not pass the "stacktrace" in the errorlog
			else
				_logger.info(e.toString(), e);
		}
	}

	/** Get time when we last did a data refresh */
	public long getLastLocalRefreshTime()
	{ 
		return _lastLocalRefreshTime; 
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

	/** get dependant CM's postpone time */
	public CountersModel getDependantCmThatHasPostponeTime()
	{
		List<String> cmList = getDependsOnCm();
		if (cmList == null)
			return null;

		for (String cmName : cmList)
		{
			CountersModel cm = GetCounters.getCmByName(cmName);
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
		if (getPostponeTime() == 0)
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
	private void initColumnStuff(SamplingCnt cnt)
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
		List<String> cmList = getDependsOnCm();
		if (cmList == null)
			return;

		for (String cmName : cmList)
		{
			CountersModel cm = GetCounters.getCmByName(cmName);
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
			_dependsOnCm = new LinkedList<String>();
		_dependsOnCm.add(cmName);
	}
	/** Get a list of CM's that we depends upon */
	public List<String> getDependsOnCm()
	{
		return _dependsOnCm;
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

	/** called from GUI to refresh data */
	public final synchronized void refresh() throws Exception
	{
		refresh(AseTune.getCounterCollector().getMonConnection());
	}

	/** Refresh data */
	public final synchronized void refresh(Connection conn) throws Exception
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
		
		// is it time to do refresh or not
		if (getTimeToNextPostponedRefresh() > 0)
		{
			_logger.debug("Next refresh for the cm '"+getName()+"' will have to wait '"+TimeUtils.msToTimeStr(getTimeToNextPostponedRefresh())+"'.");
			// NOTE: should we do setValidSampleData(false) here or not?, I think so...
			setValidSampleData(false);
			return;
		}

		// SET the message handler used for this CM
		SybMessageHandler curMsgHandler = null;
		if (conn instanceof SybConnection)
		{
			curMsgHandler = ((SybConnection)conn).getSybMessageHandler();
			((SybConnection)conn).setSybMessageHandler(getSybMessageHandler());
		}
		
		// now MAKE the refresh
		int rowsFetched = 0;
		try
		{
			setSampleException(null);
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
			throw e;
		}
		finally
		{
			// restore old message handler
			if (curMsgHandler != null)
			{
				((SybConnection)conn).setSybMessageHandler(curMsgHandler);
			}
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
	}

	/**
	 * This is the method to override if you want to different refresh
	 * TODO: check for more Exception, so we dont leave this code without resetting: _newSample, _diffData, _rateData
	 * 
	 * @param conn
	 * @return number of rows in the new sample.
	 * @throws Exception
	 */
	protected int refreshGetData(Connection conn) throws Exception
	{
		if (_logger.isDebugEnabled())
		{
			_logger.debug("Entering refreshCM() method for " + _name);
		}

		if (conn == null)
			return -1;

		if (_logger.isDebugEnabled())
			_logger.debug("Refreshing Counters for '"+getName()+"'.");

		// Start the timer which will be kicked of after X ms
		// This si we can do something if the refresh takes to long time
		_refreshTimer.start();


		// If the CounterModel need to be initialized by executing any 
		// specific SQL statement the firts time around
		String sqlInit = getSqlInit();
		if (sqlInit != null && !sqlInit.trim().equals(""))
		{
			if ( ! _sqlInitDone )
			{
				try
				{

					Statement stmt = conn.createStatement();
					stmt.execute(sqlInit);
					stmt.close();

					_sqlInitDone = true;
				}
				catch (SQLException e)
				{
					_logger.warn("Problem when executing the 'init' SQL statement: "+sqlInit, e);
				}
			}
		}

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
		SamplingCnt tmpDiffData = null;
		SamplingCnt tmpRateData = null;
		SamplingCnt tmpNewSample = new SamplingCnt(_name, _negativeDiffCountersToZero, _diffColumns);
		try
		{
			setSampleException(null);
			beginSqlRefresh();
			tmpNewSample.getCnt(this, conn, getSql()+getSqlWhere(), _pkCols);
		}
		catch (SQLException e)
		{
			if (tabPanel != null)
			{
				String msg = e.getMessage();
				setSampleException(e);
				// maybe use property change listeners instead: firePropChanged("status", "refreshing");
				tabPanel.setWatermarkText(msg);
			}

			// No data should be visible, so reset some structures
			// NOTE: is this enough or should we do fireXXXXChange as well... 
			_newSample = null;
			_diffData  = null;
			_rateData  = null;

			return -1;
		}
		finally
		{
			// Stop the timer.
			_refreshTimer.stop();
			endSqlRefresh();
		}

		// initialize Diss/Diff/Pct column bitmaps
		initColumnStuff(tmpNewSample);

		// if it's the first time sampling...
		if (firstTimeSample)
		{
//			saveDdl();
//			firstTimeSample = false; // done later
		}

		// Used later
		final List<Integer> deletedRows = new ArrayList<Integer>();;

		// If NO PK, then we dont need to do some stuff.
		if ( ! doDiffCalc() )
		{
			setSampleTime(tmpNewSample.samplingTime);
			setSampleInterval(0);

			if (_prevSample != null)
			{
				tmpNewSample.interval = tmpNewSample.samplingTime.getTime() - _prevSample.samplingTime.getTime(); 
				setSampleInterval(tmpNewSample.interval);
			}

			tmpDiffData = tmpNewSample;
			tmpRateData = tmpNewSample;

			// Compute local stuff
			beginLcRefresh();
			localCalculation(_prevSample, tmpNewSample, null);
			endLcRefresh();

			setDataInitialized(true); // maybe move this to the Run class later, which does the init
		}
		else
		{
			if (_prevSample != null)
			{
				// old sample is not null, so we can compute the diffs
				tmpDiffData = SamplingCnt.computeDiffCnt(_prevSample, tmpNewSample, deletedRows, _pkCols, _isDiffCol);
			}
	
			if (tmpDiffData == null)
			{
				setSampleTime(tmpNewSample.samplingTime);
				setSampleInterval(0);	
			}
			else
			{
				setSampleTime(tmpDiffData.samplingTime);
				setSampleInterval(tmpDiffData.interval);
	
				beginLcRefresh();

				// Compute local stuff
				// NOTE: this needs to be done BEFORE computeRatePerSec()
				//       otherwise the PCT columns will still be the DIFF values
				localCalculation(_prevSample, tmpNewSample, tmpDiffData);
	
				// we got some data, compute the rates and update the data model
				tmpRateData = SamplingCnt.computeRatePerSec(tmpDiffData, _isDiffCol, _isPctCol);

				endLcRefresh();

				// If we would like to have a "localRateCalculation()" this would be the place to implement it
				//localRateCalculation(tmpRateData)

				setDataInitialized(true); // maybe move this to the Run class later, which does the init
			}
		}

		// Check if there is any rows that we want to interogate more, , every CM's has to implement this.
		sendDdlDetailsRequest(tmpNewSample, tmpDiffData, tmpRateData);

		// Do we want to send an Alarm somewhere, every CM's has to implement this.
		sendAlarmRequest(tmpNewSample, tmpDiffData, tmpRateData);


		if ( ! AseTune.hasGUI() )
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
		}
		else // HAS GUI
		{
			// Make them final copies to be used in the doWork/Runnable below
			final SamplingCnt fTmpNewSample = tmpNewSample;
			final SamplingCnt fTmpDiffData = tmpDiffData;
			final SamplingCnt fTmpRateData = tmpRateData;

			final CountersModel thisCm = this;

			Runnable doWork = new Runnable()
			{
				public void run()
				{
					// IMPORTANT: move datastructure.
					_prevSample = fTmpNewSample;
					_newSample  = fTmpNewSample;
					_diffData   = fTmpDiffData;
					_rateData   = fTmpRateData;
					
					beginGuiRefresh();

					// Set: Info fields
					MainFrame.setStatus(MainFrame.ST_STATUS2_FIELD, "GUI refresh of '"+_displayName+"'");

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
							_logger.debug(getName()+":------doFireTableStructureChanged------");
							fireTableStructureChanged();
							
							// Hmm do I need to do this here...
							//getTabPanel().adjustTableColumnWidth();
						}
						else if (firstTimeSample && isDataInitialized())
						{
							firstTimeSample = false;

							//System.out.println(getName()+":-fireTable-STRUCTURE-CHANGED-");
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
					MainFrame.setStatus(MainFrame.ST_STATUS2_FIELD, "");

					endGuiRefresh();

					// send DDL Request info based on the sorted JTable
					if (getTabPanel() != null)
					{
						getTabPanel().ddlRequestInfoSave();
					}

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

//		if (tabPanel != null)
//		{
//			// Update dates on panel
//			tabPanel.setTimeInfo(getCounterClearTime(), getSampleTime(), getSampleInterval());
//		}
		
		return (tmpNewSample != null) ? tmpNewSample.getRowCount() : -1;
	}


	/**
	 * Request DDL information for first 10 rows
	 * 
	 * @param absData
	 * @param diffData
	 * @param rateData
	 */
	public void sendDdlDetailsRequest(SamplingCnt absData, SamplingCnt diffData, SamplingCnt rateData)
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
		int rows = Math.min(maxNumOfDdlsToPersist, absData.getRowCount());
		for (int r=0; r<rows; r++)
		{
			Object DBName_obj     = absData.getValueAt(r, DBName_pos);
			Object ObjectName_obj = absData.getValueAt(r, ObjectName_pos);

			if (DBName_obj instanceof String && ObjectName_obj instanceof String)
				pch.addDdl((String)DBName_obj, (String)ObjectName_obj, getName()+".abs, row="+r);
		}

		// From here on we need diffData to continue
		if (diffData == null)
			return;

		// No need to continue iff all rows has already been added :)
		rows = Math.min(maxNumOfDdlsToPersist, diffData.getRowCount());
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
				_logger.error("sendDdlDetailsRequest() sortDescOnColumns='"+StringUtil.toCommaStr(sortDescOnColumns)+"', but column '"+column+"' can't be found in diffValues, trying with next column.");
				continue;
			}

			//FIXME: the below can be donne more efficient
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
			
	} // end: sendDdlDetailsRequest

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
	 * Any CM that wants to send Alarm Requests somewhere should implement this
	 * 
	 * @param absData
	 * @param diffData
	 * @param rateData
	 */
	public void sendAlarmRequest(SamplingCnt absData, SamplingCnt diffData, SamplingCnt rateData)
	{
		// empty implementation, any subclass can implement it.
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
			_dataSource = DATA_RATE;

		// Clear dates on panel
		if (tabPanel != null)
		{
			tabPanel.reset();
		}

//		_refreshCounter = 0;

//		System.out.println(_name+":------doFireTableStructureChanged------");
		fireTableStructureChanged();
	}

	/**
	 * Reset statistical counters that will be sent to www.asetune.com<br>
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
	public void setDataSource(int dataSource)
	{
		if (dataSource != DATA_ABS && dataSource != DATA_DIFF && dataSource != DATA_RATE)
			throw new RuntimeException("Unknown dataView was specified. you specified dataView="+dataSource+". known values: DATA_ABS="+DATA_ABS+", DATA_DIFF="+DATA_DIFF+", DATA_RATE="+DATA_RATE+".");
		_dataSource = dataSource;

		// Update GUI's that are litening.
		//fireTableDataChanged(); // does not do resort
		//fireTableStructureChanged(); // does resort, but: slow and makes GUI "hopp" (columns resize)
		if (getTabPanel() != null)
			getTabPanel().sortDatatable();
	}
	
	protected CounterTableModel getCounterDataAbs()
	{
		return _newSample;
	}
	protected CounterTableModel getCounterDataDiff()
	{
		return _diffData;
	}
	protected CounterTableModel getCounterDataRate()
	{
		return _rateData;
	}

	private CounterTableModel getCounterData()
	{
		return getCounterData(_dataSource);
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
		if (_diffData == null)
			return null;
		return _diffData.samplingTime;
	}

	public int getLastSampleInterval()
	{
		if (_diffData != null)
			return (int) _diffData.interval;

		return 0;
	}
	
	// Return number of rows in the diff table
	public synchronized int size()
	{
		return (_diffData == null) ? 0 : _diffData.getRowCount();
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
			_logger.info("getValue: Cant find the column '" + colname + "'.");
			return null;
		}
		if (data.getRowCount() <= rowId)
			return null;

		return data.getValueAt(rowId, idCol);
	}

	/**
	 *  Return the value of a cell by ROWID (rowId, colId)
	 *  rowId starts at 0
	 *  colId starts at 
	 *  NOTE: note tested (2007-07-13)
	 */
	// Return the value of a cell by keyVal, (keyVal, ColumnName)
	private synchronized Double getValue(int whatData, String pkStr, String colname)
	{
		CounterTableModel data = null;

		if      (whatData == DATA_ABS)  data = getCounterDataAbs();
		else if (whatData == DATA_DIFF) data = getCounterDataDiff();
		else if (whatData == DATA_RATE) data = getCounterDataRate();
		else
			throw new RuntimeException("Only ABS, DIFF, or RATE data is available.");

		if (data == null)
		{
			_logger.debug(getName()+"getValue(whatData="+getWhatDataTranslationStr(whatData)+", pkStr='"+pkStr+"', colname='"+colname+"'): data==null; return null");
			return null;
		}

		// Get the rowId, if not found, return null
		int rowId = data.getRowNumberForPkValue(pkStr);
		if (rowId < 0)
		{
			_logger.debug(getName()+"getValue(whatData="+getWhatDataTranslationStr(whatData)+", pkStr='"+pkStr+"', colname='"+colname+"'): rowId="+rowId+": rowId < 0; return null");
			return null;
		}

		// Got object for the RowID and column name
		Object o = getValue(whatData, rowId, colname);
		if (o == null)
		{
			_logger.debug(getName()+"getValue(whatData="+getWhatDataTranslationStr(whatData)+", pkStr='"+pkStr+"', colname='"+colname+"'): rowId="+rowId+": o==null; return null");
			return null;
		}

		if (o instanceof Double)
			return (Double) o;
		else
			return new Double(o.toString());
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
			_logger.info("getRowIdsWhere: Cant find the column '" + colname + "'.");
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

	// Return the MAX of the values of a column (ColumnName)
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

//		int idCol = data.getColNames().indexOf(colname);
		int idCol = data.findColumn(colname);
		if (idCol == -1)
		{
			_logger.info("getMaxValue: Cant find the column '" + colname + "'.");
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

			Object o = data.getValueAt(rowId, idCol);
			if (o == null)
				continue;

			if (o instanceof Number)
			{
				_logger.debug("Colname='" + colname + "', Number: " + ((Number) o).doubleValue());
				result = ((Number) o).doubleValue();
			}
			else
			{
				_logger.debug("Colname='" + colname + "', toString(): " + o.toString());
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

//		int idCol = data.getColNames().indexOf(colname);
		int idCol = data.findColumn(colname);
		if (idCol == -1)
		{
			_logger.info("getMinValue: Cant find the column '" + colname + "'.");
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

			Object o = data.getValueAt(rowId, idCol);
			if (o == null)
				continue;

			if (o instanceof Number)
			{
				_logger.debug("Colname='" + colname + "', Number: " + ((Number) o).doubleValue());
				result = ((Number) o).doubleValue();
			}
			else
			{
				_logger.debug("Colname='" + colname + "', toString(): " + o.toString());
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

//		int idCol = data.getColNames().indexOf(colname);
		int idCol = data.findColumn(colname);
		if (idCol == -1)
		{
			_logger.info("getSumValuePCT: Cant find the column '" + colname + "'.");
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

			Object o = data.getValueAt(rowId, idCol);
			if (o == null)
				continue;

			if (o instanceof Long)
			{
				_logger.debug("Colname='" + colname + "', Long: " + ((Long) o).longValue());
				result += ((Long) o).longValue();
			}
			else if (o instanceof Double)
			{
				_logger.debug("Colname='" + colname + "', Double: " + ((Double) o).doubleValue());
				result += ((Double) o).doubleValue();
			}
			else
			{
				_logger.debug("Colname='" + colname + "', toString(): " + o.toString());
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

//		int idCol = data.getColNames().indexOf(colname);
		int idCol = data.findColumn(colname);
		if (idCol == -1)
		{
			_logger.info("getSumValuePCT: Cant find the column '" + colname + "'.");
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

			Object o = data.getValueAt(rowId, idCol);
			if (o == null)
				continue;

			if (o instanceof Number)
			{
				if ( ((Number)o).doubleValue() > 0.0 )
					counter++;
			}
			else
			{
				_logger.warn("NOT A INSTANCE OF NUMBER: Colname='" + colname + "', toString(): " + o.toString());
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
	public String getAbsString        (int    rowId, String colname) { Object o = getValue     (DATA_ABS, rowId, colname); return (o==null)?"":o.toString(); }
	public Object getAbsValue         (int    rowId, String colname) { return getValue         (DATA_ABS, rowId, colname); }
	public Double getAbsValue         (String pkStr, String colname) { return getValue         (DATA_ABS, pkStr, colname); }
	public Double getAbsValueAsDouble (int    rowId, String colname) { return getValueAsDouble (DATA_ABS, rowId, colname); }
	public Double getAbsValueMax      (String colname)               { return getMaxValue      (DATA_ABS, null,  colname); }
	public Double getAbsValueMin      (String colname)               { return getMinValue      (DATA_ABS, null,  colname); }
	public Double getAbsValueAvg      (String colname)               { return getAvgValue      (DATA_ABS, null,  colname); }
	public Double getAbsValueAvgGtZero(String colname)               { return getAvgValueGtZero(DATA_ABS, null,  colname); }
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
	public String getDiffString        (int    rowId, String colname) { Object o = getValue     (DATA_DIFF, rowId, colname); return (o==null)?"":o.toString(); }
	public Object getDiffValue         (int    rowId, String colname) { return getValue         (DATA_DIFF, rowId, colname); }
	public Double getDiffValue         (String pkStr, String colname) { return getValue         (DATA_DIFF, pkStr, colname); }
	public Double getDiffValueAsDouble (int    rowId, String colname) { return getValueAsDouble (DATA_DIFF, rowId, colname); }
	public Double getDiffValueMax      (String colname)               { return getMaxValue      (DATA_DIFF, null,  colname); }
	public Double getDiffValueMin      (String colname)               { return getMinValue      (DATA_DIFF, null,  colname); }
	public Double getDiffValueAvg      (String colname)               { return getAvgValue      (DATA_DIFF, null,  colname); }
	public Double getDiffValueAvgGtZero(String colname)               { return getAvgValueGtZero(DATA_DIFF, null,  colname); }
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
	public String getRateString        (int    rowId, String colname) { Object o = getValue     (DATA_RATE, rowId, colname); return (o==null)?"":o.toString(); }
	public Object getRateValue         (int    rowId, String colname) { return getValue         (DATA_RATE, rowId, colname); }
	public Double getRateValue         (String pkStr, String colname) { return getValue         (DATA_RATE, pkStr, colname); }
	public Double getRateValueAsDouble (int    rowId, String colname) { return getValueAsDouble (DATA_RATE, rowId, colname); }
	public Double getRateValueMax      (String colname)               { return getMaxValue      (DATA_RATE, null,  colname); }
	public Double getRateValueMin      (String colname)               { return getMinValue      (DATA_RATE, null,  colname); }
	public Double getRateValueAvg      (String colname)               { return getAvgValue      (DATA_RATE, null,  colname); }
	public Double getRateValueAvgGtZero(String colname)               { return getAvgValueGtZero(DATA_RATE, null,  colname); }
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
	public void setResultSetMetaData(ResultSetMetaData rsmd)
	{
		_rsmd = rsmd;		
	}
	public ResultSetMetaData getResultSetMetaData()
	{
		return _rsmd;
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
			tempProps.setProperty(base + PROP_filterAllZeroDiffCounters,  isFilterAllZero());
			tempProps.setProperty(base + PROP_sampleDataIsPaused,         isDataPollingPaused());
			tempProps.setProperty(base + PROP_sampleDataInBackground,     isBackgroundDataPollingEnabled());
			tempProps.setProperty(base + PROP_negativeDiffCountersToZero, isNegativeDiffCountersToZero());
			tempProps.setProperty(base + PROP_persistCounters,            isPersistCountersEnabled());
			tempProps.setProperty(base + PROP_persistCounters_abs,        isPersistCountersAbsEnabled());
			tempProps.setProperty(base + PROP_persistCounters_diff,       isPersistCountersDiffEnabled());
			tempProps.setProperty(base + PROP_persistCounters_rate,       isPersistCountersRateEnabled());

			tempProps.setProperty(base + PROP_postponeTime,               getPostponeTime());
			tempProps.setProperty(base + PROP_queryTimeout,               getQueryTimeout());

			tempProps.save();
		}
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
			setQueryTimeout(confProps.getIntProperty(PROP_queryTimeout, getQueryTimeout()));
			setQueryTimeout(confProps.getIntProperty(base + PROP_queryTimeout, getQueryTimeout()));
		}

		if (tempProps != null)
		{
			_inLoadProps = true;

			setQueryTimeout(                 tempProps.getIntProperty(base + PROP_queryTimeout, getQueryTimeout()) );

			setFilterAllZero(                tempProps.getBooleanProperty(base + PROP_filterAllZeroDiffCounters,  isFilterAllZero())                ,false);
			setPauseDataPolling(             tempProps.getBooleanProperty(base + PROP_sampleDataIsPaused,         isDataPollingPaused())            ,false);
			setBackgroundDataPollingEnabled( tempProps.getBooleanProperty(base + PROP_sampleDataInBackground,     isBackgroundDataPollingEnabled()) ,false);
			setNegativeDiffCountersToZero(   tempProps.getBooleanProperty(base + PROP_negativeDiffCountersToZero, isNegativeDiffCountersToZero())   ,false);
			setPersistCounters(              tempProps.getBooleanProperty(base + PROP_persistCounters,            isPersistCountersEnabled())       ,false);
			setPersistCountersAbs(           tempProps.getBooleanProperty(base + PROP_persistCounters_abs,        isPersistCountersAbsEnabled())    ,false);
			setPersistCountersDiff(          tempProps.getBooleanProperty(base + PROP_persistCounters_diff,       isPersistCountersDiffEnabled())   ,false);
			setPersistCountersRate(          tempProps.getBooleanProperty(base + PROP_persistCounters_rate,       isPersistCountersRateEnabled())   ,false);

			setPostponeTime(                 tempProps.getIntProperty    (base + PROP_postponeTime,               getPostponeTime())                ,false);

			_inLoadProps = false;
		}
	}

	public static final String PROP_filterAllZeroDiffCounters  = "filterAllZeroDiffCounters";
	public static final String PROP_sampleDataIsPaused         = "sampleDataIsPaused";
	public static final String PROP_sampleDataInBackground     = "sampleDataInBackground";
	public static final String PROP_negativeDiffCountersToZero = "negativeDiffCountersToZero";
	public static final String PROP_persistCounters            = "persistCounters";
	public static final String PROP_persistCounters_abs        = "persistCounters.abs";
	public static final String PROP_persistCounters_diff       = "persistCounters.diff";
	public static final String PROP_persistCounters_rate       = "persistCounters.rate";
	public static final String PROP_postponeTime               = "postponeTime";
	public static final String PROP_queryTimeout               = "queryTimeout";


	
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
		public void actionPerformed(ActionEvent actionevent)
		{
			if (tabPanel != null)
			{
				// maybe use property change listeners instead: firePropChanged("status", "refreshing");
				tabPanel.setWatermarkText("Refreshing data...");
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

		StoredProcCheck(String dbname, String procName, Date procDateThreshold, 
				Class<?> scriptLocation, String scriptName, String needsRoleToRecreate)
		{
			_dbname              = dbname;
			_procName            = procName;
			_procDateThreshold   = procDateThreshold;
			_scriptLocation      = scriptLocation;
			_scriptName          = scriptName;
			_needsRoleToRecreate = needsRoleToRecreate;
		}
	}
}