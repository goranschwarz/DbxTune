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

import java.util.regex.Pattern;

import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.table.TableModel;

import org.apache.log4j.Logger;
import org.apache.log4j.lf5.LogLevel;

/**
 * Filter data
 * 
 * @author gorans
 */
public class RowFilterValueAndLogLevel extends RowFilter<TableModel, Integer>
{
	private static Logger _logger = Logger.getLogger(RowFilterValueAndLogLevel.class);

	/** Is this filtering enabled/disabled */
	private boolean    _filterIsActive = false;

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

	/** the JTable, which this is connected to */
	private JTable     _table = null;

	//---------------------------------------------------------------
	// Constructors 
	//---------------------------------------------------------------
	public RowFilterValueAndLogLevel(JTable table)
	{
        super();
        if (table == null)
        	throw new IllegalArgumentException("table can't be null.");
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
		if (_logger.isDebugEnabled())
			_logger.debug("DEBUG: Resetting filter, all data will be shown in table.");

		_filterIsActive    = false;
		_level             = 0;
		_threadName        = null;
		_className         = null;
		_message           = null;
		
		_threadNamePattern = null;
		_classNamePattern  = null;
		_messagePattern    = null;

		// This kicks off the "re-filtering" == show data.
		if (_table.getRowSorter() != null)
			_table.getRowSorter().allRowsChanged();
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
			_logger.debug("DEBUG: Setting filter: level="+level+", threadName='"+threadName+"', className='"+className+"', message='"+message+"'.");

		_filterIsActive = true;
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
			_classNamePattern = Pattern.compile(_className);

		// _message: build a regExp 
		if (_message == null || (_message != null && _message.trim().equals("")) )
			_messagePattern = Pattern.compile(".*");
		else
			_messagePattern = Pattern.compile(_message);

		// This kicks off the "re-filtering" == show data.
		if (_table.getRowSorter() != null)
			_table.getRowSorter().allRowsChanged();
	}

	public void setFilterColId(int levelColId, int threadNameColId, int classNameColId, int messageColId)
	{
		if (_logger.isDebugEnabled())
			_logger.debug("DEBUG: Setting filter ColId's: level="+levelColId+", threadName="+threadNameColId+", className="+classNameColId+", message="+messageColId+".");

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
	private boolean showInView(Entry<? extends TableModel, ? extends Integer> entry)
	{
//		Object levelCellVal      = getInputValue(row, _levelColId);
//		Object threadNameCellVal = getInputValue(row, _threadNameColId);
//		Object classNameCellVal  = getInputValue(row, _classNameColId);
//		Object messageCellVal    = getInputValue(row, _messageColId);
		Object levelCellVal      = entry.getValue(_levelColId);
		Object threadNameCellVal = entry.getValue(_threadNameColId);
		Object classNameCellVal  = entry.getValue(_classNameColId);
		Object messageCellVal    = entry.getValue(_messageColId);

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
	// BEGIN: implement javax.swing.RowFilter methods 
	//---------------------------------------------------------------
	@Override
	public boolean include(Entry<? extends TableModel, ? extends Integer> entry)
	{
		// Show everything if the filer is  NOT Active, return TRUE to show rows in view
		if ( ! _filterIsActive )
			return true;

		boolean include = showInView(entry);
		if (_logger.isTraceEnabled())
		{
			if ( include )
				_logger.trace("TRACE: VISIBLE, row="+entry.getIdentifier()+", "+getTraceStr());
			else
				_logger.trace("TRACE:    HIDE, row="+entry.getIdentifier()+", "+getTraceStr());
		}
		
		return include;
	}
	//---------------------------------------------------------------
	// END: implement javax.swing.RowFilter methods 
	//---------------------------------------------------------------

//	static
//	{
//		_logger.setLevel(Level.TRACE);
//	}
}
