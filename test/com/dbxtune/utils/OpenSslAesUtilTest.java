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
package com.dbxtune.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.Before;
import org.junit.Test;

public class OpenSslAesUtilTest
{
	@Before
	public void beforeTest()
	{
		// Set Log4j Log Level
		Configurator.setRootLevel(Level.TRACE);
	}

	@Test
	public void testWriteAndReadPasswordFile()
	{
		try
		{
			// Temporary file to store password
			File   passwdFile = File.createTempFile("encrypted_passwords", ".enc");
			String passwdFileStr = passwdFile.toString();
			
			passwdFile.deleteOnExit();

			//-----------------------
			// WRITE
			//-----------------------

			// WRITE to new file for ANY Server
			String userName  = "sa";
			String srvName   = null;
			String txtPasswd = "sa-test-1";
			int    expRowCnt = 1;
			OpenSslAesUtil.writePasswdToFile(txtPasswd, userName, srvName, passwdFileStr, null);
			assertEquals(txtPasswd, OpenSslAesUtil.readPasswdFromFile(userName, srvName, passwdFileStr) );
			assertEquals(expRowCnt, getRowCount(passwdFile));
			printFileContent(passwdFile, txtPasswd);

			// UPDATE password for ANY Server
			userName  = "sa";
			srvName   = null;
			txtPasswd = "sa-test-2";
			expRowCnt = 1;
			OpenSslAesUtil.writePasswdToFile(txtPasswd, userName, srvName, passwdFileStr, null);
			assertEquals(txtPasswd, OpenSslAesUtil.readPasswdFromFile(userName, srvName, passwdFileStr) );
			assertEquals(expRowCnt, getRowCount(passwdFile));
			printFileContent(passwdFile, txtPasswd);

			// WRITE for SERVERNAME
			userName  = "sa";
			srvName   = "GORAN_1_ASE";
			txtPasswd = "sa-test-3";
			expRowCnt = 2;
			OpenSslAesUtil.writePasswdToFile(txtPasswd, userName, srvName, passwdFileStr, null);
			assertEquals(txtPasswd, OpenSslAesUtil.readPasswdFromFile(userName, srvName, passwdFileStr) );
			assertEquals(expRowCnt, getRowCount(passwdFile));
			printFileContent(passwdFile, txtPasswd);

			// WRITE for SERVERNAME
			userName  = "sa";
			srvName   = "GORAN_2_ASE";
			txtPasswd = "sa-test-4";
			expRowCnt = 3;
			OpenSslAesUtil.writePasswdToFile(txtPasswd, userName, srvName, passwdFileStr, null);
			assertEquals(txtPasswd, OpenSslAesUtil.readPasswdFromFile(userName, srvName, passwdFileStr) );
			assertEquals(expRowCnt, getRowCount(passwdFile));
			printFileContent(passwdFile, txtPasswd);

			// UPDATE for SERVERNAME
			userName  = "sa";
			srvName   = "GORAN_1_ASE";
			txtPasswd = "sa-test-5";
			expRowCnt = 3;
			OpenSslAesUtil.writePasswdToFile(txtPasswd, userName, srvName, passwdFileStr, null);
			assertEquals(txtPasswd, OpenSslAesUtil.readPasswdFromFile(userName, srvName, passwdFileStr) );
			assertEquals(expRowCnt, getRowCount(passwdFile));
			printFileContent(passwdFile, txtPasswd);

			
			//-----------------------
			// READ
			//-----------------------
			expRowCnt = 3;

			// Read previously saved password SERVERNAME
			userName  = "sa";
			srvName   = "GORAN_2_ASE";
			txtPasswd = "sa-test-4";
			assertEquals(txtPasswd, OpenSslAesUtil.readPasswdFromFile(userName, srvName, passwdFileStr) );
			assertEquals(expRowCnt, getRowCount(passwdFile));
			
			// Read previously saved password SERVERNAME
			userName  = "sa";
			srvName   = "GORAN_1_ASE";
			txtPasswd = "sa-test-5";
			assertEquals(txtPasswd, OpenSslAesUtil.readPasswdFromFile(userName, srvName, passwdFileStr) );
			assertEquals(expRowCnt, getRowCount(passwdFile));
			
			// Read previously saved password ANY-Server / FALLABACK
			userName  = "sa";
			srvName   = null;
			txtPasswd = "sa-test-2";
			assertEquals(txtPasswd, OpenSslAesUtil.readPasswdFromFile(userName, srvName, passwdFileStr) );
			assertEquals(expRowCnt, getRowCount(passwdFile));
			

			//-----------------------
			// REMOVE
			//-----------------------
			// remove previously saved password SERVERNAME
			userName  = "sa";
			srvName   = "GORAN_2_ASE";
			expRowCnt = 2;
			txtPasswd = "sa-test-2"; // since the readPasswd will use the FALLBACK
			assertEquals(true,      OpenSslAesUtil.removePasswdFromFile(userName, srvName, passwdFileStr));
			assertEquals(txtPasswd, OpenSslAesUtil.readPasswdFromFile(userName, srvName, passwdFileStr) );
			assertEquals(expRowCnt, getRowCount(passwdFile));
			printFileContent(passwdFile, txtPasswd);
			
			// remove previously saved password ANY-Server / FALLABACK
			userName  = "sa";
			srvName   = null;
			expRowCnt = 1;
			txtPasswd = null;
			assertEquals(true,      OpenSslAesUtil.removePasswdFromFile(userName, srvName, passwdFileStr));
			assertEquals(txtPasswd, OpenSslAesUtil.readPasswdFromFile(userName, srvName, passwdFileStr) );
			assertEquals(expRowCnt, getRowCount(passwdFile));
			printFileContent(passwdFile, txtPasswd);

			// remove previously saved password SERVERNAME
			userName  = "sa";
			srvName   = "GORAN_1_ASE";
			expRowCnt = 0;
			txtPasswd = null;
			assertEquals(true,      OpenSslAesUtil.removePasswdFromFile(userName, srvName, passwdFileStr));
			assertEquals(txtPasswd, OpenSslAesUtil.readPasswdFromFile(userName, srvName, passwdFileStr) );
			assertEquals(expRowCnt, getRowCount(passwdFile));
			printFileContent(passwdFile, txtPasswd);
			
		}
		catch (Exception ex)
		{
			fail("Exception was not expected: " + ex);
		}

	}

	private int getRowCount(File f)
	throws Exception
	{
		List<String> list = FileUtils.readLines(f, Charset.defaultCharset());

		return list.size();
	}

	private void printFileContent(File f, String testName)
	throws Exception
	{
		boolean debug = false;
		if ( ! debug )
			return;

		List<String> list = FileUtils.readLines(f, Charset.defaultCharset());
		
		System.out.println();
		System.out.println("----------------------------------------------------------------------");
		System.out.println("Test Name: " + testName);
		System.out.println("File '" + f + "', has " + list.size() + " rows.");

		int row = 0;
		for (String line : list)
		{
			System.out.println("row[" + row + "] |" + line + "|");
			row++;
		}
	}
}
