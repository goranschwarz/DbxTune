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
		i2Name: true, ascDesc: true, sortType: true, groupBy: true, joinType: true, joinPred: true,
		indexIOSizeInKB: true, indexBufReplStrategy: true
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
			var tag = kid.tagName;
			if (tag === 'VA' || tag === 'est' || tag === 'act') {
				continue; // already handled above
			} else if (tag === 'indName' || tag === 'indexName') {
				// A real captured plan uses <indName> (confirmed against a user-supplied XML plan) -
				// mapped to the same props.indexName key the rest of the renderer already reads
				// (effectiveScanType()/subtitleFor()), same field the text parser populates from its
				// "Index : <name>" lines. 'indexName' also accepted in case some ASE version really
				// does spell it that way - costs nothing to accept both.
				node.props.indexName = xmlText(kid);
			} else if (tag === 'wtObjName') {
				// <WorkTable><wtObjName>WorkTable2</wtObjName></WorkTable> - same class of bug as
				// indName/perKey below: wtObjName is WorkTable's own object-name property, not a
				// child operator. Mapped to the same props.objName every other node already uses for
				// its table/object name, so subtitleFor() picks it up for free (e.g. "WorkTable2"
				// under the WorkTable box) instead of rendering as a separate, unlabeled child box.
				node.props.objName = xmlText(kid);
			} else if (tag === 'perKey') {
				// A composite/multi-column key scan repeats this element once per key column - each
				// one wraps its own <keyCol>/<keyOrder> pair rather than being a plain text leaf, so
				// it can't go through the flat XML_PROPERTY_TAGS passthrough below without just
				// concatenating its descendants' text with no separator. Builds the same
				// comma-joined props.keys the text parser's "Keys are:" handling already produces.
				var keyCol   = xmlText(xmlFirstChildByTag(kid, 'keyCol'));
				var keyOrder = xmlText(xmlFirstChildByTag(kid, 'keyOrder'));
				var keyStr = [keyCol, keyOrder].filter(function (s) { return !!s; }).join(' ');
				if (keyStr) node.props.keys = (node.props.keys ? node.props.keys + ', ' : '') + keyStr;
			} else if (XML_PROPERTY_TAGS.hasOwnProperty(tag)) {
				node.props[tag] = xmlText(kid);
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
		// Experimental alternate connector style modeled on html-query-plan (the SQL Server plan
		// viewer already vendored in this app, src/com/dbxtune/sql/showplan/sqlserver/dist/qp.js):
		// instead of CSS pseudo-element borders, positions are measured after layout via
		// getBoundingClientRect() and connectors are drawn as an SVG overlay (see
		// drawConnectorLines() below). Kept fully separate from the default CSS-connector mode
		// above (toggled via the .ase-plan-lines class) specifically so it's trivial to roll back -
		// this block and drawConnectorLines() are the only things to remove.
		+ '.ase-plan-tree.ase-plan-lines li::before, .ase-plan-tree.ase-plan-lines li::after, .ase-plan-tree.ase-plan-lines ul ul::before { display: none !important; }'
		+ '.ase-plan-tree.ase-plan-lines { position: relative; }'
		+ '.ase-plan-connector-svg { position: absolute; top: 0; left: 0; overflow: visible; pointer-events: none; }'
		// Compact ("join-chain aware") layout: instead of computing positions in JS, this uses
		// nested native HTML tables - the same technique html-query-plan/QP.js itself uses (see
		// .qp-tr/.qp-node-outer in src/com/dbxtune/sql/showplan/sqlserver/css/qp.css: display:table
		// / table-cell on each operator's own <li>). Letting the BROWSER's table reflow pack sibling
		// subtrees (instead of a hand-computed bounding-box estimate) fixes both problems an earlier
		// JS-geometry version had: it only special-cased "one continuing child" chains, and even
		// there it reserved a whole subtree's diagonal bounding box for unrelated siblings stacked
		// below it, leaving huge dead space. Table layout packs every branching shape - not just
		// join chains - using exactly the space each subtree actually needs, for free, correctly,
		// with no custom position math to get wrong.
		//
		// .ase-plan-box-cell is an invisible wrapper (see renderNode()) around the actual visible
		// .ase-plan-box card - display:contents makes it a no-op everywhere except compact mode,
		// where it becomes the table-cell/table-caption instead of .ase-plan-box itself. That
		// indirection matters: a table-cell's border/background stretches to fill the whole
		// (possibly much taller) row, so putting table-cell directly on the bordered box would
		// stretch its visible border down the full row instead of hugging just its own content.
		+ '.ase-plan-box-cell { display: contents; }'
		// Horizontal (left-to-right): each <li> is a 2-cell table - its own box cell, then a second
		// cell holding its <ul> of children, which stack vertically (normal block flow) inside that
		// cell.
		+ '.ase-plan-tree.ase-plan-compact-h ul { display: block; list-style: none; margin: 0; padding: 0; position: static; }'
		+ '.ase-plan-tree.ase-plan-compact-h li { display: table; list-style: none; position: static; padding: 0; margin: 0; }'
		// vertical-align: top (not middle) on the box's own cell is deliberate: "middle" would center
		// a join over the FULL height of everything stacked below it (leaf + however deep the
		// continuing chain still goes), which re-introduces growing distance-from-leaf the deeper the
		// chain gets - just a gentler version of the same "V" problem this mode exists to fix. "top"
		// anchors the join right next to whichever child is listed first (the leaf, in practice -
		// see reorderCompactByVa()), independent of how much taller the OTHER child's subtree is.
		+ '.ase-plan-tree.ase-plan-compact-h li > .ase-plan-box-cell { display: table-cell; vertical-align: top; }'
		+ '.ase-plan-tree.ase-plan-compact-h li > ul { display: table-cell; vertical-align: top; padding-left: 56px; }'
		+ '.ase-plan-tree.ase-plan-compact-h li > ul > li { display: table; margin: 9px 0; }'
		+ '.ase-plan-tree.ase-plan-compact-h li > ul > li:first-child { margin-top: 0; }'
		+ '.ase-plan-tree.ase-plan-compact-h li > ul > li:last-child { margin-bottom: 0; }'
		// Vertical (top-to-bottom): transposed - the box cell becomes a table-caption (always renders
		// above its table regardless of source order, so no DOM change needed vs. horizontal), and
		// the children <ul> becomes a table-row whose <li> children are table-cells sitting side by
		// side.
		+ '.ase-plan-tree.ase-plan-compact-v ul { display: block; list-style: none; margin: 0; padding: 0; position: static; }'
		+ '.ase-plan-tree.ase-plan-compact-v li { display: table; list-style: none; position: static; padding: 0; margin: 0; }'
		+ '.ase-plan-tree.ase-plan-compact-v li > .ase-plan-box-cell { display: table-caption; caption-side: top; text-align: center; margin-bottom: 6px; }'
		// .ase-plan-box is a block div elsewhere (fine as a flex item in the other modes) - a
		// table-caption's default block content would stretch to the caption's full width (the
		// table's width, i.e. the widest row below it) instead of shrinking to its own content, so
		// it needs inline-block here specifically for text-align:center above to actually center it.
		+ '.ase-plan-tree.ase-plan-compact-v .ase-plan-box { display: inline-block; }'
		+ '.ase-plan-tree.ase-plan-compact-v li > ul { display: table-row; }'
		+ '.ase-plan-tree.ase-plan-compact-v li > ul > li { display: table-cell; vertical-align: top; padding: 40px 6px 0 6px; }'
		+ '.ase-plan-tree.ase-plan-compact-v li > ul > li:first-child { padding-left: 0; }'
		+ '.ase-plan-tree.ase-plan-compact-v li > ul > li:last-child { padding-right: 0; }'
		+ '.ase-plan-tree.ase-plan-compact-h li:only-child, .ase-plan-tree.ase-plan-compact-v li:only-child { padding: 0; }'
		+ '.ase-plan-box { position: relative; border: 1px solid #999; border-radius: 5px; background: #fff; padding: 5px 9px; cursor: pointer; min-width: 120px; max-width: 220px; text-align: center; box-shadow: 0 1px 2px rgba(0,0,0,0.08); }'
		+ '.ase-plan-box:hover { border-color: #4a90d9; }'
		+ '.ase-plan-box.ase-plan-warn { border-color: #d9a24a; background: #fff8ec; }'
		+ '.ase-plan-va { position: absolute; top: 2px; right: 4px; font-size: 0.72em; color: #aaa; line-height: 1; }'
		+ '.ase-plan-icon { width: 32px; height: 32px; margin: 0 auto; background-repeat: no-repeat; }'
		+ '.ase-plan-icon-row { display: flex; align-items: center; justify-content: center; gap: 2px; }'
		+ '.ase-plan-icon-row .ase-plan-icon { margin: 0; }'
		+ '.ase-plan-jointype-icon { width: 32px; height: 32px; background-repeat: no-repeat; background-size: 32px 32px; flex: none; }'
		+ '.ase-plan-label { font-weight: 600; white-space: nowrap; }'
		+ '.ase-plan-subtitle { font-size: 0.85em; color: #555; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 200px; margin: 0 auto; }'
		+ '.ase-plan-metric { font-size: 0.85em; color: #666; white-space: nowrap; }'
		+ '.ase-plan-metric.ase-plan-warn-text { color: #a3690a; font-weight: 600; }'
		+ '.ase-plan-metric-pct-warn { color: #c0392b; font-weight: 700; }'
		+ '.ase-plan-detail-pct-warn { color: #c0392b; font-weight: 700; }'
		+ '.ase-plan-metric-filter { color: #2a6f97; font-size: 0.85em; white-space: nowrap; }'
		+ '.ase-plan-detail { position: absolute; top: 100%; left: 50%; transform: translateX(-50%); z-index: 20; background: #fffef5; border: 1px solid #c9b98a; border-radius: 4px; padding: 6px 10px; margin-top: 4px; min-width: 220px; max-width: 360px; text-align: left; box-shadow: 0 2px 8px rgba(0,0,0,0.2); font-size: 0.92em; }'
		+ '.ase-plan-detail.ase-plan-detail-above { top: auto; bottom: 100%; margin-top: 0; margin-bottom: 4px; }'
		+ '.ase-plan-detail table { border-collapse: collapse; }'
		+ '.ase-plan-detail-desc { white-space: normal; font-style: italic; color: #6b5f3d; margin-bottom: 6px; padding-bottom: 6px; border-bottom: 1px solid #e6dcb8; line-height: 1.35; }'
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

	// Plain-English "what does this operator actually do" text for the detail panel, sourced from
	// SAP's own showplan/query-plan-operator reference (help.sap.com, Performance and Tuning Series:
	// Query Processing and Abstract Plans - "Using showplan" chapter and its per-operator sub-pages),
	// condensed to 1-3 sentences each. Same ordered, most-specific-first regex matching approach as
	// ICON_RULES above, but kept as its own separate list rather than reused: a few operators
	// ICON_RULES deliberately groups under one icon for visual simplicity (RESTRICT and SQFILTER share
	// an icon; HASH JOIN and HASH UNION share one; etc.) are semantically quite different operators
	// and get distinct, accurate entries here instead.
	var OPERATOR_DESCRIPTIONS = [
		{ test: /clust.*index/i,        text: 'Reads rows via a clustered index, so the table’s data pages are read in index order directly - no separate row-ID lookup into the base table is needed.' },
		{ test: /index/i,               text: 'Reads rows via a non-clustered index. If the index does not carry every needed column, each matching entry requires an extra lookup into the base table by row ID.' },
		{ test: /table\s*scan|^scan$/i, text: 'Reads every row of the table in physical/allocation order - no index is used. A leaf operator: it never has children.' },
		{ test: /n-?ary.*nest.*loop|nest.*loop.*n-?ary/i,
			text: 'A NESTED LOOP JOIN variant the optimizer never picks directly - built during code generation by folding a series of left-deep NESTED LOOP JOINs (each an inner join whose right child is a scan) into one operator, avoiding the wasted I/O of repeatedly re-draining an earlier scan that a chain of separate nested loops would cause.' },
		{ test: /nest.*loop/i,
			text: 'The simplest join strategy: for every row from the outer (left) child, opens and positions the inner (right) child - often a scan - on the first matching row, returns qualifying rows, then closes and reopens the inner side for the next outer row. Effective when a useful index exists on the inner side.' },
		{ test: /hash.*union/i,
			text: 'Performs a UNION ALL across several input streams while using SAP ASE’s hashing algorithm to eliminate duplicates - unlike MERGE UNION, the inputs do not need to be pre-sorted.' },
		{ test: /hash/i,
			text: 'Builds an in-memory hash table from one input (spilling to disk in partitions if it does not fit in memory), then probes it with the other input to find matching rows by key - avoids needing either input to be sorted, at the cost of build memory.' },
		{ test: /merge.*union/i,
			text: 'Performs a UNION ALL across several already-sorted, compatible input streams and eliminates duplicates as it merges them, taking advantage of the existing sort order rather than hashing.' },
		{ test: /merge/i,
			text: 'Joins two inputs that are both already sorted on the join key, advancing whichever side currently has the smaller key value until a match is found - effective when the inputs are large but already ordered, so no extra sort or hash table is needed.' },
		{ test: /sort/i,
			text: 'Produces an output stream ordered by a given sort key from its single child, using an in-memory or worktable-backed sort when the input is not already ordered - shown as SORT Distinct, it also eliminates duplicate rows as part of the same pass.' },
		{ test: /^limit$/i,
			text: 'Caps the number of rows passed through to its parent - the operator behind a "select top N" or LIMIT/OFFSET style query.' },
		{ test: /group\s*sorted/i,
			text: 'Operates on an input already sorted by its grouping/distinct columns, comparing each row to the previous one to either drop duplicates (Distinct) or accumulate per-group aggregate values (vector aggregation) - a non-blocking operator, it returns rows as it goes rather than waiting for all input first.' },
		{ test: /scalar\s*aggregate/i,
			text: 'Keeps a running aggregate (count, sum, min, max, average, etc.) across its entire input stream and returns a single summary row once the input is exhausted - used for an unwrapped/ungrouped aggregate like a plain select count(*).' },
		{ test: /aggregate/i,
			text: 'Computes aggregate values (count, sum, min, max, average, etc.) over its input rows.' },
		{ test: /union/i,
			text: 'Merges several compatible input streams into one output stream without eliminating duplicates - every row that enters is included in the output (that is what makes it "ALL").' },
		{ test: /sqfilter|sqlfilter/i,
			text: 'Executes correlated or uncorrelated subqueries. Its leftmost child is the outer query; each of its other children is a query plan fragment for one subquery, driven by correlation values the outer side generates.' },
		{ test: /restrict/i,
			text: 'A unary operator that evaluates expressions against column values to filter rows or compute values - e.g. a WHERE/HAVING predicate that could not be pushed down into a scan, or a computed/virtual column.' },
		{ test: /rid\s*join/i,
			text: 'A binary operator that joins two streams by row ID (RID) from the same source table - typically recombining a covering index scan’s matching row IDs with a separate lookup into the base table.' },
		{ test: /remote/i,
			text: 'Sends a SQL query to a remote server for execution and processes whatever results come back; showplan prints the formatted text of the query it sends.' },
		{ test: /scroll/i,
			text: 'Implements scrollable-cursor semantics (fetching forward, backward, or to an absolute/relative position) - insensitive cursors see a snapshot taken when opened, semi-sensitive ones re-fetch rows as they are read.' },
		{ test: /exchange/i,
			text: 'Marks the boundary between a producer and a consumer in a parallel query plan - operators below it produce data, operators above it consume it - dividing the plan into fragments that run as separate parallel processes.' },
		{ test: /sequenc/i,
			text: 'Executes each of its child subplans in turn, left to right (except the rightmost, which is what the rest of the plan reads from afterward) - used for reformatting strategies and some aggregate-processing plans.' },
		{ test: /store/i,
			text: 'Creates a worktable, fills it from its child operator’s output, and - if shown as STORE INDEX - also builds an index on it, so later operators in the plan can scan or seek it efficiently.' },
		{ test: /^worktable$/i,
			text: 'A reference to a worktable (temporary spool) that the surrounding operator - e.g. HASH UNION - creates and uses internally. Not a data-flow step of its own, just the scratch storage that operator sets up alongside its real children.' },
		{ test: /insert/i,
			text: 'Builds new rows from its child operator’s output and inserts them into the target table or worktable.' },
		{ test: /update/i,
			text: 'Modifies existing rows in the target table or worktable, using values produced by its child operator.' },
		{ test: /delete/i,
			text: 'Removes rows from the target table or worktable identified by its child operator’s output.' },
		{ test: /^emit$/i,
			text: 'Sits at the root of every query plan and always has exactly one child. Routes the resulting rows to the client, or assigns them into local variables / FETCH INTO targets.' },
		{ test: /scan/i,
			text: 'Reads rows into the query plan for other operators to process. A leaf operator (never has children) - the FROM message that follows it (FROM TABLE / FROM CACHE / FROM OR / FROM LIST) says what kind of scan it actually is.' }
	];

	// A join operator's own description (below) explains the mechanics of HOW it matches rows, but not
	// what a non-Inner Join Type actually changes about the OUTPUT - e.g. "Left Outer Join" still
	// returns every outer-side row even without a match, padded with NULLs, which the base NESTED LOOP
	// JOIN/MERGE JOIN/HASH JOIN text doesn't say (only the box's own subtitle shows the join type as
	// plain text, with no explanation of what it means). Keyed off props.joinType (captured from
	// "Join Type: X" - see JOIN_TYPE_RE - by both parsers), so it applies to whichever join operator
	// actually carries it rather than being duplicated per join strategy. Inner Join is intentionally
	// left with no addendum - that is the default behavior every join description above already
	// assumes, so restating it would be redundant.
	function joinTypeNote(node) {
		var jt = node.props && node.props.joinType;
		if (!jt) return undefined;
		if (/full\s*outer/i.test(jt))
			return 'As a Full Outer Join: rows from either side with no match on the other are still returned once each, with NULLs standing in for the missing side.';
		if (/left\s*outer/i.test(jt))
			return 'As a Left Outer Join: every row from the left (outer) side is returned even when nothing on the right matches it - NULLs stand in for the right side in that case.';
		if (/right\s*outer/i.test(jt))
			return 'As a Right Outer Join: every row from the right side is returned even when nothing on the left matches it - NULLs stand in for the left side in that case.';
		if (/left\s*semi/i.test(jt))
			return 'As a (Left) Semi Join: only the left side rows are returned, at most once each, when a match exists on the right - the right side columns are not part of the output. Typically how an EXISTS/IN predicate is implemented.';
		if (/right\s*semi/i.test(jt))
			return 'As a Right Semi Join: only the right side rows are returned, at most once each, when a match exists on the left.';
		if (/anti/i.test(jt))
			return 'As an Anti-Semi Join: only rows with NO match on the other side are returned - the inverse of a semi join, typically how a NOT EXISTS/NOT IN predicate is implemented.';
		return undefined;
	}

	// A small Venn-diagram badge shown next to a join operator's own icon, for the join types that
	// actually change the shape of the output (Left/Right/Full Outer, Anti) - the same set
	// joinTypeNote() above adds an explanatory sentence for for the same reason: Inner Join is the
	// default every join operator's base description already assumes, so it gets no badge either,
	// keeping the visual cue reserved for the cases actually worth spotting at a glance in a large
	// plan. Semi Join deliberately gets no badge - unlike the other types, it is not a distinct Venn
	// region (it is an existence filter over the same matching rows an Inner Join already shows), so
	// none of these icons - vendored from the FatCow icon set (32x32, same size as the operator's own
	// icon) - would depict it accurately. Left/Right on the Anti case defaults to "left" (the common
	// NOT EXISTS/NOT IN direction) when the join type text does not say which side.
	function joinTypeIconFor(node) {
		var jt = node.props && node.props.joinType;
		if (!jt) return undefined;
		if (/full\s*outer/i.test(jt)) return 'sql_join_outer.png';
		if (/left\s*outer/i.test(jt)) return 'sql_join_left.png';
		if (/right\s*outer/i.test(jt)) return 'sql_join_right.png';
		if (/anti/i.test(jt)) return /right/i.test(jt) ? 'sql_join_right_exclude.png' : 'sql_join_left_exclude.png';
		return undefined;
	}

	// Mirrors iconKeyFor()'s own lookup order exactly: prefer the (effective) scan type when there is
	// one, since the raw operator name for every scan is just "SCAN" - only scanType actually says
	// which kind. Falls through to matching the operator name itself otherwise.
	function operatorDescriptionFor(node) {
		var base;
		var scanType = effectiveScanType(node);
		if (scanType) {
			for (var i = 0; i < OPERATOR_DESCRIPTIONS.length; i++) {
				if (OPERATOR_DESCRIPTIONS[i].test.test(scanType)) { base = OPERATOR_DESCRIPTIONS[i].text; break; }
			}
		}
		if (base === undefined) {
			var opName = node.op || '';
			for (var j = 0; j < OPERATOR_DESCRIPTIONS.length; j++) {
				if (OPERATOR_DESCRIPTIONS[j].test.test(opName)) { base = OPERATOR_DESCRIPTIONS[j].text; break; }
			}
		}
		var note = joinTypeNote(node);
		if (base === undefined) return note;
		return note ? (base + ' ' + note) : base;
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

	// Act as a percentage of Est - undefined when either side is missing (nothing to compare),
	// Infinity when Est is 0 but Act isn't (no meaningful ratio, but still clearly "way off").
	function estActPercent(m) {
		if (m.estRows === undefined || m.actRows === undefined) return undefined;
		if (m.estRows <= 0) return m.actRows > 0 ? Infinity : undefined;
		return (m.actRows / m.estRows) * 100;
	}

	function fmtPercent(p) {
		if (p === undefined) return undefined;
		if (p === Infinity) return '∞%'; // est=0, act>0 - no finite ratio to show
		if (p >= 1000) return Math.round(p).toLocaleString() + '%';
		return (Math.round(p * 10) / 10) + '%';
	}

	// How much a single-input operator (Restrict is the classic case, but this applies to any node
	// with exactly one child) discarded between its child's output and its own. A node's own Est/Act
	// is its OUTPUT row count, not how much of its INPUT it filtered away - "Restrict shows 12,450
	// rows" alone doesn't say whether 12,450 came in and all survived, or 10 million came in and
	// almost all were thrown away. Comparing against the (single) child's actual row count is what
	// actually answers that. undefined when there isn't exactly one child, or either side's Act Rows
	// is missing/zero - multi-child nodes (joins etc.) don't have one well-defined "input" to compare
	// against, so are deliberately left alone here.
	function inputRowReductionPercent(node) {
		if (!node.children || node.children.length !== 1) return undefined;
		var childAct = node.children[0].metrics && node.children[0].metrics.actRows;
		var ownAct   = node.metrics && node.metrics.actRows;
		if (childAct === undefined || ownAct === undefined || childAct <= 0) return undefined;
		return (1 - (ownAct / childAct)) * 100;
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

		var description = operatorDescriptionFor(node);
		if (description) {
			$panel.append($('<div class="ase-plan-detail-desc"></div>').text(description));
		}

		var $tbl = $('<table></table>');

		function row(key, val, valClass) {
			if (val === undefined || val === null || val === '') return;
			var $val = $('<td></td>').text(val);
			if (valClass) $val.addClass(valClass);
			$tbl.append($('<tr></tr>')
				.append($('<td class="ase-plan-detail-key"></td>').text(key))
				.append($val));
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
			row('Act % of Est', fmtPercent(estActPercent(node.metrics)), isEstActWarn(node.metrics) ? 'ase-plan-detail-pct-warn' : undefined);
			if (node.children && node.children.length === 1) {
				row('Input Rows (Act)', fmtNum(node.children[0].metrics && node.children[0].metrics.actRows));
				row('Input Rows Filtered', fmtPercent(inputRowReductionPercent(node)));
			}
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

		var va = node.props && node.props.va;
		if (va !== undefined && va !== null && va !== '') {
			$box.append($('<div class="ase-plan-va" title="VA (vertex number)"></div>').text(va));
		}

		var $iconRow = $('<div class="ase-plan-icon-row"></div>');
		$iconRow.append($('<div class="ase-plan-icon"></div>').attr('style', iconStyleFor(node)));
		var joinIcon = joinTypeIconFor(node);
		if (joinIcon) {
			$iconRow.append($('<div class="ase-plan-jointype-icon"></div>')
				.attr('style', 'background-image:url(/images/' + joinIcon + ');')
				.attr('title', node.props.joinType));
		}
		$box.append($iconRow);
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
			// Act as a % of Est, e.g. "(1,533%)" - reuses the same >10x/<0.1x threshold as the box's
			// own orange border/text warn (isEstActWarn above, already computed into `warn`) to also
			// color just the percentage red when it's way off, on top of (not instead of) that
			// existing milder highlight.
			var pct = fmtPercent(estActPercent(node.metrics || {}));
			if (pct !== undefined) {
				var $pct = $('<span class="ase-plan-metric-pct"></span>').text(' (' + pct + ')');
				if (warn) $pct.addClass('ase-plan-metric-pct-warn');
				$metric.append($pct);
			}
			$box.append($metric);
		}

		// Separate from the Est/Act line above: how much of what flowed INTO this node it discarded,
		// not what it itself output - see the comment on inputRowReductionPercent() for why the two
		// aren't the same thing. Only shown once it's discarding a clear majority of its input (>=50%
		// - "most of the records", the threshold this was actually asked about) so it doesn't clutter
		// every ordinary pass-through node; a calm blue rather than the Est/Act line's red/orange,
		// since heavy filtering is normal/expected behavior for a Restrict, not a warning sign.
		var reduction = inputRowReductionPercent(node);
		if (reduction !== undefined && reduction >= 50) {
			var $filter = $('<div class="ase-plan-metric ase-plan-metric-filter"></div>');
			$filter.text('↓ ' + Math.round(reduction) + '% of input rows filtered');
			$box.append($filter);
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
			// A click-drag to select text inside the pinned panel still fires a 'click' on mouseup
			// (the panel is a descendant of $box, so it bubbles here) - without this check that click
			// closed/removed the very panel the user was trying to select text from, out from under
			// them, making the panel's contents effectively impossible to select/copy. Only clicking
			// the box itself (outside the panel) should toggle it.
			if ($(e.target).closest('.ase-plan-detail').length) return;
			$box.children('.ase-plan-tooltip').remove();
			var existing = $box.children('.ase-plan-detail');
			if (existing.length) { existing.remove(); return; }
			var $panel = buildDetailPanel(node);
			$box.append($panel);
			flipIfClipped($panel);
		});
		// Wrapped in a plain, invisible "cell" div rather than putting table-cell display directly
		// on .ase-plan-box: in compact mode the box's own border/background would otherwise stretch
		// to fill the whole (possibly much taller) table row/cell instead of hugging its own content
		// - the wrapper absorbs that stretch while .ase-plan-box keeps its natural size inside it.
		// display:contents in every other mode (see CSS) makes this wrapper invisible to layout, so
		// it's a no-op there.
		$li.append($('<div class="ase-plan-box-cell"></div>').append($box));

		if (node.children && node.children.length) {
			var $childUl = $('<ul></ul>');
			node.children.forEach(function (child) {
				$childUl.append(renderNode(child));
			});
			$li.append($childUl);
		}

		return $li;
	}

	var _arrowMarkerSeq = 0;

	// Experimental: draws connectors as an SVG overlay by measuring already-laid-out box positions,
	// instead of the default CSS-pseudo-element technique used by renderNode()'s <li> markup. Modeled
	// on html-query-plan's drawLines() (src/com/dbxtune/sql/showplan/sqlserver/dist/qp.js) - parent
	// and child box edges are measured via getBoundingClientRect() after the DOM is live, and an
	// elbow path with an arrowhead is drawn between them. Must run AFTER $tree is attached to the
	// document (detached elements report zero-size rects), which is why render() defers this call
	// until after $container.append($wrap) below.
	function drawConnectorLines(treeEl, horizontal) {
		var svgNS = 'http://www.w3.org/2000/svg';
		var containerRect = treeEl.getBoundingClientRect();
		var svg = document.createElementNS(svgNS, 'svg');
		svg.setAttribute('class', 'ase-plan-connector-svg');
		svg.setAttribute('width', treeEl.scrollWidth);
		svg.setAttribute('height', treeEl.scrollHeight);

		var markerId = 'ase-plan-arrow-' + (++_arrowMarkerSeq);
		var defs = document.createElementNS(svgNS, 'defs');
		var marker = document.createElementNS(svgNS, 'marker');
		marker.setAttribute('id', markerId);
		marker.setAttribute('markerWidth', '8');
		marker.setAttribute('markerHeight', '8');
		marker.setAttribute('markerUnits', 'userSpaceOnUse');
		marker.setAttribute('refX', '6');
		marker.setAttribute('refY', '3');
		marker.setAttribute('orient', 'auto');
		var arrowHead = document.createElementNS(svgNS, 'path');
		arrowHead.setAttribute('d', 'M0,0 L6,3 L0,6 Z');
		arrowHead.setAttribute('fill', '#8a8a8a');
		marker.appendChild(arrowHead);
		defs.appendChild(marker);
		svg.appendChild(defs);

		// Collected once (all boxes are already in their final position by the time this runs) so
		// each connector below can check whether its own path would cut through some OTHER box - not
		// the ones it's actually connecting. Happens rarely (a tucked sibling occasionally ends up
		// sitting in the narrow gap another connector's elbow needs to cross to reach a more distant
		// ancestor), but it does happen on real, complex plans - confirmed by measuring, not guessing.
		var allBoxRects = Array.prototype.map.call(treeEl.querySelectorAll('.ase-plan-box'), function (b) {
			var r = b.getBoundingClientRect();
			return { left: r.left - containerRect.left, right: r.right - containerRect.left,
			         top: r.top - containerRect.top, bottom: r.bottom - containerRect.top };
		});

		function segCrossesRect(x1, y1, x2, y2, rect) {
			var steps = 20;
			for (var i = 0; i <= steps; i++) {
				var t = i / steps;
				var x = x1 + (x2 - x1) * t, y = y1 + (y2 - y1) * t;
				// Shrunk by 1px so a path merely touching a box's edge (legitimately, at its own two
				// endpoint boxes) never counts as "crossing" it.
				if (x > rect.left + 1 && x < rect.right - 1 && y > rect.top + 1 && y < rect.bottom - 1) return true;
			}
			return false;
		}

		// Tests all 3 segments of a candidate elbow - (x1,y1)-mid, mid-mid (the bend), mid-(x2,y2) -
		// against every OTHER box. "Other" means not within a few px of this connector's own
		// start/end point, so a path is never rejected for legitimately touching the two boxes it
		// actually connects. `pts` is the full ordered point list, e.g. [[x1,y1],[midX,y1],[midX,y2],
		// [x2,y2]] for horizontal mode.
		function elbowCrossesOtherBox(pts) {
			var x1 = pts[0][0], y1 = pts[0][1], x2 = pts[pts.length-1][0], y2 = pts[pts.length-1][1];
			for (var i = 0; i < allBoxRects.length; i++) {
				var rect = allBoxRects[i];
				var nearOwnEnd =
					(Math.abs(rect.right - x1) < 3 || Math.abs(rect.left - x1) < 3) && y1 > rect.top - 3 && y1 < rect.bottom + 3 ||
					(Math.abs(rect.right - x2) < 3 || Math.abs(rect.left - x2) < 3) && y2 > rect.top - 3 && y2 < rect.bottom + 3;
				if (nearOwnEnd) continue;
				for (var s = 0; s < pts.length - 1; s++) {
					if (segCrossesRect(pts[s][0], pts[s][1], pts[s+1][0], pts[s+1][1], rect)) return true;
				}
			}
			return false;
		}

		treeEl.querySelectorAll('li').forEach(function (li) {
			var ul = li.parentElement;
			var parentLi = ul && ul.parentElement && ul.parentElement.tagName === 'LI' ? ul.parentElement : null;
			if (!parentLi) return; // root has no incoming connector
			var box = li.querySelector(':scope > .ase-plan-box-cell > .ase-plan-box');
			var parentBox = parentLi.querySelector(':scope > .ase-plan-box-cell > .ase-plan-box');
			if (!box || !parentBox) return;

			var r  = box.getBoundingClientRect();
			var pr = parentBox.getBoundingClientRect();

			// Every child's connector used to target the parent's exact vertical (or horizontal, in
			// vertical mode) center, regardless of which child it came from - fine when a parent only
			// has one incoming connector, but a join has two (its leaf and its continuing chain), and
			// both would converge on the identical point on the parent, overlapping right where they
			// arrive. Barely noticeable before tucking (the two children were usually far apart), but
			// tucking deliberately puts them close together, making the converged arrowheads an
			// obvious tangle. Spread each sibling's landing point evenly across the parent's edge
			// instead, based on its position among its own siblings (not on distance/order elsewhere
			// in the tree), so a single child still lands dead center - only 2+ children spread out.
			var siblingUl = li.parentElement;
			var siblings = siblingUl ? Array.prototype.slice.call(siblingUl.children) : [li];
			var siblingIndex = siblings.indexOf(li);
			var siblingCount = siblings.length;
			var spread = (siblingIndex + 1) / (siblingCount + 1); // e.g. 2 siblings -> 1/3, 2/3

			// The elbow's bend point used to sit at the exact midpoint between child and parent
			// (fraction 0.5) for every connector - fine on its own, but siblings connecting to the
			// SAME parent share the same x1/x2 (they're all in the same column), so their midpoints
			// were identical too: their vertical bend segments ran along the exact same line, visibly
			// overlapping each other even after the *endpoint* spread above pulled the arrowheads
			// apart. Spreading the bend fraction per sibling as well (a narrower range than the
			// endpoint spread, so it stays a subtle stagger rather than a zig-zag) keeps every
			// sibling's connector on its own visually distinct path. A single child still bends at
			// the true midpoint (spread=0.5 -> fraction=0.5), unchanged.
			// Biased close to the CHILD end (fraction near 0 = bend right at x1/the child, near 1 =
			// bend right at x2/the parent) rather than the middle - a tucked leaf's vertical run then
			// happens right next to its own box, inside its own column, instead of out in the gap
			// where it visually grazes the neighboring column's boxes/connectors (reported by the user
			// as still looking crowded even though it technically never crossed anything measurable).
			var baseBendFraction = 0.15 + 0.2 * spread;
			// Rare case (a tucked sibling sitting in the gap this elbow needs to cross - see the
			// allBoxRects comment above): if the natural bend fraction's path would cut through some
			// unrelated box, nudge the bend point along a few nearby alternatives and use the first
			// one that clears everything, instead of just accepting the collision. Candidates are
			// ordered to try moving even closer to the child first, then further out towards the
			// parent only as a last resort - preserving the "hug the origin" intent above whenever
			// there's a collision-free spot that still does.
			var bendCandidates = [0, -0.07, 0.1, -0.14, 0.2, 0.35, 0.5, 0.65];

			var x1, y1, x2, y2, d;
			if (horizontal) {
				x1 = r.left   - containerRect.left; y1 = (r.top   + r.bottom)  / 2 - containerRect.top;
				x2 = pr.right - containerRect.left; y2 = pr.top + pr.height * spread - containerRect.top;
				var midX = x1 + (x2 - x1) * baseBendFraction;
				for (var bi = 0; bi < bendCandidates.length; bi++) {
					var candFraction = Math.min(0.85, Math.max(0.06, baseBendFraction + bendCandidates[bi]));
					var candX = x1 + (x2 - x1) * candFraction;
					if (!elbowCrossesOtherBox([[x1,y1],[candX,y1],[candX,y2],[x2,y2]])) { midX = candX; break; }
				}
				d = 'M' + x1 + ',' + y1 + ' L' + midX + ',' + y1 + ' L' + midX + ',' + y2 + ' L' + x2 + ',' + y2;
			} else {
				x1 = (r.left  + r.right)  / 2 - containerRect.left; y1 = r.top     - containerRect.top;
				x2 = pr.left + pr.width * spread - containerRect.left; y2 = pr.bottom - containerRect.top;
				var midY = y1 + (y2 - y1) * baseBendFraction;
				for (var bj = 0; bj < bendCandidates.length; bj++) {
					var candFractionV = Math.min(0.85, Math.max(0.06, baseBendFraction + bendCandidates[bj]));
					var candY = y1 + (y2 - y1) * candFractionV;
					if (!elbowCrossesOtherBox([[x1,y1],[x1,candY],[x2,candY],[x2,y2]])) { midY = candY; break; }
				}
				d = 'M' + x1 + ',' + y1 + ' L' + x1 + ',' + midY + ' L' + x2 + ',' + midY + ' L' + x2 + ',' + y2;
			}

			var path = document.createElementNS(svgNS, 'path');
			path.setAttribute('d', d);
			path.setAttribute('fill', 'none');
			path.setAttribute('stroke', '#8a8a8a');
			path.setAttribute('stroke-width', '1.5');
			path.setAttribute('marker-end', 'url(#' + markerId + ')');
			svg.appendChild(path);
		});

		treeEl.insertBefore(svg, treeEl.firstChild);
	}

	// ─────────────────────────────────────────────────────────────────────────
	// Compact ("join-chain aware") layout — opt-in alternative to the default CSS org-chart
	// technique above.
	//
	// The org-chart technique centers a parent over the combined span of ALL its children. That's
	// fine for balanced trees, but the single most common shape in a real plan is a long left-deep
	// join chain: each operator has one child that continues the chain (a huge subtree) and one
	// that's a single new table (a tiny leaf). Centering a parent between "huge" and "tiny" drags
	// it sideways a little at every level, and across dozens of chained joins that compounds into
	// a runaway "V" spread (reported directly by a user comparing against html-query-plan's much
	// tighter staircase for the same query shape).
	//
	// An earlier version of this mode computed absolute pixel positions in JS - it fixed the join
	// chain itself, but every OTHER branch shape (anything with 2+ real children - RESTRICT,
	// SQFILTER, UNION ALL, etc.) fell back to reserving a sibling's full bounding box before
	// stacking the next one below it, which for a long diagonal chain meant reserving its entire
	// (legitimately large) diagonal span even though most of that space wasn't actually in the
	// way. Net result: everything except the join chain itself stayed sprawled out.
	//
	// The actual fix (see .ase-plan-compact-h/-v in the CSS above) is to stop computing positions
	// in JS at all and instead reuse renderNode()'s existing <ul>/<li> DOM as nested native HTML
	// tables (display:table / table-cell) - exactly what html-query-plan/QP.js itself does (see
	// .qp-tr/.qp-node-outer in src/com/dbxtune/sql/showplan/sqlserver/css/qp.css). The browser's
	// own table reflow packs each sibling subtree using only the space it actually needs, for
	// every branching shape uniformly - not just the join-chain special case - with no custom
	// geometry to get wrong. Box/tooltip/icon code above is untouched either way; only the CSS
	// classes applied to $tree change, plus reusing the existing SVG drawConnectorLines() for
	// arrows once the browser has laid everything out for real.
	//
	// One DOM-order dependency the table technique has that the org-chart technique didn't:
	// vertical-align:top on a join's own table-cell (see CSS above) aligns it with whichever child
	// is stacked FIRST in its <ul>. Parser order puts the continuing chain before the leaf, so
	// without this pass every join aligned to the top of the huge continuing subtree instead of its
	// own (tiny) leaf - each leaf then trailed further and further behind as the chain got deeper.
	//
	// First fix here sorted leaves before continuing children (whichever child had no children of
	// its own went first) - that made every join flush with SOME leaf, but not necessarily its own,
	// and had no relationship to the VA numbers now shown on each box: a user comparing the diagram
	// to the VA reading order ("lower number executes first") found the visual order agreed with VA
	// order in some places and disagreed in others, since leaf-vs-continuing and lower-VA-vs-higher-
	// VA are just different axes that happen to correlate only part of the time. Sorting by ascending
	// VA instead removes that mismatch entirely - the diagram's top-to-bottom order always matches
	// execution order - at the cost of occasionally not being flush with the smallest child, on the
	// (real-world-measured) minority of joins where the continuing chain happens to have a lower VA
	// than its sibling leaf.
	function reorderCompactByVa(treeEl) {
		treeEl.querySelectorAll('li').forEach(function (li) {
			var ul = li.querySelector(':scope > ul');
			if (!ul) return;
			var kids = Array.prototype.slice.call(ul.children);
			if (kids.length < 2) return;
			var va = function (kidLi) {
				var el = kidLi.querySelector(':scope > .ase-plan-box-cell > .ase-plan-box > .ase-plan-va');
				var n = el ? parseInt(el.textContent, 10) : NaN;
				if (!isNaN(n)) return n;
				// No VA at all - e.g. a HashUnion/HashJoin's own spool <WorkTable> in an XML plan.
				// Unlike its VA-numbered siblings, that's not a data-flow step with an execution order
				// of its own - just scratch space the operator sets up alongside the children that
				// actually feed it. Sort it first rather than last so it reads as "this operator also
				// uses X" ahead of those children, matching where ASE's own XML already lists it
				// (before its sibling scan operators). Any other no-VA case still sorts last, the
				// original safe default, since WorkTable is the only known real-world instance of this.
				var labelEl = kidLi.querySelector(':scope > .ase-plan-box-cell > .ase-plan-box > .ase-plan-label');
				var label = labelEl ? labelEl.textContent : '';
				return /^work\s*table$/i.test(label) ? -1 : Infinity;
			};
			kids.sort(function (a, b) { return va(a) - va(b); });
			kids.forEach(function (kidLi) { ul.appendChild(kidLi); });
		});
	}

	// Sorting by VA (above) fixed the "sometimes matches, sometimes doesn't" complaint, but exposed a
	// separate structural issue: whichever child block-stacks SECOND in a table cell starts only after
	// the FIRST child's full natural height - and for a "continuing chain" child, that natural height
	// is its entire recursive subtree (the chain's own <li> is itself a nested table whose row height
	// is the max of its own box and ITS children-cell, recursively all the way down), not just its own
	// box. So when the chain sorts before its sibling leaf (chain VA < leaf VA - common), the leaf gets
	// pushed hundreds of pixels down by a subtree that actually extends sideways (into deeper table
	// columns), not down in this column at all - "why can't 26 sit just below 25 instead of trailing
	// the entire subtree" was exactly this.
	//
	// Fix: a leaf has no children of its own, so it doesn't need to participate in that block-stacking
	// flow at all. Pull it out of flow (position:absolute) and place it in a small band sized from
	// real measured box heights (box height isn't fixed - it grows with an optional subtitle/metric
	// line), so the visual gap depends only on the leaf's own small size, never on how deep the OTHER
	// sibling's chain continues.
	//
	// First version of this always tucked every leaf into a band at the very TOP of the cell,
	// regardless of VA - which silently undid reorderCompactByVa() for exactly the joins it mattered
	// most for: when the continuing chain has the LOWER VA (chain executes first - the common case),
	// tucking the leaf above it put the HIGHER VA operator physically higher on screen, the opposite
	// of "lower VA reads first" (caught by the user comparing against the VA badges directly). Fixed
	// by tucking relative to the chain's own position instead of unconditionally to the top: a leaf
	// that VA-sorts BEFORE the chain tucks into a band above it (as before); a leaf that VA-sorts
	// AFTER the chain tucks into a band starting right below the chain's OWN box - specifically its
	// own small box height, not its full recursive subtree height (measured separately: the chain
	// li's natural height reflects its whole subtree per the comment above, but .ase-plan-box itself,
	// one level in, is never stretched - see the .ase-plan-box-cell comment above in the CSS block).
	// Either way the chain still flows normally and still needs its full natural subtree height
	// reserved in the cell - only the LEAF's position is decoupled from that height, never the
	// chain's own layout. Horizontal mode only for now - vertical mode's transposed table-row/
	// table-cell structure would need mirrored left/right positioning instead of top, not yet done.
	function tuckLeavesNearParent(treeEl) {
		var GAP = 9; // matches the li > ul > li margin in the CSS above
		treeEl.querySelectorAll('li').forEach(function (li) {
			var ul = li.querySelector(':scope > ul');
			if (!ul) return;
			var kids = Array.prototype.slice.call(ul.children); // already VA-sorted, see reorderCompactByVa()
			var leafKids = [], nonLeafKids = [];
			kids.forEach(function (k) {
				(k.querySelectorAll(':scope > ul > li').length === 0 ? leafKids : nonLeafKids).push(k);
			});
			// Only handle the common "one chain, one or more leaves" shape - a node with 2+ continuing
			// children is the separate "balanced" case (see reorderCompactByVa()'s comment), where
			// every child's full subtree height genuinely is needed to avoid its descendants colliding
			// with a sibling's, so it's left on normal block-stacking untouched.
			if (!leafKids.length || nonLeafKids.length !== 1) return;
			var chainLi = nonLeafKids[0];
			var chainIndex = kids.indexOf(chainLi);

			ul.style.position = 'relative';
			// A CSS-absolutely-positioned child is placed relative to its containing block's PADDING
			// edge, not its content edge - so "left: 0" here would land the leaf flush against the
			// padding edge, i.e. INSIDE the ul's own padding-left, undoing that padding rather than
			// respecting it. Reading the real computed value (rather than hardcoding the CSS's 40px)
			// keeps this from silently drifting out of sync if that padding-left ever changes.
			var stepLeft = window.getComputedStyle(ul).paddingLeft || '0px';
			var maxLeafWidth = 0;

			function tuck(leafLi, top) {
				var box = leafLi.querySelector(':scope > .ase-plan-box-cell > .ase-plan-box');
				if (!box) return 76;
				var rect = box.getBoundingClientRect();
				maxLeafWidth = Math.max(maxLeafWidth, rect.width);
				leafLi.style.position = 'absolute';
				leafLi.style.top = top + 'px';
				leafLi.style.left = stepLeft;
				leafLi.style.margin = '0';
				return rect.height;
			}

			var beforeChain = kids.slice(0, chainIndex).filter(function (k) { return leafKids.indexOf(k) >= 0; });
			var afterChain  = kids.slice(chainIndex + 1).filter(function (k) { return leafKids.indexOf(k) >= 0; });

			var offset = 0;
			beforeChain.forEach(function (leafLi) { offset += tuck(leafLi, offset) + GAP; });
			// Reserves exactly the "before" leaves' own height for them, so the (still block-flowing)
			// chain starts right after that small band instead of unconditionally at the cell's top.
			ul.style.paddingTop = offset + 'px';

			var chainOwnBox = chainLi.querySelector(':scope > .ase-plan-box-cell > .ase-plan-box');
			var chainOwnHeight = chainOwnBox ? chainOwnBox.getBoundingClientRect().height : 76;
			var afterOffset = offset + chainOwnHeight + GAP;
			afterChain.forEach(function (leafLi) { afterOffset += tuck(leafLi, afterOffset) + GAP; });

			// A tucked leaf's box width (up to the CSS max-width, driven by however long its label
			// text is) no longer feeds into the native table's own column-width calculation once it's
			// pulled out of flow via position:absolute - only the chain's own (possibly narrower) box
			// still does. So if some tucked leaf is wider than the chain's own box, the chain's OWN
			// children (one column further right) would otherwise start too close and visually collide
			// with that wider tucked sibling. Widen the gap before the chain's own children by exactly
			// the excess to compensate - the chain's own box position/width is untouched, only where
			// ITS children begin shifts right.
			var chainOwnWidth = chainOwnBox ? chainOwnBox.getBoundingClientRect().width : 0;
			if (maxLeafWidth > chainOwnWidth) {
				var chainChildrenUl = chainLi.querySelector(':scope > ul');
				if (chainChildrenUl) {
					var chainStep = parseFloat(window.getComputedStyle(chainChildrenUl).paddingLeft) || 0;
					chainChildrenUl.style.paddingLeft = (chainStep + (maxLeafWidth - chainOwnWidth)) + 'px';
				}
			}
		});
	}

	// Mirrors tuckLeavesNearParent() above for vertical (top-to-bottom) mode's transposed table-row/
	// table-cell structure: siblings sit side by side (left-to-right) instead of stacked top-to-bottom,
	// so the same bug shows up rotated 90 degrees - a leaf's table-CELL used to start only after the
	// chain sibling's full subtree WIDTH (a deep chain fans out into many cells further down and can be
	// very wide), pushing a small leaf box far to the right of where it actually connects, with a big
	// empty gap in between (reported directly by the user pointing at exactly this on a real render:
	// "move right operator closer to the left operator"). Same fix, same two axes swapped: pull the
	// leaf out of the table-row's cell flow via position:absolute and place it in a small band sized
	// from the chain's own (not its subtree's) measured box width, tucked left of the chain if the
	// leaf's VA sorts before it, right of the chain (starting right after the chain's own box width,
	// not its subtree width) otherwise.
	function tuckLeavesNearParentVertical(treeEl) {
		var GAP = 9;
		// Unlike the horizontal version, a chain's box POSITION here depends on its own cell's width
		// (it's centered within it, per the caption-based CSS above), which in turn depends on whether
		// ITS OWN children have already been tucked - so processing has to go bottom-up (descendants
		// before ancestors), not top-down: querySelectorAll() returns document/pre-order (ancestors
		// first), so every ancestor-descendant pair's order is simply reversed by reversing the whole
		// list, without needing a real tree walk.
		Array.prototype.slice.call(treeEl.querySelectorAll('li')).reverse().forEach(function (li) {
			var ul = li.querySelector(':scope > ul');
			if (!ul) return;
			var kids = Array.prototype.slice.call(ul.children); // already VA-sorted, see reorderCompactByVa()
			var leafKids = [], nonLeafKids = [];
			kids.forEach(function (k) {
				(k.querySelectorAll(':scope > ul > li').length === 0 ? leafKids : nonLeafKids).push(k);
			});
			// Same restriction as the horizontal version - only the common "one chain, one or more
			// leaves" shape; 2+ continuing children is left on normal table-row flow untouched.
			if (!leafKids.length || nonLeafKids.length !== 1) return;
			var chainLi = nonLeafKids[0];
			var chainIndex = kids.indexOf(chainLi);

			ul.style.position = 'relative';
			var ulRect = ul.getBoundingClientRect();
			// Every normal (non-tucked) cell in this row gets its vertical offset from its own CSS
			// padding-top (40px, set via ".ase-plan-tree.ase-plan-compact-v li > ul > li"), not from the
			// row itself - reading it from the chain cell (which stays untouched, still a real table
			// cell throughout) keeps a tucked leaf's own top offset in sync with that CSS value instead
			// of hardcoding it.
			var stepTop = window.getComputedStyle(chainLi).paddingTop || '0px';
			var maxLeafHeight = 0;

			function tuck(leafLi, leftRel) {
				var box = leafLi.querySelector(':scope > .ase-plan-box-cell > .ase-plan-box');
				if (!box) return 130;
				var rect = box.getBoundingClientRect();
				maxLeafHeight = Math.max(maxLeafHeight, rect.height);
				leafLi.style.position = 'absolute';
				leafLi.style.left = leftRel + 'px';
				leafLi.style.top = stepTop;
				leafLi.style.padding = '0';
				return rect.width;
			}

			var beforeChain = kids.slice(0, chainIndex).filter(function (k) { return leafKids.indexOf(k) >= 0; });
			var afterChain  = kids.slice(chainIndex + 1).filter(function (k) { return leafKids.indexOf(k) >= 0; });

			var offset = 0;
			beforeChain.forEach(function (leafLi) { offset += tuck(leafLi, offset) + GAP; });
			// Reserves exactly the "before" leaves' own width for them, so the (still normal-flow) chain
			// cell starts right after that small band instead of unconditionally at the row's left edge.
			// Has to go on the CHAIN CELL, not the row (`ul`, display:table-row) - padding on a table-row
			// isn't rendered at all per the CSS table model (unlike the horizontal version's equivalent,
			// which targets a table-CELL where padding does apply).
			if (offset > 0) chainLi.style.paddingLeft = offset + 'px';

			// The chain's own box is CENTERED (a caption, ".ase-plan-compact-v li > .ase-plan-box-cell")
			// over its own cell's FULL width - which is sized to fit its entire subtree, not just its own
			// box, and can be far wider once its descendants fan out. So unlike the horizontal version
			// (whose box-cells are never centered, always flush), the chain's box left/right edges can't
			// be derived by arithmetic from its own width alone - unaccounted centering silently ate part
			// of the intended gap and let the first after-tucked leaf overlap the chain's box. Measure the
			// real rendered edges directly instead, after the before-chain reservation above (which shifts
			// the chain, and everything centered inside it, right by `offset`).
			var chainOwnBox = chainLi.querySelector(':scope > .ase-plan-box-cell > .ase-plan-box');
			var chainRect = chainOwnBox ? chainOwnBox.getBoundingClientRect() : null;
			var afterOffset = chainRect ? (chainRect.right - ulRect.left + GAP) : (offset + 130 + GAP);
			afterChain.forEach(function (leafLi) { afterOffset += tuck(leafLi, afterOffset) + GAP; });

			// After-tucked leaves are pulled out of flow entirely (position:absolute), so - unlike the
			// before-tucked band, which stays accounted for via the real padding-left set on chainLi
			// above - nothing in normal flow reports how far right they actually reach. Left alone, this
			// node's own <li> (itself a table, per the CSS above) auto-sizes to only its in-flow content
			// (the chain's own subtree) and reports that narrower width to ITS OWN parent's row - which
			// then positions the NEXT sibling column (an entirely unrelated branch) as if this node were
			// only that narrow, letting it overlap the tucked leaf sticking out past it. Reported live by
			// the user on the real dialog: two unrelated "Index Scan" boxes rendered stacked on top of
			// each other. Force this node's own reported width to cover the true rightmost extent.
			if (afterChain.length) {
				var curMinWidth = parseFloat(window.getComputedStyle(li).minWidth) || 0;
				li.style.minWidth = Math.max(curMinWidth, afterOffset) + 'px';
			}

			// Mirrors the horizontal version's width-collision compensation, on the perpendicular axis:
			// a tucked leaf's HEIGHT no longer feeds into this row's natural height once pulled out of
			// flow, so a leaf taller than the chain's own box could otherwise have its bottom edge run
			// into the chain's OWN children (the next row down, whose top offset is only sized from the
			// chain cell's natural height). Push that next row down by the excess when needed.
			var chainOwnHeight = chainRect ? chainRect.height : 0;
			if (maxLeafHeight > chainOwnHeight) {
				var chainChildrenUl = chainLi.querySelector(':scope > ul');
				if (chainChildrenUl) {
					var extra = maxLeafHeight - chainOwnHeight;
					Array.prototype.forEach.call(chainChildrenUl.children, function (cellLi) {
						var curPad = parseFloat(window.getComputedStyle(cellLi).paddingTop) || 0;
						cellLi.style.paddingTop = (curPad + extra) + 'px';
					});
				}
			}
		});
	}

	function render(container, parsed, opts) {
		injectStyle();
		var $container = $(container);
		$container.empty();
		if (!parsed || !parsed.statements || !parsed.statements.length) return;

		var horizontal        = !!(opts && opts.horizontal);
		var useCompactLayout  = !!(opts && opts.layout === 'compact');
		var useLineConnectors = useCompactLayout || !!(opts && opts.connectorStyle === 'lines');
		var $wrap = $('<div class="ase-plan-wrap"></div>');
		var multiStatement = parsed.statements.length > 1;
		var treesForLineDrawing  = []; // arrows drawn after everything is live/laid-out by the browser
		var treesForCompactTuck  = []; // leaves tucked near their parent after the natural table layout

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
				if (useCompactLayout) {
					$tree.addClass(horizontal ? 'ase-plan-compact-h' : 'ase-plan-compact-v');
				} else if (horizontal) {
					$tree.addClass('ase-plan-horizontal');
				}
				if (useLineConnectors) $tree.addClass('ase-plan-lines');
				if (useLineConnectors) treesForLineDrawing.push($tree);
				if (useCompactLayout) treesForCompactTuck.push($tree);
				var $rootUl = $('<ul></ul>');
				$rootUl.append(renderNode(step.root));
				$tree.append($rootUl);
				if (useCompactLayout) reorderCompactByVa($tree[0]);
				$wrap.append($tree);
			});
		});

		// Close open detail panels when clicking anywhere else in the diagram.
		$wrap.on('click', function () { $wrap.find('.ase-plan-detail').remove(); });

		$container.append($wrap);

		// Both need real measured positions/sizes (getBoundingClientRect() reports zero for detached
		// elements), so both run after $tree is attached to the live document. Tucking must run before
		// the connector lines are drawn, so the arrows reflect the tucked (final) positions, not the
		// pre-tuck natural table-flow ones.
		if (useCompactLayout) {
			var tuckFn = horizontal ? tuckLeavesNearParent : tuckLeavesNearParentVertical;
			treesForCompactTuck.forEach(function ($tree) { tuckFn($tree[0]); });
		}
		treesForLineDrawing.forEach(function ($tree) { drawConnectorLines($tree[0], horizontal); });
	}

	return {
		parseXml: parseXml,
		parseText: parseText,
		render: render
	};
})();
