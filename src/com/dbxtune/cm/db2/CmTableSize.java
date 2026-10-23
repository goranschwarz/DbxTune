/*******************************************************************************
 * Copyright (C) 2010-2027 Goran Schwarz
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
package com.dbxtune.cm.db2;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.naming.NameNotFoundException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.alarm.AlarmHandler;
import com.dbxtune.alarm.events.AlarmDescriptionProviderBase;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.alarm.events.db2.AlarmEventDb2TableNotUsable;
import com.dbxtune.central.pcs.CentralPersistReader;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CmSettingsHelper.RegExpInputValidator;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.config.dict.MonTablesDictionary;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.graph.TrendGraphDataPoint;
import com.dbxtune.graph.TrendGraphDataPoint.LabelType;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;

/**
 * The biggest tables (ADMIN_GET_TAB_INFO, the physical size), with the statistics, when they were last used, and if they can be used:
 * a table in REORG PENDING, SET INTEGRITY PENDING, LOAD PENDING or not available can not be used (or not changed) until it is fixed.
 *
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmTableSize
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmTableSize.class.getSimpleName();
	public static final String   SHORT_NAME       = "Table Size";
	public static final String   HTML_DESC        =
		"<html>" +
		"<p>The biggest tables (the physical size from ADMIN_GET_TAB_INFO: data, indexes, LOBs/XML, column-organized), with the statistics, and when they were last used.</p>" +
		"<p>The tables that can not be used are always included (first): REORG PENDING (after some ALTER TABLE), SET INTEGRITY PENDING (after a LOAD or ALTER on a table with constraints), " +
		"LOAD PENDING / IN PROGRESS, or not available. See the column <b>PROBLEM</b>.</p>" +
		"<p>Only the 200 biggest tables (property '" + CM_NAME + ".sample.topRows'), once an hour (it reads the size of all tables).</p>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {CM_NAME, "ADMIN_GET_TAB_INFO", "TABLES"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = false;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 3600;
	public static final int      DEFAULT_QUERY_TIMEOUT          = 120;

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
			guiController.splashWindowProgress("Loading: Counter Model '" + CM_NAME + "'");

		return new CmTableSize(counterController, guiController);
	}

	public CmTableSize(ICounterController counterController, IGuiController guiController)
	{
		super(counterController, guiController,
				CM_NAME, GROUP_NAME, /*sql*/null, /*pkList*/null,
				DIFF_COLUMNS, PCT_COLUMNS, MON_TABLES,
				NEED_ROLES, NEED_CONFIG, NEED_SRV_VERSION, NEED_CE_VERSION,
				NEGATIVE_DIFF_COUNTERS_TO_ZERO, IS_SYSTEM_CM, DEFAULT_POSTPONE_TIME);

		setDisplayName(SHORT_NAME);
		setDescription(HTML_DESC);

		setIconFile(GUI_ICON_FILE);

		setShowClearTime(false);

		setCounterController(counterController);
		setGuiController(guiController);

		addTrendGraphs();

		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	public static final String  PROPKEY_sample_topRows = CM_NAME + ".sample.topRows";
	public static final int     DEFAULT_sample_topRows = 200;

	public static final String  GRAPH_NAME_SCHEMA_MB   = "SchemaSizeMb";

	@Override
	protected void registerDefaultValues()
	{
		super.registerDefaultValues();

		Configuration.registerDefaultValue(PROPKEY_sample_topRows, DEFAULT_sample_topRows);
	}

	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public List<CmSettingsHelper> getLocalSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		list.add(new CmSettingsHelper("Max Number of Rows", PROPKEY_sample_topRows, Integer.class, conf.getIntProperty(PROPKEY_sample_topRows, DEFAULT_sample_topRows), DEFAULT_sample_topRows, "Only get the ## biggest tables (the tables with a problem are always included). 0 = all." ));

		return list;
	}

	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_SCHEMA_MB,
			"Table Size per Schema, MB",                                     // Menu CheckBox text
			"Table Size per Schema in MB, of the Biggest Tables (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null,
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.SPACE,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("TABSCHEMA");
		pkCols.add("TABNAME");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		int topRows = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_sample_topRows, DEFAULT_sample_topRows);

		// NOTE: The sizes are in KB. A partitioned table has one row per data partition (and DPF one per database partition): summed per table
		// NOTE: SYSCAT.TABLES.STATUS: N = normal, C = set integrity pending, X = inoperative
		String sql = ""
			+ "with i as ( \n"
			+ "    select TABSCHEMA, TABNAME \n"
			+ "          ,sum(DATA_OBJECT_P_SIZE)                                          as DATA_KB \n"
			+ "          ,sum(INDEX_OBJECT_P_SIZE)                                         as INDEX_KB \n"
			+ "          ,sum(LONG_OBJECT_P_SIZE + LOB_OBJECT_P_SIZE + XML_OBJECT_P_SIZE)  as LOB_KB \n"
			+ "          ,sum(COALESCE(COL_OBJECT_P_SIZE, 0))                              as COL_KB \n"
			+ "          ,sum(COALESCE(RECLAIMABLE_SPACE, 0))                              as RECLAIMABLE_KB \n"
			+ "          ,sum(COALESCE(STATS_ROWS_MODIFIED, 0))                            as STATS_ROWS_MODIFIED \n"
			+ "          ,max(CASE WHEN AVAILABLE = 'N' THEN 'NOT AVAILABLE' \n"
			+ "                    WHEN REORG_PENDING = 'Y' THEN 'REORG PENDING' \n"
			+ "                    WHEN COALESCE(LOAD_STATUS, '') <> '' THEN 'LOAD ' || LOAD_STATUS \n"
			+ "                    WHEN INDEXES_REQUIRE_REBUILD = 'Y' THEN 'INDEXES REQUIRE REBUILD' \n"
			+ "                    ELSE '' END)                                            as INFO_PROBLEM \n"
			+ "    from table(SYSPROC.ADMIN_GET_TAB_INFO(NULL, NULL)) x \n"
			+ "    where TABTYPE = 'T' \n"
			+ "    group by TABSCHEMA, TABNAME \n"
			+ ") \n"
			+ "select /* ${cmCollectorName} */ \n"
			+ "     RTRIM(i.TABSCHEMA)                                                    as TABSCHEMA \n"
			+ "    ,i.TABNAME \n"
			+ "    ,CAST(CASE WHEN t.STATUS = 'C' THEN 'SET INTEGRITY PENDING' \n"
			+ "               WHEN t.STATUS = 'X' THEN 'INOPERATIVE' \n"
			+ "               ELSE i.INFO_PROBLEM END AS VARCHAR(30))                     as PROBLEM \n"
			+ "    ,CAST((i.DATA_KB + i.INDEX_KB + i.LOB_KB + i.COL_KB) / 1024.0 AS DECIMAL(15,1)) as TOTAL_MB \n"
			+ "    ,CAST(i.DATA_KB  / 1024.0 AS DECIMAL(15,1))                            as DATA_MB \n"
			+ "    ,CAST(i.INDEX_KB / 1024.0 AS DECIMAL(15,1))                            as INDEX_MB \n"
			+ "    ,CAST(i.LOB_KB   / 1024.0 AS DECIMAL(15,1))                            as LOB_MB \n"
			+ "    ,CAST(i.COL_KB   / 1024.0 AS DECIMAL(15,1))                            as COL_MB \n"
			+ "    ,CAST(i.RECLAIMABLE_KB / 1024.0 AS DECIMAL(15,1))                      as RECLAIMABLE_MB \n"
			+ "    ,t.CARD                                                                as ROW_COUNT_STATS \n"
			+ "    ,t.STATS_TIME \n"
			+ "    ,i.STATS_ROWS_MODIFIED \n"
			+ "    ,t.LASTUSED \n"
			+ "    ,t.TABLEORG \n"
			+ "    ,t.TBSPACE \n"
			+ "    ,t.COMPRESSION \n"
			+ "    ,t.CREATE_TIME \n"
			+ "from i \n"
			+ "left join SYSCAT.TABLES t on t.TABSCHEMA = i.TABSCHEMA and t.TABNAME = i.TABNAME \n"
			+ "order by CASE WHEN t.STATUS in ('C', 'X') OR i.INFO_PROBLEM <> '' THEN 0 ELSE 1 END, i.DATA_KB + i.INDEX_KB + i.LOB_KB + i.COL_KB desc \n"
			+ (topRows > 0 ? "fetch first " + topRows + " rows only \n" : "")
			+ "";

		return sql;
	}

	@Override
	public boolean checkDependsOnOther(DbxConnection conn)
	{
		return Db2CmUtils.checkFunctionsAreExecutable(this, conn, "SYSPROC.ADMIN_GET_TAB_INFO(NULL, NULL)");
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addTable(CM_NAME, "The biggest tables (ADMIN_GET_TAB_INFO), and the tables that can not be used.");

			mtd.addColumn(CM_NAME, "TABSCHEMA",           "<html>The schema.</html>");
			mtd.addColumn(CM_NAME, "TABNAME",             "<html>The table.</html>");
			mtd.addColumn(CM_NAME, "PROBLEM",             "<html>Empty if the table can be used. Otherwise:<ul>"
			                                            + "<li>REORG PENDING - after an ALTER TABLE that needs a REORG (the table can only be read, or not used at all)</li>"
			                                            + "<li>SET INTEGRITY PENDING - after a LOAD or ALTER on a table with constraints (it can not be used until SET INTEGRITY is run)</li>"
			                                            + "<li>LOAD PENDING / LOAD IN_PROGRESS - a LOAD failed or runs</li>"
			                                            + "<li>NOT AVAILABLE - for example after a not logged operation that failed, or a roll forward over a NOT LOGGED INITIALLY</li>"
			                                            + "<li>INDEXES REQUIRE REBUILD, INOPERATIVE</li>"
			                                            + "</ul><b>Alarm</b>: TableNotUsable</html>");
			mtd.addColumn(CM_NAME, "TOTAL_MB",            "<html>The physical size of the table: data + indexes + LOB/XML + column-organized, in MB.</html>");
			mtd.addColumn(CM_NAME, "DATA_MB",             "<html>The data, in MB.</html>");
			mtd.addColumn(CM_NAME, "INDEX_MB",            "<html>The indexes, in MB.</html>");
			mtd.addColumn(CM_NAME, "LOB_MB",              "<html>LOB, LONG and XML data, in MB.</html>");
			mtd.addColumn(CM_NAME, "COL_MB",              "<html>Column-organized data (BLU), in MB.</html>");
			mtd.addColumn(CM_NAME, "RECLAIMABLE_MB",      "<html>Space that can be given back to the table space (with REORG ... RECLAIM EXTENTS), in MB.</html>");
			mtd.addColumn(CM_NAME, "ROW_COUNT_STATS",     "<html>The number of rows, from the statistics (SYSCAT.TABLES.CARD). -1 = no statistics.</html>");
			mtd.addColumn(CM_NAME, "STATS_TIME",          "<html>When the statistics were collected (RUNSTATS). Empty = never.</html>");
			mtd.addColumn(CM_NAME, "STATS_ROWS_MODIFIED", "<html>Rows changed since the statistics were collected.</html>");
			mtd.addColumn(CM_NAME, "LASTUSED",            "<html>The date the table was last used (updated by Db2 at most once a day).</html>");
			mtd.addColumn(CM_NAME, "TABLEORG",            "<html>R = row-organized, C = column-organized (BLU).</html>");
			mtd.addColumn(CM_NAME, "TBSPACE",             "<html>The table space of the data.</html>");
			mtd.addColumn(CM_NAME, "COMPRESSION",         "<html>N = none, R = row compression, V = value compression, B = both.</html>");
			mtd.addColumn(CM_NAME, "CREATE_TIME",         "<html>When the table was created.</html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e);
		}
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		if (GRAPH_NAME_SCHEMA_MB.equals(tgdp.getName()))
		{
			// Summed per schema (sorted by name, so the lines keep their order)
			Map<String, Double> perSchema = new TreeMap<>();
			for (int r = 0; r < this.getAbsRowCount(); r++)
			{
				Double val = this.getAbsValueAsDouble(r, "TOTAL_MB");
				perSchema.merge(this.getAbsString(r, "TABSCHEMA"), val == null ? 0d : val, Double::sum);
			}
			if ( ! perSchema.isEmpty() )
				tgdp.setData(this.getTimestamp(), new java.util.LinkedHashMap<>(perSchema));
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

		CountersModel cm   = this;
		Configuration conf = Configuration.getCombinedConfiguration();

		//-------------------------------------------------------
		// TableNotUsable: REORG PENDING, SET INTEGRITY PENDING, LOAD PENDING...
		//-------------------------------------------------------
		if (isSystemAlarmsForColumnEnabledAndInTimeRange("TableNotUsable") && conf.getBooleanProperty(PROPKEY_alarm_TableNotUsable, DEFAULT_alarm_TableNotUsable))
		{
			String skipRegex = conf.getProperty(PROPKEY_alarm_TableNotUsableSkipRegex, DEFAULT_alarm_TableNotUsableSkipRegex);

			for (int r = 0; r < cm.getAbsRowCount(); r++)
			{
				final String schema  = cm.getAbsString(r, "TABSCHEMA");
				final String table   = cm.getAbsString(r, "TABNAME");
				String       problem = cm.getAbsString(r, "PROBLEM");

				if (StringUtil.isNullOrBlank(problem))
					continue;

				boolean skip = StringUtil.hasValue(skipRegex) && (schema + "." + table).matches(skipRegex);

				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest(" + cm.getName() + "): TableNotUsable: table='" + schema + "." + table + "', PROBLEM='" + problem + "', skip=" + skip + ".");

				if (skip)
					continue;

				AlarmEvent ae = new AlarmEventDb2TableNotUsable(cm, schema, table, problem);

				final String raiseText = cm.toTextTableString(DATA_ABS, r);
				final String raiseHtml = cm.toHtmlTableString(DATA_ABS, r, true, false, false);
				ae.setAlarmDescriptionProvider(this, new AlarmDescriptionProviderBase(null, null, null)
				{
					@Override
					public void setValues(CountersModel cm, AlarmEvent alarmEvent, AlarmPhase phase)
					{
						alarmEvent.setExtendedDescription(raiseText, raiseHtml);
						if (AlarmPhase.CANCEL.equals(phase))
							alarmEvent.setCancelDescription("The table " + schema + "." + table + " can be used again");
					}
				});

				ae.createAlarmOptionsMessage(this, "TableNotUsable");

				AlarmHandler.getInstance().addAlarm(ae);
			}
		}
	}

	public static final String  PROPKEY_alarm_TableNotUsable          = CM_NAME + ".alarm.system.on.TableNotUsable";
	public static final boolean DEFAULT_alarm_TableNotUsable          = true;

	public static final String  PROPKEY_alarm_TableNotUsableSkipRegex = CM_NAME + ".alarm.system.on.TableNotUsable.skip.regex";
	public static final String  DEFAULT_alarm_TableNotUsableSkipRegex = "";

	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;

		list.add(new CmSettingsHelper("TableNotUsable", isAlarmSwitch, PROPKEY_alarm_TableNotUsable, Boolean.class, conf.getBooleanProperty(PROPKEY_alarm_TableNotUsable, DEFAULT_alarm_TableNotUsable), DEFAULT_alarm_TableNotUsable, "If a table has a PROBLEM (REORG PENDING, SET INTEGRITY PENDING, LOAD PENDING, NOT AVAILABLE...) then send 'AlarmEventDb2TableNotUsable'." ));
		list.add(new CmSettingsHelper("TableNotUsable Skip Regex", PROPKEY_alarm_TableNotUsableSkipRegex, String.class, conf.getProperty(PROPKEY_alarm_TableNotUsableSkipRegex, DEFAULT_alarm_TableNotUsableSkipRegex), DEFAULT_alarm_TableNotUsableSkipRegex, "A table with a PROBLEM (REORG PENDING, SET INTEGRITY PENDING, LOAD PENDING...) sends 'AlarmEventDb2TableNotUsable'. Skip the tables where 'schema.table' matches this regexp. Empty = skip nothing.", new RegExpInputValidator()));

		return list;
	}
}
