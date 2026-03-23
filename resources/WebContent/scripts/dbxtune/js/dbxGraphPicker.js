/* ── dbxGraphPicker.js ──────────────────────────────────────────────
   Graph Picker Modal - lets user search, select and open graphs
   Requires: jQuery, jQuery UI (draggable+resizable), Bootstrap 4.6

   Usage:
     1. Include dbxGraphPicker.css in your <head>
     2. Include dbxGraphPicker.js before </body>
     3. Paste the modal HTML once anywhere in your <body>
     4. Call dbxInitGraphPickerModal() once in your $(document).ready()
     5. Open the modal via: dbxOpenGraphPickerModal(serverName, startTime)

   Example menu item in your server dropdown:
     + '<a class="dropdown-item" href="#"'
     + '   onclick=\'dbxOpenGraphPickerModal("' + serverName + '", "' + startTime + '"); return false;\'>'
     + '   Browse &amp; Select <b>Graphs</b>...</a>'
──────────────────────────────────────────────────────────────────── */

// ── State variables ──────────────────────────────────────────────
var dbxGraphPickerData      = [];
var dbxGraphPickerServer    = '';
var dbxGraphPickerStartTime = '';
var dbxGraphPickerSelected  = new Set();

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
//	const total    = $('.dbx-gp-cm-body').length;
	const visible  = $('.dbx-gp-cm-body:visible').length;
//	const hidden   = $('.dbx-gp-cm-body:hidden').length;
	
	const hasOpenGroups = visible > 0;

	$('#dbxGraphPickerExpandAll')  .toggle(!hasOpenGroups);
	$('#dbxGraphPickerCollapseAll').toggle( hasOpenGroups);
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
			+       '<small class="text-muted">'
			+         '<span class="dbx-gp-cm-selected-count" data-cm="' + cmName + '">0</span>'
			+         ' / ' + entries.length + ' selected'
			+       '</small>'
			+     '</div>';
		html +=   '<div class="dbx-gp-cm-body" ' + isHidden + '>';

		entries.forEach(function(g) {
			// Use the Set — NOT the DOM — to determine checked state
			const checked = dbxGraphPickerSelected.has(g.tableName) ? 'checked' : '';
			const startup = g.visibleAtStartup ? 'dbx-gp-startup' : '';
			const badge   = '<span class="badge badge-secondary dbx-gp-category-badge">' + g.graphCategory + '</span>';
			html += '<div class="dbx-gp-item ' + startup + '" data-cm="' + cmName + '">'
				+    '<label>'
				+      '<input type="checkbox" id="dbxGpCk-' + g.graphName + '" value="' + g.tableName  + '" data-cm="' + cmName + '" ' + checked + '> '
				+      g.graphLabel
				+      badge
				+    '</label>'
				+  '</div>';
		});

		html += '</div></div>';
	});

	$('#dbxGraphPickerList').html(html);
	dbxGpUpdateCount();
	dbxGpSyncGroupButtons();
}

// ── Public entry point — call this to open the modal ─────────────
function dbxOpenGraphPickerModal(serverName, startTime) {
	dbxGraphPickerServer    = serverName;
	dbxGraphPickerStartTime = startTime;
	dbxGraphPickerSelected  = new Set();   // reset selections for new server

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
	$('#dbxGraphPickerModal').modal('show');
}

// ── Init — call once in your $(document).ready() ─────────────────
function dbxInitGraphPickerModal() {

	// DEBUG: if/when the events are fired
	//$(document).on('show.bs.modal shown.bs.modal hide.bs.modal hidden.bs.modal', '#dbxGraphPickerModal', function(e) {
	//	console.log('Modal event fired:', e.type + '.' + e.namespace);
	//	alert('Modal event fired:' + e.type + '.' + e.namespace);
	//});

	// Draggable by header + resizable from any edge via jQuery UI
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
		//alert("MODAL: shown"); // If we want to debug that it has been running this event

		$('#dbxGraphPickerOk').tooltip();

		$('.dbx-gp-cm-body').show();
		dbxGpSyncGroupButtons(); 
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
		$('.dbx-gp-cm-body:visible input[type=checkbox]').prop('checked', true);
		dbxGpUpdateCount();
	});

	// Clear all — resets Set AND all visible checkboxes
	$(document).on('click', '#dbxGraphPickerClearAll', function() {
		dbxGraphPickerSelected.clear();
		$('#dbxGraphPickerList input[type=checkbox]').prop('checked', false);
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

	// Any checkbox change
	$(document).on('change', '#dbxGraphPickerList input[type=checkbox]', function() {
		dbxGpUpdateCount();
	});

	// Collapse / expand cm group on header click
	$(document).on('click', '.dbx-gp-cm-header', function() {
		$(this).next('.dbx-gp-cm-body').toggle();
		dbxGpSyncGroupButtons();
	});

	$('#dbxGraphPickerOk').tooltip();

	// OK — open all selected graphs (incl. filtered-out ones) in new tab
	$(document).on('click', '#dbxGraphPickerOk', function() {
		dbxGpSyncFromDom();

		if (dbxGraphPickerSelected.size === 0) {
			alert('Please select at least one graph.');
			return;
		}

		const graphList = [...dbxGraphPickerSelected].join(',');
		const gcol      = $('#dbxGraphPickerGcol').val().trim();

		let url = '/graph.html?subscribe=true'
			+ '&startTime='   + dbxGraphPickerStartTime
			+ '&sessionName=' + dbxGraphPickerServer
			+ '&graphList='   + graphList;

		if (gcol !== '') {
			url += '&gcol=' + gcol;
		}

		window.open(url, '_blank');
		$('#dbxGraphPickerModal').modal('hide');

		// Hint user to bookmark the newly opened tab
		// (small non-blocking toast instead of alert)
		// NOTE: This wont work since we opens the graphs in a new window...
		// Workaround: A tooltip on the "ok" button
		//dbxGpShowToast('Tip: Press Ctrl+D in the new tab to bookmark your graph selection!');
	});

	// Clear search and gcol when modal closes
	$(document).on('hidden.bs.modal', '#dbxGraphPickerModal', function() {
		$('#dbxGraphPickerSearch').val('');
		$('#dbxGraphPickerGcol').val('');
	});
}

// ── ShowInfo — called on OK to show a "tip" ─────────────────
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

/*
** Here are the HTML, which needs to go into the HTML page
** ---- BEGIN ----
  <!-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
   - - Modal Dialog: dbxGraphPickerModal
   - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -->
	<div class="modal fade" id="dbxGraphPickerModal" tabindex="-1" role="dialog">
		<div class="modal-dialog modal-lg" role="document">
			<div class="modal-content">
				<div class="modal-header">
					<h5 class="modal-title">Select Graphs — <span id="dbxGraphPickerServerName"></span></h5>
					<button type="button" class="close" data-dismiss="modal">&times;</button>
				</div>
				<div class="modal-body">
					<div class="input-group mb-3">
						<input type="text" id="dbxGraphPickerSearch" class="form-control" placeholder="Search by collector, graph name or category...">
						<div class="input-group-append">
							<button class="btn btn-outline-secondary" type="button" id="dbxGraphPickerClearSearch">Clear</button>
						</div>
					</div>
					<div class="mb-2">
						<button class="btn btn-sm btn-outline-primary"   id="dbxGraphPickerSelectAll">Select All Visible</button>
						<button class="btn btn-sm btn-outline-secondary" id="dbxGraphPickerClearAll">Clear All</button>
						<button class="btn btn-sm btn-outline-secondary" id="dbxGraphPickerExpandAll">Expand all Groups</button>
						<button class="btn btn-sm btn-outline-secondary" id="dbxGraphPickerCollapseAll">Close all Groups</button>
						<span class="ml-3 small" id="dbxGraphPickerSelectedCountWrap"><span id="dbxGraphPickerSelectedCount">0</span> selected</span>
						<span class="ml-3">
							<label class="text-muted small mb-0" for="dbxGraphPickerGcol">Columns:</label>
							<input type="number" id="dbxGraphPickerGcol" class="form-control form-control-sm d-inline-block ml-1"
								min="1" max="10" placeholder="default" style="width: 80px;">
						</span>
					</div>
					<div id="dbxGraphPickerList">
						<!-- populated dynamically by dbxGpRender() -->
					</div>
				</div>
				<div class="modal-footer">
					<small class="text-muted mr-auto">🟡 highlighted = visible at startup by default</small>
					<button type="button" class="btn btn-secondary" data-dismiss="modal">Cancel</button>
					<button type="button" class="btn btn-primary"   id="dbxGraphPickerOk" data-toggle="tooltip" data-placement="top" title="Opens graphs in a new tab — Tip: Press Ctrl+D in that tab to bookmark your selection">Open Selected Graphs</button>
				</div>
			</div>
		</div>
	</div>
** ---- END ----
*/