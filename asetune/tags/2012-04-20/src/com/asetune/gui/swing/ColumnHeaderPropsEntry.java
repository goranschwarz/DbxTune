package com.asetune.gui.swing;

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

	private static Logger _logger = Logger.getLogger(ColumnHeaderPropsEntry.class);

	protected String    _colName      = null;
	protected int       _modelPos     = -1;
	protected int       _viewPos      = -1;
	protected boolean   _isVisible    = true;
	protected SortOrder _sortOrder    = SortOrder.UNSORTED;
	protected int       _sortOrderPos = -1;
	protected int       _width        = -1;

	public ColumnHeaderPropsEntry()
	{
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

	public String toString()
	{
		return _colName
			+ "={modelPos="+ _modelPos
			+ ",viewPos="  + _viewPos
			+ ",isVisible="+ _isVisible
			+ ",sortOrder="+ ((_sortOrder == SortOrder.UNSORTED) ? _sortOrder : _sortOrderPos+":"+_sortOrder)
			+ ",width="    + _width
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
