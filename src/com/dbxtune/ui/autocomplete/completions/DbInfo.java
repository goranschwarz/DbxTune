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

import java.io.Serializable;

import com.dbxtune.utils.StringUtil;

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
		sb.append("<B>").append(_dbName).append("</B> - <font color='blue'>").append(_dbType).append("</font>");
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
