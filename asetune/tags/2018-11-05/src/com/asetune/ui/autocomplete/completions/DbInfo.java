package com.asetune.ui.autocomplete.completions;

import java.io.Serializable;

import com.asetune.utils.StringUtil;

/**
 * Holds information about databases
 */
public class DbInfo
implements Serializable
{
	private static final long serialVersionUID = 1L;

	public String _dbName    = null;
	public String _dbSize    = null;
	public int    _dbId      = -1;
	public String _dbOwner   = null;
	public String _dbCrDate  = null;
	public String _dbType    = null;
	public String _dbRemark  = null;
	
	@Override
	public String toString()
	{
		return super.toString() + ": name='"+_dbName+"', size='"+_dbSize+"', id='"+_dbId+"', owner='"+_dbOwner+"', crdate='"+_dbCrDate+"', type='"+_dbType+"', remark='"+_dbRemark+"'";
	}

	public String toHtmlString()
	{
		StringBuilder sb = new StringBuilder();
//		sb.append(_tabType).append(" - <B>").append(_tabName).append("</B>");
		sb.append("<B>").append(_dbName).append("</B> - <font color=\"blue\">").append(_dbType).append("</font>");
		sb.append("<HR>");
		sb.append("<BR>");
		sb.append("<B>Description:</B> ").append(StringUtil.isNullOrBlank(_dbRemark) ? "not available" : _dbRemark).append("<BR>");
		sb.append("<B>Size:</B> ")       .append(StringUtil.isNullOrBlank(_dbSize)   ? "not available" : _dbSize)  .append("<BR>");
		sb.append("<B>Owner:</B> ")      .append(StringUtil.isNullOrBlank(_dbOwner)  ? "not available" : _dbOwner) .append("<BR>");
		sb.append("<B>Create Date:</B> ").append(StringUtil.isNullOrBlank(_dbCrDate) ? "not available" : _dbCrDate).append("<BR>");
		sb.append("<B>dbid:</B> ")       .append(_dbId == -1                         ? "not available" : _dbId)    .append("<BR>");
		sb.append("<BR>");
		
		return sb.toString();
	}
}
