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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 * This installes a JVM shutdown hook by calling <code>Runtime.getRuntime().addShutdownHook(_shutdownHook);</code>
 * <p>
 * The JVM calls all shutdown hooks when <ul>
 * <li>Any thread calls System.exit()</li>
 * <li>It receives any <i>kill</i> signal from the OS</li>
 * </ul>
 * The JVM will exit <b>after</b> all shutdown hooks has been completed
 * <p>
 * This module you can register <i>shutdown</i> handlers that will help you shutdown your system in a clean way.<br>
 * Which probably means stopping various sub systems
 * <p>
 * The shutdown hook will do nothing in case thare are <b>no</b> non-daemon threads alive when it kicks off<br>
 * That means if you install a hook early/before initialization and if the init section throws a Exception which makes
 * the server to <i>not start</i>. Then the shutdown hook will do nothing. <br>
 * Nor will the shutdown hook kick in if all your threads have been stopped properly before we reach the end
 * (meaning you received a normal shutdown event from your own services, and you have done your own cleanup/stop of the system. 
 * <p>
 * Here is an example of usage:
 * <pre>
 * // Add a shutdown handler (called when we recive ctrl-c or kill -15)
 * ShutdownHandler.addShutdownHandler(new ShutdownHandler.Shutdownable()
 * {
 *     public List<String> systemShutdown()
 *     {
 *          // Some code that will shutdown/stop your worker threads 
 *          shutdownYourWorkerThreads();
 * 	
 *          // Return name of thread(s) that must be completed before the shutdown hook finishes (and leave of to the JVM)
 *          return Arrays.asList(new String[]{"threadName1ToWaitFor"});
 *      }
 * });
 * </pre>
 * 
 * @author gorans
 *
 */
public class ShutdownHandler
implements Runnable
{
	private static Logger _logger = Logger.getLogger(ShutdownHandler.class);

	public static final String  PROPKEY_maxWaitTime = "ShutdownHandler.maxWaitTime";
	public static final int     DEFAULT_maxWaitTime = 60*1000; // 60 sec

	/** Restart exit code is 8  ( or laying 8 : a lemniscate = infinity symbol) */
	public static final int     RESTART_EXIT_CODE = 8; 

	private static List<Shutdownable> _handlers = new ArrayList<>();
	private static Thread _shutdownHook = null;

	private static long _maxWaitTime = DEFAULT_maxWaitTime;

	/** used by wiatforShutdown() */
	private static Object _waitforObject = new Object();
	private static boolean _withRestart = false;
	private static Configuration _shutdownConfig = null;

	private static String _shutdownReasonText = "";

	/**
	 * Interface for handlers that want to be notified on shutdowns.
	 */
	public interface Shutdownable
	{
		/** 
		 * System has received a "shutdown" event from the OS, this could be a kill -15 &lt;spid&gt; or similar 
		 * <p>
		 * if the callback is asynchronious and you want the shutdown handler to wait for any threads terminate 
		 * before returning OK-to-shutdown to the JVM, then return a list of thread names to wait for.
		 * <p>
		 * Note: we will wait a maximum of X seconds for the threads. see: setMaxWaitTime()
		 * 
		 * @return List of Thread names to waitfor, this can be null
		 * */
		public List<String> systemShutdown();
	}

	/**
	 * The ShutdownHandler will do: Runtime.getRuntime().addShutdownHook() to our internal Thread
	 * @throws RuntimeException if a shutdown hook has already been installed
	 */
	public static void installJvmShutdownHook()
	{
		if (_shutdownHook != null)
			throw new RuntimeException("A shutdown hook is already installed.");

		// Get some basic configuration
		_maxWaitTime = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_maxWaitTime, DEFAULT_maxWaitTime);

		// Create the thread (but do NOT start it), the startup is done by the JVM when it needs it.
		_shutdownHook = new Thread( new ShutdownHandler() );
		_shutdownHook.setName("ShutdownHandler");

		// Install the hook to the JVM
		Runtime.getRuntime().addShutdownHook(_shutdownHook);
	}

	/**
	 * The ShutdownHandler will remove it'self from the shutdown hook by: Runtime.getRuntime().removeShutdownHook(_shutdownHook)
	 * @throws RuntimeException if a shutdown hook has not yet been installed
	 */
	public static void removeJvmShutdownHook()
	{
		if (_shutdownHook == null)
			throw new RuntimeException("Can't remove the shutdown hook, there is NONE installed");

		Runtime.getRuntime().removeShutdownHook(_shutdownHook);
		_shutdownHook = null;
	}

	/**
	 * Checks if we have installed a shutdown hook in the JVM
	 */
	public static boolean hasJvmShutdownHook()
	{
		return _shutdownHook != null;
	}


	/** whats the MAX time to wait for thread names returned by Shutdownable.systemShutdown() */
	public static long getMaxWaitTime()
	{
		return _maxWaitTime;
	}

	/** set the MAX time to wait for thread names returned by Shutdownable.systemShutdown() */
	public static void setMaxWaitTime(long maxWaitTime)
	{
		_maxWaitTime = maxWaitTime;
	}


	
	
	/**
	 * Install an application handler that will be called when a system shutdown happens
	 * @param handler
	 */
	public static void addShutdownHandler(Shutdownable handler) 
	{
		addShutdownHandler(-1, handler);
	}

	/**
	 * Install an application handler that will be called when a system shutdown happens
	 * @param index    The index of the list... -1 == append at the end
	 * @param handler
	 */
	public static void addShutdownHandler(int index, Shutdownable handler) 
	{
		if ( ! hasJvmShutdownHook() )
			installJvmShutdownHook();

		if (index >= 0)
			_handlers.add(index, handler);
		else
			_handlers.add(handler);
	}

	/**
	 * Removes an application handler that will be called when a system shutdown happens
	 * @param handler
	 */
	public static void removeShutdownHandler(Shutdownable handler) 
	{
		_handlers.remove(handler);
		
		// should we remove the JVM hook if we have no more handlers installed
		if (_handlers.isEmpty())
			removeJvmShutdownHook();
	}


	
	/**
	 * Call this method, and the current thread will wait untill "someone" calls method shutdown.
	 */
	public static void waitforShutdown()
	{
		_withRestart = false;

		synchronized (_waitforObject)
		{
			try
			{
				_logger.info("Waiting for shutdown on thread '"+Thread.currentThread().getName()+"'.");
				_waitforObject.wait();
				_logger.info("AFTER-Waiting for shutdown on thread '"+Thread.currentThread().getName()+"'.");
			}
			catch (InterruptedException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * Release the waiter thread
	 */
	public static void shutdown(String reasonText)
	{
		shutdown(reasonText, false, null);
	}

	/**
	 * Release the waiter thread
	 */
	public static void shutdownWithRestart(String reasonText)
	{
		shutdown(reasonText, true, null);
	}

	/**
	 * Release the waiter thread
	 */
	public static void shutdown(String reasonText, boolean withRestart, Configuration shutdownConfig)
	{
		// Only set this for "the FIRST caller"
		if (StringUtil.isNullOrBlank(_shutdownReasonText))
		{
			_shutdownConfig = shutdownConfig;
			
			_withRestart = withRestart;
			_shutdownReasonText = reasonText;
		}
		else
		{
			_logger.info("The just sent shutdown request properties [reasonText='"+reasonText+"', withRestart="+withRestart+"] wont be used... keeping first entered request with properties [reasonText='"+_shutdownReasonText+"', withRestart="+_withRestart+"]. The shutdown request will still be done, but the shutdown properties will be discarded.");
		}
		
		synchronized (_waitforObject)
		{
			_logger.info("Shutdown was called from from thread '"+Thread.currentThread().getName()+"'. reason='"+reasonText+"'.");
			_waitforObject.notifyAll();
		}
	}
	
	/**
	 * Was the restart option specified when shutdown was called
	 * @return
	 */
	public static boolean wasRestartSpecified()
	{
		return _withRestart;
	}
	
	/**
	 * If a Configuration was passed during shutdown.
	 * @return null or a Configuration object
	 */
	public static Configuration getShutdownConfiguration()
	{
		if (_shutdownConfig == null)
			return Configuration.emptyConfiguration();
		return _shutdownConfig;
	}
	
	
	public static String getShutdownReason()
	{
		return _shutdownReasonText;
	}
	

	/**
	 * Here is the shutdown hook code
	 */
	@Override
	public void run()
	{
		List<Thread> nonDeamonList = new ArrayList<>();
		for (Thread th : Thread.getAllStackTraces().keySet()) 
		{
			// Do not considder: yorself and the "DestroyJavaVM" thread
			if (th == Thread.currentThread() || "DestroyJavaVM".equals(th.getName()))
				continue;
			
			// If the thread issued System.exit()
			if (didThreadIssueExit(th))
				continue;
			
			// deamon thread, will not be considdered
			if ( th.isDaemon() )
				continue;

			// everything else
			nonDeamonList.add(th);
		}
		
		if (nonDeamonList.isEmpty())
		{
			_logger.debug("Shutdown-hook: Normal shutdown, since no 'non-daemon' thread is alive...");
			return;
		}

		if (_logger.isDebugEnabled())
		{
			_logger.debug("Shutdown-hook: Initiating non-normal shutdown. nonDeamonList count="+nonDeamonList.size());
			for (Thread th : nonDeamonList)
			{
				_logger.debug("Shutdown-hook: isDaemon="+th.isDaemon()+", threadName=|"+th.getName()+"|, th.getClass().getName()=|"+th.getClass().getName()+"|.");
			}
		}
		
		// A list of thread names to waitfor termination
		List<String> waitforThreadNames = new ArrayList<>();
		
		// Call any listeners in sequentially in *reversed* add order
		if ( ! _handlers.isEmpty() )
		{
			_logger.info("Shutdown-hook: issuing 'systemShutdown' on " + _handlers.size() + " handlers..");

			for (int i=_handlers.size()-1; i>=0; i--)
			{
				Shutdownable handler = _handlers.get(i);
				
				// call the registered shutdown methods
				List<String> waitList = handler.systemShutdown();

				// if the handler wanted us to wait for some threads to terminate before the JVM shutdown completes
				if (waitList != null && !waitList.isEmpty())
					waitforThreadNames.addAll(waitList);
			}
		}
		
		// should we WAIT for threads to terminate before we returns (and lets the JVM terminate)
		waitforThreads(waitforThreadNames);
		
		_logger.info("Shutdown-hook: end.");
	}

	/**
	 * Wait for the list of thread names to complete
	 * @param waitforThreadNames
	 */
	private void waitforThreads(List<String> waitforThreadNames)
	{
		if (waitforThreadNames.isEmpty())
		{
			_logger.info("Shutdown-hook: No threads to wait for, the waiting list is empty.");
			return;
		}

		_logger.info("Shutdown-hook: Waiting for the following thread names to terminate before JVM Shutdown: "+waitforThreadNames);

		long sleepTime   = 1000;
		long maxWaitTime = getMaxWaitTime();
		long startTime   = System.currentTimeMillis();

		// Wait for threads to terminated
		while (! waitforThreadNames.isEmpty())
		{
			// Wait for thread "GetCountersNoGui" has terminated
			String waitForThreadName = waitforThreadNames.get(0);
			
			while (true) // break on: notFound or return: on timeout
			{
				Thread waitForThread = null;
				for (Thread th : Thread.getAllStackTraces().keySet())
				{
					if (waitForThreadName.equals(th.getName()))
						waitForThread = th;
				}

				if (waitForThread == null)
				{
					waitforThreadNames.remove(waitForThreadName);
					break;  // ON NOT-FOUND: get out of the while(true)
				}
				else // The thread was found: So WAIT
				{
					// Check if the thread we are waiting for is NOT the one that issued a "exit", which caused the shutdown-hook to be triggered
					// we cant wait for our self, then we will have a timeout
					// simply remove the thread name from the waiting list nad continue.
					if (didThreadIssueExit(waitForThread))
					{
						_logger.info("Shutdown-hook: Skip waiting for thread '"+waitForThreadName+"', which issued 'System.exit'. Removing this thread from the waiting list.");
						waitforThreadNames.remove(waitForThreadName);
						break;
					}

					// But dont wait invane... onnor the timeout
					if (TimeUtils.msDiffNow(startTime) > maxWaitTime)
					{
						_logger.warn("Shutdown-hook: Waited for thread '"+waitForThreadName+"' to terminate. maxWaitTime="+maxWaitTime+" ms has been expired. STOP WAITING.");
						_logger.warn("Shutdown-hook: Here is a stacktrace of the thread '"+waitForThreadName+"' we are waiting for:" 
								+ StringUtil.stackTraceToString(waitForThread.getStackTrace()));

						String stackDump = JavaUtils.getStackDump(true);
						_logger.warn("Shutdown-hook: For completeness, lets stacktrace all other threads.\n" + stackDump);
						
						return; // ON TIMEOUT: get out method
					}

					_logger.info("Shutdown-hook: Still waiting for thread '"+waitForThreadName+"' to terminate... sleepTime="+sleepTime+", TotalWaitTime="+TimeUtils.msDiffNow(startTime)+", maxWaitTime="+maxWaitTime);

					// Sleep for X ms, but if the threads treminates earlier, then continue logic
					try { waitForThread.join(sleepTime); }
					catch (InterruptedException ignore) {}
				}
			} // end: while(true) 
		} // end: while (! waitforThreadNames.isEmpty())
	} // end: method

	/**
	 * If the threads stacktrace contains "java.lang.Shutdown.exit()"
	 * 
	 * @param thread
	 * @return
	 */
	private boolean didThreadIssueExit(Thread thread)
	{
		boolean thraedIssuedExit = false;
		for (StackTraceElement ste : thread.getStackTrace())
		{
			if (ste.getClassName().endsWith("Shutdown") && ste.getMethodName().equals("exit"))
			{
				thraedIssuedExit = true;
				break;
			}
		}
		return thraedIssuedExit;
	}
	
	
	//---------------------------------------------------------------------------------------
	//---------------------------------------------------------------------------------------
	//-- Below is some development test code
	//---------------------------------------------------------------------------------------
	//---------------------------------------------------------------------------------------
	private static class DummtWorker implements Shutdownable
	{
		String _tName     = null;
		long   _sleepTime;
		
		public DummtWorker(String name, long sleepTime)
		{
			_tName = name;
			_sleepTime = sleepTime;
			
			Thread t = new Thread(name)
			{
				@Override
				public void run()
				{
					try 
					{ 
//						if ("xxx1".equals(_tName))
//							System.exit(9);

						System.out.println("START THREAD: "+Thread.currentThread().getName()+", sleepTime="+_sleepTime);
						Thread.sleep(_sleepTime * 1000); 
						System.out.println("STOP THREAD: "+Thread.currentThread().getName());

//						if ("xxx1".equals(_tName))
//							System.exit(9);
					}
					catch (Exception ignore) { }
				}
			};
			t.start();
		}


		@Override
		public List<String> systemShutdown()
		{
			return Arrays.asList( new String[]{_tName} );
		}
	};
	
	public static void main(String[] args)
	{
		Properties log4jProps = new Properties();
		//log4jProps.setProperty("log4j.rootLogger", "INFO, A1");
		log4jProps.setProperty("log4j.rootLogger", "DEBUG, A1");
		log4jProps.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
		log4jProps.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
		log4jProps.setProperty("log4j.appender.A1.layout.ConversionPattern", "%d - %-5p - %-30c{1} - %m%n");
		PropertyConfigurator.configure(log4jProps);

		System.out.println("START.");
		System.out.println("   to test: press ctrl-c, or kill the process in some way (kill <spid>).");

		ShutdownHandler.addShutdownHandler(new DummtWorker("TIMEOUT", 50));
		ShutdownHandler.addShutdownHandler(new DummtWorker("xxx6", 45));
		ShutdownHandler.addShutdownHandler(new DummtWorker("xxx5", 40));
		ShutdownHandler.addShutdownHandler(new DummtWorker("xxx4", 35));
		ShutdownHandler.addShutdownHandler(new DummtWorker("xxx3", 30));
		ShutdownHandler.addShutdownHandler(new DummtWorker("xxx2", 25));
		ShutdownHandler.addShutdownHandler(new DummtWorker("xxx1", 20));
		
		if ( args.length > 0 )
			throw new RuntimeException("asdfasdf");
		
		try {Thread.sleep(5*1000); }
		catch (Exception ignore) { }

		System.out.println("END...");
		
//		System.exit(0);
	}
	
}
