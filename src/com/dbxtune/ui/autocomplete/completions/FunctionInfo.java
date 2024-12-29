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
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.dbxtune.config.dict.MonTablesDictionary;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.utils.ConnectionProvider;
import com.dbxtune.utils.StringUtil;

/**
 * Holds information about tables
 */
public class FunctionInfo
implements Serializable
{
	private static Logger _logger = Logger.getLogger(FunctionInfo.class);
	private static final long serialVersionUID = 1L;

	public String _funcCat      = null;
	public String _funcSchema   = null;
	public String _funcName     = null;
	public String _funcType     = null;
	public String _funcRemark   = null;
	public String _specificName = null;
	public int    _funcTypeInt  = -999;
	public boolean _isTableValuedFunction = true;
	
	public boolean _needColumnRefresh = true;
	public boolean isColumnRefreshed() {return ! _needColumnRefresh;}

	public ArrayList<FunctionColumnInfo> _columns = new ArrayList<FunctionColumnInfo>();

	public void addColumn(FunctionColumnInfo ci)
	{
		// If column name already exists, do NOT add it again
		for (FunctionColumnInfo existingCi : _columns)
		{
if (existingCi._colName == null || ci._colName == null)
{
	System.out.println("existingCi=|"+existingCi+"|.");
	System.out.println("        ci=|"+ci+"|.");
	System.out.println("");
}
			if (existingCi._colName.equals(ci._colName))
			{
				//(new Exception("callstack for: addColumn("+ci._colName+") already exists.")).printStackTrace();
				return;
			}
		}

		_columns.add(ci);
	}

	public void refreshColumnInfo(ConnectionProvider connProvider)
	{
		try
		{
			final Connection conn = connProvider.getConnection();
			if (conn == null)
				return;

			DatabaseMetaData dbmd = conn.getMetaData();

			ResultSet rs = dbmd.getFunctionColumns(_funcCat, _funcSchema, _funcName, "%");

			MonTablesDictionary mtd = MonTablesDictionaryManager.hasInstance() ? MonTablesDictionaryManager.getInstance() : null;
			while(rs.next())
			{
				FunctionColumnInfo ci = new FunctionColumnInfo();
				try {
					ci._colName   = rs.getString("COLUMN_NAME");
				} catch (SQLException ignore) {
					ci._colName   = rs.getString("PARAMETER_NAME");   // DB2 has PARAMETER_NAME instead of COLUMN_NAME
				}
				ci._colPos        = rs.getInt   ("ORDINAL_POSITION");
				ci._colType       = rs.getString("TYPE_NAME");
				ci._colLength     = rs.getInt   ("LENGTH");
				ci._colIsNullable = rs.getInt   ("NULLABLE");
				ci._colRemark     = rs.getString("REMARKS");
//				ci._colDefault    = rs.getString("COLUMN_DEF");
//				ci._colScale      = rs.getInt   ("SCALE"); // SCALE short => scale - null is returned for data types where SCALE is not applicable.
				
				// Check with the MonTable dictionary for Descriptions
				if (mtd != null && StringUtil.isNullOrBlank(ci._colRemark))
					ci._colRemark = StringUtil.stripHtmlStartEnd(mtd.getDescription(_funcName, ci._colName));

				addColumn(ci);
			}
			rs.close();

			_needColumnRefresh = false;
		}
		catch (SQLException e)
		{
			_logger.warn("Problems looking up Column MetaData for cat='"+_funcCat+"', schema='"+_funcSchema+"', function='"+_funcName+"'. using 'dbmd.getFunctionColumns()' Caught: "+e);
		}
	}

	@Override
	public String toString()
	{
		return super.toString() + ": cat='"+_funcCat+"', schema='"+_funcSchema+"', name='"+_funcName+"', type='"+_funcType+"', remark='"+_funcRemark+"'";
	}
	public String toHtmlString()
	{
		StringBuilder sb = new StringBuilder();
//		sb.append(_funcType).append(" - <B>").append(_funcName).append("</B>");
		sb.append(_funcSchema).append(".<B>").append(_funcName).append("</B> - <font color='blue'>").append(_funcType).append("</font>");
		sb.append("<HR>");
		sb.append("<BR>");
		sb.append("<B>Description:</B> ").append(StringUtil.isNullOrBlank(_funcRemark) ? "not available" : _funcRemark).append("<BR>");
		sb.append("<BR>");
		sb.append("<B>Columns:</B> ").append("<BR>");
		sb.append("<TABLE ALIGN='left' BORDER=0 CELLSPACING=0 CELLPADDING=1>");
		sb.append("<TR ALIGN='left' VALIGN='top' BGCOLOR='#ffffff'>");
		sb.append(" <TD NOWRAP BGCOLOR='#cccccc'><FONT COLOR='#000000'><b>").append("Name")       .append("</B></FONT></TD>");
		sb.append(" <TD NOWRAP BGCOLOR='#cccccc'><FONT COLOR='#000000'><b>").append("Datatype")   .append("</B></FONT></TD>");
		sb.append(" <TD NOWRAP BGCOLOR='#cccccc'><FONT COLOR='#000000'><b>").append("Length")     .append("</B></FONT></TD>");
		sb.append(" <TD NOWRAP BGCOLOR='#cccccc'><FONT COLOR='#000000'><b>").append("Nulls")      .append("</B></FONT></TD>");
		sb.append(" <TD NOWRAP BGCOLOR='#cccccc'><FONT COLOR='#000000'><b>").append("Pos")        .append("</B></FONT></TD>");
		sb.append(" <TD NOWRAP BGCOLOR='#cccccc'><FONT COLOR='#000000'><b>").append("Description").append("</B></FONT></TD>");
		sb.append("</TR>");
		int r=0;
		for (FunctionColumnInfo ci : _columns)
		{
			r++;
			if ( (r % 2) == 0 )
				sb.append("<TR ALIGN='left' VALIGN='top' BGCOLOR='#ffffff'>");
			else
				sb.append("<TR ALIGN='left' VALIGN='top' BGCOLOR='#ffffcc'>");
			sb.append("	<TD NOWRAP>").append(ci._colName)      .append("</TD>");
			sb.append("	<TD NOWRAP>").append(ci._colType)      .append("</TD>");
			sb.append("	<TD NOWRAP>").append(ci._colLength)    .append("</TD>");
			sb.append("	<TD NOWRAP>").append(ci._colIsNullable).append("</TD>");
			sb.append("	<TD NOWRAP>").append(ci._colPos)       .append("</TD>");
			sb.append("	<TD NOWRAP>").append(ci._colRemark != null ? ci._colRemark : "not available").append("</TD>");
			sb.append("</TR>");
		}
		sb.append("</TABLE>");
		sb.append("<HR>");
		sb.append("-end-<BR><BR>");
		
		return sb.toString();
	}

	public String toHtmlString(String colname)
	{
		FunctionColumnInfo ci = null;
		for (FunctionColumnInfo e : _columns)
		{
			if (colname.equalsIgnoreCase(e._colName))
			{
				ci = e;
				break;
			}
		}
		if (ci == null)
			return "Column name '"+colname+"', was not found in table '"+_funcName+"'.";

		StringBuilder sb = new StringBuilder();
		sb.append(_funcSchema).append(".<B>").append(_funcName).append(".").append(ci._colName).append("</B> - <font color='blue'>").append(_funcType).append(" - COLUMN").append("</font>");
		sb.append("<HR>"); // add Horizontal Ruler: ------------------
		sb.append("<BR>");
		sb.append("<B>Function Description:</B> ").append(StringUtil.isNullOrBlank(_funcRemark) ? "not available" : _funcRemark).append("<BR>");
		sb.append("<BR>");
		sb.append("<B>Column Description:</B> ").append(StringUtil.isNullOrBlank(ci._colRemark) ? "not available" : ci._colRemark).append("<BR>");
		sb.append("<BR>");
		sb.append("<B>Name:</B> ")       .append(ci._colName)      .append("<BR>");
		sb.append("<B>Type:</B> ")       .append(ci._colType)      .append("<BR>");
		sb.append("<B>Length:</B> ")     .append(ci._colLength)    .append("<BR>");
		sb.append("<B>Is Nullable:</B> ").append(ci._colIsNullable).append("<BR>");
		sb.append("<B>Pos:</B> ")        .append(ci._colPos)       .append("<BR>");
		sb.append("<B>Default:</B> ")    .append(ci._colDefault)   .append("<BR>");
		sb.append("<HR>");
		sb.append("-end-<BR><BR>");
		
		return sb.toString();
	}

	public FunctionColumnInfo getColumnInfo(String colname)
	{
		FunctionColumnInfo ci = null;
		for (FunctionColumnInfo e : _columns)
		{
			if (colname.equalsIgnoreCase(e._colName))
			{
				ci = e;
				break;
			}
		}
		return ci;
	}

	public String getColDdlDesc(String colname)
	{
		FunctionColumnInfo ci = getColumnInfo(colname);
		if (ci == null)
			return "Column name '"+colname+"', was not found in table '"+_funcName+"'.";

		String nulls    = ci._colIsNullable == DatabaseMetaData.columnNoNulls ? "<b>NOT</b> NULL" : "    NULL";
		String datatype = ci._colType;

		// Compose data type
		String dtlower = datatype.toLowerCase();
		if ( dtlower.equals("char") || dtlower.equals("varchar") )
			datatype = datatype + "(" + ci._colLength + ")";
		
		if ( dtlower.equals("numeric") || dtlower.equals("decimal") )
			datatype = datatype + "(" + ci._colLength + "," + ci._colScale + ")";

		return datatype + " " + nulls;
	}

	public String getColDescription(String colname)
	{
		FunctionColumnInfo ci = getColumnInfo(colname);
		if (ci == null)
			return "Column name '"+colname+"', was not found in table '"+_funcName+"'.";

		if (StringUtil.isNullOrBlank(ci._colRemark))
//			return "No Description";
			return "";
		return ci._colRemark;
	}
}
