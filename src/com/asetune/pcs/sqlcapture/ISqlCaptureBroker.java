package com.asetune.pcs.sqlcapture;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.List;

import com.asetune.pcs.PersistentCounterHandler;
import com.asetune.sql.conn.DbxConnection;

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

	public List<String> getTableNames();
	public String getTableDdlString(DatabaseMetaData dbmd, String tabName);
	public String getIndexDdlString(DatabaseMetaData dbmd, String tabName);

	public int doSqlCapture(DbxConnection conn, PersistentCounterHandler persistentCounterHandler);

	public String getInsertStatement(String tabName);
}
