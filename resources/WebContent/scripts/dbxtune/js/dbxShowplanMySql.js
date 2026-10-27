/**
 * dbxShowplanMySql.js — MySQL graphical execution plan renderer
 *
 * Parses what EXPLAIN FORMAT=JSON prints into the same tree model the other renderers use, and renders it as the same
 * CSS/DOM box-and-connector diagram (built on dbxShowplanOracle.js). It is shown in the Oracle plan dialog of dbxShowplan.js,
 * which takes the renderer from the vendor of the plan (MySqlShowplan for 'MySQL').
 *
 * MySQL has two JSON formats, both are read:
 *   - Format 1, the default on all versions (explain_json_format_version=1): nested "query_block"s, with the tables of a join
 *     in a "nested_loop" list. This is what MySqlTune records (CmActiveStatements: EXPLAIN FORMAT=JSON FOR CONNECTION, column 'ExecPlan').
 *     A table is named by its ALIAS in the statement (the real names come from the SQL text, see the dialog).
 *   - Format 2, MySQL 8.3 and above (explain_json_format_version=2): the tree of iterators, the same tree as EXPLAIN FORMAT=TREE,
 *     with the real table and schema names. EXPLAIN ANALYZE FORMAT=JSON also has what really happened (actual_rows, actual_loops...).
 *
 * Entry points (the same as OracleShowplan):
 *   var parsed = MySqlShowplan.parseText(planText);
 *   MySqlShowplan.render(containerEl, parsed, opts);
 *
 * parseText() returns null when the text is not a plan it can draw (the reason is available via getLastParseError()),
 * so the caller can fall back to showing the text as it is.
 *
 * Node model (the same shape as the other renderers):
 *   {
 *     op:       'Table scan',               // what the operation is (the icons and the descriptions are chosen on it)
 *     label:    'Table scan',
 *     metrics:  { estRows, rowsProduced, filteredPct, cost, ownCost, ownCostPct,            // the optimizer's estimate
 *                 starts, actRows, estRowsTotal, actTimeSec },                              // only with EXPLAIN ANALYZE (format 2)
 *     props:    { va (= the number of the box), objName, tableName (schema.table, format 2), alias, keyName, accessType,
 *                 access[], filter[], flags[], possibleKeys, joinBuffer, dependent, heading, cells{} (every field of the JSON object) },
 *     children: [ node, ... ]
 *   }
 * Top-level result:
 *   { format: 'json', rawText, statements: [ { label, meta, steps: [ { label, root: node } ] } ] }
 * The root node also has: _planInfo { explainFormat, sqlText, operations, hasActuals, notes[], sections{} }
 *
 * The layout CSS and the connector drawing (drawConnectorLines) are a copy of dbxShowplanOracle.js, with the class prefix
 * changed to 'my-plan' ("copy now, extract later", as the Oracle and SQL Server renderers were made).
 * The layout plumbing that is already shared lives in dbxShowplanGraph.js, loaded BEFORE this file.
 */

window.MySqlShowplan = (function () {

	// Why the most recent parseText() returned null
	var lastParseError = null;

	// ─────────────────────────────────────────────────────────────────────────
	// Parser
	// ─────────────────────────────────────────────────────────────────────────

	/** "39700.75" -> 39700.75, 5856.0 -> 5856, "" / undefined -> undefined */
	function num(v) {
		if (v === undefined || v === null || v === '') return undefined;
		var n = parseFloat(v);
		return isNaN(n) ? undefined : n;
	}

	function newNode(op, label) {
		return { op: op, label: label || op, metrics: {}, props: { access: [], filter: [], flags: [], cells: {} }, children: [] };
	}

	/** Every field of the JSON object, as text (objects are flattened: 'cost_info.read_cost'), for "the JSON object" section of the Properties pane */
	function fillCells(cells, obj, prefix, skip) {
		Object.keys(obj).forEach(function (k) {
			if (skip && skip.indexOf(k) !== -1) return;
			var v = obj[k];
			if (v === null || v === undefined) return;
			if (Array.isArray(v)) {
				if (v.length && typeof v[0] === 'object') return;   // children (a list of tables or subqueries): they are boxes of their own
				cells[prefix + k] = v.join(', ');
			} else if (typeof v === 'object') {
				fillCells(cells, v, prefix + k + '.', skip);
			} else {
				cells[prefix + k] = String(v);
			}
		});
	}

	// ---- Format 1 -----------------------------------------------------------

	// access_type -> what it is called in the diagram
	var V1_ACCESS = {
		'ALL':             'Table scan',
		'index':           'Full index scan',
		'range':           'Index range scan',
		'ref':             'Index lookup',
		'eq_ref':          'Unique index lookup',
		'ref_or_null':     'Index lookup (or NULL)',
		'const':           'Single row (const)',
		'system':          'Single row (system)',
		'index_merge':     'Index merge',
		'fulltext':        'Fulltext search',
		'unique_subquery': 'Unique subquery lookup',
		'index_subquery':  'Subquery index lookup'
	};

	// The operations that wrap the rest of a query block: {the key in the JSON, op, label}. In this order, outermost first
	var V1_OPERATIONS = [
		['ordering_operation', 'Sort',               'Sort (ORDER BY)'],
		['grouping_operation', 'Group',              'Group (GROUP BY)'],
		['duplicates_removal', 'Remove duplicates',  'Remove duplicates'],
		['windowing',          'Window',             'Window functions'],
		['buffer_result',      'Buffer result',      'Buffer result']
	];

	// Subqueries of a query block, a table or an operation: {the key in the JSON, the label}
	var V1_SUBQUERIES = [
		['attached_subqueries',       'Subquery'],
		['select_list_subqueries',    'Subquery in SELECT list'],
		['having_subqueries',         'Subquery in HAVING'],
		['order_by_subqueries',       'Subquery in ORDER BY'],
		['group_by_subqueries',       'Subquery in GROUP BY'],
		['optimized_away_subqueries', 'Subquery (optimized away)']
	];

	/** The content of a query block (its operations, tables, subqueries) is added to 'node' */
	function v1QueryBlockInto(node, qb, ctx) {
		if (qb.cost_info && qb.cost_info.query_cost !== undefined) node.metrics.cost = num(qb.cost_info.query_cost);
		if (qb.select_id !== undefined) node.props.cells['select_id'] = String(qb.select_id);
		if (qb.message) {
			node.children.push(newNode('Message', qb.message));
			ctx.messages.push(qb.message);
		}
		var content = v1Content(qb, ctx);
		if (content) node.children.push(content);
		v1Subqueries(qb, node, ctx);
	}

	function v1Subqueries(obj, node, ctx) {
		V1_SUBQUERIES.forEach(function (kv) {
			(obj[kv[0]] || []).forEach(function (sq) {
				var qb  = sq.query_block || {};
				var sub = newNode('Subquery', kv[1] + (qb.select_id !== undefined ? ' #' + qb.select_id : '') + (sq.dependent ? ' (dependent)' : ''));
				sub.props.dependent = !!sq.dependent;
				if (sq.cacheable !== undefined) sub.props.cells['cacheable'] = String(sq.cacheable);
				sub.props.cells['dependent'] = String(!!sq.dependent);
				v1QueryBlockInto(sub, qb, ctx);
				node.children.push(sub);
			});
		});
	}

	function v1Content(obj, ctx) {
		for (var i = 0; i < V1_OPERATIONS.length; i++) {
			var w = obj[V1_OPERATIONS[i][0]];
			if (!w) continue;
			var node = newNode(V1_OPERATIONS[i][1], V1_OPERATIONS[i][2]);
			if (w.using_filesort)        node.props.flags.push('Using filesort');
			if (w.using_temporary_table) node.props.flags.push('Using temporary table');
			(w.windows || []).forEach(function (win) {
				node.props.cells['window ' + win.name] = (win.functions || []).join(', ') + (win.using_filesort ? '  (sorted: ' + (win.filesort_key || []).join(', ') + ')' : '');
				if (win.using_filesort && node.props.flags.indexOf('Using filesort') === -1) node.props.flags.push('Using filesort');
			});
			if (w.cost_info && w.cost_info.sort_cost !== undefined) node.metrics.ownCost = num(w.cost_info.sort_cost);
			fillCells(node.props.cells, w, '', ['nested_loop', 'table', 'union_result', 'windows'].concat(V1_OPERATIONS.map(function (o) { return o[0]; })));
			var child = v1Content(w, ctx);
			if (child) node.children.push(child);
			v1Subqueries(w, node, ctx);
			return node;
		}
		if (obj.nested_loop)  return v1NestedLoop(obj.nested_loop, ctx);
		if (obj.table)        return v1Table(obj.table, ctx);
		if (obj.union_result) return v1Union(obj.union_result, ctx);
		return null;
	}

	/**
	 * MySQL joins the tables in the order of the list, each to the result so far: ((t1 x t2) x t3) ...
	 * How a table is joined is said on the table (using_join_buffer): 'hash join' (8.0.18 and above), 'Block Nested Loop', 'Batched Key Access'.
	 */
	function v1NestedLoop(list, ctx) {
		var inputs = list.map(function (e) { return e.table ? v1Table(e.table, ctx) : v1Content(e, ctx); }).filter(Boolean);
		if (!inputs.length) return null;
		var left = inputs[0];
		for (var i = 1; i < inputs.length; i++) {
			var right = inputs[i];
			var jb    = (right.props.joinBuffer || '').toLowerCase();
			var op    = jb === 'hash join' ? 'Hash join' : jb.indexOf('batched key') !== -1 ? 'Batched key access join' : jb ? 'Block nested loop join' : 'Nested loop join';
			var join  = newNode(op);
			join.children = [left, right];
			join.metrics.cost    = right.metrics.cost;           // prefix_cost of the last table = the cost of the join so far
			join.metrics.estRows = right.metrics.rowsProduced;   // the rows that come out of the join
			if (jb) join.props.cells['join buffer'] = right.props.joinBuffer;
			left = join;
		}
		return left;
	}

	function v1Table(t, ctx) {
		var op = V1_ACCESS[t.access_type] || ('Access type ' + (t.access_type || '?'));
		if (t.using_index && /^(index|range|ref|eq_ref|ref_or_null)$/.test(t.access_type || '')) op = 'Covering ' + op.charAt(0).toLowerCase() + op.slice(1);
		var node = newNode(op);
		var p = node.props, m = node.metrics;

		p.objName      = t.table_name;
		p.alias        = t.table_name;
		p.keyName      = t.key;
		p.accessType   = t.access_type;
		p.possibleKeys = t.possible_keys;
		p.joinBuffer   = t.using_join_buffer;
		p.partitions   = t.partitions;

		if (t.update) ctx.stmtType = 'UPDATE';
		if (t.delete) ctx.stmtType = 'DELETE';
		if (t.insert) ctx.stmtType = 'INSERT';

		m.estRows      = num(t.rows_examined_per_scan);
		m.rowsProduced = num(t.rows_produced_per_join);
		m.filteredPct  = num(t.filtered);
		if (t.cost_info) {
			m.cost    = num(t.cost_info.prefix_cost);
			var rc = num(t.cost_info.read_cost), ec = num(t.cost_info.eval_cost);
			if (rc !== undefined || ec !== undefined) m.ownCost = (rc || 0) + (ec || 0);
			m.dataRead = t.cost_info.data_read_per_join;
		}

		if (t.key) p.access.push('key ' + t.key + (t.used_key_parts ? ' (' + t.used_key_parts.join(', ') + ')' : '') + (t.ref ? ' = ' + t.ref.join(', ') : ''));
		if (t.index_condition)    p.access.push(t.index_condition);
		if (t.attached_condition) p.filter.push(t.attached_condition);

		if (t.using_index)       p.flags.push('Using index (covering: the table rows are not read)');
		if (t.using_join_buffer) p.flags.push('Using join buffer (' + t.using_join_buffer + ')');
		if (t.using_MRR)         p.flags.push('Using MRR');
		if (t.index_condition)   p.flags.push('Using index condition (pushed down to the storage engine)');
		if (t.first_match)       p.flags.push('FirstMatch (' + t.first_match + ')');
		if (t.loose_scan)        p.flags.push('LooseScan');
		if (t.using_temporary_table) p.flags.push('Using temporary table');
		if (t.using_filesort)    p.flags.push('Using filesort');

		fillCells(p.cells, t, '', ['materialized_from_subquery'].concat(V1_SUBQUERIES.map(function (s) { return s[0]; })));

		// A derived table, a view that is not merged, a CTE, or a subquery that was turned into a table: what fills it
		var ms = t.materialized_from_subquery;
		if (ms) {
			var qb  = ms.query_block || {};
			var mat = newNode('Materialize', 'Materialize' + (qb.select_id !== undefined ? ' #' + qb.select_id : '') + (ms.dependent ? ' (dependent)' : ''));
			mat.props.dependent = !!ms.dependent;
			fillCells(mat.props.cells, ms, '', ['query_block']);
			v1QueryBlockInto(mat, qb, ctx);
			node.children.push(mat);
		}
		v1Subqueries(t, node, ctx);
		return node;
	}

	function v1Union(u, ctx) {
		var node = newNode(u.using_temporary_table ? 'Union' : 'Union all', u.using_temporary_table ? 'UNION (remove duplicates)' : 'UNION ALL');
		node.props.objName = u.table_name;   // <union1,2>
		fillCells(node.props.cells, u, '', ['query_specifications']);
		(u.query_specifications || []).forEach(function (qs) {
			var qb  = qs.query_block || {};
			var sel = newNode('Query block', 'Select' + (qb.select_id !== undefined ? ' #' + qb.select_id : ''));
			v1QueryBlockInto(sel, qb, ctx);
			node.children.push(sel);
		});
		return node;
	}

	// ---- Format 2 -----------------------------------------------------------

	/**
	 * One iterator. 'operation' is the long text of EXPLAIN FORMAT=TREE ("Index lookup on o2 using ix_cust (cust_id = o.cust_id)"):
	 * the label is the part before " on <table>" or before ": ", the rest is shown as the object, the access and the filter.
	 */
	function v2Node(j, ctx) {
		var opText = j.operation || '?';
		var label  = opText;
		if (j.table_name && / on /.test(label))                                     label = label.substring(0, label.indexOf(' on '));
		else if (j.access_type === 'join' && / \(/.test(label))                     label = label.substring(0, label.indexOf(' ('));   // the condition is shown as the access
		else if (/^(Filter|Sort|Aggregate|Group aggregate|Window|Stream results|Hash|Deduplicate)\b.*: /i.test(label)) label = label.substring(0, label.indexOf(': '));
		if (label.length > 48) label = label.substring(0, 45) + '...';

		var node = newNode(label);
		var p = node.props, m = node.metrics;

		p.tableName  = j.table_name ? (j.schema_name ? j.schema_name + '.' : '') + j.table_name : undefined;
		p.objName    = j.table_name ? j.table_name + (j.alias && j.alias !== j.table_name ? ' (' + j.alias + ')' : '') : undefined;
		p.alias      = j.alias;
		p.keyName    = j.index_name;
		p.accessType = j.index_access_type || j.access_type;
		p.dependent  = !!j.dependent;
		p.heading    = j.heading;
		p.joinAlgorithm = j.join_algorithm;
		p.operationText = opText;

		if (j.lookup_condition) p.access.push(j.lookup_condition);
		(j.hash_condition || []).forEach(function (c) { p.access.push(c); });
		(j.ranges || []).forEach(function (r) { p.access.push(r); });
		if (j.condition)        p.filter.push(j.condition);
		if (j.covering)         p.flags.push('Covering index (the table rows are not read)');
		if (j.sort_fields)      p.flags.push('Sort on: ' + j.sort_fields.join(', '));
		if (j.access_type === 'sort')                 p.flags.push('Using filesort');
		if (j.access_type === 'temp_table_aggregate') p.flags.push('Using temporary table');
		if (j.zero_rows_cause)  ctx.messages.push(j.zero_rows_cause);

		m.estRows = num(j.estimated_rows);
		m.cost    = num(j.estimated_total_cost);
		if (j.actual_rows !== undefined) {
			ctx.hasActuals = true;
			var loops = num(j.actual_loops);
			m.starts       = loops === undefined ? 1 : loops;
			m.actRows      = num(j.actual_rows) * (m.starts || 1);    // MySQL prints the average per loop
			m.estRowsTotal = m.estRows === undefined ? undefined : m.estRows * (m.starts || 1);
			var lastMs     = num(j.actual_last_row_ms);
			if (lastMs !== undefined) m.actTimeSec = lastMs * (m.starts || 1) / 1000;
		}

		fillCells(p.cells, j, '', ['inputs']);
		(j.inputs || []).forEach(function (child) { node.children.push(v2Node(child, ctx)); });

		// The part of the cost that is this operation's own: MySQL's cost includes the children. Not when that gives a negative
		// number (the inner side of a nested loop is costed per loop, the join for all loops)
		if (m.cost !== undefined && node.children.length) {
			var childCost = 0, all = true;
			node.children.forEach(function (c) { if (c.metrics.cost === undefined) all = false; else childCost += c.metrics.cost; });
			if (all && m.cost - childCost >= 0) m.ownCost = m.cost - childCost;
		} else if (m.cost !== undefined) {
			m.ownCost = m.cost;
		}
		return node;
	}

	// ---- Both ---------------------------------------------------------------

	function parseText(planText) {
		lastParseError = null;
		if (!planText || !String(planText).trim()) { lastParseError = 'The plan is empty.'; return null; }

		// The JSON can have text around it (copied from the mysql client): take the outermost { ... }
		var text  = String(planText);
		var start = text.indexOf('{'), end = text.lastIndexOf('}');
		if (start === -1 || end <= start) { lastParseError = 'This is not the JSON of EXPLAIN FORMAT=JSON (no { ... } was found).'; return null; }

		var json;
		try {
			json = JSON.parse(text.substring(start, end + 1));
		} catch (ex) {
			lastParseError = 'The JSON could not be read: ' + ex.message;
			return null;
		}

		var ctx  = { stmtType: undefined, messages: [], hasActuals: false };
		var root, explainFormat, sqlText;

		if (json.query_block) {
			explainFormat = 'JSON format 1';
			root = newNode('SELECT');
			v1QueryBlockInto(root, json.query_block, ctx);
		} else if (json.query_plan || json.operation) {
			explainFormat = 'JSON format 2 (iterator tree)';
			var plan = json.query_plan || json;
			if (/not executable by iterator executor/i.test(plan.operation || '')) {
				lastParseError = 'MySQL has no iterator tree for this statement (a single-table UPDATE or DELETE): format 2 (explain_json_format_version=2) has nothing to draw. Format 1 has.';
				return null;
			}
			ctx.stmtType = json.query_type ? String(json.query_type).toUpperCase() : undefined;
			root = newNode('SELECT');
			root.metrics.cost    = num(plan.estimated_total_cost);
			root.metrics.estRows = num(plan.estimated_rows);
			root.children.push(v2Node(plan, ctx));
			// "/* select#1 */ select ..." -> "select ..."
			if (json.query) sqlText = String(json.query).replace(/^\s*\/\*\s*select#\d+\s*\*\/\s*/i, '');
		} else {
			lastParseError = 'This JSON is not a MySQL plan: it has neither "query_block" (format 1) nor "query_plan" (format 2).';
			return null;
		}

		root.op    = ctx.stmtType || 'SELECT';
		root.label = root.op;

		// Number the boxes (what a finding's "[Node N]" jumps to), and the part of the cost that is each operation's own
		var seq = 0, total = root.metrics.cost;
		DbxShowplanGraph.walkPlanNodes(root, function (n) {
			n.props.va = seq++;
			if (n.metrics.ownCost !== undefined && total > 0 && n !== root) n.metrics.ownCostPct = n.metrics.ownCost * 100 / total;
		});

		root._planInfo = {
			explainFormat: explainFormat,
			sqlText:       sqlText,
			operations:    seq - 1,
			hasActuals:    ctx.hasActuals,
			notes:         ctx.messages,
			sections:      {}
		};

		return { format: 'json', rawText: planText, statements: [ { label: 'Plan', meta: {}, steps: [ { label: '', root: root } ] } ] };
	}

	// ─────────────────────────────────────────────────────────────────────────
	// Render
	// ─────────────────────────────────────────────────────────────────────────

	var STYLE_INJECTED = false;

	// The rules of dbxShowplanOracle.js (where they are explained), with the prefix 'my-plan'
	var CSS = ''
		+ '.my-plan-wrap { font-family: -apple-system, Segoe UI, Roboto, sans-serif; font-size: 0.8em; padding: 8px 0; }'
		+ '.my-plan-tree { width: max-content; padding: 8px 4px 16px 4px; }'
		+ '.my-plan-tree ul, .my-plan-tree ul ul { display: flex; justify-content: center; padding-top: 20px; position: relative; }'
		+ '.my-plan-tree ul { list-style: none; margin: 0; padding-left: 0; }'
		+ '.my-plan-tree li { list-style: none; position: relative; padding: 20px 8px 0 8px; display: flex; flex-direction: column; align-items: center; }'
		+ '.my-plan-tree li::before, .my-plan-tree li::after { content: ""; position: absolute; top: 0; right: 50%; border-top: 1px solid var(--sp-edge-2); width: 50%; height: 20px; }'
		+ '.my-plan-tree li::after { right: auto; left: 50%; border-left: 1px solid var(--sp-edge-2); }'
		+ '.my-plan-tree li:only-child { padding-top: 0; }'
		+ '.my-plan-tree li:only-child::before, .my-plan-tree li:only-child::after { display: none; }'
		+ '.my-plan-tree li:first-child::before, .my-plan-tree li:last-child::after { border: 0 none; }'
		+ '.my-plan-tree li:last-child::before { border-right: 1px solid var(--sp-edge-2); border-radius: 0 5px 0 0; }'
		+ '.my-plan-tree li:first-child::after { border-radius: 5px 0 0 0; }'
		+ '.my-plan-tree ul ul::before { content: ""; position: absolute; top: 0; left: 50%; border-left: 1px solid var(--sp-edge-2); width: 0; height: 20px; }'
		+ '.my-plan-tree > ul > li { padding-top: 0; }'
		+ '.my-plan-tree > ul > li::before, .my-plan-tree > ul > li::after { display: none; }'
		+ '.my-plan-tree.my-plan-horizontal ul, .my-plan-tree.my-plan-horizontal ul ul { flex-direction: column; justify-content: center; padding-top: 0; padding-left: 24px; }'
		+ '.my-plan-tree.my-plan-horizontal li { padding: 8px 0 8px 24px; flex-direction: row; align-items: center; }'
		+ '.my-plan-tree.my-plan-horizontal li::before, .my-plan-tree.my-plan-horizontal li::after { top: auto; left: 0; right: auto; bottom: 50%; border-top: 0 none; border-left: 1px solid var(--sp-edge-2); width: 24px; height: 50%; }'
		+ '.my-plan-tree.my-plan-horizontal li::after { bottom: auto; top: 50%; border-top: 1px solid var(--sp-edge-2); }'
		+ '.my-plan-tree.my-plan-horizontal li:only-child { padding-left: 0; }'
		+ '.my-plan-tree.my-plan-horizontal li:first-child::before { border: 0 none; }'
		+ '.my-plan-tree.my-plan-horizontal li:last-child::after { border: 0 none; }'
		+ '.my-plan-tree.my-plan-horizontal li:last-child::before { border-right: 0 none; border-bottom: 1px solid var(--sp-edge-2); border-radius: 0; }'
		+ '.my-plan-tree.my-plan-horizontal li:first-child::after { border-radius: 0; }'
		+ '.my-plan-tree.my-plan-horizontal ul ul::before { top: 50%; left: 0; border-left: 0 none; border-top: 1px solid var(--sp-edge-2); width: 24px; height: 0; }'
		+ '.my-plan-tree.my-plan-horizontal > ul > li { padding-left: 0; }'
		+ '.my-plan-tree.my-plan-lines li::before, .my-plan-tree.my-plan-lines li::after, .my-plan-tree.my-plan-lines ul ul::before { display: none !important; }'
		+ '.my-plan-tree.my-plan-lines { position: relative; }'
		+ '.my-plan-connector-svg { position: absolute; top: 0; left: 0; overflow: visible; pointer-events: none; }'
		+ '.my-plan-box-cell { display: contents; }'
		+ '.my-plan-tree.my-plan-compact-h ul { display: block; list-style: none; margin: 0; padding: 0; position: static; }'
		+ '.my-plan-tree.my-plan-compact-h li { display: table; list-style: none; position: static; padding: 0; margin: 0; }'
		+ '.my-plan-tree.my-plan-compact-h li > .my-plan-box-cell { display: table-cell; vertical-align: top; }'
		+ '.my-plan-tree.my-plan-compact-h li > ul { display: table-cell; vertical-align: top; padding-left: 56px; }'
		+ '.my-plan-tree.my-plan-compact-h li > ul > li { display: table; margin: 9px 0; }'
		+ '.my-plan-tree.my-plan-compact-h li > ul > li:first-child { margin-top: 0; }'
		+ '.my-plan-tree.my-plan-compact-h li > ul > li:last-child { margin-bottom: 0; }'
		+ '.my-plan-tree.my-plan-compact-v ul { display: block; list-style: none; margin: 0; padding: 0; position: static; }'
		+ '.my-plan-tree.my-plan-compact-v li { display: table; list-style: none; position: static; padding: 0; margin: 0; }'
		+ '.my-plan-tree.my-plan-compact-v li > .my-plan-box-cell { display: table-caption; caption-side: top; text-align: center; margin-bottom: 6px; }'
		+ '.my-plan-tree.my-plan-compact-v .my-plan-box { display: inline-block; }'
		+ '.my-plan-tree.my-plan-compact-v li > ul { display: table-row; }'
		+ '.my-plan-tree.my-plan-compact-v li > ul > li { display: table-cell; vertical-align: top; padding: 40px 6px 0 6px; }'
		+ '.my-plan-tree.my-plan-compact-v li > ul > li:first-child { padding-left: 0; }'
		+ '.my-plan-tree.my-plan-compact-v li > ul > li:last-child { padding-right: 0; }'
		+ '.my-plan-tree.my-plan-compact-h li:only-child { padding: 0; }'
		+ '.my-plan-box { position: relative; border: 1px solid var(--sp-node-border); border-radius: 5px; background: var(--sp-bg); padding: 5px 9px; cursor: pointer; min-width: 120px; max-width: 240px; text-align: center; box-shadow: 0 1px 2px var(--sp-shadow); }'
		+ '.my-plan-box:hover { border-color: var(--sp-sel); }'
		+ '.my-plan-box.my-plan-warn { border-color: var(--sp-warn-border); background: var(--sp-warn-bg); }'
		+ '.my-plan-va { position: absolute; top: 2px; right: 4px; font-size: 0.72em; color: var(--sp-text-7); line-height: 1; }'
		+ '.my-plan-icon { width: 32px; height: 32px; margin: 0 auto; background-repeat: no-repeat; }'
		+ '.my-plan-label { font-weight: 600; white-space: nowrap; }'
		+ '.my-plan-label.my-plan-label-long { white-space: normal; min-width: 150px; }'
		+ '.my-plan-subtitle { font-size: 0.85em; color: var(--sp-text-2); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 220px; margin: 0 auto; }'
		+ '.my-plan-metric { font-size: 0.85em; color: var(--sp-text-3); white-space: nowrap; }'
		+ '.my-plan-metric.my-plan-warn-text { color: var(--sp-warn-text-3); font-weight: 600; }'
		+ '.my-plan-metric-pct-warn { color: var(--sp-err); font-weight: 700; }'
		+ '.my-plan-detail-pct-warn { color: var(--sp-err); font-weight: 700; }'
		+ '.my-plan-detail { position: fixed; z-index: 2000; color: var(--sp-text); background: var(--sp-tt-bg); border: 1px solid var(--sp-tt-border); border-radius: 4px; padding: 6px 10px; min-width: 220px; max-width: 560px; text-align: left; box-shadow: 0 2px 10px var(--sp-shadow-2); font-size: 11px; line-height: 1.35; }'
		+ '.my-plan-detail { max-height: 80vh; overflow-y: auto; overscroll-behavior: contain; }'
		+ '.my-plan-detail table { border-collapse: collapse; }'
		+ '.my-plan-detail-desc { white-space: normal; font-style: italic; color: var(--sp-tt-text); margin-bottom: 6px; padding-bottom: 6px; border-bottom: 1px solid var(--sp-tt-sep); line-height: 1.35; }'
		+ '.my-plan-detail-idx-hdr { font-size: 10px; color: var(--sp-tt-text); margin-top: 4px; }'
		+ '.my-plan-detail-wrap { white-space: normal; word-break: break-word; }'
		+ '.my-plan-detail-hint { margin-top: 5px; padding-top: 4px; border-top: 1px solid var(--sp-tt-sep); color: var(--sp-tt-muted); font-size: 10px; white-space: normal; }'
		+ '.my-plan-connector-hit { pointer-events: stroke; cursor: help; }'
		+ '.my-plan-detail td { padding: 1px 6px 1px 0; vertical-align: top; white-space: nowrap; }'
		+ '.my-plan-detail td.my-plan-detail-key { color: var(--sp-text-4); }'
		+ '.my-plan-detail .my-plan-raw-line { font-family: monospace; white-space: pre-wrap; color: var(--sp-text-2); }'
		+ '.my-plan-detail.my-plan-tooltip { pointer-events: none; cursor: default; }'
		+ '@keyframes my-plan-flash { 0%, 100% { box-shadow: 0 1px 2px var(--sp-shadow); } 20%, 60% { box-shadow: 0 0 0 5px rgba(74,144,217,0.85); } 40%, 80% { box-shadow: 0 1px 2px var(--sp-shadow); } }'
		+ '.my-plan-box.my-plan-flash { animation: my-plan-flash 0.8s ease-in-out 5; }'
		+ DbxShowplanGraph.propsCss('my-plan')
		+ '.my-plan-box.my-plan-selected { border-color: var(--sp-blue); border-width: 2px; box-shadow: 0 0 0 3px rgba(42,110,187,0.18); }'
		+ '.my-plan-prop-raw { font-family: monospace; white-space: pre-wrap; word-break: break-word; font-size: 10px; color: var(--sp-text-2); line-height: 1.35; }'
		// A note that a finding of the Plan Analysis puts on the box of its operation
		+ '.my-plan-note { white-space: normal; font-weight: 600; }'
		+ '.my-plan-note-warning, .my-plan-note-error { color: var(--sp-err); }'
		+ '.my-plan-note-info { color: var(--sp-blue); }'
		// A subquery that is executed once per row of the outer query: a dashed border
		+ '.my-plan-box.my-plan-dependent { border-style: dashed; }';

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
		WindowAggregate:    [-160, -256],
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
	// The operation (format 1: the names above, format 2: the label of the iterator) -> icon. The first that matches is used
	var ICON_RULES = [
		{ test: /^SELECT$/,                                                  icon: 'Result' },
		{ test: /^(INSERT|REPLACE)$/,                                        icon: 'Insert' },
		{ test: /^UPDATE$/,                                                  icon: 'Update' },
		{ test: /^DELETE$/,                                                  icon: 'Delete' },
		{ test: /index merge|intersect|sort_union/i,                         icon: 'Bitmap' },
		{ test: /full index scan|index scan/i,                               icon: 'IndexScan' },
		{ test: /table scan/i,                                               icon: 'TableScan' },
		{ test: /index|single row|single-row|const|fulltext|lookup/i,        icon: 'IndexSeek' },
		{ test: /hash join/i,                                                icon: 'HashMatch' },
		{ test: /join|nested loop|semijoin|antijoin/i,                       icon: 'NestedLoops' },
		{ test: /^window/i,                                                  icon: 'WindowAggregate' },
		{ test: /^sort/i,                                                    icon: 'Sort' },
		{ test: /group|aggregate|remove duplicates|deduplicat/i,             icon: 'StreamAggregate' },
		{ test: /union/i,                                                    icon: 'Concatenation' },
		{ test: /^filter/i,                                                  icon: 'Filter' },
		{ test: /^limit/i,                                                   icon: 'Top' },
		{ test: /materialize|temporary|buffer result|stream results/i,       icon: 'TableSpool' },
		{ test: /^(message|zero rows|rows fetched|constant)/i,               icon: 'ConstantScan' },
		{ test: /^(subquery|query block)/i,                                  icon: 'ComputeScalar' }
	];
	function iconStyleFor(node) {
		var pos = ICON.Catchall;
		for (var i = 0; i < ICON_RULES.length; i++) {
			if (ICON_RULES[i].test.test(node.op)) { pos = ICON[ICON_RULES[i].icon]; break; }
		}
		return 'background-image:url(' + ICON_SPRITE_URL + ');background-position:' + pos[0] + 'px ' + pos[1] + 'px;';
	}

	// What the operation does, in plain words, for the hover panel and the Properties pane. The first that matches is used
	var OPERATION_DESCRIPTIONS = [
		{ test: /^(SELECT|INSERT|REPLACE|UPDATE|DELETE)$/,  text: 'The statement itself. Its cost is the optimizer\'s estimate for the whole statement (in MySQL\'s own unit, not time).' },
		{ test: /covering.*(index scan|full index scan)/i,  text: 'Reads the whole index, in key order. The index has all the columns that are needed (covering), so the table rows are not read: the index is used as a narrow copy of the table.' },
		{ test: /full index scan|^index scan/i,             text: 'Reads the whole index, in key order, and the table row of every entry. Used to get the rows in the order of the index (to avoid a sort), but it reads everything.' },
		{ test: /table scan/i,                              text: 'Full table scan: every row of the table is read (InnoDB reads the whole clustered index). The right choice when a large part of the table is needed, expensive when only a few rows are. On the inner side of a join it is done once per row of the outer side, unless a join buffer or a hash join is used.' },
		{ test: /index range scan/i,                        text: 'Reads the part of the index that is between the limits of the condition (<, >, BETWEEN, IN, LIKE \'abc%\'), in key order, and the table row of each entry (unless the index is covering).' },
		{ test: /unique index lookup|single-row index lookup|single row/i, text: 'Looks up ONE row with a unique key (the primary key or a unique index): at most one row comes back. On the inner side of a join it is done once per row of the outer side: the cheapest way to join.' },
		{ test: /index lookup/i,                            text: 'Looks up the rows that have one value in a (non-unique) index: all matching entries, then the table row of each (unless the index is covering). On the inner side of a join it is done once per row of the outer side.' },
		{ test: /index merge|intersect|sort_union/i,        text: 'Index merge: two or more indexes of the same table are read, and their results are combined (intersection for AND, union for OR) before the rows are read. Often a sign that one composite index on the columns would be better.' },
		{ test: /fulltext/i,                                text: 'Uses a FULLTEXT index (MATCH ... AGAINST).' },
		{ test: /subquery (index )?lookup/i,                text: 'An IN (subquery) that is executed as an index lookup in the table of the subquery, once per row of the outer query.' },
		{ test: /hash join/i,                               text: 'Hash join (MySQL 8.0.18 and above): a hash table is built in memory from one input (the build input), the other input is read once and each row is looked up in it. MySQL uses it when there is no index on the join columns of the inner table; when the hash table does not fit in join_buffer_size it is written to disk.' },
		{ test: /block nested loop/i,                       text: 'Block nested loop (before MySQL 8.0.18): rows of the outer side are collected in the join buffer, and the inner table is scanned once per buffer full. Used when there is no index on the join columns of the inner table.' },
		{ test: /batched key access/i,                      text: 'Batched key access: the keys of many outer rows are collected in the join buffer and looked up in the index of the inner table in key order (Multi-Range Read), which gives less random I/O.' },
		{ test: /nested loop|join/i,                        text: 'For each row of the first input (the outer side) the second input (the inner side) is executed once. Good when the outer side gives few rows and the inner side is an index lookup; slow when the outer side gives many rows and the inner side has to scan.' },
		{ test: /^sort/i,                                   text: 'Sorts the rows (ORDER BY, or GROUP BY in old versions). In memory when they fit in sort_buffer_size, otherwise with temporary files (Sort_merge_passes). An index that gives the rows in the right order avoids the sort.' },
		{ test: /^group|aggregate/i,                        text: 'Groups the rows for GROUP BY and calculates the aggregates (COUNT, SUM...). Without a suitable index MySQL uses an internal temporary table for the groups.' },
		{ test: /remove duplicates|deduplicat/i,            text: 'Removes duplicate rows (DISTINCT, or a semijoin strategy for IN / EXISTS).' },
		{ test: /^window/i,                                 text: 'Calculates window functions (functions with OVER (...)). The rows are sorted on the PARTITION BY and ORDER BY of the window first, unless they already arrive in that order.' },
		{ test: /union all/i,                               text: 'Returns the rows of all its inputs, one input after the other (UNION ALL).' },
		{ test: /union/i,                                   text: 'UNION: the rows of all its inputs are put in a temporary table that removes the duplicates.' },
		{ test: /^filter/i,                                 text: 'Lets the rows through that pass its condition. A filter right above a table scan is a WHERE condition that no index could help with.' },
		{ test: /^limit/i,                                  text: 'Stops asking for rows as soon as the LIMIT is reached.' },
		{ test: /materialize/i,                             text: 'The result of a derived table, a view that could not be merged, a CTE, or a subquery, is written to an internal temporary table, which is then read like a table.' },
		{ test: /temporary/i,                               text: 'Reads an internal temporary table (the result of a GROUP BY, DISTINCT, UNION or a derived table).' },
		{ test: /buffer result/i,                           text: 'The result is put in a temporary table before it is sent (SQL_BUFFER_RESULT, or to be able to sort it).' },
		{ test: /^subquery/i,                               text: 'A subquery. A DEPENDENT subquery uses a column of the outer query, so it is executed again for every row of the outer query.' },
		{ test: /^query block/i,                            text: 'One SELECT of a UNION.' },
		{ test: /^(message|zero rows|rows fetched)/i,       text: 'MySQL decided this while the statement was optimized, without reading any table (no tables, an impossible WHERE, or rows that could be fetched before the execution).' }
	];
	function operationDescriptionFor(node) {
		for (var i = 0; i < OPERATION_DESCRIPTIONS.length; i++) {
			if (OPERATION_DESCRIPTIONS[i].test.test(node.op)) return OPERATION_DESCRIPTIONS[i].text;
		}
		return undefined;
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
	function fmtSec(s) {
		if (s === undefined || s === null || isNaN(s)) return undefined;
		if (s >= 3600) return Math.floor(s / 3600) + 'h ' + Math.floor((s % 3600) / 60) + 'm';
		if (s >= 60)   return Math.floor(s / 60) + 'm ' + Math.round(s % 60) + 's';
		return (Math.round(s * 1000) / 1000) + ' s';
	}

	// The estimate is called wrong when it is 10 times off (and it is about more than a handful of rows). As dbxShowplanOracle.js
	function isEstActWarn(m) {
		if (m.estRowsTotal === undefined || m.actRows === undefined) return false;
		if (m.starts === 0) return false;
		if (Math.max(m.estRowsTotal, m.actRows) < 100) return false;
		if (m.estRowsTotal <= 0 || m.actRows <= 0) return true;
		var ratio = m.actRows / m.estRowsTotal;
		return ratio >= 10 || ratio <= 0.1;
	}
	function fmtEstActDiff(m) {
		if (m.estRowsTotal === undefined || m.actRows === undefined || m.starts === 0) return undefined;
		if (m.actRows === 0)      return m.estRowsTotal >= 10 ? 'no rows' : undefined;
		if (m.estRowsTotal <= 0)  return 'estimate was 0';
		var ratio = m.actRows / m.estRowsTotal;
		var times = function (x) { return x < 10 ? String(Math.round(x * 10) / 10) : fmtNum(Math.round(x)); };
		if (ratio >= 2)   return times(ratio) + 'x more';
		if (ratio <= 0.5) return times(1 / ratio) + 'x fewer';
		return undefined;
	}

	/** The table (and the index it uses) under the label: "orders (o) · ix_cust" */
	function subtitleFor(node) {
		var p = node.props;
		if (!p.objName) return undefined;
		return p.objName + (p.keyName ? ' · ' + p.keyName : '');
	}
	function nodeTitle(node) {
		var sub = subtitleFor(node);
		return node.label + (sub ? ' ' + sub : '');
	}

	function propRow($into, key, val)  { return DbxShowplanGraph.propRow($into, 'my-plan', key, val); }
	function propSection($into, title) { return DbxShowplanGraph.propSection($into, 'my-plan', title); }
	function propSectionIfAny($into, title, fill) {
		var $tmp = $('<div></div>');
		fill($tmp);
		if ($tmp.children().length) propSection($into, title).append($tmp.children());
	}

	// ─────────────────────────────────────────────────────────────────────────
	// Properties pane: everything the plan says about ONE operation
	// ─────────────────────────────────────────────────────────────────────────
	function renderPropertiesInto(container, node) {
		var $c = $(container);
		$c.empty();
		if (!node) {
			$c.append($('<div class="my-plan-prop-empty"></div>').text('Click an operation in the plan to see all of its properties here.'));
			return;
		}
		var p = node.props, m = node.metrics;

		$c.append($('<div class="my-plan-prop-title"></div>').text(node.label));
		var sub = subtitleFor(node);
		if (sub) $c.append($('<div class="my-plan-prop-subtitle"></div>').text(sub));
		var desc = operationDescriptionFor(node);
		if (desc) $c.append($('<div class="my-plan-prop-desc"></div>').text(desc));
		(node._boxNotes || []).forEach(function (n) {
			$c.append($('<div class="my-plan-prop-warn"></div>').text('⚠ ' + n.title));
		});

		var $op = propSection($c, 'Operation');
		propRow($op, 'Box',           p.va);
		propRow($op, 'Operation',     p.operationText || node.label);
		propRow($op, 'Table',         p.tableName || p.objName);
		propRow($op, 'Alias',         p.alias && p.alias !== p.objName ? p.alias : undefined);
		propRow($op, 'Index',         p.keyName);
		propRow($op, 'Possible keys', p.possibleKeys ? p.possibleKeys.join(', ') : undefined);
		propRow($op, 'Access type',   p.accessType);
		propRow($op, 'Partitions',    p.partitions ? p.partitions.join(', ') : undefined);
		propRow($op, 'Subquery',      p.heading);

		propSectionIfAny($c, 'Conditions', function ($s) {
			[['Index / lookup', p.access], ['Filter', p.filter]].forEach(function (kv) {
				(kv[1] || []).forEach(function (text) { propRow($s, kv[0], text); });
			});
		});
		propSectionIfAny($c, 'Extra', function ($s) {
			p.flags.forEach(function (f, i) { propRow($s, i === 0 ? 'Extra' : '', f); });
		});

		propSectionIfAny($c, 'Optimizer estimate', function ($s) {
			propRow($s, 'Rows examined (per scan)', fmtNum(m.estRows));
			propRow($s, 'Rows produced (per join)', fmtNum(m.rowsProduced));
			propRow($s, 'Filtered',                 m.filteredPct === undefined ? undefined : fmtPct(m.filteredPct) + ' of the rows examined pass the conditions');
			propRow($s, 'Cost (with what is before or below)', fmtNum(m.cost));
			propRow($s, 'Cost (own)',               m.ownCost === undefined ? undefined : fmtNum(m.ownCost) + (m.ownCostPct === undefined ? '' : ' = ' + fmtPct(m.ownCostPct) + ' of the statement'));
			propRow($s, 'Data read (per join)',     m.dataRead);
		});

		propSectionIfAny($c, 'Actual (EXPLAIN ANALYZE)', function ($s) {
			propRow($s, 'Loops',            fmtNum(m.starts));
			propRow($s, 'Rows',             fmtNum(m.actRows));
			propRow($s, 'Rows vs estimate', fmtEstActDiff(m));
			propRow($s, 'Time',             fmtSec(m.actTimeSec));
		});

		// On the statement itself: what is said about the whole plan
		var pi = node._planInfo;
		if (pi) {
			var $pi = propSection($c, 'Plan information');
			propRow($pi, 'EXPLAIN',    pi.explainFormat);
			propRow($pi, 'Operations', pi.operations);
			pi.notes.forEach(function (n, i) { propRow($pi, i === 0 ? 'Note' : '', n); });
		}

		// Every field of the JSON object, as it was printed: nothing in the plan is hidden
		var $all = propSection($c, 'The JSON object');
		Object.keys(p.cells).forEach(function (h) { propRow($all, h, p.cells[h]); });
	}

	/** Hover text of the arrow between two operations: what flows along it (the rows of the CHILD) */
	function connectorTooltip(childNode, parentNode) {
		if (!childNode) return undefined;
		var m = childNode.metrics;
		var lines = [ nodeTitle(childNode) + '  →  ' + (parentNode ? parentNode.label : ''), '' ];
		if (m.actRows !== undefined)      lines.push('Actual rows:      ' + fmtNum(m.actRows));
		var est = m.rowsProduced !== undefined ? m.rowsProduced : m.estRows;
		if (est !== undefined)            lines.push('Estimated rows:   ' + fmtNum(est));
		var diff = fmtEstActDiff(m);
		if (diff !== undefined)           lines.push('Actual vs est:    ' + diff);
		return lines.length > 2 ? lines.join('\n') : undefined;
	}

	// ─────────────────────────────────────────────────────────────────────────
	// The hover panel (click = pinned): the short version of the Properties pane
	// ─────────────────────────────────────────────────────────────────────────
	function buildDetailPanel(node) {
		var p = node.props, m = node.metrics;
		var $panel = $('<div class="my-plan-detail"></div>');

		var description = operationDescriptionFor(node);
		if (description) $panel.append($('<div class="my-plan-detail-desc"></div>').text(description));

		var $tbl = $('<table></table>');
		function row(key, val, valClass) {
			if (val === undefined || val === null || val === '') return;
			var $val = $('<td></td>').text(val);
			if (valClass) $val.addClass(valClass);
			$tbl.append($('<tr></tr>').append($('<td class="my-plan-detail-key"></td>').text(key)).append($val));
		}
		row('Operation', node.label);
		row('Table',     p.tableName || p.objName);
		row('Index',     p.keyName);
		row('Possible keys', p.possibleKeys && !p.keyName ? p.possibleKeys.join(', ') + '  (not used)' : undefined);
		if (m.actRows !== undefined) {
			row('Loops',         fmtNum(m.starts));
			row('Actual rows',   fmtNum(m.actRows));
			row('Est. rows',     fmtNum(m.estRowsTotal));
			row('Actual vs est', fmtEstActDiff(m), isEstActWarn(m) ? 'my-plan-detail-pct-warn' : undefined);
			row('Time',          fmtSec(m.actTimeSec));
		} else {
			row('Rows examined', m.estRows === undefined ? undefined : fmtNum(m.estRows) + (m.rowsProduced !== undefined ? ' per scan' : ''));
			row('Rows produced', fmtNum(m.rowsProduced));
		}
		row('Filtered', m.filteredPct === undefined || m.filteredPct >= 100 ? undefined : fmtPct(m.filteredPct), m.filteredPct <= 10 ? 'my-plan-detail-pct-warn' : undefined);
		row('Cost',     m.cost === undefined ? undefined : fmtNum(m.cost) + (m.ownCostPct === undefined ? '' : '  (own: ' + fmtNum(m.ownCost) + ' = ' + fmtPct(m.ownCostPct) + ')'));
		$panel.append($tbl);

		// The conditions can be long: full width and wrapping, below the table
		[['Index / lookup', p.access], ['Filter', p.filter]].forEach(function (kv) {
			(kv[1] || []).forEach(function (text) {
				$panel.append($('<div class="my-plan-detail-idx-hdr"></div>').text(kv[0] + ':'));
				$panel.append($('<div class="my-plan-detail-wrap my-plan-raw-line"></div>').text(text));
			});
		});
		if (p.flags.length) {
			$panel.append($('<div class="my-plan-detail-idx-hdr"></div>').text('Extra:'));
			$panel.append($('<div class="my-plan-detail-wrap"></div>').text(p.flags.join('; ')));
		}
		(node._boxNotes || []).forEach(function (n) {
			$panel.append($('<div class="my-plan-detail-idx-hdr my-plan-detail-pct-warn"></div>').text('⚠ ' + n.title));
		});

		$panel.append($('<div class="my-plan-detail-hint"></div>').text('Click the box to pin this, and to show all properties in the Properties pane.'));
		return $panel;
	}

	// The detail panels are fixed-position overlays on <body>, handled by the shared panel system (dbxShowplanGraph.js)
	var _panels = DbxShowplanGraph.createPanelSystem({
		prefix:     'my-plan',
		buildPanel: function (node) { return buildDetailPanel(node); }
	});

	// Set by render(), so that a click on a box can show the operation in the Properties pane
	var _propsTarget = null;
	var _selectedBox = null;

	function selectNode($box, node) {
		if (_selectedBox) _selectedBox.removeClass('my-plan-selected');
		_selectedBox = $box;
		$box.addClass('my-plan-selected');
		if (_propsTarget) renderPropertiesInto(_propsTarget, node);
	}

	function renderNode(node) {
		var p = node.props, m = node.metrics;
		var $li  = $('<li></li>');
		var $box = $('<div class="my-plan-box"></div>');
		var warn = isEstActWarn(m);
		if (warn || (node._boxNotes || []).some(function (n) { return n.severity !== 'info'; })) $box.addClass('my-plan-warn');
		if (p.dependent) $box.addClass('my-plan-dependent');
		$box[0].__myPlanNode = node;   // for drawConnectorLines(), which walks the DOM and not the model

		// The number is what a "[Node N]" in the Plan Analysis section jumps to (oraShowplanJumpToNode() in dbxShowplan.js)
		$box.attr('data-va', p.va);
		$box.append($('<div class="my-plan-va" title="The number of the operation in this diagram"></div>').text(p.va));

		$box.append($('<div class="my-plan-icon"></div>').attr('style', iconStyleFor(node)));
		$box.append($('<div class="my-plan-label"></div>').toggleClass('my-plan-label-long', node.label.length > 24).text(node.label));
		var subtitle = subtitleFor(node);
		if (subtitle) $box.append($('<div class="my-plan-subtitle"></div>').attr('title', subtitle).text(subtitle));

		// Rows: "Est 1 / Act 2,207 (2,207x more)" with EXPLAIN ANALYZE, else the estimate (format 1: per scan, and how many pass the conditions)
		if (m.actRows !== undefined) {
			var $rows = $('<div class="my-plan-metric"></div>');
			if (warn) $rows.addClass('my-plan-warn-text');
			$rows.text((m.estRowsTotal === undefined ? '' : 'Est ' + fmtNum(m.estRowsTotal) + ' / ') + 'Act ' + fmtNum(m.actRows));
			var diff = fmtEstActDiff(m);
			if (diff !== undefined) {
				var $diff = $('<span></span>').text(' (' + diff + ')');
				if (warn) $diff.addClass('my-plan-metric-pct-warn');
				$rows.append($diff);
			}
			$box.append($rows);
		} else if (m.estRows !== undefined) {
			$box.append($('<div class="my-plan-metric"></div>').text('Rows ' + fmtNum(m.estRows)
				+ (m.filteredPct !== undefined && m.filteredPct < 100 ? ' (filtered ' + fmtPct(m.filteredPct) + ')' : '')));
		}

		// The cost includes what is before (format 1) or below (format 2): the part that is the operation's own is shown too
		// (in red when this one operation is half of the statement or more). Not on the statement itself.
		if (m.cost !== undefined) {
			var $cost = $('<div class="my-plan-metric"></div>').text('Cost ' + fmtNum(m.cost));
			if (m.ownCostPct !== undefined && m.ownCostPct >= 1 && !node._planInfo) {
				var $own = $('<span></span>').text(' (own ' + fmtPct(m.ownCostPct) + ')');
				if (m.ownCostPct >= 50) $own.addClass('my-plan-metric-pct-warn');
				$cost.append($own);
			}
			$box.append($cost);
		}
		if (m.actTimeSec !== undefined) $box.append($('<div class="my-plan-metric"></div>').text('Time ' + fmtSec(m.actTimeSec)));

		(node._boxNotes || []).forEach(function (n) {
			$box.append($('<div class="my-plan-metric my-plan-note my-plan-note-' + n.severity + '"></div>')
				.text((n.severity === 'info' ? 'ℹ ' : '⚠ ') + n.boxNote));
		});

		// Hover = the panel as a tooltip. Click = pin it (click again to close), and show the operation in the Properties pane.
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

		$li.append($('<div class="my-plan-box-cell"></div>').append($box));

		if (node.children.length) {
			var $childUl = $('<ul></ul>');
			node.children.forEach(function (child) { $childUl.append(renderNode(child)); });
			$li.append($childUl);
		}
		return $li;
	}

	// The arrows between the boxes: drawConnectorLines() of dbxShowplanOracle.js (and dbxShowplanAse.js, where each step is explained)
	var _arrowMarkerSeq = 0;
	function drawConnectorLines(treeEl, horizontal) {
		var previousSvg = treeEl.querySelector(':scope > svg.my-plan-connector-svg');
		if (previousSvg) previousSvg.remove();

		var svgNS = 'http://www.w3.org/2000/svg';
		var containerRect = treeEl.getBoundingClientRect();
		var svg = document.createElementNS(svgNS, 'svg');
		svg.setAttribute('class', 'my-plan-connector-svg');
		svg.setAttribute('width', treeEl.scrollWidth);
		svg.setAttribute('height', treeEl.scrollHeight);

		var markerId = 'my-plan-arrow-' + (++_arrowMarkerSeq);
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

		var allBoxRects = Array.prototype.map.call(treeEl.querySelectorAll('.my-plan-box'), function (b) {
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
			var box = li.querySelector(':scope > .my-plan-box-cell > .my-plan-box');
			var parentBox = parentLi.querySelector(':scope > .my-plan-box-cell > .my-plan-box');
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

			var tip = connectorTooltip(box.__myPlanNode, parentBox.__myPlanNode);
			if (tip) {
				var hit = document.createElementNS(svgNS, 'path');
				hit.setAttribute('d', d);
				hit.setAttribute('fill', 'none');
				hit.setAttribute('stroke', 'transparent');
				hit.setAttribute('stroke-width', '12');
				hit.setAttribute('class', 'my-plan-connector-hit');
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
	// Everything comes from the plan alone, so each finding says what the plan shows and what to check.
	// Returned as the findings array the dialog's "Plan Analysis" section draws (the same list the other dialogs use).
	// A finding can also put a short note on the box of its operation (node._boxNotes, drawn by renderNode()).
	// ─────────────────────────────────────────────────────────────────────────

	// A full scan of fewer rows than this is not worth a word
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
		/** A real table: not one MySQL made itself (<derived2>, <subquery2>, <union1,2>, <temporary>), and not a CTE or view that MySQL materialized (the box below it) */
		function isRealTable(n) {
			return n.props.objName && n.props.objName.charAt(0) !== '<' && !n.children.some(function (c) { return /^materialize/i.test(c.op); });
		}
		function rowsOf(n) { return n.metrics.actRows !== undefined ? n.metrics.actRows : n.metrics.estRows; }

		DbxShowplanGraph.walkPlanNodes(root, function (n) {
			var p = n.props, m = n.metrics;
			var rows = rowsOf(n);

			// --- A full table scan of many rows. On the inner side of a join (a join buffer or a hash join) it is "a join without an index"
			if (/table scan/i.test(n.op) && isRealTable(n) && rows >= MANY_ROWS && !p.joinBuffer) {
				var unused = p.possibleKeys && p.possibleKeys.length && !p.keyName;
				add(n, 'warning', 'Full table scan', 'Full table scan of ' + p.objName + ' (about ' + fmtNum(rows) + ' rows)',
					unused
						? 'The optimizer could have used the index ' + p.possibleKeys.join(' / ') + ', but it estimated that reading the whole table is cheaper: many rows match the condition, or the index statistics are off (ANALYZE TABLE updates them).'
						: 'No index could be used for the conditions on this table' + (p.filter.length ? ' (the filter: ' + p.filter[0] + ')' : '') + '. If only a small part of the rows is needed, an index on the columns of the condition helps. See also the Table Information below.',
					'full scan');
			}

			// --- The inner table of a join is joined without an index (format 1: using_join_buffer, format 2: a hash join)
			if (p.joinBuffer && isRealTable(n)) {
				add(n, rows >= MANY_ROWS ? 'warning' : 'info', 'Join', 'No index for the join on ' + p.objName + ' (' + p.joinBuffer + ')',
					'MySQL joins ' + p.objName + ' with a join buffer (' + p.joinBuffer + '): there is no index on its join columns that it can use, so the table is read' + (rows !== undefined ? ' (about ' + fmtNum(rows) + ' rows)' : '') + ' and matched in memory. An index on the join columns of ' + p.objName + ' lets MySQL look the rows up instead.',
					'no join index');
			}
			if (p.joinAlgorithm === 'hash' || p.joinAlgorithm === 'hash_join') {
				add(n, 'info', 'Join', 'Hash join' + (p.access.length ? ' on ' + p.access.join(' and ') : '') + ': no index is used for the join',
					'MySQL uses a hash join when there is no index it can use for the join condition: one input is put in a hash table in memory (join_buffer_size, on disk when it does not fit), the other is read once. Fine for big joins; for a join that returns few rows an index on the join columns of the inner table is often faster.',
					'no join index');
			}

			// --- A full index scan of many rows: the whole index is read
			if (/full index scan|^index scan|covering index scan/i.test(n.op) && isRealTable(n) && rows >= 10 * MANY_ROWS) {
				add(n, 'info', 'Full index scan', 'Full index scan of ' + p.objName + (p.keyName ? ' (' + p.keyName + ')' : '') + ', about ' + fmtNum(rows) + ' rows',
					'The whole index is read, in key order: to get the rows sorted without a sort, or because the index has all the columns that are needed. It is still all rows.');
			}

			// --- Most rows that are read are thrown away by the conditions (format 1: filtered)
			if (m.filteredPct !== undefined && m.filteredPct <= 10 && rows >= MANY_ROWS && isRealTable(n)) {
				add(n, 'info', 'Filter', 'Only ' + fmtPct(m.filteredPct) + ' of the rows read from ' + p.objName + ' are kept',
					'MySQL estimates that ' + fmtPct(m.filteredPct) + ' of the ' + fmtNum(rows) + ' rows it reads pass the conditions' + (p.filter.length ? ' (' + p.filter[0] + ')' : '') + '. An index on the columns of the condition would read only the rows that are needed.');
			}

			// --- A dependent subquery: executed again for every row of the outer query
			if (p.dependent) {
				add(n, 'warning', 'Subquery', (p.heading || n.label) + ': executed once per row of the outer query',
					'The subquery uses a column of the outer query, so MySQL executes it once for every row of the outer query. When the outer query has many rows, rewriting it as a JOIN (or a derived table that is joined) does the work once.',
					'once per outer row');
			}

			// --- Sorts and temporary tables
			// (a sort of a few rows is nothing to point at: the rows are the operation's own, or what comes into it)
			var sortRows = rows !== undefined ? rows : (n.children.length ? (n.children[0].metrics.rowsProduced !== undefined ? n.children[0].metrics.rowsProduced : rowsOf(n.children[0])) : undefined);
			if (p.flags.indexOf('Using filesort') !== -1 && (sortRows === undefined || sortRows >= MANY_ROWS)) {
				add(n, 'info', 'Sort', 'Filesort: ' + n.label,
					'The rows are sorted: in memory when they fit in sort_buffer_size, otherwise with temporary files. An index that has the columns of the ORDER BY (in that order, after the columns of an equality condition) gives the rows sorted, without the sort.');
			}
			if (p.flags.indexOf('Using temporary table') !== -1) {
				add(n, 'info', 'Temporary table', 'Temporary table: ' + n.label,
					'MySQL puts the rows in an internal temporary table (for GROUP BY, DISTINCT, UNION or a derived table). In memory while it fits (TempTable engine, temptable_max_ram), otherwise on disk.');
			}

			// --- EXPLAIN ANALYZE: the estimate was far from what happened
			if (isEstActWarn(m)) {
				add(n, 'warning', 'Estimate', 'The row estimate was wrong for ' + nodeTitle(n) + ' (' + fmtEstActDiff(m) + ')',
					'The optimizer expected ' + fmtNum(m.estRowsTotal) + ' rows, there were ' + fmtNum(m.actRows) + '. A plan built on a wrong estimate can choose the wrong join order or access path: check the index statistics (ANALYZE TABLE), and histograms for columns without an index (ANALYZE TABLE ... UPDATE HISTOGRAM).',
					fmtEstActDiff(m));
			}
		});

		(pi.notes || []).forEach(function (note) {
			add(null, 'info', 'Note', note, /impossible/i.test(note)
				? 'MySQL found that the conditions can never be true, so it does not read any table.'
				: 'MySQL says this about the statement.');
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

		// Before the boxes are made: a finding can put a note on the box of its operation
		var findings = collectFindings(parsed);

		var $wrap = $('<div class="my-plan-wrap"></div>');
		var trees = [];
		parsed.statements.forEach(function (stmt) {
			var $tree = $('<div class="my-plan-tree"></div>');
			if (useCompactLayout)   $tree.addClass(horizontal ? 'my-plan-compact-h' : 'my-plan-compact-v');
			else if (horizontal)    $tree.addClass('my-plan-horizontal');
			if (useLineConnectors)  $tree.addClass('my-plan-lines');
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
				if (horizontal) DbxShowplanGraph.tuckLeavesNearParent($tree[0], 'my-plan');
				else            DbxShowplanGraph.tuckLeavesNearParentVertical($tree[0], 'my-plan');
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
