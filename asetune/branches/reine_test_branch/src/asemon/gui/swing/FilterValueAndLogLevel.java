/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package asemon.gui.swing;

import java.util.ArrayList;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.apache.log4j.lf5.LogLevel;
import org.jdesktop.swingx.decorator.Filter;

/**
 * Filter data
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
public class FilterValueAndLogLevel 
extends Filter
{
	private static Logger _logger = Logger.getLogger(FilterValueAndLogLevel.class);

	/**
	 *  _rowMapper[row] = -1: Means: not shown, 
	 *  _rowMapper[row] = #:  Means: the int at the array position, points to the "slot" in the _visibleRows ArrayList 
	 *  Or you can say: arrayPos --points-to--> index in ArrayList where we keep the real data row 
	 */
	private int        _rowMapper[];

	/** holds data rows that should be visible in the view */
	private ArrayList  _visibleRows = new ArrayList();

	/** Is filtering on, or should all rows be visible */
	private boolean    _allRowsVisible = true;

	/** used by get/setTraceStr() which can be set in showInView and displayed in filter() */
	private String     _traceStr       = null;

	
	
	//---- Class level specifics
	private int     _levelColId        = -1;
	private int     _threadNameColId   = -1;
	private int     _classNameColId    = -1;
	private int     _messageColId      = -1;

	private int     _level             = 0;
	private String  _threadName        = null;
	private String  _className         = null;
	private String  _message           = null;

	private Pattern _threadNamePattern = null;
	private Pattern _classNamePattern  = null;
	private Pattern _messagePattern    = null;
	
	//---------------------------------------------------------------
	// Constructors 
	//---------------------------------------------------------------
	public FilterValueAndLogLevel()
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
		if (_logger.isDebugEnabled())
			System.out.println("DEBUG: Resetting filter, all data will be shown in table.");

		_allRowsVisible    = true;
		_level             = 0;
		_threadName        = null;
		_className         = null;
		_message           = null;
		
		_threadNamePattern = null;
		_classNamePattern  = null;
		_messagePattern    = null;

		refresh(); // This kicks off the "re-filtering" == show data.
	}

	
	
	/**
	 * Set what to filter on. matching rows will be kept/showed in table
	 * @param level  Is a bitmap of the levels to filter for (FATAL=1, ERROR=2, WARN=4, INFO=8, DEBUG=16, TRACE=32)
	 * @param threadName Values to be shown in the table
	 * @param className Values to be shown in the table 
	 * @param message Values to be shown in the table 
	 */
	public void setFilter(int level, String threadName, String className, String message)
	{
		if (_logger.isDebugEnabled())
			System.out.println("DEBUG: Setting filter: level="+level+", threadName='"+threadName+"', className='"+className+"', message='"+message+"'.");

		_allRowsVisible = false;
		_level          = level;
		_threadName     = threadName;
		_className      = className;
		_message        = message;

		// _threadName: build a regExp
		if (_threadName == null || (_threadName != null && _threadName.trim().equals("")) )
			_threadNamePattern = Pattern.compile(".*");
		else
			_threadNamePattern = Pattern.compile(_threadName);

		// _className: build a regExp 
		if (_className == null || (_className != null && _className.trim().equals("")) )
			_classNamePattern = Pattern.compile(".*");
		else
			_classNamePattern = Pattern.compile(_threadName);

		// _message: build a regExp 
		if (_message == null || (_message != null && _message.trim().equals("")) )
			_messagePattern = Pattern.compile(".*");
		else
			_messagePattern = Pattern.compile(_threadName);

		refresh();
	}

	public void setFilterColId(int levelColId, int threadNameColId, int classNameColId, int messageColId)
	{
		if (_logger.isDebugEnabled())
			System.out.println("DEBUG: Setting filter ColId's: level="+levelColId+", threadName="+threadNameColId+", className="+classNameColId+", message="+messageColId+".");

		_levelColId      = levelColId;
		_threadNameColId = threadNameColId;
		_classNameColId  = classNameColId;
		_messageColId    = messageColId;
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
//		if (!adapter.isTestable(_colId))
//		{
//			return false;
//		}
		
		Object levelCellVal      = getInputValue(row, _levelColId);
		Object threadNameCellVal = getInputValue(row, _threadNameColId);
		Object classNameCellVal  = getInputValue(row, _classNameColId);
		Object messageCellVal    = getInputValue(row, _messageColId);

		// Build up a TRACE string, which will be printed in filter() method.
		if ( _logger.isTraceEnabled() )
		{
			Object o;
			StringBuffer sb = new StringBuffer();

			o = levelCellVal;
			sb.append(" levelCellVal.class=");
			sb.append(o == null ? "null" : o.getClass().toString() );
			sb.append(", levelCellVal='"); sb.append(o);
			sb.append("',");

			o = threadNameCellVal;
			sb.append(" threadNameCellVal.class=");
			sb.append(o == null ? "null" : o.getClass().toString() );
			sb.append(", threadNameCellVal='"); sb.append(o);
			sb.append("',");

			o = classNameCellVal;
			sb.append(" classNameCellVal.class=");
			sb.append(o == null ? "null" : o.getClass().toString() );
			sb.append(", classNameCellVal='"); sb.append(o);
			sb.append("',");

			o = messageCellVal;
			sb.append(" messageCellVal.class=");
			sb.append(o == null ? "null" : o.getClass().toString() );
			sb.append(", messageCellVal='"); sb.append(o);
			sb.append("'.");

			setTraceStr(sb.toString());
		}

		// LEVEL
		if (_level == 0)
			return false;
		if (_level > 0)
		{
			if (levelCellVal instanceof LogLevel)
			{
				int level = 0;
				LogLevel ll = (LogLevel)levelCellVal;
				if      (LogLevel.INFO .equals(ll)) level |= 8;
				else if (LogLevel.DEBUG.equals(ll)) level |= 16;
//				else if (LogLevel.TRACE.equals(ll)) level |= 32;
				else if (ll.getLabel().equals("TRACE")) level |= 32;
				else if (LogLevel.WARN .equals(ll)) level |= 4;
				else if (LogLevel.ERROR.equals(ll)) level |= 2;
				else if (LogLevel.FATAL.equals(ll)) level |= 1;
				
				if ((_level & level) == 0) // this means NOT match
					return false;
			}
		}

		// THREAD NAME
		if (threadNameCellVal != null && _threadName != null)
		{
			if ( ! _threadNamePattern.matcher(threadNameCellVal.toString()).find() )
				return false;
		}
		// CLASS NAME
		if (classNameCellVal != null && _className != null)
		{
			if ( ! _classNamePattern.matcher(classNameCellVal.toString()).find() )
				return false;
		}
		// MESSAGE
		if (messageCellVal != null && _message != null)
		{
			if ( ! _messagePattern.matcher(messageCellVal.toString()).find() )
				return false;
		}
		
		// If we get here the row will be displayed
		return true; 
	}

	
	public void setTraceStr(String str)
	{
		_traceStr = str;
	}
	public String getTraceStr()
	{
		return (_traceStr == null) ? "" : _traceStr;
	}
	
	//---------------------------------------------------------------
	// BEGIN: implement abstract org.jdesktop.swing.decorator.Filter methods 
	//---------------------------------------------------------------

	protected void init()
	{
		if (_logger.isTraceEnabled())
			System.out.println("TRACE: init()");

		//_visibleRows = new ArrayList();
	}

	protected void reset()
	{
		if (_logger.isTraceEnabled())
			System.out.println("TRACE: reset()");

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
		if (_logger.isTraceEnabled())
			System.out.println("TRACE: filter()");

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
					System.out.println("TRACE: VISIBLE, row="+i+", _rowMapper["+i+"]="+_rowMapper[i]+"."+getTraceStr());
			}
			else
			{
				if (_logger.isTraceEnabled())
					System.out.println("TRACE:    HIDE, row="+i+", _rowMapper["+i+"]="+_rowMapper[i]+"."+getTraceStr());
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
