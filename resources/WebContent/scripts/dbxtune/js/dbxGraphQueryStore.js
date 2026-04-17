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
var _qsSrvName        = null;   // server currently displayed
var _qsTimestamp      = null;   // timestamp (history mode); null = live/latest
var _qsMainTab        = 'databases'; // 'databases' | 'topqueries' | 'detail'
var _qsDbname         = null;   // currently selected database
var _qsQueryId        = null;   // currently selected query_id
var _qsAutoOpenChecked = false;
var _qsTqFilter       = '';     // current top-queries filter string
var _qsTqLastResult   = null;   // last fetched top-queries result (re-filter without re-fetch)
var _qsTqChart        = null;   // active Chart.js instance for Top Queries bar chart
var _qsXmlPlans         = [];     // XML plan strings indexed by slot; avoids inline onclick quoting
var _qsAvailDates       = [];     // date keys from recording-databases API (newest first, 'YYYY-MM-DD')
var _qsMultiDbMode      = false;  // true when cross-db result is currently displayed
var _qsMultiDbAllRows   = null;   // raw un-sliced merged rows; kept for re-ranking without re-fetch
var _qsMultiDbSchemaFlags = null; // schema flags (hasMemoryCols etc.) from first DB response
var _qsMultiDbCancel    = false;  // set to true by Cancel button during serial fetch

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
 * Format a number with 1 decimal place + thousands separator.
 * Used for row-count and MB values that can be fractional.
 * Returns null when val is null/NaN.
 */
function _qsFmt1Dec(v)
{
	if (v == null || v === undefined) return null;
	var n = (typeof v === 'number') ? v : parseFloat(v);
	if (isNaN(n)) return null;
	var parts = n.toFixed(1).split('.');
	parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g,
		typeof _qsNumFmtMode !== 'undefined' && _qsNumFmtMode === 'locale' ? ',' : '\u00a0');
	return parts.join('.');
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

	$panel.css('display', 'flex');
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
	_qsMainTab = 'databases';
	_qsResetTabs();
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
	var dbs = (r && r.databases) ? r.databases : [];
	if (dbs.length === 0) {
		$('#query-store-content').html(
			'<div style="padding:18px 12px;">'
			+ '<p style="margin-bottom:10px;">No databases with Query Store data found on this server.</p>'
			+ '<p style="color:#6c757d;font-size:0.88em;margin-bottom:14px;">'
			+ 'Data is collected during the nightly database rollover. '
			+ 'Click <b>Extract now</b> to pull Query Store data from the live SQL Server immediately. <br>'
			+ 'Or select an older date to view in the Dropdown that say <b>Today (live)</b></p>'
			+ '<button class="btn btn-sm btn-primary" onclick="queryStoreTriggerExtractInline(this);">'
			+ '<i class="fa fa-download"></i>&nbsp; Extract now</button>'
			+ '&nbsp;<span id="qs-inline-extract-status" style="font-size:0.85em;margin-left:8px;"></span>'
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
	var periodOptions = [1,3,7,14,30].map(function(n) {
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
	$('#query-store-content').html(html);
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
	var rankBy    = $('#query-store-rankby').val()           || 'duration';
	var topN      = parseInt($('#query-store-topn').val())   || 25;
	var minExec   = parseInt($('#query-store-minexec').val()) || 2;

	// Dates to fetch: null = today/live, then historical (newest first)
	var allDates = [null].concat(_qsAvailDates);
	var dates    = allDates.slice(0, Math.min(period, allDates.length));

	var ctx = {
		dbs:       dbs,
		dates:     dates,
		dbIdx:     0,
		dateIdx:   0,
		allRows:   [],
		schemaFlags: null,
		topN:      topN,
		rankBy:    rankBy,
		aggregate: aggregate,
		minExec:   minExec,
		total:     dbs.length * dates.length,
		done:      0
	};

	_qsMultiDbCancel  = false;
	_qsMultiDbMode    = false;
	_qsMultiDbAllRows = null;

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
		_qsMergeAndRender(ctx.allRows, ctx.schemaFlags || {}, ctx.rankBy, ctx.topN, ctx.aggregate);
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
	if (ts) params.ts = ts;

	$.ajax({
		url:      '/api/cc/mgt/query-store',
		data:     params,
		dataType: 'text',
		success: function(data) {
			if (_qsMultiDbCancel) return;
			try {
				var r = JSON.parse(data);
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
function _qsMergeAndRender(allRows, schemaFlags, rankBy, topN, aggregate)
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

	// Cache for re-ranking without re-fetch
	_qsMultiDbAllRows             = allRows;
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
		_aggregate:         aggregate
	};

	queryStoreRenderTopQueries(fakeR);
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
		default:               return 'totalDurationUs';
	}
}

/** Called when a row is clicked in multi-db mode — navigates to detail for that DB/query. */
function queryStoreSelectMultiDbQuery(dbname, queryId, day)
{
	_qsMultiDbMode    = false;
	_qsMultiDbAllRows = null;
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
	// In multi-db mode, re-rank from cached rows without re-fetching
	if (_qsMultiDbMode && _qsMultiDbAllRows) {
		var rankBy    = $('#query-store-rankby').val()          || 'duration';
		var topN      = parseInt($('#query-store-topn').val())  || 25;
		var aggregate = _qsMultiDbSchemaFlags && _qsMultiDbSchemaFlags._aggregate !== false;
		_qsMergeAndRender(_qsMultiDbAllRows, _qsMultiDbSchemaFlags || {}, rankBy, topN, aggregate);
		return;
	}

	var srv    = _qsSrvName;
	var dbname = _qsDbname;
	if (!srv || !dbname) return;

	var rankBy  = $('#query-store-rankby').val()  || 'duration';
	var topN    = $('#query-store-topn').val()     || '25';
	var minExec = $('#query-store-minexec').val()  || '2';
	var ts      = _qsTimestamp || '';

	var params = { srv: srv, action: 'topQueries', dbname: dbname,
	               rankBy: rankBy, topN: topN, minExecutions: minExec };
	if (ts) params.ts = ts;

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

	var rankBy     = r.rankBy             || 'duration';
	var hasMemory  = r.hasMemoryCols      === true;
	var has2017    = r.has2017Cols        === true;
	var hasCompile = r.hasCompileDuration === true;
	var isMultiDb  = r._multiDb           === true;
	var showDay    = isMultiDb && r._aggregate === false;

	// ── Build column list dynamically based on server capabilities ────────────
	// Columns are grouped by metric — each metric's avg/max/total together for easy scanning.
	var cols   = ['queryId','objectContext','querySqlTextPreview','planCount','totalExecutions','_efficiencyPct'];
	var labels = ['Query ID','Object / Context','SQL Preview','Plans','Executions','CPU Eff %'];
	// Duration group
	cols.push('avgDurationUs','maxDurationUs','totalDurationUs');
	labels.push('Avg Elapsed (ms)','Max Elapsed (ms)','Total Elapsed (ms)');
	// CPU group
	cols.push('avgCpuUs','totalCpuUs');
	labels.push('Avg CPU (ms)','Total CPU (ms)');
	// Logical reads group
	cols.push('avgLogicalReads','maxLogicalReads','totalLogicalReads');
	labels.push('Avg Reads','Max Reads','Total Reads');
	// Physical reads group
	cols.push('avgPhysicalReads','totalPhysicalReads');
	labels.push('Avg Phys Reads','Total Phys Reads');
	// Logical writes group
	cols.push('avgLogicalWrites','totalLogicalWrites');
	labels.push('Avg Writes','Total Writes');
	// Rows group
	cols.push('avgRowcount','totalRowcount');
	labels.push('Avg Rows','Total Rows');
	// Optional groups — pushed in metric order, avg+total together
	if (hasMemory) {
		cols.push('avgMemoryMb','totalMemoryMb');
		labels.push('Avg Mem (MB)','Total Mem (MB)');
	}
	if (has2017) {
		cols.push('avgDop');
		labels.push('Avg DOP');
		cols.push('avgTempdbMb','totalTempdbMb');
		labels.push('Avg Tempdb (MB)','Total Tempdb (MB)');
		cols.push('avgLogMb','totalLogMb');
		labels.push('Avg Log (MB)','Total Log (MB)');
	} else if (r.schemaHasDopCol === true) {
		cols.push('avgDop');
		labels.push('Avg DOP');
	}
	if (hasCompile) {
		cols.push('avgCompileDurationUs','countCompiles');
		labels.push('Avg Compile (ms)','# Compiles');
	}
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
		firstSeen:             'Timestamp when this query was first recorded in the Query Store',
		lastSeen:              'Timestamp of the most recent execution recorded',
		querySqlTextPreview:   'First 90 characters of the query SQL text — hover for full text'
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
	else if (rankBy === 'compile')         { rankColSet['avgCompileDurationUs'] = true; }

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
		avgDop:1
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
				var preview = (val || '').substring(0, 90) + ((val || '').length > 90 ? '…' : '');
				html += '<td style="font-family:monospace;font-size:0.88em;max-width:300px;overflow:hidden;text-overflow:ellipsis;"'
				      + ' title="' + escHtml(val || '') + '">' + escHtml(preview) + '</td>';
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

	// Chart container goes above the table — inject placeholder then populate after DOM insertion
	var chartHtml = '<div id="qs-tq-chart-container" style="height:230px;margin-bottom:10px;"></div>';
	$('#query-store-content').html(chartHtml + html);

	_qsRenderChart(queries, rankBy);
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
		case 'cpu':            metricKey = 'totalCpuUs';            metricLabel = 'Total CPU (ms)';        break;
		case 'reads':          metricKey = 'totalLogicalReads';     metricLabel = 'Total Logical Reads';   break;
		case 'physical_reads': metricKey = 'totalPhysicalReads';    metricLabel = 'Total Phys Reads';      break;
		case 'writes':         metricKey = 'totalLogicalWrites';    metricLabel = 'Total Logical Writes';  break;
		case 'executions':     metricKey = 'totalExecutions';       metricLabel = 'Total Executions';      break;
		case 'rowcount':       metricKey = 'totalRowcount';         metricLabel = 'Total Rows';            break;
		case 'memory':         metricKey = 'totalMemoryMb';         metricLabel = 'Total Memory (MB)';     break;
		case 'tempdb':         metricKey = 'totalTempdbMb';         metricLabel = 'Total Tempdb (MB)';     break;
		case 'log':            metricKey = 'totalLogMb';            metricLabel = 'Total Log (MB)';        break;
		case 'compile':        metricKey = 'avgCompileDurationUs'; metricLabel = 'Avg Compile (ms)';    break;
		default:               metricKey = 'totalDurationUs';       metricLabel = 'Total Elapsed (ms)';   break;
	}

	// Limit to top 20 bars for readability
	var chartData = queries.slice(0, 20);
	var barLabels = chartData.map(function(q) {
		var sql = (q.querySqlTextPreview || '').replace(/\s+/g, ' ').substring(0, 38);
		return 'Q' + q.queryId + (sql ? ': ' + sql : '');
	});
	var barValues = chartData.map(function(q) {
		var v = q[metricKey];
		return (v != null && !isNaN(v)) ? Number(v) : 0;
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
				xAxes: [{ ticks: { beginAtZero: true, fontSize: 10 } }],
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
						var fmt = _qsFmtNum(v) || _qsFmt1Dec(v) || String(v);
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
	var ts = _qsTimestamp || '';

	var params = { srv: srv, action: 'queryDetail', dbname: dbname, queryId: queryId, includePlan: includePlan };
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
				queryStoreRenderDetail(r);
			} catch(ex) { queryStoreShowMsg('Parse error: ' + ex); }
		},
		error: function(xhr) {
			$('#query-store-loading').hide();
			queryStoreShowHttpError(xhr, 'queryDetail');
		}
	});
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
		// _nms: millisecond value — render with h/m/s tooltip when >= 1 s
		var _nms = function(ms, dec) {
			if (ms == null) return '—';
			var disp = (dec != null && typeof ms === 'number') ? ms.toFixed(dec) : (_qsFmtNum(ms) || ms);
			var tip  = ms >= 1000 ? _qsUsToHms(ms * 1000) : '';
			return tip ? '<span title="' + escHtml(tip) + '">' + disp + '</span>' : String(disp);
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
		      + _th('CPU Eff %',        _effTip)
		      // Duration group
		      + _th('Avg Elapsed (ms)', 'Average wall-clock elapsed time per execution, in milliseconds')
		      + _th('Min Elapsed (ms)', 'Shortest single execution observed, in milliseconds')
		      + _th('Max Elapsed (ms)', 'Longest single execution observed, in milliseconds')
		      + _th('Tot Elapsed (ms)', 'Total elapsed time summed across ALL executions for this plan, in milliseconds')
		      // CPU group
		      + _th('Avg CPU (ms)',     'Average CPU time per execution, in milliseconds')
		      + _th('Max CPU (ms)',     'Peak CPU time for a single execution, in milliseconds')
		      + _th('Tot CPU (ms)',     'Total CPU time summed across ALL executions for this plan, in milliseconds')
		      // Logical reads group
		      + _th('Avg Reads',        'Average logical (buffer pool) page reads per execution (1 page = 8 KB)')
		      + _th('Max Reads',        'Peak logical reads in a single execution (1 page = 8 KB)')
		      + _th('Tot Reads',        'Total logical page reads across ALL executions for this plan (1 page = 8 KB)')
		      // Physical reads group
		      + _th('Avg Phys Reads',   'Average physical disk page reads per execution (1 page = 8 KB; high → cache misses)')
		      + _th('Max Phys Reads',   'Peak physical disk page reads observed in a single execution (1 page = 8 KB)')
		      + _th('Tot Phys Reads',   'Total physical disk page reads across ALL executions for this plan (1 page = 8 KB)')
		      // Logical writes group
		      + _th('Avg Writes',       'Average logical page writes per execution (1 page = 8 KB)')
		      + _th('Max Writes',       'Peak logical page writes observed in a single execution (1 page = 8 KB)')
		      + _th('Tot Writes',       'Total logical page writes across ALL executions for this plan (1 page = 8 KB)')
		      // Rows group
		      + _th('Avg Rows',         'Average number of rows returned/affected per execution')
		      + _th('Tot Rows',         'Total rows returned or affected across ALL executions for this plan')
		      // Optional groups (avg+total together per metric)
		      + (_hasMemory ? _th('Avg Mem',    'Average memory grant used per execution, in MB (8-KB pages ÷ 128)') : '')
		      + (_hasMemory ? _th('Tot Mem',    'Total memory grant across ALL executions for this plan, in MB') : '')
		      + (_hasDop    ? _th('Avg DOP',    'Average degree of parallelism used per execution (SQL Server 2016+)') : '')
		      + (_has2017   ? _th('Avg Tempdb', 'Average tempdb space used per execution in MB (SQL Server 2017+)') : '')
		      + (_has2017   ? _th('Avg Log',    'Average transaction log generated per execution in MB (SQL Server 2017+)') : '')
		      + _th('First seen', 'When this plan was first recorded in the Query Store')
		      + _th('Last seen',  'When this plan was most recently recorded');
		html += '</tr></thead><tbody>';
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
			html += '<tr>'
			      + planBtnCell
			      + '<td style="color:#0066cc;font-weight:600;">' + (p.planId != null ? p.planId : '—') + '</td>'
			      + '<td>' + (p.isForcedPlan ? '<span style="color:darkorange;">✓ forced</span>' : '—') + '</td>'
			      + '<td>' + escHtml(p.planForcingType || '—') + '</td>'
			      + '<td style="text-align:right;">' + _n(p.totalExecutions)     + '</td>'
			      + _effCell(p.avgCpuUs, p.avgDop, p.avgDurationUs)
			      // Duration group
			      + '<td style="text-align:right;">' + _nus(p.avgDurationUs)     + '</td>'
			      + '<td style="text-align:right;">' + _nus(p.minDurationUs)     + '</td>'
			      + '<td style="text-align:right;">' + _nus(p.maxDurationUs)     + '</td>'
			      + '<td style="text-align:right;">' + _nus(p.totalDurationUs)   + '</td>'
			      // CPU group
			      + '<td style="text-align:right;">' + _nus(p.avgCpuUs)          + '</td>'
			      + '<td style="text-align:right;">' + _nus(p.maxCpuUs)          + '</td>'
			      + '<td style="text-align:right;">' + _nus(p.totalCpuUs)        + '</td>'
			      // Logical reads group
			      + '<td style="text-align:right;">' + _nrd(p.avgLogicalReads)   + '</td>'
			      + '<td style="text-align:right;">' + _nrd(p.maxLogicalReads)   + '</td>'
			      + '<td style="text-align:right;">' + _nrd(p.totalLogicalReads) + '</td>'
			      // Physical reads group
			      + '<td style="text-align:right;">' + _nrd(p.avgPhysicalReads)  + '</td>'
			      + '<td style="text-align:right;">' + _nrd(p.maxPhysicalReads)  + '</td>'
			      + '<td style="text-align:right;">' + _nrd(p.totalPhysicalReads)+ '</td>'
			      // Logical writes group
			      + '<td style="text-align:right;">' + _nrd(p.avgLogicalWrites)  + '</td>'
			      + '<td style="text-align:right;">' + _nrd(p.maxLogicalWrites)  + '</td>'
			      + '<td style="text-align:right;">' + _nrd(p.totalLogicalWrites)+ '</td>'
			      // Rows group
			      + '<td style="text-align:right;">' + _n(p.avgRowcount)         + '</td>'
			      + '<td style="text-align:right;">' + _n(p.totalRowcount)       + '</td>'
			      // Optional groups
			      + (_hasMemory ? '<td style="text-align:right;">' + _nmb(p.avgMemoryMb)  + '</td>' : '')
			      + (_hasMemory ? '<td style="text-align:right;">' + _nmb(p.totalMemoryMb)+ '</td>' : '')
			      + (_hasDop    ? '<td style="text-align:right;">' + _n(p.avgDop)         + '</td>' : '')
			      + (_has2017   ? '<td style="text-align:right;">' + _nmb(p.avgTempdbMb)  + '</td>' : '')
			      + (_has2017   ? '<td style="text-align:right;">' + _nmb(p.avgLogMb)     + '</td>' : '')
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
		      + _th('Wait category', 'SQL Server wait type category (e.g. CPU, Lock, I/O, Network)')
		      + _th('Total wait (ms)', 'Sum of wait time in milliseconds across all executions in this period')
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
			html += '<tr>'
			      + '<td>' + escHtml(w.waitCategory || '—') + '</td>'
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
		showSqlServerShowplanDialog(entry.xml, entry.sqlText, entry.objectName);
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
					source:               r.source
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
