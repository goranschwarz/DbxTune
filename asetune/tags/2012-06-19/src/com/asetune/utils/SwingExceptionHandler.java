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
	}
}
