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
package com.dbxtune.pcs.report;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.central.DbxTuneCentral;
import com.dbxtune.central.pcs.objects.DsrSkipEntry;
import com.dbxtune.pcs.MonRecordingInfo;
import com.dbxtune.pcs.PersistWriterBase;
import com.dbxtune.pcs.report.content.DailySummaryReportContent;
import com.dbxtune.pcs.report.content.RecordingInfo;
import com.dbxtune.pcs.report.content.ReportEntryAbstract;
import com.dbxtune.pcs.report.senders.IReportSender;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.FileUtils;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.TimeUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class DailySummaryReportAbstract
implements IDailySummaryReport
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private DbxConnection _conn = null;
	private IReportSender _sender = null;
	private String        _serverName = null;
	private String        _dbmsSchemaName = null;
	
	private DailySummaryReportContent _reportContent = null; 
	private IProgressReporter _progressReporter = null;

	private Map<String, String> _dbmsOtherInfoMap = null;

	private long _initTime;

	// Map of various values, used by: hasStatusEntry(), setStatusEntry(), getStatusEntry()
	private Map<String, Object> _statusMap = new HashMap<>();

	
	@Override
	public void          setConnection(DbxConnection conn) { _conn = conn; }
	public DbxConnection getConnection()                   { return _conn; }

	@Override
	public void   setServerName(String serverName) { _serverName = serverName; }
	public String getServerName()                  { return _serverName; }

	@Override
	public void   setDbmsSchemaName(String schemaName) { _dbmsSchemaName = schemaName; }
	public String getDbmsSchemaName()                  { return _dbmsSchemaName; }
	public String getDbmsSchemaNameSqlPrefix()
	{
		if (StringUtil.isNullOrBlank(_dbmsSchemaName))
			return "";

		if (_conn == null)
			return "[" + _dbmsSchemaName + "]."; 
		
		return _conn.getLeftQuote() + _dbmsSchemaName + _conn.getRightQuote() + "."; 
	}

	@Override public void                      setReportContent(DailySummaryReportContent content) { _reportContent = content; }
	@Override public DailySummaryReportContent getReportContent()                                  { return _reportContent; }

	@Override public long getInitTime() { return _initTime; }
	
	@Override
	public void close()
	{
		if (_conn != null)
			_conn.closeNoThrow();
	}

	@Override
	public void setProgressReporter(IProgressReporter progressReporter)
	{
		_progressReporter = progressReporter;
	}

	@Override
	public IProgressReporter getProgressReporter()
	{
		return _progressReporter;
	}

	@Override
	public void setReportSender(IReportSender reportSender)
	{
		_sender = reportSender;
	}


	/**
	 * Check if status entry in the report exists 
	 * @param statusKey
	 * @return if the value has previously been set or not
	 */
	public boolean hasStatusEntry(String statusKey)
	{
		return _statusMap.containsKey(statusKey);
	}
	/**
	 * Set status entry in the report (value in the "exists map" will be set to "" a blank string)
	 * 
	 * @param statusKey    name of the status
	 */
	public Object setStatusEntry(String statusKey)
	{
		return setStatusEntry(statusKey, "");
	}
	/**
	 * Set status entry in the report 
	 * 
	 * @param statusKey    name of the status
	 * @param statusValue  Value
	 */
	public Object setStatusEntry(String statusKey, Object statusValue)
	{
		return _statusMap.put(statusKey, statusValue);
	}
	/**
	 * Get status entry in the report 
	 * 
	 * @param statusKey    name of the status
	 * @return The value (null if not found in the Map)
	 */
	public Object getStatusEntry(String statusKey)
	{
		return _statusMap.get(statusKey);
	}

//	@Override
//	public MonTablesDictionary createMonTablesDictionary()
//	{
//		return null;
//	}
// NOTE: The above was not saving the WaitEvent Descriptions to the PCS... so this may be implemented in the future... right now, lets do it statically

	@Override
	public void init()
	throws Exception
	{
		if (_sender == null)
			throw new RuntimeException("Can't send Daily Summary Report. The sender class is null.");

		_initTime = System.currentTimeMillis();
		
		_sender.init();
		_sender.printConfig();

//		// Initialize MonTableDictionry (if we got any)
//		MonTablesDictionary monTableDict = createMonTablesDictionary();
//		if (monTableDict != null)
//		{
//			MonTablesDictionaryManager.setInstance(monTableDict);
//			monTableDict.initialize(getConnection(), false);
//		}
		// NOTE: The above was not saving the WaitEvent Descriptions to the PCS... so this may be implemented in the future... right now, lets do it statically

		// Report entries "at the TOP of the report"
		addReportEntriesTop();

		// Report entries 
		addReportEntries();

		// Report entries "at the BOTTOM of the report"
		addReportEntriesBottom();

		_logger.info("Initiated Daily Summary Report with " + getReportEntries().size() + " report entries.");
	}

	@Override
	public void send()
	{
		if (_sender == null)
			throw new RuntimeException("Can't send Daily Summary Report. The sender class is null.");

		_sender.send(getReportContent());
	}

	/**
	 * Save a report for "archiving"... For example: To DbxCentral, so it can be viewed at a later stage.
	 */
	@Override
	public void save()
	{
		boolean saveIsEnabled = Configuration.getCombinedConfiguration().getBooleanProperty(DailySummaryReportFactory.PROPKEY_save   , DailySummaryReportFactory.DEFAULT_save);
		String saveDir        = Configuration.getCombinedConfiguration().getProperty(       DailySummaryReportFactory.PROPKEY_saveDir, DailySummaryReportFactory.DEFAULT_saveDir);

		if ( ! saveIsEnabled )
		{
			_logger.info("Saving of DailyReports is not enabled. This can be enabled with property: " + DailySummaryReportFactory.PROPKEY_save);
			return;
		}

		// Get the report object
		DailySummaryReportContent content = getReportContent();


		// --------------------------------------------
		// Compose a file name to save to
		String ts = TimeUtils.getCurrentTimeForFileNameYmdHm();

		// Get server name and remove inappropriate characters so the file save do not fail.
		String srvName = content.getServerName();
		srvName = FileUtils.toSafeFileName(srvName);

		// Nothing to report?
		String ntr = content.hasNothingToReport() ? ".-NTR-" : "";

		// Filename
		String saveToFileName = saveDir + File.separatorChar + srvName + "." + ts + ntr + ".html";
		File   saveToFile = new File(saveToFileName);

		// Produce the content
//		String htmlReport = content.getReportAsHtml();

		// Write the file
		try
		{
			_logger.info("Saving of DailyReport to file '" + saveToFile.getAbsolutePath() + "'.");
			
//			org.apache.commons.io.FileUtils.write(saveToFile, htmlReport, StandardCharsets.UTF_8.name());
			content.saveReportAsFile(saveToFile);
		}
		catch (IOException ex)
		{
			_logger.error("Problems writing Daily Report to file '" + saveToFileName + "'. Caught: "+ex, ex);
		}
	}
	
	@Override
	public File getReportFile()
	{
		// Get the report object
		DailySummaryReportContent content = getReportContent();
		
		return (content == null) ? null : content.getLastSavedReportFile();
	}
	
	@Override
	public void removeOldReports()
	{
//		boolean saveIsEnabled   = Configuration.getCombinedConfiguration().getBooleanProperty(DailySummaryReportFactory.PROPKEY_save                  , DailySummaryReportFactory.DEFAULT_save);
		String  saveDir         = Configuration.getCombinedConfiguration().getProperty(       DailySummaryReportFactory.PROPKEY_saveDir               , DailySummaryReportFactory.DEFAULT_saveDir);
		int     removeAfterDays = Configuration.getCombinedConfiguration().getIntProperty(    DailySummaryReportFactory.PROPKEY_removeReportsAfterDays, DailySummaryReportFactory.DEFAULT_removeReportsAfterDays);

		File saveDirFile = new File(saveDir);
		if ( saveDirFile.exists() && saveDirFile.isDirectory() )
		{
			//_logger.info("Removing older reports than " + removeAfterDays + " days, from directory '" + saveDirFile + "'.");

			int removeCount = 0;
			
			List<String> removeList = getOldFilesInReportsDir(saveDir, removeAfterDays);
			for (String filename : removeList)
			{
				try
				{
					File f = new File(filename);
					f.delete();
					_logger.info("Removed old report '" + f.getAbsolutePath() + "'.");
					removeCount++;
				}
				catch(Exception ex)
				{
					_logger.info("Problems removing old report '" + filename + "'.", ex);
				}
			}

			_logger.info("Removed " + removeCount + " older reports than " + removeAfterDays + " days, from directory '" + saveDirFile + "'.");
		}
		else
		{
			_logger.info("Removing old reports... The directory '" + saveDirFile + "' didn't exists. Skipping this.");
		}
		
		// If any temporary files for the content was created... remove it
		DailySummaryReportContent reportContent = getReportContent();
		if (reportContent != null)
		{
			File reportFile = reportContent.getReportFile();
			if (reportFile != null)
			{
				boolean success = reportContent.removeReportFile();
				_logger.info("Removing (temporary) Report File '" + reportFile + "' " + (success ? "was successful." : "failed."));
			}
			
		}
	}

	
	/** Get a list of files in the directory that are older than X days */
	protected List<String> getOldFilesInReportsDir(String directory, int olderThanDays)
	{
		List<String> fileNames = new ArrayList<>();

		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(directory)))
		{
			for (Path path : directoryStream)
			{
				File f = path.toFile();
				
				long lastModified = f.lastModified();
				if (lastModified == 0)
					continue;

				// Transform lastModified into days
				long fileAgeInDays = (System.currentTimeMillis() - lastModified) / 1000 / 60 / 60 / 24;

				if (fileAgeInDays > olderThanDays)
				{
					fileNames.add(path.toString());
				}
				else
				{
					_logger.debug("Skipping file '" + f + "' because fileAgeInDays=" + fileAgeInDays + " is less than threshold=" + olderThanDays);
				}
			}
		}
		catch (IOException ex)
		{
			_logger.info("Problems getting old report files from directory '" + directory + "'.", ex);
		}

		// Sort the list
		Collections.sort(fileNames);
		
		return fileNames;
	}
	

	private MonRecordingInfo _recordingInfo = null;

	/** Get the DBMS Version string stored by any of the DbxTune collectors */
	public String getDbmsVersionStr()
	{
		initialize();
		return _recordingInfo.getDbmsVersionStr();
	}

	/** Get the DBMS Version string stored by any of the DbxTune collectors (The minimum value, if changed during the day) */
	public String getDbmsVersionStrMin()
	{
		initialize();
		return _recordingInfo.getDbmsVersionStrMin();
	}

	/** Get the DBMS Version string stored by any of the DbxTune collectors  (The maximum value, if changed during the day) */
	public String getDbmsVersionStrMax()
	{
		initialize();
		return _recordingInfo.getDbmsVersionStrMax();
	}

	/** If the DBMS Version String changed during the day... This is the TimeStamp it likely happened */
	public Timestamp getDbmsVersionStrChangeTime()
	{
		initialize();
		return _recordingInfo.getDbmsVersionStrChangeTime();
	}

	/** Get the DBMS Version string stored by any of the DbxTune collectors, and then parse it into a number */
	public long getDbmsVersionNum()
	{
		initialize();
		return _recordingInfo.getDbmsVersionNum();
	}

	/** Get the DBMS Server/instance name stored by any of the DbxTune collectors */
	public String getDbmsServerName()
	{
		initialize();
		return _recordingInfo.getDbmsServerName();
	}

	/** Get the DBMS Server/instance last start time */
	public Timestamp getDbmsStartTime()
	{
		initialize();
		return _recordingInfo.getDbmsStartTime();
	}

	/** Get the DBMS Server/instance last start time in days. @returns -1 if UNKNOWN. otherwise an INT value */
	public int getDbmsStartTimeInDays()
	{
		initialize();
		return _recordingInfo.getDbmsStartTimeInDays();
	}

	/** Get the DBMS other info map. This could be "PAGE-SIZE" or whatever "stuff" that are special for that DBMS type */
	public Map<String, String> getDbmsOtherInfoMap()
	{
		initialize();
		return _dbmsOtherInfoMap;
	}

	/** Set the DBMS other info map. This could be "PAGE-SIZE" or whatever "stuff" that are special for that DBMS type, and will show up in "Recording Information" */
	public void setDbmsOtherInfoMap(Map<String, String> dbmsOtherInfoMap)
	{
		_dbmsOtherInfoMap = dbmsOtherInfoMap;
	}

	/** Create the DBMS other info map. This could be "PAGE-SIZE" or whatever "stuff" that are special for that DBMS type */
	public Map<String, String> createDbmsOtherInfoMap(DbxConnection conn)
	{
		return null;
	}

	/** Get the DbxTune application type that recorded this info */
	public String getRecDbxAppName()
	{
		initialize();
		return _recordingInfo.getRecDbxAppName();
	}

	public String getRecDbxVersionStr()
	{
		initialize();
		return _recordingInfo.getRecDbxVersionStr();
	}

	/** Get the DbxTune application type that recorded this info */
	public String getRecDbxBuildStr()
	{
		initialize();
		return _recordingInfo.getRecDbxBuildStr();
	}

	public boolean isWindows()
	{
		String dbmsVerStr = getDbmsVersionStr();
		if (StringUtil.hasValue(dbmsVerStr))
		{
			if (dbmsVerStr.contains("Windows"))
			{
				return true;
			}
		}
		return false;
	}

	public MonRecordingInfo getRecordingInfo(DbxConnection conn)
	{
		return new MonRecordingInfo(conn, null);
	}
	
	/** Initialize members  */
	private void initialize()
	{
		if (_recordingInfo != null)
			return;
		
		DbxConnection conn = getConnection();

//		_recordingInfo = new MonRecordingInfo(conn, null);
		_recordingInfo = getRecordingInfo(conn);
		
		// set _recording* variables
		refreshRecordingStartEndTime(conn);
		
		// create the DBMS "other info" map
		setDbmsOtherInfoMap(createDbmsOtherInfoMap(conn));
	}

//	private boolean   _initialized       = false;
//	private String    _recAppName        = "";
//	private String    _recBuildString    = "";
//	private String    _recVersionString  = "";
//	private String    _dbmsVersionString = "";
//	private long      _dbmsVersionNum    = -1;
//	private String    _dbmsServerName    = "";
//	private Timestamp _dbmsStartTime     = null;
//
//	/** Get the DBMS Version string stored by any of the DbxTune collectors */
//	public String getDbmsVersionStr()
//	{
//		initialize();
//		return _dbmsVersionString;
//	}
//
//	/** Get the DBMS Version string stored by any of the DbxTune collectors, and then parse it into a number */
//	public long getDbmsVersionNum()
//	{
//		initialize();
//		return _dbmsVersionNum;
//	}
//
//	/** Get the DBMS Server/instance name stored by any of the DbxTune collectors */
//	public String getDbmsServerName()
//	{
//		initialize();
//		return _dbmsServerName;
//	}
//
//	/** Get the DBMS Server/instance last start time */
//	public Timestamp getDbmsStartTime()
//	{
//		initialize();
//		return _dbmsStartTime;
//	}
//
//	/** Get the DBMS Server/instance last start time in days. @returns -1 if UNKNOWN. otherwise an INT value */
//	public int getDbmsStartTimeInDays()
//	{
//		initialize();
//		
//		if (_dbmsStartTime == null)
//			return -1;
//		
//		long msDiff = System.currentTimeMillis() - _dbmsStartTime.getTime();
//
//		//return (int) (msDiff / (1000*60*60*24));
//		return (int) TimeUnit.MILLISECONDS.toDays(msDiff);
//	}
//
//
//	/** Get the DbxTune application type that recorded this info */
//	public String getRecDbxAppName()
//	{
//		initialize();
//		return _recAppName;
//	}
//
//	public String getRecDbxVersionStr()
//	{
//		initialize();
//		return _recVersionString;
//	}
//
//	/** Get the DbxTune application type that recorded this info */
//	public String getRecDbxBuildStr()
//	{
//		initialize();
//		return _recBuildString;
//	}
//	
//	/** Initialize members  */
//	private void initialize()
//	{
//		if (_initialized)
//			return;
//		
//		DbxConnection conn = getConnection();
//		
//		// Get DbxTune application name from the recorded session database
//		String appName = "";
//		if (true) // get from database...
//		{
//			String sql = "select max([ProductString]), max([VersionString]), max([BuildString]) from [MonVersionInfo]";
//
//			sql = conn.quotifySqlString(sql);
//			try ( Statement stmnt = conn.createStatement() )
//			{
//				// Unlimited execution time
//				stmnt.setQueryTimeout(0);
//				try ( ResultSet rs = stmnt.executeQuery(sql) )
//				{
//					while(rs.next())
//					{
//						appName           = rs.getString(1);
//						_recVersionString = rs.getString(2);
//						_recBuildString   = rs.getString(3);
//					}
//				}
//			}
//			catch(SQLException ex)
//			{
//				_logger.warn("Problems getting DbxTune Collector string using SQL '"+sql+"'. Caught: " + ex);
//			}
//		}
//		if (StringUtil.isNullOrBlank(appName))
//		{
//			appName = Version.getAppName();
//		}
//
//		String dbmsVersionColName   = "srvVersion";
//		String dbmsSrvNameColName   = "serverName";
//		String dbmsStartDateColName = "StartDate";
//		String tabName = "CmSummary_abs";
//
//		// What table/column is the "server version" stored in
//		if      ("AseTune"      .equalsIgnoreCase(appName)) { dbmsVersionColName = "srvVersion";  dbmsSrvNameColName = "atAtServerName"; dbmsStartDateColName = "StartDate";     tabName = "CmSummary_abs"; }
//		else if ("IqTune"       .equalsIgnoreCase(appName)) { dbmsVersionColName = "atAtVersion"; dbmsSrvNameColName = "atAtServerName"; dbmsStartDateColName = null;            tabName = "CmSummary_abs"; }
//		else if ("RsTune"       .equalsIgnoreCase(appName)) { dbmsVersionColName = "rsVersion";   dbmsSrvNameColName = "serverName";     dbmsStartDateColName = null;            tabName = "CmSummary_abs"; }
//		else if ("RaxTune"      .equalsIgnoreCase(appName)) { dbmsVersionColName = "NOT_STORED";  dbmsSrvNameColName = "atAtServerName"; dbmsStartDateColName = null;            tabName = "CmSummary_abs"; }
//		else if ("SqlServerTune".equalsIgnoreCase(appName)) { dbmsVersionColName = "srvVersion";  dbmsSrvNameColName = "atAtServerName"; dbmsStartDateColName = "StartDate";     tabName = "CmSummary_abs"; }
//		else if ("PostgresTune" .equalsIgnoreCase(appName)) { dbmsVersionColName = "version";     dbmsSrvNameColName = "instance_name";  dbmsStartDateColName = "start_time";    tabName = "CmSummary_abs"; }
//		else if ("MySqlTune"    .equalsIgnoreCase(appName)) { dbmsVersionColName = "version";     dbmsSrvNameColName = "host";           dbmsStartDateColName = "start_time";    tabName = "CmSummary_abs"; }
//		else if ("OracleTune"   .equalsIgnoreCase(appName)) { dbmsVersionColName = "VERSION";     dbmsSrvNameColName = "INSTANCE_NAME";  dbmsStartDateColName = "STARTUP_TIME";  tabName = "CmSummary_abs"; }
//		else if ("Db2Tune"      .equalsIgnoreCase(appName)) { dbmsVersionColName = "VERSION";     dbmsSrvNameColName = "DATABASE_NAME";  dbmsStartDateColName = null;            tabName = "CmSummary_abs"; }
//		else if ("HanaTune"     .equalsIgnoreCase(appName)) { dbmsVersionColName = "VERSION";     dbmsSrvNameColName = "DATABASE_NAME";  dbmsStartDateColName = null;            tabName = "CmSummary_abs"; }
//
//		String sqlVersion = "'' as [Version]";
//		if (StringUtil.hasValue(dbmsVersionColName))
//			sqlVersion = "max([" + dbmsVersionColName + "])";
//		
//		String sqlSrvName = "'' as [SrvName]";
//		if (StringUtil.hasValue(dbmsSrvNameColName))
//			sqlSrvName = "max([" + dbmsSrvNameColName + "])";
//		
//		String sqlStartDate = "NULL as [StartDate]";
//		if (StringUtil.hasValue(dbmsStartDateColName))
//			sqlStartDate = "max([" + dbmsStartDateColName + "])";
//		
//		// Construct SQL and get the version string
//		String sql = "select " + sqlVersion + ", " + sqlSrvName + ", " + sqlStartDate 
//				+ " from [" + tabName + "]";
//		
//		String    dbmsVersionString = "";
//		String    dbmsSrvName       = "";
//		Timestamp dbmsStartTime     = null;
//		
//		sql = conn.quotifySqlString(sql);
//		try ( Statement stmnt = conn.createStatement() )
//		{
//			// Unlimited execution time
//			stmnt.setQueryTimeout(0);
//			try ( ResultSet rs = stmnt.executeQuery(sql) )
//			{
//				while(rs.next())
//				{
//					dbmsVersionString = rs.getString   (1);
//					dbmsSrvName       = rs.getString   (2);
//					dbmsStartTime     = rs.getTimestamp(3);
//				}
//			}
//		}
//		catch(SQLException ex)
//		{
//			_logger.warn("Problems getting version string using SQL '"+sql+"'. Caught: " + ex);
//		}
//
//		
//		// Parse the Version String into a number
//		long ver = 0; 
//
//		if      ("AseTune"      .equalsIgnoreCase(appName)) { ver = Ver.sybVersionStringToNumber      (dbmsVersionString); }
//		else if ("IqTune"       .equalsIgnoreCase(appName)) { ver = Ver.iqVersionStringToNumber       (dbmsVersionString); }
//		else if ("RsTune"       .equalsIgnoreCase(appName)) { ver = Ver.sybVersionStringToNumber      (dbmsVersionString); }
//		else if ("RaxTune"      .equalsIgnoreCase(appName)) { ver = Ver.sybVersionStringToNumber      (dbmsVersionString); }
//		else if ("SqlServerTune".equalsIgnoreCase(appName)) { ver = Ver.sqlServerVersionStringToNumber(dbmsVersionString); }
//		else if ("PostgresTune" .equalsIgnoreCase(appName)) { ver = Ver.shortVersionStringToNumber(VersionShort.parse(dbmsVersionString)); }
//		else if ("MySqlTune"    .equalsIgnoreCase(appName)) { ver = Ver.shortVersionStringToNumber(VersionShort.parse(dbmsVersionString)); }
//		else if ("OracleTune"   .equalsIgnoreCase(appName)) { ver = Ver.oracleVersionStringToNumber   (dbmsVersionString); }
//		else if ("Db2Tune"      .equalsIgnoreCase(appName)) { ver = Ver.db2VersionStringToNumber      (dbmsVersionString); }
//		else if ("HanaTune"     .equalsIgnoreCase(appName)) { ver = Ver.hanaVersionStringToNumber     (dbmsVersionString); }
//
//		_recAppName        = appName;
//		_dbmsVersionString = dbmsVersionString;
//		_dbmsServerName    = dbmsSrvName;
//		_dbmsVersionNum    = ver;
//		_dbmsStartTime     = dbmsStartTime;
//	}


	public static String getDbxCentralBaseUrl()
	{
		// initialize with default parameters, which may change below...
		String dbxCentralProt = "http";
		String dbxCentralHost = StringUtil.getHostnameWithDomain();
		int    dbxCentralPort = 8080;

		// get where DBX CENTRAL is located.
		String sendToDbxCentralUrl = Configuration.getCombinedConfiguration().getProperty("PersistWriterToHttpJson.url", null);
		if (StringUtil.hasValue(sendToDbxCentralUrl))
		{
			// Parse the URL and get protocol/host/port
			try
			{
				URL url = new URL(sendToDbxCentralUrl);
				
				dbxCentralProt = url.getProtocol();
				dbxCentralHost = url.getHost();
				dbxCentralPort = url.getPort();
			}
			catch (MalformedURLException ex)
			{
				_logger.info("Daily Report: Problems parsing DbxCentral URL '" + sendToDbxCentralUrl + "', using defaults. Caught:" + ex);
			}
		}
		
		// Collector and DBX Central is located on the same host
		// if 'localhost' or '127.0.0.1' then get REAL localhost name
		if (dbxCentralHost.equalsIgnoreCase("localhost") || dbxCentralHost.equalsIgnoreCase("127.0.0.1"))
		{
			dbxCentralHost = StringUtil.getHostnameWithDomain();
		}
		
		// if the URL says port 8080, try to check if we can access it using port 80 also...
		if (dbxCentralPort == 8080)
		{
			// Only check this "once", remember the test in: _dbxCentralPortTest
			if (_dbxCentralPortTest == null)
			{
				try (Socket socket = new Socket()) 
				{
					int testOnPort = 80;
					int timeoutMs  = 10;

					socket.connect(new InetSocketAddress(dbxCentralHost, testOnPort), timeoutMs);
					dbxCentralPort = -1; // USE DEFAULT Port for this protocol
					
					// Should we also "remember" this after the first initial test
					_dbxCentralPortTest = dbxCentralPort;
				} 
				catch (IOException ignore) 
				{
					// Just continue to use 8080
					_dbxCentralPortTest = dbxCentralPort;
				}
			}
			else
			{
				dbxCentralPort = _dbxCentralPortTest;
			}
		}
		
		// Compose URL's
		String dbxCentralBaseUrl = dbxCentralProt + "://" + dbxCentralHost + ( dbxCentralPort == -1 ? "" : ":"+dbxCentralPort);

		// Return a Text with links
		return dbxCentralBaseUrl;
	}
	private static Integer _dbxCentralPortTest = null;


	public String createDbxCentralLink(boolean isFullText)
	{
		String dbxCentralBaseUrl = getDbxCentralBaseUrl();
		String dbxCentralUrlLast = dbxCentralBaseUrl + "/report?op=viewLatest&name="+getServerName();
		String dbxCentralUrlAll  = dbxCentralBaseUrl + "/overview#reportfiles";

		// Return a Text with links
		if (isFullText)
		{
			// Full Text
			return
					"If you have problems to read this as a mail; Here is a <a href='" + dbxCentralUrlLast + "'>Link</a> to latest HTML Report stored in DbxCentral.<br>\n" +
					"Or a <a href='" + dbxCentralUrlAll  + "'>link</a> to <b>all</b> Daily Reports.<br>\n" +
					"";
		}
		else
		{
			// Short message
			return
//					"<br>" +
					"<p style='background-color: yellow'>This is a <b>short</b> version of the Report!</p>" +
					"The full report is also attached in this mail... So open to read the full story...<br>" +
					"Also: here is a <a href='" + dbxCentralUrlLast + "'>Link</a> to latest <b>full</b> HTML Report stored in DbxCentral.<br>\n" +
					"Or a <a href='" + dbxCentralUrlAll  + "'>link</a> to <b>all</b> Daily Reports.<br>\n" +
					"";
		}
	}

	public RecordingInfo getInstanceRecordingInfo()
	{
		if (this instanceof DailySummaryReportDefault)
		{
			DailySummaryReportDefault defaultInstance = (DailySummaryReportDefault) this;

			return (RecordingInfo) defaultInstance.getReportEntry(RecordingInfo.class);
			
		}
		return null;
	}

	//---------------------------------------------------------------
	// Skip entries (in DSR reports) 
	//---------------------------------------------------------------
	private List<DsrSkipEntry> _dsrSkipEntriesForServerName = null;
//	private List<DsrSkipEntry> _dbxCentral_dsrSkipEntriesForServerName = null;
//	private List<DsrSkipEntry> _local_dsrSkipEntriesForServerName = null;

	private int _loadAttemts = 0;
	
	public List<DsrSkipEntry> getDsrSkipEntries(String srvName)
	{
		if (_dsrSkipEntriesForServerName != null)
			return _dsrSkipEntriesForServerName;

		_loadAttemts++;
		if (_loadAttemts > 1)
		{
			_logger.debug("Skipping... _loadAttemts=" + _loadAttemts + ". Refreshing Daily Summary Report SKIP Entries for srvName '" + srvName + "' from DbxCentral.");
			return Collections.emptyList();
		}
		
		String dbxCentralBaseUrl = getDbxCentralBaseUrl();
		String dbxCentralUrlSkip = dbxCentralBaseUrl + "/api/dsr/skip?srvName="+srvName;
//		String dbxCentralUrlSkip = dbxCentralBaseUrl + "/api/dsr/skip;

		_logger.info("Refreshing Daily Summary Report SKIP Entries for srvName '" + srvName + "' from DbxCentral, calling: " + dbxCentralUrlSkip);
		
		try
		{
			URL getRequest = new URL(dbxCentralUrlSkip);
			HttpURLConnection conection = (HttpURLConnection) getRequest.openConnection();
			conection.setRequestMethod("GET");
//			conection.setRequestProperty("srvName", srvName); // set userId its a sample here
			int    responseCode = conection.getResponseCode();
			String responseMsg  = conection.getResponseMessage();

			if(responseCode == HttpURLConnection.HTTP_OK) 
			{
				String readLine = null;
				BufferedReader in = new BufferedReader(new InputStreamReader(conection.getInputStream()));
				StringBuilder response = new StringBuilder();
				while ((readLine = in .readLine()) != null) 
				{
					response.append(readLine);
				}
				in .close();

				// print result
				if (_logger.isDebugEnabled())
					_logger.debug("JSON String Result " + response.toString());
				
				ObjectMapper mapper = new ObjectMapper();
				List<DsrSkipEntry> entries = mapper.readValue(response.toString(), new TypeReference<List<DsrSkipEntry>>(){});

				_dsrSkipEntriesForServerName = entries;
			} 
			else 
			{
				_logger.error("Problems getting Daily Summary Report SKIP entries, received responseCode=" + responseCode + ", Message='" + responseMsg + "', from URL: " + dbxCentralUrlSkip);
				//return null;
			}
		}
		catch (Exception ex)
		{
			_logger.error("Problems getting Daily Summary Report SKIP Entries from DbxCentral using URL: " + dbxCentralUrlSkip);
		}

		File f = new File(DbxTuneCentral.getAppConfDir() + "/DsrSkipEntry.local.json");
		try
		{
			// Lets open local entries (from a file)
			if (f.exists())
			{
				ObjectMapper mapper = new ObjectMapper();
				List<DsrSkipEntry> entries = mapper.readValue(f, new TypeReference<List<DsrSkipEntry>>(){});

				if ( ! entries.isEmpty() )
				{
					_logger.info("Also Adding " + entries.size() + " Daily Summary Report SKIP Entries for srvName '" + srvName + "' from local file '" + f.getAbsolutePath() + "'.");
					if (_dsrSkipEntriesForServerName != null)
						_dsrSkipEntriesForServerName.addAll(entries);
					else
						_dsrSkipEntriesForServerName = entries;
				}
			}
			else
			{
				_logger.info("No 'local' Daily Summary Report SKIP file was found. The file '" + f.getAbsolutePath() + "' did not exist, skipping loading of that file.");
			}
		}
		catch (Exception ex)
		{
			_logger.error("Problems getting Daily Summary Report SKIP Entries from Local File '" + f.getAbsolutePath() + "'.");
		}

		return _dsrSkipEntriesForServerName != null ? _dsrSkipEntriesForServerName : Collections.emptyList();
	}

	/**
	 * Get all StringVal in a Set 
	 * @param srvName
	 * @param className
	 * @param entryType
	 * @return Empty Set if nothing was found 
	 * @throws Exception
	 */
	public Set<String> getDsrSkipEntry(String srvName, String className, String entryType)
	{
		List<DsrSkipEntry> list = getDsrSkipEntries(srvName);
		
		Set<String> set = new HashSet<>();

		for (DsrSkipEntry entry : list)
		{
			if (entry.getSrvName().equals(srvName) && entry.getClassName().equals(className) && entry.getEntryType().equals(entryType))
				set.add(entry.getStringVal());
		}
		return set;
	}

	/**
	 * Get a Map of all entries for this server and className: key='entryType', val='Set of StringVal' 
	 * @param srvName
	 * @param className
	 * @return a Map of entries (never null, if none was found an empty Map is returned)
	 * @throws Exception
	 */
	public Map<String, Set<String>> getDsrSkipEntries(String srvName, String className)
	{
		List<DsrSkipEntry> list = getDsrSkipEntries(srvName);

		Map<String, Set<String>> map = new HashMap<>();
		
		for (DsrSkipEntry entry : list)
		{
			if (entry.getSrvName().equals(srvName) && entry.getClassName().equals(className))
			{
				Set<String> set = map.get(entry.getEntryType());
				if (set == null)
				{
					set = new HashSet<>();
					map.put(entry.getEntryType(), set);
				}

				set.add(entry.getStringVal());
			}
		}
		return map;
	}

	public String getDsrSkipEntriesAsHtmlTable(String srvName, String className)
	{
		List<DsrSkipEntry> list = getDsrSkipEntries(srvName);
		
		StringBuilder sb = new StringBuilder();
		int addCount = 0;

		sb.append("<table class='sortable'> \n");
		sb.append("  <thead> \n");
		sb.append("    <tr>  \n");
//		sb.append("      <th>SrvName</th>  \n");
//		sb.append("      <th>ClassName</th>  \n");
		sb.append("      <th>EntryType</th>  \n");
		sb.append("      <th>StringVal</th>  \n");
		sb.append("      <th>Description</th>  \n");
		sb.append("      <th>SqlTextExample</th>  \n");
		sb.append("    </tr>  \n");
		sb.append("  </thead>  \n");
		sb.append("  <tbody>  \n");

		
		for (DsrSkipEntry entry : list)
		{
			if (entry.getSrvName().equals(srvName) && entry.getClassName().equals(className))
			{
				addCount++;
				
				sb.append("    <tr> \n");
//				sb.append("      <td>" + entry.getSrvName()        + "</td>  \n");
//				sb.append("      <td>" + entry.getClassName()      + "</td>  \n");
				sb.append("      <td>" + entry.getEntryType()      + "</td>  \n");
				sb.append("      <td>" + entry.getStringVal()      + "</td>  \n");
				sb.append("      <td>" + entry.getDescription()    + "</td>  \n");
				sb.append("      <td>" + entry.getSqlTextExample() + "</td>  \n");
				sb.append("    </tr> \n");
			}
		}

		sb.append("  </tbody> \n");
		sb.append("</table> \n");

		return addCount == 0 ? "" : sb.toString();
	}
	
	//---------------------------------------------------------------
	// Reporting Period 
	//---------------------------------------------------------------
	private int _reportPeriodBeginTimeHour   = -1;
	private int _reportPeriodBeginTimeMinute = -1;

	private int _reportPeriodEndTimeHour     = -1;
	private int _reportPeriodEndTimeMinute   = -1;

	private Timestamp _reportPeriodBeginTime = null;
	private Timestamp _reportPeriodEndTime   = null;
	private String    _reportPeriodDuration  = null;

	@Override
	public void setReportPeriodBeginTime(Timestamp beginTs) { _reportPeriodBeginTime = beginTs; }
	
	@Override
	public void setReportPeriodBeginTime(int hour, int minute)
	{
		_reportPeriodBeginTimeHour   = hour;
		_reportPeriodBeginTimeMinute = minute;
	}
	
	@Override
	public void setReportPeriodEndTime  (Timestamp endTs)   { _reportPeriodEndTime   = endTs; }

	@Override
	public void setReportPeriodEndTime(int hour, int minute)
	{
		_reportPeriodEndTimeHour   = hour;
		_reportPeriodEndTimeMinute = minute;
	}
	
	@Override
	public boolean hasReportPeriod()
	{
		if (_reportPeriodBeginTime != null) return true;
		if (_reportPeriodEndTime   != null) return true;

		if (_reportPeriodBeginTimeHour >= 0 && _reportPeriodBeginTimeMinute >= 0) return true;
		if (_reportPeriodEndTimeHour   >= 0 && _reportPeriodEndTimeMinute   >= 0) return true;

		return false;
	}
	
	public Timestamp getReportPeriodBeginTime() { return _reportPeriodBeginTime; }
	public Timestamp getReportPeriodEndTime()   { return _reportPeriodEndTime; }
//	public String    getReportPeriodDuration()  { return _reportPeriodDuration; }
	public String    getReportPeriodDuration()  
	{ 
//		if (_reportPeriodDuration == null)
//			_reportPeriodDuration = TimeUtils.msToTimeStr("%HH:%MM:%SS.%ms", getReportEndTime().getTime() - getReportBeginTime().getTime() ) + "   (HH:MM:SS.millisec)";
		if (_reportPeriodDuration == null)
			_reportPeriodDuration = TimeUtils.msToTimeStrDHMS(getReportEndTime().getTime() - getReportBeginTime().getTime() ) + "   ([#d] HH:MM:SS)";
		
		return _reportPeriodDuration; 
	}


	/**
	 * @return First try getReportPeriodBeginTime(), if not available use getRecordingStartTime()
	 */
	public Timestamp getReportPeriodOrRecordingBeginTime() 
	{ 
		if (getReportPeriodBeginTime() != null)
			return getReportPeriodBeginTime(); 
		return getRecordingStartTime();
	}

	/**
	 * @return First try getReportPeriodEndTime(), if not available use getRecordingEndTime()
	 */
	public Timestamp getReportPeriodOrRecordingEndTime()
	{ 
		if (getReportPeriodEndTime() != null)
			return getReportPeriodEndTime(); 
		return getRecordingEndTime();
	}
	
	/** 
	 * Get the Actual Reporting BEGIN Time
	 * <p>
	 * If it has been set by setReportPeriodBegin/EndTime() that will be reflected<br>
	 * If it'a the full day, the Recording Start/End time will be reflected<br>
	 */
	@Override
	public Timestamp getReportBeginTime()
	{
//		return hasReportPeriod() 
//				? getReportPeriodBeginTime()
//				: getRecordingStartTime();
		return getReportPeriodBeginTime() != null 
				? getReportPeriodBeginTime()
				: getRecordingStartTime();
	}
	
	/** 
	 * Get the Actual Reporting END Time
	 * <p>
	 * If it has been set by setReportPeriodBegin/EndTime() that will be reflected<br>
	 * If it'a the full day, the Recording Start/End time will be reflected<br>
	 */
	@Override
	public Timestamp getReportEndTime()
	{
//		return hasReportPeriod() 
//				? getReportPeriodEndTime()
//				: getRecordingEndTime();
		return getReportPeriodEndTime() != null
				? getReportPeriodEndTime()
				: getRecordingEndTime();
	}

	
	//---------------------------------------------------------------
	// Recording Information
	//---------------------------------------------------------------
	private Timestamp _recordingStartTime  = null;
	private Timestamp _recordingEndTime    = null;
	private int       _recordingSampleTime = -1;
	private String    _recordingDuration   = null;

	public Timestamp getRecordingStartTime()  { return _recordingStartTime ; }
	public Timestamp getRecordingEndTime()    { return _recordingEndTime   ; }
	public int       getRecordingSampleTime() { return _recordingSampleTime; }
	public String    getRecordingDuration()   { return _recordingDuration  ; }
	
	public void refreshRecordingStartEndTime(DbxConnection conn)
	{
		_recordingSampleTime = ReportEntryAbstract.getRecordingSampleTime(conn);

		String schemaName = null;

		// Start/end time for the recording
		String sql = ""
			+ "select min([SessionSampleTime]), max([SessionSampleTime]) \n"
			+ "from "+PersistWriterBase.getTableName(conn, schemaName, PersistWriterBase.SESSION_SAMPLES, null, true) + " \n"
			+ "";

		sql = conn.quotifySqlString(sql);

		try ( Statement stmnt = conn.createStatement() )
		{
			// Unlimited execution time
			stmnt.setQueryTimeout(0);

			try ( ResultSet rs = stmnt.executeQuery(sql) )
			{
				while (rs.next())
				{
					Timestamp startTime = rs.getTimestamp(1);
					Timestamp endTime   = rs.getTimestamp(2);
					
					if (startTime != null && endTime != null)
					{
						_recordingStartTime = startTime;
						_recordingEndTime   = endTime;
						_recordingDuration  = TimeUtils.msToTimeStr("%HH:%MM:%SS", endTime.getTime() - startTime.getTime() );

						// Set Reporting Period
						if (_reportPeriodBeginTimeHour >= 0 && _reportPeriodBeginTimeMinute >= 0)
						{
							Calendar c = Calendar.getInstance();
							c.setTime(_recordingStartTime);
							c.set(Calendar.HOUR_OF_DAY, _reportPeriodBeginTimeHour);
							c.set(Calendar.MINUTE,      _reportPeriodBeginTimeMinute);
							c.set(Calendar.SECOND,      0);
							c.set(Calendar.MILLISECOND, 0);
							
							_reportPeriodBeginTime = new Timestamp(c.getTimeInMillis());
						}
						if (_reportPeriodEndTimeHour >= 0 && _reportPeriodEndTimeMinute >= 0)
						{
							Calendar c = Calendar.getInstance();
							c.setTime(_recordingEndTime);
							c.set(Calendar.HOUR_OF_DAY, _reportPeriodEndTimeHour);
							c.set(Calendar.MINUTE,      _reportPeriodEndTimeMinute);
							if (_reportPeriodEndTimeMinute == 59)
							{
								c.set(Calendar.SECOND,      59);
								c.set(Calendar.MILLISECOND, 999);
							}
							else
							{
								c.set(Calendar.SECOND,      0);
								c.set(Calendar.MILLISECOND, 0);
							}
							
							_reportPeriodEndTime = new Timestamp(c.getTimeInMillis());
						}

						_reportPeriodDuration = TimeUtils.msToTimeStr("%HH:%MM:%SS.%ms", getReportEndTime().getTime() - getReportBeginTime().getTime() ) + "   (HH:MM:SS.millisec)";
					}
				}
			}
		}
		catch(SQLException ex)
		{
			_logger.warn("Problems getting 'Recording start/end time', Caught: " + ex);
		}
	}

	@Override
	public abstract void create() throws InterruptedException, IOException;

	
	
	public String createShowSqlTextDialogHtml()
	{
		StringBuilder sb = new StringBuilder();

		sb.append("																																					\n");
		sb.append("  <!-- Modal: View SqlText dialog -->																											\n");
		sb.append("  <div class='modal fade' id='dbx-view-sqltext-dialog' tabindex='-1' role='dialog' aria-labelledby='dbx-view-sqltext-dialog' aria-hidden='true'>	\n");
//		sb.append("    <div class='modal-dialog modal-dialog-centered modal-lg' role='document'>																	\n");
		sb.append("    <div class='modal-dialog modal-dialog-centered mw-100 w-75' role='document'>																	\n");
		sb.append("      <div class='modal-content'>																												\n");
		sb.append("        <div class='modal-header'>																												\n");
		sb.append("          <h5 class='modal-title' id='dbx-view-sqltext-dialog-title'><b>SQL Text:</b> <span id='dbx-view-sqltext-objectName'></span></h5>	\n");
		sb.append("          <button type='button' class='close' data-dismiss='modal' aria-label='Close'>															\n");
		sb.append("            <span aria-hidden='true'>&times;</span>																								\n");
		sb.append("          </button>																																\n");
		sb.append("        </div>																																	\n");
		sb.append("        <div class='modal-body' style='overflow-x: auto;'>																						\n");
		sb.append("          <div class='scroll-tree' style='width: 3000px;'>																						\n");
		sb.append("            <pre><code id='dbx-view-sqltext-content' class='language-sql line-numbers dbx-view-sqltext-content' ></code></pre>								\n");
		sb.append("          </div>																																	\n");
		sb.append("        </div>																																	\n");
		sb.append("        <div class='modal-footer'>																												\n");
		sb.append("          <button type='button' class='btn btn-secondary' data-dismiss='modal'>Close</button>													\n");
		sb.append("        </div>																																	\n");
		sb.append("      </div>																																		\n");
		sb.append("    </div>																																		\n");
		sb.append("  </div>  																																		\n");
		sb.append("																																					\n");
		
		return sb.toString();
	}
	public String createShowSqlTextDialogJs()
	{
		StringBuilder sb = new StringBuilder();

		sb.append("<script> 																			\n");
		sb.append("  /**																				\n");
		sb.append("   * ---------------------------------------------------------------------------		\n");
		sb.append("   * -- View SQL Text																\n");
		sb.append("   * ---------------------------------------------------------------------------		\n");
		sb.append("   */																				\n");
		sb.append("  $('#dbx-view-sqltext-dialog').on('shown.bs.modal', function(e) {					\n");
		sb.append("      var data = $(e.relatedTarget).data();											\n");
		sb.append("      																				\n");
		sb.append("      console.log('#dbx-view-sqltext-dialog: data: ' + data, data);					\n");
		sb.append("      																				\n");
		sb.append("      $('#dbx-view-sqltext-objectName', this).text(data.objectname);					\n");
		sb.append("      $('#dbx-view-sqltext-content',    this).text(data.tooltip);					\n");
		sb.append("      																				\n");
		sb.append("      // highlight again, since the dialog DOM wasn't visible earlier				\n");
		sb.append("      Prism.highlightAll();															\n");
		sb.append("  });																				\n");
		sb.append("  																					\n");
		sb.append("</script> 																			\n");
		
		return sb.toString();
	}
}
