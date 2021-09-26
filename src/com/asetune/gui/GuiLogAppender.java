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

import java.util.Enumeration;

import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Logger;
import org.apache.log4j.lf5.LogLevel;
import org.apache.log4j.lf5.LogLevelFormatException;
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;

import com.asetune.check.CheckForUpdates;


public class GuiLogAppender
    extends AppenderSkeleton
{
	private static Logger _logger = Logger.getLogger(GuiLogAppender.class);

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
	
	@Override
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
		
		// Yes this can be done better
		// for example with a listener, but I was in a hurry...
		if (record.isSevereLevel() || record.isWarningLevel())
//			CheckForUpdates.sendLogInfoNoBlock(record);
			CheckForUpdates.getInstance().sendLogInfoNoBlock(record);
	}

	@Override
	public void close()
	{
	}

	@Override
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

	/**
	 * Get current/active TableModel
	 * 
	 * @return The current/active TableModel
	 */
	public static Log4jTableModel getTableModel()
	{
		if (_instance == null) 
			return null;
		return getInstance()._logTable;
	}

	/**
	 * create a new TableModel... all settings will/should be copied from the "old/current/active" TableModel to the "new" TableModel
	 * 
	 * @return The previous active table will be returned (if this instance has not yet been created a null will be returned)
	 */
	public static Log4jTableModel getTableModelAndCreateNew()
	{
		if (_instance == null) 
			return null;

		// create a new table... all settings will/should be copied from the "old" TableModel to the "new" TableModel
		// The previous active table will be returned
		Log4jTableModel logTable = _instance._logTable;
		_instance._logTable = new Log4jTableModel(logTable);

		_logger.info("Clearing the in-memory TableModel for 'GuiLogAppender', which currently has " + logTable.getRowCount() + " entries.");

		return logTable;
	}
}
