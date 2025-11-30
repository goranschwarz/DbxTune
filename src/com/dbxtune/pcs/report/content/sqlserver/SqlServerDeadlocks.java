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
package com.dbxtune.pcs.report.content.sqlserver;

import java.io.IOException;
import java.io.Writer;
import java.lang.invoke.MethodHandles;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.CounterControllerSqlServer;
import com.dbxtune.cm.sqlserver.CmPerfCounters;
import com.dbxtune.cm.sqlserver.CmSummary;
import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.PersistWriterJdbc;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.pcs.report.content.IReportChart;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;
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

			// sp_BlitzLock info
			if (StringUtil.hasValue(_deadlockSummaryPeriodReport_spBlitzLock))
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
				w.append("<p><b>Please considder installing 'sp_BlitzLock' on the server '" + getReportingInstance().getDbmsServerName() + "', this will give more detailed information about the deadlocks in here.</b></p>");
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
		return "Deadlock Information for the full day (origin: CmSummary,CmPerfCounters,sp_BlitzLock)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return _deadlockSummaryPeriodCount > 0;
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
}
