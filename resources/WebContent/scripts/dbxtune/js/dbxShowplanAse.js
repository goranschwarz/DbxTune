/**
 * dbxShowplanAse.js — SAP ASE (Sybase Adaptive Server Enterprise) graphical Showplan renderer
 *
 * Parses ASE's two Showplan shapes into a shared, format-agnostic tree model and renders it
 * as a pure CSS/DOM box-and-connector diagram — no external graph/layout library.
 *
 * Entry points:
 *   var parsed = AseShowplan.parseXml(xmlText);    // CachedPlanInXml (show_cached_plan_in_xml)
 *   var parsed = AseShowplan.parseText(planText);  // ShowPlanText (classic sp_showplan)
 *   AseShowplan.render(containerEl, parsed);
 *
 * Both parsers return null when the input couldn't be recognized at all, so callers can fall
 * back to a plain-text view instead of showing a broken/empty diagram — the legacy text format
 * in particular is free-form server print output with no fixed schema across ASE versions.
 *
 * Shared node model (produced by both parsers, consumed by render()):
 *   {
 *     op:       'TableScan',   // raw operator name
 *     label:    'Table Scan',  // display label
 *     metrics:  { estRows, actRows, estLio, actLio, estPio, actPio, estRowSz },
 *     props:    { objName, corrName, scanType, indId, scanOrder, positioning, scanCoverage, ... },
 *     raw:      [ ... ],       // text-parser only: unrecognized detail lines, kept verbatim
 *     children: [ node, ... ]
 *   }
 * Top-level result:
 *   { format: 'xml'|'text', statements: [ { label, meta, steps: [ { label, root: node } ] } ] }
 */

window.AseShowplan = (function () {

	// ─────────────────────────────────────────────────────────────────────────
	// XML parser (CachedPlanInXml / show_cached_plan_in_xml)
	// ─────────────────────────────────────────────────────────────────────────

	// Leaf/property tag names that are NOT recursive child-operator nodes.
	var XML_PROPERTY_TAGS = {
		VA: true, est: true, act: true, arity: true, varNo: true, objName: true, corrName: true,
		scanType: true, indId: true, indexName: true, scanOrder: true, positioning: true,
		scanCoverage: true, dataIOSizeInKB: true, dataBufReplStrategy: true, updateMode: true,
		i2Name: true, ascDesc: true, sortType: true, groupBy: true, joinType: true, joinPred: true
	};

	function xmlText(el) {
		return el ? (el.textContent || '').trim() : undefined;
	}

	function xmlNum(el) {
		var t = xmlText(el);
		if (t === undefined || t === '') return undefined;
		var n = parseFloat(t);
		return isNaN(n) ? undefined : n;
	}

	function xmlChildElements(el) {
		var out = [];
		if (!el) return out;
		for (var i = 0; i < el.childNodes.length; i++) {
			var c = el.childNodes[i];
			if (c.nodeType === 1) out.push(c);
		}
		return out;
	}

	function xmlFirstChildByTag(el, tag) {
		var kids = xmlChildElements(el);
		for (var i = 0; i < kids.length; i++) {
			if (kids[i].tagName === tag) return kids[i];
		}
		return undefined;
	}

	function parseXmlEstAct(el, target, prefix) {
		if (!el) return;
		var rowCnt = xmlFirstChildByTag(el, 'rowCnt');
		var lio    = xmlFirstChildByTag(el, 'lio');
		var pio    = xmlFirstChildByTag(el, 'pio');
		var rowSz  = xmlFirstChildByTag(el, 'rowSz');
		if (rowCnt) target[prefix + 'Rows']  = xmlNum(rowCnt);
		if (lio)    target[prefix + 'Lio']   = xmlNum(lio);
		if (pio)    target[prefix + 'Pio']   = xmlNum(pio);
		if (rowSz)  target[prefix + 'RowSz'] = xmlNum(rowSz);
	}

	function buildXmlNode(el) {
		var node = { op: el.tagName, label: el.tagName, metrics: {}, props: {}, children: [] };

		var vaEl = xmlFirstChildByTag(el, 'VA');
		if (vaEl) node.props.va = xmlText(vaEl);

		parseXmlEstAct(xmlFirstChildByTag(el, 'est'), node.metrics, 'est');
		parseXmlEstAct(xmlFirstChildByTag(el, 'act'), node.metrics, 'act');

		var kids = xmlChildElements(el);
		for (var i = 0; i < kids.length; i++) {
			var kid = kids[i];
			if (XML_PROPERTY_TAGS.hasOwnProperty(kid.tagName)) {
				if (kid.tagName !== 'VA' && kid.tagName !== 'est' && kid.tagName !== 'act') {
					node.props[kid.tagName] = xmlText(kid);
				}
			} else {
				// Unknown tag: recurse as a child operator node. This keeps the parser
				// future-proof against ASE operators/properties not seen in today's schema.
				node.children.push(buildXmlNode(kid));
			}
		}
		return node;
	}

	function parseXml(xmlString) {
		if (!xmlString || !xmlString.trim()) return null;
		try {
			var xmlDoc = $.parseXML(xmlString);
			var $doc   = $(xmlDoc);
			var $opTree = $doc.find('opTree').first();
			if (!$opTree.length) return null;

			var rootEl = xmlFirstChildByTagIgnoreOrder($opTree[0]);
			if (!rootEl) return null;

			var root = buildXmlNode(rootEl);

			var meta = {};
			var $plan = $doc.find('plan').first();
			if ($plan.length) {
				meta.planId    = $plan.find('> planId').first().text() || undefined;
				meta.execCount = $plan.find('> execCount').first().text() || undefined;
				meta.avgTime   = $plan.find('> avgTime').first().text() || undefined;
				meta.avgExecTime = $plan.find('> avgExecTime').first().text() || undefined;
			}
			var estTotals = $opTree.children('est').first();
			var actTotals = $opTree.children('act').first();
			if (estTotals.length) {
				meta.estTotalLio = estTotals.find('> totalLio').first().text() || undefined;
				meta.estTotalPio = estTotals.find('> totalPio').first().text() || undefined;
			}
			if (actTotals.length) {
				meta.actTotalLio = actTotals.find('> totalLio').first().text() || undefined;
				meta.actTotalPio = actTotals.find('> totalPio').first().text() || undefined;
			}

			var statementId = $doc.find('statementId').first().text() || undefined;
			var label = 'Statement' + (statementId ? ' ' + statementId : '');

			return {
				format: 'xml',
				statements: [ { label: label, meta: meta, steps: [ { label: null, root: root } ] } ]
			};
		} catch (ex) {
			return null;
		}
	}

	// Returns the first *element* child of opTree that isn't itself a recognized property tag
	// (opTree's structure is: a single operator element, optionally followed by top-level est/act
	// totals — see parseXml() above).
	function xmlFirstChildByTagIgnoreOrder(opTreeEl) {
		var kids = xmlChildElements(opTreeEl);
		for (var i = 0; i < kids.length; i++) {
			if (!XML_PROPERTY_TAGS.hasOwnProperty(kids[i].tagName)) return kids[i];
		}
		return undefined;
	}

	// ─────────────────────────────────────────────────────────────────────────
	// Legacy text parser (ShowPlanText / classic sp_showplan)
	// ─────────────────────────────────────────────────────────────────────────

	// Operator names are not always plain "A-Z + spaces" - real ASE output includes forms like
	// "N-ARY NESTED LOOP JOIN Operator", so the name class must allow hyphens/digits too. The VA
	// group is optional and not always the first parenthetical (e.g. "SEQUENCER Operator (Sequential
	// Mode)(VA = 72)"), so it's searched for separately in extractExtraInfo() below rather than
	// anchored right after "Operator".
	var OPERATOR_LINE_RE = /^(?:ROOT:)?([A-Z][A-Z0-9 \-]*?)\s+Operator\b\s*(.*)$/;
	var VA_RE            = /\(VA\s*=\s*(\d+)\)/;
	var JOIN_TYPE_RE      = /Join Type:\s*([^)]+)/i;
	var MODE_RE           = /\(([A-Za-z]+ Mode)\)/;
	var STATEMENT_HDR_RE = /^QUERY PLAN FOR (STATEMENT|SUBQUERY|SQL UDF)\b.*/i;
	var STEP_HDR_RE      = /^STEP\s+(\d+)\s*$/i;

	// Known detail-line phrases -> structured props. Seeded from real captured samples
	// (RefreshProcess.java / CounterSample.java) and the ASE message dictionary
	// (AseErrorMessageDictionary.java, msg IDs 6219-6289). Anything not matched here is kept
	// verbatim in node.raw[] rather than dropped, since this text format is not a fixed schema.
	var DETAIL_PATTERNS = [
		{ re: /^FROM TABLE$/i,                                     handler: function (node) { node._expectTableName = true; } },
		{ re: /^TO TABLE$/i,                                       handler: function (node) { node._expectTableName = true; } },
		{ re: /^(Worktable#?\s*\d+)\s+created\b.*$/i,              handlerRe: function (node, m) { node.props.objName = m[1].replace(/\s+/g, ''); } },
		{ re: /^Table Scan\.?$/i,                                  set: { scanType: 'TableScan' } },
		{ re: /^(Forward|Backward) Scan\.?$/i,                     handlerRe: function (node, m) { node.props.scanOrder = m[1] + 'Scan'; } },
		{ re: /^Positioning at start of table\.?$/i,               set: { positioning: 'StartOfTable' } },
		{ re: /^Positioning at end of table\.?$/i,                 set: { positioning: 'EndOfTable' } },
		{ re: /^Positioning by key\.?$/i,                          set: { positioning: 'ByKey' } },
		{ re: /^Using Clustered Index\.?$/i,                       set: { scanType: 'ClusteredIndexScan' } },
		{ re: /^Index\s*:\s*(.+)$/i,                                handlerRe: function (node, m) { node.props.indexName = m[1].trim(); } },
		{ re: /^Using I\/O Size (\d+) Kbytes for (?:data pages|index leaf pages)\.?$/i, handlerRe: function (node, m) { node.props.dataIOSizeInKB = m[1]; } },
		{ re: /^With (.+) Buffer Replacement Strategy.*$/i,        handlerRe: function (node, m) { node.props.dataBufReplStrategy = m[1].trim(); } },
		{ re: /^External Definition:\s*(.+)$/i,                    handlerRe: function (node, m) { node.props.externalDef = m[1].trim(); } },
		{ re: /^Evaluate (Ungrouped|Grouped) (.+) AGGREGATE\.?$/i, handlerRe: function (node, m) { node.props.aggregate = m[1] + ' ' + m[2]; } },
		{ re: /^Nested iteration\.?$/i,                             set: { joinStrategy: 'NestedIteration' } },
		{ re: /^Using Worktable(\d+) for internal storage\.?$/i,   handlerRe: function (node, m) { node.props.workTable = 'Worktable' + m[1]; } },
		{ re: /^Key Count:\s*(\d+)$/i,                              handlerRe: function (node, m) { node.props.keyCount = m[1]; } },
		{ re: /^Keys are:$/i,                                       handler: function (node) { node._expectKeys = true; } },
		{ re: /^The type of query is (\w+)\.?$/i,                  skip: true } // step-level meta, handled separately
	];

	function newTextNode(name, va, extra) {
		var props = {};
		if (va) props.va = va;
		if (extra) {
			var joinTypeMatch = extra.match(JOIN_TYPE_RE);
			if (joinTypeMatch) props.joinType = joinTypeMatch[1].trim();
			var modeMatch = extra.match(MODE_RE);
			if (modeMatch) props.mode = modeMatch[1].trim();
		}
		return { op: name, label: name, metrics: {}, props: props, raw: [], children: [], extra: extra };
	}

	function applyDetailLine(node, line) {
		if (node._expectTableName) {
			node.props.objName = line.replace(/\.$/, '').trim();
			node._expectTableName = false;
			node._expectCorrName = true; // table name is often followed by "from view: X" and/or a bare alias
			return;
		}
		if (node._expectCorrName) {
			node._expectCorrName = false;
			var viewMatch = line.match(/^from view:\s*(.+)$/i);
			if (viewMatch) {
				node.props.fromView = viewMatch[1].trim();
				node._expectCorrName = true; // the alias itself usually still follows
				return;
			}
			// A bare identifier right after the table name (no spaces/punctuation, no trailing
			// period) is ASE's correlation/alias name, e.g. "Foretag" then "ec_company". Anything
			// else (an "Index :" line, "Table Scan.", etc.) is NOT an alias - fall through to the
			// normal matching below instead of swallowing it.
			if (/^[A-Za-z_][\w#]*$/.test(line)) {
				node.props.corrName = line;
				return;
			}
		}
		if (node._expectKeys) {
			if (/^[\w#]+\s+(ASC|DESC)$/i.test(line)) {
				node.props.keys = (node.props.keys ? node.props.keys + ', ' : '') + line.trim();
				return;
			}
			node._expectKeys = false; // not a key line - fall through to normal matching below
		}
		for (var i = 0; i < DETAIL_PATTERNS.length; i++) {
			var pat = DETAIL_PATTERNS[i];
			var m = line.match(pat.re);
			if (!m) continue;
			if (pat.skip) return;
			if (pat.handler) { pat.handler(node); return; }
			if (pat.handlerRe) { pat.handlerRe(node, m); return; }
			if (pat.set) { for (var k in pat.set) node.props[k] = pat.set[k]; return; }
		}
		// Unrecognized — preserve verbatim so nothing is silently lost.
		node.raw.push(line);
	}

	function leadingPipeDepth(line) {
		var m = line.match(/^([\s|]*)/);
		if (!m) return 0;
		var run = m[1];
		var depth = 0;
		for (var i = 0; i < run.length; i++) if (run[i] === '|') depth++;
		return depth;
	}

	function parseStep(lines) {
		var root = null;
		var stack = {}; // depth -> node

		for (var i = 0; i < lines.length; i++) {
			var line = lines[i];
			if (!line.trim()) continue;

			var depth = leadingPipeDepth(line);
			if (depth === 0) continue; // step meta / narrative text outside the operator tree

			var content = line.replace(/^[\s|]*/, '').trim();
			if (!content) continue; // bare "|" spacer line

			var opMatch = content.match(OPERATOR_LINE_RE);
			if (opMatch) {
				var name = opMatch[1].trim().replace(/\s+/g, ' ');
				var rest = (opMatch[2] || '').trim();
				var vaMatch = rest.match(VA_RE);
				var va   = vaMatch ? vaMatch[1] : undefined;
				var node = newTextNode(name, va, rest || undefined);

				if (depth === 1) {
					root = node;
				} else {
					var parent = stack[depth - 1];
					if (parent) parent.children.push(node);
					else if (root) root.children.push(node); // defensive: unexpected depth jump
					else continue; // no root yet and depth > 1 — malformed, skip
				}
				stack[depth] = node;
				// Drop any stale deeper entries so a later sibling at this depth doesn't
				// accidentally re-parent under a previous branch's descendant.
				for (var d in stack) { if (Number(d) > depth) delete stack[d]; }
			} else {
				var target = stack[depth];
				if (target) applyDetailLine(target, content);
				// else: detail line with no matching operator at this depth — ignore, best-effort.
			}
		}
		return root;
	}

	function parseText(planText) {
		if (!planText) return null;

		var text = planText;
		// Defensive: AseConnectionUtils.getShowplan() may wrap the captured text in
		// "<html>...<pre>...</pre></html>" (see CmActiveStatements.java addHtmlTags=true).
		text = text.replace(/^[\s\S]*?<pre>/i, '');
		text = text.replace(/<\/pre>[\s\S]*$/i, '');
		// Defensive: some manually-pasted captures (e.g. raw isql console output) prefix every
		// line with "Msg: " — strip it so the pipe-depth detection below still lines up.
		text = text.replace(/^Msg:\s?/gm, '');

		var lines = text.split(/\r?\n/);

		var statements = [];
		var curStatement = null;
		var curStepLines = null;
		var curStepLabel = null;
		var sawAnyOperator = false;

		function flushStep() {
			if (curStatement && curStepLines && curStepLines.length) {
				var root = parseStep(curStepLines);
				if (root) {
					sawAnyOperator = true;
					curStatement.steps.push({ label: curStepLabel, root: root });
				}
			}
			curStepLines = null;
		}

		for (var i = 0; i < lines.length; i++) {
			var line = lines[i];

			if (STATEMENT_HDR_RE.test(line.trim())) {
				flushStep();
				curStatement = { label: line.trim(), meta: {}, steps: [] };
				statements.push(curStatement);
				curStepLines = null;
				continue;
			}

			var stepMatch = line.trim().match(STEP_HDR_RE);
			if (stepMatch) {
				flushStep();
				if (!curStatement) {
					curStatement = { label: 'Statement 1', meta: {}, steps: [] };
					statements.push(curStatement);
				}
				curStepLabel = 'STEP ' + stepMatch[1];
				curStepLines = [];
				continue;
			}

			if (curStepLines !== null) curStepLines.push(line);
		}
		flushStep();

		if (!sawAnyOperator) return null;

		// Drop statements that ended up with no parsed steps (e.g. trailing narrative-only blocks).
		statements = statements.filter(function (s) { return s.steps.length > 0; });
		if (!statements.length) return null;

		return { format: 'text', statements: statements };
	}

	// ─────────────────────────────────────────────────────────────────────────
	// Renderer — pure CSS/DOM box-and-connector tree, no external layout library
	// ─────────────────────────────────────────────────────────────────────────

	var STYLE_INJECTED = false;
	// Connector lines use the well-known pure-CSS "org chart" <ul><li> technique: each <li> draws the
	// left/right halves of the horizontal bar linking it to its siblings (suppressed for the first/
	// last child and for an only child), and each nested <ul> draws the vertical stem dropping from
	// its parent box down to that horizontal bar. This is far more robust than trying to compute
	// per-node connector positions from a flexbox row directly.
	var CSS = ''
		+ '.ase-plan-wrap { font-family: -apple-system, Segoe UI, Roboto, sans-serif; font-size: 0.8em; padding: 8px 0; }'
		+ '.ase-plan-stmt-hdr { font-weight: 600; font-size: 0.9em; color: #444; margin: 6px 0 2px 0; }'
		+ '.ase-plan-step-hdr { font-size: 0.8em; color: #888; margin-bottom: 6px; }'
		// width:max-content keeps this box sized to its own content rather than stretching to fill
		// the dialog's ancestor .scroll-tree (fixed width:3000px, shared by every section of the ASE
		// modal, not graph-specific) - otherwise the flex-centered tree ends up positioned deep
		// inside that oversized canvas and the browser's default scroll position shows mostly blank
		// space, clipping real boxes off both edges instead of starting at the tree's own left edge.
		+ '.ase-plan-tree { width: max-content; padding: 8px 4px 16px 4px; }'
		+ '.ase-plan-tree ul, .ase-plan-tree ul ul { display: flex; justify-content: center; padding-top: 20px; position: relative; }'
		+ '.ase-plan-tree ul { list-style: none; margin: 0; padding-left: 0; }'
		+ '.ase-plan-tree li { list-style: none; position: relative; padding: 20px 8px 0 8px; display: flex; flex-direction: column; align-items: center; }'
		+ '.ase-plan-tree li::before, .ase-plan-tree li::after { content: ""; position: absolute; top: 0; right: 50%; border-top: 1px solid #b0b0b0; width: 50%; height: 20px; }'
		+ '.ase-plan-tree li::after { right: auto; left: 50%; border-left: 1px solid #b0b0b0; }'
		+ '.ase-plan-tree li:only-child { padding-top: 0; }'
		+ '.ase-plan-tree li:only-child::before, .ase-plan-tree li:only-child::after { display: none; }'
		+ '.ase-plan-tree li:first-child::before, .ase-plan-tree li:last-child::after { border: 0 none; }'
		+ '.ase-plan-tree li:last-child::before { border-right: 1px solid #b0b0b0; border-radius: 0 5px 0 0; }'
		+ '.ase-plan-tree li:first-child::after { border-radius: 5px 0 0 0; }'
		+ '.ase-plan-tree ul ul::before { content: ""; position: absolute; top: 0; left: 50%; border-left: 1px solid #b0b0b0; width: 0; height: 20px; }'
		+ '.ase-plan-tree > ul > li { padding-top: 0; }'
		+ '.ase-plan-tree > ul > li::before, .ase-plan-tree > ul > li::after { display: none; }'
		// Left-to-right variant: same technique, axes swapped (top<->left, bottom<->right,
		// width<->height, border-top<->border-left) so "down the tree" becomes "right along the
		// tree". The extra `.ase-plan-horizontal` class always wins on specificity over the plain
		// rules above, so toggling it is enough to flip orientation without touching the DOM.
		+ '.ase-plan-tree.ase-plan-horizontal ul, .ase-plan-tree.ase-plan-horizontal ul ul { flex-direction: column; justify-content: center; padding-top: 0; padding-left: 24px; }'
		+ '.ase-plan-tree.ase-plan-horizontal li { padding: 8px 0 8px 24px; flex-direction: row; align-items: center; }'
		+ '.ase-plan-tree.ase-plan-horizontal li::before, .ase-plan-tree.ase-plan-horizontal li::after { top: auto; left: 0; right: auto; bottom: 50%; border-top: 0 none; border-left: 1px solid #b0b0b0; width: 24px; height: 50%; }'
		+ '.ase-plan-tree.ase-plan-horizontal li::after { bottom: auto; top: 50%; border-top: 1px solid #b0b0b0; }'
		+ '.ase-plan-tree.ase-plan-horizontal li:only-child { padding-left: 0; }'
		// These two clear the "no sibling on this side" edge exactly like the plain-mode
		// `li:first-child::before, li:last-child::after { border: 0 none; }` rule does - but that
		// rule alone isn't enough here: the generic `li::before, li::after` rule two lines up
		// unconditionally sets border-left, and (2 classes + tag + pseudo-element) beats the plain
		// rule's (1 class + tag + pseudo-class + pseudo-element) on specificity, so without an
		// explicit horizontal-scoped override the edge children kept a stray extra border segment -
		// exactly the "hook" artifact reported against the real dialog.
		+ '.ase-plan-tree.ase-plan-horizontal li:first-child::before { border: 0 none; }'
		+ '.ase-plan-tree.ase-plan-horizontal li:last-child::after { border: 0 none; }'
		+ '.ase-plan-tree.ase-plan-horizontal li:last-child::before { border-right: 0 none; border-bottom: 1px solid #b0b0b0; border-radius: 0; }'
		+ '.ase-plan-tree.ase-plan-horizontal li:first-child::after { border-radius: 0; }'
		+ '.ase-plan-tree.ase-plan-horizontal ul ul::before { top: 50%; left: 0; border-left: 0 none; border-top: 1px solid #b0b0b0; width: 24px; height: 0; }'
		+ '.ase-plan-tree.ase-plan-horizontal > ul > li { padding-left: 0; }'
		+ '.ase-plan-box { position: relative; border: 1px solid #999; border-radius: 5px; background: #fff; padding: 5px 9px; cursor: pointer; min-width: 120px; max-width: 220px; text-align: center; box-shadow: 0 1px 2px rgba(0,0,0,0.08); }'
		+ '.ase-plan-box:hover { border-color: #4a90d9; }'
		+ '.ase-plan-box.ase-plan-warn { border-color: #d9a24a; background: #fff8ec; }'
		+ '.ase-plan-icon { width: 32px; height: 32px; margin: 0 auto; background-repeat: no-repeat; }'
		+ '.ase-plan-label { font-weight: 600; white-space: nowrap; }'
		+ '.ase-plan-subtitle { font-size: 0.85em; color: #555; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 200px; margin: 0 auto; }'
		+ '.ase-plan-metric { font-size: 0.85em; color: #666; white-space: nowrap; }'
		+ '.ase-plan-metric.ase-plan-warn-text { color: #a3690a; font-weight: 600; }'
		+ '.ase-plan-detail { position: absolute; top: 100%; left: 50%; transform: translateX(-50%); z-index: 20; background: #fffef5; border: 1px solid #c9b98a; border-radius: 4px; padding: 6px 10px; margin-top: 4px; min-width: 220px; max-width: 360px; text-align: left; box-shadow: 0 2px 8px rgba(0,0,0,0.2); font-size: 0.92em; }'
		+ '.ase-plan-detail.ase-plan-detail-above { top: auto; bottom: 100%; margin-top: 0; margin-bottom: 4px; }'
		+ '.ase-plan-detail table { border-collapse: collapse; }'
		+ '.ase-plan-detail td { padding: 1px 6px 1px 0; vertical-align: top; white-space: nowrap; }'
		+ '.ase-plan-detail td.ase-plan-detail-key { color: #777; }'
		+ '.ase-plan-detail .ase-plan-raw-line { font-family: monospace; white-space: pre-wrap; color: #555; }'
		+ '.ase-plan-detail.ase-plan-tooltip { pointer-events: none; cursor: default; }'
		+ '.ase-plan-fallback { color: #888; font-size: 0.85em; font-style: italic; padding: 6px 0; }';

	function injectStyle() {
		if (STYLE_INJECTED) return;
		var styleEl = document.createElement('style');
		styleEl.type = 'text/css';
		styleEl.appendChild(document.createTextNode(CSS));
		document.head.appendChild(styleEl);
		STYLE_INJECTED = true;
	}

	// Icons reuse the sprite sheet already vendored for the SQL Server graphical plan viewer
	// (src/com/dbxtune/sql/showplan/sqlserver/css/qp_icons.png, from html-query-plan), copied to
	// resources/WebContent/images/qp_icons.png so it's servable as a plain static asset here. Each
	// icon is a 32x32 cell; offsets below are taken straight from that project's qp.css. Reusing
	// this set (rather than drawing a new one) means an operator looks the same whether the user is
	// looking at an ASE or a SQL Server plan - e.g. both show the same "Nested Loops" glyph.
	var ICON_SPRITE_URL = '/images/qp_icons.png';
	var ICON = {
		TableScan:          [ -32,  -192],
		ClusteredIndexScan: [-224,     0],
		IndexScan:          [ -96,   -96],
		NestedLoops:        [   0,   -96],
		HashMatch:          [ -64,   -64],
		MergeJoin:          [-288,   -64],
		Sort:               [-128,  -160],
		StreamAggregate:    [-224,  -160],
		Concatenation:      [ -64,   -32],
		Filter:             [   0,   -64],
		Insert:             [   0,  -192],
		Update:             [ -96,  -192],
		Delete:             [-288,  -160],
		TableSpool:         [ -64,  -192],
		Sequence:           [ -32,  -160],
		RemoteScan:         [-160,  -128],
		ComputeScalar:      [ -32,   -32],
		Result:             [-256,  -128],
		Catchall:           [ -96,  -256]
	};
	// Ordered (most-specific-first) operator-name/scanType patterns -> icon key.
	var ICON_RULES = [
		{ test: /cluster.*index/i,        icon: 'ClusteredIndexScan' },
		{ test: /index/i,                 icon: 'IndexScan' },
		{ test: /table\s*scan|^scan$/i,   icon: 'TableScan' },
		{ test: /n-ary|nest.*loop/i,      icon: 'NestedLoops' },
		{ test: /hash/i,                  icon: 'HashMatch' },
		{ test: /merge/i,                 icon: 'MergeJoin' },
		{ test: /sort/i,                  icon: 'Sort' },
		{ test: /aggregate/i,             icon: 'StreamAggregate' },
		{ test: /union/i,                 icon: 'Concatenation' },
		{ test: /restrict|filter/i,       icon: 'Filter' },
		{ test: /insert/i,                icon: 'Insert' },
		{ test: /update/i,                icon: 'Update' },
		{ test: /delete/i,                icon: 'Delete' },
		{ test: /store|spool/i,           icon: 'TableSpool' },
		{ test: /sequenc/i,               icon: 'Sequence' },
		{ test: /remote/i,                icon: 'RemoteScan' },
		{ test: /compute/i,               icon: 'ComputeScalar' },
		{ test: /^emit$/i,                icon: 'Result' },
		{ test: /scan/i,                  icon: 'IndexScan' }
	];
	// The text parser only learns "TableScan" vs "IndexScan" vs "ClusteredIndexScan" from detail
	// lines ("Table Scan.", "Using Clustered Index."), never from the generic "SCAN Operator" name
	// itself. But a plain (non-clustered) index scan has no dedicated sentence at all - the only
	// signal ASE prints is an "Index : IndexName" line (captured as props.indexName) with no
	// accompanying "Table Scan."/"Using Clustered Index." line - so that combination is inferred as
	// IndexScan here. XML plans already carry an explicit <scanType> so no inference is needed there.
	function effectiveScanType(node) {
		var p = node.props || {};
		if (p.scanType) return p.scanType;
		if (/scan/i.test(node.op || '') && p.indexName) return 'IndexScan';
		return undefined;
	}
	function iconKeyFor(node) {
		var scanType = effectiveScanType(node);
		if (scanType) {
			for (var i = 0; i < ICON_RULES.length; i++) {
				if (ICON_RULES[i].test.test(scanType)) return ICON_RULES[i].icon;
			}
		}
		var opName = node.op || '';
		for (var j = 0; j < ICON_RULES.length; j++) {
			if (ICON_RULES[j].test.test(opName)) return ICON_RULES[j].icon;
		}
		return 'Catchall';
	}
	function iconStyleFor(node) {
		var pos = ICON[iconKeyFor(node)] || ICON.Catchall;
		return 'background-image:url(' + ICON_SPRITE_URL + ');background-position:' + pos[0] + 'px ' + pos[1] + 'px;';
	}
	// The box's own headline label - for scans this spells out the scan type (rather than leaving
	// it to the icon alone, per user feedback: "you need to show what type of scan it is"), with a
	// "(By Key)" suffix when the plan reports key-based positioning (an index seek, effectively).
	function displayLabelFor(node) {
		var scanType = effectiveScanType(node);
		if (!scanType) return node.label;
		var label;
		if (/cluster/i.test(scanType))     label = 'Clustered Index Scan';
		else if (/index/i.test(scanType))  label = 'Index Scan';
		else if (/table/i.test(scanType))  label = 'Table Scan';
		else return node.label;
		var positioning = node.props && node.props.positioning;
		if (positioning && /bykey/i.test(positioning.replace(/\s+/g, ''))) label += ' (By Key)';
		return label;
	}

	function fmtNum(n) {
		if (n === undefined || n === null || isNaN(n)) return undefined;
		if (n >= 1000) return Math.round(n).toLocaleString();
		return (Math.round(n * 100) / 100).toString();
	}

	function isEstActWarn(m) {
		if (m.estRows === undefined || m.actRows === undefined) return false;
		if (m.estRows <= 0) return m.actRows > 0;
		var ratio = m.actRows / m.estRows;
		return ratio > 10 || ratio < 0.1;
	}

	// Short one-line context shown under the operator name so a large plan is scannable without
	// having to click every box (table name for scans, join type for joins, etc).
	function subtitleFor(node) {
		var p = node.props || {};
		if (p.objName)  return p.corrName ? (p.objName + ' (' + p.corrName + ')') : p.objName;
		if (p.joinType) return p.joinType;
		if (p.mode)     return p.mode;
		if (p.aggregate) return p.aggregate;
		if (p.indexName) return 'Index: ' + p.indexName;
		return undefined;
	}

	function buildDetailPanel(node) {
		var $panel = $('<div class="ase-plan-detail"></div>');
		var $tbl = $('<table></table>');

		function row(key, val) {
			if (val === undefined || val === null || val === '') return;
			$tbl.append($('<tr></tr>')
				.append($('<td class="ase-plan-detail-key"></td>').text(key))
				.append($('<td></td>').text(val)));
		}

		row('Operator', displayLabelFor(node));
		if (node.props) {
			for (var k in node.props) {
				if (k === 'va') row('VA', node.props[k]);
				else row(k, node.props[k]);
			}
		}
		if (node.metrics) {
			row('Est Rows', fmtNum(node.metrics.estRows));
			row('Act Rows', fmtNum(node.metrics.actRows));
			row('Est LIO',  fmtNum(node.metrics.estLio));
			row('Act LIO',  fmtNum(node.metrics.actLio));
			row('Est PIO',  fmtNum(node.metrics.estPio));
			row('Act PIO',  fmtNum(node.metrics.actPio));
		}
		if (node.extra) row('Extra', node.extra);
		$panel.append($tbl);

		if (node.raw && node.raw.length) {
			var $rawWrap = $('<div style="margin-top:4px;"></div>');
			node.raw.forEach(function (l) {
				$rawWrap.append($('<div class="ase-plan-raw-line"></div>').text(l));
			});
			$panel.append($rawWrap);
		}
		return $panel;
	}

	// Panels open downward by default; for a box near the bottom of the visible viewport that
	// would push the panel off-screen (and it has no way to scroll into view since it's an
	// absolutely-positioned overlay), so flip it to open upward instead.
	function flipIfClipped($panel) {
		var rect = $panel[0].getBoundingClientRect();
		if (rect.bottom > window.innerHeight) {
			$panel.addClass('ase-plan-detail-above');
		}
	}

	function renderNode(node) {
		var $li  = $('<li></li>');
		var warn = isEstActWarn(node.metrics || {});
		var $box = $('<div class="ase-plan-box"></div>');
		if (warn) $box.addClass('ase-plan-warn');

		$box.append($('<div class="ase-plan-icon"></div>').attr('style', iconStyleFor(node)));
		$box.append($('<div class="ase-plan-label"></div>').text(displayLabelFor(node)));

		var subtitle = subtitleFor(node);
		if (subtitle) {
			$box.append($('<div class="ase-plan-subtitle"></div>').attr('title', subtitle).text(subtitle));
		}

		var est = fmtNum(node.metrics && node.metrics.estRows);
		var act = fmtNum(node.metrics && node.metrics.actRows);
		if (est !== undefined || act !== undefined) {
			var $metric = $('<div class="ase-plan-metric"></div>');
			if (warn) $metric.addClass('ase-plan-warn-text');
			$metric.text('Est ' + (est === undefined ? '?' : est) + ' / Act ' + (act === undefined ? '?' : act));
			$box.append($metric);
		}

		// Hover shows the same info as a transient tooltip; click pins it open (and click again to
		// close). The two never stack — hovering while a pinned panel is already open is a no-op,
		// and clicking always clears any transient tooltip first so they don't overlap.
		$box.on('mouseenter', function () {
			if ($box.children('.ase-plan-detail').length) return;
			var $panel = buildDetailPanel(node).addClass('ase-plan-tooltip');
			$box.append($panel);
			flipIfClipped($panel);
		});
		$box.on('mouseleave', function () {
			$box.children('.ase-plan-tooltip').remove();
		});
		$box.on('click', function (e) {
			e.stopPropagation();
			$box.children('.ase-plan-tooltip').remove();
			var existing = $box.children('.ase-plan-detail');
			if (existing.length) { existing.remove(); return; }
			var $panel = buildDetailPanel(node);
			$box.append($panel);
			flipIfClipped($panel);
		});
		$li.append($box);

		if (node.children && node.children.length) {
			var $childUl = $('<ul></ul>');
			node.children.forEach(function (child) {
				$childUl.append(renderNode(child));
			});
			$li.append($childUl);
		}

		return $li;
	}

	function render(container, parsed, opts) {
		injectStyle();
		var $container = $(container);
		$container.empty();
		if (!parsed || !parsed.statements || !parsed.statements.length) return;

		var horizontal = !!(opts && opts.horizontal);
		var $wrap = $('<div class="ase-plan-wrap"></div>');
		var multiStatement = parsed.statements.length > 1;

		parsed.statements.forEach(function (stmt) {
			if (multiStatement && stmt.label) {
				$wrap.append($('<div class="ase-plan-stmt-hdr"></div>').text(stmt.label));
			}
			var multiStep = stmt.steps.length > 1;
			stmt.steps.forEach(function (step) {
				if (multiStep && step.label) {
					$wrap.append($('<div class="ase-plan-step-hdr"></div>').text(step.label));
				}
				var $tree = $('<div class="ase-plan-tree"></div>');
				if (horizontal) $tree.addClass('ase-plan-horizontal');
				var $rootUl = $('<ul></ul>');
				$rootUl.append(renderNode(step.root));
				$tree.append($rootUl);
				$wrap.append($tree);
			});
		});

		// Close open detail panels when clicking anywhere else in the diagram.
		$wrap.on('click', function () { $wrap.find('.ase-plan-detail').remove(); });

		$container.append($wrap);
	}

	return {
		parseXml: parseXml,
		parseText: parseText,
		render: render
	};
})();
