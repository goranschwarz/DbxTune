/**
 * dbxShowplanSqlServer.js — Microsoft SQL Server graphical Showplan renderer
 *
 * Parses SQL Server's ShowPlanXML into the same shape-agnostic tree model dbxShowplanAse.js uses,
 * and renders it as a pure CSS/DOM box-and-connector diagram — no external graph/layout library.
 *
 * This is the "native" alternative to the vendored html-query-plan (QP.js) renderer that has drawn
 * SQL Server plans up to now. QP.js is an XSLT->DOM black box: there is no hook to find a given
 * node's box (so Plan Analysis "[Node N]" links could never scroll to anything), no orientation
 * control, no way to fold per-operator DDL Storage data into a box, and no SSMS-style Properties
 * view. Both renderers stay available side by side, toggled from the dialog toolbar, so plans can
 * be compared against the known-good one until this one has earned the default.
 *
 * Entry points:
 *   var parsed = SqlServerShowplan.parseXml(xmlText);
 *   SqlServerShowplan.render(containerEl, parsed, opts);
 *
 * parseXml() returns null when the input couldn't be recognized at all (with the reason available
 * via getLastParseError()), so callers can fall back to the raw XML view rather than showing a
 * broken/empty diagram - same contract as the ASE renderer's.
 *
 * Shared node model (mirrors dbxShowplanAse.js so the dialog wiring, the findings list and the
 * detail-panel rendering stay symmetric between the two vendors):
 *   {
 *     op:       'Clustered Index Scan',  // @PhysicalOp
 *     label:    'Clustered Index Scan',  // display label
 *     metrics:  { estRows, actRows, estIO, estCpu, subtreeCost, nodeCost, relativeCostPct, ... },
 *     props:    { nodeId, logicalOp, objName, indexName, predicate, ... },  // printed verbatim
 *     warnings: [ { type, attrs } ],
 *     children: [ node, ... ],
 *     _xmlEl:   <RelOp> element   // kept so the Properties pane can show EVERYTHING, generically
 *   }
 * Top-level result:
 *   { format: 'xml', rawText, statements: [ { label, meta, steps: [ { label, root: node } ] } ] }
 *
 * Structural notes, all verified against html-query-plan's own 55-plan test corpus
 * (https://github.com/JustinPealing/html-query-plan/tree/master/test_plans - 536 RelOps):
 *
 *  - Plan roots are found by locating every <QueryPlan> element and taking its single direct
 *    <RelOp> child. In the whole corpus that is 65 QueryPlans, each with exactly 1 direct RelOp
 *    child, reaching all 536 RelOps with 0 left over. That one rule covers StmtSimple,
 *    StmtCursor (StmtCursor/CursorPlan/Operation/QueryPlan/RelOp) and statements nested inside
 *    StmtCond's Condition/Then/Else branches uniformly - no per-statement-type walking needed,
 *    and anything Microsoft adds later that still wraps a plan in <QueryPlan> works for free.
 *
 *  - Every RelOp has exactly one "operator body" child (IndexScan, NestedLoops, Hash, Update, ...)
 *    once the known metadata siblings are excluded - 536 of 536 in the corpus. Child operators are
 *    still found by a depth-first walk that STOPS at the first RelOp rather than by knowing which
 *    body element nests them where, so branching arity (Concat's N, a join's 2, a Sort's 1) and
 *    unknown future operators need no special-casing.
 *
 *  - A .sqlplan whose XML declaration claims encoding="utf-16" while the bytes are ASCII is common
 *    (SSMS writes UTF-16; converting to UTF-8 leaves the declaration lying). This is harmless here:
 *    DOMParser ignores the declared encoding when parsing a STRING - verified in-browser. The one
 *    genuinely fatal shape is whitespace BEFORE the declaration, which trim() below handles.
 */

window.SqlServerShowplan = (function () {

	// Reason the most recent parseXml() call returned null, for callers that want to tell the user
	// *why* the graphical diagram fell back to the raw XML view.
	var lastParseError = null;

	// ─────────────────────────────────────────────────────────────────────────
	// XML helpers
	//
	// ShowPlanXML is namespaced (http://schemas.microsoft.com/sqlserver/2004/07/showplan), so every
	// lookup goes through localName rather than tagName/getElementsByTagName - the latter would need
	// the prefix the document happens to use, which varies between capture sources.
	// ─────────────────────────────────────────────────────────────────────────

	function lname(el) {
		return el.localName || (el.nodeName || '').replace(/^.*:/, '');
	}

	function childElements(el) {
		var out = [];
		if (!el) return out;
		for (var i = 0; i < el.childNodes.length; i++) {
			if (el.childNodes[i].nodeType === 1) out.push(el.childNodes[i]);
		}
		return out;
	}

	function firstChildByName(el, name) {
		var kids = childElements(el);
		for (var i = 0; i < kids.length; i++) {
			if (lname(kids[i]) === name) return kids[i];
		}
		return undefined;
	}

	function childrenByName(el, name) {
		return childElements(el).filter(function (k) { return lname(k) === name; });
	}

	/** Every element in the subtree (including el itself) whose localName matches. */
	function descendantsByName(el, name) {
		var out = [];
		(function walk(e) {
			if (lname(e) === name) out.push(e);
			var kids = childElements(e);
			for (var i = 0; i < kids.length; i++) walk(kids[i]);
		})(el);
		return out;
	}

	/**
	 * Like descendantsByName(), but never descends into a nested <RelOp> - so it only ever sees
	 * elements belonging to THIS operator, not to the operators feeding it.
	 *
	 * This matters for <Object>: a Nested Loops / Parallelism / Sort body contains its child RelOps,
	 * and those children have Objects of their own. An unrestricted search finds the first child's
	 * table and wrongly attributes it to the parent - which then shows a table name on the subtitle
	 * of an operator that touches no table, and (worse) makes it a candidate for the DDL Storage
	 * lookup and the "Large table" size warning.
	 */
	function ownDescendantsByName(el, name) {
		var out = [];
		(function walk(e, isRoot) {
			if (!isRoot && lname(e) === 'RelOp') return;
			if (lname(e) === name) out.push(e);
			var kids = childElements(e);
			for (var i = 0; i < kids.length; i++) walk(kids[i], false);
		})(el, true);
		return out;
	}

	function attr(el, name) {
		if (!el) return undefined;
		var v = el.getAttribute(name);
		return (v === null || v === '') ? undefined : v;
	}

	function num(el, name) {
		var t = attr(el, name);
		if (t === undefined) return undefined;
		var n = parseFloat(t);
		return isNaN(n) ? undefined : n;
	}

	/** All attributes of an element as a plain object, in document order. */
	function attrsOf(el) {
		var out = {};
		if (!el || !el.attributes) return out;
		for (var i = 0; i < el.attributes.length; i++) {
			out[el.attributes[i].name] = el.attributes[i].value;
		}
		return out;
	}

	function isTrue(v) {
		return v === '1' || v === 'true' || v === 'True';
	}

	function unbracket(s) {
		return s === undefined ? undefined : s.replace(/^\[|\]$/g, '');
	}

	// ─────────────────────────────────────────────────────────────────────────
	// Parser
	// ─────────────────────────────────────────────────────────────────────────

	// Children of <RelOp> that are metadata about the operator rather than THE operator body. What
	// remains after excluding these is the single body element (IndexScan/NestedLoops/Hash/...) whose
	// attributes describe what the operator actually does.
	var RELOP_META_TAGS = {
		OutputList: true, Warnings: true, RunTimeInformation: true, MemoryFractions: true,
		InternalInfo: true, RunTimePartitionSummary: true, DefinedValues: true
	};

	/**
	 * Every <RelOp> that feeds this one, found by descending until the first RelOp on each path.
	 * Deliberately structure-agnostic: it does not need to know that NestedLoops nests two, Concat
	 * nests N, or that a Spool usually nests none (it references another node by PrimaryNodeId
	 * instead) - it just stops as soon as it finds one.
	 */
	function childRelOps(relOpEl) {
		var out = [];
		childElements(relOpEl).forEach(function (kid) {
			(function walk(e) {
				if (lname(e) === 'RelOp') { out.push(e); return; }
				childElements(e).forEach(walk);
			})(kid);
		});
		return out;
	}

	/** The single non-metadata child of a RelOp - the operator body element. */
	function bodyElementOf(relOpEl) {
		var kids = childElements(relOpEl);
		for (var i = 0; i < kids.length; i++) {
			if (!RELOP_META_TAGS.hasOwnProperty(lname(kids[i]))) return kids[i];
		}
		return undefined;
	}

	/** "db.schema.table" from an <Object> element, skipping parts the plan didn't supply. */
	function objectNameOf(objEl) {
		if (!objEl) return undefined;
		var parts = [attr(objEl, 'Database'), attr(objEl, 'Schema'), attr(objEl, 'Table')]
			.filter(function (s) { return !!s; })
			.map(unbracket);
		return parts.length ? parts.join('.') : undefined;
	}

	/**
	 * Readable one-liner for a predicate-ish element. ShowPlanXML wraps predicates in arbitrarily
	 * deep <ScalarOperator> trees, but every one of them carries a flattened @ScalarString - so the
	 * outermost ScalarString is the whole expression, already in the form SSMS displays.
	 */
	function scalarStringOf(el) {
		if (!el) return undefined;
		if (attr(el, 'ScalarString')) return attr(el, 'ScalarString');
		var sc = descendantsByName(el, 'ScalarOperator');
		for (var i = 0; i < sc.length; i++) {
			if (attr(sc[i], 'ScalarString')) return attr(sc[i], 'ScalarString');
		}
		return undefined;
	}

	/** Comma-joined column list from any element containing <ColumnReference> descendants. */
	function columnListOf(el) {
		if (!el) return undefined;
		var cols = descendantsByName(el, 'ColumnReference').map(function (c) {
			var name = attr(c, 'Column');
			if (!name) return undefined;
			// Alias-qualify only when the plan itself qualified it - matches how SSMS prints these.
			var alias = attr(c, 'Alias') || attr(c, 'Table');
			return alias ? (unbracket(alias) + '.' + name) : name;
		}).filter(function (s) { return !!s; });
		return cols.length ? cols.join(', ') : undefined;
	}

	/**
	 * Roll up <RunTimeCountersPerThread> into single actual-execution numbers, the way SSMS does:
	 * counters that describe work done are SUMMED across threads, while elapsed time is a MAX (the
	 * threads ran concurrently, so adding their wall-clock times would badly overstate it).
	 */
	var RT_SUM_ATTRS = ['ActualRows', 'ActualRowsRead', 'ActualExecutions', 'ActualCPUms',
		'ActualLogicalReads', 'ActualPhysicalReads', 'ActualReadAheads', 'ActualScans',
		'ActualEndOfScans', 'ActualLobLogicalReads', 'ActualLobPhysicalReads', 'ActualLobReadAheads',
		'ActualRebinds', 'ActualRewinds', 'Batches', 'SegmentReads', 'SegmentSkips',
		'UsedMemoryGrant', 'InputMemoryGrant', 'OutputMemoryGrant'];
	var RT_MAX_ATTRS = ['ActualElapsedms'];

	function parseRunTimeInfo(relOpEl, node) {
		var rtInfo = firstChildByName(relOpEl, 'RunTimeInformation');
		if (!rtInfo) return;
		var threads = childrenByName(rtInfo, 'RunTimeCountersPerThread');
		if (!threads.length) return;

		node.metrics.threadCount = threads.length;

		// Kept per-thread (not just the summed total below) so the Properties pane can show the row
		// distribution across threads - "how many rows did EACH thread produce" is exactly what the
		// existing Skewed Parallelism finding (dbxShowplanAnalyzer.js) judges, but that verdict was
		// only ever visible as a Plan Analysis line; the numbers behind it were reachable only by
		// expanding the raw XML tree. @Thread is NOT reliably in document order - measured across the
		// reference corpus, 159 of 171 multi-thread RelOps had it out of order - so this is explicitly
		// sorted numerically rather than trusting element order.
		if (threads.length > 1) {
			node.metrics.perThread = threads.map(function (t) {
				return { thread: parseInt(attr(t, 'Thread'), 10), actualRows: num(t, 'ActualRows') };
			}).sort(function (a, b) { return a.thread - b.thread; });
		}
		RT_SUM_ATTRS.forEach(function (a) {
			var total, any = false;
			for (var i = 0; i < threads.length; i++) {
				var v = num(threads[i], a);
				if (v === undefined) continue;
				total = (total === undefined ? 0 : total) + v;
				any = true;
			}
			if (any) node.metrics[a] = total;
		});
		RT_MAX_ATTRS.forEach(function (a) {
			var best;
			for (var i = 0; i < threads.length; i++) {
				var v = num(threads[i], a);
				if (v === undefined) continue;
				if (best === undefined || v > best) best = v;
			}
			if (best !== undefined) node.metrics[a] = best;
		});
		// Convenience alias so the box/detail code can talk about "actual rows" without knowing which
		// vendor spelling produced it - mirrors metrics.actRows in the ASE model.
		if (node.metrics.ActualRows !== undefined) node.metrics.actRows = node.metrics.ActualRows;
		// Batch vs Row mode - SSMS shows this per operator and it explains large row-throughput gaps.
		var mode = attr(threads[0], 'ActualExecutionMode');
		if (mode) node.props.actualExecutionMode = mode;
	}

	/** Operator-body properties worth surfacing on the box/summary panel, in a readable form. */
	function parseBodyProps(bodyEl, node) {
		if (!bodyEl) return;
		node.props.bodyTag = lname(bodyEl);

		// Every attribute of the body element, verbatim. The detail panel prints whatever is in
		// props with no per-key code, so new/unknown ShowPlanXML attributes surface automatically.
		var a = attrsOf(bodyEl);
		for (var k in a) {
			if (a.hasOwnProperty(k)) node.props[k] = a[k];
		}

		// <Object> - the table/index this operator touches, searched WITHOUT descending into the
		// child RelOps nested inside this body (see ownDescendantsByName). Several operators (Update,
		// and any seek/scan) carry more than one Object of their own; the first is the one being
		// read/written, which is what the subtitle and the DDL Storage lookup want.
		var objEl = ownDescendantsByName(bodyEl, 'Object')[0];
		if (objEl) {
			node.props.objName   = objectNameOf(objEl);
			node.props.indexName = unbracket(attr(objEl, 'Index'));
			node.props.indexKind = attr(objEl, 'IndexKind');
			node.props.alias     = unbracket(attr(objEl, 'Alias'));
			node.props.storage   = attr(objEl, 'Storage');
		}

		var pred = scalarStringOf(firstChildByName(bodyEl, 'Predicate'))
			|| scalarStringOf(firstChildByName(bodyEl, 'ProbeResidual'))
			|| scalarStringOf(firstChildByName(bodyEl, 'BuildResidual'));
		if (pred) node.props.predicate = pred;

		var seek = firstChildByName(bodyEl, 'SeekPredicates') || firstChildByName(bodyEl, 'SeekPredicateNew');
		if (seek) {
			var seekStr = scalarStringOf(seek) || columnListOf(seek);
			if (seekStr) node.props.seekPredicate = seekStr;
		}

		['OrderBy', 'GroupBy', 'HashKeysBuild', 'HashKeysProbe', 'PartitionColumns', 'OuterReferences']
			.forEach(function (tag) {
				var cols = columnListOf(firstChildByName(bodyEl, tag));
				if (cols) node.props[tag.charAt(0).toLowerCase() + tag.slice(1)] = cols;
			});
	}

	function parseWarnings(relOpEl, node) {
		var w = firstChildByName(relOpEl, 'Warnings');
		if (!w) return;
		node.warnings = childElements(w).map(function (el) {
			return { type: lname(el), attrs: attrsOf(el) };
		});
		// Most warnings are child elements (SpillToTempDb, PlanAffectingConvert, ...), but a few -
		// notably NoJoinPredicate - are boolean attributes on <Warnings> itself.
		var wa = attrsOf(w);
		for (var k in wa) {
			if (wa.hasOwnProperty(k) && isTrue(wa[k])) node.warnings.push({ type: k, attrs: {} });
		}
	}

	function buildNode(relOpEl) {
		var node = {
			op:       attr(relOpEl, 'PhysicalOp') || lname(relOpEl),
			label:    attr(relOpEl, 'PhysicalOp') || lname(relOpEl),
			metrics:  {},
			props:    {},
			warnings: [],
			children: [],
			_xmlEl:   relOpEl
		};

		node.props.nodeId    = attr(relOpEl, 'NodeId');
		node.props.logicalOp = attr(relOpEl, 'LogicalOp');
		if (isTrue(attr(relOpEl, 'Parallel'))) node.props.parallel = 'true';
		if (attr(relOpEl, 'EstimatedExecutionMode')) node.props.estimatedExecutionMode = attr(relOpEl, 'EstimatedExecutionMode');

		node.metrics.estRows          = num(relOpEl, 'EstimateRows');
		node.metrics.estRowsRead      = num(relOpEl, 'EstimatedRowsRead');
		node.metrics.estIO            = num(relOpEl, 'EstimateIO');
		node.metrics.estCpu           = num(relOpEl, 'EstimateCPU');
		node.metrics.avgRowSize       = num(relOpEl, 'AvgRowSize');
		node.metrics.subtreeCost      = num(relOpEl, 'EstimatedTotalSubtreeCost');
		node.metrics.tableCardinality = num(relOpEl, 'TableCardinality');
		node.metrics.estRebinds       = num(relOpEl, 'EstimateRebinds');
		node.metrics.estRewinds       = num(relOpEl, 'EstimateRewinds');

		parseBodyProps(bodyElementOf(relOpEl), node);
		parseRunTimeInfo(relOpEl, node);
		parseWarnings(relOpEl, node);

		// <OutputList> - the columns this operator passes upward. A direct child of <RelOp> itself
		// (a sibling of the body element, not inside it - confirmed against the corpus: all 536
		// RelOps carry one), which is why it is parsed here rather than in parseBodyProps(). Every
		// html-query-plan tooltip shows this; the native renderer's summary panel did not, which is
		// the gap this fixes. Kept as both the joined display string and the raw column array so the
		// detail panel can show a short excerpt while the Properties pane shows all of it - some
		// operators (wide SELECTs) carry 90+ columns, too many for a hover tooltip.
		var outputListEl = firstChildByName(relOpEl, 'OutputList');
		if (outputListEl) {
			var outCols = childrenByName(outputListEl, 'ColumnReference').map(function (c) {
				var name = attr(c, 'Column');
				if (!name) return undefined;
				var alias = attr(c, 'Alias') || attr(c, 'Table');
				return alias ? (unbracket(alias) + '.' + name) : name;
			}).filter(function (s) { return !!s; });
			if (outCols.length) {
				node.props.outputListCols = outCols;
				node.props.outputList = outCols.join(', ');
			}
		}

		node.children = childRelOps(relOpEl).map(buildNode);
		return node;
	}

	/**
	 * Per-operator cost. ShowPlanXML only publishes the CUMULATIVE subtree cost, so an operator's own
	 * cost is its subtree minus its children's subtrees - this is exactly how SSMS derives the
	 * "Cost: N%" it prints under every box. Clamped at 0 because parallel plans and rounding can
	 * otherwise produce small negative values.
	 */
	function computeCosts(root) {
		var totalCost = root.metrics.subtreeCost || 0;
		(function walk(n) {
			var kidSum = 0;
			n.children.forEach(function (c) { kidSum += (c.metrics.subtreeCost || 0); });
			var own = (n.metrics.subtreeCost || 0) - kidSum;
			n.metrics.nodeCost = own > 0 ? own : 0;
			n.metrics.relativeCostPct = totalCost > 0 ? (n.metrics.nodeCost / totalCost * 100) : 0;
			n.children.forEach(walk);
		})(root);
	}

	/**
	 * How many worker threads actually ran EACH parallel-flagged operator - not just whether the plan
	 * as a whole is parallel. Distinguishes real workers from the coordinator/consumer thread (Thread
	 * 0 - see the "Parallel Threads" note in buildThreadRows()) and from exchange/boundary operators
	 * (e.g. Gather Streams) that are marked Parallel="true" but themselves run on a single thread.
	 *
	 * On an actual plan this comes straight off <RunTimeInformation>: threadCount counts every
	 * <RunTimeCountersPerThread> element for this operator, but Thread 0 is only ONE of those elements
	 * when the operator actually runs partly on the coordinator (e.g. Parallelism/Gather Streams,
	 * which sits at the boundary and does real work on the serial side too). An operator that lives
	 * entirely inside the parallel zone - below a Repartition Streams, for instance - never touches
	 * the coordinator at all, so SQL Server emits threads numbered 1..DOP with NO Thread 0 entry.
	 * Blindly doing threadCount-1 there discards a genuine worker rather than a coordinator that was
	 * never counted in the first place (reported live: a Hash Match with Thread 1-4 in its actual
	 * per-thread breakdown - 4 real workers - showed a "3" worker-count chip). So: subtract 1 only
	 * when a Thread 0 entry is actually present among this operator's own threads.
	 *
	 * On an Estimated plan there is no per-operator thread data at all, so this falls back to the
	 * statement's overall Degree of Parallelism as a best-effort estimate - less precise (it cannot
	 * tell a boundary operator from a true worker without runtime data), but still better than no
	 * number at all.
	 */
	function applyParallelWorkerCounts(stmtNode) {
		var dop = parseInt(stmtNode.props.degreeOfParallelism, 10);
		(function walk(n) {
			var m = n.metrics, p = n.props;
			if (p.parallel) {
				if (m.threadCount > 1) {
					var hasCoordinatorThread = m.perThread && m.perThread.some(function (t) { return t.thread === 0; });
					m.parWorkers = hasCoordinatorThread ? m.threadCount - 1 : m.threadCount;
				}
				else if (m.threadCount === undefined && dop > 1) m.parWorkers = dop;
			}
			n.children.forEach(walk);
		})(stmtNode);
	}

	/**
	 * The statement box that sits at the head of every plan - the "SELECT" / "INSERT" / "UPDATE"
	 * node html-query-plan and SSMS both draw as the left-most (or top-most) node, with the whole
	 * operator tree feeding into it.
	 *
	 * It is a real node in the tree rather than a header line because that is what it is: the
	 * consumer the root operator returns its rows to. It also carries the statement-level runtime
	 * totals from <QueryTimeStats> (elapsed / CPU / UDF), which exist nowhere else in the plan -
	 * qp.xslt puts them on exactly this node too (its NodeTimeLabel template for s:StmtSimple).
	 *
	 * Cost is deliberately absent for StmtSimple, matching qp.xslt, which suppresses NodeCostLabel
	 * for s:StmtSimple|s:StmtUseDb and prints "Cost: 0%" only for cursor/conditional statements.
	 */
	function buildStatementNode(stmtEl, queryPlanEl, rootRelOp) {
		var stmtTag = stmtEl ? lname(stmtEl) : 'StmtSimple';
		var type    = (stmtEl && attr(stmtEl, 'StatementType')) || 'SELECT';

		var node = {
			op:          type,
			label:       type,
			isStatement: true,
			metrics:     {},
			props:       { stmtTag: stmtTag },
			warnings:    [],
			children:    rootRelOp ? [rootRelOp] : [],
			_xmlEl:      stmtEl || queryPlanEl
		};

		if (stmtEl) {
			var sa = attrsOf(stmtEl);
			for (var k in sa) { if (sa.hasOwnProperty(k)) node.props[k] = sa[k]; }
			node.metrics.estRows     = num(stmtEl, 'StatementEstRows');
			node.metrics.subtreeCost = num(stmtEl, 'StatementSubTreeCost');
		}
		if (node.metrics.subtreeCost === undefined && rootRelOp) {
			node.metrics.subtreeCost = rootRelOp.metrics.subtreeCost;
		}

		// Rows returned by the statement are exactly the rows its root operator emitted, so take the
		// actual count (and the estimate, when the statement itself carries none) from there. This is
		// also what html-query-plan ends up showing on its statement node - it has a cardinality
		// template only for s:RelOp, so the built-in cascade falls through to the root operator.
		if (rootRelOp) {
			if (node.metrics.estRows === undefined) node.metrics.estRows = rootRelOp.metrics.estRows;
			if (rootRelOp.metrics.actRows !== undefined) node.metrics.actRows = rootRelOp.metrics.actRows;
		}

		// Statement-level runtime totals. Not per-thread like the operators' - one set per query.
		var qts = queryPlanEl ? firstChildByName(queryPlanEl, 'QueryTimeStats') : undefined;
		if (qts) {
			node.metrics.elapsedMs    = num(qts, 'ElapsedTime');
			node.metrics.cpuMs        = num(qts, 'CpuTime');
			node.metrics.udfElapsedMs = num(qts, 'UdfElapsedTime');
			node.metrics.udfCpuMs     = num(qts, 'UdfCpuTime');
		}
		// <QueryPlan>'s own attributes - html-query-plan's statement tooltip shows Cached plan size,
		// Degree of Parallelism and Memory Grant from exactly this element (qp.xslt's ToolTipGrid:
		// s:QueryPlan/@CachedPlanSize etc.), but they were never copied out of the XML here, so they
		// were only reachable by expanding "All plan XML for this operator".
		if (queryPlanEl) {
			node.props.degreeOfParallelism    = attr(queryPlanEl, 'DegreeOfParallelism');
			node.props.nonParallelPlanReason  = attr(queryPlanEl, 'NonParallelPlanReason');
			node.metrics.cachedPlanSizeKb     = num(queryPlanEl, 'CachedPlanSize');
			node.metrics.memoryGrantKb        = num(queryPlanEl, 'MemoryGrant');
			node.metrics.compileTimeMs        = num(queryPlanEl, 'CompileTime');
			node.metrics.compileCpuMs         = num(queryPlanEl, 'CompileCPU');
			node.metrics.compileMemoryKb      = num(queryPlanEl, 'CompileMemory');
		}
		// <ThreadStat> - the query's REAL total thread usage, as opposed to DegreeOfParallelism which
		// is only the per-branch worker count. A plan can have several independent parallel branches
		// (e.g. a Merge Join whose two sorted/scanned inputs each run their own DOP-sized worker pool
		// concurrently) - Branches counts how many such pools ran, UsedThreads is their sum
		// (Branches * DOP for the common case of every branch running at the same DOP). Without this,
		// seeing "∥4" on several Parallelism operators in the same plan reads as ambiguous - does the
		// query use 4 threads total, or 4 per operator? This is the authoritative answer, straight from
		// the engine rather than inferred from the diagram.
		var threadStatEl = queryPlanEl ? firstChildByName(queryPlanEl, 'ThreadStat') : undefined;
		if (threadStatEl) {
			node.metrics.threadBranches = num(threadStatEl, 'Branches');
			node.metrics.threadsUsed    = num(threadStatEl, 'UsedThreads');
			var threadResEl = firstChildByName(threadStatEl, 'ThreadReservation');
			if (threadResEl) node.metrics.threadsReserved = num(threadResEl, 'ReservedThreads');
		}
		// <WaitStats> (SQL Server 2016 SP1+ actual plans) - the query's top wait types accumulated
		// during execution, a sibling of <QueryTimeStats> under <QueryPlan>. html-query-plan's fork
		// shows this as a "Top 10 Waits" table on the statement tooltip; this renderer had no
		// equivalent, so it was only visible via "All plan XML for this operator".
		var waitStatsEl = queryPlanEl ? firstChildByName(queryPlanEl, 'WaitStats') : undefined;
		if (waitStatsEl) {
			var waits = childrenByName(waitStatsEl, 'Wait').map(function (el) {
				return { waitType: attr(el, 'WaitType'), waitTimeMs: num(el, 'WaitTimeMs'), waitCount: num(el, 'WaitCount') };
			});
			waits.sort(function (a, b) { return (b.waitTimeMs || 0) - (a.waitTimeMs || 0); });
			if (waits.length) node.metrics.waitStats = waits;
		}
		// <ParameterList> - compiled vs runtime value for every parameter/local variable, a child of
		// <QueryPlan> (present on ~1/3 of the reference corpus). dbxShowplanAnalyzer.js already reads
		// this same element to raise findings when a value differs (parameter sniffing) or is missing
		// (an un-sniffable local variable) - this is the reference view for everything it saw, not just
		// the ones worth a finding, so a parameter that matches is still visible here.
		var paramListEl = queryPlanEl ? firstChildByName(queryPlanEl, 'ParameterList') : undefined;
		if (paramListEl) {
			var parameters = childrenByName(paramListEl, 'ColumnReference').map(function (el) {
				return {
					column:        attr(el, 'Column'),
					dataType:      attr(el, 'ParameterDataType'),
					compiledValue: attr(el, 'ParameterCompiledValue'),
					runtimeValue:  attr(el, 'ParameterRuntimeValue')
				};
			});
			if (parameters.length) node.props.parameters = parameters;
		}
		// A cursor statement's own type lives on <CursorPlan>, and drives its icon (see iconKeyFor).
		var cursorPlan = stmtEl ? firstChildByName(stmtEl, 'CursorPlan') : undefined;
		if (cursorPlan) {
			node.props.CursorActualType = attr(cursorPlan, 'CursorActualType');
			node.props.CursorName       = attr(cursorPlan, 'CursorName');
			if (attr(cursorPlan, 'CursorActualType')) node.label = type + ' (cursor)';
		}
		return node;
	}

	/** Nearest ancestor element whose localName starts with "Stmt" - the statement owning a plan. */
	function owningStatement(el) {
		var p = el.parentNode;
		while (p && p.nodeType === 1) {
			if (/^Stmt/.test(lname(p))) return p;
			p = p.parentNode;
		}
		return undefined;
	}

	function statementLabel(stmtEl, index) {
		if (!stmtEl) return 'Query ' + (index + 1);
		var text = attr(stmtEl, 'StatementText');
		if (!text) return lname(stmtEl) + ' ' + (index + 1);
		text = text.replace(/\s+/g, ' ').trim();
		return text.length > 160 ? text.slice(0, 160) + '…' : text;
	}

	function parseXml(xmlString) {
		lastParseError = null;
		if (!xmlString || !xmlString.trim()) { lastParseError = 'Plan text is empty'; return null; }

		// Whitespace before "<?xml ...?>" is a fatal XML error (the declaration must be the very first
		// thing in the document) but is harmless and common in captured/pasted plans - strip it rather
		// than fail on it. A declaration claiming encoding="utf-16" over ASCII bytes needs no handling:
		// DOMParser ignores the declared encoding when the input is already a decoded string.
		xmlString = xmlString.trim();

		var xmlDoc;
		try {
			xmlDoc = $.parseXML(xmlString);
		} catch (ex) {
			lastParseError = 'XML parse error: ' + (ex && ex.message ? ex.message : ex);
			return null;
		}
		if (!xmlDoc || !xmlDoc.documentElement) { lastParseError = 'XML parse produced no document'; return null; }

		var perr = xmlDoc.getElementsByTagName('parsererror');
		if (perr && perr.length) {
			lastParseError = 'XML parse error: ' + (perr[0].textContent || '').replace(/\s+/g, ' ').slice(0, 200);
			return null;
		}

		if (!/ShowPlanXML/i.test(lname(xmlDoc.documentElement))) {
			lastParseError = 'Not a SQL Server execution plan (root element is <'
				+ xmlDoc.documentElement.nodeName + '>, expected <ShowPlanXML>)';
			return null;
		}

		// One statement entry per <QueryPlan> - see the structural note in the file header for why
		// this single rule covers StmtSimple / StmtCursor / StmtCond-nested statements alike.
		var queryPlans = descendantsByName(xmlDoc.documentElement, 'QueryPlan');
		var statements = [];

		queryPlans.forEach(function (qp, i) {
			var rootRelOp = firstChildByName(qp, 'RelOp');
			if (!rootRelOp) return;

			var root = buildNode(rootRelOp);
			computeCosts(root);

			var stmtEl = owningStatement(qp);
			var meta = {};
			if (stmtEl) {
				var sa = attrsOf(stmtEl);
				for (var k in sa) { if (sa.hasOwnProperty(k)) meta[k] = sa[k]; }
			}
			// QueryPlan's own attributes (DegreeOfParallelism, MemoryGrant, CachedPlanSize,
			// CompileTime, CompileCPU, CompileMemory, ...) belong to the plan, not the statement, but
			// are shown together in the header - prefixed so neither can shadow the other.
			var qa = attrsOf(qp);
			for (var k2 in qa) { if (qa.hasOwnProperty(k2)) meta['QueryPlan.' + k2] = qa[k2]; }

			// The operator tree hangs off the statement node, so the diagram's root is the statement -
			// matching html-query-plan and SSMS, where the left-most node is the SELECT/INSERT/... the
			// rows ultimately flow into. computeCosts() ran on the RelOp root above, so every
			// operator's Cost % stays relative to the operator tree, not to this wrapper.
			var stmtNode = buildStatementNode(stmtEl, qp, root);
			// Needs the statement's Degree of Parallelism (just parsed above), so this runs after
			// buildStatementNode rather than alongside computeCosts().
			applyParallelWorkerCounts(stmtNode);
			markNeverExecuted(stmtNode);

			statements.push({
				label: statementLabel(stmtEl, i),
				meta:  meta,
				steps: [ { label: '', root: stmtNode } ],
				_stmtEl: stmtEl,
				_queryPlanEl: qp
			});
		});

		if (!statements.length) {
			// Real and legitimate: StmtUseDb / StmtCond-only / control-flow-only batches carry
			// statements but no <QueryPlan> at all (3 of the 55 corpus plans are exactly this).
			// Reported as a parse error so the dialog falls back to the raw XML view with an
			// explanation, rather than rendering an empty diagram.
			var stmtCount = descendantsByName(xmlDoc.documentElement, 'Statements').length;
			lastParseError = stmtCount
				? 'This plan contains statements but no execution plan (no <QueryPlan> element) - '
				  + 'typically a USE / DECLARE / control-flow-only batch.'
				: 'No <QueryPlan> element found in the plan XML';
			return null;
		}

		return { format: 'xml', rawText: xmlString, statements: statements };
	}

	var STYLE_INJECTED = false;
	// Connector lines use the well-known pure-CSS "org chart" <ul><li> technique: each <li> draws the
	// left/right halves of the horizontal bar linking it to its siblings (suppressed for the first/
	// last child and for an only child), and each nested <ul> draws the vertical stem dropping from
	// its parent box down to that horizontal bar. This is far more robust than trying to compute
	// per-node connector positions from a flexbox row directly.
	var CSS = ''
		+ '.ss-plan-wrap { font-family: -apple-system, Segoe UI, Roboto, sans-serif; font-size: 0.8em; padding: 8px 0; }'
		+ '.ss-plan-stmt-hdr { font-weight: 600; font-size: 0.9em; color: #444; margin: 6px 0 2px 0; }'
		+ '.ss-plan-step-hdr { font-size: 0.8em; color: #888; margin-bottom: 6px; }'
		// width:max-content keeps this box sized to its own content rather than stretching to fill
		// the dialog's ancestor .scroll-tree (fixed width:3000px, shared by every section of the SQL
		// Server Showplan modal, not graph-specific) - otherwise the flex-centered tree ends up positioned deep
		// inside that oversized canvas and the browser's default scroll position shows mostly blank
		// space, clipping real boxes off both edges instead of starting at the tree's own left edge.
		+ '.ss-plan-tree { width: max-content; padding: 8px 4px 16px 4px; }'
		+ '.ss-plan-tree ul, .ss-plan-tree ul ul { display: flex; justify-content: center; padding-top: 20px; position: relative; }'
		+ '.ss-plan-tree ul { list-style: none; margin: 0; padding-left: 0; }'
		+ '.ss-plan-tree li { list-style: none; position: relative; padding: 20px 8px 0 8px; display: flex; flex-direction: column; align-items: center; }'
		+ '.ss-plan-tree li::before, .ss-plan-tree li::after { content: ""; position: absolute; top: 0; right: 50%; border-top: 1px solid #b0b0b0; width: 50%; height: 20px; }'
		+ '.ss-plan-tree li::after { right: auto; left: 50%; border-left: 1px solid #b0b0b0; }'
		+ '.ss-plan-tree li:only-child { padding-top: 0; }'
		+ '.ss-plan-tree li:only-child::before, .ss-plan-tree li:only-child::after { display: none; }'
		+ '.ss-plan-tree li:first-child::before, .ss-plan-tree li:last-child::after { border: 0 none; }'
		+ '.ss-plan-tree li:last-child::before { border-right: 1px solid #b0b0b0; border-radius: 0 5px 0 0; }'
		+ '.ss-plan-tree li:first-child::after { border-radius: 5px 0 0 0; }'
		+ '.ss-plan-tree ul ul::before { content: ""; position: absolute; top: 0; left: 50%; border-left: 1px solid #b0b0b0; width: 0; height: 20px; }'
		+ '.ss-plan-tree > ul > li { padding-top: 0; }'
		+ '.ss-plan-tree > ul > li::before, .ss-plan-tree > ul > li::after { display: none; }'
		// Left-to-right variant: same technique, axes swapped (top<->left, bottom<->right,
		// width<->height, border-top<->border-left) so "down the tree" becomes "right along the
		// tree". The extra `.ss-plan-horizontal` class always wins on specificity over the plain
		// rules above, so toggling it is enough to flip orientation without touching the DOM.
		+ '.ss-plan-tree.ss-plan-horizontal ul, .ss-plan-tree.ss-plan-horizontal ul ul { flex-direction: column; justify-content: center; padding-top: 0; padding-left: 24px; }'
		+ '.ss-plan-tree.ss-plan-horizontal li { padding: 8px 0 8px 24px; flex-direction: row; align-items: center; }'
		+ '.ss-plan-tree.ss-plan-horizontal li::before, .ss-plan-tree.ss-plan-horizontal li::after { top: auto; left: 0; right: auto; bottom: 50%; border-top: 0 none; border-left: 1px solid #b0b0b0; width: 24px; height: 50%; }'
		+ '.ss-plan-tree.ss-plan-horizontal li::after { bottom: auto; top: 50%; border-top: 1px solid #b0b0b0; }'
		+ '.ss-plan-tree.ss-plan-horizontal li:only-child { padding-left: 0; }'
		// These two clear the "no sibling on this side" edge exactly like the plain-mode
		// `li:first-child::before, li:last-child::after { border: 0 none; }` rule does - but that
		// rule alone isn't enough here: the generic `li::before, li::after` rule two lines up
		// unconditionally sets border-left, and (2 classes + tag + pseudo-element) beats the plain
		// rule's (1 class + tag + pseudo-class + pseudo-element) on specificity, so without an
		// explicit horizontal-scoped override the edge children kept a stray extra border segment -
		// exactly the "hook" artifact reported against the real dialog.
		+ '.ss-plan-tree.ss-plan-horizontal li:first-child::before { border: 0 none; }'
		+ '.ss-plan-tree.ss-plan-horizontal li:last-child::after { border: 0 none; }'
		+ '.ss-plan-tree.ss-plan-horizontal li:last-child::before { border-right: 0 none; border-bottom: 1px solid #b0b0b0; border-radius: 0; }'
		+ '.ss-plan-tree.ss-plan-horizontal li:first-child::after { border-radius: 0; }'
		+ '.ss-plan-tree.ss-plan-horizontal ul ul::before { top: 50%; left: 0; border-left: 0 none; border-top: 1px solid #b0b0b0; width: 24px; height: 0; }'
		+ '.ss-plan-tree.ss-plan-horizontal > ul > li { padding-left: 0; }'
		// Experimental alternate connector style modeled on html-query-plan (the SQL Server plan
		// viewer already vendored in this app, src/com/dbxtune/sql/showplan/sqlserver/dist/qp.js):
		// instead of CSS pseudo-element borders, positions are measured after layout via
		// getBoundingClientRect() and connectors are drawn as an SVG overlay (see
		// drawConnectorLines() below). Kept fully separate from the default CSS-connector mode
		// above (toggled via the .ss-plan-lines class) specifically so it's trivial to roll back -
		// this block and drawConnectorLines() are the only things to remove.
		+ '.ss-plan-tree.ss-plan-lines li::before, .ss-plan-tree.ss-plan-lines li::after, .ss-plan-tree.ss-plan-lines ul ul::before { display: none !important; }'
		+ '.ss-plan-tree.ss-plan-lines { position: relative; }'
		+ '.ss-plan-connector-svg { position: absolute; top: 0; left: 0; overflow: visible; pointer-events: none; }'
		// The overlay stays click-through so it never steals clicks from the boxes beneath it; only
		// the invisible hit paths opt back in, so hovering an arrow shows its row-count tooltip while
		// clicking straight through it still selects whatever box is underneath.
		+ '.ss-plan-connector-hit { pointer-events: stroke; cursor: help; }'
		// The <ul>/<li> that build the tree are pure structure and must never intercept the pointer.
		// tuckLeavesNearParent() sets position:relative on a branching node's <ul> so it can absolutely
		// position tucked leaves inside it - which makes that <ul> a POSITIONED box, painting above the
		// (also positioned) connector SVG at treeEl.firstChild. Its rect spans the whole children
		// region, so it swallowed the hover for every arrow downstream of that node: measured on a
		// 3-join plan, 7 of 9 connectors crossed such a <ul>, and the only two that did not were the
		// two upstream of the first branch. Making the containers transparent restores the arrows
		// without touching paint order; .ss-plan-box opts back in so a box still wins over its own area.
		+ '.ss-plan-tree ul, .ss-plan-tree li { pointer-events: none; }'
		+ '.ss-plan-box { pointer-events: auto; }'
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
		// .ss-plan-box-cell is an invisible wrapper (see renderNode()) around the actual visible
		// .ss-plan-box card - display:contents makes it a no-op everywhere except compact mode,
		// where it becomes the table-cell/table-caption instead of .ss-plan-box itself. That
		// indirection matters: a table-cell's border/background stretches to fill the whole
		// (possibly much taller) row, so putting table-cell directly on the bordered box would
		// stretch its visible border down the full row instead of hugging just its own content.
		+ '.ss-plan-box-cell { display: contents; }'
		// Horizontal (left-to-right): each <li> is a 2-cell table - its own box cell, then a second
		// cell holding its <ul> of children, which stack vertically (normal block flow) inside that
		// cell.
		+ '.ss-plan-tree.ss-plan-compact-h ul { display: block; list-style: none; margin: 0; padding: 0; position: static; }'
		+ '.ss-plan-tree.ss-plan-compact-h li { display: table; list-style: none; position: static; padding: 0; margin: 0; }'
		// vertical-align: top (not middle) on the box's own cell is deliberate: "middle" would center
		// a join over the FULL height of everything stacked below it (leaf + however deep the
		// continuing chain still goes), which re-introduces growing distance-from-leaf the deeper the
		// chain gets - just a gentler version of the same "V" problem this mode exists to fix. "top"
		// anchors the join right next to whichever child is listed first (the leaf, in practice -
		// see reorderCompactByNodeId()), independent of how much taller the OTHER child's subtree is.
		+ '.ss-plan-tree.ss-plan-compact-h li > .ss-plan-box-cell { display: table-cell; vertical-align: top; }'
		+ '.ss-plan-tree.ss-plan-compact-h li > ul { display: table-cell; vertical-align: top; padding-left: 56px; }'
		+ '.ss-plan-tree.ss-plan-compact-h li > ul > li { display: table; margin: 9px 0; }'
		+ '.ss-plan-tree.ss-plan-compact-h li > ul > li:first-child { margin-top: 0; }'
		+ '.ss-plan-tree.ss-plan-compact-h li > ul > li:last-child { margin-bottom: 0; }'
		// Vertical (top-to-bottom): transposed - the box cell becomes a table-caption (always renders
		// above its table regardless of source order, so no DOM change needed vs. horizontal), and
		// the children <ul> becomes a table-row whose <li> children are table-cells sitting side by
		// side.
		+ '.ss-plan-tree.ss-plan-compact-v ul { display: block; list-style: none; margin: 0; padding: 0; position: static; }'
		+ '.ss-plan-tree.ss-plan-compact-v li { display: table; list-style: none; position: static; padding: 0; margin: 0; }'
		+ '.ss-plan-tree.ss-plan-compact-v li > .ss-plan-box-cell { display: table-caption; caption-side: top; text-align: center; margin-bottom: 6px; }'
		// .ss-plan-box is a block div elsewhere (fine as a flex item in the other modes) - a
		// table-caption's default block content would stretch to the caption's full width (the
		// table's width, i.e. the widest row below it) instead of shrinking to its own content, so
		// it needs inline-block here specifically for text-align:center above to actually center it.
		+ '.ss-plan-tree.ss-plan-compact-v .ss-plan-box { display: inline-block; }'
		+ '.ss-plan-tree.ss-plan-compact-v li > ul { display: table-row; }'
		+ '.ss-plan-tree.ss-plan-compact-v li > ul > li { display: table-cell; vertical-align: top; padding: 40px 6px 0 6px; }'
		+ '.ss-plan-tree.ss-plan-compact-v li > ul > li:first-child { padding-left: 0; }'
		+ '.ss-plan-tree.ss-plan-compact-v li > ul > li:last-child { padding-right: 0; }'
		// compact-h only: a solo (non-branching) link's own <li> already gets padding:0 from the
		// general ".ss-plan-tree.ss-plan-compact-h li" rule above, so this is a no-op there kept for
		// symmetry/documentation. Do NOT extend it to compact-v: a solo link's <li> is simultaneously
		// :first-child and :last-child, so the two rules just above already zero its LEFT/RIGHT padding
		// (6px each) without touching this selector at all - a compact-v-side blanket "padding: 0"
		// here was wiping the 40px TOP padding too, which is the only thing providing vertical gap for
		// the connector arrow between two boxes in a non-branching chain. Reported live: Top-to-Bottom
		// orientation had almost no visible gap/arrow room between consecutive boxes wherever the
		// chain does not branch - i.e. everywhere except right at a join/spool.
		+ '.ss-plan-tree.ss-plan-compact-h li:only-child { padding: 0; }'
		+ '.ss-plan-box { position: relative; border: 1px solid #999; border-radius: 5px; background: #fff; padding: 5px 9px; cursor: pointer; min-width: 120px; max-width: 220px; text-align: center; box-shadow: 0 1px 2px rgba(0,0,0,0.08); }'
		+ '.ss-plan-box:hover { border-color: #4a90d9; }'
		+ '.ss-plan-box.ss-plan-warn { border-color: #d9a24a; background: #fff8ec; }'
		+ '.ss-plan-box.ss-plan-big-table { border-color: #c0392b; border-width: 2px; background: #fdf1f0; }'
		+ '.ss-plan-box.ss-plan-eager-spool { border-color: #c0392b; border-width: 2px; background: #fdf1f0; }'
		+ '.ss-plan-metric.ss-plan-tablesize-warn { color: #c0392b; font-weight: 600; }'
		+ '.ss-plan-box.ss-plan-reformat { border-color: #c0392b; border-width: 2px; background: #fdf1f0; }'
		+ '.ss-plan-box.ss-plan-reformat-info { border-color: #4a90d9; border-width: 2px; background: #eef5fc; }'
		+ '.ss-plan-metric.ss-plan-reformat-info-metric { color: #2a6ebb; font-weight: 600; }'
		+ '.ss-plan-nodeid { position: absolute; top: 2px; right: 4px; font-size: 0.72em; color: #aaa; line-height: 1; }'
		+ '.ss-plan-icon { width: 32px; height: 32px; margin: 0 auto; background-repeat: no-repeat; }'
		// position:relative so the DDL-info icon (below) can be pulled out of the centered flex flow
		// and pinned to the row's own right edge without disturbing how the remaining icons center.
		+ '.ss-plan-icon-row { position: relative; display: flex; align-items: center; justify-content: center; gap: 2px; }'
		+ '.ss-plan-icon-row .ss-plan-icon { margin: 0; }'
		+ '.ss-plan-jointype-icon { width: 32px; height: 32px; background-repeat: no-repeat; background-size: 32px 32px; flex: none; }'
		// Generic small-icon-in-the-row badge (batch mode, DDL-info found/missing) - 16x16, distinct
		// from the 32x32 main operator icon and join-type icon.
		+ '.ss-plan-icon-badge { width: 16px; height: 16px; background-repeat: no-repeat; flex: none; }'
		// Anchored to the row's right edge (not a flex sibling) so it reads as a corner indicator near
		// the box border, while the main/join/batch icons stay centered as their own group.
		+ '.ss-plan-ddlinfo-icon { position: absolute; right: 2px; top: 50%; transform: translateY(-50%); background-image: url(/images/ddlinfo.png); background-size: 16px 16px; }'
		// "Missing" reuses the same base icon and draws a small red X badge over its bottom-right
		// corner in pure CSS, rather than a second hand-composited image.
		+ '.ss-plan-ddlinfo-icon.ss-plan-ddlinfo-missing::after { content: \'\'; position: absolute; right: -3px; bottom: -3px; width: 9px; height: 9px; border-radius: 50%; background: #c0392b; box-shadow: 0 0 0 1.5px #fff; }'
		+ '.ss-plan-ddlinfo-icon.ss-plan-ddlinfo-missing::before { content: \'\\2715\'; position: absolute; right: -3px; bottom: -4px; width: 9px; height: 9px; font-size: 7px; line-height: 9px; color: #fff; text-align: center; z-index: 1; }'
		+ '.ss-plan-label { font-weight: 600; white-space: nowrap; }'
		+ '.ss-plan-logicalop { font-size: 0.85em; color: #6a6a6a; font-style: italic; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 200px; margin: 0 auto; }'
		+ '.ss-plan-subtitle { font-size: 0.85em; color: #555; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 200px; margin: 0 auto; }'
		+ '.ss-plan-metric { font-size: 0.85em; color: #666; white-space: nowrap; }'
		+ '.ss-plan-metric.ss-plan-warn-text { color: #a3690a; font-weight: 600; }'
		+ '.ss-plan-metric-pct-warn { color: #c0392b; font-weight: 700; }'
		+ '.ss-plan-detail-pct-warn { color: #c0392b; font-weight: 700; }'
		+ '.ss-plan-metric-filter { color: #2a6f97; font-size: 0.85em; white-space: nowrap; }'
		// position:fixed and attached to <body> (see openDetailPanel) so the panel is never clipped by
		// the diagram's own overflow:auto viewport - JS supplies left/top. z-index clears Bootstrap's
		// modal (1050) and its backdrop, since this renders inside the Showplan dialog.
		+ '.ss-plan-detail { position: fixed; z-index: 2000; background: #fffef5; border: 1px solid #c9b98a; border-radius: 4px; padding: 6px 10px; min-width: 220px; max-width: 560px; text-align: left; box-shadow: 0 2px 10px rgba(0,0,0,0.28); font-size: 11px; line-height: 1.35; }'
		// A panel for an operator with a long predicate can be taller than the screen; cap it and let
		// it scroll rather than letting it run off the bottom.
		+ '.ss-plan-detail { max-height: 80vh; overflow-y: auto; overscroll-behavior: contain; }'
		+ '.ss-plan-detail table { border-collapse: collapse; }'
		+ '.ss-plan-detail-desc { white-space: normal; font-style: italic; color: #6b5f3d; margin-bottom: 6px; padding-bottom: 6px; border-bottom: 1px solid #e6dcb8; line-height: 1.35; }'
		+ '.ss-plan-detail-grid { display: flex; align-items: flex-start; gap: 0 14px; }'
		+ '.ss-plan-detail-right { border-left: 1px solid #e6dcb8; padding-left: 14px; }'
		+ '.ss-plan-detail-idx-hdr { font-size: 10px; color: #6b5f3d; margin-top: 4px; }'
		// Full-width wrapping text, for values too long to sit safely in a ".ss-plan-detail td"
		// (nowrap) cell without forcing the two-column grid wider than the panel - see the Predicate/
		// Output List block in buildDetailPanel().
		+ '.ss-plan-detail-wrap { white-space: normal; word-break: break-word; }'
		+ '.ss-plan-idx-tbl td { white-space: normal; }'
		+ '.ss-plan-detail td { padding: 1px 6px 1px 0; vertical-align: top; white-space: nowrap; }'
		+ '.ss-plan-detail td.ss-plan-detail-key { color: #777; }'
		+ '.ss-plan-detail .ss-plan-raw-line { font-family: monospace; white-space: pre-wrap; color: #555; }'
		+ '.ss-plan-detail.ss-plan-tooltip { pointer-events: none; cursor: default; }'
		+ '.ss-plan-fallback { color: #888; font-size: 0.85em; font-style: italic; padding: 6px 0; }'
		// Triggered by window.ssShowplanJumpToNode() (dbxShowplan.js) when a Plan Analysis finding's
		// "[Node N]" tag is clicked - draws attention to the box scrollIntoView() just centered on
		// without permanently changing its styling (the class is removed again once the animation ends).
		+ '@keyframes ss-plan-flash { 0%, 100% { box-shadow: 0 1px 2px rgba(0,0,0,0.08); } 20%, 60% { box-shadow: 0 0 0 5px rgba(74,144,217,0.85); } 40%, 80% { box-shadow: 0 1px 2px rgba(0,0,0,0.08); } }'
		// 5 discrete pulses (not a time-based cutoff) - ssShowplanJumpToNode (dbxShowplan.js) listens
		// for this animation's 'animationend' event (which only fires once, after the last iteration)
		// to remove the class, so the two stay in sync automatically if this iteration count or
		// per-pulse duration ever changes.
		+ '.ss-plan-box.ss-plan-flash { animation: ss-plan-flash 0.8s ease-in-out 5; }'
		// --- SQL Server specific additions on top of the ported ASE styling ---
		// Node ID badge (the ASE renderer's VA badge, renamed - SQL Server numbers operators by @NodeId).
		+ '.ss-plan-nodeid { position: absolute; top: 2px; right: 4px; font-size: 0.72em; color: #aaa; line-height: 1; }'
		// The clicked operator stays visibly marked while the Properties pane shows it - without this
		// there is no way to tell which box the pane is describing once the pointer has moved away.
		+ '.ss-plan-box.ss-plan-selected { border-color: #2a6ebb; border-width: 2px; box-shadow: 0 0 0 3px rgba(42,110,187,0.18); }'
		+ '.ss-plan-box.ss-plan-has-warning { border-color: #d9a24a; }'
		// Top-left corner badge, overlapping the border like .ss-plan-parallel-chip does at
		// bottom-right - pulled out of the icon row so a warning reads as an alert on the box, not
		// just another small glyph among the row's neutral operator-property icons.
		// flex centering rather than line-height/text-align - the ⚠ glyph's uneven left/right bearing
		// otherwise reads visually off-center inside the small circle.
		+ '.ss-plan-corner-warn { position: absolute; top: -8px; left: -8px; width: 17px; height: 17px; border-radius: 50%; background: #d9822b; border: 1.5px solid #fff; color: #fff; font-size: 12px; font-weight: 700; line-height: 1; display: flex; align-items: center; justify-content: center; box-shadow: 0 1px 3px rgba(0,0,0,0.3); }'
		// EXPERIMENTAL - see the m.parWorkers block in renderNode() for the reasoning. Went through a
		// full outline() (too heavy), then corner accents at one and then two corners (still read as
		// an abstract mark, not an obvious meaning), landed on the "duplicate/copies" icon metaphor:
		// N threads run N copies of this operator, so the box itself looks like a stack of sheets, the
		// same visual language as any OS "copy" icon. Two extra sheets peek out bottom-right, a fixed
		// count regardless of the real thread number (this is a symbol, not a literal 1:1 stack - a
		// DOP-64 plan cannot draw 64 sheets) - the exact count is still on the chip.
		//
		// The sheets themselves are NOT children of the box they belong to - see drawParallelStacks()
		// (called from the same late layout pass as drawConnectorLines()). A per-box child with
		// negative z-index worked in a simple chain but not in the compact/branching table layout,
		// where an adjacent operator's own opaque box can sit close enough to cover the few px meant
		// to peek out - z-index only orders content within ITS OWN box's stacking context, it has no
		// say over how that box's whole subtree compares to an unrelated sibling box next to it.
		// Positioned as one flat overlay behind the entire tree instead, so a sheet is only ever
		// hidden by a box that is genuinely, visually on top of it - never by paint-order accident.
		// Sheet border is #999 - the same grey as .ss-plan-box's own border, so the sheet reads as a
		// literal second copy of the box, not a differently-coloured annotation on top of it. The
		// chip stays purple - it is the one part meant to stand out (the actual count), same as
		// before.
		+ '.ss-plan-parallel-overlay { position: absolute; top: 0; left: 0; pointer-events: none; }'
		+ '.ss-plan-parallel-sheet { position: absolute; border: 1px solid #999; border-radius: 5px; background: #fff; }'
		// Sits past the outermost sheet's own corner (the sheet drawn at +6px), not the main box's -
		// otherwise it reads as part of the stack rather than a count attached to the whole group.
		+ '.ss-plan-parallel-chip { position: absolute; right: -9px; bottom: -9px; min-width: 15px; height: 15px; padding: 0 2px; border-radius: 8px; background: #707070; color: #fff; font-size: 10px; font-weight: 600; line-height: 15px; text-align: center; box-shadow: 0 1px 2px rgba(0,0,0,0.3); }'
		+ '.ss-plan-cost { font-weight: 600; color: #444; }'
		+ '.ss-plan-cost.ss-plan-cost-high { color: #c0392b; }'
		+ '.ss-plan-time { color: #2a6ebb; }'
		// The statement node heads the plan - given a slightly heavier frame so the eye lands on the
		// "SELECT" first and reads the flow back from there, the way it does in SSMS.
		+ '.ss-plan-box.ss-plan-statement { border-color: #7a7a7a; border-width: 2px; background: #fbfbfb; }'
		+ '.ss-plan-stmt-cost { font-weight: normal; color: #888; font-size: 0.9em; }'
		+ '.ss-plan-detail-warnings { margin-bottom: 5px; }'
		+ '.ss-plan-detail-warn-line { color: #a8500f; white-space: normal; max-width: 520px; line-height: 1.3; }'
		+ '.ss-plan-detail-hint { margin-top: 5px; padding-top: 4px; border-top: 1px solid #e6dcb8; color: #9a8f6d; font-size: 10px; white-space: normal; }'
		// --- Properties pane (the SSMS-like "everything" view, rendered into opts.propsTarget) ---
		// The generic half lives in dbxShowplanGraph.js so the ASE pane looks identical and a styling
		// fix lands once; the per-thread bars below stay here, since only SQL Server has that data.
		+ DbxShowplanGraph.neverExecutedCss('ss-plan')
		+ DbxShowplanGraph.propsCss('ss-plan')
		// Per-thread row distribution (see the 'Parallel Threads' section) - a small inline bar chart
		// next to each thread's row count, since "how is it distributed" is far easier to read as bar
		// LENGTHS than as a column of numbers alone.
		+ '.ss-plan-prop-thread-row { display: flex; align-items: center; gap: 6px; font-size: 11px; padding: 1px 0; }'
		+ '.ss-plan-prop-thread-label { flex: 0 0 52px; color: #777; }'
		+ '.ss-plan-prop-thread-track { flex: 1 1 auto; height: 9px; background: #eee; border-radius: 2px; overflow: hidden; }'
		// display:block is required, not decorative: this is a <span>, which defaults to
		// display:inline, and an inline element ignores explicit width/height entirely per the CSS
		// spec - the fill's inline width:N% style was being SET correctly but had zero visual
		// effect, rendering as a 0x0 box regardless of percentage (confirmed via computed style:
		// width 0, height 0, background-color correctly blue/red, just never painted anywhere).
		// The track span next to it looked fine only because it happens to be a DIRECT child of a
		// flex container, which CSS auto-blockifies - this fill span, nested one level deeper
		// inside the track, gets no such free pass.
		+ '.ss-plan-prop-thread-fill { display: block; height: 100%; background: #4a90d9; }'
		// Same busiest-thread-is-suspicious highlight the Skewed Parallelism Plan Analysis finding
		// (dbxShowplanAnalyzer.js) uses its threshold for - see isThreadSkewed() below, which mirrors
		// that finding's exact math so this highlight lines up with when that finding actually fires.
		+ '.ss-plan-prop-thread-fill.ss-plan-prop-thread-skewed { background: #c0392b; }'
		+ '.ss-plan-prop-thread-value { flex: 0 0 auto; min-width: 70px; text-align: right; color: #444; }'
		+ '.ss-plan-prop-thread-note { font-size: 10px; color: #999; margin-top: 3px; font-style: italic; }';

	function injectStyle() {
		if (STYLE_INJECTED) return;
		var styleEl = document.createElement('style');
		styleEl.type = 'text/css';
		styleEl.appendChild(document.createTextNode(CSS));
		document.head.appendChild(styleEl);
		STYLE_INJECTED = true;
	}
	// ─────────────────────────────────────────────────────────────────────────
	// Detail panel placement
	//
	// The panels are attached to <body> and positioned with position:fixed rather than living
	// inside their own .ss-plan-box. They have to be: the diagram scrolls inside its own
	// overflow:auto viewport (so the graph and the Properties pane can sit side by side), and an
	// absolutely-positioned descendant is clipped to the nearest scrolling ancestor. Anchored in the
	// box, a panel on a box near the edge got cut off at the viewport boundary with no way to see
	// the rest of it. html-query-plan escapes the same trap the same way - its JS tooltips are
	// appended to <body> too.
	//
	// The cost of leaving the box's coordinate space is that a fixed panel no longer travels with
	// the diagram, so scrolling/resizing has to be handled explicitly - which the shared panel
	// system's reposition() handler does (dbxShowplanGraph.js).
	// ─────────────────────────────────────────────────────────────────────────

	// The panel machinery itself is shared with the ASE renderer (dbxShowplanGraph.js) - only the
	// panel's CONTENT is vendor-specific, which is what buildDetailPanel supplies here. Same-named
	// local wrappers keep the existing call sites in renderNode()/render() unchanged.
	var _panels = DbxShowplanGraph.createPanelSystem({
		prefix:     'ss-plan',
		buildPanel: function (node) { return buildDetailPanel(node); }
	});

	function positionDetailPanel($panel, boxEl)  { return _panels.position($panel, boxEl); }
	function panelFor(boxEl)                     { return _panels.panelFor(boxEl); }
	function isPinned(boxEl)                     { return _panels.isPinned(boxEl); }
	function closePanel(boxEl)                   { return _panels.close(boxEl); }
	function closeAllPanels(onlyTooltips)        { return _panels.closeAll(onlyTooltips); }
	function openDetailPanel(node, boxEl, pinned) { return _panels.open(node, boxEl, pinned); }
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
		var previousSvg = treeEl.querySelector(':scope > svg.ss-plan-connector-svg');
		if (previousSvg) previousSvg.remove();

		var svgNS = 'http://www.w3.org/2000/svg';
		var containerRect = treeEl.getBoundingClientRect();
		var svg = document.createElementNS(svgNS, 'svg');
		svg.setAttribute('class', 'ss-plan-connector-svg');
		svg.setAttribute('width', treeEl.scrollWidth);
		svg.setAttribute('height', treeEl.scrollHeight);

		// One arrowhead per line thickness in use. markerUnits="userSpaceOnUse" means a marker does
		// NOT scale with stroke-width, so a single shared head would look like a pin on a 12px line
		// and a blob on a 2px one. Thickness is an integer 2..12 (see rowsToThickness), so this is at
		// most 11 small <marker> elements per tree, created lazily.
		var markerSeq = ++_arrowMarkerSeq;
		var defs = document.createElementNS(svgNS, 'defs');
		svg.appendChild(defs);
		var markersByThickness = {};

		function markerFor(thickness) {
			if (markersByThickness[thickness]) return markersByThickness[thickness];
			var id = 'ss-plan-arrow-' + markerSeq + '-' + thickness;
			// Head grows with the line but far more slowly, so a heavy line still ends in a readable
			// point rather than a huge triangle.
			var w = Math.round(6 + thickness * 0.9);
			var h = Math.round(4 + thickness * 0.9);
			var marker = document.createElementNS(svgNS, 'marker');
			marker.setAttribute('id', id);
			marker.setAttribute('markerWidth',  String(w));
			marker.setAttribute('markerHeight', String(h));
			marker.setAttribute('markerUnits', 'userSpaceOnUse');
			marker.setAttribute('refX', String(w));
			marker.setAttribute('refY', String(h / 2));
			marker.setAttribute('orient', 'auto');
			var arrowHead = document.createElementNS(svgNS, 'path');
			arrowHead.setAttribute('d', 'M0,0 L' + w + ',' + (h / 2) + ' L0,' + h + ' Z');
			arrowHead.setAttribute('fill', '#8a8a8a');
			marker.appendChild(arrowHead);
			defs.appendChild(marker);
			markersByThickness[thickness] = id;
			return id;
		}

		// Collected once (all boxes are already in their final position by the time this runs) so
		// each connector below can check whether its own path would cut through some OTHER box - not
		// the ones it's actually connecting. Happens rarely (a tucked sibling occasionally ends up
		// sitting in the narrow gap another connector's elbow needs to cross to reach a more distant
		// ancestor), but it does happen on real, complex plans - confirmed by measuring, not guessing.
		var allBoxRects = Array.prototype.map.call(treeEl.querySelectorAll('.ss-plan-box'), function (b) {
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
			var box = li.querySelector(':scope > .ss-plan-box-cell > .ss-plan-box');
			var parentBox = parentLi.querySelector(':scope > .ss-plan-box-cell > .ss-plan-box');
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

			// Thicker line = more rows flowing along it, exactly as html-query-plan does. Same formula
			// as its nodeToThickness(): natural log of the row count, floored, clamped to 2..12 px,
			// preferring actual rows over the estimate. A log scale is what makes this readable - row
			// counts across one plan routinely span six orders of magnitude, so anything linear would
			// leave every line but the widest hairline-thin.
			var childNode = box.__ssPlanNode;
			var thickness = rowsToThickness(connectorRows(childNode));

			// A connector into a never-executed operator carried no rows, so it is drawn washed out -
			// otherwise a dead branch keeps a full-strength line into it and still pulls the eye.
			var deadEnd = !!(childNode && childNode._neverExecuted);

			var path = document.createElementNS(svgNS, 'path');
			path.setAttribute('d', d);
			path.setAttribute('fill', 'none');
			path.setAttribute('stroke', deadEnd ? '#cfcfcf' : '#8a8a8a');
			path.setAttribute('stroke-width', String(thickness));
			// Rounded joins stop the elbow corners looking notched once the line gets heavy.
			path.setAttribute('stroke-linejoin', 'round');
			path.setAttribute('stroke-linecap', 'butt');
			path.setAttribute('marker-end', 'url(#' + markerFor(thickness) + ')');
			svg.appendChild(path);

			// Hover target + tooltip. The SVG overlay itself is pointer-events:none so it never
			// swallows clicks meant for the boxes underneath; only these invisible hit paths opt back
			// in. They are drawn fatter than the visible line so a 2px connector is still easy to
			// hit, and they carry the <title> - a native SVG tooltip, no JS or library needed.
			var tip = connectorTooltip(childNode, parentBox.__ssPlanNode);
			if (tip) {
				var hit = document.createElementNS(svgNS, 'path');
				hit.setAttribute('d', d);
				hit.setAttribute('fill', 'none');
				hit.setAttribute('stroke', 'transparent');
				hit.setAttribute('stroke-width', String(Math.max(thickness, 12)));
				hit.setAttribute('class', 'ss-plan-connector-hit');
				var title = document.createElementNS(svgNS, 'title');
				title.textContent = tip;
				hit.appendChild(title);
				svg.appendChild(hit);
			}
		});

		treeEl.insertBefore(svg, treeEl.firstChild);
	}

	/**
	 * The "stack of copies" sheets for every parallel operator (see the m.parWorkers comment in
	 * renderNode()), drawn as one flat overlay behind the whole tree - not as children of each box -
	 * for the same reason drawConnectorLines() draws arrows as one flat overlay in front of it:
	 * z-index only orders content within its own box's local stacking context, so a per-box child
	 * cannot be guaranteed to stay visible against an unrelated sibling box that happens to sit close
	 * by in a compact/branching layout. Idempotent and re-callable, same as drawConnectorLines() -
	 * called from the same layoutTuckAndConnectors() pass, after boxes have their final positions.
	 */
	function drawParallelStacks(treeEl) {
		var previous = treeEl.querySelector(':scope > div.ss-plan-parallel-overlay');
		if (previous) previous.remove();

		var boxes = Array.prototype.filter.call(treeEl.querySelectorAll('.ss-plan-box'), function (b) {
			var n = b.__ssPlanNode;
			return n && n.metrics && n.metrics.parWorkers > 0;
		});
		if (!boxes.length) return;

		var overlay = document.createElement('div');
		overlay.className = 'ss-plan-parallel-overlay';
		var containerRect = treeEl.getBoundingClientRect();

		boxes.forEach(function (box) {
			var r = box.getBoundingClientRect();
			var left = r.left - containerRect.left, top = r.top - containerRect.top;
			// One sheet, matching the box's own size exactly and offset diagonally 6px. Was two
			// (3px/6px) - the middle one made the stack read as too busy, dropped it and kept the
			// outer one so the "second sheet" gap stays clearly visible rather than shrinking to 3px.
			[6].forEach(function (offset) {
				var sheet = document.createElement('div');
				sheet.className = 'ss-plan-parallel-sheet';
				sheet.style.left   = (left + offset) + 'px';
				sheet.style.top    = (top  + offset) + 'px';
				sheet.style.width  = r.width  + 'px';
				sheet.style.height = r.height + 'px';
				overlay.appendChild(sheet);
			});
		});

		treeEl.insertBefore(overlay, treeEl.firstChild);
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
	// The actual fix (see .ss-plan-compact-h/-v in the CSS above) is to stop computing positions
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
	function reorderCompactByNodeId(treeEl) {
		treeEl.querySelectorAll('li').forEach(function (li) {
			var ul = li.querySelector(':scope > ul');
			if (!ul) return;
			var kids = Array.prototype.slice.call(ul.children);
			if (kids.length < 2) return;
			// SQL Server's @NodeId is assigned in a pre-order walk of the plan, so ascending NodeId is
			// exactly the order SSMS lists these operators in - sorting siblings by it makes the diagram's
			// visual order agree with the node numbering shown on every box (the equivalent of the ASE
			// renderer sorting by VA). A child with no NodeId shouldn't happen - every RelOp in the
			// 55-plan corpus has one - but sorts last rather than throwing, as a safe default.
			var nodeId = function (kidLi) {
				var box = kidLi.querySelector(':scope > .ss-plan-box-cell > .ss-plan-box');
				var n = box ? parseInt(box.getAttribute('data-nodeid'), 10) : NaN;
				return isNaN(n) ? Infinity : n;
			};
			kids.sort(function (a, b) { return nodeId(a) - nodeId(b); });
			kids.forEach(function (kidLi) { ul.appendChild(kidLi); });
		});
	}

	function tuckLeavesNearParent(treeEl) { return DbxShowplanGraph.tuckLeavesNearParent(treeEl, 'ss-plan'); }

	function tuckLeavesNearParentVertical(treeEl) { return DbxShowplanGraph.tuckLeavesNearParentVertical(treeEl, 'ss-plan'); }

	// Layout/tree plumbing with no vendor knowledge - shared with the other renderer so a fix
	// lands once. See dbxShowplanGraph.js (loaded before this file) for the implementations and
	// for why drawConnectorLines() is deliberately NOT shared.
	function walkPlanNodes(root, fn) { return DbxShowplanGraph.walkPlanNodes(root, fn); }

	/**
	 * Flags the operators that provably never ran (see DbxShowplanGraph.markNeverExecuted for why the
	 * test is subtree-wide rather than per-node). Runs at PARSE time, not render time, so the diagram
	 * and collectFindings() - which are reached from different call sites - can never disagree about
	 * which parts of the plan are dead.
	 *
	 * ActualExecutions is already summed across threads by RT_SUM_ATTRS, and is left undefined on a
	 * node with no <RunTimeInformation> at all - which is exactly the null the shared helper needs in
	 * order not to mistake an estimated-only plan for a plan where nothing ran.
	 */
	function markNeverExecuted(root) {
		return DbxShowplanGraph.markNeverExecuted(root, function (node) {
			// The statement pseudo-node is a wrapper buildStatementNode() invents; it has no runtime
			// counters of its own, so it is judged purely by its subtree (as null, not zero).
			var m = node.metrics || {};
			return (m.ActualExecutions === undefined) ? null : m.ActualExecutions;
		});
	}
	// ─────────────────────────────────────────────────────────────────────────
	// Icons
	//
	// Sprite sheet and cell offsets come straight from html-query-plan's qp.css
	// (src/com/dbxtune/sql/showplan/sqlserver/css/qp_icons.png, served as /images/qp_icons.png).
	// The icon NAME is derived with the same ordered rules QP's own qp.xslt NodeIcon template uses,
	// so an operator gets an identical glyph in both renderers - which is what makes the toolbar's
	// side-by-side toggle a fair comparison rather than a spot-the-difference puzzle.
	// ─────────────────────────────────────────────────────────────────────────

	var ICON_SPRITE_URL = '/images/qp_icons.png';
	var ICON = {
		Catchall: [-96,-256], ArithmeticExpression: [0,0], Assert: [-32,0], Assign: [-64,0],
		Bitmap: [-256,-192], BookmarkLookup: [-128,0], ClusteredIndexDelete: [-160,0],
		ClusteredIndexInsert: [-192,0], ClusteredIndexScan: [-224,0], ClusteredIndexSeek: [-256,0],
		ClusteredIndexMerge: [0,-256], KeyLookup: [-256,0], ClusteredIndexUpdate: [-288,0],
		Collapse: [0,-32], ComputeScalar: [-32,-32], Concatenation: [-64,-32], ConstantScan: [-96,-32],
		Convert: [-128,-32], CursorCatchall: [-96,0], Declare: [-160,-32], Delete: [-288,-160],
		DistributeStreams: [-224,-32], Dynamic: [-256,-32], EagerSpool: [-192,-160],
		FetchQuery: [-288,-32], Filter: [0,-64], GatherStreams: [-32,-64], HashMatch: [-64,-64],
		HashMatchRoot: [-64,-64], HashMatchTeam: [-64,-64], If: [-96,-64], Insert: [0,-192],
		InsertedScan: [-128,-64], Intrinsic: [-160,-64], IteratorCatchall: [-96,0],
		Keyset: [-192,-64], LanguageElementCatchall: [-96,0], LazySpool: [-192,-160],
		LogRowScan: [-224,-64], MergeInterval: [-256,-64], MergeJoin: [-288,-64], NestedLoops: [0,-96],
		NonclusteredIndexDelete: [-32,-96], NonclusteredIndexInsert: [-64,-96], IndexScan: [-96,-96],
		IndexSeek: [-128,-96], NonclusteredIndexSpool: [-160,-96], NonclusteredIndexUpdate: [-192,-96],
		OnlineIndexInsert: [-224,-96], ParameterTableScan: [-256,-96], PopulateQuery: [-192,-224],
		RdiLookup: [0,-128], RefreshQuery: [-32,-128], RemoteDelete: [-64,-128],
		RemoteInsert: [-96,-128], RemoteQuery: [-128,-128], RemoteScan: [-160,-128],
		RemoteUpdate: [-192,-128], RepartitionStreams: [-224,-128], Result: [-256,-128],
		RowCountSpool: [-288,-128], Segment: [0,-160], Sequence: [-32,-160],
		SequenceProject: [-224,-224], SnapShot: [-256,-224], Sort: [-128,-160], Split: [-160,-160],
		Spool: [-192,-160], Statement: [-256,-128], StreamAggregate: [-224,-160], Switch: [-256,-160],
		TableDelete: [-288,-160], TableInsert: [0,-192], TableScan: [-32,-192], TableSpool: [-64,-192],
		WindowSpool: [-64,-192], TableUpdate: [-96,-192], TableValuedFunction: [-128,-192],
		Top: [-160,-192], UDX: [-192,-192], Update: [-96,-192], While: [-224,-192],
		StmtCursor: [-96,-256], StmtCond: [0,-224], FastForward: [-96,0], WindowAggregate: [-160,-256],
		AdaptiveJoin: [-288,-224], IndexSpool: [-160,-96], IndexInsert: [-64,-96],
		IndexDelete: [-32,-96], IndexUpdate: [-192,-96], ColumnStoreIndexScan: [-128,-224],
		ColumnStoreIndexInsert: [-64,-224], ColumnStoreIndexDelete: [-32,-224],
		ColumnStoreIndexUpdate: [-160,-224], ColumnStoreIndexMerge: [-96,-224],
		DeletedScan: [-32,-256], TableMerge: [-65,-256], BatchHashTableBuild: [-128,-256]
	};

	/**
	 * Mirrors qp.xslt's NodeIcon template, rule for rule and in the same order:
	 *   Parallelism -> use LogicalOp (so Gather/Repartition/Distribute Streams get their own glyph)
	 *   -> CursorPlan/@CursorActualType -> @OperationType -> a Lookup IndexScan -> ColumnStore
	 *   variants -> TableValuedFunction -> PhysicalOp with spaces stripped -> Catchall.
	 */
	function iconKeyFor(node) {
		var p = node.props || {};
		var name;

		// Statement nodes, per qp.xslt's NodeIcon: a cursor statement uses its CursorActualType,
		// otherwise StmtSimple -> Statement, StmtCursor -> StmtCursor, StmtCond -> StmtCond.
		if (node.isStatement) {
			name = p.CursorActualType || p.stmtTag;
			if (name === 'StmtSimple' || name === 'StmtUseDb') name = 'Statement';
			return ICON.hasOwnProperty(name) ? name : 'Statement';
		}

		if (node.op === 'Parallelism' && p.logicalOp) {
			name = p.logicalOp.replace(/ /g, '');
		} else if (p.CursorActualType) {
			name = p.CursorActualType;
		} else if (p.OperationType) {
			name = p.OperationType;
		} else if (p.bodyTag === 'IndexScan' && isTrue(p.Lookup)) {
			name = 'KeyLookup';
		} else if (p.bodyTag === 'IndexScan' && p.storage === 'ColumnStore') {
			name = 'ColumnStoreIndexScan';
		} else if (p.bodyTag === 'ScalarInsert' && p.storage === 'ColumnStore') {
			name = 'ColumnStoreIndexInsert';
		} else if (p.bodyTag === 'Update' && p.storage === 'ColumnStore' && p.logicalOp) {
			name = 'ColumnStoreIndex' + p.logicalOp.replace(/ /g, '');
		} else if (p.bodyTag === 'TableValuedFunction') {
			name = 'TableValuedFunction';
		} else if (node.op) {
			name = node.op.replace(/ /g, '');
		}

		return (name && ICON.hasOwnProperty(name)) ? name : 'Catchall';
	}

	function iconStyleFor(node) {
		var pos = ICON[iconKeyFor(node)] || ICON.Catchall;
		return 'background-image:url(' + ICON_SPRITE_URL + ');background-position:' + pos[0] + 'px ' + pos[1] + 'px;';
	}

	// ─────────────────────────────────────────────────────────────────────────
	// Operator descriptions — plain-English "what does this operator actually do", shown at the top
	// of the hover/pinned detail panel. Condensed from Microsoft's own "Showplan Logical and
	// Physical Operators Reference" (learn.microsoft.com). Ordered, most-specific-first, matched
	// against PhysicalOp then LogicalOp - same approach as the ASE renderer's equivalent list.
	// ─────────────────────────────────────────────────────────────────────────

	var OPERATOR_DESCRIPTIONS = [
		{ test: /^Key Lookup$/i,            text: 'Looks a row up in a clustered index by its key, to fetch columns a non-clustered index did not cover. One lookup per row - a frequent cause of a cheap-looking seek becoming expensive. Widening the non-clustered index to cover the query removes it.' },
		{ test: /^RID Lookup$/i,            text: 'Looks a row up in a heap by its row identifier, to fetch columns the non-clustered index did not cover. Like a Key Lookup, but on a table with no clustered index.' },
		{ test: /Clustered Index Seek/i,    text: 'Navigates the clustered index B-tree to the specific rows matching a seek predicate, reading only that range - not the whole table.' },
		{ test: /Clustered Index Scan/i,    text: 'Reads the clustered index end to end - i.e. every row of the table, since the clustered index IS the table data. A scan on a large table where a seek was expected usually means no usable index, or a predicate the optimizer could not turn into a seek.' },
		{ test: /Index Seek/i,              text: 'Navigates a non-clustered index B-tree to the rows matching a seek predicate. If the index does not cover every column needed, each match costs an extra Key/RID Lookup into the base table.' },
		{ test: /Index Scan/i,              text: 'Reads every row of a non-clustered index. Cheaper than scanning the table when the index is narrow and covers the query, but still a full pass over that index.' },
		{ test: /Table Scan/i,              text: 'Reads every row of a heap (a table with no clustered index) in allocation order. No index is used.' },
		{ test: /Columnstore Index Scan/i,  text: 'Reads a columnstore index, fetching only the referenced columns and skipping row groups the predicate excludes. Usually runs in batch mode over large scans.' },
		{ test: /Nested Loops/i,            text: 'For each row from the outer (upper) input, searches the inner (lower) input for matching rows. Efficient when the outer input is small and the inner side has a supporting index; costly when the outer input is large, since the inner side is probed once per row.' },
		{ test: /Hash Match/i,              text: 'Builds an in-memory hash table from the first (build) input, then probes it with the second. Handles large unsorted inputs and needs no index, but requires a memory grant - if the grant is too small it spills to tempdb, which is far slower.' },
		{ test: /Adaptive Join/i,           text: 'Defers the choice between a Nested Loops and a Hash Match join until the build input has been read, then picks based on the actual row count against a threshold. Protects against a bad cardinality estimate on that input.' },
		{ test: /Merge Join/i,              text: 'Walks two inputs that are already sorted on the join key in step, matching as it goes. Very efficient when both inputs are already ordered; otherwise the required Sorts usually cost more than a Hash Match would.' },
		{ test: /Batch Hash Table Build/i,  text: 'Builds the hash table for a batch-mode hash join. Batch mode processes rows in groups of up to ~900 rather than one at a time, which greatly reduces CPU per row on large scans.' },
		{ test: /^Sort$/i,                  text: 'Sorts its input. Blocking (no rows flow out until every row is in) and memory-hungry - if the memory grant is too small it spills to tempdb. An explicit Sort often disappears if an index already provides the required order.' },
		{ test: /Sort/i,                    text: 'Sorts its input, keeping only the rows the query needs (a TopN Sort keeps just the top N, so it never has to sort the whole input).' },
		{ test: /Stream Aggregate/i,        text: 'Aggregates rows that already arrive grouped/sorted on the grouping columns, emitting one row per group as it goes. Cheap, but it depends on that ordering - which is often supplied by a preceding Sort or an index.' },
		{ test: /Hash Match.*Aggregate|Aggregate/i, text: 'Groups rows and computes aggregate values. A hash-based aggregate needs no input ordering but does need a memory grant.' },
		{ test: /Parallelism/i,             text: 'Moves rows between parallel threads. Gather Streams merges worker threads back into one; Repartition Streams redistributes rows across threads; Distribute Streams splits a single stream out to several. Excessive exchange operators, or uneven row distribution across threads, are common parallel-plan problems.' },
		{ test: /Compute Scalar/i,          text: 'Evaluates an expression to produce a computed value. Usually very cheap, and often deferred so the work actually happens in a later operator.' },
		{ test: /^Filter$/i,                text: 'Discards rows that do not satisfy its predicate. A Filter sitting far above the scan that produced the rows means the predicate could not be pushed down into the seek/scan itself.' },
		{ test: /^Top$/i,                   text: 'Passes through only the first N rows (or N percent) and then stops, which can let the operators below it stop early too.' },
		{ test: /Concatenation/i,           text: 'Appends the rows of several inputs one after another, as UNION ALL does. No duplicate removal and no sorting.' },
		{ test: /Index Spool|Table Spool|Row Count Spool|Window Spool/i, text: 'Materialises its input into a temporary worktable in tempdb so it can be re-read - once (lazy) or repeatedly (eager). An Eager Index Spool in particular often signals a missing index: the engine is building one on the fly, per execution.' },
		{ test: /^Segment$/i,               text: 'Splits the input into groups on a set of columns, flagging where each group starts - the input to window functions and other per-group operators.' },
		{ test: /Sequence Project/i,        text: 'Computes ranking/window values (ROW_NUMBER, RANK, DENSE_RANK, NTILE) over the groups a Segment marked out.' },
		{ test: /Window Aggregate/i,        text: 'Computes window-function aggregates in batch mode over an ordered stream - generally much faster than the Segment/Spool/Stream Aggregate combination it replaces.' },
		{ test: /^Assert$/i,                text: 'Verifies a condition and raises an error if it fails - how CHECK constraints, foreign keys and scalar-subquery cardinality rules are enforced.' },
		{ test: /^Split$/i,                 text: 'Turns each UPDATE into a DELETE followed by an INSERT, so the two halves can be applied separately (part of the per-index maintenance of an update).' },
		{ test: /^Collapse$/i,              text: 'Recombines a DELETE/INSERT pair on the same key back into a single update, undoing a Split where it is safe to do so.' },
		{ test: /Constant Scan/i,           text: 'Produces a small fixed set of rows (often exactly one, or none) without reading any table - used to introduce literal or computed values into a plan.' },
		{ test: /Deleted Scan|Inserted Scan/i, text: 'Reads the deleted/inserted pseudo-tables inside a trigger.' },
		{ test: /Table-valued function/i,   text: 'Executes a table-valued function and returns its rows. For multi-statement TVFs the optimizer has little cardinality information, so estimates here are often badly wrong.' },
		{ test: /^UDX$/i,                   text: 'Runs an extended/user-defined operator - XML, spatial and similar built-in extensions surface here.' },
		{ test: /Bitmap/i,                  text: 'Builds a bitmap filter from a join build input and pushes it down to the probe side, so non-matching rows are eliminated early. A parallel-plan optimisation.' },
		{ test: /Insert/i,                  text: 'Inserts rows into a table or index.' },
		{ test: /Update/i,                  text: 'Updates rows in a table or index.' },
		{ test: /Delete/i,                  text: 'Deletes rows from a table or index.' },
		{ test: /Merge/i,                   text: 'Applies the insert/update/delete actions of a MERGE statement to a table or index.' },
		{ test: /^Sequence$/i,              text: 'Runs its inputs left to right in order, returning only the rows from the last one - used for wide (per-index) update plans.' },
		{ test: /Remote/i,                  text: 'Sends the operation to a remote server (a linked server or distributed query) and reads the results back.' }
	];

	function operatorDescriptionFor(node) {
		var candidates = [node.op, node.props && node.props.logicalOp];
		for (var c = 0; c < candidates.length; c++) {
			if (!candidates[c]) continue;
			for (var i = 0; i < OPERATOR_DESCRIPTIONS.length; i++) {
				if (OPERATOR_DESCRIPTIONS[i].test.test(candidates[c])) return OPERATOR_DESCRIPTIONS[i].text;
			}
		}
		return undefined;
	}

	// Plain-English explanation per warning type, so the box's warning badge and the detail panel say
	// what is actually wrong rather than just echoing the XML element name.
	var WARNING_TEXT = {
		SpillToTempDb:          'Spilled to tempdb - the memory grant was too small to hold the data, so it was written to disk and re-read.',
		SortSpillDetails:       'Sort spilled to tempdb.',
		HashSpillDetails:       'Hash join/aggregate spilled to tempdb.',
		PlanAffectingConvert:   'An implicit type conversion changed how the plan could be built - this commonly prevents an index seek and forces a scan.',
		ColumnsWithNoStatistics:'No statistics exist for one or more columns used here, so the row estimates are guesses.',
		NoJoinPredicate:        'This join has no join predicate - it produces a Cartesian product of its two inputs.',
		MemoryGrantWarning:     'The memory grant was significantly larger or smaller than what the query actually used.',
		UnmatchedIndexes:       'A filtered index could not be used because the query is parameterised.',
		FullUpdateForOnlineIndexBuild: 'An online index build forced a full update of the table.',
		Wait:                   'The query spent measurable time waiting on a resource.'
	};

	/**
	 * SortSpillDetails/HashSpillDetails carry the actual tempdb volume on WritesToTempDb/
	 * ReadsFromTempDb - a page count (8 KB pages, the same convention as every other page-based size
	 * SQL Server reports) rather than something already in bytes/KB/MB like most other warning
	 * attributes, so it is worth converting rather than dumping the raw page count next to
	 * GrantedMemoryKb/UsedMemoryKb (which ARE already in KB). Used by warningTitle() below.
	 */
	function fmtSpillPages(attrs) {
		var PAGE_KB = 8;
		var parts = [];
		['WritesToTempDb', 'ReadsFromTempDb'].forEach(function (k) {
			var pages = parseFloat(attrs[k]);
			if (isNaN(pages)) return;
			var label = k === 'WritesToTempDb' ? 'Wrote' : 'Read';
			parts.push(label + ' ' + fmtNum(pages) + ' pages (' + fmtNum(pages * PAGE_KB / 1024) + ' MB)');
		});
		return parts.join(', ');
	}

	function warningTitle(w) {
		var base = WARNING_TEXT[w.type] || w.type;
		if ((w.type === 'SortSpillDetails' || w.type === 'HashSpillDetails') && w.attrs) {
			var spillText = fmtSpillPages(w.attrs);
			if (spillText) {
				// GrantedMemoryKb/UsedMemoryKb are the only other attributes these two warning types
				// carry - shown in MB alongside the page-derived MB above, everything else (unknown
				// future attributes) still falls through to the generic key=value dump.
				var known = { WritesToTempDb: 1, ReadsFromTempDb: 1, GrantedMemoryKb: 1, UsedMemoryKb: 1 };
				if (w.attrs.GrantedMemoryKb !== undefined) spillText += ', granted ' + fmtNum(w.attrs.GrantedMemoryKb / 1024) + ' MB';
				if (w.attrs.UsedMemoryKb    !== undefined) spillText += ', used ' + fmtNum(w.attrs.UsedMemoryKb / 1024) + ' MB';
				var rest = Object.keys(w.attrs).filter(function (k) { return !known[k]; })
					.map(function (k) { return k + '=' + w.attrs[k]; }).join(', ');
				return base + ' (' + spillText + (rest ? ', ' + rest : '') + ')';
			}
		}
		// Spill warnings carry the detail on attributes (WritesToTempDb, SpillLevel, ...) - append
		// whatever the plan supplied rather than trying to enumerate per-type attribute names.
		var extra = Object.keys(w.attrs || {}).map(function (k) { return k + '=' + w.attrs[k]; }).join(', ');
		return extra ? base + ' (' + extra + ')' : base;
	}

	// ─────────────────────────────────────────────────────────────────────────
	// Formatting helpers
	// ─────────────────────────────────────────────────────────────────────────

	function fmtNum(n) {
		if (n === undefined || n === null || isNaN(n)) return undefined;
		// Estimated row counts are fractional (EstimateRows="1416.87"); actual counts never are.
		var v = (n < 10 && n !== Math.floor(n)) ? Math.round(n * 100) / 100 : Math.round(n);
		return v.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',');
	}

	/** Plain decimal, never exponential, with trailing zeros trimmed. */
	function fmtPlainDecimal(n, maxDecimals) {
		var s = n.toFixed(Math.min(20, maxDecimals === undefined ? 6 : maxDecimals));
		if (s.indexOf('.') >= 0) s = s.replace(/0+$/, '').replace(/\.$/, '');
		return s;
	}

	function addThousands(intPart) {
		return intPart.replace(/\B(?=(\d{3})+(?!\d))/g, ',');
	}

	function fmtCost(n) {
		if (n === undefined || n === null || isNaN(n)) return undefined;
		// Was toExponential(2) below 0.001, which produced exactly the "1.20e-6" style that is hard
		// to read at a glance. Small costs are written out in full instead.
		if (n === 0) return '0';
		if (Math.abs(n) < 0.001) return fmtPlainDecimal(n, 12);
		var v = fmtPlainDecimal(n, 6);
		var parts = v.split('.');
		return addThousands(parts[0]) + (parts[1] ? '.' + parts[1] : '');
	}

	/**
	 * Format a raw attribute value from the plan XML.
	 *
	 * SQL Server writes plenty of numbers in scientific notation - `TableCardinality="4.10726e+006"`,
	 * `EstimateCPU="1e-006"` (141 such attributes across the 55-plan reference corpus) - which is
	 * near-unreadable in a properties list. Anything that parses as a plain number is rewritten in
	 * full decimal, with thousands separators once it reaches 1000. Everything else (hashes like
	 * "0x1122...", versions like "15.0.4390.2", plain words) is returned untouched, since the strict
	 * regex below will not match it.
	 */
	function fmtXmlValue(v) {
		if (typeof v === 'number') return fmtNum(v);
		if (typeof v !== 'string') return v;
		if (!/^-?\d+(\.\d+)?([eE][+-]?\d+)?$/.test(v)) return v;
		var num = parseFloat(v);
		if (!isFinite(num)) return v;
		var out;
		if (Number.isInteger(num)) out = String(num);
		else if (Math.abs(num) < 0.001) out = fmtPlainDecimal(num, 12);
		else out = fmtPlainDecimal(num, 6);
		var parts = out.split('.');
		return addThousands(parts[0]) + (parts[1] ? '.' + parts[1] : '');
	}

	/**
	 * Milliseconds as "1d 2h 3m 4s 5ms", dropping any component that is zero so a short query reads
	 * "137ms" rather than "0d 0h 0m 0s 137ms".
	 *
	 * html-query-plan prints a fixed "N.NNN s" instead, which loses all resolution below a
	 * millisecond-ish and turns anything long into an unreadable pile of seconds (a 2-hour query
	 * reads "7200.000 s"). This matches the d/h/m/s/ms convention the dialog's own Runtime-stats
	 * block already uses (fmtHMS in dbxShowplan.js), so the two agree.
	 */
	function fmtDuration(ms) {
		if (ms === undefined || ms === null || isNaN(ms)) return undefined;
		if (ms <= 0) return '0 ms';
		var d  = Math.floor(ms / 86400000);
		var h  = Math.floor((ms % 86400000) / 3600000);
		var m  = Math.floor((ms % 3600000) / 60000);
		var s  = Math.floor((ms % 60000) / 1000);
		var rest = Math.round(ms % 1000);
		var parts = [];
		if (d > 0)    parts.push(d + 'd');
		if (h > 0)    parts.push(h + 'h');
		if (m > 0)    parts.push(m + 'm');
		if (s > 0)    parts.push(s + 's');
		// Milliseconds are shown whenever non-zero, and are the sole component for a sub-second time.
		if (rest > 0 || !parts.length) parts.push(rest + 'ms');
		return parts.join(' ');
	}

	function timeLine(text) {
		return $('<div class="ss-plan-metric ss-plan-time"></div>').text(text);
	}

	function fmtBytes(b) {
		if (b === undefined || b === null || isNaN(b)) return undefined;
		if (b >= 1073741824) return (b / 1073741824).toFixed(1) + ' GB';
		if (b >= 1048576)    return (b / 1048576).toFixed(1) + ' MB';
		if (b >= 1024)       return (b / 1024).toFixed(1) + ' KB';
		return Math.round(b) + ' B';
	}

	// ─────────────────────────────────────────────────────────────────────────
	// Connector (arrow) sizing and tooltips
	//
	// The number of rows a connector carries is a property of the CHILD operator - the arrow shows
	// rows flowing out of it and into its parent.
	// ─────────────────────────────────────────────────────────────────────────

	function connectorRows(node) {
		if (!node || !node.metrics) return undefined;
		var m = node.metrics;
		// Prefer what actually happened; fall back to the estimate on an estimated-only plan.
		return (m.actRows !== undefined) ? m.actRows : m.estRows;
	}

	/**
	 * Line width in pixels for a given row count, using html-query-plan's own nodeToThickness()
	 * formula verbatim so the two renderers weight their arrows identically:
	 *
	 *     max(2, min(floor(ln(rows)), 12))
	 *
	 * The log scale is the point of it. Row counts within a single plan routinely span six orders of
	 * magnitude, so a linear mapping would render everything except the single widest edge as a
	 * hairline. ln() compresses that into a usable 2..12px band: 1 row -> 2px, 100 -> 4px,
	 * 10k -> 9px, 1M and beyond -> 12px.
	 */
	function rowsToThickness(rows) {
		var r = (rows !== undefined && rows > 0) ? rows : 1;
		return Math.max(2, Math.min(Math.floor(Math.log(r)), 12));
	}

	/**
	 * Native SVG <title> text for a connector - the numbers SSMS shows when hovering an arrow.
	 * Returned as plain multi-line text; the browser renders it as an ordinary tooltip, so this
	 * needs no tooltip library and works while the diagram is zoomed or panned.
	 */
	function connectorTooltip(childNode, parentNode) {
		if (!childNode) return undefined;
		var m = childNode.metrics || {};
		var lines = [];

		var from = childNode.op + (childNode.props && childNode.props.objName ? ' (' + childNode.props.objName + ')' : '');
		lines.push(from + '  →  ' + (parentNode ? parentNode.op : ''));
		lines.push('');

		if (m.actRows !== undefined)  lines.push('Actual rows:      ' + fmtNum(m.actRows));
		if (m.estRows !== undefined)  lines.push('Estimated rows:   ' + fmtNum(m.estRows));
		var pct = fmtEstActDiff(m);
		if (pct !== undefined)        lines.push('Actual % of est:  ' + pct);
		if (m.ActualRowsRead !== undefined) lines.push('Rows read:        ' + fmtNum(m.ActualRowsRead));
		if (m.ActualExecutions !== undefined) lines.push('Executions:       ' + fmtNum(m.ActualExecutions));
		if (m.avgRowSize !== undefined) {
			lines.push('Est. row size:    ' + fmtBytes(m.avgRowSize));
			// Estimated data size is what the optimizer costed the exchange on, and the reason a
			// "few rows" arrow can still be the expensive one when the rows are very wide.
			var rows = connectorRows(childNode);
			if (rows !== undefined && rows > 0) {
				lines.push('Est. data size:   ' + fmtBytes(rows * m.avgRowSize));
			}
		}
		return lines.join('\n');
	}

	/** Actual rows as a percentage of estimated, e.g. 1533 for a 15x underestimate. */
	function estActPercent(m) {
		if (!m || m.estRows === undefined || m.actRows === undefined || !m.estRows) return undefined;
		return (m.actRows / m.estRows) * 100;
	}

	function fmtPercent(p) {
		if (p === undefined || isNaN(p)) return undefined;
		return (p >= 1000 ? Math.round(p) : Math.round(p * 10) / 10).toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',') + '%';
	}

	/**
	 * Est/Act row comparison, but as a multiplier ("15x") rather than a percentage once the
	 * difference crosses the same >10x/<0.1x threshold isEstActWarn() already uses to flag it - a
	 * percentage like "153,300%" doesn't read as "obviously big" the way "1533x" does. Past ~1000x
	 * the exact multiplier stops being useful information, so that's reported as "huge-diff" instead
	 * of a giant number. Small/normal differences are unaffected and still show as a plain percentage.
	 *
	 * Zero actual rows is its own case, not just an extreme "huge-diff": a ratio of 0 would otherwise
	 * compute to a magnitude of Infinity (any positive estimate divided into 0), landing on
	 * "huge-diff" - technically a huge difference, but that phrasing reads as "the numbers were very
	 * different", not "nothing at all came out of this operator", which is the actually useful signal
	 * (e.g. a predicate that matched nothing, or a seek that found nothing downstream).
	 */
	function fmtEstActDiff(m) {
		var pct = estActPercent(m);
		if (pct === undefined) return undefined;
		if (!isEstActWarn(m)) return fmtPercent(pct);
		if (m.actRows === 0) return 'zero-rows';
		var ratio = pct / 100;
		var magnitude = ratio >= 1 ? ratio : 1 / ratio;
		if (magnitude >= 1000) return 'huge-diff';
		return Math.round(magnitude) + 'x';
	}

	/**
	 * A cardinality estimate is "off" when actual and estimated rows differ by more than 10x in
	 * either direction - the same threshold the ASE renderer uses, and roughly where a wrong estimate
	 * starts changing which operators/indexes the optimizer would have picked.
	 */
	function isEstActWarn(m) {
		if (!m || m.estRows === undefined || m.actRows === undefined) return false;
		if (m.estRows <= 0) return m.actRows > 0;
		var ratio = m.actRows / m.estRows;
		return ratio > 10 || ratio < 0.1;
	}

	/**
	 * How much a single-input operator (a Filter is the classic case, but this applies to any node
	 * with exactly one child) discarded between its child's output and its own. A node's own Est/Act
	 * is its OUTPUT row count, not how much of its INPUT it threw away - "Filter shows 12,450 rows"
	 * alone doesn't say whether 12,450 came in and all survived, or 10 million came in and almost all
	 * were discarded. Comparing against the (single) child's actual row count is what answers that.
	 *
	 * undefined when there isn't exactly one child, or either side's actual rows are missing/zero -
	 * multi-child nodes (joins, concats) have no single well-defined "input" to compare against.
	 *
	 * Ported from the ASE renderer, which has had this for a while; the CSS class was copied across
	 * with the rest of the file when this renderer was derived from it, but the code never was.
	 */
	/**
	 * Which operators may report a row reduction, and what to CALL it.
	 *
	 * Deliberately an allow-list. The first attempt excluded the operators that were obviously wrong
	 * (exchanges, hash-table builds, DML sinks) and let everything else through - but a block-list can
	 * only ever exclude what has already been noticed, and it let a "Sort" through claiming it had
	 * "filtered" 69% of its input. That plan (inequality_index) is a TOP 200 at DOP 6: 3,926 rows in,
	 * 1,200 out = 200 x 6 threads. The Sort kept the top N; it filtered nothing.
	 *
	 * The general trap: ANY operator can emit fewer rows than it consumed without filtering - early
	 * termination (a TOP upstream stops asking), per-thread accounting, or a build side that returns
	 * nothing by design. So only operators whose actual JOB is to reduce the row count qualify, and
	 * the wording follows the mechanism rather than calling everything "filtered": ASE's Restrict is a
	 * true filter, SQL Server's aggregates collapse rows into groups, and those are different claims.
	 */
	var ROW_REDUCING_OPS = [
		{ re: /^Filter$/i,                       verb: 'filtered' },
		{ re: /^(Stream Aggregate|Hash Match)$/i, logical: /Aggregate/i, verb: 'aggregated' }
	];

	function rowReductionVerb(node) {
		for (var i = 0; i < ROW_REDUCING_OPS.length; i++) {
			var r = ROW_REDUCING_OPS[i];
			if (!r.re.test(node.op || '')) continue;
			if (r.logical && !r.logical.test((node.props && node.props.logicalOp) || '')) continue;
			return r.verb;
		}
		return undefined;
	}

	function inputRowReductionPercent(node) {
		if (!node.children || node.children.length !== 1) return undefined;
		if (!rowReductionVerb(node)) return undefined;
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

	/** "db.schema.table [IndexName]" for the box subtitle. */
	function subtitleFor(node) {
		var p = node.props || {};
		// The statement node names no table - show the statement text instead, which is what
		// identifies it when a batch renders several statements one below the other.
		if (node.isStatement) {
			var txt = p.StatementText;
			if (!txt) return undefined;
			txt = txt.replace(/\s+/g, ' ').trim();
			return txt.length > 60 ? txt.slice(0, 60) + '…' : txt;
		}
		if (!p.objName) {
			// A Spool (Table/Row Count/Window Spool) that is NOT the one actually building the
			// worktable has no <Object> of its own - it just re-reads the spool another operator
			// already populated, named by @PrimaryNodeId (see the childRelOps() comment above). With
			// no objName there was nothing at all in this slot, which reads as "this operator doesn't
			// touch any data" - showing what it actually reads from is clearer.
			if (p.PrimaryNodeId !== undefined && p.PrimaryNodeId !== null && p.PrimaryNodeId !== '') {
				return 'Reads spool from Node ' + p.PrimaryNodeId;
			}
			return undefined;
		}
		var s = p.objName;
		if (p.alias) s += ' (' + p.alias + ')';
		if (p.indexName) s += ' [' + p.indexName + ']';
		return s;
	}

	/**
	 * An Eager Index Spool (PhysicalOp="Index Spool", LogicalOp="Eager Spool") means the engine is
	 * building a worktable index at runtime, once per execution - almost always a sign a real index
	 * is missing. Shared by renderNode() (box marking) and collectTreeFindings() (the "[Index Spool]"
	 * Plan Analysis finding) so the two conditions can't drift apart.
	 */

	// A lookup, however ShowPlanXML happens to spell it: PhysicalOp is "RID Lookup" for a heap but
	// "Clustered Index Seek" for the far more common clustered case - the only thing common to both is
	// the operator body's Lookup="true" flag, which parseBodyProps() copies through verbatim.
	function isLookup(props) {
		var v = props && props.Lookup;
		return v === true || v === 'true' || v === 1;
	}
	function isEagerIndexSpool(node) {
		var p = node.props || {};
		return /Index Spool/i.test(node.op) && /Eager/i.test(p.logicalOp || '');
	}

	/**
	 * Venn-diagram icon for LEFT/RIGHT/FULL OUTER and ANTI SEMI joins - ported from
	 * dbxShowplanAse.js's joinTypeIconFor(), re-keyed off LogicalOp (ASE has no PhysicalOp/LogicalOp
	 * split) instead of its own props.joinType. Inner/Semi joins get no badge, same as ASE.
	 */
	function joinTypeIconFor(node) {
		var jt = node.props && node.props.logicalOp;
		if (!jt) return undefined;
		if (/full\s*outer/i.test(jt)) return 'sql_join_outer.png';
		if (/left\s*outer/i.test(jt)) return 'sql_join_left.png';
		if (/right\s*outer/i.test(jt)) return 'sql_join_right.png';
		if (/anti/i.test(jt)) return /right/i.test(jt) ? 'sql_join_right_exclude.png' : 'sql_join_left_exclude.png';
		return undefined;
	}

	/** Explanatory sentence to go with joinTypeIconFor()'s badge, shown in the tooltip/Properties desc line. */
	function joinTypeNote(node) {
		var jt = node.props && node.props.logicalOp;
		if (!jt) return undefined;
		if (/full\s*outer/i.test(jt)) return 'As a Full Outer Join: every row from both sides is returned, matched where possible and NULL-padded where not.';
		if (/left\s*outer/i.test(jt)) return 'As a Left Outer Join: every row from the left (outer) side is returned, matched where possible and NULL-padded where not.';
		if (/right\s*outer/i.test(jt)) return 'As a Right Outer Join: every row from the right (outer) side is returned, matched where possible and NULL-padded where not.';
		if (/anti/i.test(jt)) return 'As an Anti Semi Join: only rows from one side that have NO match on the other side are returned.';
		return undefined;
	}

	// ─────────────────────────────────────────────────────────────────────────
	// Detail panel (hover tooltip / click-to-pin) — the SUMMARY view.
	//
	// Deliberately a summary, not everything: the full attribute dump lives in the Properties pane
	// (renderPropertiesInto() below), which is where SSMS puts it too. Cramming both into a hover
	// tooltip made it taller than the viewport on real plans.
	// ─────────────────────────────────────────────────────────────────────────

	function makeRowFn($tbl) {
		return function (key, val, valClass) {
			if (val === undefined || val === null || val === '') return;
			var $tr = $('<tr></tr>');
			$tr.append($('<td class="ss-plan-detail-key"></td>').text(key));
			$tr.append($('<td></td>').addClass(valClass || '').text(val));
			$tbl.append($tr);
		};
	}

	function buildDetailPanel(node) {
		var $panel = $('<div class="ss-plan-detail"></div>');
		var m = node.metrics || {}, p = node.props || {};

		var desc = operatorDescriptionFor(node);
		var joinNote = joinTypeNote(node);
		if (joinNote) desc = desc ? (desc + ' ' + joinNote) : joinNote;
		if (desc) $panel.append($('<div class="ss-plan-detail-desc"></div>').text(desc));

		if (node.warnings && node.warnings.length) {
			var $warns = $('<div class="ss-plan-detail-warnings"></div>');
			node.warnings.forEach(function (w) {
				$warns.append($('<div class="ss-plan-detail-warn-line"></div>').text('⚠ ' + warningTitle(w)));
			});
			$panel.append($warns);
		}

		var $tbl = $('<table></table>');
		var row = makeRowFn($tbl);

		row('Operator',      node.op);
		row('Logical Op',    p.logicalOp);
		row('Node ID',       p.nodeId);
		row('Object',        p.objName);
		row('Alias',         p.alias);
		row('Index',         p.indexName);
		row('Index Kind',    p.indexKind);

		// Same suppression as the Cardinality Estimate finding and the box itself, for the same reason:
		// "estimated N rows, produced 0" is not a misestimate on a branch that was never taken, so the
		// Act % of Est row is not highlighted as a problem here either.
		var warn = isEstActWarn(m) && !node._neverExecuted;
		row('Est Rows',      fmtNum(m.estRows));
		row('Act Rows',      fmtNum(m.actRows));
		var pct = fmtEstActDiff(m);
		if (pct !== undefined) row('Act % of Est', pct, warn ? 'ss-plan-detail-pct-warn' : '');
		row('Input Rows Filtered', fmtPercent(inputRowReductionPercent(node)));
		row('Executions',    fmtNum(m.ActualExecutions));
		row('Rows Read',     fmtNum(m.ActualRowsRead !== undefined ? m.ActualRowsRead : m.estRowsRead));
		row('Table Rows',    fmtNum(m.tableCardinality));

		row('Operator Cost', fmtCost(m.nodeCost));
		row('Cost %',        m.relativeCostPct === undefined ? undefined : fmtPercent(m.relativeCostPct));
		row('Subtree Cost',  fmtCost(m.subtreeCost));

		// Statement-level totals from <QueryTimeStats> (the whole query), distinct from the
		// per-operator ActualElapsedms below.
		row('Query Elapsed',  fmtDuration(m.elapsedMs));
		row('Query CPU',      fmtDuration(m.cpuMs));
		row('UDF Elapsed',    fmtDuration(m.udfElapsedMs));
		row('UDF CPU',        fmtDuration(m.udfCpuMs));
		row('Statement Cost', node.isStatement ? fmtCost(m.subtreeCost) : undefined);

		// Plan-cache-level facts, same set html-query-plan shows on this node (qp.xslt's ToolTipGrid
		// reads them off <QueryPlan>, which only the statement node's tooltip has as a direct child).
		if (node.isStatement) {
			row('Degree of Parallelism', p.degreeOfParallelism);
			// The real total, as opposed to DOP alone - see the ThreadStat comment in
			// buildStatementNode(). Only spells out the "N branches x DOP" arithmetic when there is
			// more than one branch; for the common single-branch case it would just restate DOP.
			row('Total Threads Used', m.threadsUsed === undefined ? undefined :
				fmtNum(m.threadsUsed) + (m.threadBranches > 1
					? ' (' + m.threadBranches + ' branches × DOP ' + (p.degreeOfParallelism || '?') + ')' : ''));
			row('Memory Grant',          m.memoryGrantKb === undefined ? undefined : fmtNum(m.memoryGrantKb) + ' KB');
			row('Cached Plan Size',      m.cachedPlanSizeKb === undefined ? undefined : fmtNum(m.cachedPlanSizeKb) + ' KB');
			row('Compile Time',          fmtDuration(m.compileTimeMs));
			row('Compile CPU',           fmtDuration(m.compileCpuMs));
			row('Compile Memory',        m.compileMemoryKb === undefined ? undefined : fmtNum(m.compileMemoryKb) + ' KB');
			row('Non-Parallel Reason',   p.nonParallelPlanReason);
		}

		row('Elapsed',       fmtDuration(m.ActualElapsedms));
		row('CPU (ms)',      fmtNum(m.ActualCPUms));
		row('Logical Reads', fmtNum(m.ActualLogicalReads));
		row('Physical Reads',fmtNum(m.ActualPhysicalReads));
		row('Threads',       m.threadCount > 1 ? m.threadCount : undefined);
		row('Exec Mode',     p.actualExecutionMode || p.estimatedExecutionMode);

		row('Seek Predicate',p.seekPredicate);
		row('Order By',      p.orderBy);
		row('Group By',      p.groupBy);
		row('Outer Refs',    p.outerReferences);

		// DDL Storage column - same treatment as the ASE renderer's: only for operators that actually
		// name a table, and only once the async lookup in loadTableInfoAsync() has landed.
		if (p.objName && (node._tableInfo || node._tableInfoPending)) {
			var $right = $('<div class="ss-plan-detail-right"></div>');
			var $rtbl = $('<table></table>');
			var rrow = makeRowFn($rtbl);
			var info = node._tableInfo;

			if (node._tableInfoPending) {
				rrow('Table Info', '⏳ Loading…');
			} else if (!info || info.found === false) {
				rrow('Table Info', 'not found in DDL Storage');
			} else {
				rrow('DDL Sample time', (info.sampleTime || '').replace(/\.\d+$/, ''));
				rrow('Table Rows',      fmtNum(info.rowTotal));
				rrow('Total Size',      info.totalMb === undefined ? undefined : fmtNum(info.totalMb) + ' MB');
				rrow('In-Row Data',     info.inRowMb === undefined ? undefined : fmtNum(info.inRowMb) + ' MB');
				rrow('LOB',             (info.lobMb === undefined || info.lobMb <= 0) ? '-no-lob-' : fmtNum(info.lobMb) + ' MB');
				rrow('Index Count',     info.indexCount);
			}
			$right.append($rtbl);

			if (info && info.indexes && info.indexes.length) {
				$right.append($('<div class="ss-plan-detail-idx-hdr"></div>').text('Indexes (' + info.indexes.length + '):'));
				var $idxTbl = $('<table class="ss-plan-idx-tbl"></table>');
				var idxRow = makeRowFn($idxTbl);
				var used = p.indexName;
				info.indexes.forEach(function (idx) {
					var cols = (idx.keys && idx.keys.length) ? idx.keys.join(', ') : '-';
					if (idx.includeCols && idx.includeCols.length) cols += ' INCLUDE (' + idx.includeCols.join(', ') + ')';
					// Mark the index this very operator is using, so a wide index list stays readable.
					var isUsed = used && idx.indexName && used.toLowerCase() === idx.indexName.toLowerCase();
					idxRow((isUsed ? '▶ ' : '') + (idx.indexName || '(heap)'),
						fmtNum(idx.sizeMb) + ' MB (' + cols + ')');
				});
				$right.append($idxTbl);
			}

			$panel.append($('<div class="ss-plan-detail-grid"></div>')
				.append($('<div class="ss-plan-detail-left"></div>').append($tbl))
				.append($right));
		} else {
			$panel.append($tbl);
		}

		// Predicate and Output List go BELOW the two-column grid, full width, rather than as rows in
		// $tbl above - a long value in a table cell (".ss-plan-detail td" is white-space:nowrap, so
		// values stay on one line and don't wrap) forced the left column wider than intended, pushing
		// the DDL column out to the right of the visible panel or off-screen entirely. Reported live:
		// a wide Predicate or Output List "hides" the DDL info. Plain wrapping blocks here have no
		// such width pressure - long text just wraps within the panel's own max-width.
		// Hash Match's own join key columns - already parsed (parseBodyProps()) into
		// props.hashKeysBuild/hashKeysProbe, alias-qualified when the plan itself qualifies them (e.g.
		// "c1.id, c1.time"), but never shown anywhere before now. Useful for exactly what Predicate is
		// useful for - which columns from which side would make an index candidate - so it gets the
		// same full-width treatment, placed just before it.
		if (p.hashKeysBuild) {
			$panel.append($('<div class="ss-plan-detail-idx-hdr"></div>').text('Hash Build Keys:'));
			$panel.append($('<div class="ss-plan-detail-wrap"></div>').text(p.hashKeysBuild));
		}
		if (p.hashKeysProbe) {
			$panel.append($('<div class="ss-plan-detail-idx-hdr"></div>').text('Hash Probe Keys:'));
			$panel.append($('<div class="ss-plan-detail-wrap"></div>').text(p.hashKeysProbe));
		}
		if (p.predicate) {
			$panel.append($('<div class="ss-plan-detail-idx-hdr"></div>').text('Predicate:'));
			$panel.append($('<div class="ss-plan-detail-wrap"></div>').text(p.predicate));
		}
		if (p.outputListCols && p.outputListCols.length) {
			// Capped in this summary view - a wide SELECT's RelOp can carry 90+ columns (measured
			// against the reference corpus), too many for a hover popup - full list in Properties.
			var OUT_CAP = 8;
			var outShown = p.outputListCols.slice(0, OUT_CAP).join(', ');
			if (p.outputListCols.length > OUT_CAP) {
				outShown += ', … (+' + (p.outputListCols.length - OUT_CAP) + ' more - see Properties)';
			}
			$panel.append($('<div class="ss-plan-detail-idx-hdr"></div>').text('Output List (' + p.outputListCols.length + '):'));
			$panel.append($('<div class="ss-plan-detail-wrap"></div>').text(outShown));
		}

		// Worker thread row distribution - same data and the same busiest-thread highlight as the
		// Properties pane's "Parallel Threads" section (see buildThreadRows()), just capped here so a
		// DOP-64 plan cannot blow up a hover popup; the full list is always one click away.
		if (m.perThread && m.perThread.length > 1) {
			$panel.append($('<div class="ss-plan-detail-idx-hdr"></div>')
				.text('Parallel Threads (' + m.perThread.length + '):'));
			$panel.append(buildThreadRows(m, 8));
		}

		// Same treatment for the statement's top wait types - capped here, full list in Properties.
		if (m.waitStats && m.waitStats.length) {
			$panel.append($('<div class="ss-plan-detail-idx-hdr"></div>')
				.text('Wait Statistics (' + m.waitStats.length + '):'));
			$panel.append(buildWaitRows(m.waitStats, 5));
		}

		if (p.parameters && p.parameters.length) {
			$panel.append($('<div class="ss-plan-detail-idx-hdr"></div>')
				.text('Parameters (' + p.parameters.length + '):'));
			$panel.append(buildParameterRows(p.parameters, 5));
		}

		$panel.append($('<div class="ss-plan-detail-hint"></div>')
			.text('Click the box to pin this, and to show all properties in the Properties pane.'));
		return $panel;
	}

	// ─────────────────────────────────────────────────────────────────────────
	// Properties pane — the SSMS-style "everything about this operator" view.
	//
	// Built from node._xmlEl (the live <RelOp> element) rather than from the parsed model, so it can
	// show EVERY attribute and nested element the plan carries without this file having to enumerate
	// them. Nested <RelOp> subtrees are pruned - those are separate operators with their own boxes.
	// ─────────────────────────────────────────────────────────────────────────

	// Attribute groupings, in the order SSMS presents them. Anything not listed falls into "Misc",
	// so an unrecognised/new attribute is still shown rather than silently dropped.
	var PROP_GROUPS = [
		{ title: 'Estimated', keys: ['EstimateRows', 'EstimatedRowsRead', 'EstimateIO', 'EstimateCPU',
			'EstimatedTotalSubtreeCost', 'AvgRowSize', 'EstimateRebinds', 'EstimateRewinds',
			'EstimatedExecutionMode', 'TableCardinality', 'EstimatedJoinType', 'AdaptiveThresholdRows'] },
		{ title: 'Actual', keys: ['ActualRows', 'ActualRowsRead', 'ActualExecutions', 'ActualElapsedms',
			'ActualCPUms', 'ActualScans', 'ActualLogicalReads', 'ActualPhysicalReads', 'ActualReadAheads',
			'ActualLobLogicalReads', 'ActualLobPhysicalReads', 'ActualLobReadAheads', 'ActualEndOfScans',
			'ActualRebinds', 'ActualRewinds', 'Batches', 'SegmentReads', 'SegmentSkips',
			'UsedMemoryGrant', 'InputMemoryGrant', 'OutputMemoryGrant'] }
	];

	function escapeText(s) {
		return $('<div></div>').text(s === undefined ? '' : String(s)).html();
	}

	// Generic pane scaffolding, shared with the ASE renderer (dbxShowplanGraph.js) so the two panes
	// can't drift apart. Same-named local wrappers keep every existing call site here unchanged; the
	// SECTIONS themselves stay in this file, since which fields are worth showing is vendor-specific.
	function propRow($into, key, val)   { return DbxShowplanGraph.propRow($into, 'ss-plan', key, val); }
	function propSection($into, title)  { return DbxShowplanGraph.propSection($into, 'ss-plan', title); }

	function renderXmlTree($into, el, depth) {
		// Stops at a nested <RelOp> - that is a different operator, with its own pane. fmtXmlValue
		// rather than the raw string, because the plan XML is full of scientific notation.
		return DbxShowplanGraph.propXmlTree($into, 'ss-plan', el, depth,
			{ stopAt: function (k) { return lname(k) === 'RelOp'; }, formatValue: fmtXmlValue });
	}

	/**
	 * Fill the Properties pane for one node. Public via the module's return value so dbxShowplan.js
	 * can also clear/repopulate it (e.g. when switching plans or renderers).
	 */
	/**
	 * Which thread (if any) is the one dbxShowplanAnalyzer.js's "Skewed parallelism" Plan Analysis
	 * finding would flag, using its EXACT threshold math (DOP-aware: 80% on one thread for a 2-way
	 * split, 50% for anything wider; the busiest thread must also beat 2x the average, and the total
	 * must be big enough that a handful of rows doesn't trip it). Duplicated rather than shared
	 * because the two files parse independently (raw XML there, the parsed tree here) - kept
	 * identical on purpose so the Properties pane's highlight and that finding never disagree about
	 * the same node. Returns -1 when nothing is skewed.
	 */
	function skewedThreadIndex(rowCounts) {
		if (rowCounts.length < 2) return -1;
		var total = rowCounts.reduce(function (a, b) { return a + b; }, 0);
		if (total === 0) return -1;
		var maxRows = Math.max.apply(null, rowCounts);
		var avgRows = total / rowCounts.length;
		var skewThreshold = rowCounts.length === 2 ? total * 0.80 : total * 0.50;
		if (maxRows > avgRows * 2 && maxRows > skewThreshold && total > rowCounts.length * 1000) {
			return rowCounts.indexOf(maxRows);
		}
		return -1;
	}

	/**
	 * Builds the "Thread N [bar] rows" rows shared by the hover/pinned detail panel (cap > 0, since
	 * a hover popup should not grow to fit a DOP-64 plan) and the Properties pane (cap 0 = show all -
	 * it already scrolls, and the reference corpus tops out at 13 threads anyway). Returns a jQuery
	 * collection: one row per thread up to the cap, an optional "+N more" line, and the Thread-0
	 * explainer line when applicable - append directly into either panel's own container.
	 */
	function buildThreadRows(m, cap) {
		var perThread = m.perThread;
		var rowCounts = perThread.map(function (t) { return t.actualRows || 0; });
		var maxRows   = Math.max.apply(null, rowCounts);
		var skewIdx   = skewedThreadIndex(rowCounts);
		var shown     = cap ? perThread.slice(0, cap) : perThread;
		var $frag = $();
		shown.forEach(function (t, i) {
			var rows = t.actualRows || 0;
			var pct  = maxRows > 0 ? (rows / maxRows * 100) : 0;
			var $fill = $('<span class="ss-plan-prop-thread-fill"></span>').css('width', pct + '%');
			if (i === skewIdx) $fill.addClass('ss-plan-prop-thread-skewed');
			$frag = $frag.add($('<div class="ss-plan-prop-thread-row"></div>')
				.append($('<span class="ss-plan-prop-thread-label"></span>').text('Thread ' + t.thread))
				.append($('<span class="ss-plan-prop-thread-track"></span>').append($fill))
				.append($('<span class="ss-plan-prop-thread-value"></span>').text(fmtNum(rows) + ' rows')));
		});
		if (cap && perThread.length > cap) {
			var extra = perThread.length - cap;
			$frag = $frag.add($('<div class="ss-plan-prop-thread-note"></div>')
				.text('+ ' + extra + ' more thread' + (extra !== 1 ? 's' : '') + ' - see Properties'));
		}
		// Thread 0 reading 0 is the normal, expected shape, not a problem - it is the
		// coordinator/consumer thread for this parallel branch, not one of the workers that actually
		// process rows. Confirmed against the reference corpus: every multi-thread operator there has
		// Thread 0 at exactly 0 rows, with no exception - stated here as a fact about what Thread 0
		// IS, not an assumption, but still worded conditionally in case a real captured plan differs.
		if (perThread[0].thread === 0 && !(perThread[0].actualRows > 0)) {
			$frag = $frag.add($('<div class="ss-plan-prop-thread-note"></div>')
				.text('Thread 0 is the coordinator/consumer thread for this parallel branch, not a worker - 0 rows there is normal.'));
		}
		return $frag;
	}

	/**
	 * Statement-level wait-type rows, sorted by time (parseXml already sorted them). Same cap/no-cap
	 * split as buildThreadRows: capped in the tooltip, uncapped in the Properties pane.
	 */
	function buildWaitRows(waitStats, cap) {
		var shown = cap ? waitStats.slice(0, cap) : waitStats;
		var $frag = $();
		shown.forEach(function (w) {
			var avg = (w.waitCount > 0 && w.waitTimeMs !== undefined) ? (w.waitTimeMs / w.waitCount) : undefined;
			var val = fmtNum(w.waitTimeMs) + ' ms' +
				(w.waitCount !== undefined ? ', ' + fmtNum(w.waitCount) + ' wait' + (w.waitCount !== 1 ? 's' : '') : '') +
				(avg !== undefined ? ' (' + fmtNum(avg) + ' ms avg)' : '');
			$frag = $frag.add($('<div class="ss-plan-prop-row"></div>')
				.append($('<span class="ss-plan-prop-key"></span>').text(w.waitType))
				.append($('<span class="ss-plan-prop-val"></span>').text(val)));
		});
		if (cap && waitStats.length > cap) {
			var extra = waitStats.length - cap;
			$frag = $frag.add($('<div class="ss-plan-prop-thread-note"></div>')
				.text('+ ' + extra + ' more wait type' + (extra !== 1 ? 's' : '') + ' - see Properties'));
		}
		return $frag;
	}

	/**
	 * Statement-level compiled-vs-runtime parameter rows. Same cap/no-cap split as the other
	 * multi-row sections. A parameter missing ParameterCompiledValue is a local variable (SQL Server
	 * cannot sniff those at compile time) rather than a real parameter - shown as "not sniffed" rather
	 * than blank, since a blank compiled value reads as a parsing gap rather than the real reason.
	 */
	function buildParameterRows(parameters, cap) {
		var shown = cap ? parameters.slice(0, cap) : parameters;
		var $frag = $();
		shown.forEach(function (pm) {
			var key = pm.column + (pm.dataType ? ' (' + pm.dataType + ')' : '');
			var val;
			if (pm.compiledValue === undefined) {
				val = 'not sniffed (local variable)';
			} else if (pm.runtimeValue === undefined || pm.runtimeValue === pm.compiledValue) {
				val = 'Compiled ' + pm.compiledValue;
			} else {
				val = 'Compiled ' + pm.compiledValue + ' → Runtime ' + pm.runtimeValue;
			}
			$frag = $frag.add($('<div class="ss-plan-prop-row"></div>')
				.append($('<span class="ss-plan-prop-key"></span>').text(key))
				.append($('<span class="ss-plan-prop-val"></span>').text(val)));
		});
		if (cap && parameters.length > cap) {
			var extra = parameters.length - cap;
			$frag = $frag.add($('<div class="ss-plan-prop-thread-note"></div>')
				.text('+ ' + extra + ' more parameter' + (extra !== 1 ? 's' : '') + ' - see Properties'));
		}
		return $frag;
	}

	function renderPropertiesInto(container, node) {
		var $c = $(container);
		$c.empty();
		if (!node) {
			$c.append($('<div class="ss-plan-prop-empty"></div>')
				.text('Click an operator in the plan to see all of its properties here.'));
			return;
		}

		var m = node.metrics || {}, p = node.props || {};

		$c.append($('<div class="ss-plan-prop-title"></div>').text(node.op));
		if (p.logicalOp && p.logicalOp !== node.op) {
			$c.append($('<div class="ss-plan-prop-subtitle"></div>').text(p.logicalOp));
		}

		var desc = operatorDescriptionFor(node);
		var joinNote = joinTypeNote(node);
		if (joinNote) desc = desc ? (desc + ' ' + joinNote) : joinNote;
		if (desc) $c.append($('<div class="ss-plan-prop-desc"></div>').text(desc));

		if (node.warnings && node.warnings.length) {
			var $w = propSection($c, 'Warnings');
			node.warnings.forEach(function (wn) {
				$w.append($('<div class="ss-plan-prop-warn"></div>').text('⚠ ' + warningTitle(wn)));
			});
		}

		var $misc = propSection($c, 'Misc');
		propRow($misc, 'Node ID',        p.nodeId);
		propRow($misc, 'Physical Op',    node.op);
		propRow($misc, 'Logical Op',     p.logicalOp);
		propRow($misc, 'Operator Cost',  fmtCost(m.nodeCost));
		propRow($misc, 'Cost %',         m.relativeCostPct === undefined ? undefined : fmtPercent(m.relativeCostPct));
		propRow($misc, 'Subtree Cost',   fmtCost(m.subtreeCost));
		propRow($misc, 'Parallel',       p.parallel ? 'True' : undefined);
		propRow($misc, 'Execution Mode', p.actualExecutionMode || p.estimatedExecutionMode);
		propRow($misc, 'Object',         p.objName);
		propRow($misc, 'Index',          p.indexName);
		propRow($misc, 'Index Kind',     p.indexKind);
		propRow($misc, 'Storage',        p.storage);
		propRow($misc, 'Alias',          p.alias);
		propRow($misc, 'Seek Predicate', p.seekPredicate);
		propRow($misc, 'Table Rows',     fmtXmlValue(m.tableCardinality));
		if (node.isStatement) {
			// <QueryTimeStats> - present on the detail (tooltip) panel already (buildDetailPanel's
			// "Query Elapsed"/"Query CPU"/"UDF Elapsed"/"UDF CPU" rows), but missing here.
			propRow($misc, 'Query Elapsed',         fmtDuration(m.elapsedMs));
			propRow($misc, 'Query CPU',             fmtDuration(m.cpuMs));
			propRow($misc, 'UDF Elapsed',           fmtDuration(m.udfElapsedMs));
			propRow($misc, 'UDF CPU',               fmtDuration(m.udfCpuMs));
			propRow($misc, 'Degree of Parallelism', p.degreeOfParallelism);
			propRow($misc, 'Total Threads Used', m.threadsUsed === undefined ? undefined :
				fmtNum(m.threadsUsed) + (m.threadBranches > 1
					? ' (' + m.threadBranches + ' branches × DOP ' + (p.degreeOfParallelism || '?') + ')' : ''));
			propRow($misc, 'Memory Grant',          m.memoryGrantKb === undefined ? undefined : fmtNum(m.memoryGrantKb) + ' KB');
			propRow($misc, 'Cached Plan Size',      m.cachedPlanSizeKb === undefined ? undefined : fmtNum(m.cachedPlanSizeKb) + ' KB');
			propRow($misc, 'Compile Time',          fmtDuration(m.compileTimeMs));
			propRow($misc, 'Compile CPU',           fmtDuration(m.compileCpuMs));
			propRow($misc, 'Compile Memory',        m.compileMemoryKb === undefined ? undefined : fmtNum(m.compileMemoryKb) + ' KB');
			propRow($misc, 'Non-Parallel Reason',   p.nonParallelPlanReason);
		}

		// Own section, placed before Output List - moved out of the Misc row list above, which is
		// where it used to live and where it was easy to overlook: reported directly as "buried in
		// All plan XML for this operator" (it was already extracted into props.predicate and shown as
		// a Misc row, just not prominently). A predicate is often a long expression; giving it a full
		// line of its own to wrap on, the same treatment Output List already gets below, makes it
		// actually readable instead of fighting the key/value row's narrow value column.
		// Hash Match's own join key columns - already parsed (parseBodyProps()) into
		// props.hashKeysBuild/hashKeysProbe, alias-qualified when the plan itself qualifies them, but
		// never shown anywhere before now. Same "own section before Predicate" treatment - useful for
		// exactly the same reason (spotting index candidates from which columns a side hashes on).
		if (p.hashKeysBuild) {
			var $hkBuild = propSection($c, 'Hash Build Keys');
			$hkBuild.append($('<div class="ss-plan-prop-row"></div>').text(p.hashKeysBuild));
		}
		if (p.hashKeysProbe) {
			var $hkProbe = propSection($c, 'Hash Probe Keys');
			$hkProbe.append($('<div class="ss-plan-prop-row"></div>').text(p.hashKeysProbe));
		}
		if (p.predicate) {
			var $pred = propSection($c, 'Predicate');
			$pred.append($('<div class="ss-plan-prop-row"></div>').text(p.predicate));
		}

		// Own section rather than another Misc row - html-query-plan's tooltip gives Output List the
		// same treatment (its own "Output List" heading, one column per line), and a wide SELECT's
		// column list is long enough that burying it in a single wrapped Misc value would be hard to
		// scan. The detail (hover/pinned) panel shows a capped excerpt of this same list; this is
		// where the rest of it lives.
		if (p.outputListCols && p.outputListCols.length) {
			var $out = propSection($c, 'Output List (' + p.outputListCols.length + ')');
			p.outputListCols.forEach(function (col) {
				$out.append($('<div class="ss-plan-prop-row"></div>').text(col));
			});
		}

		// Own section, same treatment as Predicate/Output List above - "how many rows did each thread
		// actually produce, and how skewed is that" was previously answerable only by expanding the
		// raw XML tree, even though the Skewed Parallelism Plan Analysis finding already judges
		// exactly this number. Only shown when this operator genuinely ran with more than one thread
		// - a serial operator has nothing to distribute. @Thread is not reliably in document order
		// (measured: 159 of 171 multi-thread operators in the reference corpus had it out of order),
		// so parseRunTimeInfo() already sorted this numerically before it got here.
		if (m.perThread && m.perThread.length > 1) {
			var $par = propSection($c, 'Parallel Threads (' + m.perThread.length + ')');
			$par.append(buildThreadRows(m, 0));
		}

		// Statement-level wait types accumulated during execution - only present on an actual plan
		// captured on SQL Server 2016 SP1+. Same "own section" treatment as everything else here;
		// previously only reachable via "All plan XML for this operator".
		if (m.waitStats && m.waitStats.length) {
			var $waits = propSection($c, 'Wait Statistics (' + m.waitStats.length + ')');
			$waits.append(buildWaitRows(m.waitStats, 0));
		}

		// Compiled-vs-runtime value for every parameter and local variable - the reference view for
		// everything Parameter Sniffing / Local Variables findings look at, not just the ones flagged.
		if (p.parameters && p.parameters.length) {
			var $params = propSection($c, 'Parameters (' + p.parameters.length + ')');
			$params.append(buildParameterRows(p.parameters, 0));
		}

		PROP_GROUPS.forEach(function (grp) {
			var $body = null;
			grp.keys.forEach(function (k) {
				var v = (m[k] !== undefined) ? m[k] : p[k];
				// The parsed model stores the well-known estimates under friendlier names; fall back to
				// reading the raw attribute so a group row is never blank just because of the rename.
				if (v === undefined && node._xmlEl) v = attr(node._xmlEl, k);
				if (v === undefined || v === null || v === '') return;
				if (!$body) $body = propSection($c, grp.title);
				propRow($body, k, fmtXmlValue(v));
			});
		});

		// The raw element tree - this is what makes "all the rest, like SSMS" complete without
		// enumerating anything: whatever the plan carries that the curated sections above did not pick
		// up is still here, verbatim.
		if (node._xmlEl) {
			var $raw = propSection($c, 'All plan XML for this operator');
			renderXmlTree($raw, node._xmlEl, 0);
		}

		// DDL Info - last, after the raw XML. Shown whenever this operator names a table at all, not
		// only when the lookup actually found something, so "we tried and it wasn't there" and "there
		// is no DDL context for this view" are visible facts rather than a silently missing section
		// indistinguishable from "this operator has no table". _tableInfoPending's own presence (not
		// just its value) is what tells the three states apart: loadTableInfoAsync() never touches it
		// at all when there is no srv/dbname (the standalone paste page with nothing filled in), sets
		// it true the moment a lookup starts, then false once that table's response has landed -
		// _tableInfo can end up undefined in that last state too (a table simply absent from the
		// response), so it alone cannot distinguish "resolved, not found" from "never asked".
		if (p.objName) {
			var $ddl = propSection($c, 'DDL Info');
			if (node._tableInfoPending === undefined) {
				propRow($ddl, 'Table Info', 'No DDL Storage context (open this plan with a server and database to see size/index info)');
			} else if (node._tableInfoPending) {
				propRow($ddl, 'Table Info', '⏳ Loading…');
			} else {
				var info = node._tableInfo;
				if (!info || info.found === false) {
					propRow($ddl, 'Table Info', 'Not found in DDL Storage');
				} else {
					propRow($ddl, 'Table',        info.tableName);
					propRow($ddl, 'DDL sampled',  (info.sampleTime || '').replace(/\.\d+$/, ''));
					propRow($ddl, 'Rows',         fmtNum(info.rowTotal));
					propRow($ddl, 'Total MB',     fmtNum(info.totalMb));
					propRow($ddl, 'In-Row MB',    fmtNum(info.inRowMb));
					propRow($ddl, 'LOB MB',       info.lobMb > 0 ? fmtNum(info.lobMb) : undefined);
					propRow($ddl, 'Index count',  info.indexCount);
					(info.indexes || []).forEach(function (idx) {
						var cols = (idx.keys && idx.keys.length) ? idx.keys.join(', ') : '-';
						if (idx.includeCols && idx.includeCols.length) cols += ' INCLUDE (' + idx.includeCols.join(', ') + ')';
						var extra = [];
						if (idx.lastUpdateStats) extra.push('stats ' + idx.lastUpdateStats.replace(/\.\d+$/, ''));
						if (idx.userSeeks >= 0)  extra.push('seeks ' + fmtNum(idx.userSeeks));
						if (idx.userScans >= 0)  extra.push('scans ' + fmtNum(idx.userScans));
						propRow($ddl, (idx.indexName || '(heap)'),
							fmtNum(idx.sizeMb) + ' MB (' + cols + ')' + (extra.length ? ' - ' + extra.join(', ') : ''));
					});
				}
			}
		}
	}

	// ─────────────────────────────────────────────────────────────────────────
	// Box rendering
	// ─────────────────────────────────────────────────────────────────────────

	// Set by render() so a box click can push the selected node into the pane without every box
	// needing a reference to it.
	var _propsTarget = null;
	var _selectedBox = null;

	function selectNode($box, node) {
		if (_selectedBox) _selectedBox.removeClass('ss-plan-selected');
		_selectedBox = $box;
		$box.addClass('ss-plan-selected');
		if (_propsTarget) renderPropertiesInto(_propsTarget, node);
	}

	function renderNode(node) {
		var $li  = $('<li></li>');
		var m    = node.metrics || {}, p = node.props || {};
		// Suppressed on a branch that never ran: "estimated N rows, produced 0" is not a misestimate
		// there, it is just an untaken branch. Without this a dead box keeps the amber warn tint and
		// its orange Est/Act percentage - exactly the attention the hatching exists to take away - and
		// the box would contradict the Cardinality Estimate finding, which is suppressed on the same
		// condition in collectTreeFindings().
		var warn = isEstActWarn(m) && !node._neverExecuted;
		var $box = $('<div class="ss-plan-box"></div>');
		if (warn) $box.addClass('ss-plan-warn');
		if (node.isStatement) $box.addClass('ss-plan-statement');
		if (node.warnings && node.warnings.length) $box.addClass('ss-plan-has-warning');
		// Synchronous (unlike ss-plan-big-table, which needs an async DDL-Storage lookup) - the
		// PhysicalOp/LogicalOp needed to detect this are already known at render time.
		var eagerSpool = isEagerIndexSpool(node);
		if (eagerSpool) $box.addClass('ss-plan-eager-spool');
		// Marked at parse time (markNeverExecuted), so this is just a lookup. Applied last of the box
		// states so the hatching's background-image sits on top of ss-plan-big-table's flat fill.
		if (node._neverExecuted) $box.addClass('ss-plan-never-exec');
		// Kept on the node itself (rather than a separate id -> box map) so the async table-info
		// lookup fired from render() can reach back into the live DOM for this exact node.
		node._$box = $box;
		// ...and the reverse link, so drawConnectorLines() - which walks the rendered DOM, not the
		// model - can get back to the node a box was built from. It needs the row counts to size and
		// label each connector.
		$box[0].__ssPlanNode = node;

		if (p.nodeId !== undefined && p.nodeId !== null && p.nodeId !== '') {
			// Looked up by window.ssShowplanJumpToNode() (dbxShowplan.js) when a Plan Analysis
			// finding's "[Node N]" tag is clicked, and by reorderCompactByNodeId() for sibling order.
			$box.attr('data-nodeid', p.nodeId);
			$box.append($('<div class="ss-plan-nodeid" title="Node ID"></div>').text(p.nodeId));
		}

		// Top-left corner badge, overlapping the border like the parallel chip does at bottom-right -
		// pulled out of the icon row so a warning reads as an alert on the box itself, not just another
		// small glyph among neutral operator-property icons (batch mode, join type, DDL info).
		if (node.warnings && node.warnings.length) {
			// Plain "!" rather than the ⚠ emoji glyph - the emoji's visible triangle mark sits
			// asymmetrically within its own character box in most fonts (particularly Windows'
			// Segoe UI Emoji), so no amount of flex/line-height centering on the CONTAINER can center
			// it - the glyph itself is off-center. A plain "!" has symmetric ink bounds everywhere.
			$box.append($('<div class="ss-plan-corner-warn"></div>')
				.attr('title', node.warnings.map(warningTitle).join('\n')).text('!'));
		}

		// EXPERIMENTAL - two visual treatments for "how many threads actually ran here", side by side
		// for comparison: a "stack of copies" look + chip, and a worker count appended to the existing
		// "∥" badge. m.parWorkers (applyParallelWorkerCounts()) excludes the coordinator thread and
		// exchange/boundary operators (e.g. Gather Streams) that are Parallel="true" but themselves
		// ran on one thread - so only genuinely multi-threaded operators get the stack.
		//
		// Only the chip is built here. The "stack of copies" sheets are NOT children of $box - see
		// drawParallelStacks(), called from the same late layout pass as drawConnectorLines(). They
		// were children of $box at first (offset + negative z-index, so they'd tuck behind the box's
		// own content), which worked in a simple linear chain but not in the compact/branching table
		// layout: an adjacent operator's box sits close enough that ITS OWN opaque background covers
		// the few px meant to peek out, since z-index only orders content WITHIN $box's own stacking
		// context - it says nothing about $box's whole subtree versus an unrelated sibling box outside
		// it. Reproduced on HashSpillDetails.sqlplan's Hash Match (fed by two parallel branches) before
		// concluding this and switching approach, matching what the user saw live on Merge Join.
		if (m.parWorkers > 0) {
			$box.append($('<div class="ss-plan-parallel-chip"></div>')
				.attr('title', m.parWorkers + ' worker threads').text(m.parWorkers));
		}

		var $iconRow = $('<div class="ss-plan-icon-row"></div>');
		// Stashed so the async DDL-info lookup below can append its found/missing icon into this same
		// row once it resolves (node._$box is the whole box, not specifically the icon row).
		node._$iconRow = $iconRow;
		$iconRow.append($('<div class="ss-plan-icon"></div>').attr('style', iconStyleFor(node)));
		var joinIcon = joinTypeIconFor(node);
		if (joinIcon) {
			$iconRow.append($('<div class="ss-plan-jointype-icon"></div>')
				.attr('style', 'background-image:url(/images/' + joinIcon + ');')
				.attr('title', p.logicalOp));
		}
		if ((p.actualExecutionMode || p.estimatedExecutionMode) === 'Batch') {
			// Same sprite cell html-query-plan's .qp-iconbatch uses (qp.css), for visual parity - no
			// new asset needed since this file already loads the same qp_icons.png sprite.
			$iconRow.append($('<div class="ss-plan-icon-badge"></div>')
				.attr('title', 'Batch mode execution')
				.attr('style', 'background-image:url(' + ICON_SPRITE_URL + ');background-position:-288px -192px;'));
		}
		$box.append($iconRow);

		$box.append($('<div class="ss-plan-label"></div>').text(node.op));

		// Most operators have PhysicalOp === LogicalOp (e.g. "Clustered Index Scan"/"Clustered Index
		// Scan"), so this row only appears when they genuinely differ (e.g. Nested Loops / Inner Join,
		// or Index Spool / Eager Spool) - it's what the operator is actually DOING, one level more
		// specific than its physical implementation.
		if (p.logicalOp && p.logicalOp !== node.op) {
			$box.append($('<div class="ss-plan-logicalop"></div>').text(p.logicalOp));
		}

		var subtitle = subtitleFor(node);
		if (subtitle) {
			$box.append($('<div class="ss-plan-subtitle"></div>').attr('title', subtitle).text(subtitle));
		}

		// Relative cost is the single number SSMS puts under every operator, and the one users scan
		// for first when hunting the expensive part of a plan - so it goes on the box, not just in a
		// panel. Bolded once an operator accounts for a large share of the whole plan.
		//
		// Suppressed on the statement node, matching qp.xslt (which has an empty NodeCostLabel
		// template for s:StmtSimple|s:StmtUseDb) - a statement is not one of the operators the
		// percentages are shared out between, so showing it a cost would be misleading.
		if (m.relativeCostPct !== undefined && !node.isStatement) {
			var $cost = $('<div class="ss-plan-metric ss-plan-cost"></div>')
				.text('Cost: ' + fmtPercent(m.relativeCostPct));
			if (m.relativeCostPct >= 25) $cost.addClass('ss-plan-cost-high');
			$box.append($cost);
		}

		var est = fmtNum(m.estRows), act = fmtNum(m.actRows);
		if (est !== undefined || act !== undefined) {
			var pct    = fmtEstActDiff(m);
			var estTxt = 'Est ' + (est === undefined ? '?' : est);
			// An estimated-only plan has no actual counts at all - saying "Act ?" on every single box
			// would be noise, so only the estimate is shown in that case.
			var actTxt = (act === undefined) ? undefined : 'Act ' + act;

			// One line normally; two once the combined text gets long. Row counts in the millions
			// ("Est 106,535,000 / Act 108,910,386 (102.2%)") stretched a box to ~199px against a
			// ~120px natural width, which spreads the whole diagram out. Measured across the
			// reference corpus: 47 boxes carry metric text wider than 150px, and none of them need
			// to. The threshold is on the single-line length so small plans keep the compact form.
			var oneLine = estTxt + (actTxt ? ' / ' + actTxt : '') + (pct !== undefined ? ' (' + pct + ')' : '');
			var split   = actTxt !== undefined && oneLine.length > 30;

			function pctSpan() {
				var $pct = $('<span class="ss-plan-metric-pct"></span>').text(' (' + pct + ')');
				if (warn) $pct.addClass('ss-plan-metric-pct-warn');
				return $pct;
			}

			if (split) {
				var $estLine = $('<div class="ss-plan-metric"></div>').text(estTxt);
				var $actLine = $('<div class="ss-plan-metric"></div>').text(actTxt);
				if (warn) { $estLine.addClass('ss-plan-warn-text'); $actLine.addClass('ss-plan-warn-text'); }
				// The percentage belongs with the actual count it compares against.
				if (pct !== undefined) $actLine.append(pctSpan());
				$box.append($estLine).append($actLine);
			} else {
				var $metric = $('<div class="ss-plan-metric"></div>');
				if (warn) $metric.addClass('ss-plan-warn-text');
				$metric.text(estTxt + (actTxt ? ' / ' + actTxt : ''));
				if (pct !== undefined) $metric.append(pctSpan());
				$box.append($metric);
			}
		}

		// Separate from the Est/Act line above: how much of what flowed INTO this node it discarded,
		// not what it itself output - see inputRowReductionPercent() for why those aren't the same
		// thing. Only shown once it discards a clear majority (>=50%) so ordinary pass-through nodes
		// stay uncluttered, and in a calm blue rather than the Est/Act line's red/orange, since heavy
		// filtering is normal, expected behaviour for a Filter - not a warning sign.
		var reduction = inputRowReductionPercent(node);
		if (reduction !== undefined && reduction >= 50) {
			$box.append($('<div class="ss-plan-metric ss-plan-metric-filter"></div>')
				.text('↓ ' + fmtReductionPct(reduction, m.actRows) + '% of input rows '
					+ rowReductionVerb(node)));
		}

		// Runtime timings, on the box rather than only in the detail panel - html-query-plan shows
		// these and they are one of the first things looked at on an actual plan. Same numbers and
		// same seconds-to-3-decimals formatting as qp.xslt's NodeTimeLabel templates:
		//   - operators: MAX(ActualElapsedms) across threads (already aggregated as a max in
		//     parseRunTimeInfo, since threads run concurrently)
		//   - statement node: the query totals from <QueryTimeStats>, which appear nowhere else
		if (node.isStatement) {
			if (m.elapsedMs !== undefined) $box.append(timeLine('Time: ' + fmtDuration(m.elapsedMs)));
			if (m.cpuMs     !== undefined) $box.append(timeLine('CPU Time: ' + fmtDuration(m.cpuMs)));
			if (m.udfElapsedMs !== undefined) $box.append(timeLine('UDF Time: ' + fmtDuration(m.udfElapsedMs)));
			if (m.udfCpuMs     !== undefined) $box.append(timeLine('UDF CPU Time: ' + fmtDuration(m.udfCpuMs)));
		} else if (m.ActualElapsedms !== undefined) {
			$box.append(timeLine('Time: ' + fmtDuration(m.ActualElapsedms)));
		}

		// Last on the box, matching where ss-plan-big-table's "Large table" warning ends up (it's
		// appended asynchronously in loadTableInfoAsync(), after everything from initial render).
		if (eagerSpool) {
			$box.append($('<div class="ss-plan-metric ss-plan-tablesize-warn"></div>').text('⚠ Missing Index (Eager Spool)'));
		}

		// The hatching alone says "different", not "never ran" - so the box also carries the reason in
		// words. Last on the box, after every metric, because it is a verdict about the whole operator
		// rather than another measurement of it.
		if (node._neverExecuted) {
			$box.append($('<div class="ss-plan-never-exec-note"></div>').text('never executed'));
		}

		// Hover shows a transient summary tooltip; click pins it AND pushes the node to the
		// Properties pane. The two never stack - see the ASE renderer for the same interaction.
		$box.on('mouseenter', function () {
			// Never replace a pinned panel with a transient one - the pinned panel is the user's
			// deliberate choice and its text is selectable.
			if (panelFor($box[0])) return;
			openDetailPanel(node, $box[0], false);
		});
		$box.on('mouseleave', function () {
			if (!isPinned($box[0])) closePanel($box[0]);
		});
		$box.on('click', function (e) {
			e.stopPropagation();
			selectNode($box, node);
			// Toggle: a second click on a box whose panel is already pinned closes it.
			if (isPinned($box[0])) { closePanel($box[0]); return; }
			openDetailPanel(node, $box[0], true);
		});

		// See the .ss-plan-box-cell comment in the CSS block: an invisible wrapper that absorbs the
		// table-cell stretch in compact mode so the visible box keeps its natural size.
		$li.append($('<div class="ss-plan-box-cell"></div>').append($box));

		if (node.children && node.children.length) {
			var $childUl = $('<ul></ul>');
			node.children.forEach(function (child) { $childUl.append(renderNode(child)); });
			$li.append($childUl);
		}
		return $li;
	}
	// ─────────────────────────────────────────────────────────────────────────
	// Plan Analysis findings
	//
	// Finding shape is identical to dbxShowplanAnalyzer.js's and the ASE renderer's, so
	// dbxShowplan.js's shared renderFindingsListHtml() renders all three unchanged:
	//   { severity, category, title, detail, nodeId, nodeName }
	//
	// dbxShowplanAnalyzer.js already analyses the same XML (missing indexes, implicit conversions,
	// plan-level warnings, ...) and keeps doing so - this adds only the findings that need the parsed
	// TREE rather than the raw XML, which is exactly what that analyzer cannot see.
	// ─────────────────────────────────────────────────────────────────────────

	function collectTreeFindings(stepRoots) {
		var findings = [];
		stepRoots.forEach(function (root) {
			walkPlanNodes(root, function (n) {
				var m = n.metrics || {}, p = n.props || {};

				// Cardinality misestimate. Only meaningful on an actual plan (an estimated-only plan has
				// no actual rows to compare against), and only worth reporting once the row counts are
				// big enough that being 10x out could actually change the plan shape. Excludes the
				// statement pseudo-node - buildStatementNode() copies estRows/actRows from the plan's
				// root RelOp onto the statement node too (so the statement box can show them), which
				// means a single-operator statement would otherwise report the exact same mismatch
				// twice: once as "SELECT" and once as the actual operator underneath it.
				// _neverExecuted branches are excluded because "estimated N rows, produced none" is not a
				// misestimate there - the branch was simply never taken (an adaptive join's unchosen side,
				// a startup Filter, an unreached Concatenation input). The corpus has exactly one such
				// finding today (adaptive_join.sqlplan node 4, at 'error' severity) and it is wrong.
				if (!n.isStatement && !n._neverExecuted && isEstActWarn(m)
				 && (m.actRows >= 100 || m.estRows >= 100)) {
					var diff = fmtEstActDiff(m);
					var diffTitle = diff === 'zero-rows'
						? 'Estimated ' + fmtNum(m.estRows) + ' rows, but produced none'
						: 'Estimated vs actual rows off by ' + (diff === 'huge-diff' ? 'a huge margin' : diff);
					findings.push({
						severity: (m.actRows > m.estRows * 100 || m.actRows * 100 < m.estRows) ? 'error' : 'warning',
						category: 'Cardinality Estimate',
						title:    diffTitle,
						detail:   n.op + (p.objName ? ' on ' + p.objName : '') + ' estimated ' + fmtNum(m.estRows)
						          + ' rows but produced ' + fmtNum(m.actRows) + '. A wrong estimate here can lead the '
						          + 'optimizer to pick the wrong join type, join order or memory grant.',
						nodeId:   p.nodeId,
						nodeName: n.op
					});
				}

				// Per-operator warnings the plan itself raised.
				(n.warnings || []).forEach(function (w) {
					findings.push({
						severity: /Spill|NoJoinPredicate/i.test(w.type) ? 'error' : 'warning',
						category: 'Plan Warning',
						title:    w.type.replace(/([a-z])([A-Z])/g, '$1 $2'),
						detail:   warningTitle(w) + ' (' + n.op + (p.objName ? ' on ' + p.objName : '') + ')',
						nodeId:   p.nodeId,
						nodeName: n.op
					});
				});

				// A lookup executed many times is the classic "non-covering index" pattern.
				//
				// Detected on the IndexScan/@Lookup flag rather than on PhysicalOp: SSMS DISPLAYS these as
				// "Key Lookup", but ShowPlanXML encodes a lookup into a clustered table as
				// PhysicalOp="Clustered Index Seek" with Lookup="true" - only a heap lookup actually gets
				// PhysicalOp="RID Lookup". Matching the op name therefore missed the common case entirely
				// (the corpus's own KeyLookup.sqlplan, 858 executions, went unreported), and the 1000
				// threshold then missed it a second time.
				if (isLookup(p) && m.ActualExecutions >= 100) {
					findings.push({
						severity: 'warning',
						category: 'Lookup',
						title:    'Lookup executed ' + fmtNum(m.ActualExecutions) + ' times',
						detail:   'Each execution fetches one row from ' + (p.objName || 'the base table')
						          + ' because the non-clustered index did not cover the query. Adding the missing '
						          + 'columns as INCLUDE columns on index ' + (p.indexName || '(the seek index)')
						          + ' would remove this lookup.',
						nodeId:   p.nodeId,
						nodeName: n.op
					});
				}

				// Executed far more often than the optimizer expected.
				//
				// A raw "executed many times" threshold would be wrong: 39,553 executions of an Index Seek
				// is entirely normal for the inner side of a nested loop and says nothing by itself. What
				// is actionable is the MISMATCH - the optimizer sized the loop from EstimateRebinds, and
				// if the real count is orders of magnitude higher then the outer input's cardinality
				// estimate is wrong, which is what drove the loop count. Self-calibrating, so no magic
				// "too many" number: +1 covers the first execution, which is never a rebind.
				var expectedExec = (m.estRebinds || 0) + (m.estRewinds || 0) + 1;
				if (!n.isStatement && !n._neverExecuted && m.ActualExecutions >= 100
				 && m.ActualExecutions > expectedExec * 10) {
					findings.push({
						severity: 'warning',
						category: 'Execution Count',
						title:    'Executed ' + fmtNum(m.ActualExecutions) + ' times, optimizer expected about '
						          + fmtNum(Math.round(expectedExec)),
						detail:   n.op + (p.objName ? ' on ' + p.objName : '') + ' ran '
						          + fmtNum(m.ActualExecutions) + ' times but was costed for roughly '
						          + fmtNum(Math.round(expectedExec)) + '. On the inner side of a nested loop the '
						          + 'execution count is driven by the row count of the OUTER input, so this usually '
						          + 'means the estimate feeding the join is too low - the operator itself may be '
						          + 'perfectly good. Check the statistics on the outer input, and whether a join '
						          + 'strategy other than nested loops would suit the real row counts.',
						nodeId:   p.nodeId,
						nodeName: n.op
					});
				}

				// An Eager Index Spool means the engine built an index on the fly, per execution.
				if (isEagerIndexSpool(n)) {
					findings.push({
						severity: 'error',
						category: 'Index Spool',
						title:    'Eager Index Spool - an index is being built at runtime',
						detail:   'SQL Server is building a temporary index in tempdb because no suitable index '
						          + 'exists. Creating a permanent index on the underlying table usually removes this '
						          + 'entirely and is often the single biggest win available in a plan.',
						nodeId:   p.nodeId,
						nodeName: n.op
					});
				}
			});
		});
		return findings;
	}

	// ─────────────────────────────────────────────────────────────────────────
	// DDL Storage lookup — same batched, fire-after-draw approach as the ASE renderer.
	// ─────────────────────────────────────────────────────────────────────────

	/**
	 * What this operator reads in full, if that is a bounded, size-checkable structure - or null if
	 * it is not. A Seek only touches the rows it needs, so warning on its table's size would be pure
	 * noise; only full scans are compared against the threshold.
	 *
	 * Table Scan (a heap) and Clustered Index Scan both read the table's own data pages, so both are
	 * compared against inRowMb. A non-clustered Index Scan reads one specific index's leaf level
	 * instead, looked up by name in info.indexes[].
	 */
	function scannedSizeInfo(node, info) {
		if (!info || info.found === false) return null;
		var p = node.props || {};
		if (!/Scan$/i.test(node.op || '')) return null;
		if (/Constant Scan|Deleted Scan|Inserted Scan|Log Row Scan/i.test(node.op)) return null;

		if (/Table Scan|Clustered Index Scan|Columnstore Index Scan/i.test(node.op)) {
			return info.inRowMb === undefined ? null : { mb: info.inRowMb, what: 'table' };
		}
		if (/Index Scan/i.test(node.op) && p.indexName && info.indexes) {
			for (var i = 0; i < info.indexes.length; i++) {
				if (info.indexes[i].indexName
					&& info.indexes[i].indexName.toLowerCase() === p.indexName.toLowerCase()) {
					return { mb: info.indexes[i].sizeMb, what: 'index' };
				}
			}
		}
		return null;
	}

	function loadTableInfoAsync(roots, srv, dbname, tableSizeWarnMb, onBoxesChanged, findings, onFindingsChanged) {
		// The standalone paste-a-plan page has no live server context, so there is nothing to look up.
		if (!srv || !dbname) return;

		var nodesByTable = {};
		roots.forEach(function (root) {
			walkPlanNodes(root, function (n) {
				var name = n.props && n.props.objName;
				if (!name) return;
				(nodesByTable[name] = nodesByTable[name] || []).push(n);
				n._tableInfoPending = true;
			});
		});
		var tables = Object.keys(nodesByTable);
		if (!tables.length) return;

		$.ajax({
			url:      '/api/cc/mgt/table-info',
			data:     { srv: srv, dbVendor: 'Microsoft SQL Server', dbname: dbname,
			            tables: tables.join(','), format: 'json' },
			dataType: 'json'
		}).done(function (r) {
			var byTable = (r && r.tables) || {};
			var anyBoxGrew = false;

			tables.forEach(function (name) {
				var info = byTable[name];
				nodesByTable[name].forEach(function (n) {
					n._tableInfoPending = false;
					n._tableInfo = info;

					// Found/missing icon in the icon row, for every operator that a lookup was actually
					// attempted for (has objName) - regardless of whether it turned out big enough to
					// also get the ss-plan-big-table marking below. Per the user's choice, nothing is
					// shown for the "pending" interval before this callback fires.
					if (n._$iconRow) {
						var dInfoFound = !!(info && info.found);
						var $ddlIcon = $('<div class="ss-plan-icon-badge ss-plan-ddlinfo-icon"></div>')
							.attr('title', dInfoFound ? 'DDL/Table info available' : 'DDL/Table info not found in DbxTune\'s DDL Storage');
						if (!dInfoFound) $ddlIcon.addClass('ss-plan-ddlinfo-missing');
						n._$iconRow.append($ddlIcon);
						anyBoxGrew = true;
					}

					if (!info || info.found === false) return;

					var scanned = scannedSizeInfo(n, info);
					if (scanned && scanned.mb !== undefined) {
						var isBigScan = scanned.mb > tableSizeWarnMb;
						if (isBigScan && n._$box) {
							n._$box.addClass('ss-plan-big-table');
							n._$box.append($('<div class="ss-plan-metric ss-plan-tablesize-warn"></div>')
								.text('⚠ Large ' + scanned.what + ' (' + fmtNum(scanned.mb) + ' MB)'));
							anyBoxGrew = true;
						}
						if (findings) {
							// category:'Scan' - deliberately the SAME category dbxShowplanAnalyzer.js's
							// own "[Scan] ... consider adding an index" findings use (a different
							// module, computed synchronously before this DDL data existed - there is no
							// direct handle back into its already-built finding text to append to). A
							// shared category is what groups this into the same collapsible section in
							// Plan Analysis, so "how big is the thing this scan reads" sits right next
							// to "here is why the scan might be a problem" rather than a separate group.
							// Pushed whenever the size is known at all, not only once it crosses the
							// "Big table" threshold (that case gets 'warning' + the box marking above;
							// staying under it is still worth knowing, just as 'info').
							findings.push({
								severity: isBigScan ? 'warning' : 'info',
								category: 'Scan',
								title:    (isBigScan ? 'Full scan of a ' : '') + fmtNum(scanned.mb) + ' MB '
								            + scanned.what + (isBigScan ? '' : ' scanned'),
								detail:   n.op + ' reads all of ' + name + (scanned.what === 'index'
								            ? ' index ' + n.props.indexName : '')
								          + ' (' + fmtNum(scanned.mb) + ' MB, ' + fmtNum(info.rowTotal)
								          + ' rows per DDL Storage).'
								          + (isBigScan ? ' An index supporting a seek would avoid reading it all.' : ''),
								nodeId:   n.props.nodeId,
								nodeName: n.op
							});
						}
					}
				});
			});

			// A pinned/hovered panel that was open before the data landed would otherwise keep showing
			// "Loading…" until the user closed and reopened it.
			roots.forEach(function (root) {
				walkPlanNodes(root, function (n) {
					if (!n._$box) return;
					var open = panelFor(n._$box[0]);
					if (open) openDetailPanel(n, n._$box[0], open.pinned);
					if (_selectedBox && _selectedBox[0] === n._$box[0] && _propsTarget) {
						renderPropertiesInto(_propsTarget, n);
					}
				});
			});

			if (findings && onFindingsChanged) onFindingsChanged(findings);
			// A grown box invalidates the tuck/connector positions measured before the fetch returned.
			if (anyBoxGrew && onBoxesChanged) onBoxesChanged();
		}).fail(function () {
			roots.forEach(function (root) {
				walkPlanNodes(root, function (n) { n._tableInfoPending = false; });
			});
		});
	}

	function stepRootsOf(parsed) {
		var roots = [];
		(parsed && parsed.statements || []).forEach(function (stmt) {
			stmt.steps.forEach(function (step) { roots.push(step.root); });
		});
		return roots;
	}

	/**
	 * Plan Analysis findings, independent of whether this diagram ever gets drawn on screen.
	 *
	 * Exists because Plan Analysis is a judgement about the PLAN, not about which of the two
	 * diagram libraries happens to be selected - but the tree-derived findings (cardinality
	 * mismatches, hot Key/RID Lookups, eager index spools, large scans once DDL Storage resolves)
	 * used to only run as a side effect of render() actually drawing boxes. When html-query-plan was
	 * the active renderer, render() was never called at all, so those findings were silently absent
	 * and the two renderers disagreed on the same plan's finding count - reported directly: "58 vs
	 * 80 findings" on the same XML, switching only the renderer.
	 *
	 * collectTreeFindings() and loadTableInfoAsync() both only touch the DOM through `node._$box`,
	 * and both already guard every use of it with `if (!n._$box) return` (loadTableInfoAsync's own
	 * comment: a grown box invalidates tuck/connector positions "already measured and drawn" - a
	 * no-op when nothing was ever drawn). So calling them against the parsed model alone, with no
	 * render() and no boxes, is exactly what those guards exist to make safe - confirmed by reading
	 * every _$box use, not assumed.
	 *
	 * opts: same shape as render()'s (srv, dbname, tableSizeWarnMb, onFindingsChanged). Returns the
	 * synchronous findings immediately; onFindingsChanged (if given) fires again once the async DDL
	 * Storage lookup resolves, same two-phase contract render() itself has.
	 */
	function collectFindings(parsed, opts) {
		opts = opts || {};
		if (!parsed || !parsed.statements || !parsed.statements.length) return [];
		var stepRoots = stepRootsOf(parsed);
		var findings = collectTreeFindings(stepRoots);
		if (opts.onFindingsChanged) opts.onFindingsChanged(findings);
		var tableSizeWarnMb = (typeof opts.tableSizeWarnMb === 'number' && opts.tableSizeWarnMb >= 0)
			? opts.tableSizeWarnMb : 100;
		loadTableInfoAsync(stepRoots, opts.srv, opts.dbname, tableSizeWarnMb,
			undefined /* no layout to redo - nothing was drawn */, findings, opts.onFindingsChanged);
		return findings;
	}

	// ─────────────────────────────────────────────────────────────────────────
	// render()
	// ─────────────────────────────────────────────────────────────────────────

	/**
	 * opts (all optional):
	 *   horizontal        left-to-right when true, top-to-bottom when false
	 *   srv, dbname       live server context, enables the DDL Storage lookup
	 *   tableSizeWarnMb   threshold for the "Large table/index" mark (default 100)
	 *   propsTarget       element to render the Properties pane into
	 *   onFindingsChanged callback receiving the findings array (called at least once)
	 *   onNodeSelected    callback receiving the node whose box was clicked
	 */
	function render(container, parsed, opts) {
		injectStyle();
		opts = opts || {};
		var $container = $(container);
		// Panels live on <body>, not inside the container, so emptying it would otherwise strand any
		// that were open - leaving a panel floating over the freshly drawn plan describing an
		// operator from the previous one.
		closeAllPanels(false);
		$container.empty();
		if (!parsed || !parsed.statements || !parsed.statements.length) return;

		var horizontal = !!opts.horizontal;
		_propsTarget = opts.propsTarget || null;
		_selectedBox = null;
		if (_propsTarget) renderPropertiesInto(_propsTarget, null);

		var $wrap = $('<div class="ss-plan-wrap"></div>');
		var multiStatement = parsed.statements.length > 1;
		var treesForLineDrawing = [];
		var treesForCompactTuck = [];
		var stepRoots = [];

		parsed.statements.forEach(function (stmt, idx) {
			if (multiStatement && stmt.label) {
				var $hdr = $('<div class="ss-plan-stmt-hdr"></div>')
					.text('Query ' + (idx + 1) + ': ' + stmt.label);
				var cost = stmt.meta && stmt.meta.StatementSubTreeCost;
				if (cost !== undefined) {
					$hdr.append($('<span class="ss-plan-stmt-cost"></span>').text('  (cost ' + cost + ')'));
				}
				$wrap.append($hdr);
			}
			stmt.steps.forEach(function (step) {
				var $tree = $('<div class="ss-plan-tree"></div>');
				// Compact (nested-table) layout and SVG connectors are always on - the org-chart CSS
				// modes the ASE renderer still carries were superseded there too, and a SQL Server plan
				// (deep left-leaning join chains) is the exact shape compact mode exists to fix.
				$tree.addClass(horizontal ? 'ss-plan-compact-h' : 'ss-plan-compact-v');
				$tree.addClass('ss-plan-lines');
				treesForLineDrawing.push($tree);
				treesForCompactTuck.push($tree);

				var $rootUl = $('<ul></ul>');
				$rootUl.append(renderNode(step.root));
				$tree.append($rootUl);
				reorderCompactByNodeId($tree[0]);
				$wrap.append($tree);
				stepRoots.push(step.root);
			});
		});

		// Close open detail panels when clicking anywhere else in the diagram.
		$wrap.on('click', function () { closeAllPanels(false); });

		$container.append($wrap);

		var planFindings = collectTreeFindings(stepRoots);
		if (opts.onFindingsChanged) opts.onFindingsChanged(planFindings);

		// Both passes measure with getBoundingClientRect(), which reports zero for detached elements -
		// so both must run after $container.append($wrap) above. Tucking must precede connector
		// drawing so the arrows reflect final positions. Both are idempotent, so this can safely run
		// again if the async lookup below grows a box.
		function layoutTuckAndConnectors() {
			var tuckFn = horizontal ? tuckLeavesNearParent : tuckLeavesNearParentVertical;
			treesForCompactTuck.forEach(function ($tree) { tuckFn($tree[0]); });
			treesForLineDrawing.forEach(function ($tree) { drawConnectorLines($tree[0], horizontal); });
			treesForLineDrawing.forEach(function ($tree) { drawParallelStacks($tree[0]); });
		}
		layoutTuckAndConnectors();

		var tableSizeWarnMb = (typeof opts.tableSizeWarnMb === 'number' && opts.tableSizeWarnMb >= 0)
			? opts.tableSizeWarnMb : 100;
		loadTableInfoAsync(stepRoots, opts.srv, opts.dbname, tableSizeWarnMb,
			layoutTuckAndConnectors, planFindings, opts.onFindingsChanged);
	}

	return {
		parseXml:              parseXml,
		render:                render,
		collectFindings:       collectFindings,
		renderPropertiesInto:  renderPropertiesInto,
		// Detail panels are attached to <body> to escape the diagram's scroll clipping, so a caller
		// that tears the diagram down (closing the dialog, switching to the other renderer) needs a
		// way to dismiss them - they are not removed by emptying the container.
		closePanels:           function () { closeAllPanels(false); },
		anyPanelsOpen:         function () { return _panels.anyOpen(); },
		getLastParseError:     function () { return lastParseError; }
	};
})();
