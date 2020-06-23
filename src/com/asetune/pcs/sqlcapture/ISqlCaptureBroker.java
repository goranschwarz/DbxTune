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
package com.asetune.pcs.sqlcapture;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import com.asetune.pcs.PersistentCounterHandler;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;

public interface ISqlCaptureBroker
{
//	/**
//	 * Before adding a entry to the queue, we have the ability to discard an entry from lookup...
//	 * <p>
//	 * Discard can be useful to do at an early state, for example if it's a "worker table" or 
//	 * some other objects that we do not want, or simply can't to lookup.
//	 * 
//	 * @param dbname
//	 * @param objectName
//	 * @return <b>true</b> to queue it on the list to inspect<br>	
//	 *         <b>false</b> if we want to discard the object.
//	 */
//	public boolean allowInspection(ObjectLookupQueueEntry entry);
//	
//	/**
//	 * Get DDL information from the database and pass it on to the storage thread
//	 * 
//	 * @param DbxConnection to the connection used for lookup (a connection to the source system)
//	 * @param qe Object which holds information about what to be inspected, and also members/methods where you can add information about the lookup.
//	 * @param prevLookupTimeMs how long did the previous lookup take, if that is of interest
//	 * 
//	 * @return an DdlDetails object which is filled in or NULL if you want to skip this lookup 
//	 */
//	public DdlDetails doObjectInfoLookup(DbxConnection conn, ObjectLookupQueueEntry qe, PersistentCounterHandler pch);
//
	/**
	 * Called after a connection has been made<br>
	 * Can be used to install specialized objects used by the SQL Capture Broker
	 */
	public void onConnect(DbxConnection conn);

	/**
	 * Create a connection that will be used to Capture SQL Statements from the DBMS
	 * 
	 * @return A Connection
	 * @throws SQLException
	 */
	public DbxConnection createConnection()
	throws Exception;

	/**
	 * Check the connection for any abnormalities, like open transactions etc...<p>
	 * if we return TRUE:  Then the collection is OK<br>
	 * if we return FALSE: The Connection will be closed by the PersistentCounterHandler & a new will be opened later.<br>
	 * on close, let the DBMS handle any eventual rollbacks and cleanups
	 * 
	 * @param conn The connection that we want to check.
	 * @return true=OK, false=CloseConnection
	 * @throws Exception If this happens, it's the same as if return FALSE... meaning: close the connection.
	 */
	public boolean checkConnection(DbxConnection conn)
	throws Exception;

	public List<String> getTableNames();
	public String getTableDdlString(DbxConnection conn, DatabaseMetaData dbmd, String tabName);
	public List<String> getIndexDdlString(DbxConnection conn, DatabaseMetaData dbmd, String tabName);

	/**
	 * Check if table columns are ok, or if we need to alter table, returns a list of alter statements to execute
	 * 
	 * @param conn
	 * @param dbmd
	 * @param tabName
	 * 
	 * @return a list of alter statements to execute
	 */
	public List<String> checkTableDdl(DbxConnection conn, DatabaseMetaData dbmd, String tabName);

	public int doSqlCapture(DbxConnection conn, PersistentCounterHandler persistentCounterHandler);

	public String getInsertStatement(DbxConnection conn, String tabName);

	/**
	 * Get what (if any) columns that are Dictionary Compressed.
	 * <p>
	 * The returned Map is
	 * <ul>
	 *   <li>key   = Position in the value list/array that is sent. it starts at 1... 0 is where the table name is stored.</li>
	 *   <li>value = The column name </li>
	 * </ul>
	 * 
	 * @param tabName  table name 
	 * @return a Map(colPos, colName), null if no columns are Dictionary Compressed <br>
	 *         The 'colPos' is a pointer to where in the value array/list we would find the column (position 0 is where the table name is stored)
	 */
	Map<Integer, String> getDictionaryCompressionColumnMap(String tabName);

	/**
	 * Initialize the Broker with the same configuartion as the PersistenceCounterHandler
	 * @param conf
	 */
	public void init(Configuration conf);

	public Configuration getConfiguration();
	public String        getProperty       (String propName, String  defaultValue);
	public boolean       getBooleanProperty(String propName, boolean defaultValue);
	public int           getIntProperty    (String propName, int     defaultValue);
	public long          getLongProperty   (String propName, long    defaultValue);
}
