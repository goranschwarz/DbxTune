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

import org.fife.ui.autocomplete.CompletionProvider;

import com.dbxtune.ui.autocomplete.CompletionProviderAbstract;
import com.dbxtune.ui.autocomplete.CompletionProviderAbstractSql;
import com.dbxtune.utils.StringUtil;

/**
 * Our own Completion class, which overrides toString() to make it HTML aware
 */
public class SqlFunctionCompletion
extends SqlCompletion
{
	private static final long serialVersionUID = 1L;

	public FunctionInfo _functionInfo = null;

	//-------------------------
	// helper called from constructor
	//-------------------------
	public static String createReplacementText(CompletionProviderAbstractSql provider, FunctionInfo ti, boolean addCatalog, boolean addSchema, boolean quoteNames)
	{
		String q1 = provider.getDbIdentifierQuoteStringStart();
		String q2 = provider.getDbIdentifierQuoteStringEnd();

		String catalogName = quoteNames ? q1 + ti._funcCat    + q2 : provider.fixStrangeNames(ti._funcCat);
		String schemaName  = quoteNames ? q1 + ti._funcSchema + q2 : provider.fixStrangeNames(ti._funcSchema);
		String tableName   = quoteNames ? q1 + ti._funcName   + q2 : provider.fixStrangeNames(ti._funcName);

		// If the schemaname/owner is 'dbo', do not prefix it with 'dbo.'
//		if ("dbo".equalsIgnoreCase(ti._funcSchema))
//		{
//			schemaName = "";
//			addSchema = addCatalog; // if catalog is true, we need to add a simple '.'
//		}

		String dbmsDefaultSchemaName = provider.getDbDefaultSchemaName();
		if (StringUtil.hasValue(dbmsDefaultSchemaName) && dbmsDefaultSchemaName.equals(ti._funcSchema))
			addSchema = false;

		String[] dbmsSkipResolvSchemaName = provider.getDbSkipResolvSchemaName();
		for (String schName : dbmsSkipResolvSchemaName)
		{
			if (StringUtil.hasValue(schName) && schName.equals(ti._funcSchema))
				addSchema = false;
		}

		String out = "";
		if (addCatalog) out += catalogName + ".";
		if (addSchema)  out += schemaName  + ".";
		out += tableName;
		
		return out;
	}

	//-------------------------
	// CONSTRUCTOR
	//-------------------------
	public SqlFunctionCompletion(CompletionProviderAbstractSql provider, FunctionInfo fi, boolean addCatalog, boolean addSchema, boolean quoteNames)
	{
		super(provider, fi._funcName, createReplacementText(provider, fi, addCatalog, addSchema, quoteNames));
		_functionInfo = fi;

		String shortDesc = 
			"<font color='blue'>"+fi._funcType+"</font>" +
//			" -- <i><font color='green'>" + (StringUtil.isNullOrBlank(ti._funcRemark) ? "No Description" : ti._funcRemark) + "</font></i>";
			" -- <i><font color='green'>" + (StringUtil.isNullOrBlank(fi._funcRemark) ? "" : stripMultiLineHtml(fi._funcRemark)) + "</font></i>";
		setShortDescription(shortDesc);
//		setSummary(_functionInfo.toHtmlString());
	}
	


	//-------------------------
	// OVERIDE METHODS
	//-------------------------
	
	public String getType()
	{
		if (_functionInfo          == null) return "";
		if (_functionInfo._funcType == null) return "";
		return _functionInfo._funcType;
	}
	public String getName()
	{
		if (_functionInfo          == null) return "";
		if (_functionInfo._funcName == null) return "";
		return _functionInfo._funcName;
	}

	@Override
	public String getSummary()
	{
		if ( ! _functionInfo.isColumnRefreshed() )
		{
			CompletionProvider cp = getProvider();
			if (cp instanceof CompletionProviderAbstract)
				_functionInfo.refreshColumnInfo(((CompletionProviderAbstract)cp).getConnectionProvider());
		}
		return _functionInfo.toHtmlString();
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
