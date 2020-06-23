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
package com.asetune.cm;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class CounterSampleCatalogIteratorAse 
extends CounterSampleCatalogIterator
{
	private static final long serialVersionUID = 1L;

	// This is used if no databases are in a "valid" state.
	private List<String> _fallbackList = null;
	
	/**
	 * @param name
	 * @param negativeDiffCountersToZero
	 * @param diffColNames
	 * @param prevSample
	 * @param fallbackList a list of database(s) that will be used in case of "no valid" databases can be found, typically usage is "tempdb" to at least get one database.
	 */
	public CounterSampleCatalogIteratorAse(String name, boolean negativeDiffCountersToZero, String[] diffColNames, CounterSample prevSample, List<String> fallbackList)
	{
		super(name, negativeDiffCountersToZero, diffColNames, prevSample);
		_fallbackList = fallbackList;
	}
	
	@Override
	protected List<String> getCatalogList(CountersModel cm, Connection conn)
	throws SQLException
	{
		String sql = 
			  "SELECT name \n"
			+ "FROM master.dbo.sysdatabases \n"
			+ "WHERE name not like 'tempdb%' \n"
			+ "  AND name not in ('master', 'sybsecurity', 'sybsystemdb', 'sybsystemprocs','model', 'tempdb') \n"

			+ "  AND (status  & 32)      != 32      -- ignore: Database created with for load option, or crashed while loading database, instructs recovery not to proceed \n"
			+ "  AND (status  & 64)      != 64      -- ignore: Recovery started for all databases to be recovered (Database suspect, Not recovered, Cannot be opened or used, Can be dropped only with dbcc dbrepair) \n"
			+ "  AND (status  & 1024)    != 1024    -- ignore: read only; can be set by user \n"
			+ "  AND (status  & 2048)    != 2048    -- ignore: dbo use only; can be set by user \n"
			+ "  AND (status  & 4096)    != 4096    -- ignore: single user; can be set by user \n"

			+ "  AND (status2 & 16)      != 16      -- ignore: Database is offline. \n"
			+ "  AND (status2 & 32)      != 32      -- ignore: Database is offline until recovery completes. \n"
			+ "  AND (status2 & 256)     != 256     -- ignore: Table structure written to disk. If this bit appears after recovery completes, server may be under-configured for open databases. Use sp_configure to increase this parameter. \n"
			+ "  AND (status2 & 512)     != 512     -- ignore: Database is in the process of being upgraded. \n"

			+ "  AND (status3 & 2)       != 2       -- ignore: Database is a proxy database created by high availability. \n"
			+ "  AND (status3 & 4)       != 4       -- ignore: Database has a proxy database created by high availability \n"
			+ "  AND (status3 & 8)       != 8       -- ignore: Disallow access to the database, since database is being shut down \n"
			+ "  AND (status3 & 256)     != 256     -- ignore: User-created tempdb. \n"
			+ "  AND (status3 & 4096)    != 4096    -- ignore: Database has been shut down successfully. \n"
			+ "  AND (status3 & 8192)    != 8192    -- ignore: A drop database is in progress. \n"
			+ "  AND (status3 & 4194304) != 4194304 -- ignore: archive databases \n"

			+ "ORDER BY dbid \n"
			+ "";

		ArrayList<String> list = new ArrayList<String>();

		Statement stmnt = conn.createStatement();
		ResultSet rs = stmnt.executeQuery(sql);
		while(rs.next())
		{
			String dbname = rs.getString(1);
			list.add(dbname);
		}
		
		// If the above get **no** databases that are in correct state add the "passed" database(s)
		if (list.isEmpty())
		{
			if (_fallbackList != null)
				list.addAll(_fallbackList);
		}

		return list;
	}
}
// Status Control Bits in the sysdatabases Table
// +----------+---------+--------------------------------------------------------------------------------
// | Decimal  |Hex      | Status
// +----------+---------+--------------------------------------------------------------------------------
// | 1        | 0x01    | Upgrade started on this database
// | 2        | 0x02    | Upgrade has been successful
// | 4        | 0x04    | select into/bulkcopy; can be set by user
// | 8        | 0x08    | trunc log on chkpt; can be set by user
// | 16       | 0x10    | no chkpt on recovery; can be set by user
// | 32       | 0x20    | Database created with for load option, or crashed while loading database, instructs recovery not to proceed
// | 64       | 0x04    | Recovery started for all databases to be recovered
// | 256      | 0x100   | •   Database suspect
// |          |         | •   Not recovered
// |          |         | •   Cannot be opened or used
// |          |         | •   Can be dropped only with dbcc dbrepair
// | 512      | 0x200   | ddl in tran; can be set by user
// | 1024     | 0x400   | read only; can be set by user
// | 2048     | 0x800   | dbo use only; can be set by user
// | 4096     | 0x1000  | single user; can be set by user
// | 8192     | 0x2000  | allow nulls by default; can be set by user
// +----------+---------+--------------------------------------------------------------------------------
// 
// This table lists the bit representations for the status2 column.
// status2 Control Bits in the sysdatabases Table
// +----------+------------+--------------------------------------------------------------------------------
// | Decimal  |Hex         | Status
// +----------+------------+--------------------------------------------------------------------------------
// | 1        | 0x0001     | abort tran on log full; can be set by user
// | 2        | 0x0002     | no free space acctg; can be set by user
// | 4        | 0x0004     | auto identity; can be set by user
// | 8        | 0x0008     | identity in nonunique index; can be set by user
// | 16       | 0x0010     | Database is offline
// | 32       | 0x0020     | Database is offline until recovery completes
// | 64       | 0x0040     | The table has an auto identity feature, and a unique constraint on the identity column
// | 128      | 0x0080     | Database has suspect pages
// | 256      | 0x0100     | Table structure written to disk. If this bit appears after recovery completes, server may be under-configured for open databases. Use sp_configure to increase this parameter.
// | 512      | 0x0200     | Database is in the process of being upgraded
// | 1024     | 0x0400     | Database brought online for standby access
// | 2048     | 0x0800     | When set by the user, prevents cross-database access via an alias mechanism
// | -32768   | 0xFFFF8000 | Database has some portion of the log which is not on a log-only device
// +----------+------------+--------------------------------------------------------------------------------
// 
// This table lists the bit representations for the status3 column.
// status3 Control Bits in the sysdatabases Table
// +----------+---------+--------------------------------------------------------------------------------
// | Decimal  |Hex      | Status
// +----------+---------+--------------------------------------------------------------------------------
// | 0        | 0x0000  | A normal or standard database, or a database without a proxy update in the create statement.
// | 1        | 0x0001  | You specified the proxy_update option, and the database is a user-created proxy database.
// | 2        | 0x0002  | Database is a proxy database created by high availability.
// | 4        | 0x0004  | Database has a proxy database created by high availability.
// | 8        | 0x0008  | Disallow access to the database, since database is being shut down.
// | 16       | 0x0010  | Database is a failed-over database.
// | 32       | 0x0020  | Database is a mounted database of the type master.
// | 64       | 0x0040  | Database is a mounted database.
// | 128      | 0x0080  | Writes to the database are blocked by the quiesce database command.
// | 256      | 0x0100  | User-created tempdb.
// | 512      | 0x0200  | Disallow external access to database in the server in failed-over state.
// | 1024     | 0x0400  | User-provided option to enable or disable asynchronous logging service threads. Enable through sp_dboption enbale async logging service option set to true on a particular database.
// | 4096     | 0x1000  | Database has been shut down successfully.
// | 8192     | 0x2000  | A drop database is in progress.
// +----------+---------+--------------------------------------------------------------------------------
// 
// This table lists the bit representations for the status4 column.
// status4 Control Bits in the sysdatabases Table
// +----------+------------+--------------------------------------------------------------------------------
// | Decimal  |Hex         | Status
// +----------+------------+--------------------------------------------------------------------------------
// | 512      | 0x0200     | The in-memory database has a template database with it.
// | 4096     | 0x1000     | Database is an in-memory databases.
// | 16384    | 0x4000     | 64-bit atomic operations have been enabled on this database.
// | 16777216 | 0x01000000 | All tables in the database are created as page compressed.
// | 33554432 | 0x02000000 | All tables in the database are created as row compressed.
// +----------+------------+--------------------------------------------------------------------------------
// 
// The sysdatabases system table supports the full database encryption feature in the status5, which indicates the encryption status of a database. The values are:
// Hex Description
// +------------+----------------------------------------------------------------------------------------
// | Hex        | Description
// +------------+----------------------------------------------------------------------------------------
// | 0x00000001 | Indicates whether the database is encrypted or not.
// | 0x00000002 | The database is being encrypted, and the encryption is still in progress.
// | 0x00000004 | The database is being decrypted, and the decryption is still in progress.
// | 0x00000008 | The database is only partially encrypted, either due to an error or because the process was suspended by the user.
// | 0x00000010 | The database is only partially decrypted, either due to an error or because the process was suspended by the user.
// +------------+----------------------------------------------------------------------------------------
//
