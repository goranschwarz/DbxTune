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
import java.util.LinkedHashMap;
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
import com.dbxtune.alarm.events.AlarmEventConfigResourceIsLow;
import com.dbxtune.central.pcs.CentralPersistReader;
import com.dbxtune.cm.CmChartDescriptor;
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
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmResourceLimit
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmResourceLimit.class.getSimpleName();
	public static final String   SHORT_NAME       = "Resource Limits";
	public static final String   HTML_DESC        =
		"<html>" +
		"How much is used of the resources that has a limit (processes, sessions, locks...).<br>" +
		"<br>" +
		"The most important ones are <b>processes</b> and <b>sessions</b>: when they are used up, no one can connect (ORA-00020: maximum number of processes exceeded).<br>" +
		"<code>MAX_UTILIZATION</code> is the highest value since the instance was started. If it is close to <code>LIMIT_VALUE</code>, the limit has (almost) been reached at some point.<br>" +
		"A resource where <code>LIMIT_VALUE</code> is UNLIMITED has no <code>USED_PCT</code>.<br>" +
		"<br>" +
		"<b>Note</b>: In a Container Database the limits belong to the whole instance. So this is empty when connected to a PDB, connect to CDB$ROOT to see it.<br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {CM_NAME, "V$RESOURCE_LIMIT"};
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
	@Override public Type    getTemplateLevel()                       { return Type.SMALL; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '" + CM_NAME + "'");

		return new CmResourceLimit(counterController, guiController);
	}

	public CmResourceLimit(ICounterController counterController, IGuiController guiController)
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
	public static final String GRAPH_NAME_USED_PCT = "ResLimitUsedPct";

	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_USED_PCT,
			"Resource Limits, Used in Percent", 	                // Menu CheckBox text
			"Resource Limits (processes, sessions...), Used in Percent of the Limit (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERCENT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null,
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.SRV_CONFIG,
			true,  // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("RESOURCE_NAME");

		return pkCols;
	}

	/** If we can't read the views: disable the CM, and tell what to do about it */
	@Override
	public boolean checkDependsOnOther(DbxConnection conn)
	{
		return OracleCmUtils.checkViewsAreReadable(this, conn, "V$RESOURCE_LIMIT");
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		// INITIAL_ALLOCATION and LIMIT_VALUE are strings padded with spaces, which can be a number or 'UNLIMITED'
		// A resource that is 'UNLIMITED' (or has the limit 0) has NULL in: FREE_COUNT, USED_PCT, MAX_USED_PCT
		// Note: CAST(... as NUMBER(5,1)) is needed to keep decimals (a NUMBER without precision is treated as a BIGINT)
		String hasLimit = "REGEXP_LIKE(TRIM(r.LIMIT_VALUE), '^[0-9]+$') and TRIM(r.LIMIT_VALUE) <> '0'";

		String sql = ""
			+ "select \n"
			+ "     r.RESOURCE_NAME \n"
			+ "    ,r.CURRENT_UTILIZATION \n"
			+ "    ,r.MAX_UTILIZATION \n"
			+ "    ,CAST(TRIM(r.INITIAL_ALLOCATION) as VARCHAR2(40)) as INITIAL_ALLOCATION \n"
			+ "    ,CAST(TRIM(r.LIMIT_VALUE)        as VARCHAR2(40)) as LIMIT_VALUE \n"
			+ "    ,CAST(case when " + hasLimit + " then TO_NUMBER(TRIM(r.LIMIT_VALUE)) - r.CURRENT_UTILIZATION               end as NUMBER(12,0)) as FREE_COUNT \n"
			+ "    ,CAST(case when " + hasLimit + " then 100 * r.CURRENT_UTILIZATION / TO_NUMBER(TRIM(r.LIMIT_VALUE))         end as NUMBER(5,1))  as USED_PCT \n"
			+ "    ,CAST(case when " + hasLimit + " then 100 * r.MAX_UTILIZATION     / TO_NUMBER(TRIM(r.LIMIT_VALUE))         end as NUMBER(5,1))  as MAX_USED_PCT \n"
			+ "from V$RESOURCE_LIMIT r \n"
			+ "order by r.RESOURCE_NAME \n"
			+ "";

		return sql;
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			mtd.addTable(CM_NAME, "How much is used of the resources that has a limit (processes, sessions, locks...).");

			mtd.addColumn(CM_NAME, "FREE_COUNT",   "<html>How many more that can be used before the limit is reached. NULL if the limit is UNLIMITED.<br><b>Formula</b>: LIMIT_VALUE - CURRENT_UTILIZATION</html>");
			mtd.addColumn(CM_NAME, "USED_PCT",     "<html>How much of the limit that is used right now, in percent. NULL if the limit is UNLIMITED.<br><b>Formula</b>: 100 * CURRENT_UTILIZATION / LIMIT_VALUE</html>");
			mtd.addColumn(CM_NAME, "MAX_USED_PCT", "<html>The highest usage since the instance was started, in percent of the limit. NULL if the limit is UNLIMITED.<br><b>Formula</b>: 100 * MAX_UTILIZATION / LIMIT_VALUE</html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e);
		}
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		//--------------------------------------------------------
		// One "line" for every resource that has a limit (USED_PCT is NULL for the ones that are UNLIMITED)
		//--------------------------------------------------------
		if (GRAPH_NAME_USED_PCT.equals(tgdp.getName()))
		{
			LinkedHashMap<String, Double> dataMap = new LinkedHashMap<String, Double>();

			for (int r = 0; r < this.size(); r++)
			{
				Double usedPct = this.getAbsValueAsDouble(r, "USED_PCT");
				if (usedPct != null)
					dataMap.put(this.getAbsString(r, "RESOURCE_NAME"), usedPct);
			}

			if (dataMap.size() > 0)
				tgdp.setData(this.getTimestamp(), dataMap);
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
		// UsedPct
		//-------------------------------------------------------
		if (isSystemAlarmsForColumnEnabledAndInTimeRange("UsedPct"))
		{
			double threshold         = conf.getDoubleProperty(PROPKEY_alarm_UsedPct             , DEFAULT_alarm_UsedPct);
			String resourceNameRegex = conf.getProperty      (PROPKEY_alarm_UsedPctResourceNames, DEFAULT_alarm_UsedPctResourceNames);

			for (int r=0; r<cm.getAbsRowCount(); r++)
			{
				String resourceName = cm.getAbsString(r, "RESOURCE_NAME");
				Double usedPct      = cm.getAbsValueAsDouble(r, "USED_PCT");

				// Only the resources we want to check, and that has a limit
				if (usedPct == null || StringUtil.isNullOrBlank(resourceNameRegex) || ! resourceName.matches(resourceNameRegex))
					continue;

				Double freeCount = cm.getAbsValueAsDouble(r, "FREE_COUNT");
				Double current   = cm.getAbsValueAsDouble(r, "CURRENT_UTILIZATION");

				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest(" + cm.getName() + "): UsedPct: resource='" + resourceName + "', threshold=" + threshold + ", USED_PCT='" + usedPct + "', FREE_COUNT='" + freeCount + "'.");

				if (usedPct > threshold)
				{
					AlarmEvent ae = new AlarmEventConfigResourceIsLow(cm, resourceName, freeCount, current, usedPct, threshold);

					// The below is called from AlarmHandler on: Raise, RE-RAISE & CANCEL
					ae.setAlarmDescriptionProvider(this, new AlarmDescriptionProviderBase(cm.getPk(), cm.getAbsPkValue(r), threshold)
					{
						@Override
						public void setValues(CountersModel cm, AlarmEvent alarmEvent, AlarmPhase phase)
						{
							int    rowId = cm.getAbsRowIdForPkValue(getRowPk());
							String label = "Resource '" + resourceName + "'";

							if (rowId != -1)
							{
								// Always: (RAISE, RE-RAISE, CANCEL) - Set graph values
								String extendedDescText = cm.toTextTableString(DATA_ABS, rowId);
								String extendedDescHtml = cm.toHtmlTableString(DATA_ABS, rowId, true, false, false);
								extendedDescHtml += "<br><br>" + cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_USED_PCT, resourceName);
								alarmEvent.setExtendedDescription(extendedDescText, extendedDescHtml);

								// Set CANCEL message (values that was found AFTER last raise/re-raise event)
								if (AlarmPhase.CANCEL.equals(phase))
								{
									Double USED_PCT   = cm.getAbsValueAsDouble(rowId, "USED_PCT");
									Double FREE_COUNT = cm.getAbsValueAsDouble(rowId, "FREE_COUNT");

									String cancelMsg = getAlarmCancelText(label + ": used", toAlarmCancelValue(USED_PCT) + "%, " + (FREE_COUNT == null ? "unknown" : FREE_COUNT.intValue()) + " free", "%");
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
			} // end: loop-rows
		} // end: UsedPct
	}

	@Override
	public boolean isGraphDataHistoryEnabled(String name)
	{
		// ENABLED for the following graphs
		if (GRAPH_NAME_USED_PCT.equals(name)) return true;

		// default: DISABLED
		return false;
	}

	public static final String  PROPKEY_alarm_UsedPct              = CM_NAME + ".alarm.system.if.UsedPct.gt";
	public static final double  DEFAULT_alarm_UsedPct              = 90.0;

	public static final String  PROPKEY_alarm_UsedPctResourceNames = CM_NAME + ".alarm.system.if.UsedPct.resourceNames";
	public static final String  DEFAULT_alarm_UsedPctResourceNames = "(processes|sessions)";

	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;

		list.add(new CmSettingsHelper("UsedPct"              , isAlarmSwitch, PROPKEY_alarm_UsedPct             , Double.class, conf.getDoubleProperty(PROPKEY_alarm_UsedPct             , DEFAULT_alarm_UsedPct             ), DEFAULT_alarm_UsedPct             , "If 'USED_PCT' is greater than ##.# Percent then send 'AlarmEventConfigResourceIsLow'." ));
		list.add(new CmSettingsHelper("UsedPct ResourceNames",                PROPKEY_alarm_UsedPctResourceNames, String.class, conf.getProperty      (PROPKEY_alarm_UsedPctResourceNames, DEFAULT_alarm_UsedPctResourceNames), DEFAULT_alarm_UsedPctResourceNames, "If 'UsedPct' is true; Only check resources where 'RESOURCE_NAME' matches this regexp.", new RegExpInputValidator()));

		return list;
	}

	/** Local graphs (above the table): how much of each limit is used now, and the most that has been used since startup */
	@Override
	public List<CmChartDescriptor> createChartDescriptors()
	{
		List<CmChartDescriptor> list = new ArrayList<>();

		// The resources without a limit (UNLIMITED) have no percent, so they are not in the charts
		list.add(new CmChartDescriptor()
			.id("limit-used-pct")
			.title("Resource Limits: Used Now in Percent")
			.chartType(CmChartDescriptor.CHART_TYPE_STACKED_BAR)
			.splitDir(CmChartDescriptor.SPLIT_VERTICAL)
			.labelColumn("RESOURCE_NAME")
			.valueColumns("USED_PCT")
			.seriesLabels("Used %")
			.barLabelColumn("FREE_COUNT")
			.barLabelPrefix("FREE: ")
			.isPercent(true)
			.thresholdWarn(80)
			.thresholdCrit(90)
			.skipZeroRows(true));

		list.add(new CmChartDescriptor()
			.id("limit-max-used-pct")
			.title("Resource Limits: Highest Usage since Startup in Percent")
			.chartType(CmChartDescriptor.CHART_TYPE_STACKED_BAR)
			.splitDir(CmChartDescriptor.SPLIT_VERTICAL)
			.labelColumn("RESOURCE_NAME")
			.valueColumns("MAX_USED_PCT")
			.seriesLabels("Max Used %")
			.isPercent(true)
			.thresholdWarn(80)
			.thresholdCrit(90)
			.skipZeroRows(true));

		return list;
	}
}
