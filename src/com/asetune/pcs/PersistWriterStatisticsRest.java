package com.asetune.pcs;

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
			+ ", sendCount=" + _sendCount;
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

	@Override
	public void clear()
	{
		super.clear();

		_lastSendTimeInMs = 0;

		// DO NOT RESET THE SUM
	}
}
