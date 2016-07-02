package com.asetune.tools.sqlw;

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
	
	public SqlStatementAbstract(DbxConnection conn, String sqlOrigin, String dbProductName, ArrayList<JComponent> resultCompList, SqlProgressDialog progress)
	throws SQLException
	{
		_conn           = conn;
		_sqlOrigin      = sqlOrigin;
		_dbProductName  = dbProductName;
		_resultCompList = resultCompList;
		_progress       = progress;
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
