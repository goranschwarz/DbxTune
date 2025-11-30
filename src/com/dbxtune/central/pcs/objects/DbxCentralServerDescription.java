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
package com.dbxtune.central.pcs.objects;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.central.DbxTuneCentral;
import com.dbxtune.utils.StringUtil;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

// from the file: config/SERVER_LIST
//
// ##===========================================================================
// ## Fields in this file
// ## 1 - SERVERNAME (or connection info sent to the specified DbxTune collector) 
// ## 2 - 1=Enabled, 0=Disabled
// ## 3 - Some explanation for the role of this server
// ## 4 - Script that will be used to start the server.
// ##     NOTE: <SRVNAME> is replaced with the servername in field 1
// ##===========================================================================
// ## NOTE about field 1 / ServerName
// ##  * If this field contain any strange char like '/', '\', ';' or ':' then we need to add: -A alternameSrvName
// ##    The alternate server name will be used for log files, H2 Database Recordings, DbxCentral SchemaName, etc...
// ##  For example if the server do not run on the default port name, a entry might look like:
// ##    prod-1a-pg.acme.com:1234 ; 1 ; Some Desc ; ${DBXTUNE_CENTRAL_BASE}/bin/start_postgrestune.sh <SRVNAME> -A prod-1a-pg
// ##  Or for a SQL-Server with dynamic ports
// ##    GS-1-WIN\SS_2016 ; 1 ; SQL-Server 2016 on Windows ; ${DBXTUNE_CENTRAL_BASE}/bin/start_sqlservertune.sh <SRVNAME> -A GS-1-WIN__SS_2016
// ##===========================================================================
// ## NOTE about field 4 / Script...
// ## ${DBXTUNE_CENTRAL_BASE} is normally: ${HOME}/.dbxtune/dbxc
// ## In that script you can specialize how to start the various collectors
// ## You CAN also put everything (all cmdline switches) in here as well, but the row will be very long...
// ##===========================================================================
// 
// PROD_A_ASE       ; 1 ; PROD Active Side             ; ${DBXTUNE_CENTRAL_BASE}/bin/start_asetune.sh <SRVNAME>
// PROD_B_ASE       ; 1 ; PROD Standby Side            ; ${DBXTUNE_CENTRAL_BASE}/bin/start_asetune.sh <SRVNAME>
// DEV_ASE          ; 1 ; DEV                          ; ${DBXTUNE_CENTRAL_BASE}/bin/start_asetune.sh <SRVNAME>
// SYS_ASE          ; 0 ; SYS                          ; ${DBXTUNE_CENTRAL_BASE}/bin/start_asetune.sh <SRVNAME>
// INT_ASE          ; 0 ; INT                          ; ${DBXTUNE_CENTRAL_BASE}/bin/start_asetune.sh <SRVNAME>
// STAGE_ASE        ; 0 ; STAGE                        ; ${DBXTUNE_CENTRAL_BASE}/bin/start_asetune.sh <SRVNAME>
// PROD_A1_ASE      ; 1 ; PROD Active Side - New       ; ${DBXTUNE_CENTRAL_BASE}/bin/start_asetune.sh <SRVNAME>
// PROD_B1_ASE      ; 1 ; PROD Standby Side - New      ; ${DBXTUNE_CENTRAL_BASE}/bin/start_asetune.sh <SRVNAME>
// host-1:1234      ; 1 ; Postgres on non default port ; ${DBXTUNE_CENTRAL_BASE}/bin/start_postgrestune.sh <SRVNAME> -A prod-1a-pg
// GS-1-WIN\SS_2016 ; 1 ; SQL-Server on dynamic port   ; ${DBXTUNE_CENTRAL_BASE}/bin/start_sqlservertune.sh <SRVNAME> -A GS-1-WIN__SS_2016



@JsonPropertyOrder(value = {"serverName", "serverNameOrAlias", "displayName", "enabled", "description", "extraInfo"}, alphabetic = true)
public class DbxCentralServerDescription
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private String  _serverName;
	private String  _aliasName;
	private String  _displayName;
	private boolean _isEnabled;
	private String  _description;
	private String  _extraInfo;

	/** If 'aliasName' is SET then that will be returned otherwise it's the 'serverName' */
	public String getServerOrAliasName() 
	{
		if (StringUtil.hasValue(_aliasName))
			return _aliasName;
		return _serverName;
	}
	
	public String  getServerName () { return _serverName ;  }
	public String  getAliasName  () { return _aliasName  ;  }
	public String  getDisplayName() { return _displayName;  }
	public boolean isEnabled     () { return _isEnabled  ;  }
	public String  getDescription() { return _description;  }
	public String  getExtraInfo  () { return _extraInfo  ;  }

	public void setServerName (String  serverName ) { _serverName  = serverName ; }
	public void setAliasName  (String  aliasName  ) { _aliasName   = aliasName  ; }
	public void setDisplayName(String  displayName) { _displayName = displayName; }
	public void setEnabled    (boolean enabled    ) { _isEnabled   = enabled    ; }
	public void setDescription(String  description) { _description = description; }
	public void setExtraInfo  (String  extraInfo)   { _extraInfo   = extraInfo  ; }

	public DbxCentralServerDescription(String serverName, boolean enabled, String description, String extraInfo)
	{
		super();

		_serverName  = serverName;
		_isEnabled   = enabled;
		_description = description;
		_extraInfo   = extraInfo;
		
		if (_extraInfo != null)
		{
			// Only parse if we got ANY of the switches
			if (StringUtil.containsAny(extraInfo, 
					"-A", "--serverAlias", 
					"-N", "--displayName"))
			{
				parseExtraInfo(extraInfo);
			}
		}
	}
	
	private void parseExtraInfo(String extraInfo)
	{
		boolean removeFirst = true;
		String[] args = StringUtil.translateCommandline(extraInfo, removeFirst);
		if (args.length >= 1)
		{
			try
			{
				Options options = new Options();

				// create the Options
				options.addOption( "A", "serverAlias", true, "fixme" );
				options.addOption( "N", "displayName", true, "fixme" );

				// create the command line com.dbxtune.parser
				CommandLineParser parser = new DefaultParser();

				// parse the command line arguments
				CommandLine cmd = parser.parse(options, args);

				// Handle results
				if (cmd.hasOption('A')) _aliasName   = replaceSrvNameTemplate(cmd.getOptionValue('A'), _serverName);
				if (cmd.hasOption('N')) _displayName = replaceSrvNameTemplate(cmd.getOptionValue('N'), _serverName);
			}
			catch (ParseException ex)
			{
				_logger.warn("Problem parsing extraInfo for serverName='" + _serverName + "'. args=|" + Arrays.asList(args) + "|. Caught: " + ex);
			}
		}
	}
	/**
	 * replace &lt;SRVNAME&gt; with input parameter srvName<br>
	 * and input starts and ends with '_' replace all underscores with spaces, and remove start/end underscores 
	 */
	public static String replaceSrvNameTemplate(String input, String srvName)
	{
		if (StringUtil.isNullOrBlank(input))   return input;
		if (StringUtil.isNullOrBlank(srvName)) return input;
		
		String displayName = input;

		// Fix <SRVNAME>
		if (displayName.contains("<SRVNAME>"))
			displayName = displayName.replace("<SRVNAME>", srvName);

		// if displayName starts and ends with '_' replace all underscores with spaces, and remove start/end underscores
		if (displayName.startsWith("_") && displayName.endsWith("_"))
			displayName = displayName.replace('_', ' ').trim();
		
		return displayName;
	}


	public static String getDefaultFile()
	{
		String filename = StringUtil.hasValue(DbxTuneCentral.getAppConfDir()) ? DbxTuneCentral.getAppConfDir() + "/SERVER_LIST" : "conf/SERVER_LIST";
		return filename;
	}
	
	public static Map<String, DbxCentralServerDescription> getFromFile()
	throws IOException
	{
		return getFromFile(null);
	}

	/**
	 * Returns all entries from file 'conf/SERVER_LIST' as a Map in the order of the file.
	 * @param filename
	 * @return
	 * @throws IOException
	 */
	public static Map<String, DbxCentralServerDescription> getFromFile(String filename)
	throws IOException
	{
		if (StringUtil.isNullOrBlank(filename))
			filename = getDefaultFile();

		LinkedHashMap<String, DbxCentralServerDescription> map = new LinkedHashMap<>();

		File f = new File(filename);
		if (f.exists())
		{
			try( BufferedReader bufferreader = new BufferedReader(new FileReader(f)) )
			{
				String line;
				while ((line = bufferreader.readLine()) != null)
				{
					line = line.trim();
					if (line.startsWith("#") || line.equals(""))
						continue;
					
//					String[] sa = line.split(";");
					String[] sa = line.split("(?<!\\\\);"); // Split on ';' but NOT on '\;' which allow us to use escaped semicolons
					if (sa.length > 0)
					{
						String  serverName  = sa[0].trim();
						boolean enabled     = sa.length > 1 ? sa[1].replace("\\;", ";").trim().equalsIgnoreCase("1") : false;
						String  description = sa.length > 2 ? sa[2].replace("\\;", ";").trim()                       : "";
						String  extraInfo   = sa.length > 3 ? sa[3].replace("\\;", ";").trim()                       : "";
						
						DbxCentralServerDescription entry = new DbxCentralServerDescription(serverName, enabled, description, extraInfo);
//						map.put(entry.getServerName(), entry);
						map.put(entry.getServerOrAliasName(), entry);
					}
				}
			}
		}
		else
		{
			throw new FileNotFoundException("DbxCentral Server Configuration File '"+filename+"' did not exist.");
		}
		
		return map;
	}

	/**
	 * Remove or comment-out a server in the SERVER_LIST
	 * 
	 * @param filename
	 * @param removeServerName
	 * 
	 * @throws IOException
	 */
	public static boolean removeFromFile(String filename, String removeServerName, boolean asComment)
	throws IOException
	{
		if (StringUtil.isNullOrBlank(filename))
			filename = getDefaultFile();

		// Read all entries into a map, so we can get the serverName/AliasName
		// Since it's probably the alias name that is passed into this method
		Map<String, DbxCentralServerDescription> srvMap = getFromFile(filename);
		DbxCentralServerDescription srvMapEntry = srvMap.get(removeServerName);
		
		boolean foundRow = false;

		File f = new File(filename);
		if (f.exists())
		{
			List<String> readList  = FileUtils.readLines(f, Charset.defaultCharset());
			List<String> writeList = new ArrayList<>();

			for (String line : readList)
			{
				line = line.trim();
				if (line.startsWith("#") || line.equals(""))
				{
					writeList.add(line); // write origin line
					continue;
				}
				
//				String[] sa = line.split(";");
				String[] sa = line.split("(?<!\\\\);"); // Split on ';' but NOT on '\;' which allow us to use escaped semicolons
				if (sa.length > 0)
				{
					String rowServerName  = sa[0].replace("\\;", ";").trim();
					// Check both the current-rows-read and the map-entry which was fetched by the aliasName... but instead of the aliasName use the srvName
					if (rowServerName.equals(removeServerName) || (srvMapEntry != null && rowServerName.equals(srvMapEntry.getServerName())) )
					{
						foundRow = true;
						if (asComment)
							writeList.add("#" + line); // Comment out this line
					}
					else
						writeList.add(line); // write origin line
				}
				else
					writeList.add(line); // write origin line
			}
			
			// WRITE the file back again
			FileUtils.writeLines(f, writeList);
		}
		else
		{
			throw new FileNotFoundException("DbxCentral Server Configuration File '"+filename+"' did not exist.");
		}
		
		return foundRow;
	}

	public static void main(String[] args)
	{
		DbxCentralServerDescription t0 = new DbxCentralServerDescription("t0", true, "some dummy description", "${DBXTUNE_CENTRAL_BASE}/bin/start_postgrestune.sh <SRVNAME>");
		DbxCentralServerDescription t1 = new DbxCentralServerDescription("t1", true, "some dummy description", "${DBXTUNE_CENTRAL_BASE}/bin/start_postgrestune.sh <SRVNAME> -A t1-prod-1a-pg");
		DbxCentralServerDescription t2 = new DbxCentralServerDescription("t2", true, "some dummy description", "${DBXTUNE_CENTRAL_BASE}/bin/start_postgrestune.sh <SRVNAME> -A t2-prod-1a-pg xxx");
		DbxCentralServerDescription t3 = new DbxCentralServerDescription("t3", true, "some dummy description", "${DBXTUNE_CENTRAL_BASE}/bin/start_postgrestune.sh <SRVNAME> --serverAlias t3-prod-1a-pg ");
		DbxCentralServerDescription t4 = new DbxCentralServerDescription("t4", true, "some dummy description", "${DBXTUNE_CENTRAL_BASE}/bin/start_postgrestune.sh <SRVNAME> --serverAlias t4-prod-1a-pg xxx");

		System.out.println("####################");
		System.out.println("t0: srvName=|" + t0.getServerName() + "|, aliasName=|" + t0.getAliasName() + "|, serverOrAliasName=|" + t0.getServerOrAliasName() + "|");
		System.out.println("t1: srvName=|" + t1.getServerName() + "|, aliasName=|" + t1.getAliasName() + "|, serverOrAliasName=|" + t1.getServerOrAliasName() + "|");
		System.out.println("t2: srvName=|" + t2.getServerName() + "|, aliasName=|" + t2.getAliasName() + "|, serverOrAliasName=|" + t2.getServerOrAliasName() + "|");
		System.out.println("t3: srvName=|" + t3.getServerName() + "|, aliasName=|" + t3.getAliasName() + "|, serverOrAliasName=|" + t3.getServerOrAliasName() + "|");
		System.out.println("t4: srvName=|" + t4.getServerName() + "|, aliasName=|" + t4.getAliasName() + "|, serverOrAliasName=|" + t4.getServerOrAliasName() + "|");
	}
}

