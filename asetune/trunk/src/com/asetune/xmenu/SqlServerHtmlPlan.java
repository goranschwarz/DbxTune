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
package com.asetune.xmenu;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.swing.JOptionPane;

import com.asetune.sql.showplan.ShowplanHtmlView;
import com.asetune.sql.showplan.ShowplanHtmlView.Type;
import com.asetune.utils.StringUtil;

public class SqlServerHtmlPlan
extends XmenuActionBase 
{
//	private static Logger _logger = Logger.getLogger(SqlServerHtmlPlan.class);
	private Connection _conn = null;
//	private String     _planHandle = null;
//	private boolean    _closeConnOnExit;

	/**
	 * 
	 */
	public SqlServerHtmlPlan() 
	{
		super();
	}

	/**
	 * @see com.sybase.jisql.xmenu.XmenuActionBase#doWork()
	 */
	@Override 
	public void doWork() 
	{
		_conn = getConnection();
		String inputText = getParamValue(0);
//		_closeConnOnExit = isCloseConnOnExit();

		String queryPlan = null;
		if (StringUtil.hasValue(inputText))
		{
			if (inputText.startsWith("<ShowPlanXML "))
			{
				queryPlan = inputText;
			}
			else
			{
				queryPlan = getPlan(inputText);
			}
		}

		if (StringUtil.hasValue(queryPlan))
		{
			ShowplanHtmlView.show(Type.SQLSERVER, queryPlan);
		}
	}


	public String getPlan(String planHandle)
	{
		String sqlStatement = "select query_plan from sys.dm_exec_query_plan("+planHandle+")";

		String query_plan = null;
		
		try
		{
			Statement statement = _conn.createStatement();
			ResultSet rs = statement.executeQuery(sqlStatement);
			while(rs.next())
			{
//				String dbid       = rs.getString(1);
//				String objectid   = rs.getString(2);
//				String number     = rs.getString(3);
//				String encrypted  = rs.getString(4);
//				String query_plan = rs.getString(5);

				query_plan = rs.getString("query_plan");
			}
			rs.close();
		}
		catch (Exception e)
		{
			JOptionPane.showMessageDialog(null, "Executing SQL command '"+sqlStatement+"'. Found the following error:\n."+e, "Error", JOptionPane.ERROR_MESSAGE);
		}

		return query_plan;
	}
}
