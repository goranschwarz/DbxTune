package com.asetune.sql.conn;

import java.sql.Connection;

public class MySqlConnection extends DbxConnection
{

	public MySqlConnection(Connection conn)
	{
		super(conn);
System.out.println("constructor::MySqlConnection(conn): conn="+conn);
	}

}
