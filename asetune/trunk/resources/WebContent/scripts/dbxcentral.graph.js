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
function jsonToTable(json) 
{
	if (_debug > 1)
		console.log("jsonToTable(): json=: "+json, json);

	// EXTRACT json VALUES FOR HTML HEADER. 
	var col = [];
	for (var i = 0; i < json.length; i++) {
		for (var key in json[i]) {
			if (col.indexOf(key) === -1) {
				col.push(key);
			}
		}
	}
	if (_debug > 1)
		console.log("jsonToTable(): headers: "+col, col);

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

/* **********************************************************************
** ** END INCLUDE: dbxcentral.utils.js
** **********************************************************************/






/* **********************************************************************
** ** GLOBAL VARIABLES
** **********************************************************************/
var _serverList = [];  // What servers does the URL reflect
var _graphMap   = [];  // Graphs that are displaied / instantiated
var _debug      = 0;   // Debug level: 0=off, 1=more, 2=evenMore, 3...


function dbxTuneCheckActiveAlarms()
{
	console.log("dbxTuneCheckActiveAlarms()");

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
			var activeAlarmsDiv = document.getElementById("active-alarms");

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
			for (var key in srvAlarmMap) {
				var entry = srvAlarmMap[key];
				// var graphServerName = entry.srvName;
				var graphServerName = key;

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

						activeAlarmsDiv.appendChild(srvDiv);
					}
					let tab = jsonToTable(entry);
					srvDiv.innerHTML = "Active Alarms for server: <b>"+graphServerName+"</b>";
					srvDiv.appendChild(tab);
					srvDiv.appendChild(document.createElement("br"));

					if (_debug > 1)
						console.log("TABLE for srv'"+graphServerName+"': ", tab);
				}
				else // Remove the SRV-DIV if no active alarm
				{
					let srvDiv = document.getElementById("active-alarms-srv-"+graphServerName);
					if (srvDiv !== null)
						activeAlarmsDiv.removeChild(srvDiv);
				}
			}

			// should we show the activeAlarmsDiv or not?
			if (_debug > 0)
				console.log("activeAlarmsDiv.getElementsByTagName('*').length: "+activeAlarmsDiv.getElementsByTagName('*').length);
			if (activeAlarmsDiv.getElementsByTagName('*').length > 0)
			{
				activeAlarmsDiv.style.visibility = 'visible';
				pageTitleNotification.on("---ALARM---", 1000);
			}
			else
			{
				activeAlarmsDiv.style.visibility = 'hidden';
				pageTitleNotification.off();
			}
		},
		error: function(xhr, desc, err) 
		{
			console.log(xhr);
			console.log("Details: " + desc + "\nError: " + err);
		}
	}); // end: ajax call
} // end: function

/** 
 * Subscribe to graph changes from the server side
 */
function dbxTuneGraphSubscribe() 
{
	const sessionName = getParameter("sessionName");
	const graphList   = getParameter("graphList", "");
	var   subscribe   = getParameter("subscribe", false);
	// const debug       = getParameter("debug",     0);
	const endTime     = getParameter("endTime",   "");
	console.log("Passed subscribe="+subscribe);

	var webSocket;

	if (subscribe === "false")
		subscribe = false;

	// turn subscribe off if 'endTime' is present
	if (endTime !== "")
		subscribe = false;

	// data the updates every time we get a new message
	var dbxLastSubscribeTime = moment();

	/** 
	 * Update clock... on when last subscription event was received
	 */
	function updateSubscribeClock()
	{
		let ageInSec = Math.floor((moment() - dbxLastSubscribeTime) / 1000);
		$("#subscribe-feedback-time").css("color", "");
		$("#subscribe-feedback-time").html(", " + ageInSec + " seconds ago.");

		if (ageInSec > 120)
			$("#subscribe-feedback-time").css("color", "red");

		// TEST: pageTitleNotification
		// if (ageInSec > 10 && ageInSec < 20)
		// 	pageTitleNotification.on("---DUMMY---", 1000);
		// else
		//  	pageTitleNotification.off();

		setTimeout(updateSubscribeClock, 1000);
	}

	function subscribeConnectWs() 
	{
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
		// webSocket.onerror = function (e) 
		// {
		// 	dbxLastSubscribeTime = moment();
		// 	if      (e.readyState == WebSocket.CONNECTING) { addSubscribeFeedback('ws', 'CONNECTING'); } 
		// 	else if (e.readyState == WebSocket.OPEN)       { addSubscribeFeedback('ws', 'OPEN');       } 
		// 	else if (e.readyState == WebSocket.CLOSING)    { addSubscribeFeedback('ws', 'CLOSING');    } 
		// 	else if (e.readyState == WebSocket.CLOSED)     { addSubscribeFeedback('ws', 'CLOSED');     }
		// };
	}
	

	$("#subscribe-feedback-time").html("subscribe="+subscribe);
	if (subscribe)
	{
		updateSubscribeClock(); // is automatically executed every second

		// if (!!window.EventSource)
		// {
		// 	// const eventSource = new EventSource("/api/chart/broadcast-sse?serverList="+sessionName+"&graphList="+graphList);
		// 	const eventSource = new EventSource("/api/chart/broadcast?serverList="+_serverList+"&graphList="+graphList);
		// 	const elements = document.getElementById("messages");

		// 	console.log("Entering subscribe mode: eventSourceUrl: "+eventSource.url);

		// 	eventSource.onmessage = function (e) {
		// 		// var jsonData = JSON.parse(e.data);
		// 		// var message = e.data;
		// 		// console.log(e);
		// 		dbxLastSubscribeTime = moment();
		// 		addData(e.data);
		// 		addSubscribeFeedback('data', e.data);
		// 		//addToDummyChart(e.data);
		// 	};
		// 	eventSource.onopen = function (e) {
		// 		dbxLastSubscribeTime = moment();
		// 		addSubscribeFeedback('ws', 'conn opened');
		// 	};
		// 	eventSource.onerror = function (e) {
		// 		dbxLastSubscribeTime = moment();
		// 		if (e.readyState == EventSource.CONNECTING) {
		// 			addSubscribeFeedback('ws', 'CONNECTING');
		// 		} else if (e.readyState == EventSource.OPEN) {
		// 			addSubscribeFeedback('ws', 'OPEN');
		// 		} else if (e.readyState == EventSource.CLOSING) {
		// 			addSubscribeFeedback('ws', 'CLOSING');
		// 		} else if (e.readyState == EventSource.CLOSED) {
		// 			addSubscribeFeedback('ws', 'CLOSED');
		// 		}
		// 	};
		// } else {
		// 	alert('The browser does not support Server-Sent Events');
		// }

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

		// TODO:
		// to let the Browser update it's GUI it would have been nice with a yield() method
		// but we might be able to do something like:
		//  - loop and -> push: "dbxGraph" and "graph" onto a queue/array
		//  - then pass the queue/array to a method, which dequeues the array, calls "dbxGraph.addSubscribeData([graph])", then calls itself via: setTimeout(..., 0)
		// or maybe something like this: http://debuggable.com/posts/run-intense-js-without-freezing-the-browser:480f4dd6-f864-4f72-ae16-41cccbdd56cb
		//-----------------------------------------------------------------------------------
		// Note the below code takes too looooong when we have many graphs... the above "queue"/setTimeout() stuff might be a viable solution.
		for(let gc=0; gc<graphCollectors.length; gc++) {
			let collector = graphCollectors[gc];

			// console.log("addDate(): collector="+collector, collector);
			for(let g=0; g<collector.graphs.length; g++) {
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
		//--- removed below (switch to ordinary loop) because some browers (safari) did not like => (arrow functionality)... I really start to hate JavaScript at this point...
		// graphCollectors.forEach(collector => 
		// {
		// 	collector.graphs.forEach(graph =>
		// 	{
		// 		var cmName          = graph.cmName;
		// 		var graphName       = graph.graphName;

		// 		for(var i=0; i<_graphMap.length; i++)
		// 		{
		// 			const dbxGraph = _graphMap[i];

		// 			if (dbxGraph.getServerName() === graphServerName && dbxGraph.getCmName() === cmName && dbxGraph.getGraphName() === graphName)
		// 			{
		// 				if (_debug > 0)
		// 					console.log("Sending Subscribe Graph Data for cmName="+cmName+", graphName="+graphName+", to Graph name="+dbxGraph.getFullName()+": ", graph);
		// 				dbxGraph.addSubscribeData( [ graph ] );  // as a array with 1 entry
		// 			}
		// 		}
		// 	});
		// });

		// update ACTIVE Alarm view (note: there can be several servers)
		var activeAlarmsDiv = document.getElementById("active-alarms");

		// If any Active alarms create a new SRV-DIV and a table...
		if (typeof graphJson.activeAlarms !== 'undefined')
		{
			let srvDiv = document.getElementById("active-alarms-srv-"+graphServerName);
			if (srvDiv === null)
			{
				srvDiv = document.createElement("div");
				srvDiv.id = "active-alarms-srv-"+graphServerName;

				activeAlarmsDiv.appendChild(srvDiv);
			}
			let tab = jsonToTable(graphJson.activeAlarms);
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
				activeAlarmsDiv.removeChild(srvDiv);
		}
		// should we show the activeAlarmsDiv or not?
		if (_debug > 0)
			console.log("activeAlarmsDiv.getElementsByTagName('*').length: "+activeAlarmsDiv.getElementsByTagName('*').length);
		if (activeAlarmsDiv.getElementsByTagName('*').length > 0)
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
	}
} // end: function


//--------------------------------------------------------------------
//--------------------------------------------------------------------
// Class: DbxGraph
//--------------------------------------------------------------------
//--------------------------------------------------------------------
class DbxGraph
{
	constructor(outerChartDiv, serverName, cmName, graphName, graphLabel, graphCategory, isPercentGraph, subscribeAgeInSec, gheight, gwidth, debug)
	{
		// Properties/fields
		this._serverName        = serverName;
		this._cmName            = cmName;
		this._graphName         = graphName;
		this._fullName          = cmName + "_" + graphName;
		this._graphLabel        = graphLabel;
		this._graphCategory     = graphCategory;
		this._isPercentGraph    = isPercentGraph;
		this._subscribeAgeInSec = subscribeAgeInSec;
		this._debug             = debug;

		// this._graphHeight = 150;
		this._graphHeight = 200;
		this._graphHeight = gheight;
		this._graphWidth  = gwidth;

		this._series = [];
		this._initialized = false;

		this._chartConfig = 
		{
			type: 'line',
			data: {
				labels: [],
				datasets: []
			},        
			// plugins: [{
			// 	afterDraw: function() {
			// 		console.log("afterDraw(): cmName='"+cmName+"', graphName='"+graphName+"'.");
			// 	}
			// }],
			options: {
				responsive: true,
				maintainAspectRatio: false,
				title: {
					display: true,
					fontSize: 16,
//					text: this._graphLabel
//					text: this._cmName + ":" + this._graphName + " ##### " + this._graphLabel
//					text: this._graphLabel + " ::: [" + this._serverName + "] " + this._cmName + ":" + this._graphName
					text: this._graphLabel + " ::: [" + this._serverName + "]"
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
				},
				scales: {
					xAxes: [{
						type: 'time',
						distribution: 'linear',
						time: {
							displayFormats: {
								second: 'HH:mm:ss',
								minute: 'HH:mm:ss',
								hour:   'HH:mm',
							}
						},
						ticks: {
							beginAtZero: true,  // if true, scale will include 0 if it is not already included.
						}
					}],
					yAxes: [{
						ticks: {
							beginAtZero: true,  // if true, scale will include 0 if it is not already included.
							callback: function(value, index, values) 
							{
								// Alter numbers larger than 1k
								if (value >= 1000) {
									var units = [" K", " M", " G", " T"];

									var order = Math.floor(Math.log(value) / Math.log(1000));

									// TODO: check more than one label to decide if we should use decimals for all labels or just this one...  
									
									var unitname = units[(order - 1)];
									//var num = Math.floor(value / 1000 ** order);
									var num = (value / 1000 ** order).toFixed(1).replace(/\.0$/, '');

									// output number remainder + unitname
									return num + unitname;
								}

								// return formatted original number
								// Use ChartJS default method for this, otherwise 1.0 till be 1 etc...
								return Chart.Ticks.formatters.linear(value, index, values);

								//return value.toFixed(1);
								//return value.toFixed(1).replace(/\.0$/, '');
								//return Chart.Ticks.formatters.linear(value, index, values);
								//return this.toLocaleString();
								//return value.toString();
								//return value.toLocaleString();
							}
						}
					}],
				},
				// pan: { // Container for pan options
				// 	enabled: true, // Boolean to enable panning
				// 	mode: 'x', // Panning directions. Remove the appropriate direction to disable, Eg. 'y' would only allow panning in the y direction
				// 	drag: true,
				// },
				zoom: {  // Container for zoom options
					enabled: true,  // Boolean to enable zooming
					mode: 'x', // Zooming directions. Remove the appropriate direction to disable, Eg. 'y' would only allow zooming in the y direction
					drag: true,
				},
				annotation: {
					// drawTime: 'afterDatasetsDraw',
					drawTime: 'afterDraw',
					// events: ['click'],
					annotations: [],
					// annotations: [{
					// 	// drawTime: 'afterDraw', // overrides annotation.drawTime if set
					// 	id: 'vline',
					// 	type: 'line',
					// 	mode: 'vertical',
					// 	scaleID: 'x-axis-0',
					// 	// value: null,
					// 	borderColor: 'black',
					// 	borderWidth: 3,
					// //	borderDash: [2, 2],
					// 	label: {
					// 		backgroundColor: "red",
					// 		content: "Selected Line",
					// 		enabled: true
					// 	},
					// 	onClick: function(e) {
					// 		// The annotation is is bound to the `this` variable
					// 		console.log('Annotation', e.type, this);
					// 	}
					// }]
				},
				tooltips: {
					callbacks: {
						label: function (tooltipItems, data) {
							// print numbers localized (typically 1234567.8 -> 1,234,567.8)
							return data.datasets[tooltipItems.datasetIndex].label + ': ' + tooltipItems.yLabel.toLocaleString();
						}
					}
				}
//				plugins: {
//					datalabels: {
//						formatter: function(value, context) {
//							return numeral(value).format(0,0);
//						}
//					}
//				}
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

		// Set MIN/MAX values when it's a PERCENT Graph
		if (this._debug > 0)
			console.log("GRAPH: "+this._fullName+": this._isPercentGraph="+this._isPercentGraph);

		if (this._isPercentGraph)
		{
			if (this._debug > 0)
				console.log("This is a PERCENT Graph: GraphName="+this._fullName);

			this._chartConfig.options.scales.yAxes[0].ticks.suggestedMax = 100; // Adjustment used when calculating the maximum  data value
			this._chartConfig.options.scales.yAxes[0].ticks.suggestedMin = 0;   // Adjustment used when calculating the minimum data value
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

		//---------------------------------------------------------------------------------------------------------
		// on right click: "reset zoome" on ALL charts
		//---------------------------------------------------------------------------------------------------------
		// newCanvas.addEventListener("contextmenu", function(event) {
		// 	event.preventDefault();
		// 	if (this._debug > 0)
		// 		console.log("xxxx", event);
		// 	for(var i=0; i<_graphMap.length; i++)
		// 	{
		// 		const dbxGraph = _graphMap[i];
		// 		dbxGraph._chartObject.resetZoom();
		// 	}
		// });
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
					showThisInNewTab: {
						name: "Open this graph in new Tab",
						callback: function(key, opt) 
						{
							var graphName = $(this).attr('id');
							console.log("contextMenu(graphName="+graphName+"): key='"+key+"'.");

							// Get serverName this graph is for
							var srvName = getGraphByName(graphName).getServerName();

							// Get Get current URL
							var currentUrl  = window.location.href;
							const url = new URL(currentUrl);
							const params = new URLSearchParams(url.search);

							// Change/set some parameters
							params.set('sessionName', srvName);
							params.set('graphList',   graphName);

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
			// Reset all timeline-markers
			for(var i=0; i<_graphMap.length; i++)
			{
				const dbxGraph = _graphMap[i];
			//	setTimeout(function() { 
					dbxGraph._chartObject.options.annotation.annotations = [];
					dbxGraph._chartObject.update(0);
			//	}, i+1); 
			}

			var activePoints = thisClass._chartObject.getElementsAtEvent(event); // getPointsAtEvent(evtent)
			// console.log("activePoints: ", activePoints)
			var firstPoint = activePoints[0];
			if(firstPoint !== undefined) 
			{
				var clickTs = thisClass._chartObject.data.labels[firstPoint._index];

				// Set label: 2018-01-23 19:33:00
				var tsStr1 = moment(clickTs).format().substring(0, 10);
				var tsStr2 = moment(clickTs).format().substring(11, 19);
				var labelTsStr = tsStr1 + " " + tsStr2;
				// if it's the same day: Set label: 19:33:00
				if ( moment().format().substring(0,10) === tsStr1)
					labelTsStr = tsStr2;

				// SET timeline-markers in ALL Graphs
				for(let i=0; i<_graphMap.length; i++)
				{
					const dbxGraph = _graphMap[i];
					const annotation = {
						id: 'vline',
						type: 'line',
						mode: 'vertical',
						scaleID: 'x-axis-0',
						value: clickTs,
						borderColor: 'gray',
						borderWidth: 2,
						borderDash: [2, 2],
						label: {
							enabled: true,
							content: labelTsStr,
							fontSize: 10,
							backgroundColor: 'gray',
							position: 'top',
						  }
					};
				//	setTimeout(function() { 
						dbxGraph._chartObject.options.annotation.annotations[0] = annotation;
						dbxGraph._chartObject.update(0);
				//	}, i+1); // hide after 10s
				}
			}

			// Simulate a windows resize to redraw the "whole thing"
			//window.dispatchEvent(new Event('resize'));
		});
	}

	setInitialized()   { this._initialized = true; }
	isInitialized()    { return this._initialized; }

	getHeight()        { return this._graphHeight; }
	getServerName()    { return this._serverName; }
	getCmName()        { return this._cmName; }
	getGraphName()     { return this._graphName; }
	getFullName()      { return this._fullName; }
	getGraphLabel()    { return this._graphLabel; }
	getGraphCategory() { return this._graphCategory; }

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
					if (this._chartObject.options.annotation.annotations.length > 0)
					{
						if (this._chartObject.options.annotation.annotations[0].value <= oldestTs)
						{
							if (this._debug > 0)
								console.log("Clearing timeline-marker for graph '"+this._fullName+"' due to time-expire. annotationTs='"+this._chartObject.options.annotation.annotations[0].value+"', graphpOldestTs='"+oldestTs+"'.");
							this._chartObject.options.annotation.annotations = [];
						}
					}
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
		// console.log("DbxGraph.addData(): jsonData: ", jsonData);

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

		if (jsonData.length === 0) {
			this._chartFeedback.innerHTML = "<font color='red'>No data was found.</font>";
		} else {
			this._chartFeedback.innerHTML = "";
			// this._chartFeedback.innerHTML = jsonData.length + " data points in chart";
		}
	}
}


//window.onload = function() 
function dbxTuneLoadCharts(destinationDivId)
{
	// get query string parameter
	const sessionName    = getParameter("sessionName");
	var   graphList      = getParameter("graphList");
	const startTime      = getParameter("startTime",   "2h");
	const endTime        = getParameter("endTime",     "");
	const subscribe      = getParameter("subscribe",   false);
	const debug          = getParameter("debug",       0);
	const gheight        = getParameter("gheight",     200);
	const gwidth         = getParameter("gwidth",      650);
	const sampleType     = getParameter("sampleType",  "");
	const sampleValue    = getParameter("sampleValue", "");

	_debug = debug;

	console.log("Passed sessionName="+sessionName);
	console.log("Passed graphList="+graphList);
	console.log("Passed startTime="+startTime);
	console.log("Passed endTime="+endTime);

	var loadAllGraphs = false;
	if (graphList !== undefined && graphList === "all")
	{
		loadAllGraphs = true;
		graphList = undefined;
	}

	window.addEventListener("resize", function() { console.log("received-resize-event"); });
		
	if ( sessionName == null && graphList == null)
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
				'<td>sampleType</td>' + 
				'<td>Data point about the graph can be fetched by different "methods".<br>' + 
				'Here are the methods:<br>' + 
				'<ul>' + 
				'  <li>ALL - Get all data points</li>' + 
				'  <li>AUTO - Uses a automatic formula using MAX_OVER_SAMPLES, with a <code>sampleValue=320</code></li>' + 
				'  <li>MAX_OVER_SAMPLES - Get MAX values over X number of data points</li>' + 
				'  <li>MAX_OVER_MINUTES - Get MAX values over X minutes of data points</li>' + 
				'  <li>AVG_OVER_MINUTES - Get AVERAGE values over X minutes of data points</li>' + 
				'  <li>SUM_OVER_MINUTES - Get SUM values over X minutes of data points</li>' + 
				'</ul>' + 
				'<b>default:</b> AUTO<br>' + 
				'</td>' + 
			'</tr>' +
			'<tr>' + 
				'<td>sampleValue</td>' + 
				'<td>Used by <code>sampleType</code>' + 
				'<ul>' + 
				'  <li>ALL - not used</li>' + 
				'  <li>AUTO - not used (same as specify <code>sampleType=MAX_OVER_SAMPLES&sampleValue=320</code>)</li>' + 
				'  <li>MAX_OVER_SAMPLES - A number value...</li>' + 
				'  <li>MAX_OVER_MINUTES - A number value...</li>' + 
				'  <li>AVG_OVER_MINUTES - A number value...</li>' + 
				'  <li>SUM_OVER_MINUTES - A number value... there are 60 minutes in one hour, and 1440 minutes in one day</li>' + 
				'</ul>' + 
				'<b>default:</b> ""<br>' + 
				'</td>' + 
			'</tr>' +
			'</table>' +
			'Below is a full example<br>' +
			'<code>http://dbxtune.company.com:8080/graph.html?sessionName=PROD_A_ASE&startTime=2h&subscribe=true</code>'
		);
		return;

	}

	// $('#toolbar').w2toolbar({
	//     name: 'toolbar',
	//     items: [
	//         { type: 'menu', id: 'item1', text: 'Menu', icon: 'fa-table', count: 17, items: [
	//             { text: 'Item 1', icon: 'fa-camera', count: 5 },
	//             { text: 'Item 2', icon: 'fa-picture', disabled: true },
	//             { text: 'Item 3', icon: 'fa-glass', count: 12 }
	//         ]},
	//         { type: 'break' },
	//         { type: 'menu-radio', id: 'item2', icon: 'fa-star',
	//             text: function (item) {
	//                 var text = item.selected;
	//                 var el   = this.get('item2:' + item.selected);
	//                 return 'Radio: ' + el.text;
	//             },
	//             selected: 'id3',
	//             items: [
	//                 { id: 'id1', text: 'Item 1', icon: 'fa-camera' },
	//                 { id: 'id2', text: 'Item 2', icon: 'fa-picture' },
	//                 { id: 'id3', text: 'Item 3', icon: 'fa-glass', count: 12 }
	//             ]
	//         },
	//         // { type: 'break' },
	//         // { type: 'menu-check', id: 'item3', text: 'Check', icon: 'fa-heart',
	//         //     selected: ['id3', 'id4'],
	//         //     onRefresh: function (event) {
	//         //         event.item.count = event.item.selected.length;
	//         //     },
	//         //     items: [
	//         //         { id: 'id1', text: 'Item 1', icon: 'fa-camera' },
	//         //         { id: 'id2', text: 'Item 2', icon: 'fa-picture' },
	//         //         { id: 'id3', text: 'Item 3', icon: 'fa-glass', count: 12 },
	//         //         { text: '--' },
	//         //         { id: 'id4', text: 'Item 4', icon: 'fa-glass' }
	//         //     ]
	//         // },
	//         { type: 'break' },
	//         { type: 'drop',  id: 'item4', text: 'Dropdown', icon: 'fa-plus',
	//             html: '<div style="padding: 10px; line-height: 1.5">You can put any HTML in the drop down.<br>Include tables, images, etc.</div>'
	//         },
	//         { type: 'break', id: 'break3' },
	//         { type: 'html',  id: 'item5',
	//             html: function (item) {
	//                 var html =
	//                   '<div style="padding: 3px 10px;">'+
	//                   ' CUSTOM:'+
	//                   '    <input size="10" onchange="var el = w2ui.toolbar.set(\'item5\', { value: this.value });" '+
	//                   '         style="padding: 3px; border-radius: 2px; border: 1px solid silver" value="'+ (item.value || '') + '"/>'+
	//                   '</div>';
	//                 return html;
	//             }
	//         },
	//         { type: 'spacer' },
	//         { type: 'button',  id: 'item6',  text: 'Item 6', icon: 'fa-flag' }
	//     ]
	// });

	// w2ui.toolbar.on('*', function (event) {
	//     console.log('EVENT: '+ event.type + ' TARGET: '+ event.target, event);
	// });
	
	// Remove old data in graphs when subscribibg to data. This is the age for when to remove < 0 == do not remove
	var subscribeAgeInSec = -1;
	if (subscribe)
	{
		var multiPlayer = 60;
		var localStartTime = startTime;
		if (localStartTime.match("^[0-9]+[mhdlMHDL]$")) // If value starts with a number and ends with m, h, d  m=minutes, h=hour, d=day
		{
			var lastChar = localStartTime.substring(localStartTime.length-1).toLowerCase();
			if ("m" === lastChar) multiPlayer = 60;           // Minutes
			if ("h" === lastChar) multiPlayer = 60 * 60;      // Hours
			if ("d" === lastChar) multiPlayer = 60 * 60 * 24; // Days

			localStartTime = localStartTime.substring(0, localStartTime.length-1);
		}
		subscribeAgeInSec = parseInt(localStartTime) * multiPlayer; 
		if (isNaN(subscribeAgeInSec))
			subscribeAgeInSec = -1;
	}
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
		else // Try to read it as a comma separated lits and create a graphProfile
		{
			let graphNameArr = graphList.split(",");
			graphProfile = [];
			for (let i=0; i<graphNameArr.length; i++) 
			{
				var graphEntry = {};
				graphEntry.graph = graphNameArr[i];

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
			// Take the imput parameter(s) and put them into global_serverList
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
		async: false,   // (async: true = do it in the background)
		success: function(data, status) 
		{
			const jsonResp = JSON.parse(data);
			if (_debug > 4)
				console.log("JSON PARSED DATA: ", jsonResp);

			// In what order should the graphs be created/loaded
			let createOrder = createGraphLoadOrder(graphProfile, jsonResp, loadAllGraphs);

			for(let i = 0; i < createOrder.length; i++) 
			{
				const entry = createOrder[i];

				// Create graph object
				const dbxGraph = new DbxGraph(
					destinationDivId,  // put it in "div" that has id
					entry.serverName,
					entry.cmName,
					entry.graphName,
					entry.graphLabel,
					entry.graphCategory,
					entry.percentGraph,
					subscribeAgeInSec,
					gheight,
					gwidth,
					debug
				);

				// add it to the global list/map
				_graphMap.push(dbxGraph);
			}


			// //------------------------------------------------------------------
			// // Order entries in the order we want to display them in.
			// // - add all MATCH indexes to an array
			// // - move the MATCHING slots in the array from 'resp' to 'data'
			// // - then move the rest of 'resp' to 'data'
			// const indexArr = [];
			// for(let i = 0; i < graphProfile.length; i++) 
			// {
			// 	const fullGraphName = graphProfile[i].graph;
			// 	const serverName    = (graphProfile[i]["srv"] === undefined) ? sessionName : graphProfile[i].srv;

			// 	for(var j = 0; j < jsonResp.length; j++) 
			// 	{
			// 		const entry = jsonResp[j];
			// 		//console.log("entry.tableName=|"+entry.tableName+"|, fullGraphName=|"+fullGraphName+"|, entry.serverName=|"+entry.serverName+"|, serverName=|"+serverName+"|, entry:", entry);
			// 		if (entry.tableName === fullGraphName && entry.serverName === serverName) 
			// 		{
			// 			//console.log("push="+j);
			// 			indexArr.push(j);
			// 			break;
			// 		}
			// 	}
			// }
			// const jsonData = [];
			// for(let i = 0; i < indexArr.length; i++) 
			// {
			// 	const jsonPos = indexArr[i];
			// 	jsonData.push( jsonResp[jsonPos] );  // Add it to data
			// 	jsonResp.splice(jsonPos, 1);         // remove it from responce
			// }
			// for(let i = 0; i < jsonResp.length; i++) 
			// 	jsonData.push( jsonResp[i] );

			// // for(let i = 0; i < jsonData.length; i++) 
			// // 	console.log( "jsonData["+i+"]:", jsonData[i] );

			// //------------------------------------------------------------------
			// // Loop the JSON DATA and create GRAPH objects (which is an array off graph names)
			// for(let i = 0; i < jsonData.length; i++) 
			// {
			// 	const entry = jsonData[i];
			// 	if (entry === undefined)
			// 		continue;

			// 	// console.log( "entry at["+i+"]:", entry );

			// 	let addThisGraph = false;

			// 	// If we DO NOT have a graphProfile - use visibleAtStartup from the database
			// 	if (graphProfile.length === 0)
			// 	{
			// 		if ( entry.visibleAtStartup === true )
			// 			addThisGraph = true;
			// 	}				
			// 	else // If we HAVA a graphProfile - create/display ONLY the graphs in the profile
			// 	{
			// 		for(var p = 0; p < graphProfile.length; p++) 
			// 		{
			// 			// console.log("graphProfile["+p+"]:", graphProfile[i]);
			// 			if (graphProfile[p].graph === entry.tableName)
			// 				addThisGraph = true;
			// 		}
			// 	}

			// 	// Always display UserDefinedGraphs
			// 	// if ( ! entry.cmName.startsWith("Cm") )
			// 	// 	addThisGraph = true;
			// 	if (loadAllGraphs)
			// 		addThisGraph = true;

			// 	//console.log("xxxx: addThisGraph="+addThisGraph+", entry.tableName="+entry.tableName+", graphProfile="+graphProfile);

			// 	if ( addThisGraph === false )
			// 	{
			// 		if (_debug > 0)
			// 		{
			// 			let msg = "visibleAtStartup===false";
			// 			if (graphProfile.length > 0)
			// 				msg = "not-in-profile";
			// 			console.log("Skipping graph due to '"+msg+"'. serverName="+entry.serverName+", cmName="+entry.cmName+", graphName="+entry.graphName, entry);
			// 		}
			// 		continue;
			// 	}

			// 	const dbxGraph = new DbxGraph(
			// 		destinationDivId,  // put it in div id
			// 		// "graphs",  // put it in div id
			// 		// sessionName,
			// 		entry.serverName,
			// 		entry.cmName,
			// 		entry.graphName,
			// 		entry.graphLabel,
			// 		entry.percentGraph,
			// 		subscribeAgeInSec,
			// 		gheight,
			// 		gwidth,
			// 		debug
			// 	);

			// 	// add it to the global list/map
			// 	_graphMap.push(dbxGraph);

			// } // end: for loop
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


	//-------------------------------------------------------------------
	// Load initial data for each of the created graphs in: _graphMap
	//-------------------------------------------------------------------
	loadDataForGraph(0, startTime, endTime, sampleType, sampleValue);
// 	for(var i = 0; i < _graphMap.length; i++)
// 	{
// 		const dbxGraph = _graphMap[i];

// 		const serverName = dbxGraph.getServerName();
// 		const cmName     = dbxGraph.getCmName();
// 		const graphName  = dbxGraph.getGraphName();
// 		const url        = '/api/graph/data?sessionName='+serverName+'&cmName='+cmName+'&graphName='+graphName+"&startTime="+startTime+"&endTime="+endTime+"&sampleType="+sampleType+"&sampleValue="+sampleValue;

// 		// $("#api-feedback").html("Loading Graphs DATA for serverName="+serverName+", cmName="+cmName+", graphName="+graphName+" from url: "+url);
// 		if (_debug > 0)
// 			console.log("AJAX Call: "+url);

// 		// $( "#progressbar" ).html(i+'% - START - cmName='+cmName+', graphName='+graphName);
// 		$('#progressBar').css('width', '0%');
// 		var progressCnt = 0;
// 		$.ajax(
// 		{
// 			url: url,
// 			type: 'get',
// 			// async: false,   // to call it one by one (async: true = spawn away a bunch of them in the background)
			
// 			success: function(data, status) 
// 			{
// 				// console.log("RECEIVED DATA: "+data);
// 				var jsonResp = JSON.parse(data);
				
// 				console.time('dbxGraph.addData:: cmName='+cmName+', graphName='+graphName);
// 				dbxGraph.addData(jsonResp);
// 				console.timeEnd('dbxGraph.addData:: cmName='+cmName+', graphName='+graphName);
				
// 				if (_debug > 0)
// 					console.log('DONE: loading data for cmName='+cmName+', graphName='+graphName);

// 				progressCnt++
// 				const pct = Math.round( progressCnt / _graphMap.length * 100);
// 				$('#progressBar').css('width', pct+'%');
// 				$('#progressBar').html(pct+'%');
// 				// $('#progressDiv').html("Done loading: "+cmName+' - '+graphName);
// 				if (pct === 100)
// 				{
// 					$('#progressDiv').hide();
// 					$("#api-feedback").html("Loaded "+progressCnt+" Graphs.");

// 					if (_debug > 0)
// 						console.log("DONE: loading ALL data...");
// 				}
// 			},
// 			error: function(xhr, desc, err) 
// 			{
// 				console.log(xhr);
// 				console.log("Details: " + desc + "\nError: " + err);
// 				$("#feedback_"+cmName+"_"+graphName).html("<strong>ERROR Details:</strong> " + desc + "<br /><strong>Error:</strong> " + err + "<br /><strong>ResponseText:</strong> " + xhr["responseText"] + "<br />");
// //				$("#api-feedback").html("<strong>ERROR Details:</strong> " + desc + "<br /><strong>Error:</strong> " + err + "<br /><strong>ResponseText:</strong> " + xhr["responseText"] + "<br />");
// //				$("#dialog-tab-prevReviewNote").html("<strong>ERROR Details:</strong> " + desc + "<br /><strong>Error:</strong> " + err + "<br /><strong>ResponseText:</strong> " + xhr["responseText"] + "<br />");
// //				$("#dialog-tab-prevReviewNote").css("color", "red");
// 			}
// 		}); // end: ajax call
// 	} // end: for loop
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
		const fullGraphName = graphProfile[i].graph;
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
function loadDataForGraph(indexToLoad, startTime, endTime, sampleType, sampleValue)
{
	if (indexToLoad >= _graphMap.length)
		return;

	const dbxGraph = _graphMap[indexToLoad];

	const serverName = dbxGraph.getServerName();
	const cmName     = dbxGraph.getCmName();
	const graphName  = dbxGraph.getGraphName();
	const url        = '/api/graph/data?sessionName='+serverName+'&cmName='+cmName+'&graphName='+graphName+"&startTime="+startTime+"&endTime="+endTime+"&sampleType="+sampleType+"&sampleValue="+sampleValue;

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
			
			console.time('dbxGraph.addData:: cmName='+cmName+', graphName='+graphName);
			dbxGraph.addData(jsonResp);
			console.timeEnd('dbxGraph.addData:: cmName='+cmName+', graphName='+graphName);
			
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
				// Load next GRAPH in 10 ms
				setTimeout( loadDataForGraph(indexToLoad + 1, startTime, endTime, sampleType, sampleValue), 1);
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
				// Load next GRAPH in 10 ms
				setTimeout( loadDataForGraph(indexToLoad + 1, startTime, endTime, sampleType, sampleValue), 1);
			}
		}
	}); // end: ajax call
}

/** 
 * Call when all grphs has been initially loaded
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
	$("#dbx-start-time").html(oldestTs.format("YYYY-MM-DD HH:mm"));
	$("#dbx-end-time"  ).html(newestTs.format("YYYY-MM-DD HH:mm"));

	$('#dbx-report-range span').html(oldestTs.format('YYYY-MM-DD HH:mm') + ' - ' + newestTs.format('YYYY-MM-DD HH:mm'));

	var hourDiff = Math.round(newestTs.diff(oldestTs, 'hours', true));
	$("#dbx-sample-time").html(hourDiff+"h");
	

	console.log("oldestTs: "+oldestTs+" ["+oldestTs.format("YYYY-MM-DD HH:mm:ss")+"], newestTs: "+newestTs+" ["+newestTs.format("YYYY-MM-DD HH:mm:ss")+"], hourDiff="+hourDiff);

	// window.dispatchEvent(new Event('resize'));
}
