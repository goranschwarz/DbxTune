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
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.time.DateUtils;

import com.asetune.gui.ResultSetTableModel;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;

public class UserDefinedTimelineChart
extends UserDefinedChartAbstract
{
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
		return new String[] {"startTime", "endTime"};
	}

	@Override
	public String getUrl()
	{
		return "/api/udc"
				+ "?name="    + getName()
				+ "&srvName=" + getDbmsServerName()
				+ ( getRefresh() <= 0 ? "" : "&refresh=" + getRefresh() )
				+ "&startTime="+_defaultStartTime;
	}

	@Override
	public String getDbmsSql()
	{
		String sql = super.getDbmsSql();
		
		if (sql.contains("${startTime}")) sql = sql.replace("${startTime}", "'" + _startTime + "'");
		if (sql.contains("${endTime}"  )) sql = sql.replace("${endTime}"  , "'" + _endTime   + "'");
		
		return sql;
	}
	
	@Override
	public void produce() 
	throws Exception
	{
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

		//----------------------------------------
		// startTime: If integer -> set the time
		if ( StringUtil.isInteger(startTime) )
		{
			int hours = Math.abs(StringUtil.parseInt(startTime, 2));

			startTs = new Timestamp(System.currentTimeMillis() - (hours * 3600 * 1000));
		}
		else // parse a ISO date with optional time parameter
		{
			// lets use Apache Commons
			startTs = new Timestamp(DateUtils.parseDate(startTime, allowedDateFormats).getTime());
		}

		//----------------------------------------
		// endTime
		if ( StringUtil.hasValue(endTime) )
		{
			endTs = new Timestamp(DateUtils.parseDate(endTime, allowedDateFormats).getTime());
		}
		
		
		if (startTs != null)
		{
			_startTime = startTs;
			parameterMap.put("startTime", _startTime + "");
		}

		_endTime = endTs != null ? endTs : new Timestamp(System.currentTimeMillis());
		parameterMap.put("endTime", _endTime + "");
	}

	private String createTimeline() 
	throws Exception
	{
		StringBuilder sb = new StringBuilder();

		// Connect to DBMS - with AutoClose
		try ( DbxConnection conn = dbmsConnect() ) 
		{
			String sql = getDbmsSql();

			// Execute the SQL - with AutoClose
			try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql); )
			{
				ResultSetMetaData rsmd = rs.getMetaData();
				int colCount = rsmd.getColumnCount();
				if (colCount != 5)
				{
					throw new Exception("The ResultSet must have 5 columns (it has " + colCount + "). 1=[label:String], 2=[barText:String], 3=[barColor:String], 4=[startDate:Timestamp], 5=[endDate:Timestamp]");
				}

				int col4_datatype = rsmd.getColumnType(4);
				int col5_datatype = rsmd.getColumnType(5);

				if (col4_datatype != Types.TIMESTAMP) throw new Exception("The ResultSet for column 4 has to be of type TIMESTAMP, it was " + ResultSetTableModel.getColumnJavaSqlTypeName(col4_datatype) + ". Expected ResultSet: 1=[label:String], 2=[barText:String], 3=[barColor:String], 4=[startDate:Timestamp], 5=[endDate:Timestamp]");
				if (col5_datatype != Types.TIMESTAMP) throw new Exception("The ResultSet for column 5 has to be of type TIMESTAMP, it was " + ResultSetTableModel.getColumnJavaSqlTypeName(col5_datatype) + ". Expected ResultSet: 1=[label:String], 2=[barText:String], 3=[barColor:String], 4=[startDate:Timestamp], 5=[endDate:Timestamp]");

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
				
				sb.append("<div id='servlet-params'> \n");
				sb.append("Variables: <code>" + getUrlParameters() + "</code><br> \n");
				sb.append("Scroll to: \n");
				sb.append("<a href='#' onClick=\"scrollToTop('timeline')\">Top</a>  \n");
				sb.append(" or \n");
				sb.append("<a href='#' onClick=\"scrollToBottom('timeline')\">Bottom</a> \n");
				sb.append(" of the Timeline. &emsp; <input type='checkbox' id='autoscroll-to-bottom' onclick='changeAutoscroll()'> On load AutoScroll to bottom.\n");
				sb.append("</div> \n");
				sb.append("\n");

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
				sb.append("        dataTable.addColumn({ type: 'string', role: 'style' });		// bar color			\n");
				sb.append("        dataTable.addColumn({ type: 'date', id: 'Start' });									\n");
				sb.append("        dataTable.addColumn({ type: 'date', id: 'End' });									\n");
				sb.append("        dataTable.addRows([																	\n");
				
				String prefix = " ";
				while(rs.next())
				{
					String label      = rs.getString   (1);
					String barText    = rs.getString   (2);
					String barColor   = rs.getString   (3);
					Timestamp startTs = rs.getTimestamp(4);
					Timestamp endTs   = rs.getTimestamp(5);
					
					if (StringUtil.isNullOrBlank(barColor))
						barColor = "green";

					if (endTs == null)
						endTs = new Timestamp(System.currentTimeMillis());

					label   = escapeJsQuote(label);
					barText = escapeJsQuote(barText);

//					sb.append("                          [ 'label', 'barText', 'green', new Date(2020,10,20, 14,1,1), new Date(2020,10,20, 14,1,2) ] \n");
					sb.append("            " + prefix + "[ '" + label + "', '" + barText + "', '" + barColor + "', new Date('" + startTs + "'), new Date('" + endTs + "') ] \n");
					prefix = ",";
				}
				
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
				sb.append("    function scrollToBottom (id) {															\n");
				sb.append("       var div = document.getElementById(id);												\n");
//				sb.append("       div.scrollIntoView({behavior: 'smooth', block: 'end', inline: 'nearest'});			\n");
				sb.append("       div.scrollTop = div.scrollHeight - div.clientHeight;									\n");
				sb.append("       console.log('Scroll to BOTTOM: ' + id);												\n");
				sb.append("    }																						\n");
				sb.append("    																							\n");
				sb.append("    function scrollToTop (id) {																\n");
				sb.append("       var div = document.getElementById(id);												\n");
//				sb.append("       div.scrollIntoView({behavior: 'smooth', block: 'start', inline: 'nearest'});			\n");
				sb.append("       div.scrollTop = 0;													 				\n");
				sb.append("       console.log('Scroll to TOP: ' + id);													\n");
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
		
		return sb.toString();
	}
}
