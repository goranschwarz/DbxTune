/**
 * dbxShowplan.js — injectable Postgres + SQL Server showplan dialogs
 *
 * Self-contained: injects its own modal HTML into document.body on DOM ready.
 * No changes to graph.html required beyond adding the <script> tag.
 *
 * Dependencies (loaded before this file):
 *   jQuery, Bootstrap 4, Vue 3 + pev2, QP (html-query-plan), Panzoom,
 *   sqlFormatter, Prism
 *
 * Global functions exposed:
 *   pgShowplanGetSql(), pgShowplanFormatSql(), pgShowplanCopySql(),
 *   pgShowplanCopyPlan(), pgShowplanSaveToFile(), pgShowplanOpenExternal()
 *   ssShowplanEnableZoom(), ssShowplanResetZoom(), ssShowplanFormatSql(),
 *   ssShowplanCopySql(), ssShowplanCopyXml(), ssShowplanSaveXmlToFile(),
 *   ssShowplanOpenExternal(), ssShowplanGetParameters(),
 *   ssShowplanSetParametersInSql()
 *   submit_post_via_hidden_form()
 */
(function () {
	'use strict';

	// -------------------------------------------------------------------------
	// Shared state
	// -------------------------------------------------------------------------
	var pev2app         = undefined;
	var _ssShowplanZoom = undefined;
	var _sspWaitChart   = null;    // Chart.js instance for Plan Analysis wait bar

	function escapeHtml(s) {
		if (s == null) return '';
		return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
	}

	// -------------------------------------------------------------------------
	// Lightweight SQL table-name extractor (JS port of TableNameParser.java)
	// Looks for tokens immediately after: FROM, JOIN, INTO, TABLE, USING, UPDATE, CALL
	// Filters out variables (@...), temp tables (#...), and known non-table tokens.
	// -------------------------------------------------------------------------
	function _sqlExtractTables(sql) {
		if (!sql) return [];

		// 1. Strip single-line comments (-- ...)
		var noComments = sql.replace(/--[^\r\n]*/g, ' ');

		// 2. Strip block comments (/* ... */) but preserve Oracle hints /*+ ... */
		noComments = noComments.replace(/\/\*(?!\+)[\s\S]*?\*\//g, ' ');

		// 3. Normalize: collapse whitespace, pad commas and parens with spaces
		var normalized = noComments
			.replace(/[\r\n]+/g, ' ')
			.replace(/,/g, ' , ')
			.replace(/\(/g, ' ( ')
			.replace(/\)/g, ' ) ')
			.replace(/\s+/g, ' ')
			.trim();

		// Remove trailing semicolon
		if (normalized.charAt(normalized.length - 1) === ';')
			normalized = normalized.slice(0, -1);

		var tokens  = normalized.split(' ');
		var trigger = { 'from':1, 'join':1, 'into':1, 'table':1, 'using':1, 'update':1, 'call':1 };
		var skip    = { '(':1, 'set':1, 'of':1, 'dual':1 };
		var seen    = {};
		var result  = [];

		for (var i = 0; i < tokens.length; i++) {
			var tok = tokens[i].toLowerCase();
			if (!trigger[tok]) continue;

			// Collect one or more comma-separated table names after the keyword
			i++;
			while (i < tokens.length) {
				var name = tokens[i];
				var nameLo = name.toLowerCase();
				i++;
				if (!name || skip[nameLo]) break;

				// Accept as a table name if it doesn't look like a keyword/variable/temp
				if (!nameLo.startsWith('@') && !nameLo.startsWith('#') &&
				    nameLo !== 'row_number' && !skip[nameLo]) {
					// Strip schema prefix (schema.table or db..table)
					var bare = name.replace(/^.*[.\[]/, '').replace(/[\[\]"`]/g, '');
					if (bare && !seen[bare.toLowerCase()]) {
						seen[bare.toLowerCase()] = true;
						result.push(bare);
					}
				}

				// Continue only if the next token is a comma (multi-table FROM clause)
				if (i < tokens.length && tokens[i] === ',') {
					i++;   // skip the comma, loop continues to grab next name
				} else {
					break;
				}
			}
			i--;   // outer loop will increment again
		}
		return result;
	}

	// -------------------------------------------------------------------------
	// node-sql-parser (T-SQL UMD bundle) — lazy loader + async table extractor
	// Falls back to _sqlExtractTables() if the script fails to load or parse fails.
	// -------------------------------------------------------------------------
	var _nspState    = 'idle';   // 'idle' | 'loading' | 'ready' | 'failed'
	var _nspWaiters  = [];       // callbacks queued while loading

	function _nspLoad(cb) {
		if (_nspState === 'ready')  { cb(true);  return; }
		if (_nspState === 'failed') { cb(false); return; }
		_nspWaiters.push(cb);
		if (_nspState === 'loading') return;
		_nspState = 'loading';
		var s = document.createElement('script');
		s.src = '/scripts/node-sql-parser/4.18.0/transactsql.umd.js';
		s.onload = function() {
			_nspState = 'ready';
			_nspWaiters.forEach(function(fn) { fn(true);  }); _nspWaiters = [];
		};
		s.onerror = function() {
			_nspState = 'failed';
			_nspWaiters.forEach(function(fn) { fn(false); }); _nspWaiters = [];
		};
		document.head.appendChild(s);
	}

	// Async wrapper: uses node-sql-parser when available, falls back to tokenizer.
	// callback(tables[]) is always called.
	function _extractTablesAsync(sql, callback) {
		_nspLoad(function(ok) {
			if (!ok || typeof NodeSQLParser === 'undefined') {
				callback(_sqlExtractTables(sql));
				return;
			}
			try {
				var parser = new NodeSQLParser.Parser();
				var opt    = { database: 'TransactSQL' };

				// Collect CTE names from the AST so we can exclude them
				var cteNames = {};
				try {
					var ast   = parser.astify(sql, opt);
					var stmts = Array.isArray(ast) ? ast : [ast];
					stmts.forEach(function(stmt) {
						(stmt.with || []).forEach(function(w) {
							if (w.name && w.name.value)
								cteNames[w.name.value.toLowerCase()] = true;
						});
					});
				} catch(e2) { /* ignore — we still have tableList below */ }

				var list  = parser.tableList(sql, opt);
				var seen  = {};
				var tables = [];
				list.forEach(function(entry) {
					var name = entry.split('::')[2];
					if (!name || name === 'null') return;
					if (cteNames[name.toLowerCase()])  return;   // skip CTE alias
					if (seen[name.toLowerCase()])      return;   // dedup
					seen[name.toLowerCase()] = true;
					tables.push(name);
				});

				// Fall back to tokenizer if parser returned nothing useful
				callback(tables.length ? tables : _sqlExtractTables(sql));
			} catch(e) {
				callback(_sqlExtractTables(sql));
			}
		});
	}

	// -------------------------------------------------------------------------
	// Inject modal HTML + scoped styles
	// -------------------------------------------------------------------------
	function _injectHtml() {
		if (document.getElementById('dbx-view-pgShowplan-dialog')) return;

		// Scoped CSS for the Table Information section inside the showplan dialog.
		// Uses #dbx-ssp-tableinfo-body as scope so the DSR stylesheet is not affected.
		var style = document.createElement('style');
		style.textContent = [
			/* ── outer wrapper table ── */
			'#dbx-ssp-tableinfo-body table.qs-tableinfo {',
			'  border-collapse: collapse;',
			'  width: 100%;',
			'  font-size: 0.8em;',
			'}',
			'#dbx-ssp-tableinfo-body table.qs-tableinfo > thead > tr > th {',
			'  background: #4a6fa5;',
			'  color: #fff;',
			'  font-weight: 600;',
			'  padding: 4px 8px;',
			'  border: 1px solid #3a5f95;',
			'  white-space: nowrap;',
			'}',
			'#dbx-ssp-tableinfo-body table.qs-tableinfo > tbody > tr > td {',
			'  border: 1px solid #c8c8c8;',
			'  padding: 4px 6px;',
			'  vertical-align: top;',
			'  white-space: nowrap;',
			'}',
			/* ── key/value table inside "Table Info" cell ── */
			'#dbx-ssp-tableinfo-body table.dsr-sub-table-other-info {',
			'  border-collapse: collapse;',
			'  width: 100%;',
			'  font-size: 1em;',  /* already inside 0.8em context */
			'}',
			'#dbx-ssp-tableinfo-body table.dsr-sub-table-other-info td {',
			'  border: 1px solid #d0d0d0;',
			'  padding: 2px 6px;',
			'  vertical-align: top;',
			'  white-space: nowrap;',
			'}',
			'#dbx-ssp-tableinfo-body table.dsr-sub-table-other-info td:first-child {',
			'  white-space: nowrap;',
			'  min-width: 90px;',
			'}',
			/* ── nested index table inside "Index Info" cell ── */
			'#dbx-ssp-tableinfo-body table.qs-tableinfo table.qs-tableinfo {',
			'  width: 100%;',
			'}',
			'#dbx-ssp-tableinfo-body table.qs-tableinfo table.qs-tableinfo > thead > tr > th {',
			'  background: #5a7fa8;',
			'  font-size: 0.9em;',
			'  padding: 2px 5px;',
			'  white-space: nowrap;',
			'}',
			'#dbx-ssp-tableinfo-body table.qs-tableinfo table.qs-tableinfo > tbody > tr > td {',
			'  border: 1px solid #d0d0d0;',
			'  padding: 2px 5px;',
			'  white-space: nowrap;',
			'}',
			'#dbx-ssp-tableinfo-body table.qs-tableinfo table.qs-tableinfo > tbody > tr:nth-child(even) > td {',
			'  background: #f4f7fb;',
			'}',
			/* ── missing index table ── */
			'#dbx-ssp-tableinfo-body table.dsr-missing-index-table {',
			'  border-collapse: collapse;',
			'  width: 100%;',
			'  font-size: 0.8em;',
			'}',
			'#dbx-ssp-tableinfo-body table.dsr-missing-index-table > thead > tr > th {',
			'  background: #a05a00;',
			'  color: #fff;',
			'  font-weight: 600;',
			'  padding: 4px 8px;',
			'  border: 1px solid #804800;',
			'  white-space: nowrap;',
			'}',
			'#dbx-ssp-tableinfo-body table.dsr-missing-index-table > tbody > tr > td {',
			'  border: 1px solid #c8c8c8;',
			'  padding: 4px 6px;',
			'  vertical-align: top;',
			'  white-space: nowrap;',
			'}',
			'#dbx-ssp-tableinfo-body table.dsr-missing-index-table > tbody > tr:nth-child(even) > td {',
			'  background: #fdf5ec;',
			'}',
			/* ── tooltip-div trigger links (data-toggle="modal" data-target="#dbx-view-sqltext-dialog") ── */
			'#dbx-ssp-tableinfo-body [data-toggle="modal"][data-target="#dbx-view-sqltext-dialog"] {',
			'  cursor: pointer;',
			'  color: #1a56a0;',
			'  text-decoration: underline;',
			'  text-decoration-style: dotted;',
			'  display: inline-block;',
			'  border-radius: 3px;',
			'  padding: 0 2px;',
			'}',
			'#dbx-ssp-tableinfo-body [data-toggle="modal"][data-target="#dbx-view-sqltext-dialog"]:hover {',
			'  background: #e8f0fb;',
			'  color: #0a3a7a;',
			'}',
			/* ── DSR-style hover tooltip (shows full text on hover, same as Daily Summary Report) ── */
			'#dbx-ssp-tableinfo-body [data-tooltip] {',
			'  position: relative;',
			'}',
			'#dbx-ssp-tableinfo-body [data-tooltip]:hover::before {',
			'  content: attr(data-tooltip);',
			'  position: absolute;',
			'  z-index: 1200;',
			'  top: 20px;',
			'  left: 30px;',
			'  width: 800px;',
			'  max-width: 90vw;',
			'  padding: 10px;',
			'  background: #454545;',
			'  color: #fff;',
			'  font-size: 11px;',
			'  font-family: Courier, monospace;',
			'  white-space: pre-wrap;',
			'  border-radius: 4px;',
			'  box-shadow: 0 2px 8px rgba(0,0,0,0.4);',
			'  pointer-events: none;',
			'}',
			/* ── text-viewer modal sits above the showplan modal (Bootstrap default is 1050) ── */
			'#dbx-view-sqltext-dialog {',
			'  z-index: 1100;',
			'}',
			'.modal-backdrop + .modal-backdrop {',
			'  z-index: 1090;',
			'}'
		].join('\n');
		document.head.appendChild(style);

		document.body.insertAdjacentHTML('beforeend', [
			// ---- Postgres Execution Plan dialog ----
			"<div class='modal fade' id='dbx-view-pgShowplan-dialog' role='dialog' aria-labelledby='dbx-view-pgShowplan-dialog' aria-hidden='true'>",
			"	<div class='modal-dialog modal-dialog-centered mw-100 w-75' role='document'>",
			"		<div class='modal-content' style='height: 80vh;'>",
			"			<div class='modal-header'>",
			"				<h5 class='modal-title'><b>Postgres Execution Plan:</b> <span id='dbx-view-pgShowplan-objectName'></span></h5>",
			"				<button type='button' class='close' data-dismiss='modal' aria-label='Close'><span aria-hidden='true'>&times;</span></button>",
			"			</div>",
			"			<div class='modal-body' style='overflow-x: auto;'>",
			"				<div class='scroll-tree'>",
			"					<button type='button' class='btn btn-outline-secondary btn-sm' onclick='$(\"#dbx-view-pgShowplan-content\").toggle();'>Hide/Show Below Execution Plan</button>",
			"					<br>",
			"					<div id='dbx-view-pgShowplan-content' class='dbx-view-pgShowplan-content'></div>",
			"					<br>",
			"					<button type='button' class='btn btn-outline-secondary btn-sm' onclick='$(\"#dbx-view-pgShowplan-sqlContent\").toggle();'>Hide/Show Below SQL Text</button>",
			"					<button type='button' class='btn btn-outline-secondary btn-sm' onclick='pgShowplanFormatSql();'>Format Below SQL Text</button>",
			"					<pre><code id='dbx-view-pgShowplan-sqlContent' class='language-sql line-numbers dbx-view-sqltext-content'></code></pre>",
			"					<button type='button' class='btn btn-outline-secondary btn-sm' onclick='$(\"#dbx-view-pgShowplan-planContent\").toggle();'>Hide/Show Below Plan Text</button>",
			"					<pre><code id='dbx-view-pgShowplan-planContent' class='language-json line-numbers dbx-view-sqltext-content'></code></pre>",
			"				</div>",
			"			</div>",
			"			<div class='modal-footer'>",
			"				<button type='button' class='btn btn-outline-secondary' onclick='pgShowplanCopySql();'>Copy SQL Text</button>",
			"				<button type='button' class='btn btn-outline-secondary' onclick='pgShowplanCopyPlan();'>Copy Plan Text</button>",
			"				<button type='button' class='btn btn-outline-secondary' onclick='pgShowplanSaveToFile();'>Save Plan File</button>",
			"				<button type='button' class='btn btn-outline-secondary' onclick='pgShowplanOpenExternal();'>Open Plan in External Window</button>",
			"				&emsp;&emsp;&emsp;&emsp;&emsp;",
			"				<button type='button' class='btn btn-secondary' data-dismiss='modal'>Close</button>",
			"			</div>",
			"		</div>",
			"	</div>",
			"</div>",

			// ---- SQL Server Showplan dialog ----
			"<div class='modal fade' id='dbx-view-ssShowplan-dialog' role='dialog' aria-labelledby='dbx-view-ssShowplan-dialog' aria-hidden='true'>",
			"	<div class='modal-dialog modal-dialog-centered mw-100' role='document'>",
			"		<div class='modal-content'>",
			"			<div class='modal-header' style='cursor:move;'>",
			"				<span style='color:#999;margin-right:6px;font-size:1.1em;' title='Drag to move'>&#x2630;</span>",
			"				<div style='flex:1;min-width:0;'>",
			"					<h5 class='modal-title' style='margin-bottom:1px;'><b>SQL Server Showplan</b> <span id='dbx-view-ssShowplan-plantype' style='font-size:0.78em;font-weight:normal;margin-left:6px;'></span><b>:</b> <span id='dbx-view-ssShowplan-objectName'></span></h5>",
			"					<div id='dbx-view-ssShowplan-timestamps' style='font-size:0.75em;color:#888;'></div>",
			"				</div>",
			"				<button type='button' class='close' style='margin-left:8px;' data-dismiss='modal' aria-label='Close'><span aria-hidden='true'>&times;</span></button>",
			"			</div>",
			"			<div class='modal-body' style='overflow-x:auto;padding:8px 12px;'>",
			"				<div class='scroll-tree' style='width:3000px;'>",

			"				<!-- ▶ Execution Plan -->",
			"				<details open id='dbx-ssp-sect-plan' style='border:1px solid #d0d0d0;border-radius:3px;background:#fafafa;margin-bottom:4px;'>",
			"					<summary style='cursor:pointer;padding:5px 10px;font-size:0.85em;font-weight:600;list-style:none;user-select:none;'>&#128202; Execution Plan</summary>",
			"					<div style='padding:4px 8px 10px 8px;'>",
			"						<button type='button' class='btn btn-outline-secondary btn-sm' onclick='QP.drawLines(document.getElementById(\"dbx-view-ssShowplan-content\"));'>&#8635; Redraw Lines</button>",
			"						<button type='button' class='btn btn-outline-secondary btn-sm' onclick='ssShowplanEnableZoom();'>&#128269; Enable Zoom</button>",
			"						<button type='button' class='btn btn-outline-secondary btn-sm' onclick='ssShowplanResetZoom();'>&#8634; Reset Zoom</button>",
			"						<div id='dbx-view-ssShowplan-content' class='dbx-view-ssShowplan-content' style='margin-top:6px;'></div>",
			"					</div>",
			"				</details>",

			"				<!-- ▶ Plan Analysis (managed by ssShowplanRunAnalysis) -->",
			"				<details id='dbx-view-ssShowplan-analysis' style='display:none;border:1px solid #d0d0d0;border-radius:3px;background:#fafafa;margin-bottom:4px;'>",
			"					<summary id='dbx-view-ssShowplan-analysis-summary' style='cursor:pointer;padding:5px 10px;font-size:0.85em;font-weight:600;list-style:none;user-select:none;'>&#128270; Plan Analysis</summary>",
			"					<div id='dbx-view-ssShowplan-analysis-body' style='padding:4px 12px 8px 12px;'></div>",
			"				</details>",

			"				<!-- ▶ SQL Text -->",
			"				<details open id='dbx-ssp-sect-sql' style='border:1px solid #d0d0d0;border-radius:3px;background:#fafafa;margin-bottom:4px;'>",
			"					<summary style='cursor:pointer;padding:5px 10px;font-size:0.85em;font-weight:600;list-style:none;user-select:none;'>&#128196; SQL Text</summary>",
			"					<div style='padding:4px 8px 8px 8px;'>",
			"						<button type='button' class='btn btn-outline-secondary btn-sm' style='margin-bottom:4px;' onclick='ssShowplanFormatSql();'>Format SQL</button>",
			"						<div style='position:relative;'>",
			"							<button onclick='dbxCopyCodeBlock(\"dbx-view-ssShowplan-sqlContent\");' style='position:absolute;right:6px;top:6px;z-index:10;font-size:0.75em;padding:1px 8px;cursor:pointer;background:#f0f0f0;border:1px solid #bbb;border-radius:3px;opacity:0.85;'>Copy</button>",
			"							<pre><code id='dbx-view-ssShowplan-sqlContent' class='language-sql line-numbers dbx-view-sqltext-content'></code></pre>",
			"						</div>",
			"					</div>",
			"				</details>",

			"				<!-- ▶ Parameters -->",
			"				<details id='dbx-ssp-sect-params' style='border:1px solid #d0d0d0;border-radius:3px;background:#fafafa;margin-bottom:4px;'>",
			"					<summary style='cursor:pointer;padding:5px 10px;font-size:0.85em;font-weight:600;list-style:none;user-select:none;'>&#9881;&#65039; Parameters</summary>",
			"					<div style='padding:4px 8px 8px 8px;'>",
			"						<div id='dbx-ssp-no-params' style='display:none;color:#888;font-size:0.85em;'>No parameters in this plan.</div>",
			"						<div id='dbx-ssp-compile-block'>",
			"							<div style='font-size:0.8em;font-weight:600;color:#555;margin:4px 0 2px 0;'>Compile-time values</div>",
			"							<button type='button' id='dbx-view-ssShowplan-compileParameterValuesButton' class='btn btn-outline-secondary btn-sm' style='margin-bottom:3px;' onclick=\"ssShowplanSetParametersInSql('compile');\">Apply to SQL Text</button>",
			"							<div style='position:relative;'>",
			"								<button onclick='dbxCopyCodeBlock(\"dbx-view-ssShowplan-compileParameterValues\");' style='position:absolute;right:6px;top:6px;z-index:10;font-size:0.75em;padding:1px 8px;cursor:pointer;background:#f0f0f0;border:1px solid #bbb;border-radius:3px;opacity:0.85;'>Copy</button>",
			"								<pre><code id='dbx-view-ssShowplan-compileParameterValues' class='language-sql line-numbers dbx-view-sqltext-content'></code></pre>",
			"							</div>",
			"						</div>",
			"						<div id='dbx-ssp-runtime-block'>",
			"							<div style='font-size:0.8em;font-weight:600;color:#555;margin:6px 0 2px 0;'>Runtime values</div>",
			"							<button type='button' id='dbx-view-ssShowplan-runtimeParameterValuesButton' class='btn btn-outline-secondary btn-sm' style='margin-bottom:3px;' onclick=\"ssShowplanSetParametersInSql('runtime');\">Apply to SQL Text</button>",
			"							<div style='position:relative;'>",
			"								<button onclick='dbxCopyCodeBlock(\"dbx-view-ssShowplan-runtimeParameterValues\");' style='position:absolute;right:6px;top:6px;z-index:10;font-size:0.75em;padding:1px 8px;cursor:pointer;background:#f0f0f0;border:1px solid #bbb;border-radius:3px;opacity:0.85;'>Copy</button>",
			"								<pre><code id='dbx-view-ssShowplan-runtimeParameterValues' class='language-sql line-numbers dbx-view-sqltext-content'></code></pre>",
			"							</div>",
			"						</div>",
			"					</div>",
			"				</details>",

			"				<!-- ▶ Table Information (lazy-loaded on first expand) -->",
			"				<details id='dbx-ssp-sect-tableinfo' style='border:1px solid #d0d0d0;border-radius:3px;background:#fafafa;margin-bottom:4px;'>",
			"					<summary style='cursor:pointer;padding:5px 10px;font-size:0.85em;font-weight:600;list-style:none;user-select:none;'>&#128220; Table Information</summary>",
			"					<div id='dbx-ssp-tableinfo-body' style='padding:4px 8px 8px 8px;'>",
			"						<span style='color:#888;font-size:0.85em;'>&#9203; Loading table information…</span>",
			"					</div>",
			"				</details>",

			"				<!-- ▶ XML Plan (collapsed by default) -->",
			"				<details id='dbx-ssp-sect-xml' style='border:1px solid #d0d0d0;border-radius:3px;background:#fafafa;margin-bottom:4px;'>",
			"					<summary style='cursor:pointer;padding:5px 10px;font-size:0.85em;font-weight:600;list-style:none;user-select:none;'>&#128196; XML Plan</summary>",
			"					<div style='padding:4px 8px 8px 8px;'>",
			"						<div style='position:relative;'>",
			"							<button onclick='dbxCopyCodeBlock(\"dbx-view-ssShowplan-xmlContent\");' style='position:absolute;right:6px;top:6px;z-index:10;font-size:0.75em;padding:1px 8px;cursor:pointer;background:#f0f0f0;border:1px solid #bbb;border-radius:3px;opacity:0.85;'>Copy</button>",
			"							<pre><code id='dbx-view-ssShowplan-xmlContent' class='language-xml line-numbers dbx-view-sqltext-content'></code></pre>",
			"						</div>",
			"					</div>",
			"				</details>",

			"				</div>",
			"			</div>",
			"			<div class='modal-footer'>",
			"				<button type='button' class='btn btn-outline-secondary' onclick='ssShowplanCopySql();'>Copy SQL</button>",
			"				<button type='button' class='btn btn-outline-secondary' onclick='ssShowplanCopyXml();'>Copy XML</button>",
			"				<button type='button' class='btn btn-outline-secondary' onclick='ssShowplanSaveXmlToFile();'>Save XML File</button>",
			"				<button type='button' class='btn btn-outline-secondary' onclick='ssShowplanOpenExternal();'>Open in External Window</button>",
			"				&emsp;&emsp;&emsp;&emsp;&emsp;",
			"				<button type='button' class='btn btn-secondary' data-dismiss='modal'>Close</button>",
			"			</div>",
			"		</div>",
			"	</div>",
			"</div>"
		].join('\n'));

		// ── Showplan Loader input dialog ──────────────────────────────────────────
		document.body.insertAdjacentHTML('beforeend',
			  "<div class='modal fade' id='dbx-showplan-viewer-input-dialog' tabindex='-1' role='dialog' aria-labelledby='dbx-spv-title' aria-hidden='true'>"
			+ "  <div id='dbx-spv-dialog' class='modal-dialog modal-dialog-centered' style='max-width:none;width:720px;min-width:400px;height:560px;min-height:300px;'>"
			+ "    <div id='dbx-spv-content' class='modal-content' style='display:flex;flex-direction:column;height:100%;'>"
			+ "      <div class='modal-header' style='cursor:move;flex-shrink:0;'>"
			+ "        <span style='color:#999;margin-right:6px;font-size:1.1em;' title='Drag to move'>&#x2630;</span>"
			+ "        <h5 class='modal-title' id='dbx-spv-title'>&#128221; Showplan Loader</h5>"
			+ "        <button type='button' class='close' data-dismiss='modal' aria-label='Close'><span aria-hidden='true'>&times;</span></button>"
			+ "      </div>"
			+ "      <div class='modal-body' style='padding:10px 14px;display:flex;flex-direction:column;flex:1;min-height:0;overflow:hidden;'>"
			+ "        <div style='flex-shrink:0;margin-bottom:6px;'><small class='text-muted'>Paste a SQL Server Showplan XML, or load it from a <code>.xml</code> / <code>.sqlplan</code> file.</small></div>"
			+ "        <div style='flex-shrink:0;margin-bottom:8px;display:flex;align-items:center;gap:6px;'>"
			+ "          <button type='button' class='btn btn-sm btn-outline-secondary' onclick=\"document.getElementById('dbx-spv-file-input').click();\">&#128194; Load from File</button>"
			+ "          <input type='file' id='dbx-spv-file-input' accept='.xml,.sqlplan' style='display:none' onchange='spvLoadFile(event)'>"
			+ "          <button type='button' class='btn btn-sm btn-outline-secondary' onclick=\"document.getElementById('dbx-spv-xml-input').value=''; document.getElementById('dbx-spv-error').textContent='';\">&#10006; Clear</button>"
			+ "        </div>"
			+ "        <textarea id='dbx-spv-xml-input' class='form-control'"
			+ "          placeholder='&lt;ShowPlanXML xmlns=&quot;...&quot;&gt; ... &lt;/ShowPlanXML&gt;'"
			+ "          spellcheck='false' autocomplete='off' autocorrect='off' autocapitalize='off'"
			+ "          style='font-family:monospace;font-size:0.8em;resize:none;flex:1;min-height:0;width:100%;box-sizing:border-box;'></textarea>"
			+ "      </div>"
			+ "      <div class='modal-footer' style='flex-shrink:0;'>"
			+ "        <span id='dbx-spv-error' class='text-danger mr-auto small'></span>"
			+ "        <button type='button' class='btn btn-secondary' data-dismiss='modal'>Cancel</button>"
			+ "        <button type='button' class='btn btn-primary' onclick='spvViewPlan();'>&#128202; View Plan</button>"
			+ "      </div>"
			+ "    </div>"
			+ "  </div>"
			+ "</div>");

		// ── Text viewer dialog (used by DSR tooltip-divs: data-target="#dbx-view-sqltext-dialog") ──
		if (!document.getElementById('dbx-view-sqltext-dialog')) {
			document.body.insertAdjacentHTML('beforeend',
				  "<div class='modal fade' id='dbx-view-sqltext-dialog' tabindex='-1' role='dialog' aria-hidden='true' style='z-index:1100;'>"
				+ "  <div class='modal-dialog modal-lg modal-dialog-centered'>"
				+ "    <div class='modal-content'>"
				+ "      <div class='modal-header'>"
				+ "        <h5 class='modal-title' id='dbx-sqltext-dlg-title'></h5>"
				+ "        <button type='button' class='close' data-dismiss='modal'><span>&times;</span></button>"
				+ "      </div>"
				+ "      <div class='modal-body' style='padding:8px;'>"
				+ "        <pre id='dbx-sqltext-dlg-content' style='font-size:0.8em;max-height:70vh;overflow:auto;margin:0;white-space:pre-wrap;word-break:break-word;'></pre>"
				+ "      </div>"
				+ "      <div class='modal-footer'>"
				+ "        <button type='button' class='btn btn-secondary' data-dismiss='modal'>Close</button>"
				+ "      </div>"
				+ "    </div>"
				+ "  </div>"
				+ "</div>");
		}

		// Explicitly wire up drag + resize since dbxGraphPage.js may have already
		// scanned the DOM before this modal was injected.
		setTimeout(function() {
			var $dlg  = $('#dbx-spv-dialog');
			var $cont = $('#dbx-spv-content');
			if ($dlg.length && $.fn.draggable && !$dlg.hasClass('ui-draggable')) {
				$dlg.draggable({ handle: '.modal-header' });
			}
			if ($cont.length && $.fn.resizable && !$cont.hasClass('ui-resizable')) {
				$cont.resizable({
					handles: 'e, s, se',
					alsoResize: '#dbx-spv-dialog',
					minWidth: 400, minHeight: 200
				});
			}
		}, 100);
	}

	// -------------------------------------------------------------------------
	// Shared copy-to-clipboard utility for Prism code blocks
	// -------------------------------------------------------------------------
	window.dbxCopyCodeBlock = function(id) {
		var el = document.getElementById(id);
		var txt = el ? (el.textContent || el.innerText || '') : '';
		if (!txt) return;
		if (navigator.clipboard && window.isSecureContext) {
			navigator.clipboard.writeText(txt).catch(function() { _legacyCopy(txt); });
		} else {
			_legacyCopy(txt);
		}
	};
	function _legacyCopy(txt) {
		var ta = document.createElement('textarea');
		ta.value = txt;
		ta.style.position = 'fixed'; ta.style.opacity = '0';
		document.body.appendChild(ta);
		ta.focus(); ta.select();
		try { document.execCommand('copy'); } catch(e) { alert('Copy failed:\n' + e); }
		document.body.removeChild(ta);
	}

	// -------------------------------------------------------------------------
	// Shared utility
	// -------------------------------------------------------------------------
	window.submit_post_via_hidden_form = function (url, params) {
		var hiddenForm = $('<form target="_blank" method="POST" style="display:none;"></form>').attr({ action: url }).appendTo(document.body);
		for (var i in params) {
			if (params.hasOwnProperty(i)) {
				$('<input type="hidden" />').attr({ name: i, value: params[i] }).appendTo(hiddenForm);
			}
		}
		hiddenForm.submit();
		hiddenForm.remove();
	};

	function formatXml(xml) {
		var formatted = '';
		var reg = /(>)(<)(\/*)/g;
		xml = xml.replace(reg, '$1\r\n$2$3');
		var pad = 0;
		jQuery.each(xml.split('\r\n'), function (index, node) {
			var indent = 0;
			if      (node.match(/.+<\/\w[^>]*>$/))    { indent = 0; }
			else if (node.match(/^<\/\w/))             { if (pad !== 0) pad -= 1; }
			else if (node.match(/^<\w[^>]*[^\/]>.*$/)) { indent = 1; }
			else                                        { indent = 0; }
			var padding = '';
			for (var i = 0; i < pad; i++) padding += '  ';
			formatted += padding + node + '\r\n';
			pad += indent;
		});
		return formatted;
	}

	// -------------------------------------------------------------------------
	// Postgres showplan functions
	// -------------------------------------------------------------------------
	window.pgShowplanGetSql = function (planText, setFields) {
		var sqlText = undefined;
		var queryId = undefined;

		planText = planText.trim();
		planText = planText.replace(/^duration: .* ms\s+plan:$/m, '').trim();

		try {
			var json = JSON.parse(planText);
			if (json.hasOwnProperty('Query Text'))       sqlText = json['Query Text'];
			if (json.hasOwnProperty('Query Identifier')) queryId = json['Query Identifier'];
		} catch (e) { /* continue */ }

		if (sqlText === undefined) {
			if (/^Query Text: /.test(planText)) {
				planText = planText.replace(/^Query Text: /, '').trim();
				var endPos = planText.search(/^$/m);
				sqlText = planText.substring(0, endPos).trim();
			}
			var startPos = planText.search(/Query Identifier: /m);
			if (startPos !== -1) queryId = planText.substring(startPos + 'Query Identifier: '.length).trim();
		}

		if (sqlText === undefined)
			sqlText = "-- No 'Query Text' was found in the plan. The SQL Text *may* be available in the above Plan, under 'Query' tab.";

		if (setFields) {
			$('#dbx-view-pgShowplan-sqlContent').text(sqlText);
			$('#dbx-view-pgShowplan-objectName').text(queryId === undefined ? '--Unknown Query Identifier--' : queryId);
		}
		return sqlText;
	};

	window.pgShowplanFormatSql = function () {
		var formatOptions = { language: 'postgresql', tabWidth: 4, keywordCase: 'upper', tabulateAlias: true };
		var sqlText = $('#dbx-view-pgShowplan-sqlContent').text();
		try {
			$('#dbx-view-pgShowplan-sqlContent').text(sqlFormatter.format(sqlText, formatOptions));
			Prism.highlightAll();
		} catch (err) { alert(err); }
	};

	window.pgShowplanOpenExternal = function () {
		var planText = $('#dbx-view-pgShowplan-planContent').text();
		var toUrl = '/showplan/postgres';
		if (!window.location.toString().toLowerCase().startsWith('http')) toUrl = 'http://dbxtune.gorans.org' + toUrl;
		submit_post_via_hidden_form(toUrl, { plan: planText });
	};

	window.pgShowplanSaveToFile = function () {
		var planText  = $('#dbx-view-pgShowplan-planContent').text();
		var planName  = $('#dbx-view-pgShowplan-objectName').text() || 'name';
		var link = document.createElement('a');
		link.href = URL.createObjectURL(new Blob([planText], { type: 'text/plain' }));
		link.download = 'pg_execution_plan_' + planName + '.pgplan';
		link.click();
		URL.revokeObjectURL(link.href);
	};

	window.pgShowplanCopySql = function () {
		var txt = $('#dbx-view-pgShowplan-sqlContent').text();
		var ta = document.createElement('textarea'); ta.value = txt; document.body.appendChild(ta); ta.select();
		try { document.execCommand('copy'); } catch (err) { alert('Unable to copy\n\n' + err); }
		document.body.removeChild(ta);
	};

	window.pgShowplanCopyPlan = function () {
		var txt = $('#dbx-view-pgShowplan-planContent').text();
		var ta = document.createElement('textarea'); ta.value = txt; document.body.appendChild(ta); ta.select();
		try { document.execCommand('copy'); } catch (err) { alert('Unable to copy\n\n' + err); }
		document.body.removeChild(ta);
	};

	// -------------------------------------------------------------------------
	// SQL Server showplan functions
	// -------------------------------------------------------------------------
	window.ssShowplanEnableZoom = function () {
		if (_ssShowplanZoom !== undefined) return;
		var elem = document.getElementById('dbx-view-ssShowplan-content');
		_ssShowplanZoom = Panzoom(elem, { maxScale: 1, minScale: 0.01 });
		elem.addEventListener('wheel', _ssShowplanZoom.zoomWithWheel);
	};

	window.ssShowplanResetZoom = function () {
		if (_ssShowplanZoom !== undefined) _ssShowplanZoom.reset();
	};

	window.ssShowplanFormatSql = function () {
		var formatOptions = { language: 'tsql', tabWidth: 4, keywordCase: 'upper', tabulateAlias: true };
		var sqlText = $('#dbx-view-ssShowplan-sqlContent').text();
		try {
			$('#dbx-view-ssShowplan-sqlContent').text(sqlFormatter.format(sqlText, formatOptions));
			Prism.highlightAll();
		} catch (err) { alert(err); }
	};

	window.ssShowplanOpenExternal = function () {
		var planText = $('#dbx-view-ssShowplan-xmlContent').text();
		var toUrl = '/showplan/sqlserver';
		if (!window.location.toString().toLowerCase().startsWith('http')) toUrl = 'http://dbxtune.gorans.org' + toUrl;
		submit_post_via_hidden_form(toUrl, { plan: planText });
	};

	window.ssShowplanSaveXmlToFile = function () {
		var xmlText  = $('#dbx-view-ssShowplan-xmlContent').text();
		var planName = $('#dbx-view-ssShowplan-objectName').text();
		planName = planName ? 'for_' + planName.replace(', ', '__') : 'name';
		var link = document.createElement('a');
		link.href = URL.createObjectURL(new Blob([xmlText], { type: 'text/plain' }));
		link.download = 'showplan_' + planName + '.xml.sqlplan';
		link.click();
		URL.revokeObjectURL(link.href);
	};

	window.ssShowplanCopyXml = function () {
		var txt = $('#dbx-view-ssShowplan-xmlContent').text();
		var ta = document.createElement('textarea'); ta.value = txt; document.body.appendChild(ta); ta.select();
		try { document.execCommand('copy'); } catch (err) { alert('Unable to copy\n\n' + err); }
		document.body.removeChild(ta);
	};

	window.ssShowplanGetSql = function (fallbackSqlText) {
		var xmlText = $('#dbx-view-ssShowplan-xmlContent').text();
		var tmpSql  = '-- No SQL StatementText was found in the XML Plan. The below SQL is from column \'lastKnownSql\':\n' + fallbackSqlText;
		$('#dbx-view-ssShowplan-sqlContent').text(tmpSql);

		var xmlDoc           = $.parseXML(xmlText);
		var sqlTextArr       = [];
		var queryHashArr     = [];
		var queryPlanHashArr = [];
		$(xmlDoc).find('StmtSimple').each(function (i, e) {
			sqlTextArr      .push($(e).attr('StatementText'));
			queryHashArr    .push($(e).attr('QueryHash'));
			queryPlanHashArr.push($(e).attr('QueryPlanHash'));
		});

		var sqlText = '';
		if (sqlTextArr.length === 1) {
			sqlText = sqlTextArr[0];
		} else {
			for (var i = 0; i < sqlTextArr.length; i++) {
				sqlText += '--===================================================\n-- SQL Statement ' + (i + 1) + ' of ' + sqlTextArr.length + '\n-----------------------------------------------------\n';
				sqlText += sqlTextArr[i] + '\n-- end ----------------------------------------------\n\n';
			}
		}

		if (queryPlanHashArr.length > 0 && $('#dbx-view-ssShowplan-objectName').text() === '') {
			$('#dbx-view-ssShowplan-objectName').text('QueryHash=' + queryHashArr[0] + ', QueryPlanHash=' + queryPlanHashArr[0]);
		}

		return sqlText;
	};

	window.ssShowplanCopySql = function () {
		var txt = $('#dbx-view-ssShowplan-sqlContent').text();
		var ta = document.createElement('textarea'); ta.value = txt; document.body.appendChild(ta); ta.select();
		try { document.execCommand('copy'); } catch (err) { alert('Unable to copy\n\n' + err); }
		document.body.removeChild(ta);
	};

	window.ssShowplanGetParameters = function () {
		var xmlText = $('#dbx-view-ssShowplan-xmlContent').text();
		var xmlDoc  = $.parseXML(xmlText);
		var compileArr = [], runtimeArr = [];

		$(xmlDoc).find('ParameterList').find('ColumnReference').each(function (i, e) {
			if (e.hasAttribute('ParameterCompiledValue'))
				compileArr.push('CompiledValue: Parameter="' + e.getAttribute('Column') + '" Value="' + e.getAttribute('ParameterCompiledValue') + '" DataType="' + e.getAttribute('ParameterDataType') + '"');
			if (e.hasAttribute('ParameterRuntimeValue'))
				runtimeArr.push('RuntimeValue: Parameter="' + e.getAttribute('Column') + '" Value="' + e.getAttribute('ParameterRuntimeValue') + '" DataType="' + e.getAttribute('ParameterDataType') + '"');
		});
		compileArr = compileArr.reverse();
		runtimeArr = runtimeArr.reverse();

		var $paramSect  = $('#dbx-ssp-sect-params');
		var $noParams   = $('#dbx-ssp-no-params');
		var hasAny      = compileArr.length > 0 || runtimeArr.length > 0;

		if (hasAny) {
			$noParams.hide();

			if (compileArr.length > 0) {
				$('#dbx-view-ssShowplan-compileParameterValues').text(compileArr.join('\n'));
				$('#dbx-view-ssShowplan-compileParameterValuesButton').show();
				$('#dbx-ssp-compile-block').show();
			} else {
				$('#dbx-ssp-compile-block').hide();
			}

			if (runtimeArr.length > 0) {
				$('#dbx-view-ssShowplan-runtimeParameterValues').text(runtimeArr.join('\n'));
				$('#dbx-view-ssShowplan-runtimeParameterValuesButton').show();
				$('#dbx-ssp-runtime-block').show();
			} else {
				$('#dbx-ssp-runtime-block').hide();
			}
		} else {
			$('#dbx-ssp-compile-block').hide();
			$('#dbx-ssp-runtime-block').hide();
			$noParams.show();
		}

		// Always show the Parameters section; collapse it so the user opens on demand
		$paramSect.show();
		$paramSect[0].open = false;
	};

	window.ssShowplanSetParametersInSql = function (paramType) {
		var xmlText = $('#dbx-view-ssShowplan-xmlContent').text();
		var xmlDoc  = $.parseXML(xmlText);
		$(xmlDoc).find('ParameterList').find('ColumnReference').each(function (i, e) {
			var paramName  = e.getAttribute('Column');
			var paramValue = '-unknown-';
			if (paramType === 'compile' && e.hasAttribute('ParameterCompiledValue')) paramValue = e.getAttribute('ParameterCompiledValue');
			if (paramType === 'runtime' && e.hasAttribute('ParameterRuntimeValue'))  paramValue = e.getAttribute('ParameterRuntimeValue');
			var sqlText = $('#dbx-view-ssShowplan-sqlContent').text();
			$('#dbx-view-ssShowplan-sqlContent').text(sqlText.replaceAll(paramName, paramValue));
			Prism.highlightAll();
		});
	};

	// -------------------------------------------------------------------------
	// Event handlers
	// -------------------------------------------------------------------------
	function _initHandlers() {

		// Postgres: set fields before modal becomes visible
		$('#dbx-view-pgShowplan-dialog').on('show.bs.modal', function (e) {
			var data = $(e.relatedTarget).data();
			$('#dbx-view-pgShowplan-objectName',  this).text(data.objectname);
			$('#dbx-view-pgShowplan-planContent', this).text(data.tooltip);
			pgShowplanGetSql(data.tooltip, true);
		});

		// Postgres: mount PEV2 Vue component after modal is visible
		$('#dbx-view-pgShowplan-dialog').on('shown.bs.modal', function (e) {
			var data = $(e.relatedTarget).data();
			var container = document.getElementById('dbx-view-pgShowplan-content');
			container.innerHTML = "<pev2 style='min-height: 55vh;' :plan-source='plan' :plan-query='query' />";
			var app = Vue.createApp({ data: function () { return { plan: data.tooltip, query: '' }; } });
			app.component('pev2', pev2.Plan);
			pev2app = app;
			app.mount('#dbx-view-pgShowplan-content');
			Prism.highlightAll();
		});

		// Postgres: unmount PEV2 when modal closes
		$('#dbx-view-pgShowplan-dialog').on('hidden.bs.modal', function () {
			if (pev2app) { pev2app.unmount(); pev2app = undefined; }
		});

		// SQL Server: set fields before modal becomes visible
		$('#dbx-view-ssShowplan-dialog').on('show.bs.modal', function (e) {
			// When opened programmatically (showSqlServerShowplanDialog) relatedTarget
			// is undefined — fields are already populated there, so skip.
			if (!e.relatedTarget) return;
			var data = $(e.relatedTarget).data();
			$('#dbx-view-ssShowplan-objectName', this).text(data.objectname);
			$('#dbx-view-ssShowplan-content',    this).text('Loading plan...');
			$('#dbx-view-ssShowplan-analysis',   this).hide();
			$('#dbx-view-ssShowplan-xmlContent', this).text(formatXml(data.tooltip));
			$('#dbx-view-ssShowplan-sqlContent', this).text(ssShowplanGetSql(data.sqltext));
			ssShowplanSetPlanType(data.tooltip);
			ssShowplanGetParameters();
			ssShowplanResetZoom();
		});

		// SQL Server: draw plan after modal is visible
		$('#dbx-view-ssShowplan-dialog').on('shown.bs.modal', function (e) {
			var $modal = $(this);
			var $dlg   = $modal.find('.modal-dialog');
			var $cont  = $modal.find('.modal-content');

			// ── Restore or default size ───────────────────────────────────────
			var savedW = null, savedH = null, savedL = null, savedT = null;
			try {
				savedW = localStorage.getItem('ssShowplan-dlg-width');
				savedH = localStorage.getItem('ssShowplan-dlg-height');
				savedL = localStorage.getItem('ssShowplan-dlg-left');
				savedT = localStorage.getItem('ssShowplan-dlg-top');
			} catch(ex) {}
			var w = savedW ? parseInt(savedW) : Math.round(window.innerWidth  * 0.90);
			var h = savedH ? parseInt(savedH) : Math.round(window.innerHeight * 0.88);
			$cont.css({ width: w + 'px', height: h + 'px' });

			// ── Switch from Bootstrap centering to absolute positioning ───────
			$dlg.removeClass('modal-dialog-centered');
			$dlg.css({ margin: '0', 'max-width': 'none', position: 'absolute' });
			$modal.css({ overflow: 'hidden' });

			var l = savedL ? parseInt(savedL) : Math.round((window.innerWidth  - w) / 2);
			var t = savedT ? parseInt(savedT) : Math.round((window.innerHeight - h) / 2);
			// Clamp so dialog is never completely off-screen
			l = Math.max(0, Math.min(l, window.innerWidth  - 120));
			t = Math.max(0, Math.min(t, window.innerHeight -  60));
			$dlg.css({ left: l + 'px', top: t + 'px' });

			// ── Draggable (set up once) ────────────────────────────────────────
			if ($.fn.draggable && !$dlg.hasClass('ui-draggable')) {
				$dlg.draggable({
					handle: '.modal-header',
					stop: function(ev, ui) {
						try {
							localStorage.setItem('ssShowplan-dlg-left', Math.round(ui.position.left));
							localStorage.setItem('ssShowplan-dlg-top',  Math.round(ui.position.top));
						} catch(ex) {}
					}
				});
			}

			// ── Resizable (set up once) ───────────────────────────────────────
			if ($.fn.resizable && !$cont.hasClass('ui-resizable')) {
				$cont.resizable({
					handles:   'all',
					minWidth:  500,
					minHeight: 300,
					stop: function(ev, ui) {
						try {
							localStorage.setItem('ssShowplan-dlg-width',  Math.round(ui.size.width));
							localStorage.setItem('ssShowplan-dlg-height', Math.round(ui.size.height));
						} catch(ex) {}
					}
				});
			}

			// ── Draw plan (data-toggle path only) ────────────────────────────
			if (!e.relatedTarget) return;
			var data = $(e.relatedTarget).data();
			try { QP.showPlan(document.getElementById('dbx-view-ssShowplan-content'), data.tooltip); } catch(ex) {}
			if (typeof Prism !== 'undefined') Prism.highlightAll();
			ssShowplanRunAnalysis(data.tooltip);
		});

		// Text viewer dialog — populate from the clicked trigger's data-tooltip attribute
		// (DSR generates: data-toggle="modal" data-target="#dbx-view-sqltext-dialog" data-tooltip="...")
		$(document).on('show.bs.modal', '#dbx-view-sqltext-dialog', function(e) {
			var $trigger = $(e.relatedTarget);
			var title    = $trigger.attr('title') || 'Text';
			var text     = $trigger.attr('data-tooltip') || '';
			$('#dbx-sqltext-dlg-title').text(title);
			$('#dbx-sqltext-dlg-content').text(text);
		});

		// Table Information: lazy-load on first expand
		document.getElementById('dbx-ssp-sect-tableinfo').addEventListener('toggle', function() {
			if (!this.open) return;                          // closing — do nothing
			var body = document.getElementById('dbx-ssp-tableinfo-body');
			if (!body || body.getAttribute('data-loaded') === 'true') return;  // already loaded

			var srv     = body.getAttribute('data-srv')     || '';
			var dbname  = body.getAttribute('data-dbname')  || '';
			var ts      = body.getAttribute('data-ts')      || '';
			var sqlText = body.getAttribute('data-sqltext')  || '';

			if (!srv || !dbname) {
				body.innerHTML = '<em style="color:#888;">Table information is not available — no server context.</em>';
				body.setAttribute('data-loaded', 'true');
				return;
			}

			// Parse table names client-side (node-sql-parser w/ T-SQL AST; falls back to tokenizer)
			body.innerHTML = '<span style="color:#888;font-size:0.85em;">&#9203; Parsing SQL…</span>';

			_extractTablesAsync(sqlText, function(tables) {
				if (!tables.length) {
					body.setAttribute('data-loaded', 'true');
					body.innerHTML = '<em style="color:#888;">No tables could be parsed from the SQL text.</em>';
					return;
				}

				body.innerHTML = '<span style="color:#888;font-size:0.85em;">&#9203; Loading table information…</span>';

				$.ajax({
					url:      '/api/cc/mgt/query-store',
					data:     { srv: srv, action: 'tableInfo', dbname: dbname, tables: tables.join(','), ts: ts },
					dataType: 'json',
					success:  function(r) {
						body.setAttribute('data-loaded', 'true');
						if (r && r.html) {
							var parsedMsg = '<div style="font-size:0.8em;color:#555;margin-bottom:6px;">Tables: '
								+ tables.map(function(t) { return '<code>' + escapeHtml(t) + '</code>'; }).join(', ')
								+ '</div>';
							body.innerHTML = parsedMsg + r.html;
						} else {
							body.innerHTML = '<em style="color:#888;">No table information found in DDL Storage.</em>';
						}
					},
					error:    function(xhr) {
						body.setAttribute('data-loaded', 'true');
						var msg = 'HTTP ' + xhr.status;
						try { var j = JSON.parse(xhr.responseText); msg += ': ' + (j.message || j.error || ''); } catch(e) {}
						body.innerHTML = '<span class="text-danger">Failed to load table info: ' + msg + '</span>';
					}
				});
			});
		});
	}

	// -------------------------------------------------------------------------
	// Programmatic entry point — called from Query Store "Show Plan" button
	// -------------------------------------------------------------------------
	/**
	 * Open the SQL Server execution plan dialog programmatically.
	 * Unlike the data-toggle="modal" path (which reads from e.relatedTarget),
	 * this sets the dialog content directly and then shows the modal.
	 *
	 * @param {string} xmlText      — raw XML showplan string
	 * @param {string} [sqlText]    — optional SQL text to display on the SQL tab
	 * @param {string} [objectName] — optional object/query label shown in the header
	 * @param {Object} [meta]       — optional metadata: { lastCompileStartTime, lastSeen, source, srv, dbname, ts }
	 *                                meta.srv / meta.dbname / meta.ts are used for lazy-loading Table Information.
	 */
	window.showSqlServerShowplanDialog = function(xmlText, sqlText, objectName, meta) {
		var $dlg = $('#dbx-view-ssShowplan-dialog');
		if (!$dlg.length) {
			// Dialog not yet injected — inject now and retry after a tick
			_injectHtml();
			_initHandlers();
			setTimeout(function() { window.showSqlServerShowplanDialog(xmlText, sqlText, objectName, meta); }, 50);
			return;
		}
		if (!xmlText) return;

		// Populate fields (mirrors what show.bs.modal handler does for relatedTarget)
		$('#dbx-view-ssShowplan-objectName', $dlg).text(objectName || '');

		// Timestamps subtitle (compiled / last seen) — only when meta is supplied
		var $ts = $('#dbx-view-ssShowplan-timestamps', $dlg);
		if (meta && (meta.lastCompileStartTime || meta.lastSeen)) {
			var tsParts = [];
			if (meta.lastCompileStartTime) tsParts.push('Compiled: ' + meta.lastCompileStartTime);
			if (meta.lastSeen)             tsParts.push('Last seen: ' + meta.lastSeen);
			$ts.text(tsParts.join('  \u2502  '));
		} else {
			$ts.text('');
		}

		$('#dbx-view-ssShowplan-content',    $dlg).text('Loading plan...');
		$('#dbx-view-ssShowplan-analysis',   $dlg).hide();
		$('#dbx-view-ssShowplan-xmlContent', $dlg).text(formatXml(xmlText));
		$('#dbx-view-ssShowplan-sqlContent', $dlg).text(ssShowplanGetSql(sqlText || ''));
		ssShowplanSetPlanType(xmlText);
		ssShowplanGetParameters();
		ssShowplanResetZoom();

		// Reset Table Information section — store context for lazy loading on first expand
		var tiSect = document.getElementById('dbx-ssp-sect-tableinfo');
		var tiBody = document.getElementById('dbx-ssp-tableinfo-body');
		if (tiSect && tiBody) {
			var tiSrv    = (meta && meta.srv)    ? meta.srv    : '';
			var tiDb     = (meta && meta.dbname) ? meta.dbname : '';
			var tiTs     = (meta && meta.ts)     ? meta.ts     : '';
			var tiSql    = sqlText || '';
			tiBody.setAttribute('data-srv',     tiSrv);
			tiBody.setAttribute('data-dbname',  tiDb);
			tiBody.setAttribute('data-ts',      tiTs);
			tiBody.setAttribute('data-sqltext', tiSql);
			tiBody.setAttribute('data-loaded',  'false');
			tiBody.innerHTML = '<span style="color:#888;font-size:0.85em;">&#9203; Loading table information…</span>';
			// Show or hide the section depending on whether we have a server context
			if (tiSrv && tiDb) {
				tiSect.style.display = '';
			} else {
				tiSect.style.display = 'none';
			}
			// Collapse it so the user opens it on demand
			tiSect.removeAttribute('open');
		}

		// Draw the plan + run analysis once the modal is fully visible.
		// If the dialog is already open (hasClass('show')), Bootstrap's modal('show')
		// is a no-op and shown.bs.modal will never fire — so draw in-place immediately.
		function _drawAndAnalyze() {
			try { QP.showPlan(document.getElementById('dbx-view-ssShowplan-content'), xmlText); } catch(ex) {}
			if (typeof Prism !== 'undefined') Prism.highlightAll();
			ssShowplanRunAnalysis(xmlText);
		}

		if ($dlg.hasClass('show')) {
			_drawAndAnalyze();   // already visible — update in-place
		} else {
			$dlg.one('shown.bs.modal', _drawAndAnalyze);
			$dlg.modal('show');
		}
	};

	/**
	 * Detect plan type from XML text.
	 * Returns 'actual', 'live', 'estimated', or null.
	 *
	 * 'actual'    — completed plan: has runtime counters AND at least one completion signal
	 *               (WaitStats present, or root RelOp ActualEndOfScans="1").
	 * 'live'      — from dm_exec_query_statistics_xml(): has runtime counters but none of
	 *               the above completion signals (query still executing, counters partial).
	 * 'estimated' — no RunTimeCountersPerThread at all (cached/estimated plan).
	 */
	function _ssPlanType(xmlText) {
		if (!xmlText) return null;
		try {
			var hasRuntime = /RunTimeCountersPerThread/i.test(xmlText);

			if (!hasRuntime) {
				if (/ShowPlanXMLForQuery/i.test(xmlText)) return 'actual';   // shouldn't happen, safety net
				if (/ShowPlanXML/i.test(xmlText))         return 'estimated';
				return null;
			}

			// Has runtime counters — check for completion signals.

			// Signal 1: WaitStats block is only written after the statement completes.
			if (/WaitStats/i.test(xmlText)) return 'actual';

			// Signal 2: Root operator (NodeId="0") ActualEndOfScans="1" — requires a parse.
			try {
				var doc2 = new DOMParser().parseFromString(xmlText, 'text/xml');
				if (!doc2.querySelector('parsererror')) {
					// Find the root RelOp by NodeId attribute
					var relOps = doc2.querySelectorAll('RelOp');
					for (var i = 0; i < relOps.length; i++) {
						if (relOps[i].getAttribute('NodeId') === '0') {
							var rtc = relOps[i].querySelector('RunTimeCountersPerThread');
							if (rtc && rtc.getAttribute('ActualEndOfScans') === '1') return 'actual';
							break;
						}
					}
				}
			} catch(e2) {}

			// Has runtime counters but no completion signal → live (still executing).
			return 'live';
		} catch(e) {}
		return null;
	}

	/** Set the plan type label in the modal header. */
	window.ssShowplanSetPlanType = function(xmlText) {
		var el = document.getElementById('dbx-view-ssShowplan-plantype');
		if (!el) return;
		var t = _ssPlanType(xmlText);
		if (t === 'actual') {
			el.innerHTML = '<span style="background:#d1fae5;color:#065f46;border:1px solid #6ee7b7;border-radius:3px;padding:1px 6px;font-size:0.82em;" title="Completed actual execution plan">&#10003; Actual</span>';
		} else if (t === 'live') {
			el.innerHTML = '<span style="background:#fff3cd;color:#7c4a00;border:1px solid #ffc107;border-radius:3px;padding:1px 6px;font-size:0.82em;"'
			             + ' title="Live plan from dm_exec_query_statistics_xml \u2014 query is still executing, row counts and timing are incomplete">&#9201; Live</span>'
			             + ' <span style="font-size:0.78em;color:#b45309;font-style:italic;">counters incomplete</span>';
		} else if (t === 'estimated') {
			el.innerHTML = '<span style="background:#fef9c3;color:#713f12;border:1px solid #fde047;border-radius:3px;padding:1px 6px;font-size:0.82em;" title="Estimated (no actual runtime data)">Estimated</span>';
		} else {
			el.innerHTML = '';
		}
	};

	/**
	 * Run DbxShowplanAnalyzer on the given XML and render findings into
	 * #dbx-view-ssShowplan-analysis.  Also renders Runtime Stats and Wait Stats.
	 */
	window.ssShowplanRunAnalysis = function(xmlText) {
		var details  = document.getElementById('dbx-view-ssShowplan-analysis');
		var summary  = document.getElementById('dbx-view-ssShowplan-analysis-summary');
		var bodyEl   = document.getElementById('dbx-view-ssShowplan-analysis-body');
		if (!details || !summary || !bodyEl) return;

		// Destroy any previous wait chart before wiping innerHTML
		if (_sspWaitChart) { try { _sspWaitChart.destroy(); } catch(e){} _sspWaitChart = null; }

		var isDark    = window._colorSchema === 'dark';
		var lblColor  = isDark ? '#aaa'  : '#666';
		var valColor  = isDark ? '#e0e0e0' : '#222';
		var hdrColor  = isDark ? '#999'  : '#888';
		var sepColor  = isDark ? '#333'  : '#e0e0e0';

		// ── Time formatter: "Xh Xm Xs Xms" ──────────────────────────────────
		function fmtHMS(ms) {
			if (!ms || ms <= 0) return '0 ms';
			var h = Math.floor(ms / 3600000);
			var m = Math.floor((ms % 3600000) / 60000);
			var s = Math.floor((ms % 60000) / 1000);
			var r = ms % 1000;
			var parts = [];
			if (h > 0) parts.push(h + 'h');
			if (m > 0) parts.push(m + 'm');
			if (s > 0) parts.push(s + 's');
			if (r > 0 || parts.length === 0) parts.push(r + 'ms');
			return parts.join(' ');
		}
		function fmtKB(kb) {
			if (kb >= 1048576) return (kb / 1048576).toFixed(1) + ' GB';
			if (kb >= 1024)    return (kb / 1024).toFixed(1) + ' MB';
			return kb + ' KB';
		}

		// ── Parse XML for runtime stats + wait stats ──────────────────────────
		var statsHtml = '', waitHtml = '', waitData = [];
		var xmlSrc = xmlText || ($('#dbx-view-ssShowplan-xmlContent')[0] || {}).textContent || '';
		var planType = _ssPlanType(xmlSrc);   // 'actual' | 'live' | 'estimated' | null
		var doc = null;
		if (xmlSrc) {
			try {
				doc = new DOMParser().parseFromString(xmlSrc, 'text/xml');
				if (doc.querySelector('parsererror')) doc = null;
			} catch(e) { doc = null; }
		}

		if (doc) {
			// ── Runtime Stats grid ────────────────────────────────────────────
			var stmt  = doc.querySelector('StmtSimple');
			var qp    = doc.querySelector('QueryPlan');
			var qt    = doc.querySelector('QueryTimeStats');
			var mg    = doc.querySelector('MemoryGrantInfo');

			var cost       = stmt ? parseFloat(stmt.getAttribute('StatementSubTreeCost') || '0') : 0;
			var optLevel   = stmt ? (stmt.getAttribute('StatementOptmLevel') || '') : '';
			// NonParallelPlanReason lives on QueryPlan (not StmtSimple) in modern plan XML
			var serialCode = (qp   ? (qp.getAttribute('NonParallelPlanReason')   || '') : '')
			              || (stmt ? (stmt.getAttribute('NonParallelPlanReason')  || '') : '');
			// CardinalityEstimationModelVersion lives on StmtSimple (not QueryPlan)
			var ceModel    = (stmt ? (stmt.getAttribute('CardinalityEstimationModelVersion') || '') : '')
			              || (qp   ? (qp.getAttribute('CardinalityEstimationModelVersion')   || '') : '');
			var _serialLabels = {
				'MaxDOPSetToOne':                                                 'MAXDOP 1 (server/DB/RG)',
				'QueryHintNoParallelSet':                                         'OPTION (MAXDOP 1) hint',
				'EstimatedDOPIsOne':                                              'Optimizer chose serial',
				'TSQLUserDefinedFunctionsNotParallelizable':                       'T-SQL scalar UDF',
				'CouldNotGenerateValidParallelPlan':                              'No valid parallel plan',
				'ParallelismDisabledByTraceFlag':                                 'Trace flag disabled parallelism',
				'NoParallelPlansInDesktopOrExpressEdition':                       'Express/Desktop edition',
				'TableVariableTransactionsDoNotSupportParallelNestedTransaction':  'Table variable modification',
				'DMLQueryReturnsOutputToClient':                                  'DML + OUTPUT to client',
				'NoParallelForMemoryOptimizedTables':                             'Memory-optimized table',
				'NoParallelWithRemoteQuery':                                      'Remote/linked-server query',
				'CLRUserDefinedFunctionRequiresDataAccess':                       'CLR UDF with data access',
				'NonParallelizableIntrinsicFunction':                             'Non-parallelizable function',
				'UpdatingWritebackVariable':                                      'Writing to local variable',
				'NoParallelForNativelyCompiledModule':                            'Natively compiled module'
			};
			var serial = serialCode ? (_serialLabels[serialCode] || serialCode) : '';
			// ElapsedTime / CpuTime (plan schema v1.5+) — older schemas used ElapsedTimeMs / CpuTimeMs
			var elapsed    = qt ? (parseInt(qt.getAttribute('ElapsedTime')   || qt.getAttribute('ElapsedTimeMs') || '0', 10)) : 0;
			var cpu        = qt ? (parseInt(qt.getAttribute('CpuTime')       || qt.getAttribute('CpuTimeMs')     || '0', 10)) : 0;
			var dop        = qp ? parseInt(qp.getAttribute('DegreeOfParallelism') || '-1', 10) : -1;
			var compileCPU = qp ? parseInt(qp.getAttribute('CompileCPU') || '0', 10) : 0;
			var granted    = mg ? parseInt(mg.getAttribute('GrantedMemory')  || '0', 10) : 0;
			var used       = mg ? parseInt(mg.getAttribute('MaxUsedMemory')  || '0', 10) : 0;

			var rows = [];
			if (cost > 0)         rows.push(['Cost',        cost.toLocaleString(undefined, {minimumFractionDigits:2, maximumFractionDigits:2})]);
			if (elapsed > 0)      rows.push(['Elapsed',     fmtHMS(elapsed)]);
			if (cpu > 0)          rows.push(['CPU',         fmtHMS(cpu)]);
			// DOP: show when set and != 1 (DOP 0 means "serial forced by MaxDOP setting")
			if (dop >= 0 && dop !== 1) rows.push(['DOP',   dop === 0 ? '0 (serial)' : dop.toString()]);
			if (serial)           rows.push(['Serial',      serial]);
			if (granted > 0)      rows.push(['Memory',      fmtKB(granted) + ' granted']);
			if (used > 0) {
				var usedPct = granted > 0 ? ' (' + Math.round(used / granted * 100) + '%)' : '';
				rows.push(['Used', fmtKB(used) + usedPct]);
			}
			if (optLevel)         rows.push(['Optimization', optLevel]);
			if (ceModel)          rows.push(['CE Model',    ceModel]);
			if (compileCPU > 100) rows.push(['Compile CPU', fmtHMS(compileCPU)]);

			if (rows.length > 0) {
				var sumStyle = 'cursor:pointer;font-size:0.75em;font-weight:700;color:' + hdrColor + ';text-transform:uppercase;letter-spacing:0.05em;user-select:none;padding:1px 0;';
				var liveNotice = planType === 'live'
					? ' <span style="font-size:0.9em;font-weight:400;color:#b45309;text-transform:none;letter-spacing:normal;"'
					+ ' title="Plan collected from dm_exec_query_statistics_xml \u2014 query is still executing, counters are partial">&#9201; live, partial</span>'
					: '';
				statsHtml = '<details open style="margin-bottom:6px;">'
				          + '<summary style="' + sumStyle + '">Runtime' + liveNotice + '</summary>'
				          + '<div style="padding:3px 0 2px 10px;border-left:2px solid ' + sepColor + ';margin-top:3px;">'
				          + '<table style="border-collapse:collapse;font-size:0.82em;">';
				for (var ri = 0; ri < rows.length; ri += 2) {
					statsHtml += '<tr>'
					           + '<td style="padding:1px 6px 1px 0;color:' + lblColor + ';white-space:nowrap;">' + escapeHtml(rows[ri][0]) + '</td>'
					           + '<td style="padding:1px 20px 1px 0;color:' + valColor + ';font-weight:600;">'  + escapeHtml(rows[ri][1]) + '</td>';
					if (ri + 1 < rows.length) {
						statsHtml += '<td style="padding:1px 6px 1px 0;color:' + lblColor + ';white-space:nowrap;">' + escapeHtml(rows[ri+1][0]) + '</td>'
						           + '<td style="padding:1px 0;color:' + valColor + ';font-weight:600;">'            + escapeHtml(rows[ri+1][1]) + '</td>';
					}
					statsHtml += '</tr>';
				}
				statsHtml += '</table></div></details>';
			}

			// ── Wait Stats chart ──────────────────────────────────────────────
			var wsEl = doc.querySelector('WaitStats');
			if (wsEl) {
				[].forEach.call(wsEl.querySelectorAll('Wait'), function(w) {
					var ms = parseInt(w.getAttribute('WaitTimeMs') || '0', 10);
					if (ms > 0) waitData.push({ type: w.getAttribute('WaitType') || '?', ms: ms });
				});
			}
			waitData.sort(function(a, b) { return b.ms - a.ms; });

			if (waitData.length > 0) {
				var totalWaitMs = waitData.reduce(function(s, w) { return s + w.ms; }, 0);
				var chartH = Math.min(180, waitData.length * 26 + 36);
				var sumStyle2 = 'cursor:pointer;font-size:0.75em;font-weight:700;color:' + hdrColor + ';text-transform:uppercase;letter-spacing:0.05em;user-select:none;padding:1px 0;';
				waitHtml = '<details open style="margin-bottom:6px;">'
				         + '<summary style="' + sumStyle2 + '">'
				         + 'Wait Statistics <span style="font-weight:400;font-size:0.95em;color:' + lblColor + ';text-transform:none;letter-spacing:normal;">' + fmtHMS(totalWaitMs) + ' total</span></summary>'
				         + '<div style="padding:3px 0 2px 10px;border-left:2px solid ' + sepColor + ';margin-top:3px;">'
				         + '<div style="position:relative;height:' + chartH + 'px;max-width:520px;">'
				         + '<canvas id="dbx-ssp-wait-chart"></canvas>'
				         + '</div></div></details>';
			}
		}

		// ── Analyzer findings ─────────────────────────────────────────────────
		var findings = [];
		try {
			if (!window.DbxShowplanAnalyzer) {
				details.style.display = 'none';
				return;
			}
			findings = window.DbxShowplanAnalyzer.analyze(xmlSrc) || [];
		} catch(ex) {
			summary.innerHTML = '&#9888; <b>Plan Analysis &mdash; error</b>';
			bodyEl.innerHTML = statsHtml + '<div style="color:#b71c1c;font-size:0.85em;white-space:pre-wrap;">' + escapeHtml(String(ex)) + '</div>';
			details.style.display = '';
			details.open = true;
			return;
		}

		// Summary line
		if (findings.length === 0 && !statsHtml) {
			summary.innerHTML = '&#10003; <b>Plan Analysis</b> <span style="font-weight:normal;color:#555;">&mdash; no issues detected</span>';
			bodyEl.innerHTML = '';
			details.style.display = '';
			details.open = false;
			return;
		}

		var nErr  = findings.filter(function(f){return f.severity==='error';}).length;
		var nWarn = findings.filter(function(f){return f.severity==='warning';}).length;
		var parts = [];
		if (nErr)  parts.push('<span style="color:#b71c1c;">' + nErr  + ' error'   + (nErr  > 1 ? 's' : '') + '</span>');
		if (nWarn) parts.push('<span style="color:#b45309;">' + nWarn + ' warning' + (nWarn > 1 ? 's' : '') + '</span>');
		var infoRest = findings.length - nErr - nWarn;
		if (infoRest > 0) parts.push(infoRest + ' info');
		if (findings.length > 0) {
			summary.innerHTML = '&#9888; <b>Plan Analysis &mdash; ' + findings.length
			                   + ' finding' + (findings.length !== 1 ? 's' : '') + '</b>'
			                   + (parts.length ? ' (' + parts.join(', ') + ')' : '');
		} else {
			summary.innerHTML = '&#10003; <b>Plan Analysis</b> <span style="font-weight:normal;color:#555;">&mdash; no issues detected</span>';
		}

		// Findings HTML
		var sevIcon  = { 'error': '&#10060;', 'warning': '&#9888;', 'info': 'ℹ&#65039;' };
		var sevColor = { 'error': '#b71c1c',  'warning': '#b45309',  'info': '#1565c0' };
		var sevBg    = { 'error': '#fff0f0',  'warning': '#fffbeb',  'info': '#eff6ff' };
		var sevBorder= { 'error': '#f87171',  'warning': '#fbbf24',  'info': '#93c5fd' };
		if (isDark) {
			sevBg     = { 'error': '#3b1111', 'warning': '#2e2000', 'info': '#0d1f3c' };
			sevBorder = { 'error': '#c62828', 'warning': '#b45309', 'info': '#1565c0' };
		}
		var detailColor = isDark ? '#bbb' : '#555';
		var fHtml = '';
		if (findings.length > 0) {
			var fSumStyle = 'cursor:pointer;font-size:0.75em;font-weight:700;color:' + hdrColor + ';text-transform:uppercase;letter-spacing:0.05em;user-select:none;padding:1px 0;';
			var fCountLabel = ' <span style="font-weight:400;font-size:0.95em;color:' + (isDark ? '#aaa' : '#666') + ';text-transform:none;letter-spacing:normal;">'
			                + '(' + findings.length + ')</span>';
			fHtml = '<details open style="margin-bottom:4px;">'
			      + '<summary style="' + fSumStyle + '">Findings' + fCountLabel + '</summary>'
			      + '<div style="padding:3px 0 2px 10px;border-left:2px solid ' + sepColor + ';margin-top:3px;">'
			      + '<div style="display:flex;flex-direction:column;gap:3px;font-size:0.85em;line-height:1.4;">';
			findings.forEach(function(f) {
				var icon   = sevIcon[f.severity]   || 'ℹ';
				var color  = sevColor[f.severity]  || '#333';
				var bg     = sevBg[f.severity]     || '#f8f8f8';
				var border = sevBorder[f.severity] || '#ccc';
				var nodeTag = f.nodeId != null
					? ' <span style="color:#888;font-weight:400;font-size:0.9em;">[Node ' + escapeHtml(String(f.nodeId)) + ']</span>'
					: '';
				var detailHtml = f.detail ? '<div style="color:' + detailColor + ';margin-top:2px;white-space:pre-wrap;">' + escapeHtml(f.detail) + '</div>' : '';
				fHtml += '<div style="background:' + bg + ';border-left:4px solid ' + border + ';padding:3px 8px;border-radius:2px;">'
				       + '<span style="color:' + color + ';font-weight:700;">' + icon + ' [' + escapeHtml(f.category) + '] ' + escapeHtml(f.title) + '</span>'
				       + nodeTag + detailHtml + '</div>';
			});
			fHtml += '</div></div></details>';
		}

		bodyEl.innerHTML = statsHtml + waitHtml + fHtml;
		details.style.display = '';
		details.open = (findings.length > 0 || waitData.length > 0 || statsHtml !== '');

		// ── Render Chart.js wait bar ──────────────────────────────────────────
		if (waitData.length > 0 && typeof Chart !== 'undefined') {
			var cvs = document.getElementById('dbx-ssp-wait-chart');
			if (cvs) {
				var textClr = isDark ? '#ccc' : '#555';
				var gridClr = isDark ? 'rgba(255,255,255,0.08)' : 'rgba(0,0,0,0.08)';
				// Colour bars by wait category
				var barColors = waitData.map(function(w) {
					var t = w.type;
					if (/SOS_SCHEDULER|WORKER/.test(t))              return 'rgba(59,130,246,0.80)';
					if (/MEMORY_ALLOCATION|RESOURCE_SEMAPHORE/.test(t)) return 'rgba(168,85,247,0.80)';
					if (/^LCK_/.test(t))                             return 'rgba(239,68,68,0.80)';
					if (/LATCH/.test(t))                             return 'rgba(249,115,22,0.80)';
					if (/PAGEIO|IO_COMPLETION|WRITELOG/.test(t))     return 'rgba(16,185,129,0.80)';
					if (/NETWORK|ASYNC_NETWORK/.test(t))             return 'rgba(20,184,166,0.80)';
					if (/CXPACKET|CXCONSUMER|CXSYNC/.test(t))        return 'rgba(234,179,8,0.80)';
					return 'rgba(100,116,139,0.75)';
				});
				_sspWaitChart = new Chart(cvs.getContext('2d'), {
					type: 'horizontalBar',
					data: {
						labels: waitData.map(function(w) { return w.type; }),
						datasets: [{
							data: waitData.map(function(w) { return w.ms; }),
							backgroundColor: barColors,
							borderWidth: 0
						}]
					},
					options: {
						responsive: true,
						maintainAspectRatio: false,
						legend: { display: false },
						tooltips: {
							callbacks: {
								label: function(item) { return ' ' + fmtHMS(item.xLabel); }
							}
						},
						scales: {
							xAxes: [{
								ticks: {
									beginAtZero: true,
									fontColor: textClr,
									fontSize: 10,
									callback: function(v) { return fmtHMS(v); }
								},
								gridLines: { color: gridClr }
							}],
							yAxes: [{
								ticks: { fontColor: textClr, fontSize: 10 },
								gridLines: { display: false }
							}]
						}
					}
				});
			}
		}
	};

	// -------------------------------------------------------------------------
	// Showplan Loader — public entry points
	// -------------------------------------------------------------------------

	/** Open the Showplan Loader input dialog (paste/load XML, then calls showSqlServerShowplanDialog). */
	window.openShowplanViewer = function() {
		var $dlg = $('#dbx-showplan-viewer-input-dialog');
		if (!$dlg.length) {
			_injectHtml();
			_initHandlers();
			setTimeout(window.openShowplanViewer, 50);
			return;
		}
		document.getElementById('dbx-spv-error').textContent = '';
		// Mirror the page colour scheme (cs=dark / cs=white)
		if (window._colorSchema === 'dark') $dlg.addClass('spv-dark');
		else                                $dlg.removeClass('spv-dark');
		$dlg.modal('show');
	};

	/** Called by the hidden file input — reads the selected file into the textarea. */
	window.spvLoadFile = function(event) {
		var file = event.target.files[0];
		if (!file) return;
		var reader = new FileReader();
		reader.onload = function(e) {
			document.getElementById('dbx-spv-xml-input').value = e.target.result;
			document.getElementById('dbx-spv-error').textContent = '';
		};
		reader.readAsText(file);
		event.target.value = '';   // reset so the same file can be re-loaded
	};

	/** Validate the pasted/loaded XML and open the plan viewer dialog. */
	window.spvViewPlan = function() {
		var xml   = (document.getElementById('dbx-spv-xml-input').value || '').trim();
		var errEl = document.getElementById('dbx-spv-error');
		errEl.textContent = '';
		if (!xml) {
			errEl.textContent = 'Please paste or load a Showplan XML first.';
			return;
		}
		if (xml.indexOf('<ShowPlanXML') === -1 && xml.indexOf('ShowPlanXMLForQuery') === -1) {
			errEl.textContent = 'Warning: XML does not look like a SQL Server Showplan \u2014 trying anyway.';
			// don't return — let the viewer attempt it
		}
		$('#dbx-showplan-viewer-input-dialog').modal('hide');
		setTimeout(function() {
			showSqlServerShowplanDialog(xml, '', 'Showplan Loader');
		}, 300);   // small delay so input dialog finishes closing first
	};

	// -------------------------------------------------------------------------
	// Bootstrap
	// -------------------------------------------------------------------------
	$(document).ready(function () {
		_injectHtml();
		_initHandlers();
	});

}());
