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
package com.dbxtune.cm.postgres;

import java.lang.invoke.MethodHandles;
import java.sql.Types;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.gui.MainFramePostgres;
import com.dbxtune.sql.ResultSetMetaDataCached;
import com.dbxtune.sql.ResultSetMetaDataCached.Entry;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
// this can be of help: 
//    https://www.datadoghq.com/blog/postgresql-monitoring/
//    https://github.com/DataDog/the-monitor/blob/master/postgresql/postgresql-monitoring.md

public class CmPgWalReceiver
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmPgWalReceiver.class.getSimpleName();
	public static final String   SHORT_NAME       = "WAL Receiver";
	public static final String   HTML_DESC        = 
		"<html>" +
		"Only one row, showing statistics about the WAL receiver from that receiver's connected server<br>" +
		"" +
		"</html>";

	public static final String   GROUP_NAME       = MainFramePostgres.TCP_GROUP_REPLICATION;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = Ver.ver(9,6); // According to -- https://pgpedia.info/p/pg_stat_wal_receiver.html
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"pg_stat_wal_receiver"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {};

//  Version 16.2
//	RS> Col# Label                 JDBC Type Name           Guessed DBMS type Source Table        
//	RS> ---- --------------------- ------------------------ ----------------- --------------------
//	RS> 1    pid                   java.sql.Types.INTEGER   int4              pg_stat_wal_receiver
//	RS> 2    status                java.sql.Types.VARCHAR   text              pg_stat_wal_receiver
//	RS> 3    receive_start_lsn     java.sql.Types.OTHER     pg_lsn            pg_stat_wal_receiver
//	RS> 4    receive_start_tli     java.sql.Types.INTEGER   int4              pg_stat_wal_receiver
//	RS> 5    written_lsn           java.sql.Types.OTHER     pg_lsn            pg_stat_wal_receiver
//	RS> 6    flushed_lsn           java.sql.Types.OTHER     pg_lsn            pg_stat_wal_receiver
//	RS> 7    received_tli          java.sql.Types.INTEGER   int4              pg_stat_wal_receiver
//	RS> 8    last_msg_send_time    java.sql.Types.TIMESTAMP timestamptz       pg_stat_wal_receiver
//	RS> 9    last_msg_receipt_time java.sql.Types.TIMESTAMP timestamptz       pg_stat_wal_receiver
//	RS> 10   latest_end_lsn        java.sql.Types.OTHER     pg_lsn            pg_stat_wal_receiver
//	RS> 11   latest_end_time       java.sql.Types.TIMESTAMP timestamptz       pg_stat_wal_receiver
//	RS> 12   slot_name             java.sql.Types.VARCHAR   text              pg_stat_wal_receiver
//	RS> 13   sender_host           java.sql.Types.VARCHAR   text              pg_stat_wal_receiver
//	RS> 14   sender_port           java.sql.Types.INTEGER   int4              pg_stat_wal_receiver
//	RS> 15   conninfo              java.sql.Types.VARCHAR   text              pg_stat_wal_receiver

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.MEDIUM; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '"+CM_NAME+"'");

		return new CmPgWalReceiver(counterController, guiController);
	}

	public CmPgWalReceiver(ICounterController counterController, IGuiController guiController)
	{
		super(counterController,
				CM_NAME, GROUP_NAME, /*sql*/null, /*pkList*/null, 
				DIFF_COLUMNS, PCT_COLUMNS, MON_TABLES, 
				NEED_ROLES, NEED_CONFIG, NEED_SRV_VERSION, NEED_CE_VERSION, 
				NEGATIVE_DIFF_COUNTERS_TO_ZERO, IS_SYSTEM_CM, DEFAULT_POSTPONE_TIME);

		setDisplayName(SHORT_NAME);
		setDescription(HTML_DESC);

		setIconFile(GUI_ICON_FILE);

		setCounterController(counterController);
		setGuiController(guiController);
		
//		addTrendGraphs();
		
		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
//	private static final String PROP_PREFIX                         = CM_NAME;
                                                                    
//	@Override
//	protected TabularCntrPanel createGui()
//	{
//		return new CmPgWalReceiverPanel(this);
//	}
	
	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

//		pkCols.add("slot_name");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		String sql = "SELECT * from pg_stat_wal_receiver ";

		return sql;
	}

	/**
	 *  FOR PCS -- Persistent Counter Storage -- change some data types
	 */
	@Override
	public ResultSetMetaDataCached modifyResultSetMetaData(ResultSetMetaDataCached rsmdc)
	{
		for (Entry entry : rsmdc.getEntries())
		{
			String colName = entry.getColumnLabel();

			//  'status'      -- from: CLOB --->>> varchar(30)
			//  'slot_name'   -- from: CLOB --->>> varchar(30)
			//  'sender_host' -- from: CLOB --->>> varchar(30)
			if (StringUtil.equalsAny(colName, "status", "slot_name", "sender_host"))
			{
				entry.setColumnType(Types.VARCHAR);
				entry.setPrecision(30);
				entry.setColumnDisplaySize(30);
			}

			//  'conninfo'    -- from: CLOB --->>> varchar(1024)
			if (StringUtil.equalsAny(colName, "conninfo"))
			{
				entry.setColumnType(Types.VARCHAR);
				entry.setPrecision(1024);
				entry.setColumnDisplaySize(1024);
			}

			//  'receive_start_lsn' -- from: JAVA_OBJECT --->>> varchar(30)
			//  'written_lsn'       -- from: JAVA_OBJECT --->>> varchar(30)
			//  'flushed_lsn'       -- from: JAVA_OBJECT --->>> varchar(30)
			//  'latest_end_lsn'    -- from: JAVA_OBJECT --->>> varchar(30)
			if (StringUtil.equalsAny(colName, "receive_start_lsn", "written_lsn", "flushed_lsn", "latest_end_lsn"))
			{
				entry.setColumnType(Types.VARCHAR);
				entry.setPrecision(30);
				entry.setColumnDisplaySize(30);
			}
		}

		return rsmdc;
	}

//	@Override
//	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
//	{
//		try 
//		{
//			String tabname = "pg_stat_wal_receiver";
//			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
//			
//			mtd.addTable(tabname, "xxxx.");
//
//			mtd.addColumn(tabname,  "xxxxxx",               "<html>xxxxx</html>");
//		}
//		catch (NameNotFoundException e) 
//		{
//			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
//		//	System.out.println("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
//		}
//	}
}
