package com.asetune.sql.conn;

import java.sql.Connection;

public class RaxConnection
extends TdsConnection
{

	public RaxConnection(Connection conn)
	{
		super(conn);
System.out.println("constructor::RaxConnection(conn): conn="+conn);
	}

}
