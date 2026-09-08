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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.dbxtune.central.controllers.Helper;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DbxCentralServerLayoutTest
{
	private static boolean _writeJson = true;
	
	@Test
	public void test1() throws Exception
	{
		Path configFile = Files.createTempFile(null, null);

		String fileContent = ""
				+ " \n"
				+ "GORAN_UB0_DS       ; 1 ; ASE Version 12.5.4         ; ${DBXTUNE_CENTRAL_BASE}/bin/start_asetune.sh <SRVNAME> \n"
				+ "GORANS_UB1_DS      ; 1 ; ASE Version 15.5           ; ${DBXTUNE_CENTRAL_BASE}/bin/start_asetune.sh <SRVNAME> \n"
				+ "GORAN_UB2_DS       ; 1 ; ASE Version 15.7           ; ${DBXTUNE_CENTRAL_BASE}/bin/start_asetune.sh <SRVNAME> \n"
				+ "GORAN_UB3_DS       ; 1 ; ASE Version 16.0           ; ${DBXTUNE_CENTRAL_BASE}/bin/start_asetune.sh <SRVNAME> \n"
				+ " \n"
				+ "gs-1-win:1433      ; 1 ; SQL-Server 2016 on Windows ; ${DBXTUNE_CENTRAL_BASE}/bin/start_sqlservertune.sh <SRVNAME> -A GS-1-WIN__SS_2016 \n"
				+ "gorans-ub3:1433    ; 1 ; SQL-Server 2019 on Linux   ; ${DBXTUNE_CENTRAL_BASE}/bin/start_sqlservertune.sh <SRVNAME> -A gorans-ub3-ss \n"
				+ "prod-2a-mssql      ; 1 ; SQL-Server 2019 on Linux   ; ${DBXTUNE_CENTRAL_BASE}/bin/start_sqlservertune.sh <SRVNAME> \n"
				+ "prod-2b-mssql      ; 1 ; SQL-Server 2019 on Linux   ; ${DBXTUNE_CENTRAL_BASE}/bin/start_sqlservertune.sh <SRVNAME> \n"
				+ " \n"
				+ "gorans-ub3:5432    ; 1 ; Postgress 12.7              ; ${DBXTUNE_CENTRAL_BASE}/bin/start_postgrestune.sh <SRVNAME>  -A gorans-ub3-pg \n"
				+ "gorans-ub3:3306    ; 1 ; MySql 8                     ; ${DBXTUNE_CENTRAL_BASE}/bin/start_mysqltune.sh <SRVNAME>     -A gorans-ub3-my \n"
				+ " \n"
				+ " \n";
		
		Files.write(configFile, fileContent.getBytes(StandardCharsets.UTF_8));

		if (_writeJson)
			System.out.println("Reading file='" + configFile.toString() + "'.");

		List<DbxCentralServerLayout> layout = DbxCentralServerLayout.getFromFile(configFile.toString(), null);

		// Check size
		assertEquals(10, layout.size());

		if (_writeJson)
		{
			// to JSON
			ObjectMapper om = Helper.createObjectMapper();
			String payload = om.writeValueAsString(layout);
			
			System.out.println("JSON-1:\n" + payload + "\n--end--\n");
		}
		
		if (configFile != null) 
			Files.delete(configFile);
	}

	@Test
	public void test2() throws Exception
	{
		Path configFile = Files.createTempFile(null, null);

		String fileContent = ""
				+ " \n"
				+ "#FORMAT; GROUP; Production; border=true, option1=val1, option2=val2 \n" 
				+ "#FORMAT; LABEL; Sybase Servers \n"
				+ "GORAN_UB0_DS       ; 1 ; ASE Version 12.5.4         ; ${DBXTUNE_CENTRAL_BASE}/bin/start_asetune.sh <SRVNAME> \n"
				+ "GORANS_UB1_DS      ; 1 ; ASE Version 15.5           ; ${DBXTUNE_CENTRAL_BASE}/bin/start_asetune.sh <SRVNAME> \n"
				+ "GORAN_UB2_DS       ; 1 ; ASE Version 15.7           ; ${DBXTUNE_CENTRAL_BASE}/bin/start_asetune.sh <SRVNAME> \n"
				+ "GORAN_UB3_DS       ; 1 ; ASE Version 16.0           ; ${DBXTUNE_CENTRAL_BASE}/bin/start_asetune.sh <SRVNAME> \n"
				+ " \n"
				+ "#FORMAT; LABEL; SQL Servers \n"
				+ "gs-1-win:1433      ; 1 ; SQL-Server 2016 on Windows ; ${DBXTUNE_CENTRAL_BASE}/bin/start_sqlservertune.sh <SRVNAME> -A GS-1-WIN__SS_2016 \n"
				+ "gorans-ub3:1433    ; 1 ; SQL-Server 2019 on Linux   ; ${DBXTUNE_CENTRAL_BASE}/bin/start_sqlservertune.sh <SRVNAME> -A gorans-ub3-ss \n"
				+ "prod-2a-mssql      ; 1 ; SQL-Server 2019 on Linux   ; ${DBXTUNE_CENTRAL_BASE}/bin/start_sqlservertune.sh <SRVNAME> \n"
				+ "prod-2b-mssql      ; 1 ; SQL-Server 2019 on Linux   ; ${DBXTUNE_CENTRAL_BASE}/bin/start_sqlservertune.sh <SRVNAME> \n"
				+ " \n"
				+ "#FORMAT; LABEL; Others \n"
				+ "gorans-ub3:5432    ; 1 ; Postgress 12.7              ; ${DBXTUNE_CENTRAL_BASE}/bin/start_postgrestune.sh <SRVNAME>  -A gorans-ub3-pg \n"
				+ "gorans-ub3:3306    ; 1 ; MySql 8                     ; ${DBXTUNE_CENTRAL_BASE}/bin/start_mysqltune.sh <SRVNAME>     -A gorans-ub3-my \n"
				+ " \n"
				+ " \n";
		
		Files.write(configFile, fileContent.getBytes(StandardCharsets.UTF_8));

		if (_writeJson)
			System.out.println("Reading file='" + configFile.toString() + "'.");

		List<DbxCentralServerLayout> layout = DbxCentralServerLayout.getFromFile(configFile.toString(), null);

		// Check size
		assertEquals(1, layout.size());

		for (int i=0; i<layout.size(); i++)
		{
			DbxCentralServerLayout entry = layout.get(i);
			if (i == 0) assertEquals(13, entry.getEntries().size());
			if (i == 0) assertEquals(3 , entry.getOptions().size());
		}

		if (_writeJson)
		{
			// to JSON
			ObjectMapper om = Helper.createObjectMapper();
			String payload = om.writeValueAsString(layout);
			
			System.out.println("JSON-2:\n" + payload + "\n--end--\n");
		}
		
		if (configFile != null) 
			Files.delete(configFile);
	}

	@Test
	public void test3() throws Exception
	{
		Path configFile = Files.createTempFile(null, null);

		String fileContent = ""
				+ " \n"
				+ "#FORMAT; GROUP; Production (Sybase); border=true \n" 
				+ "GORAN_UB0_DS       ; 1 ; ASE Version 12.5.4         ; ${DBXTUNE_CENTRAL_BASE}/bin/start_asetune.sh <SRVNAME> \n"
				+ "GORANS_UB1_DS      ; 1 ; ASE Version 15.5           ; ${DBXTUNE_CENTRAL_BASE}/bin/start_asetune.sh <SRVNAME> \n"
				+ "GORAN_UB2_DS       ; 1 ; ASE Version 15.7           ; ${DBXTUNE_CENTRAL_BASE}/bin/start_asetune.sh <SRVNAME> \n"
				+ "GORAN_UB3_DS       ; 1 ; ASE Version 16.0           ; ${DBXTUNE_CENTRAL_BASE}/bin/start_asetune.sh <SRVNAME> \n"
				+ " \n"
				+ "#FORMAT; GROUP; Production (SQL Server); border=true \n" 
				+ "gs-1-win:1433      ; 1 ; SQL-Server 2016 on Windows ; ${DBXTUNE_CENTRAL_BASE}/bin/start_sqlservertune.sh <SRVNAME> -A GS-1-WIN__SS_2016 \n"
				+ "gorans-ub3:1433    ; 1 ; SQL-Server 2019 on Linux   ; ${DBXTUNE_CENTRAL_BASE}/bin/start_sqlservertune.sh <SRVNAME> -A gorans-ub3-ss \n"
				+ "prod-2a-mssql      ; 1 ; SQL-Server 2019 on Linux   ; ${DBXTUNE_CENTRAL_BASE}/bin/start_sqlservertune.sh <SRVNAME> \n"
				+ "prod-2b-mssql      ; 1 ; SQL-Server 2019 on Linux   ; ${DBXTUNE_CENTRAL_BASE}/bin/start_sqlservertune.sh <SRVNAME> \n"
				+ " \n"
				+ "#FORMAT; GROUP; Production (Other DBMS); border=true \n" 
				+ "gorans-ub3:5432    ; 1 ; Postgress 12.7              ; ${DBXTUNE_CENTRAL_BASE}/bin/start_postgrestune.sh <SRVNAME>  -A gorans-ub3-pg \n"
				+ "gorans-ub3:3306    ; 1 ; MySql 8                     ; ${DBXTUNE_CENTRAL_BASE}/bin/start_mysqltune.sh <SRVNAME>     -A gorans-ub3-my \n"
				+ " \n"
				+ " \n";
		
		Files.write(configFile, fileContent.getBytes(StandardCharsets.UTF_8));

		if (_writeJson)
			System.out.println("Reading file='" + configFile.toString() + "'.");

		List<DbxCentralServerLayout> layout = DbxCentralServerLayout.getFromFile(configFile.toString(), null);

		// Check size
		assertEquals(3, layout.size());

		for (int i=0; i<layout.size(); i++)
		{
			DbxCentralServerLayout entry = layout.get(i);
			if (i == 0) assertEquals(4, entry.getEntries().size());
			if (i == 1) assertEquals(4, entry.getEntries().size());
			if (i == 2) assertEquals(2, entry.getEntries().size());
		}

		if (_writeJson)
		{
			// to JSON
			ObjectMapper om = Helper.createObjectMapper();
			String payload = om.writeValueAsString(layout);
			
			System.out.println("JSON-3:\n" + payload + "\n--end--\n");
		}
		
		if (configFile != null)
			Files.delete(configFile);
	}

	//----------------------------------------------------------------------
	// Server GROUP lookups
	//----------------------------------------------------------------------

	/** Same layout as test3: 3 groups, holding 4 / 4 / 2 servers. Two of the groups use '-A <alias>'. */
	private static final String GROUP_FILE_CONTENT = ""
			+ " \n"
			+ "#FORMAT; GROUP; Production (Sybase); border=true \n"
			+ "GORAN_UB0_DS       ; 1 ; ASE Version 12.5.4         ; ${DBXTUNE_CENTRAL_BASE}/bin/start_asetune.sh <SRVNAME> \n"
			+ "GORANS_UB1_DS      ; 1 ; ASE Version 15.5           ; ${DBXTUNE_CENTRAL_BASE}/bin/start_asetune.sh <SRVNAME> \n"
			+ "GORAN_UB2_DS       ; 1 ; ASE Version 15.7           ; ${DBXTUNE_CENTRAL_BASE}/bin/start_asetune.sh <SRVNAME> \n"
			+ "GORAN_UB3_DS       ; 1 ; ASE Version 16.0           ; ${DBXTUNE_CENTRAL_BASE}/bin/start_asetune.sh <SRVNAME> \n"
			+ " \n"
			+ "#FORMAT; GROUP; Production (SQL Server); border=true \n"
			+ "gs-1-win:1433      ; 1 ; SQL-Server 2016 on Windows ; ${DBXTUNE_CENTRAL_BASE}/bin/start_sqlservertune.sh <SRVNAME> -A GS-1-WIN__SS_2016 \n"
			+ "gorans-ub3:1433    ; 1 ; SQL-Server 2019 on Linux   ; ${DBXTUNE_CENTRAL_BASE}/bin/start_sqlservertune.sh <SRVNAME> -A gorans-ub3-ss \n"
			+ "prod-2a-mssql      ; 1 ; SQL-Server 2019 on Linux   ; ${DBXTUNE_CENTRAL_BASE}/bin/start_sqlservertune.sh <SRVNAME> \n"
			+ "prod-2b-mssql      ; 1 ; SQL-Server 2019 on Linux   ; ${DBXTUNE_CENTRAL_BASE}/bin/start_sqlservertune.sh <SRVNAME> \n"
			+ " \n"
			+ "#FORMAT; GROUP; Production (Other DBMS); border=true \n"
			+ "gorans-ub3:5432    ; 1 ; Postgress 12.7              ; ${DBXTUNE_CENTRAL_BASE}/bin/start_postgrestune.sh <SRVNAME>  -A gorans-ub3-pg \n"
			+ "gorans-ub3:3306    ; 1 ; MySql 8                     ; ${DBXTUNE_CENTRAL_BASE}/bin/start_mysqltune.sh <SRVNAME>     -A gorans-ub3-my \n"
			+ " \n";

	@Test
	public void testServerGroupLookup() throws Exception
	{
		Path configFile = Files.createTempFile(null, null);
		Files.write(configFile, GROUP_FILE_CONTENT.getBytes(StandardCharsets.UTF_8));

		DbxCentralServerLayout.clearServerGroupCache();

		String filename = configFile.toString();

		//------------------------------------------------
		// The full map
		//------------------------------------------------
		Map<String, String> map = DbxCentralServerLayout.getServerNameToGroupMap(filename);
		assertEquals(10, map.size());

		//------------------------------------------------
		// getGroupNameForServer()
		//------------------------------------------------
		assertEquals("Production (Sybase)"     , DbxCentralServerLayout.getGroupNameForServer("GORAN_UB0_DS"      , filename));
		assertEquals("Production (Sybase)"     , DbxCentralServerLayout.getGroupNameForServer("GORAN_UB3_DS"      , filename));
		assertEquals("Production (SQL Server)" , DbxCentralServerLayout.getGroupNameForServer("prod-2a-mssql"     , filename));
		assertEquals("Production (Other DBMS)" , DbxCentralServerLayout.getGroupNameForServer("gorans-ub3-my"     , filename));

		// The join key must be the ALIAS (the '-A <name>' switch), since that is what is used as the
		// schema name in the Central database, and therefore also what /api/alarm/active returns as 'srvName'
		assertEquals("Production (SQL Server)" , DbxCentralServerLayout.getGroupNameForServer("GS-1-WIN__SS_2016" , filename));
		assertEquals("Production (SQL Server)" , DbxCentralServerLayout.getGroupNameForServer("gorans-ub3-ss"     , filename));
		assertNull  (                            DbxCentralServerLayout.getGroupNameForServer("gs-1-win:1433"     , filename));

		// Unknown / blank
		assertNull(DbxCentralServerLayout.getGroupNameForServer("NO_SUCH_SERVER", filename));
		assertNull(DbxCentralServerLayout.getGroupNameForServer(""              , filename));
		assertNull(DbxCentralServerLayout.getGroupNameForServer(null            , filename));

		//------------------------------------------------
		// getServerNamesInGroups()
		//------------------------------------------------
		Set<String> sybase = DbxCentralServerLayout.getServerNamesInGroups("Production (Sybase)", filename);
		assertEquals(4, sybase.size());
		assertTrue(sybase.contains("GORAN_UB0_DS"));
		assertTrue(sybase.contains("GORAN_UB3_DS"));

		// Several groups at once
		Set<String> twoGroups = DbxCentralServerLayout.getServerNamesInGroups("Production (Sybase),Production (Other DBMS)", filename);
		assertEquals(6, twoGroups.size());
		assertTrue(twoGroups.contains("gorans-ub3-pg"));
		assertTrue(twoGroups.contains("gorans-ub3-my"));

		// Case insensitive, and tolerant of spaces around the comma
		assertEquals(6, DbxCentralServerLayout.getServerNamesInGroups("production (sybase) , PRODUCTION (OTHER DBMS)", filename).size());

		// Unknown / blank -> empty, never null
		assertEquals(0, DbxCentralServerLayout.getServerNamesInGroups("No Such Group", filename).size());
		assertEquals(0, DbxCentralServerLayout.getServerNamesInGroups(""             , filename).size());
		assertEquals(0, DbxCentralServerLayout.getServerNamesInGroups(null           , filename).size());

		Files.delete(configFile);
	}

	/** A layout file with NO '#FORMAT; GROUP;' lines must give an empty map, not blow up. */
	@Test
	public void testServerGroupLookup_noGroups() throws Exception
	{
		Path configFile = Files.createTempFile(null, null);

		String fileContent = ""
				+ "GORAN_UB0_DS ; 1 ; ASE Version 12.5.4 ; ${DBXTUNE_CENTRAL_BASE}/bin/start_asetune.sh <SRVNAME> \n"
				+ "GORAN_UB3_DS ; 1 ; ASE Version 16.0   ; ${DBXTUNE_CENTRAL_BASE}/bin/start_asetune.sh <SRVNAME> \n";

		Files.write(configFile, fileContent.getBytes(StandardCharsets.UTF_8));

		DbxCentralServerLayout.clearServerGroupCache();

		assertEquals(0, DbxCentralServerLayout.getServerNameToGroupMap(configFile.toString()).size());
		assertNull  (   DbxCentralServerLayout.getGroupNameForServer("GORAN_UB0_DS", configFile.toString()));

		Files.delete(configFile);
	}

	/** A missing file must give an empty map (and be cached), not throw. */
	@Test
	public void testServerGroupLookup_missingFile() throws Exception
	{
		DbxCentralServerLayout.clearServerGroupCache();

		String filename = new File(System.getProperty("java.io.tmpdir"), "NO_SUCH_SERVER_LIST_FILE.txt").toString();

		assertEquals(0, DbxCentralServerLayout.getServerNameToGroupMap(filename).size());
		assertEquals(0, DbxCentralServerLayout.getServerNameToGroupMap(filename).size()); // 2nd call -> served from the cache
		assertNull  (   DbxCentralServerLayout.getGroupNameForServer("GORAN_UB0_DS", filename));
	}

	/** The cached map must be rebuilt when the SERVER_LIST file changes. */
	@Test
	public void testServerGroupCacheInvalidation() throws Exception
	{
		Path configFile = Files.createTempFile(null, null);
		Files.write(configFile, GROUP_FILE_CONTENT.getBytes(StandardCharsets.UTF_8));

		DbxCentralServerLayout.clearServerGroupCache();

		String filename = configFile.toString();

		assertEquals("Production (Sybase)", DbxCentralServerLayout.getGroupNameForServer("GORAN_UB0_DS", filename));

		// Move GORAN_UB0_DS into another group
		String changedContent = ""
				+ "#FORMAT; GROUP; Development and Test \n"
				+ "GORAN_UB0_DS ; 1 ; ASE Version 12.5.4 ; ${DBXTUNE_CENTRAL_BASE}/bin/start_asetune.sh <SRVNAME> \n";

		Files.write(configFile, changedContent.getBytes(StandardCharsets.UTF_8));

		// Bump the last-modified time, so we do not depend on the file systems timestamp resolution
		File f = configFile.toFile();
		assertTrue(f.setLastModified(f.lastModified() + 2000));

		assertEquals("Development and Test", DbxCentralServerLayout.getGroupNameForServer("GORAN_UB0_DS", filename));
		assertEquals(1, DbxCentralServerLayout.getServerNameToGroupMap(filename).size());

		Files.delete(configFile);
	}
}

