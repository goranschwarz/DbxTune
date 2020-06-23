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
package com.asetune.central.pcs;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.eclipse.jetty.security.IdentityService;
import org.eclipse.jetty.security.MappedLoginService;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.util.security.Credential;

import com.asetune.central.pcs.objects.DbxCentralUser;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;

public class DbxCentralRealm extends MappedLoginService
{
	private static Logger _logger = Logger.getLogger(DbxCentralRealm.class);

	public static final String ROLE_ADMIN = "admin";
	public static final String ROLE_USER  = "user";

	public static final String PROPKEY_USER_ADMIN_FALLBACK = "DbxCentralRealm.user.admin.fallback";
	public static final String DEFAULT_USER_ADMIN_FALLBACK = "admin";

	private String	   _config = "";
	private int		   _cacheTime;
	private long	   _lastHashPurge;

    /* ------------------------------------------------------------ */
	public DbxCentralRealm() throws IOException
	{
//System.out.println(DbxCentralRealm.class.getSimpleName() + "constructor(empty)");
	}

	/* ------------------------------------------------------------ */
	public DbxCentralRealm(String name) throws IOException
	{
//System.out.println(DbxCentralRealm.class.getSimpleName() + "constructor(String name): name='"+name+"'.");
		setName(name);
	}

	/* ------------------------------------------------------------ */
	public DbxCentralRealm(String name, String config) throws IOException
	{
//System.out.println(DbxCentralRealm.class.getSimpleName() + "constructor(String name, String config) name='"+name+"', config='"+config+"'.");
		setName(name);
		setConfig(config);
	}

	/* ------------------------------------------------------------ */
	public DbxCentralRealm(String name, IdentityService identityService, String config) throws IOException
	{
//System.out.println(DbxCentralRealm.class.getSimpleName() + "constructor(String name, IdentityService identityService, String config) name='"+name+"', identityService='"+identityService+"', config='"+config+"'.");
		setName(name);
		setIdentityService(identityService);
		setConfig(config);
	}	
	
	/* ------------------------------------------------------------ */
	/**
	 * @see org.eclipse.jetty.security.MappedLoginService#doStart()
	 */
	@Override
	protected void doStart() throws Exception
	{
//System.out.println(DbxCentralRealm.class.getSimpleName() + "doStart()");
//		Properties properties = new Properties();
//		Resource resource = Resource.newResource(_config);
//		properties.load(resource.getInputStream());
//
//		_cacheTime = new Integer(properties.getProperty("cachetime"));
//
//		_cacheTime    *= 1000;
//		_lastHashPurge = 0;

		_cacheTime     = 3600 * 1000; // 1 Hours
		_lastHashPurge = 0;
		
		super.doStart();
	}

	/* ------------------------------------------------------------ */
	public String getConfig()
	{
		return _config;
	}

	/* ------------------------------------------------------------ */
	/**
	 * Load JDBC connection configuration from properties file.
	 * 
	 * @param config
	 *            Filename or url of user properties file.
	 */
	public void setConfig(String config)
	{
		if ( isRunning() )
			throw new IllegalStateException("Running");
		_config = config;
		
//System.out.println(DbxCentralRealm.class.getSimpleName() + " : CONFIG: "+config);
	}

	/* ------------------------------------------------------------ */
	@Override
	public UserIdentity login(String username, Object credentials)
	{
//System.out.println(DbxCentralRealm.class.getSimpleName() + ".login(String username, Object credentials): username='"+username+"', credentials='"+credentials+"'.");
		long now = System.currentTimeMillis();
		if ( now - _lastHashPurge > _cacheTime || _cacheTime == 0 )
		{
			// Clear information in the super class 
			_users.clear();
			_lastHashPurge = now;
			//closeConnection();
		}

		// Let the (super MappedLoginService) do it's work 
		UserIdentity uid = super.login(username, credentials);

		_logger.info("Authenticating username '" + username + "' " +( uid == null ? "FAILED" : "SUCCEEDED") );

		// if FAILED for ADMIN... then do some extra: print into in loadUser() 
		if (uid == null && "admin".equals(username))
		{
			// Skip the results: just after the printout 'ADMIN_USER_PASSWORD: User autentication... IP Address'
			loadUser(username);
		}

		return uid;
	}

	/* ------------------------------------------------------------ */
	@Override
	protected void loadUsers()
	{
	}

	/* ------------------------------------------------------------ */
	@Override
	protected UserIdentity loadUser(String username)
	{
//System.out.println(DbxCentralRealm.class.getSimpleName() + ".loadUser(String username): username='"+username+"'.");
		try
		{
			if ( ! CentralPersistReader.hasInstance() )
				throw new SQLException("There is NO CentralPersistReader instance. Sorry can't continue to autenticate username '"+username+"'.");

			// Use the PCS to read information about the user
			DbxCentralUser dbxcUser = CentralPersistReader.getInstance().getDbxCentralUser(username);

			String   passwd = null;
			String[] roles  = null;
			if (dbxcUser != null)
			{
				passwd = dbxcUser.getPassword();
				roles  = dbxcUser.getRoles();
			}

			// Special case if it's the ADMIN login
			// and it could not be found in the database... use IP ADDRESS as password
			if ("admin".equals(username) && StringUtil.isNullOrBlank(passwd))
			{
				roles = new String[] {ROLE_ADMIN, ROLE_USER};
				try
				{
					// The below did not work on machines with multiple NIC interfaces
					//InetAddress inetAddress = InetAddress.getLocalHost();
					//passwd = inetAddress.getHostAddress();
					
					// So lets try to create a socket connection, which uses the "default" NW, and then get the IP of that 
					try( final DatagramSocket socket = new DatagramSocket())
					{
						socket.connect(InetAddress.getByName("8.8.8.8"), 10002); // Note the 
						passwd = socket.getLocalAddress().getHostAddress();
					}

					// fallback - 1   (on OSX it sometimes return "0.0.0.0")
					if (StringUtil.isNullOrBlank(passwd) || "0.0.0.0".equals(passwd))
					{
						InetAddress inetAddress = InetAddress.getLocalHost();
						passwd = inetAddress.getHostAddress();
					}

					// fallback - 2
					if (StringUtil.isNullOrBlank(passwd) || "0.0.0.0".equals(passwd))
					{
						passwd = Configuration.getCombinedConfiguration().getProperty(PROPKEY_USER_ADMIN_FALLBACK, DEFAULT_USER_ADMIN_FALLBACK);
					}

					_logger.info("ADMIN_USER_PASSWORD: User autentication with username 'admin' - No entry found in DbxCentral DB. Using '"+passwd+"' as the default password.");
				}
				catch (UnknownHostException | SocketException ex)
				{
					passwd = Configuration.getCombinedConfiguration().getProperty(PROPKEY_USER_ADMIN_FALLBACK, DEFAULT_USER_ADMIN_FALLBACK);
					_logger.info("ADMIN_USER_PASSWORD: User autentication with username 'admin' - Problems getting the IP address of the current host. Using '"+passwd+"' as the default password.");
				}
			}
				
			// If use was found: Put the information in the (super MappedLoginService) user table... 
			UserIdentity uid = null;
			if (passwd != null)
				uid = super.putUser(username, Credential.getCredential(passwd), roles);

			return uid;
		}
		catch (SQLException e)
		{
			_logger.warn("UserRealm " + getName() + " could not load user information from database", e);
		}
		return null;
	}
}

//-------------------------------------------------------------------------------
// You may check the below links for other Realm code
// https://github.com/dekellum/jetty/blob/master/jetty-security/src/main/java/org/eclipse/jetty/security/JDBCLoginService.java
// https://github.com/dekellum/jetty/blob/master/jetty-security/src/main/java/org/eclipse/jetty/security/HashLoginService.java
//-------------------------------------------------------------------------------
