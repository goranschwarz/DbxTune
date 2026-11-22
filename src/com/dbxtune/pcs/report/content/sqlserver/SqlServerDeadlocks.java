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
package com.dbxtune.pcs.report.content.sqlserver;

import java.io.IOException;
import java.io.Writer;
import java.lang.invoke.MethodHandles;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.CounterControllerSqlServer;
import com.dbxtune.cm.sqlserver.CmDeadlocks;
import com.dbxtune.cm.sqlserver.CmDeadlocks.Deadlock;
import com.dbxtune.cm.sqlserver.CmDeadlocks.DlProcess;
import com.dbxtune.cm.sqlserver.CmPerfCounters;
import com.dbxtune.cm.sqlserver.SqlServerDeadlockTimelinePainter;
import com.dbxtune.cm.sqlserver.CmSummary;
import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.PersistWriterJdbc;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.pcs.report.content.IReportChart;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.DbUtils;
import com.dbxtune.utils.StringUtil;

public class SqlServerDeadlocks 
extends SqlServerAbstract
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public SqlServerDeadlocks(DailySummaryReportAbstract reportingInstance)
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

	@Override
	public void writeMessageText(Writer w, MessageType messageType)
	throws IOException
	{
		if (_deadlockSummaryPeriodCount > 0)
		{
			// in RED
			w.append("<p style='color:red'><b>Found " + _deadlockSummaryPeriodCount + " Deadlocks in the recording period...</b></p>");

			w.append(getDbxCentralLinkWithDescForGraphs(false, "Below are Deadlock Count Graphs/Charts, to indicate when on the hour you have problems",
					"CmSummary_DeadlockCountSum",
					"CmPerfCounters_DeadlockDetails"
					));

			if (_CmSummary_DeadlockCountSum     != null) _CmSummary_DeadlockCountSum    .writeHtmlContent(w, null, null);
			if (_CmPerfCounters_DeadlockDetails != null) _CmPerfCounters_DeadlockDetails.writeHtmlContent(w, null, null);

			// Deadlocks saved by CmDeadlocks (one row per deadlock, with the XML)
			if ( ! _cmDeadlocks.isEmpty() )
			{
				writeCmDeadlocks(w);
			}
			// sp_BlitzLock info
			else if (StringUtil.hasValue(_deadlockSummaryPeriodReport_spBlitzLock))
			{
				w.append("<p>Below are some Deadlock Information from: sp_BlitzLock</p>");

				//---------------------------------------------------------
				// Section 3 is HIGH Level or SUMMARY, so it goes first
				//---------------------------------------------------------
				if (_spBlitzLock_section3 != null)
				{
//					w.append("<p><b>A high-level analysis of all the stuff we pull out.</b></p>");
//					w.append(_spBlitzLock_section3.toHtmlTableString("sortable"));
					
					String  divId       = "DeadlockSummary";
					boolean showAtStart = true;
					String  htmlContent = _spBlitzLock_section3.toHtmlTableString("sortable");

					String showHideDiv = createShowHideDiv(divId, showAtStart, "<b>A high-level analysis of all the stuff we pull out.</b> (" + _spBlitzLock_section3.getRowCount() + " rows)", htmlContent);
					w.append( msOutlookAlternateText(showHideDiv, divId, null) );
				}
				else
				{
					w.append("<p>NOTE: NO high-level analysis was available (_spBlitzLock_section_3 == null)</p>");
				}

				//---------------------------------------------------------
				// Section 1 is Details
				//---------------------------------------------------------
				if (_spBlitzLock_section1 != null)
				{
					w.append("<p></p>");
//					w.append("<p><b>Below are each of the deadlocks</b></p>");

					ReportEntryTableStringRenderer tableRender = new ReportEntryTableStringRenderer()
					{
						@Override
						public String cellValue(ResultSetTableModel rstm, int row, int col, String colName, Object objVal, String strVal)
						{
							if (    "query"                    .equals(colName) // starts with "<?query \n" and ends with "?>"
							     || "object_names"             .equals(colName) // "<object>tempdb.dbo.deadlock_right</object>"
							     || "parallel_deadlock_details".equals(colName) // "<parallel_deadlock_details/>"
							     || "deadlock_graph"           .equals(colName) // "<deadlock>...</deadlock>"
							   ) 
							{
								String htmlClassName = "class='dbx-xml-text-cell'";

								if ("query".equals(colName))
								{
									strVal = strVal.replace("<?query ", "").replace("  ?>", ""); // remove: prefix/suffix
									htmlClassName = "class='dbx-sql-text-cell'";
								}

								if ("deadlock_graph".equals(colName)) // "<deadlock>...</deadlock>"
								{
									strVal = StringUtil.xmlFormat(strVal);
								}

//								strVal = "<pre class='language-xml'>" + StringEscapeUtils.escapeHtml4(strVal) + "</pre>";
								strVal = "<pre " + htmlClassName + ">" + StringEscapeUtils.escapeHtml4(strVal) + "</pre>";
							}
							
							// Make client_option_# a UnOrdered List
							if (colName != null && colName.startsWith("client_option_"))
							{
								String unorderedListStr = "<ul> \n";
								for (String str : StringUtil.parseCommaStrToList(strVal, true))
									unorderedListStr += "  <li>" + str + "</li> \n";
								unorderedListStr += "</ul> \n";
								
								strVal = unorderedListStr;
							}

						//	if ("deadlock_graph".equals(colName))
						//	{
						//		//TODO; // can we make a button/link for column "deadlock_graph" to "Download" or "Open in SSMS/SqlSentry" to view the XML deadlock reports
						//	}

							return strVal;
						}
						
					};
//					w.append(_spBlitzLock_section1.toHtmlTableString("sortable", true, true, null, tableRender));
					
					String  divId       = "DeadlockDetails";
					boolean showAtStart = false;
					String  htmlContent = _spBlitzLock_section1.toHtmlTableString("sortable", true, true, null, tableRender);

					String showHideDiv = createShowHideDiv(divId, showAtStart, "<b>Below are each of the deadlocks</b> (" + _spBlitzLock_section1.getRowCount() + " rows)", htmlContent);
					w.append( msOutlookAlternateText(showHideDiv, divId, null) );
				}
				else
				{
					w.append("<p>NOTE: NO deadlock details Available (_spBlitzLock_section_1 == null)</p>");
				}

				//---------------------------------------------------------
				// Section 2 is info from "plan cache"
				//---------------------------------------------------------
				if (_spBlitzLock_section2 != null)
				{
					w.append("<p></p>");
//					w.append("<p><b>This section holds information about the statements (if they are still in the Cache Plan)</b></p>");
//					w.append(_spBlitzLock_section2.toHtmlTableString("sortable"));

					String  divId       = "DeadlockFromPlanCache";
					boolean showAtStart = false;
					String  htmlContent = _spBlitzLock_section2.toHtmlTableString("sortable");

					String showHideDiv = createShowHideDiv(divId, showAtStart, "<b>This section holds information about the statements (if they are still in the Cache Plan)</b> (" + _spBlitzLock_section2.getRowCount() + " rows)", htmlContent);
					w.append( msOutlookAlternateText(showHideDiv, divId, null) );
				}
				else
				{
					w.append("<p>NOTE: NO deadlock Statements from PlanCache Available (_spBlitzLock_section_2 == null)</p>");
				}
			}
			else
			{
				w.append("<p><b>No deadlock details: enable the collector '" + CmDeadlocks.CM_NAME + "' (Deadlocks), or install 'sp_BlitzLock' on the server '" + getReportingInstance().getDbmsServerName() + "', to see which objects and statements deadlocked.</b></p>");
			}

			// Print any Info Messages
			if (hasInfogMsg())
			{
				w.append(getInfoMsg());
			}

			// OTHER info
//			if (StringUtil.hasValue(_deadlockSummaryPeriodReport_xxx))
//			{
//				w.append("Below are some Deadlock Information from: XXX <br>");
//			}
		}
		else
		{
			if (_deadlockSummaryPeriodCount == 0)
			{
				// in GREEN
				w.append("<p style='color:green'>Found " + _deadlockSummaryPeriodCount + " Deadlocks in the recording period...<br><br></p>");
			}
		}
	}

	@Override
	public String getSubject()
	{
		return "Deadlock Information for the full day (origin: CmSummary,CmPerfCounters,CmDeadlocks or sp_BlitzLock)";
	}

	@Override
	public IssueLevel getIssueLevel()
	{
		// FYI (as the deadlock alarm, which is an INFO alarm): some systems have many deadlocks every day, they would drown in "issue" mails
		return _deadlockSummaryPeriodCount > 0 ? IssueLevel.FYI : IssueLevel.NONE;
	}

	@Override
	public String getIssueReason()
	{
		return _deadlockSummaryPeriodCount + " deadlock(s)";
	}


	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		String schema = getReportingInstance().getDbmsSchemaName();

		try 
		{
			// Get value from PCS KeyValue Store
			try
			{
				_deadlockSummaryPeriodCount = PersistWriterJdbc.getKeyValueStoreAsInteger(conn, CounterControllerSqlServer.PCS_KEY_VALUE_deadlockCountOverRecordingPeriod, -1);
			}
			catch (SQLException ex)
			{
				_logger.warn("Problems getting Deadlock Count from 'PCS Key Value Store'.", ex);
			}
//System.out.println("_deadlockSummaryPeriodCount[STEP-1-KeyValStore]=" + _deadlockSummaryPeriodCount);
			
			String sqlGetDeadlockCount = "";
			String dummySql = "select * from [CmSummary_DeadlockCountSum] where 1 = 2";
			ResultSetTableModel dummyRstm = ResultSetTableModel.executeQuery(conn, dummySql, true, "metadata");
			if (dummyRstm.hasColumn("label_0") && dummyRstm.hasColumn("data_0"))
			{
				 // OLD Style: Label in separate column ("label_#")
				sqlGetDeadlockCount = conn.quotifySqlString("select sum([data_0]) from [CmSummary_DeadlockCountSum] where [label_0] = 'Deadlock Count'");
			}
			else
			{
				// NEW Style: ColumnName is Label
				sqlGetDeadlockCount = conn.quotifySqlString("select sum([Deadlock Count]) from [CmSummary_DeadlockCountSum]"); 
			}

			// deadlock count from the "chart/graph" table (SQL created above for OLD/NEW Style)
			try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sqlGetDeadlockCount))
			{
				int deadlockCount = -1;
				while(rs.next())
					deadlockCount = rs.getInt(1);

//System.out.println("_deadlockSummaryPeriodCount[KeyValStore]=" + _deadlockSummaryPeriodCount + ", deadlockCount[CmSummary_DeadlockCountSum]=" + deadlockCount);
				_deadlockSummaryPeriodCount = deadlockCount;
			}
			catch (SQLException ex)
			{
				_logger.warn("Problems getting Deadlock Count from 'CmSummary_DeadlockCountSum' using SQL=|" + sqlGetDeadlockCount + "|", ex);
			}

			if (_deadlockSummaryPeriodCount > 0)
			{
				readCmDeadlocks(conn, schema);
			}

			// sp_BlitzLock: only for recordings without CmDeadlocks rows
			if (_deadlockSummaryPeriodCount > 0 && _cmDeadlocks.isEmpty())
			{
				_deadlockSummaryPeriodReport_spBlitzLock = PersistWriterJdbc.getKeyValueStoreAsString(conn, CounterControllerSqlServer.PCS_KEY_VALUE_deadlockReport_blitzLock);

//System.out.println("_deadlockSummaryPeriodReport_spBlitzLock=|" + _deadlockSummaryPeriodReport_spBlitzLock + "|");
				if (StringUtil.hasValue(_deadlockSummaryPeriodReport_spBlitzLock))
				{
					if (_deadlockSummaryPeriodReport_spBlitzLock.startsWith("ERROR: ErrorCode=") || _deadlockSummaryPeriodReport_spBlitzLock.startsWith("Msg "))
					{
						_logger.info  ("Some error(s) was caught when calling 'sp_BlitzLock', this needs to be fixed: " + _deadlockSummaryPeriodReport_spBlitzLock);
						addInfoMessage("Some error(s) was caught when calling 'sp_BlitzLock', this needs to be fixed: " + _deadlockSummaryPeriodReport_spBlitzLock);
					}
					else
					{
						// Decode the report(s) into ResultSetTableModel objects so we can print some HTML Tables out of it
						List<ResultSetTableModel> rstmList = ResultSetTableModel.parseJsonMultiText(_deadlockSummaryPeriodReport_spBlitzLock);
//System.out.println(">>>>>>>>>>>>>>>>>>>>>> rstmList.size()==" + rstmList.size());
						if (rstmList.size() >= 1) _spBlitzLock_section1 = rstmList.get(0);
						if (rstmList.size() >= 2) _spBlitzLock_section2 = rstmList.get(1);
						if (rstmList.size() >= 3) _spBlitzLock_section3 = rstmList.get(2);
					}
				}
				else
				{
					_logger.info("NO 'sp_BlitzLock' has been saved in the PCS Key Value store with key '" + CounterControllerSqlServer.PCS_KEY_VALUE_deadlockReport_blitzLock + "'.");
					addInfoMessage("Some error(s) was caught when calling 'sp_BlitzLock', this needs to be fixed: " + _deadlockSummaryPeriodReport_spBlitzLock);
				}
			}

			if (_deadlockSummaryPeriodCount > 0)
			{
				// Get some Charts
				_CmSummary_DeadlockCountSum     = createTsLineChart(conn, schema, CmSummary     .CM_NAME,    CmSummary     .GRAPH_NAME_DEADLOCK_COUNT_SUM, -1, false, null,    "Deadlock Count (Summary)");
				_CmPerfCounters_DeadlockDetails = createTsLineChart(conn, schema, CmPerfCounters.CM_NAME,    CmPerfCounters.GRAPH_NAME_DEADLOCK_DETAILS  , -1, false, null,    "Deadlock Count Details (Server->Perf Counters)");
			}
			else
			{
				_logger.info("No Deadlocks were found in the period. _deadlockSummaryPeriodCount=" + _deadlockSummaryPeriodCount);
			}
		}
		catch (SQLException ex) 
		{
			_logger.error("Problems creating Deadlock Report.", ex);
			addProblemMessage("Problems creating Deadlock Report. Caught: " + ex);
		}		
	}
	
	private int    _deadlockSummaryPeriodCount              = -1;
	private String _deadlockSummaryPeriodReport_spBlitzLock = null;
//	private String _deadlockSummaryPeriodReport_xxx         = null;

	private ResultSetTableModel _spBlitzLock_section1 = null;
	private ResultSetTableModel _spBlitzLock_section2 = null;
	private ResultSetTableModel _spBlitzLock_section3 = null;
	
	private IReportChart _CmSummary_DeadlockCountSum;
	private IReportChart _CmPerfCounters_DeadlockDetails;

	/** Deadlocks from CmDeadlocks_abs, oldest first */
	private List<Deadlock> _cmDeadlocks = new ArrayList<>();

	private static final int MAX_SHAPES_IN_TABLE = 25;
	private static final int MAX_PICTURES        = 10;
	private static final int MAX_DETAIL_ROWS     = 500;

	/**
	 * Read the deadlocks saved by CmDeadlocks (if the recording has the table)
	 */
	private void readCmDeadlocks(DbxConnection conn, String schema)
	{
		String tabName = CmDeadlocks.CM_NAME + "_abs";
		if ( ! DbUtils.checkIfTableExistsNoThrow(conn, null, schema, tabName) )
		{
			_logger.info("Table '" + tabName + "' does not exist in the recording, deadlock details are taken from 'sp_BlitzLock' (if available).");
			return;
		}

		String sql = ""
			+ "select [EventTime], [DeadlockId], [DeadlockXml] \n"
			+ "from " + (StringUtil.hasValue(schema) ? "[" + schema + "]." : "") + "[" + tabName + "] \n"
			+ "where 1 = 1 \n"
			+ getReportPeriodSqlWhere()
			+ "order by [EventTime] \n"
			;

		ResultSetTableModel rstm = executeQuery(conn, sql, false, "CmDeadlocks", false);
		if (rstm == null)
			return;

		int pos_EventTime   = rstm.findColumn("EventTime");
		int pos_DeadlockId  = rstm.findColumn("DeadlockId");
		int pos_DeadlockXml = rstm.findColumn("DeadlockXml");

		Set<String> seen = new HashSet<>();
		int parseErrors = 0;
		for (int r = 0; r < rstm.getRowCount(); r++)
		{
			String id = String.valueOf(rstm.getValueAt(r, pos_DeadlockId));
			if ( ! seen.add(id) )
				continue;
			try
			{
				Object ts = rstm.getValueAt(r, pos_EventTime);
				_cmDeadlocks.add(Deadlock.parseDeadlock(String.valueOf(rstm.getValueAt(r, pos_DeadlockXml)), ts instanceof Timestamp ? (Timestamp) ts : null));
			}
			catch (Exception ex)
			{
				parseErrors++;
			}
		}
		if (parseErrors > 0)
			addInfoMessage("Problems parsing " + parseErrors + " deadlock XML(s) from '" + tabName + "', they are not included.");

		_logger.info("Read " + _cmDeadlocks.size() + " deadlocks from '" + tabName + "'.");
	}

	/** Deadlocks grouped by shape, most deadlocks first. Each list: oldest first */
	private List<List<Deadlock>> getShapes()
	{
		Map<String, List<Deadlock>> map = new LinkedHashMap<>();
		for (Deadlock d : _cmDeadlocks)
			map.computeIfAbsent(d.getShapeKey(), k -> new ArrayList<>()).add(d);
		List<List<Deadlock>> shapes = new ArrayList<>(map.values());
		shapes.sort((a, b) -> Integer.compare(b.size(), a.size()));
		return shapes;
	}

	/** "procA / procB", or the start of the statement for ad hoc SQL */
	private static String who(Deadlock d)
	{
		Set<String> set = new LinkedHashSet<>();
		for (DlProcess p : d.processes)
		{
			if (p.procName != null)
				set.add(p.procName);
			else if (p.getStatement() != null)
				set.add(StringUtil.truncate(p.getStatement().replaceAll("\\s+", " ").trim(), 80, true, null));
		}
		return String.join(" / ", set);
	}

	private static String esc(String str)
	{
		return str == null ? "" : StringEscapeUtils.escapeHtml4(str);
	}

	private void writeCmDeadlocks(Writer w)
	throws IOException
	{
		List<List<Deadlock>> shapes = getShapes();

		w.append("<p>Deadlock details from '" + CmDeadlocks.CM_NAME + "': <b>" + _cmDeadlocks.size() + "</b> deadlocks, <b>" + shapes.size() + "</b> different kinds "
				+ "(same objects and procedures/statements = same kind).</p>\n");

		// Summary per kind
		StringBuilder sb = new StringBuilder();
		sb.append("<table class='sortable'>\n");
		sb.append("<thead><tr><th>Count</th><th>First</th><th>Last</th><th>Database</th><th>Objects</th><th>Locks</th><th>Isolation</th><th>Victims (app / login)</th><th>Procedures / Statements</th></tr></thead>\n<tbody>\n");
		for (int i = 0; i < shapes.size() && i < MAX_SHAPES_IN_TABLE; i++)
		{
			List<Deadlock> shape = shapes.get(i);
			Deadlock first = shape.get(0);
			Deadlock last  = shape.get(shape.size() - 1);

			Set<String> victims = new LinkedHashSet<>();
			for (Deadlock d : shape)
				if (d.getVictim() != null)
					victims.add(StringUtil.hasValue(d.getVictim().app) ? d.getVictim().app + " / " + d.getVictim().login : String.valueOf(d.getVictim().login));

			sb.append("<tr>")
			  .append("<td align='right'>").append(shape.size()).append("</td>")
			  .append("<td>").append(first.eventTime).append("</td>")
			  .append("<td>").append(last .eventTime).append("</td>")
			  .append("<td>").append(esc(last.getVictimDbName())).append("</td>")
			  .append("<td>").append(esc(last.getObjects())).append("</td>")
			  .append("<td>").append(esc(last.getLockTypes())).append("</td>")
			  .append("<td>").append(esc(last.getIsolationLevels())).append("</td>")
			  .append("<td>").append(esc(String.join(", ", victims))).append("</td>")
			  .append("<td>").append(esc(who(last))).append("</td>")
			  .append("</tr>\n");
		}
		sb.append("</tbody></table>\n");
		if (shapes.size() > MAX_SHAPES_IN_TABLE)
			sb.append("<p>... and ").append(shapes.size() - MAX_SHAPES_IN_TABLE).append(" more kinds.</p>\n");
		w.append(sb.toString());

		// Timeline pictures (the newest deadlock of each kind)
		sb = new StringBuilder();
		for (int i = 0; i < shapes.size() && i < MAX_PICTURES; i++)
		{
			List<Deadlock> shape = shapes.get(i);
			sb.append(SqlServerDeadlockTimelinePainter.toHtmlImg(shape.get(shape.size() - 1), shape.size())).append("<br>\n");
			String lookupSql = shape.get(shape.size() - 1).getWaitResourceSql();
			if (lookupSql != null)
				sb.append("<b>SQL to look up what the sessions waited for</b> (as it is now):\n<pre style='font-size:11px; margin:2px 0 0 0;'>").append(esc(lookupSql)).append("</pre>\n");
			sb.append("<br>\n");
		}
		String picLabel = "<b>Deadlock timelines</b> (the newest deadlock of the " + Math.min(shapes.size(), MAX_PICTURES) + " most common kinds)";
		w.append(createShowHideDiv("DeadlockTimelines", true, picLabel, sb.toString()));

		// Each deadlock (newest first), with the XML as a download (.xdl opens in SSMS)
		sb = new StringBuilder();
		sb.append("<table class='sortable'>\n");
		sb.append("<thead><tr><th>Time</th><th>Database</th><th>Victim</th><th>Objects</th><th>Victim statement</th><th>Other statements</th><th>Wait ms</th><th>XML</th></tr></thead>\n<tbody>\n");
		String dbxCentralBaseUrl = getReportingInstance().getDbxCentralPublicBaseUrl();
		int rows = 0;
		for (int i = _cmDeadlocks.size() - 1; i >= 0 && rows < MAX_DETAIL_ROWS; i--, rows++)
		{
			Deadlock d = _cmDeadlocks.get(i);
			DlProcess v = d.getVictim();
			String fileName = "deadlock_" + String.valueOf(d.eventTime).replaceAll("[^0-9]", "") + ".xdl";
			String xdlId    = "dsr-deadlock-xdl-" + rows;
			String xdlLink  = "<a id='" + xdlId + "' download='" + fileName + "' href='data:application/xml;base64," + Base64.getEncoder().encodeToString(d.xml.getBytes(StandardCharsets.UTF_8)) + "'>.xdl</a>";

			// Opens the deadlock in DbxCentral's Deadlock Viewer (picture, Play, lookup SQL): dsrOpenLink() POSTs the XML of the .xdl link above
			if (StringUtil.hasValue(dbxCentralBaseUrl))
				xdlLink += "<br><a href='javascript:void(0)' class='dsr-link' data-kind='deadlock' data-path='" + esc(dbxCentralBaseUrl + "/deadlock/sqlserver") + "'"
					+ " data-xdl-id='" + xdlId + "' data-event-time='" + (d.eventTime == null ? "" : d.eventTime) + "' onclick='dsrOpenLink(this); return false;'"
					+ " title='Opens the deadlock in the DbxCentral Deadlock Viewer (timeline picture, step by step, SQL to look up the wait resources). Requires JavaScript, so it does not work in an e-mail client.'>view</a>";

			String victimStmt = v == null ? "" : (v.getProcNameAndLine() == null ? "" : "-- " + v.getProcNameAndLine() + "\n") + (v.getStatement() == null ? "" : v.getStatement());

			sb.append("<tr>")
			  .append("<td>").append(d.eventTime).append("</td>")
			  .append("<td>").append(esc(d.getVictimDbName())).append("</td>")
			  .append("<td>").append(v == null ? "" : "spid " + v.spid + " " + esc(v.app) + " " + esc(v.login)).append("</td>")
			  .append("<td>").append(esc(d.getObjects())).append("</td>")
			  .append("<td><pre class='dbx-sql-text-cell'>").append(esc(StringUtil.truncate(victimStmt, 1000, true, null))).append("</pre></td>")
			  .append("<td><pre class='dbx-sql-text-cell'>").append(esc(StringUtil.truncate(d.getOtherStatements(), 1000, true, null))).append("</pre></td>")
			  .append("<td align='right'>").append(d.getMaxWaitTimeMs()).append("</td>")
			  .append("<td>").append(xdlLink).append("</td>")
			  .append("</tr>\n");
		}
		sb.append("</tbody></table>\n");
		String label = "<b>Each deadlock</b> (newest first" + (_cmDeadlocks.size() > MAX_DETAIL_ROWS ? ", the latest " + MAX_DETAIL_ROWS + " of " + _cmDeadlocks.size() : ", " + _cmDeadlocks.size() + " rows") + "). Save the .xdl link to open the deadlock graph in SSMS, or 'view' it in DbxCentral.";
		w.append( msOutlookAlternateText(createShowHideDiv("DeadlockDetails", false, label, sb.toString()), "DeadlockDetails", null) );
	}
}
