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
package com.asetune.central.controllers;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.asetune.utils.StringUtil;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class Helper
{
//	public static final SimpleDateFormat ISO8601_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
	public static ObjectMapper createObjectMapper()
	{
		// SimpleDateFormat is NOT thread safe, so create one for each call to createObjectMapper()
		final SimpleDateFormat iso8601_sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
		final ObjectMapper om = new ObjectMapper();
		
		SimpleModule module = new SimpleModule();
		module.addSerializer(Timestamp.class, new JsonSerializer<Timestamp>()
		{
			@Override
			public void serialize(Timestamp value, JsonGenerator gen, SerializerProvider serializers) throws IOException
			{
				gen.writeString(iso8601_sdf.format(value));
			}
		});
		om.registerModule(module);

//		om.setDateFormat(new ISO8601DateFormat());
		
		om.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
//		om.configure(SerializationFeature.WRITE_DATES_WITH_ZONE_ID, true);
//		om.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, false);
		
		om.enable(SerializationFeature.INDENT_OUTPUT);
		
		return om;
	}

	public static String getParameter(HttpServletRequest req, String parameterName)
	{
		return getParameter(req, parameterName, null);
	}
	public static String getParameter(HttpServletRequest req, String parameterName, String defaultValue)
	{
		String value = req.getParameter(parameterName);
		
		if (StringUtil.isNullOrBlank(value))
			return defaultValue;
		
		if (value.equals("undefined"))
			return defaultValue;
		
		return value;
	}
	
	public static String getParameter(HttpServletRequest req, String[] parameterNameArr)
	{
		return getParameter(req, parameterNameArr, null);
	}
	public static String getParameter(HttpServletRequest req, String[] parameterNameArr, String defaultValue)
	{
		String value = null;
		for (String param : parameterNameArr)
		{
			value = req.getParameter(param);
			if (value != null)
				break;
		}

		if (StringUtil.isNullOrBlank(value))
			return defaultValue;
		
		if (value.equals("undefined"))
			return defaultValue;
		
		return value;
	}

	/** 
	 * Check for unknown input parameters<br>
	 * If any parameter is <i>unknown</i>, then: 
	 * <pre>
	 * resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Paramater 'xxx' is an unknown parameter. Known Parameters: 'aaa', 'bbb', 'ccc'");
	 * </pre>
	 * and return true
	 * 
	 * @param req
	 * @param knownParams a list of known parameters
	 * @return true if any is unknown
	 * @throws IOException 
	 */
	public static boolean hasUnKnownParameters(HttpServletRequest req, HttpServletResponse resp, String... knownParams) 
	throws IOException
	{
		// Check known input parameters
		for (String p : req.getParameterMap().keySet() )
		{
			if ( ! StringUtil.equalsAny(p, knownParams) )
			{
				resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Paramater '"+p+"' is an unknown parameter. Known Parameters: "+ StringUtil.toCommaStrQuoted("'", knownParams));
				return true;
			}
		}
		return false;
	}
	
}
