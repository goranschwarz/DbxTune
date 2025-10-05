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
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.report.content.DbmsConfigIssues;
import com.dbxtune.pcs.report.content.os.CmOsNwInfoOverview;
import com.dbxtune.pcs.report.content.os.OsCpuUsageOverview;
import com.dbxtune.pcs.report.content.os.OsIoStatOverview;
import com.dbxtune.pcs.report.content.os.OsIoStatSlowIo;
import com.dbxtune.pcs.report.content.os.OsSpaceUsageOverview;
import com.dbxtune.pcs.report.content.sqlserver.SqlServerCmDeviceIo;
import com.dbxtune.pcs.report.content.sqlserver.SqlServerConfiguration;
import com.dbxtune.pcs.report.content.sqlserver.SqlServerCpuUsageOverview;
import com.dbxtune.pcs.report.content.sqlserver.SqlServerDbSize;
import com.dbxtune.pcs.report.content.sqlserver.SqlServerDeadlocks;
import com.dbxtune.pcs.report.content.sqlserver.SqlServerJobScheduler;
import com.dbxtune.pcs.report.content.sqlserver.SqlServerMissingIndexes;
import com.dbxtune.pcs.report.content.sqlserver.SqlServerPlanCacheHistory;
import com.dbxtune.pcs.report.content.sqlserver.SqlServerQueryStore;
import com.dbxtune.pcs.report.content.sqlserver.SqlServerSlowCmDeviceIo;
import com.dbxtune.pcs.report.content.sqlserver.SqlServerTopCmExecCursors;
import com.dbxtune.pcs.report.content.sqlserver.SqlServerTopCmExecFunctionStats;
import com.dbxtune.pcs.report.content.sqlserver.SqlServerTopCmExecProcedureStats;
import com.dbxtune.pcs.report.content.sqlserver.SqlServerTopCmExecQueryStatPerDb;
import com.dbxtune.pcs.report.content.sqlserver.SqlServerTopCmExecQueryStats;
import com.dbxtune.pcs.report.content.sqlserver.SqlServerTopCmExecTriggerStats;
import com.dbxtune.pcs.report.content.sqlserver.SqlServerTopCmIndexOpStat;
import com.dbxtune.pcs.report.content.sqlserver.SqlServerTopCmIndexPhysicalAvgPageUsedPct;
import com.dbxtune.pcs.report.content.sqlserver.SqlServerTopCmIndexPhysicalTabSize;
import com.dbxtune.pcs.report.content.sqlserver.SqlServerTopCmTableSize;
import com.dbxtune.pcs.report.content.sqlserver.SqlServerUnusedIndexes;
import com.dbxtune.pcs.report.content.sqlserver.SqlServerWaitStats;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.StringUtil;

public class DailySummaryReportSqlServerTune 
extends DailySummaryReportDefault
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	@Override
	public void addReportEntries()
	{
		// Add the Alarms Active/History
		super.addReportEntries();

		// CPU
		addReportEntry( new SqlServerCpuUsageOverview(this)   );
		addReportEntry( new SqlServerWaitStats(this)          );
		addReportEntry( new OsCpuUsageOverview(this)          );
		
		// Job Agent/Scheduler
		addReportEntry( new SqlServerJobScheduler           (this));
		
		// SQL
		addReportEntry( new SqlServerPlanCacheHistory       (this));        // Check if the Plan Cache can be trusted... https://www.brentozar.com/archive/2018/07/tsql2sday-how-much-plan-cache-history-do-you-have/
		addReportEntry( new SqlServerDeadlocks              (this));
//		addReportEntry( new SqlServerTopCmExecQueryStatsDb  (this)); // Older version of the "DB" statistics
		addReportEntry( new SqlServerTopCmExecQueryStatPerDb(this));
		addReportEntry( new SqlServerTopCmExecQueryStats    (this, SqlServerTopCmExecQueryStats.ReportType.CPU_TIME));
		addReportEntry( new SqlServerTopCmExecQueryStats    (this, SqlServerTopCmExecQueryStats.ReportType.EST_WAIT_TIME));
		addReportEntry( new SqlServerTopCmExecQueryStats    (this, SqlServerTopCmExecQueryStats.ReportType.TEMPDB_SPILLS));
		addReportEntry( new SqlServerTopCmExecQueryStats    (this, SqlServerTopCmExecQueryStats.ReportType.LOGICAL_READS));
		addReportEntry( new SqlServerTopCmExecQueryStats    (this, SqlServerTopCmExecQueryStats.ReportType.LOGICAL_WRITES));
		addReportEntry( new SqlServerTopCmExecQueryStats    (this, SqlServerTopCmExecQueryStats.ReportType.PHYSICAL_READS));
		addReportEntry( new SqlServerTopCmExecQueryStats    (this, SqlServerTopCmExecQueryStats.ReportType.EXECUTION_COUNT));
//		addReportEntry( new SqlServerTopCmExecQueryStats    (this, SqlServerTopCmExecQueryStats.ReportType.RECENTLY_COMPILED));
		addReportEntry( new SqlServerTopCmExecQueryStats    (this, SqlServerTopCmExecQueryStats.ReportType.MEMORY_GRANTS));
		addReportEntry( new SqlServerTopCmExecProcedureStats(this));	// Part of mail-message
		addReportEntry( new SqlServerTopCmExecFunctionStats (this));
		addReportEntry( new SqlServerTopCmExecTriggerStats  (this));
		addReportEntry( new SqlServerTopCmExecCursors       (this));

		// QueryStore
		addReportEntry( new SqlServerQueryStore(this)         );

		// Disk IO Activity
		addReportEntry( new SqlServerCmDeviceIo(this)         );
		addReportEntry( new SqlServerSlowCmDeviceIo(this)     );
		addReportEntry( new OsSpaceUsageOverview(this)        );
		addReportEntry( new OsIoStatOverview(this)            );
		addReportEntry( new OsIoStatSlowIo(this)              );

		// Network Activity
		addReportEntry( new CmOsNwInfoOverview(this)          );

		// SQL: Accessed Tables
		addReportEntry( new SqlServerTopCmIndexOpStat(this, SqlServerTopCmIndexOpStat.ReportType.BY_LOCKS) );
		addReportEntry( new SqlServerTopCmIndexOpStat(this, SqlServerTopCmIndexOpStat.ReportType.BY_WAITS) );
		addReportEntry( new SqlServerTopCmIndexOpStat(this, SqlServerTopCmIndexOpStat.ReportType.BY_CRUD)  );
		addReportEntry( new SqlServerTopCmIndexOpStat(this, SqlServerTopCmIndexOpStat.ReportType.BY_IO)    );
		addReportEntry( new SqlServerTopCmTableSize(this) );
		addReportEntry( new SqlServerTopCmIndexPhysicalTabSize(this) );
		addReportEntry( new SqlServerTopCmIndexPhysicalAvgPageUsedPct(this) );

		// SQL: Missing/Unused Indexes
		addReportEntry( new SqlServerMissingIndexes(this) );
		addReportEntry( new SqlServerUnusedIndexes(this) );

		// Database Size
		addReportEntry( new SqlServerDbSize(this)             );

		// Configuration
		addReportEntry( new SqlServerConfiguration(this)      );
		addReportEntry( new DbmsConfigIssues(this)            );
	}




	/**
	 * Create a Map of "other information" like "SOrt Order" and other information, used in the "Recording Information" section
	 */
	@Override
	public Map<String, String> createDbmsOtherInfoMap(DbxConnection conn)
	{
		Map<String, String> otherInfo = new LinkedHashMap<>();
		String sql;

		//-------------------------------------------------------
		// License Info
		//-------------------------------------------------------
		// This can be grabbed from the Version String
		sql = "select top 1 [srvVersion] from [CmSummary_abs]";
		
		sql = conn.quotifySqlString(sql);
		try ( Statement stmnt = conn.createStatement() )
		{
			String versionStr = "";

			// Unlimited execution time
			stmnt.setQueryTimeout(0);
			try ( ResultSet rs = stmnt.executeQuery(sql) )
			{
				while(rs.next())
					versionStr = rs.getString(1);
			}

			// Remove: newLines, Tabs, and collapse double space into single space
			versionStr = versionStr.replace('\n', ' ').replace('\t', ' ');
			while(versionStr.contains("  "))
				versionStr = versionStr.replace("  ", " ");

			String edition = "";
			if      (versionStr.contains("Microsoft Corporation Standard Edition"  )) edition = "Standard Edition";
			else if (versionStr.contains("Microsoft Corporation Enterprise Edition")) edition = "Enterprise Edition";
			else if (versionStr.contains("Microsoft Corporation Web Edition"       )) edition = "Web Edition";
			else if (versionStr.contains("Microsoft Corporation Developer Edition" )) edition = "Developer Edition";
			else if (versionStr.contains("Microsoft Corporation Express Edition"   )) edition = "Express Edition";
			else if (versionStr.contains("Microsoft SQL Azure "                    )) edition = "Azure";
			
			if (StringUtil.hasValue(edition))
				otherInfo.put("SQL Server Edition", edition);
		}
		catch(Exception ex)
		{
			_logger.warn("Problems getting first row for @@version from CmSummary_abs.", ex);
		}
		

		
		//-------------------------------------------------------
		// Collation/Sort Order
		//-------------------------------------------------------
		sql = ""
			    + "select [configText] \n"
			    + "from [MonSessionDbmsConfigText] \n"
			    + "where [configName] = 'SqlServerHelpSort' \n"
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

			// +-------------------------------------------------------------------------------------------------------------------------------+
			// |Server default collation                                                                                                       |
			// +-------------------------------------------------------------------------------------------------------------------------------+
			// |Latin1-General-100, case-insensitive, accent-sensitive, kanatype-insensitive, width-insensitive, supplementary characters, UTF8|
			// +-------------------------------------------------------------------------------------------------------------------------------+
			// Rows 1
			// +--------------------------------+
			// |SERVERPROPERTY_Collation        |
			// +--------------------------------+
			// |Latin1_General_100_CI_AS_SC_UTF8|
			// +--------------------------------+
			// Rows 1
			
			String collationName = "";
			String collationDesc = "";

			for (ResultSetTableModel rstm : ResultSetTableModel.parseTextTables(configText))
			{
				if (rstm.hasColumn("Server default collation"))
				{
					collationDesc = rstm.getValueAsString(0, 0); // rowPos & colPos is at ModelPosition, starting at 0
				}

				if (rstm.hasColumn("SERVERPROPERTY_Collation"))
				{
					collationName = rstm.getValueAsString(0, 0); // rowPos & colPos is at ModelPosition, starting at 0
				}
			}

			// Put them in the "desired" order
			if (StringUtil.hasValue(collationName)) otherInfo.put("Collation Name"       , collationName);
			if (StringUtil.hasValue(collationDesc)) otherInfo.put("Collation Description", collationDesc);
		}
		catch(Exception ex)
		{
			_logger.warn("Problems getting SQL Server Config Information from table 'MonSessionDbmsConfigText'.", ex);
		}
		

		
		//-------------------------------------------------------
		// WSFC/Windows Clustering
		//-------------------------------------------------------
		sql = ""
			    + "select [configText] \n"
			    + "from [MonSessionDbmsConfigText] \n"
			    + "where [configName] = 'SqlServerClusterNodes' \n"
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

			// FIXME: describe how the table output looks like.
			// THE BELOW IS FOR 'SqlServerHelpSort' SO IT SHOULD BE CHANGED
			// +-------------------------------------------------------------------------------------------------------------------------------+
			// |Server default collation                                                                                                       |
			// +-------------------------------------------------------------------------------------------------------------------------------+
			// |Latin1-General-100, case-insensitive, accent-sensitive, kanatype-insensitive, width-insensitive, supplementary characters, UTF8|
			// +-------------------------------------------------------------------------------------------------------------------------------+
			// Rows 1
			// +--------------------------------+
			// |SERVERPROPERTY_Collation        |
			// +--------------------------------+
			// |Latin1_General_100_CI_AS_SC_UTF8|
			// +--------------------------------+
			// Rows 1
			
			String deploymentType = "";
			String clusterInfo    = "";
//			String availabilityGroupInfo = "";
			ResultSetTableModel rstmAvailabilityGroupInfo = ResultSetTableModel.createEmpty("availabilityGroupInfoHtmlTable");

			for (ResultSetTableModel rstm : ResultSetTableModel.parseTextTables(configText))
			{
				if (rstm.hasColumn("DeploymentType"))
				{
					if (rstm.getRowCount() > 0)
					{
						deploymentType = rstm.getValueAsString(0, 0); // rowPos & colPos is at ModelPosition, starting at 0
					}
				}

				// WSFC - Windows Server Failover Clustering
				if (rstm.hasColumn("NodeName"))
				{
					String entrySeparator = "";
					for (int r=0; r<rstm.getRowCount(); r++)
					{
						String nodeName          = rstm.getValueAsString(r, "NodeName"          , true, "unknown");
						String statusDescription = rstm.getValueAsString(r, "status_description", true, "");
						String isCurrentOwner    = rstm.getValueAsString(r, "is_current_owner"  , true, "");

						clusterInfo += entrySeparator + "NodeName='" + nodeName + "', isCurrentOwner=" + isCurrentOwner + ", statusDescription='" + statusDescription + "'";
//						entrySeparator = "; ";
						entrySeparator = "; <BR>";
					}
				}

				// AG - Availability Groups
				if (rstm.hasColumn("failover_mode_desc"))
				{
					Map<String, Set<String>> primary_agName_SqlNameSet   = new LinkedHashMap<>();
					Map<String, Set<String>> secondary_agName_SqlNameSet = new LinkedHashMap<>();
					Map<String, Set<String>> failoverMode_agName_ModeSet = new LinkedHashMap<>();
					Map<String, Set<String>> agType_agName_TypeSet       = new LinkedHashMap<>();
					
					for (int r=0; r<rstm.getRowCount(); r++)
					{
						String AGName             = rstm.getValueAsString(r, "AGName"             , true, "unknown");
						String sql_server_name    = rstm.getValueAsString(r, "sql_server_name"    , true, "");
						String role_desc          = rstm.getValueAsString(r, "role_desc"          , true, "");
						String failover_mode_desc = rstm.getValueAsString(r, "failover_mode_desc" , true, "");
						String AG_Type            = rstm.getValueAsString(r, "AG_Type"            , true, "");

						// primary_agName_SqlNameSet
						if ("PRIMARY".equals(role_desc))
						{
							String key = AGName;
							Set<String> sqlNameSet = primary_agName_SqlNameSet.computeIfAbsent(key, k -> new LinkedHashSet<String>());
							sqlNameSet.add(sql_server_name);
						}
						// secondary_agName_SqlNameSet
						if ("SECONDARY".equals(role_desc))
						{
							String key = AGName;
							Set<String> sqlNameSet = secondary_agName_SqlNameSet.computeIfAbsent(key, k -> new LinkedHashSet<String>());
							sqlNameSet.add(sql_server_name);
						}

						// failoverMode_agName_ModeSet
						if ("AUTOMATIC".equals(failover_mode_desc) || "MANUAL".equals(failover_mode_desc))
						{
							String key = AGName;
							Set<String> modeSet = failoverMode_agName_ModeSet.computeIfAbsent(key, k -> new LinkedHashSet<String>());
							modeSet.add(failover_mode_desc);
						}

						// agType_agName_ModeSet
						if (true)
						{
							String key = AGName;
							Set<String> modeSet = agType_agName_TypeSet.computeIfAbsent(key, k -> new LinkedHashSet<String>());
							modeSet.add(AG_Type);
						}
					}

//					// Build 'availabilityGroupInfo'
//					//    AGNAME - PRIMARY=server_name, SECONDARY=server_name_list
//					String entrySeparator = "";
//					for (Entry<String, Set<String>> entry : primary_agName_SqlNameSet.entrySet())
//					{
//						String      agName              = entry.getKey();
//						Set<String> primarySqlNameSet   = entry.getValue();
//						Set<String> secondarySqlNameSet = secondary_agName_SqlNameSet.get(agName);
//						Set<String> failoverMode        = failoverMode_agName_ModeSet.get(agName);
//						Set<String> AG_Type             = agType_agName_TypeSet      .get(agName);
//
//						availabilityGroupInfo += entrySeparator + "AG Name='" + agName + "', AG Type=" + AG_Type + ", Failover Mode=" + failoverMode + ", Primary=" + primarySqlNameSet + ", Secondary=" + secondarySqlNameSet;
////						entrySeparator = "; ";
//						entrySeparator = "; <BR>";
//					}

					// Build 'availabilityGroupInfoHtmlTable'
//					rstmAvailabilityGroupInfo = ResultSetTableModel.createEmpty("availabilityGroupInfoHtmlTable");
					rstmAvailabilityGroupInfo.addColumn("AG Name"      , 0, Types.VARCHAR, "varchar", "varchar(255)", 255, 0, "", String.class);
					rstmAvailabilityGroupInfo.addColumn("AG Type"      , 1, Types.VARCHAR, "varchar", "varchar(255)", 255, 0, "", String.class);
					rstmAvailabilityGroupInfo.addColumn("Failover Mode", 2, Types.VARCHAR, "varchar", "varchar(255)", 255, 0, "", String.class);
					rstmAvailabilityGroupInfo.addColumn("Primary"      , 3, Types.VARCHAR, "varchar", "varchar(255)", 255, 0, "", String.class);
					rstmAvailabilityGroupInfo.addColumn("Secondary"    , 4, Types.VARCHAR, "varchar", "varchar(255)", 255, 0, "", String.class);
					
					for (Entry<String, Set<String>> entry : primary_agName_SqlNameSet.entrySet())
					{
						String      agName              = entry.getKey();
						Set<String> primarySqlNameSet   = entry.getValue();
						Set<String> secondarySqlNameSet = secondary_agName_SqlNameSet.get(agName);
						Set<String> failoverMode        = failoverMode_agName_ModeSet.get(agName);
						Set<String> AG_Type             = agType_agName_TypeSet      .get(agName);

						ArrayList<Object> row = new ArrayList<>();
						row.add(agName);
						row.add(StringUtil.toCommaStr( AG_Type             ));
						row.add(StringUtil.toCommaStr( failoverMode        ));
						row.add(StringUtil.toCommaStr( primarySqlNameSet   ));
						row.add(StringUtil.toCommaStr( secondarySqlNameSet ));

						rstmAvailabilityGroupInfo.addRow(row);
					}
				}
			}

			// Put them in the "desired" order
			if (StringUtil.hasValue(deploymentType))        otherInfo.put("Deployment Type"        , deploymentType);
			if (StringUtil.hasValue(clusterInfo))           otherInfo.put("WSFC Cluster Info"      , clusterInfo);
//			if (StringUtil.hasValue(availabilityGroupInfo)) otherInfo.put("Availability Group Info", availabilityGroupInfo);
			if (rstmAvailabilityGroupInfo.hasRows())        otherInfo.put("Availability Group Info", rstmAvailabilityGroupInfo.toHtmlTableString("ag-table-info"));
		}
		catch(Exception ex)
		{
			_logger.warn("Problems getting SQL Server Config Information from table 'MonSessionDbmsConfigText'.", ex);
		}

		//-------------------------------------------------------
		// return
		//-------------------------------------------------------
		return otherInfo;
	}

}
