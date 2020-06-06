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
package com.asetune.pcs;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;

import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.ConnectionProvider;
import com.asetune.utils.DbUtils;
import com.asetune.utils.StringUtil;

public class DictCompression
{
	private static Logger _logger = Logger.getLogger(DictCompression.class);

	public enum DigestType
	{
		MD5,
		SHA1, 
		SHA256
	};

	private static int getDigestLength(DigestType digestType)
	{
		switch (digestType)
		{
		case MD5:    return 32;
		case SHA1:   return 40;
		case SHA256: return 64;
		}
		throw new RuntimeException("DigestType '" + digestType + "' is unknown.");
	}
	
	/**
	 * Hold a entry of existing hashId's that has already been persisted
	 * The KEY in the Map is the CmName/BaseTable name
	 */
	private Map<String, Map<String, Set<String>>> _valExistsMap = new HashMap<>();
	//          tabName     colName     HashId

	// Where we get a connection from
	private ConnectionProvider _connProvider;
	private DigestType _digestType;

	private static final String DCC_MARKER = "$dcc$";

	
	public DictCompression(ConnectionProvider connProvider)
	{
		this(connProvider, DigestType.MD5);
	}

	public DictCompression(ConnectionProvider connProvider, DigestType digestType)
	{
		_connProvider = connProvider;
		_digestType   = digestType;
	}


//	/** Called when a connection is made */
//	public void onConnect()
//	{
//	}

	/** Close this */
	public void close()
	{
		// Simply clear the "cache"
		_valExistsMap = new HashMap<>();		
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
	 * @return null if table already exists, otherwise: DBMS Data Type FOR the reference column FROM the base table that will be used
	 */
	public String createTable(String schemaName, String cmName, String colName, int jdbcSqlType, int length)
	throws SQLException
	{
		DbxConnection conn = _connProvider.getConnection();

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
			try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
			{
				while (rs.next())
				{
					cnt++;
					String hashId = rs.getString(1);
					
					add(cmName, colName, hashId);
				}
			}
			
			// SHould we check the Data Type for 'hashId'... If we have changed the "digest method", then the data type may need a change...
			
			_logger.info("Fetched " + cnt + " Hash ID's from table: " + qSchName + qTabName );
			
			return null;
		}
		
		// Get DBMS data types for: hashId & colVal
		String hashIdDataType = conn.getDbmsDataTypeResolver().dataTypeResolverToTarget(Types.CHAR, getDigestLength(_digestType), 0);
		String colValDataType = conn.getDbmsDataTypeResolver().dataTypeResolverToTarget(jdbcSqlType, length, 0);

		// SQL
		String sql = "create table " + qSchName + qTabName + "([hashId] " + hashIdDataType + " not null, [colVal] " + colValDataType + " null, primary key([hashId]))";

		// Translate [] into DBMS Specific Quoted chars
		sql = conn.quotifySqlString(sql);

		// Create the table
		try (Statement stmnt = conn.createStatement())
		{
			stmnt.executeUpdate(sql);
		}
		
		return hashIdDataType;
	}

	/**
	 * 
	 * @param cmName
	 * @param colName
	 * @param colVal
	 * 
	 * @return a HashId (HEX string of 64 chars), example: 73e1f2d8c1a9f25224385ff6eab3803194e4e525ea19d5f6e2f69bede14d4416
	 */
	public String store(String cmName, String colName, String colVal)
	throws SQLException
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
		
		if (hashId == null)
			throw new SQLException("hashId was NULL, which wasn't expected. cmName='" + cmName + "', colName='" + colName + "', colVal: " + colVal);

		// exit early: If it already exist in the cache, no need to add it to the storage 
		if (exists(cmName, colName, hashId))
			return hashId;

		// Add the entry to the storage
		String tabName = "[" + cmName + DCC_MARKER + colName + "]";
		String sql = "insert into " + tabName + " ([hashId], [colVal]) values(?, ?)";
		
		DbxConnection conn = _connProvider.getConnection();
		
		// Translate [] into DBMS Specific Quoted chars
		sql = conn.quotifySqlString(sql);

		try (PreparedStatement pstmnt = conn.prepareStatement(sql))
		{
			pstmnt.setString(1, hashId);
			pstmnt.setString(2, colVal);

			pstmnt.executeUpdate();
			
			// Add it to the "cache"
			add(cmName, colName, hashId);
		}

		return hashId;
	}
	
	private boolean exists(String cmName, String colName, String hashId)
	{
		return getHashIdSet(cmName, colName).contains(hashId);
	}

	private boolean add(String cmName, String colName, String hashId)
	{
		return getHashIdSet(cmName, colName).add(hashId);
	}

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
