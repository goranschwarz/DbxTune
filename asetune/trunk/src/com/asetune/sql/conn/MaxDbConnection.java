package com.asetune.sql.conn;

import java.sql.Connection;

public class MaxDbConnection extends DbxConnection
{

	public MaxDbConnection(Connection conn)
	{
		super(conn);
System.out.println("constructor::MaxDbConnection(conn): conn="+conn);
	}

}
