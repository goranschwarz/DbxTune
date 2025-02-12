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

/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.dbxtune.utils;

import java.lang.invoke.MethodHandles;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class Memory
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	//-----------------------------------------------------------------------
	// BEGIN: Memory Handler functionality
	//-----------------------------------------------------------------------
	private static Thread  _checkThread = null;
	private static boolean _running = false;
	private static int     _memLimitInMb = 20;
	private static int     _sleepTimeInSec = 3;
	private static ArrayList<MemoryListener> _memListeners = new ArrayList<Memory.MemoryListener>();

	/** used by evaluateAndWait() to wait for memory evaluation to complete */
	private static Object _evaluateAndWaitSemaphore = new Object();

	/** Interface used by any listeners */
	public interface MemoryListener
	{
		public void outOfMemoryHandler();
		public void memoryConsumption(int memoryLeftInMB);

//		/** Get a information string about the memory consumption in the implementation module */
//		public String getMemoryConsumption();
//
//		/** Get the name of the implementation */
//		public String getMemoryModuleName();
	}

	/** Set the limit in MB when the check thread will fire OutOfMemoryHandler calls, default is below 10 MB */ 
	public void setMemoryLimit(int memoryLeftInMB)
	{
		_memLimitInMb = memoryLeftInMB;
	}
	public int getMemoryLimit()
	{
		return _memLimitInMb;
	}

	/** How often should we check for memory, default is every 3 seconds */ 
	public void setSleepTime(int seconds)
	{
		_sleepTimeInSec = seconds;
	}
	public int getSleepTime()
	{
		return _sleepTimeInSec;
	}

	/** Add a listener to the memory checker */
	public static void addMemoryListener(MemoryListener l) 
	{
		if ( ! _memListeners.contains(l) )
			_memListeners.add(l);
	}

	/** remove a listener from the memory checker */
	public static void removeMemoryListener(MemoryListener l) 
	{
		_memListeners.remove(l);
	}

	/** Get a List of all listeners from the memory checker */
	public static List<MemoryListener> getMemoryListener() 
	{
		return _memListeners;
	}
	/** Call all listers.outOfMemoryHandler */
	public static void fireOutOfMemory() 
	{
		for (MemoryListener ml : getMemoryListener())
		{
			_logger.info("Memory.fireOutOfMemory(): calling outOfMemoryHandler() on listener: "+ml);
			ml.outOfMemoryHandler();
		}
	}
	/** Call all listers.memoryConsumption(memoryLeftInMB) */
	public static void fireMemoryConsumption(int memoryLeftInMB) 
	{
		for (MemoryListener ml : getMemoryListener())
		{
			ml.memoryConsumption(memoryLeftInMB);
		}
	}
//	/** Call all listers.getMemoryConsumption() */
//	public static Map<String, String> getMemoryConsumption() 
//	{
//		Map<String, String> map = new LinkedHashMap<>();
//		//  name,   memInfo
//
//		for (MemoryListener ml : getMemoryListener())
//		{
//			String name    = ml.getMemoryModuleName();
//			String memInfo = ml.getMemoryConsumption();
//			
//			map.put(name, memInfo);
//		}
//		
//		return map;
//	}

	/**
	 * If the memory checker thread is sleeping, simply interrupt it and let it go to work. <br>
	 * This is async, so your current thread will continue before the memory chaek has been completed
	 */
	public static void evaluate()
	{
		if (_checkThread != null)
		{
			_logger.info("Received 'evaluate' notification from thread '"+Thread.currentThread().getName()+"', the memory monitor thread will 'now' evaluate the memory usage.");
			_checkThread.interrupt();
		}
		else
		{
			_logger.warn("Memory monitor thread, has not been started.");
		}
	}
	
	/**
	 * If the memory checker thread is sleeping, simply interrupt it and let it go to work. <br>
	 * This also waits untill the memory thread has done it's work.
	 */
	public static void evaluateAndWait()
	{
		evaluateAndWait(-1);
	}
	
	/**
	 * If the memory checker thread is sleeping, simply interrupt it and let it go to work. <br>
	 * This also waits untill the memory thread has done it's work.
	 * 
	 * @param maxWaitTime maximum wait time before we continue (-1 = wait forever)
	 */
	public static void evaluateAndWait(int maxWaitTime)
	{
		synchronized (_evaluateAndWaitSemaphore)
		{
			try
			{
				_logger.info("Waiting for memory evaluater to complete.");
				long startTime = System.currentTimeMillis();

				if (maxWaitTime < 0)
					_evaluateAndWaitSemaphore.wait();
				else
					_evaluateAndWaitSemaphore.wait(maxWaitTime);

				_logger.info("AFTER-Waiting for memory evaluater to complete. Wait time was: " + TimeUtils.msDiffNowToTimeStr(startTime));
			}
			catch (InterruptedException ex)
			{
				_logger.info("Memory.evaluateAndWait(maxWaitTime="+maxWaitTime+"): was interupted. ex="+ex);
			}
		}
	}

	/**
	 * isRunning
	 */
	public static boolean isRunning()
	{
		return (_checkThread != null && _running);
	}

	/**
	 * Stop it
	 */
	public static void stop()
	{
		_running = false;
		if (_checkThread != null)
		{
			_checkThread.interrupt();
		}
	}
	/**
	 * Start a memory check thread that will notify any listeners
	 */
	public static void start()
	{
		if ( _checkThread != null )
		{
			_logger.info("There is already a memory check thread running, I will NOT start another one.");
			return;
		}
		_checkThread = new Thread()
		{
			@Override
			public void run()
			{
				_logger.info("Starting memory checker thread.");
				int  exceptionCount = 0;
				int  exceptionCountExitThreshold = 25;
				long lastExceptionTime = 0;
				long lastExceptionTimeThreshold = 10000;

				while(_running)
				{
					try
					{
						int mbLeftAtStart = getMemoryLeftInMB();
						
						fireMemoryConsumption(mbLeftAtStart);
	
						if (mbLeftAtStart <= _memLimitInMb)
						{
							// kick off the Garbage Collector
							System.gc();
		
							int mbLeftAfterGc = getMemoryLeftInMB();
		
							_logger.info("Free memory seems to be less that "+mbLeftAtStart+" MB. After Garbage Collection we got "+mbLeftAfterGc+" MB." + getMemoryInfoMB());
		
							// If still not enough memory, return true, which should mean TAKE ACTION;
							if (mbLeftAfterGc <= _memLimitInMb)
								fireOutOfMemory();
						}

						// Notify anyone that called: evaluateAndWait()
						synchronized (_evaluateAndWaitSemaphore)
						{
							_evaluateAndWaitSemaphore.notifyAll();
						}

						// Sleep until next check
						// This can be interupted by the method: evaluate(); 
						Thread.sleep(_sleepTimeInSec * 1000);
					}
					catch (InterruptedException ignore)
					{
						_logger.debug("Received 'interrupted', checking if we should continue to run.");
					}
					catch (Throwable t)
					{
						exceptionCount++;
						
						if (exceptionCount > exceptionCountExitThreshold)
						{
							_logger.info("Caught '"+t+"', so I will stop the memory checker thread. exceptionCount="+exceptionCount+", exceptionCountExitThreshold="+exceptionCountExitThreshold, t);
							_running = false;
						}
						else
						{
							_logger.warn("Memory checker, Caught '"+t+"', skipping this and continuing... exceptionCount="+exceptionCount+", exceptionCountExitThreshold="+exceptionCountExitThreshold, t);
						}
						// Reset execption count, if it has gone more that X milliseconds since last exception
						if (System.currentTimeMillis() - lastExceptionTime > lastExceptionTimeThreshold)
							exceptionCount = 0;
						lastExceptionTime = System.currentTimeMillis();
					}
				} // end: while(_running)

				_logger.info("Ending memory checker thread.");
				_checkThread = null;
			}
		};

		_running = true;
		_checkThread.setName("MemoryCheckThread");
		_checkThread.setDaemon(true);
		_checkThread.start();
	}

	/**
	 * Stop/shutdown the memory check thread
	 */
	public static void shutdown()
	{
		if ( _checkThread == null )
		{
			_logger.info("The memory check thread isn't running, so no need to shut it down.");
			return;
		}

		_logger.info("Sending an 'interrupt' to the memory check thread.");
		_checkThread.interrupt();
	}
	//-----------------------------------------------------------------------
	// END: Memory Handler functionality
	//-----------------------------------------------------------------------

	
	/**
	 * Check if free memory is less than the <code>thresholdMbLeft</code>. 
	 * If free memory is less, than this, call Garbage Collector and return true
	 * @param thresholdMbLeft
	 * @return true if less memory than <code>thresholdMbLeft</code> otherwise false.
	 */
	public static boolean checkMemoryUsage(int thresholdMbLeft)
	{
		int mbLeftAtStart = getMemoryLeftInMB();

		_logger.debug(getMemoryInfoMB());
		if (mbLeftAtStart <= thresholdMbLeft)
		{
			// kick off the Garbage Collector
			System.gc();

			int mbLeftAfterGc = getMemoryLeftInMB();

			_logger.info("Free memory seems to be less that "+mbLeftAtStart+" MB. After Garbage Collection we got "+mbLeftAfterGc+" MB." + getMemoryInfoMB());

			// If still not enough memory, return true, which should mean TAKE ACTION;
			if (mbLeftAfterGc <= thresholdMbLeft)
			{
				fireOutOfMemory();
				return true;
			}
		}
		return false;
	}

	
	
	
	////////////////////////////////////////////////////////////
	// KB methods
	////////////////////////////////////////////////////////////

	/** 
	 * How much memory have we got left before we reach OutOfMemory<br>
	 * Formula: getMaxMemoryInKB() - getAllocatedMemoryInKB() + getFreeMemoryInKB()
	 * @return KB
	 */
	public static long getMemoryLeftInKB()
	{
		return getMaxMemoryInKB() - getAllocatedMemoryInKB() + getFreeMemoryInKB();
	}

	/** 
	 * How much memory do we Use/Consume for the moment.<br>
	 * Formula: (totalMemory - freeMemory) / 1024
	 * @return KB
	 */
	public static long getUsedMemoryInKB()
	{
		return (Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory()) / 1024;
	}

	/**
	 * How much memory doe the JVM think we have free<br>
	 * Just calls Runtime freeMemory() / 1024
	 * @return KB
	 */
	public static long getFreeMemoryInKB()
	{
		return Runtime.getRuntime().freeMemory() / 1024;
	}

	/**
	 * How much memory has the JVM allocated for the moment<br>
	 * Just calls Runtime totalMemory() / 1024
	 * @return KB
	 */
	public static long getAllocatedMemoryInKB()
	{
		return Runtime.getRuntime().totalMemory() / 1024;
	}

	/**
	 * How much memory Can the JVM Allocate<br>
	 * Just calls Runtime maxMemory() / 1024
	 * @return KB
	 */
	public static long getMaxMemoryInKB()
	{
		return Runtime.getRuntime().maxMemory() / 1024;
	}

	/**
	 * Just get a String of: 'TotalFreeMemory', 'UsedMemory', 'FreeMemory', 'AllocatedMemory', 'MaxMemory'.
	 */
	public static String getMemoryInfoKB()
	{
		return "Memory usage: "
				+ "TotalFreeMemory=" + getMemoryLeftInKB() + " KB, "
				+ "UsedMemory=" + getUsedMemoryInKB() + " KB, "
				+ "FreeMemory=" + getFreeMemoryInKB() + " KB, "
				+ "AllocatedMemory=" + getAllocatedMemoryInKB()  + " KB, "
				+ "MaxMemory=" + getMaxMemoryInKB() + " KB."
				;
	}

	
	
	
	////////////////////////////////////////////////////////////
	// MB methods
	////////////////////////////////////////////////////////////

	/** 
	 * How much memory have we got left before we reach OutOfMemory<br>
	 * Formula: getMaxMemoryInMB() - getAllocatedMemoryInMB() + getFreeMemoryInMB()
	 * @return MB
	 */
	public static int getMemoryLeftInMB()
	{
		return getMaxMemoryInMB() - getAllocatedMemoryInMB() + getFreeMemoryInMB();
	}

	/** 
	 * How much memory do we Use/Consume for the moment.<br>
	 * Formula: (totalMemory - freeMemory) / 1024 / 1024
	 * @return MB
	 */
	public static int getUsedMemoryInMB()
	{
		return (int) (getUsedMemoryInKB() / 1024);
	}

	/**
	 * How much memory doe the JVM think we have free<br>
	 * Just calls Runtime freeMemory() / 1024 / 1024
	 * @return MB
	 */
	public static int getFreeMemoryInMB()
	{
		return (int) (getFreeMemoryInKB() / 1024);
	}

	/**
	 * How much memory has the JVM allocated for the moment<br>
	 * Just calls Runtime totalMemory() / 1024 / 1024
	 * @return MB
	 */
	public static int getAllocatedMemoryInMB()
	{
		return (int) (getAllocatedMemoryInKB() / 1024);
	}

	/**
	 * How much memory Can the JVM Allocate<br>
	 * Just calls Runtime maxMemory() / 1024 / 1024
	 * @return MB
	 */
	public static int getMaxMemoryInMB()
	{
		return (int) (getMaxMemoryInKB() / 1024);
	}

	/**
	 * Just get a String of: 'TotalFreeMemory', 'UsedMemory', 'FreeMemory', 'AllocatedMemory', 'MaxMemory'.
	 */
	public static String getMemoryInfoMB()
	{
		return "Memory usage: "
				+ "TotalFreeMemory=" + getMemoryLeftInMB() + " MB, "
				+ "UsedMemory=" + getUsedMemoryInMB() + " MB, "
				+ "FreeMemory=" + getFreeMemoryInMB() + " MB, "
				+ "AllocatedMemory=" + getAllocatedMemoryInMB()  + " MB, "
				+ "MaxMemory=" + getMaxMemoryInMB() + " MB."
				;
	}

	
	
	
	////////////////////////////////////////////////////////////
	// Physical Memory
	////////////////////////////////////////////////////////////
	@SuppressWarnings("restriction")
	private static com.sun.management.OperatingSystemMXBean _osMbean = (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();


	/**
	 * How much physical memory does machine have<br>
	 * Uses com.sun.management.OperatingSystemMXBean.getTotalPhysicalMemorySize()
	 * @return MB
	 */
	public static int getTotalPhysicalMemorySizeInMB()
	{
		@SuppressWarnings("restriction")
		long osPhysMemSize     = _osMbean.getTotalPhysicalMemorySize();

		return (int) (osPhysMemSize / 1024 / 1024);
	}
	

	/**
	 * How much <b>free</b> physical memory does machine have<br>
	 * Uses com.sun.management.OperatingSystemMXBean
	 * @return MB
	 */
	public static int getFreePhysicalMemorySizeInMB()
	{
		@SuppressWarnings("restriction")
		long osPhysFreeMemSize = _osMbean.getFreePhysicalMemorySize();

		return (int) (osPhysFreeMemSize / 1024 / 1024);
	}
	

}
