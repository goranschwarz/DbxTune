/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package asemon.cm.sql;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class VersionInfo 
{
//	public static Calendar SP_MISSING_STATS_CRDATE = new GregorianCalendar(2009, 12, 10); // YYYY, MM, DD

	public static final String  SP_MISSING_STATS_CR_STR = "2009-12-14";  // "YYYY-MM-DD"
	public static       Date    SP_MISSING_STATS_CRDATE = null;

	static
	{
		try
		{
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			SP_MISSING_STATS_CRDATE = sdf.parse(SP_MISSING_STATS_CR_STR);
		}
		catch(ParseException e)
		{
			System.out.println(e.getMessage());
		}
	}
}