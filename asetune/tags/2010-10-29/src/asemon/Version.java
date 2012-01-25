/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package asemon;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Version
{
	public static final String PRODUCT_STRING     = "AseMon";
//	public static final String VERSION_STRING     = "1.0.0";
	public static final String VERSION_STRING     = "0.0.161.dev";
	public static final String BUILD_STRING       = "2010-10-29/build 61";

	public static final boolean IS_DEVELOPMENT_VERSION   = true; // if true: date expiration will be checked on startup
	public static final String  DEV_VERSION_EXPIRE_STR  = "2011-01-30";  // "YYYY-MM-DD"
	public static       Date    DEV_VERSION_EXPIRE_DATE = null;

	public static final String SOURCE_DATE_STRING = "$Date$";
	public static final String SOURCE_REV_STRING  = "$Revision$";

	static
	{
		try
		{
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			DEV_VERSION_EXPIRE_DATE = sdf.parse(DEV_VERSION_EXPIRE_STR);
//			DEV_VERSION_EXPIRE_DATE = DateFormat.getDateInstance(DateFormat.SHORT, Locale.US).parse(DEV_VERSION_EXPIRE_STR);
		}
		catch(ParseException e)
		{
			System.out.println(e.getMessage());
		}
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
*/