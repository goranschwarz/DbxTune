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
package com.dbxtune.mgt.controllers;

import java.io.PrintWriter;
import java.io.StringWriter;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.CounterController;
import com.dbxtune.Version;
import com.dbxtune.central.controllers.DbxCentralPageTemplate;
import com.dbxtune.central.controllers.HtmlStatic.PageSection;
import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.DbUtils;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.TimeUtils;

public class SqlServerJobSchedulerActivity
extends DbxCentralPageTemplate
{
	private static final long serialVersionUID = 1L;
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public static final String PROPKEY_startTime = "SqlServerJobSchedulerActivity.startTime.default";
	public static final String DEFAULT_startTime = "-2h";

	public static final String PROPKEY_refreshTime = "SqlServerJobSchedulerActivity.refreshTime.default";
	public static final int    DEFAULT_refreshTime = 0;

	private DbxConnection _conn = null;
	

	@Override
	public String getHeadTitle()
	{
		return "SQL Server Job Scheduler Activity";
	}


	//-----------------------------------------------------------------------
	//-- Page HEAD
	//-----------------------------------------------------------------------
	@Override
	public PageSection getPageSection()
	{
		return PageSection.None;
	}
	
	@Override
	public int getHeadRefreshTime()
	{
		return getRefreshTime();
	}

	@Override
	public void craeteHtmlHeadCss(PrintWriter writer)
	{
		writer.println("<style type='text/css'>");
		writer.println();
		writer.println("    html, body { ");
		writer.println("        height: 100%; ");
		writer.println("    } ");
		writer.println();
//		writer.println("    body { ");
//		writer.println("        display: flex; ");
//		writer.println("        flex-direction: column; ");
//		writer.println("        background: red; ");
//		writer.println("    } ");
//		writer.println();
//		writer.println("    #timeline { ");
//		writer.println("        flex-grow: 1; /* Makes the timeline div expand to fill remaining space */ ");
//		writer.println("        height: 100%; /* Fallback for older browsers */ ");
//		writer.println("    } ");
		writer.println();
		writer.println("    .grow-to-bottom { ");
		writer.println("        height: 90vh; ");
		writer.println("    } ");
		writer.println();
		writer.println("</style>");
	}

	@Override
	protected List<String> getJavaScriptList()
	{
		List<String> list = new ArrayList<>();

		list.add("https://www.gstatic.com/charts/loader.js");
		list.add("/scripts/bootstrap-daterangepicker/3.1/daterangepicker.js");

		return list;
	}

	@Override
	protected List<String> getCssList()
	{
		List<String> list = new ArrayList<>();

		list.add("/scripts/bootstrap-daterangepicker/3.1/daterangepicker.css");

		return list;
	}
	
	@Override
	public String getNavbarHtmlRightSideHookin()
	{
		StringWriter out    = new StringWriter();
		PrintWriter  writer = new PrintWriter(out);

		writer.println("    <div id='dbx-report-range' class='dbx-nav-text btn btn-outline-light mr-2'>  ");
		writer.println("        <i class='glyphicon glyphicon-calendar fa fa-calendar'></i>&nbsp;  ");
		writer.println("        <span>Time Span</span> <b class='caret'></b>  ");
		writer.println("    </div>  ");
		writer.println();
		writer.println("    <div class='btn px-0 btn-outline-light'>  ");
		writer.println("        <button id='dbx-prev-day'            title='Previous DAY, same time span'    alt='Previous DAY, samt time span'    class='dbx-nav-button p-0 ml-1' type='button'><i class='glyphicon fa fa-backward'></i></button>          <!-- fa-backward fa-fast-backward          -->  ");
		writer.println("        <button id='dbx-prev-time'           title='Previous # Hours'                alt='Previous # Hours'                class='dbx-nav-button p-0'      type='button'><i class='glyphicon fa fa-arrow-circle-left'></i></button> <!-- fa-step-backward fa-arrow-circle-left -->  ");
		writer.println("        <span   id='dbx-sample-time'         title='Hours in the selected Time Span' alt='Hours in the selected Time Span' class=''>?h</span>  ");
		writer.println("        <button id='dbx-next-time'           title='Next # Hours'                    alt='Next # Hours'                    class='dbx-nav-button p-0'      type='button'><i class='glyphicon fa fa-arrow-circle-right'></i></button> <!-- fa-step-forward fa-arrow-circle-right -->  ");
		writer.println("        <button id='dbx-next-day'            title='Next DAY, same time span'        alt='Next DAY, samt time span'        class='dbx-nav-button p-0 mr-1' type='button'><i class='glyphicon fa fa-forward'></i></button>            <!-- fa-forward fa-fast-forward            -->  ");
		writer.println("    </div>  ");
		writer.println();

		return out.toString();
	}

	@Override
	public String getNavbarJavaScriptHookin()
	{
		StringWriter out    = new StringWriter();
		PrintWriter  writer = new PrintWriter(out);
		
		String urlParameterName_startTime = "startTime";
		String urlParameterName_endTime   = "endTime";

		writer.println();
		writer.println("<!-- CSS for 'Date Range' --> ");
		writer.println("<style type='text/css'> ");
		writer.println("    .dbx-nav-button { ");
		writer.println("        border: none; ");
		writer.println("        background: none; ");
		writer.println("        color: rgb(182, 187, 255) ");
		writer.println("    } ");
		writer.println()    ;
		writer.println("    .dbx-nav-text { ");
		writer.println("        color: white ");
		writer.println("    } ");
		writer.println("</style> ");
		writer.println();
		writer.println("<!-- JavaScript code for 'Date Range' --> ");
		writer.println("<script> ");
		writer.println("    // Browser loads initialize some stuff. ");
		writer.println("    window.addEventListener('load', initializeDateRangePicker());");
		writer.println();
		writer.println("    /** ");
		writer.println("     * Initialize: Date Range Picker ");
		writer.println("     */ ");
		writer.println("    function initializeDateRangePicker() ");
		writer.println("    { ");
		writer.println("        console.log('initializeDateRangePicker(): START'); ");
		writer.println();
		writer.println("        $('#dbx-report-range').daterangepicker({ ");
		writer.println("            startDate: moment().subtract(2, 'hours'), ");
		writer.println("            endDate:   moment(), ");
		writer.println("            timePicker: true, ");
		writer.println("            timePicker24Hour: true, ");
		writer.println("            timePickerIncrement: 5, ");
		writer.println("            ranges: { ");
		writer.println("                'Last 2 Hours':  [moment().subtract(2,  'hours'), moment()], ");
		writer.println("                'Last 4 Hours':  [moment().subtract(4,  'hours'), moment()], ");
		writer.println("                'Last 6 Hours':  [moment().subtract(6,  'hours'), moment()], ");
		writer.println("                'Last 8 Hours':  [moment().subtract(8,  'hours'), moment()], ");
		writer.println("                'Last 12 Hours': [moment().subtract(12, 'hours'), moment()], ");
		writer.println("                'Last 24 Hours': [moment().subtract(24, 'hours'), moment()], ");
		writer.println("                'Today':         [moment().startOf('day'), moment().endOf('day')], ");
		writer.println("                'Yesterday':     [moment().startOf('day').subtract(1,  'days'), moment().endOf('day').subtract(1, 'days')], ");
		writer.println("                'This Week':     [moment().startOf('isoWeek'), moment().endOf('isoWeek')], ");
		writer.println("                'Last 7 Days':   [moment().startOf('day').subtract(7,  'days'), moment().endOf('day')], ");
		writer.println("                'Last 2 Weeks':  [moment().startOf('day').subtract(14, 'days'), moment().endOf('day')], ");
		writer.println("                'Last 4 Weeks':  [moment().startOf('day').subtract(28, 'days'), moment().endOf('day')], ");
		writer.println("                'Last 30 Days':  [moment().startOf('day').subtract(30, 'days'), moment().endOf('day')], ");
		writer.println("                'This Month':    [moment().startOf('month'), moment().endOf('month')], ");
		writer.println("                'Last Month':    [moment().subtract(1, 'month').startOf('month'), moment().subtract(1, 'month').endOf('month')] ");
		writer.println("            } ");
		writer.println("        }); ");
		writer.println();       
		writer.println("        // On daterangepicker-APPLY ");
		writer.println("        $('#dbx-report-range').on('apply.daterangepicker', function(ev, picker) { ");
		writer.println("            dateRangePickerCb(picker.startDate, picker.endDate, picker.chosenLabel); ");
		writer.println("        }); ");
		writer.println();       
		writer.println("        // On daterangepicker-OPEN, set some start/end date ");
		writer.println("        $('#dbx-report-range').on('show.daterangepicker', function(ev, picker) { ");
		writer.println("            var startEndDate = $('#dbx-report-range span').html().split(' - '); ");
		writer.println("            if (startEndDate.length === 2) ");
		writer.println("            { ");
		writer.println("                console.log('on #dbx-report-range: show(): setting: startDate=|' + startEndDate[0] + '|, endDate=|' + startEndDate[1] + '|.'); ");
		writer.println("                picker.startDate = parseDate(startEndDate[0]); ");
		writer.println("                picker.endDate   = parseDate(startEndDate[1]); ");
		writer.println("            } ");
		writer.println("        }); ");
		writer.println();       
		writer.println("        /** ");
		writer.println("         * Initialize: PREV/NEXT Time ");
		writer.println("         */ ");
		writer.println("        // Install functions for buttons: dbx-prev-time, dbx-next-time ");
		writer.println("        $('#dbx-prev-day').click( function() { ");
		writer.println("            setNextPrevDay('prev'); ");
		writer.println("        }); ");
		writer.println("        $('#dbx-prev-time').click( function() { ");
		writer.println("            setNextPrevTime('prev'); ");
		writer.println("        }); ");
		writer.println("        $('#dbx-next-time').click( function() { ");
		writer.println("            setNextPrevTime('next'); ");
		writer.println("        }); ");
		writer.println("        $('#dbx-next-day').click( function() { ");
		writer.println("            setNextPrevDay('next'); ");
		writer.println("        }); ");
		writer.println();
		writer.println("    } ");
		writer.println();
		writer.println("    function dateRangePickerCb(start, end, label) ");
		writer.println("    { ");
		writer.println("        console.log('dateRangePickerCb CALLBACK: start=' + start + ', end=' + end + ', label=' + label); ");
		writer.println();
		writer.println("        if (label === 'Last 2 Hours' || label === 'Last 4 Hours' || label === 'Last 6 Hours' || label === 'Last 8 Hours' || label === 'Last 12 Hours' || label === 'Last 24 Hours' || label === 'Today') ");
		writer.println("        { ");
		writer.println("            const url = new URL(window.location.href); ");
		writer.println("            var   startTime = '2h'; ");
		writer.println();
		writer.println("            if      (label === 'Last 2 Hours')  startTime = '2h'; ");
		writer.println("            else if (label === 'Last 4 Hours')  startTime = '4h'; ");
		writer.println("            else if (label === 'Last 6 Hours')  startTime = '6h'; ");
		writer.println("            else if (label === 'Last 8 Hours')  startTime = '8h'; ");
		writer.println("            else if (label === 'Last 12 Hours') startTime = '12h'; ");
		writer.println("            else if (label === 'Last 24 Hours') startTime = '24h'; ");
		writer.println("            else if (label === 'Today')         startTime = start.format('YYYY-MM-DD HH:mm'); ");
		writer.println();
		writer.println("            url.searchParams.set('"    + urlParameterName_startTime + "', startTime); ");
		writer.println("            url.searchParams.delete('" + urlParameterName_endTime   + "'); ");
		writer.println();
		writer.println("            console.log('window.location.ref: ' + url); ");
		writer.println("            window.location.assign(url); ");
		writer.println("        } ");
		writer.println("        else ");
		writer.println("        { ");
		writer.println("            const url = new URL(window.location.href); ");
		writer.println("            var   startTime = start.format('YYYY-MM-DD HH:mm'); ");
		writer.println("            var   endTime   = end  .format('YYYY-MM-DD HH:mm'); ");
		writer.println();
		writer.println("            url.searchParams.set('" + urlParameterName_startTime + "', startTime); ");
		writer.println("            url.searchParams.set('" + urlParameterName_endTime   + "', endTime); ");
		writer.println();
		writer.println("            console.log('window.location.ref: ' + url); ");
		writer.println("            window.location.assign(url); ");
		writer.println("        } ");
		writer.println();
		writer.println("        // Set the Time LABEL ");
		writer.println("        setDateRangePickerLabel(start, end); ");
		writer.println("    } ");
		writer.println();
		writer.println("    function setDateRangePickerLabel(start, end) ");
		writer.println("    { ");
		writer.println("        start = moment(start); ");
		writer.println("        end   = moment(end); ");
		writer.println("        if (moment(start).isSame(moment(), 'day') && moment(end).isSame(moment(), 'day')) ");
		writer.println("            $('#dbx-report-range span').html(start.format('HH:mm') + ' - ' + end.format('HH:mm')); ");
		writer.println("        else ");
		writer.println("            $('#dbx-report-range span').html(start.format('YYYY-MM-DD HH:mm') + ' - ' + end.format('YYYY-MM-DD HH:mm')); ");
		writer.println();
		writer.println("        // Calculate days/hours ");
		writer.println("        let timeDiffHours = Math.round(end.diff(start, 'hours', true));  // Round to nearest hour ");
//		writer.println("        let timeDiffHours = end.diff(start, 'hours'); ");
		writer.println("        if (timeDiffHours <= 0) ");
		writer.println("            timeDiffHours = 1; ");
		writer.println("        let timeDiffstr = timeDiffHours + 'h'; ");
		writer.println("        if (timeDiffHours > 24) ");
		writer.println("        	  timeDiffstr = parseInt(timeDiffHours / 24) + 'd ' + parseInt(timeDiffHours % 24) + 'h'; ");
		writer.println("        $('#dbx-sample-time').html(timeDiffstr); ");
		writer.println("    } ");
		writer.println();
		writer.println("    function parseDate(dateString) ");
		writer.println("    { ");
		writer.println("        var longRegEx = /^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}/; ");
		writer.println("        if (dateString.match(longRegEx) != null) ");
		writer.println("            return moment(dateString); ");
		writer.println();
		writer.println("        var shortRegEx = /^\\d{2}:\\d{2}/; ");
		writer.println("        if (dateString.match(shortRegEx) != null) ");
		writer.println("            return moment(dateString, 'HH:mm'); ");
		writer.println();
		writer.println("        var msg = 'parseDat(): unknown date |' + dateString + '| was passed. do not match |YYYY-MM-DD HH:mm| or |HH:mm|'; ");
		writer.println("        console.log(msg); ");
		writer.println("        return msg; ");
		writer.println("    } ");
		writer.println();
		writer.println("    function setNextPrevTime(type) ");
		writer.println("    { ");
		writer.println("        let dayHourStr = $('#dbx-sample-time').html(); ");
		writer.println("        let currentHourSpan = 1; ");
		writer.println("        if (dayHourStr.indexOf('d ') != -1) { ");
		writer.println("            let tmpArr = dayHourStr.replace('h', '').split('d '); ");
		writer.println("            currentHourSpan = parseInt(tmpArr[0]*24) + parseInt(tmpArr[1]); ");
		writer.println("        } else { ");
		writer.println("            currentHourSpan = parseInt(dayHourStr.replace('h', '')); ");
		writer.println("        } ");
		writer.println("        if (isNaN(currentHourSpan)) ");
		writer.println("            return; ");
		writer.println("        if (currentHourSpan === 0) ");
		writer.println("            currentHourSpan = 1; ");
		writer.println();
		writer.println("        var startEndDate = $('#dbx-report-range span').html().split(' - '); ");
		writer.println("        if (startEndDate.length === 2) ");
		writer.println("        { ");
		writer.println("            console.log('button: ' + type + '-time: currentHourSpan=|' + currentHourSpan + '|, startDate=|' + startEndDate[0] + '|, endDate=|' + startEndDate[1] + '|.'); ");
		writer.println("            var startDate = parseDate(startEndDate[0]); ");
		writer.println("            var endDate   = parseDate(startEndDate[1]); ");
		writer.println();
		writer.println("            if (type === 'prev') { ");
		writer.println("                startDate = startDate.subtract(currentHourSpan, 'hours'); ");
		writer.println("                endDate   = endDate  .subtract(currentHourSpan, 'hours'); ");
		writer.println("            } ");
		writer.println("            else if (type === 'next') { ");
		writer.println("                startDate = startDate.add(currentHourSpan, 'hours'); ");
		writer.println("                endDate   = endDate  .add(currentHourSpan, 'hours'); ");
		writer.println("            } ");
		writer.println("            console.log('button: ' + type + '-time: AFTER adjust: currentHourSpan=|' + currentHourSpan + '|, startDate=|' + startDate.format('YYYY-MM-DD HH:mm') + '|, endDate=|' + endDate.format('YYYY-MM-DD HH:mm') + '|.'); ");
		writer.println();
		writer.println("            // USE: the daterangepicker callback to set START/END time ");
		writer.println("            dateRangePickerCb(startDate, endDate, type); ");
		writer.println("        } ");
		writer.println("    } ");
		writer.println();
		writer.println("    function setNextPrevDay(type) ");
		writer.println("    { ");
		writer.println("        var startEndDate = $('#dbx-report-range span').html().split(' - '); ");
		writer.println("        if (startEndDate.length === 2) ");
		writer.println("        { ");
		writer.println("            var startDate = parseDate(startEndDate[0]); ");
		writer.println("            var endDate   = parseDate(startEndDate[1]); ");
		writer.println();
		writer.println("            if (type === 'prev') { ");
		writer.println("                startDate = startDate.subtract(1, 'days'); ");
		writer.println("                endDate   = endDate  .subtract(1, 'days'); ");
		writer.println("            } ");
		writer.println("            else if (type === 'next') { ");
		writer.println("                startDate = startDate.add(1, 'days'); ");
		writer.println("                endDate   = endDate  .add(1, 'days'); ");
		writer.println("            } ");
		writer.println("            console.log('button: ' + type + '-time: AFTER adjust: startDate=|' + startDate.format('YYYY-MM-DD HH:mm') + '|, endDate=|' + endDate.format('YYYY-MM-DD HH:mm') + '|.'); ");
		writer.println();
		writer.println("            // USE: the daterangepicker callback to set START/END time ");
		writer.println("            dateRangePickerCb(startDate, endDate, type); ");
		writer.println("        } ");
		writer.println("    } ");
		writer.println();
		writer.println("</script> ");
		writer.println();

		return out.toString();
	}
	
	
	//-----------------------------------------------------------------------
	//-- Page BODY
	//-----------------------------------------------------------------------
	@Override
	public String getBodyAttributes()
	{
		return "onload='updateLastUpdatedClock()'";
	}

	@Override
	public void createHtmlBodyJavaScriptTop(PrintWriter writer)
	{
		writer.println("<script>");
		writer.println("function updateLastUpdatedClock() {                   ");
		writer.println("    var ageInSec = Math.floor((new Date() - lastUpdateTime) / 1000);");
		writer.println("    document.getElementById('last-update-ts').innerHTML = ageInSec + ' seconds ago'; ");
//		writer.println("    console.log('updateLastUpdatedClock(): ' + document.getElementById('last-update-ts'));");
//		writer.println("    console.log('updateLastUpdatedClock(): ' + ageInSec + ' seconds ago');");
		writer.println("    setTimeout(updateLastUpdatedClock, 1000); ");
		writer.println("}                                                     ");
		writer.println("var lastUpdateTime = new Date();");
		writer.println("</script>");
	}

	@Override
	public void createHtmlBodyContent(PrintWriter writer)
	{
		writer.println("<div class='container-fluid'>");

//		String ver = "Version: " + Version.getVersionStr() + ", Build: " + Version.getBuildStr();
//		writer.println("<h1>DbxTune - Central - " + username + "@" + hostname + "</h1>");
//		writer.println("<div class='topright'>"+ver+"</div>");

		// Create a "drop down section" where we will have
		// - When was the page loaded
		// - And various User Defined Information content
		writer.println();
		writer.println("<details>");
		writer.println("    <summary>");
		writer.println("Show Parameters, Page loaded: <span id='last-update-ts'>" + (new Timestamp(System.currentTimeMillis())) + "</span>, ");
		if (getRefreshTime() > 0)
			writer.println("This page will 'auto-refresh' every " + getRefreshTime() + " second. " );
		else
			writer.println("To 'auto-refresh' use '&refresh=##'. " );

		if (getUrlParameterBoolean("onlyLevelZero", false))
			writer.println("Show <a href='#' onClick=\"reloadCurrentUrlWithParam('onlyLevelZero', 'false')\">all job steps</a>");
		else
			writer.println("Show <a href='#' onClick=\"reloadCurrentUrlWithParam('onlyLevelZero', 'true')\">only FULL jobs</a>");

		writer.println("    </summary>");
		writer.println(getParameterDescriptionHtmlTable());
		// Another collapsable section (IN the summary) to show SQL Statement
		writer.println();
		writer.println("    <details>");
		writer.println("          <summary>");
		writer.println("              <b>Executed SQL Statement:</b><br>");
		writer.println("          </summary>");
		writer.println("          <div id='executed_sql'>");
		writer.println("          <pre>");
		writer.println(StringEscapeUtils.escapeHtml4(getSql()));
		writer.println("          </pre>");
		writer.println("          </div>");
		writer.println("    </details>");
		writer.println();
		writer.println("</details>");
		writer.println();

		writer.println("<div id='ud-content'>");
		writer.println("<!-- CONTENT - START -->");

		writer.println();
		writer.println("<!-- No data -->");
		writer.println("<div id='dbx-job-scheduler-no-data' style='color: orange; display: none;'>No data was found for period</div> ");
		writer.println();
		writer.println("<!-- On Exception (in the Java Servlet), Show div -->");
		writer.println("<div id='dbx-servlet-exception' style='color: red; display: none;'>This should only be visible on Exceptions in the servlet </div> ");
		writer.println();
		writer.println("<!-- The timeline content will be displaied in the below div 'timeline' -->");
		writer.println("<div id='timeline' class='grow-to-bottom'></div>");
		writer.println();
		
		writer.println("<!-- CONTENT - END -->");
		writer.println("<!-- END: ud-content -->");
		writer.println("</div>"); // ud-content
		
		writer.println("<!-- END: container-fluid -->");
		writer.println("</div>");

		writer.println();
		writer.println("<!-- Some java script --> ");
		writer.println("<script> ");
		writer.println("    function reloadCurrentUrlWithParam(key, value) ");
		writer.println("    { ");
		writer.println("        // Modify the URL ");
		writer.println("        const url = new URL(window.location.href); ");
		writer.println("        url.searchParams.set(key, value); ");
		writer.println();
		writer.println("        // Reload URL ");
		writer.println("        window.location.href = url.toString(); ");
		writer.println("    } ");
		writer.println("</script> ");
		try
		{
			// Everything happens here...
			createJsTimeline(writer);
		}
		catch (Exception ex)
		{
			_logger.error("Problems creating HTML Timeline content.", ex);
//			writer.println("Problems creating HTML Timeline content: Caught: " + ex);

			writer.println("");
			writer.println("<!-- Run this code if the servlet had exceptions -->");
			writer.println("<script>");
			write(writer, "    document.getElementById('dbx-servlet-exception').style.display = 'block'; ");
			write(writer, "    let tmpText  = 'Problems creating HTML Timeline content: Caught:<br>'; ");
			write(writer, "        tmpText += '<code><pre>'; ");
			write(writer, "        tmpText += '" + StringEscapeUtils.escapeHtml4(escapeJsQuote(ex.toString())).replace("\r", "").replace("\n", "<BR>") + "'; ");
			write(writer, "        tmpText += '</pre></code> '; ");
			write(writer, "    document.getElementById('dbx-servlet-exception').innerHTML = tmpText; ");
			writer.println("</script>");
			writer.println("");
		}

		writer.println();
		writer.println("    <!-- ######################################################## --> ");
		writer.println("    <!-- ## MODAL: 'dbx-view-jobIdDetails-dialog' --> ");
		writer.println("    <!-- ######################################################## --> ");
		writer.println("    <div class='modal fade' id='dbx-view-jobIdDetails-dialog'> ");
		writer.println("        <div class='modal-dialog' style='max-width: 80%;'> ");
		writer.println("            <div class='modal-content'> ");
		writer.println();
		writer.println("                <!-- Modal Header --> ");
		writer.println("                <div class='modal-header'> ");
		writer.println("                    <h5 class='modal-title' id='dbx-view-jobIdDetails-title'>Job Details</h5> ");
		writer.println("                    <button type='button' class='close' data-dismiss='modal' aria-label='Close'><span aria-hidden='true'>&times;</span></button> ");
		writer.println("                </div> ");
		writer.println();
		writer.println("                <!-- Modal body --> ");
		writer.println("                <div class='modal-body' style='height: 80vh;'> ");
		writer.println("                    <div id='timeline-modal' style='height: 100%;'></div> ");
		writer.println("                </div> ");
		writer.println();
		writer.println("                <!-- Modal footer --> ");
		writer.println("                <div class='modal-footer'> ");
		writer.println("                  <button type='button' class='btn btn-secondary' data-dismiss='modal'>Close</button> ");
		writer.println("                </div> ");
		writer.println();
		writer.println("            </div> ");
		writer.println("        </div> ");
		writer.println("    </div> ");
		writer.println();
		writer.println("<script> ");
		writer.println("    // Do stuff AFTER the modal has been opened ");
		writer.println("    $(document).on('shown.bs.modal', '#dbx-view-jobIdDetails-dialog', function () { ");
		writer.println("        console.log('OPEN MODAL[SHOW-N]: dbx-view-jobIdDetails-dialog'); ");
		writer.println("        drawChartModal();  // Redraw the chart AFTRE Opened modal, to ensure correct size...");
		writer.println("    });");
		writer.println("</script> ");
	}

	//-----------------------------------------------------------------------
	//-- Parameters
	//-----------------------------------------------------------------------
	
	public int getRefreshTime()
	{
		return getUrlParameterInt_defaultFromDesc("refresh");
	}

	private int getDefaultRefreshTime()
	{
		return Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_refreshTime, DEFAULT_refreshTime);
	}

	@Override
	public Set<UrlParameterDescription> createUrlParameterDescription()
	{
		LinkedHashSet<UrlParameterDescription> set = new LinkedHashSet<>();
	
		set.add(new UrlParameterDescription(
				"jobName",
				"Name of the job we want to show.<br>"
						+ "<br>"
						+ "Note: If parameter <code>jobId</code> is passed, this will <b>NOT</b> be used <i>(jobId overrides jobName)</i>.<br>"
						+ "<b>NO Default</b>, which means all the jobs within the start/end time",
				null,
				String.class
				));

		set.add(new UrlParameterDescription(
				"jobId",
				"Job ID that we want to show.<br>"
						+ "<br>"
						+ "Note: Overrides parameter <code>jobName</code><br>"
						+ "<b>NO Default</b>, which means all the jobs within the start/end time", 
				null, 
				String.class));

		set.add(new UrlParameterDescription(
				"startTime",
				"What is the 'startTime' we want to get data for. <br>"
						+ "<br>"
						+ "Example: <code>TODAY           </code> (set StartTime to this day at 00:00:00) <br>"
						+ "Example: <code>-4h             </code> (set StartTime to 'now' -4 hours) <br>"
						+ "Example: <code>-2d             </code> (set StartTime to 'now' -2 days) <br>"
						+ "Example: <code>2024-03-08 18:00</code> (set startTime to a absolute timestamp) <br>"
						+ "<b>Default</b>: <code>-2h      </code> (last 2 hours)", 
				Configuration.getCombinedConfiguration().getProperty(PROPKEY_startTime, DEFAULT_startTime), // "-2h"
				Timestamp.class));

		set.add(new UrlParameterDescription(
				"endTime",
				"What is the end time <br>"
						+ "<br>"
						+ "Example: <code>2024-03-08 22:00</code> (set endTime to a absolute timestamp) <br>"
						+ "Example: <code>4h              </code> (set endTime to 4 hours after the 'startTime') <br>"
						+ "Example: <code>NOW             </code> (set endTime to current time) <br>"
						+ "<b>Default</b>: <i>none</i><br>", 
                null, 
                Timestamp.class));

		set.add(new UrlParameterDescription(
				"refresh",
				"Auto refresh the page after this amount of seconds.<br>"
						+ "<br>"
						+ "Example: <code>60</code> (every 60 seconds) <br>"
						+ "Example: <code>0 </code> (turn OFF auto refresh, just press F5 instead) <br>"
						+ "<b>Default</b>: <code>" +  getDefaultRefreshTime() + "</code>", 
				getDefaultRefreshTime(), 
                Integer.class));

		set.add(new UrlParameterDescription(
				"onlyLevelZero",
				"Only show Level Zero, this to get a high level overview of the executed work.<br>"
						+ "<br>"
						+ "<b>Default</b>: <code>false</code>",
                false,
                Boolean.class));		

		set.add(new UrlParameterDescription(
				"showTimeInBars",
				"Show JobStep time in HH:MM:SS (hour:minute:second) at the end of each JobStep.<br>"
						+ "<br>"
						+ "<b>Default</b>: <code>true</code>",
                true,
                Boolean.class));		
		
		set.add(new UrlParameterDescription(
				"showKeys",
				"Show the chart 'keys' at the left side of the chart<br>"
						+ "A chart key is what groups the various rows together, also see <code>keyTransform</code><br>"
						+ "<br>"
						+ "<b>Default</b>: <code>false</code>", 
                false, 
                Boolean.class));

		set.add(new UrlParameterDescription(
				"keyTransform",
				"If we want to 'flatten' the chart in some way, we can use this to replace some part of the key (This is a regular expression)<br>"
						+ "This can be used if you have a 'parent' job, which schedules 'sub jobs', and you have 'sub' jobs on individual rows...<br>"
						+ "But, you want to 'collapse' the 'sub' records into the 'same' row as the 'parent', then you can use this to 'rewrite' the 'key' to be the same for all records that should be presented on the same row in the chart.<br>"
						+ "<br>"
						+ "Example: <code>\\[\\d+\\]</code> (remove any any strings that has numbers within square brackets, for example: <code>[000]</code>) <br>"
						+ "<b>Note</b>:    The URL will be <code>&keyTransform=\\[\\d%2B\\]</code> where '%2B' is the '+' (plus char) in escaped form, for a HTTP Query String<br>"
						+ "<b>Hint</b>:    To test your regular expressions you can use: <a href='https://www.regexpal.com/' target='_blank'>https://www.regexpal.com/</a> <br>"
						+ "<b>Default</b>: <i>none</i> ", 
                null, 
                String.class));

		set.add(new UrlParameterDescription(
				"minDurationInSeconds",
				"If we want to 'skip' items in the chart which has a 'duration' of less than ## seconds<br>"
						+ "<b>Default</b>: <code>0</code> (if not restricted by the executed SQL Statement",
                10,
                Integer.class));

		set.add(new UrlParameterDescription(
				"keepNames",
				"Only keep records in column 'BarText' that matches this regular expression <br>"
						+ "This can for example be used if you want to skip some 'sub' jobs, or other known jobs you do not want to see <br>"
						+ "<br>"
						+ "Example: <code>.*\\[0\\].*</code> (keep any rows that contains '[0]') <br>"
						+ "<b>Hint</b>:    To test your regular expressions you can use: <a href='https://www.regexpal.com/' target='_blank'>https://www.regexpal.com/</a> <br>"
						+ "<b>Default</b>: <i>none</i> ", 
                null, 
                String.class));

		set.add(new UrlParameterDescription(
				"skipNames",
				"Remove records in column 'BarText' that matches this regular expression<br>"
						+ "Much like 'keepNames', but here we can say that all names with 'Maintenence' or similar will be skipped. <br>"
						+ "<br>"
						+ "Example: <code>.*Maintenance.*</code> (remove any rows that contains 'Maintenance') <br>"
						+ "<b>Hint</b>:    To test your regular expressions you can use: <a href='https://www.regexpal.com/' target='_blank'>https://www.regexpal.com/</a> <br>"
						+ "<b>Default</b>: <i>none</i> ", 
                null, 
                String.class));

		set.add(new UrlParameterDescription(
				"fillEnd",
				"Fill the end of the chart with 'No Activity' label. Usefull if we are looking at the 'tail', so we can se time that has passed without any activity <br>"
						+ "<br>"
						+ "<b>Note</b>:    This will be disabled if the <code>endTime</code> is specified.<br>"
						+ "<b>Default</b>: <code>true</code>",
                true,
                Boolean.class));

		set.add(new UrlParameterDescription(
				"generateDummyRows",
				"Just for demo purposes. Generate some dummy records...<br>"
						+ "<br>"
						+ "Example: <code>15</code> (Generate 15 dummy rows, like 'Dummy row #') <br>"
						+ "Example: <code>20:aaa {id} bbb</code> (Generate 15 dummy rows, with the label 'aaa # bbb', where # is the row number) <br>"
						+ "<b>Default</b>: <i>none</i>",
                null,
                String.class));

		set.add(new UrlParameterDescription(
				"useDefaultTooltip",
				"Fallback to use the components default tooltip instead of the 'enhanced' one.<br>"
						+ "<br>"
						+ "<b>Default</b>: <code>false</code>",
                false,
                Boolean.class));

		return set;
	}
	
	@Override
	public void checkUrlParameters() 
	throws Exception
	{
		String startTimeStr = getUrlParameter_defaultFromDesc("startTime");
		String endTimeStr   = getUrlParameter_defaultFromDesc("endTime");
		
		Timestamp startTs = null;
		Timestamp endTs   = null;

		// Is the 'startTime' in hours or days
		int startTimeHourAdjust = 1;
		if ( StringUtil.hasValue(startTimeStr) )
		{
			if (startTimeStr.equalsIgnoreCase("TODAY"))
			{
				// Get TODAY as 'yyyy-MM-dd'
				startTimeStr = TimeUtils.toStringYmd(System.currentTimeMillis()); // add ONE minute
			}
			else if (startTimeStr.toUpperCase().endsWith("H"))
			{
				startTimeStr = startTimeStr.substring(0, startTimeStr.length()-1);
				startTimeHourAdjust = 1;
			}
			else if (startTimeStr.endsWith("D"))
			{
				startTimeStr = startTimeStr.substring(0, startTimeStr.length()-1);
				startTimeHourAdjust = 24;
			}
		}

		// Is the 'endTime' in hours or days
		int endTimeHourAdjust = 1;
		if ( StringUtil.hasValue(endTimeStr) )
		{
			if (endTimeStr.equalsIgnoreCase("NOW"))
			{
				// Get TODAY as 'yyyy-MM-dd HH:MM' 
				endTimeStr = TimeUtils.toStringYmdHm(System.currentTimeMillis() + 60*1000); // add ONE minute
			}
			else if (endTimeStr.toUpperCase().endsWith("H"))
			{
				endTimeStr = endTimeStr.substring(0, endTimeStr.length()-1);
				endTimeHourAdjust = 1;
			}
			else if (endTimeStr.endsWith("D"))
			{
				endTimeStr = endTimeStr.substring(0, endTimeStr.length()-1);
				endTimeHourAdjust = 24;
			}
		}

		//----------------------------------------
		// startTime: If integer -> set the time
		if ( StringUtil.isInteger(startTimeStr) )
		{
			int intPeriod = Math.abs(StringUtil.parseInt(startTimeStr, 2));

			startTs = new Timestamp(System.currentTimeMillis() - (intPeriod * startTimeHourAdjust * 3600 * 1000));
		}
		else
		{
			// parse a ISO date with optional time parameter
			startTs = TimeUtils.parseToTimestampX(startTimeStr);
		}

		//----------------------------------------
		// endTime
		if ( StringUtil.hasValue(endTimeStr) && startTs != null)
		{
			if ( StringUtil.isInteger(endTimeStr) )
			{
				int intPeriod = Math.abs(StringUtil.parseInt(endTimeStr, 2));

				endTs = new Timestamp(startTs.getTime() + (intPeriod * endTimeHourAdjust * 3600 * 1000));
			}
			else
			{
				// parse a ISO date with optional time parameter
				endTs = TimeUtils.parseToTimestampX(endTimeStr);
			}
		}


		//----------------------
		// SET: "startTime"
		if (startTs != null)
		{
			setUrlParameter("startTime", startTs);
		}

		//----------------------
		// SET: "endTime"
		if (endTs == null)
		{
			// set endTime to "now"
			endTimeStr = TimeUtils.toStringYmdHm(System.currentTimeMillis() + 60*1000); // add ONE minute
		}
		else
		{
			endTimeStr = "" + endTs;

			// If we have PASSED a "endTime", then do NOT fill out the end of the chart to "now"
			setUrlParameter("fillEnd", false);
		}
		setUrlParameter("endTime", endTimeStr);
	}






	//-----------------------------------------------------------------------
	//-- Open/Close
	//-----------------------------------------------------------------------
//	@Override
//	public void init(Configuration conf)
//	throws Exception
//	{
//		super.init(conf);
//		
//		// Do local initializations here
//		_defaultStartTime = conf.getProperty(PROPKEY_startTime, DEFAULT_startTime);
//	}

	@Override
	protected void open()
	throws SendResponseErrorException
	{
		// Exit early if: Counter Controller
		if ( ! CounterController.hasInstance() )
		{
			throw new SendResponseErrorException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "No Counter Controller was found."); 
		}

		try
		{
			String appName = Version.getAppName() + "-JobSchedulerActivity";

			// Create a Connection to the Source DBMS (SQL Server)
			_logger.info("Connectiong to Source DBMS with appName '" + appName + "'.");
			_conn = CounterController.getInstance().cloneMonConnection(appName);
		}
		catch (Exception ex) 
		{
			String msg = "Problems Creating a Connecting to PCS or Source DBMS. Caught: " + ex;
			if (ex instanceof SQLException)
			{
				SQLException sqlEx = (SQLException) ex;
				msg = "Problems Creating a Connecting to PCS or Source DBMS. ErrorCode=" + sqlEx.getErrorCode() + ", SqlSate='" + sqlEx.getSQLState() + "', Message|" + sqlEx.getMessage() + "|. Caught: " + ex;
			}
			_logger.error(msg, ex);
			throw new SendResponseErrorException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg); 
		}
	}

	@Override
	protected void close()
	{
		if (_conn != null)
		{
			_logger.info("Closing Connection to Source SQL Server");
			_conn.closeNoThrow();
		}
	}

	private DbxConnection getConnection()
	{
		return _conn;
	}


	private String getSql()
	{
		String jobName   = getUrlParameter("jobName");
		String jobId     = getUrlParameter("jobId");
		String startTime = getUrlParameter("startTime");
		String endTime   = getUrlParameter("endTime");
		
		int minDurationInSeconds = getUrlParameterInt_defaultFromDesc("minDurationInSeconds");
		
		// ##------------------------------------------------------------
		// ## 'dbms.sql' is mandatory field
		// ## SQL Statement that produces a TIMELINE ResultSet
		// ## Column DataType  Description
		// ## ------ --------  -------------------------------------
		// ##      1 String    labelKey  (Text on the left side of the graph)
		// ##      2 String    barText   (Text on the bar within the time-line chart)
		// ##      3 String    color     (any valid HTML color that the bar will have)
		// ##      4 Timestamp beginTime (Start time for this entry)
		// ##      5 Timestamp endTime   (End time for this entry)
		// ##      All extra columns will be put in the "tool tip" section
		// ##
		// ## ${startTime} and ${endTime} is variables that will be replaced with ISO date time 'yyyy-MM-dd HH:mm:ss.SSS'
		// ##------------------------------------------------------------
		String jobNameOrId_where = "";
		
		if (StringUtil.hasValue(jobId))
		{
			jobNameOrId_where = "      AND j.job_id = " + DbUtils.safeStr(jobId) + " \n";
		}
		else if (StringUtil.hasValue(jobName))
		{
			jobNameOrId_where = "      AND j.name = " + DbUtils.safeStr(jobName) + " \n";
		}

//TODO; // Add the below "somehow" to add "historical info" for each 'job_step_id' so we can put it in the tooltip...
//      // Possibly also add "seconds diff" to current execution time
//      // Below code was 'reused' from SqlServerJobSchedulerExtractor.java, row=240
//TODO; // In the Report Section... Add "same as above" to 'step id:s' descriptions (another tooltip) so "historical info" also is possible from the report!
//TODO; // POSSIBLY: Create a (collapsable) section with ALL available jobs(and there job steps), and when the main job was last executed... 
//      //           This so we can check jobs that only runs "periodically" like once a week/month
//		String allExecTimes = "        ,allExecTimes  = 'Only available in SQL Server 2017 and above' \n";
//		DbmsVersionInfo verInfo = _conn.getDbmsVersionInfo();
//		if (verInfo != null && verInfo.getLongVersion() >= Ver.ver(2017))
//		{
//			allExecTimes = ""
//				    + "        ,allExecTimes  = STRING_AGG( \n"
//				    + "                             CAST( \n"
//				    + "                                 'ts=' + convert(varchar(30), date_executed, 120) \n"
//				    + "                               + ', wd=' + cast(datename(weekday, date_executed) as char(9)) \n"
//				    + "                               + ', HMS=' + convert(varchar(10), dateadd(second, secs_duration, '2000-01-01'), 8) \n"
//				    + "                               + ', sec=' + cast(secs_duration as varchar(20)) \n"
//				    + "                               + ', status=' + run_status_desc \n"
//				    + "                               + ';' \n"
//				    + "                               as varchar(max)) \n"
//				    + "                           ,char(10) \n"
//				    + "                         ) WITHIN GROUP (ORDER BY date_executed) \n"
//					;
//		}
		
		String sql = ""
			    + "/* NOTE: the below needs: use msdb; GRANT EXECUTE ON msdb.dbo.agent_datetime TO PUBLIC -- or: dbxtune */ \n"
			    + "DECLARE @minimum_duration_in_seconds int = " + minDurationInSeconds + " \n"
			    + ";WITH st0 AS \n"
			    + "( \n"
			    + "    /* Get \"parent\" jobs (step_id=0) that are in the history -- jobs that already has been executed */ \n"
			    + "    SELECT \n"
			    + "        job_name     = j.name \n"
			    + "       ,run_datetime = msdb.dbo.agent_datetime(h.run_date, h.run_time) \n"
			    + "       ,h.instance_id \n"
			    + "       ,h.job_id \n"
			    + "       ,h.run_date \n"
			    + "       ,h.run_time \n"
			    + "       ,query_src = '1' \n"
			    + "    FROM msdb.dbo.sysjobhistory h \n"
			    + "    INNER JOIN msdb.dbo.sysjobs j ON h.job_id = j.job_id \n"
			    + "    WHERE h.step_id = 0 \n"
			    + "      AND msdb.dbo.agent_datetime(h.run_date, h.run_time) BETWEEN '" + startTime + "' and '" + endTime + "' \n"
			    + "      /* possibly rewrite this to use 'run_date, run_time' instead of a datetime... */ \n"
			    + "      /* this so we can create/use a index on sysjobhistory(run_date, run_time)     */ \n"
			    + "      AND (h.run_duration/10000*3600 + (h.run_duration/100)%100*60 + h.run_duration%100) >= @minimum_duration_in_seconds \n"
			    + jobNameOrId_where
			    + " \n"
			    + "    UNION ALL \n"
			    + " \n"
			    + "    /* Get \"parent\" jobs that are ACTIVE -- currently executing, which is not YET part on the sysjobhistory table */ \n"
			    + "    SELECT \n"
			    + "        job_name    = j.name \n"
			    + "       ,run_date    = ja.start_execution_date \n"
			    + "       ,instance_id = (/* id of last history that HAS executed */  select top 1 jh.instance_id from msdb.dbo.sysjobhistory jh where jh.job_id = ja.job_id and jh.step_id >= 1 order by jh.run_date desc, jh.run_time desc ) \n"
			    + "       ,job_id      = ja.job_id \n"
			    + "       ,run_date    = convert(int, convert(varchar(8), ja.start_execution_date, 112))                   /* 112 = yyyymmdd */ \n"
			    + "       ,run_time    = convert(int, REPLACE(convert(varchar(8), ja.start_execution_date, 114), ':', '')) /* 114 = hh:mi:ss:mmm  ... then remove ':' */ \n"
			    + "       ,query_src = '2' \n"
			    + "    FROM msdb.dbo.sysjobactivity ja \n"
			    + "    INNER JOIN msdb.dbo.sysjobs j ON ja.job_id = j.job_id \n"
			    + "    WHERE 1=1 \n"
			    + "      AND ja.session_id = ( select max(session_id) FROM msdb.dbo.syssessions ) \n"
			    + "      AND ja.start_execution_date IS NOT NULL \n"
			    + "      AND ja.stop_execution_date  IS NULL \n"
			    + jobNameOrId_where
			    + ") \n"
			    + ",job_history AS \n"
			    + "( \n"
			    + "        SELECT \n"
			    + "               chart_key = FORMAT(st0.run_datetime, 'yyyy-MM-dd HH:mm:ss') + ' -- ' + st0.job_name + ' - step: [' + FORMAT(jh.step_id,'D3') + ']' \n"
			    + "              ,CASE \n"
			    + "                 WHEN jh.step_id = 0 \n"
			    + "                   THEN '[' + CAST(jh.step_id as varchar(9)) + '] - FULL JOB: ' + st0.job_name \n"
			    + "                 ELSE   '[' + CAST(jh.step_id as varchar(9)) + '] - ' + jh.step_name \n"
			    + "               END as bar_name \n"
			    + "           ,CASE \n"
			    + "                WHEN jh.run_status = 0                    THEN 'red'         /* Failed      */ \n"
			    + "                WHEN jh.run_status = 1 AND jh.step_id = 0 THEN 'lightblue'   /* Succeeded on BASE  */ \n"
			    + "                WHEN jh.run_status = 1 AND jh.step_id > 0 THEN 'blue'        /* Succeeded on STEPS  */ \n"
			    + "                WHEN jh.run_status = 2                    THEN 'orange'      /* Retry       */ \n"
			    + "                WHEN jh.run_status = 3                    THEN 'red'         /* Canceled    */ \n"
			    + "                WHEN jh.run_status = 4                    THEN 'green'       /* In Progress */ \n"
			    + "            END as bar_color \n"
			    + "              ,jh.job_id \n"
			    + "              ,jh.step_id \n"
			    + "              ,main_job_start_ts  = st0.run_datetime \n"
			    + "              ,step_start_ts      = msdb.dbo.agent_datetime(st0.run_date, jh.run_time) \n"
			    + "              ,step_end_ts        = dateadd(second,  (jh.run_duration/10000*3600 + (jh.run_duration/100)%100*60 + jh.run_duration%100), msdb.dbo.agent_datetime(st0.run_date, jh.run_time)) \n"
			    + "              ,step_duration_sec  = (jh.run_duration/10000*3600 + (jh.run_duration/100)%100*60 + jh.run_duration%100) \n"
			    + "             ,query_src           = st0.query_src \n"
			    + "        FROM st0 \n"
			    + "        INNER JOIN msdb.dbo.sysjobhistory jh ON (    jh.job_id       = st0.job_id \n"
			    + "                                                 AND jh.instance_id <= st0.instance_id \n"
			    + "                                                 AND (   jh.run_date > st0.run_date \n"
			    + "                                                      OR (    jh.run_date  = st0.run_date \n"
			    + "                                                          AND jh.run_time >= st0.run_time \n"
			    + "                                                         ) \n"
			    + "                                                     ) \n"
			    + "                                                ) \n"
			    + "            /* ---------------------------------- \n"
			    + "             * The above \"join\" was found at: \n"
			    + "             * https://dba.stackexchange.com/questions/187672/linking-job-steps-from-a-single-run-together-in-sysjobhistory \n"
			    + "             * ---------------------------------- \n"
			    + "             * Note that sysjobhistory has an ID column (instance_id). at least one entry should hit the table for each step of a job that was completed, \n"
			    + "             * followed by an entry with step_id = 0, recording the outcome of the job. Each step also records the time (run_date and run_time) \n"
			    + "             * the step was started, which will be equal to or greater than the time the job was started. \n"
			    + "             * So, the step_id = 0 row for a given run has a higher instance_id than the related steps, but a lower (or equal) run time. \n"
			    + "             * \n"
			    + "             * So, try doing an initial pull of the data from the rows where step_id = 0 into a temp table (or equivalent). \n"
			    + "             * Then, all rows from sysjobhistory with the same job_id, a lower instance_id, and a higher or equal start time \n"
			    + "             * (from run_date and run_time) should belong to the job run you're looking for. \n"
			    + "             */ \n"
			    + ") \n"
			    + "SELECT \n"
			    + "     chart_key \n"
			    + "    ,bar_name \n"
			    + "    ,bar_color \n"
			    + "    ,step_start_ts \n"
			    + "    ,step_end_ts \n"
			    + "    ,job_history.step_id \n"
			    + "    ,subsystem     = CAST(js.subsystem as nvarchar(40)) \n"
			    + "    ,command       = CAST(js.command   as nvarchar(4000)) \n"
			    + "    ,js.server \n"
			    + "    ,js.database_name \n"
			    + "    ,js.database_user_name \n"
			    + "    ,js.output_file_name \n"
			    + "    ,job_history.main_job_start_ts \n"
			    + "    ,job_history.job_id \n"
			    + "    ,query_src \n"
			    + "FROM job_history \n"
			    + "LEFT OUTER JOIN msdb.dbo.sysjobsteps js ON job_history.job_id = js.job_id AND job_history.step_id = js.step_id \n"
			    + " \n"
			    + "UNION ALL \n"
			    + " \n"
			    + "/* Get Active jobs (at level 0) */ \n"
			    + "SELECT \n"
			    + "     chart_key          = FORMAT(ja.start_execution_date, 'yyyy-MM-dd HH:mm:ss') + ' -- ' + j.name + ' - step: [000]' \n"
			    + "    ,bar_name           = '[0] FULL JOB: ' + j.name \n"
			    + "    ,bar_color          = 'lightgreen' \n"
			    + "    ,startDate          = ja.start_execution_date \n"
			    + "    ,endDate            = NULL /*--getdate() -- OR NULL */ \n"
			    + "    ,step_id            = CAST(0         as int) \n"
			    + "    ,subsystem          = CAST(NULL      as nvarchar(40)) \n"
			    + "    ,command            = CAST(NULL      as nvarchar(4000)) \n"
			    + "    ,server             = CAST(NULL      as nvarchar(128)) \n"
			    + "    ,database_name      = CAST(NULL      as nvarchar(128)) \n"
			    + "    ,database_user_name = CAST(NULL      as nvarchar(128)) \n"
			    + "    ,output_file_name   = CAST(NULL      as nvarchar(128)) \n"
			    + "    ,main_job_start_ts  = ja.start_execution_date \n"
			    + "    ,job_id             = CAST(ja.job_id as nvarchar(128)) \n"
			    + "    ,query_src          = '10' \n"
			    + "FROM msdb.dbo.sysjobactivity ja \n"
			    + "INNER JOIN msdb.dbo.sysjobs   j ON ja.job_id         = j.job_id \n"
			    + "WHERE 1=1 \n"
			    + "  AND ja.session_id = ( select max(session_id) FROM msdb.dbo.syssessions ) \n"
			    + "  AND ja.start_execution_date IS NOT NULL \n"
			    + "  AND ja.stop_execution_date  IS NULL \n"
			    + jobNameOrId_where
			    + " \n"
			    + "UNION ALL \n"
			    + " \n"
			    + "/* Get Active jobs sub levels (step_id's above 0) */ \n"
			    + "SELECT \n"
			    + "     chart_key = FORMAT(ja.start_execution_date, 'yyyy-MM-dd HH:mm:ss') + ' -- ' + j.name + ' - step: [' + FORMAT(ISNULL(last_executed_step_id,0)+1,'D3') + ']' \n"
			    + "    ,bar_name  = '[' + CAST(ISNULL(last_executed_step_id,0)+1 as varchar(9)) + '] ' + js.step_name \n"
			    + "    ,bar_color = 'green' \n"
			    + "/*  ,startDate = COALESCE(ja.last_executed_step_date, ja.start_execution_date) */ \n"
			    + "    ,startDate = ja.last_executed_step_date \n"
			    + "    ,endDate   = NULL /*--getdate() -- OR NULL */ \n"
			    + "    ,js.step_id \n"
			    + "    ,js.subsystem \n"
			    + "    ,js.command \n"
			    + "    ,js.server \n"
			    + "    ,js.database_name \n"
			    + "    ,js.database_user_name \n"
			    + "    ,js.output_file_name \n"
			    + "    ,main_job_start_ts = ja.start_execution_date \n"
			    + "    ,js.job_id \n"
			    + "    ,query_src    = '20' \n"
			    + "FROM msdb.dbo.sysjobactivity ja \n"
			    + "INNER JOIN msdb.dbo.sysjobs        j ON ja.job_id         = j.job_id \n"
			    + "INNER JOIN msdb.dbo.sysjobsteps   js ON ja.job_id         = js.job_id      AND ISNULL(ja.last_executed_step_id,0)+1 = js.step_id \n"
			    + "WHERE 1=1 \n"
			    + "  AND ja.session_id = ( select max(session_id) FROM msdb.dbo.syssessions ) \n"
			    + "  AND ja.start_execution_date IS NOT NULL \n"
			    + "  AND ja.stop_execution_date  IS NULL \n"
			    + jobNameOrId_where
			    + " \n"
			    + "ORDER BY 1, 2 \n"
			    + "";

		return sql;
	}

//	private String createInfoContent()
//	{
//		StringBuilder sb = new StringBuilder();
//
//		sb.append("<div id='servlet-params'> \n");
//		sb.append("Known Parameters: <code>" + StringUtil.toCommaStr(getKnownParameters()) + "</code><br> \n");
//		sb.append("Used Variables: <code>" + getUrlParameters() + "</code><br> \n");
//		sb.append("Scroll to: \n");
//		sb.append("<a href='#' onClick=\"scrollToTop('timeline')\">Top</a>  \n");
//		sb.append(" or \n");
//		sb.append("<a href='#' onClick=\"scrollToBottom('timeline')\">Bottom</a> \n");
//		sb.append(" of the Timeline. &emsp; <input type='checkbox' id='autoscroll-to-bottom' onclick='changeAutoscroll()'> On load AutoScroll to bottom. \n");
//		sb.append(" <a href='#' data-tooltip=\"" + StringEscapeUtils.escapeHtml4(getSql()) + "\" onClick='copyExecutedSql()'>Copy Executed SQL</a>\n");
////		sb.append("<br> \n");
//		sb.append("</div> \n");
//		sb.append("\n");
//
//		sb.append("<div id='parameter-descriptions'> \n");
//		sb.append("Parameter Description:<br> \n");
//		sb.append(getParameterDescriptionHtmlTable());
//		sb.append("<br> \n");
//		sb.append("</div> \n");
//		sb.append("\n");
//
////		sb.append("<details open>");
//		sb.append("<details>");
//		sb.append("<summary>");
//		sb.append("  <b>Executed SQL Statement:</b><br> \n");
//		sb.append("</summary>");
////		sb.append("  <div id='executed_sql' style='display: none'> \n");
//		sb.append("  <div id='executed_sql'> \n");
//		sb.append("  <pre> \n");
//		sb.append(StringEscapeUtils.escapeHtml4(getSql()));
//		sb.append("  </pre> \n");
//		sb.append("  </div> \n");
//		sb.append("</details>");
//		sb.append("<br> \n");
//		sb.append("\n");
//
//		return sb.toString();
//	}

	/**
	 * Just do println to the writer with the string truncated at the end (removing spaces)
	 * @param writer
	 * @param str
	 */
	private static void write(PrintWriter writer, String str)
	{
		writer.println(StringUtil.rtrim(str));
	}
	
	/**
	 * Here it all happens<br>
	 * <ul>
	 *     <li>Calls the database</li>
	 *     <li>With the ResultSet build Java script that produces a 'timeline'</li>
	 * </ul>
	 * 
	 * @param writer
	 * @throws Exception
	 */
	private void createJsTimeline(PrintWriter writer) 
	throws Exception
	{
//		StringBuilder sb = new StringBuilder();
		String tmpParamStr;

		String sql = getSql();

		Timestamp startTime = getUrlParameterTs("startTime", null);
		Timestamp endTime   = getUrlParameterTs("endTime", null);

		// >>> onlyLevelZero
		boolean onlyLevelZero = getUrlParameterBoolean_defaultFromDesc("onlyLevelZero");

		// >>> showKeys
		boolean showKeys = getUrlParameterBoolean_defaultFromDesc("showKeys");

		// >>> keySubstitute
		String keyTransformFrom = ""; // This is a regex
		String keyTransformTo   = ""; // the value to change
		tmpParamStr  = getUrlParameter("keyTransform");
		if (tmpParamStr != null)
		{
			keyTransformFrom = StringUtils.substringBefore(tmpParamStr, "=");
			keyTransformTo   = StringUtils.substringAfter (tmpParamStr, "=");
		}
		
		// >>> minDurationInSeconds
		int minDurationInSeconds = getUrlParameterInt("minDurationInSeconds", -1);

		// >>> keepNames
		String keepNames = getUrlParameter_defaultFromDesc("keepNames");

		// >>> skipNames
		String skipNames = getUrlParameter_defaultFromDesc("skipNames");

		// >>> fillEnd
		boolean fillEnd = getUrlParameterBoolean_defaultFromDesc("fillEnd");
		
		// >>> showTimeInBars
		boolean showTimeInBars = getUrlParameterBoolean_defaultFromDesc("showTimeInBars");
		
		
		// >>> generateDummyRows
		int    generateDummyRows    = -1;
		String generateDummyRowsStr = "";
		tmpParamStr  = getUrlParameter("generateDummyRows");
		if (tmpParamStr != null)
		{
			generateDummyRows    = StringUtil.parseInt(StringUtils.substringBefore(tmpParamStr, ":"), 50);
			generateDummyRowsStr =                     StringUtils.substringAfter (tmpParamStr, ":");
		}
		
		// >>> useDefaultTooltip
		boolean useDefaultTooltip = getUrlParameterBoolean_defaultFromDesc("useDefaultTooltip");
		
		
		// Connect to DBMS - with AutoClose
		try ( DbxConnection conn = getConnection() ) 
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

				write(writer, "<script> \n");
				write(writer, "    google.charts.load('current', {'packages':['timeline']});								");
				write(writer, "    google.charts.setOnLoadCallback(drawChart);												");
				write(writer, "    function drawChart()																		");
				write(writer, "    {																						");
				write(writer, "        var container = document.getElementById('timeline');									");
				write(writer, "        var chart = new google.visualization.Timeline(container);							");
				write(writer, "        var dataTable = new google.visualization.DataTable();								");
				write(writer, "																								");
				write(writer, "        dataTable.addColumn({ type: 'string', id: 'TextLabel' });							");
				write(writer, "        dataTable.addColumn({ type: 'string', id: 'BarText'   });							");
				write(writer, "        dataTable.addColumn({ type: 'string', role: 'style'   });	// bar color			");
				if ( ! useDefaultTooltip )
				{
					write(writer, "        dataTable.addColumn({ type: 'string', role: 'tooltip' });						"); // This is if we want to produce our own tooltip
				}
				write(writer, "        dataTable.addColumn({ type: 'date', id: 'Start' });									");
				write(writer, "        dataTable.addColumn({ type: 'date', id: 'End' });									");
				write(writer, "        dataTable.addRows([																	");

				String udTooltip = "";

				Timestamp maxTs          = null;
				String    prefix         = " ";
				Timestamp prevRowStartTs = null;
				Timestamp prevRowEndTs   = null;

				SimpleDateFormat sdf     = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

				int addRecordCount = 0;
				
				// Add all jobs to a Map, so we can write it as JavaScript Objects...
				// This will be used when 'onlyLevelZero=true' and we click on *details* for a "main" job, to open a pop-up with details for the mail job.
				// jobInstanceMap: <JobId:StartTime, ListOfRecords>
				LinkedHashMap<String, List<String>> jobInstanceMap = new LinkedHashMap<>(); 

				// Loop all rows in ResultSet
				while(rs.next())
				{
					String labelKey     = rs.getString   (1);
					String barText      = rs.getString   (2);
					String barColor     = rs.getString   (3);
					Timestamp startTs   = rs.getTimestamp(4);
					Timestamp endTs     = rs.getTimestamp(5);

					String jobId        = rs.getString("job_id");
					String jobStartTime = rs.getString("main_job_start_ts");

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

					boolean addToTimeLine = true;

					// If the "onlyLevelZero", the "barText" must start with "[0] "
					if (onlyLevelZero)
					{
						if ( ! barText.startsWith("[0] ") )
							addToTimeLine = false;
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
						labelKey = labelKey.replaceAll(keyTransformFrom, keyTransformTo);
					
					udTooltip = createUserDefinedTooltip(useDefaultTooltip, labelKey, barText, startTs, endTs, extraColumns);
					labelKey  = escapeJsQuote(labelKey);
					barText   = escapeJsQuote(barText);
					
					// Add '[HH:MM:SS]' to the barText (at least for "main" jobs)
					if (showTimeInBars)
					{
						long execTimeInMs = endTs.getTime() - startTs.getTime();
						String dhms = TimeUtils.msToTimeStrDHMS(execTimeInMs);
						
						barText += " -- <" + dhms + ">";
					}

					// Add "everything" to a Map (so we can view "skipped" records later)
					String jobIdInstance = jobId + "____" + jobStartTime;
					List<String> jobIdInstanceList = jobInstanceMap.get(jobIdInstance);
					if (jobIdInstanceList == null)
					{
						jobIdInstanceList = new ArrayList<>();
						jobInstanceMap.put(jobIdInstance, jobIdInstanceList);
					}
					// Add ALL (Level0 and all-step-ids) Records... Later we will print this out to a JavaScript object.
					jobIdInstanceList.add("[ '" + labelKey + "', '" + barText + "', '" + barColor + "', " + udTooltip + " new Date('" + sdf.format(startTs) + "'), new Date('" + sdf.format(endTs) + "') ] ");

					if (addToTimeLine)
					{
						addRecordCount++;
						write(writer, "            " + prefix + "[ '" + labelKey + "', '" + barText + "', '" + barColor + "', " + udTooltip + " new Date('" + sdf.format(startTs) + "'), new Date('" + sdf.format(endTs) + "') ] ");
						prefix = ",";

						// Remember MAX TS, used if we need to "fillEnd"
						if (maxTs == null)
							maxTs = endTs;
						else
							maxTs = endTs.getTime() > maxTs.getTime() ? endTs : maxTs;  // MAX value
					}

				} // end: loop ResultSet

				// No records was found (or fillEnd in 'NO Activity' at the end)
				String noActivityLabel = "NO Activity";
				String noActivityColor = "gray"; //"#ffffcc";
				if (fillEnd)
				{
					if (maxTs == null)
					{
						Timestamp startTs = startTime;
						Timestamp endTs   = endTime;
						udTooltip = createUserDefinedTooltip(useDefaultTooltip, noActivityLabel, noActivityLabel, startTs, endTs, null);
						// NO Activity -- FULL Period
						write(writer, "            " + prefix + "[ '" + noActivityLabel + "', '" + noActivityLabel + "', '" + noActivityColor + "', " + udTooltip + " new Date('" + sdf.format(startTs) + "'), new Date('" + sdf.format(endTs) + "') ] ");
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
							write(writer, "            " + prefix + "[ '" + noActivityLabel + "', '" + noActivityLabel + "', '" + noActivityColor + "', " + udTooltip + " new Date('" + sdf.format(startTs) + "'), new Date('" + sdf.format(endTs) + "') ] ");
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
							startTs = startTime;
							endTs   = new Timestamp(startTime.getTime() + dummyTime);
						}
						else
						{
							startTs = endTs;
							endTs   = new Timestamp(startTs.getTime() + dummyTime);
						}

						udTooltip = createUserDefinedTooltip(useDefaultTooltip, dummyLabel, dummyLabel, startTs, endTs, null);

						write(writer, "            " + prefix + "[ '" + tmpDummyLabel + "', '" + tmpDummyLabel + "', '" + dummyColor + "', " + udTooltip + " new Date('" + sdf.format(startTs) + "'), new Date('" + sdf.format(endTs) + "') ] ");
						
						prefix = ",";
					}
					
				}

				// END_OF: dataTable.addRows([
				write(writer, "        ]);																				");

				// TODO: Add properties for the below
				int timelineFontSize = 10;
				int recordThresholdForSmallerFont = 22;
				
				write(writer, "        // When we click on a item copy tooltip content to the clipboard                 ");
				write(writer, "        google.visualization.events.addListener(chart, 'select', function()              ");
				write(writer, "        {                                                                                ");
				write(writer, "            var selectedItem = chart.getSelection()[0];                                  ");
				write(writer, "            if (selectedItem)                                                            ");
				write(writer, "            {                                                                            ");
				write(writer, "                // Get the toolip section, and format it into plain text                 ");
				write(writer, "                let htmlTooltip = dataTable.getValue(selectedItem.row, 3);               ");
				write(writer, "                htmlTooltip = htmlTooltip.replace(/<br>/g, '\\n');                       ");
				write(writer, "                htmlTooltip = htmlTooltip.replace(/<hr>/g, '\\n-------------------------------------------------------\\n');");
				write(writer, "                htmlTooltip = htmlTooltip.replace(/<\\/tr>/g, '\\n');                    ");
				write(writer, "                htmlTooltip = htmlTooltip.replace(/&nbsp;/g, ' ');                       ");
				write(writer, "                htmlTooltip = htmlTooltip.replace(/&emsp;/g, '\\t');                     ");
				write(writer, "                                                                                         ");
				write(writer, "                htmlTooltip = htmlTooltip.replace(/<[^>]+>/g, '');                       ");
				write(writer, "                                                                                         ");
				write(writer, "                // Write content to copy/paste buffer                                    ");
				write(writer, "                copyToClipboard(htmlTooltip);                                            ");
				write(writer, "                                                                                         ");
				write(writer, "                if (htmlTooltip.indexOf('[0] - FULL JOB: ') != -1)                       ");
				write(writer, "                {                                                                        ");
				write(writer, "                    let job_id = '';                                                     ");
				write(writer, "                    let main_job_start_ts = '';                                          ");
				write(writer, "                                                                                         ");
				write(writer, "                    // get 'job_id:'                                                     ");
				write(writer, "                    let startPos = htmlTooltip.indexOf('job_id: ');                      ");
				write(writer, "                    if (startPos >= 0)                                                   ");
				write(writer, "                    {                                                                    ");
				write(writer, "                        startPos += 'job_id: '.length;                                   ");
				write(writer, "                        let endPos = htmlTooltip.indexOf('\\n', startPos);               ");
				write(writer, "                        job_id = htmlTooltip.substring(startPos, endPos).trim();         ");
				write(writer, "                    }                                                                    ");
				write(writer, "                                                                                         ");
				write(writer, "                    // get 'main_job_start_ts:'                                          ");
				write(writer, "                    startPos = htmlTooltip.indexOf('main_job_start_ts: ');               ");
				write(writer, "                    if (startPos >= 0)                                                   ");
				write(writer, "                    {                                                                    ");
				write(writer, "                        startPos += 'main_job_start_ts: '.length;                        ");
				write(writer, "                        let endPos = htmlTooltip.indexOf('\\n', startPos);               ");
				write(writer, "                        main_job_start_ts = htmlTooltip.substring(startPos, endPos).trim(); ");
				write(writer, "                    }                                                                    ");
				write(writer, "                                                                                         ");
				write(writer, "                    // get 'main' JobName and Time                                       ");
				write(writer, "                    let jobTimeAndName = dataTable.getValue(selectedItem.row, 0);        ");
				write(writer, "                                                                                         ");
				write(writer, "                    // Open modal                                                        ");
				write(writer, "                    openModalForJobId(job_id, main_job_start_ts, jobTimeAndName);        ");
				write(writer, "                                                                                         ");
				write(writer, "                    // Close the tooltip in some way                                     ");
				write(writer, "                    document.querySelectorAll('.google-visualization-tooltip').forEach(el => el.style.display = 'none'); ");
				write(writer, "                    // setTimeout(() => chart.setSelection([]), 1000); // and UNSELECT the current selected row (but causes other problems)  ");
				write(writer, "                }                                                                        ");
				write(writer, "            }                                                                            ");
				write(writer, "        });                                                                              ");
				write(writer, "                                                                                         ");
				write(writer, "        google.visualization.events.addListener(chart, 'ready', function() {				");
				write(writer, "            console.log('DONE: Loading chart-timeline, now scrolling to bottom...');		");
				if (fillEnd)
					write(writer, "            scrollToBottom('timeline');												");
				else
					write(writer, "            //scrollToBottom('timeline'); // fillEnd was FALSE 						");
				write(writer, "        });																				");
				write(writer, "																							");
				write(writer, "        var options = 																	");
				write(writer, "        {																				");
				write(writer, "             hAxis: { format: '< HH:mm - dd MMM >' }										");
				write(writer, "            ,timeline: { 																");
				write(writer, "                showRowLabels: " + showKeys + "											");
				if (addRecordCount >= recordThresholdForSmallerFont && timelineFontSize > 0)
				{
					write(writer, "                ,rowLabelStyle: { fontSize: " + timelineFontSize + " }				");
					write(writer, "                ,barLabelStyle: { fontSize: " + timelineFontSize + " }				");
				}
				write(writer, "            } 																			");
				write(writer, "        };																				");
				write(writer, "																							");
				write(writer, "        chart.draw(dataTable, options);													");
				if (addRecordCount <= 0)
				{
					write(writer, "");
					write(writer, "        // No records was added... show some info abount that ");
					write(writer, "        console.log('No data was added. StartTime=|" + startTime + "|, endTime=|" + endTime + "|, addRecordCount=" + addRecordCount + "'); ");
					write(writer, "        document.getElementById('dbx-job-scheduler-no-data').style.display = 'block'; ");
					write(writer, "        let tmpText  = 'No data was found for period: '; ");
					write(writer, "            tmpText += '<ul> '; ");
					write(writer, "            tmpText += '  <li>startTime: " + startTime + "</li> '; ");
					write(writer, "            tmpText += '  <li>endTime:   " + endTime   + "</li> '; ");
					write(writer, "            tmpText += '</ul> '; ");
					write(writer, "        document.getElementById('dbx-job-scheduler-no-data').innerHTML = tmpText; ");
				}
				write(writer, "																							");
				write(writer, "        // set the start/end time in the 'navigation bar time-lable'						");
				write(writer, "        setDateRangePickerLabel('" + startTime+ "', '" + endTime + "');					");
				write(writer, "    }																					");
				write(writer, "																							");
//				write(writer, "    function changeAutoscroll() {														");
//				write(writer, "       var div = document.getElementById('autoscroll-to-bottom');						");
//				write(writer, "       var storedData = getStorage('dbxtune_checkboxes_');								");
//				write(writer, "       storedData.set('autoscroll-to-bottom', div.checked);								");
//				write(writer, "																							");
//				write(writer, "       console.log('changeAutoscroll: ' + div.checked);									");
//				write(writer, "    }																					");
				write(writer, "																							");
				write(writer, "    // When we click on a 'Level0' element, this array will be set						");
				write(writer, "    var g_latestSelectedJobIdInstanceArr = [];											");
				write(writer, "																							");
				write(writer, "    function openModalForJobId(job_id, main_job_start_ts, jobTimeAndName)				");
				write(writer, "    {                                                                                    ");
				write(writer, "        // Refereence the variablename and show data i a modal dialog 					");
				write(writer, "        let propName = job_id + '____' + main_job_start_ts;                              ");
				write(writer, "        let jobIdInstance = globalJobIdInstance[propName];                               ");
				write(writer, "        g_latestSelectedJobIdInstanceArr = jobIdInstance;                                ");
				write(writer, "                                                                                         ");
				write(writer, "        console.log('Selected jobIdInstance for |' + propName + '|.', jobIdInstance);    ");
//				write(writer, "        alert('TODO: Show FULL Details for child steps for job_id=|' + job_id + '|, main_job_start_ts=|' + main_job_start_ts + '|.'); ");
				write(writer, "                                                                                         ");
				write(writer, "        // Set various info in the modal dialog                                          ");
				write(writer, "        document.getElementById('dbx-view-jobIdDetails-title').innerHTML = 'Job Details for: ' + jobTimeAndName; ");
				write(writer, "                                                                                         ");
				write(writer, "        // Open the modal... drawChartModal() will be done when to modal IS OPEN         ");
				write(writer, "        $('#dbx-view-jobIdDetails-dialog').modal('show');                                ");
//				write(writer, "        google.charts.load('current', {'packages':['timeline']});						");
//				write(writer, "        google.charts.setOnLoadCallback(drawChartModal);									");
				write(writer, "    }                                                                                    ");
				write(writer, "    function drawChartModal()															");
				write(writer, "    {																					");
				write(writer, "        let container = document.getElementById('timeline-modal');						");
				write(writer, "        let chart = new google.visualization.Timeline(container);						");
				write(writer, "        let dataTable = new google.visualization.DataTable();							");
				write(writer, "	   																						");
				write(writer, "        dataTable.addColumn({ type: 'string', id: 'TextLabel' });						");
				write(writer, "        dataTable.addColumn({ type: 'string', id: 'BarText'   });						");
				write(writer, "        dataTable.addColumn({ type: 'string', role: 'style'   });						");
				write(writer, "        dataTable.addColumn({ type: 'string', role: 'tooltip' });						");
				write(writer, "        dataTable.addColumn({ type: 'date', id: 'Start' });								");
				write(writer, "        dataTable.addColumn({ type: 'date', id: 'End' });								");
//				write(writer, "        dataTable.addRows(jobIdInstance);												");
				write(writer, "        dataTable.addRows(g_latestSelectedJobIdInstanceArr);								");
				write(writer, "																							");
				write(writer, "        let options = 																	");
				write(writer, "        {																				");
				write(writer, "             hAxis: { format: '< HH:mm - dd MMM >' }											");
				write(writer, "            ,timeline: { 																");
				write(writer, "                showRowLabels: false														");
//				write(writer, "                ,rowLabelStyle: { fontSize: 10 }											");
//				write(writer, "                ,barLabelStyle: { fontSize: 10 }											");
				write(writer, "            } 																			");
				write(writer, "        };																				");
				write(writer, "        if (g_latestSelectedJobIdInstanceArr.length >= " + recordThresholdForSmallerFont + ") ");
				write(writer, "        {																				");
				write(writer, "            options.timeline.rowLabelStyle = { fontSize: " + timelineFontSize + " };		");
				write(writer, "            options.timeline.barLabelStyle = { fontSize: " + timelineFontSize + " };		");
				write(writer, "        }																				");
				write(writer, "	   																						");
				write(writer, "        // When we click on a item copy tooltip content to the clipboard                 ");
				write(writer, "        google.visualization.events.addListener(chart, 'select', function()              ");
				write(writer, "        {                                                                                ");
				write(writer, "            var selectedItem = chart.getSelection()[0];                                  ");
				write(writer, "            if (selectedItem)                                                            ");
				write(writer, "            {                                                                            ");
				write(writer, "                // Get the toolip section, and format it into plain text                 ");
				write(writer, "                let htmlTooltip = dataTable.getValue(selectedItem.row, 3);               ");
				write(writer, "                htmlTooltip = htmlTooltip.replace(/<br>/g, '\\n');                       ");
				write(writer, "                htmlTooltip = htmlTooltip.replace(/<hr>/g, '\\n-------------------------------------------------------\\n');");
				write(writer, "                htmlTooltip = htmlTooltip.replace(/<\\/tr>/g, '\\n');                    ");
				write(writer, "                htmlTooltip = htmlTooltip.replace(/&nbsp;/g, ' ');                       ");
				write(writer, "                htmlTooltip = htmlTooltip.replace(/&emsp;/g, '\\t');                     ");
				write(writer, "                                                                                         ");
				write(writer, "                htmlTooltip = htmlTooltip.replace(/<[^>]+>/g, '');                       ");
				write(writer, "                                                                                         ");
				write(writer, "                // Write content to copy/paste buffer                                    ");
				write(writer, "                copyToClipboard(htmlTooltip);                                            ");
				write(writer, "            }                                                                            ");
				write(writer, "        });                                                                              ");
				write(writer, "                                                                                         ");
//				write(writer, "        // Open the modal																");
//				write(writer, "        $('#dbx-view-jobIdDetails-dialog').modal('show'); 								");
//				write(writer, "	   																						");
				write(writer, "        // Then draw the chart															");
				write(writer, "        chart.draw(dataTable, options);													");
				write(writer, "    }																					");
				write(writer, "																							");
				write(writer, "    function recursiveScrollToTopOrBottom(element, to, level, maxLevel)					");
				write(writer, "    {                                                                                    ");
				write(writer, "        level = level + 1;                                                               ");
				write(writer, "        if (level > maxLevel)                                                            ");
				write(writer, "            return;                                                                      ");
				write(writer, "        var childArr = element.children;                                                 ");
				write(writer, "        if (childArr.length > 0)                                                         ");
				write(writer, "        {                                                                                ");
				write(writer, "            for (var child of childArr)                                                  ");
				write(writer, "            {                                                                            ");
//				write(writer, "//              if (child.css('overflow-y') == 'scroll' || child.css('overflow-y') == 'auto')                        ");
//				write(writer, "//              if ( (child.scrollHeight > child.clientHeight) && ('hidden' !== getComputedStyle(child).overflowY) )	");
				write(writer, "                if ( (child.scrollHeight > child.clientHeight) )                                                     ");
				write(writer, "                {                                                                                                    ");
				write(writer, "                    console.log('Scroll to ' + to + ': level[' + level + '], child='+child, child);                  ");
				write(writer, "                    if (to === 'top')                                                    ");
				write(writer, "                        child.scrollTop = 0;                                             ");
				write(writer, "                    else                                                                 ");
				write(writer, "                        child.scrollTop = child.scrollHeight - child.clientHeight;       ");
//				write(writer, "					                                                                        ");
//				write(writer, "//                  break;                                                               ");
				write(writer, "                }                                                                        ");
				write(writer, "				                                                                            ");
				write(writer, "                recursiveScrollToTopOrBottom(child, to, level, maxLevel);                ");
				write(writer, "            }                                                                            ");
				write(writer, "        }                                                                                ");
				write(writer, "    }                                                                                    ");
				write(writer, "                                                                                         ");
				write(writer, "    function scrollToBottom (id) {														");
				write(writer, "       var elem = document.getElementById(id);											");
				write(writer, "       recursiveScrollToTopOrBottom(elem, 'bottom', -1, 3);								");
				write(writer, "    }																					");
				write(writer, "    																						");
				write(writer, "    function scrollToTop (id) {															");
				write(writer, "       var elem = document.getElementById(id);											");
				write(writer, "       recursiveScrollToTopOrBottom(elem, 'top', -1, 3);									");
				write(writer, "    }																					");
				write(writer, "    																						");
				write(writer, "    function copyToClipboard(str) {														");
				write(writer, "       const textArea = document.createElement('textarea');								");
				write(writer, "       textArea.value = str;																");
				write(writer, "       document.body.appendChild(textArea);												");
				write(writer, "       textArea.select();																");
				write(writer, "       try {																				");
				write(writer, "       	document.execCommand('copy');													");
				write(writer, "       } catch (err) {																	");
				write(writer, "       	alert('Unable to copy to clipboard' + err);										");
				write(writer, "       }																					");
				write(writer, "       document.body.removeChild(textArea);												");
				write(writer, "																							");
				write(writer, "       console.log('copyToClipboard: ' + str);											");
				write(writer, "    }																					");
				write(writer, "																							");
				write(writer, "    function copyExecutedSql() {															");
				write(writer, "       var sqlText = document.getElementById('executed_sql').textContent;				");
				write(writer, "																							");
				write(writer, "       copyToClipboard(sqlText);															");
				write(writer, "    }																					");
				write(writer, "																							");
//				write(writer, "    // do-deferred: Scroll to bottom of 'timeline'										");
//				write(writer, "																							");
//				write(writer, "    setTimeout(function() {																");
//				write(writer, "        var savedVal_autoscrollToBottom = getStorage('dbxtune_checkboxes_').get('autoscroll-to-bottom');		");
//				write(writer, "        document.getElementById('autoscroll-to-bottom').checked = savedVal_autoscrollToBottom;				");
//				write(writer, "        if (savedVal_autoscrollToBottom) {												");
//				write(writer, "            scrollToBottom('timeline');													");
//				write(writer, "        }																				");
//				write(writer, "    }, 200);																				");
//				write(writer, "																							");
//				write(writer, "    if (document.getElementById('autoscroll-to-bottom').checked)	{						");
//				write(writer, "        setTimeout(function() { scrollToBottom('timeline') }, 100);						");
//				write(writer, "    }																					");
				
				write(writer, "																							");
				write(writer, "    // Create a Java Object that will hold ALL job instances								");
				write(writer, "    let globalJobIdInstance = {															");
				write(writer, "    }																					");
				write(writer, "																							");
				write(writer, "    // Now ADD entries to the above object. First initialize, then push					");
				for (Entry<String, List<String>> entry : jobInstanceMap.entrySet())
				{
					String       key = entry.getKey();
					List<String> val = entry.getValue();

					write(writer, "                          															");
					write(writer, "    // --------																		");
					write(writer, "    globalJobIdInstance['" + key + "'] = [];											");
					for (String row : val)
					{
						write(writer, "    globalJobIdInstance['" + key + "'].push(" + row + ");						");
					}
				}
				write(writer, "																							");
				write(writer, "</script> \n");
			}
		}
		catch (SQLException ex)
		{
			_logger.error("In '" + this.getClass().getSimpleName() + "'. Problems executing SQL Statement. ErrorCode=" + ex.getErrorCode() + ", SQLState=" + ex.getSQLState() + ", Message=|" + ex.getMessage() + "|, SQL=|" + sql + "|.");

			throw new SQLException("Problems executing SQL Statement. ErrorCode=" + ex.getErrorCode() + ", SQLState=" + ex.getSQLState() + ", Message=|" + ex.getMessage() + "|, SQL=|" + sql + "|.", ex);
		}
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
				
				tooltip += "<tr> <td nowrap><b>" + ttKey + ": </b></td> <td nowrap " + tdAttr + ">" + ttVal + "</td> </tr>";
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
