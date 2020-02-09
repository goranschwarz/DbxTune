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
package com.asetune.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class HtmlQueryString
{
	private String          _url	 = null;
	private StringBuffer    _query	 = new StringBuffer();
	private int             _counter = 0;

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
