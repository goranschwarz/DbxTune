/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package asemon.gui;

import java.sql.Timestamp;

import org.apache.log4j.lf5.LogLevel;
import org.apache.log4j.lf5.LogRecord;
import org.apache.log4j.spi.ThrowableInformation;

import asemon.utils.StringUtil;

public class Log4jLogRecord
extends LogRecord
{
    private static final long serialVersionUID = -4711707672226871758L;

	public Log4jLogRecord()
	{
	}

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
