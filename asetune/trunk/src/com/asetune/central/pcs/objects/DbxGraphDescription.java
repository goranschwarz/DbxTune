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
package com.asetune.central.pcs.objects;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder(value = {"serverNameList", "dbxProduct", "cmName", "graphName", "tableName", "graphLabel", "graphCategory", "isPercentGraph", "visibleAtStartup", "initialOrder"}, alphabetic = true)
public class DbxGraphDescription
{
	private List<String> _serverNameList;
	private String    _dbxProduct      ;
	private String    _cmName          ;
	private String    _graphName       ;
	private String    _tableName       ;
	private String    _graphLabel      ;
	private String    _graphCategory   ;
	private boolean   _isPercentGraph  ;
	private boolean   _visibleAtStartup;
	private int       _initialOrder    ;

//	public String       getRecid()            { return UUID.randomUUID().toString();  }
//	public Timestamp    getSessionStartTime() { return _sessionStartTime;  }
//	public String       getServerName      () { return _serverName      ;  }
	public List<String> getServerNameList  () { return _serverNameList  ;  }
	public String       getDbxProduct      () { return _dbxProduct      ;  }
	public String       getCmName          () { return _cmName          ;  }
	public String       getGraphName       () { return _graphName       ;  }
	public String       getTableName       () { return _tableName       ;  }
	public String       getGraphLabel      () { return _graphLabel      ;  }
	public String       getGraphCategory   () { return _graphCategory   ;  }
	public boolean      isPercentGraph     () { return _isPercentGraph  ;  }
	public boolean      isVisibleAtStartup () { return _visibleAtStartup;  }
	public int          getInitialOrder    () { return _initialOrder    ;  }

//	public void setSessionStartTime(Timestamp sessionStartTime ) { _sessionStartTime  = sessionStartTime; }
//	public void setServerName      (String     serverName      ) { _serverName        = serverName      ; }
	public void setServerName      (List<String> serverNameList) { _serverNameList    = serverNameList  ; }
	public void setDbxProduct      (String     dbxProduct      ) { _dbxProduct        = dbxProduct      ; }
	public void setCmName          (String     cmName          ) { _cmName            = cmName          ; }
	public void setGraphName       (String     graphName       ) { _graphName         = graphName       ; }
	public void setTableName       (String     tableName       ) { _tableName         = tableName       ; }
	public void setGraphLabel      (String     graphLabel      ) { _graphLabel        = graphLabel      ; }
	public void setGraphCategory   (String     graphCategory   ) { _graphCategory     = graphCategory   ; }
	public void setIsPercentGraph  (boolean    isPercentGraph  ) { _isPercentGraph    = isPercentGraph  ; }
	public void setVisibleAtStartup(boolean    visibleAtStartup) { _visibleAtStartup  = visibleAtStartup; }
	public void setInitialOrder    (int        initialOrder    ) { _initialOrder      = initialOrder    ; }

	/** Add a serverName to the server list (if it's not already exists) */ 
	public void addServerName(String serverName) 
	{
		if (_serverNameList == null)
			_serverNameList = new ArrayList<>();

		if ( ! _serverNameList.contains(serverName) )
			_serverNameList.add(serverName); 
	}

//	public DbxGraphDescription(Timestamp sessionStartTime, String serverName, String cmName, String graphName, String tableName, String graphLabel, String graphCategory, boolean isPercentGraph, boolean visibleAtStartup, int initialOrder)
	public DbxGraphDescription(List<String> serverNameList, String dbxProduct, String cmName, String graphName, String tableName, String graphLabel, String graphCategory, boolean isPercentGraph, boolean visibleAtStartup, int initialOrder)
	{
		super();

//		_sessionStartTime  = sessionStartTime;
//		_serverName        = serverName      ;
		_serverNameList    = serverNameList  ;
		_dbxProduct        = dbxProduct  ;
		_cmName            = cmName          ;
		_graphName         = graphName       ;
		_tableName         = tableName       ;
		_graphLabel        = graphLabel      ;
		_graphCategory     = graphCategory   ;
		_isPercentGraph    = isPercentGraph  ;
		_visibleAtStartup  = visibleAtStartup;
		_initialOrder      = initialOrder    ;
	}
	public DbxGraphDescription(DbxGraphDescription gd)
	{
//		this._sessionStartTime  = gd._sessionStartTime;
//		this._serverName        = gd._serverName      ;
		this._serverNameList    = gd._serverNameList == null ? null : new ArrayList<>(_serverNameList);
		this._dbxProduct        = gd._dbxProduct      ;
		this._cmName            = gd._cmName          ;
		this._graphName         = gd._graphName       ;
		this._tableName         = gd._tableName       ;
		this._graphLabel        = gd._graphLabel      ;
		this._graphCategory     = gd._graphCategory   ;
		this._isPercentGraph    = gd._isPercentGraph  ;
		this._visibleAtStartup  = gd._visibleAtStartup;
		this._initialOrder      = gd._initialOrder    ;
	}
	
	@Override
	public String toString()
	{
		// TODO Auto-generated method stub
		return super.toString() + ":"
				+   "serverNameList"   + "="  + _serverNameList   + ""
				+ ", dbxProduct"       + "='" + _dbxProduct       + "'"
				+ ", cmName"           + "='" + _cmName           + "'"
				+ ", graphName"        + "='" + _graphName        + "'"
				+ ", tableName"        + "='" + _tableName        + "'"
				+ ", graphLabel"       + "='" + _graphLabel       + "'"
				+ ", graphCategory"    + "='" + _graphCategory    + "'"
				+ ", isPercentGraph"   + "="  + _isPercentGraph   + ""
				+ ", visibleAtStartup" + "="  + _visibleAtStartup + ""
				+ ", initialOrder"     + "="  + _initialOrder     + ""
				+ ".";
	}
}
