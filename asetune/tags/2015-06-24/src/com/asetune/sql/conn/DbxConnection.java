package com.asetune.sql.conn;

import java.awt.Component;
import java.awt.Window;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverManager;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.swing.ImageIcon;

import org.apache.log4j.Logger;

import com.asetune.gui.ConnectionProfileManager;
import com.asetune.gui.ConnectionProgressDialog;
import com.asetune.sql.conn.info.DbxConnectionStateInfo;
import com.asetune.ssh.SshTunnelInfo;
import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.DbUtils;
import com.asetune.utils.RepServerUtils;
import com.asetune.utils.StringUtil;
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
	protected int    _dbmsVersionNumber      = -1;
	protected String _dbmsVersionStr         = null;
	
	protected String _dbmsCharsetName        = null;
	protected String _dbmsCharsetId          = null;
	protected String _dbmsSortOrderName      = null;
	protected String _dbmsSortOrderId        = null;
	
//	public String getUsername() { return _username; }
//	public String getPassword() { return _password; }
//	public String getServer()   { return _server; }
//	public String getDbname()   { return _dbname; }
//	public String getDriver()   { return _driver; }
//	public String getUrl()      { return _url; }

	public ConnectionProp getConnProp() { return _connProp; }
	public void setConnProp(ConnectionProp connProp) { _connProp = connProp; }

	public static boolean        hasDefaultConnProp() { return _defaultConnProp != null; }
	public static ConnectionProp getDefaultConnProp() { return _defaultConnProp; }
	public static void           setDefaultConnProp(ConnectionProp defaultConnProp) { _defaultConnProp = defaultConnProp; }

//	public void setUsername(String username) { _username = username; }
//	public void setPassword(String password) { _password = password; }
//	public void setServer  (String server)   { _server   = server; }
//	public void setDbname  (String dbname)   { _dbname   = dbname; }
//	public void setDriver  (String driver)   { _driver   = driver; }
//	public void setUrl     (String url)      { _url      = url; }

	//----------------------------------------------------------------------------------------------
	//----------------------------------------------------------------------------------------------
	// static/factory methods
	//----------------------------------------------------------------------------------------------
	//----------------------------------------------------------------------------------------------
	/**
	 * Make a connection using the already set information with setDefaultConnProp()
	 * @throws Exception
	 */
	public static DbxConnection connect(Window guiOwner, String appName)
	throws Exception
	{
		// Get default connection props
		ConnectionProp defConnProp = getDefaultConnProp();
		if (defConnProp == null)
			throw new IllegalAccessException("No default Connection Properties has ben set yet.");

		// Take a copy of the current default connection props
		defConnProp = defConnProp.clone();

		// Now set the desired options
		defConnProp.setAppName(appName);

		// Adn finally make a connection attempt
		return connect(guiOwner, defConnProp);
	}

	/**
	 * Make a connection using the already set information with setDefaultConnProp()
	 * @throws Exception
	 */
	public static DbxConnection connect(Window guiOwner)
	throws Exception
	{
		return connect(guiOwner, getDefaultConnProp());
	}

	/**
	 * Make a connection using a ConnectionProp object
	 * @throws Exception
	 */
	public static DbxConnection connect(Window guiOwner, ConnectionProp connProp)
	throws Exception
	{
		String     driverClass = connProp.getDriverClass();
		String     url         = connProp.getUrl();

		String     user        = connProp.getUsername();
		String     passwd      = connProp.getPassword();
		Properties urlOptions  = connProp.getUrlOptions();
		
		String     appname     = connProp.getAppName();
		
		SshTunnelInfo sshTunnelInfo = connProp.getSshTunnelInfo();

		try
		{
			// If no suitable driver can be found for the URL, to to load it "the old fashion way" (hopefully it's in the classpath)
			try
			{
				Driver jdbcDriver = DriverManager.getDriver(url);
				if (jdbcDriver == null)
					Class.forName(driverClass).newInstance();
			}
			catch (Exception ex)
			{
				_logger.warn("Can't load JDBC driver for URL='"+url+"' using 'old way od doing it' using: DriverManager.getDriver(url); Lets continue and try just to use DriverManager.getConnection(url, props); which is the 'new' way of doing it. Caught="+ex);
				_logger.debug("Can't load JDBC driver for URL='"+url+"' using 'old way od doing it' using: DriverManager.getDriver(url); Lets continue and try just to use DriverManager.getConnection(url, props); which is the 'new' way of doing it. Caught="+ex, ex);
			}

//			Class.forName(driver).newInstance();
//			JdbcDriverHelper.newDriverInstance(driver);

			Properties props  = new Properties();
		//	Properties props2 = new Properties(); // NOTE declared at the TOP: only used when displaying what properties we connect with
			props.put("user", user);
			props.put("password", passwd);

			if (urlOptions != null)
				props.putAll(urlOptions);

//			if (StringUtil.hasValue(urlOptions))
//			{
//				Map<String, String> urlMap = StringUtil.parseCommaStrToMap(urlOptions);
//				for (String key : urlMap.keySet())
//				{
//					String val = urlMap.get(key);
//					
//					props .put(key, val);
//				}
//			}
			
			// Add specific JDBC Properties, for specific URL's, if not already specified
			if (url.startsWith("jdbc:db2:"))
			{
				if ( ! props.containsKey("retrieveMessagesFromServerOnGetMessage") )
				{
					props .put("retrieveMessagesFromServerOnGetMessage", "true");
				}
			}

			_logger.debug("getConnection to driver='"+driverClass+"', url='"+url+"', user='"+user+"'.");

			// some applications might want to have additional OPTIONS (if not already set)
			// Lets put those "default" options in (if not already set)
			// For example in 'sqlw' this might be the 'IGNORE_DONE_IN_PROC=true' 
//FIXME: we also need to check for which URL types this is valid for...
			if (StringUtil.hasValue(appname))
			{
				Properties tmpProps = _defaultAppNameProps.get(appname);
				if (tmpProps != null)
				{
					for (Object oKey : tmpProps.keySet())
					{
						if (oKey instanceof String)
						{
							String key = (String) oKey;
							String val = tmpProps.getProperty(key);
							
							if (props.getProperty(key) == null) 
								props.put(key, val);
						}
					}
				}
			}
			
// FIXME: we might need to parse the URL and split it up into multiple connection attempts (like in jConnect, when we have a "homemade" URL with multiple host1:port,host2:port)
//        Or if we do that in the ConnectionProgressDialog... we will think about that?

			Connection conn;
			if (guiOwner == null)
			{
				conn = DriverManager.getConnection(url, props);
			}
			else
			{
				ImageIcon srvIcon = ConnectionProfileManager.getIcon32byUrl(url);
				conn = ConnectionProgressDialog.connectWithProgressDialog(guiOwner, driverClass, url, props, null, null, sshTunnelInfo, null, null, srvIcon);
			}

			
//			// Execute any SQL Init 
//			if (StringUtil.hasValue(sqlInit))
//			{
//				try
//				{
//					String[] sa =  sqlInit.split(";");
//					for (String sql : sa)
//					{
//						sql = sql.trim();
//						if ("".equals(sql))
//							continue;
//						getWaitDialog().setState(
//								"<html>" +
//								"SQL Init: "+ sql + "<br>" +
//								"</html>");
//						DbUtils.exec(conn, sql);
//					}
//				}
//				catch (SQLException ex)
//				{
//					SwingUtils.showErrorMessage(ConnectionDialog.this, "SQL Initialization Failed", 
//							"<html>" +
//							"<h2>SQL Initialization Failed</h2>" +
//							"Full SQL Init String '"+ sqlInit + "'<br>" +
//							"<br>" +
//							"<b>SQL State:     </b>" + ex.getSQLState()  + "<br>" +
//							"<b>Error number:  </b>" + ex.getErrorCode() + "<br>" +
//							"<b>Error Message: </b>" + ex.getMessage()   + "<br>" +
//							"</html>",
//							ex);
//					throw ex;
//				}
//			}

			// Create a DbxConnection (if it' not already one)
			DbxConnection dbxConn = null;
			if ( conn instanceof DbxConnection )
				dbxConn = (DbxConnection) conn;
			else
				dbxConn = createDbxConnection(conn);
			
			// Set the connection properties used, so we can reconnect if we lost the connection.
System.out.println("DbxConnection.connect(): setConnProp: "+connProp);
			dbxConn.setConnProp(connProp);

			return dbxConn;
		}
		catch (SQLException ex)
		{
//			SQLException eTmp = ex;
//			StringBuffer sb = new StringBuffer();
//			while (eTmp != null)
//			{
//				sb.append( "\n" );
//				sb.append( "ex.toString='").append( ex.toString()       ).append("', ");
//				sb.append( "Driver='"     ).append( driver              ).append("', ");
//				sb.append( "URL='"        ).append( url                 ).append("', ");
//				sb.append( "User='"       ).append( user                ).append("', ");
//				sb.append( "SQLState='"   ).append( eTmp.getSQLState()  ).append("', ");
//				sb.append( "ErrorCode="   ).append( eTmp.getErrorCode() ).append(", ");
//				sb.append( "Message='"    ).append( eTmp.getMessage()   ).append("', ");
//				sb.append( "classpath='"  ).append( System.getProperty("java.class.path") ).append("'.");
//				eTmp = eTmp.getNextException();
//			}
//			_logger.info(Version.getAppName()+" - JDBC connect FAILED (catch SQLException) Caught: "+sb.toString());
//			setException(ex);
			throw ex;
		}
		catch (Exception ex)
		{
//			_logger.info(Version.getAppName()+" - JDBC connect FAILED (catch Exception) Caught: "+ex);
//			setException(ex);
			throw ex;
		}
//		return null;
		
//		// When the connection is made, refresh/get/set some basic information about the connection.  
//		getDatabaseProductName();
//		getDatabaseProductVersion();
//		getDatabaseServerName();
//		
//		getConnectionId();   // getSpid()
//		getCurrentCatalog(); // getCurrentDbname()    or maybe getCatalog() instead...
	}


	public static DbxConnection createDbxConnection(Connection conn)
	{
		if (conn == null)
			throw new IllegalArgumentException("createDbxConnection(): conn can't be null");

		// If it's already a DbxConnection, lets simply exit
		if (conn instanceof DbxConnection)
		{
new Exception("createDbxConnection(conn='"+conn+"'): is ALREADY A DbxConnection... <<<----- simply getting out of here...").printStackTrace();
			return (DbxConnection)conn;
		}

		String productName = "";

		try
		{
			productName = conn.getMetaData().getDatabaseProductName();
			_logger.debug("createDbxConnection(conn).getDatabaseProductName() returns: '"+productName+"'.");
		}
		catch (SQLException e)
		{
			// If NO metadata installed, check if it's a Sybase Replication Server.
			// JZ0SJ: Metadata accessor information was not found on this database. Please install the required tables as mentioned in the jConnect documentation.
			if ( "JZ0SJ".equals(e.getSQLState()) )
			{
				// Check for Replication Server
				try
				{
					String str1 = "";
					String str2 = "";
					Statement stmt = conn.createStatement();
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
					productName = DbUtils.DB_PROD_NAME_SYBASE_RS;
				}
				catch(SQLException ignoreRsExceptions) {}

				// Check for Replication Agent
				if (StringUtil.isNullOrBlank(productName))
				{
    				try
    				{
    					String str1 = "";
    					Statement stmt = conn.createStatement();
    					ResultSet rs = stmt.executeQuery("ra_version");
    					while ( rs.next() )
    					{
    						str1 = rs.getString(1);
    					}
    					rs.close();
    					stmt.close();
    
    					_logger.info("Replication Agent Version '"+str1+"'.");
    
    					// If the above statement succeeds, then it must be a RepServer without metadata installed.
    					productName = DbUtils.DB_PROD_NAME_SYBASE_RAX;
    				}
    				catch(SQLException ignoreRsExceptions) {}
				}

				// Check for DR Agent (Disaster Recovery AGent)
				if (StringUtil.isNullOrBlank(productName))
				{
    				try
    				{
    					String str1 = "";
    					String str2 = "";
    					Statement stmt = conn.createStatement();
    					ResultSet rs = stmt.executeQuery("sap_version");
    					while ( rs.next() )
    					{
    						str1 = rs.getString(1);
    						str2 = rs.getString(2);
    						
    						_logger.info("DR Agent Version info type='"+str1+"', version='"+str2+"'.");

    						if ("DR Agent".equals(str1))
    						{
    	    					// If the above statement succeeds, then it must be a RepServer without metadata installed.
    	    					productName = DbUtils.DB_PROD_NAME_SYBASE_RSDRA;
    						}
    					}
    					rs.close();
    					stmt.close();
    				}
    				catch(SQLException ignoreRsExceptions) {}
				}
			}
			
			if (StringUtil.isNullOrBlank(productName))
				_logger.warn("Problems getting database product name. conn='"+conn+"', Caught: "+e);
		}


		if (StringUtil.isNullOrBlank(productName))
			return new UnknownConnection(conn);

		if      (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_SYBASE_ASE))   return new AseConnection(conn);
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_SYBASE_ASA))   return new AsaConnection(conn);
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_SYBASE_IQ))    return new IqConnection(conn);
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_SYBASE_RS))    return new RsConnection(conn);
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_SYBASE_RAX))   return new RaxConnection(conn);
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_SYBASE_RSDRA)) return new RsDraConnection(conn);
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_DB2_UX))       return new Db2Connection(conn);
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_DB2_ZOS))      return new Db2Connection(conn);
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_DERBY))        return new DerbyConnection(conn);
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_H2))           return new H2Connection(conn);
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_HANA))         return new HanaConnection(conn);
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_MAXDB))        return new MaxDbConnection(conn);
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_MSSQL))        return new SqlServerConnection(conn);
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_MYSQL))        return new MySqlConnection(conn);
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_ORACLE))       return new OracleConnection(conn);
		else return new UnknownConnection(conn);
	}


	/** Set default properties for a specific application name */
	public static void setPropertiesForAppname(String appname, Map<String,String> map)
	{
		Properties props = new Properties();
		if (map != null)
			props.putAll(map);
		_defaultAppNameProps.put(appname, props);
	}
	/** Set default properties for a specific application name */
	public static void setPropertiesForAppname(String appname, Properties props)
	{
		if (props == null)
			props = new Properties();
		_defaultAppNameProps.put(appname, props);
	}
	/** Set default properties for a specific application name */
	public static void setPropertyForAppname(String appname, String propname, String propValue)
	{
		Properties props = _defaultAppNameProps.get(appname);
		if (props == null)
			props = new Properties();
		props.put(propname, propValue);
		_defaultAppNameProps.put(appname, props);
	}

	public static Properties getPropertiesForAppname(String appname) 
	{ 
		return _defaultAppNameProps.get(appname); 
	}

	/** Some application names might want to have some specific properties added, if not already in the URL */
	private static HashMap<String, Properties> _defaultAppNameProps = new HashMap<String, Properties>();

	
	
	//----------------------------------------------------------------------------------------------
	//----------------------------------------------------------------------------------------------
	// Normal methods
	//----------------------------------------------------------------------------------------------
	//----------------------------------------------------------------------------------------------
	
	public void clearCachedValues()
	{
		_databaseProductName    = null;
		_databaseProductVersion = null;
		_databaseServerName     = null;
		_dbmsVersionNumber      = -1;
		_dbmsVersionStr         = null;  // more or less the same as _databaseProductVersion, but ASE 16 is not returning the FULL string, so this is a wrapper around  

		_dbmsCharsetName        = null;
		_dbmsCharsetId          = null;
		_dbmsSortOrderName      = null;
		_dbmsSortOrderId        = null;
	}

	/**
	 * Reconnect to the server
	 * @throws Exception
	 */
	public void reConnect(Window guiOwner)
	throws Exception
	{
//		throw new Exception("NOT YET IMPLEMENTED");

		ConnectionProp connProp = getConnProp();
		if (connProp == null)
			connProp = getDefaultConnProp();
		
		if (connProp == null)
			throw new Exception("This connection doesn't have any Connection Properties set, so It's impossible to reconnect.");

		DbxConnection newConn = connect( guiOwner, connProp );

		// Check if it's the same DbxConnection subclass
		if ( ! this.getClass().getName().equals( newConn.getClass().getName() ) )
			throw new Exception("Connection succeeded, but It's not the same subclass as previously... Can't continue. thisClass='"+this.getClass().getName()+"', newClass='"+newConn.getClass().getName()+"'.");
		
		// Set the internal Connection again
		_conn = newConn._conn;
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
	public String getDbmsServerName() 
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
		// HANA
		else if (DbUtils.DB_PROD_NAME_HANA.equals(currentDbProductName))
		{
			serverName = DbUtils.getHanaServername(_conn);
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

	public String getDbmsCharsetName()
	throws SQLException
	{
		if (_dbmsCharsetName != null)
			return _dbmsCharsetName;
		
		if (_conn == null)
			return null;
		
		String charsetName = "";
		String currentDbProductName = getDatabaseProductName();

		// FIXME: move the below code to it's individual extended classes 

		// ASE
		if      (DbUtils.DB_PROD_NAME_SYBASE_ASE.equals(currentDbProductName))
		{
			charsetName = AseConnectionUtils.getAseCharset(_conn);
		}
		// ASA SQL Anywhere
		else if (DbUtils.DB_PROD_NAME_SYBASE_ASA.equals(currentDbProductName))
		{
			charsetName = AseConnectionUtils.getAsaCharset(_conn);
		}
		// Sybase IQ
		else if (DbUtils.DB_PROD_NAME_SYBASE_IQ.equals(currentDbProductName))
		{
			charsetName = AseConnectionUtils.getAsaCharset(_conn);
		}
		// Replication Server
		else if (DbUtils.DB_PROD_NAME_SYBASE_RS.equals(currentDbProductName))
		{
			charsetName = RepServerUtils.getRsCharset(_conn);
		}
		// HANA
		else if (DbUtils.DB_PROD_NAME_HANA.equals(currentDbProductName))
		{
		}
		// H2
		else if (DbUtils.DB_PROD_NAME_H2.equals(currentDbProductName))
		{
		}
		// ORACLE
		else if (DbUtils.DB_PROD_NAME_ORACLE.equals(currentDbProductName))
		{
			charsetName = DbUtils.getOracleCharset(_conn);
		}
		// Microsoft
		else if (DbUtils.DB_PROD_NAME_MSSQL.equals(currentDbProductName))
		{
		}
		// UNKNOWN
		else
		{
		}

		_dbmsCharsetName = charsetName;
		return charsetName;
	}

	public String getDbmsCharsetId()
	throws SQLException
	{
		if (_dbmsCharsetId != null)
			return _dbmsCharsetId;
		
		if (_conn == null)
			return null;
		
		String charsetId = "";
		String currentDbProductName = getDatabaseProductName();

		// FIXME: move the below code to it's individual extended classes 

		// ASE
		if      (DbUtils.DB_PROD_NAME_SYBASE_ASE.equals(currentDbProductName))
		{
			charsetId = AseConnectionUtils.getAseCharsetId(_conn);
		}
		// ASA SQL Anywhere
		else if (DbUtils.DB_PROD_NAME_SYBASE_ASA.equals(currentDbProductName))
		{
		}
		// Sybase IQ
		else if (DbUtils.DB_PROD_NAME_SYBASE_IQ.equals(currentDbProductName))
		{
		}
		// Replication Server
		else if (DbUtils.DB_PROD_NAME_SYBASE_RS.equals(currentDbProductName))
		{
		}
		// HANA
		else if (DbUtils.DB_PROD_NAME_HANA.equals(currentDbProductName))
		{
		}
		// H2
		else if (DbUtils.DB_PROD_NAME_H2.equals(currentDbProductName))
		{
		}
		// ORACLE
		else if (DbUtils.DB_PROD_NAME_ORACLE.equals(currentDbProductName))
		{
		}
		// Microsoft
		else if (DbUtils.DB_PROD_NAME_MSSQL.equals(currentDbProductName))
		{
		}
		// UNKNOWN
		else
		{
		}

		_dbmsCharsetId = charsetId;
		return charsetId;
	}

	public String getDbmsSortOrderName() 
	throws SQLException
	{
		if (_dbmsSortOrderName != null)
			return _dbmsSortOrderName;
		
		if (_conn == null)
			return null;
		
		String sortOrderName = "";
		String currentDbProductName = getDatabaseProductName();

		// FIXME: move the below code to it's individual extended classes 

		// ASE
		if      (DbUtils.DB_PROD_NAME_SYBASE_ASE.equals(currentDbProductName))
		{
			sortOrderName = AseConnectionUtils.getAseSortorder(_conn);
		}
		// ASA SQL Anywhere
		else if (DbUtils.DB_PROD_NAME_SYBASE_ASA.equals(currentDbProductName))
		{
			sortOrderName = AseConnectionUtils.getAsaSortorder(_conn);
		}
		// Sybase IQ
		else if (DbUtils.DB_PROD_NAME_SYBASE_IQ.equals(currentDbProductName))
		{
			sortOrderName = AseConnectionUtils.getAsaSortorder(_conn);
		}
		// Replication Server
		else if (DbUtils.DB_PROD_NAME_SYBASE_RS.equals(currentDbProductName))
		{
			sortOrderName = RepServerUtils.getRsSortorder(_conn);
		}
		// HANA
		else if (DbUtils.DB_PROD_NAME_HANA.equals(currentDbProductName))
		{
		}
		// H2
		else if (DbUtils.DB_PROD_NAME_H2.equals(currentDbProductName))
		{
		}
		// ORACLE
		else if (DbUtils.DB_PROD_NAME_ORACLE.equals(currentDbProductName))
		{
			sortOrderName = DbUtils.getOracleSortorder(_conn);
		}
		// Microsoft
		else if (DbUtils.DB_PROD_NAME_MSSQL.equals(currentDbProductName))
		{
		}
		// UNKNOWN
		else
		{
		}

		_dbmsSortOrderName = sortOrderName;
		return sortOrderName;
	}

	public String getDbmsSortOrderId() 
	throws SQLException
	{
		if (_dbmsSortOrderId != null)
			return _dbmsSortOrderId;
		
		if (_conn == null)
			return null;
		
		String sortOrderId = "";
		String currentDbProductName = getDatabaseProductName();

		// FIXME: move the below code to it's individual extended classes 

		// ASE
		if      (DbUtils.DB_PROD_NAME_SYBASE_ASE.equals(currentDbProductName))
		{
			sortOrderId = AseConnectionUtils.getAseSortorderId(_conn);
		}
		// ASA SQL Anywhere
		else if (DbUtils.DB_PROD_NAME_SYBASE_ASA.equals(currentDbProductName))
		{
		}
		// Sybase IQ
		else if (DbUtils.DB_PROD_NAME_SYBASE_IQ.equals(currentDbProductName))
		{
		}
		// Replication Server
		else if (DbUtils.DB_PROD_NAME_SYBASE_RS.equals(currentDbProductName))
		{
		}
		// HANA
		else if (DbUtils.DB_PROD_NAME_HANA.equals(currentDbProductName))
		{
		}
		// H2
		else if (DbUtils.DB_PROD_NAME_H2.equals(currentDbProductName))
		{
		}
		// ORACLE
		else if (DbUtils.DB_PROD_NAME_ORACLE.equals(currentDbProductName))
		{
		}
		// Microsoft
		else if (DbUtils.DB_PROD_NAME_MSSQL.equals(currentDbProductName))
		{
		}
		// UNKNOWN
		else
		{
		}

		_dbmsSortOrderId = sortOrderId;
		return sortOrderId;
	}
	
	/**
	 * more or less the same as getDatabaseProductVersion()<br>
	 * But ASE 16.x is not returning the <b>long/full</b> string, so this is a wrapper around getDatabaseProductVersion() 
	 * @return the <i>long</i> version string from the DBMS
	 * @throws SQLException
	 */
	public String getDbmsVersionStr() 
	throws SQLException
	{
		if (_dbmsVersionStr != null)
			return _dbmsVersionStr;
		
		if (_conn == null)
			return null;
		
		String versionStr = "";
		String currentDbProductName = getDatabaseProductName();

		// FIXME: move the below code to it's individual extended classes 

		// ASE
		if      (DbUtils.DB_PROD_NAME_SYBASE_ASE.equals(currentDbProductName))
		{
			versionStr = AseConnectionUtils.getAseVersionStr(_conn);
		}
//		// ASA SQL Anywhere
//		else if (DbUtils.DB_PROD_NAME_SYBASE_ASA.equals(currentDbProductName))
//		{
//		}
//		// Sybase IQ
//		else if (DbUtils.DB_PROD_NAME_SYBASE_IQ.equals(currentDbProductName))
//		{
//		}
//		// Replication Server
//		else if (DbUtils.DB_PROD_NAME_SYBASE_RS.equals(currentDbProductName))
//		{
//		}
//		// H2
//		else if (DbUtils.DB_PROD_NAME_H2.equals(currentDbProductName))
//		{
//		}
//		// ORACLE
//		else if (DbUtils.DB_PROD_NAME_ORACLE.equals(currentDbProductName))
//		{
//		}
//		// Microsoft
//		else if (DbUtils.DB_PROD_NAME_MSSQL.equals(currentDbProductName))
//		{
//		}
		// UNKNOWN
		else
		{
			versionStr = getDatabaseProductVersion();
		}

		_dbmsVersionStr = versionStr;
		return versionStr;
	}
	
	public int getDbmsVersionNumber()
	{
		if (_dbmsVersionNumber != -1)
			return _dbmsVersionNumber;
		
		if (_conn == null)
			return -1;
		
		int dbmsVersionNumber = -1;
		String currentDbProductName = "";
		try 
		{ 
			currentDbProductName = getDatabaseProductName(); 
		}
		catch (SQLException ex)
		{
			_logger.warn("getDbmsVersionNumber(): Problems calling getDatabaseProductName(), returning -1, caught: "+ex);
			return -1;
		}

		// FIXME: move the below code to it's individual extended classes 

		// ASE
		if      (DbUtils.DB_PROD_NAME_SYBASE_ASE.equals(currentDbProductName))
		{
			dbmsVersionNumber = AseConnectionUtils.getAseVersionNumber(_conn);
		}
		// ASA SQL Anywhere
		else if (DbUtils.DB_PROD_NAME_SYBASE_ASA.equals(currentDbProductName))
		{
			dbmsVersionNumber = AseConnectionUtils.getAsaVersionNumber(_conn);
		}
		// Sybase IQ
		else if (DbUtils.DB_PROD_NAME_SYBASE_IQ.equals(currentDbProductName))
		{
			dbmsVersionNumber = AseConnectionUtils.getIqVersionNumber(_conn);
		}
		// Replication Server
		else if (DbUtils.DB_PROD_NAME_SYBASE_RS.equals(currentDbProductName))
		{
			dbmsVersionNumber = AseConnectionUtils.getRsVersionNumber(_conn);
		}
		// HANA
		else if (DbUtils.DB_PROD_NAME_HANA.equals(currentDbProductName))
		{
			dbmsVersionNumber = DbUtils.getHanaVersionNumber(_conn);
		}
		// H2
		else if (DbUtils.DB_PROD_NAME_H2.equals(currentDbProductName))
		{
		}
		// ORACLE
		else if (DbUtils.DB_PROD_NAME_ORACLE.equals(currentDbProductName))
		{
			dbmsVersionNumber = DbUtils.getOracleVersionNumber(_conn);
		}
		// Microsoft
		else if (DbUtils.DB_PROD_NAME_MSSQL.equals(currentDbProductName))
		{
//			dbmsVersionNumber = DbUtils.getSqlServerVersionNumber(_conn);
		}
		// UNKNOWN
		else
		{
		}

		_dbmsVersionNumber = dbmsVersionNumber;
		return dbmsVersionNumber;
	}

	//#################################################################################
	//#################################################################################
	//### BEGIN: delegated methods for Connection
	//#################################################################################
	//#################################################################################
	
	protected Connection _conn;

	public DbxConnection(Connection conn)
	{
		_conn = conn;
	}


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
//		return _conn.createStatement();
		return DbxStatement.create( _conn.createStatement() );
	}

	@Override
	public PreparedStatement prepareStatement(String sql) throws SQLException
	{
//		return _conn.prepareStatement(sql);
		return DbxPreparedStatement.create( _conn.prepareStatement(sql) );
	}

	@Override
	public CallableStatement prepareCall(String sql) throws SQLException
	{
//		return _conn.prepareCall(sql);
		return DbxCallableStatement.create( _conn.prepareCall(sql) );
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
		clearCachedValues();
	}

	@Override
	public boolean isClosed() throws SQLException
	{
		return _conn.isClosed();
	}

	@Override
	public DatabaseMetaData getMetaData() throws SQLException
	{
		return DbxDatabaseMetaData.create( _conn.getMetaData() );
//		return _conn.getMetaData();
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

	
	
	
	
	//#################################################################################
	//#################################################################################
	//### BEGIN: some XXXXX methods
	//#################################################################################
	//#################################################################################

	/**
	 * If the server handles databases like MS SQL_Server and Sybase ASE
	 * @return true or false
	 */
	public boolean isDatabaseAware()
	{
		return false;
	}

	/**
	 * Get information object about the connection status.<br>
	 * This could/will be used to check if the connection for example:
	 * <ul>
	 *   <li>Are we in a transaction that hasn't been committed</li>
	 *   <li>Holds any locks</li>
	 *   <li>What is the effective user</li>
	 *   <li>etc etc...</li>
	 * </ul>
	 * The returned object is used by any client that want's to check "various stuff" of the connection<br>
	 * The content of the object will vary from database vendor<br>
	 * For the moment one of the clients is: SQL Window 
	 *  
	 * @return
	 */
	public DbxConnectionStateInfo getConnectionStateInfo()
	{
		return _connStateInfo;
	}
	public void setConnectionStateInfo(DbxConnectionStateInfo csi)
	{
		_connStateInfo = csi;
	}
	public abstract DbxConnectionStateInfo refreshConnectionStateInfo();

	private DbxConnectionStateInfo _connStateInfo = null;
//	public boolean isConnectionStateNormal()
//	{
//		DbxConnectionStateInfo csi = getConnectionStateInfo();
//		if (csi == null)
//			return true;
//		return csi.isNormalState();
//	}

	
	
	
	public abstract boolean isInTransaction() throws SQLException;

	public boolean isDbmsClusterEnabled()
	{
		return false;
	}

	
//	public abstract int    getDbmsVersionNumber();
//	public abstract String getDbmsVersionString();
//
//	public abstract boolean hasDbmsAuthorization(String authName);
//	public abstract String getDbmsListeners();
//
//	public abstract String getDbmsCharset();
//	public abstract String getDbmsSortorder();
//	public abstract int    getDbmsClientCharsetId();
//	public abstract String getDbmsClientCharsetName();
//	public abstract String getDbmsClientCharsetDesc();
//
//	public abstract String getDbmsLogFileName();
//	public abstract String getObjectText();
////	public abstract String getDatabaseServerName();
////	public abstract String getDatabaseProductName();

	//#################################################################################
	//#################################################################################
	//### END: some XXXXX methods
	//#################################################################################
	//#################################################################################
}
