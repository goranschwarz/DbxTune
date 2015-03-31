package com.asetune.sql.conn;

import java.util.Map;
import java.util.Properties;

import com.asetune.ssh.SshTunnelInfo;
import com.asetune.utils.StringUtil;

public class ConnectionProp
{
	protected String _username = null;
	protected String _password = null;
	
	protected String _server = null;
	protected String _dbname = null; // or the catalog

	protected int    _loginTimeout = -1;
	protected String _driver = null;
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
		_driver        = cp._driver;
		_url           = cp._url;

		_urlOptions    = (cp._urlOptions == null) ? null : new Properties(cp._urlOptions);

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
	public String        getDriver()        { return _driver; }
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
	public void setDriver       (String        driver)          { _driver        = driver; }
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
}
