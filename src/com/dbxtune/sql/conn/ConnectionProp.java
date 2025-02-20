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
package com.dbxtune.sql.conn;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ssh.SshTunnelInfo;
import com.dbxtune.utils.StringUtil;

public class ConnectionProp
implements Cloneable
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

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
	
	protected String _sqlInit = null;

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
	public String        getPassword()      { return _password == null ? "" : _password; }
	public String        getServer()        { return _server; }
	public String        getDbname()        { return _dbname; }
	public int           getLoginTimeout()  { return _loginTimeout; }
	public String        getDriverClass()   { return _driverClass; }
	public String        getUrl()           { return _url; }
	public Properties    getUrlOptions()    { return _urlOptions; }
	public String        getAppName()       { return _appName; }
	public String        getAppVersion()    { return _appVersion; }
	public SshTunnelInfo getSshTunnelInfo() { return _sshTunnelInfo; }
	public String        getSqlInit()       { return _sqlInit; } 

	public void setUsername     (String        username)        { _username      = username; }
	public void setPassword     (String        password)        { _password      = password == null ? "" : password; }
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
	public void setSqlInit      (String        sqlInit)         { _sqlInit       = sqlInit; } 

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
	
	private int _onConnectErrorAutoCloseDialogDelayMs = 0;
	/**
	 * If A Connection GUI is visible and we FAIL to connect, normally we will stay in the dialog waiting...<br> 
	 * This can "auto close" the delay after X milliseconds!
	 * @param delayInMs   0 (or less than 0 which is the DEFAULT) means: Wait in the dialog forever... 
	 */
	public void setOnConnectErrorAutoCloseDialogDelayMs(int delayInMs)
	{
		_onConnectErrorAutoCloseDialogDelayMs = delayInMs;
	}

	/**
	 * If A Connection GUI is visible and we FAIL to connect, normally we will stay in the dialog waiting...<br> 
	 * This can "auto close" the delay after X milliseconds!
	 * @return  0 (or less) means: Wait in the dialog forever..., above 0 closes the dialog automatically after X ms 
	 */
	public int getOnConnectErrorAutoCloseDialogDelayMs()
	{
		return _onConnectErrorAutoCloseDialogDelayMs;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("_username")     .append("=").append(_username)     .append(", ");

		if (_logger.isDebugEnabled() || System.getProperty("DbxConnection.connect.nogui.print", "false").equalsIgnoreCase("true"))
			sb.append("_password") .append("=").append(_password)     .append(", ");
		else
			sb.append("_password") .append("=").append("*secret*")    .append(", ");
		
		// NOTE: The key "password" may also be in: _urlOptions ... this needs to be removed 
		String urlOptionsStr = "";
		if (_logger.isDebugEnabled() || System.getProperty("DbxConnection.connect.nogui.print", "false").equalsIgnoreCase("true"))
		{
			urlOptionsStr = _urlOptions + ""; // This will include any password in _urlOptions
		}
		else
		{
			if (_urlOptions != null)
			{
				// Copy _urlOptions and remove key "password"
				Properties tmpUrlOptions = new Properties(_urlOptions);
				tmpUrlOptions.remove("password");

				urlOptionsStr = tmpUrlOptions + ""; // no "password" key in the copy of _urlOptions
			}
		}
		

		sb.append("_server")       .append("=").append(_server)       .append(", ");
		sb.append("_dbname")       .append("=").append(_dbname)       .append(", ");
		sb.append("_loginTimeout") .append("=").append(_loginTimeout) .append(", ");
		sb.append("_driverClass")  .append("=").append(_driverClass)  .append(", ");
		sb.append("_url")          .append("=").append(_url)          .append(", ");
		sb.append("_urlOptions")   .append("=").append(urlOptionsStr) .append(", ");
		sb.append("_appName")      .append("=").append(_appName)      .append(", ");
		sb.append("_appVersion")   .append("=").append(_appVersion)   .append(", ");
		sb.append("_sqlInit")      .append("=").append(_sqlInit)      .append(", ");
		sb.append("_sshTunnelInfo").append("={").append(_sshTunnelInfo == null ? null : _sshTunnelInfo.getInfoString() ).append("}");

		return super.toString() + ": " + sb.toString();
	}

}
