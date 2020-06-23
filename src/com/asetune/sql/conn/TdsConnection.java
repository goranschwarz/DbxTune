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
package com.asetune.sql.conn;

import java.sql.Connection;
import java.sql.SQLException;

import com.asetune.sql.conn.info.DbxConnectionStateInfo;
import com.asetune.utils.Ver;
import com.sybase.jdbcx.SybConnection;
import com.sybase.jdbcx.SybMessageHandler;

public abstract class TdsConnection extends DbxConnection
{
	public TdsConnection(Connection conn)
	{
		super(conn);
		Ver.majorVersion_mustBeTenOrAbove = true;
//System.out.println("constructor::TdsConnection(conn): conn="+conn);
	}

	protected SybMessageHandler _oldMsgHandler = null;
	public void setSybMessageHandler(SybMessageHandler sybMessageHandler)
	{
		// Save the current message handler so we can restore it later
		_oldMsgHandler = ((SybConnection)_conn).getSybMessageHandler();

		((SybConnection)_conn).setSybMessageHandler(sybMessageHandler);
	}

	public void restoreSybMessageHandler()
	{
		((SybConnection)_conn).setSybMessageHandler(_oldMsgHandler);
	}

	public void cancel()
	throws SQLException
	{
		if (_conn instanceof SybConnection)
			((SybConnection)_conn).cancel();
	}

	@Override
	public abstract DbxConnectionStateInfo refreshConnectionStateInfo();

	@Override
	public abstract boolean isInTransaction() throws SQLException;
}
