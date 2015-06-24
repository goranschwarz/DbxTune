package com.asetune.test;

import java.io.File;

public class CreateHtmlImagePreviewPage
{
	public static void main(String[] args)
	{
		String pathname = "";
		pathname = "C:\\Users\\i063783\\Desktop\\icons\\fatcow-hosting-icons-3.9.2-all\\fatcow-hosting-icons-3.9.2\\FatCow_Icons16x16";

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
