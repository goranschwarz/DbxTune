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
package com.asetune.central.controllers;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.sql.Timestamp;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.asetune.Version;
import com.asetune.central.pcs.H2WriterStat;
import com.asetune.central.pcs.H2WriterStat.StatEntry;
import com.asetune.utils.StringUtil;
import com.asetune.utils.TimeUtils;

public class H2WriterStatServlet
extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	private static final Logger _logger = Logger.getLogger(MethodHandles.lookup().lookupClass());

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		ServletOutputStream out = resp.getOutputStream();
		resp.setContentType("text/html");
		resp.setCharacterEncoding("UTF-8");
//		resp.setContentType("application/json");
//		resp.setCharacterEncoding("UTF-8");


		out.println("<html>");
		out.println("<head>");

		out.println("<title>Server Overview</title> ");
		
//		if (refresh > 0)
//			out.println("<meta http-equiv='refresh' content='"+refresh+"' />");

		out.println(HtmlStatic.getOverviewHead());

		// Copied from: graph.html
		out.println("	<!--  ");
		out.println("	  ======================================================================= ");
		out.println("	  == JS imports - JAVA SCRIPTS GOES HERE ");
		out.println("	  ======================================================================= ");
		out.println("	--> ");
		out.println("	<!-- <link rel='stylesheet' href='https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/css/bootstrap.min.css' integrity='sha384-Gn5384xqQ1aoWXA+058RXPxPg6fy4IWvTNh0E263XmFcJlSAwiGgFAW/dAiS6JXm' crossorigin='anonymous'> --> ");
		out.println("	<!-- Custom styles for this template --> ");
		out.println("	<!-- <link href='/scripts/bootstrap/css/navbar-top-fixed.css' rel='stylesheet'> --> ");
		out.println("	<script type='text/javascript' src='/scripts/jquery/jquery-3.2.1.min.js'></script> ");
		out.println("	<script type='text/javascript' src='/scripts/jquery/ui/1.11.3/jquery-ui.min.js'></script> ");
		out.println("	 ");
		out.println("	<!-- ");
		out.println("<script type='text/javascript' src='http://www.chartjs.org/dist/2.7.1/Chart.bundle.js'></script> ");
		out.println("<script type='text/javascript' src='http://www.chartjs.org/samples/latest/utils.js'></script> ");
		out.println("--> ");
		out.println("	<!-- JS: Moment; used by: ChartJs, DateRangePicker --> ");
		out.println("	<script type='text/javascript' src='/scripts/moment/moment.js'></script> ");
		out.println(" ");
		out.println("	<!-- JS: Chart JS --> ");
		out.println("	<script type='text/javascript' src='/scripts/chartjs/Chart.bundle.js'></script> ");
		out.println("	<script type='text/javascript' src='/scripts/chartjs/samples/utils.js'></script> ");
		out.println("	<script type='text/javascript' src='/scripts/chartjs/plugins/chartjs-plugin-zoom.js'></script> ");
		out.println("	<script type='text/javascript' src='/scripts/chartjs/plugins/chartjs-plugin-annotation.js'></script> ");
		out.println("<!--<script type='text/javascript' src='/scripts/chartjs/plugins/chartjs-plugin-datalabels.js'></script>--> ");
		out.println("<!--<script type='text/javascript' src='/scripts/chartjs/plugins/chartjs-plugin-colorschemes.js'></script>--> ");
		out.println(" ");
		out.println("	<!-- JS: DbxCentral --> ");
		out.println("	<script type='text/javascript' src='/scripts/dbxcentral.utils.js'></script> ");
		out.println("	<script type='text/javascript' src='/scripts/dbxcentral.graph.js'></script> ");
		out.println(" ");
		out.println("	<!-- JS: Bootstrap --> ");
		out.println("	<!-- <link href='/scripts/bootstrap/css/bootstrap.min.css' rel='stylesheet'> --> ");
		out.println("	<script type='text/javascript' src='/scripts/bootstrap/js/bootstrap.min.js'></script> ");
		out.println("	<script type='text/javascript' src='/scripts/popper/1.12.9/popper.min.js'></script> ");
		out.println(" ");
		out.println("	<!-- JS: DateRangePicker --> ");
		out.println("	<script type='text/javascript' src='/scripts/bootstrap-daterangepicker/2.1.27/daterangepicker.js'></script> ");
		out.println(" ");
		out.println("	<!-- JS: Bootstrap - TABLE --> ");
		out.println("	<script type='text/javascript' src='/scripts/bootstrap-table/1.12.1/bootstrap-table.js'></script> ");
		out.println("	<!-- <script src='https://cdnjs.cloudflare.com/ajax/libs/bootstrap-table/1.12.1/extensions/editable/bootstrap-table-editable.js'></script> --> ");
		out.println("	<!-- <script src='https://cdnjs.cloudflare.com/ajax/libs/bootstrap-table/1.12.1/extensions/export/bootstrap-table-export.js'></script> --> ");
		out.println("	<!-- <script src='https://rawgit.com/hhurz/tableExport.jquery.plugin/master/tableExport.js'></script> --> ");
		out.println("	<script type='text/javascript' src='/scripts/bootstrap-table/1.12.1/extensions/filter-control/bootstrap-table-filter-control.js'></script> ");
		out.println(" ");
		out.println("	<!-- JS: jquery - contextMenu --> ");
		out.println("	<script type='text/javascript' src='/scripts/jquery-contextmenu/2.7.0/jquery.contextMenu.min.js'></script> ");
		out.println(" ");
		out.println("	<!--  ");
		out.println("	  ======================================================================= ");
		out.println("	  == CSS imports - STYLES SHEETS GOES HERE ");
		out.println("	  ======================================================================= ");
		out.println("	--> ");
		out.println("	<!-- CSS: jqueri-ui --> ");
		out.println("	<link rel='stylesheet' href='/scripts/jquery/ui/1.11.3/themes/smoothness/jquery-ui.css'> ");
		out.println(" ");
		out.println("	<!-- CSS: DbxCentral --> ");
		out.println("	<link rel='stylesheet' href='/scripts/css/dbxcentral.css'> ");
		out.println(" ");
		out.println("	<!-- CSS: iota - layout --> ");
		out.println("	<link rel='stylesheet' href='/scripts/css/iota.css' type='text/css'> ");
		out.println(" ");
		out.println("	<!-- CSS: Bootstrap --> ");
		out.println("	<link rel='stylesheet' href='/scripts/bootstrap/css/bootstrap.min.css'> ");
		out.println(" ");
		out.println("	<!-- CSS: DateRangePicker --> ");
		out.println("	<link rel='stylesheet' href='/scripts/bootstrap-daterangepicker/2.1.27/daterangepicker.css'> ");
		out.println(" ");
		out.println("	<!-- CSS: Font Awsome --> ");
		out.println("	<link rel='stylesheet' href='/scripts/font-awesome/4.4.0/css/font-awesome.min.css'> ");
		out.println(" ");
		out.println("	<!-- CSS: W2UI --> ");
		out.println("	<!-- <link rel='stylesheet' href='scripts/w2ui/w2ui-1.5.rc1.css' type='text/css'> --> ");
		out.println("	<!-- <script src='scripts/w2ui/w2ui-1.5.rc1.js' type='text/javascript'></script> --> ");
		out.println(" ");
		out.println("	<!-- JS: Bootstrap - TABLE --> ");
		out.println("	<link rel='stylesheet' href='/scripts/bootstrap-table/1.12.1/bootstrap-table.min.css'> ");
		out.println("	<!-- <link rel='stylesheet' href='https://rawgit.com/vitalets/x-editable/master/dist/bootstrap3-editable/css/bootstrap-editable.css'> --> ");
		out.println(" ");
		out.println("	<!-- JS: jquery - contextMenu --> ");
		out.println("	<link rel='stylesheet' href='/scripts/jquery-contextmenu/2.7.0/jquery.contextMenu.min.css'> ");
		out.println(" ");
		
//		out.println("<style type='text/css'>");
//		out.println("  table {border-collapse: collapse;}");
//		out.println("  th {border: 1px solid black; text-align: left; padding: 2px; white-space: nowrap; background-color:gray; color:white;}");
//		out.println("  td {border: 1px solid black; text-align: left; padding: 2px; white-space: nowrap; }");
//		out.println("  tr:nth-child(even) {background-color: #f2f2f2;}");
////		out.println("  .topright { position: absolute; top: 8px; right: 16px; font-size: 14px; }"); // topright did not work with bootstrap (and navigation bar) 
//		out.println("</style>");

		out.println("<link href='/scripts/prism/prism.css' rel='stylesheet' />");

		out.println("</head>");
		
//		out.println("<body onload='updateLastUpdatedClock()'>");
		out.println("<body>");
		out.println("<script src='/scripts/prism/prism.js'></script> ");

		out.println(HtmlStatic.getOverviewNavbar());

		out.println("<div class='container-fluid'>");

		String ver = "Version: " + Version.getVersionStr() + ", Build: " + Version.getBuildStr();
		
		out.println("<h1>DbxTune - Central - H2 Writer Statistics</h1>");

		// Check for known input parameters
		if (Helper.hasUnKnownParameters(req, resp, "filename", "startDate", "endDate", "minDuration"))
			return;

		String filename       = Helper.getParameter(req, "filename",    null);
		String startDateStr   = Helper.getParameter(req, "startDate",   null);
		String endDateStr     = Helper.getParameter(req, "endDate",     null);
		String minDurationStr = Helper.getParameter(req, "minDuration", "-1");

		int minDuration = StringUtil.parseInt(minDurationStr, -1);
		
		out.println("<p>");
		out.println("</p>");

//		out.println("Input parameters");
//		out.println("<ul>");
//		out.println("<li><b>filename                   </b> - " + filename     + "</li>");
//		out.println("<li><b>startDate                  </b> - " + startDateStr + "</li>");
//		out.println("<li><b>endDate                    </b> - " + endDateStr   + "</li>");
//		out.println("<li><b>minDuration                </b> - " + minDuration  + "</li>");
//		out.println("</ul>");

		out.println("<form action='/h2ws'> ");
		out.println("    <table> ");
		out.println("        <tr><td><b>filename   </b></td><td><input type='text' size='64'  name='filename'    value='" + filename     + "'></td> </tr> ");
		out.println("        <tr><td><b>startDate  </b></td><td><input type='datetime-local'  name='startDate'   value='" + startDateStr + "'></td> </tr> ");
		out.println("        <tr><td><b>endDate    </b></td><td><input type='datetime-local'  name='endDate'     value='" + endDateStr   + "'></td> </tr> ");
		out.println("        <tr><td><b>minDuration</b></td><td><input type='number'          name='minDuration' value='" + minDuration  + "'></td> </tr> ");
		out.println("    </table> ");
		out.println("    <br> ");
		out.println("    <input type='submit' value='Refresh with above values'> ");
		out.println("</form> ");

//		out.println("<div id='graphs' class='grid' >"); 
		out.println("<div id='graphs'>"); 
		out.println("    <div id='div-osLoadAvg1m'        > <canvas id='chart-osLoadAvg1m'        ></canvas> </div>");
		out.println("    <div id='div-h2ReadsPerSec'      > <canvas id='chart-h2ReadsPerSec'      ></canvas> </div>");
		out.println("    <div id='div-h2WritesPerSec'     > <canvas id='chart-h2WritesPerSec'     ></canvas> </div>");
		out.println("    <div id='div-h2PageCountPerSec'  > <canvas id='chart-h2PageCountPerSec'  ></canvas> </div>");
		out.println("    <div id='div-h2FileSizeGrowKbSec'> <canvas id='chart-h2FileSizeGrowKbSec'></canvas> </div>");
		out.println("    <div id='div-h2FileSizeGrowMbSec'> <canvas id='chart-h2FileSizeGrowMbSec'></canvas> </div>");
		out.println("    <div id='div-h2FileSizeKb'       > <canvas id='chart-h2FileSizeKb'       ></canvas> </div>");
		out.println("    <div id='div-h2FileSizeMb'       > <canvas id='chart-h2FileSizeMb'       ></canvas> </div>");
		out.println("</div>"); 

		out.println("</div>");

		try
		{
			Timestamp startDateTs = StringUtil.isNullOrBlank(startDateStr) ? null : TimeUtils.parseToTimestamp(startDateStr, "yyyy-MM-dd'T'HH:mm");
			Timestamp endDateTs   = StringUtil.isNullOrBlank(endDateStr  ) ? null : TimeUtils.parseToTimestamp(endDateStr,   "yyyy-MM-dd'T'HH:mm");

			// get Data
			List<StatEntry> list = H2WriterStat.parseStatFromLogFile(filename, startDateTs, endDateTs, minDuration);

//System.out.println("statEntryListSize=" + list.size());
			
			createChart(out, "osLoadAvg1m",         "Adjusted OS Load Average 1 Minute (adjusted = load/cores)", list);
			createChart(out, "h2ReadsPerSec",       "H2 Reads Per Second (info.FILE_READ)"      , list);
			createChart(out, "h2WritesPerSec",      "H2 Writes Per Second (info.FILE_WRITE)"    , list);
			createChart(out, "h2PageCountPerSec",   "H2 Page Count Per Second (info.PAGE_COUNT)", list);
			createChart(out, "h2FileSizeGrowKbSec", "H2 File Size Grow in KB Per Second"        , list);
			createChart(out, "h2FileSizeGrowMbSec", "H2 File Size Grow in MB Per Second"        , list);
			createChart(out, "h2FileSizeKb",        "H2 File Size in KB"                        , list);
			createChart(out, "h2FileSizeMb",        "H2 File Size in MB"                        , list);
		}
		catch (Exception e)
		{
			_logger.info("Problem accessing DBMS or writing JSON, Caught: "+e, e);
			throw new ServletException("Problem accessing db or writing JSON, Caught: "+e, e);
		}
		

		// Write some JavaScript code
		out.println(HtmlStatic.getJavaScriptAtEnd(true));
		
		out.println("</body>");
		out.println("</html>");
		out.flush();
		out.close();		
	}
	
	private void createChart(ServletOutputStream out, String name, String title, List<StatEntry> list)
	throws IOException
	{
		out.println("<script>");
		
		// JavaScript variable: data_passedName
		out.println("var data_" + name + " = [");
		int count=0;
		for (StatEntry se : list)
		{
			Double val = 0.0;
			if ("osLoadAvg1m"        .equals(name)) val = se.OsLoadAvgAdj_1m;
			if ("h2ReadsPerSec"      .equals(name)) val = se.FILE_READ_rate;
			if ("h2WritesPerSec"     .equals(name)) val = se.FILE_WRITE_rate;
			if ("h2PageCountPerSec"  .equals(name)) val = se.PAGE_COUNT_rate;
			if ("h2FileSizeGrowKbSec".equals(name)) val = se.H2_FILE_SIZE_KB_rate;
			if ("h2FileSizeGrowMbSec".equals(name)) val = se.H2_FILE_SIZE_MB_rate;
			if ("h2FileSizeKb"       .equals(name)) val = ((Number)se.H2_FILE_SIZE_KB_abs).doubleValue();
			if ("h2FileSizeMb"       .equals(name)) val = ((Number)se.H2_FILE_SIZE_MB_abs).doubleValue();

			//                   if we should have , as prefix?    {"ts":"2019-01-01 12:13:14.123", "val":1.1}
			//                   if we should have , as prefix?    {"x":"2019-01-01 12:13:14.123", "y":1.1}
			//                   ------------------------------    -------------------------------------------------------
//			out.println("    " + ( count++ == 0 ? "" : ",")      + "{\"ts\":\"" + se.logTs + "\", \"val\":" + val + "}");
			out.println("    " + ( count++ == 0 ? "" : ",")      + "{\"x\":\"" + se.logTs + "\", \"y\":" + val + "}");
//			out.println("    " + ( count++ == 0 ? "" : ",")      + "{\"t\":\"" + se.logTs + "\", \"y\":" + val + "}");
		}
		out.println("];");

//		out.println("var map_data_" + name + " = data_" + name + ".map(function(item) { ");
//		out.println("    return {x: new moment(item['ts']), y: item['val']};");
//		out.println("});");

		
		// JavaScript variable: config_passedName
		out.println("var config_" + name + " = {");
		out.println("    type: 'line',                                                                                                                                                                ");
		out.println("    data: {                                                                                                                                                                ");
//		out.println("        labels: [],                                                                                                                                                                ");
		out.println("        datasets: [{                                                                                                                                                                ");
		out.println("            label: '" + name + "',                                                                                                                                                            ");
		out.println("            borderColor: 'blue',                                                                                                                                        ");
		out.println("            data: data_" + name + ",                                                                                                                                                            ");
//		out.println("            data: map_data_" + name + ",                                                                                                                                                            ");
		out.println("            type: 'line',                                                                                                                                                            ");
		out.println("            pointRadius: 0,                                                                                                                                                            ");
		out.println("            fill: false,                                                                                                                                                            ");
		out.println("            lineTension: 0,                                                                                                                                                            ");
		out.println("            borderWidth: 2                                                                                                                                                            ");
		out.println("        }]                                                                                                                                                                ");
		out.println("    },                                                                                                                                                                        ");
		out.println("    options: {                                                                                                                                                                ");
		out.println("        responsive: true,                                                                                                                                                                ");
		out.println("        maintainAspectRatio: false,                                                                                                                                                                ");
		out.println("        title: {                                                                                                                                                                ");
		out.println("            display: true,                                                                                                                                                                ");
		out.println("            fontSize: 16,                                                                                                                                                                ");
		out.println("            text: '" + title + "'                                                                                                                                                                ");
		out.println("        },                                                                                                                                                                ");
		out.println("        legend: {                                                                                                                                                                ");
		out.println("            position: 'bottom',                                                                                                                                                                ");
		out.println("            labels: {                                                                                                                                                                ");
		out.println("                boxWidth: 10, // width of coloured box                                                                                                                                                                ");
		out.println("                fontSize: 10, // font size of text, default=12                                                                                                                                                                ");
		out.println("            },                                                                                                                                                                ");
		out.println("        },                                                                                                                                                                ");
		out.println("        scales: {                                                                                                                                                                ");
		out.println("            xAxes: [{                                                                                                                                                                ");
		out.println("                type: 'time',                                                                                                                                                                ");
		out.println("                format: 'YYYY-MM-DD HH:mm:ss.SSS',                                                                                                                                                                ");
		out.println("                distribution: 'linear',                                                                                                                                                                ");
		out.println("                time: {                                                                                                                                                                ");
//		out.println("                    displayFormats: {                                                                                                                                                                ");
//		out.println("                        second:  'HH:mm:ss',                                                                                                                                                                ");
//		out.println("                        minute:  'HH:mm:ss',                                                                                                                                                                ");
//		out.println("                        hour:    'HH:mm',                                                                                                                                                                ");
//		out.println("                        day:     'MMM D',                                                                                                                                                                ");
//		out.println("                        week:    'll',                                                                                                                                                                ");
//		out.println("                        month:   'MMM YYYY',                                                                                                                                                                ");
//		out.println("                        quarter: '[Q]Q - YYYY',                                                                                                                                                                ");
//		out.println("                        year:    'YYYY',                                                                                                                                                                ");
//		out.println("                    },                                                                                                                                                                ");
//		out.println("                    unit: 'day',                                                                                                                                                                ");
//		out.println("                    displayFormats: {                                                                                                                                                                ");
//		out.println("                        hour:     'MMM D HH:mm',                                                                                                                                                                ");
//		out.println("                    },                                                                                                                                                                ");
//		out.println("                    unit: 'hour',                                                                                                                                                                ");
		out.println("                    tooltipFormat: 'YYYY-MM-DD (dddd) HH:mm:ss',                                                                                                                                                                ");
		out.println("                    displayFormats: {                                                                                                                                                                ");
		out.println("                        second:  'MMM D HH:mm',                                                                                                                                                                ");
		out.println("                        minute:  'MMM D HH:mm',                                                                                                                                                                ");
		out.println("                        hour:    'MMM D HH:mm',                                                                                                                                                                ");
		out.println("                        day:     'MMM D HH:mm',                                                                                                                                                                ");
		out.println("                        week:    'MMM D HH:mm',                                                                                                                                                                ");
		out.println("                        month:   'MMM D HH:mm',                                                                                                                                                                ");
		out.println("                        quarter: 'MMM D HH:mm',                                                                                                                                                                ");
		out.println("                        year:    'MMM D HH:mm',                                                                                                                                                                ");
		out.println("                    },                                                                                                                                                                ");
		out.println("                },                                                                                                                                                                ");
		out.println("                ticks: {                                                                                                                                                                ");
		out.println("                    beginAtZero: true,  // if true, scale will include 0 if it is not already included.                                                                                                                                                                ");
		out.println("                    source: 'auto',                                                                                                                                                                ");
		out.println("                },                                                                                                                                                                ");
		out.println("                gridLines: {                                                                                                                                                                ");
		out.println("                    color: 'rgba(0, 0, 0, 0.1)',                                                                                                                                                                ");
		out.println("                    zeroLineColor: 'rgba(0, 0, 0, 0.25)'                                                                                                                                                                ");
		out.println("                },                                                                                                                                                                ");
		out.println("            }],                                                                                                                                                                ");
		out.println("            yAxes: [{                                                                                                                                                                ");
		out.println("                ticks: {                                                                                                                                                                ");
		out.println("                    beginAtZero: true,  // if true, scale will include 0 if it is not already included.                                                                                                                                                                ");
		out.println("                },                                                                                                                                                                ");
		out.println("                gridLines: {                                                                                                                                                                ");
		out.println("                    color: 'rgba(0, 0, 0, 0.1)',                                                                                                                                                                ");
		out.println("                    zeroLineColor: 'rgba(0, 0, 0, 0.25)'                                                                                                                                                                ");
		out.println("                },                                                                                                                                                                ");
		out.println("            }],                                                                                                                                                                ");
		out.println("        },                                                                                                                                                                ");
		out.println("        zoom: {  // Container for zoom options                                                                                                                                                                ");
		out.println("            enabled: true,  // Boolean to enable zooming                                                                                                                                                                ");
		out.println("            mode: 'x', // Zooming directions. Remove the appropriate direction to disable, Eg. 'y' would only allow zooming in the y direction                                                                                                                                                                ");
		out.println("            drag: true,                                                                                                                                                                ");
		out.println("        },                                                                                                                                                                ");
		out.println("        annotation: {                                                                                                                                                                ");
		out.println("            drawTime: 'afterDraw',                                                                                                                                                                ");
		out.println("            annotations: [],                                                                                                                                                                ");
		out.println("        },                                                                                                                                                                ");
		out.println("        tooltips: {                                                                                                                                                                ");
		out.println("            callbacks: {                                                                                                                                                                ");
		out.println("                label: function (tooltipItems, data) {                                                                                                                                                                ");
		out.println("                    // print numbers localized (typically 1234567.8 -> 1,234,567.8)                                                                                                                                                                ");
		out.println("                    return data.datasets[tooltipItems.datasetIndex].label + ': ' + tooltipItems.yLabel.toLocaleString();                                                                                                                                                                ");
		out.println("                }                                                                                                                                                                ");
		out.println("            }                                                                                                                                                                ");
		out.println("        },                                                                                                                                                                ");
		out.println("    },                                                                                                                                                                ");
		out.println("    tooltips: {                                                                                                                                                                ");
		out.println("        mode: 'index',                                                                                                                                                                ");
		out.println("        intersect: false,                                                                                                                                                                ");
		out.println("    },                                                                                                                                                                ");
		out.println("    hover: {                                                                                                                                                                ");
		out.println("        mode: 'nearest',                                                                                                                                                                ");
		out.println("        intersect: true                                                                                                                                                                ");
		out.println("    }                                                                                                                                                                ");
		out.println("};");

		out.println("var ctx_" + name + " = document.getElementById('chart-" + name + "').getContext('2d'); ");
		out.println("ctx_" + name + ".canvas.width  = 1000; ");
		out.println("ctx_" + name + ".canvas.height = 300; ");
		out.println("window.chart_" + name + " = new Chart(ctx_" + name + ", config_" + name + "); "); 

		out.println("</script>");
	}
}
