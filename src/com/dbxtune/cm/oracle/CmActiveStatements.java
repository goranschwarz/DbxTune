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
package com.dbxtune.cm.oracle;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.cm.CounterSample;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmActiveStatements
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(CmActiveStatements.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmActiveStatements.class.getSimpleName();
	public static final String   SHORT_NAME       = "Active Statements";
	public static final String   HTML_DESC        =
		"<html>" +
		"<p>Sessions that are currently executing or hold an open transaction in Oracle.</p>" +
		"<br><br>" +
		"Table Background colors:" +
		"<ul>" +
		"    <li>ORANGE - SID was visible in previous sample as well (multi-sampled).</li>" +
		"    <li>YELLOW - Session is a background/system process.</li>" +
		"</ul>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"V$SESSION", "V$TRANSACTION", "V$SQL"};
	public static final String[] NEED_ROLES       = new String[] {"SELECT_CATALOG_ROLE"};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = false;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.SMALL; }

	/**
	 * FACTORY method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '"+CM_NAME+"'");

		return new CmActiveStatements(counterController, guiController);
	}

	public CmActiveStatements(ICounterController counterController, IGuiController guiController)
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

	private void addTrendGraphs()
	{
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List<String> pkCols = new LinkedList<String>();
		pkCols.add("SID");
		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		String sql =
			"SELECT \n" +
			"     s.SID \n" +
			"    ,s.SERIAL# \n" +
			"    ,s.USERNAME \n" +
			"    ,s.STATUS \n" +
			"    ,s.OSUSER \n" +
			"    ,s.MACHINE \n" +
			"    ,s.PROGRAM \n" +
			"    ,s.MODULE \n" +
			"    ,s.ACTION \n" +
			"    ,s.CLIENT_INFO \n" +
			"    ,s.LOGON_TIME \n" +
			"    ,s.LAST_CALL_ET                                               AS ExecTimeInSec \n" +
			"    ,s.WAIT_CLASS \n" +
			"    ,s.EVENT \n" +
			"    ,s.WAIT_TIME_MICRO / 1000                                     AS WaitTimeMs \n" +
			"    ,s.STATE \n" +
			"    ,s.SQL_ID \n" +
			"    ,s.PREV_SQL_ID \n" +
			"    ,COALESCE(s.SQL_ID, s.PREV_SQL_ID)                            AS EffectiveSqlId \n" +
			"    ,s.SQL_CHILD_NUMBER \n" +
			"    ,s.SQL_EXEC_START \n" +
			"    ,CASE WHEN s.SQL_ID IS NULL THEN 'Y' ELSE 'N' END             AS IsPrevSql \n" +
			"    ,q.SQL_TEXT                                                   AS SqlText \n" +
			"    ,CASE WHEN t.XIDUSN IS NOT NULL THEN 'Y' ELSE 'N' END        AS HasOpenTxn \n" +
			"    ,t.USED_UBLK                                                  AS TxnUndoBlocks \n" +
			"    ,t.START_TIME                                                 AS TxnStartTime \n" +
			"    ,CAST(NULL AS VARCHAR2(4000))                                 AS ExecPlan \n" +
			"FROM V$SESSION s \n" +
			"LEFT JOIN V$TRANSACTION t ON t.ADDR = s.TADDR \n" +
			"LEFT JOIN V$SQL q         ON q.SQL_ID = COALESCE(s.SQL_ID, s.PREV_SQL_ID) \n" +
			"                         AND q.CHILD_NUMBER = COALESCE(s.SQL_CHILD_NUMBER, 0) \n" +
			"WHERE s.STATUS = 'ACTIVE' \n" +
			"   OR t.XIDUSN IS NOT NULL \n" +
			"ORDER BY s.SID \n" +
			"";

		return sql;
	}

	@Override
	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
	{
		if (newSample == null)
			return;

		List<String> colNames = newSample.getColNames();
		if (colNames == null)
			return;

		int pos_SID         = -1;
		int pos_SQL_ID      = -1;
		int pos_ChildNum    = -1;
		int pos_ExecPlan    = -1;

		for (int colId = 0; colId < colNames.size(); colId++)
		{
			String colName = colNames.get(colId);
			if      (colName.equals("SID"))              pos_SID      = colId;
			else if (colName.equals("SQL_ID"))           pos_SQL_ID   = colId;
			else if (colName.equals("SQL_CHILD_NUMBER")) pos_ChildNum = colId;
			else if (colName.equals("ExecPlan"))         pos_ExecPlan = colId;
		}

		if (pos_ExecPlan < 0 || pos_SQL_ID < 0)
			return;

		DbxConnection conn = getCounterController() == null ? null : getCounterController().getMonConnection();
		if (conn == null)
			return;

		int rowCount = newSample.getRowCount();
		for (int rowId = 0; rowId < rowCount; rowId++)
		{
			Object o_SQL_ID = newSample.getValueAt(rowId, pos_SQL_ID);
			if (o_SQL_ID == null)
				continue;

			String sqlId = o_SQL_ID.toString().trim();
			if (sqlId.isEmpty())
				continue;

			int childNum = 0;
			if (pos_ChildNum >= 0)
			{
				Object o_child = newSample.getValueAt(rowId, pos_ChildNum);
				if (o_child instanceof Number)
					childNum = ((Number) o_child).intValue();
			}

			String planText = fetchExecPlan(conn, sqlId, childNum);
			if (planText != null)
				newSample.setValueAt(planText, rowId, pos_ExecPlan);
		}
	}

	private String fetchExecPlan(DbxConnection conn, String sqlId, int childNum)
	{
		String callSql = "SELECT * FROM TABLE(DBMS_XPLAN.DISPLAY_CURSOR('" + sqlId + "', " + childNum + ", 'TYPICAL'))";

		StringBuilder sb = new StringBuilder();
		try (Statement stm = conn.createStatement())
		{
			stm.setQueryTimeout(5);
			try (ResultSet rs = stm.executeQuery(callSql))
			{
				while (rs.next())
				{
					sb.append(rs.getString(1)).append("\n");
				}
			}
		}
		catch (SQLException e)
		{
			_logger.debug("fetchExecPlan: sqlId=" + sqlId + ", childNum=" + childNum + " -- " + e.getMessage());
			return null;
		}

		return sb.length() > 0 ? sb.toString() : null;
	}

	@Override
	public void initLocalToolTipTextOnTableColumnHeader()
	{
		setLocalToolTipTextOnTableColumnHeader("SID",           "<html>Oracle session identifier.</html>");
		setLocalToolTipTextOnTableColumnHeader("SERIAL#",       "<html>Session serial number. Used together with SID to uniquely identify a session.</html>");
		setLocalToolTipTextOnTableColumnHeader("USERNAME",      "<html>Oracle username of the session.</html>");
		setLocalToolTipTextOnTableColumnHeader("STATUS",        "<html>Status of the session: ACTIVE (currently executing SQL) or INACTIVE.</html>");
		setLocalToolTipTextOnTableColumnHeader("OSUSER",        "<html>Operating system client username.</html>");
		setLocalToolTipTextOnTableColumnHeader("MACHINE",       "<html>Operating system machine name of the client.</html>");
		setLocalToolTipTextOnTableColumnHeader("PROGRAM",       "<html>Operating system program name of the client.</html>");
		setLocalToolTipTextOnTableColumnHeader("MODULE",        "<html>Module name set by the client application (via DBMS_APPLICATION_INFO).</html>");
		setLocalToolTipTextOnTableColumnHeader("ACTION",        "<html>Action name set by the client application (via DBMS_APPLICATION_INFO).</html>");
		setLocalToolTipTextOnTableColumnHeader("CLIENT_INFO",   "<html>Client info string set by the application (via DBMS_APPLICATION_INFO).</html>");
		setLocalToolTipTextOnTableColumnHeader("LOGON_TIME",    "<html>Time at which the session logged on.</html>");
		setLocalToolTipTextOnTableColumnHeader("ExecTimeInSec", "<html>LAST_CALL_ET: Elapsed time in seconds since the session became ACTIVE (or since last call completed for INACTIVE sessions).</html>");
		setLocalToolTipTextOnTableColumnHeader("WAIT_CLASS",    "<html>Class of the wait event the session is currently waiting on (e.g. User I/O, Network, Idle).</html>");
		setLocalToolTipTextOnTableColumnHeader("EVENT",         "<html>Wait event the session is currently waiting for.</html>");
		setLocalToolTipTextOnTableColumnHeader("WaitTimeMs",    "<html>WAIT_TIME_MICRO / 1000: Time in milliseconds the session has been waiting for the current event.</html>");
		setLocalToolTipTextOnTableColumnHeader("STATE",         "<html>Wait state: WAITING, WAITED SHORT TIME, WAITED KNOWN TIME, WAITED UNKNOWN TIME.</html>");
		setLocalToolTipTextOnTableColumnHeader("SQL_ID",        "<html>SQL identifier of the SQL statement currently being executed. NULL if the session is idle.</html>");
		setLocalToolTipTextOnTableColumnHeader("PREV_SQL_ID",   "<html>SQL identifier of the last SQL statement executed. Useful when SQL_ID is NULL (idle in transaction).</html>");
		setLocalToolTipTextOnTableColumnHeader("EffectiveSqlId","<html>COALESCE(SQL_ID, PREV_SQL_ID): The effective SQL being shown — current if active, previous if idle in transaction.</html>");
		setLocalToolTipTextOnTableColumnHeader("SQL_CHILD_NUMBER", "<html>Child number of the SQL cursor. Combined with SQL_ID uniquely identifies a cursor in the library cache.</html>");
		setLocalToolTipTextOnTableColumnHeader("SQL_EXEC_START","<html>Timestamp when execution of the current SQL statement began.</html>");
		setLocalToolTipTextOnTableColumnHeader("IsPrevSql",     "<html>Y if SQL_ID is NULL and we are showing PREV_SQL_ID instead (session is idle but holds an open transaction).</html>");
		setLocalToolTipTextOnTableColumnHeader("SqlText",       "<html>First ~1000 characters of the SQL text from V$SQL.</html>");
		setLocalToolTipTextOnTableColumnHeader("HasOpenTxn",    "<html>Y if the session currently holds an open transaction (has an entry in V$TRANSACTION).</html>");
		setLocalToolTipTextOnTableColumnHeader("TxnUndoBlocks", "<html>USED_UBLK: Number of undo blocks used by the current transaction.</html>");
		setLocalToolTipTextOnTableColumnHeader("TxnStartTime",  "<html>START_TIME: Time when the current transaction started.</html>");
		setLocalToolTipTextOnTableColumnHeader("ExecPlan",      "<html>Execution plan from DBMS_XPLAN.DISPLAY_CURSOR for the current SQL_ID. Fetched in localCalculation().</html>");
	}
}
