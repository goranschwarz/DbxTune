/**
 * dbxGraphBus.js — event bus + module wiring for graph.html
 *
 * GraphBus decouples the two drivers (WebSocket live data, history slider)
 * from the four panels (Active Statements, Counter Details, Alarm History,
 * DBMS Config).
 *
 * Online mode:  WebSocket data arrives
 *               → GraphBus.emit('ws-data', { srvName })
 *               → each module's listener decides what to do
 *
 * History mode: slider moves / user clicks graph timestamp
 *               → GraphBus.emit('slider-change', { ts, startTime, endTime, value, historyStart })
 *               → each module's listener decides what to do
 *
 * Must be loaded BEFORE dbxcentral.graph.js and dbxGraphPage.js.
 */

// ---------------------------------------------------------------------------
// GraphBus — tiny pub/sub
// ---------------------------------------------------------------------------
var GraphBus = (function () {
	var _h = {};
	return {
		on:   function (ev, fn) { (_h[ev] = _h[ev] || []).push(fn); },
		off:  function (ev, fn) { if (_h[ev]) _h[ev] = _h[ev].filter(function (f) { return f !== fn; }); },
		emit: function (ev, d)  { (_h[ev] || []).slice().forEach(function (fn) { try { fn(d); } catch(e) { console.error('GraphBus[' + ev + ']:', e); } }); }
	};
}());

// ---------------------------------------------------------------------------
// Module wiring — registered on DOM ready so all functions are available
// ---------------------------------------------------------------------------
$(document).ready(function () {

	// ---- slider-change listeners -------------------------------------------

	// Active Statements: load history statements for this time window
	GraphBus.on('slider-change', function (d) {
		if (typeof dbxTuneGetHistoryStatements === 'function')
			dbxTuneGetHistoryStatements(d.startTime, d.endTime);
	});

	// Counter Details: refresh CM data if panel is open
	GraphBus.on('slider-change', function (d) {
		if (typeof cmDetailSliderRefresh === 'function')
			cmDetailSliderRefresh(d.startTime);
	});

	// DBMS Config: refresh config if panel is open
	GraphBus.on('slider-change', function (d) {
		if (typeof dbmsConfigSliderRefresh === 'function')
			dbmsConfigSliderRefresh(d.startTime);
	});

	// Alarm History: load alarms around this timestamp.
	// Pass historyStart so the panel can widen the fetch window to catch
	// alarms that were raised before the default 24-hour lookback.
	GraphBus.on('slider-change', function (d) {
		if (typeof alarmPanelSliderRefresh === 'function')
			alarmPanelSliderRefresh(d.startTime, d.historyStart);
	});

	// ---- ws-data listeners -------------------------------------------------

	// Active Statements: poll for latest sample
	// (setActiveStatement() inside already handles auto-open when count > 0
	//  and the "Auto Open" option is enabled)
	GraphBus.on('ws-data', function (d) {
		if (typeof dbxTuneCheckActiveStatements === 'function')
			dbxTuneCheckActiveStatements();
	});

	// Counter Details: reload current CM when new data arrives
	// (cmDetailLiveRefresh() already skips if paused or panel is closed)
	GraphBus.on('ws-data', function (d) {
		if (typeof cmDetailLiveRefresh === 'function')
			cmDetailLiveRefresh(d.srvName);
	});

	// DBMS Config: no live data reload, but trigger the once-only auto-open restore on first WS tick
	GraphBus.on('ws-data', function (d) {
		if (typeof _dcAutoOpenChecked !== 'undefined' && !_dcAutoOpenChecked) {
			_dcAutoOpenChecked = true;
			if (!$('#dbms-config-panel').is(':visible')) {
				try { if (localStorage.getItem('dbmsConfig-panelOpen') === '1') dbmsConfigToggle(); } catch(e) {}
			}
		}
	});

	// Alarm History: update badge + auto-open/close based on active alarm count
	// (alarmPanelLiveRefresh() already checks #alarm-auto-open-chk internally)
	GraphBus.on('ws-data', function (d) {
		if (typeof alarmPanelLiveRefresh === 'function')
			alarmPanelLiveRefresh(d.srvName);
	});

});
