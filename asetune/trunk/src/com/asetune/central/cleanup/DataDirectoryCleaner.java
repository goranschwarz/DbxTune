package com.asetune.central.cleanup;

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

import com.asetune.central.DbxTuneCentral;
import com.asetune.utils.Configuration;
import com.asetune.utils.FileUtils;
import com.asetune.utils.StringUtil;
import com.asetune.utils.TimeUtils;

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
	public static final double  DEFAULT_multiplyFactor = 2.0;

	public static final String  PROPKEY_printSpaceInfo = "DataDirectoryCleaner.print.space.info";
	public static final boolean DEFAULT_printSpaceInfo = true;

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

	private Configuration _savedFileInfo = null;

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
		private long _savedFileSize;
		
		/** Create a new FileInfo */
		public FileInfo(File f)
		{
			_file = f;
			_savedFileSize = -1;

			if (_file.exists())
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
		
		public long getFileSize()
		{
			return _file.length();
		}
		public long getSavedSize()
		{
			return _savedFileSize;
		}
	}

	@Override
	public void execute(TaskExecutionContext context) throws RuntimeException
	{
		long timeSinceLastExec = System.currentTimeMillis() - _lastExec;
		if (timeSinceLastExec < _lastExecThreshold)
		{
			_logger.info("Skipping 'Data Directory Cleanup' task since it was only "+TimeUtils.msToTimeStr("%MM:%SS", timeSinceLastExec)+" (MM:SS) since last cleanup. Minimum time between cleanups is "+TimeUtils.msToTimeStr("%MM:%SS", _lastExecThreshold)+" (MM:SS).");
			return;
		}


		_logger.info("");
		_logger.info("#############################################################################################");
		_logger.info("Begin task: Data Directory Cleanup");
		
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

		_logger.info("End task: Data Directory Cleanup");
	}

	private void check(boolean dryRun)
	{
		File dataDir = new File(DATA_DIR);
		File dataDirRes  = dataDir;
		try { dataDirRes = dataDir.toPath().toRealPath().toFile(); } catch(IOException ex) { _logger.warn("Problems resolving File->Path->File");}

//		double beforeFreeMb   = dataDir.getFreeSpace()   / 1024.0 / 1024.0;
		double beforeFreeMb   = dataDir.getUsableSpace() / 1024.0 / 1024.0;
		double totalMb        = dataDir.getTotalSpace()  / 1024.0 / 1024.0;
		double totalGb        = totalMb / 1024.0;
		double beforePctUsed  = 100.0 - (beforeFreeMb / 1024.0 / totalGb * 100.0);
		
		_logger.info(String.format(_prefix + "File system usage at '%s', resolved to '%s'. Free = %.0f MB (%.1f GB), Total = %.0f MB (%.1f GB), Percent Used = %.1f %%", 
				dataDir.getAbsolutePath(), 
				dataDirRes.getAbsolutePath(),
				beforeFreeMb, beforeFreeMb/1024.0, 
				totalMb, totalMb / 1024.0, 
				beforePctUsed));
		
		// Create a new Configuration object, which holds entries for 'SavedFileInfo'
		String fileName = dataDirRes.getAbsolutePath() + File.separatorChar + Configuration.getCombinedConfiguration().getProperty(PROPKEY_savedFileInfo_filename, DEFAULT_savedFileInfo_filename);
		_savedFileInfo = new Configuration(fileName);
		_logger.info(_prefix + "Using file '"+_savedFileInfo.getFilename()+"' to store File Size Information, with "+_savedFileInfo.size()+" entries.");


		// read a bunch of DB files
		Map<String, List<FileInfo>> srvMap  = getFilesByServerName();
		Map<String, List<FileInfo>> dateMap = getFilesByTimeStamp();
		
		// Save the props file
		_savedFileInfo.save();
		
		if (_logger.isDebugEnabled())
		{
			_logger.debug(_prefix + "getFilesByServerName: "+srvMap);
			_logger.debug(_prefix + "getFilesByTimeStamp:  "+dateMap);
			_logger.debug(_prefix + "srvMap .getMaxSizeMb: "+getMaxSizeMb(srvMap,  SizeType.MAX_FILE_OR_SAVED));
			_logger.debug(_prefix + "dateMap.getMaxSizeMb: "+getMaxSizeMb(dateMap, SizeType.MAX_FILE_OR_SAVED));
		}

		double multiplyFactor = Configuration.getCombinedConfiguration().getDoubleProperty(PROPKEY_multiplyFactor, DEFAULT_multiplyFactor);
		long dateMapMaxSize = getMaxSizeMb(dateMap, SizeType.MAX_FILE_OR_SAVED);
		double needSpaceInMb = dateMapMaxSize * multiplyFactor;
		
		BigDecimal dateMapMaxSizeGb = new BigDecimal( dateMapMaxSize/1024.0 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
		BigDecimal needSpaceInGb    = new BigDecimal( needSpaceInMb /1024.0 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);

		_logger.info(_prefix + "Calculated/Esitimated Space Usage for next "+multiplyFactor+" Days is "+needSpaceInMb + " MB ("+needSpaceInGb+" GB). Maximum date-range-space-usage is "+dateMapMaxSize+" MB ("+dateMapMaxSizeGb+" GB).");

		// Should we deduct 'beforeFreeMb' from 'needSpaceInGb'
//		needSpaceInMb = needSpaceInMb - beforeFreeMb;
//		needSpaceInGb = new BigDecimal( needSpaceInMb /1024.0 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
//		BigDecimal beforeFreeGb = new BigDecimal( beforeFreeMb /1024.0 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
//		_logger.info(_prefix + "Calculated/Esitimated space to remove is "+needSpaceInMb + " MB ("+needSpaceInGb+" GB), when Free File System usage of "+beforeFreeMb+" MB ("+beforeFreeGb+" GB) was subtracted.");


		boolean doCleanup = needSpaceInMb > beforeFreeMb; 
//		_logger.info(_prefix + "doCleanup="+doCleanup);

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
				_logger.info(String.format(_prefix + "    %-30s %6d MB, \t%5.1f GB  [current: %5.1f GB, saved-max: %5.1f GB]", 
						entry.getKey(), sizeMb, sizeMb/1024.0,
						fileMb/1024.0, saveMb/1024.0
						));
			}

			_logger.info(_prefix + "H2 Database File Size group by DATE");
			for (Entry<String, List<FileInfo>> entry : dateMap.entrySet())
			{
				long sizeMb = getSizeMb(entry.getValue(), SizeType.MAX_FILE_OR_SAVED);
				long fileMb = getSizeMb(entry.getValue(), SizeType.FILE_INFO);
				long saveMb = getSizeMb(entry.getValue(), SizeType.SAVED_INFO);
				_logger.info(String.format(_prefix + "    %-30s %6d MB, \t%5.1f GB  [current: %5.1f GB, saved-max: %5.1f GB]", 
						entry.getKey(), sizeMb, sizeMb/1024.0,
						fileMb/1024.0, saveMb/1024.0
						));
			}
			_logger.info(_prefix + "END: listing H2 Database File sizes. By Groups: Server and Date");
		}

		if ( ! doCleanup )
		{
			_logger.info(_prefix + "---------------------------");
			_logger.info(_prefix + "NO Cleanup was done");
			_logger.info(_prefix + "---------------------------");
		}
		else
		{
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
			}
			
			_logger.info(_prefix + "CLEANUP will be executed, files will be removed for: "+removeMap.keySet());
			List<Path> deletedList = new ArrayList<>();
			for (Entry<String, List<FileInfo>> removeEntry : removeMap.entrySet())
			{
				for (FileInfo removeFile : removeEntry.getValue())
				{
					Path dbPath       = removeFile._file.toPath().toAbsolutePath();
					Path tracePath    = Paths.get( dbPath.toAbsolutePath().toString().replace(".mv.db", ".trace.db") );
					Path tempFilePath = Paths.get( dbPath.toAbsolutePath().toString().replace(".mv.db", ".mv.db.tempFile") );
//					Path fileInfoPath = Paths.get( dbPath.toAbsolutePath().toString().replace(".mv.db", ".mv.db.savedFileInfo") );
					
					// Maybe: add spillover files to a separate list
					// ...  matches(".*-SPILL-OVER-DB-[0-9]+")) ...

					
					try
					{
						if (_logger.isDebugEnabled())
						{
							_logger.debug(_prefix + "Removing file:             "+dbPath      .toAbsolutePath());
							_logger.debug(_prefix + "Removing file (if exists): "+tracePath   .toAbsolutePath());
							_logger.debug(_prefix + "Removing file (if exists): "+tempFilePath.toAbsolutePath());
//							_logger.debug(_prefix + "Removing file (if exists): "+fileInfoPath.toAbsolutePath());
						}

						if (dryRun) // Dry run do not do deletes
						{
							if (Files.exists(tracePath,    LinkOption.NOFOLLOW_LINKS)) deletedList.add(tracePath);
							if (Files.exists(tempFilePath, LinkOption.NOFOLLOW_LINKS)) deletedList.add(tempFilePath);
//							if (Files.exists(fileInfoPath, LinkOption.NOFOLLOW_LINKS)) deletedList.add(fileInfoPath);
							deletedList.add(dbPath);
						}
						else 
						{
							// Delete the trace file it it exists
							if (Files.deleteIfExists(tracePath))    deletedList.add(tracePath);
							if (Files.deleteIfExists(tempFilePath)) deletedList.add(tempFilePath);
//							if (Files.deleteIfExists(fileInfoPath)) deletedList.add(fileInfoPath);

							// Delete the DB FILE
							Files.delete(dbPath);
							deletedList.add(dbPath);

							// Remove the SaveInfo entry
							_savedFileInfo.remove(dbPath.toFile().getName());
						}
					}
					catch (IOException e)
					{
						_logger.warn(_prefix + "Problems deleting file '"+dbPath.toAbsolutePath()+"'. Skipping and continuing with next. Caught: "+e);
					}
				}
			}
			
			int maxDelPathLength = 0;
			for (Path path : deletedList)
				maxDelPathLength = Math.max(maxDelPathLength, path.toString().length());
				
			if (dryRun)
			{
//				_logger.info(_prefix + "DRY-RUN: The following list of files could have been deleted: "+deletedList);
				_logger.info(_prefix + "DRY-RUN: The following list of files could have been deleted:");
				for (Path path : deletedList)
					_logger.info(_prefix + "  -- DRY-RUN: delete-not-done-on: "+StringUtil.left(path.toString(), maxDelPathLength)+"     ["+FileUtils.byteToGb(path)+" GB, "+FileUtils.byteToMb(path)+" MB, "+FileUtils.byteToKb(path)+" KB]");
			}
			else
			{
//				_logger.info(_prefix + "Deleted the following list of files: "+deletedList);
				_logger.info(_prefix + "Deleted the following list of files:");
				for (Path path : deletedList)
					_logger.info(_prefix + "  -- deleted-file: "+StringUtil.left(path.toString(), maxDelPathLength)+"     ["+FileUtils.byteToGb(path)+" GB, "+FileUtils.byteToMb(path)+" MB, "+FileUtils.byteToKb(path)+" KB]");
			}
				

//			double afterFreeMb   = dataDir.getFreeSpace() / 1024.0 / 1024.0;
			double afterFreeMb   = dataDir.getUsableSpace() / 1024.0 / 1024.0;
			double afterPctUsed  = 100.0 - (afterFreeMb / 1024.0 / totalGb * 100.0);

			_logger.info(_prefix + "---------------------------");
			_logger.info(String.format(_prefix + "After cleanup. File system usage at '%s', resolved to '%s'. Free = %.0f MB (%.1f GB). %.0f MB (%.1f GB) was removed/deleted. Space Usage in Percent is now %.1f %%", 
					dataDir.getAbsolutePath(), 
					dataDirRes.getAbsolutePath(), 
					afterFreeMb, afterFreeMb/1024.0,  
					afterFreeMb - beforeFreeMb, (afterFreeMb - beforeFreeMb) / 1024.0, 
					afterPctUsed));
			_logger.info(_prefix + "---------------------------");

			// If we did NOT succeed (in freeing enough space), then write some extra info
			if( needSpaceInMb > afterFreeMb )
			{
				_logger.warn(String.format(_prefix + "Cleanup failed to remove enough space. Calculated/Esitimated space was %.0f MB. After cleanup there was only %.0f MB of free space. Problems when storing recordings may occur.",
						needSpaceInMb, afterFreeMb));
				_logger.info(_prefix + "---------------------------");
			}
		} // end: doCleanup

		// Save the props file, and thow away the object
		_logger.info(_prefix + "Saving File Size Information in file '"+_savedFileInfo.getFilename()+"', with "+_savedFileInfo.size()+" entries.");
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
//		System.out.println("getFilesByServerName: "+srvMap);
//		System.out.println("getFilesByTimeStamp:  "+tsMap);
//		System.out.println("srvMap.getMaxSize: "+t.getMaxSizeMb(srvMap));
//		System.out.println("tsMap .getMaxSize: "+t.getMaxSizeMb(tsMap));
		
		
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
# 55 23 * * * /home/sybase/asetune/cleanup_data.sh >> /home/sybase/asetune/log/cleanup_data.log 2>&1
#----------------------------------------------------------------------------

##----------------------------------------------
## Source environment
##----------------------------------------------
if [ -f ${HOME}/.asetune/DBXTUNE.env ]
then
	echo "Sourcing local environment from: ${HOME}/.asetune/DBXTUNE.env"
	. ${HOME}/.asetune/DBXTUNE.env
fi


## Settings
DBXTUNE_SAVE_DIR=${DBXTUNE_SAVE_DIR:-${HOME}/asetune/data}
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