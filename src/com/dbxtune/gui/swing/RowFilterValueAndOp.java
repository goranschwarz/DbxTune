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
package com.dbxtune.gui.swing;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.text.Collator;
import java.util.regex.Pattern;

import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.table.TableModel;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.cm.CountersModel;

/**
 * Filter data using EQual, NotEqual, GreaterThen or LessThen.
 * 
 * @author gorans
 */
public class RowFilterValueAndOp 
extends RowFilter<TableModel, Integer>
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public static final int FILTER_OP_EQ = 0;  // EQual
	public static final int FILTER_OP_NE = 1;  // Not Equal
	public static final int FILTER_OP_GT = 2;  // Greater Then
	public static final int FILTER_OP_LT = 3;  // Less Then

	/** Operator to use when filtering data: QE, NE, LT, GT */
	private int        _filterOp    = -1;

	/** String representation of what the _filterOp is applied on */
	private String     _filterVal   = null;

	/** Column id/pos in the table that we apply the operation on */	
	private int        _colId       = -1;

	/** Is this filtering enabled/disabled */
	private boolean    _filterIsActive = false;

	/** In Case, the DataCell in the table is String, apply this regexp instead */
	private Pattern    _strPattern = null;

	/** In Case, the DataCell in the table is NO String, we need to 
	 * create a object of the same type as the object DataCell. Then use
	 * compare() to check for operator (EQ NE LT GT).
	 * For the moment this is only used for Number objects. 
	 */
	private Object     _filterObj  = null;

	/** the JTable, which this is connected to */
	private JTable     _table = null;

	//---------------------------------------------------------------
	// Constructors 
	//---------------------------------------------------------------
	public RowFilterValueAndOp(JTable table)
	{
		super();
		_table = table;
	}

	
//	//---------------------------------------------------------------
//	// Public methods 
//	//---------------------------------------------------------------
	/**
	 * Turn off filtering, all rows will be visible
	 */
	public void resetFilter()
	{
		_logger.debug("Resetting filter, all data will be shown in table.");

		_filterIsActive = false;
		_filterOp       = -1;
		_colId          = -1;
		_filterVal      = null;
		_filterObj      = null;
		_strPattern     = null;

		// This kicks off the "re-filtering" == show data.
		if (_table.getRowSorter() != null)
			_table.getRowSorter().allRowsChanged();
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

		_filterIsActive = true;
		_filterOp       = op;
		_colId          = col;
		_filterVal      = val;

		// used to compare NON String values
		_filterObj = null;

		// build a regExp in case the cell value is a string
		setStrPattern();

		// This kicks off the "re-filtering" == show data.
		if (_table.getRowSorter() != null)
			_table.getRowSorter().allRowsChanged();
	}


	
	//---------------------------------------------------------------
	// private helper methods 
	//---------------------------------------------------------------
//	@SuppressWarnings("unchecked")
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

	private void setStrPattern()
	{
		if (_filterVal == null || (_filterVal != null && _filterVal.trim().equals("")) )
			_strPattern = Pattern.compile(".*");
		else
			_strPattern = Pattern.compile(_filterVal);
	}
    
    /**
	 * return true if the row matches the filter, eg will be displayed
	 * return false if value should be displayed, meaning it matches the filter
	 */
	private boolean showInView(Entry<? extends TableModel, ? extends Integer> entry)
	{
		Object cellValue = entry.getValue(_colId);
		
		// Handle NULL values in model.
		if (cellValue == null)
		{
			TableModel tm = entry.getModel();
			if (tm instanceof CountersModel)
			{
				// To get the correct "NULL" display value, we need to do the following...
				//cellValue = ((CountersModel)tm).getTabPanel().getDataTable().getNullValueDisplay();
				// But lets sheet a bit... This will NOT work if you change the NULL Display Value using property 'GTable.replace.null.with=SomeOtherNullRepresantation' 
				cellValue = GTable.DEFAULT_NULL_REPLACE;
			}
			else
			{
				// For all other table models, simply do NOT show null values when filtering
				return false;
			}
		}

		// Create a new object that we use to compare
		if (_filterObj == null)
		{
			try
			{
				if (cellValue instanceof Number)
				{
					String className = (String) cellValue.getClass().getName();
					Class<?> clazz = Class.forName(className);
					Constructor<?> constr = clazz.getConstructor(new Class[]{String.class});
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
				if (_strPattern == null)
					setStrPattern();
				return _strPattern.matcher(cellValue.toString()).find();
			}
			return compare(cellValue, _filterObj) == 0;
		}
		else if (_filterOp == FILTER_OP_NE)
		{
			if (_filterObj instanceof String)
			{
				if (_strPattern == null)
					setStrPattern();
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
	// BEGIN: implement javax.swing.RowFilter methods 
	//---------------------------------------------------------------
	@Override
	public boolean include(Entry<? extends TableModel, ? extends Integer> entry)
	{
		// Show everything if the filer is  NOT Active, return TRUE to show rows in view
		if ( ! _filterIsActive )
			return true;

		return showInView(entry);
	}
	//---------------------------------------------------------------
	// END: implement javax.swing.RowFilter methods 
	//---------------------------------------------------------------





}
