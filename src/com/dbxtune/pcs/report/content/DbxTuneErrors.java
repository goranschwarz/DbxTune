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
import java.sql.Timestamp;

import com.dbxtune.gui.GuiLogAppender;
import com.dbxtune.gui.Log4jLogRecord;
import com.dbxtune.gui.Log4jTableModel;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;

public class DbxTuneErrors
extends ReportEntryAbstract
{
//	private static Logger _logger = Logger.getLogger(DbxTuneErrors.class);
	
	private Log4jTableModel _tm = null;
	
	public DbxTuneErrors(DailySummaryReportAbstract reportingInstance)
	{
		super(reportingInstance);
	}

	@Override
	public boolean hasMinimalMessageText()
	{
		return false;
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
		// Describe this section
		sb.append("Error and Warning messages produced by this DbxTune Collector.<br>");
		sb.append("Used to check for problems/issues/bugs with the collector (<i>without having to read the errorlog</i>).<br>");
		sb.append("<br>");

		if (_tm == null)
		{
			sb.append("No Log4J Appender was installed. <br>\n");
			return;
		}
		
		if (_tm.getRowCount() == 0)
		{
			sb.append("<b>No</b> DbxTune Collector Errors or Warnings was found. <br>\n");
		}
		else
		{
			int countWithStacktrace = getRowCountWithStackTrace(_tm);
			if (countWithStacktrace > 0)
			{
				sb.append("<b>" + countWithStacktrace + "</b> DbxTune Collector Errors or Warnings with <b>attached Stacktraces</b> <br>\n");
				tableToHtmlString(_tm, sb, true); // true = only StackTrace Entries
			}
			else
			{
				sb.append("<b>No</b> DbxTune Collector Errors or Warnings with <b>attached Stacktraces</b> <br>\n");
			}
			sb.append("<br>\n");

			sb.append("Warning/Error Count: " + _tm.getRowCount() + "<br>\n");
			tableToHtmlString(_tm, sb, false); // false = all entries
		}
	}

//	@Override
//	public boolean canBeDisabled()
//	{
//		return false;
//	}

	private int getRowCountWithStackTrace(Log4jTableModel tm)
	{
		int rows = tm.getRowCount();
		int rowsWithStacktrace = 0;

		// Count Rows WITH StackTrace
		for (int r=0; r<rows; r++)
		{
			Log4jLogRecord rec = tm.getRecord(r);

			if (StringUtil.hasValue(rec.getThrownStackTrace()))
				rowsWithStacktrace++;
		}
		
		return rowsWithStacktrace;
	}

	private int tableToHtmlString(Log4jTableModel tm, Writer w, boolean onlyStacktraceEntries)
	throws IOException
	{
		int rows = tm.getRowCount();
		int rowsAppeded = 0;
		int rowsWithStacktrace = getRowCountWithStackTrace(tm);

		if (onlyStacktraceEntries && rowsWithStacktrace == 0)
		{
			w.append("No records with stacktrace was found. \n");
			return rowsAppeded;
		}
		
		w.append("<TABLE> \n");

		// Headers
		w.append("<THEAD> \n");
		w.append("  <TR> \n");
		w.append("     <TH>Time</TH> \n");
		w.append("     <TH>Level</TH> \n");
		w.append("     <TH>Thread Name</TH> \n");
		w.append("     <TH>Class Name</TH> \n");
		w.append("     <TH>Location</TH> \n");
		w.append("     <TH>Message</TH> \n");
		w.append("     <TH>Stacktrace</TH> \n");
		w.append("  </TR> \n");
		w.append("</THEAD> \n");

		// Rows
		for (int r=0; r<rows; r++) 
		{
			Log4jLogRecord rec = tm.getRecord(r);

			// Format HTML -- StackTrace
			String stackTrace = rec.getThrownStackTrace();
			if (StringUtil.isNullOrBlank(stackTrace))
			{
				stackTrace = "";
			}
			else
			{
				stackTrace = "<pre>" + stackTrace + "</pre>";
			}

			// Skip records that do NOT have stack traces
			if (onlyStacktraceEntries && StringUtil.isNullOrBlank(stackTrace))
				continue;

			// Print the ROW 
			w.append("<TBODY> \n");
			w.append("  <TR> \n");
			w.append("    <TD>")       .append("" + new Timestamp(rec.getMillis())).append("</TD> \n");  // TIME
			w.append("    <TD>")       .append("" + rec.getLevel()).append("</TD> \n");                  // LEVEL
			w.append("    <TD>")       .append(rec.getThreadDescription()).append("</TD> \n");           // Thread Name 
			w.append("    <TD>")       .append(rec.getCategory()).append("</TD> \n");                    // Class Name
			w.append("    <TD>")       .append(rec.getLocation()).append("</TD> \n");                    // Location
			w.append("    <TD>")       .append(rec.getMessage()).append("</TD> \n");                     // Message
			w.append("    <TD NOWRAP>").append(stackTrace).append("</TD> \n");                           // StackTrace
			w.append("  </TR> \n");
			w.append("</TBODY> \n");
		}

		// Footer
//		w.append("<TFOOT> \n");
//		w.append("  <TR> \n");
//		w.append("     <TD>...</TD> \n");
//		w.append("  </TR> \n");
//		w.append("</TFOOT> \n");

		w.append("</TABLE> \n");
		
		return rowsAppeded;
	}

	@Override
	public String getSubject()
	{
		return "DbxTune Collector Errorlog Warnings/Errors";
	}

	@Override
	public boolean hasIssueToReport()
	{
//		return _fullRstm.getRowCount() > 0;
		return false;
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		// nothing is done in the create...
		// all messages should already be available in the Log4j Table Model
		
		// get the table model, and at the same time clear it (or create a new model)
		_tm = GuiLogAppender.getTableModelAndCreateNew();
	}

//	@Override
//	public ResultSetTableModel getData(String whatData)
//	{
//		if ("short".equals(whatData)) return _shortRstm;
//		if ("full" .equals(whatData)) return _fullRstm;
//
//		return null;
//	}

}
