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
package com.dbxtune.pcs.report.content;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.PersistWriterBase;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;

public class AlarmsHistory
extends ReportEntryAbstract
{
//	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	
	private ResultSetTableModel _shortRstm;
	private ResultSetTableModel _fullRstm;
//	private Exception           _problem = null;
	

	public AlarmsHistory(DailySummaryReportAbstract reportingInstance)
	{
		super(reportingInstance);
	}

	@Override
	public boolean hasMinimalMessageText()
	{
		return true;
	}

	@Override
	public boolean hasShortMessageText()
	{
		return true;
	}

//	@Override
//	public void writeShortMessageText(Writer w)
//	throws IOException
//	{
//		writeMessageText(w, false);
//	}
//
//	@Override
//	public void writeMessageText(Writer w)
//	throws IOException
//	{
//		writeMessageText(w, true);
//	}

	@Override
	public void writeMessageText(Writer sb, MessageType messageType)
	throws IOException
	{
		if (_shortRstm.getRowCount() == 0)
		{
			sb.append("No Alarms has been reported <br> \n");
		}
		else
		{
			// Get a description of this section, and column names
			sb.append(getSectionDescriptionHtml(_shortRstm, true));
			
			sb.append("Alarm Count in period: " + _fullRstm.getRowCount() + "<br>\n");
			sb.append(toHtmlTable(_shortRstm));
			
//			if (isFullMessageType() && _fullRstm != null)
			if (_fullRstm != null && isFullMessageType())
			{
				// Make output more readable, in a 2 column table
				// put "xmp" tags around the data: <xmp>cellContent</xmp>, for some columns
				Map<String, String> colNameValueTagMap = null;
//				Map<String, String> colNameValueTagMap = new HashMap<>();
//				colNameValueTagMap.put("extendedDescription",     "xmp");
//				colNameValueTagMap.put("lastExtendedDescription", "xmp");
//TODO; // make formatter that are intelligent and can see if it's HTML or plain text that was stored (otherwise we wont see GRAPHS/CHARTS)
//TODO; // Should we really leave this out for SHORT message ???

				String  divId       = "alarmHistoryDetails";
				boolean showAtStart = false;
				String  htmlContent = _fullRstm.toHtmlTablesVerticalString("sortable", colNameValueTagMap);

				String showHideDiv = "<br>" + createShowHideDiv(divId, showAtStart, "Show/Hide Alarm History Details...", htmlContent);

				sb.append( msOutlookAlternateText(showHideDiv, "Alarm History Details", null) );
			}
		}
	}

	@Override
	public boolean canBeDisabled()
	{
		return false;
	}

	@Override
	public String getSubject()
	{
		return "Alarm History";
	}

	@Override
	public boolean hasIssueToReport()
	{
		if (_fullRstm == null)
			return false;

		return _fullRstm.getRowCount() > 0;
	}


	@Override
	public String[] getMandatoryTables()
	{
		String schemaName = getReportingInstance().getDbmsSchemaName();

		return new String[] { PersistWriterBase.getTableName(null, schemaName, PersistWriterBase.ALARM_HISTORY, null, false) };
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
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
		String schemaName = getReportingInstance().getDbmsSchemaName();
		
		// Get Alarms
		sql = "select [action], [duration], [eventTime], [alarmClass], [serviceInfo], [extraInfo], [severity], [state], [description] \n" +
		      "from " + PersistWriterBase.getTableName(conn, schemaName, PersistWriterBase.ALARM_HISTORY, null, true) + " \n" +
		      "where [action] not in('END-OF-SCAN', 'RE-RAISE') \n" +
		      getReportPeriodSqlWhere("eventTime") +
		      "order by [eventTime]";
		
		_shortRstm = executeQuery(conn, sql, true, "Alarm History Short");
		
		// Describe the table
		setSectionDescription(_shortRstm);
	}

	public void getAlarmsHistoryFull(DbxConnection conn)
	{
		String sql;
		String schemaName = getReportingInstance().getDbmsSchemaName();
		
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
		      "from " + PersistWriterBase.getTableName(conn, schemaName, PersistWriterBase.ALARM_HISTORY, null, true) + " \n" +
		      "where [action] not in('END-OF-SCAN', 'RE-RAISE') \n" +
		      getReportPeriodSqlWhere("eventTime") +
		      "order by [eventTime]";
		
		// Note: Truncate any cells above 128K
		boolean truncateLongCells = true;

		_fullRstm = executeQuery(conn, sql, true, "Alarm History Full", truncateLongCells);

		// Highlight sort column
		_shortRstm.setHighlightSortColumns("eventTime");
	}
}
