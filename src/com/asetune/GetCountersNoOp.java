package com.asetune;


public class GetCountersNoOp extends GetCounters
{

	@Override
	public void init()
	{
//		long startTime = System.currentTimeMillis();
//		System.out.println("Initializing.");

		this.createCounters();

//		long execTime = System.currentTimeMillis() - startTime;
//		System.out.println("Done. It took "+TimeUtils.msToTimeStr(execTime));
	}

	@Override
	public void run()
	{
	}

	@Override
	public void shutdown()
	{
	}

}
