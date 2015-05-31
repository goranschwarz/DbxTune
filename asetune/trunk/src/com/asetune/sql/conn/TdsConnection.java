package com.asetune.sql.conn;

import java.sql.Connection;
import java.sql.SQLException;

import com.asetune.sql.conn.info.DbxConnectionStateInfo;
import com.asetune.utils.AseConnectionUtils;
import com.sybase.jdbcx.SybConnection;
import com.sybase.jdbcx.SybMessageHandler;

public abstract class TdsConnection extends DbxConnection
{
	public TdsConnection(Connection conn)
	{
		super(conn);
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
