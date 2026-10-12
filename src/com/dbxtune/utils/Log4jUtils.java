/*******************************************************************************
 * Copyright (C) 2010-2027 Goran Schwarz
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

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
import org.apache.logging.log4j.core.config.Configurator;
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

//TODO; // Look at this again... it saves a lot of log files (not keeping only 3 time stamped)... So either go back to xxx.1.log, xxx.2.log, xxx.3.log ... or change it "somehow"...
//         There was no simple solution for this, so I reverted back to: X number of log files... and commented out the 'timePolicy' and the 'combinedPolicy' hopefully it can be resolved later.
//		String rolloverFilePattern = logFilePath + ".%d{yyyy-MM-dd}.%i.log";
		String rolloverFilePattern = logFilePath + ".%i.log";

		// Create a PatternLayout for the appender
		PatternLayout layout = PatternLayout.newBuilder()
				.withConfiguration(config)
				.withPattern(logPattern)
				.build();

		// Create a SizeBasedTriggeringPolicy & TimeBasedTriggeringPolicy
		SizeBasedTriggeringPolicy sizePolicy = SizeBasedTriggeringPolicy.createPolicy(maxMb + "MB");
//		TimeBasedTriggeringPolicy timePolicy = TimeBasedTriggeringPolicy.newBuilder()
//				.withInterval(1)
//				.withModulate(true)
//				.build();

		// Combine the policies
//		CompositeTriggeringPolicy combinedPolicy = CompositeTriggeringPolicy.createPolicy(sizePolicy, timePolicy);

		
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
//				.withPolicy(combinedPolicy)
				.withPolicy(sizePolicy)
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


	//--------------------------------------------------------------------------
	// Log Levels: view/change at runtime
	// Used by the "log-level" servlets in the Collectors and in DbxCentral (Admin -> Log Levels)
	//--------------------------------------------------------------------------

	/** The name we use for the root logger (in Log4j it's an empty string) */
	public static final String ROOT_LOGGER_NAME = "ROOT";

	/** The root level in 'log4j2.properties', which is what we go back to when the root logger is set to "default" */
	public static final Level DEFAULT_ROOT_LEVEL = Level.INFO;

	/** A saved log level looks like: <code>log4j.logger.com.dbxtune.cm.ase.CmSummary=DEBUG</code> */
	public static final String PROPKEY_PREFIX_savedLogLevel = "log4j.logger.";

	/** Where log levels are saved, null = they can't be saved */
	private static com.dbxtune.utils.Configuration _savedLogLevelConf = null;

	/**
	 * Called at startup: Remember where log levels are saved, and set the levels that already are saved there.
	 *
	 * @param saveConf   The Configuration to save log levels in (null = log levels can't be saved)
	 */
	public static synchronized void initSavedLogLevels(com.dbxtune.utils.Configuration saveConf)
	{
		_savedLogLevelConf = saveConf;
		if (saveConf == null)
			return;

		List<String> keys = saveConf.getKeys(PROPKEY_PREFIX_savedLogLevel);
		if ( ! keys.isEmpty() )
			_logger.info("Setting " + keys.size() + " saved log level(s), found in the file '" + saveConf.getFilename() + "'.");

		// ROOT first, then in name order (a package before it's classes)
		// The order matters: a level that is the same as the one inherited from the parent is skipped, see setLoggerLevel()
		if (keys.remove(PROPKEY_PREFIX_savedLogLevel + ROOT_LOGGER_NAME))
			keys.add(0, PROPKEY_PREFIX_savedLogLevel + ROOT_LOGGER_NAME);

		for (String key : keys)
		{
			String loggerName = key.substring(PROPKEY_PREFIX_savedLogLevel.length());
			String levelStr   = saveConf.getProperty(key, "");
			if (StringUtil.isNullOrBlank(levelStr))
				continue;

			try
			{
				setLoggerLevel(loggerName, levelStr, false);
			}
			catch (IllegalArgumentException ex)
			{
				_logger.warn("Skipping the saved log level '" + key + "=" + levelStr + "' in file '" + saveConf.getFilename() + "'. " + ex.getMessage());
			}
		}
	}

	/** @return The file where log levels are saved, null if they can't be saved */
	public static String getSavedLogLevelFilename()
	{
		return _savedLogLevelConf == null ? null : _savedLogLevelConf.getFilename();
	}

	/**
	 * Translate a level name into a Log4j Level
	 *
	 * @param levelStr   OFF, FATAL, ERROR, WARN, INFO, DEBUG, TRACE, ALL ... or blank/DEFAULT, which means: no own level (inherit from the parent)
	 * @return The Level, or null for "default"
	 * @throws IllegalArgumentException if it's not a known level
	 */
	public static Level toLevel(String levelStr)
	{
		if (StringUtil.isNullOrBlank(levelStr) || "DEFAULT".equalsIgnoreCase(levelStr.trim()))
			return null;

		Level level = Level.getLevel(levelStr.trim().toUpperCase());
		if (level == null)
			throw new IllegalArgumentException("Unknown log level '" + levelStr + "'. Known levels: OFF, FATAL, ERROR, WARN, INFO, DEBUG, TRACE, ALL or DEFAULT.");

		return level;
	}

	/**
	 * Get all loggers and what level they have.
	 * <ul>
	 *   <li>name    - logger name (normally the class name) or a package name that has it's own level, the root logger is named 'ROOT'</li>
	 *   <li>level   - the level used right now</li>
	 *   <li>nonDefault - true if the logger/package has it's own level: set with setLoggerLevel(), a saved level, or set by the program itself (like 'org.jfree').
	 *                 false = the level is inherited from the parent package, and in the end from ROOT.
	 *                 For ROOT: true if it's not INFO</li>
	 *   <li>saved   - the level that is saved (and will be used after a restart), empty if it's not saved</li>
	 * </ul>
	 * @return one entry for each logger, sorted on name, with 'ROOT' first
	 */
	public static synchronized List<Map<String, Object>> getLoggerLevels()
	{
		LoggerContext context = (LoggerContext) LogManager.getContext(false);
		Configuration config  = context.getConfiguration();

		// Loggers that has been created (classes that has been loaded)
		Set<String> names = new TreeSet<>();
		for (org.apache.logging.log4j.core.Logger logger : context.getLoggers())
			names.add(logger.getName());

		// Loggers/packages that has it's own level (a package do not have a "created" logger)
		names.addAll(config.getLoggers().keySet());

		// Saved levels (they might have been set back "in-memory" after they were saved)
		if (_savedLogLevelConf != null)
		{
			for (String key : _savedLogLevelConf.getKeys(PROPKEY_PREFIX_savedLogLevel))
				names.add(key.substring(PROPKEY_PREFIX_savedLogLevel.length()));
		}

		// The root logger is added as the first row
		names.remove("");
		names.remove(ROOT_LOGGER_NAME);

		List<Map<String, Object>> list = new ArrayList<>();

		Level rootLevel = config.getRootLogger().getLevel();
		list.add( loggerLevelEntry(ROOT_LOGGER_NAME, rootLevel, ! DEFAULT_ROOT_LEVEL.equals(rootLevel)) );

		for (String name : names)
			list.add( loggerLevelEntry(name, config.getLoggerConfig(name).getLevel(), config.getLoggers().containsKey(name)) );

		return list;
	}

	/** @return The level used right now by a logger (class name), a package or the root logger ('ROOT') */
	public static synchronized String getLoggerLevel(String loggerName)
	{
		Configuration config = ((LoggerContext) LogManager.getContext(false)).getConfiguration();

		if (ROOT_LOGGER_NAME.equalsIgnoreCase(loggerName.trim()))
			return config.getRootLogger().getLevel().name();

		return config.getLoggerConfig(loggerName.trim()).getLevel().name();
	}

	private static Map<String, Object> loggerLevelEntry(String name, Level level, boolean nonDefault)
	{
		String saved = _savedLogLevelConf == null ? null : _savedLogLevelConf.getProperty(PROPKEY_PREFIX_savedLogLevel + name);

		Map<String, Object> entry = new LinkedHashMap<>();
		entry.put("name"   , name);
		entry.put("level"  , level.name());
		entry.put("nonDefault", nonDefault);
		entry.put("saved"  , saved == null ? "" : saved);
		return entry;
	}

	/**
	 * Set the log level for a logger (class name), a package or the root logger ('ROOT')
	 *
	 * @param loggerName   Class name, package name or 'ROOT'
	 * @param levelStr     OFF, FATAL, ERROR, WARN, INFO, DEBUG, TRACE, ALL ... or blank/DEFAULT to go back to the default (inherit from the parent).
	 *                     The level it already inherits from the parent is also the same as DEFAULT (it does not get it's own level)
	 * @param save         true = also save it, so it's used after a restart
	 * @return The level that was used before the change (the new level is found with {@link #getLoggerLevel(String)})
	 * @throws IllegalArgumentException if the level is unknown, or if we can't save
	 */
	public static synchronized String setLoggerLevel(String loggerName, String levelStr, boolean save)
	{
		if (StringUtil.isNullOrBlank(loggerName))
			throw new IllegalArgumentException("No logger name was given.");
		loggerName = loggerName.trim();

		Level level = toLevel(levelStr);

		if (save && _savedLogLevelConf == null)
			throw new IllegalArgumentException("Log levels can't be saved here (no save configuration), use in-memory only.");

		LoggerContext context = (LoggerContext) LogManager.getContext(false);
		Configuration config  = context.getConfiguration();

		boolean isRoot = ROOT_LOGGER_NAME.equalsIgnoreCase(loggerName);
		if (isRoot)
			loggerName = ROOT_LOGGER_NAME;

		Level oldLevel = isRoot ? config.getRootLogger().getLevel() : config.getLoggerConfig(loggerName).getLevel();

		if (isRoot)
		{
			// The same level as the root logger normally has, is the same as "default"
			if (DEFAULT_ROOT_LEVEL.equals(level))
				level = null;

			Configurator.setRootLevel(level == null ? DEFAULT_ROOT_LEVEL : level);
		}
		else
		{
			// What we get here is: it's own LoggerConfig (if it has it's own level), otherwise the closest parent
			LoggerConfig loggerConfig = config.getLoggerConfig(loggerName);
			boolean hasOwnLevel       = loggerConfig.getName().equals(loggerName);
			Level   inheritedLevel    = hasOwnLevel ? loggerConfig.getParent().getLevel() : loggerConfig.getLevel();

			// The same level as it inherits from the parent, is the same as "default"
			// So when a logger is set to DEBUG and then back to INFO, it's NOT a "nonDefault" logger any more
			if (inheritedLevel.equals(level))
				level = null;

			if (level != null)
			{
				Configurator.setLevel(loggerName, level);
			}
			else if (hasOwnLevel)
			{
				// Remove it's own level, so it's inherited from the parent again
				config.removeLogger(loggerName);
				context.updateLoggers();
			}
		}

		Level newLevel = isRoot ? config.getRootLogger().getLevel() : config.getLoggerConfig(loggerName).getLevel();

		if (save)
		{
			String key = PROPKEY_PREFIX_savedLogLevel + loggerName;
			if (level == null)
				_savedLogLevelConf.remove(key);
			else
				_savedLogLevelConf.setProperty(key, level.name());

			_savedLogLevelConf.save();
		}

		_logger.info("Log level for '" + loggerName + "' was changed from " + oldLevel + " to " + newLevel
				+ (level == null ? " (default)" : "")
				+ (save ? ", and saved in '" + _savedLogLevelConf.getFilename() + "'." : "."));

		return oldLevel.name();
	}
}
