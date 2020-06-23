/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
 * 
 * This file is part of DbxTune
 * DbxTune is a family of sub-products *Tune, hence the Dbx
 * Here are some of the tools: AseTune, IqTune, RsTune, RaxTune, HanaTune, 
 *          SqlServerTune, PostgresTune, MySqlTune, MariaDbTune, Db2Tune, ...
 * 
 * DbxTune is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
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
