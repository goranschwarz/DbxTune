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

import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.asetune.CounterController;
import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.alarm.AlarmHandler;
import com.asetune.alarm.AlarmHelper;
import com.asetune.alarm.events.AlarmEvent;
import com.asetune.alarm.events.AlarmEventLongRunningStatement;
import com.asetune.central.pcs.CentralPersistReader;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CmSettingsHelper.RegExpInputValidator;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.postgres.gui.CmPgActivityPanel;
import com.asetune.config.dict.PostgresWaitTypeDictionary;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.graph.TrendGraphDataPoint.LabelType;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.pcs.PcsColumnOptions;
import com.asetune.pcs.PcsColumnOptions.ColumnType;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.conn.info.DbmsVersionInfo;
import com.asetune.utils.Configuration;
import com.asetune.utils.NumberUtils;
import com.asetune.utils.StringUtil;
import com.asetune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmPgActivity
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmPgActivity.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmPgActivity.class.getSimpleName();
	public static final String   SHORT_NAME       = "Processes";
	public static final String   HTML_DESC        = 
		"<html>" +
		"Statemenets that are currently executing in the ASE." +
		"<br><br>" +
		"Table Background colors:" +
		"<ul>" +
		"    <li>GREEN       - active - The backend is executing a query.</li>" +
//		"    <li>WHITE       - idle - The backend is waiting for a new client command.</li>" +
		"    <li>YELLOW      - idle in transaction -  The backend is in a transaction, but is not currently executing a query.</li>" +
//		"    <li>PINK        - idle in transaction (aborted) -  This state is similar to idle in transaction, except one of the statements in the transaction caused an error.</li>" +
//		"    <li>XXX         - fastpath function call -  The backend is executing a fast-path function.</li>" +
//		"    <li>XXX         - disabled -  This state is reported if track_activities is disabled in this backend.</li>" +
		"    <li>PINK        - PID is Blocked by another PID from running, this PID is the Victim of a Blocking Lock, which is showned in RED.</li>" +
		"    <li>RED         - PID is Blocking other PID's from running, this PID is Responslibe or the Root Cause of a Blocking Lock.</li>" +
		"    <li>DARK BEIGE  - PID has Worker Processes Connected to it (Parent for a worker thread), but only display this for columns 'pid', 'worker_count'</li>" +
		"    <li>BEIGE       - PID is a Worker Processes</li>" +
		"</ul>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"pg_stat_activity"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {};

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

		return new CmPgActivity(counterController, guiController);
	}

	public CmPgActivity(ICounterController counterController, IGuiController guiController)
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
		
		addTrendGraphs();
		
		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	public static final String  PROPKEY_sample_sslInfo          = CM_NAME + ".sample.sslInfo";
	public static final boolean DEFAULT_sample_sslInfo          = false;
	
	public static final String GRAPH_NAME_PARALELL_WORKER_USAGE = "WorkerUsage";

	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public List<CmSettingsHelper> getLocalSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		
		list.add(new CmSettingsHelper("Get SSL Information", PROPKEY_sample_sslInfo ,Boolean.class, conf.getBooleanProperty(PROPKEY_sample_sslInfo, DEFAULT_sample_sslInfo), DEFAULT_sample_sslInfo, "Include SSL information about client connections."));

		return list;
	}

	@Override
	protected void registerDefaultValues()
	{
		super.registerDefaultValues();

		Configuration.registerDefaultValue(PROPKEY_sample_sslInfo, DEFAULT_sample_sslInfo);
	}

	private void addTrendGraphs()
	{
		//--------------------------------------------------------
		addTrendGraph(GRAPH_NAME_PARALELL_WORKER_USAGE,
				"Parallel Execution Usage", 	                // Menu CheckBox text
				"Parallel Execution Usage ("+SHORT_NAME+")", // Graph Label 
				TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
				new String[] {"Total Used Workers", "Num of Paralell Statements", "Avg Workers per Statement"}, 
				LabelType.Static, 
				TrendGraphDataPoint.Category.CPU,
				false, // is Percent Graph
				false, // visible at start
				Ver.ver(13),     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height

	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		int pos__leader_pid = findColumn("leader_pid");

		//-------------------------------------------------------
		if (GRAPH_NAME_PARALELL_WORKER_USAGE.equals(tgdp.getName()) && pos__leader_pid != -1)
		{
			double workerCount = 0;
			Map<Integer, Integer> leaderPidCountMap = new HashMap<>();  // Map<leader_pid, count>
			for (int r = 0; r < getAbsRowCount(); r++)
			{
				Integer leader_pid = getAbsValueAsInteger(r, pos__leader_pid, 0);
				if (leader_pid != 0)
				{
					workerCount++;
					Integer leaderPidEntry = leaderPidCountMap.get(leader_pid);
					if (leaderPidEntry == null)
					{
						leaderPidEntry = 0;
					}
					leaderPidEntry++;
					leaderPidCountMap.put(leader_pid, leaderPidEntry);
				}
			}
			double paralellStmntCount = leaderPidCountMap.size();
			double avgWorkersPerStmnt = 0;
			if ( ! leaderPidCountMap.isEmpty() )
				avgWorkersPerStmnt = NumberUtils.round(workerCount / (leaderPidCountMap.size()*1.0), 1);
			
			// Add
			Double[] arr = new Double[3];

			arr[0] = workerCount;
			arr[1] = paralellStmntCount;
			arr[2] = avgWorkersPerStmnt;

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}
	}

	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmPgActivityPanel(this);
	}
	
	@Override
	public Map<String, PcsColumnOptions> getPcsColumnOptions()
	{
		Map<String, PcsColumnOptions> map = super.getPcsColumnOptions();

		// No settings in the super, create one, and set it at super
		if (map == null)
		{
			map = new HashMap<>();
			map.put("query", new PcsColumnOptions(ColumnType.DICTIONARY_COMPRESSION));

			// Set the map in the super
			setPcsColumnOptions(map);
		}

		return map;
	}
	
	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("pid");

		return pkCols;
	}

//	@Override
//	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
//	{
//		String im_blocked_by_pids     = "";
//		String im_blocking_other_pids = "";
//		if (versionInfo.getLongVersion() >= Ver.ver(9, 6))
//		{
//			im_blocked_by_pids     = "    ,CAST(array_to_string(pg_blocking_pids(pid), ', ') as varchar(512)) AS im_blocked_by_pids \n";
//			im_blocking_other_pids = "    ,CAST('' as varchar(512)) AS im_blocking_other_pids \n";
//		}
//		
//		return ""
//				+ "select \n"
//				+ "     * \n"
//				+       im_blocked_by_pids
//				+       im_blocking_other_pids
//				+ "    ,CASE WHEN state != 'active' THEN NULL \n"
//				+ "          ELSE cast(((EXTRACT('epoch' from CLOCK_TIMESTAMP()) - EXTRACT('epoch' from query_start)) * 1000) as int) \n"
//				+ "     END as \"execTimeInMs\" \n"
//				+ "from pg_catalog.pg_stat_activity";
//	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		String orderBy = "ORDER BY a.pid";

		//-----------------------------
		// BEGIN: SSL Info
		//-----------------------------
		String pg_stat_ssl_join  = "";  // table: 'pg_stat_ssl' was added in version 9.5 
		String ssl_isActive      = "";  // 9.5
		String ssl_version       = "";  // 9.5
		String ssl_cipher        = "";  // 9.5
		String ssl_bits          = "";  // 9.5
		String ssl_compression   = "";  // 9.5, then removed in version 14
		String ssl_client_dn     = "";  // 9.5, in version 12 changed from 'clientdn' to 'client_dn'
		String ssl_client_serial = "";  // add in: version 12
		String ssl_issuer_dn     = "";  // add in: version 12

		boolean sampleSsl = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_sample_sslInfo, DEFAULT_sample_sslInfo);
		if (sampleSsl)
		{
			// ----- 9.5
			if (versionInfo.getLongVersion() >= Ver.ver(9, 5))
			{
				pg_stat_ssl_join = "left outer join pg_stat_ssl ssl on a.pid = ssl.pid \n";
				
				ssl_isActive      = "    ,     ssl.ssl                               AS ssl \n";
				ssl_version       = "    ,cast(ssl.version       as varchar(60))     AS ssl_version \n";
				ssl_cipher        = "    ,cast(ssl.cipher        as varchar(60))     AS ssl_cipher \n";
				ssl_bits          = "    ,     ssl.bits                              AS ssl_bits \n";
				ssl_compression   = "    ,     ssl.compression                       AS ssl_compression \n";     // removed in version 14
				ssl_client_dn     = "    ,cast(ssl.clientdn      as varchar(60))     AS ssl_client_dn \n";       // in version 12 changed from 'clientdn' to 'client_dn'
			}
			
			// ----- 12
			if (versionInfo.getLongVersion() >= Ver.ver(12))
			{
				ssl_client_dn     = "    ,cast(ssl.client_dn     as varchar(60))     AS ssl_client_dn \n";       // in version 12 changed from 'clientdn' to 'client_dn'
				ssl_client_serial = "    ,cast(ssl.client_serial as numeric(18,0))   AS sss_client_serial \n";   // add in: version 12
				ssl_issuer_dn     = "    ,cast(ssl.issuer_dn     as varchar(60))     AS sss_issuer_dn \n";       // add in: version 12
			}

			// ----- 14
			if (versionInfo.getLongVersion() >= Ver.ver(14))
			{
				ssl_compression   = "";     // removed in version 14
			}
		}
		//-----------------------------
		// END: SSL Info
		//-----------------------------
		
		
		String waiting = "    ,a.waiting \n";

		// ----- 9.4
		String backend_xid = "";
		String backend_xmin = "";
		if (versionInfo.getLongVersion() >= Ver.ver(9, 4))
		{
			backend_xid  = "    ,a.backend_xid \n";
			backend_xmin = "    ,a.backend_xmin \n";
		}

		// ----- 9.6
		String im_blocked_by_pids     = "";
		String im_blocking_other_pids = "";
		String wait_event_type        = "";
		String wait_event             = "";
		if (versionInfo.getLongVersion() >= Ver.ver(9, 6))
		{
			im_blocked_by_pids     = "    ,CAST(array_to_string(pg_blocking_pids(a.pid), ', ') as varchar(512)) AS im_blocked_by_pids \n";
			im_blocking_other_pids = "    ,CAST('' as varchar(512)) AS im_blocking_other_pids \n";

			waiting         = ""; // Waiting was removed in 9.6 and replaced by wait_event_type and wait_event
			wait_event_type = "    ,CAST(a.wait_event_type as varchar(128)) AS wait_event_type \n";
			wait_event      = "    ,CAST(a.wait_event      as varchar(128)) AS wait_event \n";
		}
		
		
		// ----- 10
		String backend_type = "";
		if (versionInfo.getLongVersion() >= Ver.ver(10))
		{
			backend_type    = "    ,CAST(a.backend_type    as varchar(128)) AS backend_type \n";
		}

		// ----- 11: No changes
		// ----- 12: No changes
		// ----- 13
		String leader_pid = "";
		String worker_count = "";
		if (versionInfo.getLongVersion() >= Ver.ver(13))
		{
			leader_pid  = "    ,a.leader_pid \n";
			orderBy = "ORDER BY COALESCE(a.leader_pid, a.pid), a.backend_start \n"; 

			worker_count    = "    ,CAST(-1 as int)                         AS worker_count \n";
		}

		// ----- 14
		String query_id = "";
		if (versionInfo.getLongVersion() >= Ver.ver(13))
		{
			query_id  = "    ,a.query_id \n";
		}

		// ----- 15: No changes

		// Construct the SQL Statement
		String sql = ""
				+ "select \n"
				+ "     a.datid \n"
				+ "    ,a.datname \n"
				+ leader_pid
				+ "    ,a.pid \n"
				+ worker_count
				+ "    ,CAST(a.state            as varchar(128)) AS state \n"
				+ backend_type
				+ waiting
				+ wait_event_type
				+ wait_event

				+ im_blocked_by_pids
				+ im_blocking_other_pids

				+ "    ,a.usesysid \n"
				+ "    ,a.usename \n"
				+ "    ,CAST(a.application_name as varchar(128)) AS application_name \n"
			    + " \n"
//				+ "    ,CASE WHEN state = 'idle' OR state IS NULL THEN -1   ELSE CAST(EXTRACT('epoch' FROM clock_timestamp()) - EXTRACT('epoch' FROM xact_start   ) as int) END AS xact_start_sec \n"
//				+ "    ,CASE WHEN state = 'idle' OR state IS NULL THEN -1   ELSE CAST(EXTRACT('epoch' FROM clock_timestamp()) - EXTRACT('epoch' FROM query_start  ) as int) END AS query_start_sec \n"
//				+ "    ,CASE WHEN state = 'idle' OR state IS NULL THEN CAST(EXTRACT('epoch' FROM query_start)       - EXTRACT('epoch' FROM clock_timestamp() ) as int) \n"
//				+ "                                               ELSE CAST(EXTRACT('epoch' FROM clock_timestamp()) - EXTRACT('epoch' FROM query_start       ) as int) END AS last_query_start_sec \n" // less than 0 -->> Inactive Statement -NumberOfSecondsSinceLastExecution
				+ "    ,CAST( CASE WHEN a.state = 'idle' OR a.state IS NULL THEN -1   ELSE COALESCE( EXTRACT('epoch' FROM clock_timestamp()) - EXTRACT('epoch' FROM a.xact_start  ), -1) END as numeric(12,1)) AS xact_start_sec \n"
				+ "    ,CAST( CASE WHEN a.state = 'idle' OR a.state IS NULL THEN -1   ELSE COALESCE( EXTRACT('epoch' FROM clock_timestamp()) - EXTRACT('epoch' FROM a.query_start ), -1) END as numeric(12,1)) AS stmnt_start_sec \n"
			    + "    ,CAST( CASE WHEN a.state = 'active' \n"
			    + "                THEN COALESCE( EXTRACT('epoch' FROM clock_timestamp()) - EXTRACT('epoch' FROM a.query_start ), -1) /* active -- query-elapsed-time */ \n"
			    + "                ELSE COALESCE( EXTRACT('epoch' FROM state_change     ) - EXTRACT('epoch' FROM a.query_start ), -1) /* else   -- last-exec-time */ \n"
			    + "           END as numeric(12,1)) AS stmnt_last_exec_sec \n"
			    + "    ,CAST( COALESCE( EXTRACT('epoch' FROM clock_timestamp()) - EXTRACT('epoch' FROM a.state_change), -1) as numeric(12,1)) AS in_current_state_sec \n"
			    + " \n"
//				+ "    ,                                                         CAST(age(clock_timestamp(), backend_start) as varchar(30))     AS backend_start_age \n"
//				+ "    ,CASE WHEN state = 'idle' OR state IS NULL THEN NULL ELSE CAST(age(clock_timestamp(), xact_start)    as varchar(30)) END AS xact_start_age \n"
//				+ "    ,CASE WHEN state = 'idle' OR state IS NULL THEN NULL ELSE CAST(age(clock_timestamp(), query_start)   as varchar(30)) END AS query_start_age \n"
//				+ "    ,CAST(age(clock_timestamp(), query_start)   as varchar(30)) AS last_query_start_age \n" // time when last query was executed
//				+ "    ,CASE WHEN state = 'idle' OR state IS NULL THEN NULL ELSE CAST(age(clock_timestamp(), state_change)  as varchar(30)) END AS state_change_age \n"
				+ "    ,CAST(                                                              clock_timestamp() - backend_start   as varchar(30)) AS backend_start_age \n"
				+ "    ,CAST( CASE WHEN a.state = 'idle' OR a.state IS NULL THEN NULL ELSE clock_timestamp() - a.xact_start  END as varchar(30)) AS xact_start_age \n"
				+ "    ,CAST( CASE WHEN a.state = 'idle' OR a.state IS NULL THEN NULL ELSE clock_timestamp() - a.query_start END as varchar(30)) AS query_start_age \n"
			    + "    ,CAST( CASE WHEN a.state = 'active' \n"
			    + "                THEN clock_timestamp() - a.query_start /* active -- query-elapsed-time */ \n"
			    + "                ELSE a.state_change    - a.query_start /* else   -- last-exec-time*/ \n"
			    + "           END                                                                                          as varchar(30)) AS stmnt_last_exec_age \n"
			    + "    ,CAST(                                                        clock_timestamp() - a.state_change    as varchar(30)) AS in_current_state_age \n"
			    + " \n"
				+ "    ,a.backend_start \n"
				+ "    ,a.xact_start \n"
				+ "    ,a.query_start \n"
				+ "    ,a.state_change \n"

				+ backend_xid
				+ backend_xmin
				+ "    ,CAST(a.client_addr      as varchar(128)) AS client_addr \n"
				+ "    ,CAST(a.client_hostname  as varchar(128)) AS client_hostname \n"
				+ "    ,     a.client_port \n"
				
				+ ssl_isActive
				+ ssl_version
				+ ssl_cipher
				+ ssl_bits
				+ ssl_compression
				+ ssl_client_dn
				+ ssl_client_serial
				+ ssl_issuer_dn

				+ query_id
				+ "    ,a.query \n"

				+ "    ,CASE WHEN a.state != 'active' OR a.state IS NULL THEN -1 \n"
				+ "          ELSE cast(((EXTRACT('epoch' from CLOCK_TIMESTAMP()) - EXTRACT('epoch' from a.query_start)) * 1000) as bigint) \n"
				+ "     END as \"execTimeInMs\" \n"

				+ "    ,CASE WHEN a.xact_start IS NULL THEN -1 \n"
				+ "          ELSE cast(((EXTRACT('epoch' from CLOCK_TIMESTAMP()) - EXTRACT('epoch' from a.xact_start)) * 1000) as bigint) \n"
				+ "     END as \"xactTimeInMs\" \n"

				+ "from pg_stat_activity a \n"
				+ pg_stat_ssl_join
				+ orderBy
				+ "";
			
		return sql;
	}

	// The below is not more needed... since we do proper CAST(...) of data types in the SELECT Statement
//	/**
//	 * Change data types (or length) for some column  
//	 * <p>
//	 * We could have done that by converting columns into varchar datatypes, but since we do: "select * from ..." 
//	 * for forward/backward compatibility, this is done in the code instead...<br>
//	 * When we switch to "column specified" SQL Statement, then we can get rid of this!  
//	 */
//	@Override
//	public ResultSetMetaDataCached createResultSetMetaData(ResultSetMetaData rsmd) throws SQLException
//	{
//		ResultSetMetaDataCached rsmdc = super.createResultSetMetaData(rsmd);
//
//		if (rsmdc == null)
//			return null;
//		
//		// In PG x.y
//		setColumnShorterLength(rsmdc, "application_name" , 60);  // text --> varchar(60)
//		setColumnShorterLength(rsmdc, "client_addr"      , 30);  // text --> varchar(30)
//		setColumnShorterLength(rsmdc, "client_hostname"  , 60);  // text --> varchar(60)
//		setColumnShorterLength(rsmdc, "state"            , 30);  // text --> varchar(30)
////		setColumnShorterLength(rsmdc, "backend_xid"      , 20);  // xid  --- Already set to varchar(30) in com.asetune.sql.ddl.DbmsDdlResolverPostgres
////		setColumnShorterLength(rsmdc, "backend_xmin"     , 20);  // xid  --- Already set to varchar(30) in com.asetune.sql.ddl.DbmsDdlResolverPostgres
//		
//		// In PG 9.6
//		setColumnShorterLength(rsmdc, "wait_event_type"  , 30);  // text --> varchar(30)
//		setColumnShorterLength(rsmdc, "wait_event"       , 50);  // text --> varchar(50)
//
//		// In PG 10
//		setColumnShorterLength(rsmdc, "backend_type"     , 30);  // text --> varchar(30)
//		
//		return rsmdc;
//	}
//
//	private void setColumnShorterLength(ResultSetMetaDataCached rsmdc, String colName, int newLength)
//	{
//		int colPos = rsmdc.findColumn(colName);
//		
//		// return if column wasn't found
//		if (colPos == -1)
//			return;
//		
//		Entry colEntry = rsmdc.getEntry(colPos);
//		if (colEntry.getPrecision() > newLength)
//		{
//			colEntry.setColumnType(Types.VARCHAR);
//			colEntry.setColumnTypeName("varchar");
//			colEntry.setPrecision(newLength);
//		}
//	}
	
	@Override
	public String getToolTipTextOnTableCell(MouseEvent e, String colName, Object cellValue, int modelRow, int modelCol) 
	{
		// query
		if ("query".equals(colName))
		{
			return cellValue == null ? null : toHtmlString(cellValue.toString());
		}

		if ("wait_event_type".equals(colName))
		{
			return cellValue == null ? null : PostgresWaitTypeDictionary.getWaitEventTypeDescription(cellValue.toString());
		}
		if ("wait_event".equals(colName))
		{
			return cellValue == null ? null : PostgresWaitTypeDictionary.getWaitEventDescription(cellValue.toString());
		}		
		
		return super.getToolTipTextOnTableCell(e, colName, cellValue, modelRow, modelCol);
	}
	/** add HTML around the string, and translate line breaks into <br> */
	private String toHtmlString(String in)
	{
		String str = StringUtil.makeApproxLineBreak(in, 150, 10, "\n");
		str = str.replaceAll("\\n", "<br>");
		if (in.indexOf("<html>")>=0 || in.indexOf("<HTML>")>=0)
			return str;
		return "<html><pre>" + str + "</pre></html>";
	}
	


	@Override
	public void localCalculation(CounterSample newSample)
	{
		int pos_pid                    = newSample.findColumn("pid");
		int pos_im_blocked_by_pids     = newSample.findColumn("im_blocked_by_pids");
		int pos_im_blocking_other_pids = newSample.findColumn("im_blocking_other_pids");

		int pos_leader_pid             = newSample.findColumn("leader_pid");
		int pos_worker_count           = newSample.findColumn("worker_count");
		
		// set: pos_im_blocking_other_pids
		if (pos_im_blocked_by_pids != -1 && pos_im_blocking_other_pids != -1)
		{
			for (int rowId=0; rowId < newSample.getRowCount(); rowId++) 
			{
				int pid = newSample.getValueAsInteger(rowId, pos_pid);

				// Get LIST of SPID's that I'm blocking
				String blockingList = getBlockingListStr(newSample, pid, pos_im_blocked_by_pids, pos_pid);
				newSample.setValueAt(blockingList, rowId, pos_im_blocking_other_pids);
			}
		}

		// set: worker_count
		if (pos_leader_pid != -1 && pos_worker_count != -1)
		{
			for (int rowId=0; rowId < newSample.getRowCount(); rowId++) 
			{
				int pid = newSample.getValueAsInteger(rowId, pos_pid);

				// Get LIST of SPID's that I'm blocking
				int worker_count = getWorkersCount(newSample, pid, pos_leader_pid);
				newSample.setValueAt(worker_count, rowId, pos_worker_count);
			}
		}
	}
	
	private String getBlockingListStr(CounterSample counters, int pid, int pos_blockingPid, int pos_pid)
	{
		Set<Integer> pidSet = null;

		// Loop on all rows
		int rows = counters.getRowCount();
		for (int rowId=0; rowId<rows; rowId++)
		{
			String str_blockingPid = counters.getValueAsString(rowId, pos_blockingPid);
			if (StringUtil.hasValue(str_blockingPid))
			{
				List<String> list_blockingPids = StringUtil.parseCommaStrToList(str_blockingPid, true);
				for (String str_blkPid : list_blockingPids)
				{
					int blkPid = StringUtil.parseInt(str_blkPid, -1);
					if (blkPid != -1)
					{
						if (blkPid == pid)
						{
							if (pidSet == null)
								pidSet = new LinkedHashSet<Integer>();

							pidSet.add( counters.getValueAsInteger(rowId, pos_pid) );
						}
					}
				}
			}
		}
		if (pidSet == null)
			return "";
		return StringUtil.toCommaStr(pidSet);
	}
	
	private int getWorkersCount(CounterSample counters, int pid, int pos_leader_pid)
	{
		int workerCount = 0;

		// Loop on all rows
		int rows = counters.getRowCount();
		for (int rowId=0; rowId<rows; rowId++)
		{
			Integer leader_pid = counters.getValueAsInteger(rowId, pos_leader_pid);
			if (leader_pid != null && leader_pid.intValue() == pid)
			{
				workerCount++;
			}
		}
		return workerCount;
	}




//	@Override
//	public void sendAlarmRequest()
//	{
//		AlarmHelper.sendAlarmRequestForColumn(this, "application_name");
//		AlarmHelper.sendAlarmRequestForColumn(this, "usename");
//	}
//	
//	@Override
//	public List<CmSettingsHelper> getLocalAlarmSettings()
//	{
//		List<CmSettingsHelper> list = new ArrayList<>();
//		
//		list.addAll( AlarmHelper.getLocalAlarmSettingsForColumn(this, "application_name") );
//		list.addAll( AlarmHelper.getLocalAlarmSettingsForColumn(this, "usename") );
//		
//		return list;
//	}

	@Override
	public void sendAlarmRequest()
	{
		AlarmHelper.sendAlarmRequestForColumn(this, "application_name");
		AlarmHelper.sendAlarmRequestForColumn(this, "usename");

		sendAlarmRequestLocal();
	}
	
	public void sendAlarmRequestLocal()
	{
		if ( ! hasDiffData() )
			return;
		
		if ( ! AlarmHandler.hasInstance() )
			return;
		
		// EXIT EARLY if no alarm properties has been specified (since there can be *many* logins)
		boolean isAnyAlarmEnabled = false;
		if (isSystemAlarmsForColumnEnabledAndInTimeRange("StatementExecInSec")) isAnyAlarmEnabled = true;
//		if (isSystemAlarmsForColumnEnabledAndInTimeRange("xxxxxxxxxxx"      )) isAnyAlarmEnabled = true;

		if (isAnyAlarmEnabled == false)
			return;

		boolean debugPrint = Configuration.getCombinedConfiguration().getBooleanProperty("sendAlarmRequest.debug", _logger.isDebugEnabled());

		AlarmHandler alarmHandler = AlarmHandler.getInstance();
		
		CountersModel cm = this;

		for (int r=0; r<cm.getDiffRowCount(); r++)
		{
			//-------------------------------------------------------
			// StatementExecInSec 
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("StatementExecInSec"))
			{
				// Only continue is "state" is "active"
				String state = cm.getDiffValue(r, "state") + "";
				if ("active".equals(state))
				{
					long execTimeInMs = -1;

					// get 'execTimeInMs'
					Object o_execTimeInMs = cm.getDiffValue(r, "execTimeInMs");
					if (o_execTimeInMs != null && o_execTimeInMs instanceof Number)
					{
						execTimeInMs = ((Number)o_execTimeInMs).longValue();
					}
//					else
//					{
//						// ok, lets 
//						Object o_query_start = cm.getDiffValue(r, "query_start");
//						if (o_query_start != null && o_query_start instanceof Timestamp)
//						{
//							// Get approximate server time here (when we last refreshed this CM)
//							execTimeInMs = this.getTimestamp().getTime() - ((Timestamp)o_query_start).getTime();
//						}
//					}
					
					long StatementExecInSec = (int)execTimeInMs / 1000;
					
					long threshold = Configuration.getCombinedConfiguration().getLongProperty(PROPKEY_alarm_StatementExecInSec, DEFAULT_alarm_StatementExecInSec);

					if (debugPrint || _logger.isDebugEnabled())
						System.out.println("##### sendAlarmRequest("+cm.getName()+"): threshold="+threshold+", StatementExecInSec='"+StatementExecInSec+"'.");

					if (StatementExecInSec > threshold)
					{
						// Get config 'skip some known values'
						String skipDbnameRegExp      = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_StatementExecInSecSkipDbname,      DEFAULT_alarm_StatementExecInSecSkipDbname);
						String skipLoginRegExp       = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_StatementExecInSecSkipLogin,       DEFAULT_alarm_StatementExecInSecSkipLogin);
						String skipCmdRegExp         = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_StatementExecInSecSkipCmd,         DEFAULT_alarm_StatementExecInSecSkipCmd);
						String skipBackendTypeRegExp = Configuration.getCombinedConfiguration().getProperty(PROPKEY_alarm_StatementExecInSecSkipBackendType, DEFAULT_alarm_StatementExecInSecSkipBackendType);

						String StatementStartTime = cm.getDiffValue(r, "query_start")  + "";
						String DBName             = cm.getDiffValue(r, "datname")      + "";
						String Login              = cm.getDiffValue(r, "usename")      + "";
						String Command            = cm.getDiffValue(r, "query")        + "";
						String backend_type       = cm.hasColumn("backend_type") ? cm.getDiffValue(r, "backend_type") + "" : "";
						
						// note: this must be set to true at start, otherwise all below rules will be disabled (it "stops" processing at first doAlarm==false)
						boolean doAlarm = true;

						// The below could have been done with nested if(!skipXxx), if(!skipYyy) doAlarm=true; 
						// Below is more readable, from a variable context point-of-view, but HARDER to understand
						doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipDbnameRegExp)      || ! DBName      .matches(skipCmdRegExp        ))); // NO match in the SKIP Cmd      regexp
						doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipLoginRegExp)       || ! Login       .matches(skipCmdRegExp        ))); // NO match in the SKIP Cmd      regexp
						doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipCmdRegExp)         || ! Command     .matches(skipCmdRegExp        ))); // NO match in the SKIP Cmd      regexp
						doAlarm = (doAlarm && (StringUtil.isNullOrBlank(skipBackendTypeRegExp) || ! backend_type.matches(skipBackendTypeRegExp))); // NO match in the SKIP TranName regexp

						// Check if "PgActiveStatements" is enabled... then THAT Alarm will hold more information (Like locks etc)... so we don't need to fire this one.
						// NOTE: To override this behavior, specify 'CmPgActivity.alarm.StatementExecInSec.override=true' in the properties file.
						CountersModel cmPgActiveStatements = CounterController.getInstance().getCmByName("CmActiveStatements");
						if (cmPgActiveStatements != null)
						{
							if (cmPgActiveStatements.isActive() && cmPgActiveStatements.isSystemAlarmsForColumnEnabledAndInTimeRange("StatementExecInSec"))
							{
								// Only disable if it's a "Client Connection" (First Version of PG that has column 'backend_type' is Version 10)
								if (cm.hasColumn("backend_type") && backend_type.equals("client backend"))
								{
									if (Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_alarm_StatementExecInSecOverride, DEFAULT_alarm_StatementExecInSecOverride))
									{
										_logger.info("The Alarm 'StatementExecInSec' could/should be fired here, but the similar alarm in CM='CmActiveStatements' is enabled and will give more information. So this alarm will be CANCELED here. If you still want/need this alarm you can set property '" + PROPKEY_alarm_StatementExecInSecOverride + "=true' to override this behaviour.");
										doAlarm = false;
									}
								}
							}
						}
						
						// NO match in the SKIP regEx
						if (doAlarm)
						{
							String extendedDescText = cm.toTextTableString(DATA_RATE, r);
							String extendedDescHtml = cm.toHtmlTableString(DATA_RATE, r, true, false, false);
													
							AlarmEvent ae = new AlarmEventLongRunningStatement(cm, threshold, StatementExecInSec, StatementStartTime, DBName, Login, Command, backend_type);
							ae.setExtendedDescription(extendedDescText, extendedDescHtml);
						
							alarmHandler.addAlarm( ae );
						}
					} // end: above threshold
				} // end: is number
			} // end: StatementExecInSec
		} // end: loop rows
	} //: end: method

	public static final String  PROPKEY_alarm_StatementExecInSec                = CM_NAME + ".alarm.system.if.StatementExecInSec.gt";
	public static final int     DEFAULT_alarm_StatementExecInSec                = 3 * 60 * 60; // 3 Hours

	public static final String  PROPKEY_alarm_StatementExecInSecSkipDbname      = CM_NAME + ".alarm.system.if.StatementExecInSec.skip.dbname";
	public static final String  DEFAULT_alarm_StatementExecInSecSkipDbname      = "";

	public static final String  PROPKEY_alarm_StatementExecInSecSkipLogin       = CM_NAME + ".alarm.system.if.StatementExecInSec.skip.login";
	public static final String  DEFAULT_alarm_StatementExecInSecSkipLogin       = "";

	public static final String  PROPKEY_alarm_StatementExecInSecSkipCmd         = CM_NAME + ".alarm.system.if.StatementExecInSec.skip.cmd";
//	public static final String  DEFAULT_alarm_StatementExecInSecSkipCmd         = "^(BACKUP |RESTORE ).*";
	public static final String  DEFAULT_alarm_StatementExecInSecSkipCmd         = "";

	public static final String  PROPKEY_alarm_StatementExecInSecSkipBackendType = CM_NAME + ".alarm.system.if.StatementExecInSec.skip.backendType";
	public static final String  DEFAULT_alarm_StatementExecInSecSkipBackendType = "^(walsender)";
	
	public static final String  PROPKEY_alarm_StatementExecInSecOverride        = CM_NAME + ".alarm.system.if.StatementExecInSec.override";
	public static final boolean DEFAULT_alarm_StatementExecInSecOverride        = false;


	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;
		
		list.add(new CmSettingsHelper("StatementExecInSec",           isAlarmSwitch, PROPKEY_alarm_StatementExecInSec               , Integer.class, conf.getIntProperty    (PROPKEY_alarm_StatementExecInSec               , DEFAULT_alarm_StatementExecInSec               ), DEFAULT_alarm_StatementExecInSec               , "If any SPID's has been executed a single SQL Statement for more than ## seconds, then send alarm 'AlarmEventLongRunningStatement'." ));
		list.add(new CmSettingsHelper("StatementExecInSec SkipDbs",                  PROPKEY_alarm_StatementExecInSecSkipDbname     , String .class, conf.getProperty       (PROPKEY_alarm_StatementExecInSecSkipDbname     , DEFAULT_alarm_StatementExecInSecSkipDbname     ), DEFAULT_alarm_StatementExecInSecSkipDbname     , "If 'StatementExecInSec' is true; Discard 'datname' listed (regexp is used)."      , new RegExpInputValidator()));
		list.add(new CmSettingsHelper("StatementExecInSec SkipLogins",               PROPKEY_alarm_StatementExecInSecSkipLogin      , String .class, conf.getProperty       (PROPKEY_alarm_StatementExecInSecSkipLogin      , DEFAULT_alarm_StatementExecInSecSkipLogin      ), DEFAULT_alarm_StatementExecInSecSkipLogin      , "If 'StatementExecInSec' is true; Discard 'usename' listed (regexp is used)."      , new RegExpInputValidator()));
		list.add(new CmSettingsHelper("StatementExecInSec SkipCommands",             PROPKEY_alarm_StatementExecInSecSkipCmd        , String .class, conf.getProperty       (PROPKEY_alarm_StatementExecInSecSkipCmd        , DEFAULT_alarm_StatementExecInSecSkipCmd        ), DEFAULT_alarm_StatementExecInSecSkipCmd        , "If 'StatementExecInSec' is true; Discard 'query' listed (regexp is used)."        , new RegExpInputValidator()));
		list.add(new CmSettingsHelper("StatementExecInSec SkipBackendType",          PROPKEY_alarm_StatementExecInSecSkipBackendType, String .class, conf.getProperty       (PROPKEY_alarm_StatementExecInSecSkipBackendType, DEFAULT_alarm_StatementExecInSecSkipBackendType), DEFAULT_alarm_StatementExecInSecSkipBackendType, "If 'StatementExecInSec' is true; Discard 'backend_type' listed (regexp is used)." , new RegExpInputValidator()));
		list.add(new CmSettingsHelper("StatementExecInSec Override",                 PROPKEY_alarm_StatementExecInSecOverride       , Boolean.class, conf.getBooleanProperty(PROPKEY_alarm_StatementExecInSecOverride       , DEFAULT_alarm_StatementExecInSecOverride       ), DEFAULT_alarm_StatementExecInSecOverride       , "if 'PgActiveStatements' is enabled, then THAT Alarm will hold more information (Like locks etc)... so we don't need to fire this one... BUT if you still want it to be fired here, set this to TRUE."));

		list.addAll( AlarmHelper.getLocalAlarmSettingsForColumn(this, "application_name") );
		list.addAll( AlarmHelper.getLocalAlarmSettingsForColumn(this, "usename") );

		return list;
	}
}
