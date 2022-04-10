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
package com.asetune.cm;

import java.util.Comparator;
import java.util.List;

import javax.swing.table.TableModel;

public class ResultSetTableComparator
implements Comparator<List<Object>>
{
	private List<SortOptions> _sortOptions;
	private TableModel        _tm;
	private String            _name;
	
	public ResultSetTableComparator(List<SortOptions> sortOptions, TableModel tm, String name)
	{
		_sortOptions = sortOptions;
		_tm          = tm;
		_name        = name;
	}

	private int findColumn(String colName, boolean caseSensitive)
	{
		for (int c=0; c < _tm.getColumnCount(); c++) 
		{
			if ( caseSensitive ? colName.equals(_tm.getColumnName(c)) : colName.equalsIgnoreCase(_tm.getColumnName(c)) ) 
				return c;
		}
		return -1;
	}

	@Override
	public int compare(List<Object> leftList, List<Object> rightList)
	{
		int result = 0;

		for (SortOptions so : _sortOptions)
		{
			String  colName                = so.getColumnName();
			boolean isColNameCaseSensitive = so.isColumnNameCaseSensitive();

			boolean isAscending            = so.isAscending();
			boolean isCaseInSensitive      = so.isCaseInSensitive();
			
			int colPos = findColumn(colName, isColNameCaseSensitive);
			if (colPos == -1)
			{
				// Dismissing the "case sensitivity" and use case IN-SENSITIVE as a "backup"
				colPos = findColumn(colName, false); 
				if (colPos == -1)
					throw new RuntimeException("Sorting '" + _name + "', cant find column name '" + colName + "'.");
			}

			result = compare(colName, colPos, isAscending, isCaseInSensitive, leftList, rightList);
			if (result != 0)
				return result;
		}
		return result;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private int compare(String colName, int colPos, boolean isAscending, boolean isCaseInSensitive, List<Object> leftList, List<Object> rightList)
	{
		Object left  = leftList .get(colPos);
		Object right = rightList.get(colPos);
		
		if (left == right)
			return sortType( 0, isAscending );

		if (left == null)
			return sortType( -1, isAscending );

		if (right == null)
			return sortType( 1, isAscending );

		if ( isCaseInSensitive )
		{
			if (left instanceof String && right instanceof String)
				return sortType( String.CASE_INSENSITIVE_ORDER.compare( (String) left, (String) right ), isAscending );
		}
		
		// COMPARABLE, which would be the normal thing
		if (left instanceof Comparable)
			return sortType( ((Comparable)left).compareTo(right), isAscending );
		
		if ( left instanceof byte[] && right instanceof byte[] )
			return sortType( byteCompare((byte[])left, (byte[])right), isAscending );

		// End of line...
		throw new RuntimeException("Comparator on object, colName='"+colName+"', problem: Left do not implement 'Comparable' and is not equal to right. Left.obj=|"+left.getClass().getCanonicalName()+"|, Right.obj=|"+right.getClass().getCanonicalName()+"|, Left.toString=|"+left+"|, Right.toString=|"+right+"|.");
	}

	private int byteCompare(byte[] left, byte[] right)
	{
		for (int i = 0, j = 0; i < left.length && j < right.length; i++, j++)
		{
			int a = (left[i] & 0xff);
			int b = (right[j] & 0xff);
			if ( a != b )
			{
				return a - b;
			}
		}
		return left.length - right.length;
	}
	
	private int sortType(int order, boolean isAscending)
	{
		if (isAscending)
			return order;
		
		// Negate the number
		// * negative number to positive
		// * positive numbers negative
		// * leave 0 as 0
		return order *= -1;
	}
}
