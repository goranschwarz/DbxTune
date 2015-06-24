package com.asetune.test;

import java.io.File;

import javax.swing.filechooser.FileSystemView;

public class FileSystemTest
{
	public static void main(String[] args)
	{
		test(new File("c:"));
		test(new File("c:\\"));
//		test(new File("c:\\projects\\asetune\\data\\qqqqq"));
		test(new File("h:\\"));
//		test(new File("h:\\xxxx"));
		test(new File("${ASETUNE_HOME}/data/xxx"));
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
