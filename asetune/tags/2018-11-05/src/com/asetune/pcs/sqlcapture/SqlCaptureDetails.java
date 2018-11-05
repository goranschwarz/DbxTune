package com.asetune.pcs.sqlcapture;

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
		_records.add(row);
	}

	public List<List<Object>> getList()
	{
		return _records;
	}

}
