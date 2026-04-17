/* ── dbxGraphPicker.js ──────────────────────────────────────────────
   Graph Picker Modal — self-injectable, usable from any page.

   Usage (any page):
     1. Include this script before </body>  (CSS + HTML are injected automatically)
     2. Open via: dbxOpenGraphPickerModal(serverName, startTime [, options])

   options object (all optional):
     preSelected  — array of tableName strings to pre-check (e.g. from _graphMap)
     target       — '_blank' (default, open new tab) | '_self' (reload same tab)
     endTime      — end-time string to include in built URL
     onOpen(url)  — custom callback instead of window.open / location.href

   Example (index.html server dropdown):
     onclick='dbxOpenGraphPickerModal("MYSERVER", "2h"); return false;'

   Example (graph.html context menu, same-tab with pre-selection):
     dbxOpenGraphPickerModal(srv, startTime, {
         target: '_self', preSelected: _graphMap.map(g => g._fullName), endTime: endTime
     });
──────────────────────────────────────────────────────────────────── */

// ── Module state ─────────────────────────────────────────────────
var dbxGraphPickerData      = [];
var dbxGraphPickerServer    = '';
var dbxGraphPickerStartTime = '';
var dbxGraphPickerSelected  = new Set();
var dbxGraphPickerOrder     = {};   // { tableName: 1-based load order }
var _dbxGpOptions           = {};   // options passed to dbxOpenGraphPickerModal
var _dbxGpPopupTable        = null; // tableName the position popup is open for

// ── Save current visible checkbox states into the Set ────────────
function dbxGpSyncFromDom() {
	$('#dbxGraphPickerList input[type=checkbox]').each(function() {
		if ($(this).is(':checked')) {
			dbxGraphPickerSelected.add($(this).val());
		} else {
			dbxGraphPickerSelected.delete($(this).val());
		}
	});
}

// ── Update selected counts (total + per group) ───────────────────
function dbxGpUpdateCount() {
	dbxGpSyncFromDom();

	$('#dbxGraphPickerSelectedCount').text(dbxGraphPickerSelected.size);

	// Bold + black when something is selected, muted when nothing is
	$('#dbxGraphPickerSelectedCountWrap').toggleClass('dbx-has-selection', dbxGraphPickerSelected.size > 0);

	// Per-group count — bold and dark when non-zero, muted when zero
	$('.dbx-gp-cm-selected-count').each(function() {
		const cmName = $(this).data('cm');
		const count  = $('#dbxGraphPickerList input[type=checkbox][data-cm="' + cmName + '"]:checked').length;
		$(this).text(count);

		if (count > 0) {
			$(this).closest('small')
				.css('color', '#000')
				.css('font-weight', 'bold');
		} else {
			$(this).closest('small')
				.css('color', '')
				.css('font-weight', '');
		}
	});
}

// ── Helper to sync expand/collapse button visibility ─────────────
function dbxGpSyncGroupButtons() {
	const visible  = $('.dbx-gp-cm-body:visible').length;
	const hasOpenGroups = visible > 0;
	$('#dbxGraphPickerExpandAll')  .toggle(!hasOpenGroups);
	$('#dbxGraphPickerCollapseAll').toggle( hasOpenGroups);
}

// ── Order helpers ─────────────────────────────────────────────────

// Reassign consecutive 1, 2, 3... preserving relative order
function dbxGpRenormalize() {
	var sorted = Object.keys(dbxGraphPickerOrder).sort(function(a, b) {
		return dbxGraphPickerOrder[a] - dbxGraphPickerOrder[b];
	});
	dbxGraphPickerOrder = {};
	sorted.forEach(function(t, i) { dbxGraphPickerOrder[t] = i + 1; });
}

// Insert item at newNum, shifting others to make room
function dbxGpMoveToPosition(tableName, newNum) {
	var total = Object.keys(dbxGraphPickerOrder).length;
	newNum    = Math.max(1, Math.min(newNum, total));
	var old   = dbxGraphPickerOrder[tableName];
	if (newNum === old) return;
	Object.keys(dbxGraphPickerOrder).forEach(function(t) {
		if (t === tableName) return;
		if (newNum < old) {
			if (dbxGraphPickerOrder[t] >= newNum && dbxGraphPickerOrder[t] < old) dbxGraphPickerOrder[t]++;
		} else {
			if (dbxGraphPickerOrder[t] > old && dbxGraphPickerOrder[t] <= newNum) dbxGraphPickerOrder[t]--;
		}
	});
	dbxGraphPickerOrder[tableName] = newNum;
}

// Renumber selected items in the order they appear in dbxGraphPickerData
function dbxGpResetToListOrder() {
	var rank = 0;
	dbxGraphPickerOrder = {};
	dbxGraphPickerData.forEach(function(g) {
		if (dbxGraphPickerSelected.has(g.tableName)) {
			dbxGraphPickerOrder[g.tableName] = ++rank;
		}
	});
}

// Push updated numbers from dbxGraphPickerOrder back into badge DOM
function dbxGpRefreshBadges() {
	$('.dbx-gp-order-badge').each(function() {
		var t = $(this).data('table');
		if (dbxGraphPickerOrder[t] !== undefined) $(this).text(dbxGraphPickerOrder[t]);
	});
}

// ── Popup helpers ─────────────────────────────────────────────────

function dbxGpCloseAllPopups() {
	$('#dbx-gp-pos-popup, #dbx-gp-ctx-popup').hide();
	$('.dbx-gp-order-badge').removeClass('active');
	_dbxGpPopupTable = null;
}

function dbxGpOpenPosPopup(badge, tableName) {
	dbxGpCloseAllPopups();
	_dbxGpPopupTable = tableName;
	var total   = Object.keys(dbxGraphPickerOrder).length;
	var current = dbxGraphPickerOrder[tableName];
	var $btns   = $('#dbx-gp-pos-buttons').empty();

	for (var i = 1; i <= total; i++) {
		(function(pos) {
			var cls = (pos === current) ? 'dbx-gp-pos-btn current' : 'dbx-gp-pos-btn';
			$('<button>').addClass(cls).text(pos)
				.on('click', function(e) {
					e.stopPropagation();
					dbxGpMoveToPosition(tableName, pos);
					dbxGpRefreshBadges();
					dbxGpUpdateCount();
					dbxGpCloseAllPopups();
				})
				.appendTo($btns);
		})(i);
	}

	var r = badge.getBoundingClientRect();
	$('#dbx-gp-pos-popup').css({ top: r.bottom + 4, left: r.left }).show();
	$(badge).addClass('active');
}

function dbxGpOpenCtxMenu(badge, tableName, x, y) {
	dbxGpCloseAllPopups();
	_dbxGpPopupTable = tableName;
	var total   = Object.keys(dbxGraphPickerOrder).length;
	var current = dbxGraphPickerOrder[tableName];

	function ctxBtn(label, fn) {
		return $('<button>').addClass('dbx-gp-ctx-item').text(label)
			.on('click', function(e) { e.stopPropagation(); fn(); dbxGpCloseAllPopups(); });
	}

	var $menu = $('#dbx-gp-ctx-popup').empty();
	if (current > 1)
		$menu.append(ctxBtn('⇑  Move to top',    function() { dbxGpMoveToPosition(tableName, 1);     dbxGpRefreshBadges(); dbxGpUpdateCount(); }));
	if (current < total)
		$menu.append(ctxBtn('⇓  Move to bottom', function() { dbxGpMoveToPosition(tableName, total); dbxGpRefreshBadges(); dbxGpUpdateCount(); }));
	if (current > 1 || current < total)
		$menu.append($('<div>').css({ borderTop: '1px solid #e9ecef', margin: '3px 0' }));
	$menu.append(ctxBtn('↺  Reset all to list order', function() { dbxGpResetToListOrder(); dbxGpRefreshBadges(); dbxGpUpdateCount(); }));

	$menu.css({ top: y, left: x }).show();
	$(badge).addClass('active');
}

// ── Render the graph list ────────────────────────────────────────
function dbxGpRender(searchTerm) {
	const term = searchTerm.toLowerCase().trim();

	// Save checkbox states BEFORE wiping the DOM
	dbxGpSyncFromDom();

	const filtered = dbxGraphPickerData.filter(function(g) {
		if (!term) return true;
		return g.cmName.toLowerCase().includes(term)
			|| g.graphLabel.toLowerCase().includes(term)
			|| g.graphCategory.toLowerCase().includes(term);
	});

	if (filtered.length === 0) {
		$('#dbxGraphPickerList').html('<div class="text-center text-muted p-3">No graphs match your search.</div>');
		dbxGpUpdateCount();
		return;
	}

	// Remember which groups are currently collapsed
	const collapsed = {};
	$('.dbx-gp-cm-body').each(function() {
		const cmName = $(this).closest('.dbx-gp-cm-group').find('.dbx-gp-cm-header').data('cm');
		if ($(this).is(':hidden')) collapsed[cmName] = true;
	});

	// Group by cmName
	const grouped = {};
	filtered.forEach(function(g) {
		if (!grouped[g.cmName]) grouped[g.cmName] = [];
		grouped[g.cmName].push(g);
	});

	let html = '';
	Object.entries(grouped).forEach(function([cmName, entries]) {
		// Expand all groups when searching, otherwise respect collapsed state
		const isHidden = !term && collapsed[cmName] ? 'style="display:none"' : '';

		html += '<div class="dbx-gp-cm-group">';
		html +=   '<div class="dbx-gp-cm-header" data-cm="' + cmName + '">'
			+       '<span>' + cmName + '</span>'
			+       '<button type="button" class="dbx-gp-cm-selectall btn btn-sm btn-outline-secondary" data-cm="' + cmName + '" title="Select all graphs in this group">Select All</button>'
			+       '<button type="button" class="dbx-gp-cm-deselectall btn btn-sm btn-outline-secondary" data-cm="' + cmName + '" title="Deselect all graphs in this group">None</button>'
			+       '<small class="text-muted ml-auto">'
			+         '<span class="dbx-gp-cm-selected-count" data-cm="' + cmName + '">0</span>'
			+         ' / ' + entries.length + ' selected'
			+       '</small>'
			+     '</div>';
		html +=   '<div class="dbx-gp-cm-body" ' + isHidden + '>';

		entries.forEach(function(g) {
			// Use the Set — NOT the DOM — to determine checked state
			const checked  = dbxGraphPickerSelected.has(g.tableName) ? 'checked' : '';
			const orderNum = dbxGraphPickerSelected.has(g.tableName) ? (dbxGraphPickerOrder[g.tableName] || '') : '';
			const badgeVis = checked ? '' : 'visibility:hidden';
			const startup  = g.visibleAtStartup ? 'dbx-gp-startup' : '';
			const badge    = '<span class="badge badge-secondary dbx-gp-category-badge">' + g.graphCategory + '</span>';
			html += '<div class="dbx-gp-item ' + startup + '" data-cm="' + cmName + '">'
				+    '<label>'
				+      '<input type="checkbox" id="dbxGpCk-' + g.graphName + '" value="' + g.tableName + '" data-cm="' + cmName + '" ' + checked + '> '
				+      g.graphLabel
				+      badge
				+    '</label>'
				+    '<div class="dbx-gp-order-wrap">'
				+      '<span class="dbx-gp-order-badge" data-table="' + g.tableName + '"'
				+        ' style="' + badgeVis + '" title="Click to change load order">' + orderNum + '</span>'
				+    '</div>'
				+  '</div>';
		});

		html += '</div></div>';
	});

	$('#dbxGraphPickerList').html(html);
	dbxGpUpdateCount();
	dbxGpSyncGroupButtons();
}

// ── Public entry point ────────────────────────────────────────────
// serverName  — server to load graphs for
// startTime   — time window (e.g. "2h", "4h", ISO timestamp)
// options     — see module header for supported keys
function dbxOpenGraphPickerModal(serverName, startTime, options) {
	_dbxGpOptions           = options || {};
	dbxGraphPickerServer    = serverName;
	dbxGraphPickerStartTime = startTime;
	dbxGraphPickerSelected  = new Set();
	dbxGraphPickerOrder     = {};

	// Pre-populate selection from caller (e.g. currently shown graphs)
	if (_dbxGpOptions.preSelected && Array.isArray(_dbxGpOptions.preSelected)) {
		_dbxGpOptions.preSelected.forEach(function(t, i) {
			if (t) {
				dbxGraphPickerSelected.add(t);
				dbxGraphPickerOrder[t] = i + 1;
			}
		});
	}

	// Fetch graph list from API
	$.ajax({
		url:      '/api/graphs?sessionName=' + serverName,
		async:    false,
		dataType: 'json',
		success:  function(data) {
			dbxGraphPickerData = Array.isArray(data)        ? data
			                   : Array.isArray(data.graphs) ? data.graphs
			                   : Array.isArray(data.data)   ? data.data
			                   : Object.values(data).find(function(v) { return Array.isArray(v); }) || [];
		},
		error: function() {
			dbxGraphPickerData = [];
			console.error('dbxGraphPicker: failed to load graphs for ' + serverName);
		}
	});

	$('#dbxGraphPickerServerName').text(serverName);
	$('#dbxGraphPickerSearch').val('');
	dbxGpRender('');

	// Update OK button tooltip to reflect target behaviour
	var isSelf = (_dbxGpOptions.target === '_self');
	$('#dbxGraphPickerOk')
		.attr('title', isSelf
			? 'Reloads this page with the selected graphs'
			: 'Opens graphs in a new tab — Tip: Press Ctrl+D in that tab to bookmark your selection')
		.text(isSelf ? 'Apply Selected Graphs' : 'Open Selected Graphs');

	$('#dbxGraphPickerModal').modal('show');
}

// ── Init — self-called at script load ────────────────────────────
function dbxInitGraphPickerModal() {

	// ── 1. Inject CSS link (once) ─────────────────────────────────
	if (!document.getElementById('dbx-gp-css')) {
		$('<link>', { id: 'dbx-gp-css', rel: 'stylesheet',
		              href: '/scripts/dbxtune/css/dbxGraphPicker.css' })
			.appendTo('head');
	}

	// ── 2. Inject modal HTML (once) ───────────────────────────────
	if (!document.getElementById('dbxGraphPickerModal')) {
		$('body').append([
			'<div class="modal fade" id="dbxGraphPickerModal" tabindex="-1" role="dialog">',
			'  <div class="modal-dialog modal-lg" role="document">',
			'    <div class="modal-content">',
			'      <div class="modal-header">',
			'        <h5 class="modal-title">Select Graphs &mdash; <span id="dbxGraphPickerServerName"></span></h5>',
			'        <button type="button" class="close" data-dismiss="modal">&times;</button>',
			'      </div>',
			'      <div class="modal-body">',
			'        <div class="input-group mb-3">',
			'          <input type="text" id="dbxGraphPickerSearch" class="form-control" placeholder="Search by collector, graph name or category...">',
			'          <div class="input-group-append">',
			'            <button class="btn btn-outline-secondary" type="button" id="dbxGraphPickerClearSearch">Clear</button>',
			'          </div>',
			'        </div>',
			'        <div class="mb-2">',
			'          <button class="btn btn-sm btn-outline-primary"   id="dbxGraphPickerSelectAll">Select All Visible</button>',
			'          <button class="btn btn-sm btn-outline-secondary" id="dbxGraphPickerClearAll">Clear All</button>',
			'          <button class="btn btn-sm btn-outline-secondary" id="dbxGraphPickerExpandAll">Expand all Groups</button>',
			'          <button class="btn btn-sm btn-outline-secondary" id="dbxGraphPickerCollapseAll">Close all Groups</button>',
			'          <span class="ml-3 small" id="dbxGraphPickerSelectedCountWrap">',
			'            <span id="dbxGraphPickerSelectedCount">0</span> selected',
			'          </span>',
			'          <span class="ml-3">',
			'            <label class="text-muted small mb-0" for="dbxGraphPickerGcol">Columns:</label>',
			'            <input type="number" id="dbxGraphPickerGcol" class="form-control form-control-sm d-inline-block ml-1"',
			'              min="1" max="10" placeholder="default" style="width:80px;">',
			'          </span>',
			'        </div>',
			'        <div id="dbxGraphPickerList">',
			'          <!-- populated dynamically by dbxGpRender() -->',
			'        </div>',
			'      </div>',
			'      <div class="modal-footer">',
			'        <small class="text-muted mr-auto">&#x1F7E1; highlighted = visible at startup by default</small>',
			'        <button type="button" class="btn btn-secondary" data-dismiss="modal">Cancel</button>',
			'        <button type="button" class="btn btn-primary" id="dbxGraphPickerOk"',
			'          data-toggle="tooltip" data-placement="top"',
			'          title="Opens graphs in a new tab — Tip: Press Ctrl+D in that tab to bookmark your selection">',
			'          Open Selected Graphs',
			'        </button>',
			'      </div>',
			'    </div>',
			'  </div>',
			'</div>'
		].join('\n'));
	}

	// ── 3. Inject position popup + context menu (once) ────────────
	if (!document.getElementById('dbx-gp-pos-popup')) {
		$('body').append(
			'<div id="dbx-gp-pos-popup" class="dbx-gp-popup">' +
			'  <div class="popup-title">Move to position</div>' +
			'  <div id="dbx-gp-pos-buttons"></div>' +
			'</div>' +
			'<div id="dbx-gp-ctx-popup" class="dbx-gp-popup"></div>'
		);
	}

	// ── 4. Draggable + resizable ──────────────────────────────────
	$(document).on('shown.bs.modal', '#dbxGraphPickerModal', function() {
		$('#dbxGraphPickerModal .modal-dialog').draggable({
			handle:      '.modal-header',
			containment: 'window'
		});
		$('#dbxGraphPickerModal .modal-content').resizable({
			minHeight: 400,
			minWidth:  500,
			handles:   'all',
			resize: function() {
				const listHeight = $('#dbxGraphPickerModal .modal-content').height()
					- $('#dbxGraphPickerModal .modal-header').outerHeight()
					- $('#dbxGraphPickerModal .modal-footer').outerHeight()
					- $('#dbxGraphPickerModal .input-group').outerHeight(true)
					- $('#dbxGraphPickerModal .mb-2').outerHeight(true)
					- 60;
				$('#dbxGraphPickerList').css('max-height', listHeight + 'px');
			}
		});

		$('#dbxGraphPickerOk').tooltip();

		$('.dbx-gp-cm-body').show();
		dbxGpSyncGroupButtons();
	});

	// ── 5. Event handlers ─────────────────────────────────────────

	// Close popups on click outside or Escape
	$(document).on('click.dbxgp', '#dbxGraphPickerModal', function() { dbxGpCloseAllPopups(); });
	$(document).on('keydown.dbxgp', function(e) {
		if (e.key === 'Escape' && ($('#dbx-gp-pos-popup').is(':visible') || $('#dbx-gp-ctx-popup').is(':visible'))) {
			e.stopImmediatePropagation();
			dbxGpCloseAllPopups();
		}
	});

	// Search input
	$(document).on('input', '#dbxGraphPickerSearch', function() {
		dbxGpRender($(this).val());
	});

	// Clear search button
	$(document).on('click', '#dbxGraphPickerClearSearch', function() {
		$('#dbxGraphPickerSearch').val('');
		dbxGpRender('');
	});

	// Select all visible — only checks boxes in expanded groups
	$(document).on('click', '#dbxGraphPickerSelectAll', function() {
		$('.dbx-gp-cm-body:visible input[type=checkbox]').each(function() {
			if (!$(this).is(':checked')) {
				$(this).prop('checked', true);
				var t = $(this).val();
				dbxGraphPickerSelected.add(t);
				var nextNum = Object.keys(dbxGraphPickerOrder).length + 1;
				dbxGraphPickerOrder[t] = nextNum;
				$(this).closest('.dbx-gp-item').find('.dbx-gp-order-badge')
					.text(nextNum).css('visibility', 'visible');
			}
		});
		dbxGpUpdateCount();
	});

	// Clear all — resets Set, order AND all visible checkboxes
	$(document).on('click', '#dbxGraphPickerClearAll', function() {
		dbxGraphPickerSelected.clear();
		dbxGraphPickerOrder = {};
		$('#dbxGraphPickerList input[type=checkbox]').prop('checked', false);
		$('.dbx-gp-order-badge').text('').css('visibility', 'hidden');
		dbxGpUpdateCount();
	});

	// Expand all groups
	$(document).on('click', '#dbxGraphPickerExpandAll', function() {
		$('.dbx-gp-cm-body').show();
		dbxGpSyncGroupButtons();
	});

	// Collapse all groups
	$(document).on('click', '#dbxGraphPickerCollapseAll', function() {
		$('.dbx-gp-cm-body').hide();
		dbxGpSyncGroupButtons();
	});

	// Checkbox change — maintain order alongside selection
	$(document).on('change', '#dbxGraphPickerList input[type=checkbox]', function() {
		var t     = $(this).val();
		var badge = $(this).closest('.dbx-gp-item').find('.dbx-gp-order-badge');
		if ($(this).is(':checked')) {
			dbxGraphPickerSelected.add(t);
			var nextNum = Object.keys(dbxGraphPickerOrder).length + 1;
			dbxGraphPickerOrder[t] = nextNum;
			badge.text(nextNum).css('visibility', 'visible');
		} else {
			dbxGraphPickerSelected.delete(t);
			delete dbxGraphPickerOrder[t];
			badge.text('').css('visibility', 'hidden');
			dbxGpRenormalize();
			dbxGpRefreshBadges();
		}
		dbxGpUpdateCount();
	});

	// Left-click order badge — open position picker
	$(document).on('click', '.dbx-gp-order-badge', function(e) {
		e.stopPropagation();
		var t = $(this).data('table');
		if (_dbxGpPopupTable === t && $('#dbx-gp-pos-popup').is(':visible')) {
			dbxGpCloseAllPopups();
			return;
		}
		dbxGpOpenPosPopup(this, t);
	});

	// Right-click order badge — open context menu
	$(document).on('contextmenu', '.dbx-gp-order-badge', function(e) {
		e.preventDefault();
		e.stopPropagation();
		dbxGpOpenCtxMenu(this, $(this).data('table'), e.clientX, e.clientY);
	});

	// Collapse / expand cm group on header click
	$(document).on('click', '.dbx-gp-cm-header', function(e) {
		// Ignore clicks that originated from the per-CM select/deselect buttons
		if ($(e.target).closest('.dbx-gp-cm-selectall, .dbx-gp-cm-deselectall').length) return;
		$(this).next('.dbx-gp-cm-body').toggle();
		dbxGpSyncGroupButtons();
	});

	// "Select All" button — check every graph in that CM group
	$(document).on('click', '.dbx-gp-cm-selectall', function(e) {
		e.stopPropagation();
		var cmName = $(this).data('cm');
		$('.dbx-gp-cm-body input[type=checkbox][data-cm="' + cmName + '"]').each(function() {
			if (!$(this).is(':checked')) {
				$(this).prop('checked', true);
				var t = $(this).val();
				dbxGraphPickerSelected.add(t);
				var nextNum = Object.keys(dbxGraphPickerOrder).length + 1;
				dbxGraphPickerOrder[t] = nextNum;
				$(this).closest('.dbx-gp-item').find('.dbx-gp-order-badge')
					.text(nextNum).css('visibility', 'visible');
			}
		});
		dbxGpUpdateCount();
	});

	// "None" button — uncheck every graph in that CM group
	$(document).on('click', '.dbx-gp-cm-deselectall', function(e) {
		e.stopPropagation();
		var cmName = $(this).data('cm');
		$('.dbx-gp-cm-body input[type=checkbox][data-cm="' + cmName + '"]').each(function() {
			if ($(this).is(':checked')) {
				$(this).prop('checked', false);
				var t = $(this).val();
				dbxGraphPickerSelected.delete(t);
				delete dbxGraphPickerOrder[t];
				$(this).closest('.dbx-gp-item').find('.dbx-gp-order-badge')
					.text('').css('visibility', 'hidden');
			}
		});
		// Re-number remaining selections so order badges stay consecutive
		var remaining = Object.keys(dbxGraphPickerOrder).sort(function(a, b) {
			return dbxGraphPickerOrder[a] - dbxGraphPickerOrder[b];
		});
		remaining.forEach(function(t, i) {
			dbxGraphPickerOrder[t] = i + 1;
			$('.dbx-gp-order-badge[data-table="' + t + '"]').text(i + 1);
		});
		dbxGpUpdateCount();
	});

	// OK — open / reload with selected graphs in chosen order
	$(document).on('click', '#dbxGraphPickerOk', function() {
		dbxGpSyncFromDom();

		if (dbxGraphPickerSelected.size === 0) {
			alert('Please select at least one graph.');
			return;
		}

		dbxGpCloseAllPopups();
		dbxGpRenormalize();

		const graphList = Object.keys(dbxGraphPickerOrder)
			.sort(function(a, b) { return dbxGraphPickerOrder[a] - dbxGraphPickerOrder[b]; })
			.join(',');
		const gcol = $('#dbxGraphPickerGcol').val().trim();

		let url = '/graph.html?subscribe=true'
			+ '&startTime='   + encodeURIComponent(dbxGraphPickerStartTime)
			+ '&sessionName=' + encodeURIComponent(dbxGraphPickerServer)
			+ '&graphList='   + graphList;

		if (gcol !== '') {
			url += '&gcol=' + gcol;
		}
		if (_dbxGpOptions.endTime) {
			url += '&endTime=' + encodeURIComponent(_dbxGpOptions.endTime);
		}

		$('#dbxGraphPickerModal').modal('hide');

		if (typeof _dbxGpOptions.onOpen === 'function') {
			_dbxGpOptions.onOpen(url);
		} else if (_dbxGpOptions.target === '_self') {
			window.location.href = url;
		} else {
			window.open(url, '_blank');
		}
	});

	// Clear search, gcol and popups when modal closes
	$(document).on('hidden.bs.modal', '#dbxGraphPickerModal', function() {
		$('#dbxGraphPickerSearch').val('');
		$('#dbxGraphPickerGcol').val('');
		dbxGpCloseAllPopups();
	});
}

// ── Auto-initialise on DOM ready ─────────────────────────────────
$(function() { dbxInitGraphPickerModal(); });

// ── Toast helper ─────────────────────────────────────────────────
function dbxGpShowToast(message) {
	const toast = $('<div>')
		.text(message)
		.css({
			position:     'fixed',
			bottom:       '20px',
			right:        '20px',
			background:   '#333',
			color:        '#fff',
			padding:      '10px 16px',
			borderRadius: '4px',
			zIndex:       9999,
			fontSize:     '0.9rem'
		})
		.appendTo('body');

	setTimeout(function() { toast.fadeOut(400, function() { $(this).remove(); }); }, 4000);
}
