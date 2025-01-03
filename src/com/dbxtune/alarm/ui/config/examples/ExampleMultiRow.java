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
package com.dbxtune.alarm.ui.config.examples;

import java.lang.invoke.MethodHandles;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.alarm.AlarmHandler;
import com.dbxtune.alarm.IUserDefinedAlarmInterrogator;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.alarm.events.AlarmEventFullTranLog;
import com.dbxtune.alarm.events.AlarmEventLongRunningTransaction;
import com.dbxtune.cm.CountersModel;

public class ExampleMultiRow
implements IUserDefinedAlarmInterrogator
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

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
						AlarmEvent alarm = new AlarmEventLongRunningTransaction(cm, threshold, dbname, OldestTranInSeconds, OldestTranName);
						AlarmHandler.getInstance().addAlarm(alarm);
					}
				}
			}

			//-------------------------------------------------------
			// Full transaction log
			//-------------------------------------------------------
			Double TransactionLogFull = cm.getRateValueAsDouble(r, "TransactionLogFull");
			if (TransactionLogFull != null)
			{
				_logger.debug("##### sendAlarmRequest("+cm.getName()+"): dbname='"+dbname+"', TransactionLogFull='"+TransactionLogFull+"'.");
				if (AlarmHandler.hasInstance())
				{
					int threshold = 0;
					if (TransactionLogFull.intValue() > threshold)
						AlarmHandler.getInstance().addAlarm( new AlarmEventFullTranLog(cm, threshold, dbname, null) );
				}
			}
		}
	}
}
