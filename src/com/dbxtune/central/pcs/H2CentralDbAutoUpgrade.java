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
package com.dbxtune.central.pcs;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.H2FileFormat;
import com.dbxtune.utils.H2FileFormat.H2JarInfo;
import com.dbxtune.utils.H2FileFormat.OldFormatAction;
import com.dbxtune.utils.H2FileFormat.SpaceCheckResult;
import com.dbxtune.utils.H2UrlHelper;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.TimeUtils;

/**
 * Called at DbxCentral startup, <b>before</b> the Central database is opened.
 * <p>
 * If the DbxCentral H2 database file is written by an older H2 version (that the current H2 can't read),
 * it is upgraded by copying it into a new file (using {@link H2CentralDbCopy3} and the old H2 JAR),
 * and the original file is kept as a backup.
 */
public class H2CentralDbAutoUpgrade
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public static final String  PROPKEY_action        = "DbxTuneCentral.h2.oldFormat.action";
	public static final OldFormatAction DEFAULT_action = OldFormatAction.COPY_UPGRADE;

	/** Suffixed with the format number, for example: DbxTuneCentral.h2.oldFormat.jar.format.2=/path/to/h2-2.1.214.jar */
	public static final String  PROPKEY_jarPrefix     = "DbxTuneCentral.h2.oldFormat.jar.format.";

	public static final String  PROPKEY_spaceFactor   = "DbxTuneCentral.h2.oldFormat.spaceFactor";
	public static final double  DEFAULT_spaceFactor   = 1.0;

	public static final String  PROPKEY_spaceMarginMb = "DbxTuneCentral.h2.oldFormat.spaceMarginMb";
	public static final long    DEFAULT_spaceMarginMb = 1024;

	/**
	 * Check the Central H2 database file format, and upgrade it if needed.
	 *
	 * @throws Exception if the database can't be used (and we could not upgrade it), DbxCentral should NOT continue to start
	 */
	public static void checkAndUpgrade(Configuration conf)
	throws Exception
	{
		String url = StringUtil.envVariableSubstitution(conf.getProperty(CentralPersistWriterJdbc.PROPKEY_JDBC_URL, CentralPersistWriterJdbc.DEFAULT_JDBC_URL));
		if (url == null || ! url.startsWith("jdbc:h2:file:"))
			return;

		File dbFile = new H2UrlHelper(url).getDbFile(true);
		if (dbFile == null)
			return;

		if ( ! dbFile.exists() )
		{
			File pageStoreFile = new File(dbFile.getAbsolutePath().replace(H2FileFormat.MV_DB_SUFFIX, ".h2.db"));
			if (pageStoreFile.exists())
				_logger.error("Found an old H2 'PageStore' database file '" + pageStoreFile + "' (H2 1.3 or earlier). This can NOT be upgraded automatically.");
			return;
		}

		_logger.info(H2FileFormat.describe(dbFile));

		if ( ! H2FileFormat.needsUpgrade(dbFile) )
			return;

		int fileFormat = H2FileFormat.readFormat(dbFile);
		String manualHint = "The database can be upgraded manually with: dbxtune.sh dbxcdbcopy -J <h2-jar-that-reads-format-" + fileFormat + "> -S '" + dbFile + "' -e";

		// What should we do
		OldFormatAction action = H2FileFormat.parseAction(conf.getProperty(PROPKEY_action), DEFAULT_action);
		_logger.info("The H2 database file '" + dbFile + "' has format " + fileFormat + " which can't be read by H2 " + H2FileFormat.getCurrentH2Version() + ". Action: " + PROPKEY_action + "=" + action);

		if (OldFormatAction.ERROR.equals(action))
		{
			String msg = "The DbxCentral H2 database file '" + dbFile + "' is written by an older H2 version (format " + fileFormat + ") and property '" + PROPKEY_action + "' is '" + action + "'. "
					+ manualHint + "  Or set '" + PROPKEY_action + "=" + OldFormatAction.COPY_UPGRADE + "' to upgrade it automatically at startup.";
			_logger.error(msg);
			throw new Exception(msg);
		}

		// Find a H2 JAR that can read the old format
		File oldH2Jar = findOldH2Jar(conf, fileFormat);
		if (oldH2Jar == null)
		{
			String msg = "Can't find a H2 JAR file that can read H2 file format " + fileFormat + ". Set property '" + PROPKEY_jarPrefix + fileFormat + "=/path/to/h2-x.y.z.jar'. " + manualHint;
			_logger.error(msg);
			throw new Exception(msg);
		}
		_logger.info("Using old H2 JAR '" + oldH2Jar + "' to read the old database.");

		// Check that we have enough disk space
		double spaceFactor   = conf.getDoubleProperty(PROPKEY_spaceFactor,   DEFAULT_spaceFactor);
		long   spaceMarginMb = conf.getLongProperty  (PROPKEY_spaceMarginMb, DEFAULT_spaceMarginMb);
		SpaceCheckResult space = H2FileFormat.checkFreeSpace(dbFile, spaceFactor, spaceMarginMb);
		if ( ! space.ok )
		{
			_logger.error("#################################################################################");
			for (String line : space.message.split("\n"))
				_logger.error("## " + line);
			_logger.error("## After freeing up space: start DbxCentral again, and the upgrade will be retried.");
			_logger.error("## Or set '" + PROPKEY_action + "=" + OldFormatAction.ERROR + "' and upgrade manually (possibly to another disk). " + manualHint);
			_logger.error("#################################################################################");
			throw new Exception("Not enough free disk space to upgrade the H2 database file '" + dbFile + "'. Needed " + StringUtil.bytesToHuman(space.neededBytes) + ", free " + StringUtil.bytesToHuman(space.freeBytes) + ". See the log above for what can be removed.");
		}
		_logger.info("Disk space check OK: needed " + StringUtil.bytesToHuman(space.neededBytes) + ", free " + StringUtil.bytesToHuman(space.freeBytes) + ".");

		// Target file: always a fresh one
		String ts = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		String dbBaseName = dbFile.getAbsolutePath().substring(0, dbFile.getAbsolutePath().length() - H2FileFormat.MV_DB_SUFFIX.length());
		File targetFile = new File(dbBaseName + "_H2UPGRADE_" + ts + H2FileFormat.MV_DB_SUFFIX);
		File backupFile = new File(dbFile.getAbsolutePath() + ".h2fmt" + fileFormat + "." + ts + ".bak");

		String user   = conf.getProperty(CentralPersistWriterJdbc.PROPKEY_JDBC_USERNAME, "sa");
		String passwd = conf.getProperty(CentralPersistWriterJdbc.PROPKEY_JDBC_PASSWORD, "");

		_logger.info("#################################################################################");
		_logger.info("## Starting H2 database UPGRADE, this may take a long time (depending on the database size).");
		_logger.info("##   Source:   " + dbFile + " (" + StringUtil.bytesToHuman(space.sourceSize) + ")");
		_logger.info("##   Target:   " + targetFile);
		_logger.info("##   Old JAR:  " + oldH2Jar);
		_logger.info("#################################################################################");

		long startTime = System.currentTimeMillis();
		Thread watchdog = startProgressWatchdog(dbFile, targetFile, spaceMarginMb);
		boolean ok;
		try
		{
			ok = H2CentralDbCopy3.upgradeFromOldH2(dbFile, oldH2Jar, targetFile, user, passwd);
		}
		catch (Exception ex)
		{
			String msg = "H2 database upgrade FAILED. The original database file '" + dbFile + "' is untouched. The partial target '" + targetFile + "' is kept for inspection (remove it before a retry to save space). Caught: " + ex;
			_logger.error(msg, ex);
			throw new Exception(msg, ex);
		}
		finally
		{
			watchdog.interrupt();
		}

		if ( ! ok )
		{
			String msg = "H2 database upgrade had ERRORS (see the error report above). The original database file '" + dbFile + "' is untouched. The target '" + targetFile + "' is kept for inspection. " + manualHint;
			_logger.error(msg);
			throw new Exception(msg);
		}

		int targetFormat = H2FileFormat.readFormat(targetFile);
		if (targetFormat != H2FileFormat.getCurrentFormat())
		{
			String msg = "H2 database upgrade: the target file '" + targetFile + "' has format " + targetFormat + ", expected " + H2FileFormat.getCurrentFormat() + ". The original database file '" + dbFile + "' is untouched.";
			_logger.error(msg);
			throw new Exception(msg);
		}

		// Swap files: original -> backup, target -> original
		Files.move(dbFile.toPath(), backupFile.toPath());
		Files.move(targetFile.toPath(), dbFile.toPath());

		File traceFile = new File(dbBaseName + ".trace.db");
		if (traceFile.exists())
			Files.move(traceFile.toPath(), new File(traceFile.getAbsolutePath() + ".h2fmt" + fileFormat + "." + ts + ".bak").toPath());

		// The target trace file only has "expected" errors from the copy (like 'table not found' when checking if the target is empty)
		File targetTraceFile = new File(targetFile.getAbsolutePath().replace(H2FileFormat.MV_DB_SUFFIX, ".trace.db"));
		Files.deleteIfExists(targetTraceFile.toPath());

		_logger.info("#################################################################################");
		_logger.info("## H2 database UPGRADE was SUCCESSFUL, in " + TimeUtils.msDiffNowToTimeStr(startTime));
		_logger.info("##   Database: " + dbFile + " (" + StringUtil.bytesToHuman(dbFile.length()) + ", format " + targetFormat + ")");
		_logger.info("##   Backup:   " + backupFile + " (" + StringUtil.bytesToHuman(backupFile.length()) + ", format " + fileFormat + ")");
		_logger.info("## When you are happy with the upgraded database, remove the backup file to save space.");
		_logger.info("#################################################################################");
	}

	/**
	 * Find a H2 JAR that can read the old format
	 * <ul>
	 *   <li>Property: DbxTuneCentral.h2.oldFormat.jar.format.N</li>
	 *   <li>Search the directory where the current H2 JAR is located, and ${DBXTUNE_HOME}/lib</li>
	 * </ul>
	 */
	private static File findOldH2Jar(Configuration conf, int fileFormat)
	{
		String propVal = conf.getProperty(PROPKEY_jarPrefix + fileFormat);
		if (StringUtil.hasValue(propVal))
		{
			File f = new File(StringUtil.envVariableSubstitution(propVal));
			if (f.exists())
				return f;
			_logger.error("The H2 JAR specified in property '" + PROPKEY_jarPrefix + fileFormat + "' does not exist. file='" + f + "'.");
			return null;
		}

		// Search for h2-*.jar files, and ask each of them what file formats it can read
		List<File> dirs = new ArrayList<>();
		File currentH2Jar = H2FileFormat.getCurrentH2Jar();
		if (currentH2Jar != null)
			dirs.add(currentH2Jar.getParentFile());

		String dbxtuneHome = System.getProperty("DBXTUNE_HOME", System.getenv("DBXTUNE_HOME"));
		if (StringUtil.hasValue(dbxtuneHome))
		{
			File libDir = new File(dbxtuneHome, "lib");
			if ( ! dirs.contains(libDir.getAbsoluteFile()) )
				dirs.add(libDir.getAbsoluteFile());
		}

		H2JarInfo info = H2FileFormat.findH2JarForFormat(fileFormat, dirs);
		return info == null ? null : info.jarFile;
	}

	/**
	 * Log progress every 60 seconds, and warn if we are running low on disk space
	 */
	private static Thread startProgressWatchdog(File sourceFile, File targetFile, long spaceMarginMb)
	{
		Thread t = new Thread(() ->
		{
			long marginBytes = spaceMarginMb * 1024 * 1024;
			while ( ! Thread.currentThread().isInterrupted() )
			{
				try { Thread.sleep(60_000); }
				catch (InterruptedException ex) { break; }

				long targetSize = targetFile.length();
				long freeBytes  = targetFile.getAbsoluteFile().getParentFile().getUsableSpace();
				_logger.info("H2 database upgrade progress: target size " + StringUtil.bytesToHuman(targetSize) + " (source " + StringUtil.bytesToHuman(sourceFile.length()) + "), free disk space " + StringUtil.bytesToHuman(freeBytes) + ".");

				if (freeBytes < marginBytes)
					_logger.warn("H2 database upgrade: LOW DISK SPACE, only " + StringUtil.bytesToHuman(freeBytes) + " free in '" + targetFile.getAbsoluteFile().getParentFile() + "'.");
			}
		});
		t.setDaemon(true);
		t.setName("H2CentralDbAutoUpgrade-progress");
		t.start();
		return t;
	}
}
