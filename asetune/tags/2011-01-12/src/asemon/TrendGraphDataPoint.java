package asemon;

import java.util.Arrays;
import java.util.Date;

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
	
	public Object clone()
	{
		// Make a memory copy of the object
	    try { return super.clone(); }
		catch (CloneNotSupportedException e) 
		{ throw new Error("This should never happen!"); }
	}
	
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
