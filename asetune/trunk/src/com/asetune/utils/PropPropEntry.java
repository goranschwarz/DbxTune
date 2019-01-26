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
package com.asetune.utils;

import java.text.ParseException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;

/**
 * This is a class that holds several 'properties' inside a single property
 * <p>
 *
 * One SubProp entry looks like:
 * <pre>nameX={prop1=val, prop2=val, prop3=val}</pre>
 * 
 * A full entry looks like:
 * <pre>name1={prop1=val, prop2=val, prop3=val}; name2={prop1=val, prop2=val, prop3=val}; name3={prop1=val,prop2=val,prop3=val};</pre>
 * 
 * TODO: Add methods get*Property() to the Entry subclass so it can be used individually<br>
 * Right now we can fool this class in the following way if you just want to parse 1 SUB entry
 * <pre>
 *      String str = "columnName=c1, sqlColumnNumber=1, parseColumnNumber=1, isNullable=true, description=The long description of this column";
 *      PropPropEntry ppe = new PropPropEntry("DUMMY={"+str+"}");
 * 
 *      String  columnName        = ppe.getProperty       ("DUMMY", "columnName",        "defaultValue");
 *      int     sqlColumnNumber   = ppe.getIntProperty    ("DUMMY", "sqlColumnNumber",   -1);
 *      int     parseColumnNumber = ppe.getIntProperty    ("DUMMY", "parseColumnNumber", -1);
 *      boolean isNullable        = ppe.getBooleanProperty("DUMMY", "isNullable",        true);
 *      String  description       = ppe.getProperty       ("DUMMY", "description",       "");
 * </pre>
 * 
 * @author gorans
 *
 */
public class PropPropEntry
implements Iterable<String>
{
	private static Logger _logger = Logger.getLogger(PropPropEntry.class);

	private LinkedHashMap<String, Entry> _keys = new LinkedHashMap<String, Entry>();

	@Override
	public Iterator<String> iterator()
	{
		return _keys.keySet().iterator();
	}

	public PropPropEntry()
	{
	}
	public PropPropEntry(String confVal)
	{
		parse(confVal);
	}
	public void parse(String confVal)
	{
		// split on '; ' and stuff the entries in a Map object
		String[] strArr = confVal.split(";");
		for (int i=0; i<strArr.length; i++)
		{
			String strEntry = strArr[i].trim();
			if (StringUtil.isNullOrBlank(strEntry))
				continue;

			try 
			{
				// each entry looks like: colName={modelPos=1,viewPos=1,isVisible=true,sort=unsorted}
				// where modelPos=int[0..999], viewPos=int[0..999], isVisible=boolean[true|false], sort=String[unsorted|ascending|descending]
//				ColumnHeaderPropsEntry chpe = ColumnHeaderPropsEntry.parseKeyValue(strArr[i]);
//				colProps.put(chpe._colName, chpe);

				Entry entry = new Entry(strEntry);
				_keys.put(entry._propName, entry);
			}
			catch (ParseException e)
			{
				_logger.info("Problems parsing '"+confVal+"' with string '"+strEntry+"'. Caught: "+e);
				_logger.debug("Problems parsing '"+confVal+"' with string '"+strEntry+"'. Caught: "+e, e);
				continue;
			}
		}	
	}

	/**
	 * Put a Properties object of key/values in the props wrapper 
	 * @param mainKeyName
	 * @param props
	 */
	public void put(String mainKeyName, Properties props)
	{
		Entry entry = new Entry(mainKeyName, props);
		_keys.put(mainKeyName, entry);
	}
	
	/**
	 * Put a Map of key/values in the props wrapper 
	 * @param mainKeyName
	 * @param map
	 */
	public void put(String mainKeyName, Map<String, String> map)
	{
		Entry entry = new Entry(mainKeyName, map);
		_keys.put(mainKeyName, entry);
	}

	/**
	 * Put a key,value under a specific main property 
	 * @param mainKeyName
	 * @param subKeyName
	 * @param value
	 */
	public void put(String mainKeyName, String subKeyName, String value)
	{
		Entry entry = _keys.get(mainKeyName);
		if (entry == null)
		{
			entry = new Entry();
			entry.setName(mainKeyName);
		}
		entry.put(subKeyName, value);
		_keys.put(mainKeyName, entry);
	}

	/**
	 * Get a Set of all main keys in this object
	 * @return
	 */
	public Set<String> keySet()
	{
		return _keys.keySet();
	}

	/**
	 * Get a Map representation of the Entry for a specific key
	 * @param name
	 * @return a Map object
	 */
	public Map<String, String> getPropertyMap(String name)
	{
		Entry entry = _keys.get(name);
		if (entry == null)
			return null;

		return entry;
	}

	/**
	 * Get a Properties representation of the Entry for a specific key
	 * @param name
	 * @return a Properties object
	 */
	public Properties getPropertyProp(String name)
	{
		Entry entry = _keys.get(name);
		if (entry == null)
			return null;

		return entry.toProperties();
	}

	/**
	 * Get a String representation of the Entry for a specific key
	 * @param name
	 * @return Something like: key={subKey1=val1,subKey2=val2,subKey3=val3}
	 */
	public String getPropertyString(String name)
	{
		Entry entry = _keys.get(name);
		if (entry == null)
			return null;

		return entry.toString();
	}





	/** Does the property exists within the configuration ? */
	public boolean hasProperty(String mainKey, String propName)
	{
		return getProperty(mainKey, propName) != null;
	}

	/** Get a int value for property */
	public int getIntMandatoryProperty(String mainKey, String propName)
	throws MandatoryPropertyException
	{
		String val = getProperty(mainKey, propName);
		if (val == null)
			throw new MandatoryPropertyException("The property '"+mainKey+"', '"+propName+"' is mandatory.");
		try
		{
			return Integer.parseInt(val);
		}
		catch (NumberFormatException e)
		{
			throw new NumberFormatException("The property '"+mainKey+"', '"+propName+"' must be a number. I found value '"+val+"'.");
		}
	}
	/** Get a int value for property */
	public int getIntProperty(String mainKey, String propName)
	{
		String val = getProperty(mainKey, propName);
		return Integer.parseInt(val);
	}
	/** Get a int value for property */
	public int getIntProperty(String mainKey, String propName, int defaultValue)
	{
		String val = getProperty(mainKey, propName, Integer.toString(defaultValue));
		return Integer.parseInt(val);
	}
	/** Get a int value for property */
	public int getIntProperty(String mainKey, String propName, String defaultValue)
	{
		String val = getProperty(mainKey, propName, defaultValue);
		return Integer.parseInt(val);
	}




	/** Get a long value for property */
	public long getLongMandatoryProperty(String mainKey, String propName)
	throws MandatoryPropertyException
	{
		String val = getProperty(mainKey, propName);
		if (val == null)
			throw new MandatoryPropertyException("The property '"+mainKey+"', '"+propName+"' is mandatory.");
		return Long.parseLong(val);
	}
	/** Get a long value for property */
	public long getLongProperty(String mainKey, String propName)
	{
		String val = getProperty(mainKey, propName);
		return Long.parseLong(val);
	}
	/** Get a long value for property */
	public long getLongProperty(String mainKey, String propName, long defaultValue)
	{
		String val = getProperty(mainKey, propName, Long.toString(defaultValue));
		return Long.parseLong(val);
	}
	/** Get a long value for property */
	public long getLongProperty(String mainKey, String propName, String defaultValue)
	{
		String val = getProperty(mainKey, propName, defaultValue);
		return Long.parseLong(val);
	}




	/** Get a boolean value for property */
	public boolean getBooleanMandatoryProperty(String mainKey, String propName)
	throws MandatoryPropertyException
	{
		String val = getProperty(mainKey, propName);
		if (val == null)
			throw new MandatoryPropertyException("The property '"+mainKey+"', '"+propName+"' is mandatory.");
		return val.equalsIgnoreCase("true");
	}
	/** Get a boolean value for property */
	public boolean getBooleanProperty(String mainKey, String propName, boolean defaultValue)
	{
		String val = getProperty(mainKey, propName, Boolean.toString(defaultValue));
		if (val == null)
			return false;
		return val.equalsIgnoreCase("true");
	}
	/** Get a boolean value for property */
	public boolean getBooleanProperty(String mainKey, String propName, String defaultValue)
	{
		String val = getProperty(mainKey, propName, defaultValue);
		if (val == null)
			return false;
		return val.equalsIgnoreCase("true");
	}



	/** Get a String value for property */
	public String getMandatoryProperty(String mainKey, String propName)
	throws MandatoryPropertyException
	{
		String val = getProperty(mainKey, propName);
		if (val == null)
			throw new MandatoryPropertyException("The property '"+mainKey+"', '"+propName+"' is mandatory.");
		val = val.trim();
		return val;
	}

	/** Get a String value for property */
	public String getProperty(String mainKey, String propName)
	{
		Entry entry = _keys.get(mainKey);
		if (entry == null)
			return null;
		
		String val = entry.get(propName);
		if (val != null)
			val = val.trim();
		return val;
	}

	/** Get a String value for property */
	public String getProperty(String mainKey, String propName, String defaultValue)
	{
		String val = getProperty(mainKey, propName);
		if (val == null)
			val = defaultValue;
		return val;
	}







	@Override
	public String toString()
	{
		if (_keys.size() == 0)
			return "";

		StringBuilder sb = new StringBuilder();
		for (Entry entry : _keys.values())
		{
			sb.append(entry.toString()).append("; ");
		}

//		for (Map.Entry<String,Entry> entry : _keys.entrySet()) 
//		{
//			String key = entry.getKey();
//			Entry  val = entry.getValue();
//
//			sb.append(key).append("={").append(val.toString()).append("}; ");
//		}
		// Take away last '; '
		return sb.substring(0, sb.length()-2);
	}
	
	public String toString(int keyLength, int subValueLength)
	{
		if (_keys.size() == 0)
			return "";

		StringBuilder sb = new StringBuilder();
		for (Entry entry : _keys.values())
		{
			sb.append(entry.toString(keyLength, subValueLength)).append("; \n");
		}

		// Take away last '; \n'
		return sb.substring(0, sb.length()-3);
	}

	/**
	 * Is this PPE equal to another PPE
	 * <p>
	 * This equal checks the individual properties, so the add order doesn't matter.<br>
	 * <ul>
	 *   <li>Check if object is the same</li>
	 *   <li>Check if size differs</li>
	 *   <li>Check all key in this object exists in compareObject</li>
	 *   <li>Check all sub properties exists and has same values</li>
	 * </ul>
	 */
	@Override
	public boolean equals(Object anObject) 
	{
		if (this == anObject) 
		{
			return true;
		}

		// Check the details in PPE
		if ( anObject instanceof PropPropEntry )
		{
			PropPropEntry ppe = (PropPropEntry) anObject;
			
			// If size differ, THEN: not equal
			if ( this._keys.size() != ppe._keys.size() )
				return false;

			// LOOP keys from this->compareObject
			for (String key : _keys.keySet())
			{
				Entry thisEntry = _keys.get(key);
				Entry compEntry = ppe._keys.get(key);
				
				// Key can't be found in compareObject, THEN: not equal
				if (compEntry == null)
					return false;

				// If sub entry size differ, THEN: not equal
				if (thisEntry.size() != compEntry.size())
					return false;
				
				// check all sub entries this->compareObject
				for (String subKey : thisEntry.keySet())
				{
					String thisSubValue = thisEntry.get(subKey);
					String compSubValue = compEntry.get(subKey);

					// Key can't be found, THEN: not equal
					if (compSubValue == null)
						return false;
					
					// value is NOT the same
					if ( ! thisSubValue.equals(compSubValue) )
						return false;
				}
				
			}

			return true;
		}

		return false;
	}


	//////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////
	// SUBCLASS // SUBCLASS // SUBCLASS // SUBCLASS // SUBCLASS // SUBCLASS //
	//////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////
	public class Entry
	extends LinkedHashMap<String, String>
	{
		private static final long serialVersionUID = 1L;
		private String _propName = null;

		public void   setName(String name) { _propName = name; }
		public String getName()	           { return _propName; }

		public Entry()
		{
		}
		
		public Entry(String name, Map<String, String> map)
		{
			_propName = name;
			this.putAll(map);
		}
		
		public Entry(String name, Properties props)
		{
			_propName = name;

			Enumeration e = props.propertyNames();
			while (e.hasMoreElements()) 
			{
				String key = (String) e.nextElement();
				this.put(key, props.getProperty(key));
			}
		}
		
		public Entry(boolean keyVal, String propName, String valStr)
		throws ParseException
		{
//			Entry e = new Entry();
			Entry e = this;
			valStr = valStr.trim();
			e._propName = propName;
			if (keyVal)
			{
				if (e._propName == null)
					e._propName = "";

				int startPos = valStr.indexOf("={");
				int endPos   = valStr.lastIndexOf("}");
				if (startPos == -1)
					throw new ParseException("Can't find '={' in the input string '"+valStr+"'.", 0);
				if (endPos == -1)
					throw new ParseException("Can't find ending '}' in the input string '"+valStr+"'.", 0);
				
				e._propName = valStr.substring(0, startPos).trim();

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

				_logger.trace("parse() colName='"+e._propName+"': i="+i+", keyVal='"+strArr[i]+"'.");

				String[] strKeyVal = strArr[i].split("=");
				if (strKeyVal.length < 2)
				{
					_logger.info("Faulty key=value representation '"+strArr[i]+"' at position '"+i+"' in the string '"+strArr[i]+"'.");
					continue;
				}
				String key = strKeyVal[0].trim();
				String val = strKeyVal[1].trim();

				// Check different, properties for numbers...
				// TODO: maybe take a Property/HashMap with names that should be checked with some algorithm

//				if (key.equals("modelPos"))
//				{
//					try { e._modelPos = Integer.parseInt(val); }
//					catch (NumberFormatException ignore) 
//					{
//						throw new ParseException("The value '"+val+"' for key '"+key+"' is not a number.", 0);
//					}
//				}
//				else if (key.equals("viewPos"))
//				{
//					try { e._viewPos = Integer.parseInt(val); }
//					catch (NumberFormatException ignore) 
//					{
//						throw new ParseException("The value '"+val+"' for key '"+key+"' is not a number.", 0);
//					}
//				}
//				else if (key.equals("isVisible"))
//				{
//					// set to true even if val is unknown string (unknown = not true|false)
//					e._isVisible = ! val.equalsIgnoreCase("false");
//				}
//				else if (key.equals("sortOrder"))
//				{
//					String soEntry[] = val.split(":");
//					if (soEntry.length > 1)
//					{
//						val = soEntry[1];
//						try { e._sortOrderPos = Integer.parseInt(soEntry[0]); } 
//						catch (NumberFormatException ignore) 
//						{
//							e._sortOrderPos = -1;
//							throw new ParseException("The Sort Order position value '"+soEntry[0]+"' for key '"+key+"' is not a number.", 0);
//						}
//					}
//					if      (SortOrder.UNSORTED.toString().equals(val))   e._sortOrder = SortOrder.UNSORTED;
//					else if (SortOrder.ASCENDING.toString().equals(val))  e._sortOrder = SortOrder.ASCENDING;
//					else if (SortOrder.DESCENDING.toString().equals(val)) e._sortOrder = SortOrder.DESCENDING;
//					else
//						throw new ParseException("The value '"+val+"' for key '"+key+"' has to be '"+SortOrder.UNSORTED+"|"+SortOrder.ASCENDING+"|"+SortOrder.DESCENDING+"'.", 0);
//				}
//				else
//					throw new ParseException("Unknown key value of '"+key+"' was found in the string '"+valStr+"'.", 0);

				// now STORE the value...
				e.put(key, val);
			}

			//return e;
		}
//		public static ColumnHeaderPropsEntry parseKeyValue(String propsStr)
//		throws ParseException
//		{
//			return parse(true, null, propsStr);
//		}
//
//		public static ColumnHeaderPropsEntry parseValue(String colName, String propsStr)
//		throws ParseException
//		{
//			return parse(false, colName, propsStr);
//		}

		public Entry(String propsStr)
		throws ParseException
		{
			this(true, null, propsStr);
		}

		public Entry(String name, String propsStr)
		throws ParseException
		{
			this(false, name, propsStr);
		}
		
		@Override
		public String toString()
		{
			StringBuilder sb = new StringBuilder();

			if (_propName != null)
				sb.append(_propName).append("={");

			for (Map.Entry<String,String> entry : this.entrySet()) 
			{
				String key = entry.getKey();
				String val = entry.getValue();

				sb.append(key).append("=").append(val).append(",");
			}
			sb.deleteCharAt(sb.length()-1);

			if (_propName != null)
				sb.append("}");

			return sb.toString();
		}

		public String toString(int keyLength, int subValueLength)
		{
			StringBuilder sb = new StringBuilder();

			if (_propName != null)
				sb.append(StringUtil.left(_propName, keyLength, true)).append("={");

			for (Map.Entry<String,String> entry : this.entrySet()) 
			{
				String key = entry.getKey();
				String val = entry.getValue();

				sb.append(key).append("=").append(StringUtil.left(val, subValueLength, true)).append(",");
			}
			sb.deleteCharAt(sb.length()-1);

			if (_propName != null)
				sb.append("}");

			return sb.toString();
		}

		public Map<String, String> getMap()
		{
			return this;
		}

		public Properties toProperties()
		{
			Properties props = new Properties();
			for (Map.Entry<String,String> entry : this.entrySet()) 
			{
				String key = entry.getKey();
				String val = entry.getValue();

				props.setProperty(key, val);
			}
			return props;
		}
	}

	////////////////////////////////////////////////////////////
	// TEST
	////////////////////////////////////////////////////////////
	public static void main(String[] args)
	{
		PropPropEntry ppe = null;

		ppe = new PropPropEntry("Cm1={p1=1, p2=2, p3=3};Cm2={p1=1,p2=2,p3=3}; Cm3={ p1 = 1, p2 = 2, p3 = 3 };");
		System.out.println("Test1: '"+ppe+"'.");
		System.out.println("Test1.getPropertyProp(Cm1): '"+ppe.getPropertyProp("Cm1")+"'.");
		System.out.println();
		
//		ppe = new PropPropEntry("{p1=1,p2=2,p3=3};{p1=1,p2=2,p3=3};");
//		System.out.println("Test1: '"+ppe+"'.");
//		System.out.println("Test1.getProperty(Cm1): '"+ppe.getProperty("Cm1")+"'.");
//		System.out.println();
		
		ppe = new PropPropEntry("DBID={modelPos=0,viewPos=0,isVisible=true,sortOrder=UNSORTED}; ObjectID={modelPos=1,viewPos=1,isVisible=true,sortOrder=UNSORTED}; IndexID={modelPos=2,viewPos=2,isVisible=true,sortOrder=UNSORTED}; DBName={modelPos=3,viewPos=3,isVisible=true,sortOrder=UNSORTED}; ObjectName={modelPos=4,viewPos=4,isVisible=true,sortOrder=UNSORTED}; ObjectType={modelPos=5,viewPos=5,isVisible=true,sortOrder=UNSORTED}; PartitionID={modelPos=6,viewPos=6,isVisible=true,sortOrder=UNSORTED}; PartitionName={modelPos=7,viewPos=7,isVisible=true,sortOrder=UNSORTED}; TotalSizeKB={modelPos=8,viewPos=8,isVisible=true,sortOrder=UNSORTED}; CachedKB={modelPos=9,viewPos=9,isVisible=true,sortOrder=UNSORTED}; CacheName={modelPos=10,viewPos=10,isVisible=true,sortOrder=UNSORTED}");
		System.out.println("Test2: '"+ppe+"'.");
		System.out.println();

		ppe = new PropPropEntry();
		ppe.put("CM1", "p1", "v1");
		ppe.put("CM1", "p2", "v2");
		ppe.put("CM1", "p3", "v3");
		ppe.put("CM2", "p1", "v1");
		ppe.put("CM2", "p2", "v2");
		System.out.println("Test3: '"+ppe+"'.");
		System.out.println();

		ppe = new PropPropEntry("Objects={postpone=0,paused=false,bg=false,resetNC20=true,storePcs=false,pcsAbs=true,pcsDiff=true,pcsRate=true}; Processes={postpone=0,paused=false,bg=false,resetNC20=true,storePcs=false,pcsAbs=true,pcsDiff=true,pcsRate=true}; Databases={postpone=0,paused=false,bg=false,resetNC20=true,storePcs=false,pcsAbs=true,pcsDiff=true,pcsRate=true}; Temp Db={postpone=0,paused=false,bg=false,resetNC20=true,storePcs=false,pcsAbs=true,pcsDiff=true,pcsRate=true}; Waits={postpone=0,paused=false,bg=false,resetNC20=true,storePcs=false,pcsAbs=true,pcsDiff=true,pcsRate=true}; Engines={postpone=0,paused=false,bg=true,resetNC20=true,storePcs=false,pcsAbs=true,pcsDiff=true,pcsRate=true}; System Load={postpone=0,paused=false,bg=false,resetNC20=false,storePcs=false,pcsAbs=true,pcsDiff=true,pcsRate=true}; Data Caches={postpone=0,paused=false,bg=true,resetNC20=true,storePcs=false,pcsAbs=true,pcsDiff=true,pcsRate=true}; Pools={postpone=0,paused=false,bg=false,resetNC20=true,storePcs=false,pcsAbs=true,pcsDiff=true,pcsRate=true}; Devices={postpone=0,paused=false,bg=false,resetNC20=true,storePcs=false,pcsAbs=true,pcsDiff=true,pcsRate=true}; IO Sum={postpone=0,paused=false,bg=true,resetNC20=true,storePcs=false,pcsAbs=true,pcsDiff=true,pcsRate=true}; IO Queue={postpone=0,paused=false,bg=true,resetNC20=true,storePcs=false,pcsAbs=true,pcsDiff=true,pcsRate=true}; Spinlock Sum={postpone=300,paused=false,bg=false,resetNC20=true,storePcs=false,pcsAbs=true,pcsDiff=true,pcsRate=true}; Sysmon Raw={postpone=300,paused=false,bg=false,resetNC20=true,storePcs=false,pcsAbs=true,pcsDiff=true,pcsRate=true}; RepAgent={postpone=300,paused=false,bg=false,resetNC20=true,storePcs=false,pcsAbs=true,pcsDiff=true,pcsRate=true}; Cached Procedures={postpone=0,paused=false,bg=false,resetNC20=true,storePcs=false,pcsAbs=true,pcsDiff=true,pcsRate=true}; Procedure Cache={postpone=0,paused=false,bg=true,resetNC20=true,storePcs=false,pcsAbs=true,pcsDiff=true,pcsRate=true}; Procedure Call Stack={postpone=0,paused=false,bg=false,resetNC20=true,storePcs=false,pcsAbs=true,pcsDiff=true,pcsRate=true}; Cached Objects={postpone=600,paused=false,bg=false,resetNC20=false,storePcs=false,pcsAbs=true,pcsDiff=true,pcsRate=true}; Errorlog={postpone=0,paused=false,bg=false,resetNC20=true,storePcs=false,pcsAbs=true,pcsDiff=false,pcsRate=false}; Deadlock={postpone=0,paused=false,bg=false,resetNC20=true,storePcs=false,pcsAbs=true,pcsDiff=false,pcsRate=false}; Proc Cache Module Usage={postpone=0,paused=false,bg=false,resetNC20=false,storePcs=false,pcsAbs=true,pcsDiff=true,pcsRate=true}; Proc Cache Memory Usage={postpone=0,paused=false,bg=false,resetNC20=false,storePcs=false,pcsAbs=true,pcsDiff=true,pcsRate=true}; Statement Cache={postpone=0,paused=false,bg=false,resetNC20=false,storePcs=false,pcsAbs=true,pcsDiff=true,pcsRate=true}; Statement Cache Details={postpone=0,paused=false,bg=false,resetNC20=false,storePcs=false,pcsAbs=true,pcsDiff=true,pcsRate=true}; Active Objects={postpone=0,paused=false,bg=false,resetNC20=false,storePcs=false,pcsAbs=true,pcsDiff=true,pcsRate=true}; Active Statements={postpone=0,paused=false,bg=false,resetNC20=true,storePcs=false,pcsAbs=true,pcsDiff=true,pcsRate=true}; Blocking={postpone=0,paused=false,bg=false,resetNC20=false,storePcs=false,pcsAbs=true,pcsDiff=true,pcsRate=true}; Missing Statistics={postpone=0,paused=false,bg=false,resetNC20=false,storePcs=false,pcsAbs=true,pcsDiff=true,pcsRate=true}; sp_monitorconfig={postpone=600,paused=false,bg=false,resetNC20=false,storePcs=false,pcsAbs=true,pcsDiff=true,pcsRate=true}");
		System.out.println("TestX: '"+ppe+"'.");
		System.out.println();
	}
}

