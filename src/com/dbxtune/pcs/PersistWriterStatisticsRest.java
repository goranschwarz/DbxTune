/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
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
package com.dbxtune.pcs;

import java.util.TimeZone;

import com.dbxtune.utils.TimeUtils;

public class PersistWriterStatisticsRest
extends PersistWriterStatistics
{
	private long _lastSendTimeInMs = 0;
	private long _sumSendTimeInMs  = 0;
	private long _maxSendTimeInMs  = 0;
	private long _sendCount        = 0;

	private long _lastCrJsonTimeInMs = 0;
	private long _sumCrJsonTimeInMs  = 0;
	private long _maxCrJsonTimeInMs  = 0;
	private long _crJsonCount        = 0;

	private long _lastSentTimestamp  = 0;

	@Override
	public String getStatisticsString()
	{
		return 
			  "lastCrJsonTimeInMs="+ _lastCrJsonTimeInMs
			+ ", avgCrJsonTimeInMs=" + ( _crJsonCount == 0 ? "na" : _sumCrJsonTimeInMs/_crJsonCount ) 
			+ ", maxCrJsonTimeInMs=" + _maxCrJsonTimeInMs 
			+ ", crJsonCount=" + _crJsonCount
			+ ", lastSendTimeInMs="+ _lastSendTimeInMs
			+ ", avgSendTimeInMs=" + ( _sendCount == 0 ? "na" : _sumSendTimeInMs/_sendCount ) 
			+ ", maxSendTimeInMs=" + _maxSendTimeInMs 
			+ ", sendCount=" + _sendCount
			+ getLastSentTimestampStr()
			;
	}

	public void setLastSendTimeInMs(long sendTime)
	{
		_lastSendTimeInMs = sendTime;
		
		_sendCount++;
		_sumSendTimeInMs += sendTime;
		_maxSendTimeInMs = Math.max(sendTime, _maxSendTimeInMs);
	}

	public void setLastCreateJsonTimeInMs(long createTime)
	{
		_lastCrJsonTimeInMs = createTime;
		
		_crJsonCount++;
		_sumCrJsonTimeInMs += createTime;
		_maxCrJsonTimeInMs = Math.max(createTime, _maxCrJsonTimeInMs);
	}

	public void setLastSentTimestamp(long ts)
	{
		_lastSentTimestamp = ts;
	}

	public long getLastSentTimestamp()
	{
		return _lastSentTimestamp;
	}

	public String getLastSentTimestampStr()
	{
		if (_lastSentTimestamp <= 0)
			return "";

		String tz = TimeZone.getDefault().getID();
		String ltzStr = TimeUtils.toString   (_lastSentTimestamp);
		String utcStr = TimeUtils.toStringUtc(_lastSentTimestamp);
		return ", lastSentTs=[" + tz + "='" + ltzStr + "', UTC='" + utcStr + "', UTC_ts=" + _lastSentTimestamp + "]";
	}

	@Override
	public void clear()
	{
		super.clear();

		_lastSendTimeInMs = 0;

		// DO NOT RESET THE SUM
	}
}
