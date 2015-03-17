package com.asetune.sql.conn;

import java.awt.Component;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.DbUtils;
import com.asetune.utils.RepServerUtils;
import com.asetune.utils.SwingUtils;

public abstract class DbxConnection
implements Connection
{
	private static Logger _logger = Logger.getLogger(DbxConnection.class);

//	protected String _username = null;
//	protected String _password = null;
//	
//	protected String _server = null;
//	protected String _dbname = null; // or the catalog
//
//	protected String _driver = null;
//	protected String _url    = null;

	protected ConnectionProp _connProp = null;
	protected static ConnectionProp _defaultConnProp = null;
	
	protected String _databaseProductName    = null;
	protected String _databaseProductVersion = null;
	protected String _databaseServerName     = null;

	
//	public String getUsername() { return _username; }
//	public String getPassword() { return _password; }
//	public String getServer()   { return _server; }
//	public String getDbname()   { return _dbname; }
//	public String getDriver()   { return _driver; }
//	public String getUrl()      { return _url; }

	public ConnectionProp getConnProp() { return _connProp; }
	public void setConnProp(ConnectionProp connProp) { _connProp = connProp; }

	public static ConnectionProp getDefaultConnProp() { return _defaultConnProp; }
	public static void setDefaultConnProp(ConnectionProp defaultConnProp) { _defaultConnProp = defaultConnProp; }

//	public void setUsername(String username) { _username = username; }
//	public void setPassword(String password) { _password = password; }
//	public void setServer  (String server)   { _server   = server; }
//	public void setDbname  (String dbname)   { _dbname   = dbname; }
//	public void setDriver  (String driver)   { _driver   = driver; }
//	public void setUrl     (String url)      { _url      = url; }

	/**
	 * Make a connection using the already set information with setDriver(), setUsername(), setPassword(), { setUrl() | setServer() } [setDbname()]
	 * @throws SQLException
	 */
	public void connect()
	throws SQLException
	{
		
		// When the connection is made, refresh/get/set some basic information about the connection.  
		getDatabaseProductName();
		getDatabaseProductVersion();
		getDatabaseServerName();
		
//		getConnectionId();   // getSpid()
//		getCurrentCatalog(); // getCurrentDbname()    or maybe getCatalog() instead...
	}


	/**
	 * Basically checks if the connection is OK, probably calls isClosed()
	 * @return
	 */
	public boolean isConnectionOk()
	{
		return isConnectionOk(false, null);
	}
	public boolean isConnectionOk(boolean guiMsgOnError, Component guiOwner)
	{
		String msg   = "";
		String title = "Checking DB Connection";

		if ( _conn == null ) 
		{	
			msg = "The Connection object is null.";
			_logger.debug(msg);

			if (guiOwner != null)
				SwingUtils.showWarnMessage(guiOwner, title, msg, new Exception(msg));

			return false;
		}
		
		try
		{
			if ( _conn.isClosed() )
			{
				msg = "The Connection object is NOT connected.";
				_logger.debug(msg);

				if (guiOwner != null)
					SwingUtils.showWarnMessage(guiOwner, title, msg, new Exception(msg));

				return false;
			}
		}
		catch (SQLException e)
		{
			_logger.debug("When checking the DB Connection, Caught exception.", e);

			if (guiOwner != null)
				showSqlExceptionMessage(guiOwner, "Checking DB Connection", "When checking the DB Connection, we got an SQLException", e);
			
			return false;
		}
		return true;
	}

	/**
	 * Get the connected database product name, simply call jdbc.getMetaData().getDatabaseProductName();
	 * @return null if not connected else: Retrieves the name of this database product.
	 * @see java.sql.DatabaseMetaData.getDatabaseProductName
	 */
	public String getDatabaseProductName() 
	throws SQLException
	{
		if (_databaseProductName != null)
			return _databaseProductName;

		if (_conn == null)
			return null;

		try
		{
			String str = _conn.getMetaData().getDatabaseProductName();
			_logger.debug("getDatabaseProductName() returns: '"+str+"'.");
			
			_databaseProductName = str;
			return str; 
		}
		catch (SQLException e)
		{
			// If NO metadata installed, check if it's a Sybase Replication Server.
			// JZ0SJ: Metadata accessor information was not found on this database. Please install the required tables as mentioned in the jConnect documentation.
			if ( "JZ0SJ".equals(e.getSQLState()) )
			{
				try
				{
					String str1 = "";
					String str2 = "";
					Statement stmt = _conn.createStatement();
					ResultSet rs = stmt.executeQuery("admin rssd_name");
					while ( rs.next() )
					{
						str1 = rs.getString(1);
						str2 = rs.getString(2);
					}
					rs.close();
					stmt.close();

					_logger.info("Replication Server with RSSD at '"+str1+"."+str2+"'.");

					// If the above statement succeeds, then it must be a RepServer without metadata installed.
					_databaseProductName = DbUtils.DB_PROD_NAME_SYBASE_RS;
					return _databaseProductName;
				}
				catch(SQLException ignoreRsExceptions) {}
			}
			_logger.debug("getDatabaseProductName() Caught: "+e, e);
			throw e;
		}
	}
	/**
	 * Get the connected database version string, simply call jdbc.getMetaData().getDatabaseProductName();
	 * @return null if not connected else: Retrieves the version number of this database product.
	 * @see java.sql.DatabaseMetaData.getDatabaseProductName
	 */
	public String getDatabaseProductVersion() 
	throws SQLException
	{
		if (_databaseProductVersion != null)
			return _databaseProductVersion;

		if (_conn == null)
			return null;

		try
		{
			String str = _conn.getMetaData().getDatabaseProductVersion();
			_logger.debug("getDatabaseProductVersion() returns: '"+str+"'.");
			
			_databaseProductVersion = str;
			return str; 
		}
		catch (SQLException e)
		{
			// If NO metadata installed, check if it's a Sybase Replication Server.
			// JZ0SJ: Metadata accessor information was not found on this database. Please install the required tables as mentioned in the jConnect documentation.
			if ( "JZ0SJ".equals(e.getSQLState()) )
			{
				try
				{
					String str = "";
					Statement stmt = _conn.createStatement();
					ResultSet rs = stmt.executeQuery("admin version");
					while ( rs.next() )
					{
						str = rs.getString(1);
					}
					rs.close();
					stmt.close();

					_logger.info("Replication Server with Version string '"+str+"'.");

					// If the above statement succeeds, then it must be a RepServer without metadata installed.
					_databaseProductVersion = str;
					return str;
				}
				catch(SQLException ignoreRsExceptions) {}
			}
			_logger.debug("getDatabaseProductVersion() Caught: "+e, e);
			throw e;
		}
	}

	/**
	 * Check if current connected product name is equal to the input parameter
	 * @param str Name of the product to test for
	 * @return true if equal
	 */
	public boolean isDatabaseProduct(String str)
	{
		if (str == null)
			return false;

		try
		{
			String currentDbProductName = getDatabaseProductName();
			return str.equals(currentDbProductName);
		}
		catch (SQLException e)
		{
			_logger.debug("isDatabaseProduct() Caught: "+e, e);
			return false;
		}
	}

	/**
	 * Get the connected database server/instance name.
	 * @return null if not connected else: Retrieves the name of this database server/instance name.
	 * @see java.sql.DatabaseMetaData.getDatabaseProductName
	 */
	public String getDatabaseServerName() 
	throws SQLException
	{
		if (_databaseServerName != null)
			return _databaseServerName;
		
		if (_conn == null)
			return null;
		
		String serverName = "";
		String currentDbProductName = getDatabaseProductName();

		// FIXME: move the below code to it's individual extended classes 

		// ASE
		if      (DbUtils.DB_PROD_NAME_SYBASE_ASE.equals(currentDbProductName))
		{
			serverName = AseConnectionUtils.getAseServername(_conn);
		}
		// ASA SQL Anywhere
		else if (DbUtils.DB_PROD_NAME_SYBASE_ASA.equals(currentDbProductName))
		{
			serverName = AseConnectionUtils.getAseServername(_conn);
		}
		// Sybase IQ
		else if (DbUtils.DB_PROD_NAME_SYBASE_IQ.equals(currentDbProductName))
		{
			serverName = AseConnectionUtils.getAseServername(_conn);
		}
		// Replication Server
		else if (DbUtils.DB_PROD_NAME_SYBASE_RS.equals(currentDbProductName))
		{
			serverName = RepServerUtils.getServerName(_conn);
		}
		// H2
		else if (DbUtils.DB_PROD_NAME_H2.equals(currentDbProductName))
		{
		}
		// ORACLE
		else if (DbUtils.DB_PROD_NAME_ORACLE.equals(currentDbProductName))
		{
			serverName = DbUtils.getOracleServername(_conn);
		}
		// Microsoft
		else if (DbUtils.DB_PROD_NAME_MSSQL.equals(currentDbProductName))
		{
			serverName = AseConnectionUtils.getAseServername(_conn);
		}
		// UNKNOWN
		else
		{
		}

		_databaseServerName = serverName;
		return serverName;
	}
	
	//#################################################################################
	//#################################################################################
	//### BEGIN: delegated methods for Connection
	//#################################################################################
	//#################################################################################
	
	private Connection _conn;

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException
	{
		return _conn.unwrap(iface);
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException
	{
		return _conn.isWrapperFor(iface);
	}

	@Override
	public Statement createStatement() throws SQLException
	{
		return _conn.createStatement();
	}

	@Override
	public PreparedStatement prepareStatement(String sql) throws SQLException
	{
		return _conn.prepareStatement(sql);
	}

	@Override
	public CallableStatement prepareCall(String sql) throws SQLException
	{
		return _conn.prepareCall(sql);
	}

	@Override
	public String nativeSQL(String sql) throws SQLException
	{
		return _conn.nativeSQL(sql);
	}

	@Override
	public void setAutoCommit(boolean autoCommit) throws SQLException
	{
		_conn.setAutoCommit(autoCommit);
	}

	@Override
	public boolean getAutoCommit() throws SQLException
	{
		return _conn.getAutoCommit();
	}

	@Override
	public void commit() throws SQLException
	{
		_conn.commit();
	}

	@Override
	public void rollback() throws SQLException
	{
		_conn.rollback();
	}

	@Override
	public void close() throws SQLException
	{
		_conn.close();
	}

	@Override
	public boolean isClosed() throws SQLException
	{
		return _conn.isClosed();
	}

	@Override
	public DatabaseMetaData getMetaData() throws SQLException
	{
		return _conn.getMetaData();
	}

	@Override
	public void setReadOnly(boolean readOnly) throws SQLException
	{
		_conn.setReadOnly(readOnly);
	}

	@Override
	public boolean isReadOnly() throws SQLException
	{
		return _conn.isReadOnly();
	}

	@Override
	public void setCatalog(String catalog) throws SQLException
	{
		_conn.setCatalog(catalog);
	}

	@Override
	public String getCatalog() throws SQLException
	{
		return _conn.getCatalog();
	}

	@Override
	public void setTransactionIsolation(int level) throws SQLException
	{
		_conn.setTransactionIsolation(level);
	}

	@Override
	public int getTransactionIsolation() throws SQLException
	{
		return _conn.getTransactionIsolation();
	}

	@Override
	public SQLWarning getWarnings() throws SQLException
	{
		return _conn.getWarnings();
	}

	@Override
	public void clearWarnings() throws SQLException
	{
		_conn.clearWarnings();
	}

	@Override
	public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException
	{
		return _conn.createStatement(resultSetType, resultSetConcurrency);
	}

	@Override
	public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException
	{
		return _conn.prepareStatement(sql, resultSetType, resultSetConcurrency);
	}

	@Override
	public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException
	{
		return _conn.prepareCall(sql, resultSetType, resultSetConcurrency);
	}

	@Override
	public Map<String, Class<?>> getTypeMap() throws SQLException
	{
		return _conn.getTypeMap();
	}

	@Override
	public void setTypeMap(Map<String, Class<?>> map) throws SQLException
	{
		_conn.setTypeMap(map);
	}

	@Override
	public void setHoldability(int holdability) throws SQLException
	{
		_conn.setHoldability(holdability);
	}

	@Override
	public int getHoldability() throws SQLException
	{
		return _conn.getHoldability();
	}

	@Override
	public Savepoint setSavepoint() throws SQLException
	{
		return _conn.setSavepoint();
	}

	@Override
	public Savepoint setSavepoint(String name) throws SQLException
	{
		return _conn.setSavepoint(name);
	}

	@Override
	public void rollback(Savepoint savepoint) throws SQLException
	{
		_conn.rollback(savepoint);
	}

	@Override
	public void releaseSavepoint(Savepoint savepoint) throws SQLException
	{
		_conn.releaseSavepoint(savepoint);
	}

	@Override
	public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException
	{
		return _conn.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
	}

	@Override
	public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException
	{
		return _conn.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
	}

	@Override
	public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException
	{
		return _conn.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
	}

	@Override
	public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException
	{
		return _conn.prepareStatement(sql, autoGeneratedKeys);
	}

	@Override
	public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException
	{
		return _conn.prepareStatement(sql, columnIndexes);
	}

	@Override
	public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException
	{
		return _conn.prepareStatement(sql, columnNames);
	}

	@Override
	public Clob createClob() throws SQLException
	{
		return _conn.createClob();
	}

	@Override
	public Blob createBlob() throws SQLException
	{
		return _conn.createBlob();
	}

	@Override
	public NClob createNClob() throws SQLException
	{
		return _conn.createNClob();
	}

	@Override
	public SQLXML createSQLXML() throws SQLException
	{
		return _conn.createSQLXML();
	}

	@Override
	public boolean isValid(int timeout) throws SQLException
	{
		return _conn.isValid(timeout);
	}

	@Override
	public void setClientInfo(String name, String value) throws SQLClientInfoException
	{
		_conn.setClientInfo(name, value);
	}

	@Override
	public void setClientInfo(Properties properties) throws SQLClientInfoException
	{
		_conn.setClientInfo(properties);
	}

	@Override
	public String getClientInfo(String name) throws SQLException
	{
		return _conn.getClientInfo(name);
	}

	@Override
	public Properties getClientInfo() throws SQLException
	{
		return _conn.getClientInfo();
	}

	@Override
	public Array createArrayOf(String typeName, Object[] elements) throws SQLException
	{
		return _conn.createArrayOf(typeName, elements);
	}

	@Override
	public Struct createStruct(String typeName, Object[] attributes) throws SQLException
	{
		return _conn.createStruct(typeName, attributes);
	}

	//#################################################################################
	//#################################################################################
	//### END: delegated methods for Connection
	//#################################################################################
	//#################################################################################

	

	
	
	
	
	//#################################################################################
	//#################################################################################
	//### BEGIN: some helper methods
	//#################################################################################
	//#################################################################################
	
	public static String showSqlExceptionMessage(Component owner, String title, String msg, SQLException sqlex) 
	{
		String exMsg    = getMessageFromSQLException(sqlex, true, "010HA"); // 010HA: The server denied your request to use the high-availability feature. Please reconfigure your database, or do not request a high-availability session.
		String exMsgRet = getMessageFromSQLException(sqlex, false);

		String extraInfo = ""; 

		// JZ00L: Login failed
		if (AseConnectionUtils.containsSqlState(sqlex, "JZ00L"))
//		if ( "JZ00L".equals(sqlex.getSQLState()) )
		{
			extraInfo  = "<br>" +
					"<b>Are you sure about the password, SQL State 'JZ00L'</b><br>" +
					"The first Exception 'JZ00L' in <b>most</b> cases means: wrong user or password<br>" +
					"Before you begin searching for strange reasons, please try to login again and make sure you use the correct password.<br>" +
					"<br>" +
					"Below is all Exceptions that happened during the login attempt.<br>" +
					"<HR NOSHADE>";
		}

		// JZ0IB: The server's default charset of roman8 does not map to an encoding that is available in the client Java environment. Because jConnect will not be able to do client-side conversion, the connection is unusable and is being closed. Try using a later Java version, or try including your Java installation's i18n.jar or charsets.jar file in the classpath.
		if (AseConnectionUtils.containsSqlState(sqlex, "JZ0IB"))
//		if ( "JZ0IB".equals(sqlex.getSQLState()) )
		{
			extraInfo  = "<br>" +
					"<b>Try a Workaround for the problem, SQL State 'JZ0IB'</b><br>" +
					"In the Connection Dialog, in the field 'URL Option' specify:<br>" +
					"<font size=\"4\">" +
					"  <code>CHARSET=iso_1</code><br>" +
					"</font>" +
					"<i>This will hopefully get you going, but characters might not converted correctly</i><br>" +
					"<br>" +
					"Below is all Exceptions that happened during the login attempt.<br>" +
					"<HR NOSHADE>";
		}

		String htmlStr = 
			"<html>" +
			  msg + "<br>" +
			  extraInfo + 
//			  "<br>" + 
//			  exMsg.replace("\n", "<br>") + 
			  exMsg + 
			"</html>"; 

		SwingUtils.showErrorMessage(owner, title, htmlStr, sqlex);
		
		return exMsgRet;
	}

	public static String getMessageFromSQLException(SQLException sqlex, boolean htmlTags, String... discardSqlState) 
	{
		StringBuffer sb = new StringBuffer("");
		if (htmlTags)
			sb.append("<UL>");

		ArrayList<String> discardedMessages = null;

		boolean first = true;
		while (sqlex != null)
		{
			boolean discardThis = false;
			for (String sqlState : discardSqlState)
			{
				if (sqlState.equals(sqlex.getSQLState()))
					discardThis = true;
			}

			if (discardThis)
			{
				if (discardedMessages == null)
					discardedMessages = new ArrayList<String>();
				discardedMessages.add( sqlex.getMessage() );
			}
			else
			{
    			if (first)
    				first = false;
    			else
    				sb.append( "\n" );
    
    			if (htmlTags)
    				sb.append("<LI>");
    			sb.append( sqlex.getMessage() );
    			if (htmlTags)
    				sb.append("</LI>");
			}

			sqlex = sqlex.getNextException();
		}

		if (htmlTags)
			sb.append("</UL>");

		if (discardedMessages != null)
		{
			sb.append(htmlTags ? "<BR><BR><B>Note</B>:" : "\nNote: ");
			sb.append("The following messages was also found but, they are not as important as above messages.").append(htmlTags ? "<br>" : "\n");
			if (htmlTags)
				sb.append("<HR NOSHADE>").append("<UL>");
			for (String msg : discardedMessages)
			{
    			if (htmlTags)
    				sb.append("<LI>");
    			sb.append( msg );
    			if (htmlTags)
    				sb.append("</LI>");
			}
			if (htmlTags)
				sb.append("</UL>");
		}
		
		return sb.toString();
	}

	//#################################################################################
	//#################################################################################
	//### END: some helper methods
	//#################################################################################
	//#################################################################################
	
}
