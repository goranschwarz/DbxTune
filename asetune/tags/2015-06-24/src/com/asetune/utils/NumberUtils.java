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

		if (asStr.length() > 10)  // Integer.MAX_VALUE == 2147483647 ... "2147483647".length() == 10
		{
			return new Long(asStr);
		}

		return new Integer(asStr);
	}

}
