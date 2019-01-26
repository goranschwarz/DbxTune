/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
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

import java.sql.SQLWarning;

import com.asetune.sql.pipe.PipeCommand;
import com.asetune.utils.ColorUtils;

public class JBcpWarning
extends JAseMessage
{
	private static final long serialVersionUID = 1L;

	private static String createStr(SQLWarning sqlw, PipeCommand pipeCmd)
	{
		String mainText = "The BCP command '"+pipeCmd.getCmd().getCmdStr()+"' had some Warning messages.\n";
		String sqlWarningsText = "";

		int w = 0;
		while (sqlw != null)
		{
			String wmsg = sqlw.getMessage();
			
			sqlWarningsText += "SQLWarning("+w+"): " + wmsg;
			if ( ! sqlWarningsText.endsWith("\n") )
				sqlWarningsText += "\n";
				
			sqlw = sqlw.getNextWarning();
			if (w == 0 && sqlw == null)
				break;
			w++;
		}
		if (w > 1) // If we had a Warning Chain... add the chain, else "reset" the warnings...
			sqlWarningsText = "Below is the full SQLWarning chain, there are "+w+" Warnings:\n" + sqlWarningsText;

		return mainText + sqlWarningsText;
	}

	public JBcpWarning(SQLWarning bcpSqlWarning, PipeCommand pipeCmd, String sql)
	{
		super(createStr(bcpSqlWarning, pipeCmd), sql);

		setForeground(ColorUtils.VERY_DARK_YELLOW);
	}
}
