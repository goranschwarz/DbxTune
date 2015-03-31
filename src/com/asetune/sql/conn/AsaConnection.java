package com.asetune.sql.conn;

import java.sql.Connection;

public class AsaConnection 
extends TdsConnection
{

	public AsaConnection(Connection conn)
	{
		super(conn);
System.out.println("constructor::AsaConnection(conn): conn="+conn);
	}

}
