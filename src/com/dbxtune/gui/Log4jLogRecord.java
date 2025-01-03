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

/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.dbxtune.gui;

import java.sql.Timestamp;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;

import com.dbxtune.utils.StringUtil;


public class Log4jLogRecord
{
    private static final long serialVersionUID = -4711707672226871758L;

	private static long _globalSequence = 0;
	private long _sequence;
	private long _timeMs;
	private String _level;
	private String _threadName;
	private String _className;
	private String _location;
	private Throwable _throwable;
	private String _message;

	private LogEvent _log4jLogEvent;

	/** This one should be used */
	public Log4jLogRecord(LogEvent log4jLogEvent)
	{
		_sequence      = ++_globalSequence;
		_log4jLogEvent = log4jLogEvent;

		setTimeMs    (log4jLogEvent.getTimeMillis());
		setLevel     (log4jLogEvent.getLevel() + "");
		setThreadName(log4jLogEvent.getThreadName());
		setClassName (log4jLogEvent.getLoggerName());
		setLocation  (log4jLogEvent.getSource() + "");
		setThrowable (log4jLogEvent.getThrown());
		setMessage   (log4jLogEvent.getMessage().getFormattedMessage());
		
	}

	public long      getSequence  () { return _sequence;   }
	public long      getTimeMs    () { return _timeMs;     }
	public String    getLevel     () { return _level;      }
	public String    getThreadName() { return _threadName; }
	public String    getClassName () { return _className;  }
	public String    getLocation  () { return _location;   }
	public Throwable getThrowable () { return _throwable;  }
	public String    getMessage   () { return _message;    }
	public String    getThrownStackTraceAsString() { return StringUtil.stackTraceToString(_throwable);  }
	
//	public void setSequence  (long      sequence  ) { _sequence   = sequence  ; }
	public void setTimeMs    (long      timeMs    ) { _timeMs     = timeMs    ; }
	public void setLevel     (String    level     ) { _level      = level     ; }
	public void setThreadName(String    threadName) { _threadName = threadName; }
	public void setClassName (String    className ) { _className  = className ; }
	public void setLocation  (String    location  ) { _location   = location  ; }
	public void setThrowable (Throwable throwable ) { _throwable  = throwable ; }
	public void setMessage   (String    message   ) { _message    = message   ; }


	public boolean isWarningLevel()
	{
		boolean isWarning = false;
//		if ( LogLevel.WARN.equals(getLevel()) )
//			isWarning = true;
		if ( _log4jLogEvent.getLevel().isInRange(Level.WARN, Level.WARN) )
			isWarning = true;
		return isWarning;
	}

	public boolean isSevereLevel()
	{
		boolean isSevere = false;
//		if (LogLevel.ERROR.equals(getLevel()) || LogLevel.FATAL.equals(getLevel()))
//			isSevere = true;
		if ( _log4jLogEvent.getLevel().isMoreSpecificThan(Level.ERROR) )
			isSevere = true;
		return isSevere;
	}

//	public void setThrownStackTrace(ThrowableInformation throwableInfo)
//	{
//		String stackTraceArray[] = throwableInfo.getThrowableStrRep();
//		StringBuffer stackTrace = new StringBuffer();
//		for (int i = 0; i < stackTraceArray.length; i++)
//		{
//			String nextLine = stackTraceArray[i] + "\n";
//			stackTrace.append(nextLine);
//		}
//
//		super._thrownStackTrace = stackTrace.toString();
//	}
	
	public String getToolTipText()
	{
		StringBuffer tt = new StringBuffer("<html>");

//		tt.append("    <b>Sequence id: </b>"); tt.append( getSequenceNumber() );
//		tt.append("<br><b>Time: </b>");        tt.append( new Timestamp(getMillis()) );
//		tt.append("<br><b>Level: </b>");       tt.append( getLevel() );
//		tt.append("<br><b>Class Name: </b>");  tt.append( getCategory() );
//		tt.append("<br><b>Location: </b>");    tt.append( getLocation() );
//		tt.append("<br><b>Message: </b>");     tt.append( StringUtil.makeApproxLineBreak(getMessage(), 100, 2, "<BR>") );
//		if ( getThrownStackTrace() != null )
//		{
//			tt.append("<br><b>Exception: </b><br>");    
//			tt.append( getThrownStackTrace().replaceAll("\\n", "<br>") );
//		}
//		tt.append("</html>");

		tt.append("    <b>Sequence id: </b>"); tt.append( _sequence );
		tt.append("<br><b>Time: </b>");        tt.append( new Timestamp(_timeMs) );
		tt.append("<br><b>Level: </b>");       tt.append( _level );
		tt.append("<br><b>Class Name: </b>");  tt.append( _className );
		tt.append("<br><b>Location: </b>");    tt.append( _location ); // ????
		tt.append("<br><b>Message: </b>");     tt.append( StringUtil.makeApproxLineBreak(_message, 100, 2, "<BR>") );

		if ( _throwable != null )
		{
			tt.append("<br><b>Exception: </b><br>");    
			tt.append( StringUtil.stackTraceToString(_throwable).replaceAll("\\n", "<br>") );
		}
		tt.append("</html>");
		
		return tt.toString();		
	}
}
