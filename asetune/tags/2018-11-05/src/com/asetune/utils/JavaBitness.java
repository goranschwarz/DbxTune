package com.asetune.utils;

/**
 * This class detects the Java-Version
 */
public class JavaBitness
{
	/**
	 * Returns the Version of Java.
	 */
	public static int getBitness()
	{
		String bitnessStr = System.getProperty("sun.arch.data.model");
		if (bitnessStr == null)
			return 0;

		try
		{
			return Integer.parseInt(bitnessStr);
		}
		catch (Throwable e)
		{
			// TODO: handle exception
		}

		return 0;
	}


	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		System.exit(getBitness());
	}
}
