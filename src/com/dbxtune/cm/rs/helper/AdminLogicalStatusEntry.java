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

import java.sql.Timestamp;

import com.dbxtune.utils.StringUtil;

public class AdminLogicalStatusEntry
{
	
	public AdminLogicalStatusEntry(
		String logicalConnectionName,
		String activeConnectionName,
		String activeConnState,
		String standbyConnectionName,
		String standbyConnState,
		String controllerRS,
		String operationInProgress,
		String stateOfOperationInProgress,
		String spid)
	{
		_logicalConnNameFull  = logicalConnectionName;
		_logicalConnId        = getId       (logicalConnectionName);
		_logicalConnName      = getSrvDbName(logicalConnectionName);
		_logicalConnSrvName   = getSrvName  (logicalConnectionName);
		_logicalConnDbName    = getDbName   (logicalConnectionName);
		
		_activeConnNameFull   = activeConnectionName;
		_activeConnState      = activeConnState;
		_activeConnId         = getId       (activeConnectionName);
		_activeConnName       = getSrvDbName(activeConnectionName);
		_activeConnSrvName    = getSrvName  (activeConnectionName);
		_activeConnDbName     = getDbName   (activeConnectionName);
		
		_standbyConnNameFull  = standbyConnectionName;
		_standbyConnState     = standbyConnState;
		_standbyConnId        = getId       (standbyConnectionName);
		_standbyConnName      = getSrvDbName(standbyConnectionName);
		_standbyConnSrvName   = getSrvName  (standbyConnectionName);
		_standbyConnDbName    = getDbName   (standbyConnectionName);
		
		_rsName               = controllerRS;
		_rsId                 = getId     (controllerRS);
		_rsSrvName            = getSrvName(controllerRS);
		
		_opInProgress         = operationInProgress;
		_stateOfOpInProgress  = stateOfOperationInProgress;
		_spid                 =  spid;
	}
	
	private String _logicalConnNameFull = "";
	private int    _logicalConnId       = 0;
	private String _logicalConnName     = "";
	private String _logicalConnSrvName  = "";
	private String _logicalConnDbName   = "";

	private String _activeConnNameFull  = "";
	private String _activeConnState     = "";
	private int    _activeConnId        = 0;
	private String _activeConnName      = "";
	private String _activeConnSrvName   = "";
	private String _activeConnDbName    = "";
	private Timestamp _activeTimestamp = null; // set using 'select getdate()' if we do update the active side.

	private String _standbyConnNameFull = "";
	private String _standbyConnState    = "";
	private int    _standbyConnId       = 0;
	private String _standbyConnName     = "";
	private String _standbyConnSrvName  = "";
	private String _standbyConnDbName   = "";

	private String _rsName              = "";
	private int    _rsId                = 0;
	private String _rsSrvName           = "";

	private String _opInProgress        = "";
	private String _stateOfOpInProgress = "";
	private String _spid                = "";


	/** get id, from the string "[id] srv.db" */
	private static int getId(String str)
	{
		if (StringUtil.isNullOrBlank(str))
			return 0;
		
		int id = 0;
		int startPos = str.indexOf("[");
		int endPos   = str.indexOf("] ");
		
		if (startPos != -1 && endPos != -1)
		{
			String idStr = str.substring(startPos+1, endPos);
			id = StringUtil.parseInt(idStr, 0);
		}
		return id;
	}

	/** get srv.db, from the string "[id] srv.db" */
	private static String getSrvDbName(String str)
	{
		if (StringUtil.isNullOrBlank(str))
			return "";
		
		String name = "";
		int startPos = str.indexOf("] ");
		if (startPos != -1)
			name = str.substring(startPos+2);

		return name;
	}

	/** get srv, from the string "[id] srv.db" */
	private static String getSrvName(String str)
	{
		String name = getSrvDbName(str);
		
		int dotPos = name.indexOf(".");
		if (dotPos != -1)
			name = name.substring(0, dotPos);

		return name;
	}
	/** get db, from the string "[id] srv.db" */
	private static String getDbName(String str)
	{
		String name = getSrvDbName(str);
		
		int dotPos = name.indexOf(".");
		if (dotPos != -1)
			name = name.substring(dotPos+1);

		return name;
	}
	
	public String getLogicalConnName     () { return _logicalConnName    ; }
	public int    getLogicalConnId       () { return _logicalConnId      ; }
	public String getLogicalConnSrvName  () { return _logicalConnSrvName ; }
	public String getLogicalConnDbName   () { return _logicalConnDbName  ; }

	public String getActiveConnName      () { return _activeConnName     ; }
	public String getActiveConnState     () { return _activeConnState    ; }
	public int    getActiveConnId        () { return _activeConnId       ; }
	public String getActiveConnSrvName   () { return _activeConnSrvName  ; }
	public String getActiveConnDbName    () { return _activeConnDbName   ; }

	public void      setActiveTimestamp(Timestamp timestamp) { _activeTimestamp = timestamp; }
	public Timestamp getActiveTimestamp()                    { return _activeTimestamp; }

	public String getStandbyConnName     () { return _standbyConnName    ; }
	public String getStandbyConnState    () { return _standbyConnState   ; }
	public int    getStandbyConnId       () { return _standbyConnId      ; }
	public String getStandbyConnSrvName  () { return _standbyConnSrvName ; }
	public String getStandbyConnDbName   () { return _standbyConnDbName  ; }

//	public String getRsName              () { return _rsName             ; }
	public int    getRsId                () { return _rsId               ; }
	public String getRsSrvName           () { return _rsSrvName          ; }

	public String getOpInProgress        () { return _opInProgress       ; }
	public String getStateOfOpInProgress () { return _stateOfOpInProgress; }
	public String getSpid                () { return _spid               ; }
	
	
	@Override
	public String toString()
	{
		return super.toString() + " (" +
			"logicalConnId"       + "="  + _logicalConnId       + ", " +
			"logicalConnName"     + "='" + _logicalConnName     + "', " +
			"logicalConnSrvName"  + "='" + _logicalConnSrvName  + "', " +
			"logicalConnDbName"   + "='" + _logicalConnDbName   + "', " +

			"activeConnId"        + "="  + _activeConnId        + ", " +
			"activeConnName"      + "='" + _activeConnName      + "', " +
			"activeConnSrvName"   + "='" + _activeConnSrvName   + "', " +
			"activeConnDbName"    + "='" + _activeConnDbName    + "', " +
			"activeConnState"     + "='" + _activeConnState     + "', " +

			"standbyConnId"       + "="  + _standbyConnId       + ", " +
			"standbyConnName"     + "='" + _standbyConnName     + "', " +
			"standbyConnSrvName"  + "='" + _standbyConnSrvName  + "', " +
			"standbyConnDbName"   + "='" + _standbyConnDbName   + "', " +
			"standbyConnState"    + "='" + _standbyConnState    + "', " +

			"rsName"              + "='" + _rsName              + "', " +
			"rsId"                + "="  + _rsId                + ", " +
			"rsSrvName"           + "='" + _rsSrvName           + "', " +

			"opInProgress"        + "='" + _opInProgress        + "', " +
			"stateOfOpInProgress" + "='" + _stateOfOpInProgress + "', " +
			"spid"                + "='" + _spid                + "')";
	}

	public static void main(String[] args)
	{
		AdminLogicalStatusEntry x = new AdminLogicalStatusEntry("[180] LDS1.b2b", "[186] PROD_A1_ASE.b2b", "Active/", "[197] PROD_B1_ASE.b2b", "Active/", "[16777317] PROD_REP", "None", "None", "");
		System.out.println("x="+x);
	}
}
