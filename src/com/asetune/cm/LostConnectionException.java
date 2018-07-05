package com.asetune.cm;

/**
 * When we are not conected to DBMS anymore.
 * @author gorans
 *
 */
public class LostConnectionException 
extends Exception
{
	private static final long serialVersionUID = 1L;

	public LostConnectionException(String message, Exception cause)
	{
		super(message, cause);
	}
}
