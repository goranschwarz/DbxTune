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
package com.dbxtune.pcs;

import com.dbxtune.utils.StringUtil;

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


	// ------------------------------------------------
	// Remember Max length for string formating between getStatisticsString() calls
	// ------------------------------------------------
	private int _maxLen_inserts      = 0;
	private int _maxLen_updates      = 0;
	private int _maxLen_deletes      = 0;

	private int _maxLen_createTables = 0;
	private int _maxLen_alterTables  = 0;
	private int _maxLen_dropTables   = 0;

	private int _maxLen_ddlSaveCount = 0;

	private int _maxLen_sqlCaptureEntryCount = 0;
	private int _maxLen_sqlCaptureBatchCount = 0;

	private int _maxLen_ddlSaveCountSum = 0;

	private int _maxLen_sqlCaptureEntryCountSum = 0;
	private int _maxLen_sqlCaptureBatchCountSum = 0;

	
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
		_maxLen_inserts                  = Math.max(_maxLen_inserts                , (_inserts                 +", ").length());
		_maxLen_updates                  = Math.max(_maxLen_updates                , (_updates                 +", ").length());
		_maxLen_deletes                  = Math.max(_maxLen_deletes                , (_deletes                 +", ").length());

		_maxLen_createTables             = Math.max(_maxLen_createTables           , (_createTables            +", ").length());
		_maxLen_alterTables              = Math.max(_maxLen_alterTables            , (_alterTables             +", ").length());
		_maxLen_dropTables               = Math.max(_maxLen_dropTables             , (_dropTables              +", ").length());

		_maxLen_ddlSaveCount             = Math.max(_maxLen_ddlSaveCount           , (_ddlSaveCount            +", ").length());
		_maxLen_ddlSaveCountSum          = Math.max(_maxLen_ddlSaveCountSum        , (_ddlSaveCountSum         +", ").length());

		_maxLen_sqlCaptureBatchCount     = Math.max(_maxLen_sqlCaptureBatchCount   , (_sqlCaptureBatchCount    +", ").length());
		_maxLen_sqlCaptureEntryCount     = Math.max(_maxLen_sqlCaptureEntryCount   , (_sqlCaptureEntryCount    +", ").length());
		_maxLen_sqlCaptureBatchCountSum  = Math.max(_maxLen_sqlCaptureBatchCountSum, (_sqlCaptureBatchCountSum +", ").length());
		_maxLen_sqlCaptureEntryCountSum  = Math.max(_maxLen_sqlCaptureEntryCountSum, (_sqlCaptureEntryCountSum +", ").length());

		String retStr = 
			  "inserts="                 + StringUtil.left(_inserts                +", "  , _maxLen_inserts                )
			+ "updates="                 + StringUtil.left(_updates                +", "  , _maxLen_updates                )
			+ "deletes="                 + StringUtil.left(_deletes                +", "  , _maxLen_deletes                )

			+ "createTables="            + StringUtil.left(_createTables           +", "  , _maxLen_createTables           )
			+ "alterTables="             + StringUtil.left(_alterTables            +", "  , _maxLen_alterTables            )
			+ "dropTables="              + StringUtil.left(_dropTables             +", "  , _maxLen_dropTables             )
			;

		// Do NOT print DDL info if we havn't got any...
		if (_ddlSaveCountSum > 0) retStr += ""
			+ "ddlSaveCount="            + StringUtil.left(_ddlSaveCount           +", "  , _maxLen_ddlSaveCount           )
			+ "ddlSaveCountSum="         + StringUtil.left(_ddlSaveCountSum        +", "  , _maxLen_ddlSaveCountSum        )
			;

		// Do NOT print SQL CAPTURE info if we havn't got any...
		if (_sqlCaptureEntryCountSum > 0) retStr += ""
			+ "sqlCaptureBatchCount="    + StringUtil.left(_sqlCaptureBatchCount   +", "  , _maxLen_sqlCaptureBatchCount   )
			+ "sqlCaptureEntryCount="    + StringUtil.left(_sqlCaptureEntryCount   +", "  , _maxLen_sqlCaptureEntryCount   )
			+ "sqlCaptureBatchCountSum=" + StringUtil.left(_sqlCaptureBatchCountSum+", "  , _maxLen_sqlCaptureBatchCountSum)
			+ "sqlCaptureEntryCountSum=" + StringUtil.left(_sqlCaptureEntryCountSum+". "  , _maxLen_sqlCaptureEntryCountSum) // Last line is a '.'
			;
		
		return retStr;
	}
}
