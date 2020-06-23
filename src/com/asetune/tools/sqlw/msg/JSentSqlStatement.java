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

import java.util.Scanner;

import com.asetune.utils.ColorUtils;

public class JSentSqlStatement
extends JAseMessage
{
	private static final long serialVersionUID = 1L;

	@SuppressWarnings("unused")
	private int _scriptRow;

	private static String createStr(String sql)
	{
		StringBuilder sb = new StringBuilder();
		Scanner scanner = new Scanner(sql);
		int row = 0;
		while (scanner.hasNextLine()) 
		{
			String str = scanner.nextLine();
			row++;
			sb.append(row).append("> ").append(str);
			if (scanner.hasNextLine())
				sb.append("\n");
		}
		return sb.toString();
	}

	public JSentSqlStatement(String sql, int scriptRow)
	{
		super(createStr(sql), sql);
		_scriptRow = scriptRow;
		
		setForeground(ColorUtils.VERY_DARK_GREEN);
	}
}
