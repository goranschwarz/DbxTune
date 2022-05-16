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
package com.asetune.cm.db2;

import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.gui.MainFrame;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.conn.info.DbmsVersionInfo;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */

public class CmSystemResourses
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmAdminWhoSqm.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmSystemResourses.class.getSimpleName();
	public static final String   SHORT_NAME       = "System Resources";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<h4>System Resources</h4>" + 
		"Displays various System Resources." +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

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
			guiController.splashWindowProgress("Loading: Counter Model '"+CM_NAME+"'");

		return new CmSystemResourses(counterController, guiController);
	}

	public CmSystemResourses(ICounterController counterController, IGuiController guiController)
	{
		super(counterController,
				CM_NAME, GROUP_NAME, /*sql*/null, /*pkList*/null, 
				DIFF_COLUMNS, PCT_COLUMNS, MON_TABLES, 
				NEED_ROLES, NEED_CONFIG, NEED_SRV_VERSION, NEED_CE_VERSION, 
				NEGATIVE_DIFF_COUNTERS_TO_ZERO, IS_SYSTEM_CM, DEFAULT_POSTPONE_TIME);

		setDisplayName(SHORT_NAME);
		setDescription(HTML_DESC);

		setIconFile(GUI_ICON_FILE);

		setShowClearTime(false);
//		setBackgroundDataPollingEnabled(true, false);
		
		setCounterController(counterController);
		setGuiController(guiController);
		
		addTrendGraphs();
		
		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	
	private void addTrendGraphs()
	{
	}

//	@Override
//	protected TabularCntrPanel createGui()
//	{
//		return new CmRaSysmonPanel(this);
//	}

	@Override
	public String[] getDependsOnConfigForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return NEED_CONFIG;
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addTable("ENV_GET_SYSTEM_RESOURCES",  "The ENV_GET_SYSTEM_RESOURCES table function returns operating system, CPU, memory, and other information that is related to members on the system.");

			mtd.addColumn("ENV_GET_SYSTEM_RESOURCES", "MEMBER"                ,    "<html>member - Database member monitor element</html>");
			mtd.addColumn("ENV_GET_SYSTEM_RESOURCES", "OS_NAME"               ,    "<html>os_name - Operating system name monitor element</html>");
			mtd.addColumn("ENV_GET_SYSTEM_RESOURCES", "HOST_NAME"             ,    "<html>host_name - Host name monitor element</html>");
			mtd.addColumn("ENV_GET_SYSTEM_RESOURCES", "OS_VERSION"            ,    "<html>os_version - Operating system version monitor element</html>");
			mtd.addColumn("ENV_GET_SYSTEM_RESOURCES", "OS_RELEASE"            ,    "<html>os_release - Operating system release monitor element</html>");
			mtd.addColumn("ENV_GET_SYSTEM_RESOURCES", "MACHINE_IDENTIFICATION",    "<html>machine_identification - Host hardware identification monitor element</html>");
			mtd.addColumn("ENV_GET_SYSTEM_RESOURCES", "OS_LEVEL"              ,    "<html>os_level - Operating system level monitor element</html>");
			mtd.addColumn("ENV_GET_SYSTEM_RESOURCES", "CPU_TOTAL"             ,    "<html>cpu_total - Number of CPUs monitor element</html>");
			mtd.addColumn("ENV_GET_SYSTEM_RESOURCES", "CPU_ONLINE"            ,    "<html>cpu_online - Number of CPUs online monitor element</html>");
			mtd.addColumn("ENV_GET_SYSTEM_RESOURCES", "CPU_CONFIGURED"        ,    "<html>cpu_configured - Number of configured CPUs monitor element</html>");
			mtd.addColumn("ENV_GET_SYSTEM_RESOURCES", "CPU_SPEED"             ,    "<html>cpu_speed - CPU clock speed monitor element</html>");
			mtd.addColumn("ENV_GET_SYSTEM_RESOURCES", "CPU_TIMEBASE"          ,    "<html>cpu_timebase - Frequency of timebase register increment monitor element</html>");
			mtd.addColumn("ENV_GET_SYSTEM_RESOURCES", "CPU_HMT_DEGREE"        ,    "<html>cpu_hmt_degree - Number of logical CPUs monitor element</html>");
			mtd.addColumn("ENV_GET_SYSTEM_RESOURCES", "CPU_CORES_PER_SOCKET"  ,    "<html>cpu_cores_per_socket - Number of CPU cores per socket monitor element</html>");
			mtd.addColumn("ENV_GET_SYSTEM_RESOURCES", "MEMORY_TOTAL"          ,    "<html>memory_total - Total physical memory monitor element</html>");
			mtd.addColumn("ENV_GET_SYSTEM_RESOURCES", "MEMORY_FREE"           ,    "<html>memory_free - Amount of free physical memory monitor element</html>");
			mtd.addColumn("ENV_GET_SYSTEM_RESOURCES", "MEMORY_SWAP_TOTAL"     ,    "<html>memory_swap_total - Total swap space monitor element</html>");
			mtd.addColumn("ENV_GET_SYSTEM_RESOURCES", "MEMORY_SWAP_FREE"      ,    "<html>memory_swap_free - Total free swap space monitor element</html>");
			mtd.addColumn("ENV_GET_SYSTEM_RESOURCES", "VIRTUAL_MEM_TOTAL"     ,    "<html>virtual_mem_total - Total virtual memory monitor element</html>");
			mtd.addColumn("ENV_GET_SYSTEM_RESOURCES", "VIRTUAL_MEM_RESERVED"  ,    "<html>virtual_mem_reserved - Reserved virtual memory monitor element</html>");
			mtd.addColumn("ENV_GET_SYSTEM_RESOURCES", "VIRTUAL_MEM_FREE"      ,    "<html>virtual_mem_free - Free virtual memory monitor element</html>");
			mtd.addColumn("ENV_GET_SYSTEM_RESOURCES", "CPU_LOAD_SHORT"        ,    "<html>cpu_load_short - Processor load (short timeframe) monitor element</html>");
			mtd.addColumn("ENV_GET_SYSTEM_RESOURCES", "CPU_LOAD_MEDIUM"       ,    "<html>cpu_load_medium - Processor load (medium timeframe) monitor element</html>");
			mtd.addColumn("ENV_GET_SYSTEM_RESOURCES", "CPU_LOAD_LONG"         ,    "<html>cpu_load_long - Processor load (long timeframe) monitor element</html>");
			mtd.addColumn("ENV_GET_SYSTEM_RESOURCES", "CPU_USAGE_TOTAL"       ,    "<html>cpu_usage_total - Processor usage monitor element</html>");
			mtd.addColumn("ENV_GET_SYSTEM_RESOURCES", "CPU_USER"              ,    "<html>cpu_user - Non-kernel processing time monitor element</html>");
			mtd.addColumn("ENV_GET_SYSTEM_RESOURCES", "CPU_IDLE"              ,    "<html>cpu_idle - Processor idle time monitor element</html>");
			mtd.addColumn("ENV_GET_SYSTEM_RESOURCES", "CPU_IOWAIT"            ,    "<html>cpu_iowait - IO Wait time monitor element</html>");
			mtd.addColumn("ENV_GET_SYSTEM_RESOURCES", "CPU_SYSTEM"            ,    "<html>cpu_system - Kernel time monitor element</html>");
			mtd.addColumn("ENV_GET_SYSTEM_RESOURCES", "SWAP_PAGE_SIZE"        ,    "<html>swap_page_size - Swap page size monitor element</html>");
			mtd.addColumn("ENV_GET_SYSTEM_RESOURCES", "SWAP_PAGES_IN"         ,    "<html>swap_pages_in - Pages swapped in from disk monitor element</html>");
			mtd.addColumn("ENV_GET_SYSTEM_RESOURCES", "SWAP_PAGES_OUT"        ,    "<html>swap_pages_out - Pages swapped out to disk monitor element</html>");
			mtd.addColumn("ENV_GET_SYSTEM_RESOURCES", "OS_FULL_VERSION"       ,    "<html>os_full_version - Operating system full version monitor element</html>");
			mtd.addColumn("ENV_GET_SYSTEM_RESOURCES", "OS_KERNEL_VERSION"     ,    "<html>os_kernel_version - Operating system kernel identifier monitor element</html>");
			mtd.addColumn("ENV_GET_SYSTEM_RESOURCES", "OS_ARCH_TYPE"          ,    "<html>os_arch_type - Operating system architecture type monitor element</html>");
		}
		catch (NameNotFoundException e) {/*ignore*/}
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
		String sql = "SELECT * FROM TABLE(SYSPROC.ENV_GET_SYSTEM_RESOURCES())";

		return sql;
	}
}
