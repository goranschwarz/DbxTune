package com.asetune.sql.pipe;

public abstract class PipeCommandAbstract
implements IPipeCommand
{
//	abstract public IPipeCommand parse(String input) throws PipeCommandException;

	protected String _cmdStr = null;
	
	public PipeCommandAbstract(String input)
	{
		_cmdStr = input;
	}

	@Override 
	public String getCmdStr()
	{
		return _cmdStr;
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
	public void doEndPoint(Object input) throws Exception
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
