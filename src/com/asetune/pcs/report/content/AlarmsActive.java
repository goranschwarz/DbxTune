/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
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

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

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
			sb.append(toHtmlTable(_shortRstm));

			if (_fullRstm != null)
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

		return new String[] { PersistWriterBase.getTableName(null, schemaName, PersistWriterBase.ALARM_ACTIVE, null, false) };
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
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
		      "FROM " + PersistWriterBase.getTableName(conn, schemaName, PersistWriterBase.ALARM_ACTIVE, null, true) + " \n" +
		      "WHERE 1 = 1 \n" +
		      getReportPeriodSqlWhere("createTime") +
		      "ORDER BY [createTime]";

		_fullRstm = executeQuery(conn, sql, true, "Active Alarms Full");

		// Highlight sort column
		_shortRstm.setHighlightSortColumns("createTime");
	}

}
