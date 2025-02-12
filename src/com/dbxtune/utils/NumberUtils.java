/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
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
package com.dbxtune.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.LongAdder;

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
			Long l = Long.valueOf(asStr);
			if (l > Integer.MAX_VALUE)
				return l;
			else
				return Integer.valueOf(l.intValue());
		}
		// Handle longer values than Integer.MAX_VALUE
		else if (asStr.length() > 10)  // Integer.MAX_VALUE == 2147483647 ... "2147483647".length() == 10
		{
			return Long.valueOf(asStr);
		}

		return Integer.valueOf(asStr);
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

	public static boolean isNumeric(String strNum, boolean stripCommas, boolean stripSpaces)
	{
		if (strNum == null)
			return false;

		// Number Input string is formated with "," on every thousand for readability... so remove that
		if (stripCommas)
			strNum = strNum.replace(",", "").trim();

		// Number Input string is formated with " " on every thousand for readability... so remove that
		if (stripSpaces)
			strNum = strNum.replace(" ", "").trim();
		
		try 
		{
			Double.parseDouble(strNum);
		}
		catch (NumberFormatException nfe)
		{
			return false;
		}
		return true;
	}

	/**
	 * Compare 2 number objects... The value, do not care about data types
	 * <br>
	 * Simply converts them into a double and do compare 
	 *
	 * @param n1
	 * @param n2
	 * 
	 * @return
	 */
	public static boolean compareValues(Number n1, Number n2)
	{
		if (n1 == null && n2 == null) return true;
		if (n1 == null || n2 == null) return false;
		
		// FIXME: Possibly do this in a more secure way... Not sure if BigDecimal etc may be converted *CORRECTLY* to a double value 
		//        Isn't there another utility that already does this...

		double d1 = n1.doubleValue();
		double d2 = n2.doubleValue();

		return Double.compare(d1, d2) == 0;
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
	    return Double.valueOf( bd.doubleValue() );

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

	/**
	 * Round a Double value at some decimals, and return as BigDecimal
	 * @param val
	 * @param places
	 */
	public static BigDecimal roundAsBigDecimal(Double value, int places)
	{
		if (value == null)
			return null;
			
		if (places < 0) 
			throw new IllegalArgumentException();

		BigDecimal bd = new BigDecimal(value);
		bd = bd.setScale(places, RoundingMode.HALF_UP);
	    return bd;
	}

	/**
	 * Round a double value at some decimals, and return as BigDecimal
	 * @param val
	 * @param places
	 */
	public static BigDecimal roundAsBigDecimal(double value, int places)
	{
		if (places < 0) 
			throw new IllegalArgumentException();

		BigDecimal bd = new BigDecimal(value);
		bd = bd.setScale(places, RoundingMode.HALF_UP);
	    return bd;
	}

	/** Calculates bytes to KB, with # decimals */ 
	public static double toKb(long bytes, int roundingDecimals)
	{
		return round( bytes / 1024.0, roundingDecimals);
	}

	/** Calculates bytes to MB, with # decimals */ 
	public static double toMb(long bytes, int roundingDecimals)
	{
		return round( bytes / 1024.0 / 1024.0, roundingDecimals);
	}

	/** Calculates bytes to GB, with # decimals */ 
	public static double toGb(long bytes, int roundingDecimals)
	{
		return round( bytes / 1024.0 / 1024.0 / 1024.0, roundingDecimals);
	}

	/**
	 * Returns a value 'asValue' using the same data type as the input 'numberObj'
	 * 
	 * @param numberObj     a data type template /this is the data type we will create the new number 'asValue'
	 * @param asValue       The value you want to create (this is a integer)
	 * @return
	 */
	public static Number toNumberValue(Number numberObj, int asValue)
	{
		if (numberObj == null) throw new RuntimeException("toNumberValue() expects a 'numberObj' to decide what data type to return");

		// Well known data types
		if (numberObj instanceof Integer   ) return Integer   .valueOf(asValue);
		if (numberObj instanceof Long      ) return Long      .valueOf(asValue);
		if (numberObj instanceof Short     ) return Short     .valueOf(asValue + "");
		if (numberObj instanceof Double    ) return Double    .valueOf(asValue);
		if (numberObj instanceof BigDecimal) return BigDecimal.valueOf(asValue);
		if (numberObj instanceof Float     ) return Float     .valueOf(asValue);
		if (numberObj instanceof Byte      ) return Byte      .valueOf(asValue + "");

		// Not that Well known data types
//		if (numberObj instanceof BigInteger       ) return new BigInteger().add(asValue);
		if (numberObj instanceof AtomicInteger    ) return new AtomicInteger    (asValue);
		if (numberObj instanceof AtomicLong       ) return new AtomicLong       (asValue);
		if (numberObj instanceof LongAdder        ) return new LongAdder        ();
//		if (numberObj instanceof LongAccumulator  ) return new LongAccumulator  ();
		if (numberObj instanceof DoubleAdder      ) return new DoubleAdder      ();
//		if (numberObj instanceof DoubleAccumulator) return new DoubleAccumulator();

		throw new RuntimeException("toNumberValue() numberObj=" + numberObj + " is of unhandled class " + numberObj.getClass().getName());
	}
}
