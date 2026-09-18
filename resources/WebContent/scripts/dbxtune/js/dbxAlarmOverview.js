/**
 * dbxAlarmOverview.js — Flatten/filter helpers for the "Alarm Overview" (all alarms in all CMs).
 *
 * Data only (no rendering): used by config.html (Bootstrap 5 / bootstrap-table 1.22)
 * and graph.html Counter Details (Bootstrap 4 / hand-built table).
 *
 * Input is 'cmList' from '/api/cc/mgt/config/get', where each CM has 'alarmSettings.alarms[]'
 * (see NoGuiConfigGetServlet).
 */

/**
 * Returns one row per (CM, alarm).
 * <p>
 * isEffective = the alarm will actually be evaluated: CM enabled, CM alarms enabled, CM system alarms enabled, and the alarm itself enabled.
 * isModified  = any of the alarm's parameters (including 'isAlarmEnabled' and 'timeRangeCron') differs from its default.
 */
function dbxAlarmOverviewFlatten(cmList)
{
	var rows = [];
	if ( ! Array.isArray(cmList) )
		return rows;

	cmList.forEach(function(cm)
	{
		var alarms = (cm.alarmSettings && Array.isArray(cm.alarmSettings.alarms)) ? cm.alarmSettings.alarms : [];

		alarms.forEach(function(alarm)
		{
			var params    = Array.isArray(alarm.parameters) ? alarm.parameters : [];
			var mainParam = params.find(function(p) { return p.isMainParam === true; }) || params[0] || {};

			var isCmEnabled           = cm.isCmEnabled           !== false;
			var isCmAlarmEnabled      = cm.isAlarmEnabled        !== false;
			var isSystemAlarmsEnabled = cm.isSystemAlarmsEnabled !== false;
			var isAlarmEnabled        = alarm.isAlarmEnabled     !== false;

			var modifiedParams = params.filter(function(p) { return p.isDefaultValue === false; });

			var row = {
				cmName                : cm.cmName,
				displayName           : cm.displayName || cm.cmName,
				groupName             : cm.groupName || '',
				name                  : alarm.name,
				isCmEnabled           : isCmEnabled,
				isSystemAlarmsEnabled : isSystemAlarmsEnabled,
				isAlarmEnabled        : isAlarmEnabled,
				isEffective           : isCmEnabled && isCmAlarmEnabled && isSystemAlarmsEnabled && isAlarmEnabled,
				mainParamValue        : mainParam.value,
				mainParamDefault      : mainParam.defaultValue,
				mainParamProperty     : mainParam.property || '',
				isModified            : modifiedParams.length > 0,
				modifiedParamNames    : modifiedParams.map(function(p) { return p.name; }),
				paramCount            : params.length,
				timeRangeCron         : alarm.timeRangeCron || '',
				timeRangeDescription  : alarm.timeRangeDescrption || '', // NOTE: spelled that way by NoGuiConfigGetServlet
				description           : alarm.description || '',
				parameters            : params
			};

			// Everything a user might search for: names, descriptions, parameter names/values/property keys
			var parts = [row.cmName, row.displayName, row.groupName, row.name, row.description, row.timeRangeCron];
			params.forEach(function(p) { parts.push(p.name, p.value, p.property); });
			row.searchText = parts.filter(function(s) { return s !== undefined && s !== null; }).join(' ').toLowerCase();

			rows.push(row);
		});
	});

	return rows;
}

/**
 * Filter rows produced by dbxAlarmOverviewFlatten()
 * @param opts {search: 'text', onlyEffective: bool, onlyModified: bool}
 *             'search' is split on whitespace, ALL words must match (case insensitive)
 */
function dbxAlarmOverviewFilter(rows, opts)
{
	opts = opts || {};
	var words = String(opts.search || '').toLowerCase().split(/\s+/).filter(function(w) { return w !== ''; });

	return rows.filter(function(row)
	{
		if (opts.onlyEffective && !row.isEffective) return false;
		if (opts.onlyModified  && !row.isModified ) return false;
		for (var i = 0; i < words.length; i++)
		{
			if (row.searchText.indexOf(words[i]) === -1)
				return false;
		}
		return true;
	});
}

// Allow unit testing from node
if (typeof module !== 'undefined' && module.exports)
	module.exports = { dbxAlarmOverviewFlatten: dbxAlarmOverviewFlatten, dbxAlarmOverviewFilter: dbxAlarmOverviewFilter };
