/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
 * 
 * This file is part of DbxTune
 * DbxTune is a family of sub-products *Tune, hence the Dbx
 * Here are some of the tools: AseTune, IqTune, RsTune, RaxTune, HanaTune, 
 *          SqlServerTune, PostgresTune, MySqlTune, MariaDbTune, Db2Tune, ...
 * 
 * DbxTune is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.asetune.graph;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import com.asetune.graph.TrendGraphDataPoint.Category;
import com.asetune.graph.TrendGraphDataPoint.LabelType;

/**
 * This holds DATA for each data serie in this chart 
 * <ul>
 *    <li>One LinkedList contains all "timestamps"<li>
 *    <li>One LinkedHashMap holds each of the data serie<li>
 *    <li>For each serie, a LinkedList holds DatPoints(a Double)<li>
 * </ul>
 * Data points are removed when "to old" 
 *  
 * @author goran
 *
 */
public class ChartDataHistorySeries
{
	private static Logger _logger = Logger.getLogger(ChartDataHistorySeries.class);

	private String _name;
	private int    _minutes;
	
	private boolean _isPeriodAchieved;

//	private LinkedList<Entry> _values = new LinkedList<>();

	/** Every "serie/label" will have a List with Entries */
//	private LinkedHashMap<String, LinkedList<Entry>> _series = new LinkedHashMap<>();
	private LinkedHashMap<String, LinkedList<Double>> _series = new LinkedHashMap<>();

	/** */
	private LinkedList<Long> _timeList = new LinkedList<>();

	private static final Double FILL_VALUE = new Double(0d);

	public static final int DEFAULT_KEEP_AGE_IN_MINUTES = 60; // 60 minutes

	/**
	 * 
	 * @param name
	 * @throws RuntimeException
	 */
	public ChartDataHistorySeries(String name)
	throws RuntimeException
	{
		this(name, -1);
	}

	/**
	 * 
	 * @param name
	 * @param minutes             Negative value means the default
	 * @throws RuntimeException
	 */
	public ChartDataHistorySeries(String name, int minutes)
	throws RuntimeException
	{
		if (name == null)   throw new RuntimeException("Name can't be null");
		if (name.isEmpty()) throw new RuntimeException("Name can't be '' (blank or empty string)");

		if (minutes < 1)
			minutes = DEFAULT_KEEP_AGE_IN_MINUTES;
		
		_name    = name;
		_minutes = minutes;
		_isPeriodAchieved = false;
	}

	/**
	 * Add a entry
	 * 
	 * @param val
	 * @return this object so we easily can call <code>getAvg</code> on a <i>one-liner</i>
	 */
	public synchronized ChartDataHistorySeries add(TrendGraphDataPoint dp)
	{
		// first remove old entries
		removeOldEntries();
		
		if (dp == null)
		{
			_logger.info("ChartDataHistorySeries.add(TrendGraphDataPoint): dp == null. Simply skipping and return early.");
			return null;
		}
		if ( ! dp.hasData() )
		{
			_logger.info("ChartDataHistorySeries.add(TrendGraphDataPoint): dp.hasData() == false. for TrenGraph[" + dp + "]. Simply skipping and return early.");
			return null;
		}
		if (dp.getDate() == null)
		{
			_logger.info("ChartDataHistorySeries.add(TrendGraphDataPoint): dp.getDate() == null. for TrenGraph[" + dp + "]. Simply skipping and return early.");
			return null;
		}

		long time = dp.getDate().getTime();

		String[] lArr = dp.getLabel();
//		String[] lArr = dp.getLabelDisplay();
		Double[] dArr = dp.getData();

		List<String> inExistingSeries_butNotInInputSeries = new ArrayList<>(_series.keySet());

		for (int i=0; i<lArr.length; i++)
		{
			String name = lArr[i];
			Double data = dArr[i];

			// Get List, if not exists: Create it
			LinkedList<Double> serieList = _series.get(name);
			if (serieList == null)
			{
				serieList = new LinkedList<>();
				_series.put(name, serieList);
			}

			// If the data points is not same as _timeList then we need to "fill" with entries 
			// This happens if it's a "new" entry 
			while (serieList.size() <_timeList.size())
				serieList.add( FILL_VALUE );
			
			// Add entry 
			// note: new entries are added at "END", so oldest entry would be FIRST
			serieList.add( data );

			inExistingSeries_butNotInInputSeries.remove(name);
		}
		
		// TODO: Add dummy entries for series that exists, but was NOT part of the TrendGraphDataPoint
		if ( ! inExistingSeries_butNotInInputSeries.isEmpty() )
		{
			for (String serieName : inExistingSeries_butNotInInputSeries)
			{
				LinkedList<Double> serieList = _series.get(serieName);
				
				for (int f=0; f<_timeList.size(); f++)
					serieList.add( FILL_VALUE );
			}
		}

		// Add the "time" to the list where we keep track of what time's we have
		_timeList.add(time);
		
		// Check that ALL Series are of the same size
		List<String> errorList = null;
		for (java.util.Map.Entry<String, LinkedList<Double>> entry : _series.entrySet())
		{
			String             serieName     = entry.getKey();
			LinkedList<Double> dataPointList = entry.getValue();
			
			if (_timeList.size() != dataPointList.size())
			{
				if (errorList == null)
					errorList = new ArrayList<>();

				errorList.add(serieName + "=" + dataPointList.size());
			}
		}
		if (errorList != null)
		{
			throw new RuntimeException("ChartDataHistorySeries:Name='" + _name + "': Number of entries is not of expected size: time.size()=" + _timeList.size() + ", FaultySeries: " + errorList);
		}

		return this;
	}

	/**
	 * Remove entries that's no longer in the sample period
	 */
	private synchronized void removeOldEntries()
	{
		if (_timeList.isEmpty())
			return;

		long now = System.currentTimeMillis();
		long threshold = _minutes * 60 * 1000;
		while( ! _timeList.isEmpty() )
		{
			Long oldestEntry = _timeList.getFirst(); // note: new entries are added at "END", so oldest entry would be FIRST
			long ageinMs = now - oldestEntry;
			if (ageinMs > threshold)
			{
				// Remove "oldest" entry for each data list serie
				for (LinkedList<Double> serieDataList : _series.values())
					serieDataList.removeFirst(); // oldest entry would be FIRST
				
				_timeList.removeFirst(); // oldest entry would be FIRST
				_isPeriodAchieved = true;
			}
			else
				break;
		}
	}


	public LinkedList<Long> getTimerList()
	{
		return _timeList;
	}

	public LinkedHashMap<String, LinkedList<Double>> getSeriesMap()
	{
		return _series;
	}

	/** get name of the entry */
	public String getName()
	{
		return _name;
	}

	/** get how many minutes does this counter hold */
	public int getSampleMinutes()
	{
		return _minutes;
	}

	/** Returns the number of values in the period. */
	public int size()
	{
		// first remove old entries
		removeOldEntries();

		return _timeList.size();
	}

	/** reset the list of values and set _isPeriodAchieved=false */
	public void reset()
	{
		_isPeriodAchieved = false;
		_timeList.clear();
		_series.clear();
	}

	/** get milliseconds for the oldest entry */
	public synchronized long getOldestAgeInMs()
	{
		// first remove old entries
		removeOldEntries();

		if (_timeList.isEmpty())
			return -1;

		Long oldestEntry = _timeList.getFirst(); // note: new entries are added at "END", so oldest entry would be FIRST
		long now = System.currentTimeMillis();
		long ageinMs = now - oldestEntry;

		return ageinMs;
	}

	/** get seconds for the oldest entry */
	public int getOldestAgeInSec()
	{
		if (_timeList.isEmpty())
			return -1;

		return (int) getOldestAgeInMs() / 1000;
	}

	/**
	 * has time objective been Achieved (has we been samples for a longer time than X minutes)
	 * @return 
	 */
	public boolean isPeriodAchieved()
	{
		// first remove old entries
		removeOldEntries();

		return _isPeriodAchieved;
	}
	
//	/**
//	 * Get average value for records that has been sampled during the sample period
//	 * <ul>
//	 *   <li>If the period has no values, ZERO will be returned </li>
//	 *   <li>if the sample period has <b>NOT</b> been achieved, average will still be calculated on added values.</li>
//	 * </ul>
//	 * 
//	 * To check if the sample period has been achieved call <code>isPeriodAchieved()</code>
//	 */
//	public double getAvg()
//	{
//		return getAvg(0d, false);
//	}
//
//	/**
//	 * Get average value for records that has been sampled during the sample period
//	 * <ul>
//	 *   <li>If the period has no values, <i>defaultVal</i> will be returned </li>
//	 *   <li>if the sample period has <b>NOT</b> been achieved, average will still be calculated on added values.</li>
//	 * </ul>
//	 * 
//	 * To check if the sample period has been achieved call <code>isPeriodAchieved()</code>
//	 * 
//	 * @param defaultVal                                  default value
//	 */
//	public double getAvg(double defaultVal)
//	{
//		return getAvg(defaultVal, false);
//	}
//
//	/**
//	 * Get average value for records that has been sampled during the sample period
//	 * <ul>
//	 *   <li>If the period has no values, <i>defaultVal</i> will be returned </li>
//	 *   <li>if the sample period has <b>NOT</b> been achieved, average or <i>defaultVal</i> will be returned. based on parameter: <code>returnDefaultIfPeriodHasNotBeenAchieved</code></li>
//	 * </ul>
//	 * 
//	 * To check if the sample period has been achieved call <code>isPeriodAchieved()</code>
//	 * 
//	 * @param defaultVal                                  default value
//	 * @param returnDefaultIfPeriodHasNotBeenAchieved     like the name of the parameter
//	 */
//	public synchronized double getAvg(double defaultVal, boolean returnDefaultIfPeriodHasNotBeenAchieved)
//	{
//		return getAvg(defaultVal, returnDefaultIfPeriodHasNotBeenAchieved, -1);
//	}
//
//	/**
//	 * Get average value for records that has been sampled during the sample period
//	 * <ul>
//	 *   <li>If the period has no values, <i>defaultVal</i> will be returned </li>
//	 *   <li>if the sample period has <b>NOT</b> been achieved, average or <i>defaultVal</i> will be returned. based on parameter: <code>returnDefaultIfPeriodHasNotBeenAchieved</code></li>
//	 * </ul>
//	 * 
//	 * To check if the sample period has been achieved call <code>isPeriodAchieved()</code>
//	 * 
//	 * @param defaultVal                                  default value
//	 * @param returnDefaultIfPeriodHasNotBeenAchieved     like the name of the parameter
//	 * @param capValue                                    Upper value for the avgCalculation, only used if above 0. (values above this will be read as "this" value) 
//	 * 
//	 */
//	public synchronized double getAvg(double defaultVal, boolean returnDefaultIfPeriodHasNotBeenAchieved, double capValue)
//	{
//		// first remove old entries
//		removeOldEntries();
//
//		int size = _values.size();
//		double sum = 0;
//
//		if (size == 0)
//			return defaultVal;
//
//		if ( returnDefaultIfPeriodHasNotBeenAchieved  &&  ! isPeriodAchieved() )
//			return defaultVal;
//
//		for (Entry e : _values)
//		{
//			if (capValue > 0 && e._val > capValue)
//			{
//				sum += capValue;
//			}
//			else
//			{
//				sum += e._val;
//			}
//		}
//		
//		return sum / size;
//	}
//
//	/**
//	 * Get peak/max value over the period 
//	 * @return
//	 */
//	public double getPeakNumber()
//	{
//		double peak = 0;
//		
//		for (Entry e : _values)
//		{
//			peak = Math.max(peak, e._val);
//		}
//		
//		return peak;
//	}
//	
//
//	/**
//	 * Get peak time over the period 
//	 * @return
//	 */
//	public Timestamp getPeakTimestamp()
//	{
//		double peakNumber = 0;
//		long   peakTime   = 0;
//		
//		for (Entry e : _values)
//		{
//			if (e._val >= peakNumber)
//			{
//				peakNumber = e._val;
//				peakTime   = e._ts;
//			}
//		}
//		
//		return new Timestamp(peakTime);
//	}
//	
//
//	public List<Entry> getValues()
//	{
//		return _values;
//	}
//	/**
//	 * Simple place holder entry
//	 */
//	static class Entry
//	{
//		long   _ts;
//		double _val;
//
//		public Entry(long ts, double val)
//		{
//			_ts  = ts;
//			_val = val;
//		}
//		public long   getTime()  { return _ts; }
//		public double getValue() { return _val; }
//	}



//	/**
//	 * Small dummy test to see how an image looks like!
//	 * <p>
//	 * Use https://jsfiddle.net/ to check result
//	 * @param args
//	 */
//	public static void main(String[] args)
//	{
//		ChartDataCounter si = ChartDataManager.getInstance(null, "swapIn",  5);
//		ChartDataCounter so = ChartDataManager.getInstance(null, "swapOut", 5);
//
//		int crCnt = 30;
//		long startTime = System.currentTimeMillis() / 1000 - 300;
//
//		Random r = new Random();
//		int low = 100;
//		int high = 1000;
//		
//		for (int i=0; i<crCnt; i++)
//		{
//			int si_val = r.nextInt(high-low) + low;
//			int so_val = r.nextInt(high-low) + low;
//			
//			si._values.add(new Entry((startTime + (i*30))* 1000 , 1000+si_val));
//			so._values.add(new Entry((startTime + (i*30))* 1000 , 1200+so_val));
//		}
//
//		// Create a small chart, that can be used in emails etc.
//		String htmlChartImage = ChartDataHelper.getChartAsHtmlImage("OS Swapping (15 minutes)", 
//				ChartDataManager.getInstance(null, "swapIn",  5),
//				ChartDataManager.getInstance(null, "swapOut", 5));
//
//		// To check the image, use for example: https://jsfiddle.net/
//		System.out.println(htmlChartImage);
//	}

	/**
	 * Small dummy test
	 */
	public static void main(String[] args)
	{
		ChartDataHistorySeries d1 = new ChartDataHistorySeries("dummyS1", -1);
		TrendGraphDataPoint tgdp = new TrendGraphDataPoint("DummyChart", "Chart Label", "graphProps", Category.OTHER, false, false, TrendGraphDataPoint.RUNTIME_REPLACED_LABELS, LabelType.Dynamic);

		long ts = System.currentTimeMillis();
		long testId = 0;

		//--------------------------------------------------------
		// Normal add
		//--------------------------------------------------------
		Double[] dArray = new Double[3];
		String[] lArray = new String[dArray.length];

		testId = 0;
		lArray[0] = "s1"; dArray[0] = 1d;
		lArray[1] = "s2"; dArray[1] = 2d;
		lArray[2] = "s3"; dArray[2] = 3d;
		tgdp.setDataPoint( new Date(ts+(testId*1000*60)), lArray, dArray);
		d1.add(tgdp);
		System.out.println("d1.size=" + d1.size() + ", xxx=" + d1._series);
//TODO; Write tests here
//	* d1.size==0, s1[0]==1d, s2[0]==2d, s3[0]==3d;

		testId = 1;
		lArray[0] = "s1"; dArray[0] = 10d;
		lArray[1] = "s2"; dArray[1] = 20d;
		lArray[2] = "s3"; dArray[2] = 30d;
		tgdp.setDataPoint( new Date(ts+(testId*1000*60)), lArray, dArray);
		d1.add(tgdp);
		System.out.println("d1.size=" + d1.size() + ", xxx=" + d1._series);

		testId = 2;
		lArray[0] = "s1"; dArray[0] = 100d;
		lArray[1] = "s2"; dArray[1] = 200d;
		lArray[2] = "s3"; dArray[2] = 300d;
		tgdp.setDataPoint( new Date(ts+(testId*1000*60)), lArray, dArray);
		d1.add(tgdp);
		System.out.println("d1.size=" + d1.size() + ", xxx=" + d1._series);

		//--------------------------------------------------------
		// Add without some series
		//--------------------------------------------------------
		dArray = new Double[1];
		lArray = new String[dArray.length];

		testId = 3;
		lArray[0] = "s2"; dArray[0] = 2_000d;
		tgdp.setDataPoint( new Date(ts+(testId*1000*60)), lArray, dArray);
		d1.add(tgdp);
		System.out.println("d1.size=" + d1.size() + ", xxx=" + d1._series);

		testId = 4;
		lArray[0] = "s1"; dArray[0] = 1_000d;
		tgdp.setDataPoint( new Date(ts+(testId*1000*60)), lArray, dArray);
		d1.add(tgdp);
		System.out.println("d1.size=" + d1.size() + ", xxx=" + d1._series);

		testId = 5;
		lArray[0] = "s3"; dArray[0] = 3_000d;
		tgdp.setDataPoint( new Date(ts+(testId*1000*60)), lArray, dArray);
		d1.add(tgdp);
		System.out.println("d1.size=" + d1.size() + ", xxx=" + d1._series);


		//--------------------------------------------------------
		// Add without new series
		//--------------------------------------------------------
		dArray = new Double[4];
		lArray = new String[dArray.length];

		testId = 6;
		lArray[0] = "s1"; dArray[0] = 10_000d;
		lArray[1] = "s2"; dArray[1] = 20_000d;
		lArray[2] = "s3"; dArray[2] = 30_000d;
		lArray[3] = "s4"; dArray[3] = 40_000d;
		tgdp.setDataPoint( new Date(ts+(testId*1000*60)), lArray, dArray);
		d1.add(tgdp);
		System.out.println("d1.size=" + d1.size() + ", xxx=" + d1._series);


		// Create a small chart, that can be used in emails etc.
		String htmlChartImageAll = ChartDataHistoryCreator.getChartAsHtmlImage(d1.getName(), d1);

		String htmlChartImageSome = ChartDataHistoryCreator.getChartAsHtmlImage(d1.getName(), d1, "s1", "s3");

		// To check the image, use for example: https://jsfiddle.net/
		System.out.println("=====================================================================================");
		System.out.println("== ALL Series");
		System.out.println("=====================================================================================");
		System.out.println(htmlChartImageAll);
		System.out.println("-------------------------------------------------------------------------------------");

		System.out.println("=====================================================================================");
		System.out.println("== Only 's1' & 's3' Series");
		System.out.println("=====================================================================================");
		System.out.println(htmlChartImageSome);
		System.out.println("-------------------------------------------------------------------------------------");
	}
}
