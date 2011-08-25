/**
*/

package asemon;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;

import org.apache.log4j.Logger;

import asemon.gui.MainFrame;
import asemon.gui.SummaryPanel;
import asemon.gui.TabularCntrPanel;
import asemon.gui.TrendGraph;
import asemon.utils.Configuration;

public class CountersModel 
extends AbstractTableModel
{
    private static final long serialVersionUID = -7486772146682031469L;

	/** Log4j logging. */
	private static Logger	   _logger	          = Logger.getLogger(CountersModel.class);

//	public  static final int   FILTER_EQ          = 0;  // EQUAL
//	public  static final int   FILTER_NE          = 1;  // NOT_EQUAL
//	public  static final int   FILTER_GT          = 2;  // GREATER THAN
//	public  static final int   FILTER_LT          = 3;  // LESS THAN
//	private static final int   FILTER__MAX_ID     = FILTER_LT;

	public static final int	   DATA_ABS	          = 1;
	public static final int	   DATA_DIFF	      = 2;
	public static final int	   DATA_RATE	      = 3;

	private boolean            _negativeDiffCountersToZero = true;

	protected Timer              _refreshTimer = new Timer(200, new RefreshTimerAction());
	private String	           _name;
	private String             _displayName       = null;  // Name that will be tabname etc
	private String	           _description       = "";    // Can be used for tool tips etc
	private String	           _problemDesc       = "";    // Can be used for tool tips etc

	// Sample info, this members are set by the "main" sample thread 
	private String             _serverName        = "";
	private Timestamp          _sampleTimeHead    = null;
	private Timestamp          _counterClearTime  = null;
	private Timestamp          _sampleTime        = null;
	private long               _sampleInterval    = 0;

	// private int typeModel;
	private int                _serverVersion     = 0;
	private String             _sqlInit           = null;
	private String             _sqlClose          = null; // Not used yet
	private String	           _sqlRequest        = null;
	private String             _sqlWhere          = "";
	protected TabularCntrPanel   tabPanel          = null;
	private List               _pkCols            = null;
//	private int	               idKey1;
//	private int	               idKey2;
//	private int	               idKey3;

	private String[]	       _monTablesInQuery	  = null;

	private ResultSetMetaData  _rsmd;

	// Structure for storing list of columns to compute difference between two samples
	private String[]	       _diffColumns = null;
	private boolean[]          _isDiffCol   = null;
//	private Vector	           bitmapColsCalcDiff	= null;

	// Structure for storing list of columns to display rate as PCT rather than pure rate
	private String[]	       _pctColumns  = null;
	protected boolean[]          _isPctCol    = null;
//	private Vector	           bitmapColsCalcPCT	= null;

	// In the filter (check for nonZeroValues) disregards these column(s)
	private String[]	       _diffDissColumns = null;
	private boolean[]          _isDiffDissCol   = null;
	
//	private String	           filterColName;
//	private int	               filterColId;
//	private int                filterOp;
//
//	private Object             filterValue;
//

	/** If this CM is valid, the connected ASE Server might not support 
	 * this CM due to to early version or not configured properly. */
	private boolean            _isActive = true;

	/** A collection of Graphs connected to this CM */
	private Map                _trendGraphs = new HashMap();

	private boolean            filterAllZeroDiffCounters = false;
	private boolean            sampleData = true;
	private boolean            sampleDataInBackground = false;
	private boolean            persistCounters = true;
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
	** BEGIN: implementing TableModel or overriding AbstractTableModel
	**---------------------------------------------------
	*/
	public void addTableModelListener(TableModelListener l)
	{
		//System.out.println("addTableModelListener(l="+l+")");
		super.addTableModelListener(l);
	}
	public void removeTableModelListener(TableModelListener l)
	{
		//System.out.println("removeTableModelListener(l="+l+")");
		super.removeTableModelListener(l);
	}

//	public Class getColumnClass(int columnIndex)
//	{
//		if (!isDataInitialized())   return null;
//		if (chosenData == null) return null;
//		return chosenData.get(row, col);
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


	
	
	/*---------------------------------------------------
	** BEGIN: constructors
	**---------------------------------------------------
	*/
	public CountersModel()
	{
	}

//	public CountersModel(String nm, String sql, TabularCntrPanel tp, int idK1, int idK2, int idK3, String[] ccd, String[] ccp, String[] monTables)
	public CountersModel(String nm, String sql, List pkList, String[] ccd, String[] ccp, String[] monTables, boolean negativeDiffCountersToZero)
	{
		// Initialize a model for use with a JTable
		_name = nm;
		if (_name.length() > 20)
		{
			throw new RuntimeException("Max length of a CountersModel name is 20 chars. the name '"+_name+"' is of length "+_name.length()+".");
		}
		_sqlRequest       = sql;
		_sqlWhere         = "";
		_pkCols           = pkList;
		_diffColumns      = ccd;
		_pctColumns       = ccp;
		_monTablesInQuery = monTables;
		_negativeDiffCountersToZero = negativeDiffCountersToZero;

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

		// Load saved properties
		loadProps();

		setDataInitialized(false);
		setSwingRefreshOK(true);
	}

	/*---------------------------------------------------
	** END: constructors
	**---------------------------------------------------
	*/

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
	
	
	public void setNegativeDiffCountersToZero(boolean b)
	{
		_negativeDiffCountersToZero = b;
		saveProps();
	}
	public boolean isNegativeDiffCountersToZero()
	{
		return _negativeDiffCountersToZero;
	}

	public void setFilterAllZero(boolean b)
	{
		filterAllZeroDiffCounters = b;
		saveProps();
	}
	public boolean isFilterAllZero()
	{
		return filterAllZeroDiffCounters;
	}

	public void setPauseDataPolling(boolean b)
	{
		if (b)
			sampleData = false;
		else
			sampleData = true;
		saveProps();
	}

//	public boolean isDataPollingEnabled()
//	{
//		return sampleData;
//	}
	public boolean isDataPollingPaused()
	{
		return !sampleData;
	}

	
	public void setBackgroundDataPollingEnabled(boolean b)
	{
			sampleDataInBackground = b;
			saveProps();
	}

	public boolean isBackgroundDataPollingEnabled()
	{
		return sampleDataInBackground;
	}

	public void setPersistCounters(boolean b)
	{
		persistCounters = b;
		//_logger.error("PERSIST setPersistData() method, NOT FULLY IMPLEMENTED.", new Exception("PERSIST setPersistData() method, NOT FULLY IMPLEMENTED.") );
		saveProps();
	}

	public boolean isPersistCountersEnabled()
	{
		return persistCounters;
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

	public boolean isDiffDissColumn(int col) { return _isDiffDissCol[col]; }
	public boolean isDiffColumn(int col)     { return _isDiffCol[col]; }
	public boolean isPctColumn(int col)      { return _isPctCol[col]; }

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
			tabPanel.setCounterModel(this);
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
		return active;
	}
	public void updateGraphs()
	{
		if (_trendGraphs.size() == 0)
			return;

		for (Iterator it = _trendGraphs.keySet().iterator(); it.hasNext();)
		{
			String graphName = (String)it.next();
			TrendGraph tg = getTrendGraph(graphName);
			tg.addPoint();
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

	public boolean equalsTabPanel(Component comp)
	{
		return comp.equals(tabPanel);
	}

	/** */
	public void setServerVersion(int serverVersion)
	{
		_serverVersion = serverVersion;
	}
	/** */
	public int getServerVersion()
	{
		return _serverVersion;
	}

	/** In here we could call getServerVersion() and decide what SQL syntax we should 
	 * use and what monitor tables and coulmns we should query.
	 * <p>
	 * To set the new version dependent SQL statement, use setSql() method. 
	 * <p>
	 * If getSql() shows NULL or empty SQL statement, this method will be called
	 * to compose a new SQL statement. 
	 */
	public void initSql()
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
	public void setSqlInit(String sql)
	{
		_sqlInit = sql;
	}
	/** */
	public void setSqlClose(String sql)
	{
		_sqlClose = sql;
	}


	public String[] getMonTablesInQuery()
	{
		return _monTablesInQuery;
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
		return MainFrame.isMonConnected();
	}

	public void endOfRefresh()
	{
		if (tabPanel != null)
		{
			tabPanel.setWatermark();
		}
	}

	public synchronized void refresh() throws Exception
	{
		refresh(MainFrame.getMonConnection());
	}
	public synchronized void refresh(Connection conn) throws Exception
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
		if (firstTimeSample && _sqlInit != null)
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

		newSample = new SamplingCnt("newSample", _negativeDiffCountersToZero);
		if (_sqlRequest == null)
		{
			initSql();
		}
		try
		{
			newSample.getCnt(this, conn, getSql()+getSqlWhere(), _pkCols);
		}
		catch (SQLException e)
		{
			if (tabPanel != null)
			{
				String msg = e.getMessage();
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

		// Initialize isDiffDissCol array
		if (_isDiffDissCol == null)
		{
			Vector colNames = newSample.getColNames();
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

		// Initialize isDiffCol array
		if (_isDiffCol == null)
		{
			Vector colNames = newSample.getColNames();
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

		// Initialize isPctCol array
		if (_isPctCol == null)
		{
			Vector colNames = newSample.getColNames();
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

		// if it's the first time sampling...
		if (firstTimeSample)
		{
//			saveDdl();
			firstTimeSample = false;
		}

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

			// Update dates on panel
			if (tabPanel != null)
				tabPanel.setTimeInfo(getCounterClearTime(), getSampleTime(), getSampleInterval());

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

			// Update dates on panel
			if (tabPanel != null)
				tabPanel.setTimeInfo(getCounterClearTime(), getSampleTime(), getSampleInterval());
			
			// we got some data, compute the rates and update the data model
//			rateData = SamplingCnt.computeRatePerSec(diffData, bitmapColsCalcDiff, bitmapColsCalcPCT);
			rateData = SamplingCnt.computeRatePerSec(diffData, _isDiffCol, _isPctCol);

			setDataInitialized(true);
//			if (TM != null)
			{
				updateTM();
			}
			
			// Save Counters to somewhere
			//saveCounterData();
		}
		oldSample = newSample;

		if (tabPanel != null && !tabPanel.isTableInitialized())
		{
//			System.out.println(_name+":------doFireTableStructureChanged------");
			fireTableStructureChanged();
			tabPanel.adjustTableColumnWidth();
		}
		else
		{
//			System.out.println(_name+":-fireTableDataChanged-");
			fireTableDataChanged();
		}
		
		if (tabPanel != null)
		{
			// every now and then, adjust the column with of the table
			if (false)
				tabPanel.adjustTableColumnWidth();
		}
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
		String key = new String("");
		Object val = null;

		Vector newRow;
		Vector oldRow;
		int oldRowId;
		Object oldVal;
		Object newVal;

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
			long intervall = 0;
			if (diffData != null)
				intervall = diffData.interval;
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
	private synchronized Object getValue(int whatData, int rowId, int colId)
	{
		SamplingCnt data = null;

		if      (whatData == DATA_ABS)  data = newSample;
		else if (whatData == DATA_DIFF) data = diffData;
		else if (whatData == DATA_RATE) data = rateData;
		else
			throw new RuntimeException("Only ABS, DIFF, or RATE data is available.");

		if (data == null)
		{
			_logger.debug("getValue(type, rowId, colId): Cant find any data.");
			return null;
		}

		if (data.getColNames().size() <= colId)
		{
			_logger.debug("getValue(type, rowId, colId): colId='"+colId+"', is larger that columns in data '"+data.getColNames().size()+"'.");
			return null;
		}
		if (data.getRowCount() <= rowId)
		{
			_logger.debug("getValue(type, rowId, colId): rowId='"+rowId+"', is larger that rows in data '"+data.getRowCount()+"'.");
			return null;
		}

		return data.getValueAt(rowId, colId);
	}

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

	private static String[] _graphMethods = {
		"absMax",  "absMin",  "absAvg",  "absAvgGtZero",  "absSum",
		"diffMax", "diffMin", "diffAvg", "diffAvgGtZero", "diffSum",
		"rateMax", "rateMin", "rateAvg", "rateAvgGtZero", "rateSum"};

	public static String[] getValidGraphMethods()
	{
		return _graphMethods;
	}
	public static String getValidGraphMethodsString()
	{
		String ops = "";
		for (int i=0; i<_graphMethods.length; i++)
		{
			ops += "'" + _graphMethods[i] + "', ";
		}
		return ops;
	}
	public static boolean isValidGraphMethod(String op)
	{
		for (int i=0; i<_graphMethods.length; i++)
		{
			if (_graphMethods[i].equals(op))
				return true;
		}
		return false;
	}
	
	public void setSwingRefreshOK(boolean b)
	{
		swingRefreshOK = b;
	}
	public boolean isSwingRefreshOK() 
	{
		// If 
		if (tabPanel != null)
		{
			tabPanel.setActiveGraph( hasActiveGraphs() );
		}

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

	private void saveProps()
  	{
		Configuration tempProps = Configuration.getInstance(Configuration.TEMP);
		String base = this.getName() + ".";

		if (tempProps != null)
		{
			tempProps.setProperty(base + "filterAllZeroDiffCounters",  filterAllZeroDiffCounters);
			tempProps.setProperty(base + "sampleData",                 sampleData);
			tempProps.setProperty(base + "sampleDataInBackground",     sampleDataInBackground);
			tempProps.setProperty(base + "persistCounters",            persistCounters);
			tempProps.setProperty(base + "negativeDiffCountersToZero", _negativeDiffCountersToZero);

			tempProps.save();
		}
	}

	private void loadProps()
	{
		Configuration tempProps = Configuration.getInstance(Configuration.TEMP);
//		Configuration confProps = Configuration.getInstance(Configuration.CONF);
		String base = this.getName() + ".";
		
		// Use this value as a fallback if the one later on is not there.
//		if (confProps != null)
//			_negativeDiffCountersToZero = confProps.getBooleanProperty("SamplingCnt.negativeDiffCountersToZero", _negativeDiffCountersToZero);


		if (tempProps != null)
		{
			filterAllZeroDiffCounters   = tempProps.getBooleanProperty(base + "filterAllZeroDiffCounters",  filterAllZeroDiffCounters);
			sampleData                  = tempProps.getBooleanProperty(base + "sampleData",                 sampleData);
			sampleDataInBackground      = tempProps.getBooleanProperty(base + "sampleDataInBackground",     sampleDataInBackground);
			persistCounters             = tempProps.getBooleanProperty(base + "persistCounters",            persistCounters);
			_negativeDiffCountersToZero = tempProps.getBooleanProperty(base + "negativeDiffCountersToZero", _negativeDiffCountersToZero);
		}
	}

	
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

}