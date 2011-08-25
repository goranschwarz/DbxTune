/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package asemon.gui.swing;

import java.lang.reflect.Constructor;
import java.text.Collator;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.jdesktop.swingx.decorator.Filter;

/**
 * Filter data using EQual, NotEqual, GreaterThen or LessThen.
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
public class FilterValueAndOp 
extends Filter
{
	private static Logger _logger = Logger.getLogger(FilterValueAndOp.class);

	public static final int FILTER_OP_EQ = 0;  // EQual
	public static final int FILTER_OP_NE = 1;  // Not Equal
	public static final int FILTER_OP_GT = 2;  // Greater Then
	public static final int FILTER_OP_LT = 3;  // Less Then

	/**
	 *  _rowMapper[row] = -1: Means: not shown, 
	 *  _rowMapper[row] = #:  Means: the int at the array position, points to the "slot" in the _visibleRows ArrayList 
	 *  Or you can say: arrayPos --points-to--> index in ArrayList where we keep the real data row 
	 */
	private int        _rowMapper[];

	/** holds data rows that should be visible in the view */
	private ArrayList  _visibleRows = new ArrayList();

	/** Operator to use when filtering data: QE, NE, LT, GT */
	private int        _filterOp    = -1;

	/** String representation of what the _filterOp is applied on */
	private String     _filterVal   = null;

	/** Column id/pos in the table that we apply the operation on */	
	private int        _colId       = -1;

	/** Is filtering on, or should all rows be visible */
	private boolean    _allRowsVisible = true;

	/** In Case, the DataCell in the table is String, apply this regexp instead */
	private Pattern    _strPattern = null;

	/** In Case, the DataCell in the table is NO String, we need to 
	 * create a object of the same type as the object DataCell. Then use
	 * compare() to check for operator (EQ NE LT GT).
	 * For the moment this is only used for Number objects. 
	 */
	private Object     _filterObj  = null;


	//---------------------------------------------------------------
	// Constructors 
	//---------------------------------------------------------------
	public FilterValueAndOp()
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
		_filterOp       = -1;
		_colId          = -1;
		_filterVal      = null;
		_filterObj      = null;
		_strPattern     = null;

//		reset();
		refresh(); // This kicks off the "re-filtering" == show data.
	}

	
	
	/**
	 * Set what to filter on. matching rows will be kept/showed in table
	 * @param op  Operation: 0=(EQ:equal), 1=(NE:not equal), 2=(GT:greater then), 3=(LT:less then)
	 * @param col column id to match Operator and data value with 
	 * @param val data value to match the table/data cells with. 
	 */
	public void setFilter(int op, int col, String val)
	{
		_logger.debug("Setting filter: op="+op+", col="+col+", val='"+val+"'.");

		_allRowsVisible = false;
		_filterOp       = op;
		_colId          = col;
		_filterVal      = val;

		// used to compare NON String values
		_filterObj = null;

		// build a regExp in case the cell value is a string
		if (_filterVal == null || (_filterVal != null && _filterVal.trim().equals("")) )
			_strPattern = Pattern.compile(".*");
		else
			_strPattern = Pattern.compile(_filterVal);

//		setColumnIndex(col);
		refresh(); // setColumnIndex() does refresh()
	}


	
	//---------------------------------------------------------------
	// private helper methods 
	//---------------------------------------------------------------
    private int compare(Object o1, Object o2)
	{
		// If both values are null return 0
		if (o1 == null && o2 == null)
		{
			return 0;
		}
		else if (o1 == null)
		{ // Define null less than everything.
			return -1;
		}
		else if (o2 == null)
		{
			return 1;
		}

		// make sure we use the collator for string compares
		if ((o1.getClass() == String.class) && (o2.getClass() == String.class))
		{
			return Collator.getInstance().compare((String) o1, (String) o2);
		}

		if ((o1.getClass().isInstance(o2)) && (o1 instanceof Comparable))
		{
			Comparable c1 = (Comparable) o1;
			return c1.compareTo(o2);
		}
		else if (o2.getClass().isInstance(o1) && (o2 instanceof Comparable))
		{
			Comparable c2 = (Comparable) o2;
			return -c2.compareTo(o1);
		}

		return Collator.getInstance().compare(o1.toString(), o2.toString());
	}

    
    /**
	 * return true if the row matches the filter, eg will be displayed
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
		if (!adapter.isTestable(_colId))
		{
			return false;
		}
		
		Object cellValue = getInputValue(row, _colId);

		// Create a new object that we use to compare
		if (_filterObj == null)
		{
			try
			{
				if (cellValue instanceof Number)
				{
					String className = (String) cellValue.getClass().getName();
					Class clazz = Class.forName(className);
					Constructor constr = clazz.getConstructor(new Class[]{String.class});
					_filterObj = constr.newInstance(new Object[]{_filterVal});
				}
				// For Timestamp, it work just as good using the String Matcher
				//else if (cellValue instanceof Timestamp)
				//{
				//	_filterObj = Timestamp.valueOf(_filterVal);
				//}
				else // make it into a String
				{
					_filterObj = _filterVal;
				}
			}
			catch (Exception e) 
			{
				// Problems creating a object...
				// So lets go to some fallback... probably a string...
				//e.printStackTrace();
				_logger.info("Problems create a Number of the string '"+_filterVal+"' for filtering, using String matching instead. "+e.getMessage());
				_filterObj = _filterVal;
			}
		}

		// If String, go and do reqexp
		// else USE Comparable on the objects it they implements it, 
		// otherwise do fallback on string matching
		
		if (_filterOp == FILTER_OP_EQ)
		{
			if (_filterObj instanceof String)
			{
				return _strPattern.matcher(cellValue.toString()).find();
			}
			return compare(cellValue, _filterObj) == 0;
		}
		else if (_filterOp == FILTER_OP_NE)
		{
			if (_filterObj instanceof String)
			{
				return !_strPattern.matcher(cellValue.toString()).find();
			}
			return compare(cellValue, _filterObj) != 0;
		}
		else if (_filterOp == FILTER_OP_LT)
		{
			return compare(cellValue, _filterObj) < 0;
		}
		else if (_filterOp == FILTER_OP_GT)
		{
			return compare(cellValue, _filterObj) > 0;
		}
		else
		{
			_logger.warn("Unknown _filterOp = "+_filterOp);
		}

		// If we get here, which never really happens, show the row...
		return true; 
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
					Object cellValue = getInputValue(i, _colId);
					_logger.trace("VISIBLE, row="+i+", _rowMapper["+i+"]="+_rowMapper[i]+", cellValue.class="+cellValue.getClass()+", cellValue='"+cellValue+"'.");
				}
			}
			else
			{
				if (_logger.isTraceEnabled())
				{
					Object cellValue = getInputValue(i, _colId);
					_logger.trace("   HIDE, row="+i+", _rowMapper["+i+"]="+_rowMapper[i]+", cellValue.class="+cellValue.getClass()+", cellValue='"+cellValue+"'.");
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
