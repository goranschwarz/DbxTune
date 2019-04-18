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
package com.asetune.tools.sqlw;

import java.awt.Component;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import javax.swing.JComponent;

import org.apache.commons.cli.Options;

import com.asetune.sql.SqlProgressDialog;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.pipe.PipeCommandException;
import com.asetune.utils.StringUtil;

public class SqlStatementCmdConnect 
extends SqlStatementAbstract
{
//	private static Logger _logger = Logger.getLogger(SqlStatementCmdTabDiff.class);

	private String[] _args = null;
//	private String _originCmd = null;
	
	private String _profileName;

	public SqlStatementCmdConnect(DbxConnection conn, String sqlOrigin, String dbProductName, ArrayList<JComponent> resultCompList, SqlProgressDialog progress, Component owner, QueryWindow queryWindow)
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
		String params = input.replace("\\connect", "").trim();

		_args = StringUtil.translateCommandline(params, false);

		if (_args.length >= 1)
		{
			_profileName = _args[0];
		}
		else
		{
			printHelp(null, "Please specify some parameters.");
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
		sb.append("usage: \\connect profileName\n");
		sb.append("\n");
		sb.append("Description:\n");
		sb.append("  - Connect to any DBMS (just like open the connection dialog, and press a ConnectionProfile)\n");
		sb.append("    NOTE: If you are already connected 'somewhere', you will first be disconnected.\n");
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

		_queryWindow.doConnect(_profileName);
		
		return false; // true=We Have A ResultSet, false=No ResultSet
	}
}
