/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
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
package com.dbxtune.cm.ase;

import java.util.LinkedList;
import java.util.List;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmRaSyncTaskStats
extends CountersModel
{
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmRaSyncTaskStats.class.getSimpleName();
	public static final String   SHORT_NAME       = "Sync Task Stat";
	public static final String   HTML_DESC        = 
		"<html>" +
		"FIXME." +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_REP_AGENT;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

//	public static final long     NEED_SRV_VERSION = 1600000;
	public static final long     NEED_SRV_VERSION = Ver.ver(16,0);
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"monRepSyncTaskStatistics"};
	public static final String[] NEED_ROLES       = new String[] {"mon_role"};
	public static final String[] NEED_CONFIG      = new String[] {"enable rep agent threads"};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"NumberOfTaskWakeups           ", 
		"NumberTimerTaskWakeups        ", 
		"TotalSleepTime                ", 
//		"LongestSleepTime              ", 
//		"AverageSleepTime              ", 
//		"MaxTasksInSleepQueue          ", 
		"TotalSyncCommitTime           ", 
		"TotalFlushTime                ", 
//		"AverageFlushTime              ", 
		"NumberOfCommits               ", 
		"NumberOfSleeps                ", 
		"CommitToRAProcessTime         ", 
//		"AverageCommitToRAProcessTime  ", 
//		"LongestCommitToRAProcessTime  ", 
		"RAProcessToPkgFlushTime       ", 
//		"AverageRAProcessToPkgFlushTime", 
//		"LongestRAProcessToPkgFlushTime", 
		"PkgFlushToAckTime             ", 
//		"AveragePkgFlushToAckTime      ", 
//		"LongestPkgFlushToAckTime      ", 
		"AckToScheduleTime             ", 
//		"AverageAckToScheduleTime      ", 
//		"LongestAckToScheduleTime      ", 
		"_last_dummy_column_which_do_not_exists_in_resultset_" 
		};

// Adaptive Server Enterprise/16.0 SP02 Beta 3/EBF 24693 SMP/P/x86_64/Enterprise Linux/asecorona/0/64-bit/OPT/Tue Jun  9 10:47:20 2015
	
//	RS> Col# Label                          JDBC Type Name         Guessed DBMS type
//	RS> ---- ------------------------------ ---------------------- -----------------
//	RS> 1    DBID                           java.sql.Types.INTEGER int              
//	RS> 2    SPID                           java.sql.Types.INTEGER int              
//	RS> 3    InstanceID                     java.sql.Types.TINYINT tinyint          
//	RS> 4    NumberOfTaskWakeups            java.sql.Types.BIGINT  bigint           
//	RS> 5    NumberTimerTaskWakeups         java.sql.Types.BIGINT  bigint           
//	RS> 6    TotalSleepTime                 java.sql.Types.BIGINT  bigint           
//	RS> 7    LongestSleepTime               java.sql.Types.BIGINT  bigint           
//	RS> 8    AverageSleepTime               java.sql.Types.BIGINT  bigint           
//	RS> 9    MaxTasksInSleepQueue           java.sql.Types.BIGINT  bigint           
//	RS> 10   TotalSyncCommitTime            java.sql.Types.BIGINT  bigint           
//	RS> 11   TotalFlushTime                 java.sql.Types.BIGINT  bigint           
//	RS> 12   AverageFlushTime               java.sql.Types.BIGINT  bigint           
//	RS> 13   NumberOfCommits                java.sql.Types.BIGINT  bigint           
//	RS> 14   NumberOfSleeps                 java.sql.Types.BIGINT  bigint           
//	RS> 15   CommitToRAProcessTime          java.sql.Types.BIGINT  bigint           
//	RS> 16   AverageCommitToRAProcessTime   java.sql.Types.BIGINT  bigint           
//	RS> 17   LongestCommitToRAProcessTime   java.sql.Types.BIGINT  bigint           
//	RS> 18   RAProcessToPkgFlushTime        java.sql.Types.BIGINT  bigint           
//	RS> 19   AverageRAProcessToPkgFlushTime java.sql.Types.BIGINT  bigint           
//	RS> 20   LongestRAProcessToPkgFlushTime java.sql.Types.BIGINT  bigint           
//	RS> 21   PkgFlushToAckTime              java.sql.Types.BIGINT  bigint           
//	RS> 22   AveragePkgFlushToAckTime       java.sql.Types.BIGINT  bigint           
//	RS> 23   LongestPkgFlushToAckTime       java.sql.Types.BIGINT  bigint           
//	RS> 24   AckToScheduleTime              java.sql.Types.BIGINT  bigint           
//	RS> 25   AverageAckToScheduleTime       java.sql.Types.BIGINT  bigint           
//	RS> 26   LongestAckToScheduleTime       java.sql.Types.BIGINT  bigint           
//	RS> 27   DBName                         java.sql.Types.VARCHAR varchar(30)      
	
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

		return new CmRaSyncTaskStats(counterController, guiController);
	}

	public CmRaSyncTaskStats(ICounterController counterController, IGuiController guiController)
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
//		return new CmRepSyncTaskStatisticsPanel(this);
//	}

	@Override
	public String[] getDependsOnConfigForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return NEED_CONFIG;
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("DBID");
		pkCols.add("SPID");
		pkCols.add("InstanceID");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		String sql = "select * from master..monRepSyncTaskStatistics";
		return sql;
	}
}
