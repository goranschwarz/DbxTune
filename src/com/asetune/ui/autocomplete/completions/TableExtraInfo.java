package com.asetune.ui.autocomplete.completions;

import java.text.NumberFormat;

public class TableExtraInfo
{
	public static final String TableRowCount       = "TableRowCount";
	public static final String TableTotalSizeInKb  = "TableTotalSizeInKb";
	public static final String TableDataSizeInKb   = "TableDataSizeInKb";
	public static final String TableIndexSizeInKb  = "TableIndexSizeInKb";
	public static final String TableLobSizeInKb    = "TableLobSizeInKb";

	public static final String TableLockScheme     = "TableLockScheme";

	public static final String TablePartitionCount = "TablePartitionCount";

	public static final String IndexExtraInfo      = "IndexExtraInfo";

	private String _name     = "";
	private String _descName = "";

	private Object _val      = null;
	
	private String _textDesc = null;
	private String _htmlDesc = null;

	public void setName(String name)                { _name     = name; }
	public void set_descName(String descName)       { _descName = descName; }
	public void set_val(Object val)                 { _val      = val; }
	public void setTextDescription(String textDesc) { _textDesc = textDesc; }
	public void setHtmlDescription(String htmlDesc) { _htmlDesc = htmlDesc; }

	public String getName()            { return _name; }
	public String getDescriptiveName() { return _descName; }
	public Object getValue()           { return _val; }
	public String getTextDescription() { return _textDesc; }
	public String getHtmlDescription() { return _htmlDesc; }

	public String getStringValue()     
	{
		if (_val == null)
			return "";

		// Use "formated" numbers
		if (_val instanceof Number)
		{
			return NumberFormat.getNumberInstance().format( (Number)_val );
		}

		return _val.toString();
	}


	public TableExtraInfo(String name, String descriptiveName, Object value, String textDescription, String htmlDescription)
	{
		_name     = name;
		_descName = descriptiveName;
		_val      = value;
		_textDesc = textDescription;
		_htmlDesc = htmlDescription;

		if (_htmlDesc == null)
		{
			setHtmlDescription(_textDesc);
		}
	}
}
