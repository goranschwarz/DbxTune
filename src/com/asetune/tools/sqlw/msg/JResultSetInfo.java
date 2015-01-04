package com.asetune.tools.sqlw.msg;

import com.asetune.gui.ResultSetTableModel;
import com.asetune.utils.ColorUtils;

public class JResultSetInfo
extends JAseMessage
{
	private static final long serialVersionUID = 1L;

	@SuppressWarnings("unused")
	private int _scriptRow;

	private static String createStr(ResultSetTableModel rstm)
	{
		return rstm.getResultSetInfo();
	}

	public JResultSetInfo(ResultSetTableModel rstm, String sql, int scriptRow)
	{
		super(createStr(rstm), sql);
		_scriptRow = scriptRow;
		
		setForeground(ColorUtils.VERY_DARK_GREEN);
	}
}
