/**
 * dbxRefreshStatus.js -- graph.html: show what the collector(s) are refreshing right now, when a sample takes long time
 *
 * Collector (RefreshStatusNoGui)  --POST /api/pcs/refresh-status-->  DbxCentral  --WebSocket /api/chart/broadcast-ws-->  here
 *   - The collector only sends while a sample has been running for more than 'RefreshStatusNoGui.push.thresholdSec'
 *     (default 5 seconds): when the status changes, and every 5 seconds. When the sample ends it sends "refreshing":false.
 *   - Wired in dbxGraphBus.js:  'ws-refresh-status' -> onStatus(),  'ws-data' -> onData()
 *
 * Navbar: a chip, hidden when all servers are on time, with a dropdown that has one row per server.
 *   - amber: a sample is running longer than the threshold
 *            (or, if the collector does not report: the sample is more than 5 seconds overdue)
 *   - red:   the current step has run longer than the sample interval,
 *            no status update for 15 seconds (collector died, or can't reach DbxCentral),
 *            or no data for max(3 x sample interval, 3 minutes)
 */
var DbxRefreshStatus = (function () {

	var STALE_MS          = 15 * 1000;     // While refreshing, the collector sends every 5 seconds
	var NO_DATA_MIN_MS    = 3 * 60 * 1000;
	var LATE_NO_STATUS_MS = 5 * 1000;      // No report from the collector, but the sample is this much overdue: show it as late anyway
	var SLOWEST_CM_COUNT  = 3;             // How many CMs to show in "Slowest CMs" (all of them are in the tooltip)

	var _srv         = {};              // srvName -> { lastDataTs, interval, status, statusRecvTs, head }
	var _pageStartTs = Date.now();
	var _timer       = null;
	var _wasLate     = {};              // srvName -> was it late at the previous render (to auto open the dialog on a NEW late server)
	var _lastOverLimit = null;          // { srvName, time, limitSec }: last time a sample passed the "auto open" limit (shown in the dialog title)

	var AUTO_OPEN_KEY = 'dbxSampleInfo-autoOpen'; // localStorage: '0' = do not auto open the "sample info" dialog (default: open)

	function isAutoOpen()
	{
		try { return localStorage.getItem(AUTO_OPEN_KEY) !== '0'; } catch (e) { return true; }
	}

	var AUTO_OPEN_SEC_KEY = 'dbxSampleInfo-autoOpenSec'; // localStorage: auto open when a sample runs longer than # seconds (default 30; slow samples are shown in the dialog/chip from 5 s, 'RefreshStatusNoGui.push.thresholdSec', but the dialog only pops up for really slow ones)

	function getAutoOpenSec()
	{
		var v = 30;
		try { var s = localStorage.getItem(AUTO_OPEN_SEC_KEY); if (s !== null && s !== '' && ! isNaN(s)) v = Number(s); } catch (e) {}
		return Math.max(0, v);
	}

	function getSrv(srvName)
	{
		if ( ! _srv[srvName] )
			_srv[srvName] = { lastDataTs: 0, interval: 0, status: null, statusRecvTs: 0, head: null };
		return _srv[srvName];
	}

	/** Called on every data sample (GraphBus 'ws-data') */
	function onData(d)
	{
		var s = getSrv(d.srvName);
		s.lastDataTs = Date.now();
		s.head       = d.head || null; // sessionSampleTime, sampleDurationMs, cmRefreshTimes (shown in the "Samples per server" table)
		if (d.sampleInterval > 0)
			s.interval = d.sampleInterval;

		// The data from the sample has arrived, so that sample is done
		s.status = null;

		startTimer();
		render();
	}

	/** Called on every "refresh status" message (GraphBus 'ws-refresh-status') */
	function onStatus(json)
	{
		var s = getSrv(json.serverName);
		if ( ! s.interval && json.sampleIntervalSec > 0 )
			s.interval = json.sampleIntervalSec;

		if (json.refreshing)
		{
			s.status       = json;
			s.statusRecvTs = Date.now() - (json.replayAgeMs || 0); // replayAgeMs: DbxCentral re-sent an older message when we connected
		}
		else
		{
			s.status = null;
		}

		startTimer();
		render();
	}

	function startTimer()
	{
		if (_timer === null)
			_timer = setInterval(function () { render(); renderSampleInfo(); }, 1000);
	}

	/** 75000 -> "1:15", 3725000 -> "1:02:05" */
	function fmt(ms)
	{
		if (ms < 0)
			return '-';
		var sec = Math.floor(ms / 1000);
		var h   = Math.floor(sec / 3600);
		var m   = Math.floor((sec % 3600) / 60);
		var s   = sec % 60;
		var pad = function (n) { return (n < 10 ? '0' : '') + n; };
		return (h > 0 ? h + ':' + pad(m) : m) + ':' + pad(s);
	}

	/** What do we know about this server right now */
	function describe(srvName, now)
	{
		var s          = getSrv(srvName);
		var intervalMs = (s.interval || 60) * 1000;
		var r = {
			srvName:    srvName,
			late:       false,    // show in the chip
			red:        false,
			doing:      '',
			stepMs:     -1,
			sampleMs:   -1,
			chipMs:     -1,
			lastDataMs: s.lastDataTs ? now - s.lastDataTs : -1
		};

		if (s.status)
		{
			var sinceRecv = now - s.statusRecvTs;
			r.late     = true;
			r.kind     = 'status';   // the collector reports what it is doing
			r.doing    = s.status.status + (s.status.subStatus ? ' (' + s.status.subStatus + ')' : '');
			r.stepMs   = s.status.statusMs + sinceRecv;
			r.sampleMs = s.status.sampleMs + sinceRecv;
			r.chipMs   = r.stepMs;
			r.lateMs   = r.sampleMs; // for "auto open when a sample runs longer than # seconds"

			if (r.stepMs > intervalMs)
				r.red = true;

			if (sinceRecv > STALE_MS)
			{
				r.red    = true;
				r.doing += ' - no update for ' + fmt(sinceRecv);
			}
			return r;
		}

		// No status: on time, or no data at all
		if (s.lastDataTs)
		{
			var silentMs = now - s.lastDataTs;
			if (silentMs > Math.max(3 * intervalMs, NO_DATA_MIN_MS))
			{
				r.late   = true;
				r.kind   = 'nodata';
				r.red    = true;
				r.doing  = 'No data for ' + fmt(silentMs) + ' (collector stopped, or it can not reach DbxCentral?)';
				r.chipMs = silentMs;
				r.lateMs = silentMs;
			}
			else if (s.interval)
			{
				var nextInMs = s.lastDataTs + intervalMs - now;
				r.doing = (nextInMs > 0) ? 'Waiting for next sample (in about ' + fmt(nextInMs) + ')' : 'Sampling...';

				// Fallback when the collector does not report (older collector, or no writer to DbxCentral):
				// the sample is overdue (sampling for a while) -> late, and red if longer than the sample interval
				var overdueMs = -nextInMs;
				if (overdueMs > LATE_NO_STATUS_MS)
				{
					r.late   = true;
					r.kind   = 'overdue';  // no report from the collector
					r.red    = overdueMs > intervalMs;
					r.doing  = 'Sampling for ' + fmt(overdueMs) + ' (no details from the collector)';
					r.chipMs = overdueMs;
					r.lateMs = overdueMs;
				}
			}
			else
			{
				r.doing = 'On time';
			}
		}
		else
		{
			// Do not raise the chip for servers that has not sent anything since the page was loaded (stopped long ago, or DbxCentral itself)
			r.doing = 'No data since the page was loaded (' + fmt(now - _pageStartTs) + ')';
		}
		return r;
	}

	function render()
	{
		var $wrap = $('#dbx-refresh-status');
		if ($wrap.length === 0)
			return;

		var subscribed = (typeof _subscribe !== 'undefined') && _subscribe;
		var history    = (typeof isHistoryViewActive === 'function') && isHistoryViewActive();
		if ( ! subscribed || history )
		{
			$wrap.hide();
			return;
		}

		var now  = Date.now();
		var rows = getSrvList().map(function (srv) { return describe(srv, now); });
		var late = rows.filter(function (r) { return r.late; });

		// Auto open the "sample info" dialog when a server's sample BECOMES longer than the user's limit
		// (if closed by the user, it stays closed until the next server passes the limit)
		var newlyLate = false;
		var limitMs   = getAutoOpenSec() * 1000;
		rows.forEach(function (r) {
			var over = r.late && (r.lateMs || 0) >= limitMs;
			if (over && ! _wasLate[r.srvName])
			{
				newlyLate = true;
				_lastOverLimit = { srvName: r.srvName, time: new Date(), limitSec: getAutoOpenSec() }; // shown in the dialog title
			}
			_wasLate[r.srvName] = over;
		});
		if (newlyLate && isAutoOpen() && ! $('#dbx-sample-info-dialog').is(':visible'))
		{
			$('#dbx-sample-info').hide();
			showDialog();
		}

		if (late.length === 0)
		{
			$wrap.hide();
			return;
		}

		// Worst first: red, then the longest running
		late.sort(function (a, b) { return (b.red - a.red) || (b.chipMs - a.chipMs); });
		var worst = late[0];

		var $chip = $('#dbx-refresh-status-chip');
		$chip.toggleClass('dbx-rs-red',   worst.red);
		$chip.toggleClass('dbx-rs-amber', ! worst.red);
		$chip.attr('title', late.map(function (r) { return r.srvName + ': ' + r.doing + ' [' + fmt(r.chipMs) + ']'; }).join('\n'));
		$('#dbx-refresh-status-text').text(worst.srvName + ': ' + worst.doing + (worst.stepMs >= 0 ? ' \u00b7 ' + fmt(worst.stepMs) : '')); // "No data for 3:41" already has the time
		$('#dbx-refresh-status-more').text(late.length > 1 ? '+' + (late.length - 1) : '').toggle(late.length > 1);

		// Dropdown: one row per server, late ones first
		rows.sort(function (a, b) { return (b.late - a.late) || (b.red - a.red) || a.srvName.localeCompare(b.srvName); });
		var $tbody = $('#dbx-refresh-status-rows').empty();
		rows.forEach(function (r) {
			var $tr = $('<tr>').toggleClass('dbx-rs-row-red', r.red).toggleClass('dbx-rs-row-amber', r.late && ! r.red);
			$tr.append($('<td>').text(r.srvName));
			$tr.append($('<td>').text(r.doing));
			$tr.append($('<td class="text-end">').text(fmt(r.stepMs)));
			$tr.append($('<td class="text-end">').text(fmt(r.sampleMs)));
			$tr.append($('<td class="text-end">').text(r.lastDataMs >= 0 ? fmt(r.lastDataMs) + ' ago' : '-'));
			$tbody.append($tr);
		});

		$wrap.show();
	}

	/** All servers on this page (the list can be filled in later, for example when sessionName=all) */
	function getSrvList()
	{
		var srvList = (typeof _serverList !== 'undefined' && _serverList.length) ? _serverList.slice() : Object.keys(_srv);
		Object.keys(_srv).forEach(function (srv) { if (srvList.indexOf(srv) === -1) srvList.push(srv); });
		return srvList;
	}

	/** 1730 -> "1.7 s", 340 -> "340 ms" */
	function fmtMs(ms)
	{
		return (ms >= 1000) ? (ms / 1000).toFixed(1) + ' s' : ms + ' ms';
	}

	/** Columns of the "sample info" table (hover popup and dialog) */
	// minWidthOf: the column is at least as wide as this text, so the table does not change width when the value switches (e.g. "Waiting" <-> "Sampling...")
	var SAMPLE_INFO_COLUMNS = [
		{ title: 'Server'                                                 },
		{ title: 'Next sample', cls: 'text-end', minWidthOf: 'in 00:00 ' },
		{ title: 'Now',                          minWidthOf: 'Sampling...' },
		{ title: 'Last sample'                                            },
		{ title: 'Took'                                                   },
		{ title: 'Slowest CMs / Sample Details',
		  tip:   'When the sample is on time: the 3 slowest CMs of the last sample.\n'
		       + 'When the sample is late: what the collector is doing right now (current CM, sub status and time on that step),\n'
		       + 'or why we have no details (the collector does not report, or no data arrives).' }
	];

	/**
	 * Navbar clock ("12s"): per server, what was sampled last time and when the next sample is due.
	 * Shown in the hover popup (#dbx-sample-info) and in the dialog opened by clicking the clock (#dbx-sample-info-dialog)
	 */
	function renderSampleInfo()
	{
		['#dbx-sample-info', '#dbx-sample-info-dialog'].forEach(function (id) {
			var $box = $(id);
			if ($box.length && $box.is(':visible'))
				renderSampleInfoTable($box.find('table.dbx-rs-table'));
		});
	}

	function renderSampleInfoTable($table)
	{
		if ($table.find('thead th').length === 0)
		{
			var $hr = $('<tr>');
			SAMPLE_INFO_COLUMNS.forEach(function (c) {
				var $th = $('<th>').addClass(c.cls || '').text(c.title);
				if (c.tip)
					$th.attr('title', c.tip).addClass('dbx-sid-help');
				$hr.append($th);
			});
			$table.find('thead').append($hr);

			// Min width = rendered width of 'minWidthOf' (in the same font as the values) + the cell's left/right padding
			var $th    = $hr.children().first();
			var padX   = (parseFloat($th.css('padding-left')) || 0) + (parseFloat($th.css('padding-right')) || 0);
			var $probe = $('<span>').css({ position: 'absolute', visibility: 'hidden', 'white-space': 'nowrap' }).appendTo($table.parent());
			SAMPLE_INFO_COLUMNS.forEach(function (c, i) {
				if (c.minWidthOf)
					$hr.children().eq(i).css('min-width', Math.ceil($probe.text(c.minWidthOf).outerWidth() + padX) + 'px');
			});
			$probe.remove();
		}

		var now    = Date.now();
		var $tbody   = $table.find('tbody').empty();
		var lateCnt  = 0;
		var anyRed   = false;
		getSrvList().forEach(function (srv) {
			var s    = getSrv(srv);
			var r    = describe(srv, now);
			var head = s.head || {};

			if (r.late) lateCnt++;
			if (r.red)  anyRed = true;

			// Last sample: collector sample time (HH:mm:ss) + how long ago it arrived
			var lastSample = '-';
			if (s.lastDataTs)
				lastSample = String(head.sessionSampleTime || '').substring(11, 19) + ' (' + fmt(now - s.lastDataTs) + ' ago)';

			// How long every CM took in the last sample: [{cm, ms, status=ok|timeout|error, msg}] (sent by the collector, slowest first)
			var cmRefreshTimes = (head.cmRefreshTimes || []).slice().sort(function (a, b) { return b.ms - a.ms; });
			var cmSumMs = cmRefreshTimes.reduce(function (sum, c) { return sum + c.ms; }, 0);
			var cmText  = function (c) { return c.cm + ' ' + fmtMs(c.ms) + (c.status && c.status !== 'ok' ? ' (' + c.status + ')' : ''); };

			// Took: the whole sample time (newer collectors, otherwise the sum of all CMs), the details in a tooltip
			var took = '-';
			if (head.sampleDurationMs >= 0 && cmRefreshTimes.length)
			{
				took = {
					text:  fmtMs(head.sampleDurationMs) + ', ' + cmRefreshTimes.length + ' CMs',
					title: 'Whole sample: ' + fmtMs(head.sampleDurationMs) + '\n'
					     + 'CMs (' + cmRefreshTimes.length + '): ' + fmtMs(cmSumMs) + '\n'
					     + 'Other (post refresh, alarm handling, etc): ' + fmtMs(Math.max(0, head.sampleDurationMs - cmSumMs))
				};
			}
			else if (cmRefreshTimes.length)
			{
				took = { text: fmtMs(cmSumMs) + ', ' + cmRefreshTimes.length + ' CMs', title: 'Sum of all CMs (this collector does not send the whole sample time)' };
			}

			// Slowest CMs: the top SLOWEST_CM_COUNT (+ a note if other CMs failed), all CMs (and errors) in the tooltip
			var slowest = '-';
			if (cmRefreshTimes.length)
			{
				var top        = cmRefreshTimes.slice(0, SLOWEST_CM_COUNT);
				var failedRest = cmRefreshTimes.slice(SLOWEST_CM_COUNT).filter(function (c) { return c.status && c.status !== 'ok'; }).length;
				slowest = {
					text:  top.map(cmText).join(', ') + (failedRest > 0 ? ' \u00b7 +' + failedRest + ' failed' : ''),
					title: 'All CMs in the last sample (slowest first):\n'
					     + cmRefreshTimes.map(function (c) { return cmText(c) + (c.msg ? ' -- ' + c.msg : ''); }).join('\n')
				};
			}

			// Next sample: the collector sleeps 'interval' seconds after a sample, and the data arrives right after the sample
			var next  = '-';
			var doing = r.doing; // late servers: what the collector reported (or "No data for ...")
			if (s.status)
				next = { icon: 'fa fa-refresh fa-spin', text: 'now' };
			else if (s.lastDataTs && s.interval)
			{
				var nextInMs = s.lastDataTs + s.interval * 1000 - now;
				next = (nextInMs >= 0) ? 'in ' + fmt(nextInMs) : { icon: 'fa fa-refresh fa-spin', text: fmt(-nextInMs) }; // overdue: spinning icon + time
				if ( ! r.late )
					doing = (nextInMs >= 0) ? 'Waiting' : 'Sampling...';
			}

			// Late: "Now" is just the short state, and the details go into the (wider) "Slowest CMs" column
			//   status  = what the collector is doing right now (+ time on the current step), with a spinning icon
			//   overdue = "Sampling for m:ss (no details from the collector)"
			//   nodata  = "No data for m:ss (...)"
			if (r.late)
			{
				doing = (r.kind === 'nodata') ? 'No data' : 'Sampling...';
				if (r.kind === 'status')
					slowest = { icon: 'fa fa-refresh fa-spin', text: r.doing + ' \u00b7 ' + fmt(r.stepMs) };
				else
					slowest = r.doing;
			}

			// Same order as SAMPLE_INFO_COLUMNS
			var cells = [srv, next, doing, lastSample, took, slowest];
			var $tr = $('<tr>').toggleClass('dbx-rs-row-red', r.red).toggleClass('dbx-rs-row-amber', r.late && ! r.red);
			cells.forEach(function (val, i) {
				var $td = $('<td>').addClass(SAMPLE_INFO_COLUMNS[i].cls || '');
				if (val && val.icon)
					$td.append($('<i>').addClass(val.icon).attr('title', 'Sampling (or sending the data)'), ' ', document.createTextNode(val.text));
				else if (val && val.title)
					$td.text(val.text).attr('title', val.title).addClass('dbx-sid-help');
				else
					$td.text(val);
				$tr.append($td);
			});
			$tbody.append($tr);
		});

		// Dialog title bar: warning colour (and count) while any server is late (sample longer than the collector threshold, or no data)
		var $header = $table.closest('.dbx-sample-info-box').find('.dbx-sid-header');
		$header.toggleClass('dbx-sid-alert', anyRed);
		$header.toggleClass('dbx-sid-warn',  lateCnt > 0 && ! anyRed);
		// ... and when (and on what server) a sample last ran longer than the "auto open" limit
		var lastOver = '';
		if (_lastOverLimit)
		{
			var t = _lastOverLimit.time;
			var hms = (typeof moment === 'function') ? moment(t).format('HH:mm:ss') : t.toTimeString().substring(0, 8);
			lastOver = ' \u00b7 last > ' + _lastOverLimit.limitSec + ' s: ' + _lastOverLimit.srvName + ' @ ' + hms;
		}
		$header.find('.dbx-sid-title').text('Samples per server' + (lateCnt > 0 ? ': ' + lateCnt + ' late' : '') + lastOver);

		// Link to the "Collector Refresh Time" graphs for the servers on this page (same as: Servers page -> Collector Refresh Time)
		$table.closest('.dbx-sample-info-box').find('#dbx-sid-refreshtime-link').attr('href',
			'/graph.html?subscribe=true&startTime=2h&sessionName=' + getSrvList().map(encodeURIComponent).join(',') + '&graphList=CmSummary_CmRefreshTime&gcols=1');
	}

	/** Show a box below the navbar clock, kept inside the window (the table can be wide) */
	function showBelowClock($box, anchorEl)
	{
		var rect = (anchorEl || document.getElementById('subscribe-feedback-time')).getBoundingClientRect();
		$box.css({ top: rect.bottom + 4, left: 0 }).show();
		renderSampleInfo();
		var left = Math.min(rect.left, window.innerWidth - $box.outerWidth() - 8);
		$box.css({ left: Math.max(8, left) });
	}

	var DIALOG_POS_KEY = 'dbxSampleInfo-dialogPos'; // localStorage: {"top":#,"left":#} where the user last dragged the dialog

	/** Open the dialog where the user last left it (kept inside the window), or below the clock */
	function showDialog()
	{
		var $dialog = $('#dbx-sample-info-dialog');
		var pos = null;
		try { pos = JSON.parse(localStorage.getItem(DIALOG_POS_KEY) || 'null'); } catch (e) {}

		if ( ! pos || isNaN(pos.top) || isNaN(pos.left) )
		{
			showBelowClock($dialog);
			return;
		}

		$dialog.css({ top: 0, left: 0 }).show();
		renderSampleInfo();
		// Make sure it is visible, even if the window is smaller than when the position was saved
		var maxLeft = Math.max(0, window.innerWidth  - $dialog.outerWidth());
		var maxTop  = Math.max(0, window.innerHeight - $dialog.outerHeight());
		$dialog.css({ top: Math.min(Math.max(0, pos.top), maxTop), left: Math.min(Math.max(0, pos.left), maxLeft) });
	}

	$(document).ready(function () {
		// Move out of the (collapsible) navbar, so a narrow window does not hide an open dialog
		$('#dbx-sample-info, #dbx-sample-info-dialog').appendTo('body');

		var $dialog = $('#dbx-sample-info-dialog');

		// Dialog: can be moved by its title bar, stays open until closed (does not block the page)
		if ($.fn.draggable)
			$dialog.draggable({
				handle:      '.dbx-sid-header',
				containment: 'window',
				stop: function () {
					// Remember the position (viewport coordinates, the dialog is position:fixed)
					var rect = this.getBoundingClientRect();
					try { localStorage.setItem(DIALOG_POS_KEY, JSON.stringify({ top: Math.round(rect.top), left: Math.round(rect.left) })); } catch (e) {}
				}
			});
		$dialog.find('.btn-close').on('click', function () { $dialog.hide(); });

		// "Auto open when late" checkbox, remembered in this browser
		$('#dbx-sid-autoopen')
			.prop('checked', isAutoOpen())
			.on('change', function () {
				try { localStorage.setItem(AUTO_OPEN_KEY, this.checked ? '1' : '0'); } catch (e) {}
			});

		// "...when a sample runs longer than # s", remembered in this browser
		$('#dbx-sid-autoopen-sec')
			.val(getAutoOpenSec())
			.on('change', function () {
				var v = Math.max(0, parseInt(this.value, 10) || 0);
				this.value = v;
				try { localStorage.setItem(AUTO_OPEN_SEC_KEY, String(v)); } catch (e) {}
			});

		// Hover: quick view below the hovered element (not when the dialog is already open)
		// on the clock ("12s"), the "data received from <server>" text left of it, and the "refresh" button right of it
		$('#subscribe-feedback-time, #subscribe-feedback-srv, #dbx-refresh')
			.on('mouseenter', function () {
				if ( ! $dialog.is(':visible') )
					showBelowClock($('#dbx-sample-info'), this);
			})
			.on('mouseleave', function () {
				$('#dbx-sample-info').hide();
			});

		$('#subscribe-feedback-time')
			.css('cursor', 'pointer')
			// Click: open/close the dialog
			.on('click', function () {
				$('#dbx-sample-info').hide();
				if ($dialog.is(':visible'))
					$dialog.hide();
				else
				{
					showDialog();
					startTimer(); // keep it updated, even before any data has arrived
				}
			});
	});

	return {
		onData:   onData,
		onStatus: onStatus,
		render:   render
	};
}());
