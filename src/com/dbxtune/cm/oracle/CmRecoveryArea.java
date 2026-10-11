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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.alarm.AlarmHandler;
import com.dbxtune.alarm.events.AlarmDescriptionProviderBase;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.alarm.events.oracle.AlarmEventOraRecoveryAreaLowFreeSpace;
import com.dbxtune.central.pcs.CentralPersistReader;
import com.dbxtune.cm.CmSettingsHelper;
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

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmRecoveryArea
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmRecoveryArea.class.getSimpleName();
	public static final String   SHORT_NAME       = "Recovery Area";
	public static final String   HTML_DESC        =
		"<html>" +
		"How full the Fast Recovery Area (FRA) is, and what type of files that uses the space.<br>" +
		"<br>" +
		"The FRA holds: archived logs, backups, flashback logs (and sometimes: online redo logs and control files).<br>" +
		"<b>When it is full, the archiver can't write archived logs, and the database 'hangs'</b> (ORA-19809, ORA-00257) until space is freed.<br>" +
		"<br>" +
		"Some of the used space is <i>reclaimable</i>: files that are obsolete (or already backed up), which Oracle deletes by itself when it needs the space.<br>" +
		"So the number to look at is <code>NET_USED_PCT</code> (used - reclaimable). Oracle itself writes a warning in the alert log at 85% and 97%.<br>" +
		"<br>" +
		"<b>Note</b>: No rows means that no recovery area is configured (<code>db_recovery_file_dest</code> is not set).<br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_DISK;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {CM_NAME, "V$RECOVERY_FILE_DEST", "V$RECOVERY_AREA_USAGE"};
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

		return new CmRecoveryArea(counterController, guiController);
	}

	public CmRecoveryArea(ICounterController counterController, IGuiController guiController)
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
	public static final String GRAPH_NAME_USED_PCT         = "FraUsedPct";
	public static final String GRAPH_NAME_USED_MB          = "FraUsedMb";
	public static final String GRAPH_NAME_USED_PCT_BY_TYPE = "FraUsedPctByType";

	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_USED_PCT,
			"Recovery Area, Used in Percent", 	                // Menu CheckBox text
			"Recovery Area (FRA), Used Space in Percent (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERCENT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] {"Net Used % (used - reclaimable)", "Used %", "Reclaimable %"},
			LabelType.Static,
			TrendGraphDataPoint.Category.SPACE,
			true,  // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_USED_MB,
			"Recovery Area, Used in MB", 	                // Menu CheckBox text
			"Recovery Area (FRA), Used Space in MB (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] {"Limit MB", "Used MB", "Reclaimable MB"},
			LabelType.Static,
			TrendGraphDataPoint.Category.SPACE,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_USED_PCT_BY_TYPE,
			"Recovery Area, Used in Percent by File Type", 	                // Menu CheckBox text
			"Recovery Area (FRA), Used Space in Percent by File Type (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERCENT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] {"Archived Logs %", "Backups %", "Flashback Logs %", "Other %"},
			LabelType.Static,
			TrendGraphDataPoint.Category.SPACE,
			true,  // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("NAME");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		// One row (or no rows if no recovery area is configured). This looks the same in all versions (and in a CDB the recovery area belongs to the whole CDB)
		// Note: CAST(... as NUMBER(#,1)) is needed to keep decimals (a NUMBER without precision is treated as a BIGINT)
		//       "else 0" in the sum(case...) is needed, otherwise we get: SQLWarning 'ORA-24347: Warning of a NULL column in an aggregate function'
		String sql = ""
			+ "select \n"
			+ "     r.NAME \n"
			+ "    ,CAST(r.SPACE_LIMIT                                        / 1048576 as NUMBER(18,1)) as LIMIT_MB \n"
			+ "    ,CAST(r.SPACE_USED                                         / 1048576 as NUMBER(18,1)) as USED_MB \n"
			+ "    ,CAST(r.SPACE_RECLAIMABLE                                  / 1048576 as NUMBER(18,1)) as RECLAIMABLE_MB \n"
			+ "    ,CAST((r.SPACE_LIMIT - r.SPACE_USED + r.SPACE_RECLAIMABLE) / 1048576 as NUMBER(18,1)) as NET_FREE_MB \n"
			+ "    ,r.NUMBER_OF_FILES \n"
			+ "    ,CAST(100 * (r.SPACE_USED - r.SPACE_RECLAIMABLE) / NULLIF(r.SPACE_LIMIT, 0) as NUMBER(5,1)) as NET_USED_PCT \n"
			+ "    ,CAST(100 * r.SPACE_USED                         / NULLIF(r.SPACE_LIMIT, 0) as NUMBER(5,1)) as USED_PCT \n"
			+ "    ,CAST(100 * r.SPACE_RECLAIMABLE                  / NULLIF(r.SPACE_LIMIT, 0) as NUMBER(5,1)) as RECLAIMABLE_PCT \n"
			+ "    ,u.ARCHIVED_LOG_PCT \n"
			+ "    ,u.BACKUP_PCT \n"
			+ "    ,u.FLASHBACK_LOG_PCT \n"
			+ "    ,u.OTHER_PCT \n"
			+ "from V$RECOVERY_FILE_DEST r \n"
			+ "cross join (select \n"
			+ "                 CAST(sum(case when FILE_TYPE = 'ARCHIVED LOG'                                                    then PERCENT_SPACE_USED else 0 end) as NUMBER(5,1)) as ARCHIVED_LOG_PCT \n"
			+ "                ,CAST(sum(case when FILE_TYPE in ('BACKUP PIECE', 'IMAGE COPY')                                   then PERCENT_SPACE_USED else 0 end) as NUMBER(5,1)) as BACKUP_PCT \n"
			+ "                ,CAST(sum(case when FILE_TYPE = 'FLASHBACK LOG'                                                   then PERCENT_SPACE_USED else 0 end) as NUMBER(5,1)) as FLASHBACK_LOG_PCT \n"
			+ "                ,CAST(sum(case when FILE_TYPE not in ('ARCHIVED LOG', 'BACKUP PIECE', 'IMAGE COPY', 'FLASHBACK LOG') then PERCENT_SPACE_USED else 0 end) as NUMBER(5,1)) as OTHER_PCT \n"
			+ "            from V$RECOVERY_AREA_USAGE) u \n"
			+ "where r.NAME is not null \n"
			+ "";

		return sql;
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			mtd.addTable(CM_NAME, "How full the Fast Recovery Area (FRA) is, and what type of files that uses the space.");

			mtd.addColumn(CM_NAME, "NAME",              "<html>Location of the recovery area (a directory or an ASM disk group). This is the parameter: <code>db_recovery_file_dest</code></html>");
			mtd.addColumn(CM_NAME, "LIMIT_MB",          "<html>How much space the database is allowed to use in the recovery area. This is the parameter: <code>db_recovery_file_dest_size</code><br><b>Note</b>: This is a quota, NOT the size of the disk. If the disk is smaller than this, the disk gets full first.</html>");
			mtd.addColumn(CM_NAME, "USED_MB",           "<html>Used space in MB (including space that can be reclaimed).</html>");
			mtd.addColumn(CM_NAME, "RECLAIMABLE_MB",    "<html>Space in MB used by files that are obsolete (or already backed up). Oracle deletes those files by itself when it needs the space.</html>");
			mtd.addColumn(CM_NAME, "NET_FREE_MB",       "<html>Free space in MB, including the space that can be reclaimed.<br><b>Formula</b>: LIMIT_MB - USED_MB + RECLAIMABLE_MB</html>");
			mtd.addColumn(CM_NAME, "NUMBER_OF_FILES",   "<html>Number of files in the recovery area.</html>");
			mtd.addColumn(CM_NAME, "NET_USED_PCT",      "<html>Used space in percent that can NOT be reclaimed. <b>This is the one to look at</b> (and what the alarm checks).<br><b>Formula</b>: 100 * (USED - RECLAIMABLE) / LIMIT</html>");
			mtd.addColumn(CM_NAME, "USED_PCT",          "<html>Used space in percent (including space that can be reclaimed).<br><b>Formula</b>: 100 * USED / LIMIT</html>");
			mtd.addColumn(CM_NAME, "RECLAIMABLE_PCT",   "<html>Space in percent that can be reclaimed.<br><b>Formula</b>: 100 * RECLAIMABLE / LIMIT</html>");
			mtd.addColumn(CM_NAME, "ARCHIVED_LOG_PCT",  "<html>Percent of the recovery area used by: archived logs.<br>If this is what fills the area: backup the archived logs (and delete them), or check that the backup job is running.</html>");
			mtd.addColumn(CM_NAME, "BACKUP_PCT",        "<html>Percent of the recovery area used by: RMAN backup pieces and image copies.<br>If this is what fills the area: check the RMAN retention policy (and that obsolete backups are deleted).</html>");
			mtd.addColumn(CM_NAME, "FLASHBACK_LOG_PCT", "<html>Percent of the recovery area used by: flashback logs.<br>If this is what fills the area: check <code>db_flashback_retention_target</code>, and if there are any guaranteed restore points.</html>");
			mtd.addColumn(CM_NAME, "OTHER_PCT",         "<html>Percent of the recovery area used by: everything else (control files, online redo logs, foreign archived logs...).</html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e);
		}
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		// No rows: no recovery area is configured
		if (this.size() == 0)
			return;

		String[] colNames = null;
		if (GRAPH_NAME_USED_PCT        .equals(tgdp.getName())) colNames = new String[] {"NET_USED_PCT", "USED_PCT", "RECLAIMABLE_PCT"};
		if (GRAPH_NAME_USED_MB         .equals(tgdp.getName())) colNames = new String[] {"LIMIT_MB", "USED_MB", "RECLAIMABLE_MB"};
		if (GRAPH_NAME_USED_PCT_BY_TYPE.equals(tgdp.getName())) colNames = new String[] {"ARCHIVED_LOG_PCT", "BACKUP_PCT", "FLASHBACK_LOG_PCT", "OTHER_PCT"};

		if (colNames != null)
		{
			Double[] arr = new Double[colNames.length];
			for (int i = 0; i < arr.length; i++)
				arr[i] = this.getAbsValueAsDouble(0, colNames[i], 0d);

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
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
		// NetUsedPct
		//-------------------------------------------------------
		if (isSystemAlarmsForColumnEnabledAndInTimeRange("NetUsedPct"))
		{
			double threshold = conf.getDoubleProperty(PROPKEY_alarm_NetUsedPct, DEFAULT_alarm_NetUsedPct);

			for (int r=0; r<cm.getAbsRowCount(); r++)
			{
				String destName       = cm.getAbsString       (r, "NAME");
				Double netUsedPct     = cm.getAbsValueAsDouble(r, "NET_USED_PCT");
				Double usedPct        = cm.getAbsValueAsDouble(r, "USED_PCT",        0d);
				Double reclaimablePct = cm.getAbsValueAsDouble(r, "RECLAIMABLE_PCT", 0d);
				Double netFreeMb      = cm.getAbsValueAsDouble(r, "NET_FREE_MB",     0d);

				// NULL: the limit is 0
				if (netUsedPct == null)
					continue;

				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest(" + cm.getName() + "): NetUsedPct: dest='" + destName + "', threshold=" + threshold + ", NET_USED_PCT='" + netUsedPct + "', USED_PCT='" + usedPct + "', RECLAIMABLE_PCT='" + reclaimablePct + "', NET_FREE_MB='" + netFreeMb + "'.");

				if (netUsedPct > threshold)
				{
					AlarmEvent ae = new AlarmEventOraRecoveryAreaLowFreeSpace(cm, destName, netFreeMb.intValue(), netUsedPct, usedPct, reclaimablePct, threshold);

					// The below is called from AlarmHandler on: Raise, RE-RAISE & CANCEL
					ae.setAlarmDescriptionProvider(this, new AlarmDescriptionProviderBase(cm.getPk(), cm.getAbsPkValue(r), threshold)
					{
						@Override
						public void setValues(CountersModel cm, AlarmEvent alarmEvent, AlarmPhase phase)
						{
							int    rowId = cm.getAbsRowIdForPkValue(getRowPk());
							String label = "Recovery Area '" + destName + "'";

							if (rowId != -1)
							{
								// Always: (RAISE, RE-RAISE, CANCEL) - Set graph values
								String extendedDescText = cm.toTextTableString(DATA_ABS, rowId);
								String extendedDescHtml = cm.toHtmlTableString(DATA_ABS, rowId, true, false, false);
								extendedDescHtml += "<br><br>" + cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_USED_PCT);
								extendedDescHtml += "<br><br>" + cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_USED_PCT_BY_TYPE);
								alarmEvent.setExtendedDescription(extendedDescText, extendedDescHtml);

								// Set CANCEL message (values that was found AFTER last raise/re-raise event)
								if (AlarmPhase.CANCEL.equals(phase))
								{
									Double NET_USED_PCT = cm.getAbsValueAsDouble(rowId, "NET_USED_PCT");
									Double NET_FREE_MB  = cm.getAbsValueAsDouble(rowId, "NET_FREE_MB");

									String cancelMsg = getAlarmCancelText(label + ": net used space", toAlarmCancelValue(NET_USED_PCT) + "%, " + toAlarmCancelValue(NET_FREE_MB) + " MB free", "%");
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
					ae.createAlarmOptionsMessage(this, "NetUsedPct");

					AlarmHandler.getInstance().addAlarm(ae);
				}
			} // end: loop-rows
		} // end: NetUsedPct
	}

	@Override
	public boolean isGraphDataHistoryEnabled(String name)
	{
		// ENABLED for the following graphs
		if (GRAPH_NAME_USED_PCT        .equals(name)) return true;
		if (GRAPH_NAME_USED_PCT_BY_TYPE.equals(name)) return true;

		// default: DISABLED
		return false;
	}

	public static final String  PROPKEY_alarm_NetUsedPct = CM_NAME + ".alarm.system.if.NetUsedPct.gt";
	public static final double  DEFAULT_alarm_NetUsedPct = 85.0;

	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;

		list.add(new CmSettingsHelper("NetUsedPct", isAlarmSwitch, PROPKEY_alarm_NetUsedPct, Double.class, conf.getDoubleProperty(PROPKEY_alarm_NetUsedPct, DEFAULT_alarm_NetUsedPct), DEFAULT_alarm_NetUsedPct, "If 'NET_USED_PCT' (used - reclaimable) is greater than ##.# Percent then send 'AlarmEventOraRecoveryAreaLowFreeSpace'." ));

		return list;
	}
}
