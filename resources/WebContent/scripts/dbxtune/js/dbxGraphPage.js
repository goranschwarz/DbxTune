/**
 * dbxGraphPage.js — graph.html page controller
 *
 * Contains page initialisation (window.onload), date-range picker,
 * dark-mode toggle, filter dialog, and history-mode / timeline slider logic.
 *
 * Loaded by graph.html only, after all dependency scripts.
 */

		// used to hold Graph Filter "objects" for the Filter Dialog (initialized firt time the "filter" button is pressed)
		var _filterDialogContentArr = [];

		window.onload = function ()
		{
			// Do we support the browser...
			// Get version of MS: IE or Edge
			// IE or EDGE are not supported
			var ieVersion = detectIE();
			if (ieVersion !== false)
			{
				var browserVersion = ((ieVersion >= 12) ? 'Edge ' : 'IE ') + ieVersion;
				document.getElementById('api-feedback').innerHTML = "Current Browser: <b>" + browserVersion + "</b> may not work as expected. Please use Chrome, Firefox or Safari";
				// document.getElementById('api-feedback').innerHTML = "Current Browser: <b>" + browserVersion + "</b> is not supported. Please use Chrome, Firefox or Safari";
				// alert("Current Browser: " + browserVersion + " is not supported. Please use Chrome, Firefox or Safari");
				//return;
				if (ieVersion < 12)
				{
					document.getElementById('api-feedback').innerHTML = "<font color='red'>Current Browser: <b>" + browserVersion + "</b> is not supported. Please use Chrome, Firefox or Safari</font>";
					alert("Current Browser: " + browserVersion + " is not supported. Please use Chrome, Firefox or Safari");
					// return;
				}
			}

			// Get browsers window size
			console.log("Browser Window size: width="+$(window).width()+", height="+$(window).height());

			// handle parameters: gwidth & gcols
			// gwidth = is set as a approx width for the graphs, and then lets the grid-layout handler decide "how" it will be displayed
			// gcols  = on the current browser size, calculate what 'gwidth' should be be (gwidth = browserWindowWidth / gcols)
			var gwidth = getParameter("gwidth", 650);
			var gcols  = getParameter("gcols");
			if (gcols !== undefined)
			{
				console.log("gcols="+gcols);
				gwidth = $(window).width() / gcols -10;
			}
			console.log("gwidth="+gwidth);

			// grid thresholds: xs > 0px, sm > 640px, md > 860px, lg > 1080px, xl > 1300px
			// $('#graphs').css({
			//     "grid-gap": "0px",
			//     "--cols-xs": "1",
			//     "--cols-sm": "1",
			//     "--cols-md": "1",
			//     "--cols-lg": "2",
			//     "--cols-xl": "auto-fit", "--cols-size-xl": "minmax("+gwidth+"px, 1fr)"
			// });
			$('#graphs').css({
				"grid-gap": "0px",
				"--cols-xs": "1",
				"--cols-sm": "auto-fit", "--cols-size-sm": "minmax("+gwidth+"px, 1fr)",
				"--cols-md": "auto-fit", "--cols-size-md": "minmax("+gwidth+"px, 1fr)",
				"--cols-lg": "auto-fit", "--cols-size-lg": "minmax("+gwidth+"px, 1fr)",
				"--cols-xl": "auto-fit", "--cols-size-xl": "minmax("+gwidth+"px, 1fr)"
			});



			/**
			 * Initialize: Date Range Picker
			 */
			// var start = moment().subtract(2, 'hours');
			// var end = moment();
			function dateRangePickerCb(start, end, label, picker)
			{
				console.log("CALLBACK: start="+start+", end="+end+", label="+label);
				// alert("CALLBACK: start="+start+", end="+end+", label="+label);

				if (label === "Last 2 Hours" || label === "Last 4 Hours" || label === "Last 6 Hours" || label === "Last 8 Hours" || label === "Last 12 Hours" || label === "Last 24 Hours" || label === "Today")
				{
					const url = new URL(window.location.href);
					var   startTime = "2h";

					if      (label === "Last 2 Hours")  startTime = "2h";
					else if (label === "Last 4 Hours")  startTime = "4h";
					else if (label === "Last 6 Hours")  startTime = "6h";
					else if (label === "Last 8 Hours")  startTime = "8h";
					else if (label === "Last 12 Hours") startTime = "12h";
					else if (label === "Last 24 Hours") startTime = "24h";
					else if (label === "Today")         startTime = start.format('YYYY-MM-DD HH:mm');

					url.searchParams.set("startTime", startTime);
					url.searchParams.set("subscribe", true);
					url.searchParams.delete("endTime");

					console.log("window.location.ref: "+url);
					window.location.assign(url);
				}
				else if (label === "Start Time + 2H" || label === "Start Time + 4H" || label === "Start Time + 8H")
				{
					// Get current start/end Time from 'the button label' #dbx-report-range)
					var currentStartTime = '';
					var currentEndTime   = '';
					const currentStartEndDate = $('#dbx-report-range span').html().split(" - ");
					if (currentStartEndDate.length === 2)
					{
						currentStartTime = parseDate(currentStartEndDate[0]);
						currentEndTime   = parseDate(currentStartEndDate[1]);
					}

					var   startTime = currentStartTime.format('YYYY-MM-DD HH:mm');
					var   endTime   = '2h';

					if      (label === "Start Time + 2H")  endTime = '2h';
					else if (label === "Start Time + 4H")  endTime = '4h';
					else if (label === "Start Time + 8H")  endTime = '8h';
					
					const url = new URL(window.location.href);

					url.searchParams.set("startTime", startTime);
					url.searchParams.set("endTime",   endTime);
					url.searchParams.set("subscribe", false);

					console.log("window.location.ref: "+url);
					window.location.assign(url);
				}
				else
				{
					const url = new URL(window.location.href);
					var   startTime = start.format('YYYY-MM-DD HH:mm');
					var   endTime   = end  .format('YYYY-MM-DD HH:mm');

					url.searchParams.set("startTime", startTime);
					url.searchParams.set("endTime",   endTime);
					url.searchParams.set("subscribe", false);

					console.log("window.location.ref: "+url);
					window.location.assign(url);
				}

				//$('#dbx-report-range span').html(start.format('YYYY-MM-DD HH:mm') + ' - ' + end.format('YYYY-MM-DD HH:mm'));
				if (moment(start).isSame(moment(), 'day') && moment(end).isSame(moment(), 'day'))
					$('#dbx-report-range span').html(start.format('HH:mm') + ' - ' + end.format('HH:mm'));
				else
					$('#dbx-report-range span').html(start.format('YYYY-MM-DD HH:mm') + ' - ' + end.format('YYYY-MM-DD HH:mm'));
			}

			$('#dbx-report-range').daterangepicker({
				// startDate: start,
				// endDate: end,
				startDate: moment().subtract(2, 'hours'),
				endDate:   moment(),
				timePicker: true,
				timePicker24Hour: true,
				timePickerIncrement: 5,
				ranges: {
					'Last 2 Hours':    [moment().subtract(2,  'hours'), moment()],
					'Last 4 Hours':    [moment().subtract(4,  'hours'), moment()],
					'Last 6 Hours':    [moment().subtract(6,  'hours'), moment()],
					'Last 8 Hours':    [moment().subtract(8,  'hours'), moment()],
					'Last 12 Hours':   [moment().subtract(12, 'hours'), moment()],
					'Last 24 Hours':   [moment().subtract(24, 'hours'), moment()],
					'Start Time + 2H': [moment(), moment()],
					'Start Time + 4H': [moment(), moment()],
					'Start Time + 8H': [moment(), moment()],
					'Today':           [moment().startOf('day'), moment().endOf('day')],
					'Yesterday':       [moment().startOf('day').subtract(1,  'days'), moment().endOf('day').subtract(1, 'days')],
					'This Week':       [moment().startOf('isoWeek'), moment().endOf('isoWeek')],
					'Last 7 Days':     [moment().startOf('day').subtract(7,  'days'), moment().endOf('day')],
					'Last 2 Weeks':    [moment().startOf('day').subtract(14, 'days'), moment().endOf('day')],
					'Last 4 Weeks':    [moment().startOf('day').subtract(28, 'days'), moment().endOf('day')],
					'Last 30 Days':    [moment().startOf('day').subtract(30, 'days'), moment().endOf('day')],
					'This Month':      [moment().startOf('month'), moment().endOf('month')],
					'Last Month':      [moment().subtract(1, 'month').startOf('month'), moment().subtract(1, 'month').endOf('month')]
				}
			});
			// }, dateRangePickerCb); // Using the callback caused "callback to be called" when user clickes "outside the picker"... su use elow 'apply.daterangepicker' instead

			// On daterangepicker-APPLY
			$('#dbx-report-range').on('apply.daterangepicker', function(ev, picker) {
				dateRangePickerCb(picker.startDate, picker.endDate, picker.chosenLabel, picker);
			});

			// On daterangepicker-OPEN, set some start/end date
			$('#dbx-report-range').on('show.daterangepicker', function(ev, picker) {
				var startEndDate = $('#dbx-report-range span').html().split(" - ");
				if (startEndDate.length === 2)
				{
					console.log("on 'dbx-report-range': show(): setting: startDate=|"+startEndDate[0]+"|, endDate=|"+startEndDate[1]+"|.");
					picker.startDate = parseDate(startEndDate[0]);
					picker.endDate   = parseDate(startEndDate[1]);
				}

				// TODO: if graph[0] has a "timeline marker", then set the surounding time of 2 hours...
			});

			// dateRangePickerCb(start, end);

			// Install functions for buttons: dbx-prev-time, dbx-next-time
			$("#dbx-prev-day").click( function() {
				setNextPrevDay('prev');
			});
			$("#dbx-prev-time").click( function() {
				setNextPrevTime('prev');
			});
			$("#dbx-next-time").click( function() {
				setNextPrevTime('next');
			});
			$("#dbx-next-day").click( function() {
				setNextPrevDay('next');
			});

			function parseDate(dateString)
			{
				var longRegEx = /^\d{4}-\d{2}-\d{2} \d{2}:\d{2}/;
				if (dateString.match(longRegEx) != null)
					return moment(dateString);

				var shortRegEx = /^\d{2}:\d{2}/;
				if (dateString.match(shortRegEx) != null)
					return moment(dateString, 'HH:mm');

				var msg = "parseDat(): unknown date '"+dateString+"' was passed. do not match 'YYYY-MM-DD HH:mm' or 'HH:mm'";
				console.log(msg);
				return msg;
			}

			function setNextPrevTime(type)
			{
				var currentHourSpan = parseInt( $("#dbx-sample-time").html().replace("h", "") );
				if (isNaN(currentHourSpan))
					return;
				if (currentHourSpan === 0)
					currentHourSpan = 1;

				var startEndDate = $('#dbx-report-range span').html().split(" - ");
				if (startEndDate.length === 2)
				{
					console.log("button: "+type+"-time: currentHourSpan=|"+currentHourSpan+"|, startDate=|"+startEndDate[0]+"|, endDate=|"+startEndDate[1]+"|.");
					var startDate = parseDate(startEndDate[0]);
					var endDate   = parseDate(startEndDate[1]);

					if (type === "prev") {
						startDate = startDate.subtract(currentHourSpan, 'hours');
						endDate   = endDate  .subtract(currentHourSpan, 'hours');
					}
					else if (type === "next") {
						startDate = startDate.add(currentHourSpan, 'hours');
						endDate   = endDate  .add(currentHourSpan, 'hours');
					}
					console.log("button: "+type+"-time: AFTER adjust: currentHourSpan=|"+currentHourSpan+"|, startDate=|"+startDate.format('YYYY-MM-DD HH:mm')+"|, endDate=|"+endDate.format('YYYY-MM-DD HH:mm')+"|.");

					// USE: the daterangepicker callback to set START/END time
					dateRangePickerCb(startDate, endDate, type);
				}
			}

			function setNextPrevDay(type)
			{
//				var currentHourSpan = parseInt( $("#dbx-sample-time").html().replace("h", "") );
//				if (isNaN(currentHourSpan))
//					return;
//				if (currentHourSpan === 0)
//					currentHourSpan = 1;

				var startEndDate = $('#dbx-report-range span').html().split(" - ");
				if (startEndDate.length === 2)
				{
					var startDate = parseDate(startEndDate[0]);
					var endDate   = parseDate(startEndDate[1]);

					if (type === "prev") {
						startDate = startDate.subtract(1, 'days');
						endDate   = endDate  .subtract(1, 'days');
					}
					else if (type === "next") {
						startDate = startDate.add(1, 'days');
						endDate   = endDate  .add(1, 'days');
					}
					console.log("button: "+type+"-time: AFTER adjust: startDate=|"+startDate.format('YYYY-MM-DD HH:mm')+"|, endDate=|"+endDate.format('YYYY-MM-DD HH:mm')+"|.");

					// USE: the daterangepicker callback to set START/END time
					dateRangePickerCb(startDate, endDate, type);
				}
			}

			// Install functions for button: dbx-filter
			$("#dbx-filter").click( function()
			{
				dbxOpenFilterDialog();
			});

			// Install functions for button: dbx-history (on/off)
			$("#dbx-history-on") .click( function() { dbxHistoryAction(); });
			$("#dbx-history-off").click( function() { dbxHistoryAction(); });

			// Make Bootsrap Modal Dialogs, resizable & movable (on the head)
			var isSafari = !!navigator.userAgent.match(/Version\/[\d\.]+.*Safari/);
			var iOS = /iPad|iPhone|iPod/.test(navigator.userAgent) && !window.MSStream;
			if ( ! (isSafari && iOS) )
			{
				// ── Dialog geometry persistence (localStorage) ────────────────────
				function _saveDialogGeom(id) {
					var s = document.getElementById(id).style;
					localStorage.setItem('dbx.geom.' + id, JSON.stringify(
						{ top: s.top, left: s.left, width: s.width, height: s.height }
					));
				}
				function _restoreDialogGeom(id) {
					try {
						var g = JSON.parse(localStorage.getItem('dbx.geom.' + id) || 'null');
						if (!g) return;
						var s = document.getElementById(id).style;
						if (g.top)    s.top    = g.top;
						if (g.left)   s.left   = g.left;
						if (g.width)  s.width  = g.width;
						if (g.height) s.height = g.height;
					} catch(e) {}
				}

				// All modals dialogs
				$('.modal-content').resizable({ alsoResize: ".modal-dialog" });
				$('.modal-dialog').draggable({ handle: ".modal-header" });

				// Statements
				$('.active-statements-class').resizable({ handles: 'n, w, nw, s, sw', stop: function() { _saveDialogGeom('active-statements'); } });
//				$('.active-statements-class').resizable({ handles: 'all' });
				$('.active-statements-class').draggable({ handle: ".active-statements-ctl-class", stop: function() { _saveDialogGeom('active-statements'); } });
				_restoreDialogGeom('active-statements');

				// Active Alarms
				$('.active-alarms-class').resizable({ handles: 'n', stop: function() { _saveDialogGeom('active-alarms'); } });
				$('.active-alarms-class').draggable({ handle: ".active-alarms-ctl-class", stop: function() { _saveDialogGeom('active-alarms'); } });
				_restoreDialogGeom('active-alarms');

				// Alarm Panel
				$('.alarm-panel-class').resizable({ handles: 'n, e, s, w, ne, nw, se, sw', stop: function() { _saveDialogGeom('alarm-panel'); } });
				$('.alarm-panel-class').draggable({ handle: ".alarm-panel-hdr-class", stop: function() { _saveDialogGeom('alarm-panel'); } });
				_restoreDialogGeom('alarm-panel');

				// CM Detail Panel
				$('.cm-detail-class').resizable({ handles: 'n, e, s, w, ne, nw, se, sw', stop: function() { _saveDialogGeom('cm-detail-panel'); } });
				$('.cm-detail-class').draggable({ handle: ".cm-detail-ctl-class", stop: function() { _saveDialogGeom('cm-detail-panel'); } });
				_restoreDialogGeom('cm-detail-panel');

				// DBMS Config Panel
				$('.dbms-config-class').resizable({ handles: 'n, e, s, w, ne, nw, se, sw', stop: function() { _saveDialogGeom('dbms-config-panel'); } });
				$('.dbms-config-class').draggable({ handle: ".dbms-config-ctl-class", stop: function() { _saveDialogGeom('dbms-config-panel'); } });
				_restoreDialogGeom('dbms-config-panel');

				// Bring-to-front on click — last clicked panel always wins z-order
				// window._dbxTopZ is global so alarmDetailShowModal() can bump modal above panels
				window._dbxTopZ = 910;
				$('#active-statements, #cm-detail-panel, #dbms-config-panel, #alarm-panel').on('mousedown', function() {
					$(this).css('z-index', ++window._dbxTopZ);
				});
			}

			$('#dbx-filter-dialog').on('show.bs.modal', function () {
				$(this).find('.modal-body').css({
					'max-height':'100%'
				});
			});

			// What should happen when we click OK in the dialog
			$("#dbx-filter-dialog-ok").click( function()
			{
				$('#dbx-filter-dialog').modal('hide');

				var selectedRecords = $('#dbx-graph-filter-table').bootstrapTable('getSelections');

				// hide ALL graphs
				for(let i=0; i<_graphMap.length; i++)
				{
					const dbxGraph = _graphMap[i];
					var x = document.getElementById(dbxGraph.getFullName());
					x.style.display = 'none';
					// console.log('HIDE: x='+x, x);
				}
				// show marked ones ALL graphs
				for (let i = 0; i < selectedRecords.length; i++)
				{
					const record = selectedRecords[i];
					var x = document.getElementById(record.fullName);
					x.style.display = 'block';
					// console.log('SHOW: x='+x, x);
				}
			});

			/**
			 * LOAD DBXTUNE GRAPHS/CHARTS
			 */
			dbxTuneLoadCharts("graphs");

			// Check if we got any SQL Statements that are active
			dbxTuneCheckActiveStatements();

			// Check if we got any alarms that are active
			dbxTuneCheckActiveAlarms();

			/* Start to subscribe to data, sent by the backend-server */
			dbxTuneGraphSubscribe();
			
//			/* If we have 'markTime' then enable historical mode, and set the position in the timeline */
//			var markTime = getParameter("markTime");
//			if (markTime !== undefined)
//			{
//				// THE BELOW IS UGGLY... Sleep for a while before kicking off the history load
//				// TODO: Change this to be executed when we *know* we have data in the proper structures
//				setTimeout(function(){ console.log('ENABLE HISTORY MODE'); dbxHistoryAction(); }, 1000);
//				setTimeout(function(){ console.log('SET TIME IN HISTORY MODE: markTime=|' + markTime + '|'); dbxHistoryAction(markTime); }, 2000);
//			}
		}
		function scrollToFormatter(value, row, index)
		{
			// console.log("scrollToFormatter: index="+index, row);
			// return '<a href="javascript:void(0)" onclick="scrollToAction('+ row.fullName +')">GoTo</a>';
			return '<a href="javascript:void(0)" onclick="scrollToAction('+ row.fullName +')"><span class="glyphicon fa fa-arrow-circle-right"></span></a>';
		}
		function scrollToAction(name)
		{
			console.log("scrollToAction: name="+name, name);
			$('#dbx-filter-dialog').modal('hide');
			// name.scrollIntoView();
			var topOfElement = name.offsetTop - 150;
			// window.scroll({ top: topOfElement, behavior: "smooth" });
			window.scroll({ top: topOfElement });

			// $(name).scrollTo(150);
			// $('html, body').animate({
			//     scrollTop: ($(name).offset().top)
			// },500);
			$(name).effect( "bounce", {times:5}, 1000 );
		}
		function scrollToDisableCheckRow(element)
		{
			console.log("scrollToDisableCheckRow: element="+element, element);
			return $.inArray(element.tagName, ['A', 'BUTTON']);
		}

		function dbxOpenFilterDialog()
		{
			// loop all available graphs and add it to the table in the dialog
			if (_filterDialogContentArr.length === 0)
			{
				for(let i=0; i<_graphMap.length; i++)
				{
					const dbxGraph = _graphMap[i];

					var row = {
						"visible"   : true,
						"desc"      : dbxGraph.getGraphLabel(),
						"type"      : dbxGraph.getGraphCategory(),
						"cm"        : dbxGraph.getCmName(),
						"graphName" : dbxGraph.getGraphName(),
						"fullName"  : dbxGraph.getFullName(),
					};
					_filterDialogContentArr.push(row);
				}

				$('#dbx-graph-filter-table').bootstrapTable({data: _filterDialogContentArr});
			}

			// Show the dialog
			$("#dbx-filter-dialog").modal('show');
		}

		/**
		 * Get History Array position for a specififc time
		 * returns -1 if not found
		 */
		function getHistoryArrayPosForTs(momentTs)
		{
			if (_lastHistoryMomentsArray === undefined) return -1;
			if (_lastHistoryMomentsArray === null     ) return -1;

			let momentsArray  = _lastHistoryMomentsArray;
			let momentTsEpoch = momentTs.valueOf();

			for (var m=0; m<momentsArray.length; m++)
			{
				let startTime = momentsArray[m].valueOf();
				let endTime   = momentsArray[momentsArray.length-1].add(1, 'minute'); // set one minute past the "last" array time
				if (m + 1 < momentsArray.length)
					endTime = momentsArray[m + 1].valueOf();

				if (momentTsEpoch >= startTime && momentTsEpoch < endTime)
				{
					console.log("######################## getHistoryArrayPosForTs(ts=|" + momentTs + "|) #### FOUND-POS ################ --m=" + m);
					return m;
				}
			}
			return -1;
		}

		// Used to deferr calling of "loadHistory" on slider "change event"
		var _historyLoadDeferredTimerId = -1;

		function dbxHistoryAction(momentTs)
		{
			var isHistoryViewEnabled = $("#dbx-history-container-div").is(":visible");

			console.log("dbxHistoryAction(momentTs=|" + momentTs + "|): isHistoryViewEnabled=" + isHistoryViewEnabled);

			// Change to NORMAL MODE
			if (isHistoryViewEnabled && momentTs === undefined)
			{
				console.log("HISTORY: is HIDDEN... reloading page");

				// Reload current page to get in "normal" mode again
				location.reload();
				return;
			}

			// Update Slider pos to a specififc timestamp
			// and *fire* the "change" event on the slider
			if (isHistoryViewEnabled && momentTs !== undefined)
			{
				// ensure that it's a moment object
				if (typeof momentTs === 'string' || momentTs instanceof String)
				{
					momentTs = momentTs.replace("+", " "); // If the date string contain '+' instead of ' '
					momentTs = moment(momentTs);
				}
				
				let toArrayPos = getHistoryArrayPosForTs(momentTs);
				if (toArrayPos > 0)
				{
					let sliderPos = toArrayPos + 1;
					console.log("DEBUG: set the slider to value. toArrayPos=" + toArrayPos + ", sliderPos=" + sliderPos);

					var dbxHistoryTimelineSlider = document.getElementById('dbx-history-timeline-slider');

					dbxHistoryTimelineSlider.value = sliderPos;
					dbxHistoryTimelineSlider.dispatchEvent( new Event('change') );
				}
				return;
			}

			// Change to HISTORY MODE
			if (true)
			{
				console.log("HISTORY: is VISIBLE");

				// Show the dialog
				$("#dbx-history-container-div").toggle();

				// Toogle the buttons
				$("#dbx-history-on") .toggle();
				$("#dbx-history-off").toggle();

				// HIDE alarms
				$("#active-alarms-top").hide();
				$("#active-alarms")    .hide();

				// Show "History View Mode"
//				$("#dbx-history-label-top").show();
				var historyLabelTopDiv = document.getElementById("dbx-history-label-top");
				historyLabelTopDiv.style.visibility = 'visible';


				console.log("_graphMap.length=" + _graphMap.length);
				if (_graphMap.length > 0)
				{
					let dbxGraph = _graphMap[0];
//					console.log("dbxGraph=" + dbxGraph);
//					console.log("dbxGraph=" + dbxGraph, dbxGraph);

//					console.log("dbxGraph.getTsArraySize() = " + dbxGraph.getTsArraySize());
//					console.log("dbxGraph.getTsArray()     = " + dbxGraph.getTsArray() , dbxGraph.getTsArray());
//					console.log("dbxGraph.getOldestTs()    = " + dbxGraph.getOldestTs(), dbxGraph.getOldestTs());
//					console.log("dbxGraph.getNewestTs()    = " + dbxGraph.getNewestTs(), dbxGraph.getNewestTs());

//					console.log("dbxGraph.getOldestTs() fmt= " + dbxGraph.getOldestTs().format('YYYY-MM-DD HH:mm:ss'));
//					console.log("dbxGraph.getNewestTs() fmt= " + dbxGraph.getNewestTs().format('YYYY-MM-DD HH:mm:ss'));

					// Collect timestamps from ALL graphs (not just the first) to build a full time range
					// Use a Map keyed by ms-epoch to deduplicate, then sort chronologically
					const _allTsMap = new Map();
					let globalOldest = null, globalNewest = null;
					for (let gi = 0; gi < _graphMap.length; gi++)
					{
						const gTsArr = _graphMap[gi].getTsArray();
						for (let t = 0; t < gTsArr.length; t++)
						{
							const ms = gTsArr[t].valueOf();
							if (!_allTsMap.has(ms)) _allTsMap.set(ms, gTsArr[t].clone());
							if (globalOldest === null || ms < globalOldest.valueOf()) globalOldest = gTsArr[t];
							if (globalNewest === null || ms > globalNewest.valueOf()) globalNewest = gTsArr[t];
						}
					}
					// Fall back to first graph bounds if nothing was collected
					if (globalOldest === null) globalOldest = dbxGraph.getOldestTs();
					if (globalNewest === null) globalNewest = dbxGraph.getNewestTs();

					$("#dbx-history-slider-left-text") .html( globalOldest.format('YYYY-MM-DD HH:mm:ss') );
					$("#dbx-history-slider-right-text").html( globalNewest.format('YYYY-MM-DD HH:mm:ss') );

					// Build momentsArray from all graphs' timestamps, sorted chronologically
					const momentsArray = Array.from(_allTsMap.values()).sort((a, b) => a.valueOf() - b.valueOf());

					// Save it as a GLOBAL var, so we can do lookups on it as well...
					_lastHistoryMomentsArray = momentsArray;

					let oldestTsStr = globalOldest.format('YYYY-MM-DD HH:mm:ss');
					let newestTsStr = globalNewest.format('YYYY-MM-DD HH:mm:ss');
					var activeHistoryStatementsArr = dbxTuneGetActiveHistoryStatements(oldestTsStr, newestTsStr);

					// Fetch alarm events for the full timeline range and render orange markers
					renderAlarmTimelineMarkers(oldestTsStr, newestTsStr, momentsArray);
//console.log(">>>>> activeHistoryStatementsArr=" + activeHistoryStatementsArr, activeHistoryStatementsArr);
//console.log(">>>>> activeHistoryStatementsArr.lenth=" + activeHistoryStatementsArr.length);
//console.log(">>>>> momentsArray.lenth=" + momentsArray.length);

					// Create an array with Booleans, that indicates if it HAS an Statement for the time in the 'momentsArray'
					const hasActiveHistoryStatementArr = [];
					for (var m=0; m<momentsArray.length; m++)
					{
//console.log("--m="+m+"---------------------------------");

						let startTime = momentsArray[m].valueOf();
						let endTime   = momentsArray[momentsArray.length-1].clone().add(1, 'minute').valueOf(); // set one minute past the "last" array time... clone() so that we don't change the original object
						if (m + 1 < momentsArray.length)
							endTime = momentsArray[m + 1].valueOf();

						var hasActiveHistoryStatement = -1;
						for (var h=0; h<activeHistoryStatementsArr.length; h++)
						{
							let tmpSampleTime = activeHistoryStatementsArr[h].sampleTime;
							if (tmpSampleTime >= startTime && tmpSampleTime < endTime)
							{
//console.log("######################## TRUE ################ --m=" + m + " --h=" + h);
								hasActiveHistoryStatement = h;
								break;
							}
//console.log("moment["+m+"](" + momentsArray[m].format('YYYY-MM-DD HH:mm:ss') + ")===activeHistoryStatementsArr["+h+"](" + moment(activeHistoryStatementsArr[h].sampleTime).format('YYYY-MM-DD HH:mm:ss') + ") ===> " + hasActiveHistoryStatement);
						}

						hasActiveHistoryStatementArr.push(hasActiveHistoryStatement);
					}
//console.log(">>>>> hasActiveHistoryStatementArr.lenth=" + hasActiveHistoryStatementArr.length, hasActiveHistoryStatementArr);
					// Save the array to a global value -- used to find NEXT/PREV active History Statement
					_hasActiveHistoryStatementArr = hasActiveHistoryStatementArr;

					// lets see what a Plain Slider gets us
					var dbxHistoryTimelineSlider = document.getElementById('dbx-history-timeline-slider');

					// Set tooltip
					dbxHistoryTimelineSlider.title = ""
						+ "Navigation: \n"
						+ "The gray/green bar under the slider, indicates where there are Active Statements \n"
						+ " - Use 'Ctrl+left' for Previous Active Statement \n"
						+ " - Use 'Ctrl+right' for Next Active Statement \n"
						+ " - or simply left/right arrows to move in the slider. \n"
						+ " - or click on any 'line' on any Graph. \n"
						+ "Note: Clicking a Graph, but not on a 'line', causes you to leave 'History View Mode' \n"
						+ "";

					// Clear existing data marks and alarm marks
					var existingDots = document.getElementsByClassName('dbx-history-timeline-slider-marker');
					while (existingDots[0])
						existingDots[0].parentNode.removeChild(existingDots[0]);
					var existingAlarmDots = document.getElementsByClassName('dbx-history-alarm-marker');
					while (existingAlarmDots[0])
						existingAlarmDots[0].parentNode.removeChild(existingAlarmDots[0]);

					// Create data marks
					dbxHistoryTimelineSlider.max = momentsArray.length;
					for (var m=0; m<momentsArray.length; m++)
					{
						// TODO: Possibly change witdh of "marker" depending on "momentsArray.length" ????  (but it needs to be tested on various sizes)

						var color = hasActiveHistoryStatementArr[m] >= 0 ? "green" : "gray";
						var markElement = document.createElement('div');
						markElement.className = 'dbx-history-timeline-slider-marker';
						markElement.style.backgroundColor = color;
						var position = (m / dbxHistoryTimelineSlider.max) * 100;
						markElement.style.left = position + '%';
						dbxHistoryTimelineSlider.parentNode.appendChild(markElement);

						if (hasActiveHistoryStatementArr[m] >= 0)
						{
							var obj = activeHistoryStatementsArr[ hasActiveHistoryStatementArr[m] ];
							markElement.title = ""
								+ "sampleTime: " + obj.sampleTimeIso8601 + "\n"
								+ "SrvName: " + obj.srvNames[0].srvName  + "\n"
								+ "AppName: " + obj.srvNames[0].appName  + "\n"
								+ "CmNames: " + obj.srvNames[0].cmNames  + "\n"
								+ "atPos:   " + m + "\n"
								;
							let sliderPos = m + 1;
							markElement.onclick = function()
							{
								dbxHistoryTimelineSlider.value = sliderPos;
								dbxHistoryTimelineSlider.dispatchEvent( new Event('change') );
							};
						}
						else
						{
//							markElement.title = momentsArray[m].format('YYYY-MM-DD HH:mm:ss');
							markElement.title = momentsArray[m].format('YYYY-MM-DD HH:mm:ss') + "  atPos: " + m;

							let sliderPos = m + 1;
							markElement.onclick = function()
							{
								dbxHistoryTimelineSlider.value = sliderPos;
								dbxHistoryTimelineSlider.dispatchEvent( new Event('change') );
							};
						}
					}

					// ADD 'input' listener -- when we are moving the slider (without stopping)
					dbxHistoryTimelineSlider.addEventListener('input', function(event)
					{
						let value = parseInt(event.target.value) - 1;
//						console.log("Slide=" + value);
						let selectedTs = momentsArray[value];
						$( "#dbx-history-slider-center-text" ).html( selectedTs.format('YYYY-MM-DD HH:mm:ss') );
					});

					// ADD 'change' listener -- when we are finnished moving slider (do ACTION)
					dbxHistoryTimelineSlider.addEventListener('change', function(event)
					{
						let value = parseInt(event.target.value) - 1;
						console.log("Change=" + value);

						let momentTs     = momentsArray[value];
						let startTime    = momentsArray[value].format('YYYY-MM-DD HH:mm:ss');
						let endTime      = momentsArray[momentsArray.length-1].clone().add(1, 'minute').format('YYYY-MM-DD HH:mm:ss'); // set one minute past the "last" array time, clone (to not change the value inside moment)
						let endTimeShort = "";

						if (value + 1 < momentsArray.length)
						{
							momentTs     = momentsArray[value + 1];
							endTime      = momentsArray[value + 1].format('YYYY-MM-DD HH:mm:ss');
							endTimeShort = momentsArray[value + 1].format('HH:mm:ss');
						}

						$( "#dbx-history-slider-center-text" ).html( startTime + " - " + endTimeShort);

						// Cancel the below timer-action (if we get *multiple* change events after each other)
						if (_historyLoadDeferredTimerId !== -1)
							clearTimeout(_historyLoadDeferredTimerId);
						
						// Call the "load action" AFTER 300ms, so a DEFERRED Execution, to not "overload" on slider movements (via left/right arrow movement)
						_historyLoadDeferredTimerId = setTimeout(function() 
						{
							// Dispatch to all module panels via GraphBus
							// historyStart = leftmost bound of the full history range so alarm panel
					// can fetch far enough back to catch long-running active alarms.
					var historyStart = momentsArray[0].format('YYYY-MM-DD HH:mm:ss');
					GraphBus.emit('slider-change', { ts: momentTs, startTime: startTime, endTime: endTime, value: value, historyStart: historyStart });

							// Update visual timeline markers on all charts (graph rendering, not a module concern)
							dbxTuneSetTimeLineMarkerForAllGraphs(momentTs, value);

							// Set focus so we can do: Ctrl + left/right
							dbxHistoryTimelineSlider.focus();

						}, 300);
					});

					// Install slider keyboard: Ctrl+left, Ctrl+right
					dbxHistoryTimelineSlider.addEventListener('keydown', function(event)
					{
						if (event.ctrlKey && event.key === "ArrowLeft")
						{
							console.log("XXXXXXXXXXX slider: ctrl+left");
							event.preventDefault();
							dbxHistoryPrevActiveStatement();
						}

						if (event.ctrlKey && event.key === "ArrowRight")
						{
							console.log("XXXXXXXXXXX slider: ctrl+right");
							event.preventDefault();
							dbxHistoryNextActiveStatement();
						}
					});
				}

				if (momentTs === undefined)
				{
					// no input parameter: Set slider to the END
					dbxHistoryTimelineSlider.value = dbxHistoryTimelineSlider.max;
					dbxHistoryTimelineSlider.dispatchEvent( new Event('change') );
				}
				else
				{
					// if passed parameter, then call this method again... 
					// Second time: it will then already be in "active/visible" mode, and position will simple be moved, then the function will exit early)
					setTimeout(function() { dbxHistoryAction(momentTs); }, 500);
				}
			} // end: Change to HISTORY MODE
			
		} // end: function

		function dbxHistoryPrevActiveStatement()
		{
			let dbxHistoryTimelineSlider = document.getElementById('dbx-history-timeline-slider');
			let currentSliderPos = parseInt(dbxHistoryTimelineSlider.value); // NOTE: 'value' is typeof 'string'
			let startArrPos = currentSliderPos - 2;

			// loop backward to find previous
			let arrayPos = -1; // not found
			for (var h=startArrPos; h>0; h--)
			{
				if (_hasActiveHistoryStatementArr[h] !== -1)
				{
					arrayPos = h;
					break;
				}
			}
			if (arrayPos !== -1)
			{
				console.log("PREVIOUS Active Statement was found at ArrayPos=" + arrayPos);
				dbxHistoryTimelineSlider.value = arrayPos + 1;
				dbxHistoryTimelineSlider.dispatchEvent( new Event('change') );
			}
			else
			{
				console.log("PREVIOUS Active Statement was NOT found");
			}
		}

		function dbxHistoryNextActiveStatement()
		{
			let dbxHistoryTimelineSlider = document.getElementById('dbx-history-timeline-slider');
			let currentSliderPos = parseInt(dbxHistoryTimelineSlider.value); // NOTE: 'value' is typeof 'string'
			let startArrPos = currentSliderPos;

			// loop forward to find next
			let arrayPos = -1; // not found
			for (var h=startArrPos; h<_hasActiveHistoryStatementArr.length; h++)
			{
				if (_hasActiveHistoryStatementArr[h] !== -1)
				{
					arrayPos = h;
					break;
				}
			}

			if (arrayPos !== -1)
			{
				console.log("NEXT Active Statement was found at ArrayPos=" + arrayPos);
				dbxHistoryTimelineSlider.value = arrayPos + 1;
				dbxHistoryTimelineSlider.dispatchEvent( new Event('change') );
			}
			else
			{
				console.log("NEXT Active Statement was NOT found");
			}
		}

		//
/*
		$("#active-statements").on('scroll', function() {
			let xxx = $("#active-statements");
			let leftPos = $("#active-statements").scrollLeft();
			console.log("leftPos: " + leftPos);
			$(".active-statements-srv-info-class").css("left", leftPos + "px");
			$(".active-statements-srv-info-class").css("background-color", "red"); // just to indicate that "something" is happening... to be removed
		});
*/

