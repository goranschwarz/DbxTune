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

import com.dbxtune.ui.autocomplete.CompletionProviderAbstractSql;
import com.dbxtune.utils.StringUtil;

/**
 * Our own Completion class, which overrides toString() to make it HTML aware
 */
public class SqlTableCompletion
extends SqlCompletion
{
	private static final long serialVersionUID = 1L;

	public TableInfo _tableInfo = null;

	//-------------------------
	// helper called from constructor
	//-------------------------
	public static String createReplacementText(CompletionProviderAbstractSql provider, TableInfo ti, boolean addCatalog, boolean addSchema, boolean quoteNames)
	{
		String q1 = provider.getDbIdentifierQuoteStringStart();
		String q2 = provider.getDbIdentifierQuoteStringEnd();

		String catalogName = quoteNames ? q1 + ti._tabCat    + q2 : provider.fixStrangeNames(ti._tabCat);
		String schemaName  = quoteNames ? q1 + ti._tabSchema + q2 : provider.fixStrangeNames(ti._tabSchema);
		String tableName   = quoteNames ? q1 + ti._tabName   + q2 : provider.fixStrangeNames(ti._tabName);

		// If the schemaname/owner is 'dbo', do not prefix it with 'dbo.'
//		if ("dbo".equalsIgnoreCase(ti._tabSchema))
//		{
//			schemaName = "";
//			addSchema = addCatalog; // if catalog is true, we need to add a simple '.'
//		}

		String dbmsDefaultSchemaName = provider.getDbDefaultSchemaName();
		if (StringUtil.hasValue(dbmsDefaultSchemaName) && dbmsDefaultSchemaName.equals(ti._tabSchema))
			addSchema = false;
		
		String[] dbmsSkipResolvSchemaName = provider.getDbSkipResolvSchemaName();
		for (String schName : dbmsSkipResolvSchemaName)
		{
			if (StringUtil.hasValue(schName) && schName.equals(ti._tabSchema))
				addSchema = false;
		}

		String out = "";
		if (addCatalog) out += catalogName + ".";
		if (addSchema)  out += schemaName  + ".";
		out += tableName;
		
		return out;
	}

	//-------------------------
	// CONSTRUCTOR(S)
	//-------------------------
	public SqlTableCompletion(CompletionProviderAbstractSql provider, TableInfo ti, boolean addCatalog, boolean addSchema, boolean quoteNames)
	{
		super(provider, ti._tabName, createReplacementText(provider, ti, addCatalog, addSchema, quoteNames));
		_tableInfo = ti;

		String shortDesc = 
			"<font color='blue'>"+ti._tabType+"</font>" +
//			" -- <i><font color='green'>" + (StringUtil.isNullOrBlank(ti._tabRemark) ? "No Description" : ti._tabRemark) + "</font></i>";
			" -- <i><font color='green'>" + (StringUtil.isNullOrBlank(ti._tabRemark) ? "" : stripMultiLineHtml(ti._tabRemark)) + "</font></i>";
		setShortDescription(shortDesc);
//		setSummary(_tableInfo.toHtmlString());
	}
	


	//-------------------------
	// OVERIDE METHODS
	//-------------------------
	
	public String getType()
	{
		if (_tableInfo          == null) return "";
		if (_tableInfo._tabType == null) return "";
		return _tableInfo._tabType;
	}
	public String getName()
	{
		if (_tableInfo          == null) return "";
		if (_tableInfo._tabName == null) return "";
		return _tableInfo._tabName;
	}

	@Override
	public String getSummary()
	{
		if ( ! _tableInfo.isColumnRefreshed() )
		{
			CompletionProvider cp = getProvider();
//			if (cp instanceof CompletionProviderAbstract)
//				_tableInfo.refreshColumnInfo(((CompletionProviderAbstract)cp).getConnectionProvider());
			if (cp instanceof CompletionProviderAbstractSql)
				_tableInfo.refreshColumnInfo(((CompletionProviderAbstractSql)cp).getConnection());
		}
		return _tableInfo.toHtmlString();
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
