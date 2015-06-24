package com.asetune.tools.sqlw.msg;

import com.asetune.utils.ColorUtils;

public class JAseRowCount
extends JAseMessage
{
	private static final long serialVersionUID = 1L;

	private int _rowCount;

	public JAseRowCount(final int rowCount, String originSql)
	{
		super("(" + rowCount + " rows affected)", originSql);
		_rowCount = rowCount;
//		init();
		
		setForeground(ColorUtils.VERY_DARK_GREEN);
	}
	
	public int getRowCount()
	{
		return _rowCount;
	}
}
