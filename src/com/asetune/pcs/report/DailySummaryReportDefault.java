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
package com.asetune.pcs.report;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

import com.asetune.gui.ResultSetTableModel;
import com.asetune.pcs.PersistWriterBase;
import com.asetune.pcs.report.content.DailySummaryReportContent;
import com.asetune.sql.conn.DbxConnection;

public class DailySummaryReportDefault 
extends DailySummaryReportAbstract
{
	private static Logger _logger = Logger.getLogger(DailySummaryReportDefault.class);

	private ResultSetTableModel _alarmsActiveShortRstm;
	private ResultSetTableModel _alarmsActiveFullRstm;
	private Exception           _alarmsActiveProblem;
	
	private ResultSetTableModel _alarmsHistoryShortRstm;
	private ResultSetTableModel _alarmsHistoryFullRstm;
	private Exception           _alarmsHistoryProblem;

	@Override
	public void create()
	{
		DailySummaryReportContent content = new DailySummaryReportContent();
		content.setServerName( getServerName() );

		// Get Alarms (all in DB since it should be a "rolling" database)
		getAlarmsActiveShort();
		getAlarmsHistoryShort();

		getAlarmsActiveFull();
		getAlarmsHistoryFull();

		// Create and set TEXT/HTML Content
		content.setReportAsText(createText());
		content.setReportAsHtml(createHtml());

		content.setNothingToReport(isEmpty());

		// set the content
		setReportContent(content);
	}

	private boolean isEmpty()
	{
		return    _alarmsActiveFullRstm.getRowCount() == 0
		       && _alarmsHistoryFullRstm.getRowCount() == 0;
	}

	private void getAlarmsActiveShort()
	{
		DbxConnection conn = getConnection();
		String sql;

		// Get Alarms
		sql = "select [createTime], [alarmClass], [serviceInfo], [extraInfo], [severity], [state], [description] \n" +
		      "from ["+PersistWriterBase.getTableName(conn, PersistWriterBase.ALARM_ACTIVE, null, false) + "] \n" +
		      "order by [createTime]";
		sql = conn.quotifySqlString(sql);
		try ( Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql); )
		{
			_alarmsActiveShortRstm = new ResultSetTableModel(rs, "Active Alarms Short");
			
			if (_logger.isDebugEnabled())
				_logger.debug("_alarmsActiveShortRstm.getRowCount()="+ _alarmsActiveShortRstm.getRowCount());
		}
		catch(SQLException ex)
		{
			_alarmsActiveProblem = ex;

			_alarmsActiveShortRstm = ResultSetTableModel.createEmpty("Active Alarms Short");
			_logger.warn("Problems getting Alarms Short: " + ex);
		}
	}

	private void getAlarmsActiveFull()
	{
		DbxConnection conn = getConnection();
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
		sql = conn.quotifySqlString(sql);
		try ( Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql); )
		{
			_alarmsActiveFullRstm = new ResultSetTableModel(rs, "Active Alarms Full");
			
			if (_logger.isDebugEnabled())
				_logger.debug("_alarmsActiveFullRstm.getRowCount()="+ _alarmsActiveFullRstm.getRowCount());
		}
		catch(SQLException ex)
		{
			_alarmsActiveProblem = ex;

			_alarmsActiveFullRstm = ResultSetTableModel.createEmpty("Active Alarms");
			_logger.warn("Problems getting Alarms Full: " + ex);
		}
	}

	private void getAlarmsHistoryShort()
	{
		DbxConnection conn = getConnection();
		String sql;

		// Get Alarms
		sql = "select [action], [duration], [eventTime], [alarmClass], [serviceInfo], [extraInfo], [severity], [state], [description] \n" +
		      "from ["+PersistWriterBase.getTableName(conn, PersistWriterBase.ALARM_HISTORY, null, false) + "]\n" +
		      "where [action] not in('END-OF-SCAN', 'RE-RAISE') \n" +
		      "order by [eventTime]";
		sql = conn.quotifySqlString(sql);
		try ( Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql); )
		{
			_alarmsHistoryShortRstm = new ResultSetTableModel(rs, "Alarm History Short");
			
			if (_logger.isDebugEnabled())
				_logger.debug("_alarmsHistoryRstm.getRowCount()="+ _alarmsHistoryShortRstm.getRowCount());
		}
		catch(SQLException ex)
		{
			_alarmsHistoryProblem = ex;

			_alarmsHistoryShortRstm = ResultSetTableModel.createEmpty("Active History");
			_logger.warn("Problems getting Alarms Short: " + ex);
		}
	}

	private void getAlarmsHistoryFull()
	{
		DbxConnection conn = getConnection();
		String sql;

		// Get Alarms
		sql = "select \n" +
		      "     [action],                 \n" +
		      "     [duration],               \n" +
		      "     [eventTime],              \n" +
		      "     [alarmClass],             \n" +
		      "     [serviceType],            \n" +
		      "     [serviceName],            \n" +
		      "     [serviceInfo],            \n" +
		      "     [extraInfo],              \n" +
		      "     [category],               \n" +
		      "     [severity],               \n" +
		      "     [state],                  \n" +
		      "     [repeatCnt],              \n" +
		      "     [createTime],             \n" +
		      "     [cancelTime],             \n" +
		      "     [timeToLive],             \n" +
		      "     [threshold],              \n" +
		      "     [data],                   \n" +
		      "     [lastData],               \n" +
		      "     [description],            \n" +
		      "     [lastDescription],        \n" +
		      "     [extendedDescription],    \n" +
		      "     [lastExtendedDescription] \n" +
		      "from ["+PersistWriterBase.getTableName(conn, PersistWriterBase.ALARM_HISTORY, null, false) + "]\n" +
		      "where [action] not in('END-OF-SCAN', 'RE-RAISE') \n" +
		      "order by [eventTime]";
		sql = conn.quotifySqlString(sql);
		try ( Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql); )
		{
			_alarmsHistoryFullRstm = new ResultSetTableModel(rs, "Alarm History Full");
			
			if (_logger.isDebugEnabled())
				_logger.debug("_alarmsHistoryFullRstm.getRowCount()="+ _alarmsHistoryFullRstm.getRowCount());
		}
		catch(SQLException ex)
		{
			_alarmsHistoryProblem = ex;
			
			_alarmsHistoryFullRstm = ResultSetTableModel.createEmpty("Active History Full");
			_logger.warn("Problems getting Alarms Full: " + ex);
		}
	}

	private String createText()
	{
		StringBuilder sb = new StringBuilder();

		sb.append("Daily Summary Report for Servername: ").append(getServerName()).append("\n");
		
		sb.append("=======================================================\n");
		sb.append(" Active Alarms \n");
		sb.append("-------------------------------------------------------\n");
		if (_alarmsActiveFullRstm.getRowCount() == 0)
			sb.append("No Active Alarms\n");
		else
		{
			sb.append("Active Alarm Count: ").append(_alarmsActiveFullRstm.getRowCount()).append("\n");
			sb.append(_alarmsActiveShortRstm.toAsciiTableString());
			sb.append(_alarmsActiveFullRstm.toAsciiTablesVerticalString());
		}
		if (_alarmsActiveProblem != null)
			sb.append(_alarmsActiveProblem);
		sb.append("\n");
		
		sb.append("=======================================================\n");
		sb.append(" Alarm History \n");
		sb.append("-------------------------------------------------------\n");
		if (_alarmsHistoryFullRstm.getRowCount() == 0)
			sb.append("No Alarms has been reported\n");
		else
		{
			sb.append("Alarm Count in period: ").append(_alarmsHistoryFullRstm.getRowCount()).append("\n");
			sb.append(_alarmsHistoryShortRstm.toAsciiTableString());
			sb.append(_alarmsHistoryFullRstm.toAsciiTablesVerticalString());
		}
		if (_alarmsHistoryProblem != null)
			sb.append(_alarmsHistoryProblem);
		sb.append("\n");
		
		sb.append("\n");
		sb.append("\n");
		sb.append("--end-of-report--\n");

		return sb.toString();
	}
	
	private String createHtml()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("<html>\n");

		sb.append("\n");
		sb.append("<head> \n");
		sb.append("    <meta name='x-apple-disable-message-reformatting' />\n");
//		sb.append("    <meta name='viewport' content='width=device-width, user-scalable=no, initial-scale=1, maximum-scale=1, minimal-ui'>\n");

		sb.append("    <style> \n");
		sb.append("        body { \n");
		sb.append("            -webkit-text-size-adjust:100%; \n");
		sb.append("            -ms-text-size-adjust:100%; \n");
		sb.append("            font-family: Arial, Helvetica, sans-serif; \n");
		sb.append("        } \n");
		sb.append("        pre { \n");
		sb.append("            font-size: 9px; \n");
		sb.append("            word-wrap: none; \n");
		sb.append("            white-space: no-wrap; \n");
		sb.append("            space: nowrap; \n");
		sb.append("        } \n");
		sb.append("        table { \n");
		sb.append("            mso-table-layout-alt: fixed; \n"); // not sure about this - https://gist.github.com/webtobesocial/ac9d052595b406d5a5c1
		sb.append("            mso-table-overlap: never; \n");    // not sure about this - https://gist.github.com/webtobesocial/ac9d052595b406d5a5c1
		sb.append("            mso-table-wrap: none; \n");        // not sure about this - https://gist.github.com/webtobesocial/ac9d052595b406d5a5c1
		sb.append("            border-collapse: collapse; \n");
		sb.append("            ; \n");
		sb.append("        } \n");
		sb.append("        th {border: 1px solid black; text-align: left; padding: 2px; white-space: nowrap; background-color:gray; color:white;} \n");
		sb.append("        td {border: 1px solid black; text-align: left; padding: 2px; white-space: nowrap; } \n");
		sb.append("        tr:nth-child(even) {background-color: #f2f2f2;} \n");
		
//		sb.append("        h2 { border-bottom: 3px solid black; border-top: 3px solid black; } \n");
		sb.append("        h3 { border-bottom: 1px solid black; border-top: 1px solid black; margin-bottom:3px; } \n");
		sb.append("    </style> \n");
		sb.append("</head> \n");
		sb.append("\n");

		sb.append("<body>\n");
//		sb.append("<body style'min-width: 100%'>\n");
//		sb.append("<body style'min-width: 2048px'>\n");
//		sb.append("<body style'min-width: 1024px'>\n");
		sb.append("\n");

		sb.append("<h2>Daily Summary Report for Servername: ").append(getServerName()).append("</h2>\n");

		sb.append("<h3>Active Alarms</h3> \n");
		if (_alarmsActiveFullRstm.getRowCount() == 0)
			sb.append("No Active Alarms \n");
		else
		{
			sb.append("Active Alarm Count: ").append(_alarmsActiveFullRstm.getRowCount()).append("<br>\n");
			sb.append(_alarmsActiveShortRstm.toHtmlTableString("activeAlarmsOverview"));
			sb.append(_alarmsActiveFullRstm.toHtmlTablesVerticalString("activeAlarmsDetails"));
		}
		if (_alarmsActiveProblem != null)
			sb.append("<pre>").append(_alarmsActiveProblem).append("</pre>");
		sb.append("\n<br>");

		sb.append("<h3>Alarm History</h3> \n");
		if (_alarmsHistoryFullRstm.getRowCount() == 0)
			sb.append("No Alarms has been reported \n");
		else
		{
			sb.append("Alarm Count in period: ").append(_alarmsHistoryFullRstm.getRowCount()).append("\n");
			sb.append(_alarmsHistoryShortRstm.toHtmlTableString("historyAlarmsOverview"));
			sb.append(_alarmsHistoryFullRstm.toHtmlTablesVerticalString("historyAlarmsDetails"));
		}
		if (_alarmsHistoryProblem != null)
			sb.append("<pre>").append(_alarmsHistoryProblem).append("</pre>");
		sb.append("\n<br>");

		sb.append("\n<br>");
		sb.append("\n<br>");
		sb.append("<code>--end-of-report--</code>\n");

		sb.append("\n");
		sb.append("</body>\n");
		sb.append("</html>\n");
		return sb.toString();
	}
}
