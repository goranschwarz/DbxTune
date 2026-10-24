/**
 * dbxShowplanDb2.js — Db2 (LUW) graphical execution plan renderer
 *
 * Parses a Db2 plan into the same tree model the other renderers use, and renders it as the same CSS/DOM box-and-connector
 * diagram (built on dbxShowplanMySql.js / dbxShowplanOracle.js). It is shown by the /showplan/db2 page (ShowplanDb2Servlet).
 * Two formats are read:
 *   - The JSON that Db2Tune records (CmActiveStatements, column 'EXEC_PLAN': the explain tables after EXPLAIN_FROM_SECTION,
 *     see Db2ExplainUtils.java, "planFormat": "db2-explain-tables-1").
 *   - The text of db2exfmt (what a DBA has, and what is pasted into tickets): the 'Access Plan' tree, and when they are there
 *     'Plan Details' (costs, arguments, predicates), 'Objects Used in Access Plan' (statistics), the statements and Db2's own
 *     diagnostics (EXP0256I index advice...). Also with section actuals ('Rows Actual'). See parseExfmtPlan().
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

	/** A box for the second reader of a shared result: the operator, without its inputs (they are drawn under the first reader) */
	function sharedRef(n) {
		var r = newNode(n.op, n.label);
		r.props.va      = n.props.va;
		r.props.isRef   = true;
		r.props.objName = n.props.objName;
		r.metrics.cost  = n.metrics.cost;
		return r;
	}

	// EXPLAIN_PREDICATE.HOW_APPLIED: START / STOP = the range of an index scan (the "seek"), JOIN = the join condition,
	// SARG / RESID / PREDICATE... = a filter on the rows that are read
	var ACCESS_HOW = { START: true, STOP: true, JOIN: true };

	// The arguments that say something worth a line in the box panel (EXPLAIN_ARGUMENT.ARGUMENT_TYPE)
	var FLAG_ARGS = {
		SCANDIR: 'Scan direction', PREFETCH: 'Prefetch', MAXPAGES: 'Max pages', TABLOCK: 'Table lock', ROWLOCK: 'Row lock',
		'JN INPUT': 'Join input', EARLYOUT: 'Early out', FETCHMAX: 'Fetch max', SORTKEY: 'Sort key', GROUPBYC: 'Group by columns',
		GROUPBYN: 'Group by columns', UNIQUE: 'Unique', TEMPSIZE: 'Temp size', SPILLED: 'Spilled', OUTERJN: 'Outer join',
		ISCANMAX: 'Index scan max', JN_TYPE: 'Join type', HASHCODE: 'Hash code', BITFLTR: 'Bit filter'
	};

	/** The JSON of Db2Tune, or the text of db2exfmt: both become the same tree */
	function parseText(planText) {
		lastParseError = null;
		if (!planText || !String(planText).trim()) { lastParseError = 'The plan is empty.'; return null; }

		var text = String(planText).replace(/\r\n?/g, '\n');
		var root = text.trim().charAt(0) === '{' ? parseJsonPlan(text) : parseExfmtPlan(text);
		if (!root) return null;

		finishTree(root);
		return { format: root._planInfo.format, rawText: planText, statements: [ { label: 'Plan', meta: {}, steps: [ { label: '', root: root } ] } ] };
	}

	/**
	 * What is computed the same way for both formats: the part of the cost that is each operator's own (Db2's cost includes
	 * its inputs), its share of the statement, the number of operators, and a number for the tables and indexes
	 * (they have no number of their own: it is what a finding's "[Node N]" jumps to)
	 */
	function finishTree(root) {
		DbxShowplanGraph.walkPlanNodes(root, function (n) {
			if (n.props.isLeaf || n.metrics.cost === undefined) return;
			var childCost = 0;
			n.children.forEach(function (c) { if (!c.props.isLeaf && c.metrics.cost !== undefined) childCost += c.metrics.cost; });
			n.metrics.ownCost = Math.max(0, n.metrics.cost - childCost);
		});
		var total = root._planInfo.totalCost !== undefined ? root._planInfo.totalCost : root.metrics.cost;
		var operations = 0, maxId = 0;
		DbxShowplanGraph.walkPlanNodes(root, function (n) {
			if (n.props.isLeaf) return;
			operations++;
			maxId = Math.max(maxId, n.props.va || 0);
			if (n.metrics.ownCost !== undefined && total > 0 && n !== root) n.metrics.ownCostPct = n.metrics.ownCost * 100 / total;
		});
		DbxShowplanGraph.walkPlanNodes(root, function (n) { if (n.props.isLeaf) n.props.va = ++maxId; });
		root._planInfo.totalCost  = total;
		root._planInfo.operations = operations;
	}

	// ---- The JSON of Db2Tune ------------------------------------------------

	function parseJsonPlan(text) {
		var start = text.indexOf('{'), end = text.lastIndexOf('}');
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
				// A second reader of a shared result (a TEMP that is read twice): its subtree is drawn once, this one is a reference box (as db2exfmt draws it)
				if (feedsOther[s.sourceId]) child = sharedRef(child);
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

		var notes = [];
		if (json.isolation === 'RR') notes.push('Isolation RR (repeatable read): every row that is read is locked until the end of the transaction, also the rows that do not qualify.');

		root._planInfo = {
			format:        'json',
			sqlText:       json.statementText,
			statementType: json.statementType,
			totalCost:     num(json.totalCost),
			queryOpt:      json.queryOpt,
			isolation:     json.isolation,
			db2Version:    json.db2Version,
			explainTime:   json.explainTime,
			source:        json.source,
			notes:         notes,
			sections:      {}
		};
		return root;
	}

	// ---- The text of db2exfmt -------------------------------------------------
	//
	//                   Rows                       <- the legend (the first operator, RETURN, shows the names of the values)
	//                  RETURN
	//                  (   1)
	//                   Cost
	//                    I/O
	//                    |
	//                  1043.49                     <- rows out (with section actuals a second line: the actual rows)
	//                  HSJOIN^                     <- the operator, with the join type marks of the "Operator Symbols" legend
	//                  (   5)                      <- the operator number
	//                  547399                      <- cumulative cost (timerons)
	//                  490798                      <- cumulative I/O
	//         /------------+------------\          <- the inputs: '|' one, '/--+--\' two or more (the outer input to the left)
	//       25                          1043.49
	//     TBSCAN                        HSJOIN^
	//     (   6)  ...                   ...
	//       |
	//       25                                     <- a table or an index: its rows (statistics)
	// TABLE: TPCH                                  <- the type and the schema
	//     NATION                                   <- the name
	//       Q7                                     <- the correlation name in the optimized statement
	//
	// The boxes are found by their position: an operator by its '(  N)', the values by the token straight above / below it.
	// A box's parent is found by following the connector above it ('|', or the '/--+--\' line) to the box above it.
	// "Plan Details", "Objects Used in Access Plan", the statements and the diagnostics add to that, when they are in the text.

	// An operator, with the join type marks of db2exfmt: >JOIN left outer, JOIN< right outer, >JOIN< full outer,
	// xJOIN / JOINx left / right antijoin, ^JOIN / JOIN^ left / right early out. They combine: x^NLJOIN = left antijoin with early out.
	// And pGRPBY = a partial (early) aggregation
	var EXFMT_OPERATOR = /^([>^a-z]*)([A-Z][A-Z0-9]*?)([<x^]*)$/;

	function joinTypeText(pre, post) {
		var t = [];
		if (pre.indexOf('p') >= 0) t.push('Partial aggregation (an early step: the groups are completed further up)');
		if (pre.indexOf('>') >= 0 && post.indexOf('<') >= 0) t.push('Full outer join');
		else if (pre.indexOf('>') >= 0)                      t.push('Left outer join');
		else if (post.indexOf('<') >= 0)                     t.push('Right outer join');
		if (pre.indexOf('x') >= 0)  t.push('Left antijoin (NOT EXISTS / NOT IN)');
		if (post.indexOf('x') >= 0) t.push('Right antijoin');
		if (pre.indexOf('^') >= 0)  t.push('Left early out (the first match of a row is enough: EXISTS / IN / DISTINCT)');
		if (post.indexOf('^') >= 0) t.push('Right early out');
		return t.length ? t.join(', ') : undefined;
	}

	/** The whitespace delimited token of a line that is nearest to column 'c' (it must start or end within 'maxDist'), or null */
	function tokenAt(line, c, maxDist) {
		if (line === undefined) return null;
		var best = null, re = /\S+/g, m;
		while ((m = re.exec(line)) !== null) {
			var s = m.index, e = m.index + m[0].length - 1;
			var dist = c >= s && c <= e ? 0 : Math.min(Math.abs(c - s), Math.abs(c - e));
			if (dist <= (maxDist === undefined ? 3 : maxDist) && (!best || dist < best.dist))
				best = { text: m[0], start: s, end: e, center: (s + e) / 2, dist: dist };
		}
		return best;
	}

	/** "1043.49" / "2e+06" -> number, "NA" or not a number -> undefined (also for negative numbers: -1 = not known) */
	function exfmtNum(token) {
		if (!token || !/^[-+]?[0-9.]+(e[-+]?[0-9]+)?$/i.test(token.text)) return undefined;
		return num(token.text);
	}

	/** The lines after a heading and its '-----' line, until the next heading (a line that ends with ':' and starts at column 0) */
	function exfmtSection(text, heading) {
		// NOTE: (?![\s\S]) = the end of the text ('$' is the end of every line with the flag 'm')
		var m = new RegExp('^' + heading + ':?[ \\t]*\\n-+\\n([\\s\\S]*?)(?=\\n[A-Z][A-Za-z ]*:[ \\t]*\\n-+\\n|\\n\\*{5,}|\\n-{10,} STATEMENT|(?![\\s\\S]))', 'm').exec(text);
		return m ? m[1].replace(/^\s*\n/, '').replace(/\s+$/, '') : undefined;
	}

	function parseExfmtPlan(text) {
		var lines = text.split('\n');

		// The tree: after 'Access Plan:' (a pasted tree can be without it) until the next section
		var first = 0;
		for (var i = 0; i < lines.length; i++) {
			if (/^\s*Access Plan:/.test(lines[i])) { first = i + 1; break; }
		}
		var last = lines.length - 1;
		for (var j = first; j < lines.length; j++) {
			if (/^\s*(Operator Symbols|Extended Diagnostic Information|Plan Details|Objects Used in Access Plan)\s*:?/.test(lines[j])) { last = j - 1; break; }
		}
		var actuals = false;
		for (var k = first; k <= last; k++) {
			if (/Rows Actual/.test(lines[k])) actuals = true;
		}

		// The operators: '(  N)' with the operator name straight above it
		var boxes = [];
		for (var li = first; li <= last; li++) {
			var re = /\(\s*(\d+)\)/g, m;
			while ((m = re.exec(lines[li])) !== null) {
				var c     = m.index + (m[0].length - 1) / 2;
				var nameT = tokenAt(lines[li - 1], c, 2);
				var opM   = nameT ? EXFMT_OPERATOR.exec(nameT.text) : null;
				if (!opM) continue;
				var rowsT = tokenAt(lines[li - (actuals ? 3 : 2)], c, 4);
				boxes.push({
					kind:     'op',
					id:       parseInt(m[1], 10),
					name:     nameT.text, op: opM[2], joinType: joinTypeText(opM[1], opM[3]),
					center:   nameT.center,
					top:      li - (actuals ? 3 : 2),
					bottom:   li + 2,
					rows:     exfmtNum(rowsT),
					actRows:  actuals ? exfmtNum(tokenAt(lines[li - 2], c, 4)) : undefined,
					cost:     exfmtNum(tokenAt(lines[li + 1], c, 4)),
					io:       exfmtNum(tokenAt(lines[li + 2], c, 4)),
					children: []
				});
			}
			// The tables and indexes: 'TABLE: SCHEMA', the name below it, and the correlation name below that
			var reO = /\b([A-Z][A-Z0-9]*(?:-[A-Z]+)?): +(\S+)/g, mo;
			while ((mo = reO.exec(lines[li])) !== null) {
				var oc    = mo.index + (mo[0].length - 1) / 2;
				var nameO = tokenAt(lines[li + 1], oc, 6);
				if (!nameO) continue;
				var corr  = tokenAt(lines[li + 2], oc, 6);
				var hasCorr = corr && /^Q\d+$/.test(corr.text);
				boxes.push({
					kind:     'obj',
					objType:  mo[1],
					schema:   mo[2],
					name:     nameO.text,
					corr:     hasCorr ? corr.text : undefined,
					center:   oc,
					top:      li - (actuals ? 2 : 1),
					bottom:   li + (hasCorr ? 2 : 1),
					rows:     exfmtNum(tokenAt(lines[li - (actuals ? 2 : 1)], oc, 6)),
					children: []
				});
			}
		}
		var ops = boxes.filter(function (b) { return b.kind === 'op'; });
		if (!ops.length) {
			lastParseError = 'No plan was found: it is neither the JSON that Db2Tune records, nor the text of db2exfmt (the tree with "(   1)" under RETURN).';
			return null;
		}

		// The parent of every box: follow the connector above it to the box above it
		boxes.forEach(function (b) {
			var conn = lines[b.top - 1];
			if (conn === undefined) return;
			var pos = -1;
			for (var d = 0; d <= 3 && pos < 0; d++) {
				[Math.round(b.center) - d, Math.round(b.center) + d].forEach(function (x) { if (pos < 0 && '|/\\+'.indexOf(conn.charAt(x)) >= 0) pos = x; });
			}
			if (pos < 0) return;

			// '|' = straight up. '/', '\' or '+' = the '/---+---\' line: the parent is above the stretch of it
			var from = pos, to = pos;
			if (conn.charAt(pos) !== '|') {
				while (from > 0 && '-+/\\'.indexOf(conn.charAt(from - 1)) >= 0) from--;
				while (to < conn.length - 1 && '-+/\\'.indexOf(conn.charAt(to + 1)) >= 0) to++;
			}
			var plus = conn.indexOf('+', from);
			var mid  = conn.charAt(pos) === '|' ? pos : (plus >= 0 && plus <= to ? plus : (from + to) / 2);

			var parent = null;
			ops.forEach(function (p) {
				if (p === b || p.bottom !== b.top - 2) return;
				if (p.center < from - 4 || p.center > to + 4) return;
				if (!parent || Math.abs(p.center - mid) < Math.abs(parent.center - mid)) parent = p;
			});
			if (parent) {
				parent.children.push(b);
				b.hasParent = true;
			}
		});

		// The details of the operators ("Plan Details") and of the tables and indexes ("Objects Used in Access Plan")
		var details = parseExfmtPlanDetails(text);
		var objects = parseExfmtObjects(text);

		// NOTE: db2exfmt draws a shared result (a TEMP read twice) a second time with the same number, without its inputs: that box is a reference (isRef)
		function toNode(b) {
			var node;
			if (b.kind === 'obj') {
				var isIndex = /INDEX/.test(b.objType);
				var full    = b.schema + '.' + b.name;
				var od      = objects[full] || {};
				node = newNode(isIndex ? 'INDEX' : 'TABLE', b.name);
				node.props.objName    = full;
				node.props.objType    = isIndex ? 'IX' : 'TA';
				node.props.isLeaf     = true;
				node.props.statsTime  = od.statsTime;
				node.props.tablespace = od.tablespace;
				node.props.noStats    = od.found && !isIndex && !od.statsTime;
				node.metrics.objRows  = od.rows !== undefined ? od.rows : b.rows;   // the tree shows it rounded (5.99861e+07)
				node.metrics.objPages = od.pages;
				if (b.objType !== 'TABLE' && b.objType !== 'INDEX') node.props.cells['Object type'] = b.objType;   // CO-TABLE (column-organized), NNAME (nickname), TFUNC...
				node.props.columnOrganized = /^CO-/.test(b.objType);
				if (b.corr) node.props.cells['Correlation name'] = b.corr;
				Object.keys(od.cells || {}).forEach(function (k) { node.props.cells[k] = od.cells[k]; });
				return node;
			}

			var dt = details[b.id] || {};
			node = newNode(b.op, b.name);
			var p = node.props, m = node.metrics;
			p.va           = b.id;
			p.isRef        = !b.children.length && ops.some(function (o) { return o !== b && o.id === b.id && o.children.length; });
			p.joinType     = b.joinType;
			m.estRows      = b.rows;
			m.actRows      = b.actRows;
			m.cost         = b.cost !== undefined ? b.cost : dt.cost;
			m.ioCost       = b.io !== undefined ? b.io : dt.ioCost;
			m.cpuCost      = dt.cpuCost;
			m.firstRowCost = dt.firstRowCost;
			m.buffers      = dt.buffers;
			if (b.joinType) p.flags.push('Join type: ' + b.joinType);
			(dt.args || []).forEach(function (a) {
				p.cells[a.key] = p.cells[a.key] ? p.cells[a.key] + ', ' + a.value : a.value;
				if (FLAG_ARGS[a.key]) p.flags.push(FLAG_ARGS[a.key] + ': ' + a.value);
			});
			(dt.predicates || []).forEach(function (pr) { (pr.isAccess ? p.access : p.filter).push(pr.line); });

			// The inputs from left to right (the outer input of a join is to the left)
			b.children.sort(function (x, y) { return x.center - y.center; }).forEach(function (cb) {
				var child = toNode(cb);
				node.children.push(child);
				if (child.props.isLeaf && child.props.objType === 'IX') p.keyName = child.props.objName;
				else if (child.props.isLeaf)                           p.objName = child.props.objName;
			});
			return node;
		}

		var top = ops.filter(function (b) { return b.id === 1; })[0] || ops.filter(function (b) { return !b.hasParent; })[0];
		var root = toNode(top);

		// The rest of the text
		var totalCost = /Total Cost:\s*([0-9.e+\-]+)/i.exec(text);
		var degree    = /Query Degree:\s*(-?\d+)/i.exec(text);
		var original  = exfmtSection(text, 'Original Statement');
		var optimized = exfmtSection(text, 'Optimized Statement');
		var version   = /DB2_VERSION:\s*(\S+)/.exec(text);
		var explTime  = /EXPLAIN_TIME:\s*(\S+)/.exec(text);
		var isolation = /Isolation Level:\s*(.+)/.exec(text);
		var optLevel  = /Optimization Level:\s*(\d+)/.exec(text);
		var level     = /Explain level:\s*(.+)/.exec(text);

		// The advice of Db2 itself (EXP0256I: an index for a zigzag join, EXP0021W: no statistics...)
		var notes = [];
		var diagRe = /Diagnostic Details:\s*([\s\S]*?)(?=\n\s*\n|\nDiagnostic Identifier:|$)/g, dm;
		while ((dm = diagRe.exec(text)) !== null)
			notes.push(dm[1].replace(/\s+/g, ' ').trim());
		if (isolation && /Repeatable Read/i.test(isolation[1]))
			notes.push('Isolation RR (repeatable read): every row that is read is locked until the end of the transaction, also the rows that do not qualify.');

		root._planInfo = {
			format:        'db2exfmt',
			sqlText:       original || optimized,
			optimizedText: optimized,
			totalCost:     totalCost ? num(totalCost[1]) : undefined,
			queryDegree:   degree ? degree[1] : undefined,
			queryOpt:      optLevel ? optLevel[1] : undefined,
			isolation:     isolation ? isolation[1].trim() : undefined,
			db2Version:    version ? version[1] : undefined,
			explainTime:   explTime ? explTime[1] : undefined,
			source:        level ? level[1].trim() : 'db2exfmt',
			hasActuals:    actuals,
			notes:         notes,
			sections:      {}
		};
		return root;
	}

	/**
	 * "Plan Details": per operator its costs, arguments and predicates. Key: the operator number
	 *	5) HSJOIN: (Hash Join)
	 *		Cumulative Total Cost: 		547399
	 *		...
	 *		Arguments:                       Predicates:
	 *		---------                        ----------
	 *		BITFLTR : (Hash Join Bit Filter used)      13) Predicate used in Join,
	 *			FALSE                            ...
	 *			                                 Predicate Text:
	 *			                                 --------------
	 *			                                 (Q4.O_ORDERKEY = Q3.L_ORDERKEY)
	 */
	function parseExfmtPlanDetails(text) {
		var result = {};
		var start = text.search(/^Plan Details:/m);
		if (start < 0) return result;
		var end = text.search(/^Objects Used in Access Plan:/m);
		var body = text.substring(start, end > start ? end : text.length);

		var heads = [], re = /^\s*(\d+)\)\s+([>^a-z]*[A-Z][A-Z0-9]*[<x^]*)\s*:\s*\((.*)\)\s*$/gm, m;
		while ((m = re.exec(body)) !== null) heads.push({ id: parseInt(m[1], 10), at: m.index, after: re.lastIndex });
		heads.forEach(function (h, i) {
			var block = body.substring(h.after, i + 1 < heads.length ? heads[i + 1].at : body.length);
			var d = { args: [], predicates: [] };
			var v = function (label) { var x = new RegExp(label + ':\\s*([-0-9.e+]+)', 'i').exec(block); return x ? num(x[1]) : undefined; };
			d.cost         = v('Cumulative Total Cost');
			d.cpuCost      = v('Cumulative CPU Cost');
			d.ioCost       = v('Cumulative I/O Cost');
			d.firstRowCost = v('Cumulative First Row Cost');
			d.buffers      = v('Estimated Bufferpool Buffers');

			// Arguments: 'NAME : (description)' and the value on the lines below it
			var argPart = /Arguments:\s*\n\s*-+\n([\s\S]*?)(?=\n\s*(Predicates|Input Streams|Output Streams):|$)/.exec(block);
			if (argPart) {
				var cur = null;
				argPart[1].split('\n').forEach(function (l) {
					var am = /^\s*([A-Z][A-Z0-9_ #]*?)\s*:\s*\((.*)\)\s*$/.exec(l);
					if (am) { cur = { key: am[1].trim(), value: '' }; d.args.push(cur); }
					else if (cur && l.trim()) cur.value += (cur.value ? ' ' : '') + l.trim();
				});
			}

			// Predicates: 'N) <kind> Predicate,' ... 'Filter Factor:' ... 'Predicate Text:' '-----' <text until a blank line>
			var predRe = /^\s*\d+\)\s+(.*?)\s*Predicate,?\s*$([\s\S]*?)Predicate Text:\s*\n\s*-+\n([\s\S]*?)(?=\n\s*\n|(?![\s\S]))/gm, pm;
			while ((pm = predRe.exec(block)) !== null) {
				var kind  = pm[1].replace(/\s+used in$/i, '').trim() || 'Predicate';
				var ff    = /Filter Factor:\s*([-0-9.e+]+)/.exec(pm[2]);
				var ptext = pm[3].replace(/\s+/g, ' ').trim();
				d.predicates.push({
					isAccess: /Start Key|Stop Key|Join/i.test(kind),
					line:     kind + ': ' + ptext + (ff ? '   (filter factor ' + fmtNum(parseFloat(ff[1])) + ')' : '')
				});
			}
			result[h.id] = d;
		});
		return result;
	}

	/** "Objects Used in Access Plan": the statistics of the tables and indexes. Key: schema.name */
	function parseExfmtObjects(text) {
		var result = {};
		var start = text.search(/^Objects Used in Access Plan:/m);
		if (start < 0) return result;
		var body = text.substring(start);
		var re = /^\s*Schema:\s*(\S+)\s*\n\s*Name:\s*(\S+)\s*\n\s*Type:\s*(.+)\n([\s\S]*?)(?=\n\s*Schema:|(?![\s\S]))/gm, m;
		while ((m = re.exec(body)) !== null) {
			var block = m[4];
			var get = function (label) { var x = new RegExp('^\\s*' + label + ':[ \\t]*(.*)$', 'mi').exec(block); return x ? x[1].trim() : undefined; };
			var o = { found: true, cells: {} };
			o.statsTime  = get('Last statistics update') || undefined;
			o.tablespace = get('Tablespace name');
			o.pages      = num(get('Number of buffer pool pages'));
			o.rows       = num(get('Number of rows'));
			o.cells['Type'] = m[3].trim();
			[['Number of rows', 'Rows (statistics)'], ['Index leaf pages', 'Leaf pages'], ['Index tree levels', 'Levels'],
			 ['Index full key cardinality', 'Full key distinct'], ['Index clustering statistic', 'Cluster ratio'],
			 ['Base Table Schema', 'Base table schema'], ['Base Table Name', 'Base table']].forEach(function (kv) {
				var val = get(kv[0]);
				if (val !== undefined && val !== '' && val !== '-1') o.cells[kv[1]] = val;
			});
			var cols = /Columns in index:\s*\n([\s\S]*?)(?=\n\s*[A-Z][a-z].*:|$)/.exec(block);
			if (cols) o.cells['Index columns'] = cols[1].split('\n').map(function (s) { return s.trim(); }).filter(Boolean).join(', ');
			result[m[1] + '.' + m[2]] = o;
		}
		return result;
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
		// The second reader of a shared result: a reference to the box where it is computed
		+ '.db2-plan-box.db2-plan-ref { border-style: dotted; opacity: 0.8; }'
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
		if (n > 0 && n < 0.01) return n.toPrecision(2);   // 2.08464e-05 rows is not 0 (an estimate below 1 row: the optimizer expects almost nothing)
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
			propRow($op, 'Join type',       p.joinType);
			propRow($op, 'Table',           p.objName);
			propRow($op, 'Index',           p.keyName);

			propSectionIfAny($c, 'Predicates', function ($s) {
				[['Range / join', p.access], ['Filter', p.filter]].forEach(function (kv) {
					(kv[1] || []).forEach(function (text) { propRow($s, kv[0], text); });
				});
			});

			propSectionIfAny($c, 'Optimizer estimate (cost in timerons)', function ($s) {
				propRow($s, 'Rows out',                 fmtNum(m.estRows));
				propRow($s, 'Rows out (actual)',        fmtNum(m.actRows));
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
			propRow($pi, 'Plan format',        pi.format === 'db2exfmt' ? 'the text of db2exfmt' : 'Db2Tune (explain tables as JSON)');
			propRow($pi, 'Query degree',       pi.queryDegree);
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
			row('Join type', p.joinType);
			row('Table',    p.objName);
			row('Index',    p.keyName);
			row('Rows out', fmtNum(m.estRows));
			row('Actual rows', fmtNum(m.actRows));
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
		if (p.isRef)  $box.addClass('db2-plan-ref').attr('title', 'A shared result: it is computed once, under the other box with number ' + p.va + ', and read here again');
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
			// With section actuals (db2exfmt of EXPLAIN_FROM_ACTIVITY) also what really came out of the operator
			if (m.actRows !== undefined)      $box.append($('<div class="db2-plan-metric"></div>').text('Rows ' + (m.estRows === undefined ? '?' : fmtNum(m.estRows)) + ' / act ' + fmtNum(m.actRows)));
			else if (m.estRows !== undefined) $box.append($('<div class="db2-plan-metric"></div>').text('Rows ' + fmtNum(m.estRows)));
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

		if (p.isRef)
			$box.append($('<div class="db2-plan-metric db2-plan-note db2-plan-note-info"></div>').text('shared: see the other (' + p.va + ')'));

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
			if (p.isRef) return;   // the second reader of a shared result: its findings are on the first one

			// --- A table without statistics: the optimizer guesses its size
			if (p.isLeaf && p.noStats) {
				add(n, 'warning', 'Statistics', 'No statistics on ' + p.objName,
					'RUNSTATS has never been run on the table, so the optimizer uses default numbers for its size and its columns: the plan can be far from good. '
					+ 'RUNSTATS ON TABLE ' + p.objName + ' WITH DISTRIBUTION AND SAMPLED DETAILED INDEXES ALL (or turn on AUTO_RUNSTATS).',
					'no statistics');
				return;
			}
			if (p.isLeaf) return;

			// --- A table scan of many rows (not of a column-organized table, BLU: there a scan is the way it is read, only the columns that are needed)
			var leaf = n.op === 'TBSCAN' ? tableLeafOf(n) : null;
			if (leaf && leaf.props.columnOrganized) leaf = null;
			var tableRows = leaf ? (leaf.metrics.objRows !== undefined ? leaf.metrics.objRows : leaf.metrics.estRows) : undefined;
			if (leaf && tableRows >= MANY_ROWS) {
				add(n, 'warning', 'Table scan', 'Table scan of ' + leaf.props.objName + ' (about ' + fmtNum(tableRows) + ' rows)',
					'Every page of the table is read' + (p.filter.length ? ', and the predicate is applied to each row (' + p.filter[0] + ')' : '') + '. '
					+ (m.estRows !== undefined && m.estRows < tableRows / 10 ? 'Only about ' + fmtNum(m.estRows) + ' rows are expected to qualify: an index on the columns of the predicate would read only those.' : ''),
					'table scan');
			}

			// --- A nested loop join with a table scan on the inner side: the table is scanned once per row of the outer side
			// (the argument 'JN INPUT' of the input says OUTER or INNER, db2exfmt draws the outer one to the left)
			if (n.op === 'NLJOIN' && n.children.length >= 2) {
				var inner = n.children.filter(function (c) { return c.props.cells['JN INPUT'] === 'INNER'; })[0] || n.children[n.children.length - 1];
				var outer = n.children.filter(function (c) { return c !== inner && !c.props.isLeaf; })[0] || n.children[0];
				var outerRows = outer.metrics.estRows;
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
			// (not the sort of row ids below a RIDSCN: that is list prefetch, it sorts the row ids to read the pages in order)
			if (n.op === 'RIDSCN')
				n.children.forEach(function (c) { if (c.op === 'SORT') c._isRidSort = true; });
			if (n.op === 'SORT' && !n._isRidSort && n.children.length && n.children[0].metrics.estRows >= 10 * MANY_ROWS) {
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
