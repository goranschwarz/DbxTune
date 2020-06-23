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
package com.asetune.alarm.ui.config.examples;

import java.util.Calendar;
import java.util.Date;

import org.apache.log4j.Logger;

import com.asetune.Version;
import com.asetune.alarm.AlarmHandler;
import com.asetune.alarm.IUserDefinedAlarmInterrogator;
import com.asetune.alarm.events.AlarmEvent;
import com.asetune.cm.CountersModel;
import com.asetune.utils.Configuration;
import com.asetune.utils.Ver;

public class ExampleAdvanced
implements IUserDefinedAlarmInterrogator
{
	private static Logger _logger = Logger.getLogger(ExampleAdvanced.class);

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

		// if Database Version is NOT what you expect; exit
		//    Ver.ver(12,5,4, 10)  == Version 12.5.4 ESD#10
		//    Ver.ver(15,5,0, 2)   == Version 15.5.0 ESD#2
		//    Ver.ver(15,7,0, 112) == Version 15.7.0 SP112
		//    Ver.ver(16,0,0, 1)   == Version 16.0.0 SP1
		//    Ver.ver(16,0,0, 2,5) == Version 16.0.0 SP2 PL5
		if (cm.getServerVersion() < Ver.ver(12,5,1))
			return;

		// If the sample time is between 23:00 en the evening and 08:00 in the morning; exit
		// Note: the below can probably be made simpler
	    int fromTime = 2300;
	    int toTime   = 800;
	    Date date = new Date(cm.getSampleTimeHead().getTime()); //Set the Calendar to current sample time
	    Calendar c = Calendar.getInstance();
	    c.setTime(date);
	    int t = c.get(Calendar.HOUR_OF_DAY) * 100 + c.get(Calendar.MINUTE);
	    boolean isBetween = toTime > fromTime && t >= fromTime && t <= toTime || toTime < fromTime && (t >= fromTime || t <= toTime);
	    if (isBetween)
	    	return;
		
		// If we havn't got all desired column names; exit
		String[] desiredCols = {"DBName", "LogSizeFreeInMb", "LogSizeUsedPct", "DataSizeFreeInMb", "DataSizeUsedPct"};
		if ( ! cm.hasColumns(desiredCols) )
		{
			_logger.warn("Not all desired column names was available in cm '"+cm.getName()+"'. Missing columns: " + cm.getMissingColumns(desiredCols));
			return;
		}
		
		// Set treshold values for various columns
		int logSizeThreshold = 100; // 100 MB
		int dbSizeThreshold  = 100; // 100 MB

		// Database names are not the same in all server
		String desiredDBName = "mxg";

		// For different servers do different things, like
		// - different threshold values
		// - Simply do not continue for server name 'SOME_NAME'
		String serverName = cm.getServerName();
		if ("PROD_A_DS".equals(serverName))
		{
			desiredDBName = "mxg_prod";
			logSizeThreshold =  5 * 1024; //  5 GB
			dbSizeThreshold  = 20 * 1024; // 20 GB
		}
		else if ("TEST_DS".equals(serverName))
		{
			desiredDBName = "mxg_test";
			logSizeThreshold =  2 * 1024; //  2 GB
			dbSizeThreshold  = 10 * 1024; // 10 GB
		}
		else if ("SYS_DS".equals(serverName))
		{
			// Get thresholds from the configuartion
			logSizeThreshold =  Configuration.getCombinedConfiguration().getIntProperty("SYS_DS.logSizeThresholdInMb", 1024); // 1GB
			dbSizeThreshold  =  Configuration.getCombinedConfiguration().getIntProperty("SYS_DS.dbSizeThreshold",      2048); // 2GB
		}
		else if ("DEV_DS".equals(serverName))
		{
			// Skip alarm generation for this server
			return;
		}
		
		//-----------------------------------
		// Loop all RATE Rows
		//-----------------------------------
		for (int r=0; r<cm.getRateRowCount(); r++)
		{
			String dbname = cm.getRateString(r, "DBName");

			// Only check the desired database
			if (dbname.equalsIgnoreCase(desiredDBName))
			{
				//-------------------------------------------------------
				// Log size used
				//-------------------------------------------------------
				Double LogSizeFreeInMb = cm.getAbsValueAsDouble(r, "LogSizeFreeInMb");
				Double LogSizeUsedPct  = cm.getAbsValueAsDouble(r, "LogSizeUsedPct");
				if (LogSizeFreeInMb != null)
				{
					if (LogSizeFreeInMb.intValue() > logSizeThreshold)
					{
						AlarmHandler.getInstance().addAlarm( new AlarmEventLogSpaceUsage(cm, logSizeThreshold, dbname, LogSizeFreeInMb, LogSizeUsedPct) );
					}
				}

				//-------------------------------------------------------
				// Log size used
				//-------------------------------------------------------
				Double DataSizeFreeInMb = cm.getAbsValueAsDouble(r, "DataSizeFreeInMb");
				Double DataSizeUsedPct  = cm.getAbsValueAsDouble(r, "DataSizeUsedPct");
				if (DataSizeFreeInMb != null)
				{
					if (DataSizeFreeInMb.intValue() > dbSizeThreshold)
					{
						AlarmHandler.getInstance().addAlarm( new AlarmEventDataSpaceUsage(cm, dbSizeThreshold, dbname, DataSizeFreeInMb, DataSizeUsedPct) );
					}
				}
			} // end: if desiredDBName
		} // end: loop for row
	} // end: method
	
	/**
	 * This is a local definition of an AlarmEvent, called 'LogSpaceUsage' <br>
	 * You can here it here in the file if it's not going to be reused by any other interrorators <br>
	 * Or create a file AlarmEventLogSpaceUsage.java in ${DBXTUNE_HOME}/resources/alarm-handler-src/asetune/alarms/ <br>
	 * so you can reuse the AlarmEventLogSpaceUsage in any other User Defined Alarms <br>
	 */
	public class AlarmEventLogSpaceUsage
	extends AlarmEvent
	{
		private static final long serialVersionUID = 1L;

		public AlarmEventLogSpaceUsage(CountersModel cm, Number threshold, String dbname, Number freeInMb, Number usedPct)
		{
			super(
					Version.getAppName(), // serviceType
					cm.getServerName(),   // serviceName
					cm.getName(),         // serviceInfo
					dbname,               // extraInfo
					AlarmEvent.Category.SPACE, 
					AlarmEvent.Severity.WARNING, 
					AlarmEvent.ServiceState.UP, 
					"Found That LOG Space usage is low in '" + cm.getServerName() + "', dbname='" + dbname +"'. freeInMb=" + freeInMb + ", usedPct='"+usedPct+"'. (threshold="+threshold+")",
					threshold);

			// Set: Time To Live if postpone is enabled
			setTimeToLive(cm);

			// Set the raw data carier: current cpu usage
			setData(freeInMb);
		}
	} // end: class

	public class AlarmEventDataSpaceUsage
	extends AlarmEvent
	{
		private static final long serialVersionUID = 1L;

		public AlarmEventDataSpaceUsage(CountersModel cm, Number threshold, String dbname, Number freeInMb, Number usedPct)
		{
			super(
					Version.getAppName(), // serviceType
					cm.getServerName(),   // serviceName
					cm.getName(),         // serviceInfo
					dbname,               // extraInfo
					AlarmEvent.Category.SPACE, 
					AlarmEvent.Severity.WARNING, 
					AlarmEvent.ServiceState.UP, 
					"Found That DATA Space usage is low in '" + cm.getServerName() + "', dbname='" + dbname +"'. freeInMb=" + freeInMb + ", usedPct='"+usedPct+"'. (threshold="+threshold+")",
					threshold);

			// Set: Time To Live if postpone is enabled
			setTimeToLive(cm);

			// Set the raw data carier: current cpu usage
			setData(freeInMb);
		}
	} // end: class

} // end: class
