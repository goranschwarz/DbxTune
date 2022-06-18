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
package com.asetune.central.pcs.objects;

import static org.junit.Assert.assertEquals;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.Test;

import com.asetune.central.controllers.Helper;
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
}

