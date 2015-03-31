package com.asetune.sql.conn;

import java.sql.Connection;

public class AseConnection 
extends TdsConnection
{

	public AseConnection(Connection conn)
	{
		super(conn);
System.out.println("constructor::AseConnection(conn): conn="+conn);
	}

}
