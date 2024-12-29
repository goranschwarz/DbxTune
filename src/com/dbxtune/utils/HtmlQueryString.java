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
package com.dbxtune.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

public class HtmlQueryString
{
	private String          _url	 = null;
	private StringBuffer    _query	 = new StringBuffer();
	private int             _counter = 0;

	public HtmlQueryString()
	{
	}

	public HtmlQueryString(String url)
	{
		setUrl(url);
	}

	public HtmlQueryString(String name, String value)
	{
		encode(name, value);
	}

//	public int entryCount()
//	{
//		return _entries;
//	}
//
//	public int length()
//	{
//		return _query.length();
//	}

//	/**
//	 * Remove parameter from URL or Query String
//	 * @param url
//	 * @param remove
//	 * @return
//	 */
//	public static String removeParameterFromURL(final String url, final String remove) 
//	{
//		final String[] urlArr = url.split("\\?");
//		final String params = Arrays.asList(urlArr[1].split("&")).stream()
//				.filter(item -> !item.split("=")[0].equalsIgnoreCase(remove)).collect(Collectors.joining("&"));
//		return String.join("?", urlArr[0], params);
//	}
	/**
	 * Remove parameter from Query String
	 * @param url
	 * @param remove
	 * @return
	 */
	public static String removeParameter(final String queryString, final String... removeKey) 
	{
		Map<String, String> map = parseQueryString(queryString);

		for (String key : removeKey)
		{
			map.remove(key);
		}
		
		HtmlQueryString qs = new HtmlQueryString();
		for (Entry<String, String> entry : map.entrySet())
		{
			qs.add(entry.getKey(), entry.getValue());
		}

		return qs.getQuery();
	}

    /**
	 * Parse a querystring into a map of key/value pairs.
	 *
	 * @param queryString the string to parse (without the '?')
	 * @return key/value pairs mapping to the items in the querystring
	 */
	public static Map<String, String> parseQueryString(String queryString) 
	{
		Map<String, String> map = new LinkedHashMap<>();
		if ((queryString == null) || (queryString.equals(""))) 
		{
			return map;
		}
		String[] params = queryString.split("&");
		for (String param : params) 
		{
			try 
			{
				String[] keyValuePair = param.split("=", 2);
				String name = URLDecoder.decode(keyValuePair[0], "UTF-8");
				if (name == "") 
					continue;

				String value = keyValuePair.length > 1 ? URLDecoder.decode(keyValuePair[1], "UTF-8") : "";
				map.put(name, value);
			} 
			catch (UnsupportedEncodingException e) 
			{
				// ignore this parameter if it can't be decoded
			}
		}
		return map;
	}
	
	public synchronized void add(String name, String value)
	{
		if (_query.length() > 0)
			_query.append('&');
		encode(name, value);
	}

	public synchronized void add(String name, int value)
	{
		if (_query.length() > 0)
			_query.append('&');
		encode(name, Integer.toString(value));
	}

	public synchronized void add(String name, long value)
	{
		if (_query.length() > 0)
			_query.append('&');
		encode(name, Long.toString(value));
	}

	private synchronized void encode(String name, String value)
	{
		if (value == null)
		value = "";

		// replace all '\', with '/', which makes some fields more readable.
		value = value.replace('\\', '/');

//		// The below translates ' ' -->> '+' -->> '%20'
//		try
//		{
//			_query.append(URLEncoder.encode(name, StandardCharsets.UTF_8.toString()).replace("+", "%20"));
//			_query.append('=');
//			_query.append(URLEncoder.encode(value, StandardCharsets.UTF_8.toString()).replace("+", "%20"));
//		}
//		catch (UnsupportedEncodingException ex)
//		{
//			throw new RuntimeException("Broken VM does not support UTF-8");
//		}

		// The below translates ' ' -->> '+'
//		try
//		{
//			URLCodec urlCodec = new URLCodec("UTF-8");
//			
//			_query.append(urlCodec.encode(name));
//			_query.append('=');
//			_query.append(urlCodec.encode(value));
//		}
//		catch (EncoderException ex)
//		{
//			throw new RuntimeException("Problems encoding URL parameters as UTF-8", ex);
//		}
		
		// The below translates ' ' -->> '+'
		try
		{
			_query.append(URLEncoder.encode(name, "UTF-8"));
			_query.append('=');
			_query.append(URLEncoder.encode(value, "UTF-8"));
		}
		catch (UnsupportedEncodingException ex)
		{
			throw new RuntimeException("Broken VM does not support UTF-8");
		}
	}

	/** This is just a number that can be used for "anything" */
	public int getCounter()
	{
		return _counter;
	}
	/** This is just a number that can be used for "anything" */
	public void setCounter(int counter)
	{
		_counter = counter;
	}

	public String getUrl()
	{
		return _url;
	}
	public void setUrl(String url)
	{
		_url = url;
	}

	public String getQuery()
	{
		return _query.toString();
	}

	@Override
	public String toString()
	{
		if (StringUtil.hasValue(getUrl()))
			return getUrl() + "?" + getQuery();

		return getQuery();
	}
}
