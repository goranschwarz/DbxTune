/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
 *
 * This file is part of DbxTune
 * DbxTune is a family of sub-products *Tune, hence combindead PRODUCT://://name://://
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
package com.dbxtune.service;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.DbxTune;
import com.dbxtune.central.DbxTuneCentral;
import com.dbxtune.utils.ShutdownHandler;

/**
 * Windows Service wrapper for DbxTune, designed for use with Apache Procrun (prunsrv.exe).
 * <p>
 * Procrun calls the static {@link #start(String[])} and {@link #stop(String[])} methods
 * to manage the service lifecycle. In {@code jvm} mode, Procrun expects {@code start()} to
 * block until the service should stop, which matches the existing DbxTune shutdown pattern
 * where {@code main()} blocks on {@link ShutdownHandler#waitforShutdown()}.
 * <p>
 * <b>Usage with Procrun:</b>
 * <pre>
 *   --StartMode=jvm
 *   --StartClass=com.dbxtune.service.WindowsServiceWrapper
 *   --StartMethod=start
 *   --StartParams=toolName;arg1;arg2;...
 *   --StopMode=jvm
 *   --StopClass=com.dbxtune.service.WindowsServiceWrapper
 *   --StopMethod=stop
 * </pre>
 * <p>
 * The first element of the start params must be the tool name (e.g., "ase", "sqlserver",
 * "postgres", "central"). Remaining params are passed through to the tool's {@code main()} method.
 */
public class WindowsServiceWrapper
{
	private static final Logger _logger = LogManager.getLogger(WindowsServiceWrapper.class);

	/** Maps tool name (lowercase) to the main class name used by DbxTune's instance factory. */
	private static final Map<String, String> TOOL_TO_CLASSNAME = new LinkedHashMap<>();
	static
	{
		TOOL_TO_CLASSNAME.put("ase",       "asetune");
		TOOL_TO_CLASSNAME.put("iq",        "iqtune");
		TOOL_TO_CLASSNAME.put("rs",        "rstune");
		TOOL_TO_CLASSNAME.put("rax",       "raxtune");
		TOOL_TO_CLASSNAME.put("hana",      "hanatune");
		TOOL_TO_CLASSNAME.put("sqlserver", "sqlservertune");
		TOOL_TO_CLASSNAME.put("oracle",    "oracletune");
		TOOL_TO_CLASSNAME.put("postgres",  "postgrestune");
		TOOL_TO_CLASSNAME.put("mysql",     "mysqltune");
		TOOL_TO_CLASSNAME.put("db2",       "db2tune");
		TOOL_TO_CLASSNAME.put("central",   "central");
	}

	/**
	 * Called by Apache Procrun to start the service.
	 * <p>
	 * This method blocks until {@link #stop(String[])} is called (via ShutdownHandler),
	 * which is the expected behavior for Procrun's {@code jvm} start mode.
	 *
	 * @param args First element is the tool name (e.g., "ase", "sqlserver", "central").
	 *             Remaining elements are passed as arguments to the tool's main() method.
	 */
	public static void start(String[] args)
	{
		if (args == null || args.length == 0)
		{
			_logger.error("WindowsServiceWrapper.start(): No arguments provided. First argument must be the tool name (e.g., ase, sqlserver, postgres, central).");
			_logger.error("Available tool names: " + TOOL_TO_CLASSNAME.keySet());
			return;
		}

		String toolName = args[0].toLowerCase().trim();
		String[] toolArgs = Arrays.copyOfRange(args, 1, args.length);

		_logger.info("WindowsServiceWrapper.start(): toolName='" + toolName + "', toolArgs=" + Arrays.toString(toolArgs));

		String mainClassName = TOOL_TO_CLASSNAME.get(toolName);
		if (mainClassName == null)
		{
			_logger.error("WindowsServiceWrapper.start(): Unknown tool name '" + toolName + "'. Available tool names: " + TOOL_TO_CLASSNAME.keySet());
			return;
		}

		if ("central".equals(toolName))
		{
			// DbxTuneCentral has its own main() with different initialization
			_logger.info("WindowsServiceWrapper.start(): Launching DbxTuneCentral...");
			DbxTuneCentral.main(toolArgs);
			// DbxTuneCentral.main() blocks on ShutdownHandler.waitforShutdown(), then returns
		}
		else
		{
			// Set the main class name before calling DbxTune.main(),
			// since the stack trace detection won't find the right *Tune class
			// when launched through the service wrapper.
			DbxTune.setAppNameCmd(mainClassName);

			_logger.info("WindowsServiceWrapper.start(): Launching DbxTune with mainClassName='" + mainClassName + "'...");
			DbxTune.main(toolArgs);
			// DbxTune.main() blocks on ShutdownHandler.waitforShutdown(), then returns
		}

		_logger.info("WindowsServiceWrapper.start(): main() has returned. Service is stopping.");
	}

	/**
	 * Called by Apache Procrun to stop the service.
	 * <p>
	 * Signals the ShutdownHandler to unblock the waiting main thread in {@link #start(String[])}.
	 * The existing shutdown handlers in DbxTune/DbxTuneCentral will handle cleanup
	 * (stopping collectors, closing H2 databases, stopping Jetty, etc.).
	 *
	 * @param args Not used, but required by Procrun's stop method signature.
	 */
	public static void stop(String[] args)
	{
		_logger.info("WindowsServiceWrapper.stop(): Windows Service stop requested. Initiating graceful shutdown...");
		ShutdownHandler.shutdown("Windows Service Stop");
	}
}
