/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
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
package com.asetune.gui.swing;

import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.table.TableModel;

import org.apache.log4j.Logger;

/**
 * Filter data out (do not show) rows where all specified columns has a 0 value
 *
 * @author gorans
 */
public class RowFilterDiffCounterIsZero 
extends RowFilter<TableModel, Integer>
{
	private static Logger _logger = Logger.getLogger(RowFilterDiffCounterIsZero.class);

	/** Columns id/pos in the table that we apply the check on */
	private int        _colIds[]     = null;

	/** Is filtering on, or should all rows be visible */
	private boolean    _filterIsActive = false;


	/** the JTable, which this is connected to */
	private JTable     _table = null;

	//---------------------------------------------------------------
	// Constructors 
	//---------------------------------------------------------------
	public RowFilterDiffCounterIsZero(JTable table)
	{
        super();
        _table = table;
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

		_filterIsActive = false;
		_colIds         = null;

		// This kicks off the "re-filtering"
		if (_table.getRowSorter() != null)
			_table.getRowSorter().allRowsChanged();
	}



	/** Helper to get a column position in the table */
	private int getColumnPos(JTable table, String colname)
	{
		TableModel tm = table.getModel();
		if (tm == null)
			return -1;

		for (int c=0; c<tm.getColumnCount(); c++)
		{
			if ( colname.equals( tm.getColumnName(c) ) )
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

		// Do not set filters if the table is "empty"
		if (table.getModel().getColumnCount() == 0)
			return;

		int[] cols = new int[colNames.length];
		for (int i=0; i<cols.length; i++)
		{
			cols[i] = getColumnPos(table, colNames[i]);
			if (cols[i] == -1)
			{
				_logger.debug("Can't find column name '"+colNames[i]+"' in JTable when setting filter.");
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

		_filterIsActive = true;
		_colIds         = cols;

		// This kicks off the "re-filtering"
		if (_table.getRowSorter() != null)
			_table.getRowSorter().allRowsChanged();
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
	private boolean showInView(Entry<? extends TableModel, ? extends Integer> entry)
	{
		boolean showRow = false;
		for (int c=0; c<_colIds.length; c++)
		{
			// if we have negative column positions, skip them.
			if (_colIds[c] < 0)
				continue;

//			Object cellValue = getInputValue(row, _colIds[c]);
			Object cellValue = entry.getValue(_colIds[c]);

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
