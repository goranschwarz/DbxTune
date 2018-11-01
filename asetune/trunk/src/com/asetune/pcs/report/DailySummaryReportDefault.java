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

	private ResultSetTableModel _alarmsActiveRstm;
	private ResultSetTableModel _alarmsHistoryRstm;

	@Override
	public void create()
	{
		DailySummaryReportContent content = new DailySummaryReportContent();
		content.setServerName( getServerName() );

		// Get Alarms (all in DB since it should be a "rolling" database)
		getAlarmsActive();
		getAlarmsHistory();

		// Create and set TEXT/HTML Content
		content.setReportAsText(createText());
		content.setReportAsHtml(createHtml());

		content.setNothingToReport(isEmpty());

		// set the content
		setReportContent(content);
	}

	private boolean isEmpty()
	{
		return    _alarmsActiveRstm.getRowCount() == 0
		       && _alarmsHistoryRstm.getRowCount() == 0;
	}

	private void getAlarmsActive()
	{
		DbxConnection conn = getConnection();
		String sql;
		String q = conn.getQuotedIdentifierChar();

		// Get Alarms
		sql = "select * \n" +
		      "from #"+PersistWriterBase.getTableName(PersistWriterBase.ALARM_ACTIVE, null, false) + "#\n" +
		      "order by #createTime#";
		sql = sql.replace("#", q);
		try ( Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql); )
		{
			_alarmsActiveRstm = new ResultSetTableModel(rs, "Active Alarms");
			
			if (_logger.isDebugEnabled())
				_logger.debug("_alarmsActiveRstm.getRowCount()="+ _alarmsActiveRstm.getRowCount());
		}
		catch(SQLException ex)
		{
			_alarmsActiveRstm = ResultSetTableModel.createEmpty("Active Alarms");
			_logger.warn("Problems getting Alarms: " + ex);
		}
	}

	private void getAlarmsHistory()
	{
		DbxConnection conn = getConnection();
		String sql;
		String q = conn.getQuotedIdentifierChar();

		// Get Alarms
		sql = "select * \n" +
		      "from #"+PersistWriterBase.getTableName(PersistWriterBase.ALARM_HISTORY, null, false) + "#\n" +
		      "where #action# not in('END-OF-SCAN', 'RE-RAISE') \n" +
		      "order by #eventTime#";
		sql = sql.replace("#", q);
		try ( Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql); )
		{
			_alarmsHistoryRstm = new ResultSetTableModel(rs, "Alarm History");
			
			if (_logger.isDebugEnabled())
				_logger.debug("_alarmsHistoryRstm.getRowCount()="+ _alarmsHistoryRstm.getRowCount());
		}
		catch(SQLException ex)
		{
			_alarmsHistoryRstm = ResultSetTableModel.createEmpty("Active History");
			_logger.warn("Problems getting Alarms: " + ex);
		}
	}

	private String createText()
	{
		StringBuilder sb = new StringBuilder();

		sb.append("Daily Summary Report for Servername: ").append(getServerName()).append("\n");
		
		sb.append("=======================================================\n");
		sb.append(" Active Alarms \n");
		sb.append("-------------------------------------------------------\n");
		if (_alarmsActiveRstm.getRowCount() == 0)
			sb.append("No Active Alarms");
		else
			sb.append(_alarmsActiveRstm.toAsciiTableString());
		sb.append("\n");
		
		sb.append("=======================================================\n");
		sb.append(" Alarm History \n");
		sb.append("-------------------------------------------------------\n");
		if (_alarmsHistoryRstm.getRowCount() == 0)
			sb.append("No Alarms has been reported");
		else
			sb.append(_alarmsHistoryRstm.toAsciiTableString());
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
		if (_alarmsActiveRstm.getRowCount() == 0)
			sb.append("No Active Alarms \n");
		else
			sb.append(_alarmsActiveRstm.toHtmlTableString());
		sb.append("\n<br>");

		sb.append("<h3>Alarm History</h3> \n");
		if (_alarmsHistoryRstm.getRowCount() == 0)
			sb.append("No Alarms has been reported \n");
		else
			sb.append(_alarmsHistoryRstm.toHtmlTableString());
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
