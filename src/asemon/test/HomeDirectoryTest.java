package asemon.test;

import java.io.File;
import java.io.IOException;

public class HomeDirectoryTest
{
	private static final String USER_HOME = System.getProperty("user.home");
	private static final String ASEMON_HOME_STR = USER_HOME+"/.asemon";

	public static void main(String[] args)
	{
		File asemonHomeDir = new File(ASEMON_HOME_STR);

		
		try
		{
			System.out.println("asemonHomeDir='"+asemonHomeDir.getCanonicalPath()+"'.");
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		if (asemonHomeDir.exists())
		{
			System.out.println("dir '"+ASEMON_HOME_STR+"' existed.");
		}
	}
}
