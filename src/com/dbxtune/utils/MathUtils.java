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

public class MathUtils
{

	/**
	 * is value 'val' near the 'baseVal'
	 * <p>
	 * near(10, 100, 120) will return false    <br>
	 * near(10, 100, 110) will return true     <br>
	 * near(10, 100, 90)  will return true     <br>
	 * 
	 * @param pct Percent of 'baseVal' to be considered as near
	 * @param baseVal The value to be used as a base for compare
	 * @param val the value to check if it's near the baseVal
	 * @return true if near
	 */
	public static boolean pctNear(int pct, int baseVal, int val)
	{
		int pctVal = (int) (baseVal * (pct/100.0));
		int baseLow  = baseVal - pctVal;
		int baseHigh = baseVal + pctVal;
		
		return (val >= baseLow && val <= baseHigh);
	}
	
	/**
	 * Round a Decimal into some decimals points, using BigDecimal.ROUND_HALF_EVEN
	 * 
	 * @param val      The Decimal value to round
	 * @param scale    To number of decimals
	 */
	public static Double round(Double val, int scale)
	{
		if (val == null)
			return null;
		
		BigDecimal bd = new BigDecimal( val ).setScale(scale, BigDecimal.ROUND_HALF_EVEN);
		return bd.doubleValue();
	}
	
	/**
	 * Round a Decimal into some decimals points, using BigDecimal.ROUND_HALF_EVEN
	 * 
	 * @param val      The Decimal value to round
	 * @param scale    To number of decimals
	 */
	public static BigDecimal roundToBigDecimal(Double val, int scale)
	{
		if (val == null)
			return null;
		
		return new BigDecimal( val ).setScale(scale, BigDecimal.ROUND_HALF_EVEN);
	}
	
	/**
	 * Round a Float into some decimals points, using BigDecimal.ROUND_HALF_EVEN
	 * 
	 * @param val      The Float value to round
	 * @param scale    To number of decimals
	 */
	public static Double round(Float val, int scale)
	{
		if (val == null)
			return null;
		
		BigDecimal bd = new BigDecimal( val ).setScale(scale, BigDecimal.ROUND_HALF_EVEN);
		return bd.doubleValue();
	}
	
	public static BigDecimal max(BigDecimal bd1, BigDecimal bd2)
	{
		if (bd1 == null && bd2 == null)
			return null;
		
		if (bd1 == null)
			return bd2;

		if (bd2 == null)
		    return bd1;

		return bd1.max(bd2);
	}

	public static void main(String[] args)
	{
		System.out.println("near(10, 100, 80 ) == false : " + pctNear(10, 100, 80));
		System.out.println("near(10, 100, 89 ) == false : " + pctNear(10, 100, 89));
		System.out.println("near(10, 100, 90 ) == true  : " + pctNear(10, 100, 90));
		System.out.println("near(10, 100, 91 ) == true  : " + pctNear(10, 100, 91));
		System.out.println("near(10, 100, 100) == true  : " + pctNear(10, 100, 100));
		System.out.println("near(10, 100, 109) == true  : " + pctNear(10, 100, 109));
		System.out.println("near(10, 100, 110) == true  : " + pctNear(10, 100, 110));
		System.out.println("near(10, 100, 111) == false : " + pctNear(10, 100, 111));
		System.out.println("near(10, 100, 120) == false : " + pctNear(10, 100, 120));

		System.out.println("round(1.1234d, 1) == 1.1 : " + round(1.1234d, 1));
		System.out.println("round(1.1234f, 1) == 1.1 : " + round(1.1234f, 1));
	}

}
