/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.utils;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

public class Memory
{
	private static Logger _logger          = Logger.getLogger(Memory.class);

	//-----------------------------------------------------------------------
	// BEGIN: Memory Handler functionality
	//-----------------------------------------------------------------------
	private static Thread _checkThread = null;
	private static int    _memLimitInMb = 10;
	private static ArrayList<MemoryListener> _memListeners = new ArrayList<Memory.MemoryListener>();

	public interface MemoryListener
	{
		public void outOfMemoryHandler();
		public void memoryConsumption(int memoryLeftInMB);
	}

	/** Set the limit in MB when the check thread will fire OutOfMemoryHandler calls, default is below 10 MB */ 
	public void setMemoryLimit(int memoryLeftInMB)
	{
		_memLimitInMb = memoryLeftInMB;
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
				try
				{
					while(true)
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
						
						Thread.sleep(2 * 1000);
					}
				}
				catch (InterruptedException ignore)
				{
					_logger.info("Received 'interrupted', so I will stop the memory checker thread.");
				}
				catch (Throwable t)
				{
					_logger.info("Caught '"+t+"', so I will stop the memory checker thread.", t);
				}
				_logger.info("Ending memory checker thread.");
				_checkThread = null;
			}
		};
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
}