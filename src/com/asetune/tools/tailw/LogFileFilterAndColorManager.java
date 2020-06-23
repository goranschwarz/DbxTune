/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
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
package com.asetune.tools.tailw;

import java.awt.Color;
import java.io.File;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Icon;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.asetune.Version;
import com.asetune.utils.FileUtils;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;

public class LogFileFilterAndColorManager
{
	private static Logger _logger = Logger.getLogger(LogFileFilterAndColorManager.class);

	private static LogFileFilterAndColorManager _instance = null;
	private String _filename = null;
	
	private LinkedHashMap<String, Set<FilterEntry>> _groups = new LinkedHashMap<>();
	private Set<FilterEntry> _currentGroup = new HashSet<>();
	private String           _currentGroupName = null;
	
	private static FilterEntry _allow = new FilterEntry("allow all", ".*", null, null, null, null, Action.Allow);

	public static final String DEFAULT_FILTER_GROUP = "No Filter";
	
	public static final String DEFAULT_filename = System.getProperty("user.home") + "/.asetune/LogFileFilters.xml";
	
	public static final Icon FILTER_ROW_DISCARDED_ICON = SwingUtils.readImageIcon(Version.class, "images/filter_row_discarded_16.png");
	public static final Icon FILTER_ROW_INFO_ICON      = SwingUtils.readImageIcon(Version.class, "images/filter_row_information_16.png");
	public static final Icon FILTER_ROW_WARNING_ICON   = SwingUtils.readImageIcon(Version.class, "images/filter_row_warning_16.png");
	public static final Icon FILTER_ROW_ERROR_ICON     = SwingUtils.readImageIcon(Version.class, "images/filter_row_error_16.png");

	public static LogFileFilterAndColorManager getInstance()
	{
		if (_instance == null)
			_instance = new LogFileFilterAndColorManager();
		
		return _instance;
	}

	public static LogFileFilterAndColorManager setInstance(LogFileFilterAndColorManager instance)
	{
		LogFileFilterAndColorManager current = _instance;
		_instance = instance;
		return current;
	}

	public static boolean hasInstance()
	{
		return _instance != null;
	}


	public LogFileFilterAndColorManager()
	{
		this(DEFAULT_filename);
	}
	public LogFileFilterAndColorManager(String filename)
	{
		_filename = filename;
		loadFile();
	}

	private String getfilterElement(Element eElement, String tagname)
	{
		NodeList nl = eElement.getElementsByTagName(tagname);
		if (nl.getLength() > 0)
		{
			return nl.item(0).getTextContent();
		}
		return null;
	}
	private void loadFile()
	{
		System.out.println("loadFile(): _filename='"+_filename+"'.");
		File xmlFile = new File(_filename);
		String xmlFileContent = null;
		try
		{
			_groups = new LinkedHashMap<>();
			
			_groups.put(DEFAULT_FILTER_GROUP, new HashSet<FilterEntry>());
			
			if ( ! xmlFile.exists() )
			{
//				_logger.info("The LogFile Filter xml file '"+xmlFile+"' did NOT exists. So no filters will be loaded.");
				
				String classPathFileName = "resources/LogFileFilters.xml";
				_logger.info("The LogFile Filter xml file '"+xmlFile+"' did NOT exists. Trying '"+classPathFileName+"' from the classpath. You can copy the file from $DBXTUNE_HOME/lib/asetune.jar:"+classPathFileName);
				xmlFileContent = FileUtils.readFile(Version.class, classPathFileName);
				xmlFile = null;

				if (StringUtil.isNullOrBlank(xmlFileContent))
				{
					_logger.warn("The LogFile Filter xml file '"+xmlFile+"' did NOT exists. Nor did I find content in the resource '"+classPathFileName+"'. So no filters will be loaded.");
					return;
				}
			}

			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = xmlFile != null ? dBuilder.parse(xmlFile) : dBuilder.parse(IOUtils.toInputStream(xmlFileContent, "UTF-8"));

			// optional, but recommended, read this: http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
			doc.getDocumentElement().normalize();

			System.out.println("Root element :" + doc.getDocumentElement().getNodeName());

			NodeList filters = doc.getElementsByTagName("Filter");

			System.out.println("----------------------------");

			for (int i=0; i<filters.getLength(); i++)
			{
				Node filter = filters.item(i);

//				System.out.println("\nfilters :" + filters.getNodeName());

				String filterGroupName = ((Element)filter.getParentNode()).getAttribute("name");
				
				if ( filter.getNodeType() == Node.ELEMENT_NODE )
				{

					Element eElement = (Element) filter;

					String nameStr    = getfilterElement(eElement, "name");
					String regExpStr  = getfilterElement(eElement, "regExp");
					String actionStr  = getfilterElement(eElement, "action");
					String fgColorStr = getfilterElement(eElement, "fgColor");
					String bgColorStr = getfilterElement(eElement, "bgColor");
					String iconStr    = getfilterElement(eElement, "icon");
					String level      = getfilterElement(eElement, "level");

					System.out.println("filterGroupName=|"+filterGroupName+"|.");
					System.out.println("    name   =|"+nameStr   +"|");
					System.out.println("    regExp =|"+regExpStr +"|");
					System.out.println("    action =|"+actionStr +"|");
					System.out.println("    fgColor=|"+fgColorStr+"|");
					System.out.println("    bgColor=|"+bgColorStr+"|");
					//FilterEntry fe = new FilterEntry(name, regexp, fgColor, bgColor, action);
					
					Action action = Action.Allow;
					try { action = Action.valueOf(actionStr); }
					catch (Throwable th) { _logger.error("Not a valid action in FilterGroup='"+filterGroupName+"', FilterName='"+nameStr+"'. The action='"+actionStr+"' is not a valid action. Setting the action to 'Allow'"); }

					Set<FilterEntry> group = _groups.get(filterGroupName);
					if (group == null)
					{
						group = new HashSet<>();
						_groups.put(filterGroupName, group);
					}
					group.add(new FilterEntry(nameStr, regExpStr, null, null, iconStr, level, action));
				}
			}
		}
		catch (Exception ex)
		{
			_logger.error("Problems reading the XML file '"+xmlFile+"'. Caught: "+ex, ex);
		}
	}
	
	public boolean setFilterGroup(String name)
	{
		_currentGroup = _groups.get(name);
		_currentGroupName = _currentGroup != null ? name : null;
		return _currentGroup != null;
	}
	
	public String getCurrentFilterGroupName()
	{
		return _currentGroupName;
	}
	
	public Set<String> getFilterGroups()
	{
		return _groups.keySet();
	}
	
	public FilterEntry getFilterEntryForRow(String row)
	{
		if (_currentGroup == null)
			return _allow;

		for (FilterEntry filterEntry : _currentGroup)
		{
			if (filterEntry.matchesRow(row))
				return filterEntry;
		}
		return _allow;
	}

	public enum Action
	{
		Allow, Discard
	};

	public enum Level
	{
		None, Discard, Info, Error, Warning
	};

	public static class FilterEntry
	{
		private String _name;
		
		private String _regExpStr;
		private Pattern _pattern;

		private Color  _fgColor;
		private Color  _bgColor;
		
		private Icon   _icon;
		private Level  _level;
		
		private Action _action;

		public String getName()    { return _name;  }
		public String getRegExp()  { return _regExpStr;  }
		public Action getAction()  { return _action;  }
		public Color  getFgColor() { return _fgColor; }
		public Color  getBgColor() { return _bgColor; }
		public Icon   getIcon()    { return _icon; }
		public Level  getLevel()   { return _level; }
		
		public boolean isAllowed()
		{
			return _action.equals(Action.Allow);
		}

		public FilterEntry(String regExp)
		{
			_regExpStr = regExp;
			_pattern = Pattern.compile(_regExpStr);
		}
		
		private Icon createIcon(String iconStr)
		{
			if (iconStr == null)
				return null;

			if      ("NONE"   .equalsIgnoreCase(iconStr)) return null;
			else if ("DISCARD".equalsIgnoreCase(iconStr)) return FILTER_ROW_DISCARDED_ICON;
			else if ("INFO "  .equalsIgnoreCase(iconStr)) return FILTER_ROW_INFO_ICON;
			else if ("ERROR"  .equalsIgnoreCase(iconStr)) return FILTER_ROW_ERROR_ICON;
			else if ("WARNING".equalsIgnoreCase(iconStr)) return FILTER_ROW_WARNING_ICON;
			else
			{
				return null;
//				SwingUtils.readImageIcon(clazz, filename)
			}
		}

		private Level createLevel(String levelStr)
		{
			if (levelStr == null)
				return Level.None;

			if      ("NONE"   .equalsIgnoreCase(levelStr)) return Level.None;
			else if ("DISCARD".equalsIgnoreCase(levelStr)) return Level.Discard;
			else if ("INFO"   .equalsIgnoreCase(levelStr)) return Level.Info;
			else if ("ERROR"  .equalsIgnoreCase(levelStr)) return Level.Error;
			else if ("WARNING".equalsIgnoreCase(levelStr)) return Level.Warning;
			else
			{
				return Level.None;
			}
		}

		public FilterEntry(String name, String regexp, Color fgColor, Color bgColor, String icon, String level, Action action)
		{
			_name      = name; 
			_regExpStr = regexp;
			_pattern   = Pattern.compile(_regExpStr);

			_fgColor   = fgColor;
			_bgColor   = bgColor;
			
			_icon      = createIcon(icon);
			_level     = createLevel(StringUtil.hasValue(level) ? level : icon);
			
			_action    = action;
		}

		private boolean matchesRow(String input)
		{
			Matcher m = _pattern.matcher(input);
			return m.find();
		}

	}
}
