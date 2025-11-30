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
package com.dbxtune.cm.rs.helper;

import java.lang.invoke.MethodHandles;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.sql.conn.DbxConnection;

public class AdminLogicalStatusList
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public static ArrayList<AdminLogicalStatusEntry> getList(DbxConnection conn)
	throws SQLException
	{
		String sql = "admin logical_status";
		
//		1> admin logical_status
//		RS> Col# Label                          JDBC Type Name         Guessed DBMS type Source Table
//		RS> ---- ------------------------------ ---------------------- ----------------- ------------
//		RS> 1    Logical Connection Name        java.sql.Types.VARCHAR varchar(75)       -none-      
//		RS> 2    Active Connection Name         java.sql.Types.VARCHAR varchar(75)       -none-      
//		RS> 3    Active Conn State              java.sql.Types.VARCHAR varchar(61)       -none-      
//		RS> 4    Standby Connection Name        java.sql.Types.VARCHAR varchar(75)       -none-      
//		RS> 5    Standby Conn State             java.sql.Types.VARCHAR varchar(61)       -none-      
//		RS> 6    Controller RS                  java.sql.Types.VARCHAR varchar(75)       -none-      
//		RS> 7    Operation in Progress          java.sql.Types.VARCHAR varchar(122)      -none-      
//		RS> 8    State of Operation in Progress java.sql.Types.VARCHAR varchar(122)      -none-      
//		RS> 9    Spid                           java.sql.Types.VARCHAR varchar(5)        -none-      
//		+-----------------------+------------------------+-----------------+------------------------+------------------+-------------------+---------------------+------------------------------+----+
//		|Logical Connection Name|Active Connection Name  |Active Conn State|Standby Connection Name |Standby Conn State|Controller RS      |Operation in Progress|State of Operation in Progress|Spid|
//		+-----------------------+------------------------+-----------------+------------------------+------------------+-------------------+---------------------+------------------------------+----+
//		|[180] LDS1.b2b         |[186] PROD_A1_ASE.b2b   |Active/          |[197] PROD_B1_ASE.b2b   |Active/           |[16777317] PROD_REP|None                 |None                          |    |
//		|[179] LDS1.gorans      |[184] PROD_A1_ASE.gorans|Active/          |[194] PROD_B1_ASE.gorans|Active/           |[16777317] PROD_REP|None                 |None                          |    |
//		|[182] LDS1.Linda       |[190] PROD_A1_ASE.Linda |Active/          |[199] PROD_B1_ASE.Linda |Active/           |[16777317] PROD_REP|None                 |None                          |    |
//		|[181] LDS1.mts         |[188] PROD_A1_ASE.mts   |Active/          |[198] PROD_B1_ASE.mts   |Active/           |[16777317] PROD_REP|None                 |None                          |    |
//		|[183] LDS1.PML         |[192] PROD_A1_ASE.PML   |Active/          |[204] PROD_B1_ASE.PML   |Active/           |[16777317] PROD_REP|None                 |None                          |    |
//		+-----------------------+------------------------+-----------------+------------------------+------------------+-------------------+---------------------+------------------------------+----+
//		(5 rows affected)
		

		ArrayList<AdminLogicalStatusEntry> list = new ArrayList<>();

		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql);)
		{
			while(rs.next())
			{
				AdminLogicalStatusEntry entry = new AdminLogicalStatusEntry(
						rs.getString(1),
						rs.getString(2),
						rs.getString(3),
						rs.getString(4),
						rs.getString(5),
						rs.getString(6),
						rs.getString(7),
						rs.getString(8),
						rs.getString(9)	);

				// FIXME: add a SKIP and INCLUDE list that can work with regexp... just like alarms for AseDatabases
//				if ( ! "gorans".equalsIgnoreCase(entry.getActiveConnDbName()) )
//				{
//					System.out.println("SKIPPING: "+entry.getLogicalConnName()+", active='"+entry.getActiveConnName()+"', standby'"+entry.getStandbyConnName()+"'.");
//					continue;
//				}
				
//				//-------------------------------------------------------
//				// LastDbBackupAgeInHours
//				//-------------------------------------------------------
//				if (isSystemAlarmsForColumnEnabled("LastDbBackupAgeInHours"))
//				{
//					Double val = cm.getAbsValueAsDouble(r, "LastDbBackupAgeInHours");
//					if (val != null)
//					{
//						int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_LastDbBackupAgeInHours, DEFAULT_alarm_LastDbBackupAgeInHours);
//						if (val.intValue() > threshold)
//						{
//							// Get config 'skip some transaction names'
//							String keepDbRegExp  = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_LastDbBackupAgeInHoursForDbs,  DEFAULT_alarm_LastDbBackupAgeInHoursForDbs);
//							String skipDbRegExp  = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_LastDbBackupAgeInHoursSkipDbs, DEFAULT_alarm_LastDbBackupAgeInHoursSkipDbs);
//							String keepSrvRegExp = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_LastDbBackupAgeInHoursForSrv,  DEFAULT_alarm_LastDbBackupAgeInHoursForSrv);
//							String skipSrvRegExp = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_LastDbBackupAgeInHoursSkipSrv, DEFAULT_alarm_LastDbBackupAgeInHoursSkipSrv);
	//
//							// The below could have been done with neasted if(keep-db), if(keep-srv), if(!skipDb), if(!skipSrv) doAlarm=true; 
//							// Below is more readable, from a variable context point-of-view, but HARDER to understand
//							boolean doAlarm = true; // note: this must be set to true at start, otherwise all below rules will be disabled (it "stops" processing at first doAlarm==false)
//							doAlarm = (doAlarm && (StringUtil.isNullOrBlank(keepDbRegExp)  ||   dbname     .matches(keepDbRegExp ))); //     matches the KEEP Db  regexp
//							doAlarm = (doAlarm && (StringUtil.isNullOrBlank(keepSrvRegExp) ||   dbmsSrvName.matches(keepSrvRegExp))); //     matches the KEEP Srv regexp
//							doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipDbRegExp)  || ! dbname     .matches(skipDbRegExp ))); // NO match in the SKIP Db  regexp
//							doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipSrvRegExp) || ! dbmsSrvName.matches(skipSrvRegExp))); // NO match in the SKIP Srv regexp
	//
//							// NO match in the SKIP regexp
//							if (doAlarm)
//							{
//								AlarmHandler.getInstance().addAlarm( new AlarmEventOldBackup(cm, threshold, "DB", dbname, val.intValue()) );
//							}
//						}
//					}
//				}
				
				list.add(entry);
			}
		}
		catch (SQLException ex)
		{
			//---------------------------------------------------------------------------
			// If 'admin logical_status' doesn't have any Logical Connections...
			//---------------------------------------------------------------------------
			// java.sql.SQLException: JZ0R2: No result set for this query.
			if ("JZ0R2".equals(ex.getSQLState()))
			{
				// Just do a DEBUG write here
				// The upper component 'CounterSampleWsIterator' will write: 'Skipping collecting WS Latency statistics. 'admin logical_status' returned 0 rows.'
				_logger.debug("No 'Logical Connections' was detected. The command 'admin logical_status' did NOT return any ResultSet");
				// Note: we will still return the EMPTY List at the bottom.
			}
			else
			{
				throw ex;
			}
		}
		
		return list;
	}
}
