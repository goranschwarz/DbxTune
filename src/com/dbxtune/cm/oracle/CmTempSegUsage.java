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
import com.dbxtune.alarm.events.oracle.AlarmEventOraTablespaceLowFreeSpace;
import com.dbxtune.central.pcs.CentralPersistReader;
import com.dbxtune.cm.CmChartDescriptor;
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
public class CmTempSegUsage
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmTempSegUsage.class.getSimpleName();
	public static final String   SHORT_NAME       = "Temp Usage";
	public static final String   HTML_DESC        =
		"<html>" +
		"How much of the temporary tablespace(s) that is in use right now, and by whom.<br>" +
		"<br>" +
		"Temp space is used by sorts, hash joins, temporary tables and temporary LOBs that do not fit in memory.<br>" +
		"There are two kinds of rows (column <code>ROW_TYPE</code>):" +
		"<ul>" +
		"  <li><b>TABLESPACE</b> - one row per temporary tablespace: what is really in use, and how big it can get.</li>" +
		"  <li><b>SESSION</b>    - one row per session and type of usage (SORT, HASH, DATA, LOB_DATA...) that holds temp space right now, the biggest first.</li>" +
		"</ul>" +
		"<b>Note</b>: The tab 'Tablespaces' shows a temporary tablespace as (almost) full most of the time. That is normal: space that has been used once stays allocated, and is reused.<br>" +
		"What matters is what is <i>in use</i>, which is what this tab shows (and has the alarm on). When it is used up, statements fail with 'ORA-01652: unable to extend temp segment'.<br>" +
		"<b>Note</b>: In a Container Database, this shows the temporary tablespace(s) of the container you are connected to.<br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_DISK;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {CM_NAME, "V$TEMPSEG_USAGE", "DBA_TEMP_FREE_SPACE"};
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
			guiController.splashWindowProgress("Loading: Counter Model '" + CM_NAME + "'");

		return new CmTempSegUsage(counterController, guiController);
	}

	public CmTempSegUsage(ICounterController counterController, IGuiController guiController)
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
	public static final String GRAPH_NAME_IN_USE_MB = "TempInUseMb";

	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_IN_USE_MB,
			"Temp Space, In Use in MB", 	                // Menu CheckBox text
			"Temporary Tablespace(s), Space that is In Use right now in MB (" + SHORT_NAME + ")", // Graph Label
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

		pkCols.add("ROW_TYPE");
		pkCols.add("TABLESPACE");
		pkCols.add("SID");
		pkCols.add("SERIAL#");
		pkCols.add("SEGTYPE");

		return pkCols;
	}

	/** If we can't read the views: disable the CM, and tell what to do about it */
	@Override
	public boolean checkDependsOnOther(DbxConnection conn)
	{
		return OracleCmUtils.checkViewsAreReadable(this, conn, "V$TEMPSEG_USAGE", "V$SESSION", "DBA_TEMP_FREE_SPACE", "DBA_TEMP_FILES", "DBA_TABLESPACES");
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		// Two kinds of rows, so the graph and the alarm always has a row to work with (also when no session uses temp space)
		//   TABLESPACE - one row per temporary tablespace. In use = TABLESPACE_SIZE - FREE_SPACE (where FREE_SPACE includes space that is allocated but can be reused)
		//   SESSION    - one row per session and segment type that holds temp space right now
		// MAX_MB is how big the tablespace can get (the files with AUTOEXTEND at it's MAXSIZE)
		// This looks the same in all versions
		// Note: CAST(... as NUMBER(#,1)) is needed to keep decimals (a NUMBER without precision is treated as a BIGINT)
		String maxBytes = "(select TABLESPACE_NAME, sum(greatest(BYTES, MAXBYTES)) as MAX_BYTES from DBA_TEMP_FILES group by TABLESPACE_NAME)";

		String sql = ""
			+ "select \n"
			+ "     CAST('TABLESPACE' as VARCHAR2(10))                                                             as ROW_TYPE \n"
			+ "    ,f.TABLESPACE_NAME                                                                              as TABLESPACE \n"
			+ "    ,CAST(0    as NUMBER(10))                                                                       as SID \n"
			+ "    ,CAST(0    as NUMBER(10))                                                                       as SERIAL# \n"
			+ "    ,CAST('-'  as VARCHAR2(9))                                                                      as SEGTYPE \n"
			+ "    ,CAST(NULL as VARCHAR2(128))                                                                    as USERNAME \n"
			+ "    ,CAST(NULL as VARCHAR2(64))                                                                     as PROGRAM \n"
			+ "    ,CAST(NULL as VARCHAR2(64))                                                                     as MODULE \n"
			+ "    ,CAST(NULL as VARCHAR2(13))                                                                     as SQL_ID \n"
			+ "    ,CAST((f.TABLESPACE_SIZE - f.FREE_SPACE) / 1048576                           as NUMBER(18,1))   as IN_USE_MB \n"
			+ "    ,CAST(100 * (f.TABLESPACE_SIZE - f.FREE_SPACE) / NULLIF(m.MAX_BYTES, 0)      as NUMBER(5,1))    as IN_USE_PCT \n"
			+ "    ,CAST(f.TABLESPACE_SIZE / 1048576                                            as NUMBER(18,1))   as ALLOC_MB \n"
			+ "    ,CAST(m.MAX_BYTES       / 1048576                                            as NUMBER(18,1))   as MAX_MB \n"
			+ "    ,(select count(distinct x.SESSION_ADDR) from V$TEMPSEG_USAGE x where x.TABLESPACE = f.TABLESPACE_NAME) as SESSION_COUNT \n"
			+ "from DBA_TEMP_FREE_SPACE f \n"
			+ "join " + maxBytes + " m on m.TABLESPACE_NAME = f.TABLESPACE_NAME \n"
			+ "union all \n"
			+ "select \n"
			+ "     CAST('SESSION' as VARCHAR2(10))                                                                as ROW_TYPE \n"
			+ "    ,u.TABLESPACE                                                                                   as TABLESPACE \n"
			+ "    ,s.SID \n"
			+ "    ,s.SERIAL# \n"
			+ "    ,u.SEGTYPE \n"
			+ "    ,s.USERNAME \n"
			+ "    ,CAST(s.PROGRAM as VARCHAR2(64))                                                                as PROGRAM \n"
			+ "    ,CAST(s.MODULE  as VARCHAR2(64))                                                                as MODULE \n"
			+ "    ,s.SQL_ID \n"
			+ "    ,CAST(sum(u.BLOCKS) * t.BLOCK_SIZE / 1048576                                 as NUMBER(18,1))   as IN_USE_MB \n"
			+ "    ,CAST(100 * sum(u.BLOCKS) * t.BLOCK_SIZE / NULLIF(m.MAX_BYTES, 0)            as NUMBER(5,1))    as IN_USE_PCT \n"
			+ "    ,CAST(NULL as NUMBER(18,1))                                                                     as ALLOC_MB \n"
			+ "    ,CAST(NULL as NUMBER(18,1))                                                                     as MAX_MB \n"
			+ "    ,CAST(NULL as NUMBER(10))                                                                       as SESSION_COUNT \n"
			+ "from V$TEMPSEG_USAGE u \n"
			+ "join V$SESSION s       on s.SADDR           = u.SESSION_ADDR \n"
			+ "join DBA_TABLESPACES t on t.TABLESPACE_NAME = u.TABLESPACE \n"
			+ "join " + maxBytes + " m on m.TABLESPACE_NAME = u.TABLESPACE \n"
			+ "group by u.TABLESPACE, s.SID, s.SERIAL#, u.SEGTYPE, s.USERNAME, s.PROGRAM, s.MODULE, s.SQL_ID, t.BLOCK_SIZE, m.MAX_BYTES \n"
			+ "order by 1 desc, 10 desc \n"
			+ "";

		return sql;
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			mtd.addTable(CM_NAME, "How much of the temporary tablespace(s) that is in use right now, and by whom.");

			mtd.addColumn(CM_NAME, "ROW_TYPE",      "<html><b>TABLESPACE</b> = the row is the total for a temporary tablespace.<br><b>SESSION</b> = the row is what one session uses (for one type of usage).</html>");
			mtd.addColumn(CM_NAME, "TABLESPACE",    "<html>Name of the temporary tablespace.</html>");
			mtd.addColumn(CM_NAME, "SID",           "<html>Session ID. 0 on a TABLESPACE row.</html>");
			mtd.addColumn(CM_NAME, "SERIAL#",       "<html>Session serial number. 0 on a TABLESPACE row.</html>");
			mtd.addColumn(CM_NAME, "SEGTYPE",       "<html>What the temp space is used for: SORT, HASH, DATA (temporary tables), INDEX, LOB_DATA, LOB_INDEX. '-' on a TABLESPACE row.</html>");
			mtd.addColumn(CM_NAME, "SQL_ID",        "<html>The statement the session is running right now. <b>Note</b>: this is not always the statement that allocated the temp space.</html>");
			mtd.addColumn(CM_NAME, "IN_USE_MB",     "<html>Temp space in use right now, in MB.<br><b>TABLESPACE row</b>: (TABLESPACE_SIZE - FREE_SPACE) from DBA_TEMP_FREE_SPACE<br><b>SESSION row</b>: sum(BLOCKS) * block size from V$TEMPSEG_USAGE</html>");
			mtd.addColumn(CM_NAME, "IN_USE_PCT",    "<html>IN_USE_MB in percent of how big the tablespace can get (MAX_MB). This is what the alarm checks, on the TABLESPACE rows.</html>");
			mtd.addColumn(CM_NAME, "ALLOC_MB",      "<html>Size in MB that the temp files are right now. Only on a TABLESPACE row.</html>");
			mtd.addColumn(CM_NAME, "MAX_MB",        "<html>Size in MB the tablespace can grow to, when AUTOEXTENSIBLE files has reached it's MAXSIZE. Only on a TABLESPACE row.<br><b>Note</b>: The files can only grow if the disk has room for it.</html>");
			mtd.addColumn(CM_NAME, "SESSION_COUNT", "<html>Number of sessions that use the temporary tablespace right now. Only on a TABLESPACE row (0 on a SESSION row).</html>");
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
		// One "line" for every temporary tablespace (the TABLESPACE rows)
		//--------------------------------------------------------
		if (GRAPH_NAME_IN_USE_MB.equals(tgdp.getName()))
		{
			LinkedHashMap<String, Double> dataMap = new LinkedHashMap<String, Double>();

			for (int r = 0; r < this.size(); r++)
			{
				if ("TABLESPACE".equals(this.getAbsString(r, "ROW_TYPE")))
					dataMap.put(this.getAbsString(r, "TABLESPACE"), this.getAbsValueAsDouble(r, "IN_USE_MB", 0d));
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
		// TempInUsePct
		//-------------------------------------------------------
		if (isSystemAlarmsForColumnEnabledAndInTimeRange("TempInUsePct"))
		{
			double threshold = conf.getDoubleProperty(PROPKEY_alarm_TempInUsePct, DEFAULT_alarm_TempInUsePct);

			for (int r=0; r<cm.getAbsRowCount(); r++)
			{
				// Only the rows that are the total for a temporary tablespace
				if ( ! "TABLESPACE".equals(cm.getAbsString(r, "ROW_TYPE")) )
					continue;

				String tsName   = cm.getAbsString       (r, "TABLESPACE");
				Double inUsePct = cm.getAbsValueAsDouble(r, "IN_USE_PCT");
				Double inUseMb  = cm.getAbsValueAsDouble(r, "IN_USE_MB", 0d);
				Double maxMb    = cm.getAbsValueAsDouble(r, "MAX_MB",    0d);

				if (inUsePct == null)
					continue;

				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest(" + cm.getName() + "): TempInUsePct: tablespace='" + tsName + "', threshold=" + threshold + ", IN_USE_PCT='" + inUsePct + "', IN_USE_MB='" + inUseMb + "', MAX_MB='" + maxMb + "'.");

				if (inUsePct > threshold)
				{
					AlarmEvent ae = new AlarmEventOraTablespaceLowFreeSpace(cm, tsName, (int) (maxMb - inUseMb), inUsePct, threshold);

					// The below is called from AlarmHandler on: Raise, RE-RAISE & CANCEL
					ae.setAlarmDescriptionProvider(this, new AlarmDescriptionProviderBase(cm.getPk(), cm.getAbsPkValue(r), threshold)
					{
						@Override
						public void setValues(CountersModel cm, AlarmEvent alarmEvent, AlarmPhase phase)
						{
							int    rowId = cm.getAbsRowIdForPkValue(getRowPk());
							String label = "Temporary tablespace '" + tsName + "'";

							if (rowId != -1)
							{
								// Always: (RAISE, RE-RAISE, CANCEL) - Set the tablespace row, and the biggest users as a table
								String extendedDescText = cm.toTextTableString(DATA_ABS, rowId);
								String extendedDescHtml = cm.toHtmlTableString(DATA_ABS, rowId, true, false, false);

								// The 3 sessions that uses most (the SESSION rows comes in that order)
								int added = 0;
								for (int i=0; i<cm.getAbsRowCount() && added < 3; i++)
								{
									if ("SESSION".equals(cm.getAbsString(i, "ROW_TYPE")) && tsName.equals(cm.getAbsString(i, "TABLESPACE")))
									{
										extendedDescText += "\n" + cm.toTextTableString(DATA_ABS, i);
										extendedDescHtml += "<br>" + cm.toHtmlTableString(DATA_ABS, i, true, false, false);
										added++;
									}
								}
								extendedDescHtml += "<br><br>" + cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_IN_USE_MB, tsName);
								alarmEvent.setExtendedDescription(extendedDescText, extendedDescHtml);

								// Set CANCEL message (values that was found AFTER last raise/re-raise event)
								if (AlarmPhase.CANCEL.equals(phase))
								{
									Double IN_USE_PCT = cm.getAbsValueAsDouble(rowId, "IN_USE_PCT");
									Double IN_USE_MB  = cm.getAbsValueAsDouble(rowId, "IN_USE_MB");

									String cancelMsg = getAlarmCancelText(label + ": space in use", toAlarmCancelValue(IN_USE_PCT) + "%, " + toAlarmCancelValue(IN_USE_MB) + " MB", "%");
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
					ae.createAlarmOptionsMessage(this, "TempInUsePct");

					AlarmHandler.getInstance().addAlarm(ae);
				}
			} // end: loop-rows
		} // end: TempInUsePct
	}

	@Override
	public boolean isGraphDataHistoryEnabled(String name)
	{
		// ENABLED for the following graphs
		if (GRAPH_NAME_IN_USE_MB.equals(name)) return true;

		// default: DISABLED
		return false;
	}

	public static final String  PROPKEY_alarm_TempInUsePct = CM_NAME + ".alarm.system.if.TempInUsePct.gt";
	public static final double  DEFAULT_alarm_TempInUsePct = 90.0;

	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;

		list.add(new CmSettingsHelper("TempInUsePct", isAlarmSwitch, PROPKEY_alarm_TempInUsePct, Double.class, conf.getDoubleProperty(PROPKEY_alarm_TempInUsePct, DEFAULT_alarm_TempInUsePct), DEFAULT_alarm_TempInUsePct, "If 'IN_USE_PCT' of a temporary tablespace (what is in use right now, in percent of how big it can get) is greater than ##.# Percent then send 'AlarmEventOraTablespaceLowFreeSpace'." ));

		return list;
	}

	/** Local graphs (beside the table): which sessions use the temp space, and how full the temporary tablespaces are */
	@Override
	public List<CmChartDescriptor> createChartDescriptors()
	{
		List<CmChartDescriptor> list = new ArrayList<>();

		// A session can have more than one row (one per kind of segment: SORT, HASH, LOB_DATA...): they are added up
		list.add(new CmChartDescriptor()
			.id("temp-by-session")
			.title("Temp Space per Session (MB)")
			.chartType(CmChartDescriptor.CHART_TYPE_DUAL_PIE_BAR)
			.labelColumn("SID")
			.labelPrefixColumns("USERNAME")
			.filter("ROW_TYPE", "SESSION")
			.valueColumns("IN_USE_MB")
			.seriesLabels("In Use MB")
			.skipZeroRows(true)
			.maxItems(15)
			.pieOtherLimit(0.02)
			.splitRatio(0.6));

		list.add(new CmChartDescriptor()
			.id("temp-used-pct")
			.title("Temporary Tablespaces: In Use in Percent (of the size they can grow to)")
			.chartType(CmChartDescriptor.CHART_TYPE_STACKED_BAR)
			.labelColumn("TABLESPACE")
			.filter("ROW_TYPE", "TABLESPACE")
			.valueColumns("IN_USE_PCT")
			.seriesLabels("In Use %")
			.barLabelColumn("IN_USE_MB")
			.barLabelPrefix("IN USE MB: ")
			.isPercent(true)
			.thresholdWarn(80)
			.thresholdCrit(90));

		return list;
	}
}
