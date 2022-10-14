/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
 * 
 * This file is part of DbxTune
 * DbxTune is a family of sub-products *Tune, hence the Dbx
 * Here are some of the tools: AseTune, IqTune, RsTune, RaxTune, HanaTune, 
 *          SqlServerTune, PostgresTune, MySqlTune, MariaDbTune, Db2Tune, ...
 * 
 * DbxTune is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executor;

import javax.swing.ImageIcon;

import org.apache.log4j.Logger;

import com.asetune.gui.ConnectionDialog;
import com.asetune.gui.ConnectionProfileManager;
import com.asetune.gui.ConnectionProgressDialog;
import com.asetune.gui.swing.WaitForExecDialog;
import com.asetune.sql.JdbcUrlParser;
import com.asetune.sql.ResultSetMetaDataCached;
import com.asetune.sql.conn.info.DbmsVersionInfo;
import com.asetune.sql.conn.info.DbmsVersionInfoUnknown;
import com.asetune.sql.conn.info.DbxConnectionStateInfo;
import com.asetune.sql.ddl.DbmsDdlResolverAnsiSql;
import com.asetune.sql.ddl.DbmsDdlResolverAsa;
import com.asetune.sql.ddl.DbmsDdlResolverAse;
import com.asetune.sql.ddl.DbmsDdlResolverDb2;
import com.asetune.sql.ddl.DbmsDdlResolverDerby;
import com.asetune.sql.ddl.DbmsDdlResolverH2;
import com.asetune.sql.ddl.DbmsDdlResolverHana;
import com.asetune.sql.ddl.DbmsDdlResolverIq;
import com.asetune.sql.ddl.DbmsDdlResolverMaxDb;
import com.asetune.sql.ddl.DbmsDdlResolverMySql;
import com.asetune.sql.ddl.DbmsDdlResolverOracle;
import com.asetune.sql.ddl.DbmsDdlResolverPostgres;
import com.asetune.sql.ddl.DbmsDdlResolverRax;
import com.asetune.sql.ddl.DbmsDdlResolverRs;
import com.asetune.sql.ddl.DbmsDdlResolverRsDa;
import com.asetune.sql.ddl.DbmsDdlResolverRsDra;
import com.asetune.sql.ddl.DbmsDdlResolverSqlServer;
import com.asetune.sql.ddl.IDbmsDataTypeResolver;
import com.asetune.sql.ddl.IDbmsDdlResolver;
import com.asetune.ssh.SshTunnelInfo;
import com.asetune.ui.autocomplete.completions.TableExtraInfo;
import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.Configuration;
import com.asetune.utils.DbUtils;
import com.asetune.utils.H2UrlHelper;
import com.asetune.utils.RepServerUtils;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;
import com.asetune.utils.Ver;
import com.asetune.utils.VersionShort;
import com.sybase.jdbcx.SybConnection;

public abstract class DbxConnection
implements Connection, AutoCloseable
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

	public enum MarkTypes
	{
		MarkForReConnect
	};

	protected ConnectionProp _connProp = null;
	protected static ConnectionProp _defaultConnProp = null;
	
	private   long   _connectTime = 0;
	
	protected String _databaseProductName     = null;
	protected String _databaseProductVersion  = null;
	protected String _databaseServerName      = null;
	protected long   _dbmsVersionNumber       = -1;
	protected String _dbmsVersionStr          = null;
	protected int    _dbmsSessionId           = -1;
	
	protected String _dbmsPageSizeInKb        = null;
	protected String _dbmsCharsetName         = null;
	protected String _dbmsCharsetId           = null;
	protected String _dbmsSortOrderName       = null;
	protected String _dbmsSortOrderId         = null;
	
	protected String _dbIdentifierQuoteString = null;
	protected String _dbExtraNameCharacters   = null;
	
	protected DbmsVersionInfo _dbmsVersionInfo = null;

	/** When did we connect. 
	 * @return 0 if no connection has been made, otherwise the 'long' from System.currentTimeMillis()
	 */
	public long getConnectTime() { return _connectTime; }
	
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

	public ConnectionProp getConnPropOrDefault() 
	{
		if (_connProp != null)
			return _connProp;
		return _defaultConnProp;
	}
	
//	public void setUsername(String username) { _username = username; }
//	public void setPassword(String password) { _password = password; }
//	public void setServer  (String server)   { _server   = server; }
//	public void setDbname  (String dbname)   { _dbname   = dbname; }
//	public void setDriver  (String driver)   { _driver   = driver; }
//	public void setUrl     (String url)      { _url      = url; }

	/**
	 * Get details about the version... typically it should be cast to any specific DbmsVersionInfo  (Sybase*, SqlServer, Postgres, ... etc)
	 * @return should never be null.
	 */
	public DbmsVersionInfo getDbmsVersionInfo()
	{
		if (_dbmsVersionInfo == null)
			setDbmsVersionInfo( createDbmsVersionInfo() );

		return _dbmsVersionInfo;
	}

	public void setDbmsVersionInfo(DbmsVersionInfo info) 
	{ 
		_dbmsVersionInfo = info; 
	}

	/**
	 * Any subclasses of DbxConnection MUST override this method, otherwise the ERROR message will be printed. 
	 * @return 
	 * @return
	 */
	public DbmsVersionInfo createDbmsVersionInfo()
	{ 
		String msg = "The createDbmsVersionInfo() has not been implemeted by the DbxConnection class '" + this.getClass().getSimpleName() + "', please do so!";
		_logger.error(msg, new RuntimeException(msg));
		
		return new DbmsVersionInfoUnknown(this);
	}

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
		ConnectionProp connProp = defConnProp.clone();

		// Now set the desired options
		connProp.setAppName(appName);

		// Adn finally make a connection attempt
		return connect(guiOwner, connProp);
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

//System.out.println("DbxConnection.connect(Window, ConnectionProp): connProp="+connProp);
		try
		{
			// If no suitable driver can be found for the URL, to to load it "the old fashion way" (hopefully it's in the classpath)
			try
			{
				Driver jdbcDriver = DriverManager.getDriver(url);
				if (jdbcDriver == null)
					Class.forName(driverClass).newInstance();
//System.out.println("DbxConnection.connect(Window, ConnectionProp): DriverManager.getDriver(url) --->>> jdbcDriver="+jdbcDriver);
			}
			catch (Exception ex)
			{
				_logger.warn( "Can't locate JDBC driver '"+driverClass+"' for URL='"+url+"' using 'DriverManager.getDriver(url)' Lets continue, but first try to load the class '"+driverClass+"' using 'Class.forName(driver).newInstance()' then connect to it using: DriverManager.getConnection(url, props); Caught="+ex);
				_logger.debug("Can't locate JDBC driver '"+driverClass+"' for URL='"+url+"' using 'DriverManager.getDriver(url)' Lets continue, but first try to load the class '"+driverClass+"' using 'Class.forName(driver).newInstance()' then connect to it using: DriverManager.getConnection(url, props); Caught="+ex, ex);

				try { Class.forName(driverClass).newInstance(); }
				catch( ClassNotFoundException | InstantiationException | IllegalAccessException ex2 )
				{
					_logger.warn("DriverManager.getDriver(url), threw Exception '"+ex+"', so we did 'Class.forName(driverClass).newInstance()', and that caused: "+ex2);
				}
				//JdbcDriverHelper.newDriverInstance(driverClass);
			}

//			Class.forName(driver).newInstance();
//			JdbcDriverHelper.newDriverInstance(driver);

			Properties props  = new Properties();
		//	Properties props2 = new Properties(); // NOTE declared at the TOP: only used when displaying what properties we connect with
			
			// Do this first... otherwise we might write over the below user/password
			if (urlOptions != null)
				props.putAll(urlOptions);

//System.out.println("DbxConnection-connect(): user="+user+", passwd="+(passwd==null?"-NULL-":passwd)+", connProp="+connProp);
			props.put("user", user);
			props.put("password", passwd);


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
			
			//----------------------------------------------------------------------------------
			// Add specific JDBC Properties, for specific URL's, if not already specified
			//----------------------------------------------------------------------------------
			// DB2
			if (url.startsWith("jdbc:db2:"))
			{
				if ( ! props.containsKey("retrieveMessagesFromServerOnGetMessage") )
				{
					props .put("retrieveMessagesFromServerOnGetMessage", "true");
				}
			}

			// Sybase/jConnect
			if (url.startsWith("jdbc:sybase:Tds:"))
			{
				if (StringUtil.hasValue(appname) &&  ! props.containsKey("APPLICATIONNAME") )
				{
					props .put("APPLICATIONNAME", appname);
				}
			}

			// Microsoft SQL-Server
			if (url.startsWith("jdbc:sqlserver:"))
			{
				if (StringUtil.hasValue(appname) &&  ! props.containsKey("applicationName") )
				{
					props .put("applicationName", appname);
				}

				if ( ! props.containsKey("trustServerCertificate") )
				{
					boolean trustServerCertificate = Configuration.getCombinedConfiguration().getBooleanProperty(ConnectionDialog.PROPKEY_CONN_SQLSERVER_TRUST_CERT, ConnectionDialog.DEFAULT_CONN_SQLSERVER_TRUST_CERT);
					if (trustServerCertificate) // the JDBC driver default is: trustServerCertificate=false
					{
						props .put("trustServerCertificate", "true");
						_logger.info("Adding Connection Property 'trustServerCertificate=true' when making a connection to SQL Server. This can be changed by adding '" + ConnectionDialog.PROPKEY_CONN_SQLSERVER_TRUST_CERT + "=false' in the Configuration file.");
					}
				}
			}

			_logger.debug("getConnection to driver='"+driverClass+"', url='"+url+"', user='"+user+"'.");
//System.out.println("getConnection to driver='"+driverClass+"', url='"+url+"', user='"+user+"'.");
//new Exception("DUMMY EX to get CALLSTACK").printStackTrace();

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
//System.out.println("DbxConnection.connect(Window, ConnectionProp): guiOwner=FALSE. url="+url);
				if (sshTunnelInfo != null)
				{
					String msg = "SSH Tunnel is NOT-YET-IMPLEMENTED when guiOwner is null, for now: Please pass GUI Owner for this to work.";
					System.out.println("WARNING: "+msg);
					_logger.warn(msg, new Exception(msg));
				}

				if (System.getProperty("DbxConnection.connect.nogui.print", "false").equalsIgnoreCase("true"))
					System.out.println("#### DEBUG ####: no-gui: DbxConnection.connect(): url='" + url + "', props[" + StringUtil.toCommaStr(props) + "], connProp=[" + connProp + "].");
				
				conn = DriverManager.getConnection(url, props);
			}
			else
			{
//System.out.println("DbxConnection.connect(Window, ConnectionProp): guiOwner=TRUE. url="+url);
				
//				public static DbxConnection connectWithProgressDialog(Window owner, String rawJdbcDriver, String rawJdbcUrl, Properties rawJdbcProps, ConnectionProgressExtraActions extraTasks, SshConnection sshConn, SshTunnelInfo sshTunnelInfo, String desiredDbProductName, String sqlInit, ImageIcon srvIcon)
//				public static DbxConnection connectWithProgressDialog(Window owner, String urlStr, ConnectionProgressExtraActions extraTasks, SshConnection sshConn, SshTunnelInfo sshTunnelInfo, String desiredDbProductName, String sqlInit, ImageIcon srvIcon)
				ImageIcon srvIcon = ConnectionProfileManager.getIcon32byUrl(url);
				if (url.startsWith("jdbc:sybase:Tds:"))
				{
					// Note: ASE is taking a special code path... this is especially because of SSH Tunnel...
					//       But I need to recode all this stuff to make it more "clean"
					//       Meaning NOT using the AseConnectionFactory (and multiple HostPorts 'host1:port,host2:port') stuff
					conn = ConnectionProgressDialog.connectWithProgressDialog(
							guiOwner,      // Window owner
							url,           // String urlStr
							connProp,      // ConnectionProp
							null,          // ConnectionProgressExtraActions extraTasks
							null,          // SshConnection sshConn
							sshTunnelInfo, // SshTunnelInfo sshTunnelInfo
							null,          // String desiredDbProductName
							null,          // String sqlInit
							srvIcon);      // ImageIcon srvIcon
				}
				else
				{
					conn = ConnectionProgressDialog.connectWithProgressDialog(
							guiOwner,      // Window owner
							driverClass,   // String rawJdbcDriver
							url,           // String rawJdbcUrl
							props,         // Properties rawJdbcProps
							connProp,      // ConnectionProp
							null,          // ConnectionProgressExtraActions extraTasks
							null,          // SshConnection sshConn
							sshTunnelInfo, // SshTunnelInfo sshTunnelInfo
							null,          // String desiredDbProductName
							null,          // String sqlInit
							srvIcon);      // ImageIcon srvIcon
				}
			}

//System.out.println("AFTER CONNECT: DbxConnection.connect(Window, ConnectionProp): conn="+conn);
			
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
//System.out.println("DbxConnection.connect(): setConnProp: "+connProp);
			dbxConn.setConnProp(connProp);

			// Set time when the connection was made
			dbxConn._connectTime = System.currentTimeMillis();
			
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

			// The Postgres Wire Protocol is used by some other DBMS's as well: CockroachDB, H2... 
			if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_POSTGRES))
			{
				String sql = "select version()";
				try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
				{
					// H2:          'PostgreSQL 8.2.23 server protocol using H2 1.4.200 (2019-10-14)'
					// CockroachDB: 'CockroachDB CCL v19.2.2 (x86_64-unknown-linux-gnu, built 2019/12/11 01:33:43, go1.12.12)'
					while(rs.next())
						productName = rs.getString(1);
					
					if (StringUtil.hasValue(productName))
					{
						if      (productName.indexOf(" H2 ") >= 0)       productName = DbUtils.DB_PROD_NAME_H2;
						else if (productName.startsWith("CockroachDB ")) productName = DbUtils.DB_PROD_NAME_COCKROACHDB;
						else                                             productName = DbUtils.DB_PROD_NAME_POSTGRES;
					}
				}
				catch(SQLException ex)
				{
					_logger.error("Problems when connecting to '" + productName + "', executing '" + sql + "'.");
				}
			}
		}
		catch (SQLException e)
		{
			// If NO metadata installed, check if it's a Sybase Replication Server.
			// JZ0SJ: Metadata accessory information was not found on this database. Please install the required tables as mentioned in the jConnect documentation.
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

				// Check for Data Asurance
				if (StringUtil.isNullOrBlank(productName))
				{
    				try
    				{
    					String str1 = "";
    					Statement stmt = conn.createStatement();
    					ResultSet rs = stmt.executeQuery("version");
    					while ( rs.next() )
    					{
    						str1 = rs.getString(1);
    						
        					_logger.info("Data Assurance Version '"+str1+"'.");

    						if (StringUtil.hasValue(str1))
    						{
    							if (str1.startsWith("SAP Replication Server Data Assurance"))
    								productName = DbUtils.DB_PROD_NAME_SYBASE_RSDA;
    						}
    					}
    					rs.close();
    					stmt.close();
    				}
    				catch(SQLException ignoreRsExceptions) {}
				}

				// Check for ??? ... @@version (possibly IQ or any other Sybase TDS service) 
				if (StringUtil.isNullOrBlank(productName))
				{
    				try
    				{
    					String str1 = "";
    					Statement stmt = conn.createStatement();
    					ResultSet rs = stmt.executeQuery("select @@version");
    					while ( rs.next() )
    					{
    						str1 = rs.getString(1);
    						
        					_logger.info("unknown-srv-type: @@version='"+str1+"'.");

    						if (StringUtil.hasValue(str1))
    						{
    							if (str1.startsWith("Sybase IQ/"))
    								productName = DbUtils.DB_PROD_NAME_SYBASE_IQ;

    							if (str1.startsWith("SAP IQ/"))
    								productName = DbUtils.DB_PROD_NAME_SYBASE_IQ;
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
		{
			if (conn instanceof SybConnection)
				return new TdsUnknownConnection(conn);

			return new UnknownConnection(conn);
		}

		DbxConnection retConn = null;
		if      (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_SYBASE_ASE))   retConn = new AseConnection(conn);
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_SYBASE_ASA))   retConn = new AsaConnection(conn);
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_SYBASE_IQ))    retConn = new IqConnection(conn);
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_SYBASE_RS))    retConn = new RsConnection(conn);
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_SYBASE_RAX))   retConn = new RaxConnection(conn);
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_SYBASE_RSDRA)) retConn = new RsDraConnection(conn);
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_SYBASE_RSDA))  retConn = new RsDaConnection(conn);
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_DB2_LUW))      retConn = new Db2Connection(conn);
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_DB2_ZOS))      retConn = new Db2Connection(conn);
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_DERBY))        retConn = new DerbyConnection(conn);
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_H2))           retConn = new H2Connection(conn);
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_HANA))         retConn = new HanaConnection(conn);
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_MAXDB))        retConn = new MaxDbConnection(conn);
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_MYSQL))        retConn = new MySqlConnection(conn);
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_ORACLE))       retConn = new OracleConnection(conn);
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_POSTGRES))     retConn = new PostgresConnection(conn);
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_MSSQL))        retConn = new SqlServerConnection(conn);
		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_APACHE_HIVE))  retConn = new ApacheHiveConnection(conn);
//		else if (DbUtils.isProductName(productName, DbUtils.DB_PROD_NAME_COCKROACHDB))  retConn = new CockroachDbConnection(conn);
		else
		{
			_logger.info("Creating a DbxConnection of 'UnknownConnection' for product '" + productName + "'.");
			retConn = new UnknownConnection(conn);
		}
		
		// Create the DBMS Version Info object, which holds "specifics" for every DBMS Vendor
		// for example: 
		//    * Sybase ASE will have method: isClusterEdition()
		//    * SQL-Server will have method: isAzudeDatabase(), isXxx(), etc
//		retConn.setDbmsVersionInfo( retConn.createDbmsVersionInfo() );
		
		// Return the connection
		return retConn;
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
	// Quoted Identifiers methods
	//----------------------------------------------------------------------------------------------
	//----------------------------------------------------------------------------------------------

	/** get what character to use (on the left side) of a Database Identifier that needs to be quoted, normally it's the same character on both sides */
	public String getLeftQuote()
	{
		String dbProductName = getDatabaseProductNameNoThhrow("");
		String dbQuoteString = getDbQuotedIdentifierChar();

		return (DbUtils.DB_PROD_NAME_SYBASE_ASE.equals(dbProductName) || DbUtils.DB_PROD_NAME_MSSQL.equals(dbProductName)) ? "[" : dbQuoteString; 
	}

	/** get what character to use (on the right side) of a Database Identifier that needs to be quoted, normally it's the same character on both sides */
	public String getRightQuote() 
	{ 
		String dbProductName = getDatabaseProductNameNoThhrow("");
		String dbQuoteString = getDbQuotedIdentifierChar();

		return (DbUtils.DB_PROD_NAME_SYBASE_ASE.equals(dbProductName) || DbUtils.DB_PROD_NAME_MSSQL.equals(dbProductName)) ? "]" : dbQuoteString; 
	}

	/**
	 * Helper method to Replace "fake" begin/end Quoted Identifier Chars into DBMS Vendor Specific Quoted Identifier<br>
	 * If a SQL String constant is discovered, no translation will be done inside that part of the full string<br>
	 * <br>
	 * Expected behavior, with a DBMS Quoted Identifier Character of <code>"</code>
	 * <table border=1>
	 *    <tr> <th>input</th> <th>output</th> </tr>
	 *    <tr> <td><code> select [c1] from [t1] where [c2] like 'abc%'       </code></td> <td><code> select "c1" from "t1" where "c2" like 'val%'       </code></td> </tr>
	 *    <tr> <td><code> select [c1] from [t1] where [c2] like '[abc]%'     </code></td> <td><code> select "c1" from "t1" where "c2" like '[abc]%'     </code></td> </tr>
	 *    <tr> <td><code> select [c1] from [t1] where [c2] like 'It''s [a]%' </code></td> <td><code> select "c1" from "t1" where "c2" like 'It''s [a]%' </code></td> </tr>
	 * </table>
	 * 
	 * @param sql       SQL Statement text where '[' and ']' will be replaced with DBMS Vendor Specific Quoted Identifier
	 * 
	 * @return The quotified string
	 */
	public String quotifySqlString(String sql)
	{
		return quotifySqlString(sql, '[', ']');
	}

	/**
	 * Helper method to Replace "fake" begin/end Quoted Identifier Chars into DBMS Vendor Specific Quoted Identifier<br>
	 * If a SQL String constant is discovered, no translation will be done inside that part of the full string<br>
	 * <br>
	 * Expected behavior, with a DBMS Quoted Identifier Character of <code>"</code>
	 * <table border=1>
	 *    <tr> <th>input</th> <th>output</th> </tr>
	 *    <tr> <td><code> select [c1] from [t1] where [c2] like 'abc%'       </code></td> <td><code> select "c1" from "t1" where "c2" like 'val%'       </code></td> </tr>
	 *    <tr> <td><code> select [c1] from [t1] where [c2] like '[abc]%'     </code></td> <td><code> select "c1" from "t1" where "c2" like '[abc]%'     </code></td> </tr>
	 *    <tr> <td><code> select [c1] from [t1] where [c2] like 'It''s [a]%' </code></td> <td><code> select "c1" from "t1" where "c2" like 'It''s [a]%' </code></td> </tr>
	 * </table>
	 * 
	 * @param sql       SQL Statement text where '[' and ']' will be replaced with DBMS Vendor Specific Quoted Identifier
	 * @param leftChar  character representing a "left" side of a Quoted Identifier that should be replaced (in the example above [)
	 * @param rightChar character representing a "right" side of a Quoted Identifier that should be replaced (in the example above ])
	 * 
	 * @return The quotified string
	 */
	public String quotifySqlString(String sql, char leftChar, char rightChar)
	{
		// NOTE TO MYSELF: This is to complicated... can we make it simpler... maybe with regular expression ???
		
		if (sql == null)
			return null;

		String lQic = getLeftQuote();
		String rQic = getRightQuote();

		int strLen = sql.length();
		StringBuilder sb = new StringBuilder(strLen);

		boolean inSingleQuotes = false;
		boolean inDbmsQuotes   = false;   // In DBMS Quoted Identifier Character Count
		int     leftDbmsQuoteCount  = 0;  // increment every time we see START "[" character
		
		for (int i=0; i<strLen; i++)
		{
			char c  = sql.charAt(i);

			// "flip" the boolean every time we see a "single quote char" (and we are NOT in quoted identifier)
			if (c == '\'' && !inDbmsQuotes)
				inSingleQuotes = ! inSingleQuotes;

			// increment count every time we see a "[" char (or whatever the left char replacement is) and we are NOT inside a SQL Single Quote
			if (c == leftChar && !inSingleQuotes)
			{
				inDbmsQuotes = true;
				leftDbmsQuoteCount++;
			}

			if (inSingleQuotes)
			{
				sb.append(c);
			}
			else if (inDbmsQuotes)
			{
				// nc means next char... so look ahead 1 char
				char nc = (i+1 < strLen) ? sql.charAt(i+1) : ' ';

				// If NEXT char is also a "end" character... then it's an "escaped" character
				// meaning we are seeing "]]" which is NOT the "end-char" 
				// So add CURRENT and NEXT to the output buffer, and start at the top... to continue grabbing chars... 
				if (c == rightChar && nc == rightChar)
				{
					sb.append(rightChar);
				
					// Only append the NEXT char if the method's endChar is a "]" and the destination DBMS Quoted Identifier Char is also a "]"... keep the escape char "]" 
					// OR: if the DBMS Quoted Identifier Char is NOT "]" then we should NOT append the escape char "]" and only append ONE "]"... discard the escape char "]"
					if (rightChar == ']' && rQic.equals("]"))
						sb.append(rightChar);

					i++;
					continue;
				}

				// If we have "] " ... we have a end character (which is NOT escaped)
				// This means that we are done with the Quoted Identifier
				if (c == rightChar && nc != rightChar )
				{
					inDbmsQuotes = false;
					leftDbmsQuoteCount = 0;
				}

				// if this is NOT the FIRST "[" or LAST "]" char, then simply add characters 
				if (inDbmsQuotes && leftDbmsQuoteCount > 1)
				{
					sb.append(c);
				}
				else
				{
					// So we must be at FIRST "[" or LAST "]" ... so change those chars to what the DBMS wants to have.
					if      ( c == leftChar  ) { sb.append(lQic); } // change "[" to the DBMS quoted identifier char
					else if ( c == rightChar ) { sb.append(rQic); } // change "]" to the DBMS quoted identifier char
					else                       { sb.append(c);    } // well this should never happen... but just in case... add to the buffer
				}
			}
			else
			{
				// we are NOT between singleQuotes nor between FIRST "[" or LAST "]" char, the simply add chars
				sb.append(c);
			}
		}
		
		return sb.toString();
	}
	public String quotifySqlStringXXXX(String sql, char leftChar, char rightChar)
	{
		// NOTE TO MYSELF: This is to complicated... can we make it simpler... maybe with regular expression ???
		
		if (sql == null)
			return null;

		String lq = getLeftQuote();
		String rq = getRightQuote();

		StringBuilder sb = new StringBuilder(sql.length());

		boolean inSingleQuotes = false;
		int     inDbmsQicCount = 0;  // In DBMS Quoted Identifier Character Count
		
		for (int i=0; i<sql.length(); i++)
		{
			char c  = sql.charAt(i);
			char cc = i+1<sql.length() ? sql.charAt(i+1) : ' '; // cc means next char... so look ahead 1 char

			// "flip" the boolean every time we see a "single quote char" (and we are NOT in quoted identifier)
			if (c == '\'' && inDbmsQicCount == 0)
				inSingleQuotes = ! inSingleQuotes;

			// increment count every time we see a "[" char (or whatever the left char replacement is) and we are NOT inside a SQL Single Quote
			if (c == leftChar && !inSingleQuotes)
				inDbmsQicCount++;

System.out.println("i="+i+", c='"+c+"', cc='"+cc+"', inSingleQuotes="+inSingleQuotes+", inDbmsQicCount="+inDbmsQicCount);
			if (inSingleQuotes)
			{
				sb.append(c);
			}
			else if (inDbmsQicCount > 0)
			{
				// decrement count every time we see a "]" char (or whatever the right char replacement is)
				//            but if next char is also "[" do not decrement 
				// meaning we will NOT decrement on "]]" only on "]?" (where ? is any character) 
				if (c == rightChar && cc != rightChar)
					inDbmsQicCount--;

System.out.println(" ---- i="+i+", c='"+c+"', cc='"+cc+"', inDbmsQicCount="+inDbmsQicCount);
				// If it's the FIRST or LAST character of the Quoted String then REPLACE with DBMS Quotes
				if (inDbmsQicCount == 1 || inDbmsQicCount == 0)
				{
					if      ( c == leftChar  ) { sb.append(lq); }
					else if ( c == rightChar ) { sb.append(rq); }
					else                       { sb.append(c);  }
				}
				else
				{
					sb.append(c);
				}
			}
			else
			{
//				if      ( c == leftChar  ) { sb.append(lq); }
//				else if ( c == rightChar ) { sb.append(rq); }
//				else                       { sb.append(c);  }
				sb.append(c);
			}
		}
		
		return sb.toString();
	}
	public String quotifySqlString_____OriginThatOnlyHandlesSqlSingleQuotes(String sql, char leftChar, char rightChar)
	{
		if (sql == null)
			return null;

		String lq = getLeftQuote();
		String rq = getRightQuote();

		StringBuilder sb = new StringBuilder(sql.length());

		boolean inSingleQuotes         = false;
		
		for (int i=0; i<sql.length(); i++)
		{
			char c = sql.charAt(i);

			// "flip" the boolean every time we see a "single quote char"
			if (c == '\'')
				inSingleQuotes = ! inSingleQuotes;

			if (inSingleQuotes)
			{
				sb.append(c);
			}
			else
			{
				if      ( c == leftChar  ) sb.append(lq);
				else if ( c == rightChar ) sb.append(rq);
				else                       sb.append(c);
			}
		}
		
		return sb.toString();
	}

	/**
	 * Add DBMS Vendor Specific Quoted Identifier Chars around the input string<br>
	 * 
	 * @param str one or more
	 * @return
	 */
	public String quotify(String... name)
	{
		if (name == null)
			return "";
		
		if (name.length == 0)
			return "";

		if (name.length == 1 && StringUtil.isNullOrBlank(name[0]))
			return "";
		
		StringBuilder sb = new StringBuilder();

		if (name.length == 1)
		{
			sb.append(getLeftQuote()).append(name[0]).append(getRightQuote());
		}
		else
		{
			for (String str : name)
			{
				sb.append(getLeftQuote()).append(str).append(getRightQuote()).append(", ");
			}
			// Remove last ", "
			sb.delete(sb.length()-2, sb.length());
		}
		
		return sb.toString();
	}

	/**
	 * Add DBMS Quoted Identifier Chars around the input string<br>
	 * if input string is empty it will simply be returned untouched.
	 * 
	 * @param cat   Catalog or database name
	 * @param sch   Schema name
	 * @param obj   Object name
	 * 
	 * @return
	 */
	public String quotifyObj(String cat, String sch, String obj)  
	{
		if (StringUtil.isNullOrBlank(cat) && StringUtil.isNullOrBlank(sch) && StringUtil.isNullOrBlank(obj))
			return "";
		
		StringBuilder sb = new StringBuilder();

		cat = quotify( cat );
		sch = quotify( sch );
		obj = quotify( obj );

		if (StringUtil.hasValue(cat)) sb.append(cat).append(".");
		if (StringUtil.hasValue(sch)) sb.append(sch).append(".");
		if (StringUtil.hasValue(obj)) sb.append(obj);

		return sb.toString();
	}

	/**
	 * Add DBMS Vendor Specific Quoted Identifier Chars around the input string, if it needs it...<br>
	 * if input string is empty it will simply be returned untouched.
	 * 
	 * @param str
	 * @return
	 */
	public String quotifyIfNeeded(String... name)  
	{ 
		if (name == null)
			return "";
		
		if (name.length == 0)
			return "";

		if (name.length == 1 && StringUtil.isNullOrBlank(name[0]))
			return "";
		
		// What "extra" characters does the DBMS allow in table name WITHOUT quotation
		String dbExtraNameCharacters = getDbExtraNameCharacters();

		StringBuilder sb = new StringBuilder();

		for (String str : name)
		{
			boolean normalChars = true;
			for (int i=0; i<str.length(); i++)
			{
				char c = str.charAt(i);

				// goto next character for any "allowed" characters
				if ( Character.isLetterOrDigit(c) ) continue;
				if ( c == '_' )                     continue;

				if (dbExtraNameCharacters != null && dbExtraNameCharacters.indexOf(c) >= 0) continue;

				// if any other chars, then break and signal "non-normal-char" detected
				normalChars = false;
				break;
			}

			if ( normalChars )
				sb.append(str);
			else
				sb.append(quotify(str));
				
			sb.append(", ");
		}
		// Remove last ", "
		sb.delete(sb.length()-2, sb.length());
		
		return sb.toString();
	}

	/**
	 * Add DBMS Quoted Identifier Chars around the input string, if it needs it...<br>
	 * if input string is empty it will simply be returned untouched.
	 * 
	 * @param cat   Catalog or database name
	 * @param sch   Schema name
	 * @param obj   Object name
	 * 
	 * @return
	 */
	public String quotifyObjIfNeeded(String cat, String sch, String obj)  
	{
		if (StringUtil.isNullOrBlank(cat) && StringUtil.isNullOrBlank(sch) && StringUtil.isNullOrBlank(obj))
			return "";

		StringBuilder sb = new StringBuilder();

		cat = quotifyIfNeeded( cat );
		sch = quotifyIfNeeded( sch );
		obj = quotifyIfNeeded( obj );

		if (StringUtil.hasValue(cat)) sb.append(cat).append(".");
		if (StringUtil.hasValue(sch)) sb.append(sch).append(".");
		if (StringUtil.hasValue(obj)) sb.append(obj);

		return sb.toString();
	}


	//----------------------------------------------------------------------------------------------
	//----------------------------------------------------------------------------------------------
	// Normal methods
	//----------------------------------------------------------------------------------------------
	//----------------------------------------------------------------------------------------------
	
	public void clearCachedValues()
	{
		_dbmsVersionInfo         = null;

		_databaseProductName     = null;
		_databaseProductVersion  = null;
		_databaseServerName      = null;
		_dbmsVersionNumber       = -1;
		_dbmsVersionStr          = null;  // more or less the same as _databaseProductVersion, but ASE 16 is not returning the FULL string, so this is a wrapper around  
		_dbmsSessionId           = -1;

		_dbmsCharsetName         = null;
		_dbmsCharsetId           = null;
		_dbmsSortOrderName       = null;
		_dbmsSortOrderId         = null;
		
		_dbIdentifierQuoteString = null;
		_dbExtraNameCharacters   = null;
		
		_connectTime = 0;
	}

	/**
	 * Reconnect to the server
	 * @throws Exception
	 */
	public void reConnect(Window guiOwner)
	throws Exception
	{
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
	 * Basically checks if the connection is OK, probably calls isValid(2) or isClosed()
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
		
		if (_logger.isDebugEnabled())
			_logger.debug("DbxConnection.isConnectionOk(guiMsgOnError="+guiMsgOnError+", guiOwner='"+guiOwner+"'): _conn="+_conn+", _conn.class="+(_conn==null?"-null-":_conn.getClass().getName())+", this.class="+this.getClass().getName()+", _databaseProductName='"+_databaseProductName+"'.");

		if ( _conn == null ) 
		{	
			msg = "The Connection object is null.";
			_logger.debug(msg);

			if (guiOwner != null)
				SwingUtils.showWarnMessage(guiOwner, title, msg, new Exception(msg));

			return false;
		}
		
//		try
//		{
////			if ( _conn.isClosed() )
//			/*
//			 * Note: isClosed() do not seems to have a query timeout... and in some cases it just hangs forever...
//			 *       so lets try with isValid() instead
//			 *       If this isn't good enough, lets try to fiddle around with: get/setNetworkTimeout()
//			 *
//			 * Note2: Do not do _conn.isValid(), instead do this.isValid().  
//			 *        - _conn is the Vendors implementation (for example SybConnection)
//			 *        - this is the DbxConnection where we can override isValid()
//			 */
//			if ( ! this.isValid(1) ) // timeout in seconds
//			{
//				msg = "The Connection object is NOT connected.";
//				_logger.debug(msg);
//
//				if (guiOwner != null)
//					SwingUtils.showWarnMessage(guiOwner, title, msg, new Exception(msg));
//
//				return false;
//			}
//		}
//		catch (SQLException e)
//		{
//			_logger.debug("When checking the DB Connection, Caught exception.", e);
//
//			if (guiOwner != null)
//				showSqlExceptionMessage(guiOwner, "Checking DB Connection", "When checking the DB Connection, we got an SQLException", e);
//			
//			return false;
//		}
		
		/*
		 * Note: isClosed() do not seems to have a query timeout... and in some cases it just hangs forever...
		 *       so lets try with isValid() instead
		 *       If this isn't good enough, lets try to fiddle around with: get/setNetworkTimeout()
		 *
		 * Note2: Do not do _conn.isValid(), instead do this.isValid().  
		 *        - _conn is the Vendors implementation (for example SybConnection)
		 *        - this is the DbxConnection where we can override isValid()
		 */

		
		if (guiOwner == null)
		{
			try
			{
				if ( ! this.isValid(1) ) // timeout in seconds
				{
					msg = "The Connection object is NOT connected.";
					_logger.debug(msg);

					return false;
				}
				return true;
			}
			catch (SQLException e)
			{
				_logger.debug("When checking the DB Connection, Caught exception.", e);
				return false;
			}
		}
		else
		{
			// Create a Waitfor Dialog and Executor, then execute it.
			Window owner = (Window) guiOwner;
			WaitForExecDialog wait = new WaitForExecDialog(owner, "Checking if DBMS Connection is Valid");

			WaitForExecDialog.BgExecutor doWork = new WaitForExecDialog.BgExecutor(wait)
			{
				@Override
				public Object doWork()
				{
					try
					{
						if ( ! DbxConnection.this.isValid(1) ) // timeout in seconds
						{
							String msg = "The Connection object is NOT connected.";
							_logger.debug(msg);

							if (guiMsgOnError)
								SwingUtils.showWarnMessage(guiOwner, title, msg, new Exception(msg));
							
							return false;
						}
						return true;
					}
					catch (SQLException e)
					{
						_logger.debug("When checking the DB Connection, Caught exception.", e);
						if (guiMsgOnError)
							showSqlExceptionMessage(guiOwner, "Checking DB Connection", "When checking the DB Connection, we got an SQLException", e);

						return false;
					}
				}
			}; // END: new WaitForExecDialog.BgExecutor()
			
			// Execute and WAIT
			Boolean connIsValid = (Boolean) wait.execAndWait(doWork, 300);
			return connIsValid;
		}
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

		if      (this instanceof RsConnection)    { _databaseProductName = DbUtils.DB_PROD_NAME_SYBASE_RS;    return _databaseProductName; }
		else if (this instanceof RaxConnection)   { _databaseProductName = DbUtils.DB_PROD_NAME_SYBASE_RAX;   return _databaseProductName; }
		else if (this instanceof RsDaConnection)  { _databaseProductName = DbUtils.DB_PROD_NAME_SYBASE_RSDA;  return _databaseProductName; }
		else if (this instanceof RsDraConnection) { _databaseProductName = DbUtils.DB_PROD_NAME_SYBASE_RSDRA; return _databaseProductName; }

		try
		{
			String str = _conn.getMetaData().getDatabaseProductName();
			_logger.debug("getDatabaseProductName() returns: '"+str+"'.");
			
			_databaseProductName = str;
			return str; 
		}
		catch (SQLException e)
		{
//			// If NO metadata installed, check if it's a Sybase Replication Server.
//			// JZ0SJ: Metadata accessor information was not found on this database. Please install the required tables as mentioned in the jConnect documentation.
//			if ( "JZ0SJ".equals(e.getSQLState()) )
//			{
//				try
//				{
//					String str1 = "";
//					String str2 = "";
//					Statement stmt = _conn.createStatement();
//					ResultSet rs = stmt.executeQuery("admin rssd_name");
//					while ( rs.next() )
//					{
//						str1 = rs.getString(1);
//						str2 = rs.getString(2);
//					}
//					rs.close();
//					stmt.close();
//
//					_logger.info("Replication Server with RSSD at '"+str1+"."+str2+"'.");
//
//					// If the above statement succeeds, then it must be a RepServer without metadata installed.
//					_databaseProductName = DbUtils.DB_PROD_NAME_SYBASE_RS;
//					return _databaseProductName;
//				}
//				catch(SQLException ignoreRsExceptions) {}
//			}
			_logger.debug("getDatabaseProductName() Caught: "+e, e);
			throw e;
		}
	}

	/** same as getDatabaseProductName() but instead of throwing SQLException, then use the default value */
	public String getDatabaseProductNameNoThhrow(String defaultValueOnException) 
	{
		try
		{
			return getDatabaseProductName();
		}
		catch (SQLException ex)
		{
			return defaultValueOnException;
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
			if (this instanceof RsConnection)    
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
					return _databaseProductVersion;
				}
				catch(SQLException ignoreRsExceptions) {}
			}
			else if (this instanceof RaxConnection)
			{
				try
				{
					String str1 = "";
					Statement stmt = _conn.createStatement();
					ResultSet rs = stmt.executeQuery("ra_version");
					while ( rs.next() )
					{
						str1 = rs.getString(1);
					}
					rs.close();
					stmt.close();

					_logger.info("Replication Agent Version '"+str1+"'.");

					// If the above statement succeeds, then it must be a RepServer without metadata installed.
					_databaseProductVersion = str1;
					return _databaseProductVersion;
				}
				catch(SQLException ignoreRsExceptions) {}
			}
			else if (this instanceof RsDaConnection)
			{
				try
				{
					String str1 = "";
					Statement stmt = _conn.createStatement();
					ResultSet rs = stmt.executeQuery("version");
					while ( rs.next() )
					{
						str1 = rs.getString(1);
						
    					_logger.info("Data Assurance Version '"+str1+"'.");

						if (StringUtil.hasValue(str1))
						{
							if (str1.startsWith("SAP Replication Server Data Assurance"))
								_databaseProductVersion = str1;
						}
					}
					rs.close();
					stmt.close();
					return _databaseProductVersion;
				}
				catch(SQLException ignoreRsExceptions) {}
			}
			else if (this instanceof RsDraConnection)
			{
				try
				{
					String str1 = "";
					String str2 = "";
					Statement stmt = _conn.createStatement();
					ResultSet rs = stmt.executeQuery("sap_version");
					while ( rs.next() )
					{
						str1 = rs.getString(1);
						str2 = rs.getString(2);
						
						_logger.info("DR Agent Version info type='"+str1+"', version='"+str2+"'.");

						if ("DR Agent".equals(str1))
						{
	    					// If the above statement succeeds, then it must be a RepServer without metadata installed.
							_databaseProductVersion = str2;
						}
					}
					rs.close();
					stmt.close();
					return _databaseProductVersion;
				}
				catch(SQLException ignoreRsExceptions) {}
			}

//			// If NO metadata installed, check if it's a Sybase Replication Server.
//			// JZ0SJ: Metadata accessor information was not found on this database. Please install the required tables as mentioned in the jConnect documentation.
//			if ( "JZ0SJ".equals(e.getSQLState()) )
//			{
//				try
//				{
//					String str = "";
//					Statement stmt = _conn.createStatement();
//					ResultSet rs = stmt.executeQuery("admin version");
//					while ( rs.next() )
//					{
//						str = rs.getString(1);
//					}
//					rs.close();
//					stmt.close();
//
//					_logger.info("Replication Server with Version string '"+str+"'.");
//
//					// If the above statement succeeds, then it must be a RepServer without metadata installed.
//					_databaseProductVersion = str;
//					return str;
//				}
//				catch(SQLException ignoreRsExceptions) {}
//			}
			_logger.debug("getDatabaseProductVersion() Caught: "+e, e);
			throw e;
		}
	}

	/**
	 * Check if current connected product name is equal to the input parameter
	 * @param str Name of the product to test for
	 * @return true if equal
	 */
	public boolean isDatabaseProduct(String... names)
	{
		String currentDbProductName = null;
		try
		{
			currentDbProductName = getDatabaseProductName();
		}
		catch (SQLException e)
		{
			_logger.debug("isDatabaseProduct() Caught: "+e, e);
			return false;
		}
		
		return DbUtils.isProductName(currentDbProductName, names);
	}

//	public boolean isDatabaseProduct(String str)
//	{
//		if (str == null)
//			return false;
//
//		try
//		{
//			String currentDbProductName = getDatabaseProductName();
//			return str.equals(currentDbProductName);
//		}
//		catch (SQLException e)
//		{
//			_logger.debug("isDatabaseProduct() Caught: "+e, e);
//			return false;
//		}
//	}

	/**
	 * Get the session id of the connection to the DBMS
	 * 
	 * @return -1 if not known
	 */
	protected abstract int getDbmsSessionId_impl()
	throws SQLException;

	/**
	 * Get the session id of the connection to the DBMS
	 * 
	 * @return -1 if not known
	 */
	public int getDbmsSessionId() 
	{
		if (_dbmsSessionId != -1)
			return _dbmsSessionId;

		try
		{
			_dbmsSessionId = getDbmsSessionId_impl();
		}
		catch (SQLException ex)
		{
			_logger.error("getDbmsSessionId(): returning blank-string, Caught: " + ex);
			_dbmsSessionId = -1;
		}
		return _dbmsSessionId;
	}

	/**
	 * Get the connected database server/instance name.
	 * @return null if not connected else: Retrieves the name of this database server/instance name.
	 * @see java.sql.DatabaseMetaData.getDatabaseProductName
	 */
	public String getDbmsServerNameNoThrow() 
	{
		try
		{
			return getDbmsServerName();
		}
		catch (SQLException ex)
		{
			_logger.error("getDbmsServerNameNoThrow(): returning blank-string, Caught: " + ex);
			return "";
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
			H2UrlHelper urlHelper = new H2UrlHelper(_conn.getMetaData().getURL());
			if ( "file".equals(urlHelper.getUrlType()) ) serverName = "LOCAL-FILE:" + urlHelper.getFile().getName();
			if ( "tcp" .equals(urlHelper.getUrlType()) ) serverName = urlHelper.getUrlTcpHostPort();
			if ( "ssl" .equals(urlHelper.getUrlType()) ) serverName = urlHelper.getUrlTcpHostPort();
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
		// Postgres == overridden in PostgresConnection
		// MySQL    == overridden in MySqlConnection
		// DB2      == overridden in Db2Connection
		// UNKNOWN
		else
		{
			// Should we use the URL from the Jdbc Connection...
			String jdbcUrl = _conn.getMetaData().getURL();
			JdbcUrlParser urlParser = JdbcUrlParser.parse(jdbcUrl);
			
			_databaseServerName = urlParser.getHostPortStr();
		}

		_databaseServerName = serverName;
		return serverName;
	}

	public String getDbmsPageSizeInKb()
	throws SQLException
	{
		if (_dbmsPageSizeInKb != null)
			return _dbmsPageSizeInKb;

		String pageSizeInKb = "";
		String currentDbProductName = getDatabaseProductName();

		// FIXME: move the below code to it's individual extended classes 

		// ASE
		if      (DbUtils.DB_PROD_NAME_SYBASE_ASE.equals(currentDbProductName))
		{
			int pageSize = AseConnectionUtils.getAsePageSize(_conn);
			pageSizeInKb = pageSize <= 0 ? "" : AseConnectionUtils.getAsePageSize(_conn)/1024 + "";
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
		// MySQL
		else if (DbUtils.DB_PROD_NAME_MYSQL.equals(currentDbProductName))
		{
			// pageSizeInKb = "16"; // for InnoDB, but I dont know how to *really* check it...
		}
		// Microsoft
		else if (DbUtils.DB_PROD_NAME_MSSQL.equals(currentDbProductName))
		{
			pageSizeInKb = "8";
		}
		// UNKNOWN
		else
		{
		}

		_dbmsPageSizeInKb = pageSizeInKb;
		return pageSizeInKb;
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
		// Microsoft
		else if (DbUtils.DB_PROD_NAME_MSSQL.equals(currentDbProductName))
		{
			versionStr = AseConnectionUtils.getAseVersionStr(_conn);
		}
		// DB2
		else if (DbUtils.isProductName(currentDbProductName, DbUtils.DB_PROD_NAME_DB2_LUW, DbUtils.DB_PROD_NAME_DB2_ZOS))
		{
			versionStr = DbUtils.getDb2VersionStr(_conn);
		}
		// UNKNOWN
		else
		{
			versionStr = getDatabaseProductVersion();
		}

		_dbmsVersionStr = versionStr;
		return versionStr;
	}
	
	public long getDbmsVersionNumber()
	{
		if (_dbmsVersionNumber != -1)
			return _dbmsVersionNumber;
		
		if (_conn == null)
			return -1;
		
		long dbmsVersionNumber = -1;
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
			// H2 -- override in H2Connection
		}
		// ORACLE
		else if (DbUtils.DB_PROD_NAME_ORACLE.equals(currentDbProductName))
		{
			dbmsVersionNumber = DbUtils.getOracleVersionNumber(_conn);
		}
		// Microsoft -- override in SqlServerConnection
//		else if (DbUtils.DB_PROD_NAME_MSSQL.equals(currentDbProductName))
//		{
//			dbmsVersionNumber = DbUtils.getSqlServerVersionNumber(_conn);
//		}
		// DB2
		else if (DbUtils.isProductName(currentDbProductName, DbUtils.DB_PROD_NAME_DB2_LUW, DbUtils.DB_PROD_NAME_DB2_ZOS))
		{
			dbmsVersionNumber = DbUtils.getDb2VersionNumber(_conn);
		}
		// UNKNOWN
		else
		{
			try
			{
				// Get the version string and convert it into a "short int" xx.yy.zz -> xxyyzz
				String verStr = getDatabaseProductVersion();
				int shortVerInt = VersionShort.parse(verStr);

				// Make the "short int version" a "longer int version" 11.2.3 -> 110203 -> 112300000
				// The "long" is the version we use in DbxTune... (in the future it might be even larger, (11020300000) 11 02 03 000 00  major minor main sp pl
				dbmsVersionNumber = Ver.shortVersionStringToNumber(shortVerInt);
				
				// No Warning message for Postgres
				if (DbUtils.DB_PROD_NAME_POSTGRES.equals(currentDbProductName)) {} 
				// If 'unknown' then write warning message so we can see if it's "parsed" correctly for that product. When it's verified we can add a line in the above if statement
				else
					_logger.info("getDbmsVersionNumber(): Unhandled ProductName='"+currentDbProductName+"' with VersionString='"+verStr+"' parsed into: shortVerInt="+shortVerInt+", dbmsVersionNumber="+dbmsVersionNumber);
			}
			catch (SQLException ex)
			{
				_logger.warn("getDbmsVersionNumber(): Problems getting version string from getDatabaseProductVersion()");
			}
		}

		_dbmsVersionNumber = dbmsVersionNumber;
		return dbmsVersionNumber;
	}

	/**
	 * Returns a LinkedHashMap<key=String, val=Object> with "extra" information from the dbms instance
	 * @return
	 */
	public Map<String, Object> getDbmsExtraInfo()
	{
		return null;
	}
	
	
	/**
	 * Return various extra information about a Table<br>
	 * This information can for example be used by a ToolTip when displaying information on the table.<br>
	 * For example
	 * <ul>
	 *      <li>TableRowCount</li>
	 *      <li>TableTotalSizeInMb</li>
	 *      <li>TableDataSizeInMb</li>
	 *      <li>TableIndexSizeInMb</li>
	 *      <li>TableLobSizeInMb</li>
	 * </ul>
	 * 
	 * @return a Map with various extra table information. 
	 */
	public Map<String, TableExtraInfo> getTableExtraInfo(String cat, String schema, String table)
	{
		return null;
	}
	/**
	 * Return a list of objects that the view references<br>
	 * 
	 * @return a List of strings with references objects. 
	 */
	public List<String> getViewReferences(String cat, String schema, String table)
	{
		return null;
	}

	
	
	/**
	 * Close the connection quitetly
	 */
	public void closeNoThrow()
	{
		try
		{
			close();
		}
		catch(SQLException ex)
		{
			if (_logger.isDebugEnabled())
				_logger.debug("Problems closing connection, Caught: "+ex);
		}
	}
	
	
	//#################################################################################
	//#################################################################################
	//### BEGIN: Some generic helper methods for executing SQL Statemenets
	//#################################################################################
	//#################################################################################
	
	/**
	 * Execute a SQL Statement thats doesnt return any ResultSet, and does not throw any exception
	 * 
	 * @param sql SQL To be executed
	 * @return > 0 on success, -1 on failure
	 */
	public int dbExecNoException(String sql)
	{
		try
		{
			return dbExec(sql, true);
		}
		catch (SQLException e)
		{
			return -1;
		}
	}

	/**
	 * Execute a SQL Statement thats doesnt return any ResultSet, and does not throw any exception
	 * 
	 * @param sql SQL To be executed
	 * @return true on success, false on failure
	 * 
	 * @throws SQLException
	 */
	public int dbExec(String sql)
	throws SQLException
	{
		return dbExec(sql, true);
	}

	/**
	 * Execute a SQL Statement thats doesnt return any ResultSet, and does not throw any exception
	 * 
	 * @param sql           SQL To be executed
	 * @param printErrors   If we should print errors to the error log
	 * @return true on success, false on failure
	 * 
	 * @throws SQLException
	 */
	public int dbExec(String sql, boolean printErrors)
	throws SQLException
	{
		if (_logger.isDebugEnabled())
		{
			_logger.debug("SEND SQL: " + sql);
		}
//System.out.println("dbExec(): SEND SQL: " + sql);

		try
		{
			int count = 0;
			Statement s = createStatement();
			s.execute(sql);
			count = s.getUpdateCount();
			s.close();
			
			return count;
		}
		catch(SQLException e)
		{
			if (printErrors)
				_logger.warn("Problems when executing sql statement: "+sql+" SqlException: ErrorCode="+e.getErrorCode()+", SQLState="+e.getSQLState()+", toString="+e.toString());
			throw e;
		}
	}
	//#################################################################################
	//#################################################################################
	//### END: Some generic helper methods for executing SQL Statemenets
	//#################################################################################
	//#################################################################################

	
	
	
	
	@Override
	public String toString()
	{
		return getClass().getName() + "@" + Integer.toHexString(hashCode()) + "[_conn=" + _conn + "]";
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
		clearCachedValues();
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

	//#######################################################
	//############################# JDBC 4.1
	//#######################################################
	
	@Override
	public void abort(Executor executor) throws SQLException
	{
		_conn.abort(executor);
	}

	@Override
	public int getNetworkTimeout() throws SQLException
	{
		return _conn.getNetworkTimeout();
	}

	@Override
	public String getSchema() throws SQLException
	{
		return _conn.getSchema();
	}

	@Override
	public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException
	{
		_conn.setNetworkTimeout(executor, milliseconds);
	}

	@Override
	public void setSchema(String schema) throws SQLException
	{
		_conn.setSchema(schema);
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
					"<font size='4'>" +
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
	 * If the server handles databases like MS SQL_Server and Sybase ASE return true
	 * @return true or false
	 */
	public boolean isDatabaseAware()
	{
		return false;
	}

//	getQuotedIdentifierChar
	public String getDbQuotedIdentifierChar()
//	public String getQuotedIdentifierChar()
	{
		// return at once if the string is cached
		if (_dbIdentifierQuoteString != null)
			return _dbIdentifierQuoteString;

		// Get the data if it wasn't cached
		try 
		{
			DatabaseMetaData md = getMetaData();
			_dbIdentifierQuoteString = md.getIdentifierQuoteString();
		}
		catch (SQLException ex) 
		{
			_logger.warn("Problems when getting Quoted Identifier. returning char '\"'. DatabaseMetaData.getIdentifierQuoteString() caught: "+ex);
			return "\"";
		}
		
		return _dbIdentifierQuoteString;
	}

	public String getDbExtraNameCharacters()
	{
		// return at once if the string is cached
		if (_dbExtraNameCharacters != null)
			return _dbExtraNameCharacters;

		// Get the data if it wasn't cached
		try 
		{
			DatabaseMetaData md = getMetaData();
			_dbExtraNameCharacters = md.getExtraNameCharacters();
		}
		catch (SQLException ex) 
		{
			_logger.warn("Problems when getting Extra Name Characters for without Quoted Identifier. returning char ''. DatabaseMetaData.getExtraNameCharacters() caught: "+ex);
			return "";
		}
		
		return _dbExtraNameCharacters;
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

	/**
	 * Get active server roles or Permissions that the current user has in the current DBMS
	 * @param conn
	 * @return null if not known, otherwise a list of strings.
	 */
	public List<String> getActiveServerRolesOrPermissions()
	{
		return null;
	}

	/** internally used to store Connection Markers */
	private Set<MarkTypes> _connectionMarkers = new LinkedHashSet<MarkTypes>();
	
	/** Set a specific "marker" at the connection level */
	public void setConnectionMark(MarkTypes markType)
	{
		_connectionMarkers.add(markType);
	}
//	/** Get a specific "marker" object at the connection level */
//	public MarkTypes getConnectionMark(MarkTypes markType)
//	{
//		return _connectionMarkers.get(markType);
//	}
	/** Check if a specific "marker" object at the connection level is set */
	public boolean isConnectionMarked(MarkTypes markType)
	{
		return _connectionMarkers.contains(markType);
	}
	/** Clear a specific "marker" object at the connection level */
	public void clearConnectionMark(MarkTypes markType)
	{
		_connectionMarkers.remove(markType);
	}

	//-------------------------------------------------
	// BEGIN: SCHEMA methods
	//-------------------------------------------------
	/**
	 * Just checks if a schema exists
	 * @param schemaName
	 * @throws SQLException 
	 */
	public boolean checkIfSchemaExists(String schemaName) throws SQLException
	{
		boolean exists = false;

		DatabaseMetaData md = getMetaData();
		
//		String qic = md.getIdentifierQuoteString();
//		if (StringUtil.isNullOrBlank(qic))
//			qic = "\"";

		ResultSet rs = md.getSchemas();
		while(rs.next())
		{
			String schema = rs.getString(1);
			if (schemaName.equalsIgnoreCase(schema))
			{
				exists = true;
				break;
			}
		}
		rs.close();
		
		return exists;
	}

	/** 
	 * Create a schema 
	 * @param schemaName
	 * @throws SQLException 
	 */
	public void createSchema(String schemaName) throws SQLException
	{
//		String qic = getMetaData().getIdentifierQuoteString();
		dbExec("create schema " + getLeftQuote() + schemaName + getRightQuote());
	}

	/**
	 * Create a schema if not already exists<br>
	 * This will call:
	 * <ul>
	 *   <li>checkIfSchemaExists(schemaName)</li>
	 *   <li>createSchema(schemaName)</li>
	 * </ul>
	 * 
	 * This can typically be overloaded with something like (below if the database supports the syntax)
	 * <pre>
	 * public void createSchemaIfNotExists(String schemaName) throws SQLException
	 * {
	 * 	dbExec("create schema " + getLeftQuote() + schemaName + getRightQuote());
	 * }
	 * </pre>
	 * @param schemaName
	 * @throws SQLException 
	 */
	public void createSchemaIfNotExists(String schemaName) throws SQLException
	{
		if (checkIfSchemaExists(schemaName))
			return;
		
		createSchema(schemaName);
	}

	/** 
	 * Drop a schema 
	 * @param schemaName
	 * @throws SQLException 
	 */
	public void dropSchema(String schemaName) throws SQLException
	{
//		String qic = getMetaData().getIdentifierQuoteString();
		dbExec("drop schema " + getLeftQuote() + schemaName + getRightQuote());
	}

	//-------------------------------------------------
	// END: SCHEMA methods
	//-------------------------------------------------

	
	
	
	//-------------------------------------------------
	// BEGIN: PROPERTIES methods
	//-------------------------------------------------
	private HashMap<String, Object> _propsMap = new HashMap<>();

	/** Check if a Property exists */
	public boolean hasProperty(String key)
	{
		return _propsMap.containsKey(key);
	}
	
	/** set/assign a property value */
	public void setProperty(String key, Object value)
	{
		_propsMap.put(key, value);
	}
	
	/** get a property value */
	public Object getProperty(String key, Object defaultValue)
	{
		if (_propsMap.containsKey(key))
			return _propsMap.get(key);
		else
			return defaultValue;
	}
	
	/** get the Map that holds "properties" used by hasProperty(), setProperty(), getProperty() */
	public Map<String, Object> getPropertyMap()
	{
		return _propsMap;
	}
	//-------------------------------------------------
	// END: PROPERTIES methods
	//-------------------------------------------------


	//-------------------------------------------------
	// BEGIN: DBMS DATA-TYPE & DDL RESOLVER methods
	//-------------------------------------------------
	private IDbmsDataTypeResolver _dbmsDataTypeResolver;
	private IDbmsDdlResolver      _dbmsDdlResolver;

	/**
	 * Get a DDL Resolver that will help you to figure out various database specific stuff when you want to create a DDL statement that is specific for for this DBMS Vendor.
	 * @return never NULL, and the desired resolver for that DBMS Vendor
	 */
	public IDbmsDataTypeResolver getDbmsDataTypeResolver()
	{
		if (_dbmsDataTypeResolver == null)
			_dbmsDataTypeResolver = createDbmsDataTypeResolver();
		
		return _dbmsDataTypeResolver;
	}
	public IDbmsDdlResolver getDbmsDdlResolver()
	{
		if (_dbmsDdlResolver == null)
			_dbmsDdlResolver = createDbmsDdlResolver();
		
		return _dbmsDdlResolver;
	}

	public IDbmsDataTypeResolver createDbmsDataTypeResolver()
	{
		return (IDbmsDataTypeResolver) createDbmsDdlResolver(); // note: for the moment DbmsDdlResolver*** implements both: IDbmsDdlResolver and IDbmsDataTypeResolver
	}
	/**
	 * This will create a instance of DbmsDdlResolver based on what DbxConnection you are currently using
	 * 
	 * @return
	 */
	public IDbmsDdlResolver createDbmsDdlResolver()
	{
		if (this instanceof AsaConnection      ) return new DbmsDdlResolverAsa      (this);
		if (this instanceof AseConnection      ) return new DbmsDdlResolverAse      (this);
		if (this instanceof Db2Connection      ) return new DbmsDdlResolverDb2      (this);
		if (this instanceof DerbyConnection    ) return new DbmsDdlResolverDerby    (this);
		if (this instanceof H2Connection       ) return new DbmsDdlResolverH2       (this);
		if (this instanceof HanaConnection     ) return new DbmsDdlResolverHana     (this);
		if (this instanceof IqConnection       ) return new DbmsDdlResolverIq       (this);
		if (this instanceof MaxDbConnection    ) return new DbmsDdlResolverMaxDb    (this);
		if (this instanceof MySqlConnection    ) return new DbmsDdlResolverMySql    (this);
		if (this instanceof OracleConnection   ) return new DbmsDdlResolverOracle   (this);
		if (this instanceof PostgresConnection ) return new DbmsDdlResolverPostgres (this);
		if (this instanceof RaxConnection      ) return new DbmsDdlResolverRax      (this);
		if (this instanceof RsConnection       ) return new DbmsDdlResolverRs       (this);
		if (this instanceof RsDaConnection     ) return new DbmsDdlResolverRsDa     (this);
		if (this instanceof RsDraConnection    ) return new DbmsDdlResolverRsDra    (this);
		if (this instanceof SqlServerConnection) return new DbmsDdlResolverSqlServer(this);

		// For unknown/unmapped Connections lets use the "ANSI"
		_logger.warn("No DBMS DDL Resolver for DbxConnetion implementation '" + this.getClass().getName() + "', DBMS Vendor '" + this.getDatabaseProductNameNoThhrow(this.getClass().getSimpleName()) + "' was found. Using 'ANSI SQL' instead.");
		return new DbmsDdlResolverAnsiSql(this);
	}

	public static IDbmsDataTypeResolver createDbmsDataTypeResolver(String dbmsProductName)
	{
		return (IDbmsDataTypeResolver) createDbmsDdlResolver(dbmsProductName); // note: for the moment DbmsDdlResolver*** implements both: IDbmsDdlResolver and IDbmsDataTypeResolver
	}
	/**
	 * NOT SURE IF WE SHOULD CREATE THIS OR NOT, it will/may create problems due to: null is passed as a DbxConnection so we can't really do much (lookups: get QuotedIdentifierChar() etc...)
	 * This will create a instance of DbmsDdlResolver based on what dbmsProductName (get from Connection.getMetaData().getDatabaseProductName()) you are currently using
	 * <p>
	 * Or DbUtils.DB_PROD_NAME_xxxxx
	 * 
	 * @return
	 */
	public static IDbmsDdlResolver createDbmsDdlResolver(String dbmsProductName)
	{
		if (DbUtils.isProductName(dbmsProductName, DbUtils.DB_PROD_NAME_SYBASE_ASA  )) return new DbmsDdlResolverAsa      (null);
		if (DbUtils.isProductName(dbmsProductName, DbUtils.DB_PROD_NAME_SYBASE_ASE  )) return new DbmsDdlResolverAse      (null);
		if (DbUtils.isProductName(dbmsProductName, DbUtils.DB_PROD_NAME_DB2_LUW     )) return new DbmsDdlResolverDb2      (null);
		if (DbUtils.isProductName(dbmsProductName, DbUtils.DB_PROD_NAME_DB2_ZOS     )) return new DbmsDdlResolverDb2      (null);
		if (DbUtils.isProductName(dbmsProductName, DbUtils.DB_PROD_NAME_DERBY       )) return new DbmsDdlResolverDerby    (null);
		if (DbUtils.isProductName(dbmsProductName, DbUtils.DB_PROD_NAME_H2          )) return new DbmsDdlResolverH2       (null);
		if (DbUtils.isProductName(dbmsProductName, DbUtils.DB_PROD_NAME_HANA        )) return new DbmsDdlResolverHana     (null);
		if (DbUtils.isProductName(dbmsProductName, DbUtils.DB_PROD_NAME_SYBASE_IQ   )) return new DbmsDdlResolverIq       (null);
		if (DbUtils.isProductName(dbmsProductName, DbUtils.DB_PROD_NAME_MAXDB       )) return new DbmsDdlResolverMaxDb    (null);
		if (DbUtils.isProductName(dbmsProductName, DbUtils.DB_PROD_NAME_MYSQL       )) return new DbmsDdlResolverMySql    (null);
		if (DbUtils.isProductName(dbmsProductName, DbUtils.DB_PROD_NAME_ORACLE      )) return new DbmsDdlResolverOracle   (null);
		if (DbUtils.isProductName(dbmsProductName, DbUtils.DB_PROD_NAME_POSTGRES    )) return new DbmsDdlResolverPostgres (null);
		if (DbUtils.isProductName(dbmsProductName, DbUtils.DB_PROD_NAME_SYBASE_RS   )) return new DbmsDdlResolverRs       (null);
		if (DbUtils.isProductName(dbmsProductName, DbUtils.DB_PROD_NAME_SYBASE_RSDA )) return new DbmsDdlResolverRsDa     (null);
		if (DbUtils.isProductName(dbmsProductName, DbUtils.DB_PROD_NAME_SYBASE_RSDRA)) return new DbmsDdlResolverRsDra    (null);
		if (DbUtils.isProductName(dbmsProductName, DbUtils.DB_PROD_NAME_MSSQL       )) return new DbmsDdlResolverSqlServer(null);

		// For unknown/unmapped Connections lets use the "ASNI"
		_logger.warn("No DBMS DDL Resolver for DBMS Vendor '" + dbmsProductName + "' was found. Using 'ANSI SQL' instead.");
		return new DbmsDdlResolverAnsiSql(null);
	}
	
	public static List<String> getDbmsDdlSupportedVendors()
	{
		List<String> list = new ArrayList<>();
		
		list.add(DbUtils.DB_PROD_NAME_SYBASE_ASA  );
		list.add(DbUtils.DB_PROD_NAME_SYBASE_ASE  );
		list.add(DbUtils.DB_PROD_NAME_DB2_LUW     );
		list.add(DbUtils.DB_PROD_NAME_DB2_ZOS     );
		list.add(DbUtils.DB_PROD_NAME_DERBY       );
		list.add(DbUtils.DB_PROD_NAME_H2          );
		list.add(DbUtils.DB_PROD_NAME_HANA        );
		list.add(DbUtils.DB_PROD_NAME_SYBASE_IQ   );
		list.add(DbUtils.DB_PROD_NAME_MAXDB       );
		list.add(DbUtils.DB_PROD_NAME_MYSQL       );
		list.add(DbUtils.DB_PROD_NAME_ORACLE      );
		list.add(DbUtils.DB_PROD_NAME_POSTGRES    );
		list.add(DbUtils.DB_PROD_NAME_SYBASE_RS   );
		list.add(DbUtils.DB_PROD_NAME_SYBASE_RSDA );
		list.add(DbUtils.DB_PROD_NAME_SYBASE_RSDRA);
		list.add(DbUtils.DB_PROD_NAME_MSSQL       );
		
		return list;
	}

	/**
	 * Simply calls <code>ResultSetMetaDataCached.createNormalizedRsmd(originRsmd, getDatabaseProductNameNoThhrow("DbxConnection")</code>
	 * @param originRsmd
	 * @return
	 */
	public ResultSetMetaDataCached createNormalizedRsmd(ResultSetMetaDataCached originRsmd)
	{
		return ResultSetMetaDataCached.createNormalizedRsmd(originRsmd, getDatabaseProductNameNoThhrow("DbxConnection"));
	}

	/**
	 * Simply calls <code>ResultSetMetaDataCached.createNormalizedRsmd(originRs)</code>
	 * @param originRs
	 * @return
	 */
	public ResultSetMetaDataCached createNormalizedRsmd(ResultSet originRs)
	throws SQLException
	{
		return ResultSetMetaDataCached.createNormalizedRsmd(originRs);
	}

	/**
	 * Simply calls <code>ResultSetMetaDataCached.createTargetRsmd(sourceRsmd, getDatabaseProductNameNoThhrow("DbxConnection")</code>
	 * @param sourceRsmd
	 * @return
	 */
	public ResultSetMetaDataCached createTargetRsmd(ResultSetMetaDataCached sourceRsmd)
	{
		return ResultSetMetaDataCached.transformToTargetDbms(sourceRsmd, getDatabaseProductNameNoThhrow("DbxConnection"));
	}

	//-------------------------------------------------
	// END: DBMS DDL RESOLVER methods
	//-------------------------------------------------

	/**
	 * Get Row Count Estimate for a specific table.
	 * <p>
	 * This returns -1 if it's not supported be the implementation, OR it can't find the information in the DBMS dictionary
	 * <p>
	 * Known Implementations
	 * <ul>
	 *    <li>H2</li>
	 *    <li>Postgres</li>
	 *    <li>Sybase ASE</li>
	 *    <li>Microsoft SQL-Server</li>
	 *    <li>MySQL</li>
	 *    <li>DB2</li>
	 * </ul>
	 * @param catalog   name of the catalog
	 * @param schema    name of the schema
	 * @param table     name of the table
	 * @return
	 */
	public long getRowCountEstimate(String catalog, String schema, String table)
	throws SQLException
	{
		return -1;
	}

	/**
	 * Get Row Count Actual for a specific table. 
	 * <p>
	 * WARNING: This does select count(*) from table
	 * <p>
	 * This returns -1 if it's not supported be the implementation
	 * 
	 * @param catalog   name of the catalog
	 * @param schema    name of the schema
	 * @param table     name of the table
	 * @return
	 */
	public long getRowCountActual(String catalog, String schema, String table)
	throws SQLException
	{
		long rowCount = -1;
		
		String cat = StringUtil.isNullOrBlank(catalog) ? "" : "[" + catalog + "]";
		String sch = StringUtil.isNullOrBlank(schema)  ? "" : "[" + schema  + "]";
		String tab = StringUtil.isNullOrBlank(table)   ? "" : "[" + table   + "]";
		
		String sql = "select count(*) from " + cat + sch + tab;
		sql = this.quotifySqlString(sql);
		
		try (Statement stmnt = this.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while(rs.next())
				rowCount = rs.getLong(1);
		}
		
		return rowCount;
	}

	
	//----------------------------------------------------------------------------------------------
	//----------------------------------------------------------------------------------------------
	// BEGIN: Object name "resolvers"
	//----------------------------------------------------------------------------------------------
	//----------------------------------------------------------------------------------------------
	
	/**
	 * Build a DBMS dependent table name WITH DBMS Quoted Identifiers, for example:
	 * <ul>
	 *     <li>Sybase ASE:    <code>[catalog].[schema].[table]</code></li>
	 *     <li>MS SQL_Server: <code>[catalog].[schema].[table]</code></li>
	 *     <li>Postgres       <code>"schema"."table"</code>    -- CAT is stripped out</li>
	 *     <li>MySQL:         <code>`catalog`.`table`</code>   -- SCHEMA is stripped out</li>
	 * </ul>
	 * 
	 * @param catalogName
	 * @param schemaName
	 * @param tableName
	 * @return
	 */
	public String getFullTableName(String catalogName, String schemaName, String tableName)
	{
		return getFullTableName(catalogName, schemaName, tableName, true);
	}
	/**
	 * Build a DBMS dependent table name, for example:
	 * <ul>
	 *     <li>Sybase ASE:    <code>[catalog].[schema].[table]</code></li>
	 *     <li>MS SQL_Server: <code>[catalog].[schema].[table]</code></li>
	 *     <li>Postgres       <code>"schema"."table"</code>    -- CAT is stripped out</li>
	 *     <li>MySQL:         <code>`catalog`.`table`</code>   -- SCHEMA is stripped out</li>
	 * </ul>
	 * 
	 * @param catalogName
	 * @param schemaName
	 * @param tableName
	 * @param addQuotes  Should we use Quoted Identifiers
	 * @return
	 */
	public String getFullTableName(String catalogName, String schemaName, String tableName, boolean addQuotes)
	{
		StringBuilder sb = new StringBuilder();

		// Simplify null handling
		if (catalogName == null) catalogName = "";
		if (schemaName  == null) schemaName  = "";
		if (tableName   == null) tableName   = "";

		catalogName = catalogName.trim();
		schemaName  = schemaName .trim();
		tableName   = tableName  .trim();

		// If 'null' was passed in, simply "strip" that off
		if (catalogName.equalsIgnoreCase("null")) catalogName = "";
		if (schemaName .equalsIgnoreCase("null")) schemaName  = "";
		if (tableName  .equalsIgnoreCase("null")) tableName   = "";
		
		// Should we use Quoted Identifiers
		String lq = "";
		String rq = "";
		
		if (addQuotes)
		{
			lq = getLeftQuote();
			rq = getRightQuote();
		}
		
		// Do not use Catalog name for H2
		if (isDatabaseProduct(DbUtils.DB_PROD_NAME_H2))
		{
			catalogName = "";
		}

		// Do not use Catalog name for Postgres
		if (isDatabaseProduct(DbUtils.DB_PROD_NAME_POSTGRES))
		{
			catalogName = "";
		}
		
		// MySQL: only use Catalog OR Schema name (MySQL; has Database, but NOT schema)
		if (isDatabaseProduct(DbUtils.DB_PROD_NAME_MYSQL))
		{
			// If catalogName is BLANK but schemaName HAS VALUE ... move it to catalogName
			if (StringUtil.isNullOrBlank(catalogName) && StringUtil.hasValue(schemaName))
			{
				catalogName = schemaName;
				schemaName = "";
			}

			// if both(catalog & schema) has values, TRUST catalogName
			if (StringUtil.hasValue(catalogName) && StringUtil.hasValue(schemaName))
				schemaName = "";
		}
		
		
		// CATALOG
		if (StringUtil.hasValue(catalogName))
		{
			sb.append(lq).append(catalogName).append(rq).append(".");
		}
		
		// SCHEMA
		if (StringUtil.hasValue(schemaName))
		{
			sb.append(lq).append(schemaName).append(rq).append(".");
		}
		
		// TABLENAME
		sb.append(lq).append(tableName).append(rq);
		
		return sb.toString();
	}

	/**
	 * The SQL value that the JDBC driver maps boolean values to<br>
	 * Inspired by Hibernate Dialect: https://appdoc.app/artifact/org.hibernate/hibernate/3.2.0.cr3/org/hibernate/dialect/Dialect.html#toBooleanValueString(boolean)
	 * <p>
	 * TODO: This should really be implemented as a Separate class "Dialect", and then we should do conn.getDialect().toBooleanValueString(true|false)
	 * 
	 * @param bool    
	 * @return
	 */
	public String toBooleanValueString(boolean bool)
	{
		return bool ? "1" : "0";
	}

	/**
	 * Generate SQL String for bitwise AND operation (Inspired by Hibernate Dialect)
	 * @param x1   Expression 1 
	 * @param x2   Expression 1
	 * @return (x1 & x2)
	 */
	public String toBitAnd(String x1, int x2)
	{
		return toBitAnd(x1, Integer.toString(x2));
	}
	/**
	 * Generate SQL String for bitwise AND operation (Inspired by Hibernate Dialect)
	 * @param x1   Expression 1 
	 * @param x2   Expression 1
	 * @return (x1 & x2)
	 */
	public String toBitAnd(String x1, String x2)
	{
		return "(" + x1 + " & " + x2 + ")";
	}
	
	
//	/**
//	 * Return a DBMS data type for this DBMS (default is to use the same logic as in ResultSetTableModel.getColumnTypeName
//	 * <p>
//	 * But override this to be more DBMS agnostic
//	 * 
//	 * @param rsmd
//	 * @param col
//	 * 
//	 * @return a String with DBMS data type. For example: <code>int</code>, <code>varchar(30)</code> or <code>numeric(10,2)</code>
//	 */
//	public String getColumnTypeName(ResultSetMetaData rsmd, int col)
//	{
//		return ResultSetTableModel.getColumnTypeName(rsmd, col);
//	}

//	/**
//	 * Checks if the currect database connection is of the product name<br>
//	 * This uses: <code>DbUtils.isProductName(this.getDatabaseProductName(), dbProdNameOracle);</code>
//	 * 
//	 * @param dbProdNameOracle  
//	 * @return true or false
//	 */
//	public boolean isDatabaseProductName(String dbProdNameOracle)
//	{
//		try
//		{
//			return DbUtils.isProductName(this.getDatabaseProductName(), dbProdNameOracle);
//		}
//		catch (SQLException ex)
//		{
//			_logger.info("isDatabaseProductName() caught: "+ex);
//			return false;
//		}
//	}

	
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
