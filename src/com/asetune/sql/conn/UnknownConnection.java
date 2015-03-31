package com.asetune.sql.conn;

import java.sql.Connection;

public class UnknownConnection extends DbxConnection
{

	public UnknownConnection(Connection conn)
	{
		super(conn);
System.out.println("constructor::UnknownConnection(conn): conn="+conn);
	}

}
