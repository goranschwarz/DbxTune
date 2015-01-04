package com.asetune.tools.sqlw.msg;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.asetune.utils.ColorUtils;
import com.asetune.utils.TimeUtils;

public class JClientExecTime
extends JAseMessage
{
	private static final long serialVersionUID = 1L;

	private long _execStartTime;
	private long _execStopTime;
	private long _execFinnishTime;
	private long _execReadRsTime;

	public JClientExecTime(final long execStartTime, final long execStopTime, final long execFinnishTime, long execReadRsSum, int atLine, String originSql)
	{
		super("Client Exec Time: " + TimeUtils.msToTimeStr( "%MM:%SS.%ms", execFinnishTime - execStartTime) 
				+ " (sqlExec="     + TimeUtils.msToTimeStr( "%MM:%SS.%ms", execStopTime    - execStartTime)
				+ ", readResults=" + TimeUtils.msToTimeStr( "%MM:%SS.%ms", execReadRsSum)
				+ ", other="       + TimeUtils.msToTimeStr( "%MM:%SS.%ms", (execFinnishTime - execStopTime) - execReadRsSum)
				+ "), at '"+(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()))
				+"', for SQL starting at Line "+atLine, originSql );

		_execStartTime   = execStartTime;
		_execStopTime    = execStopTime;
		_execFinnishTime = execFinnishTime;
		_execReadRsTime  = execReadRsSum;

//		init();
		
		setForeground(ColorUtils.VERY_DARK_GREEN);
	}
	
	public Object getExecStartTime()   { return _execStartTime; }
	public Object getExecStopTime()    { return _execStopTime; }
	public Object getExecFinnishTime() { return _execFinnishTime; }
	public Object getExecReadRsTime()  { return _execReadRsTime; }
}
