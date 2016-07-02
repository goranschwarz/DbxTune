package com.asetune.ui.autocomplete.completions;

import org.fife.ui.autocomplete.CompletionProvider;

import com.asetune.ui.autocomplete.CompletionProviderAbstract;
import com.asetune.ui.autocomplete.CompletionProviderAbstractSql;
import com.asetune.utils.StringUtil;

/**
 * Our own Completion class, which overrides toString() to make it HTML aware
 */
public class SqlTableCompletion
extends SqlCompletion
{
	private static final long serialVersionUID = 1L;

	public TableInfo _tableInfo = null;

	public static String createReplacementText(CompletionProviderAbstractSql provider, TableInfo ti, boolean addCatalog, boolean addSchema, boolean quoteNames)
	{
		String q = provider.getDbIdentifierQuoteString();
		String catalogName = quoteNames ? q+ti._tabCat+q    : provider.fixStrangeNames(ti._tabCat);
		String schemaName  = quoteNames ? q+ti._tabSchema+q : provider.fixStrangeNames(ti._tabSchema);
		String tableName   = quoteNames ? q+ti._tabName+q   : provider.fixStrangeNames(ti._tabName);

		// If the schemaname/owner is 'dbo', do not prefix it with 'dbo.'
//		if ("dbo".equalsIgnoreCase(ti._tabSchema))
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
	public SqlTableCompletion(CompletionProviderAbstractSql provider, TableInfo ti, boolean addCatalog, boolean addSchema, boolean quoteNames)
	{
		super(provider, ti._tabName, createReplacementText(provider, ti, addCatalog, addSchema, quoteNames));
		_tableInfo = ti;

		String shortDesc = 
			"<font color=\"blue\">"+ti._tabType+"</font>" +
//			" -- <i><font color=\"green\">" + (StringUtil.isNullOrBlank(ti._tabRemark) ? "No Description" : ti._tabRemark) + "</font></i>";
			" -- <i><font color=\"green\">" + (StringUtil.isNullOrBlank(ti._tabRemark) ? "" : stripMultiLineHtml(ti._tabRemark)) + "</font></i>";
		setShortDescription(shortDesc);
//		setSummary(_tableInfo.toHtmlString());
	}
	
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
			if (cp instanceof CompletionProviderAbstract)
				_tableInfo.refreshColumnInfo(((CompletionProviderAbstract)cp).getConnectionProvider());
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
