package com.asetune.central;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DbxCentralStatistics
{
	private static DbxCentralStatistics _instance = null;

	/** one StatEntry for every server name we have received info from */
	private ConcurrentHashMap<String, ServerEntry> _serverMap = new ConcurrentHashMap<>();

	private String _shutdownReason = "";
	
	
	public static DbxCentralStatistics getInstance()
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
	public ServerEntry getServerEntry(String srvName)
	{
		if (_serverMap == null)
			_serverMap = new ConcurrentHashMap<>();

		ServerEntry se = _serverMap.get(srvName);
		if (se == null)
		{
			se = new ServerEntry();
			_serverMap.put(srvName, se);
		}
		
		return se;
	}
	
	/**
	 * Clear all members...
	 */
	public void clear()
	{
		if (_serverMap == null)
			_serverMap = new ConcurrentHashMap<>();
		
		for (ServerEntry se : _serverMap.values())
		{
			se.clear();
		}

		_shutdownReason = "";
	}


	public void setShutdownReason(String str) { _shutdownReason = str; }

	public String getShutdownReason() { return _shutdownReason; }
	
	/**
	 * Detailed statistics for every server
	 */
	public static class ServerEntry
	{
		private int _alarmCount = 0;
		private int _receiveCount = 0;
		private int _receiveGraphCount = 0;

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

		public int getAlarmCount()                  { return _alarmCount++; }
		public int getReceiveCount()                { return _receiveCount++; }
		public int getReceiveGraphCount()           { return _receiveGraphCount++; }


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
