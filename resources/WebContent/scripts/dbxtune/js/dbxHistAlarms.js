/**
 * dbxHistAlarms.js — Historical Alarm Viewer
 *
 * Opens a floating panel showing historical alarms for a specific server,
 * with date-range picker, action filter (Raise/Cancel/All), text filter,
 * detail popup, and a direct link to graph.html at the alarm's time window.
 *
 * Uses a position:fixed floating panel (same pattern as alarm-panel /
 * cm-detail-panel in graph.html) so jQuery UI draggable + resizable work
 * reliably — Bootstrap modals interfere with resizable handles.
 *
 * Dependencies: jQuery, jQuery UI (draggable + resizable),
 *               Bootstrap 4 modal (detail popup only), moment.js,
 *               dbxcentral.utils.js (provides escHtml)
 */

// ── State ────────────────────────────────────────────────────────────────────
var _histAlarmSrv       = null;   // server currently being viewed (null = ALL)
var _histAlarmData      = [];     // raw data from API
var _histAlarmFiltered  = [];     // after action + text filter
var _histAlarmDetailIdx = -1;     // row index shown in detail modal
var _histAlarmDark      = false;  // dark mode toggle
var _histAlarmSrvList   = [];     // cached server list for dropdown
var _histAlarmSortCol   = 'eventTime'; // active sort column key (null = unsorted)
var _histAlarmSortDir   = 'asc';       // 'asc' | 'desc' | 'none'

// ── Panel + detail-modal HTML injection ──────────────────────────────────────
$(function()
{
	if ($('#hist-alarm-panel').length > 0) return;

	$('body').append([
		// ── Backdrop ───────────────────────────────────────────────────────
		"<div id='hist-alarm-backdrop'",
		"     style='display:none;position:fixed;top:0;left:0;width:100%;height:100%;",
		"            background:rgba(0,0,0,0.45);z-index:1040;'",
		"     onclick='histAlarmClose();'>",
		"</div>",

		// ── Floating panel ─────────────────────────────────────────────────
		"<div id='hist-alarm-panel'",
		"     style='display:none;position:fixed;top:65px;left:5%;width:88%;height:72vh;",
		"            z-index:1050;background:#fff;border:1px solid #aaa;border-radius:4px;",
		"            flex-direction:column;box-shadow:0 4px 20px rgba(0,0,0,0.3);'>",

		// Header (drag handle)
		"  <div id='hist-alarm-hdr'",
		"       style='display:flex;align-items:center;gap:8px;flex-shrink:0;",
		"              padding:5px 10px;background:#f8f9fa;border-bottom:1px solid #dee2e6;",
		"              border-radius:4px 4px 0 0;cursor:move;user-select:none;'>",
		"    <span style='font-weight:700;font-size:0.95em;white-space:nowrap;'>",
		"      <i class='fa fa-history'></i>&nbsp; Historical Alarms",
		"    </span>",
		"    <select id='hist-alarm-srv-sel' class='form-control form-control-sm'",
		"            style='width:auto;max-width:280px;'",
		"            onchange='histAlarmSrvChange();'>",
		"      <option value=''>-- All Servers --</option>",
		"    </select>",
		"    <span style='font-size:0.82em;border:1px solid gray;border-radius:5px;",
		"                 margin-left:6px;padding:1px 6px;'>",
		"      Options: &nbsp;",
		"      <label style='margin:0;font-weight:normal;cursor:pointer;'>",
		"        <input type='checkbox' id='hist-alarm-dark-chk'",
		"               onchange='histAlarmDarkToggle(this.checked);'>&nbsp;Dark mode",
		"      </label>",
		"    </span>",
		"    <span style='margin-left:auto;'>",
		"      <button type='button' style='background:none;border:none;font-size:1.3em;",
		"                                   line-height:1;cursor:pointer;padding:0 4px;'",
		"              onclick='histAlarmClose();' title='Close'>&times;</button>",
		"    </span>",
		"  </div>",

		// Body
		"  <div style='display:flex;flex-direction:column;flex:1 1 0;min-height:0;padding:8px;gap:6px;'>",

		// Search bar
		"    <div style='display:flex;flex-wrap:wrap;gap:6px;align-items:center;flex-shrink:0;'>",
		"      <label style='margin:0;font-size:0.9em;font-weight:600;'>Period:</label>",
		"      <select id='hist-alarm-preset' class='form-control form-control-sm' style='width:auto;' onchange='histAlarmSearch();'>",
		"        <option value='today' selected>Today</option>",
		"        <option value='1d'>Last 24 Hours</option>",
		"        <option value='3d'>3 days</option>",
		"        <option value='7d'>7 days</option>",
		"        <option value='14d'>14 days</option>",
		"        <option value='1m'>1 month</option>",
		"        <option value='2m'>2 months</option>",
		"        <option value='3m'>3 months</option>",
		"        <option value='6m'>6 months</option>",
		"        <option value='custom'>Custom range\u2026</option>",
		"      </select>",
		"      <span id='hist-alarm-custom-range'",
		"            style='display:none;align-items:center;gap:4px;flex-wrap:wrap;'>",
		"        <label style='margin:0;font-size:0.9em;'>From:</label>",
		"        <input type='datetime-local' id='hist-alarm-from'",
		"               class='form-control form-control-sm' style='width:auto;'>",
		"        <label style='margin:0;font-size:0.9em;'>To:</label>",
		"        <input type='datetime-local' id='hist-alarm-to'",
		"               class='form-control form-control-sm' style='width:auto;'>",
		"      </span>",
		"      <button class='btn btn-sm btn-primary' onclick='histAlarmSearch();'>",
		"        <i class='fa fa-search'></i> Search",
		"      </button>",
		"      <span style='font-size:0.85em;border:1px solid gray;border-radius:5px;",
		"                   margin-left:6px;padding:1px 6px;display:flex;gap:10px;align-items:center;'>",
		"        <label style='margin:0;cursor:pointer;'>",
		"          <input type='radio' name='hist-alarm-action' value='RAISE' checked",
		"                 onchange='histAlarmApplyFilter();'>&nbsp;Raise",
		"        </label>",
		"        <label style='margin:0;cursor:pointer;'>",
		"          <input type='radio' name='hist-alarm-action' value='CANCEL'",
		"                 onchange='histAlarmApplyFilter();'>&nbsp;Cancel",
		"        </label>",
		"        <label style='margin:0;cursor:pointer;'>",
		"          <input type='radio' name='hist-alarm-action' value='ALL'",
		"                 onchange='histAlarmApplyFilter();'>&nbsp;All",
		"        </label>",
		"      </span>",
		"    </div>",

		// Filter bar
		"    <div style='display:flex;gap:6px;align-items:center;flex-shrink:0;'>",
		"      <input type='text' id='hist-alarm-filter' class='form-control form-control-sm'",
		"             placeholder='regex filter\u2026' style='max-width:300px;'",
		"             oninput='histAlarmApplyFilter();'>",
		"      <button class='btn btn-sm btn-outline-secondary' title='Clear filter'",
		"              onclick=\"document.getElementById('hist-alarm-filter').value='';",
		"                       histAlarmApplyFilter();\">&#x2715;</button>",
		"      <span id='hist-alarm-count' style='font-size:0.85em;color:#888;'></span>",
		"    </div>",

		// Table — scrollable, fills remaining height
		"    <div id='hist-alarm-table-wrap'",
		"         style='overflow:auto;flex:1 1 0;min-height:0;'>",
		"      <span style='color:#888;font-size:0.9em;'>Choose a time range and click Search.</span>",
		"    </div>",

		"  </div>",  // end body
		"</div>",    // end panel

		// ── Detail floating panel ──────────────────────────────────────────
		"<div id='hist-alarm-detail-panel'",
		"     style='display:none;position:fixed;top:80px;left:20%;width:55%;height:65vh;",
		"            z-index:1060;background:#fff;border:1px solid #aaa;border-radius:4px;",
		"            flex-direction:column;box-shadow:0 4px 20px rgba(0,0,0,0.35);'>",
		"  <div id='hist-alarm-detail-hdr'",
		"       style='display:flex;align-items:center;gap:8px;flex-shrink:0;",
		"              padding:5px 10px;background:#f8f9fa;border-bottom:1px solid #dee2e6;",
		"              border-radius:4px 4px 0 0;cursor:move;user-select:none;'>",
		"    <span style='font-weight:700;font-size:0.95em;'>",
		"      <i class='fa fa-info-circle'></i>&nbsp; Alarm Detail",
		"    </span>",
		"    <span style='margin-left:auto;display:flex;gap:6px;align-items:center;'>",
		"      <button class='btn btn-sm btn-primary' id='hist-alarm-detail-graph-btn'",
		"              onclick='histAlarmOpenGraphFromDetail();'>",
		"        <i class='fa fa-external-link'></i>&nbsp; Open in Graph View",
		"      </button>",
		"      <button type='button' style='background:none;border:none;font-size:1.3em;",
		"                                   line-height:1;cursor:pointer;padding:0 4px;'",
		"              onclick='histAlarmDetailClose();' title='Close'>&times;</button>",
		"    </span>",
		"  </div>",
		"  <div style='overflow:auto;flex:1 1 0;min-height:0;padding:8px;'>",
		"    <div id='hist-alarm-detail-body'></div>",
		"  </div>",
		"</div>",

		// ── Styles ─────────────────────────────────────────────────────────
		"<style>",
		"/* Dark mode — main panel */",
		"#hist-alarm-panel.ha-dark { background:#1e1e1e !important; color:#ddd; border-color:#555; }",
		"#hist-alarm-panel.ha-dark #hist-alarm-hdr { background:#2d2d2d !important; border-bottom-color:#444; }",
		"#hist-alarm-panel.ha-dark .form-control,",
		"#hist-alarm-panel.ha-dark select { background:#2d2d2d; color:#ddd; border-color:#555; }",
		"#hist-alarm-panel.ha-dark table  { color:#ddd; }",
		"#hist-alarm-panel.ha-dark thead  { background:#2d2d2d !important; color:#ccc; }",
		"#hist-alarm-panel.ha-dark tbody tr:hover { background:#2a2a2a !important; }",
		"#hist-alarm-panel.ha-dark td,",
		"#hist-alarm-panel.ha-dark th    { border-color:#444 !important; }",
		"/* Dark mode — detail panel */",
		"#hist-alarm-detail-panel.ha-dark { background:#1e1e1e !important; color:#ddd; border-color:#555; }",
		"#hist-alarm-detail-panel.ha-dark #hist-alarm-detail-hdr { background:#2d2d2d !important; border-bottom-color:#444; }",
		"#hist-alarm-detail-panel.ha-dark table { color:#ddd; }",
		"#hist-alarm-detail-panel.ha-dark td { border-color:#444 !important; }",
		"</style>"
	].join('\n'));

	// Show/hide custom date inputs
	$('#hist-alarm-preset').on('change', function() {
		$('#hist-alarm-custom-range').css('display', $(this).val() === 'custom' ? 'flex' : 'none');
	});

	// jQuery UI — main panel
	$('#hist-alarm-panel').draggable({ handle: '#hist-alarm-hdr', scroll: false });
	$('#hist-alarm-panel').resizable({ handles: 'n, e, s, w, ne, nw, se, sw', minWidth: 500, minHeight: 300 });

	// jQuery UI — detail panel
	$('#hist-alarm-detail-panel').draggable({ handle: '#hist-alarm-detail-hdr', scroll: false });
	$('#hist-alarm-detail-panel').resizable({ handles: 'n, e, s, w, ne, nw, se, sw', minWidth: 400, minHeight: 200 });
});

// ── Open / Close ──────────────────────────────────────────────────────────────

function histAlarmOpen(srvName)
{
	// normalise — null / undefined / empty all mean -- All Servers --
	_histAlarmSrv       = (srvName && srvName.trim()) ? srvName.trim() : null;
	_histAlarmData      = [];
	_histAlarmFiltered  = [];
	_histAlarmDetailIdx = -1;
	_histAlarmSortCol   = 'eventTime';
	_histAlarmSortDir   = 'asc';

	$('#hist-alarm-table-wrap').html(
		'<span style="color:#888;font-size:0.9em;"><i class="fa fa-spinner fa-spin"></i> Loading\u2026</span>');
	$('#hist-alarm-count').text('');
	$('#hist-alarm-filter').val('');
	$('input[name="hist-alarm-action"][value="RAISE"]').prop('checked', true);
	$('#hist-alarm-preset').val('today');
	$('#hist-alarm-custom-range').hide();

	$('#hist-alarm-backdrop').show();
	$('#hist-alarm-panel').css('display', 'flex');

	// Populate server dropdown (fetch once, then reuse)
	if (_histAlarmSrvList.length === 0) {
		$.ajax({
			url: 'api/sessions', type: 'GET',
			success: function(raw) {
				try {
					var sessions = JSON.parse(raw);
					if (Array.isArray(sessions)) {
						_histAlarmSrvList = sessions
							.map(function(s) {
								return s.srvSession ? s.srvSession.serverName : (s.serverName || null);
							})
							.filter(function(n) { return n; });
					}
				} catch(e) {}
				_histAlarmPopulateSrvSel(_histAlarmSrv);
				histAlarmSearch();
			},
			error: function() {
				_histAlarmPopulateSrvSel(_histAlarmSrv);
				histAlarmSearch();
			}
		});
	} else {
		_histAlarmPopulateSrvSel(_histAlarmSrv);
		histAlarmSearch();
	}
}

function _histAlarmPopulateSrvSel(selected)
{
	var $sel = $('#hist-alarm-srv-sel');
	$sel.empty().append('<option value="">-- All Servers --</option>');
	_histAlarmSrvList.forEach(function(name) {
		$sel.append('<option value="' + escHtml(name) + '">' + escHtml(name) + '</option>');
	});
	// "DbxCentral" — special hidden server (schema: DbxcLocalMetrics)
	$sel.append('<option value="DbxcLocalMetrics">DbxCentral</option>');
	$sel.val(selected || '');
}

function histAlarmSrvChange()
{
	var val = $('#hist-alarm-srv-sel').val();
	_histAlarmSrv = val || null;
	histAlarmSearch();
}

function histAlarmClose()
{
	$('#hist-alarm-backdrop').hide();
	$('#hist-alarm-panel').hide();
}

// ── Dark mode ─────────────────────────────────────────────────────────────────

function histAlarmDarkToggle(on)
{
	_histAlarmDark = on;
	$('#hist-alarm-panel').toggleClass('ha-dark', on);
	$('#hist-alarm-detail-panel').toggleClass('ha-dark', on);
}

function histAlarmDetailClose()
{
	$('#hist-alarm-detail-panel').hide();
}

// ── Search ────────────────────────────────────────────────────────────────────

function histAlarmSearch()
{
	if (!$('#hist-alarm-panel').is(':visible')) return;

	var startTime, endTime;
	var preset = $('#hist-alarm-preset').val();

	if (preset === 'custom') {
		startTime = ($('#hist-alarm-from').val() || '').replace('T', ' ');
		endTime   = ($('#hist-alarm-to'  ).val() || '').replace('T', ' ');
		if (!startTime || !endTime) { alert('Please enter both From and To dates.'); return; }
	} else if (preset === 'today') {
		startTime = moment().startOf('day').format('YYYY-MM-DD HH:mm:ss');
		endTime   = moment().format('YYYY-MM-DD HH:mm:ss');
	} else {
		var unit  = preset.slice(-1);
		var num   = parseInt(preset, 10);
		var from  = moment().subtract(num, unit === 'm' ? 'months' : 'days');
		startTime = from.format('YYYY-MM-DD HH:mm:ss');
		endTime   = moment().format('YYYY-MM-DD HH:mm:ss');
	}

	$('#hist-alarm-table-wrap').html(
		'<span style="color:#888;font-size:0.9em;"><i class="fa fa-spinner fa-spin"></i> Loading\u2026</span>');
	$('#hist-alarm-count').text('');

	// Fetch history + active alarms in parallel, then match and render
	// _histAlarmSrv === null means -- All Servers --; pass '' to the API
	var srvParam = _histAlarmSrv || '';

	var histDone   = false, activeDone  = false;
	var histData   = [],    activeData  = [];

	function onBothDone() {
		if (!histDone || !activeDone) return;
		_histAlarmData = histData;
		_histAlarmMatchEvents(_histAlarmData, activeData);
		histAlarmApplyFilter();
	}

	$.ajax({
		url:  'api/alarm/history',
		data: { srv: srvParam, startTime: startTime, endTime: endTime },
		type: 'GET',
		success: function(raw) {
			try   { histData = JSON.parse(raw); } catch(e) { histData = []; }
			if (!Array.isArray(histData)) histData = [];
			histDone = true;
			onBothDone();
		},
		error: function(xhr) {
			$('#hist-alarm-table-wrap').html('<span style="color:red;">Error: HTTP ' + xhr.status + '</span>');
			histDone = true; onBothDone();
		}
	});

	$.ajax({
		url:  'api/alarm/active',
		data: { srv: srvParam },
		type: 'GET',
		success: function(raw) {
			try   { activeData = JSON.parse(raw); } catch(e) { activeData = []; }
			if (!Array.isArray(activeData)) activeData = [];
			activeDone = true;
			onBothDone();
		},
		error: function() { activeDone = true; onBothDone(); }
	});
}

// ── Filter ────────────────────────────────────────────────────────────────────

function histAlarmApplyFilter()
{
	var actionFilter = $('input[name="hist-alarm-action"]:checked').val() || 'RAISE';
	var textFilter   = ($('#hist-alarm-filter').val() || '').trim();

	var data = _histAlarmData;

	if (actionFilter !== 'ALL') {
		data = data.filter(function(e) {
			return (e.action || '').toUpperCase() === actionFilter;
		});
	}

	if (textFilter) {
		try {
			var re = new RegExp(textFilter, 'i');
			data = data.filter(function(e) {
				return Object.values(e).some(function(v) {
					return re.test(v == null ? '' : String(v));
				});
			});
		} catch(ex) { /* invalid regex — show all */ }
	}

	_histAlarmFiltered = data;

	var actionTotal = (actionFilter === 'ALL')
		? _histAlarmData.length
		: _histAlarmData.filter(function(e) {
			return (e.action || '').toUpperCase() === actionFilter;
		  }).length;

	$('#hist-alarm-count').text(
		textFilter
			? (data.length + ' / ' + actionTotal + ' rows')
			: (data.length + ' rows'));

	histAlarmRender(data);
}

// ── Sort ──────────────────────────────────────────────────────────────────────

function histAlarmSort(colKey)
{
	if (_histAlarmSortCol === colKey) {
		// Cycle: asc → desc → none (original order)
		if      (_histAlarmSortDir === 'asc' ) { _histAlarmSortDir = 'desc'; }
		else if (_histAlarmSortDir === 'desc') { _histAlarmSortDir = 'none'; _histAlarmSortCol = null; }
		else                                   { _histAlarmSortDir = 'asc';  _histAlarmSortCol = colKey; }
	} else {
		_histAlarmSortCol = colKey;
		_histAlarmSortDir = 'asc';
	}
	histAlarmRender(_histAlarmFiltered);
}

// ── Render table ──────────────────────────────────────────────────────────────

function histAlarmRender(data)
{
	if (data.length === 0) {
		$('#hist-alarm-table-wrap').html(
			'<div class="alert alert-info mt-1 mb-0 py-1 px-2" style="font-size:0.9em;">' +
			'No alarms match the current filter.</div>');
		return;
	}

	// Sort data before rendering (skip when unsorted / original order)
	var sortedData = data.slice();
	if (_histAlarmSortCol && _histAlarmSortDir !== 'none') {
		var sortKey = _histAlarmSortCol;
		var sortDir = _histAlarmSortDir;
		sortedData.sort(function(a, b) {
			var av, bv;
			if (sortKey === '_duration') {
				av = a._durationMs != null ? a._durationMs
				   : a._isActive           ? 1e15
				   : a._closedByRestart    ? 1e16 : 1e17;
				bv = b._durationMs != null ? b._durationMs
				   : b._isActive           ? 1e15
				   : b._closedByRestart    ? 1e16 : 1e17;
			} else if (sortKey === 'eventTime' || sortKey === 'cancelTime') {
				av = a[sortKey] ? moment(a[sortKey]).valueOf() : 0;
				bv = b[sortKey] ? moment(b[sortKey]).valueOf() : 0;
			} else {
				av = (a[sortKey] == null ? '' : String(a[sortKey])).toLowerCase();
				bv = (b[sortKey] == null ? '' : String(b[sortKey])).toLowerCase();
			}
			if (av < bv) return sortDir === 'asc' ? -1 : 1;
			if (av > bv) return sortDir === 'asc' ?  1 : -1;
			return 0;
		});
	}

	var cols = [
		{ key: 'eventTime',   label: 'Time'        },
		{ key: 'serviceName', label: 'Server Name'  },
		{ key: 'action',      label: 'Action'       },
		{ key: '_duration',   label: 'Duration'     },
		{ key: 'severity',    label: 'Severity'     },
		{ key: 'category',    label: 'Category'     },
		{ key: 'state',       label: 'State'        },
		{ key: 'serviceInfo', label: 'CM Name'      },
		{ key: 'alarmClass',  label: 'Alarm Class'  },
		{ key: 'extraInfo',   label: 'Extra Info'   },
		{ key: 'description', label: 'Description'  }
	];

	var html = '<table class="table table-sm table-hover table-bordered mb-0"'
	         + ' style="font-size:0.82em;white-space:nowrap;width:auto;">';
	html += '<thead class="thead-light"><tr>';
	html += '<th style="width:36px;"></th>';  // button column first
	cols.forEach(function(c) {
		var arrow = '';
		if (c.key === _histAlarmSortCol && _histAlarmSortDir !== 'none')
			arrow = ' ' + (_histAlarmSortDir === 'asc' ? '&#9650;' : '&#9660;');
		html += '<th style="cursor:pointer;white-space:nowrap;" onclick="histAlarmSort(\'' + c.key + '\');">'
		      + escHtml(c.label) + arrow + '</th>';
	});
	html += '</tr></thead><tbody>';

	// use the sorted copy for rendering
	data = sortedData;

	data.forEach(function(row, idx)
	{
		var durationHtml = _histAlarmDurationCell(row);
		var sev = (row.severity || '').toUpperCase();
		var rowBg = '';
		if      (sev === 'ERROR'   || sev === 'CRITICAL') rowBg = 'background:rgba(220,50,50,0.08);';
		else if (sev === 'WARNING')                        rowBg = 'background:rgba(255,180,0,0.10);';

		html += '<tr style="cursor:pointer;' + rowBg + '" onclick="histAlarmShowDetail(' + idx + ');">';

		// Open-in-graph button — leftmost
		html += '<td style="text-align:center;padding:2px 4px;">'
		      + '<button class="btn btn-outline-primary btn-sm"'
		      + ' style="padding:2px 6px;"'
		      + ' title="Open in Graph View"'
		      + ' onclick="event.stopPropagation();histAlarmOpenGraph(' + idx + ');">'
		      + '<i class="fa fa-external-link"></i></button>'
		      + '</td>';

		cols.forEach(function(c) {
			if (c.key === '_duration') {
				html += '<td>' + durationHtml + '</td>';
			} else if (c.key === 'action') {
				html += '<td>' + _histAlarmActionBadge(row.action) + '</td>';
			} else if (c.key === 'severity') {
				html += '<td>' + _histAlarmSeverityBadge(row.severity) + '</td>';
			} else if (c.key === 'state') {
				html += '<td>' + _histAlarmStateBadge(row.state) + '</td>';
			} else if (c.key === 'alarmClass') {
				html += '<td><strong>' + escHtml(row.alarmClass || '') + '</strong></td>';
			} else if (c.key === 'eventTime') {
				var ts = row.eventTime ? moment(row.eventTime).format('YYYY-MM-DD HH:mm:ss') : '';
				html += '<td>' + escHtml(ts) + '</td>';
			} else if (c.key === 'description') {
				var val = (row[c.key] == null) ? '' : String(row[c.key]).replace(/<[^>]*>/g, '');
				html += '<td>' + escHtml(val) + '</td>';
			} else {
				var val = (row[c.key] == null) ? '' : String(row[c.key]);
				html += '<td>' + escHtml(val) + '</td>';
			}
		});

		html += '</tr>';
	});

	html += '</tbody></table>';
	$('#hist-alarm-table-wrap').html(html);
}

// ── Detail popup ──────────────────────────────────────────────────────────────

function histAlarmShowDetail(idx)
{
	_histAlarmDetailIdx = idx;
	var row = _histAlarmFiltered[idx];
	if (!row) return;

	var duration = '';
	var durationOrange = false;
	if ((row.action || '').toUpperCase() === 'RAISE') {
		if (row._durationMs != null)
			duration = _histAlarmFmtDuration(row._durationMs);
		else if (row._isActive)
			duration = 'Still Active';
		else if (row._closedByRestart) {
			duration = 'Unknown (Still Active? or Closed by collector restart)';
			durationOrange = true;
		}
	}

	var fields = [
		['Server',               row.srvName,              false],
		['Time',                 row.eventTime  ? moment(row.eventTime ).format('YYYY-MM-DD HH:mm:ss') : '', false],
		['Action',               row.action,               false],
		['Severity',             null,                     false],  // badge
		['Cancel Time',          row.cancelTime ? moment(row.cancelTime).format('YYYY-MM-DD HH:mm:ss') : '', false],
		['Duration',             duration,                 false, durationOrange],
		['Category',             row.category,             false],
		['State',                row.state,                false],
		['Alarm Class',          row.alarmClass,           false],
		['Service Type',         row.serviceType,          false],
		['Service Name',         row.serviceName,          false],
		['CM Name (serviceInfo)',row.serviceInfo,           false],
		['Extra Info',           row.extraInfo,            false],
		['Alarm ID',             row.alarmId,              false],
		['Repeat Count',         row.repeatCnt,            false],
		['Threshold',            row.threshold,            false],
		['Time To Live',         row.timeToLive,           false],
		['Create Time',          row.createTime ? moment(row.createTime).format('YYYY-MM-DD HH:mm:ss') : '', false],
		['Alarm Duration (raw)', row.alarmDuration,        false],
		['Full Duration',        row.fullDuration,         false],
		['Full Duration Adj(s)', row.fullDurationAdjustmentInSec, false],
		['Session Start',        row.SessionStartTime ? moment(row.SessionStartTime).format('YYYY-MM-DD HH:mm:ss') : '', false],
		['Session Sample',       row.SessionSampleTime ? moment(row.SessionSampleTime).format('YYYY-MM-DD HH:mm:ss') : '', false],
		['Description',          row.description,          true ],
		['Last Description',     row.lastDescription,      true ],
		['Extended Description', row.extendedDescription,  true ],
		['Last Ext. Description',row.lastExtendedDescription, true ],
		['Data',                 row.data,                 true ],
		['Last Data',            row.lastData,             true ]
	];

	var html = '<table class="table table-sm table-bordered mb-0" style="font-size:0.85em;">';
	fields.forEach(function(f) {
		var label    = f[0];
		var val      = f[1];
		var isHtml   = f[2];
		var isOrange = !!f[3];

		if (label === 'Severity') {
			html += '<tr>'
			      + '<td style="font-weight:600;white-space:nowrap;width:170px;">Severity</td>'
			      + '<td>' + _histAlarmSeverityBadge(row.severity) + '</td>'
			      + '</tr>';
			return;
		}
		if (label === 'Action') {
			html += '<tr>'
			      + '<td style="font-weight:600;white-space:nowrap;width:170px;">Action</td>'
			      + '<td>' + _histAlarmActionBadge(row.action) + '</td>'
			      + '</tr>';
			return;
		}
		if (label === 'State') {
			if (!row.state) return;
			html += '<tr>'
			      + '<td style="font-weight:600;white-space:nowrap;width:170px;">State</td>'
			      + '<td>' + _histAlarmStateBadge(row.state) + '</td>'
			      + '</tr>';
			return;
		}
		if (val == null || val === '') return;
		var cell = isHtml ? String(val) : escHtml(String(val));
		if (label === 'Alarm Class')
			cell = '<strong>' + escHtml(String(val)) + '</strong>';
		if (isOrange)
			cell = '<span style="color:orange;">' + cell + '</span>';
		html += '<tr>'
		      + '<td style="font-weight:600;white-space:nowrap;width:170px;">' + escHtml(label) + '</td>'
		      + '<td style="white-space:normal;">' + cell + '</td>'
		      + '</tr>';
	});
	html += '</table>';

	$('#hist-alarm-detail-body').html(html);
	$('#hist-alarm-detail-panel').toggleClass('ha-dark', _histAlarmDark);
	$('#hist-alarm-detail-graph-btn').toggle(!!row.eventTime);
	$('#hist-alarm-detail-panel').css('display', 'flex');
}

function histAlarmOpenGraphFromDetail()
{
	if (_histAlarmDetailIdx >= 0) histAlarmOpenGraph(_histAlarmDetailIdx);
}

// ── Open in Graph View ────────────────────────────────────────────────────────

function histAlarmOpenGraph(idx)
{
	var row = _histAlarmFiltered[idx];
	if (!row || !row.eventTime) return;

	// For RAISE: eventTime=raise, cancelTime from matched CANCEL
	// For CANCEL: eventTime=cancel, raise from matched RAISE (back-link)
	var isCancelRow = (row.action || '').toUpperCase() === 'CANCEL';
	var raiseTime   = isCancelRow && row._matchedRaise
	                ? moment(row._matchedRaise.eventTime)
	                : moment(row.eventTime);
	var cancelTime  = isCancelRow
	                ? moment(row.eventTime)
	                : row.cancelTime     ? moment(row.cancelTime)
	                : row._matchedCancel ? moment(row._matchedCancel.eventTime)
	                :                      null;

	var startTime     = raiseTime.clone().subtract(15, 'minutes').format('YYYY-MM-DD HH:mm:ss');
	var endTime       = cancelTime
	                  ? cancelTime.clone().add(15, 'minutes').format('YYYY-MM-DD HH:mm:ss')
	                  : raiseTime.clone().add(2, 'hours').format('YYYY-MM-DD HH:mm:ss');
	var markTime      = raiseTime.clone().add(1, 'minutes').format('YYYY-MM-DD HH:mm:ss');
	var markStartTime = raiseTime.format('YYYY-MM-DD HH:mm:ss');
	var markEndTime   = cancelTime
	                  ? cancelTime.format('YYYY-MM-DD HH:mm:ss')
	                  : moment(endTime, 'YYYY-MM-DD HH:mm:ss').subtract(15, 'minutes').format('YYYY-MM-DD HH:mm:ss');

	var url = '/graph.html?subscribe=false'
	        + '&sessionName='   + encodeURIComponent(row.srvName || _histAlarmSrv)
	        + '&startTime='     + encodeURIComponent(startTime)
	        + '&endTime='       + encodeURIComponent(endTime)
	        + '&markTime='      + encodeURIComponent(markTime)
	        + '&markStartTime=' + encodeURIComponent(markStartTime)
	        + '&markEndTime='   + encodeURIComponent(markEndTime);

	window.open(url, '_blank');
}

// ── Helpers ───────────────────────────────────────────────────────────────────

/**
 * Match each RAISE event to its CANCEL counterpart.
 * Primary key: alarmId (valid within same collector session).
 * Fallback key: alarmClass + serviceName + serviceInfo (for cross-restart cases).
 * Unmatched RAISEs are checked against activeAlarms:
 *   - found in active → _isActive = true
 *   - not found       → _closedByRestart = true (CANCEL probably lost at restart)
 */
/**
 * Match each RAISE event to its nearest CANCEL counterpart.
 *
 * Match each RAISE to its CANCEL.
 *
 * Primary:  alarmId — same UUID on RAISE and CANCEL within the same collector
 *           session (no restart between the two events).
 * Fallback: alarmClass + serviceName + serviceInfo — used when a collector
 *           restart occurred between RAISE and CANCEL, causing new UUIDs.
 *
 * Duration = cancelEvent.eventTime - raiseEvent.eventTime.
 * (alarmDuration on RAISE rows = check-cycle interval, not alarm duration.)
 *
 * Unmatched RAISEs are checked against activeAlarms:
 *   _isActive        = true  → alarm is still open
 *   _closedByRestart = true  → CANCEL was lost at collector restart
 */
function _histAlarmMatchEvents(allData, activeAlarms)
{
	// Build cancel lookups
	var cancelById  = {};   // alarmId  → cancel row
	var cancelByKey = {};   // class|svc|info → list of cancel rows (fallback)

	allData.forEach(function(row) {
		if ((row.action || '').toUpperCase() !== 'CANCEL') return;
		if (row.alarmId) cancelById[row.alarmId] = row;
		var key = _histAlarmEventKey(row);
		if (!cancelByKey[key]) cancelByKey[key] = [];
		cancelByKey[key].push(row);
	});

	// Build active alarm lookup — both by alarmId (primary) and fallback key
	var activeKeys = {};
	var activeById = {};
	if (Array.isArray(activeAlarms)) {
		activeAlarms.forEach(function(a) {
			activeKeys[_histAlarmEventKey(a)] = true;
			if (a.alarmId) activeById[a.alarmId] = true;
		});
	}

	allData.forEach(function(row) {
		if ((row.action || '').toUpperCase() !== 'RAISE') return;

		var raiseMs    = moment(row.eventTime).valueOf();
		var bestCancel = null;
		var bestDiff   = Infinity;

		// Primary: match by alarmId
		if (row.alarmId && cancelById[row.alarmId]) {
			var c    = cancelById[row.alarmId];
			var diff = moment(c.eventTime).valueOf() - raiseMs;
			if (diff >= 0) { bestCancel = c; bestDiff = diff; }
		}

		// Fallback: match by class+service key (nearest CANCEL after RAISE)
		if (!bestCancel) {
			var key = _histAlarmEventKey(row);
			(cancelByKey[key] || []).forEach(function(c) {
				var diff = moment(c.eventTime).valueOf() - raiseMs;
				if (diff >= 0 && diff < bestDiff) { bestDiff = diff; bestCancel = c; }
			});
		}

		var key = _histAlarmEventKey(row);
		row._matchedCancel   = bestCancel;
		row._durationMs      = bestCancel ? bestDiff : null;
		row._isActive        = !bestCancel && (!!activeById[row.alarmId] || !!activeKeys[key]);
		row._closedByRestart = !bestCancel && !row._isActive;

		// Back-link: let the CANCEL row know its RAISE
		if (bestCancel && !bestCancel._matchedRaise)
			bestCancel._matchedRaise = row;
	});
}

function _histAlarmEventKey(row)
{
	return (row.alarmClass  || '') + '|'
	     + (row.serviceName || '') + '|'
	     + (row.serviceInfo || '');
}

function _histAlarmDurationCell(row)
{
	if ((row.action || '').toUpperCase() !== 'RAISE') return '';

	if (row._durationMs != null) {
		var txt = _histAlarmFmtDuration(row._durationMs);
		return escHtml(txt === '0s' ? '?' : txt);
	}
	if (row._isActive)
		return '<span style="color:red;font-weight:600;">Active</span>';
	if (row._closedByRestart)
		return '<span style="color:orange;" title="No CANCEL found — still active? or closed by collector restart">Active?</span>';
	return '';
}

function _histAlarmFmtSeconds(secVal)
{
	var sec = parseInt(secVal, 10);
	if (isNaN(sec)) return String(secVal);
	return _histAlarmFmtDuration(sec * 1000);
}

function _histAlarmFmtDuration(ms)
{
	if (ms < 0) ms = 0;
	var s = Math.floor(ms / 1000);
	var m = Math.floor(s / 60);  s = s % 60;
	var h = Math.floor(m / 60);  m = m % 60;
	var d = Math.floor(h / 24);  h = h % 24;
	var parts = [];
	if (d > 0) parts.push(d + 'd');
	if (h > 0) parts.push(h + 'h');
	if (m > 0) parts.push(m + 'm');
	if (s > 0 || parts.length === 0) parts.push(s + 's');
	return parts.join(' ');
}

function _histAlarmActionBadge(action)
{
	if (!action) return '';
	var a   = action.toUpperCase();
	var cls = a === 'RAISE'  ? 'danger'
	        : a === 'CANCEL' ? 'success'
	        :                  'secondary';
	return '<span class="badge badge-' + cls + '">' + escHtml(action) + '</span>';
}

function _histAlarmStateBadge(state)
{
	if (!state) return '';
	var s   = state.toUpperCase();
	var cls = s === 'UP'       ? 'success'
	        : s === 'AFFECTED' ? 'warning'
	        : s === 'DOWN'     ? 'danger'
	        :                    'secondary';  // UNKNOWN
	return '<span class="badge badge-' + cls + '">' + escHtml(state) + '</span>';
}

function _histAlarmSeverityBadge(sev)
{
	if (!sev) return '';
	var s   = sev.toUpperCase();
	var cls = 'secondary';
	if      (s === 'CRITICAL') cls = 'danger';
	else if (s === 'ERROR')    cls = 'danger';
	else if (s === 'WARNING')  cls = 'warning';
	else if (s === 'INFO')     cls = 'info';
	else if (s === 'NOTICE')   cls = 'primary';
	return '<span class="badge badge-' + cls + '">' + escHtml(sev) + '</span>';
}
