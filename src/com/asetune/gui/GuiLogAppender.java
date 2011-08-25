/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.gui;

import java.util.Enumeration;

import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Logger;
import org.apache.log4j.lf5.LogLevel;
import org.apache.log4j.lf5.LogLevelFormatException;
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;


public class GuiLogAppender
    extends AppenderSkeleton
{
	private Log4jTableModel _logTable = new Log4jTableModel();
	private static GuiLogAppender _instance = null;

	// Initialize the TRACE level
	static
	{
		// TRACE is not registered, in the LogLevel...
		// so it makes LogLevel.valueOf(level) to throw LogLevelFormatException
		LogLevel.register( new LogLevel("TRACE", 5) );
	}

	@SuppressWarnings("rawtypes")
	public GuiLogAppender()
	{
		setName( "GuiLogAppender" );
		_instance = this;

		//- TODO: CAN WE DO IT AT NON-ROOT-LOGGER
		//- TODO: CAN WE CHANGE THE setLevel(in the Log4jViewer.SetLevelDialog) to be just "GuiLogAppender" logger... or choose different loggers/appenders from a combobox
		//- TODO: JColorChooser, JColorComboBox ....at take a new look at that

		// Go to Log4j and register/add this appender
		// this so we don't need it in the Log4j properties file
		boolean isInstalled = false;
		Enumeration en = Logger.getRootLogger().getAllAppenders();
		while (en.hasMoreElements())
		{
			Appender a = (Appender) en.nextElement();
			if (a.equals(this))
				isInstalled = true;
		}
		if (! isInstalled)
		{
			//System.out.println("Installing the '"+getName()+"' as a root logger in log4j.");
			Logger.getRootLogger().addAppender(this);
		}
	}
	
	protected void append(LoggingEvent event)
	{
		String category = event.getLoggerName();
		String logMessage = event.getRenderedMessage();
		String nestedDiagnosticContext = event.getNDC();
		String threadDescription = event.getThreadName();
		String level = event.getLevel().toString();
		long time = event.timeStamp;
		LocationInfo locationInfo = event.getLocationInformation();

		Log4jLogRecord record = new Log4jLogRecord();
		record.setCategory(category);
		record.setMessage(logMessage);
		record.setLocation(locationInfo.fullInfo);
		record.setMillis(time);
		record.setThreadDescription(threadDescription);
		if (nestedDiagnosticContext != null)
			record.setNDC(nestedDiagnosticContext);
		else
			record.setNDC("");
		if (event.getThrowableInformation() != null)
			record.setThrownStackTrace(event.getThrowableInformation());
		try
		{
			record.setLevel(LogLevel.valueOf(level));
		}
		catch (LogLevelFormatException e)
		{
//			System.out.println("level='"+level+"', e="+e);
//			e.printStackTrace();
			record.setLevel(LogLevel.WARN);
		}

		if (_logTable != null)
			_logTable.addMessage(record);
	}

	public void close()
	{
	}

	public boolean requiresLayout()
	{
		return false;
	}
	
	public static synchronized GuiLogAppender getInstance()
	{
		if (_instance == null)
			_instance = new GuiLogAppender();
		return _instance;
	}

	public static Log4jTableModel getTableModel()
	{
		if (_instance == null) 
			return null;
		return getInstance()._logTable;
	}
}
