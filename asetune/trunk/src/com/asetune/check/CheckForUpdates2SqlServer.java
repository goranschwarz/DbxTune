package com.asetune.check;

import java.util.List;

public class CheckForUpdates2SqlServer extends CheckForUpdates2
{

	@Override
	public QueryString createCheckForUpdate(Object... params)
	{
		System.out.println("NOT_YET_IMPLEMENTED: createCheckForUpdate()");
		return null;
	}

	@Override
	public QueryString createSendConnectInfo(Object... params)
	{
		System.out.println("NOT_YET_IMPLEMENTED: createSendConnectInfo()");
		return null;
	}

	@Override
	public List<QueryString> createSendCounterUsageInfo(Object... params)
	{
		System.out.println("NOT_YET_IMPLEMENTED: createSendCounterUsageInfo()");
		return null;
	}

	@Override
	public List<QueryString> createSendMdaInfo(Object... params)
	{
		System.out.println("NOT_YET_IMPLEMENTED: createSendMdaInfo()");
		return null;
	}

	@Override
	public QueryString createSendUdcInfo(Object... params)
	{
		System.out.println("NOT_YET_IMPLEMENTED: createSendUdcInfo()");
		return null;
	}

	@Override
	public QueryString createSendLogInfo(Object... params)
	{
		System.out.println("NOT_YET_IMPLEMENTED: createSendLogInfo()");
		return null;
	}

}
