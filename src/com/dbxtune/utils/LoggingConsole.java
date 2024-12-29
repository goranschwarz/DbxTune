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

import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;

public class LoggingConsole
{

	/** Initialize a console logger with INFO level */
	public static void init()
	{
		init(false, false);
	}

	/** Initialize a console logger with DEBUG or TRACE level */
	public static void init(boolean debug, boolean trace)
	{
		Properties log4jProps = new Properties();
		log4jProps.setProperty("log4j.rootLogger", "INFO, console");
		
		if (debug)
			log4jProps.setProperty("log4j.rootLogger", "DEBUG, console");
		
		if (trace)
			log4jProps.setProperty("log4j.rootLogger", "TRACE, console");
		
		log4jProps.setProperty("log4j.appender.console",                          "org.apache.log4j.ConsoleAppender");
		log4jProps.setProperty("log4j.appender.console.layout",                   "org.apache.log4j.PatternLayout");
		log4jProps.setProperty("log4j.appender.console.layout.ConversionPattern", "%d - %-5p - %-30t - %-30c{1} - %m%n");

		PropertyConfigurator.configure(log4jProps);
	}

}
