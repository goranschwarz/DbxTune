package com.asetune.utils;

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
	}
}
