/* jshint esversion: 6 */

/**
 * Utility function: Get a specififc parameter from the window URL
 * @param {*} key 
 * @param {*} defaultValue 
 */
function getParameter(key, defaultValue)
{
	// const inputUrl = new URL(window.location.href);
	// const value = inputUrl.searchParams.get(key);
	// return inputUrl.searchParams.has(key) ? value : defaultValue;

	var vars = [], hash;
    var hashes = window.location.href.slice(window.location.href.indexOf('?') + 1).split('&');
    for(var i = 0; i < hashes.length; i++)
    {
        hash = hashes[i].split('=');
        vars.push(hash[0]);
        vars[hash[0]] = hash[1];
	}
	
	//return vars.hasOwnProperty(key) ? vars[key] : defaultValue;
	//return vars.hasOwnProperty(key) ? decodeURIComponent(vars[key]) : defaultValue;
//	var retValue = vars.hasOwnProperty(key) ? decodeURIComponent(vars[key]) : defaultValue;
	var retValue = vars.hasOwnProperty(key) ? decodeURIComponent(vars[key].replace(/\+/g, ' ')) : defaultValue;
	//console.log("getParameter(key='"+key+"',default='"+defaultValue+"') <<--- '"+retValue+"'.");
	return retValue;
}

/**
 * Utility function: Get CHECK if parameter is part of the URL parameters
 * @param {*} key 
 */
function isParameter(key)
{
	var vars = [], hash;
    var hashes = window.location.href.slice(window.location.href.indexOf('?') + 1).split('&');
    for(var i = 0; i < hashes.length; i++)
    {
        hash = hashes[i].split('=');
        vars.push(hash[0]);
        vars[hash[0]] = hash[1];
	}

	if (vars.hasOwnProperty(key))
		return true;
	return false;
}

///**
// * Utility function: get Current login name. If we are not logged in a blank field will be returned.
// * TODO: possibly better to do this async... with a callback function as a parameter
// */
//function getCurrentLogin()
//{
//	var userName = "";
//	var isLogin  = false;
//	console.log("getCurrentLogin(): /login-check");
//	$.ajax(
//	{
//		url: "/login-check",
//		type: 'get',
//		async: false,   // to call it one by one (async: true = spawn away a bunch of them in the background)
//
//		success: function(data, status)
//		{
//			var jsonResp = JSON.parse(data);
//			console.log("getCurrentLogin(): jsonResp", jsonResp);
//			
//			userName = jsonResp.userName;
//			isLogin  = jsonResp.isLogin;
//		},
//		error: function(xhr, desc, err) 
//		{
//			console.log(xhr);
//			console.log("Details: " + desc + "\nError: " + err);
//		}
//	}); // end: ajax call
//	
//	if (isLogin === false)
//		return "";
//	return userName;
//}

/**
 * Utility function: check if a user is logged in, a callback function will be called on completion
 */
function isLoggedIn(callback)
{
	console.log("getCurrentLogin(callback): /login-check");
	$.ajax(
	{
		url: "/login-check",
		type: 'get',
		// async: false,   // to call it one by one (async: true = spawn away a bunch of them in the background)

		success: function(data, status)
		{
			var jsonResp = JSON.parse(data);
			console.log("isLoggedIn(): jsonResp", jsonResp);
			
			var asUserName = jsonResp.asUserName;
			var isLoggedIn = jsonResp.isLoggedIn;
			var isAdmin    = false;
			
			if (jsonResp.hasOwnProperty("isAdmin"))
				isAdmin    = jsonResp.isAdmin;

			// Set some stuff in the NavigationBar (elements may not exist on all pages)
			var elIn   = document.getElementById("dbx-nb-isLoggedIn-div");
			var elOut  = document.getElementById("dbx-nb-isLoggedOut-div");
			var elUser = document.getElementById("dbx-nb-isLoggedInUser-div");
			if (isLoggedIn)
			{
				if (elIn)   elIn  .style.display = "block";
				if (elOut)  elOut .style.display = "none";
				if (elUser) elUser.textContent   = asUserName;
			}
			else
			{
				if (elIn)   elIn  .style.display = "none";
				if (elOut)  elOut .style.display = "block";
				if (elUser) elUser.textContent   = "";
			}
			
			callback(isLoggedIn, asUserName, isAdmin);
		},
		error: function(xhr, desc, err) 
		{
			console.log(xhr);
			console.log("Details: " + desc + "\nError: " + err);
		}
	}); // end: ajax call
}

/**
 * Utility function: Create a HTML Table using a JSON input
 * grabbed from: http://www.encodedna.com/javascript/populate-json-data-to-html-table-using-javascript.htm
 */
function jsonToTable(json, stripHtmlInCells) 
{
	// EXTRACT json VALUES FOR HTML HEADER. 
	var col = [];
	for (var i = 0; i < json.length; i++) {
		for (var key in json[i]) {
			if (col.indexOf(key) === -1) {
				col.push(key);
			}
		}
	}

	// CREATE DYNAMIC TABLE.
	var table = document.createElement("table");
	var thead = table.createTHead();
	var tbody = table.createTBody();

	var borderStyle = "1px solid black";
	table.style.border = borderStyle;
	table.style.borderCollapse = "collapse";

	// CREATE HTML TABLE HEADER ROW USING THE EXTRACTED HEADERS ABOVE.
	var tr = thead.insertRow(-1);                   // TABLE ROW.

	for (let i = 0; i < col.length; i++) {
		var th = document.createElement("th");      // TABLE HEADER.
		th.noWrap = true;
		th.style.border = borderStyle;
		th.innerHTML = col[i];
		tr.appendChild(th);
	}

	// ADD JSON DATA TO THE TABLE AS ROWS.
	for (let i = 0; i < json.length; i++) {

		tr = tbody.insertRow(-1);

		for (var j = 0; j < col.length; j++) {
			var tabCell = tr.insertCell(-1);
			tabCell.noWrap = true;
			tabCell.style.border = borderStyle;
			
			var originTxt = json[i][col[j]];

			// Render cell value directly — full content always visible;
			// details available via the row-detail modal (click any row).
			tabCell.innerHTML = originTxt;
		}
	}
	return table;
}

function stripHtml(html)
{
	var tmp = document.createElement("DIV");
	tmp.innerHTML = html;
	return tmp.textContent || tmp.innerText || "";
}

function isHTML(str) {
	var a = document.createElement('div');
	a.innerHTML = str;

	for (var c = a.childNodes, i = c.length; i--; ) 
	{
		if (c[i].nodeType == 1) 
			return true; 
	}

	return false;
}

/**
 * detect IE
 * returns version of IE or false, if browser is not Internet Explorer
 */
function detectIE() 
{
	var ua = window.navigator.userAgent;

	// Test values; Uncomment to check result …

	// IE 10
	// ua = 'Mozilla/5.0 (compatible; MSIE 10.0; Windows NT 6.2; Trident/6.0)';

	// IE 11
	// ua = 'Mozilla/5.0 (Windows NT 6.3; Trident/7.0; rv:11.0) like Gecko';

	// Edge 12 (Spartan)
	// ua = 'Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.71 Safari/537.36 Edge/12.0';

	// Edge 13
	// ua = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2486.0 Safari/537.36 Edge/13.10586';

	console.log("Browser UserAgent: " + ua);

	var msie = ua.indexOf('MSIE ');
	if (msie > 0) {
		// IE 10 or older => return version number
		return parseInt(ua.substring(msie + 5, ua.indexOf('.', msie)), 10);
	}

	var trident = ua.indexOf('Trident/');
	if (trident > 0) {
		// IE 11 => return version number
		var rv = ua.indexOf('rv:');
		return parseInt(ua.substring(rv + 3, ua.indexOf('.', rv)), 10);
	}

	var edge = ua.indexOf('Edge/');
	if (edge > 0) {
		// Edge (IE 12+) => return version number
		return parseInt(ua.substring(edge + 5, ua.indexOf('.', edge)), 10);
	}

	// other browser
	return false;
}



/** 
 * moment.min(...) do not accept undefined... so wrap the moment.min 
 */
function dbxMinDt(dt1, dt2)
{
	if (dt1 !== undefined && dt2 !== undefined)
		return moment.min(dt1, dt2);
	if (dt1 !== undefined)
		return dt1;
	if (dt2 !== undefined)
		return dt2;
	return undefined;
}

/** 
 * moment.max(...) do not accept undefined... so wrap the moment.max 
 */
function dbxMaxDt(dt1, dt2)
{
	if (dt1 !== undefined && dt2 !== undefined)
		return moment.max(dt1, dt2);
	if (dt1 !== undefined)
		return dt1;
	if (dt2 !== undefined)
		return dt2;
	return undefined;
}




/**
 * Make the windows tab title "blink"
 * Usage: pageTitleNotification.on("New Message!", 1000);
 *        pageTitleNotification.off
 * grabbed from: https://github.com/curttimson/Flashing-Page-Title-Notification/blob/master/src/PageTitleNotification.js
 */
(function(window, document) 
{
	window.pageTitleNotification = (function () 
	{
		var config = {
			currentTitle: null,
			interval: null
		};

		var on = function (notificationText, intervalSpeed) {
			if (!config.interval) {
				config.currentTitle = document.title;
				config.interval = window.setInterval(function() {
					document.title = (config.currentTitle === document.title) ? notificationText : config.currentTitle;
				}, (intervalSpeed) ? intervalSpeed : 1000);
			}
		};

		var off = function () {
			window.clearInterval(config.interval);
			config.interval = null;
			if (config.currentTitle !== null)
				document.title = config.currentTitle;
		};

		return {
			on: on,
			off: off
		};
	})();
}(window, document));




/**
 * this function will return us an object with a "set" and "get" method
 * using either localStorage if available, or defaulting to document.cookie
 */
function getStorage(key_prefix) 
{
    if (window.localStorage) {
        // use localStorage:
        return {
            set: function(id, data) {
				//console.log("storedData[localStorage].set(): id="+id+", val="+data);
                localStorage.setItem(key_prefix+id, data);
            },
            get: function(id) {
				var val = localStorage.getItem(key_prefix+id); 
				//console.log("storedData[localStorage].get(): id="+id+", val="+val);
                return val;
            }
        };
    } else {
        // use document.cookie:
        return {
            set: function(id, data) {
				//console.log("storedData[cookie].set(): id="+id+", val="+data);
                document.cookie = key_prefix+id+'='+encodeURIComponent(data);
            },
            get: function(id, data) {
                var cookies = document.cookie, parsed = {};
                cookies.replace(/([^=]+)=([^;]*);?\s*/g, function(whole, key, value) {
                    parsed[key] = unescape(value);
                });
                return parsed[key_prefix+id];
            }
        };
    }
}

// ── HTML escaping ───────────────────────────────────────────────────────────
// Defined here so it is available to dbxAlarm.js and dbxcentral.graph.js alike.

function escHtml(s)
{
	if (s == null) return '';
	return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}

// Alarm callbacks, modal helpers and mute dialog are in dbxAlarm.js

// ── Row filter (shared by Counter Details, DBMS Config, Alarm History) ────────
//
// applyRowFilter(rows, columns, filter)
//   rows    – array of arrays  (each row is an array of cell values)
//   columns – array of column name strings, parallel to row cells
//   filter  – free-text: plain regex  OR  "where col op val [AND/OR ...]"
//
// Returns { filteredRows, filterError }

function applyRowFilter(rows, columns, filter)
{
	filter = (filter || '').trim();
	if (filter === '') return { filteredRows: rows || [], filterError: '' };

	var filteredRows = rows || [];
	var filterError  = '';

	if (filter.match(/^where\s+/i)) {
		try   { filteredRows = _whereFilter(rows, columns, filter.replace(/^where\s+/i, '')); }
		catch (e) { filterError = '(where error: ' + e.message + ')'; filteredRows = rows; }
	} else {
		try {
			var re = new RegExp(filter, 'i');
			filteredRows = (rows || []).filter(function(row) {
				return row.some(function(cell) {
					return re.test(cell === null || cell === undefined ? '' : String(cell));
				});
			});
		} catch (e) { filterError = '(invalid regex)'; filteredRows = rows; }
	}

	return { filteredRows: filteredRows, filterError: filterError };
}

function _whereFilter(rows, columns, whereExpr)
{
	var colIndex = {};
	columns.forEach(function(c, i) { colIndex[c.toLowerCase()] = i; });
	var conditions = _splitWhereConditions(whereExpr);
	return rows.filter(function(row) {
		var result = _evalWhereCondition(conditions[0].expr, colIndex, row);
		for (var i = 1; i < conditions.length; i++) {
			var right = _evalWhereCondition(conditions[i].expr, colIndex, row);
			result = (conditions[i].op === 'OR') ? result || right : result && right;
		}
		return result;
	});
}

function _splitWhereConditions(expr)
{
	var parts = [], ops = [], cur = '', inQuote = false, i = 0;
	while (i < expr.length) {
		if (!inQuote && expr[i] === "'") { inQuote = true;  cur += expr[i++]; continue; }
		if ( inQuote && expr[i] === "'") { inQuote = false; cur += expr[i++]; continue; }
		if (!inQuote) {
			var rest = expr.substring(i);
			var andM = rest.match(/^AND\b/i), orM = rest.match(/^OR\b/i);
			if (andM) { parts.push(cur.trim()); ops.push('AND'); cur = ''; i += andM[0].length; continue; }
			if (orM)  { parts.push(cur.trim()); ops.push('OR');  cur = ''; i += orM[0].length;  continue; }
		}
		cur += expr[i++];
	}
	if (cur.trim()) parts.push(cur.trim());
	return parts.map(function(p, idx) { return { expr: p, op: idx === 0 ? null : ops[idx - 1] }; });
}

function _evalWhereCondition(expr, colIndex, row)
{
	expr = expr.trim();
	var m, ci;

	m = expr.match(/^(\w+)\s+IS\s+NOT\s+NULL$/i);
	if (m) { ci = _whereColIdx(m[1], colIndex); return row[ci] !== null && row[ci] !== undefined && row[ci] !== ''; }

	m = expr.match(/^(\w+)\s+IS\s+NULL$/i);
	if (m) { ci = _whereColIdx(m[1], colIndex); return row[ci] === null || row[ci] === undefined || row[ci] === ''; }

	m = expr.match(/^(\w+)\s+LIKE\s+'([^']*)'$/i);
	if (m) {
		ci = _whereColIdx(m[1], colIndex);
		var pat = '^' + m[2].replace(/[.*+?^${}()|[\]\\]/g, '\\$&').replace(/%/g, '.*').replace(/_/g, '.') + '$';
		return new RegExp(pat, 'i').test(row[ci] === null ? '' : String(row[ci]));
	}

	m = expr.match(/^(\w+)\s*(=|!=|<>|>=|<=|>|<)\s*(.+)$/i);
	if (m) {
		ci = _whereColIdx(m[1], colIndex);
		var op = m[2], valRaw = m[3].trim(), cell = row[ci], val;
		if      (valRaw.match(/^'[^']*'$/))             { val = valRaw.slice(1, -1); cell = cell === null || cell === undefined ? '' : String(cell); }
		else if (valRaw.toLowerCase() === 'null')        { val = null; }
		else if (!isNaN(Number(valRaw)))                 { val = Number(valRaw); cell = Number(cell); }
		else                                             { val = valRaw; cell = cell === null || cell === undefined ? '' : String(cell); }
		switch (op) {
			case '=':  return cell == val;
			case '!=': case '<>': return cell != val;
			case '>':  return cell >  val;
			case '>=': return cell >= val;
			case '<':  return cell <  val;
			case '<=': return cell <= val;
		}
	}
	throw new Error('Cannot parse: ' + expr);
}

function _whereColIdx(name, colIndex)
{
	var idx = colIndex[name.toLowerCase()];
	if (idx === undefined) throw new Error('Unknown column: ' + name);
	return idx;
}

// ── Column-name completion  (Ctrl+Space in filter inputs) ─────────────────────
//
// Usage:
//   dbxFilterKeydown(event, function() { return myColumns; })
//
// The second argument is a zero-arg function that returns the current column
// names array.  Each filter input calls a thin wrapper that supplies its own
// column source:
//   onkeydown="cmDetailFilterKeydown(event);"
//   onkeydown="dbmsConfigFilterKeydown(event);"
//   onkeydown="alarmFilterKeydown(event);"
//   onkeydown="dbmsAsciiFilterKeydown(event, 'tableId');"

var _colComp    = null;   // { inputEl, $drop, matches, partial }
var _colCompIdx = -1;

function dbxFilterKeydown(e, getColumnsFn)
{
	if (e.ctrlKey && e.key === ' ') {
		e.preventDefault();
		_showColCompletion(e.target, getColumnsFn());
		return;
	}
	_colCompKeydown(e);
}

function _showColCompletion(inputEl, columns)
{
	if (!columns || !columns.length) return;

	// Word before cursor — includes * and % as wildcard chars
	var pos     = inputEl.selectionStart;
	var partial = (inputEl.value.substring(0, pos).match(/([\w*%]*)$/) || ['',''])[1];

	// Match: wildcards → regex; otherwise substring (contains)
	var matches;
	if (!partial) {
		matches = columns.slice();
	} else if (/[*%]/.test(partial)) {
		var reStr = partial.replace(/[.+?^${}()|[\]\\]/g, '\\$&').replace(/[*%]/g, '.*');
		var wcRe  = new RegExp(reStr, 'i');
		matches = columns.filter(function(h) { return wcRe.test(h); });
	} else {
		var lc = partial.toLowerCase();
		matches = columns.filter(function(h) { return h.toLowerCase().indexOf(lc) >= 0; });
	}
	if (!matches.length) { _hideColCompletion(); return; }

	_hideColCompletion();
	_colCompIdx = -1;

	var rect  = inputEl.getBoundingClientRect();
	var $drop = $('<div>').css({
		position: 'fixed', left: rect.left + 'px', top: (rect.bottom + 1) + 'px',
		zIndex: 9999, background: '#fff', border: '1px solid #adb5bd',
		borderRadius: '3px', boxShadow: '0 2px 8px rgba(0,0,0,0.15)',
		maxHeight: '200px', overflowY: 'auto',
		minWidth: Math.max(160, rect.width) + 'px',
		fontSize: '0.82em', fontFamily: 'monospace'
	});

	matches.forEach(function(col, idx) {
		$('<div>').text(col).css({ padding: '2px 8px', cursor: 'pointer', whiteSpace: 'nowrap' })
			.on('mouseover', function() { $drop.children().css('background',''); $(this).css('background','#e8f0fe'); _colCompIdx = idx; })
			.on('mousedown', function(e) {
				e.preventDefault();
				_insertColCompletion(inputEl, col, partial.length);
				_hideColCompletion();
			})
			.appendTo($drop);
	});

	$('body').append($drop);
	_colComp = { inputEl: inputEl, $drop: $drop, matches: matches, partial: partial };
	$(document).one('mousedown.colcomp', function(e) {
		if (!$(e.target).closest($drop).length) _hideColCompletion();
	});
}

function _colCompKeydown(e)
{
	if (!_colComp) return;
	var items = _colComp.$drop.children();
	var n     = items.length;

	if      (e.key === 'ArrowDown') { e.preventDefault(); _colCompIdx = (_colCompIdx + 1) % n; }
	else if (e.key === 'ArrowUp'  ) { e.preventDefault(); _colCompIdx = (_colCompIdx - 1 + n) % n; }
	else if (e.key === 'Escape'   ) { e.preventDefault(); _hideColCompletion(); return; }
	else if (e.key === 'Enter' || e.key === 'Tab') {
		if (_colCompIdx >= 0) {
			e.preventDefault();
			_insertColCompletion(_colComp.inputEl, _colComp.matches[_colCompIdx], _colComp.partial.length);
			_hideColCompletion();
		} else { _hideColCompletion(); }
		return;
	} else { return; }

	items.css('background','');
	items.eq(_colCompIdx).css('background','#e8f0fe')[0].scrollIntoView({ block: 'nearest' });
}

function _hideColCompletion()
{
	if (_colComp) { _colComp.$drop.remove(); _colComp = null; }
	_colCompIdx = -1;
	$(document).off('mousedown.colcomp');
}

function _insertColCompletion(inputEl, col, partialLen)
{
	var pos = inputEl.selectionStart, val = inputEl.value;
	inputEl.value = val.substring(0, pos - partialLen) + col + val.substring(pos);
	var np = pos - partialLen + col.length;
	inputEl.setSelectionRange(np, np);
	$(inputEl).trigger('input');
}
