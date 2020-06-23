/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
 * 
 * This file is part of DbxTune
 * DbxTune is a family of sub-products *Tune, hence the Dbx
 * Here are some of the tools: AseTune, IqTune, RsTune, RaxTune, HanaTune, 
 *          SqlServerTune, PostgresTune, MySqlTune, MariaDbTune, Db2Tune, ...
 * 
 * DbxTune is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.cm.sql;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class VersionInfo
{
//	public static Calendar SP_MISSING_STATS_CRDATE = new GregorianCalendar(2009, 12, 10); // YYYY, MM, DD

	public static final String  SP_MISSING_STATS_CR_STR       = "2016-11-27";  // "YYYY-MM-DD"
	public static       Date    SP_MISSING_STATS_CRDATE       = null;

	public static final String  SP_ASETUNE_RA_STATS_CR_STR    = "2011-09-28";  // "YYYY-MM-DD"
	public static       Date    SP_ASETUNE_RA_STATS_CRDATE    = null;

	public static final String  SP_ASETUNE_QP_METRICS_CR_STR  = "2016-11-27";  // "YYYY-MM-DD"
	public static       Date    SP_ASETUNE_QP_METRICS_CRDATE  = null;

	public static final String  SP_LIST_UNUSED_INDEXES_CR_STR = "2011-04-14";  // "YYYY-MM-DD"
	public static       Date    SP_LIST_UNUSED_INDEXES_CRDATE = null;

	public static final String  SP_WHOISW_CR_STR              = "2016-04-18";  // "YYYY-MM-DD"
	public static       Date    SP_WHOISW_CRDATE              = null;

	public static final String  SP_WHOISB_CR_STR              = "2016-04-18";  // "YYYY-MM-DD"
	public static       Date    SP_WHOISB_CRDATE              = null;

	public static final String  SP_LOCK2_CR_STR               = "2011-04-14";  // "YYYY-MM-DD"
	public static       Date    SP_LOCK2_CRDATE               = null;

	public static final String  SP_LOCKSUM_CR_STR             = "2011-04-14";  // "YYYY-MM-DD"
	public static       Date    SP_LOCKSUM_CRDATE             = null;

	public static final String  SP_SPACEUSED2_CR_STR          = "2011-04-14";  // "YYYY-MM-DD"
	public static       Date    SP_SPACEUSED2_CRDATE          = null;

	public static final String  SP_OPENTRAN_CR_STR            = "2011-04-14";  // "YYYY-MM-DD"
	public static       Date    SP_OPENTRAN_CRDATE            = null;

	public static final String  SP__OPTDIAG_CR_STR            = "2006-06-20";  // "YYYY-MM-DD"
	public static       Date    SP__OPTDIAG_CRDATE            = null;

	public static final String  SP__UPDATE_INDEX_STAT_CR_STR  = "2011-04-14";  // "YYYY-MM-DD"
	public static       Date    SP__UPDATE_INDEX_STAT_CRDATE  = null;

	static
	{
		try
		{
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

			SP_MISSING_STATS_CRDATE       = sdf.parse(SP_MISSING_STATS_CR_STR);
			SP_ASETUNE_RA_STATS_CRDATE    = sdf.parse(SP_ASETUNE_RA_STATS_CR_STR);
			SP_ASETUNE_QP_METRICS_CRDATE  = sdf.parse(SP_ASETUNE_QP_METRICS_CR_STR);

			SP_WHOISW_CRDATE              = sdf.parse(SP_WHOISW_CR_STR);
			SP_WHOISB_CRDATE              = sdf.parse(SP_WHOISB_CR_STR);
			SP_LOCK2_CRDATE               = sdf.parse(SP_LOCK2_CR_STR);
			SP_LOCKSUM_CRDATE             = sdf.parse(SP_LOCKSUM_CR_STR);
			SP_SPACEUSED2_CRDATE          = sdf.parse(SP_SPACEUSED2_CR_STR);
			SP_OPENTRAN_CRDATE            = sdf.parse(SP_OPENTRAN_CR_STR);

			SP__OPTDIAG_CRDATE            = sdf.parse(SP__OPTDIAG_CR_STR);
			SP__UPDATE_INDEX_STAT_CRDATE  = sdf.parse(SP__UPDATE_INDEX_STAT_CR_STR);
		}
		catch(ParseException e)
		{
			System.out.println(e.getMessage());
		}
	}
}