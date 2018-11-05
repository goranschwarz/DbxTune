package com.asetune.tools.sqlw.msg;

import com.asetune.utils.ColorUtils;

public class JAseLimitedResultSetTop
extends JAseMessage
{
	private static final long serialVersionUID = 1L;

	public JAseLimitedResultSetTop(int numberOfRows, String originSql)
	{
		super("Reading the ResultSet was stopped after "+numberOfRows+" rows.", originSql);
//		init();

		setForeground(ColorUtils.DARK_RED);
	}
}
