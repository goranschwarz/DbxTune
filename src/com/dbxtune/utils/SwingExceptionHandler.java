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

import org.apache.log4j.Logger;

public class SwingExceptionHandler
{
	private static final Logger	_logger	= Logger.getLogger(SwingExceptionHandler.class);

	public static void register()
	{
		System.setProperty("sun.awt.exception.handler", SwingExceptionHandler.class.getName());
	}

	public void handle(Throwable ex)
	{
		_logger.warn("Problems in AWT/Swing Event Dispatch Thread, Caught: "+ex.toString(), ex);
		
		// Maybe do some more if we are out of memory.
		if (ex instanceof OutOfMemoryError)
		{
			_logger.info("Send notification to memory monitor to evaluate memory usage, so that any listeners can take appropriate actions.");
			Memory.evaluate();
		}
	}
}
