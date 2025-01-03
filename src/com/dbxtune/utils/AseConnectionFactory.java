/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
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

/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.dbxtune.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.invoke.MethodHandles;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.AppDir;
import com.dbxtune.Version;
import com.dbxtune.gui.ConnectionProgressCallback;
import com.dbxtune.gui.ConnectionProgressDialog;
import com.dbxtune.sql.conn.ConnectionProp;
import com.dbxtune.ssh.SshTunnelInfo;
import com.sybase.util.ds.interfaces.Service;
import com.sybase.util.ds.interfaces.SyInterfacesDriver;
import com.sybase.util.ds.interfaces.SyInterfacesEntry;

public class AseConnectionFactory
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public static final String PROPKEY_LOGINTIMEOUT = "AseConnectionFactory.loginTimeout";
	public static final int    DEFAULT_LOGINTIMEOUT = 10;
	
	private static String              _driver              = "com.sybase.jdbc42.jdbc.SybDriver";
	private static String              _urlTemplate         = "jdbc:sybase:Tds:HOST:PORT";
	private static String              _urlTemplateHostPort = "HOST:PORT";
//	private static String              _host                = "";
//	private static int                 _port                = -1;
	private static Map<String, List<String>> _hostPortMap   = null;
	private static String              _server              = "";
	private static String              _username            = "";
	private static String              _password            = "";
	private static String              _appname             = "";
	private static String              _appVersion          = "";
	private static String              _hostname            = "";
	private static Properties          _props               = new Properties();
	
	/** Some application names might want to have some specific properties added, if not already in the URL */
	private static HashMap<String, Properties> _defaultAppNameProps = new HashMap<String, Properties>();

	private static SyInterfacesDriver _interfacesDriver = null;

	/** on first connect attempt write version information about the jdbcDriver, this will be set to true after first connect attempt */
	private static boolean    _jdbcDriverInfoHasBeenWritten = false;

	/**
	 * STATIC INITIALIZATION
	 */
	static
	{
		String jdbcDriver      = "com.sybase.jdbc42.jdbc.SybDriver";
		String jdbcUrlTemplate = "jdbc:sybase:Tds:HOST:PORT";

		// for jTDS, the below can be used. See http://jtds.sourceforge.net/
//		String jdbcDriver      = "net.sourceforge.jtds.jdbc.Driver";  // http://jtds.sourceforge.net/faq.html
//		String jdbcUrlTemplate = "jdbc:jtds:sybase://HOST:PORT";
		// jdbc:jtds:<server_type>://<server>[:<port>][/<database>][;<property>=<value>[;...]]
		// <server_type> is one of either 'sqlserver' or 'sybase' (their meaning is quite obvious)
		// <port> is the port the database server is listening to (default is 1433 for SQL Server and 7100 for Sybase) and 
		// <database> is the database name -- JDBC term: catalog -- (if not specified, the user's default database is used). 
		// The set of properties supported by jTDS is:

		Configuration conf = Configuration.getCombinedConfiguration();
		if (conf != null)
		{
			_driver      = conf.getProperty("jdbcDriver");
			_urlTemplate = conf.getProperty("jdbcUrlTemplate");
		}

		// Try the SYSTEM
		if (_driver      == null) _driver      = System.getProperty("jdbcDriver");
		if (_urlTemplate == null) _urlTemplate = System.getProperty("jdbcUrlTemplate");

		// Fall back on static values
		if (_driver      == null) _driver      = jdbcDriver;
		if (_urlTemplate == null) _urlTemplate = jdbcUrlTemplate;

		_logger.info("Using JDBC Driver '"+_driver+"'. This can be changed using property 'jdbcDriver=driver'. In the config file or system properties");
		_logger.info("Using URL Template '"+_urlTemplate+"'. This can be changed using property 'jdbcUrlTemplate=template'. In the config file or system properties");
		
		// Get SYBASE ENV and check if the interfaces file exist
//		String envSybase = System.getProperty("SYBASE");
		String envSybase = System.getenv("SYBASE");
		String interfacesFile = null;
		boolean interfacesFileExist = false;
		if (envSybase != null)
		{
			envSybase = envSybase.trim();
			// Strip off any trailing '/' or '\' of the SYBASE environment string
			if (envSybase.endsWith("/") || envSybase.endsWith("\\"))
				envSybase = envSybase.substring(0, envSybase.length()-1);
			
			// add the interfaces file
			if ( PlatformUtils.getCurrentPlattform() == PlatformUtils.Platform_WIN )
				interfacesFile = envSybase + "\\ini\\sql.ini";
			else
				interfacesFile = envSybase + "/interfaces";

			// Check if the interfaces file exists.
			File ifile = new File(interfacesFile);
			interfacesFileExist = ifile.exists();
			if ( ! interfacesFileExist )
				_logger.info("The SYBASE environment variable was found, but the interfaces file '"+interfacesFile+"' didn't exists.");
		}

		// Create a local/dummy interfaces if the SYBASE can't be found.
		if ( ! interfacesFileExist )
		{
			interfacesFile = createPrivateInterfacesFile(null);
		}

		// Try to open the interfaces file.
		try 
		{
			if (interfacesFile != null)
			{
				System.setProperty("interfaces.file", interfacesFile);
				_interfacesDriver = new SyInterfacesDriver(interfacesFile);
				_interfacesDriver.open(interfacesFile);
			}
			else
			{
				_interfacesDriver = new SyInterfacesDriver();
				_interfacesDriver.open();
			}
		}
		catch(Exception ex)
		{
			_logger.warn("Problems reading SYBASE Name/Directory Service file '"+interfacesFile+"'.");
			_logger.warn("SyInterfacesDriver Problem: "+ex);

			// Problems open the interfaces file
			// FALLBACK to create/use the private interfaces file.
			String privateInterfacesFile = getPrivateInterfacesFile(true);
			try
			{
				_logger.info("Trying to open the local "+Version.getAppName()+" Name/Directory Service file '"+privateInterfacesFile+"'.");
				createPrivateInterfacesFile(privateInterfacesFile);

				System.setProperty("interfaces.file", privateInterfacesFile);
				_interfacesDriver = new SyInterfacesDriver(privateInterfacesFile);
				_interfacesDriver.open(privateInterfacesFile);
			}
			catch(Exception ex2)
			{
				_logger.warn("Even Problems reading LOCAL "+Version.getAppName()+" Name/Directory Service file '"+privateInterfacesFile+"'.");
				_logger.warn("LOCAL FILE SyInterfacesDriver Problem: "+ex2);
			}
		}

		if (_interfacesDriver == null)
		{
			_logger.warn("SYBASE or Local "+Version.getAppName()+" Name/Directory Service could NOT be initialized, creating an EMPTY place holder.");

			// Set a NON initialized SyDriver just to avoid NullPointerExceptions.
			_interfacesDriver = new SyInterfacesDriver();
		}
		else
		{
			_logger.info("Using '"+_interfacesDriver.getBundle()+"' file for ASE server name lookup.");
		}
	}

	/** get name of a local/private interfaces file */
	public static String getPrivateInterfacesFile(boolean setSybaseHome)
	{
		String file = null;
		String tmpSybaseEnvLocation = (AppDir.getAppStoreDir() != null) ? AppDir.getAppStoreDir() : "";

		if ( PlatformUtils.getCurrentPlattform() == PlatformUtils.Platform_WIN )
			file = tmpSybaseEnvLocation + "\\sql.ini";
		else
			file = tmpSybaseEnvLocation + "/interfaces";

		if (setSybaseHome)
		{
    		// Note: this might get printed several times
    		if (System.getenv("SYBASE") == null)
    		{
    			_logger.info("SYBASE environment variable was not set, setting System Property 'sybase.home' to '"+tmpSybaseEnvLocation+"'.");
    			System.setProperty("sybase.home", tmpSybaseEnvLocation);
    		}
		}

		return file;
	}

	/** check/create the local/private interfaces file */
	public static String createPrivateInterfacesFile(String file)
	{
		// set a local filename if one wasn't passed
		if (file == null)
		{
			file = getPrivateInterfacesFile(true);
			_logger.info("I will try to use the interfaces file '"+file+"'.");
		}
		
		// Check if the interfaces file exists.
		File ifile = new File(file);
		boolean fileExists = ifile.exists();
		if ( fileExists )
		{
			_logger.info("The interfaces file '"+file+"' already exists, lets try to use it.");
			return file;
		}


		_logger.info("Creating a dummy interfaces file named '"+file+"'.");
		try
		{
			BufferedWriter out = new BufferedWriter(new FileWriter(file));
			if ( PlatformUtils.getCurrentPlattform() == PlatformUtils.Platform_WIN )
			{
				String nl = System.getProperty("line.separator");
				out.write(";; ----------------------------------------------- " + nl);
				out.write(";; Server - DUMMY_ASE " + nl);
				out.write(";; This entry was added by '"+Version.getAppName()+"' " + nl);
				out.write(";; ----------------------------------------------- " + nl);
				out.write("[DUMMY_ASE]" + nl);
				out.write("query=TCP, localhost, 5000" + nl);
				out.write(nl);
			}
			else
			{
				String nl = System.getProperty("line.separator");
				out.write("# ----------------------------------------------- " + nl);
				out.write("# Server - DUMMY_ASE " + nl);
				out.write("# This entry was added by '"+Version.getAppName()+"' " + nl);
				out.write("# ----------------------------------------------- " + nl);
				out.write("DUMMY_ASE" + nl);
				out.write("\tquery tcp ether localhost 5000" + nl);
				out.write(nl);
			}
			out.close();
		}
		catch (IOException e)
		{
			_logger.error("Problems when creating the interfaces file named '"+file+"', continuing anyway. Caught: "+e);
			file = null;
		}
		return file;
	}
	


	/**
	 * Reset the static fields of the AseConnectionFactory
	 */
	public static void reset()
	{
//		_host        = "";
//		_port        = -1;
		_hostPortMap = null;
		_server      = "";
		_username    = "";
		_password    = "";
		_appname     = "";
		_appVersion  = "";
		_hostname    = "";
		_props       = new Properties();
		_defaultAppNameProps = new HashMap<String, Properties>();
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
	public static void setPropertiesForAppname(String appname, String propname, String propValue)
	{
		Properties props = _defaultAppNameProps.get(appname);
		if (props == null)
			props = new Properties();
		props.put(propname, propValue);
		_defaultAppNameProps.put(appname, props);
	}

	public static void setProperties(Map<String,String> map)
	{
		_props = new Properties();
		if (map != null)
			_props.putAll(map);
	}
	public static void setProperties(Properties props)
	{
		if (props == null)
			props = new Properties();
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
	public static void setUrlTemplate(String urlTemplate)
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
//	public static void setHost(String host)
//	{
//		_host = host;
//
//		String server = getServerName(_host, _port); 
//		_server = (server == null ? "" : server); 
//	}
//	public static void setPort(int port)
//	{
//		_port = port;
//
//		String server = getServerName(_host, _port); 
//		_server = (server == null ? "" : server); 
//	}

	public static void setServer(String servername)
	{
		_server = servername;
		
		// If servername is in interfaces file, set the Hostport...
		String hostPort = getIHostPortStr(servername);
		if (hostPort != null && ! hostPort.trim().equals(""))
			setHostPort(hostPort);
	}
	public static void setAppName(String appname)
	{
		_appname = appname;
	}
	public static void setAppVersion(String appVersion)
	{
		_appVersion = appVersion;
	}
	public static void setHostName(String hostname)
	{
		_hostname = hostname;
	}
	
	public static Properties          getPropertiesForAppname(String appname) { return _defaultAppNameProps.get(appname); }
	public static Properties          getProperties()              { return _props; }
	public static Object              getProperty(String propname) { return _props.getProperty(propname); }
	public static String              getDriver()                  { return _driver; }
	public static String              getUrlTemplate()             { return _urlTemplate; }
	public static String              getUrlTemplateBase()         { return _urlTemplate.replace(_urlTemplateHostPort, ""); }
	public static String              getUrlTemplateHostPort()     { return _urlTemplateHostPort; }
	public static String              getUrl()                     { return _urlTemplate; }
	public static String              getUser()                    { return _username; }
	public static String              getPassword()                { return _password; }
//	public static String              getHost()                    { return _host; }
//	public static int                 getPort()                    { return _port; }
	public static Map<String, List<String>> getHostPortMap()       { return _hostPortMap; }
	public static String              getServer()                  { return _server; }
	public static String              getAppName()                 { return _appname; }
	public static String              getAppVersion()              { return _appVersion; }
	public static String              getHostName()                { return _hostname; }


	//---------------------------------------
	// HOST methods
	//---------------------------------------
	/** If we have more than one host,port in the connection string, get them all as a Array */
	public static String[] getHostArr() { return StringUtil.commaStrToArray(getHosts()); }

	/** If we have more than one host,port in the connection string, get them all as a comma separated String */
	public static String getHosts() { return getHosts(","); }

	/** If we have more than one host,port in the connection string, get them all as a comma separated String */
	public static String getHosts(String sepStr)
	{
		if (_hostPortMap == null || (_hostPortMap != null && _hostPortMap.isEmpty()) )
			return null;

		return StringUtil.toCommaStrMultiMapKey(_hostPortMap, sepStr);
	}

	/** If we have more than one host,port in the connection string, get the FIRST hostname */
	public static String getFirstHost() 
	{ 
		String[] sa = StringUtil.commaStrToArray(getHosts());
		if (sa.length == 0)
			return null;
		return sa[0]; 
	}

	
	//---------------------------------------
	// PORT methods
	//---------------------------------------
	/** If we have more than one host,port in the connection string, get them all as a Array */
	public static String[] getPortArr() { return StringUtil.commaStrToArray(getPorts()); }

	/** If we have more than one host,port in the connection string, get them all as a comma separated String */
	public static String getPorts() { return getPorts(","); }

	/** If we have more than one host,port in the connection string, get them all as a comma separated String */
	public static String getPorts(String sepStr)
	{
		if (_hostPortMap == null || (_hostPortMap != null && _hostPortMap.isEmpty()) )
			return null;

		return StringUtil.toCommaStrMultiMapVal(_hostPortMap, sepStr);
	}

	/** If we have more than one host,port in the connection string, get the FIRST port */
	public static int getFirstPort() 
	{ 
		String[] sa = StringUtil.commaStrToArray(getPorts());
		if (sa.length == 0)
			return -1;
		try 
		{ 
			return Integer.parseInt(sa[0]); 
		}
		catch (NumberFormatException e) 
		{
			_logger.debug("getFirstPort Caught: "+e);
			return -1; 
		}
	}

	//---------------------------------------
	// HOST/PORT methods
	//---------------------------------------
	/** 
	 * Set hostPortMap based on the Map which has key=value (which are host=port)
	 * @param hostPortMap {host1=port1[,host2=port2[,hostN=portN]] 
	 */
	public static void setHostPort(Map<String, List<String>> hostPortMap)
	{
		_hostPortMap = hostPortMap;
		_server = getIServerName(_hostPortMap);
	}

	/**
	 * Set hostPortMap based on the input parameters below
	 * <p>
	 * If the comma separated list of host and ports doesn't match, the missing ones will be padded with "" (empty strings)
	 * 
	 * @param hosts a comma separated list of hosts that we can use to connect to "host1[,host2[,hostN]]"
	 * @param ports a comma separated list of ports that we can use to connect to "port1[,port2[,portN]]"
	 */
	public static void setHostPort(String hosts, String ports)
	{
		String hostPortStr = toHostPortStr(hosts, ports);
		setHostPort( hostPortStr );
	}

	/** 
	 * Set host and port into the hostPortMap based on the jConnect format of passing host:port
	 * @param hostPortStr host1:port1[,host2:port2[,hostN:portN]] 
	 */
	public static void setHostPort(String hostPortStr)
	{
		Map<String,List<String>> hostPortMap = StringUtil.parseCommaStrToMultiMap(hostPortStr, ":", ",");
		setHostPort( hostPortMap );
	}

	/** 
	 * Add host and port into the current hostPortMap based on the jConnect format of passing host:port
	 * @param hostPortStr host1:port1[,host2:port2[,hostN:portN]] 
	 */
	public static void addHostPort(String hostPortStr)
	{
		Map<String, List<String>> addHostPortMap = StringUtil.parseCommaStrToMultiMap(hostPortStr, ":", ",");
		if (_hostPortMap == null)
			_hostPortMap = new LinkedHashMap<String, List<String>>();

		// Add it to current MAP
		_hostPortMap.putAll(addHostPortMap);
	}
	
	/**
	 * Get host and port in the jConnect URL format
	 * @return a String that contains host1:port1[,host2:port2[,hostN:portN]]
	 */
	public static String getHostPortStr()
	{
		if (_hostPortMap == null || (_hostPortMap != null && _hostPortMap.isEmpty()) )
			return null;

		return StringUtil.toCommaStrMultiMap(_hostPortMap, ":", ",");
	}


	//---------------------------------------
	// Interfaces file methods
	//---------------------------------------
	/** get sql.ini or interfaces file name from Sybase Interfaces Driver */
	public static String getIFileName()
	{
		return _interfacesDriver.getBundle();
	}

	/** set sql.ini or interfaces file name for Sybase Interfaces Driver */
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


	/** 
	 * get a list of comma separated hosts that will be tried when connecting to server
	 * <p> The infromation is fetched from the Sybase Interfaces Driver
	 * */
	public static String getIHosts(String server) { return getIHosts(server, ", "); }

	/** 
	 * get a list of <code>sepStr</code> separated hosts that will be tried when connecting to server
	 * <p> The infromation is fetched from the Sybase Interfaces Driver
	 */
	@SuppressWarnings({ "unused", "unchecked" })
	public static String getIHosts(String server, String sepStr)
	{
		SyInterfacesEntry ie = _interfacesDriver.getEntry(server);

		if (ie == null)
			return null;

		String hosts = "";

		// LOOP query/master rows in current SERVERNAME entry
		List<Service> queryRows = ie.getServices();
		for (Iterator<Service> it = queryRows.iterator(); it.hasNext(); )
		{
			// se Service Entry
			Service se = it.next();

			if ( Service.QUERY.equalsIgnoreCase(se.getType()) )
			{
				String seHost = se.getHost();
				String sePort = se.getPort();

				hosts += seHost + sepStr;
			}
		} // end: LOOP queryRows
		if (hosts.endsWith(sepStr))
			hosts = hosts.substring(0, hosts.length() - sepStr.length());

		return hosts;
	}

	/** 
	 * get first host that will be tried when connecting to server
	 * <p> The infromation is fetched from the Sybase Interfaces Driver
	 */
	public static String getIFirstHost(String server)
	{
		SyInterfacesEntry interfaceEntry = _interfacesDriver.getEntry(server);
		
		return (interfaceEntry == null) ? null : interfaceEntry.getHost();
	}


	
	/** 
	 * get a list of comma separated ports that will be tried when connecting to server
	 * <p> The infromation is fetched from the Sybase Interfaces Driver
	 */
	public static String getIPorts(String server) { return getIPorts(server, ", "); }

	/** 
	 * get a list of <code>sepStr</code> separated ports that will be tried when connecting to server 
	 * <p> The infromation is fetched from the Sybase Interfaces Driver
	 */
	@SuppressWarnings({ "unused", "unchecked" })
	public static String getIPorts(String server, String sepStr)
	{
		SyInterfacesEntry ie = _interfacesDriver.getEntry(server);

		if (ie == null)
			return null;

		String ports = "";

		// LOOP query/master rows in current SERVERNAME entry
		List<Service> queryRows = ie.getServices();
		for (Iterator<Service> it = queryRows.iterator(); it.hasNext(); )
		{
			// se Service Entry
			Service se = it.next();

			if ( Service.QUERY.equalsIgnoreCase(se.getType()) )
			{
				String seHost = se.getHost();
				String sePort = se.getPort();

				ports += sePort + sepStr;
			}
		} // end: LOOP queryRows
		if (ports.endsWith(sepStr))
			ports = ports.substring(0, ports.length() - sepStr.length());

		return ports;
	}

	/** 
	 * get first port that will be tried when connecting to server 
	 * <p> The infromation is fetched from the Sybase Interfaces Driver
	 */
	public static int getIFirstPort(String server)
	{
		SyInterfacesEntry interfaceEntry = _interfacesDriver.getEntry(server);
		
		return (interfaceEntry == null) ? -1 : Integer.parseInt( interfaceEntry.getPort() );
	}


	/**
	 * Get host and port in the jConnect URL format
	 * @return a String that contains host1:port1[,host2:port2[,hostN:portN]]
	 */
	public static String getIHostPortStr(String server)
	{
		SyInterfacesEntry ie = _interfacesDriver.getEntry(server);

		if (ie == null)
		{
			_logger.warn("getIHostPortStr(): Can not find an entry for server '"+server+"', from the interfaces driver '"+_interfacesDriver.getBundle()+"'. null will be returned.");
			return null;
		}

		String hostPortStr = "";

		// LOOP query/master rows in current SERVERNAME entry
		List<Service> queryRows = ie.getServices();
		for (Iterator<Service> it = queryRows.iterator(); it.hasNext(); )
		{
			// se Service Entry
			Service se = it.next();

			if ( Service.QUERY.equalsIgnoreCase(se.getType()) )
			{
				String seHost = se.getHost();
				String sePort = se.getPort();

				hostPortStr += seHost + ":" + sePort;
				
				if (it.hasNext())
					hostPortStr += ",";
			}
		} // end: LOOP queryRows

		return hostPortStr;
	}

	/**
	 * FIXME: describe this one
	 * @param serverName
	 * @return
	 */
	public static String resolvInterfaceEntry(String serverName)
	{
		return resolvInterfaceEntry(serverName, null);
	}

	/**
	 * FIXME: describe this one
	 * @param serverName
	 * @param iniFile
	 * @return
	 */
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
//			if ( System.getProperty("os.name").startsWith("Windows"))
			if ( PlatformUtils.getCurrentPlattform() == PlatformUtils.Platform_WIN )
				_logger.error("Problems reading '%SYBASE%\\ini\\sql.ini' file.");
			else
				_logger.error("Problems reading '$SYBASE/interfaces' file.");
			_logger.error("SyInterfacesDriver error: "+ex.getMessage());
		}
		
		return ret;
	}
	
	/**
	 * Get server names from the Sybase interfaces driver.
	 * @return String array of server names
	 */
	public static String[] getIServerNames()
	{
		String[] servers = _interfacesDriver.getServers();
		if (servers != null)
			Arrays.sort(servers);
		return servers;
	}

	/**
	 * Get server name from the Sybase Interfaces Driver (sql.ini / interfaces)
	 * <p>
	 * This loops the interfaces driver from start to end.<br>
	 * The first server with the "query" row that contained host,port is returned
	 * <p>
	 * This method do NOT handle multiple multiple "query" rows matching
	 * 
	 * @param host hostname to the server
	 * @param port port number to the server
	 * @return First matching server name that had a "query" row with: host, port 
	 */
	@SuppressWarnings("unchecked")
	public static String getIServerName(String host, int port)
	{
		if ( host == null || (host != null && host.trim().equals("")) )
			throw new IllegalArgumentException("Host can't be null or empty.");
		if (port <= 0)
			throw new IllegalArgumentException("Port number must be larger than zero, port is now "+port);

		if (_interfacesDriver == null)
			return null;

		// LOOP SERVERNAME entries
		Enumeration<SyInterfacesEntry> en = _interfacesDriver.entries();
		while (en.hasMoreElements())
		{
			SyInterfacesEntry ie = en.nextElement();
			String serverName = ie.getName();

			// LOOP query/master rows in current SERVERNAME entry
			List<Service> queryRows = ie.getServices();
			for (Iterator<Service> it = queryRows.iterator(); it.hasNext(); )
			{
				Service service = it.next();

				_logger.debug(serverName + " - Service: "+service.toString());
				if ( Service.QUERY.equalsIgnoreCase(service.getType()) )
				{
					String eHost = service.getHost();
					String ePort = service.getPort();

					if (eHost != null && ePort != null)
					{
						if (eHost.equalsIgnoreCase(host) && ePort.equals(Integer.toString(port)))
						{
							_logger.debug("Found Sybase Server Interface Entry '"+ie.getName()+"' for host '"+eHost+"', port '"+ePort+"'.");
							return ie.getName();
						}
					}
				}
			}
		}
		return null;
	}
	
	/**
	 * Get server name from the Sybase Interfaces Driver (sql.ini / interfaces)
	 * <p>
	 * This loops the interfaces driver from start to end.<br>
	 * The first server with the "query" row that contained ALL host=port in the hostPortMap is returned
	 * <p>
	 * This method DOES handle multiple multiple "query" rows matching
	 * 
	 * @param hostPortMap a Map<String, String> that contains key<String>(hostname) = value<String>(portnum)
	 * @return First matching server name that had ALL "query" row with: host, port 
	 */
//	public static String getIServerName(Map<String,List<String>> hostPortMap)
	public static String getIServerName(Map hostPortMap)
	{
		if (hostPortMap == null)
			throw new IllegalArgumentException("hostPortMap can't be null.");
		if (hostPortMap.size() == 0)
			throw new IllegalArgumentException("hostPortMap is empty, it needs at least 1 entry.");

		if (_interfacesDriver == null)
			return null;

		int expectedMatch = hostPortMap.size();

		// LOOP SERVERNAME entries
		Enumeration en = _interfacesDriver.entries();
		while (en.hasMoreElements())
		{
			SyInterfacesEntry ie = (SyInterfacesEntry) en.nextElement();
			String serverName = ie.getName();

			_logger.trace("Searching in server '"+serverName+"'.");
			int matchCounter = 0;

			// LOOP hostPortMap
			for (Iterator hpIt = hostPortMap.keySet().iterator(); hpIt.hasNext();)
			{
				// hpe Host Port Entry
				String hpeHost = (String) hpIt.next();
				Object hpePort = hostPortMap.get(hpeHost);

				if (hpePort instanceof List)
				{
					List list = (List) hpePort;
					expectedMatch += list.size() - 1;
					for (Iterator listIt = list.iterator(); listIt.hasNext();)
					{
						hpePort = listIt.next();
						matchCounter += isHostPortInSyIntEntry(ie, hpeHost, (String)hpePort);
					}
				}
				else
					matchCounter += isHostPortInSyIntEntry(ie, hpeHost, (String)hpePort);

				// LOOP query/master rows in current SERVERNAME entry
				
				if (matchCounter == expectedMatch)
				{
					_logger.debug("Found the server entry '"+serverName+"' that matched all entries in the hostPortMap '"+hostPortMap+"'.");
					return serverName;
				}

			} // end LOOP hostPortMap

		} // end: LOOP interfaces
		return null;
	}
	@SuppressWarnings("unchecked")
	private static int isHostPortInSyIntEntry(SyInterfacesEntry ie, String hpeHost, String hpePort)
	{
		String serverName = ie.getName();
		List<Service> queryRows = ie.getServices();
		for (Iterator<Service> it = queryRows.iterator(); it.hasNext(); )
		{
			// se Service Entry
			Service se = it.next();

			_logger.trace("      -> '"+se+"'.");
			if ( Service.QUERY.equalsIgnoreCase(se.getType()) )
			{
				String seHost = se.getHost();
				String sePort = se.getPort();

				if (seHost != null && sePort != null)
				{
					if (hpeHost.equalsIgnoreCase(seHost) && hpePort.equals(sePort))
					{
						_logger.debug("Found Sybase Server Interface Entry '"+serverName+"' for host '"+seHost+"', port '"+sePort+"'.");
						return 1;
//						matchCounter++;
//						_logger.trace("      =========> FOUND ENTRY: matchCount="+matchCounter+", '"+se+"'.");
//						_logger.debug("Found Sybase Server Interface Entry '"+serverName+"' for host '"+seHost+"', port '"+sePort+"'. matchCount="+matchCounter+", hostPortMap.size()="+hostPortMap.size());
					}
				}
			}
		} // end: LOOP queryRows
		return 0;
	}
	
	/**
	 * Get server name from the Sybase Interfaces Driver (sql.ini / interfaces)
	 * <p>
	 * This loops the interfaces driver from start to end.<br>
	 * The first server with the "query" row that contained ALL host=port in the hostPortStr is returned
	 * <p>
	 * This method DOES handle multiple multiple "query" rows matching
	 * 
	 * @param hostPortStr a String that contains host1:port1[,host2:port2[,hostN:portN]]
	 * @return First matching server name that had ALL "query" row with matching host:port 
	 */
	public static String getIServerName(String hostPortStr)
	{
		Map<String,List<String>> hostPortMap = StringUtil.parseCommaStrToMultiMap(hostPortStr, ":", ",");
		if (hostPortMap.size() == 0)
			return null;
		return getIServerName(hostPortMap);
	}

	/**
	 * Get server name from the Sybase Interfaces Driver (sql.ini / interfaces)
	 * <p>
	 * This loops the interfaces driver from start to end.<br>
	 * The first server with the "query" row that contained ALL host=port in the hostPortStr is returned
	 * <p>
	 * This method DOES handle multiple multiple "query" rows matching
	 * 
	 * @param hosts a String that contains host1[,host2[,hostN]]
	 * @param ports a String that contains port1[,port2[,portN]]
	 * @return First matching server name that had ALL "query" row with matching host:port 
	 */
	public static String getIServerName(String hosts, String ports) 
	{
		String hostPortStr = toHostPortStr(hosts, ports);
		return getIServerName(hostPortStr);
	}

	/**
	 * Get server name from the Sybase Interfaces Driver (sql.ini / interfaces)
	 * <p>
	 * This loops the interfaces driver from start to end.<br>
	 * The first server with the "query" row that contained ALL host=port in the hostPortStr is returned
	 * <p>
	 * This method DOES handle multiple multiple "query" rows matching
	 * 
	 * @param hostsArr a String[] that contains {"host1"[, "host2"[, "hostN"]]}
	 * @param portsArr a String[] that contains {"port1"[, "port2"[, "portN"]]}
	 * @return First matching server name that had ALL "query" row with matching host:port 
	 */
	public static String getIServerName(String[] hostsArr, String[] portsArr)
	{
		String hostPortStr = toHostPortStr(hostsArr, portsArr);
		return getIServerName(hostPortStr);
	}


	


	/**
	 * Write a entry to the sql.ini or interfaces file
	 * @param filename    name of the sql.ini or interfaces file entry
	 * @param servername  name of the server to add
	 * @param hostPortStr host1:port, host2:port, host3:port
	 */
	public static void addIFileEntry(String filename, String servername, String hostPortStr)
	{
		PrintWriter out = null;
		try 
		{
			Map<String, String> hostPortMap = StringUtil.parseCommaStrToMap(hostPortStr, ":", ",");

		    out = new PrintWriter(new BufferedWriter(new FileWriter(filename, true)));
			if ( PlatformUtils.getCurrentPlattform() == PlatformUtils.Platform_WIN )
			{
				out.println();
				out.println("[" + servername + "]");
				for (String key : hostPortMap.keySet())
				{
					String val = hostPortMap.get(key);
					out.println("query=TCP, " + key + ", " + val);
				}
			}
			else
			{
				out.println();
				out.println(servername);
				for (String key : hostPortMap.keySet())
				{
					String val = hostPortMap.get(key);
					out.println("\tquery tcp ether " + key + " " + val);
				}
			}
		}
		catch (IOException e) 
		{
			SwingUtils.showErrorMessage(null, "Problems writing to sql.ini or interfaces", 
					"<html>Problems writing server entry to file '"+filename+"'.<br><br><b>"+e+"</b><html>", e);
		}
		finally
		{
			if(out != null)
				out.close();
		} 		
	}

	//---------------------------------------
	// some generic methods
	//---------------------------------------
	/**
	 * convert input parameters into a HostPort String (see below)
	 * 
	 * @param hosts a String that contains host1[,host2[,hostN]]
	 * @param ports a String that contains port1[,port2[,portN]]
	 * 
	 * @return a String that looks like "host1:port1[,host2:port2[,hostN:portN]]"
	 */
	public static String toHostPortStr(String hosts, String ports)
	{
		String[] hostsArr = StringUtil.commaStrToArray(hosts);
		String[] portsArr = StringUtil.commaStrToArray(ports);

		return toHostPortStr(hostsArr, portsArr);
	}

	/**
	 * convert input parameters into a HostPort String (see below)
	 * 
	 * @param hostsArr a String[] that contains {"host1"[, "host2"[, "hostN"]]}
	 * @param portsArr a String[] that contains {"port1"[, "port2"[, "portN"]]}
	 * 
	 * @return a String that looks like "host1:port1[,host2:port2[,hostN:portN]]"
	 */
	public static String toHostPortStr(String[] hostsArr, String[] portsArr)
	{
		String hostPortStr = "";
		for (int i=0; i<hostsArr.length||i<portsArr.length; i++)
		{
			hostPortStr += 
				(( i < hostsArr.length) ? hostsArr[i] : "") +
				":" + 
				(( i < portsArr.length) ? portsArr[i] : "") +
				",";
		}
		if (hostPortStr.endsWith(","))  // remove last comma ','
			hostPortStr = hostPortStr.substring(0, hostPortStr.length()-1);

		return hostPortStr;
	}
	
	/**
	 * convert input parameters into a HostPort String (see below)
	 * 
	 * @param host The host name
	 * @param port The port number
	 * 
	 * @return a String that looks like "host1:port1"
	 */
	public static String toHostPortStr(String host, int port)
	{
		return host + ":" + port;
	}


	/**
	 * Check if the hostPort String is of a valid form.
	 * @param aseServerStr the string to check it should look like host1:port1[,host2:port2[,hostN:portN]]
	 * @return true if it's a valid name
	 */
	public static boolean isHostPortStrValid(String hostPortStr)
	{
		String errorStr = isHostPortStrValidReason(hostPortStr);
		return errorStr == null;
	}

	/**
	 * Check if the hostPort String is of a valid form.
	 * @param aseServerStr the string to check it should look like host1:port1[,host2:port2[,hostN:portN]]
	 * @return null if everything is OK. An error message if things was not correct.
	 */
	public static String isHostPortStrValidReason(String hostPortStr)
	{
		if (hostPortStr == null)
			return "Input parameter is null.";

		Map<String,List<String>> hostPortMap = StringUtil.parseCommaStrToMultiMap(hostPortStr, ":", ",");

		if (hostPortMap == null)
			return "HostPortMap is null. input string was '"+hostPortStr+"'.";

		if (hostPortMap.isEmpty())
			return "HostPortMap has zero entries. input string was '"+hostPortStr+"'.";

		// Loop the entries in the Map, which was parsed above
		for (Iterator it = hostPortMap.keySet().iterator(); it.hasNext();)
		{
			String host = (String) it.next();
			Object port = hostPortMap.get(host);

			if (host.trim().equals(""))
				return "Hostname can't be empty. (host='"+host+"', port='"+port+"').";

			if (port instanceof List)
			{
				List list = (List) port;
				for (Iterator listIt = list.iterator(); listIt.hasNext();)
				{
					port = listIt.next();

					try	{ Integer.parseInt(port+""); }
					catch (NumberFormatException ignore)
					{
						return "The port number '"+port+"' is not a number. (host='"+host+"', port='"+port+"'.)";
					}
				}
			}
			else
			{
				try	{ Integer.parseInt(port+""); }
				catch (NumberFormatException ignore)
				{
					return "The port number '"+port+"' is not a number. (host='"+host+"', port='"+port+"'.)";
				}
			}
		}
		return null;
	}
	
	
	

	public static String getLocalHostname()
	{
		String hostname = null;
		try 
		{
			InetAddress addr = InetAddress.getLocalHost();

			hostname = addr.getHostName();
			if (hostname != null)
			{
				// if not IP adress, stip off the prefix (save only hostname)
				if ( ! hostname.matches("^[0-9].*") )
				{
					int firstDot = hostname.indexOf(".");
					if (firstDot > 0)
						hostname = hostname.substring(0, firstDot);
				}
			}
		}
		catch (UnknownHostException e) {/*ignore*/}

		return hostname;
	}
	
	
	
	
	
	
	//---------------------------------------
	// CONNECTION methods
	//---------------------------------------

	/** get a connection using the static settings priviously made, but override the input parameters for this method */
	public static Connection getConnection(String dbname, String appname, String hostname) 
	throws ClassNotFoundException, SQLException
	{
		return getConnection(getHostPortStr(), dbname, _username, _password, appname, _appVersion, hostname, (Properties)null, (ConnectionProgressCallback)null);
	}

	/** get a connection using the static settings priviously made, but override the input parameters for this method */
	public static Connection getConnection(String dbname) 
	throws ClassNotFoundException, SQLException
	{
		return getConnection(getHostPortStr(), dbname, _username, _password, _appname, _appVersion, _hostname, (Properties)null, (ConnectionProgressCallback)null);
	}

	/** get a connection using the static settings previously made, but override the input parameters for this method 
	 * @param _connProp 
	 * @param connectionProgressDialog */
	public static Connection getConnection(ConnectionProgressDialog connectionProgressDialog, ConnectionProp connProp) 
	throws ClassNotFoundException, SQLException
	{
		String username   = _username;
		String password   = _password;
		String appname    = _appname;
		String appVersion = _appVersion;
		String hostname   = _hostname;
		String hostPortStr = getHostPortStr();

		if (connProp != null)
		{
			username   = connProp.getUsername();
			password   = connProp.getPassword();
			appname    = connProp.getAppName();
			appVersion = connProp.getAppVersion();
			hostname   = _hostname;
			
			// Another horrible workaround until we rewrite this thing from scratch.
			if (StringUtil.hasValue(connProp.getUrl()))
			{
				try
				{
					AseUrlHelper urlHelper = AseUrlHelper.parseUrl(connProp.getUrl());
					hostPortStr = urlHelper.getHostPortStr();

//System.out.println("AseConnectionFactory.getConnection(connectionProgressDialog, connProp): hostPortStr=|"+hostPortStr+"|, connProp="+connProp);
					if (connProp.getSshTunnelInfo() != null)
					{
						SshTunnelInfo ti = connProp.getSshTunnelInfo();
						hostPortStr = ti.getLocalHost() + ":" + ti.getLocalPort();

//System.out.println("NEW hostPortStr due to SshTunnelInfo. hostPortStr=|" + hostPortStr + "|.");
						
						// TODO: Check if we have got a Tunnel... (if not set one up)
//						SshTunnelManager tm = SshTunnelManager.getInstance();
//						if (tm != null)
//						{
//							tm.
//						}
					}
					if (StringUtil.isNullOrBlank(hostPortStr))
					{
						throw new SQLException("Can't get a proper 'host:port' String when parsing ASE Url '"+connProp.getUrl()+"'.");
					}
				}
				catch(ParseException ex)
				{
					throw new SQLException("Problems parsing ASE Url '"+connProp.getUrl()+"'.", ex);
				}
			}
		}
		return getConnection(hostPortStr, null, username, password, appname, appVersion, hostname, (Properties)null, (ConnectionProgressCallback)null);
	}
	public static Connection getConnection(ConnectionProgressCallback cpd) 
	throws ClassNotFoundException, SQLException
	{
		return getConnection(getHostPortStr(), null, _username, _password, _appname, _appVersion, _hostname, (Properties)null, cpd);
	}

	/** get a connection using the static settings priviously made, but override the input parameters for this method */
	public static Connection getConnection(String host, int port, String dbname, String username, String password, String appname, String appVersion, String hostname) 
	throws ClassNotFoundException, SQLException
	{
		return getConnection(host+":"+port, dbname, username, password, appname, appVersion, hostname, (Properties)null, (ConnectionProgressCallback)null);
	}

	/** get a connection using the static settings priviously made, but override the input parameters for this method */
	public static Connection getConnection(String host, int port, String dbname, String username, String password, String appname, String appVersion, String hostname, Properties connProps) 
	throws ClassNotFoundException, SQLException
	{
		return getConnection(host+":"+port, dbname, username, password, appname, appVersion, hostname, connProps, (ConnectionProgressCallback)null);
	}

	/** get a connection using the static settings priviously made, but override the input parameters for this method */
	public static Connection getConnection(String hosts, String ports, String dbname, String username, String password, String appname, String appVersion, String hostname, Properties connProps) 
	throws ClassNotFoundException, SQLException
	{
		return getConnection(toHostPortStr(hosts,ports), dbname, username, password, appname, appVersion, hostname, connProps, (ConnectionProgressCallback)null);
	}
	/** get a connection */
	public static Connection getConnection(String srvname, String dbname, String username, String password, String appname)
	throws ClassNotFoundException, SQLException
	{
		String hostPortStr;
		if (srvname.indexOf(':') >= 0) // If it's already a host:port, use that...
			hostPortStr = srvname;
		else
			hostPortStr = AseConnectionFactory.getIHostPortStr(srvname);

		return getConnection(hostPortStr, dbname, username, password, appname, null, null, null, (ConnectionProgressCallback)null);
	}
	

//	 * get a connection using all the input parameters (this does not use the static fields previously set) 
	public static Connection getConnection(String hostPortStr, String dbname, String username, String password, String appname, String appVersion, String hostname, Properties connProps, ConnectionProgressCallback cpc) 
	throws ClassNotFoundException, SQLException
	{
		String url = getUrlTemplate();

		if (url         == null) throw new SQLException("No proper URL was passed. url='"+url+"'.");
		if (hostPortStr == null) throw new SQLException("No proper hostPortStr was passed. hostPortStr='"+hostPortStr+"'.");

		//		url = url.replaceAll("HOST", host);
//		url = url.replaceAll("PORT", Integer.toString(port));
		url = url.replaceAll("HOST:PORT", hostPortStr);

		// If hostname is not specified, get the current host and use that...
		if (hostname == null || (hostname != null && hostname.trim().equals("")))
		{
			hostname = getLocalHostname();
		}

		// If password is "null", then make it an empty password
		if (password != null && password.equalsIgnoreCase("null"))
			password = "";
		
		Properties props = new Properties(_props);
		if (props.getProperty("user")            == null && username != null) props.put("user",            username);
		if (props.getProperty("password")        == null && password != null) props.put("password",        password);
		if (props.getProperty("APPLICATIONNAME") == null && appname  != null) props.put("APPLICATIONNAME", appname);
		if (props.getProperty("HOSTNAME")        == null && hostname != null) props.put("HOSTNAME",        hostname);

		// I just invented this propery, so it's NOT supported by the jConnect, but we can use it to 'set clientapplname, set clientname, set clienthostname'
		if (props.getProperty("CLIENT_APPLICATION_VERSION") == null && appVersion != null) props.put("CLIENT_APPLICATION_VERSION", appVersion);

		// Forces jConnect to cancel all Statements on a Connection when a
		// read timeout is encountered. This behavior can be used when a
		// client has calls execute() and the timeout occurs because of a
		// deadlock (for example, trying to read from a table that is currently
		// being updated in another transaction). 
		// The default value is false.
		if (props.getProperty("QUERY_TIMEOUT_CANCELS_ALL") == null) 
			props.put("QUERY_TIMEOUT_CANCELS_ALL", "true");

		// some applications might want to have additional OPTIONS (if not already set)
		// Lets put those "default" options in (if not already set)
		// For example in 'sqlw' this might be the 'IGNORE_DONE_IN_PROC=true' 
		if (props.getProperty("APPLICATIONNAME") != null)
		{
			String tmpAppName = props.getProperty("APPLICATIONNAME");
			Properties tmpProps = _defaultAppNameProps.get(tmpAppName);
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

		// if host,port is in the interfaces/sql.ini, set the SERVICENAME property
//		String serverName = AseConnectionFactory.getIServerName(hostPortStr);
//		if ( serverName != null && ! serverName.trim().equals("") )
//			if (props.getProperty("SERVICENAME") == null) 
//				props.put("SERVICENAME", serverName);

		// Set database
//		if (dbname != null && !dbname.trim().equals(""))
//			url += "/"+dbname;
		if (dbname != null && !dbname.trim().equals(""))
			props.put("DATABASE", dbname);

		// add all passed properties...
		if (connProps != null)
			props.putAll(connProps);

		return getConnection(_driver, url, props, cpc);
	}

	/** get a connection using all the input parameters (this does not use the static fields previously set) */
	public static Connection getConnection(String url, String username, String password, Properties connProps, ConnectionProgressCallback cpc) 
	throws ClassNotFoundException, SQLException
	{
		String hostname = getLocalHostname();

		// If password is "null", then make it an empty password
		if (password != null && password.equalsIgnoreCase("null"))
			password = "";
		
		Properties props = new Properties(_props);
		if (props.getProperty("user")            == null) props.put("user",            username);
		if (props.getProperty("password")        == null) props.put("password",        password);
		if (props.getProperty("APPLICATIONNAME") == null) props.put("APPLICATIONNAME", _appname);
		if (props.getProperty("HOSTNAME")        == null) props.put("HOSTNAME",        hostname);
//		if (props.getProperty("DISABLE_UNPROCESSED_PARAM_WARNINGS") == null) props.put("DISABLE_UNPROCESSED_PARAM_WARNINGS", "true");

		// Forces jConnect to cancel all Statements on a Connection when a
		// read timeout is encountered. This behavior can be used when a
		// client has calls execute() and the timeout occurs because of a
		// deadlock (for example, trying to read from a table that is currently
		// being updated in another transaction). 
		// The default value is false.
		if (props.getProperty("QUERY_TIMEOUT_CANCELS_ALL") == null) 
			props.put("QUERY_TIMEOUT_CANCELS_ALL", "true");

		if (connProps != null)
			props.putAll(connProps);

		return getConnection(_driver, url, props, cpc);
	}

	/**
	 * get a connection using all the input parameters (this does not use the static fields previously set)
	 * <p>
	 * If the there are multiple host:port in the URL, multiple connect attempt is tried<br>
	 * This can be turned of wit the property 'AseConnectionFactory.emulateMultipleQueryRowSupport=false'
	 * <p>
	 * Also if the property 'application.gui=true', some GUI feedback will be displayed
	 * 
	 * @param driverClassName
	 * @param url
	 * @param props
	 * @return The connection
	 * 
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	public static Connection getConnection(String driverClassName, String url, final Properties props, final ConnectionProgressCallback cpd) 
	throws ClassNotFoundException, SQLException
	{
		// if we are printing out debug information, strip off the password
		Properties debugProps = null;
		if (_logger.isDebugEnabled())
		{
			debugProps = new Properties(props);
			debugProps.remove("password");
			debugProps.remove("PASSWORD");
		}
		
		// Look up the JDBC driver by class name.  When the class loads, it
		// automatically registers itself with the DriverManager used in
		// the next step.
//		Class.forName(driverClassName);
		// If no suitable driver can be found for the URL, to to load it "the old fashion way" (hopefully it's in the classpath)
		try
		{
//System.out.println("AseConnectionFactory.getConnection(driverClassName='"+driverClassName+"', url='"+url+"')");
			Driver jdbcDriver = DriverManager.getDriver(url);
			if (jdbcDriver == null)
				Class.forName(driverClassName).newInstance();
		}
		catch (Throwable ex)
		{
			_logger.warn( "Can't locate JDBC driver '"+driverClassName+"' for URL='"+url+"' using 'DriverManager.getDriver(url)' Lets continue, but first try to load the class '"+driverClassName+"' using 'Class.forName(driver).newInstance()' then connect to it using: DriverManager.getConnection(url, props); Caught="+ex);
			_logger.debug("Can't locate JDBC driver '"+driverClassName+"' for URL='"+url+"' using 'DriverManager.getDriver(url)' Lets continue, but first try to load the class '"+driverClassName+"' using 'Class.forName(driver).newInstance()' then connect to it using: DriverManager.getConnection(url, props); Caught="+ex, ex);

			try { Class.forName(driverClassName).newInstance(); }
			catch( Throwable ex2 )
//			catch( ClassNotFoundException | InstantiationException | IllegalAccessException ex2 )
			{
				_logger.warn("DriverManager.getDriver(url), threw Exception '"+ex+"', so we did 'Class.forName(driverClass).newInstance()', and that caused: "+ex2);
			}
		}

		// Get some configuration....
		boolean emulateMultipleQueryRowSupport = true;
		int loginTimeout = DEFAULT_LOGINTIMEOUT;
//		Configuration conf = Configuration.getInstance(Configuration.CONF);
		Configuration conf = Configuration.getCombinedConfiguration();
		if (conf != null)
		{
			emulateMultipleQueryRowSupport = conf.getBooleanProperty("AseConnectionFactory.emulateMultipleQueryRowSupport", true);
			loginTimeout                   = conf.getIntProperty(PROPKEY_LOGINTIMEOUT, loginTimeout);
		}
		else
		{
			emulateMultipleQueryRowSupport = System.getProperty("AseConnectionFactory.emulateMultipleQueryRowSupport", "true").trim().equalsIgnoreCase("true");
			try { loginTimeout = Integer.parseInt(System.getProperty(PROPKEY_LOGINTIMEOUT, loginTimeout+"")); }
			catch (NumberFormatException ignore) {}
		}
		
		// Parse the URL to check for multiple host:port entries
		AseUrlHelper urlHelper = null;
		try { urlHelper = AseUrlHelper.parseUrl(url); }
		catch (ParseException ignore) 
		{
			_logger.debug("Caught Exception when parsing the URL string '"+url+"'. Cause: "+ignore, ignore);
		}

		// if option REQUEST_HA_SESSION is set to TRUE, then DO NOT emulate "Multiple Query Row" 
		if (props != null && props.containsKey("REQUEST_HA_SESSION"))
			if ( props.getProperty("REQUEST_HA_SESSION").trim().equalsIgnoreCase("true") )
				emulateMultipleQueryRowSupport = false;
		if (url.indexOf("REQUEST_HA_SESSION=true") > 0)
			emulateMultipleQueryRowSupport = false;

		// allow {xxx} strings in jConnect
		// JDBC uses {str} as special stuff (SQL Escape Sequences for JDBC)
		// http://docs.oracle.com/cd/E13222_01/wls/docs91/jdbc_drivers/sqlescape.html
		if (props.getProperty("ESCAPE_PROCESSING_DEFAULT") == null) 
			props.put("ESCAPE_PROCESSING_DEFAULT", "false");

		// IS_CLOSED_TEST=INTERNAL
		// This means that isClosed() test does not send any SQL to server
		// The INTERNAL setting means that jConnect will return true for isClosed() only when Connection.close() has been called, or when jConnect has detected an IOException that has disabled the Connection
		// if to many sp_mda calls is executed, try set IS_CLOSED_TEST="select 1" instead

		//-----------------------------------------------------
		// CONNECT NOW
		//-----------------------------------------------------
		
		// NORMAL CONNECT (NO GUI PROGRESS)
		Connection conn = null;
		if ( emulateMultipleQueryRowSupport == false || urlHelper == null)
		{
			if (_logger.isDebugEnabled())
				_logger.debug("getConnection(simple url): driverClassName='"+driverClassName+"', url='"+url+"', props='"+debugProps+"'");

			//-----------------------------------------------------
			// Now use that driver to connect to the database
			//-----------------------------------------------------
			DriverManager.setLoginTimeout(loginTimeout);
			conn = DriverManager.getConnection(url, props);
		}
		else // EMULATE MULTIPLE QUERY ROW (WITH gui progress if Progress object was passed)
		{
			SQLException firstSqlex = null;
			List<String> urlList = urlHelper.getUrlList();

			for (Iterator<String> it = urlList.iterator(); it.hasNext();)
			{
				final String urlEntry = it.next();
				try
				{
					if (_logger.isDebugEnabled())
						_logger.debug("getConnection(MULTY_QUERY_ROWS): driverClassName='"+driverClassName+"', url='"+urlEntry+"', props='"+debugProps+"'");

					// UPDATE PROGRESS
					if (cpd != null)
						cpd.setTaskStatus(urlEntry, ConnectionProgressCallback.TASK_STATUS_CURRENT);

					//-----------------------------------------------------
					// Now use that driver to connect to the database
					//-----------------------------------------------------
//System.out.println("getConnection(MULTY_QUERY_ROWS): driverClassName='"+driverClassName+"', url='"+urlEntry+"', props='"+debugProps+"'");
					DriverManager.setLoginTimeout(loginTimeout);
					conn = DriverManager.getConnection(urlEntry, props);

					if (_logger.isDebugEnabled())
						_logger.debug("getConnection(MULTY_QUERY_ROWS): ---- SUCCEEDED ---- url='"+urlEntry+"', props='"+debugProps+"'.");

					// UPDATE PROGRESS
					if (cpd != null)
						cpd.setTaskStatus(urlEntry, ConnectionProgressCallback.TASK_STATUS_SUCCEEDED);

					// get out of the loop when a connection is made.
					break; 
				}
				catch (SQLException e)
				{
					// UPDATE PROGRESS
					if (cpd != null)
						cpd.setTaskStatus(urlEntry, ConnectionProgressCallback.TASK_STATUS_FAILED, e);

					// Add all subsequent exceptions to the first one
					if (firstSqlex == null)
						firstSqlex = e;
					else
						firstSqlex.setNextException(e);

					// Decide if we should throw a error now or wait until later
					boolean throwNow = false;

					// JZ00L: Login failed.
					if (e.getSQLState().equals("JZ00L")) 
						throwNow = true;

//					// JZ006: Caught IOException
//					if (e.getSQLState().equals("JZ006")) 
//						throwNow = false;

					// If we are on the LAST entry, we need to throw the exception.
					if ( ! it.hasNext() )
						throwNow = true;

					if (throwNow)
					{
						// UPDATE PROGRESS
						if (cpd != null)
							cpd.setTaskStatus(urlEntry, ConnectionProgressCallback.TASK_STATUS_FAILED_LAST, e);

						throw firstSqlex;
					}
					else
					{
						_logger.warn("Connecting to '"+urlEntry+"', had problems, but more host/port will be tried. Caught: " + e);
					}
				}
			}
		}

		// Write info about what JDBC driver we connects via.
		if ( ! _jdbcDriverInfoHasBeenWritten )
		{
			_jdbcDriverInfoHasBeenWritten = true;
			try 
			{
				if (_logger.isDebugEnabled()) 
				{
					_logger.debug("The following drivers have been loaded:");
					Enumeration<Driver> drvEnum = DriverManager.getDrivers();
					while( drvEnum.hasMoreElements() )
					{
						_logger.debug("    " + drvEnum.nextElement().toString());
					}
				}

				DatabaseMetaData dbmd = conn.getMetaData();
				if (dbmd != null)
				{
					_logger.info("JDBC driver version: " + dbmd.getDriverVersion());
				}
			} 
			catch (SQLException ignore) {}
		}

		// Get the product name
		// Note: RepServer and possibly other OpenServer/JTDS implementetions will throw: JZ0SJ: Metadata accessor information was not found on this database. Please install the required tables as mentioned in the jConnect documentation.
		String productName = "";
		try { 
			DatabaseMetaData dbmd = conn.getMetaData();
			if (dbmd != null)
				productName = dbmd.getDatabaseProductName();
		}
		catch(SQLException ex) {}

		// Only try the below if we have a volid product name
		if (StringUtil.hasValue(productName))
		{
			// auto commit ceheck/set
			try 
			{
				if (conn.getAutoCommit() == false)
				{
					_logger.info("AutoCommit was turned 'off'. I will turn this to 'on' for this connection.");
					conn.setAutoCommit(true);
				}
			}
			catch(SQLException ex) 
			{
				_logger.info("Problems getting/setting AutoCommit. Caught: "+ex);
			}
				
			// Only for Sybase ASE
			if (DbUtils.DB_PROD_NAME_SYBASE_ASE.equals(productName))
			{
				// make strings unaware of quotation ""
				// meaning SQL statements like: print "some string" will work...
				try
				{
					// If this is not set to 'off', things like (print "any string") wont work
					conn.createStatement().execute("set quoted_identifier off");

					if (props != null)
					{
						String aseSetStr = "";
						String appName    = props.getProperty("APPLICATIONNAME");
						String hostName   = props.getProperty("HOSTNAME");
						String appVersion = props.getProperty("CLIENT_APPLICATION_VERSION");

						if (StringUtil.hasValue(appName))    aseSetStr += "set clientname '"     + appName  + "' \n";
						if (StringUtil.hasValue(hostName))   aseSetStr += "set clienthostname '" + hostName + "' \n";
						if (StringUtil.hasValue(appVersion)) aseSetStr += "set clientapplname '" + appName + " - " + appVersion  + "' \n";

						if (StringUtil.hasValue(aseSetStr))
							conn.createStatement().execute(aseSetStr);
					}
				}
				catch (SQLException sqle)
				{
					String errStr = "";
					while (sqle != null)
					{
						errStr += sqle.getMessage() + " ";
						sqle = sqle.getNextException();
					}
					_logger.warn("Failed to execute 'set quoted_identifier off' when connecting. Problem: "+errStr);
				}
			} // end: SYBASE_ASE
		} // end: hasValue(productName)

		return conn;
	}

	
	public static void printConnectionPropertyInfo() 
	{
		printConnectionPropertyInfo(null, null);
	}
	public static void printConnectionPropertyInfo(String driverClassName, String url) 
	{
		if (driverClassName == null)
			driverClassName = _driver;
		
		if (url == null)
			url = _urlTemplate;

		Driver driver = null;
		DriverPropertyInfo[] attributes = null;

		try
		{
			Class.forName(driverClassName);
	
			Properties info = new Properties();
			driver = DriverManager.getDriver(url);
	
			attributes = driver.getPropertyInfo(url, info);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return;
		}

//		System.out.println("driver=" + driver);
//		System.out.println("attributes=" + attributes);

		// zero length means a connection attempt can be made
		System.out.println("Resolving properties for: " + driver.getClass().getName());

		for (int i = 0; i < attributes.length; i++)
		{
			// get the property metadata
			String   name        = attributes[i].name;
			String[] choicesArr  = attributes[i].choices;
			boolean  required    = attributes[i].required;
			String   description = attributes[i].description;
			String   value       = attributes[i].value;

			String choises = "-none-";
			if (choicesArr != null && choicesArr.length > 0)
			{
				choises = "";
				for (int j = 0; j < choicesArr.length; j++)
					choises += choicesArr[j] + ", ";
				if (choises.endsWith(", "))
					choises = choises.substring(0, choises.length()-2);
			}
			else

			
			// printout property metadata
			System.out.println("\n-----------------------------------------------------------");
			System.out.println(" Name:        " + name);
			System.out.println(" Required:    " + required);
			System.out.println(" Value:       " + value);
			System.out.println(" Choices are: ");
			System.out.println(" Description: " + description);
		}

	}

//	public Connection newConnection(Properties inProps, boolean logErrors)
//	throws ManageException
////	throws NotConnectedException
//	{
//		Connection conn = null;
//		try
//		{
//			String jdbcDriver = "com.sybase.jdbc42.jdbc.SybDriver";
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

//	/*---------------------------------------------------
//	** BEGIN: ShowConnectionProgressDialog
//	**---------------------------------------------------
//	*/
//	private static class ShowConnectionProgressDialog 
//	extends JDialog implements ActionListener
//	{
//        private static final long serialVersionUID = 1L;
//
//        private JLabel     _hostPort_lbl = new JLabel("Connecting to: ");
//        private JTextField _hostPort_txt = new JTextField();
//
//        private JButton   _ok_but = new JButton("OK");
//
//        private ShowConnectionProgressDialog(Dialog owner)
//		{
//			super(owner);
////			super(owner, false);
//
////			if (_icon != null && _icon instanceof ImageIcon)
////				((Frame)this.getOwner()).setIconImage(((ImageIcon)_icon).getImage());
//
//			initComponents();
//
//			// Set initial size
////			int width  = (3 * Toolkit.getDefaultToolkit().getScreenSize().width)  / 4;
////			int height = (3 * Toolkit.getDefaultToolkit().getScreenSize().height) / 4;
////			setSize(width, height);
//			pack();
//
//			Dimension size = getPreferredSize();
//			size.width = 700;
//
//			setPreferredSize(size);
////			setMinimumSize(size);
//			setSize(size);
//
//			setLocationRelativeTo(owner);
//
//			setVisible(true);
//		}
//
//        protected void initComponents() 
//		{
//			setTitle("Connection Information");
//			
//			JPanel panel = new JPanel();
//			panel.setLayout(new MigLayout("ins 0","[fill]",""));
//
////			JScrollPane scroll = new JScrollPane( init() );
////			scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
//
//			panel.add(init(),   "height 100%, wrap 15");
//			panel.add(_ok_but,  "tag ok, gapright 15, bottom, right, pushx, wrap 15");
//
//			setContentPane(panel);
//
//			// ADD ACTIONS TO COMPONENTS
//			_ok_but.addActionListener(this);
//		}
//
//        protected JPanel init() 
//		{
//			JPanel panel = new JPanel();
//			panel.setLayout(new MigLayout("insets 20 20 20 20","[grow]",""));
//
//			panel.add(_hostPort_lbl, "");
//			panel.add(_hostPort_txt, "push, grow");
//
//			return panel;
//		}
//		public void actionPerformed(ActionEvent e)
//		{
//			if ( _ok_but.equals(e.getSource()) )
//			{
//				dispose();
//			}
//		}
//		
//		public void setHostPort(String hostPort)
//		{
//			_hostPort_txt.setText(hostPort);
//		}
//	}
//	/*---------------------------------------------------
//	** END: ShowConnectionProgressDialog
//	**---------------------------------------------------
//	*/
}
