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
			
			var originTxt   = json[i][col[j]];
			var strippedTxt = stripHtml( originTxt );

			// If originTxt contains HTML Tags, then add both ORIGIN & STRIPPED and a button where we can "switch" between the two fields
			if (isHTML(originTxt))
			{
				var newDiv = document.createElement('div');

				var newlink = document.createElement('a');
				newlink.appendChild(document.createTextNode('Toggle: Compact or Formatted Content'));
				newlink.setAttribute('href', 'javascript:toggleActiveTableExtendedDescription();');
				
				var originDiv   = document.createElement('div');
				var strippedDiv = document.createElement('div');
				originDiv  .setAttribute('class', 'active-table-extDesc-origin-class');
				strippedDiv.setAttribute('class', 'active-table-extDesc-stripped-class');
				
				originDiv  .innerHTML = originTxt;
				strippedDiv.innerHTML = strippedTxt;
				
				originDiv  .style.display = stripHtmlInCells ? 'none'  : 'block';
				strippedDiv.style.display = stripHtmlInCells ? 'block' : 'none';
				
				newDiv.appendChild(newlink);
				newDiv.appendChild(originDiv);
				newDiv.appendChild(strippedDiv);

				tabCell.appendChild(newDiv);
			}
			else
			{
				//var cellContent = stripHtmlInCells ? strippedTxt : originTxt;
				tabCell.innerHTML = originTxt;
			}
			
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

function toggleActiveTableExtendedDescription()
{
//	var extDesc = document.getElementsByClassName("active-table-extDesc-origin-class active-table-extDesc-stripped-class");
	var extDesc = document.querySelectorAll('.active-table-extDesc-origin-class,.active-table-extDesc-stripped-class')

	// Toggle all elements in the above clases
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

	// Reload the page
	//location.reload();
	
	toggleActiveTableExtendedDescription();
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
	//activeStatementsAutoOpenChkClick( document.getElementById("active-statements-auto-open-chk") );

	// Restore 'Compact Extended Desc' at the ACTIVE STATEMENTS "window"
	var savedVal_activeStatementsCompactExtDescChk = getStorage('dbxtune_checkboxes_').get("active-statements-compExtDesc-chk");
	if (savedVal_activeStatementsCompactExtDescChk == 'checked') $("#active-statements-compExtDesc-chk").attr('checked', 'checked');
	if (savedVal_activeStatementsCompactExtDescChk == 'not')     $("#active-statements-compExtDesc-chk").removeAttr('checked');
	//activeStatementsCompExtDescClick( document.getElementById("active-statements-compExtDesc-chk") );

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
		$("#active-alarms").css("background-color", 'rgba(255, 195, 83, 1.0)');
	}
	else
	{
		$("#active-alarms").css("background-color", 'rgba(255, 195, 83, 0.5)');
	}

	// Save last known value in "WebBrowser storage"
	getStorage('dbxtune_checkboxes_').set("active-alarms-solid-chk", checkbox.checked ? 'checked' : 'not');
}
function activeAlarmsCompExtDescClick(checkbox) 
{
	console.log('activeAlarmsCompExtDescClick(): Checked: ' + checkbox.checked);

	// Save last known value in "WebBrowser storage"
	getStorage('dbxtune_checkboxes_').set("active-alarms-compExtDesc-chk", checkbox.checked ? 'checked' : 'not');

	// Reload the page
	//location.reload();
	
	toggleActiveTableExtendedDescription();
}

// do: deferred (since all DOM elements might not be created yet)
setTimeout(function()
{
	// Restore 'Compact Extended Desc' at the ACTIVE ALARMS "window"
	var savedVal_activeAlarmsCompactExtDescChk = getStorage('dbxtune_checkboxes_').get("active-alarms-compExtDesc-chk");
	if (savedVal_activeAlarmsCompactExtDescChk == 'checked') $("#active-alarms-compExtDesc-chk").attr('checked', 'checked');
	if (savedVal_activeAlarmsCompactExtDescChk == 'not')     $("#active-alarms-compExtDesc-chk").removeAttr('checked');
	//activeAlarmsCompExtDescClick( document.getElementById("active-alarms-compExtDesc-chk") );

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
}, 10);


// Create a CALLBACK function to set TableRow Colors etc, used when calling: jsonToTable(...)
var alarmTrCallback = function(tr, row)
{
//	// hasExtDesc
//	if (row.hasOwnProperty('hasExtDesc'))
//		tr.className = "active-alarm-checkbox";
//	
//	// hasLastExtDesc
//	if (row.hasOwnProperty('hasLastExtDesc'))
//		tr.className = "active-alarm-checkbox";
};
// Create a CALLBACK function to set TD/CELL Colors & formatted values
var alarmTdCallback = function(td, metaData, cellContent, rowData)
{
	if (isNaN(cellContent)) // Typically STRING
	{
	}
	else if (typeof(cellContent) === typeof(true)) // BOOLEAN ... isNaN(true) is FALSE
	{
		if (false === cellContent) { td.innerHTML = ""; td.className = "image-unchecked"; }
		if (true  === cellContent) { td.innerHTML = ""; td.className = "image-checked"; }
	}
	else // I guss this must be a NUMBER
	{
	}
	
	if (metaData.columnName === "alarmInfo" && rowData.hasOwnProperty('alarmInfo') && cellContent === true)
	{
		td.appendChild( createAlarmInfoToolTipDiv(JSON.stringify(rowData), "Alarm Info") );
	}
	if (metaData.columnName === "hasExtDesc" && rowData.hasOwnProperty('hasExtDesc') && cellContent === true)
	{
		td.appendChild( createExtDescTableToolTipDiv(rowData.extendedDescription, "Extended Description") );
	}
	if (metaData.columnName === "hasLastExtDesc" && rowData.hasOwnProperty('hasLastExtDesc') && cellContent === true)
	{
		td.appendChild( createExtDescTableToolTipDiv(rowData.lastExtendedDescription, "Last Extended Description") );
	}
};


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
					let stripHtmlInCells = document.getElementById("active-alarms-compExtDesc-chk").checked;
//					let tab = jsonToTable(entry, stripHtmlInCells);
console.log("XXXXXXXX AlarmTable just BEFORE jsonToTable()...", entry);
					let tab = jsonToTable(entry, stripHtmlInCells, alarmTrCallback, alarmTdCallback);
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
			}
			else
			{
				activeAlarmsTopDiv.style.visibility = 'hidden';
				activeAlarmsDiv.style.visibility = 'hidden';
				pageTitleNotification.off();
			}

			// Set how many ACTIVE ALarms we have...
			document.getElementById('active-alarms-count').innerHTML = alarmSumCount;
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
		const wsUrl = "ws://" + location.host + "/api/chart/broadcast-ws?serverList="+_serverList+"&graphList="+encodeURIComponent(graphList);
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
			let stripHtmlInCells = document.getElementById("active-alarms-compExtDesc-chk").checked;
//			let tab = jsonToTable(entry, stripHtmlInCells);
			let tab = jsonToTable(entry, stripHtmlInCells, alarmTrCallback, alarmTdCallback);
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
		div.setAttribute("title"           , "Click to Open Text Dialog... \n-------------------------------\n" + dataVal);
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
		let stripHtmlInCells = document.getElementById("active-statements-compExtDesc-chk").checked;
		let tab = jsonToTable(filteredCounterData, stripHtmlInCells, trCallback, tdCallback, metaDataArr);

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


		// Set count at the top of the poupup window
		$("#active-statements-count").html(totalActiveStatementsCount);

		// Floting button: Set how many ACTIVE Statements we have and make it BLINK
		$("#active-statements-btn").html("&lt;" + totalActiveStatementsCount + "&gt;");
		if ( totalActiveStatementsCount > 0 )
		{
			$("#active-statements-btn").html("&lt;" + totalActiveStatementsCount + "&gt;");
			$("#active-statements-btn").css("animation", "blink 1.5s infinite");
			$("#active-statements-btn").css("background", "rgba(229, 228, 226, 0.5)");
		}
		else
		{
			// Reset floating button: active-statements-btn
			$("#active-statements-btn").html("&lt;/&gt;");
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
						name: "Reset Zoom (on this graph)",
						callback: function(key, opt) 
						{
							var graphName = $(this).attr('id');
							console.log("contextMenu(graphName="+graphName+"): key='"+key+"'.");
							getGraphByName(graphName)._chartObject.resetZoom();
						}
					},
					resetZoomAll: {
						name: "Reset Zoom (on ALL graphs)",
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
			}
		});
	}

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
	//dbxTuneCheckActiveAlarms();

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

