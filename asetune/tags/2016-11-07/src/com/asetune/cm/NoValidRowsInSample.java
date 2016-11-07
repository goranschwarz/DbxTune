package com.asetune.cm;

public class NoValidRowsInSample 
extends Exception
{
	private static final long serialVersionUID = 1L;

	public NoValidRowsInSample(String desc)
	{
		super(desc);
	}
}
