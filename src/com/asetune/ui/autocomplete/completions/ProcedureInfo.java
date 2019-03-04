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

import java.io.Serializable;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.ui.autocomplete.CompletionProviderAbstractSql;
import com.asetune.utils.ConnectionProvider;
import com.asetune.utils.StringUtil;

/**
 * Holds information about procedures
 */
public class ProcedureInfo
implements Serializable
{
	private static Logger _logger = Logger.getLogger(ProcedureInfo.class);
	private static final long serialVersionUID = 1L;

	public String _procCat     = null;
	public String _procSchema  = null;
	public String _procName    = null;
	public String _procType    = null;
	public String _procRemark  = null;
	public String _procSpecificName = null;
	
	public String  _oraPackageName   = null; // ONLY if oracle -- needs special lookup...
	
	public boolean _needParamsRefresh = true;
	public boolean isParamsRefreshed() {return ! _needParamsRefresh;}

	public ArrayList<ProcedureParameterInfo> _parameters = new ArrayList<ProcedureParameterInfo>();

	public void addParameter(ProcedureParameterInfo ci)
	{
		_parameters.add(ci);
	}

	public void refreshParameterInfo(ConnectionProvider connProvider)
	{
		try
		{
			final Connection conn = connProvider.getConnection();
			if (conn == null)
				return;

			DatabaseMetaData dbmd = conn.getMetaData();

			int colId = 0;
			ResultSet rs = dbmd.getProcedureColumns(_procCat, _procSchema, _procName, "%");

			MonTablesDictionary mtd = MonTablesDictionaryManager.hasInstance() ? MonTablesDictionaryManager.getInstance() : null;
			while(rs.next())
			{
				colId++;

				ProcedureParameterInfo ppi = new ProcedureParameterInfo();
				ppi._paramName       = rs.getString("COLUMN_NAME");
				ppi._paramPos        = colId;
				ppi._paramInOutType  = CompletionProviderAbstractSql.procInOutDecode(rs.getShort("COLUMN_TYPE")); // IN - OUT - INOUT
				ppi._paramType       = rs.getString("TYPE_NAME");
				ppi._paramLength     = rs.getInt   ("LENGTH");
				ppi._paramIsNullable = rs.getInt   ("NULLABLE");
				ppi._paramRemark     = rs.getString("REMARKS");
				ppi._paramDefault    = rs.getString("COLUMN_DEF");
				ppi._paramScale      = rs.getInt   ("SCALE");
				
				// Check with the MonTable dictionary for Descriptions
				if (mtd != null && StringUtil.isNullOrBlank(ppi._paramRemark))
					ppi._paramRemark = StringUtil.stripHtmlStartEnd(mtd.getDescription(_procName, ppi._paramName));

				addParameter(ppi);
			}
			rs.close();

			_needParamsRefresh = false;
		}
		catch (SQLException e)
		{
			_logger.warn("Problems looking up Parmeter MetaData for procedure '"+_procName+"'. Caught: "+e);
		}
	}

	@Override
	public String toString()
	{
		return super.toString() + ": cat='"+_procCat+"', schema='"+_procSchema+"', name='"+_procName+"', type='"+_procType+"', remark='"+_procRemark+"'";
	}

	public String toHtmlString()
	{
		StringBuilder sb = new StringBuilder();
//		sb.append(_tabType).append(" - <B>").append(_tabName).append("</B>");
		sb.append(_procSchema).append(".<B>").append(_procName).append("</B> - <font color='blue'>").append(_oraPackageName==null?"":"PACKAGE ").append(_procType).append("</font>");
		sb.append("<HR>");
		sb.append("<BR>");
		sb.append("<B>Description:</B> ").append(StringUtil.isNullOrBlank(_procRemark) ? "not available" : _procRemark).append("<BR>");
		sb.append("<BR>");
		sb.append("<B>Columns:</B> ").append("<BR>");
		sb.append("<TABLE ALIGN='left' BORDER=0 CELLSPACING=0 CELLPADDING=1>");
		sb.append("<TR ALIGN='left' VALIGN='top' BGCOLOR='#ffffff'>");
		sb.append(" <TD NOWRAP BGCOLOR='#cccccc'><FONT COLOR='#000000'><b>").append("Name")       .append("</B></FONT></TD>");
		sb.append(" <TD NOWRAP BGCOLOR='#cccccc'><FONT COLOR='#000000'><b>").append("ParamType")  .append("</B></FONT></TD>");
		sb.append(" <TD NOWRAP BGCOLOR='#cccccc'><FONT COLOR='#000000'><b>").append("Datatype")   .append("</B></FONT></TD>");
		sb.append(" <TD NOWRAP BGCOLOR='#cccccc'><FONT COLOR='#000000'><b>").append("Length")     .append("</B></FONT></TD>");
		sb.append(" <TD NOWRAP BGCOLOR='#cccccc'><FONT COLOR='#000000'><b>").append("Nulls")      .append("</B></FONT></TD>");
		sb.append(" <TD NOWRAP BGCOLOR='#cccccc'><FONT COLOR='#000000'><b>").append("Pos")        .append("</B></FONT></TD>");
		sb.append(" <TD NOWRAP BGCOLOR='#cccccc'><FONT COLOR='#000000'><b>").append("Description").append("</B></FONT></TD>");
		sb.append("</TR>");
		int r=0;
		for (ProcedureParameterInfo pi : _parameters)
		{
			r++;
			if ( (r % 2) == 0 )
				sb.append("<TR ALIGN='left' VALIGN='top' BGCOLOR='#ffffff'>");
			else
				sb.append("<TR ALIGN='left' VALIGN='top' BGCOLOR='#ffffcc'>");
			sb.append("	<TD NOWRAP>").append(pi._paramName)      .append("</TD>");
			sb.append("	<TD NOWRAP>").append(pi._paramInOutType) .append("</TD>");
			sb.append("	<TD NOWRAP>").append(pi._paramType)      .append("</TD>");
			sb.append("	<TD NOWRAP>").append(pi._paramLength)    .append("</TD>");
			sb.append("	<TD NOWRAP>").append(pi._paramIsNullable).append("</TD>");
			sb.append("	<TD NOWRAP>").append(pi._paramPos)       .append("</TD>");
			sb.append("	<TD NOWRAP>").append(pi._paramRemark != null ? pi._paramRemark : "not available").append("</TD>");
			sb.append("</TR>");
		}
		sb.append("</TABLE>");
		sb.append("<HR>");
		sb.append("-end-<BR><BR>");
		
		return sb.toString();
	}

	public String toHtmlString(String paramName)
	{
		ProcedureParameterInfo pi = null;
		for (ProcedureParameterInfo e : _parameters)
		{
			if (paramName.equalsIgnoreCase(e._paramName))
			{
				pi = e;
				break;
			}
		}
		if (pi == null)
			return "Parameter name '"+paramName+"', was not found in procedure '"+_procName+"'.";

		StringBuilder sb = new StringBuilder();
		sb.append(_procSchema).append(".<B>").append(_procName).append(".").append(pi._paramName).append("</B> - <font color='blue'>").append(_procType).append(" - COLUMN").append("</font>");
		sb.append("<HR>"); // add Horizontal Ruler: ------------------
		sb.append("<BR>");
		sb.append("<B>Procedure Description:</B> ").append(StringUtil.isNullOrBlank(_procRemark) ? "not available" : _procRemark).append("<BR>");
		sb.append("<BR>");
		sb.append("<B>Column Description:</B> ").append(StringUtil.isNullOrBlank(pi._paramRemark) ? "not available" : pi._paramRemark).append("<BR>");
		sb.append("<BR>");
		sb.append("<B>Name:</B> ")       .append(pi._paramName)      .append("<BR>");
		sb.append("<B>In/Out:</B> ")     .append(pi._paramInOutType) .append("<BR>");
		sb.append("<B>Type:</B> ")       .append(pi._paramType)      .append("<BR>");
		sb.append("<B>Length:</B> ")     .append(pi._paramLength)    .append("<BR>");
		sb.append("<B>Is Nullable:</B> ").append(pi._paramIsNullable).append("<BR>");
		sb.append("<B>Pos:</B> ")        .append(pi._paramPos)       .append("<BR>");
		sb.append("<B>Default:</B> ")    .append(pi._paramDefault)   .append("<BR>");
		sb.append("<HR>");
		sb.append("-end-<BR><BR>");
		
		return sb.toString();
	}

	public ProcedureParameterInfo getParameterInfo(String colname)
	{
		ProcedureParameterInfo ci = null;
		for (ProcedureParameterInfo e : _parameters)
		{
			if (colname.equalsIgnoreCase(e._paramName))
			{
				ci = e;
				break;
			}
		}
		return ci;
	}

	public String getParamDdlDesc(String paramName)
	{
		ProcedureParameterInfo pi = getParameterInfo(paramName);
		if (pi == null)
			return "Parameter name '"+paramName+"', was not found in procedure '"+_procName+"'.";

		String nulls    = pi._paramIsNullable == DatabaseMetaData.columnNoNulls ? "<b>NOT</b> NULL" : "    NULL";
		String datatype = pi._paramType;

		// Compose data type
		String dtlower = datatype.toLowerCase();
		if ( dtlower.equals("char") || dtlower.equals("varchar") )
			datatype = datatype + "(" + pi._paramLength + ")";
		
		if ( dtlower.equals("numeric") || dtlower.equals("decimal") )
			datatype = datatype + "(" + pi._paramLength + "," + pi._paramScale + ")";

		return datatype + " " + nulls;
	}

	public String getColDescription(String paramName)
	{
		ProcedureParameterInfo pi = getParameterInfo(paramName);
		if (pi == null)
			return "Column name '"+paramName+"', was not found in table '"+_procName+"'.";

		if (StringUtil.isNullOrBlank(pi._paramRemark))
//			return "No Description";
			return "";
		return pi._paramRemark;
	}
}
