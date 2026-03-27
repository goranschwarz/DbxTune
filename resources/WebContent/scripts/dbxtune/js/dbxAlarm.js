/**
 * dbxAlarm.js  — Alarm-related UI components for DbxCentral
 * Shared between index.html, graph.html, and any future alarm-displaying pages.
 *
 * Dependencies: jQuery, Bootstrap modal, moment (timestamp formatting),
 *               Font Awesome icons, dbxcentral.utils.js (provides escHtml)
 *
 * On DOMReady this script injects the three alarm modal dialogs into <body>
 * (if not already present) so pages do not need to duplicate that HTML.
 */

// ── Modal HTML injection ────────────────────────────────────────────────────

$(function()
{
	// Extended Description viewer
	if ($('#dbx-view-extDescTable-dialog').length === 0) {
		$('body').append([
			"<div class='modal fade' id='dbx-view-extDescTable-dialog' role='dialog'",
			"     aria-labelledby='dbx-view-extDescTable-dialog' aria-hidden='true'>",
			"  <div class='modal-dialog modal-dialog-centered mw-100 w-75' role='document'>",
			"    <div class='modal-content'>",
			"      <div class='modal-header'>",
			"        <h5 class='modal-title'>",
			"          <b><span id='dbx-view-extDescTable-label'></span></b>",
			"          <span id='dbx-view-extDescTable-objectName'></span>",
			"        </h5>",
			"        <button type='button' class='close' data-dismiss='modal' aria-label='Close'>",
			"          <span aria-hidden='true'>&times;</span>",
			"        </button>",
			"      </div>",
			"      <div class='modal-body' style='overflow-x:auto;'>",
			"        <div class='scroll-tree' style='width:3000px;'>",
			"          <div id='dbx-view-extDescTable-content' class='dbx-view-extDescTable-content'></div>",
			"        </div>",
			"      </div>",
			"      <div class='modal-footer'>",
			"        <button type='button' class='btn btn-secondary' data-dismiss='modal'>Close</button>",
			"      </div>",
			"    </div>",
			"  </div>",
			"</div>"
		].join('\n'));
	}

	// Alarm detail viewer
	if ($('#dbx-view-alarmView-dialog').length === 0) {
		$('body').append([
			"<div class='modal fade' id='dbx-view-alarmView-dialog' role='dialog'",
			"     aria-labelledby='dbx-view-alarmView-dialog' aria-hidden='true'>",
			"  <div class='modal-dialog modal-dialog-centered mw-100 w-75' role='document'>",
			"    <div class='modal-content'>",
			"      <div class='modal-header'>",
			"        <h5 class='modal-title'>",
			"          <b><span id='dbx-view-alarmView-label'></span></b>",
			"          <span id='dbx-view-alarmView-objectName'></span>",
			"        </h5>",
			"        <button type='button' class='close' data-dismiss='modal' aria-label='Close'>",
			"          <span aria-hidden='true'>&times;</span>",
			"        </button>",
			"      </div>",
			"      <div class='modal-body' style='overflow-x:auto;overflow-y:auto;max-height:55vh;'>",
			"        <div class='scroll-tree' style='width:3000px;'>",
			"          <div id='dbx-view-alarmView-content' class='dbx-view-alarmView-content'></div>",
			"        </div>",
			"      </div>",
			"      <div class='modal-footer'>",
			"        <button type='button' class='btn btn-secondary' data-dismiss='modal'>Close</button>",
			"      </div>",
			"    </div>",
			"  </div>",
			"</div>"
		].join('\n'));
	}

	// Alarm mute/unmute dialog
	if ($('#alarm-mute-dialog').length === 0) {
		$('body').append([
			"<div class='modal fade' id='alarm-mute-dialog' tabindex='-1' role='dialog'>",
			"  <div class='modal-dialog modal-md' role='document' style='margin-top:70px;'>",
			"    <div class='modal-content'>",
			"      <div class='modal-header' id='alarm-mute-dialog-header'",
			"           style='background:#1e2d40;color:#e8edf2;padding:10px 16px;'>",
			"        <h5 class='modal-title' id='alarm-mute-dialog-title' style='font-size:0.95rem;'>",
			"          <i class='fa fa-bell-slash' style='color:#f0ad4e;margin-right:6px;'></i> Mute Alarm",
			"        </h5>",
			"        <button type='button' class='close' data-dismiss='modal'",
			"                style='color:#e8edf2;opacity:0.8;'>&times;</button>",
			"      </div>",
			"      <div class='modal-body' style='font-size:0.85rem;padding:16px 20px;'>",
			"        <div id='alarm-mute-info'",
			"             style='background:#f8f9fa;border:1px solid #dee2e6;border-radius:6px;",
			"                    padding:9px 12px;margin-bottom:14px;font-size:0.8rem;'></div>",
			"        <div id='alarm-mute-existing' style='display:none;margin-bottom:14px;'>",
			"          <label style='font-weight:600;font-size:0.8rem;color:#495057;'>Muted by</label>",
			"          <div id='alarm-mute-existing-info'",
			"               style='background:#f8f9fa;border:1px solid #dee2e6;border-radius:5px;",
			"                      padding:8px 12px;font-size:0.8rem;'></div>",
			"        </div>",
			"        <div id='alarm-mute-reason-section'>",
			"          <label style='font-weight:600;font-size:0.8rem;color:#495057;margin-bottom:3px;display:block;'>",
			"            Reason for muting <span style='color:#dc3545;'>*</span>",
			"          </label>",
			"          <textarea id='alarm-mute-reason' rows='3'",
			"            style='width:100%;border:1px solid #ced4da;border-radius:5px;",
			"                   padding:7px 10px;font-size:0.82rem;resize:vertical;'",
			"            placeholder='e.g. Known issue - batch job running until 14:00'></textarea>",
			"          <div style='display:flex;align-items:center;gap:10px;margin-top:10px;'>",
			"            <label style='margin:0;font-weight:600;font-size:0.8rem;white-space:nowrap;'>Auto-unmute after:</label>",
			"            <select id='alarm-mute-expires'",
			"                    style='font-size:0.8rem;border:1px solid #ced4da;border-radius:4px;padding:3px 8px;'>",
			"              <option value='0'>Never (permanent)</option>",
			"              <option value='1'>1 hour</option>",
			"              <option value='2'>2 hours</option>",
			"              <option value='4' selected>4 hours</option>",
			"              <option value='8'>8 hours</option>",
			"              <option value='12'>12 hours</option>",
			"              <option value='24'>24 hours</option>",
			"              <option value='eod'>End of day</option>",
			"              <option value='eow'>End of week</option>",
			"              <option value='168'>7 days</option>",
			"            </select>",
			"          </div>",
			"        </div>",
			"        <div style='margin-top:12px;font-size:0.75rem;color:#6c757d;'>",
			"          <i class='fa fa-user'></i> As: <strong id='alarm-mute-user'>anonymous</strong>",
			"        </div>",
			"      </div>",
			"      <div class='modal-footer' style='padding:10px 16px;'>",
			"        <button type='button' class='btn btn-secondary btn-sm' data-dismiss='modal'>Cancel</button>",
			"        <button type='button' class='btn btn-success btn-sm' id='alarm-unmute-btn'",
			"                style='display:none;' onclick='alarmMuteSubmit(\"unmute\")'>",
			"          <i class='fa fa-bell'></i> Unmute Alarm",
			"        </button>",
			"        <button type='button' class='btn btn-warning btn-sm' id='alarm-mute-btn'",
			"                onclick='alarmMuteSubmit(\"mute\")'>",
			"          <i class='fa fa-bell-slash'></i> Mute Alarm",
			"        </button>",
			"      </div>",
			"    </div>",
			"  </div>",
			"</div>"
		].join('\n'));
	}

	// ── Modal event handlers ────────────────────────────────────────────────

	$('#dbx-view-extDescTable-dialog').on('show.bs.modal', function(e) {
		if (!e.relatedTarget) return;
		var data = $(e.relatedTarget).data();
		$('#dbx-view-extDescTable-objectName', this).text(data.objectname || '');
		$('#dbx-view-extDescTable-content',    this).html(data.tooltip   || '');
		$('#dbx-view-extDescTable-label',      this).html(data.label     || '');
		$('#dbx-view-extDescTable-dialog').animate({ scrollTop: 0 }, 'slow');
	});

	$('#dbx-view-alarmView-dialog').on('show.bs.modal', function(e) {
		if (!e.relatedTarget) return; // programmatic call via alarmDetailShowModal() — content already set
		var data = $(e.relatedTarget).data();
		$('#dbx-view-alarmView-objectName', this).text(data.objectname || '');
		$('#dbx-view-alarmView-content',    this).html('Formatting the Alarm Message&hellip;');
		$('#dbx-view-alarmView-label',      this).html(data.label      || '');
		$.ajax({
			url:         '/api/alarm/formatter',
			type:        'post',
			data:        JSON.stringify(data.tooltip),
			contentType: 'application/json; charset=utf-8',
			success: function(resp) { $('#dbx-view-alarmView-content').html(resp); },
			error:   function(xhr)  { $('#dbx-view-alarmView-content').html(xhr.responseText); }
		});
		$('#dbx-view-alarmView-dialog').animate({ scrollTop: 0 }, 'slow');
	});
});

// ── Tooltip div helpers ─────────────────────────────────────────────────────

function createExtDescTableToolTipDiv(data, label)
{
	var div = document.createElement('div');
	div.innerHTML = '&nbsp;';
	div.setAttribute('title',       'Click to Open Dialog...');
	div.setAttribute('data-toggle', 'modal');
	div.setAttribute('data-target', '#dbx-view-extDescTable-dialog');
	div.setAttribute('data-objectname', '');
	div.setAttribute('data-tooltip', data);
	div.setAttribute('data-label',   label);
	return div;
}

function createAlarmInfoToolTipDiv(data, label)
{
	var div = document.createElement('div');
	div.innerHTML = '&nbsp;';
	div.setAttribute('title',       'Click to Open Dialog...');
	div.setAttribute('data-toggle', 'modal');
	div.setAttribute('data-target', '#dbx-view-alarmView-dialog');
	div.setAttribute('data-objectname', '');
	div.setAttribute('data-tooltip', data);
	div.setAttribute('data-label',   label);
	return div;
}

// ── Dark modal theme helper ─────────────────────────────────────────────────

function _dbxModalApplyDark($modal)
{
	var isDark = $('body').hasClass('cs-dark') || $('body').hasClass('dark')
		|| document.documentElement.getAttribute('data-theme') === 'dark'
		|| (window.location && window.location.search.indexOf('cs=dark') >= 0);
	$modal.find('.modal-content').toggleClass('dbx-modal-dark', isDark);
}

// ── Alarm detail modal ──────────────────────────────────────────────────────

/**
 * Open #dbx-view-alarmView-dialog and populate it via /api/alarm/formatter.
 * Works in both index.html and graph.html.
 */
function alarmDetailShowModal(rowJson)
{
	var $modal = $('#dbx-view-alarmView-dialog');
	if ($modal.length === 0) return;

	var topZ = (window._dbxTopZ || 910) + 200;
	$modal.css('z-index', topZ);
	setTimeout(function() { $('.modal-backdrop').css('z-index', topZ - 1); }, 30);
	_dbxModalApplyDark($modal);

	$('#dbx-view-alarmView-label').text('Alarm Detail');
	$('#dbx-view-alarmView-objectName').text('');
	$('#dbx-view-alarmView-content').html('<em style="color:#777">Formatting&hellip;</em>');
	$modal.modal('show');

	$.ajax({
		url:         '/api/alarm/formatter',
		type:        'post',
		data:        rowJson,
		contentType: 'application/json; charset=utf-8',
		success: function(data) { $('#dbx-view-alarmView-content').html(data); },
		error:   function(xhr)  { $('#dbx-view-alarmView-content').html(xhr.responseText || 'Error formatting alarm.'); }
	});
}

// ── Alarm table row/cell callbacks ──────────────────────────────────────────

/**
 * TR callback for alarm tables — severity row-class + muted dimming + click-to-detail.
 * Shared between index.html (active alarms bar) and graph.html (alarm panel).
 */
var alarmTrCallback = function(tr, row)
{
	var sev = (row.severity || '').toUpperCase();
	if      (sev === 'ERROR'   || sev === 'CRITICAL') tr.className += ' alarm-row-error';
	else if (sev === 'WARNING')                        tr.className += ' alarm-row-warning';
	else if (sev === 'INFO'    || sev === 'NOTICE')    tr.className += ' alarm-row-info';

	if (row.isMuted) {
		tr.style.opacity    = '0.45';
		tr.style.background = '#f8f9fa';
	}

	tr.style.cursor = 'pointer';
	tr.title = 'Click to view full alarm details';
	var rowJson = JSON.stringify(row);
	tr.addEventListener('click', function(e) {
		if ($(e.target).closest('[data-toggle="modal"]').length > 0) return;
		alarmDetailShowModal(rowJson);
	});
};

/**
 * TD callback for alarm tables — mute button, severity/action badges,
 * string truncation, timestamp formatting.
 * Shared between index.html and graph.html.
 */
var alarmTdCallback = function(td, metaData, cellContent, rowData)
{
	// Mute / unmute button on the alarmId cell
	if (metaData.columnName === 'alarmId') {
		var muteLabel = rowData.isMuted ? '<i class="fa fa-bell"></i>' : '<i class="fa fa-bell-slash"></i>';
		var muteStyle = rowData.isMuted ? 'background:#6c757d;color:#fff;' : '';
		var btn = document.createElement('button');
		btn.innerHTML = muteLabel;
		btn.title     = rowData.isMuted ? 'Unmute alarm' : 'Mute alarm';
		btn.style.cssText = 'font-size:0.68rem;padding:1px 5px;border-radius:3px;border:1px solid #adb5bd;cursor:pointer;margin-right:5px;' + muteStyle;
		btn.setAttribute('data-toggle', 'modal');
		(function(row) {
			btn.addEventListener('click', function(e) {
				e.stopPropagation();
				alarmShowMuteDialog(row);
			});
		})(rowData);
		td.insertBefore(btn, td.firstChild);
	}

	// Truncate long strings (not timestamp columns)
	var ALARM_TRUNC = 80;
	if (typeof cellContent === 'string' && cellContent.length > ALARM_TRUNC
			&& !(metaData.columnName || '').endsWith('Time'))
	{
		var plain = cellContent.replace(/<[^>]+>/g, '').trim();
		td.textContent = plain.substring(0, ALARM_TRUNC) + '\u2026';
		td.title       = plain.substring(0, 300);
	}

	// Boolean → checkbox image
	if (typeof cellContent === 'boolean') {
		td.innerHTML = '';
		td.className = cellContent ? 'image-checked' : 'image-unchecked';
	}

	// Special tooltip columns (alarmInfo / extendedDescription)
	if (metaData.columnName === 'alarmInfo' && rowData.hasOwnProperty('alarmInfo') && cellContent === true)
		td.appendChild(createAlarmInfoToolTipDiv(JSON.stringify(rowData), 'Alarm Info'));
	if (metaData.columnName === 'hasExtDesc' && rowData.hasOwnProperty('hasExtDesc') && cellContent === true)
		td.appendChild(createExtDescTableToolTipDiv(rowData.extendedDescription, 'Extended Description'));
	if (metaData.columnName === 'hasLastExtDesc' && rowData.hasOwnProperty('hasLastExtDesc') && cellContent === true)
		td.appendChild(createExtDescTableToolTipDiv(rowData.lastExtendedDescription, 'Last Extended Description'));

	// Severity → Bootstrap badge
	if (metaData.columnName === 'severity') {
		var sev = String(cellContent || '').toUpperCase();
		var cls = (sev === 'ERROR' || sev === 'CRITICAL') ? 'danger' : sev === 'WARNING' ? 'warning' : 'info';
		td.innerHTML = '<span class="badge badge-' + cls + '">' + escHtml(String(cellContent)) + '</span>';
	}

	// Action → Bootstrap badge
	if (metaData.columnName === 'action') {
		var act    = String(cellContent || '').toUpperCase();
		var actCls = act === 'CANCEL' ? 'success' : 'danger';
		td.innerHTML = '<span class="badge badge-' + actCls + '">' + escHtml(String(cellContent)) + '</span>';
	}

	// Timestamp columns → YYYY-MM-DD HH:mm:ss
	if (metaData.columnName && metaData.columnName.endsWith('Time') && cellContent && typeof moment !== 'undefined') {
		var m = moment(cellContent);
		if (m.isValid()) td.textContent = m.format('YYYY-MM-DD HH:mm:ss');
	}
};

// ── Mute / Unmute dialog ────────────────────────────────────────────────────

var _alarmMuteCurrentRow = null;

function alarmShowMuteDialog(alarmRow)
{
	_alarmMuteCurrentRow = alarmRow;
	var isMuted = alarmRow.isMuted;

	document.getElementById('alarm-mute-info').innerHTML =
		  '<div style="display:flex;gap:6px;margin-bottom:3px;"><span style="font-weight:600;color:#6c757d;min-width:90px;">ID:</span><code>'          + escHtml(alarmRow.alarmId      || '') + '</code></div>'
		+ '<div style="display:flex;gap:6px;margin-bottom:3px;"><span style="font-weight:600;color:#6c757d;min-width:90px;">Server:</span><span>'       + escHtml(alarmRow.srvName      || '') + '</span></div>'
		+ '<div style="display:flex;gap:6px;margin-bottom:3px;"><span style="font-weight:600;color:#6c757d;min-width:90px;">Severity:</span><span>'      + escHtml(alarmRow.severity     || '') + '</span></div>'
		+ '<div style="display:flex;gap:6px;"><span style="font-weight:600;color:#6c757d;min-width:90px;">Description:</span><span>'                    + escHtml(alarmRow.description  || alarmRow.lastDescription || '') + '</span></div>';

	if (isMuted) {
		document.getElementById('alarm-mute-dialog-title').innerHTML = '<i class="fa fa-bell" style="color:#f0ad4e;margin-right:6px;"></i> Alarm is Muted \u2013 Unmute?';
		document.getElementById('alarm-mute-reason-section').style.display = 'none';
		document.getElementById('alarm-mute-existing').style.display       = 'block';
		document.getElementById('alarm-mute-existing-info').innerHTML =
			  '<div style="display:flex;gap:6px;margin-bottom:3px;"><span style="font-weight:600;color:#6c757d;min-width:70px;">User:</span><span>'    + escHtml(alarmRow.mutedByUser  || '') + '</span></div>'
			+ '<div style="display:flex;gap:6px;margin-bottom:3px;"><span style="font-weight:600;color:#6c757d;min-width:70px;">At:</span><span>'      + escHtml((alarmRow.mutedTime    || '').replace('T',' ').substring(0,19)) + '</span></div>'
			+ '<div style="display:flex;gap:6px;margin-bottom:3px;"><span style="font-weight:600;color:#6c757d;min-width:70px;">Expires:</span><span>' + escHtml(alarmRow.muteExpiresAt ? alarmRow.muteExpiresAt.replace('T',' ').substring(0,19) : 'Never') + '</span></div>'
			+ '<div style="display:flex;gap:6px;"><span style="font-weight:600;color:#6c757d;min-width:70px;">Reason:</span><em>'                     + escHtml(alarmRow.muteReason   || '') + '</em></div>';
		document.getElementById('alarm-mute-btn').style.display   = 'none';
		document.getElementById('alarm-unmute-btn').style.display = '';
	} else {
		document.getElementById('alarm-mute-dialog-title').innerHTML = '<i class="fa fa-bell-slash" style="color:#f0ad4e;margin-right:6px;"></i> Mute Alarm';
		document.getElementById('alarm-mute-reason-section').style.display = 'block';
		document.getElementById('alarm-mute-existing').style.display       = 'none';
		document.getElementById('alarm-mute-reason').value = '';
		document.getElementById('alarm-mute-btn').style.display   = '';
		document.getElementById('alarm-unmute-btn').style.display = 'none';
	}

	var userEl = document.getElementById('dbx-logged-in-user');
	document.getElementById('alarm-mute-user').textContent =
		(userEl && userEl.textContent.trim()) ? userEl.textContent.trim() : 'anonymous';

	$('#alarm-mute-dialog').modal('show');
}

function _alarmMuteCalcHours(val)
{
	var now = new Date();
	if (val === 'eod') {
		var eod = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 23, 59, 59);
		return Math.max(1, Math.ceil((eod - now) / 3600000));
	}
	if (val === 'eow') {
		// End of this Sunday; if today is Sunday, use next Sunday
		var day = now.getDay(); // 0=Sun
		var daysToSunday = day === 0 ? 7 : 7 - day;
		var eow = new Date(now.getFullYear(), now.getMonth(), now.getDate() + daysToSunday, 23, 59, 59);
		return Math.max(1, Math.ceil((eow - now) / 3600000));
	}
	return parseInt(val, 10);
}

function alarmMuteSubmit(action)
{
	if (!_alarmMuteCurrentRow) return;
	var body = {
		alarmId:     _alarmMuteCurrentRow.alarmId,
		srvName:     _alarmMuteCurrentRow.srvName || '',
		action:      action,
		mutedByUser: document.getElementById('alarm-mute-user').textContent || 'anonymous'
	};
	if (action === 'mute') {
		body.reason       = document.getElementById('alarm-mute-reason').value.trim();
		var expiresVal    = document.getElementById('alarm-mute-expires').value;
		var expiresHours  = _alarmMuteCalcHours(expiresVal);
		body.expiresHours = expiresHours > 0 ? expiresHours : null;
	}
	$.ajax({
		url:         'api/alarm/mute',
		type:        'post',
		contentType: 'application/json',
		data:        JSON.stringify(body),
		success: function() {
			$('#alarm-mute-dialog').modal('hide');
			// Refresh whichever alarm list is on this page
			if (typeof dbxTuneCheckActiveAlarms === 'function') dbxTuneCheckActiveAlarms();
			else if (typeof alarmPanelLoadActive === 'function') alarmPanelLoadActive();
		},
		error: function(xhr) { alert('Error: ' + (xhr.responseText || 'Unknown error')); }
	});
}

// ── Shared Alarm Panel ────────────────────────────────────────────────────────
// Injected on DOMReady if #alarm-panel is not already in the page HTML.
// graph.html has it statically; index.html (and future pages) get it injected.

$(function() {
	if ($('#alarm-panel').length === 0) {
		// Inject panel CSS (graph.html has it statically; other pages need it here)
		$('head').append([
			"<style id='alarm-panel-injected-css'>",
			"#alarm-panel {",
			"  position: fixed; bottom: 0; left: 0; width: 100%; height: 20%; min-height: 120px;",
			"  background: #fff; border-top: 2px solid rgba(200,110,0,0.6);",
			"  box-shadow: 0 -4px 12px rgba(0,0,0,0.15); z-index: 910;",
			"  display: flex; flex-direction: column; overflow: hidden;",
			"}",
			"#alarm-panel-header {",
			"  display: flex; align-items: center; cursor: move; padding: 4px 8px;",
			"  background: rgba(255,148,20,0.85); color: #fff;",
			"  border-bottom: 2px solid rgba(200,110,0,0.9); flex-shrink: 0; gap: 6px;",
			"}",
			"#alarm-panel-header label, #alarm-panel-header .close { color: #fff !important; }",
			"#alarm-panel-body { flex: 1; overflow: auto; padding: 4px 8px; font-size: 0.82em; }",
			"#alarm-panel.alarm-dark { background:#1e1e1e; border-top-color:rgba(200,110,0,0.9); }",
			"#alarm-panel.alarm-dark #alarm-history-range { background:rgba(180,90,0,0.6) !important; color:#fff; }",
			"#alarm-panel.alarm-dark #alarm-history-range input { background:#2a1a00; color:#ffe0b0; border-color:#a06000; }",
			"#alarm-panel.alarm-dark #alarm-panel-body { background:#1e1e1e; color:#e0e0e0; }",
			"#alarm-panel.alarm-dark #alarm-panel-body .table { color:#e0e0e0; }",
			"#alarm-panel.alarm-dark #alarm-panel-body .table th { background:#2d2d2d !important; color:#ddd; border-color:#444; }",
			"#alarm-panel.alarm-dark #alarm-panel-body .table td { border-color:#3a3a3a; color:#e0e0e0; background-color:#1e1e1e; }",
			"#alarm-panel.alarm-dark #alarm-panel-body .table-hover tbody tr:hover td { background:#2a2a2a; color:#fff; }",
			"</style>"
		].join('\n'));

		$('body').append([
			"<div id='alarm-panel' class='alarm-panel-class' style='display:none;'>",
			"  <div id='alarm-panel-header' class='alarm-panel-hdr-class'>",
			"    <strong><i class='fa fa-bell'></i> <span id='alarm-panel-title'>Active Alarms</span></strong>",
			"    &nbsp;",
			"    <label style='font-size:0.8em;font-weight:normal;margin:0 8px 0 0;cursor:pointer;' title='Include muted alarms'>",
			"      <input type='checkbox' id='alarm-show-muted-chk' onclick='alarmPanelShowMutedToggle(this);'>",
			"      &nbsp;Show muted",
			"    </label>",
			"    <span style='font-size:0.8em;display:flex;align-items:center;gap:3px;'>",
			"      <button style='font-size:0.85em;height:20px;padding:0 4px;cursor:pointer;' title='Clear filter'",
			"              onclick=\"document.getElementById('alarm-filter-input').value='';alarmPanelFilterInput('');\">&#x2715;</button>",
			"      <span id='alarm-filter-count' style='color:#fff;white-space:nowrap;flex-shrink:0;'></span>",
			"      <span style='white-space:nowrap;'>Filter:</span>",
			"      <input type='text' id='alarm-filter-input' placeholder='regex  or  where col = val'",
			"             style='font-size:0.85em;height:20px;padding:0 4px;width:400px;font-family:monospace;'",
			"             oninput='alarmPanelFilterInput(this.value);'>",
			"    </span>",
			"    <span style='margin-left:auto;'></span>",
			"    <label style='font-size:0.8em;font-weight:normal;margin:0 8px 0 0;cursor:pointer;'",
			"           title='Auto: Open when there are Active Alarms, Close when no alarms'>",
			"      <input type='checkbox' id='alarm-auto-open-chk' name='alarm-auto-open-chk'",
			"             onclick='alarmAutoOpenChkClick(this);' value='auto-open' checked>",
			"      &nbsp;Auto Open",
			"    </label>",
			"    <label style='font-size:0.8em;font-weight:normal;margin:0;cursor:pointer;'",
			"           title='Toggle dark background for the alarm panel'>",
			"      <input type='checkbox' id='alarm-dark-chk' name='alarm-dark-chk'",
			"             onclick='alarmDarkToggle(this.checked);'>",
			"      &nbsp;Dark mode",
			"    </label>",
			"    &nbsp;",
			"    <button type='button' class='close' style='font-size:1.2em;' onclick='alarmPanelToggle();'>&times;</button>",
			"  </div>",
			"  <div id='alarm-history-range' style='display:none;padding:2px 8px;background:rgba(255,148,20,0.55);font-size:0.8em;color:#fff;flex-shrink:0;'>",
			"    Window: &minus;<input type='number' id='alarm-panel-before' min='1' max='1440' value='30'",
			"            style='width:46px;font-size:0.85em;padding:0 2px;height:18px;' onchange='alarmHistoryWindowChanged();'>&nbsp;min",
			"    &nbsp;/&nbsp;",
			"    +<input type='number' id='alarm-panel-after' min='0' max='1440' value='5'",
			"            style='width:46px;font-size:0.85em;padding:0 2px;height:18px;' onchange='alarmHistoryWindowChanged();'>&nbsp;min",
			"  </div>",
			"  <div id='alarm-panel-body'>",
			"    <div id='alarm-panel-content'></div>",
			"  </div>",
			"</div>"
		].join('\n'));
		if ($.fn.draggable) $('#alarm-panel').draggable({ handle: '#alarm-panel-header', containment: 'window' });
		if ($.fn.resizable) $('#alarm-panel').resizable({ handles: 'n,e,s,w,ne,nw,se,sw' });
	}

	// Seed in-memory scroll pos from localStorage (persists across page reloads).
	// Runs unconditionally — graph.html has a static panel so the inject block above is skipped,
	// but the scroll listener must still be attached.
	try {
		var _saved = JSON.parse(localStorage.getItem(_alarmPageKey('alarmPanel-scroll')) || 'null');
		if (_saved) { _alarmScrollPos.top = _saved.top || 0; _alarmScrollPos.left = _saved.left || 0; }
	} catch(e) {}

	// Attach scroll listener once — update in-memory pos immediately (no debounce) so it
	// is never clobbered by a "Loading…" content reset; also persist to localStorage (debounced).
	var _alarmScrollTimer = null;
	$('#alarm-panel-body').on('scroll', function() {
		if (_alarmScrollFrozen) return;   // ignore resets caused by content-shrink during load
		_alarmScrollPos.top  = $(this).scrollTop();
		_alarmScrollPos.left = $(this).scrollLeft();
		clearTimeout(_alarmScrollTimer);
		_alarmScrollTimer = setTimeout(function() {
			try {
				localStorage.setItem(_alarmPageKey('alarmPanel-scroll'),
					JSON.stringify({ top: _alarmScrollPos.top, left: _alarmScrollPos.left }));
			} catch(e) {}
		}, 500);
	});
});

// ── Shared alarm panel state ──────────────────────────────────────────────────
var _alarmPanelOpen    = false;
var _alarmPanelAllData = [];
var _alarmScrollPos    = { top: 0, left: 0 };  // in-memory scroll pos, updated immediately on scroll
var _alarmScrollFrozen = false;                 // true while loading — prevents content-shrink scroll events from clobbering pos

// ── Shared alarm panel functions ──────────────────────────────────────────────

/** Page-specific storage key — keeps graph.html and index.html settings separate. */
function _alarmPageKey(key)
{
	var page = (window.location.pathname.split('/').pop() || 'index').replace('.html', '');
	return page + '_' + key;
}

function alarmDarkToggle(on)
{
	if (on) $('#alarm-panel').addClass('alarm-dark'); else $('#alarm-panel').removeClass('alarm-dark');
	try { localStorage.setItem(_alarmPageKey('alarmPanelDark'), on ? '1' : '0'); } catch(e) {}
}

function alarmAutoOpenChkClick(checkbox)
{
	if (checkbox === null) return;
	getStorage('dbxtune_checkboxes_').set(_alarmPageKey('alarm-auto-open-chk'), checkbox.checked ? 'checked' : 'not');
}

/**
 * Generic toggle — no history support (used by index.html).
 * graph.html overrides this in dbxcentral.graph.js with a history-aware version.
 */
function alarmPanelToggle()
{
	_alarmPanelOpen = !_alarmPanelOpen;
	if (_alarmPanelOpen) {
		var $p = $('#alarm-panel');
		if (!$p.data('positioned')) {
			var h = $p.outerHeight() || Math.round(window.innerHeight * 0.30);
			$p.css({ bottom: '', top: (window.innerHeight - h) + 'px', left: '0px' });
			$p.data('positioned', true);
		}
		var savedDark = null;
		try { savedDark = localStorage.getItem(_alarmPageKey('alarmPanelDark')); } catch(e) {}
		var dark = savedDark !== null ? (savedDark === '1') : false;
		$('#alarm-dark-chk').prop('checked', dark);
		alarmDarkToggle(dark);
		$p.show();
		alarmPanelLoadActive();
	} else {
		$('#alarm-panel').hide();
	}
}

function alarmPanelUpdateBtn(activeCount, mutedCount)
{
	if (activeCount > 0) {
		$('#alarm-btn-icon').removeClass('fa-bell-slash').addClass('fa-bell');
		$('#alarm-panel-btn').addClass('alarm-active');
		$('#alarm-btn-count').text(activeCount).css('display', 'inline-block');
		$('#active-alarms-top').show();
	} else {
		$('#alarm-btn-icon').removeClass('fa-bell').addClass('fa-bell-slash');
		$('#alarm-panel-btn').removeClass('alarm-active');
		$('#alarm-btn-count').css('display', 'none');
		$('#active-alarms-top').hide();
	}
	if (mutedCount > 0) {
		$('#alarm-btn-muted').text(mutedCount).css('display', 'inline-block');
	} else {
		$('#alarm-btn-muted').css('display', 'none');
	}
}

/**
 * Fetch /api/alarm/active and update the panel + button pills.
 * serverList (optional array) filters to those servers only.
 * graph.html overrides this in dbxcentral.graph.js to pass _serverList.
 */
function alarmPanelLoadActive(serverList)
{
	$('#alarm-panel-title').text('Active Alarms');
	$('#alarm-panel-content').html('<em style="color:#777">Loading&hellip;</em>');
	$.ajax({
		url: 'api/alarm/active',
		type: 'get',
		success: function(data) {
			var jsonResp;
			try { jsonResp = JSON.parse(data); } catch(e) {
				$('#alarm-panel-content').html('<em>Parse error</em>'); return;
			}
			if (Array.isArray(serverList) && serverList.length > 0)
				jsonResp = jsonResp.filter(function(e) { return serverList.indexOf(e.srvName) >= 0; });
			var activeCount = Array.isArray(jsonResp) ? jsonResp.filter(function(e) { return !e.isMuted; }).length : 0;
			var mutedCount  = Array.isArray(jsonResp) ? jsonResp.filter(function(e) { return  e.isMuted; }).length : 0;
			alarmPanelUpdateBtn(activeCount, mutedCount);
			alarmPanelRenderActive(jsonResp, null);
		},
		error: function() {
			$('#alarm-panel-content').html('<em style="color:red">Failed to load alarms.</em>');
		}
	});
}

function alarmPanelRenderActive(jsonResp, srvName)
{
	$('#alarm-panel-title').text('Active Alarms');
	_alarmPanelAllData = Array.isArray(jsonResp) ? jsonResp : [];
	alarmPanelApplyFilter();
}

function alarmPanelApplyFilter()
{
	var filterText = ($('#alarm-filter-input').val() || '').trim();
	var showMuted  = $('#alarm-show-muted-chk').prop('checked');

	var data = _alarmPanelAllData;
	if (!showMuted)
		data = data.filter(function(e) { return !e.isMuted; });

	if (filterText) {
		try {
			var re = new RegExp(filterText, 'i');
			data = data.filter(function(e) {
				return Object.values(e).some(function(v) {
					return re.test(v === null || v === undefined ? '' : String(v));
				});
			});
		} catch(ex) { /* invalid regex — show all */ }
	}

	var visibleTotal = showMuted
		? _alarmPanelAllData.length
		: _alarmPanelAllData.filter(function(e) { return !e.isMuted; }).length;
	$('#alarm-filter-count').text(filterText ? (data.length + ' / ' + visibleTotal + ' rows') : (visibleTotal + ' rows'));

	if (data.length === 0) {
		var msg = (_alarmPanelAllData.filter(function(e) { return !e.isMuted; }).length === 0)
			? '<div class="alert alert-success mt-1 mb-0 py-1 px-2" style="font-size:0.9em;">&#10003; No active alarms.</div>'
			: '<div class="alert alert-info mt-1 mb-0 py-1 px-2" style="font-size:0.9em;">No alarms match the filter.</div>';
		$('#alarm-panel-content').html(msg);
		_alarmScrollFrozen = false;  // unfreeze even when no rows
		return;
	}

	var entry = reWriteAlarmEntry(data);
	var tab = jsonToTable(entry, false, alarmTrCallback, alarmTdCallback);
	$('#alarm-panel-content').empty().append(tab);

	// Restore scroll position after browser finishes layout/paint.
	// setTimeout(50) is unreliable for large tables — use double-rAF instead,
	// which guarantees the browser has completed its first paint (layout is done).
	var $body = $('#alarm-panel-body');
	var sTop  = _alarmScrollPos.top;
	var sLeft = _alarmScrollPos.left;
	requestAnimationFrame(function() {
		$body.scrollTop(sTop).scrollLeft(sLeft);
		requestAnimationFrame(function() {
			$body.scrollTop(sTop).scrollLeft(sLeft);
			_alarmScrollFrozen = false;
		});
	});
}

function alarmPanelFilterInput(val)     { alarmPanelApplyFilter(); }
function alarmPanelShowMutedToggle(chk) { alarmPanelApplyFilter(); }

function alarmPanelRevealMuted(event)
{
	event.stopPropagation();
	if (!_alarmPanelOpen) {
		_alarmPanelOpen = true;
		var $p = $('#alarm-panel');
		if (!$p.data('positioned')) {
			var h = $p.outerHeight() || Math.round(window.innerHeight * 0.30);
			$p.css({ bottom: '', top: (window.innerHeight - h) + 'px', left: '0px' });
			$p.data('positioned', true);
		}
		$p.show();
	}
	var chk = document.getElementById('alarm-show-muted-chk');
	if (chk && !chk.checked) { chk.checked = true; alarmPanelShowMutedToggle(chk); }
}
