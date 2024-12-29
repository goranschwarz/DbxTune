/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
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
package com.dbxtune.tools.sqlw.msg;

import java.util.Scanner;

import com.dbxtune.utils.ColorUtils;

public class JSkipSendSqlStatement
extends JAseMessage
{
	private static final long serialVersionUID = 1L;

	private static String createStr(String sql)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("-----------------------------------------------------------------------------------------------------------\n");
		sb.append("The below SQL Statement looks like it only consist of comments... This will NOT be sent to the server.     \n");
		sb.append("Note: If you still want to SEND it, the behaiviour can be changed under: Options -> Send empty SQL Batches \n");
		sb.append("-----------------------------------------------------------------------------------------------------------\n");
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

	public JSkipSendSqlStatement(String sql)
	{
		super(createStr(sql), sql);
		
		setForeground(ColorUtils.VERY_DARK_YELLOW);
	}
}
