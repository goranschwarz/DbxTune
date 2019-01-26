/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
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

import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.asetune.ssh.SshTunnelInfo;
import com.asetune.utils.StringUtil;

public class ConnectionProp
{
	private static Logger _logger = Logger.getLogger(ConnectionProp.class);

	protected String _username = null;
	protected String _password = null;
	
	protected String _server = null;
	protected String _dbname = null; // or the catalog

	protected int    _loginTimeout = -1;
	protected String _driverClass = null;
	protected String _url    = null;

	protected Properties _urlOptions = null;

	protected String _appName    = null;
	protected String _appVersion = null; 

	protected SshTunnelInfo _sshTunnelInfo = null;

	public ConnectionProp()
	{
	}
	public ConnectionProp(ConnectionProp cp)
	{
		_username      = cp._username;
		_password      = cp._password;

		_server        = cp._server;
		_dbname        = cp._dbname;

		_loginTimeout  = cp._loginTimeout;
		_driverClass   = cp._driverClass;
		_url           = cp._url;

//		_urlOptions    = (cp._urlOptions == null) ? null : new Properties(cp._urlOptions);
		if (cp._urlOptions != null)
		{
			_urlOptions = new Properties();
			_urlOptions.putAll(cp._urlOptions);
		}

		_appName       = cp._appName;
		_appVersion    = cp._appVersion;
		
		_sshTunnelInfo = cp._sshTunnelInfo;
	}

	@Override
	public ConnectionProp clone()
	{
		return new ConnectionProp(this);
	}

	
	public String        getUsername()      { return _username; }
	public String        getPassword()      { return _password; }
	public String        getServer()        { return _server; }
	public String        getDbname()        { return _dbname; }
	public int           getLoginTimeout()  { return _loginTimeout; }
	public String        getDriverClass()   { return _driverClass; }
	public String        getUrl()           { return _url; }
	public Properties    getUrlOptions()    { return _urlOptions; }
	public String        getAppName()       { return _appName; }
	public String        getAppVersion()    { return _appVersion; }
	public SshTunnelInfo getSshTunnelInfo() { return _sshTunnelInfo; }

	public void setUsername     (String        username)        { _username      = username; }
	public void setPassword     (String        password)        { _password      = password; }
	public void setServer       (String        server)          { _server        = server; }
	public void setDbname       (String        dbname)          { _dbname        = dbname; }
	public void setLoginTimeout (String        loginTimeoutStr) { _loginTimeout  = StringUtil.parseInt(loginTimeoutStr, -1); }
	public void setLoginTimeout (int           loginTimeout)    { _loginTimeout  = loginTimeout; }
	public void setDriverClass  (String        driverClass)     { _driverClass   = driverClass; }
	public void setUrl          (String        url)             { _url           = url; }
	public void getUrlOptions   (Properties    urlOptions)      { _urlOptions    = urlOptions; }
	public void setAppName      (String        appName)         { _appName       = appName; }
	public void setAppVersion   (String        appVersion)      { _appVersion    = appVersion; }
	public void setSshTunnelInfo(SshTunnelInfo sshTunnelInfo)   { _sshTunnelInfo = sshTunnelInfo; }

	public void setUrlOption(String key, String value)
	{
		if (_urlOptions == null)
			_urlOptions = new Properties();
		
		_urlOptions.setProperty(key, value);
	}

	public String getUrlOption(String key)
	{
		return getUrlOption(key, null);
	}

	public String getUrlOption(String key, String defaultValue)
	{
		if (_urlOptions == null)
			_urlOptions = new Properties();
		
		return _urlOptions.getProperty(key, defaultValue);
	}

	public void setUrlOptions(String urlOptions)
	{
		setUrlOptions(urlOptions, false);
	}

	public void setUrlOptions(Properties urlOptions)
	{
		_urlOptions = urlOptions;

		if (_urlOptions == null)
			_urlOptions = new Properties();
	}

	public void setUrlOptions(String urlOptions, boolean resetAllPrev)
	{
		if (_urlOptions == null || resetAllPrev)
			_urlOptions = new Properties();

		if (StringUtil.hasValue(urlOptions))
		{
			Map<String, String> urlMap = StringUtil.parseCommaStrToMap(urlOptions);
			for (String key : urlMap.keySet())
			{
				String val = urlMap.get(key);
				_urlOptions.put(key, val);
			}
		}
	}
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("_username")     .append("=").append(_username)     .append(", ");

		if (_logger.isDebugEnabled())
			sb.append("_password") .append("=").append(_password)     .append(", ");
		else
			sb.append("_password") .append("=").append("*secret*")    .append(", ");
		
		sb.append("_server")       .append("=").append(_server)       .append(", ");
		sb.append("_dbname")       .append("=").append(_dbname)       .append(", ");
		sb.append("_loginTimeout") .append("=").append(_loginTimeout) .append(", ");
		sb.append("_driverClass")  .append("=").append(_driverClass)  .append(", ");
		sb.append("_url")          .append("=").append(_url)          .append(", ");
		sb.append("_urlOptions")   .append("=").append(_urlOptions)   .append(", ");
		sb.append("_appName")      .append("=").append(_appName)      .append(", ");
		sb.append("_appVersion")   .append("=").append(_appVersion)   .append(", ");
		sb.append("_sshTunnelInfo").append("={").append(_sshTunnelInfo == null ? null : _sshTunnelInfo.getInfoString() ).append("}");

		return super.toString() + ": " + sb.toString();
	}
}
