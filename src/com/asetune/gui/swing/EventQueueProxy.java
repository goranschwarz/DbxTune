/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
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
package com.asetune.gui.swing;

import java.awt.AWTEvent;
import java.awt.EventQueue;
import java.awt.Toolkit;

import org.apache.log4j.Logger;

import com.asetune.utils.Memory;
import com.asetune.utils.SwingUtils;

public class EventQueueProxy extends EventQueue
{
	private static Logger _logger = Logger.getLogger(EventQueueProxy.class);

	@Override
	protected void dispatchEvent(AWTEvent newEvent)
	{
		try
		{
			super.dispatchEvent(newEvent);
		}
		catch (Throwable t)
		{
			_logger.warn("Unhandled Execption in SWING EventDispatchThread when dispatching an event. Caught: "+t, t);
//			_logger.warn("XXXXXXXXXXXXXX: t.getCause(): "+t.getCause(), t.getCause());
			
			if (t instanceof OutOfMemoryError)
			{
				_logger.info("Calling the Memory.fireOutOfMemory() handler to request a 'cleanup' of components that has registered themself. Here is a list of the components: " + Memory.getMemoryListener());
				Memory.fireOutOfMemory();
			}

			if (_logger.isDebugEnabled())
			{
				SwingUtils.showErrorMessage("Swing EDT Unhandled execption", "Unhandled Execption in SWING EventDispatchThread when dispatching an event. Caught: "+t, t);
			}
		}
	}
	
	public static void install()
	{
		EventQueue queue = Toolkit.getDefaultToolkit().getSystemEventQueue();
		queue.push(new EventQueueProxy());
	}
}
