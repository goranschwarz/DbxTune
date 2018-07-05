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
