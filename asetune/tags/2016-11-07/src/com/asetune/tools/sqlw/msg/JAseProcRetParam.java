package com.asetune.tools.sqlw.msg;

import com.asetune.gui.ResultSetTableModel;
import com.asetune.utils.ColorUtils;

public class JAseProcRetParam
extends JAseMessage
{
	private static final long serialVersionUID = 1L;

	private int    _pos;
	private Object _val;
	private int    _sqlType; // java.sql.Types

	private static String toValue(Object val)
	{
		if (val == null)
			return ResultSetTableModel.DEFAULT_NULL_REPLACE; // ResultSetTableModel.NULL_REPLACE;
		
		if (val instanceof String)
			return "'" + val + "'";

		return val + "";
	}
	public JAseProcRetParam(final int pos, final Object val, final int type, String originSql)
	{
		super("RPC Return parameter: pos="+pos+", value="+toValue(val), originSql);
		_pos     = pos;
		_val     = val;
		_sqlType = type;
//		init();
		
		setForeground(ColorUtils.VERY_DARK_GREEN);
	}
	
	public Object getValue()
	{
		return _val;
	}

	public int getPosition()
	{
		return _pos;
	}

	public int getType()
	{
		return _sqlType;
	}
}
