package com.asetune.tools.sqlw.msg;

import com.asetune.utils.ColorUtils;

public class JLoadfileMessage
extends JAseMessage
{
	private static final long serialVersionUID = 1L;
	
	public JLoadfileMessage(String message, String originSql)
	{
		super("LOADFILE: "+message, originSql);

		setForeground(ColorUtils.VERY_DARK_BLUE);
	}
}
