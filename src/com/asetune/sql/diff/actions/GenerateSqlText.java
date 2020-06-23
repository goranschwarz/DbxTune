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
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.asetune.sql.diff.actions;

import java.sql.SQLException;
import java.util.List;

import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.diff.DiffContext;
import com.asetune.sql.diff.DiffSink;
import com.asetune.sql.diff.DiffContext.DiffSide;

public class GenerateSqlText
extends GenerateSqlAbstract
{
	public GenerateSqlText(DiffContext context, DbxConnection conn)
	{
		this(context.getSink(), conn);
	}
	
	public GenerateSqlText(DiffSink sink, DbxConnection conn)
	{
		super(sink, conn);
	}

	public List<String> getSql(DiffSide side, String goString)
	throws SQLException
	{
		return generateSqlFix(side, goString);
	}
}
