package com.asetune.utils;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

public class JavaUtils
{
	/**
	 * Get the PID of the current running JVM Instance.
	 * @param fallback if we can't get the PID for some reason, return this value instead
	 * @return
	 */
	public static String getProcessId(final String fallback)
	{
		// Note: may fail in some JVM implementations
		// therefore fallback has to be provided

		// something like '<pid>@<hostname>', at least in SUN / Oracle JVMs
		final String jvmName = ManagementFactory.getRuntimeMXBean().getName();
		final int index = jvmName.indexOf('@');

		if ( index < 1 )
		{
			// part before '@' empty (index = 0) / '@' not found (index = -1)
			return fallback;
		}

		try
		{
			return Long.toString(Long.parseLong(jvmName.substring(0, index)));
		}
		catch (NumberFormatException e)
		{
			// ignore
		}
		return fallback;
	}
	


	/**
	 * Get a string with all threads stack-dumps... A little like jstack<br>
	 * 
	 * @param extraFields    Print some extra fields.
	 * @return
	 */
	public static String getStackDump(boolean extraFields)
	{
		StringBuilder out = new StringBuilder();

		OperatingSystemMXBean osBean     = ManagementFactory.getOperatingSystemMXBean();
		ThreadMXBean          threadBean = ManagementFactory.getThreadMXBean();
		MemoryMXBean          memoryBean = ManagementFactory.getMemoryMXBean();
		
		if (extraFields)
		{
			out.append("Dump of " + threadBean.getThreadCount() + " threads at " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(System.currentTimeMillis()))).append("\n");
			out.append("thread.findDeadlockedThreads:             " + Arrays.toString(threadBean.findDeadlockedThreads())).append("\n");
			out.append("thread.getThreadCount:                    " + threadBean.getThreadCount()).append("\n");
			out.append("thread.getTotalStartedThreadCount:        " + threadBean.getTotalStartedThreadCount()).append("\n");
			out.append("memory.getObjectPendingFinalizationCount: " + memoryBean.getObjectPendingFinalizationCount()).append("\n");
			out.append("memory.getHeapMemoryUsage:                " + memoryBean.getHeapMemoryUsage()).append("\n");
			out.append("memory.getNonHeapMemoryUsage:             " + memoryBean.getNonHeapMemoryUsage()).append("\n");
			out.append("memory.asetune.util.getMemoryInfoMB:      " + Memory.getMemoryInfoMB()).append("\n");
			out.append("    os.getAvailableProcessors:            " + osBean.getAvailableProcessors()).append("\n");
			out.append("    os.getSystemLoadAverage:              " + osBean.getSystemLoadAverage()).append("\n");
			out.append("\n");
			
		}
//		double CPUTime   = cpuUser  .doubleValue() + cpuSystem.doubleValue() + cpuIdle.doubleValue();
//		double CPUUser   = cpuUser  .doubleValue();
//		double CPUSystem = cpuSystem.doubleValue();
//		double CPUIdle   = cpuIdle  .doubleValue();
//
//		BigDecimal pctCPUTime       = new BigDecimal( ((1.0 * (CPUUser + CPUSystem)) / CPUTime) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
//		BigDecimal pctUserCPUTime   = new BigDecimal( ((1.0 * (CPUUser            )) / CPUTime) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
//		BigDecimal pctSystemCPUTime = new BigDecimal( ((1.0 * (CPUSystem          )) / CPUTime) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
//		BigDecimal pctIdleCPUTime   = new BigDecimal( ((1.0 * (CPUIdle            )) / CPUTime) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);

		ThreadInfo[] infos = threadBean.dumpAllThreads(true, true);
		for (ThreadInfo ti : infos)
		{
			if (extraFields)
			{
				long totalCpuTime  = threadBean.getThreadCpuTime(ti.getThreadId()) / 1000000L;
				long userCpuTime   = threadBean.getThreadUserTime(ti.getThreadId()) / 1000000L;
				long systemCpuTime = totalCpuTime - userCpuTime;
				BigDecimal userCpuPct   = totalCpuTime <= 0 ? new BigDecimal(0) : new BigDecimal( ((1.0 * (userCpuTime))   / totalCpuTime) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
				BigDecimal systemCpuPct = totalCpuTime <= 0 ? new BigDecimal(0) : new BigDecimal( ((1.0 * (systemCpuTime)) / totalCpuTime) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
				
				String extraInfo = "\t   + isInNative=" + ti.isInNative() + ", isSuspended=" + ti.isSuspended() + ", blockCnt=" + ti.getBlockedCount() + ", waitCnt=" + ti.getWaitedCount() + ", blockTime=" + ti.getBlockedTime() + ", waitTime=" + ti.getWaitedTime() + "\n"
				                 + "\t   + lockName='" + ti.getLockName() + "', ownedBy='" + ti.getLockOwnerName() + "', LockOwnerId=" + ti.getLockOwnerId() + "\n"
				                 + "\t   + thread Cpu Time Ms: Total=" + totalCpuTime + ", User=" + userCpuTime + " (" + userCpuPct + "%), System=" + systemCpuTime + "(" + systemCpuPct + "%)\n";

				StringBuilder sb = new StringBuilder(ti.toString());
		
				int firstTab = sb.indexOf("\t");
				if (firstTab != -1)
					sb.insert(firstTab, extraInfo);
				out.append(sb.toString());
			}
			else
			{
				out.append(ti);
			}
		}
		return out.toString();
	}
}
