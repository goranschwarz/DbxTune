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
 *       // DBMS version string (@@version, version(), ...) - tells the LLM what the server can do. Supply it
 *       // when known (the Daily Summary Report passes the RECORDED value); otherwise, when 'srv' is given,
 *       // it is resolved LIVE from that server's Collector (CmSummary) - see fetchDbmsVersion().
 *       dbmsVersion: 'Microsoft SQL Server 2019 ...', // optional
 *       sampleTime:  '2026-09-15 10:20:30',         // optional - which CmSummary sample to read the version from (default: latest)
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
 *       provider:   'claude',                      // optional - preselects this provider in the model dropdown
 *       model:      'opus-max',                    // optional - preselects this model entry (of 'provider')
 *       origin:     'Active Statements',           // optional - where the request was made; only recorded in the
 *                                                   // saved request log (LlmAdviceLog.java). Default: the page path.
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
 * open() does NOT call the LLM: it shows a model dropdown (every model configured in DBX_CENTRAL.conf, see
 * /api/llm/config 'models') and a "Get Advice" button. Each press asks the chosen model and adds its answer
 * on top of the earlier ones (so models can be compared). Only 'preview: true' builds its result at once.
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
			'  width:min(1400px, 94vw); max-height:88vh; overflow:auto; background:#fff; z-index:10001;' + // wide: the side by side SQL diff
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
			// that Prism.highlightElement() (see initCodeBlocks()) auto-copies its "language-sql"
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
			// Same look as the other section titles (bold, normal size/colour), only the fields inside stay smaller
			'.dbx-llm-sent-details > summary { cursor:pointer; font-weight:bold; font-size:1rem; }' +
			'.dbx-llm-sent-field { margin-top:8px; }' +
			'.dbx-llm-sent-field-label { font-weight:bold; font-size:0.85rem; color:#495057; }' +
			'.dbx-llm-sql-changed { color:#664d03; background:#fff3cd; border:1px solid #ffecb5; border-radius:4px;' +
			'  padding:6px 10px; margin-bottom:6px; font-family:Arial, Helvetica, sans-serif; font-size:0.85rem; }' +
			'.dbx-llm-sql-unchanged { color:#0f5132; background:#d1e7dd; border:1px solid #badbcc; border-radius:4px;' +
			'  padding:6px 10px; margin-bottom:6px; font-family:Arial, Helvetica, sans-serif; font-size:0.85rem; }' +
			'.dbx-llm-copy-btn { margin-left:8px; font-size:0.78rem; padding:1px 8px; cursor:pointer;' +
			'  border:1px solid #ced4da; border-radius:3px; background:#fff; color:#495057; }' +
			'.dbx-llm-copy-btn:hover { background:#e9ecef; }' +
			'.dbx-llm-pre-compact { font-size:0.78rem !important; }' +
			// Code blocks and the diff: small and scrollable by default, "Expand" shows all of it (renderCodeBlock())
			'.dbx-llm-capped { max-height:300px; overflow:auto; }' +
			'.dbx-llm-capped.dbx-llm-expanded { max-height:none; }' +
			'.dbx-llm-size-bar { text-align:right; }' +
			'.dbx-llm-size-btn { font-size:0.75rem; padding:0 10px; margin-top:2px; cursor:pointer; border:1px solid #ced4da; border-radius:3px; background:#fff; color:#495057; }' +
			'.dbx-llm-size-btn:hover { background:#e9ecef; }' +
			// Small grey text after a section title: the diff counts, "By the LLM"
			'.dbx-llm-title-note { font-weight:normal; font-size:0.85rem; color:#6c757d; }' +
			'.dbx-llm-diff-modes { text-align:right; margin-bottom:3px; }' +
			'.dbx-llm-diff-modes button { font-size:0.75rem; padding:0 10px; cursor:pointer; border:1px solid #ced4da; background:#fff; color:#495057; }' +
			'.dbx-llm-diff-modes button:first-child { border-radius:3px 0 0 3px; }' +
			'.dbx-llm-diff-modes button:last-child { border-radius:0 3px 3px 0; border-left:none; }' +
			'.dbx-llm-diff-modes button.dbx-llm-active { background:#0d6efd; border-color:#0d6efd; color:#fff; }' +
			'html.dbx-sp-dark .dbx-llm-diff-modes button:not(.dbx-llm-active), html[data-theme="dark"] .dbx-llm-diff-modes button:not(.dbx-llm-active) { border-color:#555c63; background:#2b3035; color:#dee2e6; }' +
			// The SQL diff (renderSqlDiff(), drawn by diff2html): no file header bar, our fonts/colours, the dark theme via its own variables
			// position: diff2html's line numbers are position:absolute - without it they are not clipped by the 300px scroll box
			'.dbx-llm-diff { border:1px solid #dee2e6; border-radius:4px; position:relative; }' +
			'.dbx-llm-diff .d2h-file-header { display:none; }' +
			'.dbx-llm-diff .d2h-file-wrapper { border:none; border-radius:0; margin-bottom:0; }' +
			'.dbx-llm-diff .d2h-diff-table { font-family:"Courier New", monospace; font-size:0.82rem; line-height:1.45; }' +
			// Global table rules (dark: striped even rows) must not show through
			'.dbx-llm-diff tr { background-color:transparent !important; }' +
			// Unified: a changed line pair in red/green like GitHub, not diff2html's yellow 'changed' colours
			'.dbx-llm-diff { --d2h-change-del-color:var(--d2h-del-bg-color); --d2h-change-ins-color:var(--d2h-ins-bg-color); }' +
			// Prism's theme has light grey punctuation and a half-white background on operators: unreadable on red/green rows
			'.dbx-llm-diff .token { background:none !important; }' +
			'.dbx-llm-diff .token.punctuation, .dbx-llm-diff .token.operator { color:#24292f; }' +
			'.dbx-llm-diff .token.keyword { color:#0550ae; }' +
			'.dbx-llm-diff .token.function { color:#8250df; }' +
			'.dbx-llm-diff .token.string { color:#0a3069; }' +
			'.dbx-llm-diff .token.number, .dbx-llm-diff .token.boolean { color:#953800; }' +
			'html.dbx-sp-dark .dbx-llm-diff .token.keyword, html[data-theme="dark"] .dbx-llm-diff .token.keyword { color:#79c0ff; }' +
			'html.dbx-sp-dark .dbx-llm-diff .token.function, html[data-theme="dark"] .dbx-llm-diff .token.function { color:#d2a8ff; }' +
			'html.dbx-sp-dark .dbx-llm-diff .token.string, html[data-theme="dark"] .dbx-llm-diff .token.string { color:#a5d6ff; }' +
			'html.dbx-sp-dark .dbx-llm-diff .token.number, html[data-theme="dark"] .dbx-llm-diff .token.number, html.dbx-sp-dark .dbx-llm-diff .token.boolean, html[data-theme="dark"] .dbx-llm-diff .token.boolean { color:#ffa657; }' +
			'html.dbx-sp-dark .dbx-llm-diff .token.punctuation, html[data-theme="dark"] .dbx-llm-diff .token.punctuation, html.dbx-sp-dark .dbx-llm-diff .token.operator, html[data-theme="dark"] .dbx-llm-diff .token.operator { color:#e6edf3; }' +
			// Dark: diff2html's light scheme drawn with its dark colours, so it follows the page's theme switch without a re-render
			'html.dbx-sp-dark .dbx-llm-diff, html[data-theme="dark"] .dbx-llm-diff { border-color:#30363d; color:#e6edf3; background:#0d1117;' +
			'  --d2h-bg-color:var(--d2h-dark-bg-color); --d2h-border-color:var(--d2h-dark-border-color); --d2h-dim-color:var(--d2h-dark-dim-color);' +
			'  --d2h-line-border-color:var(--d2h-dark-line-border-color); --d2h-file-header-bg-color:var(--d2h-dark-file-header-bg-color);' +
			'  --d2h-file-header-border-color:var(--d2h-dark-file-header-border-color); --d2h-empty-placeholder-bg-color:var(--d2h-dark-empty-placeholder-bg-color);' +
			'  --d2h-empty-placeholder-border-color:var(--d2h-dark-empty-placeholder-border-color); --d2h-selected-color:var(--d2h-dark-selected-color);' +
			'  --d2h-ins-bg-color:var(--d2h-dark-ins-bg-color); --d2h-ins-border-color:var(--d2h-dark-ins-border-color); --d2h-ins-highlight-bg-color:var(--d2h-dark-ins-highlight-bg-color);' +
			'  --d2h-ins-label-color:var(--d2h-dark-ins-label-color); --d2h-del-bg-color:var(--d2h-dark-del-bg-color); --d2h-del-border-color:var(--d2h-dark-del-border-color);' +
			'  --d2h-del-highlight-bg-color:var(--d2h-dark-del-highlight-bg-color); --d2h-del-label-color:var(--d2h-dark-del-label-color);' +
			'  --d2h-change-del-color:var(--d2h-dark-del-bg-color); --d2h-change-ins-color:var(--d2h-dark-ins-bg-color);' +
			'  --d2h-info-bg-color:var(--d2h-dark-info-bg-color); --d2h-info-border-color:var(--d2h-dark-info-border-color);' +
			'  --d2h-change-label-color:var(--d2h-dark-change-label-color); --d2h-moved-label-color:var(--d2h-dark-moved-label-color); }' +
			'html.dbx-sp-dark .dbx-llm-size-btn, html[data-theme="dark"] .dbx-llm-size-btn { border-color:#555c63; background:#2b3035; color:#dee2e6; }' +
			'html.dbx-sp-dark .dbx-llm-size-btn:hover, html[data-theme="dark"] .dbx-llm-size-btn:hover { background:#343a40; }' +
			'html.dbx-sp-dark .dbx-llm-title-note, html[data-theme="dark"] .dbx-llm-title-note { color:#adb5bd; }' +
			'.dbx-llm-version { font-family:Arial, Helvetica, sans-serif; font-size:0.85rem; color:#495057; margin-top:8px; word-break:break-word; }' +
			'.dbx-llm-saved-note { font-family:Arial, Helvetica, sans-serif; font-size:0.85rem; line-height:1.6; color:#084298; background:#e7f1ff; border:1px solid #b6d4fe; border-radius:4px; padding:6px 10px; margin:0 0 10px 0; }' +
			'.dbx-llm-saved-note a { color:#0a58ca; font-weight:bold; }' +
			'html.dbx-sp-dark .dbx-llm-saved-note, html[data-theme="dark"] .dbx-llm-saved-note { color:#9ec5fe; background:#031633; border-color:#084298; }' +
			'html.dbx-sp-dark .dbx-llm-saved-note a, html[data-theme="dark"] .dbx-llm-saved-note a { color:#6ea8fe; }' +
			'.dbx-llm-login { font-family:Arial, Helvetica, sans-serif; margin:8px 0; }' +
			'.dbx-llm-login-btn { background:#0d6efd; color:#fff; border:1px solid #0d6efd; border-radius:4px; padding:3px 12px; cursor:pointer; margin-right:6px; }' +
			// "Continue to login / Skip" popup (askForLogin) - above an open Bootstrap dialog and our own floating dialog (10000/10001)
			'.dbx-llm-asklogin-backdrop { position:fixed; inset:0; background:rgba(0,0,0,0.35); z-index:10040; display:flex; align-items:center; justify-content:center; }' +
			'.dbx-llm-asklogin-box { background:#fff; color:#212529; border-radius:6px; box-shadow:0 4px 24px rgba(0,0,0,0.35); padding:16px 20px; max-width:420px; font-family:Arial, Helvetica, sans-serif; }' +
			'.dbx-llm-asklogin-title { font-weight:bold; font-size:1.05rem; margin-bottom:8px; }' +
			'.dbx-llm-asklogin-buttons { margin-top:14px; display:flex; gap:8px; justify-content:flex-end; flex-wrap:wrap; }' +
			'.dbx-llm-asklogin-skip { background:#fff; color:#495057; border:1px solid #adb5bd; border-radius:4px; padding:3px 12px; cursor:pointer; }' +
			'html.dbx-sp-dark .dbx-llm-asklogin-box, html[data-theme="dark"] .dbx-llm-asklogin-box { background:#2b3035; color:#e9ecef; }' +
			'html.dbx-sp-dark .dbx-llm-asklogin-skip, html[data-theme="dark"] .dbx-llm-asklogin-skip { background:#343a40; color:#dee2e6; border-color:#6c757d; }' +
			// Dark theme: Showplan dialogs (DbxShowplanTheme: <html class="dbx-sp-dark">) or dark DbxCentral pages (dbxTheme.js)
			'html.dbx-sp-dark .dbx-llm-dialog, html[data-theme="dark"] .dbx-llm-dialog { background:#22272e; color:#e9ecef; box-shadow:0 4px 24px rgba(0,0,0,0.7); }' +
			'html.dbx-sp-dark .dbx-llm-header, html[data-theme="dark"] .dbx-llm-header { background:#343a40; }' +
			'html.dbx-sp-dark .dbx-llm-status, html[data-theme="dark"] .dbx-llm-status, html.dbx-sp-dark .dbx-llm-sent-field-label, html[data-theme="dark"] .dbx-llm-sent-field-label, html.dbx-sp-dark .dbx-llm-version, html[data-theme="dark"] .dbx-llm-version { color:#adb5bd; }' +
			'html.dbx-sp-dark .dbx-llm-spinner, html[data-theme="dark"] .dbx-llm-spinner { border-color:#555c63; border-top-color:#dee2e6; }' +
			'html.dbx-sp-dark .dbx-llm-pre, html[data-theme="dark"] .dbx-llm-pre { background:#1a1d21; color:#e9ecef; border-left-color:#3d8bfd; }' +
			'html.dbx-sp-dark .dbx-llm-prose, html[data-theme="dark"] .dbx-llm-prose { background:#1c2025; color:#e9ecef; border-left-color:#6c757d; }' +
			'html.dbx-sp-dark .dbx-llm-prose code, html[data-theme="dark"] .dbx-llm-prose code { background:#343a40; color:#e9ecef; }' +
			'html.dbx-sp-dark .dbx-llm-error, html[data-theme="dark"] .dbx-llm-error { color:#ea868f; background:#2c0b0e; border-color:#842029; }' +
			'html.dbx-sp-dark .dbx-llm-sql-changed, html[data-theme="dark"] .dbx-llm-sql-changed { color:#ffda6a; background:#332701; border-color:#997404; }' +
			'html.dbx-sp-dark .dbx-llm-sql-unchanged, html[data-theme="dark"] .dbx-llm-sql-unchanged { color:#75b798; background:#051b11; border-color:#0f5132; }' +
			'html.dbx-sp-dark .dbx-llm-copy-btn, html[data-theme="dark"] .dbx-llm-copy-btn { border-color:#555c63; background:#2b3035; color:#dee2e6; }' +
			'html.dbx-sp-dark .dbx-llm-copy-btn:hover, html[data-theme="dark"] .dbx-llm-copy-btn:hover { background:#343a40; }' +
			// The model dropdown + "Get Advice" button, and one block per answer (newest on top). Own classes, no
			// Bootstrap: this also runs on pages without it. max-width: see .dbx-llm-prose (the 3000px showplan wrapper).
			'.dbx-llm-picker { display:flex; flex-wrap:wrap; align-items:center; gap:8px; margin-bottom:12px; font-family:Arial, Helvetica, sans-serif; }' +
			'.dbx-llm-picker-label { font-weight:bold; }' +
			'.dbx-llm-model-select { font-size:0.9rem; padding:3px 6px; border:1px solid #ced4da; border-radius:4px; background:#fff; color:#212529; max-width:100%; }' +
			'.dbx-llm-ask-btn { background:#0d6efd; color:#fff; border:1px solid #0d6efd; border-radius:4px; padding:3px 12px; cursor:pointer; }' +
			'.dbx-llm-ask-btn:disabled { opacity:0.6; cursor:default; }' +
			'.dbx-llm-picker-hint { font-size:0.8rem; color:#6c757d; }' +
			'.dbx-llm-run { border:1px solid #dee2e6; border-radius:6px; margin-bottom:12px; max-width:min(1600px, 96vw); box-sizing:border-box; }' +
			'.dbx-llm-run-header { background:#f1f3f5; color:#212529; padding:6px 10px; font-family:Arial, Helvetica, sans-serif; font-weight:bold; border-bottom:1px solid #dee2e6; border-radius:6px 6px 0 0; cursor:pointer; }' +
			'.dbx-llm-run:not([open]) > .dbx-llm-run-header { border-bottom:none; border-radius:6px; }' +
			'.dbx-llm-run-info { font-weight:normal; color:#6c757d; }' +
			'.dbx-llm-run-body { padding:10px; }' +
			'html.dbx-sp-dark .dbx-llm-model-select, html[data-theme="dark"] .dbx-llm-model-select { background:#2b3035; color:#e9ecef; border-color:#495057; }' +
			'html.dbx-sp-dark .dbx-llm-picker-hint, html[data-theme="dark"] .dbx-llm-picker-hint, html.dbx-sp-dark .dbx-llm-run-info, html[data-theme="dark"] .dbx-llm-run-info { color:#adb5bd; }' +
			'html.dbx-sp-dark .dbx-llm-run, html[data-theme="dark"] .dbx-llm-run { border-color:#495057; }' +
			'html.dbx-sp-dark .dbx-llm-run-header, html[data-theme="dark"] .dbx-llm-run-header { background:#2b3035; color:#e9ecef; border-bottom-color:#495057; }';
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
		// The link to the saved request first (the user wants it on top), then the error
		var html = renderSavedNote(serverData && serverData.savedUrl, serverData && serverData.savedForDays);
		html += '<div class="dbx-llm-error">' + escapeHtml(msg) + '</div>';
		html += opts ? renderInputContext(opts) : '';
		if (opts)
			html += renderSentDetails(opts, ddlContext, serverData && serverData.promptSent, serverData && serverData.providerId, serverData && serverData.model);
		container.innerHTML = html;
		initCodeBlocks(container);
	}

	/** Guess a reasonable sql-formatter "language" dialect from a DBMS product name. */
	function sqlFormatterDialect(dbVendor)
	{
		var v = (dbVendor || '').toLowerCase();
		if (v.indexOf('sql server') >= 0) return 'tsql';
		if (v.indexOf('sybase') >= 0 || v.indexOf('adaptive server') >= 0) return 'tsql'; // closest available dialect to Sybase T-SQL
		if (v.indexOf('postgres') >= 0) return 'postgresql';
		if (v.indexOf('oracle') >= 0) return 'plsql';
		return 'sql';
	}

	/** Pretty-print SQL when sql-formatter is on the page (as-is otherwise). Used for the SQL blocks AND the diff, so both look the same. */
	function formatSql(sql, dbVendor)
	{
		if (typeof window.sqlFormatter !== 'undefined' && typeof window.sqlFormatter.format === 'function')
		{
			try {
				// Same user SQL Format options as the Format SQL buttons, when dbxSqlFormat.js is on the page
				return window.dbxSqlFormat
					? window.dbxSqlFormat.format(sql, sqlFormatterDialect(dbVendor))
					: window.sqlFormatter.format(sql, { language: sqlFormatterDialect(dbVendor), keywordCase: 'upper', functionCase: 'upper', dataTypeCase: 'upper' });
			}
			catch (e) { console.warn('dbxLlmAdvice: sqlFormatter.format() failed, showing SQL as-is: ' + e.message); }
		}
		return sql;
	}

	/**
	 * Render a block of SQL, pretty-printed + syntax highlighted (with line numbers) when sql-formatter/Prism are available.
	 * @param smallFont  smaller text - for reference/input material (the original SQL Text, the Execution Plan)
	 *                   as opposed to the LLM's actual answer (Suggested SQL)
	 */
	function renderSqlBlock(sql, dbVendor, smallFont)
	{
		return renderCodeBlock(escapeHtml(formatSql(sql, dbVendor)), 'sql', smallFont);
	}

	/**
	 * A code block: line numbers (Prism's Line Numbers plugin, when Prism is on the page), small and scrollable by
	 * default, with an "Expand" button to show all of it (and "Collapse" back) - the button only shows when the
	 * content does not fit, see refreshCodeBlocks(). Highlighted after insertion (initCodeBlocks()): Prism needs the DOM.
	 * @param codeHtml   already HTML-escaped text
	 * @param language   Prism language: 'sql', or 'plaintext' (plan text, workload profile)
	 */
	function renderCodeBlock(codeHtml, language, smallFont)
	{
		var preClass = 'dbx-llm-pre dbx-llm-capped' + (smallFont ? ' dbx-llm-pre-compact' : '') + (typeof window.Prism !== 'undefined' ? ' line-numbers' : '');
		return '<div class="dbx-llm-code">'
			+ '<pre class="' + preClass + '"><code class="language-' + language + '">' + codeHtml + '</code></pre>'
			+ '<div class="dbx-llm-size-bar"><button type="button" class="dbx-llm-size-btn" style="display:none">Expand</button></div>'
			+ '</div>';
	}

	/**
	 * What an execution plan is, so it gets the right highlighting: XML (SQL Server showplan), JSON (MySQL EXPLAIN
	 * FORMAT=JSON, Postgres) or text (ASE, Oracle, Db2, ...). XML and JSON are indented when they come as one long line.
	 * Very large plans stay plain text: highlighting megabytes of XML would freeze the page.
	 * @return {language, label, text}
	 */
	function describePlan(plan)
	{
		var t = String(plan).trim();
		if (t.length > 2 * 1024 * 1024)
			return { language: 'plaintext', label: 'text (too large to highlight)', text: plan };

		if (t.charAt(0) === '<')
			return { language: 'xml', label: 'XML', text: (t.split('\n').length < 5) ? formatXml(t) : plan };

		if (t.charAt(0) === '{' || t.charAt(0) === '[')
		{
			try { return { language: 'json', label: 'JSON', text: JSON.stringify(JSON.parse(t), null, 2) }; }
			catch (e) { /* not JSON after all: text */ }
		}
		return { language: 'plaintext', label: 'text', text: plan };
	}

	/** Indent XML that comes as one line: one tag per line, 2 spaces per level (text-only elements stay on one line). */
	function formatXml(xml)
	{
		var out = [], level = 0;
		xml.replace(/>\s*</g, '>\n<').split('\n').forEach(function (line)
		{
			line = line.trim();
			if (!line) return;
			if (/^<\//.test(line))
				level = Math.max(level - 1, 0);
			out.push(new Array(level + 1).join('  ') + line);
			// An opening tag that is not closed on the same line (not '</x>', '<x/>', '<?..?>', '<!..>', '<x>text</x>')
			if (/^<[^\/?!]/.test(line) && !/\/>$/.test(line) && line.indexOf('</') < 0)
				level++;
		});
		return out.join('\n');
	}

	/**
	 * Prism languages that are not in our Prism bundle (prism-1.30.0.js), loaded from its components when a block needs one.
	 */
	var PRISM_COMPONENTS_URL = '/scripts/prism/1.30.0/components/';
	var _prismComponentPromises = {};
	function loadPrismLanguage(language)
	{
		if (!window.Prism || window.Prism.languages[language])
			return Promise.resolve();
		if (!_prismComponentPromises[language])
		{
			_prismComponentPromises[language] = new Promise(function (resolve, reject)
			{
				var script = document.createElement('script');
				script.src = PRISM_COMPONENTS_URL + 'prism-' + language + '.min.js';
				script.onload  = function () { resolve(); };
				script.onerror = function () { delete _prismComponentPromises[language]; reject(new Error('could not load ' + script.src)); };
				document.head.appendChild(script);
			});
		}
		return _prismComponentPromises[language];
	}

	/**
	 * After the blocks are in the DOM: highlight them (line numbers come with it), then show the "Expand" button where
	 * the content does not fit. A block in a closed <details> has no size yet: done again when it is opened.
	 */
	function initCodeBlocks(container)
	{
		if (typeof window.Prism !== 'undefined')
		{
			var codeEls = container.querySelectorAll('.dbx-llm-code code[class*="language-"]');
			for (var i = 0; i < codeEls.length; i++)
			{
				var codeEl   = codeEls[i];
				var language = (codeEl.className.match(/language-(\S+)/) || [])[1];
				if (language && language !== 'plaintext' && !window.Prism.languages[language])
				{
					// Not in our bundle (JSON): load it, then highlight (plain text with line numbers until then, or if it fails)
					window.Prism.highlightElement(codeEl);
					loadPrismLanguage(language).then(
						(function (el) { return function () { window.Prism.highlightElement(el); refreshCodeBlocks(container); }; })(codeEl),
						function (err) { console.warn('dbxLlmAdvice: ' + err.message); });
					continue;
				}
				window.Prism.highlightElement(codeEl);
			}
		}

		if (!container.getAttribute('data-dbx-llm-toggle'))
		{
			container.setAttribute('data-dbx-llm-toggle', 'true');
			// 'toggle' does not bubble, but it can be caught on the way down (capture)
			container.addEventListener('toggle', function () { refreshCodeBlocks(container); }, true);
		}

		if (!_sizeBtnHandlerInstalled)
		{
			_sizeBtnHandlerInstalled = true;
			document.addEventListener('click', function (e)
			{
				var btn = e.target.closest && e.target.closest('.dbx-llm-size-btn');
				if (!btn) return;
				var box = btn.closest('.dbx-llm-code').querySelector('.dbx-llm-capped');
				var expanded = box.classList.toggle('dbx-llm-expanded');
				btn.textContent = expanded ? 'Collapse' : 'Expand';
			});
		}

		refreshCodeBlocks(container);
	}
	var _sizeBtnHandlerInstalled = false;

	/** For the visible blocks: re-measure the line numbers (they depend on line wrapping) and show "Expand" only when needed. */
	function refreshCodeBlocks(container)
	{
		var blocks = container.querySelectorAll('.dbx-llm-code');
		for (var i = 0; i < blocks.length; i++)
		{
			var box = blocks[i].querySelector('.dbx-llm-capped');
			var btn = blocks[i].querySelector('.dbx-llm-size-btn');
			if (!box || !btn || box.offsetParent === null)
				continue; // not visible (a closed <details>): measured when it is opened

			if (box.tagName === 'PRE' && box.classList.contains('line-numbers') && window.Prism && window.Prism.plugins && window.Prism.plugins.lineNumbers)
				window.Prism.plugins.lineNumbers.resize(box);

			var expanded = box.classList.contains('dbx-llm-expanded');
			btn.style.display = (expanded || box.scrollHeight > box.clientHeight + 2) ? '' : 'none';
		}
	}

	//--------------------------------------------------------------------------
	// SQL diff: the original SQL vs the LLM's rewrite (jsdiff makes the patch, diff2html draws it; loaded when needed)
	//--------------------------------------------------------------------------
	var JSDIFF_URL        = '/scripts/jsdiff/9.0.0/diff.min.js';
	var DIFF2HTML_JS_URL  = '/scripts/diff2html/3.4.56/diff2html.min.js';
	var DIFF2HTML_CSS_URL = '/scripts/diff2html/3.4.56/diff2html.min.css';
	var _diffLibsPromise = null;

	/**
	 * jsdiff (global 'Diff') and diff2html (global 'Diff2Html') are only needed when an answer has a SQL rewrite:
	 * load them then, once - instead of adding them to every page that includes this file.
	 */
	function loadDiffLibs()
	{
		function loadScript(url, isLoaded)
		{
			if (isLoaded())
				return Promise.resolve();
			return new Promise(function (resolve, reject)
			{
				var script = document.createElement('script');
				script.src = url;
				script.onload  = function () { if (isLoaded()) resolve(); else reject(new Error('no library in ' + url)); };
				script.onerror = function () { reject(new Error('could not load ' + url)); };
				document.head.appendChild(script);
			});
		}

		if (!_diffLibsPromise)
		{
			if (!document.querySelector('link[href="' + DIFF2HTML_CSS_URL + '"]'))
			{
				var link = document.createElement('link');
				link.rel  = 'stylesheet';
				link.href = DIFF2HTML_CSS_URL;
				document.head.appendChild(link);
			}
			_diffLibsPromise = Promise.all([
				loadScript(JSDIFF_URL,       function () { return window.Diff      && typeof window.Diff.createTwoFilesPatch === 'function'; }),
				loadScript(DIFF2HTML_JS_URL, function () { return window.Diff2Html && typeof window.Diff2Html.html === 'function'; })
			]).catch(function (err) { _diffLibsPromise = null; throw err; });
		}
		return _diffLibsPromise;
	}

	/** SQL text as highlighted HTML (Prism, when on the page), for one line or a part of a line of the diff. */
	function highlightSqlFragment(text)
	{
		if (window.Prism && window.Prism.languages && window.Prism.languages.sql)
			return window.Prism.highlight(text, window.Prism.languages.sql, 'sql');
		return escapeHtml(text);
	}

	var DIFF_MODE_STORAGE_KEY = 'dbxLlmAdvice.diffMode';

	/**
	 * Render the diff of the original SQL vs the suggested SQL into 'host', like GitHub: side by side (the default) or
	 * unified, switched with two buttons (the choice is remembered in the browser). diff2html pairs a changed line with
	 * the most similar line on the other side and marks the changed words in it. Both are formatted the same way
	 * first, and whitespace is ignored, so only real changes show. The counts go into 'statEl'.
	 */
	function renderSqlDiff(host, statEl, oldSql, newSql, dbVendor)
	{
		loadDiffLibs().then(function ()
		{
			var oldText = formatSql(oldSql, dbVendor).replace(/\s+$/, '') + '\n';
			var newText = formatSql(newSql, dbVendor).replace(/\s+$/, '') + '\n';
			// The whole SQL as context (not GitHub's 3 lines around a change): it is short, and read as a whole.
			// ignoreWhitespace: the SQL formatter re-aligns operators ('o.status    <>') when the lines around them change.
			var patch = window.Diff.createTwoFilesPatch('SQL', 'SQL', oldText, newText, '', '', { context: 100000, ignoreWhitespace: true });

			var mode = 'split';
			try { mode = window.localStorage.getItem(DIFF_MODE_STORAGE_KEY) || 'split'; } catch (e) { /* no memory */ }

			host.innerHTML = '<div class="dbx-llm-diff-modes">'
				+ '<button type="button" data-mode="split">Side by side</button><button type="button" data-mode="unified">Unified</button></div>'
				+ '<div class="dbx-llm-code">'
				+ '<div class="dbx-llm-diff dbx-llm-capped"></div>'
				+ '<div class="dbx-llm-size-bar"><button type="button" class="dbx-llm-size-btn" style="display:none">Expand</button></div>'
				+ '</div>';

			function show(newMode)
			{
				mode = (newMode === 'unified') ? 'unified' : 'split';
				var box = host.querySelector('.dbx-llm-diff');
				// Always the light scheme: the dark theme is applied by our CSS (injectStyle()), so it follows the page's theme switch
				box.innerHTML = window.Diff2Html.html(patch, {
					drawFileList: false, outputFormat: (mode === 'unified') ? 'line-by-line' : 'side-by-side',
					matching: 'lines', diffStyle: 'word', colorScheme: 'light' });
				// SQL colours: highlight each text piece of a line, keeping diff2html's <ins>/<del> word marks
				box.querySelectorAll('.d2h-code-line-ctn').forEach(function (ctn)
				{
					Array.prototype.slice.call(ctn.childNodes).forEach(function (n)
					{
						if (n.nodeType === 1)
							n.innerHTML = highlightSqlFragment(n.textContent);
						else if (n.nodeType === 3)
						{
							var span = document.createElement('span');
							span.innerHTML = highlightSqlFragment(n.textContent);
							ctn.replaceChild(span, n);
						}
					});
				});
				// Side by side: scrolling one side sideways scrolls the other one too (both halves stay on the same columns)
				var sides = box.querySelectorAll('.d2h-file-side-diff');
				if (sides.length === 2)
				{
					sides[0].addEventListener('scroll', function () { if (sides[1].scrollLeft !== sides[0].scrollLeft) sides[1].scrollLeft = sides[0].scrollLeft; });
					sides[1].addEventListener('scroll', function () { if (sides[0].scrollLeft !== sides[1].scrollLeft) sides[0].scrollLeft = sides[1].scrollLeft; });
				}
				host.querySelectorAll('.dbx-llm-diff-modes button').forEach(function (btn) { btn.classList.toggle('dbx-llm-active', btn.getAttribute('data-mode') === mode); });
				refreshCodeBlocks(host);
			}
			host.querySelector('.dbx-llm-diff-modes').addEventListener('click', function (e)
			{
				var btn = e.target.closest('button[data-mode]');
				if (!btn) return;
				try { window.localStorage.setItem(DIFF_MODE_STORAGE_KEY, btn.getAttribute('data-mode')); } catch (ex) { /* no memory */ }
				show(btn.getAttribute('data-mode'));
			});
			show(mode);

			if (statEl)
			{
				var file = window.Diff2Html.parse(patch)[0] || { deletedLines: 0, addedLines: 0 };
				var removed = file.deletedLines, added = file.addedLines;
				statEl.textContent = ' - ' + removed + ' line' + (removed === 1 ? '' : 's') + ' removed, ' + added + ' added';
			}
		})
		.catch(function (err)
		{
			host.innerHTML = '<div class="dbx-llm-status">The diff can not be shown: ' + escapeHtml(err.message) + '</div>';
		});
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
	//
	// The temporary <textarea> goes inside the enclosing Bootstrap modal (when there is one), not on
	// <body>: the modal's focus trap pulls focus back the moment anything outside it is focused, which
	// drops the selection - execCommand('copy') then still returns true but copies an EMPTY string.
	function copyTextToClipboard(text, btnEl)
	{
		var host = (btnEl && btnEl.closest && btnEl.closest('.modal')) || document.body;
		var textArea = document.createElement('textarea');
		textArea.value = text;
		textArea.setAttribute('readonly', '');
		textArea.style.position = 'fixed'; textArea.style.top = '0'; textArea.style.left = '0'; textArea.style.opacity = '0';
		host.appendChild(textArea);
		textArea.focus();
		textArea.select();
		var ok = false;
		try { ok = document.execCommand('copy'); } catch (err) {}
		host.removeChild(textArea);
		if (btnEl && btnEl.focus) btnEl.focus();

		if (!ok)
		{
			alert('Unable to copy to the clipboard - select the text and press Ctrl+C instead.');
			return;
		}
		if (btnEl)
		{
			var orig = btnEl.textContent;
			btnEl.textContent = 'Copied!';
			setTimeout(function () { btnEl.textContent = orig; }, 1500);
		}
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

	/** A "Copy" button (or another label) wired to _copyRegistry; '' if there's nothing to copy. */
	function makeCopyButtonHtml(text, label)
	{
		if (!text) return '';

		ensureCopyButtonHandler();
		var copyId = 'dbx-llm-copy-' + Math.random().toString(36).slice(2);
		_copyRegistry[copyId] = text;
		return ' <button type="button" class="dbx-llm-copy-btn" data-copy-id="' + copyId + '">' + escapeHtml(label || 'Copy') + '</button>';
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
		if (opts.dbmsVersion)     parts.push('DBMS version:\n' + opts.dbmsVersion);
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
		// NOTE: _copyRegistry is reset in open()/renderSavedRequest() (a new view), not here: the answers of
		//       several models are kept below each other, and their "Copy" buttons must all keep working.
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
			html += renderSentField('DBMS version', opts.dbmsVersion);
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
	 * to open "Show what was sent to the LLM" and pick it out of the full prompt text. Smaller text
	 * than the "Suggested SQL" block below it (renderSqlBlock's smallFont / .dbx-llm-pre-compact)
	 * since this is reference material, not the answer. Each is its own <details>, CLOSED by default
	 * (only the Explanation is open), so the answer is what the reader sees first. Every block starts
	 * small and scrollable, with an "Expand" button when it does not fit (renderCodeBlock()).
	 */
	function renderInputContext(opts)
	{
		var html = '';

		// One line, not a collapsible - it is short, and it tells the reader which server the advice targets
		if (opts.dbmsVersion)
		{
			html += '<div class="dbx-llm-version"><b>DBMS Version:</b> ' + escapeHtml(opts.dbmsVersion) + '</div>';
		}

		if (opts.sql)
		{
			html += '<details class="dbx-llm-section-details">'
				+ '<summary class="dbx-llm-section-title">SQL Text</summary>' + renderSqlBlock(opts.sql, opts.dbVendor, /*smallFont*/ true) + '</details>';
		}
		if (opts.plan)
		{
			var plan = describePlan(opts.plan);
			html += '<details class="dbx-llm-section-details">'
				+ '<summary class="dbx-llm-section-title">Execution Plan<span class="dbx-llm-title-note"> - ' + plan.label + '</span></summary>'
				+ renderCodeBlock(escapeHtml(plan.text), plan.language, /*smallFont*/ true) + '</details>';
		}
		if (opts.workloadProfile)
		{
			// Plain text (see buildWorkloadProfile), so NOT renderSqlBlock()
			html += '<details class="dbx-llm-section-details">'
				+ '<summary class="dbx-llm-section-title">Workload Profile</summary>'
				+ renderCodeBlock(escapeHtml(opts.workloadProfile), 'plaintext', /*smallFont*/ true) + '</details>';
		}

		return html;
	}

	/**
	 * When the request was saved (DbxCentral.llm.log.save, see LlmAdviceLog.java): a link to the saved page, which any
	 * DbxCentral user can open (/llm-log) - so it can be shared - a "Copy link" button, and how long it is kept.
	 * @param savedUrl      the server's link (a path), or empty when the request was not saved
	 * @param savedForDays  days the saved page is kept (0 or less = until an administrator removes it)
	 */
	function renderSavedNote(savedUrl, savedForDays)
	{
		if (!savedUrl) return '';
		var days = parseInt(savedForDays, 10);
		var kept = days > 0 ? 'Kept for ' + days + ' days' : 'Kept until an administrator removes it';
		return '<div class="dbx-llm-saved-note">'
			+ '&#128279; This request is saved: <a href="' + escapeHtml(savedUrl) + '" target="_blank" rel="noopener">Open saved request</a>'
			+ makeCopyButtonHtml(window.location.origin + savedUrl, 'Copy link')
			+ '<br>' + kept + '. Anyone with a DbxCentral login can open the link, so you can share it.'
			+ '</div>';
	}

	function renderResult(container, result, opts, ddlContext)
	{
		// The link to the saved request first (the user wants it on top), then the answer
		var html = renderSavedNote(result.savedUrl, result.savedForDays);
		html += renderInputContext(opts);

		if (result.optimizedSql)
		{
			// Compare against the model's own echo of the SQL it analyzed (origin_sql) when
			// available - more accurate than opts.sql if the model reformatted it in its echo -
			// falling back to opts.sql (what we actually sent) otherwise. Kept as a secondary check:
			// the prompt already asks the model to leave optimized_sql empty when unchanged, this
			// just catches the case where it returns identical SQL anyway.
			var baselineSql = result.originSql || opts.sql;
			var sqlChanged  = normalizeSqlForCompare(result.optimizedSql) !== normalizeSqlForCompare(baselineSql);
			html += '<details class="dbx-llm-section-details"><summary class="dbx-llm-section-title">Suggested SQL<span class="dbx-llm-title-note"> - By the LLM</span></summary>';
			html += sqlChanged
				? '<div class="dbx-llm-sql-changed">SQL was changed by the LLM.</div>'
				: '<div class="dbx-llm-sql-unchanged">SQL was <b>not</b> changed - the LLM returned the same statement.</div>';
			html += renderSqlBlock(result.optimizedSql, opts.dbVendor) + '</details>';

			// What the LLM changed, line by line: the SQL we sent vs its rewrite (filled in by renderSqlDiff() below)
			if (sqlChanged)
			{
				html += '<details class="dbx-llm-section-details dbx-llm-diff-section" open><summary class="dbx-llm-section-title">SQL Changes (diff)'
					+ '<span class="dbx-llm-title-note dbx-llm-diff-stat"></span></summary>'
					+ '<div class="dbx-llm-diff-host"><div class="dbx-llm-status">Building the diff...</div></div></details>';
			}
		}
		else if (result.explanation)
		{
			// The model is asked to leave 'optimized_sql' empty (not echo the original back) when
			// it has no rewrite to suggest - a real, expected outcome, not a missing answer.
			html += '<details class="dbx-llm-section-details"><summary class="dbx-llm-section-title">Suggested SQL<span class="dbx-llm-title-note"> - By the LLM</span></summary>';
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
		initCodeBlocks(container);

		var diffHost = container.querySelector('.dbx-llm-diff-host');
		if (diffHost)
			renderSqlDiff(diffHost, container.querySelector('.dbx-llm-diff-stat'), opts.sql, result.optimizedSql, opts.dbVendor);
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
				httpErr.status       = resp.status;
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
		var isOra = (opts.dbVendor === 'Oracle');   // as ASE: /api/cc/mgt/table-info. 'dbname' is the container (PDB) and can be empty
		var isDb2 = (opts.dbVendor === 'DB2/');     // as Oracle. 'dbname' is the schema and can be empty (then a table without a schema is looked for in all schemas)
		if (!isAse && !isMs && !isOra && !isDb2)
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
				if (isAse || isOra || isDb2)
				{
					url = '/api/cc/mgt/table-info';
					qs.set('srv', opts.srv);
					qs.set('dbVendor', opts.dbVendor);
					qs.set('format', 'text');
					qs.set('dbname', opts.dbname || '');
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

		// (Oracle has no "database name" to give: the container name is optional. Db2: the schema is optional)
		if (opts.srv && (opts.dbname || opts.dbVendor === 'Oracle' || opts.dbVendor === 'DB2/'))
			return fetchDdlContextBySrv(opts);

		return Promise.resolve('');
	}

	/**
	 * Column in CmSummary that holds the DBMS version string - it differs per Collector.
	 * The source of truth is MonRecordingInfo.initialize() (Java), which the Daily Summary Report uses:
	 *   srvVersion  = AseTune, SqlServerTune     atAtVersion = IqTune     rsVersion = RsTune
	 *   version     = PostgresTune, MySqlTune    VERSION     = OracleTune, Db2Tune, HanaTune
	 * First one present in the response wins (exact, case-sensitive match).
	 */
	var DBMS_VERSION_COLUMNS = ['srvVersion', 'atAtVersion', 'rsVersion', 'version', 'VERSION'];

	/** 'YYYY-MM-DD HH:mm:ss' in browser-local time - the same "now" dbxcentral.graph.js sends to navSample */
	function nowAsSampleTime()
	{
		function p2(n) { return (n < 10 ? '0' : '') + n; }
		var d = new Date();
		return d.getFullYear() + '-' + p2(d.getMonth() + 1) + '-' + p2(d.getDate())
			+ ' ' + p2(d.getHours()) + ':' + p2(d.getMinutes()) + ':' + p2(d.getSeconds());
	}

	/** Read CmSummary (abs) at exactly 'sampleTime' on srv's Collector, and pick the version column out of it. */
	function fetchCmSummaryVersionAt(srv, sampleTime)
	{
		var qs = new URLSearchParams();
		qs.set('srv',  srv);
		qs.set('cm',   'CmSummary');
		qs.set('time', sampleTime);
		qs.set('type', 'abs');

		return fetchJson('/api/cc/mgt/cm/data?' + qs.toString()).then(function (r)
		{
			var cols = (r && r.columns) || [];
			var rows = (r && r.rows)    || [];
			if (!rows.length) return '';

			for (var i = 0; i < DBMS_VERSION_COLUMNS.length; i++)
			{
				var idx = cols.indexOf(DBMS_VERSION_COLUMNS[i]);
				if (idx >= 0 && rows[0][idx])
					return String(rows[0][idx]).trim();
			}
			return '';
		});
	}

	/**
	 * Resolve the DBMS version string for the prompt. Never rejects - resolves '' when it can not be found.
	 *   1. opts.dbmsVersion, when the caller already has it (the Daily Summary Report passes the RECORDED one)
	 *   2. opts.srv: LIVE from that server's Collector, out of CmSummary. At opts.sampleTime when given (all
	 *      CMs in a sample share the same SessionSampleTime), otherwise - or when that sample has no
	 *      CmSummary - from the latest CmSummary sample before opts.sampleTime/now.
	 *      Needs the Collector to be reachable, same as the srv-based DDL lookup.
	 */
	function fetchDbmsVersion(opts)
	{
		opts = opts || {};
		if (opts.dbmsVersion)
			return Promise.resolve(String(opts.dbmsVersion));

		if (!opts.srv)
			return Promise.resolve('');

		var atSample = opts.sampleTime ? fetchCmSummaryVersionAt(opts.srv, opts.sampleTime) : Promise.resolve('');

		return atSample
			.then(function (version)
			{
				if (version) return version;

				var qs = new URLSearchParams();
				qs.set('srv',  opts.srv);
				qs.set('cm',   'CmSummary');
				qs.set('time', opts.sampleTime || nowAsSampleTime());
				qs.set('dir',  'prev');

				return fetchJson('/api/cc/mgt/cm/navSample?' + qs.toString()).then(function (nav)
				{
					return (nav && nav.found && nav.sampleTime) ? fetchCmSummaryVersionAt(opts.srv, nav.sampleTime) : '';
				});
			})
			.catch(function (err)
			{
				console.warn('dbxLlmAdvice: could not resolve the DBMS version for srv=' + opts.srv + ', continuing without it: ' + (err && err.message));
				return '';
			});
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

	/**
	 * Ask /api/llm/optimize-sql and render the answer (or the prompt preview) into container.
	 * @param model  the entry chosen in the model dropdown ({provider, model}), or null for the server default / opts.provider
	 * @return a Promise of the answer (null on failure) - never rejects
	 */
	function callOptimize(container, opts, ddlContext, model)
	{
		renderStatus(container, opts.preview ? 'Building prompt preview...' : 'Asking the LLM for optimization advice...');

		// A clock while we wait: some models think for minutes (high/max effort, big plans)
		var clockTimer = null;
		if (!opts.preview)
		{
			var started = Date.now();
			var clock   = document.createElement('span');
			clock.className = 'dbx-llm-clock';
			clock.textContent = ' 0s';
			container.querySelector('.dbx-llm-status').appendChild(clock);
			clockTimer = setInterval(function ()
			{
				if (!clock.isConnected) { clearInterval(clockTimer); return; }
				var s = Math.floor((Date.now() - started) / 1000);
				clock.textContent = ' ' + (s >= 60 ? Math.floor(s / 60) + 'm ' : '') + (s % 60) + 's';
			}, 1000);
		}

		var body = {
			sql:             opts.sql,
			ddlContext:      ddlContext,
			plan:            opts.plan,
			workloadProfile: opts.workloadProfile,
			dbVendor:        opts.dbVendor,
			dbmsVersion:     opts.dbmsVersion,
			provider:        model ? model.provider : opts.provider,
			model:           model ? model.model    : opts.model,
			preview:         !!opts.preview,
			// Not part of the prompt - only recorded in the saved request log (LlmAdviceLog.java)
			srvName:         opts.srv,
			origin:          opts.origin || window.location.pathname
		};

		return fetchJson('/api/llm/optimize-sql', {
			method:  'POST',
			headers: { 'Content-Type': 'application/json' },
			body:    JSON.stringify(body)
		})
		.then(function (data)
		{
			clearInterval(clockTimer);
			if (opts.preview) renderPromptPreview(container, data && data.promptSent);
			else              renderResult(container, data, opts, ddlContext);
			return data;
		})
		.catch(function (err)
		{
			clearInterval(clockTimer);
			if (opts.preview)
			{
				container.innerHTML = '<div class="dbx-llm-error">Failed to build prompt preview: ' + escapeHtml(err.message) + '</div>';
				return null;
			}
			renderError(container, 'Failed to get LLM advice: ' + err.message, opts, ddlContext, err.responseData);

			// Login is required, but we are not logged in (LlmSqlOptimizeServlet 'not-logged-in', or the
			// MandatoryLoginFilter's 401): offer a Log in button, and ask if the user wants to log in now.
			if (err.status === 401)
			{
				var retry    = function () { open(opts); };
				var loginDiv = document.createElement('div');
				loginDiv.className = 'dbx-llm-login';
				loginDiv.innerHTML = '<button type="button" class="dbx-llm-login-btn">Log in</button> and then ask for the advice again.';
				loginDiv.querySelector('button').addEventListener('click', function () { requestLogin(retry); });
				container.insertBefore(loginDiv, container.firstChild ? container.firstChild.nextSibling : null);
				askForLogin(retry);
			}
			return null;
		});
	}

	/**
	 * "Not logged in" message + a Log in button, and ask if the user wants to log in now.
	 * @param retry  called after a successful login (on pages where we can log in without leaving the page)
	 */
	function renderLoginRequired(container, msg, retry)
	{
		container.innerHTML = '<div class="dbx-llm-error">' + escapeHtml(msg) + '</div>'
			+ '<div class="dbx-llm-login"><button type="button" class="dbx-llm-login-btn">Log in</button> and then ask for the advice again.</div>';
		container.querySelector('.dbx-llm-login-btn').addEventListener('click', function () { requestLogin(retry); });
		askForLogin(retry);
	}

	/**
	 * Small popup: "Continue to login" or "Skip and do NOT login". On skip the "not logged in" message
	 * (with its Log in button) simply stays. Not a Bootstrap modal: it must work on pages without
	 * Bootstrap too, and stack above an open Bootstrap dialog and our own floating dialog.
	 */
	function askForLogin(retry)
	{
		var old = document.getElementById('dbx-llm-askLogin');
		if (old) old.parentNode.removeChild(old);

		var div = document.createElement('div');
		div.id = 'dbx-llm-askLogin';
		div.className = 'dbx-llm-asklogin-backdrop';
		div.innerHTML = '<div class="dbx-llm-asklogin-box" role="dialog" aria-modal="true">'
			+ '<div class="dbx-llm-asklogin-title">Login required</div>'
			+ '<div>You need to log in to ask for LLM Optimization Advice.</div>'
			+ '<div class="dbx-llm-asklogin-buttons">'
			+ '<button type="button" class="dbx-llm-login-btn" data-act="login">Continue to login</button>'
			+ '<button type="button" class="dbx-llm-asklogin-skip" data-act="skip">Skip and do NOT login</button>'
			+ '</div></div>';
		document.body.appendChild(div);

		div.addEventListener('click', function (e)
		{
			var act = e.target && e.target.getAttribute && e.target.getAttribute('data-act');
			if (!act) return;
			div.parentNode.removeChild(div);
			if (act === 'login')
				requestLogin(retry);
		});
		div.querySelector('[data-act="login"]').focus();
	}

	/**
	 * Take the user to the login. Pages with the DbxCentral login dialog (dbxLoginModal.js: graph.html,
	 * index.html, the server rendered pages like /llm-advice and the Showplan pages) log in WITHOUT leaving
	 * the page: the login dialog is shown on top of everything (also the Showplan dialog), and 'retry' asks
	 * for the advice again when logged in. Other pages go to the start page's login, which returns here
	 * ('returnUrl').
	 * @param retry  called after a successful "stay on this page" login (may be null)
	 */
	function requestLogin(retry)
	{
		if (typeof window.dbxOpenLogin === 'function' && document.getElementById('dbx-login-dialog'))
		{
			window.dbxOpenLogin({ stayOnPage: true, onSuccess: retry });
			return;
		}
		window.location.href = '/index.html?login=open&returnUrl=' + encodeURIComponent(window.location.pathname + window.location.search + window.location.hash);
	}

	/**
	 * Re-render a saved request (a page written by LlmAdviceLog.java and served by /admin/llm-log?op=view)
	 * with the same renderer as the live dialog. The saved page is plain - no scripts - and holds the values
	 * as text in fixed element ids, plus the metadata as JSON in 'dbx-llm-log-meta'.
	 * @param target  element, or CSS selector, to render into (it replaces the plain content)
	 */
	function renderSavedRequest(target)
	{
		function text(id) { var el = document.getElementById(id); return el ? el.textContent : null; }

		var meta = {};
		try { meta = JSON.parse(text('dbx-llm-log-meta') || '{}'); }
		catch (err) { console.warn('dbxLlmAdvice.renderSavedRequest(): could not parse the metadata: ' + err.message); }

		// No dbmsVersion here: the saved page's header table already shows it
		var opts = {
			sql:             text('dbx-llm-sql'),
			plan:            text('dbx-llm-plan'),
			workloadProfile: text('dbx-llm-workload'),
			dbVendor:        meta.dbVendor,
			provider:        meta.providerId,
			target:          target
		};

		injectStyle();
		var container = resolveContainer(opts);
		_copyRegistry = {};
		if (meta.status === 'OK')
		{
			renderResult(container, {
				optimizedSql: text('dbx-llm-optimized-sql'),
				explanation:  text('dbx-llm-explanation'),
				promptSent:   text('dbx-llm-prompt'),
				providerId:   meta.providerId,
				model:        meta.model
			}, opts, '');
		}
		else
		{
			renderError(container, 'The LLM call failed: ' + (text('dbx-llm-error') || meta.errorMessage || 'unknown error'), opts, '',
				{ promptSent: text('dbx-llm-prompt'), providerId: meta.providerId });
		}
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

		// A new view: the "Copy" buttons of whatever was shown here before are gone
		_copyRegistry = {};

		// The prompt preview needs no login and no model: build it right away
		if (opts.preview)
		{
			lookupContext(container, opts).then(function (ddlContext) { callOptimize(container, opts, ddlContext, null); });
			return;
		}

		// A login is required but we are not logged in: say so (and go to the login) first - nothing can be asked anyway.
		// Otherwise show the model dropdown and the "Get Advice" button: NOTHING is sent to an LLM (nothing costs money)
		// before the user presses it. A failing config call (null) still shows the button, for the server's default model.
		getConfig().then(function (cfg)
		{
			if (cfg && cfg.loginRequired && !cfg.loggedIn)
			{
				renderLoginRequired(container, 'Not logged in! A login is required to ask for LLM advice.', function () { open(opts); });
				return;
			}
			if (cfg && cfg.configured === false)
			{
				container.innerHTML = '<div class="dbx-llm-error">LLM Optimization Advice is not configured: no LLM provider is set up in DBX_CENTRAL.conf.</div>';
				return;
			}
			renderModelPicker(container, opts, (cfg && cfg.models) || []);
		});
	}

	/**
	 * The table DDL/index/stats and the DBMS version - looked up ONCE per open() (the promise is kept on opts), so
	 * asking a second model does not repeat a lookup that can take long with many tables.
	 * Resolves (never rejects) to the DDL context ('' when there is none); sets opts.dbmsVersion.
	 */
	function lookupContext(container, opts)
	{
		if (!opts._contextPromise)
		{
			if (!opts.ddlContext)
				renderStatus(container, 'Looking up table DDL/index/stats...');

			// Both lookups resolve (never reject) to '' on failure, so the advice is still asked for without them
			var ddlPromise = opts.ddlContext ? Promise.resolve(opts.ddlContext) : fetchDdlContext(opts);
			opts._contextPromise = Promise.all([ddlPromise, fetchDbmsVersion(opts)]).then(function (res)
			{
				opts.dbmsVersion = res[1] || '';
				return res[0];
			});
		}
		return opts._contextPromise;
	}

	var LAST_MODEL_STORAGE_KEY = 'dbxLlmAdvice.lastModel';

	/**
	 * The model dropdown (grouped by provider) + "Get Advice" button, and the results area under it. Every press asks
	 * the chosen model and adds its answer on TOP, keeping the earlier ones - so the answers of models can be compared.
	 * @param models  /api/llm/config 'models': [{provider, providerName, model, modelId, label, isDefault}]
	 */
	function renderModelPicker(container, opts, models)
	{
		// No list (an older DbxCentral, or the config call failed): one entry, the server's default model
		if (models.length === 0)
			models = [{ provider: opts.provider, providerName: 'Default', model: opts.model, label: 'The default model', isDefault: true }];

		// Preselect: the last choice on this browser (if still configured), else the caller's provider, else the default.
		// The last choice is remembered together with the default of that moment: when the admin changes the default
		// (DbxCentral.llm.provider / the order of a 'model' list), everybody starts from the new default once.
		var defModel = models.filter(function (m) { return m.isDefault; })[0];
		var defKey   = defModel ? defModel.provider + '/' + defModel.model : '';
		var last = null;
		try { last = JSON.parse(window.localStorage.getItem(LAST_MODEL_STORAGE_KEY) || 'null'); } catch (e) { /* private mode etc: no memory */ }
		if (last && last.defaultModel !== defKey)
			last = null;

		var selected = -1;
		models.forEach(function (m, i) { if (selected < 0 && last && m.provider === last.provider && m.model === last.model) selected = i; });
		models.forEach(function (m, i) { if (selected < 0 && opts.provider && m.provider === opts.provider && (!opts.model || m.model === opts.model)) selected = i; });
		models.forEach(function (m, i) { if (selected < 0 && m.isDefault) selected = i; });
		if (selected < 0) selected = 0;

		var optionsHtml = '';
		var groupProvider = null;
		models.forEach(function (m, i)
		{
			if (m.provider !== groupProvider)
			{
				optionsHtml += (groupProvider !== null ? '</optgroup>' : '') + '<optgroup label="' + escapeHtml(m.providerName || m.provider) + '">';
				groupProvider = m.provider;
			}
			var title = (m.modelId && m.modelId !== m.label) ? ' title="' + escapeHtml(m.modelId) + '"' : '';
			optionsHtml += '<option value="' + i + '"' + (i === selected ? ' selected' : '') + title + '>'
				+ escapeHtml(m.label || m.model || '') + (m.isDefault ? ' (default)' : '') + '</option>';
		});
		optionsHtml += '</optgroup>';

		container.innerHTML = '<div class="dbx-llm-picker">'
			+ '<span class="dbx-llm-picker-label">Model</span>'
			+ '<select class="dbx-llm-model-select" aria-label="LLM model">' + optionsHtml + '</select>'
			+ '<button type="button" class="dbx-llm-ask-btn">Get Advice</button>'
			+ '<span class="dbx-llm-picker-hint">Nothing is sent to the LLM before you press the button.</span>'
			+ '</div>'
			+ '<div class="dbx-llm-results"></div>';

		var select  = container.querySelector('.dbx-llm-model-select');
		var askBtn  = container.querySelector('.dbx-llm-ask-btn');
		var hint    = container.querySelector('.dbx-llm-picker-hint');
		var results = container.querySelector('.dbx-llm-results');

		askBtn.addEventListener('click', function ()
		{
			var model = models[parseInt(select.value, 10)] || models[0];
			try { window.localStorage.setItem(LAST_MODEL_STORAGE_KEY, JSON.stringify({ provider: model.provider, model: model.model, defaultModel: defKey })); } catch (e) { /* no memory */ }

			// One request at a time: a double click must not pay twice
			askBtn.disabled = true;
			hint.textContent = '';

			// A <details>: click the header to fold/unfold this answer (handy with several answers)
			var run = document.createElement('details');
			run.className = 'dbx-llm-run';
			run.open = true;
			run.innerHTML = '<summary class="dbx-llm-run-header">' + escapeHtml((model.providerName || model.provider || '') + ' / ' + (model.label || model.model || ''))
				+ '<span class="dbx-llm-run-info"></span></summary>'
				+ '<div class="dbx-llm-run-body"></div>';
			results.insertBefore(run, results.firstChild);

			var body    = run.querySelector('.dbx-llm-run-body');
			var info    = run.querySelector('.dbx-llm-run-info');
			var startMs = Date.now();

			lookupContext(body, opts)
				.then(function (ddlContext) { return callOptimize(body, opts, ddlContext, model); })
				.then(function (data)
				{
					var txt = ' - ' + ((Date.now() - startMs) / 1000).toFixed(1) + ' s';
					if (data && data.inputTokens != null && data.outputTokens != null)
						txt += ', ' + data.inputTokens.toLocaleString() + ' in / ' + data.outputTokens.toLocaleString() + ' out tokens';
					info.textContent = txt;
					askBtn.disabled = false;
				});
		});
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

	/**
	 * Full /api/llm/config answer ({enabled, configured, provider, providerName, model, loginRequired,
	 * loggedIn}) - see LlmConfigServlet. Unlike isEnabled() this is fetched fresh on every call, since
	 * the login state can change after the page was loaded. Resolves to null (never rejects) on failure.
	 */
	function getConfig()
	{
		return fetch('/api/llm/config')
			.then(function (resp) { return resp.json(); })
			.catch(function (err)
			{
				console.warn('dbxLlmAdvice: /api/llm/config failed: ' + err.message);
				return null;
			});
	}

	/**
	 * Short status text for next to an "LLM Optimization Advice" heading, from a getConfig() result:
	 * the default model (and how many more can be chosen), or why it can't be used right now.
	 */
	function getStatusText(cfg)
	{
		if (!cfg)            return '';
		if (cfg.configured === false) return 'Needs to be configured';

		var txt = cfg.model || cfg.providerName || cfg.provider || '';
		if (cfg.models && cfg.models.length > 0)
		{
			var def = cfg.models.filter(function (m) { return m.isDefault; })[0] || cfg.models[0];
			txt = def.label || def.model;
			if (cfg.models.length > 1)
				txt += ' (+' + (cfg.models.length - 1) + ' more)';
		}
		if (cfg.loginRequired && !cfg.loggedIn)
			txt += (txt ? ' - ' : '') + 'Needs Login';
		return txt;
	}

	return {
		open: open,
		close: close,
		isEnabled: isEnabled,
		getConfig: getConfig,
		getStatusText: getStatusText,
		// Exposed for callers that build their own /api/llm/optimize-sql request instead of going
		// through open() - notably dbxShowplan.js's "LLM Prompt Preview" - so the prompt they PREVIEW
		// is the same one open() would actually SEND.
		buildWorkloadProfile: buildWorkloadProfile,
		// Same reason: previews resolve the DBMS version exactly the way open() does.
		fetchDbmsVersion: fetchDbmsVersion,
		// Admin page: show a saved request (see LlmAdviceLog.java / LlmAdviceLogServlet.java)
		renderSavedRequest: renderSavedRequest
	};
})();
