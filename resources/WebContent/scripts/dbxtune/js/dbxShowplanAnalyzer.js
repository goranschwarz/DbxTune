/**
 * dbxShowplanAnalyzer.js — SQL Server XML Execution Plan analyzer
 *
 * Parses a SQL Server XML showplan and produces a list of "findings":
 * warnings, anti-patterns, and interesting observations extracted from
 * the plan tree.  Runs entirely in the browser — no server round-trip.
 *
 * Entry point:
 *   var findings = DbxShowplanAnalyzer.analyze(xmlText);
 *   // findings: [ { severity, category, title, detail, nodeId, nodeName } ]
 *
 * Severity levels:  'error' | 'warning' | 'info'
 *
 * Based on patterns from:
 *   - Erik Darling's PerformanceStudio (https://github.com/erikdarlingdata/PerformanceStudio)
 *   - Brent Ozar's sp_BlitzCache
 *   - SQL Server XML Showplan XSD (sql2022)
 */

window.DbxShowplanAnalyzer = (function () {

	// ── regexes ──────────────────────────────────────────────────────────────

	var FunctionInPredicateRegex = /\b(CONVERT_IMPLICIT|CONVERT|CAST|isnull|coalesce|datepart|datediff|dateadd|year|month|day|upper|lower|ltrim|rtrim|trim|substring|left|right|charindex|replace|len|datalength|abs|floor|ceiling|round|reverse|stuff|format)\s*\(/i;
	var LeadingWildcardLikeRegex = /\blike\b[^'""]*?N?'%/i;
	var CaseInPredicateRegex = /\bCASE\s+(WHEN\b|$)/i;
	var CteDefinitionRegex = /(?:\bWITH\s+|\,\s*)(\w+)\s+AS\s*\(/i;

	// ── helpers ──────────────────────────────────────────────────────────────

	function attr(el, name) {
		return el ? el.getAttribute(name) : null;
	}

	function numAttr(el, name) {
		var v = attr(el, name);
		return v !== null ? parseFloat(v) : null;
	}

	function boolAttr(el, name) {
		var v = attr(el, name);
		return v === '1' || v === 'true';
	}

	function childrenByTag(el, tag) {
		if (!el) return [];
		// Strip namespace prefix if present
		var localTag = tag.split(':').pop();
		var result = [];
		var children = el.childNodes;
		for (var i = 0; i < children.length; i++) {
			var c = children[i];
			if (c.nodeType === 1) {
				var ln = c.localName || c.nodeName.split(':').pop();
				if (ln === localTag) result.push(c);
			}
		}
		return result;
	}

	function descendantsByTag(root, tag) {
		if (!root) return [];
		var localTag = tag.split(':').pop();
		var result = [];
		var all = root.getElementsByTagName ? root.getElementsByTagName(tag) : [];
		// Also try with namespace wildcard
		if (all.length === 0 && root.getElementsByTagNameNS) {
			all = root.getElementsByTagNameNS('*', localTag);
		}
		for (var i = 0; i < all.length; i++) result.push(all[i]);
		return result;
	}

	function firstByTag(root, tag) {
		var els = descendantsByTag(root, tag);
		return els.length > 0 ? els[0] : null;
	}

	function getWaitStats(doc) {
		var waits = [];
		var ws = firstByTag(doc, 'WaitStats');
		if (ws) {
			descendantsByTag(ws, 'Wait').forEach(function(w) {
				waits.push({
					type: attr(w, 'WaitType'),
					ms: numAttr(w, 'WaitTimeMs'),
					count: numAttr(w, 'WaitCount')
				});
			});
		}
		return waits;
	}

	function getSerialPlanReason(reasonCode) {
		var reasons = {
			"MaxDOPSetToOne":                                                  "MAXDOP 1 is configured (server/DB/Resource Governor level)",
			"QueryHintNoParallelSet":                                          "OPTION (MAXDOP 1) hint forces serial execution",
			"EstimatedDOPIsOne":                                               "Optimizer estimated DOP = 1 (serial plan expected to be optimal)",
			"TSQLUserDefinedFunctionsNotParallelizable":                        "T-SQL scalar UDF prevents parallelism",
			"CouldNotGenerateValidParallelPlan":                               "Optimizer could not generate a valid parallel plan (check for UDFs or table variable modifications)",
			"ParallelismDisabledByTraceFlag":                                  "Parallelism disabled by trace flag",
			"NoParallelPlansInDesktopOrExpressEdition":                        "Express/Desktop edition does not support parallelism",
			"TableVariableTransactionsDoNotSupportParallelNestedTransaction":  "Table variable modification prevents parallelism",
			"DMLQueryReturnsOutputToClient":                                   "DML with OUTPUT clause returning results to client prevents parallelism",
			"NoParallelForMemoryOptimizedTables":                              "Memory-optimized tables do not support parallel plans",
			"NoParallelWithRemoteQuery":                                       "Remote queries cannot use parallelism",
			"CLRUserDefinedFunctionRequiresDataAccess":                        "CLR UDF with data access prevents parallelism",
			"NonParallelizableIntrinsicFunction":                              "Non-parallelizable intrinsic function prevents parallelism",
			"UpdatingWritebackVariable":                                       "Writing to a local variable forces serial execution",
			"NoParallelCursorFetchByBookmark":                                 "Cursor fetch by bookmark prevents parallelism",
			"NoParallelDynamicCursor":                                         "Dynamic cursor prevents parallelism",
			"NoParallelForNativelyCompiledModule":                             "Natively compiled module prevents parallelism",
			"NoParallelCreateIndexInNonEnterpriseEdition":                     "Non-Enterprise edition: parallel index build not available",
			"MixedSerialAndParallelOnlineIndexBuildNotSupported":              "Mixed serial/parallel online index build not supported"
		};
		return reasons[reasonCode] || reasonCode;
	}

	function isRowstoreScan(op) {
		var phys = attr(op, 'PhysicalOp') || '';
		return (phys.indexOf('Scan') !== -1) && 
		       (phys.indexOf('Spool') === -1) && 
		       (phys.indexOf('Constant') === -1) && 
		       (phys.indexOf('Columnstore') === -1);
	}

	// ── Expr### resolution ───────────────────────────────────────────────────
	//
	// SQL Server uses Expr#### aliases for computed values.  Every operator
	// that produces such a value has a <DefinedValues><DefinedValue> block:
	//
	//   <DefinedValue>
	//     <ColumnReference Column="Expr1004" />          ← the alias
	//     <ScalarOperator ScalarString="[t].[a]+[t].[b]">   ← the definition
	//       ...
	//     </ScalarOperator>
	//   </DefinedValue>
	//
	// ScalarString is what SQL Server itself renders as the expression text, so
	// we use it directly.  If it is absent (uncommon) we attempt a best-effort
	// recursive walk of the XML tree.

	/**
	 * Scan the whole plan and return a map { 'Expr1004': 'scalarString' }.
	 * The scalar string may itself contain Expr### references (chained aliases).
	 */
	function buildExprMap(doc) {
		var map = {};
		descendantsByTag(doc, 'DefinedValue').forEach(function(dv) {
			var exprName  = null;
			var scalarStr = null;
			// Walk direct children: first ColumnReference = the alias,
			// ScalarOperator = the definition.  Some plans wrap it in ValueVector.
			for (var i = 0; i < dv.childNodes.length; i++) {
				var c  = dv.childNodes[i];
				if (c.nodeType !== 1) continue;
				var ln = (c.localName || c.nodeName).split(':').pop();
				if (ln === 'ColumnReference' && !exprName) {
					exprName = attr(c, 'Column');
				} else if (ln === 'ValueVector' && !exprName) {
					// <ValueVector><ColumnReference .../></ValueVector>
					var inner = firstByTag(c, 'ColumnReference');
					if (inner) exprName = attr(inner, 'Column');
				} else if (ln === 'ScalarOperator' && scalarStr === null) {
					// Use ScalarString when present — it's already human-readable.
					scalarStr = attr(c, 'ScalarString');
					if (!scalarStr) {
						// Fallback: try to extract a simple Identifier → ColumnReference
						var cr = firstByTag(c, 'ColumnReference');
						if (cr) {
							var tbl = attr(cr, 'Table') || '';
							var col = attr(cr, 'Column') || '?';
							scalarStr = tbl ? tbl + '.' + col : col;
						}
					}
				}
			}
			if (exprName && /^Expr\d+/.test(exprName) && scalarStr) {
				map[exprName] = scalarStr;
			}
		});
		return map;
	}

	/**
	 * Resolve all Expr### tokens inside `str` using the map, up to `depth` levels.
	 * Returns { text, clean } where clean=true means every leaf is a direct
	 * column reference with no arithmetic/function operators.
	 *
	 * A result is "clean" only if it is a simple [Table].[Column] pattern.
	 * Anything with operators (+, -, CONVERT, function calls, etc.) is a "guess"
	 * because it is a derived value, not a real storage column.
	 */
	function resolveExprStr(str, exprMap, depth) {
		if (depth > 8) return { text: str, clean: false };

		var clean  = true;
		// Replace each Expr### token with its resolved form
		var result = str.replace(/Expr\d+/g, function(m) {
			if (!exprMap[m]) { clean = false; return m; }   // unresolvable → not clean
			var inner = resolveExprStr(exprMap[m], exprMap, depth + 1);
			if (!inner.clean) clean = false;
			return inner.text;
		});

		// After resolving Expr chains, decide if the result is "clean":
		// a simple column reference looks like [name] or [t].[c] or t.c
		// with no operator characters and no parentheses.
		if (clean) {
			var stripped = result.trim();
			// Allow: word chars, spaces, dots, brackets, @
			// Disallow: +  -  *  /  (  )  =  <  >  !  comma  CONVERT keyword etc.
			clean = /^[\w\s@.\[\]]+$/.test(stripped) && !/^\d+$/.test(stripped);
		}
		return { text: result, clean: clean };
	}

	/**
	 * Resolve a single column name.  If it is an Expr###, look it up.
	 * Returns { text, clean } as above.
	 */
	function resolveCol(colName, exprMap) {
		if (!colName) return { text: '?', clean: false };
		if (!/^Expr\d+/.test(colName)) return { text: colName, clean: true };
		if (!exprMap[colName])          return { text: colName, clean: false };
		return resolveExprStr(exprMap[colName], exprMap, 0);
	}

	// ── Finding builder ───────────────────────────────────────────────────────

	function finding(severity, category, title, detail, nodeId, nodeName) {
		return { severity: severity, category: category, title: title,
		         detail: detail || '', nodeId: nodeId || null, nodeName: nodeName || null };
	}

	// ── Main analyzer ─────────────────────────────────────────────────────────

	function analyze(xmlText) {
		if (!xmlText || !xmlText.trim()) return [];

		var doc;
		try {
			var parser = new DOMParser();
			doc = parser.parseFromString(xmlText, 'text/xml');
		} catch (e) {
			return [finding('error', 'Parse', 'XML parse error', String(e))];
		}

		// Check for parser error
		var parseErr = doc.querySelector('parsererror');
		if (parseErr) {
			return [finding('error', 'Parse', 'XML parse error', parseErr.textContent)];
		}

		var findings = [];

		// Build Expr### → ScalarString map for the whole plan
		var exprMap = buildExprMap(doc);

		// Collect all RelOp (operator) nodes
		var relOps = descendantsByTag(doc, 'RelOp');

		// Statement-level info & metadata
		var stmtSimple  = firstByTag(doc, 'StmtSimple');
		var stmtText    = stmtSimple ? attr(stmtSimple, 'StatementText') : null;
		var stmtCost    = stmtSimple ? numAttr(stmtSimple, 'StatementSubTreeCost') : 0;
		var optLevel    = stmtSimple ? attr(stmtSimple, 'StatementOptmLevel') : '';
		var abortReason = stmtSimple ? attr(stmtSimple, 'StatementOptmEarlyAbortReason') : '';
		// NonParallelPlanReason lives on QueryPlan in modern plans, StmtSimple in older ones
		var queryPlan     = firstByTag(doc, 'QueryPlan');
		var serialCode    = (queryPlan  ? attr(queryPlan,  'NonParallelPlanReason') : '')
		                 || (stmtSimple ? attr(stmtSimple, 'NonParallelPlanReason') : '');

		var compileTime   = queryPlan ? numAttr(queryPlan, 'CompileTime') : 0;
		var compileCPU    = queryPlan ? numAttr(queryPlan, 'CompileCPU') : 0;
		var estDop        = queryPlan ? numAttr(queryPlan, 'DegreeOfParallelism') : 0;
		var waitStats     = getWaitStats(doc);
		var queryTime     = firstByTag(doc, 'QueryTimeStats');
		// ElapsedTime/CpuTime (plan schema v1.5+); older schemas used ElapsedTimeMs/CpuTimeMs
		var actualElapsed = queryTime ? (numAttr(queryTime, 'ElapsedTime') || numAttr(queryTime, 'ElapsedTimeMs') || 0) : 0;
		var actualCPU     = queryTime ? (numAttr(queryTime, 'CpuTime')     || numAttr(queryTime, 'CpuTimeMs')     || 0) : 0;
		var udfElapsedMs  = queryTime ? (numAttr(queryTime, 'UdfElapsedTime') || 0) : 0;
		var udfCpuMs      = queryTime ? (numAttr(queryTime, 'UdfCpuTime')     || 0) : 0;

		// S.0 Wait Stats are rendered as a bar chart in the Plan Analysis header — no finding needed.

		// ── S.1 Serial Plan Reasons (Rule 3) ─────────────────────────────────
		if (serialCode && stmtCost >= 1.0 && optLevel !== 'TRIVIAL' && (actualElapsed === null || actualElapsed > 0)) {
			var reason = getSerialPlanReason(serialCode);
			var serialSev;
			if (serialCode === 'EstimatedDOPIsOne') {
				serialSev = 'info';   // Optimizer chose serial — not actionable
			} else if (serialCode === 'MaxDOPSetToOne') {
				// If MAXDOP 1 appears explicitly in the query text it is a user hint → warning.
				// If it comes from server/DB/Resource Governor config → info only (not actionable here).
				serialSev = (stmtText && /\bMAXDOP\s+1\b/i.test(stmtText)) ? 'warning' : 'info';
			} else if (["QueryHintNoParallelSet",
			            "TSQLUserDefinedFunctionsNotParallelizable",
			            "CouldNotGenerateValidParallelPlan",
			            "CLRUserDefinedFunctionRequiresDataAccess"].indexOf(serialCode) !== -1) {
				serialSev = 'warning';
			} else {
				serialSev = 'info';
			}
			findings.push(finding(serialSev, 'Serial Plan',
				'Query running serially: ' + reason,
				'Cost: ' + stmtCost.toFixed(2) + '. The query might benefit from parallelism but was forced to run on a single thread.'));
		}

		// ── S.2 Compilation Issues (Rule 18, 19) ─────────────────────────────
		if (abortReason === 'MemoryLimitExceeded') {
			findings.push(finding('error', 'Compile',
				'Compile memory exceeded',
				'Optimization was aborted early because the compile memory limit was exceeded. The plan is likely suboptimal.'));
		}
		if (compileCPU >= 1000) {
			var compileSev     = compileCPU >= 5000 ? 'error' : 'warning';
			var compileBenefit = (actualElapsed > 0) ? ' — ' + Math.min(100, Math.round(compileCPU / actualElapsed * 100)) + '% of elapsed' : '';
			findings.push(finding(compileSev, 'Compile',
				'High Compile CPU (' + compileCPU + 'ms' + compileBenefit + ')',
				'Query took significant CPU time just to compile. Consider simplifying the query or using plan guides to stabilize the plan.'
				+ (compileCPU >= 5000 ? '\n⚠ Extreme compile overhead — consider breaking the query into smaller parts using #temp tables.' : '')));
		}

		// ── S.3 Parallelism Efficiency (Rule 25, 31) ─────────────────────────
		if (estDop > 1 && actualElapsed >= 1000 && actualCPU > 0) {
			var speedup = actualCPU / actualElapsed;
			var efficiency = Math.max(0, Math.min(100, (speedup - 1) / (estDop - 1) * 100));

			var idealElapsed = actualCPU / estDop;  // what elapsed would be if perfectly CPU-bound
			var benefit_pct  = actualElapsed > 0 ? Math.min(100, Math.round((actualElapsed - idealElapsed) / actualElapsed * 100)) : 0;
			if (speedup < 0.5) {
				findings.push(finding('warning', 'Parallelism',
					'Parallel wait bottleneck (efficiency: ' + efficiency.toFixed(0) + '%)',
					'Threads spent most of their time waiting (likely on I/O or locks) rather than working on CPU.'
					+ '\nIdeal elapsed if CPU-bound: ' + (idealElapsed / 1000).toFixed(2) + 's. Potential saving: ~' + benefit_pct + '% of elapsed.'));
			} else if (efficiency < 40) {
				var parallelSev = efficiency < 20 ? 'error' : 'warning';
				findings.push(finding(parallelSev, 'Parallelism',
					'Ineffective parallelism (' + efficiency.toFixed(0) + '%, DOP ' + estDop + ')',
					'CPU usage is not scaling well with the degree of parallelism.'
					+ '\nIdeal elapsed (full DOP utilization): ' + (idealElapsed / 1000).toFixed(2) + 's. Potential saving: ~' + benefit_pct + '% of elapsed.'));
			}
		}

		// ── S.4 Local Variables without RECOMPILE (Rule 20) ─────────────────
		var paramList = firstByTag(doc, 'ParameterList');
		if (paramList) {
			var unsniffed = [];
			descendantsByTag(paramList, 'ColumnReference').forEach(function(p) {
				if (attr(p, 'ParameterCompiledValue') === null) {
					unsniffed.push(attr(p, 'Column'));
				}
			});
			if (unsniffed.length > 0 && stmtText && !/\bRECOMPILE\b/i.test(stmtText) && stmtCost >= 1.0) {
				findings.push(finding('warning', 'Local Variables',
					'Local variables detected: ' + unsniffed.join(', '),
					'SQL Server cannot sniff local variable values at compile time. This can lead to average density estimates instead of actual values. Consider OPTION (RECOMPILE).'));
			}
		}

		// ── S.5 OPTIMIZE FOR UNKNOWN (Rule 27) ──────────────────────────────
		if (stmtText && /\bOPTIMIZE\s+FOR\s+UNKNOWN\b/i.test(stmtText)) {
			findings.push(finding('warning', 'Hint',
				'OPTIMIZE FOR UNKNOWN applied',
				'This hint uses average density estimates instead of sniffed parameter values. It can help with plan instability but may be suboptimal for skewed data.'));
		}

		// ── 1. Missing index suggestions & Quality (Rule 30) ─────────────────
		var missingIndexGroups = descendantsByTag(doc, 'MissingIndexGroup');
		missingIndexGroups.forEach(function(mig) {
			var impact = numAttr(mig, 'Impact');
			var mis = descendantsByTag(mig, 'MissingIndex');
			mis.forEach(function(mi) {
				var db    = attr(mi, 'Database');
				var table = attr(mi, 'Table');
				var equality = [];
				var inequality = [];
				var includes = [];
				descendantsByTag(mi, 'ColumnGroup').forEach(function(cg) {
					var usage = attr(cg, 'Usage');
					descendantsByTag(cg, 'Column').forEach(function(c) {
						var name = attr(c, 'Name');
						if (usage === 'EQUALITY') equality.push(name);
						else if (usage === 'INEQUALITY') inequality.push(name);
						else if (usage === 'INCLUDE') includes.push(name);
					});
				});

				var title = 'Missing index suggestion (impact ' + (impact ? impact.toFixed(1) : '?') + '%)';
				var detail = 'Table: ' + (db || '') + '..' + (table || '') + '\nColumns: ' + equality.concat(inequality).join(', ') + (includes.length ? ' INCLUDE ' + includes.join(', ') : '');

				if (includes.length > 5) {
					detail += '\n⚠ "Kitchen sink" index: SQL Server is suggesting many INCLUDE columns. Evaluate which ones are actually needed.';
				}
				if (equality.length + inequality.length > 4) {
					detail += '\n⚠ Wide key columns: Evaluating ' + (equality.length + inequality.length) + ' key columns may increase maintenance cost.';
				}

				findings.push(finding(impact < 25 ? 'info' : 'warning', 'Missing Index', title, detail));
			});
		});

		// ── 2. Warnings on operators ──────────────────────────────────────────
		var warnings = descendantsByTag(doc, 'Warnings');
		warnings.forEach(function(w) {
			// SpillToTempDb
			descendantsByTag(w, 'SpillToTempDb').forEach(function(s) {
				var spills = numAttr(s, 'SpillLevel') || numAttr(s, 'SpillPages') || 1;
				var parent = w.parentNode;
				var nodeId = parent ? attr(parent, 'NodeId') : null;
				var op     = parent ? attr(parent, 'PhysicalOp') || attr(parent, 'LogicalOp') : null;
				findings.push(finding('error', 'Spill',
					'Sort/Hash Spill to TempDB',
					'Operator: ' + (op || '?') + (spills > 1 ? ' (level ' + spills + ')' : '') +
					'\nData spilled to disk — indicates memory grant too small or bad cardinality estimate.',
					nodeId, op));
			});

			// NoJoinPredicate
			if (descendantsByTag(w, 'NoJoinPredicate').length > 0) {
				var parent = w.parentNode;
				findings.push(finding('error', 'Join',
					'No Join Predicate (Cross Join / Cartesian product)',
					'A join has no predicate — may be an accidental CROSS JOIN.',
					attr(parent, 'NodeId'), attr(parent, 'PhysicalOp')));
			}

			// ColumnsWithNoStatistics
			descendantsByTag(w, 'ColumnsWithNoStatistics').forEach(function(c) {
				var cols = [];
				descendantsByTag(c, 'ColumnReference').forEach(function(cr) {
					cols.push((attr(cr, 'Table') || '') + '.' + (attr(cr, 'Column') || ''));
				});
				findings.push(finding('warning', 'Statistics',
					'Columns with no statistics',
					'Columns: ' + cols.join(', ') + '\nCardinality estimates will be poor.'));
			});

			// PlanAffectingConvert
			descendantsByTag(w, 'PlanAffectingConvert').forEach(function(c) {
				var isSeekPlan = (attr(c, 'Expression') || '').indexOf('Seek Plan') !== -1;
				findings.push(finding(isSeekPlan ? 'error' : 'warning', 'Implicit Conversion',
					'Implicit conversion affecting plan',
					'Expression: ' + (attr(c, 'Expression') || '') +
					'\nConvertIssue: ' + (attr(c, 'ConvertIssue') || '') +
					'\nThis can prevent index seeks and cause scans.'));
			});

			// MemoryGrant warning
			descendantsByTag(w, 'MemoryGrantWarning').forEach(function(m) {
				findings.push(finding('warning', 'Memory',
					'Memory grant warning',
					'GrantWarningKind: ' + (attr(m, 'GrantWarningKind') || '?')));
			});
		});

		// ── 2b. Data Type Mismatch via GetRangeWithMismatchedTypes (Rule 13) ──
		// Compute Scalar nodes whose DefinedValues contain GetRangeWithMismatchedTypes
		// or GetRangeThroughConvert indicate a range predicate type mismatch that
		// silently prevents index seeks. This is distinct from PlanAffectingConvert.
		relOps.forEach(function(op) {
			if (attr(op, 'PhysicalOp') !== 'Compute Scalar') return;
			descendantsByTag(op, 'DefinedValue').forEach(function(dv) {
				var scalar = firstByTag(dv, 'ScalarOperator');
				if (!scalar) return;
				var ss = attr(scalar, 'ScalarString') || scalar.textContent || '';
				var marker = ss.indexOf('GetRangeWithMismatchedTypes') !== -1
					? 'GetRangeWithMismatchedTypes'
					: ss.indexOf('GetRangeThroughConvert') !== -1
						? 'GetRangeThroughConvert'
						: null;
				if (!marker) return;
				findings.push(finding('warning', 'Implicit Conversion',
					'Data type mismatch in range predicate (' + marker + ')',
					'SQL Server is converting types to evaluate a range predicate: ' + ss.substring(0, 300)
					+ '\nThis can prevent an index seek and introduce extra CPU overhead.'
					+ '\nFix: ensure query parameter types match the column data type exactly.',
					attr(op, 'NodeId'), 'Compute Scalar'));
			});
		});

		// ── 3. Key Lookup (bookmark lookup) & RID Lookup (Rule 10) ───────────
		relOps.forEach(function(op) {
			var phys = attr(op, 'PhysicalOp') || '';
			if (phys === 'Key Lookup' || phys.indexOf('RID Lookup') !== -1) {
				var nodeId = attr(op, 'NodeId');
				var rows   = numAttr(op, 'EstimateRows');
				var predicate = attr(op, 'Predicate');
				var isRid = phys.indexOf('RID Lookup') !== -1;
				
				var detail = 'Estimated rows: ' + (rows !== null ? rows : '?');
				if (isRid) detail += '\nThis table is a heap (no clustered index). Add a clustered index to improve performance.';
				else detail += '\nConsider adding covering columns to the nonclustered index.';
				
				if (predicate) detail += '\nResidual Predicate: ' + predicate;

				findings.push(finding(predicate || isRid ? 'error' : 'warning', isRid ? 'RID Lookup' : 'Key Lookup',
					phys + (predicate ? ' with residual predicate' : ''),
					detail, nodeId, phys));
			}
		});

		// ── 4. Table Scan / Clustered Index Scan (Rule 4, 32, 33) ─────────────
		relOps.forEach(function(op) {
			var phys = attr(op, 'PhysicalOp');
			if (phys === 'Table Scan' || phys === 'Clustered Index Scan') {
				var nodeId     = attr(op, 'NodeId');
				var estRows    = numAttr(op, 'EstimateRows');
				var estCost    = numAttr(op, 'EstimatedTotalSubtreeCost');
				var tableCard  = numAttr(op, 'TableCardinality');
				var objNode    = firstByTag(op, 'Object');
				var tableName  = objNode ? (attr(objNode, 'Table') || attr(objNode, 'Index') || '') : '';

				var actualRows = 0;
				var actualRowsRead = 0;
				var ri = firstByTag(op, 'RunTimeInformation');
				if (ri) {
					descendantsByTag(ri, 'RunTimeCountersPerThread').forEach(function(t) {
						actualRows += numAttr(t, 'ActualRows') || 0;
						actualRowsRead += numAttr(t, 'ActualRowsRead') || 0;
					});
				}

				if (estRows !== null && estRows > 1000) {
					var detail = 'Estimated rows: ' + estRows.toLocaleString();
					if (actualRowsRead > 0) {
						detail += '\nActual rows read: ' + actualRowsRead.toLocaleString() + ' (returned: ' + actualRows.toLocaleString() + ')';
						if (actualRowsRead > (actualRows || 1) * 10 && actualRowsRead > 10000) {
							detail += '\n⚠ High scan overhead: SQL Server read ' + (actualRowsRead / (actualRows || 1)).toFixed(1) + 'x more rows than it needed.';
						}
						
						// Rule 32: Cardinality misestimate on scan
						if (estRows > actualRows * 10 && (actualRows / (actualRowsRead || 1)) < 0.1) {
							findings.push(finding('error', 'Cardinality',
								'Scan Cardinality Misestimate',
								'Estimated ' + estRows.toLocaleString() + ' rows but only ' + actualRows.toLocaleString() + ' returned. This likely caused the optimizer to choose a scan instead of a seek.',
								nodeId, phys));
						}
					}
					
					// Rule 33: CE Guess
					if (tableCard > 100000 && !ri) {
						var sel = estRows / tableCard;
						var guess = null;
						if      (sel >= 0.29  && sel <= 0.31)  guess = "30% equality guess";
						else if (sel >= 0.098 && sel <= 0.102) guess = "10% inequality guess";
						else if (sel >= 0.08  && sel <= 0.10)  guess = "9% LIKE/range guess";
						else if (sel >= 0.155 && sel <= 0.175) guess = "~16.4% compound-predicate guess";
						else if (sel >= 0.005 && sel <= 0.015) guess = "1% multi-predicate guess";
						
						if (guess) {
							findings.push(finding('warning', 'Cardinality',
								'Estimated Plan CE Guess detected: ' + guess,
								'The optimizer used a default guess which may hide the need for an index.',
								nodeId, phys));
						}
					}

					if (estCost !== null) detail += '\nEstimated cost: ' + estCost.toFixed(4);
					detail += '\nConsider adding or refining an index to convert this scan into a seek.';

					findings.push(finding('warning', 'Scan',
						phys + ' on ' + (tableName || 'table'),
						detail, nodeId, phys));
				}
			}
		});

		// ── 4b. Scan With Residual Predicate (Rule 11) ───────────────────────
		// A rowstore scan with ANY predicate means SQL Server scans all rows then
		// filters — pushing the predicate into an index would be more efficient.
		// (non-SARGable predicates are separately reported in Rule 17; skip those
		//  here to avoid duplicates — Rule 17 gives more specific advice.)
		relOps.forEach(function(op) {
			if (!isRowstoreScan(op)) return;
			var predicate = attr(op, 'Predicate');
			if (!predicate) return;

			// Skip if non-SARGable — Rule 17 already covers that more specifically
			var isNonSargable = CaseInPredicateRegex.test(predicate)
				|| predicate.indexOf('CONVERT_IMPLICIT') !== -1
				|| /\b(isnull|coalesce)\s*\(/i.test(predicate)
				|| LeadingWildcardLikeRegex.test(predicate)
				|| FunctionInPredicateRegex.test(predicate);
			if (isNonSargable) return;

			var nodeId    = attr(op, 'NodeId');
			var phys      = attr(op, 'PhysicalOp');
			var objNode   = firstByTag(op, 'Object');
			var tableName = objNode ? (attr(objNode, 'Table') || attr(objNode, 'Index') || '') : '';

			var actualRowsRead = 0, actualRowsOut = 0;
			var ri11 = firstByTag(op, 'RunTimeInformation');
			if (ri11) {
				descendantsByTag(ri11, 'RunTimeCountersPerThread').forEach(function(t) {
					actualRowsRead += numAttr(t, 'ActualRowsRead') || 0;
					actualRowsOut  += numAttr(t, 'ActualRows')     || 0;
				});
			}

			var detail11 = 'Scan on ' + (tableName || 'table') + ' has a residual predicate — rows are read then filtered, not filtered at index level.'
				+ '\nPredicate: ' + predicate;
			if (actualRowsRead > 0) {
				var kept_pct = (actualRowsOut / actualRowsRead * 100).toFixed(1);
				detail11 += '\nActual: read ' + actualRowsRead.toLocaleString() + ', returned ' + actualRowsOut.toLocaleString() + ' (' + kept_pct + '% kept)';
			}
			detail11 += '\nConsider adding the predicate column(s) to an index to enable a seek.';

			findings.push(finding('warning', 'Scan',
				'Scan with residual predicate on ' + (tableName || 'table'),
				detail11, nodeId, phys));
		});

		// ── 5. Row estimate vs actual mismatch (Rule 5) ──────────────────────
		// (Only available in actual plans with RuntimeInformation)
		relOps.forEach(function(op) {
			// Skip Adaptive Joins — they self-correct at runtime
			if (attr(op, 'IsAdaptiveJoin') === '1' || attr(op, 'PhysicalOp') === 'Adaptive Join') return;

			var estRows = numAttr(op, 'EstimateRows');
			var ri = firstByTag(op, 'RunTimeInformation');
			if (!ri) return;

			var threads    = descendantsByTag(ri, 'RunTimeCountersPerThread');
			var actualTotal = 0;
			var execCount   = 0;
			threads.forEach(function(t) {
				actualTotal += numAttr(t, 'ActualRows') || 0;
				execCount    = Math.max(execCount, numAttr(t, 'ActualExecutions') || 1);
			});

			// Normalise to per-execution rows so inner-side NL operators compare fairly
			var actualPerExec = execCount > 1 ? actualTotal / execCount : actualTotal;

			if (estRows !== null && estRows > 0 && actualPerExec > 0) {
				var ratio = Math.max(estRows, actualPerExec) / Math.min(estRows, actualPerExec);
				if (ratio >= 10) {
					var phys = attr(op, 'PhysicalOp') || attr(op, 'LogicalOp');

					// Walk ancestors for harm signals: spills, bad join types, Sort/Hash
					// (skip transparent operators: Parallelism, Compute Scalar, Filter, Segment, Top)
					var transparentOps = { 'Parallelism': 1, 'Compute Scalar': 1, 'Filter': 1, 'Segment': 1, 'Top': 1 };
					var hasHarm = firstByTag(op, 'Warnings') !== null || (phys && phys.indexOf('Join') !== -1);
					if (!hasHarm) {
						var anc = op.parentNode;
						for (var d = 0; anc && d < 6; d++) {
							var ancLn = (anc.localName || anc.nodeName).split(':').pop();
							if (ancLn === 'RelOp') {
								var ancPhys = attr(anc, 'PhysicalOp') || '';
								if (firstByTag(anc, 'Warnings') !== null
								    || ancPhys.indexOf('Join') !== -1
								    || ancPhys.indexOf('Sort') !== -1
								    || ancPhys.indexOf('Hash') !== -1) {
									hasHarm = true; break;
								}
								if (!transparentOps[ancPhys]) break; // stop at non-transparent op
							}
							anc = anc.parentNode;
						}
					}

					if (hasHarm) {
						var sev = ratio >= 100 ? 'error' : 'warning';
						findings.push(finding(sev, 'Cardinality',
							'Row estimate vs actual mismatch (' + ratio.toFixed(0) + 'x)',
							'Estimated: ' + estRows.toLocaleString()
							+ ', Actual per execution: ' + actualPerExec.toFixed(0)
							+ (execCount > 1 ? ' (' + execCount.toLocaleString() + ' executions)' : '')
							+ '\nPoor cardinality estimate contributing to suboptimal plan shape or resource allocation.',
							attr(op, 'NodeId'), phys));
					}
				}
			}
		});

		// ── 6. Parallelism (CXPACKET / large DOP) ────────────────────────────
		var hasParallelism = false;
		relOps.forEach(function(op) {
			if (attr(op, 'PhysicalOp') === 'Parallelism') hasParallelism = true;
		});
		var stmtDop = stmtSimple ? numAttr(stmtSimple, 'DegreeOfParallelism') : null;
		if (hasParallelism && stmtDop !== null && stmtDop > 8) {
			findings.push(finding('info', 'Parallelism',
				'High Degree of Parallelism (' + stmtDop + ')',
				'Query uses ' + stmtDop + ' threads. Verify MAXDOP setting is appropriate.'));
		}

		// ── 7. Adaptive Join ─────────────────────────────────────────────────
		relOps.forEach(function(op) {
			if (attr(op, 'PhysicalOp') === 'Adaptive Join') {
				findings.push(finding('info', 'Adaptive Join',
					'Adaptive Join',
					'Plan uses an Adaptive Join — SQL Server switches between Hash and Nested Loops at runtime based on actual row count.',
					attr(op, 'NodeId'), 'Adaptive Join'));
			}
		});

		// ── 8. Forced plan ───────────────────────────────────────────────────
		if (stmtSimple && attr(stmtSimple, 'QueryPlanCostPercentage') !== null) {
			var planGuideName = attr(stmtSimple, 'PlanGuideName');
			var useHint       = attr(stmtSimple, 'UseHintList');
			if (planGuideName) {
				findings.push(finding('info', 'Plan Guide',
					'Plan Guide: ' + planGuideName,
					'This plan was shaped by a plan guide.'));
			}
			if (useHint) {
				findings.push(finding('info', 'Hint',
					'USE HINT applied: ' + useHint, ''));
			}
		}

		// ── 9. Remote query (linked server) ──────────────────────────────────
		relOps.forEach(function(op) {
			var phys = attr(op, 'PhysicalOp');
			if (phys === 'Remote Query' || phys === 'Remote Scan' || phys === 'Remote Insert' || phys === 'Remote Update' || phys === 'Remote Delete') {
				findings.push(finding('warning', 'Remote Query',
					phys + ' — Linked Server access',
					'Query accesses a remote server. Performance depends on network and remote server load.',
					attr(op, 'NodeId'), phys));
			}
		});

		// ── 10. UDF calls (user-defined functions) ───────────────────────────
		// Statement-level UDF time (actual plans — QueryTimeStats/@UdfElapsedTime)
		if (udfElapsedMs > 0) {
			var udfBenefit = actualElapsed > 0 ? ' — ' + Math.min(100, Math.round(udfElapsedMs / actualElapsed * 100)) + '% of elapsed' : '';
			var udfStmtSev = udfElapsedMs >= 1000 ? 'error' : 'warning';
			findings.push(finding(udfStmtSev, 'UDF',
				'Scalar UDF elapsed time: ' + udfElapsedMs + 'ms' + udfBenefit,
				'Total time spent executing scalar UDFs for this statement: ' + udfElapsedMs + 'ms (CPU: ' + udfCpuMs + 'ms).'
				+ '\nScalar UDFs run row-by-row and prevent parallelism. Consider inlining logic or rewriting as inline TVFs.'));
		}
		relOps.forEach(function(op) {
			if (attr(op, 'LogicalOp') !== 'Compute Scalar') return;

			// Check per-node UDF elapsed time (actual plans)
			var nodeUdfMs = 0;
			var ri_udf = firstByTag(op, 'RunTimeInformation');
			if (ri_udf) {
				descendantsByTag(ri_udf, 'RunTimeCountersPerThread').forEach(function(t) {
					nodeUdfMs += numAttr(t, 'UdfElapsedTime') || 0; // ms
				});
			}
			if (nodeUdfMs > 0) {
				findings.push(finding(nodeUdfMs >= 1000 ? 'error' : 'warning', 'UDF',
					'Scalar UDF at node ' + (attr(op, 'NodeId') || '?') + ': ' + nodeUdfMs + 'ms',
					'UDF execution measured at ' + nodeUdfMs + 'ms for this operator. '
					+ 'Consider inlining the function logic or rewriting as an inline TVF.',
					attr(op, 'NodeId'), 'Compute Scalar'));
			} else {
				// Estimated plan fallback: look for UDF name in ScalarOperator text
				var defn = firstByTag(op, 'DefinedValue');
				if (defn) {
					var scal = firstByTag(defn, 'ScalarOperator');
					if (scal && scal.textContent && scal.textContent.indexOf('udf') !== -1) {
						findings.push(finding('warning', 'UDF',
							'Scalar UDF reference in Compute Scalar',
							'A scalar UDF is referenced in this operator. Scalar UDFs prevent parallelism and execute row-by-row. '
							+ 'Consider inlining or rewriting as an inline TVF.',
							attr(op, 'NodeId'), 'Compute Scalar'));
					}
				}
			}
		});

		// ── 11. Large estimated rows at root ─────────────────────────────────
		if (relOps.length > 0) {
			var root = relOps[0];
			var rootRows = numAttr(root, 'EstimateRows');
			var rootCost = numAttr(root, 'EstimatedTotalSubtreeCost');
			if (rootCost !== null && rootCost > 100) {
				findings.push(finding('info', 'Cost',
					'High estimated total cost: ' + rootCost.toFixed(0),
					'Estimated rows returned: ' + (rootRows !== null ? rootRows.toLocaleString() : '?') +
					'\nHigh-cost plans often benefit from index or query optimization.'));
			}
		}

		// ── 12. Memory Grant (Rule 9) ────────────────────────────────────────
		var memGrant = firstByTag(doc, 'MemoryGrantInfo');
		if (memGrant) {
			var granted  = numAttr(memGrant, 'GrantedMemory');
			var desired  = numAttr(memGrant, 'DesiredMemory');
			var used     = numAttr(memGrant, 'MaxUsedMemory');
			var wait     = numAttr(memGrant, 'GrantWaitTimeMs');

			if (wait > 0) {
				var memWaitBenefit = actualElapsed > 0 ? ' — ' + Math.min(100, Math.round(wait / actualElapsed * 100)) + '% of elapsed' : '';
				findings.push(finding(wait > 5000 ? 'error' : 'warning', 'Memory',
					'Memory grant wait: ' + wait + 'ms' + memWaitBenefit,
					'The query had to wait for a memory grant before starting.'
					+ (wait > 5000 ? '\n⚠ Severe grant wait — server memory pressure or over-estimated grant on a concurrent query.' : '')));
			}

			if (granted !== null && desired !== null) {
				if (granted < desired) {
					findings.push(finding('warning', 'Memory',
						'Memory grant insufficient (' + Math.round(granted/1024) + ' MB granted, ' + Math.round(desired/1024) + ' MB desired)',
						'Query received less memory than it needed. Spills are likely.'));
				} else if (used !== null && used < granted * 0.1 && granted > 102400) {
					findings.push(finding('warning', 'Memory',
						'Excessive memory grant (' + Math.round(granted/1024) + ' MB granted, only ' + Math.round(used/1024) + ' MB used)',
						'Overestimated by ' + (granted / (used || 1)).toFixed(1) + 'x. This memory is reserved and unavailable to other queries.'));
				} else if (granted > 1048576) {
					findings.push(finding('warning', 'Memory',
						'Large memory grant: ' + Math.round(granted/1024) + ' MB',
						'Queries with large memory grants can lead to resource semaphore waits.'));
				}
			}
		}

		// ── 13. Parameter sniffing signals ───────────────────────────────────
		var paramListEl = firstByTag(doc, 'ParameterList');
		if (paramListEl) {
			var params = descendantsByTag(paramListEl, 'ColumnReference');
			var sniffed = [];
			params.forEach(function(p) {
				var compiled = attr(p, 'ParameterCompiledValue');
				var runtime  = attr(p, 'ParameterRuntimeValue');
				if (compiled !== null && runtime !== null && compiled !== runtime) {
					sniffed.push(attr(p, 'Column') + ': compiled=' + compiled + ', runtime=' + runtime);
				}
			});
			if (sniffed.length > 0) {
				findings.push(finding('warning', 'Parameter Sniffing',
					'Parameter sniffing detected (' + sniffed.length + ' parameter(s) differ)',
					sniffed.join('\n') +
					'\nPlan compiled for different parameter values than currently executing. Consider OPTIMIZE FOR, RECOMPILE, or query hints.'));
			}
		}

		// ── 14. Nested Loops with many executions on inner side (Rule 16) ─────
		relOps.forEach(function(op) {
			if (attr(op, 'PhysicalOp') !== 'Nested Loops') return;
			if (attr(op, 'IsAdaptiveJoin') === '1') return; // Adaptive joins self-correct

			// Locate the NestedLoops element; its direct RelOp children are [outer, inner]
			var nlEl = firstByTag(op, 'NestedLoops');
			var innerRelOp = null;
			if (nlEl) {
				var nlRelOpChildren = childrenByTag(nlEl, 'RelOp');
				if (nlRelOpChildren.length >= 2) innerRelOp = nlRelOpChildren[1];
			}

			var innerActualExecs = 0;
			if (innerRelOp) {
				var ri = firstByTag(innerRelOp, 'RunTimeInformation');
				if (ri) {
					descendantsByTag(ri, 'RunTimeCountersPerThread').forEach(function(t) {
						innerActualExecs += numAttr(t, 'ActualExecutions') || 0;
					});
				}
			}

			var outerRows = numAttr(op, 'EstimateRows') || 0;
			if (innerActualExecs > 100000 || outerRows > 10000) {
				var nlSev = innerActualExecs > 1000000 ? 'error' : 'warning';
				findings.push(finding(nlSev, 'Join',
					'Nested Loops with high execution count',
					'Inner side executed ' + (innerActualExecs ? innerActualExecs.toLocaleString() : 'many') + ' times.'
					+ (innerActualExecs > 1000000 ? ' ⚠ Over 1 million inner-side executions.' : '')
					+ '\nA Hash or Merge Join may be more efficient for large outer inputs.',
					attr(op, 'NodeId'), 'Nested Loops'));
			}
		});

		// ── 15. Eager Index Spool (Rule 2) ────────────────────────────────────
		relOps.forEach(function(op) {
			var phys  = attr(op, 'PhysicalOp');
			var logic = attr(op, 'LogicalOp');
			if (phys !== 'Index Spool' || logic !== 'Eager Spool') return;

			var nodeId     = attr(op, 'NodeId');
			var estRows    = numAttr(op, 'EstimateRows');
			var rebinds    = numAttr(op, 'EstimateRebinds') || 0;
			var rewinds    = numAttr(op, 'EstimateRewinds') || 0;
			var executions = rebinds + rewinds + 1;

			// Key columns: Spool > SeekPredicateNew > SeekKeys > Prefix > RangeColumns > ColumnReference
			// (mirrors qp.xslt: s:Spool/s:SeekPredicateNew/s:SeekKeys/s:Prefix/s:RangeColumns/s:ColumnReference/@Column)
			var seekCols = [], anySeekGuess = false;
			var spoolEl  = firstByTag(op, 'Spool');
			var spnEl    = spoolEl ? firstByTag(spoolEl, 'SeekPredicateNew') : null;
			var seekKeys = spnEl   ? firstByTag(spnEl,   'SeekKeys')        : null;
			if (seekKeys) {
				childrenByTag(seekKeys, 'Prefix').forEach(function(prefix) {
					var rcEl = firstByTag(prefix, 'RangeColumns');
					if (!rcEl) return;
					childrenByTag(rcEl, 'ColumnReference').forEach(function(cr) {
						var col = attr(cr, 'Column') || '?';
						if (col === '?') return;
						var res = resolveCol(col, exprMap);
						if (seekCols.indexOf(res.text) === -1) {
							seekCols.push(res.text);
							if (!res.clean) anySeekGuess = true;
						}
					});
				});
			}

			// Schema/table from first ColumnReference in OutputList (mirrors qp.xslt)
			// Include columns: direct ColumnReference children of OutputList, minus seek keys
			var inclCols = [], anyInclGuess = false;
			var schema = '', table = '';
			var outputList = firstByTag(op, 'OutputList');
			if (outputList) {
				childrenByTag(outputList, 'ColumnReference').forEach(function(cr, i) {
					if (i === 0) {
						schema = attr(cr, 'Schema') || '';
						table  = attr(cr, 'Table')  || '';
					}
					var col = attr(cr, 'Column') || '?';
					if (col === '?') return;
					var res = resolveCol(col, exprMap);
					if (seekCols.indexOf(res.text) !== -1) return;  // already a key column
					if (inclCols.indexOf(res.text) !== -1) return;
					inclCols.push(res.text);
					if (!res.clean) anyInclGuess = true;
				});
			}
			var tableRef = (schema && table) ? schema + '.' + table
			             : table              ? table
			             : '<table>';

			// Cap INCLUDE list to avoid noise
			var inclTruncated = false;
			if (inclCols.length > 10) { inclCols = inclCols.slice(0, 10); inclTruncated = true; }

			var guessNote = (anySeekGuess || anyInclGuess)
				? '\n⚠ Column names marked with ~ are resolved from Expr### aliases — verify against your actual schema.'
				: '';

			var detail = 'Estimated rows: ' + (estRows !== null ? estRows.toLocaleString() : '?')
			           + ', Executions: ' + executions
			           + '\nSQL Server is building a temporary index at runtime — a permanent index is missing.'
			           + '\nThis is usually expensive and a strong signal to create a real index.';

			if (seekCols.length > 0) {
				detail += '\n\nCREATE NONCLUSTERED INDEX [IX_missing] ON ' + tableRef
				        + ' (' + seekCols.join(', ') + ')';
				if (inclCols.length > 0) {
					detail += '\nINCLUDE (' + inclCols.join(', ')
					        + (inclTruncated ? ', ...' : '') + ')';
				}
				detail += '\n-- WITH (SORT_IN_TEMPDB=ON, DATA_COMPRESSION=PAGE)';
			} else {
				detail += '\n\nCould not determine index key columns from this plan.';
			}
			if (guessNote) detail += '\n' + guessNote;

			findings.push(finding('error', 'Eager Spool',
				'Eager Index Spool — missing index!',
				detail, nodeId, 'Index Spool'));
		});

		// ── 15b. Lazy Spool with poor cache reuse (Rule 14) ──────────────────
		// A Lazy Table Spool is a correlated caching spool: it rebuilds ("rebinds")
		// when the outer key changes and replays ("rewinds") when it repeats.
		// If rebinds >> rewinds the cache is almost never reused — a waste.
		relOps.forEach(function(op) {
			if (attr(op, 'PhysicalOp') !== 'Table Spool' || attr(op, 'LogicalOp') !== 'Lazy Spool') return;

			var rebinds = numAttr(op, 'EstimateRebinds') || 0;
			var rewinds = numAttr(op, 'EstimateRewinds') || 0;

			// Prefer actual stats if available
			var ri_ls = firstByTag(op, 'RunTimeInformation');
			if (ri_ls) {
				var actReb = 0, actRew = 0;
				descendantsByTag(ri_ls, 'RunTimeCountersPerThread').forEach(function(t) {
					actReb += numAttr(t, 'ActualRebinds') || 0;
					actRew += numAttr(t, 'ActualRewinds') || 0;
				});
				if (actReb > 0 || actRew > 0) { rebinds = actReb; rewinds = actRew; }
			}

			if (rebinds > 100 && rewinds < rebinds * 5) {
				var lazySev = rewinds < rebinds ? 'error' : 'warning';
				findings.push(finding(lazySev, 'Spool',
					'Lazy Spool with poor cache reuse (rebinds: ' + rebinds.toLocaleString() + ', rewinds: ' + rewinds.toLocaleString() + ')',
					'The spool rebuilds its cache more often than it reuses it — the caching overhead exceeds its benefit.'
					+ (rewinds < rebinds ? '\n⚠ Cache NEVER reuses data (more misses than hits).' : '')
					+ '\nInvestigate why the outer input does not produce repeating key values for this spool.',
					attr(op, 'NodeId'), 'Table Spool'));
			}
		});

		// ── 16. Skewed parallelism (one thread doing most of the work) ─────────
		relOps.forEach(function(op) {
			if (attr(op, 'PhysicalOp') !== 'Parallelism') return;
			var ri = firstByTag(op, 'RunTimeInformation');
			if (!ri) return;
			var threads = descendantsByTag(ri, 'RunTimeCountersPerThread');
			if (threads.length < 2) return;
			var rowCounts = [];
			threads.forEach(function(t) {
				var rows = numAttr(t, 'ActualRows');
				if (rows !== null) rowCounts.push(rows);
			});
			if (rowCounts.length < 2) return;
			var total = rowCounts.reduce(function(a,b){return a+b;}, 0);
			if (total === 0) return;
			var maxRows = Math.max.apply(null, rowCounts);
			var avgRows = total / rowCounts.length;
			// DOP-aware threshold: 80% on one thread for DOP 2, 50% for higher DOP
			// Also require busiest thread > 2x average (filters trivially small result sets)
			var skewThreshold = rowCounts.length === 2 ? total * 0.80 : total * 0.50;
			if (maxRows > avgRows * 2 && maxRows > skewThreshold && total > rowCounts.length * 1000) {
				var pct = (maxRows / total * 100).toFixed(0);
				findings.push(finding('warning', 'Parallelism',
					'Skewed parallelism — thread imbalance (' + pct + '% on one thread)',
					'Busiest thread: ' + maxRows.toLocaleString() + ' rows of ' + total.toLocaleString() + ' total (' + pct + '%)'
					+ '\nThreads: [' + rowCounts.map(function(r){return r.toLocaleString();}).join(', ') + ']'
					+ '\nCauses: skewed data distribution, GUID keys, or non-parallel-friendly predicates.'
					+ '\nConsider: partition elimination, filtered indexes, or MAXDOP hints.',
					attr(op, 'NodeId'), 'Parallelism'));
			}
		});

		// ── 17. Non-SARGable Predicates (Rule 12) ─────────────────────────────
		relOps.forEach(function(op) {
			var predicate = attr(op, 'Predicate');
			if (predicate && isRowstoreScan(op)) {
				var issue = null;
				if (CaseInPredicateRegex.test(predicate)) issue = "CASE expression in predicate";
				else if (predicate.indexOf("CONVERT_IMPLICIT") !== -1) issue = "Implicit conversion (CONVERT_IMPLICIT)";
				else if (/\b(isnull|coalesce)\s*\(/.test(predicate)) issue = "ISNULL/COALESCE wrapping column";
				else if (LeadingWildcardLikeRegex.test(predicate)) issue = "Leading wildcard LIKE pattern";
				else if (FunctionInPredicateRegex.test(predicate)) issue = "Function call on column side";

				if (issue) {
					findings.push(finding('warning', 'Non-SARGable',
						issue + ' prevents index seek',
						'Predicate: ' + predicate + '\nThis forces a full scan of the index or table, preventing efficient seeks.',
						attr(op, 'NodeId'), attr(op, 'PhysicalOp')));
				}
			}
		});

		// ── 18. Table Variables (Rule 22) ───────────────────────────────────
		relOps.forEach(function(op) {
			var obj = firstByTag(op, 'Object');
			var table = obj ? attr(obj, 'Table') : '';
			if (table && table.indexOf('@') === 0) {
				var phys = attr(op, 'PhysicalOp') || '';
				var isMod = phys.indexOf('Insert') !== -1 || phys.indexOf('Update') !== -1 || phys.indexOf('Delete') !== -1;
				findings.push(finding(isMod ? 'error' : 'warning', 'Table Variable',
					'Table variable detected: ' + table,
					'Table variables lack statistics. ' + (isMod ? 'Modifying them forces serial execution for the entire plan.' : 'This can lead to poor row estimates and join choices.')));
			}
		});

		// ── 19. Table-Valued Functions (Rule 23) ─────────────────────────────
		relOps.forEach(function(op) {
			if (attr(op, 'LogicalOp') === 'Table-valued function') {
				var obj = firstByTag(op, 'Object');
				var name = obj ? attr(obj, 'Table') : 'TVF';
				findings.push(finding('warning', 'TVF',
					'Multi-statement Table-Valued Function: ' + name,
					'Multi-statement TVFs have fixed row estimates (1 or 100 rows) and lack statistics. Consider inlining or using a temp table.',
					attr(op, 'NodeId'), 'TVF'));
			}
		});

		// ── 20. CTE Multiple References (Rule 21) ────────────────────────────
		if (stmtText) {
			var cteRegex = new RegExp(CteDefinitionRegex.source, 'gi');
			var match;
			while ((match = cteRegex.exec(stmtText)) !== null) {
				var cteName = match[1];
				var refs = stmtText.match(new RegExp('\\b(FROM|JOIN)\\s+' + cteName + '\\b', 'gi'));
				if (refs && refs.length > 1) {
					findings.push(finding('warning', 'CTE',
						'CTE "' + cteName + '" is referenced ' + refs.length + ' times',
						'SQL Server re-executes the CTE for every reference. Materialize into a #temp table instead to improve performance.'));
				}
			}
		}

		// ── 21. Many-to-Many Merge Join (Rule 17) ────────────────────────────
		relOps.forEach(function(op) {
			if (boolAttr(op, 'ManyToMany') && (attr(op, 'PhysicalOp') || '').indexOf('Merge') !== -1) {
				findings.push(finding('warning', 'Join',
					'Many-to-many Merge Join',
					'Both sides of the Merge Join have duplicate values. This forces a worktable in TempDB, which can be expensive.',
					attr(op, 'NodeId'), 'Merge Join'));
			}
		});

		// ── 22. NOT IN with Nullable Columns (Rule 28) ───────────────────────
		relOps.forEach(function(op) {
			if (attr(op, 'PhysicalOp') === 'Row Count Spool') {
				var ri = firstByTag(op, 'RunTimeInformation');
				var rewinds = 0;
				if (ri) {
					descendantsByTag(ri, 'RunTimeCountersPerThread').forEach(function(t) {
						rewinds += numAttr(t, 'ActualRewinds') || 0;
					});
				}
				if (rewinds > 10000 && stmtText && /\bNOT\s+IN\b/i.test(stmtText)) {
					findings.push(finding('error', 'Anti-Semi Join',
						'NOT IN with nullable column pattern detected',
						'Row Count Spool has ' + rewinds.toLocaleString() + ' rewinds. This prevents efficient Anti-Semi Joins. Use NOT EXISTS or add WHERE col IS NOT NULL.',
						attr(op, 'NodeId'), 'Row Count Spool'));
				}
			}
		});

		// ── 23. Filter Operator discarding many rows (Rule 1) ─────────────────
		relOps.forEach(function(op) {
			if (attr(op, 'PhysicalOp') === 'Filter') {
				var actualRows = 0;
				var ri = firstByTag(op, 'RunTimeInformation');
				if (ri) {
					descendantsByTag(ri, 'RunTimeCountersPerThread').forEach(function(t) {
						actualRows += numAttr(t, 'ActualRows') || 0;
					});
				}

				var inputRows = 0;
				var innerRelOp = firstByTag(op, 'RelOp');
				if (innerRelOp) {
					var iri = firstByTag(innerRelOp, 'RunTimeInformation');
					if (iri) {
						descendantsByTag(iri, 'RunTimeCountersPerThread').forEach(function(t) {
							inputRows += numAttr(t, 'ActualRows') || 0;
						});
					}
				}

				if (inputRows > 10000 && actualRows < inputRows * 0.1) {
					findings.push(finding('warning', 'Filter',
						'Filter operator discarding many rows',
						'Filter received ' + inputRows.toLocaleString() + ' rows but only passed ' + actualRows.toLocaleString() + ' (' + (actualRows / (inputRows || 1) * 100).toFixed(1) + '%). Filtering should ideally happen earlier in the plan via an index.',
						attr(op, 'NodeId'), 'Filter'));
				}
			}
		});

		// ── 24. Join OR Expansion (Rule 15) ──────────────────────────────────
		relOps.forEach(function(op) {
			if (attr(op, 'PhysicalOp') !== 'Concatenation') return;
			var constantScans = descendantsByTag(op, 'ConstantScan');
			if (constantScans.length < 2) return;
			// Validate: a true OR expansion has a MergeInterval ancestor (not a plain UNION ALL)
			var hasMergeInterval = false;
			var checkAnc = op.parentNode;
			for (var mi_d = 0; checkAnc && mi_d < 10; mi_d++) {
				var miLn = (checkAnc.localName || checkAnc.nodeName).split(':').pop();
				if (miLn === 'MergeInterval') { hasMergeInterval = true; break; }
				checkAnc = checkAnc.parentNode;
			}
			if (!hasMergeInterval) return; // plain UNION ALL — not an OR expansion
			findings.push(finding('warning', 'Join',
				'Join OR clause (OR expansion)',
				'SQL Server rewrote an OR join predicate as multiple independent range lookups via Merge Interval. '
				+ 'This can be inefficient for large datasets. Consider rewriting with UNION ALL or separate queries.',
				attr(op, 'NodeId'), 'Concatenation'));
		});

		// ── 25. Top Above Scan (Rule 24) ─────────────────────────────────────
		relOps.forEach(function(op) {
			var phys = attr(op, 'PhysicalOp');
			if (phys === 'Top' || attr(op, 'LogicalOp') === 'Top N Sort') {
				var scanChild = firstByTag(op, 'RelOp');
				if (scanChild && isRowstoreScan(scanChild)) {
					findings.push(finding('warning', 'Scan',
						'Top/Sort above ' + attr(scanChild, 'PhysicalOp'),
						'The query is scanning and sorting just to return a few rows. An index on the ORDER BY columns could eliminate the sort.',
						attr(op, 'NodeId'), phys));
				}
			}
		});

		// ── 26. Row Goal (Rule 26) ───────────────────────────────────────────
		relOps.forEach(function(op) {
			var estRows = numAttr(op, 'EstimateRows');
			var estNoGoal = numAttr(op, 'EstimateRowsWithoutRowGoal');
			if (estNoGoal !== null && estRows !== null && estNoGoal > estRows * 2) {
				findings.push(finding('info', 'Row Goal',
					'Row Goal active: estimate reduced ' + (estNoGoal / estRows).toFixed(0) + 'x',
					'The optimizer changed the plan shape to find the first few rows quickly. This can be risky if the "goal" is not met fast.',
					attr(op, 'NodeId'), attr(op, 'PhysicalOp')));
			}
		});

		// Sort by severity (errors first, then warnings, then info)
		var sevOrder = { 'error': 0, 'warning': 1, 'info': 2 };
		findings.sort(function(a, b) {
			return (sevOrder[a.severity] || 2) - (sevOrder[b.severity] || 2);
		});

		return findings;
	}

	// ── Public API ────────────────────────────────────────────────────────────

	return {
		analyze: analyze
	};

}());
