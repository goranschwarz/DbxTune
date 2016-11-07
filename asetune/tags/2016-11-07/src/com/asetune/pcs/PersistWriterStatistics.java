package com.asetune.pcs;

public class PersistWriterStatistics
{
	private int _inserts      = 0;
	private int _updates      = 0;
	private int _deletes      = 0;

	private int _createTables = 0;
	private int _alterTables  = 0;
	private int _dropTables   = 0;
	
	private int _ddlSaveCount = 0;

	private int _sqlCaptureEntryCount = 0;
	private int _sqlCaptureBatchCount = 0;

	
	private int _insertsSum      = 0;
	private int _updatesSum      = 0;
	private int _deletesSum      = 0;

	private int _createTablesSum = 0;
	private int _alterTablesSum  = 0;
	private int _dropTablesSum   = 0;
	
	private int _ddlSaveCountSum = 0;

	private int _sqlCaptureEntryCountSum = 0;
	private int _sqlCaptureBatchCountSum = 0;

	
	public void clear()
	{
		_inserts = 0;
		_updates = 0;
		_deletes = 0;

		_createTables = 0;
		_alterTables  = 0;
		_dropTables   = 0;
		_ddlSaveCount = 0;
		
		_sqlCaptureEntryCount = 0;
		_sqlCaptureBatchCount = 0;

		// DO NOT RESET THE SUM
	}
	
	public void incInserts()                     { _inserts++; _insertsSum++; }
	public void incUpdates()                     { _updates++; _updatesSum++; }
	public void incDeletes()                     { _deletes++; _deletesSum++; }

	public void incInserts(int cnt)              { _inserts += cnt; _insertsSum += cnt; }
	public void incUpdates(int cnt)              { _updates += cnt; _updatesSum += cnt; }
	public void incDeletes(int cnt)              { _deletes += cnt; _deletesSum += cnt; }

	public void incCreateTables()                { _createTables++;         _createTablesSum++;         }
	public void incAlterTables()                 { _alterTables++;          _alterTablesSum++;          }
	public void incDropTables()                  { _dropTables++;           _dropTablesSum++;           }
	public void incDdlSaveCount()                { _ddlSaveCount++;         _ddlSaveCountSum++;         }
	public void incSqlCaptureEntryCount()        { _sqlCaptureEntryCount++; _sqlCaptureEntryCountSum++; }
	public void incSqlCaptureBatchCount()        { _sqlCaptureBatchCount++; _sqlCaptureBatchCountSum++; }

	public void incCreateTables        (int cnt) { _createTables         += cnt;  _createTablesSum         += cnt; }
	public void incAlterTables         (int cnt) { _alterTables          += cnt;  _alterTablesSum          += cnt; }
	public void incDropTables          (int cnt) { _dropTables           += cnt;  _dropTablesSum           += cnt; }
	public void incDdlSaveCount        (int cnt) { _ddlSaveCount         += cnt;  _ddlSaveCountSum         += cnt; }
	public void incSqlCaptureEntryCount(int cnt) { _sqlCaptureEntryCount += cnt;  _sqlCaptureEntryCountSum += cnt; }
	public void incSqlCaptureBatchCount(int cnt) { _sqlCaptureBatchCount += cnt;  _sqlCaptureBatchCountSum += cnt; }

	
	// get 
	public int getInserts()                      { return _inserts; }
	public int getUpdates()                      { return _updates; }
	public int getDeletes()                      { return _deletes; }
                                                 
	public int getCreateTables()                 { return _createTables; }
	public int getAlterTables()                  { return _alterTables;  }
	public int getDropTables()                   { return _dropTables;   }
	public int getDdlSaveCount()                 { return _ddlSaveCount; }
	public int getSqlCaptureEntryCount()         { return _sqlCaptureEntryCount; }
	public int getSqlCaptureBatchCount()         { return _sqlCaptureBatchCount; }

	// get SUM 
	public int getInsertsSum()                   { return _insertsSum; }
	public int getUpdatesSum()                   { return _updatesSum; }
	public int getDeletesSum()                   { return _deletesSum; }
                                              
	public int getCreateTablesSum()              { return _createTablesSum; }
	public int getAlterTablesSum()               { return _alterTablesSum;  }
	public int getDropTablesSum()                { return _dropTablesSum;   }
	public int getDdlSaveCountSum()              { return _ddlSaveCountSum; }
	public int getSqlCaptureEntryCountSum()      { return _sqlCaptureEntryCountSum; }
	public int getSqlCaptureBatchCountSum()      { return _sqlCaptureBatchCountSum; }

	public String getStatisticsString()
	{
		return "inserts="+_inserts+", updates="+_updates+", deletes="+_deletes+", createTables="+_createTables+", alterTables="+_alterTables+", dropTables="+_dropTables+", ddlSaveCount="+_ddlSaveCount+", ddlSaveCountSum="+_ddlSaveCountSum+", sqlCaptureBatchCount="+_sqlCaptureBatchCount+", sqlCaptureEntryCount="+_sqlCaptureEntryCount+".";
	}

}
