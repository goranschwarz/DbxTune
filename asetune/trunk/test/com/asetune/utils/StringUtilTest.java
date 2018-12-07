package com.asetune.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class StringUtilTest
{

//	@Test
//	public void test()
//	{
//		fail("A lot of stuff needs to be tested here... but not yet implemented");
//	}

	@Test
	public void testRemoveLastComma()
	{
		assertEquals("",        StringUtil.removeLastComma(",") );
		assertEquals("",        StringUtil.removeLastComma(", ") );
		assertEquals("",        StringUtil.removeLastComma(", \n") );
		assertEquals("",        StringUtil.removeLastComma(", \n") );
	}

	@Test
	public void testRemoveLastNewLine()
	{
		assertEquals("",        StringUtil.removeLastNewLine("\n") );
		assertEquals("",        StringUtil.removeLastNewLine("\n ") );
		assertEquals(" \n",     StringUtil.removeLastNewLine(" \n\n") ); // should this return "" or " \n" ????
	}

	@Test
	public void testRemoveSemicolonAtEnd()
	{
		assertEquals("",        StringUtil.removeSemicolonAtEnd(";") );
		assertEquals("",        StringUtil.removeSemicolonAtEnd("; ") );
		assertEquals("",        StringUtil.removeSemicolonAtEnd("; \n\n") );
		assertEquals("xx",      StringUtil.removeSemicolonAtEnd("xx; ") );
		assertEquals("xx",      StringUtil.removeSemicolonAtEnd("xx; \n\n") );
		assertEquals(" x",      StringUtil.removeSemicolonAtEnd(" x; ") );
		assertEquals(" x",      StringUtil.removeSemicolonAtEnd(" x; \n\n") );
	}

	@Test
	public void testIsNullOrBlankForAll()
	{
		assertEquals(true,        StringUtil.isNullOrBlankForAll(null) );
		assertEquals(true,        StringUtil.isNullOrBlankForAll(null, null) );
		assertEquals(true,        StringUtil.isNullOrBlankForAll("") );
		assertEquals(true,        StringUtil.isNullOrBlankForAll("", "") );
		assertEquals(false,       StringUtil.isNullOrBlankForAll("", "aValue") );
	}

	@Test
	public void testHasValueForAllForAll()
	{
		assertEquals(false,       StringUtil.hasValueForAll(null) );
		assertEquals(false,       StringUtil.hasValueForAll(null, null) );
		assertEquals(false,       StringUtil.hasValueForAll("") );
		assertEquals(false,       StringUtil.hasValueForAll("", "") );
		assertEquals(false,       StringUtil.hasValueForAll("", "aValue") );
		assertEquals(true,        StringUtil.hasValueForAll("aValue", "aValue") );
	}

}
