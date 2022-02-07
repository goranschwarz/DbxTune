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
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.asetune.utils;

import java.sql.Types;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.h2.tools.SimpleResultSet;

import com.asetune.gui.ResultSetTableModel;

/**
 * Usage:
 * 
 * @author goran
 *
 */
public class HtmlTableProducer
{
	private ResultSetTableModel _rstm;
	private List<String> _headers;
	private List<String> _footers;
	private String _htmlClassName;
	private LinkedHashMap<String, ColumnCopyRow> _ccr = new LinkedHashMap<>();
	
	private RowFilter _rowFilter;

	
	public HtmlTableProducer(ResultSetTableModel rstm, String htmlClassName)
	{
		_rstm = rstm;
		_htmlClassName = htmlClassName;
	}

	public HtmlTableProducer add(String rowName, ColumnCopyRow entry) 
	{ 
		_ccr.put(rowName, entry); 
		return this; 
	}
	
	public HtmlTableProducer setTableHeaders(String... name)
	{ 
		_headers = Arrays.asList(name);
		return this; 
	}
	
	public HtmlTableProducer setTableFooter(String... name)
	{ 
		_footers = Arrays.asList(name);
		return this; 
	}

	/**
	 * Check that everything is OK
	 * @return this object for "pipelining"
	 * 
	 * @throws RuntimeException incase of issues
	 */
	public HtmlTableProducer validate()
	{ 
		return validate(true);
	}
	/**
	 * Check that everything is OK
	 * 
	 * @param checkRstmColumns    If we want to check if columns exists in the ResultSet Table Model
	 * @return this object for "pipelining"
	 * 
	 * @throws RuntimeException incase of issues
	 */
	public HtmlTableProducer validate(boolean checkRstmColumns)
	{ 
		int headersCnt = _headers == null ? -1 : _headers.size();
		int footersCnt = _footers == null ? -1 : _footers.size();
		int bodyColCnt = -1;
		int bodyColCntMod = -1;

		int row = 0;
		int colSum = 0;
		int[] colCntForRow = new int[_ccr.size()];

		// Loop row and check if column count is same on all rows.
		for (ColumnCopyRow cch : _ccr.values())
		{
//			int cols = cch._ccd.size();
			int cols = 0;
			for (ColumnCopyAbstract ccd : cch._ccd)
				cols += ccd.getColSpan();
			
			colCntForRow[row] = cols;
			colSum += cols;
			row++;
		}
		
		bodyColCnt    = colSum / row;
		bodyColCntMod = colSum % row;

//System.out.println("validate(): colSum=" + colSum + ", bodyColCnt=" + bodyColCnt + ", bodyColCntMod=" + bodyColCntMod + ", colCntForRow=" + StringUtil.toCommaStr(colCntForRow) + ", headersCnt=" + headersCnt + ", footersCnt=" + footersCnt);
		
		if (bodyColCntMod != 0)
		{
			throw new RuntimeException("Number of columns within the table body is NOT the same for all rows. colCntForRow=" + StringUtil.toCommaStr(colCntForRow));
		}

		if (headersCnt != -1 && bodyColCnt != headersCnt)
			throw new RuntimeException("Number of headers is NOT equal numer of columns in table body. bodyColCnt=" + bodyColCnt + ", headersCnt=" + headersCnt + ", headers=" + StringUtil.toCommaStrQuoted(_headers));

		if (footersCnt != -1 && bodyColCnt != footersCnt)
			throw new RuntimeException("Number of headers is NOT equal numer of columns in table body. bodyColCnt=" + bodyColCnt + ", footersCnt=" + footersCnt + ", footers=" + StringUtil.toCommaStrQuoted(_footers));

		// check that all expected Column names can be found in the ResultSetTableModel 
		if (checkRstmColumns)
		{
			Set<String> notFoundColumnNames = new LinkedHashSet<>();
			
			for (ColumnCopyRow cch : _ccr.values())
			{
				for (ColumnCopyAbstract ccd : cch._ccd)
				{
					// Only check some sub classes
					if ( ccd instanceof ColumnCopyDef )
					{
						String rstmColname = ccd._copyColumnName;
						
						if ( ! _rstm.hasColumn(rstmColname) )
							notFoundColumnNames.add(rstmColname);
					}
				}
			}
			
			if ( ! notFoundColumnNames.isEmpty() )
			{
				throw new RuntimeException("Missing expected column name(s) " + StringUtil.toCommaStrQuoted("'", notFoundColumnNames) + " in the ResultSetTableModel named '" + _rstm.getName() + "'.");
			}
		}

		return this;
	}
	
	public String getHtmlTextForRow(int rstmRow)
	{
		StringBuilder sb = new StringBuilder();

		String className = "";
		if (StringUtil.hasValue(_htmlClassName))
			className = " class='" + _htmlClassName + "'";
		
		sb.append("<table" + className + "> \n");

		// Table Header
		if (_headers != null && !_headers.isEmpty())
		{
			sb.append("<thead> \n");
			sb.append("<tr> \n");

			sb.append("  <th></th> \n"); // first column is empty, which is the "row name"

			for (String header : _headers)
			{
				String thProps = "";

				// Check for <th> properties, like alignment
				// example: "column-name;align='right'"
				int pos = header.indexOf(';');
				if (pos > 0) // not as first char 
				{
					thProps = " " + header.substring(pos + 1);
					header  = header.substring(0, pos);
				}
				
				sb.append("  <th").append(thProps).append(">").append( header ).append("</th> \n");
			}
			
			sb.append("</tr> \n");
			sb.append("</thead> \n");
		}

		// Table BODY
		sb.append("<tbody> \n");
		for (Entry<String, ColumnCopyRow> entry : _ccr.entrySet())
		{
			String rowLabel    = entry.getKey();
			ColumnCopyRow cch = entry.getValue();

			boolean keepRow = true;
			for (ColumnCopyAbstract ccd : cch._ccd)
				if ( ccd instanceof ColumnCopyDef  &&  ! ccd._colRender.keepRow(_rstm.getValueAsObject(rstmRow, ccd._copyColumnName)) )
					keepRow = false;

			// Filter out some rows, with user defined logic
			if (keepRow && _rowFilter != null)
			{
				keepRow = _rowFilter.include(_rstm, rstmRow, rowLabel);
			}
			
			if (keepRow)
			{
				sb.append("<tr> \n");
				sb.append("  <td><b>").append( rowLabel ).append("</b></td> \n");
				for (ColumnCopyAbstract ccd : cch._ccd)
				{
					String tdProps = "";
					
					if (ccd.getColSpan() > 1)
						tdProps += " colspan='" + ccd.getColSpan() + "'";

					if ( StringUtil.hasValue(ccd.getColAlign()) )
						tdProps += " align='" + ccd.getColAlign() + "'";

					if (ccd instanceof EmptyColumn)
					{
						sb.append("  <td").append(tdProps).append("></td> \n");
					}
					else if (ccd instanceof ColumnStatic)
					{
						String content = ((ColumnStatic)ccd).getStaticContent();
						sb.append("  <td").append(tdProps).append("><i>").append(content).append("</i></td> \n");
					}
					else if (ccd instanceof ColumnRowLabelCopy)
					{
						String content = rowLabel;
						sb.append("  <td").append(tdProps).append("><b>").append(content).append("</b></td> \n");
					}
					else if (ccd instanceof ColumnContentProducer)
					{
						String content = ((ColumnContentProducer)ccd).getValue(_rstm, rstmRow, rowLabel);
						sb.append("  <td").append(tdProps).append(">").append(content).append("</td> \n");
					}
					else
					{
						Object rowContentObj = _rstm.getValueAsObject(rstmRow, ccd._copyColumnName);
						String rowContentStr = ccd._colRender.render(rowContentObj);
						
						if ( StringUtil.isNullOrBlank(ccd.getColAlign()) )
						{
							 String renderAlignment = ccd._colRender.getAlignment(rowContentObj);
							 if (StringUtil.hasValue(renderAlignment))
								tdProps += " align='" + renderAlignment + "'";
						}
						
						if (ccd.isColBold())
							rowContentStr = "<b>" + rowContentStr + "</b>";
						
						sb.append("  <td").append(tdProps).append(">").append( rowContentStr ).append("</td> \n");
					}
				}
				sb.append("</tr> \n");
			}
		}
		sb.append("</tbody> \n");
		
		// Table Footer
		if (_footers != null && !_footers.isEmpty())
		{
			sb.append("<tfoot> \n");
			sb.append("<tr> \n");

			sb.append("  <td></td> \n"); // first column is empty, which is the "row name"

			for (String header : _footers)
				sb.append("  <td>").append( header ).append("</td> \n");
			
			sb.append("</tr> \n");
			sb.append("</tfoot> \n");
		}

		sb.append("</table> \n");
		
		return sb.toString();
	}

	public static ColumnCopyRender MS_TO_HMS = new ColumnCopyRender() 
	{
		@Override
		public String render(Object in)
		{
			if (in instanceof Number)
				return TimeUtils.msToTimeStrDHMS( ((Number)in).longValue() );

			return in == null ? "" : in.toString();
		}
	};
	
	public static ColumnCopyRender US_TO_HMS = new ColumnCopyRender() 
	{
		@Override
		public String render(Object in)
		{
			if (in instanceof Number)
				return TimeUtils.usToTimeStrDHMS( ((Number)in).longValue() );

			return in == null ? "" : in.toString();
		}
	};
	
	public static ColumnCopyRender ONE_DECIMAL = new ColumnCopyRender() 
	{
		NumberFormat nf = null;
		{ // init/constructor section
			try
			{
				nf = new DecimalFormat();
				nf.setMinimumFractionDigits(1);
				nf.setMaximumFractionDigits(1);
			}
			catch (Throwable t)
			{
				nf = NumberFormat.getInstance();
			}
		}

		@Override
		public String render(Object in)
		{
			if (in instanceof Number)
				return nf.format(in);

			return in == null ? "" : in.toString();
		}
	};

	/**
	 * 
	 * @author goran
	 *
	 */
	public static class ColumnCopyRender
	{
		/** how to "print" the information */
		public String render(Object in)
		{
			if (in == null)
				return "";

			if (in instanceof Number)
			{
				NumberFormat nf = NumberFormat.getInstance();
				return nf.format(in);
			}

			return in.toString();
		}

		/** alignment */
		public String getAlignment(Object cellContent)
		{
			if (cellContent == null)
				return "";

			if (cellContent instanceof Number)
			{
				return "right";
			}
			return "";
		}

		/** Should we keep or skip this row */
		public boolean keepRow(Object in)
		{
			return true;
		}
	}

	/**
	 * 
	 * @author goran
	 *
	 */
	public static class ColumnCopyRow
	{
		private List<ColumnCopyAbstract> _ccd = new ArrayList<>();
		
		public ColumnCopyRow add(ColumnCopyAbstract ccd)
		{
			_ccd.add(ccd);
			return this;
		}

		public ColumnCopyRow addEmptyCol() 
		{ 
			_ccd.add( new EmptyColumn() );
			return this;
		}

		public ColumnCopyRow addStaticCol(String content) 
		{ 
			_ccd.add( new ColumnStatic(content) );
			return this;
		}

		public ColumnCopyRow addRowLabelCopy() 
		{ 
			_ccd.add( new ColumnRowLabelCopy() );
			return this;
		}
	}

	/**
	 * 
	 * @author goran
	 *
	 */
	public abstract static class ColumnCopyAbstract
	{
		String           _copyColumnName;
		ColumnCopyRender _colRender;
		String           _align;
		int              _span;
		boolean          _bold = false;

		public ColumnCopyAbstract(String copyColumnName)
		{
			this(copyColumnName, null, 1, null);
		}

		public ColumnCopyAbstract(String copyColumnName, ColumnCopyRender renderer)
		{
			this(copyColumnName, null, 1, renderer);
		}

		public ColumnCopyAbstract(String copyColumnName, String align, int colSpan)
		{
			this(copyColumnName, align, colSpan, null);
		}

		public ColumnCopyAbstract(String copyColumnName, String align, int colSpan, ColumnCopyRender renderer)
		{
			_copyColumnName = copyColumnName;
			_align = align;
			_span  = colSpan;
			_colRender = renderer != null ? renderer : new ColumnCopyRender();
		}
		
		public String  getColAlign() { return _align; }
		public int     getColSpan()  { return _span; }
		public boolean isColBold()   { return _bold; }

		public ColumnCopyAbstract setRenderer(ColumnCopyRender renderer) { _colRender = renderer; return this; }
		public ColumnCopyAbstract setColAlign(String align)              { _align     = align;    return this; }
		public ColumnCopyAbstract setColSpan (int cnt)                   { _span      = cnt;      return this; }
		public ColumnCopyAbstract setColBold ()                          { _bold      = true;     return this; }
		
//		public boolean isEmpty()
//		{
//			return false;
//		}
//
//		public String getStaticContent()
//		{
//			return null;
//		}
	}
	public static class ColumnCopyDef
	extends ColumnCopyAbstract
	{
		public ColumnCopyDef(String copyColumnName)
		{
			super(copyColumnName, null, 1, null);
		}

		public ColumnCopyDef(String copyColumnName, ColumnCopyRender renderer)
		{
			super(copyColumnName, null, 1, renderer);
		}

		public ColumnCopyDef(String copyColumnName, String align, int colSpan)
		{
			super(copyColumnName, align, colSpan, null);
		}

		public ColumnCopyDef(String copyColumnName, String align, int colSpan, ColumnCopyRender renderer)
		{
			super(copyColumnName, align, colSpan, renderer);
		}
		
	}

	/**
	 * 
	 * @author goran
	 *
	 */
	public static class EmptyColumn
	extends ColumnCopyAbstract
	{
		public EmptyColumn()
		{
			super("");
		}
//		@Override
//		public boolean isEmpty()
//		{
//			return true;
//		}
	}

	/**
	 * 
	 * @author goran
	 *
	 */
	public static class ColumnStatic
	extends ColumnCopyAbstract
	{
		String _label;
		public ColumnStatic(String label)
		{
			super("");
			_label = label;
		}

		public String getStaticContent()
		{
			return _label;
		}
	}

	public static class ColumnRowLabelCopy
	extends ColumnCopyAbstract
	{
		public ColumnRowLabelCopy()
		{
			super("");
		}
	}

	public abstract static class ColumnContentProducer
	extends ColumnCopyAbstract
	{
		public ColumnContentProducer()
		{
			super("");
		}

		public abstract String getValue(ResultSetTableModel rstm, int rstmRow, String rowKey);
	}

	
	
	public interface RowFilter
	{
		public boolean include(ResultSetTableModel rstm, int rstmRow, String rowKey);
	}

	public void setRowFilter(RowFilter rowFilter)
	{
		_rowFilter = rowFilter;
	}
	
	
//	public static String createHtmlKeyValueTableFromRow2(ResultSetTableModel sourceRstm, int row, LinkedHashMap<String, List<ColumnCopyDef>> rowSpec, String htmlClassname)
//	{
//		return "";
//	}
//	public static String createHtmlKeyValueTableFromRow(HtmlTableProducer htp, int row)
//	{
//		return htp.getHtmlTextForRow(row);
//	}


	public static String createHtmlTable(LinkedHashMap<String, String> map, String classname)
	{
		StringBuilder sb = new StringBuilder();

		String className = "";
		if (StringUtil.hasValue(classname))
			className = " class='" + classname + "'";
		
		sb.append("<table" + className + "> \n");

		sb.append("<tbody> \n");
		for (Entry<String, String> entry : map.entrySet())
		{
			sb.append("<tr> \n");
			sb.append("  <td><b>").append( entry.getKey()   ).append("</b></td> \n");
			sb.append("  <td>")   .append( entry.getValue() ).append(    "</td> \n");
			sb.append("</tr> \n");
		}
		sb.append("</tbody> \n");

		sb.append("</table> \n");
		
		return sb.toString();
	}

	public static void main(String[] args)
	{
		SimpleResultSet srs = new SimpleResultSet();
		srs.addColumn("c1",    Types.VARCHAR,       60, 0);
		srs.addColumn("c2",    Types.VARCHAR,       60, 0);
		srs.addColumn("c3",    Types.VARCHAR,       60, 0);
		srs.addColumn("c4",    Types.VARCHAR,       60, 0);
		srs.addColumn("c5",    Types.VARCHAR,       60, 0);
		srs.addColumn("c6",    Types.VARCHAR,       60, 0);

		srs.addRow("r0-c1", "r0-c2", "r0-c3", "r0-c4", "r0-c5", "r0-c6");
		srs.addRow("r1-c1", "r1-c2", "r1-c3", "r1-c4", "r1-c5", "r1-c6");
//		srs.addRow("r2-c1", "r2-c2", "r2-c3", "r2-c4", "r2-c5", "r2-c6");
//		srs.addRow("r3-c1", "r3-c2", "r3-c3", "r3-c4", "r3-c5", "r3-c6");
//		srs.addRow("r4-c1", "r4-c2", "r4-c3", "r4-c4", "r4-c5", "r4-c6");
//		srs.addRow("r5-c1", "r5-c2", "r5-c3", "r5-c4", "r5-c5", "r5-c6");
//		srs.addRow("r6-c1", "r6-c2", "r6-c3", "r6-c4", "r6-c5", "r6-c6");
//		srs.addRow("r7-c1", "r7-c2", "r7-c3", "r7-c4", "r7-c5", "r7-c6");
//		srs.addRow("r8-c1", "r8-c2", "r8-c3", "r8-c4", "r8-c5", "r8-c6");
//		srs.addRow("r9-c1", "r9-c2", "r9-c3", "r9-c4", "r9-c5", "r9-c6");
		
		try
		{
			ResultSetTableModel rstm = new ResultSetTableModel(srs, "dummyTable");

			ColumnCopyRender xxxRender = new ColumnCopyRender() 
			{
				@Override
				public String render(Object in)
				{
					return "<xxxxxxx>" + in + "</xxxxxxx>";
				}
			};
			
			HtmlTableProducer htp = new HtmlTableProducer(rstm, "dummy-class-name");
			htp.setTableHeaders("h1", "h2", "h3");
			htp.add("c1,c2,c3", new ColumnCopyRow().add( new ColumnCopyDef("c1") ) .add( new ColumnCopyDef("c2") ) .add( new ColumnCopyDef("c3") ));
			htp.add("c4,c5"   , new ColumnCopyRow().add( new ColumnCopyDef("c4") ) .add( new ColumnCopyDef("c5", "left", 2, null) ));
			htp.add("c5,c5"   , new ColumnCopyRow().add( new ColumnCopyDef("c5").setColAlign("left").setColSpan(2) ) .add( new ColumnCopyDef("c5") ));
			htp.add("c6"      , new ColumnCopyRow().add( new EmptyColumn()       ) .addEmptyCol()                  .add( new ColumnCopyDef("c6", null, 1, xxxRender) ));
			htp.validate();

			for (int r=0; r<rstm.getRowCount(); r++)
			{
				String txt = htp.getHtmlTextForRow(r);
				System.out.println("");
				System.out.println("----- row=" + r + " -------------------------------------------------------------");
				System.out.println(txt);
				System.out.println("-------------------------------------------------------------------------");
			}
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
}
