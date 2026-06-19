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
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.cm.CounterSample;
import com.dbxtune.cm.CounterSampleCatalogIteratorPostgres;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.config.dict.MonTablesDictionary;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmPgTableBloat
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmPgTableBloat.class.getSimpleName();
	public static final String   SHORT_NAME       = "Table Bloat";
	public static final String   HTML_DESC        = 
		"<html>" +
		"Show Table BLOAT on TABLES" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {CM_NAME};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {"extra_pct", "bloat_pct"};
	public static final String[] DIFF_COLUMNS     = new String[] {};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 600; // every 10 minute
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
//	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; } // 10 seconds is the default
	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.ALL; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '"+CM_NAME+"'");

		return new CmPgTableBloat(counterController, guiController);
	}

	public CmPgTableBloat(ICounterController counterController, IGuiController guiController)
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
		pkCols.add("schemaname");
		pkCols.add("tablename");

		return pkCols;
	}

	// RS> Col# Label         JDBC Type Name         Guessed DBMS type Source Table
	// RS> ---- ------------- ---------------------- ----------------- ------------
	// RS> 1    dbname        java.sql.Types.VARCHAR varchar(256)      -none-      
	// RS> 2    schemaname    java.sql.Types.VARCHAR varchar(256)      -none-      
	// RS> 3    tablename     java.sql.Types.VARCHAR varchar(256)      -none-      
	// RS> 4    real_size_mb  java.sql.Types.NUMERIC numeric(15,1)     -none-      
	// RS> 5    extra_size_mb java.sql.Types.NUMERIC numeric(15,1)     -none-      
	// RS> 6    extra_pct     java.sql.Types.NUMERIC numeric(10,1)     -none-      
	// RS> 7    fillfactor    java.sql.Types.INTEGER int4              -none-      
	// RS> 8    bloat_size_mb java.sql.Types.NUMERIC numeric(15,1)     -none-      
	// RS> 9    bloat_pct     java.sql.Types.NUMERIC numeric(10,1)     -none-      
	// RS> 10   is_na         java.sql.Types.BIT     bool              -none-      	

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		String name = CM_NAME;
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addTable(name, CM_NAME);
			
			mtd.addColumn(name, "dbname",         "<html>Name of the database</html>");
			mtd.addColumn(name, "schemaname",     "<html>schema of the table</html>");
			mtd.addColumn(name, "tablename",      "<html>the table name</html>");
			mtd.addColumn(name, "real_size_mb",   "<html>real size of the table in MB</html>");
			mtd.addColumn(name, "extra_size_mb",  "<html>estimated extra size in MB not used/needed in the table. This extra size is composed by the fillfactor, bloat and alignment padding spaces</html>");
			mtd.addColumn(name, "extra_pct",      "<html>estimated percentage of the real size used by extra_size</html>");
			mtd.addColumn(name, "fillfactor",     "<html>the fillfactor of the table</html>");
			mtd.addColumn(name, "bloat_size_mb",  "<html>estimated size in MB of the bloat without the extra space kept for the fillfactor</html>");
			mtd.addColumn(name, "bloat_pct",      "<html>estimated percentage of the real size used by bloat_size</html>");
			mtd.addColumn(name, "is_na",          "<html>is the estimation 'Not Applicable' ? If true, do not trust the stats</html>");
		}
		catch (NameNotFoundException e) 
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
		//	System.out.println("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
		}
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
		/* from: https://github.com/ioguix/pgsql-bloat-estimation */
		/* WARNING: executed with a non-superuser role, the query inspect only tables and materialized view (9.3+) you are granted to read.
		 * This query is compatible with PostgreSQL 9.0 and more
		 */
		String sql = ""
			    + "SELECT \n"
			    + "     CAST(current_database() as varchar(256)) AS dbname \n"
			    + "    ,CAST(schemaname         as varchar(256)) AS schemaname \n"
			    + "    ,CAST(tblname            as varchar(256)) AS tablename \n"
			    + "    ,CAST( bs*tblpages / 1024.0 / 1024.0 as numeric(15,1)) AS real_size_mb \n"
			    + "    ,CAST( (tblpages-est_tblpages)*bs / 1024.0 / 1024.0 as numeric(15,1)) AS extra_size_mb \n"
			    + "    ,CAST( \n"
			    + "        CASE WHEN tblpages > 0 AND tblpages - est_tblpages > 0 \n"
			    + "            THEN 100 * (tblpages - est_tblpages)/tblpages::float \n"
			    + "            ELSE 0 \n"
			    + "        END \n"
			    + "     as numeric(10,1)) AS extra_pct \n"
			    + "    ,fillfactor \n"
			    + "    ,CAST( \n"
			    + "        CASE WHEN tblpages - est_tblpages_ff > 0 \n"
			    + "            THEN (tblpages-est_tblpages_ff)*bs \n"
			    + "            ELSE 0 \n"
			    + "        END / 1024.0 / 1024.0 \n"
			    + "     as numeric(15,1)) AS bloat_size_mb \n"
			    + "    ,CAST( \n"
			    + "        CASE WHEN tblpages > 0 AND tblpages - est_tblpages_ff > 0 \n"
			    + "            THEN 100 * (tblpages - est_tblpages_ff)/tblpages::float \n"
			    + "            ELSE 0 \n"
			    + "        END \n"
			    + "     as numeric(10,1)) AS bloat_pct \n"
			    + "    ,is_na \n"
			    + "  -- , tpl_hdr_size, tpl_data_size, (pst).free_percent + (pst).dead_tuple_percent AS real_frag -- (DEBUG INFO) \n"
			    + "FROM ( \n"
			    + "  SELECT ceil( reltuples / ( (bs-page_hdr)/tpl_size ) ) + ceil( toasttuples / 4 ) AS est_tblpages, \n"
			    + "    ceil( reltuples / ( (bs-page_hdr)*fillfactor/(tpl_size*100) ) ) + ceil( toasttuples / 4 ) AS est_tblpages_ff, \n"
			    + "    tblpages, fillfactor, bs, tblid, schemaname, tblname, heappages, toastpages, is_na \n"
			    + "    -- , tpl_hdr_size, tpl_data_size, pgstattuple(tblid) AS pst -- (DEBUG INFO) \n"
			    + "  FROM ( \n"
			    + "    SELECT \n"
			    + "      ( 4 + tpl_hdr_size + tpl_data_size + (2*ma) \n"
			    + "        - CASE WHEN tpl_hdr_size%ma = 0 THEN ma ELSE tpl_hdr_size%ma END \n"
			    + "        - CASE WHEN ceil(tpl_data_size)::int%ma = 0 THEN ma ELSE ceil(tpl_data_size)::int%ma END \n"
			    + "      ) AS tpl_size, bs - page_hdr AS size_per_block, (heappages + toastpages) AS tblpages, heappages, \n"
			    + "      toastpages, reltuples, toasttuples, bs, page_hdr, tblid, schemaname, tblname, fillfactor, is_na \n"
			    + "      -- , tpl_hdr_size, tpl_data_size \n"
			    + "    FROM ( \n"
			    + "      SELECT \n"
			    + "        tbl.oid AS tblid, ns.nspname AS schemaname, tbl.relname AS tblname, tbl.reltuples, \n"
			    + "        tbl.relpages AS heappages, coalesce(toast.relpages, 0) AS toastpages, \n"
			    + "        coalesce(toast.reltuples, 0) AS toasttuples, \n"
			    + "        coalesce(substring( \n"
			    + "          array_to_string(tbl.reloptions, ' ') \n"
			    + "          FROM 'fillfactor=([0-9]+)')::smallint, 100) AS fillfactor, \n"
			    + "        current_setting('block_size')::numeric AS bs, \n"
			    + "        CASE WHEN version()~'mingw32' OR version()~'64-bit|x86_64|ppc64|ia64|amd64' THEN 8 ELSE 4 END AS ma, \n"
			    + "        24 AS page_hdr, \n"
			    + "        23 + CASE WHEN MAX(coalesce(s.null_frac,0)) > 0 THEN ( 7 + count(s.attname) ) / 8 ELSE 0::int END \n"
			    + "           + CASE WHEN bool_or(att.attname = 'oid' and att.attnum < 0) THEN 4 ELSE 0 END AS tpl_hdr_size, \n"
			    + "        sum( (1-coalesce(s.null_frac, 0)) * coalesce(s.avg_width, 0) ) AS tpl_data_size, \n"
			    + "        bool_or(att.atttypid = 'pg_catalog.name'::regtype) \n"
			    + "          OR sum(CASE WHEN att.attnum > 0 THEN 1 ELSE 0 END) <> count(s.attname) AS is_na \n"
			    + "      FROM pg_attribute AS att \n"
			    + "        JOIN pg_class AS tbl ON att.attrelid = tbl.oid \n"
			    + "        JOIN pg_namespace AS ns ON ns.oid = tbl.relnamespace \n"
			    + "        LEFT JOIN pg_stats AS s ON s.schemaname=ns.nspname \n"
			    + "          AND s.tablename = tbl.relname AND s.inherited=false AND s.attname=att.attname \n"
			    + "        LEFT JOIN pg_class AS toast ON tbl.reltoastrelid = toast.oid \n"
			    + "      WHERE NOT att.attisdropped \n"
			    + "        AND tbl.relkind in ('r','m') \n"
			    + "      GROUP BY 1,2,3,4,5,6,7,8,9,10 \n"
			    + "      ORDER BY 2,3 \n"
			    + "    ) AS s \n"
			    + "  ) AS s2 \n"
			    + ") AS s3 \n"
//			    + "-- WHERE NOT is_na \n"
//			    + "--   AND tblpages*((pst).free_percent + (pst).dead_tuple_percent)::float4/100 >= 1 \n"
			    + "WHERE schemaname not in('pg_catalog') \n"
//			    + "--ORDER BY bloat_size_mb DESC \n"
			    + "ORDER BY schemaname, tblname \n"
			    + "";

		return sql;
	}

	private void addTrendGraphs()
	{
	}
}
