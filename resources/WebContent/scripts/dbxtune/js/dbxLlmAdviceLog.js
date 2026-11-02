/**
 * dbxLlmAdviceLog.js - the list of saved "LLM Optimization Advice" requests (see LlmAdviceLog.java / LlmAdviceLogServlet.java).
 *
 * Used by two pages, with the same toolbar (period, refresh, filter, delete) and table:
 *   - admin/admin.html     ("LLM Optimization Advice - Saved Requests"): everyone's requests, from /admin/llm-log
 *   - llm-ai-advice.html   ("My Saved Advice"):                            the logged-in user's own, from /llm-log
 *
 * Usage:
 *   dbxLlmAdviceLog.init('container-id', { admin: true });   // or { admin: false }
 *
 * Needs jQuery and applyRowFilter() (dbxcentral.utils.js).
 */
var dbxLlmAdviceLog = (function()
{
	'use strict';

	// The filter's column names: rows are arrays in this order, so the shared applyRowFilter() can be used (regex or 'where ...')
	var COLS = ['time', 'user', 'server', 'origin', 'provider', 'model', 'status', 'durationMs', 'inputTokens', 'outputTokens', 'sql', 'label', 'effort'];

	function esc(v)
	{
		return $('<span>').text(v === null || v === undefined ? '' : String(v)).html();
	}

	/**
	 * Render the list into the element 'containerId' and load it.
	 * @param opts.admin  true: all users' requests (/admin/llm-log, with a User column);
	 *                    false: only the logged-in user's own (/llm-log)
	 */
	function init(containerId, opts)
	{
		var admin   = !!(opts && opts.admin);
		var url     = admin ? '/admin/llm-log' : '/llm-log';
		var $root   = $('#' + containerId);
		var entries = [];
		var shown   = []; // the file names of the rows the list shows now (after the period and the filter): "Delete shown"

		$root.html(
			  '<div class="d-flex flex-wrap align-items-center gap-2 mb-2">'
			+   '<select class="form-select form-select-sm dbx-llmlog-days" style="width:auto;">'
			+     '<option value="1">Last 24 hours</option>'
			+     '<option value="7" selected>Last 7 days</option>'
			+     '<option value="30">Last 30 days</option>'
			+     '<option value="0">All saved</option>'
			+   '</select>'
			+   '<button type="button" class="btn btn-sm btn-primary dbx-llmlog-refresh"><i class="fa fa-refresh"></i> Refresh</button>'
			+   '<input type="text" class="form-control form-control-sm dbx-llmlog-filter" style="width:320px;"'
			+     ' placeholder="' + (admin ? "Filter: regex, or 'where user = x and status = FAILED'" : "Filter: regex, or 'where server = x and status = FAILED'") + '">'
			+   '<span class="text-danger small dbx-llmlog-filter-err"></span>'
			+   '<button type="button" class="btn btn-sm btn-outline-danger ms-auto dbx-llmlog-delete-shown" style="display:none;"'
			+     ' title="Delete all saved requests shown in the list below (narrow it with the period and the filter first)"></button>'
			+ '</div>'
			+ '<div class="small text-muted mb-1 dbx-llmlog-info"></div>'
			+ '<div class="mb-2 dbx-llmlog-summary"></div>'
			+ '<div style="width:100%; overflow-x:auto;">'
			+   '<table class="table table-sm table-bordered table-hover dbx-llmlog-table" style="display:none; width:100%; white-space:nowrap;">'
			+     '<thead class="table-light"><tr>'
			+       '<th>Time</th>' + (admin ? '<th>User</th>' : '') + '<th>Server</th><th>Origin</th><th>Provider / Model</th>'
			+       '<th>Status</th><th>Duration</th><th>Tokens in / out</th><th>SQL</th><th></th>'
			+     '</tr></thead>'
			+     '<tbody></tbody>'
			+   '</table>'
			+ '</div>');

		function $f(cls) { return $root.find('.' + cls); }

		function showError(msg)
		{
			$f('dbx-llmlog-summary').html('<span class="text-danger">' + esc(msg) + '</span>');
		}

		function load()
		{
			$.ajax({
				url:      url,
				data:     { op: 'list', days: $f('dbx-llmlog-days').val() },
				dataType: 'json',
				cache:    false,
				success: function(data)
				{
					entries = data.entries || [];
					var keep = (data.keepDays > 0 ? data.keepDays + ' days' : 'forever');
					$f('dbx-llmlog-info').text(admin
						? (data.saveEnabled ? 'Saving is ON' : 'Saving is OFF (DbxCentral.llm.log.save=false)')
							+ ', kept ' + keep + ' (DbxCentral.llm.log.keep.days)'
							+ ', each answer shows a link to its saved request (any user can open it: /llm-log)'
							+ '. Folder: ' + data.dir
						: (data.saveEnabled ? 'Your requests are kept ' + keep + '.' : 'Requests are not saved on this DbxCentral (DbxCentral.llm.log.save=false).')
							+ ' Click a row to open it: the answer, what was sent and the model\'s thinking.');
					render();
				},
				error: function(xhr)
				{
					entries = [];
					render();
					showError('Could not load the saved LLM requests: ' + ((xhr.responseJSON && xhr.responseJSON.message) || ('HTTP ' + xhr.status)));
				}
			});
		}

		/** Delete saved requests (after a confirmation), then reload the list */
		function del(names, what)
		{
			if (!names || names.length === 0)
				return;
			if (!confirm('Delete ' + what + '?\n\nThe saved file(s) are removed from DbxCentral - this can not be undone.'))
				return;
			$.ajax({
				url:         url,
				method:      'POST',
				data:        { op: 'delete', name: names },
				traditional: true, // name=a&name=b (not name[]=a)
				dataType:    'json',
				success: function() { load(); }, // the list (and its summary) show the result
				error: function(xhr)
				{
					showError('Could not delete: ' + ((xhr.responseJSON && xhr.responseJSON.message) || ('HTTP ' + xhr.status)));
				}
			});
		}

		function render()
		{
			var rows = entries.map(function(e) {
				var row = [e.time, e.user, e.srvName, e.origin, e.providerId, e.model, e.status, e.durationMs, e.inputTokens, e.outputTokens, e.sqlPreview, e.modelLabel || e.modelEntry, e.effort];
				row.entry = e; // not an array element, so the filter does not search it
				return row;
			});
			var res = applyRowFilter(rows, COLS, $f('dbx-llmlog-filter').val());
			$f('dbx-llmlog-filter-err').text(res.filterError);

			var users = {}, failed = 0, stopped = 0, tokIn = 0, tokOut = 0;
			var html = '';
			res.filteredRows.forEach(function(r)
			{
				var e    = r.entry;
				var view = url + '?op=view&name=' + encodeURIComponent(e.name);
				users[e.user] = true;
				if (e.status === 'FAILED')  failed++;
				if (e.status === 'STOPPED') stopped++;
				tokIn  += e.inputTokens  || 0;
				tokOut += e.outputTokens || 0;

				// Stopped by the user (the Stop button): not an error, but not an answer either
				var rowClass = e.status === 'FAILED' ? ' table-danger' : e.status === 'STOPPED' ? ' table-warning' : '';
				// Under the model: the dropdown label (or our own entry name) and the effort sent, when there are
				var modelNote = [e.modelLabel || e.modelEntry, e.effort ? 'effort=' + e.effort : null].filter(Boolean).join(' · ');
				html += '<tr class="dbx-llmlog-row' + rowClass + '" style="cursor:pointer;" data-view="' + esc(view) + '"'
					+ ' title="' + esc(e.status !== 'OK' && e.errorMessage ? e.errorMessage : 'Click to open') + '">'
					+ '<td>' + esc(e.time) + '</td>'
					+ (admin ? '<td>' + esc(e.user) + '</td>' : '')
					+ '<td>' + esc(e.srvName) + '</td>'
					+ '<td>' + esc(e.origin) + '</td>'
					+ '<td>' + esc(e.providerId) + ' / ' + esc(e.model)
					+ (modelNote ? '<br><small class="text-muted">' + esc(modelNote) + '</small>' : '') + '</td>'
					+ '<td>' + esc(e.status) + '</td>'
					+ '<td class="text-end">' + (e.durationMs != null ? (e.durationMs / 1000).toFixed(1) + 's' : '') + '</td>'
					+ '<td class="text-end">' + esc(e.inputTokens != null ? e.inputTokens.toLocaleString() : '') + ' / ' + esc(e.outputTokens != null ? e.outputTokens.toLocaleString() : '') + '</td>'
					+ '<td style="max-width:420px; overflow:hidden; text-overflow:ellipsis;">' + esc(e.sqlPreview ? e.sqlPreview.substring(0, 100) : '') + '</td>'
					+ '<td><a href="' + esc(view) + '&raw=true" class="dbx-llmlog-nobubble" title="Download the saved file (plain, self-contained)"><i class="fa fa-download"></i></a>'
					+ '&nbsp;&nbsp;<a href="javascript:void(0)" class="text-danger dbx-llmlog-nobubble dbx-llmlog-delete" data-name="' + esc(e.name) + '" data-time="' + esc(e.time) + '" title="Delete this saved request"><i class="fa fa-trash"></i></a></td>'
					+ '</tr>';
			});
			$f('dbx-llmlog-table').find('tbody').html(html);
			shown = res.filteredRows.map(function(r) { return r.entry.name; });
			$f('dbx-llmlog-delete-shown').toggle(shown.length > 0).html('<i class="fa fa-trash"></i> Delete shown (' + shown.length + ')');
			$f('dbx-llmlog-table').toggle(shown.length > 0);
			$f('dbx-llmlog-summary').html(
				'<b>' + shown.length + '</b> requests'
				+ (shown.length !== entries.length ? ' (of ' + entries.length + ')' : '')
				+ ', <b>' + failed + '</b> failed' + (stopped ? ', <b>' + stopped + '</b> stopped' : '')
				+ (admin ? ', by <b>' + Object.keys(users).length + '</b> users' : '')
				+ ', tokens in / out: <b>' + tokIn.toLocaleString() + '</b> / <b>' + tokOut.toLocaleString() + '</b>');
		}

		$f('dbx-llmlog-days')   .on('change', load);
		$f('dbx-llmlog-refresh').on('click',  load);
		$f('dbx-llmlog-filter') .on('input',  render);
		$f('dbx-llmlog-delete-shown').on('click', function() { del(shown, 'the ' + shown.length + ' saved requests shown in the list'); });
		$root.on('click', '.dbx-llmlog-row',      function() { window.open(this.dataset.view, '_blank'); });
		$root.on('click', '.dbx-llmlog-nobubble', function(ev) { ev.stopPropagation(); });
		$root.on('click', '.dbx-llmlog-delete',   function() { del([this.dataset.name], 'this saved request (' + this.dataset.time + ')'); });

		load();
	}

	return { init: init };
})();
