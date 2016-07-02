package com.asetune.ui.autocomplete.completions;

import com.asetune.ui.autocomplete.CompletionProviderAbstractSql;
import com.asetune.utils.StringUtil;

/**
 * Our own Completion class, which overrides toString() to make it HTML aware
 */
public class SqlDbCompletion
extends SqlCompletion
{
	private static final long serialVersionUID = 1L;

	public DbInfo _dbInfo = null;
	
	public SqlDbCompletion(CompletionProviderAbstractSql provider, DbInfo di)
	{
		super(provider, di._dbName, provider.fixStrangeNames(di._dbName));

		_dbInfo = di;

		String shortDesc = 
			"<font color=\"blue\">"+di._dbType+"</font>" +
//			" -- <i><font color=\"green\">" + (StringUtil.isNullOrBlank(di._dbRemark) ? "No Description" : di._dbRemark) + "</font></i>";
			" -- <i><font color=\"green\">" + (StringUtil.isNullOrBlank(di._dbRemark) ? "" : stripMultiLineHtml(di._dbRemark)) + "</font></i>";
		setShortDescription(shortDesc);
//		setSummary(_dbInfo.toHtmlString());
	}

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
