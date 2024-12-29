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
package com.dbxtune.gui.swing;

import java.text.ParseException;

import javax.swing.SortOrder;

import org.apache.log4j.Logger;

/* 
 * each entry looks like: colName={modelPos=1,viewPos=1,isVisible=true,sort=unsorted}
 * where modelPos=int[0..999], viewPos=int[0..999], isVisible=boolean[true|false], sort=int:String[unsorted|ascending|descending] 
 */
public class ColumnHeaderPropsEntry
{
////FIX to temporaraly implement: import org.jdesktop.swingx.decorator.SortOrder;
//public static final class SortOrder 
//{	
//	private final String name;
//	private SortOrder(String name) 
//	{
//		this.name = name;
//	}
//	public String toString() 
//	{
//		return name;
//	}
//	public static final SortOrder ASCENDING = new SortOrder("ascending");
//	public static final SortOrder DESCENDING = new SortOrder("descending");
//	public static final SortOrder UNSORTED = new SortOrder("unsorted");
//}

	public static final int AS_LAST_VIEW_COLUMN = Integer.MAX_VALUE;

	private static Logger _logger = Logger.getLogger(ColumnHeaderPropsEntry.class);

	protected String    _colName      = null;
	protected int       _modelPos     = -1;
	protected int       _viewPos      = -1;
//	protected String    _afterColName = null;
	protected boolean   _isVisible    = true;
	protected SortOrder _sortOrder    = SortOrder.UNSORTED;
	protected int       _sortOrderPos = -1;
	protected int       _width        = -1;

	public String    getColumnName()   { return _colName;      }
	public int       getModelPos()     { return _modelPos;     }
	public int       getViewPos()      { return _viewPos;      }
	public boolean   isVisible()       { return _isVisible;    }
	public SortOrder getSortOrder()    { return _sortOrder;    }
	public int       getSortOrderPos() { return _sortOrderPos; }
	public int       getWidth()        { return _width;        }
	
	public ColumnHeaderPropsEntry()
	{
	}

	public ColumnHeaderPropsEntry(String name, int viewPos)
	{
		_colName      = name;
		_viewPos      = viewPos;
	}
//	public ColumnHeaderPropsEntry(String name, int viewPos, String afterColumnName)
//	{
//		_colName      = name;
//		_viewPos      = viewPos;
//		_afterColName = afterColumnName;
//	}

	public ColumnHeaderPropsEntry(String name, int viewPos, SortOrder sortOrder)
	{
		_colName      = name;
		_viewPos      = viewPos;
		_sortOrder    = sortOrder;
	}

	public ColumnHeaderPropsEntry(String name, int modelPos, int viewPos, boolean isVisible, SortOrder sortOrder, int sortOrderPos, int width)
	{
		_colName      = name;
		_modelPos     = modelPos;
		_viewPos      = viewPos;
		_isVisible    = isVisible;
		_sortOrder    = sortOrder;
		_sortOrderPos = sortOrderPos;
		_width        = width;
	}

	private static ColumnHeaderPropsEntry parse(boolean keyVal, String colName, String valStr)
	throws ParseException
	{
		ColumnHeaderPropsEntry e = new ColumnHeaderPropsEntry();
		valStr = valStr.trim();
		e._colName = colName;
		if (keyVal)
		{
			if (e._colName == null)
				e._colName = "";

			int startPos = valStr.indexOf("={");
			int endPos   = valStr.lastIndexOf("}");
			if (startPos == -1)
				throw new ParseException("Can't find '={' in the input string '"+valStr+"'.", 0);
			if (endPos == -1)
				throw new ParseException("Can't find ending '}' in the input string '"+valStr+"'.", 0);
			
			e._colName = valStr.substring(0, startPos);

			// strip off 'colName={p1=xxx,p2=yyy}' and leave the values inside the {} as the result
			// 'colName={p1=xxx,p2=yyy}' -> 'p1=xxx,p2=yyy'
			valStr = valStr.substring(startPos+2, endPos);
		}

		String[] strArr = valStr.split(",");
		for (int i=0; i<strArr.length; i++)
		{
			// each entry looks like: colName={modelPos=1,viewPos=1,isVisible=true,sort=unsorted}
			// where modelPos=int[0..999], viewPos=int[0..999], isVisible=boolean[true|false], sort=String[unsorted|ascending|descending]
			//
			strArr[i] = strArr[i].trim();

			_logger.trace("parse() colName='"+e._colName+"': i="+i+", keyVal='"+strArr[i]+"'.");

			String[] strKeyVal = strArr[i].split("=");
			if (strKeyVal.length < 2)
			{
				_logger.info("Faulty key=value representation '"+strArr[i]+"' at position '"+i+"' in the string '"+strArr[i]+"'.");
				continue;
			}
			String key = strKeyVal[0].trim();
			String val = strKeyVal[1].trim();

			if (key.equals("modelPos"))
			{
				try { e._modelPos = Integer.parseInt(val); }
				catch (NumberFormatException ignore) 
				{
					throw new ParseException("The value '"+val+"' for key '"+key+"' is not a number.", 0);
				}
			}
			else if (key.equals("viewPos"))
			{
				try { e._viewPos = Integer.parseInt(val); }
				catch (NumberFormatException ignore) 
				{
					throw new ParseException("The value '"+val+"' for key '"+key+"' is not a number.", 0);
				}
			}
			else if (key.equals("isVisible"))
			{
				// set to true even if val is unknown string (unknown = not true|false)
				e._isVisible = ! val.equalsIgnoreCase("false");
			}
			else if (key.equals("sortOrder"))
			{
				String soEntry[] = val.split(":");
				if (soEntry.length > 1)
				{
					val = soEntry[1];
					try { e._sortOrderPos = Integer.parseInt(soEntry[0]); } 
					catch (NumberFormatException ignore) 
					{
						e._sortOrderPos = -1;
						throw new ParseException("The Sort Order position value '"+soEntry[0]+"' for key '"+key+"' is not a number.", 0);
					}
				}
				if      (SortOrder.UNSORTED.toString().equals(val))   e._sortOrder = SortOrder.UNSORTED;
				else if (SortOrder.ASCENDING.toString().equals(val))  e._sortOrder = SortOrder.ASCENDING;
				else if (SortOrder.DESCENDING.toString().equals(val)) e._sortOrder = SortOrder.DESCENDING;
				else
					throw new ParseException("The value '"+val+"' for key '"+key+"' has to be '"+SortOrder.UNSORTED+"|"+SortOrder.ASCENDING+"|"+SortOrder.DESCENDING+"'.", 0);
			}
			else if (key.equals("width"))
			{
				try { e._width = Integer.parseInt(val); }
				catch (NumberFormatException ignore) 
				{
					throw new ParseException("The value '"+val+"' for key '"+key+"' is not a number.", 0);
				}
			}
//			else if (key.equals("afterColName"))
//			{
//				if (StringUtil.hasValue(val))
//					e._afterColName = val;
//			}
			else
				throw new ParseException("Unknown key value of '"+key+"' was found in the string '"+valStr+"'.", 0);
		}

		return e;
	}
	public static ColumnHeaderPropsEntry parseKeyValue(String propsStr)
	throws ParseException
	{
		return parse(true, null, propsStr);
	}

	public static ColumnHeaderPropsEntry parseValue(String colName, String propsStr)
	throws ParseException
	{
		return parse(false, colName, propsStr);
	}

	@Override
	public String toString()
	{
		return _colName
			+ "={modelPos="+ _modelPos
			+ ",viewPos="  + _viewPos
			+ ",isVisible="+ _isVisible
			+ ",sortOrder="+ ((_sortOrder == SortOrder.UNSORTED) ? _sortOrder : _sortOrderPos+":"+_sortOrder)
			+ ",width="    + _width
//			+ ( StringUtil.isNullOrBlank(_afterColName) ? "" : ",afterColName=" + _afterColName )
			+"}";
	}

	
	public static void main(String[] args)
	{
		ColumnHeaderPropsEntry p;
		String s;
		
		try 
		{ 
			s = "noSpace={modelPos=1,viewPos=1,isVisible=xxx,sortOrder=unsorted}";
			p = parseKeyValue(s); 
			System.out.println("SUCCEEDED: \n\t"+s+"\n\t"+p);

			s = "spaceInPropList={modelPos=1, viewPos=1, isVisible=false, sortOrder=ascending}"; 
			p = parseKeyValue(s); 
			System.out.println("SUCCEEDED: \n\t"+s+"\n\t"+p);

			s = "spaceKeyVal-1={modelPos =1, viewPos =1, isVisible =xxx, sortOrder =descending}"; 
			p = parseKeyValue(s); 
			System.out.println("SUCCEEDED: \n\t"+s+"\n\t"+p);

			s = "spaceKeyVal-2={modelPos= 1, viewPos= 1, isVisible= xxx, sortOrder= unsorted}"; 
			p = parseKeyValue(s); 
			System.out.println("SUCCEEDED: \n\t"+s+"\n\t"+p);

			s = "spaceKeyVal-3={modelPos = 1, viewPos = 1, isVisible = xxx, sortOrder = unsorted}"; 
			p = parseKeyValue(s); 
			System.out.println("SUCCEEDED: \n\t"+s+"\n\t"+p);
		}
		catch (ParseException e) { System.out.println("TEST FAILED:"); e.printStackTrace();}
	}
}
