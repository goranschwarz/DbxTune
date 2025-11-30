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
package com.dbxtune.pcs.sqlcapture;

import java.util.ArrayList;
import java.util.List;

/**
 * This holds DATA for SQL Capture entries<br>
 * Each of the SQL Capture entries should have the TABLE_NAME where to store the information as the first element in the list
 * 
 * @author gorans
 */
public class SqlCaptureDetails
{
	private List<List<Object>> _records = new ArrayList<List<Object>>();

	public boolean isEmpty()
	{
		return _records.isEmpty();
	}

	public int size()
	{
		return _records.size();
	}

	public void add(List<Object> row)
	{
		if (row == null)
			return;

		_records.add(row);
	}

	public List<List<Object>> getList()
	{
		return _records;
	}

}
