package com.asetune.sql.pipe;

import com.asetune.sql.SqlProgressDialog;

public abstract class PipeCommandAbstract
implements IPipeCommand
{
//	abstract public IPipeCommand parse(String input) throws PipeCommandException;

	protected String _cmdStr = null;
	protected String _sqlStr = null;
	
	public PipeCommandAbstract(String input, String sqlString)
	{
		_cmdStr = input;
		_sqlStr = sqlString;
	}

	@Override 
	public String getCmdStr()
	{
		return _cmdStr;
	}

	@Override 
	public String getSqlString()
	{
		return _sqlStr;
	}

	@Override abstract public String getConfig();
	
	@Override
	public void open() 
	throws Exception
	{
	}

	@Override
	public void doPipe(Object input) throws Exception
	{
	}

	@Override
	public void doEndPoint(Object input, SqlProgressDialog progressDialog) throws Exception
	{
	}

	@Override
	public void close()
	{
	}

	@Override
	public Object getEndPointResult(String type)
	{
		return null;
	}

}
