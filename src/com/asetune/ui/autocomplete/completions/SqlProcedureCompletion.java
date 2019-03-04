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

import com.asetune.ui.autocomplete.CompletionProviderAbstract;
import com.asetune.ui.autocomplete.CompletionProviderAbstractSql;
import com.asetune.utils.StringUtil;

/**
 * Our own Completion class, which overrides toString() to make it HTML aware
 */
public class SqlProcedureCompletion
extends SqlCompletion
{
	private static final long serialVersionUID = 1L;

	public ProcedureInfo _procInfo = null;
	
	//-------------------------
	// helper called from constructor
	//-------------------------
	public static String createReplacementText(CompletionProviderAbstractSql provider, ProcedureInfo pi, boolean addCatalog, String catName, boolean addSchema, boolean quoteNames)
	{
		String tmpCatalogName = pi._procCat;
		if (catName != null)
		{
			tmpCatalogName = catName;
			addCatalog = true;
		}
			
		String q = provider.getDbIdentifierQuoteString();

		String catalogName = quoteNames ? q+tmpCatalogName+q : provider.fixStrangeNames(tmpCatalogName);
		String schemaName  = quoteNames ? q+pi._procSchema+q : provider.fixStrangeNames(pi._procSchema);
		String tableName   = quoteNames ? q+pi._procName+q   : provider.fixStrangeNames(pi._procName);
		
		if (pi._oraPackageName != null)
		{
			String pkgName = quoteNames ? q+pi._oraPackageName+q : provider.fixStrangeNames(pi._oraPackageName);
			tableName = tableName + "." + pkgName;
		}
			

		// If the schemaname/owner is 'dbo', do not prefix it with 'dbo.'
//		if ("dbo".equalsIgnoreCase(pi._procSchema))
//		{
//			schemaName = "";
//			addSchema = addCatalog; // if catalog is true, we need to add a simple '.'
//		}

		String out = "";
		if (addCatalog) out += catalogName + ".";
		if (addSchema)  out += schemaName  + ".";
		out += tableName;
		
		return out;
	}

	//-------------------------
	// CONSTRUCTOR(S)
	//-------------------------
//	public SqlProcedureCompletion(CompletionProviderAbstractSql provider, ProcedureInfo pi)
	public SqlProcedureCompletion(CompletionProviderAbstractSql provider, ProcedureInfo pi, boolean addCatalog, String catalogName, boolean addSchema, boolean quoteNames)
	{
		super(provider, pi._procName, createReplacementText(provider, pi, addCatalog, catalogName, addSchema, quoteNames));
		_procInfo = pi;

		String shortDesc = 
			"<font color='blue'>"+(pi._oraPackageName==null?"":"PACKAGE ")+pi._procType+"</font>" +
			(StringUtil.isNullOrBlank(pi._procSpecificName) ? "" : ", SpecificName="+pi._procSpecificName) +
//			" -- <i><font color='green'>" + (StringUtil.isNullOrBlank(pi._procRemark) ? "No Description" : pi._procRemark) + "</font></i>";
			" -- <i><font color='green'>" + (StringUtil.isNullOrBlank(pi._procRemark) ? "" : stripMultiLineHtml(pi._procRemark)) + "</font></i>";
		setShortDescription(shortDesc);
		//setSummary(_procInfo.toHtmlString());
	}

	public String getType()
	{
		if (_procInfo           == null) return "";
		if (_procInfo._procType == null) return "";
		return _procInfo._procType;
	}
	public String getName()
	{
		if (_procInfo           == null) return "";
		if (_procInfo._procName == null) return "";
		return _procInfo._procName;
	}
	public String getRemark()
	{
		if (_procInfo             == null) return "";
		if (_procInfo._procRemark == null) return "";
		return _procInfo._procRemark;
	}

	@Override
	public String getSummary()
	{
		if ( ! _procInfo.isParamsRefreshed() )
		{
			CompletionProvider cp = getProvider();
			if (cp instanceof CompletionProviderAbstract)
				_procInfo.refreshParameterInfo(((CompletionProviderAbstract)cp).getConnectionProvider());
		}
		return _procInfo.toHtmlString();
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
