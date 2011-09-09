/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.utils;

import java.util.Date;

/**
 * @author qschgor
 *
 */
public class TimeUtils
{

	/*---------------------------------------------------
	** class members
	**---------------------------------------------------
	*/

	/*---------------------------------------------------
	** Constructors
	**---------------------------------------------------
	*/

	/*---------------------------------------------------
	** Methods: local to class
	**---------------------------------------------------
	*/

	/**
	 * Convert a long into a time string HH:MM:SS.ms
	 */
	public static String msToTimeStr(long execTime)
	{
		String execTimeHH = "00";
		String execTimeMM = "00";
		String execTimeSS = "00";
		String execTimeMs = "000";

		// MS
		execTimeMs = "000" + execTime % 1000;
		execTimeMs = execTimeMs.substring(execTimeMs.length()-3);
		execTime = execTime / 1000;

		// Seconds
		execTimeSS = "00" + execTime % 60;
		execTimeSS = execTimeSS.substring(execTimeSS.length()-2);
		execTime = execTime / 60;

		// Minutes
		execTimeMM = "00" + execTime % 60;
		execTimeMM = execTimeMM.substring(execTimeMM.length()-2);
		execTime = execTime / 60;

		// Hour
//		execTimeHH = "00" + execTime % 60;
//		execTimeHH = execTimeHH.substring(execTimeHH.length()-2);
//		execTime = execTime / 60;
		execTimeHH = "00" + execTime;
		execTimeHH = (execTime < 100) ? execTimeHH.substring(execTimeHH.length()-2) : "" + execTime;

		return execTimeHH+":"+execTimeMM+":"+execTimeSS+":"+execTimeMs;
	}

	/**
	 * Convert a long into a time string 
	 * 
	 * @param format %HH:%MM:%SS.%ms
	 * @param execTime
	 * @return a string of the format description
	 */
	public static String msToTimeStr(String format, long execTime)
	{
		String execTimeHH = "00";
		String execTimeMM = "00";
		String execTimeSS = "00";
		String execTimeMs = "000";

		// MS
		execTimeMs = "000" + execTime % 1000;
		execTimeMs = execTimeMs.substring(execTimeMs.length()-3);
		execTime = execTime / 1000;

		// Seconds
		execTimeSS = "00" + execTime % 60;
		execTimeSS = execTimeSS.substring(execTimeSS.length()-2);
		execTime = execTime / 60;

		// Minutes
		execTimeMM = "00" + execTime % 60;
		execTimeMM = execTimeMM.substring(execTimeMM.length()-2);
		execTime = execTime / 60;

		// Hour
//		execTimeHH = "00" + execTime % 60;
//		execTimeHH = execTimeHH.substring(execTimeHH.length()-2);
//		execTime = execTime / 60;
		execTimeHH = "00" + execTime;
		execTimeHH = (execTime < 100) ? execTimeHH.substring(execTimeHH.length()-2) : "" + execTime;
		

		format = format.replaceAll("%HH", execTimeHH);
		format = format.replaceAll("%MM", execTimeMM);
		format = format.replaceAll("%SS", execTimeSS);
		format = format.replaceAll("%ms", execTimeMs);

		return format;
	}





	public static final int DD_MS          = 0;
	public static final int DD_MILLISECOND = DD_MS;
	public static final int DD_SS          = 1;
	public static final int DD_SECOND      = DD_SS;
	public static final int DD_MI          = 2;
	public static final int DD_MINUTE      = DD_MI;
	public static final int DD_HH          = 3;
	public static final int DD_HOUR        = DD_HH;
	public static final int DD_DD          = 4;
	public static final int DD_DAY         = DD_DD;
	public static final int DD_WK          = 5;
	public static final int DD_WEEK        = DD_WK;
	public static final int DD_MM          = 6;
	public static final int DD_MONTH       = DD_MM;

	public static int datediff(int type, Date startDate, Date endDate)
	{
		return datediff( type, startDate.getTime(), endDate.getTime() );
	}
	public static int datediff(int type, long startDate, long endDate)
	{
		long diffInSec = ((endDate / 1000) - (startDate / 1000));
		long ret = 0;

		switch (type)
		{
			case DD_MILLISECOND:
				ret = endDate - startDate;
				break;

			case DD_SECOND:
				ret = diffInSec;
				break;

			case DD_MINUTE:
				ret = diffInSec / 60;
				break;

			case DD_HOUR:
				ret = diffInSec / 60 / 60;
				break;

			case DD_DAY:
				ret = diffInSec / 60 / 60 / 24;
				break;

			case DD_WEEK:
				ret = diffInSec  / 60 / 60 / 24 / 7;
				break;

			case DD_MONTH:
				// Comparing ASE datediff(mm) on month and the below method
				// showed that using 365 days per year seemed to work all
				// the way upp to year 2069. Then this system will hopefully
				//  NOT be around anymore. (and I will be 100 years old :^)
				ret = diffInSec  / 2628000;   // 2628000 = 365      / 12 * 24 * 60 * 60;
				//ret = diffInSec  / 2629800; // 2629800 = 365.25   / 12 * 24 * 60 * 60;
				//ret = diffInSec  / 2629746; // 2629746 = 365.2425 / 12 * 24 * 60 * 60;
				break;

		}

		return (int) ret;
	}

	/*
	 * *********************************************************************
	 * ******* SUB CLASSES ******** SUB CLASSES ******** SUB CLASSES *******
	 * *********************************************************************
	 */


	/*
	 * *********************************************************************
	 * ********* TEST CODE ********* TEST CODE ********* TEST CODE *********
	 * *********************************************************************
	 */

}
