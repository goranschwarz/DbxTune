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
package com.asetune.test;

import java.io.File;

public class CreateHtmlImagePreviewPage
{
	public static void main(String[] args)
	{
		String pathname = "";
		pathname = "C:\\Users\\i063783\\Desktop\\icons\\fatcow-hosting-icons-3.9.2-all\\fatcow-hosting-icons-3.9.2\\FatCow_Icons16x16";
		pathname = "C:\\Users\\gorans\\Desktop\\icons\\fatcow-hosting-icons-3.9.2\\FatCow_Icons16x16";
		pathname = "C:\\Users\\gorans\\Desktop\\icons\\fatcow-hosting-icons-3.9.2\\FatCow_Icons32x32";

		if ( args.length == 0 )
		{
			System.out.println("");
			System.out.println("Create a HTML page with thumnail presentation of images.");
			System.out.println("");
			System.out.println("Usage: directory");
			System.out.println("");
//			return;
		}
		else
		{
			pathname = args[0];
		}

		File dirName = new File(pathname);
		File[] listOfFiles = dirName.listFiles();

		System.out.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 3.2 Final//EN\">");
		System.out.println("<HTML>");
		System.out.println("<HEAD>");
		System.out.println("<TITLE></TITLE>");
		System.out.println("</HEAD>");
		System.out.println("<BODY>");

		for (File file : listOfFiles)
		{
			if ( file.isFile() )
				System.out.println("<IMG SRC=\""+file.getName()+"\" ALT=\""+file.getName()+"\" TITLE=\""+file.getName()+"\" BORDER=0>");
		}

		System.out.println("</BODY>");
		System.out.println("</HTML>");
	}
}
