/**
 * dbxConfig.js — the "Collector Configuration" page, modern layout (config.html; was config2.html during development).
 *
 * Same data and save API as the classic page (config_classic.html):
 *   GET  /api/cc/mgt/config/get?srvName=...   (NoGuiConfigGetServlet, via ProxyConfigGetServlet)
 *   POST /api/cc/mgt/config/set?srvName=...   (NoGuiConfigSetServlet, via ProxyConfigSetServlet, admin only -> 403)
 *
 * Alarms are edited inline with the SAME field model as the Alarm Editor dialog (DbxAlarmEditor.model),
 * so field kinds, ordering and validation are identical in both places.
 *
 * All edits (alarms, pre-checks, options, local settings) are collected in one "pending" map and saved together
 * from the save bar: one 'alarmBatch' request per changed alarm (validated all-or-nothing by the collector),
 * one request per option / setting / pre-check. Items that fail stay pending, with the collector's message.
 *
 * Not logged in as admin: everything is read-only. The login is re-checked when the browser tab gets focus,
 * so logging in on another tab unlocks the page without losing what is selected or pending.
 *
 * Requires: dbxcentral.utils.js (getParameter, isLoggedIn), dbxAlarmEditor.js, dbxAlarmOverview.js, jQuery (for isLoggedIn), Prism (optional).
 */
var DbxConfig2 = (function()
{
	'use strict';

	var M = DbxAlarmEditor.model;

	// The whole page state. render() draws everything from it.
	var S = {
		srvName    : '',
		cfg        : null,     // '/api/cc/mgt/config/get' response
		loadError  : null,
		isLoggedIn : false,
		isAdmin    : false,
		userName   : '',
		authLost   : false,    // a save got 403 (login expired, or not admin any more)

		tab        : 'counters',   // counters | overview | writers | dsr
		selCm      : null,
		sub        : 'alarms',     // alarms | options | settings | info
		open       : {},           // expanded alarms: { 'cmName|alarmName': true }
		cmFilter   : { q: '', alarms: false, mod: false, pend: false, off: false },
		ovFilter   : { q: '', eff: false, mod: false, pend: false },
		selWriter  : null,
		wsub       : 'settings',   // settings | filters
		dsub       : 'report',     // Daily Summary Report sub tab: report | mail | sections
		mr         : {},           // mail rules editor state, per field key: { view, edit, testOpen, test, testing, testRes }
		mrInfo     : null,         // from the collector: alarm names, enum values, server group (op 'mailRoutingInfo')
		tpl        : {},           // template editor state, per field key: { preview, vars, action, last, card }

		reg        : {},   // key -> meta of every editable field drawn (type, cmName, alarm, name, orig, def, kind, label)
		pend       : {},   // key -> meta + { value }
		errors     : {},   // key -> message from the collector (last save)
		saving     : false,
		menuOpen   : false,
		listOpen   : false,
		message    : null, // { type: ok|err|warn|info, text }
		flashKeys  : {}    // rows to highlight briefly: { key: true }
	};

	//------------------------------------------------------------------
	// Helpers
	//------------------------------------------------------------------
	function esc(v)
	{
		if (v === undefined || v === null) return '';
		return String(v).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
	}
	function el(id) { return document.getElementById(id); }
	function toStr(v) { return (v === undefined || v === null) ? '' : Array.isArray(v) ? v.join(', ') : String(v); }
	function baseName(p) { return String(p || '').split(/[\\/]/).pop(); }

	/** A long or multi-line value (eg a template) in one short line: its first line + "... (N lines)" */
	function shortVal(v)
	{
		v = String(v);
		if (v.length <= 120 && v.indexOf('\n') === -1) return v;
		var lines = v.split('\n'), first = lines[0].length > 80 ? lines[0].substring(0, 80) + '...' : lines[0];
		return lines.length > 1 ? first + ' ... (' + lines.length + ' lines)' : first;
	}

	/** Config file with the given role ('server' | 'shared'), or null (older collectors do not send 'configFiles') */
	function configFile(role)
	{
		return ((S.cfg && S.cfg.configFiles) || []).find(function(f) { return f.role === role; }) || null;
	}

	// Descriptions from the collector may hold HTML (<br>, <b>, ...): show them as plain text, never as markup
	var _textCache = {};
	function htmlToText(s)
	{
		if (!s) return '';
		if (s.indexOf('<') === -1 && s.indexOf('&') === -1) return s;
		if (s in _textCache) return _textCache[s];
		var doc = new DOMParser().parseFromString(String(s).replace(/<br\s*\/?>/gi, ' '), 'text/html');
		return (_textCache[s] = (doc.body.textContent || '').replace(/\s+/g, ' ').trim());
	}

	// A collector's longer help (the foldable "How ... work" sections): keep its structure, but only harmless tags,
	// without any attributes (no links, styles or event handlers), and drop script/style content
	var SAFE_TAGS = { B: 1, I: 1, CODE: 1, PRE: 1, BR: 1, UL: 1, OL: 1, LI: 1, P: 1, H2: 1, H3: 1, H4: 1 };
	function safeHtml(s)
	{
		if (!s) return '';
		var walk = function(n) {
			return [].map.call(n.childNodes, function(c) {
				if (c.nodeType === 3) return esc(c.nodeValue);
				if (c.nodeType !== 1 || c.tagName === 'SCRIPT' || c.tagName === 'STYLE') return '';
				var t = c.tagName.toLowerCase();
				if (!SAFE_TAGS[c.tagName]) return walk(c);
				return t === 'br' ? '<br>' : '<' + t + '>' + walk(c) + '</' + t + '>';
			}).join('');
		};
		return walk(new DOMParser().parseFromString(String(s), 'text/html').body);
	}

	function cmList()      { return (S.cfg && S.cfg.cmList) || []; }
	function writerList()  { return (S.cfg && S.cfg.alarmWriters) || []; }
	function dsrCfg()      { return (S.cfg && S.cfg.dailySummaryReport && S.cfg.dailySummaryReport.settings) ? S.cfg.dailySummaryReport : null; } // null: a collector without it (older version)
	/** The Daily Summary Report entries of a sub tab (report | mail | sections), in the shape propField() takes */
	function dsrList(sub)
	{
		var d = dsrCfg();
		if (!d) return [];
		if (sub === 'mail') return (d.sender && d.sender.settings) || [];
		if (sub === 'sections')
			return (d.sections || []).map(function(s) {
				return { name: s.subject, property: s.property, value: s.value, defaultValue: s.defaultValue, datatype: 'Boolean', description: '',
				         canBeDisabled: s.canBeDisabled, className: s.className, source: s.source, sourceInMemoryOnly: s.sourceInMemoryOnly };
			});
		return d.settings || [];
	}
	function findCm(name)  { return cmList().find(function(c) { return c.cmName === name; }) || null; }
	function alarmsOf(cm)  { return (cm && cm.alarmSettings && cm.alarmSettings.alarms) || []; }
	function preChecksOf(cm) { return (cm && cm.alarmSettings && cm.alarmSettings.preChecks) || []; }
	function pendCount()   { return Object.keys(S.pend).length; }
	function pendKeysFor(pred) { return Object.keys(S.pend).filter(function(k) { return pred(S.pend[k]); }); }
	function cur(key, orig) { return key in S.pend ? S.pend[key].value : orig; }

	function alarmKey(cmName, alarmName, paramName) { return 'A|' + cmName + '|' + alarmName + '|' + paramName; }
	function propKey(type, cmName, name) { return type.charAt(0).toUpperCase() + '|' + cmName + '|' + name; }

	/** Option / Local Setting / Pre-check entry -> the same field shape as DbxAlarmEditor.model.buildFields() */
	function propField(p)
	{
		var kind = M.fieldKind(null, p);
		if (p.datatype === 'Long') kind = 'int';
		var vn = p.validatorName || '';
		if (vn.indexOf('UrlInputValidator')  !== -1) kind = 'url';
		if (vn.indexOf('JsonInputValidator') !== -1) kind = 'json';
		if (vn.indexOf('MailRoutingInputValidator') !== -1) kind = 'mailRouting'; // mail recipients: CSV or JSON rules (MailHelper), validated by the collector
		if (vn.indexOf('TemplateInputValidator')    !== -1) kind = 'template';    // a writer's message template, rendered by the collector
		var val = (p.value === null || p.value === undefined) ? '' : String(p.value);
		var def = (p.defaultValue === null || p.defaultValue === undefined) ? '' : String(p.defaultValue);
		if (kind === 'bool') { val = String(val.toLowerCase() === 'true'); def = String(def.toLowerCase() === 'true'); }
		return {
			name: p.name, label: p.name, kind: kind, isMain: false, orig: val, value: val, def: def,
			property: p.property || '', description: p.description || '', secret: val === '***secret***',
			source: p.source, sourceInMemoryOnly: p.sourceInMemoryOnly
		};
	}

	/** f = { kind, value, meta } (meta: the S.reg / S.pend entry, needed by 'template') */
	function validateField(f)
	{
		if (f.kind === 'mailRouting') return mrValidateField(f.value);
		if (f.kind === 'template')    return tplValidateField(f.meta, f.value);
		if (f.kind === 'url')
		{
			if (f.value === '') return {};
			try { new URL(f.value); return {}; } catch (e) { return { error: 'Not a valid URL' }; }
		}
		if (f.kind === 'json')
		{
			if (f.value === '') return {};
			try { JSON.parse(f.value); return {}; } catch (e) { return { error: 'Invalid JSON: ' + e.message }; }
		}
		return M.validate(f);
	}

	/** The server side field for a pending entry (after a reload the 'orig' may have changed) */
	function lookupField(meta)
	{
		if (meta.type === 'writer')
		{
			var w = writerList().find(function(x) { return x.className === meta.writer; });
			var wp = w ? ((meta.wsub === 'filters' ? w.filters : w.settings) || []).find(function(x) { return x.name === meta.name; }) : null;
			return wp ? propField(wp) : null;
		}
		if (meta.type === 'dsr')
		{
			var dp = dsrList(meta.wsub).find(function(x) { return x.property === meta.property; });
			return dp ? propField(dp) : null;
		}
		var cm = findCm(meta.cmName);
		if (!cm) return null;
		if (meta.type === 'alarm')
		{
			var a = alarmsOf(cm).find(function(x) { return x.name === meta.alarm; });
			return a ? (M.buildFields(a).find(function(f) { return f.name === meta.name; }) || null) : null;
		}
		var list = meta.type === 'options' ? cm.options : meta.type === 'settings' ? cm.settings : preChecksOf(cm);
		var p = (list || []).find(function(x) { return x.name === meta.name; });
		return p ? propField(p) : null;
	}

	function isCmModified(cm)
	{
		return modifiedList(cm).length > 0;
	}

	//------------------------------------------------------------------
	// Where a value comes from: 'source' per value, from collectors newer than 2026-09-28 (NoGuiConfigGetServlet.ValueSource)
	//------------------------------------------------------------------
	var SOURCE_LABEL = { server: 'Server config', shared: 'Shared config', temp: 'Temporary', user: 'User config', system: 'System config', 'default': 'Default' };

	function sourceLabel(source) { return SOURCE_LABEL[source] || source; }

	function sourceTitle(p)
	{
		var f = configFile(p.source);
		var t = p.source === 'default' ? 'Not set in any config file, the default from the code is used'
		      : 'Set in the ' + sourceLabel(p.source).toLowerCase() + (f ? ': ' + f.filename : '');
		if (p.source === 'shared' && configFile('server')) t += '\n(set it for this server only with "Save for THIS server")';
		return t;
	}

	/** Chips for a field row: where the value comes from (+ 'in memory only' when not written to the file) */
	function sourceChips(p)
	{
		if (!p || !p.source) return '';
		var h = '<span class="c2-src c2-src-' + esc(p.source) + '" title="' + esc(sourceTitle(p)) + '"><i class="fa-regular fa-file-lines"></i> ' + esc(sourceLabel(p.source)) + '</span>';
		if (p.sourceInMemoryOnly)
			h += '<span class="c2-src c2-src-mem" title="Changed with an in-memory only save: not written to the server config file, so it is lost when the collector restarts">'
				+ '<i class="fa-solid fa-memory"></i> in memory only</span>';
		return h;
	}

	/** ' [Server config]' for tooltip lines, '' when the collector does not tell */
	function sourceSuffix(p) { return p && p.source ? '  [' + sourceLabel(p.source) + (p.sourceInMemoryOnly ? ', in memory only' : '') + ']' : ''; }

	/** The 'Save Counters*' options are shown by their own badge (persistBadge), not in the "mod" list */
	function isPersistOption(cm, p) { return (p.property || '').indexOf(cm.cmName + '.persistCounters') === 0; }

	/** Everything in a CM that differs from its default, as text lines: 'Alarm LockWaits: TimeThreshold = 30 (default: 10)  [Server config]' */
	function modifiedList(cm)
	{
		var lines = [];
		var line  = function(prefix, name, p) { return prefix + name + ' = ' + (p.value === '' ? '(empty)' : p.value) + '  (default: ' + (p.defaultValue === '' || p.defaultValue === null ? '(empty)' : p.defaultValue) + ')' + sourceSuffix(p); };
		alarmsOf(cm).forEach(function(a) {
			(a.parameters || []).forEach(function(p) {
				if (p.isDefaultValue !== false) return;
				var n = p.name.indexOf(a.name + ' ') === 0 ? p.name.substring(a.name.length + 1) : p.name;
				lines.push(line('Alarm ' + a.name + ': ', n, p));
			});
		});
		preChecksOf(cm)   .forEach(function(p) { if (p.isDefaultValue === false) lines.push(line('Pre check: ', p.name, p)); });
		(cm.options  || []).forEach(function(p) { if (p.isDefaultValue === false && !isPersistOption(cm, p)) lines.push(line('Option: ', p.name, p)); });
		(cm.settings || []).forEach(function(p) { if (p.isDefaultValue === false) lines.push(line('Setting: '  , p.name, p)); });
		return lines;
	}

	/** Tooltip text for a list of modified lines (capped, a tooltip should not fill the screen) */
	function modifiedTitle(lines)
	{
		var max = 25;
		return 'Differs from the default:\n' + lines.slice(0, max).map(function(l) { return '• ' + l; }).join('\n')
			+ (lines.length > max ? '\n... and ' + (lines.length - max) + ' more' : '');
	}

	/** A collector option by its property key after the CM name ('postponeTime' -> '<cm>.postponeTime'), or null */
	function optionOf(cm, key)
	{
		return (cm.options || []).find(function(p) { return p.property === cm.cmName + '.' + key; }) || null;
	}
	function optionValue(cm, key)
	{
		var o = optionOf(cm, key);
		return o ? String(o.value) : null;
	}

	/** Save Counters (to the recording database): green/red database icon */
	function persistOf(cm)
	{
		var o = optionOf(cm, 'persistCounters');
		if (!o) return null;
		var on = String(o.value) === 'true';
		var yn = function(k) { return optionValue(cm, 'persistCounters.' + k) === 'true' ? 'yes' : 'no'; };
		var title = on ? 'Counter data is saved to the recording database (Abs: ' + yn('abs') + ', Diff: ' + yn('diff') + ', Rate: ' + yn('rate') + ')'
		               : 'Counter data is NOT saved to the recording database';
		title += sourceSuffix(o) + '\n(change it in Options: Save Counters)';
		return { on: on, title: title };
	}

	function persistBadge(cm)
	{
		var ps = persistOf(cm);
		return ps ? '<span class="c2-bdg c2-bdg-save ' + (ps.on ? 'c2-save-on' : 'c2-save-off') + '" title="' + esc(ps.title) + '"><i class="fa-solid fa-database"></i></span>' : '';
	}

	/** 300 -> '5m', 5400 -> '1h30m', 45 -> '45s' */
	function fmtSecs(secs)
	{
		var h = Math.floor(secs / 3600), m = Math.floor(secs % 3600 / 60), s = secs % 60;
		return (h ? h + 'h' : '') + (m ? m + 'm' : '') + (s || (!h && !m) ? s + 's' : '');
	}

	/** Postpone: the counter is refreshed at most every N seconds (null when not postponed) */
	function postponeOf(cm)
	{
		var secs = parseInt(optionValue(cm, 'postponeTime') || '0', 10);
		if (!(secs > 0)) return null;
		var enabled = optionValue(cm, 'postponeIsEnabled') !== 'false';
		return { secs: secs, enabled: enabled, text: fmtSecs(secs),
			title: enabled ? 'Postponed: refreshed at most every ' + fmtSecs(secs) + ' (' + secs + ' seconds), not on every sample'
			               : 'Postpone time ' + fmtSecs(secs) + ' is set, but postpone is disabled (refreshed on every sample)' };
	}

	function postponeBadge(cm)
	{
		var pp = postponeOf(cm);
		return pp ? '<span class="c2-bdg c2-bdg-pp' + (pp.enabled ? '' : ' c2-bdg-off') + '" title="' + esc(pp.title) + '"><i class="fa-regular fa-clock"></i> ' + esc(pp.text) + '</span>' : '';
	}

	/** Why the alarms of a CM will not fire, [] when they can */
	function whyNoAlarms(cm)
	{
		var why = [];
		if (cm.isCmEnabled === false)           why.push('the Counter is not enabled');
		if (cm.isAlarmEnabled === false)        why.push('alarms are disabled for this Counter');
		if (cm.isSystemAlarmsEnabled === false) why.push('system alarms are disabled for this Counter');
		return why;
	}

	function presetFor(cron) { return M.CRON_PRESETS.find(function(p) { return p.cron === cron; }) || null; }

	//------------------------------------------------------------------
	// Pending changes
	//------------------------------------------------------------------
	function setPend(key, value)
	{
		var meta = S.reg[key];
		if (!meta || !S.isAdmin) return;
		if (value === meta.orig) delete S.pend[key];
		else S.pend[key] = Object.assign({}, meta, { value: value });
		delete S.errors[key];
	}

	function blockingCount()
	{
		return Object.keys(S.pend).filter(function(k) {
			var p = S.pend[k];
			return validateField({ kind: p.kind, value: p.value, meta: p }).error;
		}).length;
	}

	/** After a (re)load: refresh 'orig' of pending entries, drop the ones that now match the server */
	function reconcilePending()
	{
		Object.keys(S.pend).forEach(function(k) {
			var f = lookupField(S.pend[k]);
			if (!f) { delete S.pend[k]; return; }
			S.pend[k].orig = f.orig;
			S.pend[k].def  = f.def;
			if (S.pend[k].value === f.orig) { delete S.pend[k]; delete S.errors[k]; }
		});
	}

	//------------------------------------------------------------------
	// Mail rules editor, field kind 'mailRouting' (the setting has a MailRoutingInputValidator, eg AlarmWriterToMail.to)
	// The value is a comma separated list of addresses, or a JSON array of rules (see the Java class MailHelper).
	// Validation and the "who would get this alarm" tester run on the COLLECTOR (POST /api/cc/mgt/writer/try -> /api/mgt/writer/try),
	// so there is only one rule engine: the collector's own.
	//------------------------------------------------------------------
	var MR_CONDS  = ['serverName', 'serverGroup', 'alarmName', 'category', 'severity', 'state', 'action', 'time'];
	var MR_ALARM_CONDS = ['alarmName', 'category', 'severity', 'state', 'action', 'time']; // need an alarm: never match a Daily Summary Report (MailHelper.Rule.isAlarmOnly)
	var MR_FIELDS = [ // the edit form, in order: [key, label, placeholder, suggestions (field in S.mrInfo)]
		['name',        'Name',         'eg: DBA team',                                         null],
		['serverName',  'Server name',  'regexp (full match), eg: PROD_.*',                     null],
		['serverGroup', 'Server group', 'regexp, the DbxCentral group of the server',           null],
		['alarmName',   'Alarm name',   'regexp, eg: (FullTranLog|SrvDown)',                    'alarmNames'],
		['category',    'Category',     'regexp, eg: DOWN|SPACE',                               'categories'],
		['severity',    'Severity',     'regexp, eg: ERROR  or  WARNING|ERROR  or  (?!INFO).* (all but INFO)', 'severities'],
		['state',       'State',        'regexp, eg: DOWN|AFFECTED',                            'states'],
		['action',      'Action',       'RAISE or CANCEL',                                      'actions'],
		['time',        'Time',         'cron on the time the alarm was raised, eg: * 0-7,18-23 * * *  (a leading ! = NOT within)', null],
		['to',          'To',           'a@acme.com, b@acme.com',                               null],
		['cc',          'Cc',           '',                                                     null],
		['comment',     'Comment',      '',                                                     null]
	];
	var _mrIds = {}, _mrSeq = 0;                          // field key -> id prefix (stable over re-renders, for the focus restore)
	var _mrVal = {}, _mrValBusy = {}, _mrValTimer = null; // value -> the collector's validation of it

	function mrId(key)    { if (!(key in _mrIds)) _mrIds[key] = 'c2mr' + (++_mrSeq); return _mrIds[key]; }
	function mrState(key) { return S.mr[key] || (S.mr[key] = { view: 'rules', edit: null, testOpen: false, test: null, testing: false, testRes: null }); }
	function mrIsJson(v)  { var t = String(v || '').trim(); return t.charAt(0) === '[' || t.charAt(0) === '{'; }

	/** null for a plain address list, { rules: [...] } for JSON rules, or { error } for JSON that can't be used */
	function mrParse(v)
	{
		if (!mrIsJson(v)) return null;
		try
		{
			var a = JSON.parse(v);
			return Array.isArray(a) ? { rules: a } : { error: 'The JSON must be an array of rules: [ {...}, {...} ]' };
		}
		catch (e) { return { error: 'Invalid JSON: ' + e.message }; }
	}

	function mrPost(body)
	{
		return fetch('/api/cc/mgt/writer/try?srvName=' + encodeURIComponent(S.srvName), {
			method  : 'POST',
			headers : { 'Content-Type': 'application/json' },
			body    : JSON.stringify(body)
		})
		.then(function(r) { return r.text().then(function(txt) { var j = {}; try { j = JSON.parse(txt); } catch (e) {} return { status: r.status, json: j }; }); })
		.catch(function(err) { return { status: 0, json: { message: String(err) } }; });
	}
	function mrFailText(res) { return res.json.message || res.json.error || ('HTTP ' + res.status); }

	/** Ask the collector to validate this value (debounced, once per distinct value), then re-render */
	function mrValidateLater(value, propKey)
	{
		if (value in _mrVal || _mrValBusy[value]) return;
		clearTimeout(_mrValTimer);
		_mrValTimer = setTimeout(function()
		{
			_mrValBusy[value] = true;
			mrPost({ op: 'mailRouting', value: value, propKey: propKey }).then(function(res) // propKey: for a user that is not admin the collector checks the SAVED value
			{
				delete _mrValBusy[value];
				_mrVal[value] = res.status === 200 ? res.json : { failed: mrFailText(res) };
				render();
			});
		}, 400);
	}

	function mrLoadInfo()
	{
		if (S.mrInfo) return;
		S.mrInfo = { loading: true };
		mrPost({ op: 'mailRoutingInfo' }).then(function(res)
		{
			S.mrInfo = res.status === 200 ? res.json : { failed: mrFailText(res) };
			render();
		});
	}

	/** validateField() for 'mailRouting': the JSON syntax here, the rules by the collector (once it has answered for this value) */
	function mrValidateField(value)
	{
		var p = mrParse(value);
		if (p && p.error) return { error: p.error };
		var v = _mrVal[value];
		if (v && !v.failed && v.errors && v.errors.length)
			return { error: v.errors.length + ' problem' + (v.errors.length === 1 ? '' : 's') + ' found by the collector, see the rules (the collector ignores rules with problems)' };
		return {};
	}

	/** Change the rules: fn(rules) edits the array in place */
	function mrChange(key, fn)
	{
		var p = mrParse(cur(key, S.reg[key].orig));
		var rules = p && p.rules ? p.rules : [];
		fn(rules);
		setPend(key, JSON.stringify(rules));
	}

	/** 'YYYY-MM-DDTHH:MM' of now, for a datetime-local input */
	function mrLocalNow()
	{
		var d = new Date();
		return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().substring(0, 16);
	}

	function mrBtn(key, act, arg, html, title, cls)
	{
		return '<button type="button" class="' + (cls || 'c2-icon-btn') + '" data-mr-key="' + esc(key) + '" data-mr-act="' + act + '"'
			+ (arg !== null && arg !== undefined ? ' data-mr-arg="' + esc(arg) + '"' : '') + (title ? ' title="' + esc(title) + '"' : '') + '>' + html + '</button>';
	}

	function mrEditor(key, f, val, ro)
	{
		mrLoadInfo();
		var st = mrState(key), id = mrId(key), info = S.mrInfo || {};
		var parsed = mrParse(val);
		var v = _mrVal[val];
		if (!v) mrValidateLater(val, S.reg[key].property);
		var dis = ro ? ' disabled' : '';
		var ka  = ' data-mr-key="' + esc(key) + '"';
		var h = '<div class="c2-mr">';

		// Suggestions for the edit form and the tester (free text is still allowed: they are regexps)
		['alarmNames', 'categories', 'severities', 'states', 'actions'].forEach(function(n) {
			var sugg = (info[n] || []).concat(n === 'severities' ? ['WARNING|ERROR', '(?!INFO).*'] : []); // + the usual "skip INFO" regexps
			h += '<datalist id="' + id + '-dl-' + n + '">' + sugg.map(function(x) { return '<option value="' + esc(x) + '">'; }).join('') + '</datalist>';
		});

		var status = !v     ? '<i class="fa-solid fa-spinner fa-spin"></i> Checking with the collector...'
		           : v.failed ? '<span class="c2-warn-i"><i class="fa-solid fa-triangle-exclamation"></i> Could not check with the collector: ' + esc(v.failed) + '</span>'
		           : v.errors && v.errors.length ? '<span class="c2-err-i"><i class="fa-solid fa-circle-xmark"></i> ' + v.errors.length + ' problem' + (v.errors.length === 1 ? '' : 's') + ' (the collector ignores those rules)</span>'
		           : '<span class="c2-ok-i"><i class="fa-solid fa-circle-check"></i> OK, checked by the collector</span>';
		h += '<div class="c2-mr-bar">';
		if (parsed && !parsed.error)
			h += mrBtn(key, 'view', 'rules', 'Rules', null, 'c2-tab' + (st.view === 'rules' ? ' c2-on' : '')) + mrBtn(key, 'view', 'raw', 'Raw JSON', null, 'c2-tab' + (st.view === 'raw' ? ' c2-on' : ''));
		h += '<span class="c2-mr-status">' + status + '</span></div>';

		if (!parsed)
		{
			// A plain address list
			h += '<input type="text" class="c2-in' + (key in S.pend ? ' c2-pend' : '') + '" data-k="' + esc(key) + '" value="' + esc(val) + '"' + dis + ' spellcheck="false" placeholder="a@acme.com, b@acme.com">';
			h += '<div class="c2-help">A plain list of addresses: everybody gets every alarm.' + (ro ? '' : ' To send different alarms to different people, convert it to rules.') + '</div>';
			if (!ro) h += '<div class="c2-mr-acts">' + mrBtn(key, 'convert', null, '<i class="fa-solid fa-list-check"></i> Convert to rules', null, 'c2-btn c2-btn-sm') + '</div>';
		}
		else if (parsed.error || st.view === 'raw')
		{
			h += '<textarea id="' + id + '-raw" class="c2-in c2-mr-raw' + (key in S.pend ? ' c2-pend' : '') + (parsed.error ? ' c2-bad' : '') + '" rows="8" spellcheck="false"' + ka + ' data-mr-raw="1"' + dis + '>' + esc(val) + '</textarea>';
		}
		else
		{
			var known = MR_CONDS.concat(['name', 'enabled', 'fallback', 'to', 'cc', 'comment']);
			var vr = (v && v.rules) || [];
			h += '<table class="c2-mr-tbl"><thead><tr><th>#</th><th title="Enabled">On</th><th>Name</th><th>When</th><th>Who gets it</th><th></th></tr></thead><tbody>';
			parsed.rules.forEach(function(r, i)
			{
				var sv    = vr[i] || null; // the collector's verdict for this rule
				var isObj = r !== null && typeof r === 'object' && !Array.isArray(r);
				var rr    = isObj ? r : {};
				var on    = String(rr.enabled).toLowerCase() !== 'false';
				var conds = '';
				if (String(rr.fallback).toLowerCase() === 'true') conds += '<span class="c2-mr-cond c2-mr-fb">fallback: when no other rule matched</span>';
				MR_CONDS.forEach(function(k) {
					if (rr[k] === undefined || rr[k] === '') return;
					var skip = S.reg[key].type === 'dsr' && MR_ALARM_CONDS.indexOf(k) !== -1; // the report's recipients: an alarm condition never matches a report
					conds += '<span class="c2-mr-cond' + (skip ? ' c2-mr-unk' : '') + '"' + (skip ? ' title="An alarm condition: this rule is skipped for reports"' : '') + '><b>' + esc(k) + '</b> ' + esc(rr[k]) + '</span>';
				});
				Object.keys(rr).forEach(function(k) { if (known.indexOf(k) === -1 && k.charAt(0) !== '_') conds += '<span class="c2-mr-cond c2-mr-unk" title="Unknown key">' + esc(k) + '?</span>'; });
				if (!isObj) conds += '<span class="c2-mr-cond c2-mr-unk">' + esc(JSON.stringify(r)) + '</span>';
				var who = (rr.to ? '<div><span class="c2-mr-lbl">To</span> ' + esc(rr.to) + '</div>' : '') + (rr.cc ? '<div><span class="c2-mr-lbl">Cc</span> ' + esc(rr.cc) + '</div>' : '');

				h += '<tr class="c2-mr-r' + (sv && !sv.valid ? ' c2-mr-rbad' : '') + (on ? '' : ' c2-mr-roff') + (st.edit === i ? ' c2-mr-rsel' : '') + '">'
					+ '<td class="c2-mr-n">' + (i + 1) + '</td>'
					+ '<td><input type="checkbox" id="' + id + '-' + i + '-enabled"' + ka + ' data-mr-idx="' + i + '" data-mr-field="enabled"' + (on ? ' checked' : '') + (ro || !isObj ? ' disabled' : '') + ' title="Enabled"></td>'
					+ '<td>' + esc(rr.name || '') + '</td>'
					+ '<td>' + conds + '</td>'
					+ '<td class="c2-mr-who">' + who + '</td>'
					+ '<td class="c2-mr-rowacts">'
					+ (isObj ? mrBtn(key, 'edit', i, '<i class="fa-solid ' + (ro ? 'fa-eye' : 'fa-pen') + '"></i>', ro ? 'Show' : 'Edit') : '')
					+ (ro ? '' : mrBtn(key, 'copy', i, '<i class="fa-regular fa-copy"></i>', 'Copy') + mrBtn(key, 'del', i, '<i class="fa-regular fa-trash-can"></i>', 'Delete'))
					+ '</td></tr>';

				if (sv && (sv.errors.length || sv.warnings.length))
					h += '<tr class="c2-mr-sub"><td></td><td colspan="5">'
						+ sv.errors  .map(function(m) { return '<div class="c2-err">' + esc(m) + ' (the collector ignores this rule)</div>'; }).join('')
						+ sv.warnings.map(function(m) { return '<div class="c2-warn">' + esc(m) + '</div>'; }).join('') + '</td></tr>';

				if (st.edit === i && isObj)
				{
					h += '<tr class="c2-mr-sub"><td></td><td colspan="5"><div class="c2-mr-form">';
					var isReport = S.reg[key].type === 'dsr';
					MR_FIELDS.forEach(function(fd)
					{
						// The report's recipients: a report has no alarm, so a rule with an alarm condition never matches it.
						// Do not offer those fields, but still show one that has a value (so it can be seen, and cleared).
						var alarmOnly = MR_ALARM_CONDS.indexOf(fd[0]) !== -1;
						if (isReport && alarmOnly && (rr[fd[0]] === undefined || rr[fd[0]] === ''))
							return;
						var fid = id + '-' + i + '-' + fd[0];
						h += '<label for="' + fid + '">' + esc(fd[1]) + (isReport && alarmOnly ? '<div class="c2-err-i">skipped for reports</div>' : '') + '</label>'
							+ '<input type="text" class="c2-in" id="' + fid + '"' + ka + ' data-mr-idx="' + i + '" data-mr-field="' + fd[0] + '"'
							+ ' value="' + esc(rr[fd[0]] === undefined ? '' : rr[fd[0]]) + '"' + (fd[3] ? ' list="' + id + '-dl-' + fd[3] + '"' : '')
							+ ' placeholder="' + esc(fd[2]) + '" spellcheck="false"' + dis + '>';
					});
					var fb = String(rr.fallback).toLowerCase() === 'true';
					h += '<span></span><label class="c2-sw' + (ro ? ' c2-ro' : '') + '"><input type="checkbox" id="' + id + '-' + i + '-fallback"' + ka + ' data-mr-idx="' + i + '" data-mr-field="fallback"' + (fb ? ' checked' : '') + dis + '><i></i>'
						+ 'Fallback: only used when no other rule matched (then it has no conditions)</label>';
					h += '</div><div class="c2-mr-acts">' + mrBtn(key, 'done', i, 'Done', null, 'c2-btn c2-btn-sm') + '</div></td></tr>';
				}
			});
			h += '</tbody></table>';
			if (!parsed.rules.length) h += '<div class="c2-empty">No rules: nobody gets any mail.</div>';
			if (!ro)
				h += '<div class="c2-mr-acts">' + mrBtn(key, 'add', null, '<i class="fa-solid fa-plus"></i> Add rule', null, 'c2-btn c2-btn-sm')
					+ mrBtn(key, 'addfb', null, '<i class="fa-solid fa-plus"></i> Add fallback', 'Who gets the alarms that no other rule matched', 'c2-btn c2-btn-sm') + '</div>';
		}
		if (v && v.jsonError) h += '<div class="c2-err">Collector: ' + esc(v.jsonError) + '</div>';

		// Tester: for the Daily Summary Report who gets THIS server's report, else who would get an alarm
		if (S.reg[key].type === 'dsr')
			h += mrReportWho(key, val, info);
		else
		{
			h += '<div class="c2-mr-acts">' + mrBtn(key, 'testopen', null, '<i class="fa-solid fa-flask"></i> Who would get this alarm?', 'Try the rules as they are in the editor (unsaved), with the collector\'s own code', 'c2-btn c2-btn-sm' + (st.testOpen ? ' c2-on' : '')) + '</div>';
			if (st.testOpen)
			{
				if (!st.test)
					st.test = { serverName: (S.cfg && S.cfg.srvName) || S.srvName, serverGroup: info.serverGroup || '', alarmName: '', severity: 'ERROR', category: 'OTHER', state: 'AFFECTED', action: 'RAISE', time: mrLocalNow() };
				var t = st.test;
				var inp = function(k, lbl, list, type) {
					return '<label for="' + id + '-t-' + k + '">' + lbl + '</label><input type="' + (type || 'text') + '" class="c2-in" id="' + id + '-t-' + k + '"' + ka + ' data-mr-test="' + k + '" value="' + esc(t[k]) + '"'
						+ (list ? ' list="' + id + '-dl-' + list + '"' : '') + ' spellcheck="false">';
				};
				var sel = function(k, lbl, values) {
					return '<label for="' + id + '-t-' + k + '">' + lbl + '</label><select class="c2-in" id="' + id + '-t-' + k + '"' + ka + ' data-mr-test="' + k + '">'
						+ (values || []).map(function(x) { return '<option' + (x === t[k] ? ' selected' : '') + '>' + esc(x) + '</option>'; }).join('') + '</select>';
				};
				h += '<div class="c2-mr-test"><div class="c2-mr-form">'
					+ inp('serverName', 'Server name') + inp('serverGroup', 'Server group') + inp('alarmName', 'Alarm name', 'alarmNames')
					+ sel('severity', 'Severity', info.severities || ['INFO', 'WARNING', 'ERROR'])
					+ sel('category', 'Category', info.categories || ['OTHER'])
					+ sel('state',    'State',    info.states     || ['UP', 'AFFECTED', 'DOWN'])
					+ sel('action',   'Action',   (info.actions || ['RAISE', 'CANCEL']).filter(function(a) { return a !== 'RE-RAISE'; }))
					+ inp('time', 'Raised at', null, 'datetime-local')
					+ '</div><div class="c2-help">Server group empty = not in a group (a rule with a server group then never matches). The time is your browser\'s local time.</div>'
					+ '<div class="c2-mr-acts">' + mrBtn(key, 'test', null, st.testing ? '<i class="fa-solid fa-spinner fa-spin"></i> Testing...' : '<i class="fa-solid fa-play"></i> Test', null, 'c2-btn c2-btn-sm c2-btn-pri') + '</div>';
				if (st.testRes)
				{
					if (st.testRes.failed)
						h += '<div class="c2-warn">Could not test: ' + esc(st.testRes.failed) + '</div>';
					else
					{
						var rs = st.testRes.json.result || { to: [], cc: [], matchedRules: [] };
						h += '<div class="c2-mr-res">'
							+ (rs.to.length || rs.cc.length
								? '<div><span class="c2-mr-lbl">To</span> ' + esc(rs.to.join(', ') || '-') + '</div>' + (rs.cc.length ? '<div><span class="c2-mr-lbl">Cc</span> ' + esc(rs.cc.join(', ')) + '</div>' : '')
								  + '<div class="c2-help">Rule' + (rs.matchedRules.length === 1 ? '' : 's') + ': ' + esc(rs.matchedRules.join(', ')) + (rs.usedFallback ? ' (the fallback, no other rule matched)' : '') + '</div>'
								: '<div class="c2-err-i">Nobody gets this mail: no rule matched, and there is no fallback rule.</div>')
							+ (st.testRes.value !== val ? '<div class="c2-warn">The rules have changed since this test, press Test again.</div>' : '')
							+ '</div>';
					}
				}
				h += '</div>';
			}
		}

		if (f.description)
			h += '<details class="c2-mr-help"><summary>How the rules work</summary><div class="c2-help c2-richhelp">' + safeHtml(f.description) + '</div></details>';
		return h + '</div>';
	}

	var _mrRep = {}, _mrRepBusy = {}, _mrRepTimer = null; // 'server|value' -> the collector's answer for this server's report

	/**
	 * "Who gets this server's report?" for the Daily Summary Report's recipients: asked from the collector with the rules in the
	 * editor, for THIS server and no alarm (so rules with an alarm condition are skipped, as when the report is sent). No inputs.
	 */
	function mrReportWho(key, val, info)
	{
		var srv = (S.cfg && S.cfg.srvName) || S.srvName;
		var rk  = srv + '|' + val;
		var res = _mrRep[rk];
		if (!res && !_mrRepBusy[rk])
		{
			clearTimeout(_mrRepTimer);
			_mrRepTimer = setTimeout(function()
			{
				_mrRepBusy[rk] = true;
				mrPost({ op: 'mailRouting', value: val, propKey: S.reg[key] ? S.reg[key].property : '', sample: { serverName: srv, serverGroup: info.serverGroup || null } })
				.then(function(r)
				{
					delete _mrRepBusy[rk];
					_mrRep[rk] = r.status === 200 ? r.json : { failed: mrFailText(r) };
					render();
				});
			}, 400);
		}

		var h = '<div class="c2-mr-test"><div><b>Who gets this server\'s report?</b> <span class="c2-help">(' + esc(srv) + ')</span></div>';
		if (!res)       return h + '<div class="c2-help"><i class="fa-solid fa-spinner fa-spin"></i> Asking the collector...</div></div>';
		if (res.failed) return h + '<div class="c2-warn">Could not ask the collector: ' + esc(res.failed) + '</div></div>';

		var rs      = res.result || { to: [], cc: [], matchedRules: [] };
		var all     = rs.to.concat(rs.cc.filter(function(a) { return rs.to.indexOf(a) === -1; })); // the report sender adds a rule's cc to To
		var skipped = (res.rules || []).filter(function(r) { return r.alarmOnly; }).map(function(r) { return r.name || ('rule ' + (r.index + 1)); });
		h += '<div class="c2-mr-res">'
			+ (all.length
				? '<div><span class="c2-mr-lbl">To</span> ' + esc(all.join(', ')) + '</div>'
				  + '<div class="c2-help">Rule' + (rs.matchedRules.length === 1 ? '' : 's') + ': ' + esc(rs.matchedRules.join(', ')) + (rs.usedFallback ? ' (the fallback, no other rule matched)' : '')
				  + (rs.cc.length ? '. A report has no Cc: the cc addresses of the rules are added to To.' : '') + '</div>'
				: '<div class="c2-err-i">Nobody gets this server\'s report: no rule matched, and there is no fallback rule.</div>')
			+ (skipped.length ? '<div class="c2-help">Skipped for reports (an alarm condition, eg severity, never matches a report): ' + esc(skipped.join(', ')) + '</div>' : '')
			+ (res.savedOnly ? '<div class="c2-help">These are the SAVED rules (log in as admin to try changes before saving).</div>' : '')
			+ '</div>';
		return h + '</div>';
	}

	function mrOnInput(t, e)
	{
		var key = t.getAttribute('data-mr-key');
		if (t.hasAttribute('data-mr-test')) // tester inputs: just remember them (no re-render while typing)
		{
			mrState(key).test[t.getAttribute('data-mr-test')] = t.value;
			return;
		}
		var isChk = t.type === 'checkbox';
		if (isChk !== (e.type === 'change')) return;
		if (t.hasAttribute('data-mr-raw')) { setPend(key, t.value); render(); return; }

		var idx = parseInt(t.getAttribute('data-mr-idx'), 10), field = t.getAttribute('data-mr-field');
		mrChange(key, function(rules)
		{
			var r = rules[idx];
			if (r === null || typeof r !== 'object') return;
			if      (field === 'enabled')  { if (t.checked) delete r.enabled;  else r.enabled = false; }
			else if (field === 'fallback') { if (t.checked) r.fallback = true; else delete r.fallback; }
			else if (t.value === '')       delete r[field];
			else                           r[field] = t.value;
		});
		render();
	}

	function mrOnClick(t)
	{
		var key = t.getAttribute('data-mr-key'), act = t.getAttribute('data-mr-act'), arg = t.getAttribute('data-mr-arg');
		var st = mrState(key), i = parseInt(arg, 10);
		switch (act)
		{
		case 'view':     st.view = arg; break;
		case 'edit':     st.edit = st.edit === i ? null : i; break;
		case 'done':     st.edit = null; break;
		case 'add':      mrChange(key, function(rules) { rules.push({ name: '', serverName: '.*', to: '' }); st.edit = rules.length - 1; }); break;
		case 'addfb':    mrChange(key, function(rules) { rules.push({ fallback: true, to: '' }); st.edit = rules.length - 1; }); break;
		case 'copy':     mrChange(key, function(rules) { rules.splice(i + 1, 0, JSON.parse(JSON.stringify(rules[i]))); st.edit = i + 1; }); break;
		case 'del':
			if (!window.confirm('Delete rule #' + (i + 1) + '?')) return;
			mrChange(key, function(rules) { rules.splice(i, 1); });
			st.edit = null;
			break;
		case 'convert':
			setPend(key, JSON.stringify([{ name: 'Everybody', serverName: '.*', to: String(cur(key, S.reg[key].orig)).trim() }]));
			st.view = 'rules';
			break;
		case 'testopen': st.testOpen = !st.testOpen; break;
		case 'test':
			if (st.testing) return;
			var tv = cur(key, S.reg[key].orig), tt = st.test, ms = tt.time ? new Date(tt.time).getTime() : 0;
			st.testing = true;
			mrPost({ op: 'mailRouting', value: tv, propKey: S.reg[key].property, sample: { serverName: tt.serverName, serverGroup: tt.serverGroup || null, alarmName: tt.alarmName || '',
			                                                   category: tt.category, severity: tt.severity, state: tt.state, action: tt.action, time: isNaN(ms) ? 0 : ms } })
			.then(function(res)
			{
				st.testing = false;
				st.testRes = res.status === 200 ? { value: tv, json: res.json } : { failed: mrFailText(res) };
				render();
			});
			break;
		}
		render();
	}

	//------------------------------------------------------------------
	// Template editor, field kind 'template' (the setting has a TemplateInputValidator, eg AlarmWriterToMail.msg.body.template)
	// The text is a Velocity template. The COLLECTOR renders it, through the writer's own createTemplatePreview() - the renderer the
	// Swing Template Editor uses - so this page never re-implements a writer's variables or escaping. Same endpoint as the mail rules.
	//------------------------------------------------------------------
	var TPL_ACTIONS = ['RAISE', 'RE-RAISE', 'CANCEL'];
	var _tplIds = {}, _tplSeq = 0;      // field key -> id prefix (stable over re-renders, for the focus restore)
	var _tplInfo = {};                  // 'writer|propKey' -> answer of op 'templateInfo': variables, alternatives, hasDesignerCard
	var _tplRes = {}, _tplBusy = {};    // JSON of the request -> answer of op 'templatePreview'
	var _tplValid = {};                 // 'writer|propKey|value' -> the collector's save check: '' = OK, else why it would be refused
	var _tplTimer = {};                 // field key -> debounce timer

	function tplId(key)    { if (!(key in _tplIds)) _tplIds[key] = 'c2tp' + (++_tplSeq); return _tplIds[key]; }
	function tplState(key) { return S.tpl[key] || (S.tpl[key] = { preview: false, vars: false, action: 'RAISE', last: null, card: null }); }
	function tplRows(v)    { return Math.max(3, Math.min(24, String(v).split('\n').length + 1)); }

	/** The request that renders this field's value: with this writer's other unsaved values (the Teams card shows the edited title) */
	function tplRequest(key, value)
	{
		var m = S.reg[key], values = {};
		Object.keys(S.pend).forEach(function(k) {
			var p = S.pend[k];
			if (k !== key && p.type === 'writer' && p.writer === m.writer && p.property) values[p.property] = p.value;
		});
		return { op: 'templatePreview', writerName: m.writer, propKey: m.property, template: value, action: tplState(key).action, values: values };
	}

	/** Render on the collector (debounced per field, once per distinct request), then re-render the page */
	function tplRenderLater(key, req)
	{
		var rk = JSON.stringify(req);
		if (rk in _tplRes || _tplBusy[rk]) return;
		clearTimeout(_tplTimer[key]);
		_tplTimer[key] = setTimeout(function()
		{
			_tplBusy[rk] = true;
			mrPost(req).then(function(res)
			{
				delete _tplBusy[rk];
				_tplRes[rk] = res.status === 200 ? res.json : { failed: mrFailText(res) };
				if (res.status === 200) _tplValid[req.writerName + '|' + req.propKey + '|' + req.template] = res.json.validationError || '';
				render();
			});
		}, 500);
	}

	function tplLoadInfo(m)
	{
		var ik = m.writer + '|' + m.property;
		if (!_tplInfo[ik])
		{
			_tplInfo[ik] = { loading: true };
			mrPost({ op: 'templateInfo', writerName: m.writer, propKey: m.property }).then(function(res)
			{
				_tplInfo[ik] = res.status === 200 ? res.json : { failed: mrFailText(res) };
				render();
			});
		}
		return _tplInfo[ik];
	}

	/** validateField() for 'template': blocking when the collector said it would refuse to save this value */
	function tplValidateField(m, value)
	{
		var why = m ? _tplValid[m.writer + '|' + m.property + '|' + value] : '';
		return why ? { error: 'The collector refuses to save a template that does not render. ' + why } : {};
	}

	function tplBtn(key, op, arg, html, title, cls)
	{
		return '<button type="button" class="' + (cls || 'c2-btn c2-btn-sm') + '" data-tpl-key="' + esc(key) + '" data-tpl-do="' + op + '"'
			+ (arg !== null && arg !== undefined ? ' data-tpl-arg="' + esc(arg) + '"' : '') + (title ? ' title="' + esc(title) + '"' : '') + '>' + html + '</button>';
	}

	/** Copy to the clipboard: navigator.clipboard (https or localhost, and the browser may refuse it), else a selected textarea */
	function copyText(text)
	{
		if (navigator.clipboard && window.isSecureContext)
			return navigator.clipboard.writeText(text).then(function() { return true; }, function() { return copyTextOld(text); });
		return Promise.resolve(copyTextOld(text));
	}
	function copyTextOld(text)
	{
		var ta = document.createElement('textarea');
		ta.value = text;
		ta.style.position = 'fixed';
		ta.style.opacity = '0';
		document.body.appendChild(ta);
		ta.select();
		var ok = false;
		try { ok = document.execCommand('copy'); } catch (e) {}
		document.body.removeChild(ta);
		return ok;
	}

	function tplEditor(key, f, val, ro)
	{
		var st = tplState(key), id = tplId(key), m = S.reg[key], ka = ' data-tpl-key="' + esc(key) + '"';
		var info = tplLoadInfo(m);

		// Render when there is something to check (a changed value) or to show (the preview)
		var req = (st.preview || key in S.pend) ? tplRequest(key, val) : null;
		var res = req ? _tplRes[JSON.stringify(req)] : null;
		if (req && !res) tplRenderLater(key, req);
		if (res && !res.failed) st.last = res;
		var err = res && !res.failed && res.error ? res.error : null;

		var h = '<div class="c2-tpl"><div class="c2-mr-bar">'
			+ tplBtn(key, 'preview', null, '<i class="fa-regular fa-eye"></i> Preview', 'Render it on the collector, with an example alarm, the way the writer renders it', 'c2-tab' + (st.preview ? ' c2-on' : ''))
			+ tplBtn(key, 'vars', null, '<i class="fa-solid fa-code"></i> Variables', 'The variables a template can use', 'c2-tab' + (st.vars ? ' c2-on' : ''))
			+ '<label class="c2-tpl-actlbl" for="' + id + '-act">as</label><select class="c2-in c2-tpl-act" id="' + id + '-act"' + ka + ' data-tpl-action="1" title="Render it for this alarm action">'
			+ TPL_ACTIONS.map(function(a) { return '<option' + (a === st.action ? ' selected' : '') + '>' + a + '</option>'; }).join('') + '</select>';
		if (req)
		{
			var status = !res      ? '<i class="fa-solid fa-spinner fa-spin"></i> Rendering on the collector...'
			           : res.failed ? '<span class="c2-warn-i"><i class="fa-solid fa-triangle-exclamation"></i> Could not render it on the collector: ' + esc(res.failed) + '</span>'
			           : err        ? '<span class="c2-err-i"><i class="fa-solid fa-circle-xmark"></i> ' + esc(err.type) + (err.line > 0 ? ' at line ' + err.line + ', column ' + err.column : '') + '</span>'
			           : '<span class="c2-ok-i"><i class="fa-solid fa-circle-check"></i> OK, rendered by the collector</span>';
			h += '<span class="c2-mr-status">' + status + (err && err.line > 0 ? ' ' + tplBtn(key, 'goto', err.line, 'Go to line', 'Select that line in the template') : '') + '</span>';
		}
		h += '</div>';

		// A leading newline: the HTML parser drops the first newline after <textarea>, this keeps a template that starts with one
		h += '<textarea id="' + id + '-txt" class="c2-in c2-tpl-txt' + (key in S.pend ? ' c2-pend' : '') + (err ? ' c2-bad' : '') + '" rows="' + tplRows(val) + '"'
			+ ' spellcheck="false" wrap="off"' + ka + ' data-tpl-txt="1"' + (ro ? ' readonly' : '') + '>\n' + esc(val) + '</textarea>';

		// Message details when the save check did not already say it (fieldRow shows that one)
		if (err && !tplValidateField(m, val).error)
			h += '<div class="c2-err">' + esc(err.message) + '</div>';

		if (!ro && (info.alternatives || []).length || info.hasDesignerCard)
		{
			h += '<div class="c2-mr-acts">';
			if (!ro)
				(info.alternatives || []).forEach(function(a, i) {
					h += tplBtn(key, 'alt', i, '<i class="fa-solid fa-file-import"></i> Use the ' + esc(a.label), 'Replace the text with the writer\'s ' + a.label + ' template');
				});
			if (info.hasDesignerCard)
				h += tplBtn(key, 'card', null, '<i class="fa-regular fa-copy"></i> Copy card for Designer', 'Copy just the Adaptive Card (without the Teams message envelope) for https://adaptivecards.io/designer');
			h += '</div>';
		}
		if (st.card)
			h += '<div class="c2-tpl-prev"><div class="c2-tpl-prevh">The card for the Adaptive Card Designer - select it and copy it ' + tplBtn(key, 'cardclose', null, 'Close') + '</div>'
				+ '<textarea id="' + id + '-card" class="c2-in c2-tpl-txt" rows="10" readonly spellcheck="false" wrap="off">\n' + esc(st.card) + '</textarea></div>';

		if (st.vars)
		{
			if (!info.variables)
				h += '<div class="c2-help">' + (info.failed ? 'Could not get the variables from the collector: ' + esc(info.failed) : '<i class="fa-solid fa-spinner fa-spin"></i> Loading the variables...') + '</div>';
			else
			{
				h += '<div class="c2-tpl-vars">' + info.variables.map(function(v, i) {
					var t = htmlToText(v.desc);
					return ro ? '<span class="c2-tpl-var" title="' + esc(t) + '">' + esc(v.name) + '</span>'
					          : tplBtn(key, 'var', i, esc(v.name), t + '  Inserts: ' + v.insert, 'c2-tpl-var');
				}).join('') + '</div>';
				h += '<div class="c2-help">' + (ro ? '' : 'Click one to insert it at the cursor. ') + 'Hover for a description. The ones marked "Teams card template only" work in the Teams card template only.</div>';
				if (info.help)
					h += '<details class="c2-mr-help"><summary>How templates work</summary><div class="c2-help c2-richhelp">' + safeHtml(info.help) + '</div></details>';
			}
		}

		// The preview: the latest answer (the previous one while a new one is on its way, so it does not blink while typing)
		var shown = st.preview ? (res && !res.failed ? res : st.last) : null;
		if (shown)
		{
			var out = shown.text !== undefined ? shown.text : shown.previewText;
			if (out !== undefined && out !== null)
			{
				var isHtml = /^\s*<html/i.test(out);
				h += '<div class="c2-tpl-prev"><div class="c2-tpl-prevh">'
					+ (shown.text === undefined ? '<span class="c2-err-i">What it renders - NOT usable, see the error</span>' : 'Preview: <b>' + esc(st.action) + '</b> of an example alarm' + (isHtml ? ', as an HTML mail' : ''))
					+ (shown !== res ? ' <i class="fa-solid fa-spinner fa-spin" title="Updating"></i>' : '') + '</div>'
					// sandbox="": no scripts, no same-origin access - nothing in the rendered mail can reach this page
					+ (isHtml ? '<iframe class="c2-tpl-frame" sandbox="" title="Preview of the rendered mail" srcdoc="' + esc(out) + '"></iframe>'
					          : '<pre class="c2-tpl-pre">' + esc(out) + '</pre>')
					+ '</div>';
			}
		}
		return h + '</div>';
	}

	function tplOnInput(t, e)
	{
		var key = t.getAttribute('data-tpl-key');
		if (t.hasAttribute('data-tpl-action'))
		{
			if (e.type === 'change') { tplState(key).action = t.value; render(); }
			return;
		}
		if (e.type !== 'input' || !S.reg[key]) return;

		var wasPend = key in S.pend;
		setPend(key, t.value);
		if (wasPend && key in S.pend)
		{
			// Typing: no re-render on every key (it would rebuild the preview), just this field and the save bar. The collector's answer re-renders.
			t.rows = tplRows(t.value);
			renderSaveBar();
			tplRenderLater(key, tplRequest(key, t.value));
			return;
		}
		render();
	}

	function tplOnClick(t)
	{
		var key = t.getAttribute('data-tpl-key'), op = t.getAttribute('data-tpl-do'), i = parseInt(t.getAttribute('data-tpl-arg'), 10);
		var st = tplState(key), m = S.reg[key], ta = el(tplId(key) + '-txt');
		if (!m) return;
		var info = _tplInfo[m.writer + '|' + m.property] || {};
		switch (op)
		{
		case 'preview':   st.preview = !st.preview; break;
		case 'vars':      st.vars    = !st.vars;    break;
		case 'cardclose': st.card    = null;        break;
		case 'goto':
			if (!ta) return;
			var lines = ta.value.split('\n'), from = 0;
			for (var n = 0; n < i - 1 && n < lines.length; n++) from += lines[n].length + 1;
			ta.focus();
			ta.setSelectionRange(from, from + (lines[i - 1] || '').length);
			ta.scrollTop = Math.max(0, (i - 3) * (ta.scrollHeight / Math.max(1, lines.length)));
			return;
		case 'var':
			var v = (info.variables || [])[i];
			if (!v || !ta || ta.readOnly) return;
			var a = ta.selectionStart, b = ta.selectionEnd, top = ta.scrollTop;
			setPend(key, ta.value.substring(0, a) + v.insert + ta.value.substring(b));
			render();
			// the click moved the focus to the button: back to the template, with the cursor after the insert
			var nt = el(tplId(key) + '-txt');
			if (nt) { nt.focus(); nt.setSelectionRange(a + v.insert.length, a + v.insert.length); nt.scrollTop = top; }
			return;
		case 'alt':
			var alt = (info.alternatives || [])[i];
			if (!alt) return;
			if (String(cur(key, m.orig)).trim() !== '' && !window.confirm('Replace the template with the ' + alt.label + '?')) return;
			setPend(key, alt.value);
			break;
		case 'card':
			mrPost(Object.assign(tplRequest(key, cur(key, m.orig)), { designerCard: true })).then(function(res)
			{
				var j = res.json;
				if (res.status !== 200 || !j.card)
				{
					S.message = { type: 'err', text: 'No card to copy: ' + (res.status !== 200 ? mrFailText(res) : j.cardError || (j.error && j.error.message) || 'the template does not render') };
					render();
					return;
				}
				copyText(j.card).then(function(ok)
				{
					// When the browser refuses, show the card so it can be copied by hand
					st.card = ok ? null : j.card;
					S.message = ok ? { type: 'ok', text: 'The card is on the clipboard. Paste it into the Adaptive Card Designer (https://adaptivecards.io/designer, host app: Microsoft Teams).' }
					               : { type: 'warn', text: 'The browser did not allow copying to the clipboard: the card is shown below the template, select it and copy it.' };
					render();
				});
			});
			return;
		}
		render();
	}

	//------------------------------------------------------------------
	// Rendering: one field row (same grammar as the Alarm Editor dialog)
	//------------------------------------------------------------------
	/**
	 * @param key   pending/registry key
	 * @param f     field (DbxAlarmEditor.model shape)
	 * @param meta  { type, cmName, alarm?, writer?, wsub?, readOnly?, timeRangeDescr?, extraLbl? }
	 */
	function fieldRow(key, f, meta)
	{
		var label = f.kind === 'enabled' ? 'Enabled' : f.kind === 'cron' ? 'Time range' : f.label;
		S.reg[key] = { key: key, type: meta.type, cmName: meta.cmName, alarm: meta.alarm, writer: meta.writer, wsub: meta.wsub, property: f.property,
		               name: f.name, orig: f.orig, def: f.def, kind: f.kind, label: label };

		var ro    = !S.isAdmin || f.secret || meta.readOnly;
		var dis   = ro ? ' disabled' : '';
		var pend  = key in S.pend;
		var val   = cur(key, f.orig);
		var v     = validateField({ kind: f.kind, value: val, meta: S.reg[key] });
		var srvErr = S.errors[key];
		var h = '';

		var lbl = (f.kind === 'enabled' || f.isMain) ? '<b>' + esc(label) + '</b>' : esc(label);
		h += '<div class="c2-row' + (S.flashKeys[key] ? ' c2-flash' : '') + (f.orig !== f.def ? ' c2-row-mod' : '') + '" data-row="' + esc(key) + '">';
		h += '<div class="c2-lbl">' + lbl + (pend ? '<span class="c2-changed">changed</span>' : '')
			+ (f.orig !== f.def ? '<div><span class="c2-bdg c2-bdg-mod" title="The saved value differs from the default (' + esc(f.def === '' ? '(empty)' : f.def) + ')">modified</span></div>' : '')
			+ (meta.extraLbl || '') + '</div><div class="c2-val">';

		if (f.kind === 'enabled' || f.kind === 'bool')
		{
			var on = val === 'true';
			h += '<label class="c2-sw' + (pend ? ' c2-pend' : '') + (ro ? ' c2-ro' : '') + '"><input type="checkbox" data-k="' + esc(key) + '"' + (on ? ' checked' : '') + dis + '><i></i>'
				+ (f.kind === 'enabled' ? (on ? 'The alarm is on' : 'The alarm is off') : (on ? 'Yes' : 'No')) + '</label>';
		}
		else if (f.kind === 'cron')
		{
			var preset = presetFor(val);
			h += '<div class="c2-presets">';
			M.CRON_PRESETS.forEach(function(p) {
				h += '<button type="button" class="c2-preset' + (p === preset ? ' c2-on' : '') + '" data-k="' + esc(key) + '" data-set="' + esc(p.cron) + '" title="' + esc(p.cron) + '"' + dis + '>' + esc(p.label) + '</button>';
			});
			h += '</div>';
			h += '<input type="text" class="c2-in' + (pend ? ' c2-pend' : '') + (v.error ? ' c2-bad' : '') + '" data-k="' + esc(key) + '" value="' + esc(val) + '"' + dis + ' spellcheck="false">';
			var desc = preset ? preset.desc : (val === f.orig && meta.timeRangeDescr ? meta.timeRangeDescr : 'Custom (the collector describes it after saving)');
			h += '<div class="c2-help">' + esc(desc) + ' &middot; cron: minute hour day-of-month month day-of-week, a leading <code>!</code> means NOT within</div>';
		}
		else if (f.kind === 'mailRouting')
		{
			h += mrEditor(key, f, val, ro);
		}
		else if (f.kind === 'template')
		{
			h += tplEditor(key, f, val, ro);
		}
		else
		{
			var num = f.kind === 'int' || f.kind === 'double';
			h += '<input type="text" class="c2-in' + (num ? ' c2-num' : '') + (pend ? ' c2-pend' : '') + (v.error ? ' c2-bad' : '') + '" data-k="' + esc(key) + '" value="' + esc(val) + '"' + dis
				+ ' spellcheck="false" placeholder="' + (f.kind === 'regex' ? '(empty = no filter)' : '(empty)') + '">';
			if (f.kind === 'map')
			{
				h += '<div>';
				M.parseMap(val).forEach(function(e) {
					h += '<span class="c2-chip ' + (e.ok ? 'c2-chip-ok' : 'c2-chip-bad') + '">' + (e.ok ? esc(e.key) + ' &rarr; ' + esc(e.num) : 'invalid: ' + esc(e.text)) + '</span>';
				});
				h += '</div>';
			}
		}

		if (v.error) h += '<div class="c2-err">' + esc(v.error) + '</div>';
		if (srvErr)  h += '<div class="c2-err">Collector: ' + esc(htmlToText(srvErr)) + '</div>';
		if (v.warn)  h += '<div class="c2-warn">' + esc(v.warn) + '</div>';

		if (f.kind !== 'cron' && f.kind !== 'enabled' && f.kind !== 'mailRouting' && f.description) // mailRouting: inside the editor (it's long)
			h += '<div class="c2-help">' + esc(htmlToText(f.description)) + '</div>';

		if (f.kind === 'enabled')
		{
			if (f.source) h += '<div class="c2-def">' + sourceChips(f) + '</div>';
		}
		else
		{
			h += '<div class="c2-def">' + sourceChips(f) + '<span>default: <code>' + esc(f.def === '' ? '(empty)' : shortVal(f.def)) + '</code></span>';
			if (val !== f.def && !ro)
				h += '<button type="button" class="c2-btn c2-btn-sm" data-reset="' + esc(key) + '"><i class="fa-solid fa-rotate-left"></i> Reset to default</button>';
			if (f.property)
				h += '<code class="c2-prop" title="Property name">' + esc(f.property) + '</code>';
			h += '</div>';
		}
		h += '</div></div>';
		return h;
	}

	//------------------------------------------------------------------
	// Rendering: one alarm (collapsible), used by the Counters and the Alarm Overview tab
	//------------------------------------------------------------------
	function alarmBlock(cm, a, showCm)
	{
		var okey   = cm.cmName + '|' + a.name;
		var isOpen = !!S.open[okey];
		var fields = M.buildFields(a);
		var kf     = function(f) { return alarmKey(cm.cmName, a.name, f.name); };
		// buildFields() (shared with the Alarm Editor) does not carry 'source', add it from the parameters
		fields.forEach(function(f) {
			var p = (a.parameters || []).find(function(x) { return x.name === f.name; });
			if (p) { f.source = p.source; f.sourceInMemoryOnly = p.sourceInMemoryOnly; }
		});

		var enabledF = fields.find(function(f) { return f.kind === 'enabled'; });
		var cronF    = fields.find(function(f) { return f.kind === 'cron'; });
		var mainF    = fields.find(function(f) { return f.isMain; }) || fields.find(function(f) { return f.kind !== 'enabled' && f.kind !== 'cron'; });

		var enabled  = enabledF ? cur(kf(enabledF), enabledF.orig) === 'true' : a.isAlarmEnabled !== false;
		var cron     = cronF ? cur(kf(cronF), cronF.orig) : (a.timeRangeCron || '');
		var preset   = presetFor(cron);
		var mainVal  = mainF ? cur(kf(mainF), mainF.orig) : '';
		var modLines = fields.filter(function(f) { return f.orig !== f.def; }).map(function(f) {
			return (f.kind === 'enabled' ? 'Enabled' : f.kind === 'cron' ? 'Time range' : f.label) + ' = ' + (f.orig === '' ? '(empty)' : f.orig) + '  (default: ' + (f.def === '' ? '(empty)' : f.def) + ')' + sourceSuffix(f);
		});
		var modified = modLines.length > 0;
		var nPend    = fields.filter(function(f) { return kf(f) in S.pend; }).length;
		var nErr     = fields.filter(function(f) { return S.errors[kf(f)]; }).length;

		var h = '<div class="c2-al' + (isOpen ? ' c2-open' : '') + (enabled ? '' : ' c2-dis') + (modified ? ' c2-al-mod' : '') + '" data-al="' + esc(okey) + '">';
		h += '<div class="c2-al-h" data-toggle="' + esc(okey) + '" role="button" tabindex="0" aria-expanded="' + isOpen + '">';
		h += '<i class="fa-solid fa-chevron-right c2-car"></i>';
		h += '<span class="c2-al-n">' + esc(a.name)
			+ (showCm ? ' <span class="c2-al-cm">' + esc(cm.displayName || cm.cmName) + '</span>' : '')
			+ '<small>' + esc(htmlToText(a.description)) + '</small></span>';
		h += '<span class="c2-al-meta">';
		if (!enabled) h += '<span class="c2-pill">off</span>';
		if (mainF)    h += '<span class="c2-mono' + (mainVal !== mainF.def ? ' c2-mod' : '') + '" title="' + esc(mainF.label) + (mainVal !== mainF.def ? ' (default: ' + esc(mainF.def) + ')' : '') + '">' + esc(mainVal.length > 28 ? mainVal.substring(0, 26) + '...' : mainVal) + '</span>';
		h += '<span class="c2-pill" title="' + esc(cron) + '">' + esc(preset ? preset.label : 'custom time') + '</span>';
		if (modified) h += '<span class="c2-bdg c2-bdg-mod" title="' + esc(modifiedTitle(modLines)) + '">modified</span>';
		if (nPend)    h += '<span class="c2-bdg c2-bdg-pend" title="Unsaved changes">' + nPend + ' unsaved</span>';
		if (nErr)     h += '<span class="c2-bdg c2-bdg-err">error</span>';
		if (showCm)   h += '<button type="button" class="c2-icon-btn" data-goto="' + esc(okey) + '" title="Show in the Counters tab"><i class="fa-solid fa-arrow-right-to-bracket"></i></button>';
		h += '</span></div>';

		if (isOpen)
		{
			h += '<div class="c2-al-b">';
			fields.forEach(function(f) {
				h += fieldRow(kf(f), f, { type: 'alarm', cmName: cm.cmName, alarm: a.name, timeRangeDescr: a.timeRangeDescrption });
			});
			h += '</div>';
		}
		h += '</div>';
		return h;
	}

	//------------------------------------------------------------------
	// Rendering: page parts
	//------------------------------------------------------------------
	function renderHeader()
	{
		var c = S.cfg || {};
		var h = '<div class="c2-ph-top"><h1>' + esc(c.srvName || S.srvName || '-') + '</h1>';
		if (c.srvDisplayName && c.srvDisplayName !== c.srvName) h += '<span class="c2-pill">' + esc(c.srvDisplayName) + '</span>';
		if (c.srvDbmsName)  h += '<span class="c2-pill c2-pill-blue" title="DBMS server name">DBMS: ' + esc(c.srvDbmsName) + '</span>';
		if (c.srvAliasName && c.srvAliasName !== c.srvName) h += '<span class="c2-pill" title="Alias">' + esc(c.srvAliasName) + '</span>';
		if (S.cfg) h += '<span class="c2-pill ' + (S.isAdmin ? 'c2-pill-ok' : '') + '">' + (S.isAdmin ? '<i class="fa-solid fa-pen"></i> Editable (admin)' : '<i class="fa-solid fa-lock"></i> Read only') + '</span>';
		h += '<span class="c2-sp"></span>'
			+ '<a class="c2-link" href="/config_classic.html?srvName=' + encodeURIComponent(S.srvName) + '">Classic view</a></div>';

		// Configuration files, own line (collectors older than 2026-09-28 do not send 'configFiles')
		var files = c.configFiles || [];
		var order = 'Search order (the first file that has a value wins):\n' + files.map(function(f, i) { return '  ' + (i + 1) + '. ' + f.filename; }).join('\n');
		var fh = '';
		files.forEach(function(f) {
			if (f.role !== 'server' && f.role !== 'shared' && f.role !== 'user') return;
			var lbl = f.role === 'server' ? 'Server config' : f.role === 'shared' ? 'Shared config' : 'User config';
			fh += '<span class="c2-pill c2-pill-file" title="' + esc(f.filename + '\n' + f.description + '\n\n' + order) + '"><i class="fa-regular fa-file-lines"></i> '
				+ lbl + ': <span class="c2-mono">' + esc(baseName(f.filename)) + '</span></span>';
		});
		if (fh) h += '<div class="c2-files">' + fh + '</div>';
		h += '<div class="c2-sub">Collector configuration. Changes are sent to the running collector, and optionally saved to its configuration file.</div>';

		var nAlarms = cmList().reduce(function(n, cm) { return n + alarmsOf(cm).length; }, 0);
		var tabs = [['counters', 'Counters', cmList().length], ['overview', 'Alarm Overview', nAlarms], ['writers', 'Alarm Writers', writerList().length], ['dsr', 'Daily Summary Report', null]];
		h += '<div class="c2-tabs" role="tablist">';
		tabs.forEach(function(t) {
			h += '<button type="button" class="c2-tab' + (S.tab === t[0] ? ' c2-on' : '') + '" data-tab="' + t[0] + '" role="tab" aria-selected="' + (S.tab === t[0]) + '">' + t[1]
				+ (t[2] !== null && S.cfg ? '<span class="c2-cnt">' + t[2] + '</span>' : '') + '</button>';
		});
		h += '</div>';
		el('c2-head').innerHTML = h;
	}

	function bannersHtml()
	{
		var h = '';
		if (S.cfg && !S.isAdmin)
		{
			var login = '<a href="/index.html?login=open" target="_blank" rel="noopener">Log in as admin</a>';
			if (S.authLost)
				h += '<div class="c2-banner c2-banner-warn"><i class="fa-solid fa-lock"></i> Your admin login has expired or was rejected, so nothing more was saved. ' + login
					+ ' (a new tab opens), then come back here and press Save again. Your unsaved changes are kept.</div>';
			else if (S.isLoggedIn)
				h += '<div class="c2-banner c2-banner-info"><i class="fa-solid fa-lock"></i> Read only. Logged in as <b>' + esc(S.userName) + '</b>, admin rights are needed to change the configuration. ' + login + ' in another tab, this page unlocks when you come back.</div>';
			else
				h += '<div class="c2-banner c2-banner-info"><i class="fa-solid fa-lock"></i> Read only. ' + login + ' to change the configuration (a new tab opens), this page unlocks when you come back.</div>';
		}
		if (S.message)
			h += '<div class="c2-banner c2-banner-' + S.message.type + '"><button type="button" class="c2-x" data-dismiss-msg title="Dismiss">&times;</button>' + esc(S.message.text) + '</div>';
		return h;
	}

	function chip(group, name, label, on)
	{
		return '<button type="button" class="c2-chipf' + (on ? ' c2-on' : '') + '" data-chip="' + group + ':' + name + '" aria-pressed="' + on + '">' + label + '</button>';
	}

	function renderCounters()
	{
		var F = S.cmFilter, words = F.q.toLowerCase().split(/\s+/).filter(Boolean);
		var list = '', grp, shown = 0; // grp starts undefined: groupName can be null (CmSummary)
		cmList().forEach(function(cm)
		{
			if (!cm._search)
			{
				var parts = [cm.cmName, cm.displayName, cm.groupName];
				alarmsOf(cm).forEach(function(a) { parts.push(a.name); (a.parameters || []).forEach(function(p) { parts.push(p.name, p.property); }); });
				[].concat(cm.options || [], cm.settings || [], preChecksOf(cm)).forEach(function(p) { parts.push(p.name, p.property); });
				cm._search = parts.filter(Boolean).join(' ').toLowerCase();
			}
			var nPend = pendKeysFor(function(p) { return p.cmName === cm.cmName; }).length;
			if (words.some(function(w) { return cm._search.indexOf(w) === -1; })) return;
			if (F.alarms && !alarmsOf(cm).length) return;
			if (F.mod && !isCmModified(cm)) return;
			if (F.pend && !nPend) return;
			if (F.off && cm.isCmEnabled !== false) return;
			shown++;

			if (cm.groupName !== grp) { grp = cm.groupName; list += '<div class="c2-grp">' + esc(grp || 'General') + '</div>'; }
			var mods = modifiedList(cm);
			var dot = cm.isCmEnabled === false ? '' : (alarmsOf(cm).length && whyNoAlarms(cm).length ? 'c2-dot-y' : 'c2-dot-g');
			var dotTitle = cm.isCmEnabled === false ? 'Counter disabled' : (dot === 'c2-dot-y' ? 'Enabled, but its alarms will not fire' : 'Enabled');
			list += '<div class="c2-cm' + (cm.cmName === S.selCm ? ' c2-on' : '') + (cm.isCmEnabled === false ? ' c2-off' : '') + '" data-cm="' + esc(cm.cmName) + '" role="button" tabindex="0">'
				+ '<span class="c2-dot ' + dot + '" title="' + dotTitle + '"></span>'
				+ '<span class="c2-nm">' + esc(cm.displayName || cm.cmName) + '<small>' + esc(cm.cmName) + '</small></span>'
				// badge order: mod, unsaved, postpone, alarms, save (last)
				+ (mods.length ? '<span class="c2-bdg c2-bdg-mod c2-bdg-click" data-modcm="' + esc(cm.cmName) + '" role="button" tabindex="0" title="' + esc(modifiedTitle(mods) + '\n\nClick to show them') + '">mod ' + mods.length + '</span>' : '')
				+ (nPend ? '<span class="c2-bdg c2-bdg-pend" title="Unsaved changes">' + nPend + '</span>' : '')
				+ postponeBadge(cm)
				+ (alarmsOf(cm).length ? '<span class="c2-bdg" title="' + alarmsOf(cm).length + ' alarms"><i class="fa-regular fa-bell"></i> ' + alarmsOf(cm).length + '</span>' : '')
				+ persistBadge(cm)
				+ '</div>';
		});
		if (!shown) list = '<div class="c2-empty">No counters match the filter.</div>';

		var anyPend = pendCount() > 0;
		var h = '<div class="c2-md">'
			+ '<div class="c2-card c2-list"><div class="c2-list-h">'
			+ '<input type="search" id="c2-cm-q" class="c2-search" placeholder="Filter counters, alarms, properties..." value="' + esc(F.q) + '" autocomplete="off">'
			+ '<div class="c2-flt">' + chip('cm', 'alarms', 'Has alarms', F.alarms) + chip('cm', 'mod', 'Only modified', F.mod) + chip('cm', 'off', 'Disabled', F.off)
			+ (anyPend || F.pend ? chip('cm', 'pend', 'Unsaved', F.pend) : '') + '</div>'
			+ '</div><div class="c2-list-b" id="c2-cmlist">' + list + '</div></div>'
			+ '<div class="c2-card c2-det">' + counterDetail() + '</div></div>';
		return h;
	}

	function statusChip(on, label, title)
	{
		return '<span class="c2-stat ' + (on ? 'c2-stat-on' : 'c2-stat-off') + '" title="' + esc(title) + ' (read only: can not be changed from this page)">'
			+ '<i class="fa-solid ' + (on ? 'fa-check' : 'fa-xmark') + '"></i> ' + label + '</span>';
	}

	function counterDetail()
	{
		var cm = findCm(S.selCm);
		if (!cm) return '<div class="c2-empty">Select a counter to the left.</div>';

		var h = '<div class="c2-det-h"><div class="c2-det-t"><h2>' + esc(cm.displayName || cm.cmName) + '</h2>'
			+ '<div class="c2-sub c2-mono">' + esc(cm.cmName) + (cm.groupName ? ' &middot; ' + esc(cm.groupName) : '') + '</div></div><div class="c2-stats">'
			+ statusChip(cm.isCmEnabled !== false, 'Collector enabled', 'Is the counter collecting data')
			+ statusChip(cm.isAlarmEnabled !== false, 'Alarms', 'Are alarms enabled for this counter');
		if (cm.hasSystemAlarms)                 h += statusChip(cm.isSystemAlarmsEnabled !== false, 'System alarms', 'Are the built-in alarms of this counter enabled');
		if (cm.hasUserDefinedAlarmInterrogator) h += statusChip(cm.isUserDefinedAlarmsEnabled !== false, 'User defined alarms', 'Are the user defined alarms of this counter enabled');
		var pp = postponeOf(cm);
		if (pp) h += '<span class="c2-stat ' + (pp.enabled ? 'c2-stat-pp' : 'c2-stat-off') + '" title="' + esc(pp.title + ' (change it in Options: Postpone Time / Is Postpone Enabled)') + '">'
			+ '<i class="fa-regular fa-clock"></i> ' + (pp.enabled ? 'Every ' + esc(pp.text) : esc(pp.text) + ' (disabled)') + '</span>';
		var ps = persistOf(cm);
		if (ps) h += '<span class="c2-stat ' + (ps.on ? 'c2-stat-on' : 'c2-stat-bad') + '" title="' + esc(ps.title) + '">'
			+ '<i class="fa-solid fa-database"></i> ' + (ps.on ? 'Saved' : 'Not saved') + '</span>';
		h += '</div></div>';

		var subs = [['alarms', 'Alarms', alarmsOf(cm).length], ['options', 'Options', (cm.options || []).length], ['settings', 'Local Settings', (cm.settings || []).length], ['info', 'Info', null]];
		var cmMods = modifiedKeys(cm);
		h += '<div class="c2-stabs" role="tablist">';
		subs.forEach(function(s) {
			var nP = pendKeysFor(function(p) { return p.cmName === cm.cmName && subOf(p.type) === s[0]; }).length;
			var nM = cmMods.filter(function(m) { return m.sub === s[0]; }).length;
			h += '<button type="button" class="c2-tab' + (S.sub === s[0] ? ' c2-on' : '') + '" data-sub="' + s[0] + '" role="tab">' + s[1]
				+ (s[2] !== null ? '<span class="c2-cnt">' + s[2] + '</span>' : '')
				+ (nM ? '<span class="c2-cnt c2-cnt-mod" title="' + nM + ' value' + (nM === 1 ? '' : 's') + ' differ from the default">mod ' + nM + '</span>' : '')
				+ (nP ? '<span class="c2-cnt c2-cnt-pend" title="Unsaved changes">' + nP + '</span>' : '') + '</button>';
		});
		h += '</div><div class="c2-pane">';

		if (S.sub === 'alarms')
		{
			var alarms = alarmsOf(cm), pre = preChecksOf(cm);
			if (!alarms.length && !pre.length)
				h += '<div class="c2-empty">This counter has no system alarms.</div>';
			else
			{
				var why = whyNoAlarms(cm);
				if (why.length && alarms.length)
					h += '<div class="c2-banner c2-banner-warn"><i class="fa-solid fa-triangle-exclamation"></i> The alarms below will not fire even when enabled: ' + esc(why.join(', ')) + '.</div>';
				if (pre.length)
				{
					h += '<div class="c2-sec">Pre checks <span class="c2-sec-sub">checked on each row before the alarm thresholds, to skip entries early</span></div><div class="c2-rows">';
					pre.forEach(function(p) { h += fieldRow(propKey('P', cm.cmName, p.name), propField(p), { type: 'alarmParams', cmName: cm.cmName }); });
					h += '</div>';
				}
				if (alarms.length)
				{
					h += '<div class="c2-sec">Alarms <span class="c2-sec-sub">click an alarm to ' + (S.isAdmin ? 'change' : 'see') + ' it'
						+ ' &middot; <a href="#" data-openall="1">expand all</a> &middot; <a href="#" data-openall="0">collapse all</a></span></div>';
					alarms.forEach(function(a) { h += alarmBlock(cm, a, false); });
				}
			}
		}
		else if (S.sub === 'options' || S.sub === 'settings')
		{
			var list = S.sub === 'options' ? (cm.options || []) : (cm.settings || []);
			var type = S.sub;
			if (!list.length) h += '<div class="c2-empty">No ' + (type === 'options' ? 'collector options' : 'local settings') + ' for this counter.</div>';
			else
			{
				h += '<div class="c2-rows">';
				list.forEach(function(p) { h += fieldRow(propKey(type === 'options' ? 'O' : 'S', cm.cmName, p.name), propField(p), { type: type, cmName: cm.cmName }); });
				h += '</div>';
			}
		}
		else
		{
			var info = [['CM Name', cm.cmName, true], ['Display Name', cm.displayName], ['Group Name', cm.groupName], ['Primary Key', toStr(cm.pkCols), true],
			            ['Diff Columns', toStr(cm.diffCols), true], ['Percent Columns', toStr(cm.pctCols), true], ['Need Server Config', toStr(cm.needSrvConfig), true],
			            ['Need Server Roles', toStr(cm.needSrvRoles)], ['Need Server Version', toStr(cm.needSrvVersion)], ['Depends On CM', toStr(cm.dependsOnCm), true]];
			if (cm.osCommand) info.push(['OS Host', cm.osHost], ['OS Command Mode', cm.osCommandExecMode]);   // Host Monitor CM's
			h += '<dl class="c2-info">';
			info.forEach(function(i) { h += '<dt>' + i[0] + '</dt><dd' + (i[2] ? ' class="c2-mono"' : '') + '>' + (i[1] ? esc(i[1]) : '<span class="c2-faint">-</span>') + '</dd>'; });
			h += '</dl>';
			[['Init SQL', cm.sqlInit, 'sql'], ['Get Counter SQL', cm.sqlRefresh, 'sql'], ['Close SQL', cm.sqlClose, 'sql'], ['OS Command', cm.osCommand, 'none']].forEach(function(s) {
				if (toStr(s[1]).trim() === '') return;
				h += '<div class="c2-sec">' + s[0] + '</div><pre class="c2-sql"><code class="language-' + s[2] + '">' + esc(toStr(s[1])) + '</code></pre>';
			});
		}
		h += '</div>';
		return h;
	}

	function subOf(type) { return type === 'options' ? 'options' : type === 'settings' ? 'settings' : 'alarms'; }

	function renderOverview()
	{
		var F = S.ovFilter;
		var rows = dbxAlarmOverviewFlatten(cmList());
		var total = rows.length;
		rows = dbxAlarmOverviewFilter(rows, { search: F.q, onlyEffective: F.eff, onlyModified: F.mod });
		if (F.pend) rows = rows.filter(function(r) { return pendKeysFor(function(p) { return p.type === 'alarm' && p.cmName === r.cmName && p.alarm === r.name; }).length; });

		var h = '<div class="c2-ov"><div class="c2-card c2-ov-h">'
			+ '<input type="search" id="c2-ov-q" class="c2-search" placeholder="Search: counter, alarm, description, parameter..." value="' + esc(F.q) + '" autocomplete="off">'
			+ '<div class="c2-flt">' + chip('ov', 'eff', 'Only effective', F.eff) + chip('ov', 'mod', 'Only modified', F.mod)
			+ (pendCount() || F.pend ? chip('ov', 'pend', 'Unsaved', F.pend) : '')
			+ '<span class="c2-count">' + rows.length + ' of ' + total + ' alarms</span></div>'
			+ '<div class="c2-help"><b>Effective</b> = the counter, its alarms, its system alarms and the alarm itself are enabled. <b>Modified</b> = at least one value differs from its default.</div>'
			+ '</div>';
		if (!rows.length) h += '<div class="c2-empty">No alarms match the filter.</div>';
		rows.forEach(function(r) {
			var cm = findCm(r.cmName);
			var a  = alarmsOf(cm).find(function(x) { return x.name === r.name; });
			if (cm && a) h += alarmBlock(cm, a, true);
		});
		return h + '</div>';
	}

	function renderWriters()
	{
		var ws = writerList();
		if (!S.selWriter && ws.length) S.selWriter = (ws.find(function(w) { return w.isActive; }) || ws[0]).name;

		var list = '';
		ws.forEach(function(w) {
			list += '<div class="c2-cm' + (w.name === S.selWriter ? ' c2-on' : '') + (w.isActive ? '' : ' c2-off') + '" data-writer="' + esc(w.name) + '" role="button" tabindex="0">'
				+ '<span class="c2-dot ' + (w.isActive ? 'c2-dot-g' : '') + '" title="' + (w.isActive ? 'Active' : 'Not active') + '"></span>'
				+ '<span class="c2-nm">' + esc(w.name) + '<small>' + esc(w.isActive ? 'active' : 'not active') + '</small></span></div>';
		});
		if (!ws.length) list = '<div class="c2-empty">No alarm writers.</div>';

		var det = '<div class="c2-empty">Select an alarm writer to the left.</div>';
		var w = ws.find(function(x) { return x.name === S.selWriter; });
		if (w)
		{
			det = '<div class="c2-det-h"><div class="c2-det-t"><h2>' + esc(w.name) + '</h2><div class="c2-sub c2-mono">' + esc(w.className) + '</div>'
				+ (w.description ? '<div class="c2-help">' + esc(htmlToText(w.description)) + '</div>' : '') + '</div>'
				+ '<div class="c2-stats">' + statusChip(w.isActive, 'Active', 'Is this alarm writer used') + '</div></div>';
			det += '<div class="c2-stabs">'
				+ '<button type="button" class="c2-tab' + (S.wsub === 'settings' ? ' c2-on' : '') + '" data-wsub="settings">Settings<span class="c2-cnt">' + (w.settings || []).length + '</span></button>'
				+ '<button type="button" class="c2-tab' + (S.wsub === 'filters' ? ' c2-on' : '') + '" data-wsub="filters">Filters<span class="c2-cnt">' + (w.filters || []).length + '</span></button></div>';
			det += '<div class="c2-pane"><div class="c2-help c2-mb">'
				+ (w.isActive ? 'Changes are validated by the collector, and the writer is re-created with them before its next use (no restart needed).'
				              : 'This writer is not active in the collector (it is not in <code>AlarmHandler.WriterClass</code>), changes are only saved.')
				+ ' Passwords can only be changed in the collector\'s configuration file.</div><div class="c2-rows">';
			var entries = S.wsub === 'settings' ? (w.settings || []) : (w.filters || []);
			if (!entries.length) det += '<div class="c2-empty">None.</div>';
			entries.forEach(function(p) {
				var extra = (p.isMandatory ? '<span class="c2-bdg" title="Mandatory">mandatory</span>' : '')
					+ (p.isSelected === false ? '<span class="c2-bdg" title="Not activated">not activated</span>' : '');
				det += fieldRow('W|' + w.name + '|' + S.wsub + '|' + p.name, propField(p), { type: 'writer', cmName: '', writer: w.className, wsub: S.wsub, extraLbl: extra ? '<div>' + extra + '</div>' : '' });
			});
			det += '</div></div>';
		}

		return '<div class="c2-md"><div class="c2-card c2-list"><div class="c2-list-b" id="c2-wlist">' + list + '</div></div>'
			+ '<div class="c2-card c2-det">' + det + '</div></div>';
	}

	/** The Daily Summary Report tab: the report's settings, its sender's settings and the report sections (all read at every report run) */
	function renderDsr()
	{
		var d = dsrCfg();
		if (!d)
			return '<div class="c2-wrap"><div class="c2-card c2-pane"><div class="c2-empty">This collector is too old to configure the Daily Summary Report from here (it needs a newer version).</div></div></div>';

		var snd   = d.sender || {};
		var secs  = dsrList('sections');
		var subs  = [['report', 'Report', (d.settings || []).length], ['mail', snd.name || 'Sender', ((snd.settings) || []).length], ['sections', 'Sections', secs.length]];
		var cKey  = 'D|report|DailySummaryReport.create';
		var cRow  = (d.settings || []).find(function(s) { return s.property === 'DailySummaryReport.create'; });
		var isOn  = cRow ? String(cur(cKey, cRow.value)).toLowerCase() === 'true' : false;

		var h = '<div class="c2-det-h"><div class="c2-det-t"><h2>Daily Summary Report</h2><div class="c2-sub c2-mono">' + esc(d.reportClassName || '') + '</div>'
			+ '<div class="c2-help">Created when the recording database rolls over to a new day (normally at midnight), then sent by <code>' + esc(snd.name || '?') + '</code>. '
			+ 'Changes are used at the next report, no restart needed.</div></div>'
			+ '<div class="c2-stats">' + statusChip(isOn, isOn ? 'Created' : 'Not created', 'Is the report created for this server (Report > Create report)') + '</div></div>';

		h += '<div class="c2-stabs">' + subs.map(function(s) {
			return '<button type="button" class="c2-tab' + (S.dsub === s[0] ? ' c2-on' : '') + '" data-dsub="' + s[0] + '">' + esc(s[1]) + '<span class="c2-cnt">' + s[2] + '</span></button>';
		}).join('') + '</div><div class="c2-pane">';

		if (S.dsub === 'sections')
		{
			h += '<div class="c2-help c2-mb">The parts of the report. A section that is switched off is not created (and its queries are not run).'
				+ (S.isAdmin ? ' <button type="button" class="c2-btn c2-btn-sm" data-dsr-all="true">All on</button> <button type="button" class="c2-btn c2-btn-sm" data-dsr-all="false">All off</button>' : '') + '</div>';
			if (d.sectionsError) h += '<div class="c2-warn">' + esc(d.sectionsError) + '</div>';
		}
		else if (S.dsub === 'mail')
			h += '<div class="c2-help c2-mb">The settings of <code>' + esc(snd.className || '') + '</code>. Passwords can only be changed in the collector\'s configuration file.</div>';

		h += '<div class="c2-rows">';
		var entries = dsrList(S.dsub);
		if (!entries.length) h += '<div class="c2-empty">None.</div>';
		entries.forEach(function(p) {
			var fixed = S.dsub === 'sections' && !p.canBeDisabled;
			var extra = (p.isMandatory ? '<span class="c2-bdg" title="Mandatory">mandatory</span>' : '') + (fixed ? '<span class="c2-bdg" title="This section can not be switched off">always included</span>' : '');
			h += fieldRow('D|' + S.dsub + '|' + p.property, propField(p), { type: 'dsr', cmName: '', wsub: S.dsub, readOnly: fixed, extraLbl: extra ? '<div>' + extra + '</div>' : '' });
		});
		h += '</div></div>';

		return '<div class="c2-wrap"><div class="c2-card c2-det">' + h + '</div></div>';
	}

	function renderSaveBar()
	{
		var n = pendCount(), bar = el('c2-savebar');
		bar.classList.toggle('c2-show', n > 0);
		if (!n) { bar.innerHTML = ''; S.menuOpen = S.listOpen = false; return; }

		var blocking = blockingCount();
		var h = '<button type="button" class="c2-sb-cnt" id="c2-sb-list" title="Show the unsaved changes"><b>' + n + '</b> unsaved change' + (n === 1 ? '' : 's') + ' <i class="fa-solid fa-chevron-up"></i></button>';
		if (blocking) h += '<span class="c2-sb-bad">' + blocking + ' invalid</span>';
		h += '<button type="button" class="c2-btn c2-btn-dark" id="c2-sb-discard"' + (S.saving ? ' disabled' : '') + '>Discard</button>';
		h += '<button type="button" class="c2-btn c2-btn-pri" id="c2-sb-save"' + (!S.isAdmin || blocking || S.saving ? ' disabled' : '')
			+ ' title="' + (!S.isAdmin ? 'Log in as admin to save' : blocking ? 'Fix the invalid values first' : 'Choose where to save') + '">'
			+ (S.saving ? '<i class="fa-solid fa-spinner fa-spin"></i> Saving...' : 'Save changes <i class="fa-solid fa-chevron-up"></i>') + '</button>';

		if (S.menuOpen)
		{
			var srvFile = configFile('server'), sharedFile = configFile('shared');
			h += '<div class="c2-menu c2-menu-r">'
				+ '<div class="c2-menu-h">Where should the changes be saved?</div>'
				+ '<button type="button" class="c2-item" data-savetype="IN_MEMORY">Only in-memory (do not save to any file)<small>Used until the collector is restarted</small></button>'
				+ '<button type="button" class="c2-item" data-savetype="THIS_SERVER">Save for THIS server<small>'
				+ (srvFile ? 'Written to <code>' + esc(srvFile.filename) + '</code>, overrides the shared file, survives a restart'
				           : 'Written to ' + esc(S.srvName) + '\'s own configuration file, survives a restart') + '</small></button>'
				+ '<button type="button" class="c2-item" disabled title="Not implemented by the collector yet">Save for ALL Servers sharing the same config file<small>'
				+ (sharedFile ? 'Would write to <code>' + esc(sharedFile.filename) + '</code> - ' : '') + 'Not implemented by the collector yet</small></button>'
				+ '</div>';
		}

		if (S.listOpen)
		{
			h += '<div class="c2-menu c2-menu-l"><div class="c2-menu-h">Unsaved changes (click to show)</div>';
			Object.keys(S.pend).forEach(function(k) {
				var p = S.pend[k], cm = findCm(p.cmName);
				var where = p.type === 'writer' ? String(p.writer).split('.').pop() + ' › ' + p.label
				          : p.type === 'dsr'    ? 'Daily Summary Report › ' + p.label
				          : (cm ? (cm.displayName || cm.cmName) : p.cmName) + (p.alarm ? ' › ' + p.alarm : '') + ' › ' + p.label;
				h += '<button type="button" class="c2-item" data-jump="' + esc(k) + '">' + esc(where)
					+ '<small><code>' + esc(p.orig === '' ? '(empty)' : shortVal(p.orig)) + '</code> &rarr; <code>' + esc(p.value === '' ? '(empty)' : shortVal(p.value)) + '</code>'
					+ (S.errors[k] ? ' &middot; <span class="c2-err-i">' + esc(htmlToText(S.errors[k])) + '</span>' : '') + '</small></button>';
			});
			h += '</div>';
		}
		bar.innerHTML = h;
	}

	function render()
	{
		// keep focus, caret and list scroll over the re-render
		var a = document.activeElement, focus = null;
		if (a && (a.id || a.getAttribute('data-k')) && el('c2-main').contains(a))
			focus = { id: a.id, k: a.getAttribute('data-k'), set: a.getAttribute('data-set'), caret: (a.type === 'text' || a.type === 'search' || a.type === 'textarea') ? a.selectionStart : null,
			          end: a.selectionEnd, top: a.type === 'textarea' ? a.scrollTop : null };
		var scroll = {};
		['c2-cmlist', 'c2-wlist'].forEach(function(id) { if (el(id)) scroll[id] = el(id).scrollTop; });
		// template editors: keep each one's cursor and scroll, also when not focused (a variable is inserted where the cursor was)
		var tplSel = {};
		document.querySelectorAll('#c2-main textarea[data-tpl-txt]').forEach(function(t) { tplSel[t.id] = [t.selectionStart, t.selectionEnd, t.scrollTop]; });

		S.reg = {};
		renderHeader();

		var h = '<div class="c2-banners">' + bannersHtml() + '</div>';
		if (S.loadError)
			h += '<div class="c2-wrap"><div class="c2-banner c2-banner-err">Failed to load the configuration for "' + esc(S.srvName) + '": ' + esc(S.loadError) + '</div></div>';
		else if (!S.cfg)
			h += '<div class="c2-wrap"><div class="c2-empty"><i class="fa-solid fa-spinner fa-spin"></i> Loading the configuration...</div></div>';
		else if (S.tab === 'counters') h += renderCounters();
		else if (S.tab === 'overview') h += renderOverview();
		else if (S.tab === 'writers')  h += renderWriters();
		else h += renderDsr();
		el('c2-main').innerHTML = h;

		Object.keys(scroll).forEach(function(id) { if (el(id)) el(id).scrollTop = scroll[id]; });
		Object.keys(tplSel).forEach(function(id) {
			var t = el(id);
			if (!t) return;
			try { t.setSelectionRange(tplSel[id][0], tplSel[id][1]); } catch (e) {}
			t.scrollTop = tplSel[id][2];
		});
		if (focus)
		{
			var t = focus.id ? el(focus.id)
				: document.querySelector('#c2-main [data-k="' + CSS.escape(focus.k) + '"]' + (focus.set ? '[data-set="' + CSS.escape(focus.set) + '"]' : ':not([data-set])'));
			if (t)
			{
				t.focus();
				if (focus.caret !== null) { try { t.setSelectionRange(focus.caret, Math.max(focus.caret, focus.end || 0)); } catch (e) {} }
				if (focus.top !== null) t.scrollTop = focus.top; // a long template: stay where you were
			}
		}
		if (window.Prism) document.querySelectorAll('#c2-main pre.c2-sql code').forEach(function(c) { Prism.highlightElement(c); });
		renderSaveBar();
		updateUrl();
	}

	/** Keep tab + counter in the URL, so a reload (or a copied link) shows the same thing */
	function updateUrl()
	{
		try
		{
			var u = new URL(window.location.href);
			u.searchParams.set('tab', S.tab === 'counters' ? 'counters' : S.tab === 'overview' ? 'alarmOverview' : S.tab === 'writers' ? 'alarmWriters' : 'dsr');
			if (S.tab === 'counters' && S.selCm) u.searchParams.set('cm', S.selCm);
			u.searchParams.delete('alarm');
			u.searchParams.delete('param');
			if (u.href !== window.location.href) window.history.replaceState(null, '', u.href);
		}
		catch (e) {}
	}

	function scrollToKey(sel)
	{
		var t = document.querySelector(sel);
		if (t) t.scrollIntoView({ block: 'center' });
	}

	/** Highlight one or more rows (keys) for a moment */
	function flash(keys)
	{
		var set = {};
		[].concat(keys).forEach(function(k) { set[k] = true; });
		S.flashKeys = set;
		setTimeout(function() {
			if (S.flashKeys !== set) return; // a newer flash owns it
			S.flashKeys = {};
			document.querySelectorAll('.c2-flash').forEach(function(r) { r.classList.remove('c2-flash'); });
		}, 2500);
	}

	/**
	 * The modified values of a CM (same rule as the "mod" badge: differs from the default, Save Counters* excluded),
	 * in page order: [{ key, sub, alarm }] - key is the row key used by fieldRow()
	 */
	function modifiedKeys(cm)
	{
		var list = [];
		var nd = function(p) { return p.isDefaultValue === false; };
		preChecksOf(cm).filter(nd).forEach(function(p) { list.push({ key: propKey('P', cm.cmName, p.name), sub: 'alarms' }); });
		alarmsOf(cm).forEach(function(a) {
			var params = a.parameters || [];
			M.buildFields(a).forEach(function(f) { // field order = the order the rows are drawn in
				var p = params.find(function(x) { return x.name === f.name; });
				if (p && nd(p)) list.push({ key: alarmKey(cm.cmName, a.name, f.name), sub: 'alarms', alarm: a.name });
			});
		});
		(cm.options  || []).filter(function(p) { return nd(p) && !isPersistOption(cm, p); }).forEach(function(p) { list.push({ key: propKey('O', cm.cmName, p.name), sub: 'options' }); });
		(cm.settings || []).filter(nd).forEach(function(p) { list.push({ key: propKey('S', cm.cmName, p.name), sub: 'settings' }); });
		return list;
	}

	/** Click on "mod": open every modified alarm of the CM, go to the first modified row, highlight all of them */
	function showModified(cmName)
	{
		var cm = findCm(cmName);
		if (!cm) return;
		var mods = modifiedKeys(cm);
		S.tab   = 'counters';
		S.selCm = cmName;
		if (!mods.length) { render(); return; }

		S.sub = mods[0].sub;
		// only the modified alarms open (for this CM), so they are easy to see
		alarmsOf(cm).forEach(function(a) { delete S.open[cm.cmName + '|' + a.name]; });
		mods.forEach(function(m) { if (m.alarm) S.open[cm.cmName + '|' + m.alarm] = true; });

		flash(mods.map(function(m) { return m.key; }));
		render();
		scrollToKey('[data-row="' + CSS.escape(mods[0].key) + '"]');
	}

	//------------------------------------------------------------------
	// Navigation
	//------------------------------------------------------------------
	function selectCm(cmName)
	{
		S.selCm = cmName;
		render();
		// show the top of the new counter (the page may be scrolled far down in the previous one)
		var d = document.querySelector('.c2-det'), nav = document.querySelector('nav.navbar');
		var top = d ? d.getBoundingClientRect().top : 0, navH = nav ? nav.getBoundingClientRect().height : 0;
		if (d && (top < navH || window.innerWidth < 900))
			window.scrollBy(0, top - navH - 8);
	}

	/** Show an alarm (and optionally one of its parameters) in the Counters tab */
	function showAlarm(cmName, alarmName, paramName)
	{
		var cm = findCm(cmName);
		if (!cm) return;
		S.tab = 'counters';
		S.selCm = cmName;
		S.sub = 'alarms';
		var a = alarmsOf(cm).find(function(x) { return x.name === alarmName; });
		if (!a) { render(); return; }
		S.open[cmName + '|' + alarmName] = true;

		var key = null;
		if (paramName)
		{
			var f = M.buildFields(a).find(function(x) { return x.name === paramName || x.name === alarmName + ' ' + paramName || x.label === paramName; });
			if (f) key = alarmKey(cmName, alarmName, f.name);
		}
		if (key) flash(key);
		render();
		if (key) scrollToKey('[data-row="' + CSS.escape(key) + '"]');
		else     scrollToKey('[data-al="' + CSS.escape(cmName + '|' + alarmName) + '"]');
	}

	function jumpTo(key)
	{
		var p = S.pend[key];
		if (!p) return;
		S.listOpen = false;
		if (p.type === 'writer')
		{
			var w = writerList().find(function(x) { return x.className === p.writer; });
			S.tab = 'writers';
			if (w) S.selWriter = w.name;
			S.wsub = p.wsub || 'settings';
			flash(key);
			render();
			scrollToKey('[data-row="' + CSS.escape(key) + '"]');
			return;
		}
		if (p.type === 'dsr')
		{
			S.tab  = 'dsr';
			S.dsub = p.wsub || 'report';
			flash(key);
			render();
			scrollToKey('[data-row="' + CSS.escape(key) + '"]');
			return;
		}
		S.tab = 'counters';
		S.selCm = p.cmName;
		S.sub = subOf(p.type);
		if (p.type === 'alarm') S.open[p.cmName + '|' + p.alarm] = true;
		flash(key);
		render();
		scrollToKey('[data-row="' + CSS.escape(key) + '"]');
	}

	//------------------------------------------------------------------
	// Events (delegated on document, so re-rendering keeps them)
	//------------------------------------------------------------------
	function onInput(e)
	{
		var t = e.target;
		// search boxes: 'input' only ('change' fires on blur, i.e. on mousedown of the row being clicked, and the re-render would swallow that click)
		if (t.id === 'c2-cm-q') { if (e.type === 'input') { S.cmFilter.q = t.value; render(); } return; }
		if (t.id === 'c2-ov-q') { if (e.type === 'input') { S.ovFilter.q = t.value; render(); } return; }

		// Text fields on every keystroke ('input'), checkboxes on 'change' only.
		// (NOT 'change' on text fields: it fires on mousedown of e.g. the Save button, and the re-render would swallow that click)
		// The mail rules editor has its own fields (a rule field changes the whole JSON value)
		if (t.hasAttribute && t.hasAttribute('data-mr-key')) { mrOnInput(t, e); return; }
		if (t.hasAttribute && t.hasAttribute('data-tpl-key')) { tplOnInput(t, e); return; }

		var key = t.getAttribute && t.getAttribute('data-k');
		if (!key) return;
		var isChk = t.type === 'checkbox';
		if (isChk !== (e.type === 'change')) return;
		setPend(key, isChk ? String(t.checked) : t.value);
		render();
	}

	function onClick(e)
	{
		var t = e.target.closest('button, a, [data-modcm], [data-cm], [data-writer], [data-toggle]'); // [data-modcm] sits inside [data-cm]: closest() finds it first

		// a click anywhere else closes the save-bar menus
		if ((S.menuOpen || S.listOpen) && !(t && (t.id === 'c2-sb-save' || t.id === 'c2-sb-list' || t.hasAttribute('data-savetype') || t.hasAttribute('data-jump'))))
		{
			S.menuOpen = S.listOpen = false;
			renderSaveBar();
		}
		if (!t) return;

		if (t.hasAttribute('data-mr-act')) { mrOnClick(t); return; }
		if (t.hasAttribute('data-tpl-do')) { tplOnClick(t); return; }
		if (t.hasAttribute('data-tab'))    { S.tab = t.getAttribute('data-tab'); render(); return; }
		if (t.hasAttribute('data-sub'))    { S.sub = t.getAttribute('data-sub'); render(); return; }
		if (t.hasAttribute('data-wsub'))   { S.wsub = t.getAttribute('data-wsub'); render(); return; }
		if (t.hasAttribute('data-dsub'))   { S.dsub = t.getAttribute('data-dsub'); render(); return; }
		if (t.hasAttribute('data-dsr-all'))
		{
			// all report sections that can be switched off
			var on = t.getAttribute('data-dsr-all');
			dsrList('sections').forEach(function(s) { if (s.canBeDisabled && S.reg['D|sections|' + s.property]) setPend('D|sections|' + s.property, on); });
			render();
			return;
		}
		if (t.hasAttribute('data-modcm'))  { showModified(t.getAttribute('data-modcm')); return; }
		if (t.hasAttribute('data-cm'))     { selectCm(t.getAttribute('data-cm')); return; }
		if (t.hasAttribute('data-writer')) { S.selWriter = t.getAttribute('data-writer'); render(); return; }
		if (t.hasAttribute('data-goto'))
		{
			e.stopPropagation();
			var g = t.getAttribute('data-goto'), i = g.indexOf('|');
			showAlarm(g.substring(0, i), g.substring(i + 1));
			return;
		}
		if (t.hasAttribute('data-toggle'))
		{
			var ok = t.getAttribute('data-toggle');
			if (S.open[ok]) delete S.open[ok]; else S.open[ok] = true;
			render();
			return;
		}
		if (t.hasAttribute('data-openall'))
		{
			e.preventDefault();
			var cm = findCm(S.selCm), openIt = t.getAttribute('data-openall') === '1';
			alarmsOf(cm).forEach(function(a) { if (openIt) S.open[cm.cmName + '|' + a.name] = true; else delete S.open[cm.cmName + '|' + a.name]; });
			render();
			return;
		}
		if (t.hasAttribute('data-chip'))
		{
			var c = t.getAttribute('data-chip').split(':'), F = c[0] === 'cm' ? S.cmFilter : S.ovFilter;
			F[c[1]] = !F[c[1]];
			render();
			return;
		}
		if (t.hasAttribute('data-set'))   { setPend(t.getAttribute('data-k'), t.getAttribute('data-set')); render(); return; }
		if (t.hasAttribute('data-reset'))
		{
			var rk = t.getAttribute('data-reset');
			if (S.reg[rk]) setPend(rk, S.reg[rk].def);
			render();
			return;
		}
		if (t.hasAttribute('data-dismiss-msg')) { S.message = null; render(); return; }
		if (t.id === 'c2-sb-list')    { S.listOpen = !S.listOpen; S.menuOpen = false; renderSaveBar(); return; }
		if (t.id === 'c2-sb-save')    { S.menuOpen = !S.menuOpen; S.listOpen = false; renderSaveBar(); return; }
		if (t.id === 'c2-sb-discard')
		{
			var n = pendCount();
			if (!window.confirm('Discard ' + n + ' unsaved change' + (n === 1 ? '' : 's') + '?')) return;
			S.pend = {};
			S.errors = {};
			render();
			return;
		}
		if (t.hasAttribute('data-savetype')) { S.menuOpen = false; save(t.getAttribute('data-savetype')); return; }
		if (t.hasAttribute('data-jump'))     { jumpTo(t.getAttribute('data-jump')); return; }
	}

	function onKey(e)
	{
		if (e.key === 'Escape' && (S.menuOpen || S.listOpen)) { S.menuOpen = S.listOpen = false; renderSaveBar(); return; }
		// Enter/Space on the "role=button" rows (counter list, alarm header)
		if ((e.key === 'Enter' || e.key === ' ') && e.target.matches && e.target.matches('[data-modcm], [data-cm], [data-writer], [data-toggle]'))
		{
			e.preventDefault();
			e.target.click();
		}
	}

	//------------------------------------------------------------------
	// Server calls
	//------------------------------------------------------------------
	function loadConfig()
	{
		return fetch('/api/cc/mgt/config/get?srvName=' + encodeURIComponent(S.srvName))
			.then(function(r) { if (!r.ok) throw new Error('HTTP ' + r.status); return r.json(); })
			.then(function(j)
			{
				S.cfg = j;
				S.loadError = null;
				if (j.srvName && S.srvName && j.srvName !== S.srvName)
					console.log('WARNING: srvName=\'' + S.srvName + '\' but the collector says \'' + j.srvName + '\'... are we on the correct server?');
				if (!S.srvName) S.srvName = j.srvName || '';
				if (!findCm(S.selCm)) S.selCm = cmList().length ? cmList()[0].cmName : null;
				reconcilePending();
			});
	}

	/** Same rule as the server (ProxyConfigSetServlet) and the Alarm Editor: role 'admin' or the user named 'admin' */
	function checkLogin()
	{
		return new Promise(function(resolve)
		{
			var done = false;
			var finish = function(isLogged, user, isAdmin)
			{
				if (done) return;
				done = true;
				var wasAdmin = S.isAdmin;
				S.isLoggedIn = !!isLogged;
				S.userName   = user || '';
				S.isAdmin    = isAdmin === true || user === 'admin';
				if (S.isAdmin) S.authLost = false;
				resolve(wasAdmin !== S.isAdmin);
			};
			try { isLoggedIn(finish); } catch (e) { finish(false, '', false); } // dbxcentral.utils.js (also updates the navbar)
			setTimeout(function() { finish(S.isLoggedIn, S.userName, S.isAdmin); }, 5000); // isLoggedIn() never calls back on an error
		});
	}

	function postSet(body)
	{
		return fetch('/api/cc/mgt/config/set?srvName=' + encodeURIComponent(S.srvName), {
			method  : 'POST',
			headers : { 'Content-Type': 'application/json' },
			body    : JSON.stringify(body)
		})
		.then(function(r) {
			return r.text().then(function(txt) {
				var j = {};
				try { j = JSON.parse(txt); } catch (e) {}
				return { status: r.status, json: j };
			});
		});
	}

	/**
	 * Save everything pending: one 'alarmBatch' per alarm, one request per option / setting / pre-check / writer property (sequential).
	 * @param saveType 'IN_MEMORY' | 'THIS_SERVER'
	 */
	async function save(saveType)
	{
		if (S.saving || !pendCount() || blockingCount()) return;
		S.saving = true;
		S.message = null;
		renderSaveBar();

		// Build the requests
		var batches = {}, reqs = [];
		Object.keys(S.pend).forEach(function(k)
		{
			var p = S.pend[k];
			if (p.type === 'alarm')
			{
				var bk = p.cmName + '|' + p.alarm;
				if (!batches[bk])
				{
					batches[bk] = { body: { type: 'alarmBatch', saveType: saveType, cmName: p.cmName, optName: p.alarm, change: {} }, keys: [] };
					reqs.push(batches[bk]);
				}
				batches[bk].body.change[p.name] = p.value;
				batches[bk].keys.push(k);
			}
			else if (p.type === 'writer')
			{
				// cmName must be there (the collector reads it as a mandatory field), optName is the property key
				reqs.push({ body: { type: 'writer', saveType: saveType, writerName: p.writer, cmName: '', optName: p.property, change: { value: p.value } }, keys: [k] });
			}
			else if (p.type === 'dsr')
			{
				reqs.push({ body: { type: 'dsr', saveType: saveType, cmName: '', optName: p.property, change: { value: p.value } }, keys: [k] });
			}
			else
			{
				reqs.push({ body: { type: p.type, saveType: saveType, cmName: p.cmName, optName: p.name, change: { value: p.value } }, keys: [k] });
			}
		});

		var saved = 0, failed = 0, denied = false, bootNeeded = false;
		for (var i = 0; i < reqs.length && !denied; i++)
		{
			var r = reqs[i];
			try
			{
				var res = await postSet(r.body);
				if (res.status === 403 || res.status === 401) { denied = true; break; }
				if (res.status === 200 && String(res.json.status).toLowerCase() === 'success')
				{
					r.keys.forEach(function(k) { delete S.pend[k]; delete S.errors[k]; });
					saved += r.keys.length;
					if (res.json.isBootNeeded === true) bootNeeded = true;
				}
				else
				{
					failed += r.keys.length;
					var txt = [res.json.info, res.json.error, res.json.message].filter(Boolean).join(' - ') || ('HTTP ' + res.status);
					var fe = res.json.errors || {};
					r.keys.forEach(function(k) { S.errors[k] = fe[S.pend[k].name] || txt; });
				}
			}
			catch (err)
			{
				failed += r.keys.length;
				r.keys.forEach(function(k) { S.errors[k] = String(err); });
			}
		}

		if (denied)
		{
			S.isAdmin  = false;
			S.authLost = true;
		}
		if (saved)
		{
			try { await loadConfig(); }
			catch (err) { console.error('dbxConfig: reload after save failed', err); }
		}
		S.saving = false;

		var where = saveType === 'IN_MEMORY' ? 'in memory only (used until the collector is restarted)' : 'for ' + S.srvName + ' (written to its configuration file)';
		var parts = [];
		if (saved)  parts.push(saved + ' change' + (saved === 1 ? '' : 's') + ' saved ' + where + '.');
		if (failed) parts.push(failed + ' change' + (failed === 1 ? '' : 's') + ' NOT saved, see the messages at the fields (still unsaved).');
		if (bootNeeded) parts.push('The collector needs to be restarted for some of the changes to take effect.');
		// (a 403 is explained by the read-only banner, which goes away by itself when the login is OK again)
		S.message = parts.length ? { type: failed || denied ? (saved ? 'warn' : 'err') : 'ok', text: parts.join(' ') } : null;
		render();
		if (!failed && !denied) setTimeout(function() { if (S.message && S.message.type === 'ok') { S.message = null; render(); } }, 6000);
	}

	//------------------------------------------------------------------
	// Init
	//------------------------------------------------------------------
	function init()
	{
		S.srvName = getParameter('srvName', '');
		var tab = getParameter('tab', '');
		S.tab = tab === 'alarmOverview' ? 'overview' : tab === 'alarmWriters' ? 'writers' : tab === 'dsr' ? 'dsr' : 'counters';
		var pCm = getParameter('cm', ''), pAlarm = getParameter('alarm', ''), pParam = getParameter('param', '');
		if (pCm) S.selCm = pCm;

		document.addEventListener('input',   onInput);
		document.addEventListener('change',  onInput);
		document.addEventListener('click',   onClick);
		document.addEventListener('keydown', onKey);

		window.addEventListener('beforeunload', function(e) {
			if (pendCount()) { e.preventDefault(); e.returnValue = ''; }
		});

		// Logged in on another tab? Re-check when this tab gets the focus again (at most every 3 seconds)
		var lastCheck = 0;
		var recheck = function() {
			if (document.visibilityState !== 'visible' || Date.now() - lastCheck < 3000) return;
			lastCheck = Date.now();
			checkLogin().then(function(changed) { if (changed) render(); });
		};
		window.addEventListener('focus', recheck);
		document.addEventListener('visibilitychange', recheck);

		render();
		lastCheck = Date.now();
		Promise.all([loadConfig().catch(function(err) { S.loadError = String(err.message || err); }), checkLogin()])
			.then(function()
			{
				render();
				if (pCm && pAlarm && S.tab === 'counters') showAlarm(pCm, pAlarm, pParam);
				else if (pCm && pAlarm && S.tab === 'overview') { S.open[pCm + '|' + pAlarm] = true; render(); scrollToKey('[data-al="' + CSS.escape(pCm + '|' + pAlarm) + '"]'); }
				else if (S.selCm) { var r = document.querySelector('.c2-cm.c2-on'); if (r) r.scrollIntoView({ block: 'nearest' }); }
			});
	}

	return { init: init, _state: S };
})();
