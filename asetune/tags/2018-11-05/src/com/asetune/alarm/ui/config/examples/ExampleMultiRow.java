package com.asetune.alarm.ui.config.examples;

import org.apache.log4j.Logger;

import com.asetune.alarm.AlarmHandler;
import com.asetune.alarm.IUserDefinedAlarmInterrogator;
import com.asetune.alarm.events.AlarmEventFullTranLog;
import com.asetune.alarm.events.AlarmEventLongRunningTransaction;
import com.asetune.cm.CountersModel;

public class ExampleMultiRow
implements IUserDefinedAlarmInterrogator
{
	private static Logger _logger = Logger.getLogger(ExampleMultiRow.class);

	/**
	 * Check values for this CM (Counter Model) and generate any desired alarms
	 */
	@Override
	public void interrogateCounterData(CountersModel cm)
	{
		// No RATE data, get out of here (on first sample we will only have ABS data)
		if ( ! cm.hasRateData() )
			return;

		// If we havn't got any alarm handler; exit
		if ( ! AlarmHandler.hasInstance() )
			return;

		// If we havn't got all desired column names; exit
		String[] desiredCols = {"DBName", "OldestTranInSeconds", "OldestTranName", "TransactionLogFull"};
		if ( ! cm.hasColumns(desiredCols) )
		{
			_logger.warn("Not all desired column names was available in cm '"+cm.getName()+"'. Missing columns: " + cm.getMissingColumns(desiredCols));
			return;
		}
		
		// Loop all RATE Rows
		for (int r=0; r<cm.getRateRowCount(); r++)
		{
			String dbname = cm.getRateString(r, "DBName");

			//-------------------------------------------------------
			// Long running transaction
			//-------------------------------------------------------
			Double OldestTranInSeconds = cm.getAbsValueAsDouble(r, "OldestTranInSeconds");
			if (OldestTranInSeconds != null)
			{
				int threshold = 10;
				if (OldestTranInSeconds.intValue() > threshold)
				{
					String OldestTranName = cm.getRateString(r, "OldestTranName");

					// If it's a DUMP DATABASE or DUMP TRANSACTION, do not alarm...
					if (OldestTranName != null && !OldestTranName.startsWith("DUMP "))
					{
						AlarmHandler.getInstance().addAlarm( new AlarmEventLongRunningTransaction(cm, threshold, dbname, OldestTranInSeconds, OldestTranName) );
					}
				}
			}

			//-------------------------------------------------------
			// Full transaction log
			//-------------------------------------------------------
			Double TransactionLogFull = cm.getRateValueAsDouble(r, "TransactionLogFull");
			if (TransactionLogFull != null)
			{
				System.out.println("##### sendAlarmRequest("+cm.getName()+"): dbname='"+dbname+"', TransactionLogFull='"+TransactionLogFull+"'.");
				if (AlarmHandler.hasInstance())
				{
					int threshold = 0;
					if (TransactionLogFull.intValue() > threshold)
						AlarmHandler.getInstance().addAlarm( new AlarmEventFullTranLog(cm, threshold, dbname) );
				}
			}
		}
	}
}
