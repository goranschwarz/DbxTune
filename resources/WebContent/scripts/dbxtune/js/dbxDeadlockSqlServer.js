/**
 * SQL Server deadlock picture: a SVG timeline with tooltips, a step-by-step "Play" and the SQL to look up the wait resources.
 * Used by the Counter Details "Row Detail" of CmDeadlocks (dbxGraphCounterDetails.js) and the Deadlock Viewer page
 * (DeadlockSqlServerServlet, Tools -> Deadlock -> SQL Server). The XML is parsed by the server (/api/sqlserver/deadlock-parse,
 * the same parser as the DSR and the DeadlockCount alarm); the picture has the same layout as SqlServerDeadlockTimelinePainter.
 * Needs: jQuery, escHtml() (dbxcentral.utils.js), Prism (optional, for the SQL).
 */

/**
 * Parse a deadlock report on the server and draw it into $pic (replaces its content).
 * @param xml        a <deadlock>, <deadlock-list> or XE <event> report
 * @param eventTime  when it was detected, 'yyyy-MM-dd HH:mm:ss.SSS', or '' (then the event's time, or an estimate)
 * @param opts       { isDark: bool, $modal: the dialog it is in (stop Play / hide the tooltip on close), onParsed: function(d) }
 */
function sqlServerDeadlockRender($pic, xml, eventTime, opts)
{
	opts = opts || {};
	$pic.addClass('dbx-dl-pic').html('<i class="fa fa-spinner fa-spin"></i> Drawing the deadlock...');
	$.ajax({
		url: '/api/sqlserver/deadlock-parse',
		type: 'POST',
		data: { xml: xml, eventTime: eventTime || '' },
		dataType: 'json',
		success: function(d) {
			if (d.error) { $pic.html('<i>' + escHtml(d.error) + '</i>'); if (opts.onParsed) opts.onParsed(d); return; }
			var dl = sqlServerDeadlockSvg(d, opts.isDark);
			$pic.html('<div class="d-flex align-items-start mb-1" style="gap:6px;">'
				+ '<div class="btn-group btn-group-sm">'
				+ '<button type="button" class="btn btn-outline-secondary" data-dl="play" title="Play: how the deadlock happened, step by step"><i class="fa fa-play"></i> Play</button>'
				+ '<button type="button" class="btn btn-outline-secondary" data-dl="prev" title="Previous step"><i class="fa fa-step-backward"></i></button>'
				+ '<button type="button" class="btn btn-outline-secondary" data-dl="next" title="Next step"><i class="fa fa-step-forward"></i></button>'
				+ '<button type="button" class="btn btn-outline-secondary" data-dl="all"  title="Show the whole picture">Show all</button>'
				+ '</div><div class="dbx-dl-caption" style="font-size:0.85em;padding-top:3px;min-height:2.6em;max-width:700px;"></div></div>'
				+ dl.html
				// SQL to look up each waitresource as it is now (from the server: CmDeadlocks.Deadlock.getWaitResourceSql)
				+ (d.processes.some(function(p) { return p.waitResourceSql; })
					? '<details class="mt-1" style="max-width:960px;"><summary style="font-size:0.85em;cursor:pointer;">'
						+ '<b>SQL: look up the wait resources</b> (what the sessions waited for, as it is <b>now</b>: the data may have changed, or be gone, since the deadlock)</summary>'
						+ '<pre class="language-sql" style="white-space:pre-wrap;margin:4px 0 0;font-size:0.8em;"><code class="language-sql" style="white-space:pre-wrap;">'
						+ escHtml(d.processes.map(function(p) { return p.waitResourceSql; }).filter(function(v, i, a) { return v && a.indexOf(v) === i; }).join('\n\n'))
						+ '</code></pre></details>'
					: ''));
			if (typeof Prism !== 'undefined')
				$pic.find('code.language-sql').each(function() { Prism.highlightElement(this); });

			// Play: step k shows the parts with data-step <= k (later ones faint), k = 0 is the whole picture
			var $svg = $pic.find('svg'), nSteps = dl.steps.length, cur = 0, timer = null;
			function showStep(k) {
				cur = k;
				$svg.find('[data-step]').each(function() {
					var st = +this.getAttribute('data-step');
					this.style.opacity = (k === 0 || st <= k) ? 1 : 0.08;
					$(this).toggleClass('dbx-dl-now', k > 0 && st === k);
				});
				$svg.toggleClass('dbx-dl-cycle', k >= dl.stepCycle);
				$pic.find('.dbx-dl-caption').html(k === 0 ? 'Press <b>Play</b> to see how the deadlock happened, step by step.' : '<b>Step ' + k + '/' + nSteps + ':</b> ' + dl.steps[k - 1]);
			}
			function stopPlay() {
				clearInterval(timer); timer = null;
				$pic.find('[data-dl=play]').html('<i class="fa fa-play"></i> Play');
			}
			$pic.find('[data-dl]').on('click', function() {
				var what = this.getAttribute('data-dl');
				if (what === 'play' && timer === null) {
					showStep(cur === 0 || cur === nSteps ? 1 : cur + 1);
					$(this).html('<i class="fa fa-pause"></i> Pause');
					timer = setInterval(function() { if (cur >= nSteps) stopPlay(); else showStep(cur + 1); }, 2500);
					return;
				}
				stopPlay();
				if (what === 'prev') showStep(Math.max(1, cur - 1));
				if (what === 'next') showStep(Math.min(nSteps, cur + 1));
				if (what === 'all')  showStep(0);
			});
			showStep(0);
			if (opts.$modal) opts.$modal.one('hide.bs.modal', stopPlay);

			// One floating tooltip (on <body>: above the modal) for every SVG part with a data-tip
			var $tip = $('#dbx-dl-tip');
			if ($tip.length === 0)
				$tip = $('<div id="dbx-dl-tip" style="position:fixed;z-index:100000;display:none;max-width:560px;padding:6px 9px;border-radius:4px;'
					+ 'background:#222;color:#eee;font-size:12px;line-height:1.35;box-shadow:0 2px 8px rgba(0,0,0,.35);pointer-events:none;"></div>').appendTo('body');
			$pic.find('[data-tip]')
				.on('mousemove', function(e) {
					$tip.html(this.getAttribute('data-tip')).show();
					var x = e.clientX + 14, y = e.clientY + 14;
					if (x + $tip.outerWidth()  > window.innerWidth)  x = Math.max(0, e.clientX - $tip.outerWidth()  - 8);
					if (y + $tip.outerHeight() > window.innerHeight) y = Math.max(0, e.clientY - $tip.outerHeight() - 8);
					$tip.css({ left: x, top: y });
				})
				.on('mouseleave', function() { $tip.hide(); });
			if (opts.$modal) opts.$modal.one('hide.bs.modal', function() { $tip.hide(); });
			if (opts.onParsed) opts.onParsed(d);
		},
		error: function(xhr) { $pic.html('<i>Could not draw the deadlock: ' + escHtml(xhr.status + ' ' + xhr.statusText) + '</i>'); }
	});
}

/**
 * SQL Server deadlock (JSON from /api/sqlserver/deadlock-parse) as a SVG timeline with tooltips (data-tip, HTML).
 * Same layout as SqlServerDeadlockTimelinePainter (the PNG in the DSR and the DeadlockCount alarm):
 * one lane per spid, "holds" bar from the transaction start, "waits" bar from the wait start, arrows from a
 * waiting session to the session that holds the lock, columns = the known moments in time order.
 * Returns {html, steps, stepCycle}: the parts have data-step="n" and steps[n-1] tells what happens in step n ("Play" in the Row Detail).
 */
function sqlServerDeadlockSvg(d, isDark)
{
	var W = 960, MARGIN = 14, LABEL_W = 170, AFTER_W = 120, LANE_H = 34, LANE_GAP = 26, HEADER_H = 34, MAX_LANES = 8;
	var C = isDark
		? { text:'#ddd', muted:'#aaa', holdFill:'#12352c', holdLine:'#3fb58f', holdText:'#a6e6cf', waitFill:'#3d1d1d', waitLine:'#e06c6c', waitText:'#f4bcbc', arrow:'#ff6b6b', axis:'#888' }
		: { text:'#2c2c2a', muted:'#5f5e5a', holdFill:'#e1f5ee', holdLine:'#0f6e56', holdText:'#085041', waitFill:'#fcebeb', waitLine:'#a32d2d', waitText:'#791f1f', arrow:'#e24b4a', axis:'#888780' };
	var FONT = '12px sans-serif', FONT_BOLD = 'bold 13px sans-serif', FONT_SMALL = '11px sans-serif', FONT_MONO = '11px monospace';

	var esc = escHtml;
	var ctx = document.createElement('canvas').getContext('2d');
	function fit(text, width, font) { // cut the text to the width, ending with "..."
		ctx.font = font;
		if (ctx.measureText(text).width <= width) return text;
		for (var len = text.length - 1; len > 3; len--)
			if (ctx.measureText(text.substring(0, len) + '...').width <= width) return text.substring(0, len) + '...';
		return '';
	}
	function ms(s) { return s ? new Date(s.replace(' ', 'T')).getTime() : null; }
	function fmtMs(v) {
		if (v < 1000)    return v + ' ms';
		if (v < 10000)   return (v / 1000).toFixed(2) + ' s';
		if (v < 60000)   return (v / 1000).toFixed(1) + ' s';
		if (v < 3600000) return Math.floor(v / 60000) + ' min ' + Math.floor((v % 60000) / 1000) + ' s';
		return Math.floor(v / 3600000) + ' h ' + Math.floor((v % 3600000) / 60000) + ' min';
	}
	function shortName(n) { if (!n) return '?'; var p = n.split('.'); return p.length === 3 ? p[1] + '.' + p[2] : n; }
	function objOrType(r) { return r.objectName || r.type; }
	function proc(id) { return d.processes.find(function(p) { return p.id === id; }); }
	function spidOf(id) { var p = proc(id); return p ? 'spid ' + p.spid + (p.ecid > 0 ? ' ecid ' + p.ecid : '') : id; } // ecid: parallel query thread
	function who(list) { // "spid 80 (X), spid 81 (S)"
		var s = list.map(function(o) { return spidOf(o.id) + (o.mode ? ' (' + o.mode + ')' : ''); });
		return s.filter(function(v, i) { return s.indexOf(v) === i; }).join(', ') || '-';
	}
	function resHtml(r) { // one lock resource, for tooltips
		return '<b>' + esc(r.type) + (r.mode ? ' ' + esc(r.mode) : '') + '</b>'
			+ (r.objectName ? ' on <b>' + esc(r.objectName) + '</b>' : '')
			+ (r.indexName  ? '<br>index: ' + esc(r.indexName) : '')
			+ '<br>held by: ' + esc(who(r.owners)) + '<br>wanted by: ' + esc(who(r.waiters));
	}
	function stmtHtml(p) {
		return (p.procName ? '<br><b>procedure:</b> ' + esc(p.procName + (p.procLine > 0 ? ' (line ' + p.procLine + ')' : '')) : '')
			+ (p.statement ? '<br><b>statement:</b><pre style="color:inherit;white-space:pre-wrap;margin:2px 0 0;font-size:11px;max-height:200px;overflow:hidden;">' + esc(p.statement) + '</pre>' : '');
	}
	function tipAttr(html) { return ' data-tip="' + esc(html) + '" style="cursor:help"'; }

	// Lanes: one per spid (all ecids), victim first
	var victim = d.processes.find(function(p) { return p.victim; });
	var spids = [];
	if (victim) spids.push(victim.spid);
	d.processes.forEach(function(p) { if (spids.indexOf(p.spid) < 0) spids.push(p.spid); });
	var lanes = spids.map(function(spid) {
		var l = { spid: spid, victim: false, procs: [], tranStart: null, waitTimeMs: 0, waitsFor: [], holds: [], waitMode: null };
		d.processes.forEach(function(p) {
			if (p.spid !== spid) return;
			l.procs.push(p);
			l.victim = l.victim || p.victim;
			var ts = ms(p.tranStarted);
			if (ts !== null && (l.tranStart === null || ts < l.tranStart)) l.tranStart = ts;
			l.waitTimeMs = Math.max(l.waitTimeMs, p.waitTimeMs);
		});
		l.procs.sort(function(a, b) { return a.ecid - b.ecid; });
		var ids = l.procs.map(function(p) { return p.id; });
		d.resources.forEach(function(r) {
			r.waiters.forEach(function(w) {
				if (ids.indexOf(w.id) < 0) return;
				if (l.waitsFor.indexOf(r) < 0) l.waitsFor.push(r);
				if (!l.waitMode && w.mode) l.waitMode = w.mode;
			});
			r.owners.forEach(function(o) { if (ids.indexOf(o.id) >= 0 && l.holds.indexOf(r) < 0) l.holds.push(r); });
		});
		return l;
	}).slice(0, MAX_LANES);

	var detect = ms(d.eventTime);
	if (detect === null) // should not happen: use the latest tran start
		lanes.forEach(function(l) { if (l.tranStart !== null && (detect === null || l.tranStart > detect)) detect = l.tranStart; });
	if (detect === null) detect = 0;

	// The known moments, in time order: tran start and wait start of every lane
	var moments = [];
	lanes.forEach(function(l) {
		var waitStart = detect - l.waitTimeMs;
		if (l.tranStart !== null && l.tranStart < waitStart && moments.indexOf(l.tranStart) < 0) moments.push(l.tranStart);
		if (l.waitTimeMs > 0 && moments.indexOf(waitStart) < 0) moments.push(waitStart);
	});
	if (moments.length === 0) moments.push(detect - 1);
	moments.sort(function(a, b) { return a - b; });

	// "Play" steps: the events in time order (tran start before a wait at the same time), then the cycle, the victim, the others continue
	var events = [];
	lanes.forEach(function(l) {
		var waitStart = detect - l.waitTimeMs;
		events.push({ t: l.tranStart !== null ? l.tranStart : waitStart - 1, kind: 0, lane: l });
		if (l.waitTimeMs > 0) events.push({ t: waitStart, kind: 1, lane: l });
	});
	events.sort(function(a, b) { return a.t - b.t || a.kind - b.kind; });
	events.forEach(function(e, i) { if (e.kind === 0) e.lane.stepTran = i + 1; else e.lane.stepWait = i + 1; });
	var steps = events.map(function() { return ''; }); // texts, filled in when the lanes are drawn
	var stepCycle = events.length + 1, stepVictim = events.length + 2, stepAfter = events.length + 3;
	function stepAttr(n) { return ' data-step="' + n + '"'; }
	var plotX0 = MARGIN + LABEL_W, detectX = W - MARGIN - AFTER_W;
	function momentX(t) { return plotX0 + Math.floor((detectX - plotX0) * (moments.indexOf(t) / moments.length)); }

	// Statement and resource lines below the lanes (full text in the tooltip)
	var lines = [];
	lanes.forEach(function(l) {
		var p = l.procs[0];
		var pn = p.procName ? p.procName + (p.procLine > 0 ? ' (line ' + p.procLine + ')' : '') : null;
		lines.push({ text: 'spid ' + l.spid + ': ' + (pn ? pn + ': ' : '') + (p.statement || '').replace(/\s+/g, ' ').trim(),
		             tip: '<b>spid ' + l.spid + '</b>' + stmtHtml(p) });
	});
	d.resources.forEach(function(r) {
		lines.push({ text: r.type + (r.mode ? ' ' + r.mode : '') + (r.objectName ? ' on ' + r.objectName : '') + (r.indexName ? ', index ' + r.indexName : '')
		                 + ': held by ' + who(r.owners) + ', wanted by ' + who(r.waiters),
		             tip: resHtml(r) });
	});

	var lanesTop = MARGIN + HEADER_H;
	var axisY    = lanesTop + lanes.length * (LANE_H + LANE_GAP) - LANE_GAP + 16;
	var stmtTop  = axisY + 40;
	var H        = stmtTop + lines.length * 16 + 30;

	var s = [];
	function text(x, y, str, font, color, extra) {
		s.push('<text x="' + x + '" y="' + y + '" style="font:' + font + '" fill="' + color + '"' + (extra || '') + '>' + esc(str) + '</text>');
	}
	function bar(x, y, w, str, fill, line, color, dashed, tip, step) {
		w = Math.max(w, 2);
		s.push('<g' + tipAttr(tip) + stepAttr(step) + '><rect x="' + x + '" y="' + y + '" width="' + w + '" height="' + LANE_H + '" rx="3" fill="' + fill + '" stroke="' + line + '"'
			+ (dashed ? ' stroke-dasharray="4 3"' : '') + '/>');
		var t = fit(str, w - 10, FONT);
		if (t) text(x + 6, y + LANE_H / 2 + 5, t, FONT, color);
		s.push('</g>');
	}

	// Header
	var dbName = (victim && victim.dbName) || (d.processes[0] && d.processes[0].dbName);
	var lockTypes = d.resources.map(function(r) { return r.type; }).filter(function(v, i, a) { return a.indexOf(v) === i; }).sort().join(', ');
	text(MARGIN, MARGIN + 14, (d.eventTime || '') + (d.eventTimeNote ? ' (' + d.eventTimeNote + ')' : '') + '   ' + (dbName ? 'database ' + dbName + '   ' : '') + lockTypes + (d.parallel ? '   parallel query' : ''), FONT_BOLD, C.text);

	// Lanes
	lanes.forEach(function(l, i) {
		l.y = lanesTop + i * (LANE_H + LANE_GAP);
		var waitStart = detect - l.waitTimeMs;
		l.xWait  = l.waitTimeMs > 0 ? momentX(waitStart) : detectX;
		l.xStart = (l.tranStart !== null && moments.indexOf(l.tranStart) >= 0) ? momentX(l.tranStart) : Math.max(plotX0, l.xWait - 60);
		var p = l.procs[0];

		// left label
		var laneTip = '<b>spid ' + l.spid + '</b>' + (l.victim ? ' <span style="color:#ff8a8a">victim (got error 1205)</span>' : '')
			+ (l.procs.length > 1 ? '<br>threads (ecid): ' + l.procs.map(function(x) { return x.ecid; }).join(', ') : '')
			+ '<br>login: ' + esc(p.login || '-') + '<br>host: ' + esc(p.host || '-') + '<br>application: ' + esc(p.app || '-')
			+ '<br>database: ' + esc(p.dbName || '-') + '<br>isolation level: ' + esc(p.isolationLevel || '-')
			+ '<br>transaction: ' + esc(p.tranName || '-') + (p.tranStarted ? ', started ' + esc(p.tranStarted) + ' (-' + fmtMs(detect - l.tranStart) + ')' : '')
			+ '<br>waited: ' + fmtMs(l.waitTimeMs) + stmtHtml(p);
		s.push('<g' + tipAttr(laneTip) + '><rect x="' + MARGIN + '" y="' + l.y + '" width="' + (LABEL_W - 10) + '" height="' + LANE_H + '" fill="transparent"/>');
		text(MARGIN, l.y + 14, 'spid ' + l.spid, FONT_BOLD, C.text);
		text(MARGIN, l.y + 29, fit((l.victim ? 'victim  ' : '') + (p.app || p.login || ''), LABEL_W - 10, FONT_SMALL), FONT_SMALL, l.victim ? C.waitLine : C.muted);
		s.push('</g>');

		// holds bar
		var holdsTxt = l.holds.length === 0 ? 'running' : 'holds ' + l.holds.map(function(r) {
			var o = r.owners.find(function(o) { return l.procs.some(function(x) { return x.id === o.id; }); });
			return (o && o.mode ? o.mode + ' on ' : '') + shortName(objOrType(r));
		}).filter(function(v, i, a) { return a.indexOf(v) === i; }).join(', ');
		var holdsTip = '<b>spid ' + l.spid + ' holds</b> (since the transaction start at the latest; the report has no time for when a lock was taken)'
			+ (l.holds.length === 0 ? '<br>no lock in this deadlock' : l.holds.map(function(r) { return '<br><br>' + resHtml(r); }).join(''));
		bar(l.xStart, l.y, l.xWait - l.xStart, holdsTxt, C.holdFill, C.holdLine, C.holdText, false, holdsTip, l.stepTran);
		steps[l.stepTran - 1] = (l.tranStart !== null ? '<b>spid ' + l.spid + '</b> starts its transaction (-' + fmtMs(detect - l.tranStart) + ')' : '<b>spid ' + l.spid + '</b> runs')
			+ (l.holds.length === 0 ? '.' : ' and at some point gets ' + esc(holdsTxt.substring('holds '.length)) + '.');

		// waits bar
		if (l.waitTimeMs > 0)
		{
			var waitsTxt = 'waits ' + (l.waitMode ? l.waitMode + ' ' : '') + 'on '
				+ (l.waitsFor.length === 0 ? (p.waitResource || '?') : l.waitsFor.map(function(r) { return shortName(objOrType(r)); }).filter(function(v, i, a) { return a.indexOf(v) === i; }).join(', '))
				+ '  (' + fmtMs(l.waitTimeMs) + ')';
			var waitsTip = '<b>spid ' + l.spid + ' waits ' + fmtMs(l.waitTimeMs) + '</b> for a ' + esc(l.waitMode || p.lockMode || '?') + ' lock'
				+ '<br>wait resource: ' + esc(p.waitResource || '-') + (p.waitResourceSql ? ' <i>(SQL to look it up: below the picture)</i>' : '')
				+ l.waitsFor.map(function(r) { return '<br><br>' + resHtml(r); }).join('') + stmtHtml(p);
			bar(l.xWait, l.y, detectX - l.xWait, waitsTxt, C.waitFill, C.waitLine, C.waitText, true, waitsTip, l.stepWait);
			var holders = []; // owners that are not waiting for the same resource (a parallel query: the other threads of the same spid)
			l.waitsFor.forEach(function(r) { r.owners.forEach(function(o) { if (!r.waiters.some(function(w) { return w.id === o.id; })) holders.push(o); }); });
			steps[l.stepWait - 1] = '<b>spid ' + l.spid + '</b> asks for ' + esc(waitsTxt.substring('waits '.length).replace(/^on /, '').replace(/\s+\(.*\)$/, '')) + ' (-' + fmtMs(l.waitTimeMs) + ')'
				+ (holders.length ? ', but ' + esc(who(holders)) + ' holds it: <b>spid ' + l.spid + ' waits</b>.' : ': <b>spid ' + l.spid + ' waits</b>.');
		}

		// after the detection
		if (l.victim)
		{
			s.push('<g' + tipAttr('<b>spid ' + l.spid + ' is the deadlock victim</b><br>Its batch is cancelled and its transaction is rolled back (error 1205), so the other sessions can go on.') + stepAttr(stepVictim) + '>');
			text(detectX + 10, l.y + LANE_H / 2 + 5, 'killed (1205)', FONT, C.waitLine);
			s.push('</g>');
		}
		else
			bar(detectX, l.y, AFTER_W - 10, 'continues', C.holdFill, C.holdLine, C.holdText, false, '<b>spid ' + l.spid + ' continues</b><br>It gets its lock when the victim is rolled back.', stepAfter);
	});

	// Arrows: waiting lane -> lane that holds the lock
	s.push('<defs><marker id="dbx-dl-arrow" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse">'
		+ '<path d="M0,0 L10,5 L0,10 z" fill="' + C.arrow + '"/></marker></defs>');
	var arrowNo = 0;
	lanes.forEach(function(from) {
		var done = [];
		from.waitsFor.forEach(function(r) {
			r.owners.forEach(function(o) {
				var owner = proc(o.id);
				if (!owner || owner.spid === from.spid || done.indexOf(owner.spid) >= 0) return;
				done.push(owner.spid);
				var to = lanes.find(function(l) { return l.spid === owner.spid; });
				if (!to) return;
				var fromX = from.xWait + Math.min(40 + (arrowNo % 4) * 18, Math.max(10, Math.floor((detectX - from.xWait) / 2)));
				var toX   = Math.max(to.xStart + 10, to.xWait - 20 - (arrowNo % 4) * 14);
				var fromY = to.y > from.y ? from.y + LANE_H : from.y;
				var toY   = to.y > from.y ? to.y            : to.y + LANE_H;
				var w = r.waiters.find(function(w) { return from.procs.some(function(x) { return x.id === w.id; }); });
				var tip = '<b>spid ' + from.spid + ' waits for spid ' + to.spid + '</b><br>spid ' + from.spid + ' wants ' + esc((w && w.mode) || '?')
					+ ' on ' + esc(objOrType(r)) + (r.indexName ? ' (index ' + esc(r.indexName) + ')' : '') + ', held by spid ' + to.spid + ' (' + esc(o.mode || '?') + ')';
				// wide transparent line under the arrow, so it is easy to hover
				s.push('<g class="dbx-dl-arr"' + tipAttr(tip) + stepAttr(from.stepWait || stepCycle) + '><line x1="' + fromX + '" y1="' + fromY + '" x2="' + toX + '" y2="' + toY + '" stroke="transparent" stroke-width="10"/>'
					+ '<line x1="' + fromX + '" y1="' + fromY + '" x2="' + toX + '" y2="' + toY + '" stroke="' + C.arrow + '" stroke-width="1.6" marker-end="url(#dbx-dl-arrow)"/></g>');
				arrowNo++;
			});
		});
	});

	// Detection line + axis with the moments
	s.push('<line x1="' + detectX + '" y1="' + (lanesTop - 8) + '" x2="' + detectX + '" y2="' + axisY + '" stroke="' + C.axis + '" stroke-dasharray="4 4"' + stepAttr(stepCycle) + '/>');
	s.push('<line x1="' + plotX0 + '" y1="' + axisY + '" x2="' + (detectX + AFTER_W - 10) + '" y2="' + axisY + '" stroke="' + C.axis + '"/>');
	var lastLabelEnd = -1e9;
	ctx.font = FONT_SMALL;
	moments.forEach(function(t) {
		var x = momentX(t);
		s.push('<line x1="' + x + '" y1="' + (axisY - 4) + '" x2="' + x + '" y2="' + (axisY + 4) + '" stroke="' + C.axis + '"/>');
		var lbl = '-' + fmtMs(detect - t), w = ctx.measureText(lbl).width;
		if (x - w / 2 > lastLabelEnd + 6)
		{
			var what = lanes.filter(function(l) { return l.tranStart === t; }).map(function(l) { return 'spid ' + l.spid + ' started its transaction'; })
				.concat(lanes.filter(function(l) { return l.waitTimeMs > 0 && detect - l.waitTimeMs === t; }).map(function(l) { return 'spid ' + l.spid + ' started to wait'; }));
			s.push('<g' + tipAttr('<b>' + esc(lbl) + '</b> before the detection<br>' + what.join('<br>')) + '>');
			text(x - w / 2, axisY + 18, lbl, FONT_SMALL, C.muted);
			s.push('</g>');
			lastLabelEnd = x + w / 2;
		}
	});
	var detLbl = 'detected' + (d.eventTime ? ' ' + d.eventTime.substring(11) : '') + (d.eventTimeNote ? ' (' + d.eventTimeNote + ')' : '');
	var detW = ctx.measureText(detLbl).width;
	s.push('<g' + tipAttr('<b>Deadlock detected</b> ' + esc(d.eventTime || '') + (d.eventTimeNote ? ' (' + esc(d.eventTimeNote) + ')' : '') + '<br>The lock monitor found the cycle and chose the victim.'
		+ (d.eventTimeNote === 'estimated' ? '<br>The report has no detection time: estimated as the latest transaction start + its wait time.' : '')
		+ (d.eventTimeNote === 'UTC' ? '<br>The time of the event is UTC (the other times in the report are the server local time).' : '')) + stepAttr(stepCycle) + '>');
	text(Math.min(detectX - detW / 2, W - MARGIN - detW), axisY + 18, detLbl, FONT_SMALL, C.text);
	s.push('</g>');

	// Statements and resources
	lines.forEach(function(ln, i) {
		s.push('<g' + tipAttr(ln.tip) + '>');
		text(MARGIN, stmtTop + i * 16, fit(ln.text, W - 2 * MARGIN, FONT_MONO), FONT_MONO, C.text);
		s.push('</g>');
	});

	// Legend
	text(MARGIN, H - 10, fit('Bars start at the transaction start (the report has no time for when a lock was taken). Red = waiting for a lock. Arrow = waits for a lock held by that session. Hover for details.', W - 2 * MARGIN, FONT_SMALL), FONT_SMALL, C.muted);

	// "Play" steps after the events
	var victims = lanes.filter(function(l) { return l.victim; }).map(function(l) { return 'spid ' + l.spid; });
	var others  = lanes.filter(function(l) { return !l.victim; }).map(function(l) { return 'spid ' + l.spid; });
	var cycle   = []; // per thread, so a parallel query (one spid) has a cycle too
	d.resources.forEach(function(r) { r.waiters.forEach(function(w) { r.owners.forEach(function(o) {
		var c = spidOf(w.id) + ' waits for ' + spidOf(o.id);
		if (w.id !== o.id && cycle.indexOf(c) < 0) cycle.push(c);
	}); }); });
	steps.push('<b>Deadlock:</b> ' + esc(cycle.join(', ')) + ' - nobody can go on. The lock monitor finds the cycle'
		+ (d.eventTime ? ' (detected ' + esc(d.eventTime.substring(11)) + ')' : '') + '.');
	steps.push('<b>' + esc(victims.join(', ') || '?') + '</b> is chosen as the victim: its batch is cancelled and its transaction is rolled back (error 1205), which frees its locks.');
	if (others.length)
		steps.push('<b>' + esc(others.join(', ')) + '</b> ' + (others.length > 1 ? 'get their locks and continue.' : 'gets its lock and continues.'));

	// Play: later parts are faint, the current one glows, the cycle's arrows flow
	var css = '<style>'
		+ '.dbx-dl-pic [data-step]{transition:opacity .5s}'
		+ '.dbx-dl-pic .dbx-dl-now{filter:drop-shadow(0 0 4px #f5a623)}'
		+ '.dbx-dl-pic svg.dbx-dl-cycle .dbx-dl-arr line[marker-end]{stroke-width:3;stroke-dasharray:7 4;animation:cmDlFlow .7s linear infinite}'
		+ '@keyframes cmDlFlow{to{stroke-dashoffset:-11}}</style>';

	return {
		html: '<svg xmlns="http://www.w3.org/2000/svg" width="' + W + '" height="' + H + '" viewBox="0 0 ' + W + ' ' + H + '">' + css + s.join('') + '</svg>',
		steps: steps,
		stepCycle: stepCycle
	};
}
