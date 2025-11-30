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
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.dbxtune.sql;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MsSqlUrlHelper
{
	private static final String PREFIX = "jdbc:sqlserver://";

	private String  _host;
	private String  _instance;
	private Integer _port;
	private final Map<String, String> _properties = new LinkedHashMap<>();

	private MsSqlUrlHelper()
	{
	}

	public static MsSqlUrlHelper parse(String jdbcUrl)
	{
		if ( !jdbcUrl.startsWith(PREFIX) )
		{
			throw new IllegalArgumentException("Not a SQL Server JDBC URL: " + jdbcUrl);
		}

		MsSqlUrlHelper helper = new MsSqlUrlHelper();

		// Strip prefix
		String withoutPrefix = jdbcUrl.substring(PREFIX.length());

		// Split serverPart and _properties
		String[] split      = withoutPrefix.split(";", 2);
		String   serverPart = split[0];
		String   propsPart  = split.length > 1 ? split[1] : "";

		// Regex: _host\_instance:_port
		Pattern p = Pattern.compile("([^\\\\:]+)(?:\\\\([^:]+))?(?::(\\d+))?");
		Matcher m = p.matcher(serverPart);
		if ( !m.matches() )
		{
			throw new IllegalArgumentException("Invalid SQL Server server specification: " + serverPart);
		}

		helper._host = m.group(1);
		helper._instance = m.group(2);
		helper._port = m.group(3) != null ? Integer.valueOf(m.group(3)) : null;

		// Parse _properties into a map
		if ( ! propsPart.isEmpty() )
		{
			String[] kvPairs = propsPart.split(";");
			for (String kv : kvPairs)
			{
				if ( kv.isEmpty() )
					continue;
				String[] kvSplit = kv.split("=", 2);
				String   key     = kvSplit[0];
				String   value   = kvSplit.length > 1 ? kvSplit[1] : "";
				helper._properties.put(key, value);
			}
		}

		return helper;
	}

//	public static String extractHost(String srvName)
//	{
//		int bsPos = srvName.indexOf("\\");
//		if (bsPos >= 0)
//		{
//			String host     = srvName.substring(0, bsPos);
//			String instance = srvName.substring(bsPos + 1);
//			
//			return host;
//		}
//		
//		return srvName;
//	}

	public static String extractHost(String srvName)
	{
		int bsPos = srvName.indexOf("\\");
		if (bsPos >= 0)
		{
			return srvName.substring(0, bsPos);
		}
		
		return srvName;
	}
	
	public static String extractInstance(String srvName)
	{
		int bsPos = srvName.indexOf("\\");
		if (bsPos >= 0)
		{
			return srvName.substring(bsPos + 1);
		}
		
		return null;
	}
	
	
	public String getHost()
	{
		return _host;
	}

	public String getInstance()
	{
		return _instance;
	}

	public Integer getPort()
	{
//		if (_port == null)
//			return 1433;

		return _port;
	}

	public Integer getPort(int defaultPort)
	{
		if (_port == null)
			return defaultPort;

		return _port;
	}

	public Map<String, String> getProperties()
	{
		return _properties;
	}

	public String getHostInstance()
	{
		if (_instance == null)
			return _host;
		
		return _host + "\\" + _instance;
	}

	public String getHostPortStr()
	{
		StringBuilder sb = new StringBuilder();

		sb.append(_host);

		if ( _instance != null ) sb.append("\\").append(_instance);
		if ( _port     != null ) sb.append(":") .append(_port);

		return sb.toString();
	}

	public String getUrlOptions()
	{
		StringBuilder sb = new StringBuilder();

		_properties.forEach((k, v) -> sb.append(k).append("=").append(v).append(";"));

		// Remove last ";"
		if (sb.length() > 0)
			sb.setLength(sb.length() - 1);
		
		return sb.toString();
	}

	public boolean isHostFqdn()
	{
		if ( _host == null )
		{
			return false;
		}
		// Host must contain a dot and not end with a dot
		return _host.contains(".") && !_host.endsWith(".");
	}
	
	/**
	 * Set the host name (if 'name' contains a '\' then we also set the instance name
	 * @param name
	 */
	public void setHost(String name)
	{
		int bsPos = name.indexOf("\\");
		if (bsPos >= 0)
		{
			String host     = name.substring(0, bsPos);
			String instance = name.substring(bsPos + 1);

			this._host = host;
			this._instance = instance;
		}
		else
		{
			this._host = name;
		}
	}

	public void setInstance(String instance)
	{
		this._instance = instance;
	}

	public void setHostInstance(String hostInstance)
	{
		int bsPos = hostInstance.indexOf("\\");
		if (bsPos >= 0)
		{
			this._host     = hostInstance.substring(0, bsPos);
			this._instance = hostInstance.substring(bsPos + 1);
		}
		else
		{
			this._host = hostInstance;
		}
	}

	public void setPort(Integer port)
	{
		this._port = port;
	}

	public void setProperty(String key, String value)
	{
		_properties.put(key, value);
	}

	public String toUrl()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(PREFIX).append(_host);

		if ( _instance != null )
		{
			sb.append("\\").append(_instance);
		}

		if ( _port != null )
		{
			sb.append(":").append(_port);
		}

		if ( !_properties.isEmpty() )
		{
			sb.append(";");
			_properties.forEach((k, v) -> sb.append(k).append("=").append(v).append(";"));
			// Remove last ";"
			sb.setLength(sb.length() - 1);
		}

		return sb.toString();
	}

	// Example usage
	public static void main(String[] args)
	{
		String original = "jdbc:sqlserver://axsealisql02.motor.local\\INST01:50001;encrypt=true;loginTimeout=30";
//		original = "jdbc:sqlserver://prod-b1-mssql";

		MsSqlUrlHelper helper = MsSqlUrlHelper.parse(original);

		System.out.println("Host    : " + helper.getHost());
		System.out.println("Instance: " + helper.getInstance());
		System.out.println("Port    : " + helper.getPort());
		System.out.println("Props   : " + helper.getProperties());

		// Change just the _host
		helper.setHost("newhost");

		String updated = helper.toUrl();
		System.out.println("Updated : " + updated);
	}

}
