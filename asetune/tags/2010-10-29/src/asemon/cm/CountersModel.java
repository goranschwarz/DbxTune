/**
*/

package asemon.cm;
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
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;

import org.apache.log4j.Logger;

import asemon.Asemon;
import asemon.GetCounters;
import asemon.MonWaitEventIdDictionary;
import asemon.TrendGraphDataPoint;
import asemon.gui.MainFrame;
import asemon.gui.SummaryPanel;
import asemon.gui.TabularCntrPanel;
import asemon.gui.TrendGraph;
import asemon.gui.swing.GTabbedPane;
import asemon.pcs.PersistentCounterHandler;
import asemon.utils.AseConnectionUtils;
import asemon.utils.AseSqlScript;
import asemon.utils.Configuration;
import asemon.utils.StringUtil;
import asemon.utils.TimeUtils;

import com.sybase.jdbcx.SybConnection;
import com.sybase.jdbcx.SybMessageHandler;

public class CountersModel 
extends AbstractTableModel
implements Cloneable 
{
    private static final long serialVersionUID = -7486772146682031469L;

	/** Log4j logging. */
	private static Logger      _logger            = Logger.getLogger(CountersModel.class);

//	public  static final int   FILTER_EQ          = 0;  // EQUAL
//	public  static final int   FILTER_NE          = 1;  // NOT_EQUAL
//	public  static final int   FILTER_GT          = 2;  // GREATER THAN
//	public  static final int   FILTER_LT          = 3;  // LESS THAN
//	private static final int   FILTER__MAX_ID     = FILTER_LT;

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

	// private int typeModel;
	private boolean            _isInitialized     = false;
	private boolean            _runtimeInitialized= false;
	private int                _serverVersion     = 0;
	private boolean            _isClusterEnabled  = false;
	private List               _activeRoleList    = null;
	private String             _sqlInit           = null;
	private String             _sqlClose          = null; // Not used yet
	private String             _sqlRequest        = null;
	private String             _sqlWhere          = "";
	private TabularCntrPanel   tabPanel           = null;
	private List               _pkCols            = null;
//	private int	               idKey1;
//	private int	               idKey2;
//	private int	               idKey3;

	private String[]           _monTablesInQuery     = null;
	
	private List               _dependsOnCm          = null;
	private String[]           _dependsOnRole        = null;
	private String[]           _dependsOnConfig      = null;
	private int                _dependsOnVersion     = 0;
	private int                _dependsOnCeVersion   = 0;
	private List               _dependsOnStoredProc  = null; // containes: StoredProcCheck objects
	
	/** If we should refresh this CM in a different manner than the default refresh rate. 0=useDefault, >0=number of seconds between samples */
	private int                _postponeTime         = 0;
	/** every time the CM is refreshed set this to System.currentTimeMillis() */
	private long               _lastLocalRefreshTime = 0;

	private ResultSetMetaData  _rsmd;

	// Structure for storing list of columns to compute difference between two samples
	private String[]           _diffColumns = null;
	private boolean[]          _isDiffCol   = null;
//	private Vector	           bitmapColsCalcDiff	= null;

	// Structure for storing list of columns to display rate as PCT rather than pure rate
	private String[]           _pctColumns  = null;
	private boolean[]          _isPctCol    = null;
//	private Vector	           bitmapColsCalcPCT	= null;

	// In the filter (check for nonZeroValues) disregards these column(s)
	private String[]           _diffDissColumns = null;
	private boolean[]          _isDiffDissCol   = null;
	
//	private String             filterColName;
//	private int                filterColId;
//	private int                filterOp;
//
//	private Object             filterValue;
//

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
	private Map                _trendGraphs = new LinkedHashMap();

	/** A collection of Graphs actual data connected to the CM 
	 *  The graph data should still be calculated even in a non-gui environment
	 *  so it can be persisted, This makes it <b>much</b> easier to redraw a
	 *  graph when the off-line data is viewed in a later stage */
	private Map                _trendGraphsData = new HashMap();

	private boolean            _filterAllZeroDiffCounters = false;
	private boolean            _sampleDataIsPaused        = false;
	private boolean            _sampleDataInBackground    = false;
	private boolean            _persistCounters           = false;
	private boolean            _persistCountersAbs        = true;
	private boolean            _persistCountersDiff       = true;
	private boolean            _persistCountersRate       = true;

	// 
	private int clearCmLevel;

	// Use quoted identifier
//	private static boolean useQuotedIdentifier = true;
//	private static String qic = "\"";

//  class MyModel extends DefaultTableModel {
//    public MyModel() {
//      super();
//    }
//      public MyModel(int row, int col) {super(row, col);}
//      public boolean isCellEditable(int row, int col) {return false;}
//      public Class getColumnClass(int columnIndex){
//        if (chosenData==null) return Object.class;
//        if (chosenData.rows.size()==0) return Object.class;
//        if ( ((Vector)chosenData.rows.get(0)).get(columnIndex)==null ) return Object.class;
//        return ((Vector)chosenData.rows.get(0)).get(columnIndex).getClass();
//      }
//  }
//  private MyModel TM=null;
//	protected CountersTableModel TM=null;

	protected SamplingCnt oldSample=null;      // Contains old raw data
	protected SamplingCnt newSample=null;      // Contains new raw data
	protected SamplingCnt diffData=null;       // diff between newSample and oldSample data (not filtered)
	protected SamplingCnt rateData=null;       // diffData / sampleInterval
//	protected SamplingCnt chosenData=null;     // diff or newSample, depends on user choice (Absolute, interval)
//	protected SamplingCnt dataTM=null;           // data=diff but can be filtered (does not countain all data)
	
//jre6	protected TableSorter sorter;
//	protected TableSorter sorter;
	
	private int _dataSource = DATA_RATE;

	private boolean dataInitialized=false;
	private boolean firstTimeSample=true;

	protected boolean swingRefreshOK;
//	public String displayMode;
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


	//	private int selectedModelRow=-1; // Row currently selected in the Model. -1 if none


//	public static String filterOpToString(int op)
//	{
//		switch (op)
//        {
//        case FILTER_EQ:  return "=";
//        case FILTER_NE:  return "!=";
//        case FILTER_GT:  return ">";
//        case FILTER_LT:  return "<";
//        default:
//        	return "Unknown filter id '"+op+"'.";
//        }
//	}

	/*---------------------------------------------------
	** BEGIN: constructors
	**---------------------------------------------------
	*/
	public CountersModel()
	{
	}

	/**
	 * 
	 */
	public CountersModel
	(
			String   name,             // Name of the Counter Model
			String   sql,              // SQL Used to grab a sample from the counter data
			List     pkList,           // A list of columns that will be used during diff calculations to lookup values in previous samples
			String[] diffColumns,      // Columns to do diff calculations on
			String[] pctColumns,       // Columns that is to be considered as Percent calculated columns, (they still need to be apart of diffColumns)
			String[] monTables,        // What monitor tables are accessed in this query, used for TOOLTIP lookups
			String[] dependsOnRole,    // Needs following role(s)
			String[] dependsOnConfig,  // Check that these configurations are above 0
			int      dependsOnVersion, // What version of ASE do we need to sample for this CounterModel
			int      dependsOnCeVersion, // What version of ASE-CE do we need to sample for this CounterModel
			boolean  negativeDiffCountersToZero, // if diff calculations is negative, reset the counter to zero.
			boolean  systemCm
	)
	{
		// Initialize a model for use with a JTable
		_name     = name;
		_systemCm = systemCm;
		if ( ! isValidCmName(_name, _systemCm) )
		{
			String err = checkForValidCmName(_name);
			throw new RuntimeException("Problems when creating cm '"+_name+"'. "+err);
			//throw new RuntimeException("Max length of a CountersModel name is 20 chars. the name '"+_name+"' is of length "+_name.length()+".");
		}
		_sqlRequest         = sql;
		_sqlWhere           = "";
		_pkCols             = pkList;
		_diffColumns        = diffColumns;
		_pctColumns         = pctColumns;
		_monTablesInQuery   = monTables;
		_dependsOnRole      = dependsOnRole;
		_dependsOnConfig    = dependsOnConfig;
		_dependsOnVersion   = dependsOnVersion;
		_dependsOnCeVersion = dependsOnCeVersion;
		_negativeDiffCountersToZero = negativeDiffCountersToZero;

		_sybMessageHandler  = createSybMessageHandler();
		
		//		filterColId       = -1;
		oldSample         = null; // Contains old raw data
		newSample         = null; // Contains new raw data
		diffData          = null; // diff between newSample and oldSample data (not filtered)
//		chosenData        = null; // diff or newSample, depends on user choice (Absolute, interval)
//		dataTM            = null; // dataTM=diff but can be filtered (does not countain all data)
//		displayMode       = "RATE_PER_SEC";
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
		setSwingRefreshOK(true);
	}

	/*---------------------------------------------------
	** END: constructors
	**---------------------------------------------------
	*/

	public CountersModel copyForStorage()
	{
//		CountersModel c = new CountersModel();
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

		c._isInitialized              = this._isInitialized;
		c._runtimeInitialized         = this._runtimeInitialized;
		c._serverVersion              = this._serverVersion;
		c._isClusterEnabled           = this._isClusterEnabled;
		c._activeRoleList             = this._activeRoleList;
		c._sqlInit                    = this._sqlInit;
		c._sqlClose                   = this._sqlClose;
		c._sqlRequest                 = this._sqlRequest;
		c._sqlWhere                   = this._sqlWhere;
		c.tabPanel                    = this.tabPanel;
		c._pkCols                     = this._pkCols;

		c._monTablesInQuery           = this._monTablesInQuery;

		c._dependsOnCm                = this._dependsOnCm;
		c._dependsOnRole              = this._dependsOnRole;
		c._dependsOnConfig            = this._dependsOnConfig;
		c._dependsOnVersion           = this._dependsOnVersion;
		c._dependsOnCeVersion         = this._dependsOnCeVersion;
		c._dependsOnStoredProc        = this._dependsOnStoredProc;

		c._postponeTime               = this._postponeTime;
		c._lastLocalRefreshTime       = this._lastLocalRefreshTime;

		c._rsmd                       = this._rsmd;

		c._diffColumns                = this._diffColumns;
		c._isDiffCol                  = this._isDiffCol;

		c._pctColumns                 = this._pctColumns;
		c._isPctCol                   = this._isPctCol;

		c._diffDissColumns            = this._diffDissColumns;
		c._isDiffDissCol              = this._isDiffDissCol;
		
		c._hasValidSampleData         = this._hasValidSampleData;
		
		c._isActive                   = this._isActive;

		c._trendGraphs                = null;
		// needs to be copied/cloned, since the TrendGraphDataPoint is reused.
		c._trendGraphsData = new HashMap();
		for (Iterator it = this._trendGraphsData.keySet().iterator(); it.hasNext();)
		{
			String              key = (String) it.next();
			TrendGraphDataPoint val = (TrendGraphDataPoint) this._trendGraphsData.get(key);

			c._trendGraphsData.put(key, val.clone());
		}

		c._filterAllZeroDiffCounters  = this._filterAllZeroDiffCounters;
		c._sampleDataIsPaused          = this._sampleDataIsPaused;
		c._sampleDataInBackground     = this._sampleDataInBackground;
		c._persistCounters            = this._persistCounters;
		c._persistCountersAbs         = this._persistCountersAbs;
		c._persistCountersDiff        = this._persistCountersDiff;
		c._persistCountersRate        = this._persistCountersRate;


		c.oldSample                   = this.oldSample;
		c.newSample                   = this.newSample;
		c.diffData                    = this.diffData;
		c.rateData                    = this.rateData;
		
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
//	public Class getColumnClass(int columnIndex)
//	{
//		if (!isDataInitialized())   return null;
//
//		SamplingCnt data = getCounterData();
//		if (data == null) return null;
//		return data.getColumnClass(columnIndex);
//	}

	public int getColumnCount()
	{
//		if (!isDataInitialized())   return 0;
//		if (chosenData == null) return 0;
//	    return chosenData.getColumnCount();
		SamplingCnt data = getCounterData();
		int c = 0;
		if (isDataInitialized() && data != null)
			c = data.getColumnCount();
//		System.out.println(_name+":getColumnCount() <- "+c);
		return c;
    }

	public String getColumnName(int col)
	{
//		if (!isDataInitialized())   return null;
//		if (chosenData == null) return null;
//		return chosenData.getColumnName(col);
		SamplingCnt data = getCounterData();
		String s = null;
		if (isDataInitialized() && data != null)
			s = data.getColumnName(col);
//		System.out.println(_name+":getColumnName(col="+col+") <- '"+s+"'.");
		return s;
	}

	public int getRowCount()
	{
//		if (!isDataInitialized())   return 0;
//		if (chosenData == null) return 0;
//		return chosenData.getRowCount();
		int c = 0;
		SamplingCnt data = getCounterData();
		if (isDataInitialized() && data != null)
			c = data.getRowCount();
//		System.out.println(_name+":getRowCount() <- "+c);
		return c;
    }

	public Object getValueAt(int row, int col)
	{
		if (!isDataInitialized())   return null;

		SamplingCnt data = getCounterData();
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
//		if (!isDataInitialized())   return null;
//		if (chosenData == null) return null;
//		return chosenData.getColumnName(col);
		SamplingCnt data = getCounterData();
		int pos = -1;
		if (isDataInitialized() && data != null)
			pos = data.getColId(colName);
//		System.out.println(_name+":getColumnPos(colName="+colName+") <- "+pos);
		return pos;
	}

	/*---------------------------------------------------
	** END: implementing TableModel or overriding AbstractTableModel
	**---------------------------------------------------
	*/

	/** will most likely be used to set off-line counter data */
	public void setValueAt(int type, Object value, int row, int col)
	{
		SamplingCnt data = null;
		if      (type == DATA_ABS)  { if (newSample == null) {newSample = new SamplingCnt("offline-abs",  false); data = newSample;} else data = newSample;}
		else if (type == DATA_DIFF) { if (diffData  == null) {diffData  = new SamplingCnt("offline-diff", false); data = diffData;}  else data = diffData;}
		else if (type == DATA_RATE) { if (rateData  == null) {rateData  = new SamplingCnt("offline-rate", false); data = rateData;}  else data = rateData;}
		else
			throw new RuntimeException("Only ABS, DIFF, or RATE data is available.");

		data.setValueAt(value, row, col);

//		fireTableCellUpdated(row, col);
	}

	/** will most likely be used to set off-line counter data */
	public void setColumnNames(int type, List cols)
	{
		SamplingCnt data = null;
		if      (type == DATA_ABS)  { if (newSample == null) {newSample = new SamplingCnt("offline-abs",  false); data = newSample;} else data = newSample;}
		else if (type == DATA_DIFF) { if (diffData  == null) {diffData  = new SamplingCnt("offline-diff", false); data = diffData;}  else data = diffData;}
		else if (type == DATA_RATE) { if (rateData  == null) {rateData  = new SamplingCnt("offline-rate", false); data = rateData;}  else data = rateData;}
		else
			throw new RuntimeException("Only ABS, DIFF, or RATE data is available.");

		data.setColumnNames(cols);
		initColumnStuff(data);

//		fireTableStructureChanged();
//		if (tabPanel != null)
//			tabPanel.adjustTableColumnWidth();
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
		if (Asemon.hasGUI())
		{
			if ( ! isActive() )
				return false;

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
				refresh = Asemon.getCounterCollector().isMonConnected(true, true);

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
		// Dif calculations can still be done if there are no PK... then it's only 1 row...
		//if ( _pkCols == null || (_pkCols != null && _pkCols.size() == 0) )
		//		return false;

		if ( _diffColumns == null || (_diffColumns != null && _diffColumns.length == 0) )
				return false;
		
		return true;
	}

	public void setNegativeDiffCountersToZero(boolean b)
	{
		// No need to continue if we are not changing it
		if (isNegativeDiffCountersToZero() == b)
			return;

		_negativeDiffCountersToZero = b;
		saveProps();

		if (getTabPanel() != null)
			getTabPanel().setOptionNegativeDiffCntToZero(b);
	}
	public boolean isNegativeDiffCountersToZero()
	{
		return _negativeDiffCountersToZero;
	}

	public void setFilterAllZero(boolean b)
	{
		// No need to continue if we are not changing it
		if (isFilterAllZero() == b)
			return;

		_filterAllZeroDiffCounters = b;
		saveProps();
	}
	public boolean isFilterAllZero()
	{
		return _filterAllZeroDiffCounters;
	}

	public void setPauseDataPolling(boolean b)
	{
		// No need to continue if we are not changing it
		if (isDataPollingPaused() == b)
			return;

		_sampleDataIsPaused = b;
		saveProps();

		if (getTabPanel() != null)
			getTabPanel().setOptionPauseDataPolling(b);
	}

	public boolean isDataPollingPaused()
	{
		return _sampleDataIsPaused;
	}

	
	public void setBackgroundDataPollingEnabled(boolean b)
	{
		// No need to continue if we are not changing it
		if (isBackgroundDataPollingEnabled() == b)
			return;

		_sampleDataInBackground = b;
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

	public void setPersistCounters(boolean b)
	{
		// No need to continue if we are not changing it
		if (isPersistCountersEnabled() == b)
			return;

		_persistCounters = b;
		//_logger.error("PERSIST setPersistData() method, NOT FULLY IMPLEMENTED.", new Exception("PERSIST setPersistData() method, NOT FULLY IMPLEMENTED.") );
		saveProps();

		if (getTabPanel() != null)
			getTabPanel().setOptionPersistCounters(b);
	}
	public boolean isPersistCountersEnabled()
	{
		return _persistCounters;
	}
	
	public void setPersistCountersAbs(boolean b)
	{
		// No need to continue if we are not changing it
		if (isPersistCountersAbsEnabled() == b)
			return;

		_persistCountersAbs = b;
		//_logger.error("PERSIST setPersistData() method, NOT FULLY IMPLEMENTED.", new Exception("PERSIST setPersistData() method, NOT FULLY IMPLEMENTED.") );
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
	
	public void setPersistCountersDiff(boolean b)
	{
		// No need to continue if we are not changing it
		if (isPersistCountersDiffEnabled() == b)
			return;

		_persistCountersDiff = b;
		//_logger.error("PERSIST setPersistData() method, NOT FULLY IMPLEMENTED.", new Exception("PERSIST setPersistData() method, NOT FULLY IMPLEMENTED.") );
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
	
	public void setPersistCountersRate(boolean b)
	{
		// No need to continue if we are not changing it
		if (isPersistCountersRateEnabled() == b)
			return;

		_persistCountersRate = b;
		//_logger.error("PERSIST setPersistData() method, NOT FULLY IMPLEMENTED.", new Exception("PERSIST setPersistData() method, NOT FULLY IMPLEMENTED.") );
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
	public String getDescription()
	{
		return _description;
	}
	public void setDisplayName(String str)
	{
		_displayName = str;
	}
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

	public List getPk()
	{
		return _pkCols;
	}

	public Vector getDataVector(int whatData)
	{
		SamplingCnt data = null;

		if      (whatData == DATA_ABS)  data = newSample;
		else if (whatData == DATA_DIFF) data = diffData;
		else if (whatData == DATA_RATE) data = rateData;
		else
			throw new RuntimeException("Only ABS, DIFF, or RATE data is available.");

		if (data == null)
			return null;

		return data.getDataVector();
	}

	public synchronized Vector getColNames(int whatData)
	{
		SamplingCnt data = null;

		if      (whatData == DATA_ABS)  data = newSample;
		else if (whatData == DATA_DIFF) data = diffData;
		else if (whatData == DATA_RATE) data = rateData;
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
	 * Used by the TabularCntrPanel.JTable to get tool tip on a cell level.
	 * Override it to set specific tooltip... 
	 * @param e
	 * @param colName
	 * @param modelRow
	 * @param modelCol
	 * @return
	 */
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

		// If we are CONNECTED and we have a USER DEFINED TOOLTIP for this columns
		if (cellValue != null)
		{
			String sql = MainFrame.getUserDefinedToolTip(getName(), colName);

			if ( sql != null && ! Asemon.getCounterCollector().isMonConnected() )
				return "<html>" +
				       "No runtime tool tip available for '"+colName+"'. <br>" +
				       "Not connected to the monitored server.<br>" +
				       "</html>";

			if (sql != null)
			{
				try
				{
					Connection conn = Asemon.getCounterCollector().getMonConnection();

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
		for (Iterator it = _trendGraphs.keySet().iterator(); it.hasNext();)
		{
			String graphName = (String)it.next();
			TrendGraph tg = getTrendGraph(graphName);
			if (tg.isGraphEnabled())
				active = true;
		}
		
		// FIXME: move this somewhere else... 
		if (tabPanel != null)
		{
			setBackgroundDataPollingEnabled( active );
			tabPanel.setEnableBgPollingCheckbox( active );
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

		for (Iterator it = _trendGraphs.keySet().iterator(); it.hasNext();)
		{
			String graphName = (String)it.next();
			TrendGraph tg = getTrendGraph(graphName);
			updateGraph(tg);
		}
	}

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
	
	public Map getTrendGraphs()
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

		for (Iterator it = _trendGraphsData.keySet().iterator(); it.hasNext();)
		{
			String graphName = (String)it.next();
			TrendGraphDataPoint tgdp = getTrendGraphData(graphName);
			updateGraphData(tgdp);
			
			//System.out.println("cm='"+StringUtil.left(this.getName(),25)+"', _trendGraphData="+tgdp);
			if (_logger.isDebugEnabled())
				_logger.debug("cm='"+StringUtil.left(this.getName(),25)+"', _trendGraphData="+tgdp);
		}
	}
	
	public TrendGraphDataPoint getTrendGraphData(String name)
	{
		return (TrendGraphDataPoint) _trendGraphsData.get(name);
	}
	
	public Map getTrendGraphData()
	{
		return _trendGraphsData;
	}
	
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
		for (Iterator it = _trendGraphsData.keySet().iterator(); it.hasNext();)
		{
			String graphName = (String)it.next();
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
	public void setActiveRoles(List activeRoleList)
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
	}

	/** 
	 * This method is used to check if the CounterTable should be initialized or not.<br>
	 * for example "deadlock pipe" may not be active...<br>
	 * If server version is below 15.0.2 'statement cache' info should not be 
	 * polled since those monTables doesn't exist. And the GUI Table for this should
	 * not be VISABLE... call setActive(false) in those cases.
	 */
	public void init(Connection conn)
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
		doSqlClose(Asemon.getCounterCollector().getMonConnection());
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
			if (Asemon.getCounterCollector().isMonConnected())
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
		_dependsOnConfig = dependsOnConfig;
		if (_dependsOnConfig == null)
		{
			String[] emptyArray = {};
			_dependsOnConfig = emptyArray;
		}

	}
	
	/** Check a specific configuration parameter */
	public boolean checkDependsOnConfig(Connection conn, String configNameVal)
	{
		if (configNameVal == null || (configNameVal != null && configNameVal.equals("")) )
			throw new IllegalArgumentException("checkDependsOnConfig(): configNameVal='"+configNameVal+"' must be a value.");

		String[] configNameArr = configNameVal.split("=");
		String configName  = configNameVal;
		String configValue = null;
		if (configNameArr.length >= 1) configName  = configNameArr[0];
		if (configNameArr.length >= 2) configValue = configNameArr[1];

		int configHasValue = AseConnectionUtils.getAseConfigRunValueNoEx(conn, configName);
		_logger.debug("Checking for ASE Configuration '"+configName+"', which has value '"+configHasValue+"'. Option to re-configure to value '"+configValue+"' if not set.");

		// In NO_GUI mode, we might want to auto configure monitoring...
		boolean doReconfigure = false;
		if ( ! Asemon.hasGUI() )
		{
			Configuration conf = Configuration.getInstance(Configuration.CONF);
			doReconfigure = conf.getBooleanProperty("nogui.configuration.fix", false);
		}
		//doReconfigure = true; // if you want to force when testing

		// If no config value has been specified, we can't do initialization...
		// Doing it with a dummy value of 1 or simular would be dangerous, lets use 'statement cache size'
		// as an example... then we would enable statement cache... and to a SMALL value as well = BAD-BAD-BAD
		if (configValue == null)
			doReconfigure = false;

		// Should we do RECONFIGURE
		if (configHasValue <= 0  &&  doReconfigure)
		{
			// CHECK IF WE HAVE "sa role", so we can re-configure
			boolean hasSaRole = AseConnectionUtils.hasRole(conn, AseConnectionUtils.SA_ROLE);
			if ( ! hasSaRole )
			{
				_logger.warn("Can not adjust the configuration '"+configName+"' to value '"+configValue+"'. To do that the connected user needs to have '"+AseConnectionUtils.SA_ROLE+"'.");
			}
			else
			{
				_logger.info("Trying to set configuration '"+configName+"' to value '"+configValue+"'.");

				try
				{
					AseConnectionUtils.setAseConfigValue(conn, configName, configValue);
				}
				catch (SQLException e)
				{
					_logger.error("Problems setting ASE configuration '"+configName+"' to '"+configValue+"'. Caught: "+AseConnectionUtils.sqlExceptionToString(e));
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
			_logger.warn("When trying to initialize Counters Models ("+getName()+") in ASE Version "+getServerVersion()+", I found that '"+configName+"' wasn't configured (which is done with: sp_configure '"+configName+"'), so monitoring information about '"+getDisplayName()+"' will NOT be enabled.");

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
			_logger.warn("When trying to initialize Counters Models ("+getName()+") in ASE Version "+getServerVersionStr()+", I need atleast ASE Version "+getDependsOnVersionStr()+" for that.");

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
			_logger.warn("When trying to initialize Counters Models ("+getName()+") in ASE Cluster Edition Version "+getServerVersionStr()+", I need atleast ASE Cluster Edition Version "+getDependsOnCeVersionStr()+" for that.");

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
	public void addDependsOnStoredProc(String dbname, String procName, Date procDateThreshold, Class scriptLocation, String scriptName, String needsRoleToRecreate)
	{
		if (dbname            == null) throw new IllegalArgumentException("addDependsOnStoredProc(): 'dbname' cant be null");
		if (procName          == null) throw new IllegalArgumentException("addDependsOnStoredProc(): 'procName' cant be null");
		if (procDateThreshold == null) throw new IllegalArgumentException("addDependsOnStoredProc(): 'procDateThreshold' cant be null");
		if (scriptLocation    == null) throw new IllegalArgumentException("addDependsOnStoredProc(): 'scriptLocation' cant be null");
		if (scriptName        == null) throw new IllegalArgumentException("addDependsOnStoredProc(): 'scriptName' cant be null");

		if (_dependsOnStoredProc == null)
			_dependsOnStoredProc = new LinkedList();

		StoredProcCheck spc = new StoredProcCheck(dbname, procName, procDateThreshold, scriptLocation, scriptName, needsRoleToRecreate);
		_dependsOnStoredProc.add(spc);
	}
	
	public boolean checkDependsOnStoredProc(Connection conn, String dbname, String procName, Date procDateThreshold, Class scriptLocation, String scriptName, String needsRoleToRecreate)
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
				AseSqlScript script = new AseSqlScript(conn);
				try
				{
					_logger.info("Creating procedure '"+procName+"' in '"+dbname+"'.");
					script.setMsgPrefix(scriptName+": ");
					script.execute(scriptLocation, scriptName);
					hasProc = true;
				}
				catch (SQLException e) 
				{
					_logger.error("Problem loading the script '"+scriptName+"'.", e);
				}
				script.close();
			}

			if ( ! hasProc )
			{
				String msg = "Missing stored proc '"+procName+"' in database '"+dbname+"' please create it.";
				setActive(false, msg);

				_logger.debug(getName() + ": should be HIDDEN.");
				_logger.warn("When trying to initialize Counters Models ("+getName()+") in ASE Version "+getServerVersion()+", "+msg+" (connect with a user that has '"+needsRoleToRecreate+"' or load the proc from '$ASEMON_HOME/classes' under the class '"+scriptLocation.getClass().getName()+"' you will find the script '"+scriptName+"').");

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
		for (Iterator it = _dependsOnStoredProc.iterator(); it.hasNext();) 
		{
			StoredProcCheck spc = (StoredProcCheck) it.next();

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



	//	public String getToolTip(String colName)
//	{
//		return MonTablesDictionary.getInstance().getDescription(_monTablesInQuery, colName);
//	}

	public String getName()
	{
		return _name;
	}

//	public void setSelectedViewRow(int i)
//	{
//		if (i == -1)
//		{
//			selectedModelRow = -1;
//			return;
//		}
////		selectedModelRow = sorter.modelIndex(i);
////jre6		selectedModelRow = sorter.modelIndex(i);
////FIXME: do convertion view/model
////		When a table uses a sorter, the data the users sees may be in a 
////		different order than that specified by the data model, and may not 
////		include all rows specified by the data model. The data the user  
////		actually sees is known as the view, and has its own set of coordinates. 
////		JTable provides methods that convert from model coordinates to view 
////		coordinates — convertColumnIndexToView and convertRowIndexToView — 
////		and that convert from view coordinates to model coordinates — 
////		convertColumnIndexToModel and convertRowIndexToModel.
//		
//		// System.out.println("Model rowid = " + selectedModelRow);
//		// System.out.println("View  rowid = " + sorter.viewIndex(selectedModelRow));
//	}

//	public int getSelectedViewRow()
//	{
////jre6		if (selectedModelRow != -1)
////jre6			return sorter.viewIndex(selectedModelRow);
////jre6		else
//		if (selectedModelRow != -1)
//			return sorter.viewIndex(selectedModelRow);
//		else
//			return -1;
//	}

//	public Object getSelectedRowValueForColName(String colName)
//	{
//		int colId = dataTM.getColId(colName);
//		if (colId < 0)
//			return null;
//
//		return dataTM.getValue(selectedModelRow, colId);
//	}


	/**
	 * Set filter operations.
	 * @param colName
	 * @param op
	 * @param val
	 */
//	public synchronized void setFilterOff()
//	{
//		setFilter(null, -1, null);
//	}

//	public synchronized void setFilter(String colName, int op, Object val)
//	{
//		boolean reset = false;
//		if (colName == null) reset = true;
//		if (colName != null && colName.equals("")) reset = true;
//		if (colName != null && colName.equals("<none>")) reset = true;
//
//		if (op < 0  || op > FILTER__MAX_ID)
//		{
//			op = -1;
//			reset = true;
//		}
//
//		if (val == null) reset = true;
//		if (val != null && val.equals("")) reset = true;
//
//		if (reset)
//		{
//			_logger.debug("setFilter(): RESET");
//			filterColName = null;
//			filterColId   = -1;
//			filterOp      = -1;
//			filterValue   = null;
//			return;
//		}
//		
//		filterColName = colName;
//		filterColId   = dataTM.getColId(colName);
//		filterOp      = op;
//		filterValue   = val;
//
//		_logger.debug("setFilter(): colName='"+filterColName+"', colId="+filterColId+", op='"+filterOpToString(filterOp)+"', val='"+filterValue+"'.");
//	}

	/**
	 * @param row
	 * @return true if the row should NOT be listed in the table
	 */
//	private boolean isFiltered(Vector row){ return false; }
//	private boolean isFiltered(Vector row)
//	{
//		Object cell;
//		boolean hidden = false;
//
//		if (filterColId >= 0)
//		{
//			if (filterValue == null)
//				return false;
//
//			if (filterOp == -1)
//				return false;
//
//			// Get the Object of the cell
//			cell = row.get(filterColId);
//
//			if (cell == null)
//			{
////				System.out.println("cell=NULL");
//				hidden = true;
//				return hidden;
//			}
//
//			int comp = 0;
//
//			// MAKE the compare
//			try
//			{
//				// Special for STRING in-case of EQUAL and NOT EQUAL
//				// do String.match instead.
//				if (cell instanceof String  && (filterOp == FILTER_EQ || filterOp == FILTER_NE) )
//				{
//					if ( ((String)cell).matches((String)filterValue) )
//						comp = 0;
//					else
//						comp = 1;
//				}
//				else
//				{
//					comp = ((Comparable)cell).compareTo(filterValue);
//				}
//			}
//			catch (ClassCastException e)
//			{
//				_logger.info("Warning when filtering. operation='"+filterOpToString(filterOp)+"', for tablevalue='"+cell+"':"+cell.getClass().getName()+", filterValue='"+filterValue+"':"+filterValue.getClass().getName()+".");
//				if (cell instanceof Number)
//				{
//					comp = Double.compare(
//							((Number)cell).doubleValue(),
//							((Number)filterValue).doubleValue() );
//				}
//			}
//			catch (Throwable t)
//			{
//				_logger.error("Problems when filtering. operation='"+filterOpToString(filterOp)+"', for tablevalue='"+cell+"':"+cell.getClass().getName()+", filterValue='"+filterValue+"':"+filterValue.getClass().getName()+".", t);
//				return false;
//			}
//
//			hidden = true;
//			if (filterOp == FILTER_EQ && comp == 0) hidden = false;
//			if (filterOp == FILTER_NE && comp != 0) hidden = false;
//			if (filterOp == FILTER_GT && comp >  0) hidden = false;
//			if (filterOp == FILTER_LT && comp <  0) hidden = false;
//			
//			if (_logger.isDebugEnabled())
//				_logger.debug("Filter: hidden="+hidden+", op='"+filterOpToString(filterOp)+"', for datavalue='"+cell+"':"+cell.getClass().getName()+", filterValue='"+filterValue+"':"+filterValue.getClass().getName()+".");
//		}
//
//		if (filterAllZeroDiffCounters && !hidden)
//		{
//			boolean allZero = true;
//			for (int c=0; c<row.size(); c++)
//			{
//				if (isDeltaCalculatedColumn(c))
//				{
//					Object o = row.get(c);
//					if (o instanceof Number)
//					{
//						if ( ((Number)o).doubleValue() != 0 )
//						{
//							allZero = false;
//							break;
//						}
//					}
//					else
//					{
//						if (_logger.isDebugEnabled())
//							_logger.debug("Filter: allZeroCheck, un handled class: "+o.getClass().getName());
//					}
//				}
//			}
//			if (allZero)
//			{
//				hidden = true;
//				if (_logger.isDebugEnabled())
//					_logger.debug("Filtering 'all zero counters' row.");
//			}
//		}
//
//		return hidden;
//	}

  
	public boolean showAbsolute()
	{
		return _dataSource == DATA_ABS;
	}

//	public boolean isDeltaCalculatedColumn(int index)
//	{
//		if (((Integer) bitmapColsCalcDiff.get(index)).intValue() == 1)
//			return true;
//		else
//			return false;
//	}
//
//	public boolean isPctColumn(int index)
//	{
//		if (((Integer) bitmapColsCalcPCT.get(index)).intValue() == 1)
//			return true;
//		else
//			return false;
//	}
//	public boolean isDeltaCalculatedColumn(int index)
//	{
//		return _isDiffCol[index];
//	}
//
//	public boolean isPctColumn(int index)
//	{
//		return _isPctCol[index];
//	}

	// do local calculation, this should be overridden for local calculations...
	// not as today with if statements...
	public void localCalculation()
	{
	}
				
	public boolean isConnected()
	{
		return Asemon.getCounterCollector().isMonConnected();
	}
	public boolean isOfflineConnected()
	{
		return MainFrame.isOfflineConnected();
	}

	public void endOfRefresh()
	{
		if (tabPanel != null)
		{
			tabPanel.setWatermark();
		}
	}

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

	public Exception getSampleException()            { return _sampleException; }
	public void      setSampleException(Exception e) 
	{
		_sampleException = e; 
		_logger.info(e.toString(), e);
	}

	public long      getLastLocalRefreshTime()       { return _lastLocalRefreshTime; }

	public int       getPostponeTime()               { return _postponeTime; }
	public void      setPostponeTime(int seconds) 
	{ 
		// No need to continue if we are not changing it
		if (getPostponeTime() == seconds)
			return;

		_postponeTime = seconds;
		saveProps();

		if (getTabPanel() != null)
			getTabPanel().setPostponeTime(seconds);
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
			Vector colNames = cnt.getColNames();
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
			Vector colNames = cnt.getColNames();
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
			Vector colNames = cnt.getColNames();
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
		long timeLimit = 1000;
		if (timeDiff > timeLimit)
			throw new Exception("Depends on '"+cm.getName()+"', which last refresh time was '"+timeDiff+"' ms ago. The limit has to be less than '"+timeLimit+"' ms.");
	}
	public void checkDependsOnOtherCm()
	throws Exception
	{
		List cmList = getDependsOnCm();
		if (cmList == null)
			return;

		for (Iterator it = cmList.iterator(); it.hasNext();)
		{
			String cmName = (String) it.next();
			
			CountersModel cm = GetCounters.getCmByName(cmName);
			if (cm == null)
				throw new Exception("Depends on '"+cmName+"', which can't be found.");
			
			checkDependsOnOtherCm(cm);
		}
	}
	public void addDependsOnCm(String cmName)
	{
		if (_dependsOnCm == null)
			_dependsOnCm = new LinkedList();
		_dependsOnCm.add(cmName);
	}
	public List getDependsOnCm()
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
	public void incSumRowCount() { _sumRowCount += getRowCount(); }

	/** called from GUI */
	public final synchronized void refresh() throws Exception
	{
		refresh(Asemon.getCounterCollector().getMonConnection());
	}

	/** called from NO GUI */
	public final synchronized void refresh(Connection conn) throws Exception
	{
		// check if we depends on other CM's
		checkDependsOnOtherCm();
		
		// is it time to do refresh or not
		if (getTimeToNextPostponedRefresh() > 0)
		{
			_logger.debug("Next refresh for the cm '"+getName()+"' will have to wait '"+TimeUtils.msToTimeStr(getTimeToNextPostponedRefresh())+"'.");
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
		try
		{
			refreshGetData(conn);

			// Simulate a slow connection...
			//try { Thread.sleep(300); }
			//catch (InterruptedException ignore) {}
		}
		catch (Exception e)
		{
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
		incRefreshCounter();
		incSumRowCount();
		setInitialized(true);

		// Set last refresh time
		_lastLocalRefreshTime = System.currentTimeMillis();
	}

	/**
	 * This is the method to override if you want to different refresh
	 * @param conn
	 * @throws Exception
	 */
	protected void refreshGetData(Connection conn) throws Exception
	{
		if (_logger.isDebugEnabled())
		{
			_logger.debug("Entering refreshCM() method for " + _name);
		}

		if ( ! isSwingRefreshOK() )
		{
			// a swing refresh is in process, don't break the data, so wait for the next loop
			if (_logger.isDebugEnabled())
			{
				_logger.debug("Exit refreshCM() method for " + _name + ". Swingrefresh already processing.");
			}
			return;
		}

		if (conn == null)
			return;

		if (_logger.isDebugEnabled())
			_logger.debug("Refreshing Counters for '"+getName()+"'.");

		// Start the timer which will be kicked of after X ms
		// This si we can do something if the refresh takes to long time
		_refreshTimer.start();


		// If the CounterModel need to be initialized by executing any 
		// specific SQL statement the firts time around
		if (firstTimeSample && _sqlInit != null && !_sqlInit.trim().equals(""))
		{
			try
			{
				Statement stmt = conn.createStatement();
				stmt.execute(_sqlInit);
				stmt.close();
			}
			catch (SQLException e)
			{
				_logger.warn("Problem when executing the 'init' SQL statement.", e);
			}
		}

		if (_sqlRequest == null)
		{
			initSql(conn);
		}

		newSample = new SamplingCnt(_name, _negativeDiffCountersToZero);
		try
		{
			_sampleException = null;
			newSample.getCnt(this, conn, getSql()+getSqlWhere(), _pkCols);
		}
		catch (SQLException e)
		{
			if (tabPanel != null)
			{
				String msg = e.getMessage();
				_sampleException = e;
				// maybe use property change listeners instead: firePropChanged("status", "refreshing");
				tabPanel.setWatermarkText(msg);
			}
			return;
		}
		finally
		{
			// Stop the timer.
			_refreshTimer.stop();
		}

		initColumnStuff(newSample);
//		// Initialize isDiffDissCol array
//		if (_isDiffDissCol == null)
//		{
//			Vector colNames = newSample.getColNames();
//			if (colNames != null)
//			{
//				_isDiffDissCol = new boolean[ colNames.size() ];
//				for (int i = 0; i < _isDiffDissCol.length; i++)
//				{
//					String colname = (String) colNames.get(i);
//					boolean found = false;
//					for (int j = 0; j < _diffDissColumns.length; j++)
//					{
//						if (colname.equals(_diffDissColumns[j]))
//							found = true;
//					}
//					_logger.trace(_name+" col["+i+"]="+found+", colname="+colname);
//					_isDiffDissCol[i] = found;
//				}
//			}
//		}
//
//		// Initialize isDiffCol array
//		if (_isDiffCol == null)
//		{
//			Vector colNames = newSample.getColNames();
//			if (colNames != null)
//			{
//				_isDiffCol = new boolean[ colNames.size() ];
//				for (int i = 0; i < _isDiffCol.length; i++)
//				{
//					String colname = (String) colNames.get(i);
//					boolean found = false;
//					for (int j = 0; j < _diffColumns.length; j++)
//					{
//						if (colname.equals(_diffColumns[j]))
//							found = true;
//					}
//					_logger.trace(_name+" col["+i+"]="+found+", colname="+colname);
//					_isDiffCol[i] = found;
//				}
//			}
//		}
//
//		// Initialize isPctCol array
//		if (_isPctCol == null)
//		{
//			Vector colNames = newSample.getColNames();
//			if (colNames != null)
//			{
//				_isPctCol = new boolean[ colNames.size() ];
//				for (int i = 0; i < _isPctCol.length; i++)
//				{
//					String colname = (String) colNames.get(i);
//					boolean found = false;
//					for (int j = 0; j < _pctColumns.length; j++)
//					{
//						if (colname.equals(_pctColumns[j]))
//							found = true;
//					}
//					_logger.trace(_name+" col["+i+"]="+found+", colname="+colname);
//					_isPctCol[i] = found;
//				}
//			}
//		}

		// if it's the first time sampling...
		if (firstTimeSample)
		{
//			saveDdl();
			firstTimeSample = false;
		}

		// If NO PK, then we dont need to do some stuff.
		if ( ! doDiffCalc() )
		{
			setSampleTime(newSample.samplingTime);
			setSampleInterval(0);

			if (oldSample != null)
			{
				newSample.interval = newSample.samplingTime.getTime() - oldSample.samplingTime.getTime(); 
				setSampleInterval(newSample.interval);
			}

			diffData = newSample;
			rateData = newSample;
			setDataInitialized(true);
		}
		else
		{
			if (oldSample != null)
			{
				// old sample is not null, so we can compute the diffs
	//			diffData = SamplingCnt.computeDiffCnt(oldSample, newSample, idKey1, idKey2, idKey3, bitmapColsCalcDiff);
	//			diffData = SamplingCnt.computeDiffCnt(oldSample, newSample, _pkCols, bitmapColsCalcDiff);
				diffData = SamplingCnt.computeDiffCnt(oldSample, newSample, _pkCols, _isDiffCol);
			}
	
			if (diffData == null)
			{
				setSampleTime(newSample.samplingTime);
				setSampleInterval(0);
	
				// This is the first call, diffData could not be computed because
				// oldsample was not present
	//			dataTM = new SamplingCnt(newSample, false, "dataTM");
	//			maxRowSeen = dataTM.getRowCount();
	//			if (TM != null)
					setupTM();
			}
			else
			{
				// Compute local stuff
				localCalculation();
	
				setSampleTime(diffData.samplingTime);
				setSampleInterval(diffData.interval);
	
				// we got some data, compute the rates and update the data model
	//			rateData = SamplingCnt.computeRatePerSec(diffData, bitmapColsCalcDiff, bitmapColsCalcPCT);
				rateData = SamplingCnt.computeRatePerSec(diffData, _isDiffCol, _isPctCol);
	
				setDataInitialized(true);
	//			if (TM != null)
				{
					updateTM();
				}
				
				// Calculte what values we should have in the graphs
				// FIXME: maybe this should be moved outside the "diff" section
				//        and also move localCalculation() to the same place... 
				updateGraphData();
	
				// Save Counters to somewhere
				//saveCounterData();
			}
		}
		oldSample = newSample;

		if ( Asemon.hasGUI() )
		{
			Runnable doWork = new Runnable()
			{
				public void run()
				{
					if (getTabPanel() != null && !getTabPanel().isTableInitialized())
					{
						_logger.debug(getName()+":------doFireTableStructureChanged------");
						fireTableStructureChanged();
						getTabPanel().adjustTableColumnWidth();
					}
					else
					{
						_logger.debug(getName()+":-fireTableDataChanged-");
						fireTableDataChanged();
					}
				}
			};

			// Invoke this job on the SWING Event Dispather Thread
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
	}

	private void setupTM()
	{
		// are we in GUI/NO-GUI mode
		if ( ! Asemon.hasGUI() )
			return;

		Runnable setupTM_inSwingThread = new Runnable()
		{
			public void run()
			{
				setupTM_code();
			}
		};
		setSwingRefreshOK(false);
		SwingUtilities.invokeLater(setupTM_inSwingThread);
	}

	private void setupTM_code()
	{
		try
		{
			if (_logger.isDebugEnabled())
			{
				_logger.debug("Entering setupTM_code() method for " + _name + " maxrow=" + maxRowSeen);
			}
//			TM.setDataVector(dataTM.getDataVector(), dataTM.getColNames());
//jre6			sorter.setTableModel(TM);
//			sorter.setTableModel(TM);

			// Setup list of filter columns in the filterCol combo box
			// Colnames will only be set if the list is NOT initialized.
//			tabPanel.setFilterColumns( dataTM.getColNames() );

			// Update dates on panel
//			tabPanel.setTimeInfo(null, dataTM.samplingTime, 0);
//			tabPanel.absDateTxt.setText(MainFrame.summaryPanel.getCountersCleared());
//			tabPanel.intDateTxt.setText(dataTM.samplingTime.toString());
			if (_logger.isDebugEnabled())
			{
				_logger.debug("Leaving setupTM_code() method for " + _name);
			}
//			if (getName().equals("CMprocActivity"))
//				tabPanel.dataTable.setToolTipText("Double-click a row to get the details");
//			if (getName().equals("CMprocActivity"))
//				tabPanel.setTableToolTipText("Double-click a row to get the details");
			
		}
		finally
		{
			setSwingRefreshOK(true);
//			tabPanel.calcColumnWidths();
			if (tabPanel != null)
			{
//				tabPanel.setWatermark();
			}
		}
	}


	public synchronized void updateTM()
	{
		// are we in GUI/NO-GUI mode
		if ( ! Asemon.hasGUI() )
			return;

		Runnable updateTM_inSwingThread = new Runnable()
		{
			public void run()
			{
				updateTM_code();
			}
		};

		if ((isDataInitialized()) && (isSwingRefreshOK()))
		{
			setSwingRefreshOK(false);
			SwingUtilities.invokeLater(updateTM_inSwingThread);
		}
		else
		{
			if (tabPanel != null)
			{
//				tabPanel.setWatermark();
			}

			if ( ! isDataInitialized() ) 
			{
				GetCounters.setWaitEvent("Data has not yet been initialized.");
//				MainFrame.statusFld.setText("Wait... Data has not yet been initialized.");
			}
			else if ( isSwingRefreshOK() )
			{
				GetCounters.setWaitEvent("Swing Refresh is not yet true.");
//				MainFrame.statusFld.setText("Wait... Swing Refresh is not yet true.");
			}
		}
	}

	private void updateTM_code()
	{
		// Used to refresh the JTable associated with this TABLEMODEL
//		String key = new String("");
//		Object val = null;
//
//		Vector newRow;
//		Vector oldRow;
//		int oldRowId;
//		Object oldVal;
//		Object newVal;

		if (_logger.isDebugEnabled())
		{
			_logger.debug("Entering updateTM_code() method for " + _name);
		}

//		if (dataTM == null)
//		{
//			_logger.debug("updateTM_code : data is null");
//			return;
//		}
//		if (idKey1 == 0)
//		{
//			// should not happen here , but ...
//			return;
//		}

		try
		{
			// Update dates on panel
//			long intervall = 0;
//			if (diffData != null)
//				intervall = diffData.interval;
//			tabPanel.setTimeInfo(null, newSample.samplingTime, intervall);
//			tabPanel.absDateTxt.setText(MainFrame.summaryPanel.getCountersCleared());
//			tabPanel.intDateTxt.setText(newSample.samplingTime.toString());
//			if (diffData != null)
//				tabPanel.intervalTxt.setText(Integer.toString((int) diffData.interval));

//			if (_dataSource == DATA_ABS)
//			{
//				chosenData = newSample;
//			}
//			else if (_dataSource == DATA_DIFF)
//			{
//				chosenData = diffData;
//			}
//			else if (_dataSource == DATA_RATE)
//			{
//				chosenData = rateData;
//			}
//			else
//			{
//				chosenData = null;
//			}

			// Save times
//			dataTM.samplingTime = chosenData.samplingTime;
//			dataTM.interval     = chosenData.interval;

//SamplingCnt x = null;
//             _logger.info("1:-------- CM: "+getName());
//x=newSample; _logger.info("1:newSample  rows="+x.getRowCount()+", cols="+x.getColumnCount());
//x=oldSample; _logger.info("1:oldSample  rows="+x.getRowCount()+", cols="+x.getColumnCount());
//x=diffData;  _logger.info("1:diffData   rows="+x.getRowCount()+", cols="+x.getColumnCount());
//x=chosenData;_logger.info("1:chosenData rows="+x.getRowCount()+", cols="+x.getColumnCount());
//x=dataTM;    _logger.info("1:dataTM     rows="+x.getRowCount()+", cols="+x.getColumnCount());
			// Loop on all new rows
//			boolean rowInserted = false;
//			for (int newRowId = 0; newRowId < chosenData.getRowCount(); newRowId++)
//			{
//				newRow = (Vector) chosenData.getRow(newRowId);
//				boolean newRowIsFiltered = isFiltered(newRow);
//				key = chosenData.getPkValue(newRowId);
//
//				// Retreive old same row
//				oldRowId = dataTM.getRowNumberForPkValue(key);
//
//				
//				// Old row found
//				if (oldRowId != -1)
//				{
////System.out.println("dataTM.-update-rowid="+oldRowId);
////					TM.fireTableRowsUpdated(oldRowId, oldRowId);
//					oldRow = (Vector) dataTM.getRow(oldRowId);
//					for (int i=0; i<dataTM.getColumnCount(); i++)
//					{
//						if ( ! dataTM.isColPartOfPk(i) )
//						{
//							oldVal = oldRow.get(i);
//							newVal = newRow.get(i);
//							if (oldVal == null)
//							{
//								if (newVal != null)
//								{
//									// Just set the new value, 
//									// row will be filtered later
//									oldRow.setElementAt(newVal, i);
//								}
//								continue;
//							}
//							if (oldVal.equals(newVal))
//								continue;
//
//							if (newRowIsFiltered)
//								// Just set the new value, row will be filtered
//								// after
//								oldRow.setElementAt(newVal, i);
//							else
//								// set data and notify to TableModel
//								TM.setValueAt(newVal, oldRowId, i);
//						}
//					}
//				}
//				// Old row not found, add new row to dataTM and TM
//				else
//				{
//					// Check if it is filtered
//					if ( ! newRowIsFiltered )
//					{
//						// save HKEY with corresponding row
//						dataTM.addRow(newRow);
//
//						// add row
////						Vector newRowClone = (Vector) newRow.clone();
////						TM.addRow(newRowClone);
////						TM.setRowCount(dataTM.getRowCount());
//						rowInserted = true;
//						
//						int rowIdAdded = dataTM.getRowCount() - 1;
//						TM.fireTableRowsInserted(rowIdAdded, rowIdAdded);
//					}
//				}
//			}
////x=dataTM;    _logger.info("x:dataTM     rows="+x.getRowCount()+", cols="+x.getColumnCount());
//
//			// Check if some old row no longer exists or must be filtered
//			boolean rowdeleted = false;
//			for (oldRowId = dataTM.getRowCount() - 1; oldRowId >= 0; oldRowId--)
//			{
//				oldRow = (Vector) dataTM.getRow(oldRowId);
//				boolean oldRowIsFiltered = isFiltered(oldRow);
//				key = dataTM.getPkValue(oldRowId);
//
//				int r = chosenData.getRowNumberForPkValue(key);
////_logger.info("oldRowId="+oldRowId+", oldRowIsFiltered="+oldRowIsFiltered+", r="+r+", key='"+key+"'.");
//				if (r == -1 || oldRowIsFiltered)
//				{
////_logger.info("DELETE oldRowId="+oldRowId+".");
////System.out.println("dataTM.----remove-row----, key="+key+", oldRowId="+oldRowId);
//					// Corresponding new row not found, or old row filtered
////					TM.removeRow(oldRowId);
//					dataTM.remove(key);
//					rowdeleted = true;
////					TM.setRowCount(dataTM.getRowCount());
//					TM.fireTableRowsDeleted(oldRowId, oldRowId);
//				}
//			}
////x=dataTM;    _logger.info("y:dataTM     rows="+x.getRowCount()+", cols="+x.getColumnCount());
////_logger.info("rowDeleted="+rowdeleted);
//			if (rowdeleted)
//			{
//				dataTM.newRowIds();
////				TM.fireTableDataChanged();
//			}
////             _logger.info("2:-------- CM: "+getName());
////x=newSample; _logger.info("2:newSample  rows="+x.getRowCount()+", cols="+x.getColumnCount());
////x=oldSample; _logger.info("2:oldSample  rows="+x.getRowCount()+", cols="+x.getColumnCount());
////x=diffData;  _logger.info("2:diffData   rows="+x.getRowCount()+", cols="+x.getColumnCount());
////x=chosenData;_logger.info("2:chosenData rows="+x.getRowCount()+", cols="+x.getColumnCount());
////x=dataTM;    _logger.info("2:dataTM     rows="+x.getRowCount()+", cols="+x.getColumnCount());
		}
		finally
		{
			// Update the view if anything has changed
			//TM.fireTableDataChanged();

			setSwingRefreshOK(true);
//			if (maxRowSeen < dataTM.getRowCount())
//			{
//				maxRowSeen = dataTM.getRowCount();
////jre6				sorter.setTableModel(TM);
////				sorter.setTableModel(TM);
//			}
			
			if (tabPanel != null)
			{
//				tabPanel.calcColumnWidths();
//				tabPanel.setWatermark();
			}
		}
	}


	public void clearForRead()
	{
		if (oldSample != null) oldSample.removeAllRows();
		if (newSample != null) newSample.removeAllRows();
		if (diffData  != null) diffData .removeAllRows();
		if (rateData  != null) rateData .removeAllRows();
	}

	public void clear()
	{
		clear(100);
	}
	public synchronized void clear(int clearLevel)
	{
		clearCmLevel = clearLevel;

		oldSample         = null;
		newSample         = null;
		diffData          = null;
		rateData          = null;
		setSwingRefreshOK(true);
		setDataInitialized(false);
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
		fireTableDataChanged();
	}
	
	private SamplingCnt getCounterData()
	{
		return getCounterData(_dataSource);
	}
	private SamplingCnt getCounterData(int whatData)
	{
		SamplingCnt data = null;
	
		if      (whatData == DATA_ABS)  data = newSample;
		else if (whatData == DATA_DIFF) data = diffData;
		else if (whatData == DATA_RATE) data = rateData;
		else
			throw new RuntimeException("Only ABS, DIFF, or RATE data is available.");
		return data;
	}

	public boolean hasAbsData()
	{
		return newSample != null;
	}

	public boolean hasDiffData()
	{
		return diffData != null;
	}

	public boolean hasRateData()
	{
		return rateData != null;
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
		if (diffData == null)
			return null;
		return diffData.samplingTime;
	}

	public int getLastSampleInterval()
	{
		if (diffData != null)
			return (int) diffData.interval;

		return 0;
	}
	
	// Return number of rows in the diff table
	public synchronized int size()
	{
		return (diffData == null) ? 0 : diffData.getRowCount();
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
		SamplingCnt data = null;

		if      (whatData == DATA_ABS)  data = newSample;
		else if (whatData == DATA_DIFF) data = diffData;
		else if (whatData == DATA_RATE) data = rateData;
		else
			throw new RuntimeException("Only ABS, DIFF, or RATE data is available.");

		if (data == null)
			return null;

		int idCol = data.getColNames().indexOf(colname);
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
//	private synchronized Object getValue(int whatData, int rowId, int colId)
//	{
//		SamplingCnt data = null;
//
//		if      (whatData == DATA_ABS)  data = newSample;
//		else if (whatData == DATA_DIFF) data = diffData;
//		else if (whatData == DATA_RATE) data = rateData;
//		else
//			throw new RuntimeException("Only ABS, DIFF, or RATE data is available.");
//
//		if (data == null)
//		{
//			_logger.debug("getValue(type, rowId, colId): Cant find any data.");
//			return null;
//		}
//
//		if (data.getColNames().size() <= colId)
//		{
//			_logger.debug("getValue(type, rowId, colId): colId='"+colId+"', is larger that columns in data '"+data.getColNames().size()+"'.");
//			return null;
//		}
//		if (data.getRowCount() <= rowId)
//		{
//			_logger.debug("getValue(type, rowId, colId): rowId='"+rowId+"', is larger that rows in data '"+data.getRowCount()+"'.");
//			return null;
//		}
//
//		return data.getValueAt(rowId, colId);
//	}

	// Return the value of a cell by keyVal, (keyVal, ColumnName)
	private synchronized Double getValue(int whatData, String keyVal, String colname)
	{
		SamplingCnt data = null;

		if      (whatData == DATA_ABS)  data = newSample;
		else if (whatData == DATA_DIFF) data = diffData;
		else if (whatData == DATA_RATE) data = rateData;
		else
			throw new RuntimeException("Only ABS, DIFF, or RATE data is available.");

		if (data == null)
			return null;

		for (int i = 0; i < data.getRowCount(); i++)
		{
			Vector row = (Vector) data.getRow(i);
			if (row.get(0).equals(keyVal))
			{
				Object o = getValue(whatData, i, colname);
				if (o instanceof Double)
				{
					return (Double) o;
				}
				else
				{
					return new Double(o.toString());
				}
			}
		}
		return null;
	}

	// Return the MAX of the values of a column (ColumnName)
	private synchronized Double getMaxValue(int whatData, String colname)
	{
		SamplingCnt data = null;

		if      (whatData == DATA_ABS)  data = newSample;
		else if (whatData == DATA_DIFF) data = diffData;
		else if (whatData == DATA_RATE) data = rateData;
		else
			throw new RuntimeException("Only ABS, DIFF, or RATE data is available.");

		if (data == null)
			return null;

		int idCol = data.getColNames().indexOf(colname);
		if (idCol == -1)
		{
			_logger.info("getMaxValue: Cant find the column '" + colname + "'.");
			return null;
		}
		if (data.getRowCount() == 0)
			return null;

		double maxResult = 0;
		double result = 0;
		for (int i = 0; i < data.getRowCount(); i++)
		{
			Object o = data.getValueAt(i, idCol);
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
	private synchronized Double getMinValue(int whatData, String colname)
	{
		SamplingCnt data = null;

		if      (whatData == DATA_ABS)  data = newSample;
		else if (whatData == DATA_DIFF) data = diffData;
		else if (whatData == DATA_RATE) data = rateData;
		else
			throw new RuntimeException("Only ABS, DIFF, or RATE data is available.");

		if (data == null)
			return null;

		int idCol = data.getColNames().indexOf(colname);
		if (idCol == -1)
		{
			_logger.info("getMinValue: Cant find the column '" + colname + "'.");
			return null;
		}
		if (data.getRowCount() == 0)
			return null;

		double minResult = 0;
		double result = 0;
		for (int i = 0; i < data.getRowCount(); i++)
		{
			Object o = data.getValueAt(i, idCol);
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
	private synchronized Double getSumValue(int whatData, String colname)
	{
		SamplingCnt data = null;

		if      (whatData == DATA_ABS)  data = newSample;
		else if (whatData == DATA_DIFF) data = diffData;
		else if (whatData == DATA_RATE) data = rateData;
		else
			throw new RuntimeException("Only ABS, DIFF, or RATE data is available.");

		if (data == null)
			return null;

		int idCol = data.getColNames().indexOf(colname);
		if (idCol == -1)
		{
			_logger.info("getSumValuePCT: Cant find the column '" + colname + "'.");
			return null;
		}
		if (data.getRowCount() == 0)
			return null;
		double result = 0;
		for (int i = 0; i < data.getRowCount(); i++)
		{
			Object o = data.getValueAt(i, idCol);
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
	private synchronized int getCountGtZero(int whatData, String colname)
	{
		SamplingCnt data = null;

		if      (whatData == DATA_ABS)  data = newSample;
		else if (whatData == DATA_DIFF) data = diffData;
		else if (whatData == DATA_RATE) data = rateData;
		else
			throw new RuntimeException("Only ABS, DIFF, or RATE data is available.");

		if (data == null)
			return 0;

		int idCol = data.getColNames().indexOf(colname);
		if (idCol == -1)
		{
			_logger.info("getSumValuePCT: Cant find the column '" + colname + "'.");
			return 0;
		}
		if (data.getRowCount() == 0)
			return 0;

		int counter = 0;
		for (int i = 0; i < data.getRowCount(); i++)
		{
			Object o = data.getValueAt(i, idCol);
			if (o == null)
				continue;

			if (o instanceof Number)
			{
				if ( ((Number)o).longValue() > 0 )
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
	private synchronized Double getAvgValue(int whatData, String colname)
	{
		SamplingCnt data = null;

		if      (whatData == DATA_ABS)  data = newSample;
		else if (whatData == DATA_DIFF) data = diffData;
		else if (whatData == DATA_RATE) data = rateData;
		else
			throw new RuntimeException("Only ABS, DIFF, or RATE data is available.");

		if (data == null)
			return null;

		Double sum = getSumValue(whatData, colname);
		if (sum == null)
			return null;

		int count = data.getRowCount();
		
		if (count == 0)
			return new Double(0);
		else
			return new Double(sum.doubleValue() / count);
	}

	// Return the AVG of the values of a Long column (ColumnName)
	private synchronized Double getAvgValueGtZero(int whatData, String colname)
	{
		SamplingCnt data = null;

		if      (whatData == DATA_ABS)  data = newSample;
		else if (whatData == DATA_DIFF) data = diffData;
		else if (whatData == DATA_RATE) data = rateData;
		else
			throw new RuntimeException("Only ABS, DIFF, or RATE data is available.");

		if (data == null)
			return null;

		Double sum = getSumValue(whatData, colname);
		if (sum == null)
			return null;

		int count = getCountGtZero(whatData, colname);
		
		if (count == 0)
			return new Double(0);
		else
			return new Double(sum.doubleValue() / count);
	}

	// Return the Primary Key for a specific row
	private synchronized String getPkValue(int whatData, int rowId)
	{
		SamplingCnt data = null;

		if      (whatData == DATA_ABS)  data = newSample;
		else if (whatData == DATA_DIFF) data = diffData;
		else if (whatData == DATA_RATE) data = rateData;
		else
			throw new RuntimeException("Only ABS, DIFF, or RATE data is available.");

		if (data == null)
			return null;

		return data.getPkValue(rowId);
	}

	// Wrapper functions to read ABSOLUTE values
	public String getAbsString        (int    rowId,  String colname) { Object o = getValue    (DATA_ABS, rowId,  colname); return (o==null)?"":o.toString(); }
	public Object getAbsValue         (int    rowId,  String colname) { return getValue        (DATA_ABS, rowId,  colname); }
	public Double getAbsValue         (String keyVal, String colname) { return getValue        (DATA_ABS, keyVal, colname); }
	public Double getAbsValueAsDouble (int    rowId,  String colname) { return getValueAsDouble(DATA_ABS, rowId,  colname); }
	public Double getAbsValueMax      (String colname) { return getMaxValue      (DATA_ABS, colname); }
	public Double getAbsValueMin      (String colname) { return getMinValue      (DATA_ABS, colname); }
	public Double getAbsValueAvg      (String colname) { return getAvgValue      (DATA_ABS, colname); }
	public Double getAbsValueAvgGtZero(String colname) { return getAvgValueGtZero(DATA_ABS, colname); }
	public Double getAbsValueSum      (String colname) { return getSumValue      (DATA_ABS, colname); }
	public String getAbsPkValue       (int    rowId)   { return getPkValue       (DATA_ABS, rowId  ); }

	// Wrapper functions to read DIFF (new-old) values
	public String getDiffString        (int    rowId,  String colname) { Object o = getValue    (DATA_DIFF, rowId,  colname); return (o==null)?"":o.toString(); }
	public Object getDiffValue         (int    rowId,  String colname) { return getValue        (DATA_DIFF, rowId,  colname); }
	public Double getDiffValue         (String keyVal, String colname) { return getValue        (DATA_DIFF, keyVal, colname); }
	public Double getDiffValueAsDouble (int    rowId,  String colname) { return getValueAsDouble(DATA_DIFF, rowId,  colname); }
	public Double getDiffValueMax      (String colname) { return getMaxValue      (DATA_DIFF, colname); }
	public Double getDiffValueMin      (String colname) { return getMinValue      (DATA_DIFF, colname); }
	public Double getDiffValueAvg      (String colname) { return getAvgValue      (DATA_DIFF, colname); }
	public Double getDiffValueAvgGtZero(String colname) { return getAvgValueGtZero(DATA_DIFF, colname); }
	public Double getDiffValueSum      (String colname) { return getSumValue      (DATA_DIFF, colname); }
	public String getDiffPkValue       (int    rowId)   { return getPkValue       (DATA_DIFF, rowId  ); }

	// Wrapper functions to read RATE DIFF/time values
	public String getRateString        (int    rowId,  String colname) { Object o = getValue    (DATA_RATE, rowId,  colname); return (o==null)?"":o.toString(); }
	public Object getRateValue         (int    rowId,  String colname) { return getValue        (DATA_RATE, rowId,  colname); }
	public Double getRateValue         (String keyVal, String colname) { return getValue        (DATA_RATE, keyVal, colname); }
	public Double getRateValueAsDouble (int    rowId,  String colname) { return getValueAsDouble(DATA_RATE, rowId,  colname); }
	public Double getRateValueMax      (String colname) { return getMaxValue      (DATA_RATE, colname); }
	public Double getRateValueMin      (String colname) { return getMinValue      (DATA_RATE, colname); }
	public Double getRateValueAvg      (String colname) { return getAvgValue      (DATA_RATE, colname); }
	public Double getRateValueAvgGtZero(String colname) { return getAvgValueGtZero(DATA_RATE, colname); }
	public Double getRateValueSum      (String colname) { return getSumValue      (DATA_RATE, colname); }
	public String getRatePkValue       (int    rowId)   { return getPkValue       (DATA_RATE, rowId  ); }

	
	public void setSwingRefreshOK(boolean b)
	{
		swingRefreshOK = b;
	}
	public boolean isSwingRefreshOK() 
	{
		return swingRefreshOK;
	}
	
	public void setDataInitialized(boolean b)
	{
		dataInitialized = b;
	}
	public boolean isDataInitialized()
	{
		return dataInitialized;
	}
	
	/**
	 * If we are in swing, the data is fetched by a special thread.
	 * So we need to wait for data to be fetched and processed before 
	 * we can continue to read the data...
	 * FIXME: change this to wait() on an Object instead.
	 */
	public void waitForSwingDataRefresh()
	{
		if (isDataInitialized())
		{
			long startWait = System.currentTimeMillis();

			// FIXME: change this to wait() on an Object instead.
			while(true)
			{
				if ( isSwingRefreshOK() )
					return;

				// Don't wait for too, long...
				long waitedForMs = System.currentTimeMillis() - startWait;
				if ( waitedForMs > 1000 )
				{
					_logger.warn(getName() + ": waitForSwingDataRefresh() timed out after "+waitedForMs+" ms.");
					return;
				}

				try 
				{ 
					MainFrame.setStatus(MainFrame.ST_STATUS_FIELD, "Refreshing... Graphs... Waiting for '"+getDisplayName()+"' to completing data read.");

					_logger.debug(getName() + ": waitForSwingDataRefresh(), 20 ms");
					Thread.sleep(20); 
				}
				catch(InterruptedException ignore) 
				{
					return;
				}
			}
		}
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
	private void saveProps()
  	{
		Configuration tempProps = Configuration.getInstance(Configuration.TEMP);
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

			tempProps.save();
		}
	}

	private void loadProps()
	{
		Configuration tempProps = Configuration.getInstance(Configuration.TEMP);
//		Configuration confProps = Configuration.getInstance(Configuration.CONF);
		String base = this.getName() + ".";
		
		if (tempProps != null)
		{
			_inLoadProps = true;

			setFilterAllZero(                tempProps.getBooleanProperty(base + PROP_filterAllZeroDiffCounters,  isFilterAllZero()) );
			setPauseDataPolling(             tempProps.getBooleanProperty(base + PROP_sampleDataIsPaused,         isDataPollingPaused()) );
			setBackgroundDataPollingEnabled( tempProps.getBooleanProperty(base + PROP_sampleDataInBackground,     isBackgroundDataPollingEnabled()) );
			setNegativeDiffCountersToZero(   tempProps.getBooleanProperty(base + PROP_negativeDiffCountersToZero, isNegativeDiffCountersToZero()) );
			setPersistCounters(              tempProps.getBooleanProperty(base + PROP_persistCounters,            isPersistCountersEnabled()) );
			setPersistCountersAbs(           tempProps.getBooleanProperty(base + PROP_persistCounters_abs,        isPersistCountersAbsEnabled()) );
			setPersistCountersDiff(          tempProps.getBooleanProperty(base + PROP_persistCounters_diff,       isPersistCountersDiffEnabled()) );
			setPersistCountersRate(          tempProps.getBooleanProperty(base + PROP_persistCounters_rate,       isPersistCountersRateEnabled()) );

			setPostponeTime(                 tempProps.getIntProperty    (base + PROP_postponeTime,               getPostponeTime()) );

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
		String _dbname;               // in what database should the proc be in
		String _procName;             // name of the proc
		Date   _procDateThreshold;    // recreate if the proc is created "earlier" than this date
		Class  _scriptLocation;       // in what "directory" (actually a classname) do we find the script 
		String _scriptName;           // name of the script (from within the jar file or classpath)
		String _needsRoleToRecreate;  // what ROLE inside ASE server do we need to create this proc

		StoredProcCheck(String dbname, String procName, Date procDateThreshold, 
				Class scriptLocation, String scriptName, String needsRoleToRecreate)
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