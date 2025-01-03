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
package com.dbxtune.check;

import java.lang.invoke.MethodHandles;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.CounterController;
import com.dbxtune.CounterControllerSqlServer;
import com.dbxtune.ICounterController;
import com.dbxtune.Version;
import com.dbxtune.config.dict.MonTablesDictionary;
import com.dbxtune.config.dict.MonTablesDictionaryManager;

public class CheckForUpdatesSqlServer extends CheckForUpdatesDbx
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private static final String SQLSERVERTUNE_DMV_INFO_URL = "http://www.dbxtune.com/sqlserver_dmv_info.php";

//	@Override
//	public QueryString createCheckForUpdate(Object... params)
//	{
//		System.out.println("NOT_YET_IMPLEMENTED: createCheckForUpdate()");
//		return null;
//	}

//	@Override
//	public QueryString createSendConnectInfo(Object... params)
//	{
//		System.out.println("NOT_YET_IMPLEMENTED: createSendConnectInfo()");
//		return null;
//	}

//	@Override
//	public List<QueryString> createSendCounterUsageInfo(Object... params)
//	{
//		System.out.println("NOT_YET_IMPLEMENTED: createSendCounterUsageInfo()");
//		return null;
//	}

//	@Override
//	public List<QueryString> createSendMdaInfo(Object... params)
//	{
//		System.out.println("NOT_YET_IMPLEMENTED: createSendMdaInfo()");
//		return null;
//	}

//	@Override
//	public QueryString createSendUdcInfo(Object... params)
//	{
//		System.out.println("NOT_YET_IMPLEMENTED: createSendUdcInfo()");
//		return null;
//	}

//	@Override
//	public QueryString createSendLogInfo(Object... params)
//	{
//		System.out.println("NOT_YET_IMPLEMENTED: createSendLogInfo()");
//		return null;
//	}

	@Override
//	public List<QueryString> createSendMdaInfo()
	public List<QueryString> createSendMdaInfo(Object... params)
	{
		if ( ! MonTablesDictionaryManager.hasInstance() )
		{
			_logger.debug("MonTablesDictionary not initialized when trying to send connection info, skipping this.");
			return null;
		}
		MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
		if (mtd == null)
		{
			_logger.debug("MonTablesDictionary was null when trying to send connection info, skipping this.");
			return null;
		}

		if (mtd.getDbmsExecutableVersionNum() <= 0)
		{
			_logger.debug("MonTablesDictionary srvVersionNum is zero, stopping here.");
			return null;
		}

//		if (mtd.getDbmsMonTableVersionNum() > 0 && mtd.getDbmsExecutableVersionNum() != mtd.getDbmsMonTableVersionNum())
//		{
//			_logger.info("MonTablesDictionary srvVersionNum("+mtd.getDbmsExecutableVersionNum()+") and installmaster/monTables VersionNum("+mtd.getDbmsMonTableVersionNum()+") is not in sync, so we don't want to send MDA info about this.");
//			return null;
//		}

		if ( ! CounterController.getInstance().isMonConnected() )
		{
			_logger.debug("No DBMS Connection to the monitored server.");
			return null;
		}

		List<QueryString> sendQueryList = new ArrayList<QueryString>();

//		int rowCountSum = 0;

		Date timeNow = new Date(System.currentTimeMillis());

		String checkId          = getCheckId() + "";
		String clientTime       = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(timeNow);

		String srvVersion        = mtd.getDbmsExecutableVersionNum() + "";
		boolean isAzureAnalytics = false; 
		
//		String atAtVersion = mtd.getDbmsExecutableVersionStr();
//		if (StringUtil.hasValue(atAtVersion))
//			isAzureAnalytics = atAtVersion.contains("Microsoft SQL Azure");
		
		ICounterController cc = CounterController.getInstance();
		if (cc instanceof CounterControllerSqlServer)
			isAzureAnalytics = ((CounterControllerSqlServer)cc).isAzureAnalytics();

		// Get MDA information 
		try
		{
			// TABLES
			// COLUMNS
			// PARAMETERS
			// SYSOBJECTS - SKIPPED IN SQL-Server

			// -------------------------------------
			// -- DMV - "TABLES"
			// -------------------------------------
			String sql_dmv_tables_rowCount = ""
				    + "-- COUNT \n"
				    + "select cnt = view_cnt + func_cnt \n"
				    + "from ( \n"
				    + "    select view_cnt=count(*) \n"
				    + "    from sys.system_objects o \n"
				    + "    where 1=1 \n"
				    + "      and o.type in('V') \n"
				    + "      and o.is_ms_shipped = 1 \n"
				    + "      and o.name like 'dm_%' \n"
				    + ") v, \n"
				    + "( \n"
				    + "    select func_cnt=count(*) \n"
				    + "    from sys.system_objects o \n"
				    + "    where 1=1 \n"
				    + "      and o.type in('IF', 'TF') \n"
				    + "      and o.is_ms_shipped = 1 \n"
				    + "      and o.name like 'dm_%' \n"
				    + ") f \n"
				    + "";

			String sql_dmv_tables = ""
				    + "-- TABLES \n"
				    + "select \n"
				    + "    Type           ='DM_VIEW', \n"
				    + "    TabName        = o.name, \n"
				    + "    TabId          = o.object_id, \n"
				    + "    ColName        = '', \n"
				    + "    ColId          = -1, \n"
				    + "    TypeName       = '', \n"
				    + "    TypeLen        = -1, \n"
				    + "    TypeIsNullable = -1, \n"
				    + "    Description    = '' \n"
				    + "from sys.system_objects o \n"
				    + "where 1=1 \n"
				    + "  and o.type in('V') \n"
				    + "  and o.is_ms_shipped = 1 \n"
				    + "  and o.name like 'dm_%' \n"
				    + " \n"
				    + "union all \n"
				    + " \n"
				    + "-- FUNCTIONS \n"
				    + "select \n"
				    + "    Type           = 'DM_FUNC', \n"
				    + "    TabName        = o.name, \n"
				    + "    TabId          = o.object_id, \n"
				    + "    ColName        = '', \n"
				    + "    ColId          = -1, \n"
				    + "    TypeName       = '', \n"
				    + "    TypeLen        = -1, \n"
				    + "    TypeIsNullable = -1, \n"
				    + "    Description    = '' \n"
				    + "from sys.system_objects o \n"
				    + "where 1=1 \n"
				    + "  and o.type in('IF', 'TF') \n"
				    + "  and o.is_ms_shipped = 1 \n"
				    + "  and o.name like 'dm_%' \n"
				    + " \n"
				    + "order by 2 \n"
				    + "";
			

			// -------------------------------------
			// -- DMV - "COLUMNS"
			// -------------------------------------
			String sql_dmv_columns_rowCount = ""
				    + "-- COUNT \n"
				    + "select cnt = view_cnt + func_cnt \n"
				    + "from ( \n"
				    + "    select view_cnt = count(*) \n"
				    + "    from sys.system_objects o \n"
				    + "    INNER JOIN sys.system_columns sc ON o.object_id = sc.object_id \n"
				    + "    INNER JOIN sys.types t           ON sc.system_type_id = t.system_type_id AND sc.user_type_id = t.user_type_id \n"
				    + "    where 1=1 \n"
				    + "      and o.type in('V') \n"
				    + "      and o.is_ms_shipped = 1 \n"
				    + "      and o.name like 'dm_%' \n"
				    + ") v, \n"
				    + "( \n"
				    + "    select func_cnt = count(*) \n"
				    + "    from sys.system_objects o \n"
				    + "    INNER JOIN sys.system_columns sc ON o.object_id = sc.object_id \n"
				    + "    INNER JOIN sys.types t           ON sc.system_type_id = t.system_type_id AND sc.user_type_id = t.user_type_id \n"
				    + "    where 1=1 \n"
				    + "      and o.type in('IF', 'TF') \n"
				    + "      and o.is_ms_shipped = 1 \n"
				    + "      and o.name like 'dm_%' \n"
				    + ") f \n"
				    + "";

			String sql_dmv_columns = ""
				    + "-- TABLE COLUMNS \n"
				    + "select \n"
				    + "    Type           ='DM_VIEW_COL', \n"
				    + "    TabName        = o.name, \n"
				    + "    TabId          = o.object_id, \n"
				    + "    ColName        = sc.name, \n"
				    + "    ColId          = sc.column_id, \n"
				    + "    TypeName       = t.name, \n"
				    + "    TypeLen        = sc.max_length, \n"
				    + "    TypeIsNullable = sc.is_nullable, \n"
				    + "    Description    = '' \n"
				    + "from sys.system_objects o \n"
				    + "INNER JOIN sys.system_columns sc ON o.object_id = sc.object_id \n"
				    + "INNER JOIN sys.types t           ON sc.system_type_id = t.system_type_id AND sc.user_type_id = t.user_type_id \n"
				    + "where 1=1 \n"
				    + "  and o.type in('V') \n"
				    + "  and o.is_ms_shipped = 1 \n"
				    + "  and o.name like 'dm_%' \n"
				    + " \n"
				    + "union all \n"
				    + " \n"
				    + "-- FUNCTION COLUMNS \n"
				    + "select \n"
				    + "    Type           = 'DM_FUNC_COL', \n"
				    + "    TabName        = o.name, \n"
				    + "    TabId          = o.object_id, \n"
				    + "    ColName        = sc.name, \n"
				    + "    ColId          = sc.column_id, \n"
				    + "    TypeName       = t.name, \n"
				    + "    TypeLen        = sc.max_length, \n"
				    + "    TypeIsNullable = sc.is_nullable, \n"
				    + "    Description    = '' \n"
				    + "from sys.system_objects o \n"
				    + "INNER JOIN sys.system_columns sc ON o.object_id = sc.object_id \n"
				    + "INNER JOIN sys.types t           ON sc.system_type_id = t.system_type_id AND sc.user_type_id = t.user_type_id \n"
				    + "where 1=1 \n"
				    + "  and o.type in('IF', 'TF') \n"
				    + "  and o.is_ms_shipped = 1 \n"
				    + "  and o.name like 'dm_%' \n"
				    + " \n"
				    + "order by 2, 5 \n"
				    + "";
			
			// -------------------------------------
			// -- DM_FUNCTIONS - PARAMETERS
			// -------------------------------------
			String sql_dmv_parameters_rowCount = ""
				    + "select count(*) \n"
				    + "from sys.system_objects o \n"
				    + "INNER JOIN sys.system_parameters sc ON o.object_id = sc.object_id \n"
				    + "INNER JOIN sys.types t           ON sc.system_type_id = t.system_type_id AND sc.user_type_id = t.user_type_id \n"
				    + "where 1=1 \n"
				    + "  and o.type in('IF', 'TF') \n"
				    + "  and o.is_ms_shipped = 1 \n"
				    + "  and o.name like 'dm_%' \n"
				    + "";
			
			String sql_dmv_parameters = ""
				    + "select \n"
				    + "    Type           = 'DM_FUNC_PARAMS', \n"
				    + "    FuncName       = o.name, \n"
				    + "    FuncId         = o.object_id, \n"
				    + "    ColName        = sc.name, \n"
				    + "    ColId          = sc.parameter_id, \n"
				    + "    TypeName       = t.name, \n"
				    + "    TypeLen        = sc.max_length, \n"
				    + "    TypeIsNullable = sc.is_nullable, \n"
				    + "    Description    = '' \n"
				    + "from sys.system_objects o \n"
				    + "INNER JOIN sys.system_parameters sc ON o.object_id = sc.object_id \n"
				    + "INNER JOIN sys.types t           ON sc.system_type_id = t.system_type_id AND sc.user_type_id = t.user_type_id \n"
				    + "where 1=1 \n"
				    + "  and o.type in('IF', 'TF') \n"
				    + "  and o.is_ms_shipped = 1 \n"
				    + "  and o.name like 'dm_%' \n"
				    + "order by o.name, sc.parameter_id \n"
				    + "";
			

			int sendMdaInfoBatchSize = getSendMdaInfoBatchSize();

			// DM VEWS/FUNCTIONS
			getMdaInfo(CounterController.getInstance().getMonConnection(), 
					checkId, clientTime, System.getProperty("user.name"), srvVersion, isAzureAnalytics+"", 
					sql_dmv_tables_rowCount, sql_dmv_tables, 
					sendMdaInfoBatchSize, sendQueryList);

			// DM VEWS/FUNCTIONS COLUMNS
			getMdaInfo(CounterController.getInstance().getMonConnection(), 
					checkId, clientTime, System.getProperty("user.name"), srvVersion, isAzureAnalytics+"", 
					sql_dmv_columns_rowCount, sql_dmv_columns, 
					sendMdaInfoBatchSize, sendQueryList);
			
			// DM FUNCTIONS PARAMETERS
			getMdaInfo(CounterController.getInstance().getMonConnection(), 
					checkId, clientTime, System.getProperty("user.name"), srvVersion, isAzureAnalytics+"", 
					sql_dmv_parameters_rowCount, sql_dmv_parameters, 
					sendMdaInfoBatchSize, sendQueryList);
			
//			// DM VIEWS
//			String sql_dm_views_rowCount = ""
//				    + "select count(*) \n"
//				    + "from sys.system_objects o \n"
//				    + "INNER JOIN sys.system_columns sc ON o.object_id = sc.object_id \n"
//				    + "INNER JOIN sys.types t           ON sc.system_type_id = t.system_type_id AND sc.user_type_id = t.user_type_id \n"
//				    + "where 1=1 \n"
//				    + "  and o.type in('V') \n"
//				    + "  and o.is_ms_shipped = 1 \n"
//				    + "  and o.name like 'dm_%' \n"
//				    + "";
//			String sql_dm_views = ""
//				    + "select "
//				    	+ "Type           = 'DM_VIEW', "
//				    	+ "ViewName       = o.name, "
//				    	+ "ViewId         = o.object_id, "
//				    	+ "ColName        = sc.name, "
//				    	+ "ColId          = sc.column_id, "
//				    	+ "TypeName       = t.name, "
//				    	+ "TypeLen        = sc.max_length, "
//				    	+ "TypeIsNullable = sc.is_nullable, "
//				    	+ "Description    = '' \n"
//				    + "from sys.system_objects o \n"
//				    + "INNER JOIN sys.system_columns sc ON o.object_id = sc.object_id \n"
//				    + "INNER JOIN sys.types t           ON sc.system_type_id = t.system_type_id AND sc.user_type_id = t.user_type_id \n"
//				    + "where 1=1 \n"
//				    + "  and o.type in('V') \n"
//				    + "  and o.is_ms_shipped = 1 \n"
//				    + "  and o.name like 'dm_%' \n"
//				    + "order by o.name, sc.column_id \n"
//				    + "";
//			
//			
//			// DM FUNCTIONS
//			String sql_dm_functions_rowCount = ""
//				    + "select count(*) \n"
//				    + "from sys.system_objects o \n"
//				    + "INNER JOIN sys.system_parameters sc ON o.object_id = sc.object_id \n"
//				    + "INNER JOIN sys.types t              ON sc.system_type_id = t.system_type_id AND sc.user_type_id = t.user_type_id \n"
//				    + "where 1=1 \n"
//				    + "  and o.type in('IF', 'TF') \n"
//				    + "  and o.is_ms_shipped = 1 \n"
//				    + "  and o.name like 'dm_%' \n"
//				    + "";
//			String sql_dm_functions = ""
//				    + "select "
//				    	+ "Type           = 'DM_FUNC', "
//				    	+ "FuncName       = o.name, "
//				    	+ "FuncId         = o.object_id, "
//				    	+ "ColName        = sc.name, "
//				    	+ "ColId          = sc.parameter_id, "
//				    	+ "TypeName       = t.name, "
//				    	+ "TypeLen        = sc.max_length, "
//				    	+ "TypeIsNullable = sc.is_nullable, "
//				    	+ "Description    = '' \n"
//				    + "from sys.system_objects o \n"
//				    + "INNER JOIN sys.system_parameters sc ON o.object_id = sc.object_id \n"
//				    + "INNER JOIN sys.types t              ON sc.system_type_id = t.system_type_id AND sc.user_type_id = t.user_type_id \n"
//				    + "where 1=1 \n"
//				    + "  and o.type in('IF', 'TF') \n"
//				    + "  and o.is_ms_shipped = 1 \n"
//				    + "  and o.name like 'dm_%' \n"
//				    + "order by o.name, sc.parameter_id \n"
//				    + "";
//			
//
//			int sendMdaInfoBatchSize = getSendMdaInfoBatchSize();
//
//			// DM VIEWS (from system tables)
//			getMdaInfo(CounterController.getInstance().getMonConnection(), 
//					checkId, clientTime, System.getProperty("user.name"), srvVersion, isAzure+"", 
//					sql_dm_views_rowCount, sql_dm_views, 
//					sendMdaInfoBatchSize, sendQueryList);
//
//			// DM FUNCTIONS (from system tables)
//			getMdaInfo(CounterController.getInstance().getMonConnection(), 
//					checkId, clientTime, System.getProperty("user.name"), srvVersion, isAzure+"", 
//					sql_dm_functions_rowCount, sql_dm_functions, 
//					sendMdaInfoBatchSize, sendQueryList);

			
//			_logger.info("sendMdaInfo: Starting to send "+rowCountSum+" MDA information entries in "+sendQueryList.size()+" batches, for ASE Version '"+mtd.getAseExecutableVersionNum()+"'.");
			_logger.info("sendMdaInfo: Sending DMV information entries for SQL-Server Version '"+mtd.getDbmsExecutableVersionNum()+"'. (sendDmvInfoBatchSize="+sendMdaInfoBatchSize+")");
		}
		catch (SQLException e)
		{
			sendQueryList.clear();
			_logger.debug("Problems when getting MDA information. Caught: "+e, e);
		}

		return sendQueryList;
	}

	private int getMdaInfo(
			Connection conn,
			String checkId, 
			String clientTime, 
			String userName, 
			String srvVersionNum, 
			String isAzure, 
			String sqlGetCount, 
			String sqlGetValues,
			int batchSize,
			List<QueryString> sendQueryList)
	throws SQLException
	{
		// URL TO USE
		String urlStr = SQLSERVERTUNE_DMV_INFO_URL;

		Statement  stmt = conn.createStatement();
		ResultSet  rs;

		// get expected rows
		int expectedRows = 0;
		rs = stmt.executeQuery(sqlGetCount);
		while ( rs.next() )
			expectedRows = rs.getInt(1);
		rs.close();

		// get VALUES
		rs = stmt.executeQuery(sqlGetValues);

		int rowId        = 0;
		int rowsInBatch  = 0;
		int batchCounter = 0;
		QueryString urlParams = new QueryString(urlStr);

		while ( rs.next() )
		{
			rowId++;
			rowsInBatch++;

			if (batchCounter == 0)
			{
				if (_logger.isDebugEnabled())
					urlParams.add("debug",    "true");

				urlParams.add("checkId",            checkId);
				urlParams.add("clientTime",         clientTime);
				urlParams.add("clientAppName",      Version.getAppName());
				urlParams.add("userName",           userName);

				urlParams.add("srvVersion",         srvVersionNum);
				urlParams.add("isAzure",            isAzure);

				urlParams.add("expectedRows",       expectedRows+"");
			}

			urlParams.add("type"        + "-" + batchCounter, rs.getString(1));
			urlParams.add("TableName"   + "-" + batchCounter, rs.getString(2));
			urlParams.add("TableID"     + "-" + batchCounter, rs.getString(3));
			urlParams.add("ColumnName"  + "-" + batchCounter, rs.getString(4));
			urlParams.add("ColumnID"    + "-" + batchCounter, rs.getString(5));
			urlParams.add("TypeName"    + "-" + batchCounter, rs.getString(6));
			urlParams.add("Length"      + "-" + batchCounter, rs.getString(7));
			urlParams.add("IsNullable"  + "-" + batchCounter, rs.getString(8));
			urlParams.add("Description" + "-" + batchCounter, rs.getString(9));

			urlParams.add("rowId"       + "-" + batchCounter, rowId+"");

			batchCounter++;

			// start new batch OR on last row
			if (batchCounter >= batchSize || rowId >= expectedRows)
			{
				// add number of records added to this entry
				urlParams.add("batchSize",      batchCounter+"");
//System.out.println("QueryString: length="+urlParams.length()+", entries="+urlParams.entryCount()+".");

				batchCounter = 0;
				urlParams.setCounter(rowsInBatch);
				sendQueryList.add(urlParams);

				urlParams = new QueryString(urlStr);
				rowsInBatch = 0;
				//rowId = 0; // Do NOT reset rowId here
			}
		}

		rs.close();
		stmt.close();
		
		return rowId;
	}

}
