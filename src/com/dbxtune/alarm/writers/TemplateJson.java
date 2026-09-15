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
package com.dbxtune.alarm.writers;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JSON string escaping for Velocity templates that produce JSON (eg the Microsoft Teams Adaptive Card).
 * <p>
 * Exposed to every template as <code>$Json</code>:
 * <ul>
 *   <li><code>$Json.str($value)</code> - a complete, quoted JSON string: <code>"Blocked by \"spid 42\""</code></li>
 *   <li><code>$Json.esc($value)</code> - the escaped contents only, for use inside quotes you wrote yourself</li>
 * </ul>
 * Why this matters: a HTML template with an unescaped <code>&lt;</code> renders slightly wrong, but a JSON
 * template with an unescaped <code>"</code>, backslash or newline produces a payload the receiver rejects,
 * which means the alarm message is <b>lost</b>. Alarm descriptions routinely contain all three (SQL text).
 *
 * @author Goran Schwarz
 */
public class TemplateJson
{
	private TemplateJson()
	{
	}

	/**
	 * Escape a value so it can be placed <i>inside</i> a JSON string literal.
	 *
	 * @param value  anything, <code>null</code> gives ""
	 * @return the escaped text, without surrounding quotes
	 */
	public static String esc(Object value)
	{
		if (value == null)
			return "";

		String str = value.toString();
		StringBuilder sb = new StringBuilder(str.length() + 16);

		for (int i = 0; i < str.length(); i++)
		{
			char c = str.charAt(i);
			switch (c)
			{
				case '"':  sb.append("\\\""); break;
				case '\\': sb.append("\\\\"); break;
				case '\b': sb.append("\\b");  break;
				case '\f': sb.append("\\f");  break;
				case '\n': sb.append("\\n");  break;
				case '\r': sb.append("\\r");  break;
				case '\t': sb.append("\\t");  break;
				default:
					if (c < 0x20)
						sb.append(String.format("\\u%04x", (int) c)); // any other control char
					else
						sb.append(c);
			}
		}
		return sb.toString();
	}

	/**
	 * A complete, quoted JSON string.
	 *
	 * @param value  anything, <code>null</code> gives <code>""</code> (an empty JSON string, not <code>null</code>)
	 * @return eg <code>"Blocked by \"spid 42\""</code>
	 */
	public static String str(Object value)
	{
		return "\"" + esc(value) + "\"";
	}

	/**
	 * A translation map for {@link WriterUtils#createMessageFromTemplate}, which makes every standard template
	 * variable (<code>${description}</code>, <code>${extendedDescription}</code>, ...) JSON-safe, so a template
	 * can simply write <code>"text": "${description}"</code>.
	 * <p>
	 * It produces exactly the same result as {@link #esc(Object)}. The map is applied by plain string
	 * replacement, entry by entry and in iteration order, so the order is load bearing:
	 * the <b>backslash must be first</b>. Otherwise the backslashes introduced by the later entries would be
	 * escaped a second time (<code>"</code> -&gt; <code>\"</code> -&gt; <code>\\"</code>). Hence a LinkedHashMap.
	 */
	public static Map<String, String> createTranslationMap()
	{
		Map<String, String> map = new LinkedHashMap<>();

		map.put("\\", "\\\\");   // MUST be first
		map.put("\"", "\\\"");
		map.put("\b", "\\b");
		map.put("\f", "\\f");
		map.put("\n", "\\n");
		map.put("\r", "\\r");
		map.put("\t", "\\t");

		for (char c = 0; c < 0x20; c++)
		{
			if (c == '\b' || c == '\f' || c == '\n' || c == '\r' || c == '\t')
				continue;
			map.put(String.valueOf(c), String.format("\\u%04x", (int) c));
		}

		return map;
	}
}
