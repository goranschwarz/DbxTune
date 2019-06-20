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

import java.util.HashMap;
import java.util.Map;

import com.asetune.gui.ResultSetTableModel;
import com.asetune.pcs.PersistWriterBase;
import com.asetune.pcs.report.DailySummaryReportAbstract;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;

public class AlarmsHistory
extends ReportEntryAbstract
{
//	private static Logger _logger = Logger.getLogger(AlarmsHistory.class);
	
	private ResultSetTableModel _shortRstm;
	private ResultSetTableModel _fullRstm;
//	private Exception           _problem = null;
	

	public AlarmsHistory(DailySummaryReportAbstract reportingInstance)
	{
		super(reportingInstance);
	}

	@Override
	public String getMsgAsText()
	{
		StringBuilder sb = new StringBuilder();

		if (_shortRstm.getRowCount() == 0)
		{
			sb.append("No Alarms has been reported \n");
		}
		else
		{
			sb.append("Alarm Count in period: ").append(_fullRstm.getRowCount()).append("\n");
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
			sb.append("No Alarms has been reported <br> \n");
		}
		else
		{
			// Get a description of this section, and column names
			sb.append(getSectionDescriptionHtml(_shortRstm, true));
			
			sb.append("Alarm Count in period: ").append(_fullRstm.getRowCount()).append("<br>\n");
			sb.append(_shortRstm.toHtmlTableString("sortable"));
			
			// Make output more readable, in a 2 column table
			// put "xmp" tags around the data: <xmp>cellContent</xmp>, for some columns
			Map<String, String> colNameValueTagMap = new HashMap<>();
			colNameValueTagMap.put("extendedDescription",     "xmp");
			colNameValueTagMap.put("lastExtendedDescription", "xmp");

			sb.append(_fullRstm.toHtmlTablesVerticalString("sortable", colNameValueTagMap));
		}

		if (hasProblem())
			sb.append("<pre>").append(getProblem()).append("</pre> \n");

		sb.append("\n<br>");

		return sb.toString();
	}

	@Override
	public String getSubject()
	{
		return "Alarm History";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return _fullRstm.getRowCount() > 0;
	}


	@Override
	public void create(DbxConnection conn, String srvName, Configuration conf)
	{
		getAlarmsHistoryShort(conn);
		getAlarmsHistoryFull(conn);
	}

	@Override
	public ResultSetTableModel getData(String whatData)
	{
		if ("short".equals(whatData)) return _shortRstm;
		if ("full" .equals(whatData)) return _fullRstm;

		return null;
	}
	

	/**
	 * Set descriptions for the table, and the columns
	 */
	private void setSectionDescription(ResultSetTableModel rstm)
	{
		if (rstm == null)
			return;
		
		rstm.setDescription("This is a description of all Alarms that was raised/closed for the report period.<br>" +
				"First a <i>short</i> list will be displayed (in tabular format). <br>" +
				"Second a <i>detailed</i> list will be displayed (one 2 column table for each row in the <i>short</i> list). <br>" +
				"");

//		_shortRstm.setColumnDescription(colName, desc);
	}

	public void getAlarmsHistoryShort(DbxConnection conn)
	{
		String sql;

		// Get Alarms
		sql = "select [action], [duration], [eventTime], [alarmClass], [serviceInfo], [extraInfo], [severity], [state], [description] \n" +
		      "from ["+PersistWriterBase.getTableName(conn, PersistWriterBase.ALARM_HISTORY, null, false) + "]\n" +
		      "where [action] not in('END-OF-SCAN', 'RE-RAISE') \n" +
		      "order by [eventTime]";
		
		_shortRstm = executeQuery(conn, sql, true, "Alarm History Short");
		
		// Describe the table
		setSectionDescription(_shortRstm);
	}

	public void getAlarmsHistoryFull(DbxConnection conn)
	{
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
		
		_fullRstm = executeQuery(conn, sql, true, "Alarm History Full");
	}
}
