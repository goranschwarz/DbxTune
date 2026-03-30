/** dbxGraphCounterDetails.js — Counter Details panel for graph.html */
// CM Detail Panel state
var _cmSrvName      = null;
var _cmTimestamp    = null;
var _cmGroup        = null;
var _cmName         = null;
var _cmType         = 'rate';
var _cmHideZero     = false;  // hide rows where all diff/rate values are 0
var _cmListData     = null;
var _cmCurrentData  = null;   // last fetched data result (for re-filtering without re-fetching)
var _cmFilter       = '';     // current filter string
var _cmTrendCache   = null;  // cached /api/graphs response (all graphs for this server)
var _cmSort         = { col: -1, stage: 0 }; // sort state for current CM: stage 0=original 1=desc 2=asc
var _cmSortMap      = {};                    // per-CM sort state, keyed by cmName — survives tab switching
var _cmFilterMap    = {};                    // per-CM filter text (session only, not persisted to localStorage)
var _cmDetailClickRows = null;               // {columns, tooltips, rows} — row store for click-to-detail modal
// Auto-open restore: fire once on the first live/slider tick after page load
var _cmAutoOpenChecked = false;
// Sticky "last viewed" — persisted to localStorage so re-opening after reload returns to the same tab
var _cmLastGroup    = (function(){ try { return localStorage.getItem('cmDetail-lastGroup') || null; } catch(e) { return null; } }());
var _cmLastCm       = (function(){ try { return localStorage.getItem('cmDetail-lastCm')    || null; } catch(e) { return null; } }());

//-----------------------------------------------------------
// CM DETAIL PANEL
//-----------------------------------------------------------

function cmDetailLoadList(srvName, timestamp)
{
	if (!srvName || !timestamp) return;

	// Normalize timestamp to "yyyy-MM-dd HH:mm:ss" — handles ISO strings, JS Date strings, moment objects
	timestamp = moment(timestamp).format("YYYY-MM-DD HH:mm:ss");

	_cmSrvName   = srvName;
	_cmTimestamp = timestamp;

	$('#cm-detail-panel').css('display', 'flex');
	$('#cm-detail-srv').text('[' + srvName + ']');
	$('#cm-detail-ts').text('@ ' + timestamp);
	$('#cm-detail-loading').show();
	$('#cm-detail-table').html('');

	$.ajax({
		url: '/api/cc/mgt/cm/list',
		data: { srv: srvName, time: timestamp },
		dataType: 'text',
		success: function(data) {
			$('#cm-detail-loading').hide();
			try {
				var r = JSON.parse(data);
				if (r.error) { cmDetailShowMsg(r.message || r.error); return; }
				_cmListData = r;
				// Use the exact SessionSampleTime the server resolved (ms-precision match from MonSessionSampleDetailes)
				if (r.resolvedTime) { _cmTimestamp = r.resolvedTime.substring(0, 19); $('#cm-detail-ts').text('@ ' + _cmTimestamp); }
				// Pre-fetch trend graph cache (async, non-blocking)
				_cmTrendEnsureCache(function() { cmTrendGraphsUpdateButton(); });
				cmDetailRenderGroups(r.groups);
			} catch(ex) { cmDetailShowMsg('Parse error: ' + ex); }
		},
		error: function(xhr) {
			$('#cm-detail-loading').hide();
			cmDetailShowMsg('HTTP ' + xhr.status + ': ' + xhr.responseText);
		}
	});
}

function cmDetailRenderGroups(groups)
{
	var $gt = $('#cm-group-tabs').empty();
	if (!groups || !groups.length) { cmDetailShowMsg('No groups'); return; }

	// Auto-select: prefer last viewed group (if still present), otherwise first non-Other group with data
	var groupNames = groups.map(function(g) { return g.groupName; });
	var selGroup;
	if (_cmLastGroup && groupNames.indexOf(_cmLastGroup) >= 0) {
		selGroup = _cmLastGroup;
	} else {
		// First pass: first group with data that isn't 'Other'
		selGroup = null;
		for (var i = 0; i < groups.length; i++) {
			if (groups[i].groupName !== 'Other' && groups[i].cms.some(function(c) { return c.hasData; })) {
				selGroup = groups[i].groupName; break;
			}
		}
		// Second pass: allow 'Other' if nothing else has data
		if (!selGroup) {
			for (var i = 0; i < groups.length; i++) {
				if (groups[i].cms.some(function(c) { return c.hasData; })) { selGroup = groups[i].groupName; break; }
			}
		}
		if (!selGroup) selGroup = groups[0].groupName;
	}

	groups.forEach(function(g) {
		var hasAny = g.cms.some(function(c) { return c.hasData; });
		var li = $('<li class="nav-item ' + (hasAny ? 'cm-tab-has-data' : 'cm-tab-no-data') + '"></li>').attr('data-group-name', g.groupName);
		var a  = $('<a class="nav-link' + (g.groupName === selGroup ? ' active' : '') + '" href="#"></a>');
		if (g.groupIcon)
			a.append(cmMakeIcon(g.groupIcon));
		a.append(document.createTextNode(g.groupName));
		a.on('click', (function(gn) { return function(e) { e.preventDefault(); cmDetailSelectGroup(gn); }; })(g.groupName));
		li.append(a);
		$gt.append(li);
	});

	_cmGroup = selGroup;
	_cmLastGroup = selGroup;
	try { localStorage.setItem('cmDetail-lastGroup', selGroup); } catch(e) {}
	var selGroupData = null;
	for (var i = 0; i < groups.length; i++) {
		if (groups[i].groupName === selGroup) { selGroupData = groups[i]; break; }
	}
	if (selGroupData) cmDetailRenderCms(selGroupData.cms);
}

/**
 * Load and apply all per-CM preferences (type, hide-zero, filter) when
 * switching to a new CM.  Updates globals + DOM controls so the subsequent
 * cmDetailLoadData call picks up the right type and the rendered table uses
 * the right filter/hide-zero state.
 */
function _cmApplyPrefsForCm(cmName)
{
	// Sort: in-memory map first (survives slider ticks), then localStorage (survives page reload)
	_cmSort = _cmSortMap[cmName] || { col: -1, stage: 0 };
	if (_cmSort.col < 0) {
		try {
			var ss = JSON.parse(localStorage.getItem('cmDetail-sort-' + cmName) || 'null');
			if (ss && ss.col >= 0 && ss.stage > 0) {
				_cmSort = { col: ss.col, stage: ss.stage };
				_cmSortMap[cmName] = { col: _cmSort.col, stage: _cmSort.stage }; // warm the map
			}
		} catch(e) {}
	}

	// Abs/Diff/Rate — persisted per CM in localStorage, default 'rate'
	var savedType = null;
	try { savedType = localStorage.getItem('cmDetail-type-' + cmName); } catch(e) {}
	_cmType = savedType || 'rate';
	$('#cm-detail-type-' + _cmType).prop('checked', true);
	// Re-enable Diff/Rate buttons — the data response will disable them again if this CM has no diff columns.
	// Without this, the disabled state from a previous abs-only CM bleeds into the next CM.
	$('#cm-detail-type-diff').prop('disabled', false).attr('title', 'Delta values \u2014 the difference between this sample and the previous one. Displayed in blue.');
	$('#cm-detail-type-rate').prop('disabled', false).attr('title', 'Rate per second \u2014 the delta divided by the elapsed time between samples, giving a per-second rate. Displayed in blue.');

	// Hide Unchanged — persisted per CM in localStorage, default false
	var savedHz = null;
	try { savedHz = localStorage.getItem('cmDetail-hideZero-' + cmName); } catch(e) {}
	_cmHideZero = savedHz === '1';
	$('#cm-detail-hide-zero').prop('checked', _cmHideZero);

	// Filter text — session-only per CM (not in localStorage)
	_cmFilter = _cmFilterMap[cmName] || '';
	$('#cm-detail-filter-input').val(_cmFilter);
}

function cmDetailRenderCms(cms)
{
	var $ct = $('#cm-name-tabs').empty();
	if (!cms || !cms.length) { cmDetailShowMsg('No CMs in this group'); return; }

	// Auto-select: prefer last viewed CM (if present in this group), otherwise first CM with data
	var cmNames = cms.map(function(c) { return c.cmName; });
	var selCm = (_cmLastCm && cmNames.indexOf(_cmLastCm) >= 0)
		? _cmLastCm
		: cms[0].cmName;
	if (selCm === cms[0].cmName && !(_cmLastCm && cmNames.indexOf(_cmLastCm) >= 0)) {
		// No last-CM match — fall back to first CM with data
		for (var i = 0; i < cms.length; i++) {
			if (cms[i].hasData) { selCm = cms[i].cmName; break; }
		}
	}

	cms.forEach(function(c) {
		var li = $('<li class="nav-item ' + (c.hasData ? 'cm-tab-has-data' : 'cm-tab-no-data') + '"></li>').attr('data-cm-name', c.cmName);
		var a  = $('<a class="nav-link' + (c.cmName === selCm ? ' active' : '') + '" href="#"></a>');
		if (c.iconFile)
			a.append(cmMakeIcon(c.iconFile));
		a.append(document.createTextNode(c.displayName));
		a.on('click', (function(cn) { return function(e) { e.preventDefault(); cmDetailSelectCm(cn); }; })(c.cmName));
		// Right-click → context menu with "Properties…"
		a.on('contextmenu', (function(cn) { return function(e) { e.preventDefault(); _cmShowContextMenu(e, cn); }; })(c.cmName));
		li.append(a);
		$ct.append(li);
	});

	if (_cmName && _cmName !== selCm) {
		_cmSortMap[_cmName]  = _cmSort;   // save sort for previous CM
		_cmFilterMap[_cmName] = _cmFilter; // save filter for previous CM
	}
	_cmApplyPrefsForCm(selCm);  // restore sort / type / hide-zero / filter
	_cmName  = selCm;
	_cmLastCm = selCm;
	try { localStorage.setItem('cmDetail-lastCm', selCm); } catch(e) {}
	cmTrendGraphsUpdateButton();
	cmDetailLoadData(_cmSrvName, selCm, _cmTimestamp, _cmType);
}

function cmDetailSelectGroup(groupName)
{
	_cmGroup = groupName;
	_cmLastGroup = groupName;
	try { localStorage.setItem('cmDetail-lastGroup', groupName); } catch(e) {}
	$('#cm-group-tabs .nav-link').removeClass('active');
	$('#cm-group-tabs li[data-group-name="' + groupName + '"] .nav-link').addClass('active');
	if (!_cmListData) return;
	var g = null;
	for (var i = 0; i < _cmListData.groups.length; i++) {
		if (_cmListData.groups[i].groupName === groupName) { g = _cmListData.groups[i]; break; }
	}
	if (g) cmDetailRenderCms(g.cms);
}

function cmDetailSelectCm(cmName)
{
	if (_cmName !== cmName) {
		if (_cmName) {
			_cmSortMap[_cmName]  = _cmSort;   // save sort for the tab we're leaving
			_cmFilterMap[_cmName] = _cmFilter; // save filter for the tab we're leaving
		}
		_cmApplyPrefsForCm(cmName);  // restore sort / type / hide-zero / filter
	}
	_cmName = cmName;
	_cmLastCm = cmName;
	try { localStorage.setItem('cmDetail-lastCm', cmName); } catch(e) {}
	// Toggle active class only — no need to re-render the whole tab list
	$('#cm-name-tabs .nav-link').removeClass('active');
	$('#cm-name-tabs li[data-cm-name="' + cmName + '"] .nav-link').addClass('active');
	cmTrendGraphsUpdateButton();
	cmDetailLoadData(_cmSrvName, cmName, _cmTimestamp, _cmType);
}

function cmDetailTypeChanged(type)
{
	_cmType = type;
	// Persist per-CM so switching tabs and reloading restores the right type
	if (_cmName) try { localStorage.setItem('cmDetail-type-' + _cmName, type); } catch(e) {}
	if (_cmSrvName && _cmName && _cmTimestamp)
		cmDetailLoadData(_cmSrvName, _cmName, _cmTimestamp, _cmType);
}

// Helper: build a CM tab icon <img> element.
// The server returns a default icon for missing files (no 404), so no
// client-side fallback logic is needed. Browser caches for 24 hours.
function cmMakeIcon(iconFile)
{
	var src = '/api/cc/mgt/cm/icon?file=' + encodeURIComponent(iconFile);
	return $('<img>').attr({ src: src, width: 16, height: 16 })
		.css({ verticalAlign: 'middle', marginRight: '3px', marginBottom: '2px' });
}

// Helper: find the CmSampleInfo object for cmName in the last loaded list
function cmDetailFindCmInfo(cmName)
{
	if (!_cmListData || !_cmListData.groups) return null;
	for (var gi = 0; gi < _cmListData.groups.length; gi++) {
		var cms = _cmListData.groups[gi].cms;
		for (var ci = 0; ci < cms.length; ci++) {
			if (cms[ci].cmName === cmName) return cms[ci];
		}
	}
	return null;
}

function cmDetailLoadData(srvName, cmName, timestamp, type)
{
	if (!srvName || !cmName || !timestamp) return;

	// Clear previous charts immediately — prevents stale charts from a previous CM
	// lingering while the new CM's data is loading (or if it has no charts/data).
	_cmChartInstances.forEach(function(c) { try { c.destroy(); } catch(e) {} });
	_cmChartInstances = [];
	$('#cm-detail-charts').empty().hide();
	// Reset table-wrap to full width (no chart pane)
	$('#cm-detail-table-wrap').css({ flex: '1 1 100%', order: '' });
	$('#cm-detail-content').css({ flexDirection: 'row' });

	// Check collector status for this CM at this sample time
	var cmInfo = cmDetailFindCmInfo(cmName);
	if (cmInfo && cmInfo.exceptionMsg) {
		// Collector threw an exception — show warning, no point querying CmDataServlet
		$('#cm-detail-loading').hide();
		var full = cmInfo.exceptionFullText || '';
		var html = '<div class="alert alert-warning mt-2" style="font-size:0.85em;">'
			+ '<strong>&#9888; Collector problem at sample time</strong><br>'
			+ $('<span>').text(cmInfo.exceptionMsg).html();
		if (full) {
			html += '<br><details style="margin-top:4px"><summary style="cursor:pointer;">Full stack trace</summary>'
				+ '<pre style="font-size:0.8em;white-space:pre-wrap;margin-top:4px;">'
				+ $('<span>').text(full).html()
				+ '</pre></details>';
		}
		html += '</div>';
		$('#cm-detail-table').html(html);
		return;
	}
	if (cmInfo && !cmInfo.hasData) {
		// CM exists but saved no rows this sample (postponed, or nothing matched filter)
		$('#cm-detail-loading').hide();
		$('#cm-detail-table').html('<div class="alert alert-secondary mt-2" style="font-size:0.85em;">'
			+ 'No data collected for <strong>' + $('<span>').text(cmName).html() + '</strong>'
			+ ' at sample time <strong>' + $('<span>').text(timestamp).html() + '</strong>.'
			+ '</div>');
		return;
	}

	$('#cm-detail-loading').show();
	$('#cm-detail-table').html('');
	// Show spinner after 100ms (avoids flicker on fast responses)
	var busyTimer = setTimeout(function() {
		document.getElementById('dbx-history-get-data-bussy').style.visibility = 'visible';
	}, 100);
	$.ajax({
		url: '/api/cc/mgt/cm/data',
		data: { srv: srvName, cm: cmName, time: timestamp, type: type },
		dataType: 'text',
		success: function(data) {
			clearTimeout(busyTimer);
			document.getElementById('dbx-history-get-data-bussy').style.visibility = 'hidden';
			$('#cm-detail-loading').hide();
			try {
				var r = JSON.parse(data);

				// --- Auto-switch to ABS when diff/rate table doesn't exist ---
				// HostMonitor CMs (iostat, vmstat, etc.) and other ABS-only CMs don't
				// have _diff/_rate tables.  When we request type=diff/rate and get
				// "table-not-found", silently switch to ABS instead of showing an error.
				// Also handle "no-data-in-window" for diff/rate: the table may exist
				// but have no rows (e.g. first sample after startup — diff needs 2 samples).
				if (r.error && type !== 'abs') {
					var isTableMissing = (r.error === 'table-not-found');
					var isNoData       = (r.error === 'no-data-in-window');
					if (isTableMissing || isNoData) {
						_cmType = 'abs';
						// Persist only when the table is structurally missing (not transient "no data")
						if (isTableMissing)
							try { localStorage.setItem('cmDetail-type-' + cmName, 'abs'); } catch(e) {}
						$('#cm-detail-type-abs').prop('checked', true);
						if (isTableMissing)
							$('#cm-detail-type-diff, #cm-detail-type-rate')
								.prop('disabled', true)
								.attr('title', 'No Diff/Rate data for this CM');
						cmDetailLoadData(srvName, cmName, timestamp, 'abs');
						return;
					}
				}
				if (r.error) { cmDetailShowMsg(r.message || r.error); return; }

				// Case 3: data returned but no diff columns in schema
				var hasNoDiffCols = (!r.diffColumns || r.diffColumns.length === 0);
				var hasNoDiffData = (r.rowCount === 0 || hasNoDiffCols);
				$('#cm-detail-type-diff, #cm-detail-type-rate')
					.prop('disabled', hasNoDiffCols)
					.attr('title', hasNoDiffCols ? 'No Diff/Rate data for this CM' : '');
				if (!hasNoDiffCols) {
					$('#cm-detail-type-diff').attr('title', 'Delta values \u2014 the difference between this sample and the previous one. Displayed in blue.');
					$('#cm-detail-type-rate').attr('title', 'Rate per second \u2014 the delta divided by the elapsed time between samples, giving a per-second rate. Displayed in blue.');
				}
				if (type !== 'abs' && hasNoDiffData) {
					_cmType = 'abs';
					if (hasNoDiffCols)
						try { localStorage.setItem('cmDetail-type-' + cmName, 'abs'); } catch(e) {}
					$('#cm-detail-type-abs').prop('checked', true);
					cmDetailLoadData(srvName, cmName, timestamp, 'abs');
					return;
				}
				r._effectiveType = type;
				$('#cm-detail-type-' + type).prop('checked', true);
				cmDetailRenderTable(r);
			} catch(ex) { cmDetailShowMsg('Parse error: ' + ex); }
		},
		error: function(xhr) {
			clearTimeout(busyTimer);
			document.getElementById('dbx-history-get-data-bussy').style.visibility = 'hidden';
			$('#cm-detail-loading').hide();
			cmDetailShowMsg('HTTP ' + xhr.status);
		}
	});
}

function cmDetailRenderTable(r)
{
	_cmCurrentData = r;
	$('#cm-detail-type-bar').css('display', 'flex');
	$('#cm-detail-filter-bar').css('display', 'flex');
	_cmFilter = $('#cm-detail-filter-input').val() || '';
	cmDetailRenderFiltered(r, _cmFilter);
}

function cmDetailApplyFilter(filter)
{
	_cmFilter = filter;
	// Remember filter per-CM for this session (not persisted to localStorage)
	if (_cmName) _cmFilterMap[_cmName] = filter;
	// Persist hide-zero per-CM whenever the filter bar changes
	if (_cmName) {
		var hz = document.getElementById('cm-detail-hide-zero');
		try { localStorage.setItem('cmDetail-hideZero-' + _cmName, (hz && hz.checked) ? '1' : '0'); } catch(e) {}
	}
	if (!_cmCurrentData) return;
	cmDetailRenderFiltered(_cmCurrentData, filter);
}

function cmDetailRenderFiltered(r, filter)
{
	var rows = r.rows || [];
	var result = applyRowFilter(rows, r.columns, filter);
	var filteredRows = result.filteredRows;
	var filterError  = result.filterError;

	// Hide-zero filter: remove rows where every diff/rate column is 0 or null
	_cmHideZero = document.getElementById('cm-detail-hide-zero') ? document.getElementById('cm-detail-hide-zero').checked : false;
	filter = (filter || '').trim();

	// Apply column sort (stage 1=desc, stage 2=asc, stage 0=original order)
	if (_cmSort.col >= 0 && _cmSort.stage > 0) {
		var sc   = _cmSort.col;
		var desc = _cmSort.stage === 1;
		filteredRows = filteredRows.slice().sort(function(a, b) {
			var va = a[sc], vb = b[sc];
			if (va === null || va === undefined) va = '';
			if (vb === null || vb === undefined) vb = '';
			var na = Number(va), nb = Number(vb);
			if (!isNaN(na) && !isNaN(nb)) return desc ? nb - na : na - nb;
			var sa = String(va).toLowerCase(), sb = String(vb).toLowerCase();
			return desc ? (sa > sb ? -1 : sa < sb ? 1 : 0) : (sa < sb ? -1 : sa > sb ? 1 : 0);
		});
	}

	// Build diff-column and pct-column name sets (case-insensitive) for colouring
	var diffSet = {};
	if (r.diffColumns) r.diffColumns.forEach(function(dc) { diffSet[dc.toLowerCase()] = true; });
	var pctSet = {};
	if (r.pctColumns) r.pctColumns.forEach(function(pc) { pctSet[pc.toLowerCase()] = true; });

	// Per-column flags
	var isNumeric     = r.isNumeric || [];
	var isDiff        = r.columns ? r.columns.map(function(c) { return !!diffSet[c.toLowerCase()]; }) : [];
	var isPct         = r.columns ? r.columns.map(function(c) { return !!pctSet[c.toLowerCase()]; }) : [];
	var effectiveType = r._effectiveType || _cmType;  // may be auto-switched to abs for ABS-only CMs

	// Apply hide-zero: drop rows where ALL diff/rate columns are 0 or null
	if (_cmHideZero && effectiveType !== 'abs') {
		var anyDiff = isDiff.some(function(d) { return d; });
		if (anyDiff) {
			filteredRows = filteredRows.filter(function(row) {
				return isDiff.some(function(d, i) {
					if (!d) return false;
					var nv = parseFloat(row[i]);
					return !isNaN(nv) && nv !== 0;
				});
			});
		}
	}

	// Row count — computed AFTER hide-zero so the displayed number matches what's visible
	var countText = filterError
		? filterError
		: (filter || _cmHideZero)
			? (filteredRows.length + ' / ' + rows.length + ' rows')
			: (rows.length + ' rows');
	$('#cm-detail-filter-count').text(countText);
	$('#cm-detail-row-count').text(countText);

	// Info bar: show cmSampleTime and cmSampleMs alongside resolved time and row count
	var sampleInfo = '';
	if (r.cmSampleTime) sampleInfo += '&nbsp; SampleTime: <b>' + escHtml(r.cmSampleTime) + '</b>';
	if (r.cmSampleMs  ) sampleInfo += '&nbsp; SampleMs: <b>'   + escHtml(String(r.cmSampleMs)) + '</b>';

	var h = '<p style="color:#6c757d;font-size:0.8em;margin:2px 0;">'
		+ 'CM: <b>' + escHtml(r.cmName) + '</b>'
		+ '&nbsp; Type: <b>' + escHtml(r.type) + '</b>'
		+ sampleInfo
		+ '&nbsp; Rows: <b>' + r.rowCount + '</b></p>'
		+ '<table class="table table-sm table-bordered table-hover" style="font-size:0.78em;white-space:nowrap;width:auto;">'
		+ '<thead class="thead-light"><tr>'
		+ r.columns.map(function(c, i) {
			var indicator = (_cmSort.col === i) ? (_cmSort.stage === 1 ? ' &#9660;' : ' &#9650;') : '';
			var tt = (r.tooltips && r.tooltips[i]) ? r.tooltips[i] : '';
			var ttAttr = '';
			if (tt) {
				// Some tooltips are wrapped in <html>...</html> -- strip the outer wrapper so
				// inner HTML (tables, bold, line-breaks etc.) renders correctly in the tooltip.
				var ttHtml = tt.replace(/^\s*<html>\s*/i, '').replace(/\s*<\/html>\s*$/i, '').trim();
				// Bootstrap tooltip with html:true renders markup instead of raw text.
				ttAttr = ' data-toggle="tooltip" data-html="true" data-placement="bottom" title="'
					+ ttHtml.replace(/"/g, '&quot;') + '"';
			}
			var thStyle = 'cursor:pointer;user-select:none;';
			if (isPct[i])                              thStyle += 'color:#cc0000;';
			else if (isDiff[i] && effectiveType !== 'abs') thStyle += 'color:#0066cc;';
			return '<th style="' + thStyle + '" onclick="cmDetailSortCol(' + i + ');"' + ttAttr + '>'
				+ escHtml(c) + indicator + '</th>';
		}).join('')
		+ '</tr></thead><tbody>';

	if (!filteredRows || filteredRows.length === 0) {
		h += '<tr><td colspan="' + r.columns.length + '" style="text-align:center;color:#6c757d;">'
			+ (filter ? 'No rows match filter' : 'No rows') + '</td></tr>';
	} else {
		// Store rows for click-to-detail (index stored on <tr data-ri>)
		_cmDetailClickRows = { columns: r.columns, tooltips: r.tooltips || [], rows: filteredRows };
		filteredRows.forEach(function(row, ri) {
			h += '<tr data-ri="' + ri + '" style="cursor:pointer;" title="Click to view full row details">'
				+ row.map(function(v, i) {
				var style = 'style="';
				if (isNumeric[i]) style += 'text-align:right;';
				// PCT columns: red, bold when non-zero, with percent bar — shown even in ABS mode
				// (mirrors Java GUI: HighlighterPctData + discardPctHighlighterOnAbsTable() == false)
				if (isPct[i]) {
					style += 'color:#cc0000;';
					var _pctVal = parseFloat(v);
					if (isNumeric[i] && !isNaN(_pctVal) && _pctVal !== 0) style += 'font-weight:bold;';
					// Background percent bar (0-100%), subtle fill like the Swing PctPainter
					if (isNumeric[i] && !isNaN(_pctVal) && _pctVal > 0) {
						var pctW = Math.min(100, Math.max(0, _pctVal));
						style += 'background:linear-gradient(to right,rgba(220,53,69,0.12) ' + pctW + '%,transparent ' + pctW + '%);';
					}
				} else if (isDiff[i] && effectiveType !== 'abs') {
					style += 'color:#0066cc;';
					// Bold when non-zero numeric (mirrors Active Statements colouring)
					var _numVal = parseFloat(v);
					if (isNumeric[i] && !isNaN(_numVal) && _numVal !== 0) style += 'font-weight:bold;';
				}
				style += '"';
				var cell;
				if (v === null || v === undefined)
					cell = '<span style="color:#aaa">null</span>';
				else if (v === true || v === false)
					cell = '<input type="checkbox"' + (v ? ' checked' : '') + ' onclick="return false;" style="cursor:default;pointer-events:none;">';
				else {
					var s = String(v);
					if (s.length > 60) {
						var plain = s.replace(/<[^>]+>/g, '').trim();
						cell = '<span title="' + escHtml(plain.substring(0, 300)) + '">'
							+ escHtml(plain.substring(0, 60)) + '\u2026</span>';
					} else {
						cell = escHtml(s);
					}
				}
				return '<td ' + style + '>' + cell + '</td>';
			}).join('') + '</tr>';
		});
	}
	h += '</tbody></table>';

	// Restore scroll position from localStorage (persists across tab switches and panel close/reopen)
	// Pattern mirrors Active Statements: save on scroll event, restore after render + setTimeout fallback
	var $body      = $('#cm-detail-body');
	var scrollKey  = 'cmDetail-scroll-' + (r.cmName || 'default');
	var savedTop   = 0, savedLeft = 0;
	try {
		var saved = JSON.parse(localStorage.getItem(scrollKey) || 'null');
		if (saved) { savedTop = saved.top || 0; savedLeft = saved.left || 0; }
	} catch(e) {}

	// Destroy existing tooltip instances before replacing the DOM (prevents orphaned tooltip divs)
	$('#cm-detail-table th[data-toggle="tooltip"]').tooltip('dispose');
	$('#cm-detail-table').html(h);

	// Activate Bootstrap tooltips on the freshly rendered column headers
	$('#cm-detail-table th[data-toggle="tooltip"]').tooltip({ container: '#cm-detail-panel', boundary: 'window' });

	// Row click → detail modal (delegated so it survives re-renders)
	$('#cm-detail-table tbody').off('click', 'tr').on('click', 'tr', function() {
		var ri = parseInt($(this).attr('data-ri'), 10);
		if (isNaN(ri) || !_cmDetailClickRows) return;
		cmDetailRowShowModal(_cmDetailClickRows.columns, _cmDetailClickRows.rows[ri], _cmDetailClickRows.tooltips);
	});

	// Restore immediately (works when content height is already known)
	$body.scrollTop(savedTop).scrollLeft(savedLeft);
	// Also restore after a tick in case the browser resets scroll after DOM update
	setTimeout(function() { $body.scrollTop(savedTop).scrollLeft(savedLeft); }, 1);

	// Render chart(s) if the CM declares any chart descriptors
	cmChartRender(r, filteredRows);
}

/**
 * Show a click-to-detail modal for a Counter Details row.
 * Unpivots the column array + value array into a 2-column table rendered client-side.
 * Column tooltips (descriptions) are shown as a third column when available.
 */
/**
 * Returns true if the string value appears to be an HTML fragment —
 * i.e. it starts with '<' (after leading whitespace) and contains '>'.
 * Used by the row-detail modals to decide whether to render a value as
 * HTML or escape it as plain text.
 */
function looksLikeHtml(s) {
	var t = s.replace(/^\s+/, '');
	return t.charAt(0) === '<' && t.indexOf('>') >= 0;
}

function cmDetailRowShowModal(columns, row, tooltips)
{
	var $modal = $('#dbx-view-alarmView-dialog');
	if ($modal.length === 0) return;

	var topZ = (window._dbxTopZ || 910) + 200;
	$modal.css('z-index', topZ);
	setTimeout(function() { $('.modal-backdrop').css('z-index', topZ - 1); }, 30);
	_dbxModalApplyDark($modal);

	$('#dbx-view-alarmView-label').text('Row Detail');
	$('#dbx-view-alarmView-objectName').text('');

	var hasTooltips = tooltips && tooltips.some(function(t) { return t && t.length > 0; });
	var html = '<table class="table table-sm table-bordered" style="font-size:0.85em;word-break:break-word;">'
		+ '<thead class="thead-light"><tr><th style="white-space:nowrap">Column</th><th>Value</th>'
		+ (hasTooltips ? '<th>Description</th>' : '') + '</tr></thead><tbody>';

	columns.forEach(function(col, i) {
		var v = row[i];
		var cellHtml;
		if (v === null || v === undefined)
			cellHtml = '<span style="color:#aaa">null</span>';
		else if (v === true || v === false)
			cellHtml = v ? '<i class="fa fa-check-square-o text-success"></i>'
			             : '<i class="fa fa-square-o text-muted"></i>';
		else {
			var s = String(v);
			if (looksLikeHtml(s))
				cellHtml = '<div style="overflow:auto;">' + s + '</div>';
			else
				cellHtml = '<pre style="white-space:pre-wrap;margin:0;font-size:0.9em;">' + escHtml(s) + '</pre>';
		}

		var ttHtml = '';
		if (hasTooltips && tooltips[i]) {
			ttHtml = '<td style="font-size:0.8em;color:#555;">'
				+ tooltips[i].replace(/^\s*<html>\s*/i,'').replace(/\s*<\/html>\s*$/i,'') + '</td>';
		}

		html += '<tr><td style="white-space:nowrap;font-weight:bold;">' + escHtml(col) + '</td>'
			+ '<td>' + cellHtml + '</td>' + ttHtml + '</tr>';
	});

	html += '</tbody></table>';
	$('#dbx-view-alarmView-content').html(html);
	$modal.modal('show');
}

//-----------------------------------------------------------
// Client-side WHERE filter
// Supports:  col = val   col != val   col <> val
//            col > val   col >= val   col < val   col <= val
//            col LIKE 'pattern'  (% = .*)
/**
 * Shared row-filter utility used by CM Detail and DBMS Config panels.
 * Supports two modes:
 *   "where <expr>"  — SQL-like WHERE evaluated by cmDetailWhereFilter()
 *   anything else   — case-insensitive regex matched against each cell
 *
 * Returns { filteredRows, filterError }.  filteredRows === rows when
 * filter is empty or an error occurs (callers never receive undefined).
 */
function applyRowFilter(rows, columns, filter)
{
	filter = (filter || '').trim();
	if (filter === '') return { filteredRows: rows || [], filterError: '' };

	var filteredRows = rows || [];
	var filterError  = '';

	if (filter.match(/^where\s+/i)) {
		try   { filteredRows = cmDetailWhereFilter(rows, columns, filter.replace(/^where\s+/i, '')); }
		catch (e) { filterError = '(where error: ' + e.message + ')'; filteredRows = rows; }
	} else {
		try {
			var re = new RegExp(filter, 'i');
			filteredRows = (rows || []).filter(function(row) {
				return row.some(function(cell) {
					return re.test(cell === null || cell === undefined ? '' : String(cell));
				});
			});
		} catch (e) { filterError = '(invalid regex)'; filteredRows = rows; }
	}

	return { filteredRows: filteredRows, filterError: filterError };
}

//            col IS NULL   col IS NOT NULL
// Values:    'quoted string'   number   null
// Joining:   AND / OR  (left-to-right evaluation)
//-----------------------------------------------------------
function cmDetailWhereFilter(rows, columns, whereExpr)
{
	// Build column-name → index map (case-insensitive)
	var colIndex = {};
	columns.forEach(function(c, i) { colIndex[c.toLowerCase()] = i; });

	// Tokenise respecting single-quoted strings, then split on AND/OR
	var conditions = cmDetailSplitConditions(whereExpr);

	return rows.filter(function(row) {
		var result = cmDetailEvalCondition(conditions[0].expr, colIndex, row);
		for (var i = 1; i < conditions.length; i++) {
			var right = cmDetailEvalCondition(conditions[i].expr, colIndex, row);
			if (conditions[i].op === 'OR')
				result = result || right;
			else
				result = result && right;
		}
		return result;
	});
}

function cmDetailSplitConditions(expr)
{
	// Split on AND/OR that are NOT inside single quotes
	var parts  = [];
	var ops    = [];
	var cur    = '';
	var inQuote = false;
	var i = 0;
	while (i < expr.length)
	{
		if (!inQuote && expr[i] === "'") { inQuote = true;  cur += expr[i++]; continue; }
		if ( inQuote && expr[i] === "'") { inQuote = false; cur += expr[i++]; continue; }
		if (!inQuote)
		{
			var rest = expr.substring(i);
			var andM = rest.match(/^AND\b/i);
			var orM  = rest.match(/^OR\b/i);
			if (andM) { parts.push(cur.trim()); ops.push('AND'); cur = ''; i += andM[0].length; continue; }
			if (orM)  { parts.push(cur.trim()); ops.push('OR');  cur = ''; i += orM[0].length;  continue; }
		}
		cur += expr[i++];
	}
	if (cur.trim()) parts.push(cur.trim());

	return parts.map(function(p, idx) { return { expr: p, op: idx === 0 ? null : ops[idx - 1] }; });
}

function cmDetailEvalCondition(expr, colIndex, row)
{
	expr = expr.trim();

	// IS NOT NULL
	var m = expr.match(/^(\w+)\s+IS\s+NOT\s+NULL$/i);
	if (m) {
		var ci = cmDetailColIdx(m[1], colIndex);
		return row[ci] !== null && row[ci] !== undefined && row[ci] !== '';
	}
	// IS NULL
	m = expr.match(/^(\w+)\s+IS\s+NULL$/i);
	if (m) {
		var ci = cmDetailColIdx(m[1], colIndex);
		return row[ci] === null || row[ci] === undefined || row[ci] === '';
	}
	// LIKE 'pattern'
	m = expr.match(/^(\w+)\s+LIKE\s+'([^']*)'$/i);
	if (m) {
		var ci = cmDetailColIdx(m[1], colIndex);
		var pat = '^' + m[2].replace(/[.*+?^${}()|[\]\\]/g, '\\$&').replace(/%/g, '.*').replace(/_/g, '.') + '$';
		return new RegExp(pat, 'i').test(row[ci] === null ? '' : String(row[ci]));
	}
	// Comparison: col op val
	m = expr.match(/^(\w+)\s*(=|!=|<>|>=|<=|>|<)\s*(.+)$/i);
	if (m) {
		var ci     = cmDetailColIdx(m[1], colIndex);
		var op     = m[2];
		var valRaw = m[3].trim();
		var cell   = row[ci];
		var val;

		if (valRaw.match(/^'[^']*'$/)) {            // quoted string
			val  = valRaw.slice(1, -1);
			cell = cell === null || cell === undefined ? '' : String(cell);
		} else if (valRaw.toLowerCase() === 'null') { // null literal
			val  = null;
		} else if (!isNaN(Number(valRaw))) {           // number
			val  = Number(valRaw);
			cell = Number(cell);
		} else {                                        // bare word → string
			val  = valRaw;
			cell = cell === null || cell === undefined ? '' : String(cell);
		}

		switch (op) {
			case '=':            return cell == val;
			case '!=': case '<>':return cell != val;
			case '>':            return cell >  val;
			case '>=':           return cell >= val;
			case '<':            return cell <  val;
			case '<=':           return cell <= val;
		}
	}
	throw new Error('Cannot parse: ' + expr);
}

function cmDetailColIdx(name, colIndex)
{
	var idx = colIndex[name.toLowerCase()];
	if (idx === undefined) throw new Error('Unknown column: ' + name);
	return idx;
}

function cmDetailShowMsg(msg)
{
	$('#cm-detail-table').html('<div style="padding:15px;text-align:center;color:#6c757d;">' + escHtml(String(msg)) + '</div>');
}

function cmDetailToggle()
{
	var $panel = $('#cm-detail-panel');
	if ($panel.is(':visible')) {
		cmDetailClose();
		return;
	}
	// Restore dark-mode preference from localStorage; default to page colour scheme on first visit
	var savedDarkCm = null;
	try { savedDarkCm = localStorage.getItem('cmDetailDark'); } catch(e) {}
	var dark = (savedDarkCm !== null) ? (savedDarkCm === '1') : (_colorSchema === 'dark');
	$('#cm-detail-dark').prop('checked', dark);
	if (dark) $panel.addClass('cm-dark'); else $panel.removeClass('cm-dark');

	// Attach scroll listener once — saves position to localStorage per CM (mirrors Active Statements)
	var $body = $('#cm-detail-body');
	if (!$body.data('cmScrollListenerAttached')) {
		$body.data('cmScrollListenerAttached', true);
		var _cmScrollTimer = null;
		$body.on('scroll', function() {
			clearTimeout(_cmScrollTimer);
			_cmScrollTimer = setTimeout(function() {
				if (_cmName) {
					try {
						localStorage.setItem('cmDetail-scroll-' + _cmName,
							JSON.stringify({ top: $body.scrollTop(), left: $body.scrollLeft() }));
					} catch(e) {}
				}
			}, 500);
		});
	}

	$panel.css('display', 'flex');
	try { localStorage.setItem('cmDetail-panelOpen', '1'); } catch(e) {}
	// Auto-load on first open (both live and history mode)
	if (!_cmSrvName && _serverList && _serverList.length > 0) {
		var _autoTs;
		if (isHistoryViewActive()) {
			// History mode: use last-known timestamp or read from the slider display text
			_autoTs = _cmTimestamp;
			if (!_autoTs) {
				var _sliderTxt = ($('#dbx-history-slider-center-text').text() || '').trim();
				_autoTs = _sliderTxt.split(' - ')[0].trim();
			}
		} else {
			_autoTs = moment().format('YYYY-MM-DD HH:mm:ss');
		}
		if (_autoTs && _autoTs.match(/^\d{4}-\d{2}-\d{2}/))
			cmDetailLoadList(_serverList[0], _autoTs);
	}
}

// Toggle dark mode on the CM detail panel and persist the preference
function cmDetailDarkToggle(on)
{
	var $panel = $('#cm-detail-panel');
	if (on) $panel.addClass('cm-dark'); else $panel.removeClass('cm-dark');
	try { localStorage.setItem('cmDetailDark', on ? '1' : '0'); } catch(e) {}
}

// Called from addData() on every live sample — refresh current CM if panel is open and not paused
function cmDetailLiveRefresh(srvName)
{
	if (!_cmAutoOpenChecked) {
		_cmAutoOpenChecked = true;
		// Only auto-open if the panel is not already open — calling toggle on a visible panel closes it.
		if (!$('#cm-detail-panel').is(':visible')) {
			try {
				if (localStorage.getItem('cmDetail-panelOpen') === '1') {
					cmDetailToggle(); // opens panel + fires cmDetailLoadList internally
					return;           // don't also run the refresh below — that would double-fetch
				}
			} catch(e) {}
		}
	}
	if (!$('#cm-detail-panel').is(':visible')) return;
	if ($('#cm-detail-paused').prop('checked')) return;
	var srv = _cmSrvName || srvName;
	if (!srv) return;
	var ts = moment().format('YYYY-MM-DD HH:mm:ss');
	_cmTimestamp = ts;
	$('#cm-detail-ts').text('@ ' + ts);

	if (!_cmName)
	{
		// No CM selected yet — do a full load (builds tabs, selects first CM with data)
		cmDetailLoadList(srv, ts);
		return;
	}

	// Re-query MonSessionSampleDetailes to update tab colours (same as native GUI on every sample),
	// then refresh data for the currently selected CM — without rebuilding the tab DOM.
	$.ajax({
		url: '/api/cc/mgt/cm/list',
		data: { srv: srv, time: ts },
		dataType: 'text',
		success: function(data) {
			try {
				var r = JSON.parse(data);
				if (!r.error) {
					_cmListData = r;
					cmDetailUpdateTabColors(r.groups);
					// Use exact SessionSampleTime from MonSessionSampleDetailes
					if (r.resolvedTime) { _cmTimestamp = r.resolvedTime.substring(0, 19); $('#cm-detail-ts').text('@ ' + _cmTimestamp); }
				}
			} catch(ex) {}
			cmDetailLoadData(srv, _cmName, _cmTimestamp, _cmType);
		},
		error: function() {
			// Still refresh CM data even if list call failed
			cmDetailLoadData(srv, _cmName, ts, _cmType);
		}
	});
}

// Update only the has-data/no-data classes on existing tab <li> elements.
// Called on live refresh so tab colours stay current without rebuilding the DOM.
function cmDetailUpdateTabColors(groups)
{
	if (!groups) return;
	groups.forEach(function(g) {
		var hasAny = g.cms.some(function(c) { return c.hasData; });
		$('#cm-group-tabs li[data-group-name]').each(function() {
			if ($(this).attr('data-group-name') === g.groupName) {
				$(this).toggleClass('cm-tab-has-data', hasAny).toggleClass('cm-tab-no-data', !hasAny);
			}
		});
		g.cms.forEach(function(c) {
			$('#cm-name-tabs li[data-cm-name]').each(function() {
				if ($(this).attr('data-cm-name') === c.cmName) {
					$(this).toggleClass('cm-tab-has-data', c.hasData).toggleClass('cm-tab-no-data', !c.hasData);
				}
			});
		});
	});
}

// Called when the history slider moves — refresh CM data for the selected time
function cmDetailSliderRefresh(startTime)
{
	if (!_cmAutoOpenChecked) {
		_cmAutoOpenChecked = true;
		if (!$('#cm-detail-panel').is(':visible')) {
			try {
				if (localStorage.getItem('cmDetail-panelOpen') === '1') {
					cmDetailToggle();
					return;
				}
			} catch(e) {}
		}
	}
	if (!$('#cm-detail-panel').is(':visible')) return;
	var srv = _cmSrvName || (_serverList && _serverList.length > 0 ? _serverList[0] : null);
	if (!srv || !startTime) return;
	cmDetailLoadList(srv, startTime);
}

// Column sort: 3-stage cycle — desc → asc → original
function cmDetailSortCol(colIdx)
{
	// Force-hide any stuck Bootstrap tooltip before re-rendering the table
	$('[data-toggle="tooltip"]').tooltip('hide');

	if (_cmSort.col === colIdx) {
		_cmSort.stage = (_cmSort.stage === 1) ? 2 : (_cmSort.stage === 2) ? 0 : 1;
		if (_cmSort.stage === 0) _cmSort.col = -1;
	} else {
		_cmSort.col   = colIdx;
		_cmSort.stage = 1;   // start with desc (highest first)
	}

	// Keep map in sync so slider timestamp changes restore the current sort
	if (_cmName) {
		_cmSortMap[_cmName] = { col: _cmSort.col, stage: _cmSort.stage };
		// Also persist to localStorage so sort survives page reload
		try {
			if (_cmSort.col >= 0)
				localStorage.setItem('cmDetail-sort-' + _cmName, JSON.stringify({ col: _cmSort.col, stage: _cmSort.stage }));
			else
				localStorage.removeItem('cmDetail-sort-' + _cmName);
		} catch(e) {}
	}

	cmDetailApplyFilter(_cmFilter);
}

function cmDetailClose()
{
	try { localStorage.setItem('cmDetail-panelOpen', '0'); } catch(e) {}
	$('#cm-detail-panel').hide();
	_cmSrvName = _cmTimestamp = _cmGroup = _cmName = _cmListData = _cmCurrentData = null;
	_cmFilter = '';
	_cmSort    = { col: -1, stage: 0 };
	_cmSortMap = {};
	// Destroy active charts
	_cmChartInstances.forEach(function(c) { try { c.destroy(); } catch(e) {} });
	_cmChartInstances = [];
	$('#cm-detail-charts').empty().hide();
	$('#cm-group-tabs,#cm-name-tabs').empty();
	$('#cm-detail-type-bar').hide();
	$('#cm-detail-filter-bar').hide();
	$('#cm-detail-filter-input').val('');
	$('#cm-detail-filter-count').text('');
	$('#cm-detail-row-count').text('');
	$('#cm-detail-table').html('');
}

/**
 * Render a single table cell value.
 * - URLs (http/https) become clickable links opening in a new tab.
 * - Native booleans and the strings "true"/"false" become disabled checkboxes.
 * - Everything else is HTML-escaped text.
 */
function renderCell(val)
{
	if (val === null || val === undefined) return '<span style="color:#aaa;">null</span>';
	if (val === true  || String(val).toLowerCase() === 'true')
		return '<input type="checkbox" checked onclick="return false;" style="cursor:default;pointer-events:none;">';
	if (val === false || String(val).toLowerCase() === 'false')
		return '<input type="checkbox" onclick="return false;" style="cursor:default;pointer-events:none;">';
	var s = String(val);
	if (s.match(/^https?:\/\//i))
		return '<a href="' + escHtml(s) + '" target="_blank" rel="noopener noreferrer">' + escHtml(s) + '</a>';
	return escHtml(s);
}

// escHtml() defined in dbxcentral.utils.js

//-----------------------------------------------------------
// CM CHART RENDERING (Chart.js)
// Driven by CmChartDescriptor[] in the JSON response.
//-----------------------------------------------------------

/** Active Chart.js instances — destroyed before re-rendering */
var _cmChartInstances = [];

/** Per-descriptor chart-mode override (pie vs bar for dual-pie-bar) */
var _cmChartModeMap = {};

/** Per-descriptor bar orientation override: 'auto' (default), 'horizontal', 'vertical' */
var _cmChartOrientMap = {};

/**
 * Color palette for chart series — 20 distinct, visually pleasant colors.
 * Cycles if more items are needed.
 */
var _cmChartPalette = [
	'#4e79a7','#f28e2b','#e15759','#76b7b2','#59a14f',
	'#edc948','#b07aa1','#ff9da7','#9c755f','#bab0ac',
	'#86bcb6','#8cd17d','#b6992d','#499894','#d37295',
	'#fabfd2','#d4a6c8','#a0cbe8','#ffbe7d','#d7b5a6'
];

/**
 * Get a threshold-aware color for a percentage value.
 */
function _cmChartPctColor(val, warn, crit) {
	if (val >= crit) return '#dc3545'; // red
	if (val >= warn) return '#fd7e14'; // orange
	return '#28a745';                  // green
}

/**
 * Main entry: render all chart descriptors for the current CM data.
 * Called at the end of cmDetailRenderFiltered().
 */
function cmChartRender(r, filteredRows) {
	// Destroy previous charts
	_cmChartInstances.forEach(function(c) { try { c.destroy(); } catch(e) {} });
	_cmChartInstances = [];

	var $container = $('#cm-detail-charts');
	$container.empty();

	var descriptors = r.chartDescriptors;
	if (!descriptors || descriptors.length === 0) {
		$container.hide();
		// Reset layout: table takes full width
		$('#cm-detail-table-wrap').css({ flex: '1 1 100%' });
		$('#cm-detail-content').css({ flexDirection: 'row' });
		return;
	}

	$container.show();

	// Determine layout from first descriptor's splitDir
	// "horizontal" = charts to the right of table (default)
	// "vertical"   = charts above the table
	var splitDir  = (descriptors[0].splitDir || 'horizontal');
	var splitRatio = descriptors[0].splitRatio || 0.5; // table fraction

	if (splitDir === 'horizontal') {
		// Side-by-side: table on left, charts on right
		var tablePct = Math.round(splitRatio * 100);
		var chartPct = 100 - tablePct;
		$('#cm-detail-content').css({ flexDirection: 'row' });
		$('#cm-detail-table-wrap').css({ flex: '1 1 ' + tablePct + '%', order: '' });
		$container.css({ flex: '0 1 ' + chartPct + '%', minWidth: '250px', maxWidth: '', borderLeft: '1px solid #ccc', borderBottom: '', order: '' });
	} else {
		// Vertical: charts ABOVE table
		$('#cm-detail-content').css({ flexDirection: 'column' });
		$('#cm-detail-table-wrap').css({ flex: '1 1 auto', order: 1 });
		$container.css({ flex: '0 0 auto', width: '100%', minWidth: '', maxWidth: '', borderLeft: 'none', borderBottom: '1px solid #ccc', order: 0 });
	}

	// For vertical layout: charts sit side-by-side above the table, sharing full width
	// For horizontal layout: charts stack vertically in the right pane
	if (splitDir === 'vertical') {
		$container.css({ display: 'flex', flexWrap: 'wrap' });
	} else {
		$container.css({ display: 'block' });
	}

	// Build column-name → index map
	var colIdx = {};
	(r.columns || []).forEach(function(c, i) { colIdx[c] = i; });

	var chartCount = descriptors.length;

	descriptors.forEach(function(desc) {
		var chartId = 'cm-chart-' + (desc.id || 'unknown');

		// Wrapper div — sizing depends on layout direction
		var wrapStyle = 'margin:4px;padding:6px;border:1px solid #ddd;border-radius:4px;background:#fff;';
		if (splitDir === 'vertical') {
			// Share full width evenly; min-width so they wrap if panel is narrow
			var pct = Math.floor(100 / chartCount) - 1;
			wrapStyle += 'flex:1 1 ' + pct + '%;min-width:200px;box-sizing:border-box;';
		}
		var $wrap = $('<div id="' + chartId + '-wrap" style="' + wrapStyle + '"></div>');

		// Title
		if (desc.title) {
			$wrap.append('<div style="font-size:0.82em;font-weight:600;margin-bottom:4px;text-align:center;">'
				+ escHtml(desc.title) + '</div>');
		}

		// Resolve chart type and orientation
		var chartType = desc.chartType || 'dual-pie-bar';
		var effectiveType = chartType;
		var isBarMode = false;

		if (chartType === 'dual-pie-bar') {
			var chartKey = (_cmName || '') + '::' + desc.id;
			var mode = _cmChartModeMap[chartKey];
			if (!mode) { try { mode = localStorage.getItem('cmChart-mode-' + chartKey); } catch(e) {} }
			if (!mode) mode = 'pie';
			effectiveType = mode === 'bar' ? 'stacked-bar' : 'pie';
			isBarMode = (mode === 'bar');
		} else if (chartType === 'stacked-bar' || chartType === 'bar' || chartType === 'horizontal-bar') {
			isBarMode = true;
		}

		// Build toolbar row
		var $toolbar = $('<div style="text-align:center;margin-bottom:4px;"></div>');
		var btnStyle = 'font-size:0.72em;padding:1px 6px;margin:0 2px;';
		var hasToolbar = false;

		// Pie/Bar toggle for dual-pie-bar
		if (chartType === 'dual-pie-bar') {
			var chartKey = (_cmName || '') + '::' + desc.id;
			var mode = _cmChartModeMap[chartKey];
			if (!mode) { try { mode = localStorage.getItem('cmChart-mode-' + chartKey); } catch(e) {} }
			if (!mode) mode = 'pie';
			var $btnPie = $('<button class="btn btn-xs btn-sm ' + (mode === 'pie' ? 'btn-primary' : 'btn-outline-secondary')
				+ '" style="' + btnStyle + '">Pie</button>');
			var $btnBar = $('<button class="btn btn-xs btn-sm ' + (mode === 'bar' ? 'btn-primary' : 'btn-outline-secondary')
				+ '" style="' + btnStyle + '">Bar</button>');
			(function(ck) {
				$btnPie.click(function() { _cmChartModeMap[ck] = 'pie'; try { localStorage.setItem('cmChart-mode-' + ck, 'pie'); } catch(e) {} cmChartRender(r, filteredRows); });
				$btnBar.click(function() { _cmChartModeMap[ck] = 'bar'; try { localStorage.setItem('cmChart-mode-' + ck, 'bar'); } catch(e) {} cmChartRender(r, filteredRows); });
			})(chartKey);
			$toolbar.append($btnPie).append($btnBar);
			hasToolbar = true;
		}

		// Orientation toggle for any bar-type chart (including bar mode of dual-pie-bar)
		if (isBarMode) {
			var orient = _cmChartOrientMap[desc.id] || 'auto';
			if (hasToolbar) $toolbar.append($('<span style="margin:0 4px;color:#ccc;">|</span>'));
			var orientBtns = [
				{ key: 'auto',       label: 'Auto (switch based on row count)',  icon: '<i class="fa fa-magic"></i>' },
				{ key: 'horizontal', label: 'Horizontal bars (laying)',          icon: '<i class="fa fa-bar-chart fa-rotate-90"></i>' },
				{ key: 'vertical',   label: 'Vertical bars (standing)',          icon: '<i class="fa fa-bar-chart"></i>' }
			];
			orientBtns.forEach(function(ob) {
				var cls = (orient === ob.key) ? 'btn-primary' : 'btn-outline-secondary';
				var $btn = $('<button class="btn btn-xs btn-sm ' + cls + '" style="' + btnStyle + '" title="' + ob.label + '">'
					+ ob.icon + '</button>');
				(function(descId, key) {
					$btn.click(function() { _cmChartOrientMap[descId] = key; cmChartRender(r, filteredRows); });
				})(desc.id, ob.key);
				$toolbar.append($btn);
			});
			hasToolbar = true;
		}

		if (hasToolbar) $wrap.append($toolbar);

		// Canvas
		var $canvas = $('<canvas id="' + chartId + '" style="max-height:280px;"></canvas>');
		$wrap.append($canvas);
		$container.append($wrap);

		// Extract data from filtered rows
		var data = _cmChartExtractData(desc, colIdx, filteredRows);
		if (!data || data.labels.length === 0) {
			$canvas.replaceWith('<div style="color:#999;font-size:0.8em;text-align:center;padding:20px;">No data</div>');
			return;
		}

		// Create chart — pass orientation override for bar charts
		var orientOverride = isBarMode ? (_cmChartOrientMap[desc.id] || 'auto') : null;
		var chart = _cmChartCreate($canvas[0], effectiveType, desc, data, orientOverride);
		if (chart) _cmChartInstances.push(chart);
	});
}

/**
 * Extract labels and datasets from filtered rows based on a descriptor.
 * Returns { labels: [...], datasets: [{ label, data, colors }] }
 */
function _cmChartExtractData(desc, colIdx, rows) {
	var labelIdx = colIdx[desc.labelColumn];
	if (labelIdx === undefined) return null;

	var valCols = desc.valueColumns || [];
	var valIdxs = valCols.map(function(c) { return colIdx[c]; });
	// Check all value columns exist
	if (valIdxs.some(function(i) { return i === undefined; })) return null;

	var groupByIdx = desc.groupByColumn ? colIdx[desc.groupByColumn] : undefined;
	var barLabelIdx = desc.barLabelColumn ? colIdx[desc.barLabelColumn] : undefined;

	// Aggregate rows into: { label -> [agg_per_value_column] }
	// groupAggFunc: "sum" (default), "avg", "max"
	var aggFunc    = desc.groupAggFunc || 'sum';
	var aggregated = {};
	var aggCounts  = {};   // label -> row count (for avg calculation)
	var barLabels  = {};   // label -> barLabelColumn value (max-wins for grouped rows)
	var labelOrder = [];

	rows.forEach(function(row) {
		var label = groupByIdx !== undefined ? String(row[groupByIdx] || '') : String(row[labelIdx] || '');

		// skipValue filter
		if (desc.skipValue && String(row[labelIdx] || '') === desc.skipValue) return;

		var vals = valIdxs.map(function(vi) {
			var v = row[vi];
			return (v === null || v === undefined) ? 0 : parseFloat(v) || 0;
		});

		// skipZeroRows: skip if all values are zero
		if (desc.skipZeroRows && vals.every(function(v) { return v === 0; })) return;

		if (!aggregated[label]) {
			aggregated[label] = vals.slice();
			aggCounts[label]  = 1;
			labelOrder.push(label);
		} else {
			aggCounts[label]++;
			for (var i = 0; i < vals.length; i++) {
				if (aggFunc === 'max')
					aggregated[label][i] = Math.max(aggregated[label][i], vals[i]);
				else // sum (and avg — we divide after the loop)
					aggregated[label][i] += vals[i];
			}
		}

		// Capture bar label value (for "FREE MB: xxx" text on bars) — use max for grouped
		if (barLabelIdx !== undefined) {
			var blv = row[barLabelIdx];
			var parsed = (blv === null || blv === undefined) ? 0 : parseFloat(blv) || 0;
			barLabels[label] = Math.max(barLabels[label] || 0, parsed);
		}
	});

	// For avg: divide sums by count
	if (aggFunc === 'avg') {
		labelOrder.forEach(function(label) {
			var cnt = aggCounts[label] || 1;
			for (var i = 0; i < aggregated[label].length; i++)
				aggregated[label][i] = aggregated[label][i] / cnt;
		});
	}

	// Sort by first value column descending (largest slice first)
	labelOrder.sort(function(a, b) {
		return (aggregated[b][0] || 0) - (aggregated[a][0] || 0);
	});

	// Apply maxItems limit
	if (desc.maxItems > 0 && labelOrder.length > desc.maxItems) {
		var kept  = labelOrder.slice(0, desc.maxItems);
		var rest  = labelOrder.slice(desc.maxItems);
		var others = new Array(valCols.length).fill(0);
		rest.forEach(function(lbl) {
			for (var i = 0; i < valCols.length; i++)
				others[i] += aggregated[lbl][i];
		});
		aggregated['Others'] = others;
		kept.push('Others');
		labelOrder = kept;
	}

	// Apply pieOtherLimit for pie charts — done at render time
	// Build datasets (one per valueColumn)
	var datasets = valCols.map(function(vc, vi) {
		return {
			label: (desc.seriesLabels && desc.seriesLabels[vi]) ? desc.seriesLabels[vi] : vc,
			data: labelOrder.map(function(lbl) { return aggregated[lbl][vi]; })
		};
	});

	// Build bar label array parallel to labelOrder (for "FREE MB: xxx" on bars)
	var barLabelValues = barLabelIdx !== undefined
		? labelOrder.map(function(lbl) { return barLabels[lbl] || 0; })
		: null;

	return { labels: labelOrder, datasets: datasets, aggregated: aggregated, barLabelValues: barLabelValues };
}

/**
 * Create a Chart.js chart on a canvas element.
 */
function _cmChartCreate(canvas, chartType, desc, data, orientOverride) {
	var ctx = canvas.getContext('2d');

	if (chartType === 'pie') {
		return _cmChartCreatePie(ctx, desc, data);
	} else if (chartType === 'stacked-bar' || chartType === 'bar' || chartType === 'horizontal-bar') {
		return _cmChartCreateBar(ctx, desc, data, chartType === 'stacked-bar', orientOverride);
	}
	return null;
}

/**
 * Create a pie chart. For dual-pie-bar with multiple value columns,
 * show only the first valueColumn in the pie (consistent with the Swing GUI).
 */
function _cmChartCreatePie(ctx, desc, data) {
	var labels   = data.labels.slice();
	var values   = data.datasets[0].data.slice();
	var serLabel = data.datasets[0].label;

	// Collapse small slices into "Others" based on pieOtherLimit
	var total = values.reduce(function(s, v) { return s + v; }, 0);
	if (total > 0 && desc.pieOtherLimit > 0) {
		var threshold = total * desc.pieOtherLimit;
		var newLabels = [], newValues = [], othersVal = 0;
		for (var i = 0; i < labels.length; i++) {
			if (values[i] < threshold) {
				othersVal += values[i];
			} else {
				newLabels.push(labels[i]);
				newValues.push(values[i]);
			}
		}
		if (othersVal > 0) {
			newLabels.push('Others');
			newValues.push(othersVal);
		}
		labels = newLabels;
		values = newValues;
	}

	// Colors
	var bgColors;
	if (desc.isPercent) {
		bgColors = values.map(function(v) {
			return _cmChartPctColor(v, desc.thresholdWarn || 80, desc.thresholdCrit || 90);
		});
	} else {
		bgColors = labels.map(function(_, i) { return _cmChartPalette[i % _cmChartPalette.length]; });
	}

	return new Chart(ctx, {
		type: 'pie',
		data: {
			labels: labels,
			datasets: [{
				label: serLabel,
				data: values,
				backgroundColor: bgColors,
				borderWidth: 1
			}]
		},
		options: {
			responsive: true,
			maintainAspectRatio: false,
			legend: {
				position: 'right',
				labels: { fontSize: 10, boxWidth: 12 }
			},
			tooltips: {
				callbacks: {
					title: function(tooltipItems, chartData) {
						// Show the slice/item name as the tooltip title
						return chartData.labels[tooltipItems[0].index] || '';
					},
					label: function(tooltipItem, chartData) {
						var dsLabel = chartData.datasets[tooltipItem.datasetIndex].label || '';
						var val     = chartData.datasets[tooltipItem.datasetIndex].data[tooltipItem.index];
						var total   = chartData.datasets[tooltipItem.datasetIndex].data.reduce(function(s, v) { return s + v; }, 0);
						var pct     = total > 0 ? ((val / total) * 100).toFixed(1) : '0.0';
						return dsLabel + ': ' + _cmChartFormatNumber(val) + ' (' + pct + '%)';
					}
				}
			}
		}
	});
}

/**
 * Create a bar (or stacked bar) chart.
 * Auto-layout: ≤10 items → horizontal bars (laying), >10 → vertical bars (standing).
 * This mirrors the native GUI's PlotOrientation.AUTO behaviour.
 */
function _cmChartCreateBar(ctx, desc, data, stacked, orientOverride) {
	// Orientation: 'auto' (default) uses native-GUI threshold (≤10 → horizontal, >10 → vertical)
	// User can override via toggle buttons: 'horizontal' or 'vertical'
	var horizontal;
	if (orientOverride === 'horizontal')    horizontal = true;
	else if (orientOverride === 'vertical') horizontal = false;
	else /* 'auto' */                       horizontal = (data.labels.length <= 30);

	var datasets = data.datasets.map(function(ds, di) {
		var bgColors;
		if (desc.isPercent && data.datasets.length === 1) {
			// Per-bar threshold colors (green/orange/red)
			bgColors = ds.data.map(function(v) {
				return _cmChartPctColor(v, desc.thresholdWarn || 80, desc.thresholdCrit || 90);
			});
		} else {
			bgColors = _cmChartPalette[di % _cmChartPalette.length];
		}
		return {
			label: ds.label,
			data: ds.data,
			backgroundColor: bgColors,
			borderWidth: 1
		};
	});

	// The "value axis" is the one that shows numbers (Y for vertical, X for horizontal)
	var valueTicks = {
		beginAtZero: true,
		fontSize: 10,
		callback: function(value) { return _cmChartFormatNumber(value); }
	};
	if (desc.isPercent) {
		valueTicks.max = 100;
		valueTicks.min = 0;
	}

	// The "category axis" shows labels (X for vertical, Y for horizontal)
	var categoryTicks = { fontSize: 9 };
	if (!horizontal) {
		categoryTicks.autoSkip = true;
		categoryTicks.maxRotation = 45;
	}

	var xAxes, yAxes;
	if (horizontal) {
		// Horizontal: labels on Y-axis, values on X-axis
		xAxes = [{ ticks: valueTicks, stacked: stacked }];
		yAxes = [{ ticks: categoryTicks, stacked: stacked }];
	} else {
		// Vertical: labels on X-axis, values on Y-axis
		xAxes = [{ ticks: categoryTicks, stacked: stacked }];
		yAxes = [{ ticks: valueTicks, stacked: stacked }];
	}

	// Bar label text on bars (e.g. "FREE MB: 3 144")
	var barLabelVals  = data.barLabelValues; // array parallel to data.labels, or null
	var barLabelPfx   = desc.barLabelPrefix || 'FREE MB: ';
	var hasBarLabels  = barLabelVals && barLabelVals.length > 0;

	// Build tooltip callback — include FREE MB in tooltip when available
	var tooltipCb = function(tooltipItem, chartData) {
		var ds  = chartData.datasets[tooltipItem.datasetIndex];
		var val = ds.data[tooltipItem.index];
		var suffix = desc.isPercent ? '%' : '';
		var line = ds.label + ': ' + _cmChartFormatNumber(val) + suffix;
		if (hasBarLabels) {
			var freeMb = barLabelVals[tooltipItem.index];
			line += '  (' + barLabelPfx + _cmChartFormatNumber(freeMb) + ')';
		}
		return line;
	};

	// Animation onComplete: draw "FREE MB: xxx" text inside/beside each bar
	var animOnComplete = hasBarLabels ? function(animation) {
		var chart = animation.chart || this;
		var ctx2  = chart.ctx;
		ctx2.save();
		ctx2.font      = '10px sans-serif';
		ctx2.fillStyle = '#333';
		ctx2.textBaseline = 'middle';

		var meta = chart.getDatasetMeta(0);
		if (!meta || !meta.data) { ctx2.restore(); return; }

		meta.data.forEach(function(bar, idx) {
			var freeMb = barLabelVals[idx];
			if (freeMb === undefined || freeMb === null) return;
			var txt = barLabelPfx + _cmChartFormatNumber(freeMb);

			if (horizontal) {
				// Horizontal bar: text inside bar, vertically centred
				var barWidth = bar._model.x - bar._model.base;
				var textW    = ctx2.measureText(txt).width;
				if (barWidth > textW + 8) {
					// Fits inside bar
					ctx2.textAlign = 'left';
					ctx2.fillText(txt, bar._model.base + 4, bar._model.y);
				} else {
					// Draw right of bar
					ctx2.textAlign = 'left';
					ctx2.fillText(txt, bar._model.x + 3, bar._model.y);
				}
			} else {
				// Vertical bar: text inside bar, horizontally centred
				var barHeight = bar._model.base - bar._model.y;
				if (barHeight > 16) {
					ctx2.textAlign = 'center';
					ctx2.fillText(txt, bar._model.x, bar._model.y + barHeight / 2);
				}
				// If bar too short, skip (tooltip still shows it)
			}
		});
		ctx2.restore();
	} : undefined;

	return new Chart(ctx, {
		type: horizontal ? 'horizontalBar' : 'bar',
		data: { labels: data.labels, datasets: datasets },
		options: {
			responsive: true,
			maintainAspectRatio: false,
			animation: hasBarLabels ? { onComplete: animOnComplete } : {},
			legend: {
				display: data.datasets.length > 1,
				labels: { fontSize: 10, boxWidth: 12 }
			},
			scales: { xAxes: xAxes, yAxes: yAxes },
			tooltips: {
				callbacks: { label: tooltipCb }
			}
		}
	});
}

/**
 * Format numbers with space as thousands separator (e.g. 27 793, 3 144.1).
 * No K/M/B abbreviations — mirrors the native GUI's NumberFormat style.
 */
function _cmChartFormatNumber(val) {
	if (val === null || val === undefined) return '0';
	var n = parseFloat(val);
	if (isNaN(n)) return String(val);
	// Up to 2 decimal places, trim trailing zeros
	var str = parseFloat(n.toFixed(2)).toString();
	// Add space thousands separator to the integer part
	var parts = str.split('.');
	parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ' ');
	return parts.join('.');
}

//-----------------------------------------------------------
// TREND GRAPHS BUTTON
//-----------------------------------------------------------

/** Update the Trend Graphs button state for the current CM. Called after CM switch. */
function cmTrendGraphsUpdateButton()
{
	var $btn = $('#cm-detail-trend-btn');
	if (!$btn.length) return;

	var graphs = _cmTrendGetGraphsForCm(_cmName);
	var $badge = $('#cm-detail-trend-badge');
	if (graphs.length === 0) {
		$btn.prop('disabled', true).attr('title', 'No Trend Graphs for this Performance Counter');
		$badge.hide();
	} else {
		$btn.prop('disabled', false).attr('title', graphs.length + ' Trend Graph' + (graphs.length > 1 ? 's' : '') + ' for this Performance Counter');
		$badge.text(graphs.length).show();
	}
	// Close popup when switching CMs
	$('#cm-trend-popup').hide();
}

/** Fetch (cached) all graph properties for this server */
function _cmTrendEnsureCache(callback)
{
	if (_cmTrendCache) { callback(_cmTrendCache); return; }
	var srv = (_cmSrvName || getParameter('sessionName', '')).split(',')[0].trim();
	if (!srv) { callback([]); return; }
	$.ajax({
		url: '/api/graphs?sessionName=' + srv,
		dataType: 'json',
		success: function(data) {
			_cmTrendCache = Array.isArray(data) ? data
			              : Array.isArray(data.graphs) ? data.graphs
			              : Array.isArray(data.data)   ? data.data
			              : Object.values(data).find(function(v) { return Array.isArray(v); }) || [];
			callback(_cmTrendCache);
		},
		error: function() { _cmTrendCache = []; callback([]); }
	});
}

/** Get graphs belonging to a specific CM from cache */
function _cmTrendGetGraphsForCm(cmName)
{
	if (!_cmTrendCache || !cmName) return [];
	return _cmTrendCache.filter(function(g) { return g.cmName === cmName; });
}

/** Get set of graph tableNames currently loaded on the page */
function _cmTrendLoadedSet()
{
	var s = {};
	if (typeof _graphMap !== 'undefined' && _graphMap) {
		for (var i = 0; i < _graphMap.length; i++)
			s[_graphMap[i]._fullName || _graphMap[i].getFullName()] = true;
	}
	return s;
}

/** Toggle the trend graphs popup */
function cmTrendGraphsToggle()
{
	var $popup = $('#cm-trend-popup');
	if ($popup.is(':visible')) { $popup.hide(); return; }

	var graphs = _cmTrendGetGraphsForCm(_cmName);
	if (graphs.length === 0) return;

	var loaded = _cmTrendLoadedSet();
	var html = '<div class="cm-tg-hdr" style="padding:5px 8px;font-weight:600;font-size:0.9em;border-bottom:1px solid #dee2e6;background:#f1f3f5;">'
		+ '<i class="fa fa-line-chart" style="margin-right:4px;"></i>'
		+ escHtml(_cmName) + ' &mdash; ' + graphs.length + ' graph' + (graphs.length > 1 ? 's' : '')
		+ '</div>';

	graphs.forEach(function(g) {
		var isLoaded = !!loaded[g.tableName];
		var icon = isLoaded
			? '<i class="fa fa-check-circle" style="color:#28a745;margin-right:5px;" title="Loaded on this page &mdash; click to jump"></i>'
			: '<i class="fa fa-circle-o" style="color:#aaa;margin-right:5px;" title="Not loaded &mdash; click to open Graph Picker"></i>';
		var style = 'padding:5px 8px;cursor:pointer;border-bottom:1px solid #f0f0f0;display:flex;align-items:center;';
		html += '<div class="cm-tg-item" style="' + style + '" '
			+ 'onclick="cmTrendGraphClick(\'' + escHtml(g.tableName) + '\',' + isLoaded + ');" '
			+ 'title="' + (isLoaded ? 'Jump to this graph' : 'Open Graph Picker to add this graph') + '">'
			+ icon
			+ '<span style="flex:1;">' + escHtml(g.graphLabel || g.graphName) + '</span>'
			+ '<span class="badge" style="background:' + (isLoaded ? '#e8f5e9;color:#2e7d32' : '#f5f5f5;color:#999') + ';font-size:0.8em;margin-left:4px;">'
			+ escHtml(g.graphCategory || '') + '</span>'
			+ '</div>';
	});

	$popup.html(html).show();
}

/** Handle click on a trend graph item */
function cmTrendGraphClick(tableName, isLoaded)
{
	$('#cm-trend-popup').hide();

	if (isLoaded) {
		// Reuse the same scroll + bounce effect as the search/filter dialog
		var el = document.getElementById(tableName);
		if (el && typeof scrollToAction === 'function') {
			scrollToAction(el);
		} else if (el) {
			el.scrollIntoView({ behavior: 'smooth', block: 'center' });
		}
	} else {
		// Open the Graph Picker with search pre-set to CM name
		var srv       = (_cmSrvName || getParameter('sessionName', '')).split(',')[0].trim();
		var startTime = getParameter('startTime', '2h');
		var endTime   = getParameter('endTime',   '');
		var preSel    = (typeof _graphMap !== 'undefined' && _graphMap)
			? _graphMap.map(function(g) { return g._fullName || g.getFullName(); })
			: [];

		dbxOpenGraphPickerModal(srv, startTime, {
			target:      '_self',
			preSelected: preSel,
			endTime:     endTime
		});

		// Pre-fill search with CM name and highlight the clicked graph
		setTimeout(function() {
			var $search = $('#dbxGraphPickerSearch');
			if ($search.length) {
				$search.val(_cmName).trigger('input');
			}
			// Find the checkbox for the clicked graph and highlight its row
			var $cb = $('#dbxGraphPickerList input[value="' + tableName + '"]');
			if ($cb.length) {
				var $item = $cb.closest('.dbx-gp-item');
				$item.css('background', '#d0e8ff');
				// Scroll into view within the picker list
				if ($item[0]) {
					$item[0].scrollIntoView({ behavior: 'smooth', block: 'center' });
				}
			}
		}, 300);
	}
}

// Close popup when clicking outside
$(document).on('click', function(e) {
	if (!$(e.target).closest('#cm-trend-popup, #cm-detail-trend-btn').length)
		$('#cm-trend-popup').hide();
});

// Add hover effect for popup items (light mode)
$(document).on('mouseenter', '.cm-tg-item', function() { $(this).css('background', '#f0f4ff'); });
$(document).on('mouseleave', '.cm-tg-item', function() { $(this).css('background', ''); });

//-----------------------------------------------------------
// CM CONTEXT MENU + PROPERTIES DIALOG
//-----------------------------------------------------------

var _cmConfigCache = null;   // cached response from /api/cc/mgt/config/get
var _cmConfigCacheSrv = null; // which server the cache is for

/** Show context menu at mouse position for the given CM tab */
function _cmShowContextMenu(e, cmName) {
	var $menu = $('#cm-context-menu');
	$menu.css({ top: e.clientY + 'px', left: e.clientX + 'px' }).show();
	// Bind click
	$('#cm-ctx-properties').off('click').on('click', function(ev) {
		ev.preventDefault();
		$menu.hide();
		cmPropsOpen(cmName);
	});
	// Close on click outside
	$(document).one('click', function() { $menu.hide(); });
}

/** Fetch config data (cached per server) and open properties modal */
function cmPropsOpen(cmName) {
	if (!_cmSrvName) return;

	// If we already have it cached for this server, use it
	if (_cmConfigCache && _cmConfigCacheSrv === _cmSrvName) {
		_cmPropsRender(cmName, _cmConfigCache);
		return;
	}

	// Show modal immediately with loading state
	$('#cm-props-title').html('<i class="fa fa-info-circle"></i> ' + cmName + ' — Properties');
	$('#cm-props-info-fields').html('<div class="text-center text-muted py-3"><i class="fa fa-spinner fa-spin"></i> Loading...</div>');
	$('#cm-props-info-sql').hide();
	$('#cm-props-options-content').html('');
	$('#cm-props-settings-content').html('');
	$('#cm-props-alarms-content').html('');
	$('#cm-props-modal').modal('show');

	$.ajax({
		url: '/api/cc/mgt/config/get',
		data: { srvName: _cmSrvName },
		dataType: 'json',
		success: function(data) {
			_cmConfigCache = data;
			_cmConfigCacheSrv = _cmSrvName;
			_cmPropsRender(cmName, data);
		},
		error: function(xhr) {
			$('#cm-props-info-fields').html('<div class="text-danger">Failed to load configuration: ' + (xhr.statusText || 'Unknown error') + '</div>');
		}
	});
}

/** Render all 4 tabs of the properties modal */
function _cmPropsRender(cmName, configData) {
	// Find the CM object in the config response
	var cmList = configData.cmList || configData.counters || configData;
	var cmObj = null;
	if (Array.isArray(cmList)) {
		for (var i = 0; i < cmList.length; i++) {
			if (cmList[i].cmName === cmName) { cmObj = cmList[i]; break; }
		}
	}

	if (!cmObj) {
		$('#cm-props-info-fields').html('<div class="text-warning">CM "' + cmName + '" not found in configuration.</div>');
		$('#cm-props-info-sql').hide();
		$('#cm-props-options-content').html('');
		$('#cm-props-settings-content').html('');
		$('#cm-props-alarms-content').html('');
		// Still show modal
		$('#cm-props-modal').modal('show');
		return;
	}

	var displayName = cmObj.displayName || cmName;
	$('#cm-props-title').html('<i class="fa fa-info-circle"></i> ' + displayName + ' — Properties');

	// --- INFO tab ---
	// Extract Postpone Time / Query Timeout from options array (if present)
	var postponeTimeVal = null;
	var postponeTimeDef = null;
	var queryTimeoutVal = null;
	var queryTimeoutDef = null;
	if (cmObj.options && Array.isArray(cmObj.options)) {
		cmObj.options.forEach(function(o) {
			if (o.name === 'Postpone Time') { postponeTimeVal = o.value; postponeTimeDef = o.defaultValue; }
			if (o.name === 'Query Timeout') { queryTimeoutVal = o.value; queryTimeoutDef = o.defaultValue; }
		});
	}
	var postponeTime = postponeTimeVal != null ? (postponeTimeVal + ' s (' + (String(postponeTimeVal) === String(postponeTimeDef) ? 'default' : 'modified') + ')') : '(not available)';
	var queryTimeout = queryTimeoutVal != null ? (queryTimeoutVal + ' s (' + (String(queryTimeoutVal) === String(queryTimeoutDef) ? 'default' : 'modified') + ')') : '(not available)';

	// Use 'divider' as a special marker for a visual separator row
	var infoFields = [
		{ label: 'CM Name',              value: cmObj.cmName },
		{ label: 'Display Name',         value: cmObj.displayName },
		{ label: 'Group',                value: cmObj.groupName },
		{ label: 'Is Enabled',           value: _cmPropsBool(cmObj.isCmEnabled), isHtml: true },
		{ label: 'Has System Alarms',    value: _cmPropsBool(cmObj.hasSystemAlarms) + (cmObj.hasSystemAlarms ? '&nbsp;&nbsp;(enabled: ' + _cmPropsBool(cmObj.isSystemAlarmsEnabled) + ')' : ''), isHtml: true },
		{ label: 'Has User Defined Alarms', value: _cmPropsBool(cmObj.hasUserDefinedAlarmInterrogator) + (cmObj.hasUserDefinedAlarmInterrogator ? '&nbsp;&nbsp;(enabled: ' + _cmPropsBool(cmObj.isUserDefinedAlarmsEnabled) + ')' : ''), isHtml: true },
		{ label: 'Postpone Time',        value: postponeTime },
		{ label: 'Query Timeout',        value: queryTimeout },
		'divider',
		{ label: 'Primary Key',          value: _cmPropsArr(cmObj.pkCols) },
		{ label: 'Diff Columns',         value: _cmPropsArr(cmObj.diffCols) },
		{ label: 'Pct Columns',          value: _cmPropsArr(cmObj.pctCols) },
		'divider',
		{ label: 'Need Server Config',   value: _cmPropsArr(cmObj.needSrvConfig) },
		{ label: 'Need Server Roles',    value: _cmPropsArr(cmObj.needSrvRoles) },
		{ label: 'Need Server Version',  value: cmObj.needSrvVersion || '(any)' },
		{ label: 'Depends On CM',        value: _cmPropsArr(cmObj.dependsOnCm) },
		{ label: 'Init SQL',             value: cmObj.sqlInit || '(none)' },
		{ label: 'Close SQL',            value: cmObj.sqlClose || '(none)' }
	];
	var html = '<table class="table table-sm table-borderless" style="font-size:0.92em;">';
	infoFields.forEach(function(f) {
		if (f === 'divider') {
			html += '<tr><td colspan="2" style="border-bottom:1px solid #dee2e6;padding:2px 0;"></td></tr>';
			return;
		}
		var val = f.value || '(none)';
		var valHtml = f.isHtml ? val : escHtml(val);
		html += '<tr><td style="width:180px;font-weight:600;white-space:nowrap;color:#555;">' + f.label + '</td>'
		      + '<td style="word-break:break-all;">' + valHtml + '</td></tr>';
	});
	html += '</table>';
	$('#cm-props-info-fields').html(html);

	// SQL
	var sql = cmObj.sqlRefresh || '';
	if (sql) {
		$('#cm-props-sql').text(sql);
		$('#cm-props-info-sql').show();
		if (typeof Prism !== 'undefined') Prism.highlightElement($('#cm-props-sql')[0]);
	} else {
		$('#cm-props-info-sql').hide();
	}

	// --- OPTIONS tab ---
	$('#cm-props-options-content').html(_cmPropsSettingsTable(cmObj.options, 'Collector Options'));

	// --- LOCAL SETTINGS tab ---
	$('#cm-props-settings-content').html(_cmPropsSettingsTable(cmObj.settings, 'Local Settings'));

	// --- ALARMS tab ---
	$('#cm-props-alarms-content').html(_cmPropsAlarmsHtml(cmObj.alarmSettings));

	// Reset to Info tab
	$('#cm-props-tabs a:first').tab('show');

	// Show modal if not yet visible
	$('#cm-props-modal').modal('show');
}

/** Format an array or string value for display */
function _cmPropsArr(val) {
	if (!val) return '(none)';
	if (Array.isArray(val)) return val.length === 0 ? '(none)' : val.join(', ');
	return String(val);
}

/** Format a boolean for display — green Yes / red No */
function _cmPropsBool(val) {
	if (val === true)  return '<span style="color:#28a745;font-weight:600;">Yes</span>';
	if (val === false) return '<span style="color:#dc3545;font-weight:600;">No</span>';
	return '(unknown)';
}

/** Build an HTML table for Options or Local Settings arrays */
function _cmPropsSettingsTable(arr, title) {
	if (!arr || !arr.length) return '<div class="text-muted py-2">No ' + title.toLowerCase() + ' available.</div>';

	var html = '<table class="table table-sm table-striped table-hover" style="font-size:0.88em;">'
		+ '<thead><tr>'
		+ '<th>Name</th>'
		+ '<th>Value</th>'
		+ '<th>Default</th>'
		+ '<th style="width:50px;text-align:center;">Changed</th>'
		+ '<th>Property</th>'
		+ '<th>Description</th>'
		+ '</tr></thead><tbody>';

	arr.forEach(function(s) {
		var changed = s.isDefaultValue === false;
		var changedIcon = changed ? '<span style="color:#dc3545;" title="Non-default value">●</span>' : '<span style="color:#ccc;">–</span>';
		var valStyle = changed ? 'font-weight:600;color:#0d6efd;' : '';
		html += '<tr>'
			+ '<td style="white-space:nowrap;">' + escHtml(s.name || '') + '</td>'
			+ '<td style="' + valStyle + '">' + renderCell(s.value) + '</td>'
			+ '<td style="color:#888;">' + renderCell(s.defaultValue) + '</td>'
			+ '<td style="text-align:center;">' + changedIcon + '</td>'
			+ '<td style="color:#999;font-size:0.88em;font-family:monospace;word-break:break-all;">' + escHtml(s.property || '') + '</td>'
			+ '<td style="color:#666;font-size:0.92em;">' + escHtml(s.description || '') + '</td>'
			+ '</tr>';
	});

	html += '</tbody></table>';
	return html;
}

/** Build HTML for the Alarms tab (preChecks + alarm list with expandable parameters) */
function _cmPropsAlarmsHtml(alarmSettings) {
	if (!alarmSettings) return '<div class="text-muted py-2">No alarm settings available.</div>';

	var html = '';

	// Pre-checks
	if (alarmSettings.preChecks && alarmSettings.preChecks.length) {
		html += '<b>Pre Checks</b> <span style="color:#888;font-size:0.88em;">(checked before applying alarm thresholds)</span>';
		html += _cmPropsSettingsTable(alarmSettings.preChecks, 'Pre Checks');
	}

	// Alarms
	var alarms = alarmSettings.alarms;
	if (!alarms || !alarms.length) {
		html += '<div class="text-muted py-2">No alarms defined.</div>';
		return html;
	}

	html += '<b>Alarms</b> <span style="color:#888;font-size:0.88em;">(click an alarm to see its parameters)</span>';
	html += '<div class="list-group mt-1" style="font-size:0.88em;">';

	alarms.forEach(function(alarm, idx) {
		var alarmId = 'cm-props-alarm-' + idx;
		html += '<div class="list-group-item list-group-item-action" style="cursor:pointer;padding:6px 12px;" '
			+ 'onclick="$(\'#' + alarmId + '\').toggle();">'
			+ '<div style="display:flex;justify-content:space-between;align-items:center;">'
			+ '<span><i class="fa fa-bell" style="color:#ffc107;margin-right:6px;"></i><b>' + escHtml(alarm.name || '') + '</b></span>'
			+ '<i class="fa fa-chevron-down" style="color:#aaa;font-size:0.8em;"></i>'
			+ '</div>';
		if (alarm.description) {
			html += '<div style="color:#666;font-size:0.9em;margin-top:2px;">' + escHtml(alarm.description) + '</div>';
		}
		html += '</div>';

		// Expandable parameters section
		html += '<div id="' + alarmId + '" style="display:none;padding:4px 12px 8px;border:1px solid #eee;border-top:none;background:#fafafa;">';
		if (alarm.parameters && alarm.parameters.length) {
			html += '<table class="table table-sm table-striped mb-0" style="font-size:0.88em;">'
				+ '<thead><tr><th>Name</th><th>Value</th><th>Default</th><th style="width:50px;text-align:center;">Changed</th><th>Property</th><th>Description</th></tr></thead><tbody>';
			alarm.parameters.forEach(function(p) {
				var changed = p.isDefaultValue === false;
				var changedIcon = changed ? '<span style="color:#dc3545;">●</span>' : '<span style="color:#ccc;">–</span>';
				var valStyle = changed ? 'font-weight:600;color:#0d6efd;' : '';
				html += '<tr>'
					+ '<td style="white-space:nowrap;">' + escHtml(p.name || '') + '</td>'
					+ '<td style="' + valStyle + '">' + renderCell(p.value) + '</td>'
					+ '<td style="color:#888;">' + renderCell(p.defaultValue) + '</td>'
					+ '<td style="text-align:center;">' + changedIcon + '</td>'
					+ '<td style="color:#999;font-size:0.88em;font-family:monospace;word-break:break-all;">' + escHtml(p.property || '') + '</td>'
					+ '<td style="color:#666;font-size:0.92em;">' + escHtml(p.description || '') + '</td>'
					+ '</tr>';
			});
			html += '</tbody></table>';
		} else {
			html += '<div class="text-muted" style="font-size:0.88em;">No parameters for this alarm.</div>';
		}
		html += '</div>';
	});

	html += '</div>';
	return html;
}

