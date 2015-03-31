package com.asetune.sql.conn;

import java.sql.Connection;

public class Db2Connection
extends DbxConnection
{

	public Db2Connection(Connection conn)
	{
		super(conn);
System.out.println("constructor::Db2Connection(conn): conn="+conn);
	}

}
