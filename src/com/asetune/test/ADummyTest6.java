/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
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
package com.asetune.test;

import com.asetune.utils.StringUtil;

public class ADummyTest6
{

	public static void dummyM1() { dummyM2(); }
	public static void dummyM2() { dummyM3(); }
	public static void dummyM3() { dummyM4(); }
	public static void dummyM4() { dummyM5(); }
	public static void dummyM5() { dummyM6(); }
	public static void dummyM6() { dummyWork(); }
	
	public static void dummyWork()
	{
		for (Thread th : Thread.getAllStackTraces().keySet()) 
		{
			System.out.println("XXX: isDaemon="+th.isDaemon()+", threadName=|"+th.getName()+"|, th.getClass().getName()=|"+th.getClass().getName()+"|.");

			ThreadGroup tg = th.getThreadGroup();
			boolean isSystem = tg == null ? false : "system".equalsIgnoreCase(tg.getName());
			String  tgName   = tg == null ? "-null-" : tg.getName();
					System.out.println("isSystem="+isSystem+", tgName='"+tgName+"'."); 
//			System.out.println("getThreadGroup="+th.getThreadGroup()); 
			System.out.println("Stacktrace for Thread '"+th.getName()+"'." 
					+ StringUtil.stackTraceToString(th.getStackTrace()));
		}
	}

	public static void main(String[] args)
	{
		Thread th;

		th = new Thread("Dummy-Thread-1")
		{
			@Override
			public void run() 
			{
				try { Thread.sleep(5000); }
				catch(InterruptedException ignore) {}
			};
		};
		th.setDaemon(true);
		th.start();
		
		th = new Thread("Dummy-Thread-2")
		{
			@Override
			public void run() 
			{
				try { Thread.sleep(5000); }
				catch(InterruptedException ignore) {}
			};
		};
		//th.setDaemon(true);
		th.start();
		
		th = new Thread("Dummy-Thread-3")
		{
			@Override
			public void run() 
			{
				try { Thread.sleep(5000); }
				catch(InterruptedException ignore) {}
			};
		};
		th.setDaemon(true);
		th.start();
		
		th = new Thread("Dummy-Thread-4")
		{
			@Override
			public void run() 
			{
				try { Thread.sleep(5000); }
				catch(InterruptedException ignore) {}
			};
		};
		//th.setDaemon(true);
		th.start();
		
		dummyM1();
	}

}
