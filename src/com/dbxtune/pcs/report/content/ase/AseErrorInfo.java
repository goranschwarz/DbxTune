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
package com.dbxtune.pcs.report.content.ase;

import java.io.IOException;
import java.io.Writer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.dbxtune.config.dict.AseErrorMessageDictionary;
import com.dbxtune.gui.ModelMissmatchException;
import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.pcs.report.content.SparklineHelper;
import com.dbxtune.pcs.report.content.SparklineHelper.DataSource;
import com.dbxtune.pcs.report.content.SparklineHelper.SparkLineParams;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;

public class AseErrorInfo extends AseAbstract
{
	private static Logger _logger = Logger.getLogger(AseErrorInfo.class);
	
	private ResultSetTableModel _shortRstm;
	private ResultSetTableModel _sqlTextRstm;
//	private Exception           _problem = null;
	private List<String>        _messages = new ArrayList<>();
	private List<String>        _miniChartJsList = new ArrayList<>();

	public AseErrorInfo(DailySummaryReportAbstract reportingInstance)
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
		return false;
	}

//	@Override
//	public void writeShortMessageText(Writer w)
//	throws IOException
//	{
//	}

	@Override
	public void writeMessageText(Writer sb, MessageType messageType)
	throws IOException
	{
		if (_messages.size() > 0)
		{
			sb.append("<b>Messages:</b> \n");
			sb.append("<ul> \n");
			for (String msg : _messages)
				sb.append("  <li>").append(msg).append("</li> \n");
			sb.append("</ul> \n");
		}

		if (_shortRstm.getRowCount() == 0)
		{
			sb.append("No Error Information <br>\n");
		}
		else
		{
			// Get a description of this section, and column names
			sb.append(getSectionDescriptionHtml(_shortRstm, true));

			sb.append("Error Information Count: " + _shortRstm.getRowCount() + "<br>\n");
			sb.append(toHtmlTable(_shortRstm));

			if (_sqlTextRstm != null)
			{
				// put "xmp" tags around the data: <xmp>cellContent</xmp>, for some columns
				Map<String, String> colNameValueTagMap = new HashMap<>();
				colNameValueTagMap.put("SQLText", "xmp");

				String  divId       = "errorSqlText";
				boolean showAtStart = false;
				String  htmlContent = _sqlTextRstm.toHtmlTableString("sortable", colNameValueTagMap);
				
				String showHideDiv = createShowHideDiv(divId, showAtStart, "Show/Hide Error SQL Text, for above errors (all text's may not be available)...", htmlContent);

				// Compose special condition for Microsoft Outlook
				sb.append(msOutlookAlternateText(showHideDiv, "Error Text", null));
			}
		}
		
		// Write JavaScript code for CPU SparkLine
		if (isFullMessageType())
		{
			for (String str : _miniChartJsList)
				sb.append(str);
		}
	}

	@Override
	public String getSubject()
	{
		return "Error Information (order by: ErrorCount)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}

	/**
	 * Set descriptions for the table, and the columns
	 */
	private void setSectionDescription(ResultSetTableModel rstm)
	{
		if (rstm == null)
			return;
		
		// Section description
		rstm.setDescription(
				"Errors that ended up in the MDA Table 'monSysStatements' with 'ErrorStatus' != 0 <br>" +
				"This can be used to drilldown or pinpoint troublesome SQL Statements. <br>" +
				"<br>" +
				"<b>Note</b>: To view the SQL Statement that was responsible for the error(s), There is a table <i>which is collapsed</i> where they can be viewed.<br>" +
				"&emsp; You can also connect to the Detailed Recording Database. (Using the Desktop Version of " + getReportingInstance().getRecDbxAppName() + "), use 'Tools-&gt;Captue SQL Offline View...' <br>" +
				"");

		// Columns description
		rstm.setColumnDescription("ErrorStatus" , "ASE Error Number");
		rstm.setColumnDescription("ErrorCount"  , "Number of records found");
		rstm.setColumnDescription("SpidCount"   , "Number if different SPID's this message occurred for");
		rstm.setColumnDescription("FirstEntry"  , "Time of first occurrence, this day");
		rstm.setColumnDescription("LastEntry"   , "Time of last occurrence, this day");
		rstm.setColumnDescription("Duration"    , "To pinpoint if the errors occurred during a specific time on the day");
		rstm.setColumnDescription("ErrorMessage", "ASE Message description (from master.dbo.sysmessages)");
	}

	@Override
	public String[] getMandatoryTables()
	{
		return new String[] { "MonSqlCapStatements" };
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		Set<String> skipErrorNumbers = StringUtil.parseCommaStrToSet( localConf.getProperty("AseErrorInfo.skip.errors", "") );
		
		String skipErrorsWhereClause = "";
		if ( ! skipErrorNumbers.isEmpty() )
		{
			skipErrorsWhereClause = " and [ErrorStatus] not in(" + StringUtil.toCommaStr(skipErrorNumbers) + ") \n";

			_messages.add("Skipping error numbers " + skipErrorNumbers);

			_logger.info("Skipping error numbers " + skipErrorNumbers + " for server '" + srvName + "'.");
		}

		int skipErrorCountAbove = localConf.getIntProperty("AseErrorInfo.skip.ErrorCountAbove", 100);
		_messages.add("Skipping SQL Errors: SQL Text, if 'ErrorCount' is above " + skipErrorCountAbove + ". This can be changed with property <code>AseErrorInfo.skip.ErrorCountAbove = ####</code>");
		
		String sql = "-- source table: MonSqlCapStatements \n"
			    + "select \n"
			    + "    [ErrorStatus]            as [ErrorStatus] \n"
			    + "   ,cast('' as varchar(255)) as [ErrorCount__chart] \n"
			    + "   ,count(*)                 as [ErrorCount] \n"
			    + "   ,count(distinct [SPID])   as [SpidCount] \n"
			    + "   ,min([StartTime])         as [FirstEntry] \n"
			    + "   ,max([StartTime])         as [LastEntry] \n"
			    + "   ,cast('' as varchar(30))  as [Duration] \n"
			    + "   ,cast('' as varchar(255)) as [ErrorMessage] \n"
			    + "from [MonSqlCapStatements] \n"
			    + "where [ErrorStatus] > 0 \n"
			    + skipErrorsWhereClause
				+ getReportPeriodSqlWhere("StartTime")
			    + "group by [ErrorStatus] \n"
			    + "order by [ErrorCount] desc \n"
			    + "";

		_shortRstm = executeQuery(conn, sql, false, "SQL With ErrorStatus");
		if (_shortRstm == null)
		{
			_shortRstm = ResultSetTableModel.createEmpty("SQL With ErrorStatus");
			return;
		}
		else
		{
			// Highlight sort column
			_shortRstm.setHighlightSortColumns("ErrorCount");

			// Describe the table
			setSectionDescription(_shortRstm);

			// set duration
			setDurationColumn(_shortRstm, "FirstEntry", "LastEntry", "Duration");
			
			// Fill in the "ErrorMessage" column from a Static Dictionary
			int pos_ErrorStatus  = _shortRstm.findColumn("ErrorStatus");
			int pos_ErrorCount   = _shortRstm.findColumn("ErrorCount");
			int pos_ErrorMessage = _shortRstm.findColumn("ErrorMessage");

			if (pos_ErrorStatus >= 0 && pos_ErrorMessage >= 0)
			{
				for (int r=0; r<_shortRstm.getRowCount(); r++)
				{
					int ErrorStatus = _shortRstm.getValueAsInteger(r, pos_ErrorStatus);

					String msgStr = AseErrorMessageDictionary.getInstance().getDescription(ErrorStatus);
					_shortRstm.setValueAtWithOverride(msgStr, r, pos_ErrorMessage);
				}
			}

//			String sqlTextTable = "MonSqlCapStatements";
//			if (DbUtils.checkIfTableExistsNoThrow(conn, null, null, "MonSqlCapSqlText"))
//				sqlTextTable = "MonSqlCapSqlText";
//
//			// Check if table "MonSqlCapStatements or MonSqlCapSqlText" has Dictionary Compressed Columns (any columns ends with "$dcc$")
//			boolean hasDictCompCols = false;
//			try {
//				hasDictCompCols = DictCompression.hasCompressedColumnNames(conn, null, sqlTextTable);
//			} catch (SQLException ex) {
//				_logger.error("Problems checking for Dictionary Compressed Columns in table '" + sqlTextTable + "'.", ex);
//			}

			// Get SQL Details for a specific error number
			if (pos_ErrorStatus >= 0 && pos_ErrorCount >= 0 && pos_ErrorMessage >= 0)
			{
				for (int r=0; r<_shortRstm.getRowCount(); r++)
				{
					int ErrorStatus = _shortRstm.getValueAsInteger(r, pos_ErrorStatus);
					int ErrorCount  = _shortRstm.getValueAsInteger(r, pos_ErrorCount);

					if (ErrorCount > skipErrorCountAbove)
						_messages.add("NOTE: For ErrorNumber " + ErrorStatus + " only the first " + skipErrorCountAbove + " SQL Text(s) will be included in the report!");

					getErrorSqlText(conn, ErrorStatus, skipErrorCountAbove);
				}
			}
			
			// Mini Chart on "Physical Reads"
			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("ErrorCount__chart")
					.setHtmlWhereKeyColumnName   ("ErrorStatus")
					.setDbmsTableName            ("MonSqlCapStatements")
					.setDbmsSampleTimeColumnName ("StartTime")
					.setDbmsDataValueColumnName  ("1").setDbmsDataValueColumnNameIsExpression(true)
					.setDbmsWhereKeyColumnName   ("ErrorStatus")
					.setSparklineTooltipPostfix  ("Number of Error Messages of #### in below period")
					.validate()));
		}
	}

	private void getErrorSqlText(DbxConnection conn, int errorNumber, int topRows)
	{
		String sql = ""
			    + "select top " + topRows + " \n"
			    + "        [ErrorStatus] \n"
			    + "       ,count(*) as [ErrorCount] \n"
			    + "       ,[ServerLogin] \n"
			    + "       ,[ProcName] \n"
			    + "       ,[LineNumber] \n"
			    + "       ,[JavaSqlHashCode] \n"
			    + "       ,min([StartTime]) as [FirstEntry] \n"
			    + "       ,max([StartTime]) as [LastEntry] \n"
//			    + "       ,datediff(second, min([StartTime]), max([StartTime])) as [Duration_sec] \n"
			    + "       ,cast('' as varchar(30)) as [Duration] \n"
			    + "       ,(select [colVal] from [MonSqlCapStatements$dcc$SQLText] i where i.[hashId] = o.[SQLText$dcc$]) as [SQLText] \n"
			    + "from [MonSqlCapStatements] o \n"
			    + "where [ErrorStatus] != 0 \n"
			    + "  and [ErrorStatus] = " + errorNumber + " \n"
			    + "group by [ErrorStatus], [ServerLogin], [ProcName], [LineNumber], [JavaSqlHashCode] \n"
			    + "order by [ErrorStatus], [ErrorCount] desc \n"
			    + "";
		
		sql = conn.quotifySqlString(sql);
		try ( Statement stmnt = conn.createStatement() )
		{
			// Unlimited execution time
			stmnt.setQueryTimeout(0);
			try ( ResultSet rs = stmnt.executeQuery(sql) )
			{
				ResultSetTableModel rstm = createResultSetTableModel(rs, "SqlErrorText", sql);
				
				if (_sqlTextRstm == null)
					_sqlTextRstm = rstm;
				else
					_sqlTextRstm.add(rstm);
			}

			// Calculate Duration
			setDurationColumn(_sqlTextRstm, "FirstEntry", "LastEntry", "Duration");
		}
		catch(SQLException ex)
		{
			setProblemException(ex);

			_logger.warn("Problems getting ErrorSqlText for ErrorStatus = "+errorNumber+": " + ex);
		} 
		catch(ModelMissmatchException ex)
		{
			setProblemException(ex);

			_logger.warn("Problems (merging into previous ResultSetTableModel) when getting ErrorSqlText for ErrorStatus = "+errorNumber+": " + ex);
		} 
	}
}
