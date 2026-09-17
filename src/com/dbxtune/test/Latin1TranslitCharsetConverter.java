/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
 * 
 * This file is part of DbxTune
 * DbxTune is a family of sub-products *Tune, hence the Dbx
 * Here are some of the tools: AseTune, IqTune, RsTune, RaxTune, HanaTune, 
 *          SqlServerTune, PostgresTune, MySqlTune, MariaDbTune, Db2Tune, ...
 * 
 * DbxTune is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.dbxtune.test;

import java.io.CharConversionException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import com.sybase.jdbcx.CharsetConverter;

/*
    ###########################################################
    ## jConnect properties
    ###########################################################
    Properties props = new Properties();
    props.put("user", user);
    props.put("password", pwd);
    props.put("CHARSET", "iso_1");   // ASE name for ISO-8859-1; use "cp1252" if that's the server charset
    props.put("CHARSET_CONVERTER_CLASS", "com.dbxtune.test.Latin1TranslitCharsetConverter");
    
    Connection conn = DriverManager.getConnection("jdbc:sybase:Tds:host:5000/db", props);
    
    ###########################################################
    ## URL
    ###########################################################
    jdbc:sybase:Tds:host:5000/db?CHARSET=iso_1&CHARSET_CONVERTER_CLASS=com.dbxtune.test.Latin1TranslitCharsetConverter
    
    ###########################################################
    ## Note:
    ###########################################################
     * The converter applies to all text, including the SQL statement itself. Normally that's harmless, since SQL keywords are ASCII. Just be aware of it.
     * unichar/univarchar/unitext parameters may bypass the converter. When the server supports Unicode datatypes, jConnect may send those parameters as UTF-16. 
       Set DISABLE_UNICHAR_SENDING=true if you want everything to go through your converter.
     * Server-side alternative (no transliteration). You can connect with CHARSET=utf8 and let ASE convert to its own charset. 
       By default, unmappable characters then raise error 2402. With set char_convert on with no_error, ASE replaces them with ? instead. 
       That matches plain iconv without //TRANSLIT, so if you want ś → s, you still need the custom converter.
 */
public class Latin1TranslitCharsetConverter 
implements CharsetConverter 
{
	private Charset charset = Charset.forName("ISO-8859-1");

	// jConnect passes the Java encoding name mapped from the server charset
	// (e.g. ASE "iso_1" -> "ISO8859_1", "cp1252" -> "Cp1252")
	@Override
	public void setEncoding(String enc) throws UnsupportedEncodingException 
	{
		System.out.println(">>>>>>>>> DEBUG: Latin1TranslitCharsetConverter.setEncoding(enc=|" + enc + "|)");
		try 
		{
			charset = Charset.forName(enc);
		} 
		catch (Exception e) 
		{
			throw new UnsupportedEncodingException(enc);
		}
	}

	// Java -> server: transliterate first, then encode
	@Override
	public byte[] fromUnicode(String str) throws CharConversionException 
	{
		System.out.println("          DEBUG: Latin1TranslitCharsetConverter.fromUnicode(str=|" + str + "|) <<< |" + Latin1Translit.translit(str) + "|");
		return Latin1Translit.translit(str).getBytes(charset);
	}

	// server -> Java: plain decode
	@Override
	public String toUnicode(byte[] b) throws CharConversionException 
	{
//		System.out.println("          DEBUG: Latin1TranslitCharsetConverter.toUnicode(b=|" + b + "|) <<< |" + (new String(b, charset)) + "|");
		return new String(b, charset);
	}
}
