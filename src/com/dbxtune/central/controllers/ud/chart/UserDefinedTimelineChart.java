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
package com.dbxtune.central.controllers.ud.chart;

import java.lang.invoke.MethodHandles;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.central.controllers.DbxTimelineRows;
import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.TimeUtils;

public class UserDefinedTimelineChart
extends UserDefinedChartAbstract
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public static final String PROPKEY_startTime = "startTime.default";
	public static final String DEFAULT_startTime = "-2";
	
	private String    _defaultStartTime = DEFAULT_startTime;
	private Timestamp _startTime;
	private Timestamp _endTime;

	public UserDefinedTimelineChart(Configuration conf)
	throws Exception
	{
		super(conf);
	}

	@Override
	public void init(Configuration conf)
	throws Exception
	{
		super.init(conf);
		
		// Do local initializations here
		_defaultStartTime = conf.getProperty(PROPKEY_startTime, DEFAULT_startTime);
	}

	@Override
	public String[] getKnownParameters()
	{
		return new String[] {"startTime", "endTime", "showKeys", "keyTransform", "minDurationInSeconds", "keepNames", "skipNames", "fillEnd", "generateDummyRows", "useDefaultTooltip", "onlyLevelZero", "filter"};
	}

	@Override
	public Map<String, String> getParameterDescription()
	{
		LinkedHashMap<String, String> map = new LinkedHashMap<>();
		
		map.put("refresh",              "Auto refresh the page after this amount of seconds.<br>"
		                                    + "<br>"
		                                    + "Example: <code>60</code> (every 60 seconds) <br>"
		                                    + "Example: <code>0 </code> (turn OFF auto refresh, just press F5 instead) <br>"
		                                    + "<b>Default</b>: <code>" +  getRefresh() + "</code>");

		map.put("startTime",            "What is the 'startTime' we want to get data for. <br>"
		                                    + "<br>"
		                                    + "Example: <code>TODAY           </code> (set StartTime to this day at 00:00:00) <br>"
		                                    + "Example: <code>WEEK            </code> (set StartTime to this week, Monday at 00:00:00) <br>"
		                                    + "Example: <code>-4h             </code> (set StartTime to 'now' -4 hours) <br>"
		                                    + "Example: <code>-2d             </code> (set StartTime to 'now' -2 days) <br>"
		                                    + "Example: <code>-1w             </code> (set StartTime to 'now' -1 week) <br>"
		                                    + "Example: <code>-3m             </code> (set StartTime to 'now' -3 months) <br>"
		                                    + "Example: <code>2024-03-08 18:00</code> (set startTime to a absolute timestamp) <br>"
		                                    + "Format: <code>[-]#{h|d|w|m}</code> where '-' is optional, # is a number, h=Hours, d=Days, w=Weeks, m=Months <br>"
		                                    + "<b>Default</b>: <code>-2       </code> (last 2 hours)");

		map.put("endTime",              "What is the end time <br>"
		                                    + "<br>"
		                                    + "Example: <code>2024-03-08 22:00</code> (set endTime to a absolute timestamp) <br>"
		                                    + "Example: <code>4h              </code> (set endTime to 4 hours after the 'startTime') <br>"
		                                    + "Example: <code>1d              </code> (set endTime to 1 day after the 'startTime', also: <code>w</code>=Weeks, <code>m</code>=Months) <br>"
		                                    + "Example: <code>NOW             </code> (set endTime to current time) <br>"
		                                    + "<b>Default</b>: <i>now</i><br>");

		map.put("showKeys",             "Show the chart 'keys' at the left side of the chart<br>"
		                                    + "A chart key is what groups the various rows together, also see <code>keyTransform</code><br>"
		                                    + "<br>"
		                                    + "<b>Default</b>: <code>false</code>");

		map.put("keyTransform",         "If we want to 'flatten' the chart in some way, we can use this to replace some part of the key (This is a regular expression)<br>"
		                                    + "This can be used if you have a 'parent' job, which schedules 'sub jobs', and you have 'sub' jobs on individual rows...<br>"
		                                    + "But, you want to 'collapse' the 'sub' records into the 'same' row as the 'parent', then you can use this to 'rewrite' the 'key' to be the same for all records that should be presented on the same row in the chart.<br>"
		                                    + "<br>"
		                                    + "Example: <code>\\[\\d+\\]</code> (remove any any strings that has numbers within square brackets, for example: <code>[000]</code>) <br>"
		                                    + "<b>Note</b>:    The URL will be <code>&keyTransform=\\[\\d%2B\\]</code> where '%2B' is the '+' (plus char) in escaped form, for a HTTP Query String<br>"
		                                    + "<b>Hint</b>:    To test your regular expressions you can use: <a href='https://www.regexpal.com/' target='_blank'>https://www.regexpal.com/</a> <br>"
		                                    + "<b>Default</b>: <i>none</i> ");

		map.put("minDurationInSeconds", "If we want to 'skip' items in the chart which has a 'duration' of less than ## seconds<br>"
		                                    + "<b>Default</b>: <code>0</code> (if not restricted by the executed SQL Statement");

		map.put("keepNames",            "Only keep records in column 'BarText' that matches this regular expression <br>"
		                                    + "This can for example be used if you want to skip some 'sub' jobs, or other known jobs you do not want to see <br>"
		                                    + "<br>"
		                                    + "Example: <code>.*\\[0\\].*</code> (keep any rows that contains '[0]') <br>"
		                                    + "<b>Hint</b>:    To test your regular expressions you can use: <a href='https://www.regexpal.com/' target='_blank'>https://www.regexpal.com/</a> <br>"
		                                    + "<b>Default</b>: <i>none</i> ");

		map.put("skipNames",            "Remove records in column 'BarText' that matches this regular expression<br>"
		                                    + "Much like 'keepNames', but here we can say that all names with 'Maintenence' or similar will be skipped. <br>"
		                                    + "<br>"
		                                    + "Example: <code>.*Maintenance.*</code> (remove any rows that contains 'Maintenance') <br>"
		                                    + "<b>Hint</b>:    To test your regular expressions you can use: <a href='https://www.regexpal.com/' target='_blank'>https://www.regexpal.com/</a> <br>"
		                                    + "<b>Default</b>: <i>none</i> ");

		map.put("fillEnd",              "Fill the end of the chart with 'No Activity' label. Usefull if we are looking at the 'tail', so we can se time that has passed without any activity <br>"
		                                    + "<br>"
		                                    + "<b>Note</b>:    This will be disabled if the <code>endTime</code> is specified.<br>"
		                                    + "<b>Default</b>: <code>true</code>");

		map.put("generateDummyRows",    "Just for demo purposes. Generate some dummy records...<br>"
		                                    + "<br>"
		                                    + "Example: <code>15</code> (Generate 15 dummy rows, like 'Dummy row #') <br>"
		                                    + "Example: <code>20:aaa {id} bbb</code> (Generate 15 dummy rows, with the label 'aaa # bbb', where # is the row number) <br>"
		                                    + "<b>Default</b>: <i>none</i>");

		map.put("useDefaultTooltip",    "Fallback to use the components default tooltip instead of the 'enhanced' one.<br>"
		                                    + "<br>"
		                                    + "<b>Default</b>: <code>false</code>");

		map.put("filter",               "Initial text for the <i>Filter by name</i> box in the chart toolbar: only show rows where the label (or a nested row / bar text)<br>"
		                                    + "contains this text (case insensitive). The box can be changed on the page.<br>"
		                                    + "<b>Default</b>: <i>none</i>");

		map.put("onlyLevelZero",        "Only show Level Zero, this to get a high level overview of the executed work.<br>"
		                                    + "<br>"
		                                    + "If the SQL has a <code>parentKey</code> column, all rows are fetched and the nested rows start collapsed (click a row to expand it).<br>"
		                                    + "Otherwise only rows where 'BarText' starts with <code>[0] </code> are shown.<br>"
		                                    + "<b>Default</b>: <code>false</code>");

		return map;
	}

	@Override
	public String getUrl()
	{
		return "/api/udc"
				+ "?name="    + getName()
//				+ "&srvName=" + getDbmsServerName()
				+ ( getRefresh() <= 0 ? "" : "&refresh=" + getRefresh() )
				+ "&showKeys=false"
				+ "&onlyLevelZero=false"
				+ "&startTime="+_defaultStartTime;
	}

	@Override
	public String getDbmsSql()
	{
		String sql = super.getDbmsSql();
		
		// If the ${start/endTime} is surrounded with single quotes
		if (sql.contains("'${startTime}'")) sql = sql.replace("'${startTime}'", "'" + _startTime + "'");
		if (sql.contains("'${endTime}'"  )) sql = sql.replace("'${endTime}'"  , "'" + _endTime   + "'");

		// If the ${start/endTime} is NOT surrounded with single quotes
		if (sql.contains( "${startTime}" )) sql = sql.replace( "${startTime}" , "'" + _startTime + "'");
		if (sql.contains( "${endTime}"   )) sql = sql.replace( "${endTime}"   , "'" + _endTime   + "'");
		
		return sql;
	}
	
	@Override
	public void produce() 
	throws Exception
	{
		String info = createInfoContent();
		setInfoContent(info);

		String res = createTimeline();
		setContent(res);
	}

	@Override
	public List<String> getJavaScriptList()
	{
		List<String> list = new ArrayList<>();
		
		// Timeline chart: vis-timeline + dbxTimeline.js (replaced the Google Charts Timeline, which needs internet access)
		list.addAll(DbxTimelineRows.JAVASCRIPT_LIST);
		list.addAll(DbxTimelineRows.CSS_LIST);

		return list;
	}

	@Override
	public void checkUrlParameters(Map<String, String> parameterMap) throws Exception
	{
		String startTime = parameterMap.get("startTime");
		if (StringUtil.isNullOrBlank(startTime))
			startTime = _defaultStartTime;

		String endTime   = parameterMap.get("endTime");

		// startTime: TODAY, WEEK, [-]#{h|d|w|m} (back from now), # (hours) or a date with optional time
		// endTime:   NOW, #{h|d|w|m} (after the startTime), # (hours) or a date with optional time
		Timestamp startTs = TimeUtils.parseStartTime(startTime);
		Timestamp endTs   = startTs != null ? TimeUtils.parseEndTime(endTime, startTs) : null;


		//----------------------
		// SET: "startTime"
		if (startTs != null)
		{
			_startTime = startTs;
			parameterMap.put("startTime", _startTime + "");
		}

		//----------------------
		// SET: "endTime"
		if (endTs == null)
		{
			// set endTime to "now"
			_endTime = new Timestamp(System.currentTimeMillis());
		}
		else
		{
			_endTime = endTs;

			// If we have PASSED a "endTime", then do NOT fill out the end of the chart to "now"
			parameterMap.put("fillEnd", false + "");
		}
		parameterMap.put("endTime", _endTime + "");
	}


	private String createInfoContent()
	{
		StringBuilder sb = new StringBuilder();

		sb.append("<div id='servlet-params'> \n");
		sb.append("Known Parameters: <code>" + StringUtil.toCommaStr(getKnownParameters()) + "</code><br> \n");
		sb.append("Used Variables: <code>" + getUrlParameters() + "</code><br> \n");
		sb.append("Scroll to: \n");
		sb.append("<a href='#' onClick=\"scrollToTop('timeline')\">Top</a>  \n");
		sb.append(" or \n");
		sb.append("<a href='#' onClick=\"scrollToBottom('timeline')\">Bottom</a> \n");
		sb.append(" of the Timeline. &emsp; <input type='checkbox' id='autoscroll-to-bottom' onclick='changeAutoscroll()'> On load AutoScroll to bottom. \n");
		sb.append(" <a href='#' data-tooltip=\"" + StringEscapeUtils.escapeHtml4(getDbmsSql()) + "\" onClick='copyExecutedSql()'>Copy Executed SQL</a>\n");
//		sb.append("<br> \n");
		sb.append("</div> \n");
		sb.append("\n");

		sb.append("<div id='parameter-descriptions'> \n");
		sb.append("Parameter Description:<br> \n");
		sb.append(getParameterDescriptionHtmlTable());
		sb.append("<br> \n");
		sb.append("</div> \n");
		sb.append("\n");

		sb.append("<div id='resultset-description'> \n");
		sb.append("ResultSet columns: 1=labelKey, 2=barText, 3=barColor, 4=startTime, 5=endTime, all other columns are shown in the tooltip.<br> \n");
		sb.append("Optional column <code>parentKey</code>: the labelKey of the row this row is nested under (expand/collapse by clicking the parent row). \n");
		sb.append("</div> \n");
		sb.append("\n");

//		sb.append("<details open>");
		sb.append("<details>");
		sb.append("<summary>");
		sb.append("  <b>Executed SQL Statement:</b><br> \n");
		sb.append("</summary>");
//		sb.append("  <div id='executed_sql' style='display: none'> \n");
		sb.append("  <div id='executed_sql'> \n");
		sb.append("  <pre> \n");
		sb.append(StringEscapeUtils.escapeHtml4(getDbmsSql()));
		sb.append("  </pre> \n");
		sb.append("  </div> \n");
		sb.append("</details>");
		sb.append("<br> \n");
		sb.append("\n");

		return sb.toString();
	}

	private String createTimeline() 
	throws Exception
	{
		StringBuilder sb = new StringBuilder();
		String tmpParamStr;

		String sql = getDbmsSql();

		// get 'parameters'
		Map<String, String> urlParams = getUrlParameters();

		// >>> onlyLevelZero
		boolean onlyLevelZero = false;
		tmpParamStr  = urlParams.get("onlyLevelZero");
		if (tmpParamStr != null && tmpParamStr.equalsIgnoreCase("true"))
			onlyLevelZero = true;

		// >>> showKeys
		boolean showKeys = false;
		tmpParamStr  = urlParams.get("showKeys");
		if (tmpParamStr != null && tmpParamStr.equalsIgnoreCase("true"))
			showKeys = true;

		// >>> keySubstitute
		String keyTransformFrom = ""; // This is a regex
		String keyTransformTo   = ""; // the value to change
		tmpParamStr  = urlParams.get("keyTransform");
		if (tmpParamStr != null)
		{
			keyTransformFrom = StringUtils.substringBefore(tmpParamStr, "=");
			keyTransformTo   = StringUtils.substringAfter (tmpParamStr, "=");
		}
		
		// >>> minDurationInSeconds
		int minDurationInSeconds = -1;
		tmpParamStr  = urlParams.get("minDurationInSeconds");
		if (tmpParamStr != null)
			minDurationInSeconds = StringUtil.parseInt(tmpParamStr, -1);

		// >>> keepNames
		String keepNames = urlParams.get("keepNames");

		// >>> skipNames
		String skipNames = urlParams.get("skipNames");

		// >>> fillEnd
		boolean fillEnd = true;
		tmpParamStr  = urlParams.get("fillEnd");
		if (tmpParamStr != null && tmpParamStr.equalsIgnoreCase("false"))
			fillEnd = false;
		
		// >>> generateDummyRows
		int    generateDummyRows    = -1;
		String generateDummyRowsStr = "";
		tmpParamStr  = urlParams.get("generateDummyRows");
		if (tmpParamStr != null)
		{
			generateDummyRows    = StringUtil.parseInt(StringUtils.substringBefore(tmpParamStr, ":"), 50);
			generateDummyRowsStr =                     StringUtils.substringAfter (tmpParamStr, ":");
		}
		
		// >>> useDefaultTooltip
		boolean useDefaultTooltip = false;
		tmpParamStr  = urlParams.get("useDefaultTooltip");
		if (tmpParamStr != null && tmpParamStr.equalsIgnoreCase("true"))
			useDefaultTooltip = true;
		
		
		// Connect to DBMS - with AutoClose
		try ( DbxConnection conn = dbmsConnect() ) 
		{
			// Execute the SQL - with AutoClose
			try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql); )
			{
				ResultSetMetaData rsmd = rs.getMetaData();
				int colCount = rsmd.getColumnCount();
				if (colCount < 5)
				{
					throw new Exception("The ResultSet must have minimum 5 columns (it has " + colCount + "). 1=[labelKey:String], 2=[barText:String], 3=[barColor:String], 4=[startDate:Timestamp], 5=[endDate:Timestamp] all extra columns will go in 'tooltip'.");
				}

				int col4_datatype = rsmd.getColumnType(4);
				int col5_datatype = rsmd.getColumnType(5);

				if (col4_datatype != Types.TIMESTAMP) throw new Exception("The ResultSet for column 4 has to be of type TIMESTAMP, it was " + ResultSetTableModel.getColumnJavaSqlTypeName(col4_datatype) + ". Expected ResultSet: 1=[labelKey:String], 2=[barText:String], 3=[barColor:String], 4=[startDate:Timestamp], 5=[endDate:Timestamp]");
				if (col5_datatype != Types.TIMESTAMP) throw new Exception("The ResultSet for column 5 has to be of type TIMESTAMP, it was " + ResultSetTableModel.getColumnJavaSqlTypeName(col5_datatype) + ". Expected ResultSet: 1=[labelKey:String], 2=[barText:String], 3=[barColor:String], 4=[startDate:Timestamp], 5=[endDate:Timestamp]");

//				sb.append("<style type='text/css'> \n");
//			    sb.append("    #timeline { \n");
//			    sb.append("    } \n");
//				sb.append("</style> \n");

				// Try to make the padding/spacing on the rows a bit smaller (this did not work)
				// https://almende.github.io/chap-links-library/js/timeline/doc/#Styles
//				sb.append("<style type='text/css'> \n");
//			    sb.append("    div.timeline-axis-text { \n");
//			    sb.append("        padding: 33px; \n");
//			    sb.append("    } \n");
//				sb.append("</style> \n");

				// Below is grabbed from DbxTune Daily Summary Reports (and not implemented yet, just for the future)
				// To be used for: minimizing text "at top", and has that text as "tooltip popups" for 'SQL Text', 'Known Parameters' & 'Used Variables'
//				sb.append("<style type='text/css'> \n");
//				sb.append("        [data-tooltip] { \n");
//				sb.append("            position: relative; \n");
//				sb.append("        } \n");
//				sb.append(" \n");
//				sb.append("        [data-tooltip]:hover::before { \n");
//				sb.append("            content: attr(data-tooltip); \n");
//				sb.append("/*            content: 'Click to Open Text Dialog...'; */ \n");
//				sb.append("            position: absolute; \n");
//				sb.append("            z-index: 103; \n");
//				sb.append("            top: 20px; \n");
//				sb.append("            left: 30px; \n");
//				sb.append("/*            width: 1800px;					*/ \n");
//				sb.append("            width: 220px; \n");
//				sb.append("            padding: 10px; \n");
//				sb.append("            background: #454545; \n");
//				sb.append("            color: #fff; \n");
//				sb.append("            font-size: 11px; \n");
//				sb.append("            font-family: Courier; \n");
//				sb.append("            white-space: pre-wrap; \n");
//				sb.append("        } \n");
//				sb.append("        [data-title]:hover::after { \n");
//				sb.append("            content: ''; \n");
//				sb.append("            position: absolute; \n");
//				sb.append("            bottom: -12px; \n");
//				sb.append("            left: 8px; \n");
//				sb.append("            border: 8px solid transparent; \n");
//				sb.append("            border-bottom: 8px solid #000; \n");
//				sb.append("        } \n");
//				sb.append("</style> \n");

				sb.append("<div id='timeline' style='height: 85vh;'> \n");
				sb.append("</div> \n");
				sb.append("\n");

				// Optional column 'parentKey': nest this row under the row with that key
				int parentKeyCol = -1;
				for (int col = 6; col <= colCount; col++)
				{
					if ("parentKey".equalsIgnoreCase(rsmd.getColumnLabel(col)))
						parentKeyCol = col;
				}

				// All rows (bars) for DbxTimeline, see DbxTimelineRows and /scripts/dbxtune/js/dbxTimeline.js
				List<Map<String, Object>> rows = new ArrayList<>();

				Timestamp maxTs          = null;
				Timestamp prevRowStartTs = null;
				Timestamp prevRowEndTs   = null;

				SimpleDateFormat sdf     = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

				// Loop all rows in ResultSet
				while(rs.next())
				{
					String labelKey   = rs.getString   (1);
					String barText    = rs.getString   (2);
					String barColor   = rs.getString   (3);
					Timestamp startTs = rs.getTimestamp(4);
					Timestamp endTs   = rs.getTimestamp(5);

					// Columns above FIVE will be added as tooltip values (except 'parentKey')
					Map<String, String> extraColumns = null;
					if (colCount > 5)
					{
						extraColumns = new LinkedHashMap<>();
						for (int col = 6; col <= colCount; col++)
						{
							if (col == parentKeyCol)
								continue;

							String colName = rsmd.getColumnLabel(col);
							String colVal  = rs.getString(col);

							if (StringUtil.hasValue(colVal))
								extraColumns.put(colName, colVal);
						}
					}
					String parentKey = parentKeyCol > 0 ? rs.getString(parentKeyCol) : null;
					
					if (startTs == null)
						startTs = prevRowEndTs;

					// Adjust start time in some specific case 
					// If we have a extra column 'step_id', and this is above 1, AND the startTs is the same as previusRowStartTs then choose previousRowEndTs as startTime
					// This is a *special* case for SQL Server Job Scheduler...
					if (extraColumns != null)
					{
						 int stepId = StringUtil.parseInt(extraColumns.get("step_id"), -1);
						 if (stepId >= 2)
						 {
							 if (startTs != null && prevRowStartTs != null && prevRowEndTs != null && startTs.equals(prevRowStartTs))
							 {
								 startTs = prevRowEndTs;
							 }
						 }
					}
					
					// Check if endTs is BEFORE startTs ... Then the GUI component will behave "strange" 
					if (endTs != null && startTs != null)
					{
//						if (endTs.getTime() < startTs.getTime())
						if (endTs.before(startTs))
						{
							// just ADD 60 seconds to the startTs
							Timestamp newEndTs = new Timestamp(startTs.getTime() + 60 * 1000);
							
							_logger.warn("The 'endTs' is before 'startTs'... labelKey='" + labelKey + "', barText='" + barText + "', origin[startTs='" + sdf.format(startTs) + "', endTs='" + sdf.format(endTs) + "']. Adjusting 'endTs' to be 60 seconds AFTER startTs. newEndTs='" + sdf.format(newEndTs) + "'.");
							endTs = newEndTs;
						}
					}

					// Remember the previous row start/end Timestamps (if we need them at next row loop)
					prevRowStartTs = startTs;
					prevRowEndTs   = endTs;

					// If the "onlyLevelZero", the "barText" must start with "[0] "
					// But with a 'parentKey' column, all rows are sent (and the nested rows start collapsed)
					if (onlyLevelZero && parentKeyCol < 0)
					{
						if ( ! barText.startsWith("[0] ") )
							continue;
					}

					// If the "duration" is not long enough, skip this record!
					if (minDurationInSeconds != -1 && startTs != null && endTs != null)
					{
						long durationInMs = endTs.getTime() - startTs.getTime();
						if (durationInMs < minDurationInSeconds * 1000)
							continue;
					}

					// Should we "keep" this name ???
					if (StringUtil.hasValue(keepNames))
					{
						if ( ! barText.matches(keepNames) )
							continue;
					}

					// Should we "skip" this name ???
					if (StringUtil.hasValue(skipNames))
					{
						if ( barText.matches(skipNames) )
							continue;
					}
					
					if (StringUtil.isNullOrBlank(barColor))
						barColor = "green";

					if (endTs == null)
						endTs = new Timestamp(System.currentTimeMillis());

					// Change/transform the 'key' so we possibly can group "collapse" several rows (sub-tasks) on 1 row
					if (StringUtil.hasValue(keyTransformFrom))
					{
						labelKey = labelKey.replaceAll(keyTransformFrom, keyTransformTo);
						if (parentKey != null)
							parentKey = parentKey.replaceAll(keyTransformFrom, keyTransformTo);
					}

					String tooltip = createUserDefinedTooltip(useDefaultTooltip, labelKey, barText, startTs, endTs, extraColumns);

					Map<String, Object> row = DbxTimelineRows.createRow(labelKey, barText, barColor, tooltip, startTs, endTs);
					if (StringUtil.hasValue(parentKey))
						row.put("parentKey", parentKey);
					rows.add(row);

					// Remember MAX TS, used if we need to "fillEnd"
					if (maxTs == null)
						maxTs = endTs;
					else
						maxTs = endTs.getTime() > maxTs.getTime() ? endTs : maxTs;  // MAX value

				} // end: loop ResultSet

				// No records was found (or fillEnd in 'NO Activity' at the end)
				String noActivityLabel = "NO Activity";
				String noActivityColor = "gray"; //"#ffffcc";
				if (fillEnd)
				{
					if (maxTs == null)
					{
						// NO Activity -- FULL Period
						String tooltip = createUserDefinedTooltip(useDefaultTooltip, noActivityLabel, noActivityLabel, _startTime, _endTime, null);
						rows.add(DbxTimelineRows.createRow(noActivityLabel, noActivityLabel, noActivityColor, tooltip, _startTime, _endTime));
					}
					else
					{
						Timestamp startTs = maxTs;
						Timestamp endTs   = new Timestamp(System.currentTimeMillis());

						// Only write "end-filler" if it's more than 10 seconds
						long tsDiffMs = endTs.getTime() - startTs.getTime();
						if (tsDiffMs > 10_000)
						{
							// NO Activity -- AT THE END
							String tooltip = createUserDefinedTooltip(useDefaultTooltip, noActivityLabel, noActivityLabel, startTs, endTs, null);
							rows.add(DbxTimelineRows.createRow(noActivityLabel, noActivityLabel, noActivityColor, tooltip, startTs, endTs));
						}
					}
				}

				if (generateDummyRows > 0)
				{
					String dummyLabel = StringUtil.isNullOrBlank(generateDummyRowsStr) ? "Dummy row {id}" : generateDummyRowsStr + " ";
					String dummyColor = "yellow";
					int    dummyTime  = 60_000; // 60 seconds

					Timestamp startTs = null;
					Timestamp endTs   = null;

					for (int r = 1; r <= generateDummyRows; r++)
					{
						String tmpDummyLabel = dummyLabel.replace("{id}", Integer.toString(r));
						if (startTs == null)
						{
							startTs = _startTime;
							endTs   = new Timestamp(_startTime.getTime() + dummyTime);
						}
						else
						{
							startTs = endTs;
							endTs   = new Timestamp(startTs.getTime() + dummyTime);
						}

						String tooltip = createUserDefinedTooltip(useDefaultTooltip, dummyLabel, dummyLabel, startTs, endTs, null);
						rows.add(DbxTimelineRows.createRow(tmpDummyLabel, tmpDummyLabel, dummyColor, tooltip, startTs, endTs));
					}
				}

				sb.append("<script> \n");
				sb.append("    // All rows, see /scripts/dbxtune/js/dbxTimeline.js \n");
				sb.append("    const _dbxTimelineRows = " + DbxTimelineRows.toScriptJson(rows) + "; \n");
				sb.append("\n");
				sb.append("    const _dbxTimeline = DbxTimeline.create('timeline', _dbxTimelineRows, { \n");
				sb.append("        start:          " + DbxTimelineRows.toJsTs(_startTime) + ", \n");
				sb.append("        end:            " + DbxTimelineRows.toJsTs(_endTime)   + ", \n");
				sb.append("        startExpanded:  " + (!onlyLevelZero) + ", \n");
				sb.append("        filter:         " + DbxTimelineRows.toScriptJson(StringUtil.nullToValue(urlParams.get("filter"), "")) + ", \n");
				sb.append("        showKeys:       " + showKeys + ", \n");
				sb.append("        scrollToBottom: true \n");
				sb.append("    }); \n");
				sb.append("\n");
				sb.append("    function scrollToBottom(id) { _dbxTimeline.scrollToBottom(); } \n");
				sb.append("    function scrollToTop(id)    { _dbxTimeline.scrollToTop();    } \n");
				sb.append("\n");
				sb.append("    function changeAutoscroll() { \n");
				sb.append("       var div = document.getElementById('autoscroll-to-bottom'); \n");
				sb.append("       getStorage('dbxtune_checkboxes_').set('autoscroll-to-bottom', div.checked); \n");
				sb.append("    } \n");
				sb.append("\n");
				sb.append("    function copyExecutedSql() { \n");
				sb.append("       DbxTimeline.copyToClipboard(document.getElementById('executed_sql').textContent); \n");
				sb.append("    } \n");
				sb.append("\n");
				sb.append("    // Restore the 'autoscroll-to-bottom' checkbox (the chart is always scrolled to the bottom when loaded) \n");
				sb.append("    document.getElementById('autoscroll-to-bottom').checked = getStorage('dbxtune_checkboxes_').get('autoscroll-to-bottom'); \n");
				sb.append("</script> \n");
			}
		}
		catch (SQLException ex)
		{
			_logger.error("In '" + this.getClass().getSimpleName() + "'. Problems executing SQL Statement. ErrorCode=" + ex.getErrorCode() + ", SQLState=" + ex.getSQLState() + ", Message=|" + ex.getMessage() + "|, SQL=|" + sql + "|.");

			throw new SQLException("Problems executing SQL Statement. ErrorCode=" + ex.getErrorCode() + ", SQLState=" + ex.getSQLState() + ", Message=|" + ex.getMessage() + "|, SQL=|" + sql + "|.", ex);
		}
		
		return sb.toString();
	}

	private String createUserDefinedTooltip(boolean useDefaultTooltip, String label, String barText, Timestamp startTs, Timestamp endTs, Map<String, String> extraColumns)
	{
		if (useDefaultTooltip)
			return null;
		
		SimpleDateFormat ymd       = new SimpleDateFormat("yyyy-MM-dd");
		SimpleDateFormat hms       = new SimpleDateFormat("HH:mm:ss");
		SimpleDateFormat dayOfWeek = new SimpleDateFormat("EEEE");
		
		String tooltip = ""
				+ "<div style='padding:10px; white-space:nowrap; font-size:12px; font-family:Arial;'>"
				+ "<b>" + label + "</b>"
				+ "<hr>"
				+ "<b>" + barText + "</b>"
				+ "<hr>"
				+ "<table style='font-size:12px; font-family:Arial;'>" 
				+ "<tr> <td nowrap><b>Duration: </b></td> <td nowrap>" + calculateDuration(startTs, endTs) + "</td> </tr>"
				+ "<tr> <td nowrap><b>&nbsp;    </b></td> <td nowrap>&nbsp;</td> </tr>"
				+ "<tr> <td nowrap><b>Day:      </b></td> <td nowrap>" + (startTs == null ? "-NULL-" : dayOfWeek.format(startTs)) + "</td> </tr>"
				+ "<tr> <td nowrap><b>Start:    </b></td> <td nowrap>" + (startTs == null ? "-NULL-" : ymd.format(startTs)      ) + " <b>" + (startTs == null ? "-NULL-" : hms.format(startTs)) + "</b></td> </tr>"
				+ "<tr> <td nowrap><b>End:      </b></td> <td nowrap>" + (  endTs == null ? "-NULL-" : ymd.format(endTs)        ) + " <b>" + (  endTs == null ? "-NULL-" : hms.format(endTs)  ) + "</b></td> </tr>"
				;
		
		if (extraColumns != null && !extraColumns.isEmpty())
		{
			// first add blank row in table
			tooltip += "<tr> <td nowrap><b>&nbsp;    </b></td> <td nowrap>&nbsp;</td> </tr>";

			// Then for each value add a table row...
			for (Entry<String, String> entry : extraColumns.entrySet())
			{
				String ttKey = entry.getKey();
				String ttVal = entry.getValue();

				String tdAttr = "";

//				// If this is a "tooltip-td-attribute" for some specific column... skip this row
//				if (ttKey.startsWith("tooltip-td-attribute:"))
//					continue;

				if (ttKey.startsWith("tooltip-layout-"))
				{
					if (ttKey.startsWith("tooltip-layout-separator"))
					{
						ttKey = "&nbsp;";
						ttVal = "&nbsp;";
					}
				}

//				// Get various attributes for this tool tip, for example: should it be colored
//				String toolTipTdAttributeForKey = extraColumns.get("tooltip-td-attribute:" + ttKey);
//				if (StringUtil.hasValue(toolTipTdAttributeForKey))
//				{
//					// Tke val could for example be: "style='color:red;'"
//					tdAttr = toolTipTdAttributeForKey;
//				}
				
				// If the tooltipValue looks like: "<tooltip-td-attribute>style='color:red;'</tooltip-td-attribute>This tooltip will be in color red..."
				// Then we will extract the "style" and "the tip" separately
				if (ttVal.startsWith("<tooltip-td-attribute>"))
				{
					tdAttr = StringUtils.substringBetween(ttVal, "<tooltip-td-attribute>", "</tooltip-td-attribute>");
					ttVal  = StringUtils.substringAfter(ttVal, "</tooltip-td-attribute>").trim();
				}
				
				// Fixes: 
				//  - Strange characters into HTML characters
				//  - Newlines 
				ttVal = StringEscapeUtils.escapeHtml4(ttVal);
//				ttVal = ttVal.replace("\r", "\\r");
//				ttVal = ttVal.replace("\n", "\\n");
				ttVal = ttVal.replace("\r", "");
				ttVal = ttVal.replace("\n", "<br>");
				ttVal = ttVal.replace("\\", "&#92;");
				
				// If key is "command", then add <code></code> (hard coded because: I was lazy)
				if (ttKey.equalsIgnoreCase("command"))
					ttVal = "<code>" + ttVal + "</code>";

				// If key is "message", then try to parse the message a bit and add NEWLINES in some places (hard coded because: I was lazy)
				if (ttKey.equalsIgnoreCase("message"))
					ttVal = tooltipForMessage(ttVal);

				// Hack to get a separator in before column 'main_job_start_ts' (hard coded because: I was lazy)
				if (ttKey.equals("main_job_start_ts"))
					tooltip += "<tr> <td>&nbsp;</td> <td>&nbsp;</td> </tr>";
				
				tooltip += "<tr> <td nowrap><b>" + ttKey + ": </b></td> <td nowrap " + tdAttr + ">" + ttVal + "</td> </tr>";
			}
		}
		
		tooltip += ""
				+ "</table>"
				+ "<br>"
				+ "</div>";
		
		return tooltip;
	}

	/**
	 * Try to make messages a bit more readable<br>
	 *  - If it's to long try to add NEWLINE somewhere
	 *  - NewLine on some special words
	 *  - If it looks like a "console log line" add newlines
	 * @param ttVal
	 * @return
	 */
	private static String tooltipForMessage(String ttVal)
	{
		if (ttVal == null)
			return null;
		
		if (ttVal.length() < 64)
			return ttVal;
		
		// "Executed as user: MAXM\\goran.schwarz. " -->> "Executed as user: MAXM\\goran.schwarz. <BR>"
		ttVal = ttVal.replaceFirst("Executed as user: \\S* ", "$0<BR>");
		
		// Console messages
		ttVal = ttVal.replace("DEBUG   - ", "<BR>DEBUG   - ");
		ttVal = ttVal.replace("INFO    - ", "<BR>INFO    - ");
		ttVal = ttVal.replace("WARNING - ", "<BR>WARNING - ");
		ttVal = ttVal.replace("ERROR   - ", "<BR>ERROR   - ");

		// Some Error messages
		ttVal = ttVal.replace("ERROR-MSG: ", "<BR>ERROR-MSG: ");
		
		ttVal = ttVal.replace("Warning! ", "<BR>Warning! ");
		ttVal = ttVal.replace("Warning: ", "<BR>Warning: ");

		ttVal = ttVal.replace("Process Exit Code ", "<BR>Process Exit Code ");
		
		ttVal = ttVal.replace("The step failed."   , "<BR>The step failed."); 
		ttVal = ttVal.replace("The step succeeded.", "<BR>The step succeeded.");
		
		// Remove any "double newlines"
		ttVal = ttVal.replace("<BR><BR>", "<BR>");
		
		return ttVal;
	}
	
	private String calculateDuration(Timestamp startTs, Timestamp endTs)
	{
		if (startTs == null || endTs == null)
			return "-NULL-";

		long duration = endTs.getTime() - startTs.getTime();

		String res = ""; // java.util.concurrent.TimeUnit;
		
		long days    = TimeUnit.MILLISECONDS.toDays(duration);
		long hours   = TimeUnit.MILLISECONDS.toHours(duration)   - TimeUnit.DAYS   .toHours(  TimeUnit.MILLISECONDS.toDays(duration));
		long minutes = TimeUnit.MILLISECONDS.toMinutes(duration) - TimeUnit.HOURS  .toMinutes(TimeUnit.MILLISECONDS.toHours(duration));
		long seconds = TimeUnit.MILLISECONDS.toSeconds(duration) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration));
//		long millis  = TimeUnit.MILLISECONDS.toMillis(duration)  - TimeUnit.SECONDS.toMillis( TimeUnit.MILLISECONDS.toSeconds(duration));

		if (days > 0)
			res += days + " days, ";

		if (hours > 0)
			res += hours + "h ";

		if (minutes > 0)
			res += minutes + "m ";

		if (seconds > 0)
			res += seconds + "s ";

		if ( days == 0 )
			res += String.format("    &emsp;<code>%02d:%02d:%02d</code>", hours, minutes, seconds);
		else
			res += String.format("    &emsp;<code>%dd %02d:%02d:%02d</code>", days, hours, minutes, seconds);

		return res;
	}
}
