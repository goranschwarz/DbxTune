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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.asetune.sql.diff.DiffContext;

public class PkComparator
implements Comparator<Object[]>
{
	private DiffContext _context;

	private List<String> _colNames;
	private List<Comparator> _colComparators = new ArrayList<>();

	public PkComparator(DiffContext context)
	{
		_context = context;

		_colNames = _context.getPkColumns();
//		for (String colName : _colNames)
//		{
//			_colComparators.add( new ColComparator(context, colName) );
//		}
		for (int c=0; c<_colNames.size(); c++)
		{
			String colName      = _colNames.get(c);
			int    jdbcDataType = _context.getPkColumnJdbcDataType(colName);
		
			_colComparators.add( new ColComparator(context, colName, jdbcDataType) );
		}
	}
	
//	public PkComparator(boolean caseInSensitive, List<String> colNames)
//	{
//		_colNames = colNames;
//		for (String colName : colNames)
//		{
//			_colComparators.add( new ColComparator(caseInSensitive) );
//		}
//	}
//	public PkComparator(boolean caseInSensitive, String... colNames)
//	{
//		this(caseInSensitive, Arrays.asList(colNames));
//	}
	
	@Override
	public int compare(Object[] left, Object[] right)
	{
		if (left == null)
			return -1;

		if (right == null)
			return 1;

		if (_context.isTraceEnabled())
			_context.addTraceMessage("  * PkComparator: colNames=" + _colNames);

		for (int i=0; i<_colComparators.size(); i++)
		{
			Comparator comparator = _colComparators.get(i);
			
			int retval = comparator.compare(left[i], right[i]);
			if (retval != 0)
				return retval;
			
		}

		// if comparators are exhausted, return 0
		return 0;
	}
}
