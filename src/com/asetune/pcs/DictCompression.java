/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
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
package com.asetune.pcs;

import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;

import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;
//import com.asetune.utils.ConnectionProvider;
import com.asetune.utils.DbUtils;
import com.asetune.utils.StringUtil;
import com.asetune.utils.TimeUtils;

public class DictCompression
{
	private static Logger _logger = Logger.getLogger(DictCompression.class);

	public enum DigestType
	{
		MD5,
		SHA1, 
		SHA256;

		/** parse the value */
		public static DigestType fromString(String text)
		{
			for (DigestType type : DigestType.values()) 
			{
				// check for upper/lower: 
				if (type.name().equalsIgnoreCase(text))
					return type;
			}

			throw new IllegalArgumentException("Unknown DigestType '" + text + "' found, possible values: "+StringUtil.toCommaStr(DigestType.values()));
		}
	};

	/**
	 * Hold a entry of existing hashId's that has already been persisted
	 * The KEY in the Map is the CmName/BaseTable name
	 */
	private Map<String, Map<String, Set<String>>> _valExistsMap = new HashMap<>();
	//          tabName     colName     HashId

	// Where we get a connection from
//	private ConnectionProvider _connProvider;
	private DigestType _digestType;

	public static final String DCC_MARKER = "$dcc$";

	public static final String  PROPKEY_digestType                = "DictCompression.digest.type";
	public static final String  DEFAULT_digestType                = DigestType.MD5.toString();
	
	public static final String  PROPKEY_enabled                   = "DictCompression.enabled";
	public static final boolean DEFAULT_enabled                   = true;

	public static final String  PROPKEY_statistics_enabled        = "DictCompression.statistics.enabled";
	public static final boolean DEFAULT_statistics_enabled        = true;

	public static final String  PROPKEY_statistics_printTimeInSec = "DictCompression.statistics.printTimeInSec";
//	public static final int     DEFAULT_statistics_printTimeInSec = 3600; // 1 hour
//	public static final int     DEFAULT_statistics_printTimeInSec = 1800; // 30 minutes
	public static final int     DEFAULT_statistics_printTimeInSec = 900;  // 15 minutes
//	public static final int     DEFAULT_statistics_printTimeInSec = 300;  // 5 minutes

	
	// Statistics fields
	private long _statLastReportTime        = System.currentTimeMillis();

	private long _stat_printTimeInSec       = -1;

	private long _stat_lookupCount          = 0;
	private long _stat_reuseCount           = 0;
	private long _stat_addCount             = 0;

	private long _stat_bytesSaved           = 0;
	private long _stat_bytesAdded           = 0;
	
	//////////////////////////////////////////////
	// BEGIN: Instance
	//////////////////////////////////////////////
	// implements singleton pattern
	private static DictCompression _instance = null;
	private static boolean         _isEnabled = false;

	public static DictCompression getInstance()
	{
//		if (_instance == null)
//			throw new RuntimeException("No instance of DictCompression has been assigned. Do that with setInstance()");

		if (_instance == null)
			_instance = new DictCompression();
		
		return _instance;
	}

//	public static boolean hasInstance()
//	{
//		return (_instance != null);
//	}
//
//	public static void setInstance(DictCompression inst)
//	{
//		_instance = inst;
//	}
	//////////////////////////////////////////////
	// END: Instance
	//////////////////////////////////////////////

	public static boolean isEnabledInConfiguration()
	{
		return Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_enabled, DEFAULT_enabled);
	}

	public static boolean isEnabled()
	{
		return _isEnabled;
	}

// Maybe if we want to enable this on a "Table" level...
// * Add info in: createTable(...)
// * Add/Set info in: getCompressedTableNames() or whoever that calls it... (on startup)
// * empty it on clear()
//	public static boolean isEnabled(String tabName)
//	{
//		if ( ! _isEnabled )
//			return false;
//		
//		return _isEnabledForTable.contains(tabName);
//	}
//	private static Set<String> _isEnabledForTable = new HashSet<>();

	public static void setEnabled(boolean enable)
	{
		_isEnabled = enable;
	}
	

	//////////////////////////////////////////////
	// BEGIN: Constructors
	//////////////////////////////////////////////
	public DictCompression()
	{
		this( DigestType.fromString( Configuration.getCombinedConfiguration().getProperty(PROPKEY_digestType, DEFAULT_digestType) ) );
	}

	public DictCompression(DigestType digestType)
	{
		_digestType = digestType;
		
		_stat_printTimeInSec = Configuration.getCombinedConfiguration().getLongProperty(PROPKEY_statistics_printTimeInSec, DEFAULT_statistics_printTimeInSec);
		
		_logger.info("Dictionary Compression Created with digest type '" + _digestType + "', Statistics will be reported every " + _stat_printTimeInSec + " second (on activity).");
	}

	//	public DictCompression(ConnectionProvider connProvider)
//	{
//		this(connProvider, DigestType.MD5);
//	}
//
//	public DictCompression(DigestType digestType)
//	{
//		this(null, digestType);
//	}
//
//	public DictCompression(ConnectionProvider connProvider, DigestType digestType)
//	{
//		_connProvider = connProvider;
//		_digestType   = digestType;
//	}

	//////////////////////////////////////////////
	// END: Constructors
	//////////////////////////////////////////////

	private void printStatistics()
	{
		printStatistics(false);
	}
	private void printStatistics(boolean forcePrint)
	{
		long secSinceLastReport = (System.currentTimeMillis() - _statLastReportTime) / 1000;
		
		// Exit if we havn't reached "time to report"...
		if (forcePrint == false) // the default
		{
			if (secSinceLastReport < _stat_printTimeInSec)
				return;
		}

		// Print report (this should only be 1 line)
		String report = String.format("Dictionary Compression Statistics: savedSpace[MB=%.1f, KB=%.1f], addedSpace[MB=%.1f, KB=%.1f], lookups[cnt=%d, add=%d, reUse=%d, reUsePct=%.1f]. This report period was for %s (MM:SS) or %d seconds.",
				(_stat_bytesSaved/1024.0/1024.0), (_stat_bytesSaved/1024.0),
				(_stat_bytesAdded/1024.0/1024.0), (_stat_bytesAdded/1024.0),
				_stat_lookupCount, _stat_addCount, _stat_reuseCount, ((_stat_reuseCount*100.0f)/_stat_lookupCount),
				TimeUtils.secToTimeStrShort(secSinceLastReport), secSinceLastReport);
		_logger.info(report);

		// Reset statistics...
		_statLastReportTime = System.currentTimeMillis();

		_stat_lookupCount   = 0;
		_stat_reuseCount    = 0;
		_stat_addCount      = 0;

		_stat_bytesSaved    = 0;
		_stat_bytesAdded    = 0;
	}

	/**
	 * Get String length of the Various Digest types
	 * @param digestType
	 * @return
	 */
	public static int getDigestLength(DigestType digestType)
	{
		switch (digestType)
		{
		case MD5:    return 32;
		case SHA1:   return 40;
		case SHA256: return 64;
		}
		throw new RuntimeException("DigestType '" + digestType + "' is unknown.");
	}

	/** */
	public String getDigestSourceColumnName(String colName)
	{
		return colName.trim() + DCC_MARKER;
	}

	/** Get String length of the Current instantiated Digest types */
	public int getDigestLength()
	{
		return getDigestLength(_digestType);
	}
	
	/** Get the JDBC data type for how any Digest String will be stored as */
	public int getDigestJdbcType()
	{
		return Types.CHAR;
	}

	/**
	 * Check if there are <b>any</b> columns that looks like a Dictionary Compressed Column (ending with "$dcc$")
	 * 
	 * @param conn         The connection used to get DatabaseMetaData
	 * @param schemaName   Tables Schema name (can be null, of schema name isn't used)
	 * @param tabName      Table name to get all columns from
	 * 
	 * @return true if any column ends with "$dcc$"
	 */
	public static boolean hasCompressedColumnNames(DbxConnection conn, String schemaName, String tabName)
	throws SQLException
	{
		DatabaseMetaData dbmd = conn.getMetaData();

		ResultSet rsmd = dbmd.getColumns(null, schemaName, tabName, "%");
		while(rsmd.next())
		{
			String originColName  = rsmd.getString("COLUMN_NAME");
			
			if (originColName.endsWith(DCC_MARKER))
			{
				rsmd.close();
				return true;
			}
		}
		rsmd.close();
		return false;
	}

	/**
	 * Check if there are <b>any</b> tables that looks like a Dictionary Compressed Table (contains "$dcc$")
	 * 
	 * @param conn         The connection used to get DatabaseMetaData
	 * @param schemaName   Tables Schema name (can be null, of schema name isn't used)
	 * 
	 * @return true if any column ends with "$dcc$"
	 */
	public static boolean hasCompressedTableNames(DbxConnection conn, String schemaName)
	throws SQLException
	{
		DatabaseMetaData dbmd = conn.getMetaData();

		ResultSet rsmd = dbmd.getTables(null, schemaName, "%" + DCC_MARKER + "%", new String[] {"TABLE"});
		while(rsmd.next())
		{
			rsmd.close();
			return true;
		}
		rsmd.close();
		return false;
	}

	/**
	 * Get any tables that looks like a Dictionary Compressed Table (contains "$dcc$")
	 * 
	 * @param conn                     The connection used to get DatabaseMetaData
	 * @param schemaName               Tables Schema name (can be null, of schema name isn't used)
	 * @param returnSchemaPrefixed     true=Return both 'schemaName.tableName', false=Return only the 'tableName' 
	 * 
	 * @return a Set with table names which has "$dcc$" in the name (but the "$dcc$ColName" is removed)
	 */
	public static Set<String> getCompressedTableNames(DbxConnection conn, String schemaName, boolean returnSchemaPrefixed)
	throws SQLException
	{
		Set<String> tables = new LinkedHashSet<>();
		
		DatabaseMetaData dbmd = conn.getMetaData();

		ResultSet rsmd = dbmd.getTables(null, schemaName, "%" + DCC_MARKER + "%", new String[] {"TABLE"});
		while(rsmd.next())
		{
			String schName = rsmd.getString("TABLE_SCHEM");
			String tabName = rsmd.getString("TABLE_NAME");

			int pos = tabName.indexOf(DCC_MARKER);
			if (pos == -1)
				continue;

			// Strip of: $dcc$ColName and just leave the table name
			tabName = tabName.substring(0, pos);
			
			// ADD values to the Table Set 
			if (returnSchemaPrefixed)
			{
				if (StringUtil.hasValue(schName))
					schName = schName + ".";

				tables.add(schName + tabName);
			}
			else
			{
				tables.add(tabName);
			}
		}
		rsmd.close();
		
		return tables;
	}

	/**
	 * Rewrite/resolve a column in a select statement into doing a "in-line sub select" 
	 * <pre>
	 * if colName ends with '$dcc$'
	 *     rewrite from: 'xxx$dcc$' -->> '(select [colVal] from [tabName$dcc$xxx] where [hashId] = [xxx$dcc$]) AS [xxx]'
	 * else
	 *     return [colName]
	 * </pre>
	 * Note: Replace the [] with DBMS Specific Quoted Char(s)<br> 
	 * which can be done with: <code>DbxConnection conn.quotifySqlString(sql);</code>
	 * <p>
	 * 
	 * @param cmName
	 * @param colName
	 * @return
	 */
	public static String getRewriteForColumnName(String cmName, String colName)
	{
		if (cmName  == null) return null;
		if (colName == null) return null;
		
		if (colName.endsWith(DictCompression.DCC_MARKER))
		{
			String strippedColName = colName.substring(0, colName.length() - DictCompression.DCC_MARKER.length());
			colName = "(select [colVal] from [" + cmName + DCC_MARKER + strippedColName + "] where [hashId] = [" + colName + "]) AS [" + strippedColName + "]";
		}
		else
		{
			colName = "[" + colName + "]";
		}
		return colName;
		
	}

	/**
	 * Get column names from DatabaseMetaData, put the column names in a <code>LinkedHashMap&lt;originColName, rewriteColName&gt;</code><br>
	 * The Rewritten column names are done via method: {@link #getRewriteForColumnName(String, String)}
	 * 
	 * @param conn         The connection used to get DatabaseMetaData
	 * @param schemaName   Tables Schema name (can be null, of schema name isn't used)
	 * @param tabName      Table name to get all columns from
	 * @return The map described above
	 * 
	 * @throws SQLException In case of issues when calling <code>DatabaseMetaData dbmd.getColumns(null, schemaName, tabName, "%");</code>
	 */
	public static LinkedHashMap<String, String> getRewriteForColumnNames(DbxConnection conn, String schemaName, String tabName)
	throws SQLException
	{
		LinkedHashMap<String, String> map = new LinkedHashMap<>();
		
		DatabaseMetaData dbmd = conn.getMetaData();

		ResultSet rsmd = dbmd.getColumns(null, schemaName, tabName, "%");
		while(rsmd.next())
		{
			String originColName  = rsmd.getString("COLUMN_NAME");
			String rewriteColName = getRewriteForColumnName(tabName, originColName);

			map.put(originColName, rewriteColName);
		}
		rsmd.close();
		
		return map;
	}

	/**
	 * Get a "column list" for a select statement with rewritten column names as done with getRewriteForColumnName(tabName, colName)
	 * 
	 * @param conn         The connection used to get DatabaseMetaData
	 * @param schemaName   Tables Schema name (can be null, of schema name isn't used)
	 * @param tabName      Table name to get all columns from
	 * @return The "column list" described above
	 * 
	 * @throws SQLException In case of issues when calling <code>DatabaseMetaData dbmd.getColumns(null, schemaName, tabName, "%");</code>
	 */
	public static String getRewriteForSelectColumnList(DbxConnection conn, String schemaName, String tabName)
	throws SQLException
	{
		StringBuilder sb = new StringBuilder();

		LinkedHashMap<String, String> map = DictCompression.getRewriteForColumnNames(conn, schemaName, tabName);
		for (String colStr : map.values())
		{
			if (sb.length() > 0)
				sb.append(", ");
			sb.append(colStr);
		}
		return sb.toString();
	}

	public static String getValueForHashId(DbxConnection conn, String schemaName, String tabName, String colName, String hashId)
	throws SQLException
	{
		String qSchName = "";
		if (StringUtil.hasValue(schemaName))
			qSchName = "[" + schemaName + "].";
		String qTabName = "[" + tabName + DCC_MARKER + colName + "]";
		
		String sql = conn.quotifySqlString("select [colVal] from " + qSchName + qTabName + " where [hashId] = '" + hashId + "'");
		
		String result = null;
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while (rs.next())
			{
				result = rs.getString(1);
			}
		}
		return result;
	}
	
	/** Clear the cache */
	public void clear()
	{
		printStatistics(true);

		// Simply clear the "cache"
		_valExistsMap = new HashMap<>();		
	}

	/** Close this */
	public void close()
	{
		clear();
		
		// And potentially some more stuff...
	}
	
	public int populateCacheForTable(DbxConnection conn, String schemaName, String cmName, String colName)
	throws SQLException
	{
		String tabName = cmName + DCC_MARKER + colName;

		String qSchName = "";
		if (StringUtil.hasValue(schemaName))
			qSchName = "[" + schemaName + "].";
		String qTabName = "[" + cmName + DCC_MARKER + colName + "]";

		// Check that the table exists
		if ( DbUtils.checkIfTableExistsNoThrow(conn, null, schemaName, tabName) )
		{
			// Populate the cache with existing values
			String sql = "select [hashId] from " + qSchName + qTabName;
			sql = conn.quotifySqlString(sql);

			int cnt = 0;
			int dupCnt = 0;
			try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
			{
				while (rs.next())
				{
					cnt++;
					String hashId = rs.getString(1);

					boolean newEntry = add(cmName, colName, hashId);
					if ( ! newEntry )
						dupCnt++;
				}
			}
			
			// SHould we check the Data Type for 'hashId'... If we have changed the "digest method", then the data type may need a change...

			_logger.debug("Dictionary Compression: Fetched " + cnt + " (dupCnt=" + dupCnt + ") Hash ID's from table " + qSchName + qTabName + " for column '" + colName + "'.");
			
			return cnt;
		}
		
		return -1;
	}

	/**
	 * Create underlying storage for the Dictionary Compression<br>
	 * If it already exists it should not be created (instead the _valExistsMap is populated)
	 * 
	 * @param schemaName  null if no schema is used
	 * @param cmName      Name of the CM or the Table name that will be used
	 * @param colName     Name of the column
	 * @param jdbcType    Origin JDBC Data type
	 * @param length      Length of the JDBC Data type
	 * 
	 * @return 
	 * <ul>
	 *   <li>null if table already exists</li>
	 *   <li>if (createTable==false): The DDL text for how to create table</li>
	 *   <li>otherwise: DBMS Data Type FOR the reference column FROM the base table that will be used (example: for MD5: <code>char(32)</code> or for SHA1: <code>char(40)</code> or for SHS256: <code>char(64)</code>)</li>
	 * </ul>
	 */
//	public String createTable(String schemaName, String cmName, String colName, int jdbcSqlType, int length, boolean createTable)
//	throws SQLException
//	{
//		if (_connProvider == null)
//			throw new RuntimeException("Sorry, the instance hasn't been created with a ConnectionProvider");
//		
//		DbxConnection conn = _connProvider.getConnection();
//		return createTable(conn, schemaName, cmName, colName, jdbcSqlType, length, createTable);
//	}

	public String createTable(DbxConnection conn, String schemaName, String cmName, String colName, int jdbcSqlType, int length, boolean createTable)
	throws SQLException
	{
		String tabName = cmName + DCC_MARKER + colName;

		String qSchName = "";
		if (StringUtil.hasValue(schemaName))
			qSchName = "[" + schemaName + "].";
		String qTabName = "[" + cmName + DCC_MARKER + colName + "]";

		// Check that the table exists
		if ( DbUtils.checkIfTableExistsNoThrow(conn, null, schemaName, tabName) )
		{
			// Populate the cache with existing values
			String sql = "select [hashId] from " + qSchName + qTabName;
			sql = conn.quotifySqlString(sql);

			int cnt = 0;
			int dupCnt = 0;
			try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
			{
				while (rs.next())
				{
					cnt++;
					String hashId = rs.getString(1);

					boolean newEntry = add(cmName, colName, hashId);
					if ( ! newEntry )
						dupCnt++;
				}
			}
			
			// SHould we check the Data Type for 'hashId'... If we have changed the "digest method", then the data type may need a change...

			_logger.debug("Dictionary Compression: Fetched " + cnt + " (dupCnt=" + dupCnt + ") Hash ID's from table " + qSchName + qTabName + " for column '" + colName + "'.");

			return null;
		}
		
		// Get DBMS data types for: hashId & colVal
		String hashIdDataType = conn.getDbmsDataTypeResolver().dataTypeResolverToTarget(getDigestJdbcType(), getDigestLength(), 0);
		String colValDataType = conn.getDbmsDataTypeResolver().dataTypeResolverToTarget(jdbcSqlType, length, 0);

		// SQL
		String sql = "create table " + qSchName + qTabName + "([hashId] " + hashIdDataType + " not null, [colVal] " + colValDataType + " null, primary key([hashId]))";

		// Translate [] into DBMS Specific Quoted chars
		sql = conn.quotifySqlString(sql);
		
		// Create the table
		if (createTable)
		{
			try (Statement stmnt = conn.createStatement())
			{
				_logger.info("Dictionary Compression: Creating table " + qSchName + qTabName + " for column '" + colName + "'.");
				stmnt.executeUpdate(sql);
				
				// Clear the "in-memory" cache for CmName/ColName
				Set<String> hashIdSet = getHashIdSet(cmName, colName);
				if (hashIdSet.size() > 0)
				{
					_logger.info("Clearing digest set/values for table '" + cmName + "' and column '" + colName + "', which had " + hashIdSet.size() + " entries.");
					hashIdSet.clear();
				}
			}
			
			return hashIdDataType;
		}
		else
		{
			return sql;
		}
	}

	/**
	 * Save a Column Value... a digest string will be returned (save in the DBMS will only be done if it's a NEW column value)
	 * 
	 * @param conn       The DBMS Connection used when string value 
	 * @param cmName     The Counter Model (or table prefix) that will be used when saving 
	 * @param colName    The the Column Name this is for
	 * @param colVal     The String value to Normalize (and store in DBMS)
	 * 
	 * @return a HashId (HEX string of ## chars, depending on the Digest Type used when it was created)<br>
	 *         if 'colVal' was null, NULL will be returned<br>
	 *         <b>example</b>: 73e1f2d8c1a9f25224385ff6eab38031
	 */
//	public String store(String cmName, String colName, String colVal)
//	throws SQLException
//	{
//		if (_connProvider == null)
//			throw new RuntimeException("Sorry, the instance hasn't been created with a ConnectionProvider");
//		
//		DbxConnection conn = _connProvider.getConnection();
//		return store(conn, cmName, colName, colVal);
//	}
	public String store(DbxConnection conn, String cmName, String colName, String colVal)
	throws SQLException
	{
		if (colVal == null)
			return null;

		String hashId = getDigest(colVal);
		if (hashId == null)
			throw new SQLException("hashId was NULL, which wasn't expected. cmName='" + cmName + "', colName='" + colName + "', colVal: " + colVal);

		// exit early: If it already exist in the cache, no need to add it to the storage 
		if (exists(cmName, colName, hashId, colVal))
			return hashId;

		// Add the entry to the storage
		String tabName = "[" + cmName + DCC_MARKER + colName + "]";
		String sql = "insert into " + tabName + " ([hashId], [colVal]) values(?, ?)";
		
		// Translate [] into DBMS Specific Quoted chars
		sql = conn.quotifySqlString(sql);

		try (PreparedStatement pstmnt = conn.prepareStatement(sql))
		{
			pstmnt.setString(1, hashId);
			pstmnt.setString(2, colVal);

			// Add it to the "cache" (done before the executeUpdate() so even if it fail then we still have added it as EXISTSING)  
			add(cmName, colName, hashId);

			// Make the insert
			pstmnt.executeUpdate();
		}

		// Only done periodically
		printStatistics();
		
		return hashId;
	}

	/**
	 * Add a value to a "batch" that will be stored in the DBMS later on (when calling storeBatch())
	 * 
	 * @param conn       The DBMS Connection used when string value 
	 * @param cmName     The Counter Model (or table prefix) that will be used when saving 
	 * @param colName    The the Column Name this is for
	 * @param colVal     The String value to Normalize (and store in DBMS)
	 * 
	 * @return a HashId (HEX string of ## chars, depending on the Digest Type used when it was created)<br>
	 *         if 'colVal' was null, NULL will be returned<br>
	 *         <b>example</b>: 73e1f2d8c1a9f25224385ff6eab38031
	 * @throws SQLException
	 */
	public String addToBatch(DbxConnection conn, String cmName, String colName, String colVal)
	throws SQLException
	{
		if (colVal == null)
			return null;

		String hashId = getDigest(colVal);
		if (hashId == null)
			throw new SQLException("hashId was NULL, which wasn't expected. cmName='" + cmName + "', colName='" + colName + "', colVal: " + colVal);

		// exit early: If it already exist in the cache, no need to add it to the storage 
		if (exists(cmName, colName, hashId, colVal))
			return hashId;

		// Now add the hashId to the "exists" or "cache" structure (it will be persisted later, on storeBatch()
		add(cmName, colName, hashId);


		// Add the entry to the Queue
		if (_batchQueue == null)
			_batchQueue = new HashMap<>();

		// save the colVal in structure
		Map<String, Map<String, String>> colMap = _batchQueue.get(cmName);
		if (colMap == null)
		{
			colMap = new HashMap<>();
			_batchQueue.put(cmName, colMap);
		}

		Map<String, String> hashIdMap = colMap.get(colName);
		if (hashIdMap == null)
		{
			hashIdMap = new HashMap<>();
			colMap.put(colName, hashIdMap);
		}

		// Finally put KEY/VALUE in Map
		hashIdMap.put(hashId, colVal);

		// Only done periodically
		printStatistics();

		return hashId;
	}
	
	/** If we use "batch" methods, store the information in a structure before we execute */        
//	private Map<String, String> _batchQueue; // <hashId, value>
	//          tabName     colName     hashId  colVal
	private Map<String, Map<String, Map<String, String>>> _batchQueue; //<tabName, <colName, <hashId, colVal>>>

	/**
	 * Values that previously was added with 'addToBatch(...)' will be persisted to the DBMS
	 * 
	 * @param conn       The DBMS Connection 
	 * @return           Number of records saved in the DBMS
	 * @throws SQLException
	 */
	public int storeBatch(DbxConnection conn)
	throws SQLException
	{
		if (_batchQueue == null)   return 0;
		if (_batchQueue.isEmpty()) return 0;

		int rows = 0;

		// Loop cmName(s)
		for (String cmName : _batchQueue.keySet())
		{
			// Loop colName(s)
			Map<String, Map<String, String>> colMap = _batchQueue.get(cmName);
			for (String colName : colMap.keySet())
			{
				// Add the entry to the storage
				String tabName = "[" + cmName + DCC_MARKER + colName + "]";
				String sql = "insert into " + tabName + " ([hashId], [colVal]) values(?, ?)";
				
				// Translate [] into DBMS Specific Quoted chars
				sql = conn.quotifySqlString(sql);

				Map<String, String> hashIdMap = colMap.get(colName);

				// Loop hashId, colVal
				// ADD to SQL Batch
				try
				{
					PreparedStatement pstmnt = conn.prepareStatement(sql);

					for (Entry<String, String> hashEntry : hashIdMap.entrySet())
					{
						String hashId = hashEntry.getKey();
						String colVal = hashEntry.getValue();

						pstmnt.setString(1, hashId);
						pstmnt.setString(2, colVal);
	//System.out.println("DictCompression.storeBatch(): SQL=" + sql + " --->>>> hash='" + hashId + "', val='" + colVal + "'");

						pstmnt.addBatch();
						rows++;
					}
					
					// SAVE/EXECUTE - SQL Batch
					pstmnt.executeBatch();
					pstmnt.close();
				}
				catch (SQLException ex)
				{
					_logger.error("Problems adding values to Dictionary Compressed table " + tabName + ". Clearing this batch and continuing with next table. Cleared size=" + hashIdMap.size() + ", Entries=" + hashIdMap);
					hashIdMap.clear();
				}
			}
		}
		
		// Clear the old structure, a new one will be allocated bu add(...)
		_batchQueue = null;

		return rows;
	}

	/** get a digest for the column value, the returned string length depends on the Digest type used when instantiating this instance */
	private String getDigest(String colVal)
	{
		String hashId = null;
		switch (_digestType)
		{
		case MD5:
			hashId = DigestUtils.md5Hex(colVal);
			break;

		case SHA1:
			hashId = DigestUtils.sha1Hex(colVal);
			break;

		case SHA256:
			hashId = DigestUtils.sha256Hex(colVal);
			break;
		}
		return hashId;
	}
	
	/** 
	 * check if the HashId already exists in the "cache"
	 *  
	 * @param cmName      cm or table name
	 * @param colName     column name
	 * @param hashId      hash id to check if it exists
	 * @param colVal      raw data. this is only used for statistics
	 */
	private boolean exists(String cmName, String colName, String hashId, String colVal)
	{
		boolean exists = getHashIdSet(cmName, colName).contains(hashId);

		_stat_lookupCount++;

		if (exists)
		{
			_stat_reuseCount++;
			if (colVal != null)
				_stat_bytesSaved += colVal.length();
		}
		else
		{
			_stat_addCount++;
			if (colVal != null)
				_stat_bytesAdded += colVal.length();
		}
			
		return exists;
	}

	/** check the HashId to the "cache" */
	private boolean add(String cmName, String colName, String hashId)
	{
		return getHashIdSet(cmName, colName).add(hashId);
	}

	/** helper method to get the HashId SET */
	private Set<String> getHashIdSet(String cmName, String colName)
	{
		Map<String, Set<String>> colMap = _valExistsMap.get(cmName);
		if (colMap == null)
		{
			colMap = new HashMap<>();
			_valExistsMap.put(cmName, colMap);
		}

		Set<String> hashIdSet = colMap.get(colName);
		if (hashIdSet == null)
		{
			hashIdSet = new HashSet<>();
			colMap.put(colName, hashIdSet);
		}

		return hashIdSet;
	}
	
	
//	public String storeSha256(String cmName, String colName, String colVal)
//	throws SQLException
//	{
////		String xxx = DigestUtils.sha256Hex("some String");
////		System.out.println("XXXXXXXXXXXXXXX="+xxx);
//		// 64Bytes=73e1f2d8c1a9f25224385ff6eab3803194e4e525ea19d5f6e2f69bede14d4416
//		// 64Bytes=73e1f2d8c1a9f25224385ff6eab3803194e4e525ea19d5f6e2f69bede14d4416
//
//		String hashId = DigestUtils.sha256Hex(colVal);
//		return hashId;
//	}
//
//	public String storeSha1(String cmName, String colName, String colVal)
//	throws SQLException
//	{
//		String hashId = DigestUtils.sha1Hex(colVal);
//		return hashId;
//	}
//
//	public String storeMd5(String cmName, String colName, String colVal)
//	throws SQLException
//	{
//		String hashId = DigestUtils.md5Hex(colVal);
//		return hashId;
//	}
//
//	public String getDbmsDataTypeForSha256() { return "char(64)"; }
//	public String getDbmsDataTypeForSha1()   { return "char(40)"; }
//	public String getDbmsDataTypeForMd5()    { return "char(32)"; }
	
	public static void main(String[] args)
	{
		String someStr = "some String";
		String md5    = DigestUtils.md5Hex   (someStr);
		String sha1   = DigestUtils.sha1Hex  (someStr);
		String sha256 = DigestUtils.sha256Hex(someStr);

		System.out.println("MD5     length=" + md5   .length() + ", value='" + someStr + "', digest='" + md5    + "'.");
		System.out.println("SHA-1   length=" + sha1  .length() + ", value='" + someStr + "', digest='" + sha1   + "'.");
		System.out.println("SHA-256 length=" + sha256.length() + ", value='" + someStr + "', digest='" + sha256 + "'.");
	}
}
