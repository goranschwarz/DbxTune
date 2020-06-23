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
package com.asetune.sql.diff.comp;

import java.util.Comparator;

import com.asetune.sql.diff.DiffContext;

public class ColComparator
implements Comparator<Object>
{
	private DiffContext _context;
	private boolean     _isCaseInSensitive = true;
	private String      _colName           = "-UNKNOWN-";
	private int         _jdbcDataType      = -99999;

	// Possibly if we want to keep track of left/right column names (and datatypes)... Column names can differ at least in Upper/Lower case
//	private String      _leftColName            = "-UNKNOWN-";
//	private int         _leftJdbcDataType       = -99999;
//	private String      _rightColName           = "-UNKNOWN-";
//	private int         _rightJdbcDataType      = -99999;
	
	public ColComparator(DiffContext context, String colName, int jdbcDataType)
	{
		_context = context;
		_isCaseInSensitive = _context.isCaseInSensitive();

		_jdbcDataType = jdbcDataType;

		_colName = colName;
		if (_colName == null)
			_colName = "-UNKNOWN-";
	}
//	public ColComparator(boolean caseInSensitive)
//	{
//		_isCaseInSensitive = caseInSensitive;
//	}

	@Override
	public int compare(Object left, Object right)
	{
		if (_context.isTraceEnabled())
			_context.addTraceMessage("    ColComparator: Left.obj=" + ( left  == null ? null : left .getClass().getCanonicalName() ) 
			                                       + ", Right.obj=" + ( right == null ? null : right.getClass().getCanonicalName() )
			                                       +", Left.toString=|" + left + "|, Right.toString=|" + right + "|.");
		if (left == right)
			return 0;

		if (left == null)
			return -1;

		if (right == null)
			return 1;

		if ( _isCaseInSensitive )
		{
			if (left instanceof String && right instanceof String)
				return String.CASE_INSENSITIVE_ORDER.compare( (String) left, (String) right );
		}
		
		// COMPARABLE, which would be the normal thing
		if (left instanceof Comparable)
			return ((Comparable)left).compareTo(right);


		// Handle Anything that was NOT camparable...
		
		// BINARY
//		if (_jdbcDataType == Types.BINARY || _jdbcDataType == Types.VARBINARY || _jdbcDataType == Types.LONGVARBINARY )
		if ( left instanceof byte[] && right instanceof byte[] )
			return byteCompare((byte[])left, (byte[])right);
		
		// if it's EQUAL then we can return
		if (left.equals(right))
			return 0;

		// End of line...
		throw new RuntimeException("Comparator on object, colName='"+_colName+"', problem: Left do not implement 'Comparable' and is not equal to right. Left.obj=|"+left.getClass().getCanonicalName()+"|, Right.obj=|"+right.getClass().getCanonicalName()+"|, Left.toString=|"+left+"|, Right.toString=|"+right+"|.");
	}

	public int byteCompare(byte[] left, byte[] right)
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
}
