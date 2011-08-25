/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package asemon.utils;

import org.apache.log4j.Logger;

public class Memory
{
	private static Logger _logger          = Logger.getLogger(Memory.class);

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
				return true;
		}
		return false;
	}

	public static long getMemoryLeftInKB()
	{
		return getMaxMemoryInKB() - getAllocatedMemoryInKB() + getFreeMemoryInKB();
	}

	public static long getUsedMemoryInKB()
	{
		return (Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory()) / 1024;
	}

	public static long getFreeMemoryInKB()
	{
		return Runtime.getRuntime().freeMemory() / 1024;
	}

	public static long getAllocatedMemoryInKB()
	{
		return Runtime.getRuntime().totalMemory() / 1024;
	}

	public static long getMaxMemoryInKB()
	{
		return Runtime.getRuntime().maxMemory() / 1024;
	}

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

	public static int getMemoryLeftInMB()
	{
		return getMaxMemoryInMB() - getAllocatedMemoryInMB() + getFreeMemoryInMB();
	}

	public static int getUsedMemoryInMB()
	{
		return (int) (getUsedMemoryInKB() / 1024);
	}

	public static int getFreeMemoryInMB()
	{
		return (int) (getFreeMemoryInKB() / 1024);
	}

	public static int getAllocatedMemoryInMB()
	{
		return (int) (getAllocatedMemoryInKB() / 1024);
	}

	public static int getMaxMemoryInMB()
	{
		return (int) (getMaxMemoryInKB() / 1024);
	}

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
