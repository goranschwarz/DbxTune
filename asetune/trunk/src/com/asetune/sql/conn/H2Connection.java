package com.asetune.sql.conn;

import java.sql.Connection;
import java.sql.SQLException;

import com.asetune.sql.conn.info.DbxConnectionStateInfo;
import com.asetune.sql.conn.info.DbxConnectionStateInfoGenericJdbc;

public class H2Connection extends DbxConnection
{

	public H2Connection(Connection conn)
	{
		super(conn);
System.out.println("constructor::H2Connection(conn): conn="+conn);
	}

	@Override
	public DbxConnectionStateInfo refreshConnectionStateInfo()
	{
		DbxConnectionStateInfo csi = new DbxConnectionStateInfoGenericJdbc(this);
		setConnectionStateInfo(csi);
		return csi;
	}

	@Override
	public boolean isInTransaction() throws SQLException
	{
		return false; // FIXME: Don't know how to check this, so lets assume FALSE
	}
}
