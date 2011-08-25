/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package asemon.utils;

import org.apache.log4j.Logger;

public class Memory
{
	private static Logger _logger          = Logger.getLogger(Memory.class);

	public static void checkMemoryUsage(int thresholdMbLeft)
	{
		int mbLeft = getMemoryLeftInMB();

		_logger.debug(getMemoryInfoMB());
		if (mbLeft <= thresholdMbLeft)
		{
			// kick off the Garbage Collector
			System.gc();

			_logger.info("Free memory seems to be less that "+mbLeft+" MB. " + getMemoryInfoMB());
		}
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
