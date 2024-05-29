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

import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.naming.NameNotFoundException;

import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.alarm.AlarmHandler;
import com.asetune.alarm.events.AlarmEvent;
import com.asetune.alarm.events.postgres.AlarmEventPgReplicationLag;
import com.asetune.central.pcs.CentralPersistReader;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.graph.TrendGraphDataPoint.LabelType;
import com.asetune.gui.MainFramePostgres;
import com.asetune.sql.ResultSetMetaDataCached;
import com.asetune.sql.ResultSetMetaDataCached.Entry;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.conn.info.DbmsVersionInfo;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;
import com.asetune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
// this can be of help: 
//    https://www.datadoghq.com/blog/postgresql-monitoring/
//    https://github.com/DataDog/the-monitor/blob/master/postgresql/postgresql-monitoring.md

public class CmPgRepSlots
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmPgRepSlots.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmPgRepSlots.class.getSimpleName();
	public static final String   SHORT_NAME       = "Rep Slots";
	public static final String   HTML_DESC        = 
		"<html>" +
		"One row per Replication destination, which has created any physical/logical replication slot.<br>" +
		"A replication slot ensures that the WAL isn't removed until all slots had received it's data." +
		"</html>";

	public static final String   GROUP_NAME       = MainFramePostgres.TCP_GROUP_REPLICATION;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = Ver.ver(9,4);
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"pg_replication_slots"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] 
	{
		"lag_kb_diff"
	};

//  Version 16.2
//	RS> Col# Label               JDBC Type Name         Guessed DBMS type Source Table        
//	RS> ---- ------------------- ---------------------- ----------------- --------------------
//	RS> 1    slot_name           java.sql.Types.VARCHAR text              pg_replication_slots,  since 9.4
//	RS> 2    plugin              java.sql.Types.VARCHAR text              pg_replication_slots,  since 9.4
//	RS> 3    slot_type           java.sql.Types.VARCHAR text              pg_replication_slots,  since 9.4
//	RS> 4    datoid              java.sql.Types.BIGINT  oid               pg_replication_slots,  since 9.4
//	RS> 5    database            java.sql.Types.VARCHAR text              pg_replication_slots,  since 9.4
//	RS> 6    temporary           java.sql.Types.BIT     bool              pg_replication_slots,  since 10.0
//	RS> 7    active              java.sql.Types.BIT     bool              pg_replication_slots,  since 9.4
//	RS> 8    active_pid          java.sql.Types.INTEGER int4              pg_replication_slots,  since 9.5
//	RS> 9    xmin                java.sql.Types.OTHER   xid               pg_replication_slots,  since 9.4
//	RS> 10   catalog_xmin        java.sql.Types.OTHER   xid               pg_replication_slots,  since 9.4
//	RS> 11   restart_lsn         java.sql.Types.OTHER   pg_lsn            pg_replication_slots,  since 9.4
//	RS> 12   confirmed_flush_lsn java.sql.Types.OTHER   pg_lsn            pg_replication_slots,  since 9.6
//	RS> 13   wal_status          java.sql.Types.VARCHAR text              pg_replication_slots,  since 13.0
//	RS> 14   safe_wal_size       java.sql.Types.BIGINT  int8              pg_replication_slots,  since 13.0
//	RS> 15   two_phase           java.sql.Types.BIT     bool              pg_replication_slots,  since 14.0
//	RS> 16   conflicting         java.sql.Types.BIT     bool              pg_replication_slots,  since 16.0
	
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

		return new CmPgRepSlots(counterController, guiController);
	}

	public CmPgRepSlots(ICounterController counterController, IGuiController guiController)
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
//	private static final String PROP_PREFIX                         = CM_NAME;
                                                                    
	public static final String GRAPH_NAME_LAG_KB         = "LagInKb";
	
//	@Override
//	protected TabularCntrPanel createGui()
//	{
//		return new CmPgRepSlotsPanel(this);
//	}
	
	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("slot_name");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		String pg_wal_lsn_diff           = "pg_xlog_location_diff";
		String pg_current_wal_insert_lsn = "pg_current_xlog_insert_location";
		if (versionInfo.getLongVersion() > Ver.ver(10))
		{
			pg_wal_lsn_diff           = "pg_wal_lsn_diff";
			pg_current_wal_insert_lsn = "pg_current_wal_insert_lsn";
		}
		
		String sql = ""
			    + "SELECT \n"
			    + "     CAST(" + pg_wal_lsn_diff + "(" + pg_current_wal_insert_lsn+ "(), restart_lsn)/1024.0        as numeric(12,1)) AS lag_kb \n"
			    + "    ,CAST(" + pg_wal_lsn_diff + "(" + pg_current_wal_insert_lsn+ "(), restart_lsn)/1024.0        as numeric(12,1)) AS lag_kb_diff \n"
			    + "    ,CAST(" + pg_wal_lsn_diff + "(" + pg_current_wal_insert_lsn+ "(), restart_lsn)/1024.0/1024.0 as numeric(12,1)) AS lag_mb \n"
			    + "    ,* \n"
			    + "FROM pg_replication_slots; \n"
			    + "";

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

			//  'slot_type'           -- from: CLOB        --->>> varchar(30)
			//  'wal_status'          -- from: CLOB        --->>> varchar(30)
			//  'restart_lsn'         -- from: JAVA_OBJECT --->>> varchar(30)
			//  'confirmed_flush_lsn' -- from: JAVA_OBJECT --->>> varchar(30)
			if (StringUtil.equalsAny(colName, "slot_type", "wal_status", "restart_lsn", "confirmed_flush_lsn"))
			{
				entry.setColumnType(Types.VARCHAR);
				entry.setPrecision(30);
				entry.setColumnDisplaySize(30);
			}
		}

		return rsmdc;
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try 
		{
			String tabname = "pg_replication_slots";
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			
			mtd.addTable(tabname, "The pg_replication_slots view provides a listing of all replication slots that currently exist on the database cluster, along with their current state.");

			mtd.addColumn(tabname,  "lag_kb",               "<html>How many KB has not yet been transferred to the destination.<br>Algorithm: pg_wal_lsn_diff(pg_current_wal_insert_lsn(), restart_lsn)/1024</html>");
			mtd.addColumn(tabname,  "lag_kb_diff",          "<html>Diff value since last sample, for Column 'lag_kb'</html>");
			mtd.addColumn(tabname,  "lag_mb",               "<html>How many KB has not yet been transferred to the destination.<br>Algorithm: pg_wal_lsn_diff(pg_current_wal_insert_lsn(), restart_lsn)/1024/1024</html>");
			
			mtd.addColumn(tabname,  "slot_name",            "<html>A unique, cluster-wide identifier for the replication slot</html>");
			mtd.addColumn(tabname,  "plugin",               "<html>The base name of the shared object containing the output plugin this logical slot is using, or null for physical slots.</html>");
			mtd.addColumn(tabname,  "slot_type",            "<html>The slot type: physical or logical</html>");
			mtd.addColumn(tabname,  "datoid",               "<html>The OID of the database this slot is associated with, or null. Only logical slots have an associated database.</html>");
			mtd.addColumn(tabname,  "database",             "<html>The name of the database this slot is associated with, or null. Only logical slots have an associated database.</html>");
			mtd.addColumn(tabname,  "temporary",            "<html>True if this is a temporary replication slot. Temporary slots are not saved to disk and are automatically dropped on error or when the session has finished.</html>");
			mtd.addColumn(tabname,  "active",               "<html>True if this slot is currently actively being used</html>");
			mtd.addColumn(tabname,  "active_pid",           "<html>The process ID of the session using this slot if the slot is currently actively being used. NULL if inactive.</html>");
			mtd.addColumn(tabname,  "xmin",                 "<html>The oldest transaction that this slot needs the database to retain. VACUUM cannot remove tuples deleted by any later transaction.</html>");
			mtd.addColumn(tabname,  "catalog_xmin",         "<html>The oldest transaction affecting the system catalogs that this slot needs the database to retain. VACUUM cannot remove catalog tuples deleted by any later transaction.</html>");
			mtd.addColumn(tabname,  "restart_lsn",          "<html>The address (LSN) of oldest WAL which still might be required by the consumer of this slot and thus won't be automatically removed during checkpoints unless this LSN gets behind more than max_slot_wal_keep_size from the current LSN. NULL if the LSN of this slot has never been reserved.</html>");
			mtd.addColumn(tabname,  "confirmed_flush_lsn",  "<html>The address (LSN) up to which the logical slot's consumer has confirmed receiving data. Data corresponding to the transactions committed before this LSN is not available anymore. NULL for physical slots.</html>");
			mtd.addColumn(tabname,  "wal_status",           "<html>Availability of WAL files claimed by this slot. Possible values are:"
			                                                    + "<ul>"
			                                                    + "   <li>reserved means that the claimed files are within max_wal_size.</li>"
			                                                    + "   <li>extended means that max_wal_size is exceeded but the files are still retained, either by the replication slot or by wal_keep_size.</li>"
			                                                    + "   <li>unreserved means that the slot no longer retains the required WAL files and some of them are to be removed at the next checkpoint. This state can return to reserved or extended.</li>"
			                                                    + "   <li>lost means that some required WAL files have been removed and this slot is no longer usable.</li>"
			                                                    + "</ul>"
			                                                    + "The last two states are seen only when max_slot_wal_keep_size is non-negative. If restart_lsn is NULL, this field is null."
			                                                    + "</html>");
			mtd.addColumn(tabname,  "safe_wal_size",        "<html>The number of bytes that can be written to WAL such that this slot is not in danger of getting in state \"lost\". It is NULL for lost slots, as well as if max_slot_wal_keep_size is -1.</html>");
			mtd.addColumn(tabname,  "two_phase",            "<html>True if the slot is enabled for decoding prepared transactions. Always false for physical slots.</html>");
			mtd.addColumn(tabname,  "conflicting",          "<html>True if this logical slot conflicted with recovery (and so is now invalidated). Always NULL for physical slots.</html>");
		}
		catch (NameNotFoundException e) 
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
		//	System.out.println("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
		}
	}
	

	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_LAG_KB,
			"Replication Lag in KB", 	              // Menu CheckBox text
			"Replication Lag in KB ("+SHORT_NAME+")", // Graph Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_KB, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, LabelType.Dynamic, 
			TrendGraphDataPoint.Category.REPLICATION,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

	}


	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		if (GRAPH_NAME_LAG_KB.equals(tgdp.getName()))
		{
			// Write 1 "line" for every slot...
			// Basebackup also creates a temporary replication slot with different names all the times, so normalize this into "pg_basebackup_###"
			Map<String, Double> map = new LinkedHashMap<>();
			for (int i = 0; i < this.size(); i++)
			{
				String slotName = this.getAbsString       (i, "slot_name");
				Double logKb    = this.getAbsValueAsDouble(i, "lag_kb");

				// Skip some slots
				if (slotName == null)
					continue;

				// Rename some slots
				if (slotName.startsWith("pg_basebackup_"))
					slotName = "pg_basebackup_###";

				map.put(slotName, logKb);
			}
			
			// Set the values
			if ( ! map.isEmpty() )
				tgdp.setData(this.getTimestamp(), map);
		}
	}

	@Override
	public void sendAlarmRequest()
	{
		if ( ! hasAbsData() )
			return;
		
		if ( ! AlarmHandler.hasInstance() )
			return;

		boolean debugPrint = Configuration.getCombinedConfiguration().getBooleanProperty("sendAlarmRequest.debug", _logger.isDebugEnabled());

		CountersModel cm = this;

		//-------------------------------------------------------
		// total_lag_kb
		//-------------------------------------------------------
		if (isSystemAlarmsForColumnEnabledAndInTimeRange("total_lag_kb"))
		{
			for (int r=0; r<cm.getAbsRowCount(); r++)
			{
				String slot_name  = cm.getAbsString   (r, "slot_name");
				Double lag_kb = cm.getAbsValueAsDouble(r, "lag_kb");
				
				if (lag_kb != null)
				{
					int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_LagKb, DEFAULT_alarm_LagKb);

					if (debugPrint || _logger.isDebugEnabled())
						System.out.println("##### sendAlarmRequest(" + cm.getName() + "): threshold=" + threshold + ", lag_kb='" + lag_kb + "'.");

					if (lag_kb.intValue() > threshold)
					{
						String extendedDescText = "";
						String extendedDescHtml = cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_LAG_KB);

						AlarmEvent ae = new AlarmEventPgReplicationLag(cm, slot_name, lag_kb.intValue(), threshold); 
						ae.setExtendedDescription(extendedDescText, extendedDescHtml);

						AlarmHandler.getInstance().addAlarm(ae);
					}
				}
			}
		}

	} // end: method

	@Override
	public boolean isGraphDataHistoryEnabled(String name)
	{
		// ENABLED for the following graphs
		if (GRAPH_NAME_LAG_KB.equals(name)) return true;

		// default: DISABLED
		return false;
	}
	@Override
	public int getGraphDataHistoryTimeInterval(String name)
	{
		// Keep interval: default is 60 minutes
		return super.getGraphDataHistoryTimeInterval(name);
	}

	
	public static final String  PROPKEY_alarm_LagKb              = CM_NAME + ".alarm.system.if.lag_kb.gt";
	public static final int     DEFAULT_alarm_LagKb              = 1024 * 100; // 100 MB

	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		
		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;
		
		list.add(new CmSettingsHelper("total_lag_kb"      , isAlarmSwitch, PROPKEY_alarm_LagKb        , Integer.class, conf.getIntProperty(PROPKEY_alarm_LagKb        , DEFAULT_alarm_LagKb)        , DEFAULT_alarm_LagKb        , "If 'lag_kb' is greater than this value, send 'AlarmEventPgReplicationLag'." ));

		return list;
	}
}
