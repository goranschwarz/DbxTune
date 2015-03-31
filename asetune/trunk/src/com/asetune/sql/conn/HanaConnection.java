package com.asetune.sql.conn;

import java.sql.Connection;

public class HanaConnection extends DbxConnection
{

	public HanaConnection(Connection conn)
	{
		super(conn);
System.out.println("constructor::HanaConnection(conn): conn="+conn);
	}

}
