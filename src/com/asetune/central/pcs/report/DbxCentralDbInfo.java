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
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.asetune.central.pcs.report;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.asetune.central.DbxTuneCentral;
import com.asetune.central.cleanup.CentralH2Defrag;
import com.asetune.central.cleanup.CentralH2Defrag.H2StorageInfo;
import com.asetune.central.cleanup.CentralPcsJdbcCleaner;
import com.asetune.central.controllers.OverviewServlet;
import com.asetune.central.controllers.OverviewServlet.H2DbFileType;
import com.asetune.central.pcs.H2WriterStat;
import com.asetune.central.pcs.H2WriterStat.StatEntry;
import com.asetune.gui.ResultSetTableModel;
import com.asetune.pcs.report.DailySummaryReportAbstract;
import com.asetune.pcs.report.content.ReportEntryAbstract;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;
import com.asetune.utils.TimeUtils;

public class DbxCentralDbInfo
extends ReportEntryAbstract
{
	private static Logger _logger = Logger.getLogger(DbxCentralDbInfo.class);

	private ResultSetTableModel _h2FileInfo;
	private ResultSetTableModel _recordings;

	private File   _dataDir;
	private File   _dataDirRes;
	private double _dataDir_freeGb;
	private double _dataDir_totalGb;
	private double _dataDir_pctUsed;
	
	List<StatEntry> _h2FileStats;

	public DbxCentralDbInfo(DailySummaryReportAbstract reportingInstance)
	{
		super(reportingInstance);
	}

	@Override
	public boolean hasShortMessageText()
	{
		return true;
	}

	@Override
	public void writeMessageText(Writer w, MessageType messageType)
	throws IOException
	{
		PrintWriter out = new PrintWriter(w);

		out.println("<p>H2 Databases used by Dbx Central</p>");
		
		out.println("<p>");
		out.println("File system usage at <code>" + _dataDir + "</code>, resolved to <code>" + _dataDirRes + "</code>.<br>");
		out.println(String.format("Free = %.1f GB, Total = %.1f GB, Percent Used = %.1f %%<br>", _dataDir_freeGb, _dataDir_totalGb, _dataDir_pctUsed));
		out.println("</p>");
		
		// H2 Write information
		String fn = "DBX_CENTRAL_H2WriterStatCronTask.log";
		long numOfDays = 30;
		String startDate = TimeUtils.toStringIso8601( new Timestamp( System.currentTimeMillis() - (1000*3600*24*numOfDays) ) ).substring(0, "2019-01-01T".length()) + "00:00"; // Copy first part "YYYY-MM-DDT" then add "00:00" start of day
		String endDate   = TimeUtils.toStringIso8601( new Timestamp( System.currentTimeMillis()                            ) ).substring(0, "2019-01-01T".length()) + "23:59"; // Copy first part "YYYY-MM-DDT" then add "23:59" end of day
		out.println("<a href='/h2ws?filename=" + fn + "&startDate=" + startDate + "&endDate=" + endDate + "'>Show a Chart of H2 Read/Write Statistics for last " + numOfDays + " day</a><br>");
		out.println("<br>");
		
		if (_h2FileStats != null)
		{
			createChart(w, "h2FileSizeMb", "H2 File Size in MB", _h2FileStats);
		}


		if (_h2FileInfo != null)
		{
			out.println( _h2FileInfo.toHtmlTableString("sortable") );
		}

		// What sessions are stored in the Dbx Central database
		if (_recordings != null)
		{
			out.println("<br>");
			out.println("<br>");
			out.println("<p>");
			out.println("What <i>sessions</i> are stored in the Dbx Central database");
			out.println("</p>");

			out.println( _recordings.toHtmlTableString("sortable") );
		}


		out.flush();
//		out.close();
	}

	@Override
	public boolean canBeDisabled()
	{
		return false;
	}

	@Override
	public String getSubject()
	{
		return "DbxCentral DB Storage Information";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false;
	}

//	@Override
//	public String[] getMandatoryTables()
//	{
//		return new String[] { "CmOsMpstat_abs" };
//	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		_dataDir = new File(DbxTuneCentral.getAppDataDir());
		_dataDirRes = null;
		try { _dataDirRes = _dataDir.toPath().toRealPath().toFile(); } catch(IOException ex) { _logger.warn("Problems resolving File->Path->File");}

		_dataDir_freeGb   = _dataDir.getUsableSpace() / 1024.0 / 1024.0 / 1024.0;
		_dataDir_totalGb  = _dataDir.getTotalSpace()  / 1024.0 / 1024.0 / 1024.0;
		_dataDir_pctUsed  = 100.0 - (_dataDir_freeGb / _dataDir_totalGb * 100.0);
		
		//--------------------------------------------------
		// H2 File Info
		if (true)
		{
			H2StorageInfo h2StorageInfo = CentralH2Defrag.getH2StorageInfo();

			_h2FileInfo = ResultSetTableModel.createEmpty("H2 DB File info");

			int col = 0;
			_h2FileInfo.addColumn("File"      , col++, Types.VARCHAR, "varchar", "varchar(255)", 255, 0, "", String.class);
			_h2FileInfo.addColumn("Size GB"   , col++, Types.VARCHAR, "varchar", "varchar(255)", 255, 0, "", String.class);
			_h2FileInfo.addColumn("Size MB"   , col++, Types.VARCHAR, "varchar", "varchar(255)", 255, 0, "", String.class);
			_h2FileInfo.addColumn("Saved Info", col++, Types.VARCHAR, "varchar", "varchar(255)", 255, 0, "", String.class);
			_h2FileInfo.addColumn("JDBC Url"  , col++, Types.VARCHAR, "varchar", "varchar(255)", 255, 0, "", String.class);
			
			for (String file : OverviewServlet.getFilesH2Dbs(H2DbFileType.DBX_CENTRAL))
			{
				ArrayList<Object> row = new ArrayList<Object>();
				File f = new File(file);

				String dbName = f.getName().split("\\.")[0];
				
//				String srvName = dbName;
//				if (dbName.matches(".*_[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]"))
//				{
//					srvName = dbName.substring(0, dbName.length()-"_yyyy-MM-dd".length());
//				}
				String collectorHostname = StringUtil.getHostnameWithDomain();
//				String collectorHostname = hostname;
//				DbxCentralSessions session = centralSessionMap.get(srvName);
//				if (session != null)
//					collectorHostname = session.getCollectorHostname(); 

				String jdbcUrl  = "jdbc:h2:tcp://" + collectorHostname + "/" + dbName + ";IFEXISTS=TRUE;DB_CLOSE_ON_EXIT=FALSE";
				
				String sizeInGB = String.format("%.1f GB", f.length() / 1024.0 / 1024.0 / 1024.0);
				String sizeInMB = String.format("%.1f MB", f.length() / 1024.0 / 1024.0);
				
				row.add( dbName );
				row.add( sizeInGB );
				row.add( sizeInMB );
				row.add( h2StorageInfo );
				row.add( "<code>" + jdbcUrl + "</code>");

				_h2FileInfo.addRow(row);
			}
		}

		//--------------------------------------------------
		// Print some content of the Central Database
		if (true)
		{
			int keepDays = Configuration.getCombinedConfiguration().getIntProperty(CentralPcsJdbcCleaner.PROPKEY_keepDays, CentralPcsJdbcCleaner.DEFAULT_keepDays);

			String sql = ""
					+ "select \n"
					+ "	   [ServerName]            AS [Server Name], \n"
					+ "	   [OnHostname]            AS [On Host], \n"
					+ "	   [ProductString]         AS [Collector Type], \n"
					+ "	   min([SessionStartTime]) AS [First Sample], \n"
					+ "	   max([LastSampleTime])   AS [Last Sample], \n"
					+ "	   datediff(day, max([LastSampleTime]),   CURRENT_TIMESTAMP    ) AS [Last Sample Age In Days], \n"
					+ "	   datediff(day, min([SessionStartTime]), max([LastSampleTime])) AS [Num Of Days Sampled] \n"
					+ "	   " + keepDays + "        AS [Retention Days] \n"
					+ "from [DbxCentralSessions] \n"
					+ "group by [ServerName], [OnHostname], [ProductString] \n"
					+ "order by 1 \n"
					+ "";

				// change '[' and ']' into DBMS specific Quoted Identifier Chars
				sql = conn.quotifySqlString(sql);

				_recordings = executeQuery(conn, sql, true, "Recordings");
		}
		
		//--------------------------------------------------
		// H2 Database file Size Graph
		// The below is "borrowed" from: com.asetune.central.controllers.H2WriterStatServlet
		if (true)
		{
			try
			{
				String filename = "DBX_CENTRAL_H2WriterStatCronTask.log";
				int daysOfHistory = 7;
				int minDuration   = -1;
				
				LocalDateTime now = LocalDateTime.now();
				LocalDateTime then = now.minusDays(7);

				Timestamp startDateTs = Timestamp.valueOf(now.minusDays(daysOfHistory));
				Timestamp endDateTs   = Timestamp.valueOf(now);

//				Timestamp startDateTs = StringUtil.isNullOrBlank(startDateStr) ? null : TimeUtils.parseToTimestamp(startDateStr, "yyyy-MM-dd'T'HH:mm");
//				Timestamp endDateTs   = StringUtil.isNullOrBlank(endDateStr  ) ? null : TimeUtils.parseToTimestamp(endDateStr,   "yyyy-MM-dd'T'HH:mm");

				// get Data
				_h2FileStats = H2WriterStat.parseStatFromLogFile(filename, startDateTs, endDateTs, minDuration);

	//System.out.println("statEntryListSize=" + list.size());
				
			}
			catch (Exception e)
			{
				_logger.info("Problem Getting H2 File Size History, Caught: " + e, e);
			}
		}
	} // end: method


	private void createChart(Writer w, String string, String string2, List<StatEntry> h2FileStats) 
	throws IOException
	{
		// TODO: Can we reuse the "createLineChart" that does both PNG and chart.js charts...

		w.append("Chart for H2 Datafile size is not yet implemented.\n");
	} // end: method


}
