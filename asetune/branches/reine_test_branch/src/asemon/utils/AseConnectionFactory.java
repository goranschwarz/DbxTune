/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package asemon.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.sybase.util.ds.interfaces.SyInterfacesDriver;
import com.sybase.util.ds.interfaces.SyInterfacesEntry;

public class AseConnectionFactory
{
	private static Logger _logger = Logger.getLogger(AseConnectionFactory.class);

	private static String     _driver   = "com.sybase.jdbc3.jdbc.SybDriver";
	private static String     _urlTemplate = "jdbc:sybase:Tds:HOST:PORT";
	private static String     _host     = "";
	private static int        _port     = -1;
	private static String     _server   = "";
	private static String     _username = "";
	private static String     _password = "";
	private static String     _appname  = "";
	private static Properties _props = new Properties();

	private static SyInterfacesDriver _interfacesDriver = null;

	public static void reset()
	{
		_host     = "";
		_port     = -1;
		_server   = "";
		_username = "";
		_password = "";
		_appname  = "";
		_props = new Properties();
	}

	public static boolean setInterfaces(String file)
	throws Exception
	{
		try 
		{
			SyInterfacesDriver newDriver = new SyInterfacesDriver(file);
			if ( newDriver != null )
			{
				_interfacesDriver = newDriver;
				_logger.info("Just opened the interfaces file '"+ _interfacesDriver.getBundle() +"'.");
				return true;
			}
			return false;
		}
		catch(Exception ex)
		{
			_logger.error("Problems reading interfaces or sql.ini file.", ex);
			throw ex;
		}
	}

	public static void setProperties(Properties props)
	{
		_props = props;
	}
	public static void setProperty(String propname, Object prop)
	{
		_props.put(propname, prop);
	}
	public static void setDriver(String driver)
	{
		_driver = driver;
	}
	public static void setUrl(String urlTemplate)
	{
		_urlTemplate = urlTemplate;
	}
	public static void setUser(String username)
	{
		_username = username;
	}
	public static void setPassword(String password)
	{
		_password = password;
	}
	public static void setHost(String host)
	{
		_host = host;
		_server = getServerName(_host, _port); 
	}
	public static void setPort(int port)
	{
		_port = port;
		_server = getServerName(_host, _port); 
	}
	public static void setServer(String servername)
	{
		_server = servername;
	}
	public static void setAppName(String appname)
	{
		_appname = appname;
	}
	
	public static Properties getProperties() { return _props; }
	public static Object     getProperty(String propname) { return _props.getProperty(propname); }
	public static String     getDriver()     { return _driver; }
	public static String     getUrl()        { return _urlTemplate; }
	public static String     getUser()       { return _username; }
	public static String     getPassword()   { return _password; }
	public static String     getHost()       { return _host; }
	public static int        getPort()       { return _port; }
	public static String     getServer()     { return _server; }
	public static String     getAppName()    { return _appname; }

	public static String     getIFileName()
	{
		return _interfacesDriver.getBundle();
	}

	public static String getHost(String server)
	{
		SyInterfacesEntry interfaceEntry = _interfacesDriver.getEntry(server);
		
		return (interfaceEntry == null) ? null : interfaceEntry.getHost();
	}

	public static String getPortStr(String server)
	{
		SyInterfacesEntry interfaceEntry = _interfacesDriver.getEntry(server);
		
		return (interfaceEntry == null) ? null : interfaceEntry.getPort();
	}

	public static int getPort(String server)
	{
		SyInterfacesEntry interfaceEntry = _interfacesDriver.getEntry(server);
		
		return (interfaceEntry == null) ? -1 : Integer.parseInt( interfaceEntry.getPort() );
	}

	public static String[] getServers()
	{
		String[] servers = _interfacesDriver.getServers();
		if (servers != null)
			Arrays.sort(servers);
		return servers;
	}

	public static String     getServerName(String host, int port)
	{
		if (host == null || (host != null && host.trim().equals("")) || port <= 0)
			return null;

		if (_interfacesDriver == null)
			return null;

		Enumeration en = _interfacesDriver.entries();
		while (en.hasMoreElements())
		{
			SyInterfacesEntry ie = (SyInterfacesEntry) en.nextElement();
			
			String eHost = ie.getHost();
			String ePort = ie.getPort();

			if (_logger.isTraceEnabled())
				_logger.trace("InterfaceEntry: "+ie.toString());

			if (eHost != null && ePort != null)
			{
				if (eHost.equalsIgnoreCase(host) && ePort.equals(Integer.toString(port)))
				{
					_logger.debug("Found Sybase Server Interface Entry '"+ie.getName()+"' for host '"+eHost+"', port '"+ePort+"'.");
					return ie.getName();
				}
			}
		}
		return "";
	}
	
	public static Connection getConnection(String dbname, String appname) 
	throws ClassNotFoundException, SQLException
	{
		return getConnection(_host, _port, dbname, _username, _password, appname);
	}
	public static Connection getConnection(String dbname) 
	throws ClassNotFoundException, SQLException
	{
		return getConnection(_host, _port, dbname, _username, _password, _appname);
	}
	public static Connection getConnection() 
	throws ClassNotFoundException, SQLException
	{
		return getConnection(_host, _port, null, _username, _password, _appname);
	}
	public static Connection getConnection(String host, int port, String dbname, String username, String password, String appname) 
	throws ClassNotFoundException, SQLException
	{
		String url = _urlTemplate;
		url = url.replaceAll("HOST", host);
		url = url.replaceAll("PORT", Integer.toString(port));

		Properties props = new Properties(_props);
		if (props.getProperty("user")            == null) props.put("user",            username);
		if (props.getProperty("password")        == null) props.put("password",        password);
		if (props.getProperty("APPLICATIONNAME") == null) props.put("APPLICATIONNAME", appname);

		if (dbname != null && !dbname.trim().equals(""))
			url += "/"+dbname;

		return getConnection(_driver, url, props);
	}

	public static Connection getConnection(String url, String username, String password) 
	throws ClassNotFoundException, SQLException
	{
		Properties props = new Properties(_props);
		if (props.getProperty("user")            == null) props.put("user",            username);
		if (props.getProperty("password")        == null) props.put("password",        password);
		if (props.getProperty("APPLICATIONNAME") == null) props.put("APPLICATIONNAME", _appname);
		if (props.getProperty("DISABLE_UNPROCESSED_PARAM_WARNINGS") == null) props.put("DISABLE_UNPROCESSED_PARAM_WARNINGS", "true");

		return getConnection(_driver, url, props);
	}

	public static Connection getConnection(String driverClassName, String url, Properties props) 
	throws ClassNotFoundException, SQLException
	{
		// Look up the JDBC driver by class name.  When the class loads, it
		// automatically registers itself with the DriverManager used in
		// the next step.
		Class.forName(driverClassName);

		// Now use that driver to connect to the database
		return DriverManager.getConnection(url, props);

//		JDBCLoginService svc = new JDBCLoginService(driverClassName, url);
//		JXLoginPane.Status status = JXLoginPane.showLoginDialog(null, svc);
//		if (status == JXLoginPane.Status.SUCCEEDED) 
//		{
//			System.out.println("Login Succeeded!");
//		} 
//		else 
//		{
//			System.out.println("Login Failed: " + status);
//		}
//		return svc.getConnection();
	}

	
	public static String resolvInterfaceEntry(String serverName)
	{
		return resolvInterfaceEntry(serverName, null);
	}
	public static String resolvInterfaceEntry(String serverName, String iniFile)
	{
		String ret = null;
		
		if (serverName == null)
		{
			throw new RuntimeException("OpenConnectionDlg.resolvInterfaceEntry(serverName), can't be null.");
		}

		SyInterfacesDriver iniDriver = null;
		try 
		{
			if ( iniFile != null && ! iniFile.trim().equals("") )
			{
				_logger.debug("Trying to open SyInterfacesDriver with file '"+iniFile+"'.");
				iniDriver = new SyInterfacesDriver(iniFile);
			}
			else
			{
				_logger.debug("Trying to open SyInterfacesDriver with NO FILE, using the SyInterfacesDriver default.");
				iniDriver = new SyInterfacesDriver();
			}

			if ( iniDriver != null )
			{
				iniDriver.open();

				_logger.debug("Just opened the interfaces file '"+ iniDriver.getBundle() +"'.");
	
				SyInterfacesEntry interfaceEntry = iniDriver.getEntry(serverName);
				
				if (interfaceEntry != null)
				{
					ret = interfaceEntry.getHost() + ":" + interfaceEntry.getPort();
				}
			}
		}
		catch(Exception ex)
		{
			if ( System.getProperty("os.name").startsWith("Windows"))
				_logger.error("Problems reading '%SYBASE%\\ini\\sql.ini' file.");
			else
				_logger.error("Problems reading '$SYBASE/interfaces' file.");
			_logger.error("SyInterfacesDriver error: "+ex.getMessage());
		}
		
		return ret;
	}
	
	static
	{
		try 
		{
			_interfacesDriver = new SyInterfacesDriver();
			_interfacesDriver.open();
		}
		catch(Exception ex)
		{
			if ( System.getProperty("os.name").startsWith("Windows"))
				_logger.error("Problems reading '%SYBASE%\\ini\\sql.ini' file.");
			else
				_logger.error("Problems reading '$SYBASE/interfaces' file.");
			_logger.error("SyInterfacesDriver error: "+ex.getMessage());
		}

		if (_interfacesDriver != null)
		{
			_logger.info("Using '"+_interfacesDriver.getBundle()+"' file for ASE server name lookup.");
		}
	}
	
	
	
//	public Connection newConnection(Properties inProps, boolean logErrors)
//	throws ManageException
////	throws NotConnectedException
//	{
//		Connection conn = null;
//		try
//		{
//			String jdbcDriver = "com.sybase.jdbc3.jdbc.SybDriver";
//			String jdbcUrl    = "jdbc:sybase:Tds:HOSTNAME:HOSTPORT";
//			if (_props != null)
//			{
//				jdbcDriver = _props.getProperty("jdbcDriver", jdbcDriver);
//				jdbcUrl    = _props.getProperty("jdbcUrl",    jdbcUrl);
//			}
//
//			Properties props;
//			if (inProps == null)
//				props = new Properties();
//			else
//				props = new Properties(inProps);
//			if (props.getProperty("user")     == null) props.put("user",     _username);
//			if (props.getProperty("password") == null) props.put("password", _password);
//			props.put("APPLICATIONNAME", Version.getAppName());
////			props.put("CHARSET", "iso_1");
//			//props.put("HOSTNAME", "");
//
//			String testUser = props.getProperty("user");
//			if ( testUser == null || (testUser != null && testUser.equals("")) )
//			{
//				String msg = "No user has been specified when connecting to server '"+_servername+"'. Please add the property '"+_propPrefix+".username=XXX' to the configuration file.";
//				_logger.warn(msg);
//				throw new ManageException(msg);
//			}
//			
//			Class.forName(jdbcDriver).newInstance();
//
//			if (_extensiveLogging)
//				_logger.info("Open a new connection to a Sybase server named '"+_servername+"' at host '"+_hostname+"' and port '"+_hostport+"'.");
//
//			jdbcUrl = jdbcUrl.replaceAll("HOSTNAME", _hostname);
//			jdbcUrl = jdbcUrl.replaceAll("HOSTPORT", Integer.toString(_hostport));
//
//			_logger.debug("Try open a connection to server='"+_servername+"'("+_hostname+":"+_hostport+"), username='"+_username+"', password='"+_password+"', jdbcDriver='"+jdbcDriver+"', jdbcUrl='"+jdbcUrl+"'.");
//			if (_loginTimeout > 0)
//			{
//				DriverManager.setLoginTimeout(_loginTimeout);
//			}
//			conn = DriverManager.getConnection(jdbcUrl, props);
//
//			// get various server info, this is overidden in 
//			// any subclasses that wants to keep special things about there server type.
//			setConnection(conn);
//
//			// Write info about what JDBC driver we connects via.
//			if ( ! _jdbcDriverInfoHasBeenWritten )
//			{
//				_jdbcDriverInfoHasBeenWritten = true;
//				try 
//				{
//					if (_logger.isDebugEnabled()) 
//					{
//						_logger.debug("The following drivers have been loaded:");
//						Enumeration drvEnum = DriverManager.getDrivers();
//						while( drvEnum.hasMoreElements() )
//						{
//							_logger.debug("    " + drvEnum.nextElement().toString());
//						}
//					}
//
//					DatabaseMetaData dbmd = conn.getMetaData();
//					if (dbmd != null)
//					{
//						_logger.info("JDBC driver version: " + dbmd.getDriverVersion());
//					}
//				} 
//				catch (SQLException ignore) {}
//			}
//			
//			return conn;
//		}
//		catch (SQLException oe)
//		{
//			// SQL State: JZ00M
//			// Login timed out. 
//			// Check that your database server is running on the host and port 
//			// number you specified. Also check the database server for other 
//			// conditions (such as a full tempdb) that might be causing it to hang
//
//			SQLException e = oe;
//			StringBuffer sb = new StringBuffer();
//			while (e != null)
//			{
//				sb.append(" Caught: ");
//				sb.append( e.getMessage() );
//				e = e.getNextException();
//			}
//			String msg = "Problems when connecting to a Server '"+_servername+"'."+sb.toString();
//			if ( logErrors)
//				_logger.error(msg);
//			
//			// Print some extra info if it was a LOGIN FAILED
//			if (oe.getSQLState().equals("JZ00L"))
//			{
//				_logger.info("The login was tried with user '"+_username+"', password '"+_password+"' to the server '"+_servername+"'.");
//			}
//			throw new NotConnectedException(_servername, getManagedType(), msg, oe);
//		}
//		catch (ClassNotFoundException e)
//		{
//			String msg = "Problems when connecting to Server '"+_servername+"'. Caught: "+e;
//			if ( logErrors)
//				_logger.error(msg);
//			throw new ManageException(msg, e);
//			//throw new NotConnectedException(_servername, getManagedType(), "Problems when connecting to a Server '"+_servername+"'.", e);
//		}
//		catch (InstantiationException e)
//		{
//			String msg = "Problems when connecting to Server '"+_servername+"'. Caught: "+e;
//			if ( logErrors)
//				_logger.error(msg);
//			throw new ManageException(msg, e);
//			//throw new NotConnectedException(_servername, getManagedType(), "Problems when connecting to a Server '"+_servername+"'.", e);
//		}
//		catch (IllegalAccessException e)
//		{
//			String msg = "Problems when connecting to Server '"+_servername+"'. Caught: "+e;
//			if ( logErrors)
//				_logger.error(msg);
//			throw new ManageException(msg, e);
//			//throw new NotConnectedException(_servername, getManagedType(), "Problems when connecting to a Server '"+_servername+"'.", e);
//		}
//	}

}
