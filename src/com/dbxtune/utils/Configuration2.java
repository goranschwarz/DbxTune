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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/*###################################################################
 * NOT THIS IS NOT USED YET -- I WILL HAVE TO THINK ABOUT THIS
 *###################################################################
 */

/**
 * A format-preserving properties file reader/writer.
 * <p>
 * Unlike {@link Configuration}, which loses all comments and blank lines on
 * every save, this class only rewrites the value portion of lines whose key
 * was explicitly changed via {@link #setProperty}.  All other lines — comments,
 * blank lines, unchanged key=value entries, and original whitespace/separators
 * — are kept verbatim.  Keys not present in the original file are appended at
 * the end.
 * <p>
 * Reading uses the standard {@link Properties#load} so all Java properties
 * escaping and Unicode conventions are handled correctly.  Writing uses the
 * same ISO-8859-1 encoding as the standard {@link Properties} format.
 */
public class Configuration2
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	/** Standard Java properties file encoding */
	private static final Charset PROPS_CHARSET = StandardCharsets.ISO_8859_1;

	private String _filename;

	/** Raw lines read from the file — used for format-preserving save */
	private List<String> _rawLines = new ArrayList<>();

	/** Parsed key→value (from Properties.load, handles all escaping/unicode) */
	private final Map<String, String> _props = new LinkedHashMap<>();

	/** Keys changed/added since last load or save, in insertion order */
	private final Set<String> _changedKeys = new LinkedHashSet<>();


	// -------------------------------------------------------------------------
	// Constructors
	// -------------------------------------------------------------------------

	public Configuration2() {}

	public Configuration2(String filename)
	{
		load(filename);
	}


	// -------------------------------------------------------------------------
	// Load
	// -------------------------------------------------------------------------

	public String getFilename()
	{
		return _filename;
	}

	public void load(String filename)
	{
		_filename = filename;
		_rawLines.clear();
		_props.clear();
		_changedKeys.clear();

		if (filename == null)
			return;

		File f = new File(filename);
		if (!f.exists())
		{
			_logger.debug("Configuration2.load(): file not found: '{}'", filename);
			return;
		}

		// Read raw lines — kept for verbatim reproduction on save
		try
		{
			_rawLines = new ArrayList<>(Files.readAllLines(f.toPath(), PROPS_CHARSET));
		}
		catch (IOException e)
		{
			_logger.warn("Configuration2.load(): failed to read raw lines from '{}': {}", filename, e.getMessage());
		}

		// Parse into Properties for correct escape/unicode handling
		try (FileInputStream fis = new FileInputStream(f))
		{
			Properties p = new Properties();
			p.load(fis);
			for (String key : p.stringPropertyNames())
				_props.put(key, p.getProperty(key));
		}
		catch (IOException e)
		{
			_logger.warn("Configuration2.load(): failed to parse properties from '{}': {}", filename, e.getMessage());
		}
	}


	// -------------------------------------------------------------------------
	// Read
	// -------------------------------------------------------------------------

	public boolean hasProperty(String key)
	{
		return _props.containsKey(key);
	}

	public String getProperty(String key)
	{
		return _props.get(key);
	}

	public String getProperty(String key, String defaultValue)
	{
		String val = _props.get(key);
		return val != null ? val : defaultValue;
	}

	public int getIntProperty(String key, int defaultValue)
	{
		String val = _props.get(key);
		if (val == null)
			return defaultValue;
		try { return Integer.parseInt(val.trim()); }
		catch (NumberFormatException e) { return defaultValue; }
	}

	public long getLongProperty(String key, long defaultValue)
	{
		String val = _props.get(key);
		if (val == null)
			return defaultValue;
		try { return Long.parseLong(val.trim()); }
		catch (NumberFormatException e) { return defaultValue; }
	}

	public boolean getBooleanProperty(String key, boolean defaultValue)
	{
		String val = _props.get(key);
		if (val == null)
			return defaultValue;
		String t = val.trim();
		return "true".equalsIgnoreCase(t) || "yes".equalsIgnoreCase(t) || "1".equals(t);
	}


	// -------------------------------------------------------------------------
	// Write
	// -------------------------------------------------------------------------

	public boolean isDirty()
	{
		return !_changedKeys.isEmpty();
	}

	public void setProperty(String key, String value)
	{
		_props.put(key, value);
		_changedKeys.add(key);
	}

	public void setProperty(String key, int value)
	{
		setProperty(key, Integer.toString(value));
	}

	public void setProperty(String key, long value)
	{
		setProperty(key, Long.toString(value));
	}

	public void setProperty(String key, boolean value)
	{
		setProperty(key, Boolean.toString(value));
	}

	public void removeProperty(String key)
	{
		_props.remove(key);
		_changedKeys.add(key); // mark as changed so save() removes it from the file
	}


	// -------------------------------------------------------------------------
	// Save — format-preserving
	// -------------------------------------------------------------------------

	/**
	 * Save changed properties back to the file, preserving the original format.
	 * <ul>
	 *   <li>Comment lines, blank lines, and unchanged key=value lines are kept verbatim.</li>
	 *   <li>Lines whose key appears in the change set have their value portion replaced in-place,
	 *       keeping the original key spelling, separator ({@code =} / {@code :}), and surrounding whitespace.</li>
	 *   <li>Removed keys have their line deleted from the file.</li>
	 *   <li>New keys (not in the original file) are appended at the end.</li>
	 * </ul>
	 *
	 * @throws IOException if the file cannot be written
	 */
	public synchronized void save() throws IOException
	{
		if (_filename == null)
		{
			_logger.warn("Configuration2.save(): no filename set, cannot save.");
			return;
		}
		if (_changedKeys.isEmpty())
		{
			_logger.debug("Configuration2.save(): no changes to save for '{}'.", _filename);
			return;
		}

		List<String> output = new ArrayList<>(_rawLines.size() + _changedKeys.size() + 2);
		Set<String> writtenKeys = new LinkedHashSet<>();

		for (String line : _rawLines)
		{
			String trimmed = line.trim();

			// Blank or comment line — keep verbatim
			if (trimmed.isEmpty() || trimmed.charAt(0) == '#' || trimmed.charAt(0) == '!')
			{
				output.add(line);
				continue;
			}

			// Try to identify the key on this line
			String lineKey = extractKey(trimmed);
			if (lineKey != null && _changedKeys.contains(lineKey))
			{
				if (_props.containsKey(lineKey))
				{
					// Replace value in-place, preserving key + separator + surrounding whitespace
					output.add(rebuildLine(line, escapeValue(_props.get(lineKey))));
					_logger.debug("Configuration2.save(): updated key='{}' in '{}'.", lineKey, _filename);
				}
				else
				{
					// Key was removed — drop this line entirely
					_logger.debug("Configuration2.save(): removed key='{}' from '{}'.", lineKey, _filename);
				}
				writtenKeys.add(lineKey);
			}
			else
			{
				output.add(line);
			}
		}

		// Append new keys not present in the original file
		boolean addedBlank = false;
		for (String key : _changedKeys)
		{
			if (!writtenKeys.contains(key) && _props.containsKey(key))
			{
				if (!addedBlank)
				{
					output.add("");
					addedBlank = true;
				}
				output.add(escapeKey(key) + "=" + escapeValue(_props.get(key)));
				_logger.info("Configuration2.save(): appended new key='{}' to '{}'.", key, _filename);
			}
		}

		// Ensure parent directory exists
		File f = new File(_filename);
		File parent = f.getParentFile();
		if (parent != null && !parent.exists())
			parent.mkdirs();

		Files.write(Paths.get(_filename), output, PROPS_CHARSET);

		int savedCount = writtenKeys.size() + (addedBlank ? (_changedKeys.size() - writtenKeys.size()) : 0);
		_logger.info("Configuration2.save(): saved {} key(s) to '{}'.", savedCount, _filename);

		_changedKeys.clear();

		// Keep _rawLines in sync so repeated saves stay consistent
		_rawLines = output;
	}


	// -------------------------------------------------------------------------
	// Parsing helpers (package-private for unit testing)
	// -------------------------------------------------------------------------

	/**
	 * Extract the unescaped key from a trimmed, non-comment, non-blank properties line.
	 * Returns {@code null} if the line cannot be parsed as a key=value entry.
	 */
	static String extractKey(String trimmedLine)
	{
		if (trimmedLine.isEmpty() || trimmedLine.charAt(0) == '#' || trimmedLine.charAt(0) == '!')
			return null;

		StringBuilder key = new StringBuilder();
		int i = 0;
		int len = trimmedLine.length();

		while (i < len)
		{
			char c = trimmedLine.charAt(i);

			// Escaped character — include the literal char in the key
			if (c == '\\' && i + 1 < len)
			{
				i++;
				key.append(trimmedLine.charAt(i));
				i++;
				continue;
			}

			// Key ends at separator or whitespace
			if (c == '=' || c == ':' || Character.isWhitespace(c))
				break;

			key.append(c);
			i++;
		}

		return key.length() > 0 ? key.toString() : null;
	}

	/**
	 * Rebuild a properties line keeping the original key spelling, separator, and
	 * surrounding whitespace, but replacing the value portion.
	 */
	static String rebuildLine(String originalLine, String escapedNewValue)
	{
		int valueStart = findValueStart(originalLine);
		return originalLine.substring(0, valueStart) + escapedNewValue;
	}

	/**
	 * Find the character index in {@code line} where the value portion begins.
	 * This mirrors the parsing logic in {@link Properties#load} so the split
	 * point is consistent with what was read.
	 */
	static int findValueStart(String line)
	{
		int i = 0;
		int len = line.length();

		// Skip leading whitespace
		while (i < len && Character.isWhitespace(line.charAt(i))) i++;

		// Skip key (handle escape sequences)
		while (i < len)
		{
			char c = line.charAt(i);
			if (c == '\\' && i + 1 < len) { i += 2; continue; }
			if (c == '=' || c == ':' || Character.isWhitespace(c)) break;
			i++;
		}

		// Skip whitespace between key and separator
		int beforeSep = i;
		while (i < len && Character.isWhitespace(line.charAt(i))) i++;

		// Consume explicit separator (= or :) if present, then trailing whitespace
		if (i < len && (line.charAt(i) == '=' || line.charAt(i) == ':'))
		{
			i++; // consume separator
			while (i < len && Character.isWhitespace(line.charAt(i))) i++;
		}
		else if (i > beforeSep)
		{
			// Whitespace-only separator (no = or :) — value already starts here
		}

		return i;
	}

	/** Escape a key for writing to a properties file */
	static String escapeKey(String key)
	{
		return key
			.replace("\\", "\\\\")
			.replace(" ",  "\\ ")
			.replace("=",  "\\=")
			.replace(":",  "\\:")
			.replace("\t", "\\t")
			.replace("\n", "\\n")
			.replace("\r", "\\r");
	}

	/** Escape a value for writing to a properties file */
	static String escapeValue(String value)
	{
		if (value == null)
			return "";
		return value
			.replace("\\", "\\\\")
			.replace("\n", "\\n")
			.replace("\r", "\\r")
			.replace("\t", "\\t");
	}


	// -------------------------------------------------------------------------
	// toString
	// -------------------------------------------------------------------------

	@Override
	public String toString()
	{
		return "Configuration2[file='" + _filename + "', keys=" + _props.size() + ", dirty=" + _changedKeys.size() + "]";
	}
}
