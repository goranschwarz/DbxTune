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
package com.asetune.sql.conn.info;

import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Ver;

public class DbmsVersionInfoSqlServer
extends DbmsVersionInfo
{
//	private static Logger _logger = Logger.getLogger(DbmsVersionInfoSqlServer.class);

	public static final long VERSION_AZURE_SQL_DB            = Ver.ver(9999, 1);
	public static final long VERSION_AZURE_SYNAPSE_ANALYTICS = Ver.ver(9999, 2);
	public static final long VERSION_AZURE_MANAGED_INSTANCE  = Ver.ver(9999, 3);

	// isAzureXxx(versionNum) -- extract only the FIRST part of the version number 9999 0# 00 YYYY MMDD
	//                                                                             ^^^^ ^^               <<<--- high part of number represents 9999=Azure, 01=DB, 02=Synapse, 03=ManagedInstance
	//                                                                                        ^^^^ ^^^^  <<<--- build day of the Azure version in: Year(4) Month(2) Day(2)
	public static boolean isAzureDb              (long versionNumber) { return ((versionNumber / 1_0000_0000L)*1_0000_0000L) == VERSION_AZURE_SQL_DB; }
	public static boolean isAzureSynapseAnalytics(long versionNumber) { return ((versionNumber / 1_0000_0000L)*1_0000_0000L) == VERSION_AZURE_SYNAPSE_ANALYTICS; }
	public static boolean isAzureManagedInstance (long versionNumber) { return ((versionNumber / 1_0000_0000L)*1_0000_0000L) == VERSION_AZURE_MANAGED_INSTANCE; }


	private Boolean _isAzureDb               = null;
	private Boolean _isAzureSynapseAnalytics = null;
	private Boolean _isAzureManagedInstance  = null;

	public DbmsVersionInfoSqlServer(DbxConnection conn)
	{
		super(conn);
		create(conn);
	}

	public DbmsVersionInfoSqlServer(long longVersion)
	{
		super(null);

		setLongVersion(longVersion);
		setAzureDb              (false);
		setAzureSynapseAnalytics(false);
		setAzureManagedInstance (false);
	}

	/**
	 * Do the work in here
	 */
	private void create(DbxConnection conn)
	{
// The below is now done in SqlServerConnection, method: getDbmsVersionNumber/getEngineEdition
//		String sql     = null;
//		String version = null;
//		String edition = null;
//		int    engineEditionInt = -1;
//		String engineEdition = null;
//
//		// Get @@version and @@xxx
//		sql = "SELECT @@version, CAST(SERVERPROPERTY('Edition') as varchar(60)), CAST(SERVERPROPERTY('EngineEdition') as int)";
//		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
//		{
//			while(rs.next())
//			{
//				version          = rs.getString(1);
//				edition          = rs.getString(2);
//				engineEditionInt = rs.getInt   (3);
//
//				if      (engineEditionInt == 1) { engineEdition = "Personal/Desktop";   }
//				else if (engineEditionInt == 2) { engineEdition = "Standard";           }
//				else if (engineEditionInt == 3) { engineEdition = "Enterprise";         }
//				else if (engineEditionInt == 4) { engineEdition = "Express";            }
//				else if (engineEditionInt == 5) { engineEdition = "Azure SQL Database";       _isAzureDb = true; }
//				else if (engineEditionInt == 6) { engineEdition = "Azure SQL Data Warehouse"; _isAzureSynapseAnalytics = true; }
//			//	else if (engineEditionInt == 7) { engineEdition = "";                   }
//				else if (engineEditionInt == 8) { engineEdition = "Azure Managed Instance";   _isAzureManagedInstance = true; }
//				else                            { engineEdition = "-unknown-";          }
//			}
//		}
//		catch (SQLException ex)
//		{
//			_logger.error("Problem discovering SQL-Server version using SQL=|" + sql + "|. Caught: " + ex);
//		}
	}

	public void    setAzureDb              (Boolean b) { _isAzureDb               = b; }
	public void    setAzureSynapseAnalytics(Boolean b) { _isAzureSynapseAnalytics = b; }
	public void    setAzureManagedInstance (Boolean b) { _isAzureManagedInstance  = b; }


	public boolean  isAzureDb()               { return _isAzureDb               != null ? _isAzureDb               : isAzureDb              (_conn.getDbmsVersionNumber()); }
	public boolean  isAzureSynapseAnalytics() { return _isAzureSynapseAnalytics != null ? _isAzureSynapseAnalytics : isAzureSynapseAnalytics(_conn.getDbmsVersionNumber()); }
	public boolean  isAzureManagedInstance()  { return _isAzureManagedInstance  != null ? _isAzureManagedInstance  : isAzureManagedInstance (_conn.getDbmsVersionNumber()); }

	@Override
	public String toString()
	{
		return super.toString() + "[longVer=" + getLongVersion() + ", isAzureDb=" + _isAzureDb + ", isAzureSynapseAnalytics=" + _isAzureSynapseAnalytics + ", isAzureManagedInstance=" + _isAzureManagedInstance + "]";
	}
}
