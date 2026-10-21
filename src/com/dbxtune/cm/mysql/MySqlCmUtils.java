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
package com.dbxtune.cm.mysql;

import java.lang.invoke.MethodHandles;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.cm.CountersModel;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfoMySql;

public class MySqlCmUtils
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	/**
	 * Can the login read the views a CM needs? If not: disable the CM, and tell what to do about it (in the log, and as a tooltip on the tab).
	 * <p>
	 * The monitoring login has no access to user data, only: PROCESS, REPLICATION CLIENT, SELECT on performance_schema and SELECT, EXECUTE on sys (see DbmsSetup/mysql.txt).<br>
	 * So the GRANT that is suggested depends on where the view lives.<br>
	 * Use it in the CM's checkDependsOnOther():
	 * <pre>
	 * return MySqlCmUtils.checkViewsAreReadable(this, conn, "performance_schema.threads", "information_schema.INNODB_TRX");
	 * </pre>
	 *
	 * @param cm          The CM that needs the views
	 * @param conn
	 * @param viewNames   The views the CM reads, with the schema name (performance_schema.xxx, sys.xxx, information_schema.xxx)
	 * @return true if all the views are readable, false if the CM was disabled
	 */
	public static boolean checkViewsAreReadable(CountersModel cm, DbxConnection conn, String... viewNames)
	{
		DbmsVersionInfoMySql versionInfo = (conn.getDbmsVersionInfo() instanceof DbmsVersionInfoMySql) ? (DbmsVersionInfoMySql) conn.getDbmsVersionInfo() : null;

		for (String viewName : viewNames)
		{
			String viewLower = viewName.toLowerCase();

			// performance_schema=OFF: the tables are there, but they are always empty. So the CM would show nothing, without any error.
			if (viewLower.startsWith("performance_schema.") && versionInfo != null && ! versionInfo.isPerfSchemaOn())
			{
				String msg = "performance_schema is OFF in this server, so '" + viewName + "' is always empty.";
				String fix = "set 'performance_schema=ON' in the server's configuration file (my.cnf) and restart the server.";
				return disable(cm, msg, fix);
			}

			// NOTE: DbUtils.checkIfTableIsSelectable() does 'where 1=2', which is NOT enough for the INNODB_* views in information_schema:
			//       the PROCESS privilege is only checked when the view is filled. So read one row.
			if (canReadOneRow(conn, viewName))
				continue;

			String msg = "The login can't read '" + viewName + "'.";
			String fix;
			if      (viewLower.startsWith("performance_schema."))            fix = "GRANT SELECT ON performance_schema.* TO <user>;";
			else if (viewLower.startsWith("sys."))                           fix = "GRANT SELECT, EXECUTE ON sys.* TO <user>;";
			else if (viewLower.startsWith("information_schema.innodb_"))     fix = "GRANT PROCESS ON *.* TO <user>;";
			else                                                             fix = "GRANT SELECT ON " + viewName + " TO <user>;";

			// MariaDB has other views than MySQL: the GRANT is probably not the answer
			if (versionInfo != null && versionInfo.isMariaDb())
			{
				msg = "This is MariaDB (" + versionInfo.getVersionString() + "), and the login can't read '" + viewName + "' (it may not exist in MariaDB). MySqlTune is made for MySQL 8.0 and above.";
				fix = "none, this Counter Model is not available for MariaDB.";
			}

			return disable(cm, msg, fix);
		}

		return true;
	}

	/** Can we read (one row of) the view? Any error is a NO */
	private static boolean canReadOneRow(DbxConnection conn, String viewName)
	{
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery("select 1 from " + viewName + " limit 1"))
		{
			while (rs.next())
				;
			return true;
		}
		catch (SQLException ex)
		{
			_logger.debug("canReadOneRow(): view='" + viewName + "', Caught: " + ex);
			return false;
		}
	}

	/** Disable the CM, with the reason in the log and as a tooltip on the tab */
	private static boolean disable(CountersModel cm, String msg, String fix)
	{
		_logger.warn("When trying to initialize Counters Model '" + cm.getName() + "', named '" + cm.getDisplayName() + "': " + msg + " Fix: " + fix);

		cm.setActive(false, msg + "\n" + "Fix: " + fix);

		TabularCntrPanel tcp = cm.getTabPanel();
		if (tcp != null)
		{
			tcp.setUseFocusableTips(true);
			tcp.setToolTipText("<html>" + msg + "<br>Fix: <code>" + fix.replace("<", "&lt;").replace(">", "&gt;") + "</code></html>");
		}

		return false;
	}
}
