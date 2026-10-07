/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
 * 
 * This file is part of DbxTune
 * DbxTune is a family of sub-products *Tune, hence the Dbx
 * Here are some of the tools: AseTune, IqTune, RsTune, RaxTune, HanaTune, 
 *          SqlServerTune, PostgresTune, MySqlTune, MariaDbTune, Db2Tune, ...
 * 
 * DbxTune is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.dbxtune.alarm.events;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.List;

import com.dbxtune.utils.NumberUtils;

/**
 * Base for the in-line (anonymous class) {@link AlarmDescriptionProvider}'s in the CM's sendAlarmRequest().
 * <p>
 * It holds what the provider needs later (at RAISE/RE-RAISE/CANCEL): the PK of the row the alarm is about and the threshold,
 * and has helpers for the CANCEL text: {@link #getAlarmCancelText(String, Object, String)} (<code>"... is now X (threshold T unit)"</code>),
 * {@link #getAlarmCancelTextGone(String)} (<code>"... is no longer present"</code>) and {@link #toAlarmCancelValue(Object)}.
 * <p>
 * The provider is written where the alarm is created, see the example in {@link AlarmDescriptionProvider}. The variants:
 * <ul>
 *   <li><b>A row with a PK</b> (a database, disk, config...): <code>new AlarmDescriptionProviderBase(cm.getPk(), cm.getAbsPkValue(r), threshold)</code>,
 *       find the row with <code>cm.getAbsRowIdForPkValue(getRowPk())</code>, rebuild the table + graphs from the CURRENT data.</li>
 *   <li><b>No row</b> (single row CM's, sums, averages, moving averages): <code>new AlarmDescriptionProviderBase(null, null, threshold)</code>,
 *       read row 0 / the sum / the average directly.</li>
 *   <li><b>A row without a PK</b>: find it by a column, <code>cm.getAbsRowIdWhere("job_name", job_name)</code>.</li>
 *   <li><b>"Offender" alarms</b> (statements, sessions, SQL text): capture the table in a <code>final String</code> when the alarm is (re)raised,
 *       only the graphs and the CANCEL text are from the CURRENT data.</li>
 * </ul>
 * Inside the provider: use 'cm' (the parameter) to read data, and 'rowId' (never the 'r' of the sendAlarmRequest() loop).
 */
public abstract class AlarmDescriptionProviderBase
implements AlarmDescriptionProvider
{
	private final List<String> _pkCols;
	private final String _rowPk;
	private final Number _threshold;

	/**
	 * @param pkCols     List of pk columns (can be null or empty)
	 * @param rowPk      The PK value of the row the alarm is about (from getAbsPkValue(r)), used to find the row again, can be null
	 * @param threshold  The threshold that was crossed (can be null)
	 */
	public AlarmDescriptionProviderBase(List<String> pkCols, String rowPk, Number threshold)
	{
		_pkCols    = pkCols;
		_rowPk     = rowPk;
		_threshold = threshold;
	}
	
	public List<String> getPkCols()    { return _pkCols; }
	public String       getRowPk()     { return _rowPk; }
	public Number       getThreshold() { return _threshold; }
	
	/**
	 * For the CANCEL text: <code>"&lt;label&gt; is now &lt;valueNow&gt; (threshold &lt;threshold&gt;)"</code><br>
	 * Decimal numbers are rounded to 1 decimal. 'threshold' null = no threshold part.
	 */
	public String getAlarmCancelText(String label, Object valueNow)
	{
		return getAlarmCancelText(label, valueNow, "");
	}

	/**
	 * Same as getAlarmCancelText(label, valueNow), but with a unit after the threshold: <code>"... (threshold 1000 MB)"</code>
	 * @param thresholdUnit  for example " MB", "%" or " seconds" (null or "" = no unit)
	 */
	public String getAlarmCancelText(String label, Object valueNow, String thresholdUnit)
	{
		return label + " is now " + toAlarmCancelValue(valueNow)
			+ (getThreshold() == null ? "" : " (threshold " + toAlarmCancelValue(getThreshold()) + (thresholdUnit == null ? "" : thresholdUnit) + ")");
	}

	/** For the CANCEL text: <code>"&lt;label&gt; is no longer present"</code> (the row is gone, for example the session or database) */
	public String getAlarmCancelTextGone(String label)
	{
		return label + " is no longer present";
	}

	/** For the CANCEL text: null -&gt; "unknown", decimal numbers rounded to 1 decimal, dates as yyyy-MM-dd HH:mm:ss */
	public String toAlarmCancelValue(Object val)
	{
		if (val == null)
			return "unknown";

		if (val instanceof Double || val instanceof Float || val instanceof BigDecimal)
			return NumberUtils.round(((Number) val).doubleValue(), 1) + "";

		if (val instanceof java.util.Date)
			return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format((java.util.Date) val);

		return val.toString();
	}
}
