/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
 * 
 * This file is part of DbxTune
 * DbxTune is a family of sub-products *Tune, hence the Dbx
 * Here are some of the tools: AseTune, IqTune, RsTune, RaxTune, HanaTune, 
 *          SqlServerTune, PostgresTune, MySqlTune, MariaDbTune, Db2Tune, ...
 * 
 * DbxTune is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.asetune.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

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

	/**
	 * Round a Double value at some decimals
	 * @param val
	 * @param places
	 */
	public static Double round(Double value, int places)
	{
		if (value == null)
			return null;
			
		if (places < 0) 
			throw new IllegalArgumentException();

		BigDecimal bd = new BigDecimal(value);
		bd = bd.setScale(places, RoundingMode.HALF_UP);
	    return new Double( bd.doubleValue() );

//	    return new BigDecimal( value ).setScale(places, RoundingMode.HALF_UP).doubleValue();
	}

	/**
	 * Round a double value at some decimals
	 * @param val
	 * @param places
	 */
	public static double round(double value, int places)
	{
		if (places < 0) 
			throw new IllegalArgumentException();

		BigDecimal bd = new BigDecimal(value);
		bd = bd.setScale(places, RoundingMode.HALF_UP);
	    return bd.doubleValue();

//	    return new BigDecimal( value ).setScale(places, RoundingMode.HALF_UP).doubleValue();
	}
}
