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
package com.asetune.cm.sqlserver;

import java.awt.Window;
import java.awt.event.MouseEvent;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.naming.NameNotFoundException;

import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.alarm.AlarmHandler;
import com.asetune.alarm.events.AlarmEvent;
import com.asetune.alarm.events.sqlserver.AlarmEventAgLogSendQueueSize;
import com.asetune.alarm.events.sqlserver.AlarmEventAgRoleChange;
import com.asetune.alarm.events.sqlserver.AlarmEventAgSecondaryCommitTimeLag;
import com.asetune.alarm.events.sqlserver.AlarmEventAgSplitBrain;
import com.asetune.alarm.events.sqlserver.AlarmEventAgUnexpectedState;
import com.asetune.alarm.events.sqlserver.AlarmEventBagRolePercentSkewed;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.sqlserver.gui.CmAlwaysOnPanel;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.graph.TrendGraphDataPoint.LabelType;
import com.asetune.gui.DbSelectionForGraphsDialog;
import com.asetune.gui.MainFrame;
import com.asetune.gui.ResultSetTableModel;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.gui.swing.PromptForPassword;
import com.asetune.gui.swing.PromptForPassword.SaveType;
import com.asetune.sql.JdbcUrlParser;
import com.asetune.sql.conn.ConnectionProp;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.conn.DbxConnectionPool;
import com.asetune.sql.conn.DbxConnectionPoolMap;
import com.asetune.utils.Configuration;
import com.asetune.utils.JsonUtils;
import com.asetune.utils.OpenSslAesUtil;
import com.asetune.utils.OpenSslAesUtil.DecryptionException;
import com.asetune.utils.StringUtil;
import com.asetune.utils.TimeUtils;
import com.asetune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmAlwaysOn
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmAlwaysOn.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmAlwaysOn.class.getSimpleName();
	public static final String   SHORT_NAME       = "AlwaysOn";
	public static final String   HTML_DESC        = 
		"<html>" +
		"Information about Availability Groups (AlwasyOn)." +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {
			"CmAlwaysOn", 
			"availability_replicas", 
			"availability_groups", 
			"availability_databases_cluster", 
			"dm_hadr_availability_group_states", 
			"dm_hadr_availability_replica_states", 
			"dm_hadr_database_replica_states",
			"dm_db_log_stats"};
	public static final String[] NEED_ROLES       = new String[] {"VIEW SERVER STATE", "VIEW ANY DEFINITION"};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
			"xxxxx"
	};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = false;
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

		return new CmAlwaysOn(counterController, guiController);
	}

	public CmAlwaysOn(ICounterController counterController, IGuiController guiController)
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
		
		addDependsOnCm(CmPerfCounters.CM_NAME); // CmPerfCounters must have been executed before this cm

		addTrendGraphs();
		
		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	private static final String  PROP_PREFIX                         = CM_NAME;

	public static final String  PROPKEY_update_primary               = PROP_PREFIX + ".update.primary";
	public static final boolean DEFAULT_update_primary               = true;
	
	public static final String  PROPKEY_update_primaryIntervalInSec  = PROP_PREFIX + ".update.primary.intervalInSec";
	public static final long    DEFAULT_update_primaryIntervalInSec  = 300;

	public static final String  PROPKEY_show_RemoteRows              = PROP_PREFIX + ".show.remote.rows";
	public static final boolean DEFAULT_show_RemoteRows              = true;

	public static final String  PROPKEY_sample_liveRemoteData        = PROP_PREFIX + ".sample.live.remoteData";
	public static final boolean DEFAULT_sample_liveRemoteData        = true;

	public static final String  PROPKEY_sample_liveRemoteDataPerfCnt = PROP_PREFIX + ".sample.live.remoteData.perfCounters";
	public static final boolean DEFAULT_sample_liveRemoteDataPerfCnt = true;

	public static final String  PROPKEY_show_liveRemoteData          = PROP_PREFIX + ".show.live.remoteData";
	public static final boolean DEFAULT_show_liveRemoteData          = true;

	public static final String COLVAL_LOCALITY_LOCAL                 = "LOCAL";
	public static final String COLVAL_LOCALITY_REMOTE                = "REMOTE";
	public static final String COLVAL_LOCALITY_REMOTE_LIVE_DATA      = "REMOTE-LIVE-DATA";
	public static final int    COLPOS_LOCALITY                       = 0;
	
	public static final String PRIMARY_ROLE   = "PRIMARY"; 
	public static final String SECONDARY_ROLE = "SECONDARY"; 
	public static final String RESOLVING_ROLE = "RESOLVING";
	
	// This holds the "previous" sample ROLE_DESC... which can be used to check if ROLE has been changed from 1 same to the other.
	// This Map is used and maintained by the method: sendAlarmRequest()
	private Map<String, String> _previousSampleRole = new HashMap<>();

	// This holds CmPerfCounters for each of the SECONDARY Servers (if this is configured)
	private Map<String, CmPerfCounters> _secondaryServerPerfCounterMap = new HashMap<>();

	// Set in localCalculation
	private String _localSrvName = "";

	public static final String GRAPH_NAME_WRITE_TRANS         = "WriteTrans";
	public static final String GRAPH_NAME_BYTES_TO_REPLICA    = "BytesToReplica";
	public static final String GRAPH_NAME_RECOVERY_QUEUE      = "RecoveryQueue";


	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_WRITE_TRANS,
				"AlwaysOn Write Transactions Per Sec",        // Menu CheckBox text
				"AlwaysOn Write Transactions Per Sec ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
				TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL,
				null, 
				LabelType.Dynamic,
				TrendGraphDataPoint.Category.REPLICATION,
				false, // is Percent Graph
				false, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height

		addTrendGraph(GRAPH_NAME_BYTES_TO_REPLICA,
				"AlwaysOn Bytes Sent to Replica Per Sec",        // Menu CheckBox text
				"AlwaysOn Bytes Sent to Replica Per Sec ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
				TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_BYTES,
				null, 
				LabelType.Dynamic,
				TrendGraphDataPoint.Category.REPLICATION,
				false, // is Percent Graph
				false, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height

		addTrendGraph(GRAPH_NAME_RECOVERY_QUEUE,
				"AlwaysOn Recovery Queue, # Log Records",        // Menu CheckBox text
				"AlwaysOn Recovery Queue, # Log Records ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
				TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL,
				null, 
				LabelType.Dynamic,
				TrendGraphDataPoint.Category.REPLICATION,
				false, // is Percent Graph
				false, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height
	}
	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		if (GRAPH_NAME_WRITE_TRANS.equals(tgdp.getName()))
		{
			HashMap<String, Object> whereClause = new HashMap<>(2);
			whereClause.put("locality",       COLVAL_LOCALITY_LOCAL);
			whereClause.put("role_desc",      PRIMARY_ROLE);

			List<Integer> rowIds = getAbsRowIdsWhere(whereClause);

			if (rowIds.isEmpty())
				return;

			// Write 1 "line" for every database
			Double[] dArray = new Double[rowIds.size()];
			String[] lArray = new String[rowIds.size()];
			int d = 0;
			for (int row : rowIds)
			{
//				String server_name = this.getAbsString       (row, "server_name");
				String dbname      = this.getAbsString       (row, "database_name");
				Double dvalue      = this.getAbsValueAsDouble(row, "p_MirroredWriteTransactionsPerSec");

				lArray[d] = dbname;
				dArray[d] = dvalue;
				d++;
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
		}

		if (GRAPH_NAME_BYTES_TO_REPLICA.equals(tgdp.getName()))
		{
			HashMap<String, Object> whereClause = new HashMap<>(2);
			whereClause.put("locality",       COLVAL_LOCALITY_REMOTE);
			whereClause.put("role_desc",      SECONDARY_ROLE);

			List<Integer> rowIds = getAbsRowIdsWhere(whereClause);

			if (rowIds.isEmpty())
				return;

			// Write 1 "line" for every database
			Double[] dArray = new Double[rowIds.size()];
			String[] lArray = new String[rowIds.size()];
			int d = 0;
			for (int row : rowIds)
			{
				String server_name = this.getAbsString       (row, "server_name");
				String ag_name     = this.getAbsString       (row, "ag_name");
				Double dvalue      = this.getAbsValueAsDouble(row, "p2s_BytesSentToReplicaPerSec");

				lArray[d] = ag_name + ":" + server_name;
				dArray[d] = dvalue;
				d++;
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
		}

		if (GRAPH_NAME_RECOVERY_QUEUE.equals(tgdp.getName()))
		{
			HashMap<String, Object> whereClause = new HashMap<>(2);
			whereClause.put("locality",       COLVAL_LOCALITY_REMOTE_LIVE_DATA);
			whereClause.put("role_desc",      SECONDARY_ROLE);

			List<Integer> rowIds = getAbsRowIdsWhere(whereClause);

			if (rowIds.isEmpty())
			{
				// Well we might be on the secondary server... 
				whereClause.clear();
				whereClause.put("locality",       COLVAL_LOCALITY_LOCAL);
				whereClause.put("role_desc",      SECONDARY_ROLE);

				rowIds = getAbsRowIdsWhere(whereClause);
				
				if (rowIds.isEmpty())
					return;
			}

			// Write 1 "line" for every database
			Double[] dArray = new Double[rowIds.size()];
			String[] lArray = new String[rowIds.size()];
			int d = 0;
			for (int row : rowIds)
			{
				String server_name = this.getAbsString       (row, "server_name");
				String dbname      = this.getAbsString       (row, "database_name");
				Double dvalue      = this.getAbsValueAsDouble(row, "s_RecoveryQueue");

				lArray[d] = server_name + ":" + dbname;
				dArray[d] = dvalue;
				d++;
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
		}
	}
		
	@Override
	public Class<?> getColumnClass(int columnIndex)
	{
		// use CHECKBOX for column "HasShowPlan"
		String colName = getColumnName(columnIndex);

		if      ("basic_features".equals(colName)) return Boolean.class;
		else if ("db_failover"   .equals(colName)) return Boolean.class;
		else return super.getColumnClass(columnIndex);
	}

	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmAlwaysOnPanel(this);
	}

	@Override
	protected void registerDefaultValues()
	{
		super.registerDefaultValues();

		Configuration.registerDefaultValue(PROPKEY_update_primary              , DEFAULT_update_primary);
		Configuration.registerDefaultValue(PROPKEY_update_primaryIntervalInSec , DEFAULT_update_primaryIntervalInSec);
		Configuration.registerDefaultValue(PROPKEY_sample_liveRemoteData       , DEFAULT_sample_liveRemoteData);
		Configuration.registerDefaultValue(PROPKEY_sample_liveRemoteDataPerfCnt, DEFAULT_sample_liveRemoteDataPerfCnt);
	}
	
	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public List<CmSettingsHelper> getLocalSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		
		list.add(new CmSettingsHelper("Update Primary DB",                     PROPKEY_update_primary              , Boolean.class, conf.getBooleanProperty(PROPKEY_update_primary               , DEFAULT_update_primary               ), DEFAULT_update_primary              , "Update Active DB" ));
		list.add(new CmSettingsHelper("Update Primary DB Interval",            PROPKEY_update_primaryIntervalInSec , Long   .class, conf.getLongProperty   (PROPKEY_update_primaryIntervalInSec  , DEFAULT_update_primaryIntervalInSec  ), DEFAULT_update_primaryIntervalInSec , "Update Active DB, Every X second." ));

		list.add(new CmSettingsHelper("Sample Live Remote Data",               PROPKEY_sample_liveRemoteData       , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_liveRemoteData        , DEFAULT_sample_liveRemoteData        ), DEFAULT_sample_liveRemoteData       , "Fetch Live Data from SECONDARY Server, so we can check for 'SPLIT-BRAIN' (role PRIMARY in more than one instance) or similar issues." ));
		list.add(new CmSettingsHelper("Sample Live Remote Data Perf Counters", PROPKEY_sample_liveRemoteData       , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_liveRemoteDataPerfCnt , DEFAULT_sample_liveRemoteDataPerfCnt ), DEFAULT_sample_liveRemoteDataPerfCnt, "Fetch Live Data (PerfCounters) from SECONDARY Server, so we evaluate Remote Performance Counters on the local server side. NOTE: This also needs 'Sample Live Remote Data'." ));

		return list;
	}

	@Override
	public void addMonTableDictForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addTable("CmAlwaysOn", "Statistics for AlwaysOn or Availability Groups");

			mtd.addColumn("CmAlwaysOn", "locality                             ".trim(),  "<html>Is this entry a 'LOCAL' or a 'REMOTE' entry. <br><b>Formula</b>: CASE WHEN ars.is_local = 1 THEN 'LOCAL' ELSE 'REMOTE' END </html>");
			mtd.addColumn("CmAlwaysOn", "server_name                          ".trim(),  "<html>What server name does this entry hold information for.</html>");
			mtd.addColumn("CmAlwaysOn", "ag_name                              ".trim(),  "<html>Availability Group Name</html>");
			mtd.addColumn("CmAlwaysOn", "Validated                            ".trim(),  "<html>If 'Sample Live Remote Data' is enabled. Then we can validate the 'role_desc' with the '"+COLVAL_LOCALITY_REMOTE_LIVE_DATA+"' records."
			                                                                               + "The statuses mey be:<table>"
			                                                                               + "  <tr> <td>&bull;</td> <td> <b>YES        </b> </td> <td> 'LOCAL' and '"+COLVAL_LOCALITY_REMOTE_LIVE_DATA+"' doesn't have the same 'role_desc'      </td></tr>"
			                                                                               + "  <tr> <td>&bull;</td> <td> <b>-          </b> </td> <td> 'REMOTE' records doesn't need to be validated...                                     </td></tr>"
			                                                                               + "  <tr> <td>&bull;</td> <td> <b>INVALID    </b> </td> <td> 'LOCAL' and '"+COLVAL_LOCALITY_REMOTE_LIVE_DATA+"'  have the <b>same</b> 'role_desc'      </td></tr>"
			                                                                               + "  <tr> <td>&bull;</td> <td> <b>SPLIT-BRAIN</b> </td> <td> 'LOCAL' and '"+COLVAL_LOCALITY_REMOTE_LIVE_DATA+"' have <b>PRIMARY</b> at the 'role_desc' </td></tr>"
			                                                                               + "  <tr> <td>&bull;</td> <td> <b>NULL       </b> </td> <td> Not even validated ('Sample Live Remote Data' is <b>disabled</b>.                    </td></tr>"
			                                                                               + "</table></html>");
			mtd.addColumn("CmAlwaysOn", "BagPct                               ".trim(),  "<html>Percent of the Logical BAG (Basic Availability Group) which has the same 'role_desc'. The logical group is prefix of a group name (prefix is chars before first '_' or '-' character. <br>"
			                                                                               + "If the value is <b>NOT</b> at 100%, it simply means that the 'role_desc' (PRIMARY/SECONDARY/RESOLVING) is not equaly distributed acros the logical group.<br>"
			                                                                               + "Which means: That some database or databases (within the logical group) is PRIMARY at the wrong location, which might be considdered as a <i>SPLIT-BRAIN</i> but on a <b>logical level</b>.<br>"
			                                                                               + "<br>"
			                                                                               + "<b>NOTE:</b> Hover over the cell to get details of what databases are considdered to be in this group, etc...<br>"
			                                                                               + "</html>");
			mtd.addColumn("CmAlwaysOn", "BagPercentDetails                    ".trim(),  "<html>Percent of the Logical BAG (Basic Availability Group) which has the same 'role_desc'. The logical group is prefix of a group name (prefix is chars before first '_' or '-' character. <br>"
			                                                                               + "If the value is <b>NOT</b> at 100%, it simply means that the 'role_desc' (PRIMARY/SECONDARY/RESOLVING) is not equaly distributed acros the logical group.<br>"
			                                                                               + "Which means: That some database or databases (within the logical group) is PRIMARY at the wrong location, which might be considdered as a <i>SPLIT-BRAIN</i> but on a <b>logical level</b>.<br>"
			                                                                               + "</html>");
			mtd.addColumn("CmAlwaysOn", "DbUseCount                           ".trim(),  "<html>Number of users that are actually using this database (or number of sessions that has this db as it's Current Working Database)<br>Note: -1 = Not Applicable</html>");
			mtd.addColumn("CmAlwaysOn", "PRIMARY_server_name                  ".trim(),  "<html>The server name which holds the PRIMARY database.</html>");
			mtd.addColumn("CmAlwaysOn", "AvailabilityModeConfig               ".trim(),  "<html>Sama as 'availability_mode_desc' <br>"
			                                                                               + "<p>Description of availability_mode, one of:<br><br>"
			                                                                               + "ASYNCHRONOUS_COMMIT -- The primary replica can commit transactions without waiting for the secondary to write the log to disk.<br>"
			                                                                               + "SYNCHRONOUS_COMMIT  -- The primary replica waits to commit a given transaction until the secondary replica has written the transaction to disk.<br>"
			                                                                               + "CONFIGURATION_ONLY  -- The primary replica sends availability group configuration metadata to the replica synchronously. User data is not transmitted to the replica. Available in SQL Server 2017 CU1 and later.<br>"
			                                                                               + "<br>"
			                                                                               + "To change this the availability mode of an availability replica, use the AVAILABILITY_MODE option of ALTER AVAILABILITY GROUPTransact-SQL statement.<br>"
			                                                                               + "You cannot change the availability mode of a replica to CONFIGURATION_ONLY. You cannot change a CONFIGURATION_ONLY replica to a secondary or primary replica."
			                                                                               + "</p></html>");
			mtd.addColumn("CmAlwaysOn", "SecondaryCommitTimeLag               ".trim(),  "<html>"
			                                                                               + "This is only reported when we are logged in and monitoring a PRIMARY database<br>"
			                                                                               + "When 'role_desc' is 'SECONDARY':"
			                                                                               + "<ul>"
			                                                                               + "   <li>This is the difference between 'last_commit_time' from the <i>primary</i> and <i>secondary</i records</li>"
			                                                                               + "   <li>Presented in the form: HH:MM:SS.ms </li>"
			                                                                               + "</ul>"
			                                                                               + "When 'role_desc' is 'PRIMARY':<br>"
			                                                                               + "<ul>"
			                                                                               + "   <li>This is the MAX difference between 'last_commit_time' from the <i>primary</i> and <i>secondary</i records.<br>"
			                                                                               + "       So if you have <b>many</b> secondaries, this is the server with the highest <i>backlog</i>. </li> "
			                                                                               + "   <li>Presented in the form: secondarySrvName=HH:MM:SS.ms </li>"
			                                                                               + "</ul>"
			                                                                               + "</html>");
			mtd.addColumn("CmAlwaysOn", "LogSendQueueSizeKb                   ".trim(),  "<html>Same column as 'log_send_queue_size'.<br>Amount of log records of the primary database that has not been sent to the secondary databases, in kilobytes (KB).</html>");
			mtd.addColumn("CmAlwaysOn", "DataSizeInMb                         ".trim(),  "<html>Database Size in MB.<br><b>Formula</b>: SELECT sum(mf.size / 128.0) FROM sys.master_files mf WHERE mf.type = 0 </html>");
			mtd.addColumn("CmAlwaysOn", "LogSize                              ".trim(),  "<html>Log Size in MB.<br>     <b>Formula</b>: SELECT sum(mf.size / 128.0) FROM sys.master_files mf WHERE mf.type = 1 </html>");

//			mtd.addColumn("CmAlwaysOn", "primaryTransactionsPerSec            ".trim(),  "<html>grabed from CmPerfCounters: ':Databases', 'Transactions/sec'      , ${dbname}</html>"); 
//			mtd.addColumn("CmAlwaysOn", "primaryWriteTransactionsPerSec       ".trim(),  "<html>grabed from CmPerfCounters: ':Databases', 'Write Transactions/sec', ${dbname}</html>"); 
//			mtd.addColumn("CmAlwaysOn", "primaryLogBytesFlushedPerSec         ".trim(),  "<html>grabed from CmPerfCounters: ':Databases', 'Log Bytes Flushed/sec' , ${dbname}</html>"); 
//			mtd.addColumn("CmAlwaysOn", "primaryLogFlushesPerSec              ".trim(),  "<html>grabed from CmPerfCounters: ':Databases', 'Log Flushes/sec'       , ${dbname}</html>"); 
//
//			mtd.addColumn("CmAlwaysOn", "primaryBytesSentToReplicaPerSec      ".trim(),  "<html>grabed from CmPerfCounters: ':Availability Replica', 'Bytes Sent to Replica/sec'       , ${ag_name}:${seondarySrvName}</html>"); 
//			mtd.addColumn("CmAlwaysOn", "primarySendsToReplicaPerSec          ".trim(),  "<html>grabed from CmPerfCounters: ':Availability Replica', 'Sends to Replica/sec'            , ${ag_name}:${seondarySrvName}</html>"); 
//			mtd.addColumn("CmAlwaysOn", "primaryBytesSentToTransportPerSec    ".trim(),  "<html>grabed from CmPerfCounters: ':Availability Replica', 'Bytes Sent to Transport/sec'     , ${ag_name}:${seondarySrvName}</html>"); 
//			mtd.addColumn("CmAlwaysOn", "primarySendsToTransportPerSec        ".trim(),  "<html>grabed from CmPerfCounters: ':Availability Replica', 'Sends to Transport/sec'          , ${ag_name}:${seondarySrvName}</html>"); 
//			mtd.addColumn("CmAlwaysOn", "primaryBytesReceivedFromReplicaPerSec".trim(),  "<html>grabed from CmPerfCounters: ':Availability Replica', 'Bytes Received from Replica/sec' , ${ag_name}:${seondarySrvName}</html>"); 
//			mtd.addColumn("CmAlwaysOn", "primaryReceivesFromReplicaPerSec     ".trim(),  "<html>grabed from CmPerfCounters: ':Availability Replica', 'Receives from Replica/sec'       , ${ag_name}:${seondarySrvName}</html>"); 
//			mtd.addColumn("CmAlwaysOn", "primaryFlowControlTimeMsPerSec       ".trim(),  "<html>grabed from CmPerfCounters: ':Availability Replica', 'Flow Control Time (ms/sec)'      , ${ag_name}:${seondarySrvName}</html>"); 
//			mtd.addColumn("CmAlwaysOn", "primaryFlowControlPerSec             ".trim(),  "<html>grabed from CmPerfCounters: ':Availability Replica', 'Flow Control/sec'                , ${ag_name}:${seondarySrvName}</html>"); 
//			mtd.addColumn("CmAlwaysOn", "primaryResentMessagesPerSec          ".trim(),  "<html>grabed from CmPerfCounters: ':Availability Replica', 'Resent Messages/sec'             , ${ag_name}:${seondarySrvName}</html>"); 
//
//			mtd.addColumn("CmAlwaysOn", "secRecoveryQueue                     ".trim(),  "<html>grabed from CmPerfCounters: ':Database Replica', 'Recovery Queue'                   , ${dbname}</html>"); 
//			mtd.addColumn("CmAlwaysOn", "secRedoneBytesPerSec                 ".trim(),  "<html>grabed from CmPerfCounters: ':Database Replica', 'Redone Bytes/sec'                 , ${dbname}</html>"); 
//			mtd.addColumn("CmAlwaysOn", "secRedonesPerSec                     ".trim(),  "<html>grabed from CmPerfCounters: ':Database Replica', 'Redones/sec'                      , ${dbname}</html>"); 
//			mtd.addColumn("CmAlwaysOn", "secLogSendQueue                      ".trim(),  "<html>grabed from CmPerfCounters: ':Database Replica', 'Log Send Queue'                   , ${dbname}</html>"); 
//			mtd.addColumn("CmAlwaysOn", "secLogApplyReadyQueue                ".trim(),  "<html>grabed from CmPerfCounters: ':Database Replica', 'Log Apply Ready Queue'            , ${dbname}</html>"); 
//			mtd.addColumn("CmAlwaysOn", "secLogApplyPendingQueue              ".trim(),  "<html>grabed from CmPerfCounters: ':Database Replica', 'Log Apply Pending Queue'          , ${dbname}</html>"); 
//			mtd.addColumn("CmAlwaysOn", "secLogBytesReceivedPerSec            ".trim(),  "<html>grabed from CmPerfCounters: ':Database Replica', 'Log Bytes Received/sec'           , ${dbname}</html>"); 
//			mtd.addColumn("CmAlwaysOn", "secFileBytesReceivedPerSec           ".trim(),  "<html>grabed from CmPerfCounters: ':Database Replica', 'File Bytes Received/sec'          , ${dbname}</html>"); 
//			mtd.addColumn("CmAlwaysOn", "secMirroredWriteTransactionsPerSec   ".trim(),  "<html>grabed from CmPerfCounters: ':Database Replica', 'Mirrored Write Transactions/sec'  , ${dbname}</html>"); 
//			mtd.addColumn("CmAlwaysOn", "secTransactionDelay                  ".trim(),  "<html>grabed from CmPerfCounters: ':Database Replica', 'Transaction Delay'                , ${dbname}</html>"); 
//			mtd.addColumn("CmAlwaysOn", "secGroupCommitTime                   ".trim(),  "<html>grabed from CmPerfCounters: ':Database Replica', 'Group Commit Time'                , ${dbname}</html>"); 
//			mtd.addColumn("CmAlwaysOn", "secGroupCommitsPerSec                ".trim(),  "<html>grabed from CmPerfCounters: ':Database Replica', 'Group Commits/Sec'                , ${dbname}</html>"); 
//			mtd.addColumn("CmAlwaysOn", "secLogBytesCompressedPerSec          ".trim(),  "<html>grabed from CmPerfCounters: ':Database Replica', 'Log Bytes Compressed/sec'         , ${dbname}</html>"); 
//			mtd.addColumn("CmAlwaysOn", "secLogCompressionsPerSec             ".trim(),  "<html>grabed from CmPerfCounters: ':Database Replica', 'Log Compressions/sec'             , ${dbname}</html>"); 
//			mtd.addColumn("CmAlwaysOn", "secLogCompressionCachemissesPerSec   ".trim(),  "<html>grabed from CmPerfCounters: ':Database Replica', 'Log Compression Cache misses/sec' , ${dbname}</html>"); 
//			mtd.addColumn("CmAlwaysOn", "secLogCompressionCachehitsPerSec     ".trim(),  "<html>grabed from CmPerfCounters: ':Database Replica', 'Log Compression Cache hits/sec'   , ${dbname}</html>"); 
//			mtd.addColumn("CmAlwaysOn", "secLogBytesDecompressedPerSec        ".trim(),  "<html>grabed from CmPerfCounters: ':Database Replica', 'Log Bytes Decompressed/sec'       , ${dbname}</html>"); 
//			mtd.addColumn("CmAlwaysOn", "secLogDecompressionsPerSec           ".trim(),  "<html>grabed from CmPerfCounters: ':Database Replica', 'Log Decompressions/sec'           , ${dbname}</html>"); 
//			mtd.addColumn("CmAlwaysOn", "secDatabaseFlowControlDelay          ".trim(),  "<html>grabed from CmPerfCounters: ':Database Replica', 'Database Flow Control Delay'      , ${dbname}</html>"); 
//			mtd.addColumn("CmAlwaysOn", "secDatabaseFlowControlsPerSec        ".trim(),  "<html>grabed from CmPerfCounters: ':Database Replica', 'Database Flow Controls/sec'       , ${dbname}</html>"); 
//			mtd.addColumn("CmAlwaysOn", "secTotalLogrequiringundo             ".trim(),  "<html>grabed from CmPerfCounters: ':Database Replica', 'Total Log requiring undo'         , ${dbname}</html>"); 
//			mtd.addColumn("CmAlwaysOn", "secLogRemainingForUndo               ".trim(),  "<html>grabed from CmPerfCounters: ':Database Replica', 'Log remaining for undo'           , ${dbname}</html>"); 
//			mtd.addColumn("CmAlwaysOn", "secRedoBytesRemaining                ".trim(),  "<html>grabed from CmPerfCounters: ':Database Replica', 'Redo Bytes Remaining'             , ${dbname}</html>"); 
//			mtd.addColumn("CmAlwaysOn", "secRedoBlockedPerSec                 ".trim(),  "<html>grabed from CmPerfCounters: ':Database Replica', 'Redo blocked/sec'                 , ${dbname}</html>"); 

			// pcttd = 'Performance Counter ToolTip Description'... could be a extra description to any of the p_, pt2_ and s_ columns...
			String pcttd = "Columns with prefix 'p_', 'p2s_' and 's_' are only visible on specific rows, depending on the 'locality' and 'role_desc'."
					+ "<ul>"
					+ "  <li><code>p_&nbsp;&nbsp;</code> -- locality = 'LOCAL' and role_desc = 'PRIMARY'.                It reflects Performance Counters that are regarding PRIMARY databases. (in transit to the Secondary Server)</li>"
					+ "  <li><code>p2s_</code>           -- locality = 'REMOTE' (only available on any Primary servers). It reflects Performance Counters that are from PRIMARY <b>to</b> SECONDARY transfer, since there can be many recipients for a PRIMARY. and it's also on the whole Availability Group.</li>"
					+ "  <li><code>s_&nbsp;&nbsp;</code> -- locality = 'REMOTE-LIVE-DATA' and role_desc = 'SECONDARY' (so only available on any Secondary SQL-Server), and reflects various <i>recovery</i> </li>"
					+ "</ul>"
					+ "Below description is From Microsofts manual pages: https://docs.microsoft.com/en-us/sql/relational-databases/performance-monitor/sql-server-database-replica ...<br>"
					+ "<hr>"
					+ "<br>";
			
			mtd.addColumn("CmAlwaysOn", "p_TransactionsPerSec                ".trim(),  "<html>grabed from CmPerfCounters: ':Databases',            'Transactions/sec'                 , ${dbname}                     <br>" + pcttd + "Number of transactions started for the database per second.<br>br>Transactions/sec does not count XTP-only transactions (transactions started by a natively compiled stored procedure).</html>"); 
			mtd.addColumn("CmAlwaysOn", "p_WriteTransactionsPerSec           ".trim(),  "<html>grabed from CmPerfCounters: ':Databases',            'Write Transactions/sec'           , ${dbname}                     <br>" + pcttd + "Number of transactions that wrote to the database and committed, in the last second.</html>"); 
			mtd.addColumn("CmAlwaysOn", "p_LogBytesFlushedPerSec             ".trim(),  "<html>grabed from CmPerfCounters: ':Databases',            'Log Bytes Flushed/sec'            , ${dbname}                     <br>" + pcttd + "Total number of log bytes flushed.</html>"); 
			mtd.addColumn("CmAlwaysOn", "p_LogFlushesPerSec                  ".trim(),  "<html>grabed from CmPerfCounters: ':Databases',            'Log Flushes/sec'                  , ${dbname}                     <br>" + pcttd + "Number of log flushes per second.</html>"); 

			mtd.addColumn("CmAlwaysOn", "p_MirroredWriteTransactionsPerSec   ".trim(),  "<html>grabed from CmPerfCounters: ':Database Replica',     'Mirrored Write Transactions/sec'  , ${dbname}                     <br>" + pcttd + "View on 'Primary'.   Number of transactions that were written to the primary database and then waited to commit until the log was sent to the secondary database, in the last second.</html>"); 
			mtd.addColumn("CmAlwaysOn", "p_TransactionDelay                  ".trim(),  "<html>grabed from CmPerfCounters: ':Database Replica',     'Transaction Delay'                , ${dbname}                     <br>" + pcttd + "View on 'Primary'.   Delay in waiting for unterminated commit acknowledgment for all the current transactions, in milliseconds. Divide by Mirrored Write Transaction/sec to get Avg Transaction Delay. For more information, see SQL Server 2012 AlwaysOn – Part 12 – Performance Aspects and Performance Monitoring II</html>"); 
			mtd.addColumn("CmAlwaysOn", "p_GroupCommitTime                   ".trim(),  "<html>grabed from CmPerfCounters: ':Database Replica',     'Group Commit Time'                , ${dbname}                     <br>" + pcttd + "View on 'Primary'.   Number of microseconds all transactions group commit waited.</html>"); // This was found at: https://support.microsoft.com/en-us/help/3173156/update-adds-alwayson-extended-events-and-performance-counters-in-sql-s
			mtd.addColumn("CmAlwaysOn", "p_GroupCommitsPerSec                ".trim(),  "<html>grabed from CmPerfCounters: ':Database Replica',     'Group Commits/Sec'                , ${dbname}                     <br>" + pcttd + "View on 'Primary'.   Number of times transactions waited for group commit.</html>");        // This was found at: https://support.microsoft.com/en-us/help/3173156/update-adds-alwayson-extended-events-and-performance-counters-in-sql-s
			mtd.addColumn("CmAlwaysOn", "p_LogBytesCompressedPerSec          ".trim(),  "<html>grabed from CmPerfCounters: ':Database Replica',     'Log Bytes Compressed/sec'         , ${dbname}                     <br>" + pcttd + "View on 'Primary?'.  The amount of log in bytes compressed per sec.</html>");               // This was found at: https://support.microsoft.com/en-us/help/3173156/update-adds-alwayson-extended-events-and-performance-counters-in-sql-s
			mtd.addColumn("CmAlwaysOn", "p_LogCompressionsPerSec             ".trim(),  "<html>grabed from CmPerfCounters: ':Database Replica',     'Log Compressions/sec'             , ${dbname}                     <br>" + pcttd + "View on 'Primary?'.  The number of log blocks compressed per sec.</html>");                 // This was found at: https://support.microsoft.com/en-us/help/3173156/update-adds-alwayson-extended-events-and-performance-counters-in-sql-s
			mtd.addColumn("CmAlwaysOn", "p_LogCompressionCachemissesPerSec   ".trim(),  "<html>grabed from CmPerfCounters: ':Database Replica',     'Log Compression Cache misses/sec' , ${dbname}                     <br>" + pcttd + "View on 'Primary?'.  The number of log block compression cache misses per sec.</html>");    // This was found at: https://support.microsoft.com/en-us/help/3173156/update-adds-alwayson-extended-events-and-performance-counters-in-sql-s
			mtd.addColumn("CmAlwaysOn", "p_LogCompressionCachehitsPerSec     ".trim(),  "<html>grabed from CmPerfCounters: ':Database Replica',     'Log Compression Cache hits/sec'   , ${dbname}                     <br>" + pcttd + "View on 'Primary?'.  The number of log block compression hits per sec.</html>");            // This was found at: https://support.microsoft.com/en-us/help/3173156/update-adds-alwayson-extended-events-and-performance-counters-in-sql-s
			mtd.addColumn("CmAlwaysOn", "p_LogBytesDecompressedPerSec        ".trim(),  "<html>grabed from CmPerfCounters: ':Database Replica',     'Log Bytes Decompressed/sec'       , ${dbname}                     <br>" + pcttd + "View on 'Primary?'.  The amount of log in bytes decompressed per sec.</html>");             // This was found at: https://support.microsoft.com/en-us/help/3173156/update-adds-alwayson-extended-events-and-performance-counters-in-sql-s
			mtd.addColumn("CmAlwaysOn", "p_LogDecompressionsPerSec           ".trim(),  "<html>grabed from CmPerfCounters: ':Database Replica',     'Log Decompressions/sec'           , ${dbname}                     <br>" + pcttd + "View on 'Primary?'.  The number of log blocks decompressed per sec.</html>");               // This was found at: https://support.microsoft.com/en-us/help/3173156/update-adds-alwayson-extended-events-and-performance-counters-in-sql-s
			mtd.addColumn("CmAlwaysOn", "p_DatabaseFlowControlDelay          ".trim(),  "<html>grabed from CmPerfCounters: ':Database Replica',     'Database Flow Control Delay'      , ${dbname}                     <br>" + pcttd + "View on 'Primary?'.  Duration spent in database flow control wait.</html>");                // This was found at: https://support.microsoft.com/en-us/help/3173156/update-adds-alwayson-extended-events-and-performance-counters-in-sql-s
			mtd.addColumn("CmAlwaysOn", "p_DatabaseFlowControlsPerSec        ".trim(),  "<html>grabed from CmPerfCounters: ':Database Replica',     'Database Flow Controls/sec'       , ${dbname}                     <br>" + pcttd + "View on 'Primary?'.  The number of database flow control per sec.</html>");                 // This was found at: https://support.microsoft.com/en-us/help/3173156/update-adds-alwayson-extended-events-and-performance-counters-in-sql-s

			mtd.addColumn("CmAlwaysOn", "p2s_BytesSentToReplicaPerSec        ".trim(),  "<html>grabed from CmPerfCounters: ':Availability Replica', 'Bytes Sent to Replica/sec'        , ${ag_name}:${seondarySrvName} <br>" + pcttd + "Number of bytes sent to the remote availability replica per second. On the primary replica this is the number of bytes sent to the secondary replica. On the secondary replica this is the number of bytes sent to the primary replica.</html>"); 
			mtd.addColumn("CmAlwaysOn", "p2s_SendsToReplicaPerSec            ".trim(),  "<html>grabed from CmPerfCounters: ':Availability Replica', 'Sends to Replica/sec'             , ${ag_name}:${seondarySrvName} <br>" + pcttd + "Number of Always On messages sent to this availability replica per second.</html>"); 
			mtd.addColumn("CmAlwaysOn", "p2s_BytesSentToTransportPerSec      ".trim(),  "<html>grabed from CmPerfCounters: ':Availability Replica', 'Bytes Sent to Transport/sec'      , ${ag_name}:${seondarySrvName} <br>" + pcttd + "Actual number of bytes sent per second over the network to the remote availability replica. On the primary replica this is the number of bytes sent to the secondary replica. On the secondary replica this is the number of bytes sent to the primary replica.</html>"); 
			mtd.addColumn("CmAlwaysOn", "p2s_SendsToTransportPerSec          ".trim(),  "<html>grabed from CmPerfCounters: ':Availability Replica', 'Sends to Transport/sec'           , ${ag_name}:${seondarySrvName} <br>" + pcttd + "Actual number of Always On messages sent per second over the network to the remote availability replica. On the primary replica this is the number of messages sent to the secondary replica. On the secondary replica this is the number of messages sent to the primary replica.</html>"); 
			mtd.addColumn("CmAlwaysOn", "p2s_BytesReceivedFromReplicaPerSec  ".trim(),  "<html>grabed from CmPerfCounters: ':Availability Replica', 'Bytes Received from Replica/sec'  , ${ag_name}:${seondarySrvName} <br>" + pcttd + "Number of bytes received from the availability replica per second. Pings and status updates will generate network traffic even on databases with no user updates.</html>"); 
			mtd.addColumn("CmAlwaysOn", "p2s_ReceivesFromReplicaPerSec       ".trim(),  "<html>grabed from CmPerfCounters: ':Availability Replica', 'Receives from Replica/sec'        , ${ag_name}:${seondarySrvName} <br>" + pcttd + "Number of Always On messages received from the replica per second.</html>"); 
			mtd.addColumn("CmAlwaysOn", "p2s_FlowControlTimeMsPerSec         ".trim(),  "<html>grabed from CmPerfCounters: ':Availability Replica', 'Flow Control Time (ms/sec)'       , ${ag_name}:${seondarySrvName} <br>" + pcttd + "Time in milliseconds that log stream messages waited for send flow control, in the last second.</html>"); 
			mtd.addColumn("CmAlwaysOn", "p2s_FlowControlPerSec               ".trim(),  "<html>grabed from CmPerfCounters: ':Availability Replica', 'Flow Control/sec'                 , ${ag_name}:${seondarySrvName} <br>" + pcttd + "Number of times flow-control initiated in the last second. Flow Control Time (ms/sec) divided by Flow Control/sec is the average time per wait.</html>"); 
			mtd.addColumn("CmAlwaysOn", "p2s_ResentMessagesPerSec            ".trim(),  "<html>grabed from CmPerfCounters: ':Availability Replica', 'Resent Messages/sec'              , ${ag_name}:${seondarySrvName} <br>" + pcttd + "Number of Always On messages resent in the last second.</html>"); 

			mtd.addColumn("CmAlwaysOn", "s_LogSendQueue                      ".trim(),  "<html>grabed from CmPerfCounters: ':Database Replica',     'Log Send Queue'                   , ${dbname}                     <br>" + pcttd + "View on 'Secondary'. Amount of log records in the log files of the primary database, in kilobytes, that haven't been sent to the secondary replica. This value is sent to the secondary replica from the primary replica. Queue size doesn't include FILESTREAM files that are sent to a secondary.</html>"); 
			mtd.addColumn("CmAlwaysOn", "s_RecoveryQueue                     ".trim(),  "<html>grabed from CmPerfCounters: ':Database Replica',     'Recovery Queue'                   , ${dbname}                     <br>" + pcttd + "View on 'Secondary'. Amount of log records in the log files of the secondary replica that have not been redone.	</html>"); 
			mtd.addColumn("CmAlwaysOn", "s_RedoneBytesPerSec                 ".trim(),  "<html>grabed from CmPerfCounters: ':Database Replica',     'Redone Bytes/sec'                 , ${dbname}                     <br>" + pcttd + "View on 'Secondary'. Amount of log records redone on the secondary database in the last second.</html>"); 
			mtd.addColumn("CmAlwaysOn", "s_RedonesPerSec                     ".trim(),  "<html>grabed from CmPerfCounters: ':Database Replica',     'Redones/sec'                      , ${dbname}                     <br>" + pcttd + "View on 'Secondary'. -not-found-in-manual- </html>"); 
			mtd.addColumn("CmAlwaysOn", "s_LogApplyReadyQueue                ".trim(),  "<html>grabed from CmPerfCounters: ':Database Replica',     'Log Apply Ready Queue'            , ${dbname}                     <br>" + pcttd + "View on 'Secondary'. Number of log blocks that are waiting and ready to be applied to the database replica.</html>"); 
			mtd.addColumn("CmAlwaysOn", "s_LogApplyPendingQueue              ".trim(),  "<html>grabed from CmPerfCounters: ':Database Replica',     'Log Apply Pending Queue'          , ${dbname}                     <br>" + pcttd + "View on 'Secondary'. Number of log blocks that are waiting to be applied to the database replica.</html>"); 
			mtd.addColumn("CmAlwaysOn", "s_LogBytesReceivedPerSec            ".trim(),  "<html>grabed from CmPerfCounters: ':Database Replica',     'Log Bytes Received/sec'           , ${dbname}                     <br>" + pcttd + "View on 'Secondary'. Amount of log records received by the secondary replica for the database in the last second.</html>"); 
			mtd.addColumn("CmAlwaysOn", "s_FileBytesReceivedPerSec           ".trim(),  "<html>grabed from CmPerfCounters: ':Database Replica',     'File Bytes Received/sec'          , ${dbname}                     <br>" + pcttd + "View on 'Secondary'. Amount of FILESTREAM data received by the secondary replica for the secondary database in the last second.</html>"); 
			mtd.addColumn("CmAlwaysOn", "s_TotalLogRequiringUndo             ".trim(),  "<html>grabed from CmPerfCounters: ':Database Replica',     'Total Log requiring undo'         , ${dbname}                     <br>" + pcttd + "View on 'Secondary'. Total kilobytes of log that must be undone.</html>"); 
			mtd.addColumn("CmAlwaysOn", "s_LogRemainingForUndo               ".trim(),  "<html>grabed from CmPerfCounters: ':Database Replica',     'Log remaining for undo'           , ${dbname}                     <br>" + pcttd + "View on 'Secondary'. The amount of log, in kilobytes, remaining to complete the undo phase.</html>"); 
			mtd.addColumn("CmAlwaysOn", "s_RedoBytesRemaining                ".trim(),  "<html>grabed from CmPerfCounters: ':Database Replica',     'Redo Bytes Remaining'             , ${dbname}                     <br>" + pcttd + "View on 'Secondary'. The amount of log, in kilobytes, remaining to be redone to finish the reverting phase.</html>"); 
			mtd.addColumn("CmAlwaysOn", "s_RedoBlockedPerSec                 ".trim(),  "<html>grabed from CmPerfCounters: ':Database Replica',     'Redo blocked/sec'                 , ${dbname}                     <br>" + pcttd + "View on 'Secondary'. Number of times the redo thread was blocked on locks held by readers of the database.</html>"); 

			mtd.addColumn("CmAlwaysOn", "AtServerName                         ".trim(),  "<html>simply value of @@servername</html>"); 
			
		}
		catch (NameNotFoundException e) {/*ignore*/}
	}

	@Override
	public String[] getDependsOnConfigForVersion(Connection conn, long srvVersion, boolean isAzure)
	{
		return NEED_CONFIG;
	}

	@Override
	public List<String> getPkForVersion(Connection conn, long srvVersion, boolean isAzure)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("locality"); // Needed for 'Sample Live Remote Data'
		pkCols.add("server_name");
		pkCols.add("database_name");

		return pkCols;
	}


	/**
	 * Hook into the refresh and BEFORE we refresh data, try to update data...
	 */
	@Override
	protected int refreshGetData(DbxConnection conn) throws Exception
	{
		updatePrimaryDatabases(conn);

		// Now call super that does *all* work of refreshing data
		return super.refreshGetData(conn);
	}

	
	//--------------------------------------------------------------------------------------
	// BEGIN: Special code to get "real-time" data for each SECONDARY (called from: private_localCalculation(newSample);
	//--------------------------------------------------------------------------------------
	/** a "real time" record for each remote server, which can be used to check for "SPLIT BRAIN" or other *bad* situations */
//	private Map<String, ActualRemoteInfo> _secondaryInfo;
//	private Map<String, ResultSetTableModel> _secondaryInfoMap;

//	/** This is a Set of all SECONDARY servers, which is populated by localCalculation(CounterSample newSample), which is within super.refreshGetData(conn) */
//	private Set<String> _secondaryServerSet = new LinkedHashSet<>();

	/** Connection pool (one connection) per SECONDARY server */
	private DbxConnectionPoolMap _cpm;

//	private static class ActualRemoteInfo
//	{
//		String    _srvName;
//		String    _dbname;
//		String    _ag_name;
//		String    _primarySrvName;
//		Timestamp _lastCommitTime;
//
//		String    _role;
//		String    _operationalState;
//		String    _connectedState;
//		String    _recoveryHealth;
//		String    _syncHealth;
//		String    _syncState;
//		String    _suspendedReason;
//		String    _databaseState;
//	}

	public void closeConnPool()
	{
		if (_cpm != null)
			_cpm.close();
		_cpm = null;
	}

	private String getPasswordForRemoteServer(String userName, String srvName)
	{
		try 
		{
//			String aseUser   = storeConfigProps.getProperty("conn.dbmsUsername", "sa");
//			String aseServer = plainAseServerName ? cmd.getOptionValue('S', null) : dbmsCmdLineSwitchHostname;

			// Note: generate a passwd in linux: echo 'thePasswd' | openssl enc -aes-128-cbc -a -salt -pass:mssql
			String passwd = OpenSslAesUtil.readPasswdFromFile(userName, srvName, null, "mssql");
			
			if (passwd != null)
			{
				_logger.info("Grabbed DBMS password '******', for user '"+userName+"', DBMS Server '"+srvName+"' was from the file '"+OpenSslAesUtil.getPasswordFilename()+"'.");

				if (_logger.isDebugEnabled())
					_logger.info("No DBMS password was specified. But the password '"+passwd+"', for user '"+userName+"', DBMS Server '"+srvName+"' was grabbed from the file '"+OpenSslAesUtil.getPasswordFilename()+"'.");

				if (System.getProperty("nogui.password.print", "false").equalsIgnoreCase("true"))
					System.out.println("#### DEBUG ####: No DBMS password was specified. But the password '"+passwd+"', for user '"+userName+"', DBMS Server '"+srvName+"' was grabbed from the file '"+OpenSslAesUtil.getPasswordFilename()+"'.");

				return passwd;
			}
			else
				_logger.info("No DBMS password was specified. and NO entry, for user '"+userName+"', DBMS Server '"+srvName+"' was found in the file '"+OpenSslAesUtil.getPasswordFilename()+"'.");
		}
		catch(DecryptionException ex)
		{
			_logger.info("Problems decrypting the password for user '"+userName+"', DBMS Server '"+srvName+"'. Probably a bad passphrase for the encrypted passwd. Caught: "+ex);
		}
		catch(FileNotFoundException ex)
		{
			_logger.info("The password file '"+OpenSslAesUtil.getPasswordFilename()+"' didn't exists.");
		}
		catch(IOException ex)
		{
			_logger.error("Problems reading the password file "+OpenSslAesUtil.getPasswordFilename()+"'. Caught: "+ex);
		}
		return null;
	}

	private DbxConnection getConnection(CountersModel cm, DbxConnection srvConn, String srvName)
	throws SQLException
	{
		if (srvConn == null)
			throw new RuntimeException("The 'template' Connection can not be null.");
		
		if (_cpm == null)
			throw new RuntimeException("Connection pool Map is not initialized");

		// Are we in GUI mode or not (connections can then use)
		Window guiOwner = null;
//		guiOwner = cm.getGuiController().getGuiHandle();
		if (MainFrame.hasInstance())
			guiOwner = MainFrame.getInstance();

		// reuse a connection if one exists
		if (_cpm.hasMapping(srvName))
		{
			// Set status
			if (cm != null && cm.getGuiController() != null)
				cm.getGuiController().setStatus(MainFrame.ST_STATUS2_FIELD, "get conn to srv '"+srvName+"'");
			
			return _cpm.getPool(srvName).getConnection(guiOwner);
		}

		// Grab the ConnectionProperty from the template Connection
		ConnectionProp connProp = srvConn.getConnProp();
		if (connProp == null)
			throw new SQLException("No ConnectionProperty object could be found at the template connection.");
		
		// Clone the ConnectionProp
		connProp = new ConnectionProp(connProp);

		// Check if the user/server can be found in '~mssql/.passwd.enc' file.
		// If not should we continue... or should we try with the same password?
		String passwd = getPasswordForRemoteServer(connProp.getUsername(), srvName);
		if (passwd != null)
		{
			connProp.setPassword(passwd);
		}
		else
		{
			if (guiOwner != null)
			{
				// Ask for password to the remote server...
				String promptPasswd = PromptForPassword.show(guiOwner, "Please specify Password for User '"+connProp.getUsername()+"' to DBMS '"+srvName+"'.", srvName, connProp.getUsername(), SaveType.TO_HOME_DOT_PASSWD_ENC, "mssql");
				if (promptPasswd != null)
				{
					connProp.setPassword(promptPasswd);
					try
					{
						OpenSslAesUtil.writePasswdToFile(promptPasswd, connProp.getUsername());
					}
					catch(Exception ex)
					{
						_logger.error("Trying to save passwd... Skipping this and Continuing... Caught: " + ex);
					}
				}
				
			}
			_logger.info("Trying to connect (to remote server '"+srvName+"' ) with the same password as we used to server '"+connProp.getUrl()+"', if this FAILS, please add password to the file '"+OpenSslAesUtil.getPasswordFilename()+"'.");
		}
		
		// Set the new server name
		String url = connProp.getUrl();
		JdbcUrlParser p = JdbcUrlParser.parse(url); 
		p.setHost(srvName); // set the new server name

		url = p.toUrl();
		connProp.setUrl(url);
		
		// If the remote server is down, we want a shorter login timeout
		connProp.setUrlOption("loginTimeout", "2"); // 2 seconds login timeout  -- default=15
		
		// if GUI: on error close the dialog after 5 seconds 
		connProp.setOnConnectErrorAutoCloseDialogDelayMs(5000);

		
		// Create a new connection pool for this DB
		DbxConnectionPool cp = new DbxConnectionPool(this.getClass().getSimpleName(), connProp, 5); // Max size = 5

		// Set status in GUI if available
		if (cm != null && cm.getGuiController() != null)
			cm.getGuiController().setStatus(MainFrame.ST_STATUS2_FIELD, "Connecting to srv '"+srvName+"'");

		// grab a new connection.
		DbxConnection dbConn = cp.getConnection(guiOwner);

		_logger.info("Created a new Connection for db '"+srvName+"', which will be cached in a connection pool. with maxSize=5, url='"+url+"', connProp="+connProp);
		
		// when first connection is successfull, add the connection pool to the MAP
		_cpm.setPool(srvName, cp);
		
		return dbConn;
	}

	/**
	 * Release a connection
	 * 
	 * @param cm
	 * @param dbConn
	 * @param dbname
	 */
	private void releaseConnection(CountersModel cm, DbxConnection dbConn, String srvName)
	{
		if (dbConn == null)
			return;

		if (_cpm == null)
			throw new RuntimeException("Connection pool Map is not initialized");
		
		if (_cpm.hasMapping(srvName))
		{
			_cpm.getPool(srvName).releaseConnection(dbConn);
		}
		else
		{
			// The connection pool did not exists, close this connection.
			_logger.info("When trying to 'give back' a connection to the connection pool with key '"+srvName+"'. The key could not be found, so CLOSING the connection instead.");
			
			// Close the connection...
			dbConn.closeNoThrow();
		}
	}

	@Override
	public void close()
	{
		closeConnPool();
		super.close();
	}

	/**
	 * Get "server info" at all the secondary databases/servers<br>
	 * This will be used later on to create alarms if we have several databases/servers in the state PRIMARY, 
	 * which may happen on "unplanned fail-overs" or if someone "fuck-things-up"
	 * @param conn 
	 * 
	 * @return
	 */
	private Map<String, ResultSetTableModel> getSecondaryServerInfo(DbxConnection templateConn, Set<String> secondaryServerSet)
	{
		Map<String, ResultSetTableModel> map = new HashMap<>();
		
		// Procedure:
		//  - foreach servername
		//    - check if we have a password in ~mssql/.passwd.enc (if not we can't continue)
		//    - connect to SECONDARY server (use a connection pool for this, if connections fails we must be able to "re-connect")
		//    - get role/state for ALL databases
		// in method sendAlarmRequest()...
		//    - check current *role* and compare it to the SECONDARY... if both are PRIMARY --->>> ALARM
		//      - Possibly check "other" stuff to...
		//
		int queryTimeout = getQueryTimeout();
		if (_logger.isDebugEnabled())
			_logger.debug(getName()+": queryTimeout="+queryTimeout);
		
		// Should we use the same SQL statement as we executed locally
		String sql = getSql();
		
		for (String srvName : secondaryServerSet)
		{
			DbxConnection dbConn = null;
			try
			{
				// Grab a connection (from the connection pool)
				dbConn = getConnection(this, templateConn, srvName);
				
				// set context to the correct database
				dbConn.setCatalog("master");
				if (_logger.isDebugEnabled())
					_logger.debug("Setting database context to 'master'.");

				if (getGuiController() != null)
						getGuiController().setStatus(MainFrame.ST_STATUS2_FIELD, "for srv '"+srvName+"'");

				Statement stmnt = dbConn.createStatement();

				stmnt.setQueryTimeout(queryTimeout); // XX seconds query timeout
				if (_logger.isDebugEnabled())
					_logger.debug("QUERY_TIMEOUT="+queryTimeout+", for Cm='"+getName()+"' and remoteServerName = '"+srvName+"'.");

				ResultSet rs = stmnt.executeQuery(sql);

				ResultSetTableModel rstm = new ResultSetTableModel(rs, getName()+"-remote-"+srvName);
				map.put(srvName, rstm);

				rs.close();
				stmnt.close();
				
				// get PerfCounter (but only counters that we need)
				boolean sampleLiveRemoteDataPerfCounters = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_sample_liveRemoteDataPerfCnt, DEFAULT_sample_liveRemoteDataPerfCnt);
				if (sampleLiveRemoteDataPerfCounters)
				{
					try
					{
						CmPerfCounters perfCounters = _secondaryServerPerfCounterMap.get(srvName);
						// Create a new if it didn't exist
						if (perfCounters == null)
						{
//							perfCounters = new CmPerfCounters(getCounterController(), null); // we can't use this (getCounterController), since it registers the CM at the controller...
							perfCounters = new CmPerfCounters(null, null);

							perfCounters.setServerVersion(this.getServerVersion());
							perfCounters.setClusterEnabled(this.isClusterEnabled());

							// set the active roles, so it can be used in initSql()
							perfCounters.setActiveServerRolesOrPermissions(this.getActiveServerRolesOrPermissions());

							// Now when we are connected to a server, and properties are set in the CM, 
							// mark it as runtime initialized (or late initialization)
							// do NOT do this at the end of this method, because some of the above stuff
							// will be used by the below method calls.
							perfCounters.setRuntimeInitialized(true);

							// Initializes SQL, use getServerVersion to check what we are connected to.
							perfCounters.initSql(dbConn);

							// Use this method if we need to check anything in the database.
							// for example "deadlock pipe" may not be active...
							// If server version is below 15.0.2 statement cache info should not be VISABLE
							perfCounters.init(dbConn);
							
							// Initialize graphs for the version you just connected to
							// This will simply enable/disable graphs that should not be visible for the ASE version we just connected to
							//perfCounters.initTrendGraphForVersion(monTablesVersion);

							// Remove all Graphs ... we don't need that here...
							// FIXME: Possibly disable all the Trend Graphs instead
							perfCounters.getTrendGraphData().clear();

							// Restrict the select to a SMALLER Range of data.
//							perfCounters.setSqlWhere(" WHERE substring(object_name, charindex(':', object_name),128) IN (':Databases', ':Database Replica', ':Availability Replica') ");
							perfCounters.setSqlWhere(" WHERE ( substring(object_name, charindex(':', object_name),128) = ':Database Replica' ) \n"
							                       + "    OR ( substring(object_name, charindex(':', object_name),128) = ':Availability Replica' ) \n"
							                       + "    OR ( substring(object_name, charindex(':', object_name),128) = ':Databases' AND counter_name IN ('Transactions/sec', 'Write Transactions/sec', 'Log Bytes Flushed/sec', 'Log Flushes/sec') ) \n"
							                       );

							// SET THE NEW OBJECT INTO THE MAP
							_secondaryServerPerfCounterMap.put(srvName, perfCounters);
						}

						// Refresh the REMOTE PerfCounters
						perfCounters.refresh(dbConn);

						// DEBUG: Print the REMOTE PerfCounters as a TABLE 
						if (_logger.isDebugEnabled())
						{
							_logger.debug("");
							_logger.debug("SrvName="+srvName);
							_logger.debug(StringUtil.toTableString(perfCounters.getColNames(CountersModel.DATA_ABS), perfCounters.getDataCollection(CountersModel.DATA_ABS)));
						}
						
						if (_logger.isDebugEnabled())
						{
							_logger.debug("\nSrvName=" + srvName + "\n" + StringUtil.toTableString(perfCounters.getColNames(CountersModel.DATA_ABS), perfCounters.getDataCollection(CountersModel.DATA_ABS)));
						}
					}
					catch (Exception ex)
					{
						_logger.warn("Problems when getting 'CmPerfCounters' for the remote server '" + srvName + "'.", ex);
					}
				}
			}
			catch (SQLException ex)
			{
				_logger.error("When trying to get real-time data from 'REMOTE' server '"+srvName+"', there was a problem. Caught: Error=" + ex.getErrorCode() + ", Message=" + ex);
			}
			finally 
			{
				releaseConnection(this, dbConn, srvName);
			}
		}

		return map;
	}

	//--------------------------------------------------------------------------------------
	// END: Special code to get "real-time" data for each SECONDARY
	//--------------------------------------------------------------------------------------

	private void updatePrimaryDatabases(DbxConnection conn)
	{
		boolean updateActive           = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_update_primary,              DEFAULT_update_primary);
		long updateActiveIntervalInSec = Configuration.getCombinedConfiguration().getLongProperty   (PROPKEY_update_primaryIntervalInSec, DEFAULT_update_primaryIntervalInSec);

		// How many seconds since last update...
		long secondsSinceLastActiveUpdate = TimeUtils.msDiffNow(_lastUpdateOfActive) / 1000;
		
		
		if (updateActive && secondsSinceLastActiveUpdate > updateActiveIntervalInSec)
		{
			_lastUpdateOfActive = System.currentTimeMillis();
			
			// Get a list of databases
			String sql = ""
				    + "select d.name \n"
				    + "from sys.databases d \n"
				    + "join sys.dm_hadr_availability_replica_states ars on d.replica_id = ars.replica_id \n"
				    + "where d.replica_id is not null \n"
				    + "  and ars.is_local = 1 \n"
				    + "  and ars.role_desc = 'PRIMARY' \n"
				    + "";

			List<String> primaryDbs = new ArrayList<>();
			try ( Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql) )
			{
				while(rs.next())
					primaryDbs.add(rs.getString(1));
			}
			catch (SQLException ex)
			{
				_logger.warn("Problems getting PRIMARY databases in Available Groups. Continuing anyway. Caught: Error="+ex.getErrorCode()+", Msg='"+ex.getMessage().trim()+"', SQL="+sql);
			}


			for (String dbname : primaryDbs)
			{
				String updateTable = 
						"-- Create the dummy table if it do not exist \n" +
						"if not exists (select 1 from " + dbname + ".sys.objects where name = 'sqlserverTune_ag_dummy_update' and type = 'U') \n" +
						"begin \n" +
						"   exec('create table " + dbname + ".dbo.sqlserverTune_ag_dummy_update(id varchar(36), primaryServerName varchar(30), ts datetime, primary key(id))') \n" +
						"   exec('grant select,insert,update,delete on " + dbname + ".dbo.sqlserverTune_ag_dummy_update to public') \n" +
						"end \n" +
						"-- remove all old rows \n" +
						"exec('delete from " + dbname + ".dbo.sqlserverTune_ag_dummy_update') \n" +
						"-- insert a new dummy row \n" +
						"exec('insert into " + dbname + ".dbo.sqlserverTune_ag_dummy_update(id, primaryServerName, ts) select newid(), @@servername, getdate()') \n" +
						"";

				try ( Statement stmnt = conn.createStatement() )
				{
					stmnt.executeUpdate(updateTable);
				}
				catch (SQLException ex)
				{
					_logger.warn("Problems updating dummy table 'sqlserverTune_ag_dummy_update' at '"+dbname+"'. But continuing with next step. Caught: Error="+ex.getErrorCode()+", Msg='"+ex.getMessage().trim()+"', SQL="+updateTable);
				}
			}

			// wait "a while" for the records to be replicated...
			long sleepTime = 1000;
			if (sleepTime > 0)
			{
				if (getGuiController() != null)
					getGuiController().setStatus(MainFrame.ST_STATUS2_FIELD, "Wait a short while (" + sleepTime + " ms) for data to be replicated.");

				try { Thread.sleep(sleepTime); }
				catch (InterruptedException ignore) {}
			}
		}
	}
	private long _lastUpdateOfActive = 0;

	@Override
	public String getSqlForVersion(Connection conn, long srvVersion, boolean isAzure)
	{
//System.out.println("CmAlwaysOn: getSqlForVersion(): srvVersion="+srvVersion+", isAzure="+isAzure);
		if (_cpm == null)
			_cpm = new DbxConnectionPoolMap();

		
		String DRS_secondary_lag_seconds = "";
		if (srvVersion >= Ver.ver(2016))
		{
			DRS_secondary_lag_seconds = "    ,drs.secondary_lag_seconds \n";
		}

		String log_truncation_holdup_reason = "";
		String total_log_size_mb            = "";
		String log_recovery_size_mb         = "";
		String active_log_size_mb           = "";
		String log_state                    = "";
		String cross_apply__dm_db_log_stats = "";
		if (srvVersion >= Ver.ver(2016,0,0, 2))
		{
			log_truncation_holdup_reason = "    ,log_truncation_holdup_reason          = CASE WHEN ars.is_local = 1 THEN ls.log_truncation_holdup_reason                 ELSE NULL END \n";
			log_recovery_size_mb         = "    ,log_recovery_size_mb                  = CASE WHEN ars.is_local = 1 THEN convert(decimal(10,1), ls.log_recovery_size_mb) ELSE NULL END \n";
			active_log_size_mb           = "    ,active_log_size_mb                    = CASE WHEN ars.is_local = 1 THEN convert(decimal(10,1), ls.active_log_size_mb  ) ELSE NULL END \n";
			total_log_size_mb            = "    ,total_log_size_mb                     = CASE WHEN ars.is_local = 1 THEN convert(decimal(10,1), ls.total_log_size_mb   ) ELSE NULL END \n";
			cross_apply__dm_db_log_stats = "cross apply sys.dm_db_log_stats(drs.database_id) ls \n";
		}
		if (srvVersion >= Ver.ver(2019))
		{
			log_state                    = "    ,log_state                             = CASE WHEN ars.is_local = 1 THEN ls.log_state                                    ELSE NULL END \n";
		}


		String sql = ""
			+ "select \n"
			+ "     locality                              = CASE WHEN ars.is_local = 1 THEN convert(varchar(20), 'LOCAL') ELSE convert(varchar(20), 'REMOTE') END \n" // Make it 20 so REMOTE-LIVE-DATA also fits
			+ "    ,server_name                           = ar.replica_server_name \n"
			+ "    ,ag_name                               = ag.name \n"
			+ "    ,adc.database_name \n"
			+ "    ,ars.role_desc \n"
			+ "    ,Validated                             = convert(varchar(30), null) \n"
			+ "    ,BagPct                                = convert(int,         -1) \n"
//			+ "    ,DbUseCount                            = CASE WHEN ars.is_local = 1 THEN (select count(*) from sys.sysprocesses p where p.dbid = drs.database_id) ELSE -1 END \n" // or sys.dm_exec_requests
			+ "    ,DbUseCount                            = CASE WHEN ars.is_local = 1 THEN (select count(*) from sys.dm_exec_requests p where p.database_id = drs.database_id and connection_id is not null) ELSE -1 END \n" // or sys.dm_exec_requests
			+ "    ,PRIMARY_server_name                   = primary_replica \n"
			+ "\n"
			+ "    ,ars.operational_state_desc \n"
			+ "    ,ars.connected_state_desc \n"
			+ "    ,ars.recovery_health_desc \n"
			+ "    ,ars.synchronization_health_desc \n"
			+ "    ,drs.synchronization_state_desc \n"
			+ "    ,drs.suspend_reason_desc \n"
			+ "    ,drs.database_state_desc \n"
			+ "\n"
			+ "    ,AvailabilityModeConfig                = ar.availability_mode_desc \n"
			+ "    ,ar.create_date \n"
			+ "    ,drs.last_commit_time \n"
			+ "    ,SecondaryCommitTimeLag                = convert(varchar(45), NULL) \n"
			+ DRS_secondary_lag_seconds
			+ "    ,LogSendQueueSizeKb                    = drs.log_send_queue_size\n"
			+ log_truncation_holdup_reason
			+ log_recovery_size_mb
			+ active_log_size_mb
			+ total_log_size_mb
			+ log_state
			+ "\n"
			+ "    ,DataSizeInMb = (SELECT convert(decimal(10,1), sum(mf.size / 128.0)) FROM sys.master_files mf WHERE mf.type = 0 AND mf.database_id = drs.database_id) \n"
			+ "    ,LogSize      = (SELECT convert(decimal(10,1), sum(mf.size / 128.0)) FROM sys.master_files mf WHERE mf.type = 1 AND mf.database_id = drs.database_id) \n"
			+ "\n"
			+ "    ,p_TransactionsPerSec               = convert(decimal(17,2), NULL) \n"  // grab from CmPerfCounters: String pk = createPkStr(":Databases", "Transactions/sec"                           , dbname); 
			+ "    ,p_WriteTransactionsPerSec          = convert(decimal(17,2), NULL) \n"  // grab from CmPerfCounters: String pk = createPkStr(":Databases", "Write Transactions/sec"                     , dbname); 
			+ "    ,p_LogBytesFlushedPerSec            = convert(decimal(17,2), NULL) \n"  // grab from CmPerfCounters: String pk = createPkStr(":Databases", "Log Bytes Flushed/sec"                      , dbname); 
			+ "    ,p_LogFlushesPerSec                 = convert(decimal(17,2), NULL) \n"  // grab from CmPerfCounters: String pk = createPkStr(":Databases", "Log Flushes/sec"                            , dbname); 
			+ "    \n"
			+ "    ,p_MirroredWriteTransactionsPerSec  = convert(decimal(17,2), NULL) \n"  // grab from CmPerfCounters: String pk = createPkStr(":Database Replica", "Mirrored Write Transactions/sec"     , dbname);
			+ "    ,p_TransactionDelay                 = convert(decimal(17,2), NULL) \n"  // grab from CmPerfCounters: String pk = createPkStr(":Database Replica", "Transaction Delay"                   , dbname);
			+ "    ,p_GroupCommitTime                  = convert(decimal(17,2), NULL) \n"  // grab from CmPerfCounters: String pk = createPkStr(":Database Replica", "Group Commit Time"                   , dbname);
			+ "    ,p_GroupCommitsPerSec               = convert(decimal(17,2), NULL) \n"  // grab from CmPerfCounters: String pk = createPkStr(":Database Replica", "Group Commits/Sec"                   , dbname);
			+ "    ,p_LogBytesCompressedPerSec         = convert(decimal(17,2), NULL) \n"  // grab from CmPerfCounters: String pk = createPkStr(":Database Replica", "Log Bytes Compressed/sec"            , dbname);
			+ "    ,p_LogCompressionsPerSec            = convert(decimal(17,2), NULL) \n"  // grab from CmPerfCounters: String pk = createPkStr(":Database Replica", "Log Compressions/sec"                , dbname);
			+ "    ,p_LogCompressionCachemissesPerSec  = convert(decimal(17,2), NULL) \n"  // grab from CmPerfCounters: String pk = createPkStr(":Database Replica", "Log Compression Cache misses/sec"    , dbname);
			+ "    ,p_LogCompressionCachehitsPerSec    = convert(decimal(17,2), NULL) \n"  // grab from CmPerfCounters: String pk = createPkStr(":Database Replica", "Log Compression Cache hits/sec"      , dbname);
			+ "    ,p_LogBytesDecompressedPerSec       = convert(decimal(17,2), NULL) \n"  // grab from CmPerfCounters: String pk = createPkStr(":Database Replica", "Log Bytes Decompressed/sec"          , dbname);
			+ "    ,p_LogDecompressionsPerSec          = convert(decimal(17,2), NULL) \n"  // grab from CmPerfCounters: String pk = createPkStr(":Database Replica", "Log Decompressions/sec"              , dbname);
			+ "    ,p_DatabaseFlowControlDelay         = convert(decimal(17,2), NULL) \n"  // grab from CmPerfCounters: String pk = createPkStr(":Database Replica", "Database Flow Control Delay"         , dbname);
			+ "    ,p_DatabaseFlowControlsPerSec       = convert(decimal(17,2), NULL) \n"  // grab from CmPerfCounters: String pk = createPkStr(":Database Replica", "Database Flow Controls/sec"          , dbname);
			+ "    \n"
			+ "    ,p2s_BytesSentToReplicaPerSec       = convert(decimal(17,2), NULL) \n"  // grab from CmPerfCounters: String pk = createPkStr(":Availability Replica", "Bytes Sent to Replica/sec"       , AG_gs1:prod-2b-mssql);
			+ "    ,p2s_SendsToReplicaPerSec           = convert(decimal(17,2), NULL) \n"  // grab from CmPerfCounters: String pk = createPkStr(":Availability Replica", "Sends to Replica/sec"            , AG_gs1:prod-2b-mssql);
			+ "    ,p2s_BytesSentToTransportPerSec     = convert(decimal(17,2), NULL) \n"  // grab from CmPerfCounters: String pk = createPkStr(":Availability Replica", "Bytes Sent to Transport/sec"     , AG_gs1:prod-2b-mssql);
			+ "    ,p2s_SendsToTransportPerSec         = convert(decimal(17,2), NULL) \n"  // grab from CmPerfCounters: String pk = createPkStr(":Availability Replica", "Sends to Transport/sec"          , AG_gs1:prod-2b-mssql);
			+ "    ,p2s_BytesReceivedFromReplicaPerSec = convert(decimal(17,2), NULL) \n"  // grab from CmPerfCounters: String pk = createPkStr(":Availability Replica", "Bytes Received from Replica/sec" , AG_gs1:prod-2b-mssql);
			+ "    ,p2s_ReceivesFromReplicaPerSec      = convert(decimal(17,2), NULL) \n"  // grab from CmPerfCounters: String pk = createPkStr(":Availability Replica", "Receives from Replica/sec"       , AG_gs1:prod-2b-mssql);
			+ "    ,p2s_FlowControlTimeMsPerSec        = convert(decimal(17,2), NULL) \n"  // grab from CmPerfCounters: String pk = createPkStr(":Availability Replica", "Flow Control Time (ms/sec)"      , AG_gs1:prod-2b-mssql);
			+ "    ,p2s_FlowControlPerSec              = convert(decimal(17,2), NULL) \n"  // grab from CmPerfCounters: String pk = createPkStr(":Availability Replica", "Flow Control/sec"                , AG_gs1:prod-2b-mssql);
			+ "    ,p2s_ResentMessagesPerSec           = convert(decimal(17,2), NULL) \n"  // grab from CmPerfCounters: String pk = createPkStr(":Availability Replica", "Resent Messages/sec"             , AG_gs1:prod-2b-mssql);
			+ "\n"
			+ "    ,s_LogSendQueue                     = convert(decimal(17,2), NULL) \n"  // grab from CmPerfCounters: String pk = createPkStr(":Database Replica", "Log Send Queue"                      , dbname);
			+ "    ,s_RecoveryQueue                    = convert(decimal(17,2), NULL) \n"  // grab from CmPerfCounters: String pk = createPkStr(":Database Replica", "Recovery Queue"                      , dbname);
			+ "    ,s_RedoneBytesPerSec                = convert(decimal(17,2), NULL) \n"  // grab from CmPerfCounters: String pk = createPkStr(":Database Replica", "Redone Bytes/sec"                    , dbname);
			+ "    ,s_RedonesPerSec                    = convert(decimal(17,2), NULL) \n"  // grab from CmPerfCounters: String pk = createPkStr(":Database Replica", "Redones/sec"                         , dbname);
			+ "    ,s_LogApplyReadyQueue               = convert(decimal(17,2), NULL) \n"  // grab from CmPerfCounters: String pk = createPkStr(":Database Replica", "Log Apply Ready Queue"               , dbname);
			+ "    ,s_LogApplyPendingQueue             = convert(decimal(17,2), NULL) \n"  // grab from CmPerfCounters: String pk = createPkStr(":Database Replica", "Log Apply Pending Queue"             , dbname);
			+ "    ,s_LogBytesReceivedPerSec           = convert(decimal(17,2), NULL) \n"  // grab from CmPerfCounters: String pk = createPkStr(":Database Replica", "Log Bytes Received/sec"              , dbname);
			+ "    ,s_FileBytesReceivedPerSec          = convert(decimal(17,2), NULL) \n"  // grab from CmPerfCounters: String pk = createPkStr(":Database Replica", "File Bytes Received/sec"             , dbname);
			+ "    ,s_TotalLogRequiringUndo            = convert(decimal(17,2), NULL) \n"  // grab from CmPerfCounters: String pk = createPkStr(":Database Replica", "Total Log requiring undo"            , dbname);
			+ "    ,s_LogRemainingForUndo              = convert(decimal(17,2), NULL) \n"  // grab from CmPerfCounters: String pk = createPkStr(":Database Replica", "Log remaining for undo"              , dbname);
			+ "    ,s_RedoBytesRemaining               = convert(decimal(17,2), NULL) \n"  // grab from CmPerfCounters: String pk = createPkStr(":Database Replica", "Redo Bytes Remaining"                , dbname);
			+ "    ,s_RedoBlockedPerSec                = convert(decimal(17,2), NULL) \n"  // grab from CmPerfCounters: String pk = createPkStr(":Database Replica", "Redo blocked/sec"                    , dbname);
			+ "\n"
			+ "    ,ag.basic_features \n"
			+ "    ,ag.dtc_support \n"
			+ "    ,ag.db_failover \n"
			+ "    ,ag.is_distributed \n"
			+ "    ,ag.version \n"
			+ "    ,ag.failure_condition_level \n"
			+ "    ,ag.health_check_timeout \n"
			+ "\n"
			+ "    ,ar.endpoint_url \n"
			+ "    ,ar.availability_mode_desc \n"
			+ "    ,ar.failover_mode_desc \n"
			+ "    ,ar.session_timeout \n"
			+ "    ,ar.primary_role_allow_connections_desc \n"
			+ "    ,ar.secondary_role_allow_connections_desc \n"
//			+ "    ,ar.create_date \n"
			+ "    ,ar.modify_date \n"
			+ "    ,ar.read_only_routing_url \n"
			+ "\n"
			+ "    ,ars.last_connect_error_number \n"
			+ "    ,ars.last_connect_error_description \n"
			+ "    ,ars.last_connect_error_timestamp \n"
			+ "\n"
			+ "    ,drs.is_suspended \n"
			+ "    ,drs.is_commit_participant \n"
			+ "    ,drs.is_primary_replica                -- 2014 \n"
			+ "    ,drs.recovery_lsn \n"
			+ "    ,drs.truncation_lsn \n"
			+ "    ,drs.last_sent_lsn \n"
			+ "    ,drs.last_sent_time \n"
			+ "    ,drs.last_received_lsn \n"
			+ "    ,drs.last_received_time \n"
			+ "    ,drs.last_hardened_lsn \n"
			+ "    ,drs.last_hardened_time \n"
			+ "    ,drs.last_redone_lsn \n"
			+ "    ,drs.last_redone_time \n"
			+ "    ,drs.log_send_queue_size \n"
			+ "    ,drs.log_send_rate \n"
			+ "    ,drs.redo_queue_size \n"
			+ "    ,drs.redo_rate \n"
			+ "    ,drs.filestream_send_rate \n"
			+ "    ,drs.end_of_log_lsn \n"
			+ "    ,drs.last_commit_lsn \n"
//			+ "    ,drs.last_commit_time \n"
			+ "    ,drs.low_water_mark_for_ghosts \n"
//			+ "    ,drs.secondary_lag_seconds            --- 2016 \n"

			+ "    ,drs_synchronization_state             = drs.synchronization_state  \n"
			+ "    ,drs_synchronization_health            = drs.synchronization_health \n"
			+ "    ,drs_database_state                    = drs.database_state         \n"
			+ "    ,drs_suspend_reason                    = drs.suspend_reason         \n"

			+ "    ,ars_role                              = ars.role                   \n"
			+ "    ,ars_operational_state                 = ars.operational_state      \n"
			+ "    ,ars_recovery_health                   = ars.recovery_health        \n"
			+ "    ,ars_synchronization_health            = ars.synchronization_health \n"
			+ "    ,ars_connected_state                   = ars.connected_state        \n"

			+ "    ,ags_primary_recovery_health           = ags.primary_recovery_health   \n"
			+ "    ,ags_secondary_recovery_health         = ags.secondary_recovery_health \n"
			+ "    ,ags_synchronization_health            = ags.synchronization_health    \n"

			+ "    ,ar_availability_mode                  = ar.availability_mode                \n"
			+ "    ,ar_failover_mode                      = ar.failover_mode                    \n"
			+ "    ,ar_primary_role_allow_connections     = ar.primary_role_allow_connections   \n"
			+ "    ,ar_secondary_role_allow_connections   = ar.secondary_role_allow_connections \n"

			+ "    ,AtServerName                          = @@servername\n"
			+ "    ,BagPercentDetails                     = convert(varchar(512), null) \n"
			
			+ " \n"
			+ "from sys.availability_replicas ar \n"
			+ "inner join sys.availability_groups                       ag on ar.group_id   = ag.group_id \n"
			+ "inner join sys.availability_databases_cluster           adc on ar.group_id   = adc.group_id \n"
			+ "inner join sys.dm_hadr_availability_group_states        ags on ar.group_id   = ags.group_id \n"
			+ "left outer join sys.dm_hadr_availability_replica_states ars on ar.replica_id = ars.replica_id \n"
			+ "left outer join sys.dm_hadr_database_replica_states     drs on ar.replica_id = drs.replica_id \n"
			+ cross_apply__dm_db_log_stats
			+ "where 1 = 1 \n"
//			+ "order by ar.replica_server_name, adc.database_name \n"
			+ "order by 1, 4, 2  -- locality, database_name, server_name \n"
			+ "";
		
		return sql;
	}
	
	@Override
	public String getToolTipTextOnTableCell(MouseEvent e, String colName, Object cellValue, int modelRow, int modelCol) 
	{
		// SQL TEXT
		if ("BagPct".equals(colName))
		{
			// Find 'OldestTranSqlText' column, is so get it and set it as the tool tip
			int pos = findColumn("BagPercentDetails");
			if (pos > 0)
			{
				Object cellVal = getValueAt(modelRow, pos);
				if (cellVal instanceof String)
				{
					String str = (String) cellVal;
					if (JsonUtils.isPossibleJson(str))
						return JsonUtils.format(str, true);
					else
						return str;
				}
			}
		}
		
		return super.getToolTipTextOnTableCell(e, colName, cellValue, modelRow, modelCol);
	}

	@Override
	public void localCalculation(CounterSample newSample)
	{
		// Enrich some columns with data from: CmPerfCounters
		private_localCalculation(newSample);

		// if possible, get "roles" from the SECONDARY database(es)
		boolean sampleLiveRemoteData = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_sample_liveRemoteData, DEFAULT_sample_liveRemoteData);
		if (sampleLiveRemoteData)
		{
			sampleLiveRemoteData(newSample);
			validateLocalAndLiveRemoteData(newSample);
		}

		// If there are BAG (Basic Availability Groups), then check if all servers has same role[PRIMARY/SECONDARY] within a group are located on the same server
		// This is represented as a Percentage Number on each "logical group" (a "logical group" is the name/chars before first '_' or '-'  
		setBagRolePercent(newSample, COLVAL_LOCALITY_LOCAL);
		setBagRolePercent(newSample, COLVAL_LOCALITY_REMOTE_LIVE_DATA);
	}

	private void setBagRolePercent(CounterSample newSample, String locality)
	{
		Map<String, BagInfo> bagMap = new LinkedHashMap<>(); 

		HashMap<String, Object> whereClause = new HashMap<>(2);
		whereClause.put("locality",       locality);
		whereClause.put("basic_features", true);
		
		List<Integer> rowsLocal = newSample.getRowIdsWhere(whereClause);
//System.out.println("");
//System.out.println("rowsLocal.size()="+rowsLocal.size()+": "+rowsLocal);
		
		for (Integer rowId : rowsLocal)
		{
			String agName = newSample.getValueAsString(rowId, "ag_name");
			String dbname = newSample.getValueAsString(rowId, "database_name");
			String role   = newSample.getValueAsString(rowId, "role_desc");
//System.out.println("agName='"+agName+"', dbname='"+dbname+"', role='"+role+"'.");

			String agNamePrefix = getAgNamePrefix(agName);
			if ( ! agName.equals(agNamePrefix) )
			{
				BagInfo bagInfo = bagMap.get(agNamePrefix);
				if (bagInfo == null)
				{
					bagInfo = new BagInfo(newSample, agNamePrefix, rowsLocal);
					bagMap.put(agNamePrefix, bagInfo);
				}

				bagInfo.addRowForDbname(role, dbname, rowId);
			}
		}
		
		for (BagInfo entry : bagMap.values())
		{
			entry.calculateAndSet();
		}
	}
	private String getAgNamePrefix(String agName)
	{
		int pos = agName.indexOf('_');
		if (pos == -1)
			pos = agName.indexOf('-');
		if (pos > 0)
		{
			String prefix = agName.substring(0, pos+1);
			return prefix;
		}
		return agName;
	}
	private static class BagInfo
	{
		CounterSample _newSample;
		String _bagPrefixName;
		List<Integer> _rowIds = new ArrayList<>();
		Map<String, List<Integer>> _primaryDbs   = new LinkedHashMap<>();
		Map<String, List<Integer>> _secondaryDbs = new LinkedHashMap<>();
		Map<String, List<Integer>> _resolvingDbs = new LinkedHashMap<>();

		int _primaryPct   = 0;
		int _secondaryPct = 0;
		int _resolvingPct = 0;

		public BagInfo(CounterSample newSample, String bagPrefixName, List<Integer> rowIds)
		{
			_newSample     = newSample;
			_bagPrefixName = bagPrefixName;
			_rowIds        = rowIds;
		}
		
		public void addRowForDbname(String role, String dbname, int rowId)
		{
			Map<String, List<Integer>> map = null;;
			
			if      (PRIMARY_ROLE  .equals(role)) map = _primaryDbs;
			else if (SECONDARY_ROLE.equals(role)) map = _secondaryDbs;
			else if (RESOLVING_ROLE.equals(role)) map = _resolvingDbs;
			else
			{
				_logger.error("BagInfo.addRowForDbname(dbname='" + dbname + "', role='" + role + "'. Skipping this role, only 'PRIMARY', 'SECONDARY', 'RESOLVING' are recognized.");
				return;
			}

			List<Integer> rids = map.get(dbname);
			if (rids == null)
			{
				rids = new ArrayList<>();
				map.put(dbname, rids);
			}

			rids.add(rowId);
		}

		public void calculateAndSet()
		{
			int rowCount = _rowIds.size();

			_primaryPct   = (sum(_primaryDbs  ) / rowCount) * 100;
			_secondaryPct = (sum(_secondaryDbs) / rowCount) * 100;
			_resolvingPct = (sum(_resolvingDbs) / rowCount) * 100;
			
//System.out.println("--- bagPrefixName='" + _bagPrefixName + "', _primaryPct=" + _primaryPct + ", _secondaryPct=" + _secondaryPct + ", _resolvingPct=" + _resolvingPct + "");
//System.out.println("    bagPrefixName='" + _bagPrefixName + "', _primaryDbs=" + _primaryDbs + ", _secondaryDbs=" + _secondaryDbs + ", _resolvingDbs=" + _resolvingDbs + "");

			setBagPercentValues(PRIMARY_ROLE  , _primaryDbs  , _primaryPct);
			setBagPercentValues(SECONDARY_ROLE, _secondaryDbs, _secondaryPct);
			setBagPercentValues(RESOLVING_ROLE, _resolvingDbs, _resolvingPct);
		}
		
		private int sum(Map<String, List<Integer>> map)
		{
			int sum = 0;
			for (List<Integer> list : map.values())
				sum += list.size();
			return sum;
		}
		
		private void setBagPercentValues(String role, Map<String, List<Integer>> map, int pct)
		{
			int pos_BagPct            = _newSample.findColumn("BagPct");
			int pos_BagPercentDetails = _newSample.findColumn("BagPercentDetails");
			
//			String details =  pct + "% " + role + ", LogicalGroupName='" + _bagPrefixName + "', dbnames=" + StringUtil.toCommaStrQuoted("'", "'", map.keySet());
			String jsonDet =  "{#percent#: " + pct + ", #role#: #" + role + "#, #logicalGroupName#: #" + _bagPrefixName + "#, #dbnames#: [" + StringUtil.toCommaStrQuoted(map.keySet()) + "]}";
			jsonDet = jsonDet.replace('#', '"');

			for (Entry<String, List<Integer>> entry : map.entrySet())
			{
				for (Integer rowId : entry.getValue())
				{
					_newSample.setValueAt(pct,     rowId, pos_BagPct);
					_newSample.setValueAt(jsonDet, rowId, pos_BagPercentDetails);
				}
			}
		}
	}

	private void sampleLiveRemoteData(CounterSample newSample)
	{
		DbxConnection conn = getCounterController().getMonConnection();

		int pos_locality      = newSample.findColumn("locality");
		int pos_server_name   = newSample.findColumn("server_name");

		// A SET of unique SECONDARY server names
		Set<String> secondaryServerSet = new LinkedHashSet<>();		
		
		// Loop on all rows
		for (int rowId = 0; rowId < newSample.getRowCount(); rowId++)
		{
			String locality      = "" + newSample.getValueAt(rowId, pos_locality);
			String server_name   = "" + newSample.getValueAt(rowId, pos_server_name);

			// Add "remote" servers to a Set so we can get/extract ACTUAL or "real time" role/status information from each remote.
			// This can be used to check for multiple PRIMARY entries (or SPLIT BRAN) which can happen on a "unplanned failover"
			if (COLVAL_LOCALITY_REMOTE.equals(locality))
				secondaryServerSet.add(server_name);
		}

		// For each unique SECONDARY server, go and execute SQL
		Map<String, ResultSetTableModel> secondaryInfoMap = getSecondaryServerInfo(conn, secondaryServerSet);
		//System.out.println("_secondaryInfoMap.size()="+_secondaryInfoMap.size());
		
		for (Entry<String, ResultSetTableModel> e : secondaryInfoMap.entrySet())
		{
			String              srvName = e.getKey();     
			ResultSetTableModel rstm    = e.getValue();
			
			// Compose a WHERE Clause
			HashMap<String, Object> whereClause = new HashMap<>(1);
			whereClause.put("locality", COLVAL_LOCALITY_LOCAL);

			// Get what records that cotains above search argument
			List<Integer> rows = rstm.getRowIdsWhere(whereClause);

			// Loop the rows we found in above search
			for (Integer rowId : rows)
			{
				// Get the ROW from the ResultSetTableModel
				List<Object> row = rstm.getRowList(rows.get(rowId));

				// If we have grabbed Performance Counters from the SECONDARY Server...
				// Set the column values
				CmPerfCounters perfCounters = _secondaryServerPerfCounterMap.get(srvName);
				if (perfCounters != null)
				{
					String dbname    = rstm.getValueAsString(rowId, "database_name");
					String role_desc = rstm.getValueAsString(rowId, "role_desc");
					
					if (SECONDARY_ROLE.equals(role_desc))
					{
    					setValueSecondarySrvRow(row, rstm, "s_LogSendQueue"                    , perfCounters, createPkStr(":Database Replica", "Log Send Queue"                   , dbname));
    					setValueSecondarySrvRow(row, rstm, "s_RecoveryQueue"                   , perfCounters, createPkStr(":Database Replica", "Recovery Queue"                   , dbname));
    					setValueSecondarySrvRow(row, rstm, "s_RedoneBytesPerSec"               , perfCounters, createPkStr(":Database Replica", "Redone Bytes/sec"                 , dbname));
    					setValueSecondarySrvRow(row, rstm, "s_RedonesPerSec"                   , perfCounters, createPkStr(":Database Replica", "Redones/sec"                      , dbname));
    					setValueSecondarySrvRow(row, rstm, "s_LogApplyReadyQueue"              , perfCounters, createPkStr(":Database Replica", "Log Apply Ready Queue"            , dbname));
    					setValueSecondarySrvRow(row, rstm, "s_LogApplyPendingQueue"            , perfCounters, createPkStr(":Database Replica", "Log Apply Pending Queue"          , dbname));
    					setValueSecondarySrvRow(row, rstm, "s_LogBytesReceivedPerSec"          , perfCounters, createPkStr(":Database Replica", "Log Bytes Received/sec"           , dbname));
    					setValueSecondarySrvRow(row, rstm, "s_FileBytesReceivedPerSec"         , perfCounters, createPkStr(":Database Replica", "File Bytes Received/sec"          , dbname));
    					setValueSecondarySrvRow(row, rstm, "s_TotalLogRequiringUndo"           , perfCounters, createPkStr(":Database Replica", "Total Log requiring undo"         , dbname));
    					setValueSecondarySrvRow(row, rstm, "s_LogRemainingForUndo"             , perfCounters, createPkStr(":Database Replica", "Log remaining for undo"           , dbname));
    					setValueSecondarySrvRow(row, rstm, "s_RedoBytesRemaining"              , perfCounters, createPkStr(":Database Replica", "Redo Bytes Remaining"             , dbname));
    					setValueSecondarySrvRow(row, rstm, "s_RedoBlockedPerSec"               , perfCounters, createPkStr(":Database Replica", "Redo blocked/sec"                 , dbname));
					}
					else if (PRIMARY_ROLE.equals(role_desc))
					{
						setValueSecondarySrvRow(row, rstm, "p_TransactionsPerSec"              , perfCounters, createPkStr(":Databases"       , "Transactions/sec"                 , dbname));
						setValueSecondarySrvRow(row, rstm, "p_WriteTransactionsPerSec"         , perfCounters, createPkStr(":Databases"       , "Write Transactions/sec"           , dbname));
						setValueSecondarySrvRow(row, rstm, "p_LogBytesFlushedPerSec"           , perfCounters, createPkStr(":Databases"       , "Log Bytes Flushed/sec"            , dbname));
						setValueSecondarySrvRow(row, rstm, "p_LogFlushesPerSec"                , perfCounters, createPkStr(":Databases"       , "Log Flushes/sec"                  , dbname));

    					setValueSecondarySrvRow(row, rstm, "p_MirroredWriteTransactionsPerSec" , perfCounters, createPkStr(":Database Replica", "Mirrored Write Transactions/sec"  , dbname));
    					setValueSecondarySrvRow(row, rstm, "p_TransactionDelay"                , perfCounters, createPkStr(":Database Replica", "Transaction Delay"                , dbname));
    					setValueSecondarySrvRow(row, rstm, "p_GroupCommitTime"                 , perfCounters, createPkStr(":Database Replica", "Group Commit Time"                , dbname));
    					setValueSecondarySrvRow(row, rstm, "p_GroupCommitsPerSec"              , perfCounters, createPkStr(":Database Replica", "Group Commits/Sec"                , dbname));
    					setValueSecondarySrvRow(row, rstm, "p_LogBytesCompressedPerSec"        , perfCounters, createPkStr(":Database Replica", "Log Bytes Compressed/sec"         , dbname));
    					setValueSecondarySrvRow(row, rstm, "p_LogCompressionsPerSec"           , perfCounters, createPkStr(":Database Replica", "Log Compressions/sec"             , dbname));
    					setValueSecondarySrvRow(row, rstm, "p_LogCompressionCachemissesPerSec" , perfCounters, createPkStr(":Database Replica", "Log Compression Cache misses/sec" , dbname));
    					setValueSecondarySrvRow(row, rstm, "p_LogCompressionCachehitsPerSec"   , perfCounters, createPkStr(":Database Replica", "Log Compression Cache hits/sec"   , dbname));
    					setValueSecondarySrvRow(row, rstm, "p_LogBytesDecompressedPerSec"      , perfCounters, createPkStr(":Database Replica", "Log Bytes Decompressed/sec"       , dbname));
    					setValueSecondarySrvRow(row, rstm, "p_LogDecompressionsPerSec"         , perfCounters, createPkStr(":Database Replica", "Log Decompressions/sec"           , dbname));
    					setValueSecondarySrvRow(row, rstm, "p_DatabaseFlowControlDelay"        , perfCounters, createPkStr(":Database Replica", "Database Flow Control Delay"      , dbname));
    					setValueSecondarySrvRow(row, rstm, "p_DatabaseFlowControlsPerSec"      , perfCounters, createPkStr(":Database Replica", "Database Flow Controls/sec"       , dbname));

    					String ag_name      = rstm.getValueAsString(rowId, "ag_name");
						String LocalSrvName = _localSrvName; //rstm.getValueAsString(rowId, "AtServerName");
						
						String agNameAndReplicaSrvName = ag_name + ":" + LocalSrvName; // AG_gs1:prod-2b-mssql   ( PRIMARY-->>SECONDARY)

						setValueSecondarySrvRow(row, rstm, "p2s_BytesSentToReplicaPerSec"      , perfCounters, createPkStr(":Availability Replica", "Bytes Sent to Replica/sec"       , agNameAndReplicaSrvName));
						setValueSecondarySrvRow(row, rstm, "p2s_SendsToReplicaPerSec"          , perfCounters, createPkStr(":Availability Replica", "Sends to Replica/sec"            , agNameAndReplicaSrvName));
						setValueSecondarySrvRow(row, rstm, "p2s_BytesSentToTransportPerSec"    , perfCounters, createPkStr(":Availability Replica", "Bytes Sent to Transport/sec"     , agNameAndReplicaSrvName));
						setValueSecondarySrvRow(row, rstm, "p2s_SendsToTransportPerSec"        , perfCounters, createPkStr(":Availability Replica", "Sends to Transport/sec"          , agNameAndReplicaSrvName));
						setValueSecondarySrvRow(row, rstm, "p2s_BytesReceivedFromReplicaPerSec", perfCounters, createPkStr(":Availability Replica", "Bytes Received from Replica/sec" , agNameAndReplicaSrvName));
						setValueSecondarySrvRow(row, rstm, "p2s_ReceivesFromReplicaPerSec"     , perfCounters, createPkStr(":Availability Replica", "Receives from Replica/sec"       , agNameAndReplicaSrvName));
						setValueSecondarySrvRow(row, rstm, "p2s_FlowControlTimeMsPerSec"       , perfCounters, createPkStr(":Availability Replica", "Flow Control Time (ms/sec)"      , agNameAndReplicaSrvName));
						setValueSecondarySrvRow(row, rstm, "p2s_FlowControlPerSec"             , perfCounters, createPkStr(":Availability Replica", "Flow Control/sec"                , agNameAndReplicaSrvName));
						setValueSecondarySrvRow(row, rstm, "p2s_ResentMessagesPerSec"          , perfCounters, createPkStr(":Availability Replica", "Resent Messages/sec"             , agNameAndReplicaSrvName));
					}
				}

				// COPY the row from the RSTM
				List<Object> copyRow = new ArrayList<>( row );

				// Set column "locality" to be "REMOTE-LIVE-DATA"
				copyRow.set(COLPOS_LOCALITY, COLVAL_LOCALITY_REMOTE_LIVE_DATA);

				// Add the record to the "new sample"
				newSample.addRow(this, copyRow);
			}
		}
	}
	private void setValueSecondarySrvRow(List<Object> row, ResultSetTableModel rstm, String colName, CmPerfCounters perfCounters, String pk)
	{
		int colPos = rstm.findColumn(colName);
		if (colPos == -1)
		{
			System.out.println("colPos: was -1 for colName='" + colName + "'.");
			return;
		}
		
		int pkRowId = perfCounters.getAbsRowIdForPkValue(pk);
		if (pkRowId == -1)
		{
			_logger.info("setValueSecondarySrv(): NO VALUE FOR: pk=" + pk );
		}
		else
		{
			Double val = perfCounters.getAbsValueAsDouble(pkRowId, "calculated_value");
			row.set(colPos, val);
		}
	}

	private void validateLocalAndLiveRemoteData(CounterSample newSample)
	{
		int pos_Validated = newSample.findColumn("Validated");
		
		if (pos_Validated == -1)
			return;
		
//System.out.println( StringUtil.toTableString(newSample.getColNames(), newSample.getDataCollection()) );
		
		for (int r=0; r<newSample.getRowCount(); r++)
		{
			String locality            = newSample.getValueAsString(r, "locality");
//			String server_name         = newSample.getValueAsString(r, "server_name");
			String dbname              = newSample.getValueAsString(r, "database_name");
			String ag_name             = newSample.getValueAsString(r, "ag_name");
//			String role_desc           = newSample.getValueAsString(r, "role_desc");
			
//			String PRIMARY_server_name = newSample.getValueAsString(r, "PRIMARY_server_name");
//			String AtServerName        = newSample.getValueAsString(r, "AtServerName");

//			boolean isLocal            = COLVAL_LOCALITY_LOCAL .equals(locality);
			boolean isRemote           = COLVAL_LOCALITY_REMOTE.equals(locality);
//			boolean isRemoteLiveData   = COLVAL_LOCALITY_REMOTE_LIVE_DATA.equals(locality);
//			boolean isPrimary          = PRIMARY_ROLE  .equals(role_desc);
//			boolean isSecondary        = SECONDARY_ROLE.equals(role_desc);

			//-------------------------------------------------------
			// SplitBrain
			//-------------------------------------------------------
			// 
			// ### on prod-2a-prod IN a Normal situation 2a is PRIMARY
			// +----------------+-------------+-------+-------------+---------+----------+-------------------+----------------------+...+----------------------+--------------------+--------------------+---------------------------+--------------------------+-------------------+-------------------+------------+-------+...+-------------+
			// |locality        |server_name  |ag_name|database_name|role_desc|DbUseCount|PRIMARY_server_name|AvailabilityModeConfig|...|operational_state_desc|connected_state_desc|recovery_health_desc|synchronization_health_desc|synchronization_state_desc|suspend_reason_desc|database_state_desc|DataSizeInMb|LogSize|...|AtServerName |
			// +----------------+-------------+-------+-------------+---------+----------+-------------------+----------------------+...+----------------------+--------------------+--------------------+---------------------------+--------------------------+-------------------+-------------------+------------+-------+...+-------------+
			// |LOCAL           |prod-2a-mssql|AG_gs1 |gs1          |PRIMARY  |0         |prod-2a-mssql      |SYNCHRONOUS_COMMIT    |...|ONLINE                |CONNECTED           |ONLINE              |HEALTHY                    |SYNCHRONIZED              |                   |ONLINE             |512.0       |1024.0 |...|prod-2a-mssql|
			// |LOCAL           |prod-2a-mssql|AG_gs2 |gs2          |PRIMARY  |0         |prod-2a-mssql      |SYNCHRONOUS_COMMIT    |...|ONLINE                |CONNECTED           |ONLINE              |HEALTHY                    |SYNCHRONIZED              |                   |ONLINE             |8820.4      |1023.9 |...|prod-2a-mssql|
			// |LOCAL           |prod-2a-mssql|AG_gs3 |gs3          |PRIMARY  |0         |prod-2a-mssql      |SYNCHRONOUS_COMMIT    |...|ONLINE                |CONNECTED           |ONLINE              |HEALTHY                    |SYNCHRONIZED              |                   |ONLINE             |512.0       |1024.0 |...|prod-2a-mssql|
			// |REMOTE          |prod-2b-mssql|AG_gs1 |gs1          |SECONDARY|-1        |prod-2a-mssql      |SYNCHRONOUS_COMMIT    |...|                      |CONNECTED           |                    |HEALTHY                    |SYNCHRONIZED              |                   |                   |512.0       |1024.0 |...|prod-2a-mssql|
			// |REMOTE          |prod-2b-mssql|AG_gs2 |gs2          |SECONDARY|-1        |prod-2a-mssql      |SYNCHRONOUS_COMMIT    |...|                      |CONNECTED           |                    |HEALTHY                    |SYNCHRONIZED              |                   |                   |8820.4      |1023.9 |...|prod-2a-mssql|
			// |REMOTE          |prod-2b-mssql|AG_gs3 |gs3          |SECONDARY|-1        |prod-2a-mssql      |SYNCHRONOUS_COMMIT    |...|                      |CONNECTED           |                    |HEALTHY                    |SYNCHRONIZED              |                   |                   |512.0       |1024.0 |...|prod-2a-mssql|
			// |REMOTE-LIVE-DATA|prod-2b-mssql|AG_gs1 |gs1          |SECONDARY|0         |prod-2a-mssql      |SYNCHRONOUS_COMMIT    |...|ONLINE                |CONNECTED           |ONLINE              |HEALTHY                    |SYNCHRONIZED              |                   |ONLINE             |512.0       |1024.0 |...|prod-2b-mssql|
			// |REMOTE-LIVE-DATA|prod-2b-mssql|AG_gs2 |gs2          |SECONDARY|0         |prod-2a-mssql      |SYNCHRONOUS_COMMIT    |...|ONLINE                |CONNECTED           |ONLINE              |HEALTHY                    |SYNCHRONIZED              |                   |ONLINE             |8820.4      |1023.9 |...|prod-2b-mssql|
			// |REMOTE-LIVE-DATA|prod-2b-mssql|AG_gs3 |gs3          |SECONDARY|0         |prod-2a-mssql      |SYNCHRONOUS_COMMIT    |...|ONLINE                |CONNECTED           |ONLINE              |HEALTHY                    |SYNCHRONIZED              |                   |ONLINE             |512.0       |1024.0 |...|prod-2b-mssql|
			// +----------------+-------------+-------+-------------+---------+----------+-------------------+----------------------+...+----------------------+--------------------+--------------------+---------------------------+--------------------------+-------------------+-------------------+------------+-------+...+-------------+
            // 
			// ### on prod-2b-prod IN a Normal situation 2b is SECONDARY
			// +----------------+-------------+-------+-------------+---------+----------+-------------------+----------------------+...+----------------------+--------------------+--------------------+---------------------------+--------------------------+-------------------+-------------------+------------+-------+...+-------------+
			// |locality        |server_name  |ag_name|database_name|role_desc|DbUseCount|PRIMARY_server_name|AvailabilityModeConfig|...|operational_state_desc|connected_state_desc|recovery_health_desc|synchronization_health_desc|synchronization_state_desc|suspend_reason_desc|database_state_desc|DataSizeInMb|LogSize|...|AtServerName |
			// +----------------+-------------+-------+-------------+---------+----------+-------------------+----------------------+...+----------------------+--------------------+--------------------+---------------------------+--------------------------+-------------------+-------------------+------------+-------+...+-------------+
			// |LOCAL           |prod-2b-mssql|AG_gs1 |gs1          |SECONDARY|0         |prod-2a-mssql      |SYNCHRONOUS_COMMIT    |...|ONLINE                |CONNECTED           |ONLINE              |HEALTHY                    |SYNCHRONIZED              |                   |ONLINE             |512.0       |1024.0 |...|prod-2b-mssql|
			// |LOCAL           |prod-2b-mssql|AG_gs2 |gs2          |SECONDARY|0         |prod-2a-mssql      |SYNCHRONOUS_COMMIT    |...|ONLINE                |CONNECTED           |ONLINE              |HEALTHY                    |SYNCHRONIZED              |                   |ONLINE             |8820.4      |1023.9 |...|prod-2b-mssql|
			// |LOCAL           |prod-2b-mssql|AG_gs3 |gs3          |SECONDARY|0         |prod-2a-mssql      |SYNCHRONOUS_COMMIT    |...|ONLINE                |CONNECTED           |ONLINE              |HEALTHY                    |SYNCHRONIZED              |                   |ONLINE             |512.0       |1024.0 |...|prod-2b-mssql|
			// |REMOTE          |prod-2a-mssql|AG_gs1 |gs1          |         |-1        |prod-2a-mssql      |SYNCHRONOUS_COMMIT    |...|                      |                    |                    |                           |                          |                   |                   |            |       |...|prod-2b-mssql|
			// |REMOTE          |prod-2a-mssql|AG_gs2 |gs2          |         |-1        |prod-2a-mssql      |SYNCHRONOUS_COMMIT    |...|                      |                    |                    |                           |                          |                   |                   |            |       |...|prod-2b-mssql|
			// |REMOTE          |prod-2a-mssql|AG_gs3 |gs3          |         |-1        |prod-2a-mssql      |SYNCHRONOUS_COMMIT    |...|                      |                    |                    |                           |                          |                   |                   |            |       |...|prod-2b-mssql|
			// |REMOTE-LIVE-DATA|prod-2a-mssql|AG_gs1 |gs1          |PRIMARY  |0         |prod-2a-mssql      |SYNCHRONOUS_COMMIT    |...|ONLINE                |CONNECTED           |ONLINE              |HEALTHY                    |SYNCHRONIZED              |                   |ONLINE             |512.0       |1024.0 |...|prod-2a-mssql|
			// |REMOTE-LIVE-DATA|prod-2a-mssql|AG_gs2 |gs2          |PRIMARY  |0         |prod-2a-mssql      |SYNCHRONOUS_COMMIT    |...|ONLINE                |CONNECTED           |ONLINE              |HEALTHY                    |SYNCHRONIZED              |                   |ONLINE             |8820.4      |1023.9 |...|prod-2a-mssql|
			// |REMOTE-LIVE-DATA|prod-2a-mssql|AG_gs3 |gs3          |PRIMARY  |0         |prod-2a-mssql      |SYNCHRONOUS_COMMIT    |...|ONLINE                |CONNECTED           |ONLINE              |HEALTHY                    |SYNCHRONIZED              |                   |ONLINE             |512.0       |1024.0 |...|prod-2a-mssql|
			// +----------------+-------------+-------+-------------+---------+----------+-------------------+----------------------+...+----------------------+--------------------+--------------------+---------------------------+--------------------------+-------------------+-------------------+------------+-------+...+-------------+
            // 
            // 
			// ### [mssql@prod-2b-mssql ~]$ ./bag_admin.sh --primary prod-2a-mssql --secondary prod-2b-mssql failover
			// ### AFTER FAILOVER ############################################################################################################################################################
            // 
            // 
			// ### on prod-2a-prod IN a SPLIT-BRAIN scenario (failover from 2a -> 2b, then start 2a, before setting 2a to SECONDARY)
			// +----------------+-------------+-------+-------------+---------+----------+-------------------+----------------------+...+----------------------+--------------------+--------------------+---------------------------+--------------------------+-------------------+-------------------+------------+-------+...+-------------+
			// |locality        |server_name  |ag_name|database_name|role_desc|DbUseCount|PRIMARY_server_name|AvailabilityModeConfig|...|operational_state_desc|connected_state_desc|recovery_health_desc|synchronization_health_desc|synchronization_state_desc|suspend_reason_desc|database_state_desc|DataSizeInMb|LogSize|...|AtServerName |
			// +----------------+-------------+-------+-------------+---------+----------+-------------------+----------------------+...+----------------------+--------------------+--------------------+---------------------------+--------------------------+-------------------+-------------------+------------+-------+...+-------------+
			// |LOCAL           |prod-2a-mssql|AG_gs1 |gs1          |PRIMARY  |0         |prod-2a-mssql      |SYNCHRONOUS_COMMIT    |...|ONLINE                |CONNECTED           |ONLINE              |HEALTHY                    |SYNCHRONIZED              |                   |ONLINE             |512.0       |1024.0 |...|prod-2a-mssql|
			// |LOCAL           |prod-2a-mssql|AG_gs2 |gs2          |PRIMARY  |0         |prod-2a-mssql      |SYNCHRONOUS_COMMIT    |...|ONLINE                |CONNECTED           |ONLINE              |HEALTHY                    |SYNCHRONIZED              |                   |ONLINE             |8820.4      |1023.9 |...|prod-2a-mssql|
			// |LOCAL           |prod-2a-mssql|AG_gs3 |gs3          |PRIMARY  |0         |prod-2a-mssql      |SYNCHRONOUS_COMMIT    |...|ONLINE                |CONNECTED           |ONLINE              |HEALTHY                    |SYNCHRONIZED              |                   |ONLINE             |512.0       |1024.0 |...|prod-2a-mssql|
			// |REMOTE          |prod-2b-mssql|AG_gs1 |gs1          |SECONDARY|-1        |prod-2a-mssql      |SYNCHRONOUS_COMMIT    |...|                      |DISCONNECTED        |                    |NOT_HEALTHY                |NOT SYNCHRONIZING         |                   |                   |512.0       |1024.0 |...|prod-2a-mssql|
			// |REMOTE          |prod-2b-mssql|AG_gs2 |gs2          |SECONDARY|-1        |prod-2a-mssql      |SYNCHRONOUS_COMMIT    |...|                      |DISCONNECTED        |                    |NOT_HEALTHY                |NOT SYNCHRONIZING         |                   |                   |8820.4      |1023.9 |...|prod-2a-mssql|
			// |REMOTE          |prod-2b-mssql|AG_gs3 |gs3          |SECONDARY|-1        |prod-2a-mssql      |SYNCHRONOUS_COMMIT    |...|                      |DISCONNECTED        |                    |NOT_HEALTHY                |NOT SYNCHRONIZING         |                   |                   |512.0       |1024.0 |...|prod-2a-mssql|
			// |REMOTE-LIVE-DATA|prod-2b-mssql|AG_gs1 |gs1          |PRIMARY  |0         |prod-2b-mssql      |SYNCHRONOUS_COMMIT    |...|ONLINE                |CONNECTED           |ONLINE              |HEALTHY                    |SYNCHRONIZED              |                   |ONLINE             |512.0       |1024.0 |...|prod-2b-mssql|
			// |REMOTE-LIVE-DATA|prod-2b-mssql|AG_gs2 |gs2          |PRIMARY  |0         |prod-2b-mssql      |SYNCHRONOUS_COMMIT    |...|ONLINE                |CONNECTED           |ONLINE              |HEALTHY                    |SYNCHRONIZED              |                   |ONLINE             |8820.4      |1023.9 |...|prod-2b-mssql|
			// |REMOTE-LIVE-DATA|prod-2b-mssql|AG_gs3 |gs3          |PRIMARY  |0         |prod-2b-mssql      |SYNCHRONOUS_COMMIT    |...|ONLINE                |CONNECTED           |ONLINE              |HEALTHY                    |SYNCHRONIZED              |                   |ONLINE             |512.0       |1024.0 |...|prod-2b-mssql|
			// +----------------+-------------+-------+-------------+---------+----------+-------------------+----------------------+...+----------------------+--------------------+--------------------+---------------------------+--------------------------+-------------------+-------------------+------------+-------+...+-------------+
            // 
			// ### on prod-2b-prod IN a SPLIT-BRAIN scenario (failover from 2a -> 2b, then start 2a, before setting 2a to SECONDARY)
			// +----------------+-------------+-------+-------------+---------+----------+-------------------+----------------------+...+----------------------+--------------------+--------------------+---------------------------+--------------------------+--------------------+-------------------+------------+-------+...+-------------+
			// |locality        |server_name  |ag_name|database_name|role_desc|DbUseCount|PRIMARY_server_name|AvailabilityModeConfig|...|operational_state_desc|connected_state_desc|recovery_health_desc|synchronization_health_desc|synchronization_state_desc|suspend_reason_desc |database_state_desc|DataSizeInMb|LogSize|...|AtServerName |
			// +----------------+-------------+-------+-------------+---------+----------+-------------------+----------------------+...+----------------------+--------------------+--------------------+---------------------------+--------------------------+--------------------+-------------------+------------+-------+...+-------------+
			// |LOCAL           |prod-2b-mssql|AG_gs1 |gs1          |PRIMARY  |0         |prod-2b-mssql      |SYNCHRONOUS_COMMIT    |...|ONLINE                |CONNECTED           |ONLINE              |HEALTHY                    |SYNCHRONIZED              |                    |ONLINE             |512.0       |1024.0 |...|prod-2b-mssql|
			// |LOCAL           |prod-2b-mssql|AG_gs2 |gs2          |PRIMARY  |0         |prod-2b-mssql      |SYNCHRONOUS_COMMIT    |...|ONLINE                |CONNECTED           |ONLINE              |HEALTHY                    |SYNCHRONIZED              |                    |ONLINE             |8820.4      |1023.9 |...|prod-2b-mssql|
			// |LOCAL           |prod-2b-mssql|AG_gs3 |gs3          |PRIMARY  |0         |prod-2b-mssql      |SYNCHRONOUS_COMMIT    |...|ONLINE                |CONNECTED           |ONLINE              |HEALTHY                    |SYNCHRONIZED              |                    |ONLINE             |512.0       |1024.0 |...|prod-2b-mssql|
			// |REMOTE          |prod-2a-mssql|AG_gs1 |gs1          |SECONDARY|-1        |prod-2b-mssql      |SYNCHRONOUS_COMMIT    |...|                      |DISCONNECTED        |                    |NOT_HEALTHY                |NOT SYNCHRONIZING         |SUSPEND_FROM_PARTNER|                   |512.0       |1024.0 |...|prod-2b-mssql|
			// |REMOTE          |prod-2a-mssql|AG_gs2 |gs2          |SECONDARY|-1        |prod-2b-mssql      |SYNCHRONOUS_COMMIT    |...|                      |DISCONNECTED        |                    |NOT_HEALTHY                |NOT SYNCHRONIZING         |SUSPEND_FROM_PARTNER|                   |8820.4      |1023.9 |...|prod-2b-mssql|
			// |REMOTE          |prod-2a-mssql|AG_gs3 |gs3          |SECONDARY|-1        |prod-2b-mssql      |SYNCHRONOUS_COMMIT    |...|                      |DISCONNECTED        |                    |NOT_HEALTHY                |NOT SYNCHRONIZING         |SUSPEND_FROM_PARTNER|                   |512.0       |1024.0 |...|prod-2b-mssql|
			// |REMOTE-LIVE-DATA|prod-2a-mssql|AG_gs1 |gs1          |PRIMARY  |0         |prod-2a-mssql      |SYNCHRONOUS_COMMIT    |...|ONLINE                |CONNECTED           |ONLINE              |HEALTHY                    |SYNCHRONIZED              |                    |ONLINE             |512.0       |1024.0 |...|prod-2a-mssql|
			// |REMOTE-LIVE-DATA|prod-2a-mssql|AG_gs2 |gs2          |PRIMARY  |0         |prod-2a-mssql      |SYNCHRONOUS_COMMIT    |...|ONLINE                |CONNECTED           |ONLINE              |HEALTHY                    |SYNCHRONIZED              |                    |ONLINE             |8820.4      |1023.9 |...|prod-2a-mssql|
			// |REMOTE-LIVE-DATA|prod-2a-mssql|AG_gs3 |gs3          |PRIMARY  |0         |prod-2a-mssql      |SYNCHRONOUS_COMMIT    |...|ONLINE                |CONNECTED           |ONLINE              |HEALTHY                    |SYNCHRONIZED              |                    |ONLINE             |512.0       |1024.0 |...|prod-2a-mssql|
			// +----------------+-------------+-------+-------------+---------+----------+-------------------+----------------------+...+----------------------+--------------------+--------------------+---------------------------+--------------------------+--------------------+-------------------+------------+-------+...+-------------+

//			if (isRemote && isSecondary)
			if (isRemote)
			{
//System.out.println();
//System.out.println("----- locality='" + locality + "', server_name='" + server_name + "', ag_name='" + ag_name + "', database_name='" + dbname + "', role_desc='" + role_desc + "', AtServerName='" + AtServerName + "'");
				// locality==REMOTE: get row for locality==REMOTE-LIVE-DATA
				HashMap<String, Object> whereClauseRemoteLiveData = new HashMap<>(4);
				whereClauseRemoteLiveData.put("locality",       COLVAL_LOCALITY_REMOTE_LIVE_DATA);
//				whereClauseRemoteLiveData.put("server_name",    server_name);
				whereClauseRemoteLiveData.put("ag_name",        ag_name);
				whereClauseRemoteLiveData.put("database_name",  dbname);
				
				List<Integer> rowsRemoteLiveData = newSample.getRowIdsWhere(whereClauseRemoteLiveData);
//System.out.println("getAbsRowIdsWhere(whereClauseRemoteLiveData):: returns="+rowsRemoteLiveData+", where="+whereClauseRemoteLiveData);
					
					
				// locality==REMOTE: get row for locality==REMOTE-LOCAL
				HashMap<String, Object> whereClauseLocal = new HashMap<>(4);
				whereClauseLocal.put("locality",       COLVAL_LOCALITY_LOCAL);
//				whereClauseLocal.put("server_name",    AtServerName);
				whereClauseLocal.put("ag_name",        ag_name);
				whereClauseLocal.put("database_name",  dbname);
				
				List<Integer> rowsLocal = newSample.getRowIdsWhere(whereClauseLocal);
//System.out.println("getAbsRowIdsWhere(whereClauseLocal)::          returns="+rowsLocal+", where="+whereClauseLocal);

				// for REMOTE records: Set 'Validated' to "-"
				newSample.setValueAt("-", r, pos_Validated);
				
				if (rowsRemoteLiveData.size() == 1 && rowsLocal.size() == 1)
				{
					int rowIdRemoteLiveData = rowsRemoteLiveData.get(0);
					int rowIdLocal          = rowsLocal         .get(0);
					
					String remoteLiveDataRoleDesc = newSample.getValueAsString(rowIdRemoteLiveData, "role_desc"); // if null it returns ""
					String localRoleDesc          = newSample.getValueAsString(rowIdLocal         , "role_desc"); // if null it returns ""

					
//System.out.println("--->>>>>>>>>>> localRoleDesc='" + localRoleDesc + "', remoteLiveDataRoleDesc='" + remoteLiveDataRoleDesc + "'");
					
					if ( ! localRoleDesc.equals(remoteLiveDataRoleDesc) )
					{
						newSample.setValueAt("YES", rowIdLocal         , pos_Validated);
						newSample.setValueAt("YES", rowIdRemoteLiveData, pos_Validated);
					}
					else
					{
//System.out.println("--->>>>>>>>>>> -ALARM-ALARM-ALARM- ***DIFF*** localRoleDesc='" + localRoleDesc + "', remoteLiveDataRoleDesc='" + remoteLiveDataRoleDesc + "'");
						
						newSample.setValueAt("INVALID", rowIdLocal         , pos_Validated);
						newSample.setValueAt("INVALID", rowIdRemoteLiveData, pos_Validated);

						if (PRIMARY_ROLE.equals(localRoleDesc) && PRIMARY_ROLE.equals(remoteLiveDataRoleDesc))
						{
//System.out.println("--->>>>>>>>>>> -ALARM-ALARM-ALARM- ***BOTH-IS-PRIMARY*** localRoleDesc='" + localRoleDesc + "', remoteLiveDataRoleDesc='" + remoteLiveDataRoleDesc + "'");
							newSample.setValueAt("SPLIT-BRAIN", rowIdLocal         , pos_Validated);
							newSample.setValueAt("SPLIT-BRAIN", rowIdRemoteLiveData, pos_Validated);
						}
					}
				}
			} // end: isRemote
		} // end: loop rows
	}

	public void private_localCalculation(CounterSample newSample)
	{
		int pos_locality                              = -1;
		int pos_server_name                           = -1;
		int pos_ag_name                               = -1;
		int pos_database_name                         = -1;
		int pos_role_desc                             = -1;

		int pos_PRIMARY_server_name                   = -1;
		int pos_last_commit_time                      = -1;
		int pos_SecondaryCommitTimeLag                = -1;

		int pos_AtServerName                          = -1;

		// dm_os_performance_counters: Only available on PRIMARY
		int pos_p_TransactionsPerSec               = -1; // grab from CmPerfCounters: String pk = createPkStr(":Databases"       , "Transactions/sec"                 , dbname);
		int pos_p_WriteTransactionsPerSec          = -1; // grab from CmPerfCounters: String pk = createPkStr(":Databases"       , "Write Transactions/sec"           , dbname);
		int pos_p_LogBytesFlushedPerSec            = -1; // grab from CmPerfCounters: String pk = createPkStr(":Databases"       , "Log Bytes Flushed/sec"            , dbname);
		int pos_p_LogFlushesPerSec                 = -1; // grab from CmPerfCounters: String pk = createPkStr(":Databases"       , "Log Flushes/sec"                  , dbname);

		int pos_p_MirroredWriteTransactionsPerSec  = -1;  // grab from CmPerfCounters: String pk = createPkStr(":Database Replica", "Mirrored Write Transactions/sec"  , dbname);
		int pos_p_TransactionDelay                 = -1;  // grab from CmPerfCounters: String pk = createPkStr(":Database Replica", "Transaction Delay"                , dbname);
		int pos_p_GroupCommitTime                  = -1;  // grab from CmPerfCounters: String pk = createPkStr(":Database Replica", "Group Commit Time"                , dbname);
		int pos_p_GroupCommitsPerSec               = -1;  // grab from CmPerfCounters: String pk = createPkStr(":Database Replica", "Group Commits/Sec"                , dbname);
		int pos_p_LogBytesCompressedPerSec         = -1;  // grab from CmPerfCounters: String pk = createPkStr(":Database Replica", "Log Bytes Compressed/sec"         , dbname);
		int pos_p_LogCompressionsPerSec            = -1;  // grab from CmPerfCounters: String pk = createPkStr(":Database Replica", "Log Compressions/sec"             , dbname);
		int pos_p_LogCompressionCachemissesPerSec  = -1;  // grab from CmPerfCounters: String pk = createPkStr(":Database Replica", "Log Compression Cache misses/sec" , dbname);
		int pos_p_LogCompressionCachehitsPerSec    = -1;  // grab from CmPerfCounters: String pk = createPkStr(":Database Replica", "Log Compression Cache hits/sec"   , dbname);
		int pos_p_LogBytesDecompressedPerSec       = -1;  // grab from CmPerfCounters: String pk = createPkStr(":Database Replica", "Log Bytes Decompressed/sec"       , dbname);
		int pos_p_LogDecompressionsPerSec          = -1;  // grab from CmPerfCounters: String pk = createPkStr(":Database Replica", "Log Decompressions/sec"           , dbname);
		int pos_p_DatabaseFlowControlDelay         = -1;  // grab from CmPerfCounters: String pk = createPkStr(":Database Replica", "Database Flow Control Delay"      , dbname);
		int pos_p_DatabaseFlowControlsPerSec       = -1;  // grab from CmPerfCounters: String pk = createPkStr(":Database Replica", "Database Flow Controls/sec"       , dbname);

		// dm_os_performance_counters: Only available on PRIMARY but for every Replica/SECONDARY we send information to
		int pos_p2s_BytesSentToReplicaPerSec       = -1; // grab from CmPerfCounters: String pk = createPkStr(":Availability Replica", "Bytes Sent to Replica/sec"       , AG_gs1:prod-2b-mssql);
		int pos_p2s_SendsToReplicaPerSec           = -1; // grab from CmPerfCounters: String pk = createPkStr(":Availability Replica", "Sends to Replica/sec"            , AG_gs1:prod-2b-mssql);
		int pos_p2s_BytesSentToTransportPerSec     = -1; // grab from CmPerfCounters: String pk = createPkStr(":Availability Replica", "Bytes Sent to Transport/sec"     , AG_gs1:prod-2b-mssql);
		int pos_p2s_SendsToTransportPerSec         = -1; // grab from CmPerfCounters: String pk = createPkStr(":Availability Replica", "Sends to Transport/sec"          , AG_gs1:prod-2b-mssql);
		int pos_p2s_BytesReceivedFromReplicaPerSec = -1; // grab from CmPerfCounters: String pk = createPkStr(":Availability Replica", "Bytes Received from Replica/sec" , AG_gs1:prod-2b-mssql);
		int pos_p2s_ReceivesFromReplicaPerSec      = -1; // grab from CmPerfCounters: String pk = createPkStr(":Availability Replica", "Receives from Replica/sec"       , AG_gs1:prod-2b-mssql);
		int pos_p2s_FlowControlTimeMsPerSec        = -1; // grab from CmPerfCounters: String pk = createPkStr(":Availability Replica", "Flow Control Time (ms/sec)"      , AG_gs1:prod-2b-mssql);
		int pos_p2s_FlowControlPerSec              = -1; // grab from CmPerfCounters: String pk = createPkStr(":Availability Replica", "Flow Control/sec"                , AG_gs1:prod-2b-mssql);
		int pos_p2s_ResentMessagesPerSec           = -1; // grab from CmPerfCounters: String pk = createPkStr(":Availability Replica", "Resent Messages/sec"             , AG_gs1:prod-2b-mssql);
		
		// dm_os_performance_counters: Only available on SECONDARY
		int pos_s_LogSendQueue                     = -1;  // grab from CmPerfCounters: String pk = createPkStr(":Database Replica", "Log Send Queue"                   , dbname);
		int pos_s_RecoveryQueue                    = -1;  // grab from CmPerfCounters: String pk = createPkStr(":Database Replica", "Recovery Queue"                   , dbname);
		int pos_s_RedoneBytesPerSec                = -1;  // grab from CmPerfCounters: String pk = createPkStr(":Database Replica", "Redone Bytes/sec"                 , dbname);
		int pos_s_RedonesPerSec                    = -1;  // grab from CmPerfCounters: String pk = createPkStr(":Database Replica", "Redones/sec"                      , dbname);
		int pos_s_LogApplyReadyQueue               = -1;  // grab from CmPerfCounters: String pk = createPkStr(":Database Replica", "Log Apply Ready Queue"            , dbname);
		int pos_s_LogApplyPendingQueue             = -1;  // grab from CmPerfCounters: String pk = createPkStr(":Database Replica", "Log Apply Pending Queue"          , dbname);
		int pos_s_LogBytesReceivedPerSec           = -1;  // grab from CmPerfCounters: String pk = createPkStr(":Database Replica", "Log Bytes Received/sec"           , dbname);
		int pos_s_FileBytesReceivedPerSec          = -1;  // grab from CmPerfCounters: String pk = createPkStr(":Database Replica", "File Bytes Received/sec"          , dbname);
		int pos_s_TotalLogRequiringUndo            = -1;  // grab from CmPerfCounters: String pk = createPkStr(":Database Replica", "Total Log requiring undo"         , dbname);
		int pos_s_LogRemainingForUndo              = -1;  // grab from CmPerfCounters: String pk = createPkStr(":Database Replica", "Log remaining for undo"           , dbname);
		int pos_s_RedoBytesRemaining               = -1;  // grab from CmPerfCounters: String pk = createPkStr(":Database Replica", "Redo Bytes Remaining"             , dbname);
		int pos_s_RedoBlockedPerSec                = -1;  // grab from CmPerfCounters: String pk = createPkStr(":Database Replica", "Redo blocked/sec"                 , dbname);
		
		// Find column Id's
		List<String> colNames = newSample.getColNames();
		if (colNames == null)
			return;

		for (int colId=0; colId < colNames.size(); colId++) 
		{
			String colName = colNames.get(colId);

			if      (colName.equals("locality"                          )) pos_locality                           = colId;
			else if (colName.equals("server_name"                       )) pos_server_name                        = colId;
			else if (colName.equals("ag_name"                           )) pos_ag_name                            = colId;
			else if (colName.equals("database_name"                     )) pos_database_name                      = colId;
			else if (colName.equals("role_desc"                         )) pos_role_desc                          = colId;
			
			else if (colName.equals("PRIMARY_server_name"               )) pos_PRIMARY_server_name                = colId;
			else if (colName.equals("last_commit_time"                  )) pos_last_commit_time                   = colId;
			else if (colName.equals("SecondaryCommitTimeLag"            )) pos_SecondaryCommitTimeLag             = colId;
			
			else if (colName.equals("AtServerName"                      )) pos_AtServerName                       = colId;

			else if (colName.equals("p_TransactionsPerSec"              )) pos_p_TransactionsPerSec               = colId;
			else if (colName.equals("p_WriteTransactionsPerSec"         )) pos_p_WriteTransactionsPerSec          = colId;
			else if (colName.equals("p_LogBytesFlushedPerSec"           )) pos_p_LogBytesFlushedPerSec            = colId;
			else if (colName.equals("p_LogFlushesPerSec"                )) pos_p_LogFlushesPerSec                 = colId;
			
			else if (colName.equals("p_MirroredWriteTransactionsPerSec" )) pos_p_MirroredWriteTransactionsPerSec  = colId;
			else if (colName.equals("p_TransactionDelay"                )) pos_p_TransactionDelay                 = colId;
			else if (colName.equals("p_GroupCommitTime"                 )) pos_p_GroupCommitTime                  = colId;
			else if (colName.equals("p_GroupCommitsPerSec"              )) pos_p_GroupCommitsPerSec               = colId;
			else if (colName.equals("p_LogBytesCompressedPerSec"        )) pos_p_LogBytesCompressedPerSec         = colId;
			else if (colName.equals("p_LogCompressionsPerSec"           )) pos_p_LogCompressionsPerSec            = colId;
			else if (colName.equals("p_LogCompressionCachemissesPerSec" )) pos_p_LogCompressionCachemissesPerSec  = colId;
			else if (colName.equals("p_LogCompressionCachehitsPerSec"   )) pos_p_LogCompressionCachehitsPerSec    = colId;
			else if (colName.equals("p_LogBytesDecompressedPerSec"      )) pos_p_LogBytesDecompressedPerSec       = colId;
			else if (colName.equals("p_LogDecompressionsPerSec"         )) pos_p_LogDecompressionsPerSec          = colId;
			else if (colName.equals("p_DatabaseFlowControlDelay"        )) pos_p_DatabaseFlowControlDelay         = colId;
			else if (colName.equals("p_DatabaseFlowControlsPerSec"      )) pos_p_DatabaseFlowControlsPerSec       = colId;

			else if (colName.equals("p2s_BytesSentToReplicaPerSec"      )) pos_p2s_BytesSentToReplicaPerSec       = colId;
			else if (colName.equals("p2s_SendsToReplicaPerSec"          )) pos_p2s_SendsToReplicaPerSec           = colId;
			else if (colName.equals("p2s_BytesSentToTransportPerSec"    )) pos_p2s_BytesSentToTransportPerSec     = colId;
			else if (colName.equals("p2s_SendsToTransportPerSec"        )) pos_p2s_SendsToTransportPerSec         = colId;
			else if (colName.equals("p2s_BytesReceivedFromReplicaPerSec")) pos_p2s_BytesReceivedFromReplicaPerSec = colId;
			else if (colName.equals("p2s_ReceivesFromReplicaPerSec"     )) pos_p2s_ReceivesFromReplicaPerSec      = colId;
			else if (colName.equals("p2s_FlowControlTimeMsPerSec"       )) pos_p2s_FlowControlTimeMsPerSec        = colId;
			else if (colName.equals("p2s_FlowControlPerSec"             )) pos_p2s_FlowControlPerSec              = colId;
			else if (colName.equals("p2s_ResentMessagesPerSec"          )) pos_p2s_ResentMessagesPerSec           = colId;
			
			else if (colName.equals("s_LogSendQueue"                    )) pos_s_LogSendQueue                     = colId;
			else if (colName.equals("s_RecoveryQueue"                   )) pos_s_RecoveryQueue                    = colId;
			else if (colName.equals("s_RedoneBytesPerSec"               )) pos_s_RedoneBytesPerSec                = colId;
			else if (colName.equals("s_RedonesPerSec"                   )) pos_s_RedonesPerSec                    = colId;
			else if (colName.equals("s_LogApplyReadyQueue"              )) pos_s_LogApplyReadyQueue               = colId;
			else if (colName.equals("s_LogApplyPendingQueue"            )) pos_s_LogApplyPendingQueue             = colId;
			else if (colName.equals("s_LogBytesReceivedPerSec"          )) pos_s_LogBytesReceivedPerSec           = colId;
			else if (colName.equals("s_FileBytesReceivedPerSec"         )) pos_s_FileBytesReceivedPerSec          = colId;
			else if (colName.equals("s_TotalLogRequiringUndo"           )) pos_s_TotalLogRequiringUndo            = colId;
			else if (colName.equals("s_LogRemainingForUndo"             )) pos_s_LogRemainingForUndo              = colId;
			else if (colName.equals("s_RedoBytesRemaining"              )) pos_s_RedoBytesRemaining               = colId;
			else if (colName.equals("s_RedoBlockedPerSec"               )) pos_s_RedoBlockedPerSec                = colId;
		}

		CmPerfCounters perfCounters = (CmPerfCounters) getCounterController().getCmByName(CmPerfCounters.class.getSimpleName());
		
		// Loop on all rows
		for (int rowId = 0; rowId < newSample.getRowCount(); rowId++)
		{
			String locality      = "" + newSample.getValueAt(rowId, pos_locality);
			String server_name   = "" + newSample.getValueAt(rowId, pos_server_name);
			String dbname        = "" + newSample.getValueAt(rowId, pos_database_name);
			String ag_name       = "" + newSample.getValueAt(rowId, pos_ag_name);
			String role_desc     = "" + newSample.getValueAt(rowId, pos_role_desc);

			_localSrvName        = "" + newSample.getValueAt(rowId, pos_AtServerName);

			boolean isLocal     = COLVAL_LOCALITY_LOCAL .equals(locality);
			boolean isRemote    = COLVAL_LOCALITY_REMOTE.equals(locality);
			boolean isPrimary   = PRIMARY_ROLE  .equals(role_desc);
			boolean isSecondary = SECONDARY_ROLE.equals(role_desc);

			// Add "remote" servers to a Set so we can get/extract ACTUAL or "real time" role/status information from each remote.
			// This can be used to check for multiple PRIMARY entries (or SPLIT BRAN) which can happen on a "unplanned failover"
//			if (isRemote)
//				_secondaryServerSet.add(server_name);
			
			// Below Counters in 'perfCounters' is only valid if server is LOCAL and it's a PRIMARY
			if (isLocal && isPrimary)
			{
				setValue(newSample, rowId, pos_p_TransactionsPerSec             , perfCounters, createPkStr(":Databases"       , "Transactions/sec"                 , dbname));
				setValue(newSample, rowId, pos_p_WriteTransactionsPerSec        , perfCounters, createPkStr(":Databases"       , "Write Transactions/sec"           , dbname));
				setValue(newSample, rowId, pos_p_LogBytesFlushedPerSec          , perfCounters, createPkStr(":Databases"       , "Log Bytes Flushed/sec"            , dbname));
				setValue(newSample, rowId, pos_p_LogFlushesPerSec               , perfCounters, createPkStr(":Databases"       , "Log Flushes/sec"                  , dbname));

				setValue(newSample, rowId, pos_p_MirroredWriteTransactionsPerSec, perfCounters, createPkStr(":Database Replica", "Mirrored Write Transactions/sec"  , dbname));
				setValue(newSample, rowId, pos_p_TransactionDelay               , perfCounters, createPkStr(":Database Replica", "Transaction Delay"                , dbname));
				setValue(newSample, rowId, pos_p_GroupCommitTime                , perfCounters, createPkStr(":Database Replica", "Group Commit Time"                , dbname));
				setValue(newSample, rowId, pos_p_GroupCommitsPerSec             , perfCounters, createPkStr(":Database Replica", "Group Commits/Sec"                , dbname));
				setValue(newSample, rowId, pos_p_LogBytesCompressedPerSec       , perfCounters, createPkStr(":Database Replica", "Log Bytes Compressed/sec"         , dbname));
				setValue(newSample, rowId, pos_p_LogCompressionsPerSec          , perfCounters, createPkStr(":Database Replica", "Log Compressions/sec"             , dbname));
				setValue(newSample, rowId, pos_p_LogCompressionCachemissesPerSec, perfCounters, createPkStr(":Database Replica", "Log Compression Cache misses/sec" , dbname));
				setValue(newSample, rowId, pos_p_LogCompressionCachehitsPerSec  , perfCounters, createPkStr(":Database Replica", "Log Compression Cache hits/sec"   , dbname));
				setValue(newSample, rowId, pos_p_LogBytesDecompressedPerSec     , perfCounters, createPkStr(":Database Replica", "Log Bytes Decompressed/sec"       , dbname));
				setValue(newSample, rowId, pos_p_LogDecompressionsPerSec        , perfCounters, createPkStr(":Database Replica", "Log Decompressions/sec"           , dbname));
				setValue(newSample, rowId, pos_p_DatabaseFlowControlDelay       , perfCounters, createPkStr(":Database Replica", "Database Flow Control Delay"      , dbname));
				setValue(newSample, rowId, pos_p_DatabaseFlowControlsPerSec     , perfCounters, createPkStr(":Database Replica", "Database Flow Controls/sec"       , dbname));
			}

			// Below Counters in 'perfCounters' is only valid if server is NOT LOCAL and it's a SECONDARY
			// Yes it sound strange: But there can be MANY SECONDARYS for a PRIMARY...
			if (isRemote && isSecondary)
			{
				String agNameAndReplicaSrvName = ag_name + ":" + server_name; // AG_gs1:prod-2b-mssql
				
				setValue(newSample, rowId, pos_p2s_BytesSentToReplicaPerSec             , perfCounters, createPkStr(":Availability Replica", "Bytes Sent to Replica/sec"       , agNameAndReplicaSrvName));
				setValue(newSample, rowId, pos_p2s_SendsToReplicaPerSec                 , perfCounters, createPkStr(":Availability Replica", "Sends to Replica/sec"            , agNameAndReplicaSrvName));
				setValue(newSample, rowId, pos_p2s_BytesSentToTransportPerSec           , perfCounters, createPkStr(":Availability Replica", "Bytes Sent to Transport/sec"     , agNameAndReplicaSrvName));
				setValue(newSample, rowId, pos_p2s_SendsToTransportPerSec               , perfCounters, createPkStr(":Availability Replica", "Sends to Transport/sec"          , agNameAndReplicaSrvName));
				setValue(newSample, rowId, pos_p2s_BytesReceivedFromReplicaPerSec       , perfCounters, createPkStr(":Availability Replica", "Bytes Received from Replica/sec" , agNameAndReplicaSrvName));
				setValue(newSample, rowId, pos_p2s_ReceivesFromReplicaPerSec            , perfCounters, createPkStr(":Availability Replica", "Receives from Replica/sec"       , agNameAndReplicaSrvName));
				setValue(newSample, rowId, pos_p2s_FlowControlTimeMsPerSec              , perfCounters, createPkStr(":Availability Replica", "Flow Control Time (ms/sec)"      , agNameAndReplicaSrvName));
				setValue(newSample, rowId, pos_p2s_FlowControlPerSec                    , perfCounters, createPkStr(":Availability Replica", "Flow Control/sec"                , agNameAndReplicaSrvName));
				setValue(newSample, rowId, pos_p2s_ResentMessagesPerSec                 , perfCounters, createPkStr(":Availability Replica", "Resent Messages/sec"             , agNameAndReplicaSrvName));
			}

			// Try to update: SecondaryCommitTimeLag
			// get 'last_commit_time' for CURRENT row and for 'PRIMARY_server_name'
			// Then diff the result:
			//   - put the diff in the REMOTE row
			//   - put the MAX-diff in the LOCAL (primary) row, with the remote-servername as a "prefix"
			if (isRemote && isSecondary)
			{
				Object o_lct = newSample.getValueAt(rowId, pos_last_commit_time);
				String PRIMARY_server_name = "" + newSample.getValueAt(rowId, pos_PRIMARY_server_name);
				if (o_lct != null && o_lct instanceof Timestamp)
				{
					int primaryRowId = getRowIdForLocalPrimarySrvDb(newSample, 
							pos_locality     , COLVAL_LOCALITY_LOCAL, 
							pos_role_desc    , PRIMARY_ROLE, 
							pos_server_name  , PRIMARY_server_name, 
							pos_database_name, dbname);
					
					if (primaryRowId >= 0)
					{
						Timestamp this_lct = (Timestamp) o_lct;
						Timestamp prim_lct = (Timestamp) newSample.getValueAt(primaryRowId, pos_last_commit_time);
						
						long   msDiff  = prim_lct.getTime() - this_lct.getTime();
						String diffStr = TimeUtils.msToTimeStr(msDiff);

						// Set the DIFF at this REMOTE ROW
						newSample.setValueAt(diffStr, rowId, pos_SecondaryCommitTimeLag);

						// Get the PRIMARY local row
						String PRIMARY_SecondaryCommitTimeLag = (String) newSample.getValueAt(primaryRowId, pos_SecondaryCommitTimeLag);
						if (StringUtil.hasValue(PRIMARY_SecondaryCommitTimeLag))
						{
							// parse the entry: srvname=HH:MM:SS.ms
							try
							{
								String[] sa = PRIMARY_SecondaryCommitTimeLag.split("=");
								Timestamp prevTs = TimeUtils.parseToTimestamp(sa[0], "HH:mm:ss.SSS");
								
								// Set the server name with the highest time diff
								if (msDiff > prevTs.getTime())
								{
									String val = server_name + "=" + diffStr;
									newSample.setValueAt(val, primaryRowId, pos_SecondaryCommitTimeLag);
								}
							}
							catch (ParseException ignore) {}
						}
						else
						{
							String val = server_name + "=" + diffStr;
							newSample.setValueAt(val, primaryRowId, pos_SecondaryCommitTimeLag);
						}
						
					}
				}
			}

			// Below Counters in 'perfCounters' is only valid if server is LOCAL and it's a SECONDARY
			if (isLocal && isSecondary)
			{
				setValue(newSample, rowId, pos_s_LogSendQueue                   , perfCounters, createPkStr(":Database Replica", "Log Send Queue"                   , dbname));
				setValue(newSample, rowId, pos_s_RecoveryQueue                  , perfCounters, createPkStr(":Database Replica", "Recovery Queue"                   , dbname));
				setValue(newSample, rowId, pos_s_RedoneBytesPerSec              , perfCounters, createPkStr(":Database Replica", "Redone Bytes/sec"                 , dbname));
				setValue(newSample, rowId, pos_s_RedonesPerSec                  , perfCounters, createPkStr(":Database Replica", "Redones/sec"                      , dbname));
				setValue(newSample, rowId, pos_s_LogApplyReadyQueue             , perfCounters, createPkStr(":Database Replica", "Log Apply Ready Queue"            , dbname));
				setValue(newSample, rowId, pos_s_LogApplyPendingQueue           , perfCounters, createPkStr(":Database Replica", "Log Apply Pending Queue"          , dbname));
				setValue(newSample, rowId, pos_s_LogBytesReceivedPerSec         , perfCounters, createPkStr(":Database Replica", "Log Bytes Received/sec"           , dbname));
				setValue(newSample, rowId, pos_s_FileBytesReceivedPerSec        , perfCounters, createPkStr(":Database Replica", "File Bytes Received/sec"          , dbname));
				setValue(newSample, rowId, pos_s_TotalLogRequiringUndo          , perfCounters, createPkStr(":Database Replica", "Total Log requiring undo"         , dbname));
				setValue(newSample, rowId, pos_s_LogRemainingForUndo            , perfCounters, createPkStr(":Database Replica", "Log remaining for undo"           , dbname));
				setValue(newSample, rowId, pos_s_RedoBytesRemaining             , perfCounters, createPkStr(":Database Replica", "Redo Bytes Remaining"             , dbname));
				setValue(newSample, rowId, pos_s_RedoBlockedPerSec              , perfCounters, createPkStr(":Database Replica", "Redo blocked/sec"                 , dbname));
			}
		}		
	}
	
	private int getRowIdForLocalPrimarySrvDb(CounterSample newSample, int pos_locality, String locality, int pos_role_desc, String role_desc, int pos_server_name, String PRIMARY_server_name, int pos_dbname, String dbname)
	{
		for (int rowId = 0; rowId < newSample.getRowCount(); rowId++)
		{
			String r_locality      = "" + newSample.getValueAt(rowId, pos_locality);
			String r_role_desc     = "" + newSample.getValueAt(rowId, pos_role_desc);
			String r_server_name   = "" + newSample.getValueAt(rowId, pos_server_name);
			String r_dbname        = "" + newSample.getValueAt(rowId, pos_dbname);

			if (r_locality.equals(locality) && r_role_desc.equals(role_desc) && r_server_name.equals(PRIMARY_server_name) && r_dbname.equals(dbname))
				return rowId;
		}
		return -1;
	}

	private void setValue(CounterSample newSample, int rowId, int colPos, CmPerfCounters perfCounters, String pk)
	{
		if (colPos == -1)
		{
			System.out.println("colPos: was -1 for row=" + rowId);
			return;
		}
		
		Double val = perfCounters.getAbsValueAsDouble(pk, "calculated_value");
		newSample.setValueAt(val, rowId, colPos);

		// not found and NOT first sample... write some info message
		if ( val == null && ! perfCounters.isFirstTimeSample() )
		{
			_logger.info("NO VALUE FOR: pk=" + pk );
		}
	}

	@Override
	public void sendAlarmRequest()
	{
		if ( ! hasDiffData() )
			return;
		
		if ( ! AlarmHandler.hasInstance() )
			return;

		AlarmHandler alarmHandler = AlarmHandler.getInstance();
		
		CountersModel cm = this;
//		String dbmsSrvName = cm.getServerName();

		boolean debugPrint = System.getProperty("sendAlarmRequest.debug", "false").equalsIgnoreCase("true");

		for (int r=0; r<cm.getDiffRowCount(); r++)
		{
			String locality            = cm.getAbsString(r, "locality");
			String server_name         = cm.getAbsString(r, "server_name");
			String dbname              = cm.getAbsString(r, "database_name");
			String ag_name             = cm.getAbsString(r, "ag_name");
			String role_desc           = cm.getAbsString(r, "role_desc");
			String Validated           = cm.getAbsString(r, "Validated");
			
			String PRIMARY_server_name = cm.getAbsString(r, "PRIMARY_server_name");
			String AtServerName        = cm.getAbsString(r, "AtServerName");

			boolean isLocal            = COLVAL_LOCALITY_LOCAL           .equals(locality);
			boolean isRemote           = COLVAL_LOCALITY_REMOTE          .equals(locality);
//			boolean isRemoteLiveData   = COLVAL_LOCALITY_REMOTE_LIVE_DATA.equals(locality);
//			boolean isPrimary          = PRIMARY_ROLE  .equals(role_desc);
//			boolean isSecondary        = SECONDARY_ROLE.equals(role_desc);
			boolean isOnPrimaryServer  = PRIMARY_server_name != null && PRIMARY_server_name.equals(AtServerName);

			// availability_replicas
			String AR_availability_mode_desc               = cm.getAbsString(r, "availability_mode_desc");
//			String AR_primary_role_allow_connections_desc  = cm.getAbsString(r, "primary_role_allow_connections_desc");

			// dm_hadr_availability_group_states
//			String AGS_primary_recovery_health_desc         = cm.getAbsString(r, "primary_recovery_health_desc");
//			String AGS_secondary_recovery_health_desc       = cm.getAbsString(r, "secondary_recovery_health_desc");
			String AGS_synchronization_health_desc          = cm.getAbsString(r, "synchronization_health_desc");

			// dm_hadr_availability_replica_states
//			String ARS_role_desc                            = cm.getAbsString(r, "role_desc");
			String ARS_operational_state_desc               = cm.getAbsString(r, "operational_state_desc");
			String ARS_recovery_health_desc                 = cm.getAbsString(r, "recovery_health_desc");
//			String ARS_synchronization_health_desc          = cm.getAbsString(r, "synchronization_health_desc");
			String ARS_connected_state_desc                 = cm.getAbsString(r, "connected_state_desc");

			// dm_hadr_database_replica_states
			String DRS_synchronization_state_desc           = cm.getAbsString(r, "synchronization_state_desc");
//			String DRS_synchronization_health_desc          = cm.getAbsString(r, "synchronization_health_desc");
			String DRS_database_state_desc                  = cm.getAbsString(r, "database_state_desc");
			String DRS_suspend_reason_desc                  = cm.getAbsString(r, "suspend_reason_desc");


			//-------------------------------------------------------
			// Role Change
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("RoleChange"))
			{
				String prevSampleRoleKey = locality + ":" + server_name + ":" + dbname;
				String prevSampleRole = _previousSampleRole.get(prevSampleRoleKey);

				if (prevSampleRole != null)
				{
					// ROLE CHANGE
					if ( ! prevSampleRole.equals(role_desc) )
					{
						String extendedDescText = cm.toTextTableString(DATA_RATE, r);
						String extendedDescHtml = cm.toHtmlTableString(DATA_RATE, r, true, false, false);
						
						AlarmEvent ae = new AlarmEventAgRoleChange(cm, ag_name, server_name, dbname, prevSampleRole, role_desc);
						ae.setExtendedDescription(extendedDescText, extendedDescHtml);
						
						alarmHandler.addAlarm( ae );
					}
				}

				// Remember the PREVIOUS role for NEXT sample
				_previousSampleRole.put(prevSampleRoleKey, role_desc);
			}

//			//-------------------------------------------------------
//			// State Change
//			//-------------------------------------------------------
//			if (isSystemAlarmsForColumnEnabledAndInTimeRange("StateChange"))
//			{
//				String prevSampleRoleKey = locality + ":" + server_name + ":" + dbname;
//				String prevSampleRole = _previousSampleRole.get(prevSampleRoleKey);
//
//				if (prevSampleRole != null)
//				{
//					// ROLE CHANGE
//					if ( ! prevSampleRole.equals(role_desc) )
//					{
//						String extendedDescText = cm.toTextTableString(DATA_RATE, r);
//						String extendedDescHtml = cm.toHtmlTableString(DATA_RATE, r, true, false, false);
//						AlarmEvent ae = new AlarmEventAgStateChange(cm, ag_name, server_name, dbname, prevSampleRole, role_desc);
//						ae.setExtendedDescription(extendedDescText, extendedDescHtml);
//						
//						alarmHandler.addAlarm( ae );
//					}
//				}
//
//				// Remember the PREVIOUS role for NEXT sample
//				_previousSampleRole.put(prevSampleRoleKey, role_desc);
//			}

			//-------------------------------------------------------
			// UnExpectedState
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("UnExpectedState"))
			{
//				if (debugPrint || _logger.isDebugEnabled())
//					System.out.println("##### sendAlarmRequest("+cm.getName()+"): ag_name='"+ag_name+"', server_name='"+server_name+"', dbname='"+dbname+"', XXXXXX='"+XXXXX+"'.");

				String stateStr = "";
				
				if (isRemote && isOnPrimaryServer)
				{
					if (  "SYNCHRONOUS_COMMIT".equals(AR_availability_mode_desc) &&  ! StringUtil.equalsAny(DRS_synchronization_state_desc, "SYNCHRONIZED"                 ) ) stateStr += "synchronization_state='"  + DRS_synchronization_state_desc  + "', ";
					if ( "ASYNCHRONOUS_COMMIT".equals(AR_availability_mode_desc) &&  ! StringUtil.equalsAny(DRS_synchronization_state_desc, "SYNCHRONIZED", "SYNCHRONIZING") ) stateStr += "synchronization_state='"  + DRS_synchronization_state_desc  + "', ";
					if ( ! "HEALTHY"      .equals(AGS_synchronization_health_desc) ) stateStr += "synchronization_health='" + AGS_synchronization_health_desc + "', ";
//					if ( ! "ONLINE"       .equals(ARS_operational_state_desc     ) ) stateStr += "operational_state='"      + ARS_operational_state_desc      + "', ";
					if ( ! "CONNECTED"    .equals(ARS_connected_state_desc       ) ) stateStr += "connected_state='"        + ARS_connected_state_desc        + "', ";
//					if ( ! "ONLINE"       .equals(ARS_recovery_health_desc       ) ) stateStr += "recovery_health='"        + ARS_recovery_health_desc        + "', ";
					if ( StringUtil     .hasValue(DRS_suspend_reason_desc)         ) stateStr += "suspend_reason='"         + DRS_suspend_reason_desc         + "', ";
//					if ( ! "ONLINE"       .equals(DRS_database_state_desc        ) ) stateStr += "database_state='"         + DRS_database_state_desc         + "', ";
				}
				else if (isLocal)
				{
					if (  "SYNCHRONOUS_COMMIT".equals(AR_availability_mode_desc) &&  ! StringUtil.equalsAny(DRS_synchronization_state_desc, "SYNCHRONIZED"                 ) ) stateStr += "synchronization_state='"  + DRS_synchronization_state_desc  + "', ";
					if ( "ASYNCHRONOUS_COMMIT".equals(AR_availability_mode_desc) &&  ! StringUtil.equalsAny(DRS_synchronization_state_desc, "SYNCHRONIZED", "SYNCHRONIZING") ) stateStr += "synchronization_state='"  + DRS_synchronization_state_desc  + "', ";
					if ( ! "HEALTHY"      .equals(AGS_synchronization_health_desc) ) stateStr += "synchronization_health='" + AGS_synchronization_health_desc + "', ";
					if ( ! "ONLINE"       .equals(ARS_operational_state_desc     ) ) stateStr += "operational_state='"      + ARS_operational_state_desc      + "', ";
					if ( ! "CONNECTED"    .equals(ARS_connected_state_desc       ) ) stateStr += "connected_state='"        + ARS_connected_state_desc        + "', ";
					if ( ! "ONLINE"       .equals(ARS_recovery_health_desc       ) ) stateStr += "recovery_health='"        + ARS_recovery_health_desc        + "', ";
					if ( StringUtil     .hasValue(DRS_suspend_reason_desc)         ) stateStr += "suspend_reason='"         + DRS_suspend_reason_desc         + "', ";
					if ( ! "ONLINE"       .equals(DRS_database_state_desc        ) ) stateStr += "database_state='"         + DRS_database_state_desc         + "', ";
				}


				// MAKE Alarm (if needed)
				if (StringUtil.hasValue(stateStr))
				{
					stateStr = StringUtil.removeLastComma(stateStr);
					
					String extendedDescText = cm.toTextTableString(DATA_RATE, r);
					String extendedDescHtml = cm.toHtmlTableString(DATA_RATE, r, true, false, false);
					
					AlarmEvent ae = new AlarmEventAgUnexpectedState(cm, ag_name, server_name, dbname, stateStr);
					ae.setExtendedDescription(extendedDescText, extendedDescHtml);
					
					alarmHandler.addAlarm( ae );
				}
			}

			//-------------------------------------------------------
			// LogSendQueueSizeInMb
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("LogSendQueueSizeInMb"))
			{
				// above 20M
				// When Lagging more than XX seconds
//				if (isLocal && isOnPrimaryServer)
				if (isLocal)
				{
					Double LogSendQueueSizeKb = cm.getAbsValueAsDouble(r, "LogSendQueueSizeKb");
					if (LogSendQueueSizeKb != null)
					{
						int LogSendQueueSizeMb = LogSendQueueSizeKb.intValue() / 1024;
						
						if (debugPrint || _logger.isDebugEnabled())
							System.out.println("##### sendAlarmRequest("+cm.getName()+"): ag_name='"+ag_name+"', server_name='"+server_name+"', dbname='"+dbname+"', LogSendQueueSizeMb='"+LogSendQueueSizeMb+"'.");

						int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_LogSendQueueSizeInMb, DEFAULT_alarm_LogSendQueueSizeInMb);
						if (LogSendQueueSizeMb > threshold)
						{
							String extendedDescText = cm.toTextTableString(DATA_RATE, r);
							String extendedDescHtml = cm.toHtmlTableString(DATA_RATE, r, true, false, false);
							
							AlarmEvent ae = new AlarmEventAgLogSendQueueSize(cm, ag_name, server_name, dbname, LogSendQueueSizeMb, threshold);
							ae.setExtendedDescription(extendedDescText, extendedDescHtml);
							
							alarmHandler.addAlarm( ae );
						}
						
					}
				}
			}

			//-------------------------------------------------------
			// SecondaryCommitTimeLag
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("SecondaryCommitTimeLag"))
			{
				// When Lagging more than XX seconds
				if (isRemote || isOnPrimaryServer)
				{
					String SecondaryCommitTimeLag = cm.getAbsString(r, "SecondaryCommitTimeLag");
					if (StringUtil.hasValue(SecondaryCommitTimeLag))
					{
						if (debugPrint || _logger.isDebugEnabled())
							System.out.println("##### sendAlarmRequest("+cm.getName()+"): ag_name='"+ag_name+"', server_name='"+server_name+"', dbname='"+dbname+"', SecondaryCommitTimeLag='"+SecondaryCommitTimeLag+"'.");

						int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_SecondaryCommitTimeLagInSeconds, DEFAULT_alarm_SecondaryCommitTimeLagInSeconds);
						try 
						{
							Timestamp secondaryCommitTimeLagTs = TimeUtils.parseToUtcTimestamp(SecondaryCommitTimeLag, "HH:mm:ss.SSS");
							long secondaryCommitTimeLagSec = -1;
							if (secondaryCommitTimeLagTs != null)
								secondaryCommitTimeLagSec = secondaryCommitTimeLagTs.getTime() / 1000;
							
							if (secondaryCommitTimeLagSec > threshold)
							{
								String extendedDescText = cm.toTextTableString(DATA_RATE, r);
								String extendedDescHtml = cm.toHtmlTableString(DATA_RATE, r, true, false, false);

								AlarmEvent ae = new AlarmEventAgSecondaryCommitTimeLag(cm, ag_name, server_name, dbname, SecondaryCommitTimeLag, threshold);
								ae.setExtendedDescription(extendedDescText, extendedDescHtml);
								
								alarmHandler.addAlarm( ae );
							}
						} 
						catch (ParseException ignore) {}
					}
				}

			}

			//-------------------------------------------------------
			// SplitBrain
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("SplitBrain"))
			{
//				if ("INVALID".equals(Validated))
//				{
//					String extendedDescText = cm.toTextTableString(DATA_RATE, r);
//					String extendedDescHtml = cm.toHtmlTableString(DATA_RATE, r, true, false, false);
//					
//					AlarmEvent ae = new AlarmEventAgNotValid(cm, ag_name, server_name, dbname, role_desc);
//					ae.setExtendedDescription(extendedDescText, extendedDescHtml);
//					
//					alarmHandler.addAlarm( ae );
//				}

				if (isLocal && "SPLIT-BRAIN".equals(Validated))
				{
					String remoteLiveDataServername = "-unknown-";
					
					// Get the REMOTE-LIVE-DATA record
					HashMap<String, Object> whereClauseRemoteLiveData = new HashMap<>(4);
					whereClauseRemoteLiveData.put("locality",       COLVAL_LOCALITY_REMOTE_LIVE_DATA);
//					whereClauseRemoteLiveData.put("server_name",    server_name);
					whereClauseRemoteLiveData.put("ag_name",        ag_name);
					whereClauseRemoteLiveData.put("database_name",  dbname);
					
					List<Integer> rowsRemoteLiveData = getAbsRowIdsWhere(whereClauseRemoteLiveData);

					if (rowsRemoteLiveData.size() == 1)
					{
						int rowIdRemoteLiveData = rowsRemoteLiveData.get(0);
						remoteLiveDataServername = getAbsString(rowIdRemoteLiveData, "server_name");
					}
					
					// Create and send the alarm
					String extendedDescText = cm.toTextTableString(DATA_RATE, r);
					String extendedDescHtml = cm.toHtmlTableString(DATA_RATE, r, true, false, false);
					
					AlarmEvent ae = new AlarmEventAgSplitBrain(cm, ag_name, server_name, dbname, remoteLiveDataServername);
					ae.setExtendedDescription(extendedDescText, extendedDescHtml);
					
					alarmHandler.addAlarm( ae );
				}
			}

			//-------------------------------------------------------
			// BagPercentSkewed
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("BagRolePercentSkewed"))
			{
				Double BagPct = cm.getAbsValueAsDouble(r, "BagPct");
				if (BagPct != null)
				{
					int bagPct = BagPct.intValue();
					
					if (debugPrint || _logger.isDebugEnabled())
						System.out.println("##### sendAlarmRequest("+cm.getName()+"): ag_name='"+ag_name+"', bagPct='"+bagPct+"'.");

					if (bagPct < 100 && bagPct != -1)
					{
						String agNamePrefix      = getAgNamePrefix(ag_name);
						String BagPercentDetails = cm.getAbsString(r, "BagPercentDetails");
						
						String extendedDescText = cm.toTextTableString(DATA_RATE, r);
						String extendedDescHtml = cm.toHtmlTableString(DATA_RATE, r, true, false, false);
						
						AlarmEvent ae = new AlarmEventBagRolePercentSkewed(cm, locality, agNamePrefix, bagPct, BagPercentDetails);
						ae.setExtendedDescription(extendedDescText, extendedDescHtml);
						
						alarmHandler.addAlarm( ae );
					}
					
				}
				
			}
		} // end: loop rows
	}


	public static final String  PROPKEY_alarm_RoleChange                      = CM_NAME + ".alarm.system.on.RoleChange";
	public static final boolean DEFAULT_alarm_RoleChange                      = true;

	public static final String  PROPKEY_alarm_BagRolePercentSkewed            = CM_NAME + ".alarm.system.on.BagPercentSkewed";
	public static final boolean DEFAULT_alarm_BagRolePercentSkewed            = true;

//	public static final String  PROPKEY_alarm_StateChange                     = CM_NAME + ".alarm.system.on.StateChange";
//	public static final boolean DEFAULT_alarm_StateChange                     = true;

	public static final String  PROPKEY_alarm_UnExpectedState                 = CM_NAME + ".alarm.system.on.UnExpectedState";
	public static final boolean DEFAULT_alarm_UnExpectedState                 = true;

	public static final String  PROPKEY_alarm_LogSendQueueSizeInMb            = CM_NAME + ".alarm.system.if.LogSendQueueSizeInMb.gt";
	public static final int     DEFAULT_alarm_LogSendQueueSizeInMb            = 20;

	public static final String  PROPKEY_alarm_SecondaryCommitTimeLagInSeconds = CM_NAME + ".alarm.system.if.SecondaryCommitTimeLagInSeconds.gt";
	public static final int     DEFAULT_alarm_SecondaryCommitTimeLagInSeconds = 15 * 60;

	public static final String  PROPKEY_alarm_SplitBrain                      = CM_NAME + ".alarm.system.on.SplitBrain";
	public static final boolean DEFAULT_alarm_SplitBrain                      = true;


	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;

		list.add(new CmSettingsHelper("SplitBrain"            , isAlarmSwitch, PROPKEY_alarm_SplitBrain                      , Boolean.class, conf.getBooleanProperty(PROPKEY_alarm_SplitBrain                      , DEFAULT_alarm_SplitBrain                      ), DEFAULT_alarm_SplitBrain                      , "if 'Sample Live Remote Data' is enabled and 'role_desc' is 'PRIMARY' at both sides... send 'AlarmEventAgSplitBrain'." ));
		list.add(new CmSettingsHelper("BagRolePercentSkewed"  , isAlarmSwitch, PROPKEY_alarm_BagRolePercentSkewed            , Boolean.class, conf.getBooleanProperty(PROPKEY_alarm_BagRolePercentSkewed            , DEFAULT_alarm_BagRolePercentSkewed            ), DEFAULT_alarm_BagRolePercentSkewed            , "if 'basic_features' is true and 'ag_name' group by PREFIX (chars before '_' or '-'), then ALL 'role_desc' should be the same in the LogicalGroup if this is not 100%. Which means: That some database(s) (within the logical group) is PRIMARY at the wrong location, which might be considdered as a 'SPLIT-BRAIN' but on a *logical level*. Send 'AlarmEventBagRolsPercentSkewed'." ));
		list.add(new CmSettingsHelper("RoleChange"            , isAlarmSwitch, PROPKEY_alarm_RoleChange                      , Boolean.class, conf.getBooleanProperty(PROPKEY_alarm_RoleChange                      , DEFAULT_alarm_RoleChange                      ), DEFAULT_alarm_RoleChange                      , "When 'role_desc' changes, send 'AlarmEventAgRoleChange'." ));
//		list.add(new CmSettingsHelper("StateChange"           , isAlarmSwitch, PROPKEY_alarm_StateChange                     , Boolean.class, conf.getBooleanProperty(PROPKEY_alarm_StateChange                     , DEFAULT_alarm_StateChange                     ), DEFAULT_alarm_StateChange                     , "When any of the columns 'xxx', 'yyy', 'zzz' changes, send 'AlarmEventAgStateChange'." ));
		list.add(new CmSettingsHelper("UnExpectedState"       , isAlarmSwitch, PROPKEY_alarm_UnExpectedState                 , Boolean.class, conf.getBooleanProperty(PROPKEY_alarm_UnExpectedState                 , DEFAULT_alarm_UnExpectedState                 ), DEFAULT_alarm_UnExpectedState                 , "When any of the columns 'xxx', 'yyy', 'zzz' contains an 'UnExpected State', send 'AlarmEventAgUnexpectedState'." ));
		list.add(new CmSettingsHelper("LogSendQueueSizeInMb"  , isAlarmSwitch, PROPKEY_alarm_LogSendQueueSizeInMb            , Integer.class, conf.getIntProperty    (PROPKEY_alarm_LogSendQueueSizeInMb            , DEFAULT_alarm_LogSendQueueSizeInMb            ), DEFAULT_alarm_LogSendQueueSizeInMb            , "If 'LogSendQueueSizeKb' is greater than ## (in MB) then send 'AlarmEventAgLogSendQueueSize'." ));
		list.add(new CmSettingsHelper("SecondaryCommitTimeLag", isAlarmSwitch, PROPKEY_alarm_SecondaryCommitTimeLagInSeconds , Integer.class, conf.getIntProperty    (PROPKEY_alarm_SecondaryCommitTimeLagInSeconds , DEFAULT_alarm_SecondaryCommitTimeLagInSeconds ), DEFAULT_alarm_SecondaryCommitTimeLagInSeconds , "If 'SecondaryCommitTimeLag' is greater than ### Seconds, for any entry where 'locality' is 'REMOTE' and this is the PRIMARY server, then send 'AlarmEventAgSecondaryCommitTimeLag'." ));

		return list;
	}	
}
