/** dbxGraphDbmsConfig.js — DBMS Configuration panel for graph.html */
//-----------------------------------------------------------
// DBMS CONFIG PANEL
//-----------------------------------------------------------

// Auto-open restore: fire once on the first live/slider tick after page load
var _dcAutoOpenChecked = false;
var _dcSrvName   = null;   // server name currently displayed
var _dcTimestamp = null;   // timestamp currently displayed
var _dcData      = null;   // last loaded JSON response
var _dcMainTab   = 'params';  // 'params' | 'texts' | 'issues'
var _dcTextTab   = null;   // currently selected configName sub-tab
var _dcFilter    = '';     // active filter text
// History mode cache: config is effectively static within a monitoring session.
// Cache the last fetched response per server so slider ticks don't re-fetch.
var _dcHistoryCache    = null; // { srvName, resolvedTs, data }
var _dcHistoryCacheKey = null; // srvName used as cache key

/** Toggle the DBMS Config panel open/closed */
function dbmsConfigToggle()
{
	var $panel = $('#dbms-config-panel');
	if ($panel.is(':visible')) {
		dbmsConfigClose();
		return;
	}

	// On first open: center horizontally, near the top (just below the navbar)
	if (!_dcSrvName) {
		$panel.css('display', 'flex'); // must be visible to measure width
		var panelW = $panel.outerWidth();
		var left   = Math.max(0, Math.round((window.innerWidth - panelW) / 2));
		$panel.css({ top: '100px', left: left + 'px' });
	}

	$panel.css('display', 'flex');
	try { localStorage.setItem('dbmsConfig-panelOpen', '1'); } catch(e) {}

	// Restore dark mode
	var saved = getStorage('dbxtune_checkboxes_').get('dbms-config-dark-chk');
	var dark  = (saved === 'checked') ? true
	          : (saved === 'not')     ? false
	          : (_colorSchema === 'dark');
	$('#dbms-config-dark').prop('checked', dark);
	if (dark) $panel.addClass('dc-dark'); else $panel.removeClass('dc-dark');

	// Auto-load on every open
	dbmsConfigRefresh();
}

/** Reload DBMS config data for the current (or best-available) server + timestamp */
function dbmsConfigRefresh()
{
	var srv = _dcSrvName || (_serverList && _serverList.length > 0 ? _serverList[0] : null);
	var ts;
	if (isHistoryViewActive()) {
		ts = _dcTimestamp || _cmTimestamp;
		if (!ts) {
			var txt = ($('#dbx-history-slider-center-text').text() || '').trim();
			ts = txt.split(' - ')[0].trim();
		}
	} else {
		ts = _dcTimestamp || moment().format('YYYY-MM-DD HH:mm:ss');
	}
	if (srv && ts && ts.match(/^\d{4}-\d{2}-\d{2}/))
		dbmsConfigLoad(srv, ts);
}

/** Close and reset the DBMS Config panel */
function dbmsConfigClose()
{
	try { localStorage.setItem('dbmsConfig-panelOpen', '0'); } catch(e) {}
	$('#dbms-config-panel').hide();
	_dcSrvName = _dcTimestamp = _dcData = _dcTextTab = null;
	_dcMainTab = 'params';
	_dcFilter  = '';
	$('#dbms-config-text-subtabs').hide().empty();
	$('#dbms-config-filter-bar').hide();
	$('#dbms-config-filter-input').val('');
	$('#dbms-config-filter-count').text('');
	$('#dbms-config-content').html('');
	// Reset main tab active state
	$('#dbms-config-tabs-main .nav-link').removeClass('active');
	$('#dbms-config-tabs-main .nav-link').first().addClass('active');
}

/** Toggle dark mode for the DBMS Config panel */
function dbmsConfigDarkToggle(on)
{
	if (on) $('#dbms-config-panel').addClass('dc-dark');
	else    $('#dbms-config-panel').removeClass('dc-dark');
	getStorage('dbxtune_checkboxes_').set('dbms-config-dark-chk', on ? 'checked' : 'not');
}

/** Load DBMS config data from the API for the given server+timestamp */
function dbmsConfigLoad(srvName, timestamp)
{
	if (!srvName || !timestamp) return;
	_dcSrvName   = srvName;
	_dcTimestamp = timestamp;
	$('#dbms-config-srv').text('[' + srvName + ']');

	// In history mode the config is effectively static within a monitoring session.
	// Serve from cache if the same server was already loaded, avoiding a redundant fetch.
	if (isHistoryViewActive() && _dcHistoryCache && _dcHistoryCacheKey === srvName) {
		_dcData = _dcHistoryCache;
		var captureTs = _dcData.resolvedTs || _dcData.ts || timestamp;
		$('#dbms-config-ts').text('Captured: ' + captureTs);
		dbmsConfigRender(_dcMainTab);
		return;
	}

	$('#dbms-config-ts' ).text('@ ' + timestamp);
	$('#dbms-config-loading').show();
	$('#dbms-config-content').html('');

	$.ajax({
		url:      '/api/cc/mgt/dbms-config',
		data:     { srv: srvName, ts: timestamp },
		dataType: 'text',
		success: function(data) {
			$('#dbms-config-loading').hide();
			try {
				var r = JSON.parse(data);
				if (r.error) { dbmsConfigShowMsg(r.message || r.error); return; }
				_dcData = r;
				// Cache for subsequent slider ticks in history mode
				if (isHistoryViewActive()) {
					_dcHistoryCache    = r;
					_dcHistoryCacheKey = srvName;
				}
				// Show the actual capture time (SessionStartTime) from the response
				var captureTs = r.resolvedTs || r.ts || _dcTimestamp;
				$('#dbms-config-ts').text('Captured: ' + captureTs);
				dbmsConfigRender(_dcMainTab);
			} catch(ex) { dbmsConfigShowMsg('Parse error: ' + ex); }
		},
		error: function(xhr) {
			$('#dbms-config-loading').hide();
			dbmsConfigShowMsg('HTTP ' + xhr.status + ': ' + xhr.statusText);
		}
	});
}

/** Render the currently selected main tab */
function dbmsConfigRender(tab)
{
	_dcMainTab = tab;
	_dcFilter  = '';
	$('#dbms-config-filter-input').val('');
	$('#dbms-config-filter-count').text('');

	// Highlight the correct main tab link
	$('#dbms-config-tabs-main .nav-link').removeClass('active');
	$('#dbms-config-tabs-main .nav-link').each(function() {
		if ($(this).attr('onclick') && $(this).attr('onclick').indexOf("'" + tab + "'") >= 0)
			$(this).addClass('active');
	});

	if (!_dcData) return;

	if (tab === 'params') {
		$('#dbms-config-text-subtabs').hide().empty();
		$('#dbms-config-filter-bar').css('display','flex');
		dbmsConfigRenderParams(_dcData.params);
	} else if (tab === 'texts') {
		$('#dbms-config-filter-bar').hide();
		dbmsConfigRenderTexts(_dcData.texts);
	} else if (tab === 'issues') {
		$('#dbms-config-text-subtabs').hide().empty();
		$('#dbms-config-filter-bar').css('display','flex');
		dbmsConfigRenderIssues(_dcData.issues);
	}
}

/** Handle main tab click */
function dbmsConfigMainTabClick(evt, tab)
{
	evt.preventDefault();
	dbmsConfigRender(tab);
}

// ── Parameters tab ──────────────────────────────────────────────────────────

function dbmsConfigRenderParams(params)
{
	if (!params || !params.columns || params.columns.length === 0) {
		$('#dbms-config-content').html('<div class="p-3 text-muted">No parameter data available.</div>');
		return;
	}
	// Default filter: show only non-default values (if the column exists)
	var hasNonDefault = params.columns.some(function(c) { return c.toLowerCase() === 'nondefault'; });
	var defaultFilter = hasNonDefault ? 'where NonDefault = true' : '';
	_dcFilter = defaultFilter;
	$('#dbms-config-filter-input').val(defaultFilter);
	dbmsConfigApplyFilterToData(params.columns, params.rows, 'dc-params-table', defaultFilter);
}

// ── Text Snippets tab ────────────────────────────────────────────────────────

function dbmsConfigRenderTexts(texts)
{
	var $st = $('#dbms-config-text-subtabs').css('display','flex').empty();

	if (!texts || texts.length === 0) {
		$('#dbms-config-content').html('<div class="p-3 text-muted">No text snippets available.</div>');
		return;
	}

	// Build sub-tabs
	// Restore previously selected sub-tab if it still exists in this data set
	var restoredTab = null;
	if (_dcTextTab) {
		for (var ti = 0; ti < texts.length; ti++) {
			var tn = texts[ti].configName || ('snippet-' + ti);
			if (tn === _dcTextTab) { restoredTab = texts[ti]; break; }
		}
	}

	texts.forEach(function(t, idx) {
		var name  = t.configName || ('snippet-' + idx);
		var label = t.tabLabel   || name;   // human-readable label, falls back to configName
		var isActive = restoredTab ? (name === _dcTextTab) : (idx === 0);
		if (isActive && !restoredTab) _dcTextTab = name;
		$st.append('<li class="nav-item"><a class="nav-link' + (isActive ? ' active' : '') + '" href="#"'
			+ ' data-config-name="' + escHtml(name) + '"'
			+ ' onclick="dbmsConfigTextTabClick(event,\'' + name.replace(/'/g,"\\'") + '\');return false;">'
			+ escHtml(label) + '</a></li>');
	});

	// Show selected tab's content (restored or first)
	var showTab = restoredTab || texts[0];
	if (showTab) dbmsConfigShowTextContent(showTab);
}

function dbmsConfigTextTabClick(evt, configName)
{
	evt.preventDefault();
	_dcTextTab = configName;
	$('#dbms-config-text-subtabs .nav-link').removeClass('active');
	$('#dbms-config-text-subtabs .nav-link[data-config-name="' + configName + '"]').addClass('active');
	if (_dcData && _dcData.texts) {
		var found = null;
		for (var i = 0; i < _dcData.texts.length; i++) {
			if (_dcData.texts[i].configName === configName) { found = _dcData.texts[i]; break; }
		}
		if (found) dbmsConfigShowTextContent(found);
	}
}

/**
 * Render a single text snippet entry.
 * ASCII tables (isql-style) are converted to sortable/filterable HTML tables.
 * Other text is shown in a <pre>.
 */
function dbmsConfigShowTextContent(entry)
{
	var text = entry.configText || '';
	if (!text) {
		$('#dbms-config-content').html('<div class="p-3 text-muted">No content.</div>');
		return;
	}

	var segments = parseAsciiContent(text);
	var html = '';
	segments.forEach(function(seg, idx) {
		if (seg.type === 'table') {
			html += renderAsciiTable(seg, 'dc-ascii-' + idx);
		} else {
			html += '<pre style="white-space:pre-wrap;font-size:0.82em;border:1px solid #dee2e6;border-radius:3px;padding:6px;margin-bottom:6px;">'
				+ escHtml(seg.text) + '</pre>';
		}
	});
	$('#dbms-config-content').html(html || '<div class="p-3 text-muted">Empty.</div>');
}

// ── Issues tab ───────────────────────────────────────────────────────────────

function dbmsConfigRenderIssues(issues)
{
	if (!issues || !issues.columns || issues.columns.length === 0 ||
		(issues.rows && issues.rows.length === 0 && issues.columns.length > 0)) {
		var emptyMsg = (!issues || !issues.columns || issues.columns.length === 0)
			? 'No issues data available.'
			: 'No configuration issues detected.';
		$('#dbms-config-content').html('<div class="p-3 text-muted">' + emptyMsg + '</div>');
		dbmsConfigApplyFilter('');
		return;
	}
	var html = dbmsConfigBuildTable('dc-issues-table', issues.columns, issues.rows);
	$('#dbms-config-content').html(html);
	dbmsConfigApplyFilter('');
}

// ── Shared table builder ─────────────────────────────────────────────────────

/**
 * Build a Bootstrap table with sortable column headers and filter support.
 * The filter bar (#dbms-config-filter-bar) must already be shown before calling.
 */
function dbmsConfigBuildTable(tableId, columns, rows)
{
	var html = '<table id="' + tableId + '" class="table table-sm table-bordered table-hover" style="font-size:0.82em;white-space:nowrap;">';
	html += '<thead class="thead-light"><tr>';
	columns.forEach(function(col, idx) {
		html += '<th style="cursor:pointer;user-select:none;" onclick="dbmsConfigSortTable(\'' + tableId + '\',' + idx + ');">'
			+ escHtml(col) + ' <span class="sort-icon" style="font-size:0.75em;color:#999;"></span></th>';
	});
	html += '</tr></thead><tbody>';
	if (!rows || rows.length === 0) {
		html += '<tr><td colspan="' + columns.length + '" class="text-center text-muted">No rows</td></tr>';
	} else {
		rows.forEach(function(row) {
			html += '<tr>';
			row.forEach(function(val) {
				html += '<td>' + renderCell(val) + '</td>';
			});
			html += '</tr>';
		});
	}
	html += '</tbody></table>';
	return html;
}

/** Sort a table by column index (cycle: asc → desc → original) */
function dbmsConfigSortTable(tableId, colIdx)
{
	var $tbl  = $('#' + tableId);
	var $th   = $tbl.find('thead th').eq(colIdx);
	var cur   = $th.data('sort') || 0;  // 0=none, 1=asc, 2=desc
	var next  = (cur + 1) % 3;
	$th.data('sort', next);

	// Reset other headers
	$tbl.find('thead th').not($th).data('sort', 0)
		.find('.sort-icon').text('');
	$th.find('.sort-icon').text(next === 1 ? ' ▲' : next === 2 ? ' ▼' : '');

	var $tbody = $tbl.find('tbody');
	var rows   = $tbody.find('tr').toArray();

	if (next === 0) {
		// Restore original order (stored as data-orig-idx)
		rows.sort(function(a, b) {
			return ($(a).data('orig-idx') || 0) - ($(b).data('orig-idx') || 0);
		});
	} else {
		rows.sort(function(a, b) {
			var av = $(a).find('td').eq(colIdx).text();
			var bv = $(b).find('td').eq(colIdx).text();
			var an = parseFloat(av), bn = parseFloat(bv);
			if (!isNaN(an) && !isNaN(bn))
				return next === 1 ? an - bn : bn - an;
			return next === 1 ? av.localeCompare(bv) : bv.localeCompare(av);
		});
	}

	// Remember original order on first sort
	$tbody.find('tr').each(function(i) {
		if ($(this).data('orig-idx') === undefined) $(this).data('orig-idx', i);
	});

	$tbody.empty().append(rows);
	// Re-apply filter after sort
	dbmsConfigApplyFilter(_dcFilter);
}

/** Called from the filter input — re-renders the active params/issues table with the new filter */
function dbmsConfigApplyFilter(text)
{
	_dcFilter = text;
	if (!_dcData) return;

	if (_dcMainTab === 'params' && _dcData.params) {
		dbmsConfigApplyFilterToData(_dcData.params.columns, _dcData.params.rows, 'dc-params-table', text);
	} else if (_dcMainTab === 'issues' && _dcData.issues) {
		dbmsConfigApplyFilterToData(_dcData.issues.columns, _dcData.issues.rows, 'dc-issues-table', text);
	}
}

/**
 * Filter columns/rows and rebuild the table inside #dbms-config-content.
 * Supports:
 *   - "where col = val"  SQL-style (reuses cmDetailWhereFilter)
 *   - plain regex against full row text
 */
function dbmsConfigApplyFilterToData(columns, rows, tableId, text)
{
	var result = applyRowFilter(rows || [], columns, text);
	var filteredRows = result.filteredRows;
	var filterError  = result.filterError;
	var filter = (text || '').trim();

	var total   = (rows || []).length;
	var visible = filteredRows.length;

	var html = dbmsConfigBuildTable(tableId, columns, filteredRows);
	$('#dbms-config-content').html(html);

	if (filter)
		$('#dbms-config-filter-count').text(filterError || ('Showing ' + visible + ' / ' + total));
	else
		$('#dbms-config-filter-count').text(total + ' rows');
}

// ── ASCII table parser ───────────────────────────────────────────────────────

/**
 * Parse isql-style ASCII output into an array of segments:
 *   { type: 'table', headers: [...], rows: [[...], ...] }
 *   { type: 'text',  text: '...' }
 *
 * Isql format (example):
 *   colA    colB    colC
 *   ------- ------- ------
 *   value1  value2  value3
 */
/** Split a pipe-delimited row:  |val1|val2|val3|  →  ['val1','val2','val3'] */
function splitPipeRow(line)
{
	var s = line.trim();
	if (s.charAt(0) === '|') s = s.slice(1);
	if (s.charAt(s.length - 1) === '|') s = s.slice(0, -1);
	return s.split('|').map(function(c) { return c.trim(); });
}

/**
 * Parse text that may contain one or more ASCII tables into segments.
 *
 * Handles two formats:
 *
 * 1. Box format (SAP ASE / MySQL isql):
 *      +--------+------+
 *      |ColA    |ColB  |
 *      +--------+------+
 *      |val1    |val2  |
 *      +--------+------+
 *      Rows N              ← optional trailing line
 *
 * 2. Dash-separator format (older isql):
 *      ColA    ColB
 *      ------- ----
 *      val1    val2
 *
 * Returns array of  { type:'table', headers:[...], rows:[[...], ...] }
 *                or { type:'text',  text:'...' }
 */
function parseAsciiContent(text)
{
	if (!text) return [{ type: 'text', text: '' }];

	var lines    = text.split('\n');
	var segments = [];
	var i        = 0;

	function isBoxSep(l)  { return /^\+[-+]+\+\s*$/.test(l.trim()); }
	function isBoxRow(l)  { return /^\|.*\|\s*$/.test(l.trim()); }
	function isDashSep(l) { var t = l.trim(); return t.length > 2 && /^[-\s]+$/.test(t) && t.indexOf('-') >= 0; }

	while (i < lines.length) {
		var trimmed = lines[i].trim();

		// ── Box-style +---+---+ table ────────────────────────────────────
		if (isBoxSep(trimmed)) {
			i++; // skip opening separator

			// Expect header row  |col1|col2|
			if (i < lines.length && isBoxRow(lines[i].trim())) {
				var headers = splitPipeRow(lines[i]);
				i++;

				// Skip separator after header
				if (i < lines.length && isBoxSep(lines[i].trim())) i++;

				// Collect data rows
				var tableRows = [];
				while (i < lines.length) {
					var rl = lines[i].trim();
					if (isBoxSep(rl))      { i++; break; }   // closing separator
					if (isBoxRow(rl))      { tableRows.push(splitPipeRow(lines[i])); i++; }
					else                    break;
				}

				// Skip optional "Rows N" line
				if (i < lines.length && /^Rows\s+\d+/i.test(lines[i].trim())) i++;

				segments.push({ type: 'table', headers: headers, rows: tableRows });
			} else {
				// Lone separator — treat as text
				segments.push({ type: 'text', text: trimmed });
			}
			continue;
		}

		// ── Dash-separator style  header\n-------\ndata ─────────────────
		if (i + 1 < lines.length && isDashSep(lines[i + 1])) {
			var headerLine = lines[i];
			var sepLine    = lines[i + 1];
			var cols = [];
			var dm, dre = /-+/g;
			while ((dm = dre.exec(sepLine)) !== null)
				cols.push({ start: dm.index, end: dm.index + dm[0].length });

			if (cols.length > 0) {
				var headers2 = cols.map(function(c) { return headerLine.substring(c.start, c.end).trim(); });
				i += 2;
				var tableRows2 = [];
				while (i < lines.length) {
					var rl2 = lines[i];
					if (rl2.trim() === '' || isDashSep(rl2)) { if (rl2.trim() === '') i++; break; }
					tableRows2.push(cols.map(function(c) {
						return rl2.length > c.start ? rl2.substring(c.start, Math.min(c.end + 1, rl2.length)).trim() : '';
					}));
					i++;
				}
				segments.push({ type: 'table', headers: headers2, rows: tableRows2 });
				continue;
			}
		}

		// ── Plain text ────────────────────────────────────────────────────
		var textLines = [];
		while (i < lines.length) {
			if (isBoxSep(lines[i].trim())) break;
			if (i + 1 < lines.length && isDashSep(lines[i + 1])) break;
			textLines.push(lines[i]);
			i++;
		}
		var t = textLines.join('\n').trim();
		if (t) segments.push({ type: 'text', text: t });
	}

	return segments.length > 0 ? segments : [{ type: 'text', text: text }];
}

/**
 * Render one parsed ASCII table segment as a sortable/filterable Bootstrap table.
 * @param {object} seg - { type:'table', headers:[...], rows:[[...], ...] }
 * @param {string} tableId - unique HTML id for the <table>
 */
function renderAsciiTable(seg, tableId)
{
	var html = '<div style="margin-bottom:6px;">';

	// Filter row — only shown when table has more than 5 rows
	if (seg.rows.length > 5) {
		html += '<div style="display:flex;align-items:center;gap:4px;margin-bottom:3px;">'
			+ '<span style="font-size:0.8em;color:#6c757d;">Filter:</span>'
			+ '<input type="text" style="font-size:0.8em;height:20px;padding:0 4px;width:200px;font-family:monospace;"'
			+ ' placeholder="regex..." oninput="dbmsAsciiFilter(this,\'' + tableId + '\');">'
			+ '<span id="' + tableId + '-cnt" style="font-size:0.78em;color:#6c757d;"></span>'
			+ '</div>';
	}

	html += '<table id="' + tableId + '" class="table table-sm table-bordered table-hover" style="font-size:0.8em;white-space:nowrap;width:auto;">';
	html += '<thead class="thead-light"><tr>';
	seg.headers.forEach(function(h, idx) {
		html += '<th style="cursor:pointer;user-select:none;" onclick="dbmsAsciiSort(this,\'' + tableId + '\',' + idx + ');">'
			+ escHtml(h) + ' <span class="sort-icon" style="font-size:0.75em;color:#999;"></span></th>';
	});
	html += '</tr></thead><tbody>';
	if (seg.rows.length === 0) {
		html += '<tr><td colspan="' + seg.headers.length + '" class="text-center text-muted">No rows</td></tr>';
	} else {
		seg.rows.forEach(function(row, ri) {
			html += '<tr data-orig="' + ri + '">';
			row.forEach(function(cell) {
				html += '<td>' + renderCell(cell) + '</td>';
			});
			html += '</tr>';
		});
	}
	html += '</tbody></table></div>';
	return html;
}

/** Filter rows of an ASCII-parsed table */
function dbmsAsciiFilter(input, tableId)
{
	var text   = input.value;
	var $rows  = $('#' + tableId + ' tbody tr');
	var total   = $rows.length;
	var visible = 0;
	var re = null;
	if (text) { try { re = new RegExp(text, 'i'); } catch(e) {} }

	$rows.each(function() {
		var show = !re || re.test($(this).text());
		$(this).toggle(show);
		if (show) visible++;
	});

	var $cnt = $('#' + tableId + '-cnt');
	$cnt.text(text ? ('Showing ' + visible + ' / ' + total) : (total + ' rows'));
}

/** Sort an ASCII-parsed table by column */
function dbmsAsciiSort(th, tableId, colIdx)
{
	var $th   = $(th);
	var cur   = parseInt($th.data('sort') || 0);
	var next  = (cur + 1) % 3;
	$th.data('sort', next);

	$('#' + tableId + ' thead th').not($th).each(function() {
		$(this).data('sort', 0);
		$(this).find('.sort-icon').text('');
	});
	$th.find('.sort-icon').text(next === 1 ? ' ▲' : next === 2 ? ' ▼' : '');

	var $tbody = $('#' + tableId + ' tbody');
	var rows   = $tbody.find('tr').toArray();

	if (next === 0) {
		rows.sort(function(a, b) {
			return parseInt($(a).data('orig') || 0) - parseInt($(b).data('orig') || 0);
		});
	} else {
		rows.sort(function(a, b) {
			var av = $(a).find('td').eq(colIdx).text();
			var bv = $(b).find('td').eq(colIdx).text();
			var an = parseFloat(av), bn = parseFloat(bv);
			if (!isNaN(an) && !isNaN(bn))
				return next === 1 ? an - bn : bn - an;
			return next === 1 ? av.localeCompare(bv) : bv.localeCompare(av);
		});
	}
	$tbody.empty().append(rows);
}

/** Show a plain message in the DBMS Config body */
function dbmsConfigShowMsg(msg)
{
	$('#dbms-config-loading').hide();
	$('#dbms-config-content').html('<div style="padding:15px;text-align:center;color:#6c757d;">' + escHtml(msg) + '</div>');
}

/**
 * Called from the history slider when the timestamp changes.
 * Refreshes the DBMS Config panel if it is open.
 */
function dbmsConfigSliderRefresh(ts)
{
	if (!_dcAutoOpenChecked) {
		_dcAutoOpenChecked = true;
		if (!$('#dbms-config-panel').is(':visible')) {
			try {
				if (localStorage.getItem('dbmsConfig-panelOpen') === '1') {
					dbmsConfigToggle();
					return;
				}
			} catch(e) {}
		}
	}
	if ($('#dbms-config-panel').is(':visible') && _dcSrvName && ts) {
		_dcTimestamp = ts;
		$('#dbms-config-ts').text('@ ' + ts);
		dbmsConfigLoad(_dcSrvName, ts);
	}
}

