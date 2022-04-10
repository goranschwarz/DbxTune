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
package com.asetune.pcs.report;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.asetune.pcs.report.content.DbmsConfigIssues;
import com.asetune.pcs.report.content.ase.AseCmDeviceIo;
import com.asetune.pcs.report.content.ase.AseCmSqlStatement;
import com.asetune.pcs.report.content.ase.AseConfiguration;
import com.asetune.pcs.report.content.ase.AseCpuUsageOverview;
import com.asetune.pcs.report.content.ase.AseDbSize;
import com.asetune.pcs.report.content.ase.AseErrorInfo;
import com.asetune.pcs.report.content.ase.AseSlowCmDeviceIo;
import com.asetune.pcs.report.content.ase.AseSpMonitorConfig;
import com.asetune.pcs.report.content.ase.AseStatementCacheUsageOverview;
import com.asetune.pcs.report.content.ase.AseTopCmActiveStatements;
import com.asetune.pcs.report.content.ase.AseTopCmCachedProcs;
import com.asetune.pcs.report.content.ase.AseTopCmObjectActivity;
import com.asetune.pcs.report.content.ase.AseTopCmStmntCacheDetails;
import com.asetune.pcs.report.content.ase.AseTopSlowDynAndStmnt;
import com.asetune.pcs.report.content.ase.AseTopSlowNormalizedSql;
import com.asetune.pcs.report.content.ase.AseTopSlowProcCalls;
import com.asetune.pcs.report.content.ase.AseTopSlowSqlText;
import com.asetune.pcs.report.content.ase.AseUnusedIndexes;
import com.asetune.pcs.report.content.ase.AseWaitStats;
import com.asetune.pcs.report.content.os.OsCpuUsageOverview;
import com.asetune.pcs.report.content.os.OsIoStatOverview;
import com.asetune.pcs.report.content.os.OsIoStatSlowIo;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.StringUtil;

public class DailySummaryReportAseTune 
extends DailySummaryReportDefault
{
	private static Logger _logger = Logger.getLogger(DailySummaryReportAseTune.class);
	
	@Override
	public void addReportEntries()
	{
		// Add the Alarms Active/History
		super.addReportEntries();

		// ASE Error Info
		addReportEntry( new AseErrorInfo(this)              );

		// CPU, just an overview
		addReportEntry( new AseCpuUsageOverview(this)       );
		addReportEntry( new AseWaitStats(this)              );
		addReportEntry( new OsCpuUsageOverview(this)        );
		
		// SQL: from mon SysStatements...
		addReportEntry( new AseCmSqlStatement(this)         );
		addReportEntry( new AseTopSlowNormalizedSql(this, AseTopSlowNormalizedSql.ReportType.CPU_TIME)   );
		addReportEntry( new AseTopSlowNormalizedSql(this, AseTopSlowNormalizedSql.ReportType.WAIT_TIME)  );
		addReportEntry( new AseTopSlowSqlText(this)         );
		addReportEntry( new AseTopSlowProcCalls(this)       );
		addReportEntry( new AseTopSlowDynAndStmnt(this)     );

		// SQL: from Cm's
		addReportEntry( new AseStatementCacheUsageOverview(this) ); // This isn't really SQL, but statistics/charts on the Statement Cache
		addReportEntry( new AseTopCmStmntCacheDetails(this, AseTopCmStmntCacheDetails.ReportType.CPU_TIME) );
		addReportEntry( new AseTopCmStmntCacheDetails(this, AseTopCmStmntCacheDetails.ReportType.WAIT_TIME) );
		addReportEntry( new AseTopCmCachedProcs(this)       );
		addReportEntry( new AseTopCmActiveStatements(this)  );

		// SQL: Accessed Tables
		addReportEntry( new AseTopCmObjectActivity(this, AseTopCmObjectActivity.ReportType.LOGICAL_READS) );
		addReportEntry( new AseTopCmObjectActivity(this, AseTopCmObjectActivity.ReportType.LOCK_WAIT_TIME) );
//		addReportEntry( new AseTopCmObjectActivityLockWaits(this) );
//		addReportEntry( new AseTopCmObjectActivityTabSize(this) );
		addReportEntry( new AseUnusedIndexes(this) );

		// Disk IO Activity (Slow devices & Overall charts)
		addReportEntry( new OsIoStatOverview(this)          );
		addReportEntry( new OsIoStatSlowIo(this)            );
		addReportEntry( new AseCmDeviceIo(this)             );
		addReportEntry( new AseSlowCmDeviceIo(this)         );

		// Database Size
		addReportEntry( new AseDbSize(this)                 );

		// ASE Configuration
		addReportEntry( new AseConfiguration(this)          );
		addReportEntry( new DbmsConfigIssues(this)          );
		addReportEntry( new AseSpMonitorConfig(this)        );
	}
	

	
	
	
	
	
	
	
	
	/**
	 * Create a Map of "other information" like "ASE Page Size" and other information, used in the "Recording Information" section
	 */
	@Override
	public Map<String, String> createDbmsOtherInfoMap(DbxConnection conn)
	{
		Map<String, String> otherInfo = new LinkedHashMap<>();
		String sql;

		//-------------------------------------------------------
		// ASE Page Size
		//-------------------------------------------------------
		sql = "select top 1 [asePageSize] from [CmSummary_abs]";
		
		sql = conn.quotifySqlString(sql);
		try ( Statement stmnt = conn.createStatement() )
		{
			int asePageSize = -1;

			// Unlimited execution time
			stmnt.setQueryTimeout(0);
			try ( ResultSet rs = stmnt.executeQuery(sql) )
			{
				while(rs.next())
					asePageSize = rs.getInt(1);
			}

			otherInfo.put("ASE Page Size", asePageSize + "  (<b>" + (asePageSize/1024) + "K</b>)");
		}
		catch(Exception ex)
		{
			_logger.warn("Problems getting ASE Page Size from DDL Storage.", ex);
		}
		
		//-------------------------------------------------------
		// Sort Order and Charset
		//-------------------------------------------------------
		sql = ""
			    + "select [configText] \n"
			    + "from [MonSessionDbmsConfigText] \n"
			    + "where [configName] = 'AseHelpSort' \n"
			    + "  and [SessionStartTime] = (select max([SessionStartTime]) from [MonSessionDbmsConfigText]) \n"
			    + "";

		sql = conn.quotifySqlString(sql);
		try ( Statement stmnt = conn.createStatement() )
		{
			String configText = "";

			// Unlimited execution time
			stmnt.setQueryTimeout(0);
			try ( ResultSet rs = stmnt.executeQuery(sql) )
			{
				while(rs.next())
					configText = rs.getString(1);
			}

			// +------------------------------------------------------------------+
			// |Character Set = 1, iso_1                                          |
			// |    ISO 8859-1 (Latin-1) - Western European 8-bit character set.  |
			// |Sort Order = 50, bin_iso_1                                        |
			// |    Binary ordering, for the ISO 8859/1 or Latin-1 character set (|
			// |    iso_1).                                                       |
			// +------------------------------------------------------------------+
			String sortOrder = "";
			String charset   = "";

			for (String cfgLine : StringUtil.readLines(configText))
			{
				if (cfgLine.startsWith("|Sort Order = "   )) sortOrder = cfgLine.substring("|Sort Order = "   .length(), cfgLine.length()-1).trim();
				if (cfgLine.startsWith("|Character Set = ")) charset   = cfgLine.substring("|Character Set = ".length(), cfgLine.length()-1).trim();
			}

			otherInfo.put("ASE Sort Order"   , sortOrder);
			otherInfo.put("ASE Character Set", charset);
		}
		catch(Exception ex)
		{
			_logger.warn("Problems getting ASE COnfig Information from table 'MonSessionDbmsConfigText'.", ex);
		}

		//-------------------------------------------------------
		// License Info
		//-------------------------------------------------------
		sql = ""
			    + "select [configText] \n"
			    + "from [MonSessionDbmsConfigText] \n"
			    + "where [configName] = 'AseLicenseInfo' \n"
			    + "  and [SessionStartTime] = (select max([SessionStartTime]) from [MonSessionDbmsConfigText]) \n"
			    + "";
		
		sql = conn.quotifySqlString(sql);
		try ( Statement stmnt = conn.createStatement() )
		{
			String configText = "";

			// Unlimited execution time
			stmnt.setQueryTimeout(0);
			try ( ResultSet rs = stmnt.executeQuery(sql) )
			{
				while(rs.next())
					configText = rs.getString(1);
			}

			// GOOD ASE License
	        // +----------+--------+--------+------------------+-----------+----------+------+-------------+-----------+-----------------------------------------------------------------------------------------------------------------------------------------------------+-----------+-----------------------------------------+
	        // |InstanceID|Quantity|Name    |Edition           |Type       |Version   |Status|LicenseExpiry|GraceExpiry|LicenseID                                                                                                                                            |Filter     |Attributes                               |
	        // +----------+--------+--------+------------------+-----------+----------+------+-------------+-----------+-----------------------------------------------------------------------------------------------------------------------------------------------------+-----------+-----------------------------------------+
	        // |         0|      16|ASE_CORE|Enterprise Edition|CPU license|2020.12310|OK    |(NULL)       |(NULL)     |1387 8AA8 F65A D6A1 DF69 1F98 3983 B139 EE0B 1B19 A408 3575 A7EC DD42 F7C4 0960 810F E7C4 22A9 F408 0B41 2E1B 0E71 7CF6 4861 34FB E717 67BB D8C3 7978|PE=EE;LT=CP|CO=Sybase, Inc.;AS=A;MP=2568;CP=8928;EGO=|
	        // +----------+--------+--------+------------------+-----------+----------+------+-------------+-----------+-----------------------------------------------------------------------------------------------------------------------------------------------------+-----------+-----------------------------------------+

			// Developer Edition
			// +----------+--------+--------+-----------------+----------------------------+---------+------+-------------+-----------+-----------------------------------------------------------------------------------------------------------------------------------------------------+------+------------------------------------------------+
			// |InstanceID|Quantity|Name    |Edition          |Type                        |Version  |Status|LicenseExpiry|GraceExpiry|LicenseID                                                                                                                                            |Filter|Attributes                                      |
			// +----------+--------+--------+-----------------+----------------------------+---------+------+-------------+-----------+-----------------------------------------------------------------------------------------------------------------------------------------------------+------+------------------------------------------------+
			// |         0|       8|ASE_CORE|Developer Edition|Development and test license|2013.1231|OK    |(NULL)       |(NULL)     |0A1F EC50 7138 C903 8D79 F4B9 499B C658 6275 ABE7 32CF F29D A8A2 B9D7 63AF 07DD 38A9 A181 789D 57E6 459D 56D2 B305 0052 B73E 0E82 79CF C471 B49B AF0E|PE=DE |CO=Sybase, Inc.;V=15.0;AS=A;ME=1;MC=25;MP=0;CP=0|
			// +----------+--------+--------+-----------------+----------------------------+---------+------+-------------+-----------+-----------------------------------------------------------------------------------------------------------------------------------------------------+------+------------------------------------------------+

			// Grace Expiry Mode
			// +----------+--------+--------+-------+------+-------+------+-------------+---------------------+---------+-----------+----------+
			// |InstanceID|Quantity|Name    |Edition|Type  |Version|Status|LicenseExpiry|GraceExpiry          |LicenseID|Filter     |Attributes|
			// +----------+--------+--------+-------+------+-------+------+-------------+---------------------+---------+-----------+----------+
			// |         0|       1|ASE_CORE|(NULL) |(NULL)|(NULL) |graced|(NULL)       |2022-04-04 15:46:29.0|(NULL)   |PE=EE;LT=DT|(NULL)    |
			// +----------+--------+--------+-------+------+-------+------+-------------+---------------------+---------+-----------+----------+
			List<String> columns = null;
			for (String cfgLine : StringUtil.readLines(configText))
			{
				// SKIP lines that are not "label" or "data"
				if ( ! cfgLine.startsWith("|") )
					continue;

				// Get all columns
				if (cfgLine.startsWith("|") && columns == null)
				{
					columns = StringUtil.splitOnCharAllowQuotes(cfgLine, '|', true);
					continue;
				}

				// Check that we got desired columns
				if (columns == null)                        continue;
				if ( ! columns.contains("Name"          ) ) continue;
				if ( ! columns.contains("Status"        ) ) continue;
				if ( ! columns.contains("Type"          ) ) continue;
				if ( ! columns.contains("LicenseExpiry" ) ) continue;
				if ( ! columns.contains("GraceExpiry"   ) ) continue;
				
				List<String> valueList = StringUtil.splitOnCharAllowQuotes(cfgLine, '|', true);

				// Only rows where Name=ASE_CORE
				String name          = valueList.get(columns.indexOf("Name"));
				String status        = valueList.get(columns.indexOf("Status"));
				String edition       = valueList.get(columns.indexOf("Edition"));
				String type          = valueList.get(columns.indexOf("Type"));
				String licenseExpiry = valueList.get(columns.indexOf("LicenseExpiry"));
				String graceExpiry   = valueList.get(columns.indexOf("GraceExpiry"));

				String licExpiryStr = "";
				if ( ! "(NULL)".equals(licenseExpiry) ) 
					licExpiryStr = ", LicenseExpiry='" + licenseExpiry + "'";

				String licenseInfo = "Edition='<b>" + edition + "</b>', Type='<b>" + type + "</b>', Status='<b>" + status + "</b>'" + licExpiryStr;

				// in GRACED mode
				if ( ! "(NULL)".equals(graceExpiry  ) ) 
					licenseInfo = "<font color='red'>Is in <b>Grace Period</b> and will stop working at '<b>" + graceExpiry + "</b>'.</font>";

				otherInfo.put("ASE License, " + name, licenseInfo);
			}
		}
		catch(Exception ex)
		{
			_logger.warn("Problems getting ASE COnfig Information from table 'MonSessionDbmsConfigText'.", ex);
		}

		
		//-------------------------------------------------------
		// return
		//-------------------------------------------------------
		return otherInfo;
	}
	
//	// NOTE: The below was not saving the WaitEvent Descriptions to the PCS... so this may be implemented in the future... right now, lets do it statically
//	@Override
//	public MonTablesDictionary createMonTablesDictionary()
//	{
//		return new MonTablesDictionaryAse();
//	}
	
}
