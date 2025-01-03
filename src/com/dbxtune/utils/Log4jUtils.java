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
package com.dbxtune.utils;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.DefaultRolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.SizeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.core.layout.PatternLayout;

public class Log4jUtils
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public static class FilterOnClassNameAndMessage 
	extends AbstractFilter 
	{
		private String _fullClassName;
		private String _message;

		public FilterOnClassNameAndMessage(Class<?> clazz, String message)
		{
			_fullClassName = clazz.getName();
			_message = message;
		}

		public FilterOnClassNameAndMessage(String fullClassName, String message)
		{
			_fullClassName = fullClassName;
			_message = message;
		}

		@Override
		public Result filter(LogEvent event) 
		{
			if (event.getLoggerName().equals(_fullClassName))
			{
				String msg = "" + event.getMessage();

				if (msg.indexOf(_message) != -1)
					return Result.ACCEPT;
			}
			return Result.DENY;
		}
	}	

	
	private static class FilterOnClassName 
	extends AbstractFilter 
	{
		private String _fullClassName;

		public FilterOnClassName(Class<?> clazz)
		{
			_fullClassName = clazz.getName();
		}

//		public FilterOnClassName(String fullClassName)
//		{
//			_fullClassName = fullClassName;
//		}

		@Override
		public Result filter(LogEvent event) 
		{
			if (event.getLoggerName().equals(_fullClassName))
			{
				return Result.ACCEPT;
			}
			return Result.DENY;
		}
	}	
	
	public static void addRollingFileAppenderWithFilter(String loggerName, File logFilePath, String logPattern, int maxFiles, int maxMb, Class<?> onlyOnClass, List<AbstractFilter> extraFilters, Level initialLogLevel) 
	{
		addRollingFileAppenderWithFilter(loggerName, logFilePath.getAbsolutePath(), logPattern, maxFiles, maxMb, onlyOnClass, extraFilters, initialLogLevel);
	}
	public static void addRollingFileAppenderWithFilter(String loggerName, String logFilePath, String logPattern, int maxFiles, int maxMb, Class<?> onlyOnClass, List<AbstractFilter> extraFilters, Level initialLogLevel) 
	{
		// Get the LoggerContext
		LoggerContext context = (LoggerContext) LogManager.getContext(false);

		// Get the configuration
		Configuration config = context.getConfiguration();

//		String logPattern = "%d{yyyy-MM-dd HH:mm:ss} %-5p %c{1}:%L - %m%n";
		if (logPattern == null)
			logPattern = "%d - %-5p - %m%n";

		String rolloverFilePattern = logFilePath + ".%i.%d{yyyy-MM-dd}.log";
		
		// Create a PatternLayout for the appender
		PatternLayout layout = PatternLayout.newBuilder()
				.withConfiguration(config)
				.withPattern(logPattern)
				.build();

		// Create a SizeBasedTriggeringPolicy
		SizeBasedTriggeringPolicy policy = SizeBasedTriggeringPolicy.createPolicy(maxMb + "MB");

		// Create a DefaultRolloverStrategy
		DefaultRolloverStrategy strategy = DefaultRolloverStrategy.newBuilder()
				.withMax(maxFiles + "") // Max # files
				.withConfig(config)
				.build();

		// Create a RegexFilter for specific classes
//		Filter filter = RegexFilter.createFilter(classRegex, null, true, Filter.Result.ACCEPT, Filter.Result.DENY);
		Filter filter = null;
		if (onlyOnClass != null)
		{
			filter = new FilterOnClassName(onlyOnClass);
		}

		_logger.info("Adding separate rolling log file for '" + loggerName + "' using file '" + logFilePath + "' with logPattern '" + logPattern + "', filter '" + onlyOnClass + "'. [maxMb=" + maxMb + ", maxFiles=" + maxFiles + ", rolloverFilePattern='" + rolloverFilePattern + "'].");

		// Create the RollingFileAppender
		RollingFileAppender appender = RollingFileAppender.newBuilder()
				.setName(loggerName)
				.setLayout(layout)
				.setFilter(filter)
				.setConfiguration(config)
				.withFileName(logFilePath)
				.withFilePattern(rolloverFilePattern)
				.withAppend(true)
				.withPolicy(policy)
				.withStrategy(strategy)
				.build();

		// Add any additional filters
		if (extraFilters != null)
		{
			for (Filter extraFilter : extraFilters)
			{
				appender.addFilter(extraFilter);
			}
		}
		
		// Start the appender
		appender.start();

		// Add the appender to the configuration
		config.addAppender(appender);

		// Create an AppenderRef
//		AppenderRef ref = AppenderRef.createAppenderRef(loggerName, null, null);

		// Add the AppenderRef to the RootLoggerConfig
		LoggerConfig loggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
		loggerConfig.addAppender(appender, initialLogLevel, null);

		// Update the LoggerContext
		context.updateLoggers();
	}

}
