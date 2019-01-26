/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
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
package com.asetune.utils;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;

import javax.management.MBeanServer;

/**
 * Grabbed from: https://blogs.oracle.com/sundararajan/programmatically-dumping-heap-from-java-applications
 * <br>
 * https://stackoverflow.com/questions/12295824/create-heap-dump-from-within-application-without-hotspotdiagnosticmxbean
 *
 */
public class HeapDump
{
	// This is the name of the HotSpot Diagnostic MBean
	private static final String	   HOTSPOT_BEAN_NAME = "com.sun.management:type=HotSpotDiagnostic";

	// field to store the hotspot diagnostic MBean
	private static volatile Object hotspotMBean;

	/**
	 * Call this method from your application whenever you want to dump the heap
	 * snapshot into a file.
	 *
	 * @param fileName	   name of the heap dump file
	 * @param live         flag that tells whether to dump only the live objects
	 */
	static void dumpHeap(String fileName, boolean live)
	{
		// initialize hotspot diagnostic MBean
		initHotspotMBean();
		try
		{
			Class clazz = Class.forName("com.sun.management.HotSpotDiagnosticMXBean");
			Method m = clazz.getMethod("dumpHeap", String.class, boolean.class);
			m.invoke(hotspotMBean, fileName, live);
		}
		catch (RuntimeException re)
		{
			throw re;
		}
		catch (Exception exp)
		{
			throw new RuntimeException(exp);
		}
	}

	// initialize the hotspot diagnostic MBean field
	private static void initHotspotMBean()
	{
		if ( hotspotMBean == null )
		{
			synchronized (HeapDump.class)
			{
				if ( hotspotMBean == null )
				{
					hotspotMBean = getHotspotMBean();
				}
			}
		}
	}

	// get the hotspot diagnostic MBean from the
	// platform MBean server
	private static Object getHotspotMBean()
	{
		try
		{
			Class clazz = Class.forName("com.sun.management.HotSpotDiagnosticMXBean");
			MBeanServer server = ManagementFactory.getPlatformMBeanServer();
			Object bean = ManagementFactory.newPlatformMXBeanProxy(server, HOTSPOT_BEAN_NAME, clazz);
			return bean;
		}
		catch (RuntimeException re)
		{
			throw re;
		}
		catch (Exception exp)
		{
			throw new RuntimeException(exp);
		}
	}

	public static void main(String[] args)
	{
		// default heap dump file name
		String fileName = "D:\\heap.bin";
		// by default dump only the live objects
		boolean live = true;

		// simple command line options
		switch (args.length)
		{
		case 2:
			live = args[1].equals("true");
		case 1:
			fileName = args[0];
		}

		// dump the heap
		dumpHeap(fileName, live);
	}
	
//	// This is the name of the HotSpot Diagnostic MBean
//	private static final String						HOTSPOT_BEAN_NAME = "com.sun.management:type=HotSpotDiagnostic";
//
//	// field to store the hotspot diagnostic MBean
//	private static volatile HotSpotDiagnosticMXBean	hotspotMBean;
//
//	 /**
//	  * Call this method from your application whenever you
//	  * want to dump the heap snapshot into a file.
//	  *
//	  * @param fileName name of the heap dump file
//	  * @param live flag that tells whether to dump
//	  * only the live objects
//	  */
//	static void dumpHeap(String fileName, boolean live)
//	{
//		// initialize hotspot diagnostic MBean
//		initHotspotMBean();
//		try
//		{
//			hotspotMBean.dumpHeap(fileName, live);
//		}
//		catch (RuntimeException re)
//		{
//			throw re;
//		}
//		catch (Exception exp)
//		{
//			throw new RuntimeException(exp);
//		}
//	}
//
//	// initialize the hotspot diagnostic MBean field
//	private static void initHotspotMBean()
//	{
//		if ( hotspotMBean == null )
//		{
//			synchronized (HeapDump.class)
//			{
//				if ( hotspotMBean == null )
//				{
//					hotspotMBean = getHotspotMBean();
//				}
//			}
//		}
//	}
//
//	// get the hotspot diagnostic MBean from the
//	// platform MBean server
//	private static HotSpotDiagnosticMXBean getHotspotMBean()
//	{
//		try
//		{
//			MBeanServer server = ManagementFactory.getPlatformMBeanServer();
//			HotSpotDiagnosticMXBean bean = ManagementFactory.newPlatformMXBeanProxy(server, HOTSPOT_BEAN_NAME, HotSpotDiagnosticMXBean.class);
//			return bean;
//		}
//		catch (RuntimeException re)
//		{
//			throw re;
//		}
//		catch (Exception exp)
//		{
//			throw new RuntimeException(exp);
//		}
//	}
//
//	public static void main(String[] args)
//	{
//		// default heap dump file name
//		String fileName = "heap.bin";
//		
//		// by default dump only the live objects
//		boolean live = true;
//		
//		// simple command line options
//		switch (args.length)
//		{
//		case 2:
//			live = args[1].equals("true");
//		case 1:
//			fileName = args[0];
//		}
//		
//		// dump the heap
//		dumpHeap(fileName, live);
//	}

}
