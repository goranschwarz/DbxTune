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
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.asetune.sql.diff;

import java.awt.Component;
import java.awt.Window;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.asetune.gui.ResultSetTableModel;
import com.asetune.sql.SqlProgressDialog;
import com.asetune.sql.conn.ConnectionProp;
import com.asetune.sql.diff.comp.ColComparator;
import com.asetune.sql.diff.comp.PkComparator;
import com.asetune.tools.sqlw.msg.Message;
import com.asetune.tools.sqlw.msg.Message.Severity;
import com.asetune.utils.StringUtil;

public class DiffContext
{
	private int     _maxDiffCount = Integer.MAX_VALUE;
	private boolean _isCaseInSensitive = false;
	private int     _debugLevel = 0;
	private boolean _isToStdoutEnabled = false;
	protected boolean _noPk_generateColumns = true;

	private List<String> _pkColumns;
	private List<String> _pkGeneratedColumns;
	
	private DiffTable _leftDt;
	private DiffTable _rightDt;

	private DiffSink _sink;
	
	private ColComparator[] _colComparator;
	private PkComparator  _pkComparator;

	protected List<Message> _msgList = new ArrayList<>();

	protected Component _guiOwner;
	private SqlProgressDialog _progressDialog;
	
	public enum DiffSide
	{
		LEFT, 
		RIGHT
	};
	
//	public DiffContext(List<String> pkList, ResultSet left, ResultSet right)
//	throws DiffException, SQLException
//	{
//		this(
//			new DiffTable(DiffSide.LEFT,  pkList, left), 
//			new DiffTable(DiffSide.RIGHT, pkList, right), 
//			Integer.MAX_VALUE, false);
//	}
//
//	public DiffContext(DiffTable left, DiffTable right)
//	{
//		this(left, right, Integer.MAX_VALUE, false);
//	}
//
//	public DiffContext(DiffTable left, DiffTable right, int maxDiffCount, boolean caseInSensitive)
//	{
//		_leftDt  = left;
//		_rightDt = right;
//
//		_maxDiffCount      = maxDiffCount;
//		_isCaseInSensitive = caseInSensitive;
//		
//		_colComparator = new ColComparator(_isCaseInSensitive);
//
//		_sink = new DiffSink(_leftDt, _rightDt, maxDiffCount);
//
////		_pkComparator = new PkComparator(false, "id");
//		_pkComparator = new PkComparator(_isCaseInSensitive, _leftDt.getPkColumnNames());
//	}
	
	public DiffContext()
	{
	}

	public DiffContext(List<String> pkList)
	{
		_pkColumns = pkList;
	}

	public DiffContext(int maxDiffCount, boolean caseInSensitive)
	{
		_maxDiffCount      = maxDiffCount;
		_isCaseInSensitive = caseInSensitive;
	}

	public void setDiffTable(DiffSide side, DiffTable dt)
	{
		if (DiffSide.LEFT .equals(side)) _leftDt   = dt;
		if (DiffSide.RIGHT.equals(side)) _rightDt  = dt;
	}

	public void setDiffTable(DiffSide side, ResultSet rs, ConnectionProp connProps) 
	throws DiffException, SQLException
	{
		DiffTable dt = new DiffTable(side, this, rs, connProps);
		
		if (DiffSide.LEFT .equals(side)) _leftDt   = dt;
		if (DiffSide.RIGHT.equals(side)) _rightDt  = dt;
	}
	

	public void setGuiOwner(Component guiOwner)
	{
		_guiOwner = guiOwner;
	}

	public Component getGuiOwner()
	{
		return _guiOwner;
	}
	public Window getGuiOwnerAsWindow()
	{
		Component tmpGuiOwner = getGuiOwner();
		Window guiOwner = null;
		if (tmpGuiOwner != null && tmpGuiOwner instanceof Window)
			guiOwner = (Window) tmpGuiOwner;

		return guiOwner;
	}

	public void setProgressDialog(SqlProgressDialog progressDialog)
	{
		_progressDialog = progressDialog;
	}
	
	public SqlProgressDialog getProgressDialog()
	{
		return _progressDialog;
	}
	
	public void setPkColumns(List<String> pkList)
	{
		_pkColumns = pkList;
	}

	public List<String> getPkColumns()
	{
		return _pkColumns;
	}

	public void setPkGeneratedColumns(List<String> pkList)
	{
		_pkGeneratedColumns = pkList;
	}

	public List<String> getPkGeneratedColumns()
	{
		return _pkGeneratedColumns;
	}
	
	public int getPkColumnJdbcDataType(String colName)
	{
		List<String>  colNames         = getLeftDt().getColumnNames();
		List<Integer> colJdbcDataTypes = getLeftDt().getJdbcDataTypes();
		
//		int pos = colNames.indexOf(colName);
		int pos = StringUtil.indexOfIgnoreCase(colNames, colName);

		return colJdbcDataTypes.get(pos);
	}

	protected ColComparator getColumnComparator(int colId)
	{
		return _colComparator[colId];
	}

	public PkComparator getPkComparator()
	{
		return _pkComparator;
	}
	
	public DiffTable getLeftDt()  { return _leftDt; }
	public DiffTable getRightDt() { return _rightDt; }
	public DiffSink  getSink()    { return _sink; }

	public int getMaxDiffCount()
	{
		return _maxDiffCount;
	}
	

	/** Sets if we should compare strings as Case In Sensitive */
	public void    setCaseInSensitive(boolean val) { _isCaseInSensitive = val; }
	public boolean isCaseInSensitive()             { return _isCaseInSensitive; }


	/**
	 * Validate the all the inputs are correct...
	 * 
	 * @throws DiffException
	 */
	public void validate()
	throws DiffException
	{
		//---------------------------------------------------
		// Check Column Count
		//---------------------------------------------------
		if (_leftDt.getColumnCount() != _rightDt.getColumnCount())
			throw new DiffException("Number of columns on the left and right side is not equal. Left table '"+_leftDt.getFullTableName()+"', Right table '"+_rightDt.getFullTableName()+"'.\n" 
					+ getErrorColumnInfo(getLeftDt()) + "\n" 
					+ getErrorColumnInfo(getRightDt()) );
		
		if (isTraceEnabled())
			addTraceMessage("left_ColCount="+_leftDt.getColumnCount()+", left_ColCount="+_rightDt.getColumnCount());
		
		// If no PK was specified, then get it from the LEFT DiffTable
		if (getPkColumns() == null)
			setPkColumns(getLeftDt().getPkColumnNames());

		//---------------------------------------------------
		// Check if PK Exists
		//---------------------------------------------------
		// This is already done in DiffTable constructor

		//---------------------------------------------------
		// Check if PK columns are SUPPORTED
		//---------------------------------------------------
		for (String pkColName : getLeftDt().getPkColumnNames())
			if (getLeftDt().isJdbcDataTypeSupportedInPkColumnThrow(pkColName))
				/* ABOVE THROWS Exception on problems */;

		for (String pkColName : getRightDt().getPkColumnNames())
			if (getRightDt().isJdbcDataTypeSupportedInPkColumnThrow(pkColName))
				/* ABOVE THROWS Exception on problems */;

		//---------------------------------------------------
		// Check if COLUMNS are SUPPORTED
		//---------------------------------------------------
		for (int c=0; c<getLeftDt().getColumnCount(); c++)
			if (getLeftDt().isJdbcDataTypeSupportedInDiffColumnThrow(c))
				/* ABOVE THROWS Exception on problems */;

		for (int c=0; c<getRightDt().getColumnCount(); c++)
			if (getRightDt().isJdbcDataTypeSupportedInDiffColumnThrow(c))
				/* ABOVE THROWS Exception on problems */;
		
		
		//---------------------------------------------------
		// Everything looks OK, initialize some "stuff"
		//---------------------------------------------------

		// Create Column Comparators
		_colComparator = new ColComparator[_leftDt.getColumnCount()];
		for (int c=0; c<getLeftDt().getColumnCount(); c++)
			_colComparator[c] = new ColComparator(this, getLeftDt().getColumnNames().get(c), getLeftDt().getJdbcDataTypes().get(c));

		// Create PK Column Comparators
		_pkComparator = new PkComparator(this);

		// Create SINK
		_sink = new DiffSink(this, getLeftDt(), getRightDt(), getMaxDiffCount());
	}
	
	private String getErrorColumnInfo(DiffTable dt)
	{
		String colDesc = "";
		for (int c=0; c<dt.getColumnCount(); c++)
			colDesc += "col["+c+"]={name='" + dt.getColumnNames().get(c) + "', jdbcDataType=" + ResultSetTableModel.getColumnJavaSqlTypeName(dt.getJdbcDataTypes().get(c)) + "}, ";
		
		colDesc = colDesc.substring(0, colDesc.length() - 2);
		
		return dt.getDiffSide() + ": FullTableName='" + dt.getFullTableName() + "', ColumnCount=" + dt.getColumnCount() + ", " + colDesc;
	}

	
	/**
	 * Wrapper method to execute diff and get diff-count
	 * <p>
	 * Creates a DiffEngine and call diff()... then return diff count (from the sink)
	 * 
	 * @return
	 * @throws Exception
	 */
	public int doDiff() 
	throws Exception
	{
		DiffEngine engine = new DiffEngine(this);
		engine.diff();

//		clone();

		if (isDebugEnabled())
		{
			String msg = "doDiff:"
					+ " Total Diff Count = "    + getSink().getCurrentDiffCount() 
					+ ", Left Missing Rows = "  + getSink().getLeftMissingRows() .size()
					+ ", Right Missing Rows = " + getSink().getRightMissingRows().size()
					+ ", Column Diff Rows = "   + getSink().getDiffColumnValues().size()
					;
			addDebugMessage(msg);
		}

		return getSink().getCurrentDiffCount();
	}

//	public void close()
//	{
//		if (getLeftDt() != null)
//			getLeftDt().close();
//
//		if (getRightDt() != null)
//			getRightDt().close();
//	}


	public void setMessageDebugLevel(int level) { _debugLevel        = level; }
	public void setMessageToStdout(boolean val) { _isToStdoutEnabled = val; }

	public boolean isStdoutOutEnabled() { return _isToStdoutEnabled; }
	public boolean isTraceEnabled()   { return _debugLevel >= 2; }
	public boolean isDebugEnabled()   { return _debugLevel >= 1; }
	public boolean isInfoEnabled()    { return true; }
	public boolean isWarningEnabled() { return true; }
	public boolean isErrorEnabled()   { return true; }

	public void addTraceMessage  (String msg) { if (isTraceEnabled  ()) _msgList.add(new Message(Severity.TRACE  , msg)); if (isStdoutOutEnabled()) System.out.println("TRACE: "   + msg); }
	public void addDebugMessage  (String msg) { if (isDebugEnabled  ()) _msgList.add(new Message(Severity.DEBUG  , msg)); if (isStdoutOutEnabled()) System.out.println("DEBUG: "   + msg); }
	public void addInfoMessage   (String msg) { if (isInfoEnabled   ()) _msgList.add(new Message(Severity.INFO   , msg)); if (isStdoutOutEnabled()) System.out.println("INFO: "    + msg); }
	public void addWarningMessage(String msg) { if (isWarningEnabled()) _msgList.add(new Message(Severity.WARNING, msg)); if (isStdoutOutEnabled()) System.out.println("WARNING: " + msg); }
	public void addErrorMessage  (String msg) { if (isErrorEnabled  ()) _msgList.add(new Message(Severity.ERROR  , msg)); if (isStdoutOutEnabled()) System.out.println("ERROR: "   + msg); }

	public boolean hasMessages()
	{
		if (_msgList == null)
			return false;

		return _msgList.size() > 0;
	}
	
	public List<Message> getMessages()
	{
		if (_msgList == null)
			return Collections.emptyList();

		return _msgList;
	}

	public void clearMessages()
	{
		_msgList = new ArrayList<>();
	}
}
