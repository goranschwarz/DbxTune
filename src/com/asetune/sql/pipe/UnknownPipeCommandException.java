package com.asetune.sql.pipe;

public class UnknownPipeCommandException 
extends Exception
{
	private static final long serialVersionUID = 1L;

	public UnknownPipeCommandException(String msg)
	{
		super(msg);
	}
}

