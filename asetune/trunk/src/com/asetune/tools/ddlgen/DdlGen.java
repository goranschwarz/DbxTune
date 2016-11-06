package com.asetune.tools.ddlgen;

import java.sql.SQLException;

import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.DbUtils;

public abstract class DdlGen
{
	public enum Type
	{
		DB, TABLE, VIEW, PROCEDURE, FUNCTION, RAW_PARAMS
	};
	
	private DbxConnection  _conn          = null;
	private String         _defaultDbname = null;
	private String         _extraParams   = null;
	
	public DbxConnection getConnection()      { return _conn; }
	public String        getDefaultDbname()   { return _defaultDbname; }
	public String        getExtraParams()     { return _extraParams; }

	public void setDefaultDbname(String name) { _defaultDbname = name; }
	public void setExtraParams(String params) { _extraParams   = params; }


	protected DdlGen(DbxConnection conn)
	{
		_conn = conn;
	}

	public static DdlGen create(DbxConnection conn)
	throws Exception
	{
		return create(conn, false, false);
	}

	public static DdlGen create(DbxConnection conn, boolean alwaysUseJdbcMetaData, boolean useJdbcMetaDataIfNoImplementation)
	throws Exception
	{
		String databaseProductName = conn.getDatabaseProductName();

		if (alwaysUseJdbcMetaData)
		{
			return new DdlGenJdbcMetaData(conn);
		}
		
		if (DbUtils.isProductName(databaseProductName, DbUtils.DB_PROD_NAME_SYBASE_ASE))
		{
			return new DdlGenAse(conn);
		}
		else
		{
			if (useJdbcMetaDataIfNoImplementation)
				return new DdlGenJdbcMetaData(conn);
			else
				throw new Exception("DBMS Type '"+databaseProductName+"' is not yet supported.");
		}
	}

	public static boolean isDbmsSupported(DbxConnection conn) 
	throws SQLException
	{
		return isDbmsSupported(conn.getDatabaseProductName());
	}
	
	public static boolean isDbmsSupported(String databaseProductName)
	{
		return DbUtils.isProductName(databaseProductName, 
				DbUtils.DB_PROD_NAME_SYBASE_ASE);
	}
	
	public String getUsedCommand()
	{
		return "";
	}
	
	public String getCommandForType(Type type, String name)
	{
		return "";
	}
	
	public abstract String getDdlForType(Type type, String name) throws Exception;

	public abstract String getDdlForDb(String name) throws Exception;
	public abstract String getDdlForTable(String name) throws Exception;
	public abstract String getDdlForView(String name) throws Exception;
	public abstract String getDdlForProcedure(String name) throws Exception;
	public abstract String getDdlForFunction(String name) throws Exception;
}
