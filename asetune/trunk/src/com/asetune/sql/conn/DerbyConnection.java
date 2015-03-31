package com.asetune.sql.conn;

import java.sql.Connection;

public class DerbyConnection extends DbxConnection
{

	public DerbyConnection(Connection conn)
	{
		super(conn);
System.out.println("constructor::DerbyConnection(conn): conn="+conn);
	}

}
