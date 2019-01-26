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
import java.util.ArrayList;

import javax.swing.JComponent;

import com.asetune.sql.SqlProgressDialog;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.tools.sqlw.msg.JLoadfileMessage;

public abstract class SqlStatementAbstract
implements SqlStatement
{
	protected DbxConnection _conn;
	protected String _sqlOrigin;
	protected String _dbProductName;
	protected ArrayList<JComponent> _resultCompList;
	protected SqlProgressDialog _progress;
	protected Component _owner;
	
	public SqlStatementAbstract(DbxConnection conn, String sqlOrigin, String dbProductName, ArrayList<JComponent> resultCompList, SqlProgressDialog progress, Component owner)
	throws SQLException
	{
		_conn           = conn;
		_sqlOrigin      = sqlOrigin;
		_dbProductName  = dbProductName;
		_resultCompList = resultCompList;
		_progress       = progress;
		_owner          = owner;
	}

	@Override
	public void readRpcReturnCodeAndOutputParameters(ArrayList<JComponent> resultCompList, boolean asPlainText) throws SQLException
	{
	}
	
	public void setProgressState(String state)
	{
		if (_progress != null)
			_progress.setState(state);
	}

	public void addResultMessage(String msg)
	{
		if (_resultCompList != null)
			_resultCompList.add(new JLoadfileMessage(msg, _sqlOrigin));
	}
}
