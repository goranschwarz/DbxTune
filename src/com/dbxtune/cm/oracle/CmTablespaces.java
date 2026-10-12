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
package com.dbxtune.cm.oracle;

import java.lang.invoke.MethodHandles;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.naming.NameNotFoundException;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.alarm.AlarmHandler;
import com.dbxtune.alarm.events.AlarmDescriptionProviderBase;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.alarm.events.oracle.AlarmEventOraTablespaceLowFreeSpace;
import com.dbxtune.central.pcs.CentralPersistReader;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CmSettingsHelper.MapNumberValidator;
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
import com.dbxtune.sql.conn.info.DbmsVersionInfoOracle;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmTablespaces
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmTablespaces.class.getSimpleName();
	public static final String   SHORT_NAME       = "Tablespaces";
	public static final String   HTML_DESC        =
		"<html>" +
		"One row per tablespace: how big it is, how big it can get, and how much of that is used.<br>" +
		"<br>" +
		"<b>MAX_MB</b> is the size the tablespace can grow to (data files that are AUTOEXTENSIBLE grows when they get full).<br>" +
		"<b>USED_PCT</b> is how much of MAX_MB that is used. <i>Note: the files can only grow if the disk has room for it, see FILE_COUNT/AUTOEXT_FILE_COUNT/ALLOC_MB.</i><br>" +
		"<br>" +
		"TEMPORARY and UNDO tablespaces are normally <i>full</i> (the space is reused when it is needed), so they are by default not checked by the alarms.<br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_DISK;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {CM_NAME, "DBA_TABLESPACES", "DBA_TABLESPACE_USAGE_METRICS"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 300;
	public static final int      DEFAULT_QUERY_TIMEOUT          = 30;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.SMALL; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '" + CM_NAME + "'");

		return new CmTablespaces(counterController, guiController);
	}

	public CmTablespaces(ICounterController counterController, IGuiController guiController)
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
	public static final String GRAPH_NAME_USED_PCT = "TsUsedPct";
	public static final String GRAPH_NAME_USED_MB  = "TsUsedMb";
	public static final String GRAPH_NAME_FREE_MB  = "TsFreeMb";    // used by the DbxCentral Space Forecast Report (SpaceForecastServlet)
	public static final String GRAPH_NAME_MAX_MB   = "TsMaxMb";     // Not that important... but for the DbxCentral Space Forecast Report

	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_USED_PCT,
			"Tablespace Used in Percent", 	                // Menu CheckBox text
			"Tablespace Used in Percent of Max Size (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERCENT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null,
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.SPACE,
			true,  // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_USED_MB,
			"Tablespace Used Space in MB", 	                // Menu CheckBox text
			"Tablespace Used Space in MB (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null,
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.SPACE,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_FREE_MB,
			"Tablespace Free Space in MB", 	                // Menu CheckBox text
			"Tablespace Free Space in MB, up to Max Size (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB, CentralPersistReader.SampleType.MIN_OVER_SAMPLES),
			null,
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.SPACE,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_MAX_MB,
			"Tablespace Max Size in MB", 	                // Menu CheckBox text
			"Tablespace Max Size in MB: how big it can grow (" + SHORT_NAME + ")", // Graph Label
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

		// CON_ID is always 0 in a non-CDB (and in 11.2), in a CDB the same tablespace name exists in many containers
		pkCols.add("CON_ID");
		pkCols.add("TABLESPACE_NAME");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		DbmsVersionInfoOracle oraVersionInfo = (DbmsVersionInfoOracle) versionInfo;

		// Note: USED_SPACE and TABLESPACE_SIZE is in BLOCKS
		//       CAST(... as NUMBER(#,1)) is needed to keep decimals (a NUMBER without precision is treated as a BIGINT)
		String cols = ""
			+ "    ,t.TABLESPACE_NAME \n"
			+ "    ,t.CONTENTS \n"
			+ "    ,t.STATUS \n"
			+ "    ,t.BIGFILE \n"
			+ "    ,t.BLOCK_SIZE \n"
			+ "    ,f.FILE_COUNT \n"
			+ "    ,f.AUTOEXT_FILE_COUNT \n"
			+ "    ,CAST(f.ALLOC_BYTES / 1048576                                          as NUMBER(18,1)) as ALLOC_MB \n"
			+ "    ,CAST(m.TABLESPACE_SIZE                  * t.BLOCK_SIZE / 1048576      as NUMBER(18,1)) as MAX_MB \n"
			+ "    ,CAST(m.USED_SPACE                       * t.BLOCK_SIZE / 1048576      as NUMBER(18,1)) as USED_MB \n"
			+ "    ,CAST((m.TABLESPACE_SIZE - m.USED_SPACE) * t.BLOCK_SIZE / 1048576      as NUMBER(18,1)) as FREE_MB \n"
			+ "    ,CAST(m.USED_PERCENT                                                   as NUMBER(5,1))  as USED_PCT \n"
			+ "";

		// 11.2 and above: a non-CDB, or when we are connected to a PDB
		String sql = ""
			+ "select \n"
			+ "     CAST(0    as NUMBER(10))    as CON_ID \n"
			+ "    ,CAST(NULL as VARCHAR2(128)) as CON_NAME \n"
			+ cols
			+ "from DBA_TABLESPACES t \n"
			+ "left join DBA_TABLESPACE_USAGE_METRICS m on m.TABLESPACE_NAME = t.TABLESPACE_NAME \n"
			+ "left join (select TABLESPACE_NAME, count(*) as FILE_COUNT, sum(decode(AUTOEXTENSIBLE, 'YES', 1, 0)) as AUTOEXT_FILE_COUNT, sum(BYTES) as ALLOC_BYTES \n"
			+ "           from (select TABLESPACE_NAME, AUTOEXTENSIBLE, BYTES from DBA_DATA_FILES \n"
			+ "                 union all \n"
			+ "                 select TABLESPACE_NAME, AUTOEXTENSIBLE, BYTES from DBA_TEMP_FILES) \n"
			+ "           group by TABLESPACE_NAME) f on f.TABLESPACE_NAME = t.TABLESPACE_NAME \n"
			+ "order by t.TABLESPACE_NAME \n"
			+ "";

		// 12.1 and above, connected to CDB$ROOT: one row per tablespace in EVERY container
		if (versionInfo.getLongVersion() >= Ver.ver(12,1) && oraVersionInfo.isCdbRoot())
		{
			String sqlCdb = ""
				+ "select \n"
				+ "     t.CON_ID \n"
				+ "    ,c.NAME as CON_NAME \n"
				+ cols
				+ "from CDB_TABLESPACES t \n"
				+ "left join V$CONTAINERS c                   on c.CON_ID = t.CON_ID \n"
				+ "left join CDB_TABLESPACE_USAGE_METRICS m   on m.CON_ID = t.CON_ID and m.TABLESPACE_NAME = t.TABLESPACE_NAME \n"
				+ "left join (select CON_ID, TABLESPACE_NAME, count(*) as FILE_COUNT, sum(decode(AUTOEXTENSIBLE, 'YES', 1, 0)) as AUTOEXT_FILE_COUNT, sum(BYTES) as ALLOC_BYTES \n"
				+ "           from (select CON_ID, TABLESPACE_NAME, AUTOEXTENSIBLE, BYTES from CDB_DATA_FILES \n"
				+ "                 union all \n"
				+ "                 select CON_ID, TABLESPACE_NAME, AUTOEXTENSIBLE, BYTES from CDB_TEMP_FILES) \n"
				+ "           group by CON_ID, TABLESPACE_NAME) f on f.CON_ID = t.CON_ID and f.TABLESPACE_NAME = t.TABLESPACE_NAME \n"
				+ "order by t.CON_ID, t.TABLESPACE_NAME \n"
				+ "";

			// The SQL for 12.1 and above can NOT be tested on all versions: so "dry-run" it, and if it fails use the 11.2 SQL (which works on all versions)
			// conn is null when we just want to "view" the SQL (the CM Properties dialog)
			if (conn == null)
			{
				sql = sqlCdb;
			}
			else
			{
				try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery("select * from (" + sqlCdb + ") where 1=2"))
				{
					sql = sqlCdb;
				}
				catch (SQLException ex)
				{
					_logger.warn(CM_NAME + ": The SQL for a CDB (version " + Ver.versionNumToStr(versionInfo.getLongVersion()) + ") failed, only the tablespaces in CDB$ROOT will be shown. Caught: " + ex.getMessage().trim());
				}
			}
		}

		return sql;
	}

	/** If we can't read the views: disable the CM, and tell what to do about it */
	@Override
	public boolean checkDependsOnOther(DbxConnection conn)
	{
		return OracleCmUtils.checkViewsAreReadable(this, conn, "DBA_TABLESPACES", "DBA_TABLESPACE_USAGE_METRICS", "DBA_DATA_FILES", "DBA_TEMP_FILES");
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			mtd.addTable(CM_NAME, "One row per tablespace: how big it is, how big it can get, and how much of that is used.");

			mtd.addColumn(CM_NAME, "CON_ID",             "<html>Container ID.<br>0 = This is not a Container Database (or the version is below 12.1).<br>1 = CDB$ROOT, 2 = PDB$SEED, 3 and above = a Pluggable Database (PDB).<br><b>Note</b>: Only when connected to CDB$ROOT we can see tablespaces in other containers.</html>");
			mtd.addColumn(CM_NAME, "CON_NAME",           "<html>Name of the Container (PDB) the tablespace belongs to. NULL if this is not a Container Database.</html>");
			mtd.addColumn(CM_NAME, "FILE_COUNT",         "<html>Number of data files (or temp files) in the tablespace.</html>");
			mtd.addColumn(CM_NAME, "AUTOEXT_FILE_COUNT", "<html>Number of files that are AUTOEXTENSIBLE (they grow when they get full, up to it's MAXSIZE).<br>If this is 0, the tablespace can NOT grow by itself.</html>");
			mtd.addColumn(CM_NAME, "ALLOC_MB",           "<html>Size in MB that the files are right now (space allocated on disk).<br><b>Formula</b>: sum(BYTES) / 1024 / 1024 from DBA_DATA_FILES + DBA_TEMP_FILES</html>");
			mtd.addColumn(CM_NAME, "MAX_MB",             "<html>Size in MB the tablespace can grow to, when AUTOEXTENSIBLE files has reached it's MAXSIZE.<br><b>Note</b>: The files can only grow if the disk has room for it.<br><b>Formula</b>: TABLESPACE_SIZE * BLOCK_SIZE / 1024 / 1024</html>");
			mtd.addColumn(CM_NAME, "USED_MB",            "<html>Used space in MB.<br><b>Formula</b>: USED_SPACE * BLOCK_SIZE / 1024 / 1024</html>");
			mtd.addColumn(CM_NAME, "FREE_MB",            "<html>Free space in MB, up to MAX_MB.<br><b>Formula</b>: (TABLESPACE_SIZE - USED_SPACE) * BLOCK_SIZE / 1024 / 1024</html>");
			mtd.addColumn(CM_NAME, "USED_PCT",           "<html>Used space in percent of MAX_MB.<br><b>Formula</b>: USED_PERCENT from DBA_TABLESPACE_USAGE_METRICS</html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e);
		}
	}

	/** In a CDB: 'containerName:tablespaceName', otherwise just the tablespace name. Used as: graph label, and to identify an alarm */
	private static String getTablespaceLabel(CountersModel cm, int rowId)
	{
		String conName = cm.getAbsString(rowId, "CON_NAME");
		String tsName  = cm.getAbsString(rowId, "TABLESPACE_NAME");

		return StringUtil.hasValue(conName) ? conName + ":" + tsName : tsName;
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		// All the graphs is: one "line" for every tablespace
		String colName = null;
		if (GRAPH_NAME_USED_PCT.equals(tgdp.getName())) colName = "USED_PCT";
		if (GRAPH_NAME_USED_MB .equals(tgdp.getName())) colName = "USED_MB";
		if (GRAPH_NAME_FREE_MB .equals(tgdp.getName())) colName = "FREE_MB";
		if (GRAPH_NAME_MAX_MB  .equals(tgdp.getName())) colName = "MAX_MB";

		if (colName != null)
		{
			Double[] dArray = new Double[this.size()];
			String[] lArray = new String[dArray.length];
			for (int i = 0; i < dArray.length; i++)
			{
				lArray[i] = getTablespaceLabel(this, i);
				dArray[i] = this.getAbsValueAsDouble(i, colName);
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
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

		String skipContentsRegex = conf.getProperty(PROPKEY_alarm_SkipContentsRegex, DEFAULT_alarm_SkipContentsRegex);

		for (int r=0; r<cm.getAbsRowCount(); r++)
		{
			String tsName   = cm.getAbsString(r, "TABLESPACE_NAME");
			String tsLabel  = getTablespaceLabel(cm, r);
			String contents = cm.getAbsString(r, "CONTENTS");

			// TEMPORARY and UNDO are normally "full" (the space is reused when it's needed)
			if (StringUtil.hasValue(skipContentsRegex) && contents.matches(skipContentsRegex))
				continue;

			// No usage info (for example: an OFFLINE tablespace)
			Double usedPct = cm.getAbsValueAsDouble(r, "USED_PCT");
			Double freeMb  = cm.getAbsValueAsDouble(r, "FREE_MB");
			if (usedPct == null || freeMb == null)
				continue;

			//-------------------------------------------------------
			// UsedPct
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("UsedPct"))
			{
				// Get the threshold for this tablespace: the FIRST entry that matches is used (in the order they are written)
				// format: name=#, regexp=#   example: SYSAUX=99, PDB1:USERS=80, .*=95
				Number threshold = null;
				Map<String, String> map = StringUtil.parseCommaStrToMap(conf.getProperty(PROPKEY_alarm_UsedPct, DEFAULT_alarm_UsedPct));
				for (String key : map.keySet())
				{
					if (tsName.matches(key) || tsLabel.matches(key))
					{
						threshold = NumberUtils.createNumber(map.get(key));
						break;
					}
				}

				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest(" + cm.getName() + "): UsedPct: tablespace='" + tsLabel + "', threshold=" + threshold + ", usedPct='" + usedPct + "', freeMb='" + freeMb + "'.");

				if (threshold != null && usedPct > threshold.doubleValue())
				{
					AlarmEvent ae = new AlarmEventOraTablespaceLowFreeSpace(cm, tsLabel, freeMb.intValue(), usedPct, threshold.doubleValue());

					// The below is called from AlarmHandler on: Raise, RE-RAISE & CANCEL
					ae.setAlarmDescriptionProvider(this, new AlarmDescriptionProviderBase(cm.getPk(), cm.getAbsPkValue(r), threshold)
					{
						@Override
						public void setValues(CountersModel cm, AlarmEvent alarmEvent, AlarmPhase phase)
						{
							int    rowId = cm.getAbsRowIdForPkValue(getRowPk());
							String label = "Tablespace '" + tsLabel + "'";

							if (rowId != -1)
							{
								// Always: (RAISE, RE-RAISE, CANCEL) - Set graph values
								String extendedDescText = cm.toTextTableString(DATA_ABS, rowId);
								String extendedDescHtml = cm.toHtmlTableString(DATA_ABS, rowId, true, false, false);
								extendedDescHtml += "<br><br>" + cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_USED_PCT, tsLabel);
								alarmEvent.setExtendedDescription(extendedDescText, extendedDescHtml);

								// Set CANCEL message (values that was found AFTER last raise/re-raise event)
								if (AlarmPhase.CANCEL.equals(phase))
								{
									Double USED_PCT = cm.getAbsValueAsDouble(rowId, "USED_PCT");
									Double FREE_MB  = cm.getAbsValueAsDouble(rowId, "FREE_MB");

									String cancelMsg = getAlarmCancelText(label + ": used space", toAlarmCancelValue(USED_PCT) + "%, " + toAlarmCancelValue(FREE_MB) + " MB free", "%");
									alarmEvent.setCancelDescription(cancelMsg);
								}
							}
							else
							{
								if (AlarmPhase.CANCEL.equals(phase))
									alarmEvent.setCancelDescription(getAlarmCancelTextGone(label));
							}
						}
					});

					// Information about how to disable this alarm
					ae.createAlarmOptionsMessage(this, "UsedPct");

					AlarmHandler.getInstance().addAlarm(ae);
				}
			} // end: UsedPct

			//-------------------------------------------------------
			// FreeMb
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("FreeMb"))
			{
				// Get the threshold for this tablespace: the FIRST entry that matches is used (in the order they are written)
				// format: name=#, regexp=#   example: SYSAUX=100, PDB1:USERS=5000, .*=500
				Number threshold = null;
				Map<String, String> map = StringUtil.parseCommaStrToMap(conf.getProperty(PROPKEY_alarm_FreeMb, DEFAULT_alarm_FreeMb));
				for (String key : map.keySet())
				{
					if (tsName.matches(key) || tsLabel.matches(key))
					{
						threshold = NumberUtils.createNumber(map.get(key));
						break;
					}
				}

				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest(" + cm.getName() + "): FreeMb: tablespace='" + tsLabel + "', threshold=" + threshold + ", freeMb='" + freeMb + "', usedPct='" + usedPct + "'.");

				if (threshold != null && freeMb < threshold.doubleValue())
				{
					AlarmEvent ae = new AlarmEventOraTablespaceLowFreeSpace(cm, tsLabel, freeMb.intValue(), usedPct, threshold.intValue());

					// The below is called from AlarmHandler on: Raise, RE-RAISE & CANCEL
					ae.setAlarmDescriptionProvider(this, new AlarmDescriptionProviderBase(cm.getPk(), cm.getAbsPkValue(r), threshold)
					{
						@Override
						public void setValues(CountersModel cm, AlarmEvent alarmEvent, AlarmPhase phase)
						{
							int    rowId = cm.getAbsRowIdForPkValue(getRowPk());
							String label = "Tablespace '" + tsLabel + "'";

							if (rowId != -1)
							{
								// Always: (RAISE, RE-RAISE, CANCEL) - Set graph values
								String extendedDescText = cm.toTextTableString(DATA_ABS, rowId);
								String extendedDescHtml = cm.toHtmlTableString(DATA_ABS, rowId, true, false, false);
								extendedDescHtml += "<br><br>" + cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_FREE_MB, tsLabel);
								alarmEvent.setExtendedDescription(extendedDescText, extendedDescHtml);

								// Set CANCEL message (values that was found AFTER last raise/re-raise event)
								if (AlarmPhase.CANCEL.equals(phase))
								{
									Double USED_PCT = cm.getAbsValueAsDouble(rowId, "USED_PCT");
									Double FREE_MB  = cm.getAbsValueAsDouble(rowId, "FREE_MB");

									String cancelMsg = getAlarmCancelText(label + ": free space", toAlarmCancelValue(FREE_MB) + " MB, " + toAlarmCancelValue(USED_PCT) + "% used", " MB");
									alarmEvent.setCancelDescription(cancelMsg);
								}
							}
							else
							{
								if (AlarmPhase.CANCEL.equals(phase))
									alarmEvent.setCancelDescription(getAlarmCancelTextGone(label));
							}
						}
					});

					// Information about how to disable this alarm
					ae.createAlarmOptionsMessage(this, "FreeMb");

					AlarmHandler.getInstance().addAlarm(ae);
				}
			} // end: FreeMb

		} // end: loop-rows
	}

	@Override
	public boolean isGraphDataHistoryEnabled(String name)
	{
		// ENABLED for the following graphs
		if (GRAPH_NAME_USED_PCT.equals(name)) return true;
		if (GRAPH_NAME_FREE_MB .equals(name)) return true;

		// default: DISABLED
		return false;
	}

	public static final String  PROPKEY_alarm_SkipContentsRegex = CM_NAME + ".alarm.system.skip.contents";
	public static final String  DEFAULT_alarm_SkipContentsRegex = "(TEMPORARY|UNDO)";

	public static final String  PROPKEY_alarm_UsedPct           = CM_NAME + ".alarm.system.if.UsedPct.gt";
	public static final String  DEFAULT_alarm_UsedPct           = ".*=95";

	public static final String  PROPKEY_alarm_FreeMb            = CM_NAME + ".alarm.system.if.FreeMb.lt";
	public static final String  DEFAULT_alarm_FreeMb            = "";

	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;
		CmSettingsHelper.Type isPreCheck    = CmSettingsHelper.Type.IS_PRE_CHECK;

		list.add(new CmSettingsHelper("SkipContentsRegex", isPreCheck   , PROPKEY_alarm_SkipContentsRegex, String.class, conf.getProperty(PROPKEY_alarm_SkipContentsRegex, DEFAULT_alarm_SkipContentsRegex), DEFAULT_alarm_SkipContentsRegex, "Skip tablespaces where 'CONTENTS' matches this regexp (TEMPORARY and UNDO tablespaces are normally full, the space is reused when needed).", new RegExpInputValidator()));

		list.add(new CmSettingsHelper("UsedPct"          , isAlarmSwitch, PROPKEY_alarm_UsedPct          , String.class, conf.getProperty(PROPKEY_alarm_UsedPct          , DEFAULT_alarm_UsedPct          ), DEFAULT_alarm_UsedPct          , "If 'USED_PCT' is greater than ##.# Percent then send 'AlarmEventOraTablespaceLowFreeSpace'. format: tablespace1=#, tablespace2=#, .*=#  (Note: the 'tablespace' can use regexp, in a CDB also 'pdbName:tablespace', and the FIRST entry that matches is used)", new MapNumberValidator()));
		list.add(new CmSettingsHelper("FreeMb"           , isAlarmSwitch, PROPKEY_alarm_FreeMb           , String.class, conf.getProperty(PROPKEY_alarm_FreeMb           , DEFAULT_alarm_FreeMb           ), DEFAULT_alarm_FreeMb           , "If 'FREE_MB' is less than ## MB then send 'AlarmEventOraTablespaceLowFreeSpace'. format: tablespace1=#, tablespace2=#, .*=#  (Note: the 'tablespace' can use regexp, in a CDB also 'pdbName:tablespace', and the FIRST entry that matches is used)", new MapNumberValidator()));

		return list;
	}
}
