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
package com.dbxtune.utils;

import java.lang.invoke.MethodHandles;
import java.text.ParseException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;



/**
 * @author gorans
 *
 */
public class AseUrlHelper
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long   serialVersionUID   = 1L;
	
	private static String _urlStart = "jdbc:sybase:Tds:";

	private String                   _originUrl  = null;
	private String                   _serverName = null; // if the we have an entry in the interfaces/sql.ini file, this is the responding servername
	private Map<String,List<String>> _hostPort   = null;
	private String                   _dbname     = null;
	private Map<String,String>       _options    = null;

	static
	{
//		_logger.setLevel(Level.DEBUG);
	}

	private AseUrlHelper()
	{
	}

	private AseUrlHelper(String originUrl, String serverName, Map<String,List<String>> hostPort, String dbname, Map<String,String> options)
	{
		_originUrl  = originUrl;
		_serverName = serverName;
		_hostPort   = hostPort;
		_dbname     = dbname;
		_options    = options;
	}

//	public AseUrlHelper(String url)
//	throws ParseException
//	{
//		parseUrl(url);
//	}

	/**
	 * If the host1:port2[,host2:port2] was found in the sql.ini/interfaces file, this is the name
	 */
	public String getServerName()
	{
		return _serverName;
	}

	/**
	 * Get all the host(s) used in the URL
	 * @return a comma separated list of hosts: host1[, host2[, host3]]
	 */
	public String getHosts()
	{
		return getHosts(", ");
	}
	/**
	 * Get all the host(s) used in the URL
	 * @param entrySep specify the string used as a delimiter for the hosts
	 * @return a <code>entrySep</code> separated list of hosts: host1[, host2[, host3]]
	 */
	public String getHosts(String entrySep)
	{
		return StringUtil.toCommaStrMultiMapKey(_hostPort, entrySep);
	}

	/**
	 * Get all the port(s) used in the URL
	 * @return a comma separated list of ports: port1[, port2[, port3]]
	 */
	public String getPorts()
	{
		return getPorts(", ");
	}
	/**
	 * Get all the port(s) used in the URL
	 * @param entrySep specify the string used as a delimiter for the ports
	 * @return a <code>entrySep</code> separated list of ports: port1[, port2[, port3]]
	 */
	public String getPorts(String entrySep)
	{
		return StringUtil.toCommaStrMultiMapVal(_hostPort, entrySep);
	}

	/**
	 * @return Number of host port entries in the Map
	 */
	public int getHostPortCount()
	{
		if (_hostPort == null)
			return 0;

		int count = 0;
		for (Iterator it = _hostPort.entrySet().iterator(); it.hasNext();)
		{
			Object val = it.next();
			if (val instanceof List)
				count += ((List)val).size();
			else
				count++;
		}
		return count;
	}

	/**
	 * Get the the Map wher all host=port entries, which are used in the URL
	 * @return a Map with host=port entries
	 */
	public Map<String,List<String>> getHostPortMap()
	{
		return _hostPort;
	}

	/**
	 * Get all the host(s):port(s) used in the URL
	 * @return a String Array : host1:port1, host1:port2, host3:port3
	 */
	public String[] getHostPortArr()
	{
		return StringUtil.commaStrToArray(getHostPortStr());
	}

	/**
	 * Get all the host(s):port(s) used in the URL
	 * @return a comma separated list of host:port: host1:port1[, host1:port2[, host3:port3]]
	 */
	public String getHostPortStr()
	{
		return getHostPortStr(", ");
	}
	/**
	 * Get all the host(s):port(s) used in the URL
	 * @param entrySep specify the string used as a delimiter for the host:port
	 * @return a <code>entrySep</code> separated list of host:port: host1:port1[, host1:port2[, host3:port3]]
	 */
	public String getHostPortStr(String entrySep)
	{
		return StringUtil.toCommaStrMultiMap(_hostPort, ":", entrySep);
	}

	/**
	 * If any database was specified in the URL
	 * @return null or the database name
	 */
	public String getDbname()
	{
		return _dbname;
	}

	/**
	 * Get all the options used in the URL
	 * @return a comma separated list of options: OPT1=val[, OPT2=val[, OPT3=val]]
	 */
	public String getOptions()
	{
		return getOptions(", ");
	}
	/**
	 * Get all the options used in the URL
	 * @param entrySep specify the string used as a delimiter for the options
	 * @return a <code>entrySep</code> separated list of options: OPT1=val[, OPT2=val[, OPT3=val]]
	 */
	public String getOptions(String entrySep)
	{
		return StringUtil.toCommaStr(_options, "=", entrySep);
	}

	/**
	 * Get the original URL when you called parseUrl
	 * @return the original URL string
	 */
	public String getUrlOrigin()
	{
		return _originUrl;
	}

	/**
	 * Build a URL based on the information from the parsed URL string
	 * @return URL string
	 */
	public String getUrl()
	{
		return AseUrlHelper.buildUrlString(_hostPort, _dbname, _options);

//		StringBuilder url = new StringBuilder();
//
//		// add the start
//		url.append(_urlStart);
//
//		// add host1:port1[,host2:port2]
//		if (_hostPort != null && _hostPort.size() > 0)
//		{
//			url.append(StringUtil.toCommaStrMultiMap(_hostPort, ":", ","));
//		}
//
//		// add dbname
//		if (_dbname != null && ! _dbname.trim().equals(""))
//			url.append("/").append(_dbname);
//
//		// add options
//		if (_options != null && _options.size() > 0)
//		{
//			url.append("?");
//			url.append(StringUtil.toCommaStr(_options, "=", "&"));
//		}
//
//		return url.toString();
	}

	/**
	 * If the URL contains several host:port entries, split those apart and return a List 
	 * where each list entries is a full URL, with only one host:port in each entry 
	 * <p> 
	 * For the moment it seems that jConnect <b>still</b> can't handle multiple host:port entries
	 * so this would be a workaround to try each list entry individually in a connection loop
	 * to simulate multiple "query" rows in the sql.ini/interfaces file.
	 * <p>
	 * So if we had the original URL: <br>
	 * <code>jdbc:sybase:Tds:host1:1111,host2:2222/pubs2?APPLICATIONNAME=testapp</code>
	 * <p>
	 * we will return a List of two entries:<br>
	 * <code>jdbc:sybase:Tds:host1:1111/pubs2?APPLICATIONNAME=testapp</code>
	 * <code>jdbc:sybase:Tds:host2:2222/pubs2?APPLICATIONNAME=testapp</code>
	 * 
	 * @return See above text
	 */
	public List<String> getUrlList()
	{
		LinkedList<String> urlList  = new LinkedList<String>();
		
		// LOOP THE HOSTS and create one entry for each of the Host/Port entries
		// host1:port1[,host2:port2]
		if (_hostPort != null && _hostPort.size() > 0)
		{
			for (Iterator it = _hostPort.keySet().iterator(); it.hasNext();)
			{
				String host = (String) it.next();
				Object port = _hostPort.get(host);

				if (port instanceof List)
				{
					List list = (List) port;
					for (Iterator listIt = list.iterator(); listIt.hasNext();)
					{
						port = listIt.next();
						urlList.add( composeUrlEntry(host, port) );
					}
				}
				else
					urlList.add( composeUrlEntry(host, port) );
			}
		}
		return urlList;
	}
	public String composeUrlEntry(String host, Object port)
	{
		StringBuilder urlEntry = new StringBuilder();

		// add the start
		urlEntry.append(_urlStart);

		// add host1:port1
		urlEntry.append(host).append(":").append(port);

		// add dbname
		if (_dbname != null && ! _dbname.trim().equals(""))
			urlEntry.append("/").append(_dbname);

		// add options
		if (_options != null && _options.size() > 0)
		{
			urlEntry.append("?");
			urlEntry.append(StringUtil.toCommaStr(_options, "=", "&"));
		}

		if (_logger.isDebugEnabled())
			_logger.debug("composeUrlEntry: returns '"+urlEntry.toString()+"'.");

		return urlEntry.toString();
	}

	/**
	 * Do we have multiple host:port in the url?
	 * @return true if we have host1:port2,host2:port2
	 */
	public boolean hasMultiHostPort()
	{
		return getHostPortCount() > 1;
	}

	@Override
	public String toString()
	{
		return super.toString();
	}


	/**
	 * Parse a URL string into individual parts
	 * <p>
	 * This means that you can extract what host:port(s) the URL contains<br>
	 * Or get the options in the URL
	 * 
	 * @param urlStr the URL string you want to parse
	 * @return a AseUrlHelper object
	 * 
	 * @throws ParseException if something bad happened
	 */
	public static AseUrlHelper parseUrl(String urlStr)
	throws ParseException
	{
//		URL="jdbc:sybase:Tds:server1:port1,server2:port2,...,serverN:portN/mydb?&PACKETSIZE=1024&DYNAMIC_PREPARE=true&REQUEST_HA_SESSION=true"
//		URL="jdbc:sybase:Tds:server1:port1[,server2:port2,...,serverN:portN][/mydb]?&PACKETSIZE=1024&DYNAMIC_PREPARE=true&REQUEST_HA_SESSION=true"

		if (urlStr == null)
		{
			new Exception("DUMMY PRINT EXCEPTION to grab from where it was called").printStackTrace();
		}
		
		String urlStart = "jdbc:sybase:Tds:";
		
		if ( ! urlStr.startsWith(urlStart) )
			throw new ParseException("The URL has to start with '"+urlStart+"'. The input looks like '"+urlStr+"'.", 0);
			//throw new IllegalArgumentException("The URL has to start with '"+urlStart+"'.");

		Map<String,List<String>> hostPort = null;
		String                   dbname   = null;
		Map<String,String>       options  = null;

		// Separate Option from URL
		// if we found options, parse them into a Property object
		String baseStr    = null;
		String optionsStr = null;
		if (urlStr.indexOf("?") >= 0)
		{
			// strip out the option
			baseStr = urlStr.substring(0, urlStr.indexOf("?"));
			// copy out the option
			optionsStr = urlStr.substring(urlStr.indexOf("?")+1);

			// parse all the options
			options = StringUtil.parseCommaStrToMap(optionsStr, "=", "&");
		}
		else
			baseStr = urlStr;
		
		// get dbname + strip it off from baseStr
		if (baseStr.indexOf("/") >= 0)
		{
			// copy dbname
			dbname = baseStr.substring(baseStr.indexOf("/")+1);
			// strip of the dbname
			baseStr = baseStr.substring(0, baseStr.indexOf("/"));
		}

		// strip of the 'urlStart'
		baseStr = baseStr.substring(urlStart.length());

		// check if last part is a number, which it should be
//		boolean lastPartIsNumber = true;
//		String[] sa = baseStr.split(":");
//		try { Integer.parseInt(sa[sa.length-1]); }
//		catch (NumberFormatException e) 
//		{
//			// FIXME: read char by char until we see a non number... maybe... 
//			lastPartIsNumber = false;
//		}

		// now the things left should look like host1:port1,host2:port,hostN:portN
		hostPort = StringUtil.parseCommaStrToMultiMap(baseStr, ":", ",");

		// get the servername from sql.ini/interfaces file
		String serverName = null;
		if (hostPort != null && !hostPort.isEmpty())
			serverName = AseConnectionFactory.getIServerName(hostPort);

		if (_logger.isDebugEnabled())
		{
			_logger.debug("----------------------------------------------");
			_logger.debug("originUrl  = '" + urlStr     + "'.");
			_logger.debug("serverName = '" + serverName + "'.");
			_logger.debug("hostPort   = '" + hostPort   + "'.");
			_logger.debug("dbname     = '" + dbname     + "'.");
			_logger.debug("options    = '" + options    + "'.");
		}

		return new AseUrlHelper(urlStr, serverName, hostPort, dbname, options);
	}


	/**
	 * Build a jConnect URL String
	 * 
	 * @param hostPort a Map of host,port values
	 * @param dbname dbname to use
	 * @param options options to use
	 * @return jdbc:sybase:Tds:host1:port1[,host2:port2,hostN:portN][/dbname][?OPT=val&OPT2=val&OPT3=val]
	 */
	public static String buildUrlString(Map<String,List<String>> hostPort, String dbname, Map<String,String> options)
	{
		if (_logger.isDebugEnabled())
			_logger.debug("AseUrlHelper.getUrlString(hostPort='"+hostPort+"', dbname='"+dbname+"', options='"+options+"').");

		if (hostPort == null)
			throw new IllegalArgumentException("hostPort can't be null");
		if (hostPort.isEmpty())
			throw new IllegalArgumentException("hostPort can't be empty");

		// Grab the template: "jdbc:sybase:Tds:HOST:PORT"
		String urlTemplate = AseConnectionFactory.getUrlTemplate();

		if (urlTemplate == null)
			throw new IllegalArgumentException("The url template grabbed from AseConnectionFactory can't be null.");
		if (urlTemplate.indexOf("HOST:PORT") == -1)
			throw new IllegalArgumentException("The url template grabbed from AseConnectionFactory must contain 'HOST:PORT'. urlTemplate='"+urlTemplate+"'");
		
		String url = urlTemplate;
//		url = url.replaceAll("HOST", _host_txt.getText().trim());
//		url = url.replaceAll("PORT", _port_txt.getText().trim());

		// build a host1:port1[,host2:port2,hostN:portN] string and replace the HOST:PORT from the template
		String hostPortStr = StringUtil.toCommaStrMultiMap(hostPort, ":", ",");
		url = url.replaceAll("HOST:PORT", hostPortStr);


		// add database
		if (dbname != null && dbname.trim().length() > 0)
			url += "/" + dbname;
		
		// Add options
		if (options != null && options.size() > 0)
			url += "?" + StringUtil.toCommaStr(options, "=", "&");

		if (_logger.isDebugEnabled())
			_logger.debug("AseUrlHelper.returns='"+url+"'.");
		return url;
	}

	/**
	 * Get URL Options
	 * @return
	 */
	public Map<String, String> getUrlOptionsMap()
	{
		return _options;
	}

	/**
	 * Set URL Options
	 * @return
	 */
	public void setUrlOptionsMap(Map<String, String> urlOptionsMap)
	{
		_options = urlOptionsMap;
	}

	/**
	 * Get first host name (if there are several)
	 */
	public String getFirstHost()
	{
		if (_hostPort == null)
			return null;

		for (String key : _hostPort.keySet())
			return key;

		return null;
	}

	/**
	 * Get first port number (if there are several)
	 */
	public int getFirstPort()
	{
		if (_hostPort == null)
			return -1;

		for (List<String> list : _hostPort.values())
			return Integer.parseInt( list.get(0) );

		return -1;
	}
}
