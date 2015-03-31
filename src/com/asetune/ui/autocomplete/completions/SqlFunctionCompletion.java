package com.asetune.ui.autocomplete.completions;

import org.fife.ui.autocomplete.CompletionProvider;

import com.asetune.ui.autocomplete.CompletionProviderAbstract;
import com.asetune.ui.autocomplete.CompletionProviderAbstractSql;
import com.asetune.utils.StringUtil;

/**
 * Our own Completion class, which overrides toString() to make it HTML aware
 */
public class SqlFunctionCompletion
extends SqlCompletion
{
	private static final long serialVersionUID = 1L;

	public FunctionInfo _functionInfo = null;

	public static String createReplacementText(CompletionProviderAbstractSql provider, FunctionInfo ti, boolean addCatalog, boolean addSchema, boolean quoteNames)
	{
		String q = provider.getDbIdentifierQuoteString();
		String catalogName = quoteNames ? q+ti._funcCat+q    : provider.fixStrangeNames(ti._funcCat);
		String schemaName  = quoteNames ? q+ti._funcSchema+q : provider.fixStrangeNames(ti._funcSchema);
		String tableName   = quoteNames ? q+ti._funcName+q   : provider.fixStrangeNames(ti._funcName);

		// If the schemaname/owner is 'dbo', do not prefix it with 'dbo.'
//		if ("dbo".equalsIgnoreCase(ti._funcSchema))
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
	public SqlFunctionCompletion(CompletionProviderAbstractSql provider, FunctionInfo fi, boolean addCatalog, boolean addSchema, boolean quoteNames)
	{
		super(provider, fi._funcName, createReplacementText(provider, fi, addCatalog, addSchema, quoteNames));
		_functionInfo = fi;

		String shortDesc = 
			"<font color=\"blue\">"+fi._funcType+"</font>" +
//			" -- <i><font color=\"green\">" + (StringUtil.isNullOrBlank(ti._funcRemark) ? "No Description" : ti._funcRemark) + "</font></i>";
			" -- <i><font color=\"green\">" + (StringUtil.isNullOrBlank(fi._funcRemark) ? "" : fi._funcRemark) + "</font></i>";
		setShortDescription(shortDesc);
//		setSummary(_functionInfo.toHtmlString());
	}
	
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
