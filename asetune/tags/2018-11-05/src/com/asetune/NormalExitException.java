package com.asetune;

public class NormalExitException
extends Exception
{
	private static final long serialVersionUID = 1L;
	public NormalExitException()
	{
		super();
	}
	public NormalExitException(String msg)
	{
		super(msg);
	}
}
