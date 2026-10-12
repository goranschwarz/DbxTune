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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.config.dict.MonTablesDictionary;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.sql.conn.info.DbmsVersionInfoOracle;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmLongOps
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmLongOps.class.getSimpleName();
	public static final String   SHORT_NAME       = "Long Ops";
	public static final String   HTML_DESC        =
		"<html>" +
		"Long running operations that are going on right now, and how far they have come: full table scans, index builds, RMAN backups, statistics gathering...<br>" +
		"<br>" +
		"Oracle only lists an operation here if it takes more than (about) 6 seconds, and only for some types of operations.<br>" +
		"<code>PCT_DONE</code> and <code>TIME_REMAINING</code> (seconds) are Oracle's own estimates.<br>" +
		"<br>" +
		"<b>Note</b>: An operation that was interrupted (for example a query that was cancelled) stays in the list as 'not finished'.<br>" +
		"So operations that have not moved for a while are not shown.<br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {CM_NAME, "V$SESSION_LONGOPS"};
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

		return new CmLongOps(counterController, guiController);
	}

	public CmLongOps(ICounterController counterController, IGuiController guiController)
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

		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	public static final String  PROPKEY_sample_staleMinutes = CM_NAME + ".sample.staleMinutes";
	public static final int     DEFAULT_sample_staleMinutes = 10;

	@Override
	protected void registerDefaultValues()
	{
		super.registerDefaultValues();

		Configuration.registerDefaultValue(PROPKEY_sample_staleMinutes, DEFAULT_sample_staleMinutes);
	}

	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public List<CmSettingsHelper> getLocalSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		list.add(new CmSettingsHelper("Skip Operations not updated for ## Minutes", PROPKEY_sample_staleMinutes, Integer.class, conf.getIntProperty(PROPKEY_sample_staleMinutes, DEFAULT_sample_staleMinutes), DEFAULT_sample_staleMinutes, "Do not show operations that has not been updated for ## minutes. (an operation that was interrupted stays in V$SESSION_LONGOPS as 'not finished')" ));

		return list;
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("SID");
		pkCols.add("SERIAL#");
		pkCols.add("START_TIME");
		pkCols.add("OPNAME");

		return pkCols;
	}

	/** If we can't read the views: disable the CM, and tell what to do about it */
	@Override
	public boolean checkDependsOnOther(DbxConnection conn)
	{
		return OracleCmUtils.checkViewsAreReadable(this, conn, "V$SESSION_LONGOPS");
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		DbmsVersionInfoOracle oraVersionInfo = (DbmsVersionInfoOracle) versionInfo;

		// 11.2 and above
		String sql = getSql("CAST(0 as NUMBER(10))", "CAST(NULL as VARCHAR2(128))", "");

		// 12.1 and above, in a Container Database: which container the session is in
		if (versionInfo.getLongVersion() >= Ver.ver(12,1) && oraVersionInfo.isCdb())
		{
			String sqlCdb = getSql("l.CON_ID", "c.NAME", "      left join V$CONTAINERS c on c.CON_ID = l.CON_ID \n");

			// The SQL for 12.1 and above can NOT be tested on all versions: so "dry-run" it, and if it fails use the 11.2 SQL (which works on all versions)
			// conn is null when we just want to "view" the SQL (the CM Properties dialog)
			if (conn == null)
			{
				sql = sqlCdb;
			}
			else
			{
				try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery("select * from (" + sqlCdb + ") where 1=2"))
				{
					sql = sqlCdb;
				}
				catch (SQLException ex)
				{
					_logger.warn(CM_NAME + ": The SQL for a CDB (version " + Ver.versionNumToStr(versionInfo.getLongVersion()) + ") failed, the container name will not be shown. Caught: " + ex.getMessage().trim());
				}
			}
		}

		return sql;
	}

	private static String getSql(String CON_ID, String CON_NAME, String joinContainers)
	{
		int staleMinutes = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_sample_staleMinutes, DEFAULT_sample_staleMinutes);

		// Only what is NOT finished (SOFAR < TOTALWORK), and has been updated lately
		// ROW_NUM: there is no real key in V$SESSION_LONGOPS, so if two rows gets the same key (PK) we keep the last updated one
		// Note: CAST(... as NUMBER(5,1)) is needed to keep decimals (a NUMBER without precision is treated as a BIGINT)
		return ""
			+ "select \n"
			+ "     CON_ID \n"
			+ "    ,CON_NAME \n"
			+ "    ,SID \n"
			+ "    ,SERIAL# \n"
			+ "    ,USERNAME \n"
			+ "    ,OPNAME \n"
			+ "    ,TARGET \n"
			+ "    ,PCT_DONE \n"
			+ "    ,SOFAR \n"
			+ "    ,TOTALWORK \n"
			+ "    ,UNITS \n"
			+ "    ,START_TIME \n"
			+ "    ,LAST_UPDATE_TIME \n"
			+ "    ,ELAPSED_SECONDS \n"
			+ "    ,TIME_REMAINING \n"
			+ "    ,SQL_ID \n"
			+ "    ,MESSAGE \n"
			+ "from (select \n"
			+ "           " + CON_ID   + " as CON_ID \n"
			+ "          ," + CON_NAME + " as CON_NAME \n"
			+ "          ,l.SID \n"
			+ "          ,l.SERIAL# \n"
			+ "          ,l.USERNAME \n"
			+ "          ,l.OPNAME \n"
			+ "          ,l.TARGET \n"
			+ "          ,CAST(100 * l.SOFAR / NULLIF(l.TOTALWORK, 0) as NUMBER(5,1)) as PCT_DONE \n"
			+ "          ,l.SOFAR \n"
			+ "          ,l.TOTALWORK \n"
			+ "          ,l.UNITS \n"
			+ "          ,l.START_TIME \n"
			+ "          ,l.LAST_UPDATE_TIME \n"
			+ "          ,l.ELAPSED_SECONDS \n"
			+ "          ,l.TIME_REMAINING \n"
			+ "          ,l.SQL_ID \n"
			+ "          ,l.MESSAGE \n"
			+ "          ,row_number() over (partition by l.SID, l.SERIAL#, l.START_TIME, l.OPNAME order by l.LAST_UPDATE_TIME desc) as ROW_NUM \n"
			+ "      from V$SESSION_LONGOPS l \n"
			+ joinContainers
			+ "      where l.SOFAR < l.TOTALWORK \n"
			+ "        and l.LAST_UPDATE_TIME > SYSDATE - " + staleMinutes + " / 1440 \n"
			+ "     ) \n"
			+ "where ROW_NUM = 1 \n"
			+ "order by START_TIME \n"
			+ "";
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			mtd.addTable(CM_NAME, "Long running operations that are going on right now, and how far they have come.");

			mtd.addColumn(CM_NAME, "CON_ID",           "<html>Container ID. 0 = This is not a Container Database (or the version is below 12.1).</html>");
			mtd.addColumn(CM_NAME, "CON_NAME",         "<html>Name of the Container (PDB) the session is in. NULL if this is not a Container Database.</html>");
			mtd.addColumn(CM_NAME, "OPNAME",           "<html>What kind of operation it is, like: 'Table Scan', 'Sort Output', 'RMAN: full datafile backup', 'Gather Table Statistics'.</html>");
			mtd.addColumn(CM_NAME, "TARGET",           "<html>What the operation works on (for example the table that is scanned).</html>");
			mtd.addColumn(CM_NAME, "PCT_DONE",         "<html>How far the operation has come, in percent.<br><b>Formula</b>: 100 * SOFAR / TOTALWORK</html>");
			mtd.addColumn(CM_NAME, "SOFAR",            "<html>Work done so far, in the unit of column UNITS.</html>");
			mtd.addColumn(CM_NAME, "TOTALWORK",        "<html>Total work to do, in the unit of column UNITS. <b>Note</b>: This is an estimate, it can change while the operation runs.</html>");
			mtd.addColumn(CM_NAME, "UNITS",            "<html>Unit of SOFAR and TOTALWORK, for example: Blocks, Rows, Objects.</html>");
			mtd.addColumn(CM_NAME, "LAST_UPDATE_TIME", "<html>When Oracle last updated this row.</html>");
			mtd.addColumn(CM_NAME, "ELAPSED_SECONDS",  "<html>Seconds since the operation started.</html>");
			mtd.addColumn(CM_NAME, "TIME_REMAINING",   "<html>Oracle's estimate of how many seconds that are left.</html>");
			mtd.addColumn(CM_NAME, "MESSAGE",          "<html>Oracle's own summary of the row, as text.</html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e);
		}
	}
}
