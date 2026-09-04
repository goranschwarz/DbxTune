/*******************************************************************************
 * dbxLlmAdvice.js
 *
 * Shared "Get LLM Optimization Advice" UI for DbxCentral.
 * Include this script on any page that wants to offer LLM-based SQL
 * optimization advice (Daily Summary Report, the Active Statements web
 * dialog on graph.html, the Showplan viewer), and call:
 *
 *   dbxLlmAdvice.open({
 *       sql:        '<the SQL statement>',        // required
 *       plan:       '<execution plan text/xml>',  // optional
 *       ddlContext: '<DDL/index/stats text>',      // optional - if already known, skips both lookups below
 *
 *       // Execution statistics / usage-over-time for the statement. Supply EITHER of these:
 *       workloadData:    '<json>',                 // optional - raw, as harvested from the Daily Summary
 *                                                   // Report's sparkline sub-table by dsrHarvestWorkload()
 *                                                   // (see SparklineHelper.getWorkloadHarvesterJs()).
 *                                                   // Turned into 'workloadProfile' by open().
 *       workloadProfile: '<plain text>',           // optional - the finished text, if the caller has it
 *       dbVendor:   'Adaptive Server Enterprise',  // required if ddlContext isn't supplied and a lookup is wanted
 *
 *       // EITHER of these two (if neither is supplied, no DDL/index/stats context is fetched):
 *       jdbcUrl:    'jdbc:h2:...',                 // resolves ddlContext via /api/llm/context (an H2 recording file)
 *       jdbcUser:   'sa',                          // optional, default 'sa'
 *       jdbcPass:   '',                            // optional
 *       //   -- or --
 *       srv:        'PROD_ASE_01',                 // resolves ddlContext LIVE via /api/cc/mgt/table-info (ASE) or
 *                                                   // /api/cc/mgt/query-store (SQL Server), by parsing 'sql' for
 *                                                   // table names client-side (needs dbxSqlTableNames.js loaded on
 *                                                   // the page) and asking that server's currently-running
 *                                                   // Collector - so it needs 'srv' to be reachable right now, not
 *                                                   // just "was monitored when this link was generated". Preferred
 *                                                   // over jdbcUrl when both would apply (always-fresh schema info,
 *                                                   // and the caller doesn't need to know/carry an H2 file path).
 *       ts:         '',                             // optional - SQL Server Query Store snapshot timestamp, ignored for ASE
 *
 *       dbname:     'mydb',                        // current database name - required for the srv-based lookup above
 *       provider:   'claude',                      // optional - defaults to the server-configured default provider
 *       preview:    false,                          // optional - if true, build and show the exact prompt that
 *                                                   // would be sent (via /api/llm/optimize-sql's preview mode)
 *                                                   // instead of actually calling the LLM provider. Works even
 *                                                   // when DbxCentral.llm.enabled is off (no credentials/config
 *                                                   // touched) - handy to let the user paste the same prompt
 *                                                   // into a different LLM chat and compare answers.
 *       target:     document.getElementById('x')   // optional - a DOM element (or CSS selector) to render into
 *                                                   // directly instead of a floating modal. Use this on pages
 *                                                   // dedicated to showing the advice (e.g. /llm-advice).
 *   });
 *
 * If sql-formatter (window.sqlFormatter) and Prism (window.Prism, with the
 * sql component) are loaded on the host page, the suggested SQL is pretty-
 * printed and syntax highlighted; otherwise it's shown as plain text. Neither
 * is a hard dependency.
 *
 * No jQuery/Bootstrap dependency otherwise - this needs to work on the
 * minimal standalone Showplan viewer page too. Vanilla DOM + fetch() only.
 *
 * Before rendering ANY trigger button/section that would call open(), callers must first check:
 *
 *   dbxLlmAdvice.isEnabled().then(function (enabled) { if (enabled) { ...render the button... } });
 *
 * This reflects the DbxCentral.llm.enabled master switch - the feature may be turned off entirely,
 * in which case no trigger UI should be presented at all, not just a button that errors on click.
 ******************************************************************************/

var dbxLlmAdvice = (function () {

	var MODAL_ID = 'dbx-llm-advice-modal';

	//--------------------------------------------------------------------------
	// CSS (injected once)
	//--------------------------------------------------------------------------
	function injectStyle()
	{
		if (document.getElementById('dbx-llm-advice-style'))
			return;

		var style = document.createElement('style');
		style.id = 'dbx-llm-advice-style';
		style.textContent =
			'.dbx-llm-backdrop { position:fixed; top:0; left:0; width:100%; height:100%;' +
			'  background:rgba(0,0,0,0.5); z-index:10000; }' +
			'.dbx-llm-dialog { position:fixed; top:5%; left:50%; transform:translateX(-50%);' +
			'  width:min(760px, 92vw); max-height:88vh; overflow:auto; background:#fff; z-index:10001;' +
			'  border-radius:6px; box-shadow:0 4px 24px rgba(0,0,0,0.35); }' +
			'.dbx-llm-header { background:#454545; color:#fff; padding:12px 16px; border-radius:6px 6px 0 0;' +
			'  display:flex; justify-content:space-between; align-items:center; }' +
			'.dbx-llm-header h5 { margin:0; font-size:1.1rem; }' +
			'.dbx-llm-close { cursor:pointer; background:none; border:none; color:#fff; font-size:1.3rem; line-height:1; }' +
			'.dbx-llm-body { padding:16px; font-family:Arial, Helvetica, sans-serif; }' +
			'.dbx-llm-status { color:#495057; margin-bottom:10px; font-family:Arial, Helvetica, sans-serif; }' +
			'.dbx-llm-spinner { display:inline-block; width:16px; height:16px; border:2px solid #ccc;' +
			'  border-top-color:#454545; border-radius:50%; animation:dbx-llm-spin 0.8s linear infinite; margin-right:8px; vertical-align:middle; }' +
			'@keyframes dbx-llm-spin { to { transform:rotate(360deg); } }' +
			'.dbx-llm-section-title { font-family:Arial, Helvetica, sans-serif; font-weight:bold; font-size:1rem; margin-top:16px; margin-bottom:4px; }' +
			'.dbx-llm-section-details { margin-top:16px; }' +
			'.dbx-llm-section-details > .dbx-llm-section-title { margin-top:0; margin-bottom:4px; cursor:pointer; }' +
			// font-size needs !important: renderSqlBlock()'s <pre> holds a <code class="language-sql">
			// that Prism.highlightElement() (see highlightAllSqlBlocks()) auto-copies its "language-sql"
			// class onto once highlighted - after that, Prism's own "pre[class*=language-] { font-size:1em }"
			// (specificity 0,1,1) outranks this plain class selector (0,1,0) and silently wins, so SQL
			// blocks render at the browser default (~16px) regardless of what's set here.
			'.dbx-llm-pre { background:#f8f9fa; border-left:4px solid #007bff; padding:10px; margin:0;' +
			'  font-family:"Courier New", monospace; font-size:0.85rem !important; white-space:pre-wrap; word-break:break-word; overflow-x:auto; }' +
			// max-width caps the reading column at a sane width - without it, this section inherits
			// whatever width its ancestor happens to impose. In target mode (dbxLlmAdvice.open({target})
			// e.g. the ASE/SQL Server showplan dialogs' inline "LLM Optimization Advice" section), that
			// ancestor is the showplan dialog's `.scroll-tree` wrapper, which is a fixed 3000px wide to
			// make room for the graphical plan diagram - text here would otherwise wrap at 3000px
			// instead of the dialog's own visible width, forcing an unrelated horizontal scroll just to
			// read prose. 90vw keeps it responsive down to narrow viewports too.
			'.dbx-llm-prose { background:#f8f9fa; border-left:4px solid #6c757d; padding:12px 14px;' +
			'  font-family:Arial, Helvetica, sans-serif; font-size:0.95rem; line-height:1.5; word-break:break-word;' +
			'  max-width:min(900px, 90vw); box-sizing:border-box; }' +
			'.dbx-llm-prose p { margin:0 0 10px 0; }' +
			'.dbx-llm-prose p:last-child, .dbx-llm-prose ul:last-child, .dbx-llm-prose ol:last-child { margin-bottom:0; }' +
			'.dbx-llm-prose ul, .dbx-llm-prose ol { margin:0 0 10px 0; padding-left:22px; }' +
			'.dbx-llm-prose li { margin-bottom:4px; }' +
			'.dbx-llm-prose code { background:#e9ecef; padding:1px 4px; border-radius:3px; font-family:"Courier New", monospace; font-size:0.9em; }' +
			'.dbx-llm-error { color:#842029; background:#f8d7da; border:1px solid #f5c2c7; border-radius:4px; padding:10px;' +
			'  font-family:Arial, Helvetica, sans-serif; }' +
			'.dbx-llm-sent-details { margin-top:18px; font-family:Arial, Helvetica, sans-serif; font-size:0.9rem; }' +
			'.dbx-llm-sent-details summary { cursor:pointer; color:#495057; }' +
			'.dbx-llm-sent-field { margin-top:8px; }' +
			'.dbx-llm-sent-field-label { font-weight:bold; font-size:0.85rem; color:#495057; }' +
			'.dbx-llm-sql-changed { color:#664d03; background:#fff3cd; border:1px solid #ffecb5; border-radius:4px;' +
			'  padding:6px 10px; margin-bottom:6px; font-family:Arial, Helvetica, sans-serif; font-size:0.85rem; }' +
			'.dbx-llm-sql-unchanged { color:#0f5132; background:#d1e7dd; border:1px solid #badbcc; border-radius:4px;' +
			'  padding:6px 10px; margin-bottom:6px; font-family:Arial, Helvetica, sans-serif; font-size:0.85rem; }' +
			'.dbx-llm-copy-btn { margin-left:8px; font-size:0.78rem; padding:1px 8px; cursor:pointer;' +
			'  border:1px solid #ced4da; border-radius:3px; background:#fff; color:#495057; }' +
			'.dbx-llm-copy-btn:hover { background:#e9ecef; }' +
			'.dbx-llm-pre-compact { font-size:0.78rem !important; max-height:220px; overflow:auto; }';
		document.head.appendChild(style);
	}

	//--------------------------------------------------------------------------
	// Modal DOM (injected once, reused across calls) - only used when no
	// "target" element is given to open()
	//--------------------------------------------------------------------------
	function ensureModal()
	{
		var existing = document.getElementById(MODAL_ID);
		if (existing)
			return existing;

		var backdrop = document.createElement('div');
		backdrop.className = 'dbx-llm-backdrop';
		backdrop.id = MODAL_ID + '-backdrop';
		backdrop.style.display = 'none';

		var dialog = document.createElement('div');
		dialog.className = 'dbx-llm-dialog';
		dialog.id = MODAL_ID;
		dialog.style.display = 'none';
		dialog.innerHTML =
			'<div class="dbx-llm-header">' +
			'  <h5>Get LLM Optimization Advice</h5>' +
			'  <button type="button" class="dbx-llm-close" aria-label="Close">&times;</button>' +
			'</div>' +
			'<div class="dbx-llm-body" id="' + MODAL_ID + '-body"></div>';

		document.body.appendChild(backdrop);
		document.body.appendChild(dialog);

		dialog.querySelector('.dbx-llm-close').addEventListener('click', close);
		backdrop.addEventListener('click', close);

		return dialog;
	}

	function showModal()
	{
		document.getElementById(MODAL_ID + '-backdrop').style.display = 'block';
		document.getElementById(MODAL_ID).style.display = 'block';
	}

	function close()
	{
		var backdrop = document.getElementById(MODAL_ID + '-backdrop');
		var dialog   = document.getElementById(MODAL_ID);
		if (backdrop) backdrop.style.display = 'none';
		if (dialog)   dialog.style.display   = 'none';
	}

	/** Resolve the container to render into: opts.target (element or selector) for inline mode, or the modal body. */
	function resolveContainer(opts)
	{
		if (opts.target)
		{
			var el = (typeof opts.target === 'string') ? document.querySelector(opts.target) : opts.target;
			if (el) return el;
			console.warn('dbxLlmAdvice: opts.target did not resolve to an element, falling back to modal.');
		}

		ensureModal();
		showModal();
		return document.getElementById(MODAL_ID + '-body');
	}

	//--------------------------------------------------------------------------
	// Rendering helpers
	//--------------------------------------------------------------------------
	function escapeHtml(s)
	{
		if (s == null) return '';
		return String(s)
			.replace(/&/g, '&amp;')
			.replace(/</g, '&lt;')
			.replace(/>/g, '&gt;')
			.replace(/"/g, '&quot;');
	}

	function renderStatus(container, msg)
	{
		container.innerHTML = '<div class="dbx-llm-status"><span class="dbx-llm-spinner"></span>' + escapeHtml(msg) + '</div>';
	}

	/**
	 * Render an error, but still show "what was sent" underneath it (same as a successful result) -
	 * so a failed call is still useful for debugging instead of just an opaque error message.
	 * @param serverData the parsed JSON body of a failed HTTP response, if any (may carry
	 *                    'promptSent' when the server got far enough to build the prompt before
	 *                    the call to the LLM provider itself failed) - see fetchJson().
	 */
	function renderError(container, msg, opts, ddlContext, serverData)
	{
		var html = '<div class="dbx-llm-error">' + escapeHtml(msg) + '</div>';
		html += opts ? renderInputContext(opts) : '';
		if (opts)
			html += renderSentDetails(opts, ddlContext, serverData && serverData.promptSent, serverData && serverData.providerId);
		container.innerHTML = html;
		highlightAllSqlBlocks(container);
	}

	/** Guess a reasonable sql-formatter "language" dialect from a DBMS product name. */
	function sqlFormatterDialect(dbVendor)
	{
		var v = (dbVendor || '').toLowerCase();
		if (v.indexOf('sql server') >= 0) return 'tsql';
		if (v.indexOf('sybase') >= 0 || v.indexOf('adaptive server') >= 0) return 'tsql'; // closest available dialect to Sybase T-SQL
		if (v.indexOf('postgres') >= 0) return 'postgresql';
		return 'sql';
	}

	/**
	 * Render a block of SQL, pretty-printed + syntax highlighted when sql-formatter/Prism are available.
	 * @param compact  when true, renders smaller and height-capped (scrollable) - for reference/input
	 *                 material (the original SQL Text, alongside the Execution Plan) as opposed to the
	 *                 LLM's actual answer (Suggested SQL), which stays full-size since it's the point.
	 */
	function renderSqlBlock(sql, dbVendor, compact)
	{
		var formatted = sql;
		if (typeof window.sqlFormatter !== 'undefined' && typeof window.sqlFormatter.format === 'function')
		{
			try { formatted = window.sqlFormatter.format(sql, { language: sqlFormatterDialect(dbVendor), keywordCase: 'upper' }); }
			catch (e) { console.warn('dbxLlmAdvice: sqlFormatter.format() failed, showing SQL as-is: ' + e.message); }
		}

		var preClass = compact ? 'dbx-llm-pre dbx-llm-pre-compact' : 'dbx-llm-pre';

		if (typeof window.Prism !== 'undefined')
		{
			var id = 'dbx-llm-sql-' + Math.random().toString(36).slice(2);
			// Highlight after insertion (see highlightAllSqlBlocks()) since Prism needs the element in the DOM.
			return { html: '<pre class="' + preClass + '"><code id="' + id + '" class="language-sql">' + escapeHtml(formatted) + '</code></pre>', highlightId: id };
		}

		return { html: '<pre class="' + preClass + '">' + escapeHtml(formatted) + '</pre>', highlightId: null };
	}

	/** Apply inline Markdown (**bold**, `code`) to already-HTML-escaped text. */
	function inlineMarkdown(escapedText)
	{
		return escapedText
			.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
			.replace(/`([^`]+)`/g, '<code>$1</code>');
	}

	/**
	 * Minimal Markdown-to-HTML fallback for when the vendored `marked` library isn't loaded on the
	 * host page: paragraphs, bullet lists ('- '/'* '), numbered lists ('1. '), **bold** and `code`.
	 * No headings/tables/nested lists/links. Input is HTML-escaped first, so this only ever adds
	 * our own controlled tags around already-safe text.
	 */
	function renderMarkdownFallback(text)
	{
		var escaped = escapeHtml(text);
		var blocks  = escaped.split(/\n\s*\n/);

		return blocks.map(function (block)
		{
			var lines = block.split('\n').filter(function (l) { return l.trim() !== ''; });
			if (lines.length === 0) return '';

			var isBullet   = lines.every(function (l) { return /^\s*[-*]\s+/.test(l); });
			var isNumbered = !isBullet && lines.every(function (l) { return /^\s*\d+[.)]\s+/.test(l); });

			if (isBullet)
				return '<ul>' + lines.map(function (l) { return '<li>' + inlineMarkdown(l.replace(/^\s*[-*]\s+/, '')) + '</li>'; }).join('') + '</ul>';

			if (isNumbered)
				return '<ol>' + lines.map(function (l) { return '<li>' + inlineMarkdown(l.replace(/^\s*\d+[.)]\s+/, '')) + '</li>'; }).join('') + '</ol>';

			return '<p>' + lines.map(inlineMarkdown).join('<br>') + '</p>';
		}).join('');
	}

	/**
	 * Render Markdown text (the LLM's explanation) as HTML. Prefers the vendored `marked` library
	 * (resources/WebContent/scripts/marked) for full CommonMark support, sanitizing its output with
	 * the vendored DOMPurify (resources/WebContent/scripts/dompurify) since `marked` does not escape
	 * raw HTML embedded in its input by itself - this is model-generated text, not text we wrote, so
	 * it must be treated as untrusted. Falls back to a small hand-rolled subset (already-safe, since
	 * it HTML-escapes first) if either library isn't loaded on the host page.
	 */
	function renderMarkdown(text)
	{
		if (typeof window.marked !== 'undefined' && typeof window.marked.parse === 'function')
		{
			try
			{
				var rawHtml = window.marked.parse(text);
				if (typeof window.DOMPurify !== 'undefined' && typeof window.DOMPurify.sanitize === 'function')
					return window.DOMPurify.sanitize(rawHtml);

				console.warn('dbxLlmAdvice: marked is loaded but DOMPurify is not - using the safer built-in Markdown subset instead.');
			}
			catch (e)
			{
				console.warn('dbxLlmAdvice: marked.parse() failed, falling back to the built-in Markdown subset: ' + e.message);
			}
		}

		return renderMarkdownFallback(text);
	}

	/**
	 * Raw text for pending "Copy" buttons, keyed by a per-render id - looked up by the single,
	 * page-lifetime-scoped click handler registered in ensureCopyButtonHandler() below. Kept out of
	 * the DOM (rather than e.g. a data-* attribute) since prompt/SQL text can be arbitrarily large
	 * and contain quotes/newlines that would need careful escaping to round-trip through an attribute.
	 */
	var _copyRegistry = {};
	var _copyBtnHandlerInstalled = false;

	/** Copy raw text to the clipboard using the same execCommand('copy') approach as dbxSqlText.js's copy button, for consistency and since it works without a secure (https/localhost) context. */
	function copyTextToClipboard(text, btnEl)
	{
		var textArea = document.createElement('textarea');
		textArea.value = text;
		document.body.appendChild(textArea);
		textArea.select();
		try
		{
			document.execCommand('copy');
			if (btnEl)
			{
				var orig = btnEl.textContent;
				btnEl.textContent = 'Copied!';
				setTimeout(function () { btnEl.textContent = orig; }, 1500);
			}
		}
		catch (err)
		{
			alert('Unable to copy to clipboard\n\n' + err);
		}
		document.body.removeChild(textArea);
	}

	/**
	 * Installed once (not per-render, since the modal body / inline target element is reused across
	 * repeated dbxLlmAdvice.open() calls) - a single delegated click listener on document handles
	 * every "Copy" button this module ever renders, looking up its raw text in _copyRegistry.
	 */
	function ensureCopyButtonHandler()
	{
		if (_copyBtnHandlerInstalled) return;
		_copyBtnHandlerInstalled = true;

		document.addEventListener('click', function (e)
		{
			var btn = e.target.closest && e.target.closest('.dbx-llm-copy-btn');
			if (!btn) return;

			// A copy button can sit inside a <summary> (the "Show what was sent" toggle) - without
			// this, clicking it would also open/close the <details> since the click bubbles up.
			e.preventDefault();
			e.stopPropagation();

			var text = _copyRegistry[btn.getAttribute('data-copy-id')];
			if (text) copyTextToClipboard(text, btn);
		});
	}

	/** A "Copy" button wired to _copyRegistry; '' if there's nothing to copy. */
	function makeCopyButtonHtml(text)
	{
		if (!text) return '';

		ensureCopyButtonHandler();
		var copyId = 'dbx-llm-copy-' + Math.random().toString(36).slice(2);
		_copyRegistry[copyId] = text;
		return ' <button type="button" class="dbx-llm-copy-btn" data-copy-id="' + copyId + '">Copy</button>';
	}

	function renderSentField(label, value, copyable)
	{
		if (!value) return '';

		var copyBtnHtml = copyable ? makeCopyButtonHtml(value) : '';

		return '<div class="dbx-llm-sent-field"><div class="dbx-llm-sent-field-label">' + escapeHtml(label) + ':' + copyBtnHtml + '</div>'
			+ '<pre class="dbx-llm-pre">' + escapeHtml(value) + '</pre></div>';
	}

	/**
	 * Plain-text rendition of everything that was (or would have been) sent to the LLM, for the
	 * "Show what was sent" summary-line Copy button - lets the user paste the exact same context
	 * into some other LLM (e.g. a browser-based chat) when they don't want to use this feature's
	 * own provider, or when the call failed. Mirrors the fallback fields rendered below the summary
	 * when promptSent isn't available.
	 */
	function buildFallbackSentText(opts, ddlContext)
	{
		var parts = [];
		if (opts.dbVendor)        parts.push('DBMS vendor:\n' + opts.dbVendor);
		if (opts.sql)             parts.push('SQL:\n' + opts.sql);
		if (ddlContext)           parts.push('DDL / index / stats context:\n' + ddlContext);
		if (opts.plan)            parts.push('Execution plan:\n' + opts.plan);
		if (opts.workloadProfile) parts.push('Workload profile:\n' + opts.workloadProfile);
		return parts.join('\n\n');
	}

	//--------------------------------------------------------------------------------------------
	// BEGIN: Workload profile
	//--------------------------------------------------------------------------------------------
	//
	// The Daily Summary Report shows, per statement, a "sparkline" sub-table: one row per metric
	// (exec-cnt, exec-time, cpu-time, l-read, ...) with a chart, a Total and an Avg per exec.
	// That is exactly the context an optimizer wants -- how often does this run, and WHEN.
	//
	// The report's dsrHarvestWorkload() (see SparklineHelper.getWorkloadHarvesterJs() on the Java side)
	// reads that table out of the page at CLICK time and passes it here as 'workloadData' JSON:
	//
	//     { begin: '2026-01-11 00:00', intervalMin: 10,
	//       rows: [ { k:'exec-cnt', t:'103', a:'', u:'', v:'0,0,3,12,...' }, ... ] }
	//
	//   k = metric name (the sub-table's row label)   t = Total       a = Avg per exec
	//   u = unit ('ms', 'pgs', ...)                   v = the sparkline datapoints, one per interval
	//
	// Nothing is stored in the report for this -- 'v' is simply the values='...' attribute that already
	// draws the chart. Keeping the analysis HERE (rather than in the report) also means that improving
	// the wording or the heuristics below improves already-archived reports too.
	//
	// NOTE: 't' and 'a' are RENDERED display strings and are locale formatted ('1 173 635', '218,8').
	//       They are passed through verbatim and must never be re-parsed. Only 'v' is parsed, and that
	//       one is locale independent (Java Number.toString()).
	//--------------------------------------------------------------------------------------------

	/** Bucket index -> 'HH:MM' (or 'MM-DD HH:MM' when the period spans more than one day). */
	function wlTimeOfBucket(begin, intervalMin, idx, multiDay)
	{
		// 'begin' is 'yyyy-MM-dd HH:mm' -- parse by hand, Date('2026-01-11 00:00') is not portable
		var m = /^(\d{4})-(\d{2})-(\d{2})[ T](\d{2}):(\d{2})/.exec(begin || '');
		if (!m) return '#' + idx;

		var d = new Date(+m[1], +m[2] - 1, +m[3], +m[4], +m[5]);
		d.setMinutes(d.getMinutes() + idx * (intervalMin || 10));

		function p2(n) { return (n < 10 ? '0' : '') + n; }

		var hhmm = p2(d.getHours()) + ':' + p2(d.getMinutes());
		return multiDay ? (p2(d.getMonth() + 1) + '-' + p2(d.getDate()) + ' ' + hhmm) : hhmm;
	}

	/** Compact number for the prose ('peak 2 737 528 at ...') - the raw datapoints are plain numbers. */
	function wlNum(n)
	{
		if (!isFinite(n)) return String(n);
		// Group thousands and cap at 1 decimal, so a derived value reads like the report's own numbers
		return n.toLocaleString(undefined, { maximumFractionDigits: 1 });
	}

	/**
	 * Describe the SHAPE of one metric's datapoints over time - the whole point of this file's
	 * contribution to the prompt. Deliberately conservative: it states what the numbers show
	 * (where the load sits, how wide the active window is, where the peak is) and never speculates.
	 */
	function wlDescribeSeries(csv, begin, intervalMin, multiDay, unit)
	{
		var parts = String(csv).split(',');
		var vals = [];
		for (var i = 0; i < parts.length; i++)
		{
			// An empty entry means "no data in this interval" (SparklineHelper.doNotShowZeroValues)
			var v = parseFloat(parts[i]);
			vals.push(isFinite(v) ? v : 0);
		}
		var n = vals.length;
		if (n === 0) return '';

		var total = 0, max = -Infinity, maxIdx = 0, active = 0, first = -1, last = -1;
		for (var j = 0; j < n; j++)
		{
			var v2 = vals[j];
			total += v2;
			if (v2 > max) { max = v2; maxIdx = j; }
			if (v2 > 0) { active++; if (first < 0) first = j; last = j; }
		}

		if (active === 0 || total <= 0)
			return 'no activity in the period';

		var at = function (idx) { return wlTimeOfBucket(begin, intervalMin, idx, multiDay); };
		// State the unit on the peak - it is a derived number, and without a unit "peak 8 545" is
		// ambiguous next to a Total the report renders as "00:00:08"
		var peakStr = ', peak ' + wlNum(max) + (unit ? ' ' + unit : '') + ' at ' + at(maxIdx);

		if (active === 1)
			return 'a single ' + (intervalMin || 10) + '-minute interval at ' + at(first) + peakStr;

		// How concentrated is the load? (share of the total held by the biggest 1 and biggest 3 intervals)
		var sorted = vals.slice().sort(function (a, b) { return b - a; });
		var top1 = sorted[0] / total;
		var top3 = (sorted[0] + (sorted[1] || 0) + (sorted[2] || 0)) / total;

		if (top1 >= 0.5)
			return 'dominated by one peak at ' + at(maxIdx) + ' (' + Math.round(top1 * 100) + '% of the total)'
				+ ', otherwise ' + (active > n * 0.5 ? 'low but continuous' : 'mostly idle') + peakStr;

		if (top3 >= 0.8)
		{
			// name the actual times of the 3 biggest intervals, in time order
			var idxs = [];
			for (var k = 0; k < n; k++) idxs.push(k);
			idxs.sort(function (a, b) { return vals[b] - vals[a]; });
			var top3Idx = idxs.slice(0, 3).sort(function (a, b) { return a - b; });
			var times = [];
			for (var t = 0; t < top3Idx.length; t++) times.push(at(top3Idx[t]));
			return 'concentrated in a few peaks at ' + times.join(', ') + ' (' + Math.round(top3 * 100) + '% of the total)' + peakStr;
		}

		var spanPct = (last - first + 1) / n;
		var meanActive = total / active;
		var even = max <= meanActive * 3;

		if (spanPct >= 0.9)
			return (even ? 'spread fairly evenly across the whole period' : 'active across the whole period but uneven')
				+ ' (' + active + ' of ' + n + ' intervals active)' + peakStr;

		return 'only between ' + at(first) + ' and ' + at(last)
			+ ' (' + active + ' of ' + n + ' intervals active, ' + (even ? 'fairly even' : 'uneven') + ' within that window)'
			+ peakStr;
	}

	/**
	 * Turn the harvested 'workloadData' JSON into the plain text that goes into the prompt.
	 * Returns '' when there is nothing usable, so callers can just assign the result.
	 */
	function buildWorkloadProfile(workloadData)
	{
		if (!workloadData) return '';

		var data;
		try { data = (typeof workloadData === 'string') ? JSON.parse(workloadData) : workloadData; }
		catch (e) { console.log('dbxLlmAdvice: could not parse workloadData: ' + e); return ''; }

		if (!data || !data.rows || !data.rows.length) return '';

		var intervalMin = data.intervalMin || 10;
		var begin = data.begin || '';

		// How many intervals do we have? (used for the header, and to decide HH:MM vs MM-DD HH:MM)
		var nPoints = 0;
		for (var i = 0; i < data.rows.length; i++)
		{
			if (data.rows[i].v)
				nPoints = Math.max(nPoints, String(data.rows[i].v).split(',').length);
		}
		var multiDay = (nPoints * intervalMin) > 24 * 60;

		var lines = [];
		if (begin && nPoints)
		{
			lines.push('Measured over ' + begin + ' - ' + wlTimeOfBucket(begin, intervalMin, nPoints - 1, multiDay)
				+ ' (' + nPoints + ' x ' + intervalMin + '-minute intervals).');
		}
		lines.push('Each metric below: Total for the period, Avg per execution, and how the usage is distributed over time.');
		lines.push('');

		for (var r = 0; r < data.rows.length; r++)
		{
			var row = data.rows[r];
			if (!row || !row.k) continue;

			// Rows with no chart and no numbers (e.g. 'note') carry free text in 'n'
			if (row.n)
			{
				lines.push('  ' + row.k + ': ' + row.n);
				continue;
			}

			// The underlying DBMS column name, when we could derive it from the sparkline's css class.
			// It often carries the unit that the display label lacks (Elapsed_ms, LogicalReadsMb, MemUsageKB).
			var head = row.k + ((row.c && row.c !== row.k) ? ' (' + row.c + ')' : '');
			var nums = [];
			// 'n/a' is what the report itself prints where a Total is meaningless (e.g. 'l-read/row') -
			// passing it on as "Total n/a" would just be noise to the LLM
			if (row.t && row.t.toLowerCase() !== 'n/a') nums.push('Total ' + row.t + (row.u ? ' ' + row.u : ''));
			if (row.a) nums.push('Avg per exec ' + row.a + (row.u ? ' ' + row.u : ''));

			var shape = row.v ? wlDescribeSeries(row.v, begin, intervalMin, multiDay, row.u) : '';

			var line = '  ' + head;
			if (nums.length) line += ': ' + nums.join(', ');
			if (shape)       line += (nums.length ? '  --  ' : ': ') + shape;
			lines.push(line);
		}

		// The datapoints themselves. The prose above is our reading of the shape; give the model the
		// numbers as well so it can spot things the heuristics do not describe (a ramp, a plateau, a
		// repeating cycle, one interval that is 100x its neighbours, ...) instead of having to trust us.
		var csv = [];
		for (var c = 0; c < data.rows.length; c++)
		{
			var crow = data.rows[c];
			if (!crow || !crow.k || !crow.v) continue;

			// An empty entry means "no data in this interval" (SparklineHelper.doNotShowZeroValues).
			// Spell it as 0 so every series has the same length and nothing has to be guessed.
			var vals = String(crow.v).split(',');
			for (var i = 0; i < vals.length; i++)
			{
				var t = vals[i].trim();
				vals[i] = t === '' ? '0' : t;
			}

			csv.push('  ' + crow.k + ((crow.c && crow.c !== crow.k) ? ' (' + crow.c + ')' : '') + ': ' + vals.join(','));
		}

		if (csv.length)
		{
			lines.push('');
			lines.push('Below are the raw values behind those descriptions, for the same period, if you want to analyse them yourself.');
			lines.push('One value per interval, in time order' + (begin ? ', starting at ' + begin : '')
				+ ' and ' + intervalMin + ' minutes apart' + (nPoints ? ' (' + nPoints + ' values per metric)' : '') + '.');
			lines.push('');
			lines = lines.concat(csv);
		}

		return lines.join('\n');
	}
	//--------------------------------------------------------------------------------------------
	// END: Workload profile
	//--------------------------------------------------------------------------------------------

	/**
	 * The "Show what was sent to the LLM" collapsible - shared by both the success and error
	 * renderers, so this is visible regardless of whether the call actually succeeded.
	 * @param promptSent  the exact prompt text, if known (preferred - most precise)
	 * @param providerId  resolved provider id, if known (falls back to opts.provider / '(default)')
	 * @param model       specific model used, if known
	 */
	function renderSentDetails(opts, ddlContext, promptSent, providerId, model)
	{
		// Each render replaces the previous one's DOM, so its "Copy" button(s) can never be clicked
		// again; reset here rather than growing _copyRegistry across repeated dbxLlmAdvice.open()
		// calls in a long-lived page session.
		_copyRegistry = {};

		var providerLabel = escapeHtml(providerId || opts.provider || '(default)');
		if (model) providerLabel += ' / ' + escapeHtml(model);

		var fullText = promptSent || buildFallbackSentText(opts, ddlContext);

		var html = '<details class="dbx-llm-sent-details">'
			+ '<summary>Show what was sent to the LLM (provider: ' + providerLabel + ')' + makeCopyButtonHtml(fullText) + '</summary>';
		if (promptSent)
			html += renderSentField('Full prompt sent (' + promptSent.length.toLocaleString() + ' chars)', promptSent, /*copyable*/ true);
		else
		{
			html += renderSentField('SQL', opts.sql);
			html += renderSentField('DDL / index / stats context', ddlContext);
			html += renderSentField('Execution plan', opts.plan);
			html += renderSentField('Workload profile', opts.workloadProfile);
			html += renderSentField('DBMS vendor', opts.dbVendor);
		}
		html += '</details>';
		return html;
	}

	/**
	 * Collapse whitespace and lower-case, so pretty-printing/keyword-casing differences introduced
	 * by renderSqlBlock()'s sql-formatter pass (or by the LLM itself, e.g. trailing whitespace)
	 * don't register as a "changed" SQL statement - only an actual rewrite should.
	 */
	function normalizeSqlForCompare(sql)
	{
		if (!sql) return '';
		return String(sql).replace(/\s+/g, ' ').trim().toLowerCase();
	}

	/**
	 * The original SQL Text (and, if supplied, the Execution Plan) that was analyzed - shown
	 * up-front, compact, on both success and error, so the reader has that context without having
	 * to open "Show what was sent to the LLM" and pick it out of the full prompt text. Kept
	 * noticeably smaller than the "Suggested SQL" block below it (see renderSqlBlock's compact
	 * flag / .dbx-llm-pre-compact) since this is reference material, not the answer. Each is its
	 * own <details> (open by default, same as before) rather than a plain block, so it can be
	 * collapsed out of the way once read - both can get long (a full showplan especially).
	 */
	function renderInputContext(opts)
	{
		var html = '';

		if (opts.sql)
		{
			var sqlTextBlock = renderSqlBlock(opts.sql, opts.dbVendor, /*compact*/ true);
			html += '<details class="dbx-llm-section-details" open>'
				+ '<summary class="dbx-llm-section-title">SQL Text</summary>' + sqlTextBlock.html + '</details>';
		}
		if (opts.plan)
		{
			html += '<details class="dbx-llm-section-details" open>'
				+ '<summary class="dbx-llm-section-title">Execution Plan</summary>'
				+ '<pre class="dbx-llm-pre dbx-llm-pre-compact">' + escapeHtml(opts.plan) + '</pre></details>';
		}
		if (opts.workloadProfile)
		{
			// Plain text (see buildWorkloadProfile), so NOT renderSqlBlock()
			html += '<details class="dbx-llm-section-details" open>'
				+ '<summary class="dbx-llm-section-title">Workload Profile</summary>'
				+ '<pre class="dbx-llm-pre dbx-llm-pre-compact">' + escapeHtml(opts.workloadProfile) + '</pre></details>';
		}

		return html;
	}

	/** Prism-highlight every SQL code block in container - there can now be more than one (SQL Text + Suggested SQL). */
	function highlightAllSqlBlocks(container)
	{
		if (typeof window.Prism === 'undefined') return;
		var codeEls = container.querySelectorAll('code.language-sql');
		for (var i = 0; i < codeEls.length; i++)
			window.Prism.highlightElement(codeEls[i]);
	}

	function renderResult(container, result, opts, ddlContext)
	{
		var html = renderInputContext(opts);

		if (result.optimizedSql)
		{
			// Compare against the model's own echo of the SQL it analyzed (origin_sql) when
			// available - more accurate than opts.sql if the model reformatted it in its echo -
			// falling back to opts.sql (what we actually sent) otherwise. Kept as a secondary check:
			// the prompt already asks the model to leave optimized_sql empty when unchanged, this
			// just catches the case where it returns identical SQL anyway.
			var baselineSql = result.originSql || opts.sql;
			var sqlChanged  = normalizeSqlForCompare(result.optimizedSql) !== normalizeSqlForCompare(baselineSql);
			var sqlBlock    = renderSqlBlock(result.optimizedSql, opts.dbVendor);
			html += '<details class="dbx-llm-section-details" open><summary class="dbx-llm-section-title">Suggested SQL</summary>';
			html += sqlChanged
				? '<div class="dbx-llm-sql-changed">SQL was changed by the LLM.</div>'
				: '<div class="dbx-llm-sql-unchanged">SQL was <b>not</b> changed - the LLM returned the same statement.</div>';
			html += sqlBlock.html + '</details>';
		}
		else if (result.explanation)
		{
			// The model is asked to leave 'optimized_sql' empty (not echo the original back) when
			// it has no rewrite to suggest - a real, expected outcome, not a missing answer.
			html += '<details class="dbx-llm-section-details" open><summary class="dbx-llm-section-title">Suggested SQL</summary>';
			html += '<div class="dbx-llm-sql-unchanged">No SQL changes suggested - the LLM found the statement fine as-is.</div></details>';
		}

		if (result.explanation)
		{
			// Rendered as (a small subset of) Markdown - the prompt asks the model for
			// bullet/numbered lists, **bold** and `code` spans - not raw escaped text.
			html += '<details class="dbx-llm-section-details" open><summary class="dbx-llm-section-title">Explanation</summary>';
			html += '<div class="dbx-llm-prose">' + renderMarkdown(result.explanation) + '</div></details>';
		}
		if (!result.optimizedSql && !result.explanation)
			html += '<div class="dbx-llm-status">The model did not return a usable answer.</div>';

		// "What was sent" - so the user can see exactly what context/prompt/model was used.
		html += renderSentDetails(opts, ddlContext, result.promptSent, result.providerId, result.model);

		container.innerHTML = html;
		highlightAllSqlBlocks(container);
	}

	//--------------------------------------------------------------------------
	// Network calls
	//--------------------------------------------------------------------------
	function fetchJson(url, options)
	{
		return fetch(url, options).then(function (resp)
		{
			return resp.text().then(function (text)
			{
				var data;
				try
				{
					data = text ? JSON.parse(text) : {};
				}
				catch (parseErr)
				{
					// Non-JSON response (e.g. a 404/500 from Jetty's default error page, most likely
					// because DbxCentral hasn't been restarted yet with the endpoint registered).
					throw new Error('Server returned a non-JSON response (HTTP ' + resp.status + '). '
						+ 'Has DbxCentral been restarted since this feature was deployed?');
				}

				if (!resp.ok)
				{
					var msg = (data && (data.message || data.error)) || ('HTTP ' + resp.status);
					var httpErr = new Error(msg);
					httpErr.responseData = data; // may carry 'promptSent' - see LlmSqlOptimizeServlet
					throw httpErr;
				}
				return data;
			});
		});
	}

	function fetchDdlContextByJdbcUrl(opts)
	{
		var qs = new URLSearchParams();
		qs.set('jdbcUrl',  opts.jdbcUrl);
		if (opts.jdbcUser) qs.set('jdbcUser', opts.jdbcUser);
		if (opts.jdbcPass) qs.set('jdbcPass', opts.jdbcPass);
		qs.set('sql',      opts.sql || '');
		qs.set('dbVendor', opts.dbVendor || '');
		if (opts.dbname)   qs.set('dbname', opts.dbname);

		return fetchJson('/api/llm/context?' + qs.toString())
			.then(function (data) { return data.ddlContext || ''; })
			.catch(function (err)
			{
				console.warn('dbxLlmAdvice: /api/llm/context failed, continuing without DDL context: ' + err.message);
				return '';
			});
	}

	/**
	 * Live srv/dbname-based DDL/index/stats lookup - parses 'sql' for table names client-side (via
	 * dbxSqlTableNames.js, if loaded), then asks that server's currently-running Collector for their
	 * DDL/index/stats via /api/cc/mgt/table-info (ASE) or /api/cc/mgt/query-store (SQL Server) - the
	 * same endpoints dbxShowplan.js's own "Get LLM Optimization Advice" section already uses.
	 * Resolves to '' (never rejects) on any failure - missing dbxSqlTableNames.js, unsupported vendor,
	 * no tables found, srv unreachable, etc. - so callers can always fall back to SQL-only advice.
	 */
	function fetchDdlContextBySrv(opts)
	{
		var isAse = (opts.dbVendor === 'Adaptive Server Enterprise');
		var isMs  = (opts.dbVendor === 'Microsoft SQL Server');
		if (!isAse && !isMs)
			return Promise.resolve('');

		if (typeof DbxSqlTableNames === 'undefined' || !DbxSqlTableNames.extractTablesAsync)
			return Promise.resolve('');

		return new Promise(function (resolve)
		{
			DbxSqlTableNames.extractTablesAsync(opts.sql, function (tables)
			{
				if (!tables || !tables.length)
				{
					resolve('');
					return;
				}

				var qs = new URLSearchParams();
				var url;
				if (isAse)
				{
					url = '/api/cc/mgt/table-info';
					qs.set('srv', opts.srv);
					qs.set('dbVendor', opts.dbVendor);
					qs.set('format', 'text');
					qs.set('dbname', opts.dbname);
					qs.set('tables', tables.join(','));
				}
				else
				{
					url = '/api/cc/mgt/query-store';
					qs.set('srv', opts.srv);
					qs.set('action', 'tableInfo');
					qs.set('format', 'text');
					qs.set('dbname', opts.dbname);
					qs.set('tables', tables.join(','));
					qs.set('ts', opts.ts || '');
				}

				fetchJson(url + '?' + qs.toString())
					.then(function (data) { resolve((data && data.text) || ''); })
					.catch(function (err)
					{
						console.warn('dbxLlmAdvice: ' + url + ' failed, continuing without DDL context: ' + err.message);
						resolve('');
					});
			});
		});
	}

	function fetchDdlContext(opts)
	{
		if (opts.jdbcUrl)
			return fetchDdlContextByJdbcUrl(opts);

		if (opts.srv && opts.dbname)
			return fetchDdlContextBySrv(opts);

		return Promise.resolve('');
	}

	/**
	 * opts.preview true: render the exact prompt text that would be sent, via /api/llm/optimize-sql's
	 * preview mode - no provider is called, so this works even when the LLM feature itself is off.
	 */
	function renderPromptPreview(container, promptSent)
	{
		if (!promptSent)
		{
			container.innerHTML = '<div class="dbx-llm-error">Could not build a prompt preview.</div>';
			return;
		}

		container.innerHTML = ''
			+ '<div class="dbx-llm-section-title">Prompt that would be sent' + makeCopyButtonHtml(promptSent) + '</div>'
			+ '<pre class="dbx-llm-pre">' + escapeHtml(promptSent) + '</pre>';
	}

	function callOptimize(container, opts, ddlContext)
	{
		renderStatus(container, opts.preview ? 'Building prompt preview...' : 'Asking the LLM for optimization advice...');

		var body = {
			sql:             opts.sql,
			ddlContext:      ddlContext,
			plan:            opts.plan,
			workloadProfile: opts.workloadProfile,
			dbVendor:        opts.dbVendor,
			provider:        opts.provider,
			preview:         !!opts.preview
		};

		fetchJson('/api/llm/optimize-sql', {
			method:  'POST',
			headers: { 'Content-Type': 'application/json' },
			body:    JSON.stringify(body)
		})
		.then(function (data)
		{
			if (opts.preview) renderPromptPreview(container, data && data.promptSent);
			else              renderResult(container, data, opts, ddlContext);
		})
		.catch(function (err)
		{
			if (opts.preview) container.innerHTML = '<div class="dbx-llm-error">Failed to build prompt preview: ' + escapeHtml(err.message) + '</div>';
			else              renderError(container, 'Failed to get LLM advice: ' + err.message, opts, ddlContext, err.responseData);
		});
	}

	//--------------------------------------------------------------------------
	// Public API
	//--------------------------------------------------------------------------
	function open(opts)
	{
		opts = opts || {};
		if (!opts.sql)
		{
			console.error('dbxLlmAdvice.open(): a "sql" field is required.');
			return;
		}

		// Turn the raw datapoints harvested from the Daily Summary Report into prose, ONCE, here - so
		// every caller gets it, and every renderer below can just look at opts.workloadProfile.
		// A caller that already has the finished text can pass 'workloadProfile' directly instead.
		if (!opts.workloadProfile && opts.workloadData)
			opts.workloadProfile = buildWorkloadProfile(opts.workloadData);

		injectStyle();
		var container = resolveContainer(opts);
		renderStatus(container, 'Preparing request...');

		if (opts.ddlContext)
		{
			callOptimize(container, opts, opts.ddlContext);
		}
		else
		{
			renderStatus(container, 'Looking up table DDL/index/stats...');
			fetchDdlContext(opts).then(function (ddlContext) { callOptimize(container, opts, ddlContext); });
		}
	}

	//--------------------------------------------------------------------------
	// Feature toggle - DbxCentral.llm.enabled (see LlmClientRegistry.isFeatureEnabled()).
	// Fetched once, immediately, so it's already resolved by the time a user has clicked into any
	// dialog that wants to gate a button/section on it. Callers should await isEnabled() before
	// rendering ANY LLM-related trigger UI - not just before calling open() - since a disabled
	// feature must not leave dead-looking buttons/links visible.
	//--------------------------------------------------------------------------
	var _enabledPromise = fetch('/api/llm/config')
		.then(function (resp) { return resp.json(); })
		.then(function (data) { return !!(data && data.enabled); })
		.catch(function (err)
		{
			console.warn('dbxLlmAdvice: /api/llm/config failed, treating the feature as disabled: ' + err.message);
			return false;
		});

	function isEnabled()
	{
		return _enabledPromise;
	}

	return {
		open: open,
		close: close,
		isEnabled: isEnabled,
		// Exposed for callers that build their own /api/llm/optimize-sql request instead of going
		// through open() - notably dbxShowplan.js's "LLM Prompt Preview" - so the prompt they PREVIEW
		// is the same one open() would actually SEND.
		buildWorkloadProfile: buildWorkloadProfile
	};
})();
