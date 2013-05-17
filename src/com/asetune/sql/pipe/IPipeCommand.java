package com.asetune.sql.pipe;

public interface IPipeCommand
{
	public String getConfig();
	
	public void open() throws Exception;
	public void doPipe(Object input) throws Exception;
	public void doEndPoint(Object input) throws Exception;
	public void close();
}
