package com.asetune.tools.sqlw.msg;

import com.asetune.utils.ColorUtils;

public class JAseProcRetCode
extends JAseMessage
{
	private static final long serialVersionUID = 1L;

	private int _returnCode;

	public JAseProcRetCode(final int returnCode, String originSql)
	{
		super("(return status = "+returnCode+")", originSql);
		_returnCode = returnCode;
//		init();
		
		setForeground(ColorUtils.VERY_DARK_GREEN);
	}
	
	public int getReturnCode()
	{
		return _returnCode;
	}
}
