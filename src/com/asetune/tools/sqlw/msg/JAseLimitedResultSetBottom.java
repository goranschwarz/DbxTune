package com.asetune.tools.sqlw.msg;

import com.asetune.utils.ColorUtils;

public class JAseLimitedResultSetBottom
extends JAseMessage
{
	private static final long serialVersionUID = 1L;

	public JAseLimitedResultSetBottom(int numberOfRows, int bottomLimit, String originSql)
	{
		super("Discarded "+numberOfRows+" first rows from the ResultSet, and only keeping the last "+bottomLimit+" records.", originSql);
//		init();

		setForeground(ColorUtils.DARK_RED);
	}
}
