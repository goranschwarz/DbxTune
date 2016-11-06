package com.asetune.utils;

import java.math.BigDecimal;

public class NumberUtils
{
	/**
	 * Try to convert a Object to a Number
	 * 
	 * @param o The object trying to convert.
	 * @return a Number
	 * @throws NumberFormatException if it's null or can't be converted
	 */
	public static Number toNumber(Object o)
	{
		if (o == null)
			throw new NumberFormatException("Sorry can't convert a 'null' value to a Number.");

		if (o instanceof Number)
			return (Number) o;
		
		String asStr = o.toString().trim();
		if (asStr.indexOf(".") > 0)
		{
			return new BigDecimal(asStr);
		}

		// handle "number" that are still 10 char length but above 2147483647... between 2147483647 and 9999999999
		if (asStr.length() == 10)  // Integer.MAX_VALUE == 2147483647 ... "2147483647".length() == 10
		{
			Long l = new Long(asStr);
			if (l > Integer.MAX_VALUE)
				return l;
			else
				return new Integer(l.intValue());
		}
		// Handle longer values than Integer.MAX_VALUE
		else if (asStr.length() > 10)  // Integer.MAX_VALUE == 2147483647 ... "2147483647".length() == 10
		{
			return new Long(asStr);
		}

		return new Integer(asStr);
	}

	public static boolean isNumber(String str)
	{
		try
		{
			Long.parseLong(str);
			return true;
		}
		catch(NumberFormatException e)
		{
			return false;
		}
	}
}
