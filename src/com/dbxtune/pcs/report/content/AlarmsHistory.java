/*******************************************************************************
 * Copyright (C) 2010-2027 Goran Schwarz
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

import org.apache.commons.text.StringEscapeUtils;

import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.PersistWriterBase;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;

public class AlarmsHistory
extends ReportEntryAbstract
{
//	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	
	private ResultSetTableModel _summaryRstm;
	private ResultSetTableModel _shortRstm;
	private ResultSetTableModel _fullRstm;

	/** Above this many raise/cancel rows the flat list is collapsed (the summary is shown first anyway) */
	private static final int SHORT_LIST_SHOW_MAX_ROWS = 30;
	private boolean             _hasCancelDescription = false; // column 'cancelDescription' does not exist in older recordings
	private boolean             _hasAlarmAction       = false; // column 'alarmAction' does not exist in older recordings
	private int[]               _raiseCountByLevel    = new int[2]; // [0] = ISSUE, [1] = FYI, see countByIssueLevel()
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
			
			// Summary first: one row per alarm and object, so 90 raises of the same alarm is 1 line
			if (_summaryRstm != null && _summaryRstm.getRowCount() > 0)
			{
				sb.append("<b>Summary:</b> one row per alarm and object (" + _summaryRstm.getRowCount() + " rows). "
						+ "Column 'alarmAction': <code>ALARM</code> = sent to the alarm writers (mail, Teams...), <code>REPORT</code> = only recorded here (property '&lt;AlarmEventClass&gt;.action').<br>\n");
				sb.append(toHtmlTable(_summaryRstm));
				sb.append("<br>\n");
			}

			sb.append("Alarm Count in period: " + _fullRstm.getRowCount() + "<br>\n");
			if (_shortRstm.getRowCount() <= SHORT_LIST_SHOW_MAX_ROWS)
				sb.append(toHtmlTable(_shortRstm));
			else
				sb.append( msOutlookAlternateText(createShowHideDiv("alarmHistoryShort", false, "Show/Hide all " + _shortRstm.getRowCount() + " Alarm raise/cancel rows...", toHtmlTable(_shortRstm)), "Alarm History", null) );
			
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
	public IssueLevel getIssueLevel()
	{
		if (_shortRstm == null || _shortRstm.getRowCount() == 0)
			return IssueLevel.NONE;

		// Only CANCEL rows (raised before the period) is also FYI
		return _raiseCountByLevel[0] > 0 ? IssueLevel.ISSUE : IssueLevel.FYI;
	}

	@Override
	public String getIssueReason()
	{
		return issueReason(_raiseCountByLevel, "raised", _shortRstm == null ? 0 : _shortRstm.getRowCount());
	}

	@Override
	public java.util.Map<String, String> getKeyFacts()
	{
		int issue = _raiseCountByLevel[0];
		int fyi   = _raiseCountByLevel[1];
		return java.util.Map.of("Alarms raised", "<b>" + (issue + fyi) + "</b> <span style='color:#5d6878;'>(" + issue + " WARNING/ERROR)</span>");
	}

	/**
	 * Count alarms per level, from rows with columns: severity, alarmAction, cnt
	 * <ul>
	 *   <li>[0] ISSUE: WARNING/ERROR alarms that were sent to the alarm writers (alarmAction ALARM, or NULL/missing in older recordings)</li>
	 *   <li>[1] FYI:   INFO alarms, or alarms that were only recorded (alarmAction REPORT)</li>
	 * </ul>
	 */
	static int[] countByIssueLevel(ResultSetTableModel rstm)
	{
		int[] count = new int[2];
		if (rstm == null)
			return count;

		for (int r = 0; r < rstm.getRowCount(); r++)
		{
			String severity = rstm.getValueAsString (r, "severity",    false, "");
			String action   = rstm.getValueAsString (r, "alarmAction", false, "");
			int    cnt      = rstm.getValueAsInteger(r, "cnt",         false, 0);

			boolean isFyi = "INFO".equalsIgnoreCase(severity) || "REPORT".equalsIgnoreCase(action);
			count[isFyi ? 1 : 0] += cnt;
		}
		return count;
	}

	/** eg: "2 alarm(s) raised (WARNING/ERROR), 5 FYI alarm(s) (INFO or action REPORT)" */
	static String issueReason(int[] countByLevel, String verb, int rowCount)
	{
		if (countByLevel[0] == 0 && countByLevel[1] == 0)
			return rowCount + " alarm cancel(s), of alarms raised before the report period";

		String reason = "";
		if (countByLevel[0] > 0)
			reason += countByLevel[0] + " alarm(s) " + verb + " (WARNING/ERROR)";
		if (countByLevel[1] > 0)
			reason += (reason.isEmpty() ? "" : ", ") + countByLevel[1] + " FYI alarm(s) (INFO, or action REPORT)";
		return reason;
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
		// just to get Column names
		String schemaName = getReportingInstance().getDbmsSchemaName();
		String dummySql = "select * from " + PersistWriterBase.getTableName(conn, schemaName, PersistWriterBase.ALARM_HISTORY, null, true) + " where 1 = 2";
		ResultSetTableModel dummyRstm = executeQuery(conn, dummySql, true, "metadata");
		_hasCancelDescription = dummyRstm.hasColumnNoCase("cancelDescription");
		_hasAlarmAction       = dummyRstm.hasColumnNoCase("alarmAction");

		getAlarmsHistorySummary(conn);
		getAlarmsHistoryShort(conn);
		getAlarmsHistoryFull(conn);

		// Raised alarms per severity and action: decides the Issue Level (NTR / FYI / ISSUE)
		String sql = "select [severity], " + (_hasAlarmAction ? "[alarmAction]" : "cast(null as varchar(10))") + " as [alarmAction], count(*) as [cnt] \n" +
		             "from " + PersistWriterBase.getTableName(conn, schemaName, PersistWriterBase.ALARM_HISTORY, null, true) + " \n" +
		             "where [action] = 'RAISE' \n" +
		             getReportPeriodSqlWhere("eventTime") +
		             "group by [severity]" + (_hasAlarmAction ? ", [alarmAction]" : "") + " \n";
		_raiseCountByLevel = countByIssueLevel( executeQuery(conn, sql, true, "Alarm History Count by Level") );
	}

	/** One row per alarm and object (alarmClass + serviceInfo + extraInfo): how many raises, first and last */
	public void getAlarmsHistorySummary(DbxConnection conn)
	{
		String schemaName = getReportingInstance().getDbmsSchemaName();

		// 'action': what the collector did with the alarm: ALARM = sent, REPORT = only recorded (column 'alarmAction'; older recordings have no such column = ALARM)
		String actionCol = _hasAlarmAction ? "coalesce(max([alarmAction]), 'ALARM')" : "cast('ALARM' as varchar(10))";

		String sql = "select [alarmClass], " + actionCol + " as [alarmAction], max([severity]) as [severity], [serviceInfo], [extraInfo], count(*) as [raiseCount], min([eventTime]) as [firstRaise], max([eventTime]) as [lastRaise] \n" +
		             "from " + PersistWriterBase.getTableName(conn, schemaName, PersistWriterBase.ALARM_HISTORY, null, true) + " \n" +
		             "where [action] = 'RAISE' \n" +
		             getReportPeriodSqlWhere("eventTime") +
		             "group by [alarmClass], [serviceInfo], [extraInfo] \n" +
		             "order by count(*) desc, min([eventTime])";

		_summaryRstm = executeQuery(conn, sql, true, "Alarm History Summary");
	}

	@Override
	public ResultSetTableModel getData(String whatData)
	{
		if ("summary".equals(whatData)) return _summaryRstm;
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
		      (_hasCancelDescription ? "    ,[cancelDescription] \n" : "") +
		      "from " + PersistWriterBase.getTableName(conn, schemaName, PersistWriterBase.ALARM_HISTORY, null, true) + " \n" +
		      "where [action] not in('END-OF-SCAN', 'RE-RAISE') \n" +
		      getReportPeriodSqlWhere("eventTime") +
		      "order by [eventTime]";
		
		_shortRstm = executeQuery(conn, sql, true, "Alarm History Short");
		appendCancelDescription(_shortRstm);
		
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
		      "     [alarmId],                \n" +
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
		      "     [lastExtendedDescription],\n" +
		      "     [alarmOptions]            \n" +
		      (_hasCancelDescription ? "    ,[cancelDescription]       \n" : "") +
		      "from " + PersistWriterBase.getTableName(conn, schemaName, PersistWriterBase.ALARM_HISTORY, null, true) + " \n" +
		      "where [action] not in('END-OF-SCAN', 'RE-RAISE') \n" +
		      getReportPeriodSqlWhere("eventTime") +
		      "order by [eventTime]";
		
		// Note: Truncate any cells above 128K
		boolean truncateLongCells = true;

		_fullRstm = executeQuery(conn, sql, true, "Alarm History Full", truncateLongCells);
		appendCancelDescription(_fullRstm);

		// Highlight sort column
		_shortRstm.setHighlightSortColumns("eventTime");
	}

	/**
	 * On CANCEL rows: add the 'cancelDescription' (how things looked when the alarm went away) at the end of 'description',
	 * then remove the 'cancelDescription' column (it's shown in 'description' now)
	 */
	private void appendCancelDescription(ResultSetTableModel rstm)
	{
		if (rstm == null || !rstm.hasColumnNoCase("cancelDescription"))
			return;

		for (int r = 0; r < rstm.getRowCount(); r++)
		{
			String action     = rstm.getValueAsString(r, "action",            false, "");
			String cancelDesc = rstm.getValueAsString(r, "cancelDescription", false, "");

			if ("CANCEL".equalsIgnoreCase(action) && !cancelDesc.isBlank())
			{
				String desc = rstm.getValueAsString(r, "description", false, "");
				rstm.setValueAtWithOverride(desc + "<br><b>Cancel Description:</b> " + StringEscapeUtils.escapeHtml4(cancelDesc), r, "description");
			}
		}
		rstm.removeColumnNoCase("cancelDescription");
	}
}
