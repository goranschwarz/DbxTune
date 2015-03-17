package com.asetune;

import java.sql.Connection;

import com.asetune.pcs.PersistContainer.HeaderInfo;

public class CounterControllerNoOp
extends CounterControllerAbstract
{

	public CounterControllerNoOp(boolean hasGui)
	{
		super(hasGui);
	}

	@Override
	public void init()
	{
	}

	@Override
	public void checkServerSpecifics()
	{
	}

	@Override
	public HeaderInfo createPcsHeaderInfo()
	{
		return null;
	}

	@Override
	public void initCounters(Connection conn, boolean hasGui, int srvVersion, boolean isClusterEnabled, int monTablesVersion) throws Exception
	{
	}

	@Override
	public void createCounters(boolean hasGui)
	{
	}

	@Override
	public String getServerTimeCmd()
	{
		return null;
	}

	@Override
	protected String getIsClosedSql()
	{
		return null;
	}
}
