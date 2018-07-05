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

	public static final String PROPKEY_cron = "DataDirectoryCleaner.cron";
	public static final String DEFAULT_cron = "54 23 * * *";
//	public static final String DEFAULT_cron = "* * * * *";

	public static final String  PROPKEY_multiplyFactor = "DataDirectoryCleaner.multiply.factor";
	public static final double  DEFAULT_multiplyFactor = 2.0;

	public static final String  PROPKEY_printSpaceInfo = "DataDirectoryCleaner.print.space.info";
	public static final boolean DEFAULT_printSpaceInfo = true;

	public static final String  PROPKEY_LOG_FILE_PATTERN = "DataDirectoryCleaner.log.file.pattern";
	public static final String  DEFAULT_LOG_FILE_PATTERN    = "%d - %-5p - %m%n";
	
	public static final String  PROPKEY_dryRun = "DataDirectoryCleaner.dryRun";
	public static final boolean DEFAULT_dryRun = false;
//	public static final boolean DEFAULT_dryRun = true; // For test purposes

	public static final String EXTRA_LOG_NAME = DataDirectoryCleaner.class.getSimpleName() + "-TaskLogger";

	private static final String _prefix = "DATA-DIR-CLEANUP: ";
	
	private boolean _dryRun = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_dryRun, DEFAULT_dryRun);

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

//	private int getFreeSizeInMb()
//	{
//		File dataDir = new File(DATA_DIR);
//		
//		int freeMb = (int) dataDir.getFreeSpace() / 1024 / 1024 / 1024;
//
//		double freeGb   = dataDir.getFreeSpace()   / 1024.0 / 1024.0 / 1024.0;
////		double usableGb = dataDir.getUsableSpace() / 1024.0 / 1024.0 / 1024.0;
//		double totalGb  = dataDir.getTotalSpace()  / 1024.0 / 1024.0 / 1024.0;
//		double pctUsed  = 100.0 - (freeGb / totalGb * 100.0);
//		
//		System.out.println("File system usage at '"+dataDir+"'<br>");
//		System.out.println(String.format("Free = %.1f GB, Total = %.1f GB, Percent Used = %.1f %%<br>", freeGb, totalGb, pctUsed));
//		
//		return freeMb;
//	}

	private Map<String, List<File>> getFilesByServerName()
	{
		List<File> files = getTimestampedDbFiles();

		Map<String, List<File>> map = new LinkedHashMap<>();
		
		for (File f : files)
		{
			String[] sa = f.getName().split("_[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]");
			String srvName = sa[0];
			
			List<File> list = map.get(srvName);
			if (list == null)
				list = new ArrayList<>();
			
			list.add(f);
			map.put(srvName, list);
		}
		return map;
	}
	
	private Map<String, List<File>> getFilesByTimeStamp()
	{
		List<File> files = getTimestampedDbFiles();

		Map<String, List<File>> map = new LinkedHashMap<>();

		for (File f : files)
		{
			String fname = f.getName();
			int p = fname.lastIndexOf("_");
			if (p != -1)
			{
				String ts = fname.substring(p+1, fname.length()-".MV.DB".length());
				
				List<File> list = map.get(ts);
				if (list == null)
					list = new ArrayList<>();
				
				list.add(f);
				map.put(ts, list);
			}
		}
		return map;
	}
	
	private List<File> getTimestampedDbFiles()
	{
		List<String> files = getFilesH2Dbs();
		List<File> output = new ArrayList<>();

		for (String file : files)
		{
			File f = new File(file);
			if (f.exists() && f.isFile())
			{
				if (f.getName().toUpperCase().matches(".*_[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9].MV.DB"))
				{
					output.add(f);
				}
			}
		}
		return output;
	}

	
	private long getMaxSizeMb(Map<String, List<File>> map)
	{
		long maxSize = 0;

		for (List<File> list : map.values())
		{
			long sum = 0;
			for (File f : list)
				sum += f.length();

			maxSize = Math.max(maxSize, sum);
		}
		
		return maxSize / 1024 / 1024;
	}

	private long getSizeMb(List<File> list)
	{
		long sum = 0;
		for (File f : list)
			sum += f.length();

		return sum / 1024 / 1024;
	}
//	private long getSizeGb(List<File> list)
//	{
//		return getSizeMb(list) / 1024;
//	}



	@Override
	public void execute(TaskExecutionContext context) throws RuntimeException
	{
		_logger.info("");
		_logger.info("#############################################################################################");
		_logger.info("Begin task: Data Directory Cleanup");

		check(_dryRun);

		_logger.info("End task: Data Directory Cleanup");
	}

	private void check(boolean dryRun)
	{
		Map<String, List<File>> srvMap  = getFilesByServerName();
		Map<String, List<File>> dateMap = getFilesByTimeStamp();

		File dataDir = new File(DATA_DIR);
		double beforeFreeMb   = dataDir.getFreeSpace()   / 1024.0 / 1024.0;
		double totalMb        = dataDir.getTotalSpace()  / 1024.0 / 1024.0;
		double totalGb        = totalMb / 1024.0;
		double beforePctUsed  = 100.0 - (beforeFreeMb / 1024.0 / totalGb * 100.0);
		
		_logger.info(String.format(_prefix + "File system usage at '%s'. Free = %.0f MB (%.1f GB), Total = %.0f MB (%.1f GB), Percent Used = %.1f %%", 
				dataDir.getAbsolutePath(), 
				beforeFreeMb, beforeFreeMb/1024.0, 
				totalMb, totalMb / 1024.0, 
				beforePctUsed));
		
		if (_logger.isDebugEnabled())
		{
			_logger.debug(_prefix + "getFilesByServerName: "+srvMap);
			_logger.debug(_prefix + "getFilesByTimeStamp:  "+dateMap);
			_logger.debug(_prefix + "srvMap .getMaxSizeMb: "+getMaxSizeMb(srvMap));
			_logger.debug(_prefix + "dateMap.getMaxSizeMb: "+getMaxSizeMb(dateMap));
		}

		double multiplyFactor = Configuration.getCombinedConfiguration().getDoubleProperty(PROPKEY_multiplyFactor, DEFAULT_multiplyFactor);
		long dateMapMaxSize = getMaxSizeMb(dateMap);
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
			for (Entry<String, List<File>> entry : srvMap.entrySet())
			{
				long sizeMb = getSizeMb(entry.getValue());
				_logger.info(String.format(_prefix + "    %-30s %6d MB, \t%5.1f GB", entry.getKey(), sizeMb, sizeMb/1024.0));
			}

			_logger.info(_prefix + "H2 Database File Size group by DATE");
			for (Entry<String, List<File>> entry : dateMap.entrySet())
			{
				long sizeMb = getSizeMb(entry.getValue());
				_logger.info(String.format(_prefix + "    %-30s %6d MB, \t%5.1f GB", entry.getKey(), sizeMb, sizeMb/1024.0));
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
			Map<String, List<File>> removeMap = new LinkedHashMap<>();
			long removeMapSizeMb = 0;
			for (Entry<String, List<File>> entry : dateMap.entrySet())
			{
				removeMap.put(entry.getKey(), entry.getValue());
				removeMapSizeMb += getSizeMb(entry.getValue());
				
				// When we got enough MB to delete, get out of loop
				if (removeMapSizeMb >= needSpaceInMb)
					break;
			}
			
			_logger.info(_prefix + "CLEANUP will be executed, files will be removed for: "+removeMap.keySet());
			List<Path> deletedList = new ArrayList<>();
			for (Entry<String, List<File>> removeEntry : removeMap.entrySet())
			{
				for (File removeFile : removeEntry.getValue())
				{
					Path dbPath = removeFile.toPath().toAbsolutePath();
					Path tracePath = Paths.get( dbPath.toAbsolutePath().toString().replace(".mv.db", ".trace.db") );
					try
					{
						if (_logger.isDebugEnabled())
						{
							_logger.debug(_prefix + "Removing file:             "+dbPath.toAbsolutePath());
							_logger.debug(_prefix + "Removing file (if exists): "+tracePath.toAbsolutePath());
						}

						if (dryRun) // Dry run do not do deletes
						{
							if (Files.exists(tracePath, LinkOption.NOFOLLOW_LINKS))
								deletedList.add(tracePath);
							deletedList.add(dbPath);
						}
						else 
						{
							// Delete the trace file it it exists
							if (Files.deleteIfExists(tracePath))
								deletedList.add(tracePath);

							// Delete the DB FILE
							Files.delete(dbPath);
							deletedList.add(dbPath);
						}
					}
					catch (IOException e)
					{
						_logger.warn(_prefix + "Problems deleting file '"+dbPath.toAbsolutePath()+"'. Skipping and continuing with next. Caught: "+e);
					}
				}
			}
			
			if (dryRun)
			{
//				_logger.info(_prefix + "DRY-RUN: The following list of files could have been deleted: "+deletedList);
				_logger.info(_prefix + "DRY-RUN: The following list of files could have been deleted:");
				for (Path path : deletedList)
					_logger.info(_prefix + "  -- DRY-RUN: delete-not-done-on: "+path);
			}
			else
			{
//				_logger.info(_prefix + "Deleted the following list of files: "+deletedList);
				_logger.info(_prefix + "Deleted the following list of files:");
				for (Path path : deletedList)
					_logger.info(_prefix + "  -- deleted-file: "+path);
			}
				

			double afterFreeMb   = dataDir.getFreeSpace() / 1024.0 / 1024.0;
			double afterPctUsed  = 100.0 - (afterFreeMb / 1024.0 / totalGb * 100.0);

			_logger.info(_prefix + "---------------------------");
			_logger.info(String.format(_prefix + "After cleanup. File system usage at '%s'. Free = %.0f MB (%.1f GB). %.0f MB (%.1f GB) was removed/deleted. Space Usage in Percent is now %.1f %%", 
					dataDir.getAbsolutePath(), 
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
		
//		Map<String, List<File>> srvMap = t.getFilesByServerName();
//		Map<String, List<File>> tsMap = t.getFilesByTimeStamp();
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