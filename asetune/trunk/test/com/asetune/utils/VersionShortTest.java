package com.asetune.utils;

import static org.junit.Assert.*;

import org.junit.Test;

public class VersionShortTest
{

	@Test
	public void test()
	{
		// Test "raw" integers and generated integers
		assertEquals(100101,  VersionShort.toInt(10, 1, 1));
		assertEquals( 10000,  VersionShort.toInt( 1, 0, 0));

		// Test generated-integers  and  parsed version str
		assertEquals(VersionShort.toInt(10, 1, 1), VersionShort.parse("10.1.1"));
		assertEquals(VersionShort.toInt(10, 1, 0), VersionShort.parse("10.1"));
		assertEquals(VersionShort.toInt(10, 1, 1), VersionShort.parse("xxx 10.1.1 xxx"));

		// test "raw" version str  and  generated ver str from integer
		assertEquals( "1.23.45", VersionShort.toStr(12345));
		assertEquals( "3.3.10",  VersionShort.toStr(30310));
	}
}
