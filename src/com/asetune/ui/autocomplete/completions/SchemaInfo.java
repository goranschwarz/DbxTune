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
		sb.append("<B>").append(_name).append("</B> - <font color='blue'>").append(_cat).append("</font>");
		sb.append("<HR>");
		sb.append("<BR>");
		sb.append("<B>Description:</B> ").append(StringUtil.isNullOrBlank(_remark) ? "not available" : _remark).append("<BR>");
		sb.append("<BR>");
		
		return sb.toString();
	}
}
