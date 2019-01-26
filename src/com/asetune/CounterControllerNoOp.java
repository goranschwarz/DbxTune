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
package com.asetune;

import com.asetune.pcs.PersistContainer.HeaderInfo;
import com.asetune.sql.conn.DbxConnection;

public class CounterControllerNoOp
extends CounterControllerAbstract
{

	public CounterControllerNoOp(boolean hasGui)
	{
		super(hasGui);
	}

	@Override
	public void init()
	{
	}

	@Override
	public void checkServerSpecifics()
	{
	}

	@Override
	public HeaderInfo createPcsHeaderInfo()
	{
		return null;
	}

	@Override
//	public void initCounters(Connection conn, boolean hasGui, long srvVersion, boolean isClusterEnabled, long monTablesVersion) throws Exception
//	{
//	}
	public void initCounters(DbxConnection conn, boolean hasGui, long srvVersion, boolean isClusterEnabled, long monTablesVersion) throws Exception
	{
	}

	@Override
	public void createCounters(boolean hasGui)
	{
	}

	@Override
	public String getServerTimeCmd()
	{
		return null;
	}

	@Override
	protected String getIsClosedSql()
	{
		return null;
	}
}
