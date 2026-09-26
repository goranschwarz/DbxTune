/**
 * dbxTimeline.js — timeline (Gantt like) chart used by the SQL Server Job Scheduler Timeline and the
 * DbxCentral User Defined Timeline chart. Replaces the Google Charts Timeline (which needs internet access).
 *
 * Built on vis-timeline (scripts/vis-timeline/8.5.4), which must be loaded BEFORE this file.
 *
 * Usage:
 *     DbxTimeline.create('timeline', rows, { start: '2026-09-25T00:00:00', end: '2026-09-25T23:59:00', ... });
 *
 * Each row is one bar:
 *     key         Row key: bars with the same key are drawn on the same row                       (mandatory)
 *     text        Text on the bar
 *     textHtml    Optional HTML for the bar (sanitized), instead of 'text'. Classes 'dbx-tl-good' / 'dbx-tl-warn' / 'dbx-tl-bad'
 *                 on a <span> give a green / orange / red "pill", readable on any bar color
 *     color       Any CSS color for the bar (the text color is chosen black/white from it)
 *     tooltip     HTML tooltip (sanitized, see XSS_WHITELIST). If missing a simple one is built
 *     start, end  'YYYY-MM-DDTHH:mm:ss' (local time) or epoch ms                                 (mandatory)
 *     label       Text for the row label (default: key)
 *     parentKey   Key of the row this row is nested under. Nested rows are shown when the parent is
 *                 expanded (click the row label or one of the parent's bars). If the parent key does not
 *                 exist the row is a top-level row.
 *     laneKey,    Optional. When ALL nested rows have these, a "Per job" view is offered: one row per
 *     laneSubKey  laneKey (all its top-level bars), expanded into one row per laneSubKey (all nested bars).
 *                 "Per job" is then the default view (until the user picks another, see saveViewPref).
 *     laneSubOrder Optional number to sort the laneSubKey rows (for example the step id)
 *     failed      Optional boolean; counted in a red badge on the "Per job" rows
 *     Any other fields (jobId, stepId, ...) are passed back to the click callbacks.
 *
 * Options:
 *     start, end        Initial visible period
 *     startExpanded     Nested rows expanded at start (default false)
 *     showKeys          Show row labels (default true). Always shown when there are nested rows,
 *                       since the labels are used to expand/collapse
 *     scrollToBottom    Scroll to the last row at start (default false)
 *     nowMarker         Show a vertical 'now' line (default true)
 *     smallFontRows     Use a smaller font when there are more rows than this (default 22)
 *     stateKey          Key for sessionStorage, to keep the expanded rows over reloads/auto-refresh
 *                       (default: location.pathname + location.search). The view (Per execution / Per job) is a
 *                       browser wide preference in localStorage ('dbxtune_timeline_view').
 *     legend            Array of { color, text } shown as a color legend below the chart (default: none)
 *     filter            Initial text for the toolbar's filter box (case insensitive "contains" on row/job/step names
 *                       and bar texts; a matching nested row keeps its parent). Default: what was typed earlier in this tab.
 *     showClickDetails  Show the tooltip of the last clicked bar in a panel below the chart (default true)
 *
 * Clicking a bar or a row label opens a menu: expand/collapse (rows with nested rows), the page's own items, and
 * 'Copy information'. Clicking the arrow of a row label expands/collapses directly.
 * Double clicking a bar or row label expands/collapses its nested rows (on a nested row: collapses its parent).
 *     expandText,       Menu text for expand/collapse (default 'Show child rows' / 'Hide child rows')
 *     collapseText
 *     menuItems(row, ctx)  Returns extra menu items [{ text, action: function() }] for the clicked row
 *                       (for a row label: the first bar of that row). ctx = { view, isParent }
 */

var DbxTimeline = (function () {

	// HTML allowed in tooltips and labels (vis-timeline sanitizes with js-xss).
	// Our tooltips use tables with inline styles, see createUserDefinedTooltip() in the servlets.
	var XSS_WHITELIST = {
		a: ['href', 'target', 'title'], b: [], br: [], code: [], div: ['style', 'class'], em: [], font: ['color'],
		hr: [], i: [], p: ['style'], pre: ['style'], small: [], span: ['style', 'class', 'data-dbx-gid'], strong: [],
		table: ['style', 'class', 'border'], tbody: [], thead: [], th: ['style', 'nowrap', 'colspan'],
		tr: ['style'], td: ['style', 'nowrap', 'colspan', 'class'], u: []
	};

	var STYLE_ID = 'dbx-timeline-style';
	var CSS = ''
		+ '.dbx-tl-toolbar { display:flex; flex-wrap:wrap; gap:4px 6px; align-items:center; margin:4px 0; font-size:13px; }\n'
		+ '.dbx-tl-toolbar .btn-group .btn.active { pointer-events:none; }\n'
		+ '.dbx-tl .vis-timeline { border:1px solid #dee2e6; font-size:12px; }\n'
		+ '.dbx-tl .vis-item { border-radius:0; font-size:11px; border-color:rgba(0,0,0,.35); }\n'
		+ '.dbx-tl .vis-item.vis-range { min-width:2px; }\n'
		+ '.dbx-tl .vis-item.vis-range .vis-item-overflow { overflow:visible; }\n'
		+ '.dbx-tl .vis-item.dbx-tl-clip .vis-item-overflow { overflow:hidden; }\n'
		+ '.dbx-tl .vis-item .vis-item-content { padding:1px 4px; white-space:nowrap; }\n'
		// Dark bar: white text when it stays inside the bar (clipped), otherwise dark text with a white halo,
		// which is readable both on the bar and on the white background after a short bar
		+ '.dbx-tl .vis-item.dbx-tl-light.dbx-tl-clip .vis-item-content { color:#fff; }\n'
		+ '.dbx-tl .vis-item.dbx-tl-light:not(.dbx-tl-clip) .vis-item-content { color:#212529; text-shadow:0 0 2px #fff, 0 0 3px #fff, 0 0 4px #fff; }\n'
		+ '.dbx-tl .vis-item.vis-selected { box-shadow:0 0 0 2px #212529; z-index:2; }\n'
		+ '.dbx-tl .vis-label .vis-inner { padding:1px 6px; white-space:nowrap; }\n'
		+ '.dbx-tl .vis-label.dbx-tl-parent { cursor:pointer; }\n'
		+ '.dbx-tl .vis-label.dbx-tl-parent:hover, .dbx-tl .vis-label.dbx-tl-clickable:hover { background:#e7f1ff; }\n'
		// Expand/collapse arrow drawn with CSS borders (the unicode triangles render as emoji on Windows)
		+ '.dbx-tl .vis-label.dbx-tl-parent .vis-inner:before { content:""; display:inline-block; width:0; height:0; margin:0 7px 0 1px; vertical-align:1px;'
		+ '    border-style:solid; border-width:4px 0 4px 6px; border-color:transparent transparent transparent #495057; }\n'
		+ '.dbx-tl .vis-label.dbx-tl-parent.dbx-tl-expanded .vis-inner:before { margin:0 6px 0 0; border-width:6px 4px 0 4px; border-color:#495057 transparent transparent transparent; }\n'
		+ '.dbx-tl .vis-label.dbx-tl-child { background:#fafafa; color:#495057; }\n'
		+ '.dbx-tl .vis-label.dbx-tl-child .vis-inner { padding-left:26px; }\n'
		+ '.dbx-tl .vis-label.dbx-tl-clickable { cursor:pointer; }\n'
		+ '.dbx-tl.dbx-tl-small .vis-item  { font-size:10px; }\n'
		+ '.dbx-tl.dbx-tl-small .vis-label { font-size:11px; }\n'
		+ '.dbx-tl .vis-custom-time.dbx-tl-now { background-color:#dc3545; width:2px; }\n'
		// Outside the selected period: hatched grey background, and faded bars
		+ '.dbx-tl .vis-item.vis-background.dbx-tl-outside { border:0; background:repeating-linear-gradient(45deg, rgba(108,117,125,.10) 0 6px, rgba(108,117,125,.20) 6px 12px); }\n'
		+ '.dbx-tl .vis-item.dbx-tl-outside-item { opacity:.45; }\n'
		+ '.dbx-tl.dbx-tl-nokeys .vis-panel.vis-left { display:none; }\n'
		// Our own tooltip: 'position:fixed' on <body>, so it is not cut off by the chart's 'overflow:hidden'
		+ '.dbx-tl-tooltip { position:fixed; z-index:1070; display:none; pointer-events:none; background:#fff; color:#212529; border:1px solid #adb5bd;'
		+ '    box-shadow:0 2px 8px rgba(0,0,0,.25); white-space:nowrap; font-size:12px; max-width:calc(100vw - 16px); max-height:calc(100vh - 16px); overflow:hidden; }\n'
		+ '.dbx-tl-tooltip hr { margin:3px 0; }\n'
		+ '.dbx-tl-menu { position:fixed; z-index:1071; display:none; min-width:200px; max-width:calc(100vw - 16px); padding:4px 0; background:#fff;'
		+ '    border:1px solid rgba(0,0,0,.175); border-radius:6px; box-shadow:0 4px 12px rgba(0,0,0,.2); font-size:13px; }\n'
		+ '.dbx-tl-menu-title { padding:4px 12px 6px; font-weight:600; color:#495057; border-bottom:1px solid #e9ecef; margin-bottom:4px;'
		+ '    white-space:nowrap; overflow:hidden; text-overflow:ellipsis; }\n'
		+ '.dbx-tl-menu-item { display:block; width:100%; padding:4px 12px; border:0; background:none; text-align:left; color:#212529; white-space:nowrap; }\n'
		+ '.dbx-tl-menu-item:hover, .dbx-tl-menu-item:focus { background:#e9ecef; outline:none; }\n'
		+ '.vis-tooltip { background:#fff !important; color:#212529; border:1px solid #adb5bd !important; box-shadow:0 2px 8px rgba(0,0,0,.25);'
		+ '               padding:0 !important; border-radius:0; white-space:nowrap !important; font-size:12px; z-index:1060; }\n'
		+ '.vis-tooltip hr { margin:3px 0; }\n'
		+ '.dbx-tl-legend { display:flex; flex-wrap:wrap; gap:4px 16px; margin:6px 0; font-size:12px; }\n'
		+ '.dbx-tl-legend span { display:inline-flex; align-items:center; gap:5px; }\n'
		+ '.dbx-tl-legend i { width:22px; height:11px; border:1px solid rgba(0,0,0,.3); display:inline-block; }\n'
		+ '.dbx-tl-details { display:none; border:1px solid #dee2e6; border-left:4px solid #0d6efd; border-radius:4px; padding:6px 10px; margin:6px 0; font-size:13px; }\n'
		+ '.dbx-tl-details pre { margin:4px 0 0; font-size:12px; white-space:pre-wrap; max-height:40vh; overflow:auto; }\n'
		+ '.dbx-tl .vis-item .dbx-tl-good, .dbx-tl .vis-item .dbx-tl-warn, .dbx-tl .vis-item .dbx-tl-bad { border-radius:3px; padding:0 3px; text-shadow:none; }\n'
		+ '.dbx-tl .vis-item .dbx-tl-good { background:#d1e7dd; color:#0f5132; border:1px solid #a3cfbb; }\n'
		+ '.dbx-tl .vis-item .dbx-tl-warn { background:#ffe5b4; color:#663c00; border:1px solid #ffc36b; }\n'
		+ '.dbx-tl .vis-item .dbx-tl-bad  { background:#f8d7da; color:#842029; border:1px solid #f1aeb5; }\n'
		+ '.dbx-tl-badge { font-size:10px; background:#6c757d; color:#fff; border-radius:8px; padding:0 6px; margin-left:4px; }\n'
		+ '.dbx-tl-badge.dbx-tl-fail { background:#dc3545; }\n'
		;

	function injectCss()
	{
		if (document.getElementById(STYLE_ID))
			return;
		var style = document.createElement('style');
		style.id = STYLE_ID;
		style.textContent = CSS;
		document.head.appendChild(style);
	}

	function escapeHtml(str)
	{
		return String(str == null ? '' : str)
			.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#39;');
	}

	function toMs(val)
	{
		if (val == null || val === '')
			return null;
		if (typeof val === 'number')
			return val;
		var ms = new Date(val).getTime();
		return isNaN(ms) ? null : ms;
	}

	function fmtTs(ms)
	{
		return moment(ms).format('YYYY-MM-DD HH:mm:ss');
	}

	function fmtDuration(ms)
	{
		var s = Math.max(0, Math.round(ms / 1000));
		var pad = function(n) { return (n < 10 ? '0' : '') + n; };
		return pad(Math.floor(s / 3600)) + ':' + pad(Math.floor(s / 60) % 60) + ':' + pad(s % 60);
	}

	// true if the color is dark (so the bar text should be white)
	var _colorCtx = null;
	var _darkCache = {};
	function isDarkColor(color)
	{
		if (!color)
			return false;
		if (_darkCache[color] !== undefined)
			return _darkCache[color];

		if (_colorCtx === null)
			_colorCtx = document.createElement('canvas').getContext('2d');

		// Let the browser normalize any CSS color (names, rgb(), hsl()) to '#rrggbb' or 'rgba(...)'
		_colorCtx.fillStyle = '#000001';
		_colorCtx.fillStyle = color;
		var c = _colorCtx.fillStyle;
		var r, g, b;
		if (c.charAt(0) === '#') {
			r = parseInt(c.substr(1, 2), 16); g = parseInt(c.substr(3, 2), 16); b = parseInt(c.substr(5, 2), 16);
		} else {
			var m = c.match(/\d+(\.\d+)?/g) || [255, 255, 255];
			r = +m[0]; g = +m[1]; b = +m[2];
		}
		var dark = (0.299 * r + 0.587 * g + 0.114 * b) < 140;
		_darkCache[color] = dark;
		return dark;
	}

	function htmlToPlainText(html)
	{
		return String(html || '')
			.replace(/<br\s*\/?>/gi, '\n')
			.replace(/<hr\s*\/?>/gi, '\n-------------------------------------------------------\n')
			.replace(/<\/tr>/gi, '\n')
			.replace(/<[^>]+>/g, '')
			.replace(/&nbsp;/g, ' ')
			.replace(/&emsp;/g, '\t')
			.replace(/&lt;/g, '<').replace(/&gt;/g, '>').replace(/&quot;/g, '"').replace(/&#39;/g, "'").replace(/&#92;/g, '\\')
			.replace(/&amp;/g, '&');
	}

	// Remove everything from the HTML that is not in XSS_WHITELIST (elements and attributes).
	// Elements like script/style are dropped with their content, other unknown elements are replaced by their children.
	var DROP_WITH_CONTENT = { SCRIPT: 1, STYLE: 1, IFRAME: 1, OBJECT: 1, EMBED: 1, TEMPLATE: 1, NOSCRIPT: 1, SVG: 1, MATH: 1 };
	function sanitizeHtml(html)
	{
		var doc = new DOMParser().parseFromString('<body>' + String(html == null ? '' : html) + '</body>', 'text/html');

		function clean(node)
		{
			var children = Array.prototype.slice.call(node.childNodes);
			children.forEach(function (child) {
				if (child.nodeType === 3) // text
					return;
				if (child.nodeType !== 1) { node.removeChild(child); return; }

				var tag = child.tagName.toUpperCase();
				var allowedAttrs = XSS_WHITELIST[tag.toLowerCase()];
				if (DROP_WITH_CONTENT[tag]) { node.removeChild(child); return; }

				clean(child);
				if (!allowedAttrs)
				{
					while (child.firstChild)
						node.insertBefore(child.firstChild, child);
					node.removeChild(child);
					return;
				}
				Array.prototype.slice.call(child.attributes).forEach(function (a) {
					var name = a.name.toLowerCase();
					var val  = a.value || '';
					if (allowedAttrs.indexOf(name) === -1)
						child.removeAttribute(a.name);
					else if (name === 'href' && !/^(https?:|\/|#)/i.test(val.trim()))
						child.removeAttribute(a.name);
					else if (name === 'style' && /url\s*\(|expression\s*\(|javascript:/i.test(val))
						child.removeAttribute(a.name);
				});
			});
		}
		clean(doc.body);
		return doc.body.innerHTML;
	}

	// One tooltip element for the page
	var _tooltipEl = null;
	function getTooltipEl()
	{
		if (_tooltipEl === null)
		{
			_tooltipEl = document.createElement('div');
			_tooltipEl.className = 'dbx-tl-tooltip';
			document.body.appendChild(_tooltipEl);
		}
		return _tooltipEl;
	}
	function showTooltip(html, clientX, clientY)
	{
		if (_menuEl && _menuEl.style.display === 'block') // no tooltip on top of an open click-menu
			return;
		var el = getTooltipEl();
		el.innerHTML = sanitizeHtml(html);
		el.style.display = 'block';
		moveTooltip(clientX, clientY);
	}
	function moveTooltip(clientX, clientY)
	{
		var el = _tooltipEl;
		if (!el || el.style.display !== 'block')
			return;
		var gap = 14, margin = 8;
		var w = el.offsetWidth, h = el.offsetHeight;
		var vw = window.innerWidth, vh = window.innerHeight;

		// Right of the mouse, or left of it if it does not fit. Always inside the window.
		var left = clientX + gap;
		if (left + w > vw - margin)
			left = clientX - gap - w;
		left = Math.max(margin, Math.min(left, vw - margin - w));

		// Below the mouse, or pushed up so the bottom is inside the window
		var top = clientY + gap;
		if (top + h > vh - margin)
			top = vh - margin - h;
		top = Math.max(margin, top);

		el.style.left = left + 'px';
		el.style.top  = top  + 'px';
	}
	function hideTooltip()
	{
		if (_tooltipEl)
			_tooltipEl.style.display = 'none';
	}

	// One click-menu element for the page
	var _menuEl = null;
	function hideMenu()
	{
		if (_menuEl)
			_menuEl.style.display = 'none';
	}
	function showMenu(title, items, clientX, clientY)
	{
		if (_menuEl === null)
		{
			_menuEl = document.createElement('div');
			_menuEl.className = 'dbx-tl-menu';
			_menuEl.setAttribute('role', 'menu');
			document.body.appendChild(_menuEl);

			// Close on: click outside, Escape, scroll/resize
			document.addEventListener('mousedown', function (e) { if (!_menuEl.contains(e.target)) hideMenu(); }, true);
			document.addEventListener('keydown',   function (e) { if (e.key === 'Escape') hideMenu(); });
			window.addEventListener('resize', hideMenu);
			window.addEventListener('scroll', hideMenu, true);
		}
		_menuEl.innerHTML = '';

		var header = document.createElement('div');
		header.className = 'dbx-tl-menu-title';
		header.textContent = title;
		_menuEl.appendChild(header);

		items.forEach(function (item) {
			var btn = document.createElement('button');
			btn.type = 'button';
			btn.className = 'dbx-tl-menu-item';
			btn.setAttribute('role', 'menuitem');
			btn.textContent = item.text;
			btn.addEventListener('click', function () {
				hideMenu();
				item.action();
			});
			_menuEl.appendChild(btn);
		});

		// Place at the mouse, inside the window
		hideTooltip();
		_menuEl.style.display = 'block';
		var margin = 8, w = _menuEl.offsetWidth, h = _menuEl.offsetHeight;
		var left = Math.max(margin, Math.min(clientX + 2, window.innerWidth  - margin - w));
		var top  = clientY + 2;
		if (top + h > window.innerHeight - margin)
			top = clientY - 2 - h;
		top = Math.max(margin, top);
		_menuEl.style.left = left + 'px';
		_menuEl.style.top  = top  + 'px';

		var first = _menuEl.querySelector('.dbx-tl-menu-item');
		if (first)
			first.focus({ preventScroll: true });
	}

	function copyToClipboardFallback(str)
	{
		try {
			var ta = document.createElement('textarea');
			ta.value = str;
			document.body.appendChild(ta);
			ta.select();
			document.execCommand('copy');
			document.body.removeChild(ta);
		} catch (e) {
			console.log('DbxTimeline: unable to copy to clipboard. Caught: ' + e);
		}
	}

	function copyToClipboard(str)
	{
		// navigator.clipboard is async and rejects when the document is not focused (or not a secure context)
		if (navigator.clipboard && window.isSecureContext)
			navigator.clipboard.writeText(str).catch(function () { copyToClipboardFallback(str); });
		else
			copyToClipboardFallback(str);
	}

	function defaultTooltip(row)
	{
		return '<div style="padding:10px;">'
			+ '<b>' + escapeHtml(row.key) + '</b><hr><b>' + escapeHtml(row.text) + '</b><hr>'
			+ '<table>'
			+ '<tr><td nowrap><b>Duration:</b></td><td nowrap>' + fmtDuration(row._end - row._start) + '</td></tr>'
			+ '<tr><td nowrap><b>Start:</b></td><td nowrap>' + fmtTs(row._start) + '</td></tr>'
			+ '<tr><td nowrap><b>End:</b></td><td nowrap>'   + fmtTs(row._end)   + '</td></tr>'
			+ '</table></div>';
	}

	//---------------------------------------------------------------------------------------------
	// State kept in sessionStorage (so 'refresh=##' keeps what the user expanded)
	//---------------------------------------------------------------------------------------------
	function loadState(key)
	{
		try {
			var json = window.sessionStorage.getItem(key);
			return json ? JSON.parse(json) : null;
		} catch (e) {
			return null;
		}
	}
	function saveState(key, state)
	{
		try {
			window.sessionStorage.setItem(key, JSON.stringify(state));
		} catch (e) {
			// ignore (private mode, quota...)
		}
	}

	// The view ('exec' = Per execution, 'lane' = Per job) is a browser wide preference (localStorage),
	// so it is the same in all tabs, dates and servers. The expanded rows are per tab + URL (sessionStorage).
	var VIEW_PREF_KEY = 'dbxtune_timeline_view';
	function loadViewPref()
	{
		try {
			return window.localStorage.getItem(VIEW_PREF_KEY);
		} catch (e) {
			return null;
		}
	}
	function saveViewPref(view)
	{
		try {
			window.localStorage.setItem(VIEW_PREF_KEY, view);
		} catch (e) {
			// ignore (private mode, quota...)
		}
	}

	//---------------------------------------------------------------------------------------------
	// create
	//---------------------------------------------------------------------------------------------
	function create(containerId, rows, opts)
	{
		opts = $.extend({
			start:          null,
			end:            null,
			startExpanded:  false,
			showKeys:       true,
			scrollToBottom: false,
			nowMarker:      true,
			smallFontRows:  22,
			stateKey:       null,
			legend:         null,
			filter:         null,
			showClickDetails: true,
			expandText:     'Show child rows',
			collapseText:   'Hide child rows',
			menuItems:      null
		}, opts || {});

		injectCss();

		var container = document.getElementById(containerId);
		container.innerHTML = '';
		container.classList.add('dbx-tl');

		var stateKey = 'dbxTimeline|' + (opts.stateKey || (location.pathname + location.search));
		var state    = loadState(stateKey) || {};

		//-------------------------------------------------
		// Prepare rows
		var validRows = [];
		(rows || []).forEach(function (row) {
			row._start = toMs(row.start);
			row._end   = toMs(row.end);
			if (row._start == null || row.key == null)
				return;
			if (row._end == null || row._end < row._start)
				row._end = row._start;
			validRows.push(row);
		});
		rows = validRows;

		var keysPresent = {};
		rows.forEach(function (r) { keysPresent[r.key] = true; });
		rows.forEach(function (r) {
			r._isChild = (r.parentKey != null && r.parentKey !== r.key && keysPresent[r.parentKey] === true);
		});
		var hasNesting = rows.some(function (r) { return r._isChild; });
		var hasLanes   = hasNesting && rows.every(function (r) { return !r._isChild || (r.laneKey != null && r.laneSubKey != null); });

		//-------------------------------------------------
		// Toolbar
		var toolbar = document.createElement('div');
		toolbar.className = 'dbx-tl-toolbar';
		var html = '';
		if (hasLanes)
		{
			html += '<div class="btn-group btn-group-sm" role="group">'
			     +  '<button type="button" class="btn btn-outline-secondary" data-dbx-tl="view-exec" title="One row per execution; expand to see its steps">Per execution</button>'
			     +  '<button type="button" class="btn btn-outline-secondary" data-dbx-tl="view-lane" title="One row per job with all executions; expand to see one row per step">Per job</button>'
			     +  '</div>';
		}
		if (hasNesting)
		{
			html += '<button type="button" class="btn btn-sm btn-outline-secondary" data-dbx-tl="expand-all">Expand all</button>'
			     +  '<button type="button" class="btn btn-sm btn-outline-secondary" data-dbx-tl="collapse-all">Collapse all</button>';
		}
		html += '<input type="search" class="form-control form-control-sm" data-dbx-tl-filter placeholder="Filter by name..." '
		     +  '       title="Show only rows (jobs) whose name contains this text (case insensitive). Matching steps keep their job." style="width:200px; display:inline-block;">'
		     +  '<span class="dbx-tl-filter-count text-muted" style="font-size:12px;"></span>'
		     +  '<button type="button" class="btn btn-sm btn-outline-secondary" data-dbx-tl="fit"  title="Show the whole period">Whole period</button>'
		     +  '<button type="button" class="btn btn-sm btn-outline-secondary" data-dbx-tl="last" title="Zoom in on the last 3 hours of the period">Last 3h</button>'
		     +  '<span class="text-muted" style="font-size:12px;">Wheel: scroll rows, Ctrl+wheel: zoom, Shift+wheel or drag: move in time, click: menu, double click: show/hide nested rows</span>';
		toolbar.innerHTML = html;
		container.appendChild(toolbar);

		var chartDiv = document.createElement('div');
		container.appendChild(chartDiv);

		// Legend and "last clicked bar" details, below the chart
		var afterDiv = document.createElement('div');
		container.appendChild(afterDiv);
		if (opts.legend && opts.legend.length > 0)
		{
			var legendDiv = document.createElement('div');
			legendDiv.className = 'dbx-tl-legend';
			legendDiv.innerHTML = opts.legend.map(function (l) {
				return '<span><i style="background:' + escapeHtml(l.color) + '"></i>' + escapeHtml(l.text) + '</span>';
			}).join('');
			afterDiv.appendChild(legendDiv);
		}
		var detailsDiv = document.createElement('div');
		detailsDiv.className = 'dbx-tl-details';
		afterDiv.appendChild(detailsDiv);

		function showClickDetails(row, plainText)
		{
			if (!opts.showClickDetails)
				return;
			detailsDiv.innerHTML = '<b>Clicked:</b> <span></span> &mdash; tooltip copied to the clipboard<pre></pre>';
			detailsDiv.querySelector('span').textContent = row.label || row.key;
			detailsDiv.querySelector('pre').textContent = plainText;
			detailsDiv.style.display = 'block';
		}

		//-------------------------------------------------
		// Build the model for a view:
		//   groups[]: { id, label, order, parentId, childIds[], rows[] }
		function buildModel(view)
		{
			var groups = {};
			var list   = [];
			function getGroup(id, label, parentId)
			{
				var g = groups[id];
				if (!g)
				{
					g = { id: id, label: label, parentId: parentId, childIds: [], rows: [] };
					groups[id] = g;
					list.push(g);
					if (parentId != null)
						groups[parentId].childIds.push(id);
				}
				return g;
			}

			rows.forEach(function (r) {
				if (view === 'lane')
				{
					if (!r._isChild)
					{
						var topId = 'L|' + (r.laneKey != null ? r.laneKey : r.key);
						getGroup(topId, r.laneKey != null ? r.laneKey : (r.label || r.key), null).rows.push(r);
					}
				}
				else
				{
					if (!r._isChild)
						getGroup('K|' + r.key, r.label || r.key, null).rows.push(r);
				}
			});
			rows.forEach(function (r) {
				if (!r._isChild)
					return;
				if (view === 'lane')
				{
					var topId = 'L|' + r.laneKey;
					if (!groups[topId])
						getGroup(topId, r.laneKey, null);
					getGroup(topId + '|' + r.laneSubKey, r.laneSubKey, topId).rows.push(r);
				}
				else
				{
					getGroup('K|' + r.key, r.label || r.key, 'K|' + r.parentKey).rows.push(r);
				}
			});

			// Top-level order = order of first appearance, children right after their parent
			var ordered = [];
			var tops = list.filter(function (g) { return g.parentId == null; });

			// Filter (search box): keep top rows where the row, one of its bars, or one of its nested rows matches
			var allTopCount = tops.length;
			if (filterText)
			{
				var f = filterText.toLowerCase();
				var rowMatches = function (r) {
					return [r.label, r.text, r.laneKey, r.laneSubKey].some(function (s) { return s != null && String(s).toLowerCase().indexOf(f) !== -1; });
				};
				var groupMatches = function (g) {
					return String(g.label).toLowerCase().indexOf(f) !== -1 || g.rows.some(rowMatches);
				};
				tops = tops.filter(function (g) {
					return groupMatches(g) || g.childIds.some(function (cid) { return groupMatches(groups[cid]); });
				});
			}
			if (view === 'lane')
			{
				tops.forEach(function (g) {
					var n = g.rows.length;
					g.badge = n > 0 ? n : null;
					g.fails = g.rows.filter(function (r) { return r.failed === true; }).length;
					g.childIds.sort(function (a, b) { return (groups[a].rows[0].laneSubOrder || 0) - (groups[b].rows[0].laneSubOrder || 0) || (a < b ? -1 : 1); });
				});
			}
			tops.forEach(function (g) {
				ordered.push(g);
				g.childIds.forEach(function (cid) { ordered.push(groups[cid]); });
			});
			ordered.forEach(function (g, i) { g.order = i; });

			return { groups: groups, ordered: ordered, tops: tops, allTopCount: allTopCount };
		}

		//-------------------------------------------------
		// vis DataSets
		var visGroups = new vis.DataSet();
		var visItems  = new vis.DataSet();
		var model     = null;
		var view      = (hasLanes && loadViewPref() !== 'exec') ? 'lane' : 'exec'; // default: 'Per job' (when available)

		// Filter text from the search box: the URL ('filter' option) wins over what was typed earlier in this tab
		var filterText = (opts.filter != null && String(opts.filter).trim() !== '') ? String(opts.filter).trim() : (state.filter || '');
		$(toolbar).find('[data-dbx-tl-filter]').val(filterText);
		var expanded  = {};  // groupId -> true
		var itemSeq   = 0;
		var itemRow   = {};  // visItemId -> row

		function groupLabelHtml(g)
		{
			// 'data-dbx-gid': a click on the label finds its row from the label itself (see groupIdFromLabel)
			var html = '<span data-dbx-gid="' + escapeHtml(g.id) + '">' + escapeHtml(g.label) + '</span>';
			if (g.badge != null)
				html += ' <span class="dbx-tl-badge">' + g.badge + '</span>';
			if (g.fails)
				html += ' <span class="dbx-tl-badge dbx-tl-fail">' + g.fails + ' failed</span>';
			return html;
		}

		function toVisGroup(g)
		{
			var cls = [];
			if (g.childIds.length > 0) { cls.push('dbx-tl-parent'); if (expanded[g.id]) cls.push('dbx-tl-expanded'); }
			if (g.parentId != null)      cls.push('dbx-tl-child');
			if (g.childIds.length === 0) cls.push('dbx-tl-clickable');
			return { id: g.id, content: groupLabelHtml(g), order: g.order, className: cls.join(' ') };
		}

		function toVisItems(g)
		{
			var clip = g.rows.length > 1;
			return g.rows.map(function (r) {
				var id = ++itemSeq;
				itemRow[id] = r;
				var cls = [];
				if (clip)               cls.push('dbx-tl-clip');
				if (opts.start != null && opts.end != null && (r._end <= winStart || r._start >= winEnd))
					cls.push('dbx-tl-outside-item'); // completely outside the selected period
				if (isDarkColor(r.color)) cls.push('dbx-tl-light');
				return {
					id:        id,
					group:     g.id,
					start:     r._start,
					end:       r._end,
					type:      'range',
					content:   r.textHtml ? sanitizeHtml(r.textHtml) : escapeHtml(r.text != null ? r.text : ''),
					style:     r.color ? 'background-color:' + r.color + ';' : '',
					className: cls.join(' ')
				};
			});
		}

		function render()
		{
			hideTooltip(); // rows under the mouse will change
			model = buildModel(view);
			itemSeq = 0; itemRow = {};

			var groupsToAdd = [];
			var itemsToAdd  = [];
			model.ordered.forEach(function (g) {
				if (g.parentId != null && !expanded[g.parentId])
					return;
				groupsToAdd.push(toVisGroup(g));
				Array.prototype.push.apply(itemsToAdd, toVisItems(g));
			});
			// Grey out the time before/after the selected period (background items span all rows)
			if (opts.start != null && opts.end != null)
			{
				var YEAR = 365 * 24 * 3600 * 1000;
				itemsToAdd.push({ id: 'dbx-tl-bg-before', type: 'background', start: winStart - 10 * YEAR, end: winStart, className: 'dbx-tl-outside', content: '' });
				itemsToAdd.push({ id: 'dbx-tl-bg-after',  type: 'background', start: winEnd, end: winEnd + 10 * YEAR,   className: 'dbx-tl-outside', content: '' });
			}
			visItems.clear();
			visGroups.clear();
			visGroups.add(groupsToAdd);
			visItems.add(itemsToAdd);

			var visibleRows = groupsToAdd.length;
			container.classList.toggle('dbx-tl-small', opts.smallFontRows > 0 && visibleRows > opts.smallFontRows);

			// Filter: "N of M" (and highlight the box while a filter is active)
			var countEl = toolbar.querySelector('.dbx-tl-filter-count');
			if (countEl)
				countEl.textContent = filterText ? (model.tops.length + ' of ' + model.allTopCount + (view === 'lane' ? ' jobs' : ' rows')) : '';
			$(toolbar).find('[data-dbx-tl-filter]').toggleClass('border-primary', !!filterText);

			// Toolbar: active view
			$(toolbar).find('[data-dbx-tl=view-exec]').toggleClass('active', view === 'exec');
			$(toolbar).find('[data-dbx-tl=view-lane]').toggleClass('active', view === 'lane');
		}

		function setExpanded(groupId, expand)
		{
			hideTooltip(); // rows under the mouse will change
			var g = model.groups[groupId];
			if (!g || g.childIds.length === 0 || !!expanded[groupId] === !!expand)
				return;

			if (expand)
			{
				expanded[groupId] = true;
				var groupsToAdd = [], itemsToAdd = [];
				g.childIds.forEach(function (cid) {
					groupsToAdd.push(toVisGroup(model.groups[cid]));
					Array.prototype.push.apply(itemsToAdd, toVisItems(model.groups[cid]));
				});
				visGroups.add(groupsToAdd);
				visItems.add(itemsToAdd);
			}
			else
			{
				delete expanded[groupId];
				var childSet = {};
				g.childIds.forEach(function (cid) { childSet[cid] = true; });
				var itemIds = visItems.getIds({ filter: function (it) { return childSet[it.group] === true; } });
				itemIds.forEach(function (id) { delete itemRow[id]; });
				visItems.remove(itemIds);
				visGroups.remove(g.childIds);
			}
			visGroups.update(toVisGroup(g));
		}

		function setAll(expand)
		{
			hideTooltip(); // rows under the mouse will change
			var groupsToAdd = [], itemsToAdd = [], groupsToRemove = [], itemsToRemove = [], parentsToUpdate = [];
			model.tops.forEach(function (g) {
				if (g.childIds.length === 0 || !!expanded[g.id] === !!expand)
					return;
				if (expand)
				{
					expanded[g.id] = true;
					g.childIds.forEach(function (cid) {
						groupsToAdd.push(toVisGroup(model.groups[cid]));
						Array.prototype.push.apply(itemsToAdd, toVisItems(model.groups[cid]));
					});
				}
				else
				{
					delete expanded[g.id];
					Array.prototype.push.apply(groupsToRemove, g.childIds);
				}
				parentsToUpdate.push(g);
			});
			if (!expand && groupsToRemove.length > 0)
			{
				var removeSet = {};
				groupsToRemove.forEach(function (id) { removeSet[id] = true; });
				itemsToRemove = visItems.getIds({ filter: function (it) { return removeSet[it.group] === true; } });
				itemsToRemove.forEach(function (id) { delete itemRow[id]; });
				visItems.remove(itemsToRemove);
				visGroups.remove(groupsToRemove);
			}
			if (expand)
			{
				visGroups.add(groupsToAdd);
				visItems.add(itemsToAdd);
			}
			visGroups.update(parentsToUpdate.map(toVisGroup));
		}

		function persist()
		{
			var s = { expanded: {}, filter: filterText }; // expanded rows per view (the view itself is in localStorage, see saveViewPref)
			s.expanded[view] = Object.keys(expanded);
			var old = loadState(stateKey) || {};
			if (old.expanded)
				Object.keys(old.expanded).forEach(function (v) { if (v !== view) s.expanded[v] = old.expanded[v]; });
			saveState(stateKey, s);
		}

		function initExpanded()
		{
			expanded = {};
			var saved = state.expanded && state.expanded[view];
			var m = buildModel(view);
			if (saved)
			{
				saved.forEach(function (id) { if (m.groups[id] && m.groups[id].childIds.length > 0) expanded[id] = true; });
			}
			else if (opts.startExpanded)
			{
				// all top rows (also the ones hidden by the filter, so clearing the filter shows them expanded)
				Object.keys(m.groups).forEach(function (id) { var g = m.groups[id]; if (g.parentId == null && g.childIds.length > 0) expanded[id] = true; });
			}
		}

		//-------------------------------------------------
		// Timeline
		var winStart = toMs(opts.start);
		var winEnd   = toMs(opts.end);
		if (winStart == null || winEnd == null)
		{
			rows.forEach(function (r) {
				if (winStart == null || r._start < winStart) winStart = r._start;
				if (winEnd   == null || r._end   > winEnd  ) winEnd   = r._end;
			});
			if (winStart == null) { winEnd = Date.now(); winStart = winEnd - 2 * 3600 * 1000; }
		}

		// The container's height is the max for toolbar + chart + legend. Then let the container follow the chart,
		// so the legend (and the click details) are right below a short chart.
		var containerHeight = container.clientHeight;
		var chartHeight = containerHeight > 150 ? (containerHeight - toolbar.offsetHeight - afterDiv.offsetHeight - 8) + 'px' : '80vh';
		container.style.height = 'auto';

		var timeline = new vis.Timeline(chartDiv, visItems, visGroups, {
			start:          winStart,
			end:            winEnd,
			// Do not let the user move/zoom far into the time where we have no data (before/after the period).
			// The end allows +50% for the bar texts, see showPeriod()
			min:            winStart - (winEnd - winStart) * 0.10,
			max:            winEnd   + (winEnd - winStart) * 0.50,
			zoomMin:        60 * 1000,
			stack:          false,
			orientation:    { axis: 'both' },
			maxHeight:      chartHeight,
			verticalScroll: true,
			// Wheel only scrolls the rows (vis zooms on every wheel event unless a zoomKey is set, which felt like
			// "it starts to zoom" when the scroll reached the top/bottom). Ctrl+wheel zooms, Shift+wheel moves in time.
			zoomKey:              'ctrlKey',
			horizontalScroll:     true,
			horizontalScrollKey:  'shiftKey',
			groupOrder:     'order',
			margin:         { item: { horizontal: 0, vertical: 2 } },
			format:         {
				minorLabels: { minute: 'HH:mm', hour: 'HH:mm' },
				majorLabels: { minute: 'ddd D MMM', hour: 'ddd D MMM' }
			},
			xss:            { filterOptions: { whiteList: XSS_WHITELIST } },
			// After vis has drawn its initial window: extend it so the last bar texts fit, and scroll to the bottom
			onInitialDrawComplete: function () {
				showPeriod(winStart);
				if (opts.scrollToBottom)
					scrollVertical(true);
			}
		});

		if (!opts.showKeys && !hasNesting)
			container.classList.add('dbx-tl-nokeys');

		if (opts.nowMarker)
		{
			var now = Date.now();
			timeline.addCustomTime(now, 'dbx-tl-now');
			timeline.setCustomTimeTitle('Now: ' + fmtTs(now), 'dbx-tl-now');
			$(chartDiv).find('.vis-custom-time.dbx-tl-now').length || $(chartDiv).find('.vis-custom-time').addClass('dbx-tl-now');
		}

		initExpanded();
		render();

		// Show the period from 'start', and extend the end so the text of the last bars is not cut off at the right edge
		// (the text of a bar is drawn after/over the bar, and can be much wider than the bar)
		var _measureCtx = null;
		function showPeriod(start)
		{
			var end     = winEnd;
			var center  = chartDiv.querySelector('.vis-panel.vis-center');
			var chartPx = center ? center.clientWidth : 0;
			if (chartPx > 100)
			{
				if (!_measureCtx)
					_measureCtx = document.createElement('canvas').getContext('2d');
				var anyItem = chartDiv.querySelector('.vis-item .vis-item-content');
				var style   = anyItem ? getComputedStyle(anyItem) : null;
				_measureCtx.font = style ? (style.fontSize + ' ' + style.fontFamily) : '11px sans-serif';

				visItems.forEach(function (it) {
					var row = itemRow[it.id];
					if (!row || row._end < start || row._end > winEnd || /dbx-tl-clip/.test(it.className || '') || !row.text)
						return;
					var textPx = _measureCtx.measureText(row.text).width + 16; // + padding and the pills
					if (textPx > chartPx * 0.7)
						textPx = chartPx * 0.7;
					// the text starts at the bar start: end must satisfy  start + (end-start) * (1 - textPx/chartPx) >= barStart
					var needEnd = start + (row._start - start) / (1 - textPx / chartPx);
					if (needEnd > end)
						end = needEnd;
				});
				// never zoom out more than 50% because of text
				end = Math.min(end, winEnd + (winEnd - start) * 0.5);
			}
			timeline.setWindow(start, end, { animation: false });
		}

		//-------------------------------------------------
		// Events
		timeline.on('itemover', function (props) {
			var row = itemRow[props.item];
			if (row && props.event)
				showTooltip(row.tooltip ? row.tooltip : defaultTooltip(row), props.event.clientX, props.event.clientY);
		});
		timeline.on('itemout', hideTooltip);
		chartDiv.addEventListener('mousemove',  function (e) {
			// vis does not always send 'itemout' (for example when rows are added/removed under the mouse)
			if (e.target && e.target.closest && !e.target.closest('.vis-item'))
				hideTooltip();
			else
				moveTooltip(e.clientX, e.clientY);
		});
		chartDiv.addEventListener('mouseleave', hideTooltip);
		chartDiv.addEventListener('wheel',      hideTooltip, { passive: true });

		// Click a bar or a row label: a small menu with what to do with that row
		function openRowMenu(g, row, clientX, clientY)
		{
			var items = [];
			if (g.childIds.length > 0)
			{
				items.push({
					text:   expanded[g.id] ? opts.collapseText : opts.expandText,
					action: function () { setExpanded(g.id, !expanded[g.id]); persist(); }
				});
			}
			if (opts.menuItems)
				Array.prototype.push.apply(items, opts.menuItems(row, { view: view, isParent: g.childIds.length > 0 }) || []);
			items.push({
				text:   'Copy information',
				action: function () {
					var plainText = htmlToPlainText(row.tooltip ? row.tooltip : defaultTooltip(row));
					copyToClipboard(plainText);
					showClickDetails(row, plainText);
				}
			});
			showMenu(g.label, items, clientX, clientY);
		}

		function clientXY(props)
		{
			var ev = props.event && (props.event.srcEvent || props.event);
			if (ev && ev.clientX != null)
				return { x: ev.clientX, y: ev.clientY, target: ev.target };
			return { x: props.pageX - window.scrollX, y: props.pageY - window.scrollY, target: null };
		}

		// What was clicked: { g: group, row, pos, onArrow } or null
		function resolveClick(props)
		{
			var pos = clientXY(props);
			if (props.item != null)
			{
				var row = itemRow[props.item];
				var visItem = visItems.get(props.item);
				var g = visItem ? model.groups[visItem.group] : null;
				return (row && g) ? { g: g, row: row, pos: pos, isItem: true, onArrow: false } : null;
			}
			if (props.what === 'group-label' && props.group != null)
			{
				// vis finds the group from the mouse Y position in the chart area, which can be a different row than the
				// clicked label (if the label panel and the chart are not scrolled the same). Use the clicked label if we can.
				var labelEl = pos.target && pos.target.closest ? pos.target.closest('.vis-label') : null;
				var gidEl   = labelEl ? labelEl.querySelector('[data-dbx-gid]') : null;
				var grp     = model.groups[gidEl ? gidEl.getAttribute('data-dbx-gid') : props.group];
				if (!grp || grp.rows.length === 0 && grp.childIds.length === 0)
					return null;
				var firstRow = grp.rows.length > 0 ? grp.rows[0] : model.groups[grp.childIds[0]].rows[0];
				var onArrow  = grp.childIds.length > 0 && labelEl && (pos.x - labelEl.getBoundingClientRect().left) < 22;
				return { g: grp, row: firstRow, pos: pos, isItem: false, onArrow: !!onArrow };
			}
			return null;
		}

		// Single click: menu (delayed a bit, so a double click does not also open it)
		// Double click: show/hide the nested rows (on a nested row: hide the rows of its parent)
		var DOUBLE_CLICK_MS = 250;
		var pendingMenu = null;

		timeline.on('click', function (props) {
			hideTooltip();
			var c = resolveClick(props);
			if (!c)
				return;
			if (c.isItem)
				showClickDetails(c.row, htmlToPlainText(c.row.tooltip ? c.row.tooltip : defaultTooltip(c.row)));

			// A click on the arrow of a parent row expands/collapses directly (like a tree)
			if (c.onArrow)
			{
				setExpanded(c.g.id, !expanded[c.g.id]);
				persist();
				return;
			}
			clearTimeout(pendingMenu);
			pendingMenu = setTimeout(function () { openRowMenu(c.g, c.row, c.pos.x, c.pos.y); }, DOUBLE_CLICK_MS);
		});

		timeline.on('doubleClick', function (props) {
			clearTimeout(pendingMenu);
			hideMenu();
			var c = resolveClick(props);
			if (!c || c.onArrow) // the two clicks on the arrow have already toggled
				return;
			if (c.g.childIds.length > 0)
				setExpanded(c.g.id, !expanded[c.g.id]);
			else if (c.g.parentId != null)
				setExpanded(c.g.parentId, false);
			else
				return;
			persist();
		});

		// Search box: filter while typing (and 'search' fires when the (x) clear button is clicked)
		var filterTimer = null;
		$(toolbar).on('input search', '[data-dbx-tl-filter]', function () {
			var val = this.value.trim();
			clearTimeout(filterTimer);
			filterTimer = setTimeout(function () {
				if (val === filterText)
					return;
				filterText = val;
				render();
				persist();
			}, 200);
		});

		$(toolbar).on('click', '[data-dbx-tl]', function () {
			switch ($(this).attr('data-dbx-tl'))
			{
				case 'view-exec':    if (view !== 'exec') { view = 'exec'; saveViewPref(view); state = loadState(stateKey) || {}; initExpanded(); render(); persist(); } break;
				case 'view-lane':    if (view !== 'lane') { view = 'lane'; saveViewPref(view); state = loadState(stateKey) || {}; initExpanded(); render(); persist(); } break;
				case 'expand-all':   setAll(true);  persist(); break;
				case 'collapse-all': setAll(false); persist(); break;
				case 'fit':          showPeriod(winStart); break;
				case 'last':         showPeriod(Math.max(winStart, winEnd - 3 * 3600 * 1000)); break;
			}
		});

		// vis-timeline has no public API for vertical scroll; use the internal setter (it clamps to the top/bottom)
		function scrollVertical(toBottom)
		{
			try {
				timeline._setScrollTop(toBottom ? -1e9 : 0);
				timeline.redraw();
			} catch (e) {
				console.log('DbxTimeline: scroll to ' + (toBottom ? 'bottom' : 'top') + ' failed. Caught: ' + e);
			}
		}


		return {
			timeline:       timeline,
			expandAll:      function () { setAll(true);  persist(); },
			collapseAll:    function () { setAll(false); persist(); },
			scrollToTop:    function () { scrollVertical(false); },
			scrollToBottom: function () { scrollVertical(true);  },
			rowCount:       rows.length
		};
	}

	return {
		create:          create,
		htmlToPlainText: htmlToPlainText,
		copyToClipboard: copyToClipboard
	};
})();
