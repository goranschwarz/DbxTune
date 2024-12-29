/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
 * 
 * This file is part of DbxTune
 * DbxTune is a family of sub-products *Tune, hence the Dbx
 * Here are some of the tools: AseTune, IqTune, RsTune, RaxTune, HanaTune, 
 *          SqlServerTune, PostgresTune, MySqlTune, MariaDbTune, Db2Tune, ...
 * 
 * DbxTune is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.dbxtune.mgt.controllers;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;

import com.dbxtune.utils.StringUtil;

public class HtmlBootstrap4ModalDialog
{
	private String _id;

	public String getId() { return _id; }
	
	public HtmlBootstrap4ModalDialog(String id)
	{
		_id = id;
	}
	
	public void createHtml(PrintWriter writer)
	{
		writer.println();
		writer.println("<!-- ################################################################ -->");
		writer.println("<!-- ## BEGIN: Bootstrap 4 - Modal Dialog -->");
		writer.println("<!-- ## ID: " + getId() + " -->");
		writer.println("<!-- ################################################################ -->");

		writer.println();
		writer.println("<!-- HTML part -->");
		writer.println("    <div class='modal fade' id='dbx-" + getId() + "-dialog' role='dialog' aria-labelledby='dbx-" + getId() + "-dialog' aria-hidden='true'> ");
		writer.println("        <div class='modal-dialog modal-dialog-centered mw-100 w-75' role='document'> ");
		writer.println("            <div class='modal-content'> ");
		writer.println("                <div class='modal-header'> ");
		writer.println("                    <h5 class='modal-title' id='dbx-" + getId() + "-dialog-title'><b>Text Type:</b> <span id='dbx-" + getId() + "-objectName'></span> <span id='dbx-" + getId() + "-sqlDialect'></span></h5> ");
		writer.println("                    <button type='button' class='close' data-dismiss='modal' aria-label='Close'> ");
		writer.println("                        <span aria-hidden='true'>&times;</span> ");
		writer.println("                    </button> ");
		writer.println("                </div> ");
		writer.println("                <div class='modal-body' style='overflow-x: auto;'> ");

		
		// User defined content
		craeteModalBody(writer);

//		writer.println("                    <div class='scroll-tree' style='width: 3000px;'> ");
//		writer.println("                        <pre><code id='dbx-" + getId() + "-content' class='language-sql line-numbers dbx-" + getId() + "-content' ></code></pre> ");
//		writer.println("                    </div> ");
		writer.println("                </div> ");
		writer.println("                <div class='modal-footer'> ");
		for (Button entry : getButtons())
		{
			writer.println("                    <button type='button' class='btn btn-outline-" + entry._buttonType + "' onclick='" + entry._functionNameOnClick +"();'>" + entry._name+ "</button> ");
		}
		writer.println("                    &emsp;&emsp;&emsp;&emsp;&emsp; ");
		writer.println("                    <button type='button' class='btn btn-secondary' data-dismiss='modal'>Close</button> ");
		writer.println("                </div> ");
		writer.println("            </div> ");
		writer.println("        </div> ");
		writer.println("    </div> ");

		writer.println();
		writer.println("<!-- JavaScript part -->");
		writer.println("<script>");
		
		writer.println();
		writer.println("    <-- SHOW MODAL FUNCTION (before visible) --> ");
		writer.println("    $('#dbx-" + getId() + "-dialog').on('show.bs.modal', function(e) ");
		writer.println("    { ");
		writer.println("        var data = $(e.relatedTarget).data(); ");
		writer.println();
		writer.println("        console.log('SHOW: #dbx-" + getId() + "-dialog: data: ' + data, data); ");
		writer.println();
		writer.println("        $('#dbx-" + getId() + "-objectName', this).text(data.objectname); ");
		writer.println("        $('#dbx-" + getId() + "-content',    this).html(data.tooltip); ");
		writer.println("        $('#dbx-" + getId() + "-label',      this).html(data.label); ");
		writer.println("    }); ");

		writer.println();
		writer.println("    <-- SHOWN MODAL FUNCTION (after has become visible) --> ");
		writer.println("    $('#dbx-" + getId() + "-dialog').on('shown.bs.modal', function(e) ");
		writer.println("    { ");
		writer.println("        var data = $(e.relatedTarget).data(); ");
		writer.println();
		writer.println("        console.log('SHOWN: #dbx-" + getId() + "-dialog: data: ' + data, data); ");
		writer.println();
		writer.println("        // Scroll top top ");
		writer.println("        $('#dbx-" + getId() + "-dialog').animate({ scrollTop: 0 }, 'slow'); ");
		writer.println("    }); ");

		for (Button entry : getButtons())
		{
			if (StringUtil.hasValue(entry._functionJsContent))
			{
				writer.println();
				writer.println("<-- FUNCTION: " + entry._functionNameOnClick + " --> ");
				writer.println("function " + entry._functionNameOnClick + "() ");
				writer.println("{ ");
				writer.println(entry._functionJsContent);
				writer.println("} ");
			}
		}

		// User defined content
		createJavaScript(writer);

		writer.println("</script>");

		writer.println();
		writer.println("<!-- ################################################################ -->");
		writer.println("<!-- ## END: Bootstrap 4 - Modal Dialog -->");
		writer.println("<!-- ## ID: " + getId() + " -->");
		writer.println("<!-- ################################################################ -->");
		writer.println();
	}

	public enum ButtonType
	{
		primary, secondary, success, info, warning, danger, dark, light, link;
	};
	public static class Button
	{
		String     _name;
		ButtonType _buttonType = ButtonType.secondary;
		String     _functionNameOnClick;
		String     _functionJsContent;

		public Button(String name, ButtonType buttonType)
		{
			this(name, buttonType, null, null);
		}
		public Button(String name, ButtonType buttonType, String functionNameOnClick, String functionJsContent)
		{
			_name       = name;
			_buttonType = buttonType;
			_functionNameOnClick = functionNameOnClick;
			_functionJsContent   = functionJsContent;
		}
	}

	/**  */
	public List<Button> getButtons()
	{
		return Collections.emptyList();
	}
	
	/** implement this to create JavaScript specifics */
	public void createJavaScript(PrintWriter writer)
	{
	}

	/** 
	 * This creates a DIV with id: "dbx-" + getId() + "-content" and a class with the same name as the 'id'
	 * implement this write Content for the modal dialog 
	 */
	public void craeteModalBody(PrintWriter writer)
	{
		writer.println("                    <div id='dbx-" + getId() + "-content' class='dbx-" + getId() + "-content'></div> ");
	}
}
