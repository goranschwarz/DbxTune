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
import java.util.HashMap;
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
import com.dbxtune.central.controllers.DbxTimelineRows;
import com.dbxtune.central.controllers.HtmlStatic.PageSection;
import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.report.content.sqlserver.SqlServerJobScheduler;
import com.dbxtune.pcs.report.content.sqlserver.SqlServerJobScheduler.ReturnObject_allExecTimes;
import com.dbxtune.pcs.report.content.sqlserver.SqlServerJobScheduler.SourceType;
import com.dbxtune.pcs.report.content.sqlserver.SqlServerJobScheduler.StatObject;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.DbUtils;
import com.dbxtune.utils.MathUtils;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.TimeUtils;

public class SqlServerJobSchedulerTimeline
extends DbxCentralPageTemplate
{
	private static final long serialVersionUID = 1L;
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public static final String PROPKEY_startTime = "SqlServerJobSchedulerTimeline.startTime.default";
	public static final String DEFAULT_startTime = "-2h";

	public static final String PROPKEY_refreshTime = "SqlServerJobSchedulerTimeline.refreshTime.default";
	public static final int    DEFAULT_refreshTime = 0;

	private DbxConnection _conn = null;
	
	private Map<String, StatObject> _jobIdStepIdExecSummaryMap;
	
	private boolean _debugMode    = false;
	private double  _deviationPct = 50.0;
	private double  _diffColorPct = 10.0;


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
	public void craeteHtmlHeadCssPre(PrintWriter writer)
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
		writer.println("    /* Override some 'prism' formatting to be smaller & wrap long lines */ ");
		writer.println("    code[class*=language-], pre[class*=language-] { ");
		writer.println("        font-size: 11px; ");
		writer.println("        white-space: pre-wrap; ");
		writer.println("    } ");
		writer.println();
		writer.println("</style>");
	}

	@Override
	public void craeteHtmlHeadCssPost(PrintWriter writer)
	{
		writer.println("<style type='text/css'>");
		writer.println();
		writer.println("    /* Override some 'prism' formatting to be smaller & wrap long lines */ ");
		writer.println("    code[class*=language-], pre[class*=language-] { ");
		writer.println("        font-size: 11px; ");
		writer.println("        white-space: pre-wrap; ");
		writer.println("    } ");
		writer.println();
//		writer.println("    /* Modal dialog... if we use several... we need to play around with the 'z-index' */ ");
//		writer.println("    /* I'm guessing this only works for 2 modals, and possible only for bootstrap 4 */ ");
//		writer.println("    .modal:nth-of-type(even) { ");
//		writer.println("        z-index: 1052 !important; ");
//		writer.println("    } ");
//		writer.println("    .-backdrop.show:nth-of-type(even) { ");
//		writer.println("        z-index: 1051 !important; ");
//		writer.println("    } ");
//		writer.println();
		writer.println("</style>");
	}
	
	@Override
	protected List<String> getJavaScriptList()
	{
		List<String> list = new ArrayList<>();

		// Timeline chart: vis-timeline + dbxTimeline.js (replaced the Google Charts Timeline, which needs internet access)
		list.addAll(DbxTimelineRows.JAVASCRIPT_LIST);

		// Date Range Picker
		list.add("/scripts/bootstrap-daterangepicker/3.1/daterangepicker.js");

		// Chart.js
//		list.add("/scripts/chartjs/2.7.3/Chart.min.js");
//		list.add("/scripts/chartjs/2.7.3/plugins/chartjs-plugin-annotation.js");
//		list.add("https://cdnjs.cloudflare.com/ajax/libs/Chart.js/3.7.0/chart.min.js"); 
//		list.add("https://cdnjs.cloudflare.com/ajax/libs/chartjs-plugin-annotation/2.2.1/chartjs-plugin-annotation.min.js"); 
		list.add("/scripts/chartjs/4.5.1/dist/chart.umd.min.js"); 
		list.add("/scripts/chartjs-plugin-annotation/3.1.0/chartjs-plugin-annotation.min.js"); 
		
		// Prism -- to get TEXT field(s) to look better
//		list.add("https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/prism.min.js");
//		list.add("https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/components/prism-sql.min.js");
//		list.add("https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/components/prism-json.min.js");
//		list.add("https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/plugins/line-numbers/prism-line-numbers.min.js");
		list.add("/scripts/prism/prism-1.30.0.js"); // This is a replacement that contains all of the above prism files

		return list;
	}

	@Override
	protected List<String> getCssList()
	{
		List<String> list = new ArrayList<>();

		// Date Range Picker
		list.add("/scripts/bootstrap-daterangepicker/3.1/daterangepicker.css");

		// Timeline chart
		list.addAll(DbxTimelineRows.CSS_LIST);

		// Prism -- to get TEXT field(s) to look better
//		list.add("https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/themes/prism-okaidia.min.css");
//		list.add("https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/plugins/line-numbers/prism-line-numbers.min.css");
		list.add("/scripts/prism/prism-1.30.0.css"); // This is a replacement that contains all of the above prism files

		return list;
	}
	
	@Override
	public String getNavbarHtmlRightSideHookin()
	{
		StringWriter out    = new StringWriter();
		PrintWriter  writer = new PrintWriter(out);

		writer.println("    <div id='dbx-report-range' class='dbx-nav-text btn btn-outline-light me-2'>  ");
		writer.println("        <i class='glyphicon glyphicon-calendar fa fa-calendar'></i>&nbsp;  ");
		writer.println("        <span>Time Span</span> <b class='caret'></b>  ");
		writer.println("    </div>  ");
		writer.println();
		writer.println("    <div class='btn px-0 btn-outline-light'>  ");
		writer.println("        <button id='dbx-prev-day'            title='Previous DAY, same time span'    alt='Previous DAY, samt time span'    class='dbx-nav-button p-0 ms-1' type='button'><i class='glyphicon fa fa-backward'></i></button>          <!-- fa-backward fa-fast-backward          -->  ");
		writer.println("        <button id='dbx-prev-time'           title='Previous # Hours'                alt='Previous # Hours'                class='dbx-nav-button p-0'      type='button'><i class='glyphicon fa fa-arrow-circle-left'></i></button> <!-- fa-step-backward fa-arrow-circle-left -->  ");
		writer.println("        <span   id='dbx-sample-time'         title='Hours in the selected Time Span' alt='Hours in the selected Time Span' class=''>?h</span>  ");
		writer.println("        <button id='dbx-next-time'           title='Next # Hours'                    alt='Next # Hours'                    class='dbx-nav-button p-0'      type='button'><i class='glyphicon fa fa-arrow-circle-right'></i></button> <!-- fa-step-forward fa-arrow-circle-right -->  ");
		writer.println("        <button id='dbx-next-day'            title='Next DAY, same time span'        alt='Next DAY, samt time span'        class='dbx-nav-button p-0 me-1' type='button'><i class='glyphicon fa fa-forward'></i></button>            <!-- fa-forward fa-fast-forward            -->  ");
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
		writer.println("                'Last 2 Hours':    [moment().subtract(2,  'hours'), moment()], ");
		writer.println("                'Last 4 Hours':    [moment().subtract(4,  'hours'), moment()], ");
		writer.println("                'Last 6 Hours':    [moment().subtract(6,  'hours'), moment()], ");
		writer.println("                'Last 8 Hours':    [moment().subtract(8,  'hours'), moment()], ");
		writer.println("                'Last 12 Hours':   [moment().subtract(12, 'hours'), moment()], ");
		writer.println("                'Last 24 Hours':   [moment().subtract(24, 'hours'), moment()], ");
		writer.println("                'Start Time + 2H': [moment(), moment()], ");
		writer.println("                'Start Time + 4H': [moment(), moment()], ");
		writer.println("                'Start Time + 8H': [moment(), moment()], ");
		writer.println("                'Today':           [moment().startOf('day'), moment().endOf('day')], ");
		writer.println("                'Yesterday':       [moment().startOf('day').subtract(1,  'days'), moment().endOf('day').subtract(1, 'days')], ");
		writer.println("                'This Week':       [moment().startOf('isoWeek'), moment().endOf('isoWeek')], ");
		writer.println("                'Last 7 Days':     [moment().startOf('day').subtract(7,  'days'), moment().endOf('day')], ");
		writer.println("                'Last 2 Weeks':    [moment().startOf('day').subtract(14, 'days'), moment().endOf('day')], ");
		writer.println("                'Last 4 Weeks':    [moment().startOf('day').subtract(28, 'days'), moment().endOf('day')], ");
		writer.println("                'Last 30 Days':    [moment().startOf('day').subtract(30, 'days'), moment().endOf('day')], ");
		writer.println("                'This Month':      [moment().startOf('month'), moment().endOf('month')], ");
		writer.println("                'Last Month':      [moment().subtract(1, 'month').startOf('month'), moment().subtract(1, 'month').endOf('month')] ");
		writer.println("            } ");
		writer.println("        }); ");
		writer.println();       
		writer.println("        // On daterangepicker-APPLY ");
		writer.println("        $('#dbx-report-range').on('apply.daterangepicker', function(ev, picker) { ");
		writer.println("            dateRangePickerCb(picker.startDate, picker.endDate, picker.chosenLabel, picker); ");
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
		writer.println("    function dateRangePickerCb(start, end, label, picker) ");
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
		writer.println("        else if (label === 'Start Time + 2H' || label === 'Start Time + 4H' || label === 'Start Time + 8H') ");
		writer.println("        { ");
//		writer.println("            const currentStartTime = moment(picker.startDate); ");
		writer.println("            // Get current start/end Time from 'the button label' #dbx-report-range) ");
		writer.println("            var currentStartTime = ''; ");
		writer.println("            var currentEndTime   = ''; ");
		writer.println("            const currentStartEndDate = $('#dbx-report-range span').html().split(' - '); ");
		writer.println("            if (currentStartEndDate.length === 2) ");
		writer.println("            { ");
		writer.println("                currentStartTime = parseDate(currentStartEndDate[0]); ");
		writer.println("                currentEndTime   = parseDate(currentStartEndDate[1]); ");
		writer.println("            } ");
		writer.println();
		writer.println("            var   startTime = currentStartTime.format('YYYY-MM-DD HH:mm'); ");
		writer.println("            var   endTime   = '2h'; ");
		writer.println("                                                           ");
		writer.println("            if      (label === 'Start Time + 2H')  endTime = '2h'; ");
		writer.println("            else if (label === 'Start Time + 4H')  endTime = '4h'; ");
		writer.println("            else if (label === 'Start Time + 8H')  endTime = '8h'; ");
		writer.println();
		writer.println("            const url = new URL(window.location.href); ");
		writer.println();
		writer.println("            url.searchParams.set('" + urlParameterName_startTime + "', startTime); ");
		writer.println("            url.searchParams.set('" + urlParameterName_endTime   + "', endTime); ");
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
		writer.println();
		writer.println("<script>");
		writer.println();
		writer.println("function updateLastUpdatedClock() {                   ");
		writer.println("    var ageInSec = Math.floor((new Date() - lastUpdateTime) / 1000);");
		writer.println("    document.getElementById('last-update-ts').innerHTML = ageInSec + ' seconds ago'; ");
//		writer.println("    console.log('updateLastUpdatedClock(): ' + document.getElementById('last-update-ts'));");
//		writer.println("    console.log('updateLastUpdatedClock(): ' + ageInSec + ' seconds ago');");
		writer.println("    setTimeout(updateLastUpdatedClock, 1000); ");
		writer.println("}                                                     ");
		writer.println("var lastUpdateTime = new Date();");
		writer.println();
		writer.println("</script>");
		writer.println();
	}

	@Override
	public void createHtmlBodyJavaScriptBottom(PrintWriter writer)
	{
		writer.println();
		writer.println("<script>");
		writer.println();
		writer.println("<!-- Initialize various stuff --> ");
		writer.println();
		writer.println("<!-- Make all modal dialogs: Dragable & Resizeable --> ");
		writer.println("$('.modal-content').resizable({ alsoResize: '.modal-dialog' }); ");
		writer.println("$('.modal-dialog' ).draggable({ handle:     '.modal-header' }); ");
		writer.println();
		writer.println("</script>");
		writer.println();
	}

	/**
	 * Describe Colors used in the TimeLine
	 * @param writer
	 * @return
	 */
	public void createColorDescriptions(PrintWriter writer)
	{
		writer.println();
		writer.println("Below are descriptions of bar colors used in the timeline");
		writer.println("<table>");
		writer.println("<thead>");
		writer.println("    <tr> <th>Color</th> <th>Description</th> </tr>");
		writer.println("</thead>");
		writer.println("<tbody>");
		writer.println("    <tr> <td>Light Blue</td>    <td>Top level (step_id=0) of any job... Click the bar or the row label to expand/collapse its steps</td> </tr>");
		writer.println("    <tr> <td>Blue</td>          <td>Normal Job Step (step_id>0) </td> </tr>");
		writer.println("    <tr> <td>Red</td>           <td>JobStep Failed</td> </tr>");
		writer.println("    <tr> <td>Orange</td>        <td>JobStep has some issues/warnings</td> </tr>");
		writer.println("    <tr> <td>Pink</td>          <td>JobStep is slower than <i>normal</i> (based on average historical execuation times, see tooltip for details)</td> </tr>");
		writer.println("    <tr> <td>Light Green</td>   <td>JobStep is faster than <i>normal</i> (based on average historical execuation times, see tooltip for details)</td> </tr>");
		writer.println("    <tr> <td>Green</td>         <td>The JobStep is currently Executing, any '<i>Tep Level/step_id=0</i> will be a lighter green</td> </tr>");
		writer.println("    <tr> <td>Gray</td>          <td><i>NO Activity</i> At the end of the timeline, if endTime is not specified...</td> </tr>");
		writer.println("</tbody>");
		writer.println("</table>");
		writer.println("<br>");
		writer.println();
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
		
		// Describe used colors in the timeline
		createColorDescriptions(writer);
		
		// Describe URL Parameters 
		writer.println("Below are descriptions of URL Parameters for this page");
		writer.println(getParameterDescriptionHtmlTable());
		
		// Another collapsable section (IN the summary) to show SQL Statement
		writer.println();
		writer.println("    <details>");
		writer.println("          <summary>");
		writer.println("              <b>Executed SQL Statement:</b><br>");
		writer.println("          </summary>");
		writer.println("          <div id='executed_sql'>");
		writer.println("<pre>");
		writer.println("<code class='language-sql line-numbers'>");
		writer.println(StringEscapeUtils.escapeHtml4(getSql()));
		writer.println("</code>");
		writer.println("</pre>");
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
				"deviationPct",
				"If the execution time deviates to much from the average execution time, mark them pink or lightGreen<br>"
						+ "This is the <b>base</b> percent to be considdered. <br>"
						+ "But for fast executions the base percent will be multipied with X. <br>"
						+ "And for longer executions the multiply factor will decrease. <br>"
						+ "And for really long executions the multiply factor will be below 0.75 or 0.5 <br>"
						+ "<br>"
						+ "<b>Default</b>: <code>50.0</code>", 
                50.0d,
                Double.class));

		set.add(new UrlParameterDescription(
				"diffColorPct",
				"Mark the <i>diff</i> from the average execution time in the bar text: green if faster, orange if slower<br>"
						+ "when it differs at least this many percent. (Executions shorter than 10 seconds are not marked.)<br>"
						+ "<br>"
						+ "<b>Default</b>: <code>10.0</code>",
                10.0d,
                Double.class));

		set.add(new UrlParameterDescription(
				"showKeys",
				"Show the chart 'keys' at the left side of the chart<br>"
						+ "A chart key is what groups the various rows together, also see <code>keyTransform</code><br>"
						+ "<br>"
						+ "<b>Default</b>: <code>false</code>", 
                false, 
                Boolean.class));

		set.add(new UrlParameterDescription(
				"fillEnd",
				"Fill the end of the chart with 'No Activity' label. Usefull if we are looking at the 'tail', so we can se time that has passed without any activity <br>"
						+ "<br>"
						+ "<b>Note</b>:    This will be disabled if the <code>endTime</code> is specified.<br>"
						+ "<b>Default</b>: <code>true</code>",
                true,
                Boolean.class));

		set.add(new UrlParameterDescription(
				"minDurationInSeconds",
				"If we want to 'skip' items in the chart which has a 'duration' of less than ## seconds<br>"
						+ "<b>Default</b>: <code>0</code> (if not restricted by the executed SQL Statement",
                10,
                Integer.class));

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
				"generateDummyRows",
				"Just for demo purposes. Generate some dummy records...<br>"
						+ "<br>"
						+ "Example: <code>15</code> (Generate 15 dummy rows, like 'Dummy row #') <br>"
						+ "Example: <code>20:aaa {id} bbb</code> (Generate 15 dummy rows, with the label 'aaa # bbb', where # is the row number) <br>"
						+ "<b>Default</b>: <i>none</i>",
                null,
                String.class));

		set.add(new UrlParameterDescription(
				"debug",
				"Enable some debugging.<br>"
						+ "<br>"
						+ "<b>Default</b>: <code>false</code>",
                false,
                Boolean.class));

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

		//----------------------------------------
		// debug
		_debugMode = getUrlParameterBoolean_defaultFromDesc("debug");

		//----------------------------------------
		// deviationPct
		_deviationPct = getUrlParameterDouble("deviationPct", 50.0);

		//----------------------------------------
		// diffColorPct
		_diffColorPct = getUrlParameterDouble("diffColorPct", 10.0);

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


		// NOTE: When I removed the 'msdb.dbo.agent_datetime' function, it seems to slow down a bit
		//       Lets live with that for now... and see if we can solve that "later"
		//       BUT: We should REWRITE the the below from start... using a bit "smarter" code... This starts to be "clunky" 
		String sql = ""
//			    + "/* NOTE: the below needs: use msdb; GRANT EXECUTE ON msdb.dbo.agent_datetime TO PUBLIC -- or: dbxtune */ \n"
			    + "/* NOTE: since I dont want to use function 'msdb.dbo.agent_datetime' due to authorizations/grant issues */ \n"
			    + "/*       It's emulated via: convert(datetime, convert(varchar(8), RUN_DATE)) + ' ' + stuff(stuff(right(1000000 + RUN_TIME,6),3,0,':'),6,0,':') */ \n"
			    + "/*       It might be a bit slower, but we dont need to 'grant execution' on the function... */ \n"
			    + "DECLARE @minimum_duration_in_seconds int = " + minDurationInSeconds + " \n"
			    + ";WITH st0 AS \n"
			    + "( \n"
			    + "    /* Get \"parent\" jobs (step_id=0) that are in the history -- jobs that already has been executed */ \n"
			    + "    SELECT \n"
			    + "        job_name     = j.name \n"
//			    + "       ,run_datetime = msdb.dbo.agent_datetime(h.run_date, h.run_time) \n"
			    + "       ,run_datetime = convert(datetime, convert(varchar(8), h.run_date)) + ' ' + stuff(stuff(right(1000000+h.run_time,6),3,0,':'),6,0,':') \n"
			    + "       ,h.instance_id \n"
			    + "       ,h.job_id \n"
			    + "       ,h.run_date \n"
			    + "       ,h.run_time \n"
			    + "       ,query_src = '1' \n"
			    + "    FROM msdb.dbo.sysjobhistory h \n"
			    + "    INNER JOIN msdb.dbo.sysjobs j ON h.job_id = j.job_id \n"
			    + "    WHERE h.step_id = 0 \n"
//			    + "      AND msdb.dbo.agent_datetime(h.run_date, h.run_time) BETWEEN '" + startTime + "' and '" + endTime + "' \n"
			    + "      AND convert(datetime, convert(varchar(8), h.run_date)) + ' ' + stuff(stuff(right(1000000+h.run_time,6),3,0,':'),6,0,':') BETWEEN '" + startTime + "' and '" + endTime + "' \n"
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
//			    + "              ,step_start_ts      = msdb.dbo.agent_datetime(st0.run_date, jh.run_time) \n"
//				+ "              ,step_end_ts        = dateadd(second,  (jh.run_duration/10000*3600 + (jh.run_duration/100)%100*60 + jh.run_duration%100), msdb.dbo.agent_datetime(st0.run_date, jh.run_time)) \n"
			    + "              ,step_start_ts      = convert(datetime, convert(varchar(8), st0.run_date)) + ' ' + stuff(stuff(right(1000000+jh.run_time,6),3,0,':'),6,0,':') \n"
			    + "              ,step_end_ts        = dateadd(second,  (jh.run_duration/10000*3600 + (jh.run_duration/100)%100*60 + jh.run_duration%100), convert(datetime, convert(varchar(8), st0.run_date)) + ' ' + stuff(stuff(right(1000000+jh.run_time,6),3,0,':'),6,0,':')) \n"
			    + "              ,step_duration_sec  = (jh.run_duration/10000*3600 + (jh.run_duration/100)%100*60 + jh.run_duration%100) \n"
			    + "              ,sql_message_id     = jh.sql_message_id \n"
			    + "              ,sql_severity       = jh.sql_severity \n"
			    + "              ,message            = jh.message \n"
			    + "              ,query_src          = st0.query_src \n"
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
			    + "    ,sql_message_id = CASE WHEN job_history.sql_message_id = 0 THEN '' ELSE CAST(job_history.sql_message_id as varchar(10)) END \n"
			    + "    ,sql_severity   = CASE WHEN job_history.sql_severity   = 0 THEN '' ELSE CAST(job_history.sql_severity   as varchar(10)) END \n"
			    + "    ,message        = CAST(job_history.message as nvarchar(4000)) \n"
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
			    + "    ,sql_message_id     = CAST('' as varchar(10)) \n"
			    + "    ,sql_severity       = CAST('' as varchar(10)) \n"
			    + "    ,message            = CAST('' as nvarchar(4000)) \n"
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
			    + "     chart_key          = FORMAT(ja.start_execution_date, 'yyyy-MM-dd HH:mm:ss') + ' -- ' + j.name + ' - step: [' + FORMAT(ISNULL(last_executed_step_id,0)+1,'D3') + ']' \n"
			    + "    ,bar_name           = '[' + CAST(ISNULL(last_executed_step_id,0)+1 as varchar(9)) + '] ' + js.step_name \n"
			    + "    ,bar_color          = 'green' \n"
			    + "/*  ,startDate          = COALESCE(ja.last_executed_step_date, ja.start_execution_date) */ \n"
			    + "    ,startDate          = ja.last_executed_step_date \n"
			    + "    ,endDate            = NULL /*--getdate() -- OR NULL */ \n"
			    + "    ,js.step_id \n"
			    + "    ,js.subsystem \n"
			    + "    ,js.command \n"
			    + "    ,js.server \n"
			    + "    ,js.database_name \n"
			    + "    ,js.database_user_name \n"
			    + "    ,js.output_file_name \n"
			    + "    ,main_job_start_ts  = ja.start_execution_date \n"
			    + "    ,js.job_id \n"
			    + "    ,query_src          = '20' \n"
			    + "    ,sql_message_id     = CAST('' as varchar(10)) \n"
			    + "    ,sql_severity       = CAST('' as varchar(10)) \n"
			    + "    ,message            = CAST('' as nvarchar(4000)) \n"
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
	
//	private String getSql_allExecTimes_jobId_stepId()
//	{
//		String allExecTimes = "        ,allExecTimes  = 'Only available in SQL Server 2017 or later' \n";
//		DbmsVersionInfo verInfo = _conn.getDbmsVersionInfo();
//		if (verInfo != null && verInfo.getLongVersion() >= Ver.ver(2017))
//		{
//			allExecTimes = ""
//				    + "    ,allExecTimes  = STRING_AGG( \n"
//				    + "                         CAST( \n"
//				    + "                             'ts=' + convert(varchar(30), h.run_ts, 120) \n"
//				    + "                           + ', wd=' + cast(datename(weekday, h.run_ts) as char(9)) \n"
//				    + "                           + ', HMS=' + convert(varchar(10), dateadd(second, h.run_duration_sec, '2000-01-01'), 8) \n"
//				    + "                           + ', sec=' + cast(h.run_duration_sec as varchar(20)) \n"
//				    + "                           + ', status=' + h.run_status_desc \n"
//				    + "                           + ';' \n"
//				    + "                           as varchar(max)) \n"
//				    + "                       ,char(10) \n"
//				    + "                     ) WITHIN GROUP (ORDER BY h.run_ts) \n"
//					;
//		}
//
//		String sql = ""
//			    + ";WITH jobhistory as \n"
//			    + "( \n"
//			    + "    SELECT \n"
////			    + "         jh.instance_id \n" // IF THIS IS UNCOMMENT, then remember to add comma on next line
//			    + "         job_id = cast(jh.job_id as varchar(40)) \n"
//			    + "        ,jh.step_id \n"
////			    + "        ,jh.step_name \n"
////			    + "        ,jh.sql_message_id \n"
////			    + "        ,jh.sql_severity \n"
////			    + "        ,jh.message \n"
////			    + "        ,jh.run_status \n"
////			    + "        ,jh.run_date \n"
////			    + "        ,jh.run_time \n"
////			    + "        ,jh.run_duration \n"
////			    + "        ,jh.operator_id_emailed \n"
////			    + "        ,jh.operator_id_netsent \n"
////			    + "        ,jh.operator_id_paged \n"
////			    + "        ,jh.retries_attempted \n"
////			    + "        ,jh.server \n"
//			    + "        ,run_status_desc = \n"
//			    + "            CASE \n"
//			    + "                WHEN jh.run_status = 0 THEN 'FAILED' \n"
//			    + "                WHEN jh.run_status = 1 THEN 'SUCCESS' \n"
//			    + "                WHEN jh.run_status = 2 THEN 'RETRY' \n"
//			    + "                WHEN jh.run_status = 3 THEN 'CANCELED' \n"
//			    + "                WHEN jh.run_status = 4 THEN 'IN PROGRESS' \n"
//			    + "                ELSE '-UNKNOWN-' + cast(jh.run_status as varchar(10)) + '-' \n"
//			    + "            END \n"
//			    + "        ,run_ts = convert(datetime, convert(varchar(8), jh.run_date)) \n"
//			    + "                                  + ' ' \n"
//			    + "                                  + stuff(stuff(right(1000000 + jh.run_time \n"
//			    + "                                                     ,6) \n"
//			    + "                                                ,3,0,':') \n"
//			    + "                                          ,6,0,':') \n"
//			    + "        ,run_duration_sec = jh.run_duration / 10000 * 3600 \n"
//			    + "                          + jh.run_duration % 10000 / 100 * 60 \n"
//			    + "                          + jh.run_duration % 100 \n"
//			    + "    FROM msdb.dbo.sysjobhistory jh \n"
//			    + ") \n"
//			    + "SELECT \n"
//			    + "     h.job_id \n"
//			    + "    ,h.step_id \n"
//			    + allExecTimes
//			    + "FROM jobhistory h \n"
//			    + "GROUP BY h.job_id, h.step_id \n"
//			    + "ORDER BY h.job_id, h.step_id \n"
//			    + "";
//
//		return sql;
//	}

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
			// Get Name of THIS server (Get the server name, which depends on dbms/aliasName and displayName)
			String serverName = CounterController.getInstance().getServerName();

			//-----------------------------------------
			// Add modal popup for execution times of: (job_id, step_id)
			SqlServerJobScheduler.createStaticHtmlAndJavaScriptContent(writer, serverName);
			
			// add variable(s) for 'ExecTime' for all 'job_id' and 'step_id' so we can view them in detail (at a second level) modal dialog
			ReturnObject_allExecTimes ro_allExecTimes = SqlServerJobScheduler.createStaticJavaScript_lookupContent__jobId_stepId__to__allExecTimes(writer, conn, SourceType.ONLINE);
			_jobIdStepIdExecSummaryMap = ro_allExecTimes.getStatMap();

			// we also need: 'jobId' to 'name'
			SqlServerJobScheduler.createStaticJavaScript_lookupContent__jobId__to__name(writer, conn, SourceType.ONLINE);


			//---------------------------------------------------------
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

				// All rows (bars) for DbxTimeline, see DbxTimelineRows and /scripts/dbxtune/js/dbxTimeline.js
				List<Map<String, Object>> rows = new ArrayList<>();

				// Key of the "FULL JOB" row (step_id=0) for each job execution (jobId____mainJobStartTime). The steps are nested under it.
				Map<String, String> jobInstanceParentKey = new HashMap<>();

				Timestamp maxTs          = null;
				Timestamp prevRowStartTs = null;
				Timestamp prevRowEndTs   = null;

				int addRecordCount = 0;

				// Loop all rows in ResultSet
				while(rs.next())
				{
					String labelKey     = rs.getString   (1);
					String barText      = rs.getString   (2);
					String barColor     = rs.getString   (3);
					Timestamp startTs   = rs.getTimestamp(4);
					Timestamp endTs     = rs.getTimestamp(5);

					String jobId        = rs.getString("job_id");
					int    stepId       = rs.getInt   ("step_id");
					String jobStartTime = rs.getString("main_job_start_ts");

					// Truncate 'jobStartTime' at the second level (remove any milliseconds)
					if (StringUtil.hasValue(jobStartTime))
						jobStartTime = jobStartTime.substring(0, "YYYY-mm-dd HH:MM:SS".length());
					
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
					//	 int stepId = StringUtil.parseInt(extraColumns.get("step_id"), -1);
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
							
							_logger.warn("The 'endTs' is before 'startTs'... labelKey='" + labelKey + "', barText='" + barText + "', origin[startTs='" + TimeUtils.toStringYmdHms(startTs) + "', endTs='" + TimeUtils.toStringYmdHms(endTs) + "']. Adjusting 'endTs' to be 60 seconds AFTER startTs. newEndTs='" + TimeUtils.toStringYmdHms(newEndTs) + "'.");
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

					// Change Bar Color -- Based on message, sqlCode, sqlSeverity
					if (extraColumns != null)
					{
						// Do NOT override the color RED if we already decided on an ERROR
						if ( ! "red".equals(barColor) )
						{
							String sqlMessage     = StringUtil.nullToValue( extraColumns.get("message")       , "");
						//	int    sqlMessageCode = StringUtil.parseInt(    extraColumns.get("sql_message_id"), -1);
							int    sqlSeverity    = StringUtil.parseInt(    extraColumns.get("sql_severity")  , -1);
								
							if (sqlMessage.contains("ERROR-MSG: "))
								barColor = "orange";

							if (sqlSeverity > 10)
								barColor = "orange";
						}
					}

					// If no 'barColor' has been set, make it green
					if (StringUtil.isNullOrBlank(barColor))
						barColor = "green";

					if (endTs == null)
						endTs = new Timestamp(System.currentTimeMillis());

					// Change/transform the 'key' so we possibly can group "collapse" several rows (sub-tasks) on 1 row
					String origLabelKey = labelKey;
					if (StringUtil.hasValue(keyTransformFrom))
						labelKey = labelKey.replaceAll(keyTransformFrom, keyTransformTo);

					String tooltip = createUserDefinedTooltip(useDefaultTooltip, labelKey, barText, startTs, endTs, extraColumns);

					// Job and Step name (the SQL for active jobs has no '-' after the "[#]" step number)
					String jobName  = StringUtils.substringBeforeLast(StringUtils.substringAfter(origLabelKey, " -- "), " - step: [");
					String stepName = barText.replaceFirst("^\\[\\d+\\]\\s*-?\\s*", "");

					// Bar text as HTML, where the "diff" from the average execution time is marked green/orange (see 'diffColorPct')
					String barTextHtml = null;

					// Add '[HH:MM:SS]' to the barText (at least for "main" jobs)
					if (showTimeInBars)
					{
						String dhms      = "unknown";
						String statStr   = "";
						String diffClass = null;
						if (startTs != null)
						{
							long execTimeInMs  = endTs.getTime() - startTs.getTime();
							dhms = TimeUtils.msToTimeStrDHMS(execTimeInMs);

							// 50% deviation (but it will be adjusted based on the execution time)
							// - shorter     execution time (below x minutes): a higher PCT will be applied (less sensitive)
							// - medium      execution time:(below x hour):    a medium PCT will be applied (little less sensitive)
							// - long        execution time:(above x hour):    UNCHANGED PCT will be applied
							// - really long execution time:(above x hour):    a smaller PCT will be applied
							//double deviationPct = 50.0;
							// NOTE: 'deviationPct' is now a URL Parameter which is set before this loop
							
							// If it deviates to much from the average execution time... 
							// Get some text explanation that we can use in the "barText"
							statStr = getStatExecutionDeviation(jobId, stepId, (execTimeInMs/1000), _deviationPct);

							// Set some color if it deviates 
							if ( statStr.contains("SLOWER") ) barColor = "#FFC0CB"; // pink
							if ( statStr.contains("FASTER") ) barColor = "#DAF7A6"; // light green

							diffClass = getAvgDiffCssClass(jobId, stepId, (execTimeInMs/1000));
						}

						// statStr looks like ", diff: +00:38 [13.3%]..."
						if (diffClass != null && statStr.startsWith(", "))
						{
							barTextHtml = StringEscapeUtils.escapeHtml4(barText + " -- <" + dhms) + ", "
									+ "<span class='" + diffClass + "'>" + StringEscapeUtils.escapeHtml4(statStr.substring(2)) + "</span>"
									+ StringEscapeUtils.escapeHtml4(">");
						}
						barText += " -- <" + dhms + statStr + ">";
					}

					Map<String, Object> row = DbxTimelineRows.createRow(labelKey, barText, barColor, tooltip, startTs, endTs);
					if (barTextHtml != null)
						row.put("textHtml", barTextHtml);
					row.put("jobId"  , jobId);
					row.put("stepId" , stepId);
					row.put("startTs", TimeUtils.toStringYmdHms(startTs)); // used as "start time" marker in the execution history dialog
					row.put("laneKey", jobName);
					if ("red".equals(barColor))
						row.put("failed", true);

					// Nest the steps under the "FULL JOB" (step_id=0) row of the same job execution
					String jobIdInstance = jobId + "____" + jobStartTime;
					if (stepId == 0)
					{
						jobInstanceParentKey.put(jobIdInstance, labelKey);
						row.put("label", (jobStartTime != null && jobStartTime.length() >= 19 ? jobStartTime.substring(11, 19) + " " : "") + jobName);
					}
					else
					{
						String stepLabel = "[" + stepId + "] - " + stepName;
						row.put("label"       , stepLabel);
						row.put("parentKey"   , jobInstanceParentKey.get(jobIdInstance));
						row.put("laneSubKey"  , stepLabel);
						row.put("laneSubOrder", stepId);
					}
					rows.add(row);
					addRecordCount++;

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
						String tooltip = createUserDefinedTooltip(useDefaultTooltip, noActivityLabel, noActivityLabel, startTime, endTime, null);
						rows.add(DbxTimelineRows.createRow(noActivityLabel, noActivityLabel, noActivityColor, tooltip, startTime, endTime));
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
							startTs = startTime;
							endTs   = new Timestamp(startTime.getTime() + dummyTime);
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

				// Use a smaller font when there are more rows than this
				int recordThresholdForSmallerFont = 22;

				write(writer, "<script>");
				write(writer, "    // All rows: FULL JOB rows (step_id=0) and their steps (nested with 'parentKey'), see /scripts/dbxtune/js/dbxTimeline.js");
				write(writer, "    const _dbxTimelineRows = " + DbxTimelineRows.toScriptJson(rows) + ";");
				write(writer, "");
				write(writer, "    // Extra items in the click menu: the dialog with all executions of the job (step_id=0) or step (from SqlServerJobScheduler)");
				write(writer, "    function jobTimelineMenuItems(row, ctx)");
				write(writer, "    {");
				write(writer, "        if ( ! row.jobId )");
				write(writer, "            return [];");
				write(writer, "        return [{");
				write(writer, "            text:   'Show historical executions graph' + (row.stepId > 0 ? ' (step ' + row.stepId + ')' : ' (full job)'),");
				write(writer, "            action: function() { openTimeLineChartDialog_byIds(row.jobId, row.stepId, row.startTs); }");
				write(writer, "        }];");
				write(writer, "    }");
				write(writer, "");
				write(writer, "    DbxTimeline.create('timeline', _dbxTimelineRows, {");
				write(writer, "        start:          " + DbxTimelineRows.toJsTs(startTime) + ",");
				write(writer, "        end:            " + DbxTimelineRows.toJsTs(endTime)   + ",");
				write(writer, "        startExpanded:  " + (!onlyLevelZero) + ",");
				write(writer, "        showKeys:       " + showKeys + ",");
				write(writer, "        scrollToBottom: " + fillEnd + ",");
				write(writer, "        smallFontRows:  " + recordThresholdForSmallerFont + ",");
				write(writer, "        legend: [ // same colors as in getSql(), createJsTimeline() and createColorDescriptions()");
				write(writer, "            { color: 'lightblue',  text: 'FULL JOB (step_id=0)' },");
				write(writer, "            { color: 'blue',       text: 'Normal step' },");
				write(writer, "            { color: 'red',        text: 'Failed / Canceled' },");
				write(writer, "            { color: 'orange',     text: 'Retry / Warnings' },");
				write(writer, "            { color: '#FFC0CB',    text: 'Slower than normal' },");
				write(writer, "            { color: '#DAF7A6',    text: 'Faster than normal' },");
				write(writer, "            { color: 'lightgreen', text: 'Executing (job)' },");
				write(writer, "            { color: 'green',      text: 'Executing (step)' },");
				write(writer, "            { color: 'gray',       text: 'No Activity' },");
				write(writer, "            { color: '#d1e7dd',    text: 'diff: faster than avg (>= " + _diffColorPct + "%)' },");
				write(writer, "            { color: '#ffe5b4',    text: 'diff: slower than avg (>= " + _diffColorPct + "%)' }");
				write(writer, "        ],");
				write(writer, "        expandText:     'Show job steps',");
				write(writer, "        collapseText:   'Hide job steps',");
				write(writer, "        menuItems:      jobTimelineMenuItems");
				write(writer, "    });");
				if (addRecordCount <= 0)
				{
					write(writer, "");
					write(writer, "    // No records was added... show some info abount that ");
					write(writer, "    console.log('No data was added. StartTime=|" + startTime + "|, endTime=|" + endTime + "|, addRecordCount=" + addRecordCount + "'); ");
					write(writer, "    document.getElementById('dbx-job-scheduler-no-data').style.display = 'block'; ");
					write(writer, "    let tmpText  = 'No data was found for period: '; ");
					write(writer, "        tmpText += '<ul> '; ");
					write(writer, "        tmpText += '  <li>startTime: " + startTime + "</li> '; ");
					write(writer, "        tmpText += '  <li>endTime:   " + endTime   + "</li> '; ");
					write(writer, "        tmpText += '</ul> '; ");
					write(writer, "    document.getElementById('dbx-job-scheduler-no-data').innerHTML = tmpText; ");
				}
				write(writer, "");
				write(writer, "    // set the start/end time in the 'navigation bar time-lable'");
				write(writer, "    setDateRangePickerLabel('" + startTime + "', '" + endTime + "');");
				write(writer, "</script>");
			}
		}
		catch (Exception ex)
		{
			String msg = "In '" + this.getClass().getSimpleName() + "'. Problems when creating Job Scheduler Timeline. Caught: " + ex;
			if (ex instanceof SQLException)
			{
				SQLException sqlex = (SQLException) ex;
				msg = "In '" + this.getClass().getSimpleName() + "'. Problems executing SQL Statement. ErrorCode=" + sqlex.getErrorCode() + ", SQLState=" + sqlex.getSQLState() + ", Message=|" + sqlex.getMessage() + "|, SQL=|" + sql + "|.";
			}

			writer.println();
			writer.println("<!-- BEGIN: ERROR MESSAGE in Servlet ");
			writer.println(msg);
			writer.println("  -- END: ERROR MESSAGE in Servlet --> ");
			writer.println();

			_logger.error(msg);

			throw new SQLException(msg, ex);
		}
	}

	
	/**
	 * Get deviation from the Average execution time
	 * <br>
	 * NOTE: Part of the below code is also implemented in createUserDefinedTooltip(...), so if you change here, also change that code...
	 * 
	 * @param jobId
	 * @param stepId
	 * @param execTimeInSec
	 * 
	 * @return "" if no deviation, otherwise a string the can be used to present the deviation
	 */
	/**
	 * CSS class (from dbxTimeline.js) for the "diff" part of the bar text, when the execution time differs at least
	 * 'diffColorPct' percent from the average: 'dbx-tl-good' (faster) or 'dbx-tl-warn' (slower). Otherwise null.
	 * <br>
	 * Same rules as getStatExecutionDeviation() for short executions and missing averages.
	 */
	private String getAvgDiffCssClass(String jobId, int stepId, long execTimeInSec)
	{
		if (execTimeInSec < 10 || _jobIdStepIdExecSummaryMap == null)
			return null;

		StatObject statObj = _jobIdStepIdExecSummaryMap.get(jobId + "____" + stepId);
		if (statObj == null || statObj.getAvg() <= 0)
			return null;

		double diffPct = ((execTimeInSec - statObj.getAvg()) * 100.0) / statObj.getAvg();
		if (Math.abs(diffPct) < _diffColorPct)
			return null;

		return diffPct < 0 ? "dbx-tl-good" : "dbx-tl-warn";
	}

	private String getStatExecutionDeviation(String jobId, int stepId, long execTimeInSec, double basePctThresh)
	{
		if (execTimeInSec < 10)
			return "";

		if (_jobIdStepIdExecSummaryMap == null)
			return " no-avg-data";

		// also add deviation from the average
		String key = jobId + "____" + stepId;  // NOTE: Separator between 'jobId' and 'stepId' is 4 underscores 
		StatObject statObj = _jobIdStepIdExecSummaryMap.get(key);

		//Exit early if no entry was found.
		if (statObj == null)
			return " no-avg-for-id-found";

		// Get avg times 
		long   avgTimeSec = statObj.getAvg();
		String avgTimeStr = TimeUtils.secToTimeStrShort(avgTimeSec);

		long   avgDiffSec = execTimeInSec - avgTimeSec;
		String avgDiffStr = TimeUtils.secToTimeStrShort(avgDiffSec);

		// isOutlier will be true if more/less than X % deviation from the average
		//           but it's MORE Sensitive for longer execution times 
		//           and for shorter execution times we allow more deviations (bump up PCT on shorter avg executions)
//		double adjustedPct = adjustThresholdBasedOnMath(avgTimeSec, basePctThresh); // use this if you can come up with a nice algorithm for adjusting the PCT
        double adjustedPct = adjustThresholdBasedOnTime(avgTimeSec, basePctThresh);
		
		double upperBound = avgTimeSec * (1 + (adjustedPct / 100.0));
		double lowerBound = avgTimeSec * (1 - (adjustedPct / 100.0));
		
		// Are we IN RANGE or an "out-lier"
		boolean isOutlier = execTimeInSec > upperBound || execTimeInSec < lowerBound;

		double pctDeviationFromAvg = avgTimeSec == 0 ? 0.0 : ((execTimeInSec - avgTimeSec) / (avgTimeSec*1.0)) * 100.0;
		pctDeviationFromAvg = Math.abs(pctDeviationFromAvg); // ABS to make it positive number all the time
		pctDeviationFromAvg = MathUtils.round(pctDeviationFromAvg, 1);
		
		String debugStr = "";
		if (_logger.isDebugEnabled() || _debugMode)
		{
			debugStr = ", DEBUG: basePct=" + basePctThresh + ", adjustedPct=" + adjustedPct + " -- isOutlier=" + isOutlier + ": execTimeInSec=" + execTimeInSec + ", avgTimeSec=" + avgTimeSec + ", lowerBound=" + MathUtils.round(lowerBound,2) + ", upperBound=" + MathUtils.round(upperBound,2);

			_logger.debug("getStatExecutionDeviation(jobId='" + jobId + "', stepId=" + stepId + ", basePct=" + basePctThresh + "): "
					+ "---- isOutlier=" + isOutlier + ": execTimeInSec=" + execTimeInSec + ", avgTimeSec=" + avgTimeSec 
					+ " ::: lowerBound=" + MathUtils.round(lowerBound,2) + ", upperBound=" + MathUtils.round(upperBound,2) + ", adjustedPct=" + adjustedPct);
		}

		String sign = "+";
		if (avgDiffSec < 0)
			sign = "-";
		
		String outlierNote = "";
		if (isOutlier)
		{
			if (avgDiffSec < 0)
				outlierNote = " FASTER";
			else
				outlierNote = " ***SLOWER***";
		}
		
		// Add "sign" and Percent to: avgDiffStr
		avgDiffStr = sign + avgDiffStr + " [" + pctDeviationFromAvg + "%]";

//		return ", diff: " + avgDiffStr + outlierNote + ", avg: " + avgTimeStr + debugStr;
		return ", diff: " + avgDiffStr + outlierNote + debugStr;
	}

	/** 
	 * Simple adjustment based on the average time
	 * <p>
	 * Allow larger deviation for shorter execution times.
	 * The basePctThresh will be multiplied X time based on time
	 */
	private static double adjustThresholdBasedOnTime(double avgTimeSec, double basePctThresh)
	{
		// Less than 1 minute
		if ( avgTimeSec < 60 )
		{ 
			return basePctThresh * 4;
		}

		// Between 1 and 10 minutes
		if ( avgTimeSec < 600 )
		{ 
			return basePctThresh * 3;
		}

		// Between 10 minutes and 1 hour
		if ( avgTimeSec < 3600 )
		{ 
			return basePctThresh * 2;
		}

		// Between 1 hour and 2 hours
		if ( avgTimeSec < 3600 * 2 )
		{ 
			return basePctThresh * 1; // 
		}

		// Between 2 hour and 4 hours
		if ( avgTimeSec < 3600 * 2 )
		{ 
			return basePctThresh * 0.75; // 
		}

		// Above 4 hours
		return basePctThresh * 0.5;
	}

//	/** Use some "math" to calculate the new adjustedPct */ 
//	private static double adjustThresholdBasedOnMath(double avgTimeSec, double basePctThresh)
//	{
//		return Math.log10(avgTimeSec + 1) * 2;
////		return Math.log10(avgTimeSec + 1) / Math.log10(3600 + 1);
//	}
	

	private String createUserDefinedTooltip(boolean useDefaultTooltip, String label, String barText, Timestamp startTs, Timestamp endTs, Map<String, String> extraColumns)
	{
		if (useDefaultTooltip)
			return null;
		
		SimpleDateFormat ymd       = new SimpleDateFormat("yyyy-MM-dd");
		SimpleDateFormat hms       = new SimpleDateFormat("HH:mm:ss");
		SimpleDateFormat dayOfWeek = new SimpleDateFormat("EEEE");

		String diffDurationTT = "";
		String avgDurationTT  = "";
		String deviationPctTT = "";
		if (extraColumns != null && !extraColumns.isEmpty())
		{
			String jobId  = extraColumns.get("job_id");
			String stepId = extraColumns.get("step_id");
			
			String key = jobId + "____" + stepId;  // NOTE: Separator between 'jobId' and 'stepId' is 4 underscores 
			StatObject statObj = _jobIdStepIdExecSummaryMap.get(key);
			
			if (statObj != null)
			{
				long execTimeInSec = -1;
				if (startTs != null && endTs != null)
					execTimeInSec = (endTs.getTime() - startTs.getTime()) / 1000;

				String avgTimeStr2 = " (based of " + statObj.getCount() + " executions, max=" + TimeUtils.secToTimeStrHMS(statObj.getMax()) + ", min=" + TimeUtils.secToTimeStrHMS(statObj.getMin()) + ")";

				// Get avg times 
				long   avgTimeSec = statObj.getAvg();
				String avgTimeStr = TimeUtils.secToTimeStrHMS(avgTimeSec);

				long   avgDiffSec = execTimeInSec - avgTimeSec;
				String avgDiffStr = TimeUtils.secToTimeStrHMS(avgDiffSec);

		        double adjustedPct = adjustThresholdBasedOnTime(avgTimeSec, _deviationPct);
				
				double upperBound = avgTimeSec * (1 + (adjustedPct / 100.0));
				double lowerBound = avgTimeSec * (1 - (adjustedPct / 100.0));
				
				// Are we IN RANGE or an "out-lier"
				boolean isOutlier = execTimeInSec > upperBound || execTimeInSec < lowerBound;

				double pctDeviationFromAvg = avgTimeSec == 0 ? 0.0 : ((execTimeInSec - avgTimeSec) / (avgTimeSec*1.0)) * 100.0;
				pctDeviationFromAvg = Math.abs(pctDeviationFromAvg); // ABS to make it positive number all the time
				pctDeviationFromAvg = MathUtils.round(pctDeviationFromAvg, 1);
				
				String sign = " (<span style='color:red'>slower</span> by " + pctDeviationFromAvg + "%)";
				if (avgDiffSec < 0)
					sign = " (<span style='color:green'>faster</span> by " + pctDeviationFromAvg + "%)";
				
				String outlierNote = "";
				if (isOutlier)
				{
					if (avgDiffSec < 0)
						outlierNote = " FASTER";
					else
						outlierNote = " ***SLOWER***";
				}

				String deviationInfo = ""
						+ "isOutlier="      + isOutlier + outlierNote
						+ ", deviationPct=" + MathUtils.round(_deviationPct, 1)
						+ ", adjustedPct="  + MathUtils.round(adjustedPct, 1)
						;
				
				
				avgDurationTT  = "<tr> <td nowrap><b>Avg Duration:   </b></td> <td nowrap>" + "<code>" + avgTimeStr + "</code>" + avgTimeStr2 + "</td> </tr>";
				diffDurationTT = "<tr> <td nowrap><b>Diff Avg Dur:   </b></td> <td nowrap>" + "<code>" + avgDiffStr + "</code>" + sign        + "</td> </tr>";
				deviationPctTT = "<tr> <td nowrap><b>Deviation Info: </b></td> <td nowrap>" + deviationInfo                                   + "</td> </tr>";
			}
		}
		
		String tooltip = ""
				+ "<div style='padding:10px; white-space:nowrap; font-size:12px; font-family:Arial;'>"
				+ "<b>" + label + "</b>"
				+ "<hr>"
				+ "<b>" + barText + "</b>"
				+ "<hr>"
				+ "<table style='font-size:12px; font-family:Arial;'>" 
				+ "<tr> <td nowrap><b>Duration: </b></td> <td nowrap>" + calculateDuration(startTs, endTs) + "</td> </tr>"
				+ avgDurationTT
				+ diffDurationTT
				+ deviationPctTT
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
				{
					String subsystem   =  "" + extraColumns.get("subsystem");
					int    msgNumber   = StringUtil.parseInt(extraColumns.get("sql_message_id"), -1);
					int    msgSeverity = StringUtil.parseInt(extraColumns.get("sql_severity"  ), -1);

//					ttVal = tooltipForMessage(ttVal);
					ttVal = formatMessageString(ttVal, "<BR>", subsystem, msgNumber, msgSeverity);
				}

				// Hack to get a separator in before column 'main_job_start_ts'
				if (ttKey.equals("main_job_start_ts"))
					tooltip += "<tr> <td>&nbsp;</td> <td>&nbsp;</td> </tr>";
				
				tooltip += "<tr> <td nowrap><b>" + ttKey + ": </b></td> <td nowrap " + tdAttr + ">" + ttVal + "</td> </tr>";

				// Hack to get a separator in before column 'query_src'
				if (ttKey.equals("query_src"))
					tooltip += "<tr> <td>&nbsp;</td> <td><hr></td> </tr>";
			}
		}
		
		tooltip += ""
				+ "</table>"
				+ "<br>"
				+ "</div>";
		
		return tooltip;
	}

//	/**
//	 * Try to make messages a bit more readable<br>
//	 *  - If it's to long try to add NEWLINE somewhere
//	 *  - NewLine on some special words
//	 *  - If it looks like a "console log line" add newlines
//	 * @param ttVal
//	 * @return
//	 */
//	private static String tooltipForMessage(String ttVal)
//	{
//		if (ttVal == null)
//			return null;
//		
//		if (ttVal.length() < 64)
//			return ttVal;
//		
//		// "Executed as user: MAXM\\goran.schwarz. " -->> "Executed as user: MAXM\\goran.schwarz. <BR>"
//		ttVal = ttVal.replaceFirst("Executed as user: \\S* ", "$0<BR>");
//		
//		// Console messages
//		ttVal = ttVal.replace("DEBUG   - ", "<BR>DEBUG   - ");
//		ttVal = ttVal.replace("INFO    - ", "<BR>INFO    - ");
//		ttVal = ttVal.replace("WARNING - ", "<BR>WARNING - ");
//		ttVal = ttVal.replace("ERROR   - ", "<BR>ERROR   - ");
//
//		// Some Error messages
//		ttVal = ttVal.replace("ERROR-MSG: ", "<BR>ERROR-MSG: ");
//		
//		ttVal = ttVal.replace("Warning! ", "<BR>Warning! ");
//		ttVal = ttVal.replace("Warning: ", "<BR>Warning: ");
//
//		ttVal = ttVal.replace("Process Exit Code ", "<BR>Process Exit Code ");
//		
//		ttVal = ttVal.replace("The step failed."   , "<BR>The step failed."); 
//		ttVal = ttVal.replace("The step succeeded.", "<BR>The step succeeded.");
//		
//		// Remove any "double newlines"
//		ttVal = ttVal.replace("<BR><BR>", "<BR>");
//		
//		return ttVal;
//	}
	/**
	 * Try to make messages a bit more readable<br>
	 *  - If it's to long try to add NEWLINE somewhere
	 *  - NewLine on some special words
	 *  - If it looks like a "console log line" add newlines
	 * @param msg            The input
	 * @param newline        What is the String for newline
	 * @param subsystem      
	 * @param msgNumber 
	 * @param msgSeverity    
	 * @return
	 */
	private static String formatMessageString(String msg, String newline, String subsystem, int msgNumber, int msgSeverity)
	{
		if (msg == null)
			return null;
		
		if (msg.length() < 64)
			return msg;
		
		// "Executed as user: MAXM\\goran.schwarz. " -->> "Executed as user: MAXM\\goran.schwarz. <BR>"
		msg = msg.replaceFirst("Executed as user: \\S* ", "$0" + newline);
		
		// Console messages
		msg = msg.replace("DEBUG   - ",          newline + "DEBUG   - ");
		msg = msg.replace("INFO    - ",          newline + "INFO    - ");
		msg = msg.replace("WARNING - ",          newline + "WARNING - ");
		msg = msg.replace("ERROR   - ",          newline + "ERROR   - ");

		// Some Error messages
		msg = msg.replace("ERROR-MSG: ",         newline + "ERROR-MSG: ");
		
		// Make a newline *after* '[SQLSTATE #####] (Message ####)'
		msg = msg.replaceAll("\\[SQLSTATE \\d+\\] \\(Message \\d+\\)", "$0" + newline); 

		msg = msg.replace("Process Exit Code ",  newline + "Process Exit Code ");
		
		msg = msg.replace("The step failed."   , newline + "The step failed."); 
		msg = msg.replace("The step succeeded.", newline + "The step succeeded.");
		
		// Remove any "double newlines" (that only contains a period '.' char) 
		msg = msg.replace(newline + ".  " + newline, newline);
		
		// Remove any "double newlines"
		msg = msg.replace(newline + newline, newline);
		
		// If it's a T-SQL subsystem, lets remove some "stuff"
		if ("TSQL".equals(subsystem))
		{
			// SQLSTATE description: https://en.wikipedia.org/wiki/SQLSTATE
			
			// Simplify message: "[SQLSTATE 01000] (Message 50000)" -> "" (empty string)
			msg = msg.replaceAll("\\[SQLSTATE 01\\d+\\] \\(Message \\d+\\)", ""); // Remove any of the "warning" SQL STATES
		}
		
		return msg;
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

//		if ( days == 0 )
//			res += String.format("    &emsp;<code>%02d:%02d:%02d</code>", hours, minutes, seconds);
//		else
//			res += String.format("    &emsp;<code>%dd %02d:%02d:%02d</code>", days, hours, minutes, seconds);
		if ( days == 0 )
			res = String.format("<code>%02d:%02d:%02d</code> ("     + res.trim() + ")", hours, minutes, seconds);
		else
			res = String.format("<code>%dd %02d:%02d:%02d</code> (" + res.trim() + ")", days, hours, minutes, seconds);

		return res;
	}
}
