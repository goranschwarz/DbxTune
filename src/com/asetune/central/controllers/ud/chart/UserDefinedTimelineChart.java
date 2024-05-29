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
package com.asetune.central.controllers.ud.chart;

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
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.log4j.Logger;

import com.asetune.gui.ResultSetTableModel;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;

public class UserDefinedTimelineChart
extends UserDefinedChartAbstract
{
	private static Logger _logger = Logger.getLogger(UserDefinedTimelineChart.class);

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
		return new String[] {"startTime", "endTime", "showKeys", "keyTransform", "minDurationInSeconds", "keepNames", "skipNames", "fillEnd", "generateDummyRows", "useDefaultTooltip"};
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
		                                    + "Example: <code>-4h             </code> (set StartTime to 'now' -4 hours) <br>"
		                                    + "Example: <code>-2d             </code> (set StartTime to 'now' -2 days) <br>"
		                                    + "Example: <code>2024-03-08 18:00</code> (set startTime to a absolute timestamp) <br>"
		                                    + "<b>Default</b>: <code>-2       </code> (last 2 hours)");

		map.put("endTime",              "What is the end time <br>"
		                                    + "<br>"
		                                    + "Example: <code>2024-03-08 22:00</code> (set endTime to a absolute timestamp) <br>"
		                                    + "Example: <code>4h              </code> (set endTime to 4 hours after the 'startTime') <br>"
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
		
		return map;
	}

	@Override
	public String getUrl()
	{
		return "/api/udc"
				+ "?name="    + getName()
				+ "&srvName=" + getDbmsServerName()
				+ ( getRefresh() <= 0 ? "" : "&refresh=" + getRefresh() )
				+ "&showKeys=false"
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
	public List<String> getJaveScriptList()
	{
		List<String> list = new ArrayList<>();
		
		list.add("https://www.gstatic.com/charts/loader.js");

		return list;
	}

	@Override
	public void checkUrlParameters(Map<String, String> parameterMap) throws Exception
	{
		String[] allowedDateFormats = new String[] {"yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM-dd HH", "yyyy-MM-dd"};
		
		String startTime = parameterMap.get("startTime");
		if (StringUtil.isNullOrBlank(startTime))
			startTime = _defaultStartTime;

		String endTime   = parameterMap.get("endTime");
		
		Timestamp startTs = null;
		Timestamp endTs   = null;

		// Is the 'startTime' in hours or days
		int startTimeHourAdjust = 1;
		if ( StringUtil.hasValue(startTime) )
		{
			if (startTime.equalsIgnoreCase("TODAY"))
			{
				// Get TODAY as 'yyyy-MM-dd'
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
				startTime = sdf.format(new Timestamp(System.currentTimeMillis()));
			}
			else if (startTime.toUpperCase().endsWith("H"))
			{
				startTime = startTime.substring(0, startTime.length()-1);
				startTimeHourAdjust = 1;
			}
			else if (startTime.endsWith("D"))
			{
				startTime = startTime.substring(0, startTime.length()-1);
				startTimeHourAdjust = 24;
			}
		}

		// Is the 'endTime' in hours or days
		int endTimeHourAdjust = 1;
		if ( StringUtil.hasValue(endTime) )
		{
			if (endTime.toUpperCase().endsWith("H"))
			{
				endTime = endTime.substring(0, endTime.length()-1);
				endTimeHourAdjust = 1;
			}
			else if (endTime.endsWith("D"))
			{
				endTime = endTime.substring(0, endTime.length()-1);
				endTimeHourAdjust = 24;
			}
		}

		//----------------------------------------
		// startTime: If integer -> set the time
		if ( StringUtil.isInteger(startTime) )
		{
			int intPeriod = Math.abs(StringUtil.parseInt(startTime, 2));

			startTs = new Timestamp(System.currentTimeMillis() - (intPeriod * startTimeHourAdjust * 3600 * 1000));
		}
		else
		{
			// parse a ISO date with optional time parameter -- lets use Apache Commons
			startTs = new Timestamp(DateUtils.parseDate(startTime, allowedDateFormats).getTime());
		}

		//----------------------------------------
		// endTime
		if ( StringUtil.hasValue(endTime) && startTs != null)
		{
			if ( StringUtil.isInteger(endTime) )
			{
				int intPeriod = Math.abs(StringUtil.parseInt(endTime, 2));

				endTs = new Timestamp(startTs.getTime() + (intPeriod * endTimeHourAdjust * 3600 * 1000));
			}
			else
			{
				// parse a ISO date with optional time parameter -- lets use Apache Commons
				endTs = new Timestamp(DateUtils.parseDate(endTime, allowedDateFormats).getTime());
			}
		}


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

		// >>> showKeys
		boolean showKeys = true;
		tmpParamStr  = urlParams.get("showKeys");
		if (tmpParamStr != null && tmpParamStr.equalsIgnoreCase("false"))
			showKeys = false;

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

				sb.append("<div id='timeline' style='height: 90%;'> \n");
				sb.append("</div> \n");
				sb.append("\n");

				sb.append("<script> \n");
				sb.append("    google.charts.load('current', {'packages':['timeline']});								\n");
				sb.append("    google.charts.setOnLoadCallback(drawChart);												\n");
				sb.append("    function drawChart()																		\n");
				sb.append("    {																						\n");
				sb.append("        var container = document.getElementById('timeline');									\n");
				sb.append("        var chart = new google.visualization.Timeline(container);							\n");
				sb.append("        var dataTable = new google.visualization.DataTable();								\n");
				sb.append("																								\n");
				sb.append("        dataTable.addColumn({ type: 'string', id: 'TextLabel' });							\n");
				sb.append("        dataTable.addColumn({ type: 'string', id: 'BarText'   });							\n");
				sb.append("        dataTable.addColumn({ type: 'string', role: 'style'   });	// bar color			\n");
				if ( ! useDefaultTooltip )
				{
					sb.append("        dataTable.addColumn({ type: 'string', role: 'tooltip' });							\n"); // This is if we want to produce our own tooltip
				}
				sb.append("        dataTable.addColumn({ type: 'date', id: 'Start' });									\n");
				sb.append("        dataTable.addColumn({ type: 'date', id: 'End' });									\n");
				sb.append("        dataTable.addRows([																	\n");

				String udTooltip = "";

				Timestamp maxTs          = null;
				String    prefix         = " ";
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

					// Columns above FIVE will be added as tooltip values
					Map<String, String> extraColumns = null;
					if (colCount > 5)
					{
						extraColumns = new LinkedHashMap<>();
						for (int col = 6; col <= colCount; col++)
						{
							String colName = rsmd.getColumnLabel(col);
							String colVal  = rs.getString(col);

							if (StringUtil.hasValue(colVal))
								extraColumns.put(colName, colVal);
						}
					}
					
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
						labelKey = labelKey.replaceAll(keyTransformFrom, keyTransformTo);
					
					udTooltip = createUserDefinedTooltip(useDefaultTooltip, labelKey, barText, startTs, endTs, extraColumns);
					labelKey  = escapeJsQuote(labelKey);
					barText   = escapeJsQuote(barText);
					
					// TODO: is there some way we can set background color.. If it's a "FULL JOB" it would be nice to a have a different BG color

//					sb.append("                          [ 'label', 'barText', 'green', new Date(2020,10,20, 14,1,1), new Date(2020,10,20, 14,1,2) ] \n");
					sb.append("            " + prefix + "[ '" + labelKey + "', '" + barText + "', '" + barColor + "', " + udTooltip + " new Date('" + sdf.format(startTs) + "'), new Date('" + sdf.format(endTs) + "') ] \n");
					prefix = ",";

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
						Timestamp startTs = _startTime;
						Timestamp endTs   = _endTime;
						udTooltip = createUserDefinedTooltip(useDefaultTooltip, noActivityLabel, noActivityLabel, startTs, endTs, null);
						// NO Activity -- FULL Period
						sb.append("            " + prefix + "[ '" + noActivityLabel + "', '" + noActivityLabel + "', '" + noActivityColor + "', " + udTooltip + " new Date('" + sdf.format(startTs) + "'), new Date('" + sdf.format(endTs) + "') ] \n");
					}
					else
					{
						Timestamp startTs = maxTs;
						Timestamp endTs   = new Timestamp(System.currentTimeMillis());
						udTooltip = createUserDefinedTooltip(useDefaultTooltip, noActivityLabel, noActivityLabel, startTs, endTs, null);

						// Only write "end-filler" if it's more than 10 seconds
						long tsDiffMs = endTs.getTime() - startTs.getTime();
						if (tsDiffMs > 10_000)
						{
							// NO Activity -- AT THE END
							sb.append("            " + prefix + "[ '" + noActivityLabel + "', '" + noActivityLabel + "', '" + noActivityColor + "', " + udTooltip + " new Date('" + sdf.format(startTs) + "'), new Date('" + sdf.format(endTs) + "') ] \n");
						}
					}
					prefix = ",";
				}

				if (generateDummyRows > 0)
				{
					String dummyLabel = StringUtil.isNullOrBlank(generateDummyRowsStr) ? "Dummy row {id}" : generateDummyRowsStr + " ";
					String dummyColor = "yellow";
					int    dummyTime  = 60_000; // 60 seconds

					Timestamp startTs = null;
					Timestamp endTs   = null;

					udTooltip = useDefaultTooltip ? "" : "'dummy tooltip', ";

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

						udTooltip = createUserDefinedTooltip(useDefaultTooltip, dummyLabel, dummyLabel, startTs, endTs, null);

						sb.append("            " + prefix + "[ '" + tmpDummyLabel + "', '" + tmpDummyLabel + "', '" + dummyColor + "', " + udTooltip + " new Date('" + sdf.format(startTs) + "'), new Date('" + sdf.format(endTs) + "') ] \n");
						
						prefix = ",";
					}
					
				}

				// END_OF: dataTable.addRows([
				sb.append("        ]);																					\n");

				
				sb.append("																								\n");
				sb.append("        google.visualization.events.addListener(chart, 'ready', function() {					\n");
				sb.append("            console.log('DONE: Loading chart-timeline, now scrolling to bottom...');			\n");
				sb.append("            scrollToBottom('timeline');														\n");
				sb.append("        });																					\n");
				sb.append("																								\n");
				sb.append("        // https://almende.github.io/chap-links-library/js/timeline/doc/						\n");
				sb.append("        var options = 																		\n");
				sb.append("        {																					\n");
				sb.append("            timeline: { 																		\n");
				sb.append("                showRowLabels: " + showKeys + "												\n");
				sb.append("            } 																				\n");
//				sb.append("            timeline: { singleColor: '#8d8' },												\n");
//				sb.append("            width: '100%',																	\n");
//				sb.append("            height: '100%'																	\n");
				sb.append("        };																					\n");
				sb.append("																								\n");
				sb.append("        chart.draw(dataTable, options);														\n");
				sb.append("    }																						\n");
				sb.append("																								\n");
				sb.append("    function changeAutoscroll() {															\n");
				sb.append("       var div = document.getElementById('autoscroll-to-bottom');							\n");
				sb.append("       var storedData = getStorage('dbxtune_checkboxes_');									\n");
				sb.append("       storedData.set('autoscroll-to-bottom', div.checked);									\n");
				sb.append("																								\n");
				sb.append("       console.log('changeAutoscroll: ' + div.checked);										\n");
				sb.append("    }																						\n");
				sb.append("																								\n");
				sb.append("																								\n");
				sb.append("    function recursiveScrollToTopOrBottom(element, to, level, maxLevel)						\n");
				sb.append("    {                                                                                        \n");
				sb.append("        level = level + 1;                                                                   \n");
				sb.append("        if (level > maxLevel)                                                                \n");
				sb.append("            return;                                                                          \n");
				sb.append("        var childArr = element.children;                                                     \n");
				sb.append("        if (childArr.length > 0)                                                             \n");
				sb.append("        {                                                                                    \n");
				sb.append("            for (var child of childArr)                                                      \n");
				sb.append("            {                                                                                \n");
//				sb.append("//              if (child.css('overflow-y') == 'scroll' || child.css('overflow-y') == 'auto')                        \n");
//				sb.append("//              if ( (child.scrollHeight > child.clientHeight) && ('hidden' !== getComputedStyle(child).overflowY) )	\n");
				sb.append("                if ( (child.scrollHeight > child.clientHeight) )                                                     \n");
				sb.append("                {                                                                                                    \n");
				sb.append("                    console.log('Scroll to ' + to + ': level[' + level + '], child='+child, child);                  \n");
				sb.append("                    if (to === 'top')                                                        \n");
				sb.append("                        child.scrollTop = 0;                                                 \n");
				sb.append("                    else                                                                     \n");
				sb.append("                        child.scrollTop = child.scrollHeight - child.clientHeight;           \n");
//				sb.append("					                                                                            \n");
//				sb.append("//                  break;                                                                   \n");
				sb.append("                }                                                                            \n");
				sb.append("				                                                                                \n");
				sb.append("                recursiveScrollToTopOrBottom(child, to, level, maxLevel);                    \n");
				sb.append("            }                                                                                \n");
				sb.append("        }                                                                                    \n");
				sb.append("    }                                                                                        \n");
				sb.append("                                                                                             \n");
				sb.append("																								\n");
//				sb.append("    function scrollToBottom (id) {															\n");
//				sb.append("       var div = document.getElementById(id);												\n");
////				sb.append("       div.scrollIntoView({behavior: 'smooth', block: 'end', inline: 'nearest'});			\n");
//				sb.append("       div.scrollTop = div.scrollHeight - div.clientHeight;									\n");
//				sb.append("       console.log('Scroll to BOTTOM: ' + id);												\n");
//				sb.append("    }																						\n");
//				sb.append("    																							\n");
//				sb.append("    function scrollToTop (id) {																\n");
//				sb.append("       var div = document.getElementById(id);												\n");
////				sb.append("       div.scrollIntoView({behavior: 'smooth', block: 'start', inline: 'nearest'});			\n");
//				sb.append("       div.scrollTop = 0;													 				\n");
//				sb.append("       console.log('Scroll to TOP: ' + id);													\n");
//				sb.append("    }																						\n");
				sb.append("    																							\n");
				sb.append("    function scrollToBottom (id) {															\n");
				sb.append("       var elem = document.getElementById(id);												\n");
				sb.append("       recursiveScrollToTopOrBottom(elem, 'bottom', -1, 3);									\n");
				sb.append("    }																						\n");
				sb.append("    																							\n");
				sb.append("    function scrollToTop (id) {																\n");
				sb.append("       var elem = document.getElementById(id);												\n");
				sb.append("       recursiveScrollToTopOrBottom(elem, 'top', -1, 3);										\n");
				sb.append("    }																						\n");
				sb.append("    																							\n");
				sb.append("    function copyExecutedSql() {																\n");
				sb.append("       var sqlText = document.getElementById('executed_sql').textContent;					\n");
				sb.append("																								\n");
				sb.append("       const textArea = document.createElement('textarea');									\n");
				sb.append("       textArea.value = sqlText;																\n");
				sb.append("       document.body.appendChild(textArea);													\n");
				sb.append("       textArea.select();																	\n");
				sb.append("       try {																					\n");
				sb.append("       	document.execCommand('copy');														\n");
				sb.append("       } catch (err) {																		\n");
				sb.append("       	alert('Unable to copy to clipboard' + err);											\n");
				sb.append("       }																						\n");
				sb.append("       document.body.removeChild(textArea);													\n");
				sb.append("																								\n");
				sb.append("       console.log('copyExecutedSql: ' + sqlText);											\n");
				sb.append("    }																						\n");
				sb.append("																								\n");
				sb.append("    // do-deferred: Scroll to bottom of 'timeline'											\n");
				sb.append("																								\n");
				sb.append("    setTimeout(function() {																	\n");
				sb.append("        var savedVal_autoscrollToBottom = getStorage('dbxtune_checkboxes_').get('autoscroll-to-bottom');		\n");
				sb.append("        document.getElementById('autoscroll-to-bottom').checked = savedVal_autoscrollToBottom;				\n");
				sb.append("        if (savedVal_autoscrollToBottom) {													\n");
				sb.append("            scrollToBottom('timeline');														\n");
				sb.append("        }																					\n");
				sb.append("    }, 200);																					\n");
				sb.append("																								\n");
//				sb.append("    if (document.getElementById('autoscroll-to-bottom').checked)	{							\n");
//				sb.append("        setTimeout(function() { scrollToBottom('timeline') }, 100);							\n");
//				sb.append("    }																						\n");
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
			return "";
		
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
				
				tooltip += "<tr> <td nowrap><b>" + ttKey + "</b></td> <td nowrap " + tdAttr + ">" + ttVal + "</td> </tr>";
			}
		}
		
		tooltip += ""
				+ "</table>"
				+ "<br>"
				+ "</div>";
		
		return "'" + escapeJsQuote(tooltip) + "', ";
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
