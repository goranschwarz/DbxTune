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

	public static final String  SP_MISSING_STATS_CR_STR       = "2010-11-16";  // "YYYY-MM-DD"
	public static       Date    SP_MISSING_STATS_CRDATE       = null;

	public static final String  SP_LIST_UNUSED_INDEXES_CR_STR = "2010-11-16";  // "YYYY-MM-DD"
	public static       Date    SP_LIST_UNUSED_INDEXES_CRDATE = null;

	public static final String  SP_WHOISW_CR_STR              = "2010-11-16";  // "YYYY-MM-DD"
	public static       Date    SP_WHOISW_CRDATE              = null;

	public static final String  SP_LOCK2_CR_STR               = "2010-11-16";  // "YYYY-MM-DD"
	public static       Date    SP_LOCK2_CRDATE               = null;

	public static final String  SP_LOCKSUM_CR_STR             = "2010-11-16";  // "YYYY-MM-DD"
	public static       Date    SP_LOCKSUM_CRDATE             = null;

	public static final String  SP_OPENTRAN_CR_STR            = "2010-11-16";  // "YYYY-MM-DD"
	public static       Date    SP_OPENTRAN_CRDATE            = null;

	public static final String  SP_BLOCK_CR_STR               = "2010-11-16";  // "YYYY-MM-DD"
	public static       Date    SP_BLOCK_CRDATE               = null;

	static
	{
		try
		{
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

			SP_MISSING_STATS_CRDATE       = sdf.parse(SP_MISSING_STATS_CR_STR);
			SP_LIST_UNUSED_INDEXES_CRDATE = sdf.parse(SP_LIST_UNUSED_INDEXES_CR_STR);

			SP_WHOISW_CRDATE              = sdf.parse(SP_WHOISW_CR_STR);
			SP_LOCK2_CRDATE               = sdf.parse(SP_LOCK2_CR_STR);
			SP_LOCKSUM_CRDATE             = sdf.parse(SP_LOCKSUM_CR_STR);
			SP_OPENTRAN_CRDATE            = sdf.parse(SP_OPENTRAN_CR_STR);
			SP_BLOCK_CRDATE               = sdf.parse(SP_BLOCK_CR_STR);
		}
		catch(ParseException e)
		{
			System.out.println(e.getMessage());
		}
	}
}