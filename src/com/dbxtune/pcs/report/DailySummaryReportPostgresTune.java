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

import java.lang.invoke.MethodHandles;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.pcs.report.content.DbmsConfigIssues;
import com.dbxtune.pcs.report.content.os.CmOsNwInfoOverview;
import com.dbxtune.pcs.report.content.os.OsCpuUsageOverview;
import com.dbxtune.pcs.report.content.os.OsIoStatOverview;
import com.dbxtune.pcs.report.content.os.OsIoStatSlowIo;
import com.dbxtune.pcs.report.content.os.OsSpaceUsageOverview;
import com.dbxtune.pcs.report.content.postgres.PostgresConfig;
import com.dbxtune.pcs.report.content.postgres.PostgresConfiguration;
import com.dbxtune.pcs.report.content.postgres.PostgresDbSize;
import com.dbxtune.pcs.report.content.postgres.PostgresLongRunningStmnts;
import com.dbxtune.pcs.report.content.postgres.PostgresSrvIoStats;
import com.dbxtune.pcs.report.content.postgres.PostgresSrvWaitStats;
import com.dbxtune.pcs.report.content.postgres.PostgresStatementsPerDb;
import com.dbxtune.pcs.report.content.postgres.PostgresTopDeadRows;
import com.dbxtune.pcs.report.content.postgres.PostgresTopSql;
import com.dbxtune.pcs.report.content.postgres.PostgresTopTableAccess;
import com.dbxtune.pcs.report.content.postgres.PostgresTopTableSize;
import com.dbxtune.sql.conn.DbxConnection;

public class DailySummaryReportPostgresTune 
extends DailySummaryReportDefault
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	@Override
	public void addReportEntries()
	{
		// Add the Alarms Active/History
		super.addReportEntries();

		// CPU / Waits
		addReportEntry( new OsCpuUsageOverview    (this) );
		addReportEntry( new PostgresSrvIoStats    (this) );
		addReportEntry( new PostgresSrvWaitStats  (this) );

		// Statements
		addReportEntry( new PostgresLongRunningStmnts(this) );

		// SQL
		addReportEntry( new PostgresStatementsPerDb(this) );
		addReportEntry( new PostgresTopSql         (this) );
		addReportEntry( new PostgresTopDeadRows    (this) );
		addReportEntry( new PostgresTopTableAccess (this) );
// TODO; // Implement 'UnusedIndexes' using: (CmPgIndexes)pg_stat_user_indexes.idx_scan == 0, and to get "score" we can use (CmPgTables)pg_stat_user_tables.n_tup_{ins|del|upd} to get an idea how often the table (but not the index) is maintained...
//		addReportEntry( new PostgresTopUnusedIndexes(this) );

		
		// Disk IO Activity
		addReportEntry( new OsSpaceUsageOverview  (this) );
		addReportEntry( new PostgresConfig        (this) );
		addReportEntry( new PostgresDbSize        (this) );  // DB SIZE and or growth
		addReportEntry( new PostgresTopTableSize  (this) );
		addReportEntry( new OsIoStatOverview      (this) );
		addReportEntry( new OsIoStatSlowIo        (this) );

		// Network Activity
		addReportEntry( new CmOsNwInfoOverview    (this) );

		// Configuration
		addReportEntry( new PostgresConfiguration(this) );
		addReportEntry( new DbmsConfigIssues(this)      );
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
		// IS IN RECOVERY MODE
		//-------------------------------------------------------

		// Read ALL rows, and add rows to a list when 'in_recovery' state changes
		// I Tried various SQL Windowing functionality... but this solution was simpler (and more portable)
		sql = ""
			    + "select \n"
			    + "     [in_recovery] \n"
			    + "    ,[SessionSampleTime] \n"
			    + "from [CmSummary_abs] \n"
			    + "";

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		sql = conn.quotifySqlString(sql);
		try ( Statement stmnt = conn.createStatement() )
		{
			// Unlimited execution time
			stmnt.setQueryTimeout(0);
			try ( ResultSet rs = stmnt.executeQuery(sql) )
			{
				List<String> list = new ArrayList<>();

				int     rowcount         = 0;
				boolean last_in_recovery = false;
				
				boolean   in_recovery = false;
				Timestamp ts          = null;

				while(rs.next())
				{
					rowcount++;
					in_recovery = rs.getBoolean  (1);
					ts          = rs.getTimestamp(2);

					if (rowcount == 1)
					{
						list.add(in_recovery + "|" + sdf.format(ts));
						last_in_recovery = in_recovery;
					}
					else
					{
						if (last_in_recovery != in_recovery)
							list.add(in_recovery + "|" + sdf.format(ts));

						last_in_recovery = in_recovery;
					}
				}

				if (list.size() == 1)
				{
					if (in_recovery)
						otherInfo.put("In Recovery/Standby Mode", "<font color='red'>The whole period this Instance has been in Recovery/Standby Mode</font>");
					else
						otherInfo.put("Not In Recovery Mode", "The whole period this Instance has been in Normal (r/w) Mode");
					
				}
				if (list.size() > 1)
				{
					String changeHtmlInfo = "\n<table border='1'> <thead> <tr> <th>in_recovery</th> <th>at_time</th> </tr> </thead>\n<tbody>\n";
					for (String info : list)
					{
						String inRecovery = StringUtils.substringBefore(info, "|");
						String atTime     = StringUtils.substringAfter (info, "|");
						changeHtmlInfo += "<tr> <td>" + (inRecovery.equalsIgnoreCase("true") ? "<font color='red'>TRUE </font>" : "FALSE") + "</td> <td>" + atTime + "</td> </tr>\n";
					}
					changeHtmlInfo += "</tbody>\n</table>\t";

					otherInfo.put("<font color='red'>Recovery Mode Changes</font>", changeHtmlInfo);
				}
			}
		}
		catch(Exception ex)
		{
			_logger.warn("Problems getting 'IN RECOVERY MODE' from PCS Storage.", ex);
		}


		//-------------------------------------------------------
		// return
		//-------------------------------------------------------
		return otherInfo;
	}
}
