package com.asetune.sql.pipe;

public abstract class PipeCommandAbstract
implements IPipeCommand
{
//	abstract public IPipeCommand parse(String input) throws PipeCommandException;

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
	public void doEndPoint(Object input) throws Exception
	{
	}

	@Override
	public void close()
	{
	}

}
