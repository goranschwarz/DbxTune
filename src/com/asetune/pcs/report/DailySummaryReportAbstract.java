/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
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
package com.asetune.pcs.report;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;

import com.asetune.Version;
import com.asetune.pcs.PersistentCounterHandler;
import com.asetune.pcs.report.content.DailySummaryReportContent;
import com.asetune.pcs.report.senders.IReportSender;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;
import com.asetune.utils.FileUtils;
import com.asetune.utils.StringUtil;
import com.asetune.utils.TimeUtils;
import com.asetune.utils.Ver;
import com.asetune.utils.VersionShort;

public abstract class DailySummaryReportAbstract
implements IDailySummaryReport
{
	private static Logger _logger = Logger.getLogger(DailySummaryReportAbstract.class);

	private DbxConnection _conn = null;
	private IReportSender _sender = null;
	private String        _serverName = null;
	
	private DailySummaryReportContent _reportContent = null; 

	@Override
	public void          setConnection(DbxConnection conn) { _conn = conn; }
	public DbxConnection getConnection()                   { return _conn; }

	@Override
	public void   setServerName(String serverName) { _serverName = serverName; }
	public String getServerName()                  { return _serverName; }

	@Override public void                      setReportContent(DailySummaryReportContent content) { _reportContent = content; }
	@Override public DailySummaryReportContent getReportContent()                                  { return _reportContent; }

	@Override
	public void setReportSender(IReportSender reportSender)
	{
		_sender = reportSender;
	}

	@Override
	public void init()
	throws Exception
	{
		if (_sender == null)
			throw new RuntimeException("Can't send Daily Summary Report. The sender class is null.");

		_sender.init();
		_sender.printConfig();		
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
		String ts = TimeUtils.getCurrentTimeForFileNameHM();

		// Get server name and remove inappropriate characters so the file save do not fail.
		String srvName = content.getServerName();
		srvName = FileUtils.toSafeFileName(srvName);

		// Nothing to report?
		String ntr = content.hasNothingToReport() ? ".-NTR-" : "";

		// Filename
		String saveToFileName = saveDir + File.separatorChar + srvName + "." + ts + ntr + ".html";
		File   saveToFile = new File(saveToFileName);

		// Produce the content
		String htmlReport = content.getReportAsHtml();

		// Write the file
		try
		{
			_logger.info("Saving of DailyReport to file '" + saveToFile.getAbsolutePath() + "'.");
			
			org.apache.commons.io.FileUtils.write(saveToFile, htmlReport, StandardCharsets.UTF_8.name());
		}
		catch (IOException ex)
		{
			_logger.error("Problems writing Daily Report to file '" + saveToFileName + "'. Caught: "+ex, ex);
		}
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
	

//--------------------------------------------------------------------
// THE BELOW is not used for the moment... uncomment and test it
// OR: do "select * from CmXXXX_diff where 1=2" to get all column names... and then check if desired column names are available
//--------------------------------------------------------------------
	private String _versionString = "";

	/** Get the DBMS Version string stored by any of the DbxTune collectors */
	public String getSrvVersionStr()
	{
		getSrvVersionNum();
		return _versionString;
	}

	/** Get the DBMS Version string stored by any of the DbxTune collectors, and then parse it into a number */
	public long getSrvVersionNum()
	{
		DbxConnection conn = getConnection();
		
		// Get DbxTune application name from the recorded session database
		String appName = "";
		if (true) // get from database...
		{
			String sql = "select max([ProductString]) from [MonVersionInfo]";

			sql = conn.quotifySqlString(sql);
			try ( Statement stmnt = conn.createStatement() )
			{
				// Unlimited execution time
				stmnt.setQueryTimeout(0);
				try ( ResultSet rs = stmnt.executeQuery(sql) )
				{
					while(rs.next())
						appName = rs.getString(1);
				}
			}
			catch(SQLException ex)
			{
				_logger.warn("Problems getting DbxTune Collector string using SQL '"+sql+"'. Caught: " + ex);
			}
		}
		if (StringUtil.isNullOrBlank(appName))
			appName = Version.getAppName();
		

		String colName = "srvVersion";
		String tabName = "CmSummary_abs";

		// What table/column is the "server version" stored in
		if      ("AseTune"      .equalsIgnoreCase(appName)) { colName = "srvVersion";  tabName = "CmSummary_abs"; }
		else if ("IqTune"       .equalsIgnoreCase(appName)) { colName = "atAtVersion"; tabName = "CmSummary_abs"; }
		else if ("RsTune"       .equalsIgnoreCase(appName)) { colName = "rsVersion";   tabName = "CmSummary_abs"; }
		else if ("RaxTune"      .equalsIgnoreCase(appName)) { colName = "NOT_STORED";  tabName = "CmSummary_abs"; }
		else if ("SqlServerTune".equalsIgnoreCase(appName)) { colName = "srvVersion";  tabName = "CmSummary_abs"; }
		else if ("PostgresTune" .equalsIgnoreCase(appName)) { colName = "version";     tabName = "CmSummary_abs"; }
		else if ("MySqlTune"    .equalsIgnoreCase(appName)) { colName = "version";     tabName = "CmSummary_abs"; }
		else if ("OracleTune"   .equalsIgnoreCase(appName)) { colName = "VERSION";     tabName = "CmSummary_abs"; }
		else if ("Db2Tune"      .equalsIgnoreCase(appName)) { colName = "VERSION";     tabName = "CmSummary_abs"; }
		else if ("HanaTune"     .equalsIgnoreCase(appName)) { colName = "VERSION";     tabName = "CmSummary_abs"; }
		
		// Construct SQL and get the version string
		String sql = "select max([" + colName + "]) from [" + tabName + "]";
		String versionString = "";
		
		sql = conn.quotifySqlString(sql);
		try ( Statement stmnt = conn.createStatement() )
		{
			// Unlimited execution time
			stmnt.setQueryTimeout(0);
			try ( ResultSet rs = stmnt.executeQuery(sql) )
			{
				while(rs.next())
				{
					versionString = rs.getString(1);
				}
			}
		}
		catch(SQLException ex)
		{
			_logger.warn("Problems getting version string using SQL '"+sql+"'. Caught: " + ex);
		}

		
		// Parse the Version String into a number
		long ver = 0; 

		if      ("AseTune"      .equalsIgnoreCase(appName)) { ver = Ver.sybVersionStringToNumber      (versionString); }
		else if ("IqTune"       .equalsIgnoreCase(appName)) { ver = Ver.iqVersionStringToNumber       (versionString); }
		else if ("RsTune"       .equalsIgnoreCase(appName)) { ver = Ver.sybVersionStringToNumber      (versionString); }
		else if ("RaxTune"      .equalsIgnoreCase(appName)) { ver = Ver.sybVersionStringToNumber      (versionString); }
		else if ("SqlServerTune".equalsIgnoreCase(appName)) { ver = Ver.sqlServerVersionStringToNumber(versionString); }
		else if ("PostgresTune" .equalsIgnoreCase(appName)) { ver = Ver.shortVersionStringToNumber(VersionShort.parse(versionString)); }
		else if ("MySqlTune"    .equalsIgnoreCase(appName)) { ver = Ver.shortVersionStringToNumber(VersionShort.parse(versionString)); }
		else if ("OracleTune"   .equalsIgnoreCase(appName)) { ver = Ver.oracleVersionStringToNumber   (versionString); }
		else if ("Db2Tune"      .equalsIgnoreCase(appName)) { ver = Ver.db2VersionStringToNumber      (versionString); }
		else if ("HanaTune"     .equalsIgnoreCase(appName)) { ver = Ver.hanaVersionStringToNumber     (versionString); }

		_versionString = versionString;
		return ver;
	}


	@Override
	public abstract void create();

}
