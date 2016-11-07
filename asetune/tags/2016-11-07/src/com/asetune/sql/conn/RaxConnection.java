package com.asetune.sql.conn;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

import com.asetune.sql.conn.info.DbxConnectionStateInfo;
import com.asetune.sql.conn.info.DbxConnectionStateInfoRax;
import com.asetune.utils.Ver;

public class RaxConnection
extends TdsConnection
{
	private static Logger _logger = Logger.getLogger(RaxConnection.class);

	public RaxConnection(Connection conn)
	{
		super(conn);
		Ver.majorVersion_mustBeTenOrAbove = true;
//System.out.println("constructor::RaxConnection(conn): conn="+conn);
	}

	@Override
	public DbxConnectionStateInfo refreshConnectionStateInfo()
	{
		DbxConnectionStateInfo csi = new DbxConnectionStateInfoRax(this);
		setConnectionStateInfo(csi);
		return csi;
	}

	@Override
	public boolean isInTransaction() throws SQLException
	{
		return false; // FIXME: Don't know how to check this, so lets assume FALSE
	}

	//---------------------------------------
	// Lets cache some stuff
	//---------------------------------------
	private String _cached_srvName = null;

	@Override
	public String getDbmsServerName()
	{
		if (_cached_srvName != null)
			return _cached_srvName;

		final String UNKNOWN = "";

		if ( ! isConnectionOk(false, null) )
			return UNKNOWN;

		try
		{
            // 1> ra_version_all
            // RS> Col# Label     JDBC Type Name      Guessed DBMS type
            // RS> ---- --------- ------------------- -----------------
            // RS> 1    Component java.sql.Types.CHAR char(21)         
            // RS> 2    Version   java.sql.Types.CHAR char(148)        

			String name = UNKNOWN;

			Statement stmt = createStatement();
			ResultSet rs = stmt.executeQuery("ra_version_all");
			while (rs.next())
			{
				String comp = rs.getString(1);
				String ver  = rs.getString(2);
				
				if (comp != null) comp = comp.trim();
				if (ver  != null) ver  = ver .trim();

				if ("Instance:".equals(comp))
				{
					name = ver;
					int dashPos = name.indexOf('-');
					if (dashPos >= 0)
						name = name.substring(0, dashPos).trim();
				}
			}
			rs.close();
			stmt.close();

			_cached_srvName = name;
			return name;
		}
		catch (SQLException e)
		{
			_logger.debug("When getting 'server name', Caught exception.", e);

			return UNKNOWN;
		}
	}
}
