/**
 * dbxTheme.js — Light / dark colour theme for DbxCentral pages.
 *
 * Follows the OS / browser setting (prefers-color-scheme) by default, and lets the user override it:
 *   'auto'  : follow the OS (default), also when the OS switches while the page is open
 *   'light' : always light
 *   'dark'  : always dark
 * The choice is saved in localStorage ('dbxtune.colorScheme'), shared by all pages that include this file,
 * and other open tabs follow a change at once.
 *
 * Sets on <html>: data-theme="light|dark" (page CSS) and data-bs-theme="light|dark" (Bootstrap 5.3 components).
 * Include it FIRST in <head> so the page is never drawn in the wrong theme. Plain JS on purpose (no jQuery).
 *
 * The switch (a small dropdown: icon + Auto / Light / Dark) is added to the navbar by itself, just before the
 * login/user part (the 'ul.navbar-nav' holding #dbx-nb-isLoggedIn-div), so the navbar copies need no markup.
 * Pages without a navbar just follow the saved choice.
 *
 * Pages with their OWN dark design (graph.html) mark <html data-dbx-theme-own>: then data-theme/data-bs-theme are
 * NOT set (the page's CSS/Bootstrap stay as they are), the page reads DbxTheme.effective() and listens to onChange().
 * graph.html: see dbxGraphColorSchema() in dbxcentral.graph.js (the old URL parameter 'cs' is ignored).
 *
 * Usage:  DbxTheme.get()         -> 'auto' | 'light' | 'dark'   (the saved choice)
 *         DbxTheme.effective()   -> 'light' | 'dark'            (what is shown now)
 *         DbxTheme.set('dark')
 *         DbxTheme.onChange(function(effective, choice) { ... })
 *         DbxTheme.panelMode({ key, mount, apply })  -> per panel [Auto | Light | Dark] control (see below)
 */
var DbxTheme = (function()
{
	'use strict';

	var KEY       = 'dbxtune.colorScheme';
	var listeners = [];
	var mql       = window.matchMedia ? window.matchMedia('(prefers-color-scheme: dark)') : null;
	// Light = an own SVG sun: Font Awesome 7's fa-sun (regular and solid) looks like the Settings gear at navbar size
	var SUN_SVG   = '<svg width="1.05em" height="1.05em" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true" style="vertical-align:-0.15em">'
	              + '<circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4"/></svg>';
	var ICON      = { auto: '<i class="fa-solid fa-circle-half-stroke"></i>', light: SUN_SVG, dark: '<i class="fa-regular fa-moon"></i>' };

	function get()
	{
		var v = null;
		try { v = window.localStorage.getItem(KEY); } catch (e) {} // storage may be blocked
		return (v === 'light' || v === 'dark') ? v : 'auto';
	}

	function effective()
	{
		var choice = get();
		if (choice !== 'auto') return choice;
		return (mql && mql.matches) ? 'dark' : 'light';
	}

	function apply()
	{
		var eff = effective(), root = document.documentElement;
		if (!root.hasAttribute('data-dbx-theme-own'))
		{
			root.setAttribute('data-theme',    eff);
			root.setAttribute('data-bs-theme', eff);
		}
		updateSwitch();
		listeners.forEach(function(fn) { try { fn(eff, get()); } catch (e) { console.error('DbxTheme listener failed', e); } });
	}

	function set(choice)
	{
		try
		{
			if (choice === 'light' || choice === 'dark') window.localStorage.setItem(KEY, choice);
			else                                         window.localStorage.removeItem(KEY);
		}
		catch (e) {}
		apply();
	}

	function onChange(fn) { if (typeof fn === 'function') listeners.push(fn); }

	//------------------------------------------------------------------
	// Navbar switch
	//------------------------------------------------------------------
	function switchText()
	{
		var cur = get();
		return cur === 'auto' ? 'Auto (now ' + effective() + ')' : cur === 'light' ? 'Light' : 'Dark';
	}

	function mountSwitch()
	{
		if (document.getElementById('dbx-theme-nav')) return;
		var collapse = document.querySelector('nav.navbar .navbar-collapse');
		if (!collapse) return; // no navbar on this page

		var ul = document.createElement('ul');
		ul.id        = 'dbx-theme-nav';
		ul.className = 'navbar-nav';
		ul.innerHTML =
			  '<li class="nav-item dropdown">'
			+   '<a class="nav-link dropdown-toggle" href="#" id="dbx-theme-toggle" role="button" data-bs-toggle="dropdown" aria-expanded="false">'
			+     '<span class="dbx-theme-icon"></span></a>'
			+   '<ul class="dropdown-menu dropdown-menu-end" aria-labelledby="dbx-theme-toggle" style="min-width:250px;">'
			+     '<li><h6 class="dropdown-header">Colour theme (remembered in this browser)</h6></li>'
			+     item('auto',  'Auto',  'Follow the operating system')
			+     item('light', 'Light', 'Always light')
			+     item('dark',  'Dark',  'Always dark')
			+   '</ul>'
			+ '</li>';

		// just before the login/user part, else last
		var user = document.getElementById('dbx-nb-isLoggedIn-div') || document.getElementById('dbx-nb-isLoggedOut-div');
		var userUl = user ? user.closest('ul.navbar-nav') : null;
		// the login <ul> may also hold other things first (e.g. time span buttons, DbxCentralPageTemplate pages):
		// then go right before the login part INSIDE it, so the switch is next to Login like on graph.html
		var firstUserPart = userUl ? userUl.querySelector(':scope > #dbx-nb-isLoggedIn-div, :scope > #dbx-nb-isLoggedOut-div') : null;
		if (firstUserPart && firstUserPart.previousElementSibling) userUl.insertBefore(ul, firstUserPart);
		else if (userUl && userUl.parentNode === collapse)          collapse.insertBefore(ul, userUl);
		else                                                         collapse.appendChild(ul);

		ul.addEventListener('click', function(e) {
			var a = e.target.closest('[data-dbx-theme]');
			if (!a) return;
			e.preventDefault();
			set(a.getAttribute('data-dbx-theme'));
		});
		updateSwitch();
	}

	function item(v, label, sub)
	{
		return '<li><a class="dropdown-item d-flex align-items-start gap-2" href="#" data-dbx-theme="' + v + '" role="menuitemradio">'
			+ '<span style="width:16px;text-align:center;margin-top:1px;">' + ICON[v] + '</span>'
			+ '<span class="flex-grow-1">' + label + '<br><small class="dbx-theme-sub" data-v="' + v + '">' + sub + '</small></span>'
			+ '<i class="fa-solid fa-check dbx-theme-check" style="margin-top:4px;"></i></a></li>';
	}

	function updateSwitch()
	{
		var nav = document.getElementById('dbx-theme-nav');
		if (!nav) return;
		var cur = get();
		nav.querySelector('.dbx-theme-icon').innerHTML = ICON[cur];
		var toggle = document.getElementById('dbx-theme-toggle');
		toggle.title = 'Colour theme: ' + switchText();
		toggle.setAttribute('aria-label', toggle.title);
		nav.querySelectorAll('[data-dbx-theme]').forEach(function(a) {
			var on = a.getAttribute('data-dbx-theme') === cur;
			a.classList.toggle('active', on);
			a.setAttribute('aria-checked', String(on));
			a.querySelector('.dbx-theme-check').style.visibility = on ? 'visible' : 'hidden';
		});
		var autoSub = nav.querySelector('.dbx-theme-sub[data-v="auto"]');
		if (autoSub) autoSub.textContent = 'Follow the operating system (now ' + (mql && mql.matches ? 'dark' : 'light') + ')';
	}

	//------------------------------------------------------------------
	// Per panel override: a small [Auto | Light | Dark] control
	//------------------------------------------------------------------
	/**
	 * For panels/dialogs that can be shown in another scheme than the page (their own "Dark mode" choice).
	 *   opts.key   : name of the panel, the choice is saved in localStorage 'dbxtune.panelTheme.<key>'
	 *   opts.mount : element or selector (or a function returning one) where the control is rendered;
	 *                may not exist yet (injected later): call refresh() once it does
	 *   opts.apply : function(isDark) - show the panel dark/light; called at once, on a click, and when the
	 *                page scheme changes while the panel is on 'Auto'
	 * Auto (default) = follow the page. Returns { isDark(), get(), refresh() }.
	 */
	function panelMode(opts)
	{
		var pkey = 'dbxtune.panelTheme.' + opts.key;
		var PM_TITLE = { auto: 'Auto: same as the page', light: 'Always light (this panel only)', dark: 'Always dark (this panel only)' };

		function pget()
		{
			var v = null;
			try { v = window.localStorage.getItem(pkey); } catch (e) {}
			return (v === 'light' || v === 'dark') ? v : 'auto';
		}
		function pset(v)
		{
			try
			{
				if (v === 'light' || v === 'dark') window.localStorage.setItem(pkey, v);
				else                               window.localStorage.removeItem(pkey);
			}
			catch (e) {}
			refresh();
		}
		function isDarkNow()
		{
			var v = pget();
			return v === 'auto' ? effective() === 'dark' : v === 'dark';
		}
		function mountEl()
		{
			var m = (typeof opts.mount === 'function') ? opts.mount() : opts.mount;
			if (typeof m === 'string') m = document.querySelector(m);
			if (m && m.jquery) m = m[0];
			return m || null;
		}
		function render()
		{
			var m = mountEl();
			if (!m) return;
			var cur = pget();
			if (!m.querySelector('.dbx-pm'))
			{
				m.innerHTML = '<button type="button" class="dbx-pm" aria-haspopup="menu" aria-expanded="false">'
					+ '<span class="dbx-pm-lbl">Theme</span><span class="dbx-pm-icon"></span><span class="dbx-pm-caret">&#9662;</span></button>';
				m.querySelector('.dbx-pm').addEventListener('click', function(e) {
					e.preventDefault(); e.stopPropagation(); // the panel headers are drag handles / have own click handlers
					if (pmMenu && pmMenu._btn === this) { closePmMenu(); return; } // 2nd click: close
					openPmMenu(this, pget(), isDarkNow(), pset);
				});
			}
			var btn = m.querySelector('.dbx-pm');
			btn.querySelector('.dbx-pm-icon').innerHTML = ICON[cur];
			btn.title = 'Colours for this panel: ' + (cur === 'auto' ? 'Auto (same as the page, now ' + effective() + ')' : PM_TITLE[cur]);
			btn.classList.toggle('dbx-pm-fixed', cur !== 'auto');
		}
		function refresh()
		{
			injectPmCss();
			render();
			try { opts.apply(isDarkNow()); } catch (e) { console.error('DbxTheme.panelMode(' + opts.key + ') apply failed', e); }
		}

		onChange(function() { if (pget() === 'auto') refresh(); else render(); });
		window.addEventListener('storage', function(e) { if (e.key === pkey) refresh(); }); // other tab
		if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', refresh);
		else                                   refresh();

		return { isDark: isDarkNow, get: pget, refresh: refresh };
	}

	/** The small Auto/Light/Dark menu of a panel control: one shared element on <body> (never clipped by a panel) */
	var pmMenu = null;
	function openPmMenu(btn, cur, dark, choose)
	{
		closePmMenu();
		var PM_SUB = { auto: 'Same as the page (now ' + effective() + ')', light: 'Always light, this panel only', dark: 'Always dark, this panel only' };
		pmMenu = document.createElement('div');
		pmMenu.className = 'dbx-pm-menu' + (dark ? ' dbx-pm-menu-dark' : '');
		pmMenu.setAttribute('role', 'menu');
		pmMenu.innerHTML = ['auto', 'light', 'dark'].map(function(v) {
			return '<button type="button" role="menuitemradio" aria-checked="' + (v === cur) + '" data-pm="' + v + '"' + (v === cur ? ' class="dbx-pm-on"' : '') + '>'
				+ '<span class="dbx-pm-mi">' + ICON[v] + '</span>'
				+ '<span><b>' + v.charAt(0).toUpperCase() + v.slice(1) + '</b><small>' + PM_SUB[v] + '</small></span>'
				+ '<span class="dbx-pm-chk">' + (v === cur ? '&#10003;' : '') + '</span></button>';
		}).join('');
		document.body.appendChild(pmMenu);
		btn.setAttribute('aria-expanded', 'true');

		// below the button, right edge aligned, kept inside the window
		var r = btn.getBoundingClientRect(), w = pmMenu.offsetWidth, h = pmMenu.offsetHeight;
		var left = Math.max(4, Math.min(r.right - w, window.innerWidth - w - 4));
		var top  = (r.bottom + 2 + h <= window.innerHeight) ? r.bottom + 2 : Math.max(4, r.top - h - 2);
		pmMenu.style.left = left + 'px';
		pmMenu.style.top  = top  + 'px';

		pmMenu.addEventListener('click', function(e) {
			var b = e.target.closest('[data-pm]');
			if (!b) return;
			e.stopPropagation();
			closePmMenu();
			choose(b.getAttribute('data-pm'));
		});
		pmMenu._btn = btn;
		var first = pmMenu.querySelector('.dbx-pm-on') || pmMenu.querySelector('button');
		if (first) first.focus();
	}
	function closePmMenu()
	{
		if (!pmMenu) return;
		if (pmMenu._btn) pmMenu._btn.setAttribute('aria-expanded', 'false');
		pmMenu.remove();
		pmMenu = null;
	}
	document.addEventListener('mousedown', function(e) { if (pmMenu && !pmMenu.contains(e.target) && !(pmMenu._btn && pmMenu._btn.contains(e.target))) closePmMenu(); }, true);
	document.addEventListener('keydown',   function(e) { if (pmMenu && e.key === 'Escape') closePmMenu(); });
	window.addEventListener('resize', closePmMenu);
	window.addEventListener('scroll', function(e) { if (pmMenu && !pmMenu.contains(e.target)) closePmMenu(); }, true);

	function injectPmCss()
	{
		if (document.getElementById('dbx-pm-css')) return;
		var st = document.createElement('style');
		st.id = 'dbx-pm-css';
		st.textContent =
			  '.dbx-pm{display:inline-flex;align-items:center;gap:3px;vertical-align:middle;border:1px solid rgba(128,128,128,.6);border-radius:4px;'
			+ 'background:transparent;color:inherit;padding:0 5px;margin:0;font:inherit;font-size:0.92em;line-height:1.35;cursor:pointer;}'
			+ '.dbx-pm:hover{background:rgba(128,128,128,.18);}'
			+ '.dbx-pm.dbx-pm-fixed{border-color:#0d6efd;box-shadow:inset 0 0 0 1px #0d6efd;}' // Light/Dark override: make it visible
			+ '.dbx-pm-caret{font-size:0.75em;opacity:.8;}'
			+ '.dbx-pm-lbl{margin-right:2px;}'
			+ '.dbx-pm-menu{position:fixed;z-index:2147483000;min-width:230px;padding:4px 0;border-radius:6px;font-size:13px;text-align:left;'
			+ 'background:#fff;color:#212529;border:1px solid #ced4da;box-shadow:0 4px 16px rgba(0,0,0,.25);}'
			+ '.dbx-pm-menu button{display:flex;align-items:flex-start;gap:8px;width:100%;border:0;background:none;color:inherit;padding:5px 12px;font:inherit;text-align:left;cursor:pointer;}'
			+ '.dbx-pm-menu button:hover,.dbx-pm-menu button:focus{background:#e9ecef;outline:none;}'
			+ '.dbx-pm-menu button.dbx-pm-on{background:#e7f1ff;}'
			+ '.dbx-pm-menu small{display:block;font-size:11px;color:#6c757d;}'
			+ '.dbx-pm-mi{width:16px;text-align:center;margin-top:1px;}'
			+ '.dbx-pm-chk{margin-left:auto;padding-left:8px;color:#0d6efd;}'
			+ '.dbx-pm-menu-dark{background:#2b3035;color:#dee2e6;border-color:#495057;box-shadow:0 4px 16px rgba(0,0,0,.6);}'
			+ '.dbx-pm-menu-dark button:hover,.dbx-pm-menu-dark button:focus{background:#343a40;}'
			+ '.dbx-pm-menu-dark button.dbx-pm-on{background:#1a2b40;}'
			+ '.dbx-pm-menu-dark small{color:#adb5bd;}'
			+ '.dbx-pm-menu-dark .dbx-pm-chk{color:#6ea8fe;}';
		document.head.appendChild(st);
	}

	//------------------------------------------------------------------
	// Init
	//------------------------------------------------------------------
	// OS switched (e.g. at sunset): the page only changes in 'auto', the "(now ...)" text always
	if (mql)
	{
		var osChanged = function() { if (get() === 'auto') apply(); else updateSwitch(); };
		if (mql.addEventListener) mql.addEventListener('change', osChanged);
		else if (mql.addListener) mql.addListener(osChanged); // older Safari
	}
	// Changed in another tab
	window.addEventListener('storage', function(e) { if (e.key === KEY) apply(); });

	apply(); // now, while still in <head>

	if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', mountSwitch);
	else                                   mountSwitch();

	return { get: get, set: set, effective: effective, onChange: onChange, panelMode: panelMode };
})();
