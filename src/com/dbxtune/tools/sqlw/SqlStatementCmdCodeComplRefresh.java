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
package com.dbxtune.tools.sqlw;

import java.awt.Component;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import javax.swing.JComponent;

import org.apache.commons.cli.Options;

import com.dbxtune.sql.SqlProgressDialog;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.pipe.PipeCommandException;
import com.dbxtune.utils.StringUtil;

public class SqlStatementCmdCodeComplRefresh 
extends SqlStatementAbstract
{
//	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private String[] _args = null;
//	private String _originCmd = null;

	public SqlStatementCmdCodeComplRefresh(DbxConnection conn, String sqlOrigin, String dbProductName, ArrayList<JComponent> resultCompList, SqlProgressDialog progress, Component owner, QueryWindow queryWindow)
	throws SQLException, PipeCommandException
	{
		super(conn, sqlOrigin, dbProductName, resultCompList, progress, owner, queryWindow);
		parse(sqlOrigin);
		init();
	}

	/**
	 * ============================================================
	 * see printHelp() for usage
	 * ------------------------------------------------------------
	 * 
	 * @param input
	 * @return
	 * @throws PipeCommandException
	 */
	public void parse(String input)
	throws SQLException, PipeCommandException
	{
//		_originCmd = input;
		String params = input.replace("\\ccr", "").trim();

		_args = StringUtil.translateCommandline(params, false);

		if (_args.length >= 1)
		{
			//_dbname = _args[0];
			printHelp(null, "This command do not have any parameters.");
		}
		else
		{
		}
	}

	private static void printHelp(Options options, String errorStr)
	throws PipeCommandException
	{
		StringBuilder sb = new StringBuilder();

		if (errorStr != null)
		{
			sb.append("\n");
			sb.append(errorStr);
			sb.append("\n");
		}

		sb.append("\n");
		sb.append("usage: \\ccr \n");
		sb.append("\n");
		sb.append("Description:\n");
		sb.append("  - Code Completion Refresh (just like pressing the 'Refresh' button in the 'Code Completion Button')\n");
		sb.append("\n");
		
		throw new PipeCommandException(sb.toString());
	}
	
	private void init()
	throws SQLException
	{
//		System.out.println("SqlStatementCmdDdlGen.init(): _sqlOrigin = " + _sqlOrigin);
	}

	@Override
	public Statement getStatement()
	{
		return new StatementDummy();
	}

	@Override
	public boolean execute() throws SQLException
	{
		if (_queryWindow == null)
			throw new SQLException("NO _queryWindow instance.");

		DbxConnection conn = _queryWindow.getConnection();
		if (conn != null)
		{
			String vendor  = conn.getDatabaseProductName();
			String srvName = conn.getDbmsServerName();
			String dbname  = conn.getCatalog();
			
			addInfoMessage("Refreshed the Code Completion data for: Catalog Name '" + dbname + "', DBMS Vendor '" + vendor + "', Server Name '" + srvName + "'.");
		}
		else
		{
			addInfoMessage("Not currently connected to any server.");
		}

		_queryWindow.doCodeCompletionRefresh();
		
		return false; // true=We Have A ResultSet, false=No ResultSet
	}
}
