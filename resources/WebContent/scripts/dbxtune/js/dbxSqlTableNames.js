/**
 * Lightweight SQL table-name extractor, shared by dbxShowplan.js (the modal Showplan dialogs) and
 * ShowplanAseServlet.java's standalone /showplan/ase page - both need to turn a SQL statement's
 * text into a list of referenced table names (to drive a DDL Storage / table-info lookup), and
 * this used to live privately inside dbxShowplan.js until the standalone page needed it too.
 *
 * window.DbxSqlTableNames.extractTablesAsync(sql, callback) is the only entry point - callback
 * is always called exactly once with a (possibly empty) array of table names.
 */
window.DbxSqlTableNames = (function () {

	// -------------------------------------------------------------------------
	// Lightweight SQL table-name extractor (JS port of TableNameParser.java)
	// Looks for tokens immediately after: FROM, JOIN, INTO, TABLE, USING, UPDATE, CALL
	// Filters out variables (@...), temp tables (#...), and known non-table tokens.
	// -------------------------------------------------------------------------
	function sqlExtractTables(sql) {
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
	// Falls back to sqlExtractTables() if the script fails to load or parse fails.
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
	function extractTablesAsync(sql, callback) {
		_nspLoad(function(ok) {
			if (!ok || typeof NodeSQLParser === 'undefined') {
				callback(sqlExtractTables(sql));
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
				callback(tables.length ? tables : sqlExtractTables(sql));
			} catch(e) {
				callback(sqlExtractTables(sql));
			}
		});
	}

	// -------------------------------------------------------------------------
	// findJoinColumnsForTable(sql, tableName, corrName, callback) - used by dbxShowplanAse.js to
	// suggest an index when a REFORMATTING strategy shows up: given the table (and, if the query
	// aliased it, the correlation name actually used in the SQL) a plan materialized into a
	// worktable, find which column(s) it's joined/filtered on by equality - those are exactly the
	// columns an index would need to cover to avoid the reformat. callback(columns[]) is always
	// called, possibly with an empty array.
	// -------------------------------------------------------------------------

	// Recursively walks a node-sql-parser expression tree (binary_expr nodes, {left, right,
	// operator}) collecting the column name on either side of an "=" comparison whenever the OTHER
	// side is a column_ref belonging to aliasLower - this catches both join predicates
	// (a.c4 = b.c4) and simple filters (b.c4 = 123), since either would benefit from an index.
	function collectEqualityColumns(expr, aliasLower, results) {
		if (!expr || typeof expr !== 'object') return;
		if (expr.type === 'binary_expr') {
			if (expr.operator === '=') {
				var left  = expr.left;
				var right = expr.right;
				if (left  && left.type  === 'column_ref' && left.table  && String(left.table).toLowerCase()  === aliasLower) results.push(left.column);
				if (right && right.type === 'column_ref' && right.table && String(right.table).toLowerCase() === aliasLower) results.push(right.column);
			}
			collectEqualityColumns(expr.left,  aliasLower, results);
			collectEqualityColumns(expr.right, aliasLower, results);
		} else if (expr.type === 'unary_expr' || expr.type === 'expr_list') {
			collectEqualityColumns(expr.expr || expr.value, aliasLower, results);
		}
	}

	function dedupPreserveOrder(list) {
		var seen = {};
		var out  = [];
		list.forEach(function (c) {
			if (!c) return;
			var key = String(c).toLowerCase();
			if (seen[key]) return;
			seen[key] = true;
			out.push(c);
		});
		return out;
	}

	// Checks both the statement's WHERE clause and every FROM-item's JOIN...ON clause - a join
	// predicate can live in either place depending on old-style comma-join vs. explicit JOIN syntax.
	function astFindJoinColumns(ast, aliasLower) {
		var results = [];
		var stmts = Array.isArray(ast) ? ast : [ast];
		stmts.forEach(function (stmt) {
			if (stmt.where) collectEqualityColumns(stmt.where, aliasLower, results);
			(stmt.from || []).forEach(function (f) {
				if (f && f.on) collectEqualityColumns(f.on, aliasLower, results);
			});
		});
		return dedupPreserveOrder(results);
	}

	// Plain-text fallback when node-sql-parser isn't available or the AST walk finds nothing -
	// same "never fail, just degrade" pattern extractTablesAsync() above already follows.
	function regexFindJoinColumns(sql, aliasCandidates) {
		var results = [];
		aliasCandidates.forEach(function (alias) {
			if (!alias) return;
			var esc = alias.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
			// \*? tolerates Sybase/ASE's legacy outer-join operators (*= / =*), not just ANSI '=' -
			// real captured ASE plans lean heavily on these instead of LEFT/RIGHT JOIN. "?...\"? tolerates
			// a double-quoted column name (alias."colName") - common in captured real-world SQL.
			var re1 = new RegExp('\\b' + esc + '\\."?(\\w+)"?\\s*\\*?=', 'gi');
			var re2 = new RegExp('=\\*?\\s*' + esc + '\\."?(\\w+)"?\\b', 'gi');
			var m;
			while ((m = re1.exec(sql)) !== null) results.push(m[1]);
			while ((m = re2.exec(sql)) !== null) results.push(m[1]);
		});
		return dedupPreserveOrder(results);
	}

	// A table referenced with no alias, inside a scope where it's the only table (e.g. a small
	// derived-table subquery: FROM "xMAKARND" WHERE XMAKLAR = ?), has nothing to qualify its columns
	// with - "XMAKLAR", not "xMAKARND.XMAKLAR". Finds the table's own FROM clause and scans forward,
	// stopping at the next UNION/closing paren so a large statement's unrelated later parts aren't
	// swept in, for a bare "WHERE/AND <col> =" predicate.
	function regexFindBareColumnNearTable(sql, tableName) {
		var esc = tableName.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
		var fromRe = new RegExp('FROM\\s+"?' + esc + '"?\\b', 'i');
		var m = fromRe.exec(sql);
		if (!m) return [];

		var scopeEnd = sql.length;
		['UNION', ')'].forEach(function (stopTok) {
			var idx = sql.indexOf(stopTok, m.index + m[0].length);
			if (idx !== -1 && idx < scopeEnd) scopeEnd = idx;
		});
		var scope = sql.slice(m.index, scopeEnd);

		var results = [];
		var colRe = /\b(?:WHERE|AND)\s+"?(\w+)"?\s*\*?=/gi;
		var cm;
		while ((cm = colRe.exec(scope)) !== null) results.push(cm[1]);
		return dedupPreserveOrder(results);
	}

	function findJoinColumnsForTable(sql, tableName, corrName, callback) {
		if (!sql) { callback([]); return; }
		var aliasCandidates = [corrName, tableName].filter(Boolean);
		if (!aliasCandidates.length) { callback([]); return; }

		_nspLoad(function (ok) {
			if (ok && typeof NodeSQLParser !== 'undefined') {
				try {
					var parser = new NodeSQLParser.Parser();
					var ast    = parser.astify(sql, { database: 'TransactSQL' });
					for (var i = 0; i < aliasCandidates.length; i++) {
						var cols = astFindJoinColumns(ast, aliasCandidates[i].toLowerCase());
						if (cols.length) { callback(cols); return; }
					}
				} catch (e) { /* fall through to regex below */ }
			}
			var cols = regexFindJoinColumns(sql, aliasCandidates);
			if (!cols.length) cols = regexFindBareColumnNearTable(sql, tableName);
			callback(cols);
		});
	}

	return {
		extractTablesAsync: extractTablesAsync,
		findJoinColumnsForTable: findJoinColumnsForTable
	};
})();
