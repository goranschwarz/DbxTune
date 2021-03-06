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
package com.asetune.gui;

import java.sql.Timestamp;

import org.apache.log4j.lf5.LogLevel;
import org.apache.log4j.lf5.LogRecord;
import org.apache.log4j.spi.ThrowableInformation;

import com.asetune.utils.StringUtil;


public class Log4jLogRecord
extends LogRecord
{
    private static final long serialVersionUID = -4711707672226871758L;

	public Log4jLogRecord()
	{
	}

	public boolean isWarningLevel()
	{
		boolean isWarning = false;
		if ( LogLevel.WARN.equals(getLevel()) )
			isWarning = true;
		return isWarning;
	}

	@Override
	public boolean isSevereLevel()
	{
		boolean isSevere = false;
		if (LogLevel.ERROR.equals(getLevel()) || LogLevel.FATAL.equals(getLevel()))
			isSevere = true;
		return isSevere;
	}

	public void setThrownStackTrace(ThrowableInformation throwableInfo)
	{
		String stackTraceArray[] = throwableInfo.getThrowableStrRep();
		StringBuffer stackTrace = new StringBuffer();
		for (int i = 0; i < stackTraceArray.length; i++)
		{
			String nextLine = stackTraceArray[i] + "\n";
			stackTrace.append(nextLine);
		}

		super._thrownStackTrace = stackTrace.toString();
	}
	
	public String getToolTipText()
	{
		StringBuffer tt = new StringBuffer("<html>");

		tt.append("    <b>Sequence id: </b>"); tt.append( getSequenceNumber() );
		tt.append("<br><b>Time: </b>");        tt.append( new Timestamp(getMillis()) );
		tt.append("<br><b>Level: </b>");       tt.append( getLevel() );
		tt.append("<br><b>Class Name: </b>");  tt.append( getCategory() );
		tt.append("<br><b>Location: </b>");    tt.append( getLocation() );
		tt.append("<br><b>Message: </b>");     tt.append( StringUtil.makeApproxLineBreak(getMessage(), 100, 2, "<BR>") );
		if ( getThrownStackTrace() != null )
		{
			tt.append("<br><b>Exception: </b><br>");    
			tt.append( getThrownStackTrace().replaceAll("\\n", "<br>") );
		}
		tt.append("</html>");

		return tt.toString();		
	}
}
