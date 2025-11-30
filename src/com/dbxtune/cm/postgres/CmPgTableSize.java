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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.cm.CounterSample;
import com.dbxtune.cm.CounterSampleCatalogIteratorPostgres;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmPgTableSize
extends CountersModel
{
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmPgTableSize.class.getSimpleName();
	public static final String   SHORT_NAME       = "Table Size";
	public static final String   HTML_DESC        = 
		"<html>" +
		"Show how large tables are (rows, total, data, index & toast)" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"pg_class"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
			"row_estimate_diff",
			"total_kb",
			"data_kb",
			"index_kb",
			"toast_kb"
	};
	
//	RS> Col# Label             JDBC Type Name         Guessed DBMS type Source Table
//	RS> ---- ----------------- ---------------------- ----------------- ------------
//	RS> 1    oid               java.sql.Types.BIGINT  oid               pg_class    
//	RS> 2    table_schema      java.sql.Types.VARCHAR name(2147483647)  pg_namespace
//	RS> 3    table_name        java.sql.Types.VARCHAR name(2147483647)  pg_class    
//	RS> 4    row_estimate      java.sql.Types.BIGINT  int8              -none-      
//	RS> 5    row_estimate_diff java.sql.Types.BIGINT  int8              -none-      
//	RS> 6    total_kb          java.sql.Types.BIGINT  int8              -none-      
//	RS> 7    data_kb           java.sql.Types.BIGINT  int8              -none-      
//	RS> 8    index_kb          java.sql.Types.BIGINT  int8              -none-      
//	RS> 9    toast_kb          java.sql.Types.BIGINT  int8              -none-      
//	RS> 10   total_mb          java.sql.Types.BIGINT  int8              -none-      
//	RS> 11   data_mb           java.sql.Types.BIGINT  int8              -none-      
//	RS> 12   index_mb          java.sql.Types.BIGINT  int8              -none-      
//	RS> 13   toast_mb          java.sql.Types.BIGINT  int8              -none-      

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 600; // evert 10 minute
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
//	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; } // 10 seconds is the default
	@Override public int     getDefaultQueryTimeout()                 { return 30; }
	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.MEDIUM; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '"+CM_NAME+"'");

		return new CmPgTableSize(counterController, guiController);
	}

	public CmPgTableSize(ICounterController counterController, IGuiController guiController)
	{
		super(counterController, guiController,
				CM_NAME, GROUP_NAME, /*sql*/null, /*pkList*/null, 
				DIFF_COLUMNS, PCT_COLUMNS, MON_TABLES, 
				NEED_ROLES, NEED_CONFIG, NEED_SRV_VERSION, NEED_CE_VERSION, 
				NEGATIVE_DIFF_COUNTERS_TO_ZERO, IS_SYSTEM_CM, DEFAULT_POSTPONE_TIME);

		setDisplayName(SHORT_NAME);
		setDescription(HTML_DESC);

		setIconFile(GUI_ICON_FILE);

		setCounterController(counterController);
		setGuiController(guiController);
		
		addTrendGraphs();
		
		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	
//	@Override
//	protected TabularCntrPanel createGui()
//	{
//		return new CmActiveStatementsPanel(this);
//	}
	
	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("dbname");
		pkCols.add("table_schema");
		pkCols.add("table_name");

		return pkCols;
	}

	/**
	 * Create a special CounterSample, that will iterate over all databases that we will interrogate
	 */
	@Override
	public CounterSample createCounterSample(String name, boolean negativeDiffCountersToZero, String[] diffColumns, CounterSample prevSample)
	{
		List<String> fallbackDbList = Arrays.asList( new String[]{"postgres"} );
		return new CounterSampleCatalogIteratorPostgres(name, negativeDiffCountersToZero, diffColumns, prevSample, fallbackDbList);
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		// below is grabbed from https://wiki.postgresql.org/wiki/Disk_Usage
		// and then changed a bit
		return 
			"SELECT \n" +
			"      current_database() as dbname \n" +
			"    , oid \n" +
			"    , table_schema \n" +
			"    , table_name \n" +
			"    , row_estimate \n" +
			"    , row_estimate                as row_estimate_diff \n" +
			"    , total_bytes / 1024          as total_kb \n" +
			"    , table_bytes / 1024          as data_kb \n" +
			"    , index_bytes / 1024          as index_kb  \n" +
			"    , toast_bytes / 1024          as toast_kb \n" +
			"    , total_bytes / 1024 / 1024   as total_mb \n" +
			"    , table_bytes / 1024 / 1024   as data_mb  \n" +
			"    , index_bytes / 1024 / 1024   as index_mb  \n" +
			"    , toast_bytes / 1024 / 1024   as toast_mb \n" +
//			"    , pg_size_pretty(total_bytes) AS TOTAL_pretty \n" +
//			"    , pg_size_pretty(table_bytes) AS DATA_pretty \n" +
//			"    , pg_size_pretty(index_bytes) AS INDEX_pretty \n" +
//			"    , pg_size_pretty(toast_bytes) AS TOAST_pretty \n" +
			"  FROM ( \n" +
			"    SELECT *, total_bytes-index_bytes-COALESCE(toast_bytes,0) AS table_bytes  \n" +
			"    FROM ( \n" +
			"      SELECT c.oid \n" +
			"          , nspname                               AS table_schema \n" +
			"          , relname                               AS table_name \n" +
			"          , cast(c.reltuples as int8)             AS row_estimate \n" +
			"          , pg_total_relation_size(c.oid)         AS total_bytes \n" +
			"          , pg_indexes_size(c.oid)                AS index_bytes \n" +
			"          , pg_total_relation_size(reltoastrelid) AS toast_bytes \n" +
			"      FROM pg_class c \n" +
			"      LEFT JOIN pg_namespace n ON n.oid = c.relnamespace \n" +
			"      WHERE relkind = 'r' \n" +
			"        AND n.nspname NOT IN('information_schema', 'pg_catalog', 'pg_toast') \n" +
			"  ) a \n" +
			") a \n" +
			"";
	}

	private void addTrendGraphs()
	{
	}
}
