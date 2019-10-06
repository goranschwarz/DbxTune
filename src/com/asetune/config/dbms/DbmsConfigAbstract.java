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
package com.asetune.config.dbms;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.table.AbstractTableModel;

import org.apache.log4j.Logger;

import com.asetune.pcs.MonRecordingInfo;
import com.asetune.sql.conn.ConnectionProp;
import com.asetune.sql.conn.DbxConnection;

public abstract class DbmsConfigAbstract
extends AbstractTableModel 
implements IDbmsConfig
{
	private static final long serialVersionUID = 1L;

	/** Log4j logging. */
	private static Logger _logger = Logger.getLogger(DbmsConfigAbstract.class);

	private List<DbmsConfigIssue> _configIssueList = new ArrayList<>();

	private String           _dbmsServerName = "";
	private String           _dbmsVersionStr = "";
	private MonRecordingInfo _offlineRecInfo;

	private String           _lastUsedUrl    = "";
	private ConnectionProp   _lastUsedConnProp;

	//--------------------------------------------------------------------------------
	// BEGIN: issues methods
	//--------------------------------------------------------------------------------
	/** add an issue */
	@Override
	public void addConfigIssue(DbmsConfigIssue issue)
	{
		// if this is mapped to 'discard' or 'do not care'... then SKIP this issue
		boolean markedAsDiscarded = issue.isDiscarded();

		String type = markedAsDiscarded ? "discarded" : "added";
		_logger.warn("Received a DBMS Config Issue ["+type+"]. configName='"+issue.getConfigName()+"', severity="+issue.getSeverity()+", key='"+issue.getPropKey()+"', description='"+issue.getDescription().replace('\n', ' ')+"'.");
		
//		if ( markedAsDiscarded )
//			return;

		_configIssueList.add(issue);
	}

	/** Get configuration issues */
	@Override
	public List<DbmsConfigIssue> getConfigIssues()
	{
		return _configIssueList;
	}
	
	/** Check if we have any configuration issues */
	@Override
	public boolean hasConfigIssues()
	{
		int count = 0;

		for (DbmsConfigIssue issue : _configIssueList)
		{
			if ( ! issue.isDiscarded() )
				count++;
		}

		return count > 0;
//		return _configIssueList.size() > 0;
	}
	//--------------------------------------------------------------------------------
	// END: issues methods
	//--------------------------------------------------------------------------------

	@Override
	public void reset()
	{
		_configIssueList = new ArrayList<>();
	}
	
	@Override
	public void checkConfig(DbxConnection conn)
	{
	}

	@Override
	public String getFreeMemoryStr()
	{
		return "";
	}

	@Override
	public boolean isReverseEngineeringPossible()
	{
		return false;
	}

	@Override
	public String reverseEngineer(int[] modelRows)
	{
		return null;
	}

	@Override
	public String reverseEngineer(Map<String, String> keyValMap, String comment)
	{
		return null;
	}

	@Override
	public String getDbmsServerName()
	{
		return _dbmsServerName;
	}
	
	@Override
	public String getDbmsVersionStr()
	{
		return _dbmsVersionStr;
	}
	
	@Override
	public MonRecordingInfo getOfflineRecordingInfo()
	{
		return _offlineRecInfo;
	}
	
	@Override 
	public String getLastUsedUrl()
	{
		return _lastUsedUrl ; 
	}

	@Override 
	public ConnectionProp getLastUsedConnProp()
	{
		return _lastUsedConnProp ; 
	}

	public void setDbmsServerName(String str) { _dbmsServerName = str; }
	public void setDbmsVersionStr(String str) { _dbmsVersionStr = str; }
	public void setOfflineRecordingInfo(MonRecordingInfo info) { _offlineRecInfo = info; }
	
	@Override public boolean isOfflineConfig()  { return _offlineRecInfo != null; }
	@Override public boolean isOnlineConfig()   { return _offlineRecInfo == null; }

	public void   setLastUsedUrl     (String url)        { _lastUsedUrl      = url; }
	public void   setLastUsedConnProp(ConnectionProp cp) { _lastUsedConnProp = cp; }


	@Override
	public IDbmsConfigEntry getDbmsConfigEntry(String name)
	{
		Map<String, ? extends IDbmsConfigEntry> map = getDbmsConfigMap();
		return map.get(name);
	}

	
	//--------------------------------------------------------------------------------
	// BEGIN implement: AbstractTableModel
	//--------------------------------------------------------------------------------
	
	//--------------------------------------------------------------------------------
	// END implement: AbstractTableModel
	//--------------------------------------------------------------------------------
	
	
//	private static final long serialVersionUID = 1L;
//
//	/** Log4j logging. */
//	private static Logger _logger = Logger.getLogger(DbmsConfigAbstract.class);
//
//	/** Instance variable */
//	private static IDbmsConfig _instance = null;
//	
//	private boolean   _offline   = false;
//	private boolean   _hasGui    = false;
//	private Timestamp _timestamp = null;
//	
//	/** check if we got an instance or not */
//	public static boolean hasInstance()
//	{
//		return (_instance != null);
//	}
//
//	/** Get a instance of the class */
//	public static IDbmsConfig getInstance()
//	{
//		return _instance;
//	}
//
//	/** Get a instance of the class */
//	public static void setInstance(IDbmsConfig dbmsConfig)
//	{
//		_instance = dbmsConfig;
//	}
//
//
////	@Override
////	public Map<String, AseConfigEntry> getDbmsConfigMap()
////	{
////		// TODO Auto-generated method stub
////		return null;
////	}
//
//	@Override
//	public boolean isInitialized()
//	{
//		// TODO Auto-generated method stub
//		return false;
//	}
//
//	@Override
//	public void initialize(DbxConnection conn, boolean hasGui, boolean offline, Timestamp ts)
//	{
//		// TODO Auto-generated method stub
//
//	}
//
//	@Override
//	public void refresh(DbxConnection conn, Timestamp ts)
//	{
//		// TODO Auto-generated method stub
//
//	}
//
//	@Override
//	public String getDescription(String configName)
//	{
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	@Override
//	public int getRunValue(String configName)
//	{
//		// TODO Auto-generated method stub
//		return 0;
//	}
//
//	@Override
//	public List<String> getSectionList()
//	{
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	@Override
//	public Timestamp getTimestamp()
//	{
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	@Override
//	public Class<?> getColumnClass(int columnIndex)
//	{
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	@Override
//	public int getColumnCount()
//	{
//		// TODO Auto-generated method stub
//		return 0;
//	}
//
//	@Override
//	public String getColumnName(int columnIndex)
//	{
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	@Override
//	public int getRowCount()
//	{
//		// TODO Auto-generated method stub
//		return 0;
//	}
//
//	@Override
//	public Object getValueAt(int rowIndex, int columnIndex)
//	{
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	@Override
//	public boolean isCellEditable(int rowIndex, int columnIndex)
//	{
//		// TODO Auto-generated method stub
//		return false;
//	}
//
//	@Override
//	public int findColumn(String columnName)
//	{
//		// TODO Auto-generated method stub
//		return 0;
//	}
//
//	@Override
//	public String getColumnToolTip(String colName)
//	{
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	@Override
//	public String getSqlDataType(String colName)
//	{
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	@Override
//	public String getSqlDataType(int colIndex)
//	{
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	@Override
//	public String getTabLabel()
//	{
//		// TODO Auto-generated method stub
//		return null;
//	}
//
}
