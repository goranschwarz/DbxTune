/**
 * dbxShowplanDb2.js — Db2 (LUW) graphical execution plan renderer
 *
 * Parses the plan JSON that Db2Tune records (CmActiveStatements, column 'EXEC_PLAN': the explain tables after
 * EXPLAIN_FROM_SECTION, see Db2ExplainUtils.java, "planFormat": "db2-explain-tables-1") into the same tree model the
 * other renderers use, and renders it as the same CSS/DOM box-and-connector diagram (built on dbxShowplanMySql.js /
 * dbxShowplanOracle.js). It is shown by the /showplan/db2 page (ShowplanDb2Servlet).
 *
 * The JSON holds the rows of the explain tables:
 *   operators[] (EXPLAIN_OPERATOR + its EXPLAIN_ARGUMENT and EXPLAIN_PREDICATE rows), streams[] (EXPLAIN_STREAM: what flows
 *   from an operator or a table/index into an operator, with the estimated rows), objects[] (EXPLAIN_OBJECT: the tables and
 *   indexes, with their statistics). The tree is the streams: an operator's inputs are the streams that target it. A table or
 *   an index is a leaf box of its own, as db2exfmt and Visual Explain draw it.
 *
 * Entry points (the same as OracleShowplan / MySqlShowplan):
 *   var parsed = Db2Showplan.parseText(planText);
 *   Db2Showplan.render(containerEl, parsed, opts);
 *
 * parseText() returns null when the text is not a plan it can draw (the reason is available via getLastParseError()),
 * so the caller can fall back to showing the text as it is.
 *
 * Node model (the same shape as the other renderers):
 *   {
 *     op:       'TBSCAN',                   // the Db2 operator, or 'TABLE' / 'INDEX' for a leaf
 *     label:    'TBSCAN',
 *     metrics:  { estRows, cost, ownCost, ownCostPct, cpuCost, ioCost, firstRowCost, buffers,   // the optimizer's estimate (cost in timerons)
 *                 objRows, objPages },                                                         // a table / index leaf: its statistics
 *     props:    { va (= the operator number of Db2), objName, objType, access[], filter[], flags[], statsTime, tablespace,
 *                 noStats, cells{} (every argument of the operator, as Db2 wrote it) },
 *     children: [ node, ... ]
 *   }
 * Top-level result:
 *   { format: 'json', rawText, statements: [ { label, meta, steps: [ { label, root: node } ] } ] }
 * The root node also has: _planInfo { sqlText, statementType, totalCost, queryOpt, isolation, db2Version, explainTime, operations, notes[] }
 *
 * The layout CSS and the connector drawing (drawConnectorLines) are a copy of dbxShowplanMySql.js, with the class prefix
 * changed to 'db2-plan' ("copy now, extract later", as the Oracle, SQL Server and MySQL renderers were made).
 * The layout plumbing that is already shared lives in dbxShowplanGraph.js, loaded BEFORE this file.
 */

window.Db2Showplan = (function () {

	// Why the most recent parseText() returned null
	var lastParseError = null;

	// ─────────────────────────────────────────────────────────────────────────
	// Parser
	// ─────────────────────────────────────────────────────────────────────────

	/** 39700.75 -> 39700.75, "" / undefined / negative (= not known: a section explain has no I/O cost) -> undefined */
	function num(v) {
		if (v === undefined || v === null || v === '') return undefined;
		var n = parseFloat(v);
		return isNaN(n) || n < 0 ? undefined : n;
	}

	function newNode(op, label) {
		return { op: op, label: label || op, metrics: {}, props: { access: [], filter: [], flags: [], cells: {} }, children: [] };
	}

	// EXPLAIN_PREDICATE.HOW_APPLIED: START / STOP = the range of an index scan (the "seek"), JOIN = the join condition,
	// SARG / RESID / PREDICATE... = a filter on the rows that are read
	var ACCESS_HOW = { START: true, STOP: true, JOIN: true };

	// The arguments that say something worth a line in the box panel (EXPLAIN_ARGUMENT.ARGUMENT_TYPE)
	var FLAG_ARGS = {
		SCANDIR: 'Scan direction', PREFETCH: 'Prefetch', MAXPAGES: 'Max pages', TABLOCK: 'Table lock', ROWLOCK: 'Row lock',
		JN_INPUT: 'Join input', EARLYOUT: 'Early out', FETCHMAX: 'Fetch max', SORTKEY: 'Sort key', GROUPBYC: 'Group by columns',
		GROUPBYN: 'Group by columns', UNIQUE: 'Unique', TEMPSIZE: 'Temp size', SPILLED: 'Spilled', OUTERJN: 'Outer join',
		ISCANMAX: 'Index scan max', JN_TYPE: 'Join type', HASHCODE: 'Hash code', BITFLTR: 'Bit filter'
	};

	function parseText(planText) {
		lastParseError = null;
		if (!planText || !String(planText).trim()) { lastParseError = 'The plan is empty.'; return null; }

		var text  = String(planText);
		var start = text.indexOf('{'), end = text.lastIndexOf('}');
		if (start === -1 || end <= start) { lastParseError = 'This is not a Db2Tune plan (no { ... } was found). It is the JSON that Db2Tune records, not the text of db2exfmt.'; return null; }

		var json;
		try {
			json = JSON.parse(text.substring(start, end + 1));
		} catch (ex) {
			lastParseError = 'The JSON could not be read: ' + ex.message;
			return null;
		}
		if (!json.operators || !json.streams) {
			lastParseError = 'This JSON is not a Db2Tune plan: it has no "operators" and "streams" (planFormat db2-explain-tables-1).';
			return null;
		}

		// The tables and indexes, by schema.name
		var objects = {};
		(json.objects || []).forEach(function (o) { objects[o.schema + '.' + o.name] = o; });

		// The operators, by id, as nodes
		var nodes = {};
		json.operators.forEach(function (o) {
			var node = newNode(o.type || '?');
			var p = node.props, m = node.metrics;
			p.va           = o.id;
			m.cost         = num(o.totalCost);
			m.ioCost       = num(o.ioCost);
			m.cpuCost      = num(o.cpuCost);
			m.firstRowCost = num(o.firstRowCost);
			m.buffers      = num(o.buffers);

			(o.predicates || []).forEach(function (pr) {
				var txt = String(pr.text || '').replace(/\s+/g, ' ').trim();
				if (!txt) return;
				var line = txt + (pr.filterFactor !== undefined && pr.filterFactor !== null ? '   (filter factor ' + fmtNum(pr.filterFactor) + ')' : '');
				if (ACCESS_HOW[pr.howApplied]) p.access.push(pr.howApplied + ': ' + line);
				else                           p.filter.push((pr.howApplied ? pr.howApplied + ': ' : '') + line);
			});
			(o.args || []).forEach(function (a) {
				var key = a.type, val = String(a.value === undefined || a.value === null ? '' : a.value).trim();
				p.cells[key] = p.cells[key] ? p.cells[key] + ', ' + val : val;
				if (FLAG_ARGS[key]) p.flags.push(FLAG_ARGS[key] + ': ' + val);
			});
			nodes[o.id] = node;
		});

		// The streams: an operator's inputs, and the rows that come out of an operator
		var feedsOther = {};
		json.streams.forEach(function (s) {
			if (s.targetType !== 'O' || !nodes[s.targetId]) return;
			var parent = nodes[s.targetId];

			if (s.sourceType === 'O') {
				var child = nodes[s.sourceId];
				if (!child) return;
				feedsOther[s.sourceId] = true;
				child.metrics.estRows = num(s.rows);
				parent.children.push(child);
			} else {
				// A table or an index: a leaf box. The operator that reads it gets its name too (the subtitle)
				var name = s.objectSchema + '.' + s.objectName;
				var obj  = objects[name] || {};
				var leaf = newNode(obj.type === 'IX' ? 'INDEX' : 'TABLE', s.objectName);
				leaf.props.objName    = name;
				leaf.props.objType    = obj.type;
				leaf.props.statsTime  = obj.statsTime;
				leaf.props.tablespace = obj.tablespace;
				leaf.props.isLeaf     = true;
				leaf.props.noStats    = obj.type === 'TA' && !obj.statsTime;
				leaf.metrics.objRows  = obj.rows !== undefined && obj.rows >= 0 ? obj.rows : undefined;
				leaf.metrics.objPages = obj.pages !== undefined && obj.pages >= 0 ? obj.pages : undefined;
				leaf.metrics.estRows  = num(s.rows);
				if (obj.type === 'IX') {
					leaf.props.cells['Leaf pages']        = obj.nleaf;
					leaf.props.cells['Levels']            = obj.nlevels;
					leaf.props.cells['Full key distinct'] = obj.fullKeyCard;
				}
				if (s.columns) leaf.props.cells['Columns read'] = s.columns;
				parent.children.push(leaf);

				// (an index does not say its table: an IXSCAN gets the index only)
				if (obj.type === 'IX')
					parent.props.keyName = name;
				else
					parent.props.objName = name;
			}
		});

		// The root: the operator that feeds no other operator (normally RETURN, operator 1)
		var tops = Object.keys(nodes).filter(function (id) { return !feedsOther[id]; }).map(function (id) { return nodes[id]; });
		if (!tops.length) { lastParseError = 'The plan has no top operator (every operator feeds another one).'; return null; }
		var root = tops[0];

		// The part of the cost that is each operator's own: Db2's TOTAL_COST includes its inputs
		DbxShowplanGraph.walkPlanNodes(root, function (n) {
			if (n.props.isLeaf || n.metrics.cost === undefined) return;
			var childCost = 0;
			n.children.forEach(function (c) { if (!c.props.isLeaf && c.metrics.cost !== undefined) childCost += c.metrics.cost; });
			n.metrics.ownCost = Math.max(0, n.metrics.cost - childCost);
		});
		var total = num(json.totalCost) !== undefined ? num(json.totalCost) : root.metrics.cost;
		var operations = 0;
		DbxShowplanGraph.walkPlanNodes(root, function (n) {
			if (n.props.isLeaf) return;
			operations++;
			if (n.metrics.ownCost !== undefined && total > 0 && n !== root) n.metrics.ownCostPct = n.metrics.ownCost * 100 / total;
		});
		// A leaf has no Db2 number: it gets one after the operators (what a finding's "[Node N]" jumps to)
		var maxId = 0;
		Object.keys(nodes).forEach(function (id) { maxId = Math.max(maxId, parseInt(id, 10)); });
		DbxShowplanGraph.walkPlanNodes(root, function (n) { if (n.props.isLeaf) n.props.va = ++maxId; });

		var notes = [];
		if (json.isolation === 'RR') notes.push('Isolation RR (repeatable read): every row that is read is locked until the end of the transaction, also the rows that do not qualify.');

		root._planInfo = {
			sqlText:       json.statementText,
			statementType: json.statementType,
			totalCost:     total,
			queryOpt:      json.queryOpt,
			isolation:     json.isolation,
			db2Version:    json.db2Version,
			explainTime:   json.explainTime,
			source:        json.source,
			operations:    operations,
			notes:         notes,
			sections:      {}
		};

		return { format: 'json', rawText: planText, statements: [ { label: 'Plan', meta: {}, steps: [ { label: '', root: root } ] } ] };
	}

	// ─────────────────────────────────────────────────────────────────────────
	// Render
	// ─────────────────────────────────────────────────────────────────────────

	var STYLE_INJECTED = false;

	// The rules of dbxShowplanMySql.js (and dbxShowplanOracle.js, where they are explained), with the prefix 'db2-plan'
	var CSS = ''
		+ '.db2-plan-wrap { font-family: -apple-system, Segoe UI, Roboto, sans-serif; font-size: 0.8em; padding: 8px 0; }'
		+ '.db2-plan-tree { width: max-content; padding: 8px 4px 16px 4px; }'
		+ '.db2-plan-tree ul, .db2-plan-tree ul ul { display: flex; justify-content: center; padding-top: 20px; position: relative; }'
		+ '.db2-plan-tree ul { list-style: none; margin: 0; padding-left: 0; }'
		+ '.db2-plan-tree li { list-style: none; position: relative; padding: 20px 8px 0 8px; display: flex; flex-direction: column; align-items: center; }'
		+ '.db2-plan-tree li::before, .db2-plan-tree li::after { content: ""; position: absolute; top: 0; right: 50%; border-top: 1px solid var(--sp-edge-2); width: 50%; height: 20px; }'
		+ '.db2-plan-tree li::after { right: auto; left: 50%; border-left: 1px solid var(--sp-edge-2); }'
		+ '.db2-plan-tree li:only-child { padding-top: 0; }'
		+ '.db2-plan-tree li:only-child::before, .db2-plan-tree li:only-child::after { display: none; }'
		+ '.db2-plan-tree li:first-child::before, .db2-plan-tree li:last-child::after { border: 0 none; }'
		+ '.db2-plan-tree li:last-child::before { border-right: 1px solid var(--sp-edge-2); border-radius: 0 5px 0 0; }'
		+ '.db2-plan-tree li:first-child::after { border-radius: 5px 0 0 0; }'
		+ '.db2-plan-tree ul ul::before { content: ""; position: absolute; top: 0; left: 50%; border-left: 1px solid var(--sp-edge-2); width: 0; height: 20px; }'
		+ '.db2-plan-tree > ul > li { padding-top: 0; }'
		+ '.db2-plan-tree > ul > li::before, .db2-plan-tree > ul > li::after { display: none; }'
		+ '.db2-plan-tree.db2-plan-horizontal ul, .db2-plan-tree.db2-plan-horizontal ul ul { flex-direction: column; justify-content: center; padding-top: 0; padding-left: 24px; }'
		+ '.db2-plan-tree.db2-plan-horizontal li { padding: 8px 0 8px 24px; flex-direction: row; align-items: center; }'
		+ '.db2-plan-tree.db2-plan-horizontal li::before, .db2-plan-tree.db2-plan-horizontal li::after { top: auto; left: 0; right: auto; bottom: 50%; border-top: 0 none; border-left: 1px solid var(--sp-edge-2); width: 24px; height: 50%; }'
		+ '.db2-plan-tree.db2-plan-horizontal li::after { bottom: auto; top: 50%; border-top: 1px solid var(--sp-edge-2); }'
		+ '.db2-plan-tree.db2-plan-horizontal li:only-child { padding-left: 0; }'
		+ '.db2-plan-tree.db2-plan-horizontal li:first-child::before { border: 0 none; }'
		+ '.db2-plan-tree.db2-plan-horizontal li:last-child::after { border: 0 none; }'
		+ '.db2-plan-tree.db2-plan-horizontal li:last-child::before { border-right: 0 none; border-bottom: 1px solid var(--sp-edge-2); border-radius: 0; }'
		+ '.db2-plan-tree.db2-plan-horizontal li:first-child::after { border-radius: 0; }'
		+ '.db2-plan-tree.db2-plan-horizontal ul ul::before { top: 50%; left: 0; border-left: 0 none; border-top: 1px solid var(--sp-edge-2); width: 24px; height: 0; }'
		+ '.db2-plan-tree.db2-plan-horizontal > ul > li { padding-left: 0; }'
		+ '.db2-plan-tree.db2-plan-lines li::before, .db2-plan-tree.db2-plan-lines li::after, .db2-plan-tree.db2-plan-lines ul ul::before { display: none !important; }'
		+ '.db2-plan-tree.db2-plan-lines { position: relative; }'
		+ '.db2-plan-connector-svg { position: absolute; top: 0; left: 0; overflow: visible; pointer-events: none; }'
		+ '.db2-plan-box-cell { display: contents; }'
		+ '.db2-plan-tree.db2-plan-compact-h ul { display: block; list-style: none; margin: 0; padding: 0; position: static; }'
		+ '.db2-plan-tree.db2-plan-compact-h li { display: table; list-style: none; position: static; padding: 0; margin: 0; }'
		+ '.db2-plan-tree.db2-plan-compact-h li > .db2-plan-box-cell { display: table-cell; vertical-align: top; }'
		+ '.db2-plan-tree.db2-plan-compact-h li > ul { display: table-cell; vertical-align: top; padding-left: 56px; }'
		+ '.db2-plan-tree.db2-plan-compact-h li > ul > li { display: table; margin: 9px 0; }'
		+ '.db2-plan-tree.db2-plan-compact-h li > ul > li:first-child { margin-top: 0; }'
		+ '.db2-plan-tree.db2-plan-compact-h li > ul > li:last-child { margin-bottom: 0; }'
		+ '.db2-plan-tree.db2-plan-compact-v ul { display: block; list-style: none; margin: 0; padding: 0; position: static; }'
		+ '.db2-plan-tree.db2-plan-compact-v li { display: table; list-style: none; position: static; padding: 0; margin: 0; }'
		+ '.db2-plan-tree.db2-plan-compact-v li > .db2-plan-box-cell { display: table-caption; caption-side: top; text-align: center; margin-bottom: 6px; }'
		+ '.db2-plan-tree.db2-plan-compact-v .db2-plan-box { display: inline-block; }'
		+ '.db2-plan-tree.db2-plan-compact-v li > ul { display: table-row; }'
		+ '.db2-plan-tree.db2-plan-compact-v li > ul > li { display: table-cell; vertical-align: top; padding: 40px 6px 0 6px; }'
		+ '.db2-plan-tree.db2-plan-compact-v li > ul > li:first-child { padding-left: 0; }'
		+ '.db2-plan-tree.db2-plan-compact-v li > ul > li:last-child { padding-right: 0; }'
		+ '.db2-plan-tree.db2-plan-compact-h li:only-child { padding: 0; }'
		+ '.db2-plan-box { position: relative; border: 1px solid var(--sp-node-border); border-radius: 5px; background: var(--sp-bg); padding: 5px 9px; cursor: pointer; min-width: 120px; max-width: 240px; text-align: center; box-shadow: 0 1px 2px var(--sp-shadow); }'
		+ '.db2-plan-box:hover { border-color: var(--sp-sel); }'
		+ '.db2-plan-box.db2-plan-warn { border-color: var(--sp-warn-border); background: var(--sp-warn-bg); }'
		// A table or an index: a leaf, drawn lighter than the operators (as db2exfmt draws it apart)
		+ '.db2-plan-box.db2-plan-object { border-style: dashed; min-width: 100px; background: var(--sp-bg-2); }'
		+ '.db2-plan-va { position: absolute; top: 2px; right: 4px; font-size: 0.72em; color: var(--sp-text-7); line-height: 1; }'
		+ '.db2-plan-icon { width: 32px; height: 32px; margin: 0 auto; background-repeat: no-repeat; }'
		+ '.db2-plan-label { font-weight: 600; white-space: nowrap; }'
		+ '.db2-plan-label.db2-plan-label-long { white-space: normal; min-width: 150px; }'
		+ '.db2-plan-subtitle { font-size: 0.85em; color: var(--sp-text-2); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 220px; margin: 0 auto; }'
		+ '.db2-plan-metric { font-size: 0.85em; color: var(--sp-text-3); white-space: nowrap; }'
		+ '.db2-plan-metric.db2-plan-warn-text { color: var(--sp-warn-text-3); font-weight: 600; }'
		+ '.db2-plan-metric-pct-warn { color: var(--sp-err); font-weight: 700; }'
		+ '.db2-plan-detail-pct-warn { color: var(--sp-err); font-weight: 700; }'
		+ '.db2-plan-detail { position: fixed; z-index: 2000; color: var(--sp-text); background: var(--sp-tt-bg); border: 1px solid var(--sp-tt-border); border-radius: 4px; padding: 6px 10px; min-width: 220px; max-width: 560px; text-align: left; box-shadow: 0 2px 10px var(--sp-shadow-2); font-size: 11px; line-height: 1.35; }'
		+ '.db2-plan-detail { max-height: 80vh; overflow-y: auto; overscroll-behavior: contain; }'
		+ '.db2-plan-detail table { border-collapse: collapse; }'
		+ '.db2-plan-detail-desc { white-space: normal; font-style: italic; color: var(--sp-tt-text); margin-bottom: 6px; padding-bottom: 6px; border-bottom: 1px solid var(--sp-tt-sep); line-height: 1.35; }'
		+ '.db2-plan-detail-idx-hdr { font-size: 10px; color: var(--sp-tt-text); margin-top: 4px; }'
		+ '.db2-plan-detail-wrap { white-space: normal; word-break: break-word; }'
		+ '.db2-plan-detail-hint { margin-top: 5px; padding-top: 4px; border-top: 1px solid var(--sp-tt-sep); color: var(--sp-tt-muted); font-size: 10px; white-space: normal; }'
		+ '.db2-plan-connector-hit { pointer-events: stroke; cursor: help; }'
		+ '.db2-plan-detail td { padding: 1px 6px 1px 0; vertical-align: top; white-space: nowrap; }'
		+ '.db2-plan-detail td.db2-plan-detail-key { color: var(--sp-text-4); }'
		+ '.db2-plan-detail .db2-plan-raw-line { font-family: monospace; white-space: pre-wrap; color: var(--sp-text-2); }'
		+ '.db2-plan-detail.db2-plan-tooltip { pointer-events: none; cursor: default; }'
		+ '@keyframes db2-plan-flash { 0%, 100% { box-shadow: 0 1px 2px var(--sp-shadow); } 20%, 60% { box-shadow: 0 0 0 5px rgba(74,144,217,0.85); } 40%, 80% { box-shadow: 0 1px 2px var(--sp-shadow); } }'
		+ '.db2-plan-box.db2-plan-flash { animation: db2-plan-flash 0.8s ease-in-out 5; }'
		+ DbxShowplanGraph.propsCss('db2-plan')
		+ '.db2-plan-box.db2-plan-selected { border-color: var(--sp-blue); border-width: 2px; box-shadow: 0 0 0 3px rgba(42,110,187,0.18); }'
		+ '.db2-plan-prop-raw { font-family: monospace; white-space: pre-wrap; word-break: break-word; font-size: 10px; color: var(--sp-text-2); line-height: 1.35; }'
		// A note that a finding of the Plan Analysis puts on the box of its operation
		+ '.db2-plan-note { white-space: normal; font-weight: 600; }'
		+ '.db2-plan-note-warning, .db2-plan-note-error { color: var(--sp-err); }'
		+ '.db2-plan-note-info { color: var(--sp-blue); }';

	function injectStyle() {
		if (STYLE_INJECTED) return;
		var styleEl = document.createElement('style');
		styleEl.type = 'text/css';
		styleEl.appendChild(document.createTextNode(CSS));
		document.head.appendChild(styleEl);
		STYLE_INJECTED = true;
	}

	// The icons are the sprite sheet of the SQL Server plan viewer (html-query-plan), as in the other renderers
	var ICON_SPRITE_URL = '/images/qp_icons.png';
	var ICON = {
		Catchall:           [ -96, -256],
		Result:             [-256, -128],
		TableScan:          [ -32, -192],
		IndexSeek:          [-128,  -96],
		IndexScan:          [ -96,  -96],
		Bitmap:             [-256, -192],
		NestedLoops:        [   0,  -96],
		HashMatch:          [ -64,  -64],
		Sort:               [-128, -160],
		StreamAggregate:    [-224, -160],
		Concatenation:      [ -64,  -32],
		Filter:             [   0,  -64],
		Top:                [-160, -192],
		ComputeScalar:      [ -32,  -32],
		ConstantScan:       [ -96,  -32],
		TableSpool:         [ -64, -192],
		Insert:             [   0, -192],
		Update:             [ -96, -192],
		Delete:             [-288, -160]
	};
	// The Db2 operator -> icon. The first that matches is used
	var ICON_RULES = [
		{ test: /^RETURN$/,                       icon: 'Result' },
		{ test: /^INSERT$/,                       icon: 'Insert' },
		{ test: /^UPDATE$/,                       icon: 'Update' },
		{ test: /^DELETE$/,                       icon: 'Delete' },
		{ test: /^(TBSCAN|CTQ)$/,                 icon: 'TableScan' },
		{ test: /^IXSCAN$/,                       icon: 'IndexScan' },
		{ test: /^(FETCH|EISCAN|XISCAN)$/,        icon: 'IndexSeek' },
		{ test: /^(IXAND|RIDSCN)$/,               icon: 'Bitmap' },
		{ test: /^HSJOIN$/,                       icon: 'HashMatch' },
		{ test: /^(NLJOIN|MSJOIN|ZZJOIN)$/,       icon: 'NestedLoops' },
		{ test: /^SORT$/,                         icon: 'Sort' },
		{ test: /^(GRPBY|UNIQUE)$/,               icon: 'StreamAggregate' },
		{ test: /^UNION$/,                        icon: 'Concatenation' },
		{ test: /^FILTER$/,                       icon: 'Filter' },
		{ test: /^TEMP$/,                         icon: 'TableSpool' },
		{ test: /^(TBFUNC|GENROW)$/,              icon: 'ConstantScan' },
		{ test: /^TABLE$/,                        icon: 'TableScan' },
		{ test: /^INDEX$/,                        icon: 'IndexScan' }
	];
	function iconStyleFor(node) {
		var pos = ICON.Catchall;
		for (var i = 0; i < ICON_RULES.length; i++) {
			if (ICON_RULES[i].test.test(node.op)) { pos = ICON[ICON_RULES[i].icon]; break; }
		}
		return 'background-image:url(' + ICON_SPRITE_URL + ');background-position:' + pos[0] + 'px ' + pos[1] + 'px;';
	}

	// What the operator does, in plain words, for the hover panel and the Properties pane
	var OPERATION_DESCRIPTIONS = {
		RETURN: 'The result of the statement. Its cost is the optimizer\'s estimate for the whole statement, in timerons (Db2\'s own unit, not time).',
		TBSCAN: 'Table scan: every page of the table (or of a temporary table) is read, and the rows that pass the predicates (SARG) are kept. The right choice when a large part of the table is needed, expensive when only a few rows are.',
		IXSCAN: 'Index scan: reads the part of the index between the START and STOP keys (all of it when there are none). Gives row ids to a FETCH, or the column values themselves when the index has all the columns that are needed (index-only access).',
		FETCH:  'Reads the table rows of the row ids an index scan found (one page read per row, unless the table is clustered on the index). Many rows through a FETCH with a low cluster ratio = many random reads.',
		NLJOIN: 'Nested loop join: for each row of the outer input (the left one) the inner input is executed once. Good when the outer input gives few rows and the inner one is an index scan; slow when the inner input is a table scan.',
		HSJOIN: 'Hash join: the inner input (the build table, the right one) is read into a hash table in SORTHEAP, the outer input is read once and each row is looked up in it. When the hash table does not fit it spills to the temporary table space.',
		MSJOIN: 'Merge join: both inputs are read in the order of the join columns (sorted, or from an index) and merged.',
		ZZJOIN: 'Zigzag join: a star join that uses the indexes of the fact table to look up the rows of several dimensions.',
		SORT:   'Sorts the rows (ORDER BY, GROUP BY, DISTINCT, or for a merge join). In SORTHEAP when they fit, otherwise it overflows to the temporary table space (sort overflows).',
		GRPBY:  'Groups the rows for GROUP BY (or one group for an aggregate without GROUP BY) and calculates the aggregates. The rows come in sorted order.',
		UNIQUE: 'Removes duplicate rows (DISTINCT). The rows come in sorted order.',
		FILTER: 'Lets the rows through that pass its predicates (for example a subquery, or a HAVING).',
		TEMP:   'Writes its input to a temporary table, that is then read (a subquery or a common table expression that is used more than once, or to keep a result for a join).',
		UNION:  'UNION ALL of its inputs (a UNION also has a SORT and a UNIQUE to remove the duplicates).',
		IXAND:  'Index ANDing: the row ids of several index scans are combined (a bitmap), so only rows that match all of them are fetched.',
		RIDSCN: 'Row id scan: the row ids of one or more index scans are sorted (and duplicates removed) before the rows are fetched: list prefetch, or index ORing.',
		INSERT: 'Inserts the rows.',
		UPDATE: 'Updates the rows.',
		DELETE: 'Deletes the rows.',
		CTQ:    'Column-organized (BLU) to row-organized: the rows of a column-organized table are turned into rows for the operators above.',
		TBFUNC: 'A table function.',
		GENROW: 'Generates rows (a VALUES clause, or the rows of an INSERT ... VALUES).',
		TABLE:  'A table, with its statistics from when RUNSTATS was last run (rows, pages). The operator above reads it.',
		INDEX:  'An index, with its statistics from when RUNSTATS was last run. The operator above reads it.'
	};
	function operationDescriptionFor(node) {
		return OPERATION_DESCRIPTIONS[node.op];
	}

	function fmtNum(n) {
		if (n === undefined || n === null || isNaN(n)) return undefined;
		if (n >= 1000) return Math.round(n).toLocaleString();
		return (Math.round(n * 100) / 100).toString();
	}
	function fmtPct(p) {
		if (p === undefined || p === null || isNaN(p)) return undefined;
		return (p >= 10 ? Math.round(p) : Math.round(p * 10) / 10) + '%';
	}

	/** The table or index under the label: "DBXAPP.ORDERS · ORDERS_IX1" */
	function subtitleFor(node) {
		var p = node.props;
		if (p.isLeaf) return p.objName;
		if (!p.objName) return p.keyName;
		return p.objName + (p.keyName ? ' · ' + p.keyName.substring(p.keyName.lastIndexOf('.') + 1) : '');
	}
	function nodeTitle(node) {
		var sub = subtitleFor(node);
		return node.label + (sub && !node.props.isLeaf ? ' ' + sub : '');
	}

	function propRow($into, key, val)  { return DbxShowplanGraph.propRow($into, 'db2-plan', key, val); }
	function propSection($into, title) { return DbxShowplanGraph.propSection($into, 'db2-plan', title); }
	function propSectionIfAny($into, title, fill) {
		var $tmp = $('<div></div>');
		fill($tmp);
		if ($tmp.children().length) propSection($into, title).append($tmp.children());
	}

	// ─────────────────────────────────────────────────────────────────────────
	// Properties pane: everything the plan says about ONE operator
	// ─────────────────────────────────────────────────────────────────────────
	function renderPropertiesInto(container, node) {
		var $c = $(container);
		$c.empty();
		if (!node) {
			$c.append($('<div class="db2-plan-prop-empty"></div>').text('Click an operator in the plan to see all of its properties here.'));
			return;
		}
		var p = node.props, m = node.metrics;

		$c.append($('<div class="db2-plan-prop-title"></div>').text(node.label));
		var sub = subtitleFor(node);
		if (sub) $c.append($('<div class="db2-plan-prop-subtitle"></div>').text(sub));
		var desc = operationDescriptionFor(node);
		if (desc) $c.append($('<div class="db2-plan-prop-desc"></div>').text(desc));
		(node._boxNotes || []).forEach(function (n) {
			$c.append($('<div class="db2-plan-prop-warn"></div>').text('⚠ ' + n.title));
		});

		if (p.isLeaf) {
			var $obj = propSection($c, p.objType === 'IX' ? 'Index' : 'Table');
			propRow($obj, 'Name',        p.objName);
			propRow($obj, 'Rows (statistics)', m.objRows === undefined ? 'not known: no statistics' : fmtNum(m.objRows));
			propRow($obj, 'Pages',       fmtNum(m.objPages));
			propRow($obj, 'Statistics from', p.statsTime || 'never collected (RUNSTATS has not been run)');
			propRow($obj, 'Table space', p.tablespace);
			propRow($obj, 'Rows read by the operator above', fmtNum(m.estRows));
		} else {
			var $op = propSection($c, 'Operator');
			propRow($op, 'Operator number', p.va);
			propRow($op, 'Operator',        node.label);
			propRow($op, 'Table',           p.objName);
			propRow($op, 'Index',           p.keyName);

			propSectionIfAny($c, 'Predicates', function ($s) {
				[['Range / join', p.access], ['Filter', p.filter]].forEach(function (kv) {
					(kv[1] || []).forEach(function (text) { propRow($s, kv[0], text); });
				});
			});

			propSectionIfAny($c, 'Optimizer estimate (cost in timerons)', function ($s) {
				propRow($s, 'Rows out',                 fmtNum(m.estRows));
				propRow($s, 'Cost (with its inputs)',   fmtNum(m.cost));
				propRow($s, 'Cost (own)',               m.ownCost === undefined ? undefined : fmtNum(m.ownCost) + (m.ownCostPct === undefined ? '' : ' = ' + fmtPct(m.ownCostPct) + ' of the statement'));
				propRow($s, 'CPU cost',                 fmtNum(m.cpuCost));
				propRow($s, 'I/O cost',                 fmtNum(m.ioCost));
				propRow($s, 'First row cost',           fmtNum(m.firstRowCost));
				propRow($s, 'Buffers',                  fmtNum(m.buffers));
			});
		}

		// On the statement itself: what is said about the whole plan
		var pi = node._planInfo;
		if (pi) {
			var $pi = propSection($c, 'Plan information');
			propRow($pi, 'Statement type',     pi.statementType);
			propRow($pi, 'Total cost',         fmtNum(pi.totalCost));
			propRow($pi, 'Optimization class', pi.queryOpt);
			propRow($pi, 'Isolation',          pi.isolation);
			propRow($pi, 'Db2 version',        pi.db2Version);
			propRow($pi, 'Explained',          pi.explainTime ? pi.explainTime + (pi.source ? ' (' + pi.source + ')' : '') : undefined);
			propRow($pi, 'Operators',          pi.operations);
			pi.notes.forEach(function (n, i) { propRow($pi, i === 0 ? 'Note' : '', n); });
		}

		// Every argument of the operator, as Db2 wrote it: nothing in the plan is hidden
		if (Object.keys(p.cells).length) {
			var $all = propSection($c, p.isLeaf ? 'More' : 'Arguments (EXPLAIN_ARGUMENT)');
			Object.keys(p.cells).forEach(function (h) { propRow($all, h, p.cells[h]); });
		}
	}

	/** Hover text of the arrow between two boxes: what flows along it (the rows of the CHILD) */
	function connectorTooltip(childNode, parentNode) {
		if (!childNode) return undefined;
		var m = childNode.metrics;
		var lines = [ nodeTitle(childNode) + '  →  ' + (parentNode ? parentNode.label : ''), '' ];
		if (m.estRows !== undefined) lines.push('Estimated rows:   ' + fmtNum(m.estRows));
		return lines.length > 2 ? lines.join('\n') : undefined;
	}

	// ─────────────────────────────────────────────────────────────────────────
	// The hover panel (click = pinned): the short version of the Properties pane
	// ─────────────────────────────────────────────────────────────────────────
	function buildDetailPanel(node) {
		var p = node.props, m = node.metrics;
		var $panel = $('<div class="db2-plan-detail"></div>');

		var description = operationDescriptionFor(node);
		if (description) $panel.append($('<div class="db2-plan-detail-desc"></div>').text(description));

		var $tbl = $('<table></table>');
		function row(key, val, valClass) {
			if (val === undefined || val === null || val === '') return;
			var $val = $('<td></td>').text(val);
			if (valClass) $val.addClass(valClass);
			$tbl.append($('<tr></tr>').append($('<td class="db2-plan-detail-key"></td>').text(key)).append($val));
		}
		if (p.isLeaf) {
			row(p.objType === 'IX' ? 'Index' : 'Table', p.objName);
			row('Rows (statistics)', m.objRows === undefined ? 'no statistics' : fmtNum(m.objRows), p.noStats ? 'db2-plan-detail-pct-warn' : undefined);
			row('Pages',          fmtNum(m.objPages));
			row('Statistics from', p.statsTime || 'never (no RUNSTATS)', p.noStats ? 'db2-plan-detail-pct-warn' : undefined);
		} else {
			row('Operator', node.label + ' (' + p.va + ')');
			row('Table',    p.objName);
			row('Index',    p.keyName);
			row('Rows out', fmtNum(m.estRows));
			row('Cost',     m.cost === undefined ? undefined : fmtNum(m.cost) + (m.ownCostPct === undefined ? '' : '  (own: ' + fmtNum(m.ownCost) + ' = ' + fmtPct(m.ownCostPct) + ')'), m.ownCostPct >= 50 ? 'db2-plan-detail-pct-warn' : undefined);
		}
		$panel.append($tbl);

		// The predicates can be long: full width and wrapping, below the table
		[['Range / join', p.access], ['Filter', p.filter]].forEach(function (kv) {
			(kv[1] || []).forEach(function (text) {
				$panel.append($('<div class="db2-plan-detail-idx-hdr"></div>').text(kv[0] + ':'));
				$panel.append($('<div class="db2-plan-detail-wrap db2-plan-raw-line"></div>').text(text));
			});
		});
		if (p.flags.length) {
			$panel.append($('<div class="db2-plan-detail-idx-hdr"></div>').text('Arguments:'));
			$panel.append($('<div class="db2-plan-detail-wrap"></div>').text(p.flags.join('; ')));
		}
		(node._boxNotes || []).forEach(function (n) {
			$panel.append($('<div class="db2-plan-detail-idx-hdr db2-plan-detail-pct-warn"></div>').text('⚠ ' + n.title));
		});

		$panel.append($('<div class="db2-plan-detail-hint"></div>').text('Click the box to pin this, and to show all properties in the Properties pane.'));
		return $panel;
	}

	// The detail panels are fixed-position overlays on <body>, handled by the shared panel system (dbxShowplanGraph.js)
	var _panels = DbxShowplanGraph.createPanelSystem({
		prefix:     'db2-plan',
		buildPanel: function (node) { return buildDetailPanel(node); }
	});

	// Set by render(), so that a click on a box can show the operator in the Properties pane
	var _propsTarget = null;
	var _selectedBox = null;

	function selectNode($box, node) {
		if (_selectedBox) _selectedBox.removeClass('db2-plan-selected');
		_selectedBox = $box;
		$box.addClass('db2-plan-selected');
		if (_propsTarget) renderPropertiesInto(_propsTarget, node);
	}

	function renderNode(node) {
		var p = node.props, m = node.metrics;
		var $li  = $('<li></li>');
		var $box = $('<div class="db2-plan-box"></div>');
		if (p.isLeaf) $box.addClass('db2-plan-object');
		if ((node._boxNotes || []).some(function (n) { return n.severity !== 'info'; })) $box.addClass('db2-plan-warn');
		$box[0].__db2PlanNode = node;   // for drawConnectorLines(), which walks the DOM and not the model

		// The operator number of Db2 (as db2exfmt shows it): what a "[Node N]" in the Plan Analysis jumps to
		$box.attr('data-va', p.va);
		if (!p.isLeaf)
			$box.append($('<div class="db2-plan-va" title="The operator number (as db2exfmt shows it)"></div>').text(p.va));

		$box.append($('<div class="db2-plan-icon"></div>').attr('style', iconStyleFor(node)));
		$box.append($('<div class="db2-plan-label"></div>').toggleClass('db2-plan-label-long', node.label.length > 24).text(node.label));
		var subtitle = subtitleFor(node);
		if (subtitle && !p.isLeaf) $box.append($('<div class="db2-plan-subtitle"></div>').attr('title', subtitle).text(subtitle));
		if (p.isLeaf && p.objName) $box.append($('<div class="db2-plan-subtitle"></div>').attr('title', p.objName).text(p.objName.substring(0, p.objName.lastIndexOf('.'))));

		if (p.isLeaf) {
			$box.append($('<div class="db2-plan-metric"></div>').toggleClass('db2-plan-warn-text', !!p.noStats)
				.text(m.objRows === undefined ? 'No statistics' : 'Rows ' + fmtNum(m.objRows)));
		} else {
			if (m.estRows !== undefined) $box.append($('<div class="db2-plan-metric"></div>').text('Rows ' + fmtNum(m.estRows)));
			// The cost includes the inputs: the part that is the operator's own is shown too (in red when it is half of the statement or more)
			if (m.cost !== undefined) {
				var $cost = $('<div class="db2-plan-metric"></div>').text('Cost ' + fmtNum(m.cost));
				if (m.ownCostPct !== undefined && m.ownCostPct >= 1 && !node._planInfo) {
					var $own = $('<span></span>').text(' (own ' + fmtPct(m.ownCostPct) + ')');
					if (m.ownCostPct >= 50) $own.addClass('db2-plan-metric-pct-warn');
					$cost.append($own);
				}
				$box.append($cost);
			}
		}

		(node._boxNotes || []).forEach(function (n) {
			$box.append($('<div class="db2-plan-metric db2-plan-note db2-plan-note-' + n.severity + '"></div>')
				.text((n.severity === 'info' ? 'ℹ ' : '⚠ ') + n.boxNote));
		});

		// Hover = the panel as a tooltip. Click = pin it (click again to close), and show the operator in the Properties pane.
		$box.on('mouseenter', function () {
			if (_panels.panelFor($box[0])) return;   // never replace a pinned panel with a tooltip
			_panels.open(node, $box[0], false);
		});
		$box.on('mouseleave', function () {
			if (!_panels.isPinned($box[0])) _panels.close($box[0]);
		});
		$box.on('click', function (e) {
			e.stopPropagation();
			selectNode($box, node);
			if (_panels.isPinned($box[0])) { _panels.close($box[0]); return; }
			_panels.open(node, $box[0], true);
		});

		$li.append($('<div class="db2-plan-box-cell"></div>').append($box));

		if (node.children.length) {
			var $childUl = $('<ul></ul>');
			node.children.forEach(function (child) { $childUl.append(renderNode(child)); });
			$li.append($childUl);
		}
		return $li;
	}

	// The arrows between the boxes: drawConnectorLines() of dbxShowplanMySql.js (and dbxShowplanAse.js, where each step is explained)
	var _arrowMarkerSeq = 0;
	function drawConnectorLines(treeEl, horizontal) {
		var previousSvg = treeEl.querySelector(':scope > svg.db2-plan-connector-svg');
		if (previousSvg) previousSvg.remove();

		var svgNS = 'http://www.w3.org/2000/svg';
		var containerRect = treeEl.getBoundingClientRect();
		var svg = document.createElementNS(svgNS, 'svg');
		svg.setAttribute('class', 'db2-plan-connector-svg');
		svg.setAttribute('width', treeEl.scrollWidth);
		svg.setAttribute('height', treeEl.scrollHeight);

		var markerId = 'db2-plan-arrow-' + (++_arrowMarkerSeq);
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
		arrowHead.style.fill = 'var(--sp-edge)';
		marker.appendChild(arrowHead);
		defs.appendChild(marker);
		svg.appendChild(defs);

		var allBoxRects = Array.prototype.map.call(treeEl.querySelectorAll('.db2-plan-box'), function (b) {
			var r = b.getBoundingClientRect();
			return { left: r.left - containerRect.left, right: r.right - containerRect.left,
			         top: r.top - containerRect.top, bottom: r.bottom - containerRect.top };
		});

		function segCrossesRect(x1, y1, x2, y2, rect) {
			var steps = 20;
			for (var i = 0; i <= steps; i++) {
				var t = i / steps;
				var x = x1 + (x2 - x1) * t, y = y1 + (y2 - y1) * t;
				if (x > rect.left + 1 && x < rect.right - 1 && y > rect.top + 1 && y < rect.bottom - 1) return true;
			}
			return false;
		}

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
			var box = li.querySelector(':scope > .db2-plan-box-cell > .db2-plan-box');
			var parentBox = parentLi.querySelector(':scope > .db2-plan-box-cell > .db2-plan-box');
			if (!box || !parentBox) return;

			var r  = box.getBoundingClientRect();
			var pr = parentBox.getBoundingClientRect();

			var siblingUl = li.parentElement;
			var siblings = siblingUl ? Array.prototype.slice.call(siblingUl.children) : [li];
			var siblingIndex = siblings.indexOf(li);
			var siblingCount = siblings.length;
			var spread = (siblingIndex + 1) / (siblingCount + 1);

			var baseBendFraction = 0.15 + 0.2 * spread;
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
			path.style.stroke = 'var(--sp-edge)';
			path.setAttribute('stroke-width', '1.5');
			path.setAttribute('marker-end', 'url(#' + markerId + ')');
			svg.appendChild(path);

			var tip = connectorTooltip(box.__db2PlanNode, parentBox.__db2PlanNode);
			if (tip) {
				var hit = document.createElementNS(svgNS, 'path');
				hit.setAttribute('d', d);
				hit.setAttribute('fill', 'none');
				hit.setAttribute('stroke', 'transparent');
				hit.setAttribute('stroke-width', '12');
				hit.setAttribute('class', 'db2-plan-connector-hit');
				var title = document.createElementNS(svgNS, 'title');
				title.textContent = tip;
				hit.appendChild(title);
				svg.appendChild(hit);
			}
		});

		treeEl.insertBefore(svg, treeEl.firstChild);
	}

	// ─────────────────────────────────────────────────────────────────────────
	// Plan Analysis: what a DBA would point at when reading this plan.
	//
	// Everything comes from the plan alone (estimates: a section explain has no actual rows), so each finding says
	// what the plan shows and what to check. Returned as the findings array (the same list the other dialogs use).
	// A finding can also put a short note on the box of its operator (node._boxNotes, drawn by renderNode()).
	// ─────────────────────────────────────────────────────────────────────────

	// A scan of fewer rows than this is not worth a word
	var MANY_ROWS = 1000;

	function collectFindings(parsed) {
		var findings = [];
		var root = parsed.statements[0].steps[0].root;
		var pi   = root._planInfo;

		function add(node, severity, category, title, detail, boxNote) {
			findings.push({ severity: severity, category: category, title: title, detail: detail,
			                nodeId: node ? node.props.va : null, nodeName: node ? node.props.objName : undefined });
			if (node && boxNote) (node._boxNotes = node._boxNotes || []).push({ severity: severity, title: title, boxNote: boxNote });
		}
		/** The table a scan reads: its leaf (not a temporary table, which has no leaf of its own) */
		function tableLeafOf(n) {
			for (var i = 0; i < n.children.length; i++) {
				if (n.children[i].props.isLeaf && n.children[i].props.objType === 'TA') return n.children[i];
			}
			return null;
		}

		DbxShowplanGraph.walkPlanNodes(root, function (n) {
			var p = n.props, m = n.metrics;

			// --- A table without statistics: the optimizer guesses its size
			if (p.isLeaf && p.noStats) {
				add(n, 'warning', 'Statistics', 'No statistics on ' + p.objName,
					'RUNSTATS has never been run on the table, so the optimizer uses default numbers for its size and its columns: the plan can be far from good. '
					+ 'RUNSTATS ON TABLE ' + p.objName + ' WITH DISTRIBUTION AND SAMPLED DETAILED INDEXES ALL (or turn on AUTO_RUNSTATS).',
					'no statistics');
				return;
			}
			if (p.isLeaf) return;

			// --- A table scan of many rows
			var leaf = n.op === 'TBSCAN' ? tableLeafOf(n) : null;
			var tableRows = leaf ? (leaf.metrics.objRows !== undefined ? leaf.metrics.objRows : leaf.metrics.estRows) : undefined;
			if (leaf && tableRows >= MANY_ROWS) {
				add(n, 'warning', 'Table scan', 'Table scan of ' + leaf.props.objName + ' (about ' + fmtNum(tableRows) + ' rows)',
					'Every page of the table is read' + (p.filter.length ? ', and the predicate is applied to each row (' + p.filter[0] + ')' : '') + '. '
					+ (m.estRows !== undefined && m.estRows < tableRows / 10 ? 'Only about ' + fmtNum(m.estRows) + ' rows are expected to qualify: an index on the columns of the predicate would read only those. ' : '')
					+ 'See also the Table Information.',
					'table scan');
			}

			// --- A nested loop join with a table scan on the inner side: the table is scanned once per row of the outer side
			if (n.op === 'NLJOIN' && n.children.length >= 2) {
				var inner = n.children[n.children.length - 1];
				var outerRows = n.children[0].metrics.estRows;
				if (inner.op === 'TBSCAN' && tableLeafOf(inner) && outerRows > 1) {
					add(n, 'warning', 'Join', 'Nested loop join with a table scan of ' + tableLeafOf(inner).props.objName + ' on the inner side',
						'The table is scanned once for each of the about ' + fmtNum(outerRows) + ' rows of the outer input. An index on the join columns of ' + tableLeafOf(inner).props.objName + ' lets Db2 look the rows up instead.',
						'scan per outer row');
				}
			}

			// --- An index scan without start / stop keys: the whole index is read
			if (n.op === 'IXSCAN' && !p.access.length && m.estRows >= 10 * MANY_ROWS) {
				add(n, 'info', 'Index scan', 'Full index scan' + (p.keyName ? ' of ' + p.keyName : '') + ', about ' + fmtNum(m.estRows) + ' rows',
					'The index scan has no START or STOP key: the whole index is read (to get the rows in key order without a sort, or because the index has all the columns that are needed).');
			}

			// --- A sort of many rows (it can overflow to the temporary table space)
			if (n.op === 'SORT' && n.children.length && n.children[0].metrics.estRows >= 10 * MANY_ROWS) {
				add(n, 'info', 'Sort', 'Sort of about ' + fmtNum(n.children[0].metrics.estRows) + ' rows',
					'Sorted in SORTHEAP when the rows fit, otherwise it overflows to the temporary table space. An index with the columns of the ORDER BY / GROUP BY (after the columns of an equality predicate) gives the rows in order without a sort.');
			}

			// --- One operator is most of the cost
			if (m.ownCostPct >= 50 && n !== root && !(n._boxNotes || []).length) {
				add(n, 'info', 'Cost', n.label + (p.objName ? ' on ' + p.objName : '') + ' is ' + fmtPct(m.ownCostPct) + ' of the estimated cost',
					'Most of the work of the statement is estimated to be in this operator: start there.',
					fmtPct(m.ownCostPct) + ' of the cost');
			}
		});

		(pi.notes || []).forEach(function (note) {
			add(null, 'info', 'Note', note, 'Db2 says this about the statement.');
		});

		return findings;
	}

	function render(container, parsed, opts) {
		injectStyle();
		var $container = $(container);
		$container.empty();
		// The panels are on <body>, not in the container: one that is pinned would otherwise stay, pointing at a box that is gone
		_panels.closeAll();
		_propsTarget = (opts && opts.propsTarget) || null;
		_selectedBox = null;
		if (_propsTarget) renderPropertiesInto(_propsTarget, null);
		if (!parsed || !parsed.statements || !parsed.statements.length) return;

		var horizontal        = !!(opts && opts.horizontal);
		var useCompactLayout  = !!(opts && opts.layout === 'compact');
		var useLineConnectors = useCompactLayout || !!(opts && opts.connectorStyle === 'lines');

		// Before the boxes are made: a finding can put a note on the box of its operator
		var findings = collectFindings(parsed);

		var $wrap = $('<div class="db2-plan-wrap"></div>');
		var trees = [];
		parsed.statements.forEach(function (stmt) {
			var $tree = $('<div class="db2-plan-tree"></div>');
			if (useCompactLayout)   $tree.addClass(horizontal ? 'db2-plan-compact-h' : 'db2-plan-compact-v');
			else if (horizontal)    $tree.addClass('db2-plan-horizontal');
			if (useLineConnectors)  $tree.addClass('db2-plan-lines');
			$tree.append($('<ul></ul>').append(renderNode(stmt.steps[0].root)));
			$wrap.append($tree);
			trees.push($tree);
		});

		// A click on the empty part of the diagram closes the open panels
		$wrap.on('click', function () { _panels.closeAll(); });
		$container.append($wrap);

		if (opts && opts.onFindingsChanged) opts.onFindingsChanged(findings);

		// Both need the real positions of the boxes, so the tree has to be in the document first
		trees.forEach(function ($tree) {
			if (useCompactLayout) {
				if (horizontal) DbxShowplanGraph.tuckLeavesNearParent($tree[0], 'db2-plan');
				else            DbxShowplanGraph.tuckLeavesNearParentVertical($tree[0], 'db2-plan');
			}
			if (useLineConnectors) drawConnectorLines($tree[0], horizontal);
		});
	}

	return {
		parseText: parseText,
		render: render,
		renderPropertiesInto: renderPropertiesInto,
		// The detail panels are on <body>: a caller that removes the diagram (and the Escape handler) has to be able to close them
		closePanels: function () { _panels.closeAll(false); },
		anyPanelsOpen: function () { return _panels.anyOpen(); },
		getLastParseError: function () { return lastParseError; }
	};
})();
