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

import static org.junit.Assert.assertEquals;

import java.nio.charset.Charset;

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




	@Test
	public void testUtf8Len()
	{
		Charset               utf8     = Charset.forName("UTF-8");
		AllCodepointsIterator iterator = new AllCodepointsIterator();
		while (iterator.hasNext())
		{
			int codepoint = iterator.next();
			String testStr = new String(Character.toChars(codepoint));
			int strLen   = testStr.length();
			int bytesLen = testStr.getBytes(utf8).length;
			int utf8Len  = StringUtil.utf8Length(testStr);

			//System.out.println("test[codepoint=" + codepoint + ", strLen=" + strLen + ", bytesLen=" + bytesLen + ", utf8Len=" + utf8Len + "]='" + testStr + "', name=" +  Character.getName(codepoint) + ", UniCode=" + String.format("U+%04X", codepoint));

			assertEquals(bytesLen, utf8Len);
		}
	}

	private static class AllCodepointsIterator
	{
		private static final int MAX             = 0x10FFFF; // see http://unicode.org/glossary/
		private static final int SURROGATE_FIRST = 0xD800;
		private static final int SURROGATE_LAST  = 0xDFFF;
		private int              codepoint       = 0;

		public boolean hasNext()
		{
			return codepoint < MAX;
		}

		public int next()
		{
			int ret = codepoint;
			codepoint = next(codepoint);
			return ret;
		}

		private int next(int codepoint)
		{
			while (codepoint++ < MAX)
			{
				if ( codepoint == SURROGATE_FIRST )
				{
					codepoint = SURROGATE_LAST + 1;
				}
				if ( !Character.isDefined(codepoint) )
				{
					continue;
				}
				return codepoint;
			}
			return MAX;
		}
	}

}
