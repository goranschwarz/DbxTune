package com.asetune.sql.conn;

import java.sql.Connection;
import java.sql.SQLException;

import com.asetune.sql.conn.info.DbxConnectionStateInfo;
import com.asetune.utils.Ver;

public class RsDaConnection extends TdsConnection
{
	public RsDaConnection(Connection conn)
	{
		super(conn);
		Ver.majorVersion_mustBeTenOrAbove = true;
	}

	@Override
	public DbxConnectionStateInfo refreshConnectionStateInfo()
	{
		DbxConnectionStateInfo csi = null;
		setConnectionStateInfo(csi);
		return csi;
	}

	@Override
	public boolean isInTransaction() throws SQLException
	{
		return false; // FIXME: Don't know how to check this, so lets assume FALSE
	}

	/**
	 * If it's a Unknown TDS connection, it's probably some jTDS (JavaOpenServer implementation) where isValid() do not seem to work, so lets fall back to use isClosed()
	 */
	@Override
	public boolean isValid(int timeout) throws SQLException
	{
		return ! _conn.isClosed();
	}
}
