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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.cm.CountersModel;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.DbUtils;

public class OracleCmUtils
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	/**
	 * Can the login read the views a CM needs? If not: disable the CM, and tell what to do about it (in the log, and as a tooltip on the tab).
	 * <p>
	 * A CM never asks for a role by name (NEED_ROLES is empty), since the login can have either SELECT_CATALOG_ROLE or SELECT ANY DICTIONARY. So we check the views instead.<br>
	 * Use it in the CM's checkDependsOnOther():
	 * <pre>
	 * return OracleCmUtils.checkViewsAreReadable(this, conn, "V$SESSION", "V$PROCESS");
	 * </pre>
	 *
	 * @param cm          The CM that needs the views
	 * @param conn
	 * @param viewNames   The views the CM reads
	 * @return true if all the views are readable, false if the CM was disabled
	 */
	public static boolean checkViewsAreReadable(CountersModel cm, DbxConnection conn, String... viewNames)
	{
		for (String viewName : viewNames)
		{
			if (DbUtils.checkIfTableIsSelectable(conn, viewName))
				continue;

			String msg = "The login can't read '" + viewName + "'.";
			String fix = "GRANT SELECT_CATALOG_ROLE TO <user>;";

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

		return true;
	}
}
