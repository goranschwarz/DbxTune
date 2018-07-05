package com.asetune.utils;

//import org.apache.log4j.Logger;

public class WaitforShutdown
{
	// everything was moved to: ShutdownHandler
//	private static Logger _logger = Logger.getLogger(WaitforShutdown.class);
//
//	private static Object _waitforObject = new Object();
//	private static boolean _withRestart = false;
//
//	/**
//	 * Call this method, and the current thread will wait untill "someone" calls method shutdown.
//	 */
//	public static void waitforShutdown()
//	{
//		_withRestart = false;
//
//		synchronized (_waitforObject)
//		{
//			try
//			{
//				_logger.info("Waiting for shutdown on thread '"+Thread.currentThread().getName()+"'.");
//				_waitforObject.wait();
//				_logger.info("AFTER-Waiting for shutdown on thread '"+Thread.currentThread().getName()+"'.");
//			}
//			catch (InterruptedException e)
//			{
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//	}
//
//	/**
//	 * Release the waiter thread
//	 */
//	public static void shutdown(String reasonText)
//	{
//		shutdown(reasonText, false);
//	}
//
//	/**
//	 * Release the waiter thread
//	 */
//	public static void restart(String reasonText)
//	{
//		shutdown(reasonText, true);
//	}
//
//	/**
//	 * Release the waiter thread
//	 */
//	public static void shutdown(String reasonText, boolean withRestart)
//	{
//		_withRestart = withRestart;
//		
//		synchronized (_waitforObject)
//		{
//			_logger.info("Shutdown was called from from thread '"+Thread.currentThread().getName()+"'. reason='"+reasonText+"'.");
//			_waitforObject.notifyAll();
//		}
//	}
//	
//	/**
//	 * Was the restart option specified when shutdown was called
//	 * @return
//	 */
//	public static boolean wasRestartSpecified()
//	{
//		return _withRestart;
//	}
}
