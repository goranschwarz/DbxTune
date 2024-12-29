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

import static org.junit.Assert.assertEquals;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

import com.dbxtune.utils.StringUtil;

public class StringUtilTest
{

//	@Test
//	public void test()
//	{
//		fail("A lot of stuff needs to be tested here... but not yet implemented");
//	}

	@Test
	public void testWordRespectQuotes()
	{
		String str = "This is a line 'with single quotes' and \"double quotes (') and a embedded singe quote.\" -end-";
		//            0    1  2 3    4                    5    6                                                7
		
		assertEquals("This"                                           , StringUtil.wordRespectQuotes(str, 0));
		assertEquals("is"                                             , StringUtil.wordRespectQuotes(str, 1));
		assertEquals("with single quotes"                             , StringUtil.wordRespectQuotes(str, 4));
		assertEquals("double quotes (') and a embedded singe quote."  , StringUtil.wordRespectQuotes(str, 6));
		assertEquals("-end-"                                          , StringUtil.wordRespectQuotes(str, 7));
	}

	@Test
	public void testCommaStrToList_noQuotes()
	{
		assertEquals(Arrays.asList("a", "b", "c"), StringUtil.commaStrToList("a,b,c") );
		assertEquals(Arrays.asList("a", "b", "c"), StringUtil.commaStrToList("a,b,c,") );
	}

	@Test
	public void testCommaStrToList_withQuotes()
	{
		assertEquals(Arrays.asList("a", "xx\"b,b\"xx", "c"), StringUtil.commaStrToList("a,xx\"b,b\"xx,c") );
		assertEquals(Arrays.asList("a", "xx\"b,b\"xx", "c"), StringUtil.commaStrToList("a, xx\"b,b\"xx, c ") );
	}

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
	public void testCvsToMap()
	{
		String mapStr = null;
		Map<String, String> map = null;

		// Simple
		mapStr = "aaa=111,bbb=222, ccc=333";
		map = new LinkedHashMap<>();
		map.put("aaa", "111");
		map.put("bbb", "222");
		map.put("ccc", "333");
		assertEquals(map, StringUtil.parseCommaStrToMap(mapStr));
//		System.out.println(map);

		// no value
		mapStr = "aaa,bbb, ccc";
		map = new LinkedHashMap<>();
		map.put("aaa", "");
		map.put("bbb", "");
		map.put("ccc", "");
		assertEquals(map, StringUtil.parseCommaStrToMap(mapStr));
//		System.out.println(map);

		// value and no value mix
		mapStr = "aaa,bbb=, ccc=vvv";
		map = new LinkedHashMap<>();
		map.put("aaa", "");
		map.put("bbb", "");
		map.put("ccc", "vvv");
		assertEquals(map, StringUtil.parseCommaStrToMap(mapStr));
//		System.out.println(map);
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
