package com.asetune.sql.conn;

import java.util.Properties;

public class ConnectionProp
{
	protected String _username = null;
	protected String _password = null;
	
	protected String _server = null;
	protected String _dbname = null; // or the catalog

	protected String _driver = null;
	protected String _url    = null;

	protected Properties _props    = null;

	public String     getUsername() { return _username; }
	public String     getPassword() { return _password; }
	public String     getServer()   { return _server; }
	public String     getDbname()   { return _dbname; }
	public String     getDriver()   { return _driver; }
	public String     getUrl()      { return _url; }
	public Properties getProps()    { return _props; }

	public void setUsername(String username)  { _username = username; }
	public void setPassword(String password)  { _password = password; }
	public void setServer  (String server)    { _server   = server; }
	public void setDbname  (String dbname)    { _dbname   = dbname; }
	public void setDriver  (String driver)    { _driver   = driver; }
	public void setUrl     (String url)       { _url      = url; }
	public void setProps   (Properties props) { _props    = props; }

	
	public void setProperty(String key, String value)
	{
		if (_props == null)
			_props = new Properties();
		
		_props.setProperty(key, value);
	}

	public String getProperty(String key)
	{
		if (_props == null)
			_props = new Properties();
		
		return getProperty(key, null);
	}

	public String getProperty(String key, String defaultValue)
	{
		if (_props == null)
			_props = new Properties();
		
		return _props.getProperty(key, defaultValue);
	}
}
