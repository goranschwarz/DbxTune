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
