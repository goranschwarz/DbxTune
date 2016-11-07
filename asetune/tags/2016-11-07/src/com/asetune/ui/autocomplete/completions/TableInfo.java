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
import com.asetune.utils.ConnectionProvider;
import com.asetune.utils.StringUtil;

/**
 * Holds information about tables
 */
public class TableInfo
implements Serializable
{
	private static Logger _logger = Logger.getLogger(TableInfo.class);
	private static final long serialVersionUID = 1L;

	public String _tabCat     = null;
	public String _tabSchema  = null;
	public String _tabName    = null;
	public String _tabType    = null;
	public String _tabRemark  = null;
	
	public boolean _needColumnRefresh = true;
	public boolean isColumnRefreshed() {return ! _needColumnRefresh;}

	public ArrayList<TableColumnInfo> _columns = new ArrayList<TableColumnInfo>();

	public void addColumn(TableColumnInfo ci)
	{
		// If column name already exists, do NOT add it again
		for (TableColumnInfo existingCi : _columns)
		{
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

			ResultSet rs = dbmd.getColumns(_tabCat, _tabSchema, _tabName, "%");

			MonTablesDictionary mtd = MonTablesDictionaryManager.hasInstance() ? MonTablesDictionaryManager.getInstance() : null;
			while(rs.next())
			{
				TableColumnInfo ci = new TableColumnInfo();
				ci._colName       = rs.getString("COLUMN_NAME");
				ci._colPos        = rs.getInt   ("ORDINAL_POSITION");
				ci._colType       = rs.getString("TYPE_NAME");
				ci._colLength     = rs.getInt   ("COLUMN_SIZE");
				ci._colIsNullable = rs.getInt   ("NULLABLE");
				ci._colRemark     = rs.getString("REMARKS");
				ci._colDefault    = rs.getString("COLUMN_DEF");
				ci._colScale      = rs.getInt   ("DECIMAL_DIGITS");
				
				// Check with the MonTable dictionary for Descriptions
				if (mtd != null && StringUtil.isNullOrBlank(ci._colRemark))
					ci._colRemark = StringUtil.stripHtmlStartEnd(mtd.getDescription(_tabName, ci._colName));

				addColumn(ci);
			}
			rs.close();

			_needColumnRefresh = false;
		}
		catch (SQLException e)
		{
			_logger.warn("Problems looking up Column MetaData for table '"+_tabName+"'. Caught: "+e);
		}
	}

	@Override
	public String toString()
	{
		return super.toString() + ": cat='"+_tabCat+"', schema='"+_tabSchema+"', name='"+_tabName+"', type='"+_tabType+"', remark='"+_tabRemark+"'";
	}
	public String toHtmlString()
	{
		StringBuilder sb = new StringBuilder();
//		sb.append(_tabType).append(" - <B>").append(_tabName).append("</B>");
		sb.append(StringUtil.hasValue(_tabSchema) ? _tabSchema : _tabCat).append(".<B>").append(_tabName).append("</B> - <font color=\"blue\">").append(_tabType).append("</font>");
		sb.append("<HR>");
		sb.append("<BR>");
		sb.append("<B>Description:</B> ").append(StringUtil.isNullOrBlank(_tabRemark) ? "not available" : _tabRemark).append("<BR>");
		sb.append("<BR>");
		sb.append("<B>Columns:</B> ").append("<BR>");
		sb.append("<TABLE ALIGN=\"left\" BORDER=0 CELLSPACING=0 CELLPADDING=1\">");
		sb.append("<TR ALIGN=\"left\" VALIGN=\"top\" BGCOLOR=\"#ffffff\">");
		sb.append(" <TD NOWRAP BGCOLOR=\"#cccccc\"><FONT COLOR=\"#000000\"><b>").append("Name")       .append("</B></FONT></TD>");
		sb.append(" <TD NOWRAP BGCOLOR=\"#cccccc\"><FONT COLOR=\"#000000\"><b>").append("Datatype")   .append("</B></FONT></TD>");
		sb.append(" <TD NOWRAP BGCOLOR=\"#cccccc\"><FONT COLOR=\"#000000\"><b>").append("Length")     .append("</B></FONT></TD>");
		sb.append(" <TD NOWRAP BGCOLOR=\"#cccccc\"><FONT COLOR=\"#000000\"><b>").append("Nulls")      .append("</B></FONT></TD>");
		sb.append(" <TD NOWRAP BGCOLOR=\"#cccccc\"><FONT COLOR=\"#000000\"><b>").append("Pos")        .append("</B></FONT></TD>");
		sb.append(" <TD NOWRAP BGCOLOR=\"#cccccc\"><FONT COLOR=\"#000000\"><b>").append("Description").append("</B></FONT></TD>");
		sb.append("</TR>");
		int r=0;
		for (TableColumnInfo ci : _columns)
		{
			r++;
			if ( (r % 2) == 0 )
				sb.append("<TR ALIGN=\"left\" VALIGN=\"top\" BGCOLOR=\"#ffffff\">");
			else
				sb.append("<TR ALIGN=\"left\" VALIGN=\"top\" BGCOLOR=\"#ffffcc\">");
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
		TableColumnInfo ci = null;
		for (TableColumnInfo e : _columns)
		{
			if (colname.equalsIgnoreCase(e._colName))
			{
				ci = e;
				break;
			}
		}
		if (ci == null)
			return "Column name '"+colname+"', was not found in table '"+_tabName+"'.";

		StringBuilder sb = new StringBuilder();
		sb.append(_tabSchema).append(".<B>").append(_tabName).append(".").append(ci._colName).append("</B> - <font color=\"blue\">").append(_tabType).append(" - COLUMN").append("</font>");
		sb.append("<HR>"); // add Horizontal Ruler: ------------------
		sb.append("<BR>");
		sb.append("<B>Table Description:</B> ").append(StringUtil.isNullOrBlank(_tabRemark) ? "not available" : _tabRemark).append("<BR>");
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

	public TableColumnInfo getColumnInfo(String colname)
	{
		TableColumnInfo ci = null;
		for (TableColumnInfo e : _columns)
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
		TableColumnInfo ci = getColumnInfo(colname);
		if (ci == null)
			return "Column name '"+colname+"', was not found in table '"+_tabName+"'.";

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
		TableColumnInfo ci = getColumnInfo(colname);
		if (ci == null)
			return "Column name '"+colname+"', was not found in table '"+_tabName+"'.";

		if (StringUtil.isNullOrBlank(ci._colRemark))
//			return "No Description";
			return "";
		return ci._colRemark;
	}
}
