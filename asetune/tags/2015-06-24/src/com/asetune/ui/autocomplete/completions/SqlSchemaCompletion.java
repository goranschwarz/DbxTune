package com.asetune.ui.autocomplete.completions;

import com.asetune.ui.autocomplete.CompletionProviderAbstractSql;
import com.asetune.utils.StringUtil;

/**
 * Our own Completion class, which overrides toString() to make it HTML aware
 */
public class SqlSchemaCompletion
extends SqlCompletion
{
	private static final long serialVersionUID = 1L;

	public SchemaInfo _schemaInfo = null;
	
	public SqlSchemaCompletion(CompletionProviderAbstractSql provider, String schemaName)
	{
		super(provider, schemaName, provider.fixStrangeNames(schemaName)+".");

		String shortDesc = 
			"<font color=\"blue\">"+schemaName+"</font>" +
			" -- <i><font color=\"green\">SCHEMA</font></i>";
		setShortDescription(shortDesc);
	}

	public static String createReplacementText(CompletionProviderAbstractSql provider, SchemaInfo si, String catName, boolean quoteNames)
	{
		String q = provider.getDbIdentifierQuoteString();
		String catalogName = quoteNames ? q+si._cat+q  : provider.fixStrangeNames(si._cat);
		String schemaName  = quoteNames ? q+si._name+q : provider.fixStrangeNames(si._name);

		String out = "";
		out += ((catName == null) ? catalogName : catName) + ".";
		out += schemaName;
		
		return out;
	}
	public SqlSchemaCompletion(CompletionProviderAbstractSql provider, SchemaInfo si, String catName, boolean quoteNames)
	{
		super(provider, si._name, createReplacementText(provider, si, catName, quoteNames));

		_schemaInfo = si;

		String shortDesc = 
			"<font color=\"blue\">"+si._name+"</font>" +
			" -- <i><font color=\"green\">" + (StringUtil.isNullOrBlank(si._remark) ? "" : si._remark) + "</font></i>";
		setShortDescription(shortDesc);
//		setSummary(_schemaInfo.toHtmlString());
	}

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
