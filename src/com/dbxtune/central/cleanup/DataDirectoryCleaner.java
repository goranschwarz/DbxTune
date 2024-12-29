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
package com.dbxtune.central.cleanup;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.dbxtune.central.DbxTuneCentral;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.FileUtils;
import com.dbxtune.utils.NumberUtils;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.TimeUtils;

import it.sauronsoftware.cron4j.Task;
import it.sauronsoftware.cron4j.TaskExecutionContext;

/**
 * Cleanup Data Directory
 * <p>
 * Remove oldest H2 databases files.<br>
 * The idea is to remove oldest H2 DB files.<br>
 * 
 * Try to estimate how much space we will be needing for one or two days.<br>
 * If we need space, remove some files.
 * 
 * @author gorans
 *
 */
// EXTRA LOG4J FILE: https://www.calazan.com/how-to-log-to-multiple-log-files-with-log4j/

public class DataDirectoryCleaner
extends Task
{
	private static Logger _logger = Logger.getLogger(DataDirectoryCleaner.class);

	private final String DATA_DIR = DbxTuneCentral.getAppDataDir();

	public static final String  PROPKEY_start = "DataDirectoryCleaner.start";
	public static final boolean DEFAULT_start = true;

	public static final String  PROPKEY_cron = "DataDirectoryCleaner.cron";
	public static final String  DEFAULT_cron = "54 23 * * *";
//	public static final String  DEFAULT_cron = "* * * * *";

	public static final String  PROPKEY_multiplyFactor = "DataDirectoryCleaner.multiply.factor";
//	public static final double  DEFAULT_multiplyFactor = 2.0;
	public static final double  DEFAULT_multiplyFactor = 1.2;

	public static final String  PROPKEY_printSpaceInfo = "DataDirectoryCleaner.print.space.info";
	public static final boolean DEFAULT_printSpaceInfo = true;

	public static final String  PROPKEY_maxHistoricalSpaceUsageInGb  = "DataDirectoryCleaner.max.historical.space.usage.GB";
	public static final int     DEFAULT_maxHistoricalSpaceUsageInGb  = -1; // -1 == DISABLED

	public static final String  PROPKEY_maxHistoricalSpaceUsageInPct = "DataDirectoryCleaner.max.historical.space.usage.pct";
	public static final double  DEFAULT_maxHistoricalSpaceUsageInPct = 94.0; // -1 == DISABLED

	public static final String  PROPKEY_LOG_FILE_PATTERN = "DataDirectoryCleaner.log.file.pattern";
	public static final String  DEFAULT_LOG_FILE_PATTERN    = "%d - %-5p - %m%n";
	
	public static final String  PROPKEY_dryRun = "DataDirectoryCleaner.dryRun";
	public static final boolean DEFAULT_dryRun = false;
//	public static final boolean DEFAULT_dryRun = true; // For test purposes

	public static final String  PROPKEY_savedFileInfo_filename = "DataDirectoryCleaner.savedFileInfo.filename";
	public static final String  DEFAULT_savedFileInfo_filename = "DbxTune.pcs.savedFileInfo.properties";
	
	public static final String EXTRA_LOG_NAME = DataDirectoryCleaner.class.getSimpleName() + "-TaskLogger";

	private static final String _prefix = "DATA-DIR-CLEANUP: ";
	
	private boolean _dryRun = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_dryRun, DEFAULT_dryRun);
	
	private static long _lastExec = 0;
	private static long _lastExecThreshold = 300*1000; // 5 minutes 

	private static String _lastExecShortReport = "";
	
	private Configuration _savedFileInfo = null;

	public static long getH2RecodingFileSizeMb()
	{
		DataDirectoryCleaner ddc = new DataDirectoryCleaner();
		
		Map<String, List<FileInfo>> srvMap = ddc.getFilesByServerName();
		long       sumHistoryDbFileUsageMb = ddc.getSumSizeMb(srvMap, SizeType.FILE_INFO);

		return sumHistoryDbFileUsageMb;
	}

	private List<String> getFilesH2Dbs()
	{
		String directory = DATA_DIR;

		List<String> fileNames = new ArrayList<>();
		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(directory)))
		{
			for (Path path : directoryStream)
			{
				if (path.toString().endsWith(".mv.db"))
					fileNames.add(path.toString());
			}
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
		}

		// Sort the list
		Collections.sort(fileNames);
		
		return fileNames;
	}

	private Map<String, List<FileInfo>> getFilesByServerName()
	{
		List<FileInfo> files = getTimestampedDbFiles();

		Map<String, List<FileInfo>> map = new LinkedHashMap<>();
		
		for (FileInfo fi : files)
		{
			String[] sa = fi._file.getName().split("_[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]");
			String srvName = sa[0];
			
			List<FileInfo> list = map.get(srvName);
			if (list == null)
				list = new ArrayList<>();
			
			list.add(fi);
			map.put(srvName, list);
		}
		return map;
	}
	
	private Map<String, List<FileInfo>> getFilesByTimeStamp()
	{
		List<FileInfo> files = getTimestampedDbFiles();

		Map<String, List<FileInfo>> map = new LinkedHashMap<>();

		for (FileInfo fi : files)
		{
			String fname = fi._file.getName();
			int p = fname.lastIndexOf("_");
			if (p != -1)
			{
				String ts = fname.substring(p+1, fname.length()-".MV.DB".length());
				
				List<FileInfo> list = map.get(ts);
				if (list == null)
					list = new ArrayList<>();
				
				list.add(fi);
				map.put(ts, list);
			}
		}
		return map;
	}
	
	private List<FileInfo> getTimestampedDbFiles()
	{
		List<String> files = getFilesH2Dbs();
		List<FileInfo> output = new ArrayList<>();

		for (String file : files)
		{
			File f = new File(file);
			if (f.exists() && f.isFile())
			{
				if (f.getName().toUpperCase().matches(".*_[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9].MV.DB"))
				{
					FileInfo fi = new FileInfo(f);
					output.add(fi);
				}
			}
		}
		return output;
	}

	
	private long getSumSizeMb(Map<String, List<FileInfo>> map, SizeType type)
	{
		long sum = 0;
		
		for (List<FileInfo> list : map.values())
		{
			for (FileInfo fi : list)
			{
				if      (SizeType.MAX_FILE_OR_SAVED.equals(type)) sum += fi.getMaxSize();
				else if (SizeType.FILE_INFO        .equals(type)) sum += fi.getFileSize();
				else if (SizeType.SAVED_INFO       .equals(type)) sum += fi.getSavedSize();
			}
		}
		
		return sum / 1024 / 1024;
	}

	private long getMaxSizeMb(Map<String, List<FileInfo>> map, SizeType type)
	{
		long maxSize = 0;

		for (List<FileInfo> list : map.values())
		{
			long sum = 0;
			for (FileInfo fi : list)
			{
				if      (SizeType.MAX_FILE_OR_SAVED.equals(type)) sum += fi.getMaxSize();
				else if (SizeType.FILE_INFO        .equals(type)) sum += fi.getFileSize();
				else if (SizeType.SAVED_INFO       .equals(type)) sum += fi.getSavedSize();
			}

			maxSize = Math.max(maxSize, sum);
		}
		
		return maxSize / 1024 / 1024;
	}

	private long getSizeMb(List<FileInfo> list, SizeType type)
	{
		long sum = 0;
		for (FileInfo fi : list)
		{
			if      (SizeType.MAX_FILE_OR_SAVED.equals(type)) sum += fi.getMaxSize();
			else if (SizeType.FILE_INFO        .equals(type)) sum += fi.getFileSize();
			else if (SizeType.SAVED_INFO       .equals(type)) sum += fi.getSavedSize();
		}

		return sum / 1024 / 1024;
	}

	public enum SizeType
	{
		MAX_FILE_OR_SAVED, FILE_INFO, SAVED_INFO
	};
	
//	private long getSizeGb(List<FileInfo> list)
//	{
//		return getSizeMb(list) / 1024;
//	}

	private class FileInfo
	{
		private File _file; 
		private Path _path; 
		private long _savedFileSize; // This is information from the "_savedFileInfo" or the properties file where we save the file size before DB is compacted
		private long _actualFileSize;
		
		/** Create a new FileInfo */
		public FileInfo(File f)
		{
			init(f, null);
		}

		/** Create a new FileInfo */
		public FileInfo(Path path)
		{
			init(null, path);
		}

		/** simply called from the constructors */
		private void init(File f, Path p)
		{
			if (f == null && p == null)
				throw new NullPointerException("both File andPath can not be null");

			_file = f;
			_path = p;

			// Create the PATH object from the File
			if (f != null)
				_path = f.toPath();

			// Create the FILE object from the Path
			if (p != null)
				_file = p.toFile();
			
			_savedFileSize = -1;
			_actualFileSize = _file.length();

			if (_file.exists() && _savedFileInfo != null)
			{
				String key = _file.getName();
				long savedSize = _savedFileInfo.getLongProperty(key, -1);

				// Set the saved to be maximum of saved and current size
				_savedFileSize = Math.max(savedSize, _file.length());
				
				// set the size again (the current-size might be larger than the saved-size)
				_savedFileInfo.setProperty(key, _savedFileSize);
			}
			
		}

//		/** Create a new FileInfo */
//		public FileInfo(String fileStr)
//		{
//			this(new File(fileStr));
//		}
		
		/**
		 * Get MAX Size of the file.'
		 * <p>
		 * This would be either:
		 * <ul>
		 *   <li>The Current file size</li>
		 *   <li>Or the previously saved size, if that is bigger</li>
		 * </ul>
		 * @return
		 */
		public long getMaxSize()
		{
			return Math.max(_savedFileSize, _file.length());
		}
		
		/**
		 * Simply calls <code>_file.length()</code>
		 * @return
		 */
		public long getFileSize()
		{
			return _file.length();
		}
		/**
		 * Returns information from the "_savedFileInfo" or the properties file where we save the file size before DB is compacted
		 * @return
		 */
		public long getSavedSize()
		{
			return _savedFileSize;
		}
		/**
		 * Get file size (how big was the file, _file.length()) when this object was created
		 * @return
		 */
		public long getActualFileSize()
		{
			return _actualFileSize;
		}
	}
	
	public static String getLastExecShortReport()
	{
		if (_lastExecShortReport == null)
			return "";

		return _lastExecShortReport;
	}

	public static void clearLastExecShortReport()
	{
		_lastExecShortReport = "";
	}

	private static void appendToLastExecShortReport(String str)
	{
		if (_lastExecShortReport == null)
			_lastExecShortReport = "";

		_lastExecShortReport += str + "\n";
	}

	@Override
	public void execute(TaskExecutionContext context) throws RuntimeException
	{
		long timeSinceLastExec = System.currentTimeMillis() - _lastExec;
		if (timeSinceLastExec < _lastExecThreshold)
		{
			_logger.info("Skipping 'Data Directory Cleanup' task since it was only " + TimeUtils.msToTimeStr("%MM:%SS", timeSinceLastExec) + " (MM:SS) since last cleanup. Minimum time between cleanups is " + TimeUtils.msToTimeStr("%MM:%SS", _lastExecThreshold) + " (MM:SS).");
			return;
		}

		// Reset "Last Small Report" (used by the Daily Summary Report)
		_lastExecShortReport = "";

		_logger.info(               "");
		_logger.info(               "#############################################################################################");
		_logger.info(               "Begin task: Data Directory Cleanup");
		appendToLastExecShortReport("Begin task: Data Directory Cleanup");
		
		check(_dryRun);
		_lastExec = System.currentTimeMillis();
		
		//----------------------------------------------------------------
		// Maybe check if we have any open file descriptors
		// if we have to many open files (which means that we have been unsuccessful in shuting down H2), we might need to restart ourself...
		// NOTE: H2 1.4.197 seems a bit unstable when it comes to closing files... Hopefully it's not my code that forgets to close files...
		//----------------------------------------------------------------
		// Or we could check if spaceLeft is LOW  && that the cleanup did not decrease in size, then we might need to kill-or-restart any of the collectors holding the files that are already deleted.
		//----------------------------------------------------------------
		// on Linux it might be done with : lsof ... or ls _afl /proc/self/fd/ | grep deleted
		// or maybe: JMX --- UnixOperatingSystemMXBean 
		//     OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
		//     if(os instanceof UnixOperatingSystemMXBean)
		//         System.out.println("Number of open fd: " + ((UnixOperatingSystemMXBean) os).getOpenFileDescriptorCount());

		_logger.info(               "End task: Data Directory Cleanup");
		appendToLastExecShortReport("End task: Data Directory Cleanup");
	}

	private void check(boolean dryRun)
	{
		String logMsg;
		File dataDir = new File(DATA_DIR);
		File dataDirRes  = dataDir;
		try { dataDirRes = dataDir.toPath().toRealPath().toFile(); } catch(IOException ex) { _logger.warn("Problems resolving File->Path->File");}

		// How many GB could the historical Databases take
		// -1 == DISABLED
		boolean doCleanupDueToExceedingMaxHistorySpace = false;
		long    needSpaceInMb_forExceedingMaxHistorySpace = 0;
		long    cfg_maxHistorySpaceUsageInGb  = Configuration.getCombinedConfiguration().getIntProperty   (PROPKEY_maxHistoricalSpaceUsageInGb , DEFAULT_maxHistoricalSpaceUsageInGb);
		double  cfg_maxHistorySpaceUsageInPct = Configuration.getCombinedConfiguration().getDoubleProperty(PROPKEY_maxHistoricalSpaceUsageInPct, DEFAULT_maxHistoricalSpaceUsageInPct);

		// Create a new Configuration object, which holds entries for 'SavedFileInfo'
		String fileName = dataDirRes.getAbsolutePath() + File.separatorChar + Configuration.getCombinedConfiguration().getProperty(PROPKEY_savedFileInfo_filename, DEFAULT_savedFileInfo_filename);
		_savedFileInfo = new Configuration(fileName);
		logMsg = "Using file '" + _savedFileInfo.getFilename() + "' to store File Size Information, with " + _savedFileInfo.size() + " entries.";
		_logger.info(_prefix + logMsg);
		appendToLastExecShortReport(logMsg);

		// Save the props file
		//_savedFileInfo.save(); // Why should we save it just after loading it???

//		double beforeFreeMb   = dataDir.getFreeSpace()   / 1024.0 / 1024.0;
		double beforeFreeMb   = dataDir.getUsableSpace() / 1024.0 / 1024.0;
		double totalMb        = dataDir.getTotalSpace()  / 1024.0 / 1024.0;
		double totalGb        = totalMb / 1024.0;
		double beforePctUsed  = 100.0 - (beforeFreeMb / 1024.0 / totalGb * 100.0);
		
		logMsg = String.format("File system usage at '%s', resolved to '%s'. Free = %.0f MB (%.1f GB), Total = %.0f MB (%.1f GB), Percent Used = %.1f %%", 
				dataDir.getAbsolutePath(), 
				dataDirRes.getAbsolutePath(),
				beforeFreeMb, beforeFreeMb/1024.0, 
				totalMb, totalMb / 1024.0, 
				beforePctUsed);

		_logger.info(_prefix + logMsg);
		appendToLastExecShortReport(logMsg);

		
		// read a bunch of DB files
		Map<String, List<FileInfo>> srvMap  = getFilesByServerName();
		Map<String, List<FileInfo>> dateMap = getFilesByTimeStamp();

		long sumHistoryDbFileUsageMb = getSumSizeMb(srvMap, SizeType.FILE_INFO);
		long sumHistoryDbFileUsageGb = sumHistoryDbFileUsageMb / 1024;
		logMsg = "Summary of Saved Historical Recordings is " + sumHistoryDbFileUsageGb + " GB.  (" + sumHistoryDbFileUsageMb + " MB)"; 
		_logger.info(_prefix + logMsg);
		appendToLastExecShortReport(logMsg);

		// Check if we exceed 'saved historical recordings' threshold
		if (cfg_maxHistorySpaceUsageInGb > 0)
		{
			logMsg = "Max space usage in GB for historical recordings is ENABLED. The value is set to " + cfg_maxHistorySpaceUsageInGb + "GB.";
			_logger.info(_prefix + logMsg);
			appendToLastExecShortReport(logMsg);

			if (sumHistoryDbFileUsageGb > cfg_maxHistorySpaceUsageInGb)
			{
				doCleanupDueToExceedingMaxHistorySpace    = true;
				needSpaceInMb_forExceedingMaxHistorySpace = (sumHistoryDbFileUsageGb - cfg_maxHistorySpaceUsageInGb) * 1024;

				logMsg = "Cleanup will be attempted due to 'saved historical recordings' of " + sumHistoryDbFileUsageGb + " GB exceeds the max limit of " + cfg_maxHistorySpaceUsageInGb + " GB. (specified by property '" + PROPKEY_maxHistoricalSpaceUsageInGb + "')";
				_logger.info(_prefix + logMsg);
				appendToLastExecShortReport(logMsg);

				logMsg = "At least " + needSpaceInMb_forExceedingMaxHistorySpace + " MB (" + (needSpaceInMb_forExceedingMaxHistorySpace/1024) + " GB) will be deleted.";
				_logger.info(_prefix + logMsg);
				appendToLastExecShortReport(logMsg);
			}
		}
		else
		{
			logMsg = "Max space usage in GB for historical recordings is NOT enabled. This can be enabled by setting property '" + PROPKEY_maxHistoricalSpaceUsageInGb + "=###'.";
			_logger.info(_prefix + logMsg);
			appendToLastExecShortReport(logMsg);

			// Check for PERCENT Usage
			if (cfg_maxHistorySpaceUsageInPct > 0)
			{
				logMsg = "Max space usage in PERCENT for historical recordings is ENABLED. The value is set to " + cfg_maxHistorySpaceUsageInPct + " Percent.";
				_logger.info(_prefix + logMsg);
				appendToLastExecShortReport(logMsg);

				if (beforePctUsed > cfg_maxHistorySpaceUsageInPct)
				{
					long maxHistorySpaceUsageInMb_basedOnPct = (long) (totalMb * (cfg_maxHistorySpaceUsageInPct / 100.0));

					doCleanupDueToExceedingMaxHistorySpace    = true;
//					needSpaceInMb_forExceedingMaxHistorySpace = (sumHistoryDbFileUsageMb - maxHistorySpaceUsageInMb_basedOnPct);
					needSpaceInMb_forExceedingMaxHistorySpace = (maxHistorySpaceUsageInMb_basedOnPct - sumHistoryDbFileUsageMb);

					logMsg = "Cleanup will be attempted due to 'saved historical recordings' of " + NumberUtils.round(beforePctUsed, 2) + " Percent exceeds the max limit of " + cfg_maxHistorySpaceUsageInPct + " Percent. (specified by property '" + PROPKEY_maxHistoricalSpaceUsageInPct + "')";
					_logger.info(_prefix + logMsg);
					appendToLastExecShortReport(logMsg);

					logMsg = "At least " + needSpaceInMb_forExceedingMaxHistorySpace + " MB (" + (needSpaceInMb_forExceedingMaxHistorySpace/1024) + " GB) will be deleted.";
					_logger.info(_prefix + logMsg);
					appendToLastExecShortReport(logMsg);
				}
			}
			else
			{
				logMsg = "Max space usage in PERCENT for historical recordings is NOT enabled. This can be enabled by setting property '" + PROPKEY_maxHistoricalSpaceUsageInPct + "=##.#'.";
				_logger.info(_prefix + logMsg);
				appendToLastExecShortReport(logMsg);
			}
		}

		if (_logger.isDebugEnabled())
		{
			_logger.debug(_prefix + "getFilesByServerName: " + srvMap);
			_logger.debug(_prefix + "getFilesByTimeStamp:  " + dateMap);
			_logger.debug(_prefix + "srvMap .getMaxSizeMb: " + getMaxSizeMb(srvMap,  SizeType.MAX_FILE_OR_SAVED));
			_logger.debug(_prefix + "dateMap.getMaxSizeMb: " + getMaxSizeMb(dateMap, SizeType.MAX_FILE_OR_SAVED));
		}

		double multiplyFactor = Configuration.getCombinedConfiguration().getDoubleProperty(PROPKEY_multiplyFactor, DEFAULT_multiplyFactor);
		long   dateMapMaxSize = getMaxSizeMb(dateMap, SizeType.MAX_FILE_OR_SAVED);
		double needSpaceInMb  = dateMapMaxSize * multiplyFactor;
		
		BigDecimal dateMapMaxSizeGb = new BigDecimal( dateMapMaxSize/1024.0 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
		BigDecimal needSpaceInGb    = new BigDecimal( needSpaceInMb /1024.0 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);

		logMsg = "Calculated/Esitimated Space Usage for next " + multiplyFactor + " Days is " + needSpaceInMb + " MB (" + needSpaceInGb + " GB). Maximum date-range-space-usage is " + dateMapMaxSize + " MB (" + dateMapMaxSizeGb + " GB). Number of days to reserve space for can be changed by setting property '" + PROPKEY_multiplyFactor + "=#.#'.";
		_logger.info(_prefix + logMsg);
		appendToLastExecShortReport(logMsg);

		// Should we deduct 'beforeFreeMb' from 'needSpaceInGb'
//		needSpaceInMb = needSpaceInMb - beforeFreeMb;
//		needSpaceInGb = new BigDecimal( needSpaceInMb /1024.0 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
//		BigDecimal beforeFreeGb = new BigDecimal( beforeFreeMb /1024.0 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
//		_logger.info(_prefix + "Calculated/Esitimated space to remove is " + needSpaceInMb + " MB (" + needSpaceInGb + " GB), when Free File System usage of " + beforeFreeMb + " MB (" + beforeFreeGb + " GB) was subtracted.");


		// Should we do cleanup
		boolean doCleanup = needSpaceInMb > beforeFreeMb || doCleanupDueToExceedingMaxHistorySpace; 

		
		boolean printSpaceInfo = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_printSpaceInfo, DEFAULT_printSpaceInfo);
		if (printSpaceInfo)
		{
			_logger.info(_prefix + "---------------------------");
			if ( doCleanup )
				_logger.info(_prefix + "CLEANUP will be executed");
			else
				_logger.info(_prefix + "NO Cleanup will be done");
			_logger.info(_prefix + "---------------------------");

			_logger.info(_prefix + "BEGIN: listing H2 Database File sizes. By Groups: Server and Date");

			_logger.info(_prefix + "H2 Database File Size group by SERVER");
			for (Entry<String, List<FileInfo>> entry : srvMap.entrySet())
			{
				long sizeMb = getSizeMb(entry.getValue(), SizeType.MAX_FILE_OR_SAVED);
				long fileMb = getSizeMb(entry.getValue(), SizeType.FILE_INFO);
				long saveMb = getSizeMb(entry.getValue(), SizeType.SAVED_INFO);
				_logger.info(String.format(_prefix + "    %-30s %6d MB, \t%5.1f GB  [current: %5.1f GB, saved-max: %5.1f GB, diff: %5.1f]", 
						entry.getKey(), sizeMb, sizeMb/1024.0,
						fileMb/1024.0, saveMb/1024.0, (saveMb-fileMb)/1024.0
						));
			}

			_logger.info(_prefix + "H2 Database File Size group by DATE");
			for (Entry<String, List<FileInfo>> entry : dateMap.entrySet())
			{
				long sizeMb = getSizeMb(entry.getValue(), SizeType.MAX_FILE_OR_SAVED);
				long fileMb = getSizeMb(entry.getValue(), SizeType.FILE_INFO);
				long saveMb = getSizeMb(entry.getValue(), SizeType.SAVED_INFO);
				_logger.info(String.format(_prefix + "    %-30s %6d MB, \t%5.1f GB  [current: %5.1f GB, saved-max: %5.1f GB, diff: %5.1f]", 
						entry.getKey(), sizeMb, sizeMb/1024.0,
						fileMb/1024.0, saveMb/1024.0, (saveMb-fileMb)/1024.0
						));
			}
			_logger.info(_prefix + "END: listing H2 Database File sizes. By Groups: Server and Date");
		}

		if ( ! doCleanup )
		{
			_logger.info(_prefix + "---------------------------");
			_logger.info(_prefix + "NO Cleanup was done");
			_logger.info(_prefix + "---------------------------");
			
			appendToLastExecShortReport("---------------------------");
			appendToLastExecShortReport("NO Cleanup was done");
			appendToLastExecShortReport("---------------------------");
		}
		else
		{
			needSpaceInMb = Math.max(needSpaceInMb, needSpaceInMb_forExceedingMaxHistorySpace);
			
			// Compose what to be deleted
			Map<String, List<FileInfo>> removeMap = new LinkedHashMap<>();
			long removeMapSizeMb = 0;
			for (Entry<String, List<FileInfo>> entry : dateMap.entrySet())
			{
				removeMap.put(entry.getKey(), entry.getValue());
				removeMapSizeMb += getSizeMb(entry.getValue(), SizeType.FILE_INFO);
				
				// When we got enough MB to delete, get out of loop
				if (removeMapSizeMb >= needSpaceInMb)
					break;
				
//				if (doCleanupDueToExceedingMaxHistorySpace && removeMapSizeMb >= needSpaceInMb_forExceedingMaxHistorySpace)
//					break;
			}
			
			logMsg = "CLEANUP will be executed, files will be removed for " + removeMap.size() + " days: " + removeMap.keySet();
			_logger.info(_prefix + logMsg);
			appendToLastExecShortReport(logMsg);

			List<FileInfo> deletedList = new ArrayList<>();
			for (Entry<String, List<FileInfo>> removeEntry : removeMap.entrySet())
			{
				for (FileInfo removeFile : removeEntry.getValue())
				{
//					Path dbPath       = removeFile._file.toPath().toAbsolutePath();
//					Path tracePath    = Paths.get( dbPath.toAbsolutePath().toString().replace(".mv.db", ".trace.db") );
//					Path tempFilePath = Paths.get( dbPath.toAbsolutePath().toString().replace(".mv.db", ".mv.db.tempFile") );
////					Path fileInfoPath = Paths.get( dbPath.toAbsolutePath().toString().replace(".mv.db", ".mv.db.savedFileInfo") );

					FileInfo dbPath       = new FileInfo(removeFile._file.toPath().toAbsolutePath() );
					FileInfo tracePath    = new FileInfo(Paths.get( dbPath._path.toAbsolutePath().toString().replace(".mv.db", ".trace.db") ) );
					FileInfo tempFilePath = new FileInfo(Paths.get( dbPath._path.toAbsolutePath().toString().replace(".mv.db", ".mv.db.tempFile") ) );
//					FileInfo fileInfoPath = new FileInfo(Paths.get( dbPath._path.toAbsolutePath().toString().replace(".mv.db", ".mv.db.savedFileInfo") ) );
					
					// Maybe: add spillover files to a separate list
					// ...  matches(".*-SPILL-OVER-DB-[0-9] + ")) ...

					
					try
					{
						if (_logger.isDebugEnabled())
						{
							_logger.debug(_prefix + "Removing file:             " + dbPath      ._path.toAbsolutePath());
							_logger.debug(_prefix + "Removing file (if exists): " + tracePath   ._path.toAbsolutePath());
							_logger.debug(_prefix + "Removing file (if exists): " + tempFilePath._path.toAbsolutePath());
//							_logger.debug(_prefix + "Removing file (if exists): " + fileInfoPath._path.toAbsolutePath());
						}

						if (dryRun) // Dry run do not do deletes
						{
							if (Files.exists(tracePath._path,    LinkOption.NOFOLLOW_LINKS)) deletedList.add(tracePath);
							if (Files.exists(tempFilePath._path, LinkOption.NOFOLLOW_LINKS)) deletedList.add(tempFilePath);
//							if (Files.exists(fileInfoPath._path, LinkOption.NOFOLLOW_LINKS)) deletedList.add(fileInfoPath);
							deletedList.add(dbPath);
						}
						else 
						{
							// NOTE: At least on Linux/Unix the file may not be deleted at once (especially if "anyone" has the file open)
							//       So a possible workaround may be to: open and "empty" the file (write a single byte)
							//       THEN: The file size will shrink, and be removed "later" when ALL open processes has closed it's handles
							//             On the other hand: The processes which has a open handle will probably "feel bad" when it's an "empty" file...
							//             and it might possibly produce some "bad" output... So should we do this or not...
							boolean emptyTheFileBeforeDelete = false;
							if (emptyTheFileBeforeDelete)
							{
								// TODO: Possibly implement this...
							}

							// Delete the trace file if it exists
							if (Files.deleteIfExists(tracePath._path))    deletedList.add(tracePath);
							if (Files.deleteIfExists(tempFilePath._path)) deletedList.add(tempFilePath);
//							if (Files.deleteIfExists(fileInfoPath._path)) deletedList.add(fileInfoPath);

							// Delete the DB FILE
							Files.delete(dbPath._path);
							deletedList.add(dbPath);

							// Remove the SaveInfo entry
							_savedFileInfo.remove(dbPath._file.getName());
						}
					}
					catch (IOException e)
					{
						logMsg = "Problems deleting file '" + dbPath._path.toAbsolutePath() + "'. Skipping and continuing with next. Caught: " + e;
						_logger.info(_prefix + logMsg);
						appendToLastExecShortReport(logMsg);
					}
				}
			}
			
			int maxDelPathLength = 0;
			for (FileInfo fi : deletedList)
				maxDelPathLength = Math.max(maxDelPathLength, fi._path.toString().length());

			double deletedFilesSizeMbSum = 0;
			if (dryRun)
			{
				_logger.info(_prefix + "DRY-RUN: The following list of files could have been deleted:");
				for (FileInfo fi : deletedList)
				{
//					_logger.info(_prefix + "  -- DRY-RUN: delete-not-done-on: " + StringUtil.left(fi._path.toString(), maxDelPathLength) + "     [" + FileUtils.byteToGb(fi.getSavedSize()) + " GB, " + StringUtil.right(FileUtils.byteToMb(fi.getSavedSize()) + "",6) + " MB, " + StringUtil.right(FileUtils.byteToKb(fi.getSavedSize()) + "",9) + " KB]");
					_logger.info(_prefix + "  -- DRY-RUN: delete-not-done-on: " + StringUtil.left(fi._path.toString(), maxDelPathLength) + "     [" + FileUtils.byteToGb(fi.getActualFileSize()) + " GB, " + StringUtil.right(FileUtils.byteToMb(fi.getActualFileSize()) + "",6) + " MB, " + StringUtil.right(FileUtils.byteToKb(fi.getActualFileSize()) + "",9) + " KB]");
				}
			}
			else
			{
				_logger.info(_prefix + "Deleted the following list of files:");
				for (FileInfo fi : deletedList)
				{
//					logMsg = "  -- deleted-file: " + StringUtil.left(fi._path.toString(), maxDelPathLength) + "     [" + FileUtils.byteToGb(fi.getSavedSize()) + " GB, " + StringUtil.right(FileUtils.byteToMb(fi.getSavedSize()) + "",6) + " MB, " + StringUtil.right(FileUtils.byteToKb(fi.getSavedSize()) + "",9) + " KB]";
					logMsg = "  -- deleted-file: " + StringUtil.left(fi._path.toString(), maxDelPathLength) + "     [" + FileUtils.byteToGb(fi.getActualFileSize()) + " GB, " + StringUtil.right(FileUtils.byteToMb(fi.getActualFileSize()) + "",6) + " MB, " + StringUtil.right(FileUtils.byteToKb(fi.getActualFileSize()) + "",9) + " KB]";
					_logger.info(_prefix + logMsg);
					appendToLastExecShortReport(logMsg);
					
//					deletedFilesSizeMbSum += FileUtils.byteToMb(fi.getFileSize());
					deletedFilesSizeMbSum += FileUtils.byteToMb(fi.getActualFileSize());
				}

				// Write how much we deleted...
				logMsg = "Summary of all deleted files deleted was " + NumberUtils.round(deletedFilesSizeMbSum, 1) + " MB (" + NumberUtils.round(deletedFilesSizeMbSum*1.0/1024.0, 1)+ " GB).";
				_logger.info(_prefix + logMsg);
				appendToLastExecShortReport(logMsg);

				// Sleep for a few seconds, hoping that the underlying OS will/has removed/cleanup the file and released disk space...
				long postDeleteSleepSeconds = 10;
				if (postDeleteSleepSeconds > 0)
				{
					logMsg = "Sleeping for " + postDeleteSleepSeconds + " seconds, to let the underlying OS do deferred cleanup disk space, before we check for 'free space'...";
					_logger.info(_prefix + logMsg);
					appendToLastExecShortReport(logMsg);
				}
			}


//			double afterFreeMb  = dataDir.getFreeSpace() / 1024.0 / 1024.0;
			double afterFreeMb  = dataDir.getUsableSpace() / 1024.0 / 1024.0;
			double afterPctUsed = 100.0 - (afterFreeMb / 1024.0 / totalGb * 100.0);

			// Sum H2 recording databases, size AFTER Cleanup 
			Map<String, List<FileInfo>> afterSrvMap = getFilesByServerName();
			long       afterSumHistoryDbFileUsageMb = getSumSizeMb(afterSrvMap, SizeType.FILE_INFO);
			BigDecimal afterSumHistoryDbFileUsageGb = new BigDecimal( afterSumHistoryDbFileUsageMb /1024.0 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);

			
			_logger.info(_prefix + "---------------------------");
			logMsg = String.format("After cleanup. File system usage at '%s', resolved to '%s'. Free = %.0f MB (%.1f GB). %.0f MB (%.1f GB) was removed/deleted. Space Usage in Percent is now %.1f %%", 
					dataDir.getAbsolutePath(), 
					dataDirRes.getAbsolutePath(), 
					afterFreeMb, afterFreeMb/1024.0,  
					afterFreeMb - beforeFreeMb, (afterFreeMb - beforeFreeMb) / 1024.0, 
					afterPctUsed);
			_logger.info(_prefix + logMsg);
			appendToLastExecShortReport(logMsg);

			logMsg = "After cleanup. Summary of Saved Historical Recordings is " + afterSumHistoryDbFileUsageMb + " MB (" + afterSumHistoryDbFileUsageGb + " GB)."; 
			_logger.info(_prefix + logMsg);
			appendToLastExecShortReport(logMsg);
			_logger.info(_prefix + "---------------------------");

			// If we did NOT succeed (in freeing enough space), then write some extra info
			if( needSpaceInMb > afterFreeMb )
			{
				logMsg = String.format("Cleanup failed to remove enough space. Calculated/Esitimated space was %.0f MB. After cleanup there was only %.0f MB of free space. Problems when storing recordings may occur.", needSpaceInMb, afterFreeMb);
				_logger.warn(_prefix + logMsg);
				appendToLastExecShortReport("WARNING: " + logMsg);

				_logger.info(_prefix + "---------------------------");
			}
		} // end: doCleanup

		// Cleanup the content in "Saving File Size Information file" -- Remove entries that EXISTS in the properties file, but DOES NOT have a physical file.
		// TODO: Implement this

		// Save the props file, and throw away the object
		logMsg = "Saving File Size Information in file '" + _savedFileInfo.getFilename() + "', with " + _savedFileInfo.size() + " entries.";
		_logger.info(_prefix + logMsg);
		appendToLastExecShortReport(logMsg);

		_savedFileInfo.save();
		_savedFileInfo = null; // It will be read up on next call (we might change it manually...)
	}
	
	public static void main(String[] args)
	{
		Properties log4jProps = new Properties();
		log4jProps.setProperty("log4j.rootLogger", "INFO, A1");
//		log4jProps.setProperty("log4j.rootLogger", "TRACE, A1");
		log4jProps.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
		log4jProps.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
		log4jProps.setProperty("log4j.appender.A1.layout.ConversionPattern", "%d - %-5p - %-30c{1} - %m%n");
		PropertyConfigurator.configure(log4jProps);

		DataDirectoryCleaner t = new DataDirectoryCleaner();
		t.check(true);
		
//		Map<String, List<FileInfo>> srvMap = t.getFilesByServerName();
//		Map<String, List<FileInfo>> tsMap = t.getFilesByTimeStamp();
//		
//		System.out.println("getFilesByServerName: " + srvMap);
//		System.out.println("getFilesByTimeStamp:  " + tsMap);
//		System.out.println("srvMap.getMaxSize: " + t.getMaxSizeMb(srvMap));
//		System.out.println("tsMap .getMaxSize: " + t.getMaxSizeMb(tsMap));
		
		
		// - Get file-system freeSpace
		// - get max TimeStamp group * 1.5 or 2
		// - check if we have enough space
		//   if NOT
		//     - get oldest TS group -> check if that space is enough (if we delete it)
		//     if NOT
		//       - incluse another TS group...
		// - delete the "delete-list"
		// - Check/print how much we deleted...
	}
}


/*
#! /bin/bash

#============================================================================
# Description:
#   Cleanup the DBXTUNE_SAVE_DIR from old files
#   Algorith:
#     - Get availabe space on the mount point
#     - Get a summary of all files created with the same "timestamp" (many servers is probably samples)
#     - Get summary of all db files for the 2 oldest sample dates
#     - if: sizeOfSummary > availableSpace: do-cleanup
#
#   The above algorithm is simple, and it will probably have problems in it...
#
# Execute this schell script from the crontab.
# crontab example:
#############################################################################
# +--------- minute       (0 - 59)
# | +------- hour         (0 - 23)
# | | +----- day of month (1 - 31)
# | | | +--- month        (1 - 12)
# | | | | +- day of week  (0 -  6) (Sunday to Saturday; 7 is also Sunday on some systems)
# * * * * *  command to execute
#############################################################################
# 55 23 * * * /home/sybase/dbxtune/cleanup_data.sh >> /home/sybase/dbxtune/log/cleanup_data.log 2>&1
#----------------------------------------------------------------------------

##----------------------------------------------
## Source environment
##----------------------------------------------
if [ -f ${HOME}/.dbxtune/DBXTUNE.env ]
then
	echo "Sourcing local environment from: ${HOME}/.dbxtune/DBXTUNE.env"
	. ${HOME}/.dbxtune/DBXTUNE.env
fi


## Settings
DBXTUNE_SAVE_DIR=${DBXTUNE_SAVE_DIR:-${HOME}/dbxtune/data}
printReport=1

sampleNum=2   ## use X oldest samples as a template for how many MB we need in the future for db storage

## How much space in KB do we have available on DBXTUNE_SAVE_DIR
saveDirSizeInMb=$(    df -k ${DBXTUNE_SAVE_DIR} | sed '1d' | awk '{print int($2/1024)}')
saveDirAvailabeInMb=$(df -k ${DBXTUNE_SAVE_DIR} | sed '1d' | awk '{print int($4/1024)}')

## setup some files where we store temp results
dbFileList="/tmp/dbxtune.h2_db_file_list.$$"                       ## H2 files in format: "filename sizeInMb"
dbFileListSizeBySrv="/tmp/dbxtune.h2_db_file_list.sizeBySrv.$$"    ## size group by servername
dbFileListSizeByDate="/tmp/dbxtune.h2_db_file_list.sizeByDate.$$"  ## size group by sample date

## Get all files H2 db files into a tmpfile
ls -Fal ${DBXTUNE_SAVE_DIR}/*.mv.db | awk '{printf "%s %s\n", $9, int($5/1024/1024)}' | sed "s|${DBXTUNE_SAVE_DIR}/||" | sed 's/\.mv\.db//' > ${dbFileList}

## Remove DBXTUNE_CENTRAL_DB or some other set of files from the ${dbFileList} file
sed -i '/DBXTUNE_CENTRAL_DB/d' ${dbFileList}

## group size by SRVNAME into a tmpfile
cat ${dbFileList} | sed 's/_[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]//' | awk '{sum[$1]+= $2} END {for (field1 in sum) {print field1, sum[field1]}}' | sort > ${dbFileListSizeBySrv}

## group size by DATE into a tmpfile
cat ${dbFileList} | awk '{print substr($1, length($1)-9, length($1)), $2}' | awk '{sum[$1]+= $2} END {for (field1 in sum) {print field1, sum[field1]}}' | sort > ${dbFileListSizeByDate}

## What calculation type did we end up using... (used in the report section)
calculationType="${sampleNum} oldest sample dates"

## get size of the X oldest samples, which is approx what we need to have free space for
needFreeSizeInMb=$(cat ${dbFileListSizeByDate} | head -${sampleNum} | awk '{size+=$2} END {print size}')

## do average calculation (sumOfFileSizes / numberOfRecords)
sumOfFileSizes=$(cat ${dbFileListSizeByDate} | awk '{size+=$2} END {print size}')
numberOfRecords=$(cat ${dbFileListSizeByDate} | wc -l)
needFreeSizeInMbAvg=$(( ${sumOfFileSizes} / ${numberOfRecords} * ${sampleNum} ))
## If the average is higher than oldest X files... then use that
if [ ${needFreeSizeInMbAvg} -gt ${needFreeSizeInMb} ]
then
	needFreeSizeInMb=${needFreeSizeInMbAvg}
	calculationType="average over ${numberOfRecords} sample files divide by ${sampleNum}"
fi

## get MaxSize from the list and multiply by NumberOfDays we want to have space for
maxFileSize=$(cat ${dbFileListSizeByDate} | awk 'BEGIN {maxSize=0} {if($2>maxSize){maxSize=$2}} END {print maxSize}')
needFreeSizeInMbMax=$(( ${maxFileSize} * ${sampleNum} ))
## If the MAX is higher than (last calculation)... then use that
if [ ${needFreeSizeInMbMax} -gt ${needFreeSizeInMb} ]
then
	needFreeSizeInMb=${needFreeSizeInMbMax}
	calculationType="max fileSize=${maxFileSize} MB, multiplyed by ${sampleNum}"
fi


doCleanup=0
cleanupList=""
if [ ${saveDirAvailabeInMb} -lt ${needFreeSizeInMb} ]
then
	doCleanup=1
	cleanupList="$(cat ${dbFileListSizeByDate} | head -${sampleNum} | awk '{print $1}')"
	
	## check: If the X oldes files is enough... if not just go with "one more" sample
	cleanupListSize=$(cat ${dbFileListSizeByDate} | head -${sampleNum} | awk '{size+=$2} END {print size}')
	if [ ${cleanupListSize} -lt ${needFreeSizeInMb} ]
	then
		sampleNum=$(( ${sampleNum} + 1 ))
		cleanupList="$(cat ${dbFileListSizeByDate} | head -${sampleNum} | awk '{print $1}')"
		calculationType="${calculationType}, and incremented number of delete samples to ${sampleNum}"
	fi
fi

## Print report
if [ ${printReport} -gt 0 ]
then
	echo ""
	echo "################################################################################"
	echo " Date: $(date '+%Y-%m-%d %H:%M:%S')"
	echo "################################################################################"
	echo ""
	echo "=================================================="
	echo " Space info: doCleanup=${doCleanup}"
	echo "--------------------------------------------------"
	printf "%4d GB total size on DBXTUNE_SAVE_DIR\n"                 $(( ${saveDirSizeInMb}     / 1024 ))
	printf "%4d GB available on DBXTUNE_SAVE_DIR\n"                  $(( ${saveDirAvailabeInMb} / 1024 ))
	printf "%4d GB this is what we NEED using: ${calculationType}\n" $(( ${needFreeSizeInMb}    / 1024 ))
	echo ""
	if [ ${doCleanup} -gt 0 ]
	then
		echo "--- CLEANUP will be executed (files will be removed)"
	else
		echo "--- NO Cleanup will be done"
	fi
	echo ""
	echo "=================================================="
	echo " File size group by SERVER"
	echo "--------------------------------------------------"
	awk '{printf "%-30s %d GB\n", $1, $2/1024}' ${dbFileListSizeBySrv}
	echo ""
	echo "=================================================="
	echo " File size group by DATE"
	echo "--------------------------------------------------"
	awk '{printf "%-30s %d GB\n", $1, $2/1024}' ${dbFileListSizeByDate}
	echo ""
fi

## should we delete any old files
if [ ${doCleanup} -gt 0 ]
then
	echo ""
	echo "=================================================="
	echo " CLEANING UP - REMOVING OLDER SAMPLES: $( echo ${cleanupList} | tr '\n' ' ' )"
	echo "--------------------------------------------------"
	## list
	for f in ${cleanupList}
	do
		echo "*** Listing files for date: ${f}"
		ls -Falh ${DBXTUNE_SAVE_DIR}/*${f}*.db
	done
	echo ""
	## remove
	for f in ${cleanupList}
	do
		echo "*** Deleting files for date: ${f}"
		rm -f ${DBXTUNE_SAVE_DIR}/*${f}*.db
	done
	
	afterCleanupAvailabeInMb=$(df -k ${DBXTUNE_SAVE_DIR} | sed '1d' | awk '{print int($4/1024)}')
	removedMb=$(( ${afterCleanupAvailabeInMb} - ${saveDirAvailabeInMb} ))
	echo ""
	echo "=================================================="
	echo " After cleanup, Space info"
	echo "--------------------------------------------------"
	printf "%4d GB available on DBXTUNE_SAVE_DIR\n"           $(( ${afterCleanupAvailabeInMb} / 1024 ))
	printf "%4d GB was removed/deleted on DBXTUNE_SAVE_DIR\n" $(( ${removedMb}                / 1024 ))
	echo ""
fi

## Remove the temp files
rm -f ${dbFileList}
rm -f ${dbFileListSizeBySrv}
rm -f ${dbFileListSizeByDate}

*/
