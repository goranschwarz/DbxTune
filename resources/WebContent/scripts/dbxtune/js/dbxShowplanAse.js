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
 * XML plans additionally carry `plans` - one entry per <plan> block ASE cached for the statement
 * (sorted worst-total-time-first), each { planId, execCount, avgTimeUs, totalTimeUs, statements }
 * with the same `statements` shape as above; top-level `statements` is a convenience alias for
 * plans[0].statements (the default selection) so single-plan XML and text plans render unchanged.
 */

window.AseShowplan = (function () {

	// Reason the most recent parseXml()/parseText() call returned null, for callers that want to
	// tell the user *why* the graphical diagram fell back to raw text (both parsers keep returning
	// null on failure rather than throwing, to preserve the "not recognized -> fall back" contract
	// documented above - this just adds a diagnostic alongside it).
	var lastParseError = null;

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
		// _xmlEl is kept so the Properties pane can show this operator's own XML generically, the same
		// way the SQL Server renderer does - the pane can then present EVERY captured attribute,
		// including ones no parser rule above knows about, instead of only the curated props below.
		var node = { op: el.tagName, label: el.tagName, metrics: {}, props: {}, children: [], _xmlEl: el };

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
				// wtObjName is WorkTable's own object-name property, not a child operator - only
				// reached here when buildXmlNode() is called directly on a <WorkTable> element (the
				// 'WorkTable' branch below normally intercepts it first; kept as a fallback in case
				// some plan shape nests it differently).
				node.props.objName = xmlText(kid);
			} else if (tag === 'WorkTable') {
				// <WorkTable><wtObjName>WorkTable2</wtObjName></WorkTable> is not a real upstream
				// operator feeding this one - it means the operator it sits under (SORT/SortDistinct
				// spilling to disk, HashUnion/HashJoin building its internal hash table, ...)
				// materializes and uses this worktable itself, as part of its own execution. Text
				// plans already reflect that: "Using Worktable1 for internal storage." becomes
				// props.workTable on the operator's OWN node, never a separate box. Folding this XML
				// sibling the same way - instead of recursing it into node.children like an unknown
				// operator - keeps it from rendering as a misleading "input" box, and lets the
				// existing Sort Spill finding (which keys off props.workTable) fire for XML plans too.
				var wtName = xmlText(xmlFirstChildByTag(kid, 'wtObjName'));
				if (wtName) node.props.workTable = wtName;
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

		// XML's <StoreIndex> is the same operator the text parser recognizes via "...for
		// REFORMATTING."/"Creating clustered index." (see the DETAIL_PATTERNS entry above) - SAP's own
		// doc defines the two abstract-plan physical operators this pair of tags mirrors: "store" (a
		// worktable, no index - <Store>) vs. "store_index" (a *clustered index* worktable - the
		// reformatting one, <StoreIndex>). Mapped onto the same props.reformatWorktable field
		// collectTreeFindings()/findReformatSource() already key off for text plans, so everything
		// downstream (the box marking, the Plan Analysis finding, the index suggestion) just works.
		if (/^StoreIndex$/i.test(node.op)) {
			// Confirmed against a real captured plan: the worktable's name isn't on the StoreIndex
			// element itself - it's the Insert child's own <objName> (<Insert>...<objName>Worktable1
			// </objName></Insert>). A <WorkTable><wtObjName> sibling (mirroring HashUnion's internal-
			// storage annotation) is checked too in case some ASE version/operator shapes it that way.
			var wtName = node.props.objName;
			for (var wi = 0; wi < node.children.length && !wtName; wi++) {
				var kidNode = node.children[wi];
				var kidObjName = kidNode.props && kidNode.props.objName;
				if (/^Work\s*Table$/i.test(kidNode.op || '') && kidObjName) wtName = kidObjName;
				else if (kidObjName && /^Worktable/i.test(kidObjName)) wtName = kidObjName;
			}
			node.props.reformatWorktable = wtName || 'a worktable';
		}

		return node;
	}

	// ASE can cache several distinct compiled plans for the very same statement (e.g. one compiled
	// per differing literal/parameter values, or after auto-recompiles) - show_cached_plan_in_xml
	// then emits several sibling <plan> elements under one <query>, each with its own <planId>,
	// <execCount>/<avgTime>/<avgExecTime>, and full <opTree>. Earlier this only ever looked at the
	// FIRST <plan> in the document (jQuery's .find('plan').first()) and silently discarded the
	// rest - real captures with 5 plans and wildly different execCount/avgTime profiles confirmed
	// this was quietly throwing away the majority of the data. Every <plan> is now parsed into its
	// own entry in the returned `plans` array (sorted worst-total-time-first, see totalTimeUs
	// below), and the caller (dbxShowplan.js) offers a selector when there's more than one.
	function parseXml(xmlString) {
		lastParseError = null;
		if (!xmlString || !xmlString.trim()) { lastParseError = 'Plan text is empty'; return null; }
		// A leading blank line/whitespace before "<?xml ...?>" is otherwise a fatal parse error -
		// the XML spec only allows the declaration as the very first thing in the document - but
		// it's harmless and common in captured plans, so strip it rather than failing on it.
		xmlString = xmlString.trim();
		try {
			var xmlDoc = $.parseXML(xmlString);
			var $doc   = $(xmlDoc);
			var $plans = $doc.find('plan');
			if (!$plans.length) { lastParseError = 'No <plan> element found in the XML plan'; return null; }

			var statementId = $doc.find('statementId').first().text() || undefined;
			var label = 'Statement' + (statementId ? ' ' + statementId : '');

			var plans = [];
			for (var i = 0; i < $plans.length; i++) {
				var $plan = $plans.eq(i);
				var $opTree = $plan.children('opTree').first();
				if (!$opTree.length) continue; // malformed <plan> - skip rather than fail the whole doc

				var rootEl = xmlFirstChildByTagIgnoreOrder($opTree[0]);
				if (!rootEl) continue;
				var root = buildXmlNode(rootEl);

				var meta = {};
				meta.planId       = $plan.children('planId').first().text() || undefined;
				meta.execCount    = $plan.children('execCount').first().text() || undefined;
				meta.avgTime      = $plan.children('avgTime').first().text() || undefined;
				meta.avgExecTime  = $plan.children('avgExecTime').first().text() || undefined;

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

				// execCount * avgTime approximates this plan variant's total real-world cost - the
				// basis for both the default selection and the sort order offered to the user (the
				// plan actually costing the most time overall is more actionable than the one that
				// merely happens to be listed first in the XML).
				var execCountNum = parseFloat(meta.execCount);
				var avgTimeNum   = parseFloat(meta.avgTime);
				var totalTimeUs  = (!isNaN(execCountNum) && !isNaN(avgTimeNum)) ? execCountNum * avgTimeNum : 0;

				plans.push({
					planId: meta.planId,
					execCount: isNaN(execCountNum) ? undefined : execCountNum,
					avgTimeUs: isNaN(avgTimeNum) ? undefined : avgTimeNum,
					totalTimeUs: totalTimeUs,
					statements: [ { label: label, meta: meta, steps: [ { label: null, root: root } ] } ]
				});
			}
			if (!plans.length) { lastParseError = 'No usable <opTree> found in any <plan> element'; return null; }

			plans.sort(function (a, b) { return b.totalTimeUs - a.totalTimeUs; });

			return {
				format: 'xml',
				rawText: xmlString,
				plans: plans,
				statements: plans[0].statements
			};
		} catch (ex) {
			lastParseError = 'XML parse error: ' + (ex && ex.message ? ex.message : ex);
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
		// More specific than the generic "Worktable created" rule below - must come first (first
		// match wins) so a reformatting STORE's worktable is flagged via props.reformatWorktable,
		// not just given a plain props.objName like any other worktable-creating operator.
		{ re: /^(Worktable#?\s*\d+)\s+created,?\s+in\s+\w+\s+locking mode,?\s+for\s+reformatting\.?$/i,
			handlerRe: function (node, m) { node.props.objName = m[1].replace(/\s+/g, ''); node.props.reformatWorktable = node.props.objName; } },
		{ re: /^(Worktable#?\s*\d+)\s+created\b.*$/i,              handlerRe: function (node, m) { node.props.objName = m[1].replace(/\s+/g, ''); } },
		{ re: /^Table Scan\.?$/i,                                  set: { scanType: 'TableScan' } },
		{ re: /^(Forward|Backward) Scan\.?$/i,                     handlerRe: function (node, m) { node.props.scanOrder = m[1] + 'Scan'; } },
		{ re: /^Positioning at start of table\.?$/i,               set: { positioning: 'StartOfTable' } },
		{ re: /^Positioning at end of table\.?$/i,                 set: { positioning: 'EndOfTable' } },
		{ re: /^Positioning by key\.?$/i,                          set: { positioning: 'ByKey' } },
		{ re: /^Using Clustered Index\.?$/i,                       set: { scanType: 'ClusteredIndexScan' } },
		{ re: /^Index\s*:\s*(.+)$/i,                                handlerRe: function (node, m) { node.props.indexName = m[1].trim(); } },
		{ re: /^\(Total Rows:\s*(\d+)\)$/i,                         handlerRe: function (node, m) { node.props.statTotalRows = m[1]; } },
		// Both of these lines come in a data-pages and an index-leaf-pages flavour, and ASE prints them
		// for the same operator when a scan reads both. They used to be collapsed onto the data* keys,
		// which mislabelled the index line and disagreed with the XML parser - show_cached_plan_in_xml
		// has always had separate indexIOSizeInKB / indexBufReplStrategy elements (see
		// XML_PROPERTY_TAGS), so the same plan reported different property names depending on how it
		// was captured. Routed on the captured page kind so the two capture formats now agree.
		{ re: /^Using I\/O Size (\d+) Kbytes for (data pages|index leaf pages)\.?$/i,
		  handlerRe: function (node, m) {
			node.props[/index/i.test(m[2]) ? 'indexIOSizeInKB' : 'dataIOSizeInKB'] = m[1];
		  } },
		// Kept deliberately permissive after "Strategy" (rather than requiring one of the two page
		// kinds) so any wording this ASE version prints still parses at all; the trailing text is only
		// inspected to decide which of the two keys it belongs to, defaulting to data.
		{ re: /^With (.+) Buffer Replacement Strategy(.*)$/i,
		  handlerRe: function (node, m) {
			node.props[/index/i.test(m[2]) ? 'indexBufReplStrategy' : 'dataBufReplStrategy'] = m[1].trim();
		  } },
		{ re: /^External Definition:\s*(.+)$/i,                    handlerRe: function (node, m) { node.props.externalDef = m[1].trim(); } },
		{ re: /^Evaluate (Ungrouped|Grouped) (.+) AGGREGATE\.?$/i, handlerRe: function (node, m) { node.props.aggregate = m[1] + ' ' + m[2]; } },
		{ re: /^Nested iteration\.?$/i,                             set: { joinStrategy: 'NestedIteration' } },
		{ re: /^Using Worktable(\d+) for internal storage\.?$/i,   handlerRe: function (node, m) { node.props.workTable = 'Worktable' + m[1]; } },
		{ re: /^Key Count:\s*(\d+)$/i,                              handlerRe: function (node, m) { node.props.keyCount = m[1]; } },
		// Both seen in a "full" sp_showplan and previously falling through to node.raw as
		// unrecognized. "deferred_varcol"/"deferred_index" already drive a Plan Analysis finding
		// further down; this just records the mode itself, whatever it is, as a normal property.
		{ re: /^The update mode is (\w+)\.?$/i,                     handlerRe: function (node, m) { node.props.updateMode = m[1]; } },
		{ re: /^Key Ordering:\s*(.+?)\.?$/i,                        handlerRe: function (node, m) { node.props.keyOrdering = m[1].trim(); } },
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
		// _sourceLines: this operator's OWN lines from the captured sp_showplan text, verbatim (pipes
		// and all), collected by parseStep() as it consumes them. A text plan has no XML for the
		// Properties pane to show, so it shows these instead - the plain-text counterpart of _xmlEl.
		// Distinct from `raw`, which is a narrower bucket: only the detail lines no rule recognized,
		// and already stripped of their leading pipe indentation.
		return { op: name, label: name, metrics: {}, props: props, raw: [], children: [], extra: extra,
		         _sourceLines: [] };
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
				// The operator's own header line starts its source block. `line`, not `content`: the
				// original indentation is part of how the captured plan reads.
				node._sourceLines.push(line.replace(/\s+$/, ''));

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
				if (target) {
					// Belongs to the operator currently open at this depth - record it verbatim before
					// the parser picks it apart, so the Properties pane can show the operator's own
					// block exactly as ASE printed it.
					target._sourceLines.push(line.replace(/\s+$/, ''));
					applyDetailLine(target, content);
				}
				// else: detail line with no matching operator at this depth — ignore, best-effort.
			}
		}
		return root;
	}

	// Sections a "full" sp_showplan prints around the operator tree. All optional - a short plan has
	// none of them - so every one of these is best-effort and never fails the parse.
	// num: round to a whole number and localize it (ASE prints these with six decimals of precision
	// nobody reads - "246209.208247" says nothing "246,209" doesn't). unit is appended after
	// formatting, so the number is grouped but the unit isn't dragged into the digits.
	var TOTALS_PATTERNS = [
		{ key: 'Est. total I/O cost',   re: /^Total estimated I\/O cost for statement\s+\d+\s*\(at line \d+\):\s*([\d.]+)/i, num: true },
		// ASE counts procedure cache in 2K pages (same unit sp_configure/sp_monitorconfig
		// 'procedure cache size' uses), so the raw number alone means little - the size it works out
		// to is the part worth reading.
		{ key: 'Proccache used',        re: /^Proccache used during compilation:\s*([\d.]+)/i, num: true, pages2k: true },
		{ key: 'Est. total LIO',        re: /^Total estimated LIO:\s*([\d.]+)/i, num: true },
		{ key: 'Est. total PIO',        re: /^Total estimated PIO:\s*([\d.]+)/i, num: true },
		{ key: 'Est. total CPU time',   re: /^Total estimated CPU time:\s*([\d.]+)/i, num: true },
		{ key: 'Query started at',      re: /^Query has started at:\s*(.+?)\s*\.?\s*$/i },
		{ key: 'Query running for',     re: /^Query is running for:\s*([\d.]+)\s*([A-Za-z]*)/i, num: true, unitFrom: 2 }
	];

	/** Whole number, locale-grouped - the same treatment fmtNum() gives every other count. */
	function fmtWholeLocalized(v) {
		var n = parseFloat(v);
		return isNaN(n) ? v : Math.round(n).toLocaleString();
	}

	/** "108" 2K pages -> "216 KB" (or MB once it gets big enough to be worth reading that way). */
	function fmt2kPages(v) {
		var n = parseFloat(v);
		if (isNaN(n)) return undefined;
		var kb = n * 2;
		return kb >= 1024 ? (Math.round(kb / 1024 * 10) / 10).toLocaleString() + ' MB'
		                  : Math.round(kb).toLocaleString() + ' KB';
	}

	/** The "Total estimated ..." block printed after a statement's operator tree, if present. */
	function parseStatementTotals(lines) {
		var out = null;
		lines.forEach(function (raw) {
			var line = raw.trim();
			if (!line) return;
			for (var i = 0; i < TOTALS_PATTERNS.length; i++) {
				var pat = TOTALS_PATTERNS[i];
				var m = line.match(pat.re);
				if (m) {
					// ASE ends these lines with a period ("... : 131740171."), which the numeric
					// capture happily swallows. Only a TRAILING dot is removed - the decimal point in
					// "246209.208247" has to survive long enough to be rounded.
					var v = m[1].trim().replace(/\.$/, '');
					var asSize = pat.pages2k ? fmt2kPages(v) : undefined;
					if (pat.num) v = fmtWholeLocalized(v);
					if (pat.unitFrom && m[pat.unitFrom]) v += ' ' + m[pat.unitFrom];
					if (asSize) v += ' (' + asSize + ')';
					(out = out || {})[pat.key] = v;
					return;
				}
			}
		});
		return out;
	}

	var TABLE_ROW_RE     = /^TABLE:\s*\[?([^\]\t]+?)\]?\s+rows:\s*([\d]+)\s+use count:\s*([\d]+)(?:\s+datachange:\s*([\d.]+))?/i;
	var TABLES_USED_RE   = /^total number of tables used:\s*(\d+)/i;
	var WORKTABLES_RE    = /^total number of worktables:\s*(\d+)/i;
	var OPTIMIZED_RE     = /^Optimized using\s+(.+?)\s*\.?$/i;
	var OPERATOR_COUNT_RE= /^(\d+)\s+operator\(s\)\s+under root/i;
	var ABSTRACT_PLAN_RE = /^Final abstract plan text:/i;

	function parseText(planText) {
		lastParseError = null;
		if (!planText) { lastParseError = 'Plan text is empty'; return null; }

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
		// A "full" sp_showplan carries several sections around the operator tree that the tree parser
		// itself ignores (they have no leading pipe): a Tables summary, optimizer notes, the abstract
		// plan, the ASCII "Lava Operator Tree", and a block of statement totals at the very end.
		// Collected here so they are not silently thrown away - see attachPlanExtras() below for
		// where they end up.
		var planInfo = { tables: [] };
		var curStmtNotes  = null;   // "Optimized using ..." - per statement
		var curStepOpCount = null;  // "N operator(s) under root" - per STEP (see the comment below)

		function flushStep() {
			if (curStatement && curStepLines && curStepLines.length) {
				var root = parseStep(curStepLines);
				if (root) {
					// The statement-totals block sits after the operator tree, inside the same step,
					// so it is in these very lines. Belongs on the root operator: they describe the
					// whole statement, and the root is the operator the whole statement funnels into.
					var totals = parseStatementTotals(curStepLines);
					if (totals) root._planStats = totals;
					// Built fresh per step - never a shared reference, so a later step cannot rewrite an
					// earlier step's numbers after they have already been handed out.
					if (curStmtNotes || curStepOpCount) {
						root._stmtInfo = {
							optimizerNotes:     curStmtNotes ? curStmtNotes.slice() : undefined,
							operatorsUnderRoot: curStepOpCount || undefined
						};
					}
					curStepOpCount = null;
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
				curStmtNotes = null;    // notes belong to the statement about to start, not the last one
				curStepOpCount = null;
				continue;
			}

			// The extra sections, wherever they appear - most sit OUTSIDE any step (before the first
			// statement header, or between the statement header and its STEP), where nothing else
			// would look at them. Matched before the step push below so they are recorded even when
			// they do fall inside a step.
			var trimmed = line.trim();
			var mTbl = trimmed.match(TABLE_ROW_RE);
			if (mTbl) {
				planInfo.tables.push({ name: mTbl[1].trim(), rows: mTbl[2],
				                       useCount: mTbl[3], datachange: mTbl[4] });
			} else if (TABLES_USED_RE.test(trimmed)) {
				planInfo.tablesUsed = trimmed.match(TABLES_USED_RE)[1];
			} else if (WORKTABLES_RE.test(trimmed)) {
				planInfo.workTables = trimmed.match(WORKTABLES_RE)[1];
			} else if (ABSTRACT_PLAN_RE.test(trimmed)) {
				// Deliberately NOT parsed: the abstract plan is a second, equivalent description of the
				// same tree the diagram already draws. Just noted, with a pointer to where to read it.
				planInfo.hasAbstractPlan = true;
			} else if (OPERATOR_COUNT_RE.test(trimmed)) {
				// PER-STEP: a statement with two steps prints this once per step, with different
				// counts. Held in its own variable and reset at each STEP header - an earlier attempt
				// kept it on the statement and handed every step's root a REFERENCE to that one
				// object, so step 2's count silently rewrote what step 1's root reported.
				curStepOpCount = trimmed.match(OPERATOR_COUNT_RE)[1];
			} else if (OPTIMIZED_RE.test(trimmed)) {
				// Per-STATEMENT: printed after the statement header, before its first STEP.
				(curStmtNotes = curStmtNotes || []).push(trimmed.replace(/\.$/, ''));
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

		if (!sawAnyOperator) {
			lastParseError = statements.length
				? 'Found "' + statements[0].label + '" but no recognizable "... Operator" lines under it'
				: 'No "QUERY PLAN FOR ..." / "STEP n" / "... Operator" lines recognized in the plan text';
			return null;
		}

		// Drop statements that ended up with no parsed steps (e.g. trailing narrative-only blocks).
		statements = statements.filter(function (s) { return s.steps.length > 0; });
		if (!statements.length) { lastParseError = 'No statements with parsed steps found'; return null; }

		// Plan-level sections (Tables summary, optimizer notes) describe the whole plan, and ASE has
		// no statement-level box to hang them on the way the SQL Server renderer does - so they go on
		// the first root operator, which is where the user goes looking for "about this plan".
		var hasPlanInfo = planInfo.tables.length || planInfo.tablesUsed || planInfo.workTables
			|| planInfo.hasAbstractPlan;
		if (hasPlanInfo) statements[0].steps[0].root._planInfo = planInfo;

		return { format: 'text', rawText: text, statements: statements, planInfo: planInfo };
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
		+ '.ase-plan-box.ase-plan-big-table { border-color: #c0392b; border-width: 2px; background: #fdf1f0; }'
		+ '.ase-plan-metric.ase-plan-tablesize-warn { color: #c0392b; font-weight: 600; }'
		+ '.ase-plan-box.ase-plan-reformat { border-color: #c0392b; border-width: 2px; background: #fdf1f0; }'
		+ '.ase-plan-box.ase-plan-reformat-info { border-color: #4a90d9; border-width: 2px; background: #eef5fc; }'
		+ '.ase-plan-metric.ase-plan-reformat-info-metric { color: #2a6ebb; font-weight: 600; }'
		+ '.ase-plan-va { position: absolute; top: 2px; right: 4px; font-size: 0.72em; color: #aaa; line-height: 1; }'
		+ '.ase-plan-icon { width: 32px; height: 32px; margin: 0 auto; background-repeat: no-repeat; }'
		// position:relative anchors the DDL-info icon to the row's right edge (below).
		+ '.ase-plan-icon-row { position: relative; display: flex; align-items: center; justify-content: center; gap: 2px; }'
		// DDL/Table-info indicator - identical to the SQL Server one, including the CSS-drawn red X
		// for "not found" (one asset, no second pre-composited image).
		+ '.ase-plan-icon-badge { width: 16px; height: 16px; background-repeat: no-repeat; flex: none; }'
		+ '.ase-plan-ddlinfo-icon { position: absolute; right: 2px; top: 50%; transform: translateY(-50%); background-image: url(/images/ddlinfo.png); background-size: 16px 16px; }'
		+ '.ase-plan-ddlinfo-icon.ase-plan-ddlinfo-missing::after { content: \'\'; position: absolute; right: -3px; bottom: -3px; width: 9px; height: 9px; border-radius: 50%; background: #c0392b; box-shadow: 0 0 0 1.5px #fff; }'
		+ '.ase-plan-ddlinfo-icon.ase-plan-ddlinfo-missing::before { content: \'\\2715\'; position: absolute; right: -3px; bottom: -4px; width: 9px; height: 9px; font-size: 7px; line-height: 9px; color: #fff; text-align: center; z-index: 1; }'
		+ '.ase-plan-icon-row .ase-plan-icon { margin: 0; }'
		+ '.ase-plan-jointype-icon { width: 32px; height: 32px; background-repeat: no-repeat; background-size: 32px 32px; flex: none; }'
		+ '.ase-plan-label { font-weight: 600; white-space: nowrap; }'
		+ '.ase-plan-subtitle { font-size: 0.85em; color: #555; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 200px; margin: 0 auto; }'
		+ '.ase-plan-metric { font-size: 0.85em; color: #666; white-space: nowrap; }'
		+ '.ase-plan-metric.ase-plan-warn-text { color: #a3690a; font-weight: 600; }'
		+ '.ase-plan-metric-pct-warn { color: #c0392b; font-weight: 700; }'
		+ '.ase-plan-detail-pct-warn { color: #c0392b; font-weight: 700; }'
		+ '.ase-plan-metric-filter { color: #2a6f97; font-size: 0.85em; white-space: nowrap; }'
		// position:fixed and attached to <body> (see the shared panel system in dbxShowplanGraph.js)
		// so the panel is never clipped by the diagram's own overflow:auto viewport - JS supplies
		// left/top. z-index clears Bootstrap's modal (1050) and its backdrop, since this also renders
		// inside the Showplan dialog. A panel for an operator with a lot of detail can be taller than
		// the screen, so cap it and let it scroll rather than letting it run off the bottom.
		// font-size in PIXELS, not em: an em here inherits the dialog's font size, which made this
		// panel noticeably larger than the SQL Server one for the same content. Fixed px keeps the two
		// identical regardless of what the surrounding page does.
		+ '.ase-plan-detail { position: fixed; z-index: 2000; background: #fffef5; border: 1px solid #c9b98a; border-radius: 4px; padding: 6px 10px; min-width: 220px; max-width: 560px; text-align: left; box-shadow: 0 2px 10px rgba(0,0,0,0.28); font-size: 11px; line-height: 1.35; }'
		+ '.ase-plan-detail { max-height: 80vh; overflow-y: auto; overscroll-behavior: contain; }'
		+ '.ase-plan-detail table { border-collapse: collapse; }'
		+ '.ase-plan-detail-desc { white-space: normal; font-style: italic; color: #6b5f3d; margin-bottom: 6px; padding-bottom: 6px; border-bottom: 1px solid #e6dcb8; line-height: 1.35; }'
		+ '.ase-plan-detail-grid { display: flex; align-items: flex-start; gap: 0 14px; }'
		+ '.ase-plan-detail-right { border-left: 1px solid #e6dcb8; padding-left: 14px; }'
		+ '.ase-plan-detail-idx-hdr { font-size: 10px; color: #6b5f3d; margin-top: 4px; }'
		// Full-width wrapping block for values too long to sit in a nowrap "detail td" without forcing
		// the whole panel wider than the screen - index key lists especially. Same rule and same job
		// as the SQL Server panel's.
		+ '.ase-plan-detail-wrap { white-space: normal; word-break: break-word; }'
		+ '.ase-plan-detail-hint { margin-top: 5px; padding-top: 4px; border-top: 1px solid #e6dcb8; color: #9a8f6d; font-size: 10px; white-space: normal; }'
		// pointer-events:stroke makes the transparent stroke itself hoverable (the arrows are only
		// 1.5px wide, so the visible path is nearly impossible to hit deliberately).
		+ '.ase-plan-connector-hit { pointer-events: stroke; cursor: help; }'
		+ '.ase-plan-idx-tbl td { white-space: normal; }'
		+ '.ase-plan-detail td { padding: 1px 6px 1px 0; vertical-align: top; white-space: nowrap; }'
		+ '.ase-plan-detail td.ase-plan-detail-key { color: #777; }'
		+ '.ase-plan-detail .ase-plan-raw-line { font-family: monospace; white-space: pre-wrap; color: #555; }'
		+ '.ase-plan-detail.ase-plan-tooltip { pointer-events: none; cursor: default; }'
		+ '.ase-plan-fallback { color: #888; font-size: 0.85em; font-style: italic; padding: 6px 0; }'
		// Triggered by window.aseShowplanJumpToNode() (dbxShowplan.js) when a Plan Analysis finding's
		// "[Node N]" tag is clicked - draws attention to the box scrollIntoView() just centered on
		// without permanently changing its styling (the class is removed again once the animation ends).
		+ '@keyframes ase-plan-flash { 0%, 100% { box-shadow: 0 1px 2px rgba(0,0,0,0.08); } 20%, 60% { box-shadow: 0 0 0 5px rgba(74,144,217,0.85); } 40%, 80% { box-shadow: 0 1px 2px rgba(0,0,0,0.08); } }'
		// 5 discrete pulses (not a time-based cutoff) - aseShowplanJumpToNode (dbxShowplan.js) listens
		// for this animation's 'animationend' event (which only fires once, after the last iteration)
		// to remove the class, so the two stay in sync automatically if this iteration count or
		// per-pulse duration ever changes.
		+ '.ase-plan-box.ase-plan-flash { animation: ase-plan-flash 0.8s ease-in-out 5; }'
		// --- Properties pane ---
		// The generic half comes from dbxShowplanGraph.js, shared with the SQL Server pane so the two
		// look identical and a styling fix lands once.
		+ DbxShowplanGraph.propsCss('ase-plan')
		// The clicked operator stays visibly marked while the pane describes it - otherwise there is
		// no way to tell which box the pane is showing once the pointer has moved away.
		+ '.ase-plan-box.ase-plan-selected { border-color: #2a6ebb; border-width: 2px; box-shadow: 0 0 0 3px rgba(42,110,187,0.18); }'
		// Captured plan source (and unparsed detail lines): monospace and pre-wrap, because ASE's text
		// output is column-aligned - collapsing its whitespace would destroy the alignment that makes
		// it readable in the first place.
		+ '.ase-plan-prop-raw { font-family: monospace; white-space: pre-wrap; font-size: 10px; color: #555; line-height: 1.35; }';

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
		Catchall:           [ -96,  -256],
		Top:                [-160,  -192],
		RdiLookup:          [   0,  -128],
		GatherStreams:      [ -32,   -64],
		PopulateQuery:      [-192,  -224]
	};
	// Ordered (most-specific-first) operator-name/scanType patterns -> icon key.
	var ICON_RULES = [
		{ test: /cluster.*index/i,        icon: 'ClusteredIndexScan' },
		{ test: /index/i,                 icon: 'IndexScan' },
		{ test: /table\s*scan|^scan$/i,   icon: 'TableScan' },
		{ test: /rid\s*join/i,            icon: 'RdiLookup' },
		{ test: /n-?ary|nest.*loop|nljoin/i, icon: 'NestedLoops' },
		{ test: /hash/i,                  icon: 'HashMatch' },
		{ test: /merge/i,                 icon: 'MergeJoin' },
		{ test: /sort/i,                  icon: 'Sort' },
		{ test: /agg(?:regate)?/i,        icon: 'StreamAggregate' },
		{ test: /union/i,                 icon: 'Concatenation' },
		{ test: /sqfilter|sqlfilter/i,    icon: 'PopulateQuery' },
		{ test: /restrict|filter/i,       icon: 'Filter' },
		{ test: /insert/i,                icon: 'Insert' },
		{ test: /update/i,                icon: 'Update' },
		{ test: /delete/i,                icon: 'Delete' },
		{ test: /store|spool/i,           icon: 'TableSpool' },
		{ test: /sequenc/i,               icon: 'Sequence' },
		{ test: /remote/i,                icon: 'RemoteScan' },
		{ test: /compute/i,               icon: 'ComputeScalar' },
		{ test: /^emit$/i,                icon: 'Result' },
		{ test: /^limit$/i,               icon: 'Top' },
		{ test: /exchange/i,              icon: 'GatherStreams' },
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
		// A bare "SCAN Operator" (the generic LAVA-tree op name, as opposed to a "Table Scan."/"Using
		// Clustered Index." detail line) with neither of the above props set carries no scanType of
		// its own - but iconKeyFor()'s ICON_RULES already treats this exact op name as a Table Scan
		// for icon purposes (same regex reused here), so treat it the same way here too. Otherwise it
		// shows a Table Scan icon while being invisible to displayLabelFor()/the size-warning check,
		// which both key off this function.
		if (/table\s*scan|^scan$/i.test(node.op || '')) return 'TableScan';
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
		var label = node.label;
		if (scanType) {
			if (/cluster/i.test(scanType))     label = 'Clustered Index Scan';
			else if (/index/i.test(scanType))  label = 'Index Scan';
			else if (/table/i.test(scanType))  label = 'Table Scan';
			if (label !== node.label) {
				var positioning = node.props && node.props.positioning;
				if (positioning && /bykey/i.test(positioning.replace(/\s+/g, ''))) label += ' (By Key)';
			}
		}
		// A STORE operator building a worktable+index because no useful index existed (see
		// collectTreeFindings()'s Reformatting finding) - called out in the label itself, not just the
		// Plan Analysis section, so it's visible right on the box like the "Large table/index" mark.
		if (node.props && node.props.reformatWorktable) label += ' (Reformatting)';
		return label;
	}

	// Plain-English "what does this operator actually do" text for the detail panel, sourced from
	// SAP's own showplan/query-plan-operator reference (help.sap.com, Performance and Tuning Series:
	// Query Processing and Abstract Plans - "Using showplan" chapter and its per-operator sub-pages),
	// condensed to 1-3 sentences each. Same ordered, most-specific-first regex matching approach as
	// ICON_RULES above, but kept as its own separate list rather than reused: a few operators
	// ICON_RULES deliberately groups under one icon for visual simplicity (HASH JOIN and HASH UNION
	// share one, etc.) are semantically quite different operators and get distinct, accurate entries
	// here instead.
	var OPERATOR_DESCRIPTIONS = [
		{ test: /clust.*index/i,        text: 'Reads rows via a clustered index, so the table’s data pages are read in index order directly - no separate row-ID lookup into the base table is needed.' },
		{ test: /index/i,               text: 'Reads rows via a non-clustered index. If the index does not carry every needed column, each matching entry requires an extra lookup into the base table by row ID.' },
		{ test: /table\s*scan|^scan$/i, text: 'Reads every row of the table in physical/allocation order - no index is used. A leaf operator: it never has children.' },
		// "NLJoin"/"NaryNLJoin" are XML plans' shorthand for "Nested Loop Join" - real captured plans
		// use these instead of spelling "Nested"/"Loop" out the way text plans do ("N-ARY NESTED LOOP
		// JOIN Operator"), so both spellings need to be recognized here.
		{ test: /n-?ary.*(?:nest.*loop|nljoin)|(?:nest.*loop|nljoin).*n-?ary/i,
			text: 'A NESTED LOOP JOIN variant the optimizer never picks directly - built during code generation by folding a series of left-deep NESTED LOOP JOINs (each an inner join whose right child is a scan) into one operator, avoiding the wasted I/O of repeatedly re-draining an earlier scan that a chain of separate nested loops would cause.' },
		{ test: /nest.*loop|nljoin/i,
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
		{ test: /scalar.*agg/i,
			text: 'Keeps a running aggregate (count, sum, min, max, average, etc.) across its entire input stream and returns a single summary row once the input is exhausted - used for an unwrapped/ungrouped aggregate like a plain select count(*).' },
		{ test: /agg(?:regate)?/i,
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

	/**
	 * Est/Act comparison in the SQL Server renderer's form (fmtEstActDiff there - kept identical so
	 * the same mismatch reads the same way in both dialogs): a plain percentage while the estimate is
	 * roughly right, a MULTIPLIER once it is off by more than the 10x/0.1x threshold isEstActWarn()
	 * already flags ("153,300%" does not read as "big" the way "1533x" does), "huge-diff" past ~1000x
	 * where the exact multiplier stops meaning anything, and "zero-rows" when the operator produced
	 * nothing at all - which is a different fact from "the numbers differ a lot", and the more useful
	 * one. Infinity (Est 0, Act > 0) has no meaningful ratio, so it reports as huge-diff too.
	 */
	function fmtEstActDiff(m) {
		var pct = estActPercent(m);
		if (pct === undefined) return undefined;
		if (!isEstActWarn(m)) return fmtPercent(pct);
		if (m.actRows === 0) return 'zero-rows';
		if (!isFinite(pct)) return 'huge-diff';
		var ratio = pct / 100;
		var magnitude = ratio >= 1 ? ratio : 1 / ratio;
		if (magnitude >= 1000) return 'huge-diff';
		return Math.round(magnitude) + 'x';
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

	/**
	 * "100%" must mean NOTHING got through. Rounding alone breaks that promise: 1,000,000 rows in and
	 * 5,000 out is 99.5%, which rounds to a "100% of input rows filtered" that flatly contradicts the
	 * Act 5,000 printed directly above it. So one decimal is kept in the top band whenever any row
	 * actually survived, and a bare 100 is reserved for the case that genuinely produced no rows.
	 */
	function fmtReductionPct(reduction, ownAct) {
		if (reduction >= 99.5 && ownAct > 0) return String(Math.floor(reduction * 10) / 10);
		return String(Math.round(reduction));
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

	// ─────────────────────────────────────────────────────────────────────────
	// Properties pane — the "everything about this operator" view, rendered into opts.propsTarget.
	//
	// Deliberately a superset of the hover panel above: the panel is a scannable summary, this is
	// where nothing is hidden. Built from the shared building blocks (dbxShowplanGraph.js) so it is
	// visually identical to the SQL Server pane, but the SECTIONS are ASE's own - the two formats
	// simply describe different things.
	//
	// The last section shows this operator's own source, which is format-dependent and is the reason
	// this pane needed the parser changes above:
	//   XML plans  (show_cached_plan_in_xml) -> a collapsible XML tree, exactly like SQL Server's.
	//   text plans (classic sp_showplan)     -> the operator's own block of the captured text, since
	//                                           there is no XML to show. Same idea, different medium.
	// ─────────────────────────────────────────────────────────────────────────

	function propRow($into, key, val)  { return DbxShowplanGraph.propRow($into, 'ase-plan', key, val); }
	function propSection($into, title) { return DbxShowplanGraph.propSection($into, 'ase-plan', title); }

	/**
	 * Is this tag one of an operator's own PROPERTIES (as opposed to a nested child operator)?
	 * XML_PROPERTY_TAGS is the shared base; the extras are the ones buildXmlNode() also consumes as
	 * properties rather than recursing into - keep the two in step, or the Properties pane will show
	 * a property the parser folded away as if it were a separate operator (or hide one it kept).
	 */
	function isXmlPropertyTag(tag) {
		return !!XML_PROPERTY_TAGS[tag]
			|| tag === 'indName' || tag === 'wtObjName' || tag === 'WorkTable';
	}

	/**
	 * A section that only appears if it ends up with something in it. ASE's node model is sparse -
	 * an EMIT has no metrics at all - and a run of empty "Estimated"/"Actual" headers is pure noise
	 * that pushes the sections that DO have content off the top of the pane.
	 */
	function propSectionIfAny($into, title, fill) {
		var $tmp = $('<div></div>');
		fill($tmp);
		if (!$tmp.children().length) return;
		propSection($into, title).append($tmp.children());
	}

	// ASE prints only the bare strategy name ("With MRU Buffer Replacement Strategy for data pages."),
	// which says nothing about what the server actually DOES with the cache - and the two are easy to
	// read backwards, since the intuitive reading of "MRU" is the opposite of its effect here:
	//
	//   MRU - fetch and discard. Pages are put at the MRU end and reused immediately, so a large scan
	//         does not flush everything else out of the buffer pool. ASE picks this for scans it does
	//         not expect to revisit.
	//   LRU - keep in cache. Pages go through the normal LRU chain and stay until they age out, which
	//         is what you want for pages that will be read again.
	//
	// Applied to the parsed value in one place so the hover panel and the Properties pane cannot drift
	// apart, and keyed off the suffix so it covers indexBufReplStrategy as well as dataBufReplStrategy.
	var BUF_REPL_STRATEGY_NOTE = {
		MRU: 'Fetch and Discard',
		LRU: 'Keep in Cache'
	};

	function annotatePropValue(key, value) {
		if (typeof value !== 'string' || !/BufReplStrategy$/.test(key)) return value;
		var note = BUF_REPL_STRATEGY_NOTE[value.trim().toUpperCase()];
		return note ? value + ' (' + note + ')' : value;
	}

	function renderPropertiesInto(container, node) {
		var $c = $(container);
		$c.empty();
		if (!node) {
			$c.append($('<div class="ase-plan-prop-empty"></div>')
				.text('Click an operator in the plan to see all of its properties here.'));
			return;
		}

		var p = node.props || {}, m = node.metrics || {};

		$c.append($('<div class="ase-plan-prop-title"></div>').text(displayLabelFor(node)));
		var sub = subtitleFor(node);
		if (sub) $c.append($('<div class="ase-plan-prop-subtitle"></div>').text(sub));

		var desc = operatorDescriptionFor(node);
		if (desc) $c.append($('<div class="ase-plan-prop-desc"></div>').text(desc));

		var joinNote = typeof joinTypeNote === 'function' ? joinTypeNote(node) : undefined;
		if (joinNote) $c.append($('<div class="ase-plan-prop-desc"></div>').text(joinNote));

		// Misc: every parsed property, verbatim. ASE's text format is free-form and varies by server
		// version, so an allow-list would silently drop whatever this particular ASE decided to print.
		var $misc = propSection($c, 'Misc');
		propRow($misc, 'Operator', node.op);
		propRow($misc, 'VA', p.va);
		Object.keys(p).forEach(function (k) {
			if (k === 'va') return; // already shown, under its more readable name
			propRow($misc, k, annotatePropValue(k, p[k]));
		});
		if (node.extra) propRow($misc, 'Extra', node.extra);

		propSectionIfAny($c, 'Estimated', function ($est) {
			propRow($est, 'Est Rows',     fmtNum(m.estRows));
			propRow($est, 'Est LIO',      fmtNum(m.estLio));
			propRow($est, 'Est PIO',      fmtNum(m.estPio));
			propRow($est, 'Est Row Size', fmtNum(m.estRowSz));
		});

		propSectionIfAny($c, 'Actual', function ($act) {
			propRow($act, 'Act Rows',     fmtNum(m.actRows));
			propRow($act, 'Act % of Est', fmtEstActDiff(m));
			if (node.children && node.children.length === 1) {
				propRow($act, 'Input Rows (Act)',    fmtNum(node.children[0].metrics && node.children[0].metrics.actRows));
				propRow($act, 'Input Rows Filtered', fmtPercent(inputRowReductionPercent(node)));
			}
			propRow($act, 'Act LIO', fmtNum(m.actLio));
			propRow($act, 'Act PIO', fmtNum(m.actPio));
		});

		// Statement totals and the plan-level Tables summary, both from a "full" sp_showplan and both
		// parked on the root operator (see parseText) - so they only ever appear on that one box.
		if (node._planStats) {
			var $tot = propSection($c, 'Statement totals');
			Object.keys(node._planStats).forEach(function (k) { propRow($tot, k, node._planStats[k]); });
		}
		if (node._planInfo || node._stmtInfo) {
			var pi = node._planInfo || {};
			var si = node._stmtInfo || {};
			var $pi = propSection($c, 'Plan information');
			propRow($pi, 'Operators',        si.operatorsUnderRoot);
			(si.optimizerNotes || []).forEach(function (n, i) {
				propRow($pi, i === 0 ? 'Optimizer' : '', n);
			});
			propRow($pi, 'Tables used',      pi.tablesUsed);
			propRow($pi, 'Worktables',       pi.workTables);
			(pi.tables || []).forEach(function (t) {
				propRow($pi, 'TABLE ' + t.name,
					'rows: ' + t.rows + ', use count: ' + t.useCount
					+ (t.datachange !== undefined ? ', datachange: ' + t.datachange : ''));
			});
			if (pi.hasAbstractPlan) {
				// Not parsed on purpose - it describes the same tree the diagram already draws. Point
				// at where the reader can see it rather than pretending it isn't in the capture.
				$pi.append($('<div class="ase-plan-prop-empty"></div>')
					.text('This plan also carries a "Final abstract plan text" - see the Raw Plan Text section.'));
			}
		}

		// Detail lines no parser rule recognized. Worth surfacing rather than hiding: on an unfamiliar
		// ASE version this is exactly where the information the parser missed will be.
		if (node.raw && node.raw.length) {
			var $rawSec = propSection($c, 'Unparsed detail lines');
			$rawSec.append($('<div class="ase-plan-prop-raw"></div>').text(node.raw.join('\n')));
		}

		// This operator's own source, in whichever form the plan was captured.
		if (node._xmlEl) {
			var $xml = propSection($c, 'Plan XML for this operator');
			DbxShowplanGraph.propXmlTree($xml, 'ase-plan', node._xmlEl, 0, {
				// An ASE plan's child operators are arbitrary tags; the PROPERTY tags are the known
				// set, so a direct child that isn't one starts a different operator and is left to its
				// own pane. Only at depth 0 though: inside <est>/<act> the child names (rowCnt, lio,
				// pio, ...) are a different vocabulary entirely and must not be tested against this set.
				stopAt: function (k, parentDepth) {
					return parentDepth === 0 && !isXmlPropertyTag(k.tagName);
				}
			});
		} else if (node._sourceLines && node._sourceLines.length) {
			var $src = propSection($c, 'Plan text for this operator');
			$src.append($('<div class="ase-plan-prop-raw"></div>').text(stripTreePrefix(node._sourceLines)));
		}

		renderDdlInfoSection($c, node);
	}

	/**
	 * The captured lines for ONE operator, with the plan-tree drawing prefix removed - everything up
	 * to and including the last '|'. Those pipes describe where the operator sits in the whole tree,
	 * which the diagram already shows and which just indents this block pointlessly when it is being
	 * read on its own. Whatever follows the last pipe is kept verbatim, so ASE's own alignment of the
	 * detail lines under their operator (and of the key list under "Keys are:") survives.
	 */
	function stripTreePrefix(lines) {
		return (lines || []).map(function (l) {
			var i = l.lastIndexOf('|');
			return i === -1 ? l : l.slice(i + 1);
		}).join('\n');
	}

	/**
	 * DDL Storage info, last and always present - "nothing here" is itself an answer, and a section
	 * that simply vanishes leaves the reader wondering whether it failed or was never asked for.
	 */
	function renderDdlInfoSection($c, node) {
		var $sec = propSection($c, 'DDL Info');
		var name = node.props && node.props.objName;
		if (!name) {
			$sec.append($('<div class="ase-plan-prop-empty"></div>')
				.text('This operator does not reference a table.'));
			return;
		}
		if (node._tableInfoPending) {
			$sec.append($('<div class="ase-plan-prop-empty"></div>').text('Loading ' + name + ' ...'));
			return;
		}
		var info = node._tableInfo;
		if (!info) {
			$sec.append($('<div class="ase-plan-prop-empty"></div>')
				.text('No DDL/table info was looked up (no server/database context for this plan).'));
			return;
		}
		if (info.found === false) {
			$sec.append($('<div class="ase-plan-prop-empty"></div>')
				.text(name + ' was not found in DbxTune\'s DDL Storage.'));
			return;
		}
		propRow($sec, 'Table',      info.tableName || name);
		propRow($sec, 'Rows',       fmtNum(info.rowTotal));
		propRow($sec, 'Size (MB)',  fmtNum(info.sizeMb !== undefined ? info.sizeMb : info.totalMb));
		propRow($sec, 'Data (MB)',  fmtNum(info.dataMb));
		propRow($sec, 'Index (MB)', fmtNum(info.indexMb));
		propRow($sec, 'Indexes',    info.indexes ? info.indexes.length : undefined);
		propRow($sec, 'Sampled',    info.sampleTime);
	}

	/**
	 * Hover text for the arrow between two operators - what actually FLOWS along it. The row counts
	 * belong to the CHILD (the arrow carries the child's output up into its parent), which is also why
	 * a "few rows" arrow can still be the expensive one: ASE's LIO/PIO say how much work producing
	 * those rows cost. Ported from the SQL Server renderer, using ASE's own metric names.
	 */
	function connectorTooltip(childNode, parentNode) {
		if (!childNode) return undefined;
		var m = childNode.metrics || {};
		var lines = [];
		var p = childNode.props || {};
		var from = displayLabelFor(childNode) + (p.objName ? ' (' + p.objName + ')' : '');
		lines.push(from + '  →  ' + (parentNode ? displayLabelFor(parentNode) : ''));
		lines.push('');

		if (m.actRows !== undefined) lines.push('Actual rows:      ' + fmtNum(m.actRows));
		if (m.estRows !== undefined) lines.push('Estimated rows:   ' + fmtNum(m.estRows));
		var pct = fmtEstActDiff(m);
		if (pct !== undefined)       lines.push('Actual % of est:  ' + pct);
		if (m.estLio !== undefined)  lines.push('Est LIO:          ' + fmtNum(m.estLio));
		if (m.actLio !== undefined)  lines.push('Act LIO:          ' + fmtNum(m.actLio));
		if (m.estPio !== undefined)  lines.push('Est PIO:          ' + fmtNum(m.estPio));
		if (m.actPio !== undefined)  lines.push('Act PIO:          ' + fmtNum(m.actPio));
		return lines.length > 2 ? lines.join('\n') : undefined;
	}

	function buildDetailPanel(node) {
		var $panel = $('<div class="ase-plan-detail"></div>');

		var description = operatorDescriptionFor(node);
		if (description) {
			$panel.append($('<div class="ase-plan-detail-desc"></div>').text(description));
		}

		function makeRowFn($tbl) {
			return function row(key, val, valClass) {
				if (val === undefined || val === null || val === '') return;
				var $val = $('<td></td>').text(val);
				if (valClass) $val.addClass(valClass);
				$tbl.append($('<tr></tr>')
					.append($('<td class="ase-plan-detail-key"></td>').text(key))
					.append($val));
			};
		}

		var $tbl = $('<table></table>');
		var row = makeRowFn($tbl);

		row('Operator', displayLabelFor(node));
		// Long values are collected here and rendered as full-width wrapping blocks AFTER the table
		// instead of as rows in it: ".ase-plan-detail td" is nowrap (which is what keeps the ordinary
		// two-column layout tidy), so a long value - an index key list above all - would otherwise
		// stretch the whole panel far past the screen edge. Same treatment the SQL Server panel gives
		// its Predicate/Output List.
		var longVals = [];
		var LONG = 60;
		if (node.props) {
			for (var k in node.props) {
				var v = node.props[k];
				if (typeof v === 'string' && v.length > LONG) { longVals.push([k, v]); continue; }
				if (k === 'va') row('VA', v);
				else row(k, annotatePropValue(k, v));
			}
		}
		if (node.metrics) {
			row('Est Rows', fmtNum(node.metrics.estRows));
			row('Act Rows', fmtNum(node.metrics.actRows));
			row('Act % of Est', fmtEstActDiff(node.metrics), isEstActWarn(node.metrics) ? 'ase-plan-detail-pct-warn' : undefined);
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

		// Statement totals on the root operator - the headline numbers ("what did this statement cost
		// overall") are worth having in the hover panel, not only in the Properties pane.
		if (node._planStats) {
			Object.keys(node._planStats).forEach(function (k) { row(k, node._planStats[k]); });
		}
		if (node._planInfo) {
			row('Tables used', node._planInfo.tablesUsed);
			row('Worktables',  node._planInfo.workTables);
		}

		// Live table size/rowcount, from an async batched lookup fired at the end of render() - see
		// loadTableInfoAsync() there. Only meaningful when the caller supplied srv/dbname (opts.srv,
		// opts.dbname to render()); otherwise node._tableInfo/_tableInfoPending are simply never set
		// and this whole section is silently skipped (e.g. the standalone paste-a-plan page). Laid
		// out as a second column to the right of the showplan-native table above, separated by a
		// vertical divider, rather than stacked below it - keeps the panel from growing tall as more
		// DDL Storage fields/indexes get added over time (a wide plan already needs horizontal
		// scroll/pan, so trading some tooltip width for less height is the better direction here).
		var hasTableInfo = node.props && node.props.objName && (node._tableInfo || node._tableInfoPending);
		if (!hasTableInfo) {
			$panel.append($tbl);
		} else {
			var $right = $('<div class="ase-plan-detail-right"></div>');
			var $tblInfo = $('<table></table>');
			var rowInfo = makeRowFn($tblInfo);
			function fmtMb(v) {
				var s = fmtNum(v);
				return s === undefined ? undefined : s + ' MB';
			}
			if (node._tableInfo) {
				if (node._tableInfo.found) {
					var ti = node._tableInfo;
					// DDL Storage is a periodic snapshot, not a live query (see loadTableInfoAsync()'s
					// comment) - showing when it was last sampled makes that staleness visible instead
					// of implying these numbers are as current as the showplan itself.
					var sampleTime = ti.sampleTime ? String(ti.sampleTime).replace(/\.\d+$/, '') : undefined;
					rowInfo('DDL Sample time', sampleTime);
					rowInfo('Table Rows', fmtNum(ti.rowTotal));
					rowInfo('Total Table Size', fmtMb(ti.sizeMb));
					var dataMbStr = fmtMb(ti.dataMb);
					var dataPagesStr = fmtNum(ti.dataPages);
					rowInfo('Data Size', dataMbStr === undefined ? undefined : dataMbStr + (dataPagesStr === undefined ? '' : ' (' + dataPagesStr + ' Pages)'));
					// ti.indexMb is ASE's own already-computed total (the index_size column from
					// sp_spaceused's table-summary line) - not something derived here from summing the
					// per-index rows below, so it's unaffected by the synthetic "DATA"/LOB pseudo-index
					// entries AseAbstract.getTableInfoFields() has to filter out of that breakdown.
					rowInfo('Index MB', fmtMb(ti.indexMb));
					// -1 is "no LOB storage on this table" (see AseAbstract.AseTableInfo.getLobMb()) -
					// still shown, not skipped, so its absence reads as a confirmed fact rather than a
					// gap; "-no-lob-" matches the wording the Table Information section already uses.
					rowInfo('LOB', (ti.lobMb !== undefined && ti.lobMb >= 0) ? fmtMb(ti.lobMb) : '-no-lob-');
					rowInfo('Locking Schema', ti.lockScheme);
				} else {
					rowInfo('Table Info', 'not found in DDL Storage');
				}
			} else if (node._tableInfoPending) {
				rowInfo('Table Info', '⏳ Loading…');
			}
			$right.append($tblInfo);

			// Per-index breakdown (name, columns, size) - same data the table row/size counts above
			// came from (AseAbstract.getTableInfoFields()'s "indexes" array), just not collapsed to a
			// count. The index this exact operator is using (props.indexName, set for Index/Clustered
			// Index Scan) gets a small arrow prefix so it stands out from the rest of the table's
			// indexes, which are shown for context/comparison.
			var indexes = (node._tableInfo && node._tableInfo.found) ? (node._tableInfo.indexes || []) : [];
			if (indexes.length) {
				$right.append($('<div class="ase-plan-detail-idx-hdr"></div>').text('Indexes (' + indexes.length + '):'));
				var usedIndexName = node.props.indexName;
				var $idxTbl = $('<table class="ase-plan-idx-tbl"></table>');
				var rowIdx = makeRowFn($idxTbl);
				indexes.forEach(function (idx) {
					var cols = (idx.keys && idx.keys.length) ? idx.keys.join(', ') : '?';
					var isUsed = usedIndexName && idx.indexName && usedIndexName.toLowerCase() === idx.indexName.toLowerCase();
					rowIdx((isUsed ? '▶ ' : '') + idx.indexName, fmtNum(idx.sizeMb) + ' MB (' + cols + ')');
				});
				$right.append($idxTbl);
			}

			$panel.append($('<div class="ase-plan-detail-grid"></div>')
				.append($('<div class="ase-plan-detail-left"></div>').append($tbl))
				.append($right));
		}

		// The long values pulled out of the table above - full width, wrapping, below both columns so
		// neither the plan info nor the DDL column gets pushed off-screen by them.
		longVals.forEach(function (kv) {
			$panel.append($('<div class="ase-plan-detail-idx-hdr"></div>').text(kv[0] + ':'));
			$panel.append($('<div class="ase-plan-detail-wrap"></div>').text(kv[1]));
		});

		// Without this the pin-on-click behaviour (and the fact that clicking also drives the
		// Properties pane) is invisible - nothing about a hover panel suggests it can be pinned.
		$panel.append($('<div class="ase-plan-detail-hint"></div>')
			.text('Click the box to pin this, and to show all properties in the Properties pane.'));

		if (node.raw && node.raw.length) {
			var $rawWrap = $('<div style="margin-top:4px;"></div>');
			node.raw.forEach(function (l) {
				$rawWrap.append($('<div class="ase-plan-raw-line"></div>').text(l));
			});
			$panel.append($rawWrap);
		}
		return $panel;
	}

	// Detail panels are fixed-position overlays attached to <body>, managed by the shared panel
	// system (dbxShowplanGraph.js) that the SQL Server renderer already used. Previously they were
	// appended INSIDE the box and merely flipped upward when clipped, which could not help when the
	// diagram scrolls inside its own overflow:auto viewport - an absolutely-positioned descendant is
	// clipped to that scrolling ancestor, so a panel on a box near the edge was cut off with no way
	// to see the rest of it. The shared system clamps the panel into the viewport instead, and
	// repositions/dismisses it on scroll and resize.
	var _panels = DbxShowplanGraph.createPanelSystem({
		prefix:     'ase-plan',
		buildPanel: function (node) { return buildDetailPanel(node); }
	});

	// Set by render() so a box click can push the selected node into the Properties pane without
	// every box needing a reference to it.
	var _propsTarget = null;
	var _selectedBox = null;

	function selectNode($box, node) {
		if (_selectedBox) _selectedBox.removeClass('ase-plan-selected');
		_selectedBox = $box;
		$box.addClass('ase-plan-selected');
		if (_propsTarget) renderPropertiesInto(_propsTarget, node);
	}

	function renderNode(node) {
		var $li  = $('<li></li>');
		var warn = isEstActWarn(node.metrics || {});
		var $box = $('<div class="ase-plan-box"></div>');
		if (warn) $box.addClass('ase-plan-warn');
		// Kept on the node itself (rather than a separate id -> box map) so the async table-info
		// lookup fired from render() can reach back into the live DOM for this exact node once the
		// batched fetch resolves - see the end of render() and loadTableInfoAsync() below.
		node._$box = $box;

		var va = node.props && node.props.va;
		if (va !== undefined && va !== null && va !== '') {
			// Looked up by window.aseShowplanJumpToNode() (dbxShowplan.js) when a Plan Analysis
			// finding's "[Node N]" tag is clicked - see collectRawTextFindings()/collectTreeFindings()
			// above, which put the same VA into every finding's nodeId.
			$box.attr('data-va', va);
			$box.append($('<div class="ase-plan-va" title="VA (vertex number)"></div>').text(va));
		}

		var $iconRow = $('<div class="ase-plan-icon-row"></div>');
		// Stashed so the async DDL-Storage lookup can drop its found/missing icon into this row once
		// it resolves (node._$box is the whole box, not the row) - same as the SQL Server renderer.
		node._$iconRow = $iconRow;
		// ...and the reverse link, so drawConnectorLines() - which walks the rendered DOM, not the
		// model - can get back to the nodes an arrow runs between to build its hover text.
		$box[0].__asePlanNode = node;
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
			var pct = fmtEstActDiff(node.metrics || {});
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
			$filter.text('↓ ' + fmtReductionPct(reduction, (node.metrics || {}).actRows) + '% of input rows filtered');
			$box.append($filter);
		}

		// Hover shows the same info as a transient tooltip; click pins it open (and click again to
		// close). The two never stack - hovering while a pinned panel is already open is a no-op.
		// The panel now lives on <body> rather than inside the box, so the old "did the click land
		// inside the panel?" guard is no longer needed: a click inside the panel doesn't bubble to
		// the box at all, which is what used to close the panel out from under a text selection.
		$box.on('mouseenter', function () {
			// Never replace a pinned panel with a transient one - the pinned panel is the user's
			// deliberate choice and its text is selectable.
			if (_panels.panelFor($box[0])) return;
			_panels.open(node, $box[0], false);
		});
		$box.on('mouseleave', function () {
			if (!_panels.isPinned($box[0])) _panels.close($box[0]);
		});
		$box.on('click', function (e) {
			e.stopPropagation();
			selectNode($box, node);
			// Toggle: a second click on a box whose panel is already pinned closes it.
			if (_panels.isPinned($box[0])) { _panels.close($box[0]); return; }
			_panels.open(node, $box[0], true);
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
		// Idempotent - callable more than once for the same treeEl (render() re-calls this after the
		// async table-info lookup grows a box, since the previously-drawn lines were measured against
		// the pre-growth positions). Without this removal, a second call would leave the stale overlay
		// underneath instead of replacing it, doubling up every arrow.
		var previousSvg = treeEl.querySelector(':scope > svg.ase-plan-connector-svg');
		if (previousSvg) previousSvg.remove();

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

			// A second, invisible, much thicker path carrying the tooltip: the visible arrow is 1.5px
			// wide, which is far too thin to hover deliberately. Same trick the SQL Server renderer
			// uses - stroke:transparent with pointer-events:stroke, so it is grabbable but invisible.
			var tip = connectorTooltip(box.__asePlanNode, parentBox.__asePlanNode);
			if (tip) {
				var hit = document.createElementNS(svgNS, 'path');
				hit.setAttribute('d', d);
				hit.setAttribute('fill', 'none');
				hit.setAttribute('stroke', 'transparent');
				hit.setAttribute('stroke-width', '12');
				hit.setAttribute('class', 'ase-plan-connector-hit');
				var title = document.createElementNS(svgNS, 'title');
				title.textContent = tip;
				hit.appendChild(title);
				svg.appendChild(hit);
			}
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

	function tuckLeavesNearParent(treeEl) { return DbxShowplanGraph.tuckLeavesNearParent(treeEl, 'ase-plan'); }

	function tuckLeavesNearParentVertical(treeEl) { return DbxShowplanGraph.tuckLeavesNearParentVertical(treeEl, 'ase-plan'); }

	// Layout/tree plumbing with no vendor knowledge - shared with the other renderer so a fix
	// lands once. See dbxShowplanGraph.js (loaded before this file) for the implementations and
	// for why drawConnectorLines() is deliberately NOT shared.
	function walkPlanNodes(root, fn) { return DbxShowplanGraph.walkPlanNodes(root, fn); }

	// Fired once per render() call, after layout/connector-drawing is done, so a large plan's
	// initial draw is never blocked waiting on a network round trip. Looks up live table
	// size/rowcount for every distinct table referenced anywhere in the plan in a single batched
	// request (not one per operator box), then annotates each matching node (node._tableInfo,
	// consumed by buildDetailPanel()) and, for a Table Scan whose table exceeds tableSizeWarnMb,
	// marks its box - refreshing any tooltip/pinned panel that's already open so the user doesn't
	// have to close/reopen it to see the numbers that just arrived. A marked box grows by one line,
	// which invalidates the connector-line/compact-tuck positions already measured and drawn at the
	// end of render() - onBoxesChanged (render()'s own redraw step) is called once, only if at least
	// one box actually grew, so callers get a chance to redo that measurement-dependent work.
	// What this scan operator reads in full, if that's a bounded, size-checkable structure - or null
	// if it isn't (e.g. a "By Key" seek only touches the rows it needs, never the whole structure, so
	// warning on it would just be noise - same reasoning a Table Scan doesn't need, since it never has
	// a "By Key" variant to begin with). Table Scan and Clustered Index Scan both read every data page
	// of the table (in ASE the clustered index *is* the data order - there's no separate storage for
	// it), so both are compared against info.dataMb. A plain (non-clustered) Index Scan reads the
	// whole leaf level of one specific index instead - looked up by name (props.indexName, the same
	// field buildDetailPanel() uses to mark the "in use" row in its own index breakdown) against
	// info.indexes[].sizeMb.
	function scannedSizeInfo(node, info) {
		if (!info || !info.found) return null;
		var positioning = node.props && node.props.positioning;
		if (positioning && /bykey/i.test(positioning.replace(/\s+/g, ''))) return null;

		var scanType = effectiveScanType(node);
		if (scanType === 'TableScan' || scanType === 'ClusteredIndexScan')
			return { sizeMb: info.dataMb, what: 'table' };

		if (scanType === 'IndexScan') {
			var name = node.props && node.props.indexName;
			if (!name || !info.indexes) return null;
			for (var i = 0; i < info.indexes.length; i++) {
				if (info.indexes[i].indexName && info.indexes[i].indexName.toLowerCase() === name.toLowerCase())
					return { sizeMb: info.indexes[i].sizeMb, what: 'index' };
			}
		}
		return null;
	}

	function loadTableInfoAsync(roots, srv, dbname, tableSizeWarnMb, onBoxesChanged, findings, onFindingsChanged) {
		if (!srv || !dbname) return;

		var objNames = [];
		roots.forEach(function (root) {
			walkPlanNodes(root, function (node) {
				var name = node.props && node.props.objName;
				if (!name) return;
				if (objNames.indexOf(name) === -1) objNames.push(name);
				node._tableInfoPending = true;
			});
		});
		if (!objNames.length) return;

		// A panel that was already open when the table-info arrived would otherwise keep showing
		// "Loading..." until the user closed and reopened it. Rebuild it in place, preserving whether
		// it was pinned or a transient tooltip.
		function refreshOpenPanel(node) {
			if (!node._$box) return;
			var open = _panels.panelFor(node._$box[0]);
			if (open) _panels.open(node, node._$box[0], open.pinned);
		}

		$.ajax({
			url: '/api/cc/mgt/table-info',
			data: {
				srv:      srv,
				dbVendor: 'Adaptive Server Enterprise',
				dbname:   dbname,
				tables:   objNames.join(','),
				format:   'json'
			},
			dataType: 'json',
			success: function (r) {
				var tables = (r && r.tables) || {};
				var anyBoxGrew = false; // a marked box gains an extra line - connector lines/tucking measured *before* this need to be redone, see onBoxesChanged below
				roots.forEach(function (root) {
					walkPlanNodes(root, function (node) {
						var name = node.props && node.props.objName;
						if (!name) return;
						node._tableInfoPending = false;
						node._tableInfo = tables[name];

						// Found/missing indicator in the icon row, for every operator a lookup was
						// actually attempted for. Same two-state behaviour as the SQL Server renderer:
						// nothing at all while the lookup is still in flight, then found or missing.
						if (node._$iconRow) {
							var ddlFound = !!(node._tableInfo && node._tableInfo.found !== false);
							var $ddl = $('<div class="ase-plan-icon-badge ase-plan-ddlinfo-icon"></div>')
								.attr('title', ddlFound ? 'DDL/Table info available'
								                        : 'DDL/Table info not found in DbxTune\'s DDL Storage');
							if (!ddlFound) $ddl.addClass('ase-plan-ddlinfo-missing');
							node._$iconRow.append($ddl);
							anyBoxGrew = true;
						}

						var info = node._tableInfo;
						var scanInfo = scannedSizeInfo(node, info);
						if (scanInfo && scanInfo.sizeMb > tableSizeWarnMb && node._$box) {
							node._$box.addClass('ase-plan-big-table');
							node._$box.append($('<div class="ase-plan-metric ase-plan-tablesize-warn"></div>')
								.text('⚠ Large ' + scanInfo.what + ' (' + fmtNum(scanInfo.sizeMb) + ' MB)'));
							anyBoxGrew = true;
							if (findings) {
								findings.push({
									severity: 'warning',
									category: scanInfo.what === 'table' ? 'Table Scan' : 'Index Scan',
									title: 'Large ' + scanInfo.what + ' (' + fmtNum(scanInfo.sizeMb) + ' MB)',
									detail: 'This ' + scanInfo.what + ' scan reads more than the configured threshold (' + tableSizeWarnMb + ' MB) - see the "Big table" option above the diagram.',
									nodeId: node.props && node.props.va,
									nodeName: node.props && node.props.objName
								});
							}
						}

						refreshOpenPanel(node);
					});
				});
				if (anyBoxGrew && onBoxesChanged) onBoxesChanged();
				if (findings && onFindingsChanged) onFindingsChanged(findings);
			},
			error: function () {
				roots.forEach(function (root) {
					walkPlanNodes(root, function (node) { node._tableInfoPending = false; });
				});
			}
		});
	}

	// ─────────────────────────────────────────────────────────────────────────
	// Whole-plan findings - problems/notices that apply to the plan as a whole rather than one
	// specific operator (deferred updates, reformatting, runtime parallel-plan adjustments, optimizer
	// timeouts, ...), reported through opts.onFindingsChanged(findings) into the dialog's "Plan
	// Analysis" section (dbxShowplan.js) - same {severity, category, title, detail, nodeId, nodeName}
	// shape dbxShowplanAnalyzer.js uses for the SQL Server dialog's Plan Analysis. Sourced from SAP's
	// own "Performance and Tuning Series: Query Processing and Abstract Plans" plus real captured-plan
	// text that document doesn't cover (the Optimizer Timeout message) - not guessed.
	// ─────────────────────────────────────────────────────────────────────────
	var PLAN_WARNING_PATTERNS = [
		{ severity: 'warning', category: 'Deferred Update', re: /The update mode is deferred_(varcol|index)\.?/i,
			title: function (m) { return 'Deferred update (deferred_' + m[1].toLowerCase() + ')'; },
			detail: function () { return 'The slowest kind of update ASE performs.'; } },
		{ severity: 'warning', category: 'Runtime Adjustment', re: /AN ADJUSTED QUERY PLAN IS BEING USED FOR STATEMENT\s+(\d+)\s+BECAUSE NOT ENOUGH WORKER PROCESSES ARE CURRENTLY AVAILABLE\.?/i,
			title: function (m) { return 'Statement ' + m[1] + ' fell back to an adjusted plan at runtime'; },
			detail: function () { return 'Not enough worker processes were available for the parallel plan, so a reduced/serial plan was used instead.'; } },
		{ severity: 'warning', category: 'Optimizer Timeout', re: /Optimizer timed out (\d+) time\(s\) while generating this plan,?\s*Timeout Limit percentage:\s*(\d+)/i,
			title: function (m) { return 'Optimizer timed out ' + m[1] + ' time(s) while generating this plan'; },
			// Remediation options below are all straight from SAP's "Performance and Tuning Series:
			// Query Processing and Abstract Plans" doc, not guessed: opttimeoutlimit's 3 levels
			// (§1.2.1), join-count driving search-space size (§1.1 "many possible join orders..."),
			// stored procs/saved plans reducing optimization overhead (§11 Introduction to Abstract
			// Plans - "using a saved plan reduces query optimization overhead" and "Saving plans for
			// queries with long optimization times" is listed as a reason to use one), and
			// forceplan/Abstract Plans to skip the join-order search entirely (§8.2/§11).
			detail: function (m) { return 'Timeout limit percentage: ' + m[2] + '%. The optimizer gave up searching for a better plan once it had a usable one and spent this much of the estimated execution time on optimization.\n'
				+ 'Things to try:\n'
				+ '- Raise "optimization timeout limit" (sp_configure at the server level, "set plan opttimeoutlimit <n>" for the session, or plan "(use opttimeoutlimit <n>)" on the query) to let it search longer for a better plan.\n'
				+ '- Simplify the query - fewer joined tables/UNION branches per statement; the number of possible join orders/access paths grows combinatorially with join count.\n'
				+ '- If this is ad hoc/dynamic SQL (recompiled and re-optimized on every execution), consider a stored procedure or a saved Abstract Plan instead - reusing a plan avoids paying the optimization cost again.\n'
				+ '- Force a known-good join order with "set forceplan on", or bind a captured Abstract Plan, to skip the join-order search entirely.'; } },
		{ severity: 'info', category: 'Forced Plan', re: /Optimized using (?:the Abstract Plan in the PLAN clause|the forced options \(internally generated Abstract Plan\))\.?/i,
			title: function () { return 'Plan forced via an Abstract Plan'; },
			detail: function () { return 'This plan was forced via a bound Abstract Plan or query-level hint, not chosen freely by the optimizer.'; } }
	];

	// Scans the plan's own raw text/XML once for the patterns above - not tied to any one node,
	// since e.g. the runtime-adjustment message is printed before the plan tree, not attached to a
	// particular operator. Repeated identical messages (e.g. several deferred updates) are folded
	// into one finding with a "(xN)" suffix on the title rather than N separate identical entries.
	function collectRawTextFindings(rawText) {
		if (!rawText) return [];
		var counts = {}; // title -> count
		var order  = []; // finding objects, first-seen order
		PLAN_WARNING_PATTERNS.forEach(function (pat) {
			var flags = pat.re.flags.indexOf('g') === -1 ? pat.re.flags + 'g' : pat.re.flags;
			var re = new RegExp(pat.re.source, flags);
			var m;
			while ((m = re.exec(rawText)) !== null) {
				var title = pat.title(m);
				if (!counts[title]) {
					counts[title] = 0;
					order.push({ severity: pat.severity, category: pat.category, title: title, detail: pat.detail(m) });
				}
				counts[title]++;
				if (m.index === re.lastIndex) re.lastIndex++; // avoid an infinite loop on a zero-length match
			}
		});
		order.forEach(function (f) {
			var n = counts[f.title];
			if (n > 1) f.title += ' (×' + n + ')';
		});
		return order;
	}

	// A reformatting STORE/StoreIndex node's real source: per the SAP doc's own worked example (STORE
	// Operator section), the STORE's first child is the INSERT that fills the worktable, whose own
	// child is the SCAN of the actual table being materialized - walk down through intervening
	// operators until a node with a real (non-worktable) props.objName turns up.
	//
	// XML plans (<StoreIndex>) place a <WorkTable> element as a *sibling* of the real Insert/Scan
	// chain rather than folding it into a text annotation the way text plans do ("TO TABLE\nWorktable2.")
	// - naively always following children[0] would dead-end on that WorkTable node if it happens to
	// come first in document order, so it's filtered out of consideration at every level before
	// picking which child to descend into.
	//
	// Also reports whether that walk passed through a join/union operator (more than one *real*
	// child, WorkTable siblings not counted) before reaching a single table. That distinction matters:
	// if the STORE's input is itself the combined output of a UNION/join whose branches are all
	// already seeking through indexes (as opposed to reading straight through from one plain table),
	// then reformatting isn't compensating for a missing index at all - a computed/combined row set
	// has no index to begin with, so there is no single table+column an index could ever be added to.
	// Only the plain-single-table case is a genuine "no useful index existed" situation with an
	// actionable fix.
	function findReformatSource(storeNode) {
		function realChildren(n) {
			return (n.children || []).filter(function (k) { return !/^Work\s*Table$/i.test(k.op || ''); });
		}
		var throughMultiChild = false;
		var kids = realChildren(storeNode);
		if (kids.length > 1) throughMultiChild = true;
		var node = kids[0];
		var depth = 0;
		while (node && depth < 6) {
			if (node.props && node.props.objName && !/^Worktable/i.test(node.props.objName)) {
				return { node: node, throughMultiChild: throughMultiChild };
			}
			var nextKids = realChildren(node);
			if (nextKids.length > 1) throughMultiChild = true;
			node = nextKids[0];
			depth++;
		}
		return { node: null, throughMultiChild: throughMultiChild };
	}

	// Node-tree-based findings - need the parsed tree (not just raw text) to know which table a
	// reformat materializes, or which operator a sort's worktable spill belongs to.
	function collectTreeFindings(stepRoots, dbname) {
		var findings = [];
		stepRoots.forEach(function (root) {
			walkPlanNodes(root, function (node) {
				var p = node.props || {};
				// Only present at all when the plan carries actual execution counts (a real captured/
				// executed plan, not a plain estimated one) - isEstActWarn()/estActPercent() already
				// gate on that. A >10x (or <0.1x) gap between what the optimizer expected and what
				// actually came out is the classic symptom of stale or missing statistics, and it's
				// already shown per-box (the orange border/text - see renderNode()) - surfaced here too
				// so it shows up in the consolidated Plan Analysis list, not just on the diagram.
				if (isEstActWarn(node.metrics || {})) {
					var est = node.metrics.estRows, act = node.metrics.actRows;
					var pct = fmtPercent(estActPercent(node.metrics));
					var opLabel = displayLabelFor(node) + (p.objName ? ' (' + p.objName + (p.corrName ? ' ' + p.corrName : '') + ')' : '');
					findings.push({
						severity: 'warning', category: 'Est/Act Mismatch',
						title: 'Large estimate/actual row mismatch on ' + opLabel,
						detail: 'Estimated ' + (fmtNum(est) || est) + ' rows, actually returned ' + (fmtNum(act) || act) + ' (' + pct + ' of estimate). A gap this large usually means the optimizer\'s statistics are stale or missing for this operator, which can lead it to pick a suboptimal plan - consider running update statistics on the table(s) involved.',
						nodeId: p.va, nodeName: p.objName
					});
				}
				// MRU ("fetch and discard") cache strategy.
				//
				// Reported as INFO, not a warning: ASE picks MRU deliberately for scans it does not
				// expect to revisit, and on a large table that is the right call - it is what stops one
				// big scan from flushing the whole cache. It is worth surfacing because the consequence
				// (these pages are NOT retained for reuse) is invisible otherwise, and because on a
				// server with a data cache big enough to hold the table the trade is no longer worth
				// making. Whether that is the case depends on cache configuration this plan cannot see,
				// so this states the situation and the remedy rather than asserting a problem.
				var mruPages = [];
				if (/^MRU$/i.test(p.dataBufReplStrategy  || '')) mruPages.push('data pages');
				if (/^MRU$/i.test(p.indexBufReplStrategy || '')) mruPages.push('index leaf pages');
				if (mruPages.length) {
					var mruTable = p.objName || 'this table';
					var mruLabel = displayLabelFor(node)
					             + (p.objName ? ' (' + p.objName + (p.corrName ? ' ' + p.corrName : '') + ')' : '');
					findings.push({
						severity: 'info', category: 'Cache Strategy',
						title: 'MRU (fetch and discard) on ' + mruLabel + ' - ' + mruPages.join(' and ') + ' are not cached',
						detail: 'Fetch and Discard. This is used for sequential scans (like full table scans) where '
						      + 'pages are unlikely to be needed again soon. New pages are read into the wash marker '
						      + 'or LRU end, quickly discarding older pages to prevent a single massive query from '
						      + 'wiping out the entire useful cache. The ' + mruPages.join(' and ') + ' this operator '
						      + 'reads are therefore NOT retained in cache for reuse. If the data cache is large '
						      + 'enough to hold ' + mruTable + ', keeping those pages would usually be the better '
						      + 'trade - the strategy can be turned off per table:',
						suggestedDdl: "sp_cachestrategy '" + (dbname || 'dbname') + "', '"
						            + (p.objName || 'tablename') + "', 'mru', 'off'",
						nodeId: p.va, nodeName: p.objName
					});
				}

				if (p.workTable && /sort/i.test(node.op || '')) {
					// Having a worktable at all doesn't mean it actually spilled to disk - SORT can use
					// one purely in memory. physical I/O (pio) is the real "touched disk" signal;
					// logical I/O (lio) alone just means the worktable's pages were read/written at all,
					// which happens even for an in-memory worktable. Only warn when pio actually shows
					// disk activity; otherwise this is informational at most.
					var m   = node.metrics || {};
					var lio = m.actLio, pio = m.actPio;
					var severity, ioNote;
					if (pio === undefined) {
						severity = 'info';
						ioNote = 'No actual I/O counts are available for this plan, so whether it spilled to disk or stayed in memory can\'t be confirmed from here.';
					} else if (pio > 0) {
						severity = 'warning';
						ioNote = 'Physical I/O: ' + fmtNum(pio) + ' page(s)' + (lio !== undefined ? ' (logical I/O: ' + fmtNum(lio) + ')' : '') + ' - it actually spilled to disk.';
					} else {
						severity = 'info';
						ioNote = 'Physical I/O: 0' + (lio !== undefined ? ' (logical I/O: ' + fmtNum(lio) + ')' : '') + ' - it stayed in memory, no real disk cost was paid.';
					}
					findings.push({
						severity: severity, category: 'Sort Spill',
						title: 'Sort required a worktable (' + p.workTable + ')',
						detail: 'No existing index provided the order it needed. ' + ioNote,
						nodeId: p.va, nodeName: p.objName
					});
				}
				if (p.reformatWorktable) {
					var srcInfo = findReformatSource(node);
					var src = srcInfo.node;
					var label = (src && src.props && src.props.objName)
						? src.props.objName + (src.props.corrName ? ' ' + src.props.corrName : '')
						: 'a table';
					if (srcInfo.throughMultiChild) {
						// The materialized input is itself a join/union of already-indexed branches -
						// nothing here is missing an index, so no reformatSource/index suggestion, and
						// the box gets the milder "info" marking below, not the red "warning" one.
						findings.push({
							severity: 'info', category: 'Reformatting',
							title: 'Reformatting: materialized a joined/unioned result into a worktable',
							detail: 'This is typically a derived table (a subquery used as a row source in the FROM clause) whose body combines multiple already-indexed inputs - often via UNION - into one computed result. ASE built a worktable (' + p.reformatWorktable + ') with a clustered index over that result so it could be used efficiently afterward. The inputs underneath are already seeking via index, so this is not a sign of a missing index on a real table - a derived table\'s output has no index to begin with, so there is no single table/column an index could be added to here.',
							nodeId: p.va, nodeName: label
						});
						if (node._$box) {
							node._$box.addClass('ase-plan-reformat-info');
							node._$box.append($('<div class="ase-plan-metric ase-plan-reformat-info-metric"></div>')
								.text('ℹ Reformatting (' + p.reformatWorktable + ')'));
						}
					} else {
						findings.push({
							severity: 'warning', category: 'Reformatting',
							title: 'Reformatting: materialized ' + label + ' into a worktable',
							detail: 'No useful index existed, so ASE built a worktable (' + p.reformatWorktable + ') with a clustered index just to run this query.',
							nodeId: p.va, nodeName: label,
							reformatSource: src // consumed by enhanceReformatFindings() below, not rendered directly
						});
						// Same visual treatment as the "Large table/index" mark (loadTableInfoAsync()
						// below) - a red border + an inline metric line right on the box, not just a
						// line in Plan Analysis, so the operator that needs the index is easy to spot.
						if (node._$box) {
							node._$box.addClass('ase-plan-reformat');
							node._$box.append($('<div class="ase-plan-metric ase-plan-tablesize-warn"></div>')
								.text('⚠ Reformatting (' + p.reformatWorktable + ')'));
						}
					}
				}
			});
		});
		return findings;
	}

	// Async, best-effort: for each reformatting finding, ask DbxSqlTableNames (dbxSqlTableNames.js)
	// to find the join/filter column(s) the materialized table is used on in the statement's SQL
	// text, then appends a suggestion to that finding's detail and re-renders the section once
	// resolved. Silently does nothing when sqlText isn't available (bare pasted plan, no SQL) or no
	// column can be found - the base finding already stands on its own without the suggestion.
	function enhanceReformatFindings(findings, sqlText, onChanged) {
		if (!sqlText || typeof DbxSqlTableNames === 'undefined' || !DbxSqlTableNames.findJoinColumnsForTable) return;
		findings.forEach(function (f) {
			if (!f.reformatSource) return;
			var table = f.reformatSource.props && f.reformatSource.props.objName;
			var corr  = f.reformatSource.props && f.reformatSource.props.corrName;
			if (!table) return;
			DbxSqlTableNames.findJoinColumnsForTable(sqlText, table, corr, function (cols) {
				if (!cols || !cols.length) return;
				f.detail += ' Consider an index on ' + table + '(' + cols.join(', ') + ').';
				// A starting point, not a guaranteed-correct statement - see findJoinColumnsForTable()'s
				// own caveats (regex/AST heuristics, ambiguous aliases in nested scopes). Rendered as
				// its own code line by renderFindingsListHtml() (dbxShowplan.js), not folded into detail.
				var ixName = 'ix_' + table.replace(/\W/g, '') + '_' + cols.map(function (c) { return c.replace(/\W/g, ''); }).join('_');
				f.suggestedDdl = 'CREATE INDEX ' + ixName + ' ON ' + table + ' (' + cols.join(', ') + ')';
				if (onChanged) onChanged(findings);
			});
		});
	}

	function render(container, parsed, opts) {
		injectStyle();
		var $container = $(container);
		$container.empty();
		// Reset before the early return too: a plan with nothing to draw must not leave the pane
		// describing an operator from the previously-rendered plan.
		_propsTarget = (opts && opts.propsTarget) || null;
		_selectedBox = null;
		if (_propsTarget) renderPropertiesInto(_propsTarget, null);
		if (!parsed || !parsed.statements || !parsed.statements.length) return;

		var horizontal        = !!(opts && opts.horizontal);
		var useCompactLayout  = !!(opts && opts.layout === 'compact');
		var useLineConnectors = useCompactLayout || !!(opts && opts.connectorStyle === 'lines');
		var $wrap = $('<div class="ase-plan-wrap"></div>');
		var multiStatement = parsed.statements.length > 1;
		var treesForLineDrawing  = []; // arrows drawn after everything is live/laid-out by the browser
		var treesForCompactTuck  = []; // leaves tucked near their parent after the natural table layout
		var stepRoots = []; // every step's root node - walked afterward for the table-info lookup below

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
				stepRoots.push(step.root);
			});
		});

		// Close open detail panels when clicking anywhere else in the diagram.
		// Panels live on <body> now, so they are not inside $wrap to be found - close them through the
		// shared panel system instead. Tooltips and pinned panels both go, matching the old behaviour
		// of clicking empty diagram space.
		$wrap.on('click', function () { _panels.closeAll(); });

		$container.append($wrap);

		// Whole-plan findings (deferred updates, reformatting, runtime plan adjustments, optimizer
		// timeouts, ...) - reported into the dialog's separate "Plan Analysis" section (dbxShowplan.js),
		// not rendered here. Called even with zero findings so that section can show "no issues
		// detected" the same way SQL Server's does, rather than staying hidden/stale from a previous plan.
		var planFindings = collectRawTextFindings(parsed.rawText)
		                     .concat(collectTreeFindings(stepRoots, opts && opts.dbname));
		if (opts && opts.onFindingsChanged) opts.onFindingsChanged(planFindings);
		enhanceReformatFindings(planFindings, opts && opts.sqlText, opts && opts.onFindingsChanged);

		// Both need real measured positions/sizes (getBoundingClientRect() reports zero for detached
		// elements), so both run after $tree is attached to the live document. Tucking must run before
		// the connector lines are drawn, so the arrows reflect the tucked (final) positions, not the
		// pre-tuck natural table-flow ones. Pulled into a named function (both are idempotent - see
		// their own comments - safe to just re-measure and re-apply from scratch) since it also runs a
		// second time, later, if the table-info lookup below ends up growing any box.
		function layoutTuckAndConnectors() {
			if (useCompactLayout) {
				var tuckFn = horizontal ? tuckLeavesNearParent : tuckLeavesNearParentVertical;
				treesForCompactTuck.forEach(function ($tree) { tuckFn($tree[0]); });
			}
			treesForLineDrawing.forEach(function ($tree) { drawConnectorLines($tree[0], horizontal); });
		}
		layoutTuckAndConnectors();

		// Fired last, after the diagram is fully laid out and visible - a network round trip must
		// never hold up the initial draw. Needs srv/dbname (only supplied by the Showplan dialog,
		// which has a live server context - the standalone paste-a-plan page doesn't, and simply
		// skips this via the srv/dbname check inside loadTableInfoAsync()). A Table Scan flagged as
		// "big" grows its own box by one line, which would otherwise leave the tuck padding/connector
		// lines above pointing at stale (pre-growth) positions - loadTableInfoAsync() calls
		// layoutTuckAndConnectors() again, but only if a box actually grew.
		var tableSizeWarnMb = (opts && typeof opts.tableSizeWarnMb === 'number' && opts.tableSizeWarnMb >= 0) ? opts.tableSizeWarnMb : 100;
		loadTableInfoAsync(stepRoots, opts && opts.srv, opts && opts.dbname, tableSizeWarnMb, layoutTuckAndConnectors,
			planFindings, opts && opts.onFindingsChanged);
	}

	return {
		parseXml: parseXml,
		parseText: parseText,
		render: render,
		// Public so dbxShowplan.js can clear/repopulate the pane itself (e.g. when switching plans),
		// matching the SQL Server renderer's surface.
		renderPropertiesInto: renderPropertiesInto,
		// Detail panels are attached to <body> to escape the diagram's scroll clipping, so a caller
		// that tears the diagram down - or the global Escape handler - needs a way to dismiss them.
		// Mirrors SqlServerShowplan's surface.
		closePanels: function () { _panels.closeAll(false); },
		anyPanelsOpen: function () { return _panels.anyOpen(); },
		getLastParseError: function () { return lastParseError; }
	};
})();
