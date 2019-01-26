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

import com.asetune.utils.ColorUtils;

public class JAseLimitedResultSetBottom
extends JAseMessage
{
	private static final long serialVersionUID = 1L;

	public JAseLimitedResultSetBottom(int numberOfRows, int bottomLimit, String originSql)
	{
		super("Discarded "+numberOfRows+" first rows from the ResultSet, and only keeping the last "+bottomLimit+" records.", originSql);
//		init();

		setForeground(ColorUtils.DARK_RED);
	}
}
