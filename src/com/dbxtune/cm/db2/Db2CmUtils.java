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
package com.dbxtune.cm.db2;

import java.lang.invoke.MethodHandles;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.cm.CountersModel;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.sql.conn.DbxConnection;

public class Db2CmUtils
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	/** The role that DbmsSetup/db2.txt creates for the monitoring login */
	public static final String MONITOR_ROLE = "DBXTUNE_MON";

	/**
	 * Can the login use the table functions (or views) a CM needs? If not: disable the CM, and tell what to do about it (in the log, and as a tooltip on the tab).
	 * <p>
	 * The monitoring login has no access to user data, only EXECUTE on the monitoring functions, granted to the role DBXTUNE_MON (see DbmsSetup/db2.txt).<br>
	 * Db2 checks EXECUTE when the statement is compiled, so <code>where 1=2</code> is enough (tested on 11.5.8: SQL0551N without the grant).<br>
	 * Use it in the CM's checkDependsOnOther():
	 * <pre>
	 * return Db2CmUtils.checkFunctionsAreExecutable(this, conn, "MON_GET_TRANSACTION_LOG(-2)", "MON_GET_UNIT_OF_WORK(null, -2)");
	 * </pre>
	 *
	 * @param cm          The CM that needs the functions
	 * @param conn
	 * @param sources     A table function call, for example <code>MON_GET_TABLESPACE('', -2)</code>, or a view with schema, for example <code>SYSIBMADM.DB_HISTORY</code>
	 * @return true if all of them can be used, false if the CM was disabled
	 */
	public static boolean checkFunctionsAreExecutable(CountersModel cm, DbxConnection conn, String... sources)
	{
		for (String source : sources)
		{
			boolean isFunction = source.contains("(");
			String  sql        = isFunction
					? "select 1 from table(" + source + ") t where 1=2"
					: "select 1 from " + source + " where 1=2";

			try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
			{
				while (rs.next())
					;
			}
			catch (SQLException ex)
			{
				_logger.debug("checkFunctionsAreExecutable(): cm='" + cm.getName() + "', sql='" + sql + "', Caught: " + ex);

				String name = isFunction ? source.substring(0, source.indexOf('(')).trim() : source.trim();
				String msg  = "The login can't use '" + name + "'. SQLCODE=" + ex.getErrorCode() + ", SQLSTATE=" + ex.getSQLState() + ".";
				String fix;
				if (isFunction)
					fix = "GRANT EXECUTE ON FUNCTION SYSPROC." + name + " TO ROLE " + MONITOR_ROLE + ";   (or: GRANT SQLADM ON DATABASE TO USER <user>;)  See DbmsSetup/db2.txt";
				else
					fix = "GRANT SELECT ON " + name + " TO ROLE " + MONITOR_ROLE + ";   (or: GRANT SQLADM ON DATABASE TO USER <user>;)  See DbmsSetup/db2.txt";

				// SQL0440N: the function does not exist (an older Db2 version), a GRANT will not help
				if (ex.getErrorCode() == -440 || ex.getErrorCode() == -204)
				{
					msg = "'" + name + "' does not exist in this Db2 version. SQLCODE=" + ex.getErrorCode() + ".";
					fix = "none, this Counter Model needs a newer Db2 version.";
				}

				return disable(cm, msg, fix);
			}
		}
		return true;
	}

	/**
	 * Disable the CM, with the reason in the log and as a tooltip on the tab
	 *
	 * @param cm
	 * @param msg      What is wrong
	 * @param fix      What to do about it (null if there is nothing to do)
	 * @return false (always), so the caller can do: <code>return Db2CmUtils.disable(this, "...", "...");</code>
	 */
	public static boolean disable(CountersModel cm, String msg, String fix)
	{
		String fullMsg = msg + (fix == null ? "" : "\n" + "Fix: " + fix);
		_logger.warn("When trying to initialize Counters Model '" + cm.getName() + "', named '" + cm.getDisplayName() + "': " + msg + (fix == null ? "" : " Fix: " + fix));
		cm.setActive(false, fullMsg);

		TabularCntrPanel tcp = cm.getTabPanel();
		if (tcp != null)
		{
			tcp.setUseFocusableTips(true);
			tcp.setToolTipText("<html>" + msg.replace("<", "&lt;").replace(">", "&gt;") + (fix == null ? "" : "<br>Fix: <code>" + fix.replace("<", "&lt;").replace(">", "&gt;") + "</code>") + "</html>");
		}
		return false;
	}
}
