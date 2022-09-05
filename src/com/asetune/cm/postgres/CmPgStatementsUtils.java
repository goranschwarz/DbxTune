/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
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
package com.asetune.cm.postgres;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

import com.asetune.cm.CountersModel;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;

public class CmPgStatementsUtils
{
	private static Logger _logger = Logger.getLogger(CmPgStatementsUtils.class);

	public static final String  PROPKEY_createExtension     = "CmPgStatements.if.missing.pg_stat_statements.createExtension";
	public static final boolean DEFAULT_createExtension     = true; 

	/**
	 * Called before <code>refreshGetData(conn)</code> where we can make various checks
	 * <p>
	 * Note: this is a special case since SIX is recreating the Postgres Server 
	 *       (every now and then) during the day/night...
	 *       We need to check/create the extension before polling data from it!
	 */
	public static boolean beforeRefreshGetData(CountersModel cm, DbxConnection conn) throws Exception
	{
		String sql = "SELECT 1 FROM pg_catalog.pg_class c WHERE c.relname = 'pg_stat_statements' AND c.relkind = 'v' ";
		
		// Check if 'pg_stat_statements' exists... 
		// and possibly create it...
		boolean exists = false;
		try( Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql); )
		{
			while(rs.next())
				exists = true;
		}
		
		if ( ! exists )
		{
			_logger.warn("When checking Counters Model '" + cm.getName() + "', named '" + cm.getDisplayName() + "' The table 'pg_stat_statements' do not exists.");

			_logger.warn("Possible solution for 'pg_stat_statements': (1: check that file 'PG_INST_DIR/lib/pg_stat_statements.so' exists, if not install postgres contrib package), (2: check that config 'shared_preload_libraries' contains 'pg_stat_statements'), (3: optional: configure 'pg_stat_statements.max=10000', 'pg_stat_statements.track=all' in the config file and restart postgres), (4: execute: CREATE EXTENSION pg_stat_statements), (5: verify with: SELECT * FROM pg_stat_statements). or look at https://www.postgresql.org/docs/current/static/pgstatstatements.html");

			boolean createExtension = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_createExtension, DEFAULT_createExtension);
			if (createExtension)
			{
				_logger.info("AUTO-FIX: The table 'pg_stat_statements' did not exists... Trying to create it, this will simply work if you are authorized. to DISABLE the 'CREATE EXTENSION pg_stat_statements' set config '" + PROPKEY_createExtension + "=false'.");

				try (Statement stmnt = conn.createStatement())
				{
					_logger.info("AUTO-FIX: Executing: CREATE EXTENSION pg_stat_statements");

					stmnt.executeUpdate("CREATE EXTENSION pg_stat_statements");

					_logger.info("AUTO-FIX: Success executing: CREATE EXTENSION pg_stat_statements");

					// Check if the table is accessible after 'CREATE EXTENSION pg_stat_statements'
					try( Statement stmnt2 = conn.createStatement(); ResultSet rs = stmnt.executeQuery("SELECT * FROM pg_stat_statements where dbid = -999"); )
					{
						while(rs.next())
							; // just loop the RS, no rows will be found...
						exists = true;
					}
					catch (SQLException exSelChk)
					{
						exists = false;
						_logger.warn("AUTO-FIX: FAILED when checking table 'pg_stat_statements' after 'CREATE EXTENSION pg_stat_statements'. Caught: " + exSelChk);
						
						// Possibly turn off 'AUTO-FIX' since it failed... until next restart (hence the System.setProperty())
						//System.setProperty(PROPKEY_createExtension, "false"); 
					}
				}
				catch(SQLException exCrExt)
				{
					exists = false;
					_logger.warn("AUTO-FIX: FAILED when executing 'CREATE EXTENSION pg_stat_statements'. Caught: " + exCrExt);

					// Possibly turn off 'AUTO-FIX' since it failed... until next restart (hence the System.setProperty())
					//System.setProperty(PROPKEY_createExtension, "false"); 
				}
			}
		}
		
		return exists;
	}

	public static boolean checkDependsOnOther(CountersModel cm, DbxConnection conn)
	{
		boolean isOk = false;
		String  message = "";
		
		// Check if the table exists, since it's optional and needs to be installed
		try
		{
			isOk = cm.beforeRefreshGetData(conn);
		}
		catch (Exception ex)
		{
			isOk = false;
			_logger.warn("When trying to initialize Counters Model '" + cm.getName() + "', named '" + cm.getDisplayName() + "' The table 'pg_stat_statements' do not exists. This is an optional component. Caught: " + ex);
			message = ex.getMessage();
		}

		if (isOk)
		{
			_logger.info("Check dependencies SUCCESS: Table 'pg_stat_statements' exists when checking Counters Model '" + cm.getName() + "'.");
			return true;
		}
		
		cm.setActive(false, 
				"The table 'pg_stat_statements' do not exists.\n"
				+ "To enable this see: https://www.postgresql.org/docs/current/static/pgstatstatements.html\n"
				+ "\n"
				+ "Or possibly issue: CREATE EXTENSION pg_stat_statements \n"
				+ "\n"
				+ message);

		TabularCntrPanel tcp = cm.getTabPanel();
		if (tcp != null)
		{
			tcp.setUseFocusableTips(true);
			tcp.setToolTipText("<html>"
					+ "The table 'pg_stat_statements' do not exists.<br>"
					+ "To enable this see: https://www.postgresql.org/docs/current/static/pgstatstatements.html<br>"
					+ "<br>"
					+ "Or possibly issue: CREATE EXTENSION pg_stat_statements <br>"
					+ "<br>"
					+ message+"</html>");
		}
		
		return false;
	}	

}
