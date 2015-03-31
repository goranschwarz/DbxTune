package com.asetune.sql.conn;

import java.sql.Connection;

public class IqConnection 
extends TdsConnection
{

	public IqConnection(Connection conn)
	{
		super(conn);
System.out.println("constructor::IqConnection(conn): conn="+conn);
	}

}
