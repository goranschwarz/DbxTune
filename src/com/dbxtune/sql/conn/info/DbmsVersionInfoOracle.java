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
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.sql.conn.DbxConnection;

public class DbmsVersionInfoOracle
extends DbmsVersionInfo
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private boolean _isRac             = false;
	private String  _edition           = null;  // EE, SE, SE2, XE... null if unknown
	private boolean _isArchiveLogMode  = false;
	private String  _databaseRole      = null;  // PRIMARY, PHYSICAL STANDBY, LOGICAL STANDBY, SNAPSHOT STANDBY
	private String  _openMode          = null;  // READ WRITE, READ ONLY, MOUNTED...

	// Multitenant (12.1 and above), a non-CDB has the defaults below
	private boolean _isCdb             = false;
	private int     _conId             = 0;     // 0=non-CDB, 1=CDB$ROOT, 2=PDB$SEED, 3 and above is a PDB
	private String  _conName           = null;
	private int     _visibleContainers = -1;    // Only set when we are connected to CDB$ROOT

	public DbmsVersionInfoOracle(DbxConnection conn)
	{
		super(conn);
		create(conn);
	}

	/** Used when we do not have a connection (and by the "fall back to the 11.2 SQL" logic in the CM's). Everything except the version is "default", which is a non-CDB */
	public DbmsVersionInfoOracle(long longVersion)
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
		// V$DATABASE: archive log mode, role, open mode... and if it's a CDB (column 'CDB' exists in 12.1 and above)
		//---------------------------------------------------
		sql = "select * from V$DATABASE";
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			boolean hasCdbCol = hasColumn(rs.getMetaData(), "CDB");

			while(rs.next())
			{
				_isArchiveLogMode = "ARCHIVELOG".equalsIgnoreCase(rs.getString("LOG_MODE"));
				_databaseRole     = rs.getString("DATABASE_ROLE");
				_openMode         = rs.getString("OPEN_MODE");

				if (hasCdbCol)
					_isCdb = "YES".equalsIgnoreCase(rs.getString("CDB"));
			}
		}
		catch(SQLException ex)
		{
			_logger.warn("Problems getting Oracle database info. ErrorCode=" + ex.getErrorCode() + ", Message='" + ex.getMessage().trim() + "', sql=|" + sql + "|.");
		}

		//---------------------------------------------------
		// V$INSTANCE: RAC... and Edition (column 'EDITION' exists in 12.2 and above)
		//---------------------------------------------------
		sql = "select * from V$INSTANCE";
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			boolean hasEditionCol = hasColumn(rs.getMetaData(), "EDITION");

			while(rs.next())
			{
				_isRac = "YES".equalsIgnoreCase(rs.getString("PARALLEL"));

				if (hasEditionCol)
					_edition = rs.getString("EDITION");
			}
		}
		catch(SQLException ex)
		{
			_logger.warn("Problems getting Oracle instance info. ErrorCode=" + ex.getErrorCode() + ", Message='" + ex.getMessage().trim() + "', sql=|" + sql + "|.");
		}

		//---------------------------------------------------
		// Edition for versions that do not have V$INSTANCE.EDITION -- get it from the banner
		//   EE: Oracle Database 11g Enterprise Edition Release 11.2.0.4.0 - 64bit Production
		//   XE: Oracle Database 11g Express Edition Release 11.2.0.2.0 - 64bit Production
		//   SE: Oracle Database 11g Release 11.2.0.4.0 - 64bit Production
		//---------------------------------------------------
		if (_edition == null)
		{
			sql = "select BANNER from V$VERSION where BANNER like 'Oracle Database%'";
			try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
			{
				while(rs.next())
				{
					String banner = rs.getString(1);

					if      (banner.contains("Enterprise Edition")) _edition = "EE";
					else if (banner.contains("Express Edition"))    _edition = "XE";
					else                                            _edition = "SE";
				}
			}
			catch(SQLException ex)
			{
				_logger.warn("Problems getting Oracle edition. ErrorCode=" + ex.getErrorCode() + ", Message='" + ex.getMessage().trim() + "', sql=|" + sql + "|.");
			}
		}

		//---------------------------------------------------
		// CDB: which container are we connected to
		//---------------------------------------------------
		if (_isCdb)
		{
			sql = "select SYS_CONTEXT('USERENV','CON_ID'), SYS_CONTEXT('USERENV','CON_NAME') from DUAL";
			try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
			{
				while(rs.next())
				{
					_conId   = rs.getInt   (1);
					_conName = rs.getString(2);
				}
			}
			catch(SQLException ex)
			{
				_logger.warn("Problems getting Oracle container info. ErrorCode=" + ex.getErrorCode() + ", Message='" + ex.getMessage().trim() + "', sql=|" + sql + "|.");
			}
		}

		//---------------------------------------------------
		// CDB$ROOT: can we see the other containers?
		// A common user that do NOT have 'CONTAINER_DATA=ALL' will only see CDB$ROOT, and the CM's will "silently" only show data for the root.
		//---------------------------------------------------
		if (isCdbRoot())
		{
			sql = "select count(*) from V$CONTAINERS";
			try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
			{
				while(rs.next())
					_visibleContainers = rs.getInt(1);
			}
			catch(SQLException ex)
			{
				_logger.warn("Problems getting Oracle visible containers. ErrorCode=" + ex.getErrorCode() + ", Message='" + ex.getMessage().trim() + "', sql=|" + sql + "|.");
			}

			if (_visibleContainers == 1)
				_logger.warn("Connected to CDB$ROOT, but only 1 container is visible. Counters for the PDB's will NOT be collected. Fix: ALTER USER <user> SET CONTAINER_DATA=ALL CONTAINER=CURRENT;");
		}

		_logger.info("Oracle instance info: " + this);
	}

	private static boolean hasColumn(ResultSetMetaData rsmd, String colName)
	throws SQLException
	{
		for (int c = 1; c <= rsmd.getColumnCount(); c++)
		{
			if (colName.equalsIgnoreCase(rsmd.getColumnLabel(c)))
				return true;
		}
		return false;
	}

	public void    setRac(boolean b)      { _isRac = b; }
	public boolean  isRac()               { return _isRac; }

	/** EE, SE, SE2, XE... or null if we could not figure it out */
	public String  getEdition()           { return _edition; }

	public boolean  isArchiveLogMode()    { return _isArchiveLogMode; }
	public String  getDatabaseRole()      { return _databaseRole; }
	public String  getOpenMode()          { return _openMode; }

	/** Multitenant Container Database (12.1 and above) */
	public boolean  isCdb()               { return _isCdb; }

	/** Connected to CDB$ROOT: here we can see ALL containers (if the user has CONTAINER_DATA=ALL) */
	public boolean  isCdbRoot()           { return _isCdb && _conId == 1; }

	/** Connected to a Pluggable Database: here we only see "our own" data, so it behaves like a non-CDB */
	public boolean  isPdb()               { return _isCdb && _conId > 2; }

	public int     getConId()             { return _conId; }
	public String  getConName()           { return _conName; }
	public int     getVisibleContainers() { return _visibleContainers; }

	@Override
	public String toString()
	{
		return "edition=" + _edition
			+ ", isRac=" + _isRac
			+ ", isArchiveLogMode=" + _isArchiveLogMode
			+ ", databaseRole=" + _databaseRole
			+ ", openMode=" + _openMode
			+ ", isCdb=" + _isCdb
			+ ", conId=" + _conId
			+ ", conName=" + _conName
			+ ", visibleContainers=" + _visibleContainers;
	}
}
