/*******************************************************************************
 * Copyright (C) 2010-2027 Goran Schwarz
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

import java.lang.invoke.MethodHandles;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.sql.conn.DbxConnection;

public class DbmsVersionInfoMySql
extends DbmsVersionInfo
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private String  _versionString      = null;  // @@version          for example: 8.4.11, 9.7.2, 11.8.3-MariaDB-ubu2404, 8.0.36-28 (Percona)
	private String  _versionComment     = null;  // @@version_comment  for example: MySQL Community Server - GPL, MySQL Enterprise Server - Commercial, Percona Server (GPL)...

	private boolean _isMariaDb          = false;
	private boolean _isPercona          = false;
	private boolean _isEnterprise       = false;
	private boolean _isAurora           = false;
	private String  _auroraVersion      = null;

	private long    _serverId           = -1;
	private String  _serverUuid         = null;  // Not in MariaDB
	private boolean _isReadOnly         = false;
	private boolean _isSuperReadOnly    = false; // Not in MariaDB
	private boolean _isPerfSchemaOn     = false; // @@performance_schema: ON by default in MySQL, OFF by default in MariaDB
	private boolean _isLogBin           = false;
	private String  _gtidMode           = null;  // OFF, OFF_PERMISSIVE, ON_PERMISSIVE, ON   (Not in MariaDB)

	private boolean _hasSysSchema       = false; // Can we read the 'sys' schema (it is installed by default in MySQL 5.7 and above)
	private String  _sysVersion         = null;
	private int     _replicaChannels    = 0;     // Number of configured replication channels. Above 0 = this server is a replica

	public DbmsVersionInfoMySql(DbxConnection conn)
	{
		super(conn);
		create(conn);
	}

	/** Used when we do not have a connection (and by the "fall back to the 8.0 SQL" logic in the CM's). Everything except the version is "default", which is a MySQL Community Server */
	public DbmsVersionInfoMySql(long longVersion)
	{
		super(null);
		setLongVersion(longVersion);
	}

	/**
	 * Do the work in here
	 * <p>
	 * Every query is in it's own try/catch, so a missing privilege on one view do not stop us from getting the rest.
	 */
	private void create(DbxConnection conn)
	{
		String sql;

		//---------------------------------------------------
		// Variables that exists in both MySQL and MariaDB
		//---------------------------------------------------
		sql = "select @@version, @@version_comment, @@server_id, @@read_only, @@performance_schema, @@log_bin, @@basedir";
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while(rs.next())
			{
				_versionString   = rs.getString (1);
				_versionComment  = rs.getString (2);
				_serverId        = rs.getLong   (3);
				_isReadOnly      = rs.getBoolean(4);
				_isPerfSchemaOn  = rs.getBoolean(5);
				_isLogBin        = rs.getBoolean(6);
				String basedir   = rs.getString (7);

				String verLower  = (_versionString  == null) ? "" : _versionString .toLowerCase();
				String comLower  = (_versionComment == null) ? "" : _versionComment.toLowerCase();

				_isMariaDb       = verLower.contains("mariadb") || comLower.contains("mariadb");
				_isPercona       = comLower.contains("percona");
				_isEnterprise    = comLower.contains("enterprise");

				// AWS RDS installs MySQL in /rdsdbbin/
				if (basedir != null && basedir.startsWith("/rdsdbbin/"))
					_isAwsManagedDbms = true;
			}
		}
		catch(SQLException ex)
		{
			_logger.warn("Problems getting MySQL server info. ErrorCode=" + ex.getErrorCode() + ", Message='" + ex.getMessage().trim() + "', sql=|" + sql + "|.");
		}

		//---------------------------------------------------
		// MySQL only (MariaDB do not have them)
		//---------------------------------------------------
		if ( ! _isMariaDb )
		{
			sql = "select @@server_uuid, @@super_read_only, @@gtid_mode";
			try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
			{
				while(rs.next())
				{
					_serverUuid      = rs.getString (1);
					_isSuperReadOnly = rs.getBoolean(2);
					_gtidMode        = rs.getString (3);
				}
			}
			catch(SQLException ex)
			{
				_logger.warn("Problems getting MySQL server info. ErrorCode=" + ex.getErrorCode() + ", Message='" + ex.getMessage().trim() + "', sql=|" + sql + "|.");
			}

			// Amazon Aurora MySQL: the variable only exists in Aurora, so an error is the normal case
			sql = "select @@aurora_version";
			try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
			{
				while(rs.next())
				{
					_auroraVersion    = rs.getString(1);
					_isAurora         = true;
					_isAwsManagedDbms = true;
				}
			}
			catch(SQLException ignore)
			{
			}
		}

		//---------------------------------------------------
		// The 'sys' schema
		//---------------------------------------------------
		sql = "select sys_version from sys.version";
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while(rs.next())
			{
				_sysVersion   = rs.getString(1);
				_hasSysSchema = true;
			}
		}
		catch(SQLException ex)
		{
			_logger.info("The 'sys' schema can not be read. CM's that reads it will be DISABLED. ErrorCode=" + ex.getErrorCode() + ", Message='" + ex.getMessage().trim() + "', sql=|" + sql + "|.");
		}

		//---------------------------------------------------
		// Is this a replica (any configured replication channel)
		//---------------------------------------------------
		if ( ! _isMariaDb )
		{
			sql = "select count(*) from performance_schema.replication_connection_configuration";
			try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
			{
				while(rs.next())
					_replicaChannels = rs.getInt(1);
			}
			catch(SQLException ex)
			{
				_logger.info("Problems checking if the server is a replica. ErrorCode=" + ex.getErrorCode() + ", Message='" + ex.getMessage().trim() + "', sql=|" + sql + "|.");
			}
		}

		_logger.info("MySQL server info: " + this);

		if (_isMariaDb)
			_logger.warn("This is a MariaDB server (" + _versionString + "). MySqlTune is made for MySQL 8.0 and above. MariaDB has other views (and performance_schema is OFF by default), so Counter Models that can't read their views will be DISABLED, and some may show less data.");

		if ( ! _isPerfSchemaOn )
			_logger.warn("performance_schema is OFF in this server. Most Counter Models reads it, and they will be DISABLED. Fix: set 'performance_schema=ON' in the server's configuration file (my.cnf) and restart the server.");
	}

	public String  getVersionString()   { return _versionString; }
	public String  getVersionComment()  { return _versionComment; }

	public boolean isMariaDb()          { return _isMariaDb; }
	public boolean isPercona()          { return _isPercona; }
	public boolean isEnterprise()       { return _isEnterprise; }
	public boolean isAurora()           { return _isAurora; }
	public String  getAuroraVersion()   { return _auroraVersion; }

	public long    getServerId()        { return _serverId; }
	public String  getServerUuid()      { return _serverUuid; }
	public boolean isReadOnly()         { return _isReadOnly; }
	public boolean isSuperReadOnly()    { return _isSuperReadOnly; }
	public boolean isPerfSchemaOn()     { return _isPerfSchemaOn; }
	public boolean isLogBin()           { return _isLogBin; }
	public String  getGtidMode()        { return _gtidMode; }

	public boolean hasSysSchema()       { return _hasSysSchema; }
	public String  getSysVersion()      { return _sysVersion; }

	/** Number of configured replication channels (0 if this is not a replica, or if performance_schema could not be read) */
	public int     getReplicaChannels() { return _replicaChannels; }
	public boolean isReplica()          { return _replicaChannels > 0; }

	/** MySQL, MariaDB, Percona Server or Amazon Aurora MySQL */
	public String getFlavor()
	{
		if (_isMariaDb) return "MariaDB";
		if (_isPercona) return "Percona Server";
		if (_isAurora)  return "Amazon Aurora MySQL";
		return _isEnterprise ? "MySQL Enterprise" : "MySQL";
	}

	@Override
	public String toString()
	{
		return "flavor=" + getFlavor()
			+ ", version=" + _versionString
			+ ", versionComment=" + _versionComment
			+ (_isAurora ? ", auroraVersion=" + _auroraVersion : "")
			+ ", isAwsRds=" + _isAwsManagedDbms
			+ ", serverId=" + _serverId
			+ ", serverUuid=" + _serverUuid
			+ ", readOnly=" + _isReadOnly
			+ ", superReadOnly=" + _isSuperReadOnly
			+ ", performanceSchema=" + _isPerfSchemaOn
			+ ", logBin=" + _isLogBin
			+ ", gtidMode=" + _gtidMode
			+ ", sysSchema=" + (_hasSysSchema ? _sysVersion : "NOT-READABLE")
			+ ", replicaChannels=" + _replicaChannels;
	}
}
