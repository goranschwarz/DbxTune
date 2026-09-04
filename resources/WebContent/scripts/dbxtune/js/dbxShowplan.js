/**
 * dbxShowplan.js — injectable Postgres + SQL Server + ASE showplan dialogs
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
 *   ssShowplanToggleZoom(), ssShowplanFormatSql(),
 *   ssShowplanCopySql(), ssShowplanCopyXml(), ssShowplanSaveXmlToFile(),
 *   ssShowplanOpenExternal(), ssShowplanGetParameters(),
 *   ssShowplanSetParametersInSql()
 *   showAseShowplanDialog(), aseShowplanFormatSql(), aseShowplanCopySql(),
 *   aseShowplanCopyPlan(), aseShowplanRefreshLlmAdvice()
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

	// The Panzoom target (#dbx-view-ssShowplan-content / #dbx-view-aseShowplan-graphContent) is a
	// plain block <div> that stretches to fill its ancestor's width (the 3000px-wide .scroll-tree),
	// regardless of how wide the actual rendered plan diagram is - so its own getBoundingClientRect()
	// is *not* a reliable stand-in for "the content" when computing a zoom-to-fit scale (it would
	// often report ~2980px even for a small plan that only needs a couple hundred). This measures the
	// union of all descendant rects instead, which tracks the actual rendered diagram regardless of
	// whether it's narrower than the wrapper (falls short of B) or overflows past it (wider than B).
	// Must walk *all* descendants, not just direct children: the ASE renderer's node boxes happen to
	// sit right under elem, but QP (the SQL Server plan library) wraps its whole tree in one root
	// element whose own box doesn't expand to contain its position:absolute node children (normal CSS
	// behavior for absolutely-positioned descendants) - a direct-children-only union measured that
	// near-empty wrapper instead of the real diagram, producing a near-zero fit scale/pan that panned
	// the actual content off-screen (only "Disable Zoom" - which resets the transform - brought it
	// back). No plan diagram is deep enough for a full-subtree walk to be a real cost.
	function _panzoomContentRect(elem) {
		var kids = elem.querySelectorAll('*');
		if (!kids.length) return elem.getBoundingClientRect();
		var minL = Infinity, minT = Infinity, maxR = -Infinity, maxB = -Infinity;
		for (var i = 0; i < kids.length; i++) {
			var r = kids[i].getBoundingClientRect();
			if (r.width === 0 && r.height === 0) continue;
			minL = Math.min(minL, r.left); minT = Math.min(minT, r.top);
			maxR = Math.max(maxR, r.right); maxB = Math.max(maxB, r.bottom);
		}
		if (minL === Infinity) return elem.getBoundingClientRect();
		return { left: minL, top: minT, width: maxR - minL, height: maxB - minT };
	}

	// Fits the actual rendered content (see _panzoomContentRect above) inside the visible modal-body
	// viewport, centered. Panzoom applies `transform: scale(s) translate(x, y)` with transform-origin
	// 50%/50% of the *panzoom element's own box* (these are plain <div>s, not SVG - see panzoom.min.js's
	// non-SVG default), and zoom()/pan() without a `focal` option set the raw scale/pan values directly
	// with no hidden focal-point math. That makes this solvable directly: for any page-coordinate point
	// Q measured pre-transform, its position after applying scale s and pan (x,y) is
	// `O + s*((Q-O) + (x,y))`, where O is the panzoom element's own (also pre-transform) center - so
	// plugging in Q = the content box's top-left corner and solving for the (x,y) that lands it at the
	// centered target position gives the pan needed, independent of whether the content box lines up
	// with the panzoom element's own box or not.
	//
	// "Pre-transform" measurements can't be had by calling panzoom.zoom(1)/.pan(0,0) and immediately
	// reading getBoundingClientRect() - Panzoom applies its style changes inside requestAnimationFrame
	// (see setTransform's caller in panzoom.min.js), so an immediate read would still reflect whatever
	// transform was painted *before* this call (e.g. wherever the user last zoomed/panned to - exactly
	// when this is actually invoked). Blanking style.transform directly instead forces an immediate,
	// synchronous layout read with no such lag; Panzoom itself never reads back style.transform (its
	// getScale()/getPan() return its own closure state), so this doesn't confuse its bookkeeping, and
	// the final zoom()/pan() calls below overwrite the blanked style with the real fit transform anyway.
	// viewportEl: the box the content should be fitted into. Defaults to the enclosing .modal-body,
	// which is right when the panzoom element is drawn directly on the dialog's canvas (the ASE
	// dialog). The SQL Server dialog passes its own smaller diagram viewport explicitly.
	// alignTopLeft: park the fitted content against the viewport's top-left corner instead of
	// centring it. Centring assumes the viewport is roughly the size of the content, but the SQL
	// Server dialog's diagram viewport stretches to whatever height the section has (it can be a
	// couple of thousand pixels tall), so a short wide plan got centred hundreds of pixels down and
	// simply wasn't where you were looking - you had to scroll to find it. A plan reads from its
	// first operator outwards, so the top-left corner is also where the eye starts.
	function _panzoomZoomToFit(panzoom, elem, viewportEl, alignTopLeft) {
		var container = viewportEl || (elem ? elem.closest('.modal-body') : null);
		if (!panzoom || !elem || !container) return;

		var savedTransform = elem.style.transform;
		elem.style.transform = 'none';

		var elemRect    = elem.getBoundingClientRect();
		var contentRect = _panzoomContentRect(elem);
		var margin = 20;
		var contRect = container.getBoundingClientRect();

		// The toolbar buttons (Redraw/Enable Zoom/Zoom to Fit/...) sit in normal flow *above* elem,
		// inside this same .modal-body - a CSS transform on elem doesn't move them, only elem's own
		// visual paint, so centering vertically against the *whole* modal-body would happily place
		// content above elem's natural top edge, rendering underneath/behind that toolbar row (seen
		// as the diagram's top row getting visually clipped by the buttons). Anchoring the top bound
		// at elem's own natural (pre-transform) top - never higher than that - keeps the fit inside
		// the space actually below the toolbar. No equivalent issue on the other three edges: nothing
		// else in modal-body sits beside elem horizontally, and nothing sits below it.
		var topBound = Math.max(contRect.top, elemRect.top);
		var availW = container.clientWidth - margin;
		var availH = (contRect.top + container.clientHeight) - topBound - margin;
		var scale = Math.min(availW / contentRect.width, availH / contentRect.height, 1);
		scale = Math.max(scale, 0.01);

		var ox = elemRect.left + elemRect.width  / 2;
		var oy = elemRect.top  + elemRect.height / 2;
		// margin/2 inset on each side, so leftover margin space is split symmetrically rather than
		// all landing on the right/bottom edge (availW/availH already had the full margin subtracted).
		var targetLeft = contRect.left + margin / 2;
		var targetTop  = topBound      + margin / 2;
		if (!alignTopLeft) {
			targetLeft += (availW - scale * contentRect.width)  / 2;
			targetTop  += (availH - scale * contentRect.height) / 2;
		}
		var x = (targetLeft - ox) / scale - contentRect.left + ox;
		var y = (targetTop  - oy) / scale - contentRect.top  + oy;

		elem.style.transform = savedTransform;
		panzoom.zoom(scale, { animate: false });
		panzoom.pan(x, y, { animate: false });
	}

	// Toggles a Showplan dialog between its current size/position and ~98% of the screen, centered.
	// Bound to the explicit expand/restore header button (see _injectHtml()) rather than a header
	// dblclick gesture - jQuery UI draggable's handle is the same header element, and dblclick there
	// proved unreliable in practice (reported not firing at all in some real-world setups, plus firing
	// a confusing 3rd toggle via bubbling whenever the button itself was double-clicked). Re-derives
	// $dlg/$cont from dialogId on every call rather than closing over ones captured elsewhere, so it
	// doesn't depend on which shown.bs.modal invocation last ran. keyPrefix matches the localStorage
	// keys the dialog's own drag `stop` callback already uses (e.g. 'ssShowplan-dlg-') to persist
	// *position* - restore/expand are within-session only for size (mirrors the dialog's own default
	// size always being the same on a fresh open, never remembered), but the position it lands at after
	// an expand/restore still sticks across a close+reopen exactly like a manual drag does.
	function _toggleShowplanDlgExpand(dialogId, keyPrefix) {
		var $dlg  = $('#' + dialogId + ' .modal-dialog');
		var $cont = $('#' + dialogId + ' .modal-content');
		var scrPfx = screen.width + 'x' + screen.height + '_';
		var expanded = $dlg.data('expanded');
		var w, h, l, t;
		if (!expanded) {
			$dlg.data('expanded', {
				w: $cont.outerWidth(), h: $cont.outerHeight(),
				l: parseInt($dlg.css('left')), t: parseInt($dlg.css('top'))
			});
			w = Math.round(window.innerWidth  * 0.98);
			h = Math.round(window.innerHeight * 0.98);
			l = Math.round((window.innerWidth  - w) / 2);
			t = Math.round((window.innerHeight - h) / 2);
		} else {
			w = expanded.w; h = expanded.h; l = expanded.l; t = expanded.t;
			$dlg.removeData('expanded');
		}
		$cont.css({ width: w + 'px', height: h + 'px' });
		$dlg.css({ left: l + 'px', top: t + 'px' });
		try {
			localStorage.setItem(scrPfx + keyPrefix + 'left', l);
			localStorage.setItem(scrPfx + keyPrefix + 'top',  t);
		} catch(ex) {}
	}
	window.ssShowplanToggleExpand  = function () { _toggleShowplanDlgExpand('dbx-view-ssShowplan-dialog',  'ssShowplan-dlg-');  };
	window.aseShowplanToggleExpand = function () { _toggleShowplanDlgExpand('dbx-view-aseShowplan-dialog', 'aseShowplan-dlg-'); };

	// SQL table-name extraction (_sqlExtractTables/_nspLoad/_extractTablesAsync) moved into the
	// shared resources/WebContent/scripts/dbxtune/js/dbxSqlTableNames.js module - the standalone
	// /showplan/ase page (ShowplanAseServlet.java) needs the same extraction and doesn't load this
	// file (it doesn't want the Bootstrap modal chrome _injectHtml() below sets up). Call sites in
	// this file use DbxSqlTableNames.extractTablesAsync(sql, cb) directly.

	// -------------------------------------------------------------------------
	// Inject modal HTML + scoped styles
	// -------------------------------------------------------------------------
	function _injectHtml() {
		if (document.getElementById('dbx-view-pgShowplan-dialog')) return;

		// The Table Information section's own styling (table.qs-tableinfo etc.) moved to
		// resources/WebContent/scripts/dbxtune/css/dbxTableInfo.css, linked from graph.html - the
		// standalone /showplan/ase viewer (ShowplanAseServlet.java) needs the same styling and
		// doesn't load this file (no Bootstrap modal chrome wanted there). Only the rules below stay
		// here: they're about this file's own Bootstrap modal stacking, not table-info styling.
		var style = document.createElement('style');
		style.textContent = [
			/* ── text-viewer modal sits above the showplan modal (Bootstrap default is 1050) ── */
			'#dbx-view-sqltext-dialog {',
			'  z-index: 1100;',
			'}',
			'.modal-backdrop + .modal-backdrop {',
			'  z-index: 1090;',
			'}',
			/* qp.js (SQL Server plan diagram) runs in "noCssTooltip" mode (see ssShowplanRender/QP.disableCssTooltips)
			   so that hovering a node's tooltip isn't distorted by the diagram's own Panzoom transform - on real
			   hover it clones the node's .qp-tt and appends the clone straight to <body> (see qp.js's showTooltip()),
			   positioned at the mouse's page coordinates rather than inside the modal. That clone keeps qp.css's own
			   ".qp-tt { z-index: 1; }" ("Layout - can't touch this", so overridden here instead of in that vendored
			   file) - far below the modal's own z-index:1050, so once cloned to <body> as a sibling of the modal,
			   the modal always painted over it. Push it above both the showplan modal and the sqltext sub-dialog. */
			'.qp-tt {',
			'  z-index: 1200 !important;',
			'}',
			/* Prism's own CSS sets code/pre blocks (SQL Text, Parameters, XML Plan, ASE Raw Plan Text -
			   all share the .dbx-view-sqltext-content class, see _injectHtml() markup below) to a flat
			   font-size:1em, which reads noticeably bigger than the rest of this dialog's UI text (mostly
			   0.75em-0.85em). Code's own font-size is *also* a flat 1em (relative to its pre parent, not
			   inherited from it), so shrinking just the <pre> is enough - the <code> inside scales with it. */
			'pre:has(> code.dbx-view-sqltext-content) {',
			'  font-size: 0.8em !important;',
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
			"				<div style='display:flex;align-items:center;flex-shrink:0;'>",
			"					<button type='button' class='close' style='margin-left:8px;' title='Expand/restore dialog size' aria-label='Expand/restore dialog size' onclick='ssShowplanToggleExpand();'><span aria-hidden='true'>&#9974;</span></button>",
			"					<button type='button' class='close' style='margin-left:8px;' data-dismiss='modal' aria-label='Close'><span aria-hidden='true'>&times;</span></button>",
			"				</div>",
			"			</div>",
			"			<div class='modal-body' style='overflow-x:auto;padding:8px 12px;'>",
			// width:100%, not the fixed 3000px this used to be. That canvas existed so a wide QP.js
			// diagram could be scrolled by .modal-body, but it made EVERY section (Plan Analysis, SQL
			// Text, Parameters, ...) 3000px wide, so the dialog always had a long horizontal scrollbar
			// that had nothing to do with the content, and a wide plan then gave you two nested
			// horizontal scrollbars to fight with. Both renderers now draw into
			// #dbx-view-ssShowplan-content, which has its own overflow:auto, and Prism already gives
			// the SQL/XML <pre> blocks theirs - so nothing needs the oversized canvas any more.
			"				<div class='scroll-tree' style='width:100%;display:flex;align-items:flex-start;'>",
			// Left column: every <details> section. The Properties pane is a sibling of this column
			// rather than living inside the Execution Plan section, which is what lets it stay pinned
			// while you scroll all the way down past Plan Analysis / SQL Text / XML Plan. As a child
			// of the section it could only ever stick within that section's own height - and not at
			// all for a small plan, where the section is no taller than the pane so sticky has no
			// travel to work with.
			"					<div id='dbx-ssp-sections' style='flex:1 1 auto;min-width:0;'>",

			"				<!-- ▶ Execution Plan -->",
			"				<details open id='dbx-ssp-sect-plan' style='border:1px solid #d0d0d0;border-radius:3px;background:#fafafa;margin-bottom:4px;'>",
			"					<summary style='cursor:pointer;padding:5px 10px;font-size:0.85em;font-weight:600;list-style:none;user-select:none;'>&#128202; Execution Plan</summary>",
			"					<div style='padding:4px 8px 10px 8px;'>",
			// display:flex so the renderer toggle (moved to the end, below) can be pushed to the right
			// edge of this row with margin-left:auto while everything before it keeps its normal
			// left-to-right order - flex-wrap so a narrow dialog still wraps instead of overflowing.
			// column-gap replaces the small natural gap the whitespace between these button tags used
			// to give for free before this became a flex row - flex ignores that whitespace entirely,
			// which is why the buttons read as fused together with no fix (reported live, screenshot
			// showed "Hide Properties"/"Top-to-Bottom"/"Use html-query-plan" touching).
			"						<div style='position:relative;z-index:2;display:flex;align-items:center;flex-wrap:wrap;row-gap:4px;column-gap:6px;'>",
			"						<button type='button' id='dbx-view-ssShowplan-redrawBtn' class='btn btn-outline-secondary btn-sm' onclick='ssShowplanRedraw();'>&#8635; Redraw</button>",
			"						<button type='button' id='dbx-view-ssShowplan-zoomBtn' class='btn btn-outline-secondary btn-sm' onclick='ssShowplanToggleZoom();'>&#128269; Enable Zoom</button>",
			"						<button type='button' id='dbx-view-ssShowplan-zoomFitBtn' class='btn btn-outline-secondary btn-sm' style='display:none;' onclick='ssShowplanZoomToFit();'>&#8862; Zoom to Fit</button>",
			// Only meaningful with a live server context: the threshold is compared against sizes that
			// come from DDL Storage, so without srv/dbname there is nothing to compare and the control
			// is hidden (see _ssShowplanSyncToolbar). Mirrors the ASE dialog's equivalent.
			// Same look and feel as the ASE dialog's equivalent controls (see aseShowplanSetTableSizeWarnMb's
			// span and dbx-view-aseShowplan-analysis-link) - plain inline spans, not Bootstrap buttons, so
			// this row reads as toolbar options/status rather than more actions.
			"						<span id='dbx-view-ssShowplan-warnMbWrap' style='display:none;font-size:0.8em;margin-left:14px;' title='A Table Scan, Clustered Index Scan or (non-seek) Index Scan that reads more than this much data is flagged in the diagram with a red border and a &quot;Large table/index&quot; warning, and raises a Plan Analysis finding. Default 100 MB - lower it to catch smaller tables/indexes too, or raise it if 100 MB is normal-sized here. Sizes come from DDL Storage, so this needs the same srv/dbname context as the Table Information section.'>Options: Big table &gt; <input type='number' id='dbx-view-ssShowplan-warnMb' style='width:60px;padding:1px 4px;font-size:0.85em;' min='0' step='1' onchange='ssShowplanSetTableSizeWarnMb(this.value);'> MB</span>",
			// Findings count, jumping to the Plan Analysis section. Populated by ssShowplanRunAnalysis()
			// via _ssShowplanSetFindingsButton() (dbxShowplanAse.js's equivalent: linkEl, same markup).
			"						<span id='dbx-view-ssShowplan-findingsBtn' style='display:none;font-size:0.85em;margin-left:14px;cursor:pointer;font-weight:600;' onclick='ssShowplanGotoAnalysis();' title='Jump to the Plan Analysis section below'></span>",
			// Properties/Orientation/Renderer live in their OWN flex wrapper, not as three individual
			// items in the outer row - margin-left:auto on the WRAPPER (one occurrence, not one per
			// button) pushes the whole group right as a single unit. Tried putting margin-left:auto on
			// each of the three buttons individually first: _ssShowplanSyncToolbar() hides Properties
			// and Orientation entirely in html-query-plan mode, and when the outer row ran out of width
			// and wrapped, each surviving auto-margin button ended up pushed to the right edge of
			// whichever wrapped LINE it happened to land on - three separate lines, each "right-
			// aligned" on its own but visually indistinguishable from normal left flow once a line
			// held only one item. A single wrapper fixes both problems at once: it wraps as one block
			// (the three buttons never split across lines from each other), and hiding a button inside
			// it doesn't change whether the WRAPPER itself still carries margin-left:auto.
			"						<div style='display:flex;align-items:center;gap:6px;margin-left:auto;'>",
			"						<button type='button' id='dbx-view-ssShowplan-propsBtn' class='btn btn-outline-secondary btn-sm' onclick='ssShowplanToggleProps();'>&#128203; Hide Properties</button>",
			"						<button type='button' id='dbx-view-ssShowplan-orientationBtn' class='btn btn-outline-secondary btn-sm' onclick='ssShowplanToggleOrientation();'>&#8646; Top-to-Bottom</button>",
			"						<button type='button' id='dbx-view-ssShowplan-rendererBtn' class='btn btn-outline-secondary btn-sm' title='Switch between the built-in DbxTune plan diagram and the vendored html-query-plan renderer' onclick='ssShowplanToggleRenderer();'>&#8646; Use html-query-plan</button>",
			"						</div>",
			"						</div>",
			// The diagram and the SSMS-style Properties pane sit side by side. #dbx-view-ssShowplan-content
			// graphWrap now holds only the diagram - the Properties pane moved out to the dialog-level
			// column (see .scroll-tree above).
			"						<div id='dbx-view-ssShowplan-graphWrap' style='margin-top:6px;min-height:200px;width:100%;'>",
			// #dbx-view-ssShowplan-content is the fixed-size SCROLL VIEWPORT; the inner zoomTarget is
			// what both renderers draw into and what Panzoom scales. Zooming the viewport itself
			// scales its own clipping box and leaves its scrollbars in play, which clipped the plan
			// and produced scrollbars inside the zoomed content.
			"							<div id='dbx-view-ssShowplan-content' class='dbx-view-ssShowplan-content' style='overflow:auto;'>",
			"								<div id='dbx-view-ssShowplan-zoomTarget' style='width:max-content;min-width:100%;'></div>",
			"							</div>",
			"						</div>",
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
			"					<div id='dbx-ssp-tableinfo-body' style='padding:4px 8px 8px 8px;overflow-x:auto;'>",
			"						<span style='color:#888;font-size:0.85em;'>&#9203; Loading table information…</span>",
			"					</div>",
			"				</details>",

			"				<!-- ▶ LLM Optimization Advice (lazy-loaded on first expand) -->",
			"				<details id='dbx-ssp-sect-llm' style='border:1px solid #d0d0d0;border-radius:3px;background:#fafafa;margin-bottom:4px;'>",
			"					<summary style='cursor:pointer;padding:5px 10px;font-size:0.85em;font-weight:600;list-style:none;user-select:none;'>&#129302; LLM Optimization Advice</summary>",
			"					<div style='padding:4px 8px 8px 8px;'>",
			// This button lives OUTSIDE #dbx-ssp-llm-body on purpose: dbxLlmAdvice.js's target-mode
			// rendering replaces the whole innerHTML of its target element, so anything inside
			// dbx-ssp-llm-body itself would be wiped out on every (re)load.
			"						<button type='button' class='btn btn-outline-secondary btn-sm' style='margin-bottom:4px;' onclick='ssShowplanRefreshLlmAdvice();'>&#8635; Refresh Advice</button>",
			"						<div id='dbx-ssp-llm-body'>",
			"							<span style='color:#888;font-size:0.85em;'>Expand to ask an LLM for optimization advice.</span>",
			"						</div>",
			"					</div>",
			"				</details>",

			// "No exec" mode - GET /api/llm/optimize-sql never calls out to an LLM (see
			// LlmSqlOptimizeServlet's preview handling), so this is always available: when the LLM
			// feature is off, it's the only way to get advice (manually, via any LLM chat); when it's
			// on, it's still handy to see/copy the exact prompt without spending a real call - the
			// dbxLlmAdvice.isEnabled() toggle in _initHandlers() below just swaps the note text
			// (id='dbx-ssp-llmpreview-note') between those two framings. Mirrors the ASE dialog's
			// dbx-asp-sect-llmpreview.
			"				<!-- ▶ LLM Prompt Preview (always available alongside LLM Optimization Advice above) -->",
			"				<details id='dbx-ssp-sect-llmpreview' style='display:none;border:1px solid #d0d0d0;border-radius:3px;background:#fafafa;margin-bottom:4px;'>",
			"					<summary style='cursor:pointer;padding:5px 10px;font-size:0.85em;font-weight:600;list-style:none;user-select:none;'>&#128203; LLM Prompt Preview</summary>",
			"					<div style='padding:4px 8px 8px 8px;'>",
			"						<p class='text-muted' id='dbx-ssp-llmpreview-note' style='font-size:0.8em;'>This DbxCentral instance doesn't have LLM Optimization Advice turned on, so nothing gets sent anywhere - but here's the exact prompt it would send. Copy it into any LLM chat (claude.ai, chatgpt.com, ...) yourself for a quick manual shortcut into the same advice.</p>",
			"						<button type='button' class='btn btn-outline-secondary btn-sm' style='margin-bottom:4px;' onclick='ssShowplanRefreshLlmPreview();'>&#8635; Build Prompt</button>",
			"						<button type='button' class='btn btn-outline-secondary btn-sm' style='margin-bottom:4px;' onclick='ssShowplanCopyLlmPreview();'>&#128203; Copy Prompt</button>",
			"						<div id='dbx-ssp-llmpreview-body'>",
			"							<span style='color:#888;font-size:0.85em;'>Expand to build the prompt.</span>",
			"						</div>",
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

			"					</div>",
			"					<div id='dbx-view-ssShowplan-propsSplit' title='Drag to resize' style='flex:0 0 6px;align-self:stretch;cursor:col-resize;background:#e4e4e4;border-radius:3px;margin:0 4px;'></div>",
			// position:sticky pins the pane's top edge while .modal-body scrolls. Two conditions make
			// it work, both verified: .modal-body is the scrolling ancestor and every element between
			// it and the pane is overflow:visible (any clipping ancestor cancels sticky), and
			// align-self:flex-start stops the flex row stretching the pane to the full column height -
			// a stretched sticky element exactly fills its containing block and so has nowhere to move.
			// max-height is set from the dialog's visible body height by _ssShowplanFitPropsPane(); the
			// calc() here is only a sane starting value for the moment before that first runs.
			"					<div id='dbx-view-ssShowplan-propsPane' class='ss-plan-props-scroll' style='flex:0 0 320px;align-self:flex-start;position:sticky;top:4px;overflow:auto;max-height:calc(100vh - 260px);border:1px solid #d8d8d8;border-radius:3px;padding:6px 10px 8px 10px;'>",
			// Header stays outside #dbx-view-ssShowplan-propsBody: renderPropertiesInto() empties its
			// target on every selection, which would take the close button with it.
			"						<div style='display:flex;align-items:center;justify-content:space-between;gap:6px;border-bottom:1px solid #e6e6e6;margin-bottom:6px;padding-bottom:3px;'>",
			"							<span style='font-size:10px;font-weight:600;text-transform:uppercase;letter-spacing:0.04em;color:#8a8a8a;'>Properties</span>",
			"							<button type='button' title='Hide the Properties pane' aria-label='Hide the Properties pane' onclick='ssShowplanToggleProps();' style='border:0;background:none;cursor:pointer;color:#999;font-size:15px;line-height:1;padding:0 2px;'>&times;</button>",
			"						</div>",
			"						<div id='dbx-view-ssShowplan-propsBody'></div>",
			"					</div>",
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
			"</div>",

			// ---- ASE (Sybase/SAP Adaptive Server Enterprise) Showplan dialog ----
			"<div class='modal fade' id='dbx-view-aseShowplan-dialog' role='dialog' aria-labelledby='dbx-view-aseShowplan-dialog' aria-hidden='true'>",
			"	<div class='modal-dialog modal-dialog-centered mw-100' role='document'>",
			"		<div class='modal-content'>",
			"			<div class='modal-header' style='cursor:move;'>",
			"				<span style='color:#999;margin-right:6px;font-size:1.1em;' title='Drag to move'>&#x2630;</span>",
			"				<h5 class='modal-title' style='flex:1;min-width:0;'><b>ASE Showplan</b>: <span id='dbx-view-aseShowplan-objectName'></span></h5>",
			"				<div style='display:flex;align-items:center;flex-shrink:0;'>",
			"					<button type='button' class='close' style='margin-left:8px;' title='Expand/restore dialog size' aria-label='Expand/restore dialog size' onclick='aseShowplanToggleExpand();'><span aria-hidden='true'>&#9974;</span></button>",
			"					<button type='button' class='close' style='margin-left:8px;' data-dismiss='modal' aria-label='Close'><span aria-hidden='true'>&times;</span></button>",
			"				</div>",
			"			</div>",
			"			<div class='modal-body' style='overflow-x:auto;padding:8px 12px;'>",
			"				<div class='scroll-tree' style='width:3000px;'>",

			"				<!-- ▶ Graphical Plan -->",
			"				<details open id='dbx-asp-sect-graph' style='border:1px solid #d0d0d0;border-radius:3px;background:#fafafa;margin-bottom:4px;'>",
			"					<summary id='dbx-view-aseShowplan-plan-summary' style='cursor:pointer;padding:5px 10px;font-size:0.85em;font-weight:600;list-style:none;user-select:none;'>&#128202; Graphical Plan</summary>",
			"					<div style='padding:4px 8px 10px 8px;'>",
			"						<div style='position:relative;z-index:2;'>",
			"						<button type='button' class='btn btn-outline-secondary btn-sm' onclick='aseShowplanRedraw();'>&#8635; Redraw</button>",
			"						<button type='button' id='dbx-view-aseShowplan-orientationBtn' class='btn btn-outline-secondary btn-sm' onclick='aseShowplanToggleOrientation();'>&#8646; Top-to-Bottom</button>",
			"						<button type='button' id='dbx-view-aseShowplan-zoomBtn' class='btn btn-outline-secondary btn-sm' onclick='aseShowplanToggleZoom();'>&#128269; Enable Zoom</button>",
			"						<button type='button' id='dbx-view-aseShowplan-zoomFitBtn' class='btn btn-outline-secondary btn-sm' style='display:none;' onclick='aseShowplanZoomToFit();'>&#8862; Zoom to Fit</button>",
			"						<span style='font-size:0.8em;color:#888;margin-left:6px;'>Execution order is by VA# (starting at 0)</span>",
			"						<span style='font-size:0.8em;margin-left:14px;'>",
			"							Options: <span title='A Table Scan, Clustered Index Scan, or (non-By-Key) Index Scan that reads more than this much data is flagged in the diagram with a red border and a &quot;Large table/index&quot; warning - a By Key seek only touches the rows it needs, so it is never flagged. Default 100 MB - lower it to catch smaller tables/indexes too, or raise it if 100 MB is normal-sized in this environment. Requires Table Information context (srv/dbname), same as the Table Information section below - has no effect otherwise.'>Big table &gt; <input type='number' id='dbx-view-aseShowplan-warnMb' style='width:60px;padding:1px 4px;font-size:0.85em;' min='0' step='1' onchange='aseShowplanSetTableSizeWarnMb(this.value);'> MB</span>",
			"						</span>",
			"						<span id='dbx-view-aseShowplan-analysis-link' style='display:none;font-size:0.85em;margin-left:14px;cursor:pointer;font-weight:600;' onclick='aseShowplanJumpToAnalysis();' title='Jump to the Plan Analysis section below'></span>",
			"						</div>",
			"						<div id='dbx-view-aseShowplan-planSelectRow' style='display:none;margin-top:6px;font-size:0.85em;'>",
			"							<label for='dbx-view-aseShowplan-planSelect' title='ASE cached several distinct compiled plans for this statement (e.g. one per differing parameter values) - pick which one to view.'>&#128203; Cached plan:</label>",
			"							<select id='dbx-view-aseShowplan-planSelect' style='max-width:100%;padding:1px 4px;' onchange='aseShowplanSelectPlan(this.value);'></select>",
			"						</div>",
			"						<div id='dbx-view-aseShowplan-graphFallback' class='ase-plan-fallback' style='display:none;'>Could not parse this plan into a diagram &mdash; see \"Raw Plan Text\" below.</div>",
			"						<div id='dbx-view-aseShowplan-graphContent' class='dbx-view-aseShowplan-graphContent' style='margin-top:6px;'></div>",
			"					</div>",
			"				</details>",

			"				<!-- ▶ Plan Analysis (managed by AseShowplan.render()'s opts.onFindingsChanged callback) -->",
			"				<details id='dbx-asp-sect-analysis' style='display:none;border:1px solid #d0d0d0;border-radius:3px;background:#fafafa;margin-bottom:4px;'>",
			"					<summary id='dbx-view-aseShowplan-analysis-summary' style='cursor:pointer;padding:5px 10px;font-size:0.85em;font-weight:600;list-style:none;user-select:none;'>&#128270; Plan Analysis</summary>",
			"					<div id='dbx-view-aseShowplan-analysis-body' style='padding:4px 12px 8px 12px;'></div>",
			"				</details>",

			"				<!-- ▶ Raw Plan Text -->",
			"				<details id='dbx-asp-sect-plan' style='border:1px solid #d0d0d0;border-radius:3px;background:#fafafa;margin-bottom:4px;'>",
			"					<summary style='cursor:pointer;padding:5px 10px;font-size:0.85em;font-weight:600;list-style:none;user-select:none;'>&#128196; Raw Plan Text</summary>",
			"					<div style='padding:4px 8px 8px 8px;'>",
			"						<button type='button' class='btn btn-outline-secondary btn-sm' style='margin-bottom:4px;' onclick='dbxCopyCodeBlock(\"dbx-view-aseShowplan-planContent\");'>Copy Plan Text</button>",
			"						<pre><code id='dbx-view-aseShowplan-planContent' class='line-numbers dbx-view-sqltext-content'></code></pre>",
			"					</div>",
			"				</details>",

			"				<!-- ▶ SQL Text -->",
			"				<details open id='dbx-asp-sect-sql' style='border:1px solid #d0d0d0;border-radius:3px;background:#fafafa;margin-bottom:4px;'>",
			"					<summary style='cursor:pointer;padding:5px 10px;font-size:0.85em;font-weight:600;list-style:none;user-select:none;'>&#128196; SQL Text</summary>",
			"					<div style='padding:4px 8px 8px 8px;'>",
			"						<button type='button' class='btn btn-outline-secondary btn-sm' style='margin-bottom:4px;' onclick='aseShowplanFormatSql();'>Format SQL</button>",
			"						<button type='button' class='btn btn-outline-secondary btn-sm' style='margin-bottom:4px;' onclick='dbxCopyCodeBlock(\"dbx-view-aseShowplan-sqlContent\");'>Copy SQL</button>",
			"						<pre><code id='dbx-view-aseShowplan-sqlContent' class='language-sql line-numbers dbx-view-sqltext-content'></code></pre>",
			"					</div>",
			"				</details>",

			"				<!-- ▶ Table Information (lazy-loaded on first expand) -->",
			"				<details id='dbx-asp-sect-tableinfo' style='border:1px solid #d0d0d0;border-radius:3px;background:#fafafa;margin-bottom:4px;'>",
			"					<summary style='cursor:pointer;padding:5px 10px;font-size:0.85em;font-weight:600;list-style:none;user-select:none;'>&#128220; Table Information</summary>",
			"					<div id='dbx-asp-tableinfo-body' style='padding:4px 8px 8px 8px;'>",
			"						<span style='color:#888;font-size:0.85em;'>&#9203; Loading table information…</span>",
			"					</div>",
			"				</details>",

			"				<!-- ▶ LLM Optimization Advice (lazy-loaded on first expand) -->",
			"				<details id='dbx-asp-sect-llm' style='border:1px solid #d0d0d0;border-radius:3px;background:#fafafa;margin-bottom:4px;'>",
			"					<summary style='cursor:pointer;padding:5px 10px;font-size:0.85em;font-weight:600;list-style:none;user-select:none;'>&#129302; LLM Optimization Advice</summary>",
			"					<div style='padding:4px 8px 8px 8px;'>",
			// This button lives OUTSIDE #dbx-asp-llm-body on purpose: dbxLlmAdvice.js's target-mode
			// rendering replaces the whole innerHTML of its target element, so anything inside
			// dbx-asp-llm-body itself would be wiped out on every (re)load.
			"						<button type='button' class='btn btn-outline-secondary btn-sm' style='margin-bottom:4px;' onclick='aseShowplanRefreshLlmAdvice();'>&#8635; Refresh Advice</button>",
			"						<div id='dbx-asp-llm-body'>",
			"							<span style='color:#888;font-size:0.85em;'>Expand to ask an LLM for optimization advice.</span>",
			"						</div>",
			"					</div>",
			"				</details>",

			// "No exec" mode - GET /api/llm/optimize-sql never calls out to an LLM (see
			// LlmSqlOptimizeServlet's preview handling), so this is always available: when the LLM
			// feature is off, it's the only way to get advice (manually, via any LLM chat); when it's
			// on, it's still handy to see/copy the exact prompt without spending a real call - the
			// dbxLlmAdvice.isEnabled() toggle in _initHandlers() below just swaps the note text
			// (id='dbx-asp-llmpreview-note') between those two framings.
			"				<!-- ▶ LLM Prompt Preview (always available alongside LLM Optimization Advice above) -->",
			"				<details id='dbx-asp-sect-llmpreview' style='display:none;border:1px solid #d0d0d0;border-radius:3px;background:#fafafa;margin-bottom:4px;'>",
			"					<summary style='cursor:pointer;padding:5px 10px;font-size:0.85em;font-weight:600;list-style:none;user-select:none;'>&#128203; LLM Prompt Preview</summary>",
			"					<div style='padding:4px 8px 8px 8px;'>",
			"						<p class='text-muted' id='dbx-asp-llmpreview-note' style='font-size:0.8em;'>This DbxCentral instance doesn't have LLM Optimization Advice turned on, so nothing gets sent anywhere - but here's the exact prompt it would send. Copy it into any LLM chat (claude.ai, chatgpt.com, ...) yourself for a quick manual shortcut into the same advice.</p>",
			"						<button type='button' class='btn btn-outline-secondary btn-sm' style='margin-bottom:4px;' onclick='aseShowplanRefreshLlmPreview();'>&#8635; Build Prompt</button>",
			"						<button type='button' class='btn btn-outline-secondary btn-sm' style='margin-bottom:4px;' onclick='aseShowplanCopyLlmPreview();'>&#128203; Copy Prompt</button>",
			"						<div id='dbx-asp-llmpreview-body'>",
			"							<span style='color:#888;font-size:0.85em;'>Expand to build the prompt.</span>",
			"						</div>",
			"					</div>",
			"				</details>",

			"				</div>",
			"			</div>",
			"			<div class='modal-footer'>",
			"				<button type='button' class='btn btn-outline-secondary' onclick='aseShowplanCopySql();'>Copy SQL</button>",
			"				<button type='button' class='btn btn-outline-secondary' onclick='aseShowplanCopyPlan();'>Copy Plan</button>",
			"				<button type='button' class='btn btn-outline-secondary' onclick='aseShowplanOpenExternal();'>Open in External Window</button>",
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
		// paramTypes.positional tells sql-formatter that a bare "?" is a valid positional parameter
		// placeholder (JDBC-style) rather than a syntax error - without it, any captured SQL text
		// containing "?" throws a parse error here instead of formatting.
		var formatOptions = { language: 'postgresql', tabWidth: 4, keywordCase: 'upper', tabulateAlias: true, paramTypes: { positional: true } };
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
	// Turns zoom off if it's currently on: resets pan/scale first (snaps back to normal before it's
	// gone), removes the wheel listener (the actual point of this - see the matching, more detailed
	// comment on aseShowplanToggleZoom() in the ASE section below for why Panzoom's own disableZoom
	// option can't be used for this instead), then destroy()s the instance to also stop drag-to-pan.
	// A no-op if zoom isn't currently enabled. Shared by the toggle button itself and by the two
	// "about to show a different plan" call sites below, which want a guaranteed-off starting state
	// for the new plan - previously via ssShowplanResetZoom(), which only reset position/scale and
	// left zoom (and its wheel hijack) running if it happened to already be on.
	function _ssShowplanDisableZoom() {
		if (_ssShowplanZoom === undefined) return;
		var elem    = _ssShowplanZoomTarget();
		var outer   = document.getElementById('dbx-view-ssShowplan-content');
		var btn     = document.getElementById('dbx-view-ssShowplan-zoomBtn');
		var fitBtn  = document.getElementById('dbx-view-ssShowplan-zoomFitBtn');
		try { _ssShowplanZoom.reset(); } catch (ex) {}
		// Hand scrolling back to the viewport now that pan/zoom is no longer driving movement.
		if (outer) outer.style.overflow = 'auto';
		// Detach from both, so a listener bound by an older build (which attached to the transformed
		// element) is cleaned up too rather than being left behind to stack.
		if (outer) outer.removeEventListener('wheel', _ssShowplanZoom.zoomWithWheel);
		if (elem)  elem.removeEventListener('wheel', _ssShowplanZoom.zoomWithWheel);
		try { _ssShowplanZoom.destroy(); } catch (ex) {}
		_ssShowplanZoom = undefined;
		if (btn) btn.innerHTML = '&#128269; Enable Zoom';
		if (fitBtn) fitBtn.style.display = 'none';
	}

	window.ssShowplanToggleZoom = function () {
		if (_ssShowplanZoom !== undefined) { _ssShowplanDisableZoom(); return; }
		var elem   = _ssShowplanZoomTarget();
		var outer  = document.getElementById('dbx-view-ssShowplan-content');
		var btn    = document.getElementById('dbx-view-ssShowplan-zoomBtn');
		var fitBtn = document.getElementById('dbx-view-ssShowplan-zoomFitBtn');
		if (!elem) return;
		// Panzoom is an optional page asset - a page that forgets to load it should degrade to "zoom
		// unavailable", not throw an uncaught ReferenceError out of an onclick with nothing shown to
		// the user (which is exactly what /showplan/sqlserver did until it started loading panzoom).
		if (typeof Panzoom === 'undefined') {
			if (btn) { btn.disabled = true; btn.title = 'Zoom is unavailable - panzoom.min.js is not loaded on this page'; }
			return;
		}
		// step is a fixed zoom factor applied per wheel *event*, not scaled by scroll delta - a
		// trackpad fires far more events per gesture than a mouse wheel "click", so Panzoom's
		// default (0.3) feels much more aggressive there. Lower value = gentler zoom per event.
		_ssShowplanZoom = Panzoom(elem, { maxScale: 1, minScale: 0.01, step: 0.05 });
		// While zooming, Panzoom's own pan is the movement mechanism - leaving the viewport scrollable
		// too gives two competing ways to move the same content, which is what showed up as scrollbars
		// inside the zoomed plan.
		if (outer) outer.style.overflow = 'hidden';
		// The wheel listener goes on the VIEWPORT, not on the element Panzoom transforms. The
		// transformed element only receives pointer events where it is actually painted - which after
		// a zoom-to-fit can be a small patch in one corner - so anywhere else in the viewport the
		// wheel fell through and scrolled the dialog instead of zooming. The viewport always covers
		// the whole area, and zoomWithWheel acts on the Panzoom instance rather than on event.target,
		// so it does not care which element the event arrived on.
		(outer || elem).addEventListener('wheel', _ssShowplanZoom.zoomWithWheel);
		if (btn) btn.innerHTML = '&#128269; Disable Zoom';
		if (fitBtn) fitBtn.style.display = '';
	};

	window.ssShowplanZoomToFit = function () {
		// Fit into the diagram's own viewport. Passing .modal-body (the default) would size the plan
		// against the whole dialog - far larger than the area it is actually drawn in now that the
		// Properties pane sits beside it - so the result overflowed and got clipped.
		_panzoomZoomToFit(_ssShowplanZoom, _ssShowplanZoomTarget(),
			document.getElementById('dbx-view-ssShowplan-content'), true /* align top-left */);
	};

	window.ssShowplanFormatSql = function () {
		// See the matching comment on pgShowplanFormatSql() - without paramTypes.positional, a bare
		// "?" in the captured SQL throws a parse error here instead of formatting.
		var formatOptions = { language: 'tsql', tabWidth: 4, keywordCase: 'upper', tabulateAlias: true, paramTypes: { positional: true } };
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
		// Also carry sql/srv/dbname over so the standalone page's SQL Text/Table Information/LLM
		// sections work there too, instead of degrading to a bare plan+diagram view - same shared
		// context store (#dbx-ssp-tableinfo-body's data-* attrs) _ssShowplanLoadLlmAdvice() reads.
		var tiBody = document.getElementById('dbx-ssp-tableinfo-body');
		var srv    = tiBody ? (tiBody.getAttribute('data-srv')    || '') : '';
		var dbname = tiBody ? (tiBody.getAttribute('data-dbname') || '') : '';
		var sql    = $('#dbx-view-ssShowplan-sqlContent').text() || '';
		submit_post_via_hidden_form(toUrl, {
			plan:     planText,
			sql:      sql,
			dbVendor: 'Microsoft SQL Server',
			srv:      srv,
			dbname:   dbname
		});
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

		// When the XML plan carries no StatementText, fall back to the lastKnownSql-derived message
		// built above (tmpSql) instead of an empty string - callers always re-set the DOM with this
		// return value (overwriting the tmpSql already written above), and ssShowplanOpenExternal()
		// reads that same DOM text back out to POST as 'sql' to /showplan/sqlserver - an empty return
		// here silently discarded the fallback and made the standalone page think there was no SQL
		// text at all, disabling its LLM Advice section entirely instead of degrading gracefully.
		return sqlText || tmpSql;
	};

	window.ssShowplanCopySql = function () {
		var txt = $('#dbx-view-ssShowplan-sqlContent').text();
		var ta = document.createElement('textarea'); ta.value = txt; document.body.appendChild(ta); ta.select();
		try { document.execCommand('copy'); } catch (err) { alert('Unable to copy\n\n' + err); }
		document.body.removeChild(ta);
	};

	// -------------------------------------------------------------------------
	// ASE (Sybase/SAP Adaptive Server Enterprise) Showplan dialog
	// -------------------------------------------------------------------------

	// AseConnectionUtils.getShowplan() is called with addHtmlTags=true (CmActiveStatements.java),
	// which wraps its captured text in "<html>Showplan:<pre>...</pre></html>" - normally that's only
	// ever handed to the plan-text parser (which already strips it, see parseText() in
	// dbxShowplanAse.js), but the same raw capture can end up feeding the SQL Text field too, which
	// has no parser of its own to strip it first. Same defensive strip, applied here instead.
	function _aseStripHtmlWrapper(text) {
		if (!text) return text;
		return text.replace(/^[\s\S]*?<pre>/i, '').replace(/<\/pre>[\s\S]*$/i, '');
	}

	// ASE captures a dynamic SQL cursor's statement wrapped as
	// "DYNAMIC_SQL <name>: create proc <name> (...) as <actual query>" - dbxSqlText.js already strips
	// the shorter "DYNAMIC_SQL dyn198: " prefix from its own captured SQL text, but this goes further
	// and also drops the "create proc ... as" wrapper ASE generates around the dynamic statement,
	// leaving just the query itself, which is what "Format SQL" should actually be formatting rather
	// than a CREATE PROC wrapper around it.
	// The (?:\((?:[^()]|\([^()]*\))*\))? part allows one level of nesting inside the parameter list -
	// needed because ASE parameter types routinely nest their own parens, e.g. varchar(30) or
	// numeric(15,7) inside the proc's own (@P1 int, @P2 varchar(30)) parameter list. A naive \([^)]*\)
	// stops at the first ")" it finds - which closes the INNER type's paren, not the parameter list's
	// own - leaving the real closing paren unconsumed and the whole match failing (caught by testing
	// against a real parameterized example, not just a no-params one).
	var DYNAMIC_SQL_WRAPPER_RE = /^\s*DYNAMIC_SQL\s+\S+\s*:\s*create\s+proc(?:edure)?\s+\S+\s*(?:\((?:[^()]|\([^()]*\))*\))?\s*as\s*/i;
	function _aseStripDynamicSqlWrapper(text) {
		if (!text) return text;
		return text.replace(DYNAMIC_SQL_WRAPPER_RE, '');
	}

	var _aseShowplanZoom = undefined;
	var _aseShowplanLastPlanText = undefined;
	var _aseShowplanLastIsXml = undefined;
	// Which of parsed.plans[] (see dbxShowplanAse.js's parseXml() - ASE can cache several distinct
	// compiled plans for the same statement) is currently shown. Reset to 0 (the worst-total-time
	// plan, since plans[] is sorted that way) whenever a genuinely new plan is loaded; preserved
	// across Redraw/orientation-toggle/etc. re-renders of the SAME plan, which all re-parse from
	// _aseShowplanLastPlanText and would otherwise silently snap back to plan 0.
	var _aseShowplanSelectedPlanIndex = 0;
	// Left-to-right, compact-layout, SVG-line-connectors are now the defaults (all three started as
	// opt-in experiments, all three were confirmed as improvements over the originals - see
	// dbxShowplanAse.js's render()/reorderCompactByVa()/tuckLeavesNearParent() comments for the
	// compact-layout history, and drawConnectorLines() for why arrow connectors won over the CSS
	// pseudo-element ones once compact layout needed a connector style that doesn't depend on <li>
	// nesting depth).
	var _aseShowplanHorizontal = true;
	try {
		var _storedHorizontal = localStorage.getItem('dbxtune_aseShowplan_horizontal');
		if (_storedHorizontal !== null) _aseShowplanHorizontal = _storedHorizontal === 'true';
	} catch (ex) {}

	// Table Scan operators against a table bigger than this get flagged in the diagram (see
	// dbxShowplanAse.js's render()/buildDetailPanel() - needs srv/dbname context to look up table
	// size at all, same as the Table Information section below it uses). User-adjustable since
	// what counts as "big" varies a lot by environment.
	var _aseShowplanTableSizeWarnMb = 100;
	try {
		var _storedWarnMb = localStorage.getItem('dbxtune_aseShowplan_tableSizeWarnMb');
		if (_storedWarnMb !== null && !isNaN(parseFloat(_storedWarnMb))) _aseShowplanTableSizeWarnMb = parseFloat(_storedWarnMb);
	} catch (ex) {}

	// A separate "Reset Zoom" button used to sit next to this one, but with zoom left enabled a
	// mouse-wheel scroll over the plan always zooms instead of scrolling the page - reported as
	// unwanted when the user actually wanted to scroll. Merged into a single Enable/Disable toggle
	// instead: disabling doesn't just reset the pan/scale, it removes the wheel listener entirely so
	// the wheel goes back to normal scrolling until zoom is explicitly re-enabled. Panzoom's own
	// disableZoom option isn't enough on its own for this - zoomWithWheel() calls
	// event.preventDefault() unconditionally before it even checks that option, so the scroll would
	// still be eaten with nothing happening in its place.
	window.aseShowplanToggleZoom = function () {
		var elem   = document.getElementById('dbx-view-aseShowplan-graphContent');
		var btn    = document.getElementById('dbx-view-aseShowplan-zoomBtn');
		var fitBtn = document.getElementById('dbx-view-aseShowplan-zoomFitBtn');
		if (_aseShowplanZoom === undefined) {
			if (!elem) return;
			// See the matching comment in ssShowplanToggleZoom() - lower step = gentler trackpad zoom.
			_aseShowplanZoom = Panzoom(elem, { maxScale: 1, minScale: 0.01, step: 0.05 });
			elem.addEventListener('wheel', _aseShowplanZoom.zoomWithWheel);
			if (btn) btn.innerHTML = '&#128269; Disable Zoom';
			if (fitBtn) fitBtn.style.display = '';
		} else {
			try { _aseShowplanZoom.reset(); } catch (ex) {} // snap back to normal before it's gone
			if (elem) elem.removeEventListener('wheel', _aseShowplanZoom.zoomWithWheel);
			try { _aseShowplanZoom.destroy(); } catch (ex) {} // also stop drag-to-pan
			_aseShowplanZoom = undefined;
			if (btn) btn.innerHTML = '&#128269; Enable Zoom';
			if (fitBtn) fitBtn.style.display = 'none';
		}
	};

	window.aseShowplanZoomToFit = function () {
		_panzoomZoomToFit(_aseShowplanZoom, document.getElementById('dbx-view-aseShowplan-graphContent'));
	};

	window.aseShowplanRedraw = function () {
		_aseShowplanRenderGraphicalPlan(_aseShowplanLastPlanText, _aseShowplanLastIsXml);
	};

	window.aseShowplanToggleOrientation = function () {
		_aseShowplanHorizontal = !_aseShowplanHorizontal;
		try { localStorage.setItem('dbxtune_aseShowplan_horizontal', _aseShowplanHorizontal ? 'true' : 'false'); } catch (ex) {}
		var btn = document.getElementById('dbx-view-aseShowplan-orientationBtn');
		if (btn) btn.innerHTML = _aseShowplanHorizontal ? '&#8646; Top-to-Bottom' : '&#8646; Left-to-Right';
		_aseShowplanRenderGraphicalPlan(_aseShowplanLastPlanText, _aseShowplanLastIsXml);
	};

	window.aseShowplanSetTableSizeWarnMb = function (val) {
		var parsed = parseFloat(val);
		if (isNaN(parsed) || parsed < 0) parsed = 100;
		_aseShowplanTableSizeWarnMb = parsed;
		try { localStorage.setItem('dbxtune_aseShowplan_tableSizeWarnMb', String(parsed)); } catch (ex) {}
		var input = document.getElementById('dbx-view-aseShowplan-warnMb');
		if (input) input.value = parsed;
		_aseShowplanRenderGraphicalPlan(_aseShowplanLastPlanText, _aseShowplanLastIsXml);
	};

	function _aseFmtUs(us) {
		if (us === undefined || us === null || isNaN(us)) return '?';
		if (us >= 1e6) return (us / 1e6).toFixed(1) + 's';
		return Math.round(us / 1000) + 'ms';
	}

	// "#d #h #m #s" - each larger unit only appears once its total has actually reached it (a
	// sub-minute total prints just "12s", not "0d 0h 0m 12s"), but once a larger unit is shown every
	// smaller one down to seconds is too (e.g. "1d 0h 5m 3s"), matching normal duration formatting.
	// No sub-second precision - the plan selector's total is a coarse "how much this variant costs
	// overall" figure, not a precise timing.
	function _aseFmtDurationUs(us) {
		if (us === undefined || us === null || isNaN(us)) return '?';
		// Round to whole seconds FIRST, then derive d/h/m/s from that integer - rounding only the
		// leftover seconds-remainder can push it to 60 (e.g. 59.9s -> "60s" instead of "1m 0s").
		var totalSec = Math.round(us / 1e6);
		var days  = Math.floor(totalSec / 86400);
		var hours = Math.floor((totalSec % 86400) / 3600);
		var mins  = Math.floor((totalSec % 3600) / 60);
		var secs  = totalSec % 60;
		var parts = [];
		if (days)                parts.push(days + 'd');
		if (days || hours)       parts.push(hours + 'h');
		if (days || hours || mins) parts.push(mins + 'm');
		parts.push(secs + 's');
		return parts.join(' ');
	}

	// Label for one <option> in the "Cached plan" selector - rank is 1-based position in the
	// (worst-total-time-first) parsed.plans[] array, not the plan's original document order.
	function _aseFmtPlanOptionLabel(plan, index, total) {
		var execCount = (plan.execCount !== undefined) ? plan.execCount.toLocaleString() : '?';
		return 'Plan ' + (index + 1) + ' of ' + total + ' — exec ' + execCount + '× @ '
			+ _aseFmtUs(plan.avgTimeUs) + ' avg (total ' + _aseFmtDurationUs(plan.totalTimeUs) + ') [planId ' + (plan.planId || '?') + ']';
	}

	// Switches which cached plan variant (see dbxShowplanAse.js's parseXml() `plans` array) is
	// shown, then fully re-renders from the last-loaded plan text/XML - simplest way to reuse
	// _aseShowplanRenderGraphicalPlan's existing parse+render pipeline unchanged.
	window.aseShowplanSelectPlan = function (idxStr) {
		_aseShowplanSelectedPlanIndex = parseInt(idxStr, 10) || 0;
		_aseShowplanRenderGraphicalPlan(_aseShowplanLastPlanText, _aseShowplanLastIsXml);
	};

	/**
	 * Parses planText (via AseShowplan.parseXml/parseText, see dbxShowplanAse.js) and renders the
	 * graphical tree into #dbx-view-aseShowplan-graphContent. Fails soft: on parse failure/exception
	 * the graphical section is hidden and a fallback notice points at the Raw Plan Text section,
	 * which is always populated independently of this function.
	 */
	function _aseShowplanRenderGraphicalPlan(planText, isXml) {
		_aseShowplanLastPlanText = planText;
		_aseShowplanLastIsXml    = isXml;

		var graphEl     = document.getElementById('dbx-view-aseShowplan-graphContent');
		var fallbackEl  = document.getElementById('dbx-view-aseShowplan-graphFallback');
		var orientBtn   = document.getElementById('dbx-view-aseShowplan-orientationBtn');
		var zoomBtn     = document.getElementById('dbx-view-aseShowplan-zoomBtn');
		var warnMbInput = document.getElementById('dbx-view-aseShowplan-warnMb');
		if (!graphEl) return;
		if (orientBtn) orientBtn.innerHTML = _aseShowplanHorizontal ? '&#8646; Top-to-Bottom' : '&#8646; Left-to-Right';
		if (warnMbInput) warnMbInput.value = _aseShowplanTableSizeWarnMb;
		// The zoom instance (if any) stays bound to this same persistent container across redraws -
		// see the hidden.bs.modal handler's comment - so its enabled/disabled state, and therefore
		// this label, doesn't reset on redraw/reopen; just keep it in sync with the real state.
		if (zoomBtn) zoomBtn.innerHTML = _aseShowplanZoom !== undefined ? '&#128269; Disable Zoom' : '&#128269; Enable Zoom';

		// Reset any Panzoom transform from a previous plan before re-rendering.
		if (_aseShowplanZoom !== undefined) { try { _aseShowplanZoom.reset(); } catch (ex) {} }

		var parsed = null;
		var parseError = null;
		try {
			if (typeof AseShowplan === 'undefined') {
				parseError = new Error('AseShowplan library not loaded');
			} else if (isXml) {
				parsed = AseShowplan.parseXml(planText);
			} else {
				parsed = AseShowplan.parseText(planText);
			}
		} catch (ex) {
			parsed = null;
			parseError = ex;
		}

		// parseXml()/parseText() return null (rather than throwing) when the input just isn't
		// recognized - see AseShowplan.getLastParseError() for why in that case.
		if (!parsed && !parseError && typeof AseShowplan !== 'undefined' && AseShowplan.getLastParseError) {
			var reason = AseShowplan.getLastParseError();
			if (reason) parseError = new Error(reason);
		}

		if (parsed) {
			try {
				// Table Information section (below) already stashed srv/dbname as data-* attributes
				// on this element when the dialog was opened (showAseShowplanDialog()/show.bs.modal
				// handler) - reused here so the graphical plan's per-operator tooltip can look up
				// live table size/rowcount too, without needing its own separate context param.
				var tiBody = document.getElementById('dbx-asp-tableinfo-body');
				var tiSrv     = tiBody ? tiBody.getAttribute('data-srv')     : '';
				var tiDbname  = tiBody ? tiBody.getAttribute('data-dbname')  : '';
				var tiSqlText = tiBody ? tiBody.getAttribute('data-sqltext') : '';

				var planSelectRow = document.getElementById('dbx-view-aseShowplan-planSelectRow');
				var planSelectEl  = document.getElementById('dbx-view-aseShowplan-planSelect');
				if (parsed.plans && parsed.plans.length > 1) {
					if (_aseShowplanSelectedPlanIndex >= parsed.plans.length) _aseShowplanSelectedPlanIndex = 0;
					if (planSelectEl) {
						planSelectEl.innerHTML = parsed.plans.map(function (p, i) {
							return '<option value="' + i + '">' + escapeHtml(_aseFmtPlanOptionLabel(p, i, parsed.plans.length)) + '</option>';
						}).join('');
						planSelectEl.value = String(_aseShowplanSelectedPlanIndex);
					}
					if (planSelectRow) planSelectRow.style.display = '';
					parsed.statements = parsed.plans[_aseShowplanSelectedPlanIndex].statements;
				} else if (planSelectRow) {
					planSelectRow.style.display = 'none';
				}

				AseShowplan.render(graphEl, parsed, {
					horizontal: _aseShowplanHorizontal,
					connectorStyle: 'lines', // SVG-drawn arrow connectors - was opt-in, now always on
					layout: 'compact', // was opt-in ("Default Layout" toggle), now always on
					srv: tiSrv,
					dbname: tiDbname,
					sqlText: tiSqlText, // used only for the Reformatting warning's index suggestion
					tableSizeWarnMb: _aseShowplanTableSizeWarnMb,
					onFindingsChanged: renderAseAnalysisSection
				});
				graphEl.style.display = '';
				if (fallbackEl) fallbackEl.style.display = 'none';
				return;
			} catch (ex) {
				parsed = null;
				parseError = ex;
			}
		}

		// Parse (or render) failed - hide the graphical section, point at the raw text instead.
		if (parseError) console.error('ASE Showplan graphical render failed:', parseError);
		$(graphEl).empty();
		graphEl.style.display = 'none';
		if (fallbackEl) {
			fallbackEl.style.display = '';
			var baseMsg = 'Could not parse this plan into a diagram &mdash; see "Raw Plan Text" below.';
			if (parseError) {
				var errMsg = (parseError && parseError.message) ? parseError.message : String(parseError);
				fallbackEl.innerHTML = baseMsg + '<div style="margin-top:4px;font-family:monospace;font-size:0.9em;color:#a33;">' + escapeHtml(errMsg) + '</div>';
			} else {
				fallbackEl.innerHTML = baseMsg;
			}
		}
	}

	window.aseShowplanFormatSql = function () {
		// See the matching comment on pgShowplanFormatSql() - without paramTypes.positional, a bare
		// "?" in the captured SQL throws a parse error here instead of formatting.
		var formatOptions = { language: 'tsql', tabWidth: 4, keywordCase: 'upper', tabulateAlias: true, paramTypes: { positional: true } };
		var sqlText = _aseStripDynamicSqlWrapper($('#dbx-view-aseShowplan-sqlContent').text());
		try {
			$('#dbx-view-aseShowplan-sqlContent').text(sqlFormatter.format(sqlText, formatOptions));
			Prism.highlightAll();
		} catch (err) { alert(err); }
	};

	window.aseShowplanCopySql = function () {
		var txt = $('#dbx-view-aseShowplan-sqlContent').text();
		var ta = document.createElement('textarea'); ta.value = txt; document.body.appendChild(ta); ta.select();
		try { document.execCommand('copy'); } catch (err) { alert('Unable to copy\n\n' + err); }
		document.body.removeChild(ta);
	};

	window.aseShowplanCopyPlan = function () {
		var txt = $('#dbx-view-aseShowplan-planContent').text();
		var ta = document.createElement('textarea'); ta.value = txt; document.body.appendChild(ta); ta.select();
		try { document.execCommand('copy'); } catch (err) { alert('Unable to copy\n\n' + err); }
		document.body.removeChild(ta);
	};

	window.aseShowplanOpenExternal = function () {
		var toUrl = '/showplan/ase';
		if (!window.location.toString().toLowerCase().startsWith('http')) toUrl = 'http://dbxtune.gorans.org' + toUrl;
		// Also carry sql/srv/dbname over so the standalone page's SQL Text/Table Information/LLM
		// sections work there too, instead of degrading to a bare plan+diagram view - same shared
		// context store (#dbx-asp-tableinfo-body's data-* attrs) _aseShowplanLoadLlmAdvice() reads.
		var tiBody = document.getElementById('dbx-asp-tableinfo-body');
		var srv    = tiBody ? (tiBody.getAttribute('data-srv')    || '') : '';
		var dbname = tiBody ? (tiBody.getAttribute('data-dbname') || '') : '';
		var sql    = $('#dbx-view-aseShowplan-sqlContent').text() || '';
		submit_post_via_hidden_form(toUrl, {
			plan:     _aseShowplanLastPlanText || '',
			isXml:    _aseShowplanLastIsXml ? 'true' : 'false',
			sql:      sql,
			dbVendor: 'Adaptive Server Enterprise',
			srv:      srv,
			dbname:   dbname
		});
	};

	/**
	 * (Re)loads the LLM Optimization Advice section for whatever plan/SQL is currently shown in the
	 * ASE dialog. Mirrors _ssShowplanLoadLlmAdvice - see that function's javadoc-style comment for
	 * why this is called from three places (first-expand toggle, manual refresh, dialog (re)open).
	 */
	function _aseShowplanLoadLlmAdvice(body) {
		body.setAttribute('data-loaded', 'true');

		if (typeof dbxLlmAdvice === 'undefined') {
			body.innerHTML = '<em style="color:#888;">dbxLlmAdvice.js is not loaded on this page.</em>';
			return;
		}

		// Reuse the same srv/dbname/sqltext/plan context stored on the Table Information section - the
		// srv/dbname-based DDL/index/stats lookup itself now lives in dbxLlmAdvice.js (fetchDdlContextBySrv),
		// shared with the Daily Summary Report's "Get LLM Optimization Advice" link.
		var tiBody  = document.getElementById('dbx-asp-tableinfo-body');
		var srv     = tiBody ? (tiBody.getAttribute('data-srv')     || '') : '';
		var dbname  = tiBody ? (tiBody.getAttribute('data-dbname')  || '') : '';
		var sqlText = tiBody ? (tiBody.getAttribute('data-sqltext') || '') : '';
		var planText = $('#dbx-view-aseShowplan-planContent').text();

		if (!sqlText) {
			body.innerHTML = '<em style="color:#888;">No SQL text is available for this plan.</em>';
			return;
		}

		// open() itself renders a "Looking up table DDL/index/stats..." status while srv/dbname are set
		// and ddlContext isn't - no need to duplicate that here.
		var workloadData = tiBody ? (tiBody.getAttribute('data-workloaddata') || '') : '';
		dbxLlmAdvice.open({ sql: sqlText, plan: planText, dbVendor: 'Adaptive Server Enterprise', srv: srv, dbname: dbname, workloadData: workloadData, target: body });
	}

	// Manual re-run - ignores the data-loaded guard (unlike the toggle handler) since this is an
	// explicit user action.
	window.aseShowplanRefreshLlmAdvice = function() {
		var body = document.getElementById('dbx-asp-llm-body');
		if (body) _aseShowplanLoadLlmAdvice(body);
	};

	/**
	 * "No exec" counterpart to _aseShowplanLoadLlmAdvice() above - same DDL-context lookup, but
	 * finishes with a POST ...&preview:true to /api/llm/optimize-sql (LlmSqlOptimizeServlet), which
	 * builds the prompt text locally and returns it without ever calling an LLM provider, so this
	 * works even when DbxCentral.llm.enabled is off. See the dbxLlmAdvice.isEnabled() toggle below
	 * for which of this section vs. the real Advice section above is shown.
	 */
	function _aseShowplanLoadLlmPreview(body) {
		body.setAttribute('data-loaded', 'true');

		var tiBody  = document.getElementById('dbx-asp-tableinfo-body');
		var srv     = tiBody ? (tiBody.getAttribute('data-srv')     || '') : '';
		var dbname  = tiBody ? (tiBody.getAttribute('data-dbname')  || '') : '';
		var sqlText = tiBody ? (tiBody.getAttribute('data-sqltext') || '') : '';
		var planText = $('#dbx-view-aseShowplan-planContent').text();

		if (!sqlText) {
			body.innerHTML = '<em style="color:#888;">No SQL text is available for this plan.</em>';
			return;
		}

		function buildPreview(ddlContext) {
			body.innerHTML = '<span style="color:#888;font-size:0.85em;">&#9203; Building prompt…</span>';
			// Same workload profile open() would send, so the PREVIEW matches what actually gets sent.
			var wlRaw = tiBody ? (tiBody.getAttribute('data-workloaddata') || '') : '';
			var wlProfile = (wlRaw && window.dbxLlmAdvice && dbxLlmAdvice.buildWorkloadProfile)
					? dbxLlmAdvice.buildWorkloadProfile(wlRaw) : '';
			$.ajax({
				url:         '/api/llm/optimize-sql',
				method:      'POST',
				contentType: 'application/json',
				data:        JSON.stringify({ sql: sqlText, ddlContext: ddlContext || '', plan: planText, dbVendor: 'Adaptive Server Enterprise', workloadProfile: wlProfile, preview: true }),
				dataType:    'json',
				success: function(r) {
					if (r && r.promptSent) {
						var esc = function(s) { return s.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;'); };
						body.innerHTML = '<pre id="dbx-asp-llmpreview-text" class="mb-0" style="white-space:pre-wrap;max-height:400px;overflow:auto;">' + esc(r.promptSent) + '</pre>';
					} else {
						body.innerHTML = '<em style="color:#888;">Could not build a prompt preview.</em>';
					}
				},
				error: function(xhr) { body.innerHTML = '<span class="text-danger">Failed to build prompt: HTTP ' + xhr.status + '</span>'; }
			});
		}

		if (!srv || !dbname) {
			buildPreview('');
			return;
		}

		body.innerHTML = '<span style="color:#888;font-size:0.85em;">&#9203; Parsing SQL…</span>';
		DbxSqlTableNames.extractTablesAsync(sqlText, function(tables) {
			if (!tables.length) { buildPreview(''); return; }
			body.innerHTML = '<span style="color:#888;font-size:0.85em;">&#9203; Looking up table DDL/index/stats…</span>';
			$.ajax({
				url:      '/api/cc/mgt/table-info',
				data:     { srv: srv, dbVendor: 'Adaptive Server Enterprise', format: 'text', dbname: dbname, tables: tables.join(',') },
				dataType: 'json',
				success:  function(r) { buildPreview((r && r.text) || ''); },
				error:    function() { buildPreview(''); }
			});
		});
	}

	// Manual re-run - ignores the data-loaded guard, same rationale as aseShowplanRefreshLlmAdvice.
	window.aseShowplanRefreshLlmPreview = function() {
		var body = document.getElementById('dbx-asp-llmpreview-body');
		if (body) _aseShowplanLoadLlmPreview(body);
	};

	window.aseShowplanCopyLlmPreview = function() {
		var el = document.getElementById('dbx-asp-llmpreview-text');
		if (!el) { alert('Build the prompt first.'); return; }
		var ta = document.createElement('textarea'); ta.value = el.textContent; document.body.appendChild(ta); ta.select();
		try { document.execCommand('copy'); } catch (err) { alert('Unable to copy\n\n' + err); }
		document.body.removeChild(ta);
	};

	/**
	 * Open the ASE Showplan dialog programmatically. Mirrors showSqlServerShowplanDialog - see that
	 * function's comment for the relatedTarget vs. programmatic-call distinction.
	 *
	 * @param {string} planText     — plan text (sp_showplan) or XML (show_cached_plan_in_xml)
	 * @param {string} [sqlText]    — SQL text to display on the SQL Text section
	 * @param {boolean} [isXml]     — true if planText is XML (CachedPlanInXml), false for plain ShowPlanText
	 * @param {string} [objectName] — optional label shown in the header
	 * @param {Object} [meta]       — optional metadata: { srv, dbname } - used for lazy-loading Table Information / LLM Advice
	 */
	window.showAseShowplanDialog = function(planText, sqlText, isXml, objectName, meta) {
		var $dlg = $('#dbx-view-aseShowplan-dialog');
		if (!$dlg.length) {
			_injectHtml();
			_initHandlers();
			setTimeout(function() { window.showAseShowplanDialog(planText, sqlText, isXml, objectName, meta); }, 50);
			return;
		}
		if (!planText) return;

		$('#dbx-view-aseShowplan-objectName', $dlg).text(objectName || '');
		$('#dbx-view-aseShowplan-plan-summary', $dlg).html(isXml ? '&#128202; Graphical Plan (Cached Plan XML)' : '&#128202; Graphical Plan (sp_showplan)');

		var $planEl = $('#dbx-view-aseShowplan-planContent', $dlg);
		$planEl.text(isXml ? formatXml(planText) : _aseStripHtmlWrapper(planText));
		$planEl.attr('class', isXml ? 'language-xml line-numbers dbx-view-sqltext-content' : 'line-numbers dbx-view-sqltext-content');
		$('#dbx-view-aseShowplan-sqlContent', $dlg).text(_aseStripHtmlWrapper(sqlText) || '');
		_aseShowplanSelectedPlanIndex = 0; // new plan loaded - default back to the worst-total-time one
		// NOT rendered here if the dialog isn't already open - see the matching, more detailed
		// comment on the show.bs.modal handler above: measuring box positions/sizes against a still-
		// hidden container produces overlapping boxes and invisible (zero-length) connector lines.
		// Deferred into _drawAndHighlight() below, called either immediately (dialog's already open
		// and visible, so measuring now is fine) or once shown.bs.modal confirms it actually is.

		// Reset Table Information section — store context for lazy loading on first expand
		var tiSect = document.getElementById('dbx-asp-sect-tableinfo');
		var tiBody = document.getElementById('dbx-asp-tableinfo-body');
		if (tiSect && tiBody) {
			var tiSrv = (meta && meta.srv)    ? meta.srv    : '';
			var tiDb  = (meta && meta.dbname) ? meta.dbname : '';
			tiBody.setAttribute('data-srv',     tiSrv);
			tiBody.setAttribute('data-dbname',  tiDb);
			tiBody.setAttribute('data-sqltext', sqlText || '');
			// Execution statistics for this statement over the recording period, forwarded from the
			// Daily Summary Report (see SparklineHelper.getWorkloadHarvesterJs / ShowplanAseServlet).
			// The LLM Advice section below reads it back out of here.
			tiBody.setAttribute('data-workloaddata', (meta && meta.workloadData) ? meta.workloadData : '');
			tiBody.setAttribute('data-loaded',  'false');
			tiBody.innerHTML = '<span style="color:#888;font-size:0.85em;">&#9203; Loading table information…</span>';
			tiSect.style.display = (tiSrv && tiDb) ? '' : 'none';
			tiSect.removeAttribute('open');
		}

		// Reset LLM Optimization Advice section — lazy-loaded on next expand, same as Table Information
		var llmSect = document.getElementById('dbx-asp-sect-llm');
		var llmBody = document.getElementById('dbx-asp-llm-body');
		if (llmSect && llmBody) {
			llmBody.setAttribute('data-loaded', 'false');
			llmBody.innerHTML = '<span style="color:#888;font-size:0.85em;">Expand to ask an LLM for optimization advice.</span>';
			llmSect.removeAttribute('open');
		}

		// Reset LLM Prompt Preview section — same as above
		var llmPreviewSect = document.getElementById('dbx-asp-sect-llmpreview');
		var llmPreviewBody = document.getElementById('dbx-asp-llmpreview-body');
		if (llmPreviewSect && llmPreviewBody) {
			llmPreviewBody.setAttribute('data-loaded', 'false');
			llmPreviewBody.innerHTML = '<span style="color:#888;font-size:0.85em;">Expand to build the prompt.</span>';
			llmPreviewSect.removeAttribute('open');
		}

		function _drawAndHighlight() {
			_aseShowplanRenderGraphicalPlan(planText, isXml);
			if (typeof Prism !== 'undefined') Prism.highlightAll();
		}

		if ($dlg.hasClass('show')) {
			_drawAndHighlight();
		} else {
			$dlg.one('shown.bs.modal', _drawAndHighlight);
			$dlg.modal('show');
		}
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
		// Idempotent-ish: _initHandlers() can run more than once (see its call sites), and re-binding
		// the splitter's mousedown would just add a duplicate listener that does the same thing, so
		// guard it rather than stacking handlers.
		if (!_ssShowplanSplitterInited) {
			_ssShowplanSplitterInited = true;
			_ssShowplanInitPropsSplitter();
			_ssShowplanInitDragScroll();
			_ssShowplanSyncToolbar();
		}


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

		// SQL Server: destroy all objects on close to free memory
		$('#dbx-view-ssShowplan-dialog').on('hidden.bs.modal', function () {
			// Was a bare destroy() here, which - same as the ASE dialog's now-fixed equivalent -
			// doesn't remove the wheel listener (that's attached by this file, not by Panzoom itself),
			// only the drag-to-pan handlers. #dbx-view-ssShowplan-content is the persistent Panzoom
			// target and isn't removed/recreated on close (only its innerHTML gets cleared below), so
			// the leaked listener would silently keep responding to wheel events after close and a
			// second one would stack on top of it the next time zoom was enabled.
			_ssShowplanDisableZoom();
			// The native renderer's operator detail panels are attached to <body> (so the diagram's
			// own scroll container can't clip them), which means closing the dialog does not take
			// them with it - a pinned panel would be left floating over the page.
			if (window.SqlServerShowplan && window.SqlServerShowplan.closePanels) {
				try { window.SqlServerShowplan.closePanels(); } catch (ex) {}
			}
			if (_sspWaitChart) {
				try { _sspWaitChart.destroy(); } catch(e) {}
				_sspWaitChart = null;
			}
			var planEl = document.getElementById('dbx-view-ssShowplan-content');
			if (planEl) planEl.innerHTML = '';
			var bodyEl = document.getElementById('dbx-view-ssShowplan-analysis-body');
			if (bodyEl) bodyEl.innerHTML = '';
			var analysisEl = document.getElementById('dbx-view-ssShowplan-analysis');
			if (analysisEl) analysisEl.style.display = 'none';
		});

		// SQL Server: set fields before modal becomes visible
		$('#dbx-view-ssShowplan-dialog').on('show.bs.modal', function (e) {
			// When opened programmatically (showSqlServerShowplanDialog) relatedTarget
			// is undefined — fields are already populated there, so skip.
			if (!e.relatedTarget) return;
			var data = $(e.relatedTarget).data();
			$('#dbx-view-ssShowplan-objectName', this).text(data.objectname);
			_ssShowplanSetPlaceholder('Loading plan...');
			$('#dbx-view-ssShowplan-analysis',   this).hide();
			$('#dbx-view-ssShowplan-xmlContent', this).text(formatXml(data.tooltip));
			$('#dbx-view-ssShowplan-sqlContent', this).text(ssShowplanGetSql(data.sqltext));
			ssShowplanSetPlanType(data.tooltip);
			ssShowplanGetParameters();
			_ssShowplanDisableZoom();
			// Set Table Information context from trigger element's data attributes (srv/dbname/ts)
			var tiSect = document.getElementById('dbx-ssp-sect-tableinfo');
			var tiBody = document.getElementById('dbx-ssp-tableinfo-body');
			if (tiSect && tiBody) {
				var tiSrv = data.srv    || '';
				var tiDb  = data.dbname || '';
				var tiTs  = data.ts     || '';
				var tiSql = data.sqltext || '';
				tiBody.setAttribute('data-srv',     tiSrv);
				tiBody.setAttribute('data-dbname',  tiDb);
				tiBody.setAttribute('data-ts',      tiTs);
				tiBody.setAttribute('data-sqltext', tiSql);
				tiBody.setAttribute('data-loaded',  'false');
				tiBody.innerHTML = '<span style="color:#888;font-size:0.85em;">&#9203; Loading table information…</span>';
				if (tiSrv && tiDb) {
					tiSect.style.display = '';
				} else {
					tiSect.style.display = 'none';
				}
				tiSect.removeAttribute('open');
			}

			// Reset LLM Optimization Advice section — lazy-loaded on next expand, same as Table Information.
			// (This data-toggle="modal" path is separate from showSqlServerShowplanDialog() below, which
			// has its own identical reset - both need it since either can be how this dialog gets shown.)
			var llmSect = document.getElementById('dbx-ssp-sect-llm');
			var llmBody = document.getElementById('dbx-ssp-llm-body');
			if (llmSect && llmBody) {
				llmBody.setAttribute('data-loaded', 'false');
				llmBody.innerHTML = '<span style="color:#888;font-size:0.85em;">Expand to ask an LLM for optimization advice.</span>';
				llmSect.removeAttribute('open');
			}

			var llmPreviewSect = document.getElementById('dbx-ssp-sect-llmpreview');
			var llmPreviewBody = document.getElementById('dbx-ssp-llmpreview-body');
			if (llmPreviewSect && llmPreviewBody) {
				llmPreviewBody.setAttribute('data-loaded', 'false');
				llmPreviewBody.innerHTML = '<span style="color:#888;font-size:0.85em;">Expand to build the prompt.</span>';
				llmPreviewSect.removeAttribute('open');
			}
		});

		// SQL Server: draw plan after modal is visible
		$('#dbx-view-ssShowplan-dialog').on('shown.bs.modal', function (e) {
			var $modal = $(this);
			var $dlg   = $modal.find('.modal-dialog');
			var $cont  = $modal.find('.modal-content');

			// A one-shot "Try Switch to: html-query-plan" from a previous open (see
			// ssShowplanTryQpOnce()) never outlives that open - put the real persisted renderer back
			// now, before this open draws anything.
			if (_ssShowplanRendererIsTemp) {
				_ssShowplanRendererIsTemp = false;
				try {
					var _ssR3 = localStorage.getItem('dbxtune_ssShowplan_renderer');
					_ssShowplanRenderer = (_ssR3 === 'dbx' || _ssR3 === 'qp') ? _ssR3 : 'dbx';
				} catch (ex) { _ssShowplanRenderer = 'dbx'; }
			}

			// Reset scroll position now that the modal is actually visible - so a plan viewed scrolled
			// deep into e.g. "XML Plan" from a previous open doesn't leave the *next* plan (even a
			// totally different one) opening already scrolled past "Execution Plan" at the top. Doing
			// this in show.bs.modal (before display flips from none to block) doesn't reliably stick -
			// confirmed via testing that some browsers restore a display:none element's last-known
			// scroll position once it becomes visible again, silently undoing an earlier reset.
			$modal.find('.modal-body').scrollTop(0).scrollLeft(0);

			// ── Default size (always the same - not remembered across opens/reopens) ──
			// Position IS still remembered (see savedL/savedT below) - only size resets every time, so
			// the dialog doesn't come back tiny/huge from a one-off resize made while inspecting a
			// particularly small/large plan. Key prefix includes screen resolution so position is
			// remembered independently per screen — avoids misplaced dialogs when switching workstations.
			var _scrPfx = screen.width + 'x' + screen.height + '_';
			var savedL = null, savedT = null;
			try {
				savedL = localStorage.getItem(_scrPfx + 'ssShowplan-dlg-left');
				savedT = localStorage.getItem(_scrPfx + 'ssShowplan-dlg-top');
			} catch(ex) {}
			var w = Math.round(window.innerWidth  * 0.90);
			var h = Math.round(window.innerHeight * 0.88);
			// Bootstrap's own .modal-content CSS is `position: relative`, which resizable's north/west
			// handles exploit by shifting it via CSS left/top to make the dialog grow "backwards" while
			// keeping the opposite edge fixed - normal jQuery UI behavior. But .modal-content's parent
			// (.modal-dialog, $dlg) doesn't move to match: $dlg only ever gets repositioned by our OWN
			// draggable `stop` handler below, and a relatively-positioned child doesn't pull its parent
			// along when shifted. Left alone, a north/west resize would visually work for that one drag
			// but leave $dlg's tracked position out of sync with where the dialog actually renders -
			// corrupting the *next* thing that reads $dlg's left/top (a later drag, the expand/restore
			// button, or the localStorage-persisted reopen position). Explicitly pinning $cont to
			// `position:absolute; top:0; left:0` here (reasserted on every open) plus folding any
			// resize-induced offset back into $dlg in the resizable `stop` handler below keeps $dlg as
			// the single source of truth for "where the dialog is", the same way it already is for drag.
			$cont.css({ width: w + 'px', height: h + 'px', position: 'absolute', top: '0', left: '0' });

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
							localStorage.setItem(_scrPfx + 'ssShowplan-dlg-left', Math.round(ui.position.left));
							localStorage.setItem(_scrPfx + 'ssShowplan-dlg-top',  Math.round(ui.position.top));
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
						// North/west drags shifted $cont's own left/top (see the comment on $cont's
						// initial position:absolute setup above) - fold that into $dlg's tracked
						// position and reset $cont back to flush-with-$dlg, so $dlg stays the single
						// source of truth other code (drag, expand/restore, reopen) relies on.
						var contLeft = parseFloat($cont.css('left')) || 0;
						var contTop  = parseFloat($cont.css('top'))  || 0;
						if (contLeft || contTop) {
							var newLeft = parseFloat($dlg.css('left')) + contLeft;
							var newTop  = parseFloat($dlg.css('top'))  + contTop;
							$dlg.css({ left: newLeft + 'px', top: newTop + 'px' });
							$cont.css({ left: '0', top: '0' });
						}
						try {
							localStorage.setItem(_scrPfx + 'ssShowplan-dlg-left', Math.round(parseFloat($dlg.css('left'))));
							localStorage.setItem(_scrPfx + 'ssShowplan-dlg-top',  Math.round(parseFloat($dlg.css('top'))));
						} catch(ex) {}
					}
				});
			}


			// ── Auto-expand on open ─────────────────────────────────────────────
			// Every fresh open of this dialog should start in the same state a manual click of the
			// header's expand/restore button would leave it in - the 90%/88% default above is still
			// comfortably too small once a plan of any real size is on screen. Reusing the SAME toggle
			// (rather than just setting a bigger default width/height directly) means the "restore"
			// half keeps working normally afterward: clicking it snaps back to exactly the default
			// size/position just applied above, not to whatever the dialog happened to be at on some
			// earlier open. removeData() first is required, not optional: this dialog element is
			// created once and only shown/hidden (never destroyed - see _injectHtml()), so a leftover
			// 'expanded' flag from an EARLIER open in this same page session (set the moment THAT open
			// auto-expanded, never cleared unless the user manually clicked restore) would otherwise
			// make the toggle collapse the dialog instead of expanding it the second time around.
			$dlg.removeData('expanded');
			ssShowplanToggleExpand();

			// ── Draw plan (data-toggle path only) ────────────────────────────
			if (!e.relatedTarget) return;
			var data = $(e.relatedTarget).data();
			if (typeof Prism !== 'undefined') Prism.highlightAll();
			// Draws via whichever renderer is selected AND runs the analysis - see ssShowplanRunAnalysis().
			ssShowplanRunAnalysis(data.tooltip);
		});

		// ASE: destroy nothing special on close - just clear content so a stale plan never flashes
		// before the next open populates it.
		$('#dbx-view-aseShowplan-dialog').on('hidden.bs.modal', function () {
			var planEl = document.getElementById('dbx-view-aseShowplan-planContent');
			if (planEl) planEl.textContent = '';
			var graphEl = document.getElementById('dbx-view-aseShowplan-graphContent');
			if (graphEl) $(graphEl).empty();
		});

		// ASE: set fields before modal becomes visible (data-toggle="modal" path)
		$('#dbx-view-aseShowplan-dialog').on('show.bs.modal', function (e) {
			// When opened programmatically (showAseShowplanDialog) relatedTarget is undefined —
			// fields are already populated there, so skip (mirrors the SQL Server dialog above).
			if (!e.relatedTarget) return;
			var data = $(e.relatedTarget).data();
			var isXml = data.planisxml === true || data.planisxml === 'true';

			$('#dbx-view-aseShowplan-objectName', this).text(data.objectname || '');
			$('#dbx-view-aseShowplan-plan-summary', this).html(isXml ? '&#128202; Graphical Plan (Cached Plan XML)' : '&#128202; Graphical Plan (sp_showplan)');

			var $planEl = $('#dbx-view-aseShowplan-planContent', this);
			$planEl.text(isXml ? formatXml(data.plan) : _aseStripHtmlWrapper(data.plan));
			$planEl.attr('class', isXml ? 'language-xml line-numbers dbx-view-sqltext-content' : 'line-numbers dbx-view-sqltext-content');
			$('#dbx-view-aseShowplan-sqlContent', this).text(_aseStripHtmlWrapper(data.sqltext) || '');
			// NOT rendered here - show.bs.modal fires before the modal is actually visible (still
			// display:none / mid-transition), so #dbx-view-aseShowplan-graphContent measures as
			// zero-size at this point: tucking positions everything from bogus offsets (overlapping
			// boxes) and every connector line degenerates to a zero-length path (no visible arrows).
			// Confirmed by the user seeing exactly that from the Active Statements trigger, self-
			// correcting the moment Redraw/an orientation toggle re-measures against the now-fully-
			// shown container. Stash for the shown.bs.modal handler below to actually render once the
			// container has real dimensions - same two vars _aseShowplanRenderGraphicalPlan() itself
			// sets as its first step, so Redraw/toggle-orientation keep working unchanged either way.
			_aseShowplanLastPlanText = data.plan;
			_aseShowplanLastIsXml    = isXml;
			_aseShowplanSelectedPlanIndex = 0; // new plan loaded - default back to the worst-total-time one

			var tiSect = document.getElementById('dbx-asp-sect-tableinfo');
			var tiBody = document.getElementById('dbx-asp-tableinfo-body');
			if (tiSect && tiBody) {
				var tiSrv = data.srv    || '';
				var tiDb  = data.dbname || '';
				tiBody.setAttribute('data-srv',     tiSrv);
				tiBody.setAttribute('data-dbname',  tiDb);
				tiBody.setAttribute('data-sqltext', data.sqltext || '');
				tiBody.setAttribute('data-loaded',  'false');
				tiBody.innerHTML = '<span style="color:#888;font-size:0.85em;">&#9203; Loading table information…</span>';
				tiSect.style.display = (tiSrv && tiDb) ? '' : 'none';
				tiSect.removeAttribute('open');
			}

			var llmSect = document.getElementById('dbx-asp-sect-llm');
			var llmBody = document.getElementById('dbx-asp-llm-body');
			if (llmSect && llmBody) {
				llmBody.setAttribute('data-loaded', 'false');
				llmBody.innerHTML = '<span style="color:#888;font-size:0.85em;">Expand to ask an LLM for optimization advice.</span>';
				llmSect.removeAttribute('open');
			}

			var llmPreviewSect = document.getElementById('dbx-asp-sect-llmpreview');
			var llmPreviewBody = document.getElementById('dbx-asp-llmpreview-body');
			if (llmPreviewSect && llmPreviewBody) {
				llmPreviewBody.setAttribute('data-loaded', 'false');
				llmPreviewBody.innerHTML = '<span style="color:#888;font-size:0.85em;">Expand to build the prompt.</span>';
				llmPreviewSect.removeAttribute('open');
			}
		});

		// ASE: draggable/resizable, position/size persisted per-screen-resolution - same pattern as
		// the SQL Server dialog above, separate localStorage key prefix so the two don't collide.
		$('#dbx-view-aseShowplan-dialog').on('shown.bs.modal', function (e) {
			// Actual render happens here, not in show.bs.modal - see the matching comment there.
			// showAseShowplanDialog() (the programmatic entry point, relatedTarget undefined here same
			// as in show.bs.modal) renders itself via its own one-time shown.bs.modal handler instead,
			// so skip here to avoid rendering twice.
			if (e.relatedTarget) _aseShowplanRenderGraphicalPlan(_aseShowplanLastPlanText, _aseShowplanLastIsXml);

			var $modal = $(this);
			var $dlg   = $modal.find('.modal-dialog');
			var $cont  = $modal.find('.modal-content');

			// See the matching comment on the SQL Server dialog's shown.bs.modal handler above.
			$modal.find('.modal-body').scrollTop(0).scrollLeft(0);

			// Size is always the default below (not remembered) - see the matching comment on the SQL
			// Server dialog's setup above. Position still is (savedL/savedT).
			var _scrPfx = screen.width + 'x' + screen.height + '_';
			var savedL = null, savedT = null;
			try {
				savedL = localStorage.getItem(_scrPfx + 'aseShowplan-dlg-left');
				savedT = localStorage.getItem(_scrPfx + 'aseShowplan-dlg-top');
			} catch(ex) {}
			var w = Math.round(window.innerWidth  * 0.80);
			var h = Math.round(window.innerHeight * 0.80);
			// See the matching comment on the SQL Server dialog's setup above: pinning $cont to
			// position:absolute;top:0;left:0 (reasserted on every open) plus folding any resize-induced
			// offset back into $dlg in the resizable `stop` handler below keeps $dlg as the single
			// source of truth for "where the dialog is" even after a north/west resize.
			$cont.css({ width: w + 'px', height: h + 'px', position: 'absolute', top: '0', left: '0' });

			$dlg.removeClass('modal-dialog-centered');
			$dlg.css({ margin: '0', 'max-width': 'none', position: 'absolute' });
			$modal.css({ overflow: 'hidden' });

			var l = savedL ? parseInt(savedL) : Math.round((window.innerWidth  - w) / 2);
			var t = savedT ? parseInt(savedT) : Math.round((window.innerHeight - h) / 2);
			l = Math.max(0, Math.min(l, window.innerWidth  - 120));
			t = Math.max(0, Math.min(t, window.innerHeight -  60));
			$dlg.css({ left: l + 'px', top: t + 'px' });

			if ($.fn.draggable && !$dlg.hasClass('ui-draggable')) {
				$dlg.draggable({
					handle: '.modal-header',
					stop: function(ev, ui) {
						try {
							localStorage.setItem(_scrPfx + 'aseShowplan-dlg-left', Math.round(ui.position.left));
							localStorage.setItem(_scrPfx + 'aseShowplan-dlg-top',  Math.round(ui.position.top));
						} catch(ex) {}
					}
				});
			}

			if ($.fn.resizable && !$cont.hasClass('ui-resizable')) {
				$cont.resizable({
					handles:   'all',
					minWidth:  500,
					minHeight: 300,
					stop: function(ev, ui) {
						// See the matching comment on the SQL Server dialog's resizable `stop` above.
						var contLeft = parseFloat($cont.css('left')) || 0;
						var contTop  = parseFloat($cont.css('top'))  || 0;
						if (contLeft || contTop) {
							var newLeft = parseFloat($dlg.css('left')) + contLeft;
							var newTop  = parseFloat($dlg.css('top'))  + contTop;
							$dlg.css({ left: newLeft + 'px', top: newTop + 'px' });
							$cont.css({ left: '0', top: '0' });
						}
						try {
							localStorage.setItem(_scrPfx + 'aseShowplan-dlg-left', Math.round(parseFloat($dlg.css('left'))));
							localStorage.setItem(_scrPfx + 'aseShowplan-dlg-top',  Math.round(parseFloat($dlg.css('top'))));
						} catch(ex) {}
					}
				});
			}


			// Auto-expand on open - see the matching comment on the SQL Server dialog's handler above
			// for why removeData() first is required (this dialog element is shown/hidden, never
			// destroyed, so a stale flag from an earlier open in the same session would otherwise
			// collapse it instead of expanding it).
			$dlg.removeData('expanded');
			aseShowplanToggleExpand();

			if (typeof Prism !== 'undefined') Prism.highlightAll();
		});

		// ASE Table Information: lazy-load on first expand
		document.getElementById('dbx-asp-sect-tableinfo').addEventListener('toggle', function() {
			if (!this.open) return;                          // closing — do nothing
			var body = document.getElementById('dbx-asp-tableinfo-body');
			if (!body || body.getAttribute('data-loaded') === 'true') return;  // already loaded

			var srv     = body.getAttribute('data-srv')     || '';
			var dbname  = body.getAttribute('data-dbname')  || '';
			var sqlText = body.getAttribute('data-sqltext') || '';

			if (!srv || !dbname) {
				body.innerHTML = '<em style="color:#888;">Table information is not available — no server context.</em>';
				body.setAttribute('data-loaded', 'true');
				return;
			}

			body.innerHTML = '<span style="color:#888;font-size:0.85em;">&#9203; Parsing SQL…</span>';

			DbxSqlTableNames.extractTablesAsync(sqlText, function(tables) {
				if (!tables.length) {
					body.setAttribute('data-loaded', 'true');
					body.innerHTML = '<em style="color:#888;">No tables could be parsed from the SQL text.</em>';
					return;
				}

				body.innerHTML = '<span style="color:#888;font-size:0.85em;">&#9203; Loading table information…</span>';

				$.ajax({
					url:      '/api/cc/mgt/table-info',
					data:     { srv: srv, dbVendor: 'Adaptive Server Enterprise', dbname: dbname, tables: tables.join(',') },
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

		// ASE LLM Optimization Advice: lazy-load on first expand, rendered inline
		document.getElementById('dbx-asp-sect-llm').addEventListener('toggle', function() {
			if (!this.open) return;                           // closing — do nothing
			var body = document.getElementById('dbx-asp-llm-body');
			if (!body || body.getAttribute('data-loaded') === 'true') return;  // already loaded
			_aseShowplanLoadLlmAdvice(body);
		});

		// ASE LLM Prompt Preview: same lazy-load pattern, always available alongside the Advice
		// section above (see the isEnabled() toggle just below).
		document.getElementById('dbx-asp-sect-llmpreview').addEventListener('toggle', function() {
			if (!this.open) return;
			var body = document.getElementById('dbx-asp-llmpreview-body');
			if (!body || body.getAttribute('data-loaded') === 'true') return;
			_aseShowplanLoadLlmPreview(body);
		});

		// Feature-toggle gated (DbxCentral.llm.enabled) - same rationale as the SQL Server section
		// below. The Advice section only makes sense (i.e. can call a provider) when the feature is
		// on; the Preview section never calls a provider at all, so it's shown either way - just
		// with its note text swapped to explain why it's useful in each case.
		if (typeof dbxLlmAdvice !== 'undefined') {
			dbxLlmAdvice.isEnabled().then(function(enabled) {
				var llmSect        = document.getElementById('dbx-asp-sect-llm');
				var llmPreviewSect = document.getElementById('dbx-asp-sect-llmpreview');
				var llmPreviewNote = document.getElementById('dbx-asp-llmpreview-note');
				if (llmSect)        llmSect.style.display        = enabled ? '' : 'none';
				if (llmPreviewSect) llmPreviewSect.style.display = '';
				if (llmPreviewNote) {
					llmPreviewNote.textContent = enabled
						? 'Build the exact prompt that would be sent to the LLM without actually sending it - handy to see what context gets included, or to paste into your own LLM chat instead of using the Advice above.'
						: "This DbxCentral instance doesn't have LLM Optimization Advice turned on, so nothing gets sent anywhere - but here's the exact prompt it would send. Copy it into any LLM chat (claude.ai, chatgpt.com, ...) yourself for a quick manual shortcut into the same advice.";
				}
			});
		}

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

			DbxSqlTableNames.extractTablesAsync(sqlText, function(tables) {
				if (!tables.length) {
					body.setAttribute('data-loaded', 'true');
					body.innerHTML = '<em style="color:#888;">No tables could be parsed from the SQL text.</em>';
					return;
				}

				body.innerHTML = '<span style="color:#888;font-size:0.85em;">&#9203; Loading table information…</span>';

				// Vendor-generic /api/cc/mgt/table-info rather than QueryStoreServlet's SQL-Server-only
				// action=tableInfo: same DDL Storage, same rendered HTML, but the endpoint the ASE dialog
				// already uses - and the one that also serves format=json for the graphical plan's
				// per-operator lookups, so this dialog now talks to a single table-info endpoint.
				$.ajax({
					url:      '/api/cc/mgt/table-info',
					data:     { srv: srv, dbVendor: 'Microsoft SQL Server', dbname: dbname, tables: tables.join(','), ts: ts },
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

		// LLM Optimization Advice: lazy-load on first expand, rendered inline (not a nested popup)
		document.getElementById('dbx-ssp-sect-llm').addEventListener('toggle', function() {
			if (!this.open) return;                           // closing — do nothing
			var body = document.getElementById('dbx-ssp-llm-body');
			if (!body || body.getAttribute('data-loaded') === 'true') return;  // already loaded
			_ssShowplanLoadLlmAdvice(body);
		});

		// LLM Prompt Preview: same lazy-load pattern, shown instead of the section above when the
		// feature is off (see the isEnabled() toggle just below).
		document.getElementById('dbx-ssp-sect-llmpreview').addEventListener('toggle', function() {
			if (!this.open) return;
			var body = document.getElementById('dbx-ssp-llmpreview-body');
			if (!body || body.getAttribute('data-loaded') === 'true') return;
			_ssShowplanLoadLlmPreview(body);
		});

		// Feature-toggle gated (DbxCentral.llm.enabled) - the section is baked into the injected HTML
		// once, so rather than not injecting it at all, just hide it entirely when disabled (idempotent,
		// safe to run every time _initHandlers() runs). The Advice section only makes sense (i.e. can
		// call a provider) when the feature is on; the Preview section never calls a provider at all,
		// so it's shown either way - just with its note text swapped to explain why it's useful in
		// each case.
		if (typeof dbxLlmAdvice !== 'undefined') {
			dbxLlmAdvice.isEnabled().then(function(enabled) {
				var llmSect        = document.getElementById('dbx-ssp-sect-llm');
				var llmPreviewSect = document.getElementById('dbx-ssp-sect-llmpreview');
				var llmPreviewNote = document.getElementById('dbx-ssp-llmpreview-note');
				if (llmSect)        llmSect.style.display        = enabled ? '' : 'none';
				if (llmPreviewSect) llmPreviewSect.style.display = '';
				if (llmPreviewNote) {
					llmPreviewNote.textContent = enabled
						? 'Build the exact prompt that would be sent to the LLM without actually sending it - handy to see what context gets included, or to paste into your own LLM chat instead of using the Advice above.'
						: "This DbxCentral instance doesn't have LLM Optimization Advice turned on, so nothing gets sent anywhere - but here's the exact prompt it would send. Copy it into any LLM chat (claude.ai, chatgpt.com, ...) yourself for a quick manual shortcut into the same advice.";
				}
			});
		}
	}

	/**
	 * (Re)loads the LLM Optimization Advice section for whatever plan/SQL is currently shown in the
	 * dialog. Called from: the "LLM Optimization Advice" details' toggle handler (first expand only,
	 * guarded by data-loaded), window.ssShowplanRefreshLlmAdvice() (manual re-run, ignores the guard),
	 * and showSqlServerShowplanDialog() (auto re-run when a new statement is selected while the
	 * section is already open - since <details> only fires 'toggle' on an actual open/close
	 * transition, simply resetting state wouldn't refresh an already-open section on its own).
	 */
	function _ssShowplanLoadLlmAdvice(body)
	{
		body.setAttribute('data-loaded', 'true');

		if (typeof dbxLlmAdvice === 'undefined') {
			body.innerHTML = '<em style="color:#888;">dbxLlmAdvice.js is not loaded on this page.</em>';
			return;
		}

		// Reuse the same srv/dbname/ts/sqltext context stored on the Table Information section - the
		// srv/dbname-based DDL/index/stats lookup itself now lives in dbxLlmAdvice.js (fetchDdlContextBySrv),
		// shared with the Daily Summary Report's "Get LLM Optimization Advice" link.
		var tiBody  = document.getElementById('dbx-ssp-tableinfo-body');
		var srv     = tiBody ? (tiBody.getAttribute('data-srv')     || '') : '';
		var dbname  = tiBody ? (tiBody.getAttribute('data-dbname')  || '') : '';
		var ts      = tiBody ? (tiBody.getAttribute('data-ts')      || '') : '';
		var sqlText = tiBody ? (tiBody.getAttribute('data-sqltext') || '') : '';
		var xmlText = $('#dbx-view-ssShowplan-xmlContent').text();

		if (!sqlText) {
			body.innerHTML = '<em style="color:#888;">No SQL text is available for this plan.</em>';
			return;
		}

		// open() itself renders a "Looking up table DDL/index/stats..." status while srv/dbname are set
		// and ddlContext isn't - no need to duplicate that here.
		var workloadData = tiBody ? (tiBody.getAttribute('data-workloaddata') || '') : '';
		dbxLlmAdvice.open({ sql: sqlText, plan: xmlText, dbVendor: 'Microsoft SQL Server', srv: srv, dbname: dbname, ts: ts, workloadData: workloadData, target: body });
	}

	// Manual re-run, e.g. after the user notices the advice looks stale, or just wants to try again.
	// Ignores the data-loaded guard (unlike the toggle handler) since this is an explicit user action.
	window.ssShowplanRefreshLlmAdvice = function() {
		var body = document.getElementById('dbx-ssp-llm-body');
		if (body) _ssShowplanLoadLlmAdvice(body);
	};

	/**
	 * "No exec" counterpart to _ssShowplanLoadLlmAdvice() above - same DDL-context lookup, but
	 * finishes with a POST ...&preview:true to /api/llm/optimize-sql (LlmSqlOptimizeServlet), which
	 * builds the prompt text locally and returns it without ever calling an LLM provider, so this
	 * works even when DbxCentral.llm.enabled is off. Mirrors the ASE dialog's
	 * _aseShowplanLoadLlmPreview(). See the dbxLlmAdvice.isEnabled() toggle below for which of this
	 * section vs. the real Advice section above is shown.
	 */
	function _ssShowplanLoadLlmPreview(body) {
		body.setAttribute('data-loaded', 'true');

		var tiBody  = document.getElementById('dbx-ssp-tableinfo-body');
		var srv     = tiBody ? (tiBody.getAttribute('data-srv')     || '') : '';
		var dbname  = tiBody ? (tiBody.getAttribute('data-dbname')  || '') : '';
		var ts      = tiBody ? (tiBody.getAttribute('data-ts')      || '') : '';
		var sqlText = tiBody ? (tiBody.getAttribute('data-sqltext') || '') : '';
		var xmlText = $('#dbx-view-ssShowplan-xmlContent').text();

		if (!sqlText) {
			body.innerHTML = '<em style="color:#888;">No SQL text is available for this plan.</em>';
			return;
		}

		function buildPreview(ddlContext) {
			body.innerHTML = '<span style="color:#888;font-size:0.85em;">&#9203; Building prompt…</span>';
			// Same workload profile open() would send, so the PREVIEW matches what actually gets sent.
			var wlRaw = tiBody ? (tiBody.getAttribute('data-workloaddata') || '') : '';
			var wlProfile = (wlRaw && window.dbxLlmAdvice && dbxLlmAdvice.buildWorkloadProfile)
					? dbxLlmAdvice.buildWorkloadProfile(wlRaw) : '';
			$.ajax({
				url:         '/api/llm/optimize-sql',
				method:      'POST',
				contentType: 'application/json',
				data:        JSON.stringify({ sql: sqlText, ddlContext: ddlContext || '', plan: xmlText, dbVendor: 'Microsoft SQL Server', workloadProfile: wlProfile, preview: true }),
				dataType:    'json',
				success: function(r) {
					if (r && r.promptSent) {
						var esc = function(s) { return s.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;'); };
						body.innerHTML = '<pre id="dbx-ssp-llmpreview-text" class="mb-0" style="white-space:pre-wrap;max-height:400px;overflow:auto;">' + esc(r.promptSent) + '</pre>';
					} else {
						body.innerHTML = '<em style="color:#888;">Could not build a prompt preview.</em>';
					}
				},
				error: function(xhr) { body.innerHTML = '<span class="text-danger">Failed to build prompt: HTTP ' + xhr.status + '</span>'; }
			});
		}

		if (!srv || !dbname) {
			buildPreview('');
			return;
		}

		body.innerHTML = '<span style="color:#888;font-size:0.85em;">&#9203; Parsing SQL…</span>';
		DbxSqlTableNames.extractTablesAsync(sqlText, function(tables) {
			if (!tables.length) { buildPreview(''); return; }
			body.innerHTML = '<span style="color:#888;font-size:0.85em;">&#9203; Looking up table DDL/index/stats…</span>';
			$.ajax({
				url:      '/api/cc/mgt/table-info',
				data:     { srv: srv, dbVendor: 'Microsoft SQL Server', format: 'text', dbname: dbname, tables: tables.join(','), ts: ts },
				dataType: 'json',
				success:  function(r) { buildPreview((r && r.text) || ''); },
				error:    function() { buildPreview(''); }
			});
		});
	}

	// Manual re-run - ignores the data-loaded guard, same rationale as ssShowplanRefreshLlmAdvice.
	window.ssShowplanRefreshLlmPreview = function() {
		var body = document.getElementById('dbx-ssp-llmpreview-body');
		if (body) _ssShowplanLoadLlmPreview(body);
	};

	window.ssShowplanCopyLlmPreview = function() {
		var el = document.getElementById('dbx-ssp-llmpreview-text');
		if (!el) { alert('Build the prompt first.'); return; }
		var ta = document.createElement('textarea'); ta.value = el.textContent; document.body.appendChild(ta); ta.select();
		try { document.execCommand('copy'); } catch (err) { alert('Unable to copy\n\n' + err); }
		document.body.removeChild(ta);
	};

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

		_ssShowplanSetPlaceholder('Loading plan...');
		$('#dbx-view-ssShowplan-analysis',   $dlg).hide();
		$('#dbx-view-ssShowplan-xmlContent', $dlg).text(formatXml(xmlText));
		$('#dbx-view-ssShowplan-sqlContent', $dlg).text(ssShowplanGetSql(sqlText || ''));
		ssShowplanSetPlanType(xmlText);
		ssShowplanGetParameters();
		_ssShowplanDisableZoom();

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
			// Execution statistics for this statement over the recording period, forwarded from the
			// Daily Summary Report (see SparklineHelper.getWorkloadHarvesterJs / ShowplanSqlServerServlet).
			// The LLM Advice section below reads it back out of here.
			tiBody.setAttribute('data-workloaddata', (meta && meta.workloadData) ? meta.workloadData : '');
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

		// Reset LLM Optimization Advice section — lazy-loaded on next expand, same as Table Information
		var llmSect = document.getElementById('dbx-ssp-sect-llm');
		var llmBody = document.getElementById('dbx-ssp-llm-body');
		if (llmSect && llmBody) {
			llmBody.setAttribute('data-loaded', 'false');
			llmBody.innerHTML = '<span style="color:#888;font-size:0.85em;">Expand to ask an LLM for optimization advice.</span>';
			llmSect.removeAttribute('open');
		}

		// Reset LLM Prompt Preview section — same as above
		var llmPreviewSect = document.getElementById('dbx-ssp-sect-llmpreview');
		var llmPreviewBody = document.getElementById('dbx-ssp-llmpreview-body');
		if (llmPreviewSect && llmPreviewBody) {
			llmPreviewBody.setAttribute('data-loaded', 'false');
			llmPreviewBody.innerHTML = '<span style="color:#888;font-size:0.85em;">Expand to build the prompt.</span>';
			llmPreviewSect.removeAttribute('open');
		}

		// Draw the plan + run analysis once the modal is fully visible.
		// If the dialog is already open (hasClass('show')), Bootstrap's modal('show')
		// is a no-op and shown.bs.modal will never fire — so draw in-place immediately.
		function _drawAndAnalyze() {
			if (typeof Prism !== 'undefined') Prism.highlightAll();
			// ssShowplanRunAnalysis() both draws the diagram (via the selected renderer) and fills in
			// the Plan Analysis section, so the two can never disagree about which plan is on screen.
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
	 * Renders a findings array ({severity, category, title, detail, nodeId, nodeName}, the shape
	 * dbxShowplanAnalyzer.js's finding() factory produces) into the same "Findings (N)" collapsible
	 * block both the SQL Server and ASE "Plan Analysis" sections use - shared so the two dialogs
	 * can't drift apart visually. Returns just that block's HTML ('' if findings is empty) - callers
	 * own the summary-count line and any dialog-specific extras (SQL Server's Runtime/Wait Stats).
	 */
	function renderFindingsListHtml(findings, isDark, jumpFn) {
		if (!findings || !findings.length) return '';

		var hdrColor = isDark ? '#999' : '#888';
		var sepColor = isDark ? '#333' : '#e0e0e0';

		var sevIcon  = { 'error': '&#10060;', 'warning': '&#9888;', 'info': 'ℹ&#65039;' };
		var sevColor = { 'error': '#b71c1c',  'warning': '#b45309',  'info': '#1565c0' };
		var sevBg    = { 'error': '#fff0f0',  'warning': '#fffbeb',  'info': '#eff6ff' };
		var sevBorder= { 'error': '#f87171',  'warning': '#fbbf24',  'info': '#93c5fd' };
		if (isDark) {
			sevBg     = { 'error': '#3b1111', 'warning': '#2e2000', 'info': '#0d1f3c' };
			sevBorder = { 'error': '#c62828', 'warning': '#b45309', 'info': '#1565c0' };
		}
		var detailColor = isDark ? '#bbb' : '#555';

		function severityRank(s) { return s === 'error' ? 2 : (s === 'warning' ? 1 : 0); }

		function renderOneFinding(f) {
			var icon   = sevIcon[f.severity]   || 'ℹ';
			var color  = sevColor[f.severity]  || '#333';
			var bg     = sevBg[f.severity]     || '#f8f8f8';
			var border = sevBorder[f.severity] || '#ccc';
			// Clickable only when the caller passes a jump function name - window.aseShowplanJumpToNode
			// for ASE, window.ssShowplanJumpToNode for SQL Server's native renderer. The vendored
			// QP.js renderer still gets none: it offers no way to find a given node's element, so SQL
			// Server findings fall back to plain, unclickable text while it is selected.
			var nodeTag = f.nodeId != null
				? (jumpFn
					? ' <span style="color:#3a7bc8;font-weight:400;font-size:0.9em;cursor:pointer;text-decoration:underline;" onclick="' + jumpFn + '(\'' + escapeHtml(String(f.nodeId)) + '\')" title="Jump to this operator in the diagram">[Node ' + escapeHtml(String(f.nodeId)) + ']</span>'
					: ' <span style="color:#888;font-weight:400;font-size:0.9em;">[Node ' + escapeHtml(String(f.nodeId)) + ']</span>')
				: '';
			var detailHtml = f.detail ? '<div style="color:' + detailColor + ';margin-top:2px;white-space:pre-wrap;">' + escapeHtml(f.detail) + '</div>' : '';
			// A suggested fix (currently only the Reformatting finding's CREATE INDEX guess, see
			// dbxShowplanAse.js's enhanceReformatFindings()) - kept out of detail's prose and shown as
			// its own monospace line so it reads as code, not sentence text.
			var ddlHtml = f.suggestedDdl ? '<pre style="margin:4px 0 0 0;padding:4px 6px;background:' + (isDark ? '#1c1c1c' : '#f4f4f4') + ';border-radius:3px;font-family:\'Courier New\',monospace;font-size:0.85em;white-space:pre-wrap;color:' + (isDark ? '#ddd' : '#333') + ';">' + escapeHtml(f.suggestedDdl) + '</pre>' : '';
			return '<div style="background:' + bg + ';border-left:4px solid ' + border + ';padding:3px 8px;border-radius:2px;">'
			     + '<span style="color:' + color + ';font-weight:700;">' + icon + ' [' + escapeHtml(f.category) + '] ' + escapeHtml(f.title) + '</span>'
			     + nodeTag + detailHtml + ddlHtml + '</div>';
		}

		// Grouped by category, each its own collapsed-by-default <details> - a plan with a few hundred
		// findings (a real report: one repeated Cardinality/Scan pattern hit on every row of a wide
		// join) was an unreadable, un-scannable wall of boxes as one flat list. Sorted by count
		// descending, so whichever category is producing the most noise sits at the top - exactly the
		// thing worth collapsing first.
		var byCategory = {}, categoryOrder = [];
		findings.forEach(function (f) {
			var cat = f.category || 'Other';
			if (!byCategory[cat]) { byCategory[cat] = []; categoryOrder.push(cat); }
			byCategory[cat].push(f);
		});
		categoryOrder.sort(function (a, b) { return byCategory[b].length - byCategory[a].length; });

		var fSumStyle = 'cursor:pointer;font-size:0.75em;font-weight:700;color:' + hdrColor + ';text-transform:uppercase;letter-spacing:0.05em;user-select:none;padding:1px 0;';
		var fCountLabel = ' <span style="font-weight:400;font-size:0.95em;color:' + (isDark ? '#aaa' : '#666') + ';text-transform:none;letter-spacing:normal;">'
		                + '(' + findings.length + ')</span>';
		var fHtml = '<details open style="margin-bottom:4px;">'
		          + '<summary style="' + fSumStyle + '">Findings' + fCountLabel + '</summary>'
		          + '<div style="padding:3px 0 2px 10px;border-left:2px solid ' + sepColor + ';margin-top:3px;">'
		          + '<div style="display:flex;flex-direction:column;gap:6px;font-size:0.85em;line-height:1.4;">';

		categoryOrder.forEach(function (cat) {
			var list = byCategory[cat];
			var worst = 'info';
			list.forEach(function (f) { if (severityRank(f.severity) > severityRank(worst)) worst = f.severity; });
			var nErr  = list.filter(function (f) { return f.severity === 'error';   }).length;
			var nWarn = list.filter(function (f) { return f.severity === 'warning'; }).length;
			var nInfo = list.length - nErr - nWarn;
			var breakdown = [];
			if (nErr)  breakdown.push(nErr  + ' error'   + (nErr  !== 1 ? 's' : ''));
			if (nWarn) breakdown.push(nWarn + ' warning' + (nWarn !== 1 ? 's' : ''));
			if (nInfo) breakdown.push(nInfo + ' info');
			fHtml += '<details style="margin:0;">'
			       + '<summary style="cursor:pointer;font-size:0.95em;font-weight:600;color:' + sevColor[worst] + ';user-select:none;">'
			       + sevIcon[worst] + ' ' + escapeHtml(cat)
			       + ' <span style="font-weight:400;color:' + hdrColor + ';">(' + list.length
			       + (breakdown.length ? ' &mdash; ' + breakdown.join(', ') : '') + ')</span>'
			       + '</summary>'
			       + '<div style="display:flex;flex-direction:column;gap:3px;margin-top:4px;padding-left:8px;">';
			list.forEach(function (f) { fHtml += renderOneFinding(f); });
			fHtml += '</div></details>';
		});

		fHtml += '</div></div></details>';
		return fHtml;
	}

	/**
	 * Renders the ASE dialog's "Plan Analysis" section from the findings array
	 * AseShowplan.render() computes (dbxShowplanAse.js's collectRawTextFindings()/
	 * collectTreeFindings()/the per-operator "Large table/index" checks) - passed as
	 * opts.onFindingsChanged so it's called once synchronously right after the plan renders, and
	 * again whenever async work (the table-info lookup, the Reformatting index suggestion) adds to
	 * or updates the list. No Runtime/Wait Stats block - ASE has no equivalent data source for this
	 * yet, unlike ssShowplanRunAnalysis()'s SQL Server XML-sourced one.
	 */
	function renderAseAnalysisSection(findings) {
		var details = document.getElementById('dbx-asp-sect-analysis');
		var summary = document.getElementById('dbx-view-aseShowplan-analysis-summary');
		var bodyEl  = document.getElementById('dbx-view-aseShowplan-analysis-body');
		if (!details || !summary || !bodyEl) return;

		findings = findings || [];
		var isDark = window._colorSchema === 'dark';
		var linkEl = document.getElementById('dbx-view-aseShowplan-analysis-link');

		if (findings.length === 0) {
			summary.innerHTML = '&#10003; <b>Plan Analysis</b> <span style="font-weight:normal;color:#555;">&mdash; no issues detected</span>';
			bodyEl.innerHTML = '';
			details.style.display = '';
			details.open = false;
			if (linkEl) linkEl.style.display = 'none';
			return;
		}

		var nErr  = findings.filter(function(f){return f.severity==='error';}).length;
		var nWarn = findings.filter(function(f){return f.severity==='warning';}).length;
		var parts = [];
		if (nErr)  parts.push('<span style="color:#b71c1c;">' + nErr  + ' error'   + (nErr  > 1 ? 's' : '') + '</span>');
		if (nWarn) parts.push('<span style="color:#b45309;">' + nWarn + ' warning' + (nWarn > 1 ? 's' : '') + '</span>');
		var infoRest = findings.length - nErr - nWarn;
		if (infoRest > 0) parts.push(infoRest + ' info');
		summary.innerHTML = '&#9888; <b>Plan Analysis &mdash; ' + findings.length
		                   + ' finding' + (findings.length !== 1 ? 's' : '') + '</b>'
		                   + (parts.length ? ' (' + parts.join(', ') + ')' : '');

		bodyEl.innerHTML = renderFindingsListHtml(findings, isDark, 'aseShowplanJumpToNode');
		details.style.display = '';
		details.open = true;

		// Toolbar hint above the diagram, only shown when there's actually something to read below -
		// findings can scroll off well below the fold on a large diagram, easy to miss without a
		// pointer at the top.
		if (linkEl) {
			linkEl.style.display = '';
			linkEl.style.color = nErr ? '#b71c1c' : (nWarn ? '#b45309' : '#1565c0');
			linkEl.innerHTML = '&#9888; ' + findings.length + ' finding' + (findings.length !== 1 ? 's' : '') + ' &mdash; see Plan Analysis &#8595;';
		}
	}

	/** Opens (if collapsed) and scrolls to the Plan Analysis section - see the toolbar hint above. */
	window.aseShowplanJumpToAnalysis = function() {
		var details = document.getElementById('dbx-asp-sect-analysis');
		if (!details) return;
		details.open = true;
		details.scrollIntoView({ behavior: 'smooth', block: 'start' });
	};

	/**
	 * Opens (if collapsed) the Graphical Plan section, scrolls the operator with this VA into view,
	 * and briefly flashes its box (.ase-plan-flash, dbxShowplanAse.js) - wired up as the click handler
	 * for a Plan Analysis finding's "[Node N]" tag (see renderFindingsListHtml()'s jumpFn param).
	 * Boxes are tagged with data-va by dbxShowplanAse.js's renderNode().
	 */
	window.aseShowplanJumpToNode = function(va) {
		var graphSect = document.getElementById('dbx-asp-sect-graph');
		if (graphSect) graphSect.open = true;
		var box = document.querySelector('#dbx-view-aseShowplan-graphContent .ase-plan-box[data-va="' + va + '"]');
		if (!box) return;
		box.scrollIntoView({ behavior: 'smooth', block: 'center', inline: 'center' });
		// Restart the CSS animation if the same node is clicked again before the previous flash ended -
		// removing the class, forcing a reflow, then re-adding it is the standard way to do that.
		box.classList.remove('ase-plan-flash');
		void box.offsetWidth;
		box.classList.add('ase-plan-flash');
		// 'animationend' only fires once, after the CSS animation's last iteration (5 pulses - see
		// .ase-plan-flash in dbxShowplanAse.js) - removing the class here rather than via a fixed
		// setTimeout keeps the two in sync automatically if the pulse count/duration ever changes.
		box.addEventListener('animationend', function onEnd() {
			box.classList.remove('ase-plan-flash');
		}, { once: true });
	};

	/**
	 * Run DbxShowplanAnalyzer on the given XML and render findings into
	 * #dbx-view-ssShowplan-analysis.  Also renders Runtime Stats and Wait Stats.
	 */
	// ── SQL Server graphical-plan renderer state ────────────────────────────────────
	//
	// Two renderers draw into #dbx-view-ssShowplan-content: 'dbx' (dbxShowplanSqlServer.js, the
	// native one - node jumping, orientation, DDL Storage per operator, Properties pane) and 'qp'
	// (the vendored html-query-plan/QP.js). 'dbx' is the default; 'qp' is kept as the fallback and
	// as the reference to compare against until it can be retired.
	var _ssShowplanRenderer   = 'dbx';
	var _ssShowplanHorizontal = true;   // left-to-right, matching the ASE dialog's default
	var _ssShowplanShowProps  = true;
	var _ssShowplanTableSizeWarnMb = 100;
	var _ssShowplanLastXmlText = '';
	var _ssShowplanSplitterInited = false;
	// Set by ssShowplanTryQpOnce() (the "Try Switch to: html-query-plan" button offered when a plan
	// has no <QueryPlan> element for the native renderer to draw) - true for exactly one dialog
	// viewing, never written to localStorage. The permanent shown.bs.modal handler below clears it
	// and restores the real persisted renderer before the next open, so this choice is never
	// remembered "next time we enter the showplan dialog".
	var _ssShowplanRendererIsTemp = false;
	try {
		var _ssR = localStorage.getItem('dbxtune_ssShowplan_renderer');
		if (_ssR === 'dbx' || _ssR === 'qp') _ssShowplanRenderer = _ssR;
		var _ssH = localStorage.getItem('dbxtune_ssShowplan_horizontal');
		if (_ssH !== null) _ssShowplanHorizontal = _ssH === 'true';
		var _ssP = localStorage.getItem('dbxtune_ssShowplan_showProps');
		if (_ssP !== null) _ssShowplanShowProps = _ssP === 'true';
		var _ssW = parseFloat(localStorage.getItem('dbxtune_ssShowplan_tableSizeWarnMb'));
		if (!isNaN(_ssW) && _ssW >= 0) _ssShowplanTableSizeWarnMb = _ssW;
	} catch (ex) {}

	function _ssLsSet(key, val) { try { localStorage.setItem(key, val); } catch (ex) {} }

	/** Keeps the toolbar labels/visibility in step with the current renderer + toggles. */
	function _ssShowplanSyncToolbar() {
		var isDbx = _ssShowplanRenderer === 'dbx';
		// Buttons are labelled with the ACTION they perform, not the current state - same convention
		// as the ASE dialog's orientation button.
		var btn = document.getElementById('dbx-view-ssShowplan-rendererBtn');
		if (btn) btn.innerHTML = isDbx ? '&#8646; Use html-query-plan' : '&#8646; Use DbxTune Plan';
		var ori = document.getElementById('dbx-view-ssShowplan-orientationBtn');
		if (ori) {
			ori.style.display = isDbx ? '' : 'none';
			ori.innerHTML = _ssShowplanHorizontal ? '&#8646; Top-to-Bottom' : '&#8646; Left-to-Right';
		}
		var pb = document.getElementById('dbx-view-ssShowplan-propsBtn');
		if (pb) {
			pb.style.display = isDbx ? '' : 'none';
			pb.innerHTML = _ssShowplanShowProps ? '&#128203; Hide Properties' : '&#128203; Show Properties';
		}
		// "Big table >" only applies to the native renderer AND only when there is a server context to
		// look sizes up against - the same srv/dbname the Table Information section needs.
		var tiBody  = document.getElementById('dbx-ssp-tableinfo-body');
		var hasDdl  = !!(tiBody && tiBody.getAttribute('data-srv') && tiBody.getAttribute('data-dbname'));
		var warnWrap = document.getElementById('dbx-view-ssShowplan-warnMbWrap');
		if (warnWrap) warnWrap.style.display = (isDbx && hasDdl) ? '' : 'none';
		var warnInput = document.getElementById('dbx-view-ssShowplan-warnMb');
		if (warnInput && warnInput.value === '') warnInput.value = _ssShowplanTableSizeWarnMb;
		// The findings button is about the plan itself, so it stays useful under either renderer.
		var fb = document.getElementById('dbx-view-ssShowplan-findingsBtn');
		if (fb && !fb.textContent) fb.style.display = 'none';
		// The Properties pane and its splitter only mean anything for the native renderer - QP.js has
		// no per-node selection to drive them.
		var showPane = isDbx && _ssShowplanShowProps;
		var pane  = document.getElementById('dbx-view-ssShowplan-propsPane');
		var split = document.getElementById('dbx-view-ssShowplan-propsSplit');
		if (pane)  pane.style.display  = showPane ? '' : 'none';
		if (split) split.style.display = showPane ? '' : 'none';
	}

	/**
	 * The single place a SQL Server plan gets drawn. Replaces the three separate QP.showPlan() call
	 * sites that existed before, so the renderer choice is honoured no matter which entry point
	 * (modal data-toggle, programmatic showSqlServerShowplanDialog, or a toolbar redraw) opened it.
	 *
	 * onFindings, when given, receives the native renderer's tree-derived findings so the caller can
	 * merge them into the Plan Analysis section alongside dbxShowplanAnalyzer.js's XML-derived ones.
	 */
	/**
	 * The element both renderers draw into and that Panzoom transforms - deliberately INSIDE the
	 * #dbx-view-ssShowplan-content scroll viewport rather than being it. Created on demand so a
	 * dialog rendered before this existed still works.
	 */
	function _ssShowplanZoomTarget() {
		var outer = document.getElementById('dbx-view-ssShowplan-content');
		if (!outer) return null;
		var inner = document.getElementById('dbx-view-ssShowplan-zoomTarget');
		if (!inner) {
			inner = document.createElement('div');
			inner.id = 'dbx-view-ssShowplan-zoomTarget';
			// max-content so the box hugs the diagram instead of being clamped to the viewport width,
			// which is what lets the viewport scroll it. No transform-origin override - _panzoomZoomToFit
			// solves against Panzoom's default 50%/50% origin.
			inner.style.width = 'max-content';
			inner.style.minWidth = '100%';
			outer.appendChild(inner);
		}
		return inner;
	}

	/**
	 * Placeholder text while a plan loads. Must go into the zoom target, not the outer viewport:
	 * writing text into the viewport replaces its children, which would destroy the zoom target and
	 * then leave the placeholder stranded above the diagram once a fresh one was created.
	 */
	function _ssShowplanSetPlaceholder(text) {
		var el = _ssShowplanZoomTarget();
		if (el) $(el).text(text);
	}

	function _ssShowplanDrawPlan(xmlText, onFindings) {
		_ssShowplanLastXmlText = xmlText || '';
		var el = _ssShowplanZoomTarget();
		if (!el) return;
		_ssShowplanSyncToolbar();
		_ssShowplanFitPropsPane();

		// srv/dbname are stashed on the table-info body by showSqlServerShowplanDialog()/the modal
		// show handler - the same context store the Table Information and LLM sections read.
		var tiBody   = document.getElementById('dbx-ssp-tableinfo-body');
		var tiSrv    = tiBody ? (tiBody.getAttribute('data-srv')    || '') : '';
		var tiDbname = tiBody ? (tiBody.getAttribute('data-dbname') || '') : '';

		// Parsed unconditionally, BEFORE the renderer dispatch below - Plan Analysis findings are a
		// property of the plan XML, not of which of the two diagram libraries is currently drawing
		// it, so they must not disappear just because html-query-plan is selected. They used to:
		// the tree-derived findings (cardinality mismatches, hot Key/RID Lookups, eager index
		// spools, large scans once DDL Storage resolves) were only ever computed as a side effect of
		// SqlServerShowplan.render() actually drawing boxes, and render() was never even called in
		// QP mode - reported directly as the same plan showing "58 findings" under html-query-plan
		// and "80" under the native renderer, purely from toggling which one draws the diagram.
		var parsed = (typeof window.SqlServerShowplan !== 'undefined') ? window.SqlServerShowplan.parseXml(xmlText) : null;

		if (_ssShowplanRenderer === 'qp') {
			// render() below is what normally computes findings (it calls collectTreeFindings() and
			// loadTableInfoAsync() itself) - since QP.showPlan() never calls render(), collectFindings()
			// stands in for it here so this branch is not silently missing the findings render() would
			// otherwise have produced. In native ('dbx') mode below, render() is called instead and
			// must NOT also go through collectFindings() first - that would fire the DDL Storage
			// lookup twice (a real duplicate network request, not just redundant computation).
			if (parsed) {
				window.SqlServerShowplan.collectFindings(parsed,
					{ srv: tiSrv, dbname: tiDbname, tableSizeWarnMb: _ssShowplanTableSizeWarnMb, onFindingsChanged: onFindings });
			}
			try { QP.showPlan(el, xmlText); } catch (ex) {
				el.innerHTML = '<span class="text-danger">html-query-plan could not render this plan: '
					+ $('<div></div>').text(ex && ex.message ? ex.message : ex).html() + '</span>';
			}
			return;
		}

		if (typeof window.SqlServerShowplan === 'undefined') {
			el.innerHTML = '<span class="text-danger">dbxShowplanSqlServer.js is not loaded on this page - '
				+ 'switch to html-query-plan with the toolbar button.</span>';
			return;
		}

		if (!parsed) {
			// Falls back with an explanation rather than an empty diagram - a plan carrying statements
			// but no <QueryPlan> (a USE/DECLARE/control-flow-only batch) is a legitimate input, not a bug.
			var why = window.SqlServerShowplan.getLastParseError() || 'unrecognized plan format';
			el.innerHTML = '<div style="color:#888;font-style:italic;padding:8px 0;">'
				+ 'No graphical plan to show &mdash; ' + $('<div></div>').text(why).html()
				+ '<br>The full plan XML is available in the &quot;XML Plan&quot; section below.'
				// html-query-plan's own XSLT sometimes draws SOMETHING for a shape the native parser
				// rejects outright (it does not require a <QueryPlan> element the same way) - worth a
				// one-click look, but not worth remembering: ssShowplanTryQpOnce() switches for this
				// open only, unlike the toolbar toggle, which persists.
				+ '<br><button type="button" class="btn btn-outline-secondary btn-sm" '
				+ 'style="margin-top:6px;font-style:normal;" onclick="ssShowplanTryQpOnce();">'
				+ '&#8646; Try Switch to: html-query-plan</button></div>';
			var emptyPane = document.getElementById('dbx-view-ssShowplan-propsPane');
			if (emptyPane) emptyPane.innerHTML = '';
			return;
		}

		window.SqlServerShowplan.render(el, parsed, {
			horizontal:      _ssShowplanHorizontal,
			srv:             tiSrv,
			dbname:          tiDbname,
			tableSizeWarnMb: _ssShowplanTableSizeWarnMb,
			propsTarget:     (_ssShowplanShowProps
			                  ? (document.getElementById('dbx-view-ssShowplan-propsBody')
			                     || document.getElementById('dbx-view-ssShowplan-propsPane')) : null),
			onFindingsChanged: onFindings || function () {}
		});
	}

	/** Re-draws the last plan with the current settings, and re-runs the analysis so its
	 *  "[Node N]" jump links stay in step with whichever renderer is now drawing. */
	function _ssShowplanRedrawLast() {
		if (!_ssShowplanLastXmlText) { _ssShowplanSyncToolbar(); return; }
		window.ssShowplanRunAnalysis(_ssShowplanLastXmlText);
	}

	window.ssShowplanToggleRenderer = function () {
		_ssShowplanRenderer = (_ssShowplanRenderer === 'dbx') ? 'qp' : 'dbx';
		_ssLsSet('dbxtune_ssShowplan_renderer', _ssShowplanRenderer);
		// Zoom is a Panzoom instance bound to the container - tear it down before swapping renderers
		// so it never keeps transforming content that no longer exists.
		_ssShowplanDisableZoom();
		_ssShowplanRedrawLast();
	};

	/**
	 * The "Try Switch to: html-query-plan" button offered when the native renderer has nothing to
	 * draw (no <QueryPlan> element - a USE/DECLARE/control-flow-only batch). Unlike
	 * ssShowplanToggleRenderer(), this does NOT persist to localStorage - it is a one-off look at the
	 * plan through the other renderer, not a change of preference. _ssShowplanRendererIsTemp flags it
	 * so the shown.bs.modal handler puts the real persisted renderer back the next time this dialog
	 * opens, rather than remembering the switch.
	 */
	window.ssShowplanTryQpOnce = function () {
		_ssShowplanRenderer = 'qp';
		_ssShowplanRendererIsTemp = true;
		_ssShowplanDisableZoom();
		_ssShowplanRedrawLast();
	};

	window.ssShowplanToggleOrientation = function () {
		_ssShowplanHorizontal = !_ssShowplanHorizontal;
		_ssLsSet('dbxtune_ssShowplan_horizontal', _ssShowplanHorizontal ? 'true' : 'false');
		_ssShowplanDisableZoom();
		_ssShowplanRedrawLast();
	};

	window.ssShowplanToggleProps = function () {
		_ssShowplanShowProps = !_ssShowplanShowProps;
		_ssLsSet('dbxtune_ssShowplan_showProps', _ssShowplanShowProps ? 'true' : 'false');
		_ssShowplanRedrawLast();
	};

	/**
	 * "Big table >" threshold, in MB. Re-renders so the marking and the matching Plan Analysis
	 * findings update immediately rather than only on the next plan.
	 */
	window.ssShowplanSetTableSizeWarnMb = function (val) {
		var parsed = parseFloat(val);
		if (isNaN(parsed) || parsed < 0) parsed = 100;
		_ssShowplanTableSizeWarnMb = parsed;
		_ssLsSet('dbxtune_ssShowplan_tableSizeWarnMb', String(parsed));
		var input = document.getElementById('dbx-view-ssShowplan-warnMb');
		if (input) input.value = parsed;
		_ssShowplanRedrawLast();
	};

	/** Open the Plan Analysis section and scroll to it - target of the findings-count button. */
	window.ssShowplanGotoAnalysis = function () {
		var details = document.getElementById('dbx-view-ssShowplan-analysis');
		if (!details) return;
		details.style.display = '';
		details.open = true;
		details.scrollIntoView({ behavior: 'smooth', block: 'start' });
	};

	/** Toolbar "Redraw" - renderer-aware replacement for the old QP.drawLines() inline onclick. */
	window.ssShowplanRedraw = function () {
		if (_ssShowplanRenderer === 'qp') {
			try { QP.drawLines(document.getElementById('dbx-view-ssShowplan-content')); } catch (ex) {}
			return;
		}
		_ssShowplanRedrawLast();
	};

	/**
	 * Scrolls to and flashes an operator box, for a Plan Analysis finding's "[Node N]" tag. The
	 * SQL Server dialog could never do this before: QP.js exposes no way to find a given node's
	 * element. The native renderer stamps data-nodeid on every box, so it can.
	 */
	window.ssShowplanJumpToNode = function (nodeId) {
		var sect = document.getElementById('dbx-ssp-sect-plan');
		if (sect && !sect.open) sect.open = true;
		var box = document.querySelector(
			'#dbx-view-ssShowplan-content .ss-plan-box[data-nodeid="' + nodeId + '"]');
		if (!box) return;
		box.scrollIntoView({ behavior: 'smooth', block: 'center', inline: 'center' });
		box.classList.remove('ss-plan-flash');
		void box.offsetWidth;                       // restart the animation if it is already running
		box.classList.add('ss-plan-flash');
		box.addEventListener('animationend', function handler() {
			box.classList.remove('ss-plan-flash');
			box.removeEventListener('animationend', handler);
		});
	};

	/**
	 * Bound the Properties pane to the dialog's visible body height.
	 *
	 * It was capped with `calc(100vh - Npx)` - but the pane is sticky inside .modal-body, whose
	 * bottom edge is nowhere near the bottom of the browser viewport (the dialog is a fixed-size,
	 * resizable box with a footer of buttons under it). So a long property list ran past the visible
	 * body and was clipped mid-row underneath the footer, with no way to tell whether that was the
	 * end of the list or just where it got cut off.
	 *
	 * Measuring the real container instead means the pane always ends inside the dialog, so its own
	 * border marks the end of the content and its scrollbar honestly reflects what is left.
	 */
	function _ssShowplanFitPropsPane() {
		var pane = document.getElementById('dbx-view-ssShowplan-propsPane');
		if (!pane) return;
		var body = pane.closest('.modal-body');
		if (!body) return;
		// top:4 sticky offset at the top, and the same again at the bottom so the pane's lower border
		// is visibly clear of the body's edge rather than flush against it.
		var h = body.clientHeight - 22;
		if (h > 120) pane.style.maxHeight = Math.round(h) + 'px';
	}

	/** Drag-to-resize between the diagram and the Properties pane, persisted across opens. */
	function _ssShowplanInitPropsSplitter() {
		var split = document.getElementById('dbx-view-ssShowplan-propsSplit');
		var pane  = document.getElementById('dbx-view-ssShowplan-propsPane');
		if (!split || !pane) return;
		try {
			var w0 = parseFloat(localStorage.getItem('dbxtune_ssShowplan_propsWidth'));
			if (!isNaN(w0) && w0 >= 150) pane.style.flexBasis = w0 + 'px';
		} catch (ex) {}

		// The dialog is draggable, resizable and has an expand/restore toggle, so the body height
		// changes after the initial draw - re-fit the pane whenever it does.
		var mb = split.closest('.modal-body');
		if (mb && typeof ResizeObserver !== 'undefined') {
			new ResizeObserver(function () { _ssShowplanFitPropsPane(); }).observe(mb);
		}
		window.addEventListener('resize', _ssShowplanFitPropsPane);
		_ssShowplanFitPropsPane();

		var dragging = false, startX = 0, startW = 0;
		split.addEventListener('mousedown', function (e) {
			dragging = true; startX = e.clientX; startW = pane.getBoundingClientRect().width;
			// The diagram contains text and an SVG overlay; without this the drag turns into a text
			// selection as soon as the pointer leaves the 6px handle.
			document.body.style.userSelect = 'none';
			e.preventDefault();
		});
		document.addEventListener('mousemove', function (e) {
			if (!dragging) return;
			// Dragging the handle LEFT widens the (right-hand) pane, hence the negation.
			var w = Math.min(900, Math.max(180, startW - (e.clientX - startX)));
			pane.style.flexBasis = w + 'px';
		});
		document.addEventListener('mouseup', function () {
			if (!dragging) return;
			dragging = false;
			document.body.style.userSelect = '';
			_ssLsSet('dbxtune_ssShowplan_propsWidth', Math.round(pane.getBoundingClientRect().width));
		});
	}

	/**
	 * Click-and-drag panning for the diagram viewport when zoom is OFF - the same "grab the canvas and
	 * move it" gesture Panzoom already provides once zoom is enabled, just moving scrollLeft/scrollTop
	 * directly instead of a CSS transform, so it works against the plain overflow:auto viewport with
	 * no dependency on Panzoom being active. Skips entirely while zoom IS on: Panzoom owns dragging
	 * then, and the two would otherwise fight over the same mouse gesture on the same element.
	 */
	function _ssShowplanInitDragScroll() {
		var el = document.getElementById('dbx-view-ssShowplan-content');
		if (!el) return;
		el.style.cursor = 'grab';

		var dragging = false, moved = false, startX = 0, startY = 0, startLeft = 0, startTop = 0;
		// A plain click (mousedown+mouseup with no real movement in between) still has to reach the
		// box's own click handler for selection - only once the pointer has actually travelled a few
		// px do we treat this as a pan instead, and swallow the click that would otherwise follow it.
		var DRAG_THRESHOLD = 4;

		el.addEventListener('mousedown', function (e) {
			if (_ssShowplanZoom !== undefined) return; // Panzoom owns dragging while zoom is on
			if (e.button !== 0) return; // left button only - right/middle keep their usual behaviour
			dragging = true;
			moved = false;
			startX = e.clientX; startY = e.clientY;
			startLeft = el.scrollLeft; startTop = el.scrollTop;
			// Same reasoning as the Properties splitter above: without this, a drag that starts on top
			// of a box's own text turns into a text selection instead of a pan.
			document.body.style.userSelect = 'none';
		});

		document.addEventListener('mousemove', function (e) {
			if (!dragging) return;
			var dx = e.clientX - startX, dy = e.clientY - startY;
			if (!moved && Math.abs(dx) < DRAG_THRESHOLD && Math.abs(dy) < DRAG_THRESHOLD) return;
			if (!moved) { moved = true; el.style.cursor = 'grabbing'; }
			el.scrollLeft = startLeft - dx;
			el.scrollTop  = startTop  - dy;
			e.preventDefault();
		});

		document.addEventListener('mouseup', function () {
			if (!dragging) return;
			dragging = false;
			document.body.style.userSelect = '';
			el.style.cursor = 'grab';
			if (moved) {
				// A real pan just happened - swallow the click the browser is about to fire on
				// whatever box the pointer ended up over, so panning across a box never also
				// selects/pins it the way a genuine click would.
				var suppressNextClick = function (ce) {
					ce.stopPropagation();
					ce.preventDefault();
					document.removeEventListener('click', suppressNextClick, true);
				};
				document.addEventListener('click', suppressNextClick, true);
			}
		});
	}

	/**
	 * Toolbar findings hint, above the diagram. Same look and content shape as the ASE dialog's
	 * dbx-view-aseShowplan-analysis-link (see aseShowplanRunAnalysis's linkEl block): plain colored
	 * text, not a button, since findings can scroll off well below the fold on a large diagram and
	 * this is a pointer at the top, not another action to choose between.
	 */
	function _ssShowplanSetFindingsButton(findings) {
		var el = document.getElementById('dbx-view-ssShowplan-findingsBtn');
		if (!el) return;
		var count = (findings || []).length;
		if (!count) {
			el.innerHTML = '';
			el.style.display = 'none';
			return;
		}
		var nErr  = findings.filter(function (f) { return f.severity === 'error';   }).length;
		var nWarn = findings.filter(function (f) { return f.severity === 'warning'; }).length;
		el.style.display = '';
		el.style.color = nErr ? '#b71c1c' : (nWarn ? '#b45309' : '#1565c0');
		el.innerHTML = '&#9888; ' + count + ' finding' + (count !== 1 ? 's' : '') + ' — see Plan Analysis ↓';
	}

	window.ssShowplanRunAnalysis = function(xmlText, _treeFindingsOverride) {
		var details  = document.getElementById('dbx-view-ssShowplan-analysis');
		var summary  = document.getElementById('dbx-view-ssShowplan-analysis-summary');
		var bodyEl   = document.getElementById('dbx-view-ssShowplan-analysis-body');
		if (!details || !summary || !bodyEl) return;

		// Draw the diagram first. The native renderer derives findings from the parsed operator TREE
		// (cardinality misestimates per operator, per-operator plan warnings, hot Key/RID Lookups,
		// eager index spools, large scans once the DDL Storage lookup lands) - things
		// dbxShowplanAnalyzer.js's raw-XML pass cannot see. They are merged into the same findings
		// list below.
		//
		// The DDL Storage lookup is async and calls back a second time with extra findings. That
		// re-entry must refresh the findings list ONLY: redrawing would restart the lookup, which
		// would call back again, looping forever. _treeFindingsOverride is how that second pass says
		// "diagram is already on screen, just re-render the list".
		var _treeFindings = [];
		if (_treeFindingsOverride !== undefined) {
			_treeFindings = _treeFindingsOverride || [];
		} else {
			var _drawDone = false;
			_ssShowplanDrawPlan(xmlText, function (f) {
				if (!_drawDone) { _treeFindings = f || []; return; }   // synchronous first call
				window.ssShowplanRunAnalysis(xmlText, f || []);        // async update - findings only
			});
			_drawDone = true;
		}

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
			// Days included so a long-running query doesn't report "52h" - matches fmtDuration() in
			// dbxShowplanSqlServer.js, which formats the same kind of value on the plan boxes.
			var d = Math.floor(ms / 86400000);
			var h = Math.floor((ms % 86400000) / 3600000);
			var m = Math.floor((ms % 3600000) / 60000);
			var s = Math.floor((ms % 60000) / 1000);
			var r = ms % 1000;
			var parts = [];
			if (d > 0) parts.push(d + 'd');
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
				// dbxShowplanAnalyzer.js not on this page. It used to hide the whole section and bail;
				// that now also threw away the native renderer's own tree-derived findings, which do
				// not need the analyzer at all. Only give up when there is genuinely nothing to show.
				if (!_treeFindings.length && !statsHtml) {
					details.style.display = 'none';
					return;
				}
			} else {
				findings = window.DbxShowplanAnalyzer.analyze(xmlSrc) || [];
			}
		} catch(ex) {
			summary.innerHTML = '&#9888; <b>Plan Analysis &mdash; error</b>';
			bodyEl.innerHTML = statsHtml + '<div style="color:#b71c1c;font-size:0.85em;white-space:pre-wrap;">' + escapeHtml(String(ex)) + '</div>';
			details.style.display = '';
			details.open = true;
			return;
		}

		// Tree-derived findings from the native renderer (see _ssShowplanDrawPlan above). Appended
		// rather than merged-by-identity: the two analyses look at different things (raw XML vs the
		// parsed operator tree), so they do not produce duplicates of each other.
		//
		// This has to happen BEFORE the "no issues detected" early-return and the severity counts
		// below: doing it after meant a plan whose only problems were tree-derived reported "no
		// issues detected" and dropped them entirely, and the severity breakdown counted only the
		// analyzer's own findings while the headline count included both ("24 findings (7 errors,
		// 5 warnings, 1 info)").
		if (_treeFindings.length) findings = findings.concat(_treeFindings);

		// Summary line
		if (findings.length === 0 && !statsHtml) {
			_ssShowplanSetFindingsButton([]);
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

		_ssShowplanSetFindingsButton(findings);

		if (findings.length > 0) {
			summary.innerHTML = '&#9888; <b>Plan Analysis &mdash; ' + findings.length
			                   + ' finding' + (findings.length !== 1 ? 's' : '') + '</b>'
			                   + (parts.length ? ' (' + parts.join(', ') + ')' : '');
		} else {
			summary.innerHTML = '&#10003; <b>Plan Analysis</b> <span style="font-weight:normal;color:#555;">&mdash; no issues detected</span>';
		}

		// Findings HTML - shared with the ASE dialog's Plan Analysis section, see renderFindingsListHtml().
		// The "[Node N]" tags become clickable only under the native renderer, which stamps
		// data-nodeid on every box; QP.js offers no way to locate a node's element.
		var fHtml = renderFindingsListHtml(findings, isDark,
			_ssShowplanRenderer === 'dbx' ? 'ssShowplanJumpToNode' : undefined);

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
