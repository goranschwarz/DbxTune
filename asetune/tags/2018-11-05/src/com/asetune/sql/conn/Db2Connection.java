package com.asetune.sql.conn;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.asetune.sql.conn.info.DbxConnectionStateInfo;
import com.asetune.sql.conn.info.DbxConnectionStateInfoGenericJdbc;
import com.asetune.utils.Ver;

public class Db2Connection
extends DbxConnection
{

	public Db2Connection(Connection conn)
	{
		super(conn);
		Ver.majorVersion_mustBeTenOrAbove = true;
//System.out.println("constructor::Db2Connection(conn): conn="+conn);
	}

	@Override
	public boolean isInTransaction()
	throws SQLException
	{
		// Hopefully this works, but NOT certain
		String sql = 
			  "select UOW_LOG_SPACE_USED "
			+ "FROM TABLE(MON_GET_UNIT_OF_WORK((select agent_id from sysibmadm.applications where appl_id = application_id()), -1))";

		boolean retVal = false;
		
		Statement stmt = createStatement();
		ResultSet rs = stmt.executeQuery(sql);
		while (rs.next())
		{
			retVal = rs.getInt(1) > 0;
		}
		rs.close();
		stmt.close();

		return retVal;
	}

	@Override
	public DbxConnectionStateInfo refreshConnectionStateInfo()
	{
		DbxConnectionStateInfo csi = new DbxConnectionStateInfoGenericJdbc(this);
		setConnectionStateInfo(csi);
		return csi;
	}
}
