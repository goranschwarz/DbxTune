/**
 * dbxShowplanOracle.js — Oracle graphical execution plan renderer
 *
 * Parses the text that DBMS_XPLAN prints (DISPLAY_CURSOR, DISPLAY: all its functions print the same format)
 * into the same tree model dbxShowplanAse.js and dbxShowplanSqlServer.js use, and renders it as the same
 * CSS/DOM box-and-connector diagram — no external graph/layout library.
 *
 * Why the TEXT, and not the rows of V$SQL_PLAN: the DBMS_XPLAN text is what an Oracle DBA has (it
 * is what gets pasted into a ticket or a mail), it is what OracleTune records (CmActiveStatements,
 * column 'ExecPlan'), and it already holds the tree: the indentation of the 'Operation' column.
 *
 * Entry points:
 *   var parsed = OracleShowplan.parseText(planText);
 *   OracleShowplan.render(containerEl, parsed, opts);
 *
 * parseText() returns null when no plan table was found in the text (the reason is available via
 * getLastParseError()), so the caller can fall back to showing the text as it is.
 *
 * Node model (the same shape as the two other renderers):
 *   {
 *     op:       'TABLE ACCESS FULL',        // the operation, as DBMS_XPLAN prints it
 *     label:    'TABLE ACCESS FULL',
 *     metrics:  { estRows, estBytes, cost, cpuPct, estTimeSec, tempSpc, ownCost, ownCostPct,        // the optimizer's estimate
 *                 starts, actRows, estRowsTotal (= estRows x starts), actTimeSec, ownActTimeSec, ownActTimePct,
 *                 buffers, ownBuffers, ownBuffersPct, reads, writes, usedMem, usedTmp, stoppedEarly },  // only with ALLSTATS
 *     props:    { va (= the operation Id), objName, access[], filter[], storage[], projection, qblock, alias,
 *                 pstart, pstop, tq, inOut, pqDistrib, cells{} (every column of the plan line, as printed) },
 *     children: [ node, ... ]
 *   }
 * Top-level result:
 *   { format: 'text', rawText, statements: [ { label, meta, steps: [ { label, root: node } ] } ] }
 * One statement per plan table in the text (DISPLAY_CURSOR prints one per child cursor).
 * The root node of each also has: _planInfo { sqlId, childNumber, planHash, sqlText, notes[], sections{},
 *                                             hasActuals, operations, inactive }
 *
 * The layout CSS and the connector drawing (drawConnectorLines) are a copy of dbxShowplanAse.js, with
 * the class prefix changed to 'ora-plan' ("copy now, extract later", as dbxShowplanSqlServer.js was made).
 * The layout plumbing that is already shared lives in dbxShowplanGraph.js, loaded BEFORE this file.
 */

window.OracleShowplan = (function () {

	// Why the most recent parseText() returned null
	var lastParseError = null;

	// ─────────────────────────────────────────────────────────────────────────
	// Parser: the text of DBMS_XPLAN
	//
	//   SQL_ID  3h0b9daxdfpys, child number 0
	//   -------------------------------------
	//   update DBX_SEGTEST.ORDERS set AMOUNT = AMOUNT + 1 where ID = 7
	//
	//   Plan hash value: 3086314548
	//
	//   --------------------------------------------------------------------------------
	//   | Id  | Operation          | Name      | Rows  | Bytes | Cost (%CPU)| Time     |
	//   --------------------------------------------------------------------------------
	//   |   0 | UPDATE STATEMENT   |           |       |       |     2 (100)|          |
	//   |   1 |  UPDATE            | ORDERS    |       |       |            |          |
	//   |*  2 |   INDEX UNIQUE SCAN| ORDERS_PK |     1 |   161 |     1   (0)| 00:00:01 |
	//   --------------------------------------------------------------------------------
	//
	//   Predicate Information (identified by operation id):
	//   ---------------------------------------------------
	//
	//      2 - access("ID"=7)
	// ─────────────────────────────────────────────────────────────────────────

	/** "11M" -> 11000000, "340K" -> 340000, "1" -> 1, "" -> undefined. DBMS_XPLAN shortens big numbers with K, M, G, T, P, E */
	function parseNum(s) {
		if (s === undefined || s === null) return undefined;
		var m = /^([0-9]+(?:\.[0-9]+)?)\s*([KMGTPE])?$/i.exec(String(s).trim());
		if (!m) return undefined;
		var mult = { K: 1e3, M: 1e6, G: 1e9, T: 1e12, P: 1e15, E: 1e18 }[(m[2] || '').toUpperCase()] || 1;
		return parseFloat(m[1]) * mult;
	}

	/** "00:00:01" -> 1, "00:01:30.25" -> 90.25 (seconds), "" -> undefined */
	function parseTime(s) {
		var m = /^(\d+):(\d+):(\d+(?:\.\d+)?)$/.exec(String(s || '').trim());
		if (!m) return undefined;
		return parseInt(m[1], 10) * 3600 + parseInt(m[2], 10) * 60 + parseFloat(m[3]);
	}

	/** The cells of a table line: the text between the '|' characters, NOT trimmed (the indentation of 'Operation' is the tree) */
	function splitCells(line) {
		var parts = line.split('|');
		return parts.slice(1, parts.length - 1);
	}

	function isDashLine(line) {
		return /^\s*-{3,}\s*$/.test(line);
	}

	/**
	 * The lines "  <id> - text" of a section (predicates, projection, alias) into: id -> [text, ...].
	 * A line that does not start with an id continues the entry of the id above it: if it starts a new
	 * predicate ("filter(", "access(", "storage(") it is a new text, otherwise it is a long text that was wrapped.
	 */
	function parseIdSection(lines) {
		var byId = {}, curId = null;
		lines.forEach(function (line) {
			if (!line.trim()) return;
			var m = /^\s*(\d+)\s+-\s(.*)$/.exec(line);
			if (m) {
				curId = m[1];
				(byId[curId] = byId[curId] || []).push(m[2].trim());
			} else if (curId !== null) {
				var t = line.trim();
				var list = byId[curId];
				if (/^(access|filter|storage)\(/.test(t)) list.push(t);
				else list[list.length - 1] += ' ' + t;
			}
		});
		return byId;
	}

	/** One plan table (and the sections below it), starting at the header line "| Id | Operation |..." */
	function parseOnePlan(lines, headerIdx, info) {
		var headers = splitCells(lines[headerIdx]).map(function (h) { return h.trim().replace(/\s+/g, ' '); });
		var col = {};
		headers.forEach(function (h, i) { col[h] = i; });
		var opCol = col['Operation'];
		if (opCol === undefined) return null;

		// The rows: from below the dashes under the header, until the first line that is not a table line
		var i = headerIdx + 1;
		if (i < lines.length && isDashLine(lines[i])) i++;
		var nodes = [], byId = {}, stack = [], root = null;
		for (; i < lines.length; i++) {
			var line = lines[i];
			if (line.charAt(0) !== '|') break;
			var cells = splitCells(line);
			if (cells.length !== headers.length) continue;   // a line we do not understand: skip it, do not guess

			// Id: "   0", "*  2" (has a predicate), "-   5" / "- * 5" (not used: the other half of an adaptive plan)
			var idCell = cells[col['Id']] || '';
			var idMatch = /(\d+)\s*$/.exec(idCell);
			if (!idMatch) continue;
			var id = idMatch[1];

			// Operation: one more space of indentation for each level in the tree (the root has one space)
			var opCell = cells[opCol];
			var depth  = Math.max(0, (opCell.length - opCell.replace(/^\s+/, '').length) - 1);
			var op     = opCell.trim();

			var cell = function (name) { var c = col[name]; return c === undefined ? '' : cells[c].trim(); };
			var costM = /^([0-9.]+[KMGTPE]?)\s*(?:\(\s*(\d+)\s*\))?$/i.exec(cell('Cost (%CPU)') || cell('Cost'));

			var node = {
				op: op,
				label: op,
				metrics: {
					estRows:    parseNum(cell('Rows') || cell('E-Rows')),
					estBytes:   parseNum(cell('Bytes') || cell('E-Bytes')),
					cost:       costM ? parseNum(costM[1]) : undefined,
					cpuPct:     costM && costM[2] !== undefined ? parseInt(costM[2], 10) : undefined,
					estTimeSec: parseTime(cell('Time') || cell('E-Time')),
					tempSpc:    parseNum(cell('TempSpc')),
					starts:     parseNum(cell('Starts')),
					actRows:    parseNum(cell('A-Rows')),
					actTimeSec: parseTime(cell('A-Time')),
					buffers:    parseNum(cell('Buffers')),
					reads:      parseNum(cell('Reads')),
					writes:     parseNum(cell('Writes')),
					usedMem:    cell('Used-Mem') || undefined,
					usedTmp:    cell('Used-Tmp') || undefined
				},
				props: {
					va:           id,                       // the badge on the box, and what a finding's "[Node N]" jumps to
					objName:      cell('Name') || undefined,
					hasPredicate: idCell.indexOf('*') >= 0,
					inactive:     /^\s*-/.test(idCell),
					pstart:       cell('Pstart') || undefined,
					pstop:        cell('Pstop')  || undefined,
					tq:           cell('TQ')     || undefined,
					inOut:        cell('IN-OUT') || undefined,
					pqDistrib:    cell('PQ Distrib') || undefined,
					cells:        {}
				},
				children: [],
				_planLine: line
			};
			headers.forEach(function (h, hi) { var v = cells[hi].trim(); if (v && h !== 'Id' && h !== 'Operation') node.props.cells[h] = v; });

			// Hang it in the tree: the parent is the closest line above that is one level less indented
			while (stack.length && stack[stack.length - 1].depth >= depth) stack.pop();
			if (stack.length) stack[stack.length - 1].node.children.push(node);
			else if (!root)   root = node;
			else              root.children.push(node);   // should not happen (two lines at the root level): keep it visible
			stack.push({ depth: depth, node: node });

			nodes.push(node);
			byId[id] = node;
		}
		if (!root) return null;

		// An adaptive plan printed with '+ADAPTIVE' also lists the operations that were NOT chosen (Id marked '-').
		// They are not part of the plan that runs: leave them out, and hang what is below them on the closest operation that is used.
		function usedChildren(n) {
			var out = [];
			n.children.forEach(function (c) { if (c.props.inactive) out = out.concat(usedChildren(c)); else out.push(c); });
			return out;
		}
		var used = nodes.filter(function (n) { return !n.props.inactive; });
		var inactiveCount = nodes.length - used.length;
		if (inactiveCount) used.forEach(function (n) { n.children = usedChildren(n); });

		// The sections below the table: a title line, a line of dashes, then the text. Until the next plan starts.
		var sections = {}, title = null;
		for (; i < lines.length; i++) {
			var l = lines[i];
			if (/^SQL_ID\s+\S+,\s*child number/i.test(l) || /^Plan hash value:/i.test(l) || /^\|\s*Id\s*\|/.test(l) || /^EXPLAINED SQL STATEMENT:/i.test(l)) break;
			// The Hint Report has TWO title lines above its dashes: "Hint Report (identified by...):" and "Total hints for statement: N (...)"
			if (/^Hint Report/i.test(l)) {
				title = 'Hint Report';
				sections[title] = [];
				if (i + 2 < lines.length && isDashLine(lines[i + 2])) { sections[title].push(lines[i + 1]); i += 2; }
				continue;
			}
			if (l.trim() && i + 1 < lines.length && isDashLine(lines[i + 1]) && !isDashLine(l)) {
				title = l.trim();
				sections[title] = [];
				i++;   // the dashes
				continue;
			}
			if (title !== null && !isDashLine(l)) sections[title].push(l);
		}

		var notes = [];
		Object.keys(sections).forEach(function (t) {
			var body = sections[t];
			if (/^Predicate Information/i.test(t)) {
				var preds = parseIdSection(body);
				Object.keys(preds).forEach(function (pid) {
					if (!byId[pid]) return;
					preds[pid].forEach(function (text) {
						var kind = /^access\(/.test(text) ? 'access' : (/^storage\(/.test(text) ? 'storage' : 'filter');
						(byId[pid].props[kind] = byId[pid].props[kind] || []).push(text);
					});
				});
			} else if (/^Column Projection Information/i.test(t)) {
				var proj = parseIdSection(body);
				Object.keys(proj).forEach(function (pid) { if (byId[pid]) byId[pid].props.projection = proj[pid].join(' '); });
			} else if (/^Query Block Name/i.test(t)) {
				var qb = parseIdSection(body);
				Object.keys(qb).forEach(function (pid) {
					if (!byId[pid]) return;
					var parts = qb[pid].join(' ').split('/');
					byId[pid].props.qblock = parts[0].trim();
					if (parts.length > 1) byId[pid].props.alias = parts.slice(1).join('/').trim();
				});
			} else if (/^Note$/i.test(t)) {
				// "   - text" is a note, a line without the '-' continues the note above it
				body.forEach(function (nl) {
					if (!nl.trim()) return;
					if (/^\s*-\s/.test(nl) || !notes.length) notes.push(nl.replace(/^\s*-\s*/, '').trim());
					else notes[notes.length - 1] += ' ' + nl.trim();
				});
			}
		});

		// What an operation costs BY ITSELF: Oracle's Cost, A-Time and Buffers are for the operation AND everything below it.
		// Some lines have no value (Oracle leaves the Cost of a SORT AGGREGATE empty): then the lines below it count instead.
		function subtreeSum(n, field) {
			if (n.metrics[field] !== undefined) return n.metrics[field];
			var sum = 0;
			n.children.forEach(function (c) { sum += subtreeSum(c, field); });
			return sum;
		}
		function ownShare(field, ownField, pctField, minTotal) {
			var total = subtreeSum(root, field);
			used.forEach(function (n) {
				if (n.metrics[field] === undefined) return;
				var below = 0;
				n.children.forEach(function (c) { below += subtreeSum(c, field); });
				n.metrics[ownField] = Math.max(0, n.metrics[field] - below);
				if (total > 0 && total >= minTotal) n.metrics[pctField] = Math.min(100, n.metrics[ownField] / total * 100);
			});
		}
		ownShare('cost',       'ownCost',       'ownCostPct',    0);
		// A-Time is printed in 1/100 of a second (and is sampled, unless STATISTICS_LEVEL = ALL): a share of a plan
		// that ran for a few hundredths says nothing, so it is only calculated from 0.1 second
		ownShare('actTimeSec', 'ownActTimeSec', 'ownActTimePct', 0.1);
		ownShare('buffers',    'ownBuffers',    'ownBuffersPct', 0);

		var hasActuals = false;
		used.forEach(function (n) {
			var m = n.metrics;
			if (m.actRows !== undefined) hasActuals = true;
			// E-Rows is for ONE execution of the operation, A-Rows is the sum of all its executions (Starts):
			// to compare the two, the estimate has to be multiplied with Starts
			if (m.estRows !== undefined) m.estRowsTotal = m.estRows * (m.starts > 1 ? m.starts : 1);
		});

		// An operation that is stopped as soon as there are enough rows (ROWNUM, FETCH FIRST, the first row of a MIN/MAX)
		// returns fewer rows than its estimate says, and that is not an estimate error. COUNT STOPKEY and FIRST ROW stop
		// everything below them; a top-N sort only cuts its own output (it still has to read all its input).
		(function markStoppedEarly(n, below) {
			var stopsBelow = below || /^(COUNT STOPKEY|FIRST ROW)/.test(n.op);
			if (stopsBelow || /^(SORT|WINDOW) .*(STOPKEY|PUSHED RANK)/.test(n.op)) n.metrics.stoppedEarly = true;
			n.children.forEach(function (c) { markStoppedEarly(c, stopsBelow); });
		})(root, false);

		var sectionText = {};
		Object.keys(sections).forEach(function (t) { sectionText[t] = sections[t].join('\n').replace(/^\s*\n+|\s+$/g, ''); });

		root._planInfo = {
			sqlId:       info.sqlId,
			childNumber: info.childNumber,
			planHash:    info.planHash,
			sqlText:     info.sqlText,
			notes:       notes,
			sections:    sectionText,
			hasActuals:  hasActuals,
			operations:  used.length,
			inactive:    inactiveCount
		};
		return { root: root, nextLine: i };
	}

	function parseText(planText) {
		lastParseError = null;
		if (!planText || !String(planText).trim()) { lastParseError = 'The plan text is empty.'; return null; }

		var rawText = String(planText).replace(/\r\n?/g, '\n');
		var lines   = rawText.split('\n');
		var statements = [];
		var info = {};

		for (var i = 0; i < lines.length; i++) {
			var line = lines[i];
			var m;
			if ((m = /^SQL_ID\s+(\S+),\s*child number\s+(\d+)/i.exec(line)) || /^EXPLAINED SQL STATEMENT:/i.test(line)) {
				// A new cursor (format BASIC prints "EXPLAINED SQL STATEMENT:" instead of the SQL_ID line).
				// Below it (under a line of dashes): the SQL text, until "Plan hash value"
				info = m ? { sqlId: m[1], childNumber: m[2] } : {};
				var sqlLines = [];
				for (var j = i + 1; j < lines.length && !/^Plan hash value:/i.test(lines[j]) && !/^\|\s*Id\s*\|/.test(lines[j]); j++) {
					if (!isDashLine(lines[j])) sqlLines.push(lines[j]);
				}
				info.sqlText = sqlLines.join('\n').trim();
			} else if ((m = /^Plan hash value:\s*(\d+)/i.exec(line))) {
				info.planHash = m[1];
			} else if (/^\|\s*Id\s*\|/.test(line)) {
				var plan = parseOnePlan(lines, i, info);
				if (plan) {
					var label = info.sqlId ? ('SQL_ID ' + info.sqlId + ', child number ' + info.childNumber) : ('Plan ' + (statements.length + 1));
					if (info.planHash) label += ', plan hash value ' + info.planHash;
					statements.push({ label: label, meta: info, steps: [ { label: '', root: plan.root } ] });
					i = plan.nextLine - 1;
				}
				info = {};
			}
		}

		if (!statements.length) {
			// The usual reasons, in the words DBMS_XPLAN itself uses
			var why = /cannot be found|cannot fetch plan|could not be found/i.exec(rawText);
			lastParseError = why
				? 'Oracle did not return a plan: "' + rawText.trim().split('\n').filter(function (l) { return /cannot|could not/i.test(l); })[0].trim() + '"'
				: 'No plan table was found in the text (a line that starts with "| Id | Operation"). Is this the output of DBMS_XPLAN?';
			return null;
		}
		return { format: 'text', rawText: rawText, statements: statements };
	}

	// ─────────────────────────────────────────────────────────────────────────
	// Renderer: a box-and-connector tree made of plain CSS/DOM, no layout library
	// ─────────────────────────────────────────────────────────────────────────

	var STYLE_INJECTED = false;
	// The layout rules (the <ul><li> tree, left-to-right or top-to-bottom, the compact layout that is made of nested
	// CSS tables, the box, the hover panel) are the ones of dbxShowplanAse.js with the class prefix changed to 'ora-plan'.
	// Each of them is explained there, at the same rule.
	var CSS = ''
		+ '.ora-plan-wrap { font-family: -apple-system, Segoe UI, Roboto, sans-serif; font-size: 0.8em; padding: 8px 0; }'
		+ '.ora-plan-stmt-hdr { font-weight: 600; font-size: 0.9em; color: var(--sp-text-1); margin: 6px 0 2px 0; }'
		+ '.ora-plan-step-hdr { font-size: 0.8em; color: var(--sp-text-5); margin-bottom: 6px; }'
		+ '.ora-plan-tree { width: max-content; padding: 8px 4px 16px 4px; }'
		+ '.ora-plan-tree ul, .ora-plan-tree ul ul { display: flex; justify-content: center; padding-top: 20px; position: relative; }'
		+ '.ora-plan-tree ul { list-style: none; margin: 0; padding-left: 0; }'
		+ '.ora-plan-tree li { list-style: none; position: relative; padding: 20px 8px 0 8px; display: flex; flex-direction: column; align-items: center; }'
		+ '.ora-plan-tree li::before, .ora-plan-tree li::after { content: ""; position: absolute; top: 0; right: 50%; border-top: 1px solid var(--sp-edge-2); width: 50%; height: 20px; }'
		+ '.ora-plan-tree li::after { right: auto; left: 50%; border-left: 1px solid var(--sp-edge-2); }'
		+ '.ora-plan-tree li:only-child { padding-top: 0; }'
		+ '.ora-plan-tree li:only-child::before, .ora-plan-tree li:only-child::after { display: none; }'
		+ '.ora-plan-tree li:first-child::before, .ora-plan-tree li:last-child::after { border: 0 none; }'
		+ '.ora-plan-tree li:last-child::before { border-right: 1px solid var(--sp-edge-2); border-radius: 0 5px 0 0; }'
		+ '.ora-plan-tree li:first-child::after { border-radius: 5px 0 0 0; }'
		+ '.ora-plan-tree ul ul::before { content: ""; position: absolute; top: 0; left: 50%; border-left: 1px solid var(--sp-edge-2); width: 0; height: 20px; }'
		+ '.ora-plan-tree > ul > li { padding-top: 0; }'
		+ '.ora-plan-tree > ul > li::before, .ora-plan-tree > ul > li::after { display: none; }'
		+ '.ora-plan-tree.ora-plan-horizontal ul, .ora-plan-tree.ora-plan-horizontal ul ul { flex-direction: column; justify-content: center; padding-top: 0; padding-left: 24px; }'
		+ '.ora-plan-tree.ora-plan-horizontal li { padding: 8px 0 8px 24px; flex-direction: row; align-items: center; }'
		+ '.ora-plan-tree.ora-plan-horizontal li::before, .ora-plan-tree.ora-plan-horizontal li::after { top: auto; left: 0; right: auto; bottom: 50%; border-top: 0 none; border-left: 1px solid var(--sp-edge-2); width: 24px; height: 50%; }'
		+ '.ora-plan-tree.ora-plan-horizontal li::after { bottom: auto; top: 50%; border-top: 1px solid var(--sp-edge-2); }'
		+ '.ora-plan-tree.ora-plan-horizontal li:only-child { padding-left: 0; }'
		+ '.ora-plan-tree.ora-plan-horizontal li:first-child::before { border: 0 none; }'
		+ '.ora-plan-tree.ora-plan-horizontal li:last-child::after { border: 0 none; }'
		+ '.ora-plan-tree.ora-plan-horizontal li:last-child::before { border-right: 0 none; border-bottom: 1px solid var(--sp-edge-2); border-radius: 0; }'
		+ '.ora-plan-tree.ora-plan-horizontal li:first-child::after { border-radius: 0; }'
		+ '.ora-plan-tree.ora-plan-horizontal ul ul::before { top: 50%; left: 0; border-left: 0 none; border-top: 1px solid var(--sp-edge-2); width: 24px; height: 0; }'
		+ '.ora-plan-tree.ora-plan-horizontal > ul > li { padding-left: 0; }'
		+ '.ora-plan-tree.ora-plan-lines li::before, .ora-plan-tree.ora-plan-lines li::after, .ora-plan-tree.ora-plan-lines ul ul::before { display: none !important; }'
		+ '.ora-plan-tree.ora-plan-lines { position: relative; }'
		+ '.ora-plan-connector-svg { position: absolute; top: 0; left: 0; overflow: visible; pointer-events: none; }'
		+ '.ora-plan-box-cell { display: contents; }'
		+ '.ora-plan-tree.ora-plan-compact-h ul { display: block; list-style: none; margin: 0; padding: 0; position: static; }'
		+ '.ora-plan-tree.ora-plan-compact-h li { display: table; list-style: none; position: static; padding: 0; margin: 0; }'
		+ '.ora-plan-tree.ora-plan-compact-h li > .ora-plan-box-cell { display: table-cell; vertical-align: top; }'
		+ '.ora-plan-tree.ora-plan-compact-h li > ul { display: table-cell; vertical-align: top; padding-left: 56px; }'
		+ '.ora-plan-tree.ora-plan-compact-h li > ul > li { display: table; margin: 9px 0; }'
		+ '.ora-plan-tree.ora-plan-compact-h li > ul > li:first-child { margin-top: 0; }'
		+ '.ora-plan-tree.ora-plan-compact-h li > ul > li:last-child { margin-bottom: 0; }'
		+ '.ora-plan-tree.ora-plan-compact-v ul { display: block; list-style: none; margin: 0; padding: 0; position: static; }'
		+ '.ora-plan-tree.ora-plan-compact-v li { display: table; list-style: none; position: static; padding: 0; margin: 0; }'
		+ '.ora-plan-tree.ora-plan-compact-v li > .ora-plan-box-cell { display: table-caption; caption-side: top; text-align: center; margin-bottom: 6px; }'
		+ '.ora-plan-tree.ora-plan-compact-v .ora-plan-box { display: inline-block; }'
		+ '.ora-plan-tree.ora-plan-compact-v li > ul { display: table-row; }'
		+ '.ora-plan-tree.ora-plan-compact-v li > ul > li { display: table-cell; vertical-align: top; padding: 40px 6px 0 6px; }'
		+ '.ora-plan-tree.ora-plan-compact-v li > ul > li:first-child { padding-left: 0; }'
		+ '.ora-plan-tree.ora-plan-compact-v li > ul > li:last-child { padding-right: 0; }'
		+ '.ora-plan-tree.ora-plan-compact-h li:only-child, .ora-plan-tree.ora-plan-compact-v li:only-child { padding: 0; }'
		+ '.ora-plan-box { position: relative; border: 1px solid var(--sp-node-border); border-radius: 5px; background: var(--sp-bg); padding: 5px 9px; cursor: pointer; min-width: 120px; max-width: 220px; text-align: center; box-shadow: 0 1px 2px var(--sp-shadow); }'
		+ '.ora-plan-box:hover { border-color: var(--sp-sel); }'
		+ '.ora-plan-box.ora-plan-warn { border-color: var(--sp-warn-border); background: var(--sp-warn-bg); }'
		+ '.ora-plan-va { position: absolute; top: 2px; right: 4px; font-size: 0.72em; color: var(--sp-text-7); line-height: 1; }'
		+ '.ora-plan-icon { width: 32px; height: 32px; margin: 0 auto; background-repeat: no-repeat; }'
		+ '.ora-plan-icon-row { position: relative; display: flex; align-items: center; justify-content: center; gap: 2px; }'
		+ '.ora-plan-icon-row .ora-plan-icon { margin: 0; }'
		+ '.ora-plan-label { font-weight: 600; white-space: nowrap; }'
		+ '.ora-plan-subtitle { font-size: 0.85em; color: var(--sp-text-2); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 200px; margin: 0 auto; }'
		+ '.ora-plan-metric { font-size: 0.85em; color: var(--sp-text-3); white-space: nowrap; }'
		+ '.ora-plan-metric.ora-plan-warn-text { color: var(--sp-warn-text-3); font-weight: 600; }'
		+ '.ora-plan-metric-pct-warn { color: var(--sp-err); font-weight: 700; }'
		+ '.ora-plan-detail-pct-warn { color: var(--sp-err); font-weight: 700; }'
		+ '.ora-plan-metric-filter { color: var(--sp-blue-2); font-size: 0.85em; white-space: nowrap; }'
		+ '.ora-plan-detail { position: fixed; z-index: 2000; color: var(--sp-text); background: var(--sp-tt-bg); border: 1px solid var(--sp-tt-border); border-radius: 4px; padding: 6px 10px; min-width: 220px; max-width: 560px; text-align: left; box-shadow: 0 2px 10px var(--sp-shadow-2); font-size: 11px; line-height: 1.35; }'
		+ '.ora-plan-detail { max-height: 80vh; overflow-y: auto; overscroll-behavior: contain; }'
		+ '.ora-plan-detail table { border-collapse: collapse; }'
		+ '.ora-plan-detail-desc { white-space: normal; font-style: italic; color: var(--sp-tt-text); margin-bottom: 6px; padding-bottom: 6px; border-bottom: 1px solid var(--sp-tt-sep); line-height: 1.35; }'
		+ '.ora-plan-detail-idx-hdr { font-size: 10px; color: var(--sp-tt-text); margin-top: 4px; }'
		+ '.ora-plan-detail-wrap { white-space: normal; word-break: break-word; }'
		+ '.ora-plan-detail-hint { margin-top: 5px; padding-top: 4px; border-top: 1px solid var(--sp-tt-sep); color: var(--sp-tt-muted); font-size: 10px; white-space: normal; }'
		+ '.ora-plan-connector-hit { pointer-events: stroke; cursor: help; }'
		+ '.ora-plan-detail td { padding: 1px 6px 1px 0; vertical-align: top; white-space: nowrap; }'
		+ '.ora-plan-detail td.ora-plan-detail-key { color: var(--sp-text-4); }'
		+ '.ora-plan-detail .ora-plan-raw-line { font-family: monospace; white-space: pre-wrap; color: var(--sp-text-2); }'
		+ '.ora-plan-detail.ora-plan-tooltip { pointer-events: none; cursor: default; }'
		+ '.ora-plan-fallback { color: var(--sp-text-5); font-size: 0.85em; font-style: italic; padding: 6px 0; }'
		+ '@keyframes ora-plan-flash { 0%, 100% { box-shadow: 0 1px 2px var(--sp-shadow); } 20%, 60% { box-shadow: 0 0 0 5px rgba(74,144,217,0.85); } 40%, 80% { box-shadow: 0 1px 2px var(--sp-shadow); } }'
		+ '.ora-plan-box.ora-plan-flash { animation: ora-plan-flash 0.8s ease-in-out 5; }'
		+ DbxShowplanGraph.propsCss('ora-plan')
		+ '.ora-plan-box.ora-plan-selected { border-color: var(--sp-blue); border-width: 2px; box-shadow: 0 0 0 3px rgba(42,110,187,0.18); }'
		+ '.ora-plan-prop-raw { font-family: monospace; white-space: pre-wrap; font-size: 10px; color: var(--sp-text-2); line-height: 1.35; }'
		// --- Oracle's own ---
		// Top-to-bottom: the "li:only-child { padding: 0 }" rule above also takes away the 40px between an operation and its
		// ONLY child, so a chain of single operations (SELECT STATEMENT - SORT AGGREGATE - FILTER...) was stacked with no room
		// for the arrow. An Oracle plan nearly always starts with such a chain: give it its space back.
		+ '.ora-plan-tree.ora-plan-compact-v li > ul > li:only-child { padding: 40px 0 0 0; }'
		// An Oracle operation can be long ("TABLE ACCESS BY INDEX ROWID BATCHED"): such a label may take two lines instead of
		// making the box very wide (renderNode() sets the class). A normal one stays on one line, also where the layout would
		// rather make the box narrow (top-to-bottom, where a box is as wide as the row of its children).
		+ '.ora-plan-label.ora-plan-label-long { white-space: normal; min-width: 150px; }'
		// A note that a finding of the Plan Analysis puts on the box of its operation
		+ '.ora-plan-note { white-space: normal; font-weight: 600; }'
		+ '.ora-plan-note-warning, .ora-plan-note-error { color: var(--sp-err); }'
		+ '.ora-plan-note-info { color: var(--sp-blue); }'
		// The sections of the plan text in the Properties pane (Outline Data, Hint Report...): they are lists, keep their lines
		+ '.ora-plan-prop-raw { white-space: pre-wrap; word-break: break-word; }';

	function injectStyle() {
		if (STYLE_INJECTED) return;
		var styleEl = document.createElement('style');
		styleEl.type = 'text/css';
		styleEl.appendChild(document.createTextNode(CSS));
		document.head.appendChild(styleEl);
		STYLE_INJECTED = true;
	}

	// The icons are the sprite sheet of the SQL Server plan viewer (html-query-plan), as in the two other renderers:
	// one 32x32 cell per icon. So a "Nested Loops" looks the same in an Oracle, an ASE and a SQL Server plan.
	var ICON_SPRITE_URL = '/images/qp_icons.png';
	var ICON = {
		Catchall:            [ -96, -256],
		Result:              [-256, -128],
		TableScan:           [ -32, -192],
		RdiLookup:           [   0, -128],
		ClusteredIndexSeek:  [-256,    0],
		IndexSeek:           [-128,  -96],
		IndexScan:           [ -96,  -96],
		Bitmap:              [-256, -192],
		NestedLoops:         [   0,  -96],
		HashMatch:           [ -64,  -64],
		MergeJoin:           [-288,  -64],
		Sort:                [-128, -160],
		StreamAggregate:     [-224, -160],
		WindowAggregate:     [-160, -256],
		Concatenation:       [ -64,  -32],
		Filter:              [   0,  -64],
		Top:                 [-160, -192],
		ComputeScalar:       [ -32,  -32],
		ConstantScan:        [ -96,  -32],
		TableSpool:          [ -64, -192],
		Sequence:            [ -32, -160],
		SequenceProject:     [-224, -224],
		TableValuedFunction: [-128, -192],
		RemoteQuery:         [-128, -128],
		DistributeStreams:   [-224,  -32],
		GatherStreams:       [ -32,  -64],
		RepartitionStreams:  [-224, -128],
		Insert:              [   0, -192],
		Update:              [ -96, -192],
		Delete:              [-288, -160],
		TableMerge:          [ -65, -256]
	};
	// The operation, as DBMS_XPLAN prints it -> icon. In order: the first that matches is used.
	var ICON_RULES = [
		{ test: /^TABLE ACCESS .*BY .*ROWID/,                              icon: 'RdiLookup' },
		{ test: /^TABLE ACCESS (CLUSTER|HASH)/,                            icon: 'ClusteredIndexSeek' },
		{ test: /^(TABLE|MAT_VIEW|MAT_VIEW REWRITE|EXTERNAL TABLE) ACCESS/, icon: 'TableScan' },
		{ test: /^FIXED TABLE FIXED INDEX/,                                icon: 'IndexSeek' },
		{ test: /^FIXED TABLE/,                                            icon: 'TableScan' },
		{ test: /^INDEX (UNIQUE|RANGE) SCAN/,                              icon: 'IndexSeek' },
		{ test: /^INDEX/,                                                  icon: 'IndexScan' },
		{ test: /^(BITMAP|JOIN FILTER|PART JOIN FILTER)/,                  icon: 'Bitmap' },
		{ test: /^NESTED LOOPS/,                                           icon: 'NestedLoops' },
		{ test: /^HASH JOIN/,                                              icon: 'HashMatch' },
		{ test: /^MERGE JOIN/,                                             icon: 'MergeJoin' },
		{ test: /^(HASH|SORT) (GROUP BY|UNIQUE|AGGREGATE)/,                icon: 'StreamAggregate' },
		{ test: /^WINDOW/,                                                 icon: 'WindowAggregate' },
		{ test: /SORT/,                                                    icon: 'Sort' },
		{ test: /^(UNION|CONCATENATION|INTERSECTION|MINUS|EXCEPT)/,        icon: 'Concatenation' },
		{ test: /^FILTER/,                                                 icon: 'Filter' },
		{ test: /^COUNT STOPKEY/,                                          icon: 'Top' },
		{ test: /^(VIEW|COUNT|STATISTICS COLLECTOR)/,                      icon: 'ComputeScalar' },
		{ test: /^PX SEND/,                                                icon: 'DistributeStreams' },
		{ test: /^PX (COORDINATOR|RECEIVE)/,                               icon: 'GatherStreams' },
		{ test: /^PX/,                                                     icon: 'RepartitionStreams' },
		{ test: /^(PARTITION|INLIST ITERATOR)/,                            icon: 'SequenceProject' },
		{ test: /^(TEMP TABLE TRANSFORMATION|RESULT CACHE)/,               icon: 'TableSpool' },
		{ test: /^REMOTE/,                                                 icon: 'RemoteQuery' },
		{ test: /^FAST DUAL/,                                              icon: 'ConstantScan' },
		{ test: /^(COLLECTION ITERATOR|XMLTABLE|JSONTABLE)/,               icon: 'TableValuedFunction' },
		{ test: /^SEQUENCE/,                                               icon: 'Sequence' },
		{ test: /^SELECT STATEMENT/,                                       icon: 'Result' },
		{ test: /^(INSERT STATEMENT|LOAD |MULTI-TABLE INSERT)/,            icon: 'Insert' },
		{ test: /^UPDATE/,                                                 icon: 'Update' },
		{ test: /^DELETE/,                                                 icon: 'Delete' },
		{ test: /^MERGE/,                                                  icon: 'TableMerge' }
	];
	function iconStyleFor(node) {
		var pos = ICON.Catchall;
		for (var i = 0; i < ICON_RULES.length; i++) {
			if (ICON_RULES[i].test.test(node.op)) { pos = ICON[ICON_RULES[i].icon]; break; }
		}
		return 'background-image:url(' + ICON_SPRITE_URL + ');background-position:' + pos[0] + 'px ' + pos[1] + 'px;';
	}

	// What the operation does, in plain words, for the hover panel and the Properties pane.
	// In order: the first that matches is used (so the special cases are above the general ones).
	var OPERATION_DESCRIPTIONS = [
		{ test: /STATEMENT$/,                         text: 'The statement itself. Its Cost is the cost of the whole plan, and (when the plan has run-time statistics) its A-Rows, A-Time and Buffers are those of the whole execution.' },
		{ test: /^TABLE ACCESS .*FULL/,               text: 'Full table scan: every block of the table (up to its high water mark) is read, several blocks per read. The right choice when a large part of the table is needed, expensive when only a few rows are.' },
		{ test: /^TABLE ACCESS BY .*ROWID BATCHED/,   text: 'Fetches rows from the table with the ROWIDs that the index operation below it returned. BATCHED (12c and above): a number of ROWIDs are collected and sorted by block first, so the table blocks are read with less random I/O.' },
		{ test: /^TABLE ACCESS BY USER ROWID/,        text: 'Fetches rows from the table by a ROWID that the statement itself supplies. The fastest way to a row.' },
		{ test: /^TABLE ACCESS BY .*ROWID/,           text: 'Fetches rows from the table, one at a time, with the ROWIDs that the index operation below it returned. One table block visit per row: cheap for a few rows, expensive for many.' },
		{ test: /^TABLE ACCESS (CLUSTER|HASH)/,       text: 'Reads rows of a table that is stored in a cluster, by the cluster key.' },
		{ test: /^TABLE ACCESS SAMPLE/,               text: 'Reads a random sample of the table (the SAMPLE clause).' },
		{ test: /^(MAT_VIEW|MAT_VIEW REWRITE) ACCESS/, text: 'Reads a materialized view (a stored query result). REWRITE: Oracle replaced part of the query with the materialized view.' },
		{ test: /^EXTERNAL TABLE ACCESS/,             text: 'Reads a file outside the database through an external table. There are no indexes: the file is always read from start to end.' },
		{ test: /^FIXED TABLE/,                       text: 'Reads an internal memory structure of the instance (an X$ table, what the V$ views are built on). FIXED INDEX: by one of its built-in keys. FULL: the whole structure.' },
		{ test: /^INDEX UNIQUE SCAN/,                 text: 'Looks up one key in a unique index: at most one ROWID comes back.' },
		{ test: /^INDEX RANGE SCAN \(MIN\/MAX\)/,     text: 'Reads only the first or the last index entry of the range, to get a MIN or a MAX without reading the rest.' },
		{ test: /^INDEX RANGE SCAN/,                  text: 'Goes down the index to the first matching key, then reads the leaf blocks in key order for as long as the keys match. The rows come back sorted on the index key. DESCENDING: the same, from the last key and backwards.' },
		{ test: /^INDEX FULL SCAN \(MIN\/MAX\)/,      text: 'Reads only the first or the last entry of the index, to get a MIN or a MAX.' },
		{ test: /^INDEX FULL SCAN/,                   text: 'Reads the whole index in key order, one block at a time. Used to avoid a sort, or when the index holds all the columns that are needed.' },
		{ test: /^INDEX FAST FULL SCAN/,              text: 'Reads all blocks of the index, several blocks per read and in no order: the index is used as a narrow copy of the table. The table itself is not visited.' },
		{ test: /^INDEX SKIP SCAN/,                   text: 'Uses a multi-column index although its first column is not in the WHERE clause: one probe for each distinct value of the first column. Works when that column has few distinct values; an index that starts with the column in the predicate is normally better.' },
		{ test: /^INDEX/,                             text: 'Reads an index.' },
		{ test: /^BITMAP CONVERSION/,                 text: 'Converts between ROWIDs and bitmaps, so that the results of several indexes can be combined (BITMAP AND / OR) before the table is visited.' },
		{ test: /^BITMAP/,                            text: 'Works on bitmaps (one bit per row): reads a bitmap index, or combines the bitmaps of several indexes with AND / OR / MINUS.' },
		{ test: /^NESTED LOOPS/,                      text: 'For each row from the first child (the outer, driving row source) the second child (the inner) is executed once. Good when the outer side returns few rows and the inner side is an index lookup; slow when the outer side returns many rows. In 11g and above the join is often shown as two NESTED LOOPS: the inner one joins to the index, the outer one fetches the table rows.' },
		{ test: /^HASH JOIN/,                         text: 'Builds a hash table in memory from the rows of the first child (the build input, which should be the smaller one), then reads the second child once and looks each row up in the hash table. No index is needed. When the hash table does not fit in the PGA work area it is written to the temporary tablespace.' },
		{ test: /^MERGE JOIN CARTESIAN/,              text: 'A join WITHOUT a join condition: every row of the first child is combined with every row of the second. Harmless when one of the two returns a single row, a disaster when both return many.' },
		{ test: /^MERGE JOIN/,                        text: 'Both inputs are sorted on the join key (the SORT JOIN operations below, unless an index already returns the rows in order) and then merged. Used for joins on a range (<, >) or when the rows are already sorted.' },
		{ test: /^SORT AGGREGATE/,                    text: 'Calculates an aggregate (COUNT, SUM, MIN, MAX...) over all rows, without a GROUP BY: one row out. Despite its name nothing is sorted.' },
		{ test: /^SORT GROUP BY NOSORT/,              text: 'GROUP BY on rows that already arrive in the right order: no sort is needed.' },
		{ test: /^(SORT|HASH) GROUP BY/,              text: 'Groups the rows for a GROUP BY and calculates the aggregates. HASH: with a hash table (the result is not sorted). SORT: by sorting.' },
		{ test: /^(SORT|HASH) UNIQUE/,                text: 'Removes duplicate rows (DISTINCT, UNION, or a subquery that was turned into a join).' },
		{ test: /^SORT ORDER BY STOPKEY/,             text: 'A "top N" sort: the rows are sorted for an ORDER BY, but only the first N (ROWNUM / FETCH FIRST) are kept, so it needs far less memory than a full sort.' },
		{ test: /^SORT ORDER BY/,                     text: 'Sorts the rows for the ORDER BY. In memory when they fit in the PGA work area, otherwise with the temporary tablespace.' },
		{ test: /^SORT JOIN/,                         text: 'Sorts one input of a MERGE JOIN on the join key.' },
		{ test: /^BUFFER SORT/,                       text: 'Keeps the rows of its child in memory, so that they do not have to be produced again each time they are needed (for example once per row of the other side of a Cartesian join). Nothing is really sorted.' },
		{ test: /^WINDOW/,                            text: 'Calculates an analytic function (a function with OVER (...)). SORT: the rows have to be sorted first. NOSORT / BUFFER: they already arrive in the right order. PUSHED RANK / STOPKEY: only the first N rows of each group are kept.' },
		{ test: /^FILTER/,                            text: 'With ONE child: lets the rows through that pass its predicate. With SEVERAL children: for each row of the first child the other children (subqueries) are executed, like a nested loop; Oracle remembers the result for values it has just seen, so the real number of executions is one per distinct value at best and one per row at worst.' },
		{ test: /^VIEW/,                              text: 'The result of a query block that is used as it is: an inline view, a view that could not be merged into the query, a WITH subquery, or a view the optimizer made up itself (names like VW_..., index$_join$_...).' },
		{ test: /^UNION-ALL|^UNION ALL/,              text: 'Returns the rows of all its children, one child after the other (UNION ALL).' },
		{ test: /^CONCATENATION/,                     text: 'An OR was split into several branches (OR-expansion), so that each branch can use its own index. The children are executed one after the other and their rows are put together.' },
		{ test: /^(INTERSECTION|MINUS|EXCEPT)/,       text: 'Set operation on its two (sorted) inputs: INTERSECTION keeps the rows that are in both, MINUS / EXCEPT the rows of the first that are not in the second.' },
		{ test: /^COUNT STOPKEY/,                     text: 'Stops asking its child for rows as soon as the ROWNUM limit is reached.' },
		{ test: /^COUNT/,                             text: 'Numbers the rows (ROWNUM).' },
		{ test: /^PARTITION .* SINGLE/,               text: 'Only ONE partition is read (partition pruning). Pstart / Pstop show which one.' },
		{ test: /^PARTITION .* ALL/,                  text: 'ALL partitions are read: its child is executed once for each partition. There is no predicate on the partition key that could leave partitions out.' },
		{ test: /^PARTITION/,                         text: 'Its child is executed once for each partition that is needed. Pstart / Pstop show which: a number is a partition that was known when the statement was parsed, KEY means it is decided when the statement runs.' },
		{ test: /^INLIST ITERATOR/,                   text: 'Executes its child once for each value of an IN (...) list.' },
		{ test: /^TEMP TABLE TRANSFORMATION/,         text: 'A WITH subquery is executed once, its result is stored in a temporary table (the LOAD AS SELECT below), and the rest of the query reads it from there.' },
		{ test: /^LOAD AS SELECT/,                    text: 'Writes the rows of its child with a direct path load (INSERT /*+ APPEND */, CREATE TABLE AS SELECT, or the temporary table of a WITH subquery).' },
		{ test: /^LOAD TABLE CONVENTIONAL/,           text: 'Inserts the rows into the table the normal way (through the buffer cache).' },
		{ test: /^(UPDATE|DELETE|MERGE)$/,            text: 'Changes the rows that its child returns.' },
		{ test: /^FOR UPDATE/,                        text: 'Locks the rows that its child returns (SELECT ... FOR UPDATE).' },
		{ test: /^CONNECT BY/,                        text: 'A hierarchical query (CONNECT BY): starts with the root rows, then repeatedly finds the rows of the next level.' },
		{ test: /^FAST DUAL/,                         text: 'A SELECT from DUAL: Oracle returns the one row without reading anything.' },
		{ test: /^SEQUENCE/,                          text: 'Gets values from a sequence (NEXTVAL).' },
		{ test: /^REMOTE/,                            text: 'This part is executed in ANOTHER database, over a database link. What happens there is not in this plan.' },
		{ test: /^RESULT CACHE/,                      text: 'The result of this part is kept in the result cache of the instance: when it is asked for again (and the tables have not changed) it is returned without executing the operations below.' },
		{ test: /^(COLLECTION ITERATOR|XMLTABLE|JSONTABLE)/, text: 'The rows come from a function or a collection (TABLE(...), XMLTABLE, JSON_TABLE), not from a table. The optimizer normally has to guess the number of rows.' },
		{ test: /^(JOIN FILTER|PART JOIN FILTER)/,    text: 'A Bloom filter: CREATE builds it from the join keys of one side of a hash join, USE applies it on the other side (or on its partitions), so that rows that cannot match are thrown away early.' },
		{ test: /^STATISTICS COLLECTOR/,              text: 'Adaptive plan: counts the rows that pass, and with that Oracle decides during the first execution between a nested loops join and a hash join.' },
		{ test: /^PX COORDINATOR/,                    text: 'The query coordinator of a parallel statement: starts the parallel execution servers and receives their result. Everything below it runs in parallel.' },
		{ test: /^PX SEND/,                           text: 'Parallel execution: sends rows to the next set of processes. The word after SEND is how the rows are distributed (HASH, RANGE, BROADCAST, ROUND-ROBIN; QC = to the coordinator).' },
		{ test: /^PX RECEIVE/,                        text: 'Parallel execution: receives the rows that a PX SEND sent.' },
		{ test: /^PX (BLOCK|PARTITION)/,              text: 'Parallel execution: divides the table into pieces (ranges of blocks, or partitions) and gives each parallel execution server its own piece.' }
	];
	function operationDescriptionFor(node) {
		for (var i = 0; i < OPERATION_DESCRIPTIONS.length; i++) {
			if (OPERATION_DESCRIPTIONS[i].test.test(node.op)) return OPERATION_DESCRIPTIONS[i].text;
		}
		return undefined;
	}

	// The IN-OUT column of a parallel plan
	var IN_OUT_TEXT = {
		'P->P': 'parallel to parallel: rows go from one set of parallel execution servers to another',
		'P->S': 'parallel to serial: the parallel execution servers send their rows to the coordinator',
		'S->P': 'serial to parallel: ONE process produces the rows and hands them to the parallel execution servers (this part is not parallel)',
		'PCWP': 'parallel, in the same process as its parent',
		'PCWC': 'parallel, in the same process as its child'
	};

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
		return (Math.round(s * 100) / 100) + ' s';
	}
	function fmtBytes(b) {
		if (b === undefined || b === null || isNaN(b)) return undefined;
		if (b >= 1e9) return (Math.round(b / 1e8) / 10) + ' GB';
		if (b >= 1e6) return (Math.round(b / 1e5) / 10) + ' MB';
		if (b >= 1e3) return (Math.round(b / 1e2) / 10) + ' KB';
		return b + ' bytes';
	}

	// The estimate is called wrong when it is 10 times off (and it is about more than a handful of rows)
	function isEstActWarn(m) {
		if (m.estRowsTotal === undefined || m.actRows === undefined) return false;
		if (Math.max(m.estRowsTotal, m.actRows) < 100) return false;
		if (m.stoppedEarly && m.actRows < m.estRowsTotal) return false;   // it was stopped when there were enough rows
		if (m.estRowsTotal <= 0 || m.actRows <= 0) return true;
		var ratio = m.actRows / m.estRowsTotal;
		return ratio >= 10 || ratio <= 0.1;
	}
	/** "12x more", "40x fewer", "no rows". Nothing while the estimate is within a factor of 2: that is as good as an estimate gets. */
	function fmtEstActDiff(m) {
		if (m.estRowsTotal === undefined || m.actRows === undefined) return undefined;
		if (m.actRows === 0)      return m.estRowsTotal >= 10 ? 'no rows' : undefined;
		if (m.estRowsTotal <= 0)  return 'estimate was 0';
		var ratio = m.actRows / m.estRowsTotal;
		var times = function (x) { return x < 10 ? String(Math.round(x * 10) / 10) : fmtNum(Math.round(x)); };
		if (ratio >= 2)   return times(ratio) + 'x more';
		if (ratio <= 0.5) return times(1 / ratio) + 'x fewer';
		return undefined;
	}

	/** The object, with its alias in the statement when that says more than the name ("ORDERS (O2)" in a self join) */
	function subtitleFor(node) {
		var p = node.props;
		if (!p.objName) return undefined;
		var alias = aliasName(p.alias);
		return (alias && alias.toUpperCase() !== p.objName.toUpperCase()) ? p.objName + ' (' + alias + ')' : p.objName;
	}
	/** '"O1"@"SEL$1"' -> 'O1' */
	function aliasName(alias) {
		var m = /^"?([^"@]+)"?@/.exec(alias || '');
		return m ? m[1] : undefined;
	}
	function nodeTitle(node) {
		var sub = subtitleFor(node);
		return node.op + (sub ? ' ' + sub : '');
	}

	function propRow($into, key, val)  { return DbxShowplanGraph.propRow($into, 'ora-plan', key, val); }
	function propSection($into, title) { return DbxShowplanGraph.propSection($into, 'ora-plan', title); }
	function propSectionIfAny($into, title, fill) {
		var $tmp = $('<div></div>');
		fill($tmp);
		if ($tmp.children().length) propSection($into, title).append($tmp.children());
	}
	function propText($into, text) {
		if (text) $into.append($('<div class="ora-plan-prop-raw"></div>').text(text));
	}

	// ─────────────────────────────────────────────────────────────────────────
	// Properties pane: everything the plan text says about ONE operation
	// ─────────────────────────────────────────────────────────────────────────
	function renderPropertiesInto(container, node) {
		var $c = $(container);
		$c.empty();
		if (!node) {
			$c.append($('<div class="ora-plan-prop-empty"></div>')
				.text('Click an operation in the plan to see all of its properties here.'));
			return;
		}
		var p = node.props, m = node.metrics;

		$c.append($('<div class="ora-plan-prop-title"></div>').text(node.op));
		var sub = subtitleFor(node);
		if (sub) $c.append($('<div class="ora-plan-prop-subtitle"></div>').text(sub));
		var desc = operationDescriptionFor(node);
		if (desc) $c.append($('<div class="ora-plan-prop-desc"></div>').text(desc));
		(node._boxNotes || []).forEach(function (n) {
			$c.append($('<div class="ora-plan-prop-warn"></div>').text('⚠ ' + n.title));
		});

		var $op = propSection($c, 'Operation');
		propRow($op, 'Id',          p.va);
		propRow($op, 'Operation',   node.op);
		propRow($op, 'Object',      p.objName);
		propRow($op, 'Alias',       p.alias);
		propRow($op, 'Query block', p.qblock);

		propSectionIfAny($c, 'Predicates', function ($s) {
			[['Access', p.access], ['Storage', p.storage], ['Filter', p.filter]].forEach(function (kv) {
				(kv[1] || []).forEach(function (text) { propRow($s, kv[0], text); });
			});
		});

		propSectionIfAny($c, 'Optimizer estimate', function ($s) {
			propRow($s, 'Rows',                 fmtNum(m.estRows));
			if (m.starts > 1) propRow($s, 'Rows x Starts', fmtNum(m.estRowsTotal));
			propRow($s, 'Bytes',                fmtBytes(m.estBytes));
			propRow($s, 'Cost (with children)', fmtNum(m.cost));
			propRow($s, 'Cost (own)',           m.ownCost === undefined ? undefined : fmtNum(m.ownCost) + (m.ownCostPct === undefined ? '' : ' = ' + fmtPct(m.ownCostPct) + ' of the plan'));
			propRow($s, 'CPU part of the cost', m.cpuPct === undefined ? undefined : m.cpuPct + '%');
			propRow($s, 'Time',                 fmtSec(m.estTimeSec));
			propRow($s, 'Temp space',           fmtBytes(m.tempSpc));
		});

		propSectionIfAny($c, 'Actual (run-time statistics)', function ($s) {
			propRow($s, 'Starts (executions)',  fmtNum(m.starts));
			propRow($s, 'Rows',                 fmtNum(m.actRows));
			propRow($s, 'Rows vs estimate',     fmtEstActDiff(m));
			propRow($s, 'Time (with children)', fmtSec(m.actTimeSec));
			propRow($s, 'Time (own)',           m.ownActTimeSec === undefined ? undefined : fmtSec(m.ownActTimeSec) + (m.ownActTimePct === undefined ? '' : ' = ' + fmtPct(m.ownActTimePct) + ' of the plan'));
			propRow($s, 'Buffers (with children)', fmtNum(m.buffers));
			propRow($s, 'Buffers (own)',        m.ownBuffers === undefined ? undefined : fmtNum(m.ownBuffers) + (m.ownBuffersPct === undefined ? '' : ' = ' + fmtPct(m.ownBuffersPct) + ' of the plan'));
			propRow($s, 'Physical reads',       fmtNum(m.reads));
			propRow($s, 'Physical writes',      fmtNum(m.writes));
			propRow($s, 'Memory used',          m.usedMem);
			propRow($s, 'Temp used',            m.usedTmp);
		});

		propSectionIfAny($c, 'Partitions', function ($s) {
			propRow($s, 'Pstart', p.pstart);
			propRow($s, 'Pstop',  p.pstop);
		});
		propSectionIfAny($c, 'Parallel execution', function ($s) {
			propRow($s, 'Table queue (TQ)', p.tq);
			propRow($s, 'IN-OUT',           p.inOut ? p.inOut + (IN_OUT_TEXT[p.inOut] ? ' = ' + IN_OUT_TEXT[p.inOut] : '') : undefined);
			propRow($s, 'Distribution',     p.pqDistrib);
		});
		propSectionIfAny($c, 'Columns it returns (projection)', function ($s) { propText($s, p.projection); });

		// On the statement itself: what is said about the whole plan
		var pi = node._planInfo;
		if (pi) {
			var $pi = propSection($c, 'Plan information');
			propRow($pi, 'SQL_ID',          pi.sqlId);
			propRow($pi, 'Child number',    pi.childNumber);
			propRow($pi, 'Plan hash value', pi.planHash);
			propRow($pi, 'Operations',      pi.operations);
			propRow($pi, 'Not drawn',       pi.inactive ? pi.inactive + ' inactive operations of the adaptive plan' : undefined);
			pi.notes.forEach(function (n, i) { propRow($pi, i === 0 ? 'Note' : '', n); });
			// The sections that are not shown per operation (Outline Data, Peeked Binds, Hint Report...)
			Object.keys(pi.sections).forEach(function (t) {
				if (/^(Predicate Information|Column Projection Information|Query Block Name|Note$)/i.test(t) || !pi.sections[t]) return;
				propText(propSection($c, t.replace(/\s*\(identified by.*$/i, '').replace(/:$/, '')), pi.sections[t]);
			});
		}

		// Every column of the plan line, as it was printed: nothing in the text is hidden
		var $all = propSection($c, 'The plan line');
		Object.keys(p.cells).forEach(function (h) { propRow($all, h, p.cells[h]); });
	}

	/** Hover text of the arrow between two operations: what flows along it (the rows of the CHILD) */
	function connectorTooltip(childNode, parentNode) {
		if (!childNode) return undefined;
		var m = childNode.metrics;
		var lines = [ nodeTitle(childNode) + '  →  ' + (parentNode ? parentNode.op : ''), '' ];
		if (m.actRows !== undefined)      lines.push('Actual rows:      ' + fmtNum(m.actRows));
		if (m.estRowsTotal !== undefined) lines.push('Estimated rows:   ' + fmtNum(m.estRowsTotal) + (m.starts > 1 ? '  (' + fmtNum(m.estRows) + ' x ' + fmtNum(m.starts) + ' starts)' : ''));
		var diff = fmtEstActDiff(m);
		if (diff !== undefined)           lines.push('Actual vs est:    ' + diff);
		if (m.estBytes !== undefined)     lines.push('Estimated size:   ' + fmtBytes(m.estBytes));
		return lines.length > 2 ? lines.join('\n') : undefined;
	}

	// ─────────────────────────────────────────────────────────────────────────
	// The hover panel (click = pinned): the short version of the Properties pane
	// ─────────────────────────────────────────────────────────────────────────
	function buildDetailPanel(node) {
		var p = node.props, m = node.metrics;
		var $panel = $('<div class="ora-plan-detail"></div>');

		var description = operationDescriptionFor(node);
		if (description) $panel.append($('<div class="ora-plan-detail-desc"></div>').text(description));

		var $tbl = $('<table></table>');
		function row(key, val, valClass) {
			if (val === undefined || val === null || val === '') return;
			var $val = $('<td></td>').text(val);
			if (valClass) $val.addClass(valClass);
			$tbl.append($('<tr></tr>').append($('<td class="ora-plan-detail-key"></td>').text(key)).append($val));
		}
		row('Operation', node.op);
		row('Id',        p.va);
		row('Object',    subtitleFor(node));
		row('Query block', p.qblock);
		if (m.actRows !== undefined) {
			row('Starts',        fmtNum(m.starts));
			row('Actual rows',   fmtNum(m.actRows));
			row('Est. rows',     m.estRowsTotal === undefined ? undefined : fmtNum(m.estRowsTotal) + (m.starts > 1 ? '  (' + fmtNum(m.estRows) + ' x ' + fmtNum(m.starts) + ' starts)' : ''));
			row('Actual vs est', fmtEstActDiff(m), isEstActWarn(m) ? 'ora-plan-detail-pct-warn' : undefined);
			row('Time',          m.actTimeSec === undefined ? undefined : fmtSec(m.actTimeSec) + (m.ownActTimePct === undefined ? '' : '  (own: ' + fmtSec(m.ownActTimeSec) + ' = ' + fmtPct(m.ownActTimePct) + ')'));
			row('Buffers',       m.buffers === undefined ? undefined : fmtNum(m.buffers) + (m.ownBuffersPct === undefined ? '' : '  (own: ' + fmtNum(m.ownBuffers) + ' = ' + fmtPct(m.ownBuffersPct) + ')'));
			row('Physical reads', fmtNum(m.reads));
			row('Memory used',   m.usedMem);
			row('Temp used',     m.usedTmp);
		} else {
			row('Est. rows',     fmtNum(m.estRows));
		}
		row('Est. size',  fmtBytes(m.estBytes));
		row('Cost',       m.cost === undefined ? undefined : fmtNum(m.cost) + (m.ownCostPct === undefined ? '' : '  (own: ' + fmtNum(m.ownCost) + ' = ' + fmtPct(m.ownCostPct) + ')'));
		row('Est. time',  fmtSec(m.estTimeSec));
		row('Temp space', fmtBytes(m.tempSpc));
		row('Partitions', p.pstart !== undefined ? p.pstart + ' - ' + p.pstop : undefined);
		row('Parallel',   p.inOut ? p.inOut + (p.tq ? ', ' + p.tq : '') + (p.pqDistrib ? ', ' + p.pqDistrib : '') : undefined);
		$panel.append($tbl);

		// The predicates can be long: full width and wrapping, below the table
		[['Access predicate', p.access], ['Storage predicate', p.storage], ['Filter predicate', p.filter]].forEach(function (kv) {
			(kv[1] || []).forEach(function (text) {
				$panel.append($('<div class="ora-plan-detail-idx-hdr"></div>').text(kv[0] + ':'));
				$panel.append($('<div class="ora-plan-detail-wrap ora-plan-raw-line"></div>').text(text));
			});
		});
		(node._boxNotes || []).forEach(function (n) {
			$panel.append($('<div class="ora-plan-detail-idx-hdr ora-plan-detail-pct-warn"></div>').text('⚠ ' + n.title));
		});

		$panel.append($('<div class="ora-plan-detail-hint"></div>')
			.text('Click the box to pin this, and to show all properties in the Properties pane.'));
		return $panel;
	}

	// The detail panels are fixed-position overlays on <body>, handled by the shared panel system (dbxShowplanGraph.js)
	var _panels = DbxShowplanGraph.createPanelSystem({
		prefix:     'ora-plan',
		buildPanel: function (node) { return buildDetailPanel(node); }
	});

	// Set by render(), so that a click on a box can show the operation in the Properties pane
	var _propsTarget = null;
	var _selectedBox = null;

	function selectNode($box, node) {
		if (_selectedBox) _selectedBox.removeClass('ora-plan-selected');
		_selectedBox = $box;
		$box.addClass('ora-plan-selected');
		if (_propsTarget) renderPropertiesInto(_propsTarget, node);
	}

	function renderNode(node) {
		var p = node.props, m = node.metrics;
		var $li  = $('<li></li>');
		var $box = $('<div class="ora-plan-box"></div>');
		var warn = isEstActWarn(m);
		if (warn || (node._boxNotes || []).some(function (n) { return n.severity !== 'info'; })) $box.addClass('ora-plan-warn');
		$box[0].__oraPlanNode = node;   // for drawConnectorLines(), which walks the DOM and not the model

		// The Id is what a "[Node N]" in the Plan Analysis section jumps to (oraShowplanJumpToNode() in dbxShowplan.js)
		$box.attr('data-va', p.va);
		$box.append($('<div class="ora-plan-va" title="Operation Id (the Id column of the plan)"></div>').text(p.va));

		$box.append($('<div class="ora-plan-icon-row"></div>')
			.append($('<div class="ora-plan-icon"></div>').attr('style', iconStyleFor(node))));
		$box.append($('<div class="ora-plan-label"></div>').toggleClass('ora-plan-label-long', node.label.length > 24).text(node.label));
		var subtitle = subtitleFor(node);
		if (subtitle) $box.append($('<div class="ora-plan-subtitle"></div>').attr('title', subtitle).text(subtitle));

		// Rows: "Est 1 / Act 2,207 (2,207x more)" when the plan has run-time statistics, else the estimate
		if (m.actRows !== undefined) {
			var $rows = $('<div class="ora-plan-metric"></div>');
			if (warn) $rows.addClass('ora-plan-warn-text');
			$rows.text((m.estRowsTotal === undefined ? '' : 'Est ' + fmtNum(m.estRowsTotal) + ' / ') + 'Act ' + fmtNum(m.actRows));
			var diff = fmtEstActDiff(m);
			if (diff !== undefined) {
				var $diff = $('<span></span>').text(' (' + diff + ')');
				if (warn) $diff.addClass('ora-plan-metric-pct-warn');
				$rows.append($diff);
			}
			$box.append($rows);
		} else if (m.estRows !== undefined) {
			$box.append($('<div class="ora-plan-metric"></div>').text('Rows ' + fmtNum(m.estRows)));
		}

		// Cost and time: Oracle's numbers include the children, so the part that is the operation's own is shown too
		// (in red when this one operation is half of the plan or more). Not on the statement line: what is left there
		// after its children is rounding, not work.
		function metricWithOwnPct(text, ownPct) {
			var $m = $('<div class="ora-plan-metric"></div>').text(text);
			if (ownPct !== undefined && ownPct >= 1 && !node._planInfo) {
				var $own = $('<span></span>').text(' (own ' + fmtPct(ownPct) + ')');
				if (ownPct >= 50) $own.addClass('ora-plan-metric-pct-warn');
				$m.append($own);
			}
			$box.append($m);
		}
		if (m.cost       !== undefined) metricWithOwnPct('Cost ' + fmtNum(m.cost),       m.ownCostPct);
		if (m.actTimeSec !== undefined) metricWithOwnPct('Time ' + fmtSec(m.actTimeSec), m.ownActTimePct);

		(node._boxNotes || []).forEach(function (n) {
			$box.append($('<div class="ora-plan-metric ora-plan-note ora-plan-note-' + n.severity + '"></div>')
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

		// The invisible "cell" around the box: see the CSS (.ora-plan-box-cell)
		$li.append($('<div class="ora-plan-box-cell"></div>').append($box));

		if (node.children.length) {
			var $childUl = $('<ul></ul>');
			node.children.forEach(function (child) { $childUl.append(renderNode(child)); });
			$li.append($childUl);
		}
		return $li;
	}

	// The arrows between the boxes: an SVG overlay, drawn from the measured positions of the boxes after the browser
	// has laid them out. This is drawConnectorLines() of dbxShowplanAse.js (where each step is explained), with the class prefix changed.
	var _arrowMarkerSeq = 0;
	function drawConnectorLines(treeEl, horizontal) {
		var previousSvg = treeEl.querySelector(':scope > svg.ora-plan-connector-svg');
		if (previousSvg) previousSvg.remove();

		var svgNS = 'http://www.w3.org/2000/svg';
		var containerRect = treeEl.getBoundingClientRect();
		var svg = document.createElementNS(svgNS, 'svg');
		svg.setAttribute('class', 'ora-plan-connector-svg');
		svg.setAttribute('width', treeEl.scrollWidth);
		svg.setAttribute('height', treeEl.scrollHeight);

		var markerId = 'ora-plan-arrow-' + (++_arrowMarkerSeq);
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
		arrowHead.style.fill = 'var(--sp-edge)';   // style (not the attribute): SVG attributes cannot use var()
		marker.appendChild(arrowHead);
		defs.appendChild(marker);
		svg.appendChild(defs);

		var allBoxRects = Array.prototype.map.call(treeEl.querySelectorAll('.ora-plan-box'), function (b) {
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
			var box = li.querySelector(':scope > .ora-plan-box-cell > .ora-plan-box');
			var parentBox = parentLi.querySelector(':scope > .ora-plan-box-cell > .ora-plan-box');
			if (!box || !parentBox) return;

			var r  = box.getBoundingClientRect();
			var pr = parentBox.getBoundingClientRect();

			var siblingUl = li.parentElement;
			var siblings = siblingUl ? Array.prototype.slice.call(siblingUl.children) : [li];
			var siblingIndex = siblings.indexOf(li);
			var siblingCount = siblings.length;
			var spread = (siblingIndex + 1) / (siblingCount + 1); // e.g. 2 siblings -> 1/3, 2/3

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

			var tip = connectorTooltip(box.__oraPlanNode, parentBox.__oraPlanNode);
			if (tip) {
				var hit = document.createElementNS(svgNS, 'path');
				hit.setAttribute('d', d);
				hit.setAttribute('fill', 'none');
				hit.setAttribute('stroke', 'transparent');
				hit.setAttribute('stroke-width', '12');
				hit.setAttribute('class', 'ora-plan-connector-hit');
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
	// Everything here comes from the plan text alone (no lookups in the database), so each finding says what the
	// plan shows and what to check, not what is wrong. Returned as the findings array the dialog's "Plan Analysis"
	// section draws (dbxShowplan.js, the same list the ASE and SQL Server dialogs use). A finding can also put a
	// short note on the box of its operation (node._boxNotes, drawn by renderNode()).
	// ─────────────────────────────────────────────────────────────────────────

	// The functions that convert a data type. On a COLUMN in a predicate they are either written in the SQL or added by
	// Oracle because the types differ (INTERNAL_FUNCTION and SYS_OP_C2C are always added by Oracle).
	var CONVERSION_FUNCTIONS = /^(INTERNAL_FUNCTION|SYS_OP_C2C|TO_NUMBER|TO_CHAR|TO_NCHAR|TO_DATE|TO_TIMESTAMP|TO_TIMESTAMP_TZ|TO_BINARY_DOUBLE|TO_BINARY_FLOAT|CAST|RAWTOHEX|HEXTORAW)$/;
	// Not functions, although a '(' can follow them directly
	var PREDICATE_KEYWORDS   = /^(AND|OR|NOT|IN|EXISTS|IS|LIKE|BETWEEN|ANY|ALL|SOME|CASE|WHEN|THEN|ELSE)$/;

	// The lines of the 'Note' section that deserve an explanation. The first that matches is used; every other note is shown as it is.
	var NOTE_RULES = [
		{ test: /dynamic (statistics|sampling) used.*level=2\b/i, severity: 'warning',
		  text: 'Level 2 is what Oracle does by itself when a table in the statement has NO optimizer statistics: it samples the table while parsing. Gather statistics on the tables of this statement (DBMS_STATS).' },
		{ test: /dynamic (statistics|sampling) used/i, severity: 'info',
		  text: 'Oracle sampled the data while parsing, because it did not trust the stored statistics for this statement (missing statistics, a hint, parallel execution, or a SQL plan directive).' },
		{ test: /adaptive plan/i, severity: 'info',
		  text: 'The optimizer prepared two alternatives for a join (nested loops or hash join) and chose between them during the first execution, from the number of rows it really got. The plan shown is the one that was chosen.' },
		{ test: /(statistics|cardinality) feedback used/i, severity: 'info',
		  text: 'An earlier execution of this statement showed that the row estimates were wrong, so Oracle parsed it again with the row counts it had seen. A sign that the statistics do not describe the data well.' },
		{ test: /SQL plan baseline .* used/i, severity: 'info',
		  text: 'The plan is fixed by a SQL plan baseline (SQL Plan Management): the optimizer may only use the plans that were accepted for this statement. A better plan can exist but is not used until it is accepted (DBA_SQL_PLAN_BASELINES).' },
		{ test: /SQL profile .* used/i, severity: 'info',
		  text: 'A SQL profile corrects the estimates of the optimizer for this statement (DBA_SQL_PROFILES). Someone created it, normally with the SQL Tuning Advisor (which is part of the Tuning Pack: an extra license from Oracle), and it stays in use until someone drops it.' },
		{ test: /SQL patch .* used/i, severity: 'info',
		  text: 'A SQL patch adds hints to this statement (DBA_SQL_PATCHES).' },
		{ test: /outline .* used/i, severity: 'info',
		  text: 'The plan is fixed by a stored outline (the old way to fix a plan, before SQL plan baselines).' },
		{ test: /basic plan statistics not available/i, severity: 'info',
		  text: 'The plan was asked for with run-time statistics (ALLSTATS), but Oracle did not collect any for this statement: the Starts / A-Rows / A-Time columns are missing or empty. They are collected when the statement has the hint /*+ gather_plan_statistics */, or when STATISTICS_LEVEL = ALL is set for the session.' },
		{ test: /rule based optimizer used/i, severity: 'warning',
		  text: 'The statement was optimized with fixed rules instead of with statistics (the RULE hint, or OPTIMIZER_MODE = RULE). Oracle has not supported that for many versions.' },
		{ test: /PLAN_TABLE.* is old version/i, severity: 'warning',
		  text: 'The PLAN_TABLE this plan was read from was created by an older Oracle version, so columns are missing in it (predicates, for one). Drop it: Oracle then uses its own, current one.' },
		{ test: /Degree of Parallelism|automatic DOP|parallel/i, severity: 'info', text: '' },
		{ test: /direct load disabled|PDML (is )?disabled/i, severity: 'info',
		  text: 'The fast way to write (direct path load / parallel DML) was asked for but could not be used, for the reason given.' }
	];

	function collectFindings(parsed) {
		var findings = [];
		var manyPlans = parsed.statements.length > 1;

		parsed.statements.forEach(function (stmt) {
			var root = stmt.steps[0].root;
			var pi   = root._planInfo;
			var planTag = manyPlans && pi.childNumber !== undefined ? ' (child number ' + pi.childNumber + ')' : '';

			function add(node, severity, category, title, detail, boxNote) {
				findings.push({ severity: severity, category: category, title: title + planTag, detail: detail,
				                nodeId: node ? node.props.va : null, nodeName: node ? node.props.objName : undefined });
				if (node && boxNote) (node._boxNotes = node._boxNotes || []).push({ severity: severity, title: title, boxNote: boxNote });
			}
			// Rows of ONE execution of the operation: what it really returned when the plan has run-time statistics, else the estimate
			function rowsPerStart(n) {
				var m = n.metrics;
				if (m.actRows !== undefined) return m.actRows / (m.starts > 1 ? m.starts : 1);
				return m.estRows;
			}
			function isFullScan(n) { return /^(TABLE ACCESS .*FULL|MAT_VIEW ACCESS FULL|MAT_VIEW REWRITE ACCESS FULL)/.test(n.op); }
			var fullScansInLoops = [];

			// A finding can put its note on ANOTHER operation than the one being looked at (a nested loop marks its inner scan),
			// so the notes of an earlier call are cleared before the walk, not during it
			DbxShowplanGraph.walkPlanNodes(root, function (node) { node._boxNotes = undefined; });

			DbxShowplanGraph.walkPlanNodes(root, function (node) {
				var p = node.props, m = node.metrics, kids = node.children;

				// --- A join without a join condition
				if (/^MERGE JOIN CARTESIAN/.test(node.op) && kids.length >= 2) {
					var ra = rowsPerStart(kids[0]), rb = rowsPerStart(kids[1]);
					var sizes = (ra !== undefined && rb !== undefined) ? ': ' + fmtNum(ra) + ' x ' + fmtNum(rb) + ' rows' : '';
					if ((ra !== undefined && ra <= 1) || (rb !== undefined && rb <= 1)) {
						add(node, 'info', 'Cartesian Join', 'Cartesian join' + sizes,
							'There is no join condition between the two inputs, but one of them returns a single row, so nothing is multiplied. Oracle does this on purpose for a one-row input. It only becomes a problem if that "1 row" is a wrong estimate.');
					} else {
						add(node, 'warning', 'Cartesian Join', 'Cartesian join' + sizes,
							'There is no join condition between the two inputs: every row of the first is combined with every row of the second. Check that a join condition is not missing in the SQL. If the SQL is right, look at the row estimate of the inputs: the optimizer chooses this when it expects one of them to return a single row.',
							'Cartesian join');
					}
				}

				// --- A nested loop whose inner side is a full scan: the whole table is read once per outer row
				if (/^NESTED LOOPS/.test(node.op) && kids.length >= 2) {
					var inner = kids[1];
					while (/^(PARTITION|VIEW)/.test(inner.op) && inner.children.length === 1) inner = inner.children[0];
					if (isFullScan(inner) || /^INDEX FAST FULL SCAN/.test(inner.op)) {
						var times     = inner.metrics.starts !== undefined ? inner.metrics.starts : kids[0].metrics.estRows;
						var timesText = inner.metrics.starts !== undefined ? (times === 1 ? 'once' : fmtNum(times) + ' times') + ' in this execution'
						              : (times === undefined ? 'once for each row of the outer side'
						              : (times <= 1 ? 'once (the outer side is estimated to return 1 row)' : 'an estimated ' + fmtNum(times) + ' times'));
						fullScansInLoops.push(inner);
						add(inner, times === undefined || times > 1 ? 'warning' : 'info', 'Nested Loops',
							'Full scan inside a nested loop: ' + nodeTitle(inner) + ', ' + timesText,
							'This operation is the inner side of the ' + node.op + ' (Id ' + p.va + '), so it is executed once for every row that the outer side returns, and each execution reads the whole '
							+ (/^INDEX/.test(inner.op) ? 'index' : 'table') + '. A nested loop needs an index on the join column of the inner table'
							+ (inner.props.filter ? ' (see its filter: ' + inner.props.filter.join(' ') + ')' : '')
							+ '. Without one a hash join is normally the better plan: check what made the optimizer choose nested loops (a hint, or a row estimate of 1 for the outer side).',
							times === undefined || times > 1 ? 'Full scan x ' + (times === undefined ? 'N' : fmtNum(times)) : undefined);
					}
				}

				// --- A full table scan that is a large part of the plan (not one that was stopped after its first rows).
				// "A large part" is measured in time when the plan has run-time statistics and ran long enough for the
				// time to mean something, else in buffers (logical reads), else in the optimizer's cost.
				if (isFullScan(node) && fullScansInLoops.indexOf(node) < 0 && !m.stoppedEarly) {
					var shareOf   = (m.ownActTimePct !== undefined && root.metrics.actTimeSec >= 1) ? 'time'
					              : (m.ownBuffersPct !== undefined ? 'buffers' : 'cost');
					var share     = { time: m.ownActTimePct, buffers: m.ownBuffersPct, cost: m.ownCostPct }[shareOf];
					var shareText = share === undefined ? '' : ' (' + fmtPct(share) + ' of the plan\'s ' + shareOf + ')';
					var hasFilter = !!(p.filter || p.storage);
					if (hasFilter && p.pstart !== undefined && (share === undefined || share >= 10)) {
						// A partitioned table: only the partitions Pstart - Pstop are read, so the scan is already limited
						add(node, 'info', 'Full Scan',
							'Full scan of the partitions ' + p.pstart + ' - ' + p.pstop + ' with a filter: ' + (p.objName || '') + shareText,
							'Every block of the partitions that are needed is read (Pstart - Pstop; KEY = decided when the statement runs) and the rows are filtered afterwards: ' + (p.filter || p.storage).join(' ')
							+ '\nIf the filter keeps only a small part of those partitions, a (local) index on the column(s) in the filter would avoid reading all of them.');
					} else if (hasFilter && (share === undefined || share >= 10)) {
						// A warning only when the plan itself shows that the filter throws most of the table away:
						// fewer rows come out than blocks were read (run-time statistics), or than the scan costs (estimate)
						var fewRows = (m.actRows !== undefined && m.ownBuffers !== undefined) ? m.actRows < m.ownBuffers
						            : (m.actRows === undefined && m.estRows !== undefined && m.ownCost !== undefined && m.estRows < m.ownCost);
						var rowsText = (m.actRows !== undefined && m.ownBuffers !== undefined)
							? 'It returned ' + fmtNum(m.actRows) + ' rows and read ' + fmtNum(m.ownBuffers) + ' buffers (blocks) for them. '
							: (m.estRows !== undefined ? 'It is estimated to return ' + fmtNum(m.estRows) + ' rows. ' : '');
						add(node, fewRows ? 'warning' : 'info', 'Full Scan',
							'Full table scan with a filter: ' + (p.objName || '') + shareText,
							'The whole table is read and the rows are filtered afterwards: ' + (p.filter || p.storage).join(' ') + '\n' + rowsText
							+ (fewRows ? 'That is few rows for a scan of the whole table: an index on the column(s) in the filter would probably avoid most of the reading.'
							           : 'If the filter keeps only a small part of the table, an index on the column(s) in the filter would avoid reading all of it. If it keeps most of the table, the full scan is the right choice.'),
							fewRows ? 'Full scan, few rows' : undefined);
					} else if (!hasFilter && share !== undefined && share >= 50) {
						add(node, 'info', 'Full Scan', 'Full table scan: ' + (p.objName || '') + shareText,
							'The whole table is read, without a filter, and that is most of the plan. Nothing to do about it if the statement needs all the rows.');
					}
				}

				// --- A function on a column in a filter: an index on the column cannot be used for it.
				// Only where it decides how much is read: a scan of everything. (A filter on rows that an index already
				// found is a detail, and the dictionary views are full of them.) A data type conversion is reported everywhere.
				if (/^(TABLE ACCESS|INDEX|MAT_VIEW)/.test(node.op) && p.filter) {
					var readsAll = isFullScan(node) || /^INDEX (FAST )?FULL SCAN|^INDEX SKIP SCAN/.test(node.op);
					var seen = {};
					p.filter.forEach(function (text) {
						var re = /\b([A-Z][A-Z0-9_$#]*)\(\s*"(?:[^"]+"\.")?([^"]+)"/g, fm;
						while ((fm = re.exec(text))) {
							var fn = fm[1], colName = fm[2];
							if (PREDICATE_KEYWORDS.test(fn) || seen[fn + colName]) continue;
							seen[fn + colName] = true;
							if (!readsAll && !CONVERSION_FUNCTIONS.test(fn)) continue;
							if (CONVERSION_FUNCTIONS.test(fn)) {
								add(node, readsAll ? 'warning' : 'info', 'Predicate', 'Data type conversion on column ' + colName + ' (' + fn + ') in the filter of ' + nodeTitle(node),
									text + '\nThe COLUMN is converted before it is compared, so a normal index on ' + colName + ' cannot be used for this predicate, and every row that is read has to be converted. '
									+ (/^(INTERNAL_FUNCTION|SYS_OP_C2C)$/.test(fn)
										? 'Oracle added this conversion itself, because the data type of the column and of the value it is compared with differ.'
										: 'The conversion is either written in the SQL, or added by Oracle because the data type of the column and of the value differ.')
									+ ' Compare the column with a value of its own data type instead.',
									readsAll ? 'Conversion on ' + colName : undefined);
							} else {
								add(node, 'info', 'Predicate', 'Function ' + fn + ' on column ' + colName + ' in the filter of ' + nodeTitle(node),
									text + '\nA normal index on ' + colName + ' cannot be used for this predicate. If it filters away most rows: rewrite it so that the column stands alone, or create a function-based index.');
							}
						}
					});
				}

				// --- Run-time statistics: the row estimate is 10 times off. Reported where it STARTS (the operations above inherit it;
				// a line without an estimate, as a HASH JOIN inside an index join has, passes it on).
				var inheritsWarn = function (n) {
					return n.children.some(function (c) { return isEstActWarn(c.metrics) || (c.metrics.estRowsTotal === undefined && inheritsWarn(c)); });
				};
				if (isEstActWarn(m) && !inheritsWarn(node)) {
					add(node, 'warning', 'Row Estimate',
						'Row estimate is off on ' + nodeTitle(node) + ': estimated ' + fmtNum(m.estRowsTotal) + ', actual ' + fmtNum(m.actRows) + ' (' + fmtEstActDiff(m) + ')',
						'From this operation and upwards the optimizer worked with a wrong number of rows, which is the usual reason for a wrong join method or join order.'
						+ (m.starts > 1 ? ' (The estimate is for one execution: ' + fmtNum(m.estRows) + ' rows x ' + fmtNum(m.starts) + ' starts.)' : '')
						+ ' Usual causes: optimizer statistics that are missing or old' + (p.objName ? ' on ' + p.objName : '')
						+ ', a predicate on several columns that depend on each other, uneven data without a histogram, or a function on the column.');
				}

				// --- Sorts and hash joins that did not fit in memory
				var pass = /\((\d+|M)\)\s*$/.exec(m.usedMem || '');
				if (m.usedTmp || (pass && pass[1] !== '0')) {
					add(node, 'warning', 'Temp Space',
						node.op + ' did not fit in memory' + (m.usedTmp ? ': used temp ' + m.usedTmp : '') + (pass && pass[1] !== '0' ? ' (' + (pass[1] === '1' ? 'one pass' : 'several passes') + ' over temp)' : ''),
						'The work area for this operation (PGA) was too small for its rows, so they were written to the temporary tablespace and read back. Memory used: ' + (m.usedMem || '?')
						+ '. Fewer rows or narrower rows into the operation is the first thing to look for; after that PGA_AGGREGATE_TARGET.',
						'Used temp');
				} else if (m.tempSpc !== undefined) {
					add(node, 'info', 'Temp Space', node.op + ' is expected to need ' + fmtBytes(m.tempSpc) + ' of temp space',
						'The optimizer estimates that the rows of this operation do not fit in its work area (PGA) and will be written to the temporary tablespace. This is an estimate: the plan has no run-time statistics.');
				}

				// --- A subquery that is executed row by row
				if (/^FILTER/.test(node.op) && kids.length >= 2) {
					var subStarts = kids[1].metrics.starts;
					var subTimes  = subStarts !== undefined ? subStarts : kids[0].metrics.estRows;
					add(node, subTimes !== undefined && subTimes >= 1000 ? 'warning' : 'info', 'Subquery',
						'Subquery executed row by row (FILTER): '
						+ (subStarts !== undefined ? fmtNum(subStarts) + ' times in this execution' : (subTimes !== undefined ? 'up to ' + fmtNum(subTimes) + ' times (estimated)' : 'once per row')),
						'The operations from Id ' + kids[1].props.va + ' are a subquery that was not turned into a join. It is executed for each row of the first child (Id ' + kids[0].props.va
						+ '); Oracle remembers the answer for values it has just seen, so the real number depends on how many distinct values there are. With many rows a join (the subquery "unnested") is normally much faster: look for a NO_UNNEST hint, or for something in the subquery that prevents it.',
						subTimes !== undefined && subTimes >= 1000 ? 'Subquery per row' : undefined);
				}

				if (/^INDEX SKIP SCAN/.test(node.op)) {
					add(node, 'info', 'Index', 'Index skip scan on ' + (p.objName || 'an index'),
						'The first column of the index is not in the predicate, so the index is probed once for each distinct value of that column. Fine when there are few distinct values. An index that starts with the column in the predicate would be read directly.');
				}

				if (/^PARTITION .* ALL/.test(node.op)) {
					var partObj;
					DbxShowplanGraph.walkPlanNodes(node, function (n) { if (!partObj && n.props.objName) partObj = n.props.objName; });
					add(node, 'info', 'Partitions', 'All partitions are read' + (partObj ? ': ' + partObj : ''),
						'No partitions are left out (no partition pruning): the statement has no usable predicate on the partition key, or the key is wrapped in a function.');
				}
			});

			// --- What Oracle itself says about the plan
			pi.notes.forEach(function (note) {
				var rule = null;
				for (var i = 0; i < NOTE_RULES.length && !rule; i++) { if (NOTE_RULES[i].test.test(note)) rule = NOTE_RULES[i]; }
				add(null, rule ? rule.severity : 'info', 'Note', note, rule ? rule.text : '');
			});
			if (pi.inactive) {
				add(null, 'info', 'Note', pi.inactive + ' operations of the adaptive plan are not drawn',
					'The plan text was made with the format +ADAPTIVE, which also lists the alternative that was NOT chosen (the lines marked with "-"). Those are left out of the diagram; they are in the Raw Plan Text.');
			}

			// --- Hints that were not used (the Hint Report, Oracle 19c and above)
			var hintReport = pi.sections['Hint Report'];
			if (hintReport) {
				var bad = [];
				hintReport.split('\n').forEach(function (line) {
					var hm = /^\s+([UEN])\s+-\s+(.*)$/.exec(line);
					if (hm) bad.push({ U: 'not used', E: 'error', N: 'not resolved' }[hm[1]] + ':  ' + hm[2].trim());
				});
				if (bad.length) {
					add(null, 'warning', 'Hints', bad.length + ' hint' + (bad.length !== 1 ? 's' : '') + ' in the statement had no effect',
						bad.join('\n') + '\nA hint that is not used is silently ignored: the statement runs, but not with the plan its author asked for. (Not used = it conflicts with another hint, or it cannot be followed. Error = it is misspelled. Not resolved = the table or index it names is not in the statement.)');
				}
			}
		});
		return findings;
	}

	function render(container, parsed, opts) {
		injectStyle();
		var $container = $(container);
		$container.empty();
		// The panels are on <body>, not in the container: one that is pinned would otherwise stay, pointing at a box that is gone
		_panels.closeAll();
		// Reset before the early return too: a plan with nothing to draw must not leave the pane showing an operation of the previous plan
		_propsTarget = (opts && opts.propsTarget) || null;
		_selectedBox = null;
		if (_propsTarget) renderPropertiesInto(_propsTarget, null);
		if (!parsed || !parsed.statements || !parsed.statements.length) return;

		var horizontal        = !!(opts && opts.horizontal);
		var useCompactLayout  = !!(opts && opts.layout === 'compact');
		var useLineConnectors = useCompactLayout || !!(opts && opts.connectorStyle === 'lines');

		// Before the boxes are made: a finding can put a note on the box of its operation
		var findings = collectFindings(parsed);

		var $wrap = $('<div class="ora-plan-wrap"></div>');
		var trees = [];
		parsed.statements.forEach(function (stmt) {
			// DBMS_XPLAN.DISPLAY_CURSOR prints one plan per child cursor: say which is which
			if (parsed.statements.length > 1) $wrap.append($('<div class="ora-plan-stmt-hdr"></div>').text(stmt.label));
			var $tree = $('<div class="ora-plan-tree"></div>');
			if (useCompactLayout)   $tree.addClass(horizontal ? 'ora-plan-compact-h' : 'ora-plan-compact-v');
			else if (horizontal)    $tree.addClass('ora-plan-horizontal');
			if (useLineConnectors)  $tree.addClass('ora-plan-lines');
			$tree.append($('<ul></ul>').append(renderNode(stmt.steps[0].root)));
			$wrap.append($tree);
			trees.push($tree);
		});

		// A click on the empty part of the diagram closes the open panels
		$wrap.on('click', function () { _panels.closeAll(); });
		$container.append($wrap);

		if (opts && opts.onFindingsChanged) opts.onFindingsChanged(findings);

		// Both need the real positions of the boxes, so the tree has to be in the document first.
		// The leaves are moved next to their parent BEFORE the arrows are drawn, so the arrows go to where the boxes end up.
		trees.forEach(function ($tree) {
			if (useCompactLayout) {
				if (horizontal) DbxShowplanGraph.tuckLeavesNearParent($tree[0], 'ora-plan');
				else            DbxShowplanGraph.tuckLeavesNearParentVertical($tree[0], 'ora-plan');
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
