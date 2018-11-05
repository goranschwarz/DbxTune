package com.asetune.sql.conn;

import java.sql.Connection;
import java.sql.SQLException;

import com.asetune.sql.conn.info.DbxConnectionStateInfo;

public class TdsUnknownConnection extends DbxConnection
{

	public TdsUnknownConnection(Connection conn)
	{
		super(conn);
	}

	@Override
	public DbxConnectionStateInfo refreshConnectionStateInfo()
	{
		return null;
	}

	@Override
	public boolean isInTransaction() throws SQLException
	{
		return false;
	}

	/**
	 * If it's a Unknown TDS connection, it's probably some jTDS (JavaOpenServer implementation) where isValid() do not seem to work, so lets fall back to use isClosed()
	 */
	@Override
	public boolean isValid(int timeout) throws SQLException
	{
		System.out.println("INFO: TdsUnknownConnection.isValid("+timeout+") is using !isClosed() instead of isValid()");
		return ! _conn.isClosed();
	}
}
