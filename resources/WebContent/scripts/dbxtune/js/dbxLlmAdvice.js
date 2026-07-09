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
 *       ddlContext: '<DDL/index/stats text>',      // optional - if already known, skips the /api/llm/context lookup
 *       dbVendor:   'Adaptive Server Enterprise',  // required if ddlContext isn't supplied and a lookup is wanted
 *       jdbcUrl:    'jdbc:h2:...',                 // required if ddlContext isn't supplied and a lookup is wanted
 *       jdbcUser:   'sa',                          // optional, default 'sa'
 *       jdbcPass:   '',                            // optional
 *       dbname:     'mydb',                        // optional, current database name
 *       provider:   'claude',                      // optional - defaults to the server-configured default provider
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
			'.dbx-llm-pre { background:#f8f9fa; border-left:4px solid #007bff; padding:10px; margin:0;' +
			'  font-family:"Courier New", monospace; font-size:0.85rem; white-space:pre-wrap; word-break:break-word; overflow-x:auto; }' +
			'.dbx-llm-prose { background:#f8f9fa; border-left:4px solid #6c757d; padding:12px 14px;' +
			'  font-family:Arial, Helvetica, sans-serif; font-size:0.95rem; line-height:1.5; word-break:break-word; }' +
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
			'.dbx-llm-sent-field-label { font-weight:bold; font-size:0.85rem; color:#495057; }';
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
		if (opts)
			html += renderSentDetails(opts, ddlContext, serverData && serverData.promptSent, serverData && serverData.providerId);
		container.innerHTML = html;
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

	/** Render a block of SQL, pretty-printed + syntax highlighted when sql-formatter/Prism are available. */
	function renderSqlBlock(sql, dbVendor)
	{
		var formatted = sql;
		if (typeof window.sqlFormatter !== 'undefined' && typeof window.sqlFormatter.format === 'function')
		{
			try { formatted = window.sqlFormatter.format(sql, { language: sqlFormatterDialect(dbVendor), keywordCase: 'upper' }); }
			catch (e) { console.warn('dbxLlmAdvice: sqlFormatter.format() failed, showing SQL as-is: ' + e.message); }
		}

		if (typeof window.Prism !== 'undefined')
		{
			var id = 'dbx-llm-sql-' + Math.random().toString(36).slice(2);
			// Highlight after insertion (see renderResult) since Prism needs the element in the DOM.
			return { html: '<pre class="dbx-llm-pre"><code id="' + id + '" class="language-sql">' + escapeHtml(formatted) + '</code></pre>', highlightId: id };
		}

		return { html: '<pre class="dbx-llm-pre">' + escapeHtml(formatted) + '</pre>', highlightId: null };
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

	function renderSentField(label, value)
	{
		if (!value) return '';
		return '<div class="dbx-llm-sent-field"><div class="dbx-llm-sent-field-label">' + escapeHtml(label) + ':</div>'
			+ '<pre class="dbx-llm-pre">' + escapeHtml(value) + '</pre></div>';
	}

	/**
	 * The "Show what was sent to the LLM" collapsible - shared by both the success and error
	 * renderers, so this is visible regardless of whether the call actually succeeded.
	 * @param promptSent  the exact prompt text, if known (preferred - most precise)
	 * @param providerId  resolved provider id, if known (falls back to opts.provider / '(default)')
	 * @param model       specific model used, if known
	 */
	function renderSentDetails(opts, ddlContext, promptSent, providerId, model)
	{
		var providerLabel = escapeHtml(providerId || opts.provider || '(default)');
		if (model) providerLabel += ' / ' + escapeHtml(model);

		var html = '<details class="dbx-llm-sent-details">'
			+ '<summary>Show what was sent to the LLM (provider: ' + providerLabel + ')</summary>';
		if (promptSent)
			html += renderSentField('Full prompt sent', promptSent);
		else
		{
			html += renderSentField('SQL', opts.sql);
			html += renderSentField('DDL / index / stats context', ddlContext);
			html += renderSentField('Execution plan', opts.plan);
			html += renderSentField('DBMS vendor', opts.dbVendor);
		}
		html += '</details>';
		return html;
	}

	function renderResult(container, result, opts, ddlContext)
	{
		var html = '';

		if (result.optimizedSql)
		{
			var sqlBlock = renderSqlBlock(result.optimizedSql, opts.dbVendor);
			html += '<div class="dbx-llm-section-title">Suggested SQL:</div>' + sqlBlock.html;
		}
		if (result.explanation)
		{
			// Rendered as (a small subset of) Markdown - the prompt asks the model for
			// bullet/numbered lists, **bold** and `code` spans - not raw escaped text.
			html += '<div class="dbx-llm-section-title">Explanation:</div>';
			html += '<div class="dbx-llm-prose">' + renderMarkdown(result.explanation) + '</div>';
		}
		if (!result.optimizedSql && !result.explanation)
			html += '<div class="dbx-llm-status">The model did not return a usable answer.</div>';

		// "What was sent" - so the user can see exactly what context/prompt/model was used.
		html += renderSentDetails(opts, ddlContext, result.promptSent, result.providerId, result.model);

		container.innerHTML = html;

		if (result.optimizedSql && typeof window.Prism !== 'undefined')
		{
			var codeEl = container.querySelector('code.language-sql');
			if (codeEl) window.Prism.highlightElement(codeEl);
		}
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

	function fetchDdlContext(opts)
	{
		if (!opts.jdbcUrl)
			return Promise.resolve('');

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

	function callOptimize(container, opts, ddlContext)
	{
		renderStatus(container, 'Asking the LLM for optimization advice...');

		var body = {
			sql:        opts.sql,
			ddlContext: ddlContext,
			plan:       opts.plan,
			dbVendor:   opts.dbVendor,
			provider:   opts.provider
		};

		fetchJson('/api/llm/optimize-sql', {
			method:  'POST',
			headers: { 'Content-Type': 'application/json' },
			body:    JSON.stringify(body)
		})
		.then(function (data) { renderResult(container, data, opts, ddlContext); })
		.catch(function (err) { renderError(container, 'Failed to get LLM advice: ' + err.message, opts, ddlContext, err.responseData); });
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
		isEnabled: isEnabled
	};
})();
