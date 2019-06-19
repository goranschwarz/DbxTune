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
package com.asetune.pcs.report.content;

import com.asetune.gui.ResultSetTableModel;
import com.asetune.pcs.PersistWriterBase;
import com.asetune.pcs.report.DailySummaryReportAbstract;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;

public class AlarmsActive
extends ReportEntryAbstract
{
//	private static Logger _logger = Logger.getLogger(AlarmsActive.class);
	
	private ResultSetTableModel _shortRstm;
	private ResultSetTableModel _fullRstm;
//	private Exception           _problem = null;
	

	public AlarmsActive(DailySummaryReportAbstract reportingInstance)
	{
		super(reportingInstance);
	}

	@Override
	public String getMsgAsText()
	{
		StringBuilder sb = new StringBuilder();

		if (_shortRstm.getRowCount() == 0)
		{
			sb.append("No Active Alarms \n");
		}
		else
		{
			sb.append("Active Alarm Count: ").append(_fullRstm.getRowCount()).append("\n");
			sb.append(_shortRstm.toAsciiTableString());
			sb.append(_fullRstm.toAsciiTablesVerticalString());
		}

		if (hasProblem())
			sb.append(getProblem());
		
		return sb.toString();
	}

	@Override
	public String getMsgAsHtml()
	{
		StringBuilder sb = new StringBuilder();

		if (_shortRstm.getRowCount() == 0)
		{
			sb.append("No Active Alarms <br>\n");
		}
		else
		{
			sb.append("Active Alarm Count: ").append(_fullRstm.getRowCount()).append("<br>\n");
			sb.append(_shortRstm.toHtmlTableString("sortable"));
			sb.append(_fullRstm.toHtmlTablesVerticalString("sortable"));
		}

		if (hasProblem())
			sb.append("<pre>").append(getProblem()).append("</pre> \n");

		sb.append("\n<br>");

		return sb.toString();
	}

	@Override
	public String getSubject()
	{
		return "Active Alarms";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return _fullRstm.getRowCount() > 0;
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration conf)
	{
		getAlarmsActiveShort(conn);
		getAlarmsActiveFull(conn);
	}

	@Override
	public ResultSetTableModel getData(String whatData)
	{
		if ("short".equals(whatData)) return _shortRstm;
		if ("full" .equals(whatData)) return _fullRstm;

		return null;
	}

	private void getAlarmsActiveShort(DbxConnection conn)
	{
		String sql;

		// Get Alarms
		sql = "select [createTime], [alarmClass], [serviceInfo], [extraInfo], [severity], [state], [description] \n" +
		      "from ["+PersistWriterBase.getTableName(conn, PersistWriterBase.ALARM_ACTIVE, null, false) + "] \n" +
		      "order by [createTime]";

		_shortRstm = executeQuery(conn, sql, true, "Active Alarms Short");

//		sql = conn.quotifySqlString(sql);
//		try ( Statement stmnt = conn.createStatement() )
//		{
//			// Unlimited execution time
//			stmnt.setQueryTimeout(0);
//			try ( ResultSet rs = stmnt.executeQuery(sql) )
//			{
////				_shortRstm = new ResultSetTableModel(rs, "Active Alarms Short");
//				_shortRstm = createResultSetTableModel(rs, "Active Alarms Short");
//				
//				if (_logger.isDebugEnabled())
//					_logger.debug("_alarmsActiveShortRstm.getRowCount()="+ _shortRstm.getRowCount());
//			}
//		}
//		catch(SQLException ex)
//		{
//			_problem = ex;
//
//			_shortRstm = ResultSetTableModel.createEmpty("Active Alarms Short");
//			_logger.warn("Problems getting Alarms Short: " + ex);
//		}
	}

	private void getAlarmsActiveFull(DbxConnection conn)
	{
		String sql;

		// Get Alarms
		sql = "select \n" +
		      "     [createTime],             \n" +
		      "     [alarmClass],             \n" +
		      "     [serviceType],            \n" +
		      "     [serviceName],            \n" +
		      "     [serviceInfo],            \n" +
		      "     [extraInfo],              \n" +
		      "     [category],               \n" +
		      "     [severity],               \n" +
		      "     [state],                  \n" +
		      "     [repeatCnt],              \n" +
		      "     [cancelTime],             \n" +
		      "     [timeToLive],             \n" +
		      "     [threshold],              \n" +
		      "     [data],                   \n" +
		      "     [lastData],               \n" +
		      "     [description],            \n" +
		      "     [lastDescription],        \n" +
		      "     [extendedDescription],    \n" +
		      "     [lastExtendedDescription] \n" +
		      "from ["+PersistWriterBase.getTableName(conn, PersistWriterBase.ALARM_ACTIVE, null, false) + "]\n" +
		      "order by [createTime]";
		
		_fullRstm = executeQuery(conn, sql, true, "Active Alarms Full");
		
//		sql = conn.quotifySqlString(sql);
//		try ( Statement stmnt = conn.createStatement() )
//		{
//			// Unlimited execution time
//			stmnt.setQueryTimeout(0);
//			try ( ResultSet rs = stmnt.executeQuery(sql) )
//			{
////				_fullRstm = new ResultSetTableModel(rs, "Active Alarms Full");
//				_fullRstm = createResultSetTableModel(rs, "Active Alarms Full");
//				
//				if (_logger.isDebugEnabled())
//					_logger.debug("_alarmsActiveFullRstm.getRowCount()="+ _fullRstm.getRowCount());
//			}
//		}
//		catch(SQLException ex)
//		{
//			_problem = ex;
//
//			_fullRstm = ResultSetTableModel.createEmpty("Active Alarms");
//			_logger.warn("Problems getting Alarms Full: " + ex);
//		}
	}

}
