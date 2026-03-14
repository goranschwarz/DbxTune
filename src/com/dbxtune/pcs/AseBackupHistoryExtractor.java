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

/**
 * Most code for this was reused from SqlServerQueryStoreExtractor...
 */
public class AseBackupHistoryExtractor
extends DbmsExtractorAbstract
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	// NOTE: Properties for this are stored in 'CounterControllerAse' where the object is instantiated
//	public static final String PROPKEY_HistoryDays = "AseBackupHistoryExtractor.historical.days";
//	public static final int    DEFAULT_HistoryDays = 30;

	public static final String EXTRACTOR_NAME  = "ASE Backup History";
	public static final String MON_DB_NAME     = "master";
	public static final String PCS_SCHEMA_NAME = "backup_history";
	
	/** period: -1 = Everything. above 0 = Number of days to extract */
	private int           _period    = -1;
	private String        _periodStr = "";

	public AseBackupHistoryExtractor(int period, DbxConnection monConn, DbxConnection pcsConn)
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
	protected String getPeriodInfoLong()
	{
		return super.getPeriodInfoLong();
	}


	@Override
	protected List<ExtractorEntry> createExtractors() 
	{
		List<ExtractorEntry> list = new ArrayList<>();

		// dump_summary -- Is created in the Report Section and uses data from: dump_history
		list.add( new dump_history() );
		
		return list;
	}	

	
	//--------------------------------------------------------------------
	//-- dump_history
	//--------------------------------------------------------------------
	public class dump_history implements ExtractorEntry
	{
		@Override public String           getTableName()        { return this.getClass().getSimpleName(); }
//		@Override public String           getTableName()        { return "dump_history"; }
		@Override public ExtractionMethod getExtractionMethod() { return ExtractionMethod.RSTM; }
		@Override public List<String>     getCreateIndex()      { return Collections.emptyList(); }
		
		@Override
		public String getSql()
		{
//			String sql = ""
//				    + "DECLARE @dumpHistoryIsEnabled int \n"
//				    + "SELECT @dumpHistoryIsEnabled = value FROM master.dbo.syscurconfigs WHERE config = (SELECT config FROM master.dbo.sysconfigures WHERE name = 'enable dump history') \n"
//				    + "if (@dumpHistoryIsEnabled != 10) \n"
//				    + "BEGIN \n"
//				    + "	-- NOTE if the table 'master.dbo.sysdumphist' does NOT exists... bthe SQL Batch will fail... so split it into 2 SQL Batches \n"
//				    + "	PRINT 'WARNING: Dump History is NOT enabled.' \n"
//				    + "	PRINT 'Note: Enable it with the following commands.' \n"
//				    + "	PRINT '  - exec sp_configure ''enable dump history'', 1' \n"
//				    + "	PRINT '  - exec sp_dump_history create_table, @name=''master.dbo.sysdumphist''' \n"
//				    + " \n"
//				    + "	RETURN \n"
//				    + "END \n"

			// NOTE: if the table 'master.dbo.sysdumphist' does NOT exists... the SQL Batch will fail... 
			//       So we need to CHECK that PRIOR to extraction, it's done in CounterControllerAse.doLastRecordingActionBeforeDatabaseRollover() where it's instantiated
			String sql = ""
				    + "SELECT \n"
//				    + "     @@servername AS server \n"
				    + "     dh.name      AS DBName \n"
				    + "    ,CASE dh.rec_type \n"
				    + "        WHEN 2 THEN 'DATABASE' \n"
				    + "        WHEN 3 THEN 'TRAN' \n"
				    + "        WHEN 5 THEN 'CUMULATIVE' \n"
				    + "        WHEN 6 THEN 'DELTA' \n"
				    + "        ELSE 'UNKNOWN' \n"
				    + "     END AS backup_type \n"
				    + "    ,CASE dh.status \n"
				    + "        WHEN  1 THEN 'DUMP SUCCESS' \n"
				    + "        WHEN  2 THEN 'Error: ' + convert(varchar(10), cmp_lvl) \n"
				    + "        WHEN  4 THEN 'Deleted' \n"
				    + "        WHEN  8 THEN 'Altdb ON' \n"
				    + "        WHEN 16 THEN 'Altdb OFF' \n"
				    + "        WHEN 33 THEN 'LOAD, Success' \n"
				    + "        WHEN 34 THEN 'Error: ' + convert(varchar(10), cmp_lvl) \n"
				    + "        ELSE 'UNKNOWN' \n"
				    + "     END AS status \n"
				    + "    ,cast(dateadd(second, datediff(second, dh.dump_date, dh.dmp_end_time), '2020-01-01 00:00:00') as time) AS duration_hms \n"
				    + "    ,datename(dw, dh.dump_date)  AS backup_day \n"
				    + "    ,dh.dump_date \n"
				    + "    ,dh.dmp_end_time \n"
				    + "    ,CAST(round(((dh.dump_size / (1024.0/@@maxpagesize)) / 1024.0 / 1024.0), 6) as numeric(25, 1)) AS dump_size_GB \n"
				    + "    ,CAST(round(((dh.dump_size / (1024.0/@@maxpagesize)) / 1024.0), 6) as numeric(25, 0)) AS dump_size_MB \n"
				    + "    ,CAST(round(((dh.dump_size / (1024.0/@@maxpagesize))         ), 6) as numeric(25, 0)) AS dump_size_KB \n"
				    + "    ,dh.dump_size AS dump_pages \n"
				    + "    ,dh.label \n"
				    + "    ,CASE dh.status WHEN 1 THEN dh.cmp_lvl ELSE 0 END AS cmp_lvl \n"
				    + "    ,dh.num_stripes \n"
				    + "    ,dh.stripe_name \n"
				    + "    ,CASE dh.passwd WHEN 1 THEN 'yes' ELSE 'no' END AS password \n"
				    + "    ,dh.dbid \n"
				    + "FROM \n"
				    + "   master.dbo.sysdumphist dh \n"
				    + "WHERE 1 = 1 \n"
				    + "  AND dh.dump_date >= dateadd(day, -" + _period + ", getdate()) \n"
				    + "  AND dh.dbid != 0 \n"
				    + "ORDER BY \n"
				    + "   dh.name, \n"
				    + "   dh.dump_date \n"
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
		pcsCp.setUrl("jdbc:h2:file:C:/Users/goran/.dbxtune/data/PROD_A2_ASE_2026-02-02;IFEXISTS=TRUE");
		pcsCp.setUsername("sa");
		pcsCp.setPassword("");

		ConnectionProp monCp = new ConnectionProp();
		monCp.setUrl("jdbc:sybase:Tds:prod-b2-ase.maxm.se:5000?ENCRYPT_PASSWORD=true");
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
				AseBackupHistoryExtractor extractor = new AseBackupHistoryExtractor(daysToCopy, monConn, pcsConn);
				extractor.transfer();

				for (ExtractorEntry entry : extractor.getExtractors())
				{
					String sql = pcsConn.quotifySqlString("select * from [" + PCS_SCHEMA_NAME + "].[" + entry.getTableName() + "]");
					ResultSetTableModel rstm = ResultSetTableModel.executeQuery(pcsConn, sql, entry.getTableName());
//
//					System.out.println(rstm.toAsciiTableString());
					System.out.println(rstm.toHtmlTableFoldableString("sorttable", null));
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

