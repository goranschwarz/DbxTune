/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.DbUtils;
import com.dbxtune.utils.StringUtil;

public class ExecutionPlanCollection
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

//	private Map<String, String> _planMap;
	private Map<PlanKey, String> _planMap;
	private SqlServerAbstract   _reportEntry;
	private String              _id;
	private ResultSetTableModel _rstm;

	/**
	 * Class to act as a KEY for the _planMap
	 */
	protected static class PlanKey
	{
		String _dbname;
		String _planId;
		
		public String getDbname() { return _dbname; }
		public String getPlanId() { return _planId; }

		public PlanKey(String dbname, String planId)
		{
			_dbname = dbname;
			_planId = planId;
		}

		@Override
		public int hashCode()
		{
			final int prime  = 31;
			int       result = 1;
			result = prime * result + ((_dbname == null) ? 0 : _dbname.hashCode());
			result = prime * result + ((_planId == null) ? 0 : _planId.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if ( this == obj )
				return true;
			if ( obj == null )
				return false;
			if ( getClass() != obj.getClass() )
				return false;
			PlanKey other = (PlanKey) obj;
			if ( _dbname == null )
			{
				if ( other._dbname != null )
					return false;
			}
			else if ( !_dbname.equals(other._dbname) )
				return false;
			if ( _planId == null )
			{
				if ( other._planId != null )
					return false;
			}
			else if ( !_planId.equals(other._planId) )
				return false;
			return true;
		}
	}

	/**
	 * 
	 * @param reportEntry       Report entry...
	 * @param rstm              ResultSet TableModel to get info from.
	 * @param id                ID String used as a prefix or suffix in either HTML id's or JavaScripts variable names.
	 *                          if id is "empty" use rstm.getName(), if that is empty use simpleClassName of the "reportEntry"
	 */
	public ExecutionPlanCollection(SqlServerAbstract reportEntry, ResultSetTableModel rstm, String id)
	{
		_reportEntry = reportEntry;
		_rstm        = rstm;

		// set the "id"...
		_id = id;
		if (StringUtil.isNullOrBlank(_id))
			_id = rstm.getName();
		if (StringUtil.isNullOrBlank(_id))
			_reportEntry.getClass().getSimpleName();
	}

	/**
	 * Get ID string that uniquely identifies HTML tag id's and JavaScript variables
	 * @return
	 */
	public String getId()
	{
		return _id;
	}

	/**
	 * Get and return a Set of <i>identifiers</i> that we want to get XML ShowpPlan text for
	 * 
	 * @param rstm
	 * @param colName
	 * @return
	 */
//	public Set<String> getPlanHandleObjects(ResultSetTableModel rstm, String colName)
//	{
//		Set<String> set = new LinkedHashSet<>();
//		
//		int pos_colName = rstm.findColumn(colName);
//		if (pos_colName != -1)
//		{
//			for (int r=0; r<rstm.getRowCount(); r++)
//			{
//				String name = rstm.getValueAsString(r, pos_colName);
////System.out.println("rstmName='"+rstm.getName()+"', colName='"+colName+"', pos="+pos_colName+", val=|"+name+"|.");
//				
//				if (name != null)
//				{
//					set.add(name);
//				}
//			}
//		}
//		
//		return set;
//	}
	public Set<PlanKey> getPlanHandleObjects(ResultSetTableModel rstm, String dbnameCol, String colName)
	{
		Set<PlanKey> set = new LinkedHashSet<>();
		
		int pos_colName = rstm.findColumn(colName);
		if (pos_colName != -1)
		{
			for (int r=0; r<rstm.getRowCount(); r++)
			{
				String name   = rstm.getValueAsString(r, pos_colName);
				String dbname = dbnameCol == null ? null : rstm.getValueAsString(r, dbnameCol);
//System.out.println("rstmName='"+rstm.getName()+"', colName='"+colName+"', pos="+pos_colName+", val=|"+name+"|.");
				
				if (name != null)
				{
					PlanKey planKey = new PlanKey(dbname, name);
					set.add(planKey);
				}
			}
		}
		
		return set;
	}

//	/**
//	 * Get the XML Execution Plan from <i>somewhere</i>
//	 * <p>
//	 * Override this method to get it from <i>elsewhere</i> 
//	 * 
//	 * @param conn
//	 * @param nameSet
//	 * @return
//	 * @throws SQLException
//	 */
//	public Map<String, String> getShowplanAsMap(DbxConnection conn, Set<String> nameSet)
//	throws SQLException
//	{
//		Map<String, String> planMap = new LinkedHashMap<>();
//		
//		for (String name : nameSet)
//		{
//			String sql = ""
//				    + "select [objectName], [extraInfoText] as [SQLText] \n"
//				    + "from [MonDdlStorage] \n"
//				    + "where 1 = 1 \n"
//				    + "  and [dbname]     = 'statement_cache' \n"
////				    + "  and [owner]      = 'ssql' \n"
//				    + "  and [objectName] = " + DbUtils.safeStr(name) + " \n"
//				    + "";
//			
//			sql = conn.quotifySqlString(sql);
//			try ( Statement stmnt = conn.createStatement() )
//			{
//				// Unlimited execution time
//				stmnt.setQueryTimeout(0);
//				try ( ResultSet rs = stmnt.executeQuery(sql) )
//				{
//					while(rs.next())
//					{
//						String objectName    = rs.getString(1);
//						String extraInfoText = rs.getString(2);
//						
//						planMap.put(objectName, extraInfoText);
//					}
//				}
//			}
//			catch(SQLException ex)
//			{
//				//_problem = ex;
//
//				_logger.warn("Problems getting SQL Statement name = '"+name+"': " + ex);
//				throw ex;
//			} 
//		}
//		
//		return planMap;
//	}
	/**
	 * Get the XML Execution Plan from <i>somewhere</i>
	 * <p>
	 * Override this method to get it from <i>elsewhere</i> 
	 * 
	 * @param conn
	 * @param nameSet
	 * @return
	 * @throws SQLException
	 */
	public Map<PlanKey, String> getShowplanAsMap(DbxConnection conn, Set<PlanKey> nameSet)
	throws SQLException
	{
		Map<PlanKey, String> planMap = new LinkedHashMap<>();
		
		for (PlanKey planKey : nameSet)
		{
			String dbname = planKey.getDbname();
			String planId = planKey.getPlanId();
			
			String sql = ""
				    + "select [objectName], [extraInfoText] as [SQLText] \n"
				    + "from [MonDdlStorage] \n"
				    + "where 1 = 1 \n"
				    + "  and [dbname]     = 'statement_cache' \n"
//				    + "  and [owner]      = 'ssql' \n"
				    + "  and [objectName] = " + DbUtils.safeStr(planId) + " \n"
				    + "";
			
			sql = conn.quotifySqlString(sql);
			try ( Statement stmnt = conn.createStatement() )
			{
				// Unlimited execution time
				stmnt.setQueryTimeout(0);
				try ( ResultSet rs = stmnt.executeQuery(sql) )
				{
					while(rs.next())
					{
						String objectName    = rs.getString(1);
						String extraInfoText = rs.getString(2);
						
						planMap.put(new PlanKey(dbname, objectName), extraInfoText);
					}
				}
			}
			catch(SQLException ex)
			{
				//_problem = ex;

				_logger.warn("Problems getting SQL Statement name = '"+planId+"': " + ex);
				throw ex;
			} 
		}
		
		return planMap;
	}

	protected void addShowplanMap(Map<PlanKey, String> map)
	{
		if (_planMap == null)
			_planMap = new LinkedHashMap<>();
		
		_planMap.putAll(map);
	}

	protected Map<PlanKey, String> getShowplanAsMap()
	{
		return _planMap != null ? _planMap : Collections.emptyMap();
	}

	/**
	 * Render what the <b>visible</b> content should be in the anchor text.
	 * <p>
	 * Default is the same value as the passed, but surrounded with &lt;code&gt;theValue&lt;/code&gt;
	 * 
	 * @param val
	 * @return
	 */
	public Object renderCellView(Object val)
	{
		return "<code>" + val + "</code>";
	}

//	/**
//	 * 
//	 * @param conn         Connection to the PCS database
//	 * @param colName      Column name from where to get source data from and where to put the link.<br>
//	 *                     If you want the link in a another cell/column, then use the method with two column parameters
//	 */
//	public void getPlansAndSubstituteWithLinks(DbxConnection conn, String dbnameColName, String colName)
//	{
//		getPlansAndSubstituteWithLinks(conn, dbnameColName, colName, colName, null, null);
//	}


//	/**
//	 * 
//	 * @param conn            Connection to the PCS database
//	 * @param srcColName      Column name from where to get source data from 
//	 * @param destColName     Column name where to put a link to the Execution Plan 
//	 * @param planStr         Substitute cell content with a html link, use this value as the text. (if null/"" use renderer method <code>renderCellView(planId)</code>. )
//	 * @param noPlanStr       When plan wasn't found, replace with this string (if null/"" do not substitute cell content)
//	 */
//	public void getPlansAndSubstituteWithLinks(DbxConnection conn, String srcColName, String destColName, String planStr, String noPlanStr)
//	{
//		// Get ShowPlan for StatementCache entries, and SqlText from the above table
//		Set<String> planHandleObjects = getPlanHandleObjects(_rstm, srcColName);
//		
//		if (planHandleObjects != null && ! planHandleObjects.isEmpty() )
//		{
//			try 
//			{
//				// Get all QueryPlans as a Map<name, xml>
//				_planMap = getShowplanAsMap(conn, planHandleObjects);
//				
//				// Write back the "name" as a "link" in the RSTM
//				int pos_srcColName  = _rstm.findColumn(srcColName);
//				int pos_destColName = _rstm.findColumn(destColName);
//
//				if (pos_srcColName != -1 && pos_destColName != -1)
//				{
//					for (int r=0; r<_rstm.getRowCount(); r++)
//					{
//						String planId = _rstm.getValueAsString(r, pos_srcColName);
//						planId = (planId == null) ? "" : planId.trim();
//
//						if (_planMap.containsKey(planId))
//						{
//							Object linkText = renderCellView(planId);
//							if (StringUtil.hasValue(planStr))
//								linkText = planStr;
//
//							String htmlLink = "<a href='javascript:void(0)' title='Copy plan to clipboard and show in browser' onclick='showplanForId_" + getId() + "(\"" + planId + "\"); return false;'>" + linkText + "</a>";
//							_rstm.setValueAtWithOverride(htmlLink, r, pos_destColName);
//						}
//						else if (StringUtil.hasValue(noPlanStr))
//						{
//							_rstm.setValueAtWithOverride(noPlanStr, r, pos_destColName);
//						}
//					}
//				}
//				
//			}
//			catch (SQLException ex)
//			{
//				_reportEntry.setProblemException(ex);
//			}
//		}
//	}
	/**
	 * 
	 * @param conn            Connection to the PCS database
	 * @param srcColName      Column name from where to get source data from 
	 * @param destColName     Column name where to put a link to the Execution Plan 
	 * @param planStr         Substitute cell content with a html link, use this value as the text. (if null/"" use renderer method <code>renderCellView(planId)</code>. )
	 * @param noPlanStr       When plan wasn't found, replace with this string (if null/"" do not substitute cell content)
	 */
	public void getPlansAndSubstituteWithLinks(DbxConnection conn, String srcColDbname, String srcColName, String destColName, String planStr, String noPlanStr)
	{
		// Get ShowPlan for StatementCache entries, and SqlText from the above table
		Set<PlanKey> planHandleObjects = getPlanHandleObjects(_rstm, srcColDbname, srcColName);
		
		if (planHandleObjects != null && ! planHandleObjects.isEmpty() )
		{
			try 
			{
				// Get all QueryPlans as a Map<name, xml>
				_planMap = getShowplanAsMap(conn, planHandleObjects);

				substituteWithLinks(srcColDbname, srcColName, destColName, planStr, noPlanStr);
			}
			catch (SQLException ex)
			{
				_reportEntry.setProblemException(ex);
			}
		}
	}

	/**
	 * get plans by 'srcColName' (plan handle) from the PCS and store them in a Map for later usage
	 * 
	 * @param conn            Connection to the PCS database
	 * @param srcColName      Column name from where to get source data (plan handle) from 
	 */
	public void getPlans(DbxConnection conn, String srcColDbname, String srcColName)
	{
		// Get ShowPlan for StatementCache entries, and SqlText from the above table
		Set<PlanKey> planHandleObjects = getPlanHandleObjects(_rstm, srcColDbname, srcColName);
		
		if (planHandleObjects != null && ! planHandleObjects.isEmpty() )
		{
			try 
			{
				// Get all QueryPlans as a Map<name, xml>
				_planMap = getShowplanAsMap(conn, planHandleObjects);
			}
			catch (SQLException ex)
			{
				_reportEntry.setProblemException(ex);
			}
		}
	}
	
	/**
	 * Cleanup the map, remove entries that are NOT in the table.
	 * 
	 * @param srcColName      Column name from where to get source data (plan handle) from 
	 */
	public void cleanupMap(String srcColDbname, String srcColName)
	{
		// Get ShowPlan for StatementCache entries, and SqlText from the above table
		Set<PlanKey> planHandleObjects = getPlanHandleObjects(_rstm, srcColDbname, srcColName);
		
		if (planHandleObjects != null && ! planHandleObjects.isEmpty() )
		{
			_planMap.keySet().retainAll(planHandleObjects);
		}
	}
	
	/**
	 * 
	 * @param colName      Column name from where to get source data from and where to put the link.<br>
	 *                     If you want the link in a another cell/column, then use the method with two column parameters
	 */
	public void substituteWithLinks(String dbnameColName, String colName)
	{
		substituteWithLinks(dbnameColName, colName, colName, null, null);
	}

	/**
	 * 
	 * @param srcColName      Column name from where to get source data from 
	 * @param destColName     Column name where to put a link to the Execution Plan 
	 * @param planStr         Substitute cell content with a html link, use this value as the text. (if null/"" use renderer method <code>renderCellView(planId)</code>. )
	 * @param noPlanStr       When plan wasn't found, replace with this string (if null/"" do not substitute cell content)
	 */
	public void substituteWithLinks(String srcColDbname, String srcColName, String destColName, String planStr, String noPlanStr)
	{
		if (_planMap == null)   return;
//		if (_planMap.isEmpty()) return;

		// Write back the "name" as a "link" in the RSTM
		int pos_srcColName  = _rstm.findColumn(srcColName);
		int pos_destColName = _rstm.findColumn(destColName);

		if (pos_srcColName != -1 && pos_destColName != -1)
		{
			for (int r=0; r<_rstm.getRowCount(); r++)
			{
				String dbname = srcColDbname == null ? null : _rstm.getValueAsString(r, srcColDbname);
				String planId = _rstm.getValueAsString(r, pos_srcColName);
				planId = (planId == null) ? "" : planId.trim();

				PlanKey pk = new PlanKey(dbname, planId);
				
				if (_planMap.containsKey(pk))
				{
					Object linkText = renderCellView(planId);
					if (StringUtil.hasValue(planStr))
						linkText = planStr;

					String htmlLink = "<a href='javascript:void(0)' title='Copy plan to clipboard and show in browser' onclick='showplanForId_" + getId() + "(\"" + planId + "\"); return false;'>" + linkText + "</a>";
					_rstm.setValueAtWithOverride(htmlLink, r, pos_destColName);
				}
				else if (StringUtil.hasValue(noPlanStr))
				{
					_rstm.setValueAtWithOverride(noPlanStr, r, pos_destColName);
				}
			}
		}
	}

	public String getLinkText(String dbname, String planId, String planStr, String noPlanStr)
	{
		if (_planMap == null)   return null;
//		if (_planMap.isEmpty()) return null;

		PlanKey pk = new PlanKey(dbname, planId);
		
		if (_planMap.containsKey(pk))
		{
			Object linkText = renderCellView(planId);
			if (StringUtil.hasValue(planStr))
				linkText = planStr;

			return "<a href='javascript:void(0)' title='Copy plan to clipboard and show in browser' onclick='showplanForId_" + getId() + "(\"" + planId + "\"); return false;'>" + linkText + "</a>";
		}
		else
		{
			return noPlanStr;
		}
	}


	/**
	 * Write content (both HTML and JavaScript)
	 * 
	 * @param w
	 * @throws IOException
	 */
	public void writeMessageText(Writer w)
	throws IOException
	{
		if (_planMap != null && !_planMap.isEmpty())
		{
			w.append("\n");
			w.append("<!-- read Javascript and CSS for Showplan --> \n");
			w.append("<link rel='stylesheet' type='text/css' href='http://www.dbxtune.com/sqlserver_showplan/css/qp.css'> \n");
			w.append("<script src='http://www.dbxtune.com/sqlserver_showplan/dist/qp.js' type='text/javascript'></script> \n");
			w.append("\n");
//			
//			w.append("<br> \n");
//			w.append("<div id='showplan-list-" + getName() + "'> \n");
//			w.append("<b>Display the execution plan for any of the following <code>plan_handle</code>: </b> \n");
//			w.append("<ul> \n");
//			for (String planHandle : _planMap.keySet())
//			{
//				w.append("    <li> <a href='#showplan-list' title='Copy plan to clipboard and show in browser' onclick='showplanForId(\"").append(planHandle).append("\"); return true;'><code>").append(planHandle).append("</code></a> </li> \n");
//			}
//			w.append("</ul> \n");
//			w.append("</div> \n");
//			w.append(" \n");
			
			String showplanHead_innerHtml = "\"" // Start JavaScript double quote
					+ "<br>"
					+ "<hr>"
					+ "<a href='javascript:void(0)' onclick='hideShowplan_" + getId() + "();'>Hide Below Plan</a><br>"
					+ "Below is Execution plan for <code>plan_handle: \" + id + \"</code> <br>"
					+ "Note: You can also view your plan at <a href='http://www.supratimas.com' target='_blank'>http://www.supratimas.com</a>"
					+ ", or any other <i>plan-view</i> application by pasting (Ctrl-V) the clipboard content. <br>"
					+ "SentryOne Plan Explorer can be downloaded here: <a href='https://www.sentryone.com/plan-explorer' target='_blank'>https://www.sentryone.com/plan-explorer</a> <br>"
					+ "<a href='javascript:void(0)' onclick='copyShowplanToClipboardForId_" + getId() + "();'>Copy Below Plan as XML to Clipboard</a> <br>"
					+ "<a href='javascript:void(0)' onclick='downloadShowplanForId_" + getId() + "();'>Download Below Plan...</a> <br>"
					+ "<br>"
					+ "\""; // End JavaScript double quote
			
			w.append("<div id='showplan-head-" + getId() + "'></div> \n");
			w.append("<div id='showplan-container-" + getId() + "'></div> \n");
			w.append("<script type='text/javascript'> \n");
			w.append("    // Remember last plan in var... \n");
			w.append("    var last_showplanForId_" + getId() + "_id = ''; \n"); // Variable ON ID
			w.append("    var last_showplanForId_" + getId() + " = ''; \n");    // Variable ON ID
			w.append("    function showplanForId_" + getId() + "(id) \n");      // Function ON ID
			w.append("    { \n");
			w.append("        last_showplanForId_" + getId() + "_id = id;");
			w.append("        var showplanText = document.getElementById('plan_" + getId() + "_'+id).innerHTML; \n");
			
			w.append("        if (showplanText === last_showplanForId_" + getId() + ") \n");
			w.append("        { \n");
			w.append("            hideShowplan_" + getId() + "(); \n");
			w.append("            return; \n");
			w.append("        } \n");
			
			
			w.append("        last_showplanForId_" + getId() + " = showplanText; \n");
			w.append("        QP.showPlan(document.getElementById('showplan-container-" + getId() + "'), showplanText); \n");
			w.append("        document.getElementById('showplan-head-" + getId() + "').innerHTML = " + showplanHead_innerHtml + "; \n");
			w.append("        copyStringToClipboard(showplanText); \n");
			w.append("    } \n");
			w.append("    function hideShowplan_" + getId() + "() \n"); // Function ON ID
			w.append("    { \n");
			w.append("        last_showplanForId_" + getId() + " = 'none'; \n");
			w.append("        document.getElementById('showplan-head-"      + getId() + "').innerHTML = ''; \n");
			w.append("        document.getElementById('showplan-container-" + getId() + "').innerHTML = ''; \n");
			w.append("    } \n");
			w.append("\n");
			w.append("    function copyStringToClipboard (string)                                       \n");
			w.append("    {                                                                             \n");
			w.append("        function handler (event)                                                  \n");
			w.append("        {                                                                         \n");
			w.append("            event.clipboardData.setData('text/plain', string);                    \n");
			w.append("            event.preventDefault();                                               \n");
			w.append("            document.removeEventListener('copy', handler, true);                  \n");
			w.append("        }                                                                         \n");
			w.append("                                                                                  \n");
			w.append("        document.addEventListener('copy', handler, true);                         \n");
			w.append("        document.execCommand('copy');                                             \n");
			w.append("    }                                                                             \n");
			w.append("    function copyShowplanToClipboardForId_" + getId() + "()                       \n"); // Function ON ID
			w.append("    {                                                                             \n");
			w.append("        copyStringToClipboard(last_showplanForId_" + getId() + ");                \n");
			w.append("                                                                                  \n");
			w.append("        // Open a popup... and close it 3 seconds later...                        \n");
			w.append("        $('#copyPastePopup').modal('show');                                       \n");
			w.append("            setTimeout(function() {                                               \n");
			w.append("            $('#copyPastePopup').modal('hide');                                   \n");
			w.append("        }, 3000);		                                                            \n");
			w.append("    }                                                                             \n"); 
			w.append("    function downloadShowplanForId_" + getId() + "()                              \n"); // Function ON ID
			w.append("    {                                                                             \n");
			w.append("        var textToSave    = last_showplanForId_" + getId() + ";                   \n");
			w.append("        var hiddenElement = document.createElement('a');                          \n");
			w.append("        var showplanId    = last_showplanForId_" + getId() + "_id;                \n");
			w.append("        var filename      = 'showplan_" + getId() + "_' + showplanId + '.xml';    \n");
			w.append("                                                                                  \n");
			w.append("        hiddenElement.href     = 'data:attachment/text,' + encodeURI(textToSave); \n");
			w.append("        hiddenElement.target   = '_blank';                                        \n");
			w.append("        hiddenElement.download = filename                                         \n");
			w.append("        hiddenElement.click();                                                    \n");
			w.append("    }                                                                             \n"); 
			w.append("</script> \n");
			
			// HTML Code for the bootstrap popup...
			w.append("    <div class='modal fade' id='copyPastePopup'>                              \n");
			w.append("        <div class='modal-dialog'>                                            \n");
			w.append("            <div class='modal-content'>                                       \n");
			w.append("                <div class='modal-header'>                                    \n");
			w.append("                    <h4 class='modal-title'>Auto Close in 3 seconds</h4>      \n");
			w.append("                </div>                                                        \n");
			w.append("                <div class='modal-body'>                                      \n");
			w.append("                    <p>The XML Plan was copied to Clipboard</p>               \n");
//			w.append("                    <p>To see the GUI Plan, for example: Past it into SQL Window (sqlw)<br> \n");
//			w.append("                       SQL Window is included in the DbxTune package.         \n");
			w.append("                    </p>                                                      \n");
			w.append("                </div>                                                        \n");
			w.append("            </div>                                                            \n");
			w.append("        </div>                                                                \n");
			w.append("    </div>                                                                    \n");

//			for (String planHandle : _planMap.keySet())
//			{
//				String xmlPlan = _planMap.get(planHandle);
//
//				w.append("\n<script id='plan_" + getId() + "_").append(planHandle).append("' type='text/xmldata'>\n");
//				w.append(xmlPlan);
//				w.append("\n</script>\n");
//			}
			for (PlanKey pk : _planMap.keySet())
			{
				String xmlPlan = _planMap.get(pk);

				w.append("\n<script id='plan_" + getId() + "_").append( pk.getPlanId() ).append("' type='text/xmldata'>\n");
				w.append(xmlPlan);
				w.append("\n</script>\n");
			}
		}
	}
}
