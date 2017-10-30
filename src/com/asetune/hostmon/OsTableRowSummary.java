package com.asetune.hostmon;

public class OsTableRowSummary
extends OsTableRow
{
	/** number of samples this summary is based on */
	private int _numOfSamples = 0;

	/**
	 * Create a empty object which will be used to apply the summary fields to<br>
	 * The only columns/fields that will be filled in is the PK columns
	 * @param md The NetaData description for the table
	 * @param pk The Primary Keay for the record to create, should be empty "", if no PK
	 */
	public OsTableRowSummary(HostMonitorMetaData md, String pk) 
	{
		_md = md;

		int sqlCols = _md.getColumnCount();

		_pkStr = pk;
		_values = new Object[sqlCols];

		String[] pkStrPart = pk.split(":");
//		if (pkStrPart.length == 0)
//			System.out.println("pk='"+pk+"', pkStrPart.length="+pkStrPart.length);
		int pkPos = 0;

		for (HostMonitorMetaData.ColumnEntry ce : _md.getColumns())
		{
			if (ce._parseColNum > 0)
			{
				String val = null;
				if (ce._isPartOfPk)
					val = pkStrPart[pkPos++];

				// CREATE a new object of the desired type.
				int dp = ce._sqlColNum - 1;
				_values[dp] = _md.newObject(ce, val);
			}
		}
		// take away trailing ':' from pk 
		if (_pkStr.endsWith(":"))
			_pkStr = _pkStr.substring(0, _pkStr.length() - 1 );
		
//		System.out.println(":::::::::::::::::constructor->OsTableRow(ms, pk):\n"+this.toString());
	}
	
	/**
	 * If we want to do a average or summary, first we need to add X number of entries<br>
	 * At this stage the concerned columns are just "added" or summarized to the this current OsTableRowSummary object<br>
	 * Then at a second stage you can call calcAverage() to do the average calculation.
	 * @param entryToAdd
	 */
	public void addToSummary(OsTableRow entryToAdd)
	{
		_numOfSamples++;
		for (HostMonitorMetaData.ColumnEntry ce : _md.getColumns())
		{
			int dp = ce._sqlColNum - 1;
			if (ce._isStatColumn)
			{
				Object thisVal = _values[dp];
				Object addVal  = entryToAdd._values[dp];

				if (thisVal instanceof Number && addVal instanceof Number)
				{
					Number nThisVal = (Number) _values[dp];
					Number nAddVal  = (Number) entryToAdd._values[dp];

					String nStr = Double.toString( nThisVal.doubleValue() + nAddVal.doubleValue() );
					_values[dp] = _md.newObject(ce, nStr);
				}
				else
				{
					throw new RuntimeException("Unsupported data type for column '"+ce._colName+"' when adding to summary "
							+ "currentValue('"+thisVal+"')=" + (thisVal == null ? "can-not-determen-className" : thisVal.getClass().getName())
							+ ", addValue('"+addVal+"')="    + (addVal  == null ? "can-not-determen-className" : addVal.getClass().getName())
							+ ".");
				}
			}
			else // Just copy the value
			{
				//Object thisVal = _values[dp];
				Object addVal  = entryToAdd._values[dp];
				_values[dp] = addVal;
			}
		}
	}

	/**
	 * After X number of rows has been added to this OsTableRowSummary, we need to calculate 
	 * the average on columns which is marked for that.<br>
	 * Then this object would be a "normal" row which can be delivered to the client or enduser
	 */
	public void calcAverage()
	{
		for (HostMonitorMetaData.ColumnEntry ce : _md.getColumns())
		{
			int dp = ce._sqlColNum - 1;

			// if ((ce._status | HostMonitorMetaData.STATUS_COL_SUB_SAMPLE) == HostMonitorMetaData.STATUS_COL_SUB_SAMPLE)
			if (ce._status == HostMonitorMetaData.STATUS_COL_SUB_SAMPLE)
				_values[dp] = _md.newObject(ce, Integer.toString(_numOfSamples));

			if (_numOfSamples == 0)
				continue;

			if (ce._isStatColumn)
			{
				Object thisVal = _values[dp];

				if (thisVal instanceof Number)
				{
					Number nThisVal = (Number) _values[dp];

					String nStr = Double.toString( nThisVal.doubleValue() / _numOfSamples );
					_values[dp] = _md.newObject(ce, nStr);
				}
				else
				{
					throw new RuntimeException("Unsupported data type for column '"+ce._colName+"' when calculating average on value='"+thisVal+"' of type '"+thisVal.getClass().getName()+"'.");
				}
			}
		}
	}

//	public void XXXaddSummary(OsTableRow entryToAdd)
//	{
//		_numOfSamples++;
//		for (HostMonitorMetaData.ColumnEntry ce : _md.getColumns())
//		{
//			int dp = ce._sqlColNum - 1;
//			if (ce._isStatColumn)
//			{
//				Double thisVal = (Double)_values[dp];
//				Double addVal  = (Double)entryToAdd._values[dp];
//
////				System.out.println("colName='"+ce._colName+"', dp='"+dp+"', sqlColNum='"+ce._sqlColNum+"', thisVal='"+thisVal+"',addValue='"+addVal+"'.");
//				Double newVal  = new Double( thisVal + addVal );
//				_values[dp] = newVal;
//			}
//		}
//	}
//
//	public void XXXcalcAverage()
//	{
//		for (HostMonitorMetaData.ColumnEntry ce : _md.getColumns())
//		{
//			int dp = ce._sqlColNum - 1;
//
//			// if ((ce._status | HostMonitorMetaData.STATUS_COL_SUB_SAMPLE) == HostMonitorMetaData.STATUS_COL_SUB_SAMPLE)
//			if (ce._status == HostMonitorMetaData.STATUS_COL_SUB_SAMPLE)
//				_values[dp] = _md.newObject(ce, Integer.toString(_numOfSamples));
//
//			if (_numOfSamples == 0)
//				continue;
//
//			if (ce._isStatColumn)
//				_values[dp] = new Double( (Double)_values[dp] / _numOfSamples );
//
//		}
//	}
}
