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
package com.dbxtune.cm.postgres;

import java.lang.invoke.MethodHandles;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.cm.CountersModel;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfoPostgres;
import com.dbxtune.utils.Ver;

public class PostgresCmUtils
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	/**
	 * Can the login read the views (or call the functions) a CM needs? If not: disable the CM, and tell what to do about it (in the log, and as a tooltip on the tab).
	 * <p>
	 * The monitoring login only needs the role 'pg_monitor' (see DbmsSetup/postgres.txt), and 'pg_read_server_files' for the error log.<br>
	 * Use it in the CM's checkDependsOnOther():
	 * <pre>
	 * return PostgresCmUtils.checkViewsAreReadable(this, conn, "pg_ls_waldir()");
	 * </pre>
	 * NOTE: A view that is readable, but hides other sessions' data (pg_stat_activity.query without pg_read_all_stats) is NOT found by this check.
	 *
	 * @param cm          The CM that needs the views
	 * @param conn
	 * @param viewNames   Views, or set returning functions with their parameters, for example "pg_ls_waldir()"
	 * @return true if all are readable, false if the CM was disabled
	 */
	public static boolean checkViewsAreReadable(CountersModel cm, DbxConnection conn, String... viewNames)
	{
		DbmsVersionInfoPostgres versionInfo = (conn.getDbmsVersionInfo() instanceof DbmsVersionInfoPostgres) ? (DbmsVersionInfoPostgres) conn.getDbmsVersionInfo() : null;

		for (String viewName : viewNames)
		{
			if (canExecute(conn, "select 1 from " + viewName + " limit 1"))
				continue;

			String viewLower = viewName.toLowerCase();
			String msg = "The login can't read '" + viewName + "'.";
			String fix;
			if (viewLower.startsWith("pg_read_file") || viewLower.startsWith("pg_read_binary_file") || viewLower.startsWith("pg_current_logfile"))
				fix = "GRANT pg_read_server_files TO <user>; (or a superuser)";
			else if (versionInfo != null && versionInfo.getLongVersion() > 0 && versionInfo.getLongVersion() < Ver.ver(10))
				fix = "use a superuser login (Postgres before 10 has no 'pg_monitor' role)";
			else
				fix = "GRANT pg_monitor TO <user>;";

			// A managed service (RDS, Azure, Cloud SQL...) may not allow it at all: the GRANT is probably not the answer
			if (versionInfo != null && versionInfo.isCloudManagedDbms())
				msg = "This is " + versionInfo.getFlavor() + ", and the login can't read '" + viewName + "' (the service may not allow it, even with 'pg_monitor'). The data may be available in the service's own monitoring.";

			return disable(cm, msg, fix);
		}

		return true;
	}

	/**
	 * Can the login execute this SQL? Any error is a NO.
	 * <p>
	 * When AutoCommit is off (the refresh runs in ONE transaction), a failing statement aborts the whole transaction in Postgres.<br>
	 * So the statement is executed inside a SAVEPOINT, and rolled back to it on errors.
	 */
	public static boolean canExecute(DbxConnection conn, String sql)
	{
		Savepoint savepoint = null;
		try
		{
			if ( ! conn.getAutoCommit() )
				savepoint = conn.setSavepoint();

			try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
			{
				while (rs.next())
					;
			}

			if (savepoint != null)
				conn.releaseSavepoint(savepoint);
			return true;
		}
		catch (SQLException ex)
		{
			_logger.debug("canExecute(): sql=|" + sql + "|, Caught: " + ex);
			if (savepoint != null)
			{
				try { conn.rollback(savepoint); }
				catch (SQLException ex2) { _logger.warn("canExecute(): Problems rolling back to the savepoint. sql=|" + sql + "|, Caught: " + ex2); }
			}
			return false;
		}
	}

	/**
	 * Get the first column of the first row, or null on errors (logged at DEBUG) or when there are no rows.
	 * <p>
	 * Inside a SAVEPOINT when AutoCommit is off (the same reason as in canExecute(): the CM initialization and the refresh run in a transaction).
	 */
	public static String getFirstString(DbxConnection conn, String sql)
	{
		Savepoint savepoint = null;
		try
		{
			if ( ! conn.getAutoCommit() )
				savepoint = conn.setSavepoint();

			String value = null;
			try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
			{
				if (rs.next())
					value = rs.getString(1);
				while (rs.next())
					;
			}

			if (savepoint != null)
				conn.releaseSavepoint(savepoint);
			return value;
		}
		catch (SQLException ex)
		{
			_logger.debug("getFirstString(): sql=|" + sql + "|, Caught: " + ex);
			if (savepoint != null)
			{
				try { conn.rollback(savepoint); }
				catch (SQLException ex2) { _logger.warn("getFirstString(): Problems rolling back to the savepoint. sql=|" + sql + "|, Caught: " + ex2); }
			}
			return null;
		}
	}

	/** Disable the CM, with the reason in the log and as a tooltip on the tab. Always returns false (so it can be used as: return disable(...)) */
	public static boolean disable(CountersModel cm, String msg, String fix)
	{
		_logger.warn("When trying to initialize Counters Model '" + cm.getName() + "', named '" + cm.getDisplayName() + "': " + msg + " Fix: " + fix);

		cm.setActive(false, msg + "\n" + "Fix: " + fix);

		TabularCntrPanel tcp = cm.getTabPanel();
		if (tcp != null)
		{
			tcp.setUseFocusableTips(true);
			tcp.setToolTipText("<html>" + msg.replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>") + "<br>Fix: <code>" + fix.replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>") + "</code></html>");
		}

		return false;
	}
}
