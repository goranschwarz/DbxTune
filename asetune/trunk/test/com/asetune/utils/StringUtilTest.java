package com.asetune.utils;

import static org.junit.Assert.*;

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
		assertEquals("",        StringUtil.removeLastNewLine(" \n\n") );
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

}
