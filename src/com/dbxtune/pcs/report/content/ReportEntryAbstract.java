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
import java.io.StringWriter;
import java.io.Writer;
import java.lang.invoke.MethodHandles;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.CounterController;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.DictCompression;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.pcs.report.DailySummaryReportDefault;
import com.dbxtune.pcs.report.DailySummaryReportFactory;
import com.dbxtune.pcs.report.content.ReportChartTimeSeriesStackedBar.TopGroupCountReport;
import com.dbxtune.sql.SqlParserUtils;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.ddl.model.Index;
import com.dbxtune.sql.ddl.model.Table;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.CountingWriter;
import com.dbxtune.utils.DbUtils;
import com.dbxtune.utils.HtmlQueryString;
import com.dbxtune.utils.SqlUtils;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.TimeUtils;

public abstract class ReportEntryAbstract
implements IReportEntry
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public static final String  PROPKEY_TOP_STATEMENTS_ONLY_LIST_OBJECTS_IN_CURRENT_DATABASE = "DailySummaryReport.top.statements.onlyListObjectsInCurrentDatabase";
	public static final boolean DEFAULT_TOP_STATEMENTS_ONLY_LIST_OBJECTS_IN_CURRENT_DATABASE = true;

	private   Exception _problemEx;
	private   String    _problemMsg;
	private   List<String> _warningMsgList;
	private   List<String> _infoMsgList;
	protected DailySummaryReportAbstract _reportingInstance;
	private   String _disabledReason; 
	
	private   MessageType _currentMessageType;

	private   long _execTime;
	private   long _execStartTime;
	private   long _execEndTime;
	
	private boolean _collapsedHeader = false;
	
	@Override public void setExecTime(long ms) { _execTime = ms;}
	@Override public void setExecStartTime()   { _execStartTime = System.currentTimeMillis(); }
	@Override public void setExecEndTime()     { _execEndTime   = System.currentTimeMillis(); setExecTime(_execEndTime - _execStartTime); }

	@Override public long getExecTime()        { return _execTime; }
	@Override public long getExecStartTime()   { return _execStartTime; }
	@Override public long getExecEndTime()     { return _execEndTime; }

	@Override public boolean isMinimalMessageType() { return MessageType.MINIMAL_MESSAGE.equals(getCurrentMessageType()); }
	@Override public boolean isShortMessageType()   { return MessageType.SHORT_MESSAGE  .equals(getCurrentMessageType()); }
	@Override public boolean isFullMessageType()    { return MessageType.FULL_MESSAGE   .equals(getCurrentMessageType()); }
	@Override public void setCurrentMessageType(MessageType messageType) { _currentMessageType = messageType; }
	@Override public MessageType getCurrentMessageType() { return _currentMessageType; }

	@Override public boolean isCollapsedHeader() { return _collapsedHeader; }
	@Override public IReportEntry withCollapsedHeader(boolean collapsedHeader) { _collapsedHeader = collapsedHeader; return this;}
	
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

	public boolean isWindows()
	{
		return getReportingInstance().isWindows();
//		String dbmsVerStr = getReportingInstance().getDbmsVersionStr();
//		if (StringUtil.hasValue(dbmsVerStr))
//		{
//			if (dbmsVerStr.contains("Windows"))
//			{
//				return true;
//			}
//		}
//		return false;
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

//	@Override
//	public boolean hasShortMessageText()
//	{
//		return false;
//	}
//
//	@Override
//	public void writeShortMessageText(Writer w)
//	{
//	}

	public static class ReportEntryTableStringRenderer 
	implements ResultSetTableModel.TableStringRenderer
	{
		@Override
		public String tagThAttr(ResultSetTableModel rstm, int col, String colName, boolean nowrapPreferred)
		{
			String attr = ResultSetTableModel.TableStringRenderer.super.tagThAttr(rstm, col, colName, nowrapPreferred);

			if ("Sparklines".equalsIgnoreCase(colName))
			{
				// This is to make the "Sparklines" columns not to *shrink* in Outlook Mail (since it uses "Word" to render HTML)
//				attr = "class='sparkline-td'";                  // did NOT work; This class has "width: 1100;" but only for outlook "<!--[if mso]>...<![endif]-->"
//				attr = "width='1100'";                          // This is the best so far but it breaks IOS a bit, Outloook=OK, Chrome=OK, iPhone=Leaves a gap (to big for being 100% OK)
//				attr = "mso-width='1100'";                      // did NOT work at all
				attr = "style='width:1100;'";                   // BEST <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
//				attr = "mso-width-alt='1100'";                  // 
//				attr = "style='mso-width-alt:1100;'";           // did NOT work at all
//				attr = "<!--[if mso]>width='1100'<![endif]-->"; // This did NOT work... possibly I can try: "\n<!--[if mso]>\nwidth='1100'\n<![endif]-->\n" ... but does ALL the HTML Browsers accept that ???
//				attr = "\n"                                     // Width... but only for MS Outlook (this left some strange chars in Outlook)
//						+ "<!--[if mso]>\n"
//						+ "width='1100'\n"
//						+ "<![endif]-->\n";
			}

			return attr;
		}

		@Override
		public String tagTdAttr(ResultSetTableModel rstm, int row, int col, String colName, Object objVal, String strVal, boolean nowrapPreferred)
		{
			String attr = ResultSetTableModel.TableStringRenderer.super.tagTdAttr(rstm, row, col, colName, objVal, strVal, nowrapPreferred);

			if ("Sparklines".equalsIgnoreCase(colName))
			{
				// This is to make the "Sparklines" columns not to *shrink* in Outlook Mail (since it uses "Word" to render HTML)
//				attr = "class='sparkline-td'";                  // did NOT work; This class has "width: 1100;" but only for outlook "<!--[if mso]>...<![endif]-->"
//				attr = "width='1100'";                          // This is the best so far but it breaks IOS a bit, Outloook=OK, Chrome=OK, iPhone=Leaves a gap (to big for being 100% OK)
//				attr = "mso-width='1100'";                      // did NOT work at all
				attr = "style='width:1100;'";                   // BEST <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
//				attr = "mso-width-alt='1100'";                  // 
//				attr = "style='mso-width-alt:1100;'";           // did NOT work at all
//				attr = "<!--[if mso]>width='1100'<![endif]-->"; // This did NOT work... possibly I can try: "\n<!--[if mso]>\nwidth='1100'\n<![endif]-->\n" ... but does ALL the HTML Browsers accept that ???
//				attr = "\n"                                     // Width... but only for MS Outlook (this left some strange chars in Outlook)
//						+ "<!--[if mso]>\n"
//						+ "width='1100'\n"
//						+ "<![endif]-->\n";
			}

			return attr;
		}
		
		@Override
		public String cellValue(ResultSetTableModel rstm, int row, int col, String colName, Object objVal, String strVal)
		{
			return ResultSetTableModel.TableStringRenderer.super.cellValue(rstm, row, col, colName, objVal, strVal);
		}

		@Override
		public String cellToolTip(ResultSetTableModel rstm, int row, int col, String colName, Object objVal, String strVal)
		{
			if (objVal instanceof Number && objVal != null && colName != null)
			{
				if (colName.indexOf("Time") != -1 || colName.indexOf("_ms") != -1 || colName.indexOf("Ms") != -1 )
				{
					return TimeUtils.msToTimeStrDHMS(((Number)objVal).longValue());
				}
			}
			return null;
		}
	}
	
	/**
	 * Produce a String looking like '&lt;code>tab1&gt;/code&gt;, &lt;code>tab2&gt;/code&gt;, &lt;code>tab3&gt;/code&gt;'
	 * @param tableList A list of (table) names
	 * @return
	 */
	public String listToHtmlCode(Collection<String> tableList)
	{
		String out = "";
		String comma = "";
		for (String table : tableList)
		{
			out += comma + "<code>" + table + "</code>";
			comma = ", ";
		}
		return out;
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
		
		// use a renderer ReportEntryTableStringRenderer
		return rstm.toHtmlTableString("sortable", true, true, colNameValueTagMap, new ReportEntryTableStringRenderer());
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


	public ResultSetTableModel executeQuery(DbxConnection conn, String sql, String name)
	throws SQLException
	{
		// transform all "[" and "]" to DBMS Vendor Quoted Identifier Chars 
		sql = conn.quotifySqlString(sql);

		try ( Statement stmnt = conn.createStatement() )
		{
			// Unlimited execution time
			stmnt.setQueryTimeout(0);
			try ( ResultSet rs = stmnt.executeQuery(sql) )
			{
				ResultSetTableModel rstm = createResultSetTableModel(rs, name, sql, false);
				
				if (_logger.isDebugEnabled())
					_logger.debug(name + "rstm.getRowCount()="+ rstm.getRowCount());
				
				return rstm;
			}
		}
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
		try { writeMessageText(writer, getCurrentMessageType()); }
		catch (IOException ignore) {}

		return writer.toString();
	}

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
				
				if (schName == null)
					schName = getReportingInstance().getDbmsSchemaName();

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
			
			sb.append( msOutlookAlternateText(showHideDiv, "Column Description", "") );
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
	

//	/**
//	 * Create a html DIV with
//	 * <ul>
//	 *     <li>A CheckBox, with a ID, that is provided in the method call</li>
//	 *     <li>And a CheckBox label, also a parameter</li>
//	 *     <li>and a embedded DIV with the content parameter</li>
//	 * </ul>
//	 * @param divId
//	 * @param visibleAtStart
//	 * @param label
//	 * @param content
//	 * @return
//	 */
//	public String createShowHideDiv(String divId, boolean visibleAtStart, String label, String content)
//	{
//		StringBuilder sb = new StringBuilder();
//
//		String visibility = visibleAtStart ? "checked" : "unchecked";
//		
//		// Build the below:
//		// <div>
//		//     <input type='checkbox' id='DIVID' checked/>
//		//     <label for='DIVID'>LABEL GOES HERE</label>
//		//     <div class='hide-show'>CONTENT GOES HERE</div>
//		// </div>
//
//		sb.append("\n");
//		sb.append("<div>\n");
//		sb.append("    <input type='checkbox' id='").append(divId).append("' ").append(visibility).append("/> \n");
//		sb.append("    <label for='").append(divId).append("'>").append(label).append("</label> \n");
//		sb.append("    <div class='hide-show'>\n").append(content).append("\n    </div> \n");
//		sb.append("</div>\n");
//		sb.append("\n");
//
//		return sb.toString();
//	}
	/**
	 * Create a html "details" tag with
	 * 
	 * @param divId
	 * @param visibleAtStart
	 * @param label
	 * @param content
	 * @return
	 */
	public String createShowHideDiv(String divId, boolean visibleAtStart, String label, String content)
	{
		StringBuilder sb = new StringBuilder();

		String visibility = visibleAtStart ? "open" : "";
		
		// Build the below:
		// <details id='DIVID' [open]>
		//     <summary>LABEL GOES HERE</summary>
		//     CONTENT GOES HERE
		// </details>

		sb.append("\n");
		sb.append("<details id='").append(divId).append("' ").append(visibility).append(">\n");
		sb.append("    <summary>").append(label).append("</summary>\n");
		sb.append(content);
		sb.append("\n");
		sb.append("</details>\n");
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
	 * Calculate "Duration", and set that to a String column in the form "%HH:%MM:%SS"
	 * @param rstm
	 * @param timeInMsColName   Name of the column that holds Time in Milliseconds
	 * @param durationColName   Name of the column we want to set
	 */
	public void setMsToHms(ResultSetTableModel rstm, String timeInMsColName, String durationColName)
	{
		int pos_timeInMs   = rstm.findColumn(timeInMsColName);
		int pos_Duration   = rstm.findColumn(durationColName);

		if (pos_timeInMs >= 0 && pos_Duration >= 0)
		{
			for (int r=0; r<rstm.getRowCount(); r++)
			{
				Long timeInMs = rstm.getValueAsLong(r, pos_timeInMs);

				if (timeInMs != null)
				{
					String durationStr = TimeUtils.msToTimeStr("%HH:%MM:%SS", timeInMs);
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
		if (getReportingInstance().getInstanceRecordingInfo() != null)
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
	 * @param conn             DBMS Connection
	 * @param schemaName       Name of the DBMS Schema (can be null)
	 * @param cmName           CM Name
	 * @param graphName        Graph Name
	 * @param maxValue         -1 = No Max Value
	 * @param sort             Put the most active/heaviest series first (at left side)
	 * @param skipNames        Skip some Series (CVS String) 
	 * @param graphTitle       What's on top of the Graph
	 * 
	 * @return This will always returns a ReportChartObject object
	 */
	public IReportChart createTsLineChart(DbxConnection conn, String schemaName, String cmName, String graphName, int maxValue, boolean sort, String skipNames, String graphTitle)
	{
		return new ReportChartTimeSeriesLine(this, conn, schemaName, cmName, graphName, maxValue, sort, skipNames, graphTitle);
	}
//	public IReportChart createTsLineChart(DbxConnection conn, String cmName, String graphName, int maxValue, String skipNames, String graphTitle)
//	{
//		return new ReportChartTimeSeriesLine(this, conn, cmName, graphName, maxValue, false, skipNames, graphTitle);
//	}
	
	/**
	 * Simple wrapper method to create a ReportChartObject
	 * 
	 * @param conn             DBMS Connection
	 * @param schemaName       Name of the DBMS Schema (can be null)
	 * @param cmName           CM Name
	 * @param graphName        Graph Name
	 * @param maxValue         -1 = No Max Value
	 * @param skipNames        Skip some Series (CVS String) 
	 * @param graphTitle       What's on top of the Graph
	 * 
	 * @return This will always returns a ReportChartObject object
	 */
	public IReportChart createTsStackedBarChart(DbxConnection conn, String schemaName, String cmName, String dataGroupColumn, int dataGroupMinutes, TopGroupCountReport topGroupCountReport, String dataValueColumn, Double dataDivideByValue, String keepGroups, String skipGroups, String graphTitle)
	{
		return new ReportChartTimeSeriesStackedBar(this, conn, schemaName, cmName, dataGroupColumn, dataGroupMinutes, topGroupCountReport, dataValueColumn, dataDivideByValue, keepGroups, skipGroups, graphTitle);
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
	 * FIXME
	 * 
	 * @param conn
	 * @param rstm
	 * @param cmName
	 */
	public void fixDictionaryCompressedColumns(DbxConnection conn, ResultSetTableModel rstm, String cmName)
	{
		if (conn == null) return;
		if (rstm == null) return;

		// Get the list of columns ending with "$dcc$"
		List<String> dccColumns = new ArrayList<>(); 
		for (String colName : rstm.getColumnNames())
		{
			if (colName.endsWith("$dcc$"))
				dccColumns.add(colName);
		}

		// exit if no DCC columns
		if (dccColumns.isEmpty())
			return;

		// For every DCC to fix
		// - For each row in the RSTM 
		// - Get "actual" text instead of the "hash"
		// - Change the column name "strip off" the "$dcc$"
		for (String colName : dccColumns)
		{
			int replaceCount = 0;

			String originColName = colName.substring(0, colName.length() - "$dcc$".length());
			String dccTableName = cmName + "$dcc$" + originColName;
			
			for (int r=0; r<rstm.getRowCount(); r++)
			{
				int colPos = rstm.findColumn(colName);
				
				String hashId = rstm.getValueAsString(r, colName);
				String sql = conn.quotifySqlString("select [colVal] from [" + dccTableName + "] where [hashId] = '" + hashId + "'");
				
				String val = DbUtils.execQueryFirtColumnFirstRowNoThrow(conn, sql);

				if (StringUtil.hasValue(val))
				{
					rstm.setValueAtWithOverride(val, r, colPos);
					replaceCount++;
				}
			}

			// Change the "MetaData" for the column. (Column Name and Column Data Type)
			if (replaceCount > 0)
			{
				String oldColName = colName;
				String newColName = colName.substring(0, colName.length() - "$dcc$".length());

				rstm.renameColumn(oldColName, newColName);
			}
		}
	}

	/**
	 * If we have a begin/end period for this report, you want to add an extra criteria for the SQL Statement 
	 * @return
	 */
	public String getReportPeriodSqlWhere()
	{
		return getReportPeriodSqlWhere("SessionSampleTime");
	}

	/**
	 * If we have a begin/end period for this report, you want to add an extra criteria for the SQL Statement 
	 * @return
	 */
	public String getReportPeriodSqlWhere(String colname)
	{
		DailySummaryReportAbstract inst = getReportingInstance();
		
		// Most common: exit early
//		if (inst.getReportPeriodBeginTime() == null && inst.getReportPeriodEndTime() == null)
		if ( ! inst.hasReportPeriod() )
			return "";

		// we have both BEGIN and END time
		if (inst.getReportPeriodBeginTime() != null && inst.getReportPeriodEndTime() != null)
		{
			return "  AND [" + colname + "] BETWEEN '" + inst.getReportPeriodBeginTime() + "' AND '" + inst.getReportPeriodEndTime() + "' \n";
		}
		// we only have BEGIN time
		else if (inst.getReportPeriodBeginTime() != null)
		{
			return "  AND [" + colname + "] >= '" + inst.getReportPeriodBeginTime() + "' \n";
		}
		// we only have END time
		else if (inst.getReportPeriodEndTime() != null)
		{
			return "  AND [" + colname + "] <= '" + inst.getReportPeriodEndTime() + "' \n";
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



	//--------------------------------------------------------------------------------
	// BEGIN: Top rows
	//--------------------------------------------------------------------------------
	public String getTopRowsPropertyName()
	{
		return getClass().getSimpleName()+".top";
	}
	public int getTopRowsDefault()
	{
//		return 20;
		return 15;
	}
	public int getTopRows()
	{
//		Configuration conf = getReportingInstance().getLocalConfig();
		Configuration conf = Configuration.getCombinedConfiguration();
		int topRows = conf.getIntProperty(getTopRowsPropertyName(), getTopRowsDefault());
		return topRows;
	}
	//--------------------------------------------------------------------------------
	// END: Top rows
	//--------------------------------------------------------------------------------

	
	//--------------------------------------------------------------------------------
	// BEGIN: DSR - Daily Summary Report methods
	//--------------------------------------------------------------------------------
	protected String getDsrSkipEntriesAsHtmlTable()
	{
		String srvName   = getReportingInstance().getDbmsServerName();
		String className = this.getClass().getSimpleName();
		
		return getReportingInstance().getDsrSkipEntriesAsHtmlTable(srvName, className);
	}

	protected Map<String, Set<String>> getDsrSkipEntries()
	{
		String srvName   = getReportingInstance().getDbmsServerName();
		String className = this.getClass().getSimpleName();
		
		_skipEntries = getReportingInstance().getDsrSkipEntries(srvName, className);
		return _skipEntries;
	}
	private Map<String, Set<String>> _skipEntries;

	protected int getDsrSkipCount()
	{
		if (_skipEntries == null)
			_skipEntries = getDsrSkipEntries();

		int cnt = 0;

		for (Set<String> entry : _skipEntries.values())
		{
			cnt += entry.size();
		}
		
		return cnt;
	}

	protected ResultSetTableModel removeSkippedEntries(ResultSetTableModel rstm, int topRows, Map<String, Set<String>> dsrSkipEntries)
	{
		ResultSetTableModel allRemovedRows = new ResultSetTableModel(rstm, rstm.getName() + "_skippedRows", false);
		
		for (Entry<String, Set<String>> entry : dsrSkipEntries.entrySet())
		{
			String             colName = entry.getKey();
			Collection<String> values  = entry.getValue();

			List<ArrayList<Object>> removedRows = rstm.removeRows(colName, values);

			//System.out.println("removeSkippedEntries(): removeCnt="+removedRows.size()+", colName='"+colName+"', values='"+values+"'.");
			//if (!removedRows.isEmpty())
			//	System.out.println("REMOVED-ROWS: "+removedRows);
			
			if ( ! removedRows.isEmpty() )
				allRemovedRows.addRows(removedRows);
		}
		
		// finally (make sure that we have no more rows than "TOP" was saying
		// these rows should NOT be part of the returned 'allRemovedRows'
		if (topRows > 0)
		{
			while(rstm.getRowCount() > topRows)
			{
				rstm.removeRow(rstm.getRowCount() - 1);
			}
		}
		
		return allRemovedRows;
	}

	protected String createSkippedEntriesReport(ResultSetTableModel skipRstm)
	{
		String dbxCentralAdminUrl = getReportingInstance().getDbxCentralBaseUrl() + "/admin/admin.html";

		if (skipRstm == null)
			return "--NO-SKIPPED-ROWS (null)--<br><br> \n";
		
		if (skipRstm.getRowCount() == 0)
		{
			return "No <i>Skip</i> records was found. Skip records can be defined in DbxCentral under <a href='" + dbxCentralAdminUrl + "' target='_blank'>" + dbxCentralAdminUrl + "</a> or in the report table by pressing <i>'Skip This'</i> links in the below tables.<br> \n";
		}
		
		// Remove column 'SkipThis' or change the header and content to 'Remove This From The Skip Set'
		if (true)
		{
			skipRstm.removeColumn("SkipThis");
		}
		
		// add the content as a "hidden" section that user needs to expand to see
		StringBuilder sb = new StringBuilder();
		
		sb.append("<b>Skip Values:</b> The Skip Set contained " + getDsrSkipCount() + " entries, Record that <b>was skipped</b> was: " + skipRstm.getRowCount() + "<br> \n");
		sb.append("Note: Skip Values can be defined in DbxCentral under <a href='" + dbxCentralAdminUrl + "' target='_blank'>" + dbxCentralAdminUrl + "</a> or in the report table by pressing <i>'Skip This'</i> links in the below tables.<br> \n");

		String skipEntriesHtmlTable    = "<b>Table of the Skip Set.</b> <br>" 
		                               + getDsrSkipEntriesAsHtmlTable();

		String removedRecordsHtmlTable = "<b>Here is a Table of the " + skipRstm.getRowCount() + " skipped records.</b> <br>" 
		                               + skipRstm.toHtmlTableString("sortable");

		String htmlText = skipEntriesHtmlTable + removedRecordsHtmlTable;
		
		String showHideDiv = createShowHideDiv("skip-section", false, "Show/Hide Skipped entries...", htmlText);
		sb.append( msOutlookAlternateText(showHideDiv, "Skipped Entries", "") );
		sb.append("<br>");

		return sb.toString();
	}
	//--------------------------------------------------------------------------------
	// END: DSR - Daily Summary Report methods
	//--------------------------------------------------------------------------------
	

	@Override
	public List<ReportingIndexEntry> getReportingIndexes()
	{
		return Collections.emptyList();
	}

	@Override
	public void createReportingIndexes(DbxConnection conn)
	{
		List<ReportingIndexEntry> indexEntries = getReportingIndexes();

		// Early exit
		if (indexEntries == null)
			return;
		if (indexEntries.isEmpty())
			return;

		// Loop entries
		for (ReportingIndexEntry ie : indexEntries)
		{
			// Check if the table exists
			if ( ! DbUtils.checkIfTableExistsNoThrow(conn, null, null, ie.getTableName()) )
			{
				_logger.warn("createReportingIndexes(): The table '" + ie.getTableName() + "' does not exists.");
				continue;
			}

			String indexDdl  = "";
			try
			{
				String tableName = ie.getTableName();
				String indexName = "ix_DSR_" + tableName;
				String colNames  = "";
				String ixPostFix = "";

				// Check if index with SAME columns and order already exists
				// Get a Table entry for this table (the Table entry also holds Index information)
				boolean indexExists = false;
				Table tab = Table.create(conn, null, null, tableName);
				for (Index existingIndex : tab.getIndexes())
				{
					if (_logger.isDebugEnabled())
						_logger.debug("createReportingIndexes(): CHECKING TABLE '" + tableName + "' INDEX '" + existingIndex.getIndexName() + ", with COLUMNS: " + existingIndex.getColumnNames() + ", for matching columns of: " + ie.getIndexColumns());
					
					if (existingIndex.getColumnNames().equals(ie.getIndexColumns()))
					{
						_logger.debug("An index for table '" + tableName + "' with columns " + ie.getIndexColumns() + " already exists, skipping the create index.");
						indexExists = true;
						break;
					}
				}
				if (indexExists)
					continue;

				// Create index
				// 1: start to compose DDL
				String comma = "";
				for (String colName : ie.getIndexColumns())
				{
					colNames += comma + "[" + colName + "]";
					ixPostFix += "_" + colName.replaceAll("[^a-zA-Z0-9]", ""); // remove anything suspect chars keep only: a-z and numbers

					comma = ", ";
				}
				// add "column names" as a "postfix" at the end of the index name (there might be more than one index)
				indexName += ixPostFix;

				// Create the index
				if ( ! indexExists )
				{
					indexDdl = conn.quotifySqlString("create index [" + indexName + "] on [" + tableName + "] (" + colNames + ")");
					
					long startTime = System.currentTimeMillis();
					try (Statement stmnt = conn.createStatement())
					{
						stmnt.executeUpdate(indexDdl);
					}
					_logger.info("ReportEntry '" + getClass().getSimpleName() + "'. Created helper index to support Daily Summary Report. SQL='" + indexDdl + "' ExecTime=" + TimeUtils.msDiffNowToTimeStr(startTime));
				}
				else
				{
					// Write INFO on first "index already existed"
					if ( writeInfoOnIndexAlreadyExisted(indexName) )
						_logger.info("ReportEntry '" + getClass().getSimpleName() + "'. SKIPPED Creating helper index to support Daily Summary Report (it already exists). IndexName='" + indexName + "', SQL='" + indexDdl + "'.");
				}
			}
			catch (SQLException ex)
			{
				_logger.error("ReportEntry '" + getClass().getSimpleName() + "'. Problems creating a helper index, skipping the error and continuing... SQL=|" + indexDdl + "|.", ex);
			}
		}
	}

	public static class ReportingIndexEntry
	{
		private String _tableName;
		private List<String> _indexColumns;

		public ReportingIndexEntry(String tableName, String... columnNames)
		{
			_tableName    = tableName;
			_indexColumns = Arrays.asList(columnNames);
		}

		public String getTableName()
		{
			return _tableName;
		}

		public List<String> getIndexColumns()
		{
			if (_indexColumns == null)
				return Collections.emptyList();

			return _indexColumns;
		}
	}

	private Map<String, Integer> _writeInfoOnIndexAlreadyExisted_counter = new HashMap<>();
	public boolean writeInfoOnIndexAlreadyExisted(String indexName)
	{
		boolean doWrite = false;
		Integer cnt = _writeInfoOnIndexAlreadyExisted_counter.get(indexName);
		if (cnt == null)
		{
			doWrite = true;
			cnt = 0;
		}
		
		cnt++;
		_writeInfoOnIndexAlreadyExisted_counter.put(indexName, cnt);

		return doWrite;
	}








//	/**
//	 * Parse the passed SQL Text and extract table names<br>
//	 * Those table names we then get various information about like
//	 * <ul>
//	 *    <li>Table estimated RowCount, Size in various formats, Index Size, etc</li>
//	 *    <li>Each index will have a separate entry, also showing 'index_names', 'keys', sizes etc...</li>
//	 *    <li>Formatted SQL Text in a separate <i>hover-able</i> tool-tip or dialog pop-up</li>
//	 * </ul>
//	 * 
//	 * @param conn             Connection to the PCS -- Persistent Counter Storage
//	 * @param sqlText          The SQL Text we will parse for table names (which table names to lookup)
//	 * @param dbmsVendor       The DBMS Vendor the above SQL Text was executed in (SQL Dialect). If null or "" (a <i>standard</i> SQL type will be used)
//	 * @return                 HTML Text (table) with detailed information about the Table and Indexes used by the SQL Text
//	 */
//	public String getTableInformationFromSqlText(DbxConnection conn, String sqlText, String dbmsVendor)
//	{
//		// Possibly get from configuration
//		boolean parseSqlText = true;
//		if ( ! parseSqlText )
//			return "";
//		
//		String tableInfo = "";
//
//		// Parse the SQL Text to get all tables that are used in the Statement
//		String problemDesc = "";
//		Set<String> tableList = SqlParserUtils.getTables(sqlText);
////		List<String> tableList = Collections.emptyList();
////		try { tableList = SqlParserUtils.getTables(sqlText, true); }
////		catch (ParseException pex) { problemDesc = pex + ""; }
//
//		// Get information about ALL tables in list 'tableList' from the DDL Storage
//		Set<GenericTableInfo> tableInfoSet = getTableInformationFromMonDdlStorage(conn, tableList);
//		if (tableInfoSet.isEmpty() && StringUtil.isNullOrBlank(problemDesc))
//			problemDesc = "&emsp; &bull; No tables was found in the DDL Storage for tables: " + listToHtmlCode(tableList);
//
//		// And make it into a HTML table with various information about the table and indexes 
//		tableInfo = problemDesc + getTableInfoAsHtmlTable(tableInfoSet, tableList, true, "dsr-sub-table-tableinfo");
//
//		// Finally make up a message that will be appended to the SQL Text
//		if (StringUtil.hasValue(tableInfo))
//		{
//			// Surround with collapse div
//			tableInfo = ""
//					//+ "<!--[if !mso]><!--> \n" // BEGIN: IGNORE THIS SECTION FOR OUTLOOK
//
//					+ "\n<br>\n"
//					+ getFormattedSqlAsTooltipDiv(sqlText, dbmsVendor) + "\n"
////					+ "<br>\n"
//					+ "<details open> \n"
//					+ "<summary>Show/Hide Table information for " + tableList.size() + " table(s): " + listToHtmlCode(tableList) + "</summary> \n"
//					+ tableInfo
//					+ "</details> \n"
//
//					//+ "<!--<![endif]-->    \n" // END: IGNORE THIS SECTION FOR OUTLOOK
//					+ "";
//		}
//		
//		return tableInfo;
//	}
//
//	public static class GenericTableInfo {}
//	
////	protected abstract Set<GenericTableInfo> getTableInformationFromMonDdlStorage(DbxConnection conn, Set<String> tableList);
////	protected abstract String getTableInfoAsHtmlTable(Set<GenericTableInfo> tableInfoSet, Set<String> tableList, boolean b, String string);
//	protected abstract <T extends GenericTableInfo> Set<T> getTableInformationFromMonDdlStorage(DbxConnection conn, Set<String> tableList);
//	protected abstract <T extends GenericTableInfo> String getTableInfoAsHtmlTable(Set<T> tableInfoSet, Set<String> tableList, boolean b, String string);
	
	/**
	 * Parse the passed SQL Text and extract table names<br>
	 * Those table names we then get various information about like
	 * <ul>
	 *    <li>Table estimated RowCount, Size in various formats, Index Size, etc</li>
	 *    <li>Each index will have a separate entry, also showing 'index_names', 'keys', sizes etc...</li>
	 *    <li>Formatted SQL Text in a separate <i>hover-able</i> tool-tip or dialog pop-up</li>
	 * </ul>
	 * 
	 * NOTE: This is a wrapper for any *real* implementation, which uses: {@link #getDbmsTableInfoAsHtmlTable(DbxConnection, Set<String>, boolean, String)}
	 * 
	 * @param conn             Connection to the PCS -- Persistent Counter Storage
	 * @param currentDbname    Name of the current database we search for (This can be empty or null)
	 * @param sqlText          The SQL Text we will parse for table names (which table names to lookup)
	 * @param dbmsVendor       The DBMS Vendor the above SQL Text was executed in (SQL Dialect). If null or "" (a <i>standard</i> SQL type will be used)
	 * @return                 HTML Text (table) with detailed information about the Table and Indexes used by the SQL Text
	 */
	public String getDbmsTableInformationFromSqlText(DbxConnection conn, String currentDbname, String sqlText, String dbmsVendor)
	{
		// Possibly get from configuration
		boolean parseSqlText = true;
		if ( ! parseSqlText )
			return "";
		
		String tableInfo = "";

		// Parse the SQL Text to get all tables that are used in the Statement
//		String problemDesc = "";
		Set<String> tableList = SqlParserUtils.getTables(sqlText);
//		List<String> tableList = Collections.emptyList();
//		try { tableList = SqlParserUtils.getTables(sqlText, true); }
//		catch (ParseException pex) { problemDesc = pex + ""; }

//-------------------------------------------------
//		// Get information about ALL tables in list 'tableList' from the DDL Storage
//		Set<PgTableInfo> tableInfoSet = getTableInformationFromMonDdlStorage(conn, tableList);
//		if (tableInfoSet.isEmpty() && StringUtil.isNullOrBlank(problemDesc))
//			problemDesc = "&emsp; &bull; No tables was found in the DDL Storage for tables: " + listToHtmlCode(tableList);
//
//		// And make it into a HTML table with various information about the table and indexes 
//		tableInfo = problemDesc + getTableInfoAsHtmlTable(tableInfoSet, tableList, true, "dsr-sub-table-tableinfo");
//-------------------------------------------------
// The above calls was collapsed and put in a method 'getDbmsTableInfoAsHtmlTable' 
// which is implemented by any subclass... because I couldn't in a easy way make it Generic (or I'm to inexperienced with Java Generic's to make it work)
//-------------------------------------------------

		// Get information about ALL tables in list 'tableList' from the DDL Storage
		tableInfo = getDbmsTableInfoAsHtmlTable(conn, currentDbname, tableList, true, "dsr-sub-table-tableinfo");
		if (StringUtil.isNullOrBlank(tableInfo))
		{
			for (String tabName : tableList)
			{
				tableInfo += "&emsp; &bull; Table <code>" + tabName + "</code> was NOT found in the DDL Storage.<br>\n";
			}
		}
		
		
		// Finally make up a message that will be appended to the SQL Text
		if (StringUtil.hasValue(tableInfo))
		{
			// Surround with collapse div
			tableInfo = ""
					//+ "<!--[if !mso]><!--> \n" // BEGIN: IGNORE THIS SECTION FOR OUTLOOK

					+ "\n<br>\n"
					+ getFormattedSqlAsTooltipDiv(sqlText, "Hover or Click to see formatted SQL Text...", dbmsVendor) + "\n"
//					+ "<br>\n"
					+ "<details open> \n"
					+ "<summary>Show/Hide Table information for " + tableList.size() + " table(s): " + listToHtmlCode(tableList) + "</summary> \n"
					+ tableInfo
					+ "</details> \n"

					//+ "<!--<![endif]-->    \n" // END: IGNORE THIS SECTION FOR OUTLOOK
					+ "";
		}
		
		return tableInfo;
	}

	/**
	 * Get various information from DDL Storage about table and it's indexes
	 * 
	 * @param conn
	 * @param tableList
	 * @param includeIndexInfo
	 * @param classname
	 * @return
	 */
	protected String getDbmsTableInfoAsHtmlTable(DbxConnection conn, String currentDbname, Set<String> tableList, boolean includeIndexInfo, String classname)
	{
		throw new RuntimeException("getDbmsTableInfoAsHtmlTable(DbxConnection conn, String currentDbname, Set<String> tableList, boolean includeIndexInfo, String classname) -- Must be implemented by any subclass.");
	}

	public String getFormattedSqlAsTooltipDiv(String sql, String labelText, String vendor)
	{
		String displayText = ""
				+ "------------------------------------------------------------------------------------------------------------------------- \n"
				+ "-- Formatted SQL Text \n"
				+ "------------------------------------------------------------------------------------------------------------------------- \n"
				+ SqlUtils.format(sql, vendor) 
				+ "\n"
				+ "\n"
				+ "\n"
				+ "------------------------------------------------------------------------------------------------------------------------- \n"
				+ "-- Origin SQL Text \n"
				+ "------------------------------------------------------------------------------------------------------------------------- \n"
				+ sql
				+ "\n"
				+ "\n"
				+ "";

		displayText = StringEscapeUtils.escapeHtml4(displayText);

		
		// Put the "Actual Executed SQL Text" as a "tooltip"
		return "<div title='" + labelText + "' "
				+ "data-toggle='modal' "
				+ "data-target='#dbx-view-sqltext-dialog' "
//				+ "data-objectname='" + Hashkey + "' "
				+ "data-tooltip=\""   + displayText     + "\" "
				+ ">&#x1F4AC; " + labelText + "</div>"; // &#x1F4AC; ==>> symbol popup with "..."
	}

	public String getTextAsTooltipDiv(String displayText, String labelText)
	{
		displayText = StringEscapeUtils.escapeHtml4(displayText);
		
		// Put the "Actual Executed SQL Text" as a "tooltip"
		return "<div title='" + labelText + "' "
				+ "data-toggle='modal' "
				+ "data-target='#dbx-view-sqltext-dialog' "
//				+ "data-objectname='" + Hashkey + "' "
				+ "data-tooltip=\""   + displayText     + "\" "
				+ ">&#x1F4AC; " + labelText + "</div>"; // &#x1F4AC; ==>> symbol popup with "..."
	}
	

	/**
	 * Write a HTML "TH" with plain old tooltip
	 * 
	 * @param colName   Name of the column
	 * @param tooltip   tooltip (can be null)
	 * @return
	 */
	public String createHtmlTh(String colName, String tooltip)
	{
		if (StringUtil.hasValue(tooltip))
		{
			tooltip = tooltip.replace("'", "&#39;");
			return "  <th title='" + tooltip + "'>" + colName +"</th> \n";
		}

		return "  <th>" + colName +"</th> \n";
	}

	/**
	 * Write DIFF and ABS values as a "formatted String": <code><b>diffValue</b> &#9887; <i>absValue</i></code><br>
	 * If both diff and abs values are -1, the we write <code>-not-found-</code>
	 * 
	 * @param diff
	 * @param abs
	 * @return A formatted string
	 */
	public String diffAbsValues(Object diff, Object abs)
	{
		String notAvaliable = "--not-found--";
		String sepStr       = " &#9887; ";
		
		if (diff == null || abs == null)
			return notAvaliable;

		if (diff instanceof Number && abs instanceof Number)
		{
			if (((Number)diff).intValue() == -1 && ((Number)abs).intValue() == -1)
				return notAvaliable;
			
			NumberFormat nf = NumberFormat.getInstance();
			return "<b>" + nf.format(diff) + "</b>" + sepStr + "<i>" + nf.format( abs ) + "</i>";
		}
		else
		{
			return "<b>" + diff + "</b>" + sepStr + "<i>" + abs + "</i>";
		}
	}

	/**
	 * Check if status entry in the "head" report exists 
	 * @param statusKey
	 * @return if the value has previously been set or not
	 */
	@Override
	public boolean hasStatusEntry(String statusKey)
	{
		return getReportingInstance().hasStatusEntry(statusKey);
	}

	/**
	 * Set status entry in the "head" report 
	 * 
	 * @param statusKey    name of the status
	 * @return The previous value, if not previously set it will be null
	 */
	@Override
	public Object setStatusEntry(String statusKey)
	{
		return getReportingInstance().setStatusEntry(statusKey);
	}
	
	
	/**
	 * Called at start of writing a Report Entry, so we can track some statistics
	 * @param writer
	 * @param fullMessage
	 */
	@Override
	public void beginWriteEntry(Writer writer, MessageType messageType)
	{
		if (writer instanceof CountingWriter)
		{
			CountingWriter countingWriter = (CountingWriter) writer;
			
			if (MessageType.FULL_MESSAGE.equals(messageType))
			{
				_bytesWrittenAtStart_fullMessage = countingWriter.getCharsWritten();
			}
			else if (MessageType.SHORT_MESSAGE.equals(messageType))
			{
				_bytesWrittenAtStart_shortMessage = countingWriter.getCharsWritten();
			}
			else if (MessageType.MINIMAL_MESSAGE.equals(messageType))
			{
				_bytesWrittenAtStart_minimalMessage = countingWriter.getCharsWritten();
			}
			else
			{
				_logger.error("beginWriteEntry(): Unhandled messageType='" + messageType + "'.");
			}
		}
	}

	/**
	 * Called at start of writing a Report Entry, so we can track some statistics
	 * @param writer
	 * @param fullMessage
	 */
	@Override
	public void endWriteEntry(Writer writer, MessageType messageType)
	{
		if (writer instanceof CountingWriter)
		{
			CountingWriter countingWriter = (CountingWriter) writer;
			
			if (MessageType.FULL_MESSAGE.equals(messageType))
			{
				_bytesWrittenAtEnd_fullMessage = countingWriter.getCharsWritten();
				_bytesWritten_fullMessage = _bytesWrittenAtEnd_fullMessage - _bytesWrittenAtStart_fullMessage;
			}
			else if (MessageType.SHORT_MESSAGE.equals(messageType))
			{
				_bytesWrittenAtEnd_shortMessage = countingWriter.getCharsWritten();
				_bytesWritten_shortMessage = _bytesWrittenAtEnd_shortMessage - _bytesWrittenAtStart_shortMessage;
			}
			else if (MessageType.MINIMAL_MESSAGE.equals(messageType))
			{
				_bytesWrittenAtEnd_minimalMessage = countingWriter.getCharsWritten();
				_bytesWritten_minimalMessage = _bytesWrittenAtEnd_minimalMessage - _bytesWrittenAtStart_minimalMessage;
			}
			else
			{
				_logger.error("endWriteEntry(): Unhandled messageType='" + messageType + "'.");
			}
		}
	}
	private long _bytesWrittenAtStart_fullMessage;
	private long _bytesWrittenAtStart_shortMessage;
	private long _bytesWrittenAtStart_minimalMessage;
	private long _bytesWrittenAtEnd_fullMessage;
	private long _bytesWrittenAtEnd_shortMessage;
	private long _bytesWrittenAtEnd_minimalMessage;
	private long _bytesWritten_fullMessage  = -1;
	private long _bytesWritten_shortMessage = -1;
	private long _bytesWritten_minimalMessage = -1;

	@Override
	public long getCharsWrittenKb(MessageType messageType)
	{
		if (MessageType.FULL_MESSAGE.equals(messageType))
		{
			return _bytesWritten_fullMessage == -1 ? -1 : _bytesWritten_fullMessage / 1024;
		}
		else if (MessageType.SHORT_MESSAGE.equals(messageType))
		{
			return _bytesWritten_shortMessage == -1 ? -1 : _bytesWritten_shortMessage / 1024;
		}
		else if (MessageType.MINIMAL_MESSAGE.equals(messageType))
		{
			return _bytesWritten_minimalMessage == -1 ? -1 : _bytesWritten_minimalMessage / 1024;
		}
		else
		{
			_logger.error("getCharsWrittenKb(): Unhandled messageType='" + messageType + "'.");
			return -1;
		}
	}

	/**
	 * Mark with a yellow HTML label if input DIFFERS
	 * @param dbname1 
	 * @param dbname2
	 * @return
	 */
	public String markIfDifferent(String dbname1, String dbname2)
	{
		if (StringUtil.isNullOrBlank(dbname1) || StringUtil.isNullOrBlank(dbname2))
			return dbname1;
		
		if (dbname1.equals(dbname2))
			return dbname1;
		
//		return "<mark>" + dbname1 + "</mark>";
		return "<span style='background-color: yellow'>" + dbname1 + "</span>";
	}


	/**
	 * BEGIN A subsection of a report
	 * 
	 * @param w
	 * @param title
	 * @param id
	 * @throws IOException
	 */
	public void beginHtmlSubSection(Writer w, String title, String id)
	throws IOException
	{
		id = id.replace(' ', '_');

		boolean useBootstrap = true;
		if (getReportingInstance() instanceof DailySummaryReportDefault)
			useBootstrap = ((DailySummaryReportDefault)getReportingInstance()).useBootstrap();

		if (useBootstrap)
		{
			// Bootstrap "card" - BEGIN
			w.append("<!--[if !mso]><!--> \n"); // BEGIN: IGNORE THIS SECTION FOR OUTLOOK
			w.append("<div id='" + id + "' class='card border-dark mb-3'>");
			w.append("<h5 class='card-header'><b>" + title + "</b></h5>");
			w.append("<div class='card-body'>");
			w.append("<!--<![endif]-->    \n"); // END: IGNORE THIS SECTION FOR OUTLOOK
		
			w.append("<!--[if mso]> \n"); // BEGIN: ONLY FOR OUTLOOK
			w.append("<br>\n");
			w.append("<h3 id='" + id + "'>" + title + "</h3> \n");
			w.append("<![endif]-->  \n"); // END: ONLY FOR OUTLOOK
		}
		else
		{
			w.append("<br>\n");
			w.append("<h3 id='" + id + "'>" + title + "</h3> \n");
		}
	}

	/**
	 * END a subsection
	 * 
	 * @param w
	 * @throws IOException
	 */
	public void endHtmlSubSection(Writer w) 
	throws IOException
	{
		boolean useBootstrap = true;
		if (getReportingInstance() instanceof DailySummaryReportDefault)
			useBootstrap = ((DailySummaryReportDefault)getReportingInstance()).useBootstrap();

		// Section FOOTER
		if (useBootstrap)
		{
			// Bootstrap "card" - END
			w.append("<!--[if !mso]><!--> \n"); // BEGIN: IGNORE THIS SECTION FOR OUTLOOK
			w.append("</div>"); // end: card-body
			w.append("</div>"); // end: card
			w.append("<!--<![endif]-->    \n"); // END: IGNORE THIS SECTION FOR OUTLOOK
		}
	}

}

