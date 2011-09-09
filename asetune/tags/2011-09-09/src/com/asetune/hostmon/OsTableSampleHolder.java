package com.asetune.hostmon;

import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * This class that holds several OsTable instances<br>
 * The objective is that we should sample several OsTable instances (of the same type or MetaData)
 * and at a later do summary over several OsTable instances
 * <p>
 * In one example it's used by the HostMonitor object, that executes an Operating System Command
 * which receives streams records, parses the records to create Rows which will be put in a Table.<br>
 * At any later stage another thread or object is requesting data from this OsTableSampleHolder.getTableSummary(), 
 * which now calculates a summary or average over all the individual sampled instances of OsTable.
 * <p>
 * This type of functionally is used when you have a "client" or "request" process that request information
 * with a non regular sample time (which could be 5 seconds, but next time it will be 8, etc). 
 * While the underlying structure has a more fixed period for sampled values (for example every 2 seconds)
 * 
 * @author gorans
 */
public class OsTableSampleHolder
{
	private HostMonitorMetaData           _metaData = null;
	private ArrayList<OsTable>            _samples  = new ArrayList<OsTable>();
	private LinkedHashMap<String, String> _pkMap    = new LinkedHashMap<String, String>();
	
	public OsTableSampleHolder(HostMonitorMetaData metaData)
	{
		_metaData = metaData;
	}

	/**
	 * If the MetaData was unknown/null when creating the object, then we have a chance to set it here. 
	 * @param metaData
	 */
	public void setMetaData(HostMonitorMetaData metaData)
	{
		_metaData = metaData;
	}


	/**
	 * Add a OsTable sample entry to the holder
	 * @param sample
	 * NOTE: this must be synchronized with the method getTableSummary(), which empties the sample queue
	 */
	synchronized public void add(OsTable sample)
	{
		int maxQueueSize = _metaData.getMaxQueueSize();
		
		// Remove last entry in the sample table if queue size is exhausted
		if (_samples.size() > maxQueueSize)
		{
			int lastRow = _samples.size() - 1;
			//OsTable ost = _samples.get(lastRow);
			
			// remove the last entry
			_samples.remove(lastRow);

			// Remove it from the PK Map???
			// NOTE: But we need to loop all PK entries in the "other" samples to check if we can remove it or not... 
			// TODO: should we or shouldn't we???, lets do this in "next" release if there are problems with keeping the PK...
		}

		// add all PK (device names) to a map if they arn't there
		// the Map will be used by doing summary/avg calculation.
		for (OsTableRow entry : sample.getPkMap().values())
		{
			String pk = entry.getPk();
			if ( ! _pkMap.containsKey(pk))
				_pkMap.put(pk, pk);
		}
		_samples.add(sample);
	}

	/**
	 * Create a OsTable that is a reflection of all the added records to the SummaryHolder<br>
	 * At the end the "sample queue" will be cleared.
	 * @return if no rows where added, an empty OsTable will be returned (same as a "empty" result set)
	 * NOTE: this must be synchronized with the method add()
	 */
	synchronized public OsTable getTableSummary()
	{
		if (_samples.isEmpty())
			return new OsTable(_metaData);

		OsTable first = _samples.get(0);
		OsTable last  = _samples.get(_samples.size()-1);

		long sampleSpan = last.getTime() - first.getTime();

		OsTable outputTable = new OsTable(_metaData);
		outputTable.setSampleSpanTime(sampleSpan);

		if (_pkMap.size() > 0)
		{
			// Loop all Entries and do summary.
			// loop the device Map first - then loop all entries per device and do summary
			for (String pk : _pkMap.keySet())
			{
				OsTableRowSummary sumEntry = new OsTableRowSummary(_metaData, pk);
				for (OsTable tSample : _samples)
				{
					OsTableRow entry = tSample.getRowByPk(pk);
					if (entry != null)
					{
						sumEntry.addToSummary(entry);
					}
				}
	
				// Calculate average...
				sumEntry.calcAverage();
				outputTable.addRow(sumEntry);
			}
		}
		else
		{
			// FIXME: check with the MetaData if it's a "DELIVER_ALL_ROWS", which hasn't been implemented yet.
//			System.err.println("NO PK, SO LETS ADD ALL ROWS to the output table");
			for (OsTable tSample : _samples)
			{
				//System.out.println("sample.getRowCount()="+sample.getRowCount()+", sample="+sample);
				for (int r=0; r<tSample.getRowCount(); r++)
				{
					//System.out.println("r="+r+", sample.getRow(r)="+sample.getRow(r));
					outputTable.addRow(tSample.getRow(r));
				}
			}
		}

		// Clear the old records
		_samples.clear();

		// return the summary
		return outputTable;
	}
}
