package com.asetune.sql.conn;

import java.sql.Connection;

public class SqlServerConnection 
extends DbxConnection
{

	public SqlServerConnection(Connection conn)
	{
		super(conn);
System.out.println("constructor::SqlServerConnection(conn): conn="+conn);
	}

}
