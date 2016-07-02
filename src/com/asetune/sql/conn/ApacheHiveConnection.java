package com.asetune.sql.conn;

import java.sql.Connection;
import java.sql.SQLException;

import com.asetune.sql.conn.info.DbxConnectionStateInfo;
import com.asetune.sql.conn.info.DbxConnectionStateInfoGenericJdbc;
import com.asetune.utils.Ver;

public class ApacheHiveConnection
extends DbxConnection
{
	public ApacheHiveConnection(Connection conn)
	{
		super(conn);
		Ver.majorVersion_mustBeTenOrAbove = false;
//System.out.println("constructor::PostgresConnection(conn): conn="+conn);
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
		return false;
	}


//	NOTE: The below didn't work as expected... find a better way!!!
//	@Override
//	public boolean isInTransaction()
//	throws SQLException
//	{
//		String sql = 
//			"select count(*) \n" +
//			"from pg_locks \n" +
//			"where pid = pg_backend_pid() \n" +
//			"  and locktype in ('transactionid') \n" +
//			"  and mode = 'ExclusiveLock' \n";
//
//		boolean retVal = false;
//		
//		Statement stmt = createStatement();
//		ResultSet rs = stmt.executeQuery(sql);
//		while (rs.next())
//		{
//			retVal = rs.getInt(1) == 1;
//		}
//		rs.close();
//		stmt.close();
//
//		return retVal;
//	}
// workbench.db.postgresql.opentransaction.query=select count(*) from pg_locks where pid = pg_backend_pid() and locktype in ('transactionid') and mode = 'ExclusiveLock'

}
