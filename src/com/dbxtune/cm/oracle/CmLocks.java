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
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.config.dict.MonTablesDictionary;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.sql.conn.info.DbmsVersionInfoOracle;
import com.dbxtune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmLocks
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmLocks.class.getSimpleName();
	public static final String   SHORT_NAME       = "Locks";
	public static final String   HTML_DESC        =
		"<html>" +
		"The locks of the sessions that are part of a blocking situation right now: the sessions that wait for a lock, and the sessions they wait for.<br>" +
		"This is empty when no session is blocked.<br>" +
		"<br>" +
		"One row is one lock that a session holds or waits for (column <code>LOCK_STATE</code>):" +
		"<ul>" +
		"  <li><b>WAITING</b>  - the session wants this lock, and has to wait for it. <code>BLOCKED_BY_SID</code> is the session it waits for.</li>" +
		"  <li><b>BLOCKING</b> - the session holds this lock, and at least one other session waits for it.</li>" +
		"  <li><b>HOLDING</b>  - the session holds this lock, and nobody waits for it.</li>" +
		"</ul>" +
		"The most common lock types: <b>TX</b> = a transaction (a row lock is a wait for the transaction that changed the row), <b>TM</b> = a table (taken by DML, and by DDL).<br>" +
		"<code>OBJECT_NAME</code> is the table for a TM lock, and for a session that waits: the table of the row it waits for.<br>" +
		"<br>" +
		"<b>Note</b>: The alarms for blocking locks are in the tabs 'Summary' and 'Active Statements', and the number of blocked sessions over time is a graph in 'Summary'.<br>" +
		"<b>Note</b>: In a RAC, this only shows the locks of the instance we are connected to.<br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {CM_NAME, "V$LOCK", "V$SESSION"};
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

		return new CmLocks(counterController, guiController);
	}

	public CmLocks(ICounterController counterController, IGuiController guiController)
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

		// No graph here: this CM is empty when nobody is blocked (so a graph would only get values now and then).
		// The number of blocked sessions over time is in the Summary graph 'BlockingLocksGraph'.

		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("SID");
		pkCols.add("TYPE");
		pkCols.add("ID1");
		pkCols.add("ID2");

		return pkCols;
	}

	/** If we can't read the views: disable the CM, and tell what to do about it */
	@Override
	public boolean checkDependsOnOther(DbxConnection conn)
	{
		return OracleCmUtils.checkViewsAreReadable(this, conn, "V$LOCK", "V$SESSION", "V$LOCK_TYPE", "DBA_OBJECTS");
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		DbmsVersionInfoOracle oraVersionInfo = (DbmsVersionInfoOracle) versionInfo;

		// 11.2 and above
		String sql = getSql(
				"CAST(0 as NUMBER(10))",        // CON_ID
				"CAST(NULL as VARCHAR2(128))",  // CON_NAME
				"",                             // join V$CONTAINERS
				"DBA_OBJECTS o where o.OBJECT_ID = x.OBJECT_ID");

		// 12.1 and above, in a Container Database: the same object id can exist in several containers
		if (versionInfo.getLongVersion() >= Ver.ver(12,1) && oraVersionInfo.isCdb())
		{
			String sqlCdb = getSql(
					"s.CON_ID",
					"c.NAME",
					"left join V$CONTAINERS c on c.CON_ID = s.CON_ID \n",
					"CDB_OBJECTS o where o.OBJECT_ID = x.OBJECT_ID and o.CON_ID = x.CON_ID");

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
					_logger.warn(CM_NAME + ": The SQL for a CDB (version " + Ver.versionNumToStr(versionInfo.getLongVersion()) + ") failed, the container name will not be shown (and object names only for the container we are connected to). Caught: " + ex.getMessage().trim());
				}
			}
		}

		return sql;
	}

	private static String getSql(String CON_ID, String CON_NAME, String joinContainers, String objectsFromWhere)
	{
		// V$LOCK is expensive to read (even when it's small), so:
		//   - "exists (a blocked session)" is checked first: when nobody is blocked V$LOCK is not read at all
		//   - only the sessions in a blocking chain: the ones that wait (BLOCKING_SESSION is set), and the ones they wait for
		// 'AE' (edition lock) is skipped: every session has one, and it never blocks anybody
		// OBJECT_ID: for a TM lock it's ID1, for a lock we wait for it's the object of the row the session waits for (ROW_WAIT_OBJ#)
		String lockMode = "0, '-', 1, 'Null', 2, 'Row Share', 3, 'Row Exclusive', 4, 'Share', 5, 'Share Row Exclusive', 6, 'Exclusive'";

		return ""
			+ "select \n"
			+ "     x.CON_ID \n"
			+ "    ,x.CON_NAME \n"
			+ "    ,x.SID \n"
			+ "    ,x.SERIAL# \n"
			+ "    ,x.USERNAME \n"
			+ "    ,x.PROGRAM \n"
			+ "    ,x.MACHINE \n"
			+ "    ,x.LOCK_STATE \n"
			+ "    ,x.TYPE \n"
			+ "    ,x.TYPE_NAME \n"
			+ "    ,x.MODE_HELD \n"
			+ "    ,x.MODE_REQUESTED \n"
			+ "    ,x.LOCK_SECONDS \n"
			+ "    ,x.BLOCKED_BY_SID \n"
			+ "    ,(select max(o.OWNER || '.' || o.OBJECT_NAME) from " + objectsFromWhere + ") as OBJECT_NAME \n"
			+ "    ,x.EVENT \n"
			+ "    ,x.SQL_ID \n"
			+ "    ,x.ID1 \n"
			+ "    ,x.ID2 \n"
			+ "from (select \n"
			+ "           " + CON_ID   + " as CON_ID \n"
			+ "          ," + CON_NAME + " as CON_NAME \n"
			+ "          ,l.SID \n"
			+ "          ,s.SERIAL# \n"
			+ "          ,s.USERNAME \n"
			+ "          ,s.PROGRAM \n"
			+ "          ,s.MACHINE \n"
			+ "          ,CAST(case when l.REQUEST > 0 then 'WAITING' when l.BLOCK > 0 then 'BLOCKING' else 'HOLDING' end as VARCHAR2(10)) as LOCK_STATE \n"
			+ "          ,l.TYPE \n"
			+ "          ,t.NAME                                                                    as TYPE_NAME \n"
			+ "          ,CAST(decode(l.LMODE,   " + lockMode + ", TO_CHAR(l.LMODE))   as VARCHAR2(20)) as MODE_HELD \n"
			+ "          ,CAST(decode(l.REQUEST, " + lockMode + ", TO_CHAR(l.REQUEST)) as VARCHAR2(20)) as MODE_REQUESTED \n"
			+ "          ,l.CTIME                                                                   as LOCK_SECONDS \n"
			+ "          ,s.BLOCKING_SESSION                                                        as BLOCKED_BY_SID \n"
			+ "          ,case when l.TYPE = 'TM' then l.ID1 when l.REQUEST > 0 and s.ROW_WAIT_OBJ# > 0 then s.ROW_WAIT_OBJ# end as OBJECT_ID \n"
			+ "          ,s.EVENT \n"
			+ "          ,s.SQL_ID \n"
			+ "          ,l.ID1 \n"
			+ "          ,l.ID2 \n"
			+ "      from V$LOCK l \n"
			+ "      join V$SESSION s on s.SID = l.SID \n"
			+ "      " + joinContainers
			+ "      left join V$LOCK_TYPE t on t.TYPE = l.TYPE \n"
			+ "      where exists (select 1 from V$SESSION b where b.BLOCKING_SESSION is not null) \n"
			+ "        and l.TYPE <> 'AE' \n"
			+ "        and l.SID in (select w.SID              from V$SESSION w where w.BLOCKING_SESSION is not null \n"
			+ "                      union \n"
			+ "                      select w.BLOCKING_SESSION from V$SESSION w where w.BLOCKING_SESSION is not null) \n"
			+ "     ) x \n"
			+ "order by x.LOCK_SECONDS desc, x.SID \n"
			+ "";
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			mtd.addTable(CM_NAME, "The locks of the sessions that are part of a blocking situation right now.");

			mtd.addColumn(CM_NAME, "CON_ID",         "<html>Container ID. 0 = This is not a Container Database (or the version is below 12.1).</html>");
			mtd.addColumn(CM_NAME, "CON_NAME",       "<html>Name of the Container (PDB) the session is in. NULL if this is not a Container Database.</html>");
			mtd.addColumn(CM_NAME, "LOCK_STATE",     "<html><b>WAITING</b> = the session wants this lock, and has to wait for it.<br><b>BLOCKING</b> = the session holds this lock, and at least one other session waits for it.<br><b>HOLDING</b> = the session holds this lock, and nobody waits for it.</html>");
			mtd.addColumn(CM_NAME, "TYPE",           "<html>Lock type, the most common: <b>TX</b> = transaction (row locks), <b>TM</b> = table (DML), <b>UL</b> = user defined (DBMS_LOCK).</html>");
			mtd.addColumn(CM_NAME, "TYPE_NAME",      "<html>Name of the lock type (from V$LOCK_TYPE).</html>");
			mtd.addColumn(CM_NAME, "MODE_HELD",      "<html>The mode the session holds the lock in. '-' = it does not hold it (it waits for it).<br>For a TM (table) lock: 'Row Exclusive' is what normal DML takes, 'Exclusive' is taken by DDL and LOCK TABLE.</html>");
			mtd.addColumn(CM_NAME, "MODE_REQUESTED", "<html>The mode the session waits to get the lock in. '-' = it does not wait for it.</html>");
			mtd.addColumn(CM_NAME, "LOCK_SECONDS",   "<html>Seconds since the lock was taken (or for a waiter: since it started to wait).</html>");
			mtd.addColumn(CM_NAME, "BLOCKED_BY_SID", "<html>The session this session waits for (V$SESSION.BLOCKING_SESSION). 0 if the session does not wait.</html>");
			mtd.addColumn(CM_NAME, "OBJECT_NAME",    "<html>For a TM lock: the table. For a lock the session waits for: the table of the row it waits for.</html>");
			mtd.addColumn(CM_NAME, "EVENT",          "<html>What the session waits for right now (V$SESSION.EVENT), for example: 'enq: TX - row lock contention'.</html>");
			mtd.addColumn(CM_NAME, "ID1",            "<html>Lock identifier 1. For TM: the object id. For TX: undo segment number and slot.</html>");
			mtd.addColumn(CM_NAME, "ID2",            "<html>Lock identifier 2. For TX: the sequence (wrap) number of the transaction.</html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e);
		}
	}
}
