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
package com.asetune.sql.diff.actions;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import com.asetune.sql.diff.DiffContext;
import com.asetune.sql.diff.DiffSink;
import com.asetune.sql.diff.DiffSink.DiffColumnValues;
import com.asetune.sql.diff.DiffTable;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;

public class DiffTableModel
extends AbstractTableModel
{
	private static final long serialVersionUID = 1L;

	private DiffSink           _sink;
	private List<String>       _colNames;
	private List<String>       _pkColNames;
	private List<Integer>      _pkColPos = new ArrayList<>();
	private List<List<Object>> _rows     = new ArrayList<>();

	public static final String PROPKEY_COLUMN_NO_DIFF = "DiffTableModel.column.no.diff.str";
	public static final String PROPKEY_COLOR_BG_LEFT  = "DiffTableModel.color.bg.left";
	public static final String PROPKEY_COLOR_BG_RIGHT = "DiffTableModel.color.bg.right";
	public static final String PROPKEY_COLOR_FG_PK    = "DiffTableModel.color.fg.pk";
	
	public static final String DEFAULT_COLUMN_NO_DIFF = null;
	public static final String DEFAULT_COLOR_BG_LEFT  = "#b3ffcc"; // *Light* green
	public static final String DEFAULT_COLOR_BG_RIGHT = "#ffc2b3"; // *Light* red
	public static final String DEFAULT_COLOR_FG_PK    = "blue";    // blue
	
	private static final String columnNoDiffStr = Configuration.getCombinedConfiguration().getProperty(PROPKEY_COLUMN_NO_DIFF, DEFAULT_COLUMN_NO_DIFF);
	private static final String htmlColorLeft   = Configuration.getCombinedConfiguration().getProperty(PROPKEY_COLOR_BG_LEFT,  DEFAULT_COLOR_BG_LEFT);
	private static final String htmlColorRight  = Configuration.getCombinedConfiguration().getProperty(PROPKEY_COLOR_BG_RIGHT, DEFAULT_COLOR_BG_RIGHT);
	private static final String htmlColorPk     = Configuration.getCombinedConfiguration().getProperty(PROPKEY_COLOR_FG_PK,    DEFAULT_COLOR_FG_PK);
	
	public DiffTableModel(DiffContext context)
	{
		this(context.getSink());
	}

	public DiffTableModel(DiffSink sink)
	{
		_sink = sink;
		_colNames = new ArrayList<>(_sink.getLeftDt().getColumnNames());

		_pkColNames = _sink.getContext().getPkColumns();
//System.out.println("::: _pkColNames="+_pkColNames);
		for (int c=0; c<_colNames.size(); c++)
		{
//			if (_pkColNames.contains( _colNames.get(c) ))
			if ( StringUtil.containsIgnoreCase(_pkColNames, _colNames.get(c)) )
				_pkColPos.add(c);
		}
//System.out.println("::: _pkColPos="+_pkColPos);

		// ADD 1 "prefix" column, which states what TYPE this row is (LEFT-is-missing, RIGHT-is-missing, COLS DIFF)
		// NOTE: This should be done AFTER initialization of: _pkColPos
		_colNames.add(0, "DIFF-TYPE");

		buildRows();
	}
	
	private void buildRows()
	{
		// MISSING on LEFT Side
		for (Object[] row : _sink.getLeftMissingRows())
		{
			List<Object> newRow = new ArrayList<>(_colNames.size());
			newRow.add("<html><span style='background-color:"+htmlColorLeft+"'>&lt;&lt;&lt; LEFT-side-is-missing-row:</span></html>");
			for (int c=0; c<row.length; c++)
				newRow.add(DiffTable.toTableValue(row[c]));

			_rows.add(newRow);
		}

		// MISSING on RIGHT Side
		for (Object[] row : _sink.getRightMissingRows())
		{
			List<Object> newRow = new ArrayList<>(_colNames.size());
			newRow.add("<html><span style='background-color:"+htmlColorRight+"'>&gt;&gt;&gt; RIGHT-side-is-missing-row:</span></html>");
			for (int c=0; c<row.length; c++)
				newRow.add(DiffTable.toTableValue(row[c]));

			_rows.add(newRow);
		}
	
		// Columns that is DIFFERENT
		for (DiffColumnValues dcv : _sink.getDiffColumnValues())
		{
			List<Object> newRow = new ArrayList<>(_colNames.size());

			newRow.add("<html>&lt;-&gt; COLS DIFF: <span style='color:"+htmlColorPk+"'><b>PK column</b></span> - <span style='background-color:"+htmlColorLeft+"'>left value</span> - <span style='background-color:"+htmlColorRight+"'>right value</span></html>");

			int pkPos = 0; // What PK Array Position are we at
			for (int c=1; c<_colNames.size(); c++)
			{
				int adjCol = c - 1;

//System.out.println("DIFF::: adjCol="+adjCol+", _pkColPos.contains(adjCol)="+_pkColPos.contains(adjCol)+", pkPos="+pkPos);
				if (_pkColPos.contains(adjCol))
				{
//System.out.println("DIFF:::PK adjCol="+adjCol+", drv._pk[pkPos]="+drv._pk[pkPos]+", drv._pk="+StringUtil.toCommaStr(drv._pk));
					newRow.add( "<html><b><font color='"+htmlColorPk+"'>" + dcv.getPkValues() [pkPos++] + "</font></b><html>");
				}
				else
				{
					if (dcv.getLeftColumnsPos().contains(adjCol))
					{
						Object leftColVal  = DiffTable.toTableValue( dcv.getLeftValues()  [adjCol] );
						Object rightColVal = DiffTable.toTableValue( dcv.getRightValues() [adjCol] );
						
						//String cell = DiffTable.toTableValue(leftColVal) + " <<-->> " + DiffTable.toTableValue(rightColVal);
						StringBuilder sb = new StringBuilder();
						sb.append("<html><span style='background-color:").append(htmlColorLeft).append("'>").append(leftColVal).append("</span> &lt;-&gt; <span style='background-color:").append(htmlColorRight).append("'>").append(rightColVal).append("</span></html>");
//						String cell = "<html>" + DiffTable.toTableValue(leftColVal) + " <<-->> " + DiffTable.toTableValue(rightColVal) + "/<html>";
//						newRow.add(cell);
						newRow.add(sb.toString());
					}
					else
					{
						newRow.add(columnNoDiffStr);
//						newRow.add(ResultSetTableModel.DEFAULT_NULL_REPLACE);
					}
				}
			}
			
			_rows.add(newRow);
		}
	}

	@Override
	public int getRowCount()
	{
		return _rows.size();
	}
	
	@Override
	public int getColumnCount()
	{
		return _colNames.size();
	}


	@Override
	public String getColumnName(int col)
	{
		return _colNames.get(col);
	}
	
	@Override
	public Object getValueAt(int row, int col)
	{
		return _rows.get(row).get(col);
	}

	
}
