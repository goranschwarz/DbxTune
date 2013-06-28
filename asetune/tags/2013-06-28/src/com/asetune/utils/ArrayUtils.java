package com.asetune.utils;

public class ArrayUtils
{
	public static int indexOfArray(byte[] source, char[] match)
	{
		// sanity checks
		if ( source == null || match == null )
			return -1;
		if ( source.length == 0 || match.length == 0 )
			return -1;
		int ret = -1;
		int spos = 0;
		int mpos = 0;
		byte m = (byte)match[mpos];
		for (; spos < source.length; spos++)
		{
			if ( m == source[spos] )
			{
				// starting match
				if ( mpos == 0 )
					ret = spos;
				// finishing match
				else if ( mpos == match.length - 1 )
					return ret;

				mpos++;
				m = (byte)match[mpos];
			}
			else
			{
				ret = -1;
				mpos = 0;
				m = (byte)match[mpos];
			}
		}
		// If have looped the entire match[] then "ret" is valid, otherwise it's -1
		return (mpos == match.length) ? ret : -1;
	}

	public static int indexOfArray(char[] source, byte[] match)
	{
		// sanity checks
		if ( source == null || match == null )
			return -1;
		if ( source.length == 0 || match.length == 0 )
			return -1;
		int ret = -1;
		int spos = 0;
		int mpos = 0;
		byte m = match[mpos];
		for (; spos < source.length; spos++)
		{
			if ( m == source[spos] )
			{
				// starting match
				if ( mpos == 0 )
					ret = spos;
				// finishing match
				else if ( mpos == match.length - 1 )
					return ret;

				mpos++;
				m = match[mpos];
			}
			else
			{
				ret = -1;
				mpos = 0;
				m = match[mpos];
			}
		}
		// If have looped the entire match[] then "ret" is valid, otherwise it's -1
		return (mpos == match.length) ? ret : -1;
	}

	public static int indexOfArray(byte[] source, byte[] match)
	{
		// sanity checks
		if ( source == null || match == null )
			return -1;
		if ( source.length == 0 || match.length == 0 )
			return -1;
		int ret = -1;
		int spos = 0;
		int mpos = 0;
		byte m = match[mpos];
		for (; spos < source.length; spos++)
		{
			if ( m == source[spos] )
			{
				// starting match
				if ( mpos == 0 )
					ret = spos;
				// finishing match
				else if ( mpos == match.length - 1 )
					return ret;

				mpos++;
				m = match[mpos];
			}
			else
			{
				ret = -1;
				mpos = 0;
				m = match[mpos];
			}
		}
		// If have looped the entire match[] then "ret" is valid, otherwise it's -1
		return (mpos == match.length) ? ret : -1;
	}

	public static int indexOfArray(char[] source, char[] match)
	{
		// sanity checks
		if ( source == null || match == null )
			return -1;
		if ( source.length == 0 || match.length == 0 )
			return -1;
		int ret = -1;
		int spos = 0;
		int mpos = 0;
		char m = match[mpos];
		for (; spos < source.length; spos++)
		{
			if ( m == source[spos] )
			{
				// starting match
				if ( mpos == 0 )
					ret = spos;
				// finishing match
				else if ( mpos == match.length - 1 )
					return ret;

				mpos++;
				m = match[mpos];
			}
			else
			{
				ret = -1;
				mpos = 0;
				m = match[mpos];
			}
		}
		// If have looped the entire match[] then "ret" is valid, otherwise it's -1
		return (mpos == match.length) ? ret : -1;
	}

	public static void main(String[] args)
	{
		byte[] b = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 1, 2, 2 };
		char[] c = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 1, 2, 2 };

		System.out.println("test-1.1 : byte[], byte[]: " + (indexOfArray(b, new byte[] { 1, 2, 3 }) ==  0 ? "OK" : "FAIL"));
		System.out.println("test-1.2 : byte[], byte[]: " + (indexOfArray(b, new byte[] { 2, 3, 4 }) ==  1 ? "OK" : "FAIL"));
		System.out.println("test-1.3 : byte[], byte[]: " + (indexOfArray(b, new byte[] { 1, 2, 2 }) ==  9 ? "OK" : "FAIL"));
		System.out.println("test-1.4 : byte[], byte[]: " + (indexOfArray(b, new byte[] { 0, 1, 2 }) == -1 ? "OK" : "FAIL"));
		System.out.println("test-1.5 : byte[], byte[]: " + (indexOfArray(b, new byte[] { 2, 2, 2 }) == -1 ? "OK" : "FAIL"));
		System.out.println();

		System.out.println("test-2.1 : byte[], char[]: " + (indexOfArray(b, new char[] { 1, 2, 3 }) ==  0 ? "OK" : "FAIL"));
		System.out.println("test-2.2 : byte[], char[]: " + (indexOfArray(b, new char[] { 2, 3, 4 }) ==  1 ? "OK" : "FAIL"));
		System.out.println("test-2.3 : byte[], char[]: " + (indexOfArray(b, new char[] { 1, 2, 2 }) ==  9 ? "OK" : "FAIL"));
		System.out.println("test-2.4 : byte[], char[]: " + (indexOfArray(b, new char[] { 0, 1, 2 }) == -1 ? "OK" : "FAIL"));
		System.out.println("test-2.5 : byte[], char[]: " + (indexOfArray(b, new char[] { 2, 2, 2 }) == -1 ? "OK" : "FAIL"));
		System.out.println();

		System.out.println("test-3.1 : char[], byte[]: " + (indexOfArray(c, new byte[] { 1, 2, 3 }) ==  0 ? "OK" : "FAIL"));
		System.out.println("test-3.2 : char[], byte[]: " + (indexOfArray(c, new byte[] { 2, 3, 4 }) ==  1 ? "OK" : "FAIL"));
		System.out.println("test-3.3 : char[], byte[]: " + (indexOfArray(c, new byte[] { 1, 2, 2 }) ==  9 ? "OK" : "FAIL"));
		System.out.println("test-3.4 : char[], byte[]: " + (indexOfArray(c, new byte[] { 0, 1, 2 }) == -1 ? "OK" : "FAIL"));
		System.out.println("test-3.5 : char[], byte[]: " + (indexOfArray(c, new byte[] { 2, 2, 2 }) == -1 ? "OK" : "FAIL"));
		System.out.println();

		System.out.println("test-4.1 : char[], char[]: " + (indexOfArray(c, new char[] { 1, 2, 3 }) ==  0 ? "OK" : "FAIL"));
		System.out.println("test-4.2 : char[], char[]: " + (indexOfArray(c, new char[] { 2, 3, 4 }) ==  1 ? "OK" : "FAIL"));
		System.out.println("test-4.3 : char[], char[]: " + (indexOfArray(c, new char[] { 1, 2, 2 }) ==  9 ? "OK" : "FAIL"));
		System.out.println("test-4.4 : char[], char[]: " + (indexOfArray(c, new char[] { 0, 1, 2 }) == -1 ? "OK" : "FAIL"));
		System.out.println("test-5.5 : char[], char[]: " + (indexOfArray(c, new char[] { 2, 2, 2 }) == -1 ? "OK" : "FAIL"));
		System.out.println();
	}
}
