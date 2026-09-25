/**
 * dbxSqlFormat.js — shared sql-formatter wrapper + the user's "SQL Format options"
 *
 * Every "Format SQL" button (showplan dialogs, SQL Text dialog, Query Store detail) and the LLM Advice
 * SQL blocks format through dbxSqlFormat.format(), so they all follow the same options. The options are
 * remembered per browser in localStorage (key 'dbxtune_sqlFormat_options'). The SQL dialect is NOT an
 * option - each call site passes the language that fits its DBMS.
 *
 * On top of sql-formatter (which has no option for these), format() can also: put JOIN ... ON conditions
 * as an ON / AND / OR block under the JOIN, line up comparison operators, and (T-SQL) lay out Sybase
 * ASE's ROWS LIMIT / ROWS OFFSET clause, which sql-formatter does not know.
 *
 * Dependencies: sqlFormatter (sql-formatter 15.x, global), only needed at format() time - so this file
 * can be loaded before or after it.
 *
 * Global object exposed: window.dbxSqlFormat
 *   format(sql, language, extraOpts)   - format with the saved options; throws like sqlFormatter.format()
 *   isAutoFormat()                     - true if SQL should be formatted as soon as a dialog shows it
 *   getOptions() / saveOptions(o) / resetOptions()
 *   settingsButtonHtml(hostId, applyFnName, style, btnClass) - the ⚙ button that opens the options panel
 *   togglePanel(hostId, applyFnName)   - opens/closes the options panel inside element #hostId
 */
(function () {
	'use strict';

	var LS_KEY = 'dbxtune_sqlFormat_options';

	// Defaults = exactly what every Format SQL button did before these options existed.
	var DEFAULTS = {
		keywordCase:            'upper',
		functionCase:           'upper',
		dataTypeCase:           'upper',
		identifierCase:         'preserve',
		indentStyle:            'standard',
		logicalOperatorNewline: 'before',
		tabWidth:               4,
		useTabs:                false,
		expressionWidth:        50,
		linesBetweenQueries:    1,
		denseOperators:         false,
		newlineBeforeSemicolon: false,
		// ours, not sql-formatter options (see OURS below)
		joinLayout:             'onNewLine',  // JOIN ... ON: 'onNewLine' = ON/AND/OR block under the JOIN, 'asIs' = sql-formatter's layout
		alignOperators:         true,         // line up = <> < > ... in JOIN ON blocks and WHERE/HAVING condition lists
		autoFormat:             false
	};
	// Options handled by this file (post-processing / UI), never passed to sqlFormatter.format() - it rejects unknown keys
	var OURS = ['joinLayout', 'alignOperators', 'autoFormat'];

	// Allowed values - anything else read back from localStorage falls back to the default, so an old
	// or hand-edited stored value can never make sqlFormatter.format() throw on every click.
	var CASES   = ['upper', 'lower', 'preserve'];
	var CHOICES = {
		keywordCase:            CASES,
		functionCase:           CASES,
		dataTypeCase:           CASES,
		identifierCase:         CASES,
		indentStyle:            ['standard', 'tabularLeft', 'tabularRight'],
		logicalOperatorNewline: ['before', 'after'],
		joinLayout:             ['onNewLine', 'asIs']
	};
	var NUMBERS = { tabWidth: [1, 16], expressionWidth: [1, 500], linesBetweenQueries: [0, 10] };
	// every other key in DEFAULTS is a boolean

	function _sanitize(o) {
		var res = {};
		Object.keys(DEFAULTS).forEach(function (k) {
			var v = o ? o[k] : undefined;
			var d = DEFAULTS[k];
			if (CHOICES[k]) {
				res[k] = CHOICES[k].indexOf(v) >= 0 ? v : d;
			} else if (NUMBERS[k]) {
				var n = parseInt(v, 10);
				res[k] = (isFinite(n) && n >= NUMBERS[k][0] && n <= NUMBERS[k][1]) ? n : d;
			} else {
				res[k] = (typeof v === 'boolean') ? v : d;
			}
		});
		return res;
	}

	function getOptions() {
		var stored = null;
		try { stored = JSON.parse(localStorage.getItem(LS_KEY) || 'null'); } catch (ex) {}
		return _sanitize(stored);
	}

	function saveOptions(o) {
		var clean = _sanitize(o);
		try { localStorage.setItem(LS_KEY, JSON.stringify(clean)); } catch (ex) {}
		return clean;
	}

	function resetOptions() {
		try { localStorage.removeItem(LS_KEY); } catch (ex) {}
		return _sanitize(null);
	}

	function isAutoFormat() {
		return getOptions().autoFormat === true;
	}

	/**
	 * @param {string} sql
	 * @param {string} language   sql-formatter dialect: 'tsql', 'postgresql', 'sql', ...
	 * @param {Object} [extraOpts] call-site specific options that are not user settings, e.g. paramTypes
	 */
	function format(sql, language, extraOpts) {
		if (typeof sqlFormatter === 'undefined')
			throw new Error('SQL formatter library (sql-formatter) is not loaded on this page.');
		var o  = getOptions();
		var fo = { language: language || 'sql' };
		Object.keys(o).forEach(function (k) { if (OURS.indexOf(k) < 0) fo[k] = o[k]; });
		if (extraOpts) Object.keys(extraOpts).forEach(function (k) { fo[k] = extraOpts[k]; });

		// sql-formatter's own parse errors are thrown to the caller as before (it shows them). Only a
		// failure in OUR extra steps falls back to the plain sql-formatter output - they must never be
		// the reason formatting stops working.
		var rows = _isTsql(fo.language) ? _rowsLimitPre(sql, fo) : null;
		var out  = sqlFormatter.format(rows ? rows.sql : sql, fo);
		try {
			if (rows) out = _rowsLimitPost(out, rows, fo);
			if (fo.indentStyle === 'standard') {
				if (o.joinLayout === 'onNewLine') {
					// A pass moves the conditions of a JOIN as-is, so a JOIN nested inside one of them (e.g. in
					// an IN (subquery)) is done by the next pass. Blocks already done are not matched again.
					for (var pass = 0; pass < 5; pass++) {
						var next = _joinLayout(out, fo, o.alignOperators && !fo.denseOperators);
						if (next === out) break;
						out = next;
					}
				}
				if (o.alignOperators && !fo.denseOperators) out = _alignWhere(out, fo);
			}
			return out;
		} catch (ex) {
			console.warn('dbxSqlFormat: extra formatting step failed, using plain sql-formatter output: ' + (ex && ex.message ? ex.message : ex));
			return sqlFormatter.format(sql, fo);
		}
	}

	// -------------------------------------------------------------------------
	// Extra steps on top of sql-formatter. They work on its (very regular) output line by line:
	// a clause keyword on its own line, its content one indent level deeper, AND/OR conditions of
	// a list starting their own line at the list's indent (or ending the previous one when
	// logicalOperatorNewline='after'), and anything nested (subquery, parentheses, CASE) indented
	// deeper than the line it belongs to. Only used with indentStyle 'standard'.
	// -------------------------------------------------------------------------
	function _isTsql(lang) { return lang === 'tsql' || lang === 'transactsql'; }

	function _unit(fo) { return fo.useTabs ? '\t' : new Array((fo.tabWidth || 4) + 1).join(' '); }

	function _lead(s) { return s.match(/^[ \t]*/)[0]; }

	// Same-length copy of s with the contents of string literals, quoted/[bracketed] identifiers and
	// comments blanked out, so keyword/operator/paren searches never hit text inside them.
	function _mask(s) {
		var out = '', i = 0, n = s.length;
		while (i < n) {
			var c = s.charAt(i);
			if (c === "'" || c === '"' || c === '[') {
				var close = (c === '[') ? ']' : c, j = i + 1;
				while (j < n) {
					if (s.charAt(j) === close) {
						if (close !== ']' && s.charAt(j + 1) === close) { j += 2; continue; } // '' or "" escape
						break;
					}
					j++;
				}
				var end = Math.min(j, n - 1);
				out += c + new Array(end - i + (j < n ? 0 : 1)).join('_') + (j < n ? close : '');
				i = j + 1;
				continue;
			}
			if (c === '-' && s.charAt(i + 1) === '-') {
				var nl = s.indexOf('\n', i); if (nl < 0) nl = n;
				out += new Array(nl - i + 1).join('_'); i = nl; continue;
			}
			if (c === '/' && s.charAt(i + 1) === '*') {
				var ce = s.indexOf('*/', i + 2); ce = (ce < 0) ? n : ce + 2;
				out += s.substring(i, ce).replace(/[^\n]/g, '_'); i = ce; continue;
			}
			out += c; i++;
		}
		return out;
	}

	// Index of the first match of re (global regex) at paren depth 0 of the masked line, or -1.
	// Depth never goes below 0, so a line starting with the ')' that closes an earlier line still counts.
	function _findDepth0(m, re) {
		var depth = [], d = 0;
		for (var i = 0; i < m.length; i++) {
			depth.push(d);
			var ch = m.charAt(i);
			if (ch === '(') d++;
			else if (ch === ')') d = Math.max(0, d - 1);
		}
		re.lastIndex = 0;
		var mt;
		while ((mt = re.exec(m)) !== null) {
			if (depth[mt.index] === 0) return { index: mt.index, text: mt[0] };
			if (mt[0].length === 0) re.lastIndex++;
		}
		return null;
	}

	// ( ) and CASE ... END balance of a masked line - a condition is complete when this sums to 0
	function _balance(m) {
		var b = 0;
		for (var i = 0; i < m.length; i++) { var ch = m.charAt(i); if (ch === '(') b++; else if (ch === ')') b--; }
		b += (m.match(/\bCASE\b/gi) || []).length - (m.match(/\bEND\b/gi) || []).length;
		return b;
	}

	// ---- Sybase ASE:  ROWS LIMIT n [OFFSET m]  /  ROWS OFFSET m [LIMIT n] --------------------------
	// sql-formatter's tsql dialect does not know this clause: it left "rows limit 200" lower-case at the
	// end of the previous line and made OFFSET a clause of its own. So it is swapped for a placeholder
	// comment before formatting (sql-formatter keeps a comment where it was) and put back afterwards on
	// its own line at the clause level. T-SQL only - PostgreSQL has its own, valid, OFFSET n ROWS LIMIT m.
	var ROWS_VAL = '(\\d+|@{1,3}\\w+|\\?)';
	var ROWS_RE  = new RegExp('\\b(ROWS)\\s+(?:(LIMIT)\\s+' + ROWS_VAL + '(?:\\s+(OFFSET)\\s+' + ROWS_VAL + ')?'
	                                     + '|(OFFSET)\\s+' + ROWS_VAL + '(?:\\s+(LIMIT)\\s+' + ROWS_VAL + ')?)', 'gi');

	function _kwCase(word, fo) {
		return fo.keywordCase === 'upper' ? word.toUpperCase() : fo.keywordCase === 'lower' ? word.toLowerCase() : word;
	}

	function _rowsLimitPre(sql, fo) {
		var masked = _mask(sql), found = [], mt;
		ROWS_RE.lastIndex = 0;
		while ((mt = ROWS_RE.exec(masked)) !== null) {
			var tokens = mt[2] ? [mt[1], mt[2], mt[3], mt[4], mt[5]] : [mt[1], mt[6], mt[7], mt[8], mt[9]];
			tokens = tokens.filter(function (t) { return t !== undefined; })
				.map(function (t, i) { return (i === 0 || i % 2 === 1) ? _kwCase(t, fo) : t; }); // ROWS kw val [kw val]
			found.push({ index: mt.index, length: mt[0].length, text: tokens.join(' ') });
		}
		if (found.length === 0) return null;
		var res = sql;
		for (var i = found.length - 1; i >= 0; i--) {
			found[i].ph = '/*__DBX_ROWS_' + i + '__*/';
			res = res.substring(0, found[i].index) + found[i].ph + res.substring(found[i].index + found[i].length);
		}
		return { sql: res, list: found };
	}

	function _rowsLimitPost(out, rows, fo) {
		var unit = _unit(fo);
		rows.list.forEach(function (r) {
			var lines = out.split('\n'), done = false;
			for (var i = 0; i < lines.length && !done; i++) {
				var idx = lines[i].indexOf(r.ph);
				if (idx < 0) continue;
				var before = lines[i].substring(0, idx).replace(/\s+$/, '');
				var after  = lines[i].substring(idx + r.ph.length);
				if (fo.indentStyle !== 'standard') {
					lines[i] = before + ' ' + r.text + after;
				} else {
					var lead   = _lead(lines[i]);
					var clause = (lead.length >= unit.length && lead.substring(lead.length - unit.length) === unit) ? lead.substring(0, lead.length - unit.length) : lead;
					var newLine = clause + r.text + after;
					if (before.trim() === '') lines[i] = newLine;
					else                      lines.splice(i, 1, before, newLine);
				}
				done = true;
			}
			if (!done) throw new Error('ROWS LIMIT placeholder lost by sql-formatter');
			out = lines.join('\n');
		});
		return out;
	}

	// ---- Conditions of a JOIN ON / WHERE / HAVING list ----------------------------------------------
	// Collects the conditions that start at lines[start] with indentation `ind`. The first condition is
	// `firstText` with operator `firstOp` ('' for WHERE/HAVING). Returns { conds, end } where end is the
	// index of the first line not belonging to the list. Each cond: { op, lead, text, nested[] } - lead is
	// true when op started the line (false: it ended the previous line, 'after' mode); nested are its
	// further lines (deeper indented, or the closing ')' / END of something it opened).
	var LEAD_OP_RE  = /^(AND|OR)\s+/i;
	var TRAIL_OP_RE = /\s+(AND|OR)\s*$/i;

	function _collectConds(lines, start, ind, firstOp, firstText) {
		var conds = [], cur = null, bal = 0, pendingOp = null;
		function add(op, text, lead) { cur = { op: op, lead: lead, text: text, nested: [] }; conds.push(cur); bal = _balance(_mask(text)); takeTrail(); }
		function takeTrail() {
			// 'after' mode: "cond AND" - the operator belongs to the next condition
			if (bal !== 0) return;
			var last = cur.nested.length ? cur.nested[cur.nested.length - 1] : cur.text;
			var m = _mask(last).match(TRAIL_OP_RE);
			if (!m) return;
			pendingOp = last.substr(last.length - m[0].length).trim();
			last = last.substring(0, last.length - m[0].length);
			if (cur.nested.length) cur.nested[cur.nested.length - 1] = last; else cur.text = last;
		}
		add(firstOp, firstText, true);
		var i = start;
		for (; i < lines.length; i++) {
			var line = lines[i], lead = _lead(line), trimmed = line.trim();
			if (trimmed === '') break;
			if (bal > 0 || lead.length > ind.length) {       // part of the current condition
				cur.nested.push(line);
				bal += _balance(_mask(line));
				takeTrail();
				continue;
			}
			if (lead.length < ind.length) break;
			var lm = _mask(trimmed).match(LEAD_OP_RE);
			if (lm && pendingOp === null) { add(trimmed.substring(0, lm[1].length), trimmed.substring(lm[0].length), true); continue; }
			if (pendingOp !== null && !lm) { var op = pendingOp; pendingOp = null; add(op, trimmed, false); continue; }
			break;
		}
		return { conds: conds, end: i };
	}

	var CMP_RE = /<>|!=|<=|>=|=|<|>/g;
	var ALIGN_MAX_LHS = 40;

	// Pads the left side of single-line "lhs <op> rhs" conditions so the operators share one column.
	// prefixes[k] = text written before cond k on its line ('ON  ', 'AND ', ...), counted in the column.
	function _alignConds(conds, prefixes) {
		var cand = [];
		conds.forEach(function (c, k) {
			if (c.nested.length) return;
			var f = _findDepth0(_mask(c.text), CMP_RE);
			if (!f) return;
			var lhs = c.text.substring(0, f.index).replace(/\s+$/, '');
			if (lhs === '' || lhs.length > ALIGN_MAX_LHS) return;
			cand.push({ k: k, lhs: lhs, rest: c.text.substring(f.index) });
		});
		if (cand.length < 2) return;
		var col = 0;
		cand.forEach(function (x) { col = Math.max(col, prefixes[x.k].length + x.lhs.length); });
		cand.forEach(function (x) {
			var pad = col - prefixes[x.k].length - x.lhs.length;
			conds[x.k].text = x.lhs + new Array(pad + 2).join(' ') + x.rest;
		});
	}

	// ---- JOIN layout "ON on new line" ----------------------------------------------------------------
	//     INNER JOIN dbo.customers c              <- was: INNER JOIN dbo.customers c ON c.id = o.cust_id
	//         ON  c.id     = o.cust_id                                 AND c.region = o.region
	//         AND c.region = o.region
	var ON_RE   = /\bON\b/gi;
	var JOIN_RE = /\bJOIN\b/gi;

	function _joinLayout(out, fo, align) {
		var unit = _unit(fo), lines = out.split('\n'), res = [];
		for (var i = 0; i < lines.length; i++) {
			var line = lines[i], m = _mask(line), ind = _lead(line);
			var on = _findDepth0(m, ON_RE);
			var isJoin = false;
			if (on) {
				var join = _findDepth0(m, JOIN_RE);
				if (join && join.index < on.index) {
					isJoin = true;
				} else if (/^\s*\)/.test(m)) {
					// ") c ON ..." closing a joined subquery: the line that opened it must be a JOIN
					for (var j = res.length - 1; j >= 0; j--) {
						var lj = _lead(res[j]);
						if (lj.length > ind.length) continue;
						var mj = _mask(res[j]);
						isJoin = (lj.length === ind.length) && _findDepth0(mj, JOIN_RE) !== null && /\(\s*$/.test(mj);
						break;
					}
				}
			}
			if (!isJoin) { res.push(line); continue; }

			var joinPart = line.substring(0, on.index).replace(/\s+$/, '');
			var onWord   = line.substr(on.index, 2);
			var col      = _collectConds(lines, i + 1, ind, onWord, line.substring(on.index + 2).trim());
			var prefixes = col.conds.map(function (c) { var w = c.op; return w + new Array(Math.max(1, 5 - w.length)).join(' '); });
			if (align) _alignConds(col.conds, prefixes);

			res.push(joinPart);
			col.conds.forEach(function (c, k) {
				res.push(ind + unit + prefixes[k] + c.text);
				c.nested.forEach(function (n) { res.push(unit + n); });
			});
			i = col.end - 1;
		}
		return res.join('\n');
	}

	// ---- Operator alignment in WHERE / HAVING lists -----------------------------------------------------
	var WHERE_RE = /^\s*(WHERE|HAVING)\s*$/i;

	function _alignWhere(out, fo) {
		var unit = _unit(fo), lines = out.split('\n');
		for (var i = 0; i < lines.length - 1; i++) {
			if (!WHERE_RE.test(_mask(lines[i]))) continue;
			var ind = _lead(lines[i]) + unit;
			if (_lead(lines[i + 1]) !== ind) continue;
			var col = _collectConds(lines, i + 2, ind, '', lines[i + 1].trim());
			if (col.conds.length < 2) continue;
			var prefixes = col.conds.map(function (c) { return (c.op && c.lead) ? c.op + ' ' : ''; });
			var before = col.conds.map(function (c) { return c.text; });
			_alignConds(col.conds, prefixes);
			// Write back only the first line of each condition - nested lines are untouched. The trailing
			// AND/OR of 'after' mode was split off by _collectConds, so it is re-attached here.
			var k = 0, li = i + 1;
			for (; li < col.end && k < col.conds.length; k++) {
				var c = col.conds[k];
				if (c.text !== before[k]) {
					var trail = lines[li].trim().match(TRAIL_OP_RE);
					var hasTrail = trail && !c.nested.length && _mask(lines[li]).match(TRAIL_OP_RE);
					lines[li] = ind + prefixes[k] + c.text + (hasTrail ? ' ' + trail[1] : '');
				}
				li += 1 + c.nested.length;
			}
			// no skip to col.end: a WHERE inside a nested subquery of these conditions gets its own pass
		}
		return lines.join('\n');
	}

	// -------------------------------------------------------------------------
	// Options panel (inline - rendered into an empty <div id=hostId> the call site provides, not a
	// modal: most call sites already live inside a Bootstrap modal, and stacking another one on top
	// of that causes focus/backdrop trouble)
	// -------------------------------------------------------------------------
	var _ID_RE = /^[A-Za-z_][\w-]*$/;

	var CASE_LABELS = { upper: 'UPPER', lower: 'lower', preserve: 'as is' };
	var SELECTS = [
		// key, label, value->text, tooltip
		['keywordCase',            'Keywords',   CASE_LABELS, 'Case of reserved keywords (SELECT, FROM, WHERE ...)'],
		['functionCase',           'Functions',  CASE_LABELS, 'Case of function names (COUNT, ISNULL, CAST ...)'],
		['dataTypeCase',           'Data types', CASE_LABELS, 'Case of data type names (INT, VARCHAR ...)'],
		['identifierCase',         'Identifiers',CASE_LABELS, 'Case of table/column names. Experimental in sql-formatter - quoted identifiers are never changed'],
		['indentStyle',            'Indent',     { standard: 'standard', tabularLeft: 'tabular, left', tabularRight: 'tabular, right' },
		                                          'standard = indent by tab width; tabular = keywords in their own 10 char wide column'],
		['logicalOperatorNewline', 'AND / OR',   { before: 'new line before', after: 'new line after' }, 'Put the line break before or after AND / OR'],
		['joinLayout',             'JOIN ... ON',{ onNewLine: 'ON on new line', asIs: 'as sql-formatter' },
		                                          'ON on new line = ON / AND / OR block one level under the JOIN; as sql-formatter = ON on the JOIN line, extra conditions at the JOIN indent. Only with indent style standard']
	];
	var NUMBER_INPUTS = [
		['tabWidth',            'Tab width',        'Number of spaces per indent level'],
		['expressionWidth',     'Expression width', 'Max characters of a parenthesized expression kept on one line'],
		['linesBetweenQueries', 'Lines between queries', 'Empty lines between statements']
	];
	var CHECKBOXES = [
		['useTabs',                'Indent with tabs',          'Indent with tab characters instead of spaces'],
		['denseOperators',         'Dense operators',           'No spaces around operators: a=b+1 instead of a = b + 1'],
		['newlineBeforeSemicolon', 'Newline before ;',          'Put the closing semicolon on its own line'],
		['alignOperators',         'Align operators',           'Line up = <> < > <= >= in JOIN ON blocks and WHERE / HAVING lists (single-line conditions only). Only with indent style standard, not with dense operators'],
		['autoFormat',             'Auto-format when a dialog opens', 'Format the SQL automatically every time a dialog shows SQL (instead of clicking Format SQL)']
	];

	function _esc(s) {
		return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/'/g, '&#39;').replace(/"/g, '&quot;');
	}

	function _panelHtml(hostId, applyFnName) {
		var args = "'" + hostId + "','" + (applyFnName || '') + "'";
		var lbl  = "style='font-size:0.85em;margin:0 4px 0 0;white-space:nowrap;'";
		var cell = "style='display:inline-flex;align-items:center;margin:2px 14px 2px 0;'";
		var h = [];
		h.push("<div class='dbx-sqlfmt-panel' style='border:1px solid #d0d0d0;border-radius:3px;background:#f4f6f8;color:#212529;padding:6px 10px;margin:4px 0;font-size:0.9em;max-width:760px;'>");
		h.push("<div style='font-weight:600;font-size:0.85em;margin-bottom:4px;'>&#9881; SQL Format options"
			+ " <span style='font-weight:normal;color:#888;'>(saved in this browser, used by every Format SQL button)</span></div>");
		h.push("<div>");
		SELECTS.forEach(function (s) {
			h.push("<span " + cell + " title='" + _esc(s[3]) + "'><label " + lbl + ">" + s[1] + ":</label>"
				+ "<select class='form-select form-select-sm' data-opt='" + s[0] + "' style='width:auto;padding-top:0;padding-bottom:0;font-size:0.85em;'>");
			Object.keys(s[2]).forEach(function (v) { h.push("<option value='" + v + "'>" + _esc(s[2][v]) + "</option>"); });
			h.push("</select></span>");
		});
		NUMBER_INPUTS.forEach(function (n) {
			var r = NUMBERS[n[0]];
			h.push("<span " + cell + " title='" + _esc(n[2]) + "'><label " + lbl + ">" + n[1] + ":</label>"
				+ "<input type='number' class='form-control form-control-sm' data-opt='" + n[0] + "' min='" + r[0] + "' max='" + r[1] + "' step='1'"
				+ " style='width:64px;padding-top:0;padding-bottom:0;font-size:0.85em;'></span>");
		});
		h.push("</div><div>");
		CHECKBOXES.forEach(function (c) {
			h.push("<span " + cell + " title='" + _esc(c[2]) + "'><label " + lbl + "><input type='checkbox' data-opt='" + c[0] + "' style='vertical-align:middle;margin-right:3px;'>" + c[1] + "</label></span>");
		});
		h.push("</div>");
		h.push("<div style='margin-top:4px;'>"
			+ "<button type='button' class='btn btn-primary btn-sm' style='padding:0 10px;' onclick=\"dbxSqlFormat._apply(" + args + ");\" title='Save the options and re-format the SQL shown'>Apply</button> "
			+ "<button type='button' class='btn btn-outline-secondary btn-sm' style='padding:0 10px;' onclick=\"dbxSqlFormat._reset(" + args + ");\" title='Go back to the default options and re-format the SQL shown'>Reset defaults</button> "
			+ "<button type='button' class='btn btn-outline-secondary btn-sm' style='padding:0 10px;' onclick=\"dbxSqlFormat.togglePanel(" + args + ");\">Close</button>"
			+ "</div>");
		h.push("</div>");
		return h.join('');
	}

	function _fill(host, o) {
		$(host).find('[data-opt]').each(function () {
			var k = this.getAttribute('data-opt');
			if (this.type === 'checkbox') this.checked = o[k] === true;
			else                          this.value   = o[k];
		});
	}

	function _read(host) {
		var o = {};
		$(host).find('[data-opt]').each(function () {
			var k = this.getAttribute('data-opt');
			o[k] = (this.type === 'checkbox') ? this.checked : this.value;
		});
		return o;
	}

	// Re-run the call site's own Format SQL function, so its dialect/param rules still apply.
	function _runApply(applyFnName) {
		if (applyFnName && typeof window[applyFnName] === 'function') window[applyFnName]();
	}

	function togglePanel(hostId, applyFnName) {
		if (!_ID_RE.test(hostId) || (applyFnName && !_ID_RE.test(applyFnName))) return;
		var host = document.getElementById(hostId);
		if (!host) return;
		if (host.firstChild) { host.innerHTML = ''; return; }
		host.innerHTML = _panelHtml(hostId, applyFnName);
		_fill(host, getOptions());
	}

	function _apply(hostId, applyFnName) {
		var host = document.getElementById(hostId);
		if (!host) return;
		_fill(host, saveOptions(_read(host))); // show what was actually stored (out-of-range numbers reset)
		_runApply(applyFnName);
	}

	function _reset(hostId, applyFnName) {
		var host = document.getElementById(hostId);
		if (!host) return;
		_fill(host, resetOptions());
		_runApply(applyFnName);
	}

	function settingsButtonHtml(hostId, applyFnName, style, btnClass) {
		return "<button type='button' class='" + (btnClass || 'btn btn-outline-secondary btn-sm') + "'"
			+ (style ? " style='" + style + "'" : "")
			+ " title='SQL Format options' onclick=\"dbxSqlFormat.togglePanel('" + hostId + "','" + (applyFnName || '') + "');\">&#9881;</button>";
	}

	window.dbxSqlFormat = {
		DEFAULTS:           DEFAULTS,
		format:             format,
		isAutoFormat:       isAutoFormat,
		getOptions:         getOptions,
		saveOptions:        saveOptions,
		resetOptions:       resetOptions,
		settingsButtonHtml: settingsButtonHtml,
		togglePanel:        togglePanel,
		_apply:             _apply,
		_reset:             _reset
	};
}());
