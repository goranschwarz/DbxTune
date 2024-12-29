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

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

import com.dbxtune.utils.ColorUtils;

public class JOracleMessage
extends JAseMessage
{
	private static final long serialVersionUID = 1L;
	
	private static String createMsg(String owner, String name, String type, int sequence, int line, int position, String text, String attribute, int message_number)
	{
		return "Oracle SHOW ERRORS\n"
				+ "Type:           " + type + "\n"
				+ "Owner:          " + owner + "\n"
				+ "Name:           " + name + "\n"
				+ "sequence:       " + sequence + "\n"
				+ "line:           " + line + "\n"
				+ "position:       " + position + "\n"
				+ "attribute:      " + attribute + "\n"
				+ "message_number: " + message_number + "\n"
				+ text
				;
	}

	public JOracleMessage(String owner, String name, String type, int sequence, int line, int position, String text, String attribute, int message_number, 
			int scriptRow, String originSql, RSyntaxTextArea textArea)
	{
		super(createMsg(owner, name, type, sequence, line, position, text, attribute, message_number), 
				message_number, text, 16, scriptRow, position, originSql, textArea);

		setForeground(ColorUtils.DARK_RED);
	}
}
