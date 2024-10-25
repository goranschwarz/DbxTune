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
package com.asetune.cm.postgres;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.central.pcs.CentralPersistReader;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CounterSampleCatalogIteratorPostgres;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CounterTableModel;
import com.asetune.cm.CountersModel;
import com.asetune.cm.postgres.gui.CmPgTablesPanel;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.graph.TrendGraphDataPoint.LabelType;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.conn.info.DbmsVersionInfo;
import com.asetune.utils.Configuration;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmPgTables
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmPgTables.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmPgTables.class.getSimpleName();
	public static final String   SHORT_NAME       = "Table Info";
	public static final String   HTML_DESC        = 
		"<html>" +
		"One row for each table in the current database, showing statistics about accesses to that specific table." +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"pg_stat_all_tables"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] { "table_scan_pct", "index_usage_pct" };
	public static final String[] DIFF_COLUMNS     = new String[] {
			"seq_scan",
			"seq_tup_read",
			"idx_scan",
			"idx_tup_fetch",
			"n_tup_ins",
			"n_tup_upd",
			"n_tup_del",
			"n_tup_hot_upd",
			"n_tup_newpage_upd",
			"n_live_tup",
			"n_dead_tup",
			"n_mod_since_analyze",
			"n_ins_since_vacuum",
			"vacuum_count",
			"autovacuum_count",
			"analyze_count",
			"autoanalyze_count"			
	};
//	RS> Col# Label               JDBC Type Name           Guessed DBMS type
//	RS> ---- ------------------- ------------------------ -----------------
//	RS> 1    relid               java.sql.Types.BIGINT    oid              
//	RS> 2    schemaname          java.sql.Types.VARCHAR   name(2147483647) 
//	RS> 3    relname             java.sql.Types.VARCHAR   name(2147483647) 
//	RS> 4    seq_scan            java.sql.Types.BIGINT    int8             
//	RS> 5    seq_tup_read        java.sql.Types.BIGINT    int8             
//	RS> 6    idx_scan            java.sql.Types.BIGINT    int8             
//	RS> 7    idx_tup_fetch       java.sql.Types.BIGINT    int8             
//	RS> 8    n_tup_ins           java.sql.Types.BIGINT    int8             
//	RS> 9    n_tup_upd           java.sql.Types.BIGINT    int8             
//	RS> 10   n_tup_del           java.sql.Types.BIGINT    int8             
//	RS> 11   n_tup_hot_upd       java.sql.Types.BIGINT    int8             
//	RS> 12   n_live_tup          java.sql.Types.BIGINT    int8             
//	RS> 13   n_dead_tup          java.sql.Types.BIGINT    int8             
//	RS> 14   n_mod_since_analyze java.sql.Types.BIGINT    int8             
//	RS> 15   last_vacuum         java.sql.Types.TIMESTAMP timestamptz      
//	RS> 16   last_autovacuum     java.sql.Types.TIMESTAMP timestamptz      
//	RS> 17   last_analyze        java.sql.Types.TIMESTAMP timestamptz      
//	RS> 18   last_autoanalyze    java.sql.Types.TIMESTAMP timestamptz      
//	RS> 19   vacuum_count        java.sql.Types.BIGINT    int8             
//	RS> 20   autovacuum_count    java.sql.Types.BIGINT    int8             
//	RS> 21   analyze_count       java.sql.Types.BIGINT    int8             
//	RS> 22   autoanalyze_count   java.sql.Types.BIGINT    int8             
	
	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
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

		return new CmPgTables(counterController, guiController);
	}

	public CmPgTables(ICounterController counterController, IGuiController guiController)
	{
		super(counterController,
				CM_NAME, GROUP_NAME, /*sql*/null, /*pkList*/null, 
				DIFF_COLUMNS, PCT_COLUMNS, MON_TABLES, 
				NEED_ROLES, NEED_CONFIG, NEED_SRV_VERSION, NEED_CE_VERSION, 
				NEGATIVE_DIFF_COUNTERS_TO_ZERO, IS_SYSTEM_CM, DEFAULT_POSTPONE_TIME);

		setDisplayName(SHORT_NAME);
		setDescription(HTML_DESC);

		setIconFile(GUI_ICON_FILE);

//		addCmDependsOnMe(CmPgLocks.CM_NAME); // go and check CmPgLocks if it needs refresh as part of this refresh (if CmPgLocks needs to be refreshed, lets refresh since he depends on me)

		setCounterController(counterController);
		setGuiController(guiController);
		
		addTrendGraphs();
		
		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	private static final String  PROP_PREFIX                          = CM_NAME;

	public static final String  PROPKEY_sample_systemTables           = PROP_PREFIX + ".sample.systemTables";
	public static final boolean DEFAULT_sample_systemTables           = false;

	public static final String GRAPH_NAME_DEAD_ROWS                   = "DeadRows";

	@Override
	protected void registerDefaultValues()
	{
		super.registerDefaultValues();

		Configuration.registerDefaultValue(PROPKEY_sample_systemTables,           DEFAULT_sample_systemTables);
	}
	
	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public List<CmSettingsHelper> getLocalSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		
		list.add(new CmSettingsHelper("Sample System Tables", PROPKEY_sample_systemTables , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_systemTables  , DEFAULT_sample_systemTables  ), DEFAULT_sample_systemTables, "Sample System Tables" ));

		return list;
	}


	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_DEAD_ROWS,
			"Number of Dead Rows per Database", 	                // Menu CheckBox text
			"Number of Dead Rows per Database(n_dead_tup) per Database ("+SHORT_NAME+")", // Graph Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, LabelType.Dynamic, 
			TrendGraphDataPoint.Category.OPERATIONS,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		if (GRAPH_NAME_DEAD_ROWS.equals(tgdp.getName()))
		{
			// loop all rows and add "n_dead_tup" to a Map
			Map<String, Integer> dbMap = new LinkedHashMap<>();

			CounterTableModel data = getCounterDataAbs();
			
			int dbname_pos     = data.findColumn("dbname");
			int n_dead_tup_pos = data.findColumn("n_dead_tup");
			
			if (dbname_pos != -1 && n_dead_tup_pos != -1)
			{
				// Loop all rows, and add value to "dbMap"
				int rc = data.getRowCount();
				for (int rowId = 0; rowId < rc; rowId++)
				{
					String dbname      = data.getValueAt(rowId, dbname_pos) + "";
					int    n_dead_tup  = ((Number)data.getValueAt(rowId, n_dead_tup_pos )).intValue();

					Integer val = dbMap.get(dbname);
					if (val == null)
						val = 0;

					val += n_dead_tup;
					
					dbMap.put(dbname, val);
				}

				// XXXXXXXXXX
				Double[] dArray = new Double[dbMap.size() + 1 ]; // add one for ALL Tables (or sum)
				String[] lArray = new String[dArray.length];

				int ap = 1;
				double sum = 0;
				for (Entry<String, Integer> entry : dbMap.entrySet())
				{
					String dbname = entry.getKey();
					double val = entry.getValue().doubleValue();
					
					sum += val;
					
					lArray[ap] = dbname;
					dArray[ap] = val;

					ap++;
				}
				
				lArray[0] = "ALL-DBs";
				dArray[0] = sum;

				// Set the values
				tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);

			} //end: if we have columns
		} // end: graph
	} // end: method

	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmPgTablesPanel(this);
	}


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
		pkCols.add("relname");

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
		String tabName = "pg_catalog.pg_stat_user_tables";

		// Sample System Tables
		if (Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_sample_systemTables, DEFAULT_sample_systemTables))
		{
			tabName = "pg_catalog.pg_stat_all_tables";
		}

		return 
			"select \n" +
			"    current_database() AS dbname, \n" +
			"    cast(100.0 * coalesce(seq_scan, 0) / (coalesce(seq_scan, 0) + coalesce(idx_scan, 0)) as numeric(5,1)) AS table_scan_pct, \n" + 
			"    cast(100.0 * coalesce(idx_scan, 0) / (coalesce(seq_scan, 0) + coalesce(idx_scan, 0)) as numeric(5,1)) AS index_usage_pct, \n" +
			"    pg_total_relation_size(relid)/1024 AS total_kb, \n" +
			"    pg_table_size         (relid)/1024 AS data_kb, \n" +
			"    pg_indexes_size       (relid)/1024 AS index_kb, \n" +
			"    cast(CASE WHEN seq_scan > 0 THEN (seq_tup_read  * 1.0) / seq_scan ELSE 0 END as numeric(15,1)) AS seq_tup_read_per_scan, \n" +
			"    cast(CASE WHEN idx_scan > 0 THEN (idx_tup_fetch * 1.0) / idx_scan ELSE 0 END as numeric(15,1)) AS idx_tup_fetch_per_scan, \n" +
			"    * \n" +
//			"    ,pg_relation_filepath(relid) as table_file_path \n" +
			"from " + tabName + "\n" +
			"where (coalesce(seq_scan, 0) + coalesce(idx_scan, 0)) > 0"
			;
	}

//	/**
//	 * Special method called from DbmsObjectIdCache, when Objects can't be found in the cache
//	 * <p>
//	 * This instead of connecting to the DBMS and get info from there<br>
//	 * NOTE: SchemaID's are not known, so it will be set to -1 
//	 * 
//	 * @param dbnameToLookup
//	 * @param lookupId
//	 * @return
//	 */
//	public ObjectInfo getObjectIdCacheEntry(String dbnameToLookup, Number lookupId)
//	{
//		// Exit early if no cache
//		if ( ! DbmsObjectIdCache.hasInstance() )
//			return null;
//			
//		int relid_pos       = findColumn("relid");
//		int dbname_pos      = findColumn("dbname");
//		int schema_name_pos = findColumn("schemaname");
//		int table_name_pos  = findColumn("relname");
//		
//		// Loop all rows, and find matching REL-ID and DBNAME
//		int rowc = getRowCount();
//		for (int rowId = 0; rowId < rowc; rowId++)
//		{
//			String dbname = getAbsString(rowId, dbname_pos);
//			Long   relid  = getAbsValueAsLong(rowId, relid_pos);
//			if (dbname != null && relid != null)
//			{
//				if (relid.equals(lookupId) && dbname.equals(dbnameToLookup))
//				{
//					String schema_name = getAbsString(rowId, schema_name_pos);
//					String table_name  = getAbsString(rowId, table_name_pos);
//					
//					Long dbid = DbmsObjectIdCache.getInstance().getDbid(dbname);
//					long schemaId = -1;
//
//					if (dbid != null)
//					{
//						ObjectInfo objectInfo = new ObjectInfo(dbid, dbname, schemaId, schema_name, relid, table_name);
//
//						return objectInfo;
//					}
//				}
//			}
//		}
//		return null;
//	}
//	
//	@Override
//	public void localCalculation(CounterSample newSample)
//	{
//		// Exit early - If the cache isn't available
//		if ( ! DbmsObjectIdCache.hasInstance() )
//		{
//			return;
//		}
//		
//		DbmsObjectIdCache dbmsObjectIdCache = DbmsObjectIdCache.getInstance();
//
//		// If there is NO "table" objects in the cache... add entries
//		if ( ! dbmsObjectIdCache.hasObjectIds() )
//		{
//			int addCount = 0;
//
//			int relid_pos       = newSample.findColumn("relid");
//			int dbname_pos      = newSample.findColumn("dbname");
//			int schema_name_pos = newSample.findColumn("schemaname");
//			int table_name_pos  = newSample.findColumn("relname");
//			
//			// No need to continue if we havn't got the columns we need
//			if (relid_pos == -1 || dbname_pos == -1 || schema_name_pos == -1 || table_name_pos == -1)
//			{
//				_logger.info("localCalculation(): Desired columns not available (relid_pos="+relid_pos+", dbname_pos="+dbname_pos+", schema_name_pos="+schema_name_pos+", table_name_pos="+table_name_pos+"), can't resolv 'database:id' and 'relation:id' into real names.");
//				return;
//			}
//
//			// Loop all rows, and ADD entries
//			int rowc = newSample.getRowCount();
//			for (int rowId = 0; rowId < rowc; rowId++)
//			{
//				String dbname = newSample.getValueAsString(rowId, dbname_pos);
//				Long   relid  = newSample.getValueAsLong(rowId, relid_pos);
//
//				if (dbname != null && relid != null)
//				{
//					String schema_name = newSample.getValueAsString(rowId, schema_name_pos);
//					String table_name  = newSample.getValueAsString(rowId, table_name_pos);
//						
//					Long dbid = DbmsObjectIdCache.getInstance().getDbid(dbname);
//					long schemaId = -1;
//
//					if (dbid != null)
//					{
//						// Create a new Object
//						ObjectInfo objectInfo = new ObjectInfo(dbid, dbname, schemaId, schema_name, relid, table_name);
//
//						// ADD it to the Cache.
//						dbmsObjectIdCache.setObjectInfo(dbid, relid, objectInfo);
//
//						addCount++;
//					}
//				}
//			}
//			
//			_logger.info("localCalculation(): Populated " + addCount + " entries to DbmsObjectIdCache.");
//		}
//	}
}
