package com.asetune.sql.conn;

import java.sql.Connection;

public class H2Connection extends DbxConnection
{

	public H2Connection(Connection conn)
	{
		super(conn);
System.out.println("constructor::H2Connection(conn): conn="+conn);
	}

}
