package com.asetune.gui.swing;

import java.awt.AWTEvent;
import java.awt.EventQueue;
import java.awt.Toolkit;

import org.apache.log4j.Logger;

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
