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
package com.dbxtune.test;

import java.io.StringWriter;
import java.util.Enumeration;
import java.util.Properties;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.app.event.EventCartridge;
import org.apache.velocity.app.event.InvalidReferenceEventHandler;
import org.apache.velocity.context.Context;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.util.introspection.Info;

import com.dbxtune.utils.StringUtil;

public class VelocityJson
{
	/**
	 * Take the AlarmEvent and fill in the template values...
	 * 
	 * @param template                    the Velocity template
	 * @param doTrim                      remove whitespaces newlines etc from start and end
	 * @param trMap                       A Map of Strings that needs transalations: "&" -> "&amp";
	 * 
	 * @return                            The resolved template
	 * @throws ParseErrorException        If we had parser exceptions
	 * @throws MethodInvocationException  If any exceptions where thrown when calling a method on a methodName
	 * @throws ResourceNotFoundException  Resource not found...
	 */
	public static String createMessageFromTemplate(String template, String json)
	throws ParseErrorException, MethodInvocationException, ResourceNotFoundException
	{
		Properties config = new Properties();
//		config.setProperty("eventhandler.invalidreference.exception", "true");

		VelocityEngine engine = new VelocityEngine(config);
		engine.init();

		// Set the LOG LEVEL to FATAL for the Velocity PARSER... This so that the Parser exceptions will not be written to the Errolog
		// NOTE: This is specific to Log4J
		Logger logLevel = LogManager.getLogger("org.apache.velocity.parser");
		if (logLevel != null)
			logLevel.setLevel(Level.FATAL);


		boolean debug = false;
		if (debug)
		{
			Enumeration<?> loggers = LogManager.getCurrentLoggers();
			while(loggers.hasMoreElements()) 
			{
				Logger logger = (Logger) loggers.nextElement();
				if (logger.getName().startsWith("org.apache.velocity"))
				{
					logger.setLevel(Level.DEBUG);
				}
			}
		}

		VelocityContext context = new VelocityContext();

		// Add access to: com.dbxtune.utils.StringUtil
//		context.put("StringUtil", StringUtil.class);
		
		// Add access to: com.dbxtune.Version
//		context.put("Version", Version.class);
		
		// Add access to: java.lang.System
		context.put("System", System.class);
		
		// Add access to: org.apache.commons.lang3.StringUtils
//		context.put("StringUtils", StringUtils.class);
		

		// Add TYPE to context
//		context.put("type", action);

		// Dbx Central URL
//		context.put("dbxCentralUrl", dbxCentralUrl);
		
//		if (alarmEvent != null)
//		{
//			// put (basic) AlarmEvent fields
//			context.put("alarmClass"                 , StringUtil.toStr( alarmEvent.getAlarmClass()                ,trMap ));
//			context.put("serviceType"                , StringUtil.toStr( alarmEvent.getServiceType()               ,trMap ));
//			context.put("serviceName"                , StringUtil.toStr( alarmEvent.getServiceName()               ,trMap ));
//			context.put("serviceInfo"                , StringUtil.toStr( alarmEvent.getServiceInfo()               ,trMap ));
//			context.put("extraInfo"                  , StringUtil.toStr( alarmEvent.getExtraInfo()                 ,trMap ));
//			context.put("category"                   , StringUtil.toStr( alarmEvent.getCategory()                  ,trMap ));
//			context.put("severity"                   , StringUtil.toStr( alarmEvent.getSeverity()                  ,trMap ));
//			context.put("state"                      , StringUtil.toStr( alarmEvent.getState()                     ,trMap ));
//			context.put("data"                       , StringUtil.toStr( alarmEvent.getData()                      ,trMap ));
//			context.put("description"                , StringUtil.toStr( alarmEvent.getDescription()               ,trMap ));
//
//			// And some extra/extended AlarmEvent fields
//			context.put("duration"                   , StringUtil.toStr( alarmEvent.getDuration()                  ,trMap ));
//			context.put("reRaiseCount"               , StringUtil.toStr( alarmEvent.getReRaiseCount()              ,trMap ));
//			context.put("crTimeStr"                  , StringUtil.toStr( alarmEvent.getCrTimeStr()                 ,trMap ));
//			context.put("reRaiseTimeStr"             , StringUtil.toStr( alarmEvent.getReRaiseTimeStr()            ,trMap ));
//			context.put("timeToLive"                 , StringUtil.toStr( alarmEvent.getTimeToLive()                ,trMap ));
//			context.put("alarmClassAbriviated"       , StringUtil.toStr( alarmEvent.getAlarmClassAbriviated()      ,trMap ));
//			context.put("reRaiseData"                , StringUtil.toStr( alarmEvent.getReRaiseData()               ,trMap ));
//			context.put("cancelTimeStr"              , StringUtil.toStr( alarmEvent.getCancelTimeStr()             ,trMap ));
//			context.put("crAgeInMs"                  , StringUtil.toStr( alarmEvent.getCrAgeInMs()                 ,trMap ));
//			context.put("isActive"                   , StringUtil.toStr( alarmEvent.isActive()                     ,trMap ));
//			context.put("activeAlarmCount"           , StringUtil.toStr( alarmEvent.getActiveAlarmCount()          ,trMap ));
//		}
		
//		if (activeAlarmList != null)
//		{
//			context.put("activeAlarmList" , activeAlarmList);
//		}


		InvalidReferenceEventHandler invalidReferenceEventHandler = new InvalidReferenceEventHandler()
		{
			@Override
			public Object invalidGetMethod(Context context, String reference, Object object, String property, Info info)
			{
				System.out.println("DEBUG: invalid-Get-Method(context, reference='"+reference+"', object='"+object+"', info='"+info+"')");
				reportInvalidReference(reference, null, info);
				return null;
			}

			@Override
			public boolean invalidSetMethod(Context context, String leftreference, String rightreference, Info info)
			{
				System.out.println("DEBUG: invalid-Set-Method(context, leftreference='"+leftreference+"', rightreference='"+rightreference+"', info='"+info+"')");
				reportInvalidReference(leftreference, null, info);
				return false;
			}

			@Override
			public Object invalidMethod(Context context, String reference, Object object, String method, Info info)
			{
				System.out.println("DEBUG: invalid-Method(context, reference='"+reference+"', object='"+object+"', method='"+method+"', info='"+info+"')");
				if (reference == null)
					reportInvalidReference(object.getClass().getName() + "." + method, method, info);
				else
					reportInvalidReference(reference, method, info);
				return null;
			}

			private void reportInvalidReference(String reference, String method, Info info)
			{
				String lineStr   = "[line "+info.getLine()+", column "+info.getColumn()+"]";
				String methodStr = StringUtil.isNullOrBlank(method) ? "" : ", method='"+method+"'";
				throw new ParseErrorException("Reference '"+reference+"'"+methodStr+" do not exists. at "+lineStr, info);
			}
		};
		
		EventCartridge ec = new EventCartridge();
		ec.addEventHandler(invalidReferenceEventHandler);
		ec.attachToContext(context);

		// Here is where the substitution happens
		StringWriter writer = new StringWriter();
		
		Velocity.evaluate(context, writer, "AlarmTemplateWriter", template);

		String output = writer.toString();
		return output;
	}

	public static void main(String[] args)
	{
		
	}
}
