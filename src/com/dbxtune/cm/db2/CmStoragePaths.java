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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.NameNotFoundException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.alarm.AlarmHandler;
import com.dbxtune.alarm.events.AlarmDescriptionProviderBase;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.alarm.events.AlarmEventLowOsDiskFreeSpace;
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
 * The file systems the database stores its data on: the storage paths of the storage groups (automatic storage), and the containers of
 * the table spaces that are not automatic storage. How full the file system is.
 * <p>
 * An automatic storage table space grows into its storage path, so the table space itself never looks full: the file system is what fills up.
 *
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmStoragePaths
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmStoragePaths.class.getSimpleName();
	public static final String   SHORT_NAME       = "Storage Paths";
	public static final String   HTML_DESC        =
		"<html>" +
		"<p>The file systems the database stores its data on, and how full they are:</p>" +
		"<ul>" +
		"    <li>STORAGE PATH - the storage paths of the storage groups (automatic storage table spaces grow into them)</li>" +
		"    <li>CONTAINER - the containers of table spaces that are not automatic storage</li>" +
		"</ul>" +
		"<p>An automatic storage table space never looks full itself (see 'Table Space Usage'): when its file system is full, it can not grow, and inserts fail with SQL0289N.</p>" +
		"<p><b>Note</b>: the transaction log path and the archive log path are not here (Db2 has no function for their file systems). Use the OS disk space monitoring (CmOsDiskSpace) for them.</p>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {CM_NAME, "ADMIN_GET_STORAGE_PATHS", "MON_GET_CONTAINER"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = false;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 60;
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

		return new CmStoragePaths(counterController, guiController);
	}

	public CmStoragePaths(ICounterController counterController, IGuiController guiController)
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
	public static final String GRAPH_NAME_FS_FREE_MB  = "FsFreeMb";
	public static final String GRAPH_NAME_FS_USED_PCT = "FsUsedPct";

	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_FS_FREE_MB,
			"File System Free Space in MB, per Storage Path",              // Menu CheckBox text
			"File System Free Space in MB, per Storage Path or Container (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB, CentralPersistReader.SampleType.MIN_OVER_SAMPLES),
			null,
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_FS_USED_PCT,
			"File System Used in Percent, per Storage Path",               // Menu CheckBox text
			"File System Used in Percent, per Storage Path or Container (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERCENT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null,
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.DISK,
			true,  // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("PATH_TYPE");
		pkCols.add("PATH");
		pkCols.add("DBPARTITIONNUM");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		// NOTE: The sizes are in bytes. FS_ID identifies the file system (many paths can be on the same file system)
		// NOTE: Containers of automatic storage table spaces are in the storage paths (DB_STORAGE_PATH_ID is set): they are skipped
		String sql = ""
			+ "select /* ${cmCollectorName} */ \n"
			+ "     CAST('STORAGE PATH' AS VARCHAR(12))                                    as PATH_TYPE \n"
			+ "    ,p.STORAGE_GROUP_NAME                                                   as OWNER_NAME \n"
			+ "    ,p.DB_STORAGE_PATH                                                      as PATH \n"
			+ "    ,p.DBPARTITIONNUM \n"
			+ "    ,p.DB_STORAGE_PATH_STATE                                                as STATE \n"
			+ "    ,p.FS_ID \n"
			+ "    ,CAST(p.FS_TOTAL_SIZE / 1048576.0 AS DECIMAL(15,1))                     as FS_TOTAL_MB \n"
			+ "    ,CAST(p.FS_USED_SIZE  / 1048576.0 AS DECIMAL(15,1))                     as FS_USED_MB \n"
			+ "    ,CAST((p.FS_TOTAL_SIZE - p.FS_USED_SIZE) / 1048576.0 AS DECIMAL(15,1))  as FS_FREE_MB \n"
			+ "    ,CAST(p.FS_USED_SIZE * 100.0 / NULLIF(p.FS_TOTAL_SIZE, 0) AS DECIMAL(5,1)) as FS_USED_PCT \n"
			+ "    ,CAST(p.STO_PATH_FREE_SIZE / 1048576.0 AS DECIMAL(15,1))                as PATH_FREE_MB \n"
			+ "from table(ADMIN_GET_STORAGE_PATHS('', -2)) p \n"
			+ "union all \n"
			+ "select \n"
			+ "     CAST('CONTAINER' AS VARCHAR(12)) \n"
			+ "    ,c.TBSP_NAME \n"
			+ "    ,c.CONTAINER_NAME \n"
			+ "    ,c.DBPARTITIONNUM \n"
			+ "    ,CASE WHEN c.ACCESSIBLE = 1 THEN 'ACCESSIBLE' ELSE 'NOT_ACCESSIBLE' END \n"
			+ "    ,c.FS_ID \n"
			+ "    ,CAST(c.FS_TOTAL_SIZE / 1048576.0 AS DECIMAL(15,1)) \n"
			+ "    ,CAST(c.FS_USED_SIZE  / 1048576.0 AS DECIMAL(15,1)) \n"
			+ "    ,CAST((c.FS_TOTAL_SIZE - c.FS_USED_SIZE) / 1048576.0 AS DECIMAL(15,1)) \n"
			+ "    ,CAST(c.FS_USED_SIZE * 100.0 / NULLIF(c.FS_TOTAL_SIZE, 0) AS DECIMAL(5,1)) \n"
			+ "    ,CAST(NULL AS DECIMAL(15,1)) \n"
			+ "from table(MON_GET_CONTAINER('', -2)) c \n"
			+ "where c.DB_STORAGE_PATH_ID IS NULL \n"
			+ "";

		return sql;
	}

	@Override
	public boolean checkDependsOnOther(DbxConnection conn)
	{
		return Db2CmUtils.checkFunctionsAreExecutable(this, conn, "ADMIN_GET_STORAGE_PATHS('', -2)", "MON_GET_CONTAINER('', -2)");
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addTable(CM_NAME, "The file systems the database stores its data on (storage paths and containers), and how full they are.");

			mtd.addColumn(CM_NAME, "PATH_TYPE",      "<html>STORAGE PATH = a storage path of a storage group (automatic storage). CONTAINER = a container of a table space that is not automatic storage.</html>");
			mtd.addColumn(CM_NAME, "OWNER_NAME",     "<html>The storage group (for a storage path) or the table space (for a container).</html>");
			mtd.addColumn(CM_NAME, "PATH",           "<html>The storage path, or the container (a file or a directory).</html>");
			mtd.addColumn(CM_NAME, "DBPARTITIONNUM", "<html>The database partition (0 if this is not DPF).</html>");
			mtd.addColumn(CM_NAME, "STATE",          "<html>For a storage path: IN_USE, NOT_IN_USE or DROP_PENDING. For a container: ACCESSIBLE or NOT_ACCESSIBLE.</html>");
			mtd.addColumn(CM_NAME, "FS_ID",          "<html>The id of the file system. Paths with the same id are on the same file system (they share the free space).</html>");
			mtd.addColumn(CM_NAME, "FS_TOTAL_MB",    "<html>The size of the file system, in MB.</html>");
			mtd.addColumn(CM_NAME, "FS_USED_MB",     "<html>Used space of the file system (by anything, not only the database), in MB.</html>");
			mtd.addColumn(CM_NAME, "FS_FREE_MB",     "<html>Free space of the file system, in MB.</html>");
			mtd.addColumn(CM_NAME, "FS_USED_PCT",    "<html>How full the file system is, in percent.<br><b>Alarm</b>: LowFsFreeSpacePct</html>");
			mtd.addColumn(CM_NAME, "PATH_FREE_MB",   "<html>For a storage path: the free space that Db2 can use in the path, in MB. (STO_PATH_FREE_SIZE)</html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e);
		}
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		String colName = null;
		if (GRAPH_NAME_FS_FREE_MB .equals(tgdp.getName())) colName = "FS_FREE_MB";
		if (GRAPH_NAME_FS_USED_PCT.equals(tgdp.getName())) colName = "FS_USED_PCT";

		if (colName == null || this.getAbsRowCount() == 0)
			return;

		// One line per path
		Map<String, Double> map = new LinkedHashMap<>();
		for (int r = 0; r < this.getAbsRowCount(); r++)
		{
			Double val = this.getAbsValueAsDouble(r, colName);
			map.put(this.getAbsString(r, "PATH"), val == null ? 0d : val);
		}

		tgdp.setData(this.getTimestamp(), map);
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
		// LowFsFreeSpacePct: the file system of a storage path or a container is almost full
		// One alarm per file system (FS_ID): many paths can be on the same file system
		//-------------------------------------------------------
		if (isSystemAlarmsForColumnEnabledAndInTimeRange("LowFsFreeSpacePct"))
		{
			double threshold = conf.getDoubleProperty(PROPKEY_alarm_LowFsFreeSpacePct, DEFAULT_alarm_LowFsFreeSpacePct);

			Set<String> doneFsIds = new HashSet<>();
			for (int r = 0; r < cm.getAbsRowCount(); r++)
			{
				String fsId      = cm.getAbsString(r, "FS_ID");
				final String path = cm.getAbsString(r, "PATH");
				Double usedPct   = cm.getAbsValueAsDouble(r, "FS_USED_PCT");
				Double freeMb    = cm.getAbsValueAsDouble(r, "FS_FREE_MB");

				if (usedPct == null || freeMb == null || (fsId != null && doneFsIds.contains(fsId)))
					continue;

				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest(" + cm.getName() + "): LowFsFreeSpacePct: path='" + path + "', FS_ID='" + fsId + "', threshold=" + threshold + ", FS_USED_PCT=" + usedPct + ", FS_FREE_MB=" + freeMb + ".");

				if (usedPct > threshold)
				{
					if (fsId != null)
						doneFsIds.add(fsId);

					AlarmEvent ae = new AlarmEventLowOsDiskFreeSpace(cm, path, freeMb.intValue(), usedPct, threshold);

					final String raiseText = cm.toTextTableString(DATA_ABS, r);
					final String raiseHtml = cm.toHtmlTableString(DATA_ABS, r, true, false, false);

					// The below is called from AlarmHandler on: Raise, RE-RAISE & CANCEL
					ae.setAlarmDescriptionProvider(this, new AlarmDescriptionProviderBase(null, null, threshold)
					{
						@Override
						public void setValues(CountersModel cm, AlarmEvent alarmEvent, AlarmPhase phase)
						{
							// Always: (RAISE, RE-RAISE, CANCEL) - Set the extended description (from when it was (re)raised)
							alarmEvent.setExtendedDescription(raiseText, raiseHtml);

							// Set CANCEL message (values that was found AFTER last raise/re-raise event)
							if (AlarmPhase.CANCEL.equals(phase))
							{
								int rowId = cm.getRowIdWhere(DATA_ABS, "PATH", path);
								if (rowId == -1)
									alarmEvent.setCancelDescription("The path '" + path + "' is no longer used by the database");
								else
									alarmEvent.setCancelDescription(getAlarmCancelText("FS_USED_PCT", cm.getAbsValue(rowId, "FS_USED_PCT"), "%"));
							}
						}
					});

					// Information about how to disable this alarm
					ae.createAlarmOptionsMessage(this, "LowFsFreeSpacePct");

					AlarmHandler.getInstance().addAlarm(ae);
				}
			}
		}
	}

	public static final String  PROPKEY_alarm_LowFsFreeSpacePct = CM_NAME + ".alarm.system.if.FS_USED_PCT.gt";
	public static final double  DEFAULT_alarm_LowFsFreeSpacePct = 90.0;

	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;

		list.add(new CmSettingsHelper("LowFsFreeSpacePct", isAlarmSwitch, PROPKEY_alarm_LowFsFreeSpacePct, Double.class, conf.getDoubleProperty(PROPKEY_alarm_LowFsFreeSpacePct, DEFAULT_alarm_LowFsFreeSpacePct), DEFAULT_alarm_LowFsFreeSpacePct, "If 'FS_USED_PCT' (how full the file system of a storage path or a container is) is greater than ## percent then send 'AlarmEventLowOsDiskFreeSpace' (one per file system)." ));

		return list;
	}

	@Override
	public List<CmChartDescriptor> createChartDescriptors()
	{
		List<CmChartDescriptor> list = new ArrayList<>();

		// An automatic storage table space is full when the file system of its storage paths is
		list.add(new CmChartDescriptor()
			.id("fs-used-pct")
			.title("File System Used in Percent, per Storage Path or Container")
			.chartType(CmChartDescriptor.CHART_TYPE_STACKED_BAR)
			.splitDir(CmChartDescriptor.SPLIT_VERTICAL)
			.labelColumn("PATH")
			.valueColumns("FS_USED_PCT")
			.seriesLabels("Used %")
			.barLabelColumn("FS_FREE_MB")
			.barLabelPrefix("free MB: ")
			.isPercent(true)
			.thresholdWarn(80)
			.thresholdCrit(90));

		return list;
	}
}
