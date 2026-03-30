/**
 * dbxSqlText.js — injectable SQL Text + Lock Table dialogs
 *
 * Self-contained: injects its own modal HTML into document.body on DOM ready.
 * No changes to graph.html required beyond adding the <script> tag.
 *
 * Global functions exposed:
 *   sqlTextDialogCopySql()
 *   sqlTextDialogFormatSQL()
 */
(function () {
	'use strict';

	// -------------------------------------------------------------------------
	// Inject modal HTML
	// -------------------------------------------------------------------------
	function _injectHtml() {
		if (document.getElementById('dbx-view-sqltext-dialog')) return; // already present

		document.body.insertAdjacentHTML('beforeend', [
			// ---- SQL Text dialog ----
			"<div class='modal fade' id='dbx-view-sqltext-dialog' role='dialog' aria-labelledby='dbx-view-sqltext-dialog' aria-hidden='true'>",
			"	<div class='modal-dialog modal-dialog-centered mw-100 w-75' role='document'>",
			"		<div class='modal-content'>",
			"			<div class='modal-header'>",
			"				<h5 class='modal-title' id='dbx-view-sqltext-dialog-title'><b>Text Type:</b> <span id='dbx-view-sqltext-objectName'></span> <span id='dbx-view-sqltext-sqlDialect'></span></h5>",
			"				<button type='button' class='close' data-dismiss='modal' aria-label='Close'><span aria-hidden='true'>&times;</span></button>",
			"			</div>",
			"			<div class='modal-body' style='overflow-x: auto;'>",
			"				<div class='scroll-tree' style='width: 3000px;'>",
			"					<pre><code id='dbx-view-sqltext-content' class='language-sql line-numbers dbx-view-sqltext-content'></code></pre>",
			"				</div>",
			"			</div>",
			"			<div class='modal-footer'>",
			"				<button type='button' class='btn btn-outline-secondary' onclick='sqlTextDialogFormatSQL();'>Format SQL Text</button>",
			"				<button type='button' class='btn btn-outline-secondary' onclick='sqlTextDialogCopySql();'>Copy SQL Text</button>",
			"				&emsp;&emsp;&emsp;&emsp;&emsp;",
			"				<button type='button' class='btn btn-secondary' data-dismiss='modal'>Close</button>",
			"			</div>",
			"		</div>",
			"	</div>",
			"</div>",

			// ---- Lock Table dialog ----
			"<div class='modal fade' id='dbx-view-lockTable-dialog' role='dialog' aria-labelledby='dbx-view-lockTable-dialog' aria-hidden='true'>",
			"	<div class='modal-dialog modal-dialog-centered mw-100 w-75' role='document'>",
			"		<div class='modal-content'>",
			"			<div class='modal-header'>",
			"				<h5 class='modal-title' id='dbx-view-lockTable-dialog-title'><b><span id='dbx-view-lockTable-objectName'></span></b></h5>",
			"				<button type='button' class='close' data-dismiss='modal' aria-label='Close'><span aria-hidden='true'>&times;</span></button>",
			"			</div>",
			"			<div class='modal-body' style='overflow-x: auto;'>",
			"				<div class='scroll-tree' style='width: 3000px;'>",
			"					<div id='dbx-view-lockTable-content' class='dbx-view-lockTable-content'></div>",
			"				</div>",
			"			</div>",
			"			<div class='modal-footer'>",
			"				<button type='button' class='btn btn-secondary' data-dismiss='modal'>Close</button>",
			"			</div>",
			"		</div>",
			"	</div>",
			"</div>"
		].join('\n'));
	}

	// -------------------------------------------------------------------------
	// Functions (global so inline onclick= handlers in the modal can call them)
	// -------------------------------------------------------------------------
	window.sqlTextDialogCopySql = function () {
		var sqlText = $('#dbx-view-sqltext-content').text();
		var textArea = document.createElement('textarea');
		textArea.value = sqlText;
		document.body.appendChild(textArea);
		textArea.select();
		try {
			document.execCommand('copy');
		} catch (err) {
			alert('Unable to copy to clipboard\n\n' + err);
		}
		document.body.removeChild(textArea);
	};

	window.sqlTextDialogFormatSQL = function () {
		var sqlDialect = $('#dbx-view-sqltext-sqlDialect').text();
		if (sqlDialect === '') sqlDialect = 'tsql';

		var formatOptions = {
			language:      sqlDialect,
			tabWidth:      4,
			keywordCase:   'upper',
			tabulateAlias: true
		};

		var sqlText = $('#dbx-view-sqltext-content').text();

		// Remove leading ')' — known issue in SqlServerTune
		if (sqlText.startsWith(')')) sqlText = sqlText.substr(1);

		// Handle ASE "DYNAMIC_SQL dyn198: ..." prefix
		if (sqlText.startsWith('DYNAMIC_SQL ')) {
			var startPos = sqlText.indexOf(':');
			if (startPos >= 0) sqlText = sqlText.substr(startPos + 1);
		}

		// If it's a Sybase showplan, extract the SQL from between the markers
		var isSybaseShowplan = false;
		var searchForStr = 'Showplan:---- BEGIN: SQL Statement Executed ------------------------------------';
		if (sqlText.startsWith(searchForStr)) {
			var sp = sqlText.indexOf(searchForStr) + searchForStr.length;
			var ep = sqlText.indexOf('---- END: SQL Statement Executed --------------------------------------') - 1;
			if (sp >= 0 && ep >= 0) {
				isSybaseShowplan = true;
				sqlText = sqlText.substring(sp + 1, ep);
				formatOptions = {
					language:      'tsql',
					tabWidth:      4,
					keywordCase:   'upper',
					tabulateAlias: true,
					paramTypes: { positional: true, numbered: [], named: ['@'] }
				};
			}
		}

		try {
			var formattedSqlText = sqlFormatter.format(sqlText, formatOptions);
			if (isSybaseShowplan) {
				var originTxt = $('#dbx-view-sqltext-content').text();
				var newTxt    = originTxt.replace(sqlText, formattedSqlText);
				$('#dbx-view-sqltext-content').text(newTxt);
			} else {
				$('#dbx-view-sqltext-content').text(formattedSqlText);
			}
			Prism.highlightAll();
		} catch (error) {
			alert(error);
		}
	};

	// -------------------------------------------------------------------------
	// Event handlers
	// -------------------------------------------------------------------------
	function _initHandlers() {
		$('#dbx-view-sqltext-dialog').on('shown.bs.modal', function (e) {
			var data = $(e.relatedTarget).data();
			$('#dbx-view-sqltext-objectName', this).text(data.objectname);
			$('#dbx-view-sqltext-sqlDialect', this).text(data.sqldialect);
			$('#dbx-view-sqltext-content',    this).text(data.tooltip);
			Prism.highlightAll();
			$('#dbx-view-sqltext-dialog').animate({ scrollTop: 0 }, 'slow');
		});

		$('#dbx-view-lockTable-dialog').on('shown.bs.modal', function (e) {
			var data = $(e.relatedTarget).data();
			$('#dbx-view-lockTable-objectName', this).text(data.objectname);
			$('#dbx-view-lockTable-content',    this).html(data.tooltip);
			$('#dbx-view-lockTable-dialog').animate({ scrollTop: 0 }, 'slow');
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
