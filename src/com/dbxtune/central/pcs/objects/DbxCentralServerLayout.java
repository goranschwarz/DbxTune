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
package com.dbxtune.central.pcs.objects;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.central.pcs.CentralPersistReader;
import com.dbxtune.utils.StringUtil;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder(value = {"type", "options", "text", "enties", "srvSession"}, alphabetic = true)
@JsonInclude(Include.NON_NULL)
public class DbxCentralServerLayout
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private String  _type;
	private Map<String, String>  _options;
	private String  _text;
	private List<DbxCentralServerLayout>  _entries;
	private DbxCentralSessions  _srvSession;


	public String                        getType()    { return _type ;  } // group, label, server
	public Map<String, String>           getOptions() { return _options  ;  }
	public String                        getText()    { return _text;  }
	public List<DbxCentralServerLayout>  getEntries() { return _entries;  }
	public DbxCentralSessions            getSrvSession() { return _srvSession;  }

	public void setType    (String  type)                         { _type    = type ; }
	public void setOptions (Map<String, String> options)          { _options = options; }
	public void setText    (String  text)                         { _text    = text; }
	public void setEntries (List<DbxCentralServerLayout> entries) { _entries = entries; }
	public void setSrvSession(DbxCentralSessions srvSession)      { _srvSession = srvSession; }

	public boolean isGroupEntry()  { return "group" .equals(_type); }
	public boolean isLabelEntry()  { return "label" .equals(_type); }
	public boolean isServerEntry() { return "server".equals(_type); }
	
	private DbxCentralServerLayout(String type, Map<String, String> options, String text, List<DbxCentralServerLayout> entries, DbxCentralSessions srvSession)
	{
		super();

		_type       = type;
		_options    = options;
		_text       = text;
		_entries    = entries;
		_srvSession = srvSession;
	}

	public static DbxCentralServerLayout createGroupEntry(String label, Map<String, String> options, List<DbxCentralServerLayout> groupList)
	{
		return new DbxCentralServerLayout("group", options, label, groupList, null);
	}

	public static DbxCentralServerLayout createLabelEntry(String label, Map<String, String> options)
	{
		return new DbxCentralServerLayout("label", options, label, null, null);
	}

	public static DbxCentralServerLayout createServerEntry(String srvName, DbxCentralSessions srvSession)
	{
		return new DbxCentralServerLayout("server", null, srvName, null, srvSession);
	}
	

//	public static List<DbxCentralServerLayout> getFromFile()
//	throws IOException
//	{
//		return getFromFile(null);
//	}

	/**
	 * Get ALL Server Sessions (from the 'root'), in the order they were added. 
	 * @param root  The "root" entry from where we want to traverse the tree structure
	 * @return A list of DbxCentralSessions ordered in the "correct" order...
	 */
	public static List<DbxCentralSessions> getServerSessions(List<DbxCentralServerLayout> root)
	{
		List<DbxCentralSessions> list = new ArrayList<>();

		for (DbxCentralServerLayout layoutEntry : root)
		{
			if (layoutEntry.isGroupEntry())
			{
				list.addAll( getServerSessions(layoutEntry.getEntries()) );
			}
			else if (layoutEntry.isLabelEntry())
			{
				
			}
			else if (layoutEntry.isServerEntry())
			{
				list.add( layoutEntry.getSrvSession() );
			}
		}
		
		return list;
	}
	
	//--------------------------------------------------------------------------
	// Server GROUP lookups: serverName -> groupName
	//
	// Groups ONLY exists as '#FORMAT; GROUP; <name>' lines in 'conf/SERVER_LIST'.
	// They are NOT stored in the database, and a Collector has NO idea what group
	// it belongs to (the startup scripts strips all comment lines), hence DbxCentral
	// has to be the one resolving this.
	//
	// The map is built from a FILE-ONLY parse -> getFromFile(filename, null)
	//  - passing a null 'pcsReader' means NO getSessions() database call is made
	//  - and it's cached until the file changes (last-modified time or length)
	// This matters because /api/alarm/active is polled continuously by the Web UI.
	//--------------------------------------------------------------------------

	private static final Object              _groupCacheLock     = new Object();
	private static       Map<String, String> _groupCache         = null;
	private static       String              _groupCacheFilename = null;
	private static       long                _groupCacheLastMod  = -1;
	private static       long                _groupCacheLength   = -1;

	/**
	 * Get a Map of 'serverName -&gt; groupName' for all servers that are member of a GROUP.<br>
	 * Servers that are <b>not</b> within any GROUP are simply <b>not</b> in the returned Map.
	 * 
	 * @param filename  Name of the SERVER_LIST file (null/blank = the default location)
	 * @return A never-null, unmodifiable Map. An empty Map if the file is missing or has no groups.
	 */
	public static Map<String, String> getServerNameToGroupMap(String filename)
	{
		if (StringUtil.isNullOrBlank(filename))
			filename = DbxCentralServerDescription.getDefaultFile();

		File f = new File(filename);
		long lastMod = f.lastModified(); // 0 if the file do not exist
		long length  = f.length();       // 0 if the file do not exist

		synchronized (_groupCacheLock)
		{
			// Still valid?
			if (    _groupCache != null 
			     && filename.equals(_groupCacheFilename) 
			     && lastMod == _groupCacheLastMod 
			     && length  == _groupCacheLength
			   )
			{
				return _groupCache;
			}

			Map<String, String> map = new LinkedHashMap<>();
			try
			{
				// NOTE: 'null' as the pcsReader --> NO database call is made, and the
				//       synthetic "Others Servers (Not in Layout File)" group is NOT added.
				for (DbxCentralServerLayout groupEntry : getFromFile(filename, null))
				{
					if ( ! groupEntry.isGroupEntry() )
						continue;

					List<DbxCentralServerLayout> entries = groupEntry.getEntries();
					if (entries == null)
						continue;

					for (DbxCentralServerLayout srvEntry : entries)
					{
						// getText() holds 'srvDesc.getServerOrAliasName()', which is the same value
						// that is used as the SCHEMA name in the Central database, and therefore also
						// the 'srvName' that getAlarmActive() returns. So this is the correct join key.
						if (srvEntry.isServerEntry() && StringUtil.hasValue(srvEntry.getText()))
							map.put(srvEntry.getText(), groupEntry.getText());
					}
				}
			}
			catch (Exception ex)
			{
				// Note: we cache the (possibly empty) map anyway, so we do not re-read/re-throw on every request
				_logger.warn("Problems reading Server Groups from file '" + filename + "'. No group information will be available. Caught: " + ex);
			}

			_groupCache         = Collections.unmodifiableMap(map);
			_groupCacheFilename = filename;
			_groupCacheLastMod  = lastMod;
			_groupCacheLength   = length;

			return _groupCache;
		}
	}

	/**
	 * Get the GROUP name that a specific server is a member of.
	 * 
	 * @param srvName   Name of the server (as it is known in the Central database)
	 * @param filename  Name of the SERVER_LIST file (null/blank = the default location)
	 * @return The group name, or null if the server is unknown or not within any GROUP.
	 */
	public static String getGroupNameForServer(String srvName, String filename)
	{
		if (StringUtil.isNullOrBlank(srvName))
			return null;

		return getServerNameToGroupMap(filename).get(srvName);
	}

	/**
	 * Get all server names that are members of the passed GROUP name(s).
	 * 
	 * @param groupNames  One or several group names, comma separated. Case insensitive.
	 * @param filename    Name of the SERVER_LIST file (null/blank = the default location)
	 * @return A never-null Set. Empty if no groups matched.
	 */
	public static Set<String> getServerNamesInGroups(String groupNames, String filename)
	{
		Set<String> serverNames = new LinkedHashSet<>();

		if (StringUtil.isNullOrBlank(groupNames))
			return serverNames;

		Set<String> wantedGroups = new LinkedHashSet<>();
		for (String name : groupNames.split(","))
		{
			String groupName = name.trim();
			if ( ! groupName.isEmpty() )
				wantedGroups.add(groupName.toLowerCase());
		}

		for (Map.Entry<String, String> entry : getServerNameToGroupMap(filename).entrySet())
		{
			String groupName = entry.getValue();
			if (groupName != null && wantedGroups.contains(groupName.toLowerCase()))
				serverNames.add(entry.getKey());
		}

		return serverNames;
	}

	/** Discard the cached 'serverName -&gt; groupName' Map. Primarily intended for test code. */
	public static void clearServerGroupCache()
	{
		synchronized (_groupCacheLock)
		{
			_groupCache         = null;
			_groupCacheFilename = null;
			_groupCacheLastMod  = -1;
			_groupCacheLength   = -1;
		}
	}

	/**
	 * Returns all entries from file 'conf/SERVER_LIST' as a Map in the order of the file.
	 * @param filename
	 * @return
	 * @throws IOException
	 */
	public static List<DbxCentralServerLayout> getFromFile(String filename, CentralPersistReader pcsReader)
	throws IOException, SQLException
	{
		if (StringUtil.isNullOrBlank(filename))
			filename = DbxCentralServerDescription.getDefaultFile();

		List<DbxCentralServerLayout> rootList = new ArrayList<>();
		List<DbxCentralServerLayout> groupList = null;

		
		// Get sessions (which will be added to the "server" entries (or at the end if not found in the layout file)
		boolean onlyLast  = true;
		boolean getGraphs = false;
		int status = -1;
		
		List<DbxCentralSessions> sessionsList = null;
		if (pcsReader != null)
			sessionsList = pcsReader.getSessions( onlyLast, status );

		
		// Read the file...
		File f = new File(filename);
		if (f.exists())
		{
			try( BufferedReader bufferreader = new BufferedReader(new FileReader(f)) )
			{
				String line;
				int lineNum = 0;
				while ((line = bufferreader.readLine()) != null)
				{
					lineNum++;
					line = line.trim();
					
					boolean isFormatEntry = false;

					if (line.startsWith("#") || line.equals(""))
					{
						// Remove any comments '#' chars
						while (line.startsWith("#"))
							line = line.substring(1);

						// Accept any lines that starts with "FORMAT:"
						if (StringUtil.startsWithIgnoreBlankIgnoreCase(line, "FORMAT;"))
							isFormatEntry = true;
						else
							continue;
					}

					if (isFormatEntry)
					{
					//	String[] sa = line.split(";");
						String[] sa = line.split("(?<!\\\\);"); // Split on ';' but NOT on '\;' which allow us to use escaped semicolons
						if (sa.length > 0)
						{
							String  type    = sa[1].replace("\\;", ";").trim();
							String  text    = sa.length > 2 ? sa[2].replace("\\;", ";").trim() : "";
							String  options = sa.length > 3 ? sa[3].replace("\\;", ";").trim() : "";
							
							Map<String, String> optionsMap = null;
							if (StringUtil.hasValue(options))
								optionsMap = StringUtil.parseCommaStrToMap(options);

							if ("GROUP".equalsIgnoreCase(type))
							{
								groupList = new ArrayList<>(); // ALL LABEL/SERVER Entries will now be added to this list 

								// Add GROUP entry
								DbxCentralServerLayout entry = createGroupEntry(text, optionsMap, groupList);
								rootList.add(entry);
							}
							else if ("LABEL".equalsIgnoreCase(type))
							{
								// Add LABEL entry
								DbxCentralServerLayout entry = createLabelEntry(text, optionsMap);
								if (groupList != null)
									groupList.add(entry);
								else
									rootList.add(entry);
							}

							else
							{
								_logger.error("Unknown FORMAT specification '" + type + "' found in file '" + f + "' at line: " + lineNum);
							}
						}
					}
					else
					{
//						String[] sa = line.split(";");
						String[] sa = line.split("(?<!\\\\);"); // Split on ';' but NOT on '\;' which allow us to use escaped semicolons
						if (sa.length > 0)
						{
							String  serverName  = sa[0].trim();
							boolean enabled     = sa.length > 1 ? sa[1].replace("\\;", ";").trim().equalsIgnoreCase("1") : false;
							String  description = sa.length > 2 ? sa[2].replace("\\;", ";").trim()                       : "";
							String  extraInfo   = sa.length > 3 ? sa[3].replace("\\;", ";").trim()                       : "";

							// Create a ServerDescription (not having to parse ServerName or AliasName again)
							DbxCentralServerDescription srvDesc = new DbxCentralServerDescription(serverName, enabled, description, extraInfo);
							String srvOrAliasName = srvDesc.getServerOrAliasName();

							// Find the Sessions entry (in the list from the PCS Reader)
							DbxCentralSessions srvSession = null;
							if (sessionsList != null) // NOTE: sessionsList will only be null in test code.
							{
								for (DbxCentralSessions sessionEntry : sessionsList)
								{
									if (srvOrAliasName.equals(sessionEntry.getServerName()))
									{
										if (StringUtil.hasValue(srvDesc.getDisplayName()))
										{
											sessionEntry.setServerDisplayName(srvDesc.getDisplayName());
										}
										srvSession = sessionEntry;
									}
								}
								// Remove any FOUND sessions from the list (all remaining in the list -->> will be added at the very end)
								if (srvSession != null)
									sessionsList.remove(srvSession);
							}

							// Skip adding server (if it can't be found in the sessions list)
							boolean skipIfNotFound = false;
							if (skipIfNotFound)
							{
								if (srvSession == null)
									continue;
							}
							
							// Add SERVER entry
							DbxCentralServerLayout entry = createServerEntry(srvOrAliasName, srvSession);
							if (groupList != null)
								groupList.add(entry);
							else
								rootList.add(entry);
						}
					} // end: SERVER
				} // end: read file
			} // end: try
		} // end: file exists
		else
		{
			throw new FileNotFoundException("DbxCentral Server Configuration File '"+filename+"' did not exist.");
		}
	
		//-------------------------------------------
		// Add entries NOT FOUND in the file
		//-------------------------------------------
		if (sessionsList != null && !sessionsList.isEmpty())
		{
			// If we have created any previous groups... Simply add a new group named "Others" for ServerNames NOT in the "format" file
			if (groupList != null)
			{
				groupList = new ArrayList<>();

				DbxCentralServerLayout entry = createGroupEntry("Others Servers (Not in Layout File)", null, groupList);
				rootList.add(entry);
			}
			
			// Add
			for (DbxCentralSessions sessionEntry : sessionsList)
			{
				DbxCentralServerLayout entry = createServerEntry(sessionEntry.getServerName(), sessionEntry);
				if (groupList != null)
					groupList.add(entry);
				else
					rootList.add(entry);
			}
		}

		
		return rootList;
	}
}
