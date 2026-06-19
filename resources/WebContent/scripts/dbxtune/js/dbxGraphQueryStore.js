/** dbxGraphQueryStore.js — SQL Server Query Store panel for graph.html
 *
 * Only shown when the monitored server's appName === 'SqlServerTune'.
 * The #query-store-btn starts hidden; queryStoreCheckAppName() reveals it
 * when the first ws-data event carrying SqlServerTune arrives.
 *
 * Three-tab workflow:
 *   Databases  →  Top Queries  →  Query Detail
 */

// ── State ─────────────────────────────────────────────────────────────────────
var _scrPfx = screen.width + 'x' + screen.height + '_'; // screen-size prefix for size/position localStorage keys
var _qsSrvName        = null;   // server currently displayed
var _qsTimestamp      = null;   // timestamp (history mode); null = live/latest
var _qsMainTab        = 'databases'; // 'databases' | 'topqueries' | 'detail'
var _qsDbname         = null;   // currently selected database
var _qsQueryId        = null;   // currently selected query_id
var _qsAutoOpenChecked = false;
var _qsTqFilter       = '';     // current top-queries filter string
var _qsTqLastResult   = null;   // last fetched top-queries result (re-filter without re-fetch)
var _qsTqChart        = null;   // active Chart.js instance for Top Queries bar chart
var _qsTqBrushChart   = null;   // active Chart.js instance for Top Queries time-range brush
var _qsTimeRange      = { startTime: null, endTime: null }; // active brush selection (null = full range)
var _qsTqVSplit        = null;   // Split.js vertical instance (brush + chart + table)
var _qsBrushHSplit     = null;   // Split.js horizontal instance (brush line vs wait bar)
var _qsXmlPlans         = [];     // XML plan strings indexed by slot; avoids inline onclick quoting
var _qsAvailDates       = [];     // date keys from recording-databases API (newest first, 'YYYY-MM-DD')
var _qsMultiDbMode      = false;  // true when cross-db result is currently displayed
var _qsMultiDbAllRows   = null;   // raw un-sliced merged rows for CURRENT view (may be time-filtered)
var _qsMultiDbOrigRows  = null;   // original UNFILTERED rows — restored when brush filter is cleared
var _qsMultiDbOrigTimelines = null; // original UNFILTERED timelines — restored when brush filter is cleared
var _qsMultiDbSchemaFlags = null; // schema flags (hasMemoryCols etc.) from first DB response
var _qsMultiDbTimelines   = null; // merged timelines for brush/wait charts in multi-db mode
var _qsMultiDbCtx         = null; // original fetch context; kept so a time-filter re-fetch can reuse it
var _qsMultiDbCancel    = false;  // set to true by Cancel button during serial fetch
var _qsBrushRefreshUi   = null;   // ref to refreshUi() closure so Split pane drag can re-position handles
var _qsBrushResizeObs   = null;   // ResizeObserver watching the brush canvas box (for smooth resize)
var _qsDetailDay        = null;   // _day value from the last multi-db row click ('N days' or specific date)

// ── SQL Server detection ──────────────────────────────────────────────────────

// True once we have confirmed the current page includes a SqlServerTune session.
// Set by queryStoreCheckAppName() — either from ws-data or from the early api/sessions fetch.
var _qsIsSqlServer = false;

/**
 * Called from GraphBus ws-data listener AND from the early api/sessions fetch below.
 * Shows the Query Store button when a SqlServerTune server is detected.
 * The button is never hidden proactively: on a mixed-server page (ASE + SQL Server)
 * multiple appName events fire; hiding on non-SqlServerTune would race.
 */
function queryStoreCheckAppName(appName)
{
	if (appName === 'SqlServerTune') {
		_qsIsSqlServer = true;
		var btn = document.getElementById('query-store-btn');
		if (btn) btn.style.display = 'flex';
	}
}

/**
 * Early server-type detection — fires as soon as dbxcentral.graph.js has
 * fully populated _serverList (via the 'server-list-ready' GraphBus event).
 * Fetches api/sessions to get productString (= appName) and calls
 * queryStoreCheckAppName for every server that belongs to this page.
 * This shows the Query Store button immediately — without waiting for the
 * first ws-data tick — and also works in history / replay mode where
 * ws-data never fires.
 */
GraphBus.on('server-list-ready', function (d) {
	var servers = d.serverList;
	if (!servers || servers.length === 0) return;
	$.ajax({
		url: 'api/sessions', type: 'GET', dataType: 'json',
		success: function (sessions) {
			if (!Array.isArray(sessions)) return;
			sessions.forEach(function (s) {
				if (servers.indexOf(s.serverName) === -1) return;
				queryStoreCheckAppName(s.productString || '');
			});
		}
	});
});

// ── Time formatting ───────────────────────────────────────────────────────────

/**
 * Format milliseconds as "Xh Ym Zs Wms" — same style as _qsUsToHms but takes ms.
 * Leading components are omitted when zero; seconds and ms are always shown.
 */
function _qsMsToHms(msVal)
{
	if (!msVal) return '0s 0ms';
	var total = Math.round(Number(msVal));
	var h  = Math.floor(total / 3600000);
	var m  = Math.floor((total % 3600000) / 60000);
	var s  = Math.floor((total % 60000) / 1000);
	var ms = total % 1000;
	return (h ? h + 'h ' : '') + (m ? m + 'm ' : '') + s + 's ' + ms + 'ms';
}

/**
 * Format microseconds as "Xh Ym Zs Wms" — used as tooltip on timing cells.
 * Returns empty string for null/0.
 */
function _qsUsToHms(us)
{
	if (!us) return '';
	var s  = Math.floor(us / 1e6);
	var ms = Math.floor((us % 1e6) / 1000);
	var h  = Math.floor(s / 3600);
	var m  = Math.floor((s % 3600) / 60);
	var ss = s % 60;
	return (h ? h + 'h ' : '') + (m ? m + 'm ' : '') + ss + 's ' + ms + 'ms';
}

/**
 * Convert a microsecond value to a display-ready millisecond integer.
 * Returns { disp, tip } where:
 *   disp = integer ms with thousands separator (e.g. "1 234")
 *   tip  = "#h #m #s #ms" breakdown; when the ms display value is < 5,
 *          the exact µs is appended (e.g. "0s 2ms — 2 341 µs")
 */
function _qsFmtUsAsMs(us)
{
	if (us == null) return { disp: '—', tip: '' };
	var n = Number(us);
	if (isNaN(n)) return { disp: '—', tip: '' };
	var ms   = Math.round(n / 1000);
	var disp = _qsFmtNum(ms) || String(ms);
	var tip  = _qsUsToHms(n) || '0s 0ms';
	if (ms < 5) tip += ' \u2014 ' + (_qsFmtNum(Math.round(n)) || String(Math.round(n))) + ' \u00b5s';
	return { disp: disp, tip: tip };
}

/**
 * Format a number with `dec` decimal places + thousands separator.
 * Returns null when val is null/NaN.
 * _qsFmt1Dec(v) is a convenience wrapper for dec=1 (row-counts, MB values).
 */
function _qsFmtDec(v, dec)
{
	if (v == null || v === undefined) return null;
	var n = (typeof v === 'number') ? v : parseFloat(v);
	if (isNaN(n)) return null;
	var parts = n.toFixed(dec).split('.');
	parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g,
		typeof _qsNumFmtMode !== 'undefined' && _qsNumFmtMode === 'locale' ? ',' : '\u00a0');
	return parts.join('.');
}

function _qsFmt1Dec(v) { return _qsFmtDec(v, 1); }

/**
 * Format a millisecond duration as a human-readable string.
 *   < 1 000 ms  →  "800 ms"
 *   < 60 000 ms →  "12.35 s"  (2 decimal places)
 *   < 3 600 000 →  "4m 32s"
 *   ≥ 3 600 000 →  "2h 15m"
 */
function _qsFmtMsDur(ms)
{
	if (ms == null || isNaN(ms)) return String(ms);
	var v = Number(ms);
	if (v < 1000)       return (_qsFmtNum(Math.round(v)) || String(Math.round(v))) + '\u00a0ms';
	if (v < 60000)      return _qsFmtDec(v / 1000, 2) + '\u00a0s';
	if (v < 3600000) {
		var m = Math.floor(v / 60000);
		var s = Math.round((v % 60000) / 1000);
		return m + 'm\u00a0' + s + 's';
	}
	if (v < 86400000) {
		var h = Math.floor(v / 3600000);
		var m = Math.round((v % 3600000) / 60000);
		return h + 'h\u00a0' + m + 'm';
	}
	var d = Math.floor(v / 86400000);
	var h = Math.round((v % 86400000) / 3600000);
	return d + 'd\u00a0' + h + 'h';
}

/**
 * Format an 8-KB page count as a human-readable size — used as tooltip on
 * logical/physical read page-count cells.
 * Each page = 8 KB, so MB = pages / 128.
 * Returns empty string for null/0.
 */
function _qsPagesToMb(pages)
{
	if (!pages) return '';
	var mb = pages / 128.0;
	if (mb >= 1024) return (mb / 1024).toFixed(1) + ' GB';
	if (mb >= 1)    return mb.toFixed(1) + ' MB';
	return (pages * 8).toFixed(0) + ' KB';
}

// ── Number formatting ─────────────────────────────────────────────────────────

// Persisted number format for Query Store: 'locale' or 'spaces' (default 'spaces')
var _qsNumFmtMode = (function() {
	try { return localStorage.getItem('queryStore-numFmt') || 'spaces'; } catch(e) { return 'spaces'; }
}());

/** Called when the user changes the Number Format radio. Re-renders current tab. */
function qsNumFmtChanged(mode)
{
	_qsNumFmtMode = mode;
	try { localStorage.setItem('queryStore-numFmt', mode); } catch(e) {}
	// Re-render current view without re-fetching
	if (_qsMainTab === 'topqueries' && _qsTqLastResult)
		queryStoreRenderTopQueries(_qsTqLastResult);
	else if (_qsMainTab === 'detail' && _qsQueryId)
		queryStoreQueryDetailLoad(_qsQueryId);
}

/** Initialise the radio buttons from persisted setting (call once on open). */
function qsNumFmtInit()
{
	var el = document.getElementById('qs-numfmt-' + _qsNumFmtMode);
	if (el) el.checked = true;
}

/**
 * Format a number with thousands separators.
 * Uses the Query Store's own persisted format preference (_qsNumFmtMode).
 * Returns null when the value is not numeric or abs < 1000 (no formatting needed).
 */
function _qsFmtNum(v)
{
	if (v === null || v === undefined) return null;
	var n = (typeof v === 'number') ? v : parseFloat(v);
	if (isNaN(n)) return null;
	var absN = Math.abs(n);
	if (absN < 1000) return null;

	if (_qsNumFmtMode === 'locale') {
		try {
			return n.toLocaleString();
		} catch(e) { /* fall through to spaces */ }
	}
	// 'spaces' mode (default)
	var s = String(v), dot = s.indexOf('.');
	var dec = dot >= 0 ? s.substring(dot) : '';
	var intPart = String(Math.trunc(absN)).replace(/\B(?=(\d{3})+(?!\d))/g, '\u00a0');
	return (n < 0 ? '-' : '') + intPart + dec;
}

// ── Open / Close ──────────────────────────────────────────────────────────────

function queryStoreToggle()
{
	var $panel = $('#query-store-panel');
	if ($panel.is(':visible')) {
		queryStoreClose();
		return;
	}

	// On first open: center horizontally, near the top
	if (!_qsSrvName) {
		$panel.css('display', 'flex');
		var panelW = $panel.outerWidth();
		var left   = Math.max(0, Math.round((window.innerWidth - panelW) / 2));
		$panel.css({ top: '100px', left: left + 'px' });
	}

	$panel.css({ display: 'flex', 'z-index': ++window._dbxTopZ });
	try { localStorage.setItem('queryStore-panelOpen', '1'); } catch(e) {}

	// Restore dark mode preference
	var dark = _colorSchema === 'dark';
	try {
		var saved = typeof getStorage === 'function'
			? getStorage('dbxtune_checkboxes_').get('query-store-dark-chk') : null;
		if (saved === 'checked') dark = true;
		else if (saved === 'not') dark = false;
	} catch(e) {}
	$('#query-store-dark').prop('checked', dark);
	if (dark) $panel.addClass('qs-dark'); else $panel.removeClass('qs-dark');

	// Restore number format preference
	qsNumFmtInit();

	// Restore "Include XML plan" checkbox
	try {
		var planChk = localStorage.getItem('queryStore-includePlan');
		if (planChk !== null)
			$('#query-store-include-plan').prop('checked', planChk === '1');
	} catch(e) {}

	queryStoreRefresh();
}

function queryStoreClose()
{
	try { localStorage.setItem('queryStore-panelOpen', '0'); } catch(e) {}
	$('#query-store-panel').hide();
	_qsSrvName        = _qsTimestamp = null;
	_qsDbname         = _qsQueryId   = null;
	_qsMultiDbCancel  = true;   // abort any in-flight serial fetch
	_qsMultiDbMode    = false;
	_qsMultiDbAllRows = null;
	_qsMultiDbOrigRows  = null;
	_qsMultiDbOrigTimelines = null;
	_qsMultiDbTimelines = null;
	_qsMultiDbCtx     = null;
	_qsBrushRefreshUi = null;
	if (_qsBrushResizeObs) { try { _qsBrushResizeObs.disconnect(); } catch(e) {} _qsBrushResizeObs = null; }
	_qsMainTab = 'databases';
	_qsResetTabs();
	_qsDestroyTqSplits();
	$('#query-store-content').html('');
	$('#qs-date-select').hide().empty();
	$('#query-store-ts').hide();
}

function queryStoreDarkToggle(on)
{
	if (on) $('#query-store-panel').addClass('qs-dark');
	else    $('#query-store-panel').removeClass('qs-dark');
	try {
		if (typeof getStorage === 'function')
			getStorage('dbxtune_checkboxes_').set('query-store-dark-chk', on ? 'checked' : 'not');
	} catch(e) {}
	// Re-render the brush + wait charts so Chart.js canvas colors update immediately
	if (_qsMultiDbTimelines || _qsTqLastResult) {
		var tl = _qsMultiDbTimelines || (_qsTqLastResult && _qsTqLastResult.timeline) || [];
		if (tl.length >= 2) {
			var rankBy = $('#qs-rank-by').val() || 'duration';
			_qsRenderTimeBrush(tl, rankBy);
		}
	}
}

/** Returns true when the Query Store panel is currently in dark mode. */
function _qsIsDark() {
	return $('#query-store-panel').hasClass('qs-dark');
}

/** Called when "Include XML plan" checkbox changes — persists the state then reloads detail. */
function queryStoreIncludePlanChanged()
{
	var on = $('#query-store-include-plan').is(':checked');
	try { localStorage.setItem('queryStore-includePlan', on ? '1' : '0'); } catch(e) {}
	queryStoreDetailLoad();
}

// ── Recording database (date) selection ──────────────────────────────────────

/**
 * Fetch available H2 recording database date keys from the collector and
 * populate #qs-date-select.  Called once per server when the panel opens.
 * Silent no-op when date-rolling is not configured on the collector
 * (returns dateRollingEnabled=false or an empty availableDates list).
 */
function queryStoreLoadRecordingDbs(srv)
{
	if (!srv) return;
	$('#qs-date-select').hide();
	$.ajax({
		url:      '/api/cc/mgt/recording-databases',
		data:     { srv: srv },
		dataType: 'text',
		success: function(data) {
			try {
				var r = JSON.parse(data);
				if (!r.dateRollingEnabled) return; // single H2 file — no dropdown needed
				var dates    = r.availableDates || [];
				var liveKey  = r.currentDateKey || null;
				_qsAvailDates = dates.filter(function(d) { return d !== liveKey; }); // historical dates only, newest-first
				// Update "Days Avail." cells in the databases table if it rendered before this response arrived
				$('.qs-days-avail-cell').text(_qsAvailDates.length + 1);
				if (dates.length <= 1) return; // only today — no dropdown needed

				var _dayNames = ['Sun','Mon','Tue','Wed','Thu','Fri','Sat'];
				var _dateLabel = function(d) {
					try {
						var day = _dayNames[new Date(d + 'T12:00:00').getDay()];
						return '📂 ' + d + ' (' + day + ')';
					} catch(e) { return '📂 ' + d; }
				};
				var $sel = $('#qs-date-select');
				$sel.empty();
				// "Today (live)" option
				$sel.append($('<option>', { value: '', text: '📅 Today (live)' }));
				dates.forEach(function(d) {
					if (d === liveKey) return; // live date already covered above
					$sel.append($('<option>', { value: d, text: _dateLabel(d) }));
				});
				// Restore previously selected date if still available
				var cur = _qsTimestamp ? _qsTimestamp.substring(0, 10) : '';
				if (cur && $sel.find('option[value="' + cur + '"]').length)
					$sel.val(cur);
				else
					$sel.val('');

				$sel.show();
				_qsUpdateHistoricalBanner();
			} catch(ex) { /* silent: date nav is a convenience, not critical */ }
		}
	});
}

/** Called when the date dropdown changes — updates _qsTimestamp and reloads. */
function queryStoreDateChanged()
{
	var dateKey = $('#qs-date-select').val() || '';
	// Use 23:59:59 so getConnectionForTimestamp opens the right daily H2 file
	_qsTimestamp      = dateKey ? (dateKey + ' 23:59:59') : null;
	_qsMultiDbCancel  = true;   // abort any in-flight serial fetch
	_qsMultiDbMode    = false;
	_qsMultiDbAllRows = null;
	_qsMultiDbOrigRows  = null;
	_qsMultiDbOrigTimelines = null;
	_qsMultiDbTimelines = null;
	_qsMultiDbCtx     = null;
	_qsBrushRefreshUi = null;
	if (_qsBrushResizeObs) { try { _qsBrushResizeObs.disconnect(); } catch(e) {} _qsBrushResizeObs = null; }
	_qsUpdateHistoricalBanner();
	// Reset to databases tab so the user sees what's in the selected day's data
	_qsDbname  = null;
	_qsQueryId = null;
	_qsMainTab = 'databases';
	_qsResetTabs();
	_qsActivateTab('databases');
	queryStoreDatabasesLoad(_qsSrvName, _qsTimestamp);
}

/** Show/hide the "Historical" indicator next to the server name. */
function _qsUpdateHistoricalBanner()
{
	if (_qsTimestamp)
		$('#query-store-ts').show();
	else
		$('#query-store-ts').hide();
}

// ── Refresh ───────────────────────────────────────────────────────────────────

function queryStoreRefresh()
{
	var srv = _qsSrvName || (typeof _serverList !== 'undefined' && _serverList.length > 0 ? _serverList[0] : null);
	if (!srv) { queryStoreShowMsg('No server selected.'); return; }

	// Load date dropdown on first open (srv changes from null → something)
	if (_qsSrvName !== srv)
		queryStoreLoadRecordingDbs(srv);

	_qsSrvName = srv;
	$('#query-store-srv').text('[' + srv + ']');

	if (_qsMainTab === 'topqueries' && _qsDbname)
		queryStoreTopQueriesLoad();
	else if (_qsMainTab === 'detail' && _qsDbname && _qsQueryId)
		queryStoreDetailLoad();
	else
		queryStoreDatabasesLoad(srv, _qsTimestamp);
}

// ── Tab control ───────────────────────────────────────────────────────────────

function queryStoreMainTabClick(evt, tab)
{
	if (evt) evt.preventDefault();
	_qsMainTab = tab;

	$('#query-store-tabs-main .nav-link').removeClass('active');
	$('#query-store-tabs-main .nav-link').each(function() {
		if (($(this).attr('onclick') || '').indexOf("'" + tab + "'") >= 0)
			$(this).addClass('active');
	});

	if (tab === 'topqueries') {
		$('#query-store-tq-bar').css('display', 'flex');
		$('#query-store-detail-bar').hide();
		if (_qsDbname) queryStoreTopQueriesLoad();
		else queryStoreShowMsg('Select a database from the <b>Databases</b> tab first.');
	} else if (tab === 'detail') {
		$('#query-store-tq-bar').hide();
		$('#query-store-detail-bar').css('display', 'flex');
		if (_qsDbname && _qsQueryId) queryStoreDetailLoad();
		else queryStoreShowMsg('Select a query from the <b>Top Queries</b> tab first.');
	} else {
		// databases
		$('#query-store-tq-bar').hide();
		$('#query-store-detail-bar').hide();
		if (_qsSrvName) queryStoreDatabasesLoad(_qsSrvName, _qsTimestamp);
	}
}

function _qsResetTabs()
{
	$('#query-store-tabs-main .nav-link').removeClass('active');
	$('#query-store-tabs-main .nav-link').first().addClass('active');
	$('#query-store-tq-bar').hide();
	$('#query-store-detail-bar').hide();
	$('#query-store-loading').hide();
}

function _qsActivateTab(tab)
{
	$('#query-store-tabs-main .nav-link').removeClass('active');
	$('#query-store-tabs-main .nav-link').each(function() {
		if (($(this).attr('onclick') || '').indexOf("'" + tab + "'") >= 0)
			$(this).addClass('active');
	});
}

// ── Databases tab ─────────────────────────────────────────────────────────────

function queryStoreDatabasesLoad(srv, ts)
{
	if (!srv) return;
	_qsSrvName = srv;
	_qsMainTab = 'databases';
	_qsResetTabs();
	_qsActivateTab('databases');

	var params = { srv: srv, action: 'listDatabases' };
	if (ts) params.ts = ts;

	$('#query-store-loading').show();
	$('#query-store-content').html('');

	$.ajax({
		url:      '/api/cc/mgt/query-store',
		data:     params,
		dataType: 'text',
		success: function(data) {
			$('#query-store-loading').hide();
			try {
				var r = JSON.parse(data);
				if (r.error) { queryStoreShowMsg(r.message || r.error); return; }
				queryStoreRenderDatabases(r);
			} catch(ex) { queryStoreShowMsg('Parse error: ' + ex); }
		},
		error: function(xhr) {
			$('#query-store-loading').hide();
			queryStoreShowHttpError(xhr, 'listDatabases');
		}
	});
}

function queryStoreRenderDatabases(r)
{
	var dbs            = (r && r.databases)      ? r.databases      : [];
	var totalUserDbs   = (r && r.totalUserDbs   != null && r.totalUserDbs   >= 0) ? r.totalUserDbs   : -1;
	var qsEnabledCount = (r && r.qsEnabledCount != null && r.qsEnabledCount >= 0) ? r.qsEnabledCount : -1;
	var qsAllDbs       = (r && Array.isArray(r.qsAllDbs)) ? r.qsAllDbs : [];
	if (dbs.length === 0) {
		var enableSql =
			  '-- ============================================================\n'
			+ '-- Enable Query Store for ONE database\n'
			+ '-- ============================================================\n'
			+ 'USE master\n'
			+ 'GO\n'
			+ 'ALTER DATABASE [dbname] SET QUERY_STORE = ON\n'
			+ 'GO\n'
			+ 'ALTER DATABASE [dbname] SET QUERY_STORE\n'
			+ '(\n'
			+ '    OPERATION_MODE          = READ_WRITE,\n'
			+ '    MAX_STORAGE_SIZE_MB     = 2048,\n'
			+ '    INTERVAL_LENGTH_MINUTES = 60\n'
			+ ')\n'
			+ 'GO\n'
			+ '--\n'
			+ '-- ============================================================\n'
			+ '-- Full option reference (all settings shown with descriptions)\n'
			+ '-- ============================================================\n'
			+ '--ALTER DATABASE [dbname]\n'
			+ '--SET QUERY_STORE\n'
			+ '--(\n'
			+ '--\n'
			+ '--    -- OPERATION_MODE: READ_WRITE (default) | READ_ONLY\n'
			+ '--    --   READ_WRITE  - Query Store actively collects runtime stats and plan data.\n'
			+ '--    --   READ_ONLY   - Existing data is queryable but no new data is captured.\n'
			+ '--    --   Note: switches to READ_ONLY automatically when MAX_STORAGE_SIZE_MB is reached.\n'
			+ '--    OPERATION_MODE              = READ_WRITE,\n'
			+ '--\n'
			+ '--    -- MAX_STORAGE_SIZE_MB: integer (default: 100 MB on SQL 2016/2017; 1000 MB on SQL 2019+)\n'
			+ '--    --   Maximum disk space allocated to Query Store per database.\n'
			+ '--    --   When this limit is hit the store flips to READ_ONLY until cleaned up.\n'
			+ '--    MAX_STORAGE_SIZE_MB         = 2048,\n'
			+ '--\n'
			+ '--    -- CLEANUP_POLICY / STALE_QUERY_THRESHOLD_DAYS: integer in days (default: 30)\n'
			+ '--    --   Queries that have not been executed for this many days are eligible for\n'
			+ '--    --   automatic removal when SIZE_BASED_CLEANUP_MODE = AUTO triggers a cleanup.\n'
			+ '--    CLEANUP_POLICY              = (STALE_QUERY_THRESHOLD_DAYS = 30),\n'
			+ '--\n'
			+ '--    -- DATA_FLUSH_INTERVAL_SECONDS: integer in seconds (default: 900 = 15 min)\n'
			+ '--    --   How often in-memory runtime stats are written asynchronously to disk.\n'
			+ '--    --   Lower values reduce data loss on crash but increase I/O; range: 60-900.\n'
			+ '--    DATA_FLUSH_INTERVAL_SECONDS = 900,\n'
			+ '--\n'
			+ '--    -- INTERVAL_LENGTH_MINUTES: integer in minutes (default: 60)\n'
			+ '--    --   Aggregation window for runtime execution statistics.\n'
			+ '--    --   Smaller = finer time granularity, more rows; larger = coarser, fewer rows.\n'
			+ '--    --   Typical values: 15, 30, 60 (recommended), 1440 (daily aggregates).\n'
			+ '--    INTERVAL_LENGTH_MINUTES     = 60,\n'
			+ '--\n'
			+ '--    -- SIZE_BASED_CLEANUP_MODE: AUTO (default) | OFF\n'
			+ '--    --   AUTO - automatically purges oldest/cheapest queries when storage reaches ~90%.\n'
			+ '--    --   OFF  - no automatic cleanup; store goes READ_ONLY at MAX_STORAGE_SIZE_MB.\n'
			+ '--    SIZE_BASED_CLEANUP_MODE     = AUTO,\n'
			+ '--\n'
			+ '--    -- MAX_PLANS_PER_QUERY: integer (default: 200)\n'
			+ '--    --   Maximum number of execution plans retained per query_id.\n'
			+ '--    --   Prevents runaway plan bloat for queries with highly variable plans.\n'
			+ '--    MAX_PLANS_PER_QUERY         = 200,\n'
			+ '--\n'
			+ '--    -- WAIT_STATS_CAPTURE_MODE: ON (default) | OFF   [SQL Server 2017+]\n'
			+ '--    --   ON  - capture per-query wait statistics (locks, I/O, memory, etc.) per interval.\n'
			+ '--    --   OFF - skip wait capture; reduces overhead on high-throughput OLTP workloads.\n'
			+ '--    WAIT_STATS_CAPTURE_MODE     = ON,\n'
			+ '--\n'
			+ '--    -- QUERY_CAPTURE_MODE: ALL | AUTO (default on 2019+) | CUSTOM (2019+ only) | NONE\n'
			+ '--    --   ALL    - capture every distinct query (default on SQL 2016/2017).\n'
			+ '--    --   AUTO   - capture only queries deemed significant by cost/frequency heuristics.\n'
			+ '--    --   CUSTOM - apply fine-grained thresholds defined in QUERY_CAPTURE_POLICY below.\n'
			+ '--    --   NONE   - stop capturing new queries; existing data is still readable.\n'
			+ '--    QUERY_CAPTURE_MODE          = AUTO,\n'
			+ '--\n'
			+ '--    -- QUERY_CAPTURE_POLICY: only effective when QUERY_CAPTURE_MODE = CUSTOM  [SQL 2019+]\n'
			+ '--    --   A query is captured only when ALL numeric thresholds below are exceeded\n'
			+ '--    --   within the STALE_CAPTURE_POLICY_THRESHOLD evaluation window.\n'
			+ '--    QUERY_CAPTURE_POLICY        =\n'
			+ '--    (\n'
			+ '--        -- STALE_CAPTURE_POLICY_THRESHOLD: evaluation window; range: 1 HOURS - 7 DAYS (default: 1 DAYS)\n'
			+ '--        --   The time period over which EXECUTION_COUNT and CPU thresholds are measured.\n'
			+ '--        STALE_CAPTURE_POLICY_THRESHOLD = 1 HOURS,\n'
			+ '--\n'
			+ '--        -- EXECUTION_COUNT: integer (default: 30)\n'
			+ '--        --   Query must execute at least this many times within the evaluation window\n'
			+ '--        --   to be considered for capture.\n'
			+ '--        EXECUTION_COUNT                = 30,\n'
			+ '--\n'
			+ '--        -- TOTAL_COMPILE_CPU_TIME_MS: integer in ms (default: 1000)\n'
			+ '--        --   Total compilation CPU time the query must consume within the window.\n'
			+ '--        --   High compile cost indicates a query worth tracking.\n'
			+ '--        TOTAL_COMPILE_CPU_TIME_MS      = 1000,\n'
			+ '--\n'
			+ '--        -- TOTAL_EXECUTION_CPU_TIME_MS: integer in ms (default: 100)\n'
			+ '--        --   Total execution CPU time (sum across all executions) within the window.\n'
			+ '--        --   Filters out trivial queries that run often but cost almost nothing.\n'
			+ '--        TOTAL_EXECUTION_CPU_TIME_MS    = 100\n'
			+ '--    )\n'
			+ '--)\n'
			+ '--GO\n'
			+ '--\n'
			+ '-- Reference:\n'
			+ '-- https://learn.microsoft.com/en-us/sql/relational-databases/performance/monitoring-performance-by-using-the-query-store\n'
			+ '-- https://learn.microsoft.com/en-us/sql/t-sql/statements/alter-database-transact-sql-set-options\n'
			+ '\n'
			+ '-- Enable Query Store for ALL user databases on the instance\n'
			+ 'DECLARE @sql NVARCHAR(MAX) = N\'\';\n'
			+ 'SELECT @sql += N\'ALTER DATABASE \' + QUOTENAME(name)\n'
			+ '            +  N\' SET QUERY_STORE = ON (OPERATION_MODE = READ_WRITE, MAX_STORAGE_SIZE_MB = 2048);\' + CHAR(13)\n'
			+ 'FROM sys.databases\n'
			+ 'WHERE database_id > 4                       -- skip system DBs\n'
			+ '  AND state_desc = \'ONLINE\'\n'
			+ '  AND is_query_store_on = 0;\n'
			+ 'EXEC sp_executesql @sql;\n'
			+ '\n'
			+ '-- Verify which databases currently have Query Store enabled\n'
			+ 'SELECT d.name,\n'
			+ '       d.is_query_store_on,\n'
			+ '       o.desired_state_desc,\n'
			+ '       o.actual_state_desc,\n'
			+ '       o.max_storage_size_mb,\n'
			+ '       o.current_storage_size_mb,\n'
			+ '       o.query_capture_mode_desc\n'
			+ 'FROM sys.databases d\n'
			+ 'LEFT JOIN sys.database_query_store_options o ON o.database_id = d.database_id\n'
			+ 'ORDER BY d.is_query_store_on DESC, d.name;\n';

		// ── Determine state from CmDatabases counts ──────────────────────────
		var qsDisabledCount = (totalUserDbs >= 0 && qsEnabledCount >= 0) ? (totalUserDbs - qsEnabledCount) : -1;
		var mainMsg, detailsSummary, showDetails;
		if (qsEnabledCount === 0 && totalUserDbs > 0) {
			// QS disabled on every user database
			mainMsg       = 'Query Store is <b>not enabled</b> on any of the ' + totalUserDbs + ' user database' + (totalUserDbs > 1 ? 's' : '') + ' on this server.';
			detailsSummary = '&#9881;&nbsp; Not enabled for any database &mdash; click for SQL to enable it';
			showDetails   = true;
		} else if (qsEnabledCount > 0 && qsDisabledCount > 0) {
			// QS enabled on some, missing on others
			mainMsg       = 'Query Store is enabled on <b>' + qsEnabledCount + '</b> of ' + totalUserDbs + ' user databases, but no data has been extracted yet.';
			detailsSummary = '&#9881;&nbsp; Missing on ' + qsDisabledCount + ' database' + (qsDisabledCount > 1 ? 's' : '') + ' &mdash; click for SQL to enable it';
			showDetails   = true;
		} else if (qsEnabledCount > 0 && qsDisabledCount === 0) {
			// QS enabled everywhere — just needs extraction
			mainMsg       = 'Query Store is enabled on all <b>' + qsEnabledCount + '</b> user database' + (qsEnabledCount > 1 ? 's' : '') + ', but no data has been extracted yet.';
			detailsSummary = '';
			showDetails   = false;
		} else {
			// Unknown (CmDatabases not yet available or server not yet monitored)
			mainMsg       = 'No databases with Query Store data found on this server.';
			detailsSummary = '&#9881;&nbsp; Query Store not enabled on the SQL Server? &mdash; click for SQL to enable it';
			showDetails   = true;
		}

		var dbListHtml = _qsBuildStatusSection(qsAllDbs, []);

		var detailsHtml = '';
		if (showDetails) {
			detailsHtml =
				  '<details style="margin-top:18px;border:1px solid #d0d0d0;border-radius:4px;background:#fafafa;">'
				+ '<summary style="padding:6px 10px;cursor:pointer;font-weight:600;color:#333;">'
				+ detailsSummary
				+ '</summary>'
				+ '<div style="padding:8px 12px;">'
				+ '<p style="font-size:0.85em;color:#555;margin-bottom:6px;">'
				+ 'Run the snippet below in SSMS or <code>sqlcmd</code> against the target instance to enable Query Store:'
				+ '</p>'
				+ '<div style="position:relative;">'
				+ '<button class="btn btn-sm btn-outline-secondary" style="position:absolute;top:6px;right:6px;font-size:0.75em;padding:2px 8px;" '
				+ 'onclick="(function(b){var t=b.parentNode.querySelector(\'pre\').innerText;'
				+ 'navigator.clipboard.writeText(t).then(function(){b.innerText=\'Copied!\';setTimeout(function(){b.innerText=\'Copy\';},1500);});})(this)">Copy</button>'
				+ '<pre style="margin:0;background:#1e1e1e;color:#dcdcdc;padding:10px 12px;border-radius:4px;font-size:0.78em;line-height:1.35;overflow:auto;max-height:420px;">'
				+ escHtml(enableSql)
				+ '</pre>'
				+ '</div>'
				+ '<p style="font-size:0.78em;color:#777;margin-top:8px;margin-bottom:0;">'
				+ 'After enabling, wait at least one <code>INTERVAL_LENGTH_MINUTES</code> for runtime stats to accumulate, then click <b>Extract now</b> above.'
				+ '</p>'
				+ '</div>'
				+ '</details>';
		}

		$('#query-store-content').html(
			'<div style="padding:18px 12px;">'
			+ '<p style="margin-bottom:10px;">' + mainMsg + '</p>'
			+ '<p style="color:#6c757d;font-size:0.88em;margin-bottom:14px;">'
			+ 'Data is collected during the nightly database rollover. '
			+ 'Click <b>Extract now</b> to pull Query Store data from the live SQL Server immediately.<br>'
			+ 'Or click <b>Look at Yesterday\'s data</b> to switch to the most recent historical recording.</p>'
			+ '<div style="display:flex;gap:8px;align-items:center;flex-wrap:wrap;">'
			+ '<button class="btn btn-sm btn-primary" onclick="queryStoreTriggerExtractInline(this);">'
			+ '<i class="fa fa-download"></i>&nbsp; Extract now</button>'
			+ '<button class="btn btn-sm btn-outline-secondary" onclick="qsShowYesterdayData();">'
			+ '&#128197;&nbsp; Look at Yesterday\'s data</button>'
			+ '</div>'
			+ '<span id="qs-inline-extract-status" style="display:block;font-size:0.85em;margin-top:8px;"></span>'
			+ dbListHtml
			+ detailsHtml
			+ '</div>');
		return;
	}

	// Total days selectable in the date dropdown (today + historical recordings)
	var daysAvail = _qsAvailDates.length + 1;

	var dbTips = {
		dbname:              'Database name',
		daysAvailable:       'Number of days available in the date selector (today + historical recordings)',
		actualState:         'Current Query Store state as reported by SQL Server (READ_WRITE, READ_ONLY, OFF, …)',
		storageUsedPct:      'Percentage of the Query Store max storage size currently used',
		currentStorageMb:    'Amount of storage currently used by the Query Store, in MB',
		maxStorageMb:        'Maximum storage size configured for the Query Store, in MB',
		queryCount:          'Number of distinct queries captured in the Query Store for this database',
		planCount:           'Number of distinct execution plans captured',
		daysInStorage:       'Number of days of data currently retained (based on earliest/latest timestamps)',
		earliest:            'Timestamp of the oldest execution interval in the Query Store data',
		latest:              'Timestamp of the most recent execution interval in the Query Store data',
		stateIsOk:           'Whether the Query Store state is healthy (READ_WRITE)',
		willExceedMaxStorage:'Warning: the Query Store is close to its maximum storage limit'
	};

	// ── Multi-DB toolbar ──────────────────────────────────────────────────────────
	var periodOptions = [1,2,3,4,5,6,7,10,14,21,31].map(function(n) {
		return '<option value="' + n + '"' + (n === 7 ? ' selected' : '') + '>'
		     + n + (n === 1 ? ' day' : ' days') + '</option>';
	}).join('');

	var toolbarHtml =
		'<div id="qs-db-toolbar" style="display:flex;align-items:center;flex-wrap:wrap;gap:6px;margin-bottom:8px;padding:4px 0;">'
		+ '<span style="font-size:0.85em;font-weight:600;">Query selected for dbname:</span>'
		+ '<button class="btn btn-sm btn-outline-secondary" style="font-size:0.82em;padding:2px 10px;" onclick="qsDbSelectAll();"'
		+ ' title="Tick all database checkboxes">All</button>'
		+ '<button class="btn btn-sm btn-outline-secondary" style="font-size:0.82em;padding:2px 10px;" onclick="qsDbSelectNone();"'
		+ ' title="Clear all database checkboxes">None</button>'
		+ '<span style="font-size:0.82em;margin-left:4px;">Period:</span>'
		+ '<select id="qs-db-period" class="form-control form-control-sm d-inline-block" style="width:auto;font-size:0.82em;padding:1px 4px;height:auto;" onchange="qsDbUpdateQueryBtn();"'
		+ ' title="Number of recording days to include — fetches one day at a time and merges the results">' + periodOptions + '</select>'
		+ '<label style="margin:0;font-size:0.82em;font-weight:normal;cursor:pointer;"'
		+ ' title="ON: combine all fetched days into one row per query (weighted averages).&#010;OFF: show a separate row per database per day.">'
		+ '<input type="checkbox" id="qs-db-aggregate" style="margin-right:3px;" checked>&nbsp;Aggregate days</label>'
		+ '<button id="qs-db-query-btn" class="btn btn-sm btn-primary" style="font-size:0.82em;padding:2px 10px;" onclick="qsQuerySelectedDbs();" disabled'
		+ ' title="Fetch top queries for the checked databases across the chosen period and display a merged ranking">Query Selected</button>'
		+ '</div>';

	// ── Table columns (index 0 = checkbox, no sort) ───────────────────────────────
	var cols   = ['_chk','dbname','actualState','storageUsedPct','currentStorageMb','maxStorageMb',
	              'queryCount','planCount','daysInStorage','daysAvailable','earliest','latest',
	              'stateIsOk','willExceedMaxStorage'];
	var labels = ['','Database','State','Used %','Used MB','Max MB',
	              'Queries','Plans','Days stored','Days Avail.','Earliest','Latest',
	              'State OK','Near full'];

	var html = toolbarHtml;
	html += '<table id="qs-db-table" class="table table-sm table-bordered table-hover" style="font-size:0.82em;white-space:nowrap;cursor:pointer;width:auto;">';
	html += '<thead class="thead-light"><tr>';
	cols.forEach(function(col, idx) {
		if (col === '_chk') {
			html += '<th style="width:28px;text-align:center;" title="Select/deselect all">'
			      + '<input type="checkbox" id="qs-db-chk-all" onclick="qsDbToggleAll(this.checked);" title="Select all / deselect all"></th>';
		} else {
			var tip = dbTips[col] ? ' title="' + escHtml(dbTips[col]) + '"' : '';
			html += '<th style="cursor:pointer;user-select:none;"' + tip
			      + ' onclick="qsSortTable(\'qs-db-table\',' + idx + ');">'
			      + escHtml(labels[idx]) + ' <span class="sort-icon" style="font-size:0.75em;color:#999;"></span></th>';
		}
	});
	html += '</tr></thead><tbody>';

	dbs.forEach(function(db) {
		var dn      = db.dbname || '';
		var pct     = db.storageUsedPct != null ? parseFloat(db.storageUsedPct).toFixed(1) : '—';
		var stateOk = db.stateIsOk === 'YES' || db.stateIsOk === true;
		var nearFull= db.willExceedMaxStorage === 'YES' || db.willExceedMaxStorage === true;
		var rowCls  = nearFull ? ' class="table-warning"' : (!stateOk ? ' class="table-danger"' : '');
		html += '<tr' + rowCls + ' data-dbname="' + escHtml(dn) + '"'
		      + ' onclick="queryStoreSelectDb(this.dataset.dbname);"'
		      + ' title="Click to view Top Queries for \'' + escHtml(dn) + '\'">';
		// Checkbox — stop propagation so clicking the checkbox doesn't trigger the row click
		html += '<td style="text-align:center;" onclick="event.stopPropagation();">'
		      + '<input type="checkbox" class="qs-db-chk" data-dbname="' + escHtml(dn) + '"'
		      + ' onchange="qsDbUpdateQueryBtn();"></td>';
		html += '<td>' + escHtml(dn) + '</td>';
		html += '<td>' + escHtml(db.actualState || '—') + '</td>';
		html += '<td>' + pct + '%</td>';
		html += '<td>' + (db.currentStorageMb != null ? db.currentStorageMb : '—') + '</td>';
		html += '<td>' + (db.maxStorageMb     != null ? db.maxStorageMb     : '—') + '</td>';
		html += '<td>' + (db.queryCount       != null ? db.queryCount       : '—') + '</td>';
		html += '<td>' + (db.planCount        != null ? db.planCount        : '—') + '</td>';
		html += '<td>' + (db.daysInStorage    != null ? db.daysInStorage    : '—') + '</td>';
		html += '<td class="qs-days-avail-cell" style="text-align:center;">' + daysAvail + '</td>';
		html += '<td>' + escHtml(db.earliest  || '—') + '</td>';
		html += '<td>' + escHtml(db.latest    || '—') + '</td>';
		html += '<td>' + (stateOk ? '✓' : '<span style="color:red;">✗</span>') + '</td>';
		html += '<td>' + (nearFull ? '<span style="color:darkorange;">⚠</span>' : '—') + '</td>';
		html += '</tr>';
	});

	html += '</tbody></table>';
	html += '<p style="font-size:0.78em;color:#6c757d;margin-top:4px;">'
	      + 'Click a row to view top queries for the <b>selected</b> day for that database. '
	      + 'Or tick checkboxes and click <b>Query Selected</b> to aggregate queries across multiple databases.</p>';
	html += _qsBuildStatusSection(qsAllDbs, dbs.map(function(d) { return d.dbname; }));
	$('#query-store-content').html(html);
}

/**
 * Build the collapsible "Query Store Status — All Databases" section.
 * qsAllDbs  : array of {name, QsIsEnabled, QsIsOk, QsDesiredState, QsActualState,
 *                        QsMaxSizeInMb, QsUsedSpaceInMb, QsFreeSpaceInMb, QsUsedPct, QsReadOnlyReason}
 * extractedDbNames : array of dbname strings that already have extracted QS data
 */
function _qsBuildStatusSection(qsAllDbs, extractedDbNames)
{
	if (!qsAllDbs || qsAllDbs.length === 0) return '';

	var extractedSet = {};
	(extractedDbNames || []).forEach(function(n) { extractedSet[n.toLowerCase()] = true; });

	var enabledCount  = qsAllDbs.filter(function(d) { return d.QsIsEnabled; }).length;
	var disabledCount = qsAllDbs.length - enabledCount;
	var summary = qsAllDbs.length + ' databases &mdash; '
		+ '<span style="color:#155724;">' + enabledCount + ' enabled</span>'
		+ (disabledCount > 0 ? ', <span style="color:#721c24;">' + disabledCount + ' disabled</span>' : '');

	var th = function(label, title) {
		return '<th style="white-space:nowrap;font-size:0.8em;padding:3px 6px;background:#f0f0f0;border:1px solid #ccc;cursor:default;"'
			+ (title ? ' title="' + escHtml(title) + '"' : '') + '>' + label + '</th>';
	};
	var td = function(val, style) {
		return '<td style="font-size:0.8em;padding:2px 6px;border:1px solid #ddd;white-space:nowrap;' + (style || '') + '">'
			+ (val !== null && val !== undefined && val !== '' ? escHtml(String(val)) : '<span style="color:#aaa;">—</span>') + '</td>';
	};

	var _enableSqlForDb = function(dbName) {
		var n = '[' + dbName + ']';
		return '-- ============================================================\n'
			+ '-- Enable Query Store for: ' + dbName + '\n'
			+ '-- ============================================================\n'
			+ 'USE master\n'
			+ 'GO\n'
			+ 'ALTER DATABASE ' + n + ' SET QUERY_STORE = ON\n'
			+ 'GO\n'
			+ 'ALTER DATABASE ' + n + ' SET QUERY_STORE\n'
			+ '(\n'
			+ '    OPERATION_MODE          = READ_WRITE,\n'
			+ '    MAX_STORAGE_SIZE_MB     = 2048,\n'
			+ '    INTERVAL_LENGTH_MINUTES = 60\n'
			+ ')\n'
			+ 'GO\n'
			+ '--\n'
			+ '-- ============================================================\n'
			+ '-- Full option reference (all settings shown with descriptions)\n'
			+ '-- ============================================================\n'
			+ '--ALTER DATABASE ' + n + '\n'
			+ '--SET QUERY_STORE\n'
			+ '--(\n'
			+ '--\n'
			+ '--    -- OPERATION_MODE: READ_WRITE (default) | READ_ONLY\n'
			+ '--    --   READ_WRITE  - Query Store actively collects runtime stats and plan data.\n'
			+ '--    --   READ_ONLY   - Existing data is queryable but no new data is captured.\n'
			+ '--    --   Note: switches to READ_ONLY automatically when MAX_STORAGE_SIZE_MB is reached.\n'
			+ '--    OPERATION_MODE              = READ_WRITE,\n'
			+ '--\n'
			+ '--    -- MAX_STORAGE_SIZE_MB: integer (default: 100 MB on SQL 2016/2017; 1000 MB on SQL 2019+)\n'
			+ '--    --   Maximum disk space allocated to Query Store per database.\n'
			+ '--    --   When this limit is hit the store flips to READ_ONLY until cleaned up.\n'
			+ '--    MAX_STORAGE_SIZE_MB         = 2048,\n'
			+ '--\n'
			+ '--    -- CLEANUP_POLICY / STALE_QUERY_THRESHOLD_DAYS: integer in days (default: 30)\n'
			+ '--    --   Queries that have not been executed for this many days are eligible for\n'
			+ '--    --   automatic removal when SIZE_BASED_CLEANUP_MODE = AUTO triggers a cleanup.\n'
			+ '--    CLEANUP_POLICY              = (STALE_QUERY_THRESHOLD_DAYS = 30),\n'
			+ '--\n'
			+ '--    -- DATA_FLUSH_INTERVAL_SECONDS: integer in seconds (default: 900 = 15 min)\n'
			+ '--    --   How often in-memory runtime stats are written asynchronously to disk.\n'
			+ '--    --   Lower values reduce data loss on crash but increase I/O; range: 60-900.\n'
			+ '--    DATA_FLUSH_INTERVAL_SECONDS = 900,\n'
			+ '--\n'
			+ '--    -- INTERVAL_LENGTH_MINUTES: integer in minutes (default: 60)\n'
			+ '--    --   Aggregation window for runtime execution statistics.\n'
			+ '--    --   Smaller = finer time granularity, more rows; larger = coarser, fewer rows.\n'
			+ '--    --   Typical values: 15, 30, 60 (recommended), 1440 (daily aggregates).\n'
			+ '--    INTERVAL_LENGTH_MINUTES     = 60,\n'
			+ '--\n'
			+ '--    -- SIZE_BASED_CLEANUP_MODE: AUTO (default) | OFF\n'
			+ '--    --   AUTO - automatically purges oldest/cheapest queries when storage reaches ~90%.\n'
			+ '--    --   OFF  - no automatic cleanup; store goes READ_ONLY at MAX_STORAGE_SIZE_MB.\n'
			+ '--    SIZE_BASED_CLEANUP_MODE     = AUTO,\n'
			+ '--\n'
			+ '--    -- MAX_PLANS_PER_QUERY: integer (default: 200)\n'
			+ '--    --   Maximum number of execution plans retained per query_id.\n'
			+ '--    --   Prevents runaway plan bloat for queries with highly variable plans.\n'
			+ '--    MAX_PLANS_PER_QUERY         = 200,\n'
			+ '--\n'
			+ '--    -- WAIT_STATS_CAPTURE_MODE: ON (default) | OFF   [SQL Server 2017+]\n'
			+ '--    --   ON  - capture per-query wait statistics (locks, I/O, memory, etc.) per interval.\n'
			+ '--    --   OFF - skip wait capture; reduces overhead on high-throughput OLTP workloads.\n'
			+ '--    WAIT_STATS_CAPTURE_MODE     = ON,\n'
			+ '--\n'
			+ '--    -- QUERY_CAPTURE_MODE: ALL | AUTO (default on 2019+) | CUSTOM (2019+ only) | NONE\n'
			+ '--    --   ALL    - capture every distinct query (default on SQL 2016/2017).\n'
			+ '--    --   AUTO   - capture only queries deemed significant by cost/frequency heuristics.\n'
			+ '--    --   CUSTOM - apply fine-grained thresholds defined in QUERY_CAPTURE_POLICY below.\n'
			+ '--    --   NONE   - stop capturing new queries; existing data is still readable.\n'
			+ '--    QUERY_CAPTURE_MODE          = AUTO,\n'
			+ '--\n'
			+ '--    -- QUERY_CAPTURE_POLICY: only effective when QUERY_CAPTURE_MODE = CUSTOM  [SQL 2019+]\n'
			+ '--    QUERY_CAPTURE_POLICY        =\n'
			+ '--    (\n'
			+ '--        STALE_CAPTURE_POLICY_THRESHOLD = 1 HOURS,\n'
			+ '--        EXECUTION_COUNT                = 30,\n'
			+ '--        TOTAL_COMPILE_CPU_TIME_MS      = 1000,\n'
			+ '--        TOTAL_EXECUTION_CPU_TIME_MS    = 100\n'
			+ '--    )\n'
			+ '--)\n'
			+ '--GO\n'
			+ '--\n'
			+ '-- Reference:\n'
			+ '-- https://learn.microsoft.com/en-us/sql/relational-databases/performance/monitoring-performance-by-using-the-query-store\n'
			+ '-- https://learn.microsoft.com/en-us/sql/t-sql/statements/alter-database-transact-sql-set-options\n';
	};

	var tableHtml = '<table style="border-collapse:collapse;margin-top:6px;">'
		+ '<thead><tr>'
		+ th('Database')
		+ th('Copy SQL', 'Copy SQL to enable Query Store for this database')
		+ th('Enabled')
		+ th('Is OK', 'Desired state matches actual state')
		+ th('Desired State')
		+ th('Actual State')
		+ th('Max MB', 'QsMaxSizeInMb')
		+ th('Used MB', 'QsUsedSpaceInMb')
		+ th('Free MB', 'QsFreeSpaceInMb')
		+ th('Used %', 'QsUsedPct')
		+ th('ReadOnly Reason', 'QsReadOnlyReason — non-zero when QS flipped to read-only')
		+ th('Data Extracted', 'Whether QS data has been extracted into DbxTune storage')
		+ '</tr></thead><tbody>';

	qsAllDbs.forEach(function(db) {
		var dbName   = db.DBName || db.name || '';
		var enabled  = db.QsIsEnabled;
		var isOk     = db.QsIsOk;
		var hasData  = !!extractedSet[dbName.toLowerCase()];

		var rowBg    = enabled ? '#f0fff0' : '#fff0f0';
		var enLabel  = enabled
			? '<span style="color:#155724;font-weight:600;">&#10003; Yes</span>'
			: '<span style="color:#721c24;font-weight:600;">&#10007; No</span>';
		var okLabel  = isOk === null || isOk === undefined ? '<span style="color:#aaa;">—</span>'
			: (String(isOk).toUpperCase() === 'TRUE' || isOk === true)
				? '<span style="color:#155724;">&#10003;</span>'
				: '<span style="color:#721c24;">&#10007;</span>';
		var dataLabel = hasData
			? '<span style="color:#155724;">&#10003;</span>'
			: '<span style="color:#aaa;">—</span>';

		var copyCell = '<td style="font-size:0.8em;padding:2px 6px;border:1px solid #ddd;text-align:center;">';
		if (!enabled) {
			var sqlText = _enableSqlForDb(dbName);
			copyCell += '<button class="btn btn-sm btn-outline-secondary" '
				+ 'style="font-size:0.75em;padding:1px 6px;white-space:nowrap;" '
				+ 'title="' + escHtml(sqlText) + '" '
				+ 'onclick="(function(b,s){'
				+ 'if(navigator.clipboard&&navigator.clipboard.writeText){'
				+ 'navigator.clipboard.writeText(s).then(function(){b.textContent=\'Copied!\';setTimeout(function(){b.textContent=\'Copy SQL to Enable\';},1500);});'
				+ '}else{'
				+ 'var t=document.createElement(\'textarea\');t.value=s;t.style.position=\'fixed\';t.style.opacity=\'0\';document.body.appendChild(t);t.focus();t.select();'
				+ 'try{document.execCommand(\'copy\');b.textContent=\'Copied!\';setTimeout(function(){b.textContent=\'Copy SQL to Enable\';},1500);}catch(e){alert(\'Copy failed: \'+e);}'
				+ 'document.body.removeChild(t);}'
				+ '})(this,' + escHtml(JSON.stringify(sqlText)) + ')">Copy SQL to Enable</button>';
		}
		copyCell += '</td>';

		tableHtml += '<tr style="background:' + rowBg + ';">'
			+ td(dbName, 'font-weight:600;')
			+ copyCell
			+ '<td style="font-size:0.8em;padding:2px 6px;border:1px solid #ddd;text-align:center;">' + enLabel + '</td>'
			+ '<td style="font-size:0.8em;padding:2px 6px;border:1px solid #ddd;text-align:center;">' + okLabel + '</td>'
			+ td(db.QsDesiredState)
			+ td(db.QsActualState)
			+ td(db.QsMaxSizeInMb)
			+ td(db.QsUsedSpaceInMb)
			+ td(db.QsFreeSpaceInMb)
			+ td(db.QsUsedPct !== null && db.QsUsedPct !== undefined ? db.QsUsedPct + '%' : null)
			+ td(db.QsReadOnlyReason)
			+ '<td style="font-size:0.8em;padding:2px 6px;border:1px solid #ddd;text-align:center;">' + dataLabel + '</td>'
			+ '</tr>';
	});
	tableHtml += '</tbody></table>';

	return '<details style="margin-top:14px;border:1px solid #d0d0d0;border-radius:4px;background:#fafafa;">'
		+ '<summary style="padding:6px 10px;cursor:pointer;font-weight:600;color:#333;font-size:0.9em;">'
		+ '&#128200; Query Store Status &mdash; All Databases (' + summary + ')'
		+ '</summary>'
		+ '<div style="padding:6px 10px 10px 10px;overflow-x:auto;">'
		+ tableHtml
		+ '</div>'
		+ '</details>';
}

// ── Multi-DB cross-database fetch ─────────────────────────────────────────────

/** All / None / Toggle helpers for the DB checkbox column */
function qsDbSelectAll()  { $('.qs-db-chk').prop('checked', true);  qsDbUpdateQueryBtn(); }
function qsDbSelectNone() { $('.qs-db-chk').prop('checked', false); qsDbUpdateQueryBtn(); }
function qsDbToggleAll(checked) { $('.qs-db-chk').prop('checked', checked); qsDbUpdateQueryBtn(); }

/** Enable/disable the "Query Selected" button; update its label with the count */
function qsDbUpdateQueryBtn()
{
	var n = $('.qs-db-chk:checked').length;
	$('#qs-db-query-btn').prop('disabled', n === 0);
	$('#qs-db-query-btn').text(n > 0 ? 'Query ' + n + ' DB' + (n === 1 ? '' : 's') : 'Query Selected');
}

/**
 * Begin serial cross-database fetch.
 * For each selected DB × each date in the period fetches top queries with a 3× over-fetch
 * so that the final merged + sorted top-N is representative.
 */
function qsQuerySelectedDbs()
{
	var dbs = [];
	$('.qs-db-chk:checked').each(function() { dbs.push($(this).data('dbname')); });
	if (dbs.length === 0) { alert('Please select at least one database.'); return; }

	var period    = parseInt($('#qs-db-period').val()) || 7;
	var aggregate = $('#qs-db-aggregate').prop('checked');
	var rankBy    = $('#query-store-rankby').val()           || 'cpu';
	var topN      = parseInt($('#query-store-topn').val())   || 25;
	var minExec   = parseInt($('#query-store-minexec').val()) || 2;

	// Dates to fetch: null = today/live, then historical (newest first)
	var allDates = [null].concat(_qsAvailDates);
	var dates    = allDates.slice(0, Math.min(period, allDates.length));

	var ctx = {
		dbs:         dbs,
		dates:       dates,
		dbIdx:       0,
		dateIdx:     0,
		allRows:     [],
		allTimelines: [],   // collect timeline arrays from each DB/day response
		schemaFlags: null,
		topN:        topN,
		rankBy:      rankBy,
		aggregate:   aggregate,
		minExec:     minExec,
		total:       dbs.length * dates.length,
		done:        0
	};

	_qsMultiDbCancel  = false;
	_qsMultiDbMode    = false;
	_qsMultiDbAllRows = null;
	_qsMultiDbOrigRows  = null;
	_qsMultiDbOrigTimelines = null;
	_qsMultiDbTimelines = null;
	_qsMultiDbCtx     = ctx;   // store for time-filter re-fetch

	// Switch to Top Queries tab and show progress indicator
	_qsMainTab = 'topqueries';
	_qsActivateTab('topqueries');
	$('#query-store-tq-bar').css('display', 'flex');
	$('#query-store-detail-bar').hide();
	queryStoreShowMsg(
		'<div id="qs-multidb-progress" style="padding:8px 0;">'
		+ '<b>Fetching Query Store data…</b>&nbsp;'
		+ '<span id="qs-multidb-prog-txt">0 / ' + ctx.total + '</span>'
		+ '&nbsp;&nbsp;<button class="btn btn-sm btn-outline-danger" style="font-size:0.8em;"'
		+ ' onclick="_qsMultiDbCancel=true;$(\'#qs-multidb-progress\').html(\'<i>Cancelled.</i>\');">Cancel</button>'
		+ '</div>');

	_qsAdvanceFetch(ctx);
}

/** Fetches one (dbIdx, dateIdx) cell then recurses until all cells are done. */
function _qsAdvanceFetch(ctx)
{
	if (_qsMultiDbCancel) return;

	if (ctx.dbIdx >= ctx.dbs.length) {
		// All cells fetched
		_qsMergeAndRender(ctx.allRows, ctx.allTimelines, ctx.schemaFlags || {}, ctx.rankBy, ctx.topN, ctx.aggregate);
		return;
	}

	var dbname   = ctx.dbs[ctx.dbIdx];
	var dateKey  = ctx.dates[ctx.dateIdx]; // null = live/today, 'YYYY-MM-DD' = historical
	var ts       = dateKey ? (dateKey + ' 23:59:59') : '';
	var dayLabel = dateKey || 'today';
	var overFetch = Math.min(ctx.topN * 3, 200);

	var progEl = document.getElementById('qs-multidb-prog-txt');
	if (progEl) progEl.textContent =
		(ctx.done + 1) + ' / ' + ctx.total + ' — ' + dbname + ' @ ' + dayLabel;

	var params = { srv: _qsSrvName, action: 'topQueries', dbname: dbname,
	               rankBy: ctx.rankBy, topN: overFetch, minExecutions: ctx.minExec };
	if (ts)              params.ts        = ts;
	if (ctx.startTime)   params.startTime = ctx.startTime;
	if (ctx.endTime)     params.endTime   = ctx.endTime;

	console.log('[QS] _qsAdvanceFetch —', dbname, '@', dayLabel,
	    'filter:', ctx.startTime ? (ctx.startTime + ' → ' + ctx.endTime) : '(none)');

	$.ajax({
		url:      '/api/cc/mgt/query-store',
		data:     params,
		dataType: 'text',
		success: function(data) {
			if (_qsMultiDbCancel) return;
			try {
				var r = JSON.parse(data);
				console.log('[QS] response —', dbname, '@', dayLabel,
				    'queries:', r.queries ? r.queries.length : 0,
				    'filterStart:', r.filterStartTime || '(none)',
				    'filterEnd:',   r.filterEndTime   || '(none)');
				if (!r.error && r.queries && r.queries.length > 0) {
					r.queries.forEach(function(row) {
						row._dbname = dbname;
						row._day    = dayLabel;
					});
					ctx.allRows = ctx.allRows.concat(r.queries);
					// Capture schema flags from the first successful response
					if (!ctx.schemaFlags) {
						ctx.schemaFlags = {
							hasMemoryCols:      r.hasMemoryCols,
							has2017Cols:        r.has2017Cols,
							hasCompileDuration: r.hasCompileDuration,
							schemaHasDopCol:    r.schemaHasDopCol
						};
					}
				}
				// Collect timeline for brush/wait charts (even if queries array was empty)
				if (!r.error && r.timeline && r.timeline.length > 0) {
					ctx.allTimelines = ctx.allTimelines.concat(r.timeline);
				}
			} catch(ex) { /* skip unparseable responses */ }
			_qsNextFetchCell(ctx);
		},
		error: function() {
			if (_qsMultiDbCancel) return;
			_qsNextFetchCell(ctx);
		}
	});
}

/** Advance to the next (db, date) cell and continue fetching. */
function _qsNextFetchCell(ctx)
{
	ctx.done++;
	ctx.dateIdx++;
	if (ctx.dateIdx >= ctx.dates.length) {
		ctx.dateIdx = 0;
		ctx.dbIdx++;
	}
	_qsAdvanceFetch(ctx);
}

/**
 * Merge raw tagged rows and render the Top Queries table.
 * aggregate=true  → group by (dbname, queryId), weighted-average metrics across days
 * aggregate=false → keep each (dbname, day) row separately with a Day column
 */
function _qsMergeAndRender(allRows, allTimelines, schemaFlags, rankBy, topN, aggregate)
{
	if (allRows.length === 0) {
		queryStoreShowMsg('<i>No query data found for the selected databases / period.</i>');
		return;
	}

	var merged    = _qsMergeRows(allRows, aggregate);
	var metricKey = _qsRankMetricKey(rankBy);

	merged.sort(function(a, b) {
		var av = a[metricKey] != null ? Number(a[metricKey]) : 0;
		var bv = b[metricKey] != null ? Number(b[metricKey]) : 0;
		return bv - av;
	});

	// Cache for re-ranking without re-fetch.
	// Only overwrite "orig" when this is a fresh unfiltered fetch (no time filter active).
	_qsMultiDbAllRows             = allRows;
	_qsMultiDbTimelines           = allTimelines;
	if (!(_qsTimeRange && _qsTimeRange.startTime)) {
		// Unfiltered fetch — save as the baseline originals
		_qsMultiDbOrigRows      = allRows;
		_qsMultiDbOrigTimelines = allTimelines;
	}
	_qsMultiDbSchemaFlags         = schemaFlags;
	_qsMultiDbSchemaFlags._aggregate = aggregate;
	_qsMultiDbMode                = true;

	var fakeR = {
		queries:            merged.slice(0, topN),
		rankBy:             rankBy,
		hasMemoryCols:      schemaFlags.hasMemoryCols,
		has2017Cols:        schemaFlags.has2017Cols,
		hasCompileDuration: schemaFlags.hasCompileDuration,
		schemaHasDopCol:    schemaFlags.schemaHasDopCol,
		_multiDb:           true,
		_aggregate:         aggregate,
		timeline:           _qsMergeTimelines(allTimelines)
	};

	queryStoreRenderTopQueries(fakeR);
}

/**
 * Merge timeline arrays collected from multiple DB/day responses.
 * Intervals are grouped by startTime and all numeric metrics summed.
 * waitByCategory is also summed per category across intervals with the same startTime.
 * Returns the merged timeline sorted by startTime ascending.
 */
function _qsMergeTimelines(timelines)
{
	if (!timelines || timelines.length === 0) return [];
	var numericKeys = ['duration','cpu','reads','physReads','writes','executions','rowcount','memory','tempdb','log','waitMs'];
	var byStart = {};   // startTime → merged interval
	timelines.forEach(function(t) {
		var key = t.startTime || t.label || '';
		if (!byStart[key]) {
			byStart[key] = { startTime: t.startTime, endTime: t.endTime, label: t.label };
			numericKeys.forEach(function(k) { byStart[key][k] = 0; });
		}
		var m = byStart[key];
		numericKeys.forEach(function(k) {
			var v = t[k];
			if (v != null && !isNaN(v)) m[k] = (m[k] || 0) + Number(v);
		});
		// Merge waitByCategory
		if (t.waitByCategory) {
			if (!m.waitByCategory) m.waitByCategory = {};
			Object.keys(t.waitByCategory).forEach(function(cat) {
				var v = t.waitByCategory[cat];
				if (v != null && !isNaN(v)) m.waitByCategory[cat] = (m.waitByCategory[cat] || 0) + Number(v);
			});
		}
	});
	return Object.keys(byStart).sort().map(function(k) { return byStart[k]; });
}

/**
 * Merge raw rows.
 * Mode A (aggregate=true):  group by (dbname + queryId), weighted-average metrics.
 * Mode B (aggregate=false): return as-is — each row already carries _dbname + _day.
 */
function _qsMergeRows(allRows, aggregate)
{
	if (!aggregate) return allRows.slice();

	var weightedAvgCols = [
		'avgDurationUs','avgCpuUs','avgLogicalReads','avgPhysicalReads',
		'avgLogicalWrites','avgRowcount','avgMemoryMb','avgDop',
		'avgTempdbMb','avgLogMb','avgCompileDurationUs'
	];
	var maxCols  = ['maxDurationUs','maxLogicalReads'];
	var sumCols  = [
		'totalDurationUs','totalCpuUs','totalLogicalReads','totalPhysicalReads',
		'totalLogicalWrites','totalRowcount','totalMemoryMb','totalTempdbMb',
		'totalLogMb','countCompiles'
	];

	var groups = {};
	allRows.forEach(function(row) {
		var key = (row._dbname || '') + '|' + row.queryId;
		if (!groups[key]) groups[key] = { rows: [], _dbname: row._dbname, queryId: row.queryId };
		groups[key].rows.push(row);
	});

	var merged = [];
	Object.keys(groups).forEach(function(key) {
		var g    = groups[key];
		var rows = g.rows;
		var first = rows[0];
		var out   = {};

		// Scalar / text fields from first row
		['queryId','objectContext','querySqlTextPreview','_dbname'].forEach(function(c) { out[c] = first[c]; });
		out._day = rows.length > 1 ? rows.length + ' days' : (first._day || '');

		// Weighted average by totalExecutions
		var totalExec = 0;
		rows.forEach(function(r) { totalExec += (Number(r.totalExecutions) || 0); });
		out.totalExecutions = totalExec;

		weightedAvgCols.forEach(function(col) {
			if (first[col] == null) return;
			var wSum = 0;
			rows.forEach(function(r) { wSum += (Number(r[col]) || 0) * (Number(r.totalExecutions) || 0); });
			out[col] = totalExec > 0 ? Math.round(wSum / totalExec) : 0;
		});

		maxCols.forEach(function(col) {
			if (first[col] == null) return;
			var m = 0;
			rows.forEach(function(r) { m = Math.max(m, Number(r[col]) || 0); });
			out[col] = m;
		});

		sumCols.forEach(function(col) {
			if (first[col] == null) return;
			var s = 0;
			rows.forEach(function(r) { s += Number(r[col]) || 0; });
			out[col] = s;
		});

		// planCount: max across days (a plan may not appear every day)
		out.planCount = 0;
		rows.forEach(function(r) { out.planCount = Math.max(out.planCount, Number(r.planCount) || 0); });

		// firstSeen / lastSeen: earliest / latest across days
		out.firstSeen = rows.reduce(function(acc, r) {
			return (!acc || (r.firstSeen && r.firstSeen < acc)) ? r.firstSeen : acc;
		}, null) || first.firstSeen;
		out.lastSeen = rows.reduce(function(acc, r) {
			return (!acc || (r.lastSeen  && r.lastSeen  > acc)) ? r.lastSeen  : acc;
		}, null) || first.lastSeen;

		merged.push(out);
	});
	return merged;
}

/** Map a rankBy string to the JS property used for sorting merged rows. */
function _qsRankMetricKey(rankBy)
{
	switch (rankBy) {
		case 'cpu':            return 'totalCpuUs';
		case 'reads':          return 'totalLogicalReads';
		case 'physical_reads': return 'totalPhysicalReads';
		case 'writes':         return 'totalLogicalWrites';
		case 'executions':     return 'totalExecutions';
		case 'rowcount':       return 'totalRowcount';
		case 'memory':         return 'totalMemoryMb';
		case 'tempdb':         return 'totalTempdbMb';
		case 'log':            return 'totalLogMb';
		case 'compile':        return 'avgCompileDurationUs';
		case 'waittime':       return 'totalWaitMs';
		case 'plancount':      return 'planCount';
		case 'aborted':        return 'totalAbortedException';
		default:               return 'totalDurationUs';
	}
}

/** Called when a row is clicked in multi-db mode — navigates to detail for that DB/query. */
function queryStoreSelectMultiDbQuery(dbname, queryId, day)
{
	// Keep multi-db state intact so:
	//   (a) Detail can fetch from all N day-databases when the user clicked an aggregated row
	//   (b) Going back to Top Queries re-renders from the cached N-day results, not a fresh 1-day fetch
	// Only clean up chart/observer artefacts that belong to the canvas DOM being replaced.
	_qsBrushRefreshUi = null;
	if (_qsBrushResizeObs) { try { _qsBrushResizeObs.disconnect(); } catch(e) {} _qsBrushResizeObs = null; }
	_qsDetailDay      = day || null;   // remembered so queryStoreDetailLoad knows whether to multi-day fetch
	_qsDbname         = dbname;
	// If the day is a specific date (not 'today' or 'N days'), restore the timestamp
	if (day && day !== 'today' && !/\d+ days?$/.test(day)) {
		_qsTimestamp = day + ' 23:59:59';
		_qsUpdateHistoricalBanner();
	}
	queryStoreSelectQuery(queryId);
}

// ── End multi-DB helpers ──────────────────────────────────────────────────────

/** User clicked a DB row → go to Top Queries tab for that DB */
function queryStoreSelectDb(dbname)
{
	_qsDbname         = dbname;
	_qsQueryId        = null;
	_qsMultiDbMode    = false;   // leaving multi-db view → single DB
	_qsMultiDbAllRows = null;
	_qsMultiDbOrigRows  = null;
	_qsMultiDbOrigTimelines = null;
	_qsMultiDbTimelines = null;
	_qsMultiDbCtx     = null;
	_qsBrushRefreshUi = null;
	if (_qsBrushResizeObs) { try { _qsBrushResizeObs.disconnect(); } catch(e) {} _qsBrushResizeObs = null; }
	_qsTimeRange      = { startTime: null, endTime: null };  // reset brush selection
	$('#query-store-tq-dbname').text(dbname);
	$('#query-store-detail-dbname').text(dbname);
	_qsMainTab = 'topqueries';
	_qsActivateTab('topqueries');
	$('#query-store-tq-bar').css('display', 'flex');
	$('#query-store-detail-bar').hide();
	queryStoreTopQueriesLoad();
}

// ── Top Queries tab ───────────────────────────────────────────────────────────

function queryStoreTopQueriesLoad()
{
	_qsDestroyTqSplits();
	// Dispose any lingering Bootstrap tooltips from previous render — otherwise they
	// stay "hanging" and orphaned because their trigger elements are about to be removed.
	try {
		$('#query-store-content [data-toggle="tooltip"]').tooltip('dispose');
		$('.tooltip.show, .tooltip.fade').remove();
	} catch (ex) {}

	// In multi-db mode: re-rank from cache when no time filter, re-fetch when filter active
	if (_qsMultiDbMode && _qsMultiDbAllRows) {
		var rankBy    = $('#query-store-rankby').val()          || 'cpu';
		var topN      = parseInt($('#query-store-topn').val())  || 25;
		var aggregate = _qsMultiDbSchemaFlags && _qsMultiDbSchemaFlags._aggregate !== false;
		var hasFilter = _qsTimeRange && (_qsTimeRange.startTime || _qsTimeRange.endTime);
		if (hasFilter && _qsMultiDbCtx) {
			// Re-fetch all DBs/days with the time filter applied
			var refetchCtx = {
				dbs:         _qsMultiDbCtx.dbs,
				dates:       _qsMultiDbCtx.dates,
				dbIdx:       0,
				dateIdx:     0,
				allRows:     [],
				allTimelines: [],
				schemaFlags: null,
				topN:        topN,
				rankBy:      rankBy,
				aggregate:   aggregate,
				minExec:     _qsMultiDbCtx.minExec,
				total:       _qsMultiDbCtx.dbs.length * _qsMultiDbCtx.dates.length,
				done:        0,
				startTime:   _qsTimeRange.startTime,
				endTime:     _qsTimeRange.endTime
			};
			_qsMultiDbCancel = false;
			_qsMultiDbMode   = false;
			_qsMultiDbCtx    = refetchCtx;
			queryStoreShowMsg(
				'<div id="qs-multidb-progress" style="padding:8px 0;">'
				+ '<b>Re-fetching with time filter…</b>&nbsp;'
				+ '<span id="qs-multidb-prog-txt">0 / ' + refetchCtx.total + '</span>'
				+ '&nbsp;&nbsp;<button class="btn btn-sm btn-outline-danger" style="font-size:0.8em;"'
				+ ' onclick="_qsMultiDbCancel=true;$(\'#qs-multidb-progress\').html(\'<i>Cancelled.</i>\');">Cancel</button>'
				+ '</div>');
			_qsAdvanceFetch(refetchCtx);
		} else {
			// No active filter — restore from unfiltered originals so resetting the brush
			// shows the full dataset rather than whatever time-filtered slice was last fetched.
			var baseRows  = (_qsMultiDbOrigRows      || _qsMultiDbAllRows);
			var baseTimes = (_qsMultiDbOrigTimelines  || _qsMultiDbTimelines || []);
			_qsMergeAndRender(baseRows, baseTimes, _qsMultiDbSchemaFlags || {}, rankBy, topN, aggregate);
		}
		return;
	}

	var srv    = _qsSrvName;
	var dbname = _qsDbname;
	if (!srv || !dbname) return;

	var rankBy  = $('#query-store-rankby').val()  || 'cpu';
	var topN    = $('#query-store-topn').val()     || '25';
	var minExec = $('#query-store-minexec').val()  || '2';
	var ts      = _qsTimestamp || '';

	var params = { srv: srv, action: 'topQueries', dbname: dbname,
	               rankBy: rankBy, topN: topN, minExecutions: minExec };
	if (ts) params.ts = ts;
	if (_qsTimeRange && _qsTimeRange.startTime && _qsTimeRange.endTime) {
		params.startTime = _qsTimeRange.startTime;
		params.endTime   = _qsTimeRange.endTime;
	}
	console.log('[QS] topQueries fetch — db:', dbname,
	    'filter:', params.startTime ? (params.startTime + ' → ' + params.endTime) : '(none)');

	_qsTqLastResult = null;   // clear cache on fresh load
	$('#query-store-loading').show();
	$('#query-store-content').html('');

	$.ajax({
		url:      '/api/cc/mgt/query-store',
		data:     params,
		dataType: 'text',
		success: function(data) {
			$('#query-store-loading').hide();
			try {
				var r = JSON.parse(data);
				console.log('[QS] single-db response — queries:', r.queries ? r.queries.length : 0,
				    'filterStart:', r.filterStartTime || '(none)',
				    'filterEnd:',   r.filterEndTime   || '(none)');
				if (r.error) { queryStoreShowMsg(r.message || r.error); return; }
				queryStoreRenderTopQueries(r);
			} catch(ex) { queryStoreShowMsg('Parse error: ' + ex); }
		},
		error: function(xhr) {
			$('#query-store-loading').hide();
			queryStoreShowHttpError(xhr, 'topQueries');
		}
	});
}

function queryStoreRenderTopQueries(r)
{
	_qsTqLastResult = r;   // cache for re-filtering

	var allQueries = r.queries || [];
	if (allQueries.length === 0) {
		queryStoreShowMsg(
			'No queries found for database <b>' + escHtml(_qsDbname) + '</b>.<br>'
			+ '<small style="color:#6c757d;">Try <b>Extract now</b> to pull Query Store data from the live SQL Server.</small>');
		$('#qs-tq-filter-count').text('');
		return;
	}

	var rankBy      = r.rankBy             || 'cpu';
	var hasMemory   = r.hasMemoryCols      === true;
	var has2017     = r.has2017Cols        === true;
	var hasCompile  = r.hasCompileDuration === true;
	var hasAborted  = r.hasAbortedCols     === true;
	var hasWaitStat = r.hasWaitStats       === true;
	var isMultiDb   = r._multiDb           === true;
	var showDay     = isMultiDb && r._aggregate === false;

	// ── Build column list dynamically based on server capabilities ────────────
	// Columns are grouped by metric — each metric's avg/max/total together for easy scanning.
	var cols   = ['queryId','objectContext','querySqlTextPreview','planCount','totalExecutions','_spark_executions','_efficiencyPct'];
	var labels = ['Query ID','Object / Context','SQL Preview','Plans','Executions','📈 Executions','CPU Eff %'];
	// Duration group
	cols.push('avgDurationUs','maxDurationUs','totalDurationUs','_spark_duration');
	labels.push('Avg Elapsed (ms)','Max Elapsed (ms)','Total Elapsed (ms)','📈 Avg Elapsed');
	// CPU group
	cols.push('avgCpuUs','totalCpuUs','_spark_cpu');
	labels.push('Avg CPU (ms)','Total CPU (ms)','📈 Avg CPU');
	// Wait time group
	cols.push('avgWaitMs','maxWaitMs','totalWaitMs','_spark_waitMs');
	labels.push('Avg Wait (ms)','Max Wait (ms)','Total Wait (ms)','📈 Wait Time');
	// Logical reads group
	cols.push('avgLogicalReads','maxLogicalReads','totalLogicalReads','_spark_reads');
	labels.push('Avg Reads','Max Reads','Total Reads','📈 Avg Reads');
	// Physical reads group
	cols.push('avgPhysicalReads','totalPhysicalReads','_spark_physReads');
	labels.push('Avg Phys Reads','Total Phys Reads','📈 Avg Phys Reads');
	// Logical writes group
	cols.push('avgLogicalWrites','totalLogicalWrites','_spark_writes');
	labels.push('Avg Writes','Total Writes','📈 Avg Writes');
	// Rows group
	cols.push('avgRowcount','totalRowcount','_spark_rowcount');
	labels.push('Avg Rows','Total Rows','📈 Avg Rows');
	// Optional groups — pushed in metric order, avg+total together
	if (hasMemory) {
		cols.push('avgMemoryMb','totalMemoryMb','_spark_memory');
		labels.push('Avg Mem (MB)','Total Mem (MB)','📈 Avg Mem');
	}
	if (has2017) {
		cols.push('avgDop');
		labels.push('Avg DOP');
		cols.push('avgTempdbMb','totalTempdbMb','_spark_tempdb');
		labels.push('Avg Tempdb (MB)','Total Tempdb (MB)','📈 Avg Tempdb');
		cols.push('avgLogMb','totalLogMb','_spark_log');
		labels.push('Avg Log (MB)','Total Log (MB)','📈 Avg Log');
	} else if (r.schemaHasDopCol === true) {
		cols.push('avgDop');
		labels.push('Avg DOP');
	}
	if (hasCompile) {
		cols.push('avgCompileDurationUs','countCompiles');
		labels.push('Avg Compile (ms)','# Compiles');
	}
	cols.push('totalAbortedException');
	labels.push('Aborted+Exc');
	cols.push('firstSeen','lastSeen');
	labels.push('First seen','Last seen');

	// Multi-DB: prepend DB column (and Day column when not aggregated)
	if (isMultiDb) { cols.unshift('_dbname'); labels.unshift('DB'); }
	if (showDay)   { cols.splice(1, 0, '_day'); labels.splice(1, 0, 'Day'); }

	var tips = {
		_dbname:               'Database this query belongs to',
		_day:                  'Recording date (or number of days aggregated)',
		_efficiencyPct:        'CPU Efficiency = (Avg CPU \u00f7 Avg DOP) \u00f7 Avg Elapsed \u00d7 100. '
		                     + 'Measures how much of the wall-clock time was actual CPU work, corrected for parallelism. '
		                     + '\u2265\u202f70%: CPU-bound \u2014 tune algorithm/index. '
		                     + '30\u201370%: mixed \u2014 some waiting, check wait stats. '
		                     + '<\u202f30%: wait-dominated \u2014 I/O, locks, memory grants, or network are the bottleneck. '
		                     + 'Values >100% can occur due to timing granularity; treat as ~100%.',
		queryId:               'Unique Query Store query identifier',
		objectContext:         'Stored procedure, function or ad-hoc context the query belongs to',
		planCount:             'Number of distinct execution plans recorded for this query',
		totalExecutions:       'Total number of executions in this recording period',
		avgDurationUs:         'Average elapsed (wall-clock) time per execution, in milliseconds',
		avgCpuUs:              'Average CPU time consumed per execution, in milliseconds',
		avgLogicalReads:       'Average logical (buffer pool) page reads per execution (1 page = 8 KB)',
		avgPhysicalReads:      'Average physical disk page reads per execution (1 page = 8 KB; high value → cache misses)',
		avgLogicalWrites:      'Average logical (buffer pool) page writes per execution (1 page = 8 KB)',
		avgRowcount:           'Average number of rows returned/affected per execution',
		totalDurationUs:       'Sum of elapsed time across all executions, in milliseconds',
		totalCpuUs:            'Sum of CPU time across all executions, in milliseconds',
		totalLogicalReads:     'Sum of logical page reads across all executions (1 page = 8 KB)',
		totalPhysicalReads:    'Sum of physical disk page reads across all executions (1 page = 8 KB)',
		totalLogicalWrites:    'Sum of logical page writes across all executions (1 page = 8 KB)',
		totalRowcount:         'Sum of rows returned/affected across all executions',
		maxDurationUs:         'Longest single execution elapsed time observed, in milliseconds',
		maxLogicalReads:       'Peak logical page reads observed in a single execution (1 page = 8 KB)',
		avgMemoryMb:           'Average memory grant used per execution, in MB (8-KB pages ÷ 128)',
		totalMemoryMb:         'Total memory grant across all executions, in MB',
		avgDop:                'Average degree of parallelism used per execution (SQL Server 2016+)',
		avgTempdbMb:           'Average tempdb space used per execution in MB (SQL Server 2017+)',
		totalTempdbMb:         'Total tempdb space used across all executions in MB (SQL Server 2017+)',
		avgLogMb:              'Average transaction log generated per execution in MB (SQL Server 2017+)',
		totalLogMb:            'Total transaction log generated across all executions in MB (SQL Server 2017+)',
		avgCompileDurationUs:  'Average query compile/recompile duration in milliseconds (from query_store_query — SQL Server 2019+)',
		countCompiles:         'Number of times this query was compiled or recompiled (from query_store_query — SQL Server 2019+)',
		avgWaitMs:             'Average total wait time per execution in milliseconds (SQL Server 2017+)',
		maxWaitMs:             'Maximum total wait time observed in a single Query Store interval (SQL Server 2017+)',
		totalWaitMs:           'Total wait time accumulated across all executions in milliseconds (SQL Server 2017+)',
		_spark_waitMs:         'Timeline: Total Wait Time (ms) per Query Store interval — shows WHEN this query was waiting',
		totalAbortedException: 'Total count of aborted + exception executions in this recording period',
		firstSeen:             'Timestamp when this query was first recorded in the Query Store',
		lastSeen:              'Timestamp of the most recent execution recorded',
		querySqlTextPreview:   'First 90 characters of the query SQL text — hover for full text',
		_spark_executions:     'Timeline: Execution count per Query Store interval — all queries aligned to the same time range; zero where the query had no executions',
		_spark_duration:       'Timeline: Avg Elapsed (ms) per Query Store interval — all queries aligned to the same time range; zero where the query had no executions',
		_spark_cpu:            'Timeline: Avg CPU (ms) per Query Store interval — all queries aligned to the same time range; zero where the query had no executions',
		_spark_reads:          'Timeline: Avg Logical Reads per Query Store interval — all queries aligned to the same time range; zero where the query had no executions',
		_spark_physReads:      'Timeline: Avg Physical Reads per Query Store interval — all queries aligned to the same time range; zero where the query had no executions',
		_spark_writes:         'Timeline: Avg Logical Writes per Query Store interval — all queries aligned to the same time range; zero where the query had no executions',
		_spark_rowcount:       'Timeline: Avg Row Count per Query Store interval — all queries aligned to the same time range; zero where the query had no executions',
		_spark_memory:         'Timeline: Avg Memory Grant (MB) per Query Store interval — all queries aligned to the same time range; zero where the query had no executions',
		_spark_tempdb:         'Timeline: Avg Tempdb Usage (MB) per Query Store interval — all queries aligned to the same time range; zero where the query had no executions',
		_spark_log:            'Timeline: Avg Log Usage (MB) per Query Store interval — all queries aligned to the same time range; zero where the query had no executions'
	};

	// Apply filter
	var queries = allQueries;
	var filterErr = '';
	if (_qsTqFilter && typeof applyRowFilter === 'function') {
		var rowArrays = allQueries.map(function(q) { return cols.map(function(c) { return q[c]; }); });
		var res = applyRowFilter(rowArrays, cols, _qsTqFilter);
		filterErr = res.filterError || '';
		queries = res.filteredRows.map(function(arr) {
			var q = {}; cols.forEach(function(c, i) { q[c] = arr[i]; }); return q;
		});
	}
	$('#qs-tq-filter-count').text(
		filterErr ? filterErr
		: _qsTqFilter ? queries.length + ' / ' + allQueries.length + ' rows'
		: allQueries.length + ' quer' + (allQueries.length === 1 ? 'y' : 'ies'));

	if (queries.length === 0) {
		queryStoreShowMsg('No queries match filter <b>' + escHtml(_qsTqFilter) + '</b>.');
		return;
	}

	// Highlight rank-by column headers
	var rankColSet = {};
	if      (rankBy === 'duration')        { rankColSet['avgDurationUs']        = true; rankColSet['totalDurationUs']         = true; }
	else if (rankBy === 'cpu')             { rankColSet['avgCpuUs']             = true; rankColSet['totalCpuUs']              = true; }
	else if (rankBy === 'reads')           { rankColSet['avgLogicalReads']      = true; rankColSet['maxLogicalReads']         = true; rankColSet['totalLogicalReads']  = true; }
	else if (rankBy === 'physical_reads')  { rankColSet['avgPhysicalReads']     = true; rankColSet['totalPhysicalReads']      = true; }
	else if (rankBy === 'writes')          { rankColSet['avgLogicalWrites']     = true; rankColSet['totalLogicalWrites']      = true; }
	else if (rankBy === 'executions')      { rankColSet['totalExecutions']      = true; }
	else if (rankBy === 'rowcount')        { rankColSet['avgRowcount']          = true; rankColSet['totalRowcount']           = true; }
	else if (rankBy === 'memory')          { rankColSet['avgMemoryMb']          = true; rankColSet['totalMemoryMb']          = true; }
	else if (rankBy === 'tempdb')          { rankColSet['avgTempdbMb']          = true; rankColSet['totalTempdbMb']           = true; }
	else if (rankBy === 'log')             { rankColSet['avgLogMb']             = true; rankColSet['totalLogMb']              = true; }
	else if (rankBy === 'compile')         { rankColSet['avgCompileDurationUs']  = true; }
	else if (rankBy === 'waittime')        { rankColSet['avgWaitMs'] = true; rankColSet['maxWaitMs'] = true; rankColSet['totalWaitMs'] = true; rankColSet['_spark_waitMs'] = true; }
	else if (rankBy === 'plancount')       { rankColSet['planCount']             = true; }
	else if (rankBy === 'aborted')         { rankColSet['totalAbortedException'] = true; }

	var html = '<table id="qs-tq-table" class="table table-sm table-bordered table-hover" style="font-size:0.82em;white-space:nowrap;cursor:pointer;width:auto;">';
	html += '<thead class="thead-light"><tr>';
	cols.forEach(function(col, idx) {
		var hlClass = rankColSet[col] ? ' qs-rank-hl' : '';
		var tip = tips[col] ? ' title="' + escHtml(tips[col]) + '"' : '';
		html += '<th class="' + hlClass.trim() + '" style="cursor:pointer;user-select:none;"'
		      + tip
		      + ' onclick="qsSortTable(\'qs-tq-table\',' + idx + ');">'
		      + escHtml(labels[idx]) + ' <span class="sort-icon" style="font-size:0.75em;color:#999;"></span></th>';
	});
	html += '</tr></thead><tbody>';

	// Numeric columns — apply thousands-separator formatting
	var numericCols = {
		planCount:1, totalExecutions:1,
		avgDurationUs:1, maxDurationUs:1, totalDurationUs:1,
		avgCpuUs:1, totalCpuUs:1,
		avgLogicalReads:1, maxLogicalReads:1, totalLogicalReads:1,
		avgPhysicalReads:1, totalPhysicalReads:1,
		avgLogicalWrites:1, totalLogicalWrites:1,
		avgRowcount:1, totalRowcount:1,
		avgCompileDurationUs:1, countCompiles:1,
		avgDop:1,
		avgWaitMs:1, maxWaitMs:1, totalWaitMs:1, totalAbortedException:1
	};
	// µs columns — also get a human-readable h/m/s tooltip
	var usCols = {
		avgDurationUs:1, maxDurationUs:1, totalDurationUs:1,
		avgCpuUs:1, totalCpuUs:1,
		avgCompileDurationUs:1
	};
	// page-count columns — get a human-readable KB/MB/GB tooltip (1 page = 8 KB)
	var readsCols = {
		avgLogicalReads:1, maxLogicalReads:1, totalLogicalReads:1,
		avgPhysicalReads:1, totalPhysicalReads:1,
		avgLogicalWrites:1, totalLogicalWrites:1
	};
	// MB columns — already in MB from server; render as "X.X MB" with 1 decimal
	var mbCols = {
		avgTempdbMb:1, totalTempdbMb:1, avgLogMb:1, totalLogMb:1,
		avgMemoryMb:1, totalMemoryMb:1
	};
	// Decimal columns (non-integer) — render with 1 decimal + thousands separator
	var decimalCols = { avgRowcount:1, totalRowcount:1, avgDop:1 };

	queries.forEach(function(q) {
		var qid = q.queryId != null ? String(q.queryId) : '';
		// Use data-* attributes to avoid double-quote conflicts inside onclick
		if (isMultiDb) {
			html += '<tr data-qid="' + escHtml(qid) + '"'
			      + ' data-dbname="' + escHtml(q._dbname || '') + '"'
			      + ' data-day="' + escHtml(q._day || '') + '"'
			      + ' onclick="queryStoreSelectMultiDbQuery(this.dataset.dbname,this.dataset.qid,this.dataset.day);"'
			      + ' title="Click to view detail for Query ID ' + escHtml(qid) + ' in ' + escHtml(q._dbname || '') + '">';
		} else {
			html += '<tr data-qid="' + escHtml(qid) + '"'
			      + ' onclick="queryStoreSelectQuery(this.dataset.qid);"'
			      + ' title="Click to view detail for Query ID ' + escHtml(qid) + '">';
		}
		cols.forEach(function(col) {
			var val = q[col];
			if (col === '_efficiencyPct') {
				var dur = Number(q.avgDurationUs) || 0;
				var cpu = Number(q.avgCpuUs)      || 0;
				var dop = (q.avgDop != null && Number(q.avgDop) > 0) ? Number(q.avgDop) : 1;
				if (dur > 0) {
					var eff = (cpu / dop) / dur * 100;
					var effDisp  = eff.toFixed(1) + '%';
					var effColor = eff >= 70 ? '#1a7a1a' : (eff >= 30 ? '#a05000' : '#cc0000');
					html += '<td style="text-align:right;font-weight:700;color:' + effColor + ';">' + effDisp + '</td>';
				} else {
					html += '<td style="text-align:right;color:#aaa;">—</td>';
				}
			} else if (col === '_dbname') {
				html += '<td style="font-weight:600;color:#495057;">' + escHtml(String(val || '')) + '</td>';
			} else if (col === '_day') {
				html += '<td style="color:#6c757d;font-size:0.9em;">' + escHtml(String(val || '')) + '</td>';
			} else if (col === 'queryId') {
				html += '<td style="color:#0066cc;font-weight:600;">' + escHtml(qid) + '</td>';
			} else if (col === 'querySqlTextPreview') {
				var preview = (val || '').substring(0, 120) + ((val || '').length > 120 ? '…' : '');
				html += '<td style="font-family:monospace;font-size:0.88em;max-width:300px;overflow:hidden;text-overflow:ellipsis;"'
				      + ' title="' + escHtml(val || '') + '">' + escHtml(preview) + '</td>';
			} else if (col.indexOf('_spark_') === 0) {
				var sparkKey   = col.substring(7);   // strip '_spark_'
				var sparkVals  = (q.spark  && q.spark[sparkKey])  || [];
				var sparkDates = (q.sparkDates) || [];
				// unit hint for tooltip: ms / pages / MB / (blank)
				var sparkUnit  = (sparkKey === 'duration' || sparkKey === 'cpu' || sparkKey === 'waitMs') ? 'ms'
				               : (sparkKey === 'memory' || sparkKey === 'tempdb' || sparkKey === 'log') ? 'MB'
				               : (sparkKey === 'reads'  || sparkKey === 'physReads' || sparkKey === 'writes') ? 'pages'
				               : '';
				var sparkColor = undefined;
				html += '<td style="padding:2px 4px;vertical-align:middle;">'
				      + (sparkVals.length
				            ? '<span class="qs-sparkline"'
				            +   ' data-vals="'  + escHtml(sparkVals .join(',')) + '"'
				            +   ' data-dates="' + escHtml(sparkDates.join('|')) + '"'
				            +   ' data-unit="'  + escHtml(sparkUnit) + '"'
				            +   (sparkColor ? ' data-color="' + sparkColor + '"' : '')
				            +   '></span>'
				            : '<span style="color:#ccc;">—</span>')
				      + '</td>';
			} else if (usCols[col]) {
				var ur = _qsFmtUsAsMs(val);
				html += '<td style="text-align:right;"' + (ur.tip ? ' title="' + escHtml(ur.tip) + '"' : '') + '>'
				      + (val != null ? escHtml(ur.disp) : '<span style="color:#aaa;">—</span>') + '</td>';
			} else if (mbCols[col]) {
				var mbDisp = val != null
					? (_qsFmt1Dec(val) || val.toFixed(1)) + ' MB'
					: '<span style="color:#aaa;">—</span>';
				html += '<td style="text-align:right;">' + mbDisp + '</td>';
			} else if (numericCols[col]) {
				var fmt = decimalCols[col] ? _qsFmt1Dec(val) : _qsFmtNum(val);
				var display = fmt !== null ? fmt : (val != null ? escHtml(String(val)) : '<span style="color:#aaa;">—</span>');
				var tip = readsCols[col] ? _qsPagesToMb(val) : '';
				html += '<td style="text-align:right;"' + (tip ? ' title="' + escHtml(tip) + '"' : '') + '>' + display + '</td>';
			} else {
				html += '<td>' + renderCell(val) + '</td>';
			}
		});
		html += '</tr>';
	});

	html += '</tbody></table>';
	if (isMultiDb) {
		var modeDesc = r._aggregate ? 'aggregated across days' : 'per-day rows';
		html += '<p style="font-size:0.78em;color:#6c757d;margin-top:4px;">'
		      + '&#x1F5C3; <b>Multi-DB view</b> — ' + modeDesc + ', ranked by <b>' + escHtml(rankBy) + '</b>. '
		      + 'Change Rank-by to re-sort from cache without re-fetching. '
		      + 'Click a row to drill into that database&#39;s query detail.</p>';
	} else {
		html += '<p style="font-size:0.78em;color:#6c757d;margin-top:4px;">'
		      + ' Ranked by <b>' + escHtml(rankBy) + '</b>. Click a row to view full detail.</p>';
	}

	// Active time-filter banner — shown above the table when a brush selection is in effect.
	// Uses the start/end times echoed back by the server (filterStartTime/filterEndTime),
	// falling back to _qsTimeRange when rendering from the multi-db merged path.
	var filterStart = (r.filterStartTime) || (_qsTimeRange && _qsTimeRange.startTime) || '';
	var filterEnd   = (r.filterEndTime)   || (_qsTimeRange && _qsTimeRange.endTime)   || '';
	var filterBanner = '';
	if (filterStart && filterEnd) {
		var fS = filterStart.substring(5, 16);   // "MM-DD HH:mm"
		var fE = filterEnd.substring(5, 16);
		filterBanner = '<div id="qs-tq-filter-banner" style="'
		    + 'background:#fff3cd;border:1px solid #ffc107;border-radius:4px;'
		    + 'padding:4px 10px;margin-bottom:4px;font-size:0.82em;display:flex;align-items:center;gap:8px;">'
		    + '<span>&#9200; <b>Time filter active:</b> '
		    + escHtml(fS) + ' &nbsp;&#8594;&nbsp; ' + escHtml(fE)
		    + ' &nbsp;<span style="color:#6c757d;font-size:0.9em;">(drag brush handles to change &bull; '
		    + '<a href="#" style="color:#856404;" onclick="_qsBrushReset();return false;">Reset to full range</a>)'
		    + '</span></span></div>';
	}

	// Chart container goes above the table — inject placeholder then populate after DOM insertion
	// The brush widget sits ABOVE the bar chart, lets the user drag two handles to filter the time range.
	var brushHtml = '<div id="qs-tq-brush-wrap" style="display:flex;flex-direction:column;min-height:0;overflow:hidden;"></div>';
	var chartHtml = '<div id="qs-tq-chart-container" style="min-height:0;overflow:hidden;"></div>';
	var tableHtml = '<div id="qs-tq-table-wrap" style="overflow:auto;min-height:0;padding:2px 0;">' + filterBanner + html + '</div>';
	$('#query-store-content').html(brushHtml + chartHtml + tableHtml);
	// Set up flex column layout for vertical Split.js
	$('#query-store-body').css('overflow', 'hidden');
	$('#query-store-content').css({ display: 'flex', flexDirection: 'column', height: '100%' });

	_qsRenderTimeBrush(r && r.timeline ? r.timeline : [], rankBy);
	_qsInitSparklines();
	_qsRenderChart(queries, rankBy);

	// Vertical Split.js: brush wrap | main chart | table
	var _qsVSizes = null;
	try { _qsVSizes = JSON.parse(localStorage.getItem(_scrPfx + 'qs-split-tq-v-sizes')); } catch(e) {}
	if (!Array.isArray(_qsVSizes) || _qsVSizes.length !== 3) _qsVSizes = [20, 37, 43];
	// RAF token for throttling chart resize calls during split drag
	var _vSplitRafId = null;
	function _qsVSplitOnDrag() {
		if (_qsBrushRefreshUi) _qsBrushRefreshUi();
		// Chart.js 2.x uses internal polling — it won't see the new container size
		// until its next poll cycle, causing a "snap". Force an immediate resize here,
		// throttled with rAF so we don't call it every pixel.
		if (!_vSplitRafId) {
			_vSplitRafId = requestAnimationFrame(function() {
				_vSplitRafId = null;
				if (_qsTqBrushChart) try { _qsTqBrushChart.resize(); } catch(e) {}
				if (_qsTqWaitChart)  try { _qsTqWaitChart.resize();  } catch(e) {}
				if (_qsTqChart)      try { _qsTqChart.resize();      } catch(e) {}
			});
		}
	}
	_qsTqVSplit = Split(['#qs-tq-brush-wrap', '#qs-tq-chart-container', '#qs-tq-table-wrap'], {
		sizes:       _qsVSizes,
		minSize:     [60, 80, 80],
		gutterSize:  5,
		direction:   'vertical',
		gutterAlign: 'center',
		snapOffset:  0,
		gutter: function(i, dir) { var g = document.createElement('div'); g.className = 'cm-gutter cm-gutter-' + dir; return g; },
		onDrag:    function()    { _qsVSplitOnDrag(); },
		onDragEnd: function(s)   { _qsVSplitOnDrag(); try { localStorage.setItem(_scrPfx + 'qs-split-tq-v-sizes', JSON.stringify(s)); } catch(e) {} }
	});
}

// ─────────────────────────────────────────────────────────────────────────────
// Time-range brush widget — Chart.js line plot of the rank metric per interval
// with two draggable "handles" overlaying the chart for selecting a sub-range.
// On drag-end, sets _qsTimeRange and reloads the top-queries result.
// ─────────────────────────────────────────────────────────────────────────────
function _qsRenderTimeBrush(timeline, rankBy)
{
	var wrap = document.getElementById('qs-tq-brush-wrap');
	if (!wrap) return;

	if (!timeline || timeline.length < 2) {
		wrap.innerHTML = '';
		return;
	}

	// Map rankBy → timeline metric key (timeline metrics are already in display units: ms, pages, MB)
	var brushKey, brushLabel, brushUnit;
	switch (rankBy) {
		case 'cpu':            brushKey = 'cpu';        brushLabel = 'CPU (ms)';            brushUnit = 'ms';    break;
		case 'reads':          brushKey = 'reads';      brushLabel = 'Logical Reads';       brushUnit = 'pages'; break;
		case 'physical_reads': brushKey = 'physReads';  brushLabel = 'Physical Reads';      brushUnit = 'pages'; break;
		case 'writes':         brushKey = 'writes';     brushLabel = 'Logical Writes';      brushUnit = 'pages'; break;
		case 'executions':     brushKey = 'executions'; brushLabel = 'Executions';          brushUnit = '';      break;
		case 'rowcount':       brushKey = 'rowcount';   brushLabel = 'Rows';                brushUnit = '';      break;
		case 'memory':         brushKey = 'memory';     brushLabel = 'Memory (MB)';         brushUnit = 'MB';    break;
		case 'tempdb':         brushKey = 'tempdb';     brushLabel = 'Tempdb (MB)';         brushUnit = 'MB';    break;
		case 'log':            brushKey = 'log';        brushLabel = 'Log (MB)';            brushUnit = 'MB';    break;
		case 'waittime':       brushKey = 'waitMs';     brushLabel = 'Wait Time (ms)';      brushUnit = 'ms';    break;
		case 'aborted':        brushKey = 'executions'; brushLabel = 'Executions';          brushUnit = '';      break;
		case 'plancount':      brushKey = 'executions'; brushLabel = 'Executions';          brushUnit = '';      break;
		case 'compile':        brushKey = 'duration';   brushLabel = 'Elapsed (ms)';        brushUnit = 'ms';    break;
		default:               brushKey = 'duration';   brushLabel = 'Elapsed (ms)';        brushUnit = 'ms';    break;
	}

	var labels = timeline.map(function(t) { return t.label || ''; });
	var values = timeline.map(function(t) { var v = t[brushKey]; return (v != null && !isNaN(v)) ? Number(v) : 0; });
	var startTimes = timeline.map(function(t) { return t.startTime; });
	var endTimes   = timeline.map(function(t) { return t.endTime;   });

	// Build the brush HTML (chart + 2 handles + reset button + secondary stacked-wait sparkline).
	// 'qs-brush-canvas-box' is the *exact* plot area for the main brush; both handles are positioned relative to it.
	var hasFilter = !!(_qsTimeRange.startTime && _qsTimeRange.endTime);
	var hasWaitData = timeline.some(function(t) { return t.waitByCategory; });
	wrap.innerHTML =
		  '<div id="qs-brush-charts-row" style="display:flex;gap:0;align-items:stretch;flex:1;min-height:0;">'
		// Main brush — left side, takes most of the width
		+ '<div id="qs-brush-canvas-box" style="position:relative;flex:1 1 auto;border:1px solid #e0e0e0;border-radius:3px;background:#fafafa;overflow:hidden;">'
		+   '<div style="position:absolute;top:2px;left:4px;right:36px;z-index:4;display:flex;align-items:center;gap:4px;font-size:0.72em;pointer-events:none;">'
		+     '<span style="font-weight:600;color:#444;text-shadow:1px 0 2px #fafafa,-1px 0 2px #fafafa,0 1px 2px #fafafa,0 -1px 2px #fafafa;">' + escHtml(brushLabel) + '</span>'
		+     '<span id="qs-brush-range-label" style="color:#1a6fc4;font-weight:600;text-shadow:1px 0 2px #fafafa,-1px 0 2px #fafafa,0 1px 2px #fafafa,0 -1px 2px #fafafa;"></span>'
		+   '</div>'
		+   '<div style="position:absolute;top:1px;right:2px;z-index:4;">'
		+     '<button class="btn btn-sm btn-outline-secondary" id="qs-brush-reset-btn" style="font-size:0.68em;padding:0 5px;line-height:1.4;'
		+       (hasFilter ? '' : 'visibility:hidden;') + '" onclick="_qsBrushReset()">Reset</button>'
		+   '</div>'
		+   '<canvas id="qs-tq-brush-chart" style="width:100%;height:100%;"></canvas>'
		+   '<div id="qs-brush-handle-left"  class="qs-brush-handle" data-side="left"'
		+   ' style="position:absolute;top:0;bottom:0;width:8px;background:rgba(54,116,188,0.85);cursor:ew-resize;border-radius:2px;z-index:3;"></div>'
		+   '<div id="qs-brush-handle-right" class="qs-brush-handle" data-side="right"'
		+   ' style="position:absolute;top:0;bottom:0;width:8px;background:rgba(54,116,188,0.85);cursor:ew-resize;border-radius:2px;z-index:3;"></div>'
		+   '<div id="qs-brush-mask-left"  style="position:absolute;top:0;bottom:0;left:0;background:rgba(160,160,160,0.25);pointer-events:none;z-index:2;"></div>'
		+   '<div id="qs-brush-mask-right" style="position:absolute;top:0;bottom:0;right:0;background:rgba(160,160,160,0.25);pointer-events:none;z-index:2;"></div>'
		+ '</div>'
		// Secondary stacked wait-time bar — right side, fixed width
		+ (hasWaitData
			? '<div id="qs-tq-wait-box" style="position:relative;border:1px solid #e0e0e0;border-radius:3px;background:#fafafa;overflow:hidden;">'
			+   '<div style="position:absolute;top:2px;left:4px;z-index:4;font-size:0.7em;font-weight:600;color:#444;pointer-events:none;white-space:nowrap;text-shadow:1px 0 2px #fafafa,-1px 0 2px #fafafa,0 1px 2px #fafafa,0 -1px 2px #fafafa;">Wait time by category</div>'
			+   '<canvas id="qs-tq-wait-chart" style="width:100%;height:100%;"></canvas>'
			+ '</div>'
			: '')
		+ '</div>';

	// Render Chart.js line \u2014 colours adapt to dark mode
	var dark = _qsIsDark();
	var brushLineColor = dark ? 'rgba(100,170,255,0.95)' : 'rgba(54,116,188,0.95)';
	var brushFillColor = dark ? 'rgba(100,170,255,0.15)' : 'rgba(54,116,188,0.18)';
	var ttBg    = dark ? 'rgba(30,30,30,0.95)'  : 'rgba(255,255,255,0.95)';
	var ttBdr   = dark ? '#555'                  : '#ccc';
	var ttTitle = dark ? '#e0e0e0'               : '#333';
	var ttBody  = dark ? '#cccccc'               : '#444';

	if (_qsTqBrushChart) { try { _qsTqBrushChart.destroy(); } catch(e){} _qsTqBrushChart = null; }
	var ctx = document.getElementById('qs-tq-brush-chart').getContext('2d');
	_qsTqBrushChart = new Chart(ctx, {
		type: 'line',
		data: {
			labels: labels,
			datasets: [{
				data: values,
				borderColor:     brushLineColor,
				backgroundColor: brushFillColor,
				borderWidth: 1.2,
				pointRadius: 0,
				fill: true,
				lineTension: 0.15
			}]
		},
		options: {
			responsive: true, maintainAspectRatio: false, animation: { duration: 0 },
			legend: { display: false }, title: { display: false },
			tooltips: {
				mode: 'index', intersect: false, displayColors: false,
				backgroundColor: ttBg, borderColor: ttBdr, borderWidth: 1,
				titleFontColor: ttTitle, bodyFontColor: ttBody,
				callbacks: {
					title: function(items) { return items[0] && labels[items[0].index] ? labels[items[0].index] : ''; },
					label: function(item) {
						var v = (item.yLabel != null && !isNaN(item.yLabel)) ? Number(item.yLabel) : null;
						if (v == null) return null;
						var n = Math.round(v);
						var s = _qsFmtNum(n) || String(n);
						if (brushUnit === 'ms')    return s + '\u00a0ms  (' + _qsFmtMsDur(v) + ')';
						if (brushUnit === 'pages') return s + '  (' + _qsPagesToMb(n) + ')';
						if (brushUnit === 'MB')    return s + '\u00a0MB';
						return s + (brushUnit ? '\u00a0' + brushUnit : '');
					}
				}
			},
			scales: {
				xAxes: [{ display: false, gridLines: { display: false } }],
				yAxes: [{ display: false, gridLines: { display: false }, ticks: { beginAtZero: true } }]
			},
			layout: { padding: 0 }
		}
	});

	// Render the secondary stacked-wait sparkline — sliced to the active brush selection
	// so it always reflects the same intervals shown in the top-queries table.
	if (hasWaitData) {
		var n = timeline.length;
		var wiL = 0, wiR = n - 1;
		if (_qsTimeRange && _qsTimeRange.startTime && _qsTimeRange.endTime) {
			for (var wi = 0; wi < n; wi++) {
				if (startTimes[wi] && startTimes[wi] >= _qsTimeRange.startTime) { wiL = wi; break; }
			}
			for (var wj = n - 1; wj >= 0; wj--) {
				if (endTimes[wj] && endTimes[wj] <= _qsTimeRange.endTime) { wiR = wj; break; }
			}
			if (wiR < wiL) wiR = wiL;
		}
		_qsRenderWaitStackedBar(wiL === 0 && wiR === n - 1 ? timeline : timeline.slice(wiL, wiR + 1));
	}

	// Set up draggable handles
	_qsWireBrushHandles(timeline, startTimes, endTimes);

	// Horizontal Split.js between brush line chart and wait stacked bar
	if (hasWaitData && document.getElementById('qs-tq-wait-box')) {
		if (_qsBrushHSplit) { try { _qsBrushHSplit.destroy(); } catch(e) {} _qsBrushHSplit = null; }
		var _qsHSizes = null;
		try { _qsHSizes = JSON.parse(localStorage.getItem(_scrPfx + 'qs-split-brush-h-sizes')); } catch(e) {}
		if (!Array.isArray(_qsHSizes) || _qsHSizes.length !== 2) _qsHSizes = [68, 32];
		_qsBrushHSplit = Split(['#qs-brush-canvas-box', '#qs-tq-wait-box'], {
			sizes:       _qsHSizes,
			minSize:     [120, 80],
			gutterSize:  5,
			direction:   'horizontal',
			gutterAlign: 'center',
			snapOffset:  0,
			gutter: function(i, dir) { var g = document.createElement('div'); g.className = 'cm-gutter cm-gutter-' + dir; return g; },
			onDragEnd: function(s) { try { localStorage.setItem(_scrPfx + 'qs-split-brush-h-sizes', JSON.stringify(s)); } catch(e) {} }
		});
	}

}

/**
 * Render a stacked-bar Chart.js sparkline of wait time per interval, broken down by wait category.
 * One bar per interval, segments coloured by category. Uses a fixed palette cycled by category index.
 */
// Custom tooltip positioner: tooltip appears at the top of the chart area, at the cursor X.
// Registered once; referenced by name in any chart's tooltips.position option.
if (typeof Chart !== 'undefined' && Chart.Tooltip && !Chart.Tooltip.positioners.top) {
	Chart.Tooltip.positioners.top = function(elements, eventPosition) {
		var chartArea = this._chart && this._chart.chartArea;
		return {
			x: eventPosition.x,
			y: chartArea ? chartArea.top : 0
		};
	};
}

var _qsTqWaitChart = null;
function _qsRenderWaitStackedBar(timeline)
{
	if (typeof Chart === 'undefined') return;
	var canvas = document.getElementById('qs-tq-wait-chart');
	if (!canvas) return;

	if (_qsTqWaitChart) { try { _qsTqWaitChart.destroy(); } catch(e){} _qsTqWaitChart = null; }

	// Discover the global category order from the first row that has any
	var categories = [];
	for (var i = 0; i < timeline.length; i++) {
		if (timeline[i].waitByCategory) { categories = Object.keys(timeline[i].waitByCategory); break; }
	}
	if (!categories.length) return;

	// SQL Server "wait_category_desc" → fixed colours (matches Microsoft's QS GUI palette where reasonable)
	var palette = {
		'CPU':              '#5b9bd5',
		'Worker Thread':    '#9bc2e6',
		'Lock':             '#c00000',
		'Latch':            '#ed7d31',
		'Buffer Latch':     '#f4b183',
		'Buffer IO':        '#ffc000',
		'Compilation':      '#a5a5a5',
		'SQL CLR':          '#7030a0',
		'Mirroring':        '#264478',
		'Transaction':      '#9e480e',
		'Idle':             '#bfbfbf',
		'Preemptive':       '#70ad47',
		'Service Broker':   '#a9d18e',
		'Tran Log IO':      '#c00000',
		'Network IO':       '#ffd966',
		'Parallelism':      '#5b9bd5',
		'Memory':           '#7030a0',
		'User Wait':        '#bf9000',
		'Tracing':          '#a5a5a5',
		'Full Text Search': '#cc99ff',
		'Other Disk IO':    '#ffc000',
		'Replication':      '#264478',
		'Log Rate Governor':'#990000',
		'Unknown':          '#7f7f7f'
	};
	var fallback = ['#4472c4','#ed7d31','#a5a5a5','#ffc000','#5b9bd5','#70ad47','#9e480e','#7030a0','#264478','#c00000'];

	var labels = timeline.map(function(t) { return t.label || ''; });
	var datasets = categories.map(function(cat, idx) {
		var color = palette[cat] || fallback[idx % fallback.length];
		return {
			label: cat,
			data:  timeline.map(function(t) {
				return (t.waitByCategory && t.waitByCategory[cat] != null) ? Number(t.waitByCategory[cat]) : 0;
			}),
			backgroundColor: color,
			borderColor:     color,
			borderWidth: 0,
			barPercentage: 1.0,
			categoryPercentage: 1.0
		};
	});

	var dark    = _qsIsDark();
	var ttBg    = dark ? 'rgba(30,30,30,0.95)'  : 'rgba(255,255,255,0.95)';
	var ttBdr   = dark ? '#555'                  : '#ccc';
	var ttTitle = dark ? '#e0e0e0'               : '#333';
	var ttBody  = dark ? '#cccccc'               : '#444';
	var ttFoot  = dark ? '#aaaaaa'               : '#666';

	var ctx = canvas.getContext('2d');
	_qsTqWaitChart = new Chart(ctx, {
		type: 'bar',
		data: { labels: labels, datasets: datasets },
		options: {
			responsive: true, maintainAspectRatio: false, animation: { duration: 0 },
			legend: { display: false },
			title:  { display: false },
			tooltips: {
				enabled: false,
				mode: 'index', intersect: false,
				custom: function(tooltipModel) {
					if (tooltipModel.opacity === 0) { _qsHideWaitPopup(); return; }
					if (!tooltipModel.dataPoints || !tooltipModel.dataPoints.length) return;
					var chart  = this._chart;
					var data   = chart.data;
					var idx    = tooltipModel.dataPoints[0].index;
					var rows   = [];
					data.datasets.forEach(function(ds) {
						var v = Number(ds.data[idx]) || 0;
						if (v > 0) rows.push({ label: ds.label, color: ds.backgroundColor, value: v });
					});
					rows.sort(function(a, b) { return b.value - a.value; });
					var total = rows.reduce(function(s, r) { return s + r.value; }, 0);
					// Synthesise an event position from the caret coords + canvas rect
					var rect = chart.canvas.getBoundingClientRect();
					var fakeEvent = { clientX: rect.left + tooltipModel.caretX, clientY: rect.top + tooltipModel.caretY };
					_qsShowWaitPopup(fakeEvent, data.labels[idx], rows, total);
				}
			},
			scales: {
				xAxes: [{ stacked: true, display: false, gridLines: { display: false } }],
				yAxes: [{ stacked: true, display: false, gridLines: { display: false }, ticks: { beginAtZero: true } }]
			},
			layout: { padding: 0 }
		}
	});
}

/** Show a floating wait-breakdown popup near the click event. */
function _qsShowWaitPopup(event, intervalLabel, rows, total)
{
	var dark = _qsIsDark();
	var pop  = document.getElementById('qs-wait-popup');
	if (!pop) {
		pop = document.createElement('div');
		pop.id = 'qs-wait-popup';
		pop.style.cssText = 'position:fixed;z-index:9999;min-width:320px;max-width:520px;'
			+ 'border-radius:5px;box-shadow:0 4px 16px rgba(0,0,0,0.35);padding:0;'
			+ 'font-size:0.8em;cursor:default;display:none;pointer-events:none;';
		document.body.appendChild(pop);
		// Escape hides it; mouse leaves the chart via the custom tooltip callback (opacity=0)
		document.addEventListener('keydown', function(e) {
			if ((e.key === 'Escape' || e.keyCode === 27))
				_qsHideWaitPopup();
		});
	}
	// Colours
	var bg  = dark ? '#1e1e1e' : '#fff';
	var bdr = dark ? '#555'    : '#ccc';
	var hdr = dark ? '#2d2d2d' : '#f0f0f0';
	var clr = dark ? '#e0e0e0' : '#222';
	var sub = dark ? '#aaa'    : '#666';

	var rowsHtml = rows.map(function(r) {
		var pct = total > 0 ? (r.value / total * 100).toFixed(1) : '0.0';
		var ms  = Math.round(r.value);
		return '<tr>'
			+ '<td style="padding:2px 5px 2px 8px;white-space:nowrap;">'
			+   '<span style="display:inline-block;width:10px;height:10px;border-radius:2px;background:' + r.color + ';vertical-align:middle;margin-right:5px;"></span>'
			+   '<span style="color:' + clr + ';">' + escHtml(r.label) + '</span>'
			+ '</td>'
			+ '<td style="padding:2px 6px 2px 5px;text-align:right;white-space:nowrap;color:' + clr + ';">'
			+   (_qsFmtNum(ms) || String(ms)) + '&nbsp;ms'
			+ '</td>'
			+ '<td style="padding:2px 6px 2px 2px;text-align:right;white-space:nowrap;color:' + sub + ';font-size:0.88em;">'
			+   _qsMsToHms(r.value)
			+ '</td>'
			+ '<td style="padding:2px 8px 2px 2px;text-align:right;white-space:nowrap;color:' + sub + ';">'
			+   pct + '%'
			+ '</td>'
			+ '</tr>';
	}).join('');

	var totalMs = Math.round(total);
	pop.style.background   = bg;
	pop.style.border       = '1px solid ' + bdr;
	pop.style.color        = clr;
	pop.innerHTML =
		  '<div style="padding:5px 10px;background:' + hdr + ';border-bottom:1px solid ' + bdr + ';border-radius:5px 5px 0 0;">'
		+   '<span style="font-weight:600;color:' + clr + ';">' + escHtml(intervalLabel) + '</span>'
		+ '</div>'
		+ '<div>'
		+   '<table style="width:100%;border-collapse:collapse;">' + rowsHtml + '</table>'
		+ '</div>'
		+ '<div style="padding:4px 8px;border-top:1px solid ' + bdr + ';text-align:right;color:' + sub + ';">'
		+   'Total: <span style="color:' + clr + ';font-weight:600;">' + (_qsFmtNum(totalMs) || String(totalMs)) + '&nbsp;ms</span>'
		+   '&nbsp;&nbsp;<span style="font-size:0.9em;">' + _qsMsToHms(total) + '</span>'
		+ '</div>';

	// Position near the click, keeping within viewport
	var vw = window.innerWidth, vh = window.innerHeight;
	var ex = event.clientX, ey = event.clientY;
	pop.style.display = 'block';
	var pw = pop.offsetWidth, ph = pop.offsetHeight;
	var left = ex + 12;
	var top  = ey - ph / 2;
	if (left + pw > vw - 8) left = ex - pw - 8;
	if (top < 8)            top  = 8;
	if (top + ph > vh - 8)  top  = vh - ph - 8;
	pop.style.left = left + 'px';
	pop.style.top  = top  + 'px';
}

function _qsHideWaitPopup()
{
	var pop = document.getElementById('qs-wait-popup');
	if (pop) pop.style.display = 'none';
}

// Reset the brush selection to "all"
// Destroy Split.js instances and restore body layout.
function _qsDestroyTqSplits()
{
	if (_qsBrushHSplit)  { try { _qsBrushHSplit.destroy(); }  catch(e) {} _qsBrushHSplit  = null; }
	if (_qsTqVSplit)     { try { _qsTqVSplit.destroy();    }  catch(e) {} _qsTqVSplit     = null; }
	$('#query-store-body').css('overflow', 'auto');
	$('#query-store-content').css({ display: '', flexDirection: '', height: '' });
}

function _qsBrushReset()
{
	_qsTimeRange = { startTime: null, endTime: null };
	queryStoreTopQueriesLoad();
}

/**
 * Wire mouse drag on the two brush handles.
 * Stores selection in _qsTimeRange (yyyy-MM-dd HH:mm:ss) and triggers a reload on drag-end.
 */
function _qsWireBrushHandles(timeline, startTimes, endTimes)
{
	var box = document.getElementById('qs-brush-canvas-box');
	var hL  = document.getElementById('qs-brush-handle-left');
	var hR  = document.getElementById('qs-brush-handle-right');
	var mL  = document.getElementById('qs-brush-mask-left');
	var mR  = document.getElementById('qs-brush-mask-right');
	var lbl = document.getElementById('qs-brush-range-label');
	if (!box || !hL || !hR) return;

	var n  = timeline.length;
	var iL = 0;        // selected left interval index
	var iR = n - 1;    // selected right interval index

	// If a previous selection is in effect, snap handles to it
	if (_qsTimeRange && _qsTimeRange.startTime && _qsTimeRange.endTime) {
		for (var i = 0; i < n; i++) {
			if (startTimes[i] && startTimes[i] >= _qsTimeRange.startTime) { iL = i; break; }
		}
		for (var j = n - 1; j >= 0; j--) {
			if (endTimes[j] && endTimes[j] <= _qsTimeRange.endTime) { iR = j; break; }
		}
		if (iR < iL) iR = iL;
	}

	function pxFor(i) {
		var w = box.clientWidth;
		if (n <= 1) return 0;
		return Math.round(i / (n - 1) * (w - 8));   // 8 = handle width
	}
	function indexFor(px) {
		var w = box.clientWidth - 8;
		if (w <= 0) return 0;
		var i = Math.round(px / w * (n - 1));
		return Math.max(0, Math.min(n - 1, i));
	}
	function refreshUi() {
		var xL = pxFor(iL);
		var xR = pxFor(iR) + 8;   // right edge of right handle
		hL.style.left  = xL + 'px';
		hR.style.left  = (pxFor(iR)) + 'px';
		mL.style.width = xL + 'px';
		mR.style.width = (box.clientWidth - xR) + 'px';
		var s = (startTimes[iL] || '').substring(5, 16);   // "MM-DD HH:mm"
		var e = (endTimes[iR]   || '').substring(5, 16);
		if (lbl) lbl.textContent = s + '  →  ' + e + '   (' + (iR - iL + 1) + ' intervals)';
	}
	refreshUi();
	// Re-position after Split.js applies its sizes (clientWidth is 0 on first paint)
	_qsBrushRefreshUi = refreshUi;
	setTimeout(refreshUi, 80);

	// Use ResizeObserver on the canvas box so handles track the box width exactly,
	// including panel splits. Debounce with rAF to avoid jitter on rapid resizes.
	// Tear down any previous observer first so listeners never accumulate.
	if (_qsBrushResizeObs) { try { _qsBrushResizeObs.disconnect(); } catch(e) {} _qsBrushResizeObs = null; }
	if (typeof ResizeObserver !== 'undefined') {
		var _rafId = null;
		_qsBrushResizeObs = new ResizeObserver(function() {
			if (_rafId) return;                         // coalesce rapid fires
			_rafId = requestAnimationFrame(function() {
				_rafId = null;
				refreshUi();
			});
		});
		_qsBrushResizeObs.observe(box);
	} else {
		// Fallback for old browsers — window resize is good enough
		window.addEventListener('resize', refreshUi);
	}

	function startDrag(side, downEvent) {
		downEvent.preventDefault();
		var moved = false;
		function onMove(ev) {
			moved = true;
			var rect = box.getBoundingClientRect();
			var x = (ev.touches ? ev.touches[0].clientX : ev.clientX) - rect.left;
			var i = indexFor(x);
			if (side === 'left') {
				iL = Math.min(i, iR);
			} else {
				iR = Math.max(i, iL);
			}
			refreshUi();
		}
		function onUp() {
			document.removeEventListener('mousemove', onMove);
			document.removeEventListener('mouseup',   onUp);
			document.removeEventListener('touchmove', onMove);
			document.removeEventListener('touchend',  onUp);
			if (moved) {
				// Apply the selection — set _qsTimeRange and reload
				if (iL === 0 && iR === n - 1) {
					_qsTimeRange = { startTime: null, endTime: null };
				} else {
					_qsTimeRange = { startTime: startTimes[iL], endTime: endTimes[iR] };
				}
				console.log('[QS] brush drag-end → iL=' + iL + ' iR=' + iR + '/' + (n-1)
				    + '  startTime=' + _qsTimeRange.startTime
				    + '  endTime='   + _qsTimeRange.endTime);
				// Immediately slice the wait chart to the selected intervals so it
				// updates in sync with the top-queries reload.
				_qsHideWaitPopup();
				_qsRenderWaitStackedBar(timeline.slice(iL, iR + 1));
				queryStoreTopQueriesLoad();
			}
		}
		document.addEventListener('mousemove', onMove);
		document.addEventListener('mouseup',   onUp);
		document.addEventListener('touchmove', onMove);
		document.addEventListener('touchend',  onUp);
	}

	hL.addEventListener('mousedown',  function(e) { startDrag('left',  e); });
	hR.addEventListener('mousedown',  function(e) { startDrag('right', e); });
	hL.addEventListener('touchstart', function(e) { startDrag('left',  e); });
	hR.addEventListener('touchstart', function(e) { startDrag('right', e); });
}

/**
 * Render jquery sparklines inside #qs-tq-table after the table has been inserted
 * into the DOM. Each .qs-sparkline span carries:
 *   data-vals  — comma-separated numeric values (oldest→newest)
 *   data-dates — pipe-separated "MM/dd HH:mm" labels (parallel to vals)
 *   data-unit  — optional unit suffix shown in tooltip (ms, MB, or blank)
 */
function _qsInitSparklines()
{
	if (typeof $.fn.sparkline === 'undefined') return;
	_qsRenderSparklineSet('.qs-sparkline');
}

/**
 * Render all .qs-sparkline elements inside a given CSS selector context.
 * Supports optional data-color and data-width attributes per element.
 */
function _qsRenderSparklineSet(selector)
{
	if (typeof $.fn.sparkline === 'undefined') return;
	$(selector).each(function() {
		var $el   = $(this);
		var csv   = $el.attr('data-vals');
		if (!csv) return;
		// Replace NaN/null with 0 so the full time axis is always drawn — gaps in a
		// sparkline compress the x-axis and make it impossible to see WHEN things happened.
		var vals  = csv.split(',').map(function(s) { var n = Number(s); return isNaN(n) ? 0 : n; });
		if (!vals.length) return;
		var dates    = ($el.attr('data-dates') || '').split('|');
		var unit     = $el.attr('data-unit')      || '';
		var color    = $el.attr('data-color')     || '#4472C4';
		var width    = $el.attr('data-width')     || '120px';
		var rangeMax = parseFloat($el.attr('data-range-max'));
		var fill     = color === '#5b9a3c' ? 'rgba(91,154,60,0.12)' : 'rgba(68,114,196,0.12)';
		$el.sparkline(vals, {
			type:          'line',
			width:         width,
			height:        '18px',
			lineColor:     color,
			fillColor:     fill,
			lineWidth:     1.5,
			spotColor:     false,
			minSpotColor:  false,
			maxSpotColor:  '#d9534f',
			chartRangeMin: 0,
			chartRangeMax: isNaN(rangeMax) ? undefined : rangeMax,
			nullValue:     0,   // render null/gap as zero \u2014 never compress the x-axis
			tooltipFormatter: function(sp, options, fields) {
				var field = (fields && typeof fields === 'object' && !Array.isArray(fields))
				              ? fields : (fields && fields[0]);
				if (!field) return '';
				var idx  = field.offset || 0;
				var val  = (field.y !== undefined) ? field.y : (field.value || 0);
				var date = dates[idx] || '';
				var disp;
				if (unit === 'ms') {
					// Show raw ms rounded to integer + human duration
					var rawMs = _qsFmtNum(Math.round(val)) || String(Math.round(val));
					disp = rawMs + '\u00a0ms  (' + _qsFmtMsDur(val) + ')';
				} else if (unit === 'pages') {
					// Keep 1 decimal for averaged page counts; show MB conversion
					var frac = (val % 1 !== 0) ? parseFloat(val.toFixed(1)) : Math.round(val);
					var rawPg = _qsFmtNum(frac) || String(frac);
					var mb = (val / 128);
					var mbStr = mb >= 1 ? (_qsFmt1Dec(mb) || mb.toFixed(1)) + '\u00a0MB'
					                    : Math.round(val * 8) + '\u00a0KB';
					disp = rawPg + '\u00a0pages  (' + mbStr + ')';
				} else {
					var rawFmt = _qsFmtNum(Math.round(val)) || String(Math.round(val));
					disp = rawFmt + (unit ? '\u00a0' + unit : '');
				}
				return '<div style="min-width:140px;padding:3px 4px 14px 4px;">'
				     + (date ? '<div style="color:#ffd700;font-weight:600;margin-bottom:7px;">' + date + '</div>' : '')
				     + '<div style="font-size:1.1em;font-weight:700;">' + disp + '</div>'
				     + '</div>';
			}
		});
		$el.addClass('qs-sparkline-done');
	});
}

/**
 * Render a Chart.js horizontal bar chart of top queries by the selected rank metric.
 * Destroys and recreates the chart on each call to handle rank-by changes.
 */
function _qsRenderChart(queries, rankBy)
{
	if (typeof Chart === 'undefined') return;
	var container = document.getElementById('qs-tq-chart-container');
	if (!container) return;

	// Destroy previous chart instance to avoid "canvas already in use" error
	if (_qsTqChart) { try { _qsTqChart.destroy(); } catch(e) {} _qsTqChart = null; }

	// Pick the metric to chart
	var metricKey, metricLabel;
	switch (rankBy) {
		case 'cpu':            metricKey = 'totalCpuUs';            metricLabel = 'Total CPU (ms)';            break;
		case 'reads':          metricKey = 'totalLogicalReads';     metricLabel = 'Total Logical Reads';       break;
		case 'physical_reads': metricKey = 'totalPhysicalReads';    metricLabel = 'Total Phys Reads';          break;
		case 'writes':         metricKey = 'totalLogicalWrites';    metricLabel = 'Total Logical Writes';      break;
		case 'executions':     metricKey = 'totalExecutions';       metricLabel = 'Total Executions';          break;
		case 'rowcount':       metricKey = 'totalRowcount';         metricLabel = 'Total Rows';                break;
		case 'memory':         metricKey = 'totalMemoryMb';         metricLabel = 'Total Memory (MB)';         break;
		case 'tempdb':         metricKey = 'totalTempdbMb';         metricLabel = 'Total Tempdb (MB)';         break;
		case 'log':            metricKey = 'totalLogMb';            metricLabel = 'Total Log (MB)';            break;
		case 'compile':        metricKey = 'avgCompileDurationUs';  metricLabel = 'Avg Compile (ms)';          break;
		case 'waittime':       metricKey = 'totalWaitMs';           metricLabel = 'Total Wait Time (ms)';      break;
		case 'plancount':      metricKey = 'planCount';             metricLabel = 'Plan Count';                break;
		case 'aborted':        metricKey = 'totalAbortedException'; metricLabel = 'Aborted + Exceptions';      break;
		default:               metricKey = 'totalDurationUs';       metricLabel = 'Total Elapsed (ms)';        break;
	}

	// Limit to top 20 bars for readability
	var chartData = queries.slice(0, 20);
	var barLabels = chartData.map(function(q) {
		var sql = (q.querySqlTextPreview || '').replace(/\s+/g, ' ').substring(0, 38);
		return 'Q' + q.queryId + (sql ? ': ' + sql : '');
	});
	// *Us keys are stored in microseconds — convert to ms for display
	var isUsMetric = (metricKey === 'totalDurationUs' || metricKey === 'totalCpuUs' || metricKey === 'avgCompileDurationUs');
	var barValues = chartData.map(function(q) {
		var v = q[metricKey];
		var n = (v != null && !isNaN(v)) ? Number(v) : 0;
		return isUsMetric ? n / 1000 : n;
	});

	container.innerHTML = '<canvas id="qs-tq-chart"></canvas>';
	var ctx = document.getElementById('qs-tq-chart').getContext('2d');

	_qsTqChart = new Chart(ctx, {
		type: 'horizontalBar',
		data: {
			labels: barLabels,
			datasets: [{
				label: metricLabel,
				data:  barValues,
				backgroundColor: 'rgba(54,116,188,0.65)',
				borderColor:     'rgba(54,116,188,0.9)',
				borderWidth: 1
			}]
		},
		options: {
			responsive: true,
			maintainAspectRatio: false,
			legend: { display: false },
			title: {
				display: true,
				text: metricLabel + '  —  Top ' + chartData.length + ' queries (click bar to view detail)',
				fontSize: 12,
				fontColor: '#555'
			},
			scales: {
				xAxes: [{ ticks: { beginAtZero: true, fontSize: 10,
					callback: function(v) {
						if (metricLabel.indexOf('(ms)') !== -1)
							return (_qsFmtNum(v) || _qsFmt1Dec(v) || String(v)) + '\u00a0ms  (' + _qsFmtMsDur(v) + ')';
						if (metricKey === 'totalLogicalReads' || metricKey === 'totalPhysicalReads' || metricKey === 'totalLogicalWrites') {
							var mb = Math.round(v / 128);
							return (_qsFmtNum(v) || _qsFmt1Dec(v) || String(v)) + '  (' + (_qsFmtNum(mb) || String(mb)) + '\u00a0MB)';
						}
						return _qsFmtNum(v) || _qsFmt1Dec(v) || String(v);
					}
				} }],
				yAxes: [{ ticks: { fontSize: 10 } }]
			},
			onClick: function(evt, elements) {
				if (elements && elements.length > 0) {
					var idx = elements[0]._index;
					var q = chartData[idx];
					if (q) queryStoreSelectQuery(String(q.queryId));
				}
			},
			tooltips: {
				callbacks: {
					label: function(item) {
						var v = item.xLabel;
						var fmt;
						if (metricLabel.indexOf('(ms)') !== -1) {
							fmt = (_qsFmtNum(v) || _qsFmt1Dec(v) || String(v)) + '\u00a0ms  (' + _qsFmtMsDur(v) + ')';
						} else {
							fmt = _qsFmtNum(v) || _qsFmt1Dec(v) || String(v);
							// append MB for page-count metrics (8 KB pages → / 128)
							if (metricKey === 'totalLogicalReads' || metricKey === 'totalPhysicalReads' || metricKey === 'totalLogicalWrites') {
								var mb = Math.round(v / 128);
								fmt += '  (' + (_qsFmtNum(mb) || String(mb)) + '\u00a0MB)';
							}
						}
						return ' ' + metricLabel + ': ' + fmt;
					}
				}
			}
		}
	});
}

/** Filter input changed — re-render without re-fetching */
function qsTqFilterChanged(val)
{
	_qsTqFilter = (val || '').trim();
	if (_qsTqLastResult) queryStoreRenderTopQueries(_qsTqLastResult);
}

/** Ctrl+Space column completion for the top-queries filter input */
function qsFilterKeydown(e)
{
	if (typeof dbxFilterKeydown === 'function') {
		dbxFilterKeydown(e, function() {
			return ['queryId','objectContext','planCount','totalExecutions',
			        'avgDurationUs','maxDurationUs','totalDurationUs',
			        'avgCpuUs','totalCpuUs',
			        'avgLogicalReads','maxLogicalReads','totalLogicalReads',
			        'avgPhysicalReads','totalPhysicalReads',
			        'avgLogicalWrites','totalLogicalWrites',
			        'avgRowcount','totalRowcount',
			        'avgMemoryMb','totalMemoryMb',
			        'avgDop','avgTempdbMb','totalTempdbMb','avgLogMb','totalLogMb',
			        'avgCompileDurationUs','countCompiles',
			        'firstSeen','lastSeen','querySqlTextPreview'];
		});
	}
}

/** User clicked a query row → go to Query Detail tab */
function queryStoreSelectQuery(queryId)
{
	_qsQueryId = queryId;
	$('#query-store-detail-qid').text(queryId);
	_qsMainTab = 'detail';
	_qsActivateTab('detail');
	$('#query-store-tq-bar').hide();
	$('#query-store-detail-bar').css('display', 'flex');
	queryStoreDetailLoad();
}

// ── Query Detail tab ──────────────────────────────────────────────────────────

function queryStoreDetailLoad()
{
	var srv     = _qsSrvName;
	var dbname  = _qsDbname;
	var queryId = _qsQueryId;
	if (!srv || !dbname || !queryId) return;

	var includePlan = $('#query-store-include-plan').is(':checked') ? 'true' : 'false';

	_qsDestroyTqSplits();
	$('#query-store-loading').show();
	$('#query-store-content').html('');

	// If a brush time range is active, it may span multiple day-databases.
	// Fetch from every overlapping day and merge the results.
	if (_qsTimeRange && _qsTimeRange.startTime) {
		var dates = _qsDetailDatesForRange(_qsTimeRange.startTime, _qsTimeRange.endTime);
		_qsDetailMultiDayFetch(srv, dbname, queryId, includePlan, dates,
		                       _qsTimeRange.startTime, _qsTimeRange.endTime);
		return;
	}

	// Multi-db N-day aggregate mode: the user clicked an "N days" row (e.g. "7 days").
	// Fetch detail from every day-database that was originally queried and merge the results.
	if (_qsMultiDbMode && _qsMultiDbCtx && _qsMultiDbCtx.dates && _qsMultiDbCtx.dates.length > 1
	        && _qsDetailDay && /^\d+ days?$/.test(_qsDetailDay)) {
		var mdDates = _qsMultiDbCtx.dates.slice().reverse();   // oldest first → chronological timeline
		_qsDetailMultiDayFetch(srv, dbname, queryId, includePlan, mdDates, null, null);
		return;
	}

	// No time filter — single-day fetch (current timestamp or live)
	var params = { srv: srv, action: 'queryDetail', dbname: dbname, queryId: queryId, includePlan: includePlan };
	var ts = _qsTimestamp || '';
	if (ts) params.ts = ts;

	$.ajax({
		url:      '/api/cc/mgt/query-store',
		data:     params,
		dataType: 'text',
		success: function(data) {
			$('#query-store-loading').hide();
			try {
				var r = JSON.parse(data);
				if (r.error) { queryStoreShowMsg(r.message || r.error); return; }
				_qsAlignPlanSparklines(r.plans);
				queryStoreRenderDetail(r);
			} catch(ex) { queryStoreShowMsg('Parse error: ' + ex); }
		},
		error: function(xhr) {
			$('#query-store-loading').hide();
			queryStoreShowHttpError(xhr, 'queryDetail');
		}
	});
}

/**
 * Returns the subset of available day-database keys that overlap with the
 * given time range.  null = today's live database, 'YYYY-MM-DD' = historical.
 * Result is ordered oldest-first so the timeline ends up in chronological order.
 */
function _qsDetailDatesForRange(startTime, endTime)
{
	var startDate = startTime.substring(0, 10);   // 'YYYY-MM-DD'
	var endDate   = endTime.substring(0, 10);
	var today     = new Date().toISOString().substring(0, 10);

	// allDates: null (today) first, then historical newest→oldest
	var allDates  = [null].concat(_qsAvailDates || []);
	var matching  = allDates.filter(function(d) {
		var dateStr = d || today;
		return dateStr >= startDate && dateStr <= endDate;
	});
	// Reverse so we fetch oldest first → timeline is chronological after concat
	matching.reverse();
	return matching;
}

/**
 * Fetch queryDetail from each day-database in `dates` (serially), then merge
 * and render.  Each day is filtered to [startTime, endTime] on the Java side.
 */
function _qsDetailMultiDayFetch(srv, dbname, queryId, includePlan, dates, startTime, endTime)
{
	var accumulated = [];
	var idx = 0;

	function fetchNext() {
		if (idx >= dates.length) {
			$('#query-store-loading').hide();
			var merged = _qsDetailMergeResults(accumulated);
			if (!merged) { queryStoreShowMsg('No data found for this query in the selected time range.'); return; }
			_qsAlignPlanSparklines(merged.plans);
			queryStoreRenderDetail(merged);
			return;
		}

		var dateKey = dates[idx++];
		var ts      = dateKey ? (dateKey + ' 23:59:59') : '';
		var params  = { srv: srv, action: 'queryDetail', dbname: dbname,
		                queryId: queryId, includePlan: includePlan };
		if (startTime) params.startTime = startTime;
		if (endTime)   params.endTime   = endTime;
		if (ts)        params.ts        = ts;

		$.ajax({
			url:      '/api/cc/mgt/query-store',
			data:     params,
			dataType: 'text',
			success: function(data) {
				try {
					var r = JSON.parse(data);
					if (!r.error && r.plans && r.plans.length > 0) accumulated.push(r);
				} catch(e) {}
				fetchNext();
			},
			error: function() { fetchNext(); }
		});
	}

	fetchNext();
}

/**
 * Align all plan sparklines to a shared time axis so that plans active in
 * different parts of the period can be visually compared.
 *
 * Java already zero-fills a shared axis within a single-day response; this
 * function extends that to the multi-day (merged) case where a plan that only
 * appeared in some days would otherwise have a shorter sparkDates array than a
 * plan that spanned all days.
 *
 * After this call every plan has the same sparkDates array and the same-length
 * spark value arrays — missing slots are filled with 0.
 */
function _qsAlignPlanSparklines(plans)
{
	if (!plans || plans.length < 2) return;

	// Build the union of all sparkDates in order of first appearance.
	// Plans are stored oldest-first, so the natural insertion order is chronological.
	var seen = {}, unionDates = [];
	plans.forEach(function(p) {
		if (!p.sparkDates) return;
		p.sparkDates.forEach(function(d) {
			if (!seen[d]) { seen[d] = true; unionDates.push(d); }
		});
	});
	if (unionDates.length === 0) return;

	plans.forEach(function(p) {
		if (!p.sparkDates || !p.spark) return;
		if (p.sparkDates.length === unionDates.length) return;   // already aligned

		// Build lookup: date label → index in this plan's current arrays
		var idx = {};
		p.sparkDates.forEach(function(d, i) { idx[d] = i; });

		// Remap every metric array onto the union axis, inserting 0 for missing slots
		Object.keys(p.spark).forEach(function(k) {
			p.spark[k] = unionDates.map(function(d) {
				var i = idx[d];
				return i !== undefined ? (Number(p.spark[k][i]) || 0) : 0;
			});
		});
		p.sparkDates = unionDates;
	});
}

/**
 * Merge queryDetail responses from multiple day-databases into a single result
 * suitable for queryStoreRenderDetail().
 *
 * - Plans:     matched by plan_id; execution counts summed, averages weighted,
 *              maxima maximized, first/last-seen expanded.
 * - Wait stats: matched by waitCategory; totals summed, max maximized.
 * - Timeline:   concatenated and sorted chronologically.
 * - Top-level summary: recomputed from the merged plans.
 */
function _qsDetailMergeResults(results)
{
	if (!results || !results.length) return null;
	if (results.length === 1) return results[0];

	// Deep-clone the first result as the base (carries schemaFlags, queryText etc.)
	var base = JSON.parse(JSON.stringify(results[0]));

	// ── Plans ─────────────────────────────────────────────────────────────────
	var planMap = {};
	results.forEach(function(r) {
		if (!r.plans) return;
		r.plans.forEach(function(p) {
			var pid = String(p.planId);
			if (!planMap[pid]) { planMap[pid] = JSON.parse(JSON.stringify(p)); return; }
			var ex = planMap[pid];
			var eExec = Number(ex.totalExecutions) || 0;
			var nExec = Number(p.totalExecutions)  || 0;
			var tot   = eExec + nExec;
			if (tot > 0) {
				// Weighted averages
				// Page-count avg columns — round to integer after weighted average
				// (SQL Server stores reads/writes as integer pages; fractional averages are misleading)
				var intAvgCols = { avgLogicalReads:1, avgPhysicalReads:1, avgLogicalWrites:1 };
				['avgDurationUs','avgCpuUs','avgLogicalReads','avgPhysicalReads',
				 'avgLogicalWrites','avgRowcount','avgDop','avgMemoryMb','avgTempdbMb','avgLogMb'
				].forEach(function(k) {
					if (p[k] != null && ex[k] != null) {
						var avg = (ex[k] * eExec + p[k] * nExec) / tot;
						ex[k] = intAvgCols[k] ? Math.round(avg) : avg;
					} else if (p[k] != null) {
						ex[k] = p[k];
					}
				});
				// Maxima
				['maxDurationUs','maxCpuUs','maxLogicalReads','maxPhysicalReads','maxLogicalWrites'
				].forEach(function(k) {
					if (p[k] != null) ex[k] = Math.max(Number(ex[k]) || 0, Number(p[k]));
				});
				// Totals
				['totalDurationUs','totalCpuUs','totalLogicalReads','totalPhysicalReads',
				 'totalLogicalWrites','totalRowcount','totalMemoryMb'
				].forEach(function(k) {
					if (p[k] != null) ex[k] = (Number(ex[k]) || 0) + Number(p[k]);
				});
				ex.totalExecutions = tot;
				// Expand firstSeen / lastSeen
				if (p.firstSeen && (!ex.firstSeen || p.firstSeen < ex.firstSeen)) ex.firstSeen = p.firstSeen;
				if (p.lastSeen  && (!ex.lastSeen  || p.lastSeen  > ex.lastSeen))  ex.lastSeen  = p.lastSeen;
				// Concatenate sparkline data chronologically (fetch is oldest-first)
				if (p.sparkDates && p.sparkDates.length > 0) {
					ex.sparkDates = (ex.sparkDates || []).concat(p.sparkDates);
					if (p.spark) {
						if (!ex.spark) ex.spark = {};
						Object.keys(p.spark).forEach(function(k) {
							ex.spark[k] = (ex.spark[k] || []).concat(p.spark[k] || []);
						});
					}
				}
			}
		});
	});
	// Convert map back to array, sorted by avg duration desc
	base.plans = [];
	for (var pid in planMap) { if (planMap.hasOwnProperty(pid)) base.plans.push(planMap[pid]); }
	base.plans.sort(function(a, b) { return (Number(b.avgDurationUs) || 0) - (Number(a.avgDurationUs) || 0); });

	// ── Wait stats ────────────────────────────────────────────────────────────
	var waitMap = {};
	results.forEach(function(r) {
		if (!r.waitStats) return;
		r.waitStats.forEach(function(w) {
			var cat = w.waitCategory;
			if (!waitMap[cat]) { waitMap[cat] = JSON.parse(JSON.stringify(w)); return; }
			var ex = waitMap[cat];
			ex.totalWaitMs = (Number(ex.totalWaitMs) || 0) + (Number(w.totalWaitMs) || 0);
			ex.maxWaitMs   = Math.max(Number(ex.maxWaitMs) || 0, Number(w.maxWaitMs) || 0);
			ex.avgWaitMs   = ex.totalWaitMs / results.length;   // approximate
			// Concatenate sparkline data chronologically (fetch is oldest-first)
			if (w.sparkDates && w.sparkDates.length > 0) {
				ex.sparkDates = (ex.sparkDates || []).concat(w.sparkDates);
				ex.spark      = (ex.spark      || []).concat(w.spark || []);
			}
		});
	});
	base.waitStats = [];
	for (var cat in waitMap) { if (waitMap.hasOwnProperty(cat)) base.waitStats.push(waitMap[cat]); }
	base.waitStats.sort(function(a, b) { return (Number(b.totalWaitMs) || 0) - (Number(a.totalWaitMs) || 0); });

	// ── Timeline ──────────────────────────────────────────────────────────────
	var allTl = [];
	results.forEach(function(r) { if (r.timeline) allTl = allTl.concat(r.timeline); });
	allTl.sort(function(a, b) { return (a.intervalStart || '').localeCompare(b.intervalStart || ''); });
	base.timeline = allTl;

	// ── Recompute top-level summary from merged plans ────────────────────────
	var totalExec = 0, sumDurW = 0, sumCpuW = 0, sumReadsW = 0, sumRowsW = 0, maxDurUs = 0;
	var firstSeen = null, lastSeen = null;
	base.plans.forEach(function(p) {
		var exec = Number(p.totalExecutions) || 0;
		totalExec += exec;
		sumDurW   += exec * (Number(p.avgDurationUs)   || 0);
		sumCpuW   += exec * (Number(p.avgCpuUs)        || 0);
		sumReadsW += exec * (Number(p.avgLogicalReads)  || 0);
		sumRowsW  += exec * (Number(p.avgRowcount)      || 0);
		maxDurUs  = Math.max(maxDurUs, Number(p.maxDurationUs) || 0);
		if (p.firstSeen && (!firstSeen || p.firstSeen < firstSeen)) firstSeen = p.firstSeen;
		if (p.lastSeen  && (!lastSeen  || p.lastSeen  > lastSeen))  lastSeen  = p.lastSeen;
	});
	base.planCount       = base.plans.length;
	base.totalExecutions = totalExec;
	if (totalExec > 0) {
		base.avgDurationUs   = Math.round(sumDurW   / totalExec);
		base.avgCpuUs        = Math.round(sumCpuW   / totalExec);
		base.avgLogicalReads = Math.round(sumReadsW / totalExec);
		base.avgRowcount     = Math.round(sumRowsW  / totalExec);
	}
	base.maxDurationUs = maxDurUs;
	base.firstSeen     = firstSeen;
	base.lastSeen      = lastSeen;

	return base;
}

function queryStoreRenderDetail(r)
{
	var html = '';

	// ── Summary ──────────────────────────────────────────────────────────────
	html += '<div style="padding:4px 0 6px 0;border-bottom:1px solid #dee2e6;margin-bottom:8px;">';
	html += '<div style="font-size:0.82em;display:flex;flex-wrap:wrap;gap:4px 18px;">';
	var _fmtSummary = function(label, val) {
		if (val == null) return;
		var disp = _qsFmtNum(val);
		html += '<span><b>' + escHtml(label) + ':</b> ' + escHtml(String(disp != null ? disp : val)) + '</span>';
	};
	var _fmtSummaryUs = function(label, us) {
		if (us == null) return;
		var r = _qsFmtUsAsMs(us);
		html += '<span' + (r.tip ? ' title="' + escHtml(r.tip) + '"' : '') + '><b>' + escHtml(label) + ':</b> ' + escHtml(r.disp) + '</span>';
	};
	var _fmtSummaryReads = function(label, pages) {
		if (pages == null) return;
		var disp = _qsFmtNum(pages) || pages;
		var tip  = _qsPagesToMb(pages);
		html += '<span' + (tip ? ' title="' + escHtml(tip) + '"' : '') + '><b>' + escHtml(label) + ':</b> ' + escHtml(String(disp)) + '</span>';
	};
	_fmtSummary('Query ID',              r.queryId);
	_fmtSummary('Database',              r.dbname);
	_fmtSummary('Object context',        r.objectContext);
	_fmtSummary('Plan count',            r.planCount);
	_fmtSummary('Total executions',      r.totalExecutions);

	// ── CPU Efficiency % ─────────────────────────────────────────────────────
	// Formula: (AvgCPU / AvgDOP) / AvgElapsed × 100
	// Meaning: of the wall-clock time this query spent running, what fraction
	// was actual CPU work (normalized for parallelism)?
	// 100% = fully CPU-bound (no waiting). Low % = mostly waiting (I/O, locks, …).
	if (r.avgDurationUs != null && r.avgCpuUs != null) {
		var _dop  = (r.avgDop != null && r.avgDop > 0) ? r.avgDop : 1;
		var _eff  = (r.avgCpuUs / _dop) / r.avgDurationUs * 100;
		var _effDisp = _eff.toFixed(1) + '%';
		// Colour-code: ≥70% green, 30-70% orange, <30% red
		var _effColor = _eff >= 70 ? '#1a7a1a' : (_eff >= 30 ? '#a05000' : '#cc0000');
		var _effTip = 'CPU Efficiency = (Avg CPU ÷ Avg DOP) ÷ Avg Elapsed × 100\n\n'
		            + 'Measures how much of the query\'s wall-clock time was actual CPU work,\n'
		            + 'corrected for parallelism (DOP ' + _dop.toFixed(1) + ').\n\n'
		            + '≥ 70%  CPU-bound — query works hard; tuning focus: algorithm / index.\n'
		            + '30–70%  Mixed — some waiting; check wait stats below.\n'
		            + '< 30%  Wait-dominated — I/O, locks, memory grants, or network are the bottleneck.\n\n'
		            + 'Values > 100% can occur due to timing granularity; treat as ~100%.';
		html += '<span title="' + escHtml(_effTip) + '">'
		      + '<b>CPU Efficiency:</b> <span style="font-weight:700;color:' + _effColor + ';">' + _effDisp + '</span>'
		      + '</span>';
	}

	_fmtSummaryUs('Avg elapsed',    r.avgDurationUs);
	_fmtSummaryUs('Max elapsed',    r.maxDurationUs);
	_fmtSummaryUs('Avg CPU',        r.avgCpuUs);
	_fmtSummaryReads('Avg reads',        r.avgLogicalReads);
	if (r.avgRowcount != null) {
		html += '<span><b>Avg rows:</b> ' + escHtml(_qsFmt1Dec(r.avgRowcount) || String(r.avgRowcount)) + '</span>';
	}
	if (r.avgMemoryMb != null) {
		html += '<span><b>Avg memory:</b> ' + escHtml((_qsFmt1Dec(r.avgMemoryMb) || r.avgMemoryMb.toFixed(1)) + ' MB') + '</span>';
	}
	if (r.firstSeen) html += '<span><b>First seen:</b> ' + escHtml(String(r.firstSeen)) + '</span>';
	if (r.lastSeen)  html += '<span><b>Last seen:</b> '  + escHtml(String(r.lastSeen))  + '</span>';
	html += '</div>';

	// Regression signal badge
	var reg = r.regressionSignal;
	if (reg && reg.detected) {
		html += '<div style="margin-top:4px;font-size:0.8em;color:#b35900;">'
		      + '⚠ Plan regression detected — max/min elapsed ratio: <b>' + reg.maxMinRatio + '</b>'
		      + (reg.note ? ' — ' + escHtml(reg.note) : '') + '</div>';
	}
	html += '</div>';

	// ── Per-interval sparklines at the top of Query Details ──────────────────────
	// Aggregated from the plan sparkline arrays so they use the SAME time axis and
	// resolution as the Execution Plans table (SPARKLINE_INTERVALS=48, not the
	// TIMELINE_INTERVALS=24 used by the detail timeline).
	// executions: summed across plans.
	// avg metrics: weighted average by per-interval execution count.
	var _topPlans = r.plans || [];
	// Find the plan with the most sparkDates (after multi-day concatenation all plans for
	// the same set of days should have the same length, but guard just in case).
	var _refPlan = null;
	_topPlans.forEach(function(p) {
		if (p.sparkDates && p.sparkDates.length > 0)
			if (!_refPlan || p.sparkDates.length > _refPlan.sparkDates.length) _refPlan = p;
	});
	var _topSparkBuilt = false;
	var _spark = {}, _dates = [];
	if (_refPlan) {
		var _n = _refPlan.sparkDates.length;
		_dates = _refPlan.sparkDates;
		// metric keys present across all plans — union of keys found in any plan
		var _metricKeys = ['executions','duration','cpu','reads','physReads','writes','rowcount','memory','tempdb','log'];
		_metricKeys.forEach(function(k) { _spark[k] = new Array(_n).fill(0); });
		var _execTotals = new Array(_n).fill(0);   // per-interval total executions (for weighting)
		_topPlans.forEach(function(p) {
			if (!p.spark || !p.sparkDates) return;
			var pLen = p.sparkDates.length;
			// Offset into the shared axis: plans shorter than _refPlan are right-aligned
			// (they appeared in fewer days — their data covers the tail of the period).
			var offset = _n - pLen;
			var execArr = p.spark.executions || [];
			for (var i = 0; i < pLen; i++) {
				var e = Number(execArr[i]) || 0;
				_execTotals[offset + i] += e;
				_spark.executions[offset + i] += e;
				['duration','cpu','reads','physReads','writes','rowcount','memory','tempdb','log'].forEach(function(k) {
					if (!p.spark[k]) return;
					_spark[k][offset + i] += (Number(p.spark[k][i]) || 0) * e;
				});
			}
		});
		// Divide weighted sums by total executions to get weighted averages
		['duration','cpu','reads','physReads','writes','rowcount','memory','tempdb','log'].forEach(function(k) {
			_spark[k] = _spark[k].map(function(s, i) {
				return _execTotals[i] > 0 ? s / _execTotals[i] : 0;
			});
			// Remove key only when NO plan has this metric array at all (server doesn't support the column).
			// Keep all-zero keys — a flat-zero sparkline is shown in the plan table too.
			var hasKey = _topPlans.some(function(p) { return p.spark && Array.isArray(p.spark[k]); });
			if (!hasKey) delete _spark[k];
		});
		_topSparkBuilt = true;
	}
	if (_topSparkBuilt) {
		// waitMs: sum wait-category sparklines per interval — Java already built these,
		// and _qsDetailMergeResults concatenated them across days. They have their own
		// time axis (sparkDates) which may differ from the timeline, so we track it separately.
		var _waitMsVals = null, _waitMsDates = null;
		var _ws = r.waitStats;
		if (_ws && _ws.length > 0 && _ws[0].sparkDates && _ws[0].sparkDates.length > 0) {
			var _wn = _ws[0].sparkDates.length;
			var _wtotals = [];
			for (var _wi = 0; _wi < _wn; _wi++) _wtotals.push(0);
			_ws.forEach(function(w) {
				if (w.spark) {
					for (var _wi2 = 0; _wi2 < Math.min(w.spark.length, _wn); _wi2++)
						_wtotals[_wi2] += Number(w.spark[_wi2]) || 0;
				}
			});
			if (_wtotals.some(function(v) { return v > 0; })) {
				_waitMsVals  = _wtotals;
				_waitMsDates = _ws[0].sparkDates;
			}
		}

		var _detailSparkDefs = [
			{ key: 'executions', label: 'Executions',      unit: ''      },
			{ key: 'duration',   label: 'Avg Elapsed',     unit: 'ms'    },
			{ key: 'cpu',        label: 'Avg CPU',         unit: 'ms'    },
			// waitMs inserted below via _waitMsVals if available
			{ key: 'reads',      label: 'Avg Reads',       unit: 'pages' },
			{ key: 'physReads',  label: 'Avg Phys Reads',  unit: 'pages' },
			{ key: 'writes',     label: 'Avg Writes',      unit: 'pages' },
			{ key: 'rowcount',   label: 'Avg Rows',        unit: ''      },
			{ key: 'memory',     label: 'Avg Mem',         unit: 'MB'    },
			{ key: 'tempdb',     label: 'Avg Tempdb',      unit: 'MB'    },
			{ key: 'log',        label: 'Avg Log',         unit: 'MB'    }
		];
		var _waitMsInserted = false;
		var _emitWaitMs = function() {
			if (!_waitMsVals || _waitMsInserted) return;
			_waitMsInserted = true;
			html += '<div style="display:flex;flex-direction:column;align-items:center;">'
			      + '<span style="font-size:0.75em;color:#555;margin-bottom:2px;">Wait Time</span>'
			      + '<span class="qs-sparkline qs-detail-spark"'
			      + ' data-vals="'  + escHtml(_waitMsVals.join(','))  + '"'
			      + ' data-dates="' + escHtml(_waitMsDates.join('|')) + '"'
			      + ' data-unit="ms" data-width="120px"'
			      + '></span>'
			      + '</div>';
		};
		html += '<div style="display:flex;flex-wrap:wrap;gap:10px 20px;padding:5px 0 6px 0;border-bottom:1px solid #dee2e6;margin-bottom:8px;">';
		_detailSparkDefs.forEach(function(def) {
			var vals = _spark[def.key];
			// Skip only when the key was never populated (null/undefined), not when all-zero —
			// a flat-zero sparkline is still informative (e.g. physReads=0 confirms cache-only reads).
			if (!vals || !vals.length) return;
			// Insert waitMs right after cpu (before reads)
			if (def.key === 'reads') _emitWaitMs();
			html += '<div style="display:flex;flex-direction:column;align-items:center;">'
			      + '<span style="font-size:0.75em;color:#555;margin-bottom:2px;">' + def.label + '</span>'
			      + '<span class="qs-sparkline qs-detail-spark"'
			      + ' data-vals="'  + escHtml(vals.join(',')) + '"'
			      + ' data-dates="' + escHtml(_dates.join('|')) + '"'
			      + ' data-unit="'  + escHtml(def.unit) + '"'
			      + ' data-width="120px"'
			      + '></span>'
			      + '</div>';
		});
		_emitWaitMs();   // fallback: append at end if 'reads' column was empty/absent
		html += '</div>';
	}

	// ── SQL Text ──────────────────────────────────────────────────────────────
	if (r.queryText) {
		html += '<div style="margin-bottom:8px;">'
		      + '<div style="font-size:0.8em;font-weight:600;color:#555;margin-bottom:2px;">'
		      + 'SQL Text'
		      + '&nbsp;<button type="button" class="btn btn-outline-secondary btn-sm"'
		      + ' style="font-size:0.78em;padding:0px 7px;vertical-align:baseline;"'
		      + ' title="Re-format the SQL text using T-SQL formatting rules (keywords uppercased, indented)"'
		      + ' onclick="qsDetailFormatSql();">Format SQL</button>'
		      + '</div>'
		      + '<pre style="max-height:200px;overflow:auto;border:1px solid #ddd;border-radius:3px;padding:6px;font-size:0.79em;white-space:pre-wrap;"><code id="qs-detail-sql-content" class="language-sql">'
		      + escHtml(r.queryText) + '</code></pre>'
		      + '</div>';
	}

	// ── Execution Plans ───────────────────────────────────────────────────────
	var plans = r.plans || [];
	if (plans.length > 0) {
		html += '<div style="font-size:0.8em;font-weight:600;color:#555;margin-bottom:3px;">Execution Plans (' + plans.length + ')</div>';
		html += '<div style="overflow-x:auto;margin-bottom:8px;"><table class="table table-sm table-bordered" style="font-size:0.8em;white-space:nowrap;width:auto;">';
		// Detect which optional columns are present (server returns them only for SQL 2017+)
		var _hasMemory = r.schemaHasMemoryCols === true;
		var _has2017   = r.schemaHas2017Cols   === true;   // tempdb + log columns
		var _hasDop    = r.schemaHasDopCol     === true || _has2017;  // avg_dop may exist without tempdb/log
		var _n = function(v) { return _qsFmtNum(v) || (v != null ? v : '—'); };
		// _nmb: already-MB value — render as "X.X MB" with 1 decimal + thousands sep
		var _nmb = function(mb) {
			if (mb == null) return '—';
			return (_qsFmt1Dec(mb) || (typeof mb === 'number' ? mb.toFixed(1) : parseFloat(mb).toFixed(1))) + ' MB';
		};
		// _nus: µs value — display as ms (tooltip shows µs when < 1 ms, h/m/s when >= 1 s)
		var _nus = function(us) {
			if (us == null) return '—';
			var r = _qsFmtUsAsMs(us);
			return r.tip ? '<span title="' + escHtml(r.tip) + '">' + escHtml(r.disp) + '</span>' : escHtml(r.disp);
		};
		// _nrd: page-count value — render with KB/MB/GB tooltip (1 page = 8 KB)
		var _nrd = function(pages) {
			if (pages == null) return '—';
			var disp = _qsFmtNum(pages) || pages;
			var tip  = _qsPagesToMb(pages);
			return tip ? '<span title="' + escHtml(tip) + '">' + disp + '</span>' : String(disp);
		};
		// _nms: millisecond value — render integer-formatted; tooltip shows decimal precision + h/m/s
		// dec: when supplied the value is a float — show rounded integer in cell,
		//      put precise decimal value in tooltip (only when fractional part is non-zero).
		var _nms = function(ms, dec) {
			if (ms == null) return '—';
			var tips = [];
			var disp;
			if (dec != null && typeof ms === 'number') {
				// Cell: integer with thousands separator (same style as Total/Max columns)
				disp = _qsFmtNum(Math.round(ms)) || String(Math.round(ms));
				// Tooltip line 1: precise decimal value — only when there is a fractional part
				if (ms !== Math.round(ms)) {
					var precise = ms.toFixed(dec);
					tips.push((_qsFmtNum(precise) || precise) + ' ms');
				}
			} else {
				disp = _qsFmtNum(ms) || String(ms);
			}
			// Tooltip line 2 (or 1): h/m/s breakdown for values >= 1 s
			if (typeof ms === 'number' && ms >= 1000) tips.push(_qsUsToHms(ms * 1000));
			var tip = tips.join('\n');
			return tip ? '<span title="' + escHtml(tip) + '">' + disp + '</span>' : disp;
		};
		var _th = function(label, tip) {
			return '<th' + (tip ? ' title="' + escHtml(tip) + '"' : '') + '>' + escHtml(label) + '</th>';
		};
		var _effTip = 'CPU Efficiency = (Avg CPU \u00f7 Avg DOP) \u00f7 Avg Elapsed \u00d7 100. '
		            + 'How much of the wall-clock time was actual CPU work, corrected for parallelism. '
		            + '\u226570%: CPU-bound \u2014 tune algorithm/index. '
		            + '30\u201370%: mixed \u2014 some waiting, check wait stats. '
		            + '<30%: wait-dominated \u2014 I/O, locks, memory grants, or network are the bottleneck. '
		            + 'Values >100% can occur due to timing granularity; treat as ~100%.';
		var _effCell = function(avgCpuUs, avgDop, avgDurationUs) {
			var dur = Number(avgDurationUs) || 0;
			var cpu = Number(avgCpuUs)      || 0;
			var dop = (avgDop != null && Number(avgDop) > 0) ? Number(avgDop) : 1;
			if (dur <= 0) return '<td style="text-align:right;color:#aaa;">—</td>';
			var eff   = (cpu / dop) / dur * 100;
			var color = eff >= 70 ? '#1a7a1a' : (eff >= 30 ? '#a05000' : '#cc0000');
			return '<td style="text-align:right;font-weight:700;color:' + color + ';">' + eff.toFixed(1) + '%</td>';
		};
		html += '<thead class="thead-light"><tr>';
		if (plans[0] && plans[0].queryPlan !== undefined) html += _th('Plans', 'Show Plan: estimated plan stored in Query Store.\nGet Last Actual: fetches the last-known actual plan via dm_exec_query_plan_stats — requires SQL Server 2019+ (or TF 2451) and LAST_QUERY_PLAN_STATS=ON on the database.');
		html += _th('Plan ID',          'Query Store plan_id — unique identifier for this execution plan')
		      + _th('Forced',           'Whether this plan has been forced via sp_query_store_force_plan')
		      + _th('Forcing type',     'How the plan was forced (MANUAL, AUTO, QUERY_STORE, …)')
		      + _th('Executions',       'Total executions recorded for this plan')
		      + _th('📈 Executions',    'Timeline: execution count per interval — shows when this plan was active')
		      + _th('CPU Eff %',        _effTip)
		      // Duration group — sparkline LAST (section divider, mirrors Top Queries layout)
		      + _th('Avg Elapsed (ms)', 'Average wall-clock elapsed time per execution, in milliseconds')
		      + _th('Min Elapsed (ms)', 'Shortest single execution observed, in milliseconds')
		      + _th('Max Elapsed (ms)', 'Longest single execution observed, in milliseconds')
		      + _th('Tot Elapsed (ms)', 'Total elapsed time summed across ALL executions for this plan, in milliseconds')
		      + _th('📈 Avg Elapsed',   'Timeline: avg elapsed (ms) per interval for this plan')
		      // CPU group
		      + _th('Avg CPU (ms)',     'Average CPU time per execution, in milliseconds')
		      + _th('Max CPU (ms)',     'Peak CPU time for a single execution, in milliseconds')
		      + _th('Tot CPU (ms)',     'Total CPU time summed across ALL executions for this plan, in milliseconds')
		      + _th('📈 Avg CPU',       'Timeline: avg CPU (ms) per interval for this plan')
		      // Logical reads group
		      + _th('Avg Reads',        'Average logical (buffer pool) page reads per execution (1 page = 8 KB)')
		      + _th('Max Reads',        'Peak logical reads in a single execution (1 page = 8 KB)')
		      + _th('Tot Reads',        'Total logical page reads across ALL executions for this plan (1 page = 8 KB)')
		      + _th('📈 Avg Reads',     'Timeline: avg logical reads per interval for this plan')
		      // Physical reads group
		      + _th('Avg Phys Reads',   'Average physical disk page reads per execution (1 page = 8 KB; high → cache misses)')
		      + _th('Max Phys Reads',   'Peak physical disk page reads observed in a single execution (1 page = 8 KB)')
		      + _th('Tot Phys Reads',   'Total physical disk page reads across ALL executions for this plan (1 page = 8 KB)')
		      + _th('📈 Avg Phys Reads','Timeline: avg physical reads per interval for this plan')
		      // Logical writes group
		      + _th('Avg Writes',       'Average logical page writes per execution (1 page = 8 KB)')
		      + _th('Max Writes',       'Peak logical page writes observed in a single execution (1 page = 8 KB)')
		      + _th('Tot Writes',       'Total logical page writes across ALL executions for this plan (1 page = 8 KB)')
		      + _th('📈 Avg Writes',    'Timeline: avg logical writes per interval for this plan')
		      // Rows group
		      + _th('Avg Rows',         'Average number of rows returned/affected per execution')
		      + _th('Tot Rows',         'Total rows returned or affected across ALL executions for this plan')
		      + _th('📈 Avg Rows',      'Timeline: avg row count per interval for this plan')
		      // Optional groups
		      + (_hasMemory ? _th('Avg Mem',      'Average memory grant used per execution, in MB (8-KB pages ÷ 128)') : '')
		      + (_hasMemory ? _th('Tot Mem',      'Total memory grant across ALL executions for this plan, in MB') : '')
		      + (_hasMemory ? _th('📈 Avg Mem',   'Timeline: avg memory grant (MB) per interval for this plan') : '')
		      + (_hasDop    ? _th('Avg DOP',      'Average degree of parallelism used per execution (SQL Server 2016+)') : '')
		      + (_has2017   ? _th('Avg Tempdb',   'Average tempdb space used per execution in MB (SQL Server 2017+)') : '')
		      + (_has2017   ? _th('📈 Avg Tempdb','Timeline: avg tempdb usage (MB) per interval for this plan') : '')
		      + (_has2017   ? _th('Avg Log',      'Average transaction log generated per execution in MB (SQL Server 2017+)') : '')
		      + (_has2017   ? _th('📈 Avg Log',   'Timeline: avg log usage (MB) per interval for this plan') : '')
		      + _th('First seen', 'When this plan was first recorded in the Query Store')
		      + _th('Last seen',  'When this plan was most recently recorded');
		html += '</tr></thead><tbody>';

		// Pre-compute the per-metric max across ALL plans so every sparkline column shares
		// the same Y-axis range — makes plan switchovers and overlaps visually obvious.
		var _planSparkMax = {};
		plans.forEach(function(p) {
			if (!p.spark) return;
			Object.keys(p.spark).forEach(function(k) {
				p.spark[k].forEach(function(v) {
					var n = Number(v) || 0;
					if (n > (_planSparkMax[k] || 0)) _planSparkMax[k] = n;
				});
			});
		});

		_qsXmlPlans = [];   // reset plan slot array for this render
		plans.forEach(function(p) {
			// Build plan buttons cell first (leftmost), then stats columns
			var planBtnCell = '';
			if (p.queryPlan !== undefined) {
				// Store plan metadata in a slot array; pass only the index to onclick
				// to avoid embedding raw XML (with its own quotes) inside an HTML attribute.
				var planSlot = _qsXmlPlans.length;
				var planLabel = r.objectContext ? 'Query ' + r.queryId + ' \u2014 ' + r.objectContext : 'Query ' + r.queryId;
				_qsXmlPlans.push({
					xml:                  p.queryPlan            || null,
					sqlText:              r.queryText            || '',
					objectName:           planLabel,
					planId:               p.planId,
					dbname:               r.dbname,
					ts:                   _qsTimestamp           || null,
					lastCompileStartTime: p.lastCompileStartTime || null,
					lastSeen:             p.lastSeen             || null
				});
				var btnStyle = 'font-size:0.8em;height:18px;padding:0 5px;';
				planBtnCell = '<td style="white-space:nowrap;">';
				if (p.queryPlan) {
					planBtnCell += '<button type="button" style="' + btnStyle + '" onclick="qsShowXmlPlan(' + planSlot + ');" title="Estimated plan stored in Query Store">Show Plan</button> ';
				}
				planBtnCell += '<button type="button" style="' + btnStyle + '" id="qs-gla-btn-' + planSlot + '"'
				            + ' onclick="qsGetLastActual(' + planSlot + ');"'
				            + ' title="Fetch last actual execution plan via dm_exec_query_plan_stats.&#10;' 
							+ 'Requires SQL Server 2019+ (or 2016/2017 with Trace Flag 2451 enabled),&#10;' 
							+ 'and the database must have LAST_QUERY_PLAN_STATS enabled&#10;' 
							+ '(ALTER DATABASE SCOPED CONFIGURATION SET LAST_QUERY_PLAN_STATS = ON).&#10;' 
							+ '&#10;'
							+ 'If its not enabled the plan is probably a Estimated Plan, look at the dialog Header...">Get Last Actual</button>';
				planBtnCell += '</td>';
			}
			// Build plan sparkline cells (shared Y-axis via data-range-max)
			var _planSparkCell = function(key, unit) {
				var vals  = (p.spark && p.spark[key]) || [];
				var dates = (p.sparkDates) || [];
				if (!vals.length) return '<td style="padding:2px 4px;"><span style="color:#ccc;">—</span></td>';
				var rmax  = _planSparkMax[key];
				return '<td style="padding:2px 4px;vertical-align:middle;">'
				     + '<span class="qs-sparkline qs-plan-spark"'
				     + ' data-vals="'  + escHtml(vals.join(','))   + '"'
				     + ' data-dates="' + escHtml(dates.join('|'))  + '"'
				     + ' data-unit="'  + escHtml(unit)             + '"'
				     + ' data-color="#5b9a3c"'
				     + (rmax > 0 ? ' data-range-max="' + rmax + '"' : '')
				     + '></span></td>';
			};
			html += '<tr>'
			      + planBtnCell
			      + '<td style="color:#0066cc;font-weight:600;">' + (p.planId != null ? p.planId : '—') + '</td>'
			      + '<td>' + (p.isForcedPlan ? '<span style="color:darkorange;">✓ forced</span>' : '—') + '</td>'
			      + '<td>' + escHtml(p.planForcingType || '—') + '</td>'
			      + '<td style="text-align:right;">' + _n(p.totalExecutions)     + '</td>'
			      + _planSparkCell('executions', '')
			      + _effCell(p.avgCpuUs, p.avgDop, p.avgDurationUs)
			      // Duration group — sparkline last
			      + '<td style="text-align:right;">' + _nus(p.avgDurationUs)     + '</td>'
			      + '<td style="text-align:right;">' + _nus(p.minDurationUs)     + '</td>'
			      + '<td style="text-align:right;">' + _nus(p.maxDurationUs)     + '</td>'
			      + '<td style="text-align:right;">' + _nus(p.totalDurationUs)   + '</td>'
			      + _planSparkCell('duration',  'ms')
			      // CPU group
			      + '<td style="text-align:right;">' + _nus(p.avgCpuUs)          + '</td>'
			      + '<td style="text-align:right;">' + _nus(p.maxCpuUs)          + '</td>'
			      + '<td style="text-align:right;">' + _nus(p.totalCpuUs)        + '</td>'
			      + _planSparkCell('cpu',       'ms')
			      // Logical reads group
			      + '<td style="text-align:right;">' + _nrd(p.avgLogicalReads)   + '</td>'
			      + '<td style="text-align:right;">' + _nrd(p.maxLogicalReads)   + '</td>'
			      + '<td style="text-align:right;">' + _nrd(p.totalLogicalReads) + '</td>'
			      + _planSparkCell('reads',     'pages')
			      // Physical reads group
			      + '<td style="text-align:right;">' + _nrd(p.avgPhysicalReads)  + '</td>'
			      + '<td style="text-align:right;">' + _nrd(p.maxPhysicalReads)  + '</td>'
			      + '<td style="text-align:right;">' + _nrd(p.totalPhysicalReads)+ '</td>'
			      + _planSparkCell('physReads', 'pages')
			      // Logical writes group
			      + '<td style="text-align:right;">' + _nrd(p.avgLogicalWrites)  + '</td>'
			      + '<td style="text-align:right;">' + _nrd(p.maxLogicalWrites)  + '</td>'
			      + '<td style="text-align:right;">' + _nrd(p.totalLogicalWrites)+ '</td>'
			      + _planSparkCell('writes',    'pages')
			      // Rows group
			      + '<td style="text-align:right;">' + _n(p.avgRowcount)         + '</td>'
			      + '<td style="text-align:right;">' + _n(p.totalRowcount)       + '</td>'
			      + _planSparkCell('rowcount',  '')
			      // Optional groups
			      + (_hasMemory ? '<td style="text-align:right;">' + _nmb(p.avgMemoryMb)  + '</td>' : '')
			      + (_hasMemory ? '<td style="text-align:right;">' + _nmb(p.totalMemoryMb)+ '</td>' : '')
			      + (_hasMemory ? _planSparkCell('memory', 'MB') : '')
			      + (_hasDop    ? '<td style="text-align:right;">' + _n(p.avgDop)         + '</td>' : '')
			      + (_has2017   ? '<td style="text-align:right;">' + _nmb(p.avgTempdbMb)  + '</td>' : '')
			      + (_has2017   ? _planSparkCell('tempdb', 'MB') : '')
			      + (_has2017   ? '<td style="text-align:right;">' + _nmb(p.avgLogMb)     + '</td>' : '')
			      + (_has2017   ? _planSparkCell('log',    'MB') : '')
			      + '<td>' + escHtml(p.firstSeen || '—') + '</td>'
			      + '<td>' + escHtml(p.lastSeen  || '—') + '</td>'
			      + '</tr>';
		});
		html += '</tbody></table></div>';
	}

	// ── Wait Statistics ───────────────────────────────────────────────────────
	var waits = r.waitStats || [];
	if (waits.length > 0) {
		html += '<div style="font-size:0.8em;font-weight:600;color:#555;margin-bottom:3px;">Top Wait Types</div>';
		html += '<div style="overflow-x:auto;margin-bottom:8px;"><table class="table table-sm table-bordered" style="font-size:0.8em;white-space:nowrap;width:auto;">';
		html += '<thead class="thead-light"><tr>'
		      + _th('Wait category',    'SQL Server wait type category (e.g. CPU, Lock, I/O, Network)')
		      + _th('📈 Wait Time Chart', 'Timeline: total wait (ms) per Query Store interval — shows WHEN this wait type was occurring')
		      + _th('Total wait (ms)',  'Sum of wait time in milliseconds across all executions in this period')
		      + _th('Avg wait (ms)',   'Average wait time per execution in milliseconds')
		      + _th('Max wait (ms)',   'Peak wait time observed in a single execution, in milliseconds')
		      + '</tr></thead><tbody>';
		var maxAvgWait   = Math.max.apply(null, waits.map(function(w) { return Number(w.avgWaitMs)   || 0; }));
		var maxTotalWait = Math.max.apply(null, waits.map(function(w) { return Number(w.totalWaitMs) || 0; }));
		waits.forEach(function(w) {
			var avgPct   = maxAvgWait   > 0 ? Math.round(100 * (Number(w.avgWaitMs)   || 0) / maxAvgWait)   : 0;
			var totPct   = maxTotalWait > 0 ? Math.round(100 * (Number(w.totalWaitMs) || 0) / maxTotalWait) : 0;
			var avgBg    = 'background:linear-gradient(to right,rgba(70,130,180,0.22) ' + avgPct + '%,transparent ' + avgPct + '%)';
			var totBg    = 'background:linear-gradient(to right,rgba(70,130,180,0.13) ' + totPct + '%,transparent ' + totPct + '%)';
			var waitVals  = w.spark      || [];
			var waitDates = w.sparkDates || [];
			var waitSparkCell = waitVals.length
				? '<td style="padding:2px 4px;vertical-align:middle;">'
				  + '<span class="qs-sparkline"'
				  + ' data-vals="'  + escHtml(waitVals .join(','))  + '"'
				  + ' data-dates="' + escHtml(waitDates.join('|'))  + '"'
				  + ' data-unit="ms" data-color="#c0392b" data-width="100px"'
				  + '></span></td>'
				: '<td style="padding:2px 4px;"><span style="color:#ccc;">—</span></td>';
			html += '<tr>'
			      + '<td>' + escHtml(w.waitCategory || '—') + '</td>'
			      + waitSparkCell
			      + '<td style="text-align:right;' + totBg + '">' + _nms(w.totalWaitMs)  + '</td>'
			      + '<td style="text-align:right;' + avgBg + '">' + _nms(w.avgWaitMs, 2) + '</td>'
			      + '<td style="text-align:right;">'              + _nms(w.maxWaitMs)     + '</td>'
			      + '</tr>';
		});
		html += '</tbody></table></div>';
	}

	// ── Execution Timeline ────────────────────────────────────────────────────
	var timeline = r.timeline || [];
	if (timeline.length > 0) {
		html += '<div style="font-size:0.8em;font-weight:600;color:#555;margin-bottom:3px;">Execution Timeline</div>';
		html += '<div style="overflow-x:auto;margin-bottom:8px;"><table class="table table-sm table-bordered" style="font-size:0.8em;white-space:nowrap;width:auto;">';
		html += '<thead class="thead-light"><tr>'
		      + _th('Interval start', 'Start of the Query Store collection interval')
		      + _th('Interval end',   'End of the Query Store collection interval')
		      + _th('Executions',     'Number of executions in this interval')
		      + _th('CPU Eff %',      _effTip)
		      + _th('Avg Elapsed (ms)', 'Average elapsed time per execution in this interval, in milliseconds')
		      + _th('Min Elapsed (ms)', 'Shortest execution in this interval, in milliseconds')
		      + _th('Max Elapsed (ms)', 'Longest execution in this interval, in milliseconds')
		      + _th('Avg CPU (ms)',    'Average CPU time per execution in this interval, in milliseconds')
		      + _th('Avg Reads',       'Average logical page reads per execution in this interval (1 page = 8 KB)')
		      + _th('Avg Phys Reads',  'Average physical disk page reads per execution in this interval (1 page = 8 KB)')
		      + _th('Avg Writes',      'Average logical page writes per execution in this interval (1 page = 8 KB)')
		      + _th('Avg Rows',        'Average rows returned or affected per execution in this interval')
		      + (_hasMemory ? _th('Avg Mem',    'Average memory grant per execution in this interval, in MB') : '')
		      + (_hasDop   ? _th('Avg DOP',     'Average degree of parallelism per execution (SQL Server 2016+)') : '')
		      + (_has2017  ? _th('Avg Tempdb',  'Average tempdb space per execution in MB (SQL Server 2017+)') : '')
		      + (_has2017  ? _th('Avg Log',     'Average transaction log per execution in MB (SQL Server 2017+)') : '')
		      + '</tr></thead><tbody>';
		timeline.forEach(function(t) {
			html += '<tr>'
			      + '<td>' + escHtml(t.intervalStart || '—') + '</td>'
			      + '<td>' + escHtml(t.intervalEnd   || '—') + '</td>'
			      + '<td style="text-align:right;">' + _n(t.totalExecutions)    + '</td>'
			      + _effCell(t.avgCpuUs, t.avgDop, t.avgDurationUs)
			      + '<td style="text-align:right;">' + _nus(t.avgDurationUs)    + '</td>'
			      + '<td style="text-align:right;">' + _nus(t.minDurationUs)    + '</td>'
			      + '<td style="text-align:right;">' + _nus(t.maxDurationUs)    + '</td>'
			      + '<td style="text-align:right;">' + _nus(t.avgCpuUs)         + '</td>'
			      + '<td style="text-align:right;">' + _nrd(t.avgLogicalReads)   + '</td>'
			      + '<td style="text-align:right;">' + _nrd(t.avgPhysicalReads)  + '</td>'
			      + '<td style="text-align:right;">' + _nrd(t.avgLogicalWrites)  + '</td>'
			      + '<td style="text-align:right;">' + _n(t.avgRowcount)         + '</td>'
			      + (_hasMemory ? '<td style="text-align:right;">' + _nmb(t.avgMemoryMb)  + '</td>' : '')
			      + (_hasDop   ? '<td style="text-align:right;">' + _n(t.avgDop)          + '</td>' : '')
			      + (_has2017  ? '<td style="text-align:right;">' + _nmb(t.avgTempdbMb)   + '</td>' : '')
			      + (_has2017  ? '<td style="text-align:right;">' + _nmb(t.avgLogMb)      + '</td>' : '')
			      + '</tr>';
		});
		html += '</tbody></table></div>';
	}

	// ── Recommendations ───────────────────────────────────────────────────────
	var recs = r.recommendations || [];
	if (recs.length > 0) {
		html += '<div style="font-size:0.8em;font-weight:600;color:#0d6efd;margin-bottom:3px;">Tuning Recommendations (' + recs.length + ')</div>';
		html += '<div style="overflow-x:auto;margin-bottom:8px;"><table class="table table-sm table-bordered table-info" style="font-size:0.8em;white-space:nowrap;width:auto;">';
		html += '<thead class="thead-light"><tr>'
		      + _th('Name',   'Recommendation name from sys.dm_db_tuning_recommendations')
		      + _th('Type',   'Recommendation type (e.g. FORCE_LAST_GOOD_PLAN, CREATE_INDEX)')
		      + _th('Reason', 'Reason the recommendation was generated')
		      + _th('Score',  'Confidence score 0–100; higher = more confident the change will improve performance')
		      + _th('State',  'Current state of the recommendation (Active, Verifying, Success, Reverted, …)')
		      + '</tr></thead><tbody>';
		recs.forEach(function(rec) {
			html += '<tr>'
			      + '<td>' + escHtml(rec.name   || '—') + '</td>'
			      + '<td>' + escHtml(rec.type   || '—') + '</td>'
			      + '<td>' + escHtml(rec.reason || '—') + '</td>'
			      + '<td>' + (rec.score != null ? rec.score : '—') + '</td>'
			      + '<td>' + escHtml(rec.state  || '—') + '</td>'
			      + '</tr>';
		});
		html += '</tbody></table></div>';
	}

	if (!html.trim())
		html = '<div class="p-3 text-muted">No detail data available for this query.</div>';

	$('#query-store-content').html(html);

	// Render sparklines in detail view + plan table
	_qsRenderSparklineSet('#query-store-content .qs-sparkline');

	// Highlight SQL syntax with Prism if loaded
	if (typeof Prism !== 'undefined')
		Prism.highlightAllUnder(document.getElementById('query-store-content'));
}

/**
 * Show XML execution plan using the existing showplan dialog (if available).
 * @param {number|string} slotOrXml  — integer index into _qsXmlPlans[] (preferred),
 *                                     or raw XML string (legacy / direct call).
 * Slot objects are {xml, sqlText, objectName} so the dialog can show SQL text
 * alongside the visual plan.
 */
function qsShowXmlPlan(slotOrXml)
{
	var entry;
	if (typeof slotOrXml === 'number' || (typeof slotOrXml === 'string' && /^\d+$/.test(slotOrXml))) {
		entry = _qsXmlPlans[parseInt(slotOrXml, 10)];
	} else {
		// Legacy / direct call with raw XML string
		entry = { xml: slotOrXml, sqlText: '', objectName: '' };
	}
	if (!entry || !entry.xml) return;

	if (typeof showSqlServerShowplanDialog === 'function') {
		var planMeta = {
			srv:    _qsSrvName || '',
			dbname: entry.dbname || '',
			ts:     entry.ts    || ''
		};
		showSqlServerShowplanDialog(entry.xml, entry.sqlText, entry.objectName, planMeta);
	} else {
		// Fallback: open raw XML in a new window
		var w = window.open('', '_blank', 'width=900,height=600,scrollbars=yes');
		if (w) {
			w.document.write('<pre style="font-size:11px;white-space:pre-wrap;padding:10px;">'
				+ entry.xml.replace(/</g, '&lt;').replace(/>/g, '&gt;') + '</pre>');
			w.document.close();
		}
	}
}

/**
 * Fetch the last-known actual execution plan for a Query Store plan via
 * dm_exec_query_plan_stats, with DDL Storage as fallback.
 * @param {number} slot  — index into _qsXmlPlans[]
 */
function qsGetLastActual(slot)
{
	var entry = _qsXmlPlans[slot];
	if (!entry || !entry.planId || !entry.dbname) {
		alert('Plan metadata not available — reload the Query Details and try again.');
		return;
	}

	var srv = _qsSrvName;
	if (!srv) { alert('No server selected.'); return; }

	// Disable button and show spinner while fetching
	var $btn = $('#qs-gla-btn-' + slot);
	var origText = $btn.text();
	$btn.prop('disabled', true).text('⏳ Fetching…');

	$.ajax({
		url:      '/api/cc/mgt/query-store',
		data:     { srv: srv, action: 'lastActualPlan', dbname: entry.dbname, planId: entry.planId, ts: entry.ts || '' },
		dataType: 'text',
		success: function(data) {
			$btn.prop('disabled', false).text(origText);
			var r;
			try { r = JSON.parse(data); } catch(ex) { alert('Parse error: ' + ex); return; }
			if (r.error) { alert('Error: ' + (r.message || r.error)); return; }
			if (r.found) {
				var sourceLabel = r.source === 'dm_exec_query_plan_stats' ? 'Last Actual (dm_exec_query_plan_stats)'
				                : r.source === 'ddl_storage'              ? 'Last Actual (DDL Storage)'
				                :                                           'Last Actual';
				var meta = {
					lastCompileStartTime: entry.lastCompileStartTime,
					lastSeen:             entry.lastSeen,
					source:               r.source,
					srv:                  _qsSrvName || '',
					dbname:               entry.dbname || '',
					ts:                   entry.ts    || ''
				};
				if (typeof showSqlServerShowplanDialog === 'function') {
					showSqlServerShowplanDialog(r.xml, entry.sqlText, entry.objectName + ' \u2014 ' + sourceLabel, meta);
				}
			} else {
				// Not found — build a clear explanation
				var msg = r.message || 'Last actual plan not found.';
				if (r.planStatsError)  msg += '\n\ndm_exec_query_plan_stats: ' + r.planStatsError;
				if (r.ddlStorageError) msg += '\nDDL Storage: ' + r.ddlStorageError;
				alert(msg);
			}
		},
		error: function(xhr) {
			$btn.prop('disabled', false).text(origText);
			var msg = 'HTTP ' + xhr.status;
			try { var j = JSON.parse(xhr.responseText); msg += ': ' + (j.message || j.error || xhr.responseText); } catch(e) {}
			alert('Failed to fetch last actual plan: ' + msg);
		}
	});
}

// ── Extract now ───────────────────────────────────────────────────────────────

function queryStoreTriggerExtract()
{
	var srv = _qsSrvName;
	if (!srv) return;

	var $status = $('#query-store-extract-status');
	$status.text('Triggering extraction…');

	$.ajax({
		url:    '/api/cc/mgt/query-store',
		method: 'POST',
		data:   { srv: srv, action: 'extract' },
		dataType: 'text',
		success: function(data) {
			try {
				var r = JSON.parse(data);
				if (r.error) { $status.text('Error: ' + (r.message || r.error)); return; }
				$status.text(r.message || 'Extraction started.');
				setTimeout(queryStorePollExtractStatus, 4000);
			} catch(ex) { $status.text('Parse error: ' + ex); }
		},
		error: function(xhr) {
			var msg = 'HTTP ' + xhr.status;
			try { var e = JSON.parse(xhr.responseText || '{}'); if (e.message) msg = e.message; } catch(i) {}
			$status.text(msg);
		}
	});
}

/**
 * Called from the "Extract now" button embedded in the empty-databases message.
 * Updates status inline (next to the button) instead of in the Top Queries toolbar.
 */
function queryStoreTriggerExtractInline(btn)
{
	var srv = _qsSrvName;
	if (!srv) return;

	var $btn    = $(btn);
	var $status = $('#qs-inline-extract-status');
	$btn.prop('disabled', true);
	$status.text('Triggering extraction…');

	$.ajax({
		url:    '/api/cc/mgt/query-store',
		method: 'POST',
		data:   { srv: srv, action: 'extract' },
		dataType: 'text',
		success: function(data) {
			try {
				var r = JSON.parse(data);
				if (r.error) { $status.text('Error: ' + (r.message || r.error)); $btn.prop('disabled', false); return; }
				$status.text(r.message || 'Extraction started…');
				setTimeout(function() { queryStorePollExtractStatusInline($btn, $status); }, 4000);
			} catch(ex) { $status.text('Parse error: ' + ex); $btn.prop('disabled', false); }
		},
		error: function(xhr) {
			var msg = 'HTTP ' + xhr.status;
			try { var e = JSON.parse(xhr.responseText || '{}'); if (e.message) msg = e.message; } catch(i) {}
			$status.text(msg);
			$btn.prop('disabled', false);
		}
	});
}

function queryStorePollExtractStatusInline($btn, $status)
{
	var srv = _qsSrvName;
	if (!srv) return;

	$.ajax({
		url:      '/api/cc/mgt/query-store',
		data:     { srv: srv, action: 'extractStatus' },
		dataType: 'text',
		success: function(data) {
			try {
				var r = JSON.parse(data);
				var status = r.status || r.extractStatus || '';
				if (status === 'running') {
					$status.text('Extracting… (still running)');
					setTimeout(function() { queryStorePollExtractStatusInline($btn, $status); }, 4000);
				} else {
					$status.text((status || 'Done.') + ' — reloading…');
					$btn.prop('disabled', false);
					// Reload databases tab — data should now be available
					setTimeout(function() { queryStoreDatabasesLoad(_qsSrvName, _qsTimestamp); }, 1000);
				}
			} catch(ex) { $status.text('Poll error: ' + ex); $btn.prop('disabled', false); }
		},
		error: function() { $status.text('Status check failed.'); $btn.prop('disabled', false); }
	});
}

/**
 * Switch to the most-recent historical recording (yesterday or latest available day).
 * Called from the "Look at Yesterday's data" button on the empty-databases view.
 */
function qsShowYesterdayData()
{
	if (!_qsAvailDates || !_qsAvailDates.length) {
		// _qsAvailDates loads async — may not be ready yet, or date-rolling is off
		var $status = $('#qs-inline-extract-status');
		$status.text('No historical recordings found for this server.');
		return;
	}
	var dateKey = _qsAvailDates[0];   // newest historical date (newest-first array)

	// Sync the date-select dropdown if it exists
	var $sel = $('#qs-date-select');
	if ($sel.length && $sel.find('option[value="' + dateKey + '"]').length)
		$sel.val(dateKey);

	// Apply the same logic as queryStoreDateChanged()
	_qsTimestamp      = dateKey + ' 23:59:59';
	_qsMultiDbCancel  = true;
	_qsMultiDbMode    = false;
	_qsMultiDbAllRows = null;
	_qsMultiDbOrigRows  = null;
	_qsMultiDbOrigTimelines = null;
	_qsMultiDbTimelines = null;
	_qsMultiDbCtx     = null;
	_qsBrushRefreshUi = null;
	if (_qsBrushResizeObs) { try { _qsBrushResizeObs.disconnect(); } catch(e) {} _qsBrushResizeObs = null; }
	_qsUpdateHistoricalBanner();
	_qsDbname  = null;
	_qsQueryId = null;
	_qsMainTab = 'databases';
	_qsResetTabs();
	_qsActivateTab('databases');
	queryStoreDatabasesLoad(_qsSrvName, _qsTimestamp);
}

function queryStorePollExtractStatus()
{
	var srv = _qsSrvName;
	if (!srv) return;

	$.ajax({
		url:      '/api/cc/mgt/query-store',
		data:     { srv: srv, action: 'extractStatus' },
		dataType: 'text',
		success: function(data) {
			try {
				var r = JSON.parse(data);
				var $s = $('#query-store-extract-status');
				var status = r.status || r.extractStatus || '';
				if (status === 'running') {
					$s.text('Extracting… (running in background)');
					setTimeout(queryStorePollExtractStatus, 4000);
				} else {
					$s.text(status || 'Done.');
					// Auto-refresh current tab
					if (_qsMainTab === 'databases') queryStoreDatabasesLoad(_qsSrvName, _qsTimestamp);
					else if (_qsMainTab === 'topqueries' && _qsDbname) queryStoreTopQueriesLoad();
				}
			} catch(ex) {}
		}
	});
}

// ── Table sort (same pattern as dbxGraphDbmsConfig.js / qsSortTable) ──────────

function qsSortTable(tableId, colIdx)
{
	var $tbl = $('#' + tableId);
	var $th  = $tbl.find('thead th').eq(colIdx);
	var cur  = parseInt($th.data('sort') || 0);
	var next = (cur + 1) % 3;
	$th.data('sort', next);
	$tbl.find('thead th').not($th).each(function() { $(this).data('sort', 0); $(this).find('.sort-icon').text(''); });
	$th.find('.sort-icon').text(next === 1 ? ' ▲' : next === 2 ? ' ▼' : '');

	var $tbody = $tbl.find('tbody');
	var rows   = $tbody.find('tr').toArray();

	if (next === 0) {
		rows.sort(function(a, b) {
			return (parseInt($(a).data('orig-idx') || 0)) - (parseInt($(b).data('orig-idx') || 0));
		});
	} else {
		rows.sort(function(a, b) {
			var av = $(a).find('td').eq(colIdx).text();
			var bv = $(b).find('td').eq(colIdx).text();
			var an = parseFloat(av), bn = parseFloat(bv);
			if (!isNaN(an) && !isNaN(bn)) return next === 1 ? an - bn : bn - an;
			return next === 1 ? av.localeCompare(bv) : bv.localeCompare(av);
		});
	}
	$tbody.find('tr').each(function(i) {
		if ($(this).data('orig-idx') === undefined) $(this).data('orig-idx', i);
	});
	$tbody.empty().append(rows);
}

// ── History slider ────────────────────────────────────────────────────────────

/**
 * Called from GraphBus slider-change listener.
 * Refreshes the Query Store panel if it is open and timestamps have changed.
 */
function queryStoreSliderRefresh(ts)
{
	if (_qsIsSqlServer && !_qsAutoOpenChecked) {
		_qsAutoOpenChecked = true;
		if (!$('#query-store-panel').is(':visible')) {
			try { if (localStorage.getItem('queryStore-panelOpen') === '1') queryStoreToggle(); } catch(e) {}
		}
	}
	// Query Store data is effectively static within a session (extracted nightly).
	// Only refresh databases list if the panel is open and this is the first tick.
	if ($('#query-store-panel').is(':visible') && _qsSrvName && ts) {
		_qsTimestamp = ts;
		$('#query-store-ts').text('@ ' + ts);
		// Don't auto-reload — let the user click Refresh explicitly (data doesn't change per-timestamp)
	}
}

// ── Helpers ───────────────────────────────────────────────────────────────────

function qsDetailFormatSql()
{
	var el = document.getElementById('qs-detail-sql-content');
	if (!el) return;
	if (typeof sqlFormatter === 'undefined') { alert('SQL formatter library not loaded.'); return; }
	try {
		var formatted = sqlFormatter.format(el.textContent, { language: 'tsql', tabWidth: 4, keywordCase: 'upper', tabulateAlias: true });
		el.textContent = formatted;
		if (typeof Prism !== 'undefined') Prism.highlightAll();
	} catch (err) { alert('Format error: ' + err); }
}

function queryStoreShowMsg(msg)
{
	$('#query-store-loading').hide();
	$('#query-store-content').html('<div style="padding:15px;color:#6c757d;">' + msg + '</div>');
}

function queryStoreShowHttpError(xhr, action)
{
	var msg = 'HTTP ' + xhr.status + ': ' + xhr.statusText;
	try {
		var e = JSON.parse(xhr.responseText || '{}');
		if (e.message) msg = e.message;
		else if (e.error) msg = e.error + (e.message ? ' — ' + e.message : '');
	} catch(ignored) {}
	queryStoreShowMsg('<span style="color:#dc3545;">' + escHtml(msg) + '</span>'
		+ (xhr.status === 404 ? '<br><small>No Query Store data found — try <b>Extract now</b>.</small>' : ''));
}
