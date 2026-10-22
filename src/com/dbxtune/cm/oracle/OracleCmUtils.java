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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.cm.CountersModel;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;
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
			// ORACLE LICENSE: a view that is a part of a management pack is NOT touched (not even to check if it is readable) unless it is allowed
			String pack = getManagementPackFor(viewName);
			if (pack != null)
			{
				String reason = getManagementPackNotAllowedReason(conn, pack);
				if (reason != null)
				{
					String msg = "'" + viewName + "' is a part of the Oracle " + pack + ", which needs an extra license from Oracle. " + reason;

					_logger.info("When trying to initialize Counters Model '" + cm.getName() + "', named '" + cm.getDisplayName() + "': " + msg);

					cm.setActive(false, msg);

					TabularCntrPanel tcp = cm.getTabPanel();
					if (tcp != null)
					{
						tcp.setUseFocusableTips(true);
						tcp.setToolTipText("<html>" + msg + "</html>");
					}

					return false;
				}
			}

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


	//------------------------------------------------------------------------------------------
	// ORACLE LICENSES: the management packs (Diagnostics Pack and Tuning Pack) cost extra
	//
	// RULE: nothing that needs an extra license from Oracle is used, unless the user has said that the organization has that license.
	//       Nobody should get a license problem with Oracle just because they ran this tool.
	//
	// Oracle can NOT tell us if there is a license:
	//   - the parameter 'control_management_pack_access' is DIAGNOSTIC+TUNING by default (also on the Free edition), so it can say "no", but never prove "yes"
	//   - V$OPTION is what is installed, DBA_FEATURE_USAGE_STATISTICS is what has been used. Neither is a license.
	// So: the property below (default NONE) AND the parameter in the database must BOTH allow the pack.
	//
	// When you write a CM (or anything else) that reads a view or calls a package that getManagementPackFor() knows:
	//   - a view: name it in checkViewsAreReadable(), that does the rest
	//   - a package (or SQL that is not in a CM): ask getManagementPackNotAllowedReason() first, and do not run it if you get a reason back
	//------------------------------------------------------------------------------------------
	/** Same values as the Oracle parameter 'control_management_pack_access': NONE, DIAGNOSTIC or DIAGNOSTIC+TUNING. Anything else is read as NONE */
	public static final String PROPKEY_licensedManagementPacks = "OracleTune.licensed.management.packs";
	public static final String DEFAULT_licensedManagementPacks = "NONE";

	public static final String PACK_DIAGNOSTIC = "Diagnostics Pack";
	public static final String PACK_TUNING     = "Tuning Pack";      // can only be licensed together with the Diagnostics Pack

	/**
	 * Is this view (or package) a part of an Oracle management pack, which needs an extra license?
	 * <p>
	 * The names are from the Oracle manual "Database Licensing Information" (19c), chapter "Oracle Management Packs".<br>
	 * Added here, since they show the same data: the GV$ views, and the CDB_HIST_ / AWR_ROOT_ / AWR_PDB_ / AWR_CDB_ / WRH$_ names of the AWR history.<br>
	 * DBA_ADVISOR_: the manual says that only the ADDM and SQL tuning rows needs a pack, but we can't see what rows a query asks for, so the whole view counts.
	 *
	 * @param objectName    a view or a package, for example: V$ACTIVE_SESSION_HISTORY, SYS.V_$SQL_MONITOR, DBA_HIST_SQLSTAT, DBMS_SQLTUNE
	 * @return null = no pack is needed, otherwise PACK_DIAGNOSTIC or PACK_TUNING
	 */
	public static String getManagementPackFor(String objectName)
	{
		// SYS.V_$XXX is the view behind the synonym V$XXX, and GV$XXX is the same view for all instances of a RAC
		String name = objectName.trim().toUpperCase();
		if (name.startsWith("SYS."))  name = name.substring("SYS.".length());
		if (name.startsWith("GV$"))   name = name.substring("G".length());
		if (name.startsWith("GV_$"))  name = name.substring("G".length());
		if (name.startsWith("V_$"))   name = "V$" + name.substring("V_$".length());

		// Tuning Pack
		if (name.startsWith("V$SQL_MONITOR"))      return PACK_TUNING;
		if (name.startsWith("V$SQL_PLAN_MONITOR")) return PACK_TUNING;
		if (name.startsWith("DBMS_SQLTUNE"))       return PACK_TUNING;
		if (name.startsWith("DBMS_SQL_MONITOR"))   return PACK_TUNING;
		if (name.startsWith("DBMS_AUTO_SQLTUNE"))  return PACK_TUNING;
		if (name.startsWith("DBMS_PERF"))          return PACK_TUNING;
		if (name.startsWith("DBMS_ADVISOR"))       return PACK_TUNING;  // SQL Tuning Advisor and SQL Access Advisor (and ADDM, which is the Diagnostics Pack)

		// Diagnostics Pack: the 6 AWR views that the manual says can be used without the license
		if (name.equals("DBA_HIST_SNAPSHOT"))          return null;
		if (name.equals("DBA_HIST_DATABASE_INSTANCE")) return null;
		if (name.equals("DBA_HIST_SNAP_ERROR"))        return null;
		if (name.equals("DBA_HIST_SEG_STAT"))          return null;
		if (name.equals("DBA_HIST_SEG_STAT_OBJ"))      return null;
		if (name.equals("DBA_HIST_UNDOSTAT"))          return null;

		// Diagnostics Pack
		if (name.equals("V$ACTIVE_SESSION_HISTORY"))       return PACK_DIAGNOSTIC;  // ASH
		if (name.equals("X$ASH"))                          return PACK_DIAGNOSTIC;
		if (name.startsWith("DBA_HIST_"))                  return PACK_DIAGNOSTIC;  // AWR
		if (name.startsWith("CDB_HIST_"))                  return PACK_DIAGNOSTIC;
		if (name.startsWith("AWR_ROOT_"))                  return PACK_DIAGNOSTIC;
		if (name.startsWith("AWR_PDB_"))                   return PACK_DIAGNOSTIC;
		if (name.startsWith("AWR_CDB_"))                   return PACK_DIAGNOSTIC;
		if (name.startsWith("WRH$_"))                      return PACK_DIAGNOSTIC;
		if (name.startsWith("DBA_ADDM_"))                  return PACK_DIAGNOSTIC;  // ADDM
		if (name.startsWith("CDB_ADDM_"))                  return PACK_DIAGNOSTIC;
		if (name.startsWith("DBA_ADVISOR_"))               return PACK_DIAGNOSTIC;
		if (name.startsWith("CDB_ADVISOR_"))               return PACK_DIAGNOSTIC;
		if (name.startsWith("DBA_STREAMS_TP_"))            return PACK_DIAGNOSTIC;
		if (name.startsWith("DBMS_WORKLOAD_REPOSITORY"))   return PACK_DIAGNOSTIC;
		if (name.startsWith("DBMS_ADDM"))                  return PACK_DIAGNOSTIC;
		if (name.startsWith("DBMS_UMF"))                   return PACK_DIAGNOSTIC;
		if (name.startsWith("DBMS_AWRHUB"))                return PACK_DIAGNOSTIC;

		return null;
	}

	/**
	 * May we use this management pack? Only when BOTH the property 'OracleTune.licensed.management.packs' and the database parameter 'control_management_pack_access' says so.
	 *
	 * @param conn
	 * @param pack   PACK_DIAGNOSTIC or PACK_TUNING
	 * @return null = it is allowed, otherwise the reason why it is NOT allowed (a text for the user, that says what to do)
	 */
	public static String getManagementPackNotAllowedReason(DbxConnection conn, String pack)
	{
		// What the property (and the database parameter) must say for this pack
		String needs = PACK_TUNING.equals(pack) ? "DIAGNOSTIC+TUNING" : "DIAGNOSTIC";

		// 1: The user must have said that the organization has the license. The default is NONE
		String  propVal    = Configuration.getCombinedConfiguration().getProperty(PROPKEY_licensedManagementPacks, DEFAULT_licensedManagementPacks).trim().toUpperCase();
		boolean propAllows = propVal.equals("DIAGNOSTIC+TUNING") || (propVal.equals("DIAGNOSTIC") && PACK_DIAGNOSTIC.equals(pack));
		if ( ! propAllows )
		{
			return "It is only used when you have said that your organization has that license: set the property '" + PROPKEY_licensedManagementPacks + "=" + needs + "' in the configuration file. "
			     + "(it is now '" + propVal + "', and the default is '" + DEFAULT_licensedManagementPacks + "')";
		}

		// 2: And the database must allow it. (if the parameter can't be read: it is NOT allowed)
		String dbVal = null;
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery("select VALUE from V$PARAMETER where NAME = 'control_management_pack_access'"))
		{
			while (rs.next())
				dbVal = rs.getString(1);
		}
		catch (SQLException ex)
		{
			return "The property '" + PROPKEY_licensedManagementPacks + "=" + propVal + "' allows it, but the database parameter 'control_management_pack_access' could not be read, so it is NOT used. Caught: " + ex.getMessage().trim();
		}

		String  dbValUpper = (dbVal == null) ? "" : dbVal.trim().toUpperCase();
		boolean dbAllows   = dbValUpper.equals("DIAGNOSTIC+TUNING") || (dbValUpper.equals("DIAGNOSTIC") && PACK_DIAGNOSTIC.equals(pack));
		if ( ! dbAllows )
		{
			return "The property '" + PROPKEY_licensedManagementPacks + "=" + propVal + "' allows it, but the database does not: the parameter 'control_management_pack_access' is '" + dbVal + "' (it must be '" + needs + "').";
		}

		return null;
	}
}
