/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package asemon.gui.swing;

import java.util.ArrayList;

import javax.swing.JTable;

import org.apache.log4j.Logger;
import org.jdesktop.swingx.decorator.Filter;

/**
 * Filter data out (do not show) rows where all specified columns has a 0 value
 * <p>
 * _visibleRows is a ArrayList that holds data that should be visible 
 * in the table/view. This means that we always filter "in" stuff that we 
 * want to be visible in the table view.
 * <p>
 * _rowMapper is a array of integers, array size is the same size 
 * as number of rows in the actual data model (before filter in or out)
 * 
 * @author gorans
 */
public class FilterDiffCounterIsZero
extends Filter
{
	private static Logger _logger = Logger.getLogger(FilterDiffCounterIsZero.class);

	/**
	 *  _rowMapper[row] = -1: Means: not shown, 
	 *  _rowMapper[row] = #:  Means: the int at the array position, points to the "slot" in the _visibleRows ArrayList 
	 *  Or you can say: arrayPos --points-to--> index in ArrayList where we keep the real data row 
	 */
	private int        _rowMapper[];

	/** holds row positions that should be visible in the view */
	private ArrayList  _visibleRows = new ArrayList();

	/** Columns id/pos in the table that we apply the check on */
	private int        _colIds[]     = null;

	/** Is filtering on, or should all rows be visible */
	private boolean    _allRowsVisible = true;


	//---------------------------------------------------------------
	// Constructors 
	//---------------------------------------------------------------
	public FilterDiffCounterIsZero()
	{
        super();
	}

	
	//---------------------------------------------------------------
	// Public methods 
	//---------------------------------------------------------------
	/**
	 * Turn off filtering, all rows will be visible
	 */
	public void resetFilter()
	{
		_logger.debug("Resetting filter, all data will be shown in table.");

		_allRowsVisible = true;
		_colIds         = null;

		refresh(); // This kicks off the "re-filtering" == show data.
	}



	/** Helper to get a column position in the table */
	private int getColumnPos(JTable table, String colname)
	{
		for (int c=0; c<table.getColumnCount(); c++)
		{
			if ( colname.equals( table.getColumnName(c) ) )
				return c;
		}
		return -1;
	}
	
	/**
	 * Set what to filter on. matching rows will be kept/showed in table
	 * @param colNames an String array, each slot holds a column name to check.
	 */
	public void setFilter(JTable table, String[] colNames, String[] disregardCols)
	{
		_logger.trace("Setting filter: colNames="+colNames);
		
		if (table == null || colNames == null)
			return;

		int[] cols = new int[colNames.length];
		for (int i=0; i<cols.length; i++)
		{
			cols[i] = getColumnPos(table, colNames[i]);
			if (cols[i] == -1)
			{
				_logger.warn("Cant find column name '"+colNames[i]+"' in JTable when setting filter.");
			}
			else
			{
				// The columns was found
				// So check the disregard column list...
				if (disregardCols != null)
				{
					for (int d=0; d<disregardCols.length; d++)
					{
						if ( disregardCols[d].equals(colNames[i]) )
						{
							cols[i] = -1;
							_logger.trace("setFilter(table, colnames[]): -dissRegard- cols["+i+"]="+cols[i]+", for colName '"+colNames[i]+"'.");
							break;
						}
					}
				}
				_logger.trace("setFilter(table, colnames[]): cols["+i+"]="+cols[i]+", for colName '"+colNames[i]+"'.");
			}
		}
		
		setFilter(cols);
	}

	/**
	 * Set what to filter on. matching rows will be kept/showed in table
	 * @param cols an int array, each slot holds column position to check.
	 */
	public void setFilter(int[] cols)
	{
		_logger.trace("Setting filter: cols="+cols);

		if (cols == null)
			return;

		_allRowsVisible = false;
		_colIds         = cols;

		refresh(); // This kicks off the "re-filtering"
	}


	
	//---------------------------------------------------------------
	// private helper methods 
	//---------------------------------------------------------------

	/**
	 * Loops all specified columns in the row, if all columns are ZERO
	 * the return false, meaning the row should NOT be displayed
	 * <p>
	 * return true if value should be displayed, meaning it matches the
	 * filter
	 */
	private boolean showInView(int row)
	{
		// isTestable() doc says:
		// Returns true if the column should be included in testing.
		// Here: returns true if visible (that is modelToView gives a valid view column coordinate). 
		//
		// NOTE: not sure if this is correct, or even if it should be in here.
		//
//		if (!adapter.isTestable(_colId))
//		{
//			return false;
//		}

		boolean showRow = false;
		for (int c=0; c<_colIds.length; c++)
		{
			// if we have negative column positions, skip them.
			if (_colIds[c] < 0)
				continue;

			Object cellValue = getInputValue(row, _colIds[c]);

//			if (_logger.isTraceEnabled())
//				_logger.trace(" > showInView(row="+row+", col="+_colIds[c]+"): cellValue='"+cellValue+"', object='"+(cellValue==null?"-null-":cellValue.getClass().getName())+"'.");

			if (cellValue instanceof Number)
			{
				Number num = (Number)cellValue;
				if (num.doubleValue() != 0)
					return true;
			}
			else
			{
				if (cellValue != null)
				{
					String className = cellValue.getClass().getName();
					_logger.trace(" > Column position "+c+" is NOT a Number, the class type is '"+className+"'.");
				}
			}
		}
		return showRow; 
	}

	
	
	
	//---------------------------------------------------------------
	// BEGIN: implement abstract org.jdesktop.swing.decorator.Filter methods 
	//---------------------------------------------------------------

	protected void init()
	{
		_logger.trace("init()");

		//_visibleRows = new ArrayList();
	}

	protected void reset()
	{
		_logger.trace("reset()");

		// We do not need to reset if filtering is "off"
		// no need to recreate the _visibleRows and _rowMapper
		// When the filter becomes "active" again, we will do the reset()
		if (_allRowsVisible)
			return;

		// Clear out all mapping
		_visibleRows.clear();
		int inputSize = getInputSize(); // inputSize is number of rows in table
		_rowMapper = new int[inputSize];  // fromPrevious is inherited protected
		for (int i = 0; i < inputSize; i++) 
		{
			// -1 means, not visible in the table.
			_rowMapper[i] = -1;
		}
	}

	protected void filter()
	{
		_logger.trace("filter()");

		if (_allRowsVisible)
			return;

		if (_colIds == null)
		{
			_logger.info("The Filter is not properly initialized. _colIds == null, reseting the filter to show all rows.");
			_allRowsVisible = true;
			return;
		}

		int inputSize = getInputSize(); // row in the table
		int current = 0;
		for (int i = 0; i < inputSize; i++)
		{
			if ( showInView(i) )
			{
				_visibleRows.add(new Integer(i));
				// _rowMapper should "point" to the List "slot" if visible
				_rowMapper[i] = current++;

				if (_logger.isTraceEnabled())
				{
					_logger.trace("VISIBLE, row="+i+", _rowMapper["+i+"]="+_rowMapper[i]+".");
				}
			}
			else
			{
				if (_logger.isTraceEnabled())
				{
					_logger.trace("   HIDE, row="+i+", _rowMapper["+i+"]="+_rowMapper[i]+".");
				}
			}
		}
	}

	public int getSize()
	{
		if (_allRowsVisible)
			return getInputSize();
		else
			return _visibleRows.size();
	}

	protected int mapTowardModel(int row)
	{
		if (_allRowsVisible)
			return row;
		else
			return ((Integer)_visibleRows.get(row)).intValue();
	}

	protected int mapTowardView(int row)
	{
		if (_allRowsVisible)
			return row;
		else
			return row >= 0 && row < _rowMapper.length ? _rowMapper[row] : -1;
	}
	//---------------------------------------------------------------
	// END: implement abstract org.jdesktop.swing.decorator.Filter methods 
	//---------------------------------------------------------------

	
}
