/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package asemon;

public class Version
{
	public static final String PRODUCT_STRING     = "AseMon";
//	public static final String VERSION_STRING     = "1.0.0";
	public static final String VERSION_STRING     = "0.0.138.dev";
	public static final String BUILD_STRING       = "2009-03-31/build 38";

	public static final String SOURCE_DATE_STRING = "$Date$";
	public static final String SOURCE_REV_STRING  = "$Revision$";

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