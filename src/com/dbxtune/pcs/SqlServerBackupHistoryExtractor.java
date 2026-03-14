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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.sql.conn.ConnectionProp;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.DbUtils;
import com.dbxtune.utils.Ver;

/**
 * Most code for this was reused from SqlServerQueryStoreExtractor...
 */
public class SqlServerBackupHistoryExtractor
extends DbmsExtractorAbstract
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	// NOTE: Properties for this are stored in 'CounterControllerSqlServer' where the object is instantiated
//	public static final String PROPKEY_HistoryDays = "SqlServerBackupHistoryExtractor.historical.days";
//	public static final int    DEFAULT_HistoryDays = 30;
	
//	public static final String PROPKEY_details_HistoryDays = "SqlServerBackupHistoryExtractor.details.historical.days";
//	public static final int    DEFAULT_details_HistoryDays = 30;
	
	public static final String EXTRACTOR_NAME  = "SQL Server Backup History";
	public static final String MON_DB_NAME     = "msdb";
	public static final String PCS_SCHEMA_NAME = "backup_history";
	
	/** period: -1 = Everything. above 0 = Number of days to extract */
	private int           _period    = -1;
	private String        _periodStr = "";

	public SqlServerBackupHistoryExtractor(int period, DbxConnection monConn, DbxConnection pcsConn)
	{
		super(EXTRACTOR_NAME, monConn, MON_DB_NAME, pcsConn, PCS_SCHEMA_NAME);

		_period     = period;

		if (_period == 0)
			_period = -1;

		_periodStr = "everything";
		if (_period == 1)
			_periodStr = "this day";
		else if (_period > 1)
			_periodStr = "last " + _period + " days";

		if (_period < 0)
			_period = 10 * 365; // 10 Years
	}

	@Override
	protected String getPeriodInfoShort()
	{
		return _periodStr;
	}
		
	@Override
	protected List<ExtractorEntry> createExtractors() 
	{
		List<ExtractorEntry> list = new ArrayList<>();

		list.add( new backup_summary() );
		list.add( new backup_details()    );
		
		return list;
	}	
	
	//--------------------------------------------------------------------
	//-- dailySummary
	//--------------------------------------------------------------------
	public class backup_summary implements ExtractorEntry
	{
		@Override public String           getTableName()        { return this.getClass().getSimpleName(); }
//		@Override public String           getTableName()        { return "backup_summary"; }
		@Override public ExtractionMethod getExtractionMethod() { return ExtractionMethod.RSTM; }
		@Override public List<String>     getCreateIndex()      { return Collections.emptyList(); }
		
		@Override
		public String getSql()
		{
			String backup_order_and_duration_hms = "";

			if (getMonConn().getDbmsVersionInfo().getLongVersion() >= Ver.ver(2019))
			{
//				backup_order_and_duration_hms = "   ,backup_order_and_start_time = STRING_AGG(bus.database_name + ' [' + cast(cast(bus.backup_start_date as time) as varchar(8)) + ']', ', ') WITHIN GROUP ( ORDER BY bus.backup_start_date ) \n"
				backup_order_and_duration_hms = "   ,backup_order_and_duration_hms = STRING_AGG(bus.database_name + ' [' + cast(cast(dateadd(second, datediff(second, bus.backup_start_date, bus.backup_finish_date), '2020-01-01 00:00:00') as time) as varchar(8)) + ']', ', ') WITHIN GROUP ( ORDER BY bus.backup_start_date ) \n";
			}

			String sql = ""
				    + "select \n"
//				    + "    server       = @@servername \n"
				    + "    backup_date  = cast(backup_start_date as date) \n"
				    + "   ,backup_day   = datename(dw, cast(backup_start_date as date)) \n"
				    + "   ,db_count     = count(*) \n"
				    + "   ,duration_hms = cast(dateadd(second, datediff(second, min(bus.backup_start_date), max(bus.backup_finish_date)), '2020-01-01 00:00:00') as time) \n"
				    + "   ,first_start  = cast(min(bus.backup_start_date) as time) \n"
				    + "   ,last_end     = cast(max(bus.backup_finish_date) as time) \n"
				    + "   ,size_GB      = cast(sum(backup_size)            / 1024.0 / 1024.0 / 1024.0 as bigint) \n"
				    + "   ,z_size_GB    = cast(sum(compressed_backup_size) / 1024.0 / 1024.0 / 1024.0 as bigint) \n"
				    + backup_order_and_duration_hms
				    + "from msdb.dbo.backupset bus \n"
				    + "where bus.type IN ('D', 'I') \n" // D=DATABASE, I=DIFF DATABASE
				    + "group by cast(backup_start_date as date) \n"
				    + "order by cast(backup_start_date as date) desc \n"
				    + "";
				
			return sql;
		}
	}

	//--------------------------------------------------------------------
	//-- lastXDays
	//--------------------------------------------------------------------
	public class backup_details implements ExtractorEntry
	{
		@Override public String           getTableName()        { return this.getClass().getSimpleName(); }
//		@Override public String           getTableName()        { return "backup_details"; }
		@Override public ExtractionMethod getExtractionMethod() { return ExtractionMethod.RSTM; }
		@Override public List<String>     getCreateIndex()      { return Collections.emptyList(); }
		
		@Override
		public String getSql()
		{
			String sql = ""
				    + "SELECT \n"
//				    + "     @@servername                         AS server \n"
				    + "     bus.database_name                    AS database_name \n"
				    + "    ,cast(dateadd(second, datediff(second, bus.backup_start_date, bus.backup_finish_date), '2020-01-01 00:00:00') as time) AS duration_hms \n"
				    + "    ,datename(dw, bus.backup_start_date)  AS backup_day\n"
				    + "    ,bus.backup_start_date \n"
				    + "    ,bus.backup_finish_date \n"
//				    + "--    ,bus.expiration_date \n"
				    + "    ,CASE bus.type \n"
				    + "          WHEN 'D' THEN 'DATABASE' \n"
				    + "          WHEN 'I' THEN 'DIFF DATABASE' \n"
				    + "          WHEN 'L' THEN 'TRAN' \n"
				    + "          WHEN 'F' THEN 'FILE GROUP' \n"
				    + "          WHEN 'G' THEN 'DIFF FILE' \n"
				    + "          WHEN 'P' THEN 'PARTIAL' \n"
				    + "          WHEN 'Q' THEN 'DIFF PARTIAL' \n"
				    + "          ELSE bus.type \n"
				    + "     END                                                      AS backup_type \n"
				    + "    ,cast(bus.backup_size / 1024 / 1024 as bigint)            AS backup_size_mb \n"
				    + "    ,cast(bus.compressed_backup_size / 1024 / 1024 as bigint) AS z_backup_size_mb \n"
				    + "    ,bmf.logical_device_name \n"
				    + "    ,bmf.physical_device_name \n"
				    + "    ,bus.name AS backupset_name \n"
				    + "    ,bus.description \n"
				    + "FROM \n"
				    + "   msdb.dbo.backupmediafamily bmf \n"
				    + "   INNER JOIN msdb.dbo.backupset bus ON bmf.media_set_id = bus.media_set_id \n"
				    + "WHERE 1 = 1 \n"
				    + "  AND (CONVERT(datetime, bus.backup_start_date, 102) >= GETDATE() - " + _period + ") \n"
				    + "ORDER BY \n"
				    + "   bus.database_name, \n"
				    + "   bus.backup_finish_date \n"
				    + "";

			return sql;
		}
	}
	
	
	/**
	 * Simple test during development
	 * @param args
	 */
	public static void main(String[] args)
//	public static void TEST_main(String[] args)
	{
		// Set Log4j Log Level
//		Configurator.setRootLevel(Level.TRACE);


		ConnectionProp pcsCp = new ConnectionProp();
//		pcsCp.setUrl("jdbc:h2:file:C:/Users/goran/.dbxtune/data/JOB_SCHED_EXTRACT"); // ;IFEXISTS=TRUE
		pcsCp.setUrl("jdbc:h2:file:C:/Users/goran/.dbxtune/data/MM-OP-DW_2024-11-17;IFEXISTS=TRUE");
		pcsCp.setUsername("sa");
		pcsCp.setPassword("");

		ConnectionProp monCp = new ConnectionProp();
		monCp.setUrl("jdbc:sqlserver://gorans.org;trustServerCertificate=true");
		monCp.setUsername("sa");
		monCp.setPassword("**-not-on-github-**");
//		monCp.setUsername("dbxtune");
//		monCp.setPassword("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");

		try
		{
			DbxConnection pcsConn = DbxConnection.connect(null, pcsCp);
			TEST_createSessionSamplesIfNotExists(pcsConn);

			DbxConnection monConn = DbxConnection.connect(null, monCp);
			
			String srvName = "DUMMY";
			int daysToCopy = 7;
			
			_logger.info("On PCS Database Rollover: Extracting 'Backup History' information On server '" + srvName+ "'.");
			try
			{
				SqlServerBackupHistoryExtractor extractor = new SqlServerBackupHistoryExtractor(daysToCopy, monConn, pcsConn);
				extractor.transfer();

				for (ExtractorEntry entry : extractor.getExtractors())
				{
					String sql = pcsConn.quotifySqlString("select * from [" + PCS_SCHEMA_NAME + "].[" + entry.getTableName() + "]");
					ResultSetTableModel rstm = ResultSetTableModel.executeQuery(pcsConn, sql, entry.getTableName());

					System.out.println(rstm.toAsciiTableString());
				}
			}
			catch (Exception ex)
			{
				_logger.error("On PCS Database Rollover: Problems extracting 'Backup History' information from server '" + srvName + "'.", ex);
			}
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static void TEST_createSessionSamplesIfNotExists(DbxConnection conn)
	throws SQLException
	{
		String schemaName = null;
//		String schemaName = PCS_SCHEMA_NAME;
		String tabName = PersistWriterBase.getTableName(conn, schemaName, PersistWriterBase.SESSION_SAMPLES, null, false);

		if ( ! DbUtils.checkIfTableExistsNoThrow(conn, null, schemaName, tabName) )
		{
			List<String> ddlList = PersistWriterBase.getTableDdlString(conn, schemaName, PersistWriterBase.SESSION_SAMPLES, null);
			String ddl = ddlList.get(0);
			
		//	System.out.println("-------- Creating table: \n" + ddl);
			DbUtils.exec(conn, ddl);

			String sql = conn.quotifySqlString(
					  "insert into [" + tabName + "]([SessionStartTime], [SessionSampleTime]) \n"
					+ "select CURRENT_DATE(), CURRENT_TIMESTAMP");
			DbUtils.exec(conn, sql);
		}
		String sql = conn.quotifySqlString("select * from [" + tabName + "]"); 
		System.out.println(ResultSetTableModel.executeQuery(conn, sql, tabName).toAsciiTableString());
	}
}

