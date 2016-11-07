package com.asetune.sql.conn;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import com.asetune.sql.conn.info.DbxConnectionStateInfo;
import com.asetune.sql.conn.info.DbxConnectionStateInfoAse;
import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.Ver;

public class AseConnection 
extends TdsConnection
{

	public AseConnection(Connection conn)
	{
		super(conn);
		Ver.majorVersion_mustBeTenOrAbove = true;
//System.out.println("constructor::AseConnection(conn): conn="+conn);
	}

	@Override
	public DbxConnectionStateInfo refreshConnectionStateInfo()
	{
		DbxConnectionStateInfo csi = new DbxConnectionStateInfoAse(this);
		setConnectionStateInfo(csi);
		return csi;
	}

	@Override
	public boolean isInTransaction() throws SQLException
	{
		return false; // FIXME: Don't know how to check this, so lets assume FALSE
	}

	/**
	 * If the server handles databases like MS SQL_Server and Sybase ASE
	 * @return true or false
	 */
	@Override
	public boolean isDatabaseAware()
	{
		return true;
	}

	@Override
	public int getDbmsVersionNumber()
	{
		return AseConnectionUtils.getAseVersionNumber(this);
	}

	@Override
	public boolean isDbmsClusterEnabled()
	{
		return AseConnectionUtils.isClusterEnabled(this);
	}

	@Override
	public List<String> getActiveServerRolesOrPermissions()
	{
		return AseConnectionUtils.getActiveRoles(this);
	}
}
