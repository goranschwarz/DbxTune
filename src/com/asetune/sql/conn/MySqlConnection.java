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
package com.asetune.sql.conn;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.asetune.sql.conn.info.DbxConnectionStateInfo;
import com.asetune.sql.conn.info.DbxConnectionStateInfoGenericJdbc;
import com.asetune.ui.autocomplete.completions.TableExtraInfo;
import com.asetune.utils.StringUtil;
import com.asetune.utils.Ver;

public class MySqlConnection extends DbxConnection
{
	private static Logger _logger = Logger.getLogger(MySqlConnection.class);

	public MySqlConnection(Connection conn)
	{
		super(conn);
		Ver.majorVersion_mustBeTenOrAbove = true;
//System.out.println("constructor::MySqlConnection(conn): conn="+conn);
	}

	@Override
	public DbxConnectionStateInfo refreshConnectionStateInfo()
	{
		DbxConnectionStateInfo csi = new DbxConnectionStateInfoGenericJdbc(this);
		setConnectionStateInfo(csi);
		return csi;
	}

	@Override
	public boolean isInTransaction() throws SQLException
	{
		return false;
		// The below might work, but only for INNODB
		// but unfortunate for a normal user you get:
		//       MySQL: ErrorCode 1227, SQLState 42000, ExceptionClass: com.mysql.jdbc.exceptions.jdbc4.MySQLSyntaxErrorException
		//       Access denied; you need (at least one of) the PROCESS privilege(s) for this operation
		// SELECT TRX_ID FROM INFORMATION_SCHEMA.INNODB_TRX WHERE TRX_MYSQL_THREAD_ID = CONNECTION_ID();
	}

	/**
	 * If the server handles databases like MS SQL_Server and Sybase ASE
	 * @return true or false
	 */
	@Override
	public boolean isDatabaseAware()
	{
		return true;
	}

	@Override
	public Map<String, TableExtraInfo> getTableExtraInfo(String cat, String schema, String table)
	{
		LinkedHashMap<String, TableExtraInfo> extraInfo = new LinkedHashMap<>();

		String sql = 
			"SELECT  TABLE_ROWS      \n" +
			"       ,ENGINE          \n" + // MEMORY, InnoDB, MyISAM, null
			"       ,DATA_LENGTH     \n" + // For MyISAM, DATA_LENGTH is the length of the data file, in bytes.
			                               // For InnoDB, DATA_LENGTH is the approximate amount of memory allocated for the clustered index, in bytes. Specifically, it is the clustered index size, in pages, multiplied by the InnoDB page size.
			"       ,INDEX_LENGTH    \n" + // For MyISAM, INDEX_LENGTH is the length of the index file, in bytes.
			                               // For InnoDB, INDEX_LENGTH is the approximate amount of memory allocated for non-clustered indexes, in bytes. Specifically, it is the sum of non-clustered index sizes, in pages, multiplied by the InnoDB page size.
			"       ,DATA_FREE       \n" + // The number of allocated but unused bytes. 
			"       ,AVG_ROW_LENGTH  \n" + 
			"       ,AUTO_INCREMENT  \n" + // The next AUTO_INCREMENT value. 
			"       ,CREATE_TIME     \n" + // When the table was created.
			"       ,UPDATE_TIME     \n" + // When the data file was last updated.
			"       ,TABLE_COLLATION \n" + // When the data file was last updated.
			"FROM information_schema.TABLES \n" + 
			"WHERE TABLE_SCHEMA = '" + schema + "'" +
			"  AND TABLE_NAME   = '" + table + "'"
			;

		try
		{
			Statement stmnt = _conn.createStatement();
			ResultSet rs = stmnt.executeQuery(sql);
			while(rs.next())
			{
				long   TABLE_ROWS      = rs.getLong  ("TABLE_ROWS");
				String ENGINE          = rs.getString("ENGINE");
				long   DATA_LENGTH     = rs.getLong  ("DATA_LENGTH")  / 1024;
				long   INDEX_LENGTH    = rs.getLong  ("INDEX_LENGTH") / 1024;
				long   DATA_FREE       = rs.getLong  ("DATA_FREE")    / 1024;
				long   AVG_ROW_LENGTH  = rs.getLong  ("AVG_ROW_LENGTH");
				long   AUTO_INCREMENT  = rs.getLong  ("AUTO_INCREMENT");
				String CREATE_TIME     = rs.getString("CREATE_TIME");
				String UPDATE_TIME     = rs.getString("UPDATE_TIME");
				String TABLE_COLLATION = rs.getString("TABLE_COLLATION");

				extraInfo.put(TableExtraInfo.TableRowCount,       new TableExtraInfo(TableExtraInfo.TableRowCount,       "Row Count",          TABLE_ROWS,               "Number of rows in the table. Note: fetched from TABLES.TABLE_ROWS", null));
				extraInfo.put(TableExtraInfo.TableTotalSizeInKb,  new TableExtraInfo(TableExtraInfo.TableTotalSizeInKb,  "Total Size KB",      DATA_LENGTH+INDEX_LENGTH, "Table size (data + indexes). Note: fetched from TABLES.TABLE_ROWS+INDEX_LENGTH", null));
				extraInfo.put(TableExtraInfo.TableDataSizeInKb,   new TableExtraInfo(TableExtraInfo.TableDataSizeInKb,   "Data Size KB",       DATA_LENGTH,              "Data Size. Note: fetched from TABLES.DATA_LENGTH", null));
				extraInfo.put(TableExtraInfo.TableIndexSizeInKb,  new TableExtraInfo(TableExtraInfo.TableIndexSizeInKb,  "Index Size KB",      INDEX_LENGTH,             "Index size. fetched from TABLES.INDEX_LENGTH", null));

				extraInfo.put("Data Free",                        new TableExtraInfo("Data Free",                        "Data Free KB",       DATA_FREE,                "Free KB. TABLES.DATA_FREE", null));
				extraInfo.put("Avg Row Length",                   new TableExtraInfo("Avg Row Length",                   "Avg Row Length",     AVG_ROW_LENGTH,           "Average Row length in bytes. TABLES.AVG_ROW_LENGTH", null));
				
				extraInfo.put("Engine",                           new TableExtraInfo("Engine",                           "Engine",             ENGINE,                   "Storage engine", null));
				extraInfo.put("Collation",                        new TableExtraInfo("Collation",                        "Collation",          TABLE_COLLATION,          "Table Collation", null));
				extraInfo.put("Next AutoIncrement",               new TableExtraInfo("Next AutoIncrement",               "Next AutoIncrement", AUTO_INCREMENT,           "Free KB. TABLES.AUTO_INCREMENT", null));
				extraInfo.put("Create Time",                      new TableExtraInfo("Create Time",                      "Create Time",        CREATE_TIME,              "Table Creation Time. TABLES.CREATE_TIME", null));
				extraInfo.put("Last Update Time",                 new TableExtraInfo("Last Update Time",                 "Last Update Time",   UPDATE_TIME,              "Last time data was touched. TABLES.UPDATE_TIME", null));
			}
			rs.close();
		}
		catch (SQLException ex)
		{
			_logger.error("getTableExtraInfo(): Problems executing sql '"+sql+"'. Caught="+ex);
			if (_logger.isDebugEnabled())
				_logger.debug("getTableExtraInfo(): Problems executing sql '"+sql+"'. Caught="+ex, ex);
		}
		
		return extraInfo;
	}

	@Override
	public long getRowCountEstimate(String catalog, String schema, String table)
	throws SQLException
	{
		long rowCount = -1;
		
//		String whereCat = StringUtil.isNullOrBlank(catalog) ? "" : "  and upper(TABLE_CATALOG) = upper('" + catalog + "') \n";
//		String whereSch = StringUtil.isNullOrBlank(schema)  ? "" : "  and upper(TABLE_SCHEMA)  = upper('" + schema  + "') \n";
//		String whereTab = StringUtil.isNullOrBlank(table)   ? "" : "  and upper(TABLE_NAME)    = upper('" + table   + "') \n";
		String whereCat = StringUtil.isNullOrBlank(catalog) ? "" : "  and TABLE_SCHEMA  = '" + catalog + "' \n";
		String whereSch = StringUtil.isNullOrBlank(schema)  ? "" : "  and TABLE_SCHEMA  = '" + schema  + "' \n";
		String whereTab = StringUtil.isNullOrBlank(table)   ? "" : "  and TABLE_NAME    = '" + table   + "' \n";
		
		String sql = "select TABLE_ROWS \n"
				+ "from information_schema.TABLES \n"
				+ "where 1 = 1 \n"
				+ whereCat
				+ whereSch
				+ whereTab
				+ "";
		
		try (Statement stmnt = this.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while(rs.next())
				rowCount = rs.getLong(1);
		}
		
		return rowCount;
	}

	@Override
	public Map<String, Object> getDbmsExtraInfo()
	{
//		String sql = "SHOW GLOBAL VARIABLES where Variable_name in ('server_id', 'server_uuid', 'character_set_server', 'collation_server')";
		String sql = "SHOW GLOBAL VARIABLES where Variable_name in ('server_id', 'server_uuid')";
		LinkedHashMap<String, Object> map = new LinkedHashMap<>();
		
		try (Statement stmnt = _conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql) )
		{
			while (rs.next())
			{
				String key = rs.getString(1);
				String val = rs.getString(2);
				
				map.put(key, val);
			}
		}
		catch (SQLException ex)
		{
			_logger.error("getDbmsExtraInfo(): Problems executing sql '"+sql+"'. Caught="+ex);
			if (_logger.isDebugEnabled())
				_logger.debug("getDbmsExtraInfo(): Problems executing sql '"+sql+"'. Caught="+ex, ex);
		}


		sql = "SHOW SLAVE HOSTS";
		// RS> Col# Label      JDBC Type Name         Guessed DBMS type Source Table
		// RS> ---- ---------- ---------------------- ----------------- ------------
		// RS> 1    Server_id  java.sql.Types.INTEGER INT UNSIGNED      -none-      
		// RS> 2    Host       java.sql.Types.VARCHAR VARCHAR(20)       -none-      
		// RS> 3    Port       java.sql.Types.INTEGER INT UNSIGNED      -none-      
		// RS> 4    Master_id  java.sql.Types.INTEGER INT UNSIGNED      -none-      
		// RS> 5    Slave_UUID java.sql.Types.VARCHAR VARCHAR(36)       -none-      
		
		try (Statement stmnt = _conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql) )
		{
			int row = 0;
			while (rs.next())
			{
				row++;
				String key        = "<b>has-slave-"+row+"</b>";
				
				String Server_id  = rs.getString(1);
				String Host       = rs.getString(2);
				String Port       = rs.getString(3);
				String Master_id  = rs.getString(4);
				String Slave_UUID = rs.getString(5);
				
				String val = "</b>MasterId=<b>"+Master_id+"</b>, SlaveHost=<b>"+Host+"</b>, SlavePort=<b>"+Port+"</b>, SlaveId=<b>"+Server_id+"</b>, SlaveUUID=<b>"+Slave_UUID+"</b>";
				
				map.put(key, val);
			}
		}
		catch (SQLException ex)
		{
			_logger.error("getDbmsExtraInfo(): Problems executing sql '"+sql+"'. Caught="+ex);
			if (_logger.isDebugEnabled())
				_logger.debug("getDbmsExtraInfo(): Problems executing sql '"+sql+"'. Caught="+ex, ex);
		}



		sql = "SHOW SLAVE STATUS";
		// RS> Col# Label                         JDBC Type Name         Guessed DBMS type Source Table
		// RS> ---- ----------------------------- ---------------------- ----------------- ------------
		// RS> 1    Slave_IO_State                java.sql.Types.VARCHAR VARCHAR(14)       -none-      
		// RS> 2    Master_Host                   java.sql.Types.VARCHAR VARCHAR(61)       -none-      
		// RS> 3    Master_User                   java.sql.Types.VARCHAR VARCHAR(97)       -none-      
		// RS> 4    Master_Port                   java.sql.Types.INTEGER INT UNSIGNED      -none-      
		// RS> 5    Connect_Retry                 java.sql.Types.INTEGER INT UNSIGNED      -none-      
		// RS> 6    Master_Log_File               java.sql.Types.VARCHAR VARCHAR(512)      -none-      
		// RS> 7    Read_Master_Log_Pos           java.sql.Types.BIGINT  BIGINT UNSIGNED   -none-      
		// RS> 8    Relay_Log_File                java.sql.Types.VARCHAR VARCHAR(512)      -none-      
		// RS> 9    Relay_Log_Pos                 java.sql.Types.BIGINT  BIGINT UNSIGNED   -none-      
		// RS> 10   Relay_Master_Log_File         java.sql.Types.VARCHAR VARCHAR(512)      -none-      
		// RS> 11   Slave_IO_Running              java.sql.Types.VARCHAR VARCHAR(3)        -none-      
		// RS> 12   Slave_SQL_Running             java.sql.Types.VARCHAR VARCHAR(3)        -none-      
		// RS> 13   Replicate_Do_DB               java.sql.Types.VARCHAR VARCHAR(20)       -none-      
		// RS> 14   Replicate_Ignore_DB           java.sql.Types.VARCHAR VARCHAR(20)       -none-      
		// RS> 15   Replicate_Do_Table            java.sql.Types.VARCHAR VARCHAR(20)       -none-      
		// RS> 16   Replicate_Ignore_Table        java.sql.Types.VARCHAR VARCHAR(23)       -none-      
		// RS> 17   Replicate_Wild_Do_Table       java.sql.Types.VARCHAR VARCHAR(24)       -none-      
		// RS> 18   Replicate_Wild_Ignore_Table   java.sql.Types.VARCHAR VARCHAR(28)       -none-      
		// RS> 19   Last_Errno                    java.sql.Types.INTEGER INT UNSIGNED      -none-      
		// RS> 20   Last_Error                    java.sql.Types.VARCHAR VARCHAR(20)       -none-      
		// RS> 21   Skip_Counter                  java.sql.Types.INTEGER INT UNSIGNED      -none-      
		// RS> 22   Exec_Master_Log_Pos           java.sql.Types.BIGINT  BIGINT UNSIGNED   -none-      
		// RS> 23   Relay_Log_Space               java.sql.Types.BIGINT  BIGINT UNSIGNED   -none-      
		// RS> 24   Until_Condition               java.sql.Types.VARCHAR VARCHAR(6)        -none-      
		// RS> 25   Until_Log_File                java.sql.Types.VARCHAR VARCHAR(512)      -none-      
		// RS> 26   Until_Log_Pos                 java.sql.Types.BIGINT  BIGINT UNSIGNED   -none-      
		// RS> 27   Master_SSL_Allowed            java.sql.Types.VARCHAR VARCHAR(7)        -none-      
		// RS> 28   Master_SSL_CA_File            java.sql.Types.VARCHAR VARCHAR(512)      -none-      
		// RS> 29   Master_SSL_CA_Path            java.sql.Types.VARCHAR VARCHAR(512)      -none-      
		// RS> 30   Master_SSL_Cert               java.sql.Types.VARCHAR VARCHAR(512)      -none-      
		// RS> 31   Master_SSL_Cipher             java.sql.Types.VARCHAR VARCHAR(512)      -none-      
		// RS> 32   Master_SSL_Key                java.sql.Types.VARCHAR VARCHAR(512)      -none-      
		// RS> 33   Seconds_Behind_Master         java.sql.Types.BIGINT  BIGINT UNSIGNED   -none-      
		// RS> 34   Master_SSL_Verify_Server_Cert java.sql.Types.VARCHAR VARCHAR(3)        -none-      
		// RS> 35   Last_IO_Errno                 java.sql.Types.INTEGER INT UNSIGNED      -none-      
		// RS> 36   Last_IO_Error                 java.sql.Types.VARCHAR VARCHAR(20)       -none-      
		// RS> 37   Last_SQL_Errno                java.sql.Types.INTEGER INT UNSIGNED      -none-      
		// RS> 38   Last_SQL_Error                java.sql.Types.VARCHAR VARCHAR(20)       -none-      
		// RS> 39   Replicate_Ignore_Server_Ids   java.sql.Types.VARCHAR VARCHAR(512)      -none-      
		// RS> 40   Master_Server_Id              java.sql.Types.INTEGER INT UNSIGNED      -none-      
		// RS> 41   Master_UUID                   java.sql.Types.VARCHAR VARCHAR(36)       -none-      
		// RS> 42   Master_Info_File              java.sql.Types.VARCHAR VARCHAR(1024)     -none-      
		// RS> 43   SQL_Delay                     java.sql.Types.INTEGER INT UNSIGNED      -none-      
		// RS> 44   SQL_Remaining_Delay           java.sql.Types.INTEGER INT UNSIGNED      -none-      
		// RS> 45   Slave_SQL_Running_State       java.sql.Types.VARCHAR VARCHAR(20)       -none-      
		// RS> 46   Master_Retry_Count            java.sql.Types.BIGINT  BIGINT UNSIGNED   -none-      
		// RS> 47   Master_Bind                   java.sql.Types.VARCHAR VARCHAR(61)       -none-      
		// RS> 48   Last_IO_Error_Timestamp       java.sql.Types.VARCHAR VARCHAR(20)       -none-      
		// RS> 49   Last_SQL_Error_Timestamp      java.sql.Types.VARCHAR VARCHAR(20)       -none-      
		// RS> 50   Master_SSL_Crl                java.sql.Types.VARCHAR VARCHAR(512)      -none-      
		// RS> 51   Master_SSL_Crlpath            java.sql.Types.VARCHAR VARCHAR(512)      -none-      
		// RS> 52   Retrieved_Gtid_Set            java.sql.Types.VARCHAR VARCHAR(42)       -none-      
		// RS> 53   Executed_Gtid_Set             java.sql.Types.VARCHAR VARCHAR(42)       -none-      
		// RS> 54   Auto_Position                 java.sql.Types.INTEGER INT UNSIGNED      -none-      
		// RS> 55   Replicate_Rewrite_DB          java.sql.Types.VARCHAR VARCHAR(24)       -none-      
		// RS> 56   Channel_Name                  java.sql.Types.VARCHAR VARCHAR(192)      -none-      
		// RS> 57   Master_TLS_Version            java.sql.Types.VARCHAR VARCHAR(512)      -none-      
		// RS> 58   Master_public_key_path        java.sql.Types.VARCHAR VARCHAR(512)      -none-      
		// RS> 59   Get_master_public_key         java.sql.Types.INTEGER INT UNSIGNED      -none- 
		
		try (Statement stmnt = _conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql) )
		{
			int row = 0;
			while (rs.next())
			{
				row++;
				String key        = "<b>has-master-"+row+"</b>";
				
				String Master_id    = rs.getString("Master_Server_Id");
				String Master_Host  = rs.getString("Master_Host");
				String Master_Port  = rs.getString("Master_Port");
				String Master_UUID  = rs.getString("Master_UUID");
				
				String val = "</b>MasterId=<b>"+Master_id+"</b>, MasterHost=<b>"+Master_Host+"</b>, MasterPort=<b>"+Master_Port+"</b>, MasterUUID=<b>"+Master_UUID+"</b>";
				
				map.put(key, val);
			}
		}
		catch (SQLException ex)
		{
			_logger.error("getDbmsExtraInfo(): Problems executing sql '"+sql+"'. Caught="+ex);
			if (_logger.isDebugEnabled())
				_logger.debug("getDbmsExtraInfo(): Problems executing sql '"+sql+"'. Caught="+ex, ex);
		}

		return map;
	}
	
	
	/**
	 * Get the connected database server/instance name.
	 * @return null if not connected else: Retrieves the name of this database server/instance name.
	 * @see java.sql.DatabaseMetaData.getDatabaseProductName
	 */
	@Override
	public String getDbmsServerName() 
	throws SQLException
	{
		if (_databaseServerName != null)
			return _databaseServerName;
		
		if (_conn == null)
			return null;
		
		String serverName = "";

//		String sql = "select @@hostname as onHostname, @@port as portNum, database()";
		String sql = "select @@hostname as onHostname, @@port as portNum";
		try
		{
			Statement stmt = _conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next())
			{
				String ip     = rs.getString(1).trim();
				String port   = rs.getString(2).trim();
//				String dbname = rs.getString(3).trim();

				String hostname = ip;
				if (isIpAddress(ip))
				{
    				try
    				{
    					InetAddress addr = InetAddress.getByName(ip);
    					hostname = addr.getHostName();
    				}
    				catch(UnknownHostException ex)
    				{
    				}
				}

//				serverName = DbxTune.stripSrvName(hostname + ":" + port);
//				serverName = hostname + ":" + port + "/" + dbname;
				serverName = hostname + ":" + port;
			}
			rs.close();
			stmt.close();
		}
		catch (SQLException ex)
		{
			_logger.warn("Problem getting MySQL DBMS Server Name, setting this to 'unknown'. SQL='"+sql+"', caught: "+ex);
			serverName = "unknown";
		}
		
		_databaseServerName = serverName;
		return serverName;
	}

	public static boolean isIpAddress(String address)
	{
//		String regex = "^((25[0-5])|(((1[0-9]|2[0-4])|[1-9])?[0-9]))(\\.((25[0-5])|(((1[0-9]|2[0-4])|[1-9])?[0-9]))){3}$";
		String regex = "^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$";
		try
		{
			Pattern p = Pattern.compile(regex);
			Matcher m = p.matcher(address);

			return m.find();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		return false;
	}

	@Override
	protected int getDbmsSessionId_impl() throws SQLException
	{
		String sql = "select connection_id()";

		int spid = -1;
		try (Statement stmnt = _conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while(rs.next())
				spid = rs.getInt(1);
		}
		
		return spid;
	}
}
