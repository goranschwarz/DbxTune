package com.asetune.tools.sqlw.msg;

import javax.swing.JComponent;

import com.asetune.gui.ResultSetTableModel;

public class JTableResultSet
extends JComponent
{
	private static final long serialVersionUID = 1L;

	private ResultSetTableModel _tm = null;

	public JTableResultSet(final ResultSetTableModel rstm)
	{
		_tm = rstm;
	}
	
	public ResultSetTableModel getResultSetTableModel()
	{
		return _tm;
	}

	public int getRowCount()
	{
		return _tm.getRowCount();
	}
}
