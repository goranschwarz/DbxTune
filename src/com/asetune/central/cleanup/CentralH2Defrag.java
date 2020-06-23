/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
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
package com.asetune.central.cleanup;

import java.io.File;
import java.util.Date;

import org.apache.log4j.Logger;

import com.asetune.central.pcs.CentralPcsWriterHandler;
import com.asetune.central.pcs.CentralPersistWriterJdbc;
import com.asetune.central.pcs.CentralPersistWriterJdbc.H2ShutdownType;
import com.asetune.central.pcs.ICentralPersistWriter;
import com.asetune.sql.conn.ConnectionProp;
import com.asetune.utils.Configuration;
import com.asetune.utils.H2UrlHelper;
import com.asetune.utils.ShutdownHandler;
import com.asetune.utils.TimeUtils;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import it.sauronsoftware.cron4j.Task;
import it.sauronsoftware.cron4j.TaskExecutionContext;

public class CentralH2Defrag
extends Task
{
	private static Logger _logger = Logger.getLogger(CentralH2Defrag.class);

	public static final String  PROPKEY_start = "CentralH2Defrag.start";
	public static final boolean DEFAULT_start = true;

	public static final String PROPKEY_cron = "CentralH2Defrag.cron";
//	public static final String DEFAULT_cron = "* * * * *"; // at 02:00, 06:00, 10:00, 14:00, 18:00, 22:00 every day
//	public static final String DEFAULT_cron = "0 * * * *"; // at every hour, every day
//	public static final String DEFAULT_cron = "0 0,2,4,6,8,10,12,14,16,18,20,22 * * *"; // at every second hour, every day
//	public static final String DEFAULT_cron = "0 2,6,10,14,18,22 * * *"; // at 02:00, 06:00, 10:00, 14:00, 18:00, 22:00 every day
	public static final String DEFAULT_cron = "0 4 * * *"; // at 04:00 every day (since 1.4.200 takes so long time to complete DEFRAG only do it once a day to minimize down time during the day)
//	public static final String DEFAULT_cron = "30 02 1 * *"; // 02:30 first day of the month
//	public static final String DEFAULT_cron = "59 * * * *"; // testing set this to nearest minute

	public static final String  PROPKEY_LOG_FILE_PATTERN = "CentralH2Defrag.log.file.pattern";
	public static final String  DEFAULT_LOG_FILE_PATTERN    = "%d - %-5p - %-30c{1} - %m%n";
	
//	public static final String  PROPKEY_dryRun = "CentralH2Defrag.dryRun";
//	public static final boolean DEFAULT_dryRun = false;
//	public static final boolean DEFAULT_dryRun = true; // For test purposes

	public static final String EXTRA_LOG_NAME = CentralH2Defrag.class.getSimpleName() + "-TaskLogger";

//	private static final String _prefix = "H2-SHUTDOWN-DEFRAG: ";

//	private boolean _dryRun = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_dryRun, DEFAULT_dryRun);

	public static final String H2_STORAGE_INFO_FILENAME = System.getProperty("java.io.tmpdir", "/tmp") + File.separatorChar + "dbxcentral.h2-storage-info.json";
	
	public static final String  PROPKEY_H2_DEFRAG_THRESHOLD_MB = "CentralH2Defrag.threshold.mb";
	public static final int     DEFAULT_H2_DEFRAG_THRESHOLD_MB = 4096; // When the H2 database has increased with 4 GB, lets do a 'shutdown defrag'
	
	@Override
	public void execute(TaskExecutionContext context) throws RuntimeException
	{
		_logger.info("");
		_logger.info("#############################################################################################");
		_logger.info("Begin task: H2 Shutdown Defrag check");

		if ( ! CentralPcsWriterHandler.hasInstance() )
		{
			_logger.info("Skipping cleanup, no CentralPcsWriterHandler was found.");
			return;
		}

		ConnectionProp connProps = null;
		CentralPcsWriterHandler centralPcsHandler = CentralPcsWriterHandler.getInstance();
		for (ICentralPersistWriter w : centralPcsHandler.getWriters())
		{
			if (w instanceof CentralPersistWriterJdbc)
			{
				connProps = ((CentralPersistWriterJdbc) w).getStorageConnectionProps();
				break;
			}
		}
		if ( connProps == null )
		{
			_logger.info("Skipping H2 Shutdown Defrag, no CentralPersistWriterJdbc Connection Properties Object was found.");
			return;
		}
		
		String url = connProps.getUrl();
		if ( url != null && ! url.startsWith("jdbc:h2:") )
		{
			_logger.info("Skipping H2 Shutdown Defrag, the database is not H2, url must start with 'jdbc:h2:'. Current URL='"+url+"'.");
			return;
		}
		
		
		boolean doDefrag   = false;
		boolean doSaveFile = false;

		// Try to get the FILES SIZE
		H2UrlHelper urlHelper = new H2UrlHelper(url);
		File dbFile = urlHelper.getDbFile();
		if (dbFile == null)
		{
			_logger.info("Skipping H2 Shutdown Defrag, can't extract H2 database file from the URL '"+url+"'. If the URL contains a filename, the file might not exist.");
			return;
		}
		else
		{
			int dbFileSizeMb = (int) (dbFile.length() / 1024 / 1024);

			// File dbFileRes  = dbFile;
			// try { dbFileRes = dbFile.toPath().toRealPath().toFile(); } catch(IOException ex) { _logger.warn("Problems resolving File->Path->File");}

			// Read the saved information about the DB FILE SIZE
			H2StorageInfo savedInfo = getH2StorageInfo();

			if (savedInfo == null)
			{
				_logger.info("H2 storage information was not found, simply writing the file...");
				doSaveFile = true;
			}
			else
			{
				int sizeDiffMb = dbFileSizeMb - savedInfo.fileSizeMb;
				double sizeDiffPct  = (sizeDiffMb*1.0) / ((savedInfo.fileSizeMb == 0 ? 1 : savedInfo.fileSizeMb)*1.0) * 100.0;

				// If size is smaller than the saved info... save the file info
				if (sizeDiffMb < 0)
					doSaveFile = true;

				// Check if we have crossed the threshold, and we need to do defrag
				int thresholdMb = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_H2_DEFRAG_THRESHOLD_MB, DEFAULT_H2_DEFRAG_THRESHOLD_MB);
				if (sizeDiffMb >= thresholdMb)
				{
					doDefrag   = true;
					doSaveFile = false; // the file will be saved AFTER 'shutdown defrag', IF the size is smaller, done in: CentralPersistWriterJdbc.h2Shutdown()
				}

				_logger.info(String.format("H2 database storage sizeDiffMb=%d, sizeDiffPct=%.1f%%, doDefrag=%b. LastSaved size is %d MB (%.1f GB) at '%s', Current size is %d MB (%.1f GB), for H2 database file '%s'. Threshold in MB %d, which can be changed with property '%s'.", 
						sizeDiffMb,	sizeDiffPct, doDefrag, 
						savedInfo.fileSizeMb, savedInfo.fileSizeMb/1024.0, savedInfo.getAtDateStr(),
						dbFileSizeMb, dbFileSizeMb/1024.0,  
						dbFile.getAbsolutePath(),
						thresholdMb, PROPKEY_H2_DEFRAG_THRESHOLD_MB
						));
			}
			
			// Saved information about the DB FILE SIZE
			if (doSaveFile)
			{
				saveH2StorageInfoFile(dbFileSizeMb, dbFile.toString());
			}
		}

		
		// should we do DEFRAG
		if (doDefrag)
		{
			try
			{
				_logger.info("Executing H2 Shutdown Defrag for CentralPersistWriterJdbc, which will also restart Dbx Central");

				
				// The file will be created, the shutdown will look for the file, delete it, and do shutdown defrag...
//				_logger.info("H2 Shutdown Defrag: creating file '"+CentralPersistWriterJdbc.H2_SHUTDOWN_WITH_DEFRAG_FILENAME+"'.");
//				FileUtils.write(new File(CentralPersistWriterJdbc.H2_SHUTDOWN_WITH_DEFRAG_FILENAME), "from: CentralH2Defrag", StandardCharsets.UTF_8);

				Configuration shutdownConfig = new Configuration();
				shutdownConfig.setProperty("h2.shutdown.type", H2ShutdownType.DEFRAG.toString());  // DEFAULT, IMMEDIATELY, COMPACT, DEFRAG

				boolean doRestart = true;
				String reason = "Restart (with DEFRAG) Requested from "+CentralH2Defrag.class.getSimpleName()+".";
				ShutdownHandler.shutdown(reason, doRestart, shutdownConfig);
			}
			catch (Exception e)
			{
				_logger.error("Problems when executing H2 Shutdown Defrag in CentralPersistWriterJdbc", e);
			}
		}
		else
		{
			_logger.info("Skipping H2 Shutdown Defrag, doDefrag = false.");
		}

		_logger.info("End task: H2 Shutdown Defrag check");
	}

	/**
	 * Read the file <code>System.getProperty("java.io.tmpdir", "/tmp") + File.separatorChar + "dbxcentral.h2-storage-info.json";</code> as JSON content
	 * @param dbFileSizeMb
	 * @param h2DbFilename
	 */
	public static void saveH2StorageInfoFile(int dbFileSizeMb, String h2DbFilename)
	{
		H2StorageInfo savedInfo = new H2StorageInfo(dbFileSizeMb, h2DbFilename);
		
		File h2StorageInfoFile = new File(H2_STORAGE_INFO_FILENAME);
		_logger.info("Saving H2 storage information to file '"+h2StorageInfoFile+"'. dbFileSizeMb="+dbFileSizeMb);

		try
		{
			ObjectMapper mapper = new ObjectMapper();
			mapper.enable(SerializationFeature.INDENT_OUTPUT);
			mapper.writeValue(h2StorageInfoFile, savedInfo);
		}
		catch (Exception e) 
		{
			_logger.warn("Problems writing file '"+h2StorageInfoFile+"', continuing anyway. Caught: "+e);
		}
	}

	/**
	 * Read a file <code>System.getProperty("java.io.tmpdir", "/tmp") + File.separatorChar + "dbxcentral.h2-storage-info.json";</code> and populate a object
	 * @return
	 */
	public static H2StorageInfo getH2StorageInfo()
	{
		File h2StorageInfoFile = new File(H2_STORAGE_INFO_FILENAME);
		H2StorageInfo savedInfo = null;
		if (h2StorageInfoFile.exists())
		{
			try
			{
				ObjectMapper mapper = new ObjectMapper();
				savedInfo = mapper.readValue(h2StorageInfoFile, H2StorageInfo.class);
			}
			catch (Exception e) 
			{
				_logger.warn("Problems reading file '"+h2StorageInfoFile+"', continuing anyway. Caught: "+e);
			}
		}
		return savedInfo;
	}

	/**
	 * Class for JSON/JavaObject translation of some values
	 */
	@JsonPropertyOrder(value = {"fileSizeMb", "atDate", "atDateStr", "filename"}, alphabetic = true)
	public static class H2StorageInfo
	{
		private int    fileSizeMb = -1;
		private Date   atDate     = null;
		private String atDateStr  = null;
		private String filename   = null;
		
		public int    getFileSizeMb() { return fileSizeMb; }
		public Date   getAtDate()     { return atDate;     }
		public String getAtDateStr()  { return atDateStr;  }
		public String getFilename()   { return filename;   }

		public void   setFileSizeMb(int fileSizeMb)  { this.fileSizeMb = fileSizeMb; }
		public void   setAtDate(Date atDate)         { this.atDate     = atDate;     }
		public void   setAtDateStr(String atDateStr) { this.atDateStr  = atDateStr;  }
		public void   setFilename(String filename)   { this.filename   = filename;   }

		public H2StorageInfo()
		{
		}
		public H2StorageInfo(int fileSizeMb, String filename)
		{
			this.fileSizeMb = fileSizeMb;
			this.atDate     = new Date();
			this.atDateStr  = TimeUtils.toString(this.atDate.getTime());
			this.filename   = filename;
		}

		@Override
		public String toString()
		{
			return "fileSizeMb="+fileSizeMb+", atDateStr='"+atDateStr+"'";
		}
	}
}
