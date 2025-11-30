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
package com.dbxtune.tools.sqlw.msg;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.dbxtune.utils.ColorUtils;
import com.dbxtune.utils.TimeUtils;

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
		super("Client Exec Time: " + TimeUtils.msToTimeStr( "%?HH[:]%MM:%SS.%ms", execFinnishTime - execStartTime) 
				+ " (sqlExec="     + TimeUtils.msToTimeStr( "%?HH[:]%MM:%SS.%ms", execStopTime    - execStartTime)
				+ ", readResults=" + TimeUtils.msToTimeStr( "%?HH[:]%MM:%SS.%ms", execReadRsSum)
				+ ", other="       + TimeUtils.msToTimeStr( "%?HH[:]%MM:%SS.%ms", (execFinnishTime - execStopTime) - execReadRsSum)
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
