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
