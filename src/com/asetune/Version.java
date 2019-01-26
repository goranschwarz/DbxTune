/* ----------------------------------------------------------------------------
**  Copyright (C) 2010-2018 Göran Schwarz
**
**  This file is part of DbxTune
**  DbxTune is a family of sub-products *Tune, hence the Dbx
**  Here are some of the tools: AseTune, IqTune, RsTune, RaxTune, HanaTune, 
**           SqlServerTune, PostgresTune, MySqlTune, MariaDbTune, Db2Tune, ...
**
**  DbxTune is free software: you can redistribute it and/or modify
**  it under the terms of the GNU General Public License as published by
**  the Free Software Foundation, either version 3 of the License, or
**  (at your option) any later version.
**
**  DbxTune is distributed in the hope that it will be useful,
**  but WITHOUT ANY WARRANTY; without even the implied warranty of
**  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
**  GNU General Public License for more details.
**
**  You should have received a copy of the GNU General Public License
**  along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
** ----------------------------------------------------------------------------
*/
/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Version
{
	public static       String PRODUCT_STRING     = "AseTune";      // Do not have spaces etc in this one
//	public static final String VERSION_STRING     = "4.0.0";        // Use this for public releases
	public static final String VERSION_STRING     = "4.0.0.11.dev"; // Use this for early releases
	public static final String BUILD_STRING       = "2019-01-16/build 259";

	public static final boolean IS_DEVELOPMENT_VERSION  = true; // if true: date expiration will be checked on startup
	public static final String  DEV_VERSION_EXPIRE_STR  = "2020-11-30";  // "YYYY-MM-DD" 
	public static       Date    DEV_VERSION_EXPIRE_DATE = null;

	public static final String SOURCE_DATE_STRING = "$Date$";
	public static final String SOURCE_REV_STRING  = "$Revision$";

//	public static final String APP_STORE_DIR = System.getProperty("user.home") + File.separator + ".asetune";

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