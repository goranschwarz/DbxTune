/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
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
package com.asetune.ui.autocomplete.completions;

import org.fife.ui.autocomplete.CompletionProvider;

import com.asetune.ui.autocomplete.CompletionProviderAbstractSql;
import com.asetune.utils.StringUtil;

/**
 * Our own Completion class, which overrides toString() to make it HTML aware
 */
public class SqlColumnCompletion
extends SqlCompletion
{
	private static final long serialVersionUID = 1L;

	private TableInfo _tableInfo = null;

	//-------------------------
	// helper called from constructor
	//-------------------------
	public static String createReplacementText(CompletionProviderAbstractSql provider, String tabAliasName, String colname, TableInfo tableInfo, boolean quoteNames)
	{
		String q = provider.getDbIdentifierQuoteString();

		String finalColName = quoteNames ? q+colname+q : provider.fixStrangeNames(colname);
		
		if (StringUtil.isNullOrBlank(tabAliasName))
			return finalColName;
		else
			return tabAliasName + "." + finalColName;
	}

	
	//-------------------------
	// CONSTRUCTOR(S)
	//-------------------------
	public SqlColumnCompletion(CompletionProviderAbstractSql provider, String tabAliasName, String colname, TableInfo tableInfo, boolean quoteNames)
	{
//		super(provider, provider.fixStrangeNames(colname), (tabAliasName == null ? colname : tabAliasName+"."+colname));
		super(provider, provider.fixStrangeNames(colname), createReplacementText(provider, tabAliasName, colname, tableInfo, quoteNames));
		_tableInfo = tableInfo;

		TableColumnInfo ci = _tableInfo.getColumnInfo(colname);
		String colPos = "";
		if (ci != null)
			colPos = "pos="+ci._colPos+", ";

		String shortDesc = 
			"<font color='blue'>"+_tableInfo.getColDdlDesc(colname)+"</font>" +
			" -- <i><font color='green'>" + colPos + _tableInfo.getColDescription(colname) + "</font></i>";
		setShortDescription(shortDesc);
		//setSummary(_tableInfo.toHtmlString(colname));
	}


	//-------------------------
	// OVERIDE METHODS
	//-------------------------
	
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
