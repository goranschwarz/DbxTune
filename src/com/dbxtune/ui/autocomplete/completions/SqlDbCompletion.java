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
public class SqlDbCompletion
extends SqlCompletion
{
	private static final long serialVersionUID = 1L;

	public DbInfo _dbInfo = null;
	
	//-------------------------
	// helper called from constructor
	//-------------------------
	public static String createReplacementText(CompletionProviderAbstractSql provider, DbInfo di)
	{
		return provider.fixStrangeNames(di._dbName);
	}

	//-------------------------
	// CONSTRUCTOR(S)
	//-------------------------
	public SqlDbCompletion(CompletionProviderAbstractSql provider, DbInfo di)
	{
		super(provider, di._dbName, createReplacementText(provider, di));

		_dbInfo = di;

		String shortDesc = 
			"<font color='blue'>"+di._dbType+"</font>" +
//			" -- <i><font color='green'>" + (StringUtil.isNullOrBlank(di._dbRemark) ? "No Description" : di._dbRemark) + "</font></i>";
			" -- <i><font color='green'>" + (StringUtil.isNullOrBlank(di._dbRemark) ? "" : stripMultiLineHtml(di._dbRemark)) + "</font></i>";
		setShortDescription(shortDesc);
//		setSummary(_dbInfo.toHtmlString());
	}


	//-------------------------
	// OVERIDE METHODS
	//-------------------------
	
	@Override
	public String getSummary()
	{
		return _dbInfo.toHtmlString();
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
