/**
 * dbxGraphSettings.js — user settings for the graphs on graph.html (tooltip behaviour)
 *
 * Settings are global for all graphs on the page, stored per browser (localStorage), and can be
 * overridden by URL parameters (the URL wins, and is NOT saved):
 *   ttMode=all|closest                         Tooltip shows all lines at the hovered time, or only the closest line
 *   ttFmt=iso|isoday|time|locale|<moment fmt>  Tooltip timestamp format
 *   ttSort=true|false                          Sort tooltip lines by value (highest first)
 *   ttHideZero=true|false                      Hide lines with value 0 in the tooltip
 *
 * Holding SHIFT while hovering a graph temporarily switches between "all lines" and "closest line".
 *
 * The Chart.js options (see DbxGraph in dbxcentral.graph.js) reference the functions below, which read the
 * settings at call time, so a change is applied without rebuilding any chart.
 *
 * Must be loaded AFTER Chart.js and dbxcentral.utils.js, and BEFORE dbxcentral.graph.js.
 */

var DbxGraphSettings = (function () {

	var STORAGE_PREFIX = 'dbxtune_graph_settings_';
	var STORAGE_KEY    = 'tooltip';

	var TS_FORMATS = {
		iso:    { label: 'ISO',            fmt: 'YYYY-MM-DD HH:mm:ss'       },
		isoday: { label: 'ISO + weekday',  fmt: 'YYYY-MM-DD (dddd) HH:mm:ss'},
		time:   { label: 'Time only',      fmt: 'HH:mm:ss'                  },
		locale: { label: 'Browser locale', fmt: null                        }
	};

	var DEFAULTS = {
		mode:     'closest',  // 'closest' or 'all'
		sort:     true,
		hideZero: false,
		tsFormat: 'isoday'    // a key in TS_FORMATS, or a custom moment.js format string
	};

	var _saved     = load();     // what the user saved (or defaults)
	var _overrides = urlOverrides();
	var _settings  = $.extend({}, _saved, _overrides);

	function load()
	{
		var s = $.extend({}, DEFAULTS);
		try {
			var json = getStorage(STORAGE_PREFIX).get(STORAGE_KEY);
			if (json)
				$.extend(s, JSON.parse(json));
		} catch (e) {
			console.log('DbxGraphSettings: problems reading saved settings, using defaults. Caught: ' + e);
		}
		return s;
	}

	function urlOverrides()
	{
		var o = {};
		var mode = getParameter('ttMode',     '');
		var fmt  = getParameter('ttFmt',      '');
		var sort = getParameter('ttSort',     '');
		var zero = getParameter('ttHideZero', '');

		if (mode === 'all' || mode === 'closest') o.mode     = mode;
		if (fmt  !== '')                          o.tsFormat = fmt;
		if (sort !== '')                          o.sort     = (sort === 'true');
		if (zero !== '')                          o.hideZero = (zero === 'true');
		return o;
	}

	function save(newSettings)
	{
		_saved    = $.extend({}, DEFAULTS, newSettings);
		_settings = $.extend({}, _saved, _overrides);
		try {
			getStorage(STORAGE_PREFIX).set(STORAGE_KEY, JSON.stringify(_saved));
		} catch (e) {
			console.log('DbxGraphSettings: problems saving settings. Caught: ' + e);
		}
	}

	/** moment.js format for a 'tsFormat' setting, or null for "browser locale" */
	function momentFormat(tsFormat)
	{
		return TS_FORMATS.hasOwnProperty(tsFormat) ? TS_FORMATS[tsFormat].fmt : tsFormat;
	}

	function formatTs(ms, tsFormat)
	{
		var fmt = momentFormat(tsFormat);
		return fmt ? moment(ms).format(fmt) : new Date(ms).toLocaleString();
	}

	//--------------------------------------------------------------------------
	// Chart.js hooks (referenced from the DbxGraph chart options)
	//--------------------------------------------------------------------------

	// Custom interaction mode: 'all lines at hovered time' (index) or 'closest line' (nearest), SHIFT swaps them
	if (typeof Chart !== 'undefined')
	{
		Chart.Interaction.modes.dbxTooltip = function (chart, e, options, useFinalPosition)
		{
			var shift = !!(e.native && e.native.shiftKey);
			var all   = (_settings.mode === 'all') !== shift;

			return all
				? Chart.Interaction.modes.index  (chart, e, { intersect: false, axis: 'x'  }, useFinalPosition)
				: Chart.Interaction.modes.nearest(chart, e, { intersect: false, axis: 'xy' }, useFinalPosition);
		};
	}

	function tooltipFilter(tooltipItem)
	{
		var y = tooltipItem.parsed.y;
		if (y === null || y === undefined)
			return false;
		return ! (_settings.hideZero && y === 0);
	}

	function tooltipItemSort(a, b)
	{
		return _settings.sort ? (b.parsed.y - a.parsed.y) : 0;
	}

	function tooltipTitle(tooltipItems)
	{
		if (tooltipItems.length === 0)
			return '';
		return formatTs(tooltipItems[0].parsed.x, _settings.tsFormat);
	}

	//--------------------------------------------------------------------------
	// Settings dialog
	//--------------------------------------------------------------------------
	function openDialog()
	{
		var $dlg = $('#dbx-graph-settings-dialog');
		if ($dlg.length === 0)
		{
			var fmtOptions = Object.keys(TS_FORMATS).map(function (key) {
				var f = TS_FORMATS[key];
				return '<option value="' + key + '">' + f.label + (f.fmt ? ' &nbsp; ' + f.fmt : '') + (key === DEFAULTS.tsFormat ? ' &nbsp; (default)' : '') + '</option>';
			}).join('') + '<option value="custom">Custom...</option>';

			$dlg = $(''
				+ '<div class="modal fade" id="dbx-graph-settings-dialog" tabindex="-1" aria-labelledby="dbx-graph-settings-title" aria-hidden="true">'
				+ '  <div class="modal-dialog" style="max-width:none; width:500px;">'   // max-width:none: so it can be resized wider
				+ '    <div class="modal-content">'
				+ '      <div class="modal-header" style="cursor:move;">'
				+ '        <h5 class="modal-title" id="dbx-graph-settings-title"><i class="fa fa-sliders"></i> Graph Settings</h5>'
				+ '        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>'
				+ '      </div>'
				+ '      <div class="modal-body d-flex flex-column" style="overflow:auto;">'   // flex column: the tips below are pushed to the bottom
				+ '        <h6>Tooltip</h6>'
				+ '        <div class="mb-1">Show values for:</div>'
				+ '        <div class="form-check ms-2">'
				+ '          <input class="form-check-input" type="radio" name="dbx-gs-mode" id="dbx-gs-mode-all" value="all">'
				+ '          <label class="form-check-label" for="dbx-gs-mode-all">All lines at the hovered time</label>'
				+ '        </div>'
				+ '        <div class="form-check ms-2 mb-2">'
				+ '          <input class="form-check-input" type="radio" name="dbx-gs-mode" id="dbx-gs-mode-closest" value="closest">'
				+ '          <label class="form-check-label" for="dbx-gs-mode-closest">Only the line closest to the mouse</label>'
				+ '        </div>'
				+ '        <div class="form-check">'
				+ '          <input class="form-check-input" type="checkbox" id="dbx-gs-sort">'
				+ '          <label class="form-check-label" for="dbx-gs-sort">Sort lines by value (highest first)</label>'
				+ '        </div>'
				+ '        <div class="form-check mb-3">'
				+ '          <input class="form-check-input" type="checkbox" id="dbx-gs-hidezero">'
				+ '          <label class="form-check-label" for="dbx-gs-hidezero">Hide lines with value 0</label>'
				+ '        </div>'
				+ '        <label class="form-label mb-1" for="dbx-gs-tsfmt">Timestamp format:</label>'
				+ '        <select class="form-select form-select-sm" id="dbx-gs-tsfmt">' + fmtOptions + '</select>'
				+ '        <input type="text" class="form-control form-control-sm mt-1" id="dbx-gs-tsfmt-custom" placeholder="moment.js format, for example: YYYY-MM-DD HH:mm:ss.SSS">'
				+ '        <div class="form-text">Preview: <span id="dbx-gs-tsfmt-preview"></span></div>'
				+ '        <div class="alert alert-warning py-1 px-2 mt-2 mb-0 small" id="dbx-gs-url-note" style="display:none;"></div>'
				+ '        <div class="form-text mt-auto pt-3">'
				+ '          <hr class="mt-0">'
				+ '          <b>Mouse tips</b>'
				+ '          <ul class="mb-0 ps-3">'
				+ '            <li>Hold <b>Shift</b> while hovering: switch between "all lines" and "closest line"</li>'
				+ '            <li><b>Click</b> a line: mark that time in all graphs (and switch to history view)</li>'
				+ '            <li><b>Ctrl+click</b> a line: hide that line</li>'
				+ '            <li><b>Click</b> a legend entry: hide/show that line</li>'
				+ '            <li><b>Ctrl+click</b> a legend entry: show only that line (Ctrl+click it again to show all lines)</li>'
				+ '            <li><b>Drag</b> across a graph: zoom in (right-click the graph to reset the zoom)</li>'
				+ '          </ul>'
				+ '        </div>'
				+ '      </div>'
				+ '      <div class="modal-footer">'
				+ '        <button type="button" class="btn btn-outline-secondary me-auto" id="dbx-gs-reset">Reset to defaults</button>'
				+ '        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>'
				+ '        <button type="button" class="btn btn-primary" id="dbx-gs-apply">Apply</button>'
				+ '      </div>'
				+ '    </div>'
				+ '  </div>'
				+ '</div>');
			$('body').append($dlg);

			// Draggable (by the header) and resizable, like the other dialogs on graph.html
			if ($.fn.draggable) $dlg.find('.modal-dialog') .draggable({ handle: '.modal-header' });
			if ($.fn.resizable) $dlg.find('.modal-content').resizable({ alsoResize: $dlg.find('.modal-dialog'), minWidth: 380, minHeight: 300 });

			$('#dbx-gs-tsfmt')       .on('change', function () { $('#dbx-gs-tsfmt-custom').toggle($(this).val() === 'custom'); updatePreview(); });
			$('#dbx-gs-tsfmt-custom').on('input',  updatePreview);
			$('#dbx-gs-reset')       .on('click',  function () { fillDialog(DEFAULTS); });
			$('#dbx-gs-apply')       .on('click',  function () {
				save(readDialog());
				bootstrap.Modal.getOrCreateInstance($dlg[0]).hide();
			});
		}

		// Follow the page color schema (Bootstrap 5.3 color modes)
		$dlg.attr('data-bs-theme', (typeof _colorSchema !== 'undefined' && _colorSchema === 'dark') ? 'dark' : 'light');

		// Tell the user when URL parameters override the saved settings
		var urlKeys = Object.keys(_overrides);
		$('#dbx-gs-url-note')
			.text('Note: URL parameter(s) currently override the saved value for: ' + urlKeys.join(', ') + '. Apply saves your choice, but the URL still wins on this page.')
			.toggle(urlKeys.length > 0);

		fillDialog(_saved);
		bootstrap.Modal.getOrCreateInstance($dlg[0]).show();
	}

	function fillDialog(s)
	{
		$('#dbx-gs-mode-' + (s.mode === 'all' ? 'all' : 'closest')).prop('checked', true);
		$('#dbx-gs-sort')    .prop('checked', !!s.sort);
		$('#dbx-gs-hidezero').prop('checked', !!s.hideZero);

		var isPreset = TS_FORMATS.hasOwnProperty(s.tsFormat);
		$('#dbx-gs-tsfmt')       .val(isPreset ? s.tsFormat : 'custom');
		$('#dbx-gs-tsfmt-custom').val(isPreset ? '' : s.tsFormat).toggle(!isPreset);
		updatePreview();
	}

	function readDialog()
	{
		var fmt = $('#dbx-gs-tsfmt').val();
		if (fmt === 'custom')
			fmt = $.trim($('#dbx-gs-tsfmt-custom').val()) || DEFAULTS.tsFormat;

		return {
			mode:     $('input[name="dbx-gs-mode"]:checked').val(),
			sort:     $('#dbx-gs-sort')    .is(':checked'),
			hideZero: $('#dbx-gs-hidezero').is(':checked'),
			tsFormat: fmt
		};
	}

	function updatePreview()
	{
		var fmt = $('#dbx-gs-tsfmt').val();
		if (fmt === 'custom')
			fmt = $.trim($('#dbx-gs-tsfmt-custom').val()) || DEFAULTS.tsFormat;
		$('#dbx-gs-tsfmt-preview').text(formatTs(Date.now(), fmt));
	}

	return {
		tooltipFilter:   tooltipFilter,
		tooltipItemSort: tooltipItemSort,
		tooltipTitle:    tooltipTitle,
		openDialog:      openDialog,
		get:             function () { return $.extend({}, _settings); }
	};
}());
