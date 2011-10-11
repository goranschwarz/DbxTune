/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Version
{
	public static       String PRODUCT_STRING     = "AseTune"; // Do not have spaces etc in this one
//	public static final String VERSION_STRING     = "2.6.0";
	public static final String VERSION_STRING     = "2.6.0.1.dev";
	public static final String BUILD_STRING       = "2011-10-11/build 91";

	public static final boolean IS_DEVELOPMENT_VERSION   = true; // if true: date expiration will be checked on startup
	public static final String  DEV_VERSION_EXPIRE_STR  = "2012-02-30";  // "YYYY-MM-DD"
	public static       Date    DEV_VERSION_EXPIRE_DATE = null;

	public static final String SOURCE_DATE_STRING = "$Date$";
	public static final String SOURCE_REV_STRING  = "$Revision$";

	public static final String APP_STORE_DIR = System.getProperty("user.home") + File.separator + ".asetune";

	static
	{
		try
		{
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			DEV_VERSION_EXPIRE_DATE = sdf.parse(DEV_VERSION_EXPIRE_STR);
		}
		catch(ParseException e)
		{
			System.out.println(e.getMessage());
		}
	}

	/** This normally NOT used */
	public static void setAppName(String appname)
	{
		PRODUCT_STRING = appname;
	}

	public static String getAppName()
	{
		return PRODUCT_STRING;
	}

	public static String getVersionStr()
	{
		return VERSION_STRING;
	}

	public static String getBuildStr()
	{
		return BUILD_STRING;
	}

	public static String getSourceDate()
	{
		return SOURCE_DATE_STRING.replaceFirst("\\$Date: ", "").replaceFirst(" \\$", "").substring(0, "yyyy-mm-dd HH:MM:SS".length());
	}
	public static String getSourceRev()
	{
		return SOURCE_REV_STRING.replaceFirst("\\$Revision: ", "").replaceFirst(" \\$", "");
	}
}

/*------------------------------------------------------
**---- NOTE ---- NOTE ---- NOTE ---- NOTE ---- NOTE ----   
**------------------------------------------------------
**
** DO NOT FORGET TO UPDATE:
** - history.html
** - todo.html
**
** The above 2 files are read from the AboutBox.java
**
** READ the file: howto_make_release.txt
*/