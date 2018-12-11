package com.asetune;

import com.asetune.pcs.PersistContainer.HeaderInfo;
import com.asetune.sql.conn.DbxConnection;

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
//	public void initCounters(Connection conn, boolean hasGui, long srvVersion, boolean isClusterEnabled, long monTablesVersion) throws Exception
//	{
//	}
	public void initCounters(DbxConnection conn, boolean hasGui, long srvVersion, boolean isClusterEnabled, long monTablesVersion) throws Exception
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
