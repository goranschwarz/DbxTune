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

import com.dbxtune.sql.pipe.PipeCommand;
import com.dbxtune.utils.ColorUtils;

public class JToFileMessage
extends JAseMessage
{
	private static final long serialVersionUID = 1L;

	private static String createStr(String message, PipeCommand pipeCmd)
	{
		String mainText = "The TOFILE command '"+pipeCmd.getCmd().getCmdStr()+"' Completed successfully.\n";

		return mainText + message;
	}

	public JToFileMessage(String message, PipeCommand pipeCmd, String sql)
	{
		super(createStr(message, pipeCmd), sql);

		setForeground(ColorUtils.VERY_DARK_GREEN);
	}
}