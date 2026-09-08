/**
 * dbxShowplanGraph.js — layout/rendering plumbing shared by the ASE and SQL Server Showplan renderers.
 *
 * dbxShowplanSqlServer.js was built as a copy of dbxShowplanAse.js ("copy now, extract later"), and
 * both have been independently bugfixed many times since. This module holds the pieces that are
 * provably IDENTICAL in both - so a fix lands once instead of twice, and the two can no longer
 * silently drift apart.
 *
 * What lives here is deliberately limited to code with ZERO vendor knowledge: it never looks at a
 * node's fields, only at the rendered DOM, and takes the renderer's CSS class prefix as a parameter.
 * Anything that differs between the two renderers - parsing, icons, findings rules, connector
 * thickness/tooltips - deliberately stays in each renderer file.
 *
 * Vendor-specific by design, NOT candidates for this module:
 *   - drawConnectorLines(): SQL Server's version grew row-scaled line thickness, per-thickness
 *     arrowheads and hover tooltips (with wider invisible hit paths); ASE's draws fixed 1.5px lines
 *     with a single arrowhead and no tooltips. Converging those is a deliberate behaviour change for
 *     ASE, not a refactor - so it is not forced into a shared implementation here.
 *
 * Loaded BEFORE dbxShowplanAse.js / dbxShowplanSqlServer.js (graph.html, ShowplanAseServlet,
 * ShowplanSqlServerServlet). Each renderer keeps thin same-named local wrappers that pass their own
 * prefix, so the (many) existing call sites inside those files stay untouched.
 */

window.DbxShowplanGraph = (function () {

	// Walks the parsed-plan node tree (the {op, props, metrics, children} model built by
	// parseXml()/parseText() - not the rendered DOM tree, see drawConnectorLines() for that),
	// invoking fn(node) for every node.
	function walkPlanNodes(root, fn) {
		fn(root);
		if (root.children) root.children.forEach(function (child) { walkPlanNodes(child, fn); });
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
	// regardless of VA - which silently undid the renderer's own sibling-order pass for exactly the joins it mattered
	// most for: when the continuing chain has the LOWER VA (chain executes first - the common case),
	// tucking the leaf above it put the HIGHER VA operator physically higher on screen, the opposite
	// of "lower VA reads first" (caught by the user comparing against the VA badges directly). Fixed
	// by tucking relative to the chain's own position instead of unconditionally to the top: a leaf
	// that VA-sorts BEFORE the chain tucks into a band above it (as before); a leaf that VA-sorts
	// AFTER the chain tucks into a band starting right below the chain's OWN box - specifically its
	// own small box height, not its full recursive subtree height (measured separately: the chain
	// li's natural height reflects its whole subtree per the comment above, but .<prefix>-box itself,
	// one level in, is never stretched - see the .<prefix>-box-cell comment above in the CSS block).
	// Either way the chain still flows normally and still needs its full natural subtree height
	// reserved in the cell - only the LEAF's position is decoupled from that height, never the
	// chain's own layout. Horizontal mode only for now - vertical mode's transposed table-row/
	// table-cell structure would need mirrored left/right positioning instead of top, not yet done.
	function tuckLeavesNearParent(treeEl, prefix) {
		var GAP = 9; // matches the li > ul > li margin in the CSS above
		treeEl.querySelectorAll('li').forEach(function (li) {
			var ul = li.querySelector(':scope > ul');
			if (!ul) return;
			var kids = Array.prototype.slice.call(ul.children); // already sibling-sorted by the renderer, see the renderer's own sibling-order pass
			var leafKids = [], nonLeafKids = [];
			kids.forEach(function (k) {
				(k.querySelectorAll(':scope > ul > li').length === 0 ? leafKids : nonLeafKids).push(k);
			});
			// Only handle the common "one chain, one or more leaves" shape - a node with 2+ continuing
			// children is the separate "balanced" case (see the renderer's own sibling-order pass's comment), where
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
				var box = leafLi.querySelector(':scope > .' + prefix + '-box-cell > .' + prefix + '-box');
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

			var chainOwnBox = chainLi.querySelector(':scope > .' + prefix + '-box-cell > .' + prefix + '-box');
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

			// The perpendicular half of the same problem, and the counterpart of the vertical
			// version's minWidth compensation further down.
			//
			// An after-tucked leaf is pulled out of flow entirely (position:absolute), so nothing in
			// normal flow reports how far DOWN it actually reaches. Left alone, this <ul> auto-sizes
			// to its in-flow content only (the chain), and that shorter height propagates up to this
			// node's own <li> and on to ITS parent - which then stacks the NEXT SIBLING BRANCH as if
			// this subtree ended higher than it visually does, dropping that branch's boxes straight
			// on top of the tucked leaf.
			//
			// Found by measuring, not by eye: a sweep over html-query-plan's 55-plan corpus checking
			// every pair of boxes for intersection flagged exactly one case - a Clustered Index Scan
			// (node 18) tucked under one branch of "what is my accepted answer percentage rate.sqlplan"
			// landing underneath an Index Seek (node 34) from the next branch. Reserving the real
			// measured extent fixes it and leaves every other plan's layout untouched.
			//
			// The reservation goes on the <li> (display:table), NOT on the <ul> (display:table-cell):
			// CSS leaves the effect of min-height on a table-cell undefined, and browsers duly ignore
			// it - verified here by setting it and watching the overlap survive unchanged. min-height
			// on the table box itself is honoured. This is also exactly what the vertical version
			// below does with min-width, on its own axis.
			if (afterChain.length) {
				var ulTop = ul.getBoundingClientRect().top;
				var lowest = 0;
				afterChain.forEach(function (leafLi) {
					lowest = Math.max(lowest, leafLi.getBoundingClientRect().bottom - ulTop);
				});
				var curMinHeight = parseFloat(window.getComputedStyle(li).minHeight) || 0;
				li.style.minHeight = Math.max(curMinHeight, Math.ceil(lowest)) + 'px';
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
	function tuckLeavesNearParentVertical(treeEl, prefix) {
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
			var kids = Array.prototype.slice.call(ul.children); // already sibling-sorted by the renderer, see the renderer's own sibling-order pass
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
			// padding-top (40px, set via ".<prefix>-tree.<prefix>-compact-v li > ul > li"), not from the
			// row itself - reading it from the chain cell (which stays untouched, still a real table
			// cell throughout) keeps a tucked leaf's own top offset in sync with that CSS value instead
			// of hardcoding it.
			var stepTop = window.getComputedStyle(chainLi).paddingTop || '0px';
			var maxLeafHeight = 0;

			function tuck(leafLi, leftRel) {
				var box = leafLi.querySelector(':scope > .' + prefix + '-box-cell > .' + prefix + '-box');
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

			// The chain's own box is CENTERED (a caption, ".<prefix>-compact-v li > .<prefix>-box-cell")
			// over its own cell's FULL width - which is sized to fit its entire subtree, not just its own
			// box, and can be far wider once its descendants fan out. So unlike the horizontal version
			// (whose box-cells are never centered, always flush), the chain's box left/right edges can't
			// be derived by arithmetic from its own width alone - unaccounted centering silently ate part
			// of the intended gap and let the first after-tucked leaf overlap the chain's box. Measure the
			// real rendered edges directly instead, after the before-chain reservation above (which shifts
			// the chain, and everything centered inside it, right by `offset`).
			var chainOwnBox = chainLi.querySelector(':scope > .' + prefix + '-box-cell > .' + prefix + '-box');
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

	// ─────────────────────────────────────────────────────────────────────────
	// Hover / pinned detail panel
	//
	// Originally each renderer appended the panel INSIDE its own box, positioned with CSS
	// (top:100%) and flipped upward when it would run off the bottom. That is simple but the panel
	// is then clipped by the diagram's own scroll container, and a long panel near the bottom has no
	// way to be read at all. The SQL Server renderer moved to a fixed-position overlay attached to
	// <body>, clamped into the viewport - strictly better, and now shared so ASE gets it too.
	//
	// The cost of leaving the box's coordinate space is that a fixed panel no longer travels with the
	// diagram, so scrolling/resizing has to be handled explicitly - see reposition() below.
	//
	// A factory rather than a singleton: each renderer supplies its own class prefix and its own
	// panel-content builder, and gets its own registry, so two renderers loaded on the same page
	// (graph.html loads both) can never close each other's panels.
	// ─────────────────────────────────────────────────────────────────────────

	function createPanelSystem(cfg) {
		var prefix     = cfg.prefix;
		var buildPanel = cfg.buildPanel;
		var openPanels = [];            // { $panel, boxEl, pinned }
		var listenersBound = false;
		var GAP = 6;

		/** Place a panel just below its box, flipping above / clamping to stay fully on screen. */
		function position($panel, boxEl) {
			var r = boxEl.getBoundingClientRect();
			// Measure before placing - the panel's size depends on its content.
			var pw = $panel[0].offsetWidth;
			var ph = $panel[0].offsetHeight;

			var top = r.bottom + GAP;
			if (top + ph > window.innerHeight - GAP) {
				var above = r.top - ph - GAP;
				// Prefer opening upward; if it does not fit either way, clamp to the viewport so the top
				// of the panel is always readable rather than letting it run off the bottom.
				top = (above >= GAP) ? above : Math.max(GAP, window.innerHeight - ph - GAP);
			}
			var left = r.left + (r.width / 2) - (pw / 2);
			left = Math.max(GAP, Math.min(left, window.innerWidth - pw - GAP));

			$panel.css({ left: Math.round(left) + 'px', top: Math.round(top) + 'px' });
		}

		/**
		 * Re-place open panels after anything that could have moved their box. A pinned panel follows
		 * its box; a transient hover tooltip is simply dismissed (the pointer has left it by then
		 * anyway). A panel whose box has been scrolled out of sight is closed rather than left
		 * stranded against the viewport edge.
		 */
		function reposition() {
			for (var i = openPanels.length - 1; i >= 0; i--) {
				var e = openPanels[i];
				if (!e.pinned || !document.body.contains(e.boxEl)) { closeAt(i); continue; }
				var r = e.boxEl.getBoundingClientRect();
				var offScreen = r.bottom < 0 || r.top > window.innerHeight
				             || r.right  < 0 || r.left > window.innerWidth;
				if (offScreen) { closeAt(i); continue; }
				position(e.$panel, e.boxEl);
			}
		}

		function bindListeners() {
			if (listenersBound) return;
			listenersBound = true;
			// Capture phase: the diagram scrolls in its own inner container, and a scroll event on a
			// descendant does not bubble - capturing on window is what catches it.
			window.addEventListener('scroll', reposition, true);
			window.addEventListener('resize', reposition);
		}

		function closeAt(i) {
			var e = openPanels[i];
			if (e) {
				e.$panel.remove();
				if (e.boxEl) e.boxEl.__dbxPlanPanel = undefined;
				openPanels.splice(i, 1);
			}
		}

		function panelFor(boxEl) { return boxEl ? boxEl.__dbxPlanPanel : undefined; }
		function isPinned(boxEl) { var p = panelFor(boxEl); return !!(p && p.pinned); }

		function close(boxEl) {
			for (var i = 0; i < openPanels.length; i++) {
				if (openPanels[i].boxEl === boxEl) { closeAt(i); return; }
			}
		}

		function closeAll(onlyTooltips) {
			for (var i = openPanels.length - 1; i >= 0; i--) {
				if (!onlyTooltips || !openPanels[i].pinned) closeAt(i);
			}
		}

		/** Build, attach and place a panel for a node. Replaces any panel already open on that box. */
		function open(node, boxEl, pinned) {
			close(boxEl);
			// Pinning is exclusive. When panels were anchored inside their own box they sat in the
			// diagram's flow and several could coexist harmlessly; now that they are large fixed
			// overlays, two pinned panels on nearby operators simply cover each other. One at a time
			// also matches the Properties pane, which already tracks the single selected operator.
			if (pinned) {
				for (var i = openPanels.length - 1; i >= 0; i--) {
					if (openPanels[i].pinned) closeAt(i);
				}
			}
			bindListeners();
			var $panel = buildPanel(node);
			if (!pinned) $panel.addClass(prefix + '-tooltip');
			$('body').append($panel);
			position($panel, boxEl);
			var entry = { $panel: $panel, boxEl: boxEl, pinned: !!pinned };
			boxEl.__dbxPlanPanel = entry;
			openPanels.push(entry);
			return $panel;
		}

		/** Is any panel open? Lets an Escape handler know whether it has something to close here. */
		function anyOpen() { return openPanels.length > 0; }

		return { open: open, panelFor: panelFor, isPinned: isPinned, anyOpen: anyOpen,
		         close: close, closeAll: closeAll, reposition: reposition, position: position };
	}

	// ─────────────────────────────────────────────────────────────────────────
	// Properties pane building blocks
	//
	// The SSMS-style Properties pane was built for SQL Server first; ASE is getting the same pane, and
	// there is no reason for the two to drift in how a key/value row, a section header or a collapsed
	// XML subtree looks. What is shared here is deliberately only the generic scaffolding - each
	// renderer still composes its OWN sections, because the fields worth showing genuinely differ
	// (ASE has no NodeId/parallel-thread data; SQL Server has no VA numbers). Forcing the section
	// LISTS into shared config too would be an abstraction over something that isn't actually common.
	// ─────────────────────────────────────────────────────────────────────────

	/** One "key: value" line. */
	function propRow($into, prefix, key, val) {
		if (val === undefined || val === null || val === '') return;
		$into.append($('<div class="' + prefix + '-prop-row"></div>')
			.append($('<span class="' + prefix + '-prop-key"></span>').text(key))
			.append($('<span class="' + prefix + '-prop-val"></span>').text(String(val))));
	}

	/** A titled section; returns the body element to append rows into. */
	function propSection($into, prefix, title) {
		var $sec = $('<div class="' + prefix + '-prop-sect"></div>');
		$sec.append($('<div class="' + prefix + '-prop-sect-hdr"></div>').text(title));
		var $body = $('<div class="' + prefix + '-prop-sect-body"></div>');
		$sec.append($body);
		$into.append($sec);
		return $body;
	}

	/**
	 * Recursive, collapsible rendering of an XML element: its attributes as key/value rows, then its
	 * child elements as nested collapsible blocks.
	 *
	 * opts:
	 *   stopAt      - predicate(el, parentDepth): true for an element that starts a DIFFERENT
	 *                 operator, so one operator's pane never swallows its children's XML. A predicate
	 *                 rather than a tag name because the two formats differ: SQL Server has a single
	 *                 <RelOp> marker at any depth, while in an ASE plan a child OPERATOR is simply a
	 *                 direct child that isn't a known property tag - which makes the test meaningful
	 *                 only at the operator's own level. Hence parentDepth: deeper elements (an <est>
	 *                 block's <rowCnt>/<lio>/...) have their own vocabulary and must not be filtered
	 *                 against the operator-level one, or they all vanish.
	 *   formatValue - per-renderer attribute formatting (plan XML is full of scientific notation).
	 */
	function propXmlTree($into, prefix, el, depth, opts) {
		opts = opts || {};
		var stopAt      = opts.stopAt || function () { return false; };
		var formatValue = opts.formatValue || function (v) { return v; };

		var kids = Array.prototype.filter.call(el.childNodes || [], function (n) { return n.nodeType === 1; })
			.filter(function (k) { return !stopAt(k, depth); });
		var a = attributesOf(el);
		var attrKeys = Object.keys(a);
		var ownText = (el.childNodes.length === 1 && el.firstChild.nodeType === 3)
			? (el.textContent || '').trim() : '';

		// A text-only leaf reads far better as a plain "name: value" row than as a collapsible block
		// containing a single "(text)" row. This is the normal shape of an ASE plan, where properties
		// are ELEMENTS with text (<objName>authors</objName>); SQL Server's ShowPlanXML is
		// attribute-heavy and hits this case only rarely. Note the ownText test has to happen BEFORE
		// the "nothing to show" return below - checking only attributes and element children dropped
		// every one of these leaves silently, which is exactly what it did on first run.
		if (!attrKeys.length && !kids.length) {
			if (ownText) propRow($into, prefix, localName(el), formatValue(ownText));
			return;
		}

		var $details = $('<details class="' + prefix + '-prop-xml"></details>');
		if (depth < 1) $details.attr('open', 'open');
		$details.append($('<summary></summary>').text(localName(el)
			+ (attrKeys.length ? ' (' + attrKeys.length + ')' : '')));
		var $body = $('<div class="' + prefix + '-prop-xml-body"></div>');
		attrKeys.forEach(function (k) { propRow($body, prefix, k, formatValue(a[k])); });
		if (ownText) propRow($body, prefix, '(text)', ownText);
		kids.forEach(function (k) { propXmlTree($body, prefix, k, depth + 1, opts); });
		$details.append($body);
		$into.append($details);
	}

	function localName(el) {
		return el ? (el.localName || el.nodeName || '').replace(/^.*:/, '') : '';
	}

	function attributesOf(el) {
		var out = {};
		if (!el || !el.attributes) return out;
		for (var i = 0; i < el.attributes.length; i++) out[el.attributes[i].name] = el.attributes[i].value;
		return out;
	}

	/**
	 * The generic half of the Properties pane's styling, so both panes look identical and a styling
	 * fix lands once. Vendor-specific extras (SQL Server's per-thread distribution bars) stay in the
	 * renderer that has the data for them.
	 */
	function propsCss(prefix) {
		var P = '.' + prefix + '-';
		return ''
			// Scroll shadow: a soft shading at the top/bottom edge that appears ONLY while there is more
			// content to scroll to in that direction, so "is this the end of the list?" is answerable at a
			// glance. Pure CSS, no scroll handler: the two `local` white gradients scroll WITH the content
			// and so cover the fixed (`scroll`) shadow gradients exactly when the pane is at that end.
			+ P + 'props-scroll {'
			+   'background:'
			+     'linear-gradient(#fff 30%, rgba(255,255,255,0)) top / 100% 14px no-repeat local,'
			+     'linear-gradient(rgba(255,255,255,0), #fff 70%) bottom / 100% 14px no-repeat local,'
			+     'radial-gradient(farthest-side at 50% 0, rgba(0,0,0,0.14), rgba(0,0,0,0)) top / 100% 7px no-repeat scroll,'
			+     'radial-gradient(farthest-side at 50% 100%, rgba(0,0,0,0.14), rgba(0,0,0,0)) bottom / 100% 7px no-repeat scroll;'
			+ '}'
			+ P + 'prop-title { font-weight: 700; font-size: 12px; margin-bottom: 1px; }'
			+ P + 'prop-subtitle { color: #777; font-size: 10px; margin-bottom: 5px; }'
			+ P + 'prop-desc { font-style: italic; color: #5c5c5c; font-size: 10px; line-height: 1.35; margin-bottom: 8px; }'
			+ P + 'prop-empty { color: #999; font-style: italic; font-size: 11px; }'
			+ P + 'prop-sect { margin-bottom: 8px; }'
			+ P + 'prop-sect-hdr { font-weight: 600; font-size: 10px; text-transform: uppercase; letter-spacing: 0.03em; color: #6a6a6a; border-bottom: 1px solid #ddd; padding-bottom: 2px; margin-bottom: 3px; }'
			+ P + 'prop-row { display: flex; gap: 8px; font-size: 11px; padding: 1px 0; align-items: baseline; }'
			+ P + 'prop-key { flex: 0 0 42%; color: #777; word-break: break-word; }'
			// break-word (not nowrap+ellipsis): a Predicate/ScalarString value is often very long, and in a
			// narrow pane truncating it would hide exactly the part being investigated.
			+ P + 'prop-val { flex: 1 1 auto; word-break: break-word; }'
			+ P + 'prop-warn { color: #a8500f; font-size: 11px; line-height: 1.35; margin-bottom: 3px; }'
			+ P + 'prop-xml > summary { cursor: pointer; font-size: 10px; color: #555; padding: 1px 0; user-select: none; }'
			+ P + 'prop-xml-body { padding-left: 10px; border-left: 1px solid #e2e2e2; margin-left: 3px; }';
	}

	/**
	 * Marks the operators that provably never ran, so the renderer can visually strike them out and
	 * the findings rules can stay quiet about them.
	 *
	 * The obvious test - "this node's ActualExecutions is 0" - is NOT safe, and gets it wrong on 32 of
	 * the 52 zero-execution operators in the reference corpus. SQL Server 2012 batch-mode plans emit
	 * Parallelism (Repartition Streams) exchanges whose per-thread counters are all zero (or which
	 * carry only the Thread="0" coordinator row) while the entire subtree below them demonstrably ran
	 * 12 times over 108M rows. Those zeroes are a counter-reporting artifact, not evidence.
	 *
	 * So the test used here is the SUBTREE one: a node counts as never-executed only when it reports
	 * zero executions AND nothing anywhere below it reports a non-zero count. On the corpus that marks
	 * 20 operators with zero false positives, and - the useful part - it needs no per-operator
	 * allow/block list, because a running subtree is exactly what disqualifies the batch-mode
	 * exchanges. Adding an operator-name exclusion on top of it changes nothing (verified: same 20).
	 *
	 * `execOf(node)` returns that node's summed-across-threads execution count, or null/undefined when
	 * the node carries no runtime counters at all. Absent is deliberately NOT treated as zero: on an
	 * estimated-only plan every node is counter-less, and conflating the two would grey out the whole
	 * diagram (14 of the 55 corpus plans are estimated-only).
	 *
	 * Sets `node._neverExecuted` (always, true or false - so a re-parse cannot leave stale marks) and
	 * returns how many nodes were marked.
	 */
	function markNeverExecuted(root, execOf) {
		var count = 0;

		// Everything below a never-executed node is unreachable by definition, so it is marked too -
		// including nodes that carry no counters of their own, which would otherwise punch holes in an
		// otherwise solid greyed-out region.
		function markSubtree(node) {
			if (!node._neverExecuted) { node._neverExecuted = true; count++; }
			(node.children || []).forEach(markSubtree);
		}

		// Post-order: a node cannot be judged until its whole subtree has reported back.
		// Returns true when this node, or anything below it, actually ran.
		function visit(node) {
			var own      = execOf(node);
			var ranHere  = (typeof own === 'number' && own > 0);
			var ranBelow = false;
			(node.children || []).forEach(function (child) { if (visit(child)) ranBelow = true; });

			node._neverExecuted = false;
			if (own === 0 && !ranBelow) markSubtree(node);

			return ranHere || ranBelow;
		}

		visit(root);
		return count;
	}

	/**
	 * Styling for the never-executed marking - diagonal grey hatching over the whole box, with its
	 * icon desaturated and its text muted, so an untaken branch reads as "nothing to see here" at a
	 * glance without hiding what the operator actually was.
	 */
	function neverExecutedCss(prefix) {
		var P = '.' + prefix + '-';
		return ''
			// Placed after the other box-state rules (big-table, eager-spool, has-warning) so the
			// hatching wins on a box that is both never-executed and, say, over the size threshold -
			// a warning about a branch that never ran is the less important of the two.
			+ P + 'box.' + prefix + '-never-exec {'
			+   ' background-image: repeating-linear-gradient(135deg,'
			+     ' rgba(0,0,0,0) 0, rgba(0,0,0,0) 5px,'
			+     ' rgba(130,130,130,0.16) 5px, rgba(130,130,130,0.16) 10px);'
			// Explicit neutral fill so no other box state (big-table's red wash, eager-spool's) can tint a
			// dead branch - background-image alone would sit on top of whatever colour they set.
			+   ' background-color: #fafafa;'
			+   ' border-color: #c6c6c6; border-style: dashed; }'
			// Compound selectors (two classes deep) so these beat the plain .xx-plan-label /
			// .xx-plan-cost colours without needing !important.
			+ P + 'box.' + prefix + '-never-exec ' + P + 'label,'
			+ P + 'box.' + prefix + '-never-exec ' + P + 'logicalop,'
			+ P + 'box.' + prefix + '-never-exec ' + P + 'subtitle,'
			+ P + 'box.' + prefix + '-never-exec ' + P + 'metric,'
			+ P + 'box.' + prefix + '-never-exec ' + P + 'cost { color: #909090; }'
			+ P + 'box.' + prefix + '-never-exec ' + P + 'icon { filter: grayscale(1); opacity: 0.45; }'
			// The "never executed" caption itself - the only part of the box kept legible.
			+ P + 'never-exec-note { font-size: 10px; font-style: italic; color: #7a7a7a;'
			+   ' letter-spacing: 0.02em; }';
	}

	return {
		walkPlanNodes:                walkPlanNodes,
		markNeverExecuted:            markNeverExecuted,
		tuckLeavesNearParent:         tuckLeavesNearParent,
		tuckLeavesNearParentVertical: tuckLeavesNearParentVertical,

		createPanelSystem:            createPanelSystem,

		propRow:                      propRow,
		propSection:                  propSection,
		propXmlTree:                  propXmlTree,
		propsCss:                     propsCss,
		neverExecutedCss:             neverExecutedCss
	};
})();
