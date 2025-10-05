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
package com.dbxtune.ui.autocomplete.completions;

import java.text.NumberFormat;

public class TableExtraInfo
{
	public static final String TableRowCount             = "TableRowCount";
	public static final String TableTotalSizeInKb        = "TableTotalSizeInKb";
	public static final String TableDataSizeInKb         = "TableDataSizeInKb";
	public static final String TableInRowSizeInKb        = "TableInRowSizeInKb"; /* mostly for SQL Server */
	public static final String TableOverflowRowSizeInKb  = "TableOverflowRowSizeInKb"; /* mostly for SQL Server */
	public static final String TableIndexSizeInKb        = "TableIndexSizeInKb";
	public static final String TableIndexCount           = "TableIndexCount";
	public static final String TableLobSizeInKb          = "TableLobSizeInKb";

	public static final String TableLockScheme           = "TableLockScheme";

	public static final String TablePartitionCount       = "TablePartitionCount";

	public static final String IndexType                 = "IndexType";
	public static final String IndexExtraInfo            = "IndexExtraInfo";
	public static final String IndexExtraInfoDescription = "IndexExtraInfoDescription";

	public static final String IndexIncludeColumns       = "IndexIncludeColumns";

	public static final String TableOwner                = "TableOwner";

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

//	public boolean isValueMap()        { return _val != null && _val instanceof Map; }
//	public Map<String, String> getValueMap() { return (Map<String, String>) _val; }
	
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
