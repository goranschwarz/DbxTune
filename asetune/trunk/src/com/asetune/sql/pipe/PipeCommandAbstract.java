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
package com.asetune.sql.pipe;

import com.asetune.sql.SqlProgressDialog;

public abstract class PipeCommandAbstract
implements IPipeCommand
{
//	abstract public IPipeCommand parse(String input) throws PipeCommandException;

	protected String _cmdStr = null;
	protected String _sqlStr = null;
	
	public PipeCommandAbstract(String input, String sqlString)
	{
		_cmdStr = input;
		_sqlStr = sqlString;
	}

	@Override 
	public String getCmdStr()
	{
		return _cmdStr;
	}

	@Override 
	public String getSqlString()
	{
		return _sqlStr;
	}

	@Override abstract public String getConfig();
	
	@Override
	public void open() 
	throws Exception
	{
	}

	@Override
	public void doPipe(Object input) throws Exception
	{
	}

	@Override
	public void doEndPoint(Object input, SqlProgressDialog progressDialog) throws Exception
	{
	}

	@Override
	public void close()
	{
	}

	@Override
	public Object getEndPointResult(String type)
	{
		return null;
	}

}
