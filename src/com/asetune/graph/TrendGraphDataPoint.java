package com.asetune.graph;

import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

public class TrendGraphDataPoint
implements Cloneable
{
	private String    _name = null;
//	private Timestamp _ts   = null;
	private Date      _date = null;

	private String[] _labelArray = null;
	private Double[] _dataArray  = null;

	public TrendGraphDataPoint(String name)
	{
		this(name, null, null, null);
	}
	public TrendGraphDataPoint(String name, String[] labelArrya)
	{
		this(name, null, labelArrya, null);
	}
	public TrendGraphDataPoint(String name, Date date, String[] labelArrya, Double[] dataArray)
	{
		_name       = name;
		_date       = date;
		_labelArray = labelArrya;
		_dataArray  = dataArray;
	}

	public void setName (String name) { _name = name; }
	public void setDate (Date date)   { _date = date; }
	public void setLabel(String[] la) { _labelArray = la; }
	public void setData (Double[] da) { _dataArray  = da; }

	public String   getName () { return _name; }
	public Date     getDate () { return _date; }
	public String[] getLabel() { return _labelArray; }
	public Double[] getData () { return _dataArray; }
	public boolean  hasData () 
	{ 
		if (_dataArray == null || _labelArray == null)
			return false;

		if (_dataArray.length == 0 || _labelArray.length == 0)
			return false;
		
		return true;
	}

	
	// The 2 below is only used in setData() to keep track of alraedy added data, which might not been added in the current map
	private Map<String,  Integer> _labelOrder_labelName      = null;
	private Map<Integer, String>  _labelOrder_posToLabelName = null;

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
		// Initialize if not done earlier
		if (_labelOrder_labelName      == null)  _labelOrder_labelName      = new LinkedHashMap<String,  Integer>();
		if (_labelOrder_posToLabelName == null)  _labelOrder_posToLabelName = new LinkedHashMap<Integer, String>();

		// data & label array size = WaitCounterSummary.size or PRIVIOUS max size
		Double[] dArr = new Double[Math.max(map.size(), _labelOrder_labelName.size())];
		String[] lArr = new String[dArr.length];

//System.out.println("dArr & lArr length = " + dArr.length);
//		for (WaitCounterEntry wce : wcs.getClassNameMap().values())
		for (String key : map.keySet())
		{
			Double dataValue = map.get(key);
			
			// aLoc = get arrayPosition for a specific key
			// We want to have them in same order...
			Integer aPos = _labelOrder_labelName.get(key);
			if (aPos == null)
			{
				aPos = new Integer(_labelOrder_labelName.size());
				_labelOrder_labelName.put(key, aPos);
				_labelOrder_posToLabelName.put(aPos, key);
				
				// If the destination array is to small, expand it... 
				if (aPos >= dArr.length)
				{
					Double[] new_dArr = new Double[aPos + 1];
					String[] new_lArr = new String[new_dArr.length];
					System.arraycopy(dArr, 0, new_dArr, 0, dArr.length);
					System.arraycopy(lArr, 0, new_lArr, 0, lArr.length);
					dArr = new_dArr;
					lArr = new_lArr;
				}
			}
			
			dArr[aPos] = dataValue;
			lArr[aPos] = key;

//System.out.println("updateGraphData("+getName()+"."+GRAPH_NAME_CLASS_NAME+"): aLoc="+aPos+", data="+dArr[aPos]+", label='"+lArr[aPos]+"'.");
		}

		// Fill in empty/blank array entries
		for (int i=0; i<lArr.length; i++)
		{
			if (lArr[i] == null)
			{
				dArr[i] = 0.0;
				lArr[i] = _labelOrder_posToLabelName.get(i); 
				if (lArr[i] == null)
					lArr[i] = "-unknown-";
			}
		}

		// Set the values
		setDate(date);
		setLabel(lArr);
		setData (dArr);
	}
	
	public void clear()
	{
		_labelOrder_labelName      = null;
		_labelOrder_posToLabelName = null;
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
			.append("values[").append(_dataArray ==null?-1:_dataArray .length).append("]='").append(Arrays.deepToString(_dataArray)).append("'.");
		return sb.toString();
	}
}
