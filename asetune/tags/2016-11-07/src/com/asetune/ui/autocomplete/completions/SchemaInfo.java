package com.asetune.ui.autocomplete.completions;

import java.io.Serializable;

import com.asetune.utils.StringUtil;

/**
 * Holds information about databases
 */
public class SchemaInfo
implements Serializable
{
	private static final long serialVersionUID = 1L;

	public String _cat  = null;
	public String _name = null;
	public String _remark = null;

	@Override
	public String toString()
	{
		return super.toString() + ": name='"+_name+"', catalog='"+_cat+"'";
	}

	public String toHtmlString()
	{
		StringBuilder sb = new StringBuilder();
//		sb.append(_tabType).append(" - <B>").append(_tabName).append("</B>");
		sb.append("<B>").append(_name).append("</B> - <font color=\"blue\">").append(_cat).append("</font>");
		sb.append("<HR>");
		sb.append("<BR>");
		sb.append("<B>Description:</B> ").append(StringUtil.isNullOrBlank(_remark) ? "not available" : _remark).append("<BR>");
		sb.append("<BR>");
		
		return sb.toString();
	}
}
