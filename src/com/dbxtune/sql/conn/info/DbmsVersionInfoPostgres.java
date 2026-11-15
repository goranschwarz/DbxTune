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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.sql.conn.DbxConnection;

/**
 * What kind of Postgres server we are connected to.
 * <p>
 * NOTE: This is created lazily (on the first getDbmsVersionInfo()), which may be inside a refresh, where AutoCommit is off.<br>
 * A failing statement aborts the whole transaction in Postgres, so ONLY statements that can't fail are used in here
 * (functions that may not exist are looked up in pg_proc before they are called, settings are read from pg_settings).
 */
public class DbmsVersionInfoPostgres
extends DbmsVersionInfo
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private String  _versionString          = null;  // version()   for example: 'PostgreSQL 17.11 (Debian 17.11-1.pgdg13+2) on x86_64-pc-linux-gnu, compiled by gcc...'
	private long    _serverVersionNum       = -1;    // server_version_num, for example: 170011

	private boolean _isAurora               = false; // Amazon Aurora PostgreSQL (has the function aurora_version())
	private String  _auroraVersion          = null;
	private boolean _isAlloyDb              = false; // Google AlloyDB (role 'alloydbsuperuser')
	private boolean _isEdb                  = false; // EDB Postgres Advanced Server (version() contains 'EnterpriseDB')
	private boolean _isYugabyte             = false; // YugabyteDB (version() contains '-YB-')

	private boolean _isInRecovery           = false; // A standby (physical replica)

	private String  _sharedPreloadLibraries = null;  // null = not visible to this login (needs pg_read_all_settings or superuser)
	private String  _trackIoTiming          = null;
	private String  _trackWalIoTiming       = null;  // 14+
	private String  _trackFunctions         = null;
	private String  _computeQueryId         = null;  // 14+
	private String  _trackActivityQuerySize = null;

	private Map<String, String> _extensions = new LinkedHashMap<>(); // installed in the database we are connected to: name -> version
	private List<String>        _roles      = new ArrayList<>();     // roles the login is a member of (also through other roles), and 'superuser'

	public DbmsVersionInfoPostgres(DbxConnection conn)
	{
		super(conn);
		create(conn);
	}

	/** Used when we do not have a connection. Everything except the version is "default", which is a PostgreSQL Community Server */
	public DbmsVersionInfoPostgres(long longVersion)
	{
		super(null);
		setLongVersion(longVersion);
	}

	/**
	 * Do the work in here
	 * <p>
	 * Every query is in it's own try/catch, so one problem do not stop us from getting the rest.
	 */
	private void create(DbxConnection conn)
	{
		String sql;

		//---------------------------------------------------
		// version + standby
		//---------------------------------------------------
		sql = "select version(), pg_is_in_recovery()";
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while(rs.next())
			{
				_versionString = rs.getString (1);
				_isInRecovery  = rs.getBoolean(2);

				String ver = (_versionString == null) ? "" : _versionString;
				_isEdb      = ver.contains("EnterpriseDB");
				_isYugabyte = ver.contains("-YB-");
			}
		}
		catch(SQLException ex)
		{
			_logger.warn("Problems getting Postgres server info. ErrorCode=" + ex.getErrorCode() + ", SqlState=" + ex.getSQLState() + ", Message='" + ex.getMessage().trim() + "', sql=|" + sql + "|.");
		}

		//---------------------------------------------------
		// Settings (a setting that is not visible to the login, or do not exist in this version, is simply not returned)
		//---------------------------------------------------
		sql = "select name, setting from pg_settings where name in ('server_version_num', 'shared_preload_libraries', 'track_io_timing', 'track_wal_io_timing', 'track_functions', 'compute_query_id', 'track_activity_query_size')";
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while(rs.next())
			{
				String name  = rs.getString(1);
				String value = rs.getString(2);

				if      ("server_version_num"       .equals(name)) { try { _serverVersionNum = Long.parseLong(value); } catch (NumberFormatException ignore) {} }
				else if ("shared_preload_libraries" .equals(name)) _sharedPreloadLibraries = value;
				else if ("track_io_timing"          .equals(name)) _trackIoTiming          = value;
				else if ("track_wal_io_timing"      .equals(name)) _trackWalIoTiming       = value;
				else if ("track_functions"          .equals(name)) _trackFunctions         = value;
				else if ("compute_query_id"         .equals(name)) _computeQueryId         = value;
				else if ("track_activity_query_size".equals(name)) _trackActivityQuerySize = value;
			}
		}
		catch(SQLException ex)
		{
			_logger.warn("Problems getting Postgres settings. ErrorCode=" + ex.getErrorCode() + ", SqlState=" + ex.getSQLState() + ", Message='" + ex.getMessage().trim() + "', sql=|" + sql + "|.");
		}

		//---------------------------------------------------
		// Installed extensions (in the database we are connected to)
		//---------------------------------------------------
		sql = "select extname, extversion from pg_extension order by extname";
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while(rs.next())
				_extensions.put(rs.getString(1), rs.getString(2));
		}
		catch(SQLException ex)
		{
			_logger.warn("Problems getting installed Postgres extensions. ErrorCode=" + ex.getErrorCode() + ", SqlState=" + ex.getSQLState() + ", Message='" + ex.getMessage().trim() + "', sql=|" + sql + "|.");
		}

		//---------------------------------------------------
		// Managed services: they have their own "admin" roles
		//---------------------------------------------------
		_isGcpManagedDbms   = false;
		_isAwsManagedDbms   = false;
		_isAzureManagedDbms = false;

		sql = "select rolname from pg_roles where rolname in ('cloudsqlsuperuser', 'alloydbsuperuser', 'rds_superuser', 'rdsadmin', 'azure_pg_admin')";
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while(rs.next())
			{
				String rolename = rs.getString(1);
				if (rolename == null)
					continue;

				if      (rolename.startsWith("cloudsql")) _isGcpManagedDbms   = true;
				else if (rolename.startsWith("alloydb" )) { _isGcpManagedDbms = true; _isAlloyDb = true; }
				else if (rolename.startsWith("rds"     )) _isAwsManagedDbms   = true;
				else if (rolename.startsWith("azure"   )) _isAzureManagedDbms = true;
			}
		}
		catch(SQLException ex)
		{
			_logger.warn("Problems checking if the Postgres Connection is a GCP/AWS/Azure Managed DBMS Instance. ErrorCode=" + ex.getErrorCode() + ", SqlState=" + ex.getSQLState() + ", Message='" + ex.getMessage().trim() + "', sql=|" + sql + "|.");
		}

		// Amazon Aurora PostgreSQL: has the function aurora_version() (looked up first, so we never call a function that do not exist)
		if (_isAwsManagedDbms)
		{
			sql = "select p.proname from pg_proc p where p.proname = 'aurora_version' limit 1";
			try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
			{
				while(rs.next())
					_isAurora = true;
			}
			catch(SQLException ex)
			{
				_logger.info("Problems checking if this is Amazon Aurora PostgreSQL. ErrorCode=" + ex.getErrorCode() + ", Message='" + ex.getMessage().trim() + "', sql=|" + sql + "|.");
			}

			if (_isAurora)
			{
				sql = "select aurora_version()";
				try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
				{
					while(rs.next())
						_auroraVersion = rs.getString(1);
				}
				catch(SQLException ex)
				{
					_logger.info("Problems getting the Aurora version. ErrorCode=" + ex.getErrorCode() + ", Message='" + ex.getMessage().trim() + "', sql=|" + sql + "|.");
				}
			}
		}

		//---------------------------------------------------
		// Roles the login has (also through other roles)
		//---------------------------------------------------
		List<String> roles = conn.getActiveServerRolesOrPermissions();
		if (roles != null)
			_roles.addAll(roles);

		// Every connection creates one of these (the CM's that loop over all databases have their own connections), so only log it when the server changes
		// NOTE: the extensions are per database, so they are not part of the "is it new" key
		String logKey = getFlavor() + "|" + _versionString + "|" + _isInRecovery + "|" + _roles;
		if ( ! logKey.equals(_lastLoggedKey) )
		{
			_lastLoggedKey = logKey;
			_logger.info("Postgres server info: " + this);
		}
	}
	private static String _lastLoggedKey = null;

	public String  getVersionString()          { return _versionString; }
	public long    getServerVersionNum()       { return _serverVersionNum; }

	public boolean isAurora()                  { return _isAurora; }
	public String  getAuroraVersion()          { return _auroraVersion; }
	public boolean isAlloyDb()                 { return _isAlloyDb; }
	public boolean isEdb()                     { return _isEdb; }
	public boolean isYugabyte()                { return _isYugabyte; }

	/** true if this is a standby (physical replica) */
	public boolean isInRecovery()              { return _isInRecovery; }

	/** null if the setting is not visible to this login (it needs 'pg_read_all_settings' or superuser) */
	public String  getSharedPreloadLibraries() { return _sharedPreloadLibraries; }
	public String  getTrackIoTiming()          { return _trackIoTiming; }
	public String  getTrackWalIoTiming()       { return _trackWalIoTiming; }
	public String  getTrackFunctions()         { return _trackFunctions; }
	public String  getComputeQueryId()         { return _computeQueryId; }
	public String  getTrackActivityQuerySize() { return _trackActivityQuerySize; }

	/** Extensions installed in the database we are connected to: name -> version */
	public Map<String, String> getExtensions() { return _extensions; }
	public boolean hasExtension(String name)   { return _extensions.containsKey(name); }

	/** Roles the login is a member of (also through other roles). 'superuser' if the login is a superuser */
	public List<String> getRoles()             { return _roles; }
	public boolean hasRole(String name)        { return _roles.contains(name); }
	public boolean isSuperuser()               { return _roles.contains("superuser"); }

	/** Can the login see everything the CM's needs? (superuser, pg_monitor, or both pg_read_all_stats and pg_read_all_settings) */
	public boolean hasMonitorRole()
	{
		return isSuperuser() || hasRole("pg_monitor") || (hasRole("pg_read_all_stats") && hasRole("pg_read_all_settings"));
	}

	/** PostgreSQL, or the name of a fork / managed service */
	public String getFlavor()
	{
		if (_isAurora)           return "Amazon Aurora PostgreSQL";
		if (_isAwsManagedDbms)   return "Amazon RDS for PostgreSQL";
		if (_isAzureManagedDbms) return "Azure Database for PostgreSQL";
		if (_isAlloyDb)          return "Google AlloyDB";
		if (_isGcpManagedDbms)   return "Google Cloud SQL for PostgreSQL";
		if (_isYugabyte)         return "YugabyteDB";
		if (_isEdb)              return "EDB Postgres Advanced Server";
		return "PostgreSQL";
	}

	@Override
	public String toString()
	{
		return "flavor=" + getFlavor()
			+ ", version=" + _versionString
			+ ", serverVersionNum=" + _serverVersionNum
			+ (_isAurora ? ", auroraVersion=" + _auroraVersion : "")
			+ ", inRecovery=" + _isInRecovery
			+ ", sharedPreloadLibraries=" + (_sharedPreloadLibraries == null ? "NOT-VISIBLE" : "'" + _sharedPreloadLibraries + "'")
			+ ", trackIoTiming=" + _trackIoTiming
			+ ", trackWalIoTiming=" + _trackWalIoTiming
			+ ", trackFunctions=" + _trackFunctions
			+ ", computeQueryId=" + _computeQueryId
			+ ", trackActivityQuerySize=" + _trackActivityQuerySize
			+ ", extensions=" + _extensions
			+ ", roles=" + _roles;
	}
}
