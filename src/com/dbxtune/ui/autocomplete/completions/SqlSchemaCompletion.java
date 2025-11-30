/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
 * 
 * This file is part of DbxTune
 * DbxTune is a family of sub-products *Tune, hence the Dbx
 * Here are some of the tools: AseTune, IqTune, RsTune, RaxTune, HanaTune, 
 *          SqlServerTune, PostgresTune, MySqlTune, MariaDbTune, Db2Tune, ...
 * 
 * DbxTune is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.dbxtune.ui.autocomplete.completions;

import com.dbxtune.ui.autocomplete.CompletionProviderAbstractSql;
import com.dbxtune.utils.StringUtil;

/**
 * Our own Completion class, which overrides toString() to make it HTML aware
 */
public class SqlSchemaCompletion
extends SqlCompletion
{
	private static final long serialVersionUID = 1L;

	public SchemaInfo _schemaInfo = null;
	
	//-------------------------
	// helper called from constructor
	//-------------------------
	public static String createReplacementText(CompletionProviderAbstractSql provider, SchemaInfo si, String catName, boolean quoteNames)
	{
		String q1 = provider.getDbIdentifierQuoteStringStart();
		String q2 = provider.getDbIdentifierQuoteStringEnd();

		String catalogName = quoteNames ? q1 + si._cat  + q2 : provider.fixStrangeNames(si._cat);
		String schemaName  = quoteNames ? q1 + si._name + q2 : provider.fixStrangeNames(si._name);

		String out = "";
		out += ((catName == null) ? catalogName : catName) + ".";
		out += schemaName;
		
		return out;
	}

	//-------------------------
	// CONSTRUCTOR(S)
	//-------------------------
	public SqlSchemaCompletion(CompletionProviderAbstractSql provider, SchemaInfo si, String catName, boolean quoteNames)
	{
		super(provider, si._name, createReplacementText(provider, si, catName, quoteNames));

		_schemaInfo = si;

		String shortDesc = 
			"<font color='blue'>"+si._name+"</font>" +
			" -- <i><font color='green'>" + (StringUtil.isNullOrBlank(si._remark) ? "" : stripMultiLineHtml(si._remark)) + "</font></i>";
		setShortDescription(shortDesc);
//		setSummary(_schemaInfo.toHtmlString());
	}

	public SqlSchemaCompletion(CompletionProviderAbstractSql provider, String schemaName)
	{
		super(provider, schemaName, provider.fixStrangeNames(schemaName)+".");

		String shortDesc = 
			"<font color='blue'>"+schemaName+"</font>" +
			" -- <i><font color='green'>SCHEMA</font></i>";
		setShortDescription(shortDesc);
	}



	//-------------------------
	// OVERIDE METHODS
	//-------------------------
	
	@Override
	public String getSummary()
	{
		if (_schemaInfo != null)
			return _schemaInfo.toHtmlString();
		return super.getSummary();
	}

	/**
	 * Make it HTML aware
	 */
	@Override
	public String toString()
	{
		return "<html><body>" + super.toString() + "</body></html>";
	}
}
