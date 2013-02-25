package com.asetune.utils;

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
