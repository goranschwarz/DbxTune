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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.central.pcs.CentralPersistReader;
import com.dbxtune.cm.CounterSample;
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

/**
 * CPU, memory and load of the host(s) Db2 runs on, as Db2 sees it (ENV_GET_SYSTEM_RESOURCES).
 * <p>
 * It works without SSH (the host monitor CmOs* needs an SSH login to the host).
 *
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmSystemResourses
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmSystemResourses.class.getSimpleName();
	public static final String   SHORT_NAME       = "System Resources";
	public static final String   HTML_DESC        =
		"<html>" +
		"<h4>System Resources</h4>" +
		"CPU, memory and load of the host that Db2 runs on (one row per member), as Db2 sees it.<br>" +
		"It works without an SSH login to the host (which the OS tabs need).<br>" +
		"<br>" +
		"The CPU columns <code>CPU_*_PCT</code> are for the last sample in the Diff and Rate view, and since the host was started in the Abs view.<br>" +
		"<b>Note</b>: In a container or a virtual machine, it is what the container or the VM sees.<br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"ENV_GET_SYSTEM_RESOURCES"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"CPU_USER",
		"CPU_IDLE",
		"CPU_IOWAIT",
		"CPU_SYSTEM",
		"SWAP_PAGES_IN",
		"SWAP_PAGES_OUT"
	};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = 60; //CountersModel.DEFAULT_sqlQueryTimeout;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.ALL; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '" + CM_NAME + "'");

		return new CmSystemResourses(counterController, guiController);
	}

	public CmSystemResourses(ICounterController counterController, IGuiController guiController)
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
	public static final String GRAPH_NAME_CPU      = "CpuPct";
	public static final String GRAPH_NAME_LOAD_AVG = "LoadAvg";
	public static final String GRAPH_NAME_MEMORY   = "MemoryUsedPct";

	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_CPU,
			"Host CPU Usage in Percent",                                  // Menu CheckBox text
			"Host CPU Usage in Percent: user, system and I/O wait (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERCENT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Busy", "User", "System", "IO Wait" },
			LabelType.Static,
			TrendGraphDataPoint.Category.CPU,
			true,  // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_LOAD_AVG,
			"Host Load Average",                                          // Menu CheckBox text
			"Host Load Average: short, medium, long (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Short", "Medium", "Long" },
			LabelType.Static,
			TrendGraphDataPoint.Category.CPU,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_MEMORY,
			"Host Memory Used in Percent",                                // Menu CheckBox text
			"Host Memory Used in Percent, physical memory and swap (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERCENT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Memory Used", "Swap Used" },
			LabelType.Static,
			TrendGraphDataPoint.Category.MEMORY,
			true,  // is Percent Graph
			false, // visible at start
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
		return Db2CmUtils.checkFunctionsAreExecutable(this, conn, "SYSPROC.ENV_GET_SYSTEM_RESOURCES()");
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addTable("ENV_GET_SYSTEM_RESOURCES",  "The ENV_GET_SYSTEM_RESOURCES table function returns operating system, CPU, memory, and other information that is related to members on the system.");

			mtd.addColumn("ENV_GET_SYSTEM_RESOURCES", "MEMBER"                ,    "<html>member - Database member monitor element</html>");
			mtd.addColumn("ENV_GET_SYSTEM_RESOURCES", "HOST_NAME"             ,    "<html>host_name - Host name monitor element</html>");
			mtd.addColumn("ENV_GET_SYSTEM_RESOURCES", "OS_NAME"               ,    "<html>os_name - Operating system name monitor element</html>");
			mtd.addColumn("ENV_GET_SYSTEM_RESOURCES", "OS_FULL_VERSION"       ,    "<html>os_full_version - Operating system full version monitor element</html>");
			mtd.addColumn("ENV_GET_SYSTEM_RESOURCES", "OS_KERNEL_VERSION"     ,    "<html>os_kernel_version - Operating system kernel identifier monitor element</html>");
			mtd.addColumn("ENV_GET_SYSTEM_RESOURCES", "CPU_TOTAL"             ,    "<html>cpu_total - Number of CPUs</html>");
			mtd.addColumn("ENV_GET_SYSTEM_RESOURCES", "CPU_ONLINE"            ,    "<html>cpu_online - Number of CPUs online</html>");
			mtd.addColumn("ENV_GET_SYSTEM_RESOURCES", "CPU_CONFIGURED"        ,    "<html>cpu_configured - Number of configured CPUs</html>");
			mtd.addColumn("ENV_GET_SYSTEM_RESOURCES", "CPU_SPEED"             ,    "<html>cpu_speed - CPU clock speed, in MHz</html>");
			mtd.addColumn("ENV_GET_SYSTEM_RESOURCES", "CPU_BUSY_PCT"          ,    "<html>CPU busy in percent: user + system + I/O wait.<br><b>Formula</b>: 100 * (CPU_USER + CPU_SYSTEM + CPU_IOWAIT) / (CPU_USER + CPU_SYSTEM + CPU_IOWAIT + CPU_IDLE)<br>Abs: since the host was started. Diff and Rate: in the sample.</html>");
			mtd.addColumn("ENV_GET_SYSTEM_RESOURCES", "CPU_USER_PCT"          ,    "<html>CPU used by user programs (Db2 is mostly here), in percent. See CPU_BUSY_PCT for how it is computed.</html>");
			mtd.addColumn("ENV_GET_SYSTEM_RESOURCES", "CPU_SYSTEM_PCT"        ,    "<html>CPU used by the kernel, in percent. See CPU_BUSY_PCT for how it is computed.</html>");
			mtd.addColumn("ENV_GET_SYSTEM_RESOURCES", "CPU_IOWAIT_PCT"        ,    "<html>CPU idle while waiting for I/O, in percent. See CPU_BUSY_PCT for how it is computed.</html>");
			mtd.addColumn("ENV_GET_SYSTEM_RESOURCES", "CPU_LOAD_SHORT"        ,    "<html>cpu_load_short - Processor load (short timeframe, 1 minute on Linux)</html>");
			mtd.addColumn("ENV_GET_SYSTEM_RESOURCES", "CPU_LOAD_MEDIUM"       ,    "<html>cpu_load_medium - Processor load (medium timeframe, 5 minutes on Linux)</html>");
			mtd.addColumn("ENV_GET_SYSTEM_RESOURCES", "CPU_LOAD_LONG"         ,    "<html>cpu_load_long - Processor load (long timeframe, 15 minutes on Linux)</html>");
			mtd.addColumn("ENV_GET_SYSTEM_RESOURCES", "CPU_USER"              ,    "<html>cpu_user - Non-kernel processing time, in CPU ticks (on Linux 100 per second per CPU)</html>");
			mtd.addColumn("ENV_GET_SYSTEM_RESOURCES", "CPU_IDLE"              ,    "<html>cpu_idle - Processor idle time, in CPU ticks</html>");
			mtd.addColumn("ENV_GET_SYSTEM_RESOURCES", "CPU_IOWAIT"            ,    "<html>cpu_iowait - IO Wait time, in CPU ticks</html>");
			mtd.addColumn("ENV_GET_SYSTEM_RESOURCES", "CPU_SYSTEM"            ,    "<html>cpu_system - Kernel time, in CPU ticks</html>");
			mtd.addColumn("ENV_GET_SYSTEM_RESOURCES", "MEMORY_TOTAL"          ,    "<html>memory_total - Total physical memory, in MB</html>");
			mtd.addColumn("ENV_GET_SYSTEM_RESOURCES", "MEMORY_FREE"           ,    "<html>memory_free - Free physical memory, in MB</html>");
			mtd.addColumn("ENV_GET_SYSTEM_RESOURCES", "MEMORY_USED_PCT"       ,    "<html>Physical memory used, in percent.<br><b>Formula</b>: 100 - 100 * MEMORY_FREE / MEMORY_TOTAL</html>");
			mtd.addColumn("ENV_GET_SYSTEM_RESOURCES", "MEMORY_SWAP_TOTAL"     ,    "<html>memory_swap_total - Total swap space, in MB</html>");
			mtd.addColumn("ENV_GET_SYSTEM_RESOURCES", "MEMORY_SWAP_FREE"      ,    "<html>memory_swap_free - Free swap space, in MB</html>");
			mtd.addColumn("ENV_GET_SYSTEM_RESOURCES", "SWAP_USED_PCT"         ,    "<html>Swap space used, in percent.<br><b>Formula</b>: 100 - 100 * MEMORY_SWAP_FREE / MEMORY_SWAP_TOTAL</html>");
			mtd.addColumn("ENV_GET_SYSTEM_RESOURCES", "VIRTUAL_MEM_TOTAL"     ,    "<html>virtual_mem_total - Total virtual memory, in MB</html>");
			mtd.addColumn("ENV_GET_SYSTEM_RESOURCES", "VIRTUAL_MEM_FREE"      ,    "<html>virtual_mem_free - Free virtual memory, in MB</html>");
			mtd.addColumn("ENV_GET_SYSTEM_RESOURCES", "SWAP_PAGE_SIZE"        ,    "<html>swap_page_size - Swap page size, in bytes</html>");
			mtd.addColumn("ENV_GET_SYSTEM_RESOURCES", "SWAP_PAGES_IN"         ,    "<html>swap_pages_in - Pages swapped in from disk. Above 0 most of the time means that the host has too little memory.</html>");
			mtd.addColumn("ENV_GET_SYSTEM_RESOURCES", "SWAP_PAGES_OUT"        ,    "<html>swap_pages_out - Pages swapped out to disk</html>");
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

		pkCols.add("MEMBER");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		// NOTE: The CPU_xxx counters are CPU ticks (on Linux 100 per second per CPU), MEMORY_xxx are MB
		//       The _PCT columns are since the host started here, they are re-calculated for the sample in localCalculation()
		String sql = ""
			+ "select /* ${cmCollectorName} */ \n"
			+ "     MEMBER \n"
			+ "    ,HOST_NAME \n"
			+ "    ,OS_NAME \n"
			+ "    ,OS_FULL_VERSION \n"
			+ "    ,OS_KERNEL_VERSION \n"
			+ "    ,CPU_TOTAL \n"
			+ "    ,CPU_ONLINE \n"
			+ "    ,CPU_CONFIGURED \n"
			+ "    ,CPU_SPEED \n"
			+ "    ,CAST(CASE WHEN (CPU_USER + CPU_SYSTEM + CPU_IOWAIT + CPU_IDLE) > 0 THEN 100.0 * (CPU_USER + CPU_SYSTEM + CPU_IOWAIT) / (CPU_USER + CPU_SYSTEM + CPU_IOWAIT + CPU_IDLE) END AS DECIMAL(5,1)) as CPU_BUSY_PCT \n"
			+ "    ,CAST(CASE WHEN (CPU_USER + CPU_SYSTEM + CPU_IOWAIT + CPU_IDLE) > 0 THEN 100.0 *  CPU_USER                             / (CPU_USER + CPU_SYSTEM + CPU_IOWAIT + CPU_IDLE) END AS DECIMAL(5,1)) as CPU_USER_PCT \n"
			+ "    ,CAST(CASE WHEN (CPU_USER + CPU_SYSTEM + CPU_IOWAIT + CPU_IDLE) > 0 THEN 100.0 *  CPU_SYSTEM                           / (CPU_USER + CPU_SYSTEM + CPU_IOWAIT + CPU_IDLE) END AS DECIMAL(5,1)) as CPU_SYSTEM_PCT \n"
			+ "    ,CAST(CASE WHEN (CPU_USER + CPU_SYSTEM + CPU_IOWAIT + CPU_IDLE) > 0 THEN 100.0 *  CPU_IOWAIT                           / (CPU_USER + CPU_SYSTEM + CPU_IOWAIT + CPU_IDLE) END AS DECIMAL(5,1)) as CPU_IOWAIT_PCT \n"
			+ "    ,CPU_LOAD_SHORT \n"
			+ "    ,CPU_LOAD_MEDIUM \n"
			+ "    ,CPU_LOAD_LONG \n"
			+ "    ,CPU_USER \n"
			+ "    ,CPU_IDLE \n"
			+ "    ,CPU_IOWAIT \n"
			+ "    ,CPU_SYSTEM \n"
			+ "    ,MEMORY_TOTAL \n"
			+ "    ,MEMORY_FREE \n"
			+ "    ,CAST(CASE WHEN MEMORY_TOTAL      > 0 THEN 100.0 - 100.0 * MEMORY_FREE      / MEMORY_TOTAL      END AS DECIMAL(5,1)) as MEMORY_USED_PCT \n"
			+ "    ,MEMORY_SWAP_TOTAL \n"
			+ "    ,MEMORY_SWAP_FREE \n"
			+ "    ,CAST(CASE WHEN MEMORY_SWAP_TOTAL > 0 THEN 100.0 - 100.0 * MEMORY_SWAP_FREE / MEMORY_SWAP_TOTAL END AS DECIMAL(5,1)) as SWAP_USED_PCT \n"
			+ "    ,VIRTUAL_MEM_TOTAL \n"
			+ "    ,VIRTUAL_MEM_FREE \n"
			+ "    ,SWAP_PAGE_SIZE \n"
			+ "    ,SWAP_PAGES_IN \n"
			+ "    ,SWAP_PAGES_OUT \n"
			+ "from table(SYSPROC.ENV_GET_SYSTEM_RESOURCES()) t \n"
			+ "order by MEMBER \n"
			+ "";

		return sql;
	}

	/** The CPU_xxx_PCT columns: in the diff (and rate) data, for the sample (from the tick counters that was diffed) */
	@Override
	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
	{
		int pos_user    = diffData.findColumn("CPU_USER");
		int pos_system  = diffData.findColumn("CPU_SYSTEM");
		int pos_iowait  = diffData.findColumn("CPU_IOWAIT");
		int pos_idle    = diffData.findColumn("CPU_IDLE");
		int pos_busyPct = diffData.findColumn("CPU_BUSY_PCT");
		int pos_userPct = diffData.findColumn("CPU_USER_PCT");
		int pos_sysPct  = diffData.findColumn("CPU_SYSTEM_PCT");
		int pos_ioPct   = diffData.findColumn("CPU_IOWAIT_PCT");

		if (pos_user == -1 || pos_system == -1 || pos_iowait == -1 || pos_idle == -1 || pos_busyPct == -1 || pos_userPct == -1 || pos_sysPct == -1 || pos_ioPct == -1)
		{
			_logger.warn(getName() + ": localCalculation(), Problems finding one of the CPU columns.");
			return;
		}

		for (int rowId=0; rowId < diffData.getRowCount(); rowId++)
		{
			double user   = diffData.getValueAsDouble(rowId, pos_user  , 0d);
			double system = diffData.getValueAsDouble(rowId, pos_system, 0d);
			double iowait = diffData.getValueAsDouble(rowId, pos_iowait, 0d);
			double idle   = diffData.getValueAsDouble(rowId, pos_idle  , 0d);
			double total  = user + system + iowait + idle;

			diffData.setValueAt(pct(user + system + iowait, total), rowId, pos_busyPct);
			diffData.setValueAt(pct(user,                   total), rowId, pos_userPct);
			diffData.setValueAt(pct(system,                 total), rowId, pos_sysPct);
			diffData.setValueAt(pct(iowait,                 total), rowId, pos_ioPct);
		}
	}

	private static BigDecimal pct(double part, double total)
	{
		if (total <= 0)
			return null;
		return new BigDecimal(100.0 * part / total).setScale(1, RoundingMode.HALF_EVEN);
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		// The average of the members (one row if this is not pureScale or DPF)
		int rows = this.getDiffRowCount();
		if (rows == 0)
			return;

		if (GRAPH_NAME_CPU.equals(tgdp.getName()))
		{
			Double[] arr = new Double[4];
			arr[0] = avg("CPU_BUSY_PCT",   true);
			arr[1] = avg("CPU_USER_PCT",   true);
			arr[2] = avg("CPU_SYSTEM_PCT", true);
			arr[3] = avg("CPU_IOWAIT_PCT", true);
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		if (GRAPH_NAME_LOAD_AVG.equals(tgdp.getName()))
		{
			Double[] arr = new Double[3];
			arr[0] = avg("CPU_LOAD_SHORT",  false);
			arr[1] = avg("CPU_LOAD_MEDIUM", false);
			arr[2] = avg("CPU_LOAD_LONG",   false);
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		if (GRAPH_NAME_MEMORY.equals(tgdp.getName()))
		{
			Double[] arr = new Double[2];
			arr[0] = avg("MEMORY_USED_PCT", false);
			arr[1] = avg("SWAP_USED_PCT",   false);
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}
	}

	/** Average over the rows (members), from the diff data (per sample) or the abs data */
	private Double avg(String colName, boolean fromDiff)
	{
		int    rows = fromDiff ? this.getDiffRowCount() : this.getAbsRowCount();
		double sum  = 0;
		int    cnt  = 0;
		for (int r = 0; r < rows; r++)
		{
			Double val = fromDiff ? this.getDiffValueAsDouble(r, colName) : this.getAbsValueAsDouble(r, colName);
			if (val != null)
			{
				sum += val;
				cnt++;
			}
		}
		return cnt == 0 ? null : sum / cnt;
	}
}
