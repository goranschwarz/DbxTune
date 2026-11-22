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

import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.PersistWriterBase;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;

public class AlarmsActive
extends ReportEntryAbstract
{
//	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	
	private ResultSetTableModel _shortRstm;
	private ResultSetTableModel _fullRstm;
	private boolean             _hasAlarmAction = false; // column 'alarmAction' does not exist in older recordings
//	private Exception           _problem = null;
	

	public AlarmsActive(DailySummaryReportAbstract reportingInstance)
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
//		writeMessageText(w);
//	}

	@Override
	public void writeMessageText(Writer sb, MessageType messageType)
	throws IOException
	{
		if (_shortRstm.getRowCount() == 0)
		{
			sb.append("No Active Alarms <br>\n");
		}
		else
		{
			// Get a description of this section, and column names
			sb.append(getSectionDescriptionHtml(_shortRstm, true));
			
			sb.append("Active Alarm Count: " + _fullRstm.getRowCount() + "<br>\n");
			sb.append(_shortRstm.toHtmlTableString("sortable", true, true, null, new AlarmBadgeTableStringRenderer(true)));

			if (_fullRstm != null && isFullMessageType()) 
			{
				// Make output more readable, in a 2 column table
				// put "xmp" tags around the data: <xmp>cellContent</xmp>, for some columns
				Map<String, String> colNameValueTagMap = null;
//				Map<String, String> colNameValueTagMap = new HashMap<>();
//				colNameValueTagMap.put("extendedDescription",     "xmp");
//				colNameValueTagMap.put("lastExtendedDescription", "xmp");

				String  divId       = "alarmActiveDetails";
				boolean showAtStart = false;
				String  htmlContent = _fullRstm.toHtmlTablesVerticalString("sortable", colNameValueTagMap);

				String showHideDiv = "<br>" + createShowHideDiv(divId, showAtStart, "Show/Hide Active Alarm Details...", htmlContent);

				sb.append( msOutlookAlternateText(showHideDiv, "Active Alarm Details", null) );
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
		return "Active Alarms";
	}

	@Override
	public IssueLevel getIssueLevel()
	{
		if (_shortRstm == null || _shortRstm.getRowCount() == 0)
			return IssueLevel.NONE;

		return _activeCountByLevel[0] > 0 ? IssueLevel.ISSUE : IssueLevel.FYI;
	}

	@Override
	public String getIssueReason()
	{
		return AlarmsHistory.issueReason(_activeCountByLevel, "still active", _shortRstm == null ? 0 : _shortRstm.getRowCount());
	}
	private int[] _activeCountByLevel = new int[2]; // [0] = ISSUE, [1] = FYI, see AlarmsHistory.countByIssueLevel()

	@Override
	public java.util.Map<String, String> getKeyFacts()
	{
		int cnt = _shortRstm == null ? 0 : _shortRstm.getRowCount();
		return java.util.Map.of("Active alarms", "<b>" + cnt + "</b>");
	}

	@Override
	public String getLlmDigest()
	{
		// extraInfo = the alarm's object (database, table, ...); NOT the description (it can hold SQL text)
		return llmDigestRows(_shortRstm, 25, "createTime", "alarmClass", "alarmAction", "severity", "extraInfo", "state");
	}

	@Override
	public String[] getMandatoryTables()
	{
		String schemaName = getReportingInstance().getDbmsSchemaName();

		return new String[] { PersistWriterBase.getTableName(null, schemaName, PersistWriterBase.ALARM_ACTIVE, null, false) };
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		String schemaName = getReportingInstance().getDbmsSchemaName();
		String tabName    = PersistWriterBase.getTableName(conn, schemaName, PersistWriterBase.ALARM_ACTIVE, null, true);
		_hasAlarmAction = executeQuery(conn, "select * from " + tabName + " where 1 = 2", true, "metadata").hasColumnNoCase("alarmAction"); // not in older recordings

		getAlarmsActiveShort(conn);
		getAlarmsActiveFull(conn);

		// Active alarms per severity and action: decides the Issue Level (NTR / FYI / ISSUE). Same rows as _shortRstm (the list that is shown)
		String sql = "select [severity], " + (_hasAlarmAction ? "[alarmAction]" : "cast(null as varchar(10))") + " as [alarmAction], count(*) as [cnt] \n" +
		             "from " + tabName + " \n" +
		             "where 1 = 1 \n" +
		             "group by [severity]" + (_hasAlarmAction ? ", [alarmAction]" : "") + " \n";
		_activeCountByLevel = AlarmsHistory.countByIssueLevel( executeQuery(conn, sql, true, "Active Alarms Count by Level") );
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
		
		rstm.setDescription("This is a description of all <b>ACTIVE</b> Alarms during the time the report was created.<br>" +
				"First a <i>short</i> list will be displayed (in tabular format). <br>" +
				"Second a <i>detailed</i> list will be displayed (one 2 column table for each row in the <i>short</i> list). <br>" +
				"");
		
//		rstm.setColumnDescription(colName, desc);
	}

	private void getAlarmsActiveShort(DbxConnection conn)
	{
		String schemaName   = getReportingInstance().getDbmsSchemaName();
//		String schemaPrefix = getReportingInstance().getDbmsSchemaNameSqlPrefix();
		String sql;

		// Get Alarms
		sql = "SELECT [createTime], [alarmClass], [serviceInfo], [extraInfo], [severity], [state], [description] \n" +
		      (_hasAlarmAction ? "    ,[alarmAction] \n" : "") + // shown as a tag after 'alarmClass
		      "FROM " + PersistWriterBase.getTableName(conn, schemaName, PersistWriterBase.ALARM_ACTIVE, null, true) + " \n" +
		      "ORDER BY [createTime]";

		_shortRstm = executeQuery(conn, sql, true, "Active Alarms Short");

		// Describe the table
		setSectionDescription(_shortRstm);
	}

	private void getAlarmsActiveFull(DbxConnection conn)
	{
		String schemaName   = getReportingInstance().getDbmsSchemaName();
//		String schemaPrefix = getReportingInstance().getDbmsSchemaNameSqlPrefix();
		String sql;

		// Get Alarms
		sql = "SELECT \n" +
		      "     [createTime],             \n" +
		      "     [alarmClass],             \n" +
		      (_hasAlarmAction ? "     [alarmAction],            \n" : "") +
		      "     [serviceType],            \n" +
		      "     [serviceName],            \n" +
		      "     [serviceInfo],            \n" +
		      "     [extraInfo],              \n" +
		      "     [category],               \n" +
		      "     [severity],               \n" +
		      "     [state],                  \n" +
		      "     [alarmId],                \n" +
		      "     [repeatCnt],              \n" +
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
		      "FROM " + PersistWriterBase.getTableName(conn, schemaName, PersistWriterBase.ALARM_ACTIVE, null, true) + " \n" +
		      "WHERE 1 = 1 \n" +
		      getReportPeriodSqlWhere("createTime") +
		      "ORDER BY [createTime]";

		_fullRstm = executeQuery(conn, sql, true, "Active Alarms Full");

		// Highlight sort column
		_shortRstm.setHighlightSortColumns("createTime");
	}

}
