package com.asetune.sql.conn;

import java.sql.Connection;
import java.sql.SQLException;

import com.asetune.sql.conn.info.DbxConnectionStateInfo;
import com.asetune.sql.conn.info.DbxConnectionStateInfoRs;
import com.asetune.utils.AseConnectionUtils;

public class RsConnection
extends TdsConnection
{

	public RsConnection(Connection conn)
	{
		super(conn);
//System.out.println("constructor::RsConnection(conn): conn="+conn);
	}

	@Override
	public DbxConnectionStateInfo refreshConnectionStateInfo()
	{
		DbxConnectionStateInfo csi = new DbxConnectionStateInfoRs(this);
		setConnectionStateInfo(csi);
		return csi;
	}

	@Override
	public boolean isInTransaction() throws SQLException
	{
		return false; // FIXME: Don't know how to check this, so lets assume FALSE
	}

	@Override
	public int getDbmsVersionNumber()
	{
		return AseConnectionUtils.getRsVersionNumber(this);
	}

	@Override
	public boolean isDbmsClusterEnabled()
	{
		return false;
	}
}