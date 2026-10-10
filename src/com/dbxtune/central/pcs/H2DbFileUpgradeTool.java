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
import java.io.PrintWriter;
import java.lang.invoke.MethodHandles;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.Version;
import com.dbxtune.central.pcs.H2DbFileUpgrader.Result;
import com.dbxtune.central.pcs.H2DbFileUpgrader.Status;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.H2FileFormat;
import com.dbxtune.utils.Logging;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.TimeUtils;

/**
 * Batch tool to upgrade H2 database files (DbxTune recordings and the DbxCentral database)
 * written by an older H2 version, that the current H2 can't read.
 * <p>
 * Without '-e' it only lists what would be done (dry run).
 * <p>
 * Usage: <code>dbxtune.sh h2upgrade [-R dir] [-f file.mv.db]... [-k] [-J oldH2.jar] [-e]</code>
 */
public class H2DbFileUpgradeTool
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	/** What we plan to do with a file */
	private enum Action { UPGRADE, OK, TOO_NEW, NOT_H2, IN_USE, NO_JAR }

	private static class FileEntry
	{
		File   file;
		long   size;
		int    format;
		Action action;
		String info = "";
		Result result;
	}

	public static void printHelp(Options options, String errorStr)
	{
		PrintWriter pw = new PrintWriter(System.out);

		if (errorStr != null)
		{
			pw.println();
			pw.println(errorStr);
			pw.println();
		}

		pw.println("usage: h2upgrade [-h] [-R <dir>] [-f <file.mv.db>]... [-k] [-J <jarFile>] [-e] [-L <logfile>]");
		pw.println("  ");
		pw.println("Upgrade H2 database files (DbxTune recordings and the DbxCentral database) written by an older H2 version,");
		pw.println("that the current H2 (" + H2FileFormat.getCurrentH2Version() + ") can't read. Each file is copied into a new file (all tables), and verified.");
		pw.println("  ");
		pw.println("options:");
		pw.println("  -h,--help                 Usage information.");
		pw.println("  -R,--savedir <dirname>    Directory with H2 database files (*.mv.db). Default: ${DBXTUNE_SAVE_DIR} or ~/.dbxtune/dbxc/data");
		pw.println("  -f,--file <filename>      Only this file (can be specified several times). Default: all *.mv.db files in the directory");
		pw.println("  -k,--keepBackup           Keep the original file as a backup '*.mv.db.h2fmt#.yyyyMMdd_HHmmss.bak'");
		pw.println("                            Default: the original file is DELETED after a successful copy and verify.");
		pw.println("  -J,--oldH2Jar <jarFile>   H2 JAR that can read the old files. Default: search for h2-*.jar in the lib directory");
		pw.println("  -e,--exec                 Execute the upgrade. If not specified: only list what would be done.");
		pw.println("  -L,--logfile <filename>   Name of the logfile.");
		pw.println("  ");
		pw.println("Files that are in use (for example today's recording of a running collector) are skipped.");
		pw.println("  ");
		pw.flush();
	}

	public static Options buildCommandLineOptions()
	{
		Options options = new Options();
		options.addOption( Option.builder("h").longOpt("help"      ).hasArg(false).build() );
		options.addOption( Option.builder("R").longOpt("savedir"   ).hasArg(true ).build() );
		options.addOption( Option.builder("f").longOpt("file"      ).hasArg(true ).build() );
		options.addOption( Option.builder("k").longOpt("keepBackup").hasArg(false).build() );
		options.addOption( Option.builder("J").longOpt("oldH2Jar"  ).hasArg(true ).build() );
		options.addOption( Option.builder("e").longOpt("exec"      ).hasArg(false).build() );
		options.addOption( Option.builder("L").longOpt("logfile"   ).hasArg(true ).build() );
		return options;
	}

	/**
	 * Check if a file is in use by another process (for example a running collector or DbxCentral)<br>
	 * H2 holds a file lock on open databases.
	 */
	private static boolean isInUse(File f)
	{
		try (FileChannel ch = FileChannel.open(f.toPath(), StandardOpenOption.READ, StandardOpenOption.WRITE))
		{
			FileLock lock = ch.tryLock();
			if (lock == null)
				return true;
			lock.release();
			return false;
		}
		catch (Exception ex)
		{
			return true;
		}
	}

	private static List<FileEntry> getFiles(CommandLine cmd)
	{
		List<File> files = new ArrayList<>();
		if (cmd.hasOption('f'))
		{
			for (String fn : cmd.getOptionValues('f'))
				files.add(new File(fn).getAbsoluteFile());
		}
		else
		{
			String dirName = cmd.getOptionValue('R', System.getProperty("DBXTUNE_SAVE_DIR", System.getenv("DBXTUNE_SAVE_DIR")));
			if (StringUtil.isNullOrBlank(dirName))
				dirName = System.getProperty("user.home") + File.separatorChar + ".dbxtune" + File.separatorChar + "dbxc" + File.separatorChar + "data";

			File dir = new File(dirName).getAbsoluteFile();
			_logger.info("Searching for H2 database files (*" + H2FileFormat.MV_DB_SUFFIX + ") in directory '" + dir + "'.");
			File[] dirFiles = dir.listFiles((d, name) -> name.endsWith(H2FileFormat.MV_DB_SUFFIX));
			if (dirFiles == null)
				_logger.error("Directory '" + dir + "' does not exist or can't be read.");
			else
				files.addAll(Arrays.asList(dirFiles));
		}
		files.sort((a, b) -> a.getName().compareTo(b.getName()));

		// Classify each file
		Map<Integer, File> jarForFormat = new LinkedHashMap<>();
		H2DbFileUpgrader.Options jarOpt = new H2DbFileUpgrader.Options();
		if (cmd.hasOption('J'))
			jarOpt.oldH2Jar = new File(cmd.getOptionValue('J'));

		List<FileEntry> list = new ArrayList<>();
		for (File f : files)
		{
			FileEntry fe = new FileEntry();
			fe.file   = f;
			fe.size   = f.length();

			// Check "in use" first: on Windows a locked file can't even be read (so we can't read the header)
			if (f.exists() && isInUse(f))
			{
				fe.format = -1;
				fe.action = Action.IN_USE;
				fe.info   = "file is locked by another process (a running collector or DbxCentral?), or not writable";
				list.add(fe);
				continue;
			}

			fe.format = f.exists() ? H2FileFormat.readFormat(f) : -1;

			if (fe.format <= 0)
			{
				fe.action = Action.NOT_H2;
				fe.info   = f.exists() ? "not a H2 MVStore file" : "file does not exist";
			}
			else if (H2FileFormat.isTooNew(f))
			{
				fe.action = Action.TOO_NEW;
				fe.info   = "written by a newer H2 version";
			}
			else if ( ! H2FileFormat.needsUpgrade(f) )
			{
				fe.action = Action.OK;
			}
			else
			{
				File jar = jarForFormat.computeIfAbsent(fe.format, fmt -> H2DbFileUpgrader.findOldH2Jar(jarOpt, fmt));
				if (jar == null)
				{
					fe.action = Action.NO_JAR;
					fe.info   = "no H2 JAR found that can read format " + fe.format + " (use -J)";
				}
				else
				{
					fe.action = Action.UPGRADE;
					fe.info   = "using " + jar.getName();
				}
			}
			list.add(fe);
		}
		return list;
	}

	private static void printFileList(List<FileEntry> list)
	{
		long upgradeCount = 0;
		long upgradeBytes = 0;
		long maxBytes     = 0;

		_logger.info("");
		_logger.info(String.format("%-4s %-50s %12s %6s %-8s %s", "#", "File", "Size", "Format", "Action", "Info"));
		_logger.info(StringUtil.replicate("-", 120));
		int row = 0;
		for (FileEntry fe : list)
		{
			row++;
			_logger.info(String.format("%-4d %-50s %12s %6s %-8s %s", row, fe.file.getName(), StringUtil.bytesToHuman(fe.size), fe.format, fe.action, fe.info));
			if (Action.UPGRADE.equals(fe.action))
			{
				upgradeCount++;
				upgradeBytes += fe.size;
				maxBytes = Math.max(maxBytes, fe.size);
			}
		}
		_logger.info(StringUtil.replicate("-", 120));
		_logger.info("Files: " + list.size() + ", to upgrade: " + upgradeCount + " (" + StringUtil.bytesToHuman(upgradeBytes) + "). Current H2 " + H2FileFormat.getCurrentH2Version() + " writes format " + H2FileFormat.getCurrentFormat() + ".");
		if (upgradeCount > 0)
			_logger.info("Files are upgraded one at a time, the largest file needs about " + StringUtil.bytesToHuman(maxBytes) + " (+ margin) of free disk space while copying.");
		_logger.info("");
	}

	public static void main(String[] args)
	{
		Version.setAppName("H2DbFileUpgrade");
		Options options = buildCommandLineOptions();
		CommandLine cmd;
		try
		{
			cmd = new DefaultParser().parse(options, args);
		}
		catch (ParseException ex)
		{
			printHelp(options, "Error: " + ex.getMessage());
			System.exit(1);
			return;
		}
		if (cmd.hasOption('h') || (cmd.getArgs() != null && cmd.getArgs().length > 0))
		{
			printHelp(options, cmd.hasOption('h') ? null : "Unknown options: " + Arrays.toString(cmd.getArgs()));
			System.exit(cmd.hasOption('h') ? 0 : 1);
			return;
		}

		Logging.init(null, (String) null, cmd.getOptionValue('L'));
		Configuration.setInstance(Configuration.SYSTEM_CONF, new Configuration());

		List<FileEntry> list = getFiles(cmd);
		printFileList(list);

		if ( ! cmd.hasOption('e') )
		{
			_logger.info("This was a DRY RUN, nothing was changed. To execute the upgrade, add switch '-e'.");
			System.exit(0);
			return;
		}

		// Execute
		List<FileEntry> toUpgrade = new ArrayList<>();
		for (FileEntry fe : list)
			if (Action.UPGRADE.equals(fe.action))
				toUpgrade.add(fe);

		H2DbFileUpgrader.Options opt = new H2DbFileUpgrader.Options();
		opt.keepBackup     = cmd.hasOption('k');
		opt.keepBackupHint = "command line switch '-k'";
		if (cmd.hasOption('J'))
			opt.oldH2Jar = new File(cmd.getOptionValue('J'));

		long startTime = System.currentTimeMillis();
		long bytesBefore = 0;
		long bytesAfter  = 0;
		int  cnt = 0;
		boolean stopped = false;
		for (FileEntry fe : toUpgrade)
		{
			cnt++;
			_logger.info("");
			_logger.info("=================================================================================");
			_logger.info("[" + cnt + "/" + toUpgrade.size() + "] " + fe.file.getName() + " (" + StringUtil.bytesToHuman(fe.size) + ") format " + fe.format + " -> " + H2FileFormat.getCurrentFormat());
			_logger.info("=================================================================================");

			fe.result = H2DbFileUpgrader.upgrade(fe.file, null, opt);

			if (Status.UPGRADED.equals(fe.result.status))
			{
				bytesBefore += fe.result.sizeBefore;
				bytesAfter  += fe.result.sizeAfter;
				double mbPerSec = fe.result.durationMs <= 0 ? 0 : (fe.result.sizeBefore / 1024.0 / 1024.0) / (fe.result.durationMs / 1000.0);
				_logger.info(String.format("[%d/%d] DONE: %s in %s (%.1f MB/s), size %s -> %s, tables=%d, rows=%d",
						cnt, toUpgrade.size(), fe.file.getName(), TimeUtils.msToTimeStrDHMS(fe.result.durationMs), mbPerSec,
						StringUtil.bytesToHuman(fe.result.sizeBefore), StringUtil.bytesToHuman(fe.result.sizeAfter), fe.result.copyResult.tables, fe.result.copyResult.targetRows));
			}
			else
			{
				_logger.error("[" + cnt + "/" + toUpgrade.size() + "] FAILED (" + fe.result.status + "): " + fe.file.getName() + ". " + fe.result.message);

				// No point to continue if we are out of disk space
				if (Status.FAILED_NO_SPACE.equals(fe.result.status))
				{
					_logger.error("Stopping: not enough disk space. Free up space (see the list above) and run the tool again.");
					stopped = true;
					break;
				}
			}
		}

		// Summary
		int ok = 0, failed = 0, notDone = 0;
		_logger.info("");
		_logger.info("=================================================================================");
		_logger.info(" Summary");
		_logger.info("=================================================================================");
		for (FileEntry fe : toUpgrade)
		{
			String status;
			if      (fe.result == null)                              { status = "NOT DONE"; notDone++; }
			else if (Status.UPGRADED.equals(fe.result.status))       { status = "UPGRADED"; ok++; }
			else                                                     { status = "FAILED (" + fe.result.status + ")"; failed++; }
			_logger.info(String.format("  %-50s %s", fe.file.getName(), status));
		}
		int skipped = 0;
		for (FileEntry fe : list)
			if ( ! Action.UPGRADE.equals(fe.action) && ! Action.OK.equals(fe.action) )
				skipped++;
		_logger.info("---------------------------------------------------------------------------------");
		_logger.info(" Upgraded: " + ok + ", Failed: " + failed + ", Not done: " + notDone + ", Skipped (in use / no jar / not H2): " + skipped);
		_logger.info(" Disk usage of upgraded files: " + StringUtil.bytesToHuman(bytesBefore) + " -> " + StringUtil.bytesToHuman(bytesAfter));
		_logger.info(" Total time: " + TimeUtils.msToTimeStrDHMS(System.currentTimeMillis() - startTime) + (stopped ? " (STOPPED early)" : ""));
		_logger.info(" Originals: " + (opt.keepBackup ? "kept as '*.h2fmt#.<ts>.bak'" : "deleted after a successful verify (use -k to keep them)"));
		_logger.info("=================================================================================");

		System.exit(failed > 0 || notDone > 0 ? 1 : 0);
	}
}
