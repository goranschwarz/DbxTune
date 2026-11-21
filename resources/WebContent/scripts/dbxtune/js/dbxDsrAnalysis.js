/**
 * "AI Analysis" of a whole Daily Summary Report (page /llm-dsr-analysis, see LlmDsrAnalysisServlet.java).
 *
 * The page names the report ('name' = the report file, or 'srv' + 'period' = yyyy-MM-dd, from the DSR's "AI Analysis"
 * button); DbxCentral reads the report's digest from the file and builds the prompt (POST /api/llm/dsr-analysis).
 *
 * Phase 1: the prompt preview only - nothing is sent to any LLM. The prompt can be copied into any LLM chat.
 */
var dbxDsrAnalysis = (function ()
{
	'use strict';

	var API_PATH = '/api/llm/dsr-analysis';

	function esc(s)
	{
		if (s == null) return '';
		return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#39;');
	}

	function levelBadge(level)
	{
		if (level === 'ISSUE') return '<span class="badge bg-danger">ISSUE</span>';
		if (level === 'FYI')   return '<span class="badge bg-warning text-dark">FYI</span>';
		return '<span class="badge bg-secondary">NONE</span>';
	}

	function post(body)
	{
		return fetch(API_PATH, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) })
			.then(function (resp)
			{
				return resp.json().catch(function () { return { error: 'bad-response', message: 'HTTP ' + resp.status }; })
					.then(function (data) { if (!resp.ok || data.error) throw new Error(data.message || ('HTTP ' + resp.status)); return data; });
			});
	}

	/** The server configuration and hardware, as the Collector stored it at the end of the report period ('' when none: not logged in, feature off, Collector down) */
	function fetchServerContext(report)
	{
		if (typeof dbxLlmAdvice === 'undefined' || !dbxLlmAdvice.fetchServerContext || !report.srvName || !report.dbVendor)
			return Promise.resolve('');
		var ts = report.periodEnd ? report.periodEnd + ':00' : '';
		return dbxLlmAdvice.fetchServerContext({ srv: report.srvName, dbVendor: report.dbVendor, ts: ts });
	}

	function copyText(text, btn)
	{
		function done() { var old = btn.textContent; btn.textContent = 'Copied'; setTimeout(function () { btn.textContent = old; }, 1500); }
		if (navigator.clipboard && window.isSecureContext)
			return navigator.clipboard.writeText(text).then(done);
		var ta = document.createElement('textarea'); // http: no clipboard API
		ta.value = text; ta.style.position = 'fixed'; ta.style.opacity = '0';
		document.body.appendChild(ta); ta.select();
		try { document.execCommand('copy'); done(); } finally { document.body.removeChild(ta); }
	}

	function render(container, data, opts)
	{
		var r = data.report;
		var reportUrl = r.url;
		var html = '';

		// The report
		html += '<div class="card mb-3"><div class="card-body">'
			+ '<h5 class="card-title"><b>' + esc(r.srvName) + '</b> ' + levelBadge(r.reportLevel)
			+ ' <span class="text-muted" style="font-size:0.9em;">' + esc(r.dbxApp) + ' &middot; ' + esc(r.periodBegin) + ' &ndash; ' + esc(r.periodEnd) + '</span></h5>'
			+ '<div>Report: <a href="' + esc(reportUrl) + '" target="_blank">' + esc(r.name) + '</a>'
			+ ' <span class="text-muted" style="font-size:0.85em;">(created ' + esc(r.createdAt) + ')</span></div>'
			+ '</div></div>';

		// What will be sent
		html += '<div class="alert alert-info" style="font-size:0.9em;">'
			+ '<b>Nothing is sent anywhere yet.</b> Below is the prompt DbxCentral will send to the AI (LLM) - copy it into any LLM chat (claude.ai, chatgpt.com, ...) to try it. '
			+ 'Only numbers and names from the report are in it: <b>no SQL text</b>, no error message texts.'
			+ (opts.llmEnabled ? '' : ' <i>(The LLM feature is not enabled on this DbxCentral.)</i>')
			+ '</div>';

		// The sections in the digest
		html += '<details class="mb-3" open><summary style="cursor:pointer;font-weight:600;">Report sections in the digest ('
			+ data.sections.length + ', ' + data.digestChars.toLocaleString() + ' characters)</summary>'
			+ '<table class="table table-sm table-hover mt-2" style="font-size:0.9em;"><thead><tr><th>Level</th><th>Section</th><th class="text-end">Characters</th><th></th></tr></thead><tbody>';
		data.sections.forEach(function (s)
		{
			html += '<tr><td>' + levelBadge(s.level) + '</td>'
				+ '<td><a href="' + esc(reportUrl) + '#' + encodeURIComponent(s.id) + '" target="_blank">' + esc(s.subject) + '</a></td>'
				+ '<td class="text-end">' + s.chars.toLocaleString() + '</td>'
				+ '<td>' + (s.cut ? '<span class="badge bg-light text-dark" title="The section had more data than fits: it was shortened">cut</span>' : '') + '</td></tr>';
		});
		html += '</tbody></table>';
		function list(title, arr, note)
		{
			if (!arr || !arr.length) return '';
			return '<div style="font-size:0.85em;" class="mb-1"><b>' + esc(title) + '</b> <span class="text-muted">(' + esc(note) + ')</span>: '
				+ arr.map(esc).join(', ') + '</div>';
		}
		html += list('Names only', data.namesOnly, 'level NONE and no numbers in the digest: only the name is sent');
		html += list('Disabled', data.disabled, 'not in the report');
		html += list('Dropped',  data.dropped,  'did not fit in the size limit, only the name is sent');
		html += '</details>';

		// The server configuration
		html += '<div style="font-size:0.85em;" class="mb-2 text-muted">Server configuration and hardware: '
			+ (data.hasServerContext ? 'included (as stored by the Collector at the end of the report period).'
			                         : 'not included (only when logged in, the LLM feature is enabled, and the Collector can be reached).')
			+ '</div>';

		// The prompt
		var approxTokens = Math.round(data.promptSent.length / 4);
		html += '<div class="mb-1"><b>Prompt that would be sent</b>'
			+ ' <span class="text-muted" style="font-size:0.85em;">(' + data.promptSent.length.toLocaleString() + ' characters, about ' + approxTokens.toLocaleString() + ' tokens)</span>'
			+ ' <button type="button" class="btn btn-sm btn-outline-secondary dsr-analysis-copy">Copy</button></div>'
			+ '<pre style="white-space:pre-wrap; max-height:70vh; overflow:auto; border:1px solid #dee2e6; border-radius:4px; padding:8px; font-size:0.8em;">' + esc(data.promptSent) + '</pre>';

		container.innerHTML = html;
		container.querySelector('.dsr-analysis-copy').addEventListener('click', function (e) { copyText(data.promptSent, e.target); });
	}

	function renderError(container, msg)
	{
		container.innerHTML = '<div class="alert alert-danger">' + esc(msg) + '</div>';
	}

	/**
	 * @param container  where to render
	 * @param opts       {llmEnabled: bool}
	 */
	function open(container, opts)
	{
		opts = opts || {};
		var qs = new URLSearchParams(window.location.search);
		var req = { name: qs.get('name') || '', srv: qs.get('srv') || '', period: qs.get('period') || '', preview: true };
		if (!req.name && !(req.srv && req.period))
		{
			renderError(container, "Which report? Open this page from a Daily Summary Report's 'AI Analysis' button (it adds ?name=<report file>, or ?srv=<server>&period=<yyyy-MM-dd>).");
			return;
		}

		// First without the server configuration (it needs to know the vendor and the period), then with it
		post(req)
			.then(function (data)
			{
				render(container, data, opts);
				return fetchServerContext(data.report).then(function (serverContext)
				{
					if (!serverContext) return;
					req.name = data.report.name; // the exact file now
					req.serverContext = serverContext;
					return post(req).then(function (data2) { render(container, data2, opts); });
				});
			})
			.catch(function (err) { renderError(container, err.message); });
	}

	return { open: open };
})();
