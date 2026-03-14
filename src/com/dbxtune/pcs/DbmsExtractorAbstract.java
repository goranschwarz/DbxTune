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
package com.dbxtune.pcs;

import java.lang.invoke.MethodHandles;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.sql.ResultSetMetaDataCached;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.ddl.IDbmsDdlResolver;
import com.dbxtune.utils.DbUtils;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.TimeUtils;

/**
 * Helper class to transfer data from a Monitored DBMS to PCS (Persistent Counter Storage)
 * <p>
 * Example: see SqlServerBackupHistoryExtractor or SqlServerJobSchedulerExtractor
 */
public abstract class DbmsExtractorAbstract
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private String        _name;
//	private TableInfo[]   _tableInfo;

	private String        _monDbname;
	private String        _pcsSchemaName;

	private DbxConnection _monConn;
	private DbxConnection _pcsConn;

	List<ExtractorEntry> _extractors;

	protected int _totalRowsTransferred  = 0;
	protected int _totalTableTransferred = 0;

	public String getName()                  { return _name; }
	public int    getTotalRowsTransferred()  { return _totalRowsTransferred; }
	public int    getTotalTableTransferred() { return _totalTableTransferred; }

	/** Used as a prefix when writing various info/error log */
	public String getLogName()               { return getPcsSchemaName(); }


	/** Implemented by any subclasses... This is "what to do" */
	protected abstract List<ExtractorEntry> createExtractors();

	/** Get all extractors */
	protected List<ExtractorEntry> getExtractors()
	{
		return _extractors;
	}

	/** What method we should use to extract/transfer data from Source to PCS */
	public enum ExtractionMethod
	{
		/** Use ResultSetTable model as an intermediate step... Keep data in-memory... Use with small ResultSets */
		RSTM,            

		/** Stream the ResultSet from source to destination... Use with many rows */
		STREAMING
	};

	/**
	 * Simple information about an Extractor Entry
	 */
	public static interface ExtractorEntry
	{
		/** Get name of the table */
		String           getTableName();
		
		/** What server version do we need to get this information. see Ver.ver(...) */
		default public long needVersion()
		{
			return 0;
		}
		
		/** What Extraction method should we use */
		ExtractionMethod getExtractionMethod();

		/** Any indexes that we need to create, If none: return null or an empty list */
		List<String>     getCreateIndex();
		
		/** What's the SQL Statement to execute at the source DBMS to extract data */
		String           getSql();
	}

	public DbxConnection getMonConn()       { return _monConn; }
	public String        getMonDbname()     { return _monDbname; }
	public DbxConnection getPcsConn()       { return _pcsConn; }
	public String        getPcsSchemaName() { return _pcsSchemaName; }

	/**
	 * 
	 * @param name             Name of the Extractor "Group"
	 * @param monConn          Connection to the Monitored DBMS
	 * @param monDbname        Name of any database to get information from 
	 * @param pcsConn          PCS - Persistent Counter Storage Connection
	 * @param pcsSchemaName    Schema Name in the PCS we want to store this information in.
	 */
	public DbmsExtractorAbstract(String name, DbxConnection monConn, String monDbname, DbxConnection pcsConn, String pcsSchemaName)
	{
		_name          = name;
		
		_monConn       = monConn;
		_monDbname     = monDbname;
		_pcsConn       = pcsConn;
		_pcsSchemaName = pcsSchemaName;

		_extractors    = createExtractors();
	}

	/** Helper method to convert a LocalDateTime to UTC Time */
	public static LocalDateTime convertToUtc(LocalDateTime time) 
	{
	    return time.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
	}

	/**
	 * Called at start off: transfer() if you need to create any temporary tables etc before we execute transfer()
	 * <p>
	 * This is a NOOP implementation
	 */
	public void createTempTables()
	{
	}

	/**
	 * Called at end off: transfer() if you need to drop any temporary tables etc after we execute transfer()
	 * <p>
	 * This is a NOOP implementation
	 */
	public void dropTempTables()
	{
	}
	
	/**
	 * If you want to add anything the the "onTransferStart()" message.
	 * <p>
	 * The default is to return "" (empty String)
	 * @return
	 */
	protected String getPeriodInfoShort()
	{
		return "";
	}

	/**
	 * If you want to add anything the the "onTransferStart()" message.
	 * <p>
	 * The default is to return "" (empty String)
	 * @return
	 */
	protected String getPeriodInfoLong()
	{
		return "";
	}

	/** Called at start of transfer, to print out various informations.
	 * <p>
	 *  NOTE: It calls getPeriodInfo() to get additional information from the Extractor Implementation
	 */
	protected void onTransferStart()
	throws SQLException
	{
		String periodInfoShort = getPeriodInfoShort();
		String periodInfoLong  = getPeriodInfoLong();
		
		if (StringUtil.hasValue(periodInfoShort))
		{
			periodInfoShort = " (" + periodInfoShort + ")";
		}

		_logger.info("[" + getLogName() + "] Start: Transfer" + periodInfoShort + " of '" + getName() + "', in database '" + getMonDbname() + "'. " + periodInfoLong );
	}

	/** Called at end of transfer */
	protected void onTransferEnd(long startTime)
	throws SQLException
	{
		_logger.info("[" + getLogName() + "] Totally " + getTotalRowsTransferred() + " rows in " + getTotalTableTransferred() + " tables was transferred for '" + getName() + "', in database '" + getMonDbname() + "' to the PCS Schema '" + getPcsSchemaName() + "'. This took " + TimeUtils.msDiffNowToTimeStr(startTime) + " (HH:MM:SS.ms)");
	}

	/**
	 * Do the transfer
	 * 
	 * @throws SQLException
	 */
	public void transfer() 
	throws SQLException
	{
		// No need to continue if ALL extractors needs a higher version than we are connected to 
		long hasDbmsVersion = getMonConn().getDbmsVersionNumber();
		for (ExtractorEntry extractorEntry : getExtractors())
		{
			long needsDbmsVersion = extractorEntry.needVersion();
			if (needsDbmsVersion <= 0 || needsDbmsVersion >= hasDbmsVersion)
				continue;

			_logger.info("[" + getLogName() + "] NO Need to continue... for '" + getName() + "' ALL Extractors needs a verion higher than " + hasDbmsVersion + " to do it's job.");
			return;
		}
		
		onTransferStart();

		long startTime = System.currentTimeMillis();

		// Create a SCHEMA on the PCS
		String pcsSchemaName = getPcsSchemaName();
		if (StringUtil.hasValue(pcsSchemaName))
		{
			getPcsConn().createSchemaIfNotExists(pcsSchemaName);
		}

		try
		{
			// Create any temporary tables
			createTempTables();

			for (ExtractorEntry extractorEntry : _extractors)
			{
				long needsDbmsVersion = extractorEntry.needVersion();
				if (needsDbmsVersion <= 0 || needsDbmsVersion >= hasDbmsVersion)
				{
					transferTable(extractorEntry);
				}
				else
				{
					_logger.info("[" + getLogName() + "] Skipping extractor '" + extractorEntry.getTableName() + "'. Reason: It needs a at least DBMS Version " + needsDbmsVersion + ". And we are connected to version " + hasDbmsVersion + ".");
				}
			}
		}
		finally
		{
			// Drop any temporary tables
			dropTempTables();
		}

		// Print some info how we did
		onTransferEnd(startTime);
	}

	/**
	 * Wrapper of what transfer method we should use to transfer data from source to PCS
	 * 
	 * @param extractorEntry
	 * @return
	 * @throws SQLException
	 */
	protected int transferTable(ExtractorEntry extractorEntry)
	throws SQLException
	{
		boolean isStreaming = ExtractionMethod.STREAMING.equals(extractorEntry.getExtractionMethod());
//		String  tableName   = tableInfo.getTableName();
		
//		return transferTable(isStreaming, tableName);
		if (isStreaming)
		{
			return transferTableStreaming(extractorEntry);
		}
		else
		{
			return transferTableRstm(extractorEntry);
		}
	}

	/**
	 * Transfer "table" or "resultset" using a intermediate storage (in memory) to hold data before inserting them
	 * 
	 * @param extractorEntry
	 * @return
	 * @throws SQLException
	 */
	public int transferTableRstm(ExtractorEntry extractorEntry)
	throws SQLException
	{
		// Possibly: Check for SQL Server Version and do different things

//		String tabName = info.toString();
		
		DbxConnection monConn = getMonConn();
		String monDbname      = getMonDbname();

		DbxConnection pcsConn = getPcsConn();
		String pcsSchemaName  = getPcsSchemaName();
		
		String tabName = extractorEntry.getTableName();


		long startTime = System.currentTimeMillis();
		String sql = "";

		// Drop table if it already exists?
		boolean pcsTabExists = DbUtils.checkIfTableExistsNoThrow(pcsConn, null, getPcsSchemaName(), tabName);
		if (pcsTabExists)
		{
			// Should we drop it?
			// Should we truncate it?
			
			// Lets drop the table
			sql = pcsConn.quotifySqlString("drop table [" + pcsSchemaName + "].[" + tabName + "]");
			
			try (Statement stmnt = pcsConn.createStatement())
			{
				stmnt.executeUpdate(sql);
			}
		}

		// Execute SQL and store the result in 'rstm'
		// I know this isn't as "straight forward" as the code in "QueryStoreExtractor"... but it should NOT be "that" many rows...
		ResultSetMetaDataCached sourceRsmdC;
		ResultSetTableModel rstm;
		try
		{
//			sql = getSqlFor(tabInfo);
			sql = extractorEntry.getSql();
//System.out.println("TAB-NAME='" + tabName + "'.");
//System.out.println("SQL=|\n" + sql + "|.");
			rstm = ResultSetTableModel.executeQuery(monConn, sql, tabName);

			sourceRsmdC = rstm.getResultSetMetaDataCached();
			sourceRsmdC = ResultSetMetaDataCached.createNormalizedRsmd(sourceRsmdC);
		}
		catch (SQLException ex)
		{
			// Log something about the SQL caused the error.
			_logger.error("[" + getLogName() + "] Problems with SQL=|" + sql + "|. MsgNum=" + ex.getErrorCode() + ", SqlState='" + ex.getSQLState() + "', MsgText='" + ex.getMessage() + "'.");
			
			// Get out of here
			throw ex;
		}
				
		String crTabSql = null;
		IDbmsDdlResolver dbmsDdlResolver = pcsConn.getDbmsDdlResolver();
		ResultSetMetaDataCached targetRsmdC = dbmsDdlResolver.transformToTargetDbms(sourceRsmdC); // note the 'sourceRsmdC' is already normalized (done at the "top")

		crTabSql = dbmsDdlResolver.ddlTextTable(targetRsmdC, pcsSchemaName, tabName);
		crTabSql = crTabSql.trim();
		
		// Translate '[' and ']' chars into DBMS Vendor specific Quoted Chars
		crTabSql = pcsConn.quotifySqlString(crTabSql);
		
		try (Statement stmnt = pcsConn.createStatement())
		{
			stmnt.executeUpdate(crTabSql);
			
//			_logger.info("[" + monDbName + "] CREATED Destination[" + _pcsConn + "], schema '" + _schemaName + "', table '" + tabName + "'.");
			_logger.debug("[" + getLogName() + "] " + crTabSql);
//System.out.println("----------------------- Create Table: \n" + crTabSql);
		}
		catch (SQLException ex)
		{
			// Log something about the SQL caused the error.
			_logger.error("[" + getLogName() + "] Problems with Creating table '" + tabName + "', using DDL=|" + crTabSql + "|. MsgNum=" + ex.getErrorCode() + ", SqlState='" + ex.getSQLState() + "', MsgText='" + ex.getMessage() + "'.");
			
			// Get out of here
			throw ex;
		}

		// Build Insert statement
		String columnStr = " (" + StringUtil.toCommaStrQuoted('[', ']', targetRsmdC.getColumnNames()) + ")";
		
		// Build: values(?, ?, ?, ?...)
		String valuesStr = " values(" + StringUtil.removeLastComma(StringUtil.replicate("?, ", targetRsmdC.getColumnNames().size())) + ")";
		
		// Build insert SQL
		String insertSql = "insert into [" + pcsSchemaName + "].[" + tabName + "]" + columnStr + valuesStr;

		// Translate '[' and ']' chars into DBMS Vendor specific Quoted Chars
		insertSql = pcsConn.quotifySqlString(insertSql);

		// Create the Prepared Statement
		PreparedStatement pstmt = pcsConn.prepareStatement(insertSql);

		// Build the SQL Statement that will *fetch* data
//		sql = getSqlFor(tabInfo);
		sql = extractorEntry.getSql();
		sql = monConn.quotifySqlString(sql);
		
		if (_logger.isDebugEnabled())
			_logger.debug("[" + getLogName() + "] Issuing SQL for table '" + tabName + "'. \n" + sql);
//System.out.println("Issuing SQL for table '" + tabName + "'. \n" + sql);


		// Loop the saved values in 'rstm'
		int totalRowCount = 0;
		for (int r=0; r<rstm.getRowCount(); r++)
		{
			totalRowCount++;

			// Loop source columns
			for (int c=0; c<rstm.getColumnCount(); c++)
			{
				int sqlPos = c + 1;
				int sourceColJdbcDataType = sourceRsmdC.getColumnType(sqlPos);
				int targetColJdbcDataType = targetRsmdC.getColumnType(sqlPos); // FIXME: for UUID the return the "wrong" (not properly "mapped") type, it returns: ExtendedTypes.DBXTUNE_TYPE_UUID instead of ????  (workaround was to cast the 'job_id' to varchar(...) instead of: MSSQL -> uniqueidentifier
//TODO: createNormalizedRsmd also needs to set "target JDBC Type"... For example SQLServer->H2 we should do: ExtendedTypes.DBXTUNE_TYPE_UUID --> java.sql.Types.VARCHAR ??? (or does H2 have a "special" java.sql.Types.XXXX)
//      --------------------------------
//      https://www.h2database.com/html/datatypes.html?highlight=limited&search=Limit#uuid_type
//      --------------------------------
//      RFC 9562-compliant universally unique identifier. This is a 128 bit value. To store values, use PreparedStatement.setBytes, setString, or setObject(uuid) (where uuid is a java.util.UUID). ResultSet.getObject will return a java.util.UUID.
//      Please note that using an index on randomly generated data will result on poor performance once there are millions of rows in a table. The reason is that the cache behavior is very bad with randomly distributed data. This is a problem for any database system. To avoid this problem use UUID version 7 values.
//      For details, see the documentation of java.util.UUID
				
				Object nullReplacement = null;
				Object obj = rstm.getValueAt(r, c, nullReplacement);
//				String colName = rstm.getColumnName(c);

				try
				{
					// if source type is "TIMESTAMP WITH TIME ZONE"
					// Then we might have to get it as a string
					if (obj != null && sourceColJdbcDataType == Types.TIMESTAMP_WITH_TIMEZONE)
					{
//						if (_logger.isDebugEnabled() && QsTables.query_store_runtime_stats_interval.equals(tabName))
//							_logger.debug("---- query_store_runtime_stats_interval[colPos=" + sqlPos + ", colName='" + sourceRsmdC.getColumnLabel(sqlPos) + "'] val is TIMESTAMP_WITH_TIMEZONE=|" + obj.toString() + "|, objJavaClass='" + obj.getClass().getName() + "'.");
//
//						obj = obj.toString();
					}
					
					// Ugly: But translate/inject some "newlines" in the message on specific wordings...
					// Lets do this some where else, like the client that will finally read the data
//					if ("message".equalsIgnoreCase(colName) && obj != null)
//					{
//						obj = formatMessageString( obj.toString(), "<BR>" );
////						obj = formatMessageString( obj.toString(), "\n" );
//					}

					// SET the data or NULL value
					if (obj != null)
						pstmt.setObject(sqlPos, obj, targetColJdbcDataType);
					else
						pstmt.setNull(sqlPos, targetColJdbcDataType);
					
				}
				catch (SQLException ex)
				{
					String sourceColName = sourceRsmdC.getColumnLabel(sqlPos);
					String destColName   = targetRsmdC.getColumnLabel(sqlPos);

					String msg = "[" + getLogName() + "] ROW: " + totalRowCount + " - Problems setting column c=" + sqlPos + ", sourceName='" + sourceColName + "', destName='" + destColName + "'. Caught: Err=" + ex.getErrorCode() + ", State='" + ex.getSQLState() + "', msg='" + ex.getMessage().trim() + "'.";
					_logger.error(msg);

					// NOTE: Here we THROW (out of method), should we do something "better"
					throw ex;
				}
			} // end: loop source columns
			
			// Add ROW to batch
			pstmt.addBatch();

		} // end: loop rows from source

		// Send the whole batch to the TARGET
//		int[] batchInsCount = pstmt.executeBatch();
		pstmt.executeBatch();
		pstmt.close();
//System.out.println("batchInsCount.length=" + batchInsCount.length + ": " + StringUtil.toCommaStr(batchInsCount));

		_logger.info("[" + getLogName() + "] --> Transferred " + totalRowCount + " rows from '" + monDbname + "' into: schema '" + pcsSchemaName + "', table '" + tabName + "'. This took " + TimeUtils.msDiffNowToTimeStr(startTime) + " (HH:MM:SS.ms)");

		// Possibly create any indexes
//		createIndexForTable(tabInfo);
		createIndexForTable(extractorEntry);

		// Increment some statistics
		_totalRowsTransferred += totalRowCount;
		_totalTableTransferred++;

		return totalRowCount;
	} // end: method


	/**
	 * Streaming version of the "transferTable" no intermediate storage is needed
	 * @param tabName
	 * @return
	 * @throws SQLException
	 */
//	public int transferTableStreaming(String tabName)
//	public int transferTableStreaming(TableInfo tabInfo)
	public int transferTableStreaming(ExtractorEntry extractorEntry)
	throws SQLException
	{
		DbxConnection monConn = getMonConn();
		String monDbname      = getMonDbname();

		DbxConnection pcsConn = getPcsConn();
		String pcsSchemaName  = getPcsSchemaName();
		
		String tabName = extractorEntry.getTableName();

		long startTime = System.currentTimeMillis();
		String sql = "";

		// Drop table if it already exists?
		boolean pcsTabExists = DbUtils.checkIfTableExistsNoThrow(pcsConn, null, pcsSchemaName, tabName);
		if (pcsTabExists)
		{
			// Should we drop it?
			// Should we truncate it?
			
			// Lets drop the table
			sql = pcsConn.quotifySqlString("drop table [" + pcsSchemaName + "].[" + tabName + "]");
			
			try (Statement stmnt = pcsConn.createStatement())
			{
				stmnt.executeUpdate(sql);
			}
		}

		// Execute SQL (with and extra WHERE 1=2) only to get MetaData for the SOURCE ResultSet, which we are about to transfer
		// This so we can make a CRERATE TABLE DDL String
		ResultSetMetaDataCached sourceRsmdC;
//		sql = getSqlFor(tabInfo) + "\nWHERE 1=2"; // execute ('and 1=2' means more or less "no-exec") get only the ResultSet (this is needed if we append any columns like: schema_name & object_name...
		sql = extractorEntry.getSql() + "\nWHERE 1=2"; // execute ('and 1=2' means more or less "no-exec") get only the ResultSet (this is needed if we append any columns like: schema_name & object_name...
		try (Statement stmnt = monConn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			sourceRsmdC = ResultSetMetaDataCached.createNormalizedRsmd(rs);
		}
		catch (SQLException ex)
		{
			// Log something about the SQL caused the error.
			_logger.error("[" + getLogName() + "] Problems with SQL=|" + sql + "|. MsgNum=" + ex.getErrorCode() + ", SqlState='" + ex.getSQLState() + "', MsgText='" + ex.getMessage() + "'.");
			
			// Get out of here
			throw ex;
		}
		

		String crTabSql = null;
		IDbmsDdlResolver dbmsDdlResolver = pcsConn.getDbmsDdlResolver();
		ResultSetMetaDataCached targetRsmdC = dbmsDdlResolver.transformToTargetDbms(sourceRsmdC); // note the 'sourceRsmdC' is already normalized (done at the "top")

		crTabSql = dbmsDdlResolver.ddlTextTable(targetRsmdC, pcsSchemaName, tabName);
		crTabSql = crTabSql.trim();
		
		// Translate '[' and ']' chars into DBMS Vendor specific Quoted Chars
		crTabSql = pcsConn.quotifySqlString(crTabSql);
		
		try (Statement stmnt = pcsConn.createStatement())
		{
			stmnt.executeUpdate(crTabSql);
			
//			_logger.info("[" + getLogName() + "] CREATED Destination[" + _pcsConn + "], schema '" + _schemaName + "', table '" + tabName + "'.");
			_logger.debug("[" + getLogName() + "] " + crTabSql);
//System.out.println("----------------------- Create Table: \n" + crTabSql);
		}
		catch (SQLException ex)
		{
			// Log something about the SQL caused the error.
			_logger.error("[" + getLogName() + "] Problems with Creating table '" + tabName + "', using DDL=|" + crTabSql + "|. MsgNum=" + ex.getErrorCode() + ", SqlState='" + ex.getSQLState() + "', MsgText='" + ex.getMessage() + "'.");

			// Get out of here
			throw ex;
		}

		// Build Insert statement
		String columnStr = " (" + StringUtil.toCommaStrQuoted('[', ']', targetRsmdC.getColumnNames()) + ")";
		
		// Build: values(?, ?, ?, ?...)
		String valuesStr = " values(" + StringUtil.removeLastComma(StringUtil.replicate("?, ", targetRsmdC.getColumnNames().size())) + ")";
		
		// Build insert SQL
		String insertSql = "insert into [" + pcsSchemaName + "].[" + tabName + "]" + columnStr + valuesStr;

		// Translate '[' and ']' chars into DBMS Vendor specific Quoted Chars
		insertSql = pcsConn.quotifySqlString(insertSql);

		// Create the Prepared Statement
		PreparedStatement pstmt = pcsConn.prepareStatement(insertSql);

		// Build the SQL Statement that will *fetch* data
//		sql = monConn.quotifySqlString(getSqlFor(tabInfo));
		sql = extractorEntry.getSql();
		sql = monConn.quotifySqlString(sql);
		
		if (_logger.isDebugEnabled())
			_logger.debug("[" + getLogName() + "] Issuing SQL for table '" + tabName + "'. \n" + sql);
//System.out.println("Issuing SQL for table '" + tabName + "'. \n" + sql);

		// Execute SQL and Loop the SOURCE ResultSet and: setObject(), addBatch(), executeBatch()
		int totalRowCount = 0;
		try (Statement stmnt = monConn.createStatement(); ResultSet sourceRs = stmnt.executeQuery(sql))
		{
			// Loop source rows
			while (sourceRs.next())
			{
				totalRowCount++;

				// Loop source columns
				for (int sqlPos=1; sqlPos<sourceRsmdC.getColumnCount()+1; sqlPos++)
				{
					int sourceColJdbcDataType = sourceRsmdC.getColumnType(sqlPos);
					int targetColJdbcDataType = targetRsmdC.getColumnType(sqlPos);

					try
					{
						// GET data
						Object obj = sourceRs.getObject(sqlPos);

						// if source type is "TIMESTAMP WITH TIME ZONE"
						// Then we might have to get it as a string
						if (obj != null && sourceColJdbcDataType == Types.TIMESTAMP_WITH_TIMEZONE)
						{
							obj = obj.toString();
						}

						// SET the data or NULL value
						if (obj != null)
							pstmt.setObject(sqlPos, obj, targetColJdbcDataType);
						else
							pstmt.setNull(sqlPos, targetColJdbcDataType);
					}
					catch (SQLException ex)
					{
						String sourceColName = sourceRsmdC.getColumnLabel(sqlPos);
						String destColName   = targetRsmdC.getColumnLabel(sqlPos);

						String msg = "[" + monDbname + "] ROW: " + totalRowCount + " - Problems setting column c=" + sqlPos + ", sourceName='" + sourceColName + "', destName='" + destColName + "'. Caught: Err=" + ex.getErrorCode() + ", State='" + ex.getSQLState() + "', msg='" + ex.getMessage().trim() + "'.";
						_logger.error(msg);

						// NOTE: Here we THROW (out of method), should we do something "better"
						throw ex;
					}
				} // end: loop source columns

				// Add ROW to batch
				pstmt.addBatch();

			} // end: loop rows from source

		} // end: select from source
		catch (SQLException ex)
		{
			// Log something about the SQL caused the error.
			_logger.error("[" + getLogName() + "] Problems with SQL=|" + sql + "|. MsgNum=" + ex.getErrorCode() + ", SqlState='" + ex.getSQLState() + "', MsgText='" + ex.getMessage() + "'.");

			// Get out of here
			throw ex;
		}

		// Send the whole batch to the TARGET
//		int[] batchInsCount = pstmt.executeBatch();
		pstmt.executeBatch();
		pstmt.close();
//System.out.println("batchInsCount.length=" + batchInsCount.length + ": " + StringUtil.toCommaStr(batchInsCount));

		_logger.info("[" + getLogName() + "] --> Transferred " + totalRowCount + " rows from '" + monDbname + "' into: schema '" + pcsSchemaName + "', table '" + tabName + "'. This took " + TimeUtils.msDiffNowToTimeStr(startTime) + " (HH:MM:SS.ms)");

		// Possibly create any indexes
//		createIndexForTable(tabInfo);
		createIndexForTable(extractorEntry);

		// Increment some statistics
		_totalRowsTransferred += totalRowCount;
		_totalTableTransferred++;

		return totalRowCount;
	} // end: method

	/**
	 * Create indexes
	 * <p>
	 * Simply calls getCreateIndex() on the ExtractorEntry object to check/create any needed indexes...
	 * 
	 * @param extractorEntry
	 */
	public void createIndexForTable(ExtractorEntry extractorEntry)
	{
		List<String> ddlList = extractorEntry.getCreateIndex();
		if (ddlList == null)
			return;
		if (ddlList.isEmpty())
			return;

		String tableName = extractorEntry.getTableName();
		
//		DbxConnection monConn = getMonConn();
//		String monDbName      = getMonDbname();

		DbxConnection pcsConn = getPcsConn();
		String pcsSchemaName  = getPcsSchemaName();

		for (String ddl : ddlList)
		{
			ddl = pcsConn.quotifySqlString(ddl);
			
			try (Statement stmnt = pcsConn.createStatement())
			{
				_logger.info("[" + getLogName() + "]     Created index in schema '" + pcsSchemaName + "' on table '" + tableName + "'. DDL: " + ddl);
				stmnt.executeUpdate(ddl);
			}
			catch (SQLException ex)
			{
				_logger.error("[" + getLogName() + "] Problems creating index on table '" + tableName + "' using DDL '" + ddl + "'. Caught: " + ex);
			}
		}
	}
}
