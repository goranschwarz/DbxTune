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
	var retValue = vars.hasOwnProperty(key) ? decodeURIComponent(vars[key]) : defaultValue;
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

			// Set some stuff in the NavigationBar
			if (isLoggedIn)
			{
				document.getElementById("dbx-nb-isLoggedIn-div")    .style.display = "block";
				document.getElementById("dbx-nb-isLoggedOut-div")   .style.display = "none";
				document.getElementById("dbx-nb-isLoggedInUser-div").textContent   = asUserName;
			}
			else
			{
				document.getElementById("dbx-nb-isLoggedIn-div")    .style.display = "none";
				document.getElementById("dbx-nb-isLoggedOut-div")   .style.display = "block";
				document.getElementById("dbx-nb-isLoggedInUser-div").textContent   = "";
			}
			
			callback(isLoggedIn, asUserName);
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
			
			var originTxt   = json[i][col[j]];
			var strippedTxt = stripHtml( originTxt );

			// If originTxt contains HTML Tags, then add both ORIGIN & STRIPPED and a button where we can "switch" between the two fields
			if (isHTML(originTxt))
			{
				var newDiv = document.createElement('div');

				var newlink = document.createElement('a');
				newlink.appendChild(document.createTextNode('Toggle: Compact or Formatted Content'));
				newlink.setAttribute('href', 'javascript:toggleActiveAlarmsExtendedDesciption();');
				
				var originDiv   = document.createElement('div');
				var strippedDiv = document.createElement('div');
				originDiv  .setAttribute('class', 'active-alarms-extDesc-origin-class');
				strippedDiv.setAttribute('class', 'active-alarms-extDesc-stripped-class');
				
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

function toggleActiveAlarmsExtendedDesciption()
{
//	var extDesc = document.getElementsByClassName("active-alarms-extDesc-origin-class active-alarms-extDesc-stripped-class");
	var extDesc = document.querySelectorAll('.active-alarms-extDesc-origin-class,.active-alarms-extDesc-stripped-class')

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
