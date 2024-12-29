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
package com.dbxtune.test;

import java.io.File;

import javax.swing.filechooser.FileSystemView;

public class FileSystemTest
{
	public static void main(String[] args)
	{
		test(new File("c:"));
		test(new File("c:\\"));
//		test(new File("c:\\projects\\dbxtune\\data\\qqqqq"));
		test(new File("h:\\"));
//		test(new File("h:\\xxxx"));
		test(new File("${DBXTUNE_HOME}/data/xxx"));
	}
	public static void test(File f)
	{
		FileSystemView fsv = FileSystemView.getFileSystemView();

		System.out.println("------------------------------------------------------------");
		System.out.println("File: " + f);
		System.out.println("fsv.getSystemDisplayName(f)    : " + fsv.getSystemDisplayName(f));
		System.out.println("fsv.getSystemTypeDescription(f): " + fsv.getSystemTypeDescription(f));
		System.out.println("fsv.isDrive(f)                 : " + fsv.isDrive(f));
		System.out.println("fsv.isComputerNode(f)          : " + fsv.isComputerNode(f));
		System.out.println("fsv.isFileSystem(f)            : " + fsv.isFileSystem(f));
		System.out.println("fsv.isFloppyDrive(f)           : " + fsv.isFloppyDrive(f));
		System.out.println();
	}
}
