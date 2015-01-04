package com.asetune.tools.sqlw.msg;

import com.asetune.utils.ColorUtils;

public class JAseLimitedResultSet
extends JAseMessage
{
	private static final long serialVersionUID = 1L;

	public JAseLimitedResultSet(int numberOfRows, String originSql)
	{
		super("Reading the ResultSet was stopped after "+numberOfRows+" rows.", originSql);
//		init();

		setForeground(ColorUtils.DARK_RED);
	}
}
