/**
 * dbxAlarmEditor.js — Edit ONE alarm of a Counter (CM): enabled, threshold, filters, time range.
 *
 * Self contained (no Bootstrap dependency) so it works on graph.html (Bootstrap 4) and config.html (Bootstrap 5).
 * Data comes from '/api/cc/mgt/config/get' ('cmList[].alarmSettings.alarms[].parameters[]', see NoGuiConfigGetServlet),
 * and ALL changes are saved in one request: '/api/cc/mgt/config/set' with type 'alarmBatch' (see NoGuiConfigSetServlet),
 * which validates everything before saving anything.
 *
 * Usage:
 *   DbxAlarmEditor.open({
 *       srvName    : 'PROD_1A_SS',
 *       cmName     : 'CmSummary',
 *       alarmName  : 'LockWaits',
 *       configData : optional, an already fetched '/api/cc/mgt/config/get' response
 *       list       : optional, [{cmName, name}, ...] for Previous/Next (default: all alarms of the CM)
 *       onSaved    : optional, function(newConfigData) called after a successful save
 *   });
 */
var DbxAlarmEditor = (function()
{
	'use strict';

	// Time range presets (cron: minute hour day-of-month month day-of-week, a leading '!' means NOT within)
	var CRON_PRESETS = [
		{ cron: '* * * * *',       label: 'Always',               desc: 'At any time' },
		{ cron: '* 8-18 * * 1-5',  label: 'Office hours',         desc: 'Monday to Friday, 08:00 to 18:59' },
		{ cron: '!* 8-18 * * 1-5', label: 'Outside office hours', desc: 'NOT Monday to Friday, 08:00 to 18:59' },
		{ cron: '* 0-6 * * *',     label: 'Nights',               desc: 'Every day, 00:00 to 06:59' },
		{ cron: '!* 0-6 * * *',    label: 'Not nights',           desc: 'NOT every day, 00:00 to 06:59 (so 07:00 to 23:59)' }
	];

	var _s = null; // state of the open dialog

	//------------------------------------------------------------------
	// Small helpers
	//------------------------------------------------------------------
	function esc(v)
	{
		if (v === undefined || v === null) return '';
		return String(v).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
	}

	function el(id) { return document.getElementById(id); }

	function injectCss()
	{
		if (el('dbx-ae-css')) return;
		var css = [
			'.dbx-ae-overlay{position:fixed;inset:0;background:rgba(0,0,0,.45);z-index:20000;display:flex;align-items:center;justify-content:center;font-family:inherit;}',
			'.dbx-ae-dialog{background:#fff;color:#212529;border-radius:8px;width:min(760px,96vw);max-height:90vh;display:flex;flex-direction:column;box-shadow:0 8px 30px rgba(0,0,0,.3);font-size:14px;text-align:left;}',
			'.dbx-ae-head{padding:12px 16px;border-bottom:1px solid #dee2e6;display:flex;align-items:flex-start;gap:10px;}',
			'.dbx-ae-title{font-size:16px;font-weight:600;word-break:break-word;}',
			'.dbx-ae-sub{font-size:12px;color:#6c757d;}',
			'.dbx-ae-body{padding:4px 16px 8px;overflow-y:auto;flex:1;}',
			'.dbx-ae-foot{padding:10px 16px;border-top:1px solid #dee2e6;display:flex;align-items:center;gap:10px;flex-wrap:wrap;}',
			'.dbx-ae-row{display:grid;grid-template-columns:170px minmax(0,1fr);gap:12px;padding:10px 0;border-top:1px solid #f1f3f5;align-items:start;}',
			'.dbx-ae-row:first-child{border-top:none;}',
			'.dbx-ae-lbl{font-size:13px;color:#495057;padding-top:5px;word-break:break-word;}',
			'.dbx-ae-lbl b{color:#212529;}',
			'.dbx-ae-help{font-size:12px;color:#6c757d;margin-top:4px;}',
			'.dbx-ae-def{font-size:12px;color:#868e96;}',
			'.dbx-ae-err{font-size:12px;color:#c92a2a;margin-top:4px;}',
			'.dbx-ae-warn{font-size:12px;color:#a86800;margin-top:4px;}',
			'.dbx-ae-in{font-family:monospace;font-size:13px;padding:4px 6px;border:1px solid #ced4da;border-radius:4px;width:100%;box-sizing:border-box;}',
			'.dbx-ae-in.dbx-ae-num{width:160px;}',
			'.dbx-ae-in.dbx-ae-bad{border-color:#c92a2a;background:#fff5f5;}',
			'.dbx-ae-in.dbx-ae-pending,.dbx-ae-chk-pending{box-shadow:0 0 0 2px #ffe066;}',
			'.dbx-ae-btn{font-size:13px;padding:4px 12px;border:1px solid #adb5bd;border-radius:4px;background:#fff;color:#212529;cursor:pointer;white-space:nowrap;}',
			'.dbx-ae-btn:hover{background:#f1f3f5;}',
			'.dbx-ae-btn:disabled{opacity:.5;cursor:default;}',
			'.dbx-ae-btn-primary{background:#0d6efd;border-color:#0d6efd;color:#fff;}',
			'.dbx-ae-btn-primary:hover{background:#0b5ed7;}',
			'.dbx-ae-btn-sm{font-size:12px;padding:1px 8px;}',
			'.dbx-ae-x{border:none;background:none;font-size:22px;line-height:1;cursor:pointer;color:#6c757d;padding:0 4px;}',
			'.dbx-ae-chip{display:inline-block;font-size:12px;padding:2px 8px;border-radius:10px;margin:4px 4px 0 0;}',
			'.dbx-ae-chip-ok{background:#ebfbee;color:#2b8a3e;}',
			'.dbx-ae-chip-bad{background:#fff5f5;color:#c92a2a;}',
			'.dbx-ae-preset{font-size:12px;padding:3px 10px;border:1px solid #adb5bd;border-radius:14px;background:#fff;cursor:pointer;margin:0 4px 4px 0;}',
			'.dbx-ae-preset.dbx-ae-on{background:#e7f1ff;border-color:#0d6efd;color:#0a58ca;}',
			'.dbx-ae-changed{font-size:11px;padding:1px 6px;border-radius:4px;background:#fff3bf;color:#8a6d00;margin-left:6px;white-space:nowrap;}',
			'.dbx-ae-banner{font-size:13px;padding:8px 12px;border-radius:4px;margin:10px 0 2px;}',
			'.dbx-ae-banner-warn{background:#fff9db;color:#8a6d00;}',
			'.dbx-ae-banner-info{background:#e7f5ff;color:#1864ab;}',
			'.dbx-ae-banner-ok{background:#ebfbee;color:#2b8a3e;}',
			'.dbx-ae-banner-err{background:#fff5f5;color:#c92a2a;}'
		].join('\n');
		var style = document.createElement('style');
		style.id = 'dbx-ae-css';
		style.textContent = css;
		document.head.appendChild(style);
	}

	//------------------------------------------------------------------
	// Server calls
	//------------------------------------------------------------------
	function fetchConfig(srvName)
	{
		return fetch('/api/cc/mgt/config/get?srvName=' + encodeURIComponent(srvName))
			.then(function(r) { if (!r.ok) throw new Error('HTTP ' + r.status); return r.json(); });
	}

	/** Same rule as the pages and the server: role 'admin' or the user named 'admin' */
	function fetchIsAdmin()
	{
		return fetch('/login-check')
			.then(function(r) { return r.ok ? r.text() : '{}'; })
			.then(function(txt) {
				try { var j = JSON.parse(txt); return j.isAdmin === true || j.asUserName === 'admin'; }
				catch (e) { return false; }
			})
			.catch(function() { return false; });
	}

	//------------------------------------------------------------------
	// Model: turn one alarm's 'parameters[]' into editable fields
	//------------------------------------------------------------------
	function findCm(configData, cmName)
	{
		var list = (configData && configData.cmList) || [];
		for (var i = 0; i < list.length; i++)
			if (list[i].cmName === cmName) return list[i];
		return null;
	}

	function findAlarm(cmObj, alarmName)
	{
		var alarms = (cmObj && cmObj.alarmSettings && cmObj.alarmSettings.alarms) || [];
		for (var i = 0; i < alarms.length; i++)
			if (alarms[i].name === alarmName) return alarms[i];
		return null;
	}

	/**
	 * kind: 'enabled' | 'cron' | 'bool' | 'int' | 'double' | 'map' | 'regex' | 'text'
	 * The names "<alarm> isAlarmEnabled" and "<alarm> timeRangeCron" are injected by NoGuiConfigGetServlet.
	 */
	function fieldKind(alarmName, p)
	{
		if (p.name === alarmName + ' isAlarmEnabled') return 'enabled';
		if (p.name === alarmName + ' timeRangeCron')  return 'cron';
		var v = p.validatorName || '';
		if (v.indexOf('MapNumberValidator')  !== -1) return 'map';
		if (v.indexOf('RegExpInputValidator') !== -1) return 'regex';
		if (v.indexOf('CronTimeRange')       !== -1) return 'cron';
		switch (p.datatype)
		{
			case 'Boolean': return 'bool';
			case 'Integer': return 'int';
			case 'Double':  return 'double';
			default:        return 'text';
		}
	}

	function buildFields(alarm)
	{
		var fields = [];
		(alarm.parameters || []).forEach(function(p)
		{
			var kind = fieldKind(alarm.name, p);
			var val  = (p.value === null || p.value === undefined) ? '' : String(p.value);
			var def  = (p.defaultValue === null || p.defaultValue === undefined) ? '' : String(p.defaultValue);
			if (kind === 'enabled' || kind === 'bool') { val = String(val.toLowerCase() === 'true'); def = String(def.toLowerCase() === 'true'); }
			fields.push({
				name        : p.name,
				label       : p.name.indexOf(alarm.name + ' ') === 0 ? p.name.substring(alarm.name.length + 1) : p.name,
				kind        : kind,
				isMain      : p.isMainParam === true,
				orig        : val,   // value on the server
				value       : val,   // value in the dialog
				def         : def,
				property    : p.property || '',
				description : p.description || '',
				secret      : val === '***secret***'
			});
		});
		// Order: enabled, main threshold, filters, time range
		var order = { enabled: 0, cron: 3 };
		fields.sort(function(a, b) {
			var oa = a.kind in order ? order[a.kind] : (a.isMain ? 1 : 2);
			var ob = b.kind in order ? order[b.kind] : (b.isMain ? 1 : 2);
			return oa - ob;
		});
		return fields;
	}

	/** Local validation, returns {error: 'blocks save', warn: 'hint only'} */
	function validate(f)
	{
		var v = f.value;
		switch (f.kind)
		{
			case 'int':
				return /^\s*-?\d+\s*$/.test(v) ? {} : { error: 'Must be a whole number' };
			case 'double':
				return (v.trim() !== '' && isFinite(Number(v))) ? {} : { error: 'Must be a number' };
			case 'map':
				var bad = parseMap(v).filter(function(e) { return !e.ok; });
				return bad.length ? { error: bad.length + ' entr' + (bad.length === 1 ? 'y is' : 'ies are') + ' not "regex=number"' } : {};
			case 'regex':
				if (v === '') return {};
				try { new RegExp(v); return {}; }
				catch (e) { return { warn: 'The browser cannot parse this regex (the collector uses Java regex and checks it when saving): ' + e.message }; }
			case 'cron':
				var c = v.replace(/^!/, '').trim().split(/\s+/);
				return c.length === 5 ? {} : { error: 'A cron expression has 5 fields: minute hour day-of-month month day-of-week (a leading ! means NOT within)' };
			default:
				return {};
		}
	}

	/** "regex=number, regex=number" (same format as StringUtil.parseCommaStrToMap / MapNumberValidator) */
	function parseMap(v)
	{
		return String(v).split(',').map(function(s) { return s.trim(); }).filter(function(s) { return s !== ''; }).map(function(entry)
		{
			var i = entry.lastIndexOf('=');
			var key = i > 0 ? entry.substring(0, i).trim() : '';
			var num = i > 0 ? entry.substring(i + 1).trim() : '';
			var ok  = key !== '' && num !== '' && isFinite(Number(num));
			if (ok) { try { new RegExp(key); } catch (e) { ok = false; } }
			return { key: key, num: num, ok: ok, text: entry };
		});
	}

	function pendingFields()
	{
		return _s.fields.filter(function(f) { return f.value !== f.orig; });
	}

	//------------------------------------------------------------------
	// Rendering
	//------------------------------------------------------------------
	function renderField(f, idx)
	{
		var ro   = !_s.isAdmin || f.secret ? ' disabled' : '';
		var id   = 'dbx-ae-f-' + idx;
		var pend = f.value !== f.orig;
		var nonDefault = f.value !== f.def;
		var h = '';

		var lbl = f.kind === 'cron' ? 'Time range' : (f.isMain ? '<b>' + esc(f.label) + '</b>' : esc(f.label));
		h += '<div class="dbx-ae-row" data-idx="' + idx + '">';
		h += '<div class="dbx-ae-lbl">' + lbl + (pend ? '<span class="dbx-ae-changed">changed</span>' : '') + '</div><div>';

		if (f.kind === 'bool')
		{
			h += '<label style="cursor:pointer;"><input type="checkbox" id="' + id + '" class="' + (pend ? 'dbx-ae-chk-pending' : '') + '"' + (f.value === 'true' ? ' checked' : '') + ro + '> ' + (f.value === 'true' ? 'Yes' : 'No') + '</label>';
		}
		else if (f.kind === 'cron')
		{
			var preset = null;
			CRON_PRESETS.forEach(function(p) { if (p.cron === f.value) preset = p; });
			h += '<div>';
			CRON_PRESETS.forEach(function(p) {
				h += '<button type="button" class="dbx-ae-preset' + (p === preset ? ' dbx-ae-on' : '') + '" data-cron="' + esc(p.cron) + '"' + ro + ' title="' + esc(p.cron) + '">' + esc(p.label) + '</button>';
			});
			h += '</div>';
			h += '<input type="text" id="' + id + '" class="dbx-ae-in' + (pend ? ' dbx-ae-pending' : '') + '" value="' + esc(f.value) + '"' + ro + ' spellcheck="false">';
			var desc = preset ? preset.desc : (f.value === f.orig && _s.alarm.timeRangeDescrption ? _s.alarm.timeRangeDescrption : 'Custom (the collector describes it after saving)');
			h += '<div class="dbx-ae-help">' + esc(desc) + ' &middot; cron: minute hour day-of-month month day-of-week, a leading <code>!</code> means NOT within</div>';
		}
		else
		{
			var cls = 'dbx-ae-in' + ((f.kind === 'int' || f.kind === 'double') ? ' dbx-ae-num' : '') + (pend ? ' dbx-ae-pending' : '');
			h += '<input type="text" id="' + id + '" class="' + cls + '" value="' + esc(f.value) + '"' + ro + ' spellcheck="false"' + (f.kind === 'regex' ? ' placeholder="(empty = no filter)"' : '') + '>';
			if (f.kind === 'map')
			{
				h += '<div>';
				parseMap(f.value).forEach(function(e) {
					h += '<span class="dbx-ae-chip ' + (e.ok ? 'dbx-ae-chip-ok' : 'dbx-ae-chip-bad') + '">' + (e.ok ? esc(e.key) + ' &rarr; ' + esc(e.num) : 'invalid: ' + esc(e.text)) + '</span>';
				});
				h += '</div>';
			}
		}

		var v = validate(f);
		var srvErr = _s.serverErrors[f.name];
		if (v.error)  h += '<div class="dbx-ae-err">' + esc(v.error) + '</div>';
		if (srvErr)   h += '<div class="dbx-ae-err">Collector: ' + esc(srvErr) + '</div>';
		if (v.warn)   h += '<div class="dbx-ae-warn">' + esc(v.warn) + '</div>';

		if (f.kind !== 'cron' && f.description)
			h += '<div class="dbx-ae-help">' + esc(f.description) + '</div>';

		h += '<div class="dbx-ae-def">default: <code>' + esc(f.def === '' ? '(empty)' : f.def) + '</code>';
		if (nonDefault && _s.isAdmin && !f.secret)
			h += ' <button type="button" class="dbx-ae-btn dbx-ae-btn-sm" data-reset="' + idx + '">Reset to default</button>';
		h += '</div>';

		h += '</div></div>';
		return h;
	}

	function render()
	{
		var s = _s, cm = s.cmObj, a = s.alarm;
		var enabledIdx = -1;
		s.fields.forEach(function(f, i) { if (f.kind === 'enabled') enabledIdx = i; });
		var enabledField = enabledIdx >= 0 ? s.fields[enabledIdx] : null;

		// ---- header
		var pos  = s.list.length > 1 ? (s.listIdx + 1) + ' of ' + s.list.length : '';
		var head = '<div style="flex:1;min-width:0;">'
			+ '<div class="dbx-ae-title">' + esc(a.name) + '</div>'
			+ '<div class="dbx-ae-sub">' + esc(cm.displayName || cm.cmName) + ' (' + esc(cm.cmName) + ') &middot; ' + esc(s.srvName) + '</div>'
			+ '</div>';
		if (s.list.length > 1)
			head += '<span class="dbx-ae-sub" style="padding-top:4px;">' + pos + '</span>'
				+ '<button type="button" class="dbx-ae-btn dbx-ae-btn-sm" id="dbx-ae-prev" title="Previous alarm (Left arrow)">&lsaquo;</button>'
				+ '<button type="button" class="dbx-ae-btn dbx-ae-btn-sm" id="dbx-ae-next" title="Next alarm (Right arrow)">&rsaquo;</button>';
		head += '<button type="button" class="dbx-ae-x" id="dbx-ae-close" title="Close">&times;</button>';

		// ---- body
		var body = '';
		if (a.description)
			body += '<div class="dbx-ae-help" style="margin:10px 0 4px;font-size:13px;">' + esc(a.description) + '</div>';

		if (!s.isAdmin)
			body += '<div class="dbx-ae-banner dbx-ae-banner-info">Read only. <a href="/index.html?login=open" target="_blank" rel="noopener">Log in as admin</a> to change alarms (then open this dialog again).</div>';

		var why = [];
		if (cm.isCmEnabled === false)           why.push('the Counter is not enabled');
		if (cm.isAlarmEnabled === false)        why.push('alarms are disabled for this Counter');
		if (cm.isSystemAlarmsEnabled === false) why.push('system alarms are disabled for this Counter');
		if (why.length)
			body += '<div class="dbx-ae-banner dbx-ae-banner-warn">This alarm will not fire even when enabled: ' + esc(why.join(', ')) + '.</div>';

		if (s.message)
			body += '<div class="dbx-ae-banner dbx-ae-banner-' + s.message.type + '">' + esc(s.message.text) + '</div>';

		body += '<div>';
		// Enabled switch first, as its own row
		if (enabledField)
		{
			var pend = enabledField.value !== enabledField.orig;
			body += '<div class="dbx-ae-row" data-idx="' + enabledIdx + '"><div class="dbx-ae-lbl"><b>Enabled</b>' + (pend ? '<span class="dbx-ae-changed">changed</span>' : '') + '</div><div>'
				+ '<label style="cursor:pointer;font-size:14px;"><input type="checkbox" id="dbx-ae-f-' + enabledIdx + '" class="' + (pend ? 'dbx-ae-chk-pending' : '') + '"' + (enabledField.value === 'true' ? ' checked' : '') + (s.isAdmin ? '' : ' disabled') + '> '
				+ (enabledField.value === 'true' ? 'The alarm is on' : 'The alarm is off') + '</label>'
				+ (s.serverErrors[enabledField.name] ? '<div class="dbx-ae-err">Collector: ' + esc(s.serverErrors[enabledField.name]) + '</div>' : '')
				+ '</div></div>';
		}
		s.fields.forEach(function(f, i) { if (f.kind !== 'enabled') body += renderField(f, i); });
		body += '</div>';

		// ---- footer
		var pending = pendingFields();
		var blocking = s.fields.some(function(f) { return f.value !== f.orig && validate(f).error; });
		var foot = '<span class="dbx-ae-sub" style="flex:1;min-width:120px;">'
			+ (pending.length ? pending.length + ' pending: ' + esc(pending.map(function(f) { return f.kind === 'enabled' ? 'enabled' : f.kind === 'cron' ? 'time range' : f.label; }).join(', ')) : 'No changes')
			+ '</span>';
		if (s.isAdmin)
		{
			foot += '<label class="dbx-ae-sub" style="cursor:pointer;" title="Unchecked: saved in the collector\'s configuration file (survives a restart)">'
				+ '<input type="checkbox" id="dbx-ae-inmem"' + (s.inMemoryOnly ? ' checked' : '') + '> Only until restart</label>';
			foot += '<button type="button" class="dbx-ae-btn" id="dbx-ae-cancel">' + (pending.length ? 'Discard' : 'Close') + '</button>';
			foot += '<button type="button" class="dbx-ae-btn dbx-ae-btn-primary" id="dbx-ae-save"' + (!pending.length || blocking || s.saving ? ' disabled' : '') + '>' + (s.saving ? 'Saving...' : 'Save') + '</button>';
		}
		else
		{
			foot += '<button type="button" class="dbx-ae-btn" id="dbx-ae-cancel">Close</button>';
		}

		// keep focus + caret while re-rendering on input
		var active = document.activeElement, activeId = active && active.id, caret = null;
		if (active && active.id && active.id.indexOf('dbx-ae-f-') === 0 && active.type === 'text') caret = active.selectionStart;
		var bodyEl = s.root.querySelector('.dbx-ae-body');
		var scroll = bodyEl ? bodyEl.scrollTop : 0;

		s.root.querySelector('.dbx-ae-head').innerHTML = head;
		s.root.querySelector('.dbx-ae-body').innerHTML = body;
		s.root.querySelector('.dbx-ae-foot').innerHTML = foot;

		s.root.querySelector('.dbx-ae-body').scrollTop = scroll;
		if (activeId && el(activeId))
		{
			el(activeId).focus();
			if (caret !== null) { try { el(activeId).setSelectionRange(caret, caret); } catch (e) {} }
		}
		// keep the focus inside the dialog (the focused element may have been re-rendered), so Escape/arrow keys keep working
		if (!s.root.contains(document.activeElement))
			s.root.querySelector('.dbx-ae-dialog').focus();
	}

	//------------------------------------------------------------------
	// Events (delegated on the dialog root, so re-rendering keeps them)
	//------------------------------------------------------------------
	function fieldFromEvent(target)
	{
		var id = target.id || '';
		if (id.indexOf('dbx-ae-f-') !== 0) return null;
		return _s.fields[parseInt(id.substring('dbx-ae-f-'.length), 10)];
	}

	function onInput(e)
	{
		// Text fields: every keystroke ('input'). Checkboxes: 'change' only.
		// (NOT 'change' on text fields: it fires on mousedown of e.g. the Save button, and the re-render would swallow that click)
		var isChk = e.target.type === 'checkbox';
		if (isChk !== (e.type === 'change')) return;

		var f = fieldFromEvent(e.target);
		if (!f) return;
		f.value = e.target.type === 'checkbox' ? String(e.target.checked) : e.target.value;
		delete _s.serverErrors[f.name];
		_s.message = null;
		render();
	}

	function onClick(e)
	{
		var t = e.target.closest('button, input');
		if (!t) { if (e.target === _s.root) confirmThen(close); return; } // click on the backdrop
		if (t.id === 'dbx-ae-close' || t.id === 'dbx-ae-cancel') { confirmThen(close); return; }
		if (t.id === 'dbx-ae-prev') { confirmThen(function() { step(-1); }); return; }
		if (t.id === 'dbx-ae-next') { confirmThen(function() { step(+1); }); return; }
		if (t.id === 'dbx-ae-save') { save(); return; }
		if (t.id === 'dbx-ae-inmem') { _s.inMemoryOnly = t.checked; return; }
		if (t.hasAttribute('data-reset'))
		{
			var f = _s.fields[parseInt(t.getAttribute('data-reset'), 10)];
			f.value = f.def;
			delete _s.serverErrors[f.name];
			render();
			return;
		}
		if (t.hasAttribute('data-cron'))
		{
			_s.fields.forEach(function(f) { if (f.kind === 'cron') { f.value = t.getAttribute('data-cron'); delete _s.serverErrors[f.name]; } });
			render();
		}
	}

	function onKey(e)
	{
		if (e.key === 'Escape') { e.stopPropagation(); confirmThen(close); return; }

		// Left/Right arrow = Previous/Next alarm, but not while typing in a text field
		if ((e.key === 'ArrowLeft' || e.key === 'ArrowRight') && !e.altKey && !e.ctrlKey && !e.metaKey && !e.shiftKey)
		{
			var t = e.target;
			var isTextField = (t.tagName === 'INPUT' && t.type !== 'checkbox') || t.tagName === 'TEXTAREA' || t.tagName === 'SELECT' || t.isContentEditable;
			if (isTextField || _s.list.length < 2) return;
			e.preventDefault();
			e.stopPropagation();
			var dir = e.key === 'ArrowLeft' ? -1 : +1;
			confirmThen(function() { step(dir); });
		}
	}

	function confirmThen(fn)
	{
		var n = pendingFields().length;
		if (n && !window.confirm('You have ' + n + ' unsaved change' + (n === 1 ? '' : 's') + '. Discard ' + (n === 1 ? 'it' : 'them') + '?'))
			return;
		fn();
	}

	//------------------------------------------------------------------
	// Load / navigate / save
	//------------------------------------------------------------------
	function loadAlarm(cmName, alarmName)
	{
		var cmObj = findCm(_s.configData, cmName);
		var alarm = findAlarm(cmObj, alarmName);
		if (!cmObj || !alarm)
		{
			_s.root.querySelector('.dbx-ae-body').innerHTML = '<div class="dbx-ae-banner dbx-ae-banner-err">Alarm "' + esc(alarmName) + '" was not found in Counter "' + esc(cmName) + '".</div>';
			return false;
		}
		_s.cmObj  = cmObj;
		_s.alarm  = alarm;
		_s.fields = buildFields(alarm);
		_s.serverErrors = {};
		for (var i = 0; i < _s.list.length; i++)
			if (_s.list[i].cmName === cmName && _s.list[i].name === alarmName) { _s.listIdx = i; break; }
		render();
		return true;
	}

	function step(dir)
	{
		if (_s.list.length < 2) return;
		_s.listIdx = (_s.listIdx + dir + _s.list.length) % _s.list.length;
		_s.message = null;
		var e = _s.list[_s.listIdx];
		loadAlarm(e.cmName, e.name);
	}

	function save()
	{
		var pending = pendingFields();
		if (!pending.length || _s.saving) return;

		var change = {};
		pending.forEach(function(f) { change[f.name] = f.value; });

		var body = {
			type     : 'alarmBatch',
			saveType : _s.inMemoryOnly ? 'IN_MEMORY' : 'THIS_SERVER',
			cmName   : _s.cmObj.cmName,
			optName  : _s.alarm.name,
			change   : change
		};

		_s.saving = true;
		_s.message = null;
		render();

		var cmName = _s.cmObj.cmName, alarmName = _s.alarm.name, srvName = _s.srvName;
		fetch('/api/cc/mgt/config/set?srvName=' + encodeURIComponent(srvName), {
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
		})
		.then(function(res)
		{
			if (!_s) return; // closed while saving
			_s.saving = false;

			if (res.status === 403)
			{
				_s.isAdmin = false;
				_s.message = { type: 'err', text: res.json.message || 'You need to be logged in with admin rights to change the configuration.' };
				render();
				return;
			}
			if (res.status !== 200 || res.json.status !== 'success')
			{
				_s.serverErrors = res.json.errors || {};
				var txt = [res.json.info, res.json.error, res.json.message].filter(function(x) { return x; }).join(' - ');
				_s.message = { type: 'err', text: 'Not saved. ' + (txt || ('HTTP ' + res.status)) };
				render();
				return;
			}

			// Saved: re-read the configuration so we show what the collector has now
			var savedInfo = { type: 'ok', text: 'Saved' + (body.saveType === 'IN_MEMORY' ? ' (only until the collector restarts).' : '.') + (res.json.isBootNeeded ? ' The collector needs a restart for this to take effect.' : '') };
			return fetchConfig(srvName).then(function(newConfig)
			{
				if (!_s) return;
				_s.configData = newConfig;
				loadAlarm(cmName, alarmName);
				_s.message = savedInfo;
				render();
				if (typeof _s.onSaved === 'function')
				{
					try { _s.onSaved(newConfig); } catch (e) { console.error('DbxAlarmEditor: onSaved callback failed', e); }
				}
			});
		})
		.catch(function(err)
		{
			if (!_s) return;
			_s.saving = false;
			_s.message = { type: 'err', text: 'Not saved. ' + err };
			render();
		});
	}

	//------------------------------------------------------------------
	// Open / close
	//------------------------------------------------------------------
	function open(opts)
	{
		if (!opts || !opts.srvName || !opts.cmName || !opts.alarmName)
		{
			console.error('DbxAlarmEditor.open(): srvName, cmName and alarmName are required', opts);
			return;
		}
		if (_s) close();
		injectCss();

		// Bootstrap 4 and 5 modals trap the focus inside the open modal, so put the dialog INSIDE it (if any)
		var host = document.querySelector('.modal.show') || document.body;

		var root = document.createElement('div');
		root.className = 'dbx-ae-overlay';
		root.innerHTML = '<div class="dbx-ae-dialog" role="dialog" aria-modal="true" tabindex="-1" style="outline:none;">'
			+ '<div class="dbx-ae-head"><div class="dbx-ae-title">' + esc(opts.alarmName) + '</div></div>'
			+ '<div class="dbx-ae-body"><div class="dbx-ae-help" style="padding:20px 0;">Loading...</div></div>'
			+ '<div class="dbx-ae-foot"></div>'
			+ '</div>';
		host.appendChild(root);

		_s = {
			root         : root,
			srvName      : opts.srvName,
			onSaved      : opts.onSaved,
			configData   : opts.configData || null,
			list         : Array.isArray(opts.list) ? opts.list.slice() : null,
			listIdx      : 0,
			isAdmin      : false,
			inMemoryOnly : false,
			saving       : false,
			message      : null,
			fields       : [],
			serverErrors : {}
		};

		root.addEventListener('input',  onInput);
		root.addEventListener('change', onInput);
		root.addEventListener('click',  onClick);
		root.addEventListener('keydown', onKey);

		var configPromise = _s.configData ? Promise.resolve(_s.configData) : fetchConfig(opts.srvName);
		Promise.all([configPromise, fetchIsAdmin()]).then(function(res)
		{
			if (!_s || _s.root !== root) return;
			_s.configData = res[0];
			_s.isAdmin    = res[1];
			if (!_s.list)
			{
				var cmObj = findCm(_s.configData, opts.cmName);
				_s.list = ((cmObj && cmObj.alarmSettings && cmObj.alarmSettings.alarms) || []).map(function(a) { return { cmName: opts.cmName, name: a.name }; });
			}
			loadAlarm(opts.cmName, opts.alarmName);

			// focus the dialog, so Escape works (and the page behind does not get the keys)
			var dlg = root.querySelector('.dbx-ae-dialog');
			if (dlg && !dlg.contains(document.activeElement)) dlg.focus();
		})
		.catch(function(err)
		{
			if (!_s || _s.root !== root) return;
			root.querySelector('.dbx-ae-body').innerHTML = '<div class="dbx-ae-banner dbx-ae-banner-err">Failed to load the configuration: ' + esc(err) + '</div>';
			root.querySelector('.dbx-ae-foot').innerHTML = '<button type="button" class="dbx-ae-btn" id="dbx-ae-cancel">Close</button>';
		});
	}

	function close()
	{
		if (!_s) return;
		_s.root.remove();
		_s = null;
	}

	return { open: open, close: close, _parseMap: parseMap, _fieldKind: fieldKind };
})();
