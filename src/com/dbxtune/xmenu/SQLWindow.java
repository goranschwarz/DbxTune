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

/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.dbxtune.xmenu;

import java.util.HashMap;
import java.util.Properties;

import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.tools.WindowType;
import com.dbxtune.tools.sqlw.QueryWindow;
import com.dbxtune.utils.AseConnectionUtils;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.ConnectionProvider;


/**
 * @author gorans
 */
public class SQLWindow 
extends XmenuActionBase 
{

	/**
	 * 
	 */
	public SQLWindow() 
	{
		super();
	}

	/**
	 * Decide if the Caller should create a new connection for us or just pass the connection the caller was using.<br>
	 * Lets ALWAYS just REUSE the callers connection, if we need a new connection we will create one ourself from the ConnectionProvider.
	 * 
	 */
	@Override 
	public boolean createConnectionOnStart()
	{
		return false;
	}

	/**
	 */
	@Override 
	public void doWork() 
	{
//		Connection conn = getConnection();
		DbxConnection conn = getConnection();

		String sql = getConfig();
		HashMap<String,String> paramValues = getParamValues();

		
		// if we have aliases (1 parameter with multiple "names") for a parameter, then the FIRST name in the alias list will be used as a parameter name
		for (String pv : paramValues.keySet())
		{
//			sql = sql.replaceAll("\\$\\{"+pv+"\\}", getParamValue(pv));
			sql = sql.replace("${"+pv+"}", getParamValue(pv));
			
			// System.out.println("xxxxxxxxxxxxxxxx: key='"+pv+"', value='"+getParamValue(pv)+"'.");
		}

		// NOTE:
		// if the tag: <getOwner(DBNameStr,ObjectNameStr)> exists, then lookup and grab the first 
		// owner we find for a objects, if there are more than one table, with different owners, 
		// just grab the first one.
		if (sql.indexOf("<getOwner(") >= 0)
		{
			String dbname  = null;
			String objname = null;

			int startPos = sql.indexOf("<getOwner(") + "<getOwner(".length();
			int endPos   = sql.indexOf(")>");
			if (endPos >= 0)
			{
				String tagData = sql.substring(startPos, endPos);
//System.out.println("tagData='"+tagData+"'.");
				String sa[] = tagData.split(",");
				if (sa.length < 2)
				{
					// to few parameters... what to do?
				}
				else
				{
					dbname  = sa[0].trim();
					objname = sa[1].trim();
	
					String owner = AseConnectionUtils.getObjectOwner(conn, dbname, objname);
					
					// finally replace "<getOwner(...)>" with the object owner
					sql = sql.replaceAll("<getOwner(.*)>", owner);
				}
			}
			else
			{
				// Can't find the end... what to do?
			}
		}
		
		sql = modifySql(sql);

		// TODO: figure out if this was called from SQLWindow or from AseTune (or possible from what JTable sub type)
		//       if called from AseTune   open new Window
		//       if called from SQLWindow execute the statement in current window
		// can we use a stacktrace to see from where it's called?
		// or do we need to add some parameter ???
		ConnectionProvider connProvider = getConnectionProvider();
		if (connProvider instanceof QueryWindow)
		{
			QueryWindow qw = (QueryWindow)connProvider;
			qw.displayQueryResults(sql, 0, false, true);
		}
		else
		{
			boolean closeConnOnExit = true;
			DbxConnection newConn = getConnectionProvider().getNewConnection("QueryWindow");

			boolean noExec = false; // Do not execute ... just show the SQL in the windows...
			boolean goPsql = true;  // Print SQL on output
			Properties props = getAllProperties();
			if (props != null)
			{
				noExec = props.getProperty("noexec",  noExec +"").equalsIgnoreCase("true");
				goPsql = props.getProperty("go.psql", goPsql +"").equalsIgnoreCase("true");
			}
			
			QueryWindow qf = new QueryWindow(newConn, sql, false, null, closeConnOnExit, WindowType.JFRAME, getConfiguration());

			qf.setOption(QueryWindow.OptionType.SHOW_SENT_SQL      , goPsql);
			qf.setOption(QueryWindow.OptionType.PRINT_CLIENT_TIMING, true);
			qf.openTheWindow();
			
			if ( ! noExec )
				qf.displayQueryResults(sql, 0, false);
			
		}
//		else if (connProvider instanceof MainFrame)
//		{
//			QueryWindow qf = new QueryWindow(conn, sql, null, isCloseConnOnExit(), WindowType.JFRAME, getConfiguration());
//			qf.openTheWindow();
//		}
//		else
//		{
//			SwingUtils.showInfoMessage(null, "Sorry not from here", "This functionality is not available from the Component '"+connProvider.getClass().getSimpleName()+"'.");
//			return;
//		}


//				"print '11111111'\n" +
//				"exec sp_whoisw2\n" +
//				"select \"ServerName\" = @@servername\n" +
//				"select \"Current Date\" = getdate()\n" +
//				"print '222222222'\n" +
//				"select * from master..sysdatabases\n" +
//				"print '333333333'\n" +
//				"");

		
	}

	/** 
	 * Hook to modify SQL before execution, implemented by subclasses... 
	 * 
	 * @param the SQL that is about to be executed
	 * @return the new and modified SQL statement
	 */
	protected String modifySql(String sql)
	{
		return sql;
	}
	
	/**
	 * Get Properties/Configuration passed to QueryWindow Object
	 * @return
	 */
	protected Configuration getConfiguration()
	{
		return null;
	}
}
