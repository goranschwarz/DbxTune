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
package com.dbxtune.cm;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import com.dbxtune.utils.AseConnectionUtils;

public class CounterSampleCatalogIteratorAse 
extends CounterSampleCatalogIterator
{
	private static final long serialVersionUID = 1L;

	// This is used if no databases are in a "valid" state.
	private List<String> _fallbackList = null;
	
	/**
	 * @param name
	 * @param negativeDiffCountersToZero
	 * @param diffColNames
	 * @param prevSample
	 * @param fallbackList a list of database(s) that will be used in case of "no valid" databases can be found, typically usage is "tempdb" to at least get one database.
	 */
	public CounterSampleCatalogIteratorAse(String name, boolean negativeDiffCountersToZero, String[] diffColNames, CounterSample prevSample, List<String> fallbackList)
	{
		super(name, negativeDiffCountersToZero, diffColNames, prevSample);
		_fallbackList = fallbackList;
	}
	
	@Override
	protected List<String> getCatalogList(CountersModel cm, Connection conn)
	throws SQLException
	{
		List<String> list = AseConnectionUtils.getDatabaseList(conn);

		// If the above get **no** databases that are in correct state add the "passed" database(s)
		if (list.isEmpty())
		{
			if (_fallbackList != null)
				list.addAll(_fallbackList);
		}

		return list;
	}
}
