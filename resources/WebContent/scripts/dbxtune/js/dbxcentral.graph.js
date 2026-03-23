/* jshint esversion: 6 */

/* **********************************************************************
** ** BEGIN INCLUDE: dbxcentral.utils.js
** **********************************************************************
** ** Since javascript do not allow "include", I just copied my "utils" script file in here
** ** DO NOT CHANGE BELOW: instead edit in 'dbxcentral.utils.js' and then copy the content here
** **********************************************************************/
/**
 * Utility function: Get a specififc parameter from the window URL
 * @param {*} key 
 * @param {*} defaultValue 
 */
function getParameter(key, defaultValue)
{
	// console.log("window.location.href: "+window.location.href);
	// console.log("window.location: "+window.location);
	// // const inputUrl = new URL(window.location.href);
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
//	return vars.hasOwnProperty(key) ? decodeURIComponent(vars[key]) : defaultValue;
	var retValue = vars.hasOwnProperty(key) ? decodeURIComponent(vars[key]) : defaultValue;
	//console.log("getParameter(key='"+key+"',default='"+defaultValue+"') <<--- '"+retValue+"'.");
	return retValue;
}
/**
 * Utility function: Create a HTML Table using a JSON input
 * grabbed from: http://www.encodedna.com/javascript/populate-json-data-to-html-table-using-javascript.htm
 */
function jsonToTable(json, stripHtmlInCells, trCallback, tdCallback, jsonMetaDataArr) 
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

	// If it's NOT a array with MetaData Objects...
	// Create a *simple* object 
	if ( ! Array.isArray(jsonMetaDataArr) )
	{
		jsonMetaDataArr = [];
		
		for (var i = 0; i < col.length; i++) 
		{
			jsonMetaDataArr.push( {columnName:col[i]} );
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

			// Render cell value directly — HTML content is rendered as HTML,
			// plain text is rendered as plain text.  The old "Toggle: Compact or
			// Formatted Content" link has been removed; full values are always
			// available via the row-detail modal (click any row).
			tabCell.innerHTML = originTxt;
			
			// Use callback function to set TD properties
			if (typeof tdCallback === 'function')
			{
				var cellMetaData = {};
				if (Array.isArray(jsonMetaDataArr))
					cellMetaData = jsonMetaDataArr[j];

				tdCallback(tabCell, cellMetaData, originTxt, json[i]);
			}

		}
		
		// Use callback function to set TR properties
		if (typeof trCallback === 'function')
		{
			trCallback(tr, json[i]);
		}
	}
	return table;
}

/**
 * Utility function: Create a Text Table using a HTML Table
 * Produced by: Claude
 * 
 * Example usage with string:
 * const htmlString = '<table><tr><th>Name</th><th>Age</th></tr><tr><td>Alice</td><td>30</td></tr></table>';
 * console.log(htmlTableToAscii(htmlString));
 *
 * Example with DOM element:
 * const table = document.querySelector('table');
 * if (table) {
 *     console.log(htmlTableToAscii(table));
 * }
 * 
 * // Example with non-table string (returns original):
 * console.log(htmlTableToAscii('<div>Not a table</div>')); // Returns: '<div>Not a table</div>'
 * 
 */
function htmlTableToAscii(input) {
	let tableElement;
	
	// Handle string input
	if (typeof input === 'string') {
		const parser = new DOMParser();
		const doc = parser.parseFromString(input, 'text/html');
		tableElement = doc.querySelector('table');
		
		// If no table found, return original string
		if (!tableElement) {
			return input;
		}
	} else {
		tableElement = input;
	}
	
	// Get all rows from the table
	const rows = Array.from(tableElement.querySelectorAll('tr'));
	
	if (rows.length === 0) {
		return typeof input === 'string' ? input : '';
	}
	
	// Extract cell data from each row
	const data = rows.map(row => {
		const cells = Array.from(row.querySelectorAll('th, td'));
		return cells.map(cell => cell.textContent.trim());
	});
	
	// Calculate column widths
	const numCols = Math.max(...data.map(row => row.length));
	const colWidths = Array(numCols).fill(0);
	
	data.forEach(row => {
		row.forEach((cell, i) => {
			colWidths[i] = Math.max(colWidths[i], cell.length);
		});
	});
	
	// Helper function to create a separator line
	const createSeparator = () => {
		return '+' + colWidths.map(w => '-'.repeat(w + 2)).join('+') + '+';
	};
	
	// Helper function to format a row
	const formatRow = (row) => {
		const cells = row.map((cell, i) => {
			return ' ' + cell.padEnd(colWidths[i]) + ' ';
		});
		return '|' + cells.join('|') + '|';
	};
	
	// Build the ASCII table
	const separator = createSeparator();
	const result = [separator];
	
	data.forEach((row, i) => {
		result.push(formatRow(row));
		// Add separator after header (first row) and at the end
		if (i === 0 || i === data.length - 1) {
			result.push(separator);
		}
	});
	
	return result.join('\n');
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

function toggleActiveTableExtendedDescription(containerId)
{
	var root = containerId ? document.getElementById(containerId) : document;
	if (!root) root = document;
	var extDesc = root.querySelectorAll('.active-table-extDesc-origin-class,.active-table-extDesc-stripped-class');

	// Toggle all elements in the above classes (scoped to container when provided)
	for (let i=0; i<extDesc.length; i++)
	{
		extDesc[i].style.display = extDesc[i].style.display === 'none' ? 'block' : 'none';
	}
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

/* **********************************************************************
** ** END INCLUDE: dbxcentral.utils.js
** **********************************************************************/






/* **********************************************************************
** ** GLOBAL VARIABLES
** **********************************************************************/
var _serverList   = [];      // What servers does the URL reflect
var _graphMap     = [];      // Graphs that are displaied / instantiated
var _debug        = 0;       // Debug level: 0=off, 1=more, 2=evenMore, 3...
var _subscribe    = false;   // If we should subscribe to Graph data from the server
//var _colorSchema  = "white"; // Color schema, can be: "white" or "dark"
var _colorSchema  = "dark"; // Color schema, can be: "white" or "dark"
var _showSrvTimer = null;
var _lastHistoryMomentsArray = null;
var _hasActiveHistoryStatementArr = null;

// CM Detail Panel state
var _cmSrvName      = null;
var _cmTimestamp    = null;
var _cmGroup        = null;
var _cmName         = null;
var _cmType         = 'rate';
var _cmListData     = null;
var _cmCurrentData  = null;   // last fetched data result (for re-filtering without re-fetching)
var _cmFilter       = '';     // current filter string
var _cmSort         = { col: -1, stage: 0 }; // sort state for current CM: stage 0=original 1=desc 2=asc
var _cmSortMap      = {};                    // per-CM sort state, keyed by cmName — survives tab switching
var _cmDetailClickRows = null;               // {columns, tooltips, rows} — row store for click-to-detail modal
// Sticky "last viewed" — survive close so re-opening returns to the same tab
var _cmLastGroup    = null;
var _cmLastCm       = null;

// This was previously part of ChartJS 2.x -- WebContent\scripts\chartjs\samples\utils.js and it's used in function: addData(jsonData)
window.chartColors = {
	red:    'rgb(255, 99, 132)',
	orange: 'rgb(255, 159, 64)',
	yellow: 'rgb(255, 205, 86)',
	green:  'rgb(75, 192, 192)',
	blue:   'rgb(54, 162, 235)',
	purple: 'rgb(153, 102, 255)',
	grey:   'rgb(201, 203, 207)'
};


//-----------------------------------------------------------
// Observer so we can update properties when entries enters visiblity in a scroll
// add element to it using: _graphObserver.observe(document.getElementById("test"));
//-----------------------------------------------------------
const _graphObserver = new IntersectionObserver(entries => 
{
	entries.forEach(entry => 
	{
		if (entry.isIntersecting)
		{
			//console.log("Observer: entry.isIntersecting -- " + entry.target.id)
			let dbxGraph = getDbxGraphByName(entry.target.id);
			if (dbxGraph !== undefined)
			{
				dbxGraph.enterVisibility();
			}
		}
	});
});

function getDbxGraphByName(name)
{
	for (let i = 0; i < _graphMap.length; i++) 
	{
		if (_graphMap[i].getFullName() === name)
			return _graphMap[i];
	}	
	return undefined;
}


function isHistoryViewActive()
{
	return $("#dbx-history-container-div").is(":visible");
}

//-----------------------------------------------------------
// CM DETAIL PANEL
//-----------------------------------------------------------

function cmDetailLoadList(srvName, timestamp)
{
	if (!srvName || !timestamp) return;

	// Normalize timestamp to "yyyy-MM-dd HH:mm:ss" — handles ISO strings, JS Date strings, moment objects
	timestamp = moment(timestamp).format("YYYY-MM-DD HH:mm:ss");

	_cmSrvName   = srvName;
	_cmTimestamp = timestamp;

	$('#cm-detail-panel').css('display', 'flex');
	$('#cm-detail-srv').text('[' + srvName + ']');
	$('#cm-detail-ts').text('@ ' + timestamp);
	$('#cm-detail-loading').show();
	$('#cm-detail-table').html('');

	$.ajax({
		url: '/api/cc/mgt/cm/list',
		data: { srv: srvName, time: timestamp },
		dataType: 'text',
		success: function(data) {
			$('#cm-detail-loading').hide();
			try {
				var r = JSON.parse(data);
				if (r.error) { cmDetailShowMsg(r.message || r.error); return; }
				_cmListData = r;
				// Use the exact SessionSampleTime the server resolved (ms-precision match from MonSessionSampleDetailes)
				if (r.resolvedTime) { _cmTimestamp = r.resolvedTime.substring(0, 19); $('#cm-detail-ts').text('@ ' + _cmTimestamp); }
				cmDetailRenderGroups(r.groups);
			} catch(ex) { cmDetailShowMsg('Parse error: ' + ex); }
		},
		error: function(xhr) {
			$('#cm-detail-loading').hide();
			cmDetailShowMsg('HTTP ' + xhr.status + ': ' + xhr.responseText);
		}
	});
}

function cmDetailRenderGroups(groups)
{
	var $gt = $('#cm-group-tabs').empty();
	if (!groups || !groups.length) { cmDetailShowMsg('No groups'); return; }

	// Auto-select: prefer last viewed group (if still present), otherwise first non-Other group with data
	var groupNames = groups.map(function(g) { return g.groupName; });
	var selGroup;
	if (_cmLastGroup && groupNames.indexOf(_cmLastGroup) >= 0) {
		selGroup = _cmLastGroup;
	} else {
		// First pass: first group with data that isn't 'Other'
		selGroup = null;
		for (var i = 0; i < groups.length; i++) {
			if (groups[i].groupName !== 'Other' && groups[i].cms.some(function(c) { return c.hasData; })) {
				selGroup = groups[i].groupName; break;
			}
		}
		// Second pass: allow 'Other' if nothing else has data
		if (!selGroup) {
			for (var i = 0; i < groups.length; i++) {
				if (groups[i].cms.some(function(c) { return c.hasData; })) { selGroup = groups[i].groupName; break; }
			}
		}
		if (!selGroup) selGroup = groups[0].groupName;
	}

	groups.forEach(function(g) {
		var hasAny = g.cms.some(function(c) { return c.hasData; });
		var li = $('<li class="nav-item ' + (hasAny ? 'cm-tab-has-data' : 'cm-tab-no-data') + '"></li>').attr('data-group-name', g.groupName);
		var a  = $('<a class="nav-link' + (g.groupName === selGroup ? ' active' : '') + '" href="#"></a>');
		if (g.groupIcon)
			a.append(cmMakeIcon(g.groupIcon));
		a.append(document.createTextNode(g.groupName));
		a.on('click', (function(gn) { return function(e) { e.preventDefault(); cmDetailSelectGroup(gn); }; })(g.groupName));
		li.append(a);
		$gt.append(li);
	});

	_cmGroup = selGroup;
	_cmLastGroup = selGroup;
	var selGroupData = null;
	for (var i = 0; i < groups.length; i++) {
		if (groups[i].groupName === selGroup) { selGroupData = groups[i]; break; }
	}
	if (selGroupData) cmDetailRenderCms(selGroupData.cms);
}

function cmDetailRenderCms(cms)
{
	var $ct = $('#cm-name-tabs').empty();
	if (!cms || !cms.length) { cmDetailShowMsg('No CMs in this group'); return; }

	// Auto-select: prefer last viewed CM (if present in this group), otherwise first CM with data
	var cmNames = cms.map(function(c) { return c.cmName; });
	var selCm = (_cmLastCm && cmNames.indexOf(_cmLastCm) >= 0)
		? _cmLastCm
		: cms[0].cmName;
	if (selCm === cms[0].cmName && !(_cmLastCm && cmNames.indexOf(_cmLastCm) >= 0)) {
		// No last-CM match — fall back to first CM with data
		for (var i = 0; i < cms.length; i++) {
			if (cms[i].hasData) { selCm = cms[i].cmName; break; }
		}
	}

	cms.forEach(function(c) {
		var li = $('<li class="nav-item ' + (c.hasData ? 'cm-tab-has-data' : 'cm-tab-no-data') + '"></li>').attr('data-cm-name', c.cmName);
		var a  = $('<a class="nav-link' + (c.cmName === selCm ? ' active' : '') + '" href="#"></a>');
		if (c.iconFile)
			a.append(cmMakeIcon(c.iconFile));
		a.append(document.createTextNode(c.displayName));
		a.on('click', (function(cn) { return function(e) { e.preventDefault(); cmDetailSelectCm(cn); }; })(c.cmName));
		li.append(a);
		$ct.append(li);
	});

	if (_cmName && _cmName !== selCm) _cmSortMap[_cmName] = _cmSort;  // save sort for previous CM
	_cmSort  = _cmSortMap[selCm] || { col: -1, stage: 0 };            // restore sort for new CM
	_cmName  = selCm;
	_cmLastCm = selCm;
	cmDetailLoadData(_cmSrvName, selCm, _cmTimestamp, _cmType);
}

function cmDetailSelectGroup(groupName)
{
	_cmGroup = groupName;
	_cmLastGroup = groupName;
	$('#cm-group-tabs .nav-link').removeClass('active');
	$('#cm-group-tabs li[data-group-name="' + groupName + '"] .nav-link').addClass('active');
	if (!_cmListData) return;
	var g = null;
	for (var i = 0; i < _cmListData.groups.length; i++) {
		if (_cmListData.groups[i].groupName === groupName) { g = _cmListData.groups[i]; break; }
	}
	if (g) cmDetailRenderCms(g.cms);
}

function cmDetailSelectCm(cmName)
{
	if (_cmName !== cmName) {
		if (_cmName) _cmSortMap[_cmName] = _cmSort;          // save sort for the tab we're leaving
		_cmSort = _cmSortMap[cmName] || { col: -1, stage: 0 }; // restore sort for the tab we're entering
	}
	_cmName = cmName;
	_cmLastCm = cmName;
	// Toggle active class only — no need to re-render the whole tab list
	$('#cm-name-tabs .nav-link').removeClass('active');
	$('#cm-name-tabs li[data-cm-name="' + cmName + '"] .nav-link').addClass('active');
	cmDetailLoadData(_cmSrvName, cmName, _cmTimestamp, _cmType);
}

function cmDetailTypeChanged(type)
{
	_cmType = type;
	if (_cmSrvName && _cmName && _cmTimestamp)
		cmDetailLoadData(_cmSrvName, _cmName, _cmTimestamp, _cmType);
}

// Fallback icon shown when a CM icon file is missing (404). Update path when known.
var _cmIconFallback = '/api/cc/mgt/cm/icon?file=' + encodeURIComponent('images/CmNoIcon.png');

// Helper: build a CM tab icon <img> element with a 404 fallback
function cmMakeIcon(iconFile)
{
	var src = '/api/cc/mgt/cm/icon?file=' + encodeURIComponent(iconFile);
	return $('<img>').attr({ src: src, width: 16, height: 16 })
		.css({ verticalAlign: 'middle', marginRight: '3px', marginBottom: '2px' })
		.on('error', function() {
			if (this.src !== _cmIconFallback) {
				this.src = _cmIconFallback;
			} else {
				$(this).hide(); // fallback itself is also missing — hide img entirely
			}
		});
}

// Helper: find the CmSampleInfo object for cmName in the last loaded list
function cmDetailFindCmInfo(cmName)
{
	if (!_cmListData || !_cmListData.groups) return null;
	for (var gi = 0; gi < _cmListData.groups.length; gi++) {
		var cms = _cmListData.groups[gi].cms;
		for (var ci = 0; ci < cms.length; ci++) {
			if (cms[ci].cmName === cmName) return cms[ci];
		}
	}
	return null;
}

function cmDetailLoadData(srvName, cmName, timestamp, type)
{
	if (!srvName || !cmName || !timestamp) return;

	// Check collector status for this CM at this sample time
	var cmInfo = cmDetailFindCmInfo(cmName);
	if (cmInfo && cmInfo.exceptionMsg) {
		// Collector threw an exception — show warning, no point querying CmDataServlet
		$('#cm-detail-loading').hide();
		var full = cmInfo.exceptionFullText || '';
		var html = '<div class="alert alert-warning mt-2" style="font-size:0.85em;">'
			+ '<strong>&#9888; Collector problem at sample time</strong><br>'
			+ $('<span>').text(cmInfo.exceptionMsg).html();
		if (full) {
			html += '<br><details style="margin-top:4px"><summary style="cursor:pointer;">Full stack trace</summary>'
				+ '<pre style="font-size:0.8em;white-space:pre-wrap;margin-top:4px;">'
				+ $('<span>').text(full).html()
				+ '</pre></details>';
		}
		html += '</div>';
		$('#cm-detail-table').html(html);
		return;
	}
	if (cmInfo && !cmInfo.hasData) {
		// CM exists but saved no rows this sample (postponed, or nothing matched filter)
		$('#cm-detail-loading').hide();
		$('#cm-detail-table').html('<div class="alert alert-secondary mt-2" style="font-size:0.85em;">'
			+ 'No data collected for <strong>' + $('<span>').text(cmName).html() + '</strong>'
			+ ' at sample time <strong>' + $('<span>').text(timestamp).html() + '</strong>.'
			+ '</div>');
		return;
	}

	$('#cm-detail-loading').show();
	$('#cm-detail-table').html('');
	// Show spinner after 100ms (avoids flicker on fast responses)
	var busyTimer = setTimeout(function() {
		document.getElementById('dbx-history-get-data-bussy').style.visibility = 'visible';
	}, 100);
	$.ajax({
		url: '/api/cc/mgt/cm/data',
		data: { srv: srvName, cm: cmName, time: timestamp, type: type },
		dataType: 'text',
		success: function(data) {
			clearTimeout(busyTimer);
			document.getElementById('dbx-history-get-data-bussy').style.visibility = 'hidden';
			$('#cm-detail-loading').hide();
			try {
				var r = JSON.parse(data);
				if (r.error) { cmDetailShowMsg(r.message || r.error); return; }
				cmDetailRenderTable(r);
			} catch(ex) { cmDetailShowMsg('Parse error: ' + ex); }
		},
		error: function(xhr) {
			clearTimeout(busyTimer);
			document.getElementById('dbx-history-get-data-bussy').style.visibility = 'hidden';
			$('#cm-detail-loading').hide();
			cmDetailShowMsg('HTTP ' + xhr.status);
		}
	});
}

function cmDetailRenderTable(r)
{
	_cmCurrentData = r;
	$('#cm-detail-filter-bar').css('display', 'flex');
	_cmFilter = $('#cm-detail-filter-input').val() || '';
	cmDetailRenderFiltered(r, _cmFilter);
}

function cmDetailApplyFilter(filter)
{
	_cmFilter = filter;
	if (!_cmCurrentData) return;
	cmDetailRenderFiltered(_cmCurrentData, filter);
}

function cmDetailRenderFiltered(r, filter)
{
	var rows = r.rows || [];
	var result = applyRowFilter(rows, r.columns, filter);
	var filteredRows = result.filteredRows;
	var filterError  = result.filterError;
	filter = (filter || '').trim();

	// Apply column sort (stage 1=desc, stage 2=asc, stage 0=original order)
	if (_cmSort.col >= 0 && _cmSort.stage > 0) {
		var sc   = _cmSort.col;
		var desc = _cmSort.stage === 1;
		filteredRows = filteredRows.slice().sort(function(a, b) {
			var va = a[sc], vb = b[sc];
			if (va === null || va === undefined) va = '';
			if (vb === null || vb === undefined) vb = '';
			var na = Number(va), nb = Number(vb);
			if (!isNaN(na) && !isNaN(nb)) return desc ? nb - na : na - nb;
			var sa = String(va).toLowerCase(), sb = String(vb).toLowerCase();
			return desc ? (sa > sb ? -1 : sa < sb ? 1 : 0) : (sa < sb ? -1 : sa > sb ? 1 : 0);
		});
	}

	var countText = filterError
		? filterError
		: (filter ? (filteredRows.length + ' / ' + rows.length + ' rows') : (rows.length + ' rows'));
	$('#cm-detail-filter-count').text(countText);
	$('#cm-detail-row-count').text(filter ? (filteredRows.length + ' / ' + rows.length + ' rows') : (rows.length + ' rows'));

	// Build diff-column name set (case-insensitive) for blue colouring
	var diffSet = {};
	if (r.diffColumns) r.diffColumns.forEach(function(dc) { diffSet[dc.toLowerCase()] = true; });

	// Per-column flags
	var isNumeric = r.isNumeric || [];
	var isDiff    = r.columns ? r.columns.map(function(c) { return !!diffSet[c.toLowerCase()]; }) : [];

	// Info bar: show cmSampleTime and cmSampleMs alongside resolved time and row count
	var sampleInfo = '';
	if (r.cmSampleTime) sampleInfo += '&nbsp; SampleTime: <b>' + escHtml(r.cmSampleTime) + '</b>';
	if (r.cmSampleMs  ) sampleInfo += '&nbsp; SampleMs: <b>'   + escHtml(String(r.cmSampleMs)) + '</b>';

	var h = '<p style="color:#6c757d;font-size:0.8em;margin:2px 0;">'
		+ 'CM: <b>' + escHtml(r.cmName) + '</b>'
		+ '&nbsp; Type: <b>' + escHtml(r.type) + '</b>'
		+ sampleInfo
		+ '&nbsp; Rows: <b>' + r.rowCount + '</b></p>'
		+ '<table class="table table-sm table-bordered table-hover" style="font-size:0.78em;white-space:nowrap;width:auto;">'
		+ '<thead class="thead-light"><tr>'
		+ r.columns.map(function(c, i) {
			var indicator = (_cmSort.col === i) ? (_cmSort.stage === 1 ? ' &#9660;' : ' &#9650;') : '';
			var tt = (r.tooltips && r.tooltips[i]) ? r.tooltips[i] : '';
			var ttAttr = '';
			if (tt) {
				// Some tooltips are wrapped in <html>...</html> -- strip the outer wrapper so
				// inner HTML (tables, bold, line-breaks etc.) renders correctly in the tooltip.
				var ttHtml = tt.replace(/^\s*<html>\s*/i, '').replace(/\s*<\/html>\s*$/i, '').trim();
				// Bootstrap tooltip with html:true renders markup instead of raw text.
				ttAttr = ' data-toggle="tooltip" data-html="true" data-placement="bottom" title="'
					+ ttHtml.replace(/"/g, '&quot;') + '"';
			}
			return '<th style="cursor:pointer;user-select:none;" onclick="cmDetailSortCol(' + i + ');"' + ttAttr + '>'
				+ escHtml(c) + indicator + '</th>';
		}).join('')
		+ '</tr></thead><tbody>';

	if (!filteredRows || filteredRows.length === 0) {
		h += '<tr><td colspan="' + r.columns.length + '" style="text-align:center;color:#6c757d;">'
			+ (filter ? 'No rows match filter' : 'No rows') + '</td></tr>';
	} else {
		// Store rows for click-to-detail (index stored on <tr data-ri>)
		_cmDetailClickRows = { columns: r.columns, tooltips: r.tooltips || [], rows: filteredRows };
		filteredRows.forEach(function(row, ri) {
			h += '<tr data-ri="' + ri + '" style="cursor:pointer;" title="Click to view full row details">'
				+ row.map(function(v, i) {
				var style = 'style="';
				if (isNumeric[i])                    style += 'text-align:right;';
				if (isDiff[i] && _cmType !== 'abs') style += 'color:#0066cc;';
				style += '"';
				var cell;
				if (v === null || v === undefined)
					cell = '<span style="color:#aaa">null</span>';
				else if (v === true || v === false)
					cell = '<input type="checkbox"' + (v ? ' checked' : '') + ' onclick="return false;" style="cursor:default;pointer-events:none;">';
				else {
					var s = String(v);
					if (s.length > 60) {
						var plain = s.replace(/<[^>]+>/g, '').trim();
						cell = '<span title="' + escHtml(plain.substring(0, 300)) + '">'
							+ escHtml(plain.substring(0, 60)) + '\u2026</span>';
					} else {
						cell = escHtml(s);
					}
				}
				return '<td ' + style + '>' + cell + '</td>';
			}).join('') + '</tr>';
		});
	}
	h += '</tbody></table>';

	// Restore scroll position from localStorage (persists across tab switches and panel close/reopen)
	// Pattern mirrors Active Statements: save on scroll event, restore after render + setTimeout fallback
	var $body      = $('#cm-detail-body');
	var scrollKey  = 'cmDetail-scroll-' + (r.cmName || 'default');
	var savedTop   = 0, savedLeft = 0;
	try {
		var saved = JSON.parse(localStorage.getItem(scrollKey) || 'null');
		if (saved) { savedTop = saved.top || 0; savedLeft = saved.left || 0; }
	} catch(e) {}

	// Destroy existing tooltip instances before replacing the DOM (prevents orphaned tooltip divs)
	$('#cm-detail-table th[data-toggle="tooltip"]').tooltip('dispose');
	$('#cm-detail-table').html(h);

	// Activate Bootstrap tooltips on the freshly rendered column headers
	$('#cm-detail-table th[data-toggle="tooltip"]').tooltip({ container: '#cm-detail-panel', boundary: 'window' });

	// Row click → detail modal (delegated so it survives re-renders)
	$('#cm-detail-table tbody').off('click', 'tr').on('click', 'tr', function() {
		var ri = parseInt($(this).attr('data-ri'), 10);
		if (isNaN(ri) || !_cmDetailClickRows) return;
		cmDetailRowShowModal(_cmDetailClickRows.columns, _cmDetailClickRows.rows[ri], _cmDetailClickRows.tooltips);
	});

	// Restore immediately (works when content height is already known)
	$body.scrollTop(savedTop).scrollLeft(savedLeft);
	// Also restore after a tick in case the browser resets scroll after DOM update
	setTimeout(function() { $body.scrollTop(savedTop).scrollLeft(savedLeft); }, 1);
}

/**
 * Show a click-to-detail modal for a Counter Details row.
 * Unpivots the column array + value array into a 2-column table rendered client-side.
 * Column tooltips (descriptions) are shown as a third column when available.
 */
/**
 * Returns true if the string value appears to be an HTML fragment —
 * i.e. it starts with '<' (after leading whitespace) and contains '>'.
 * Used by the row-detail modals to decide whether to render a value as
 * HTML or escape it as plain text.
 */
function looksLikeHtml(s) {
	var t = s.replace(/^\s+/, '');
	return t.charAt(0) === '<' && t.indexOf('>') >= 0;
}

function cmDetailRowShowModal(columns, row, tooltips)
{
	var $modal = $('#dbx-view-alarmView-dialog');
	if ($modal.length === 0) return;

	var topZ = (window._dbxTopZ || 910) + 200;
	$modal.css('z-index', topZ);
	setTimeout(function() { $('.modal-backdrop').css('z-index', topZ - 1); }, 30);
	_dbxModalApplyDark($modal);

	$('#dbx-view-alarmView-label').text('Row Detail');
	$('#dbx-view-alarmView-objectName').text('');

	var hasTooltips = tooltips && tooltips.some(function(t) { return t && t.length > 0; });
	var html = '<table class="table table-sm table-bordered" style="font-size:0.85em;word-break:break-word;">'
		+ '<thead class="thead-light"><tr><th style="white-space:nowrap">Column</th><th>Value</th>'
		+ (hasTooltips ? '<th>Description</th>' : '') + '</tr></thead><tbody>';

	columns.forEach(function(col, i) {
		var v = row[i];
		var cellHtml;
		if (v === null || v === undefined)
			cellHtml = '<span style="color:#aaa">null</span>';
		else if (v === true || v === false)
			cellHtml = v ? '<i class="fa fa-check-square-o text-success"></i>'
			             : '<i class="fa fa-square-o text-muted"></i>';
		else {
			var s = String(v);
			if (looksLikeHtml(s))
				cellHtml = '<div style="overflow:auto;">' + s + '</div>';
			else
				cellHtml = '<pre style="white-space:pre-wrap;margin:0;font-size:0.9em;">' + escHtml(s) + '</pre>';
		}

		var ttHtml = '';
		if (hasTooltips && tooltips[i]) {
			ttHtml = '<td style="font-size:0.8em;color:#555;">'
				+ tooltips[i].replace(/^\s*<html>\s*/i,'').replace(/\s*<\/html>\s*$/i,'') + '</td>';
		}

		html += '<tr><td style="white-space:nowrap;font-weight:bold;">' + escHtml(col) + '</td>'
			+ '<td>' + cellHtml + '</td>' + ttHtml + '</tr>';
	});

	html += '</tbody></table>';
	$('#dbx-view-alarmView-content').html(html);
	$modal.modal('show');
}

//-----------------------------------------------------------
// Client-side WHERE filter
// Supports:  col = val   col != val   col <> val
//            col > val   col >= val   col < val   col <= val
//            col LIKE 'pattern'  (% = .*)
/**
 * Shared row-filter utility used by CM Detail and DBMS Config panels.
 * Supports two modes:
 *   "where <expr>"  — SQL-like WHERE evaluated by cmDetailWhereFilter()
 *   anything else   — case-insensitive regex matched against each cell
 *
 * Returns { filteredRows, filterError }.  filteredRows === rows when
 * filter is empty or an error occurs (callers never receive undefined).
 */
function applyRowFilter(rows, columns, filter)
{
	filter = (filter || '').trim();
	if (filter === '') return { filteredRows: rows || [], filterError: '' };

	var filteredRows = rows || [];
	var filterError  = '';

	if (filter.match(/^where\s+/i)) {
		try   { filteredRows = cmDetailWhereFilter(rows, columns, filter.replace(/^where\s+/i, '')); }
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

//            col IS NULL   col IS NOT NULL
// Values:    'quoted string'   number   null
// Joining:   AND / OR  (left-to-right evaluation)
//-----------------------------------------------------------
function cmDetailWhereFilter(rows, columns, whereExpr)
{
	// Build column-name → index map (case-insensitive)
	var colIndex = {};
	columns.forEach(function(c, i) { colIndex[c.toLowerCase()] = i; });

	// Tokenise respecting single-quoted strings, then split on AND/OR
	var conditions = cmDetailSplitConditions(whereExpr);

	return rows.filter(function(row) {
		var result = cmDetailEvalCondition(conditions[0].expr, colIndex, row);
		for (var i = 1; i < conditions.length; i++) {
			var right = cmDetailEvalCondition(conditions[i].expr, colIndex, row);
			if (conditions[i].op === 'OR')
				result = result || right;
			else
				result = result && right;
		}
		return result;
	});
}

function cmDetailSplitConditions(expr)
{
	// Split on AND/OR that are NOT inside single quotes
	var parts  = [];
	var ops    = [];
	var cur    = '';
	var inQuote = false;
	var i = 0;
	while (i < expr.length)
	{
		if (!inQuote && expr[i] === "'") { inQuote = true;  cur += expr[i++]; continue; }
		if ( inQuote && expr[i] === "'") { inQuote = false; cur += expr[i++]; continue; }
		if (!inQuote)
		{
			var rest = expr.substring(i);
			var andM = rest.match(/^AND\b/i);
			var orM  = rest.match(/^OR\b/i);
			if (andM) { parts.push(cur.trim()); ops.push('AND'); cur = ''; i += andM[0].length; continue; }
			if (orM)  { parts.push(cur.trim()); ops.push('OR');  cur = ''; i += orM[0].length;  continue; }
		}
		cur += expr[i++];
	}
	if (cur.trim()) parts.push(cur.trim());

	return parts.map(function(p, idx) { return { expr: p, op: idx === 0 ? null : ops[idx - 1] }; });
}

function cmDetailEvalCondition(expr, colIndex, row)
{
	expr = expr.trim();

	// IS NOT NULL
	var m = expr.match(/^(\w+)\s+IS\s+NOT\s+NULL$/i);
	if (m) {
		var ci = cmDetailColIdx(m[1], colIndex);
		return row[ci] !== null && row[ci] !== undefined && row[ci] !== '';
	}
	// IS NULL
	m = expr.match(/^(\w+)\s+IS\s+NULL$/i);
	if (m) {
		var ci = cmDetailColIdx(m[1], colIndex);
		return row[ci] === null || row[ci] === undefined || row[ci] === '';
	}
	// LIKE 'pattern'
	m = expr.match(/^(\w+)\s+LIKE\s+'([^']*)'$/i);
	if (m) {
		var ci = cmDetailColIdx(m[1], colIndex);
		var pat = '^' + m[2].replace(/[.*+?^${}()|[\]\\]/g, '\\$&').replace(/%/g, '.*').replace(/_/g, '.') + '$';
		return new RegExp(pat, 'i').test(row[ci] === null ? '' : String(row[ci]));
	}
	// Comparison: col op val
	m = expr.match(/^(\w+)\s*(=|!=|<>|>=|<=|>|<)\s*(.+)$/i);
	if (m) {
		var ci     = cmDetailColIdx(m[1], colIndex);
		var op     = m[2];
		var valRaw = m[3].trim();
		var cell   = row[ci];
		var val;

		if (valRaw.match(/^'[^']*'$/)) {            // quoted string
			val  = valRaw.slice(1, -1);
			cell = cell === null || cell === undefined ? '' : String(cell);
		} else if (valRaw.toLowerCase() === 'null') { // null literal
			val  = null;
		} else if (!isNaN(Number(valRaw))) {           // number
			val  = Number(valRaw);
			cell = Number(cell);
		} else {                                        // bare word → string
			val  = valRaw;
			cell = cell === null || cell === undefined ? '' : String(cell);
		}

		switch (op) {
			case '=':            return cell == val;
			case '!=': case '<>':return cell != val;
			case '>':            return cell >  val;
			case '>=':           return cell >= val;
			case '<':            return cell <  val;
			case '<=':           return cell <= val;
		}
	}
	throw new Error('Cannot parse: ' + expr);
}

function cmDetailColIdx(name, colIndex)
{
	var idx = colIndex[name.toLowerCase()];
	if (idx === undefined) throw new Error('Unknown column: ' + name);
	return idx;
}

function cmDetailShowMsg(msg)
{
	$('#cm-detail-table').html('<div style="padding:15px;text-align:center;color:#6c757d;">' + escHtml(String(msg)) + '</div>');
}

function cmDetailToggle()
{
	var $panel = $('#cm-detail-panel');
	if ($panel.is(':visible')) {
		cmDetailClose();
		return;
	}
	// Restore dark-mode preference from localStorage; default to page colour scheme on first visit
	var savedDarkCm = null;
	try { savedDarkCm = localStorage.getItem('cmDetailDark'); } catch(e) {}
	var dark = (savedDarkCm !== null) ? (savedDarkCm === '1') : (_colorSchema === 'dark');
	$('#cm-detail-dark').prop('checked', dark);
	if (dark) $panel.addClass('cm-dark'); else $panel.removeClass('cm-dark');

	// Attach scroll listener once — saves position to localStorage per CM (mirrors Active Statements)
	var $body = $('#cm-detail-body');
	if (!$body.data('cmScrollListenerAttached')) {
		$body.data('cmScrollListenerAttached', true);
		var _cmScrollTimer = null;
		$body.on('scroll', function() {
			clearTimeout(_cmScrollTimer);
			_cmScrollTimer = setTimeout(function() {
				if (_cmName) {
					try {
						localStorage.setItem('cmDetail-scroll-' + _cmName,
							JSON.stringify({ top: $body.scrollTop(), left: $body.scrollLeft() }));
					} catch(e) {}
				}
			}, 500);
		});
	}

	$panel.css('display', 'flex');
	// Auto-load on first open (both live and history mode)
	if (!_cmSrvName && _serverList && _serverList.length > 0) {
		var _autoTs;
		if (isHistoryViewActive()) {
			// History mode: use last-known timestamp or read from the slider display text
			_autoTs = _cmTimestamp;
			if (!_autoTs) {
				var _sliderTxt = ($('#dbx-history-slider-center-text').text() || '').trim();
				_autoTs = _sliderTxt.split(' - ')[0].trim();
			}
		} else {
			_autoTs = moment().format('YYYY-MM-DD HH:mm:ss');
		}
		if (_autoTs && _autoTs.match(/^\d{4}-\d{2}-\d{2}/))
			cmDetailLoadList(_serverList[0], _autoTs);
	}
}

// Toggle dark mode on the CM detail panel and persist the preference
function cmDetailDarkToggle(on)
{
	var $panel = $('#cm-detail-panel');
	if (on) $panel.addClass('cm-dark'); else $panel.removeClass('cm-dark');
	try { localStorage.setItem('cmDetailDark', on ? '1' : '0'); } catch(e) {}
}

// Called from addData() on every live sample — refresh current CM if panel is open and not paused
function cmDetailLiveRefresh(srvName)
{
	if (!$('#cm-detail-panel').is(':visible')) return;
	if ($('#cm-detail-paused').prop('checked')) return;
	var srv = _cmSrvName || srvName;
	if (!srv) return;
	var ts = moment().format('YYYY-MM-DD HH:mm:ss');
	_cmTimestamp = ts;
	$('#cm-detail-ts').text('@ ' + ts);

	if (!_cmName)
	{
		// No CM selected yet — do a full load (builds tabs, selects first CM with data)
		cmDetailLoadList(srv, ts);
		return;
	}

	// Re-query MonSessionSampleDetailes to update tab colours (same as native GUI on every sample),
	// then refresh data for the currently selected CM — without rebuilding the tab DOM.
	$.ajax({
		url: '/api/cc/mgt/cm/list',
		data: { srv: srv, time: ts },
		dataType: 'text',
		success: function(data) {
			try {
				var r = JSON.parse(data);
				if (!r.error) {
					_cmListData = r;
					cmDetailUpdateTabColors(r.groups);
					// Use exact SessionSampleTime from MonSessionSampleDetailes
					if (r.resolvedTime) { _cmTimestamp = r.resolvedTime.substring(0, 19); $('#cm-detail-ts').text('@ ' + _cmTimestamp); }
				}
			} catch(ex) {}
			cmDetailLoadData(srv, _cmName, _cmTimestamp, _cmType);
		},
		error: function() {
			// Still refresh CM data even if list call failed
			cmDetailLoadData(srv, _cmName, ts, _cmType);
		}
	});
}

// Update only the has-data/no-data classes on existing tab <li> elements.
// Called on live refresh so tab colours stay current without rebuilding the DOM.
function cmDetailUpdateTabColors(groups)
{
	if (!groups) return;
	groups.forEach(function(g) {
		var hasAny = g.cms.some(function(c) { return c.hasData; });
		$('#cm-group-tabs li[data-group-name]').each(function() {
			if ($(this).attr('data-group-name') === g.groupName) {
				$(this).toggleClass('cm-tab-has-data', hasAny).toggleClass('cm-tab-no-data', !hasAny);
			}
		});
		g.cms.forEach(function(c) {
			$('#cm-name-tabs li[data-cm-name]').each(function() {
				if ($(this).attr('data-cm-name') === c.cmName) {
					$(this).toggleClass('cm-tab-has-data', c.hasData).toggleClass('cm-tab-no-data', !c.hasData);
				}
			});
		});
	});
}

// Called when the history slider moves — refresh CM data for the selected time
function cmDetailSliderRefresh(startTime)
{
	if (!$('#cm-detail-panel').is(':visible')) return;
	var srv = _cmSrvName || (_serverList && _serverList.length > 0 ? _serverList[0] : null);
	if (!srv || !startTime) return;
	cmDetailLoadList(srv, startTime);
}

// Column sort: 3-stage cycle — desc → asc → original
function cmDetailSortCol(colIdx)
{
	// Force-hide any stuck Bootstrap tooltip before re-rendering the table
	$('[data-toggle="tooltip"]').tooltip('hide');

	if (_cmSort.col === colIdx) {
		_cmSort.stage = (_cmSort.stage === 1) ? 2 : (_cmSort.stage === 2) ? 0 : 1;
		if (_cmSort.stage === 0) _cmSort.col = -1;
	} else {
		_cmSort.col   = colIdx;
		_cmSort.stage = 1;   // start with desc (highest first)
	}
	cmDetailApplyFilter(_cmFilter);
}

function cmDetailClose()
{
	$('#cm-detail-panel').hide();
	_cmSrvName = _cmTimestamp = _cmGroup = _cmName = _cmListData = _cmCurrentData = null;
	_cmFilter = '';
	_cmSort    = { col: -1, stage: 0 };
	_cmSortMap = {};
	$('#cm-group-tabs,#cm-name-tabs').empty();
	$('#cm-detail-filter-bar').hide();
	$('#cm-detail-filter-input').val('');
	$('#cm-detail-filter-count').text('');
	$('#cm-detail-row-count').text('');
	$('#cm-detail-table').html('');
}

/**
 * Render a single table cell value.
 * - URLs (http/https) become clickable links opening in a new tab.
 * - Native booleans and the strings "true"/"false" become disabled checkboxes.
 * - Everything else is HTML-escaped text.
 */
function renderCell(val)
{
	if (val === null || val === undefined) return '<span style="color:#aaa;">null</span>';
	if (val === true  || String(val).toLowerCase() === 'true')
		return '<input type="checkbox" checked onclick="return false;" style="cursor:default;pointer-events:none;">';
	if (val === false || String(val).toLowerCase() === 'false')
		return '<input type="checkbox" onclick="return false;" style="cursor:default;pointer-events:none;">';
	var s = String(val);
	if (s.match(/^https?:\/\//i))
		return '<a href="' + escHtml(s) + '" target="_blank" rel="noopener noreferrer">' + escHtml(s) + '</a>';
	return escHtml(s);
}

// escHtml() defined in dbxcentral.utils.js

//-----------------------------------------------------------
// ACTIVE STATEMENTS
//-----------------------------------------------------------
function openActiveStatementsWindow()
{
	console.log('openActiveStatementsWindow()');

	$("#active-statements").toggle();
}
function activeStatementsExecTimeClick(textField)
{
	if (textField === null)
		return;

	var val = textField.value;
	console.log('activeStatementsExecTimeClick(): Active Statement MaxExecTimeInMs[text]: ' + val);
	
//	// Save last known value in "WebBrowser storage"
//	getStorage('dbxtune_checkboxes_').set("active-statements-execTime-txt", val);
//	
//	// get Counters...
//	dbxTuneCheckActiveStatements();
}
function activeStatementsRadioClick(radioBut) 
{
	if (radioBut === null)
		return;

	var rVal = typeof(radioBut) == "string" ? radioBut : radioBut.value;
	console.log('activeStatementsRadioClick(): Active Statement Window Size[RadioBut]: ' + rVal);
	
	if (rVal === "hide")
	{
		// Hide all Statements
		$("#active-statements-win").css("display", "none");
	}
	else
	{
		// Set The OUTER max-size
		$("#active-statements").css("max-height", rVal);

		// SHOW all Statements
		$("#active-statements-win").css("display", "block");
	}

	// Save last known value in "WebBrowser storage"
	getStorage('dbxtune_checkboxes_').set("active-statements-show-radio", rVal);
}
function activeStatementsCounterTypeClick(radioBut)
{
	if (radioBut === null)
		return;

	var rVal = typeof(radioBut) == "string" ? radioBut : radioBut.value;
	console.log('activeStatementsCounterTypeClick(): Active Statement CounterType[RadioBut]: ' + rVal);
	
	// Save last known value in "WebBrowser storage"
	getStorage('dbxtune_checkboxes_').set("active-statements-counter-type", rVal);
	
	// get Counters...
	dbxTuneCheckActiveStatements();
}
function activeStatementsPausedChkClick(checkbox) 
{
	if (checkbox === null)
		return;

	console.log('activeStatementsPausedChkClick(): Checked: ' + checkbox.checked);

	// Save last known value in "WebBrowser storage"
	getStorage('dbxtune_checkboxes_').set("active-statements-paused-chk", checkbox.checked ? 'checked' : 'not');

	if ( ! document.getElementById("active-statements-paused-chk").checked )
	{
		// Reset Last Active to ensure refresh
		_lastActiveStatementData = "";

		// When we UN-Pause we need to check for saved/current Active Statements
		dbxTuneCheckActiveStatements(); 
	}
	else
	{
		// This will just set the WATERMARK to PAUSED
		setActiveStatement();
	}
}
function activeStatementsAutoOpenChkClick(checkbox) 
{
	if (checkbox === null)
		return;

	console.log('activeStatementsAutoOpenChkClick(): Checked: ' + checkbox.checked);

	// Save last known value in "WebBrowser storage"
	getStorage('dbxtune_checkboxes_').set("active-statements-auto-open-chk", checkbox.checked ? 'checked' : 'not');
}
function activeStatementsSolidChkClick(checkbox) 
{
	if (checkbox === null)
		return;

	console.log('activeStatementsSolidChkClick(): Checked: ' + checkbox.checked);
	if (checkbox.checked)
	{
		$("#active-statements").css("background-color", 'rgba(229, 228, 226, 1.0)');
	}
	else
	{
		$("#active-statements").css("background-color", 'rgba(229, 228, 226, 0.7)');
	}

	// Save last known value in "WebBrowser storage"
	getStorage('dbxtune_checkboxes_').set("active-statements-solid-chk", checkbox.checked ? 'checked' : 'not');
}
function activeStatementsCompExtDescClick(checkbox)
{
	if (checkbox === null)
		return;

	console.log('activeStatementsCompExtDescClick(): Checked: ' + checkbox.checked);

	// Save last known value in "WebBrowser storage"
	getStorage('dbxtune_checkboxes_').set("active-statements-compExtDesc-chk", checkbox.checked ? 'checked' : 'not');

	// Toggle only within the active-statements panel (not alarm panel)
	toggleActiveTableExtendedDescription('active-statements-win');
}



/**
 * Send a "refresh" request to all collectors
 */
function collectorRequestRefresh()
{
	console.log('collectorRequestRefresh(): CALLED');

//	const srvName = _serverList[0]; // NOTE: This needs to be improved... check for many etc...
	const srvName = _serverList.join(','); // to CSV

	$.ajax(
	{
		url: "/api/collector-refresh?srv="+srvName,
		type: 'get',
		//async: false,   // to call it one by one (async: true = spawn away a bunch of them in the background)
			
		success: function(data, status) 
		{
			var jsonResp = JSON.parse(data);
			console.log("RECEIVED DATA[collectorRequestRefresh]: ", jsonResp);
		},
		error: function(xhr, desc, err) 
		{
			console.log(xhr);
			console.log("Details: " + desc + "\nError: " + err);
		}
	}); // end: ajax call
} // end: function


// do: deferred (since all DOM elements might not be created yet)
setTimeout(function()
{
	// Restore MaxExecTimeInMs
//	var savedVal_activeStatementsExecTime = getStorage('dbxtune_checkboxes_').get("active-statements-execTime-txt");
//	if (savedVal_activeStatementsExecTime === null || savedVal_activeStatementsExecTime === '')
//		savedVal_activeStatementsExecTime = 1000;
//	document.getElementById("active-statements-execTime-txt").value = savedVal_activeStatementsExecTime;
//	console.log("Restored 'active-statements-execTime-txt', value="+savedVal_activeStatementsExecTime);
	
	// Restore 'Paused' at the ACTIVE STATEMENTS "window"
//	var savedVal_activeStatementsPausedChk = getStorage('dbxtune_checkboxes_').get("active-statements-paused-chk");
//	if (savedVal_activeStatementsPausedChk == 'checked') $("#active-statements-paused-chk").attr('checked', 'checked');
//	if (savedVal_activeStatementsPausedChk == 'not')     $("#active-statements-paused-chk").removeAttr('checked');
	//activeStatementsPausedClick( document.getElementById("active-statements-paused-chk") );

	// Restore 'Auto Open' at the ACTIVE STATEMENTS "window"
	var savedVal_activeStatementsAutoOpenChk = getStorage('dbxtune_checkboxes_').get("active-statements-auto-open-chk");
	if (savedVal_activeStatementsAutoOpenChk == 'checked') $("#active-statements-auto-open-chk").attr('checked', 'checked');
	if (savedVal_activeStatementsAutoOpenChk == 'not')     $("#active-statements-auto-open-chk").removeAttr('checked');

	// Restore 'Auto Open' for the Alarm Panel
	var savedVal_alarmAutoOpenChk = getStorage('dbxtune_checkboxes_').get(_alarmPageKey('alarm-auto-open-chk'));
	if (savedVal_alarmAutoOpenChk == 'checked') $("#alarm-auto-open-chk").attr('checked', 'checked');
	if (savedVal_alarmAutoOpenChk == 'not')     $("#alarm-auto-open-chk").removeAttr('checked');
	//activeStatementsAutoOpenChkClick( document.getElementById("active-statements-auto-open-chk") );

	// Restore 'Solid Background' at the ACTIVE STATEMENTS "window"
	var savedVal_activeStatementsSolidChk = getStorage('dbxtune_checkboxes_').get("active-statements-solid-chk");
	if (savedVal_activeStatementsSolidChk == 'checked') $("#active-statements-solid-chk").attr('checked', 'checked');
	if (savedVal_activeStatementsSolidChk == 'not')     $("#active-statements-solid-chk").removeAttr('checked');
	activeStatementsSolidChkClick( document.getElementById("active-statements-solid-chk") );

	// Restore 'Window Size' at the ACTIVE STATEMENTS "window"
	var savedVal_activeStatementsShowRadio = getStorage('dbxtune_checkboxes_').get("active-statements-show-radio");
	console.log("RESTORE ALARM Window size: savedVal_activeStatementsShowRadio="+savedVal_activeStatementsShowRadio);
	activeStatementsRadioClick( savedVal_activeStatementsShowRadio );
	radionButtonGroupSetSelectedValue("active-statements-show-radio", savedVal_activeStatementsShowRadio);
	
	// Restore 'CounterType'
	var savedVal_activeStatementsCounterType = getStorage('dbxtune_checkboxes_').get("active-statements-counter-type");
	console.log("RESTORE ALARM Window size: savedVal_activeStatementsCounterType="+savedVal_activeStatementsCounterType);
	activeStatementsCounterTypeClick( savedVal_activeStatementsCounterType );
	radionButtonGroupSetSelectedValue("active-statements-counter-type", savedVal_activeStatementsCounterType);
}, 10);


/**
 * Get Active Statements saved in the Central PCS (last known/received values)
 */
// Remember last active statements (if no changes, then we can skip the update)
var _lastActiveStatementData = "";
function dbxTuneCheckActiveStatements()
{
	console.log("dbxTuneCheckActiveStatements()");

	if ( ! _subscribe )
	{
		$("#subscribe-feedback-msg").html("<s>subscribe</s>");
		return;
	}

//	const srvName = _serverList[0]; // NOTE: This needs to be improved... check for many etc...
	const srvName = _serverList.join(','); // to CSV
	
	if (_serverList.length > 1)
	{
		console.log("dbxTuneCheckActiveStatements(): Found " + _serverList.length + " entries in the serverlist. ONLY FIRST WILL BE CHECKED. _serverList=" + _serverList);
	}

	$.ajax(
	{
		url: "/api/last-sample?srv="+srvName+"&cm=CmActiveStatements",
		type: 'get',
		//async: false,   // to call it one by one (async: true = spawn away a bunch of them in the background)
			
		success: function(data, status) 
		{
			console.log("DEBUG: dbxTuneCheckActiveStatements(): CALL SUCCESS...");
		//	console.log("DEBUG: dbxTuneCheckActiveStatements(): CALL SUCCESS... data & _lastActiveStatementData", data, _lastActiveStatementData);
			if (data === _lastActiveStatementData)
			{
				console.log("DEBUG: dbxTuneCheckActiveStatements(): NO DATA CHANGE for STATEMENTS...");

				// This will just clear the Watermark if we are in PAUSED mode
				setActiveStatement();

				return;
			}
			_lastActiveStatementData = data;
			
			var jsonResp = JSON.parse(data);
			console.log("RECEIVED DATA[dbxTuneCheckActiveStatements]: ", jsonResp);

			setActiveStatement(jsonResp); 
		},
		error: function(xhr, desc, err) 
		{
			console.log(xhr);
			console.log("Details: " + desc + "\nError: " + err);
		}
	}); // end: ajax call
} // end: function

//function dbxTuneGetHistoryStatements(sampleTime)
function dbxTuneGetHistoryStatements(startTime, endTime)
{
	console.log("dbxTuneGetHistoryStatements(startTime=|" + startTime + "|, endTime=|" + endTime + "|.");

//	if ( ! _subscribe )
//	{
//		$("#subscribe-feedback-msg").html("<s>subscribe</s>");
//		return;
//	}

//	const srvName = _serverList[0]; // NOTE: This needs to be improved... check for many etc...
	const srvName = _serverList.join(','); // to CSV
	
	if (_serverList.length > 1)
	{
		console.log("dbxTuneGetHistoryStatements(): Found " + _serverList.length + " entries in the serverlist. ONLY FIRST WILL BE CHECKED. _serverList=" + _serverList);
	}

	const urlStr = "/api/history-sample"
	             + "?srv="        + srvName
	             + "&cm="         + "CmActiveStatements"
//	             + "&sampleTime=" + sampleTime
	             + "&startTime="  + startTime
	             + "&endTime="    + endTime
	             + "";
	console.log("dbxTuneGetHistoryStatements(): Calling url '" + urlStr + "'.");
	
	// Show info that we are getting data (this might be slow), only disaply after x ms
	// And in the below SUCCESS/FAIL -- CLOSE the info field/popup
//	document.getElementById("dbx-history-get-data-bussy").style.visibility = 'visible';
	var dbxHistoryGetDataBussyTime = setTimeout(function() { document.getElementById("dbx-history-get-data-bussy").style.visibility = 'visible'; }, 20);

	// GET Data
	$.ajax(
	{
		url: urlStr,
		type: 'get',
		//async: false,   // to call it one by one (async: true = spawn away a bunch of them in the background)
			
		success: function(data, status) 
		{
			console.log("DEBUG: dbxTuneGetHistoryStatements(): CALL SUCCESS...");
//			console.log("DEBUG: dbxTuneGetHistoryStatements(): DATA: " + data, data);
//			console.log("DEBUG: dbxTuneGetHistoryStatements(): DATA: ", data);
		//	console.log("DEBUG: dbxTuneGetHistoryStatements(): CALL SUCCESS... data & _lastActiveStatementData", data, _lastActiveStatementData);
//			if (data === _lastActiveStatementData)
//			{
//				console.log("DEBUG: dbxTuneGetHistoryStatements(): NO DATA CHANGE for STATEMENTS...");
//
//				// This will just clear the Watermark if we are in PAUSED mode
//				//setActiveStatement();
//
//				return;
//			}
			_lastActiveStatementData = data;
			
			var jsonResp = JSON.parse(data);
			console.log("RECEIVED DATA[dbxTuneGetHistoryStatements]: ", jsonResp);

			setHistoryStatement(jsonResp); 

			// CLOSE the info field (that we are getting data from the DB)
			clearTimeout(dbxHistoryGetDataBussyTime);
			document.getElementById("dbx-history-get-data-bussy").style.visibility = 'hidden';
		},
		error: function(xhr, desc, err) 
		{
			clearTimeout(dbxHistoryGetDataBussyTime);
			document.getElementById("dbx-history-get-data-bussy").style.visibility = 'hidden';

			console.log(xhr);
			console.log("Details: " + desc + "\nError: " + err);
		}
	}); // end: ajax call
} // end: function

/**
 * This should return active Statements (or CM's)
 * This so we can draw a "marker" section below the history-timeline-slider to indicate where we have ACTIVITY
 */
function dbxTuneGetActiveHistoryStatements(startTime, endTime)
{
	console.log("dbxTuneGetActiveHistoryStatements(startTime=|" + startTime + "|, endTime=|" + endTime + "|.");

//	if ( ! _subscribe )
//	{
//		$("#subscribe-feedback-msg").html("<s>subscribe</s>");
//		return;
//	}

//	const srvName = _serverList[0]; // NOTE: This needs to be improved... check for many etc...
	const srvName = _serverList.join(','); // to CSV
	
	if (_serverList.length > 1)
	{
		console.log("dbxTuneGetActiveHistoryStatements(): Found " + _serverList.length + " entries in the serverlist. ONLY FIRST WILL BE CHECKED. _serverList=" + _serverList);
	}

	const urlStr = "/api/history-active-samples"
		+ "?srv="       + srvName 
		+ "&cm="        + "CmActiveStatements" 
		+ "&startTime=" + startTime
		+ "&endTime="   + endTime;
	console.log("dbxTuneGetActiveHistoryStatements(): Calling url '" + urlStr + "'.");
	
	var returnVal = null;
	$.ajax(
	{
		url: urlStr,
		type: 'get',
		async: false,   // to call it one by one (async: true = spawn away a bunch of them in the background)
			
		success: function(data, status) 
		{
			console.log("DEBUG: dbxTuneGetActiveHistoryStatements(): CALL SUCCESS...");
//			console.log("DEBUG: dbxTuneGetActiveHistoryStatements(): DATA: " + data, data);
		//	console.log("DEBUG: dbxTuneGetActiveHistoryStatements(): CALL SUCCESS... data & _lastActiveStatementData", data, _lastActiveStatementData);
//			if (data === _lastActiveStatementData)
//			{
//				console.log("DEBUG: dbxTuneGetActiveHistoryStatements(): NO DATA CHANGE for STATEMENTS...");
//
//				// This will just clear the Watermark if we are in PAUSED mode
//				//setActiveStatement();
//
//				return;
//			}
//			_lastActiveStatementData = data;
			
			var jsonResp = JSON.parse(data);
			console.log("RECEIVED DATA[dbxTuneGetActiveHistoryStatements]: ", jsonResp);

			//setHistoryStatement(jsonResp);
			
			returnVal = jsonResp;
			
		},
		error: function(xhr, desc, err) 
		{
			console.log(xhr);
			console.log("Details: " + desc + "\nError: " + err);
		}
	}); // end: ajax call
	
	return returnVal;
} // end: function


//-----------------------------------------------------------
// ACTIVE ALARMS
//-----------------------------------------------------------
function radionButtonGroupSetSelectedValue(name, selectdValue) 
{
	console.log("radionButtonGroupSetSelectedValue(): name="+name+", selectedValue="+selectdValue);
	$('input[name="' + name+ '"][value="' + selectdValue + '"]').prop('checked', true);
}

function activeAlarmsRadioClick(radioBut) 
{
	if (radioBut === 'undefined' || radioBut === null)
		return;

	var rVal = typeof(radioBut) == "string" ? radioBut : radioBut.value;
	console.log('activeAlarmsRadioClick(): Active Alarm Window Size[RadioBut]: ' + rVal);
	
	if (rVal === "hide")
	{
		// Hide all Alarms
		$("#active-alarms-win").css("display", "none");
	}
	else
	{
		// Set The OUTER max-size
		$("#active-alarms").css("max-height", rVal);

		// SHOW all Alarms
		$("#active-alarms-win").css("display", "block");
	}

	// Save last known value in "WebBrowser storage"
	getStorage('dbxtune_checkboxes_').set("active-alarms-show-radio", rVal);
}
function activeAlarmsChkClick(checkbox) 
{
	if (checkbox === 'undefined' || checkbox === null)
		return;

	console.log('activeAlarmsChkClick(): Checked: ' + checkbox.checked);
	if (checkbox.checked)
	{
		$("#active-alarms-ctl").css("background-color", 'rgba(255, 195, 83, 1.0)');
		$("#active-alarms")    .css("background-color", 'rgba(255, 195, 83, 1.0)');
	}
	else
	{
		$("#active-alarms-ctl").css("background-color", 'rgba(255, 195, 83, 0.5)');
		$("#active-alarms")    .css("background-color", 'rgba(255, 195, 83, 0.5)');
	}

	// Save last known value in "WebBrowser storage"
	getStorage('dbxtune_checkboxes_').set("active-alarms-solid-chk", checkbox.checked ? 'checked' : 'not');
}
function activeAlarmsCompExtDescClick(checkbox)
{
	if (checkbox === 'undefined' || checkbox === null)
		return;

	console.log('activeAlarmsCompExtDescClick(): Checked: ' + checkbox.checked);

	// Save last known value in "WebBrowser storage"
	getStorage('dbxtune_checkboxes_').set("active-alarms-compExtDesc-chk", checkbox.checked ? 'checked' : 'not');

	// Toggle only within the alarm panel (not active-statements panel)
	toggleActiveTableExtendedDescription('alarm-panel');
}

// do: deferred (since all DOM elements might not be created yet)
setTimeout(function()
{
	// Restore 'Solid Background' at the ACTIVE ALARMS "window"
	var savedVal_activeAlarmsSolidChk = getStorage('dbxtune_checkboxes_').get("active-alarms-solid-chk");
	if (savedVal_activeAlarmsSolidChk == 'checked') $("#active-alarms-solid-chk").attr('checked', 'checked');
	if (savedVal_activeAlarmsSolidChk == 'not')     $("#active-alarms-solid-chk").removeAttr('checked');
	activeAlarmsChkClick( document.getElementById("active-alarms-solid-chk") );

	// Restore 'Window Size' at the ACTIVE ALARMS "window"
	var savedVal_activeAlarmsShowRadio = getStorage('dbxtune_checkboxes_').get("active-alarms-show-radio");
	console.log("RESTORE ALARM Window size: savedVal_activeAlarmsShowRadio="+savedVal_activeAlarmsShowRadio);
	activeAlarmsRadioClick( savedVal_activeAlarmsShowRadio );
	radionButtonGroupSetSelectedValue("active-alarms-show-radio", savedVal_activeAlarmsShowRadio);

	// Restore alarm history window size from localStorage
	try {
		var sb = localStorage.getItem('alarmHistoryBefore');
		var sa = localStorage.getItem('alarmHistoryAfter');
		if (sb !== null) _alarmHistoryBefore = parseInt(sb, 10) || 30;
		if (sa !== null) _alarmHistoryAfter  = parseInt(sa, 10) || 5;
	} catch(e) {}

	// Restore Active Statements dark mode — use same getStorage mechanism as all other checkboxes
	var savedAsD = getStorage('dbxtune_checkboxes_').get('active-statements-dark-chk');
	var asDark = (savedAsD === 'checked') ? true
	           : (savedAsD === 'not')     ? false
	           : (_colorSchema === 'dark');   // first visit: follow page colour scheme
	$('#active-statements-dark-chk').prop('checked', asDark);
	if (asDark) $('#active-statements').addClass('as-dark');
	else        $('#active-statements').removeClass('as-dark');
}, 10);


// Create a CALLBACK function to set TableRow Colors etc, used when calling: jsonToTable(...)
// alarmTrCallback and alarmTdCallback defined in dbxcentral.utils.js


/**
 * Open the #dbx-view-alarmView-dialog modal and populate it via /api/alarm/formatter.
 * Works in both graph.html and index.html (both have the modal + formatter handler).
 * Z-index is bumped above all floating panels using window._dbxTopZ.
 */
// _dbxModalApplyDark, alarmDetailShowModal, alarmTrCallback, alarmTdCallback,
// alarmShowMuteDialog, alarmMuteSubmit, _alarmMuteCurrentRow
// → all defined in dbxcentral.utils.js (loaded by both index.html and graph.html)


// ───────────────────────────────────────────────────────────────────────────
// Alarm Panel — replaces old orange bottom bar
// ───────────────────────────────────────────────────────────────────────────

var _alarmPanelTab      = 'active';  // graph-specific: 'active' or 'history'
var _alarmHistoryTs     = null;
var _alarmHistoryBefore = 30;  // minutes before clicked timestamp
var _alarmHistoryAfter  = 5;   // minutes after clicked timestamp
// Note: _alarmPanelOpen and _alarmPanelAllData are defined in dbxAlarm.js (shared)

function activeStatementsDarkToggle(on)
{
	if (on) $('#active-statements').addClass('as-dark'); else $('#active-statements').removeClass('as-dark');
	getStorage('dbxtune_checkboxes_').set('active-statements-dark-chk', on ? 'checked' : 'not');
}

// graph.html override of alarmPanelToggle — adds history-tab + _colorSchema support
function alarmPanelToggle()
{
	_alarmPanelOpen = !_alarmPanelOpen;
	if (_alarmPanelOpen) {
		var $p = $('#alarm-panel');
		// Convert bottom-anchored to top/left so jQuery UI draggable works
		if (!$p.data('positioned')) {
			var h = $p.outerHeight() || Math.round(window.innerHeight * 0.30);
			$p.css({ bottom: '', top: (window.innerHeight - h) + 'px', left: '0px' });
			$p.data('positioned', true);
		}
		// Restore dark mode preference (default to page colour scheme)
		var savedDark = null;
		try { savedDark = localStorage.getItem('alarmPanelDark'); } catch(e) {}
		var dark = (savedDark !== null) ? (savedDark === '1') : (_colorSchema === 'dark');
		$('#alarm-dark-chk').prop('checked', dark);
		alarmDarkToggle(dark);
		$p.show();
		if (_alarmHistoryTs) {
			alarmPanelShowTab('history');
			alarmPanelLoadHistory(_alarmHistoryTs);
		} else {
			alarmPanelShowTab('active');
			alarmPanelLoadActive();
		}
	} else {
		$('#alarm-panel').hide();
	}
}

function alarmPanelShowTab(tab)
{
	_alarmPanelTab = tab;
	if (tab === 'history' && _alarmHistoryTs) {
		alarmPanelLoadHistory(_alarmHistoryTs);
	} else if (tab === 'active') {
		$('#alarm-panel-title').text('Active Alarms');
		$('#alarm-history-range').hide();
	}
}

function alarmHistoryWindowChanged()
{
	var b = parseInt($('#alarm-panel-before').val(), 10);
	var a = parseInt($('#alarm-panel-after').val(), 10);
	if (isNaN(b) || b < 1) b = 1;
	if (isNaN(a) || a < 0) a = 0;
	_alarmHistoryBefore = b;
	_alarmHistoryAfter  = a;
	try { localStorage.setItem('alarmHistoryBefore', b); localStorage.setItem('alarmHistoryAfter', a); } catch(e) {}
	if (_alarmHistoryTs) alarmPanelLoadHistory(_alarmHistoryTs);
}

// alarmPanelUpdateBtn is defined in dbxAlarm.js (shared)

function alarmPanelLiveRefresh(srvName)
{
	// If the user is viewing history, capture that state now (before the async AJAX).
	// We must NOT overwrite _alarmHistoryTs / _alarmPanelTab or re-render the panel
	// while history mode is active — doing so (as the old code did unconditionally) would
	// replace the history view with one server's live alarms on every data push.
	var inHistory = (_alarmHistoryTs !== null);

	if (!inHistory) {
		_alarmPanelTab = 'active';
		$('#alarm-panel-title').text('Active Alarms');
	}

	$.ajax({
		url: "api/alarm/active",
		type: 'get',
		success: function(data) {
			var jsonResp;
			try { jsonResp = JSON.parse(data); } catch(e) { return; }
			// Filter to servers currently shown in this graph page
			if (Array.isArray(jsonResp) && _serverList && _serverList.length > 0)
				jsonResp = jsonResp.filter(function(e) { return _serverList.indexOf(e.srvName) >= 0; });
			var activeCount = Array.isArray(jsonResp) ? jsonResp.filter(function(e) { return !e.isMuted; }).length : 0;
			var mutedCount  = Array.isArray(jsonResp) ? jsonResp.filter(function(e) { return  e.isMuted; }).length : 0;

			// Always update button badge + page title notification
			alarmPanelUpdateBtn(activeCount, mutedCount);
			if (activeCount > 0) pageTitleNotification.on("---ALARM---", 1000);
			else                 pageTitleNotification.off();

			// Auto Open / Close — skip auto-close in history mode (user is actively reviewing)
			if (activeCount > 0 && !_alarmPanelOpen && $('#alarm-auto-open-chk').prop('checked'))
				alarmPanelToggle();
			else if (!inHistory && activeCount === 0 && _alarmPanelOpen && $('#alarm-auto-open-chk').prop('checked'))
				alarmPanelToggle();

			// Only re-render panel content in live/active mode
			if (!inHistory && _alarmPanelOpen && _alarmPanelTab === 'active')
				alarmPanelRenderActive(jsonResp, srvName);
		},
		error: function() {}
	});
}

function alarmPanelLoadActive()
{
	$('#alarm-panel-title').text('Active Alarms');
	$('#alarm-panel-content').html('<em style="color:#777">Loading&hellip;</em>');
	$.ajax({
		url: "api/alarm/active",
		type: 'get',
		success: function(data) {
			var jsonResp;
			try { jsonResp = JSON.parse(data); } catch(e) {
				$('#alarm-panel-content').html('<em>Parse error</em>'); return;
			}
			// Filter to servers currently shown in this graph page
			if (Array.isArray(jsonResp) && _serverList && _serverList.length > 0)
				jsonResp = jsonResp.filter(function(e) { return _serverList.indexOf(e.srvName) >= 0; });
			var activeCount = Array.isArray(jsonResp) ? jsonResp.filter(function(e) { return !e.isMuted; }).length : 0;
			var mutedCount  = Array.isArray(jsonResp) ? jsonResp.filter(function(e) { return  e.isMuted; }).length : 0;
			alarmPanelUpdateBtn(activeCount, mutedCount);
			alarmPanelRenderActive(jsonResp, null);
		},
		error: function() {
			$('#alarm-panel-content').html('<em style="color:red">Failed to load alarms.</em>');
		}
	});
}

function alarmPanelLoadHistory(ts)
{
	_alarmHistoryTs = ts;
	if (!_alarmPanelOpen) return;
	_alarmPanelTab = 'history';
	var mTs = moment(ts, "YYYY-MM-DD HH:mm:ss");
	$('#alarm-panel-title').text('Active Alarms at  ' + mTs.format('YYYY-MM-DD HH:mm:ss'));
	$('#alarm-history-range').hide();
	// Capture scroll pos and freeze listener BEFORE replacing content — shrinking the content
	// fires a scroll event that would otherwise reset _alarmScrollPos to 0.
	_alarmScrollPos.top  = $('#alarm-panel-body').scrollTop();
	_alarmScrollPos.left = $('#alarm-panel-body').scrollLeft();
	_alarmScrollFrozen   = true;
	$('#alarm-panel-content').html('<em style="color:#777">Loading&hellip;</em>');
	// Fetch up to 24h before T so we catch alarms raised well before the clicked timestamp
	var start = mTs.clone().subtract(24, 'hours').format("YYYY-MM-DD HH:mm:ss");
	var end   = mTs.clone().add(5, 'minutes').format("YYYY-MM-DD HH:mm:ss");
	$.ajax({
		url: "api/alarm/history",
		data: { startTime: start, endTime: end },
		type: 'get',
		success: function(data) {
			var jsonResp;
			try { jsonResp = JSON.parse(data); } catch(e) {
				$('#alarm-panel-content').html('<em>Parse error</em>'); return;
			}
			// Filter to servers currently shown in this graph page
			if (Array.isArray(jsonResp) && _serverList && _serverList.length > 0)
				jsonResp = jsonResp.filter(function(e) { return _serverList.indexOf(e.srvName) >= 0; });
			// Compute which alarms were ACTIVE at the clicked timestamp:
			// For each unique alarm keep the latest event before T, then check
			// createTime <= T and (cancelTime is null/empty or cancelTime > T)
			var activeAtT = alarmComputeActiveAt(jsonResp, mTs);
			alarmPanelRenderHistory(activeAtT, mTs);
		},
		error: function() {
			$('#alarm-panel-content').html('<em style="color:red">Failed to load alarm history.</em>');
		}
	});
}

/**
 * From a list of alarm history events, compute which alarms were ACTIVE at moment mTs.
 * An alarm is active at T when createTime <= T and (cancelTime is null/blank or cancelTime > T).
 * Multiple events for the same alarm key are deduplicated — only the latest event before T is kept.
 */
function alarmComputeActiveAt(events, mTs)
{
	if (!Array.isArray(events)) return [];
	var alarmMap = {}; // unique alarm key → latest event

	events.forEach(function(ev) {
		var evTime = ev.eventTime ? moment(ev.eventTime) : null;
		if (evTime && evTime.isAfter(mTs)) return; // skip events after T

		// Logical alarm identity — mirrors AlarmEvent.equals()/hashCode() in AlarmEvent.java.
		// AlarmEvent also includes: category, severity, state — intentionally omitted here
		// because those can change across RAISE/CANCEL cycles for the same logical alarm
		// (e.g. severity can be promoted). extraInfo is included because it distinguishes
		// sub-variants of the same alarmClass (e.g. per-partition alarms).
		var key = [ev.srvName, ev.alarmClass, ev.serviceType, ev.serviceName, ev.serviceInfo, ev.extraInfo || ''].join('|');
		if (!alarmMap[key] || (evTime && evTime.isAfter(moment(alarmMap[key].eventTime)))) {
			alarmMap[key] = ev;
		}
	});

	var active = [];
	Object.keys(alarmMap).forEach(function(key) {
		var ev         = alarmMap[key];
		var createTime = ev.createTime  ? moment(ev.createTime)  : null;
		var cancelTime = ev.cancelTime  ? moment(ev.cancelTime)  : null;
		var createdBeforeT = createTime && !createTime.isAfter(mTs);
		var notCancelledAtT = !cancelTime || cancelTime.isAfter(mTs);
		if (createdBeforeT && notCancelledAtT) {
			active.push(ev);
		}
	});

	return active;
}

// alarmPanelRenderActive, alarmPanelApplyFilter, alarmPanelFilterInput,
// alarmPanelShowMutedToggle, alarmPanelRevealMuted — all defined in dbxAlarm.js (shared)

function alarmPanelRenderHistory(jsonResp, mTs)
{
	// Store into the shared data variable so alarmPanelApplyFilter() operates
	// on the history snapshot rather than stale active-alarm data.
	_alarmPanelAllData = Array.isArray(jsonResp) ? jsonResp : [];
	if (_alarmPanelAllData.length === 0) {
		var atStr = mTs ? ' at ' + mTs.format('HH:mm:ss') : '';
		$('#alarm-panel-content').html('<div class="alert alert-success mt-1 mb-0 py-1 px-2" style="font-size:0.9em;">&#10003; No active alarms' + atStr + '.</div>');
		$('#alarm-filter-count').text('0 rows');
		return;
	}
	alarmPanelApplyFilter();
}

function alarmPanelSliderRefresh(ts)
{
	alarmPanelLoadHistory(ts);
}

// Fetch alarm events covering the full timeline range and render orange tick marks
// on the slider for any timestamp where a RAISE event occurred.
function renderAlarmTimelineMarkers(oldestTsStr, newestTsStr, momentsArray)
{
	if (!momentsArray || momentsArray.length < 2) return;
	var oldestMs = moment(oldestTsStr, 'YYYY-MM-DD HH:mm:ss').valueOf();
	var newestMs = moment(newestTsStr, 'YYYY-MM-DD HH:mm:ss').valueOf();
	var rangeMs  = newestMs - oldestMs;
	if (rangeMs <= 0) return;
	$.ajax({
		url: 'api/alarm/history',
		data: { startTime: oldestTsStr, endTime: newestTsStr },
		type: 'get',
		success: function(data) {
			var alarms;
			try { alarms = JSON.parse(data); } catch(e) { return; }
			if (!Array.isArray(alarms) || alarms.length === 0) return;
			// Filter to servers currently shown in this graph page
			if (_serverList && _serverList.length > 0)
				alarms = alarms.filter(function(a) { return _serverList.indexOf(a.srvName) >= 0; });
			if (alarms.length === 0) return;
			var slider = document.getElementById('dbx-history-timeline-slider');
			if (!slider) return;
			var parent = slider.parentNode;
			// Clear any old alarm markers before adding new ones
			var old = parent.getElementsByClassName('dbx-history-alarm-marker');
			while (old[0]) old[0].parentNode.removeChild(old[0]);
			// Add one marker per alarm event (deduplicated by ~10s buckets)
			var lastPos = -999;
			alarms.forEach(function(alarm) {
				var evtTime = alarm.eventTime || alarm.SessionSampleTime;
				if (!evtTime) return;
				var evtMs = moment(evtTime, ['YYYY-MM-DD HH:mm:ss', moment.ISO_8601]).valueOf();
				var pct   = (evtMs - oldestMs) / rangeMs * 100;
				if (pct < 0 || pct > 100) return;
				if (Math.abs(pct - lastPos) < 0.5) return; // deduplicate close markers
				lastPos = pct;
				var el = document.createElement('div');
				el.className = 'dbx-history-alarm-marker';
				el.style.left = pct + '%';
				var action = (alarm.action || '').toUpperCase();
				el.style.backgroundColor = (action === 'CANCEL') ? 'rgba(0,180,0,0.7)' : 'rgba(255,100,0,0.85)';
				var tip = (alarm.alarmClass || '') + (alarm.serviceName ? ' / ' + alarm.serviceName : '')
					+ '\n' + (alarm.action || '') + ' @ ' + evtTime;
				el.title = tip;
				el.addEventListener('click', function() {
					alarmPanelLoadHistory(moment(evtMs).format('YYYY-MM-DD HH:mm:ss'));
					if (!_alarmPanelOpen) alarmPanelToggle();
				});
				parent.appendChild(el);
			});
		},
		error: function() {} // silently ignore
	});
}


function dbxTuneCheckActiveAlarms()
{
	console.log("dbxTuneCheckActiveAlarms()");

	if ( ! _subscribe )
	{
		$("#subscribe-feedback-msg").html("<s>subscribe</s>");
		return;
	}

	$.ajax(
	{
		url: "api/alarm/active",
		type: 'get',
		//async: false,   // to call it one by one (async: true = spawn away a bunch of them in the background)
			
		success: function(data, status) 
		{
			// console.log("RECEIVED DATA: "+data);
			var jsonResp = JSON.parse(data);
				
			// update ACTIVE Alarm view (note: there can be several servers)
			var activeAlarmsTopDiv = document.getElementById("active-alarms-top");
			var activeAlarmsDiv    = document.getElementById("active-alarms");
			var activeAlarmsWinDiv = document.getElementById("active-alarms-win");

			var alarmSrvCount = 0;
			var alarmSumCount = 0;

			// reorder: stuff an outer "map", which is the server, and all it's alarms
			var srvAlarmMap = {}; // new object (this will be the Map with <String>, Array<AlarmEntry>)
			for (var i=0; i<jsonResp.length; i++) {
				var srvName = jsonResp[i].srvName;

				// Check if KEY exists, if NOT create an array, where we will "stuff" AlarmEntries
				if ( ! srvAlarmMap.hasOwnProperty(srvName) )
					srvAlarmMap[srvName] = [];

				srvAlarmMap[srvName].push(jsonResp[i]);
			}
			if (_debug > 1)
				console.log("dbxTuneCheckActiveAlarms(): srvAlarmMap:",srvAlarmMap);

			// Loop the alarm MAP
			for (var key in srvAlarmMap) 
			{
				var entry = srvAlarmMap[key];
				
				// re-write the "alarmEntry"... add 'hasExtDesc' and 'hasLastExtDesc'
				entry = reWriteAlarmEntry(entry);

				// var graphServerName = entry.srvName;
				var graphServerName = key;

				alarmSrvCount++;

				if (_serverList.indexOf(graphServerName) == -1)
				{
					if (_debug > 1)
						console.log("dbxTuneCheckActiveAlarms(): SKIPPING... NOT in the server list. graphServerName="+graphServerName+", serverlist:", _serverList);
					continue;
				}
				if (_debug > 1)
					console.log("dbxTuneCheckActiveAlarms(): graphServerName="+graphServerName+", entry:", entry);

				// If any Active alarms create a new SRV-DIV and a table...
				if (typeof graphServerName !== 'undefined')
				{
					let srvDiv = document.getElementById("active-alarms-srv-"+graphServerName);
					if (srvDiv === null)
					{
						srvDiv = document.createElement("div");
						srvDiv.id = "active-alarms-srv-"+graphServerName;

						activeAlarmsWinDiv.appendChild(srvDiv);
					}
					let tab = jsonToTable(entry, false, alarmTrCallback, alarmTdCallback);
					srvDiv.innerHTML = "Active Alarms for server: <b>"+graphServerName+"</b>";
					srvDiv.appendChild(tab);
					srvDiv.appendChild(document.createElement("br"));

					alarmSumCount += entry.length;

					if (_debug > 1)
						console.log("TABLE for srv'"+graphServerName+"': ", tab);
				}
				else // Remove the SRV-DIV if no active alarm
				{
					let srvDiv = document.getElementById("active-alarms-srv-"+graphServerName);
					if (srvDiv !== null)
						activeAlarmsWinDiv.removeChild(srvDiv);
				}
			}

			// should we show the activeAlarmsDiv or not?
			if (_debug > 0)
				console.log("activeAlarmsWinDiv.getElementsByTagName('*').length: "+activeAlarmsWinDiv.getElementsByTagName('*').length+", alarmSumCount="+alarmSumCount);
			
//			if (activeAlarmsWinDiv.getElementsByTagName('*').length > 0)
			if (alarmSumCount > 0)
			{
				activeAlarmsTopDiv.style.visibility = 'visible';
				activeAlarmsDiv.style.visibility = 'visible';
				pageTitleNotification.on("---ALARM---", 1000);
				// Auto Open: open panel when alarms exist
				if ($("#alarm-auto-open-chk").prop('checked') && !_alarmPanelOpen) { alarmPanelToggle(); }
			}
			else
			{
				activeAlarmsTopDiv.style.visibility = 'hidden';
				activeAlarmsDiv.style.visibility = 'hidden';
				pageTitleNotification.off();
				// Auto Open: close panel when no alarms
				if ($("#alarm-auto-open-chk").prop('checked') &&  _alarmPanelOpen) { alarmPanelToggle(); }
			}

			// Set how many ACTIVE ALarms we have...
			document.getElementById('active-alarms-count').innerHTML = alarmSumCount;

			// Update the alarm panel button pills and refresh the panel if open
			var _filteredResp = Array.isArray(jsonResp) ? jsonResp.filter(function(e) { return _serverList.indexOf(e.srvName) >= 0; }) : [];
			var _activeCount  = _filteredResp.filter(function(e) { return !e.isMuted; }).length;
			var _mutedCount   = _filteredResp.filter(function(e) { return  e.isMuted; }).length;
			alarmPanelUpdateBtn(_activeCount, _mutedCount);
			if (_alarmPanelOpen && _alarmPanelTab === 'active')
				alarmPanelRenderActive(_filteredResp, null);
		},
		error: function(xhr, desc, err) 
		{
			console.log(xhr);
			console.log("Details: " + desc + "\nError: " + err);
		}
	}); // end: ajax call
} // end: function

// Add "helper" entries 'hasExtDesc' and 'hasLastExtDesc' in the AlarmEntry...
function reWriteAlarmEntry(inAlarmRows)
{
	//console.log("reWriteAlarmEntry-input", inAlarmRows);
	let outAlarmRows = [];
	
	// Loop the input and add it to the output
	inAlarmRows.forEach(function (row)
	{
		let outRow = {};
		for (let key in row)
		{
			if (row.hasOwnProperty(key))
			{
				if (key === 'reRaiseExtendedDescription')
				{
					// rename 'reRaiseExtendedDescription' column to 'lastExtendedDescription'
					outRow['lastExtendedDescription'] = row[key];
				}
				else
				{
					// Copy key=value into the output object
					outRow[key] = row[key];
				}
			
				// after key 'repeatCnt' add 'hasExtDesc' and 'hasLastExtDesc'
				if (key === 'repeatCnt')
				{
					let hasExtDesc     = row.hasOwnProperty('extendedDescription')        && row['extendedDescription']        !== "";
					let hasLastExtDesc = row.hasOwnProperty('lastExtendedDescription')    && row['lastExtendedDescription']    !== "";
					let hasReRaiseExtD = row.hasOwnProperty('reRaiseExtendedDescription') && row['reRaiseExtendedDescription'] !== "";

					outRow['alarmInfo']      = true;
					outRow['hasExtDesc']     = hasExtDesc;
					outRow['hasLastExtDesc'] = hasLastExtDesc || hasReRaiseExtD;
				}
			}
		}
		outAlarmRows.push(outRow);
	});
	//console.log("reWriteAlarmEntry-output", outAlarmRows);
	return outAlarmRows;
}


/** 
 * Subscribe to graph changes from the server side
 */
function dbxTuneGraphSubscribe() 
{
	const sessionName = getParameter("sessionName");
	const graphList   = getParameter("graphList", "");
//	var   subscribe   = getParameter("subscribe", false);
	// const debug       = getParameter("debug",     0);
	const endTime     = getParameter("endTime",   "");
//	console.log("Passed subscribe="+subscribe);

	var webSocket;

//	if (subscribe === "false")
//		subscribe = false;
//
//	// turn subscribe off if 'endTime' is present
//	if (endTime !== "")
//		subscribe = false;

	if ( ! _subscribe )
	{
		$("#subscribe-feedback-msg").html("<s>subscribe</s>");
		return;
	}

	// data the updates every time we get a new message
	var dbxLastSubscribeTime = moment();

	/** 
	 * Update clock... on when last subscription event was received
	 */
	function startUpdateSubscribeClock()
	{
		updateSubscribeClock();
		setTimeout(startUpdateSubscribeClock, 1000);
	}

	function updateSubscribeClock()
	{
		if (isHistoryViewActive())
		{
			if (webSocket !== undefined)
			{
				webSocket.close();
			}

			$("#subscribe-feedback-msg" ).html("");
			$("#subscribe-feedback-srv" ).html("");

			$("#subscribe-feedback-time").css("color", "");
			$("#subscribe-feedback-time").html("History View");

//			if ($("#subscribe-feedback-time").text() == "> History View <")
//				$("#subscribe-feedback-time").html("History View &nbsp;&nbsp;");
//			else
//				$("#subscribe-feedback-time").text("> History View <");
			
			return;
		}
		
		let ageInSec = Math.floor((moment() - dbxLastSubscribeTime) / 1000);

		$("#subscribe-feedback-msg").css("display", "none");
		$("#subscribe-feedback-time").css("color", "");
//		$("#subscribe-feedback-time").html(", " + ageInSec + " seconds ago.");
		$("#subscribe-feedback-time").html("&nbsp;" + ageInSec + "s");

		if (ageInSec > 120)
		{
			$("#subscribe-feedback-msg").css("display", "block");
			$("#subscribe-feedback-time").css("color", "red");
		}
		if (ageInSec > 300)
		{
			// Reload page if we havn't got any data in 5 minutes
			location.reload();
		}

		let msgText = $("#subscribe-feedback-msg").text();
		if ( ! msgText.startsWith("data") )
			$("#subscribe-feedback-msg").css("display", "block");
		
		
		// TEST: pageTitleNotification
		// if (ageInSec > 10 && ageInSec < 20)
		// 	pageTitleNotification.on("---DUMMY---", 1000);
		// else
		//  	pageTitleNotification.off();
	}

	function subscribeConnectWs() 
	{
		if (isHistoryViewActive())
			return;

		if ( ! _subscribe )
			return;
			
		// open the connection if one does not exist
		if (webSocket !== undefined && webSocket.readyState !== WebSocket.CLOSED) 
		{
			return;
		}

		// Create a websocket
		const protocol = window.location.protocol === 'https:' ? 'wss://' : 'ws://';
		const wsUrl = protocol + location.host + "/api/chart/broadcast-ws?serverList="+_serverList+"&graphList="+encodeURIComponent(graphList);
		console.log("subscribeConnectWs(): url="+wsUrl);
		webSocket = new WebSocket(wsUrl);
		
 
		webSocket.onmessage = function(e) 
		{
			dbxLastSubscribeTime = moment();
			addData(e.data);
			addSubscribeFeedback('data', e.data);
		};
 
		webSocket.onopen = function(e) 
		{
			dbxLastSubscribeTime = moment();
			addSubscribeFeedback('ws', 'conn opened');
		};
 
		webSocket.onclose = function(e) 
		{
			addSubscribeFeedback('ws', 'conn closed');
			setTimeout(subscribeConnectWs, 10*1000);
		};

		// Log some extra info on errors
		webSocket.onerror = function (e)
		{
			console.log("WebSocket.onError: code="+e.code+", e="+e, e);
		}
		// webSocket.onerror = function (e) 
		// {
		// 	dbxLastSubscribeTime = moment();
		// 	if      (e.readyState == WebSocket.CONNECTING) { addSubscribeFeedback('ws', 'CONNECTING'); } 
		// 	else if (e.readyState == WebSocket.OPEN)       { addSubscribeFeedback('ws', 'OPEN');       } 
		// 	else if (e.readyState == WebSocket.CLOSING)    { addSubscribeFeedback('ws', 'CLOSING');    } 
		// 	else if (e.readyState == WebSocket.CLOSED)     { addSubscribeFeedback('ws', 'CLOSED');     }
		// };
	}
	

	console.log("dbxTuneGraphSubscribe(): _subscribe="+_subscribe+", expr="+(_subscribe===true));
	$("#subscribe-feedback-time").html("subscribe="+_subscribe);
	if (_subscribe)
	{
		startUpdateSubscribeClock(); // is automatically executed every second

		if (!!window.WebSocket)
		{
			subscribeConnectWs();
		} else {
			alert('The browser does not support WebSocket, dynamic graph updates will not be done.');
		}
	} // end: if subscribe

	/**
	 * 
	 */
	function addSubscribeFeedback(type, message) 
	{
		// var msg = type + ": received at: " + moment().format("HH:mm:ss");
		let msg = type + ": " + moment().format("HH:mm:ss");
		if (type !== "data") {
			msg = msg + " " + message;
		}
		$("#subscribe-feedback-msg").html(msg);
	}

	/**
	 * 
	 */
	function addData(message) 
	{
		if (isHistoryViewActive())
		{
			console.log("addData(): SKIPPING this since we are in 'HISTORY' mode.");
			return;
		}
		
		let graphJson;
		try {
			graphJson = JSON.parse(message);
		} catch(ex) {
			console.log("Problems parsing message: "+message);
			return;
		}
		if (_debug > 0)
			console.log("/api/chart/broadcast: add(): graphJson=", graphJson);

		let graphHead       = graphJson.head;
		let graphServerName = graphHead.serverName;
		let graphCollectors = graphJson.collectors;
		let dbxtuneAppName  = graphHead.appName;

		let appName     = graphHead.appName;

		// Build a new Active Statements (read data for current servers from DbxCentral)
		dbxTuneCheckActiveStatements();

		// CM Detail live refresh: reload current CM when new data arrives (not history, not paused)
		cmDetailLiveRefresh(graphServerName);

		// Alarm panel: update button badge + refresh if panel is open
		alarmPanelLiveRefresh(graphServerName);

		// TODO:
		// to let the Browser update it's GUI it would have been nice with a yield() method
		// but we might be able to do something like:
		//  - loop and -> push: "dbxGraph" and "graph" onto a queue/array
		//  - then pass the queue/array to a method, which dequeues the array, calls "dbxGraph.addSubscribeData([graph])", then calls itself via: setTimeout(..., 0)
		// or maybe something like this: http://debuggable.com/posts/run-intense-js-without-freezing-the-browser:480f4dd6-f864-4f72-ae16-41cccbdd56cb
		//-----------------------------------------------------------------------------------
		// Note the below code takes too looooong when we have many graphs... the above "queue"/setTimeout() stuff might be a viable solution.
		for(let gc=0; gc<graphCollectors.length; gc++) 
		{
			let collector = graphCollectors[gc];
//			let cmName    = collector.cmName;

			// console.log("addDate(): collector="+collector, collector);
			for(let g=0; g<collector.graphs.length; g++) 
			{
				const graph = collector.graphs[g];

				// console.log("addDate(): graph="+graph, graph);
				const cmName          = graph.cmName;
				const graphName       = graph.graphName;

				for(let i=0; i<_graphMap.length; i++)
				{
					const dbxGraph = _graphMap[i];

					if (dbxGraph.getServerName() === graphServerName && dbxGraph.getCmName() === cmName && dbxGraph.getGraphName() === graphName)
					{
					//	setTimeout(function() { 
							if (_debug > 0)
								console.log("ADD: subscribe-graph-data for srvName="+graphServerName+", cmName="+cmName+", graphName="+graphName+": ", graph);
							dbxGraph.addSubscribeData( [ graph ] );  // as a array with 1 entry
					//	}, i+1);
					}
				}
			}
		}

		// update ACTIVE Alarm view (note: there can be several servers)
		var activeAlarmsDiv    = document.getElementById("active-alarms");
		var activeAlarmsWinDiv = document.getElementById("active-alarms-win");

		// If any Active alarms create a new SRV-DIV and a table...
		if (typeof graphJson.activeAlarms !== 'undefined')
		{
//			var entry = srvAlarmMap[key];
			var entry = graphJson.activeAlarms
			
			// re-write the "alarmEntry"... add 'hasExtDesc' and 'hasLastExtDesc'
			entry = reWriteAlarmEntry(entry);

			
			let srvDiv = document.getElementById("active-alarms-srv-"+graphServerName);
			if (srvDiv === null)
			{
				srvDiv = document.createElement("div");
				srvDiv.id = "active-alarms-srv-"+graphServerName;

				activeAlarmsWinDiv.appendChild(srvDiv);
			}
			let tab = jsonToTable(entry, false, alarmTrCallback, alarmTdCallback);
			srvDiv.innerHTML = "Active Alarms for server: <b>"+graphServerName+"</b>";
			srvDiv.appendChild(tab);
			srvDiv.appendChild(document.createElement("br"));

			if (_debug > 0)
				console.log("TABLE for srv'"+graphServerName+"': ", tab);
		}
		else // Remove the SRV-DIV if no active alarm
		{
			let srvDiv = document.getElementById("active-alarms-srv-"+graphServerName);
			if (srvDiv !== null)
				activeAlarmsWinDiv.removeChild(srvDiv);
		}
		// should we show the activeAlarmsDiv or not?
		if (_debug > 0)
			console.log("activeAlarmsWinDiv.getElementsByTagName('*').length: "+activeAlarmsWinDiv.getElementsByTagName('*').length);

		if (activeAlarmsWinDiv.getElementsByTagName('*').length > 0)
		{
			activeAlarmsDiv.style.visibility = 'visible';
			pageTitleNotification.on("---ALARM---", 1000);
		}
		else
		{
			activeAlarmsDiv.style.visibility = 'hidden';
			pageTitleNotification.off();
		}


		// Update fisrt/last timestamp in the navbar
		updateNavbarInfo();

		// Show LAST Received "Server name" in navbar, and after X seconds fade out 
		$("#subscribe-feedback-srv").css("display", "block");
		$("#subscribe-feedback-srv").html("<i class='fa fa-mail-forward'></i> " + graphServerName + " <i class='fa fa-line-chart'></i> "); // fa fa-mail-forward SRVNAME -> 
		updateSubscribeClock();

		// Cancel previous timer
		if (_showSrvTimer !== null)
			clearTimeout(_showSrvTimer);

		// Kick off a new timer
		_showSrvTimer = setTimeout(function() {
			$('#subscribe-feedback-srv').fadeToggle(1000);
			_showSrvTimer = null;
		}, 4000); // hide after 4s

		// Live refresh CM detail panel if open and not paused
		if (_cmSrvName === graphServerName && _cmName
				&& $('#cm-detail-panel').is(':visible')
				&& !$('#cm-detail-paused').prop('checked'))
		{
			var liveTs = graphHead.sampleTime;
			if (liveTs)
			{
				// Normalize to "yyyy-MM-dd HH:mm:ss" (strip milliseconds if present)
				liveTs = liveTs.replace('T', ' ').replace(/\.\d+$/, '').substring(0, 19);
				_cmTimestamp = liveTs;
				$('#cm-detail-ts').text('@ ' + liveTs + ' (live)');
				cmDetailLoadData(_cmSrvName, _cmName, liveTs, _cmType);
			}
		}
	// Live refresh DBMS Config panel if open
	if ($('#dbms-config-panel').is(':visible') && _dcSrvName === graphServerName)
	{
		var dcLiveTs = graphHead.sampleTime;
		if (dcLiveTs) {
			dcLiveTs = dcLiveTs.replace('T', ' ').replace(/\.\d+$/, '').substring(0, 19);
			dbmsConfigSliderRefresh(dcLiveTs);
		}
	}
	} // end: addData()
} // end: function


	function escapeXml(unsafe) 
	{
		return unsafe.replace(/[<>&'"]/g, function (c) 
		{
			switch (c) {
				case '<': return '&lt;';
				case '>': return '&gt;';
				case '&': return '&amp;';
				case '\'': return '&apos;';
				case '"': return '&quot;';
			}
		});
	}
	/**
	 * INTERNAL: createToolTipDiv()
	 */
	function createActiveStatementToolTipDiv(data, sqlDialect)
	{
			if (_debug > 1)
				console.log("createActiveStatementToolTipDiv():: sqlDialect=|" + sqlDialect + "|");

		//const TT_PREFIX  = "<div title='Click for Detailes' data-toggle='modal' data-target='#dbx-view-sqltext-dialog' data-objectname='' data-tooltip='";
		//const TT_POSTFIX = "'>&nbsp;</div>";
		
		//const ttData = stripHtml(rowData.DbccSqlText);
		//td.innerHTML = TT_PREFIX + ttData + TT_POSTFIX;

		var doStrip = true;
		if (typeof data === 'string' || data instanceof String)
		{
			if (data.startsWith("<?xml") || data.startsWith("<ShowPlanXML "))
				doStrip = false;
		}
//		if (data.star)
		
		// Mabe check if data is XML or similar, then do NOT strip...
		const dataVal = doStrip ? stripHtml(data) : data;
//		const dataVal = data;

		const div = document.createElement("div");
		div.innerHTML = "&nbsp;";
		div.setAttribute("title"           , "Click to Open Text Dialog... \n-------------------------------\n" + dataVal);
		div.setAttribute("data-toggle"     , 'modal');
		div.setAttribute("data-target"     , '#dbx-view-sqltext-dialog');
		div.setAttribute("data-objectname" , '');
		div.setAttribute("data-tooltip"    , dataVal);
		div.setAttribute("data-sqldialect" , sqlDialect);
	
		return div;
	}
	function createLockTableToolTipDiv(data, label)
	{
		const dataVal  = data;
		const labelVal = label;

		const div = document.createElement("div");
		div.innerHTML = "&nbsp;";
		div.setAttribute("title"           , "Click to Open Text Dialog... \n-------------------------------\n" + htmlTableToAscii(dataVal));
		div.setAttribute("data-toggle"     , 'modal');
		div.setAttribute("data-target"     , '#dbx-view-lockTable-dialog');
		div.setAttribute("data-objectname" , labelVal);
		div.setAttribute("data-tooltip"    , dataVal);
	
		return div;
	}
	function createSqlServerQueryPlanToolTipDiv(data, rowData)
	{
		// FIXME: create a modal-div that can show a SQL Server Showplan (using https://github.com/JustinPealing/html-query-plan)
		//return createActiveStatementToolTipDiv(data);

		const dataVal = data;
		const lastKnownSql = rowData.hasOwnProperty('lastKnownSql') ? rowData.lastKnownSql : "";

		const div = document.createElement("div");
		div.innerHTML = "&nbsp;";
		div.setAttribute("title"           , "Click to Open Text Dialog... \n-------------------------------\n" + dataVal);
		div.setAttribute("data-toggle"     , 'modal');
		div.setAttribute("data-target"     , '#dbx-view-ssShowplan-dialog');
		div.setAttribute("data-objectname" , '');
		div.setAttribute("data-tooltip"    , dataVal);
		div.setAttribute("data-sqltext"    , lastKnownSql);
	
		return div;
	}
	function createPostgresQueryPlanToolTipDiv(data)
	{
		// creates a div that will open a modal-div where we can VIEW a Postgres Execution Plan (using https://github.com/dalibo/pev2?tab=readme-ov-file)

		const dataVal = data;

		const div = document.createElement("div");
		div.innerHTML = "&nbsp;";
		div.setAttribute("title"           , "Click to Open Text Dialog... \n-------------------------------\n" + dataVal);
		div.setAttribute("data-toggle"     , 'modal');
		div.setAttribute("data-target"     , '#dbx-view-pgShowplan-dialog');
		div.setAttribute("data-objectname" , '');
		div.setAttribute("data-tooltip"    , dataVal);
//		div.setAttribute("data-planContent"     , 'DummyValue'); --> will become object member 'plancontent'
//		div.setAttribute("data-plan-content"    , 'DummyValue'); --> will become object member 'planContent' // kebab-case -->> camelCase
	
		return div;
	}
	 
	 
	/**
	 * Show a click-to-detail modal for an Active Statements row.
	 * Renders all non-empty fields as a 2-column table, client-side — no API call needed.
	 * Reuses #dbx-view-alarmView-dialog (same modal, different content).
	 */
	function activeStmtDetailShowModal(row, metaDataArr, stripHtml)
	{
		var $modal = $('#dbx-view-alarmView-dialog');
		if ($modal.length === 0) return;

		var topZ = (window._dbxTopZ || 910) + 200;
		$modal.css('z-index', topZ);
		setTimeout(function() { $('.modal-backdrop').css('z-index', topZ - 1); }, 30);
		_dbxModalApplyDark($modal);

		$('#dbx-view-alarmView-label').text('Statement Detail');
		$('#dbx-view-alarmView-objectName').text('');

		// Build meta lookup for label/description overrides
		var metaMap = {};
		if (Array.isArray(metaDataArr))
			metaDataArr.forEach(function(m) { if (m && m.columnName) metaMap[m.columnName] = m; });

		var html = '<table class="table table-sm table-bordered" style="font-size:0.85em;word-break:break-word;">';
		html += '<thead class="thead-light"><tr><th style="white-space:nowrap">Field</th><th>Value</th></tr></thead><tbody>';

		Object.keys(row).forEach(function(key) {
			var val = row[key];
			if (val === null || val === undefined || val === '') return; // skip empty

			var displayVal;
			if (typeof val === 'boolean') {
				displayVal = val
					? '<i class="fa fa-check-square-o text-success"></i>'
					: '<i class="fa fa-square-o text-muted"></i>';
			} else if (typeof val === 'string') {
				if (stripHtml) {
					var safe = val.replace(/<[^>]+>/g, '');
					displayVal = '<pre style="white-space:pre-wrap;margin:0;font-size:0.9em;">'
						+ $('<span>').text(safe).html() + '</pre>';
				} else if (looksLikeHtml(val)) {
					displayVal = '<div style="overflow:auto;">' + val + '</div>';
				} else {
					displayVal = '<pre style="white-space:pre-wrap;margin:0;font-size:0.9em;">'
						+ $('<span>').text(val).html() + '</pre>';
				}
			} else {
				displayVal = String(val);
			}

			var label = (metaMap[key] && metaMap[key].label) ? metaMap[key].label : key;
			html += '<tr><td style="white-space:nowrap;font-weight:bold;">' + label
				+ '</td><td>' + displayVal + '</td></tr>';
		});

		html += '</tbody></table>';
		$('#dbx-view-alarmView-content').html(html);
		$modal.modal('show');
	}

	/**
	 * INTERNAL: buildActiveStatementDiv
	 */
	function buildActiveStatementDiv(appName, srvName, counters)
	{
		// Get what Type we want to view: ABS, DIFF or RATE
		var counterData = counters.rateCounters;
		var selectedCounterType = document.querySelector('input[name="active-statements-counter-type"]:checked').value;
//console.log('buildActiveStatementDiv(): selectedCounterType: ' + selectedCounterType);
		if      (selectedCounterType === 'abs' ) counterData = counters.absCounters;
		else if (selectedCounterType === 'diff') counterData = counters.diffCounters;
		else if (selectedCounterType === 'rate') counterData = counters.rateCounters;
		else console.log('buildActiveStatementDiv(): Unknown selectedCounterType: ' + selectedCounterType);
		
		// Set MetaData
		var metaDataArr = counters.metaData;

		console.log("DEBUG: buildActiveStatementDiv() appName='"+appName+"', srvName='"+srvName+"', counterData.length="+counterData.length);

		// Create a CALLBACK function to set TD/CELL Colors & formatted values
		var tdCallback = function(td, metaData, cellContent, rowData)
		{
			// Truncate long string cells up-front — full content accessible via row-click modal
			var AS_TRUNC = 60;
			if (typeof cellContent === 'string' && cellContent.length > AS_TRUNC)
			{
				var plain = cellContent.replace(/<[^>]+>/g, '').trim();
				td.textContent = plain.substring(0, AS_TRUNC) + '\u2026';
				td.title = plain.substring(0, 300);
			}

			if (isNaN(cellContent)) // Typically STRING
			{
				// Set to ABS
				td.className = "active-statement-cell-abs";
			}
			else if (typeof(cellContent) === typeof(true)) // BOOLEAN ... isNaN(true) is FALSE
			{
				// Translate booleans into checkboxes... true=[x], false=[ ]
//				if (false === cellContent) td.innerHTML = "&#x2610;";
//				if (true  === cellContent) td.innerHTML = "&#x2611;";

				if (false === cellContent) { td.innerHTML = ""; td.className = "image-unchecked"; }
				if (true  === cellContent) { td.innerHTML = ""; td.className = "image-checked"; }

			}
			else // I guss this must be a NUMBER
			{
				// check if this is a DIFF/RATE or PCT column
				if (metaData !== undefined && metaData.hasOwnProperty('isDiffColumn') && metaData.isDiffColumn === true) 
				{
					if (selectedCounterType === 'abs') 
					{
						if (cellContent !== null)
							td.innerHTML = cellContent.toLocaleString(undefined);
						td.className = "active-statement-cell-abs";
					}

					if (selectedCounterType === 'diff') 
					{
						if (cellContent !== null)
							td.innerHTML = cellContent.toLocaleString(undefined);
						td.className = "active-statement-cell-diff";

						if (cellContent != 0)
							td.style.fontWeight = "bold";
					}

					if (selectedCounterType === 'rate') 
					{
						if (cellContent !== null)
							td.innerHTML = cellContent.toLocaleString(undefined, {minimumFractionDigits: 1, maximumFractionDigits: 1});
						td.className = "active-statement-cell-rate";

						if (cellContent != 0)
							td.style.fontWeight = "bold";
					}
				}
				else if (metaData !== undefined && metaData.hasOwnProperty('isPctColumn') && metaData.isPctColumn === true) 
				{
					if (cellContent !== null)
						td.innerHTML = cellContent.toLocaleString(undefined);
					td.className = "active-statement-cell-pct";

					if (cellContent != 0)
						td.style.fontWeight = "bold";
				}
				else 
				{
					if (cellContent !== null)
						td.innerHTML = cellContent.toLocaleString(undefined);
					td.className = "active-statement-cell-abs";
				}

				// Highlight some columns (for some DbxTune collectors)
				if (metaData !== undefined && metaData.hasOwnProperty('columnName'))
				{
					if ("SqlServerTune" === appName)
					{
						if (metaData.columnName === "dop")
						{
							if (cellContent > 1)
								td.style.backgroundColor = "green";
						}
					}
				}
			}
			
			// Possibly fill in Tooltop for SQL Text etc
			if (metaData !== undefined && metaData.hasOwnProperty('columnName'))
			{
				if ("AseTune" === appName)
				{
					if (metaData.columnName === "HasMonSqlText"       && rowData.hasOwnProperty('MonSqlText')       && cellContent === true) { td.appendChild( createActiveStatementToolTipDiv(rowData.MonSqlText,      'tsql') ); }
					if (metaData.columnName === "HasDbccSqlText"      && rowData.hasOwnProperty('DbccSqlText')      && cellContent === true) { td.appendChild( createActiveStatementToolTipDiv(rowData.DbccSqlText,     'tsql') ); }
					if (metaData.columnName === "HasProcCallStack"    && rowData.hasOwnProperty('ProcCallStack')    && cellContent === true) { td.appendChild( createActiveStatementToolTipDiv(rowData.ProcCallStack,   'text') ); }
					if (metaData.columnName === "HasShowPlan"         && rowData.hasOwnProperty('ShowPlanText')     && cellContent === true) { td.appendChild( createActiveStatementToolTipDiv(rowData.ShowPlanText,    'text') ); }
					if (metaData.columnName === "HasStackTrace"       && rowData.hasOwnProperty('DbccStacktrace')   && cellContent === true) { td.appendChild( createActiveStatementToolTipDiv(rowData.DbccStacktrace,  'text') ); }
					if (metaData.columnName === "HasCachedPlanInXml"  && rowData.hasOwnProperty('CachedPlanInXml')  && cellContent === true) { td.appendChild( createActiveStatementToolTipDiv(rowData.CachedPlanInXml, 'xml' ) ); }
					if (metaData.columnName === "HasSpidLocks"        && rowData.hasOwnProperty('SpidLocks')        && cellContent === true) { td.appendChild( createLockTableToolTipDiv(      rowData.SpidLocks,       'Lock Table'       ) ); }
					if (metaData.columnName === "HasBlockedSpidsInfo" && rowData.hasOwnProperty('BlockedSpidsInfo') && cellContent === true) { td.appendChild( createLockTableToolTipDiv(      rowData.BlockedSpidsInfo,'Blocked SPID Info') ); }
					if (metaData.columnName === "HasLastKnownSqlText" && rowData.hasOwnProperty('LastKnownSqlText') && cellContent === true) { td.appendChild( createActiveStatementToolTipDiv(rowData.LastKnownSqlText,'tsql'             ) ); }
				}
				else if ("SqlServerTune" === appName && metaData !== undefined && metaData.hasOwnProperty('columnName'))
				{
					if (metaData.columnName === "HasBufferSqlText"    && rowData.hasOwnProperty('LastBufferSqlText') && cellContent === true) { td.appendChild( createActiveStatementToolTipDiv(   rowData.LastBufferSqlText, 'tsql') ); }
					if (metaData.columnName === "HasSqlText"          && rowData.hasOwnProperty('lastKnownSql')      && cellContent === true) { td.appendChild( createActiveStatementToolTipDiv(   rowData.lastKnownSql     , 'tsql') ); }
					if (metaData.columnName === "HasQueryplan"        && rowData.hasOwnProperty('query_plan')        && cellContent === true) { td.appendChild( createSqlServerQueryPlanToolTipDiv(rowData.query_plan       , rowData ) ); }
					if (metaData.columnName === "HasLiveQueryplan"    && rowData.hasOwnProperty('LiveQueryPlan')     && cellContent === true) { td.appendChild( createSqlServerQueryPlanToolTipDiv(rowData.LiveQueryPlan    , rowData ) ); }
					if (metaData.columnName === "HasSpidLocks"        && rowData.hasOwnProperty('SpidLocks')         && cellContent === true) { td.appendChild( createLockTableToolTipDiv(         rowData.SpidLocks       ,'Lock Table'       ) ); }
					if (metaData.columnName === "HasBlockedSpidsInfo" && rowData.hasOwnProperty('BlockedSpidsInfo')  && cellContent === true) { td.appendChild( createLockTableToolTipDiv(         rowData.BlockedSpidsInfo,'Blocked SPID Info') ); }
					if (metaData.columnName === "HasSpidWaitInfo"     && rowData.hasOwnProperty('SpidWaitInfo')      && cellContent === true) { td.appendChild( createLockTableToolTipDiv(         rowData.SpidWaitInfo    ,'SPID Wait Info'   ) ); }
				}
				else if ("PostgresTune" === appName && metaData !== undefined && metaData.hasOwnProperty('columnName'))
				{
					if (metaData.columnName === "has_sql_text"          && rowData.hasOwnProperty('last_known_sql_statement') && cellContent === true) { td.appendChild( createActiveStatementToolTipDiv(rowData.last_known_sql_statement, 'postgresql') ); }
					if (metaData.columnName === "has_query_plan"        && rowData.hasOwnProperty('query_plan')               && cellContent === true) { td.appendChild( createPostgresQueryPlanToolTipDiv(rowData.query_plan) ); }
					if (metaData.columnName === "has_pid_lock_info"     && rowData.hasOwnProperty('pid_lock_info')            && cellContent === true) { td.appendChild( createLockTableToolTipDiv(rowData.pid_lock_info, 'PID Lock Info') ); }
					if (metaData.columnName === "has_blocked_pids_info"                                                       && cellContent === true) { td.appendChild( createLockTableToolTipDiv(rowData.pid_lock_info, 'PID Lock Info') ); }
				}
			} // end: metaData && columnName
		};

		// Create a CALLBACK function to set TableRow Colors
		// This is done per: DbxTune Collector
		var trCallback = "";
		var tooltip = "";
		if ("AseTune" === appName)
		{
			trCallback = function(tr, row)
			{
//				console.log("AseTune:trCallback(): row:", row)

				// multiSampled
				if (row.hasOwnProperty('multiSampled') && row.multiSampled !== '')
					tr.className = "active-statement-row-multi-sampled";

				// HOLDING-LOCKS
				if (row.hasOwnProperty('monSource') && row.monSource === 'HOLDING-LOCKS')
					tr.className = "active-statement-row-holding-locks-while-idle";
				
				// Blocked by some spid
				if (row.hasOwnProperty('BlockingSPID') && row.BlockingSPID !== 0)
					tr.className = "active-statement-row-blocked";
				
				// Blocking OTHER Spids
				if (row.hasOwnProperty('BlockingOtherSpids') && row.BlockingOtherSpids !== '')
					tr.className = "active-statement-row-blocking";
				if (row.hasOwnProperty('monSource') && row.monSource === 'BLOCKER')
					tr.className = "active-statement-row-blocking";
			};
			tooltip = "Background colors: \n"
			        + " * ORANGE: Multi Sampled Statement (has been running for more than 1 sample) \n"
			        + " * YELLOW: Holding locks, while client has control (poor transaction control) \n"
			        + " * PINK:   Blocked by some other SPID \n"
			        + " * RED:    BLOCKING other spid's from working.\n"
					+ "";
		}
		else if ("SqlServerTune" === appName)
		{
			trCallback = function(tr, row)
			{
//				console.log("SqlServerTune:trCallback(): row:", row)

				// multiSampled
				if (row.hasOwnProperty('multiSampled') && row.multiSampled !== '')
					tr.className = "active-statement-row-multi-sampled";

				// Waiting-MemoryGrant
				if (row.hasOwnProperty('memory_grant_wait_time_ms') && row.memory_grant_wait_time_ms !== 0)
					tr.className = "active-statement-row-waiting-for-memory-grant";
				
				// HOLDING-LOCKS
				if (row.hasOwnProperty('monSource') && row.monSource === 'HOLDING-LOCKS')
					tr.className = "active-statement-row-holding-locks-while-idle";
				
				// Blocked by some spid
				if (row.hasOwnProperty('ImBlockedBySessionId') && row.ImBlockedBySessionId !== 0)
					tr.className = "active-statement-row-blocked";
				
				// Blocking OTHER Spids
				if (   row.hasOwnProperty('ImBlockingOtherSessionIds') && row.ImBlockingOtherSessionIds !== ''
				    && row.hasOwnProperty('ImBlockedBySessionId')      && row.ImBlockedBySessionId      === 0 )
				{
					tr.className = "active-statement-row-blocking";
				}
			};
			tooltip = "Background colors: \n"
			        + " * ORANGE:     Multi Sampled Statement (has been running for more than 1 sample) \n"
			        + " * LIGHT_BLUE: Waiting for a memory grant \n"
			        + " * YELLOW:     Holding locks, while client has control (poor transaction control) \n"
			        + " * PINK:       Blocked by some other SPID \n"
			        + " * RED:        BLOCKING other spid's from working.\n"
					+ "";
		}
		else if ("PostgresTune" === appName)
		{
			// FIXME: both in the CmActiveStatementsPanel and in here
			trCallback = function(tr, row)
			{
//				console.log("SqlServerTune:trCallback(): row:", row)

				// idle in transaction
				if (row.hasOwnProperty('state') && row.state === 'idle in transaction')
					tr.className = "active-statement-row-holding-locks-while-idle";

				// multiSampled
				if (row.hasOwnProperty('multi_sampled') && row.multi_sampled !== '')
					tr.className = "active-statement-row-multi-sampled";

				// Blocked by some spid
				if (row.hasOwnProperty('im_blocked_by_pids') && row.im_blocked_by_pids !== "")
					tr.className = "active-statement-row-blocked";
				
				// Blocking OTHER Spids
				if (   row.hasOwnProperty('im_blocking_other_pids') && row.im_blocking_other_pids !== ''
				    && row.hasOwnProperty('im_blocked_by_pids')     && row.im_blocked_by_pids     === '' )
				{
					tr.className = "active-statement-row-blocking";
				}
			};
			tooltip = "Background colors: \n"
			        + " * YELLOW: In Transaction, while client has control (poor transaction control) \n"
			        + " * ORANGE: Multi Sampled Statement (has been running for more than 1 sample) \n"
			        + " * PINK:   Blocked by some other session(s) \n"
			        + " * RED:    BLOCKING other session(s) from working.\n"
					+ "";
		}
		else
		{
			console.log("WARNING: buildActiveStatementDiv(): Unknown appName='" + appName + "', no trCallback function will be used.");
		}


		// Wrap the appName-specific trCallback to add click-to-detail modal behaviour
		// (one place — applies to AseTune, SqlServerTune, PostgresTune variants uniformly)
		var _origTrCallback = trCallback;
		var _metaDataArrRef = metaDataArr; // capture for closure
		trCallback = function(tr, row) {
			if (typeof _origTrCallback === 'function') _origTrCallback(tr, row);
			tr.style.cursor = 'pointer';
			tr.title = 'Click to view full row details';
			var rowSnap = row;
			tr.addEventListener('click', function(e) {
				if ($(e.target).closest('[data-toggle="modal"]').length > 0) return;
				activeStmtDetailShowModal(rowSnap, _metaDataArrRef, false);
			});
		};

		//-----------------------------------------------------------
		// FILTER out rows that has a "to short execution time"
		let filteredCounterData = counterData;
//		let execTimeInMs = -1;
//		let execTimeDiv = document.getElementById("active-statements-execTime-txt");
//		if (execTimeDiv)
//			execTimeInMs = execTimeDiv.value;
//
//		// 0 or Negative number == NO Filtering
//		if (execTimeInMs > 0)
//		{
//			// Different AppNames has different Column Names
//			if ("AseTune" === appName)
//			{
//				// Column name is AseTune is 'ExecTimeInMs'
//				filteredCounterData = counterData.filter(row => row.ExecTimeInMs > execTimeInMs);
//			}
//			else if ("SqlServerTune" === appName)
//			{
//				// Column name is SqlServerTune is 'ExecTimeInMs'
//				filteredCounterData = counterData.filter(row => row.ExecTimeInMs > execTimeInMs);
//			}
//			else if ("PostgresTune" === appName)
//			{
//				// Column name is PostgresTune is 'execTimeInMs'
//				filteredCounterData = counterData.filter(row => row.execTimeInMs > execTimeInMs);
//			}
//		}

//		var stmntSrvTabClasses = document.getElementsByClassName('active-statements-srv-tab-class');
//		for (var i=0; i< stmntSrvTabClasses.length; i++ ) {
//			console.log("stmntSrvTabClasses["+i+"].id=" + stmntSrvTabClasses[i].id + ", scrollLeft=" + stmntSrvTabClasses[i].scrollLeft );
//		}

		// NOTE: Maybe we should move this to "outside" the call to createActiveStatementToolTipDiv() ... 
		//       meaning in function: setActiveStatement()
		//   OR: listen on 'scroll' events to save the value into the 'localStorage' see: https://developer.mozilla.org/en-US/docs/Web/API/Document/scroll_event
		var srvTabScrollLeft = -1;
		if (localStorage.getItem('active-statements-srv-tab-' + srvName + '-scrollLeft') != null)
		{
			srvTabScrollLeft = localStorage.getItem('active-statements-srv-tab-' + srvName + '-scrollLeft');
			if (document.getElementById('active-statements-srv-tab-' + srvName))
				srvTabScrollLeft = document.getElementById('active-statements-srv-tab-' + srvName).scrollLeft;
		}

		if (srvTabScrollLeft > 0)
			localStorage.setItem('active-statements-srv-tab-' + srvName + '-scrollLeft', srvTabScrollLeft);
//console.log("active-statements-srv-tab-" + srvName + "... srvTabScrollLeft=" + srvTabScrollLeft);

		// create a new SrvDiv 
		var newSrvDiv = document.createElement("div");
		newSrvDiv.setAttribute("id",    "active-statements-srv-" + srvName);
		newSrvDiv.setAttribute("class", "active-statements-srv-class");
		newSrvDiv.setAttribute("title", tooltip);

		
		// "add" a property 'statementsRowCount' to a div the we read later.
		newSrvDiv.statementsRowCount = filteredCounterData.length;
		
		var newSrvInfoDiv = document.createElement("div");
		newSrvInfoDiv.innerHTML = "<b>" + filteredCounterData.length +"</b> Active Statements on server: <b>" + srvName + "</b>";
		newSrvInfoDiv.setAttribute("class", "active-statements-srv-info-class");

		
		// Create a table and add it to 'newSrvDiv'
		let tab = jsonToTable(filteredCounterData, false, trCallback, tdCallback, metaDataArr);
		tab.style.fontSize = '0.78em';
		tab.style.whiteSpace = 'nowrap';

		var newSrvTabDiv = document.createElement("div");
		newSrvTabDiv.id               = "active-statements-srv-tab-" + srvName;
		newSrvTabDiv.class            = "active-statements-srv-tab-class";
		newSrvTabDiv.style.overflowX  = "scroll";
//		newSrvTabDiv.scrollLeft       = srvTabScrollLeft;
		newSrvTabDiv.appendChild(tab);

		// On scroll -- save 'scrollLeft' in localStorage (so it can be restored when the popup is rebuilt)
		newSrvTabDiv.addEventListener("scroll", function(e)
		{
			setTimeout(function(ev) 
			{
				//console.log("DEBUG: saving scrollLeft=" + newSrvTabDiv.scrollLeft + " into localStorage, for 'active-statements-srv-tab-" + srvName + "'.");
				localStorage.setItem('active-statements-srv-tab-' + srvName + '-scrollLeft', newSrvTabDiv.scrollLeft);
			}, 500);
		});

		// NOTE 1: Since data in the TAB is "unknown" or not yet rendered... We need to postpone this to "later"
		//         This will probably make the screen to "flicker" a bit
		// NOTE 2: We might want to save this setting in the browsers storage... otherwise we need to scroll to XXX everytime the row is "new" (or vanishes and re-apear)
		//         localStorage.getItem("name"),  localStorage.setItem("name")
		if (srvTabScrollLeft > 0)
			setTimeout(function(){ newSrvTabDiv.scrollLeft = srvTabScrollLeft; }, 1);


		// Add stuff to the 'newSrvDiv'
		newSrvDiv.appendChild(document.createElement("br"));
		newSrvDiv.appendChild(newSrvInfoDiv);
		newSrvDiv.appendChild(newSrvTabDiv);
//		newSrvDiv.appendChild(tab);
//		newSrvDiv.appendChild(document.createElement("br"));

		// Set this LATE seems like we dont need to do above: setTimeout(...)
		if (srvTabScrollLeft > 0)
			newSrvTabDiv.scrollLeft = srvTabScrollLeft;

		return newSrvDiv;
	}

	function setActiveStatement(allEntries)
	{
		if ( document.getElementById("active-statements-paused-chk").checked )
		{
			document.getElementById("active-statements-watermark").innerHTML = "PAUSED";
			console.log("DEBUG: setActiveStatement() in PAUSED mode...");
			return;
		}
		// Set the Watermark
		document.getElementById("active-statements-watermark").innerHTML = "";

		if (allEntries === undefined)
		{
			console.log("DEBUG: setActiveStatement() parameter 'allEntries' is not passed, exiting...");
			return;
		}

		// push 'active statements' for each server on this array
		var activeStatementsDivArr = [];

		// Keep count on TOTAL active statements (in all servers)
		var totalActiveStatementsCount = 0;


		// Loop all entries
		// Build a "div" foreach of the servers that has active statements
		// One entry looks like:
		//    { appName: xxx, srvName: xxx, cmNames: [ { cmName: CmActiveStatements, lastSample: { counters: { metaData:[], absCounters: [{row},{row}], diffCounters: [{row},{row}], rateCounters: [{row},{row}] } } }... ]
		for(let e=0; e<allEntries.length; e++)
		{
			var entry = allEntries[e];
			console.log("DEBUG: CmActiveStatements--entry: "+entry, entry);

			var appName = entry.appName;
			var srvName = entry.srvName;
			
			var counters = {};
			for(let c=0; c<entry.cmNames.length; c++)
			{
				var cmNamesEntry = entry.cmNames[c];
					
				if (cmNamesEntry.cmName === "CmActiveStatements")
					counters = cmNamesEntry.lastSample.counters;
			}
			
			var newSrvDiv = buildActiveStatementDiv(appName, srvName, counters);
			
			// Push the new div on the array, which will be read later
			activeStatementsDivArr.push(newSrvDiv);
		}

		// Emty the Statements Window
		document.getElementById("active-statements-win").innerHTML = "";

		// Finally add the 'activeStatementsDivArr' to the outer div
		var activeStatementsWin = document.getElementById("active-statements-win");
		for (let i=0; i<activeStatementsDivArr.length; i++) 
		{
			const srvDiv = activeStatementsDivArr[i];
			
			// Add SrvDiv to WinDiv
			activeStatementsWin.appendChild( srvDiv );

			// Read "user defined" name 'statementsRowCount' from the SrvDiv
			totalActiveStatementsCount += activeStatementsDivArr[i].statementsRowCount;
		}


		// Re-apply dark mode after every render — check checkbox state OR saved preference
		var _asDarkChk = document.getElementById('active-statements-dark-chk');
		var _asDarkOn  = (_asDarkChk && _asDarkChk.checked)
		              || (getStorage('dbxtune_checkboxes_').get('active-statements-dark-chk') === 'checked');
		if (_asDarkOn) $('#active-statements').addClass('as-dark');

		// Set count at the top of the poupup window
		$("#active-statements-count").html(totalActiveStatementsCount);

		// Floating button: update badge count and blink when active
		if ( totalActiveStatementsCount > 0 )
		{
			$("#active-stmt-count").text(totalActiveStatementsCount).show();
			$("#active-statements-btn").css("animation", "blink 1.5s infinite");
			$("#active-statements-btn").css("background", "rgba(229, 228, 226, 0.5)");
		}
		else
		{
			// Reset floating button: active-statements-btn
			$("#active-stmt-count").text("0").hide();
			$("#active-statements-btn").css("animation", "");
			$("#active-statements-btn").css("background", "rgba(32, 32, 32, 0.5)");
		}

		// SHOW all Statements div
		if ( document.getElementById("active-statements-auto-open-chk").checked )
		{
			if ( totalActiveStatementsCount > 0 )
				$("#active-statements").css("display", "block"); // show
			else
				$("#active-statements").css("display", "none"); // hide
		}
	}

	function setHistoryStatement(allEntries)
	{
//		console.log("DEBUG: setHistoryStatement(): allEntries=" + allEntries, allEntries);
		console.log("DEBUG: setHistoryStatement(): allEntries.size=" + allEntries.length);
		// This should more or less be the same as: setActiveStatement()
		// Most possibly reuse the same modal dialog
		setActiveStatement(allEntries);
	}


	function dbxTuneSetTimeLineMarkerForAllGraphs(ts, momentArrayPos)
	{
		console.log("dbxTuneSetTimeLineMarkerForAllGraphs(ts=|" + ts + "|, momentArrayPos=" + momentArrayPos + ")");

		for(var i=0; i<_graphMap.length; i++)
		{
			const dbxGraph = _graphMap[i];

//			console.log("dbxTuneSetTimeLineMarkerForAllGraphs(ts=|" + ts + "|, momentArrayPos=" + momentArrayPos + ") i=" + i +", isInViewport=" + dbxGraph.isInViewport());

			// It takes to long to update ALL graphs (the UI feels "slugish", so just update the ones that are visible)
			// So: if dbxGraph is NOT visible "on screen".... then it just SETS the timeline (and do not update the GUI)
			// Then when the graph comes "into view" the UI update will happen
			// The above async behaviour is done by: https://developer.mozilla.org/en-US/docs/Web/API/Intersection_Observer_API
			dbxGraph.setTimelineMarker(ts);
		}
	}

//--------------------------------------------------------------------
//--------------------------------------------------------------------
// Class: DbxGraph
//--------------------------------------------------------------------
//--------------------------------------------------------------------
class DbxGraph
{
	constructor(outerChartDiv, serverName, cmName, graphName, graphLabel, graphProps, graphCategory, 
		isPercentGraph, graphLineProps, subscribeAgeInSec, gheight, gwidth, debug, 
		initStartTime, initEndTime, initSampleType, initSampleValue, isMultiDayChart,
		markStartTime, markEndTime)
	{
		// Properties/fields
		this._serverName        = serverName;
		this._cmName            = cmName;
		this._graphName         = graphName;
		this._fullName          = cmName + "_" + graphName;
		this._graphLabel        = graphLabel;
		this._graphProps        = graphProps;
		this._graphCategory     = graphCategory;
		this._isPercentGraph    = isPercentGraph;
		this._subscribeAgeInSec = subscribeAgeInSec;
		this._graphLineProps    = graphLineProps;
		this._debug             = debug;

		this._initStartTime     = initStartTime;
		this._initEndTime       = initEndTime;
		this._initSampleType    = initSampleType;
		this._initSampleValue   = initSampleValue;
		this._isMultiDayChart   = isMultiDayChart;

		this._markStartTime     = markStartTime;
		this._markEndTime       = markEndTime;
		
		// this._graphHeight = 150;
		this._graphHeight = 200;
		this._graphHeight = gheight;
		this._graphWidth  = gwidth;

		// Set the DEFAULT yAxes Label units
		this._yAxisScaleName   = "default";
		this._yAxisScaleLabels = ["", " K", " M", " G", " T"];

		this._series = [];
		this._initialized = false;

		if (typeof this._graphLineProps !== 'object')
			this._graphLineProps = {};

		this._timelineMarker = undefined;

		this._annotations = [];

		if (_debug > 0)
			console.log("Creating DbxGraph: " 
				+   "_serverName       ='" + this._serverName        + "'\n"
				+ ", _cmName           ='" + this._cmName            + "'\n"
				+ ", _graphName        ='" + this._graphName         + "'\n"
				+ ", _fullName         ='" + this._fullName          + "'\n"
				+ ", _graphLabel       ='" + this._graphLabel        + "'\n"
				+ ", _graphProps       ='" + this._graphProps        + "'\n"
				+ ", _graphCategory    ='" + this._graphCategory     + "'\n"
				+ ", _isPercentGraph   ='" + this._isPercentGraph    + "'\n"
				+ ", _graphLineProps   ='" + this._graphLineProps    + "'\n"
				+ ", _subscribeAgeInSec='" + this._subscribeAgeInSec + "'\n"
				+ ", _debug            ='" + this._debug             + "'\n"

				+ ", _initStartTime    ='" + this._initStartTime     + "'\n"
				+ ", _initEndTime      ='" + this._initEndTime       + "'\n"
				+ ", _initSampleType   ='" + this._initSampleType    + "'\n"
				+ ", _initSampleValue  ='" + this._initSampleValue   + "'\n"
				+ ", _isMultiDayChart  ='" + this._isMultiDayChart   + "'\n"
				
				+ ", _markStartTime    ='" + this._markStartTime     + "'\n"
				+ ", _markEndTime      ='" + this._markEndTime       + "'\n"
		);
				

		// get _yAxisScaleLabels from 'graphProps', which is a JSON String looking like
		// { "yAxisScaleLabels" : { "name":"normal", "div":"1000", "s0" : "", "s1" : " K",  "s2" : " M",  "s3" : " G", "s4" : " T"} }
		try {
			var tmp = JSON.parse(this._graphProps);

			this._yAxisScaleName = tmp.yAxisScaleLabels.name;

			this._yAxisScaleLabels = [];
			this._yAxisScaleLabels.push(tmp.yAxisScaleLabels.s0);
			this._yAxisScaleLabels.push(tmp.yAxisScaleLabels.s1);
			this._yAxisScaleLabels.push(tmp.yAxisScaleLabels.s2);
			this._yAxisScaleLabels.push(tmp.yAxisScaleLabels.s3);
			this._yAxisScaleLabels.push(tmp.yAxisScaleLabels.s4);

			if (_debug > 0)
				console.log("DbxGraph: " + this._fullName + ", _yAxisScaleLabels="+this._yAxisScaleLabels, this._yAxisScaleLabels);

			// NOTE: Should we look at "autoOverflow" ...
			// NOTE: Not sure if 'sampleType' and 'sampleValue' are valid anymore... but keep it for now...

			// Get defaults (if not specified with initSampleType && initSampleValue)
			if (this._initSampleType === "" && tmp.hasOwnProperty("sampleType")) {
				this._initSampleType = tmp.sampleType;
				console.log(">>>>>>>>>>>>>>>>>> srvName=" + this._serverName + ", cmName=" + this._cmName + ", graphName=" + this._graphName + ">>>> Using sampleType=|" + this._initSampleType + "|", tmp);
			}
			if (this._initSampleValue === "" && tmp.hasOwnProperty("sampleValue")) {
				this._initSampleValue = tmp.sampleValue;
				console.log(">>>>>>>>>>>>>>>>>> srvName=" + this._serverName + ", cmName=" + this._cmName + ", graphName=" + this._graphName + ">>>> Using sampleValue=|" + this._initSampleValue + "|", tmp);
			}
		} catch(e) {
			console.log("Creating DbxGraph: problems parsing 'graphProps' = '" + this._graphProps + "'. Caught: " + e, e);
		}
		

		
		let chartLabel = this._graphLabel + " ::: [" + this._serverName + "]";
		if ( this._isMultiDayChart )
		{
			// this will fail, if the startDate is '2h'... so redo this...
			let lblDateStr = moment(this._initStartTime).format("ddd YYYY-MM-DD");

			this._initStartTime
			//chartLabel = this._graphLabel + " ::: [" + this._serverName + "] at [" + lblDateStr + "]";
			chartLabel = this._graphLabel + " ::: [" + this._serverName + "] @ " + lblDateStr;
		}

		var thisDbxChart = this;

		this._chartConfig = 
		{
			type: 'line',
			data: {
				labels: [],
				datasets: []
			},        
			plugins: [{
				afterDraw: function() {
					//console.log("afterDraw(): cmName='"+cmName+"', graphName='"+graphName+"'.");
					thisDbxChart.checkForNoDataInChart();
				}
			}],
			options: {
				responsive: true,
				maintainAspectRatio: false,
				title: {
					display: true,
					fontSize: 16,
//					text: this._graphLabel
//					text: this._cmName + ":" + this._graphName + " ##### " + this._graphLabel
//					text: this._graphLabel + " ::: [" + this._serverName + "] " + this._cmName + ":" + this._graphName
//					text: this._graphLabel + " ::: [" + this._serverName + "]"
					text: chartLabel
				},
				legend: {
					position: 'bottom',
					//fullWidth: false, // Marks that this box should take the full width of the canvas (pushing down other boxes). This is unlikely to need to be changed in day-to-day use. DEFAULT=true
					labels: {
						boxWidth: 10, // width of coloured box
						fontSize: 10, // font size of text, default=12
						// fontColor: 'rgb(255, 99, 132)'
						// generateLabels: function(chart) // the below is 
						// {
						// 	var data = chart.data;
						// 	if (data.labels.length && data.datasets.length) 
						// 	{
						// 		// Somewhere here I could count the labels, get there width... and if to wide "shop off" the last part of the string and say "xxxx..." (elipse at the end)
						// 		console.log("data.datasets.length="+data.datasets.length);
						// 		console.log("data.datasets="+data.datasets);
						// 		
						// 		return data.datasets.map(function(dataset, i)
						// 		{
						// 			return {
						// 				text: "xxx:"+dataset.label,
						// 				fillStyle: dataset.backgroundColor,
						// 				hidden: !chart.isDatasetVisible(i),
						// 				lineCap: dataset.borderCapStyle,
						// 				lineDash: dataset.borderDash,
						// 				lineDashOffset: dataset.borderDashOffset,
						// 				lineJoin: dataset.borderJoinStyle,
						// 				lineWidth: dataset.borderWidth,
						// 				strokeStyle: dataset.borderColor,
						// 				// Below is extra data used for toggling the datasets
						// 				datasetIndex: i
						// 			};
						// 		}, this)
						// 	}
						// 	return [];
						// }
					},
					onClick: function(e, legendItem) 
					{
						if ( ! e.ctrlKey )
						{
							// ORIGINAL CODE -- Toggle Current 
							var index = legendItem.datasetIndex;
							var ci = this.chart;
							var meta = ci.getDatasetMeta(index);

							// See controller.isDatasetVisible comment
							meta.hidden = meta.hidden === null ? !ci.data.datasets[index].hidden : null;

							// We hid a dataset ... rerender the chart
							ci.update();
						}
						else
						{
							// CTRL -- Toggle Everything else than the current
							var index = legendItem.datasetIndex;
							var ci = this.chart;
							var alreadyHidden = (ci.getDatasetMeta(index).hidden === null) ? false : ci.getDatasetMeta(index).hidden;

							ci.data.datasets.forEach(function(e, i) 
							{
								var meta = ci.getDatasetMeta(i);

								if (i !== index) 
								{
									if (!alreadyHidden) 
									{
										meta.hidden = meta.hidden === null ? !meta.hidden : null;
									} else if (meta.hidden === null) 
									{
										meta.hidden = true;
									}
								} 
								else if (i === index) 
								{
									meta.hidden = null;
								}
							});

							ci.update();
						}
					},
				}, // end: legend
				scales: {
					xAxes: [{
						id: 'x-axis-0', // Assign an ID to the X-axis
						type: 'time',
						distribution: 'linear',
						tooltipFormat: 'YYYY-MM-DD (dddd) HH:mm:ss', // This didn't work
						time: {
							displayFormats: {
								second: 'HH:mm:ss',
								minute: 'HH:mm:ss',
								hour:   'HH:mm',
							}
						},
						ticks: {
							beginAtZero: true,  // if true, scale will include 0 if it is not already included.
						},
						gridLines: {
							color: 'rgba(0, 0, 0, 0.1)',
							zeroLineColor: 'rgba(0, 0, 0, 0.25)'
						},
					}],
					yAxes: [{
						id: 'y-axis-0', // Assign an ID to the Y-axis
						ticks: {
							beginAtZero: true,  // if true, scale will include 0 if it is not already included.
							callback: function(value, index, values) 
							{
								// If Percent Graph append a ' %' at the end.
								if ( thisDbxChart._isPercentGraph ) 
								{
									return value + " %";
								}

								var divSize = 1000;
								var units   = thisDbxChart._yAxisScaleLabels;
								
								if (thisDbxChart._yAxisScaleName === "bytes" || thisDbxChart._yAxisScaleName === "kb" || thisDbxChart._yAxisScaleName === "mb" || thisDbxChart._yAxisScaleName === "mbit")
								{
									divSize = 1024;
								}
								
								// TODO: Possibly in the future: if 'seconds', 'millisec', 'microsec' calculate minute/hour/day (right now thet are Ksec/Msec/Gsec)

								// Alter numbers larger than 1k
								if (value >= divSize) 
								{
								//	var units = ["", " K", " M", " G", " T"];

									var order = Math.floor(Math.log(value) / Math.log(divSize));

									// TODO: check more than one label to decide if we should use decimals for all labels or just this one...  
									
									var unitname = units[order];
									//var num = Math.floor(value / 1000 ** order);
									var num = (value / divSize ** order).toFixed(1).replace(/\.0$/, '');

									// output number remainder + unitname
									return num + unitname;
								}

								// return formatted original number
								// Use ChartJS default method for this, otherwise 1.0 till be 1 etc...
								var defaultFormatStr = Chart.Ticks.formatters.linear(value, index, values);

								return defaultFormatStr + units[0];

								//return value.toFixed(1);
								//return value.toFixed(1).replace(/\.0$/, '');
								//return Chart.Ticks.formatters.linear(value, index, values);
								//return this.toLocaleString();
								//return value.toString();
								//return value.toLocaleString();
							}
						},
						gridLines: {
							color: 'rgba(0, 0, 0, 0.1)',
							zeroLineColor: 'rgba(0, 0, 0, 0.25)'
						},
					}],
				}, // end: scales
				// pan: { // Container for pan options
				// 	enabled: true, // Boolean to enable panning
				// 	mode: 'x', // Panning directions. Remove the appropriate direction to disable, Eg. 'y' would only allow panning in the y direction
				// 	drag: true,
				// },
//				zoom: {  // Container for zoom options
//					enabled: true,  // Boolean to enable zooming
//					mode: 'x', // Zooming directions. Remove the appropriate direction to disable, Eg. 'y' would only allow zooming in the y direction
//					drag: true,
//				},
				annotation: {
					// drawTime: 'afterDatasetsDraw',
					drawTime: 'afterDraw',
					// events: ['click'],
					annotations: this._annotations,
//					annotations: {}, // in v3/v4 i think this is an object and not an array
				},
				tooltips: {
					callbacks: {
						label: function (tooltipItems, data) {
							// print numbers localized (typically 1234567.8 -> 1,234,567.8)
							return data.datasets[tooltipItems.datasetIndex].label + ': ' + tooltipItems.yLabel.toLocaleString();
						}
						,title: function (tooltipItems) {
							//console.log('Tooltip.title: ', tooltipItems);
							const date = new Date(tooltipItems[0].xLabel);
							return date.toLocaleString();
						}
					}
				},
			},
			tooltips: {
//				mode: 'dataset',
				mode: 'index',
				intersect: false,
//				mode: 'index',
//				mode: 'point',
//				mode: 'dataset',
//				intersect: true
//					mode: 'nearest',
//					intersect: true
			},
			hover: {
				mode: 'nearest',
				intersect: true
			},
		};

// NOTE: Keep this for v3/v4 -- multiple annotations and 'markedStartEndTime' to highlight important section
//		// Initally mark a important period
//		if ( this._markStartTime !== undefined && this._markEndTime !== undefined)
//		{
//			// We cant call this, since THIS is not fully initialized yet...
//			// this.setMarkerStartEndTime(this._markStartTime, this._markEndTime);
//			
//			// NOTE: If you change the below also chang ein function: this.setMarkerStartEndTime
//			// SET box/marker
//			const markedStartEndTime = {
//				id:   'markedStartEndTime',
//				type: 'box',
//				xMin: this._markStartTime,
//				xMax: this._markEndTime,
//				backgroundColor: 'rgba(128, 128, 128, 0.2)', // Light gray transparent background
//				borderWidth: 0
//			};
//	
//			this._chartConfig.options.plugins.annotation.annotations['markedStartEndTime'] = markedStartEndTime;
//		}
//		// Initally mark a important period
//		if ( this._markStartTime !== undefined && this._markEndTime !== undefined && this._markStartTime !== '' && this._markEndTime !== '')
//		{
//			this._markStartTime = moment(this._markStartTime);
//			this._markEndTime   = moment(this._markEndTime);
//			
//			// We cant call this, since THIS is not fully initialized yet...
//			// this.setMarkerStartEndTime(this._markStartTime, this._markEndTime);
//			
//			// NOTE: If you change the below also chang ein function: this.setMarkerStartEndTime
//			// SET box/marker
//			const markedStartEndTime = {
//				id: 'markedStartEndTime',
//				type: 'box',
//				xMin: this._markStartTime,
//				xMax: this._markEndTime,
////				xMin: 10,
////				xMax: 20,
////				yMin: 10,
////				yMax: 30,
//				borderColor: 'gray',
//				backgroundColor: 'rgba(128, 128, 128, 0.2)', // Light gray transparent background
//				borderWidth: 0
//			};
//	
//			this._annotations.push(markedStartEndTime);
//			this._chartConfig.options.annotation.annotations = this._annotations; // Only in annotations v:0.5.7
//		}
		
		// Set MIN/MAX values when it's a PERCENT Graph
		if (this._debug > 0)
			console.log("GRAPH: "+this._fullName+": this._isPercentGraph="+this._isPercentGraph);

		if (this._isPercentGraph)
		{
			if (this._debug > 0)
				console.log("This is a PERCENT Graph: GraphName="+this._fullName);

			this._chartConfig.options.scales.yAxes[0].ticks.suggestedMax = 100; // Adjustment used when calculating the maximum  data value
			this._chartConfig.options.scales.yAxes[0].ticks.suggestedMin = 0;   // Adjustment used when calculating the minimum data value
	//v3		this._chartConfig.options.scales.y.ticks.suggestedMax = 100; // Adjustment used when calculating the maximum  data value
	//v3		this._chartConfig.options.scales.y.ticks.suggestedMin = 0;   // Adjustment used when calculating the minimum data value
		}
		
		if (_colorSchema === "dark")
		{
//			var chartJsColorScheme = 'brewer.RdYlBu11';
//			this._chartConfig.options.plugins.colorschemes.scheme = chartJsColorScheme;
			
			Chart.defaults.global.defaultFontColor = '#ccc';

			this._chartConfig.options.tooltips.backgroundColor = 'rgba(255, 255, 255, 0.8)';
			this._chartConfig.options.tooltips.titleFontColor  = '#000';
			this._chartConfig.options.tooltips.bodyFontColor   = '#000';
			this._chartConfig.options.scales.xAxes[0].gridLines.color         = 'rgba(255, 255, 255, 0.2)';
			this._chartConfig.options.scales.xAxes[0].gridLines.zeroLineColor = 'rgba(255, 255, 255, 0.5)';
			this._chartConfig.options.scales.yAxes[0].gridLines.color         = 'rgba(255, 255, 255, 0.2)';
			this._chartConfig.options.scales.yAxes[0].gridLines.zeroLineColor = 'rgba(255, 255, 255, 0.5)';
	//v3		this._chartConfig.options.scales.x.grid.color         = 'rgba(255, 255, 255, 0.2)';
	//v3		this._chartConfig.options.scales.x.grid.zeroLineColor = 'rgba(255, 255, 255, 0.5)';
	//v3		this._chartConfig.options.scales.y.grid.color         = 'rgba(255, 255, 255, 0.2)';
	//v3		this._chartConfig.options.scales.y.grid.zeroLineColor = 'rgba(255, 255, 255, 0.5)';

//			Chart.defaults.global.defaultColor = 'rgba(255, 255, 255, 0.2)';
		}

		// Get the outer DIV (where we will insert the graph)
		const _outerChartDiv = document.getElementById(outerChartDiv);
		// _outerChartDiv.style['grid-gap']       = '0px';
		// _outerChartDiv.style['--cols-xl']      = 'auto-fit';
		// _outerChartDiv.style['--cols-size-xl'] = 'minmax('+gwidth+'px, 1fr)';
		// <div id="graphs" class="grid" style="grid-gap: 0px; --cols-xl: auto-fit; --cols-size-xl: minmax(700px, 1fr);">

		const thisClass=this;

		// Create a div for every graph, with ID: cmName_graphName
		// and add it to the outer div
		const newDiv = document.createElement("div");
		newDiv.setAttribute("id", this._fullName);
		newDiv.setAttribute("graph-line-props", this.buildHtmlAttrGraphLineProps());
		newDiv.style.border = "1px dotted gray";
		if (_colorSchema === "dark")
		{
			newDiv.style.background = "#343a40";  // same as color as bootstrap "dark"
			document.body.style.background = "#343a40";  // same as color as bootstrap "dark"
			
			$("#api-feedback").css('color', '#ccc');
		}
		newDiv.style.border = "1px dotted gray";
		newDiv.classList.add("dbx-graph-context-menu"); // add right click menu to the graph, using jQuery contextMenu plugin
		_outerChartDiv.appendChild(newDiv);
		this._chartDiv = newDiv;

		// add a Horizontal Ruler
		// newDiv.appendChild(document.createElement("hr"));

		// Create a canvas for every graph in above div
		const newCanvas = document.createElement("canvas");
		newCanvas.setAttribute("id", "canvas_" + this._fullName);
		newCanvas.style.display = "block";
		// newCanvas.height = this._graphHeight;
		newDiv.appendChild(newCanvas);
		this._canvas = newCanvas;

		// Create a "feedback area" for every graph in above div
		const newFeedback = document.createElement("div");
		newFeedback.setAttribute("id", "feedback_" + this._fullName);
		newFeedback.style.display = "block";
		//newCanvas.height = this._graphHeight;
		newDiv.appendChild(newFeedback);
		this._chartFeedback = newFeedback;

		// Create the Chart (for now using Chart.js)
		const ctx = newCanvas.getContext("2d");
		ctx.canvas.height = this._graphHeight;
		this._chartObject = new Chart(ctx, this._chartConfig);
		this._canvasCtx = ctx;

		// Enable/disable some specififc "lines" 
		
		
		//---------------------------------------------------------------------------------------------------------
		// on right click: 
		//---------------------------------------------------------------------------------------------------------
		try {
			$.contextMenu({
				selector: '.dbx-graph-context-menu',
				items: {
					// test: {
					// 	name: "Test",
					// 	callback: function(key, opt) 
					// 	{
					// 		console.log("Clicked on:'"+key+"', opt='"+opt+"', this='"+this+"'.", this, opt);
					// 		alert("Clicked on:'"+key+", opt='"+opt+"'.");
					// 	}
					// },
					resetZoomThis: {
						name: "Reset - This graph",
						callback: function(key, opt) 
						{
							var graphName = $(this).attr('id');
							console.log("contextMenu(graphName="+graphName+"): key='"+key+"'.");
							getGraphByName(graphName)._chartObject.resetZoom();
						}
					},
					resetZoomAll: {
						name: "Reset - ALL graphs",
						callback: function(key, opt) 
						{
							console.log("contextMenu(graphName="+opt.dbxSourceName+"): key='"+key+"'.");
							for(var i=0; i<_graphMap.length; i++)
							{
								const dbxGraph = _graphMap[i];
								dbxGraph._chartObject.resetZoom();
							}
						}
					},
					setMaxValue: {
						name: "Set Max Value in graph",
						callback: function(key, opt) 
						{
							var graphName = $(this).attr('id');
							const curVal = getGraphByName(graphName).getMaxValue()
							const newVal = prompt("Set a new temporary MAX value (blank is auto):", curVal);
							console.log("contextMenu(graphName="+graphName+"): key='"+key+"'.");
							getGraphByName(graphName).setMaxValue(newVal);
						}
					},
					setMinValue: {
						name: "Set Min Value in graph",
						callback: function(key, opt) 
						{
							var graphName = $(this).attr('id');
							const curVal = getGraphByName(graphName).getMinValue()
							const newVal = prompt("Set a new temporary MIN value (blank is auto):", curVal);
							console.log("contextMenu(graphName="+graphName+"): key='"+key+"'.");
							getGraphByName(graphName).setMinValue(newVal);
						}
					},
					hideThis: {
						name: "Hide this graph",
						callback: function(key, opt) 
						{
							var graphName = $(this).attr('id');
							console.log("contextMenu(graphName="+graphName+"): key='"+key+"'.");
							document.getElementById(graphName).style.display = 'none';
						}
					},
					filterDialog: {
						name: "Open Filter Dialog...",
						callback: function(key, opt) 
						{
							dbxOpenFilterDialog();
							// $("#dbx-filter-dialog").modal('show');
						}
					},

					copyChartChortName: {
						name: "Copy graph name",
						callback: function(key, opt) 
						{
							var graphName = $(this).attr('id');
							console.log("contextMenu(graphName="+graphName+"): key='"+key+"'.");
							
							// ---- below was grabbed from: https://techoverflow.net/2018/03/30/copying-strings-to-the-clipboard-using-pure-javascript/
							// Create new element
							var el = document.createElement('textarea');

							// Set value (string to be copied)
							el.value = graphName;

							// Set non-editable to avoid focus and move outside of view
							el.setAttribute('readonly', '');
							el.style = {position: 'absolute', left: '-9999px'};
							document.body.appendChild(el);

							// Select text inside element
							el.select();

							// Copy text to clipboard
							document.execCommand('copy');

							// Remove temporary element
							document.body.removeChild(el);
						}
					},

					separator1: "-----",

					showThisInNewTab: {
						name: "Open this graph in new Tab",
						callback: function(key, opt) 
						{
							var graphName      = $(this).attr('id');
							var graphLineProps = $(this).attr('graph-line-props');
							var graphListAttr  = graphName + graphLineProps
							console.log("contextMenu(graphName="+graphName+"): key='"+key+"', graphListAttr='"+graphListAttr+"'.");

							// Get serverName this graph is for
							var srvName = getGraphByName(graphName).getServerName();

							// Get Get current URL
							var currentUrl  = window.location.href;
							const url = new URL(currentUrl);
							const params = new URLSearchParams(url.search);

							// Change/set some parameters
							params.set('gcols',       1);
							params.set('sessionName', srvName);
							params.set('graphList',   graphListAttr);

							window.open(`${location.pathname}?${params}`, '_blank');
						}
					},
					showThisInNewTabLast7DaysFullDay: {
						name: "Open this graph (last 7 days, full day) in new Tab",
						callback: function(key, opt) 
						{
							var graphName      = $(this).attr('id');
							var graphLineProps = $(this).attr('graph-line-props');
							var graphListAttr  = graphName + graphLineProps
							console.log("contextMenu(graphName="+graphName+"): key='"+key+"', graphListAttr='"+graphListAttr+"'.");

							// Get serverName this graph is for
							var srvName = getGraphByName(graphName).getServerName();

							// Get Get current URL
							var currentUrl  = window.location.href;
							const url = new URL(currentUrl);
							const params = new URLSearchParams(url.search);

							// Change/set some parameters
							params.set('mdc',         "999");
							params.set('mdcp',        "false");
							params.set('gcols',       1);
							params.set('startTime',   new moment().subtract(6, "days").startOf('day').format('YYYY-MM-DD HH:mm'));
							params.set('endTime',     "24h");
							params.set('sessionName', srvName);
							params.set('graphList',   graphListAttr);

							window.open(`${location.pathname}?${params}`, '_blank');
						}
					},
					showThisInNewTabLast7DaysCurrentPeriod: {
						name: "Open this graph (last 7 days, # time) in new Tab",
						callback: function(key, opt) 
						{
							var graphName      = $(this).attr('id');
							var graphLineProps = $(this).attr('graph-line-props');
							var graphListAttr  = graphName + graphLineProps
							console.log("contextMenu(graphName="+graphName+"): key='"+key+"', graphListAttr='"+graphListAttr+"'.");

							// Get serverName this graph is for
							var srvName = getGraphByName(graphName).getServerName();

							// Get Get current URL
							var currentUrl  = window.location.href;
							const url = new URL(currentUrl);
							const params = new URLSearchParams(url.search);

							// Get current timespan
							var currentHourSpan = parseInt( $("#dbx-sample-time").html().replace("h", "") );
							if (isNaN(currentHourSpan))
								currentHourSpan = 2;
							if (currentHourSpan === 0)
								currentHourSpan = 2;
							if (currentHourSpan > 24)
								currentHourSpan = 24;

							// Change/set some parameters
							params.set('mdc',         "-6");
							params.set('mdcp',        "false");
							params.set('gcols',       1);
							params.set('endTime',     currentHourSpan + "h");
							params.set('sessionName', srvName);
							params.set('graphList',   graphListAttr);

							window.open(`${location.pathname}?${params}`, '_blank');
						}
					},
					showThisInNewTabLast30DaysFullDay: {
						name: "Open this graph (last 30 days, full day) in new Tab",
						callback: function(key, opt) 
						{
							var graphName      = $(this).attr('id');
							var graphLineProps = $(this).attr('graph-line-props');
							var graphListAttr  = graphName + graphLineProps
							console.log("contextMenu(graphName="+graphName+"): key='"+key+"', graphListAttr='"+graphListAttr+"'.");

							// Get serverName this graph is for
							var srvName = getGraphByName(graphName).getServerName();

							// Get Get current URL
							var currentUrl  = window.location.href;
							const url = new URL(currentUrl);
							const params = new URLSearchParams(url.search);

							// Change/set some parameters
							params.set('mdc',         "999");
							params.set('mdcp',        "false");
							params.set('gcols',       1);
							params.set('startTime',   new moment().subtract(30, "days").startOf('day').format('YYYY-MM-DD HH:mm'));
							params.set('endTime',     "24h");
							params.set('sessionName', srvName);
							params.set('graphList',   graphListAttr);

							window.open(`${location.pathname}?${params}`, '_blank');
						}
					},
					showThisInNewTabLast30DaysCurrentPeriod: {
						name: "Open this graph (last 30 days, # time) in new Tab",
						callback: function(key, opt) 
						{
							var graphName      = $(this).attr('id');
							var graphLineProps = $(this).attr('graph-line-props');
							var graphListAttr  = graphName + graphLineProps
							console.log("contextMenu(graphName="+graphName+"): key='"+key+"', graphListAttr='"+graphListAttr+"'.");

							// Get serverName this graph is for
							var srvName = getGraphByName(graphName).getServerName();

							// Get Get current URL
							var currentUrl  = window.location.href;
							const url = new URL(currentUrl);
							const params = new URLSearchParams(url.search);

							// Get current timespan
							var currentHourSpan = parseInt( $("#dbx-sample-time").html().replace("h", "") );
							if (isNaN(currentHourSpan))
								currentHourSpan = 2;
							if (currentHourSpan === 0)
								currentHourSpan = 2;
							if (currentHourSpan > 24)
								currentHourSpan = 24;
							
							// Change/set some parameters
							params.set('mdc',         "-30");
							params.set('mdcp',        "false");
							params.set('gcols',       1);
							params.set('endTime',     currentHourSpan + "h");
							params.set('sessionName', srvName);
							params.set('graphList',   graphListAttr);

							window.open(`${location.pathname}?${params}`, '_blank');
						}
					},

					separator2: "-----",
					
					showAllInNewTabLast3Days: {
						name: "Open ALL graphs in view (last 3 days) in new Tab",
						callback: function(key, opt) 
						{
							// Get Get current URL
							var currentUrl  = window.location.href;
							const url = new URL(currentUrl);
							const params = new URLSearchParams(url.search);

							// Get current timespan
							var currentHourSpan = parseInt( $("#dbx-sample-time").html().replace("h", "") );
							if (isNaN(currentHourSpan))
								currentHourSpan = 2;
							if (currentHourSpan === 0)
								currentHourSpan = 2;
							if (currentHourSpan > 24)
								currentHourSpan = 24;

							// Change/set some parameters
							params.set('endTime',     currentHourSpan + "h");
							params.set('mdc',         "-2");
							params.set('mdcp',        "false");
							params.set('gcols',       3);

							window.open(`${location.pathname}?${params}`, '_blank');
						}
					},
					showAllInNewTabLast5Days: {
						name: "Open ALL graphs in view (last 5 days) in new Tab",
						callback: function(key, opt) 
						{
							// Get Get current URL
							var currentUrl  = window.location.href;
							const url = new URL(currentUrl);
							const params = new URLSearchParams(url.search);

							// Get current timespan
							var currentHourSpan = parseInt( $("#dbx-sample-time").html().replace("h", "") );
							if (isNaN(currentHourSpan))
								currentHourSpan = 2;
							if (currentHourSpan === 0)
								currentHourSpan = 2;
							if (currentHourSpan > 24)
								currentHourSpan = 24;

							// Change/set some parameters
							params.set('endTime',     currentHourSpan + "h");
							params.set('mdc',         "-4");
							params.set('mdcp',        "false");
							params.set('gcols',       5);

							window.open(`${location.pathname}?${params}`, '_blank');
						}
					},
					showAllInNewTabLast7Days: {
						name: "Open ALL graphs in view (last 7 days) in new Tab",
						callback: function(key, opt) 
						{
							// Get Get current URL
							var currentUrl  = window.location.href;
							const url = new URL(currentUrl);
							const params = new URLSearchParams(url.search);

							// Get current timespan
							var currentHourSpan = parseInt( $("#dbx-sample-time").html().replace("h", "") );
							if (isNaN(currentHourSpan))
								currentHourSpan = 2;
							if (currentHourSpan === 0)
								currentHourSpan = 2;
							if (currentHourSpan > 24)
								currentHourSpan = 24;

							// Change/set some parameters
							params.set('endTime',     currentHourSpan + "h");
							params.set('mdc',         "-6");
							params.set('mdcp',        "false");
							params.set('gcols',       7);

							window.open(`${location.pathname}?${params}`, '_blank');
						}
					},
				}
			});

		} catch (error) {
			console.log("Failed to install Context menu: error="+error);
		}
	
		//---------------------------------------------------------------------------------------------------------
		// on "click": get the timestamp in the timeline, and draw a timeline-marker in ALL graphs on the same timestamp
		//             if you didn't click on a "point" then all timeline-markers are removed
		//---------------------------------------------------------------------------------------------------------
		newCanvas.addEventListener("click", function(event) 
		{
			// For CTRL + Click --- Hide the current line
			if (event.ctrlKey)
			{
				var clickedOnPoints = thisClass._chartObject.getElementsAtEventForMode(event, 'nearest', {intersect:true}, true);
				if (clickedOnPoints[0])
				{
					const item = clickedOnPoints[0];
					var ci = thisClass._chartObject;
					
					const dataSetIndex = item._datasetIndex;
					const meta = ci.getDatasetMeta(dataSetIndex);
					meta.hidden = meta.hidden === null ? !ci.data.datasets[dataSetIndex].hidden : null;
					ci.update();
				}
				return;
			}

			// ------------------------------------------------------------
			// If we clicked on a "label" (below the chart) -- Then do "nothing" -- Let chartJs do it's work
			// ------------------------------------------------------------
			var clickedOnPoints = thisClass._chartObject.getElementsAtEventForMode(event, 'nearest', {intersect:true}, true);
			if (clickedOnPoints.length === 0)
			{
				console.log("Skipping click on 'canvas/chart', since we did NOT click any 'line'... chartJs will do the work needed...");
				return;
			}
			else
			{
				// ------------------------------------------------------------
				// Below is "normal" click --- Mark the time in ALL charts
				// ------------------------------------------------------------
				var activePoints = thisClass._chartObject.getElementsAtEvent(event); // getPointsAtEvent(event)
				var firstPoint = activePoints[0];
				var clickTs = undefined;
				if(firstPoint !== undefined) 
					clickTs = thisClass._chartObject.data.labels[firstPoint._index];

				// Set-or-reset the timeline-markers in ALL graphs
				for(var i=0; i<_graphMap.length; i++)
				{
					const dbxGraph = _graphMap[i];
					dbxGraph.setTimelineMarker(clickTs);
				}
				
				// Then switch to history mode
				dbxHistoryAction(clickTs);

				// Load CM detail data for the clicked server and timestamp
				cmDetailLoadList(thisClass.getServerName(), clickTs);

				// Refresh DBMS Config panel if it is open
				if ($('#dbms-config-panel').is(':visible'))
					dbmsConfigLoad(thisClass.getServerName(), clickTs);
			}
		});
	} // END: Constructor

//	findClosestTimestamp(inputDate) 
//	{
//		let searchArray = this._chartObject.data.labels;
//		let closestMatch = null;
//		let smallestDiff = Infinity;
//
//		// Loop through the labels array to find the closest match
//		searchArray.forEach(entry => 
//		{
//			let diff = Math.abs(inputDate.diff(moment(entry))); // Get the absolute difference in milliseconds
//
//			if (diff < smallestDiff) 
//			{
//				smallestDiff = diff;
//				closestMatch = entry; // Update closest match
//			}
//		});
//
//		return closestMatch;
//	}
//	findClosestTimestampIndex(inputDate) 
//	{
//		let searchArray = this._chartObject.data.labels;
//		let closestIndex = -1;  // Default to -1 if no match is found
//		let smallestDiff = Infinity;
//
//		// Loop through the labels array to find the closest match
//		searchArray.forEach((entry, index) => 
//		{
//			let diff = Math.abs(inputDate.diff(moment(entry))); // Get the absolute difference in milliseconds
//		  
//			if (diff < smallestDiff) 
//			{
//				smallestDiff = diff;
//				closestIndex = index;
//			}
//		});
//
//		return closestIndex;
//	}

// NOTE: Used by v3/v4 to mark important section
//	/**
//	 * Set a marker (area of interest) start/end 
//	 * @param {*} startTime        The start time to mark
//	 * @param {*} endTime          The end time to mark 
//	 */
//	setMarkerStartEndTime(startTime, endTime)
//	{
//		console.log("setMarkerStartEndTime(startTime=|" + startTime + "|, endTime=|" + endTime + "|): srv='" + this._serverName + "', graphName='" + this._fullName + "'.");
//
//		// Reset marker area
//		if (this._chartObject.options.plugins.annotation.annotations.hasOwnProperty('markedStartEndTime'))
//			delete this._chartObject.options.plugins.annotation.annotations.markedStartEndTime;
//
//		this._chartObject.update(0);
//
//		if (startTime === undefined || endTime === undefined)
//			return;
//
//		this._markStartTime = startTime;
//		this._markEndTime   = endTime;
//
//		// NOTE: If you change the below also chang in Chart constructor/init code
//
//		// SET box/marker
//		const markedStartEndTime = {
//			id:   'markedStartEndTime',
//			type: 'box',
//			xMin: this._markStartTime,
//			xMax: this._markEndTime,
//			backgroundColor: 'rgba(128, 128, 128, 0.2)', // Light gray transparent background
//			borderWidth: 0
//		};
//
//		this._chartObject.options.plugins.annotation.annotations['markedStartEndTime'] = markedStartEndTime;
//		this._chartObject.update(0);
//	}
	/**
	 * Set a marker (area of interest) start/end 
	 * @param {*} startTime        The start time to mark
	 * @param {*} endTime          The end time to mark 
	 */
	setMarkerStartEndTime(startTime, endTime)
	{
//		console.log("setMarkerStartEndTime(startTime=|" + startTime + "|, endTime=|" + endTime + "|): srv='" + this._serverName + "', graphName='" + this._fullName + "'.");

		// Reset marker area
		const arrayPos = this._annotations.findIndex(item => item['id'] === 'markedStartEndTime')
		if (arrayPos !== -1)
		{
			this._annotations.splice(arrayPos, 1); // Remove 1 entry in the array
			this._chartObject.options.annotation.annotations = this._annotations; // Only in annotations v:0.5.7
		}

		this._chartObject.update(0);

		if ( startTime !== undefined && endTime !== undefined && startTime !== '' && endTime !== '')
		{
			//console.log("SET VALUES: setMarkerStartEndTime(startTime=|" + startTime + "|, endTime=|" + endTime + "|): srv='" + this._serverName + "', graphName='" + this._fullName + "'.");
			this._markStartTime = moment(startTime);
			this._markEndTime   = moment(endTime);

			// NOTE: If you change the below also chang in Chart constructor/init code
			// SET box/marker
			const markedStartEndTime = {
				id:   'markedStartEndTime',
				type: 'box',
				xScaleID: 'x-axis-0',
				yScaleID: 'y-axis-0',
				xMin: this._markStartTime,
				xMax: this._markEndTime,
				backgroundColor: 'rgba(128, 128, 128, 0.2)', // Light gray transparent background
				borderColor: 'gray',
				borderWidth: 0
			};

			this._annotations.push(markedStartEndTime);
			this._chartObject.options.annotation.annotations = this._annotations; // Only in annotations v:0.5.7

			this._chartObject.update(0);
		}
	}

	/**
	 * Set a timeline marker on a specific "moment object" timestamp
	 */
	setTimelineMarker(ts)
	{
//		console.log("setTimelineMarker(ts=|" + ts + "|): srv='" + this._serverName + "', graphName='" + this._fullName + "'.");

		this._timelineMarker = ts;
		if ( ! this.isInViewport() )
		{
//			console.log("<<< NOT inViewPort -- setTimelineMarker(ts=|" + ts + "|): srv='" + this._serverName + "', graphName='" + this._fullName + "'.");
			return;
		}

		// Reset timeline-marker
		const arrayPos = this._annotations.findIndex(item => item['id'] === 'timelineMarker')
		if (arrayPos !== -1)
		{
			this._annotations.splice(arrayPos, 1); // Remove 1 entry in the array
			this._chartObject.options.annotation.annotations = this._annotations; // Only in annotations v:0.5.7
		}
//console.log("------ arrayPos=" + arrayPos + ", this._annotations.length=" + this._annotations.length + " -- setTimelineMarker(ts=|" + ts + "|): srv='" + this._serverName + "', graphName='" + this._fullName + "'.");
		
//		this._chartObject.options.annotation.annotations = [];
//v3		if (this._chartObject.options.plugins.annotation.annotations.hasOwnProperty('timelineMarker'))
//v3			delete this._chartObject.plugins.options.annotation.annotations.timelineMarker;
		this._chartObject.update(0);
		
		// If no 'ts' input... get out of here (with just reseting the timeline-marker
		if (ts === undefined)
			return;

		// Set label: 2018-01-23 19:33:00
		var tsStr1 = moment(ts).format().substring(0, 10);
		var tsStr2 = moment(ts).format().substring(11, 19);
		var labelTsStr = tsStr1 + " " + tsStr2;
		// if it's the same day: Set label: 19:33:00
		if ( moment().format().substring(0,10) === tsStr1)
			labelTsStr = tsStr2;

		// SET timeline-markers in ALL Graphs
		const annotation = {
			id: 'timelineMarker',
			type: 'line',
			mode: 'vertical',
			scaleID: 'x-axis-0',
			value: ts,
			borderColor: 'gray',
			borderWidth: 2,
			borderDash: [2, 2],
			label: {
				enabled: true,
				content: labelTsStr,
				fontSize: 10,
				backgroundColor: 'gray',
				position: 'top'
			}
		};
		this._annotations.push(annotation);
		this._chartObject.options.annotation.annotations = this._annotations; // Only in annotations v:0.5.7
//		this._annotations['timelineMarker'] = annotation;
//		this._chartObject.options.annotation.annotations[0] = annotation;
//v3		this._chartObject.options.plugins.annotation.annotations['timelineMarker'] = annotation;
		this._chartObject.update(0);
//const arrayPos1 = this._annotations.findIndex(item => item['id'] === 'timelineMarker')
//console.log("<<<<<< arrayPos1=" + arrayPos1 + ", this._annotations.length=" + this._annotations.length + " -- setTimelineMarker(ts=|" + ts + "|): srv='" + this._serverName + "', graphName='" + this._fullName + "'.");
	} // end: method

	/**
	 * Is visible in the viewport (any pixel)
	 */
	isInViewport() 
	{
		const rect = this._canvas.getBoundingClientRect();

		// DOMRect { x: 8, y: 8, width: 100, height: 100, top: 8, right: 108, bottom: 108, left: 8 }
		var windowHeight = (window.innerHeight || document.documentElement.clientHeight);
		var windowWidth = (window.innerWidth || document.documentElement.clientWidth);

		// http://stackoverflow.com/questions/325933/determine-whether-two-date-ranges-overlap
		var vertInView = (rect.top <= windowHeight) && ((rect.top + rect.height) >= 0);
		var horInView = (rect.left <= windowWidth) && ((rect.left + rect.width) >= 0);

		return (vertInView && horInView);
//		return (
//			rect.top >= 0 &&
//			rect.left >= 0 &&
//			rect.bottom <= (window.innerHeight || document.documentElement.clientHeight) &&
//			rect.right <= (window.innerWidth || document.documentElement.clientWidth)
//		);
	}

	/**
	 * Called from _graphObserver when graph is "srolled into"
	 */
	enterVisibility()
	{
//		console.log("enterVisibility: name='" + this._fullName + "', this._timelineMarker='" + this._timelineMarker + "'.");
		if (this._timelineMarker !== undefined)
		{
			this.setTimelineMarker(this._timelineMarker);
		}
	}

	setInitialized()     { this._initialized = true; }
	isInitialized()      { return this._initialized; }
                         
	getHeight()          { return this._graphHeight; }
	getServerName()      { return this._serverName; }
	getCmName()          { return this._cmName; }
	getGraphName()       { return this._graphName; }
	getFullName()        { return this._fullName; }
	getGraphLabel()      { return this._graphLabel; }
	getGraphProps()      { return this._graphProps; }
	getGraphCategory()   { return this._graphCategory; }
	getGraphLineProps()  { return this._graphLineProps; }

	getInitStartTime()   { return this._initStartTime;   }
	getInitEndTime()     { return this._initEndTime;     }
	getInitSampleType()  { return this._initSampleType;  }
	getInitSampleValue() { return this._initSampleValue; }
	isMultiDayChart()    { return this._isMultiDayChart; }

	// Below is for chart.js 3
//	getMaxValue()        { return this._chartConfig.options.scales.y.max; }
//	getMinValue()        { return this._chartConfig.options.scales.y.min; }
//	setMaxValue(val)     { return this._chartConfig.options.scales.y.max = val; }
//	setMinValue(val)     { return this._chartConfig.options.scales.y.min = val; }
	getMaxValue()        { return this._chartConfig.options.scales.yAxes[0].ticks.max === undefined ? "" : this._chartConfig.options.scales.yAxes[0].ticks.max; }
	getMinValue()        { return this._chartConfig.options.scales.yAxes[0].ticks.min === undefined ? "" : this._chartConfig.options.scales.yAxes[0].ticks.min; }
	setMaxValue(val)     { this._chartConfig.options.scales.yAxes[0].ticks.max = (val !== "" && val !== null) ? parseFloat(val) : undefined; this._chartObject.update(); }
	setMinValue(val)     { this._chartConfig.options.scales.yAxes[0].ticks.min = (val !== "" && val !== null) ? parseFloat(val) : undefined; this._chartObject.update(); }

	// Run AFTER load has been completed, so we can make any "adjustments"
	onLoadCompetion()
	{
		this.enableDisableChartLines();
		
		if ( this._markStartTime !== undefined && this._markEndTime !== undefined && this._markStartTime !== '' && this._markEndTime !== '')
		{
			this.setMarkerStartEndTime(this._markStartTime, this._markEndTime);
		}
	}

	checkForNoDataInChart()
	{ 
		// No data is present (write in chart object)
		if (this._chartObject.data.datasets.length === 0) 
		{
			var ctx      = this._chartObject.chart.ctx;
			var width    = this._chartObject.chart.width;
			var height   = this._chartObject.chart.height
			var labelTxt = 'No data was found. startTime='+this._initStartTime+', endTime='+this._initEndTime;
			//this._chartObject.clear();
			
			//setTimeout(function() { 
				ctx.save();
				ctx.textAlign    = 'center';
				ctx.textBaseline = 'middle';
				ctx.fillStyle    = "#FF7F50";   // coral, #FF7F50, rgb(255,127,80)
				ctx.font         = "16px normal 'Helvetica Nueue'";
				ctx.fillText(labelTxt, width / 2, height / 2);
				ctx.restore();
			//}, 1000);
		}
	}

	buildHtmlAttrGraphLineProps()
	{
		var retStr = "";
		if (this._graphLineProps.hasOwnProperty("showCol") && this._graphLineProps.showCol.length > 0)
		{
			this._graphLineProps.showCol.forEach(function(name)
			{
				retStr += name + ";";
			});
		}
		if (this._graphLineProps.hasOwnProperty("hideCol") && this._graphLineProps.hideCol.length > 0)
		{
			this._graphLineProps.hideCol.forEach(function(name)
			{
				retStr += "-" + name + ";";
			});
		}

		// Remove last ";"
		if (retStr.endsWith(";"))
			retStr = retStr.substring(0, retStr.length-1);

		// if we got data, surround with []
		if (retStr !== "")
			retStr = "[" + retStr + "]";

		return retStr;
	}
	
	// Based on property "_graphLineProps", hide/show specific laines foreach graph...
	// The is specified in the URL. Example: http://gorans.org:8080/graph.html?startTime=4h&sessionName=GORAN_UB3_DS&graphList=CmExecutionTime_TimeGraph[Sorting;Compilation]
	//   - to show only the following 2 lines 'Sorting' and 'Compilation'                                                                               ^^^^^^^^^^^^^^^^^^^^^
	enableDisableChartLines()
	{
		var ci = this._chartObject;

		// Disable all lines EXEPT the ones in "_graphLineProps.showCol"
		if (this._graphLineProps.hasOwnProperty("showCol") && this._graphLineProps.showCol.length > 0)
		{
			// Hide ALL 
			ci.data.datasets.forEach(function(dataset, index)
			{
				var meta = ci.getDatasetMeta(index);
				meta.hidden = true;
			});
			// Show specified
			this._graphLineProps.showCol.forEach(function(name)
			{
				var foundLabel = false;
				ci.data.datasets.forEach(function(dataset, index)
				{
					if (name === dataset.label)
					{
						foundLabel = foundLabel;
						var meta = ci.getDatasetMeta(index);
						meta.hidden = false;
					}
				});
				if ( ! foundLabel )
					console.log("WARNING: enableDisableChartLines(): show: did NOT find the label '" + name + "'.");
			});
		}

		// Disable all lines in "_graphLineProps.hideCol"
		if (this._graphLineProps.hasOwnProperty("hideCol") && this._graphLineProps.hideCol.length > 0)
		{
			// HIDE specified
			this._graphLineProps.hideCol.forEach(function(name)
			{
				var foundLabel = false;
				ci.data.datasets.forEach(function(dataset, index)
				{
					if (name === dataset.label)
					{
						foundLabel = foundLabel;
						var meta = ci.getDatasetMeta(index);
						meta.hidden = true;
					}
				});
				if ( ! foundLabel )
					console.log("WARNING: enableDisableChartLines(): hide: did NOT find the label '" + name + "'.");
			});
		}

		ci.update();
	}

	getTsArraySize()
	{ 
		return this._chartObject.data.labels.length; 
	}

	getTsArray()
	{ 
		return this._chartObject.data.labels; 
	}

	getOldestTs() 
	{ 
		return this._chartObject.data.labels[0]; 
	}

	getNewestTs() 
	{
		let lastPos = this._chartObject.data.labels.length - 1;
		if (lastPos < 0)
			lastPos = 0;
		return this._chartObject.data.labels[lastPos]; 
	}
	
	// Add data comming from SSE (push data from the server)
	addSubscribeData(jsonData)
	{
		if ( ! this.isInitialized() )
		{
			if (this._debug > 0)
				console.log("Subscribe data can not yet be added for "+this._fullName);

			// Or possible add data to a queue that will be read after initial data has been loaded, but for now just "do nothing"
			return;
		}
		this.addData(jsonData);

		//------------------------------------------------------------------------------------------
		// if subscribe is on, then remove records that are older than the subscribed "time span"
		//------------------------------------------------------------------------------------------
		if (this._subscribeAgeInSec > 0 && this._chartObject.data.labels.length > 0)
		{
			// loop on all labels; break when no more values to delete
			for (let d=0; d<this._chartObject.data.labels.length; d++)
			{
				const oldestTs = this._chartObject.data.labels[0];
				const ageInSec = (moment() - oldestTs) / 1000;   // remove milliseconds
				if (this._debug > 1)
					console.log("addSubscribeData(): INFO 'removeOldValues-loop': d="+d+", name='"+this.getFullName()+"', _subscribeAgeInSec="+this._subscribeAgeInSec+", ageInSec="+ageInSec+", oldestTs="+oldestTs);

				if ( ageInSec > this._subscribeAgeInSec)
				{
					if (this._debug > 1)
						console.log("addSubscribeData(): DO-REMOVE 'removeOldValues-loop': d="+d+", name='"+this.getFullName()+"', _subscribeAgeInSec="+this._subscribeAgeInSec+", ageInSec="+ageInSec+", oldestTs="+oldestTs);

					this._chartObject.data.labels.shift();
					for (let dsi=0; dsi<this._chartObject.data.datasets.length; dsi++)
					{
						var ds = this._chartObject.data.datasets[dsi];
						ds.data.shift();
					}

					// if we have timeline-markers older than the "points" we deleted.
					// Remove the annotation
					const arrayPos = this._annotations.findIndex(item => item['id'] === 'timelineMarker')
					if (arrayPos !== -1)
					{
						if (this._annotations[arrayPos].value <= oldestTs)
						{
							if (this._debug > 0)
								console.log("Clearing timeline-marker for graph '"+this._fullName+"' due to time-expire. annotationTs='"+this._annotations[arrayPos].value+"', graphpOldestTs='"+oldestTs+"'.");
							this._annotations.splice(arrayPos, 1); // Remove 1 entry in the array
							this._chartObject.options.annotation.annotations = this._annotations; // Only in annotations v:0.5.7
						}
					}
					
//					if (this._chartObject.options.annotation.annotations.length > 0)
//					{
//						if (this._chartObject.options.annotation.annotations[0].value <= oldestTs)
//						{
//							if (this._debug > 0)
//								console.log("Clearing timeline-marker for graph '"+this._fullName+"' due to time-expire. annotationTs='"+this._chartObject.options.annotation.annotations[0].value+"', graphpOldestTs='"+oldestTs+"'.");
//							this._chartObject.options.annotation.annotations = [];
//						}
//					}
//v3					if (this._chartObject?.options?.plugins?.annotation?.annotations?.hasOwnProperty('timelineMarker'))
//v3					{
//v3						if (this._chartObject.options.plugins.annotation.annotations['timelineMarker'].value <= oldestTs)
//v3						{
//v3							if (this._debug > 0)
//v3								console.log("Clearing timeline-marker for graph '"+this._fullName+"' due to time-expire. annotationTs='"+this._chartObject.options.plugins.annotation.annotations[0].value+"', graphpOldestTs='"+oldestTs+"'.");
//v3							delete this._chartObject.options.plugins.annotation.annotations.timelineMarker;
//v3						}
//v3					}
				}
				else
				{
					break;
				}
			}
			// Redraw the graph
			// this._chartObject.update(0);
			this._chartObject.update();
		}
	}

	// method: addData()
	addData(jsonData)
	{
		if (_debug > 2)
			console.log("DbxGraph.addData(): jsonData: ", jsonData);
		if (_debug > 0)
			console.log("DbxGraph.addData(): jsonData.length=", jsonData.length);

		for (let i=0; i<jsonData.length; i++) 
		{
			const graph     = jsonData[i];
			// const cmName    = graph["cmName"];
			// const graphName = graph["graphName"];
			// const dpEntry   = graph["data"];
			// const sessionSampleTime = graph["sessionSampleTime"];
			const cmName    = graph.cmName;
			const graphName = graph.graphName;
			const dpEntry   = graph.data;
			const sessionSampleTime = graph.sessionSampleTime;

			const sstDate = moment(sessionSampleTime);

			if ( !(sstDate in this._chartObject.data.labels) ) 
			{
				this._chartObject.data.labels.push(sstDate);
			}
			const pointLabelPos = this._chartObject.data.labels.indexOf(sstDate);

			for (let dpLabel in dpEntry)
			{
				const dpData = dpEntry[dpLabel];

				var ds = undefined;
				for (let dsi=0; dsi<this._chartObject.data.datasets.length; dsi++) 
				{
					const dataset = this._chartObject.data.datasets[dsi];
					if (dataset.label === dpLabel)
						ds = dataset;
				}

				// if dataset can't be found, add it 
				if ( ds === undefined )
				{
					var colorNames = Object.keys(window.chartColors);
					var colorName = colorNames[this._chartObject.data.datasets.length % colorNames.length];
					var newColor = window.chartColors[colorName];

					//console.log("--- ds UNDEFINED i="+i+", cmName="+cmName+", graphName="+graphName+": sessionSampleTime="+sessionSampleTime+", sstDate="+sstDate+", pointLabelPos="+pointLabelPos+", dpLabel="+dpLabel);
					var newDataset = {
						label: dpLabel,             // The label for the dataset which appears in the legend and tooltips
						backgroundColor: newColor,  // The fill color under the line.
						borderColor: newColor,      // The color of the line.
						borderWidth: 2,             // The width of the line in pixels.
						pointRadius: 0,             // The radius of the point shape. If set to 0, the point is not rendered.
						pointHitRadius: 5,          // The pixel size of the non-displayed point that reacts to mouse events.
						fill: false,                // How to fill the area under the line.
						data: []
					};

					this._chartObject.data.datasets.push(newDataset);
					ds = newDataset;
				}

				ds.data[pointLabelPos] = dpData;

				if (this._debug > 9)
					console.log("    ds.data.push : i="+i+", cmName="+cmName+", graphName="+graphName+", sessionSampleTime="+sessionSampleTime+", sstDate="+sstDate+", pointLabelPos="+pointLabelPos+", dpLabel="+dpLabel+", dpData="+dpData);
			}
		}
		// set small "dots" on the chart lines if there is less than 100 data points
		if (this._chartObject.data.labels.length <= 50)
		{
			var pointRadiusSize = 1;
			if (this._chartObject.data.labels.length <= 25)
				pointRadiusSize = 2;

			// loop all datasets, and set 'pointRadius'
			for (let dsi=0; dsi<this._chartObject.data.datasets.length; dsi++) 
			{
				const dataset = this._chartObject.data.datasets[dsi];
				dataset.pointRadius = pointRadiusSize;
			}
		}
		this.setInitialized();
		this._chartObject.update();

		//-------------------------------------------------------
		// Below is trying to guess how many Legend rows, we had... if to many rows:
		// Make the canvas a bit bigger...
		//-------------------------------------------------------
		var legendLabelPixWidthSum = 0;
		for (let dsi=0; dsi<this._chartObject.data.datasets.length; dsi++) 
		{
			const dataset = this._chartObject.data.datasets[dsi];
			legendLabelPixWidthSum += dataset.label.length * 5; // lets asume that every char is approx 5 pixel wide (for simplicity)
		}
		legendLabelPixWidthSum += this._chartObject.data.datasets.length * 17; // add 17 pixels for each Legend (10 for the squre, and 7 for some spacing)
		const guessedLegendRows = Math.ceil(legendLabelPixWidthSum / this._canvas.width);
		// console.log("cmName="+this._cmName+", graphName="+this._graphName+", this._chartObject.data.datasets.length="+this._chartObject.data.datasets.length+", legendLabelPixWidthSum="+legendLabelPixWidthSum);
		// console.log("cmName="+this._cmName+", graphName="+this._graphName+", canvas.height="+this._canvas.height+", canvas.width="+this._canvas.width+".");
		// console.log("cmName="+this._cmName+", graphName="+this._graphName+", canvasCtx.canvas.height="+this._canvasCtx.canvas.height+", canvasCtx.canvas.width="+this._canvasCtx.canvas.width+".");
		// console.log("cmName="+this._cmName+", graphName="+this._graphName+", guessedLegendRows="+guessedLegendRows+".");
		if (guessedLegendRows > 1)
		{
			var curHeight = parseInt(this._canvas.style.height);

			// If less or equal the original height, then grow the canvas a bit
			if (curHeight <= this._graphHeight)
			{
				var newHeight = curHeight + (guessedLegendRows * 17); // Asume every Legend row takes approx 17px

				if (this._debug > 0)
					console.log("---------------------------- cur-Height: "+this._canvas.style.height+", newHeight="+newHeight+", guessedLegendRows="+guessedLegendRows+", cmName="+this._cmName+", graphName="+this._graphName+".");

				//this._canvas.style.height = newHeight+"px";
				this._chartObject.canvas.parentNode.style.height = newHeight+"px";

				if (this._debug > 1)
					console.log("++++++++++++++++++++++++++ check-Height: "+this._canvas.style.height);
	
				// Simulate a windows resize to redraw the "whole thing"
				// Changed to: do this-at-the-end in function: graphLoadIsComplete()
				// window.dispatchEvent(new Event('resize'));
			}
		}

		if (jsonData.length === 0) 
		{
			//this.checkForNoDataInChart();
			//this._chartFeedback.innerHTML = "<font color='red'>No data was found. startTime="+this._initStartTime+", endTime="+this._initEndTime+"</font>";
		} else {
			this._chartFeedback.innerHTML = "";
			// this._chartFeedback.innerHTML = jsonData.length + " data points in chart";
		}
	}
}



function dbxChartPrintApiHelp()
{
	// alert("Specify 'sessionName=NAME' as a parameter in the URL.");
	// return;
	$("#api-feedback").css("color", "red");
	$("#api-feedback").html(
		'<h2>Error: Missing mandatory parameter <code>sessionName</code> and/or <code>graphList</code>.</h2>' +
		'There should be a Dialog, where you can choose what you want to "graph"...<br>' +
		'But that has not yet been implemented<br>' +
		'<br>' +
		'Until then, please use the below paraemeters (note the below is priliminary, and can be changed "any" time)' +
		'<table border="1">' +
		'<tr> <th>ParameterName</th> <th>Description</th> </tr>' +
		'<tr>' + 
			'<td>sessionName</td>' + 
			'<td>Server Name you want to choose graphs for, Note: This can be a comma (,) separated list of server names (all is a shorthand, for all servers)<br>' +
			'This can also be a saved <i>template/profile</i> stored in the DbxTuneCentral database. To get available profiles: <a href="/api/graph/profiles">/api/graph/profiles</a><br>' +
			'Example 1: <code>PROD_A_ASE</code><br>' +
			'Example 2: <code>PROD_A_ASE,PROD_B_ASE</code><br>' +
			'Example 3: <code>someProfileName</code><br>' +
			'Example 4: <code>all</code><br>' +
			'</td>' + 
		'</tr>' +
		'<tr>' + 
			'<td>graphList</td>' + 
			'<td>A JSON Object that contains what you want to display graphs for, or a comma separated list of graph names<br>' + 
			'Example 1: <code>[{"srv":"PROD_A_ASE","graph":"CmSummary_aaCpuGraph"},{"srv":"PROD_B_ASE","graph":"CmSummary_aaCpuGraph"}]</code><br>' +
			'Example 2: <code>[{"graph":"CmSummary_aaCpuGraph"},{"graph":"CmSummary_OldestTranInSecGraph"}]</code><br>' +
			'Example 3: <code>all</code> Special word to choose all graph names.<br>' +
			'Example 4: <code>CmSummary_aaCpuGraph,CmSummary_OldestTranInSecGraph</code> only the 2 graphs specified in the comma separeted list.<br>' +
			'Note 1: Available SRV_NAME(S) or "sessions" can be fetched using: <a href="/api/sessions">/api/sessions</a><br>' +
			'Note 2: Name of graph(s) for a SERVER can be fetched using: <a href="/api/graphs?sessionName=replace-me-with-a-SRV_NAME-from-note-1">/api/graphs?sessionName=replace-me-with-a-SRV_NAME-from-note-1</a><br>' +
			'Note 3: If the "srv":"SRV_NAME" is specified (as in example 1). the <code>sessionName</code> parameter wont have to be specified<br>' +
			'Note 4: If only "graph":"CmName_graphName" is specified (as in example 2), or "all" (as in example 3). the <code>sessionName</code> parameter has to be specified, and if you specify more that one server, the graph(s) will be displayed for all servers<br>' +
			'Note 5: If a comma separated list is specified (as in example 4). the <code>sessionName</code> parameter has to be specified.<br>' +
			'</td>' + 
		'</tr>' +
		'<tr>' + 
			'<td>startTime</td>' + 
			'<td>A start time from when to show graphs.<br>' + 
			'This can be specified in various formats.<br>' + 
			'<ul>' + 
			'  <li>120m - Start time 120 minutes from current time</li>' + 
			'  <li>2h - Start time 2 hours from current time</li>' + 
			'  <li>1d - 1 day (24 hours) from current time</li>' + 
			'  <li>2018-02-18 18:00 - A Specififc date in time</li>' + 
			'</ul>' + 
			'<b>default:</b> 2h<br>' + 
			'</td>' + 
		'</tr>' +
		'<tr>' + 
			'<td>endTime</td>' + 
			'<td>A end time.<br>' + 
			'This can be specified in various formats.<br>' + 
			'<ul>' + 
			'  <li>120m - 120 minutes from the start time</li>' + 
			'  <li>2h - 2 hours from the start time</li>' + 
			'  <li>1d - 1 day (24 hours) from the start time</li>' + 
			'  <li>now - Current time</li>' + 
			'  <li>2018-02-18 23:30 - A Specififc date in time</li>' + 
			'</ul>' + 
			'<b>default:</b> 2h<br>' + 
			'</td>' + 
		'</tr>' +
		'<tr>' + 
			'<td>markTime</td>' + 
			'<td>Mark a time in the timeline, where you want to look, (also implies that we are going into history mode)<br>' + 
			'Format: YYYY-mm-dd HH:MM:SS<br>' + 
			'<b>default:</b> none<br>' + 
			'</td>' + 
		'</tr>' +
		'<tr>' + 
			'<td>markStartTime</td>' + 
			'<td>Draw a gray area over the chart to indicate a period of interest. (both: markStartTime and markEndTime needs to be specified)<br>' + 
			'Format: YYYY-mm-dd HH:MM:SS<br>' + 
			'<b>default:</b> none<br>' + 
			'</td>' + 
		'</tr>' +
		'<tr>' + 
			'<td>markEndTime</td>' + 
			'<td>Draw a gray area over the chart to indicate a period of interest. (both: markStartTime and markEndTime needs to be specified)<br>' + 
			'Format: YYYY-mm-dd HH:MM:SS<br>' + 
			'<b>default:</b> none<br>' + 
			'</td>' + 
		'</tr>' +
		'<tr>' +
			'<td>mdc</td>' + 
			'<td>Multi Day Chart<br>' + 
			'If you want to compare several charts in the "time line".<br>' + 
			'Lets say you want to compare 2 hours for the whole week (or more) to see any differences.<br>' + 
			'The easiest way to get a MultiDayChart is to right click on a chart and choose "Open this graph (last # days) in new tab".<br>' + 
			'Here is a couple of examples.<br>' + 
			'<ul>' + 
			'  <li>IO Summary for last 7 days, 2 hours every day.<br>' +
			'  <code>graph.html?sessionName=GORAN_UB3_DS&startTime=2h&graphList=CmSummary_aaReadWriteGraph&mdc=-7</code></li>' + 
			'  <li>CPU and IO Summary from 2019-02-01 11:00 and 3 days forward, 4 hours every day, 1 chart per row<br>' +
			'  <code>graph.html?sessionName=GORAN_UB3_DS&startTime=2019-02-01 11:00&graphList=CmSummary_aaCpuGraph,CmSummary_aaReadWriteGraph&mdc=3&endTime=4h&gcols=1</code></li>' + 
			'  <li>All default charts, for last 5 days, last 2 hours every day, 5 charts per row (one day per column in the chart view)<br>' +
			'  <code>graph.html?sessionName=GORAN_UB3_DS&startTime=2h&mdc=4&gcols=5</code></li>' + 
			'</ul>' + 
			'<b>default:</b> not enabled<br>' + 
			'</td>' + 
		'</tr>' +
		'<tr>' +
			'<td>mdcp</td>' + 
			'<td>Multi Day Chart <b>Pivot</b><br>' + 
			'Just how you do the Graph Layout! (graphs first, or days first)<br>' + 
			'<ul>' + 
			'  <li>false - First: Loop on graph names, Then: Loop on mdc-days    </li>' +
			'  <li>true  - First: Loop on mdc-days,    Then: Loop on graph names </li>' +
			'</ul>' + 
			'<b>default:</b> false<br>' + 
			'</td>' + 
		'</tr>' +
		'<tr>' +
			'<td>mdcwd</td>' + 
			'<td>Multi Day Chart <b>Week Day</b><br>' + 
			'If you just want some day of the week to be part of the "mdc/Multi Day Chart"<br>' + 
			'This is a comma separated list of numbers.<br>' + 
			'Where: 1=Monday, 2=Tuesday, 3=Wednesday, 4=Thursday, 5=Friday, 6=Saturday, 7=Sunday<br>' + 
			'<b>default:</b> 1,2,3,4,5,6,7 (all days)<br>' + 
			'</td>' + 
		'</tr>' +
		'<tr>' + 
			'<td>debug</td>' + 
			'<td>How much "debug" messages we want to print to the "console.log". <br>' +
			'This is a integer from 0-10<br>' + 
			'<b>default:</b> 2h<br>' + 
			'</td>' +
		'</tr>' +
		'<tr>' + 
			'<td>gheight</td>' + 
			'<td>Minimum Height in pixels for the graphs<br>' + 
			'<b>default:</b> 200<br>' + 
			'</td>' +
		'</tr>' +
		'<tr>' + 
			'<td>gwidth</td>' + 
			'<td>Minimum Width in pixels for the graphs<br>' + 
			'<b>default:</b> 650<br>' + 
			'</td>' +
		'</tr>' +
		'<tr>' + 
			'<td>gcols</td>' + 
			'<td>Number of graph columns. This is an automatic way to set gwidth. It just takes current browser width, divides it with <code>gcols</code>, then sets <code>gwidth</code> to that value.<br>' + 
			'<b>default:</b> not specified, meaning: it depends on your current browser size, but normally from 1 to 3<br>' + 
			'</td>' +
		'</tr>' +
		'<tr>' + 
			'<td>cs</td>' + 
			'<td>Color Schema... If you want a <i>dark</i> theme... use: <code>cs=dark</code><br>' + 
			'<b>default:</b> white<br>' + 
			'</td>' +
		'</tr>' +
		'<tr>' + 
			'<td>sampleType</td>' + 
			'<td>Data point about the graph can be fetched by different "methods".<br>' + 
			'Here are the methods:<br>' + 
			'<ul>' + 
			'  <li>ALL - Get all data points</li>' + 
			'  <li>AUTO - Uses a automatic formula, normally uses ALL, if there are to many data-points in the sample intervall, then switch over to: MAX_OVER_SAMPLES or MIN_OVER_SAMPLES</li>' + 
			'  <li>MAX_OVER_SAMPLES - Get MAX values over X number of data points</li>' + 
			'  <li>MIN_OVER_SAMPLES - Get MIN values over X number of data points</li>' + 
			'  <li>MAX_OVER_MINUTES - Get MAX values over X minutes of data points</li>' + 
			'  <li>MIN_OVER_MINUTES - Get MIN values over X minutes of data points</li>' + 
			'  <li>AVG_OVER_MINUTES - Get AVERAGE values over X minutes of data points</li>' + 
			'  <li>SUM_OVER_MINUTES - Get SUM values over X minutes of data points</li>' + 
			'</ul>' + 
			'<b>default:</b> AUTO<br>' + 
			'</td>' + 
		'</tr>' +
		'<tr>' + 
			'<td>sampleValue</td>' + 
			'<td>Used by <code>sampleType</code> <br>' + 
			'    NOTE: this is a <b>number</b>... sampleValue=360 (for XXX_OVER_SAMPLES) or sampleValue=10 (for XXX_OVER_MINUTES)' + 
			'<ul>' + 
			'  <li>ALL - <code>sampleValue</code> <b>will not be used.</b></li>' + 
			'  <li>AUTO - <code>sampleValue</code> <b>will not be used.</b></li>' + 
			'  <li>sampleValue=360   for: MAX_OVER_SAMPLES - A number value...</li>' + 
			'  <li>sampleValue=360   for: MIN_OVER_SAMPLES - A number value...</li>' + 
			'  <li>sampleValue=10    for: MAX_OVER_MINUTES - A number value... like: 10 for ten minutes</li>' + 
			'  <li>sampleValue=10    for: MIN_OVER_MINUTES - A number value... like: 10 for ten minutes</li>' + 
			'  <li>sampleValue=10    for: AVG_OVER_MINUTES - A number value... like: 10 for ten minutes</li>' + 
			'  <li>sampleValue=10    for: SUM_OVER_MINUTES - A number value... there are 60 minutes in one hour, and 1440 minutes in one day</li>' + 
			'</ul>' + 
			'<b>default:</b> ""<br>' + 
			'</td>' + 
		'</tr>' +
		'</table>' +
		'Below is a full example<br>' +
		'<code>http://dbxtune.company.com:8080/graph.html?sessionName=PROD_A_ASE&startTime=2h&subscribe=true</code>'
	);
}


//window.onload = function() 
function dbxTuneLoadCharts(destinationDivId)
{
	// get query string parameter
	const sessionName    = getParameter("sessionName");
	var   graphList      = getParameter("graphList");
	var   startTime      = getParameter("startTime",     "2h");
	var   endTime        = getParameter("endTime",       "");
	var   markTime       = getParameter("markTime",      "");               // Mark this time in the timeline (also implies that we are going into history mode)
	var   markStartTime  = getParameter("markStartTime", "");               // Start time for an area of interest
	var   markEndTime    = getParameter("markEndTime",   "");               // End time for an area of interest
	var   multiDayChart  = getParameter("mdc",           "");
	var   mdcPivot       = getParameter("mdcp",          false);
	var   mdcWeekDays    = getParameter("mdcwd",         "1,2,3,4,5,6,7");
	var   subscribe      = getParameter("subscribe",     false);
	const debug          = getParameter("debug",         0);
	const gheight        = getParameter("gheight",       200);
	const gwidth         = getParameter("gwidth",        650);
	const sampleType     = getParameter("sampleType",    "");
	const sampleValue    = getParameter("sampleValue",   "");
	const colorSchema    = getParameter("cs",            "dark");

	_debug = debug;
	_colorSchema = colorSchema;

	console.log("Passed sessionName="   + sessionName);
	console.log("Passed graphList="     + graphList);
	console.log("Passed startTime="     + startTime);
	console.log("Passed endTime="       + endTime);
	console.log("Passed markTime="      + markTime);
	console.log("Passed markStartTime=" + markStartTime);
	console.log("Passed markEndTime="   + markEndTime);
	console.log("Passed mdc="           + multiDayChart); // Multi Day Chart
	console.log("Passed mdcp="          + mdcPivot);      // Multi Day Chart - Pivot
	console.log("Passed mdcwd="         + mdcWeekDays);   // Multi Day Chart - WeekDays
	console.log("Passed colorSchema="   + colorSchema);

	if (subscribe === "false")
		subscribe = false;

	// turn subscribe off if 'endTime' is present
	if (endTime !== "")
		subscribe = false;

	// turn subscribe off if 'markTime' is present
	if (markTime !== "")
		subscribe = false;

	// Set the global variable
	_subscribe = subscribe;

	var loadAllGraphs = false;
	if (graphList !== undefined && graphList === "all")
	{
		loadAllGraphs = true;
		graphList = undefined;
	}

	window.addEventListener("resize", function() { console.log("received-resize-event"); });

	// Print HELP and EXIT
	if ( sessionName == null && graphList == null)
	{
		dbxChartPrintApiHelp();
		return;
	}

	// Extract MultiDayChart info from "startTime": which may look like "[5]2h" or "[5]2019-02-01 10:00"
	// if "startTime" starts with [], then put that value inside the [] into multiDayChart... and remove the [] content from startTime
//	var startTimeHasMdcSpec = startTime.match(/\[(.*?)\]/);
//	if (startTimeHasMdcSpec)
//	{
//		multiDayChart = startTimeHasMdcSpec[1];
//		startTime     = startTime.replace(/\[(.*?)\]/, "");
//
//		console.log("startTime has MultiDayChart Spec, setting new values for: multiDayChart="+multiDayChart+", startTime="+startTime);
//	}

	// startTime: 2019-02-03+18:00  --- remove the +
	if (startTime.match("^[0-9][0-9][0-9][0-9][-][0-9][0-9][-][0-9][0-9][+]"))
		startTime = startTime.replace("+", " ");

	// endTime: 2019-02-03+18:00  --- remove the +
	if (endTime.match("^[0-9][0-9][0-9][0-9][-][0-9][0-9][-][0-9][0-9][+]"))
		endTime = endTime.replace("+", " ");

	// markTime: 2019-02-03+18:00  --- remove the +
	if (markTime.match("^[0-9][0-9][0-9][0-9][-][0-9][0-9][-][0-9][0-9][+]"))
		markTime = markTime.replace("+", " ");

	// markStartTime: 2019-02-03+18:00  --- remove the +
	if (markStartTime.match("^[0-9][0-9][0-9][0-9][-][0-9][0-9][-][0-9][0-9][+]"))
		markStartTime = markStartTime.replace("+", " ");

	// markEndTime: 2019-02-03+18:00  --- remove the +
	if (markEndTime.match("^[0-9][0-9][0-9][0-9][-][0-9][0-9][-][0-9][0-9][+]"))
		markEndTime = markEndTime.replace("+", " ");

	// Check the startTime
	// If it's a valid data, if not it might be some strange chars, which can be removed
	//if ( ! moment(startTime).isValid() )
	//{
	//}


	// add MultiDayChart to "startTimeArr"
	// Even if NO MultiDayChart, then add 1 entry to "startTimeArr"
	var startTimeArr = [];
	console.log(">>>>>>>>>>>>>>>>>>>>> multiDayChart=|"+multiDayChart+"|.");
	if (multiDayChart === "") // NO mdc, then push "current" time
	{
		startTimeArr.push( startTime );
	}
	else
	{
		// subscribe is not supported in MultiDayChart
		_subscribe = false;
		
		// Check the startTime
		// If it's a valid data, if not then try to figure out if it's '2h' or similar
		if ( moment(startTime).isValid() )
		{
			if (_debug > 0)
				console.log(">>>>>>>>>>>>>>>>>>>>> startTime: VALID");

			if (endTime === "")
				endTime = "2h";
		}
		else
		{
			if (_debug > 0)
				console.log(">>>>>>>>>>>>>>>>>>>>> startTime: --not-a-valid-YYYY-MM-DD hh:mm-- startTime=|"+startTime+"|");

			var multiplier = 60;
			var localStartTime = startTime;
			if (localStartTime.match("^[0-9]+[mhdlMHDL]$")) // If value starts with a number and ends with m, h, d  m=minutes, h=hour, d=day
			{
				var lastChar = localStartTime.substring(localStartTime.length-1).toLowerCase();
				if ("m" === lastChar) multiplier = 0;       // Minutes
				if ("h" === lastChar) multiplier = 60;      // Hours
				if ("d" === lastChar) multiplier = 60 * 24; // Days
	
				localStartTime = localStartTime.substring(0, localStartTime.length-1);
			}
			var inMinutes = parseInt(localStartTime) * multiplier; 
			if (isNaN(inMinutes))
				inMinutes = 120;

			endTime	  = startTime; // Set end time to '2h' or whatever the startTime was before it was parsed
			startTime = moment().subtract(inMinutes, "minutes").format("YYYY-MM-DD HH:mm");
		}

		var multiDayChartDays = parseInt(multiDayChart); 
		if (isNaN(multiDayChartDays))
		{
			console.log("Can't parse multiDayChart='"+multiDayChart+"', into a number. Using 7 has hard coded value.");
			multiDayChartDays = -7;
		}
		
		// if TODAY and multiDayChartDays is Positive number then make it a negative number
		if (moment(startTime).isSame(moment(), 'day') && multiDayChartDays > 0)
		{
			multiDayChartDays = -Math.abs(multiDayChartDays); // positive to negative number
		}

		// use startTmp endTmp in a loop later on
		// for NEGATIVE NUMBER: move START back in time, and set END to passed
		// for POSITIVE NUMBER: move START to "startTime" and END to number of days
		var startTmp = moment(startTime);
		var endTmp   = moment(startTime);
		if (multiDayChartDays < 0)
		{
			if (_debug > 0)
				console.log(">>>>>>>>>>>>>>>>>>>>> MDC - NEGATIVE: "+multiDayChartDays);

			startTmp = startTmp.add(multiDayChartDays, 'days');
			endTmp   = moment(startTime);
		}
		else
		{
			if (_debug > 0)
				console.log(">>>>>>>>>>>>>>>>>>>>> MDC - POSITIVE: "+multiDayChartDays);

			endTmp   = startTmp.add(multiDayChartDays, 'days');
			startTmp = moment(startTime);
			
			// Do NOT display dates AFTER today
			if ( endTmp.isAfter(moment()) )
			{
				endTmp = moment();
				console.log("NOTE: altering end-time to '"+endTmp+"', due to overrunning current day.");
			}
		}

		if (_debug > 0)
			console.log(">>>>>>>>>>>>>>>>>>>>> startTmp=|"+startTmp.format("YYYY-MM-DD HH:mm")+"|, endTmp=|"+endTmp.format("YYYY-MM-DD HH:mm")+"|, startTime=|"+startTime+"|, endTime=|"+endTime+"|");

		var mdcWeekDaysArr = mdcWeekDays.split(",").map(Number);
		
		// Add entries to: startTimeArr
		while (startTmp.isSameOrBefore(endTmp))
		{
			var dayOfWeekNumber = startTmp.isoWeekday(); // make it into string
			if (mdcWeekDaysArr.includes(dayOfWeekNumber))
				startTimeArr.push( startTmp.format("YYYY-MM-DD HH:mm") );
			else
				console.log("MultiDayChart: Skipping '"+startTmp.format("YYYY-MM-DD")+"', dayOfWeekNumber="+dayOfWeekNumber+", since it's NOT part of the desired days '"+mdcWeekDaysArr+"' you want to include in the graphs.", mdcWeekDaysArr);

			// next day...
			startTmp = startTmp.add(1, 'days');
		}
	}
	if (_debug > 0)
		console.log(">>>>>>>>>>>>>>>>>>>>> startTimeArr:", startTimeArr);
	
	// Remove old data in graphs when subscribing to data. This is the age for when to remove < 0 == do not remove
	var subscribeAgeInSec = -1;
	if (_subscribe)
	{
		var multiplier = 60;
		var localStartTime = startTime;
		if (localStartTime.match("^[0-9]+[mhdlMHDL]$")) // If value starts with a number and ends with m, h, d  m=minutes, h=hour, d=day
		{
			var lastChar = localStartTime.substring(localStartTime.length-1).toLowerCase();
			if ("m" === lastChar) multiplier = 60;           // Minutes
			if ("h" === lastChar) multiplier = 60 * 60;      // Hours
			if ("d" === lastChar) multiplier = 60 * 60 * 24; // Days

			localStartTime = localStartTime.substring(0, localStartTime.length-1);
		}
		subscribeAgeInSec = parseInt(localStartTime) * multiplier; 
		if (isNaN(subscribeAgeInSec))
			subscribeAgeInSec = -1;
	}
	if (_debug > 0)
		console.log("subscribe="+subscribe+", subscribeAgeInSec="+subscribeAgeInSec);

	// Set the window/tab title name
	document.title = sessionName;

	// Chart JS: set some global stuff
//    Chart.defaults.global.legend = {
//        display: true,
//        position: 'bottom',
//        fullWidth: true,
//        reverse: false,
//    }
//    Chart.defaults.global.legend.position = 'bottom';

	// Get availabe graphs for this SERVER/SessionName
	// - create <div id='fullGraphName'>  <canvas id='canvas_fullGraphName'> </canvas>  <div>
	// - create a chart object
	// After this we do a new AJAX to get initial data for each graph

	// TODO: The below should be fetched from the database based on the 'user name' or 'session/template name'
	//       also the JSON format will look differently
	var graphProfile = [];
	// var graphProfile = [ 
	// 	{graph : 'CmSummary_aaCpuGraph'}, 
	// 	{graph : 'CmSummary_OldestTranInSecGraph'},
	// 	{graph : 'CmSummary_SumLockCountGraph'},
	// 	{graph : 'CmSummary_BlockingLocksGraph'},
	// 	{graph : 'CmProcessActivity_BatchCountGraph'}, 
	// 	{graph : 'CmProcessActivity_ExecTimeGraph'},
	// 	{graph : 'CmSpidWait_spidClassName'}, 
	// 	{graph : 'CmSpidWait_spidWaitName'},
	// 	{graph : 'CmExecutionTime_TimeGraph'},
	// 	{graph : 'CmSysLoad_EngineRunQLengthGraph'},
	// 	{graph : 'CmStatementCache_RequestPerSecGraph'},
	// 	{graph : 'CmOsIostat_IoWait'},
	// 	{graph : 'CmOsUptime_AdjLoadAverage'}
	// ];

	if (graphList === undefined)
	{
		//---------------------------------------------------
		// Get: 
		//---------------------------------------------------
		let url = '/api/graph/profiles?name='+sessionName;
		if (_debug > 0)
			console.log("AJAX Call: "+url);
		$("#api-feedback").html("Loading Graphs PROFILES from url: "+url);

		$.ajax(
		{
			url: url,
			type: 'get',
			async: false,   // (async: true = do it in the background)
			success: function(data, status) 
			{
				if (data === "")
					console.log("No profile was found in the db... continuing...");
				else
				{
					try {
						const jsonResp = JSON.parse(data);
						if (_debug > 4)
							console.log("JSON PARSED DATA: ", jsonResp);
		
						graphProfile = jsonResp;
					} catch (error) {
						console.log("Problems Persing the JSON profile fetched from db.");
					}
				}
			},
			error: function(xhr, desc, err) 
			{
				console.log(xhr);
				console.log("Details: " + desc + "\nError: " + err);
				$("#api-feedback").html("<strong>ERROR Details:</strong> " + desc + "<br /><strong>Error:</strong> " + err + "<br /><strong>ResponseText:</strong> " + xhr.responseText + "<br />");
				$("#api-feedback").css("color", "red");
			}
		}); // end ajax call
	}
	else
	{
		console.log("Using 'graphList' as 'graphProfile': graphList='"+graphList+"'.");
		graphList = graphList.trim();

		// This is a JSON Object, so parse it
		if (graphList.startsWith("[") || graphList.startsWith("{"))
		{
			try {
				graphProfile = JSON.parse(graphList);
			} catch (error) {
				console.log("Parsing 'graphList' Error: " + error);
				$("#api-feedback").html('Error parsing "graphList". <br>Expected value is a JSON object.<br>Your input: <code>'+graphList+'</code><br>Example: <code>graphList=[{"srv":"GORAN_UB3_DS","graph":"CmSummary_OldestTranInSecGraph"},{"srv":"GORAN_UB2_DS","graph":"CmSummary_OldestTranInSecGraph"}]</code><br>or: <code>graphList=all</code><br>or: <code>graphList=name1,name2</code>');
				$("#api-feedback").css("color", "red");
				return;
			}
		}
		else // Try to read it as a comma separated liss and create a graphProfile
		{
			let graphNameArr = graphList.split(",");
			graphProfile = [];
			for (let i=0; i<graphNameArr.length; i++) 
			{
				var graphEntry = {};
				var tmpStrEntry = graphNameArr[i];

				graphEntry.graphLineProps = {}; // Initialize with empty object

				// Plain graph name (no specifications/properties)
				if ( ! tmpStrEntry.includes("[") )
			//	if ( ! (tmpStrEntry.indexOf("[") !== -1) )
				{
					graphEntry.graph = tmpStrEntry;
					console.log("DEBUG: tmpStrEntry="+tmpStrEntry);
				}
				// If the "graphName" contains any "specifications" (if specififc "chart lines" are hidden or visible, handle them here)
				// a specification looks like: graphList=CmXXX_graph1[+line1;-line2;],CmXXX_graph2[-line2]
				//                                                   ^^^^^^^^^^^^^^^^             ^^^^^^^^
				else
				{
					// First copy/separate the: graphName and then properties into two different variables
					const tmpStrGraphName  = tmpStrEntry.substring(0, tmpStrEntry.indexOf("["));
					const tmpStrGraphProps = tmpStrEntry.substring(tmpStrEntry.indexOf("[") + 1, tmpStrEntry.lastIndexOf("]"));
					
					console.log("DEBUG: tmpStrGraphName="+tmpStrGraphName+", tmpStrGraphProps="+tmpStrGraphProps);
					
					// TODO: parse tmpStrGraphProps into a "Object"
					let tmpGraphPropArr = tmpStrGraphProps.split(";");
					let tmpGraphPropObj = { hideCol:[], showCol:[] }; // empty Object
					for (let i=0; i<tmpGraphPropArr.length; i++) 
					{
						var tmpPropEntry = tmpGraphPropArr[i];
						
						// FIXME: Check what to do with this entry... 
						if (tmpPropEntry.startsWith("-"))
							tmpGraphPropObj.hideCol.push(tmpPropEntry.substring(1)); // Remove the '-' char
						else if (tmpPropEntry.startsWith("+"))
							tmpGraphPropObj.showCol.push(tmpPropEntry.substring(1)); // Remove the '+' char
						else
							tmpGraphPropObj.showCol.push(tmpPropEntry);
					}
					
					graphEntry.graph = tmpStrGraphName;
					graphEntry.graphLineProps = tmpGraphPropObj; // FIXME: This object should later on be passed into: new DbxGraph(... entry.graphLineProps, ...) the DbxGraph Object should handle "the rest"
				}
				
				// Add the object to the profile
				graphProfile.push(graphEntry);
			}
		}
		console.log("graphProfile="+graphProfile, graphProfile);
	}

	//---------------------------------------------------
	// create a list of SERVERS (from the profile) that we will need to get GraphInformation about:
	//---------------------------------------------------
	// const serverList = [];
	if (sessionName !== undefined)
	{
		// "all", go and get all server names from DbxCentral
		if (sessionName === "all" || sessionName === "ALL")
		{
			$.ajax(
			{
				url: "api/sessions",
				type: 'get',
				async: false,   // to call it one by one (async: true = spawn away a bunch of them in the background)
					
				success: function(data, status) 
				{
					// console.log("RECEIVED DATA: "+data);
					var jsonResp = JSON.parse(data);
						
					for (var i=0; i<jsonResp.length; i++) {
						var entry = jsonResp[i];
			
						_serverList.push(entry.serverName);
					}
				},
				error: function(xhr, desc, err) 
				{
					console.log(xhr);
					console.log("Details: " + desc + "\nError: " + err);
				}
			}); // end: ajax call
		}
		else
		{
			// Take the input parameter(s) and put them into global_serverList
			_serverList = sessionName.split(",");
		}
	}

	for (let i=0; i<graphProfile.length; i++) 
	{
		if ( ! graphProfile[i].hasOwnProperty("srv") )
			continue;

		const serverName = graphProfile[i].srv;

		if (_serverList.indexOf(serverName) == -1)
			_serverList.push(serverName);
	}
	console.log("Unique Server List: "+_serverList);

	// Set the window/tab title name
	document.title = _serverList;


	//---------------------------------------------------
	// LOAD "graph information" from all servers: 
	// Then create graph objects that are stuffed in _graphMap
	//---------------------------------------------------
	// var url = '/api/graphs?sessionName='+sessionName;
	var url = '/api/graphs?sessionName='+_serverList;
	if (_debug > 0)
		console.log("AJAX Call: "+url);
	$("#api-feedback").html("Loading Graphs NAMES from url: "+url);

	$.ajax(
	{
		url: url,
		type: 'get',
//		async: false,   // (async: true = do it in the background)
		success: function(data, status) 
		{
			const jsonResp = JSON.parse(data);
			if (_debug > 4)
				console.log("JSON PARSED DATA: ", jsonResp);

			// In what order should the graphs be created/loaded
			let createOrder = createGraphLoadOrder(graphProfile, jsonResp, loadAllGraphs);

			if ( mdcPivot === false)
			{
				console.log("Graph layout: mdcPivot="+mdcPivot+", ---Normal--- layout. First=graphList, Then=startTimeArr/mdc");
			
				for(let i = 0; i < createOrder.length; i++) 
				{
					const entry = createOrder[i];
					
					//const initStartTime   = startTime;
					const initEndTime     = endTime;
					const initSampleType  = sampleType;
					const initSampleValue = sampleValue;
					const isMultiDayChart = startTimeArr.length > 1;

					// loop the Multi Day Chart (at least 1 entry in this array)
					for(let s = 0; s < startTimeArr.length; s++)
					{
						var startTimeEntry = startTimeArr[s];
						
						// Create graph object
						const dbxGraph = new DbxGraph(
							destinationDivId,  // put it in "div" that has id
							entry.serverName,
							entry.cmName,
							entry.graphName,
							entry.graphLabel,
							entry.graphProps,
							entry.graphCategory,
							entry.percentGraph,
							entry.graphLineProps,
							subscribeAgeInSec,
							gheight,
							gwidth,
							debug,
							startTimeEntry,
							initEndTime,
							initSampleType,
							initSampleValue,
							isMultiDayChart,
							markStartTime,
							markEndTime
						);

						// add it to the global list/map
						_graphMap.push(dbxGraph);
						
						// Add it to the observer instance
						_graphObserver.observe(dbxGraph._chartDiv);
					}
				} // end: for loop
			}
			else
			{
				console.log("Graph layout: mdcPivot="+mdcPivot+", ---PIVOT--- layout. First=startTimeArr/mdc, Then=graphList");

				// loop the Multi Day Chart (at least 1 entry in this array)
				for(let s = 0; s < startTimeArr.length; s++)
				{
					var startTimeEntry = startTimeArr[s];
						
					for(let i = 0; i < createOrder.length; i++) 
					{
						const entry = createOrder[i];
						
						//const initStartTime   = startTime;
						const initEndTime     = endTime;
						const initSampleType  = sampleType;
						const initSampleValue = sampleValue;
						const isMultiDayChart = startTimeArr.length > 1;

						// Create graph object
						const dbxGraph = new DbxGraph(
							destinationDivId,  // put it in "div" that has id
							entry.serverName,
							entry.cmName,
							entry.graphName,
							entry.graphLabel,
							entry.graphProps,
							entry.graphCategory,
							entry.percentGraph,
							entry.graphLineProps,
							subscribeAgeInSec,
							gheight,
							gwidth,
							debug,
							startTimeEntry,
							initEndTime,
							initSampleType,
							initSampleValue,
							isMultiDayChart,
							markStartTime,
							markEndTime
						);

						// add it to the global list/map
						_graphMap.push(dbxGraph);
					} // end: for loop
				}
			}

			//-------------------------------------------------------------------
			// Load initial data for each of the created graphs in: _graphMap	
			//-------------------------------------------------------------------
			loadDataForGraph(0);

		},
		error: function(xhr, desc, err) 
		{
			console.log(xhr);
			console.log("Details: " + desc + "\nError: " + err);
			$("#api-feedback").html("<strong>ERROR Details:</strong> " + desc + "<br /><strong>Error:</strong> " + err + "<br /><strong>ResponseText:</strong> " + xhr.responseText + "<br />");
			$("#api-feedback").css("color", "red");
		}
	}); // end ajax call
	// $("#api-feedback").html("SUCCESS: Graphs NAMES.");

} // end: function


/**
 * Get the DbxGraph object from the global array by name
 * 
 * @param {*} name    The name of the graph
 * @returns The DbxGraph Object
 */
function getGraphByName(name)
{
	for(var i=0; i<_graphMap.length; i++)
	{
		const dbxGraph = _graphMap[i];
		if (dbxGraph.getFullName() === name)
			return dbxGraph;
	}
	return null;
}

/**
 * create a object with the correct order that the graphs should be created/loaded
 * 
 * @param {*} graphProfile    Any profileObject from the "dbxcentral-db" or as a url parameter
 * @param {*} graphObjects    All available graph objects available for the specified server(s)
 * @param {*} loadAllGraphs   if we should just load everything...
 * @returns In what order the graphs sould be created/loaded
 */
function createGraphLoadOrder(graphProfile, graphObjects, loadAllGraphs)
{
	if (_debug >= 1)
		console.log("createGraphLoadOrder(graphProfile, graphObjects, loadAllGraphs): called with...", graphProfile, graphObjects, loadAllGraphs);

	if (loadAllGraphs)
		return graphObjects;

	// This is what will be returned from this function
	const retData = [];

	// If NO graph profile, return graphObjects with graphObjects.visibleAtStartup == true
	if (graphProfile === undefined || graphProfile.length === 0)
	{
		for(let i = 0; i < graphObjects.length; i++) 
		{
			const entry = graphObjects[i];
			let addThisGraph = false;

			// show: visibleAtStartup
			if ( entry.visibleAtStartup === true )
				addThisGraph = true;

			// show: User defined Counter Model
			if ( ! entry.cmName.startsWith("Cm") )
				addThisGraph = true;
				
			if ( addThisGraph )
				retData.push(entry);
		}

		if (_debug >= 1)
			console.log("createGraphLoadOrder(graphProfile, graphObjects, loadAllGraphs): returns(no-profile): ", retData);

		return retData; // <<<<----<<<<----<<<<---- RETURN <<<<----<<<<----<<<<----<<<<----
	}


	// We HAVE a profile, then: only add graphs within the profile
	for(let i = 0; i < graphProfile.length; i++) 
	{
		const fullGraphName  = graphProfile[i].graph;
		const graphLineProps = graphProfile[i].graphLineProps;
		var   serverList = [];
		
		if (graphProfile[i].hasOwnProperty("srv"))
		{
			// possibly parse comma separated list into array
			serverList = graphProfile[i].srv.split(",");
			//serverList.push(graphProfile[i].srv);
		}
		else
		{
			serverList = _serverList; // use all Servers from the GLOBAL serverList (which is sessionName=SRV[,SRV2])
		}
		//console.log("serverList.length="+serverList.length+", serverList=|"+serverList+"|", serverList);

		for(let j = 0; j < graphObjects.length; j++) 
		{
			const entry = graphObjects[j];
			
			for(let k = 0; k < serverList.length; k++) 
			{
				const serverName = serverList[k];
				//console.log("entry.tableName=|"+entry.tableName+"|, fullGraphName=|"+fullGraphName+"|, entry.serverName=|"+entry.serverName+"|, serverName=|"+serverName+"|, entry:", entry);

				if (entry.tableName === fullGraphName && entry.serverName === serverName) 
				{
					entry.graphLineProps = graphLineProps;
					retData.push(entry);
				}
			}
		}
	}

	if (_debug >= 1)
		console.log("createGraphLoadOrder(graphProfile, graphObjects, loadAllGraphs): returns: ", retData);

	return retData;
} // end: function


/**
 * Load a graph (or actually load "all" graphs that are in the _graphMap)
 * - At the end this function calls itself with the next index in the _graphMap
 * - When there are no-more-graphs-to-load we call: graphLoadIsComplete()
 * 
 * @param {*} indexToLoad 
 * @param {*} startTime 
 * @param {*} endTime 
 * @param {*} sampleType 
 * @param {*} sampleValue 
 */
function loadDataForGraph(indexToLoad)
{
	if (indexToLoad >= _graphMap.length)
		return;

	const dbxGraph = _graphMap[indexToLoad];

	const serverName  = dbxGraph.getServerName();
	const cmName      = dbxGraph.getCmName();
	const graphName   = dbxGraph.getGraphName();
	const startTime   = dbxGraph.getInitStartTime();
	const endTime     = dbxGraph.getInitEndTime();
	const sampleType  = dbxGraph.getInitSampleType();
	const sampleValue = dbxGraph.getInitSampleValue();

	const url         = '/api/graph/data?sessionName='+serverName+'&cmName='+cmName+'&graphName='+graphName+"&startTime="+startTime+"&endTime="+endTime+"&sampleType="+sampleType+"&sampleValue="+sampleValue;

	$("#api-feedback").html("Loading: "+(indexToLoad+1)+"/"+_graphMap.length+" - "+serverName+" - "+cmName+"_"+graphName);
	if (_debug > 0)
		console.log("AJAX Call: "+url);

	// $( "#progressbar" ).html(i+'% - START - cmName='+cmName+', graphName='+graphName);
	// $('#progressBar').css('width', '0%');
	$.ajax(
	{
		url: url,
		type: 'get',
		// async: false,   // to call it one by one (async: true = spawn away a bunch of them in the background)
		
		success: function(data, status) 
		{
			// console.log("RECEIVED DATA: "+data);
			var jsonResp = JSON.parse(data);
			
			console.time('dbxGraph.addData:: cmName='+cmName+', graphName='+graphName+', startTime='+startTime+', endTime='+endTime);
			dbxGraph.addData(jsonResp);
			console.timeEnd('dbxGraph.addData:: cmName='+cmName+', graphName='+graphName+', startTime='+startTime+', endTime='+endTime);

			// Do stuff after a graph has been loaded
			dbxGraph.onLoadCompetion();
			
			if (_debug > 0)
				console.log('DONE: loading data for cmName='+cmName+', graphName='+graphName);

			// Also do this if error happened, otherwise we wont load anymore data
			const pct = Math.round( (indexToLoad+1) / _graphMap.length * 100);
			$('#progressBar').css('width', pct+'%');
			$('#progressBar').html(pct+'%');
			// $('#progressDiv').html("Done loading: "+cmName+' - '+graphName);
			if (pct === 100)
			{
				graphLoadIsComplete();
			}
			else
			{
				// Load next GRAPH in 1 ms
				setTimeout( loadDataForGraph(indexToLoad + 1), 1);
			}
		},
		error: function(xhr, desc, err) 
		{
			console.log(xhr);
			console.log("Details: " + desc + "\nError: " + err);
			$("#feedback_"+cmName+"_"+graphName).html("<strong>ERROR Details:</strong> " + desc + "<br /><strong>Error:</strong> " + err + "<br /><strong>ResponseText:</strong> " + xhr.responseText + "<br />");
//				$("#api-feedback").html("<strong>ERROR Details:</strong> " + desc + "<br /><strong>Error:</strong> " + err + "<br /><strong>ResponseText:</strong> " + xhr["responseText"] + "<br />");
//				$("#dialog-tab-prevReviewNote").html("<strong>ERROR Details:</strong> " + desc + "<br /><strong>Error:</strong> " + err + "<br /><strong>ResponseText:</strong> " + xhr["responseText"] + "<br />");
//				$("#dialog-tab-prevReviewNote").css("color", "red");

			// Also do this if error happened, otherwise we wont load anymore data
			const pct = Math.round( (indexToLoad+1) / _graphMap.length * 100);
			$('#progressBar').css('width', pct+'%');
			$('#progressBar').html(pct+'%');
			// $('#progressDiv').html("Done loading: "+cmName+' - '+graphName);
			if (pct === 100)
			{
				graphLoadIsComplete();
			}
			else
			{
				// Load next GRAPH in 1 ms
				setTimeout( loadDataForGraph(indexToLoad + 1), 1);
			}
		}
	}); // end: ajax call
}

/** 
 * Call when all graphs has been initially loaded
*/
function graphLoadIsComplete()
{
	$('#progressDiv').hide();
	$("#api-feedback").html("Loaded "+(_graphMap.length)+" Graphs.");

	// Check if we got any alarms that are active
	alarmPanelLiveRefresh(null);

	// Update various info in NavBar: like First/Last time stamp
	updateNavbarInfo();

	setTimeout(function(){ $('#dbx-feedback').hide(); }, 3000); // hide after 10s

	// Simulate a windows resize to redraw the "whole thing" (so that graphs that was "grown" in height gets "redrawn")
	//window.dispatchEvent(new Event('resize'));
	//setTimeout(function(){ window.dispatchEvent(new Event('resize')); }, 500);

	// Since the above 'window.dispatchEvent(new Event('resize'));' didn't work... 
	// Resize all graph object to make them the correct size (if number of labels is to many, the "add" method resizes the div, but the graph objects still needs the "refresh" at the end)
	for(var i=0; i<_graphMap.length; i++)
	{
		const dbxGraph = _graphMap[i];
		dbxGraph._chartObject.resize();
	}

	/* If we have 'markTime' then ebale historical mode, and set the position in the timeline */
	var markTime = getParameter("markTime");
	if (markTime !== undefined)
	{
		dbxHistoryAction();
		dbxHistoryAction(markTime);
	}

//	var markStartTime = getParameter("markStartTime");
//	var markEndTime   = getParameter("markEndTime");
//	if ( markStartTime !== undefined && markEndTime !== undefined && markStartTime !== '' && markEndTime !== '')
//	{
//		for(var i=0; i<_graphMap.length; i++)
//		{
//			const dbxGraph = _graphMap[i];
//
//			dbxGraph.setMarkerStartEndTime(markStartTime, markEndTime);
//		}
//	}

	if (_debug > 0)
		console.log("DONE: loading ALL data...");
}

/** 
 * update various NavBar Information
*/
function updateNavbarInfo()
{
	if (_graphMap.length === 0)
	{
		console.log("updateNavbarInfo: nothing todo - _graphMap.length="+_graphMap.length);
		return;
	}

	// var oldestTs = 'unknown'; // getOldestTs() { return this._chartObject.data.labels[0]; }
	// var newestTs = 'unknown'; // getNewestTs() { return this._chartObject.data.labels[this._chartObject.data.labels.length-1]; }

	// Loop all graphs and get oldest newest timestamps
	var oldestTs = _graphMap[0].getOldestTs();
	var newestTs = _graphMap[0].getNewestTs();
	for(var i=0; i<_graphMap.length; i++)
	{
		const dbxGraph = _graphMap[i];

		//console.log("GRAPH_LOOP: oldestTs: "+oldestTs+", newestTs: "+newestTs);

		oldestTs = dbxMinDt(oldestTs, dbxGraph.getOldestTs());
		newestTs = dbxMaxDt(newestTs, dbxGraph.getNewestTs());
	}

	// if we got oldestTs...
	if (typeof oldestTs !== 'undefined') 
	{
		$("#dbx-start-time").html(oldestTs.format("YYYY-MM-DD HH:mm"));
		$("#dbx-end-time"  ).html(newestTs.format("YYYY-MM-DD HH:mm"));

		// today: only print HH:MM, else: YYYY-MM-DD HH:mm
		if (moment(oldestTs).isSame(moment(), 'day') && moment(newestTs).isSame(moment(), 'day'))
			$('#dbx-report-range span').html(oldestTs.format('HH:mm') + ' - ' + newestTs.format('HH:mm'));
		else
			$('#dbx-report-range span').html(oldestTs.format('YYYY-MM-DD HH:mm') + ' - ' + newestTs.format('YYYY-MM-DD HH:mm'));

		var hourDiff = Math.round(newestTs.diff(oldestTs, 'hours', true));
		$("#dbx-sample-time").html(hourDiff+"h");
		

		console.log("oldestTs: "+oldestTs+" ["+oldestTs.format("YYYY-MM-DD HH:mm:ss")+"], newestTs: "+newestTs+" ["+newestTs.format("YYYY-MM-DD HH:mm:ss")+"], hourDiff="+hourDiff);
	}
	// window.dispatchEvent(new Event('resize'));
}

//-----------------------------------------------------------
// DBMS CONFIG PANEL
//-----------------------------------------------------------

var _dcSrvName   = null;   // server name currently displayed
var _dcTimestamp = null;   // timestamp currently displayed
var _dcData      = null;   // last loaded JSON response
var _dcMainTab   = 'params';  // 'params' | 'texts' | 'issues'
var _dcTextTab   = null;   // currently selected configName sub-tab
var _dcFilter    = '';     // active filter text

/** Toggle the DBMS Config panel open/closed */
function dbmsConfigToggle()
{
	var $panel = $('#dbms-config-panel');
	if ($panel.is(':visible')) {
		dbmsConfigClose();
		return;
	}

	// On first open: center horizontally, near the top (just below the navbar)
	if (!_dcSrvName) {
		$panel.css('display', 'flex'); // must be visible to measure width
		var panelW = $panel.outerWidth();
		var left   = Math.max(0, Math.round((window.innerWidth - panelW) / 2));
		$panel.css({ top: '100px', left: left + 'px' });
	}

	$panel.css('display', 'flex');

	// Restore dark mode
	var saved = getStorage('dbxtune_checkboxes_').get('dbms-config-dark-chk');
	var dark  = (saved === 'checked') ? true
	          : (saved === 'not')     ? false
	          : (_colorSchema === 'dark');
	$('#dbms-config-dark').prop('checked', dark);
	if (dark) $panel.addClass('dc-dark'); else $panel.removeClass('dc-dark');

	// Auto-load on every open
	dbmsConfigRefresh();
}

/** Reload DBMS config data for the current (or best-available) server + timestamp */
function dbmsConfigRefresh()
{
	var srv = _dcSrvName || (_serverList && _serverList.length > 0 ? _serverList[0] : null);
	var ts;
	if (isHistoryViewActive()) {
		ts = _dcTimestamp || _cmTimestamp;
		if (!ts) {
			var txt = ($('#dbx-history-slider-center-text').text() || '').trim();
			ts = txt.split(' - ')[0].trim();
		}
	} else {
		ts = _dcTimestamp || moment().format('YYYY-MM-DD HH:mm:ss');
	}
	if (srv && ts && ts.match(/^\d{4}-\d{2}-\d{2}/))
		dbmsConfigLoad(srv, ts);
}

/** Close and reset the DBMS Config panel */
function dbmsConfigClose()
{
	$('#dbms-config-panel').hide();
	_dcSrvName = _dcTimestamp = _dcData = _dcTextTab = null;
	_dcMainTab = 'params';
	_dcFilter  = '';
	$('#dbms-config-text-subtabs').hide().empty();
	$('#dbms-config-filter-bar').hide();
	$('#dbms-config-filter-input').val('');
	$('#dbms-config-filter-count').text('');
	$('#dbms-config-content').html('');
	// Reset main tab active state
	$('#dbms-config-tabs-main .nav-link').removeClass('active');
	$('#dbms-config-tabs-main .nav-link').first().addClass('active');
}

/** Toggle dark mode for the DBMS Config panel */
function dbmsConfigDarkToggle(on)
{
	if (on) $('#dbms-config-panel').addClass('dc-dark');
	else    $('#dbms-config-panel').removeClass('dc-dark');
	getStorage('dbxtune_checkboxes_').set('dbms-config-dark-chk', on ? 'checked' : 'not');
}

/** Load DBMS config data from the API for the given server+timestamp */
function dbmsConfigLoad(srvName, timestamp)
{
	if (!srvName || !timestamp) return;
	_dcSrvName   = srvName;
	_dcTimestamp = timestamp;
	$('#dbms-config-srv').text('[' + srvName + ']');
	$('#dbms-config-ts' ).text('@ ' + timestamp);
	$('#dbms-config-loading').show();
	$('#dbms-config-content').html('');

	$.ajax({
		url:      '/api/cc/mgt/dbms-config',
		data:     { srv: srvName, ts: timestamp },
		dataType: 'text',
		success: function(data) {
			$('#dbms-config-loading').hide();
			try {
				var r = JSON.parse(data);
				if (r.error) { dbmsConfigShowMsg(r.message || r.error); return; }
				_dcData = r;
				// Show the actual capture time (SessionStartTime) from the response
				var captureTs = r.resolvedTs || r.ts || _dcTimestamp;
				$('#dbms-config-ts').text('Captured: ' + captureTs);
				dbmsConfigRender(_dcMainTab);
			} catch(ex) { dbmsConfigShowMsg('Parse error: ' + ex); }
		},
		error: function(xhr) {
			$('#dbms-config-loading').hide();
			dbmsConfigShowMsg('HTTP ' + xhr.status + ': ' + xhr.statusText);
		}
	});
}

/** Render the currently selected main tab */
function dbmsConfigRender(tab)
{
	_dcMainTab = tab;
	_dcFilter  = '';
	$('#dbms-config-filter-input').val('');
	$('#dbms-config-filter-count').text('');

	// Highlight the correct main tab link
	$('#dbms-config-tabs-main .nav-link').removeClass('active');
	$('#dbms-config-tabs-main .nav-link').each(function() {
		if ($(this).attr('onclick') && $(this).attr('onclick').indexOf("'" + tab + "'") >= 0)
			$(this).addClass('active');
	});

	if (!_dcData) return;

	if (tab === 'params') {
		$('#dbms-config-text-subtabs').hide().empty();
		$('#dbms-config-filter-bar').css('display','flex');
		dbmsConfigRenderParams(_dcData.params);
	} else if (tab === 'texts') {
		$('#dbms-config-filter-bar').hide();
		dbmsConfigRenderTexts(_dcData.texts);
	} else if (tab === 'issues') {
		$('#dbms-config-text-subtabs').hide().empty();
		$('#dbms-config-filter-bar').css('display','flex');
		dbmsConfigRenderIssues(_dcData.issues);
	}
}

/** Handle main tab click */
function dbmsConfigMainTabClick(evt, tab)
{
	evt.preventDefault();
	dbmsConfigRender(tab);
}

// ── Parameters tab ──────────────────────────────────────────────────────────

function dbmsConfigRenderParams(params)
{
	if (!params || !params.columns || params.columns.length === 0) {
		$('#dbms-config-content').html('<div class="p-3 text-muted">No parameter data available.</div>');
		return;
	}
	// Default filter: show only non-default values (if the column exists)
	var hasNonDefault = params.columns.some(function(c) { return c.toLowerCase() === 'nondefault'; });
	var defaultFilter = hasNonDefault ? 'where NonDefault = true' : '';
	_dcFilter = defaultFilter;
	$('#dbms-config-filter-input').val(defaultFilter);
	dbmsConfigApplyFilterToData(params.columns, params.rows, 'dc-params-table', defaultFilter);
}

// ── Text Snippets tab ────────────────────────────────────────────────────────

function dbmsConfigRenderTexts(texts)
{
	var $st = $('#dbms-config-text-subtabs').css('display','flex').empty();

	if (!texts || texts.length === 0) {
		$('#dbms-config-content').html('<div class="p-3 text-muted">No text snippets available.</div>');
		return;
	}

	// Build sub-tabs
	texts.forEach(function(t, idx) {
		var name  = t.configName || ('snippet-' + idx);
		var label = t.tabLabel   || name;   // human-readable label, falls back to configName
		var isFirst = (idx === 0);
		if (idx === 0) _dcTextTab = name;
		$st.append('<li class="nav-item"><a class="nav-link' + (isFirst ? ' active' : '') + '" href="#"'
			+ ' onclick="dbmsConfigTextTabClick(event,\'' + name.replace(/'/g,"\\'") + '\');return false;">'
			+ escHtml(label) + '</a></li>');
	});

	// Show first tab's content
	if (texts.length > 0)
		dbmsConfigShowTextContent(texts[0]);
}

function dbmsConfigTextTabClick(evt, configName)
{
	evt.preventDefault();
	_dcTextTab = configName;
	$('#dbms-config-text-subtabs .nav-link').removeClass('active');
	$('#dbms-config-text-subtabs .nav-link').each(function() {
		if ($(this).text() === configName) $(this).addClass('active');
	});
	if (_dcData && _dcData.texts) {
		var found = null;
		for (var i = 0; i < _dcData.texts.length; i++) {
			if (_dcData.texts[i].configName === configName) { found = _dcData.texts[i]; break; }
		}
		if (found) dbmsConfigShowTextContent(found);
	}
}

/**
 * Render a single text snippet entry.
 * ASCII tables (isql-style) are converted to sortable/filterable HTML tables.
 * Other text is shown in a <pre>.
 */
function dbmsConfigShowTextContent(entry)
{
	var text = entry.configText || '';
	if (!text) {
		$('#dbms-config-content').html('<div class="p-3 text-muted">No content.</div>');
		return;
	}

	var segments = parseAsciiContent(text);
	var html = '';
	segments.forEach(function(seg, idx) {
		if (seg.type === 'table') {
			html += renderAsciiTable(seg, 'dc-ascii-' + idx);
		} else {
			html += '<pre style="white-space:pre-wrap;font-size:0.82em;border:1px solid #dee2e6;border-radius:3px;padding:6px;margin-bottom:6px;">'
				+ escHtml(seg.text) + '</pre>';
		}
	});
	$('#dbms-config-content').html(html || '<div class="p-3 text-muted">Empty.</div>');
}

// ── Issues tab ───────────────────────────────────────────────────────────────

function dbmsConfigRenderIssues(issues)
{
	if (!issues || !issues.columns || issues.columns.length === 0 ||
		(issues.rows && issues.rows.length === 0 && issues.columns.length > 0)) {
		var emptyMsg = (!issues || !issues.columns || issues.columns.length === 0)
			? 'No issues data available.'
			: 'No configuration issues detected.';
		$('#dbms-config-content').html('<div class="p-3 text-muted">' + emptyMsg + '</div>');
		dbmsConfigApplyFilter('');
		return;
	}
	var html = dbmsConfigBuildTable('dc-issues-table', issues.columns, issues.rows);
	$('#dbms-config-content').html(html);
	dbmsConfigApplyFilter('');
}

// ── Shared table builder ─────────────────────────────────────────────────────

/**
 * Build a Bootstrap table with sortable column headers and filter support.
 * The filter bar (#dbms-config-filter-bar) must already be shown before calling.
 */
function dbmsConfigBuildTable(tableId, columns, rows)
{
	var html = '<table id="' + tableId + '" class="table table-sm table-bordered table-hover" style="font-size:0.82em;white-space:nowrap;">';
	html += '<thead class="thead-light"><tr>';
	columns.forEach(function(col, idx) {
		html += '<th style="cursor:pointer;user-select:none;" onclick="dbmsConfigSortTable(\'' + tableId + '\',' + idx + ');">'
			+ escHtml(col) + ' <span class="sort-icon" style="font-size:0.75em;color:#999;"></span></th>';
	});
	html += '</tr></thead><tbody>';
	if (!rows || rows.length === 0) {
		html += '<tr><td colspan="' + columns.length + '" class="text-center text-muted">No rows</td></tr>';
	} else {
		rows.forEach(function(row) {
			html += '<tr>';
			row.forEach(function(val) {
				html += '<td>' + renderCell(val) + '</td>';
			});
			html += '</tr>';
		});
	}
	html += '</tbody></table>';
	return html;
}

/** Sort a table by column index (cycle: asc → desc → original) */
function dbmsConfigSortTable(tableId, colIdx)
{
	var $tbl  = $('#' + tableId);
	var $th   = $tbl.find('thead th').eq(colIdx);
	var cur   = $th.data('sort') || 0;  // 0=none, 1=asc, 2=desc
	var next  = (cur + 1) % 3;
	$th.data('sort', next);

	// Reset other headers
	$tbl.find('thead th').not($th).data('sort', 0)
		.find('.sort-icon').text('');
	$th.find('.sort-icon').text(next === 1 ? ' ▲' : next === 2 ? ' ▼' : '');

	var $tbody = $tbl.find('tbody');
	var rows   = $tbody.find('tr').toArray();

	if (next === 0) {
		// Restore original order (stored as data-orig-idx)
		rows.sort(function(a, b) {
			return ($(a).data('orig-idx') || 0) - ($(b).data('orig-idx') || 0);
		});
	} else {
		rows.sort(function(a, b) {
			var av = $(a).find('td').eq(colIdx).text();
			var bv = $(b).find('td').eq(colIdx).text();
			var an = parseFloat(av), bn = parseFloat(bv);
			if (!isNaN(an) && !isNaN(bn))
				return next === 1 ? an - bn : bn - an;
			return next === 1 ? av.localeCompare(bv) : bv.localeCompare(av);
		});
	}

	// Remember original order on first sort
	$tbody.find('tr').each(function(i) {
		if ($(this).data('orig-idx') === undefined) $(this).data('orig-idx', i);
	});

	$tbody.empty().append(rows);
	// Re-apply filter after sort
	dbmsConfigApplyFilter(_dcFilter);
}

/** Called from the filter input — re-renders the active params/issues table with the new filter */
function dbmsConfigApplyFilter(text)
{
	_dcFilter = text;
	if (!_dcData) return;

	if (_dcMainTab === 'params' && _dcData.params) {
		dbmsConfigApplyFilterToData(_dcData.params.columns, _dcData.params.rows, 'dc-params-table', text);
	} else if (_dcMainTab === 'issues' && _dcData.issues) {
		dbmsConfigApplyFilterToData(_dcData.issues.columns, _dcData.issues.rows, 'dc-issues-table', text);
	}
}

/**
 * Filter columns/rows and rebuild the table inside #dbms-config-content.
 * Supports:
 *   - "where col = val"  SQL-style (reuses cmDetailWhereFilter)
 *   - plain regex against full row text
 */
function dbmsConfigApplyFilterToData(columns, rows, tableId, text)
{
	var result = applyRowFilter(rows || [], columns, text);
	var filteredRows = result.filteredRows;
	var filterError  = result.filterError;
	var filter = (text || '').trim();

	var total   = (rows || []).length;
	var visible = filteredRows.length;

	var html = dbmsConfigBuildTable(tableId, columns, filteredRows);
	$('#dbms-config-content').html(html);

	if (filter)
		$('#dbms-config-filter-count').text(filterError || ('Showing ' + visible + ' / ' + total));
	else
		$('#dbms-config-filter-count').text(total + ' rows');
}

// ── ASCII table parser ───────────────────────────────────────────────────────

/**
 * Parse isql-style ASCII output into an array of segments:
 *   { type: 'table', headers: [...], rows: [[...], ...] }
 *   { type: 'text',  text: '...' }
 *
 * Isql format (example):
 *   colA    colB    colC
 *   ------- ------- ------
 *   value1  value2  value3
 */
/** Split a pipe-delimited row:  |val1|val2|val3|  →  ['val1','val2','val3'] */
function splitPipeRow(line)
{
	var s = line.trim();
	if (s.charAt(0) === '|') s = s.slice(1);
	if (s.charAt(s.length - 1) === '|') s = s.slice(0, -1);
	return s.split('|').map(function(c) { return c.trim(); });
}

/**
 * Parse text that may contain one or more ASCII tables into segments.
 *
 * Handles two formats:
 *
 * 1. Box format (SAP ASE / MySQL isql):
 *      +--------+------+
 *      |ColA    |ColB  |
 *      +--------+------+
 *      |val1    |val2  |
 *      +--------+------+
 *      Rows N              ← optional trailing line
 *
 * 2. Dash-separator format (older isql):
 *      ColA    ColB
 *      ------- ----
 *      val1    val2
 *
 * Returns array of  { type:'table', headers:[...], rows:[[...], ...] }
 *                or { type:'text',  text:'...' }
 */
function parseAsciiContent(text)
{
	if (!text) return [{ type: 'text', text: '' }];

	var lines    = text.split('\n');
	var segments = [];
	var i        = 0;

	function isBoxSep(l)  { return /^\+[-+]+\+\s*$/.test(l.trim()); }
	function isBoxRow(l)  { return /^\|.*\|\s*$/.test(l.trim()); }
	function isDashSep(l) { var t = l.trim(); return t.length > 2 && /^[-\s]+$/.test(t) && t.indexOf('-') >= 0; }

	while (i < lines.length) {
		var trimmed = lines[i].trim();

		// ── Box-style +---+---+ table ────────────────────────────────────
		if (isBoxSep(trimmed)) {
			i++; // skip opening separator

			// Expect header row  |col1|col2|
			if (i < lines.length && isBoxRow(lines[i].trim())) {
				var headers = splitPipeRow(lines[i]);
				i++;

				// Skip separator after header
				if (i < lines.length && isBoxSep(lines[i].trim())) i++;

				// Collect data rows
				var tableRows = [];
				while (i < lines.length) {
					var rl = lines[i].trim();
					if (isBoxSep(rl))      { i++; break; }   // closing separator
					if (isBoxRow(rl))      { tableRows.push(splitPipeRow(lines[i])); i++; }
					else                    break;
				}

				// Skip optional "Rows N" line
				if (i < lines.length && /^Rows\s+\d+/i.test(lines[i].trim())) i++;

				segments.push({ type: 'table', headers: headers, rows: tableRows });
			} else {
				// Lone separator — treat as text
				segments.push({ type: 'text', text: trimmed });
			}
			continue;
		}

		// ── Dash-separator style  header\n-------\ndata ─────────────────
		if (i + 1 < lines.length && isDashSep(lines[i + 1])) {
			var headerLine = lines[i];
			var sepLine    = lines[i + 1];
			var cols = [];
			var dm, dre = /-+/g;
			while ((dm = dre.exec(sepLine)) !== null)
				cols.push({ start: dm.index, end: dm.index + dm[0].length });

			if (cols.length > 0) {
				var headers2 = cols.map(function(c) { return headerLine.substring(c.start, c.end).trim(); });
				i += 2;
				var tableRows2 = [];
				while (i < lines.length) {
					var rl2 = lines[i];
					if (rl2.trim() === '' || isDashSep(rl2)) { if (rl2.trim() === '') i++; break; }
					tableRows2.push(cols.map(function(c) {
						return rl2.length > c.start ? rl2.substring(c.start, Math.min(c.end + 1, rl2.length)).trim() : '';
					}));
					i++;
				}
				segments.push({ type: 'table', headers: headers2, rows: tableRows2 });
				continue;
			}
		}

		// ── Plain text ────────────────────────────────────────────────────
		var textLines = [];
		while (i < lines.length) {
			if (isBoxSep(lines[i].trim())) break;
			if (i + 1 < lines.length && isDashSep(lines[i + 1])) break;
			textLines.push(lines[i]);
			i++;
		}
		var t = textLines.join('\n').trim();
		if (t) segments.push({ type: 'text', text: t });
	}

	return segments.length > 0 ? segments : [{ type: 'text', text: text }];
}

/**
 * Render one parsed ASCII table segment as a sortable/filterable Bootstrap table.
 * @param {object} seg - { type:'table', headers:[...], rows:[[...], ...] }
 * @param {string} tableId - unique HTML id for the <table>
 */
function renderAsciiTable(seg, tableId)
{
	var html = '<div style="margin-bottom:6px;">';

	// Filter row — only shown when table has more than 5 rows
	if (seg.rows.length > 5) {
		html += '<div style="display:flex;align-items:center;gap:4px;margin-bottom:3px;">'
			+ '<span style="font-size:0.8em;color:#6c757d;">Filter:</span>'
			+ '<input type="text" style="font-size:0.8em;height:20px;padding:0 4px;width:200px;font-family:monospace;"'
			+ ' placeholder="regex..." oninput="dbmsAsciiFilter(this,\'' + tableId + '\');">'
			+ '<span id="' + tableId + '-cnt" style="font-size:0.78em;color:#6c757d;"></span>'
			+ '</div>';
	}

	html += '<table id="' + tableId + '" class="table table-sm table-bordered table-hover" style="font-size:0.8em;white-space:nowrap;width:auto;">';
	html += '<thead class="thead-light"><tr>';
	seg.headers.forEach(function(h, idx) {
		html += '<th style="cursor:pointer;user-select:none;" onclick="dbmsAsciiSort(this,\'' + tableId + '\',' + idx + ');">'
			+ escHtml(h) + ' <span class="sort-icon" style="font-size:0.75em;color:#999;"></span></th>';
	});
	html += '</tr></thead><tbody>';
	if (seg.rows.length === 0) {
		html += '<tr><td colspan="' + seg.headers.length + '" class="text-center text-muted">No rows</td></tr>';
	} else {
		seg.rows.forEach(function(row, ri) {
			html += '<tr data-orig="' + ri + '">';
			row.forEach(function(cell) {
				html += '<td>' + renderCell(cell) + '</td>';
			});
			html += '</tr>';
		});
	}
	html += '</tbody></table></div>';
	return html;
}

/** Filter rows of an ASCII-parsed table */
function dbmsAsciiFilter(input, tableId)
{
	var text   = input.value;
	var $rows  = $('#' + tableId + ' tbody tr');
	var total   = $rows.length;
	var visible = 0;
	var re = null;
	if (text) { try { re = new RegExp(text, 'i'); } catch(e) {} }

	$rows.each(function() {
		var show = !re || re.test($(this).text());
		$(this).toggle(show);
		if (show) visible++;
	});

	var $cnt = $('#' + tableId + '-cnt');
	$cnt.text(text ? ('Showing ' + visible + ' / ' + total) : (total + ' rows'));
}

/** Sort an ASCII-parsed table by column */
function dbmsAsciiSort(th, tableId, colIdx)
{
	var $th   = $(th);
	var cur   = parseInt($th.data('sort') || 0);
	var next  = (cur + 1) % 3;
	$th.data('sort', next);

	$('#' + tableId + ' thead th').not($th).each(function() {
		$(this).data('sort', 0);
		$(this).find('.sort-icon').text('');
	});
	$th.find('.sort-icon').text(next === 1 ? ' ▲' : next === 2 ? ' ▼' : '');

	var $tbody = $('#' + tableId + ' tbody');
	var rows   = $tbody.find('tr').toArray();

	if (next === 0) {
		rows.sort(function(a, b) {
			return parseInt($(a).data('orig') || 0) - parseInt($(b).data('orig') || 0);
		});
	} else {
		rows.sort(function(a, b) {
			var av = $(a).find('td').eq(colIdx).text();
			var bv = $(b).find('td').eq(colIdx).text();
			var an = parseFloat(av), bn = parseFloat(bv);
			if (!isNaN(an) && !isNaN(bn))
				return next === 1 ? an - bn : bn - an;
			return next === 1 ? av.localeCompare(bv) : bv.localeCompare(av);
		});
	}
	$tbody.empty().append(rows);
}

/** Show a plain message in the DBMS Config body */
function dbmsConfigShowMsg(msg)
{
	$('#dbms-config-loading').hide();
	$('#dbms-config-content').html('<div style="padding:15px;text-align:center;color:#6c757d;">' + escHtml(msg) + '</div>');
}

/**
 * Called from the history slider when the timestamp changes.
 * Refreshes the DBMS Config panel if it is open.
 */
function dbmsConfigSliderRefresh(ts)
{
	if ($('#dbms-config-panel').is(':visible') && _dcSrvName && ts) {
		_dcTimestamp = ts;
		$('#dbms-config-ts').text('@ ' + ts);
		dbmsConfigLoad(_dcSrvName, ts);
	}
}

