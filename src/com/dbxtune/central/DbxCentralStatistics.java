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
package com.dbxtune.central;

import java.sql.Timestamp;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.dbxtune.central.pcs.DbxTuneSample;

public class DbxCentralStatistics
{
	private static DbxCentralStatistics _instance = null;

	/** one StatEntry for every server name we have received info from */
	private ConcurrentHashMap<String, ServerEntry> _serverMap = new ConcurrentHashMap<>();

	private String    _shutdownReason       = "";
	private boolean   _wasRestartSpecified  = false;
	private int       _H2DbFileSize1InMb    = -1;
	private int       _H2DbFileSize2InMb    = -1;
	private int       _H2DbFileSizeDiffInMb = -1;
	private String    _pcsJdbcWriterUrl     = "";
	
	public synchronized static DbxCentralStatistics getInstance()
	{
		if (_instance == null)
			_instance = new DbxCentralStatistics();

		return _instance;
	}


	/**
	 * Get a Map of all ServerEntries
	 * @return
	 */
	public Map<String, ServerEntry> getServerEntries()
	{
		if (_serverMap == null)
			_serverMap = new ConcurrentHashMap<>();

		return _serverMap;
	}

	/**
	 * Get ServerEntry, if none is found a new will be created.
	 * 
	 * @param srvName Name of the server to update statistics for
	 * @return never null
	 */
	public ServerEntry getServerEntry(String srvName, DbxTuneSample sample)
	{
		if (_serverMap == null)
			_serverMap = new ConcurrentHashMap<>();

		ServerEntry se = _serverMap.get(srvName);
		if (se == null)
		{
			se = new ServerEntry(srvName, sample);
			_serverMap.put(srvName, se);
		}
		
		return se;
	}
	
	/**
	 * Clear all members...
	 */
	public void clear()
	{
//		if (_serverMap == null)
//			_serverMap = new ConcurrentHashMap<>();
//		
//		for (ServerEntry se : _serverMap.values())
//		{
//			se.clear();
//		}
		_serverMap = new ConcurrentHashMap<>();

		_shutdownReason       = "";
		_wasRestartSpecified  = false;
		_H2DbFileSize1InMb    = -1;
		_H2DbFileSize2InMb    = -1;
		_H2DbFileSizeDiffInMb = -1;
		_pcsJdbcWriterUrl     = "";
	}


	public void setShutdownReason(String str)    { _shutdownReason       = str; }
	public void setRestartSpecified(boolean b)   { _wasRestartSpecified  = b; }
	public void setH2DbFileSize1InMb(int mb)     { _H2DbFileSize1InMb    = mb; }
	public void setH2DbFileSize2InMb(int mb)     { _H2DbFileSize2InMb    = mb; }
	public void setH2DbFileSizeDiffInMb(int mb)  { _H2DbFileSizeDiffInMb = mb; }
	public void setJdbcWriterUrl(String jdbcUrl) { _pcsJdbcWriterUrl     = jdbcUrl; }

	public String  getShutdownReason()       { return _shutdownReason; }
	public boolean getRestartSpecified()     { return _wasRestartSpecified; }
	public int     getH2DbFileSize1InMb()    { return _H2DbFileSize1InMb; }
	public int     getH2DbFileSize2InMb()    { return _H2DbFileSize2InMb; }
	public int     getH2DbFileSizeDiffInMb() { return _H2DbFileSizeDiffInMb; }
	public String  getJdbcWriterUrl()        { return _pcsJdbcWriterUrl; }
	
	/**
	 * Detailed statistics for every server
	 */
	public static class ServerEntry
	{
		public ServerEntry(String srvName, DbxTuneSample sample)
		{
			_srvName    = srvName;
			_dbxProduct = sample.getAppName();
			
			_firstSampleTime = new Timestamp(System.currentTimeMillis());
		}
		
		private String _srvName    = "";
		private String _dbxProduct = "";
		
		private Timestamp _firstSampleTime      = null;
		private Timestamp _lastSampleTime       = null;
		
		private int _alarmCount = 0;
		private int _receiveCount = 0;
		private int _receiveGraphCount = 0;

//		public void setFirstSampleTime()          { _firstSampleTime = new Timestamp(System.currentTimeMillis()); }
		public void setLastSampleTime()           { _lastSampleTime  = new Timestamp(System.currentTimeMillis()); }
		
		public String  getFirstSampleTimeStr()   { return _firstSampleTime == null ? null : _firstSampleTime.toString(); }
		public String  getLastSampleTimeStr()    { return _lastSampleTime  == null ? null : _lastSampleTime.toString(); }
		
//		private int _inserts      = 0;
//		private int _updates      = 0;
//		private int _deletes      = 0;
//
//		private int _createTables = 0;
//		private int _alterTables  = 0;
//		private int _dropTables   = 0;
//		
//		private int _ddlSaveCount = 0;
//
//		private int _sqlCaptureEntryCount = 0;
//		private int _sqlCaptureBatchCount = 0;
//
//		
//		private int _insertsSum      = 0;
//		private int _updatesSum      = 0;
//		private int _deletesSum      = 0;
//
//		private int _createTablesSum = 0;
//		private int _alterTablesSum  = 0;
//		private int _dropTablesSum   = 0;
//		
//		private int _ddlSaveCountSum = 0;
//
//		private int _sqlCaptureEntryCountSum = 0;
//		private int _sqlCaptureBatchCountSum = 0;

		
		public void clear()
		{
			_alarmCount = 0;
			_receiveCount = 0;
			_receiveGraphCount = 0;
			
//			_inserts = 0;
//			_updates = 0;
//			_deletes = 0;
//
//			_createTables = 0;
//			_alterTables  = 0;
//			_dropTables   = 0;
//			_ddlSaveCount = 0;
//			
//			_sqlCaptureEntryCount = 0;
//			_sqlCaptureBatchCount = 0;

			// DO NOT RESET THE SUM
		}

		public void incAlarmCount()                  { _alarmCount++; }
		public void incReceiveCount()                { _receiveCount++; }
		public void incReceiveGraphCount()           { _receiveGraphCount++; }
		public void incReceiveGraphCount(int cnt)    { _receiveGraphCount += cnt; }

		public int getAlarmCount()                   { return _alarmCount++; }
		public int getReceiveCount()                 { return _receiveCount++; }
		public int getReceiveGraphCount()            { return _receiveGraphCount++; }

		public String getSrvName()                   { return _srvName; }
		public String getDbxProduct()                { return _dbxProduct; }		


//		public void incInserts()                     { _inserts++; _insertsSum++; }
//		public void incUpdates()                     { _updates++; _updatesSum++; }
//		public void incDeletes()                     { _deletes++; _deletesSum++; }
//
//		public void incInserts(int cnt)              { _inserts += cnt; _insertsSum += cnt; }
//		public void incUpdates(int cnt)              { _updates += cnt; _updatesSum += cnt; }
//		public void incDeletes(int cnt)              { _deletes += cnt; _deletesSum += cnt; }
//
//		public void incCreateTables()                { _createTables++;         _createTablesSum++;         }
//		public void incAlterTables()                 { _alterTables++;          _alterTablesSum++;          }
//		public void incDropTables()                  { _dropTables++;           _dropTablesSum++;           }
//		public void incDdlSaveCount()                { _ddlSaveCount++;         _ddlSaveCountSum++;         }
//		public void incSqlCaptureEntryCount()        { _sqlCaptureEntryCount++; _sqlCaptureEntryCountSum++; }
//		public void incSqlCaptureBatchCount()        { _sqlCaptureBatchCount++; _sqlCaptureBatchCountSum++; }
//
//		public void incCreateTables        (int cnt) { _createTables         += cnt;  _createTablesSum         += cnt; }
//		public void incAlterTables         (int cnt) { _alterTables          += cnt;  _alterTablesSum          += cnt; }
//		public void incDropTables          (int cnt) { _dropTables           += cnt;  _dropTablesSum           += cnt; }
//		public void incDdlSaveCount        (int cnt) { _ddlSaveCount         += cnt;  _ddlSaveCountSum         += cnt; }
//		public void incSqlCaptureEntryCount(int cnt) { _sqlCaptureEntryCount += cnt;  _sqlCaptureEntryCountSum += cnt; }
//		public void incSqlCaptureBatchCount(int cnt) { _sqlCaptureBatchCount += cnt;  _sqlCaptureBatchCountSum += cnt; }
//
//		
//		// get 
//		public int getInserts()                      { return _inserts; }
//		public int getUpdates()                      { return _updates; }
//		public int getDeletes()                      { return _deletes; }
//	                                                 
//		public int getCreateTables()                 { return _createTables; }
//		public int getAlterTables()                  { return _alterTables;  }
//		public int getDropTables()                   { return _dropTables;   }
//		public int getDdlSaveCount()                 { return _ddlSaveCount; }
//		public int getSqlCaptureEntryCount()         { return _sqlCaptureEntryCount; }
//		public int getSqlCaptureBatchCount()         { return _sqlCaptureBatchCount; }
//
//		// get SUM 
//		public int getInsertsSum()                   { return _insertsSum; }
//		public int getUpdatesSum()                   { return _updatesSum; }
//		public int getDeletesSum()                   { return _deletesSum; }
//	                                              
//		public int getCreateTablesSum()              { return _createTablesSum; }
//		public int getAlterTablesSum()               { return _alterTablesSum;  }
//		public int getDropTablesSum()                { return _dropTablesSum;   }
//		public int getDdlSaveCountSum()              { return _ddlSaveCountSum; }
//		public int getSqlCaptureEntryCountSum()      { return _sqlCaptureEntryCountSum; }
//		public int getSqlCaptureBatchCountSum()      { return _sqlCaptureBatchCountSum; }
//
//		public String getStatisticsString()
//		{
//			return "inserts="+_inserts+", updates="+_updates+", deletes="+_deletes+", createTables="+_createTables+", alterTables="+_alterTables+", dropTables="+_dropTables+", ddlSaveCount="+_ddlSaveCount+", ddlSaveCountSum="+_ddlSaveCountSum+", sqlCaptureBatchCount="+_sqlCaptureBatchCount+", sqlCaptureEntryCount="+_sqlCaptureEntryCount+", sqlCaptureBatchCountSum="+_sqlCaptureBatchCountSum+", sqlCaptureEntryCountSum="+_sqlCaptureEntryCountSum+".";
//		}
		
	}
}
