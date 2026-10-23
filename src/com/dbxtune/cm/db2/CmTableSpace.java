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

import javax.naming.NameNotFoundException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.alarm.AlarmHandler;
import com.dbxtune.alarm.events.AlarmDescriptionProviderBase;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.alarm.events.AlarmEventLowDbFreeSpace;
import com.dbxtune.alarm.events.db2.AlarmEventDb2TablespaceState;
import com.dbxtune.central.pcs.CentralPersistReader;
import com.dbxtune.cm.CmChartDescriptor;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CmSettingsHelper.IntegerInputValidator;
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
 * Size of the table spaces: used, free, can it grow, and its state.
 * <p>
 * A table space with automatic storage (or auto-resize) grows by itself, so "used %" of what is allocated right now says nothing:
 * the limit is the file system (see the tab 'Storage Paths') or MAXSIZE. Only a table space that can NOT grow raises the free space alarm.
 *
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmTableSpace
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmTableSpace.class.getSimpleName();
	public static final String   SHORT_NAME       = "Table Space Size";
	public static final String   HTML_DESC        =
		"<html>" +
		"<h4>Table Space Size</h4>" +
		"Size of the table spaces (one row per table space and member): allocated, used and free MB, if it can grow, and its state.<br>" +
		"<br>" +
		"<b>CAN_GROW</b>: A table space with <i>automatic storage</i> or <i>AUTORESIZE YES</i> grows by itself, until the file system is full (tab 'Storage Paths') or MAXSIZE is reached.<br>" +
		"So <code>USED_PCT</code> of what is allocated right now only matters for a table space that can <b>not</b> grow.<br>" +
		"<br>" +
		"<b>Alarms</b>: <i>LowFreeSpacePct</i> (only when it can not grow, or when it is close to MAXSIZE), <i>TbspState</i> (not NORMAL, for example BACKUP_PENDING or OFFLINE).<br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"MON_GET_TABLESPACE", CM_NAME};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	// The sizes are gauges: the Diff column shows the growth since the last sample
	public static final String[] DIFF_COLUMNS     = new String[] {
		"TOTAL_MB",
		"USED_MB"
	};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = false;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = 60; //CountersModel.DEFAULT_sqlQueryTimeout;

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

		return new CmTableSpace(counterController, guiController);
	}

	public CmTableSpace(ICounterController counterController, IGuiController guiController)
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
	public static final String GRAPH_NAME_USED_MB = "TbspUsedMb";

	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_USED_MB,
			"Table Space Used MB",                                        // Menu CheckBox text
			"Table Space Used MB, per Table Space (" + SHORT_NAME + ")",  // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null,
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.SPACE,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height
	}

	@Override
	public String[] getDependsOnConfigForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return NEED_CONFIG;
	}

	@Override
	public boolean checkDependsOnOther(DbxConnection conn)
	{
		return Db2CmUtils.checkFunctionsAreExecutable(this, conn, "MON_GET_TABLESPACE('', -2)");
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addTable(CM_NAME,  "Size of the table spaces: allocated, used and free MB, if it can grow, and its state.");

			mtd.addColumn(CM_NAME, "TBSP_NAME",                "<html>Table space name.</html>");
			mtd.addColumn(CM_NAME, "TBSP_ID",                  "<html>Table space id.</html>");
			mtd.addColumn(CM_NAME, "MEMBER",                   "<html>The database member (0 if this is not pureScale or DPF).</html>");
			mtd.addColumn(CM_NAME, "DB_NAME",                  "<html>The database.</html>");
			mtd.addColumn(CM_NAME, "TBSP_TYPE",                "<html><b>DMS</b> (database managed: files or devices that Db2 allocates) or <b>SMS</b> (system managed: directories, the files grow with the data).</html>");
			mtd.addColumn(CM_NAME, "TBSP_CONTENT_TYPE",        "<html>ANY / LARGE (regular data), SYSTEMP (system temporary), USRTEMP (user temporary).</html>");
			mtd.addColumn(CM_NAME, "STORAGE_GROUP_NAME",       "<html>The storage group (automatic storage), where it gets more space from.</html>");
			mtd.addColumn(CM_NAME, "TBSP_USING_AUTO_STORAGE",  "<html>1 = automatic storage (grows by itself, in the storage paths of its storage group).</html>");
			mtd.addColumn(CM_NAME, "TBSP_AUTO_RESIZE_ENABLED", "<html>1 = AUTORESIZE YES (a DMS table space that grows by itself).</html>");
			mtd.addColumn(CM_NAME, "CAN_GROW",                 "<html>Can the table space get more space by itself?<br>"
			                                                 + "<b>YES</b>: automatic storage or AUTORESIZE YES (until the file system is full, see tab 'Storage Paths').<br>"
			                                                 + "<b>YES_TO_MAX</b>: it grows, but only up to MAXSIZE (see USED_PCT_OF_MAX).<br>"
			                                                 + "<b>FILESYSTEM</b>: an SMS table space, it grows until the file system is full.<br>"
			                                                 + "<b>NO</b>: a DMS table space with AUTORESIZE NO: when it is full, inserts fail (SQL0289N) until someone adds space.<br>"
			                                                 + "<b>NO_MAX_REACHED</b>: it has reached MAXSIZE.</html>");
			mtd.addColumn(CM_NAME, "TBSP_STATE",               "<html>State of the table space: NORMAL, or for example BACKUP_PENDING (after a LOAD ... COPY NO), ROLLFORWARD_PENDING, OFFLINE (a container is missing), QUIESCED_..., REORG_IN_PROGRESS.<br><b>Alarm</b>: TbspState</html>");
			mtd.addColumn(CM_NAME, "TBSP_PAGE_SIZE",           "<html>Page size in bytes.</html>");
			mtd.addColumn(CM_NAME, "TOTAL_MB",                 "<html>Allocated size, in MB. The Diff column shows the growth since the last sample.</html>");
			mtd.addColumn(CM_NAME, "USABLE_MB",                "<html>Usable size (allocated minus overhead), in MB.</html>");
			mtd.addColumn(CM_NAME, "USED_MB",                  "<html>Used size, in MB. The Diff column shows the growth since the last sample.</html>");
			mtd.addColumn(CM_NAME, "FREE_MB",                  "<html>Free size of what is allocated, in MB (for an SMS table space always 0: the files grow with the data).</html>");
			mtd.addColumn(CM_NAME, "USED_PCT",                 "<html>Used in percent of the usable (allocated) size.<br><b>Note</b>: For a table space that can grow (CAN_GROW = YES), this says nothing: it allocates more when it needs it.<br><b>Formula</b>: 100 * TBSP_USED_PAGES / TBSP_USABLE_PAGES<br><b>Alarm</b>: LowFreeSpacePct (only when CAN_GROW is NO)</html>");
			mtd.addColumn(CM_NAME, "MAX_MB",                   "<html>MAXSIZE of the table space, in MB. NULL = no limit (it grows until the file system is full).</html>");
			mtd.addColumn(CM_NAME, "USED_PCT_OF_MAX",          "<html>Used in percent of MAXSIZE (NULL if there is no MAXSIZE).<br><b>Alarm</b>: LowFreeSpacePct (when CAN_GROW is YES_TO_MAX)</html>");
			mtd.addColumn(CM_NAME, "HWM_MB",                   "<html>High water mark: the highest page that is used, in MB (TBSP_PAGE_TOP). A DMS table space can not be reduced below it.</html>");
			mtd.addColumn(CM_NAME, "INCREASE_SIZE",            "<html>How much it grows each time (AUTORESIZE / automatic storage): a size in KB, or a percent ('10%'). AUTOMATIC = Db2 decides.</html>");
			mtd.addColumn(CM_NAME, "TBSP_LAST_RESIZE_TIME",    "<html>When the table space was resized (grown) the last time.</html>");
			mtd.addColumn(CM_NAME, "TBSP_NUM_CONTAINERS",      "<html>Number of containers (files, directories or devices).</html>");
			mtd.addColumn(CM_NAME, "RECLAIMABLE_SPACE_ENABLED","<html>1 = the free space can be given back to the file system (ALTER TABLESPACE ... REDUCE MAX).</html>");
			mtd.addColumn(CM_NAME, "TBSP_USED_PAGES",          "<html>Used pages.</html>");
			mtd.addColumn(CM_NAME, "TBSP_FREE_PAGES",          "<html>Free pages.</html>");
			mtd.addColumn(CM_NAME, "TBSP_USABLE_PAGES",        "<html>Usable pages.</html>");
			mtd.addColumn(CM_NAME, "TBSP_TOTAL_PAGES",         "<html>Total (allocated) pages.</html>");
			mtd.addColumn(CM_NAME, "TBSP_PENDING_FREE_PAGES",  "<html>Pages that will be free when the transactions that freed them have committed.</html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e);
		}
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("TBSP_ID");
		pkCols.add("MEMBER");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		// NOTE: TBSP_MAX_SIZE is in bytes (-1 = no limit), TBSP_INCREASE_SIZE in bytes (-1 = AUTOMATIC), TBSP_INCREASE_SIZE_PERCENT in percent (-1 = not used)
		// NOTE: An SMS table space has no free pages (the files grow with the data), so USED_PCT is 100
		String sql = ""
			+ "select /* ${cmCollectorName} */ \n"
			+ "     TBSP_NAME \n"
			+ "    ,TBSP_ID \n"
			+ "    ,MEMBER \n"
			+ "    ,CURRENT SERVER as DB_NAME \n"
			+ "    ,TBSP_TYPE \n"
			+ "    ,TBSP_CONTENT_TYPE \n"
			+ "    ,STORAGE_GROUP_NAME \n"
			+ "    ,TBSP_USING_AUTO_STORAGE \n"
			+ "    ,TBSP_AUTO_RESIZE_ENABLED \n"
			+ "    ,CAST(CASE \n"
			+ "        WHEN TBSP_TYPE = 'SMS' \n"
			+ "            THEN 'FILESYSTEM' \n"
			+ "        WHEN TBSP_USING_AUTO_STORAGE = 1 OR TBSP_AUTO_RESIZE_ENABLED = 1 \n"
			+ "            THEN CASE WHEN TBSP_MAX_SIZE > 0 AND TBSP_TOTAL_PAGES * TBSP_PAGE_SIZE >= TBSP_MAX_SIZE THEN 'NO_MAX_REACHED' \n"
			+ "                      WHEN TBSP_MAX_SIZE > 0                                                 THEN 'YES_TO_MAX' \n"
			+ "                      ELSE 'YES' END \n"
			+ "        ELSE 'NO' \n"
			+ "     END AS VARCHAR(20)) as CAN_GROW \n"
			+ "    ,TBSP_STATE \n"
			+ "    ,TBSP_PAGE_SIZE \n"
			+ "    ,CAST(TBSP_TOTAL_PAGES  * TBSP_PAGE_SIZE / 1048576.0 AS DECIMAL(15,1)) as TOTAL_MB \n"
			+ "    ,CAST(TBSP_USABLE_PAGES * TBSP_PAGE_SIZE / 1048576.0 AS DECIMAL(15,1)) as USABLE_MB \n"
			+ "    ,CAST(TBSP_USED_PAGES   * TBSP_PAGE_SIZE / 1048576.0 AS DECIMAL(15,1)) as USED_MB \n"
			+ "    ,CAST(TBSP_FREE_PAGES   * TBSP_PAGE_SIZE / 1048576.0 AS DECIMAL(15,1)) as FREE_MB \n"
			+ "    ,CAST(CASE WHEN TBSP_USABLE_PAGES > 0 THEN 100.0 * TBSP_USED_PAGES / TBSP_USABLE_PAGES END AS DECIMAL(5,1)) as USED_PCT \n"
			+ "    ,CAST(CASE WHEN TBSP_MAX_SIZE > 0 THEN TBSP_MAX_SIZE / 1048576.0 END AS DECIMAL(15,1)) as MAX_MB \n"
			+ "    ,CAST(CASE WHEN TBSP_MAX_SIZE > 0 THEN 100.0 * TBSP_USED_PAGES * TBSP_PAGE_SIZE / TBSP_MAX_SIZE END AS DECIMAL(5,1)) as USED_PCT_OF_MAX \n"
			+ "    ,CAST(TBSP_PAGE_TOP     * TBSP_PAGE_SIZE / 1048576.0 AS DECIMAL(15,1)) as HWM_MB \n"
			+ "    ,CAST(CASE WHEN TBSP_INCREASE_SIZE_PERCENT > 0 THEN TRIM(CHAR(TBSP_INCREASE_SIZE_PERCENT)) || '%' \n"
			+ "               WHEN TBSP_INCREASE_SIZE > 0         THEN TRIM(CHAR(TBSP_INCREASE_SIZE / 1024)) || ' KB' \n"
			+ "               WHEN TBSP_INCREASE_SIZE = -1        THEN 'AUTOMATIC' \n"
			+ "          END AS VARCHAR(20)) as INCREASE_SIZE \n"
			+ "    ,TBSP_LAST_RESIZE_TIME \n"
			+ "    ,TBSP_NUM_CONTAINERS \n"
			+ "    ,RECLAIMABLE_SPACE_ENABLED \n"
			+ "    ,TBSP_USED_PAGES \n"
			+ "    ,TBSP_FREE_PAGES \n"
			+ "    ,TBSP_USABLE_PAGES \n"
			+ "    ,TBSP_TOTAL_PAGES \n"
			+ "    ,TBSP_PENDING_FREE_PAGES \n"
			+ "from table(MON_GET_TABLESPACE('', -2)) t \n"
			+ "order by TBSP_NAME, MEMBER \n"
			+ "";

		return sql;
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		if (GRAPH_NAME_USED_MB.equals(tgdp.getName()))
		{
			// One line per table space (the sum of the members)
			java.util.Map<String, Double> map = new java.util.LinkedHashMap<>();
			for (int r = 0; r < this.getAbsRowCount(); r++)
			{
				String name = this.getAbsString(r, "TBSP_NAME");
				Double val  = this.getAbsValueAsDouble(r, "USED_MB");
				map.merge(name, val == null ? 0d : val, Double::sum);
			}

			String[] lArray = map.keySet().toArray(new String[0]);
			Double[] dArray = map.values().toArray(new Double[0]);
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

		for (int r=0; r<cm.getAbsRowCount(); r++)
		{
			String dbname   = cm.getAbsString(r, "DB_NAME");
			String tbspName = cm.getAbsString(r, "TBSP_NAME");
			String canGrow  = cm.getAbsString(r, "CAN_GROW");

			//-------------------------------------------------------
			// LowFreeSpacePct: only for a table space that can not grow, or that grows up to MAXSIZE
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("LowFreeSpacePct"))
			{
				int    threshold = conf.getIntProperty(PROPKEY_alarm_LowFreeSpacePct, DEFAULT_alarm_LowFreeSpacePct);
				Double usedPct   = null;

				if (canGrow.startsWith("NO"))         usedPct = "NO_MAX_REACHED".equals(canGrow) ? cm.getAbsValueAsDouble(r, "USED_PCT_OF_MAX") : cm.getAbsValueAsDouble(r, "USED_PCT");
				else if ("YES_TO_MAX".equals(canGrow)) usedPct = cm.getAbsValueAsDouble(r, "USED_PCT_OF_MAX");

				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest(" + cm.getName() + "): LowFreeSpacePct: tbsp='" + tbspName + "', CAN_GROW='" + canGrow + "', usedPct=" + usedPct + ", threshold=" + threshold + ".");

				if (usedPct != null && usedPct > threshold)
				{
					Double freeMb = "NO".equals(canGrow) ? cm.getAbsValueAsDouble(r, "FREE_MB") : diff(cm.getAbsValueAsDouble(r, "MAX_MB"), cm.getAbsValueAsDouble(r, "USED_MB"));

					AlarmEvent ae = new AlarmEventLowDbFreeSpace(cm, dbname + ":" + tbspName, freeMb == null ? 0 : freeMb.intValue(), usedPct, (double) threshold);

					// The below is called from AlarmHandler on: Raise, RE-RAISE & CANCEL
					ae.setAlarmDescriptionProvider(this, new AlarmDescriptionProviderBase(cm.getPk(), cm.getAbsPkValue(r), threshold)
					{
						@Override
						public void setValues(CountersModel cm, AlarmEvent alarmEvent, AlarmPhase phase)
						{
							int    rowId = cm.getAbsRowIdForPkValue(getRowPk());
							String label = "Table space '" + tbspName + "'";

							if (rowId != -1)
							{
								// Always: (RAISE, RE-RAISE, CANCEL)
								String extendedDescText = cm.toTextTableString(DATA_ABS, rowId);
								String extendedDescHtml = cm.toHtmlTableString(DATA_ABS, rowId, true, false, false);
								extendedDescHtml += "<br><br>" + cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_USED_MB, tbspName);
								alarmEvent.setExtendedDescription(extendedDescText, extendedDescHtml);

								// Set CANCEL message (values that was found AFTER last raise/re-raise event)
								if (AlarmPhase.CANCEL.equals(phase))
								{
									String nowCanGrow = cm.getAbsString(rowId, "CAN_GROW");
									Double nowPct     = "YES_TO_MAX".equals(nowCanGrow) || "NO_MAX_REACHED".equals(nowCanGrow) ? cm.getAbsValueAsDouble(rowId, "USED_PCT_OF_MAX") : cm.getAbsValueAsDouble(rowId, "USED_PCT");
									if ("YES".equals(nowCanGrow))
										alarmEvent.setCancelDescription(label + " can now grow by itself (CAN_GROW=YES).");
									else
										alarmEvent.setCancelDescription(getAlarmCancelText(label + ": used", toAlarmCancelValue(nowPct), "%"));
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
					ae.createAlarmOptionsMessage(this, "LowFreeSpacePct");

					AlarmHandler.getInstance().addAlarm(ae);
				}
			} // end: LowFreeSpacePct

			//-------------------------------------------------------
			// TbspState: not NORMAL (and not something that is going on and ends by itself, like a REORG)
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("TbspState"))
			{
				String state     = cm.getAbsString(r, "TBSP_STATE");
				String skipRegex = conf.getProperty(PROPKEY_alarm_TbspState_skipRegex, DEFAULT_alarm_TbspState_skipRegex);

				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest(" + cm.getName() + "): TbspState: tbsp='" + tbspName + "', TBSP_STATE='" + state + "', skipRegex='" + skipRegex + "'.");

				if (StringUtil.hasValue(state) && ! "NORMAL".equalsIgnoreCase(state.trim()) && (StringUtil.isNullOrBlank(skipRegex) || ! state.trim().matches(skipRegex)))
				{
					AlarmEvent ae = new AlarmEventDb2TablespaceState(cm, dbname, tbspName, state.trim());

					// The below is called from AlarmHandler on: Raise, RE-RAISE & CANCEL
					ae.setAlarmDescriptionProvider(this, new AlarmDescriptionProviderBase(cm.getPk(), cm.getAbsPkValue(r), null)
					{
						@Override
						public void setValues(CountersModel cm, AlarmEvent alarmEvent, AlarmPhase phase)
						{
							int    rowId = cm.getAbsRowIdForPkValue(getRowPk());
							String label = "Table space '" + tbspName + "'";

							if (rowId != -1)
							{
								// Always: (RAISE, RE-RAISE, CANCEL)
								String extendedDescText = cm.toTextTableString(DATA_ABS, rowId);
								String extendedDescHtml = cm.toHtmlTableString(DATA_ABS, rowId, true, false, false);
								alarmEvent.setExtendedDescription(extendedDescText, extendedDescHtml);

								// Set CANCEL message (values that was found AFTER last raise/re-raise event)
								if (AlarmPhase.CANCEL.equals(phase))
									alarmEvent.setCancelDescription(label + " is now in state '" + cm.getAbsString(rowId, "TBSP_STATE") + "'.");
							}
							else
							{
								if (AlarmPhase.CANCEL.equals(phase))
									alarmEvent.setCancelDescription(getAlarmCancelTextGone(label));
							}
						}
					});

					// Information about how to disable this alarm
					ae.createAlarmOptionsMessage(this, "TbspState");

					AlarmHandler.getInstance().addAlarm(ae);
				}
			} // end: TbspState
		}
	}

	private static Double diff(Double a, Double b)
	{
		if (a == null || b == null)
			return null;
		return a - b;
	}

	@Override
	public boolean isGraphDataHistoryEnabled(String name)
	{
		// ENABLED for the following graphs
		if (GRAPH_NAME_USED_MB.equals(name)) return true;

		// default: DISABLED
		return false;
	}

	public static final String  PROPKEY_alarm_LowFreeSpacePct     = CM_NAME + ".alarm.system.if.USED_PCT.gt";
	public static final int     DEFAULT_alarm_LowFreeSpacePct     = 90;

	public static final String  PROPKEY_alarm_TbspState_skipRegex = CM_NAME + ".alarm.system.if.TBSP_STATE.skip.regex";
	public static final String  DEFAULT_alarm_TbspState_skipRegex = ".*_IN_PROGRESS.*";

	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;

		list.add(new CmSettingsHelper("LowFreeSpacePct", isAlarmSwitch, PROPKEY_alarm_LowFreeSpacePct    , Integer.class, conf.getIntProperty(PROPKEY_alarm_LowFreeSpacePct, DEFAULT_alarm_LowFreeSpacePct), DEFAULT_alarm_LowFreeSpacePct, "If a table space that can NOT grow (CAN_GROW is NO) has 'USED_PCT' above ## percent, or one that grows up to MAXSIZE has 'USED_PCT_OF_MAX' above ## percent, then send 'AlarmEventLowDbFreeSpace'. A table space with automatic storage or AUTORESIZE YES (CAN_GROW=YES) never raises this: see the tab 'Storage Paths'.", new IntegerInputValidator()));
		list.add(new CmSettingsHelper("TbspState",       isAlarmSwitch, PROPKEY_alarm_TbspState_skipRegex, String .class, conf.getProperty   (PROPKEY_alarm_TbspState_skipRegex, DEFAULT_alarm_TbspState_skipRegex), DEFAULT_alarm_TbspState_skipRegex, "If 'TBSP_STATE' is not NORMAL, then send 'AlarmEventDb2TablespaceState'. States that match this regular expression are skipped (by default: things that are going on and end by themselves, like REORG_IN_PROGRESS).", new RegExpInputValidator()));

		return list;
	}

	@Override
	public List<CmChartDescriptor> createChartDescriptors()
	{
		List<CmChartDescriptor> list = new ArrayList<>();

		// USED_PCT is of the allocated size: it only matters for a table space that can NOT grow (CAN_GROW), the others allocate more when they need it
		list.add(new CmChartDescriptor()
			.id("tbsp-used-pct")
			.title("Table Space Used in Percent of the Allocated Size (only a problem when CAN_GROW is NO)")
			.chartType(CmChartDescriptor.CHART_TYPE_STACKED_BAR)
			.splitDir(CmChartDescriptor.SPLIT_VERTICAL)
			.labelColumn("TBSP_NAME")
			.valueColumns("USED_PCT")
			.seriesLabels("Used %")
			.barLabelColumn("CAN_GROW")
			.isPercent(true)
			.thresholdWarn(80)
			.thresholdCrit(90));

		list.add(new CmChartDescriptor()
			.id("tbsp-used-mb")
			.title("Used Space per Table Space (MB)")
			.chartType(CmChartDescriptor.CHART_TYPE_DUAL_PIE_BAR)
			.splitDir(CmChartDescriptor.SPLIT_VERTICAL)
			.labelColumn("TBSP_NAME")
			.valueColumns("USED_MB")
			.seriesLabels("Used MB")
			.skipZeroRows(true)
			.pieOtherLimit(0.02));

		return list;
	}
}
