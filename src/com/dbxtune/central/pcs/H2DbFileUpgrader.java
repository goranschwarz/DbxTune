/*******************************************************************************
 * Copyright (C) 2010-2027 Goran Schwarz
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

import com.dbxtune.central.pcs.H2CentralDbCopy3.CopyResult;
import com.dbxtune.central.pcs.H2CentralDbCopy3.DbType;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.H2FileFormat;
import com.dbxtune.utils.H2FileFormat.H2JarInfo;
import com.dbxtune.utils.H2FileFormat.SpaceCheckResult;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.TimeUtils;

/**
 * Upgrade a H2 database file written by an older H2 version (that the current H2 can't read) by copying it into a new file.
 * <p>
 * Used by: DbxCentral startup (the Central DB), DbxTune collectors (today's recording) and the batch tool {@link H2DbFileUpgradeTool}.
 * <p>
 * Steps: find an old H2 JAR that can read the file, check free disk space, copy all tables using {@link H2CentralDbCopy3} (with progress),
 * verify, and swap the files. After a successful verify, the original file is deleted or kept as a backup (<code>*.mv.db.h2fmt#.yyyyMMdd_HHmmss.bak</code>).
 * <p>
 * On any failure the original file is left untouched.<br>
 * This class does NOT throw exceptions, the caller decides what to do with the {@link Result}.
 */
public class H2DbFileUpgrader
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public static final double  DEFAULT_spaceFactor   = 1.0;
	public static final long    DEFAULT_spaceMarginMb = 1024;

	/** Options for the upgrade */
	public static class Options
	{
		/** Property prefix, used for 'jar.format.N' lookups and in messages. Example: "DbxTuneCentral.h2.oldFormat." */
		public String        propPrefix    = "";
		public Configuration conf          = null;

		/** Explicit old H2 JAR (for example from the command line), null = search for it */
		public File          oldH2Jar      = null;

		public double        spaceFactor   = DEFAULT_spaceFactor;
		public long          spaceMarginMb = DEFAULT_spaceMarginMb;

		/** true = keep the original file as a backup, false = delete it after a successful verify */
		public boolean       keepBackup    = false;

		public String        user          = "sa";
		public String        passwd        = "";

		/** How the user can change 'keepBackup', used in messages. null = the property: PREFIX + keepBackup=true */
		public String        keepBackupHint = null;

		public String getKeepBackupHint()
		{
			return keepBackupHint != null ? keepBackupHint : "property '" + propPrefix + "keepBackup=true'";
		}

		/**
		 * Read options from a Configuration: PREFIX + spaceFactor, spaceMarginMb, keepBackup (and jar.format.N at upgrade time)
		 */
		public static Options fromConfig(Configuration conf, String propPrefix, boolean defaultKeepBackup)
		{
			Options opt = new Options();
			opt.conf          = conf;
			opt.propPrefix    = propPrefix;
			opt.spaceFactor   = conf.getDoubleProperty (propPrefix + "spaceFactor",   DEFAULT_spaceFactor);
			opt.spaceMarginMb = conf.getLongProperty   (propPrefix + "spaceMarginMb", DEFAULT_spaceMarginMb);
			opt.keepBackup    = conf.getBooleanProperty(propPrefix + "keepBackup",    defaultKeepBackup);
			return opt;
		}
	}

	public enum Status
	{
		/** The file has a format the current H2 can read, nothing was done */
		NOT_NEEDED,
		/** The file was upgraded */
		UPGRADED,
		/** Could not find a H2 JAR that can read the old format */
		FAILED_NO_JAR,
		/** Not enough free disk space */
		FAILED_NO_SPACE,
		/** The copy failed (exception, or errors in the copy report) */
		FAILED_COPY,
		/** The copy looked OK, but the verification of the new file failed */
		FAILED_VERIFY
	};

	public static class Result
	{
		public Status           status;
		public String           message;

		public File             dbFile;
		public File             targetFile;
		public File             backupFile;  // null if the original was deleted
		public int              fromFormat;
		public int              toFormat;
		public long             sizeBefore;
		public long             sizeAfter;
		public long             durationMs;

		public CopyResult       copyResult;
		public SpaceCheckResult spaceResult;

		public boolean isOk() { return Status.NOT_NEEDED.equals(status) || Status.UPGRADED.equals(status); }
	}

	/**
	 * Upgrade the H2 database file, if it's needed.
	 *
	 * @param dbFile          The H2 database file (*.mv.db)
	 * @param expectedDbType  What type of database we expect, null = any known type (DbxCentral or DbxTune recording)
	 * @param opt             Options
	 * @return A result (never null)
	 */
	public static Result upgrade(File dbFile, DbType expectedDbType, Options opt)
	{
		long startTime = System.currentTimeMillis();

		Result res = new Result();
		res.dbFile     = dbFile;
		res.fromFormat = H2FileFormat.readFormat(dbFile);
		res.toFormat   = H2FileFormat.getCurrentFormat();
		res.sizeBefore = dbFile.length();

		if ( ! H2FileFormat.needsUpgrade(dbFile) )
		{
			res.status  = Status.NOT_NEEDED;
			res.message = "No upgrade needed. " + H2FileFormat.describe(dbFile);
			return res;
		}

		String manualHint = "The database can be upgraded manually with: dbxtune.sh h2upgrade -f '" + dbFile + "' -e";

		// Find a H2 JAR that can read the old format
		File oldH2Jar = findOldH2Jar(opt, res.fromFormat);
		if (oldH2Jar == null)
		{
			res.status  = Status.FAILED_NO_JAR;
			res.message = "Can't find a H2 JAR file that can read H2 file format " + res.fromFormat + ". Put one in the 'lib' directory, or set property '" + opt.propPrefix + "jar.format." + res.fromFormat + "=/path/to/h2-x.y.z.jar'.";
			_logger.error(res.message);
			return res;
		}
		_logger.info("Using old H2 JAR '" + oldH2Jar + "' to read the old database '" + dbFile + "'.");

		// Check that we have enough disk space
		res.spaceResult = H2FileFormat.checkFreeSpace(dbFile, opt.spaceFactor, opt.spaceMarginMb);
		if ( ! res.spaceResult.ok )
		{
			res.status  = Status.FAILED_NO_SPACE;
			res.message = "Not enough free disk space to upgrade the H2 database file '" + dbFile + "'. Needed " + StringUtil.bytesToHuman(res.spaceResult.neededBytes) + ", free " + StringUtil.bytesToHuman(res.spaceResult.freeBytes) + ".";

			_logger.error("#################################################################################");
			for (String line : res.spaceResult.message.split("\n"))
				_logger.error("## " + line);
			_logger.error("## After freeing up space, the upgrade will be retried at next start.");
			_logger.error("## Or upgrade manually (possibly to another disk). " + manualHint);
			_logger.error("#################################################################################");
			return res;
		}
		_logger.info("Disk space check OK: needed " + StringUtil.bytesToHuman(res.spaceResult.neededBytes) + ", free " + StringUtil.bytesToHuman(res.spaceResult.freeBytes) + ".");

		// Target file: always a fresh one
		String ts = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		String dbBaseName = dbFile.getAbsolutePath().substring(0, dbFile.getAbsolutePath().length() - H2FileFormat.MV_DB_SUFFIX.length());
		res.targetFile = new File(dbBaseName + "_H2UPGRADE_" + ts + H2FileFormat.MV_DB_SUFFIX);

		_logger.info("#################################################################################");
		_logger.info("## Starting H2 database UPGRADE, this may take a while (depending on the database size).");
		_logger.info("##   Source:   " + dbFile + " (" + StringUtil.bytesToHuman(res.sizeBefore) + ", format " + res.fromFormat + ")");
		_logger.info("##   Target:   " + res.targetFile);
		_logger.info("##   Old JAR:  " + oldH2Jar);
		_logger.info("##   Original: " + (opt.keepBackup ? "will be kept as a backup" : "will be DELETED after a successful verify") + " (" + opt.getKeepBackupHint() + ")");
		_logger.info("#################################################################################");

		Thread watchdog = startProgressWatchdog(dbFile, res.targetFile, opt.spaceMarginMb);
		try
		{
			res.copyResult = H2CentralDbCopy3.upgradeFromOldH2(dbFile, oldH2Jar, res.targetFile, opt.user, opt.passwd, expectedDbType);
		}
		catch (Exception ex)
		{
			res.status  = Status.FAILED_COPY;
			res.message = "H2 database upgrade FAILED. The original database file '" + dbFile + "' is untouched. The partial target '" + res.targetFile + "' is kept for inspection (remove it before a retry to save space). Caught: " + ex;
			_logger.error(res.message, ex);
			return res;
		}
		finally
		{
			watchdog.interrupt();
		}
		_logger.info("H2 database copy result: " + res.copyResult);

		if ( ! res.copyResult.ok )
		{
			res.status  = Status.FAILED_COPY;
			res.message = "H2 database upgrade had ERRORS (see the error report above). The original database file '" + dbFile + "' is untouched. The target '" + res.targetFile + "' is kept for inspection.";
			_logger.error(res.message);
			return res;
		}

		// Verify
		int targetFormat = H2FileFormat.readFormat(res.targetFile);
		String verifyProblem = null;
		if (targetFormat != H2FileFormat.getCurrentFormat())
			verifyProblem = "the target file has format " + targetFormat + ", expected " + H2FileFormat.getCurrentFormat();
		else if (res.copyResult.tables <= 0)
			verifyProblem = "no tables was copied";
		else if (res.copyResult.sourceRows != res.copyResult.targetRows)
			verifyProblem = "row count mismatch: source rows " + res.copyResult.sourceRows + ", target rows " + res.copyResult.targetRows;

		if (verifyProblem != null)
		{
			res.status  = Status.FAILED_VERIFY;
			res.message = "H2 database upgrade: verify FAILED, " + verifyProblem + ". The original database file '" + dbFile + "' is untouched. The target '" + res.targetFile + "' is kept for inspection.";
			_logger.error(res.message);
			return res;
		}
		_logger.info("H2 database upgrade: verify OK. format=" + targetFormat + ", tables=" + res.copyResult.tables + ", rows=" + res.copyResult.targetRows);

		// Swap files: original -> backup (or deleted), target -> original
		try
		{
			File traceFile       = new File(dbBaseName + ".trace.db");
			File targetTraceFile = new File(res.targetFile.getAbsolutePath().replace(H2FileFormat.MV_DB_SUFFIX, ".trace.db"));

			if (opt.keepBackup)
			{
				res.backupFile = new File(dbFile.getAbsolutePath() + ".h2fmt" + res.fromFormat + "." + ts + ".bak");
				Files.move(dbFile.toPath(), res.backupFile.toPath());
				if (traceFile.exists())
					Files.move(traceFile.toPath(), new File(traceFile.getAbsolutePath() + ".h2fmt" + res.fromFormat + "." + ts + ".bak").toPath());
			}
			else
			{
				Files.delete(dbFile.toPath());
				Files.deleteIfExists(traceFile.toPath());
			}
			Files.move(res.targetFile.toPath(), dbFile.toPath());

			// The target trace file only has "expected" errors from the copy (like 'table not found' when checking if the target is empty)
			Files.deleteIfExists(targetTraceFile.toPath());
		}
		catch (Exception ex)
		{
			res.status  = Status.FAILED_COPY;
			res.message = "H2 database upgrade: problems when swapping files. Check the files manually: original='" + dbFile + "', new='" + res.targetFile + "', backup='" + res.backupFile + "'. Caught: " + ex;
			_logger.error(res.message, ex);
			return res;
		}

		res.sizeAfter  = dbFile.length();
		res.durationMs = System.currentTimeMillis() - startTime;
		res.status     = Status.UPGRADED;
		res.message    = "H2 database UPGRADE was SUCCESSFUL, in " + TimeUtils.msToTimeStrDHMS(res.durationMs) + ". '" + dbFile + "' format " + res.fromFormat + " -> " + res.toFormat
				+ ", size " + StringUtil.bytesToHuman(res.sizeBefore) + " -> " + StringUtil.bytesToHuman(res.sizeAfter) + ", tables=" + res.copyResult.tables + ", rows=" + res.copyResult.targetRows + ".";

		_logger.info("#################################################################################");
		_logger.info("## " + res.message);
		if (res.backupFile != null)
		{
			_logger.info("##   Backup:   " + res.backupFile + " (" + StringUtil.bytesToHuman(res.backupFile.length()) + ", format " + res.fromFormat + ")");
			_logger.info("## When you are happy with the upgraded database, remove the backup file to save space.");
		}
		else
		{
			_logger.info("##   The original file was deleted (after a successful verify). To keep it: " + opt.getKeepBackupHint());
		}
		_logger.info("#################################################################################");
		return res;
	}

	/**
	 * Find a H2 JAR that can read the old format
	 * <ul>
	 *   <li>Options.oldH2Jar (for example from the command line)</li>
	 *   <li>Property: PREFIX + jar.format.N</li>
	 *   <li>Search the directory where the current H2 JAR is located, and ${DBXTUNE_HOME}/lib</li>
	 * </ul>
	 */
	public static File findOldH2Jar(Options opt, int fileFormat)
	{
		if (opt.oldH2Jar != null)
		{
			if (opt.oldH2Jar.exists())
				return opt.oldH2Jar;
			_logger.error("The specified H2 JAR does not exist. file='" + opt.oldH2Jar + "'.");
			return null;
		}

		String propVal = opt.conf == null ? null : opt.conf.getProperty(opt.propPrefix + "jar.format." + fileFormat);
		if (StringUtil.hasValue(propVal))
		{
			File f = new File(StringUtil.envVariableSubstitution(propVal));
			if (f.exists())
				return f;
			_logger.error("The H2 JAR specified in property '" + opt.propPrefix + "jar.format." + fileFormat + "' does not exist. file='" + f + "'.");
			return null;
		}

		// Search for h2-*.jar files, and ask each of them what file formats it can read
		List<File> dirs = new ArrayList<>();
		File currentH2Jar = H2FileFormat.getCurrentH2Jar();
		if (currentH2Jar != null)
			dirs.add(currentH2Jar.getAbsoluteFile().getParentFile());

		String dbxtuneHome = System.getProperty("DBXTUNE_HOME", System.getenv("DBXTUNE_HOME"));
		if (StringUtil.hasValue(dbxtuneHome))
		{
			File libDir = new File(dbxtuneHome, "lib").getAbsoluteFile();
			if ( ! dirs.contains(libDir) )
				dirs.add(libDir);
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
		t.setName("H2DbFileUpgrader-progress");
		t.start();
		return t;
	}
}
