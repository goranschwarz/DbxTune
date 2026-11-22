/**
 * "AI Analysis" of a whole Daily Summary Report (page /llm-dsr-analysis, see LlmDsrAnalysisServlet.java).
 *
 * The page names the report ('name' = the report file, or 'srv' + 'period' = yyyy-MM-dd, from the DSR's "AI Analysis"
 * button); DbxCentral reads the report's digest from the file and builds the prompt (POST /api/llm/dsr-analysis).
 *
 * The page shows the report's last analysis (cached by DbxCentral) and the Analyze button (dbxLlmAdvice.js with task 'dsr-analysis':
 * the same model picker, progress and Stop as the statement advice). What is sent (the prompt) can also be copied into any LLM chat.
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

	/**
	 * The page: the report, its last analysis (if any), the Analyze button (dbxLlmAdvice: the model picker, progress, Stop),
	 * and - folded - what is sent: the sections of the digest and the prompt.
	 */
	function render(container, data, opts)
	{
		var r = data.report;
		var html = '';

		// The report
		html += '<div class="card mb-3"><div class="card-body">'
			+ '<h5 class="card-title"><b>' + esc(r.srvName) + '</b> ' + levelBadge(r.reportLevel)
			+ ' <span class="text-muted" style="font-size:0.9em;">' + esc(r.dbxApp) + ' &middot; ' + esc(r.periodBegin) + ' &ndash; ' + esc(r.periodEnd) + '</span></h5>'
			+ '<div>Report: <a href="' + esc(r.url) + '" target="_blank">' + esc(r.name) + '</a>'
			+ ' <span class="text-muted" style="font-size:0.85em;">(created ' + esc(r.createdAt) + ')</span></div>'
			+ '</div></div>';

		// The last analysis of this report, and the button for a new one
		html += '<div id="dsr-analysis-cached"></div>';
		if (opts.llmEnabled)
			html += '<div id="dsr-analysis-run" class="mb-3"></div>';
		else
			html += '<div class="alert alert-secondary" style="font-size:0.9em;">The LLM feature is not enabled on this DbxCentral: '
				+ 'copy the prompt below into any LLM chat (claude.ai, chatgpt.com, ...) to get the analysis.</div>';

		// What is sent (folded when it can be sent from here)
		html += '<details class="mt-3"' + (opts.llmEnabled ? '' : ' open') + '><summary style="cursor:pointer;font-weight:600;">What is sent to the AI: '
			+ data.sections.length + ' report sections, about ' + Math.round(data.promptSent.length / 4).toLocaleString() + ' tokens</summary>'
			+ '<div id="dsr-analysis-preview" class="mt-2"></div></details>';

		container.innerHTML = html;
		renderPreview(container.querySelector('#dsr-analysis-preview'), data);

		var cached = data.cached;
		if (cached && typeof dbxLlmAdvice !== 'undefined')
		{
			var cachedDiv = container.querySelector('#dsr-analysis-cached');
			var tokens = (cached.inputTokens != null && cached.outputTokens != null) ? ' - ' + cached.inputTokens.toLocaleString() + ' in / ' + cached.outputTokens.toLocaleString() + ' out tokens' : '';
			cachedDiv.innerHTML = '<div class="card mb-3"><div class="card-header"><b>Last analysis</b> <span class="text-muted" style="font-size:0.9em;">'
				+ esc(cached.time) + ' by ' + esc(cached.user) + ' with ' + esc((cached.providerName || cached.providerId) + ' / ' + (cached.modelLabel || cached.model)) + esc(tokens)
				+ '</span></div><div class="card-body dsr-analysis-cached-body"></div></div>';
			dbxLlmAdvice.renderDsrAnalysis(cachedDiv.querySelector('.dsr-analysis-cached-body'), cached, { reportUrl: r.url });
		}

		// The model picker and Analyze: the same as "Get LLM Optimization Advice". 'llmOpts' is kept: the server context is filled in later.
		if (opts.llmEnabled && typeof dbxLlmAdvice !== 'undefined')
		{
			opts.llmOpts = {
				task:       'dsr-analysis',
				reportName: r.name,
				reportUrl:  r.url,
				askLabel:   cached ? 'Re-analyze' : 'Analyze',
				target:     container.querySelector('#dsr-analysis-run')
			};
			dbxLlmAdvice.open(opts.llmOpts);
		}
	}

	/** What is sent: the sections of the digest, the server configuration (or not), and the prompt with a Copy button */
	function renderPreview(target, data)
	{
		var reportUrl = data.report.url;
		var html = '<div class="text-muted mb-2" style="font-size:0.9em;">Only numbers and names from the report are sent: <b>no SQL text</b>, no error message texts.</div>';

		// The sections in the digest
		html += '<table class="table table-sm table-hover" style="font-size:0.9em;"><thead><tr><th>Level</th><th>Section</th><th class="text-end">Characters</th><th></th></tr></thead><tbody>';
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

		// The server configuration
		html += '<div style="font-size:0.85em;" class="my-2 text-muted">Server configuration and hardware: '
			+ (data.hasServerContext ? 'included (as stored by the Collector at the end of the report period).'
			                         : 'not included (only when logged in, the LLM feature is enabled, and the Collector can be reached).')
			+ '</div>';

		// The prompt
		var approxTokens = Math.round(data.promptSent.length / 4);
		html += '<div class="mb-1"><b>Prompt</b>'
			+ ' <span class="text-muted" style="font-size:0.85em;">(' + data.promptSent.length.toLocaleString() + ' characters, about ' + approxTokens.toLocaleString() + ' tokens)</span>'
			+ ' <button type="button" class="btn btn-sm btn-outline-secondary dsr-analysis-copy">Copy</button></div>'
			+ '<pre style="white-space:pre-wrap; max-height:70vh; overflow:auto; border:1px solid #dee2e6; border-radius:4px; padding:8px; font-size:0.8em;">' + esc(data.promptSent) + '</pre>';

		target.innerHTML = html;
		target.querySelector('.dsr-analysis-copy').addEventListener('click', function (e) { copyText(data.promptSent, e.target); });
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

		// First without the server configuration (it needs to know the vendor and the period), then the prompt with it.
		// Only the "what is sent" part is drawn again: a running analysis must not be thrown away.
		post(req)
			.then(function (data)
			{
				render(container, data, opts);
				return fetchServerContext(data.report).then(function (serverContext)
				{
					if (!serverContext) return;
					if (opts.llmOpts) opts.llmOpts.serverContext = serverContext; // read when Analyze is pressed
					req.name = data.report.name; // the exact file now
					req.serverContext = serverContext;
					return post(req).then(function (data2) { renderPreview(container.querySelector('#dsr-analysis-preview'), data2); });
				});
			})
			.catch(function (err) { renderError(container, err.message); });
	}

	return { open: open };
})();
