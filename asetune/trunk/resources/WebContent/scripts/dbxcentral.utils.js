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
 * Utility function: Create a HTML Table using a JSON input
 * grabbed from: http://www.encodedna.com/javascript/populate-json-data-to-html-table-using-javascript.htm
 */
function jsonToTable(json) 
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
			tabCell.innerHTML = json[i][col[j]];
		}
	}
	return table;
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
