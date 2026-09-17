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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.Map;

public class Latin1Translit
{
	// Characters that NFKD doesn't reduce to Latin-1 on its own
	private static final Map<Integer, String> MAP = Map.ofEntries
	(
		Map.entry(0x2018, "'"),     // ‘  LEFT SINGLE QUOTATION MARK
		Map.entry(0x2019, "'"),     // ’  RIGHT SINGLE QUOTATION MARK
		Map.entry(0x201A, "'"),     // ‚  SINGLE LOW-9 QUOTATION MARK
		Map.entry(0x201C, "\""),    // “  LEFT DOUBLE QUOTATION MARK
		Map.entry(0x201D, "\""),    // ”  RIGHT DOUBLE QUOTATION MARK
		Map.entry(0x201E, "\""),    // „  DOUBLE LOW-9 QUOTATION MARK
		Map.entry(0x2013, "-"),     // –  EN DASH
		Map.entry(0x2014, "-"),     // —  EM DASH
		Map.entry(0x2212, "-"),     // −  MINUS SIGN
		Map.entry(0x2026, "..."),   // …  HORIZONTAL ELLIPSIS
		Map.entry(0x2022, "o"),     // •  BULLET
//		Map.entry(0x20AC, "EUR"),   // €  EURO SIGN
		Map.entry(0x20AC, "(EUR)"), // €  EURO SIGN
//		Map.entry(0x0152, "OE"),    // Œ  LATIN CAPITAL LIGATURE OE
		Map.entry(0x0152, "(OE)"),  // Œ  LATIN CAPITAL LIGATURE OE
//		Map.entry(0x0153, "oe"),    // œ  LATIN SMALL LIGATURE OE
		Map.entry(0x0153, "(oe)"),  // œ  LATIN SMALL LIGATURE OE
		Map.entry(0x0141, "L"),     // Ł  LATIN CAPITAL LETTER L WITH STROKE
		Map.entry(0x0142, "l"),     // ł  LATIN SMALL LETTER L WITH STROKE
		Map.entry(0x0110, "D"),     // Đ  LATIN CAPITAL LETTER D WITH STROKE
		Map.entry(0x0111, "d"),     // đ  LATIN SMALL LETTER D WITH STROKE
		Map.entry(0x0131, "i"),     // ı  LATIN SMALL LETTER DOTLESS I
//		Map.entry(0x2122, "TM")     // ™  TRADE MARK SIGN
		Map.entry(0x2122, "(TM)")   // ™  TRADE MARK SIGN
	);

	/**
	 * Characters that exist in Latin-1, such as é, ö, ß and ø, are kept as they are. 
	 * Only characters outside Latin-1 are transliterated: 
	 *  - ś becomes s
	 *  - č becomes c
	 *  - € becomes EUR
	 *  - “” become "
	 */
	public static String translit(String s) 
	{
		StringBuilder sb = new StringBuilder(s.length());
		s.codePoints().forEach(cp -> 
		{
			// already Latin-1
			if (cp < 0x100) 
			{
				sb.appendCodePoint(cp);
				return;
			}
			String r = MAP.get(cp);
			if (r != null) 
			{
				sb.append(r);
				return;
			}
			// Decompose (e.g. "ś" -> "s" + accent, "ﬁ" -> "fi") and drop the accents
			String d = Normalizer.normalize(new String(Character.toChars(cp)), Normalizer.Form.NFKD).replaceAll("\\p{M}", "");
			if (!d.isEmpty() && d.chars().allMatch(c -> c < 0x100))
				sb.append(d);
			else
				sb.append('?'); // same fallback as iconv
		});
		return sb.toString();
	}

	public static byte[] toLatin1(String s) 
	{
		return translit(s).getBytes(StandardCharsets.ISO_8859_1);
	}
	public static byte[] to_ISO_8859_1(String s)
	{
		return translit(s).getBytes(StandardCharsets.ISO_8859_1);
	}

	// File equivalent of: iconv -f UTF-8 -t LATIN1//TRANSLIT in > out
	public static void main(String[] args) throws IOException 
	{
		try (BufferedReader in  = Files.newBufferedReader(Path.of(args[0]), StandardCharsets.UTF_8);
			 Writer         out = Files.newBufferedWriter(Path.of(args[1]), StandardCharsets.ISO_8859_1)) 
		{
			String line;
			while ((line = in.readLine()) != null) 
			{
				out.write(translit(line));
				out.write('\n');
			}
		}
	}

}
