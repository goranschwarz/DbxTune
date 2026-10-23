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

public class DbmsVersionInfoDb2
extends DbmsVersionInfo
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private String  _instName           = null;  // db2inst1
	private String  _serviceLevel       = null;  // DB2 v11.5.8.0
	private int     _fixpackNum         = -1;
	private int     _numMembers         = 1;     // > 1 = pureScale
	private int     _numDbPartitions    = 1;     // > 1 = DPF (partitioned database)
	private String  _edition            = null;  // INSTALLED_PROD_FULLNAME: DB2_DEVELOPER_C_EDITION, DB2_STANDARD_EDITION, DB2_ADVANCED_ENTERPRISE_SERVER_EDITION...
	private String  _licenseType        = null;  // COMMUNITY, TRIAL, LICENSED...
	private String  _dbName             = null;  // CURRENT SERVER
	private String  _sessionUser        = null;  // SESSION_USER (the login)
	private String  _hostName           = null;
	private String  _osName             = null;
	private String  _activationState    = null;  // EXPLICIT (ACTIVATE DATABASE) or NONE (activated by the first connection, and deactivated by the last; tested 11.5.8)
	private String  _logArchMeth1       = null;  // OFF = circular logging (if also LOGARCHMETH2 is OFF)
	private String  _logArchMeth2       = null;
	private String  _hadrRole           = null;  // STANDARD, PRIMARY, STANDBY
	private String  _dftTableOrg        = null;  // ROW or COLUMN (BLU)
	private String  _monReqMetrics      = null;  // NONE, BASE, EXTENDED
	private String  _monActMetrics      = null;
	private String  _monObjMetrics      = null;
	private String  _compatVector       = null;  // DB2_COMPATIBILITY_VECTOR (ORA = Oracle compatibility)
	private String  _explainSchema      = null;  // The schema where the explain tables are (the login's own schema, or SYSTOOLS), null if not found

	public DbmsVersionInfoDb2(DbxConnection conn)
	{
		super(conn);
		create(conn);
	}

	/** Used when we do not have a connection (and by the "fall back to the base SQL" logic in the CM's). Everything except the version is "default" */
	public DbmsVersionInfoDb2(long longVersion)
	{
		super(null);
		setLongVersion(longVersion);
	}

	/**
	 * Do the work in here
	 * <p>
	 * Every query is in it's own try/catch, so a missing privilege on one function do not stop us from getting the rest.<br>
	 * The SYSIBMADM views used here are readable by PUBLIC (tested on 11.5.8), MON_GET_DATABASE needs EXECUTE (see DbmsSetup/db2.txt)
	 */
	private void create(DbxConnection conn)
	{
		String sql;

		//---------------------------------------------------
		// Instance and level
		//---------------------------------------------------
		sql = "select INST_NAME, SERVICE_LEVEL, FIXPACK_NUM, NUM_MEMBERS, NUM_DBPARTITIONS from SYSIBMADM.ENV_INST_INFO";
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while(rs.next())
			{
				_instName        = trim(rs.getString(1));
				_serviceLevel    = trim(rs.getString(2));
				_fixpackNum      = rs.getInt(3);
				_numMembers      = rs.getInt(4);
				_numDbPartitions = rs.getInt(5);
			}
		}
		catch(SQLException ex)
		{
			_logger.warn("Problems getting Db2 instance info. ErrorCode=" + ex.getErrorCode() + ", Message='" + ex.getMessage().trim() + "', sql=|" + sql + "|.");
		}

		//---------------------------------------------------
		// Edition (the installed license)
		//---------------------------------------------------
		sql = "select INSTALLED_PROD_FULLNAME, LICENSE_TYPE from SYSIBMADM.ENV_PROD_INFO where LICENSE_INSTALLED = 'Y'";
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while(rs.next())
			{
				_edition     = trim(rs.getString(1));
				_licenseType = trim(rs.getString(2));
			}
		}
		catch(SQLException ex)
		{
			_logger.info("Problems getting Db2 edition. ErrorCode=" + ex.getErrorCode() + ", Message='" + ex.getMessage().trim() + "', sql=|" + sql + "|.");
		}

		//---------------------------------------------------
		// Database, login, host
		//---------------------------------------------------
		sql = "select CURRENT SERVER, SESSION_USER, (select HOST_NAME from SYSIBMADM.ENV_SYS_INFO), (select OS_NAME from SYSIBMADM.ENV_SYS_INFO) from SYSIBM.SYSDUMMY1";
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while(rs.next())
			{
				_dbName      = trim(rs.getString(1));
				_sessionUser = trim(rs.getString(2));
				_hostName    = trim(rs.getString(3));
				_osName      = trim(rs.getString(4));
			}
		}
		catch(SQLException ex)
		{
			_logger.warn("Problems getting Db2 database info. ErrorCode=" + ex.getErrorCode() + ", Message='" + ex.getMessage().trim() + "', sql=|" + sql + "|.");
		}

		//---------------------------------------------------
		// Database configuration (one row per member, take the first)
		//---------------------------------------------------
		sql = "select NAME, VALUE from SYSIBMADM.DBCFG where NAME in ('logarchmeth1', 'logarchmeth2', 'hadr_db_role', 'dft_table_org', 'mon_req_metrics', 'mon_act_metrics', 'mon_obj_metrics') order by MEMBER desc";
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while(rs.next())
			{
				String name  = trim(rs.getString(1));
				String value = trim(rs.getString(2));

				// ordered by MEMBER desc, so the value of the lowest member is the one that is kept
				if      ("logarchmeth1"   .equals(name)) _logArchMeth1  = value;
				else if ("logarchmeth2"   .equals(name)) _logArchMeth2  = value;
				else if ("hadr_db_role"   .equals(name)) _hadrRole      = value;
				else if ("dft_table_org"  .equals(name)) _dftTableOrg   = value;
				else if ("mon_req_metrics".equals(name)) _monReqMetrics = value;
				else if ("mon_act_metrics".equals(name)) _monActMetrics = value;
				else if ("mon_obj_metrics".equals(name)) _monObjMetrics = value;
			}
		}
		catch(SQLException ex)
		{
			_logger.warn("Problems getting Db2 database configuration. ErrorCode=" + ex.getErrorCode() + ", Message='" + ex.getMessage().trim() + "', sql=|" + sql + "|.");
		}

		//---------------------------------------------------
		// Registry (db2set)
		//---------------------------------------------------
		sql = "select REG_VAR_VALUE from SYSIBMADM.REG_VARIABLES where REG_VAR_NAME = 'DB2_COMPATIBILITY_VECTOR'";
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while(rs.next())
				_compatVector = trim(rs.getString(1));
		}
		catch(SQLException ex)
		{
			_logger.info("Problems getting Db2 registry variables. ErrorCode=" + ex.getErrorCode() + ", Message='" + ex.getMessage().trim() + "', sql=|" + sql + "|.");
		}

		//---------------------------------------------------
		// Activation state (needs EXECUTE on MON_GET_DATABASE)
		//---------------------------------------------------
		sql = "select DB_ACTIVATION_STATE from table(MON_GET_DATABASE(-1)) t";
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while(rs.next())
				_activationState = trim(rs.getString(1));
		}
		catch(SQLException ex)
		{
			_logger.info("Problems getting Db2 activation state. ErrorCode=" + ex.getErrorCode() + ", Message='" + ex.getMessage().trim() + "', sql=|" + sql + "|.");
		}

		//---------------------------------------------------
		// Explain tables: EXPLAIN_FROM_SECTION looks in the login's own schema first, then in SYSTOOLS
		// DbxTune never creates them (see DbmsSetup/db2.txt), so plans of running statements are only captured if the DBA created them
		//---------------------------------------------------
		sql = "select TABSCHEMA, count(*) from SYSCAT.TABLES "
			+ "where TABNAME in ('EXPLAIN_INSTANCE', 'EXPLAIN_STATEMENT', 'EXPLAIN_OPERATOR', 'EXPLAIN_STREAM', 'EXPLAIN_ARGUMENT', 'EXPLAIN_OBJECT', 'EXPLAIN_PREDICATE') "
			+ "  and TABSCHEMA in (SESSION_USER, 'SYSTOOLS') "
			+ "group by TABSCHEMA "
			+ "having count(*) = 7 "
			+ "order by case when TABSCHEMA = 'SYSTOOLS' then 2 else 1 end";
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while(rs.next())
			{
				if (_explainSchema == null)
					_explainSchema = trim(rs.getString(1));
			}
		}
		catch(SQLException ex)
		{
			_logger.info("Problems checking for Db2 explain tables. ErrorCode=" + ex.getErrorCode() + ", Message='" + ex.getMessage().trim() + "', sql=|" + sql + "|.");
		}

		_logger.info("Db2 server info: " + this);
	}

	private static String trim(String str)
	{
		return str == null ? null : str.trim();
	}

	public String  getInstanceName()      { return _instName; }
	public String  getServiceLevel()      { return _serviceLevel; }
	public int     getFixpackNum()        { return _fixpackNum; }
	public int     getNumMembers()        { return _numMembers; }
	public int     getNumDbPartitions()   { return _numDbPartitions; }
	public String  getEdition()           { return _edition; }
	public String  getLicenseType()       { return _licenseType; }
	public String  getDbName()            { return _dbName; }
	public String  getSessionUser()       { return _sessionUser; }
	public String  getHostName()          { return _hostName; }
	public String  getOsName()            { return _osName; }
	public String  getActivationState()   { return _activationState; }
	public String  getLogArchMeth1()      { return _logArchMeth1; }
	public String  getLogArchMeth2()      { return _logArchMeth2; }
	public String  getHadrRole()          { return _hadrRole; }
	public String  getDftTableOrg()       { return _dftTableOrg; }
	public String  getMonReqMetrics()     { return _monReqMetrics; }
	public String  getMonActMetrics()     { return _monActMetrics; }
	public String  getMonObjMetrics()     { return _monObjMetrics; }
	public String  getCompatVector()      { return _compatVector; }
	public String  getExplainSchema()     { return _explainSchema; }

	/** pureScale (more than one member) */
	public boolean isPureScale()          { return _numMembers > 1; }
	/** DPF: a partitioned database */
	public boolean isDpf()                { return _numDbPartitions > 1; }
	/** HADR primary or standby */
	public boolean isHadr()               { return _hadrRole != null && ! "STANDARD".equalsIgnoreCase(_hadrRole); }
	/** Explicitly activated (ACTIVATE DATABASE). If not, the database is deactivated, and all monitor counters and the package cache are lost, when the last connection leaves */
	public boolean isExplicitlyActivated(){ return "EXPLICIT".equalsIgnoreCase(_activationState); }
	/** Circular logging: no archive logs, so no online backup and no roll-forward recovery */
	public boolean isCircularLogging()    { return "OFF".equalsIgnoreCase(_logArchMeth1) && (_logArchMeth2 == null || "OFF".equalsIgnoreCase(_logArchMeth2)); }
	/** The explain tables exists (in the login's own schema, or in SYSTOOLS) */
	public boolean hasExplainTables()     { return _explainSchema != null; }

	@Override
	public String toString()
	{
		return "level=" + _serviceLevel
			+ ", fixpack=" + _fixpackNum
			+ ", edition=" + _edition
			+ ", licenseType=" + _licenseType
			+ ", instance=" + _instName
			+ ", database=" + _dbName
			+ ", host=" + _hostName
			+ ", os=" + _osName
			+ ", login=" + _sessionUser
			+ ", members=" + _numMembers
			+ ", dbPartitions=" + _numDbPartitions
			+ ", activation=" + _activationState
			+ ", logarchmeth1=" + _logArchMeth1
			+ ", logarchmeth2=" + _logArchMeth2
			+ ", hadrRole=" + _hadrRole
			+ ", dftTableOrg=" + _dftTableOrg
			+ ", monReqMetrics=" + _monReqMetrics
			+ ", monActMetrics=" + _monActMetrics
			+ ", monObjMetrics=" + _monObjMetrics
			+ ", compatibilityVector=" + _compatVector
			+ ", explainTables=" + (_explainSchema == null ? "NOT-FOUND" : _explainSchema);
	}
}
