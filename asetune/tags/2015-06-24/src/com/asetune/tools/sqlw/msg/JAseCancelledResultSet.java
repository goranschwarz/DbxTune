package com.asetune.tools.sqlw.msg;

import com.asetune.utils.ColorUtils;

public class JAseCancelledResultSet
extends JAseMessage
{
	private static final long serialVersionUID = 1L;

	public JAseCancelledResultSet(String originSql)
	{
		super("SQL was cancelled while reading the ResultSet", originSql);
//		init();
		
		setForeground(ColorUtils.DARK_RED);
	}
}
