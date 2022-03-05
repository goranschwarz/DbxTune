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
import java.util.Arrays;
import java.util.List;

import com.asetune.Version;
import com.asetune.utils.StringUtil;

public class CounterSampleCatalogIteratorSqlServer 
extends CounterSampleCatalogIterator
{
	private static final long serialVersionUID = 1L;

//	public final static List<String> DEFAULT_SKIP_DB_LIST     = Arrays.asList( new String[]{"master", "model", "tempdb", "msdb", "SSISDB", "ReportServer", "ReportServerTempDB"} );
	public final static List<String> DEFAULT_SKIP_DB_LIST     = Arrays.asList( new String[]{"master", "model",           "msdb", "SSISDB", "ReportServer", "ReportServerTempDB"} );
	public final static List<String> DEFAULT_FALLBACK_DB_LIST = Arrays.asList( new String[]{"tempdb"} );

	// This is used if no databases are in a "valid" state.
	private List<String> _skipList     = null;
	private List<String> _fallbackList = null;

	/**
	 * @param name
	 * @param negativeDiffCountersToZero
	 * @param diffColNames
	 * @param prevSample
	 * @param skipDbList   a list of database(s) that will be skipped
	 * @param fallbackList a list of database(s) that will be used in case of "no valid" databases can be found, typically usage is "tempdb" to at least get one database.
	 */
	public CounterSampleCatalogIteratorSqlServer(String name, boolean negativeDiffCountersToZero, String[] diffColNames, CounterSample prevSample, List<String> skipDbList, List<String> fallbackDbList)
	{
		super(name, negativeDiffCountersToZero, diffColNames, prevSample);

		_skipList     = skipDbList;
		_fallbackList = fallbackDbList;
	}
	
	/**
	 * @param name
	 * @param negativeDiffCountersToZero
	 * @param diffColNames
	 * @param prevSample
	 */
	public CounterSampleCatalogIteratorSqlServer(String name, boolean negativeDiffCountersToZero, String[] diffColNames, CounterSample prevSample)
	{
		super(name, negativeDiffCountersToZero, diffColNames, prevSample);

		// TODO: get below from Configuration...
		_skipList     = DEFAULT_SKIP_DB_LIST;
		_fallbackList = DEFAULT_FALLBACK_DB_LIST;
	}
	
//	@Override
//	protected List<String> getCatalogList(CountersModel cm, Connection conn)
//	throws SQLException
//	{
//		List<String> list = super.getCatalogList(cm, conn);
//		
//		// If the above get **no** databases that are in correct state add the "passed" database(s)
//		if (list.isEmpty())
//		{
//			if (_fallbackList != null)
//				list.addAll(_fallbackList);
//		}
//
//		return list;
//	}

	// If we want to filter out databases with some STATUS...
	@Override
	protected List<String> getCatalogList(CountersModel cm, Connection conn)
	throws SQLException
	{
		String skipDbNames = "";
		if (_skipList != null && !_skipList.isEmpty())
			skipDbNames = "  and d.name not in (" + StringUtil.toCommaStrQuoted('\'', _skipList) + ") \n";

//		String sql = 
//			  "SELECT name \n"
//			+ "FROM sys.databases \n"
//			+ "WHERE 1=1 \n"
//			+ skipDbNames
//			+ "  AND state_desc = 'ONLINE'      -- Only databases that are ONLINE \n"
//			+ "ORDER BY database_id \n"
//			+ "";

		// The below SQL is grabbed from sp_msforachdb
		String sql = ""
    		+ "select /* " + Version.getAppName() + ":" + this.getClass().getSimpleName() + " */ \n"
    		+ "    d.name \n"
    		+ "from sysdatabases d \n"
    		+ "where 1 = 1 \n"
			+ skipDbNames
    		+ "  and (d.status & 992 = 0) -- 0x03e0  -- 32=loading, 64=preRecovery, 128=recovering, 256=notRecovered, 512=offline \n"
    		+ "  and DATABASEPROPERTYEX(d.name, 'UserAccess') <> 'SINGLE_USER'  \n"
    		+ "  and has_dbaccess(d.name) = 1 \n"
    		+ "order by d.dbid  \n"
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
