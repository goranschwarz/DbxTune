/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
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
package com.asetune.cm.ase;

import java.sql.Connection;
import java.util.LinkedList;
import java.util.List;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.gui.MainFrame;
import com.asetune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmDeviceSegUsage
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmDeviceSegUsage.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmDeviceSegUsage.class.getSimpleName();
	public static final String   SHORT_NAME       = "Segment Usage";
	public static final String   HTML_DESC        = 
		"<html>" +
		"How much disk Usage on Segments level." +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_DISK;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

//	public static final long     NEED_SRV_VERSION = 15700;
//	public static final long     NEED_SRV_VERSION = 1570000;
	public static final long     NEED_SRV_VERSION = Ver.ver(16,0,0, 2); // 16.0 SP2
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"monDeviceSegmentUsage"};
	public static final String[] NEED_ROLES       = new String[] {"mon_role"};
	public static final String[] NEED_CONFIG      = new String[] {""};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"PagesUsed",
//		"Stranded",
		"_last_dummy_column_which_do_not_exists_in_resultset_"};

// Adaptive Server Enterprise/16.0 SP02 Beta 3/EBF 24693 SMP/P/x86_64/Enterprise Linux/asecorona/0/64-bit/OPT/Tue Jun  9 10:47:20 2015

//	RS> Col# Label         JDBC Type Name         Guessed DBMS type
//	RS> ---- ------------- ---------------------- -----------------
//	RS> 1    DBID          java.sql.Types.INTEGER int              
//	RS> 2    DeviceNumber  java.sql.Types.INTEGER int              
//	RS> 3    SegmentNumber java.sql.Types.INTEGER int              
//	RS> 4    PagesUsed     java.sql.Types.BIGINT  bigint           
//	RS> 5    Stranded      java.sql.Types.INTEGER int              
	

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;

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

		return new CmDeviceSegUsage(counterController, guiController);
	}

	public CmDeviceSegUsage(ICounterController counterController, IGuiController guiController)
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
	
	private void addTrendGraphs()
	{
	}

//	@Override
//	protected TabularCntrPanel createGui()
//	{
//		return new CmDeviceSegUsagePanel(this);
//	}

	@Override
	public String[] getDependsOnConfigForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		return NEED_CONFIG;
	}

	@Override
	public List<String> getPkForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("DBID");
		pkCols.add("DeviceNumber");
		pkCols.add("SegmentNumber");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		String sql = 
				"select \n  \n" + 
				"    dbname = db_name(DBID), \n" +  
				"    deviceName = d.name,  \n" + 
				"    segmentName = case  \n" + 
				"                    when s.SegmentNumber = 0 then 'system' \n" + 
				"                    when s.SegmentNumber = 1 then 'default' \n" + 
				"                    when s.SegmentNumber = 2 then 'logsegment' \n" + 
				"                    else 'use_defined_segment_' + convert(varchar(10),s.SegmentNumber) \n" + 
				"                  end, \n" + 
				"    s.*  \n" + 
				"from master.dbo.monDeviceSegmentUsage s, \n" +  
				"     master.dbo.sysdevices d  \n" + 
				"where s.DeviceNumber = d.vdevno  \n" + 
				"  and d.cntrltype = 0 \n";

		return sql;
	}
}
