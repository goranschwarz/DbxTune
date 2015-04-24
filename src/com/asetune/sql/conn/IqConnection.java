package com.asetune.sql.conn;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

import com.asetune.sql.conn.info.DbxConnectionStateInfo;
import com.asetune.sql.conn.info.DbxConnectionStateInfoGenericJdbc;
import com.asetune.utils.AseConnectionUtils;


public class IqConnection 
extends TdsConnection
{
	private static Logger _logger = Logger.getLogger(IqConnection.class);

	public IqConnection(Connection conn)
	{
		super(conn);
System.out.println("constructor::IqConnection(conn): conn="+conn);
	}


	@Override
	public DbxConnectionStateInfo refreshConnectionStateInfo()
	{
		DbxConnectionStateInfo csi = new DbxConnectionStateInfoGenericJdbc(this);
		setConnectionStateInfo(csi);
		return csi;
	}

	@Override
	public boolean isInTransaction() throws SQLException
	{
		return false; // FIXME: Don't know how to check this, so lets assume FALSE
	}

	@Override
	public int getDbmsVersionNumber()
	{
		return AseConnectionUtils.getAseVersionNumber(this);
	}

	@Override
	public boolean isDbmsClusterEnabled()
	{
		return false; // FIXME: check if we are in Multiplex mode...
	}

	public String getPlatform()
	{
		String sql = "select Value from sa_eng_properties() where PropName = 'Platform'";
		try
		{
			Statement stmnt = createStatement();
			ResultSet rs = stmnt.executeQuery(sql);

			String platform = "";
			while(rs.next())
				platform = rs.getString(1);
			rs.close();
			stmnt.close();

			return platform;
		}
		catch (SQLException e)
		{
			_logger.warn("Problems getting IQ Platform. SQL='"+sql+"', Caught: "+e);
			return null;
		}
	}

	public String getIqMsgFilname()
	{
		String filename = null;
		String sql = "";

		try
		{
			// Something like this will be returned: C:\Sybase\IQ-16_0\GORAN_16_IQ\errorlog.iqmsg.log
			// But it may just be the filename without a full path... 
			// If that's the case we fix that with a second SQL query (see below)
			sql = "select file_name from sysfile where dbspace_name = 'IQ_SYSTEM_MSG'";
			Statement stmnt = createStatement();
			ResultSet rs = stmnt.executeQuery(sql);
			
			while(rs.next())
				filename = rs.getString(1);
			rs.close();

			boolean hasFullPath = (filename != null && (filename.indexOf('/')>=0 || filename.indexOf('\\')>=0));
			if ( ! hasFullPath )
			{
				String dbFileName = "";
				
				// Something like this will be returned: C:\Sybase\IQ-16_0\GORAN_16_IQ\GORAN_16_IQ.db
				sql = "select Value from sa_db_properties() where PropName = 'File'";
				rs = stmnt.executeQuery(sql);
				while(rs.next())
					dbFileName = rs.getString(1);
				
				// Get only the PATH part of the file
				File f = new File(dbFileName);
//FIXME: the below wont work.... so fix this...
				filename = f.getPath() + filename;
			}
			stmnt.close();
			
			return filename;
		}
		catch (SQLException e)
		{
			_logger.warn("Problems getting IQ Msg Log filename. SQL='"+sql+"', Caught: "+e);
			return null;
		}
	}
}
