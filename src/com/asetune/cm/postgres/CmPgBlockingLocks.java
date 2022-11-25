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
package com.asetune.cm.postgres;

import java.util.List;

import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.gui.MainFrame;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.conn.info.DbmsVersionInfo;
import com.asetune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmPgBlockingLocks
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmPgBlockingLocks.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmPgBlockingLocks.class.getSimpleName();
	public static final String   SHORT_NAME       = "Blocking Locks";
	public static final String   HTML_DESC        = 
		"<html>" +
		"Blocking locks." +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = Ver.ver(14);
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"pg_stat_activity"};
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
			guiController.splashWindowProgress("Loading: Counter Model '"+CM_NAME+"'");

		return new CmPgBlockingLocks(counterController, guiController);
	}

	public CmPgBlockingLocks(ICounterController counterController, IGuiController guiController)
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
	
//	@Override
//	protected TabularCntrPanel createGui()
//	{
//		return new CmActiveStatementsPanel(this);
//	}
	
	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return null;
//		List <String> pkCols = new LinkedList<String>();
//
////		pkCols.add("pk");
//
//		return pkCols;
	}

	@Override
	public boolean isRefreshable()
	{
		boolean superVal = super.isRefreshable();
		int blockingLockCount = CmSummary.getBlockingLockCount();
		
		// Can/should we set the reason why we should NOT refresh
		if (blockingLockCount <= 0)
		{
			if (getTabPanel() != null)
				getTabPanel().setWatermarkText("Skipping refresh of 'CmPgBlockingLocks'. CmSummary says there is NO blockings, so there is no need to refresh.");

			_logger.info("Skipping refresh of 'CmPgBlockingLocks'. CmSummary.getBlockingLockCount() == " + blockingLockCount);

			return false;
		}
		
		return superVal;
	}
	
	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		//-----------------------------------------------------------------------------------------------------------
		// base of this was grabbed from -- https://postgres.ai/blog/20211018-postgresql-lock-trees
		//-----------------------------------------------------------------------------------------------------------
		//
		// But I had to change: output data types for:
		//    ColumnName         From                                    To
		//    ------------------ --------------------------------------- -----------------------------------------
		//    - 'blocked_by'     java.sql.Types.ARRAY                    VARCHAR(###)
		//    - 'state'          java.sql.Types.VARCHAR (text)           VARCHAR(###)           
		//    - 'wait'           java.sql.Types.VARCHAR (text)           VARCHAR(###)
		//    - 'wait_age'       java.sql.Types.OTHER (interval)
		//    - 'tx_age'         java.sql.Types.OTHER (interval)
		//    - 'xid_age'        java.sql.Types.VARCHAR (text)           VARCHAR(###)
		//    - 'xmin_ttf'       java.sql.Types.VARCHAR (text)           VARCHAR(###)
		//    - 'datname'        java.sql.Types.VARCHAR (text)           VARCHAR(###)
		//    - 'usename'        java.sql.Types.VARCHAR (text)           VARCHAR(###)
		//    - 'query'          java.sql.Types.VARCHAR (text)           VARCHAR(###)

		String sql = ""
			    + "with recursive activity as ( \n"
			    + "  select \n"
			    + "    pg_blocking_pids(pid) blocked_by, \n"
			    + "    *, \n"
			    + "    age(clock_timestamp(), xact_start)::interval(0) as tx_age, \n"
//			    + "    -- \"pg_locks.waitstart\" – PG14+ only; for older versions:  age(clock_timestamp(), state_change) as wait_age \n"
			    + "    age(clock_timestamp(), (select max(l.waitstart) from pg_locks l where a.pid = l.pid))::interval(0) as wait_age \n"
			    + "  from pg_stat_activity a \n"
			    + "  where state is distinct from 'idle' \n"
			    + "), blockers as ( \n"
			    + "  select \n"
			    + "    array_agg(distinct c order by c) as pids \n"
			    + "  from ( \n"
			    + "    select unnest(blocked_by) \n"
			    + "    from activity \n"
			    + "  ) as dt(c) \n"
			    + "), tree as ( \n"
			    + "  select \n"
			    + "    activity.*, \n"
			    + "    1 as level, \n"
			    + "    activity.pid as top_blocker_pid, \n"
			    + "    array[activity.pid] as path, \n"
			    + "    array[activity.pid]::int[] as all_blockers_above \n"
			    + "  from activity, blockers \n"
			    + "  where \n"
			    + "    array[pid] <@ blockers.pids \n"
			    + "    and blocked_by = '{}'::int[] \n"
			    + "  union all \n"
			    + "  select \n"
			    + "    activity.*, \n"
			    + "    tree.level + 1 as level, \n"
			    + "    tree.top_blocker_pid, \n"
			    + "    path || array[activity.pid] as path, \n"
			    + "    tree.all_blockers_above || array_agg(activity.pid) over () as all_blockers_above \n"
			    + "  from activity, tree \n"
			    + "  where \n"
			    + "    not array[activity.pid] <@ tree.all_blockers_above \n"
			    + "    and activity.blocked_by <> '{}'::int[] \n"
			    + "    and activity.blocked_by <@ tree.all_blockers_above \n"
			    + ") \n"
			    + "select \n"
			    + "  pid, \n"
			    + "  CAST(blocked_by as varchar(255)) AS blocked_by, \n"
			    + "  CAST(case when wait_event_type <> 'Lock' then replace(state, 'idle in transaction', 'idletx') else 'waiting' end as varchar(30)) AS state, \n"
			    + "  CAST(wait_event_type || ':' || wait_event as varchar(60)) AS wait, \n"
			    + "  CAST(wait_age as varchar(30)) AS wait_age, \n"
			    + "  CAST(tx_age as varchar(30)) AS tx_age, \n"
			    + "  CAST(to_char(age(backend_xid), 'FM999,999,999,990') as varchar(30)) AS xid_age, \n"
			    + "  CAST(to_char(2147483647 - age(backend_xmin), 'FM999,999,999,990') as varchar(30)) AS xmin_ttf, \n"
			    + "  CAST(datname as varchar(30)) AS datname, \n"
			    + "  CAST(usename as varchar(30)) AS usename, \n"
			    + "  (select count(distinct t1.pid) from tree t1 where array[tree.pid] <@ t1.path and t1.pid <> tree.pid) as blkd, \n"
			    + "  format( \n"
			    + "    '%s %s%s', \n"
			    + "    lpad('[' || pid::text || ']', 9, ' '), \n"
			    + "    repeat('.', level - 1) || case when level > 1 then ' ' end, \n"
			    + "    left(query, 1000) \n"
			    + "  ) as query \n"
			    + "from tree \n"
			    + "order by top_blocker_pid, level, pid \n"
			    + "";
		
		return sql;
	}

	private void addTrendGraphs()
	{
	}
}
