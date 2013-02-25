/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.utils;

public class MandatoryPropertyException 
extends Exception 
{
	private static final long serialVersionUID = -5443054597702638350L;

	public MandatoryPropertyException() 
	{
		super();
	}

	public MandatoryPropertyException(String arg0) 
	{
		super(arg0);
	}

	public MandatoryPropertyException(Throwable arg0) 
	{
		super(arg0);
	}

	public MandatoryPropertyException(String arg0, Throwable arg1) 
	{
		super(arg0, arg1);
	}

}
