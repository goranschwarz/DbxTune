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
package com.dbxtune.sql.conn.info;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

import com.dbxtune.sql.conn.DbxConnection;

public class DbmsVersionInfoPostgres
extends DbmsVersionInfo
{
	private static Logger _logger = Logger.getLogger(DbmsVersionInfoPostgres.class);

	public DbmsVersionInfoPostgres(DbxConnection conn)
	{
		super(conn);
		create(conn);
	}

	public DbmsVersionInfoPostgres(long longVersion)
	{
		super(null);
		setLongVersion(longVersion);
	}

	/**
	 * Do the work in here
	 */
	private void create(DbxConnection conn)
	{
		String sql;

		//---------------------------------------------------
		// Check if it looks like a GCP, AWS, Azure Managed DBMS Instance
		//---------------------------------------------------
		_isGcpManagedDbms   = false;
		_isAwsManagedDbms   = false;
		_isAzureManagedDbms = false;

		sql = "SELECT rolname FROM pg_roles WHERE rolname IN ('cloudsqlsuperuser', 'rds_superuser', 'rdsadmin', 'azure_pg_admin')";
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while(rs.next())
			{
				String rolename = rs.getString(1);

				if (rolename == null)
					continue;

				if      (rolename.startsWith("cloudsql")) _isGcpManagedDbms   = true;
				else if (rolename.startsWith("rds"     )) _isAwsManagedDbms   = true;
				else if (rolename.startsWith("azure"   )) _isAzureManagedDbms = true;
			}
		}
		catch(SQLException ex)
		{
			_logger.error("Problems checking if the Postgres Connection is a GCP/AWS/Azure Managed DBMS Instance. ErrorCode=" + ex.getErrorCode() + ", SqlState=" + ex.getSQLState() + ", Message='" + ex.getMessage() + "', sql=|" + sql + "|.", ex);
		}
	}
}
