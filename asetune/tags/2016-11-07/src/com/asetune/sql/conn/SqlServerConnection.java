package com.asetune.sql.conn;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import com.asetune.sql.conn.info.DbxConnectionStateInfo;
import com.asetune.sql.conn.info.DbxConnectionStateInfoSqlServer;
import com.asetune.utils.StringUtil;
import com.asetune.utils.Ver;
import com.asetune.utils.VersionSqlServer;

public class SqlServerConnection 
extends DbxConnection
{
	private static Logger _logger = Logger.getLogger(SqlServerConnection.class);

	public SqlServerConnection(Connection conn)
	{
		super(conn);
		Ver.majorVersion_mustBeTenOrAbove = false;
//System.out.println("constructor::SqlServerConnection(conn): conn="+conn);
	}

	// Cached values
	private List<String> _getActiveServerRolesOrPermissions = null;

	@Override
	public void clearCachedValues()
	{
		_getActiveServerRolesOrPermissions = null;
		super.clearCachedValues();
	}

	@Override
	public DbxConnectionStateInfo refreshConnectionStateInfo()
	{
		DbxConnectionStateInfo csi = new DbxConnectionStateInfoSqlServer(this);
		setConnectionStateInfo(csi);
		return csi;
	}

	@Override
	public boolean isInTransaction() throws SQLException
	{
		return false; // FIXME: Don't know how to check this, so lets assume FALSE
	}

	/**
	 * If the server handles databases like MS SQL_Server and Sybase ASE
	 * @return true or false
	 */
	@Override
	public boolean isDatabaseAware()
	{
		return true;
	}
	
	@Override
	public int getDbmsVersionNumber()
	{
		int srvVersionNum = 0;

		String sql = "select @@version";
		
		// version
		try
		{
			String versionStr = "";

			Statement stmt = _conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while ( rs.next() )
			{
				versionStr = rs.getString(1);
			}
			rs.close();
	
			if (srvVersionNum == 0)
			{
				srvVersionNum = VersionSqlServer.parseVersionStringToNumber(versionStr);
			}
		}
		catch (SQLException ex)
		{
			_logger.error("SqlServerConnection.getDbmsVersionNumber(), '"+sql+"'", ex);
		}
		
		return srvVersionNum;
	}

	@Override
	public String getDbmsVersionStr() 
	throws SQLException
	{
		final String UNKNOWN = "";

		if ( ! isConnectionOk() )
			return UNKNOWN;

		String sql = "select @@version";

		try
		{
			String verStr = UNKNOWN;

			Statement stmt = _conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next())
			{
				verStr = rs.getString(1);
			}
			rs.close();
			stmt.close();

			if (verStr != null)
				verStr = verStr.replace('\n', ' ');
			return verStr;
		}
		catch (SQLException e)
		{
			_logger.debug("When getting DBMS Version ('"+sql+"'), Caught exception.", e);

			return UNKNOWN;
		}
	}

	@Override
	public List<String> getActiveServerRolesOrPermissions()
	{
		if (_getActiveServerRolesOrPermissions != null)
			return _getActiveServerRolesOrPermissions;

//		RS> Col# Label           JDBC Type Name          Guessed DBMS type Source Table
//		RS> ---- --------------- ----------------------- ----------------- ------------
//		RS> 1    entity_name     java.sql.Types.NVARCHAR nvarchar(128)     -none-      
//		RS> 2    subentity_name  java.sql.Types.NVARCHAR nvarchar(128)     -none-      
//		RS> 3    permission_name java.sql.Types.NVARCHAR nvarchar(60)      -none-      
//		+-----------+--------------+-------------------------------+
//		|entity_name|subentity_name|permission_name                |
//		+-----------+--------------+-------------------------------+
//		|server     |              |CONNECT SQL                    |
//		|server     |              |SHUTDOWN                       |
//		|server     |              |CREATE ENDPOINT                |
//		|server     |              |CREATE ANY DATABASE            |
//		|server     |              |ALTER ANY LOGIN                |
//		|server     |              |ALTER ANY CREDENTIAL           |
//		|server     |              |ALTER ANY ENDPOINT             |
//		|server     |              |ALTER ANY LINKED SERVER        |
//		|server     |              |ALTER ANY CONNECTION           |
//		|server     |              |ALTER ANY DATABASE             |
//		|server     |              |ALTER RESOURCES                |
//		|server     |              |ALTER SETTINGS                 |
//		|server     |              |ALTER TRACE                    |
//		|server     |              |ADMINISTER BULK OPERATIONS     |
//		|server     |              |AUTHENTICATE SERVER            |
//		|server     |              |EXTERNAL ACCESS ASSEMBLY       |
//		|server     |              |VIEW ANY DATABASE              |
//		|server     |              |VIEW ANY DEFINITION            |
//		|server     |              |VIEW SERVER STATE              |
//		|server     |              |CREATE DDL EVENT NOTIFICATION  |
//		|server     |              |CREATE TRACE EVENT NOTIFICATION|
//		|server     |              |ALTER ANY EVENT NOTIFICATION   |
//		|server     |              |ALTER SERVER STATE             |
//		|server     |              |UNSAFE ASSEMBLY                |
//		|server     |              |ALTER ANY SERVER AUDIT         |
//		|server     |              |CONTROL SERVER                 |
//		+-----------+--------------+-------------------------------+
//		(26 rows affected)
		
		String sql = "select * from sys.fn_my_permissions(default,default)";
		try
		{
			List<String> permissionList = new LinkedList<String>();
			Statement stmt = this.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next())
			{
				String role = rs.getString(3);
				if ( ! permissionList.contains(role) )
					permissionList.add(role);
			}
			rs.close();
			stmt.close();

			if (_logger.isDebugEnabled())
				_logger.debug("getActiveServerRolesOrPermissions() returns, permissionList='"+permissionList+"'.");

			// Cache the value for next execution
			_getActiveServerRolesOrPermissions = permissionList;
			return permissionList;
		}
		catch (SQLException ex)
		{
			_logger.warn("Problems when executing sql: "+sql, ex);
			return null;
		}
	}

//	protected JtdsMessageHandler _oldMsgHandlerJtds = null;
//	protected SQLServerMessageHandler _oldMsgHandlerSqlServer = null;

//	public void setMessageHandler(SQLServerMessageHandler messageHandler)
//	{
//		if (_conn instanceof SQLServerConnection)
//		{
//    		// Save the current message handler so we can restore it later
//			_oldMsgHandlerSqlServer = ((SQLServerConnection)_conn).getSQLServerMessageHandler();
//    
//    		((SQLServerConnection)_conn).setSQLServerMessageHandler(messageHandler);
//		}
//	}

//	public void setMessageHandler(JtdsMessageHandler messageHandler)
//	{
//		if (_conn instanceof JtdsConnection)
//		{
//    		// Save the current message handler so we can restore it later
//			_oldMsgHandlerJtds = ((JtdsConnection)_conn).getJtdsMessageHandler();
//    
//    		((JtdsConnection)_conn).setJtdsMessageHandler(messageHandler);
//		}
//	}

	public void restoreMessageHandler()
	{
//		if (_conn instanceof JtdsConnection)
//		{
//			((JtdsConnection)_conn).setJtdsMessageHandler(_oldMsgHandlerJtds);
//		}
//		if (_conn instanceof SQLServerConnection)
//		{
//			((SQLServerConnection)_conn).setSQLServerMessageHandler(_oldMsgHandlerSqlServer);
//		}
	}

	/**
	 * Get (procedure) text about an object
	 * 
	 * @param conn       Connection to the database
	 * @param dbname     Name of the database (if null, current db will be used)
	 * @param objectName Name of the procedure/view/trigger...
	 * @param owner      Name of the owner, if null is passed, it will be set to 'dbo'
	 * @param aseVersion Version of the ASE, if 0, the version will be fetched from ASE
	 * @return Text of the procedure/view/trigger...
	 */
	public String getObjectText(String dbname, String objectName, String owner, int dbmsVersion)
	{
		if (StringUtil.isNullOrBlank(owner))
			owner = "dbo.";
		else
			owner = owner + ".";

//		if (dbmsVersion <= 0)
//		{
//			dbmsVersion = getDbmsVersionNumber();
//		}

		String returnText = null;
		
		String dbnameStr = dbname;
		if (dbnameStr == null)
			dbnameStr = "";
		else
			dbnameStr = dbname + ".dbo.";
			
		//--------------------------------------------
		// GET OBJECT TEXT
		String sql = dbnameStr + "sp_helptext '" + owner + objectName + "'";

		try
		{
			StringBuilder sb = new StringBuilder();

			Statement statement = createStatement();
			ResultSet rs = statement.executeQuery(sql);
			while(rs.next())
			{
				String textPart = rs.getString(1);
				sb.append(textPart);
			}
			rs.close();
			statement.close();

			if (sb.length() > 0)
				returnText = sb.toString();
		}
		catch (SQLException e)
		{
			returnText = null;
			_logger.warn("Problems getting text for object '"+objectName+"', with owner '"+owner+"', in db '"+dbname+"'. Caught: "+e); 
		}

		return returnText;
	}
}
