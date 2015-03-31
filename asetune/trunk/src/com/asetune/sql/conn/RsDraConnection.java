package com.asetune.sql.conn;

import java.sql.Connection;

public class RsDraConnection extends TdsConnection
{
	public RsDraConnection(Connection conn)
	{
		super(conn);
System.out.println("constructor::RsDraConnection(conn): conn="+conn);
	}

}
