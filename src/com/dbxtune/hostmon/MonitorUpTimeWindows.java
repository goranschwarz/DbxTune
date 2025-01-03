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
package com.dbxtune.hostmon;

import java.lang.invoke.MethodHandles;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.hostmon.HostMonitorMetaData.ColumnEntry;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.NumberUtils;


public class MonitorUpTimeWindows
extends MonitorUpTime
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public MonitorUpTimeWindows()
	{
		this(-1, null);
	}
	public MonitorUpTimeWindows(int utilVersion, String utilExtraInfo)
	{
		super(utilVersion, utilExtraInfo);
	}

	@Override
	public String getModuleName()
	{
		return this.getClass().getSimpleName();
	}

	@Override
	public String getCommand()
	{
		String cmd = super.getCommand();
		return cmd != null ? cmd : "typeperf -si " + getSleepTime() + " \"\\System\\*\" ";
	}

	@Override
	public HostMonitorMetaData createMetaData(int utilVersion, Configuration utilExtraInfo)
	{
		// Special MetaData for Perf Counters...
		HostMonitorMetaData md = new HostMonitorMetaDataWindowsTypePerf()
		{
			@Override
			public void addUserDefinedCountersInInitializeUsingFirstRow()
			{
				//int arrPosCount = getParseColumnCount();
				int colSqlPos = getColumnCount();
				colSqlPos++;
				
				addIntColumn( "nproc",                 colSqlPos++,  0, true,        "Number of cores on this machine.");

				addDecColumn( "loadAverage_1Min",      colSqlPos++,  0, true, 16, 5, "Load Average (Running Average of 'Processor Queue Length' for 1 minute)");
				addDecColumn( "loadAverage_5Min",      colSqlPos++,  0, true, 16, 5, "Load Average (Running Average of 'Processor Queue Length' for 5 minute)");
				addDecColumn( "loadAverage_15Min",     colSqlPos++,  0, true, 16, 5, "Load Average (Running Average of 'Processor Queue Length' for 15 minute)");

				addDecColumn( "adjLoadAverage_1Min",   colSqlPos++,  0, true, 16, 5, "Adjusted Load Average. Formula: loadAvg_1m / cores");
				addDecColumn( "adjLoadAverage_5Min",   colSqlPos++,  0, true, 16, 5, "Adjusted Load Average. Formula: loadAvg_5m / cores");
				addDecColumn( "adjLoadAverage_15Min",  colSqlPos++,  0, true, 16, 5, "Adjusted Load Average. Formula: loadAvg_15m / cores");
			}
		};

		md.setTableName(getModuleName());

		// It's a *streaming* command
		md.setOsCommandStreaming(true);

		// The initialization on column names etc are done in of first row read, which holds all column names, its a CSV header
		md.setInitializeUsingFirstRow(true);

		return md;
	}

	/**
	 * Called from CmOsUptime.localCalculation(OsTable osSampleTable)
	 * 
	 * @param osSampleTable
	 */
	public void setLoadAverageColumns(OsTable osSampleTable)
	{
		if (osSampleTable == null)
			return;

		int pos_nproc                = osSampleTable.findColumn("nproc");

		int pos_loadAverage_1Min     = osSampleTable.findColumn("loadAverage_1Min");
		int pos_loadAverage_5Min     = osSampleTable.findColumn("loadAverage_5Min");
		int pos_loadAverage_15Min    = osSampleTable.findColumn("loadAverage_15Min");
		
		int pos_adjLoadAverage_1Min  = osSampleTable.findColumn("adjLoadAverage_1Min");
		int pos_adjLoadAverage_5Min  = osSampleTable.findColumn("adjLoadAverage_5Min");
		int pos_adjLoadAverage_15Min = osSampleTable.findColumn("adjLoadAverage_15Min");
		
		if (pos_nproc == -1) 
		{
			_logger.error("Missing column 'nproc'=" + pos_nproc + ". Cant continue.");
			return;
		}
		if (pos_loadAverage_1Min == -1 || pos_loadAverage_5Min == -1 || pos_loadAverage_15Min == -1) 
		{
			_logger.error("Missing column 'loadAverage_1Min'=" + pos_loadAverage_1Min + ", 'loadAverage_5Min'=" + pos_loadAverage_5Min + " or 'loadAverage_15Min'=" + pos_loadAverage_15Min + ". Cant continue.");
			return;
		}
		if (pos_adjLoadAverage_1Min == -1 || pos_adjLoadAverage_5Min == -1 || pos_adjLoadAverage_15Min == -1) 
		{
			_logger.error("Missing column 'adjLoadAverage_1Min'=" + pos_adjLoadAverage_1Min + ", 'adjLoadAverage_5Min'=" + pos_adjLoadAverage_5Min + " or 'adjLoadAverage_15Min'=" + pos_adjLoadAverage_15Min + ". Cant continue.");
			return;
		}

		int nproc = getConnection().getOsCoreCount();
		
//		double load_avg_1m      = NumberUtils.round(_s_load_avg_1m , 2);
//		double load_avg_5m      = NumberUtils.round(_s_load_avg_5m , 2);
//		double load_avg_15m     = NumberUtils.round(_s_load_avg_15m, 2);
		double load_avg_1m      = NumberUtils.round(_s5_load_avg_1m , 2);
		double load_avg_5m      = NumberUtils.round(_s5_load_avg_5m , 2);
		double load_avg_15m     = NumberUtils.round(_s5_load_avg_15m, 2);

		double adj_load_avg_1m  = NumberUtils.round(load_avg_1m  / nproc, 2);
		double adj_load_avg_5m  = NumberUtils.round(load_avg_5m  / nproc, 2);
		double adj_load_avg_15m = NumberUtils.round(load_avg_15m / nproc, 2);

		// Set "nproc"
		osSampleTable.setValueAt(nproc, 0, pos_nproc);
		
		// Set "load average"
		osSampleTable.setValueAt(load_avg_1m     , 0, pos_loadAverage_1Min);
		osSampleTable.setValueAt(load_avg_5m     , 0, pos_loadAverage_5Min);
		osSampleTable.setValueAt(load_avg_15m    , 0, pos_loadAverage_15Min);
		
		// Set "adjusted load average"
		osSampleTable.setValueAt(adj_load_avg_1m , 0, pos_adjLoadAverage_1Min);
		osSampleTable.setValueAt(adj_load_avg_5m , 0, pos_adjLoadAverage_5Min);
		osSampleTable.setValueAt(adj_load_avg_15m, 0, pos_adjLoadAverage_15Min);
	}
	
    //----------------------------------------------------------------------------------------
    // implement "exponentially-damped moving averages" (uptime) 1, 5, 15 Minute load average
    //----------------------------------------------------------------------------------------
    // https://en.wikipedia.org/wiki/Load_(computing)
    // https://github.com/giampaolo/psutil/blob/51eb1dae7bf96dcc7dae51641d5770fd0d99d0ac/psutil/arch/windows/wmi.c
	//
	// http://www.brendangregg.com/blog/2017-08-08/linux-load-averages.html
	// https://www.helpsystems.com/resources/guides/unix-load-average-part-1-how-it-works
	// https://www.helpsystems.com/resources/guides/unix-load-average-part-2-not-your-average-average
	// etc...
    //----------------------------------------------------------------------------------------
	@Override
	public void addRowForCurrentSubSampleHookin(OsTableRow entry)
	{
//System.out.println(">> "+getModuleName()+": addRowForCurrentSubSampleHookin()");

		for (ColumnEntry ce : entry._md.getColumns())
		{
			if ("Processor Queue Length".equals(ce._colName))
			{
				int arrPos = ce._parseColNum;
				Object currentLoad_o = entry._values[arrPos];
				if (currentLoad_o instanceof Number)
				{
					double currentLoad = ((Number)currentLoad_o).doubleValue();

//					System.out.println("-- "+getModuleName()+": BEFORE: currentLoad="+currentLoad+". 1m="+_load_avg_1m+", 5m="+_load_avg_5m+", 15m="+_load_avg_15m);

					_s_load_avg_1m  = _s_load_avg_1m  * LOADAVG_FACTOR_1F  + currentLoad * (1.0 - LOADAVG_FACTOR_1F);
					_s_load_avg_5m  = _s_load_avg_5m  * LOADAVG_FACTOR_5F  + currentLoad * (1.0 - LOADAVG_FACTOR_5F);
					_s_load_avg_15m = _s_load_avg_15m * LOADAVG_FACTOR_15F + currentLoad * (1.0 - LOADAVG_FACTOR_15F);

					_s5_sampleCount++;
					_s5_currentLoadSum += currentLoad;

					long msTimeDiff = System.currentTimeMillis() - _s5_lastCalcTime;
					if (msTimeDiff >= 5_000)
					{
						double s5_currentLoad = _s5_currentLoadSum / _s5_sampleCount;

//						System.out.println("-- do: 5 sec calc ... _s5_currentLoadSum=" + _s5_currentLoadSum + ", _s5_sampleCount=" + _s5_sampleCount + ", s5_currentLoad=" + s5_currentLoad);

						_s5_load_avg_1m  = _s5_load_avg_1m  * LOADAVG_FACTOR_1F  + s5_currentLoad * (1.0 - LOADAVG_FACTOR_1F);
						_s5_load_avg_5m  = _s5_load_avg_5m  * LOADAVG_FACTOR_5F  + s5_currentLoad * (1.0 - LOADAVG_FACTOR_5F);
						_s5_load_avg_15m = _s5_load_avg_15m * LOADAVG_FACTOR_15F + s5_currentLoad * (1.0 - LOADAVG_FACTOR_15F);

						_s5_lastCalcTime   = System.currentTimeMillis();
						_s5_sampleCount    = 0;
						_s5_currentLoadSum = 0d;
					}
					
//					calcLoadAverage(currentLoad);
					
					if (_logger.isDebugEnabled())
					{
						_logger.debug(getModuleName()+": - AFTER:  currentLoad="+currentLoad+". "
								+   "1m=[s:"  + NumberUtils.round(_s_load_avg_1m,  2) + ", s5:" + NumberUtils.round(_s5_load_avg_1m,  2) + "]"
								+ ", 5m=[s:"  + NumberUtils.round(_s_load_avg_5m,  2) + ", s5:" + NumberUtils.round(_s5_load_avg_1m,  2) + "]"
								+ ", 15m=[s:" + NumberUtils.round(_s_load_avg_15m, 2) + ", s5:" + NumberUtils.round(_s5_load_avg_1m,  2) + "]"
								);
					}
				}
				break;
			}
		}
	}
	private long   _s5_lastCalcTime = System.currentTimeMillis();
	private int    _s5_sampleCount = 0;
	private double _s5_currentLoadSum = 0;

	//
	// These constants serve as the damping factor and are calculated with
	// 1 / exp(sampling interval in seconds / window size in seconds)
	//
	// This formula comes from linux's include/linux/sched/loadavg.h
	// https://github.com/torvalds/linux/blob/345671ea0f9258f410eb057b9ced9cefbbe5dc78/include/linux/sched/loadavg.h#L20-L23
	//
	private static final double LOADAVG_FACTOR_1F  = 0.9200444146293232478931553241;
	private static final double LOADAVG_FACTOR_5F  = 0.9834714538216174894737477501;
	private static final double LOADAVG_FACTOR_15F = 0.9944598480048967508795473394;

	// The time interval in seconds between taking load counts, same as Linux
//	private static final double SAMPLING_INTERVAL = 5; // For this implementation 5 is NOT the value... it will differ

	double _s_load_avg_1m  = 0; // calculate on every sample
	double _s_load_avg_5m  = 0; // calculate on every sample
	double _s_load_avg_15m = 0; // calculate on every sample

	double _s5_load_avg_1m  = 0; // calculate over 5 seconds
	double _s5_load_avg_5m  = 0; // calculate over 5 seconds
	double _s5_load_avg_15m = 0; // calculate over 5 seconds

	@Override
	public int getSleepTime()
	{
		return 1;
	}

	
	
	//--------------------------------------------------------------------------------------
	// Below are code for: "normal" Moving average based on 
	// https://en.wikipedia.org/wiki/Moving_average
	//--------------------------------------------------------------------------------------

//	private double  _osLoadAverage1min  = -1;
//	private double  _osLoadAverage5min  = -1;
//	private double  _osLoadAverage15min = -1;
//	private double  _osLoadAverage30min = -1;
//	private double  _osLoadAverage60min = -1;
//	private double  _osLoadAverageAdjusted1min  = -1;
//	private double  _osLoadAverageAdjusted5min  = -1;
//	private double  _osLoadAverageAdjusted15min = -1;
//	private double  _osLoadAverageAdjusted30min = -1;
//	private double  _osLoadAverageAdjusted60min = -1;
//	private LinkedList<OsLoadAvgEntry> _osLoadAverageList = null;
//
//	private synchronized void calcLoadAverage(double currentLoad)
//	{
//		// Get ONE minute Load Average From OS (if the OS isn't supporting this, -1 will be returned)
////		OperatingSystemMXBean os  = ManagementFactory.getOperatingSystemMXBean();
////		double osLoadAverage1min  = os.getSystemLoadAverage();
////		double numOfProcs         = os.getAvailableProcessors() * 1.0; // * 1.0 to convert it to a double
////		double numOfProcs         = 2 * 1.0; // * 1.0 to convert it to a double
//		double numOfProcs         = getConnection().getNproc() * 1.0; // * 1.0 to convert it to a double
//		
//
//		long now = System.currentTimeMillis();
//		
//		if (_osLoadAverageList == null)
//			_osLoadAverageList = new LinkedList<>();
//
//		// Calculate 1, 5, 15, 30 and 60 minutes into milliseconds
//		long   ms1m   = 60_000; // 60 * 1000;
//		int    cnt1m  = 0;
//		double sum1m  = 0;
//
//		long   ms5m   = 300_000; // 5 * 60 * 1000;
//		int    cnt5m  = 0;
//		double sum5m  = 0;
//
//		long   ms15m  = 900_000; // 15 * 60 * 1000;
//		int    cnt15m = 0;
//		double sum15m = 0;
//
//		long   ms30m  = 1_800_000; // 30 * 60 * 1000;
//		int    cnt30m = 0;
//		double sum30m = 0;
//
//		long   ms60m  = 3_600_000; // 60 * 60 * 1000;
//		int    cnt60m = 0;
//		double sum60m = 0;
//
//		// discard entries from the LoadAverage LIST after this number of milliseconds 
//		long maxAge = ms60m;
//		
//		
//		// Add current entry to the list
//		_osLoadAverageList.add(new OsLoadAvgEntry(now, currentLoad));
//
//		//
//		// Get oldest entry so we can check if we have enough time/samples to calculate 5, 15, 30 and 60 minute values 
//		//
//		// 60, 30, 15 and 5 minute would typically be HIGH at start time (since it's the same value as 1 minute)... 
//		// So we might want to "skip" those until we have enough data point to measure (or similar)
//		// Otherwise we will have *ALARMS* that are "faulty".
//		OsLoadAvgEntry oldestEntry = _osLoadAverageList.getFirst(); // note: new entries are added at "END", so oldest entry would be FIRST
//		long oldestAgeInMs = -1;
//		if (oldestEntry != null)
//			oldestAgeInMs = now - oldestEntry._ts;
//
//		// Loop the list and remove old entries
//		// and calculate the Average for 1, 5, 15, 30 and 60 minutes
//		for(Iterator<OsLoadAvgEntry> i = _osLoadAverageList.iterator(); i.hasNext();) 
//		{
//			OsLoadAvgEntry entry = i.next();
//			long   ts  = entry._ts;
//			double val = entry._val;
//
//			long entryAgeMs = now - ts;
//			
//			// Remove entries that are older than 60 minutes (or the MAX interval to save)
//			// and get next entry (start at the top)
//			if ( entryAgeMs > maxAge )
//			{
//				i.remove();
//				continue;
//			}
//			
//			// 60 minute average  (but only add values if we have enough samples... in this case approx 30 minutes)
//			if ( (entryAgeMs <= ms60m) && (oldestAgeInMs > ms60m / 2) )
//			{
//				cnt60m++;
//				sum60m += val;
//			}
//
//			// 30 minute average  (but only add values if we have enough samples... in this case approx 15 minutes)
//			if ( (entryAgeMs <= ms30m) && (oldestAgeInMs > ms30m / 2) )
//			{
//				cnt30m++;
//				sum30m += val;
//			}
//
//			// 15 minute average  (but only add values if we have enough samples... in this case approx 7.5 minutes)
//			if ( (entryAgeMs <= ms15m) && (oldestAgeInMs > ms15m / 2) )
//			{
//				cnt15m++;
//				sum15m += val;
//			}
//
//			// 5 minute average (but only add values if we have enough samples... in this case approx 2.5 minutes)
//			if ( (entryAgeMs <= ms5m) && (oldestAgeInMs > ms5m / 2) )
//			{
//				cnt5m++;
//				sum5m += val;
//			}
//
//			// 1 minute average (but only add values if we have enough samples... in this case approx 30 sec)
//			if ( (entryAgeMs <= ms1m) && (oldestAgeInMs > ms1m / 2) )
//			{
//				cnt1m++;
//				sum1m += val;
//			}
//		}
//		
//		// on: 5,15,30,60 minute: calculate and set scale to 2
////		_osLoadAverage1min  = osLoadAverage1min;
//		_osLoadAverage1min  = cnt1m  == 0 ? -1 : new BigDecimal( sum1m  / (cnt1m  * 1.0) ).setScale(2, BigDecimal.ROUND_HALF_EVEN).doubleValue();
//		_osLoadAverage5min  = cnt5m  == 0 ? -1 : new BigDecimal( sum5m  / (cnt5m  * 1.0) ).setScale(2, BigDecimal.ROUND_HALF_EVEN).doubleValue();
//		_osLoadAverage15min = cnt15m == 0 ? -1 : new BigDecimal( sum15m / (cnt15m * 1.0) ).setScale(2, BigDecimal.ROUND_HALF_EVEN).doubleValue();
//		_osLoadAverage30min = cnt30m == 0 ? -1 : new BigDecimal( sum30m / (cnt30m * 1.0) ).setScale(2, BigDecimal.ROUND_HALF_EVEN).doubleValue();
//		_osLoadAverage60min = cnt60m == 0 ? -1 : new BigDecimal( sum60m / (cnt60m * 1.0) ).setScale(2, BigDecimal.ROUND_HALF_EVEN).doubleValue();
//
////		_osLoadAverageAdjusted1min  =                    new BigDecimal( osLoadAverage1min / numOfProcs         ).setScale(2, BigDecimal.ROUND_HALF_EVEN).doubleValue();;
//		_osLoadAverageAdjusted1min  = cnt1m  == 0 ? -1 : new BigDecimal( (sum1m  / (cnt1m  * 1.0)) / numOfProcs ).setScale(2, BigDecimal.ROUND_HALF_EVEN).doubleValue();
//		_osLoadAverageAdjusted5min  = cnt5m  == 0 ? -1 : new BigDecimal( (sum5m  / (cnt5m  * 1.0)) / numOfProcs ).setScale(2, BigDecimal.ROUND_HALF_EVEN).doubleValue();
//		_osLoadAverageAdjusted15min = cnt15m == 0 ? -1 : new BigDecimal( (sum15m / (cnt15m * 1.0)) / numOfProcs ).setScale(2, BigDecimal.ROUND_HALF_EVEN).doubleValue();
//		_osLoadAverageAdjusted30min = cnt30m == 0 ? -1 : new BigDecimal( (sum30m / (cnt30m * 1.0)) / numOfProcs ).setScale(2, BigDecimal.ROUND_HALF_EVEN).doubleValue();
//		_osLoadAverageAdjusted60min = cnt60m == 0 ? -1 : new BigDecimal( (sum60m / (cnt60m * 1.0)) / numOfProcs ).setScale(2, BigDecimal.ROUND_HALF_EVEN).doubleValue();
//	}
//
//	/** Small Class to keep OsLoadAverage history values... so we can calculate 5 and 15 minute values */
//	private static class OsLoadAvgEntry
//	{
//		long   _ts;
//		double _val;
//		public OsLoadAvgEntry(long ts, double val)
//		{
//			_ts  = ts;
//			_val = val;
//		}
//	}
}
