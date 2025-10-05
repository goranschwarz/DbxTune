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

import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MonitorInfo;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
			out.append("memory.dbxtune.util.getMemoryInfoMB:      " + Memory.getMemoryInfoMB()).append("\n");
			out.append("    os.getAvailableProcessors:            " + osBean.getAvailableProcessors()).append("\n");
			out.append("    os.getSystemLoadAverage:              " + osBean.getSystemLoadAverage()).append("\n");
			out.append("\n");
			
		}
//		double CPUTime   = cpuUser  .doubleValue() + cpuSystem.doubleValue() + cpuIdle.doubleValue();
//		double CPUUser   = cpuUser  .doubleValue();
//		double CPUSystem = cpuSystem.doubleValue();
//		double CPUIdle   = cpuIdle  .doubleValue();
//
//		BigDecimal pctCPUTime       = new BigDecimal( ((1.0 * (CPUUser + CPUSystem)) / CPUTime) * 100 ).setScale(1, RoundingMode.HALF_EVEN);
//		BigDecimal pctUserCPUTime   = new BigDecimal( ((1.0 * (CPUUser            )) / CPUTime) * 100 ).setScale(1, RoundingMode.HALF_EVEN);
//		BigDecimal pctSystemCPUTime = new BigDecimal( ((1.0 * (CPUSystem          )) / CPUTime) * 100 ).setScale(1, RoundingMode.HALF_EVEN);
//		BigDecimal pctIdleCPUTime   = new BigDecimal( ((1.0 * (CPUIdle            )) / CPUTime) * 100 ).setScale(1, RoundingMode.HALF_EVEN);

		ThreadInfo[] infos = threadBean.dumpAllThreads(true, true);
		for (ThreadInfo ti : infos)
		{
			if (extraFields)
			{
				long totalCpuTime  = threadBean.getThreadCpuTime(ti.getThreadId()) / 1000000L;
				long userCpuTime   = threadBean.getThreadUserTime(ti.getThreadId()) / 1000000L;
				long systemCpuTime = totalCpuTime - userCpuTime;
				BigDecimal userCpuPct   = totalCpuTime <= 0 ? new BigDecimal(0) : new BigDecimal( ((1.0 * (userCpuTime))   / totalCpuTime) * 100 ).setScale(1, RoundingMode.HALF_EVEN);
				BigDecimal systemCpuPct = totalCpuTime <= 0 ? new BigDecimal(0) : new BigDecimal( ((1.0 * (systemCpuTime)) / totalCpuTime) * 100 ).setScale(1, RoundingMode.HALF_EVEN);

				// isDaemon we need Java 11
				String extraInfo = "\t   + isDaemon=" + ti.isDaemon() + ",  isInNative=" + ti.isInNative() + ", isSuspended=" + ti.isSuspended() + ", blockCnt=" + ti.getBlockedCount() + ", waitCnt=" + ti.getWaitedCount() + ", blockTime=" + ti.getBlockedTime() + ", waitTime=" + ti.getWaitedTime() + "\n"
//				String extraInfo = "\t   + isInNative=" + ti.isInNative() + ", isSuspended=" + ti.isSuspended() + ", blockCnt=" + ti.getBlockedCount() + ", waitCnt=" + ti.getWaitedCount() + ", blockTime=" + ti.getBlockedTime() + ", waitTime=" + ti.getWaitedTime() + "\n"
				                 + "\t   + lockName='" + ti.getLockName() + "', ownedBy='" + ti.getLockOwnerName() + "', LockOwnerId=" + ti.getLockOwnerId() + "\n"
				                 + "\t   + thread Cpu Time Ms: Total=" + totalCpuTime + ", User=" + userCpuTime + " (" + userCpuPct + "%), System=" + systemCpuTime + "(" + systemCpuPct + "%)\n";

				StringBuilder sb = new StringBuilder(threadInfoToString(ti, -1));
		
				int firstTab = sb.indexOf("\t");
				if (firstTab != -1)
					sb.insert(firstTab, extraInfo);
				out.append(sb.toString());
			}
			else
			{
				out.append(threadInfoToString(ti, -1));
			}
		}
		return out.toString();
	}

	/**
	 * This method is grabbed from java.lang.management.ThreadInfo.toString() <br>
	 * But in here we can decide the stackDepth, which is default to 512 instead of 8
	 * <p>
	 * Returns a string representation of this thread info.
	 * The format of this string depends on the implementation.
	 * The returned string will typically include
	 * the {@linkplain #getThreadName thread name},
	 * the {@linkplain #getThreadId thread ID},
	 * its {@linkplain #getThreadState state},
	 * and a {@linkplain #getStackTrace stack trace} if any.
	 *
	 * @return a string representation of this thread info.
	 */
	public static String threadInfoToString(ThreadInfo ti, int maxFrames) 
	{
		if (maxFrames < 0 )
			maxFrames = 512;

		StringBuilder sb = new StringBuilder("\"" + ti.getThreadName() + "\"" +
		                                     " Id=" + ti.getThreadId() + " " +
		                                     ti.getThreadState());
		if (ti.getLockName() != null) {
			sb.append(" on " + ti.getLockName());
		}
		if (ti.getLockOwnerName() != null) {
			sb.append(" owned by \"" + ti.getLockOwnerName() +
			          "\" Id=" + ti.getLockOwnerId());
		}
		if (ti.isSuspended()) {
			sb.append(" (suspended)");
		}
		if (ti.isInNative()) {
			sb.append(" (in native)");
		}
		sb.append('\n');
		int i = 0;
		StackTraceElement[] stackTrace = ti.getStackTrace();
		for (; i < stackTrace.length && i < maxFrames; i++) {
			StackTraceElement ste = stackTrace[i];
			sb.append("\tat " + ste.toString());
			sb.append('\n');
			if (i == 0 && ti.getLockInfo() != null) {
				Thread.State ts = ti.getThreadState();
				switch (ts) {
					case BLOCKED:
						sb.append("\t-  blocked on " + ti.getLockInfo());
						sb.append('\n');
						break;
					case WAITING:
						sb.append("\t-  waiting on " + ti.getLockInfo());
						sb.append('\n');
						break;
					case TIMED_WAITING:
						sb.append("\t-  waiting on " + ti.getLockInfo());
						sb.append('\n');
						break;
					default:
				}
			}

			for (MonitorInfo mi : ti.getLockedMonitors()) {
				if (mi.getLockedStackDepth() == i) {
					sb.append("\t-  locked " + mi);
					sb.append('\n');
				}
			}
		}
		if (i < stackTrace.length) {
			sb.append("\t...");
			sb.append('\n');
		}

		LockInfo[] locks = ti.getLockedSynchronizers();
		if (locks.length > 0) {
			sb.append("\n\tNumber of locked synchronizers = " + locks.length);
			sb.append('\n');
			for (LockInfo li : locks) {
				sb.append("\t- " + li);
				sb.append('\n');
			}
		}
		sb.append('\n');
		return sb.toString();
	}


	/**
	 * Get starting class<br>
	 * Simply get a stacktrace and get LAST Entry, but only the class name
	 * @return
	 */
	public static String getMainStartClass()
	{
		StackTraceElement[] stack = Thread.currentThread().getStackTrace();
		StackTraceElement main = stack[stack.length - 1];
		String mainClassName = main.getClassName();
		int lastDot = mainClassName.lastIndexOf('.');
		if (lastDot != -1)
			mainClassName = mainClassName.substring(lastDot + 1);

		return mainClassName;
	}

	/**
	 * Check if the "classname" is the "mainStartClass"...
	 * 
	 * @param classname   Check if the "classname" is the "mainStartClass"...
	 * @return
	 */
	public static boolean isMainStartClass(String classname)
	{
		if (classname == null)
			return false;

		StackTraceElement[] stack = Thread.currentThread().getStackTrace();
		StackTraceElement main = stack[stack.length - 1];
		String mainClassName = main.getClassName();
		int lastDot = mainClassName.lastIndexOf('.');
		if (lastDot != -1)
			mainClassName = mainClassName.substring(lastDot + 1);

		return classname.equals(mainClassName);
	}



	/**
	 * Check if an Exception/callstack contains the <code>searchStr</code>
	 * <p>
	 * It's like looping the stacktrace and do <code>line.contains(searchStr)</code>
	 * 
	 * @param searchStr     What to search for
	 * 
	 * @return Simply true or false
	 */
	public static boolean isCalledFrom(String searchStr)
	{
		return isCalledFrom(new Exception(), searchStr);
	}

	/**
	 * Check if an Exception/callstack contains the <code>searchStr</code>
	 * <p>
	 * It's like looping the stacktrace and do <code>line.contains(searchStr)</code>
	 * 
	 * @param ex            Exception (in null, one will be created)
	 * @param searchStr     What to search for
	 * 
	 * @return Simply true or false
	 */
	public static boolean isCalledFrom(Exception ex, String searchStr)
	{
		return isCalledFrom(ex, searchStr, -1);
	}

	/**
	 * Check if an Exception/callstack contains the <code>searchStr</code>
	 * <p>
	 * It's like looping the stacktrace and do <code>line.contains(searchStr)</code>
	 * 
	 * @param ex            Exception (in null, one will be created)
	 * @param searchStr     What to search for
	 * @param stackDepth    Whats's the search depth (if below 0, then full depth will be searched)
	 * 
	 * @return Simply true or false
	 */
	public static boolean isCalledFrom(Exception ex, String searchStr, int stackDepth)
	{
		if (ex == null)
			ex = new Exception();
		
		StackTraceElement[] callstack = ex.getStackTrace();
		
		if (stackDepth < 0)
			stackDepth = callstack.length;

		stackDepth = Math.min(stackDepth, callstack.length);
		
		for (int i = 0; i < stackDepth; i++)
		{
			String line = callstack[i].toString();
			if (line.contains(searchStr))
				return true;
		}
		
		return false;
	}
}
