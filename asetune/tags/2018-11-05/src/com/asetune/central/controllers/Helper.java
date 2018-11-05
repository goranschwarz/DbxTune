package com.asetune.central.controllers;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

import javax.servlet.http.HttpServletRequest;

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
		
		if (value == null)
			return defaultValue;
		
		if (value.equals("undefined"))
			return null;
		
		return value;
	}
	
	
}