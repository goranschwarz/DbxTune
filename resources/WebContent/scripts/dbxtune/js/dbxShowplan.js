/**
 * dbxShowplan.js — injectable Postgres + SQL Server showplan dialogs
 *
 * Self-contained: injects its own modal HTML into document.body on DOM ready.
 * No changes to graph.html required beyond adding the <script> tag.
 *
 * Dependencies (loaded before this file):
 *   jQuery, Bootstrap 4, Vue 3 + pev2, QP (html-query-plan), Panzoom,
 *   sqlFormatter, Prism
 *
 * Global functions exposed:
 *   pgShowplanGetSql(), pgShowplanFormatSql(), pgShowplanCopySql(),
 *   pgShowplanCopyPlan(), pgShowplanSaveToFile(), pgShowplanOpenExternal()
 *   ssShowplanEnableZoom(), ssShowplanResetZoom(), ssShowplanFormatSql(),
 *   ssShowplanCopySql(), ssShowplanCopyXml(), ssShowplanSaveXmlToFile(),
 *   ssShowplanOpenExternal(), ssShowplanGetParameters(),
 *   ssShowplanSetParametersInSql()
 *   submit_post_via_hidden_form()
 */
(function () {
	'use strict';

	// -------------------------------------------------------------------------
	// Shared state
	// -------------------------------------------------------------------------
	var pev2app         = undefined;
	var _ssShowplanZoom = undefined;

	// -------------------------------------------------------------------------
	// Inject modal HTML
	// -------------------------------------------------------------------------
	function _injectHtml() {
		if (document.getElementById('dbx-view-pgShowplan-dialog')) return;

		document.body.insertAdjacentHTML('beforeend', [
			// ---- Postgres Execution Plan dialog ----
			"<div class='modal fade' id='dbx-view-pgShowplan-dialog' role='dialog' aria-labelledby='dbx-view-pgShowplan-dialog' aria-hidden='true'>",
			"	<div class='modal-dialog modal-dialog-centered mw-100 w-75' role='document'>",
			"		<div class='modal-content' style='height: 80vh;'>",
			"			<div class='modal-header'>",
			"				<h5 class='modal-title'><b>Postgres Execution Plan:</b> <span id='dbx-view-pgShowplan-objectName'></span></h5>",
			"				<button type='button' class='close' data-dismiss='modal' aria-label='Close'><span aria-hidden='true'>&times;</span></button>",
			"			</div>",
			"			<div class='modal-body' style='overflow-x: auto;'>",
			"				<div class='scroll-tree'>",
			"					<button type='button' class='btn btn-outline-secondary btn-sm' onclick='$(\"#dbx-view-pgShowplan-content\").toggle();'>Hide/Show Below Execution Plan</button>",
			"					<br>",
			"					<div id='dbx-view-pgShowplan-content' class='dbx-view-pgShowplan-content'></div>",
			"					<br>",
			"					<button type='button' class='btn btn-outline-secondary btn-sm' onclick='$(\"#dbx-view-pgShowplan-sqlContent\").toggle();'>Hide/Show Below SQL Text</button>",
			"					<button type='button' class='btn btn-outline-secondary btn-sm' onclick='pgShowplanFormatSql();'>Format Below SQL Text</button>",
			"					<pre><code id='dbx-view-pgShowplan-sqlContent' class='language-sql line-numbers dbx-view-sqltext-content'></code></pre>",
			"					<button type='button' class='btn btn-outline-secondary btn-sm' onclick='$(\"#dbx-view-pgShowplan-planContent\").toggle();'>Hide/Show Below Plan Text</button>",
			"					<pre><code id='dbx-view-pgShowplan-planContent' class='language-json line-numbers dbx-view-sqltext-content'></code></pre>",
			"				</div>",
			"			</div>",
			"			<div class='modal-footer'>",
			"				<button type='button' class='btn btn-outline-secondary' onclick='pgShowplanCopySql();'>Copy SQL Text</button>",
			"				<button type='button' class='btn btn-outline-secondary' onclick='pgShowplanCopyPlan();'>Copy Plan Text</button>",
			"				<button type='button' class='btn btn-outline-secondary' onclick='pgShowplanSaveToFile();'>Save Plan File</button>",
			"				<button type='button' class='btn btn-outline-secondary' onclick='pgShowplanOpenExternal();'>Open Plan in External Window</button>",
			"				&emsp;&emsp;&emsp;&emsp;&emsp;",
			"				<button type='button' class='btn btn-secondary' data-dismiss='modal'>Close</button>",
			"			</div>",
			"		</div>",
			"	</div>",
			"</div>",

			// ---- SQL Server Showplan dialog ----
			"<div class='modal fade' id='dbx-view-ssShowplan-dialog' role='dialog' aria-labelledby='dbx-view-ssShowplan-dialog' aria-hidden='true'>",
			"	<div class='modal-dialog modal-dialog-centered mw-100 w-75' role='document'>",
			"		<div class='modal-content'>",
			"			<div class='modal-header'>",
			"				<h5 class='modal-title'><b>SQL Server Showplan:</b> <span id='dbx-view-ssShowplan-objectName'></span></h5>",
			"				<button type='button' class='close' data-dismiss='modal' aria-label='Close'><span aria-hidden='true'>&times;</span></button>",
			"			</div>",
			"			<div class='modal-body' style='overflow-x: auto;'>",
			"				<div class='scroll-tree' style='width: 3000px;'>",
			"					<button type='button' class='btn btn-outline-secondary btn-sm' onclick='$(\"#dbx-view-ssShowplan-content\").toggle();'>Hide/Show Below Execution Plan</button>",
			"					<button type='button' class='btn btn-outline-secondary btn-sm' onclick='QP.drawLines(document.getElementById(\"dbx-view-ssShowplan-content\"));'>Redraw Lines</button>",
			"					<button type='button' class='btn btn-outline-secondary btn-sm' onclick='ssShowplanEnableZoom();'>Enable Zoom</button>",
			"					<button type='button' class='btn btn-outline-secondary btn-sm' onclick='ssShowplanResetZoom();'>Reset Zoom</button>",
			"					<br><br>",
			"					<div id='dbx-view-ssShowplan-content' class='dbx-view-ssShowplan-content'></div>",
			"					<button type='button' class='btn btn-outline-secondary btn-sm' onclick='$(\"#dbx-view-ssShowplan-sqlContent\").toggle();'>Hide/Show Below SQL Text</button>",
			"					<button type='button' class='btn btn-outline-secondary btn-sm' onclick='ssShowplanFormatSql();'>Format Below SQL Text</button>",
			"					<pre><code id='dbx-view-ssShowplan-sqlContent' class='language-sql line-numbers dbx-view-sqltext-content'></code></pre>",
			"					<button type='button' id='dbx-view-ssShowplan-compileParameterValuesButton' class='btn btn-outline-secondary btn-sm' onclick=\"ssShowplanSetParametersInSql('compile');\">Apply Compile Parameters to the Above SQL Text</button>",
			"					<pre><code id='dbx-view-ssShowplan-compileParameterValues' class='language-sql line-numbers dbx-view-sqltext-content'></code></pre>",
			"					<button type='button' id='dbx-view-ssShowplan-runtimeParameterValuesButton' class='btn btn-outline-secondary btn-sm' onclick=\"ssShowplanSetParametersInSql('runtime');\">Apply Runtime Parameters to the Above SQL Text</button>",
			"					<pre><code id='dbx-view-ssShowplan-runtimeParameterValues' class='language-sql line-numbers dbx-view-sqltext-content'></code></pre>",
			"					<button type='button' class='btn btn-outline-secondary btn-sm' onclick='$(\"#dbx-view-ssShowplan-xmlContent\").toggle();'>Hide/Show Below XML Text</button>",
			"					<pre><code id='dbx-view-ssShowplan-xmlContent' class='language-xml line-numbers dbx-view-sqltext-content'></code></pre>",
			"				</div>",
			"			</div>",
			"			<div class='modal-footer'>",
			"				<button type='button' class='btn btn-outline-secondary' onclick='ssShowplanCopySql();'>Copy SQL Text</button>",
			"				<button type='button' class='btn btn-outline-secondary' onclick='ssShowplanCopyXml();'>Copy XML Text</button>",
			"				<button type='button' class='btn btn-outline-secondary' onclick='ssShowplanSaveXmlToFile();'>Save XML File</button>",
			"				<button type='button' class='btn btn-outline-secondary' onclick='ssShowplanOpenExternal();'>Open Plan in External Window</button>",
			"				&emsp;&emsp;&emsp;&emsp;&emsp;",
			"				<button type='button' class='btn btn-secondary' data-dismiss='modal'>Close</button>",
			"			</div>",
			"		</div>",
			"	</div>",
			"</div>"
		].join('\n'));
	}

	// -------------------------------------------------------------------------
	// Shared utility
	// -------------------------------------------------------------------------
	window.submit_post_via_hidden_form = function (url, params) {
		var hiddenForm = $('<form target="_blank" method="POST" style="display:none;"></form>').attr({ action: url }).appendTo(document.body);
		for (var i in params) {
			if (params.hasOwnProperty(i)) {
				$('<input type="hidden" />').attr({ name: i, value: params[i] }).appendTo(hiddenForm);
			}
		}
		hiddenForm.submit();
		hiddenForm.remove();
	};

	function formatXml(xml) {
		var formatted = '';
		var reg = /(>)(<)(\/*)/g;
		xml = xml.replace(reg, '$1\r\n$2$3');
		var pad = 0;
		jQuery.each(xml.split('\r\n'), function (index, node) {
			var indent = 0;
			if      (node.match(/.+<\/\w[^>]*>$/))    { indent = 0; }
			else if (node.match(/^<\/\w/))             { if (pad !== 0) pad -= 1; }
			else if (node.match(/^<\w[^>]*[^\/]>.*$/)) { indent = 1; }
			else                                        { indent = 0; }
			var padding = '';
			for (var i = 0; i < pad; i++) padding += '  ';
			formatted += padding + node + '\r\n';
			pad += indent;
		});
		return formatted;
	}

	// -------------------------------------------------------------------------
	// Postgres showplan functions
	// -------------------------------------------------------------------------
	window.pgShowplanGetSql = function (planText, setFields) {
		var sqlText = undefined;
		var queryId = undefined;

		planText = planText.trim();
		planText = planText.replace(/^duration: .* ms\s+plan:$/m, '').trim();

		try {
			var json = JSON.parse(planText);
			if (json.hasOwnProperty('Query Text'))       sqlText = json['Query Text'];
			if (json.hasOwnProperty('Query Identifier')) queryId = json['Query Identifier'];
		} catch (e) { /* continue */ }

		if (sqlText === undefined) {
			if (/^Query Text: /.test(planText)) {
				planText = planText.replace(/^Query Text: /, '').trim();
				var endPos = planText.search(/^$/m);
				sqlText = planText.substring(0, endPos).trim();
			}
			var startPos = planText.search(/Query Identifier: /m);
			if (startPos !== -1) queryId = planText.substring(startPos + 'Query Identifier: '.length).trim();
		}

		if (sqlText === undefined)
			sqlText = "-- No 'Query Text' was found in the plan. The SQL Text *may* be available in the above Plan, under 'Query' tab.";

		if (setFields) {
			$('#dbx-view-pgShowplan-sqlContent').text(sqlText);
			$('#dbx-view-pgShowplan-objectName').text(queryId === undefined ? '--Unknown Query Identifier--' : queryId);
		}
		return sqlText;
	};

	window.pgShowplanFormatSql = function () {
		var formatOptions = { language: 'postgresql', tabWidth: 4, keywordCase: 'upper', tabulateAlias: true };
		var sqlText = $('#dbx-view-pgShowplan-sqlContent').text();
		try {
			$('#dbx-view-pgShowplan-sqlContent').text(sqlFormatter.format(sqlText, formatOptions));
			Prism.highlightAll();
		} catch (err) { alert(err); }
	};

	window.pgShowplanOpenExternal = function () {
		var planText = $('#dbx-view-pgShowplan-planContent').text();
		var toUrl = '/showplan/postgres';
		if (!window.location.toString().toLowerCase().startsWith('http')) toUrl = 'http://dbxtune.gorans.org' + toUrl;
		submit_post_via_hidden_form(toUrl, { plan: planText });
	};

	window.pgShowplanSaveToFile = function () {
		var planText  = $('#dbx-view-pgShowplan-planContent').text();
		var planName  = $('#dbx-view-pgShowplan-objectName').text() || 'name';
		var link = document.createElement('a');
		link.href = URL.createObjectURL(new Blob([planText], { type: 'text/plain' }));
		link.download = 'pg_execution_plan_' + planName + '.pgplan';
		link.click();
		URL.revokeObjectURL(link.href);
	};

	window.pgShowplanCopySql = function () {
		var txt = $('#dbx-view-pgShowplan-sqlContent').text();
		var ta = document.createElement('textarea'); ta.value = txt; document.body.appendChild(ta); ta.select();
		try { document.execCommand('copy'); } catch (err) { alert('Unable to copy\n\n' + err); }
		document.body.removeChild(ta);
	};

	window.pgShowplanCopyPlan = function () {
		var txt = $('#dbx-view-pgShowplan-planContent').text();
		var ta = document.createElement('textarea'); ta.value = txt; document.body.appendChild(ta); ta.select();
		try { document.execCommand('copy'); } catch (err) { alert('Unable to copy\n\n' + err); }
		document.body.removeChild(ta);
	};

	// -------------------------------------------------------------------------
	// SQL Server showplan functions
	// -------------------------------------------------------------------------
	window.ssShowplanEnableZoom = function () {
		if (_ssShowplanZoom !== undefined) return;
		var elem = document.getElementById('dbx-view-ssShowplan-content');
		_ssShowplanZoom = Panzoom(elem, { maxScale: 1, minScale: 0.01 });
		elem.addEventListener('wheel', _ssShowplanZoom.zoomWithWheel);
	};

	window.ssShowplanResetZoom = function () {
		if (_ssShowplanZoom !== undefined) _ssShowplanZoom.reset();
	};

	window.ssShowplanFormatSql = function () {
		var formatOptions = { language: 'tsql', tabWidth: 4, keywordCase: 'upper', tabulateAlias: true };
		var sqlText = $('#dbx-view-ssShowplan-sqlContent').text();
		try {
			$('#dbx-view-ssShowplan-sqlContent').text(sqlFormatter.format(sqlText, formatOptions));
			Prism.highlightAll();
		} catch (err) { alert(err); }
	};

	window.ssShowplanOpenExternal = function () {
		var planText = $('#dbx-view-ssShowplan-xmlContent').text();
		var toUrl = '/showplan/sqlserver';
		if (!window.location.toString().toLowerCase().startsWith('http')) toUrl = 'http://dbxtune.gorans.org' + toUrl;
		submit_post_via_hidden_form(toUrl, { plan: planText });
	};

	window.ssShowplanSaveXmlToFile = function () {
		var xmlText  = $('#dbx-view-ssShowplan-xmlContent').text();
		var planName = $('#dbx-view-ssShowplan-objectName').text();
		planName = planName ? 'for_' + planName.replace(', ', '__') : 'name';
		var link = document.createElement('a');
		link.href = URL.createObjectURL(new Blob([xmlText], { type: 'text/plain' }));
		link.download = 'showplan_' + planName + '.xml.sqlplan';
		link.click();
		URL.revokeObjectURL(link.href);
	};

	window.ssShowplanCopyXml = function () {
		var txt = $('#dbx-view-ssShowplan-xmlContent').text();
		var ta = document.createElement('textarea'); ta.value = txt; document.body.appendChild(ta); ta.select();
		try { document.execCommand('copy'); } catch (err) { alert('Unable to copy\n\n' + err); }
		document.body.removeChild(ta);
	};

	window.ssShowplanGetSql = function (fallbackSqlText) {
		var xmlText = $('#dbx-view-ssShowplan-xmlContent').text();
		var tmpSql  = '-- No SQL StatementText was found in the XML Plan. The below SQL is from column \'lastKnownSql\':\n' + fallbackSqlText;
		$('#dbx-view-ssShowplan-sqlContent').text(tmpSql);

		var xmlDoc           = $.parseXML(xmlText);
		var sqlTextArr       = [];
		var queryHashArr     = [];
		var queryPlanHashArr = [];
		$(xmlDoc).find('StmtSimple').each(function (i, e) {
			sqlTextArr      .push($(e).attr('StatementText'));
			queryHashArr    .push($(e).attr('QueryHash'));
			queryPlanHashArr.push($(e).attr('QueryPlanHash'));
		});

		var sqlText = '';
		if (sqlTextArr.length === 1) {
			sqlText = sqlTextArr[0];
		} else {
			for (var i = 0; i < sqlTextArr.length; i++) {
				sqlText += '--===================================================\n-- SQL Statement ' + (i + 1) + ' of ' + sqlTextArr.length + '\n-----------------------------------------------------\n';
				sqlText += sqlTextArr[i] + '\n-- end ----------------------------------------------\n\n';
			}
		}

		if (queryPlanHashArr.length > 0 && $('#dbx-view-ssShowplan-objectName').text() === '') {
			$('#dbx-view-ssShowplan-objectName').text('QueryHash=' + queryHashArr[0] + ', QueryPlanHash=' + queryPlanHashArr[0]);
		}

		return sqlText;
	};

	window.ssShowplanCopySql = function () {
		var txt = $('#dbx-view-ssShowplan-sqlContent').text();
		var ta = document.createElement('textarea'); ta.value = txt; document.body.appendChild(ta); ta.select();
		try { document.execCommand('copy'); } catch (err) { alert('Unable to copy\n\n' + err); }
		document.body.removeChild(ta);
	};

	window.ssShowplanGetParameters = function () {
		var xmlText = $('#dbx-view-ssShowplan-xmlContent').text();
		var xmlDoc  = $.parseXML(xmlText);
		var compileArr = [], runtimeArr = [];

		$(xmlDoc).find('ParameterList').find('ColumnReference').each(function (i, e) {
			if (e.hasAttribute('ParameterCompiledValue'))
				compileArr.push('CompiledValue: Parameter="' + e.getAttribute('Column') + '" Value="' + e.getAttribute('ParameterCompiledValue') + '" DataType="' + e.getAttribute('ParameterDataType') + '"');
			if (e.hasAttribute('ParameterRuntimeValue'))
				runtimeArr.push('RuntimeValue: Parameter="' + e.getAttribute('Column') + '" Value="' + e.getAttribute('ParameterRuntimeValue') + '" DataType="' + e.getAttribute('ParameterDataType') + '"');
		});
		compileArr = compileArr.reverse();
		runtimeArr = runtimeArr.reverse();

		if (compileArr.length > 0) {
			$('#dbx-view-ssShowplan-compileParameterValues').text(compileArr.join('\n'));
			$('#dbx-view-ssShowplan-compileParameterValuesButton').show();
		} else {
			$('#dbx-view-ssShowplan-compileParameterValues').text('-- No Compile Parameters was found in the XML Plan.');
			$('#dbx-view-ssShowplan-compileParameterValuesButton').hide();
		}

		if (runtimeArr.length > 0) {
			$('#dbx-view-ssShowplan-runtimeParameterValues').text(runtimeArr.join('\n'));
			$('#dbx-view-ssShowplan-runtimeParameterValuesButton').show();
		} else {
			$('#dbx-view-ssShowplan-runtimeParameterValues').text('-- No Runtime Parameters was found in the XML Plan.');
			$('#dbx-view-ssShowplan-runtimeParameterValuesButton').hide();
		}
	};

	window.ssShowplanSetParametersInSql = function (paramType) {
		var xmlText = $('#dbx-view-ssShowplan-xmlContent').text();
		var xmlDoc  = $.parseXML(xmlText);
		$(xmlDoc).find('ParameterList').find('ColumnReference').each(function (i, e) {
			var paramName  = e.getAttribute('Column');
			var paramValue = '-unknown-';
			if (paramType === 'compile' && e.hasAttribute('ParameterCompiledValue')) paramValue = e.getAttribute('ParameterCompiledValue');
			if (paramType === 'runtime' && e.hasAttribute('ParameterRuntimeValue'))  paramValue = e.getAttribute('ParameterRuntimeValue');
			var sqlText = $('#dbx-view-ssShowplan-sqlContent').text();
			$('#dbx-view-ssShowplan-sqlContent').text(sqlText.replaceAll(paramName, paramValue));
			Prism.highlightAll();
		});
	};

	// -------------------------------------------------------------------------
	// Event handlers
	// -------------------------------------------------------------------------
	function _initHandlers() {

		// Postgres: set fields before modal becomes visible
		$('#dbx-view-pgShowplan-dialog').on('show.bs.modal', function (e) {
			var data = $(e.relatedTarget).data();
			$('#dbx-view-pgShowplan-objectName',  this).text(data.objectname);
			$('#dbx-view-pgShowplan-planContent', this).text(data.tooltip);
			pgShowplanGetSql(data.tooltip, true);
		});

		// Postgres: mount PEV2 Vue component after modal is visible
		$('#dbx-view-pgShowplan-dialog').on('shown.bs.modal', function (e) {
			var data = $(e.relatedTarget).data();
			var container = document.getElementById('dbx-view-pgShowplan-content');
			container.innerHTML = "<pev2 style='min-height: 55vh;' :plan-source='plan' :plan-query='query' />";
			var app = Vue.createApp({ data: function () { return { plan: data.tooltip, query: '' }; } });
			app.component('pev2', pev2.Plan);
			pev2app = app;
			app.mount('#dbx-view-pgShowplan-content');
			Prism.highlightAll();
		});

		// Postgres: unmount PEV2 when modal closes
		$('#dbx-view-pgShowplan-dialog').on('hidden.bs.modal', function () {
			if (pev2app) { pev2app.unmount(); pev2app = undefined; }
		});

		// SQL Server: set fields before modal becomes visible
		$('#dbx-view-ssShowplan-dialog').on('show.bs.modal', function (e) {
			var data = $(e.relatedTarget).data();
			$('#dbx-view-ssShowplan-objectName', this).text(data.objectname);
			$('#dbx-view-ssShowplan-content',    this).text('Loading plan...');
			$('#dbx-view-ssShowplan-xmlContent', this).text(formatXml(data.tooltip));
			$('#dbx-view-ssShowplan-sqlContent', this).text(ssShowplanGetSql(data.sqltext));
			ssShowplanGetParameters();
			ssShowplanResetZoom();
		});

		// SQL Server: draw plan after modal is visible
		$('#dbx-view-ssShowplan-dialog').on('shown.bs.modal', function (e) {
			var data = $(e.relatedTarget).data();
			QP.showPlan(document.getElementById('dbx-view-ssShowplan-content'), data.tooltip);
			Prism.highlightAll();
		});
	}

	// -------------------------------------------------------------------------
	// Bootstrap
	// -------------------------------------------------------------------------
	$(document).ready(function () {
		_injectHtml();
		_initHandlers();
	});

}());
