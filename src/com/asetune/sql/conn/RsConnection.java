package com.asetune.sql.conn;

import java.sql.Connection;

public class RsConnection
extends TdsConnection
{

	public RsConnection(Connection conn)
	{
		super(conn);
System.out.println("constructor::RsConnection(conn): conn="+conn);
	}

}
