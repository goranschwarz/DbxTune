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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.gui.ResultSetTableModel;
import com.asetune.sql.conn.DbxConnection;
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

	public ArrayList<TableColumnInfo>  _columns = new ArrayList<TableColumnInfo>();
	public      List<String>           _pk      = null;
//	public int                         _statRowCount = -1; // This should be some of object or a Map with key/value (possibly a description)
	public Map<String, TableExtraInfo> _extraInfo = null;
	public ResultSetTableModel         _mdIndex = null;
	public ResultSetTableModel         _mdFkOut = null;
	public ResultSetTableModel         _mdFkIn  = null;
	
	public List<String> _viewReferences = null;

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

//	public void refreshColumnInfo(ConnectionProvider connProvider)
//	{
//		try
//		{
//			final DbxConnection conn = connProvider.getConnection();
//			if (conn == null)
//				return;
//
//			DatabaseMetaData dbmd = conn.getMetaData();
//
//			ResultSet rs = dbmd.getColumns(_tabCat, _tabSchema, _tabName, "%");
//
//			MonTablesDictionary mtd = MonTablesDictionaryManager.hasInstance() ? MonTablesDictionaryManager.getInstance() : null;
//			while(rs.next())
//			{
//				TableColumnInfo ci = new TableColumnInfo();
//				ci._colName       = rs.getString("COLUMN_NAME");
//				ci._colPos        = rs.getInt   ("ORDINAL_POSITION");
//				ci._colType       = rs.getString("TYPE_NAME");
//				ci._colLength     = rs.getInt   ("COLUMN_SIZE");
//				ci._colIsNullable = rs.getInt   ("NULLABLE");
//				ci._colRemark     = rs.getString("REMARKS");
//				ci._colDefault    = rs.getString("COLUMN_DEF");
//				ci._colScale      = rs.getInt   ("DECIMAL_DIGITS");
//				
//				// Check with the MonTable dictionary for Descriptions
//				if (mtd != null && StringUtil.isNullOrBlank(ci._colRemark))
//					ci._colRemark = StringUtil.stripHtmlStartEnd(mtd.getDescription(_tabName, ci._colName));
//
//				addColumn(ci);
//			}
//			rs.close();
//			
//			if (StringUtil.hasValue(_tabType))
//			{
//				if ( _tabType.equalsIgnoreCase("TABLE") || _tabType.equalsIgnoreCase("SYSTEM") )
//				{
//					_pk = getPkOrFirstUniqueIndex(conn, _tabCat, _tabSchema, _tabName);
//					_extraInfo = conn.getTableExtraInfo(_tabCat, _tabSchema, _tabName);
//					
//	    			_mdIndex = new ResultSetTableModel( dbmd.getIndexInfo   (_tabCat, _tabSchema, _tabName, false, false), "getIndexInfo");
//	    			_mdFkOut = new ResultSetTableModel( dbmd.getImportedKeys(_tabCat, _tabSchema, _tabName),               "getImportedKeys");
//	    			_mdFkIn  = new ResultSetTableModel( dbmd.getExportedKeys(_tabCat, _tabSchema, _tabName),               "getExportedKeys");
//				}
//				else if ( _tabType.equalsIgnoreCase("VIEW") )
//				{
//					_viewReferences = conn.getViewReferences(_tabCat, _tabSchema, _tabName);
//				}
//			}
//
//			_needColumnRefresh = false;
//		}
//		catch (SQLException e)
//		{
//			_logger.warn("Problems looking up Column MetaData for table '"+_tabName+"'. Caught: "+e);
//		}
//	}
//	public void refreshColumnInfo(ConnectionProvider connProvider)
	public void refreshColumnInfo(final DbxConnection conn)
	{
		try
		{
//			final DbxConnection conn = connProvider.getConnection();
			if (conn == null)
				return;

			DatabaseMetaData dbmd = conn.getMetaData();

			ResultSet rs = dbmd.getColumns(_tabCat, _tabSchema, _tabName, "%");
			ResultSetTableModel rstm = new ResultSetTableModel(rs, "getColumns");
//rstm.setNullValuesAsEmptyInGetValuesAsType(true);
			rs.close();

			MonTablesDictionary mtd = MonTablesDictionaryManager.hasInstance() ? MonTablesDictionaryManager.getInstance() : null;

			for (int r=0; r<rstm.getRowCount(); r++)
			{
				TableColumnInfo ci = new TableColumnInfo();
				ci._colName       = rstm.getValueAsString (r, "COLUMN_NAME",      false);
				ci._colPos        = rstm.getValueAsInteger(r, "ORDINAL_POSITION", false);
				ci._colType       = rstm.getValueAsString (r, "TYPE_NAME",        false);
				ci._colLength     = rstm.getValueAsInteger(r, "COLUMN_SIZE",      false);
				ci._colIsNullable = rstm.getValueAsInteger(r, "NULLABLE",         false);
				ci._colRemark     = rstm.getValueAsString (r, "REMARKS",          false);
				ci._colDefault    = rstm.getValueAsString (r, "COLUMN_DEF",       false);
				ci._colScale      = rstm.getValueAsInteger(r, "DECIMAL_DIGITS",   false, 0);
				
				// Check with the MonTable dictionary for Descriptions
				if (mtd != null && StringUtil.isNullOrBlank(ci._colRemark))
					ci._colRemark = StringUtil.stripHtmlStartEnd(mtd.getDescription(_tabName, ci._colName));

				addColumn(ci);
			}
			
			if (StringUtil.hasValue(_tabType))
			{
				if ( _tabType.equalsIgnoreCase("TABLE") || _tabType.equalsIgnoreCase("SYSTEM") )
				{
					//System.out.println("refreshColumnInfo(): _tabCat='"+_tabCat+"', _tabSchema='"+_tabSchema+"', _tabName='"+_tabName+"'.");
					
					_pk = getPkOrFirstUniqueIndex(conn, _tabCat, _tabSchema, _tabName);
					_extraInfo = conn.getTableExtraInfo(_tabCat, _tabSchema, _tabName);
					
	    			_mdIndex = new ResultSetTableModel( dbmd.getIndexInfo   (_tabCat, _tabSchema, _tabName, false, false), "getIndexInfo");
	    			_mdFkOut = new ResultSetTableModel( dbmd.getImportedKeys(_tabCat, _tabSchema, _tabName),               "getImportedKeys");
	    			_mdFkIn  = new ResultSetTableModel( dbmd.getExportedKeys(_tabCat, _tabSchema, _tabName),               "getExportedKeys");
				}
				else if ( _tabType.equalsIgnoreCase("VIEW") )
				{
					_viewReferences = conn.getViewReferences(_tabCat, _tabSchema, _tabName);
				}
			}

			_needColumnRefresh = false;
		}
		catch (SQLException e)
		{
			_logger.warn("Problems looking up Column MetaData for table '"+_tabName+"'. Caught: "+e);
		}
	}

//	public List<String> getPkOrFirstUniqueIndex(Connection conn, String cat, String schema, String name)
//	throws SQLException
//	{
//		DatabaseMetaData dbmd = conn.getMetaData();
//
//		// Get Primary key
//		ResultSet rs = dbmd.getPrimaryKeys(cat, schema, name);
//		List<String> pk = new ArrayList<String>();
//		while(rs.next())
//		{
//			pk.add(rs.getString("COLUMN_NAME"));
//		}
//		rs.close();
//
//		String firstIndex = null;
//		if (pk.isEmpty())
//		{
//			rs = dbmd.getIndexInfo(cat, schema, name, true, false);
//			while(rs.next())
//			{
//				int    typ     = rs.getInt   ("TYPE");
//			//	int    pos     = rs.getInt   ("ORDINAL_POSITION");
//				String colName = rs.getString("COLUMN_NAME");
//				String idxName = rs.getString("INDEX_NAME");
//
//				if (typ == DatabaseMetaData.tableIndexStatistic)
//					continue;
//
//				if (firstIndex == null)
//					firstIndex = idxName;
//
//				// Only add cols for FIRST index
//				if (firstIndex.equals(idxName))
//				{
//					pk.add(colName);
//				}
//			}
//			rs.close();
//		}
//		return pk;
//	}
	public static List<String> getPkOrFirstUniqueIndex(Connection conn, String cat, String schema, String name)
	throws SQLException
	{
		DatabaseMetaData dbmd = conn.getMetaData();
//System.out.println("getPkOrFirstUniqueIndex(): cat='"+cat+"', schema='"+schema+"', name='"+name+"'");

		// Get Primary key
		ResultSet rs = dbmd.getPrimaryKeys(cat, schema, name);
		ResultSetTableModel rstm = new ResultSetTableModel(rs, "getPrimaryKeys");
		rs.close();

		// It looks like "KEY_SEQ" can be out of order, so lets sort...
		rstm.sort("KEY_SEQ");
		
		List<String> pk = new ArrayList<String>();
		for (int r=0; r<rstm.getRowCount(); r++)
		{
			pk.add(rstm.getValueAsString(r, "COLUMN_NAME", false));
		}

		String firstIndex = null;
		if (pk.isEmpty())
		{
			rs = dbmd.getIndexInfo(cat, schema, name, true, false);
			rstm = new ResultSetTableModel(rs, "getIndexInfo");

			// Lets try to sort this as well
			rstm.sort("ORDINAL_POSITION");

			for (int r=0; r<rstm.getRowCount(); r++)
			{
				int    typ     = rstm.getValueAsInteger(r, "TYPE",             false);
			//	int    pos     = rstm.getValueAsInteger(r, "ORDINAL_POSITION", false);
				String colName = rstm.getValueAsString (r, "COLUMN_NAME",      false);
				String idxName = rstm.getValueAsString (r, "INDEX_NAME",       false);

				if (typ == DatabaseMetaData.tableIndexStatistic)
					continue;

				if (firstIndex == null)
					firstIndex = idxName;

				// Only add cols for FIRST index
				if (firstIndex.equals(idxName))
				{
					pk.add(colName);
				}
			}
		}
		return pk;
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
		sb.append(StringUtil.hasValue(_tabSchema) ? _tabSchema : _tabCat).append(".<B>").append(_tabName).append("</B> - <font color='blue'>").append(_tabType).append("</font>");
		sb.append("<HR>");
		sb.append("<BR>");
		sb.append("<B>Description:</B> ").append(StringUtil.isNullOrBlank(_tabRemark) ? "not available" : _tabRemark).append("<BR>");
		sb.append("<BR>");
		sb.append("<B>Columns:</B> ").append("<BR>");
		sb.append("<TABLE ALIGN='left' BORDER=0 CELLSPACING=0 CELLPADDING=1>");
		sb.append("<TR ALIGN='left' VALIGN='top' BGCOLOR='#ffffff'>");
		sb.append(" <TD NOWRAP BGCOLOR='#cccccc'><FONT COLOR='#000000'><b>").append("Pk")         .append("</B></FONT></TD>");
		sb.append(" <TD NOWRAP BGCOLOR='#cccccc'><FONT COLOR='#000000'><b>").append("Name")       .append("</B></FONT></TD>");
		sb.append(" <TD NOWRAP BGCOLOR='#cccccc'><FONT COLOR='#000000'><b>").append("Datatype")   .append("</B></FONT></TD>");
		sb.append(" <TD NOWRAP BGCOLOR='#cccccc'><FONT COLOR='#000000'><b>").append("Length")     .append("</B></FONT></TD>");
		sb.append(" <TD NOWRAP BGCOLOR='#cccccc'><FONT COLOR='#000000'><b>").append("Nulls")      .append("</B></FONT></TD>");
		sb.append(" <TD NOWRAP BGCOLOR='#cccccc'><FONT COLOR='#000000'><b>").append("Pos")        .append("</B></FONT></TD>");
		sb.append(" <TD NOWRAP BGCOLOR='#cccccc'><FONT COLOR='#000000'><b>").append("Description").append("</B></FONT></TD>");
		sb.append("</TR>");
		int r=0;
		for (TableColumnInfo ci : _columns)
		{
			r++;
			if ( (r % 2) == 0 )
				sb.append("<TR ALIGN='left' VALIGN='top' BGCOLOR='#ffffff'>");
			else
				sb.append("<TR ALIGN='left' VALIGN='top' BGCOLOR='#ffffcc'>");
			sb.append("	<TD NOWRAP>").append( isColPartOfPk(ci._colName) ? "&bull;" : "&nbsp;").append("</TD>");
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

		if (StringUtil.hasValue(_tabType))
		{
			if ( _tabType.equalsIgnoreCase("TABLE") || _tabType.equalsIgnoreCase("SYSTEM") )
			{
				sb.append(getOtherTableInfo());

				sb.append("<B>Index Descriptions</B><BR>");
				sb.append(getIndexDesc(_mdIndex));
				sb.append("<HR>");

				sb.append("<B>Foreign Key</B> -&gt; TO other tables<BR>");
				sb.append(getFkOutboundDesc(_mdFkOut));
				sb.append("<HR>");

				sb.append("<B>Foreign Key</B> &lt;- FROM other tables<BR>");
				sb.append(getFkInboundDesc(_mdFkIn));
				sb.append("<HR>");
			}
			else if ( _tabType.equalsIgnoreCase("VIEW") )
			{
				sb.append(getViewReferences());
			}
		}

		sb.append("-end-<BR><BR>");
		
		return sb.toString();
	}

	// Generate HTML descriptions for "view references" stuff
	private String getViewReferences()
	{
		if (_viewReferences == null)
			return "";

		if (_viewReferences.isEmpty())
			return "";

		StringBuilder sb = new StringBuilder();
		sb.append("<B>The view references the following " + _viewReferences.size() + " objects</B><BR>");
		sb.append("<UL>");
		for (String str : _viewReferences)
		{
			sb.append("  <LI><CODE>").append(str).append("</CODE></LI>");
		}
		sb.append("</UL>");
		sb.append("<HR>");
		return sb.toString();
	}
	// Generate HTML descriptions for "Other" stuff
	private String getOtherTableInfo()
	{
		if (_extraInfo == null)
			return "";

		StringBuilder sb = new StringBuilder();
		sb.append("<B>Extra Table Information</B><BR>");

//		sb.append("<UL>");
//		for (TableExtraInfo ei : _extraInfo.values())
//		{
//			if ( ! TableExtraInfo.IndexExtraInfo.equals(ei.getName()) )
//			{
//			//	sb.append("  <LI>").append("<B>").append(ei.getDescriptiveName()).append(": </B>").append(ei.getStringValue()).append("<BR><FONT color='green'>").append(ei.getHtmlDescription()).append("</FONT></LI>");
//				sb.append("  <LI>").append("<B>").append(ei.getDescriptiveName()).append(": </B> ").append(ei.getStringValue()).append("<FONT color='green'> -- ").append(ei.getHtmlDescription()).append("</FONT></LI>");
//			}
//		}
//		sb.append("</UL>");

		sb.append("<table BORDER=0 CELLSPACING=0 CELLPADDING=1>");
		for (TableExtraInfo ei : _extraInfo.values())
		{
			if ( ! (TableExtraInfo.IndexExtraInfo.equals(ei.getName()) || TableExtraInfo.IndexExtraInfoDescription.equals(ei.getName())) )
			{
				sb.append("  <TR><TD nowrap>&nbsp;&nbsp;&nbsp;&nbsp;&bull;&nbsp;</TD><TD nowrap>").append("<B>").append(ei.getDescriptiveName()).append("</B> </TD> <TD nowrap>&nbsp;").append(ei.getStringValue()).append("&nbsp;</TD> <TD nowrap><FONT color='green'>").append(ei.getHtmlDescription()).append("</FONT></TD></TR>");
			}
		}
		sb.append("</table>");

		sb.append("<HR>");
		return sb.toString();
	}
	// Generate HTML index descriptions
	private String getIndexDesc(ResultSetTableModel md)
	{
		if (md == null)
			return "<UL><LI><CODE>No Indexes</CODE></LI></UL>";

		if (md.getRowCount() == 0)
			return "<UL><LI><CODE>No Indexes</CODE></LI></UL>";

		Map<String, Index> indexes = new LinkedHashMap<>();
		
		for (int r=0; r<md.getRowCount(); r++)
		{
			// tableIndexStatistic - this identifies table statistics that are returned in conjuction with a table's index descriptions
			// tableIndexClustered - this is a clustered index
			// tableIndexHashed    - this is a hashed index
			// tableIndexOther     - this is some other style of index
			Integer type          = md.getValueAsInteger(r, "TYPE", false);
//System.out.println("getIndexDesc(): type="+type);
			if (type == null) continue;
			if (type == DatabaseMetaData.tableIndexStatistic) continue;

			String  indexName      = md.getValueAsString (r, "INDEX_NAME",       false);
			String  nonUnique      = md.getValueAsString (r, "NON_UNIQUE",       false);
			Integer ordinalPos     = md.getValueAsInteger(r, "ORDINAL_POSITION", false);
			String  columnName     = md.getValueAsString (r, "COLUMN_NAME",      false);
			String  ascOrDesc      = md.getValueAsString (r, "ASC_OR_DESC",      false);
//			String  indexQualifier = md.getValueAsString (r, "INDEX_QUALIFIER",  false);
			String  tableName      = md.getValueAsString (r, "TABLE_NAME",       false);
//System.out.println("getIndexDesc(): indexName='"+indexName+"', nonUnique="+nonUnique+", ordinalPos="+ordinalPos+", columnName='"+columnName+"', ascOrDesc='"+ascOrDesc+"', indexQualifier='"+indexQualifier+"'.");

			if (StringUtil.isNullOrBlank(indexName))
				continue;

			Index ind = indexes.get(indexName);
			if (ind == null)
			{
				ind = new Index(indexName, tableName, type, nonUnique);
				indexes.put(indexName, ind);
			}
			ind.addColumn(ordinalPos, columnName, "D".equalsIgnoreCase(ascOrDesc));
		}
		
		if (indexes.size() == 0)
		{
			return "<UL><LI><CODE>No Indexes</CODE></LI></UL>";
		}

		StringBuilder sb = new StringBuilder();
		sb.append("<UL>\n");
		for (Index ind : indexes.values())
			sb.append("  <LI><CODE>").append(ind.getDdl()).append("</CODE></LI>\n");
		sb.append("</UL>\n");
		return sb.toString();
//		return mdIndex.toHtmlTableString();
	}
	private class Index
	{
		String  _name      = null;
		String  _qualifier = null;
		int     _type      = -1;
		boolean _isUnique  = false;
		List<String> _columns = new ArrayList<>();
		
		public Index(String name, String indexQualifier, Integer type, String nonUnique)
		{
			_name      = name;
			_qualifier = indexQualifier;
			_type      = type;
			_isUnique  = ("0".equals(nonUnique) || "FALSE".equalsIgnoreCase(nonUnique) || "NO".equalsIgnoreCase(nonUnique));
		}
		public void addColumn(int pos, String name, boolean isDescending)
		{
			_columns.add(name + (isDescending ? " desc" : ""));
		}
		public String getDdl()
		{
			boolean indexExtraInfoDescriptionHasClustered    = false;
//			boolean indexExtraInfoDescriptionHasNonClustered = false;
			
			// If we have some extra information on the index name, print that as well
			StringBuilder comment = new StringBuilder();
			if (_extraInfo != null)
			{
				for (TableExtraInfo ei : _extraInfo.values())
				{
					String indexExtraInfo1 = "";
					String indexExtraInfo2 = "";
					
					if ( TableExtraInfo.IndexExtraInfoDescription.equals(ei.getName()) )
					{
						@SuppressWarnings("unchecked")
						Map<String, String> indexInfo = (Map<String, String>) ei.getValue();
						String indexExtraInfo = indexInfo.get(_name);
						if (indexExtraInfo != null)
						{
//							sb.append(" <FONT color='green'>").append(" -- ").append(indexExtraInfo).append("</FONT>");
							indexExtraInfo1 += indexExtraInfo;
							
							if (indexExtraInfo.toLowerCase().indexOf("clustered") != -1)
							{
								indexExtraInfoDescriptionHasClustered = true;
							}

							if (indexExtraInfo.toLowerCase().indexOf("nonclustered") != -1)
							{
								indexExtraInfoDescriptionHasClustered    = false;
//								indexExtraInfoDescriptionHasNonClustered = true;
							}
						}
					}

					if ( TableExtraInfo.IndexExtraInfo.equals(ei.getName()) )
					{
						@SuppressWarnings("unchecked")
						Map<String, String> indexInfo = (Map<String, String>) ei.getValue();
						String indexExtraInfo = indexInfo.get(_name);
						if (indexExtraInfo != null)
						{
//							sb.append(" <FONT color='green'>").append(" -- ").append(indexExtraInfo).append("</FONT>");
							indexExtraInfo2 += indexExtraInfo;
						}
					}

					if (StringUtil.hasValue(indexExtraInfo1) || StringUtil.hasValue(indexExtraInfo2))
					{
						comment.append(" <FONT color='green'>").append(" -- ");

						// write Index "type" description
						comment.append(indexExtraInfo1);
						if (StringUtil.hasValue(indexExtraInfo1))
							comment.append(", ");

						// write Index "size" info
						comment.append(indexExtraInfo2);

						comment.append("</FONT>");
					}
				}
			}

			// Now build the DDL text
			StringBuilder sb = new StringBuilder("create ");

			if (_isUnique)
				sb.append("<FONT color='blue'>unique</FONT> ");

			if (_type == DatabaseMetaData.tableIndexClustered || indexExtraInfoDescriptionHasClustered)
				sb.append("<FONT color='blue'>clustered</FONT> ");
			
			sb.append("index ").append(_name).append(" on ").append(_qualifier);
			sb.append("(<FONT color='blue'>");
			sb.append(StringUtil.toCommaStr(_columns));
			sb.append("</FONT>)");

			if (_type == DatabaseMetaData.tableIndexHashed)
				sb.append(" -- hashed");

			// Add any comments from 'IndexExtraInfoDescription' and 'IndexExtraInfo'
			sb.append(comment.toString());
			
			return sb.toString();
		}
	}

	// Generate HTML ForeignKeys TO other Tables descriptions
	private String getFkOutboundDesc(ResultSetTableModel md)
	{
		if (md == null)
			return "<UL><LI><CODE>None</CODE></LI></UL>";

		if (md.getRowCount() == 0)
			return "<UL><LI><CODE>None</CODE></LI></UL>";

		Map<String, ForeignKey> fkMap = new LinkedHashMap<>();
		
		for (int r=0; r<md.getRowCount(); r++)
		{
			String  FK_NAME         = md.getValueAsString (r, "FK_NAME",       false);
			Integer DEFERRABILITY   = md.getValueAsInteger(r, "DEFERRABILITY", false);
			Integer DELETE_RULE     = md.getValueAsInteger(r, "DELETE_RULE",   false);
			Integer UPDATE_RULE     = md.getValueAsInteger(r, "UPDATE_RULE",   false);
			String  PKTABLE_CAT     = md.getValueAsString (r, "PKTABLE_CAT",   false);
			String  PKTABLE_SCHEM   = md.getValueAsString (r, "PKTABLE_SCHEM", false);
			String  PKTABLE_NAME    = md.getValueAsString (r, "PKTABLE_NAME",  false);
			String  FKTABLE_CAT     = md.getValueAsString (r, "FKTABLE_CAT",   false);
			String  FKTABLE_SCHEM   = md.getValueAsString (r, "FKTABLE_SCHEM", false);
			String  FKTABLE_NAME    = md.getValueAsString (r, "FKTABLE_NAME",  false);
			String  FKCOLUMN_NAME   = md.getValueAsString (r, "FKCOLUMN_NAME", false);
			String  PKCOLUMN_NAME   = md.getValueAsString (r, "PKCOLUMN_NAME", false);

			if (StringUtil.isNullOrBlank(FK_NAME))
				continue;

			ForeignKey fk = fkMap.get(FK_NAME);
			if (fk == null)
			{
				fk = new ForeignKey(FK_NAME, DEFERRABILITY, DELETE_RULE, UPDATE_RULE, 
						PKTABLE_CAT, PKTABLE_SCHEM, PKTABLE_NAME, 
						FKTABLE_CAT, FKTABLE_SCHEM, FKTABLE_NAME);
				fkMap.put(FK_NAME, fk);
			}
			fk.addFkColumn(FKCOLUMN_NAME);
			fk.addPkColumn(PKCOLUMN_NAME);
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append("<UL>\n");
		for (ForeignKey fk : fkMap.values())
			sb.append("  <LI><CODE>").append(fk.getDdl()).append("</CODE></LI>\n");
		sb.append("</UL>\n");
		return sb.toString();
//		return md.toHtmlTableString();
	}

	// Generate HTML ForeignKeys FROM other Tables descriptions
	private String getFkInboundDesc(ResultSetTableModel md)
	{
		return getFkOutboundDesc(md);
	}
	private static class ForeignKey
	{
		String  _fkName      = null;
		String  _deleteRule  = null;
		String  _updateRule  = null;
		String  _pkTabCat    = null;
		String  _pkTabSchema = null;
		String  _pkTabName   = null;
		String  _fkTabCat    = null;
		String  _fkTabSchema = null;
		String  _fkTabName   = null;

		List<String> _fkColumns = new ArrayList<>();
		List<String> _pkColumns = new ArrayList<>();
		
		public ForeignKey(String fkName, Integer deferrability, Integer deleteRule, Integer updateRule, String pkTabCat, String pkTabSchema, String pkTabName, String fkTabCat, String fkTabSchema, String fkTabName)
		{
			_fkName = fkName;
			
			if      (deleteRule == DatabaseMetaData.importedKeyCascade)  _deleteRule = "CASCADE";
			else if (deleteRule == DatabaseMetaData.importedKeyRestrict) _deleteRule = "RESTRICT";
			else if (deleteRule == DatabaseMetaData.importedKeySetNull)  _deleteRule = "SET NULL";
			else                                                         _deleteRule = "NO ACTION";

			if      (updateRule == DatabaseMetaData.importedKeyCascade)  _updateRule = "CASCADE";
			else if (updateRule == DatabaseMetaData.importedKeyRestrict) _updateRule = "RESTRICT";
			else if (updateRule == DatabaseMetaData.importedKeySetNull)  _updateRule = "SET NULL";
			else                                                         _updateRule = "NO ACTION";

			_pkTabCat    = pkTabCat;
			_pkTabSchema = pkTabSchema;
			_pkTabName   = pkTabName;
			_fkTabCat    = fkTabCat;
			_fkTabSchema = fkTabSchema;
			_fkTabName   = fkTabName;
		}
		public void addFkColumn(String name)
		{
			_fkColumns.add(name);
		}
		public void addPkColumn(String name)
		{
			_pkColumns.add(name);
		}
		private String objName(String cat, String schema, String name)
		{
			StringBuilder sb = new StringBuilder();
			if (StringUtil.hasValue(cat))    sb.append(cat)   .append(".");
			if (StringUtil.hasValue(schema)) sb.append(schema).append(".");
			sb.append(name);
			return sb.toString();
		}
		public String getDdl()
		{
			// constraint fk_AppServer    foreign key(versionId, resourceAppServer) references Resources_AppServer(versionId, resourceName),
			// alter table XXX add constraint XXX foreign key(c1,c2) references XXX(c1,c2)
			StringBuilder sb = new StringBuilder();
			sb.append("alter table ").append(objName(_fkTabCat, _fkTabSchema, _fkTabName));
			sb.append(" add constraint ").append(_fkName);
			sb.append(" foreign key(<FONT color='blue'>").append(StringUtil.toCommaStr(_fkColumns)).append("</FONT>)");
			sb.append("<BR>");
			sb.append(" references ").append(objName(_pkTabCat, _pkTabSchema, _pkTabName)).append("(<FONT color='blue'>").append(StringUtil.toCommaStr(_pkColumns)).append("</FONT>)");
			
			sb.append("<BR>");
			sb.append("<FONT color='green'>");
			sb.append(" -- ");
			sb.append(" on update ").append(_updateRule);
			sb.append(" on delete ").append(_deleteRule);
			sb.append("</FONT>");
			
			return sb.toString();
		}
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
		sb.append(_tabSchema).append(".<B>").append(_tabName).append(".").append(ci._colName).append("</B> - <font color='blue'>").append(_tabType).append(" - COLUMN").append("</font>");
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

	public boolean isColPartOfPk(String colname)
	{
		if (_pk == null)
			return false;
		
		return _pk.contains(colname);
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



	private int getMaxColLenth()
	{
		int max = 0;
		for (TableColumnInfo ci : _columns)
		{
			max = Math.max(max, ci._colName.length());
		}
		return max;
	}

	private String getColDataTypeDesc(String colname)
	{
		return getColDataTypeDesc(colname, false);
	}
	private String getColDataTypeDesc(String colname, boolean includeColName)
	{
		TableColumnInfo ci = getColumnInfo(colname);
		if (ci == null)
			return "  -- Column name '"+colname+"', was not found in table '"+_tabName+"'.";

		String dispColName = includeColName ? StringUtil.left(colname, getMaxColLenth())+" : " : "";
		
		String datatype = ci._colType;
		String nulls    = ci._colIsNullable == DatabaseMetaData.columnNoNulls ? ": NOT NULL" : ": Allow NULL";
		String partOfPk = isColPartOfPk(colname) ? ", Part Of: PrimaryKey or first unique index" : "";

		// Compose data type
		String dtlower = datatype.toLowerCase();
		if ( dtlower.equals("char") || dtlower.equals("varchar") )
			datatype = datatype + "(" + ci._colLength + ")";
		
		if ( dtlower.equals("numeric") || dtlower.equals("decimal") )
			datatype = datatype + "(" + ci._colLength + "," + ci._colScale + ")";

		return "  -- " + dispColName + StringUtil.left(datatype, 20) + nulls + partOfPk;
	}
	private String getColDataTypeExValue(String colname)
	{
		TableColumnInfo ci = getColumnInfo(colname);
		if (ci == null)
			return "Column name '"+colname+"', was not found in table '"+_tabName+"'.";

		String datatype = ci._colType;
		String example = "0";

		// Compose data type
		String dtlower = datatype.toLowerCase();
		if ( dtlower.equals("char") || dtlower.equals("varchar") )
			example = "'str'";
		
		if ( dtlower.equals("numeric") || dtlower.equals("decimal") )
			example = "0.0";

		return StringUtil.left(example, 5);
	}

	/** Generate SELECT statement */
	public String toSelect()
	{
		return toSelect(false, -1);
	}
	/** Generate SELECT statement */
	public String toSelect(boolean forExec, int top)
	{
		StringBuilder sb = new StringBuilder();
		String topStr = top > 0 ? "top "+top : "";
		String fullTabName = StringUtil.sqlSafeAlways(StringUtil.hasValue(_tabSchema) ? _tabSchema : _tabCat) + "." + StringUtil.sqlSafeAlways(_tabName);

		sb.append("select ").append(topStr).append("\n");
		String comma = "  ";
		for (TableColumnInfo ci : _columns)
		{
			String col   = StringUtil.left(StringUtil.sqlSafeAlways(ci._colName), getMaxColLenth(), true);
			String desc  = getColDataTypeDesc(ci._colName);
			sb.append("    ").append(comma).append(col).append(desc).append("\n");
			comma = ", ";
		}

		sb.append("from ").append(fullTabName).append("  -- pk='"+_pk+"'").append(StringUtil.isNullOrBlank(_tabRemark) ? "" : " -- desc: "+_tabRemark).append("\n");

		if ( ! forExec )
		{
			sb.append("where 1 = 1 \n");
			for (TableColumnInfo ci : _columns)
			{
				if (isColPartOfPk(ci._colName))
				{
					String col   = StringUtil.left(StringUtil.sqlSafeAlways(ci._colName), getMaxColLenth(), true);
					String desc  = getColDataTypeDesc   (ci._colName);
					String exVal = getColDataTypeExValue(ci._colName);
					sb.append("  and ").append(col).append(" = ").append(exVal).append(desc).append("\n");
				}
			}
		}
		
		return sb.toString();
	}

	/** Generate INSERT statement */
	public String toInsert()
	{
		StringBuilder sb = new StringBuilder();
		String comma;
		String fullTabName = StringUtil.sqlSafeAlways(StringUtil.hasValue(_tabSchema) ? _tabSchema : _tabCat) + "." + StringUtil.sqlSafeAlways(_tabName);

		String colsAtOneLine = " --(";
		comma = "";
		for (TableColumnInfo ci : _columns)
		{
			colsAtOneLine += comma + ci._colName;
			comma = ", ";
		}
		colsAtOneLine += ")";

		sb.append("insert into ").append(fullTabName).append(colsAtOneLine).append("\n");
		//sb.append(colsAtOneLine).append("\n");
		sb.append("(\n");
		comma = "  ";
		for (TableColumnInfo ci : _columns)
		{
			String col   = StringUtil.left(StringUtil.sqlSafeAlways(ci._colName), getMaxColLenth(), true);
			String desc  = getColDataTypeDesc(ci._colName);
			sb.append("    ").append(comma).append(col).append(desc).append("\n");
			comma = ", ";
		}
		sb.append(")\n");
		sb.append("values\n");
		sb.append("(\n");
		comma = "  ";
		for (TableColumnInfo ci : _columns)
		{
			String desc  = getColDataTypeDesc   (ci._colName, true);
			String exVal = getColDataTypeExValue(ci._colName);
			sb.append("    ").append(comma).append(exVal).append(desc).append("\n");
			comma = ", ";
		}
		sb.append(")\n");
		
		return sb.toString();
	}

	/** Generate UPDATE statement */
	public String toUpdate()
	{
		StringBuilder sb = new StringBuilder();
		String fullTabName = StringUtil.sqlSafeAlways(StringUtil.hasValue(_tabSchema) ? _tabSchema : _tabCat) + "." + StringUtil.sqlSafeAlways(_tabName);

		sb.append("update ").append(fullTabName).append("\n");
		String pre = "   set ";
		for (TableColumnInfo ci : _columns)
		{
			String col   = StringUtil.left(StringUtil.sqlSafeAlways(ci._colName), getMaxColLenth(), true);
			String desc  = getColDataTypeDesc(ci._colName);
			String exVal = getColDataTypeExValue(ci._colName);
			sb.append(pre).append(col).append(" = ").append(exVal).append(desc).append("\n");
			pre = "     , ";
		}
		sb.append(" where 1 = 1\n");
		for (TableColumnInfo ci : _columns)
		{
			if (isColPartOfPk(ci._colName))
			{
				String col   = StringUtil.left(StringUtil.sqlSafeAlways(ci._colName), getMaxColLenth(), true);
				String desc  = getColDataTypeDesc   (ci._colName);
				String exVal = getColDataTypeExValue(ci._colName);
    			sb.append("   and ").append(col).append(" = ").append(exVal).append(desc).append("\n");
			}
		}
		
		return sb.toString();
	}

	/** Generate DELETE statement */
	public String toDelete()
	{
		StringBuilder sb = new StringBuilder();
		String fullTabName = StringUtil.sqlSafeAlways(StringUtil.hasValue(_tabSchema) ? _tabSchema : _tabCat) + "." + StringUtil.sqlSafeAlways(_tabName);

		sb.append("delete from ").append(fullTabName).append("\n");
		sb.append(" where 1 = 1\n");
		for (TableColumnInfo ci : _columns)
		{
			if (isColPartOfPk(ci._colName))
			{
				String col   = StringUtil.left(StringUtil.sqlSafeAlways(ci._colName), getMaxColLenth(), true);
				String desc  = getColDataTypeDesc(ci._colName);
				String exVal = getColDataTypeExValue(ci._colName);
    			sb.append("   and ").append(col).append(" = ").append(exVal).append(desc).append("\n");
			}
		}
		
		return sb.toString();
	}
}
