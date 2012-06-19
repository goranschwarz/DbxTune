package com.asetune.test;

import java.io.File;
import java.io.IOException;

public class HomeDirectoryTest
{
	private static final String USER_HOME = System.getProperty("user.home");
	private static final String ASETUNE_HOME_STR = USER_HOME+"/.asetune";

	public static void main(String[] args)
	{
		File asetuneHomeDir = new File(ASETUNE_HOME_STR);

		
		try
		{
			System.out.println("asetuneHomeDir='"+asetuneHomeDir.getCanonicalPath()+"'.");
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		if (asetuneHomeDir.exists())
		{
			System.out.println("dir '"+ASETUNE_HOME_STR+"' existed.");
		}
	}
}
