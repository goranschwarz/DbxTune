package com.asetune.hostmon;

import java.sql.Timestamp;

public class OsTableRow
{
	protected HostMonitorMetaData _md = null;

	/** a Array of Objects, which holds both the data that will be delivered to the "client" */
	protected Object[] _values = null;

	/** A Map that holds a PK for each row. If no PK is specified, a empty string will indicate that */
	protected String   _pkStr  = "";

	/**
	 * Create a new OsTableRow object
	 */
	public OsTableRow()
	{
	}

	/**
	 * Create a new OsTableRow object
	 * @param md MetaData that holds information about the Parse(input) and SQL (output)
	 * @param inValues a String[] that will be used as source before creating the native java objects, which is described in the MetaData
	 * @throws OsRecordParseException 
	 */
	public OsTableRow(HostMonitorMetaData md, String[] inValues) 
	throws OsRecordParseException
	{
		_md = md;

		int parseCols = _md.getParseColumnCount();
		int sqlCols   = _md.getColumnCount();

		if (parseCols != inValues.length)
			throw new OsRecordParseException("MetaData ParseColumnCount="+parseCols+", and input records is '"+inValues.length+"'.");
		
		_values = new Object[sqlCols];

		for (HostMonitorMetaData.ColumnEntry ce : _md.getColumns())
		{
			int ip = ce._parseColNum - 1; // Input Position
			int dp = ce._sqlColNum   - 1; // Data Position (to local values array)
			if (ce._parseColNum > 0)
			{
				String val = inValues[ip].trim();

				if (ce._isPartOfPk)
					_pkStr += val + ":";
				
				// CREATE a new object of the desired type.
				_values[dp] = _md.newObject(ce, val);
			}
			else 
			{
				if (ce._status == HostMonitorMetaData.STATUS_COL_SAMPLE_TIME)
					_values[dp] = new Timestamp(System.currentTimeMillis());
			}
		}
		// take away trailing ':' from pk 
		if (_pkStr.endsWith(":"))
			_pkStr = _pkStr.substring(0, _pkStr.length() - 1 );

//		System.out.println("constructor->OsTableRow(ms, inValues):\n"+this.toString());
	}

	/**
	 * Get PK for this row.<br>
	 * If several columns in the PK, the records will be separated with a ':' char
	 * 
	 * @return The OK. No PK is indicated with the empty string ""
	 */
	public String getPk()
	{
		return _pkStr;
	}

	/**
	 * Get a Object for a specific column
	 * @param col, NOTE: starts at 1. so 1=col1, 2=col2, 0=outOfRange
	 * @return
	 */
	public Object getValue(int col)
	{
		return _values[ col - 1 ];
	}

	/**
	 * Set a Object for a specific column
	 * @param col, NOTE: starts at 1. so 1=col1, 2=col2, 0=outOfRange
	 * @return
	 */
	public void setValue(int col, Object val)
	{
		_values[ col - 1 ] = val;
	}

	/**
	 * Get a some debug info about the row.<br>
	 * Probably Used for debugging
	 * @return a String 
	 */
	public String toTraceString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("PK='").append(_pkStr).append("', ");
		for (HostMonitorMetaData.ColumnEntry ce : _md.getColumns())
		{
			int dp = ce._sqlColNum - 1; // Data Position (to local values array)

			sb.append("colName='").append(ce._colName).append("', ");
			sb.append("sqlColNum='").append(ce._sqlColNum).append("', ");
			sb.append("dp='").append(dp).append("', ");
			sb.append("data='").append(_values[dp]).append("', ");
			sb.append(".\n");
		}
		return sb.toString();
	}
}
