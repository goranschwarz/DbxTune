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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.postgres.gui.CmPgPidWaitPanel;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.pcs.PcsColumnOptions;
import com.asetune.pcs.PcsColumnOptions.ColumnType;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.conn.info.DbmsVersionInfo;
import com.asetune.utils.Configuration;
import com.asetune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmPgPidWait
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmPgPidWait.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmPgPidWait.class.getSimpleName();
	public static final String   SHORT_NAME       = "PID Wait";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<p>What are the PID's waiting for</p>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = Ver.ver(9, 6);
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"pg_wait_sampling_profile", "pg_stat_activity"};
	public static final String[] NEED_ROLES       = new String[] {};//{"VIEW SERVER STATE"};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
			"wait_count",
			"est_wait_time_ms"
			};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = false;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;;

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

		return new CmPgPidWait(counterController, guiController);
	}

	public CmPgPidWait(ICounterController counterController, IGuiController guiController)
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

		setCounterController(counterController);
		setGuiController(guiController);
		
//		addTrendGraphs();
		
		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	// pg_wait_sampling configuration
	private int  _pgWaitSampling_profilePeriod = -1;
	
	private static final String  PROP_PREFIX               = CM_NAME;

	public static final String  PROPKEY_show_systemThreads = PROP_PREFIX + ".sample.systemThreads";
	public static final boolean DEFAULT_show_systemThreads = true;

	public static final String  PROPKEY_show_clientRead    = PROP_PREFIX + ".sample.clientRead";
	public static final boolean DEFAULT_show_clientRead    = false;

	public static final int COLPOS_event   = 3; // NOTE: if the SQL is changed you also need to change THIS POD
	public static final int COLPOS_datname = 7; // NOTE: if the SQL is changed you also need to change THIS POD

	@Override
	protected void registerDefaultValues()
	{
		super.registerDefaultValues();

		Configuration.registerDefaultValue(PROPKEY_show_systemThreads, DEFAULT_show_systemThreads);
		Configuration.registerDefaultValue(PROPKEY_show_clientRead   , DEFAULT_show_clientRead);
	}
	
	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public List<CmSettingsHelper> getLocalSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		
		list.add(new CmSettingsHelper("Show System PID's", PROPKEY_show_systemThreads , Boolean.class, conf.getBooleanProperty(PROPKEY_show_systemThreads  , DEFAULT_show_systemThreads  ), DEFAULT_show_systemThreads, CmPgPidWaitPanel.TOOLTIP_show_systemThreads ));
		list.add(new CmSettingsHelper("Show ClientRead"  , PROPKEY_show_clientRead    , Boolean.class, conf.getBooleanProperty(PROPKEY_show_clientRead     , DEFAULT_show_clientRead     ), DEFAULT_show_clientRead   , CmPgPidWaitPanel.TOOLTIP_show_clientRead ));

		return list;
	}


	
	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmPgPidWaitPanel(this);
	}

	// PCS -- DICTIONARY Compression for column 'query'
	@Override
	public Map<String, PcsColumnOptions> getPcsColumnOptions()
	{
		Map<String, PcsColumnOptions> map = super.getPcsColumnOptions();

		// No settings in the super, create one, and set it at super
		if (map == null)
		{
			map = new HashMap<>();
			map.put("query", new PcsColumnOptions(ColumnType.DICTIONARY_COMPRESSION));

			// Set the map in the super
			setPcsColumnOptions(map);
		}

		return map;
	}
	
	/**
	 * Called before <code>refreshGetData(conn)</code> where we can make various checks
	 * <p>
	 * Note: this is a special case since Company X is recreating the Postgres Server 
	 *       (every now and then) during the day/night...
	 *       We need to check/create the extension before polling data from it!
	 */
	@Override
	public boolean beforeRefreshGetData(DbxConnection conn) throws Exception
	{
		return PostgresCmHelper.pgWaitSampling_beforeRefreshGetData(this, conn);
	}

	@Override
	public boolean checkDependsOnOther(DbxConnection conn)
	{
		return PostgresCmHelper.pgWaitSampling_checkDependsOnOther(this, conn);
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("pid");
		pkCols.add("event_type");
		pkCols.add("event");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		int tmp_pgWaitSampling_profilePeriod = _pgWaitSampling_profilePeriod;
		if (conn == null)
			tmp_pgWaitSampling_profilePeriod = 10;

		// Get Configuration for 'pg_wait_sampling.profile_period'
		if (tmp_pgWaitSampling_profilePeriod < 0)
		{
			// Get config from pg_wait_sampling
			String sql = "select cast(setting as int) as setting from pg_settings where name = 'pg_wait_sampling.profile_period'";
			try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
			{
				while(rs.next())
				{
					_pgWaitSampling_profilePeriod = rs.getInt(1);
					tmp_pgWaitSampling_profilePeriod = _pgWaitSampling_profilePeriod;
					_logger.info(CM_NAME + ": Configuration for 'pg_wait_sampling.profile_period' is set to: " + _pgWaitSampling_profilePeriod);
				}
			}
			catch (SQLException ex)
			{
				_pgWaitSampling_profilePeriod = 10;
				tmp_pgWaitSampling_profilePeriod = _pgWaitSampling_profilePeriod;
				_logger.error("Problems getting configuration for 'pg_wait_sampling.profile_period', setting the value to '10'. continuing. SQL=|" + sql + "|.", ex);
			}
		}

		String orderBy = "ORDER BY wsp.pid, wsp.event_type, wsp.event \n";
		String leader_pid_cte = "";
		String leader_pid_sel = "";
		if (versionInfo.getLongVersion() >= Ver.ver(13))
		{
			leader_pid_cte = "        ,leader_pid \n";
			leader_pid_sel = "    sa.leader_pid, \n";
			orderBy = "ORDER BY sa.leader_pid, wsp.pid, wsp.event_type, wsp.event \n";
		//	orderBy = "ORDER BY COALESCE(a.leader_pid, a.pid), a.backend_start \n"; 
		}

		String sql = ""
			    + "WITH wsp AS \n"
			    + "( \n"
			    + "    SELECT \n"
			    + "         pid \n"
			    + "        ,CAST(event_type as varchar(128)) AS event_type \n"
			    + "        ,CAST(event      as varchar(128)) AS event \n"
			    + "        ,CAST(SUM(count) as BIGINT)       AS wait_count \n"
			    + "        ,CAST(SUM(count) as BIGINT) * " + tmp_pgWaitSampling_profilePeriod + " AS est_wait_time_ms /* pg_wait_sampling.profile_period = " + _pgWaitSampling_profilePeriod + " */\n"
			    + "    FROM pg_wait_sampling_profile \n"
			    + "    WHERE 1 = 1 \n"
//			    + "      AND event_type != 'Activity' \n"   // DBMS process waiting for activity in its main processing loop
//			    + "      AND event      != 'ClientRead' \n" // Server is waiting for Commands from Client
//			    + "      AND event      != 'CheckpointWriteDelay' \n" 
			    + "    GROUP BY pid, event_type, event \n"
			    + ") \n"
			    + ", sa AS \n"
			    + "( \n"
			    + "    SELECT \n"
			    + "         pid \n"
			    + leader_pid_cte
			    + "        ,CAST(datname          as varchar(128)) AS datname \n"
			    + "        ,CAST(usename          as varchar(128)) AS usename \n"
			    + "        ,CAST(application_name as varchar(128)) AS application_name \n"
			    + "        ,CAST(backend_type     as varchar(128)) AS backend_type \n"
			    + "        ,CAST(state            as varchar(128)) AS state \n"
				+ "        ,CAST( CASE WHEN state = 'active' \n"
				+ "                    THEN clock_timestamp() - query_start /* active -- query-elapsed-time */ \n"
				+ "                    ELSE state_change      - query_start /* else   -- last-exec-time*/ \n"
				+ "               END as varchar(30)) AS stmnt_last_exec_age \n"
			    + "        ,query \n"
			    + "    FROM pg_stat_activity \n"
			    + ") \n"
			    + "SELECT \n"
			    + leader_pid_sel
			    + "     wsp.pid \n"
			    + "    ,wsp.event_type \n"
			    + "    ,wsp.event \n"
			    + "    ,wsp.wait_count \n"
			    + "    ,wsp.est_wait_time_ms \n"
			    + " \n"
			    + "    ,CAST('-------' as varchar(9)) AS delimiter \n"
			    + "    ,sa.datname \n"
			    + "    ,sa.usename \n"
			    + "    ,sa.application_name \n"
			    + "    ,sa.backend_type \n"
			    + "    ,sa.state \n"
			    + "    ,sa.stmnt_last_exec_age \n"
			    + "    ,sa.query \n"
			    + "FROM wsp \n"
			    + "INNER JOIN sa ON wsp.pid = sa.pid \n"
			    + orderBy
			    + "";
		
		
		return sql;
	}
}
