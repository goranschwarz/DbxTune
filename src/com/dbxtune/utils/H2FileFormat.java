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
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Check the on-disk format of a H2 MVStore database file (<code>*.mv.db</code>) <b>without opening the database</b>.
 * <p>
 * The first block of a MVStore file is a plain text header, like:<br>
 * <code>H:2,block:6,blockSize:1000,chunk:aa,clean:1,created:19740fd1ebf,format:2,version:aa,fletcher:8fc75088\n</code>
 * <ul>
 *   <li><code>format:1</code> = H2 1.4.x</li>
 *   <li><code>format:2</code> = H2 2.0 / 2.1</li>
 *   <li><code>format:3</code> = H2 2.2 and later</li>
 * </ul>
 * The formats the H2 on the classpath can read are taken from H2's own constants (FORMAT_READ_MIN/MAX),
 * so this keeps working when H2 is upgraded.
 */
public class H2FileFormat
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public static final String MV_DB_SUFFIX = ".mv.db";

	/** What to do when a H2 database file has a format that the current H2 can't read */
	public enum OldFormatAction
	{
		/** Log an error and stop */
		ERROR,

		/** Copy the database into a new file, using the old H2 version as source (H2CentralDbCopy3) */
		COPY_UPGRADE

		// Possible future actions: NEW_DB_FILE, H2_NATIVE (org.h2.tools.Upgrade), ...
	};

	private static int    _currentFormatWrite   = -1;
	private static int    _currentFormatReadMin = -1;
	private static int    _currentFormatReadMax = -1;

	/**
	 * Read the MVStore file header as a key/value map (values as raw strings, numbers are in hex)
	 *
	 * @return a Map with the header, or an empty map if it's not a MVStore file header
	 */
	public static Map<String, String> readHeader(File dbFile)
	throws IOException
	{
		Map<String, String> map = new LinkedHashMap<>();

		byte[] buf = new byte[4096];
		int len;
		try (InputStream in = Files.newInputStream(dbFile.toPath()))
		{
			len = in.readNBytes(buf, 0, buf.length);
		}

		// The header ends with a newline (followed by \0 padding)
		int end = 0;
		while (end < len && buf[end] != '\n' && buf[end] != 0)
			end++;

		String header = new String(buf, 0, end, StandardCharsets.ISO_8859_1);
		if ( ! header.startsWith("H:") )
			return map;

		for (String entry : header.split(","))
		{
			int pos = entry.indexOf(':');
			if (pos > 0)
				map.put(entry.substring(0, pos).trim(), entry.substring(pos + 1).trim());
		}
		return map;
	}

	/**
	 * Get the value of 'format' in the MVStore header
	 * @return format number, or -1 if not a MVStore file (or it can't be read)
	 */
	public static int readFormat(File dbFile)
	{
		return readHeaderInt(dbFile, "format", -1);
	}

	/**
	 * Get the value of 'formatRead' in the MVStore header (if not set, 'format' is used)
	 * @return format number, or -1 if not a MVStore file (or it can't be read)
	 */
	public static int readFormatRead(File dbFile)
	{
		return readHeaderInt(dbFile, "formatRead", readFormat(dbFile));
	}

	private static int readHeaderInt(File dbFile, String key, int defaultValue)
	{
		try
		{
			String val = readHeader(dbFile).get(key);
			return val == null ? defaultValue : Integer.parseInt(val, 16);
		}
		catch (IOException | NumberFormatException ex)
		{
			_logger.warn("Problems reading H2 file header '" + key + "' from file '" + dbFile + "'. Caught: " + ex);
			return defaultValue;
		}
	}

	/** Lowest file format the H2 on the classpath can read */
	public static synchronized int getCurrentFormatReadMin()
	{
		init();
		return _currentFormatReadMin;
	}

	/** Highest file format the H2 on the classpath can read */
	public static synchronized int getCurrentFormatReadMax()
	{
		init();
		return _currentFormatReadMax;
	}

	/** File format the H2 on the classpath writes (probed by creating a small temp MVStore file) */
	public static synchronized int getCurrentFormat()
	{
		init();
		return _currentFormatWrite;
	}

	private static void init()
	{
		if (_currentFormatWrite > 0)
			return;

		// Probe: create a throw-away MVStore file, and read back what format it was written with
		File tmpFile = null;
		try
		{
			tmpFile = File.createTempFile("dbxtune_h2_format_probe_", MV_DB_SUFFIX);
			tmpFile.delete();
			org.h2.mvstore.MVStore.open(tmpFile.getAbsolutePath()).close();
			_currentFormatWrite = readFormat(tmpFile);
		}
		catch (Exception ex)
		{
			_logger.warn("Problems probing the H2 MVStore file format. Caught: " + ex);
		}
		finally
		{
			if (tmpFile != null)
				tmpFile.delete();
		}

		// Get the READ range from H2 internals (FileStore in H2 2.2+, MVStore in older versions)
		ClassLoader cl = H2FileFormat.class.getClassLoader();
		_currentFormatReadMin = getH2PrivateIntConstant(cl, "FORMAT_READ_MIN", _currentFormatWrite);
		_currentFormatReadMax = getH2PrivateIntConstant(cl, "FORMAT_READ_MAX", _currentFormatWrite);

		_logger.info("H2 version '" + getCurrentH2Version() + "' writes MVStore file format " + _currentFormatWrite + ", and can read formats " + _currentFormatReadMin + " to " + _currentFormatReadMax + ".");
	}

	/**
	 * Get a (private) static int constant from the H2 MVStore classes
	 * (FileStore in H2 2.2+, MVStore in older versions)
	 */
	private static int getH2PrivateIntConstant(ClassLoader cl, String fieldName, int defaultValue)
	{
		for (String className : new String[] {"org.h2.mvstore.FileStore", "org.h2.mvstore.MVStore"})
		{
			try
			{
				Field f = Class.forName(className, true, cl).getDeclaredField(fieldName);
				f.setAccessible(true);
				return f.getInt(null);
			}
			catch (Throwable ignore) { /* try next class */ }
		}
		return defaultValue;
	}


	//--------------------------------------------------------------------------------
	// After connect: what H2 build created the database vs what build is running
	//--------------------------------------------------------------------------------

	/**
	 * Log what H2 build created the database (INFORMATION_SCHEMA.SETTINGS: CREATE_BUILD) and what build is running (info.BUILD_ID).
	 * <ul>
	 *   <li>INFO: always (when CREATE_BUILD is available)</li>
	 *   <li>WARN: if the database was created by a <b>newer</b> H2 build than the one running (a downgrade, H2 does not promise that works)</li>
	 * </ul>
	 * CREATE_BUILD is kept for the life of the database file (a normal H2 upgrade, with the same file format, does NOT change it),
	 * so an older CREATE_BUILD is normal and only logged as INFO.<br>
	 * CREATE_BUILD is NOT visible on read-only connections, then nothing is logged.
	 * <p>
	 * Never throws, problems are only logged.
	 *
	 * @param conn   A connection to a H2 database
	 * @param dbName Name/URL of the database, used in the log message
	 */
	public static void logCreateBuildInfo(Connection conn, String dbName)
	{
		if (conn == null)
			return;

		int createBuild  = -1;
		int runningBuild = -1;
		String sql = "select SETTING_NAME, SETTING_VALUE from INFORMATION_SCHEMA.SETTINGS where SETTING_NAME in ('CREATE_BUILD', 'info.BUILD_ID')";
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while (rs.next())
			{
				String name = rs.getString(1);
				int    val  = StringUtil.parseInt(rs.getString(2), -1);
				if      ("CREATE_BUILD" .equals(name)) createBuild  = val;
				else if ("info.BUILD_ID".equals(name)) runningBuild = val;
			}
		}
		catch (SQLException ex)
		{
			_logger.info("Problems getting H2 CREATE_BUILD/BUILD_ID for database '" + dbName + "'. Skipping this check. Caught: " + ex);
			return;
		}

		if (createBuild <= 0 || runningBuild <= 0)
			return;

		String msg = "H2 database '" + dbName + "' was created by H2 build " + createBuild + ", running H2 build " + runningBuild + " (version " + getCurrentH2Version() + ").";
		if (createBuild > runningBuild)
			_logger.warn(msg + " The database was created by a NEWER H2 version than the one running (a downgrade). H2 does not guarantee this works. Consider using H2 build " + createBuild + " or later.");
		else
			_logger.info(msg);
	}


	//--------------------------------------------------------------------------------
	// Find a H2 JAR file that can read a specific file format
	//--------------------------------------------------------------------------------

	/** Information about a H2 JAR file: version and what file formats it can read */
	public static class H2JarInfo
	{
		public File   jarFile;
		public String version;
		public int    formatReadMin;
		public int    formatReadMax;

		public boolean canRead(int format) { return format >= formatReadMin && format <= formatReadMax; }

		@Override
		public String toString() { return "H2JarInfo[jar='" + jarFile + "', version=" + version + ", formatRead=" + formatReadMin + "-" + formatReadMax + "]"; }
	}

	/**
	 * Load a H2 JAR in a separate (isolated) ClassLoader, and ask it what version it is and what file formats it can read.
	 * <p>
	 * This does NOT depend on the JAR file name (the H2 version number does NOT tell the file format, for example H2 2.1 writes format 2 and H2 2.4 writes format 3).
	 *
	 * @return info, or null if it's not a H2 JAR (or we can't figure out what formats it reads)
	 */
	public static H2JarInfo getJarInfo(File jarFile)
	{
		// parent = null: only the JAR itself (and the JDK), do NOT see the H2 on our own classpath
		try (URLClassLoader cl = new URLClassLoader(new URL[] { jarFile.toURI().toURL() }, null))
		{
			H2JarInfo info = new H2JarInfo();
			info.jarFile = jarFile;

			info.version = String.valueOf(Class.forName("org.h2.engine.Constants", true, cl).getField("VERSION").get(null));

			// H2 2.x: FORMAT_READ_MIN / FORMAT_READ_MAX
			info.formatReadMin = getH2PrivateIntConstant(cl, "FORMAT_READ_MIN", -1);
			info.formatReadMax = getH2PrivateIntConstant(cl, "FORMAT_READ_MAX", -1);

			// H2 1.4.x: FORMAT_READ
			if (info.formatReadMin < 0 || info.formatReadMax < 0)
			{
				int formatRead = getH2PrivateIntConstant(cl, "FORMAT_READ", -1);
				info.formatReadMin = formatRead;
				info.formatReadMax = formatRead;
			}

			if (info.formatReadMin < 0)
			{
				_logger.info("Skipping H2 JAR '" + jarFile + "' (version " + info.version + "), can't figure out what MVStore file formats it can read.");
				return null;
			}
			return info;
		}
		catch (Throwable ex)
		{
			_logger.info("Skipping JAR '" + jarFile + "', it does not look like a H2 JAR. Caught: " + ex);
			return null;
		}
	}

	/**
	 * Search the directories for H2 JAR files (h2-*.jar) that can read the file format.
	 * <p>
	 * The H2 JAR that we are currently running with is skipped (it can't read the format, otherwise we wouldn't be here).
	 *
	 * @return the highest H2 version that can read the format, or null if none was found
	 */
	public static H2JarInfo findH2JarForFormat(int format, List<File> dirs)
	{
		File currentH2Jar = getCurrentH2Jar();

		H2JarInfo best = null;
		for (File dir : dirs)
		{
			File[] files = dir.listFiles((d, name) -> name.startsWith("h2-") && name.endsWith(".jar"));
			if (files == null)
				continue;

			for (File f : files)
			{
				if (currentH2Jar != null && f.getAbsoluteFile().equals(currentH2Jar.getAbsoluteFile()))
					continue;

				H2JarInfo info = getJarInfo(f);
				if (info == null)
					continue;

				_logger.info("Found " + info + ", can read format " + format + ": " + info.canRead(format));
				if (info.canRead(format) && (best == null || compareVersions(info.version, best.version) > 0))
					best = info;
			}
		}
		return best;
	}

	/** The JAR file of the H2 on our classpath (null if not found, or not a JAR) */
	public static File getCurrentH2Jar()
	{
		try
		{
			File f = new File(org.h2.Driver.class.getProtectionDomain().getCodeSource().getLocation().toURI());
			return f.isFile() ? f : null;
		}
		catch (Exception ex)
		{
			return null;
		}
	}

	/** Compare version strings like '2.1.214' and '2.4.240' (numeric per part) */
	public static int compareVersions(String v1, String v2)
	{
		String[] p1 = v1.split("\\.");
		String[] p2 = v2.split("\\.");
		for (int i = 0; i < Math.max(p1.length, p2.length); i++)
		{
			int n1 = i < p1.length ? StringUtil.parseInt(p1[i].replaceAll("\\D.*", ""), 0) : 0;
			int n2 = i < p2.length ? StringUtil.parseInt(p2[i].replaceAll("\\D.*", ""), 0) : 0;
			if (n1 != n2)
				return Integer.compare(n1, n2);
		}
		return 0;
	}

	/** Version of the H2 on the classpath (read at runtime, the compile time constant would be inlined) */
	public static String getCurrentH2Version()
	{
		try
		{
			return String.valueOf(Class.forName("org.h2.engine.Constants").getField("VERSION").get(null));
		}
		catch (Exception ex)
		{
			return "-unknown-";
		}
	}

	/**
	 * Is the file written by an older H2 version, that the current H2 can't read
	 */
	public static boolean needsUpgrade(File dbFile)
	{
		int format = readFormat(dbFile);
		return format > 0 && format < getCurrentFormatReadMin();
	}

	/**
	 * Is the file written by a newer H2 version, that the current H2 can't read
	 */
	public static boolean isTooNew(File dbFile)
	{
		int formatRead = readFormatRead(dbFile);
		return formatRead > 0 && formatRead > getCurrentFormatReadMax();
	}

	/**
	 * One line description, for the log.
	 */
	public static String describe(File dbFile)
	{
		int format = readFormat(dbFile);
		String status = format <= 0 ? "NOT a H2 MVStore file header" : needsUpgrade(dbFile) ? "UPGRADE NEEDED" : isTooNew(dbFile) ? "TOO NEW (written by a newer H2 version)" : "OK";

		return "H2 file format check: file='" + dbFile + "', size=" + StringUtil.bytesToHuman(dbFile.length())
				+ ", format=" + format
				+ ", current H2 " + getCurrentH2Version() + " writes format=" + getCurrentFormat()
				+ " (reads " + getCurrentFormatReadMin() + "-" + getCurrentFormatReadMax() + ")"
				+ " -> " + status;
	}

	/**
	 * Parse a OldFormatAction, an unknown value will log a warning and return ERROR
	 */
	public static OldFormatAction parseAction(String val, OldFormatAction defaultAction)
	{
		if (StringUtil.isNullOrBlank(val))
			return defaultAction;

		try
		{
			return OldFormatAction.valueOf(val.trim().toUpperCase());
		}
		catch (IllegalArgumentException ex)
		{
			_logger.warn("Unknown H2 old format action '" + val + "', valid values are " + java.util.Arrays.toString(OldFormatAction.values()) + ". Using '" + OldFormatAction.ERROR + "'.");
			return OldFormatAction.ERROR;
		}
	}


	//--------------------------------------------------------------------------------
	// Free space check
	//--------------------------------------------------------------------------------

	/** Recording files: SERVERNAME_yyyy-MM-dd.mv.db (and spill over files: SERVERNAME_yyyy-MM-dd-SPILL-OVER-DB-#.mv.db) */
	private static final Pattern RECORDING_FILE_PATTERN = Pattern.compile(".*_\\d{4}-\\d{2}-\\d{2}.*\\.mv\\.db");

	public static class SpaceCheckResult
	{
		public boolean ok;
		public long    sourceSize;
		public long    freeBytes;
		public long    neededBytes;
		public long    shortfallBytes;

		/** Candidate files that can be removed, oldest first */
		public List<File> recordingCandidates = new ArrayList<>();
		/** How many of the 'recordingCandidates' is needed to cover the shortfall (-1 = even all of them are not enough) */
		public int     recordingCandidatesNeeded = -1;
		/** Old backups/leftovers from earlier upgrades */
		public List<File> leftoverFiles = new ArrayList<>();

		/** A multi line message, describing what to do (only set if not ok) */
		public String  message;
	}

	/**
	 * Check that we have enough free space to copy the database file
	 * <p>
	 * needed = fileSize * factor + marginMb
	 * <p>
	 * If there is NOT enough space, the result will contain a message with a list of old recording files
	 * (oldest first, with a running total) that can be removed to make room. <b>Nothing is removed.</b>
	 */
	public static SpaceCheckResult checkFreeSpace(File dbFile, double factor, long marginMb)
	{
		SpaceCheckResult res = new SpaceCheckResult();
		File dir = dbFile.getAbsoluteFile().getParentFile();

		res.sourceSize  = dbFile.length();
		res.neededBytes = (long) (res.sourceSize * factor) + (marginMb * 1024 * 1024);
		try
		{
			res.freeBytes = Files.getFileStore(dir.toPath()).getUsableSpace();
		}
		catch (IOException ex)
		{
			_logger.warn("Problems getting free space for directory '" + dir + "'. Caught: " + ex);
			res.freeBytes = dir.getUsableSpace();
		}
		res.ok = res.freeBytes >= res.neededBytes;
		if (res.ok)
			return res;

		res.shortfallBytes = res.neededBytes - res.freeBytes;

		// Get recording files (oldest first) and leftovers from earlier upgrades
		// Skip today's recordings, they are probably in use by a collector
		String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
		File[] files = dir.listFiles();
		if (files != null)
		{
			for (File f : files)
			{
				String name = f.getName();
				if ( ! f.isFile() || f.getAbsoluteFile().equals(dbFile.getAbsoluteFile()) )
					continue;

				if (name.contains(".h2fmt") || name.contains("_H2UPGRADE_"))
					res.leftoverFiles.add(f);
				else if (RECORDING_FILE_PATTERN.matcher(name).matches() && ! name.contains("_" + today))
					res.recordingCandidates.add(f);
			}
		}
		res.recordingCandidates.sort(Comparator.comparingLong(File::lastModified).thenComparing(File::getName));

		long sum = 0;
		for (int i = 0; i < res.recordingCandidates.size(); i++)
		{
			sum += res.recordingCandidates.get(i).length();
			if (sum >= res.shortfallBytes)
			{
				res.recordingCandidatesNeeded = i + 1;
				break;
			}
		}

		// Compose message
		String nl = "\n";
		StringBuilder sb = new StringBuilder();
		sb.append("NOT ENOUGH FREE DISK SPACE to upgrade/copy the H2 database file.").append(nl);
		sb.append("    Database file:  ").append(dbFile).append(nl);
		sb.append("    Database size:  ").append(StringUtil.bytesToHuman(res.sourceSize)).append(nl);
		sb.append("    Space needed:   ").append(StringUtil.bytesToHuman(res.neededBytes)).append("  (size * ").append(factor).append(" + ").append(marginMb).append(" MB margin)").append(nl);
		sb.append("    Free space:     ").append(StringUtil.bytesToHuman(res.freeBytes)).append("  in directory '").append(dir).append("'").append(nl);
		sb.append("    Shortfall:      ").append(StringUtil.bytesToHuman(res.shortfallBytes)).append(nl);
		sb.append(nl);

		if ( ! res.leftoverFiles.isEmpty() )
		{
			sb.append("Leftovers from earlier H2 upgrades (candidates to remove first):").append(nl);
			for (File f : res.leftoverFiles)
				sb.append("    ").append(StringUtil.left(StringUtil.bytesToHuman(f.length()), 12)).append(f.getName()).append(nl);
			sb.append(nl);
		}

		if (res.recordingCandidates.isEmpty())
		{
			sb.append("No old collector recordings was found in '").append(dir).append("'. Free up space some other way.").append(nl);
		}
		else
		{
			int toList = res.recordingCandidatesNeeded > 0 ? res.recordingCandidatesNeeded : res.recordingCandidates.size();
			long listSum = 0;
			List<String> rmList = new ArrayList<>();

			sb.append("Oldest collector recordings (today's recordings are excluded). The files marked with '*' covers the shortfall:").append(nl);
			sb.append("    ").append(StringUtil.left("Size", 12)).append(StringUtil.left("Running Total", 16)).append("File").append(nl);
			for (int i = 0; i < toList; i++)
			{
				File f = res.recordingCandidates.get(i);
				listSum += f.length();
				rmList.add(f.getAbsolutePath());
				sb.append("  * ").append(StringUtil.left(StringUtil.bytesToHuman(f.length()), 12)).append(StringUtil.left(StringUtil.bytesToHuman(listSum), 16)).append(f.getName()).append(nl);
			}

			if (res.recordingCandidatesNeeded > 0)
				sb.append("Removing these ").append(toList).append(" oldest recordings frees ").append(StringUtil.bytesToHuman(listSum)).append(" (needed ").append(StringUtil.bytesToHuman(res.shortfallBytes)).append(").").append(nl);
			else
				sb.append("Removing ALL ").append(toList).append(" old recordings frees ").append(StringUtil.bytesToHuman(listSum)).append(", which is NOT enough (needed ").append(StringUtil.bytesToHuman(res.shortfallBytes)).append(").").append(nl);

			sb.append(nl);
			sb.append("To remove them (please double check the list first):").append(nl);
			sb.append("    rm");
			for (String fn : rmList)
				sb.append(" '").append(fn).append("'");
			sb.append(nl);
		}
		res.message = sb.toString();

		return res;
	}
}
