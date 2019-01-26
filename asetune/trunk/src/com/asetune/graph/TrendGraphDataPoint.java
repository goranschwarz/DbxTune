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
package com.asetune.graph;

import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import com.asetune.utils.StringUtil;

public class TrendGraphDataPoint
implements Cloneable
{
	/** Use this when you want to assign Labels at a later stage */
	public static final String[] RUNTIME_REPLACED_LABELS = new String[] {"RUNTIME_REPLACED_LABELS"};
	
	private boolean _initializedWithRuntimeReplacedLabels = false; 
	private String    _name = null;
//	private Timestamp _ts   = null;
	private Date      _date = null;

	private String    _graphLabel     = null;
	private Category  _category       = Category.OTHER;
	private boolean   _isPercentGraph = false;
	private boolean   _visibleAtStart = false;
	
	private String[] _labelArray        = null; //        the label to use for this "line"
	private String[] _labelDisplayArray = null; // the "real" label to use for this "line", if this isn't null: _labelArray will be the real "key"...
	private Double[] _dataArray         = null;

	private Map<String, Integer> _labelMapPos = new LinkedHashMap<String, Integer>();

	private LabelType _labelType = LabelType.Static;
	
	public enum LabelType
	{
		Dynamic, Static
	};

	public enum Category
	{
		/** Usnspecified */
		OTHER, 
		
		/** CPU Resources */
		CPU, 

		/** DISK Resources */
		DISK, 

		/** SPACE utilizarion Resources */
		SPACE, 

		/** NETWORK Resources */
		NETWORK, 

		/** Server Configuration */
		SRV_CONFIG, 

		/** Data Cache */
		CACHE, 

		/** LOCK */
		LOCK,
		
		/** OPERATIONS */
		OPERATIONS,
		
		/** WAITS */
		WAITS,
		
		/** REPLICATION */
		REPLICATION, 
		
		/** MEMORY */
		MEMORY,
		
	};


//	@Deprecated
//	public TrendGraphDataPoint(String name, String[] labelArray, LabelType labelType)
//	{
//		this(name, "-This-Constructor-Will-Be-Removed-ASAP-", false, labelArray, labelType);
//	}
	public TrendGraphDataPoint(String name, String graphLabel, TrendGraphDataPoint.Category category, boolean isPercentGraph, boolean visibleAtStart, String[] labelArray, LabelType labelType)
	{
		if ( LabelType.Dynamic.equals(labelType) && labelArray == null)
			_labelArray = RUNTIME_REPLACED_LABELS;
		
		if (labelArray == null)
			throw new RuntimeException("Sorry you can not initialize the TrendGraphDataPoint, named '"+name+"' with a null labelArray. Please use 'TrendGraphDataPoint.RUNTIME_REPLACED_LABELS', if they are not known at initialization time.");

		_name           = name;
		_graphLabel     = graphLabel;
		_category       = category;
		_isPercentGraph = isPercentGraph;
		_visibleAtStart = visibleAtStart;
		_labelType      = labelType;
		
		if ( labelArray.equals(RUNTIME_REPLACED_LABELS) )
		{
			_labelArray = null;
			_initializedWithRuntimeReplacedLabels = true;
		}
		else
		{
			_labelArray = labelArray;
			_initializedWithRuntimeReplacedLabels = false;
			
			for (int i=0; i<_labelArray.length; i++)
				_labelMapPos.put(_labelArray[i], i);
		}
	}
//	public TrendGraphDataPoint(String name)
//	{
//		this(name, null, null, null);
//	}
//	public TrendGraphDataPoint(String name, String[] labelArray)
//	{
//		this(name, null, labelArray, null);
//	}
//	public TrendGraphDataPoint(String name, Date date, String[] labelArray, Double[] dataArray)
//	{
//		_name       = name;
//		_date       = date;
//		_labelArray = labelArray;
//		_dataArray  = dataArray;
//		
//		if (_labelArray != null)
//		{
//			for (int i=0; i<_labelArray.length; i++)
//				_labelMapPos.put(_labelArray[i], i);
//		}
//	}

////	public void setName (String name)         { _name = name; }
//	public void setDate (Date date)           { _date = date; }
//	public void setLabel(String[] la)         { _labelArray = la; }
//	public void setLabelDisplay(String[] lda) { _labelDisplayArray = lda; }
//	public void setData (Double[] da)         { _dataArray  = da; }
	private void setDate (Date date)           { _date = date; }
//	private void setLabel(String[] la)         { _labelArray = la; }
//	private void setLabelDisplay(String[] lda) { _labelDisplayArray = lda; }
//	private void setData (Double[] da)         { _dataArray  = da; }

	public void setValue(String label, Double data)
	{
		setValue(label, null, data);
	}
	public void setValue(String label, String labelDisplay, Double data)
	{
		// Could be done in same way as: setDataPoint(Date date, String[] labelArray, String[] labelDisplayArray, Double[] dataArray)
		// But only for 1 values...
		// Problem areas:
		//     - clearData() methods needs to be called to set all data[] to 0.0
		//     - LabelDisplay can be initialized/used at a later stage == deferred initialization of the array...
		//     - etc, etc: so lets implemet that later if we need it.
	}

	public void setDataPoint(Date date, Double[] dataArray)
	{
		if (dataArray == null)
			throw new RuntimeException("The passed dataArray is null. TrendGraphDataPoint named '"+getName()+"'.");

		if (_labelArray == null)
			throw new RuntimeException("_labelArray has not been set in the TrendGraphDataPoint named '"+getName()+"'. (_initializedWithRuntimeReplacedLabels="+_initializedWithRuntimeReplacedLabels+")");

		if (_labelArray.length != dataArray.length)
			throw new RuntimeException("Missmatch in labelArray size and dataArray size. _labelArray.length="+_labelArray.length+", labels='"+StringUtil.toCommaStr(_labelArray)+"', dataArray.length="+dataArray.length+", data='"+StringUtil.toCommaStr(dataArray)+"'. in TrendGraphDataPoint '"+getName()+"'.");

		// use common method
		setDataPoint(date, _labelArray, null, dataArray);
	}
	public void setDataPoint(Date date, String[] labelArray, Double[] dataArray)
	{
		if (dataArray == null)
			throw new RuntimeException("The passed dataArray is null. TrendGraphDataPoint named '"+getName()+"'.");

		if (labelArray == null)
			throw new RuntimeException("The passed labelArray is null. TrendGraphDataPoint named '"+getName()+"'.");

		if (labelArray.length != dataArray.length)
			throw new RuntimeException("Missmatch in the passed labelArray size and dataArray size. labelArray.length="+labelArray.length+", dataArray.length="+dataArray.length+". in TrendGraphDataPoint '"+getName()+"'.");

		// use common method
		setDataPoint(date, labelArray, null, dataArray);
	}
	public void setDataPoint(Date date, String[] labelArray, String[] labelDisplayArray, Double[] dataArray)
	{
		if (dataArray == null)
			throw new RuntimeException("The passed dataArray is null. TrendGraphDataPoint named '"+getName()+"'.");

		if (labelArray == null)
			throw new RuntimeException("The passed labelArray is null. TrendGraphDataPoint named '"+getName()+"'.");

		if (labelArray.length == 0)
			throw new RuntimeException("The passed labelArray is of length 0. TrendGraphDataPoint named '"+getName()+"'.");

		if (labelArray.length != dataArray.length)
			throw new RuntimeException("Missmatch in the passed labelArray size and dataArray size. labelArray.length="+labelArray.length+", labels='"+StringUtil.toCommaStr(labelArray)+"', dataArray.length="+dataArray.length+", data='"+StringUtil.toCommaStr(dataArray)+"'. in TrendGraphDataPoint '"+getName()+"'.");

		if (labelDisplayArray != null && labelArray.length != labelDisplayArray.length)
			throw new RuntimeException("Missmatch in the passed labelArray size and labelDisplayArray size. labelArray.length="+labelArray.length+", labels='"+StringUtil.toCommaStr(labelArray)+"', labelDisplayArray.length="+labelDisplayArray.length+", labelDisplayArray='"+StringUtil.toCommaStr(labelDisplayArray)+"'. in TrendGraphDataPoint '"+getName()+"'.");

		// First time we add data this, and it's initialized with RUNTIME_REPLACED_LABELS, then we expect some columns in the input.
//		if ( labelArray == null && _initializedWithRuntimeReplacedLabels )
//		{
//			throw new RuntimeException("The TrendGraphDataPoint named '"+getName()+"' was initialized with 'RUNTIME_REPLACED_LABELS'. Then the passed labelArray must contain some valid labels.");
//		}

		// If it was called from: setDataPoint(date, dataArray)
		if (labelArray == _labelArray)
		{
		}

		// Check for unititialized Arrays, and initialize them
		if (_dataArray == null) 
		{
			_dataArray = new Double[labelArray.length];
		}

		if (_labelArray == null) // first time: Copy the labels to _labelArray
		{
			_labelArray = new String[labelArray.length];
			for (int i=0; i<labelArray.length; i++)
			{
				_labelArray[i] = labelArray[i];
				_labelMapPos.put(labelArray[i], i);
			}
		}
		if (_labelDisplayArray == null && labelDisplayArray != null) // first time: Copy the labels or labelDisplayArray to the _labelDisplayArray
		{
			// Take the length from the labels... if labels was added this time it will be extended in the same way as _labels will be
			_labelDisplayArray = new String[_labelArray.length];
		}
//		if (_labelDisplayArray == null && labelDisplayArray != null) // first time: Copy the labels or labelDisplayArray to the _labelDisplayArray
//		{
//			_labelDisplayArray = new String[labelArray.length];
//			String[] sa = labelDisplayArray == null ? labelArray : labelDisplayArray;
//			for (int i=0; i<labelArray.length; i++)
//				_labelDisplayArray[i] = sa[i];
//		}

		// ALWAYS reset the data array (if we only send a "subset" of the labels... all data slots will be 0.0
		for (int i=0; i<_dataArray.length; i++)
			_dataArray[i] = 0.0;
		
		
		// If the labels are not in the same order...
		// Set the correct data slot (previously added)
		for (int i=0; i<labelArray.length; i++)
		{
			int dataSlotPos = i;
			Integer mapDataSlotPos = _labelMapPos.get(labelArray[i]);
			if (mapDataSlotPos == null)
			{
				// NEW label (not in current data set)
				// Make ALL the arrays bigger
				Double[] tmpDataArray          = new Double[_dataArray.length         + 1];
				String[] tmpLabelArray         = new String[_labelArray.length        + 1];
//				String[] tmpLlabelDisplayArray = new String[_labelDisplayArray.length + 1];
				System.arraycopy(_dataArray,         0, tmpDataArray,          0, _dataArray.length);
				System.arraycopy(_labelArray,        0, tmpLabelArray,         0, _labelArray.length);
//				System.arraycopy(_labelDisplayArray, 0, tmpLlabelDisplayArray, 0, _labelDisplayArray.length);
				
				_dataArray         = tmpDataArray;
				_labelArray        = tmpLabelArray;
//				_labelDisplayArray = tmpLlabelDisplayArray;
				
				_dataArray        [_dataArray        .length - 1] = 0.0;
				_labelArray       [_labelArray       .length - 1] = labelArray[i];
//				_labelDisplayArray[_labelDisplayArray.length - 1] = labelDisplayArray == null ? labelArray[i] : labelDisplayArray[i];

				if (_labelDisplayArray != null && labelDisplayArray != null)
				{
					String[] tmpLlabelDisplayArray = new String[_labelDisplayArray.length + 1];
					System.arraycopy(_labelDisplayArray, 0, tmpLlabelDisplayArray, 0, _labelDisplayArray.length);
					_labelDisplayArray = tmpLlabelDisplayArray;
					_labelDisplayArray[_labelDisplayArray.length - 1] = labelDisplayArray[i];
				}

				dataSlotPos = _dataArray.length - 1;
				_labelMapPos.put(labelArray[i], dataSlotPos);
			}
			else if (dataSlotPos != mapDataSlotPos.intValue())
			{
				// OLD Label, but not in same data array slot
				dataSlotPos = mapDataSlotPos.intValue();
			}
			// Copy the current label to the correct _dataArray position/slot 
			_dataArray[dataSlotPos] = dataArray[i];
			
			// and if we got a display label
			if (_labelDisplayArray != null && labelDisplayArray != null)
				_labelDisplayArray[dataSlotPos] = labelDisplayArray[i];
			
		}
//		FIXME

		setDate        (date);
//		setLabel       (labelArray);
//		setLabelDisplay(labelDisplayArray);
//		setData        (dataArray);
		
		// FIXME: we need to fix the above... check if:
		//        - labels is bigger than it previously was...
		//        - if labels are not in the same order, reorder them
	}
	
	// FIXME: setup a listener thing so we can fireXXX
	//        - fireLabelDeleted
	//        - fireLabelInserted
	//        - fireLabelTextModified
	//        - fireDataChanged?????  and kick it off from the EDT (since it's a GUI thing)
	
	// Also have a look at setData(Date date, Map<String, Double> map)
	// Also have a look at setData(Date date, Map<String, Double> map, Map<String, String> displayLabelMap)
	
	public String   getName ()                { return _name; }
	public String   getGraphLabel ()          { return _graphLabel; }
	public Category getCategory ()            { return _category; }
	public boolean  isPercentGraph()          { return _isPercentGraph; }
	public boolean  isVisibleAtStart()        { return _visibleAtStart; }
	public Date     getDate ()                { return _date; }
//	public String[] getLabel()                { return _labelDisplayArray; }
//	public String[] getLabelDisplay()         { return null; }
	public String[] getLabel()                { return _labelArray; }
	public String[] getLabelDisplay()         { return _labelDisplayArray; }
	public Double[] getData ()                { return _dataArray; }
	public boolean  hasData ()
	{
		if (_dataArray == null || _labelArray == null)
			return false;

		if (_dataArray.length == 0 || _labelArray.length == 0)
			return false;
		
		return true;
	}

	
//	// The 2 below is only used in setData() to keep track of already added data, which might not been added in the current map
//	private Map<String,  Integer> _labelOrder_labelName      = null;
//	private Map<Integer, String>  _labelOrder_posToLabelName = null;

	/**
	 * Set data using a Map instead of ordinary positions.
	 * <p>
	 * The main idea here is to add some labels/data in the map<br>
	 * If the first time we add [str1=1.0] [str2=99.0]<br> 
	 * And the second time we add [str1=9.0] [str3=3.3]<br>
	 * When we add the second data set, the key(str2) was not included...<br>
	 * This is handled automatically, so the data set added the second time will be.<br>
	 * [str1=9.0] [str2=0.0] [str3=3.3]<br>
	 * <p>
	 * The basic idea is to "auto grow" new labels, and add 0.0 as default values for "missing" keys<br>
	 * If you want to reset the data set (reset "auto grow" and "missing keys", just call clear()
	 *
	 * @param date Date of this data point
	 * @param map the label,dataValue to add
	 */
	public void setData(Date date, Map<String, Double> map)
	{
		setData(date, map, null);
	}

	/**
	 * Set data using a Map instead of ordinary positions.
	 * <p>
	 * The main idea here is to add some labels/data in the map<br>
	 * If the first time we add [str1=1.0] [str2=99.0]<br> 
	 * And the second time we add [str1=9.0] [str3=3.3]<br>
	 * When we add the second data set, the key(str2) was not included...<br>
	 * This is handled automatically, so the data set added the second time will be.<br>
	 * [str1=9.0] [str2=0.0] [str3=3.3]<br>
	 * <p>
	 * The basic idea is to "auto grow" new labels, and add 0.0 as default values for "missing" keys<br>
	 * If you want to reset the data set (reset "auto grow" and "missing keys", just call clear()
	 *
	 * @param date Date of this data point
	 * @param map the label,dataValue to add
	 * @param displayLabelMap the label,labelToDisplay to add. null if we do not want to change a label
	 */
	public void setData(Date date, Map<String, Double> map, Map<String, String> displayLabelMap)
	{
		String[] lArr  = map.keySet().toArray(new String[0]);
		Double[] dArr  = map.values().toArray(new Double[0]);
		
		String[] ldArr = displayLabelMap == null ? null : displayLabelMap.values().toArray(new String[0]);
		
		setDataPoint(date, lArr, ldArr, dArr);
	}
//	public void setData(Date date, Map<String, Double> map, Map<String, String> displayLabelMap)
//	{
//		// Initialize if not done earlier
//		if (_labelOrder_labelName      == null)  _labelOrder_labelName      = new LinkedHashMap<String,  Integer>();
//		if (_labelOrder_posToLabelName == null)  _labelOrder_posToLabelName = new LinkedHashMap<Integer, String>();
//
//		// data & label array size = WaitCounterSummary.size or PRIVIOUS max size
//		Double[] dArr  = new Double[Math.max(map.size(), _labelOrder_labelName.size())];
//		String[] lArr  = new String[dArr.length];
//		String[] ldArr = null; // Only used if displayLabelMap holds data
//
//		if (displayLabelMap != null && displayLabelMap.size() > 0)
//			ldArr = new String[dArr.length];
//
////System.out.println("dArr & lArr length = " + dArr.length);
////		for (WaitCounterEntry wce : wcs.getClassNameMap().values())
//		for (String key : map.keySet())
//		{
//			Double dataValue = map.get(key);
//			
//			// aLoc = get arrayPosition for a specific key
//			// We want to have them in same order...
//			Integer aPos = _labelOrder_labelName.get(key);
//			if (aPos == null)
//			{
//				aPos = new Integer(_labelOrder_labelName.size());
//				_labelOrder_labelName.put(key, aPos);
//				_labelOrder_posToLabelName.put(aPos, key);
//				
//				// If the destination array is to small, expand it... 
//				if (aPos >= dArr.length)
//				{
//					Double[] new_dArr = new Double[aPos + 1];
//					String[] new_lArr = new String[new_dArr.length];
//					System.arraycopy(dArr, 0, new_dArr, 0, dArr.length);
//					System.arraycopy(lArr, 0, new_lArr, 0, lArr.length);
//					dArr = new_dArr;
//					lArr = new_lArr;
//				}
//			}
//			
//			dArr[aPos] = dataValue;
//			lArr[aPos] = key;
//
////System.out.println("updateGraphData("+getName()+"."+GRAPH_NAME_CLASS_NAME+"): aLoc="+aPos+", data="+dArr[aPos]+", label='"+lArr[aPos]+"'.");
//		}
//
//		// Fill in empty/blank array entries
//		for (int i=0; i<lArr.length; i++)
//		{
//			if (lArr[i] == null)
//			{
//				dArr[i] = 0.0;
//				lArr[i] = _labelOrder_posToLabelName.get(i);
//				if (lArr[i] == null)
//					lArr[i] = "-unknown-";
//
//				if (displayLabelMap != null && displayLabelMap.size() > 0)
//					ldArr[i] = displayLabelMap.get(lArr[i]);
//			}
//		}
//
//		// Set the values
//		setDataPoint(date, lArr, ldArr, dArr);
////		setDate(date);
////		setLabel(lArr);
////		setLabelDisplay(ldArr);
////		setData (dArr);
//	}
	
	public void clear()
	{
//		_labelOrder_labelName      = null;
//		_labelOrder_posToLabelName = null;

		if ( LabelType.Dynamic.equals(_labelType) )
		{
    		_labelArray        = null; //        the label to use for this "line"
    		_labelDisplayArray = null; // the "real" label to use for this "line", if this isn't null: _labelArray will be the real "key"...

    		_labelMapPos       = new LinkedHashMap<String, Integer>();
		}

		_dataArray = null;
	}

	
	@Override
	public Object clone()
	{
		// Make a memory copy of the object
	    try { return super.clone(); }
		catch (CloneNotSupportedException e) 
		{ throw new Error("This should never happen!"); }
	}
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(super.toString()).append(", ")
			.append("name='").append(_name).append("', ")
			.append("date='").append(_date).append("', ")
			.append("labels[").append(_labelArray==null?-1:_labelArray.length).append("]='").append(Arrays.deepToString(_labelArray)).append("', ")
			.append("labelDisplays[").append(_labelDisplayArray==null?-1:_labelDisplayArray.length).append("]='").append(Arrays.deepToString(_labelDisplayArray)).append("', ")
			.append("values[").append(_dataArray ==null?-1:_dataArray .length).append("]='").append(Arrays.deepToString(_dataArray)).append("'.");
		return sb.toString();
	}
}
