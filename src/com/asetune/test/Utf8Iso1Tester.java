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
package com.asetune.test;

import java.io.File;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;

public class Utf8Iso1Tester
{

	public static void main(String[] args)
	{
		String filename = "C:/projects/MaxM/tmp/gorans_B5564210911.20190616.xml";
		
		File f = new File(filename);

		System.out.println("Filename: " + f);
		try
		{
			String content = FileUtils.readFileToString(f, StandardCharsets.UTF_8);
			int strlen = content.length();

			System.out.println("In-Content len: " + strlen + " bytes");
			System.out.println("In-Content len: " + String.format("%.1f", strlen/1024.0) + " KB");
			System.out.println("In-Content len: " + String.format("%.1f", strlen/1024.0/1024.0) + " MB");

			String out = utf8TranslateOobIso1Chars(content);

			int outlen = out.length();
			System.out.println("Out-Content len: " + outlen + " bytes");
			
//			for (int c=0; c<strlen; c++)
//			{
//				char ch = content.charAt(c);
//				int intVal = ch;
////				if (ch >= 128)
//				if (ch >= 256)
//				{
//					System.out.println("Pos=" + c + ", Character '" + ch + "' is above 256... int value: " + String.format("0x%x = %d", intVal, intVal));
//				}
//			}
			
			System.out.println("--- end ---");
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	public static String utf8TranslateOobIso1Chars(String str)
	{
		if (str == null)
			return null;

		// Length of the input string
		int len = str.length();

		// Create a destination, where we will add chars (converted or not-converted)
		StringBuilder sb = new StringBuilder(len);

		// Loop the string, and add chars to the output buffer
		for (int c=0; c<len; c++)
		{
			char inChar = str.charAt(c);
			int intVal = inChar;

			char outChar = inChar;
			
			// Translate some specific chars (which might still be in Windows cp1252 charset) 
			if (intVal >= 256)
			{
				switch (inChar)
				{
				case 0x201a: { outChar = '\''; reportOobChar(c, inChar, outChar); break; }  //  unicode 0x201a --> |'| - 130 -- single Low-9 Quotation Mark 
				case 0x201e: { outChar = '"';  reportOobChar(c, inChar, outChar); break; }  //  unicode 0x201e --> |"| - 132 -- Double Low-9 Quotation Mark 
				case 0x2018: { outChar = '\''; reportOobChar(c, inChar, outChar); break; }  //  unicode 0x2018 --> |'| - 145 -- left single quotation mark  
				case 0x2019: { outChar = '\''; reportOobChar(c, inChar, outChar); break; }  //  unicode 0x2019 --> |'| - 146 -- right single quotation mark 
				
				case 0x201c: { outChar = '"';  reportOobChar(c, inChar, outChar); break; }  //  unicode 0x201c --> |"| - 147 -- Left Double Quotation Mark  	
				case 0x201d: { outChar = '"';  reportOobChar(c, inChar, outChar); break; }  //  unicode 0x201d --> |"| - 148 -- Right Double Quotation Mark 	
				
				case 0x20ac: { outChar = 'E';  reportOobChar(c, inChar, outChar); break; }  //  unicode 0x20ac --> || - 128 -- Euro Sign "&euro;"
				case 0x0192: { outChar = 'f';  reportOobChar(c, inChar, outChar); break; }  //  unicode 0x0192 --> || - 131 -- Latin Small Letter F With Hook "&fnof;"
				case 0x2026: { outChar = '.';  reportOobChar(c, inChar, outChar); break; }  //  unicode 0x2026 --> || - 133 -- Horizontal Ellipsis "..."
				case 0x2020: { outChar = '*';  reportOobChar(c, inChar, outChar); break; }  //  unicode 0x2020 --> || - 134 -- Dagger "&dagger;"
				case 0x2021: { outChar = '*';  reportOobChar(c, inChar, outChar); break; }  //  unicode 0x2021 --> || - 135 -- Double Dagger "&Dagger;"
				case 0x02c6: { outChar = 'o';  reportOobChar(c, inChar, outChar); break; }  //  unicode 0x02c6 --> || - 136 -- Modifier Letter Circumflex Accent "&circ;"
				case 0x2030: { outChar = '%';  reportOobChar(c, inChar, outChar); break; }  //  unicode 0x2030 --> || - 137 -- Per Mille Sign "&permil;"
				case 0x0160: { outChar = 'S';  reportOobChar(c, inChar, outChar); break; }  //  unicode 0x0160 --> || - 138 -- Latin Capital Letter S With Caron "&Scaron;"
				case 0x2039: { outChar = '<';  reportOobChar(c, inChar, outChar); break; }  //  unicode 0x2039 --> || - 139 -- Single Left-Pointing Angle Quotation Mark "&lsaquo;"
				case 0x0152: { outChar = 'O';  reportOobChar(c, inChar, outChar); break; }  //  unicode 0x0152 --> || - 140 -- Latin Capital Ligature Oe "Oe"
				case 0x017d: { outChar = 'Z';  reportOobChar(c, inChar, outChar); break; }  //  unicode 0x017d --> || - 142 -- Latin Capital Letter Z With Caron "&#x17d;"
				case 0x2022: { outChar = '*';  reportOobChar(c, inChar, outChar); break; }  //  unicode 0x2022 --> || - 149 -- Bullet "*"
				case 0x2013: { outChar = '-';  reportOobChar(c, inChar, outChar); break; }  //  unicode 0x2013 --> || - 150 -- En Dash "-"
				case 0x2014: { outChar = '-';  reportOobChar(c, inChar, outChar); break; }  //  unicode 0x2014 --> || - 151 -- Em Dash "--"
				case 0x02dc: { outChar = '~';  reportOobChar(c, inChar, outChar); break; }  //  unicode 0x02dc --> || - 152 -- Small Tilde "~"
				case 0x2122: { outChar = 't';  reportOobChar(c, inChar, outChar); break; }  //  unicode 0x2122 --> || - 153 -- Trade Mark Sign "&trade;"
				case 0x0161: { outChar = 's';  reportOobChar(c, inChar, outChar); break; }  //  unicode 0x0161 --> || - 154 -- Latin Small Letter S With Caron
				case 0x203a: { outChar = '>';  reportOobChar(c, inChar, outChar); break; }  //  unicode 0x203a --> || - 155 -- Single Right-Pointing Angle Quotation Mark "&rsaquo;"
				case 0x0153: { outChar = 'o';  reportOobChar(c, inChar, outChar); break; }  //  unicode 0x0153 --> || - 156 -- Latin Small Ligature Oe "&oelig;"
				default:     { outChar = '?';  reportOobChar(c, inChar, outChar); break; }  // ?????????????????
				}
			}
//			else if (intVal >= 128) // cp-1252  (But those values should NOT be in utf-8 128-256 range, only in Windows cp1252)
//			{
//				switch (inChar)
//				{
//				case 0x80: { outChar = 'E';  reportOobChar(c, inChar, outChar); break; } // cp-1252 - EURO SIGN
//				case 0x82: { outChar = '\''; reportOobChar(c, inChar, outChar); break; } // cp-1252 - SINGLE LOW-9 QUOTATION MARK
//				case 0x83: { outChar = 'f';  reportOobChar(c, inChar, outChar); break; } // cp-1252 - LATIN SMALL LETTER F WITH HOOK
//				case 0x84: { outChar = '"';  reportOobChar(c, inChar, outChar); break; } // cp-1252 - DOUBLE LOW-9 QUOTATION MARK
//				case 0x85: { outChar = '.';  reportOobChar(c, inChar, outChar); break; } // cp-1252 - HORIZONTAL ELLIPSIS
//				case 0x86: { outChar = '*';  reportOobChar(c, inChar, outChar); break; } // cp-1252 - DAGGER
//				case 0x87: { outChar = '*';  reportOobChar(c, inChar, outChar); break; } // cp-1252 - DOUBLE DAGGER
//				case 0x88: { outChar = '^';  reportOobChar(c, inChar, outChar); break; } // cp-1252 - MODIFIER LETTER CIRCUMFLEX ACCENT
//				case 0x89: { outChar = '%';  reportOobChar(c, inChar, outChar); break; } // cp-1252 - PER MILLE SIGN
//				case 0x8a: { outChar = 'S';  reportOobChar(c, inChar, outChar); break; } // cp-1252 - LATIN CAPITAL LETTER S WITH CARON
//				case 0x8b: { outChar = '<';  reportOobChar(c, inChar, outChar); break; } // cp-1252 - SINGLE LEFT-POINTING ANGLE QUOTE
//				case 0x8c: { outChar = 'O';  reportOobChar(c, inChar, outChar); break; } // cp-1252 - LATIN CAPITAL LIGATURE OE
//				case 0x8e: { outChar = 'Z';  reportOobChar(c, inChar, outChar); break; } // cp-1252 - LATIN CAPITAL LETTER Z WITH CARON
//				case 0x91: { outChar = '\''; reportOobChar(c, inChar, outChar); break; } // cp-1252 - LEFT SINGLE QUOTATION MARK
//				case 0x92: { outChar = '\''; reportOobChar(c, inChar, outChar); break; } // cp-1252 - RIGHT SINGLE QUOTATION MARK
//				case 0x93: { outChar = '"';  reportOobChar(c, inChar, outChar); break; } // cp-1252 - LEFT DOUBLE QUOTATION MARK
//				case 0x94: { outChar = '"';  reportOobChar(c, inChar, outChar); break; } // cp-1252 - RIGHT DOUBLE QUOTATION MARK
//				case 0x95: { outChar = '*';  reportOobChar(c, inChar, outChar); break; } // cp-1252 - BULLET
//				case 0x96: { outChar = '-';  reportOobChar(c, inChar, outChar); break; } // cp-1252 - EN DASH
//				case 0x97: { outChar = '-';  reportOobChar(c, inChar, outChar); break; } // cp-1252 - EM DASH
//				case 0x98: { outChar = '~';  reportOobChar(c, inChar, outChar); break; } // cp-1252 - SMALL TILDE
//				case 0x99: { outChar = 'T';  reportOobChar(c, inChar, outChar); break; } // cp-1252 - TRADE MARK SIGN
//				case 0x9a: { outChar = 's';  reportOobChar(c, inChar, outChar); break; } // cp-1252 - LATIN SMALL LETTER S WITH CARON
//				case 0x9b: { outChar = '>';  reportOobChar(c, inChar, outChar); break; } // cp-1252 - SINGLE RIGHT-POINTING ANGLE QUOTE
//				case 0x9c: { outChar = 'o';  reportOobChar(c, inChar, outChar); break; } // cp-1252 - LATIN SMALL LIGATURE OE
//				case 0x9e: { outChar = 'z';  reportOobChar(c, inChar, outChar); break; } // cp-1252 - LATIN SMALL LETTER Z WITH CARON
//				case 0x9f: { outChar = 'Y';  reportOobChar(c, inChar, outChar); break; } // cp-1252 - LATIN CAPITAL LETTER Y WITH DIAERESIS
//				}
//			}
			
			sb.append(outChar);
		}

		return sb.toString();
	}
	public static void reportOobChar(int pos, char origin, char translatedTo)
	{
		int intVal = origin;
		System.out.println("At position " + pos + ", origin char '" + origin + "' (" + String.format("hex: 0x%x, dec: %d", intVal, intVal) + ") was translated into '" + translatedTo + "'.");
	}

// Comparing Characters in Windows-1252, ISO-8859-1, ISO-8859-15
// https://www.i18nqa.com/debug/table-iso8859-1-vs-windows-1252.html

//
// have a look at:	https://github.com/chrismattmann/solrcene/blob/master/modules/analysis/common/src/java/org/apache/lucene/analysis/miscellaneous/ASCIIFoldingFilter.java
	
		
//	// map incompatible non-ISO characters into plausible substitutes
//
//	"\\u0082", "\'" );		// unicode 0x201a  --> 130 -- single Low-9 Quotation Mark 
//	"\\u0084", "\"" );		// unicode 0x201e  --> 132 -- Double Low-9 Quotation Mark 
//	"\\u0091", "\'" );		// unicode 0x2018  --> 145 -- left single quotation mark  
//	"\\u0092", "\'" );		// unicode 0x2019  --> 146 -- right single quotation mark 
//
//	"\\u0093", "\"" );		// unicode 0x201c  --> 147 -- Left Double Quotation Mark  	
//	"\\u0094", "\"" );		// unicode 0x201d  --> 148 -- Right Double Quotation Mark 	
//
//	"\\u0080","&euro;");	// unicode 0x20ac  --> 128 -- Euro Sign "&euro;"
//	"\\u0083","&fnof;");	// unicode 0x0192  --> 131 -- Latin Small Letter F With Hook "&fnof;"
//	"\\u0085","...");		// unicode 0x2026  --> 133 -- Horizontal Ellipsis "..."
//	"\\u0086","&dagger;");	// unicode 0x2020  --> 134 -- Dagger "&dagger;"
//	"\\u0087","&Dagger;");	// unicode 0x2021  --> 135 -- Double Dagger "&Dagger;"
//	"\\u0088","&circ;");	// unicode 0x02c6  --> 136 -- Modifier Letter Circumflex Accent "&circ;"
//	"\\u0089","&permil;");	// unicode 0x2030  --> 137 -- Per Mille Sign "&permil;"
//	"\\u008a","&Scaron;");	// unicode 0x0160  --> 138 -- Latin Capital Letter S With Caron "&Scaron;"
//	"\\u008b","&lsaquo;");	// unicode 0x2039  --> 139 -- Single Left-Pointing Angle Quotation Mark "&lsaquo;"
//	"\\u008c","&OElig;");	// unicode 0x0152  --> 140 -- Latin Capital Ligature Oe "Oe"
//	"\\u008e","&#x17d;");	// unicode 0x017d  --> 142 -- Latin Capital Letter Z With Caron "&#x17d;"
//	"\\u0095","&bull;");	// unicode 0x2022  --> 149 -- Bullet "*"
//	"\\u0096","-");			// unicode 0x2013  --> 150 -- En Dash "-"
//	"\\u0097","--");		// unicode 0x2014  --> 151 -- Em Dash "--"
//	"\\u0098","~");			// unicode 0x02dc  --> 152 -- Small Tilde "~"
//	"\\u0099","&trade;");	// unicode 0x2122  --> 153 -- Trade Mark Sign "&trade;"
//	"\\u009a","&scaron;");	// unicode 0x0161  --> 154 -- Latin Small Letter S With Caron
//	"\\u009b","&rsaquo;");	// unicode 0x203a  --> 155 -- Single Right-Pointing Angle Quotation Mark "&rsaquo;"
//	"\\u009c","&oelig;");	// unicode 0x0153  --> 156 -- Latin Small Ligature Oe "&oelig;"
	
	
	
	
	// map incompatible non-ISO characters into plausible substitutes

//	"\\u0082", "\'" );		// case 0x201a: { outChar = '\''; reportOobChar(c, inChar, outChar); break; }  //  unicode 0x201a --> |'| - 130 -- single Low-9 Quotation Mark 
//	"\\u0084", "\"" );		// case 0x201e: { outChar = '"';  reportOobChar(c, inChar, outChar); break; }  //  unicode 0x201e --> |"| - 132 -- Double Low-9 Quotation Mark 
//	"\\u0091", "\'" );		// case 0x2018: { outChar = '\''; reportOobChar(c, inChar, outChar); break; }  //  unicode 0x2018 --> |'| - 145 -- left single quotation mark  
//	"\\u0092", "\'" );		// case 0x2019: { outChar = '\''; reportOobChar(c, inChar, outChar); break; }  //  unicode 0x2019 --> |'| - 146 -- right single quotation mark 
//
//	"\\u0093", "\"" );		// case 0x201c: { outChar = '"';  reportOobChar(c, inChar, outChar); break; }  //  unicode 0x201c --> |"| - 147 -- Left Double Quotation Mark  	
//	"\\u0094", "\"" );		// case 0x201d: { outChar = '"';  reportOobChar(c, inChar, outChar); break; }  //  unicode 0x201d --> |"| - 148 -- Right Double Quotation Mark 	
//
//	"\\u0080","&euro;");	// case 0x20ac: { outChar = 'E';  reportOobChar(c, inChar, outChar); break; }  //  unicode 0x20ac --> || - 128 -- Euro Sign "&euro;"
//	"\\u0083","&fnof;");	// case 0x0192: { outChar = 'f';  reportOobChar(c, inChar, outChar); break; }  //  unicode 0x0192 --> || - 131 -- Latin Small Letter F With Hook "&fnof;"
//	"\\u0085","...");		// case 0x2026: { outChar = '.'; reportOobChar(c, inChar, outChar); break; }  //  unicode 0x2026 --> || - 133 -- Horizontal Ellipsis "..."
//	"\\u0086","&dagger;");	// case 0x2020: { outChar = '*'; reportOobChar(c, inChar, outChar); break; }  //  unicode 0x2020 --> || - 134 -- Dagger "&dagger;"
//	"\\u0087","&Dagger;");	// case 0x2021: { outChar = '*'; reportOobChar(c, inChar, outChar); break; }  //  unicode 0x2021 --> || - 135 -- Double Dagger "&Dagger;"
//	"\\u0088","&circ;");	// case 0x02c6: { outChar = 'o'; reportOobChar(c, inChar, outChar); break; }  //  unicode 0x02c6 --> || - 136 -- Modifier Letter Circumflex Accent "&circ;"
//	"\\u0089","&permil;");	// case 0x2030: { outChar = '%'; reportOobChar(c, inChar, outChar); break; }  //  unicode 0x2030 --> || - 137 -- Per Mille Sign "&permil;"
//	"\\u008a","&Scaron;");	// case 0x0160: { outChar = 'S'; reportOobChar(c, inChar, outChar); break; }  //  unicode 0x0160 --> || - 138 -- Latin Capital Letter S With Caron "&Scaron;"
//	"\\u008b","&lsaquo;");	// case 0x2039: { outChar = '<'; reportOobChar(c, inChar, outChar); break; }  //  unicode 0x2039 --> || - 139 -- Single Left-Pointing Angle Quotation Mark "&lsaquo;"
//	"\\u008c","&OElig;");	// case 0x0152: { outChar = 'O'; reportOobChar(c, inChar, outChar); break; }  //  unicode 0x0152 --> || - 140 -- Latin Capital Ligature Oe "Oe"
//	"\\u008e","&#x17d;");	// case 0x017d: { outChar = 'Z'; reportOobChar(c, inChar, outChar); break; }  //  unicode 0x017d --> || - 142 -- Latin Capital Letter Z With Caron "&#x17d;"
//	"\\u0095","&bull;");	// case 0x2022: { outChar = '*'; reportOobChar(c, inChar, outChar); break; }  //  unicode 0x2022 --> || - 149 -- Bullet "*"
//	"\\u0096","-");			// case 0x2013: { outChar = '-'; reportOobChar(c, inChar, outChar); break; }  //  unicode 0x2013 --> || - 150 -- En Dash "-"
//	"\\u0097","--");		// case 0x2014: { outChar = '-'; reportOobChar(c, inChar, outChar); break; }  //  unicode 0x2014 --> || - 151 -- Em Dash "--"
//	"\\u0098","~");			// case 0x02dc: { outChar = '~'; reportOobChar(c, inChar, outChar); break; }  //  unicode 0x02dc --> || - 152 -- Small Tilde "~"
//	"\\u0099","&trade;");	// case 0x2122: { outChar = 't'; reportOobChar(c, inChar, outChar); break; }  //  unicode 0x2122 --> || - 153 -- Trade Mark Sign "&trade;"
//	"\\u009a","&scaron;");	// case 0x0161: { outChar = 's'; reportOobChar(c, inChar, outChar); break; }  //  unicode 0x0161 --> || - 154 -- Latin Small Letter S With Caron
//	"\\u009b","&rsaquo;");	// case 0x203a: { outChar = '>'; reportOobChar(c, inChar, outChar); break; }  //  unicode 0x203a --> || - 155 -- Single Right-Pointing Angle Quotation Mark "&rsaquo;"
//	"\\u009c","&oelig;");	// case 0x0153: { outChar = 'o'; reportOobChar(c, inChar, outChar); break; }  //  unicode 0x0153 --> || - 156 -- Latin Small Ligature Oe "&oelig;"


	
//    "\xe2\x82\xac" => "\x80", // EURO SIGN
//    "\xe2\x80\x9a" => "\x82", // SINGLE LOW-9 QUOTATION MARK
//    "\xc6\x92"     => "\x83", // LATIN SMALL LETTER F WITH HOOK
//    "\xe2\x80\x9e" => "\x84", // DOUBLE LOW-9 QUOTATION MARK
//    "\xe2\x80\xa6" => "\x85", // HORIZONTAL ELLIPSIS
//    "\xe2\x80\xa0" => "\x86", // DAGGER
//    "\xe2\x80\xa1" => "\x87", // DOUBLE DAGGER
//    "\xcb\x86"     => "\x88", // MODIFIER LETTER CIRCUMFLEX ACCENT
//    "\xe2\x80\xb0" => "\x89", // PER MILLE SIGN
//    "\xc5\xa0"     => "\x8a", // LATIN CAPITAL LETTER S WITH CARON
//    "\xe2\x80\xb9" => "\x8b", // SINGLE LEFT-POINTING ANGLE QUOTE
//    "\xc5\x92"     => "\x8c", // LATIN CAPITAL LIGATURE OE
//    "\xc5\xbd"     => "\x8e", // LATIN CAPITAL LETTER Z WITH CARON
//    "\xe2\x80\x98" => "\x91", // LEFT SINGLE QUOTATION MARK
//    "\xe2\x80\x99" => "\x92", // RIGHT SINGLE QUOTATION MARK
//    "\xe2\x80\x9c" => "\x93", // LEFT DOUBLE QUOTATION MARK
//    "\xe2\x80\x9d" => "\x94", // RIGHT DOUBLE QUOTATION MARK
//    "\xe2\x80\xa2" => "\x95", // BULLET
//    "\xe2\x80\x93" => "\x96", // EN DASH
//    "\xe2\x80\x94" => "\x97", // EM DASH
//    "\xcb\x9c"     => "\x98", // SMALL TILDE
//    "\xe2\x84\xa2" => "\x99", // TRADE MARK SIGN
//    "\xc5\xa1"     => "\x9a", // LATIN SMALL LETTER S WITH CARON
//    "\xe2\x80\xba" => "\x9b", // SINGLE RIGHT-POINTING ANGLE QUOTE
//    "\xc5\x93"     => "\x9c", // LATIN SMALL LIGATURE OE
//    "\xc5\xbe"     => "\x9e", // LATIN SMALL LETTER Z WITH CARON
//    "\xc5\xb8"     => "\x9f", // LATIN CAPITAL LETTER Y WITH DIAERESIS
	
	
}
