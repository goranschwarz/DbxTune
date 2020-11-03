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
import java.io.StringWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.asetune.CounterController;
import com.asetune.cm.CountersModel;
import com.asetune.gui.ResultSetTableModel;
import com.asetune.gui.ResultSetTableModel.TableStringRenderer;
import com.asetune.pcs.DictCompression;
import com.asetune.pcs.report.DailySummaryReportAbstract;
import com.asetune.pcs.report.DailySummaryReportFactory;
import com.asetune.pcs.report.content.ReportChartTimeSeriesStackedBar.TopGroupCountReport;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;
import com.asetune.utils.DbUtils;
import com.asetune.utils.HtmlQueryString;
import com.asetune.utils.StringUtil;
import com.asetune.utils.TimeUtils;

public abstract class ReportEntryAbstract
implements IReportEntry
{
	private static Logger _logger = Logger.getLogger(ReportEntryAbstract.class);

	private   Exception _problemEx;
	private   String    _problemMsg;
	private   List<String> _warningMsgList;
	private   List<String> _infoMsgList;
	protected DailySummaryReportAbstract _reportingInstance;
	private   String _disabledReason; 

//	private Timestamp _reportBeginTime = null;
//	private Timestamp _reportEndTime   = null;
//	
//	public Timestamp getReportBeginTime() { return _reportBeginTime; }
//	public Timestamp getReportEndTime()   { return _reportEndTime; }
//
//	public void setReportBeginTime(Timestamp ts) { _reportBeginTime = ts; }
//	public void setReportEndTime  (Timestamp ts) { _reportEndTime   = ts; }

	public ReportEntryAbstract(DailySummaryReportAbstract reportingInstance)
	{
		_reportingInstance = reportingInstance;
	}

	public boolean hasReportingInstance()
	{
		return _reportingInstance != null;
	}

	public DailySummaryReportAbstract getReportingInstance()
	{
		return _reportingInstance;
	}

	@Override
	public boolean isEnabled()
	{
		String  key          = getIsEnabledConfigKeyName();
		boolean defaultValue = DailySummaryReportFactory.DEFAULT_isReportEntryEnabled;

		return Configuration.getCombinedConfiguration().getBooleanProperty(key, defaultValue);
	}
	@Override
	public String getIsEnabledConfigKeyName()
	{
		String key = DailySummaryReportFactory.PROPKEY_isReportEntryEnabled;

		key = key.replace("<ENTRY-NAME>", this.getClass().getSimpleName());
		
		return key;
	}
	@Override
	public boolean canBeDisabled()
	{
		return true;
	}
	
	@Override
	public String getDisabledReason()
	{
		return _disabledReason;
	}
	public void setDisabledReason(String reason)
	{
		_disabledReason = reason;
	}


	/**
	 * More or less same as ResultSetTableModel.toHtmlTableString(...) but a default renderer that has column names like '*Time*', '*_ms*' or '*Ms*' a tool tip with the milliseconds transformed to HH:MM:SS.ms 
	 * @param rstm   The ResultSet to create a HTML table for
	 * @return a HTML Table 
	 */
	public String toHtmlTable(ResultSetTableModel rstm)
	{
		return toHtmlTable(rstm, null);
	}
	/**
	 * More or less same as ResultSetTableModel.toHtmlTableString(...) but a default renderer that has column names like '*Time*', '*_ms*' or '*Ms*' a tool tip with the milliseconds transformed to HH:MM:SS.ms 
	 * @param rstm                 The ResultSet to create a HTML table for
	 * @param colNameValueTagMap   A Map with column names that should have special tags/values applied around the values
	 * @return a HTML Table 
	 */
	public String toHtmlTable(ResultSetTableModel rstm, Map<String, String> colNameValueTagMap)
	{
		if (rstm == null)
			return "";
		
		// Create a default renderer
		TableStringRenderer tableRender = new ResultSetTableModel.TableStringRenderer()
		{
			@Override
			public String cellToolTip(ResultSetTableModel rstm, int row, int col, String colName, Object objVal, String strVal)
			{
				if (objVal instanceof Number && objVal != null && colName != null)
				{
					if (colName.indexOf("Time") != -1 || colName.indexOf("_ms") != -1 || colName.indexOf("Ms") != -1 )
					{
//						return TimeUtils.msToTimeStrDHMSms(((Number)objVal).longValue());
						return TimeUtils.msToTimeStrDHMS(((Number)objVal).longValue());
					}
				}
				return null;
			}
		};
		
		return rstm.toHtmlTableString("sortable", true, true, colNameValueTagMap, tableRender);
	}

	public ResultSetTableModel createResultSetTableModel(ResultSet rs, String name, String sql)
	throws SQLException
	{
		return createResultSetTableModel(rs, name, sql, true);
	}
	public ResultSetTableModel createResultSetTableModel(ResultSet rs, String name, String sql, boolean doTruncate)
	throws SQLException
	{
		ResultSetTableModel rstm = new ResultSetTableModel(rs, name, sql);

		// Set toString format for Timestamp to "yyyy-MM-dd HH:mm:ss"
		rstm.setToStringTimestampFormat_YMD_HMS();

		// use localized numbers to easier see big numbers (for example: 12345 -> 12,345)
		rstm.setToStringNumberFormat(true);

		// Truncate *long* columns
		if (doTruncate)
		{
			int truncLongCellSize = Configuration.getCombinedConfiguration().getIntProperty(DailySummaryReportFactory.PROPKEY_maxTableCellSizeKb, DailySummaryReportFactory.DEFAULT_maxTableCellSizeKb);
			rstm.truncateColumnsWithSizeInKbOver(truncLongCellSize);
		}
		
		return rstm;
	}


	public ResultSetTableModel executeQuery(DbxConnection conn, String sql, boolean onErrorCreateEmptyRstm, String name)
	{
		return executeQuery(conn, sql, onErrorCreateEmptyRstm, name, true);
	}
	public ResultSetTableModel executeQuery(DbxConnection conn, String sql, boolean onErrorCreateEmptyRstm, String name, boolean doTruncate)
	{
		// transform all "[" and "]" to DBMS Vendor Quoted Identifier Chars 
		sql = conn.quotifySqlString(sql);

		try ( Statement stmnt = conn.createStatement() )
		{
			// Unlimited execution time
			stmnt.setQueryTimeout(0);
			try ( ResultSet rs = stmnt.executeQuery(sql) )
			{
				ResultSetTableModel rstm = createResultSetTableModel(rs, name, sql, doTruncate);
				
				if (_logger.isDebugEnabled())
					_logger.debug(name + "rstm.getRowCount()="+ rstm.getRowCount());
				
				return rstm;
			}
		}
		catch(SQLException ex)
		{
			setProblemException(ex);
			
			//_fullRstm = ResultSetTableModel.createEmpty(name);
			_logger.warn("Problems getting '" + name + "': " + ex);
			
			if (onErrorCreateEmptyRstm)
				return ResultSetTableModel.createEmpty(name);
			else
				return null;
		}
	}
	
	@Override
	public String getEndOfReportText()
	{
		return "\n<br>\n";
		
	}

	
	@Override
	public String getMessageText()
	{
		StringWriter writer = new StringWriter();
		
		// StringWriter do not throw exception, but the interface does, do not care about the Exception 
		try { writeMessageText(writer); }
		catch (IOException ignore) {}

		return writer.toString();
	}

//	@Override
//	public void writeMessageText(Writer sb)
//	throws IOException
//	{
//	}

//	@Override
//	public String getMessageText()
//	{
//		StringBuilder sb = new StringBuilder();
//
//		if (hasWarningMsg())
//		{
//			sb.append(getWarningMsg());
//		}
//
//		if (hasInfogMsg())
//		{
//			sb.append(getInfoMsg());
//		}
//
//		if (hasResultSetTables())
//		{
//			for (ResultSetTableModel rstm : getResultSetTables())
//			{
//				if (rstm.getRowCount() == 0)
//				{
//					sb.append("Row Count: ").append(rstm.getRowCount()).append("<br>\n");
//				}
//				else
//				{
//					// Get a description of this section, and column names
//					sb.append(getSectionDescriptionHtml(rstm, true));
//
//					sb.append("Row Count: ").append(rstm.getRowCount()).append("<br>\n");
//					sb.append(toHtmlTable(rstm));
//				}
//			}
//		}
//
//		return sb.toString();
//	}
//
//	//-------------------------------------------------------------------------
//	// Result Set Tables
//	//-------------------------------------------------------------------------
//	private List<ResultSetTableModel> _resulSetTableList;
//
//	public void addResultSetTable(ResultSetTableModel rstm)
//	{
//		if (_resulSetTableList == null)
//			_resulSetTableList = new ArrayList<>();
//
//		_resulSetTableList.add(rstm);
//	}
//	
//	public List<ResultSetTableModel> getResultSetTables()
//	{
//		if (_resulSetTableList == null)
//			_resulSetTableList = new ArrayList<>();
//
//		return _resulSetTableList;
//	}
//
//	public boolean hasResultSetTables()
//	{
//		if (_resulSetTableList == null)
//			return false;
//
//		return !_resulSetTableList.isEmpty();
//	}
	
	
	//-------------------------------------------------------------------------
	// PROBLEM (error) messages
	//-------------------------------------------------------------------------
	@Override
	public String getProblemText()
	{
		if ( ! hasProblem() )
			return "";

		StringBuilder sb = new StringBuilder();
		
		if (getProblemException() != null)
		{
    		Exception ex = getProblemException();
    		sb.append("<pre>").append(ex).append("</pre> \n");
    		
    		if (isPrintStacktraceEnabled())
    		{
    			sb.append("<b>Stacktrace:</b><br> \n");
    			sb.append("<pre>").append( StringUtil.exceptionToString(ex)).append("</pre> \n");
    		}
		}

		if (getProblemMsg() != null)
		{
    		sb.append(getProblemMsg());
		}
		
		return sb.toString();
	}
	
	public boolean isPrintStacktraceEnabled()
	{
		return true;
	}

	/**
	 * Checks if there are any problems... indicated by: setProblemException() or setProblemStr()
	 *  
	 * @return
	 */
	@Override
	public boolean hasProblem()
	{
		return getProblemException() != null || getProblemMsg() != null;
	}


	public void setProblemException(Exception ex)
	{
		_problemEx = ex;
	}
	public Exception getProblemException()
	{
		return _problemEx;
	}


	public void addProblemMessage(String text)
	{
		if (_problemMsg == null)
			_problemMsg = "";

		_problemMsg += text;
	}
	public void setProblemMsg(String text)
	{
		_problemMsg = text;
	}
	public String getProblemMsg()
	{
		return _problemMsg;
	}


	//-------------------------------------------------------------------------
	// WARNING messages
	//-------------------------------------------------------------------------
	public void addWarningMessage(String text)
	{
		if (_warningMsgList == null)
			_warningMsgList = new ArrayList<>();

		_warningMsgList.add(text);
	}
	@Override
	public String getWarningMsg()
	{
		if (_warningMsgList == null)
			return "";
		if (_warningMsgList.isEmpty())
			return "";

		StringBuilder sb = new StringBuilder();
		
		sb.append("<div class='dsr-warning-list'> \n");
		sb.append("<font color='#ff9900'> \n");

		sb.append("<h4>Warning Messages</h4> \n");
		sb.append("<ul> \n");
		for (String msg : _warningMsgList)
		{
			sb.append("<li>").append(msg).append("</li> \n");
		}
		sb.append("</ul> \n");
		sb.append("</font> \n");
		sb.append("</div> \n");
		
		return sb.toString();
	}
	@Override
	public boolean hasWarningMsg()
	{
		if (_warningMsgList == null)
			return false;

		return !_warningMsgList.isEmpty();
	}


	//-------------------------------------------------------------------------
	// INFO messages
	//-------------------------------------------------------------------------
	public void addInfoMessage(String text)
	{
		if (_infoMsgList == null)
			_infoMsgList = new ArrayList<>();

		_infoMsgList.add(text);
	}
	@Override
	public String getInfoMsg()
	{
		if (_infoMsgList == null)
			return "";
		if (_infoMsgList.isEmpty())
			return "";

		StringBuilder sb = new StringBuilder();
		
		sb.append("<div class='dsr-info-list'> \n");

		sb.append("<h4>Messages</h4> \n");
		sb.append("<ul> \n");
		for (String msg : _infoMsgList)
		{
			sb.append("<li>").append(msg).append("</li> \n");
		}
		sb.append("</ul> \n");
		sb.append("</div> \n");
		
		return sb.toString();
	}
	@Override
	public boolean hasInfogMsg()
	{
		if (_infoMsgList == null)
			return false;

		return !_infoMsgList.isEmpty();
	}

	
	/**
	 * Is called before method create() <br>
	 * In here we can check for various stuff like if all tables exists etc
	 * <p>
	 * To indicate a problem; call setProblem()
	 * 
	 * @param conn
	 * @return
	 */
	@Override
	public void checkForIssuesBeforeCreate(DbxConnection conn)
	{
		String[] mandatoryTables = getMandatoryTables();
		if (mandatoryTables != null)
		{
			for (int t=0; t<mandatoryTables.length; t++)
			{
				String tabEntry = mandatoryTables[t];
				String[] sa = tabEntry.split("\\."); // escape the '.' since it's a regex for "any" char
				
				String catName = null;
				String schName = null;
				String tabName = null;
				
				if (sa.length == 1)
				{
					tabName = sa[0];
				}
				if (sa.length >= 2)
				{
					schName = sa[0];
					tabName = sa[1];
				}
				if (sa.length >= 3)
				{
					catName = sa[0];
					schName = sa[1];
					tabName = sa[2];
				}

				// This checks table if the table name in following order: MixedCase, then UPPER or lower (depending on the metadata)
				if ( ! DbUtils.checkIfTableExistsNoThrow(conn, catName, schName, tabName) )
				{
					catName = catName == null ? "" : catName + ".";
					schName = schName == null ? "" : schName + ".";
					String fullTabName = catName + schName + tabName;
					
					addProblemMessage("<br><font color='red'>ERROR: Creating Report Entry '" + this.getClass().getSimpleName() + "', the underlying table '" + fullTabName + "' did not exist, skipping this Report Entry.</font><br>\n");

					_logger.info("In the Report Entry '" + this.getClass().getSimpleName() + "', the table '" + fullTabName + "' did not exist.");
				}
			}
		} // end: checkTables != null
	}

	/**
	 * Get tables that we depends on in this Report<br>
	 * If we can't find them all, then this Report Entry wont be created.<br>
	 * Instead a error message will be displayed that we are missing that table.
	 * 
	 * @return a Array of tables (null = no mandatory tables). One entry in the array should look like [SchemaName.]TableName
	 */
	@Override
	public String[] getMandatoryTables()
	{
		return null;
	}

	public boolean doTableExist(DbxConnection conn, String schName, String tabName)
	{
		// Check 
		boolean tabExists = DbUtils.checkIfTableExistsNoThrow(conn, null, schName, tabName);
		if ( ! tabExists )
		{
			return false;
		}
		return true;
	}

	/**
	 * Create a description of this report section<br>
	 * Column names should be described in a list.
	 * @return
	 */
	public String getSectionDescriptionHtml(ResultSetTableModel rstm, boolean addSqlText)
	{
		if (rstm == null)
			return "";
		
		StringBuilder sb = new StringBuilder();
		
		sb.append("<p>\n");

		String desc = rstm.getDescription();
		if (desc != null)
		{
			// Strip off <html> start/end
			if (desc.substring(0, 10).toLowerCase().startsWith("<html>"))
			{
				desc = desc.substring("<html>".length());
				desc = StringUtil.removeLastStr(desc, "</html>");
			}
			sb.append(desc);
		}
		
		if (rstm.hasColumnDescriptions() && rstm.getColumnNames() != null)
		{
			String  divId       = "colDesc_" + StringUtil.stripAllNonAlphaNum(rstm.getName());
			boolean showAtStart = false;

			// as: UL - Unordered List
//			String htmlContent = "<ul> \n";
//			for (String colName : rstm.getColumnNames())
//				htmlContent += "  <li><b>" + colName + "</b> - " + rstm.getColumnDescription(colName) + "</li> \n";
//			htmlContent += "</ul> \n";

			// as: HTML Table
			String htmlContent = "<table> \n";
			htmlContent += "<thead> \n";
			htmlContent += "  <tr> <th nowrap>Column Name</th> <th nowrap>Description</th> </tr> \n";
			htmlContent += "</thead> \n";
			htmlContent += "<tbody> \n";
			for (String colName : rstm.getColumnNames())
				htmlContent += "  <tr> <td nowrap><b>" + colName + "</b></td>  <td nowrap>" + rstm.getColumnDescription(colName) + "</td> </tr> \n";
			htmlContent += "</tbody> \n";
			htmlContent += "</table> \n";

			String showHideDiv = createShowHideDiv(divId, showAtStart, "Show/Hide Column(s) description...", htmlContent);
			
			sb.append( msOutlookAlternateText(showHideDiv, "Column Desciption", "") );
		}

		if (addSqlText)
		{
			String sqlText = rstm.getSqlText();
			if (StringUtil.hasValue(sqlText))
			{
				String  divId       = "sqlText_" + sqlText.hashCode();
				boolean showAtStart = false;

				String htmlContent = ""
						+ "<hr> \n"
						+ "<xmp> \n"
						+ sqlText
						+ "</xmp> \n"
						+ "<hr> \n"
						+ "";
				
				String showHideDiv = createShowHideDiv(divId, showAtStart, "Show/Hide SQL Text that produced this report section...", htmlContent);
				
				sb.append( msOutlookAlternateText(showHideDiv, "Show/Hide SQL Text", "") );
			}
		}

		sb.append("</p>\n");
		sb.append("\n");
		return sb.toString();
	}

	
	/**
	 * Get the CounterModel's DIFF Counters as a SQL Comment, which can be appended to the SQL Used to create report content
	 * 
	 * @param cmName   short cm name
	 * @return a string in the form: <code>-- cm='cmName', possibleDiffColumns='c1,c2,c3' \n</code>
	 */
	public String getCmDiffColumnsAsSqlComment(String cmName)
	{
		if (cmName == null)
			return "";

		String sqlComment = "";
		if (CounterController.hasInstance())
		{
			CountersModel cm = CounterController.getInstance().getCmByName(cmName);
			if (cm != null)
			{
				String[] sa = cm.getDiffColumns();
				if (sa != null)
				{
					sqlComment = "-- cm='" + cmName + "', possibleDiffColumns='" + StringUtil.toCommaStr(sa) + "' \n";
				}
			}
		}
		return sqlComment;
	}
	

//	public String createShowHideDiv(String divId, boolean visibleAtStart, String label, String content)
//	{
//		StringBuilder sb = new StringBuilder();
//
//		String visibilityCss = visibleAtStart ? "block" : "none";
//		
//		sb.append("<a href='javascript:;' onclick=\"toggle_visibility('").append(divId).append("');\">").append(label).append("</a> <br>\n");
//		sb.append("<div id='").append(divId).append("' style='display: ").append(visibilityCss).append(";'>\n");
//		sb.append(content);
//		sb.append("</div>\n");
//		sb.append("\n");
//
//		return sb.toString();
//	}

	/**
	 * Create a html DIV with
	 * <ul>
	 *     <li>A CheckBox, with a ID, that is provided in the method call</li>
	 *     <li>And a CheckBox label, also a parameter</li>
	 *     <li>and a embedded DIV with the content parameter</li>
	 * </ul>
	 * @param divId
	 * @param visibleAtStart
	 * @param label
	 * @param content
	 * @return
	 */
	public String createShowHideDiv(String divId, boolean visibleAtStart, String label, String content)
	{
		StringBuilder sb = new StringBuilder();

		String visibility = visibleAtStart ? "checked" : "unchecked";
		
		// Build the below:
		// <div>
		//     <input type='checkbox' id='DIVID' checked/>
		//     <label for='DIVID'>LABEL GOES HERE</label>
		//     <div class='hide-show'>CONTENT GOES HERE</div>
		// </div>

		sb.append("\n");
		sb.append("<div>\n");
		sb.append("    <input type='checkbox' id='").append(divId).append("' ").append(visibility).append("/> \n");
		sb.append("    <label for='").append(divId).append("'>").append(label).append("</label> \n");
		sb.append("    <div class='hide-show'>\n").append(content).append("\n    </div> \n");
		sb.append("</div>\n");
		sb.append("\n");

		return sb.toString();
	}

	/**
	 * This adds conditional text NOT available if you are using Microsoft Outlook Mail client as a reader.
	 * 
	 * @param contentForOtherReaders   Content for other readers than Microsoft Outlook
	 * @param name                     Typical "name" of the section which will be suppressed for MS Outlook
	 * @param alternateMsoText         Alternate text for Microsoft Outlook... if NULL; A default text will be used
	 * @return
	 */
	public String msOutlookAlternateText(String contentForOtherReaders, String name, String alternateMsoText)
	{
		StringBuilder sb = new StringBuilder();
		
		if (alternateMsoText == null)
		{
			sb.append("\n");
			sb.append("<!--[if mso]>\n");
			sb.append("You are using Microsoft Outlook to read this...<br>\n");
			sb.append("'<i>").append(name).append("</i>' is NOT presented for Microsoft Outlook, <b>open the link at the top to see the '<i>").append(name).append("</i>'...</b><br>\n");
			sb.append("<![endif]-->\n");
		}
		else
		{
			sb.append("\n");
			sb.append("<!--[if mso]>\n");
			sb.append(alternateMsoText).append("<br>\n");
			sb.append("<![endif]-->\n");
		}
		
		sb.append("<!--[if !mso]><!-- -->\n");
		if (contentForOtherReaders != null)
		{
			sb.append(contentForOtherReaders);

			if ( ! contentForOtherReaders.endsWith("\n") )
				sb.append("\n");
		}
		sb.append("<!--<![endif]-->\n");
		
		return sb.toString();
	}

	/**
	 * Calculate "Duration", and set that to a String column in the form "%HH:%MM:%SS"
	 * @param rstm
	 * @param startColName      Start col
	 * @param stopColName       End col
	 * @param durationColName   Name of the column we want to set
	 */
	public void setDurationColumn(ResultSetTableModel rstm, String startColName, String stopColName, String durationColName)
	{
		int pos_StartEntry = rstm.findColumn(startColName);
		int pos_StopEntry  = rstm.findColumn(stopColName);
		int pos_Duration   = rstm.findColumn(durationColName);

		if (pos_StartEntry >= 0 && pos_StopEntry >= 0 && pos_Duration >= 0)
		{
			for (int r=0; r<rstm.getRowCount(); r++)
			{
				Timestamp FirstEntry = rstm.getValueAsTimestamp(r, pos_StartEntry);
				Timestamp LastEntry  = rstm.getValueAsTimestamp(r, pos_StopEntry);

				if (FirstEntry != null && LastEntry != null)
				{
					long durationInMs = LastEntry.getTime() - FirstEntry.getTime();
					String durationStr = TimeUtils.msToTimeStr("%HH:%MM:%SS", durationInMs);
					rstm.setValueAtWithOverride(durationStr, r, pos_Duration);
				}
			}
		}
	}


	/**
	 * Create a link to DbxCentral where you can add any Skip entries
	 * 
	 * @param rstm
	 * @param linkColumnName    The Column Name where we should insert the link
	 * @param skipColumnName    Column Name we should get valueString to skip
	 * @param sqlTextColumnName Column Name that contains SQL Text we want to skip (this can be null)
	 */
	public void setSkipEntriesUrl(ResultSetTableModel rstm, String linkColumnName, String skipColumnName, String sqlTextColumnName)
	{
		int pos_linkColumnName    = rstm.findColumn(linkColumnName);
		int pos_skipColumnName    = rstm.findColumn(skipColumnName);
		int pos_sqlTextColumnName = sqlTextColumnName == null ? -1 : rstm.findColumn(sqlTextColumnName);

		if (pos_linkColumnName >= 0 && pos_skipColumnName >= 0)
		{
			String dbxCentral     = getReportingInstance().getDbxCentralBaseUrl();
			String srvName        = getReportingInstance().getDbmsServerName();
			String className      = this.getClass().getSimpleName();
			String entryType      = skipColumnName;

			for (int r=0; r<rstm.getRowCount(); r++)
			{
				String skipColmnValue = rstm.getValueAsString(r, pos_skipColumnName);
				String description    = "";
				String sqlTextExample = pos_sqlTextColumnName == -1 ? "" : rstm.getValueAsString(r, pos_sqlTextColumnName);
				
				if (skipColmnValue != null)
				{
					HtmlQueryString qs = new HtmlQueryString( dbxCentral + "/admin/admin.html" );
					qs.add("op",             "openDsrSkipDialog");
					qs.add("srvName",        srvName);
					qs.add("className",      className);
					qs.add("entryType",      entryType);
					qs.add("stringVal",      skipColmnValue);
					qs.add("description",    description);
					qs.add("sqlTextExample", sqlTextExample);

					String skipUrlStr = "<a href='" + qs + "' target='_blank'>Skip This</a>";

					rstm.setValueAtWithOverride(skipUrlStr, r, pos_linkColumnName);
				}
			}
		}
	}
	

	/**
	 * Get values from PCS table <code>MonSessionParams</code> where <code>ParamName</code> is equal to 'offline.sampleTime'
	 * 
	 * @param conn
	 * @return
	 */
	public static int getRecordingSampleTime(DbxConnection conn)
	{
		String val = getRecordingSessionParameter(conn, null, "offline.sampleTime");
		
		return StringUtil.parseInt(val, -1);
	}
	
	/**
	 * Get values from PCS table <code>MonSessionParams</code>
	 * 
	 * @param conn
	 * @param type           can be null, then all "types" will be searched, but lat one found will be returned.
	 * @param paramName      Name of the parameter (or content of column <code>ParamName</code>)
	 * 
	 * @return The value as a String, NULL if not found.
	 */
	public static String getRecordingSessionParameter(DbxConnection conn, String type, String paramName)
	{
		String whereType = "";
		if (StringUtil.hasValue(type))
			whereType = "  and [Type] = '" + type + "' \n";
		
		String tabName = "MonSessionParams";
		String sql = ""
			    + "select [Type], [ParamName], [ParamValue] \n"
			    + "from ["+tabName+"] \n"
			    + "where [ParamName] = '" + paramName + "' \n"
			    + whereType
			    + "";

		String paramValue = null;
		
		sql = conn.quotifySqlString(sql);
		try ( Statement stmnt = conn.createStatement() )
		{
			// Unlimited execution time
			stmnt.setQueryTimeout(0);
			try ( ResultSet rs = stmnt.executeQuery(sql) )
			{
				while(rs.next())
				{
//					String type = rs.getString(1);
//					String name = rs.getString(2);
					String val  = rs.getString(3);

					paramValue = val;
				}
			}
		}
		catch(SQLException ex)
		{
			_logger.warn("Problems getting values from '"+tabName+"': " + ex);
		}

		return paramValue;
	}
	

	public String getDbxCentralLinkWithDescForGraphs(boolean addSpaceBefore, String description, String... graphList)
	{
		StringBuilder sb = new StringBuilder();

		if (addSpaceBefore)
		{
			sb.append("<br><br><br>\n");
			sb.append("<hr>\n");
		}
		sb.append("<p>\n");
		sb.append(description).append(" <br>\n");
		sb.append("<a href='").append(getDbxCentralLinkForGraphs(graphList)).append("'>Link to DbxCentral Graphs</a> which is a bit more dynamic than static images.\n");
		sb.append("</p>\n");
		
		return sb.toString();
	}
	public String getDbxCentralLinkForGraphs(String... graphList)
	{
		String dbxcLink = getReportingInstance().getDbxCentralBaseUrl();
		RecordingInfo recordingInfo = getReportingInstance().getInstanceRecordingInfo();

		String startTime = recordingInfo.getStartTime();
		String endTime   = recordingInfo.getEndTime();
		String srvName   = getReportingInstance().getServerName();
//		String graphList = "CmSummary_aaCpuGraph,CmEngines_cpuSum,CmEngines_cpuEng,CmSysLoad_EngineRunQLengthGraph";

		if (StringUtil.hasValue(startTime)) startTime = startTime.substring(0, "YYYY-MM-DD".length()) + " 00:00";
		if (StringUtil.hasValue(endTime))   endTime   = endTime  .substring(0, "YYYY-MM-DD".length()) + " 23:59";
		
		String dbxCentralLink = dbxcLink + "/graph.html"
				+ "?subscribe=false"
				+ "&cs=dark"
				+ "&gcols=1"
				+ "&sessionName=" + srvName 
				+ "&startTime="   + startTime 
				+ "&endTime="     + endTime 
				+ "&graphList="   + StringUtil.toCommaStr(graphList, ",") // note: normal StringUtil.toCommaStr(graphList)... makes separator as ", " (with a space after the comma)
				+ "";
		
		return dbxCentralLink;
	}
	
	/**
	 * Simple wrapper method to create a ReportChartObject
	 * 
	 * @param conn
	 * @param cmName
	 * @param graphName
	 * @param maxValue
	 * @param skipNames
	 * @param graphTitle
	 * 
	 * @return This will always returns a ReportChartObject object
	 */
	public IReportChart createTsLineChart(DbxConnection conn, String cmName, String graphName, int maxValue, String skipNames, String graphTitle)
	{
		return new ReportChartTimeSeriesLine(this, conn, cmName, graphName, maxValue, skipNames, graphTitle);
	}
	
	/**
	 * Simple wrapper method to create a ReportChartObject
	 * 
	 * @param conn
	 * @param cmName
	 * @param graphName
	 * @param maxValue
	 * @param skipNames
	 * @param graphTitle
	 * 
	 * @return This will always returns a ReportChartObject object
	 */
	public IReportChart createTsStackedBarChart(DbxConnection conn, String cmName, String dataGroupColumn, int dataGroupMinutes, TopGroupCountReport topGroupCountReport, String dataValueColumn, Double dataDivideByValue, String keepGroups, String skipGroups, String graphTitle)
	{
		return new ReportChartTimeSeriesStackedBar(this, conn, cmName, dataGroupColumn, dataGroupMinutes, topGroupCountReport, dataValueColumn, dataDivideByValue, keepGroups, skipGroups, graphTitle);
	}
	
	
	/**
	 * FIXME
	 * 
	 * @param rstm             The ResultSetTableModel that we want to fix
	 * @param conn             Connection to the Storage
	 * @param schemaName       Schema name where the Dictionary Compression tables are located (can be null)
	 * @param tabName          Base Name of the table (which in most cases are 'CmName')
	 * @param colName          Name of the column, that we want to change from 'hashId' to 'Dictionary-Compressed-Text'
	 * @return number of records that was updated...
	 */
	public int updateDictionaryCompressedColumn(ResultSetTableModel rstm, DbxConnection conn, String schemaName, String tabName, String colName, String rstmColName)
	{
		if (rstm == null)
			return 0;

		if (StringUtil.isNullOrBlank(rstmColName))
			rstmColName = colName;
		
		int rowsChanged = 0;
		// get SqlText
		int pos_colName = rstm.findColumn(rstmColName);
		if (pos_colName >= 0)
		{
			for (int r=0; r<rstm.getRowCount(); r++)
			{
				String  hashId   = rstm.getValueAsString (r, pos_colName);
				
				String query = "";
				try 
				{
					query = DictCompression.getValueForHashId(conn, schemaName, tabName, colName, hashId);
					rowsChanged++;
				} 
				catch (SQLException ex) 
				{
					query = "Problems getting Dictionary Compressed column for tabName='" +tabName + "', colName='" + colName + "', hashId='" + hashId + "'.";
				}

				// set QUERY text in the original ResultSet
				rstm.setValueAtWithOverride(query, r, pos_colName);
			}
		}
		
		return rowsChanged;
	}

	/**
	 * If we have a begin/end period for this report, you want to add an extra criteria for the SQL Statement 
	 * @return
	 */
	public String getReportPeriodSqlWhere()
	{
		return getReportPeriodSqlWhere("SessionSampleTime");
	}

//	/**
//	 * If we have a begin/end period for this report, you want to add an extra criteria for the SQL Statement 
//	 * @return
//	 */
//	public String getReportPeriodSqlWhere(String colname)
//	{
//		// Most common: exit early
//		if (getReportBeginTime() == null && getReportEndTime() == null)
//			return "";
//
//		// we have both BEGIN and END time
//		if (getReportBeginTime() != null && getReportEndTime() != null)
//		{
//			return " and [" + colname + "] between '" + getReportBeginTime() + "' and '" + getReportEndTime() + "' \n";
//		}
//		// we only have BEGIN time
//		else if (getReportBeginTime() != null)
//		{
//			return " and [" + colname + "] >= '" + getReportBeginTime() + "' \n";
//		}
//		// we only have END time
//		else if (getReportEndTime() != null)
//		{
//			return " and [" + colname + "] <= '" + getReportEndTime() + "' \n";
//		}
//		else
//		{
//			return "";
//		}
//	}
//	public boolean hasReportPeriod()
//	{
//		return (getReportBeginTime() != null || getReportEndTime() != null);
//	}

	/**
	 * If we have a begin/end period for this report, you want to add an extra criteria for the SQL Statement 
	 * @return
	 */
	public String getReportPeriodSqlWhere(String colname)
	{
		DailySummaryReportAbstract inst = getReportingInstance();
		
		// Most common: exit early
		if (inst.getReportPeriodBeginTime() == null && inst.getReportPeriodEndTime() == null)
			return "";

		// we have both BEGIN and END time
		if (inst.getReportPeriodBeginTime() != null && inst.getReportPeriodEndTime() != null)
		{
			return "  and [" + colname + "] between '" + inst.getReportPeriodBeginTime() + "' and '" + inst.getReportPeriodEndTime() + "' \n";
		}
		// we only have BEGIN time
		else if (inst.getReportPeriodBeginTime() != null)
		{
			return "  and [" + colname + "] >= '" + inst.getReportPeriodBeginTime() + "' \n";
		}
		// we only have END time
		else if (inst.getReportPeriodEndTime() != null)
		{
			return "  and [" + colname + "] <= '" + inst.getReportPeriodEndTime() + "' \n";
		}
		else
		{
			return "";
		}
	}
	public boolean hasReportPeriod()
	{
		return (getReportingInstance().getReportPeriodBeginTime() != null || getReportingInstance().getReportPeriodEndTime() != null);
	}
	
}
